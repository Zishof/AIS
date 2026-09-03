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

/**
 * Retur Barang (kantin/koperasi) — modul BARU di bawah tab Kulakan. Mencatat retur barang
 * (mis. barang rusak/kadaluarsa yang dikembalikan ke pemasok atau ditarik dari stok) beserta
 * jumlah, harga, dan keterangannya, untuk keperluan pencatatan & laporan Retur Barang.
 *
 * Entity baru → tabel "koperasi.retur_barang" otomatis dibuat (hbm2ddl=update) + terdaftar di
 * hibernate.cfg.xml. Kolom: id, produk, toko, qty, hargasatuan, total, keterangan, waktu, oleh.
 *
 * Catatan: laporan "Retur Barang" membaca tabel ini. Integrasi pengurang stok otomatis (rumus
 * stok) BELUM disertakan agar tidak mengubah perhitungan stok yang sudah berjalan; retur dicatat
 * sebagai dokumen/laporan. Java 1.7 / Hibernate 3.
 *
 * <p><b>KOREKSI per verifikasi 3 Sep 2026</b>: paragraf di atas menggambarkan NIAT desain awal
 * modul ini ("tab Kulakan", "dikembalikan ke pemasok"), tetapi penelusuran seluruh pemanggil di
 * basis kode menunjukkan modul CRUD/layar mandiri di bawah tab Kulakan itu <b>tidak pernah
 * benar-benar dibuat</b> -- tidak ada {@code ReturBarangAction}/{@code ReturBarangZkAction} di
 * mana pun. Satu-satunya titik yang benar-benar MEMBUAT baris {@code ReturBarang} saat ini adalah
 * {@code PengirimanGudangUtil.terima()}: saat menerima kiriman <b>ANTAR GUDANG/toko internal</b>,
 * porsi barang yang ditandai kondisi "rusak" dicatat otomatis sebagai satu baris
 * {@code ReturBarang} (dan SENGAJA tidak ditambahkan ke stok tujuan). Jadi entity ini pada
 * praktiknya adalah <b>efek samping otomatis satu fitur lain</b> (penerimaan transfer stok),
 * BUKAN modul retur-ke-supplier mandiri dengan alur input manual sendiri -- meski tetap dibaca
 * oleh laporan "Retur Barang" ({@code LaporanKantinUtil}) sebagai komponen kerugian P&L. Bandingkan
 * dengan {@link ReturPenjualan} (retur DARI pelanggan lewat POS, dengan alur create eksplisit dan
 * gerbang hak akses supervisor sendiri) -- keduanya independen, tidak ada relasi/warisan antar
 * keduanya; nama yang mirip ("Retur Barang" vs "Retur Penjualan") kebetulan, bukan penanda
 * hierarki kelas.</p>
 *
 * <p><b>Integrasi stok -- terkonfirmasi masih akurat</b>: klaim "integrasi pengurang stok BELUM
 * disertakan" pada paragraf di atas tetap benar sampai saat ini -- {@code
 * StokKantinUtil.formulaStokSql()} (rumus 8 suku yang menentukan stok jual) TIDAK menyebut
 * {@code retur_barang} sama sekali, berbeda dari {@link ReturPenjualan} yang eksplisit menjadi
 * suku ke-5 formula tersebut (hanya bila {@link ReturPenjualan#getKembalikanKeStok()} true).
 * Baris {@code ReturBarang} murni dokumen/laporan, tidak pernah memengaruhi angka stok jual.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "retur_barang")
public class ReturBarang extends GeneralValueObject {

    private static final long serialVersionUID = 1L;

    /** Primary key baris {@code koperasi.retur_barang}. Lihat {@link #getId()}. */
    private Long id;
    /** Produk yang tercatat rusak/diretur. Lihat {@link #getProduk()}. */
    private Produk produk;
    /** Toko/gudang tujuan penerimaan tempat baris ini tercatat. Lihat {@link #getToko()}. */
    private Toko toko;
    /** Jumlah barang. Lihat {@link #getQty()}. */
    private Double qty;
    /** Harga satuan barang, dasar {@link #getTotal()}. Lihat {@link #getHargaSatuan()}. */
    private Double hargaSatuan;
    /** Total nilai; dihitung malas dari qty×hargaSatuan bila kosong. Lihat {@link #getTotal()}. */
    private Double total;
    /** Keterangan bebas. Lihat {@link #getKeterangan()}. */
    private String keterangan;
    /** Waktu baris ini dicatat. Lihat {@link #getWaktu()}. */
    private Date waktu;
    /**
     * Nama pengguna pengubah terakhir -- field audit generik <b>shadow</b> milik
     * {@link GeneralValueObject} (WAJIB dideklarasikan ulang per entity konkret; lihat javadoc
     * {@link GeneralValueObject#getOleh()}). Untuk baris yang dibuat otomatis oleh
     * {@code PengirimanGudangUtil.terima()} (lihat javadoc kelas), field ini akan berisi
     * identitas pengguna yang memproses penerimaan kiriman, BUKAN identitas terpisah -- tidak ada
     * kolom seperti {@code kasirNama} pada {@link ais.database.model.inventory.SesiKasKasir}
     * di entity ini.
     */
    private String oleh;

    /**
     * Hook {@code @PreUpdate} Hibernate: menyinkronkan {@link #tanggal_dirubah} lewat
     * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui. Implementasi
     * kontrak {@link GeneralValueObject#onUpdate()}; isinya tipis karena logika stempel waktu
     * dipusatkan di interceptor bersama.
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    /**
     * Stempel waktu perubahan terakhir -- field shadow dengan alasan sama seperti {@link #oleh}.
     * Diinisialisasi ke waktu pembuatan object sehingga baris baru selalu punya nilai walau jalur
     * simpan lupa mengisinya.
     */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    /** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
    public ReturBarang() {
    }

    /**
     * Mengembalikan primary key baris ini.
     *
     * @return primary key, atau {@code null} bila belum tersimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Long getId() {
        return this.id;
    }

    /**
     * Menyetel primary key. Tanpa validasi.
     *
     * @param id nilai primary key baru
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Mengembalikan produk yang tercatat rusak/diretur, dengan proxy lazy diresolusi lewat
     * {@link #check(Object)}.
     *
     * @return produk terkait, boleh {@code null} (kolom tidak dipetakan {@code nullable = false})
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "produk")
    public Produk getProduk() {
        produk = check(produk);
        return produk;
    }

    /**
     * Menyetel produk terkait. Tanpa validasi.
     *
     * @param produk produk baru, boleh {@code null}
     */
    public void setProduk(Produk produk) {
        this.produk = produk;
    }

    /**
     * Mengembalikan {@link Toko}/gudang tujuan tempat baris ini tercatat, dengan proxy lazy
     * diresolusi lewat {@link #check(Object)}. Berbeda dari {@link Pedagang#getToko()}, getter ini
     * <b>tidak</b> punya fallback ke {@code Common.getCurrentToko()}.
     *
     * @return toko terkait, boleh {@code null}
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "toko")
    public Toko getToko() {
        toko = check(toko);
        return toko;
    }

    /**
     * Menyetel toko/gudang terkait. Tanpa validasi.
     *
     * @param toko toko baru, boleh {@code null}
     */
    public void setToko(Toko toko) {
        this.toko = toko;
    }

    /**
     * Mengembalikan jumlah barang -- dikalikan {@link #getHargaSatuan()} sebagai dasar
     * {@link #getTotal()}.
     *
     * @return qty, {@code 0.0} bila belum diisi
     */
    public Double getQty() {
        return qty == null ? 0.0 : qty;
    }

    /**
     * Menyetel qty. Tanpa validasi (termasuk tidak menolak nilai negatif).
     *
     * @param qty qty baru, boleh {@code null}
     */
    public void setQty(Double qty) {
        this.qty = qty;
    }

    /**
     * Mengembalikan harga satuan barang.
     *
     * @return harga satuan, {@code 0.0} bila belum diisi
     */
    public Double getHargaSatuan() {
        return hargaSatuan == null ? 0.0 : hargaSatuan;
    }

    /**
     * Menyetel harga satuan. Tanpa validasi.
     *
     * @param hargaSatuan harga satuan baru, boleh {@code null}
     */
    public void setHargaSatuan(Double hargaSatuan) {
        this.hargaSatuan = hargaSatuan;
    }

    /**
     * Mengembalikan total nilai baris ini ({@code qty × hargaSatuan}), dengan <b>pola "getter
     * destruktif" (compute-and-cache)</b> yang SAMA PERSIS dengan
     * {@link ReturPenjualan#getTotalNilai()} -- lihat javadoc lengkap di sana untuk penjelasan
     * detail perilakunya (termasuk kasus tepi total bernilai nol yang selalu dihitung ulang, dan
     * risiko nilai stale bila {@link #qty}/{@link #hargaSatuan} diubah SETELAH {@link #total}
     * sempat ter-cache bukan-nol). Ringkasnya: bila {@link #total} masih {@code null} ATAU
     * kebetulan {@code 0.0}, method ini menghitung ulang dari {@link #getQty()}{@code
     * × }{@link #getHargaSatuan()} dan <b>menyimpan hasilnya balik ke field {@link #total}</b>
     * sebelum mengembalikannya -- efek samping mengubah state object, bukan sekadar membaca. Pola
     * ini berulang di beberapa entity finansial pada inisiatif dokumentasi ini; bukan bug baru
     * yang unik pada entity ini.
     *
     * @return total nilai, dihitung ulang dari qty×hargaSatuan bila field masih kosong/nol
     */
    public Double getTotal() {
        if (total == null || total == 0.0) {
            total = getQty() * getHargaSatuan();
        }
        return total;
    }

    /**
     * Menyetel total nilai secara langsung, melewati perhitungan otomatis {@link #getTotal()}.
     * Tanpa validasi terhadap konsistensi qty×hargaSatuan.
     *
     * @param total total baru, boleh {@code null}
     */
    public void setTotal(Double total) {
        this.total = total;
    }

    /**
     * Mengembalikan keterangan bebas baris ini.
     *
     * @return keterangan, boleh {@code null}
     */
    @Column(columnDefinition = "text")
    public String getKeterangan() {
        return keterangan;
    }

    /**
     * Menyetel keterangan bebas. Tanpa validasi.
     *
     * @param keterangan keterangan baru, boleh {@code null}
     */
    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    /**
     * Mengembalikan waktu baris ini dicatat, dengan default waktu SEKARANG bila kolom kosong
     * (dihitung ulang tiap pemanggilan, tidak disimpan balik ke field).
     *
     * @return waktu pencatatan, tidak pernah {@code null}
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getWaktu() {
        return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
    }

    /**
     * Menyetel waktu pencatatan. Tanpa validasi.
     *
     * @param waktu waktu baru, boleh {@code null} (lihat {@link #getWaktu()} untuk fallback)
     */
    public void setWaktu(Date waktu) {
        this.waktu = waktu;
    }

    /**
     * Mengembalikan nama pengguna yang tercatat sebagai pengubah/pencatat baris ini.
     *
     * @return nama pengguna, atau {@code null} bila belum pernah terisi
     */
    public String getOleh() {
        return oleh;
    }

    /**
     * Menyetel nama pengguna pencatat. Tanpa validasi penolakan nilai kosong di kelas ini (berbeda
     * dari {@link GeneralValueObject#setOleh(String)}) -- langsung menimpa field apa adanya.
     *
     * @param oleh nama pengguna baru, boleh {@code null}/kosong (langsung menimpa)
     */
    public void setOleh(String oleh) {
        this.oleh = oleh;
    }

    /**
     * Mengembalikan stempel waktu perubahan terakhir, disinkronkan oleh {@link #onUpdate()} pada
     * tiap {@code UPDATE}.
     *
     * @return waktu perubahan terakhir
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    /**
     * Menyetel stempel waktu perubahan terakhir. Tanpa validasi -- normalnya diisi otomatis oleh
     * {@link #onUpdate()}, bukan dipanggil manual.
     *
     * @param tanggal_dirubah waktu perubahan terakhir
     */
    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }
}
