package ais.database.model.koperasi;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import ais.database.model.GeneralValueObject;
import ais.database.model.inventory.Toko;

/**
 * <h2>Arsip Pembatalan Transaksi Kantin/POS.</h2>
 *
 * <p><b>Masalah yang diselesaikan.</b> Sampai sebelum entitas ini ada, membatalkan transaksi POS
 * berarti MENGHAPUS FISIK barisnya dari {@code koperasi.pembelian_anggota_koperasi} dan
 * {@code koperasi.pembelian} — tidak ada kolom status/void/batal sama sekali di kedua tabel itu.
 * Akibatnya tidak ada catatan siapa membatalkan apa, kapan, dan yang paling penting: <b>kenapa</b>.
 * Pimpinan tidak punya cara memeriksa pola pembatalan yang mencurigakan.</p>
 *
 * <p><b>Kenapa arsip, bukan penanda "dibatalkan" di tabel aslinya.</b> Pilihan yang lebih lazim
 * adalah menandai transaksi sebagai dibatalkan di tempat (soft-cancel) dan membiarkan barisnya
 * tetap ada. Cara itu SENGAJA TIDAK dipakai di sini setelah audit menyeluruh, karena
 * {@code koperasi.pembelian} dibaca di sekitar 120 tempat dan <b>hanya 34 di antaranya menyaring
 * kolom {@code aktif}</b>. Membiarkan baris yang dibatalkan tetap ada berarti ~86 pembacaan lain
 * akan terus menghitungnya, dengan tiga akibat yang berbahaya:</p>
 * <ol>
 *   <li><b>Saldo siswa/anggota jadi salah</b> — {@code Siswa.getSaldo()} dan
 *       {@code DepositHelper} menjumlahkan pembelian tanpa menyaring {@code aktif}, sehingga siswa
 *       tetap tertagih untuk transaksi yang sudah dibatalkan. Ini ikut menular ke 18 laporan cetak
 *       (jrxml) saldo &amp; tabungan.</li>
 *   <li><b>Stok tidak pulih</b> — stok produk di modul ini DIHITUNG ULANG dari riwayat penjualan
 *       ({@code StokKantinUtil.recomputeStokProduk}: {@code stok = masuk + opname - keluar - pakai}),
 *       bukan dari buku besar mutasi. Dengan penghapusan, stok pulih dengan sendirinya. Bila baris
 *       dibiarkan tetap ada, stok akan berkurang permanen dan gerbang anti-oversell
 *       ({@code KantinHelper}) akan memblokir penjualan ulang barang tersebut.</li>
 *   <li><b>Baris hantu setiap simpan-ulang</b> — {@code KantinHelper} menghapus lalu menulis ulang
 *       seluruh baris rincian setiap kali checkout disimpan ulang; bila penghapusan itu berubah jadi
 *       penandaan, setiap penyimpanan meninggalkan sampah.</li>
 * </ol>
 *
 * <p>Maka pendekatan yang dipakai: saat dibatalkan, <b>seluruh isi transaksi disalin ke tabel ini
 * sebagai potret (snapshot) lengkap beserta alasan pembatalan</b>, baru kemudian baris aslinya
 * dihapus seperti sebelumnya. Dari sudut pandang seluruh laporan, stok, dan saldo, perilakunya
 * <b>persis sama seperti sebelum perubahan ini</b> — tidak ada satu pun query lama yang perlu
 * diubah, jadi tidak ada risiko angka bergeser diam-diam. Yang bertambah hanyalah: sekarang ada
 * catatan permanen yang bisa dilaporkan ke pimpinan.</p>
 *
 * <p><b>Sengaja memakai potret teks, bukan relasi.</b> {@link #getNamaAnggota()},
 * {@link #getNamaKasir()}, {@link #getCaraPembayaran()} dan {@link #getRincian()} disimpan sebagai
 * teks apa adanya pada saat pembatalan, BUKAN sebagai foreign key. Alasannya: arsip pembatalan harus
 * tetap terbaca utuh bertahun-tahun kemudian walaupun anggota, produk, atau metode pembayarannya
 * kelak diubah namanya atau dihapus. Relasi ke {@link Toko} dan {@link AnggotaKoperasi} tetap
 * disimpan (nullable) semata-mata untuk penyaringan/penelusuran, bukan untuk menampilkan nama.</p>
 *
 * <p><b>Catatan pemakaian:</b> entitas ini HANYA diisi oleh pembatalan yang dilakukan pengguna lewat
 * layar Pembelian Anggota Koperasi ({@code PembatalanTransaksiUtil}). Penghapusan teknis internal —
 * mis. hapus-lalu-tulis-ulang rincian saat checkout disimpan ulang, atau pemrosesan ulang topup —
 * BUKAN pembatalan dan sengaja tidak dicatat di sini, supaya arsip ini tetap bermakna sebagai
 * daftar pembatalan yang benar-benar dilakukan orang.</p>
 */
@Entity
@Table(schema = "koperasi", name = "pembatalan_transaksi")
public class PembatalanTransaksiKantin extends GeneralValueObject {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long pembelianAnggotaKoperasiId;
    private String kode;
    private Toko toko;
    private AnggotaKoperasi anggotaKoperasi;
    private String namaAnggota;
    private String namaKasir;
    private String caraPembayaran;
    private Double totalBiaya;
    private Double totalDiskon;
    private Date tanggalTransaksi;
    private String rincian;
    private String alasan;
    private String dibatalkanOleh;
    private String dibatalkanOlehId;
    private Date tanggalDibatalkan;
    private Boolean sudahDiposting;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** Jejak audit standar entitas (dipanggil Hibernate sebelum update). */
    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    /**
     * Id transaksi asli. SENGAJA hanya angka biasa, bukan foreign key — barisnya memang sudah
     * dihapus saat arsip ini dibuat, jadi FK justru akan gagal. Berguna untuk mencocokkan dengan
     * jejak audit/Envers bila diperlukan penelusuran lebih dalam.
     */
    @Column(name = "pembelian_anggota_koperasi_id")
    public Long getPembelianAnggotaKoperasiId() {
        return pembelianAnggotaKoperasiId;
    }

