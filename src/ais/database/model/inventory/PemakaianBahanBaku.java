package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;

/**
 * Pemakaian Bahan Baku (kantin/koperasi) — mencatat pemakaian/konsumsi stok produk sebagai
 * BAHAN BAKU (mis. bahan dapur yang dipakai untuk produksi), sehingga stok produk berkurang.
 *
 * Tabel ini sebelumnya hanya dirujuk lewat SQL native (rumus stok + beberapa dasbor) tetapi
 * BELUM punya kelas model & belum tentu ada di setiap database. Dengan adanya entity ini +
 * pendaftaran di hibernate.cfg.xml, tabel "koperasi.pemakaian_bahan_baku" akan otomatis dibuat
 * (hbm2ddl=update) dan transaksinya bisa dikelola lewat halaman Kulakan > Pemakaian Bahan Baku.
 *
 * Rumus stok produk sudah memperhitungkan pemakaian ini sebagai pengurang:
 *   stok = Σpengadaan − Σpembelian + Σopname.selisih − Σpemakaian_bahan_baku.qty
 *
 * Kolom mengikuti DDL: id, produk, toko, qty, waktu, keterangan, oleh. Java 1.7 / Hibernate 3.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pemakaian_bahan_baku")
public class PemakaianBahanBaku extends GeneralValueObject {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Produk produk;
    private Toko toko;
    private Double qty;
    private Date waktu;
    private String keterangan;
    private String oleh;
    private PembelianAnggotaKoperasi pembelianAnggotaKoperasi;

    /**
     * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum tiap {@code UPDATE}
     * baris ini lewat entity (bukan SQL native), mendelegasikan ke
     * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah} yang menyetel
     * {@link #tanggal_dirubah} ke waktu saat itu. Lihat javadoc {@link #getTanggal_dirubah()} soal
     * jalur konsumsi resep normal yang tidak memicu hook ini (menulis lewat SQL native).
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    /** Konstruktor kosong wajib Hibernate. Field diisi lewat setter/reflection sebelum atau saat pemuatan dari DB. */
    public PemakaianBahanBaku() {
    }

    /**
     * Primary key baris pemakaian ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
     * pada objek yang belum pernah di-{@code save}.
     * @return id baris, atau {@code null} jika belum tersimpan.
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Long getId() {
        return this.id;
    }

    /** Setter {@link #getId()} -- normalnya hanya dipanggil Hibernate saat memuat baris dari DB. */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Produk BAHAN BAKU yang stoknya dikonsumsi/berkurang pada baris ini (mis. kopi bubuk, gula) --
     * BUKAN produk jadi yang terjual (mis. kopi susu); lihat javadoc kelas soal peran ganda satu
     * baris pemakaian dalam rumus stok agregat {@code StokKantinUtil}. Getter memanggil
     * {@link ais.database.model.GeneralValueObject#check(Object)} untuk meresolusi proxy lazy yang
     * mungkin sudah <i>detached</i> dari session asalnya (pola getter relasi standar di seluruh
     * entity AIS) sebelum mengembalikan field.
     * @return produk bahan baku yang dikonsumsi baris ini.
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "produk")
    public Produk getProduk() {
        produk = check(produk);
        return produk;
    }

    /** Setter {@link #getProduk()}. */
    public void setProduk(Produk produk) {
        this.produk = produk;
    }

    /**
     * Toko tempat pemakaian bahan baku ini terjadi. Getter memanggil
     * {@link ais.database.model.GeneralValueObject#check(Object)}, lihat javadoc {@link #getProduk()}.
     * Penulis satu-satunya ({@link ais.action.master.inventory.BahanBakuUtil#konsumsiBahanBaku})
     * menulis lewat SQL native, jadi kolom ini bisa berisi {@code NULL} pada baris yang ditulis
     * dengan {@code Toko} tak dikenal ({@code tokoSql="null"}) -- getter TIDAK men-default ke toko
     * manapun, berbeda dari getter tanggal/qty di kelas ini.
     * @return toko baris pemakaian ini, boleh {@code null}.
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "toko")
    public Toko getToko() {
        toko = check(toko);
        return toko;
    }

    /** Setter {@link #getToko()}. */
    public void setToko(Toko toko) {
        this.toko = toko;
    }

    /**
     * Bill (header pembelian) acuan idempoten -- dipakai {@code BahanBakuUtil.konsumsiBahanBaku}
     * utk menghapus dulu baris pemakaian milik bill ini sebelum menyisipkan ulang, supaya
     * memproses ulang sebuah pesanan tidak menggandakan pemotongan stok bahan baku. Kolom ini
     * SEBELUMNYA hanya dipakai via SQL native tanpa mapping entity (hbm2ddl tak pernah tahu harus
     * membuatnya) -- sekarang dipetakan resmi spy pembuatan kolomnya diserahkan ke hbm2ddl.auto,
     * konsisten dgn cara tabel ini sendiri dirancang (lihat javadoc kelas).
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "pembelian_anggota_koperasi")
    public PembelianAnggotaKoperasi getPembelianAnggotaKoperasi() {
        pembelianAnggotaKoperasi = check(pembelianAnggotaKoperasi);
        return pembelianAnggotaKoperasi;
    }

    public void setPembelianAnggotaKoperasi(PembelianAnggotaKoperasi pembelianAnggotaKoperasi) {
        this.pembelianAnggotaKoperasi = pembelianAnggotaKoperasi;
    }

    /**
     * Kuantitas bahan baku yang dikonsumsi pada baris ini, dalam satuan dasar {@link #getProduk()}.
     * Getter null-safe: {@code null} dibaca sebagai {@code 0.0}. Nilai ditulis
     * {@code BahanBakuUtil.konsumsiBahanBaku} sebagai HASIL PERKALIAN {@code qtyResep x jual} --
     * dijumlahkan dulu (akumulasi) per bahan baku sebelum satu baris ditulis per bahan baku per
     * bill, BUKAN satu baris per item terjual.
     * @return kuantitas konsumsi baris ini, tidak pernah {@code null}.
     */
    public Double getQty() {
        return qty == null ? 0.0 : qty;
    }

    /** Setter {@link #getQty()}. */
    public void setQty(Double qty) {
        this.qty = qty;
    }

    /**
     * Waktu terjadinya pemakaian ini. Getter null-safe: {@code null} dibaca sebagai waktu saat
     * getter dipanggil ({@code WaktuUtil.getDate()}). Penulis satu-satunya ({@code BahanBakuUtil})
     * menyalin waktu transaksi penjualan (bill) ke kolom ini, bukan waktu baris ditulis.
     * @return waktu pemakaian, tidak pernah {@code null}.
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getWaktu() {
        return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
    }

    /** Setter {@link #getWaktu()}. */
    public void setWaktu(Date waktu) {
        this.waktu = waktu;
    }

    /**
     * Catatan bebas untuk baris pemakaian ini, opsional. Penulis satu-satunya
     * ({@code BahanBakuUtil.konsumsiBahanBaku}) TIDAK mengisi kolom ini (insert native-nya hanya
     * mengisi produk/qty/toko/waktu/pembelianAnggotaKoperasi) -- kolom ini praktiknya kosong
     * kecuali diisi manual lewat editor generik ({@code RevisiApiHelper}).
     * @return catatan, atau {@code null} bila tidak diisi.
     */
    @Column(columnDefinition = "text")
    public String getKeterangan() {
        return keterangan;
    }

    /** Setter {@link #getKeterangan()}. */
    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    /**
     * Identitas petugas/proses pemicu baris ini, opsional. Sama seperti {@link #getKeterangan()},
     * penulis satu-satunya ({@code BahanBakuUtil.konsumsiBahanBaku}) TIDAK mengisi kolom ini --
     * jejak audit "siapa" untuk pemakaian bahan baku sebenarnya melekat pada bill
     * ({@link #getPembelianAnggotaKoperasi()}), bukan pada kolom ini.
     * @return identitas pemicu, atau {@code null} bila tidak diisi.
     */
    public String getOleh() {
        return oleh;
    }

    /** Setter {@link #getOleh()}. */
    public void setOleh(String oleh) {
        this.oleh = oleh;
    }

    /**
     * Waktu baris ini terakhir diubah. Diinisialisasi ke waktu instansiasi objek pada deklarasi
     * field, dan diperbarui otomatis oleh {@link #onUpdate()} setiap {@code UPDATE} lewat
     * Hibernate. Penulis satu-satunya ({@code BahanBakuUtil.konsumsiBahanBaku}) menulis lewat SQL
     * native (INSERT langsung), sehingga hook {@code @PreUpdate} ini TIDAK pernah terpicu untuk
     * baris yang ditulis lewat jalur konsumsi resep normal -- kolom ini hanya relevan bila baris
     * diubah lewat entity Hibernate penuh (mis. editor generik {@code RevisiApiHelper}).
     * @return waktu baris ini terakhir diubah.
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    /** Setter {@link #getTanggal_dirubah()} -- normalnya hanya dipanggil {@link #onUpdate()}. */
    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }
}
