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

    /**
     * PK identity baris arsip pembatalan ini. {@code null} sebelum entity di-{@code save}/
     * {@code flush} ke Hibernate. {@code insertable = false} — kolom {@code id} TIDAK disertakan
     * pada statement {@code INSERT}, nilainya murni ditentukan DB lewat identity.
     *
     * @return id baris arsip, atau {@code null} bila belum tersimpan.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() {
        return id;
    }

    /**
     * Setter PK — dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak perlu
     * memanggil ini; id baru dibuat otomatis oleh DB saat insert.
     *
     * @param id id baris arsip.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /** Jejak audit standar entitas (dipanggil Hibernate sebelum update). */
    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    /**
     * Setter manual untuk {@link #tanggal_dirubah}. Jarang dipakai langsung — field ini biasanya
     * diisi otomatis oleh {@link #onUpdate()}.
     *
     * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
     */
    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }

    /**
     * Timestamp perubahan terakhir baris arsip ini, diisi otomatis oleh {@link #onUpdate()}.
     *
     * @return waktu perubahan terakhir, atau waktu instansiasi objek bila belum pernah di-update.
     */
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

    /**
     * Menetapkan id transaksi asli yang diarsipkan. Lihat catatan {@link #getPembelianAnggotaKoperasiId()}
     * — SENGAJA bukan foreign key, jadi tidak divalidasi terhadap baris apa pun.
     *
     * @param pembelianAnggotaKoperasiId id transaksi asli.
     */
    public void setPembelianAnggotaKoperasiId(Long pembelianAnggotaKoperasiId) {
        this.pembelianAnggotaKoperasiId = pembelianAnggotaKoperasiId;
    }

    /** Kode/nomor transaksi asli, agar mudah dicari kembali oleh petugas. */
    @Column(name = "kode", length = 100)
    public String getKode() {
        return kode;
    }

    /**
     * Menetapkan kode/nomor transaksi asli.
     *
     * @param kode kode transaksi asli.
     */
    public void setKode(String kode) {
        this.kode = kode;
    }

    /**
     * Relasi ke toko/outlet asal transaksi yang dibatalkan — dipertahankan (nullable) semata-mata
     * untuk penyaringan/penelusuran per toko, BUKAN sumber tampilan nama (lihat Javadoc kelas,
     * bagian "Sengaja memakai potret teks, bukan relasi").
     *
     * @return toko asal transaksi, atau {@code null} bila tidak diisi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toko", nullable = true)
    public Toko getToko() {
        toko = check(toko);
        return toko;
    }

    /**
     * Menetapkan toko/outlet asal transaksi yang dibatalkan.
     *
     * @param toko toko baru.
     */
    public void setToko(Toko toko) {
        this.toko = toko;
    }

    /**
     * Relasi ke anggota koperasi pembeli — dipertahankan (nullable) semata-mata untuk
     * penyaringan/penelusuran per anggota, BUKAN sumber tampilan nama; nama tampil dibaca dari
     * potret teks {@link #getNamaAnggota()} (lihat Javadoc kelas).
     *
     * @return anggota koperasi pembeli, atau {@code null} bila tidak diisi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anggota_koperasi", nullable = true)
    public AnggotaKoperasi getAnggotaKoperasi() {
        anggotaKoperasi = check(anggotaKoperasi);
        return anggotaKoperasi;
    }

    /**
     * Menetapkan anggota koperasi pembeli pada transaksi yang dibatalkan.
     *
     * @param anggotaKoperasi anggota koperasi baru.
     */
    public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
        this.anggotaKoperasi = anggotaKoperasi;
    }

    /** Potret nama pembeli saat dibatalkan — lihat catatan "potret teks" pada JavaDoc kelas. */
    @Column(name = "nama_anggota", length = 255)
    public String getNamaAnggota() {
        return namaAnggota;
    }

    /**
     * Menetapkan potret nama pembeli saat dibatalkan.
     *
     * @param namaAnggota nama pembeli (teks, bukan relasi).
     */
    public void setNamaAnggota(String namaAnggota) {
        this.namaAnggota = namaAnggota;
    }

    /** Potret nama kasir/petugas yang MEMBUAT transaksi (bukan yang membatalkan). */
    @Column(name = "nama_kasir", length = 255)
    public String getNamaKasir() {
        return namaKasir;
    }

    /**
     * Menetapkan potret nama kasir/petugas pembuat transaksi asli.
     *
     * @param namaKasir nama kasir (teks, bukan relasi).
     */
    public void setNamaKasir(String namaKasir) {
        this.namaKasir = namaKasir;
    }

    /**
     * Potret nama cara pembayaran (mis. tunai/deposit/nama kas) yang dipakai transaksi asli — teks
     * apa adanya, dipakai a.l. sbg penentu akun kas lawan saat mesin posting "Pembatalan Penjualan
     * Kantin" menjurnal-balikkan pembatalan yang sudah terposting (lihat Javadoc
     * {@link #getPostingHistory()}).
     *
     * @return nama cara pembayaran, atau {@code null} bila tidak diisi.
     */
    @Column(name = "cara_pembayaran", length = 255)
    public String getCaraPembayaran() {
        return caraPembayaran;
    }

    /**
     * Menetapkan potret nama cara pembayaran transaksi asli.
     *
     * @param caraPembayaran nama cara pembayaran (teks).
     */
    public void setCaraPembayaran(String caraPembayaran) {
        this.caraPembayaran = caraPembayaran;
    }

    /**
     * Total biaya/nominal transaksi asli sebelum diskon.
     *
     * @return total biaya, tidak pernah {@code null} ({@code 0} sebagai fallback).
     */
    @Column(name = "total_biaya")
    public Double getTotalBiaya() {
        return totalBiaya == null ? Double.valueOf(0) : totalBiaya;
    }

    /**
     * Menetapkan total biaya transaksi asli.
     *
     * @param totalBiaya total biaya baru.
     */
    public void setTotalBiaya(Double totalBiaya) {
        this.totalBiaya = totalBiaya;
    }

    /**
     * Total diskon yang diberikan pada transaksi asli.
     *
     * @return total diskon, tidak pernah {@code null} ({@code 0} sebagai fallback).
     */
    @Column(name = "total_diskon")
    public Double getTotalDiskon() {
        return totalDiskon == null ? Double.valueOf(0) : totalDiskon;
    }

    /**
     * Menetapkan total diskon transaksi asli.
     *
     * @param totalDiskon total diskon baru.
     */
    public void setTotalDiskon(Double totalDiskon) {
        this.totalDiskon = totalDiskon;
    }

    /** Waktu transaksi ASLI dibayar (bukan waktu pembatalan). */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_transaksi")
    public Date getTanggalTransaksi() {
        return tanggalTransaksi;
    }

    /**
     * Menetapkan waktu transaksi ASLI dibayar.
     *
     * @param tanggalTransaksi waktu transaksi asli.
     */
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

    /**
     * Menetapkan potret isi keranjang saat dibatalkan.
     *
     * @param rincian teks rincian item, satu baris per item.
     */
    public void setRincian(String rincian) {
        this.rincian = rincian;
    }

    /** Alasan pembatalan — WAJIB diisi petugas, inilah inti dari seluruh arsip ini. */
    @Column(name = "alasan", columnDefinition = "text")
    public String getAlasan() {
        return alasan;
    }

    /**
     * Menetapkan alasan pembatalan.
     *
     * @param alasan alasan pembatalan (wajib diisi oleh alur pemanggil).
     */
    public void setAlasan(String alasan) {
        this.alasan = alasan;
    }

    /** Nama pengguna yang MELAKUKAN pembatalan. */
    @Column(name = "dibatalkan_oleh", length = 255)
    public String getDibatalkanOleh() {
        return dibatalkanOleh;
    }

    /**
     * Menetapkan nama pengguna yang melakukan pembatalan.
     *
     * @param dibatalkanOleh nama pengguna pembatal.
     */
    public void setDibatalkanOleh(String dibatalkanOleh) {
        this.dibatalkanOleh = dibatalkanOleh;
    }

    /**
     * Id/username pengguna yang MELAKUKAN pembatalan, pasangan {@link #getDibatalkanOleh()}.
     *
     * @return id/username pengguna pembatal, atau {@code null} bila tidak diisi.
     */
    @Column(name = "dibatalkan_oleh_id", length = 100)
    public String getDibatalkanOlehId() {
        return dibatalkanOlehId;
    }

    /**
     * Menetapkan id/username pengguna yang melakukan pembatalan.
     *
     * @param dibatalkanOlehId id/username pengguna pembatal.
     */
    public void setDibatalkanOlehId(String dibatalkanOlehId) {
        this.dibatalkanOlehId = dibatalkanOlehId;
    }

    /**
     * Waktu pembatalan dilakukan (BUKAN waktu transaksi asli — lihat {@link #getTanggalTransaksi()}).
     * Dipakai sbg tanggal jurnal balik saat mesin posting "Pembatalan Penjualan Kantin" menjurnal
     * arsip ini (lihat Javadoc {@link #getPostingHistory()}).
     *
     * @return waktu pembatalan, atau {@code null} bila tidak diisi.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_dibatalkan")
    public Date getTanggalDibatalkan() {
        return tanggalDibatalkan;
    }

    /**
     * Menetapkan waktu pembatalan dilakukan.
     *
     * @param tanggalDibatalkan waktu pembatalan baru.
     */
    public void setTanggalDibatalkan(Date tanggalDibatalkan) {
        this.tanggalDibatalkan = tanggalDibatalkan;
    }

    /**
     * Menandai bahwa transaksi ASLI ini SUDAH pernah diposting ke jurnal akuntansi (batch
     * Penjualan Kantin) pada saat dibatalkan — diisi sekali oleh
     * {@code PembatalanTransaksiUtil.batalkan(...)} dari {@code trx.getPostingHistory() != null}.
     *
     * <p><b>Pemakaian pasca dok 61 butir C (r78603).</b> Sejak baris dasbor "Pembatalan Penjualan
     * Kantin" ditambahkan, flag {@code true} pada field ini BUKAN sekadar penanda pasif —
     * dipakai langsung sbg kriteria seleksi ({@code sudahDiposting = TRUE}) oleh
     * {@code PembatalanTransaksiUtil.kriteriaPembatalanStatic}/
     * {@code DraftJurnalRingkasanUtil.kriteriaPembatalanKantin} utk menentukan arsip mana yang
     * BUTUH dan BOLEH dijurnal-balik otomatis lewat {@code PembatalanTransaksiUtil.postingSemua}.
     * Arsip yang {@code sudahDiposting == false} (transaksi asli belum sempat terposting saat
     * dibatalkan) TIDAK PERNAH dijurnal balik — sudah keluar dari kriteria batch Penjualan sebelum
     * pernah dijurnal, jadi tidak butuh koreksi.
     *
     * @return {@code true} bila transaksi asli sudah terposting saat dibatalkan (arsip ini
     *         kandidat jurnal balik), tidak pernah {@code null} ({@code false} sebagai fallback).
     */
    @Column(name = "sudah_diposting")
    public Boolean getSudahDiposting() {
        return sudahDiposting == null ? Boolean.FALSE : sudahDiposting;
    }

    /**
     * Menetapkan status "sudah diposting" transaksi asli.
     *
     * @param sudahDiposting status baru; lihat catatan {@link #getSudahDiposting()} — bernilai
     *                       {@code true} membuat arsip ini masuk kriteria jurnal balik otomatis.
     */
    public void setSudahDiposting(Boolean sudahDiposting) {
        this.sudahDiposting = sudahDiposting;
    }

    private ais.database.model.akunting.PostingHistory postingHistory;

    /**
     * Riwayat JURNAL BALIK pembatalan (dok 61 butir C): terisi begitu mesin posting
     * "Pembatalan Penjualan Kantin" ({@code PembatalanTransaksiUtil.postingSemua}) menjurnal-
     * balikkan pendapatan transaksi terposting yang dibatalkan ini — jurnal per arsip: Dr akun
     * pendapatan (konfigurasi {@code akun_pendapatan_pembatalan_kantin_id}) / Cr akun kas dari
     * {@link #getCaraPembayaran()} (dicari per nama, potret teks), senilai {@link #getTotalBiaya()},
     * bertanggal {@link #getTanggalDibatalkan()}. Hanya relevan bila {@link #getSudahDiposting()}
     * true — arsip yang belum terposting asalnya tidak pernah masuk kriteria seleksi jurnal balik.
     * {@code null} berarti BELUM diposting (masih draf) ATAU pembatalannya sendiri tidak memerlukan
     * jurnal balik. Dilepas kembali ke {@code null} oleh
     * {@code PembatalanTransaksiUtil.batalkanPostingSemua} saat posting dibatalkan mundur (baris
     * jurnal turunan yang belum closing dihapus lebih dulu).
     *
     * @return riwayat posting jurnal balik, atau {@code null} bila belum/tidak diposting.
     */
    @javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
    @javax.persistence.JoinColumn(name = "posting_history", nullable = true)
    public ais.database.model.akunting.PostingHistory getPostingHistory() {
        postingHistory = check(postingHistory);
        return postingHistory;
    }

    /**
     * Menetapkan riwayat posting jurnal balik arsip ini.
     *
     * @param postingHistory riwayat posting baru; {@code null} utk melepas tautan (dipakai alur
     *                       batal-mundur posting).
     */
    public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
        this.postingHistory = postingHistory;
    }

}
