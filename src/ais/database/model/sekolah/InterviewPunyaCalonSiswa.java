package ais.database.model.sekolah;

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
import ais.ui.util.WaktuUtil;

/**
 * Penugasan seorang calon siswa ke satu sesi wawancara PSB/PPDB — baris
 * penghubung (<i>join entity</i>) antara {@link CalonSiswa} dan
 * {@link InterviewCalonSiswa}.
 *
 * <h2>Peran dalam alur wawancara PSB</h2>
 * <p>Alur wawancara Penerimaan Siswa Baru di AIS terdiri dari tiga lapis:</p>
 * <ol>
 *   <li>{@link GelombangPendaftaranPsb} — gelombang pendaftaran, pemilik teks
 *       pengumuman {@code infoSaatInterview} yang ditampilkan ke calon;</li>
 *   <li>{@link InterviewCalonSiswa} — <b>sesi</b> wawancara (nama sesi, rentang
 *       waktu, pewawancara/{@link ais.database.model.Pegawai}, platform video
 *       konferensi beserta tautannya, kapasitas ruangan, sekolah/yayasan);</li>
 *   <li><b>kelas ini</b> — <b>peserta</b> pada sesi tersebut, satu baris per
 *       pasangan (sesi, calon siswa).</li>
 * </ol>
 *
 * <h2>PENTING — entity ini BUKAN penyimpan hasil/keputusan wawancara</h2>
 * <p>Sekalipun namanya mengesankan "hasil wawancara", verifikasi menyeluruh
 * atas seluruh basis kode menunjukkan tidak ada satu pun kolom nilai, skor,
 * rekomendasi, atau status lulus/tidak-lulus di entity ini — dan tidak ada
 * entity lain di modul sekolah yang menyimpannya (pencarian
 * {@code nilaiInterview}/{@code hasilInterview}/{@code nilai_interview}
 * berakhir nihil). Yang tersimpan hanyalah:</p>
 * <ul>
 *   <li><b>penjadwalan</b> — {@code mulai}/{@code sampai} khusus per peserta;</li>
 *   <li><b>kesiapan peserta</b> — {@code siap}, dicentang oleh <i>calon siswa</i>
 *       sendiri dari portal PPDB, bukan oleh panitia;</li>
 *   <li><b>catatan bebas</b> — {@code keterangan}, juga diisi oleh calon siswa
 *       dari portal (kolom "Catatan" pada tombol "Saya Siap"), dan dapat pula
 *       diisi panitia lewat layar peserta.</li>
 * </ul>
 * <p>Keputusan seleksi PSB sendiri hidup di jalur lain
 * ({@link CalonSiswaPunyaVerifikasiParameter}, {@link VerifikasiKelengkapanCalonSiswa},
 * dan kolom status pada {@link CalonSiswa}). Jangan menambahkan kolom nilai ke
 * entity ini tanpa terlebih dahulu menyelaraskan dengan jalur tersebut.</p>
 *
 * <h2>Siapa menulis baris ini</h2>
 * <table border="1" summary="Produsen dan konsumen data">
 *   <tr><th>Titik</th><th>Operasi</th></tr>
 *   <tr>
 *     <td>{@code InterviewCalonSiswaAction.bukaFormPeserta(..)}</td>
 *     <td>panel admin "Peserta": daftar peserta satu sesi, tambah peserta
 *         berdasarkan <i>No. Registrasi</i>, hapus peserta.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code InterviewCalonSiswaAction.tampilkanInterview(..)}</td>
 *     <td>popup "Penugasan Wawancara" yang dipanggil dari layar PSB per calon:
 *         menugaskan calon ke salah satu sesi yang aktif hari ini.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code InterviewCalonSiswaAction.hapusSesi(..)}</td>
 *     <td>menghapus seluruh peserta sesi secara manual (bukan cascade JPA)
 *         sebelum menghapus sesinya.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code /WEB-INF/baru/modul/ppdb/_wawancara_service.jsp}</td>
 *     <td>portal calon siswa: {@code action=get_data} membaca jadwal &amp;
 *         tautan konferensi, {@code action=submit_siap} menulis
 *         {@code siap=true} + {@code keterangan}.</td>
 *   </tr>
 * </table>
 *
 * <h2>PERINGATAN KEAMANAN — layar pengelola nihil gerbang privilese</h2>
 * <p><b>{@code ais.action.master.sekolah.InterviewCalonSiswaAction} tidak
 * memuat satu pun pemanggilan {@code CommonPrivilages.checkPrevilages(..)}</b>
 * (terverifikasi: nol kemunculan pada seluruh berkas, sementara berkas Action
 * sejenis di paket yang sama — {@code AbsenGuruPiketAction},
 * {@code AlatTransportasiSiswaAction}, dsb. — memasang gerbang
 * CREATE/UPDATE/DELETE pada tombolnya). Konsekuensinya siapa pun yang dapat
 * membuka menu ini dapat membuat, mengubah, <i>dan menghapus</i> sesi wawancara
 * beserta seluruh pesertanya. Selain itu {@code muatDaftarSesi(..)} membangun
 * {@code Criteria} atas {@link InterviewCalonSiswa} <b>tanpa penyaring
 * sekolah/yayasan sama sekali</b>, dan pencarian calon pada tombol "Tambahkan
 * Peserta" mencari {@link CalonSiswa} berdasarkan {@code noRegistrasi}
 * tanpa penyaring tenant pula — pola yang sama dengan temuan pada
 * {@code RuangPSB} dan {@code CalonSiswaPunyaVerifikasiParameter}.</p>
 *
 * <p><b>Lebih jauh: endpoint portal {@code _wawancara_service.jsp} tidak
 * memeriksa sesi login sama sekali.</b> Identitas calon diambil mentah dari
 * parameter permintaan {@code id} lalu langsung
 * {@code hibSession.get(CalonSiswa.class, id)}; berkas dijangkau lewat
 * dispatcher {@code /ppdb?hanya_tampil_jsp=true&amp;p=ppdb&amp;s=_wawancara_service}
 * yang juga tanpa pemeriksaan sesi, sementara {@code applicationContext-security.xml}
 * memetakan {@code /**} ke {@code IS_AUTHENTICATED_ANONYMOUSLY}. Artinya
 * pengubahan {@code siap}/{@code keterangan} milik calon mana pun — lintas
 * sekolah dan yayasan — dapat dilakukan tanpa otentikasi apa pun. Rinciannya
 * dicatat pada {@link #getSiap()} dan {@link #getKeterangan()}.</p>
 *
 * <h2>Pemetaan persistence</h2>
 * <p>Tabel {@code sekolah.interview_punya_calon_siswa}, akses <b>property</b>
 * (anotasi menempel pada getter). Audit trail Hibernate Envers aktif
 * ({@code @Audited}) sehingga setiap perubahan — termasuk perubahan tak
 * disengaja yang dijelaskan pada {@link #getSiap()}/{@link #getKeterangan()} —
 * melahirkan revisi baru pada tabel audit.</p>
 *
 * <p><b>Field audit yang dideklarasikan ulang.</b> {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} sengaja diulang di kelas ini.
 * {@link ais.database.model.GeneralValueObject} <i>bukan</i> {@code @Entity}
 * maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa, sehingga
 * Hibernate tidak memetakan properti induk. Pengulangan ini <b>keharusan
 * teknis, bukan duplikasi yang perlu dibersihkan</b>.</p>
 *
 * <h2>Perbandingan dengan padanan Perguruan Tinggi</h2>
 * <p>Padanannya di jalur PT adalah
 * {@link ais.database.model.InterviewPunyaCalonMahasiswa} (tabel
 * {@code public.interview_punya_calon_mahasiswa}), yang strukturnya nyaris
 * identik. Dua perbedaan yang berarti:</p>
 * <ul>
 *   <li>versi PT memberi {@code @JoinColumn(name = "calon_mahasiswa", unique = true)}
 *       sehingga satu calon mahasiswa hanya bisa punya satu penugasan; versi
 *       sekolah <b>tidak</b> memasang {@code unique} pada {@code calon_siswa_id},
 *       sehingga satu calon siswa dapat memiliki banyak baris penugasan
 *       (lihat catatan pada {@link #getCalonSiswa()});</li>
 *   <li>{@link #toString()} versi sekolah aman terhadap {@code null}, sedangkan
 *       versi PT memanggil {@code biodataCalonMahasiswa.getNama()} tanpa
 *       penjaga.</li>
 * </ul>
 *
 * @author  Tim Pengembang AIS
 * @version 2026-07-16
 * @see     InterviewCalonSiswa
 * @see     CalonSiswa
 * @see     ais.database.model.InterviewPunyaCalonMahasiswa
 * @see     ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "interview_punya_calon_siswa")
public class InterviewPunyaCalonSiswa extends GeneralValueObject {

    /**
     * Versi serialisasi Java. Nilai tetap agar baris yang tersimpan di cache
     * terdistribusi/sesi HTTP lama tetap dapat dibaca setelah kelas berubah.
     * Jangan diubah kecuali bentuk state-nya memang tidak lagi kompatibel.
     */
    private static final long serialVersionUID = -6204712983011765432L;

    // ── Audit fields ─────────────────────────────────────────────────────

    /**
     * Kunci utama, {@code IDENTITY} (sequence PostgreSQL). Lihat
     * {@link #getId()} untuk implikasi id berurutan terhadap enumerasi.
     */
    private Long id;

    /**
     * Nama tampilan pengguna terakhir yang mengubah baris ini; diisi oleh
     * {@link ais.database.hibernate.AuditTimestampInterceptor}.
     */
    private String oleh;

    /**
     * Identitas (username/id pengguna) terakhir yang mengubah baris ini;
     * diisi oleh {@link ais.database.hibernate.AuditTimestampInterceptor}.
     */
    private String olehId;

    /**
     * Mengembalikan identitas pengguna terakhir yang mengubah baris ini.
     *
     * @return id pengguna pengubah terakhir, atau {@code null} bila belum
     *         pernah diisi (mis. baris yang lahir dari portal PPDB anonim).
     */
    public String getOlehId() { return olehId; }

    /**
     * Menetapkan identitas pengguna pengubah terakhir.
     *
     * <p><b>Non-obvious:</b> setter ini <i>menolak diam-diam</i> nilai
     * {@code null} maupun string kosong/whitespace — nilai lama dipertahankan.
     * Pola ini disengaja agar jejak audit tidak terhapus oleh alur yang
     * menyalin objek tanpa membawa konteks pengguna.</p>
     *
     * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
     */
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) { return; }
        this.olehId = olehId;
    }

    /**
     * Menetapkan nama tampilan pengguna pengubah terakhir.
     *
     * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong
     * diabaikan sehingga nilai lama tidak tertimpa.</p>
     *
     * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
     */
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) { return; }
        this.oleh = oleh;
    }

    /**
     * Mengembalikan nama tampilan pengguna terakhir yang mengubah baris ini.
     *
     * @return nama pengubah terakhir, atau {@code null} bila belum pernah diisi
     */
    public String getOleh() { return oleh; }

    /**
     * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum
     * {@code UPDATE} dikirim ke basis data, meneruskan objek ke
     * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
     * untuk memperbarui {@code oleh}/{@code olehId}/{@code tanggal_dirubah}.
     *
     * <p>Tidak pernah dipanggil langsung oleh kode aplikasi. Karena kait ini
     * hanya berlaku untuk {@code UPDATE}, baris baru mengandalkan nilai awal
     * {@code tanggal_dirubah} pada deklarasi field.</p>
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    /**
     * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server
     * ({@link WaktuUtil#getDate()}) saat objek dibuat, lalu diperbarui oleh
     * {@link #onUpdate()} pada setiap {@code UPDATE}.
     */
    private Date tanggal_dirubah = WaktuUtil.getDate();

    /**
     * Menetapkan stempel waktu perubahan terakhir.
     *
     * <p>Umumnya tidak perlu dipanggil manual — {@link #onUpdate()} sudah
     * mengurusnya. Berguna hanya pada alur impor/migrasi yang ingin
     * mempertahankan stempel waktu asal.</p>
     *
     * @param tanggal_dirubah stempel waktu baru
     */
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

    /**
     * Mengembalikan stempel waktu perubahan terakhir baris ini.
     *
     * @return waktu perubahan terakhir (tidak pernah {@code null} pada objek
     *         yang dibuat lewat konstruktor)
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() { return tanggal_dirubah; }

    /**
     * Label ringkas baris ini: nama calon siswa bila relasinya terisi, jika
     * tidak maka id-nya sebagai string.
     *
     * <p>Dipakai ZK untuk isi combobox/listitem dan oleh log. Perhatikan bahwa
     * memanggil method ini menyentuh {@code calonSiswa} secara langsung
     * (bukan lewat {@link #getCalonSiswa()}), sehingga <b>tidak</b> memicu
     * resolusi proxy lazy: bila objek sudah detached dan relasinya belum
     * pernah diinisialisasi, hasilnya dapat berupa nama dari proxy yang belum
     * termuat. Pada versi PT ({@code InterviewPunyaCalonMahasiswa}) method
     * serupa tidak punya penjaga {@code null} dan bisa melempar
     * {@code NullPointerException}; versi sekolah ini sudah aman.</p>
     *
     * @return nama calon siswa, atau id dalam bentuk string bila relasi kosong
     */
    public String toString() {
        return calonSiswa != null ? calonSiswa.getNama() : String.valueOf(id);
    }

    // ── Bidang data ───────────────────────────────────────────────────────

    /** Sesi wawancara induk tempat calon ini ditugaskan. */
    private InterviewCalonSiswa interviewCalonSiswa;

    /** Calon siswa yang ditugaskan pada sesi tersebut. */
    private CalonSiswa          calonSiswa;

    /**
     * Waktu mulai khusus peserta ini. Bila belum diisi, {@link #getMulai()}
     * mengambil waktu dari sesi induk — lihat catatan penting di sana soal
     * materialisasi nilai fallback ke kolom.
     */
    private Date    mulai;

    /**
     * Waktu selesai khusus peserta ini. Perilaku fallback sama dengan
     * {@link #mulai}; lihat {@link #getSampai()}.
     */
    private Date    sampai;

    /**
     * Penanda bahwa calon menyatakan dirinya siap diwawancara. Ditulis dari
     * portal PPDB oleh calon sendiri; lihat {@link #getSiap()}.
     */
    private Boolean siap;

    /**
     * Catatan bebas untuk penugasan ini (mis. pesan calon ke pewawancara).
     * Lihat {@link #getKeterangan()}.
     */
    private String  keterangan;

    /**
     * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
     *
     * <p>Baris baru hanya bermakna setelah {@link #setInterviewCalonSiswa}
     * dan {@link #setCalonSiswa} diisi; keduanya tidak dipaksakan pada tingkat
     * pemetaan (tidak ada {@code nullable = false}), jadi validasinya menjadi
     * tanggung jawab pemanggil.</p>
     */
    public InterviewPunyaCalonSiswa() {}

    // ── Kunci utama ───────────────────────────────────────────────────────

    /**
     * Mengembalikan kunci utama baris penugasan ini.
     *
     * <p>Strategi {@code IDENTITY} membuat id berurutan dan mudah ditebak.
     * Nilai ini ikut dikirim ke portal calon siswa sebagai
     * {@code interviewId} pada respons {@code get_data}.</p>
     *
     * @return id baris, atau {@code null} bila belum pernah disimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return this.id; }

    /**
     * Menetapkan kunci utama. Dipakai Hibernate saat memuat/menyimpan baris;
     * kode aplikasi normalnya tidak perlu memanggilnya.
     *
     * @param id kunci utama baru
     */
    public void setId(Long id) { this.id = id; }

    // ── Relasi ────────────────────────────────────────────────────────────

    /**
     * Mengembalikan sesi wawancara induk penugasan ini.
     *
     * <p>Relasi {@code LAZY}; getter memanggil
     * {@link ais.database.model.GeneralValueObject#check(Object)} lebih dahulu
     * sehingga proxy yang belum termuat tetap dapat diresolusi walau session
     * Hibernate asalnya sudah tertutup (dipakai luas oleh renderer ZK dan
     * JSP portal yang membaca entity di luar transaksi).</p>
     *
     * <p>Sesi induk adalah pemilik satu-satunya informasi tenant
     * (sekolah/yayasan) — entity ini sendiri tidak menyimpan kolom tenant apa
     * pun, sehingga setiap penyaringan lintas sekolah <b>wajib</b> melewati
     * relasi ini.</p>
     *
     * @return sesi wawancara induk, atau {@code null} bila belum ditetapkan
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_calon_siswa_id")
    public InterviewCalonSiswa getInterviewCalonSiswa() {
        interviewCalonSiswa = check(interviewCalonSiswa);
        return interviewCalonSiswa;
    }

    /**
     * Menetapkan sesi wawancara induk.
     *
     * <p>Tidak ada normalisasi {@code null}/id-kosong di sini (berbeda dengan
     * beberapa setter relasi lain di modul sekolah), jadi objek transien tanpa
     * id pun akan tersimpan lewat {@code CascadeType.PERSIST}.</p>
     *
     * <p>Dipanggil dari {@code InterviewCalonSiswaAction.bukaFormPeserta(..)}
     * (tombol "Tambahkan Peserta") dan
     * {@code InterviewCalonSiswaAction.tampilkanInterview(..)} (tombol
     * "Tugaskan ke Sesi Ini").</p>
     *
     * @param interviewCalonSiswa sesi wawancara induk
     */
    public void setInterviewCalonSiswa(InterviewCalonSiswa interviewCalonSiswa) {
        this.interviewCalonSiswa = interviewCalonSiswa;
    }

    /**
     * Mengembalikan calon siswa yang ditugaskan pada sesi ini.
     *
     * <p>Relasi {@code LAZY} dengan resolusi proxy lewat
     * {@link ais.database.model.GeneralValueObject#check(Object)}, sama seperti
     * {@link #getInterviewCalonSiswa()}.</p>
     *
     * <p><b>Non-obvious — tidak ada batasan keunikan.</b> Kolom
     * {@code calon_siswa_id} <i>tidak</i> ditandai {@code unique}, berbeda
     * dengan padanan PT ({@code @JoinColumn(name = "calon_mahasiswa", unique = true)}
     * pada {@link ais.database.model.InterviewPunyaCalonMahasiswa}). Akibatnya
     * satu calon siswa dapat memiliki beberapa baris penugasan sekaligus.
     * Konsumennya tidak menyiapkan diri untuk itu:
     * {@code InterviewCalonSiswaAction.tampilkanInterview(..)} hanya
     * <i>menampilkan label peringatan</i> "Sudah ditugaskan ke sesi …"
     * (memakai {@code setMaxResults(1)}) tanpa mencegah penugasan kedua, dan
     * portal {@code _wawancara_service.jsp} memilih baris dengan
     * {@code addOrder(Order.desc("id")).setMaxResults(1)} sehingga hanya
     * penugasan terbaru yang pernah terlihat oleh calon.</p>
     *
     * @return calon siswa peserta, atau {@code null} bila belum ditetapkan
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "calon_siswa_id")
    public CalonSiswa getCalonSiswa() {
        calonSiswa = check(calonSiswa);
        return calonSiswa;
    }

    /**
     * Menetapkan calon siswa peserta sesi ini.
     *
     * <p>Pada layar admin, calon dicari lebih dahulu berdasarkan
     * {@code noRegistrasi} yang diketik operator —
     * <b>tanpa penyaring sekolah/yayasan</b>, sehingga nomor registrasi milik
     * sekolah lain pun dapat ditugaskan ke sesi sekolah ini.</p>
     *
     * @param calonSiswa calon siswa peserta
     */
    public void setCalonSiswa(CalonSiswa calonSiswa) {
        this.calonSiswa = calonSiswa;
    }

    // ── Jadwal per-peserta (opsional, fallback ke sesi induk) ─────────────

    /**
     * Mengembalikan waktu mulai wawancara untuk peserta ini: nilai khusus
     * peserta bila ada, jika tidak maka waktu mulai sesi induk.
     *
     * <p><b>Non-obvious yang penting — nilai fallback ikut tersimpan.</b>
     * Pemetaan entity ini memakai akses <i>property</i>, sehingga Hibernate
     * membaca kolom {@code mulai} melalui getter ini, termasuk saat
     * {@code INSERT} dan saat pemeriksaan <i>dirty</i>. Konsekuensinya nilai
     * hasil fallback <b>dituliskan permanen ke kolom</b> pada penyimpanan
     * pertama. Semantik "kosong berarti ikut sesi induk" karenanya hanya
     * berlaku sampai baris tersimpan; setelah itu:</p>
     * <ul>
     *   <li>mengubah jadwal sesi induk lewat "Edit Sesi Wawancara"
     *       <b>tidak</b> merambat ke peserta yang sudah terdaftar — mereka
     *       tetap memegang waktu lama;</li>
     *   <li>karena portal PPDB menyaring dengan SQL mentah
     *       {@code date('<hari ini>') between date(mulai) and date(sampai)}
     *       atas kolom (bukan lewat getter), sesi yang dijadwal-ulang ke
     *       tanggal lain akan membuat calon melihat pesan "Jadwal wawancara
     *       Anda belum tersedia hari ini" pada hari-H;</li>
     *   <li>bila sesi induk sendiri belum punya {@code mulai}/{@code sampai},
     *       kolom peserta tetap {@code NULL} dan pembandingan SQL di atas
     *       tidak pernah bernilai benar — baris tersebut menjadi tak terlihat
     *       selamanya dari portal.</li>
     * </ul>
     *
     * @return waktu mulai efektif, atau {@code null} bila baris ini maupun
     *         sesi induknya tidak memilikinya
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getMulai() {
        return mulai != null ? mulai
                : (getInterviewCalonSiswa() == null ? null : getInterviewCalonSiswa().getMulai());
    }

    /**
     * Menetapkan waktu mulai khusus peserta ini.
     *
     * <p>Diisi dari kolom "Waktu Mulai Khusus (opsional)" pada panel Peserta.
     * Mengisi {@code null} <i>tidak</i> mengembalikan baris ke keadaan
     * "ikut sesi induk" secara permanen — lihat {@link #getMulai()}.</p>
     *
     * @param mulai waktu mulai khusus, boleh {@code null}
     */
    public void setMulai(Date mulai) { this.mulai = mulai; }

    /**
     * Mengembalikan waktu selesai wawancara untuk peserta ini: nilai khusus
     * peserta bila ada, jika tidak maka waktu selesai sesi induk.
     *
     * <p>Berlaku catatan materialisasi fallback yang sama persis dengan
     * {@link #getMulai()} — nilai turunan dari sesi induk akan tersimpan ke
     * kolom {@code sampai} pada penyimpanan pertama dan tidak lagi mengikuti
     * perubahan jadwal sesi.</p>
     *
     * @return waktu selesai efektif, atau {@code null} bila baris ini maupun
     *         sesi induknya tidak memilikinya
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getSampai() {
        return sampai != null ? sampai
                : (getInterviewCalonSiswa() == null ? null : getInterviewCalonSiswa().getSampai());
    }

    /**
     * Menetapkan waktu selesai khusus peserta ini.
     *
     * <p>Diisi dari kolom "Waktu Sampai Khusus (opsional)" pada panel Peserta.
     * Tidak ada validasi bahwa {@code sampai} berada setelah {@code mulai}
     * pada tingkat peserta (validasi semacam itu hanya ada pada form sesi).</p>
     *
     * @param sampai waktu selesai khusus, boleh {@code null}
     */
    public void setSampai(Date sampai) { this.sampai = sampai; }

    // ── Status kesiapan ───────────────────────────────────────────────────

    /**
     * Mengembalikan pernyataan kesiapan calon untuk diwawancara.
     *
     * <p>Kolom ini <b>bukan</b> keputusan panitia melainkan tombol "Saya Siap"
     * yang ditekan calon siswa sendiri di portal PPDB. Alurnya:
     * {@code _wawancara_service.jsp?action=submit_siap} memeriksa kelengkapan
     * berkas lewat
     * {@link VerifikasiKelengkapanCalonSiswa#ambilPesanGagalSebelumInterview(CalonSiswa, org.hibernate.Session)},
     * lalu menyetel {@code siap=true} dan membangkitkan tautan WhatsApp ke
     * nomor pewawancara. Operasinya idempoten: bila sudah {@code true},
     * permintaan berikutnya dijawab sukses tanpa menulis apa pun. Panel admin
     * hanya <i>menampilkan</i> nilainya (kolom "Siap" berisi "Ya"/"-") dan
     * tidak menyediakan cara mengubahnya kembali.</p>
     *
     * <p><b>Efek samping tak terduga:</b> getter menormalkan {@code null}
     * menjadi {@code false}. Karena pemetaan memakai akses property, baris
     * lama yang kolomnya masih {@code NULL} akan dianggap "kotor" pada
     * pembacaan berikutnya di dalam session dan ditulis ulang menjadi
     * {@code false} — memicu revisi Envers baru meskipun tidak ada pengguna
     * yang mengubah apa pun.</p>
     *
     * <p><b>PERINGATAN KEAMANAN.</b> Endpoint {@code submit_siap} mengambil
     * identitas calon dari parameter {@code id} tanpa memeriksa sesi login,
     * dan dispatcher {@code /ppdb?hanya_tampil_jsp=true&amp;p=ppdb&amp;s=…}
     * meneruskan permintaan apa adanya sementara Spring Security memetakan
     * {@code /**} ke {@code IS_AUTHENTICATED_ANONYMOUSLY}. Karena id
     * {@link CalonSiswa} berurutan, penanda kesiapan calon mana pun — lintas
     * sekolah dan yayasan — dapat diubah tanpa otentikasi.</p>
     *
     * @return {@code true} bila calon sudah menyatakan siap, {@code false}
     *         bila belum atau kolomnya masih {@code NULL}
     */
    public Boolean getSiap() { return siap == null ? false : siap; }

    /**
     * Menetapkan penanda kesiapan calon.
     *
     * <p>Satu-satunya pemanggil di produksi adalah cabang
     * {@code submit_siap} pada {@code _wawancara_service.jsp}, yang selalu
     * mengirim {@code true} di dalam transaksi tersendiri. Tidak ada jalur UI
     * mana pun yang mengembalikannya ke {@code false}.</p>
     *
     * @param siap penanda kesiapan; {@code null} akan dibaca sebagai
     *             {@code false} oleh {@link #getSiap()}
     */
    public void setSiap(Boolean siap) { this.siap = siap; }

    /**
     * Mengembalikan catatan bebas penugasan wawancara ini.
     *
     * <p>Dua produsen berbeda mengisi kolom yang sama: panitia lewat kolom
     * "Catatan" pada panel Peserta, dan calon siswa lewat kolom catatan pada
     * tombol "Saya Siap" di portal PPDB. Tulisan calon <b>menimpa</b> catatan
     * panitia karena {@code submit_siap} memanggil setter ini tanpa
     * menggabungkan isi lama. Nilainya dipantulkan kembali ke portal sebagai
     * field JSON {@code catatan}.</p>
     *
     * <p><b>Efek samping tak terduga:</b> sama seperti {@link #getSiap()},
     * getter menormalkan {@code null} menjadi string kosong. Pada baris lama
     * yang kolomnya {@code NULL}, pembacaan di dalam session akan menuliskan
     * {@code ''} ke basis data dan memunculkan revisi Envers "palsu".
     * Perbedaan {@code NULL} vs {@code ''} juga membuat kueri pelaporan yang
     * memakai {@code IS NULL} berperilaku tidak konsisten antar-baris.</p>
     *
     * <p><b>PERINGATAN KEAMANAN.</b> Karena {@code submit_siap} tidak
     * terotentikasi (lihat {@link #getSiap()}), teks sembarang dapat
     * dituliskan ke kolom ini pada baris milik calon mana pun. Isi kolom ini
     * ditampilkan kembali di portal maupun panel admin, jadi perlakukan
     * sebagai masukan yang tidak tepercaya.</p>
     *
     * @return catatan penugasan; string kosong (bukan {@code null}) bila belum
     *         pernah diisi
     */
    @Column(columnDefinition = "text")
    public String getKeterangan() { return keterangan == null ? "" : keterangan; }

    /**
     * Menetapkan catatan bebas penugasan ini.
     *
     * <p>Tidak ada penyaringan HTML/panjang di sisi model; pemanggil dari
     * panel admin memangkas spasi dan menyimpan {@code null} untuk isian
     * kosong, sedangkan portal PPDB hanya menyimpan bila teksnya tidak
     * kosong.</p>
     *
     * @param keterangan catatan baru, boleh {@code null}
     */
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
}
