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

    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    public PemakaianBahanBaku() {
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "produk")
    public Produk getProduk() {
        produk = check(produk);
        return produk;
    }

    public void setProduk(Produk produk) {
        this.produk = produk;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "toko")
    public Toko getToko() {
        toko = check(toko);
        return toko;
    }

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

    public Double getQty() {
        return qty == null ? 0.0 : qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getWaktu() {
        return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
    }

    public void setWaktu(Date waktu) {
        this.waktu = waktu;
    }

    @Column(columnDefinition = "text")
    public String getKeterangan() {
        return keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    public String getOleh() {
        return oleh;
    }

    public void setOleh(String oleh) {
        this.oleh = oleh;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }
}