    public void setPembelianAnggotaKoperasiId(Long pembelianAnggotaKoperasiId) {
        this.pembelianAnggotaKoperasiId = pembelianAnggotaKoperasiId;
    }

    /** Kode/nomor transaksi asli, agar mudah dicari kembali oleh petugas. */
    @Column(name = "kode", length = 100)
    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toko", nullable = true)
    public Toko getToko() {
        return toko;
    }

    public void setToko(Toko toko) {
        this.toko = toko;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anggota_koperasi", nullable = true)
    public AnggotaKoperasi getAnggotaKoperasi() {
        return anggotaKoperasi;
    }

    public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
        this.anggotaKoperasi = anggotaKoperasi;
    }

    /** Potret nama pembeli saat dibatalkan — lihat catatan "potret teks" pada JavaDoc kelas. */
    @Column(name = "nama_anggota", length = 255)
    public String getNamaAnggota() {
        return namaAnggota;
    }

    public void setNamaAnggota(String namaAnggota) {
        this.namaAnggota = namaAnggota;
    }

    /** Potret nama kasir/petugas yang MEMBUAT transaksi (bukan yang membatalkan). */
    @Column(name = "nama_kasir", length = 255)
    public String getNamaKasir() {
        return namaKasir;
    }

    public void setNamaKasir(String namaKasir) {
        this.namaKasir = namaKasir;
    }

    @Column(name = "cara_pembayaran", length = 255)
    public String getCaraPembayaran() {
        return caraPembayaran;
    }

    public void setCaraPembayaran(String caraPembayaran) {
        this.caraPembayaran = caraPembayaran;
    }

    @Column(name = "total_biaya")
    public Double getTotalBiaya() {
        return totalBiaya == null ? Double.valueOf(0) : totalBiaya;
    }

    public void setTotalBiaya(Double totalBiaya) {
        this.totalBiaya = totalBiaya;
    }

    @Column(name = "total_diskon")
    public Double getTotalDiskon() {
        return totalDiskon == null ? Double.valueOf(0) : totalDiskon;
    }

    public void setTotalDiskon(Double totalDiskon) {
        this.totalDiskon = totalDiskon;
    }

    /** Waktu transaksi ASLI dibayar (bukan waktu pembatalan). */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_transaksi")
    public Date getTanggalTransaksi() {
        return tanggalTransaksi;
    }

    public void setTanggalTransaksi(Date tanggalTransaksi) {
        this.tanggalTransaksi = tanggalTransaksi;
    }

    /**
     * Potret isi keranjang saat dibatalkan, satu baris per item dalam bentuk teks siap baca
     * (mis. {@code "2 x Nasi Goreng @ 12.000 = 24.000"}). Disimpan sbg teks, bukan tabel rincian
     * tersendiri, karena kebutuhannya adalah bukti/penelusuran yang harus tetap terbaca utuh
     * walaupun produknya kelak dihapus atau diganti nama.
     */
    @Column(name = "rincian", columnDefinition = "text")
    public String getRincian() {
        return rincian;
    }

    public void setRincian(String rincian) {
        this.rincian = rincian;
    }

    /** Alasan pembatalan — WAJIB diisi petugas, inilah inti dari seluruh arsip ini. */
    @Column(name = "alasan", columnDefinition = "text")
    public String getAlasan() {
        return alasan;
    }

    public void setAlasan(String alasan) {
        this.alasan = alasan;
    }

    /** Nama pengguna yang MELAKUKAN pembatalan. */
    @Column(name = "dibatalkan_oleh", length = 255)
    public String getDibatalkanOleh() {
        return dibatalkanOleh;
    }

    public void setDibatalkanOleh(String dibatalkanOleh) {
        this.dibatalkanOleh = dibatalkanOleh;
    }

    @Column(name = "dibatalkan_oleh_id", length = 100)
    public String getDibatalkanOlehId() {
        return dibatalkanOlehId;
    }

    public void setDibatalkanOlehId(String dibatalkanOlehId) {
        this.dibatalkanOlehId = dibatalkanOlehId;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_dibatalkan")
    public Date getTanggalDibatalkan() {
        return tanggalDibatalkan;
    }

    public void setTanggalDibatalkan(Date tanggalDibatalkan) {
        this.tanggalDibatalkan = tanggalDibatalkan;
    }

    /**
     * Menandai bahwa transaksi ini SUDAH pernah diposting ke jurnal akuntansi saat dibatalkan.
     * Penting bagi bagian keuangan: pembatalan transaksi yang sudah diposting meninggalkan selisih
     * antara jurnal dan data sumber, sehingga membutuhkan jurnal koreksi/pembalik tersendiri —
     * arsip ini menandainya agar tidak terlewat, bukan membuat jurnalnya secara otomatis.
     */
    @Column(name = "sudah_diposting")
    public Boolean getSudahDiposting() {
        return sudahDiposting == null ? Boolean.FALSE : sudahDiposting;
    }

    public void setSudahDiposting(Boolean sudahDiposting) {
        this.sudahDiposting = sudahDiposting;
    }
}
