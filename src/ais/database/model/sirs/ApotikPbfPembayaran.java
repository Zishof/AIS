package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
import ais.database.model.akunting.PostingHistory;
import ais.database.model.koperasi.CaraPembayaranKoperasi;

/**
 * Pembayaran utang satu dokumen PBF, mendukung pelunasan bertahap.
 *
 * <h3>Kedudukan</h3>
 *
 * <p>PBF adalah Pedagang Besar Farmasi — distributor resmi yang memasok obat ke
 * apotek. Ketika obat diterima, {@link ApotikPbfDokumen} lahir dan apotek berutang
 * sebesar {@link ApotikPbfDokumen#getTotal()}. Entity ini adalah cicilan atas utang
 * itu: satu baris untuk setiap kali apotek membayar, sehingga satu dokumen boleh
 * punya banyak baris pembayaran dan sisa utangnya adalah total dokumen dikurangi
 * jumlah seluruh barisnya.</p>
 *
 * <p>Bentuk banyak-baris ini dipilih, alih-alih satu kolom "sudah dibayar" pada
 * dokumen, karena alasan yang sama yang membuat {@link ApotikBatchKonsumsi}
 * berbentuk buku besar: sebuah angka saldo tidak dapat menjawab kapan, dengan cara
 * apa, dan oleh siapa pembayaran dilakukan, sedangkan pertanyaan-pertanyaan itulah
 * yang muncul ketika catatan apotek dan tagihan distributor tidak cocok.</p>
 *
 * <h3>Penjaga anti-lebih-bayar — ADA, dan aman terhadap balapan</h3>
 *
 * <p>Ini pertanyaan pertama yang wajar diajukan atas entity semacam ini, karena
 * pola pembayaran-bertahap tanpa penjaga sudah berulang di modul lain: tanpa
 * pemeriksaan, dua permintaan bayar yang datang bersamaan dapat membayar utang
 * yang sama dua kali penuh, dan uang yang mengalir keluar melebihi yang
 * terutang tidak akan pernah tampak di baris mana pun.</p>
 *
 * <p>Di jalur apotek, penjaga itu ADA dan bentuknya benar.
 * {@code ApotikPersediaanHelper.pbfBayar} melakukan seluruh rangkaian berikut di
 * dalam SATU transaksi: memuat dokumennya dengan {@code LockMode.UPGRADE} —
 * mengunci baris dokumen di basis data — lalu menjumlahkan seluruh pembayaran
 * yang sudah ada atas dokumen itu, menghitung sisanya, dan MENOLAK bila nominal
 * yang diminta melebihi sisa (dengan toleransi pembulatan 0,005). Baru setelah
 * itu baris pembayaran ditulis dan transaksi di-commit.</p>
 *
 * <p>Kunci baris pada dokumen itulah yang membuat penjaga ini benar-benar bekerja,
 * bukan sekadar terlihat bekerja. Tanpa kunci, dua permintaan bersamaan akan
 * sama-sama membaca "sudah dibayar" yang lama, sama-sama menyimpulkan sisanya
 * cukup, dan sama-sama lolos — pemeriksaannya ada tetapi tidak menahan apa pun.
 * Dengan kunci, permintaan kedua menunggu sampai yang pertama selesai, lalu
 * membaca jumlah yang sudah termasuk pembayaran pertama.</p>
 *
 * <p><b>Jangan menambah jalur pembuatan pembayaran yang melewatkan urutan itu.</b>
 * Entity ini sendiri tidak dapat menolak apa pun: satu baris tidak tahu berapa
 * total dokumennya sudah terbayar, dan tidak ada batasan basis data yang
 * membandingkan jumlah baris terhadap total dokumen. Seluruh perlindungan hidup
 * di satu metode. Jalur baru yang menulis {@code ApotikPbfPembayaran} tanpa
 * mengunci dokumennya lebih dulu akan membuka kembali celah yang sudah
 * ditutup.</p>
 *
 * <h3>Hubungan dengan jurnal</h3>
 *
 * <p>{@link #getPostingHistory()} adalah penanda idempoten: selama masih kosong,
 * baris ini belum menjadi jurnal dan akan ikut terjaring layar posting; sesudah
 * terisi, {@code ApotikPbfPostingHelper} melewatinya. Perhatikan bahwa entity ini
 * tidak mengunci dirinya setelah diposting — nominalnya masih dapat diubah lewat
 * setter, sementara jurnal yang sudah terbentuk tidak ikut berubah. Tidak ada
 * yang mendeteksi ketidakcocokan itu.</p>
 *
 * @see ApotikPbfDokumen dokumen utang yang dicicil baris ini
 * @see ApotikAkunMapping pemetaan akun Utang PBF dan Kas/Bank yang dipakai jurnalnya
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_pbf_pembayaran")
public class ApotikPbfPembayaran extends GeneralValueObject {

    /** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
    private static final long serialVersionUID = 1L;

    /** Kunci baris; dibangkitkan basis data. */
    private Long id;

    /** Dokumen utang PBF yang dicicil baris ini. Wajib. */
    private ApotikPbfDokumen dokumen;

    /** Cara pembayaran — pembawa akun Kas/Bank yang dikreditkan jurnalnya. Wajib. */
    private CaraPembayaranKoperasi caraBayar;

    /** Nominal yang dibayarkan. */
    private Double nominal;

    /** Tanggal pembayaran; penentu penjaringan periode pada layar posting. */
    private Date tanggal;

    /** Catatan bebas (nomor bukti transfer, dan sebagainya). */
    private String keterangan;

    /** Jurnal yang sudah terbentuk dari baris ini; kosong berarti belum diposting. */
    private PostingHistory postingHistory;

    /** Nama tampil pelaku pencatatan (bayangan audit). */
    private String oleh;

    /** Identitas akun pelaku pencatatan (bayangan audit). */
    private String olehId;

    /** Stempel ubah terakhir; disegarkan interseptor audit pada setiap UPDATE. */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    /**
     * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
     *
     * <p>{@code insertable = false}: nilai apa pun di objek Java tidak ikut dalam
     * INSERT, sehingga kunci baris tidak dapat didikte pemanggil.</p>
     *
     * @return kunci baris, atau {@code null} bila belum tersimpan
     */
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }

    /**
     * Menetapkan kunci baris; dipakai Hibernate, bukan kode aplikasi.
     *
     * @param id kunci baris
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Dokumen utang PBF yang dicicil baris ini.
     *
     * <p>Getter DESTRUKTIF: hasil {@code check(...)} ditulis balik ke field.
     * {@code check} menormalkan proksi malas Hibernate yang sudah lepas dari
     * sesinya menjadi {@code null} alih-alih membiarkan
     * {@code LazyInitializationException} meledak saat objek dibaca di luar sesi.
     * Karena itu memanggil getter ini dapat mengubah keadaan objek dan bukan
     * pembacaan murni.</p>
     *
     * <p>Relasi {@code nullable = false} — pembayaran tanpa dokumen tidak punya
     * arti: ia tidak mengurangi utang siapa pun, tidak muncul di perhitungan
     * sisa mana pun, dan uang yang keluar kehilangan lawan katanya. Perhatikan
     * bahwa cascade sengaja TIDAK disetel di sini sama sekali (berbeda dari
     * entity tetangga yang memakai PERSIST/MERGE): dokumen harus sudah ada dan
     * tersimpan sebelum pembayarannya dicatat, dan itu memang selalu benar
     * karena pemanggil memuat dokumen dari basis data untuk menguncinya.</p>
     *
     * @return dokumen utang, atau {@code null} bila proksinya sudah lepas
     */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dokumen", nullable = false)
    public ApotikPbfDokumen getDokumen() { dokumen = check(dokumen); return dokumen; }

    /**
     * Menetapkan dokumen utang yang dicicil.
     *
     * @param dokumen dokumen PBF; wajib terisi sebelum disimpan
     */
    public void setDokumen(ApotikPbfDokumen dokumen) { this.dokumen = dokumen; }

    /**
     * Cara pembayaran yang dipakai — sekaligus pembawa akun Kas/Bank jurnalnya.
     *
     * <p>Berlaku catatan getter destruktif yang sama seperti
     * {@link #getDokumen()}.</p>
     *
     * <p>Relasi ini bukan sekadar keterangan. {@code ApotikPbfPostingHelper}
     * mengambil akun kredit jurnal pembayaran dari
     * {@code caraBayar.getAkun()}: utang PBF didebit, kas/bank dikreditkan.
     * Bila cara pembayaran belum punya akun, helper tidak menebak — ia menandai
     * draf itu belum siap dengan alasan "Cara pembayaran belum mempunyai akun
     * Kas/Bank" dan tidak memposting apa pun. Sikap menolak-daripada-menebak itu
     * benar: jurnal yang mengkreditkan akun yang salah lebih sulit ditemukan
     * daripada jurnal yang tidak pernah terbentuk.</p>
     *
     * <p>{@code CaraPembayaranKoperasi} berasal dari modul koperasi dan dipakai
     * bersama; menonaktifkan sebuah cara pembayaran di sana akan menghentikan
     * pencatatan pembayaran PBF baru yang memakainya —
     * {@code pbfBayar} menolak cara pembayaran yang tidak aktif — tanpa
     * mengganggu baris yang sudah tercatat.</p>
     *
     * @return cara pembayaran, atau {@code null} bila proksinya lepas
     */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cara_bayar", nullable = false)
    public CaraPembayaranKoperasi getCaraBayar() { caraBayar = check(caraBayar); return caraBayar; }

    /**
     * Menetapkan cara pembayaran.
     *
     * @param caraBayar cara pembayaran; wajib terisi sebelum disimpan
     */
    public void setCaraBayar(CaraPembayaranKoperasi caraBayar) { this.caraBayar = caraBayar; }

    /**
     * Nominal yang dibayarkan pada kesempatan ini.
     *
     * <p>Mengembalikan {@code 0} bila kosong. Untuk kolom yang selalu
     * dijumlahkan — sisa utang dihitung sebagai total dokumen dikurangi jumlah
     * seluruh nominal — pilihan itu tepat: satu {@code null} yang lolos ke
     * penjumlahan Java akan menggagalkan perhitungan sisa utang, bukan sekadar
     * menghasilkan angka yang meleset.</p>
     *
     * <p>Kewajaran nilainya dijaga di luar entity: {@code pbfBayar} menolak
     * nominal yang tidak lebih besar dari nol dan menolak nominal yang melebihi
     * sisa utang. Entity sendiri menerima apa saja, termasuk nilai negatif yang
     * akan MENGURANGI jumlah terbayar dan dengan demikian membangkitkan kembali
     * utang yang sudah lunas. Tidak ada batasan basis data yang mencegahnya.</p>
     *
     * <p>Nominal ini pula yang menjadi nilai debit sekaligus kredit jurnalnya di
     * {@code ApotikPbfPostingHelper}, dibungkus {@code Math.abs} di sana —
     * artinya bila nilai negatif entah bagaimana tersimpan, jurnalnya akan
     * terbentuk dengan tanda yang berlawanan dari maksud barisnya. Alasan
     * tambahan untuk tidak melonggarkan penjagaan di pemanggil.</p>
     *
     * @return nominal pembayaran; {@code 0} bila kosong
     */
    @Column(name = "nominal", nullable = false)
    public Double getNominal() { return nominal == null ? Double.valueOf(0) : nominal; }

    /**
     * Menetapkan nominal pembayaran.
     *
     * <p>Menyimpan apa adanya; seluruh penjagaan kewajaran ada di
     * {@code ApotikPersediaanHelper.pbfBayar}.</p>
     *
     * @param nominal nominal pembayaran
     */
    public void setNominal(Double nominal) { this.nominal = nominal; }

    /**
     * Tanggal pembayaran.
     *
     * <p>Bukan sekadar keterangan: layar posting menjaring draf jurnal dengan
     * {@code date(b.tanggal) BETWEEN date(?) AND date(?)}, sehingga baris yang
     * tanggalnya di luar rentang yang dipilih petugas tidak akan pernah muncul
     * sebagai kandidat posting. Pembayaran bertanggal jauh di masa lalu atau
     * masa depan karena itu dapat luput dari pembukuan tanpa pesan kesalahan
     * apa pun — ia hanya tidak tampil.</p>
     *
     * @return tanggal pembayaran
     */
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal", nullable = false)
    public Date getTanggal() { return tanggal; }

    /**
     * Menetapkan tanggal pembayaran.
     *
     * @param tanggal tanggal pembayaran
     */
    public void setTanggal(Date tanggal) { this.tanggal = tanggal; }

    /**
     * Catatan bebas — nomor bukti transfer, nomor cek, dan sebagainya.
     *
     * @return keterangan, atau {@code null}
     */
    @Column(name = "keterangan", length = 500)
    public String getKeterangan() { return keterangan; }

    /**
     * Menetapkan catatan bebas.
     *
     * @param keterangan keterangan
     */
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    /**
     * Jurnal yang sudah terbentuk dari baris ini; kosong berarti belum diposting.
     *
     * <p>Berlaku catatan getter destruktif yang sama seperti
     * {@link #getDokumen()}.</p>
     *
     * <p>Ini penanda idempoten pembukuan, bukan sekadar tautan. Layar posting
     * menjaring kandidat dengan {@code posting_history IS NULL}, sehingga baris
     * yang sudah punya jurnal tidak akan terjaring lagi. Sebagai lapis kedua,
     * {@code postingSatu} memeriksa ulang di dalam transaksinya sendiri: bila
     * ternyata sudah terisi, ia me-rollback dan melaporkan berhasil tanpa
     * membentuk jurnal kedua. Dua pemeriksaan untuk hal yang sama itu disengaja
     * — yang pertama menjaga kerapian daftar, yang kedua menjaga kebenaran
     * ketika dua petugas menekan tombol posting bersamaan.</p>
     *
     * <p><b>Batas yang perlu diketahui.</b> Terisinya kolom ini tidak mengunci
     * apa pun di baris ini. {@link #setNominal(Double)} dan
     * {@link #setTanggal(Date)} tetap dapat dipanggil setelah jurnal terbentuk,
     * dan jurnalnya tidak akan ikut berubah; tidak ada pemeriksaan di mana pun
     * yang membandingkan keduanya belakangan. Untuk saat ini tidak ada jalur
     * aplikasi yang menyunting baris pembayaran setelah tersimpan, jadi keadaan
     * itu belum dapat terjadi — tetapi ia akan terjadi diam-diam pada hari
     * seseorang menambahkan formulir sunting.</p>
     *
     * @return jurnal hasil posting, atau {@code null} bila belum diposting
     */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "posting_history")
    public PostingHistory getPostingHistory() { postingHistory = check(postingHistory); return postingHistory; }

    /**
     * Menetapkan jurnal hasil posting.
     *
     * <p>Hanya dipanggil {@code ApotikPbfPostingHelper.postingSatu} sesudah
     * jurnalnya benar-benar tersimpan, di dalam transaksi yang sama. Memanggilnya
     * dari tempat lain akan menandai baris sebagai sudah-diposting padahal
     * jurnalnya tidak ada, dan baris itu tidak akan pernah terjaring lagi.</p>
     *
     * @param postingHistory jurnal hasil posting
     */
    public void setPostingHistory(PostingHistory postingHistory) { this.postingHistory = postingHistory; }

    /**
     * Nama tampil pelaku pencatatan (bayangan audit).
     *
     * @return nama pelaku, atau {@code null}
     */
    @Column(name = "oleh", length = 60) public String getOleh() { return oleh; }

    /**
     * Menetapkan nama pelaku pencatatan.
     *
     * <p>Menetapkan apa adanya, termasuk nilai kosong — berbeda dari
     * {@link ApotikBatchKonsumsi#setOleh(String)} dan
     * {@link ApotikNarkotikaLog#setOleh(String)} yang menolaknya. Untuk baris
     * pembayaran, jejak pelaku yang lebih kuat tetap tersimpan Envers di
     * {@code new_audit.apotik_pbf_pembayaran__audit}, dan pelakunya diisi
     * pemanggil dari sesi ({@code auditActor}) pada saat baris dibuat.</p>
     *
     * @param oleh nama pelaku
     */
    public void setOleh(String oleh) { this.oleh = oleh; }

    /**
     * Identitas akun pelaku pencatatan (bayangan audit).
     *
     * @return id akun pelaku, atau {@code null}
     */
    @Column(name = "oleh_id", length = 60) public String getOlehId() { return olehId; }

    /**
     * Menetapkan id akun pelaku pencatatan.
     *
     * <p>Berlaku catatan pada {@link #setOleh(String)}.</p>
     *
     * @param olehId id akun pelaku
     */
    public void setOlehId(String olehId) { this.olehId = olehId; }

    /**
     * Stempel perubahan terakhir.
     *
     * @return waktu ubah terakhir
     */
    @Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

    /**
     * Menetapkan stempel perubahan terakhir.
     *
     * @param tanggal_dirubah waktu ubah
     */
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

    /**
     * Kait JPA sebelum UPDATE: menyegarkan {@link #getTanggal_dirubah()}.
     *
     * <p>Pada pemakaian sekarang praktis tidak pernah berjalan — baris
     * pembayaran ditulis sekali lalu hanya disentuh untuk mengisi
     * {@link #setPostingHistory(PostingHistory)}, yang memang merupakan UPDATE
     * dan karena itu menggerakkan stempel ini. Itulah satu-satunya perubahan
     * yang biasa terjadi pada baris ini.</p>
     */
    @javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
