package ais.database.model.sekolah;

import static javax.persistence.GenerationType.IDENTITY;

import java.net.URLEncoder;
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
import javax.servlet.http.HttpServletRequest;

import org.hibernate.envers.Audited;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;

import ais.common.Common;
import ais.common.RequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Sesi wawancara (interview) dalam alur Penerimaan Siswa Baru (PSB/PPDB).
 *
 * <p>Setiap rekaman mewakili satu sesi wawancara yang dapat dihadiri
 * sejumlah calon siswa ({@link InterviewPunyaCalonSiswa}). Sesi dapat
 * diselenggarakan secara luring maupun daring via platform video konferensi
 * (Jitsi, Zoom, Google Meet, BBB, Skype, WhatsApp, atau lainnya).</p>
 *
 * <p>Tabel: {@code sekolah.interview_calon_siswa}. Audit trail aktif
 * via Hibernate Envers ({@code @Audited}).</p>
 *
 * <h3>Konstanta Platform Video</h3>
 * <pre>
 *   TIDAK_AKTIF = 0  – tatap muka langsung / tidak ada konferensi video
 *   JITSI       = 1  – Jitsi Meet (tautan dibangkitkan otomatis)
 *   GOOGLE_MEET = 2  – Google Meet (tautan dari lainLink)
 *   ZOOM        = 3  – Zoom (tautan dari zoomLink)
 *   BBB         = 4  – BigBlueButton (tautan dari bbbLink)
 *   SKYPE       = 5  – Skype (tautan dari skypeLink)
 *   WA          = 6  – WhatsApp (tautan dari waLink)
 *   LAIN        = 7  – Platform lain (tautan dari lainLink)
 * </pre>
 *
 * <h2>Posisi dalam alur wawancara PSB — entity INDUK</h2>
 * <p>Alur wawancara PSB di AIS bertingkat tiga, dan kelas ini menempati
 * lapisan tengah sebagai <b>induk</b>:</p>
 * <ol>
 *   <li>{@link GelombangPendaftaranPsb} — gelombang pendaftaran; pemilik teks
 *       pengumuman {@code infoSaatInterview} yang ikut ditampilkan ke calon
 *       pada layar wawancara portal;</li>
 *   <li><b>kelas ini</b> — <b>sesi/jadwal</b> wawancara: nama sesi, rentang
 *       waktu {@code mulai}–{@code sampai}, <b>pewawancara</b>
 *       ({@link ais.database.model.Pegawai}), platform video konferensi
 *       beserta seluruh tautannya, kapasitas ruangan, tenant sekolah/yayasan;</li>
 *   <li>{@code ais.database.model.sekolah.InterviewPunyaCalonSiswa} —
 *       <b>peserta</b> pada sesi, satu baris per pasangan (sesi, calon siswa),
 *       menyimpan waktu khusus per peserta, flag {@code siap}, dan catatan.</li>
 * </ol>
 * <p>Arah kepemilikan FK ada di sisi anak: {@code InterviewPunyaCalonSiswa}
 * memegang kolom {@code interview_calon_siswa_id}. Kelas ini <b>tidak</b>
 * mendeklarasikan koleksi balik, sehingga penghapusan peserta saat sesi
 * dihapus dilakukan manual di {@code InterviewCalonSiswaAction.hapusSesi(..)}
 * (bukan cascade JPA/DDL).</p>
 *
 * <h2>Kembaran di modul Perguruan Tinggi</h2>
 * <p>Kelas ini adalah klon modul sekolah dari
 * {@code ais.database.model.InterviewCalonMahasiswa} (modul PT/PMB). Kedua
 * kelas memiliki daftar konstanta platform, kumpulan kolom tautan, dan method
 * {@code generateJitsiLink()} yang praktis identik. Perubahan pada salah satu
 * biasanya perlu dicerminkan ke yang lain, tetapi <b>tidak ada mekanisme
 * sinkronisasi otomatis</b> di antara keduanya.</p>
 *
 * <h2>PERINGATAN KEAMANAN — data sesi ini bocor lewat endpoint PRA-OTENTIKASI</h2>
 * <p><b>Terverifikasi langsung dari sisi entity induk ini (bukan sekadar dari
 * entity anak).</b> Berkas
 * {@code /WEB-INF/baru/modul/ppdb/_wawancara_service.jsp} membaca objek
 * {@code InterviewCalonSiswa} melalui
 * {@code rec.getInterviewCalonSiswa()} lalu menyalin isinya apa adanya ke
 * respons JSON. Yang dibaca dari <b>kelas ini</b> adalah:</p>
 * <table border="1" summary="Properti kelas ini yang tersaji ke pemanggil anonim">
 *   <tr><th>Properti kelas ini</th><th>Field JSON</th><th>Isi</th></tr>
 *   <tr><td>{@link #getNama()}</td><td>{@code interviewerName}</td>
 *       <td>nama sesi wawancara</td></tr>
 *   <tr><td>{@link #getPegawai()} &rarr; {@code Pegawai.getNama()}</td>
 *       <td>{@code pegawaiName}</td><td><b>nama pewawancara</b></td></tr>
 *   <tr><td>{@link #getPegawai()} &rarr; {@code CommonMedia.getUrlFotoPengguna(..)}</td>
 *       <td>{@code pegawaiPhoto}</td><td><b>URL foto pewawancara</b></td></tr>
 *   <tr><td>{@link #getPegawai()} &rarr; {@code Pegawai.ambilNoHp()}</td>
 *       <td>{@code waLink}</td>
 *       <td><b>nomor HP pribadi pewawancara</b>, tertanam pada URL
 *           {@code https://api.whatsapp.com/send?phone=...} yang dikembalikan
 *           oleh {@code action=submit_siap}</td></tr>
 *   <tr><td>{@link #getOnlineMenggunakan()}</td><td>{@code onlineMenggunakan},
 *       {@code videoPlatform}, {@code videoIconKey}</td>
 *       <td>platform konferensi yang dipakai</td></tr>
 *   <tr><td>{@link #generateJitsiLink()}, {@link #getZoomLink()},
 *       {@link #getBbbLink()}, {@link #getSkypeLink()}, {@link #getWaLink()},
 *       {@link #getLainLink()}</td>
 *       <td>{@code videoLink}</td>
 *       <td><b>tautan ruang video konferensi</b> — Jitsi/Zoom/Meet/BBB/Skype/WA</td></tr>
 * </table>
 * <p>Jawaban atas pertanyaan "apakah data pewawancara memang disimpan di
 * sini": <b>ya</b>. Nama, foto, dan nomor HP pewawancara berasal dari relasi
 * {@code pegawai_id} <i>milik kelas ini</i>, dan seluruh tautan konferensi
 * adalah kolom {@code text} <i>milik kelas ini</i>. Entity anak hanya
 * menyumbang waktu khusus, flag {@code siap}, dan catatan.</p>
 *
 * <p><b>Jalur pra-otentikasinya penuh, tanpa satu pun pemeriksaan sesi.</b>
 * Berkas layanan dijangkau lewat dispatcher
 * {@code /ppdb?hanya_tampil_jsp=true&amp;p=ppdb&amp;s=_wawancara_service}.
 * Cabang {@code hanya_tampil_jsp} pada {@code /WEB-INF/baru/ppdb.jsp} hanya
 * merangkai {@code "/WEB-INF/baru/modul/" + p + "/" + s + ".jsp"} lalu
 * meng-{@code include}-nya — <b>tanpa memeriksa sesi login sama sekali</b>.
 * Berkas layanan itu sendiri mengambil identitas calon mentah dari parameter
 * {@code id} ({@code hibSession.get(CalonSiswa.class, id)}) tanpa
 * membandingkannya dengan pengguna yang sedang masuk. Konsekuensinya:</p>
 * <ul>
 *   <li><b>BACA anonim</b> — {@code action=get_data} dengan {@code id} calon
 *       siswa mana pun (identifier sekuensial, mudah dienumerasi) membocorkan
 *       seluruh baris tabel di atas, lintas sekolah dan lintas yayasan;</li>
 *   <li><b>TULIS anonim</b> — {@code action=submit_siap} menyetel
 *       {@code siap=true} dan menimpa {@code keterangan} pada penugasan calon
 *       mana pun, lalu mengembalikan nomor HP pewawancara sebagai bonus.</li>
 * </ul>
 * <p>Tautan konferensi yang bocor bersifat <b>tanpa kata sandi ruang</b>:
 * {@link #generateJitsiLink()} membangun nama ruang deterministik dari
 * {@code contextPath} + nama sesi + {@code id}, sehingga pihak luar tidak
 * hanya bisa membacanya tetapi juga bisa menebaknya dan masuk ke ruang
 * wawancara calon siswa (anak di bawah umur) sebagai penyusup.</p>
 *
 * <h2>PERINGATAN KEAMANAN — layar pengelola sesi nihil gerbang privilese</h2>
 * <p>Terverifikasi pada
 * {@code ais.action.master.sekolah.InterviewCalonSiswaAction}: <b>nol
 * kemunculan {@code checkPrevilages}/{@code CommonPrivilages}</b> pada seluruh
 * berkas (670 baris). Tidak ada satu pun tombol yang disembunyikan berdasarkan
 * hak {@code create}/{@code update}/{@code delete}; tombol <i>Edit</i>,
 * <i>Peserta</i>, <i>Hapus</i>, <i>Simpan</i>, <i>Tambahkan Peserta</i>, dan
 * <i>Tugaskan ke Sesi Ini</i> dipasang tanpa syarat. Siapa pun yang dapat
 * membuka menu ini memperoleh CRUD penuh — termasuk
 * {@code hapusSesi(..)} yang menghapus sesi <i>beserta seluruh pesertanya</i>.</p>
 *
 * <p><b>Cakupan tenant fail-open, di kedua arah.</b></p>
 * <ul>
 *   <li>{@code muatDaftarSesi(..)} membangun {@code Criteria} atas kelas ini
 *       <b>tanpa penyaring {@code sekolah}/{@code yayasan} sama sekali</b> —
 *       daftar sesi yang tampil adalah seluruh instalasi, bukan sekolah
 *       pengguna;</li>
 *   <li>{@code tampilkanInterview(..)} memilih "sesi aktif hari ini" juga
 *       tanpa penyaring tenant — calon siswa sekolah A bisa ditugaskan ke sesi
 *       milik sekolah B;</li>
 *   <li>pencarian calon pada tombol "Tambahkan Peserta" mencari
 *       {@link CalonSiswa} berdasarkan {@code noRegistrasi} tanpa penyaring
 *       tenant;</li>
 *   <li>combobox pewawancara memuat {@code Pegawai} tanpa batas sekolah/yayasan
 *       (hanya {@code setMaxResults(500)}), begitu pula combobox gelombang PSB;</li>
 *   <li>saat menyimpan, tenant hanya ditempelkan <i>bila tersedia</i>
 *       ({@code if (skolah != null) data.setSekolah(skolah);}). Untuk pengguna
 *       yang {@code SekolahUtil.getSekolah()}-nya {@code null}, baris tersimpan
 *       <b>tanpa tenant</b> dan selanjutnya terlihat dari semua sekolah — pola
 *       fail-open yang sama dengan temuan pada modul deposit dan asrama.</li>
 * </ul>
 *
 * <h2>Getter dengan efek tulis-balik (write-through)</h2>
 * <p>Pemetaan kelas ini memakai <b>akses property</b> (anotasi menempel pada
 * getter), sehingga Hibernate membaca nilai yang akan di-{@code INSERT}/
 * {@code UPDATE} melalui getter — bukan langsung dari field. Getter yang
 * "membetulkan" nilai {@code null} karena itu bukan sekadar kenyamanan baca:
 * nilai penggantinya benar-benar tertulis ke basis data pada flush berikutnya,
 * dan tercatat sebagai revisi Envers. Yang berperilaku demikian di kelas ini:</p>
 * <ul>
 *   <li>{@link #getTahunAjaran()} — {@code null} &rarr; tahun akademik
 *       <b>berjalan</b>. Akibatnya kolom {@code tahun_ajaran} praktis tidak
 *       bisa dikosongkan, dan sesi lama yang tersentuh ikut ter-cap ulang;</li>
 *   <li>{@link #getOnlineMenggunakan()} — {@code null} &rarr; {@link #TIDAK_AKTIF}
 *       (satu-satunya yang menugaskan ulang <i>field</i>-nya secara eksplisit);</li>
 *   <li>{@link #getAktif()} — {@code null} &rarr; {@code true};</li>
 *   <li>{@link #getKeterangan()} — {@code null} &rarr; string kosong, sehingga
 *       kolom berisi {@code ''} alih-alih {@code NULL};</li>
 *   <li>{@link #getKapasitasRuangan()} — {@code null} &rarr; {@code 0};</li>
 *   <li>{@link #getNama()} — nilai ter-{@code trim()};</li>
 *   <li>{@link #getYayasan()} — <b>menimpa</b> {@code yayasan} dengan yayasan
 *       milik {@link #getSekolah()} setiap kali dibaca (lihat catatan pada
 *       method tersebut).</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Field audit yang dideklarasikan ulang</b> — {@code id}, {@code oleh},
 *       {@code olehId}, {@code tanggal_dirubah} beserta aksesornya dan hook
 *       {@link #onUpdate()};</li>
 *   <li><b>Konstanta platform</b> — {@link #TIDAK_AKTIF} … {@link #LAIN};</li>
 *   <li><b>Kunci utama</b> — {@link #getId()}/{@link #setId(Long)};</li>
 *   <li><b>Bidang dasar</b> — nama, tahun ajaran, rentang waktu, platform,
 *       lima kolom tautan, flag aktif, keterangan, kapasitas ruangan;</li>
 *   <li><b>Relasi {@code @ManyToOne}</b> — {@link #getPegawai()},
 *       {@link #getGelombangPendaftaranPsb()}, {@link #getPenjurusanSekolah()},
 *       {@link #getSekolah()}, {@link #getYayasan()};</li>
 *   <li><b>Metode bantu</b> — {@link #generateJitsiLink()} dan
 *       {@link #createVideoConrefrence(InterviewCalonSiswa, Component, boolean, boolean, EventListener)}.</li>
 * </ol>
 *
 * <h2>Catatan pewarisan — {@link ais.database.model.GeneralValueObject}</h2>
 * <p>Induk kelas ini <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass}, melainkan POJO abstrak biasa. Hibernate karena itu
 * <b>tidak</b> memetakan properti apa pun milik induk. Pendeklarasian ulang
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di
 * kelas ini <b>bukan duplikasi yang keliru</b>, melainkan keharusan teknis agar
 * kolom-kolom itu punya pemetaan sama sekali. Jangan "membersihkan"-nya.
 * Layanan statis yang tetap diwarisi dan dipakai di sini adalah
 * {@link ais.database.model.GeneralValueObject#check(Object)} — resolusi proxy
 * lazy yang tidak pernah melempar exception dan tidak pernah mengembalikan
 * {@code null} untuk argumen non-null.</p>
 *
 * @author  Tim Pengembang AIS
 * @version 2026-07-16
 * @see     InterviewPunyaCalonSiswa
 * @see     GelombangPendaftaranPsb
 * @see     ais.database.model.InterviewCalonMahasiswa
 * @see     ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "interview_calon_siswa")
public class InterviewCalonSiswa extends GeneralValueObject {

    /** Versi serialisasi. Jangan diubah tanpa alasan kuat: nilai yang berbeda
     *  membuat objek sesi ZK hasil serialisasi versi lama gagal dipulihkan. */
    private static final long serialVersionUID = 3812946710234098701L;

    // ── Audit fields ─────────────────────────────────────────────────────

    /**
     * Kunci utama, kolom {@code id}. Dideklarasikan ulang karena
     * {@link GeneralValueObject} bukan {@code @MappedSuperclass}.
     */
    private Long id;

    /** Nama pengguna terakhir yang mengubah baris ini (kolom {@code oleh}). */
    private String oleh;

    /** Identifier pengguna terakhir yang mengubah baris ini (kolom {@code olehId}). */
    private String olehId;

    /**
     * Identifier pengguna terakhir yang mengubah baris ini.
     *
     * @return isi kolom {@code olehId}, boleh {@code null} untuk baris lama
     */
    public String getOlehId() { return olehId; }

    /**
     * Menyetel identifier pengguna pengubah.
     *
     * <p><b>Non-obvious:</b> setter ini <b>mengabaikan</b> nilai {@code null}
     * maupun string kosong/spasi — nilai lama dipertahankan. Perilaku ini
     * disengaja agar jejak audit tidak terhapus oleh pemanggil yang tidak
     * membawa konteks pengguna (mis. thread batch, callback, atau endpoint
     * JSP). Konsekuensinya: kolom ini <b>tidak bisa dikosongkan</b> lewat
     * setter.</p>
     *
     * @param olehId identifier pengguna; {@code null}/kosong diabaikan
     */
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) { return; }
        this.olehId = olehId;
    }

    /**
     * Menyetel nama pengguna pengubah.
     *
     * <p>Sama seperti {@link #setOlehId(String)}: {@code null} dan string
     * kosong diabaikan, sehingga nilai lama tidak pernah terhapus.</p>
     *
     * @param oleh nama pengguna; {@code null}/kosong diabaikan
     */
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) { return; }
        this.oleh = oleh;
    }

    /**
     * Nama pengguna terakhir yang mengubah baris ini.
     *
     * @return isi kolom {@code oleh}, boleh {@code null} untuk baris lama
     */
    public String getOleh() { return oleh; }

    /**
     * Hook JPA {@code @PreUpdate}: mengisi jejak audit tepat sebelum
     * {@code UPDATE} dieksekusi.
     *
     * <p>Mendelegasikan ke
     * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang
     * menyetel {@code oleh}/{@code olehId} dari konteks pengguna aktif dan
     * memperbarui {@code tanggal_dirubah}. Karena hanya terpasang pada
     * {@code @PreUpdate} (bukan {@code @PrePersist}), baris yang baru
     * di-{@code INSERT} bergantung pada nilai default field, bukan pada hook
     * ini.</p>
     *
     * <p><b>Peringatan:</b> hook ini hanya berjalan untuk operasi lewat
     * session Hibernate. Operasi massal (HQL/native bulk update) melewatinya —
     * dan juga melewati Envers.</p>
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    /**
     * Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek
     * dibuat, sehingga baris baru selalu punya nilai walau hook
     * {@link #onUpdate()} belum pernah berjalan.
     */
    private Date tanggal_dirubah = WaktuUtil.getDate();

    /**
     * Menyetel cap waktu perubahan terakhir.
     *
     * <p>Umumnya tidak dipanggil manual — {@link #onUpdate()} yang mengisinya.
     * Berbeda dari {@link #setOleh(String)}, setter ini menerima {@code null}
     * apa adanya.</p>
     *
     * @param tanggal_dirubah cap waktu baru; boleh {@code null}
     */
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

    /**
     * Cap waktu perubahan terakhir.
     *
     * <p>Tanpa {@code @Column}, sehingga nama kolom jatuh ke
     * {@code ais.database.hibernate.MyNamingStrategy} (turunan
     * {@code DefaultNamingStrategy}: nama kolom = nama properti apa adanya)
     * &mdash; kolom {@code tanggal_dirubah}.</p>
     *
     * @return cap waktu perubahan terakhir
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() { return tanggal_dirubah; }

    /**
     * Representasi teks singkat: {@code "<id>-<nama>"}.
     *
     * <p>Membaca field {@code nama} <b>langsung</b> (bukan lewat
     * {@link #getNama()}), sehingga tidak ter-{@code trim} dan bisa berisi
     * {@code null} — hasilnya bisa berupa {@code "12-null"}. Dipakai antara
     * lain oleh label debug dan oleh komponen ZK yang mem-{@code toString()}
     * nilai {@code Listitem}.</p>
     *
     * @return {@code id} dan nama sesi dipisah tanda hubung
     */
    public String toString() { return id + "-" + nama; }

    // ── Konstanta platform video konferensi ───────────────────────────────

    /** Platform: tidak ada konferensi video — wawancara tatap muka langsung. */
    public static final Integer TIDAK_AKTIF  = 0;
    /** Platform: Jitsi Meet. Tautan <b>dibangkitkan otomatis</b> oleh
     *  {@link #generateJitsiLink()}; tidak ada kolom tautan tersendiri. */
    public static final Integer JITSI        = 1;
    /** Platform: Google Meet. Tautan diambil dari {@link #getLainLink()}
     *  (berbagi kolom dengan {@link #LAIN} — tidak ada kolom {@code meetLink}). */
    public static final Integer GOOGLE_MEET  = 2;
    /** Platform: Zoom. Tautan diambil dari {@link #getZoomLink()}. */
    public static final Integer ZOOM         = 3;
    /** Platform: BigBlueButton. Tautan diambil dari {@link #getBbbLink()}. */
    public static final Integer BBB          = 4;
    /** Platform: Skype. Tautan diambil dari {@link #getSkypeLink()}. */
    public static final Integer SKYPE        = 5;
    /** Platform: WhatsApp. Tautan diambil dari {@link #getWaLink()}. */
    public static final Integer WA           = 6;
    /** Platform: lainnya. Tautan diambil dari {@link #getLainLink()}
     *  (kolom yang sama dengan {@link #GOOGLE_MEET}). */
    public static final Integer LAIN         = 7;

    // ── Bidang data ───────────────────────────────────────────────────────

    /** Nama sesi wawancara, kolom {@code nama} ({@code NOT NULL}, 150 karakter). */
    private String  nama;
    /** Tahun ajaran sesi, kolom {@code tahun_ajaran} (9 karakter, mis. {@code 2026/2027}). */
    private String  tahunAjaran;
    /** Awal rentang waktu sesi, kolom {@code mulai}. */
    private Date    mulai;
    /** Akhir rentang waktu sesi, kolom {@code sampai}. */
    private Date    sampai;
    /** Kode platform konferensi; lihat konstanta {@link #TIDAK_AKTIF}…{@link #LAIN}. */
    private Integer onlineMenggunakan;
    /** Tautan ruang Zoom (kolom {@code text}). */
    private String  zoomLink;
    /** Tautan ruang BigBlueButton (kolom {@code text}). */
    private String  bbbLink;
    /** Tautan/handle Skype (kolom {@code text}). */
    private String  skypeLink;
    /** Tautan atau nomor WhatsApp (kolom {@code text}). */
    private String  waLink;
    /** Tautan untuk Google Meet maupun platform "lainnya" (kolom {@code text}). */
    private String  lainLink;
    /** Flag aktif. Tidak pernah dibaca/ditulis oleh UI mana pun (lihat {@link #getAktif()}). */
    private Boolean aktif;
    /** Keterangan bebas untuk sesi (kolom {@code text}). */
    private String  keterangan;
    /** Kapasitas ruangan sesi — hanya dicatat, tidak pernah ditegakkan
     *  (lihat {@link #getKapasitasRuangan()}). */
    private Integer kapasitasRuangan;

    /** Pewawancara yang bertugas pada sesi ini; kolom {@code pegawai_id}. */
    private Pegawai              pegawai;
    /** Gelombang PSB tempat sesi ini bernaung; kolom {@code gelombang_pendaftaran_psb_id}. */
    private GelombangPendaftaranPsb gelombangPendaftaranPsb;
    /** Penjurusan sasaran sesi; kolom {@code penjurusan_sekolah_id} (tidak terpakai UI). */
    private PenjurusanSekolah    penjurusanSekolah;
    /** Tenant sekolah pemilik sesi; kolom {@code sekolah_id}. */
    private Sekolah              sekolah;
    /** Tenant yayasan pemilik sesi; kolom {@code yayasan_id}. */
    private Yayasan              yayasan;

    /**
     * Konstruktor tanpa argumen — wajib bagi Hibernate.
     *
     * <p>Tidak menginisialisasi apa pun selain default field. Nilai turunan
     * (tahun ajaran berjalan, platform {@link #TIDAK_AKTIF}, kapasitas 0,
     * {@code aktif=true}) baru muncul saat getter terkait dipanggil — lihat
     * bagian "getter dengan efek tulis-balik" pada Javadoc kelas.</p>
     */
    public InterviewCalonSiswa() {}

    // ── Kunci utama ───────────────────────────────────────────────────────

    /**
     * Kunci utama sesi wawancara.
     *
     * <p>Nilai dihasilkan basis data ({@code IDENTITY}); {@code insertable=false}
     * membuat kolom ini tidak pernah ikut dalam {@code INSERT}. Bernilai
     * {@code null} untuk objek yang belum tersimpan.</p>
     *
     * <p><b>Non-obvious:</b> nilai ini ikut membentuk nama ruang Jitsi di
     * {@link #generateJitsiLink()}, dan karena identifier bersifat sekuensial
     * maka nama ruang pun mudah ditebak.</p>
     *
     * @return identifier baris, atau {@code null} bila belum tersimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return this.id; }

    /**
     * Menyetel kunci utama. Hanya untuk Hibernate dan kode uji; kode aplikasi
     * tidak boleh memanggilnya pada objek yang sudah tersimpan.
     *
     * @param id identifier baris
     */
    public void setId(Long id) { this.id = id; }

    // ── Bidang dasar ──────────────────────────────────────────────────────

    /**
     * Nama sesi wawancara (mis. "Wawancara Gelombang 1 — Sesi Pagi").
     *
     * <p><b>Efek samping:</b> mengembalikan nilai yang sudah di-{@code trim()}.
     * Karena pemetaan memakai akses property, hasil trim inilah yang tertulis
     * ke kolom pada flush berikutnya — spasi tepi tidak pernah bertahan.</p>
     *
     * <p><b>Konsumen penting:</b> nilai ini (a) tersaji ke pemanggil
     * <i>anonim</i> sebagai field JSON {@code interviewerName} pada
     * {@code _wawancara_service.jsp}, dan (b) menjadi bagian nama ruang Jitsi
     * di {@link #generateJitsiLink()} — mengganti nama sesi berarti
     * <b>mengganti ruang konferensi</b>, sehingga peserta yang sudah menyimpan
     * tautan lama akan masuk ke ruang kosong.</p>
     *
     * @return nama sesi ter-trim, atau {@code null} bila belum diisi
     */
    @Column(name = "nama", nullable = false, length = 150)
    public String getNama() { return this.nama == null ? null : this.nama.trim(); }

    /**
     * Menyetel nama sesi. Kolom {@code NOT NULL}: menyimpan objek dengan nama
     * {@code null} akan gagal di tingkat basis data. Validasi "tidak boleh
     * kosong" ditegakkan di {@code InterviewCalonSiswaAction.bukaFormSesi(..)}.
     *
     * @param nama nama sesi
     */
    public void setNama(String nama) { this.nama = nama; }

    /**
     * Tahun ajaran sesi (format {@code 2026/2027}).
     *
     * <p><b>Efek samping — tulis-balik:</b> bila field bernilai {@code null},
     * getter mengembalikan {@code Common.getCurrentTahunAkademik()}. Karena
     * pemetaan memakai akses property, nilai substitusi itu benar-benar
     * di-{@code UPDATE} ke kolom {@code tahun_ajaran} pada flush berikutnya.
     * Akibat praktisnya:</p>
     * <ul>
     *   <li>kolom ini <b>tidak dapat dikosongkan</b>; layar admin yang
     *       menyetelnya ke {@code null} saat kotak isian dibiarkan kosong
     *       ({@code data.setTahunAjaran(ta.isEmpty() ? null : ta)}) tetap
     *       menghasilkan tahun berjalan di basis data;</li>
     *   <li>sesi lama ber-{@code tahun_ajaran} {@code NULL} yang tersentuh
     *       operasi apa pun akan ter-cap dengan tahun akademik <i>saat ini</i>,
     *       bukan tahun aslinya — dan perubahan itu tercatat sebagai revisi
     *       Envers seolah-olah disengaja.</li>
     * </ul>
     * <p>Nilai yang sudah terisi tidak pernah ditimpa.</p>
     *
     * @return tahun ajaran tersimpan, atau tahun akademik berjalan bila kosong
     */
    @Column(name = "tahun_ajaran", length = 9)
    public String getTahunAjaran() {
        return tahunAjaran == null ? Common.getCurrentTahunAkademik() : tahunAjaran;
    }

    /**
     * Menyetel tahun ajaran sesi.
     *
     * <p>Menyetel {@code null} <b>tidak</b> mengosongkan kolom secara efektif —
     * lihat {@link #getTahunAjaran()}.</p>
     *
     * @param tahunAjaran tahun ajaran, mis. {@code "2026/2027"}
     */
    public void setTahunAjaran(String tahunAjaran) { this.tahunAjaran = tahunAjaran; }

    /**
     * Awal rentang waktu sesi (tanggal <i>dan</i> jam).
     *
     * <p>Dipakai sebagai batas bawah pemilihan "sesi aktif hari ini" pada
     * {@code InterviewCalonSiswaAction.tampilkanInterview(..)} dan pada
     * pengurutan daftar sesi. Perlu dicatat bahwa portal calon siswa
     * ({@code _wawancara_service.jsp}) menampilkan waktu dari <i>entity
     * peserta</i> ({@code InterviewPunyaCalonSiswa.getMulai()}) dan
     * <b>tidak</b> jatuh kembali ke nilai sesi bila waktu khusus peserta
     * kosong — layar calon lalu menampilkan {@code "-"} walau sesinya sendiri
     * berjadwal jelas.</p>
     *
     * @return waktu mulai sesi, atau {@code null} bila belum dijadwalkan
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "mulai")
    public Date getMulai() { return mulai; }

    /**
     * Menyetel awal rentang waktu sesi.
     *
     * @param mulai waktu mulai; validasi urutan terhadap {@link #getSampai()}
     *              dilakukan di layar, bukan di sini
     */
    public void setMulai(Date mulai) { this.mulai = mulai; }

    /**
     * Akhir rentang waktu sesi (tanggal <i>dan</i> jam).
     *
     * @return waktu selesai sesi, atau {@code null} bila belum dijadwalkan
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "sampai")
    public Date getSampai() { return sampai; }

    /**
     * Menyetel akhir rentang waktu sesi.
     *
     * <p>Layar admin menolak nilai yang lebih awal dari {@link #getMulai()},
     * tetapi entity ini <b>tidak</b> menegakkan aturan tersebut — pemanggil
     * lain (mis. impor atau skrip) bisa menyimpan rentang terbalik.</p>
     *
     * @param sampai waktu selesai
     */
    public void setSampai(Date sampai) { this.sampai = sampai; }

    /**
     * Kode platform video konferensi yang dipakai sesi ini.
     *
     * <p><b>Efek samping — tulis-balik:</b> bila field {@code null}, getter
     * <b>menugaskan ulang field</b> ke {@link #TIDAK_AKTIF} lalu
     * mengembalikannya. Ini satu-satunya getter di kelas ini yang benar-benar
     * memutasi field (yang lain hanya mengembalikan nilai substitusi), sehingga
     * efeknya terlihat pula oleh pembaca berikutnya dalam objek yang sama, dan
     * nilai {@code 0} tertulis ke basis data pada flush.</p>
     *
     * <p>Tanpa {@code @Column}, sehingga nama kolom jatuh ke
     * {@code MyNamingStrategy} — kolom {@code onlineMenggunakan} apa adanya.</p>
     *
     * <p>Bandingkan hasilnya dengan konstanta memakai {@code equals(..)}, bukan
     * {@code ==}: nilainya bertipe {@code Integer} sehingga perbandingan
     * referensi hanya kebetulan benar untuk nilai di dalam cache
     * {@code Integer} (-128..127). Seluruh pemanggil yang ada sudah memakai
     * pola {@code JITSI.equals(ics.getOnlineMenggunakan())}.</p>
     *
     * @return kode platform, tidak pernah {@code null}
     */
    public Integer getOnlineMenggunakan() {
        if (onlineMenggunakan == null) { onlineMenggunakan = TIDAK_AKTIF; }
        return onlineMenggunakan;
    }

    /**
     * Menyetel kode platform video konferensi.
     *
     * <p>Tidak memvalidasi rentang: nilai di luar 0–7 diterima dan akan membuat
     * seluruh cabang {@link #createVideoConrefrence(InterviewCalonSiswa,
     * Component, boolean, boolean, EventListener)} meleset, sehingga tombol
     * yang dihasilkan tidak memiliki event listener sama sekali.</p>
     *
     * @param onlineMenggunakan kode platform; lihat konstanta kelas ini
     */
    public void setOnlineMenggunakan(Integer onlineMenggunakan) {
        this.onlineMenggunakan = onlineMenggunakan;
    }

    /**
     * Tautan ruang Zoom untuk sesi ini.
     *
     * <p>Dinormalkan saat dibaca: string kosong atau hanya spasi dikembalikan
     * sebagai {@code null}, selain itu di-{@code trim()}. Karena akses property,
     * normalisasi itu ikut tersimpan pada flush berikutnya.</p>
     *
     * <p><b>Keamanan:</b> nilai ini tersaji ke pemanggil <i>anonim</i> sebagai
     * field JSON {@code videoLink} bila {@link #getOnlineMenggunakan()} bernilai
     * {@link #ZOOM} — lihat peringatan pada Javadoc kelas.</p>
     *
     * @return tautan Zoom ter-trim, atau {@code null} bila kosong
     */
    @Column(columnDefinition = "text")
    public String getZoomLink() {
        return zoomLink == null || zoomLink.trim().isEmpty() ? null : zoomLink.trim();
    }

    /**
     * Menyetel tautan ruang Zoom.
     *
     * @param zoomLink URL ruang Zoom; boleh {@code null}
     */
    public void setZoomLink(String zoomLink) { this.zoomLink = zoomLink; }

    /**
     * Tautan ruang BigBlueButton untuk sesi ini.
     *
     * <p>Normalisasi dan implikasi keamanannya sama dengan
     * {@link #getZoomLink()}; dipakai bila platform bernilai {@link #BBB}.</p>
     *
     * @return tautan BBB ter-trim, atau {@code null} bila kosong
     */
    @Column(columnDefinition = "text")
    public String getBbbLink() {
        return bbbLink == null || bbbLink.trim().isEmpty() ? null : bbbLink.trim();
    }

    /**
     * Menyetel tautan ruang BigBlueButton.
     *
     * @param bbbLink URL ruang BBB; boleh {@code null}
     */
    public void setBbbLink(String bbbLink) { this.bbbLink = bbbLink; }

    /**
     * Tautan atau handle Skype untuk sesi ini.
     *
     * <p>Normalisasi dan implikasi keamanannya sama dengan
     * {@link #getZoomLink()}; dipakai bila platform bernilai {@link #SKYPE}.</p>
     *
     * @return tautan Skype ter-trim, atau {@code null} bila kosong
     */
    @Column(columnDefinition = "text")
    public String getSkypeLink() {
        return skypeLink == null || skypeLink.trim().isEmpty() ? null : skypeLink.trim();
    }

    /**
     * Menyetel tautan/handle Skype.
     *
     * @param skypeLink URL atau handle Skype; boleh {@code null}
     */
    public void setSkypeLink(String skypeLink) { this.skypeLink = skypeLink; }

    /**
     * Tautan atau nomor WhatsApp untuk sesi ini.
     *
     * <p>Label pada layar admin berbunyi "No. WhatsApp / Link WA", sehingga
     * kolom ini dalam praktiknya bisa berisi <b>nomor telepon</b> — bukan hanya
     * URL. Nilai dipakai apa adanya sebagai {@code url} pada
     * {@code popupCenter(..)}/{@code sendRedirect(..)}, jadi nomor telanjang
     * akan diperlakukan sebagai URL relatif dan gagal membuka WhatsApp.</p>
     *
     * <p><b>Keamanan:</b> tersaji ke pemanggil anonim sebagai {@code videoLink}
     * bila platform bernilai {@link #WA}. Berbeda dari nomor HP pewawancara
     * yang bocor lewat {@code submit_siap}, nilai ini bocor pada operasi
     * <i>baca</i> {@code get_data} saja.</p>
     *
     * @return tautan/nomor WhatsApp ter-trim, atau {@code null} bila kosong
     */
    @Column(columnDefinition = "text")
    public String getWaLink() {
        return waLink == null || waLink.trim().isEmpty() ? null : waLink.trim();
    }

    /**
     * Menyetel tautan/nomor WhatsApp.
     *
     * @param waLink URL {@code https://wa.me/...} atau nomor telepon; boleh {@code null}
     */
    public void setWaLink(String waLink) { this.waLink = waLink; }

    /**
     * Tautan konferensi untuk platform {@link #GOOGLE_MEET} <b>maupun</b>
     * {@link #LAIN}.
     *
     * <p><b>Non-obvious:</b> kedua platform berbagi satu kolom yang sama —
     * tidak ada {@code meetLink} tersendiri. Karena itu mengubah platform dari
     * Google Meet ke "Lainnya" (atau sebaliknya) <b>tidak</b> memerlukan
     * pengisian ulang tautan, tetapi juga berarti kedua konfigurasi tidak dapat
     * disimpan berdampingan.</p>
     *
     * @return tautan ter-trim, atau {@code null} bila kosong
     */
    @Column(columnDefinition = "text")
    public String getLainLink() {
        return lainLink == null || lainLink.trim().isEmpty() ? null : lainLink.trim();
    }

    /**
     * Menyetel tautan untuk Google Meet / platform lainnya.
     *
     * @param lainLink URL konferensi; boleh {@code null}
     */
    public void setLainLink(String lainLink) { this.lainLink = lainLink; }

    /**
     * Flag aktif sesi.
     *
     * <p><b>Observasi mentah (dilaporkan tanpa kesimpulan):</b> penelusuran
     * seluruh basis kode tidak menemukan satu pun pembaca maupun penulis
     * properti ini di luar kelas ini sendiri — layar admin
     * ({@code InterviewCalonSiswaAction}) tidak menampilkan kotak centangnya,
     * dan tidak ada query yang menyaring berdasarkan kolom ini. Nilai
     * {@code true} yang dikembalikan saat field {@code null} tetap tertulis ke
     * basis data pada flush karena pemetaan memakai akses property.</p>
     *
     * <p>Tanpa {@code @Column}, sehingga nama kolom jatuh ke
     * {@code MyNamingStrategy} — kolom {@code aktif}.</p>
     *
     * @return {@code true} bila field {@code null} atau bernilai {@code true}
     */
    public Boolean getAktif() { return aktif == null ? true : aktif; }

    /**
     * Menyetel flag aktif sesi.
     *
     * @param aktif nilai flag; {@code null} akan dibaca kembali sebagai {@code true}
     */
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    /**
     * Keterangan bebas untuk sesi (catatan panitia).
     *
     * <p><b>Efek samping — tulis-balik:</b> {@code null} dikembalikan sebagai
     * string kosong, sehingga kolom {@code keterangan} menyimpan {@code ''}
     * alih-alih {@code NULL}. Layar admin sudah menyetel {@code null} untuk
     * isian kosong, tetapi getter ini membatalkannya. Konsekuensi praktis:
     * predikat SQL {@code keterangan IS NULL} tidak pernah cocok untuk baris
     * yang pernah tersimpan lewat aplikasi.</p>
     *
     * <p>Isi kolom ini <b>tidak</b> ikut tersaji ke portal calon siswa —
     * catatan yang tampil di sana berasal dari
     * {@code InterviewPunyaCalonSiswa.getKeterangan()}, entity yang berbeda.</p>
     *
     * @return keterangan, atau string kosong bila belum diisi (tidak pernah {@code null})
     */
    @Column(columnDefinition = "text")
    public String getKeterangan() { return keterangan == null ? "" : keterangan; }

    /**
     * Menyetel keterangan sesi.
     *
     * @param keterangan teks bebas; {@code null} akan dibaca kembali sebagai string kosong
     */
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    /**
     * Kapasitas ruangan tempat sesi diselenggarakan.
     *
     * <p><b>Peringatan — nilai ini hanya dicatat, tidak pernah ditegakkan.</b>
     * Verifikasi menyeluruh atas pemanggil menunjukkan kolom ini hanya dibaca
     * dan ditulis oleh formulir sesi
     * ({@code InterviewCalonSiswaAction.bukaFormSesi(..)}). Kedua jalur
     * penambahan peserta — tombol "Tambahkan Peserta" pada
     * {@code bukaFormPeserta(..)} dan popup penugasan
     * {@code tampilkanInterview(..)} — <b>tidak</b> menghitung jumlah
     * {@code InterviewPunyaCalonSiswa} yang sudah ada, sehingga sebuah sesi
     * berkapasitas 5 dapat diisi 50 peserta tanpa peringatan apa pun.
     * Bandingkan dengan {@code RuangPSB}/{@code KelasSiswaPSB} yang benar-benar
     * memeriksa kapasitasnya sebelum menambah isi.</p>
     *
     * <p><b>Efek samping — tulis-balik:</b> {@code null} dikembalikan sebagai
     * {@code 0}, dan nilai {@code 0} itulah yang tersimpan pada flush.</p>
     *
     * @return kapasitas ruangan, atau {@code 0} bila belum diisi (tidak pernah {@code null})
     */
    @Column(name = "kapasitas_ruangan")
    public Integer getKapasitasRuangan() { return kapasitasRuangan == null ? 0 : kapasitasRuangan; }

    /**
     * Menyetel kapasitas ruangan sesi.
     *
     * @param kapasitasRuangan jumlah kursi; {@code null} akan dibaca kembali sebagai {@code 0}
     */
    public void setKapasitasRuangan(Integer kapasitasRuangan) { this.kapasitasRuangan = kapasitasRuangan; }

    // ── Relasi ────────────────────────────────────────────────────────────

    /**
     * Pewawancara yang bertugas pada sesi ini.
     *
     * <p>Relasi {@code @ManyToOne} lazy ke {@link Pegawai} lewat kolom
     * {@code pegawai_id}. Pemanggilan {@code check(..)} (warisan
     * {@link ais.database.model.GeneralValueObject}) meresolusi proxy lazy
     * secara aman: tidak pernah melempar exception dan tidak pernah
     * mengembalikan {@code null} untuk nilai non-null, sehingga getter ini aman
     * dipanggil dari objek yang sudah <i>detached</i> (mis. dari thread JSP).</p>
     *
     * <p><b>Inilah sumber seluruh data pewawancara yang bocor lewat endpoint
     * pra-otentikasi</b> {@code /ppdb?hanya_tampil_jsp=true&amp;p=ppdb&amp;s=_wawancara_service}:
     * {@code Pegawai.getNama()} menjadi field JSON {@code pegawaiName},
     * {@code CommonMedia.getUrlFotoPengguna(new Tbmuser(pegawai))} menjadi
     * {@code pegawaiPhoto}, dan {@code Pegawai.ambilNoHp()} — <b>nomor telepon
     * pribadi pewawancara</b> — ditanam ke dalam URL WhatsApp yang dikembalikan
     * oleh {@code action=submit_siap}. Tidak ada pemeriksaan sesi login di
     * jalur mana pun. Rincian lengkap ada pada Javadoc kelas.</p>
     *
     * <p><b>Efek samping:</b> menugaskan ulang field {@code pegawai} dengan
     * hasil resolusi (bisa instance berbeda dari sebelumnya).</p>
     *
     * @return pewawancara sesi, atau {@code null} bila belum ditetapkan
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "pegawai_id")
    public Pegawai getPegawai() {
        pegawai = check(pegawai);
        return pegawai;
    }

    /**
     * Menyetel pewawancara sesi.
     *
     * <p><b>Non-obvious:</b> objek {@link Pegawai} yang belum tersimpan
     * ({@code getId() == null}) ditolak menjadi {@code null}. Pengamanan ini
     * mencegah {@code CascadeType.PERSIST} tanpa sengaja <i>membuat baris
     * pegawai baru</i> saat sesi disimpan.</p>
     *
     * <p>Combobox pewawancara pada layar admin memuat {@link Pegawai} tanpa
     * penyaring sekolah/yayasan (hanya {@code setMaxResults(500)}), sehingga
     * nilai yang tersimpan di sini bisa merujuk pegawai dari sekolah lain.</p>
     *
     * @param pegawai pewawancara; {@code null} atau objek tanpa id disimpan sebagai {@code null}
     */
    public void setPegawai(Pegawai pegawai) {
        this.pegawai = pegawai == null || pegawai.getId() == null ? null : pegawai;
    }

    /**
     * Gelombang pendaftaran PSB tempat sesi ini bernaung.
     *
     * <p>Relasi lazy lewat kolom {@code gelombang_pendaftaran_psb_id}, dengan
     * resolusi proxy aman via {@code check(..)}.</p>
     *
     * <p><b>Non-obvious:</b> portal calon siswa <b>tidak</b> memakai relasi ini
     * untuk menampilkan pengumuman wawancara. {@code _wawancara_service.jsp}
     * mengambil {@code infoSaatInterview} dari gelombang milik
     * <i>calon siswa</i> ({@code casis.getGelombangPendaftaranPsb()}), bukan
     * dari gelombang milik sesi. Bila keduanya berbeda, teks yang tampil ke
     * calon adalah teks gelombangnya sendiri. Di layar admin, relasi ini hanya
     * dipakai sebagai parameter penyaring opsional pada
     * {@code muatDaftarSesi(..)} — dan penyaring itu selalu dipanggil dengan
     * {@code null}, sehingga dalam praktiknya tidak pernah aktif.</p>
     *
     * <p><b>Efek samping:</b> menugaskan ulang field dengan hasil resolusi.</p>
     *
     * @return gelombang PSB sesi, atau {@code null} bila belum ditetapkan
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "gelombang_pendaftaran_psb_id")
    public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
        gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
        return gelombangPendaftaranPsb;
    }

    /**
     * Menyetel gelombang pendaftaran PSB sesi.
     *
     * <p>Sama seperti {@link #setPegawai(Pegawai)}, objek tanpa id ditolak
     * menjadi {@code null} agar {@code CascadeType.PERSIST} tidak membuat baris
     * gelombang baru.</p>
     *
     * @param gelombangPendaftaranPsb gelombang PSB; {@code null} atau tanpa id
     *                                disimpan sebagai {@code null}
     */
    public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
        this.gelombangPendaftaranPsb = gelombangPendaftaranPsb == null
                || gelombangPendaftaranPsb.getId() == null ? null : gelombangPendaftaranPsb;
    }

    /**
     * Penjurusan sekolah yang disasar sesi ini (mis. IPA/IPS/Keagamaan).
     *
     * <p>Relasi lazy lewat kolom {@code penjurusan_sekolah_id}, dengan resolusi
     * proxy aman via {@code check(..)}.</p>
     *
     * <p><b>Observasi mentah:</b> penelusuran seluruh basis kode tidak
     * menemukan pembaca maupun penulis relasi ini di luar kelas ini —
     * {@code InterviewCalonSiswaAction} tidak menyediakan komboboksnya dan
     * {@code _wawancara_service.jsp} tidak membacanya. Kolomnya karena itu
     * selalu {@code NULL} pada instalasi yang hanya memakai UI standar.</p>
     *
     * <p><b>Efek samping:</b> menugaskan ulang field dengan hasil resolusi.</p>
     *
     * @return penjurusan sasaran, atau {@code null} (kasus lazim)
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "penjurusan_sekolah_id")
    public PenjurusanSekolah getPenjurusanSekolah() {
        penjurusanSekolah = check(penjurusanSekolah);
        return penjurusanSekolah;
    }

    /**
     * Menyetel penjurusan sasaran sesi.
     *
     * <p><b>Ketidakkonsistenan yang disengaja atau terlewat:</b> berbeda dari
     * empat setter relasi lainnya di kelas ini, setter ini <b>tidak</b>
     * memeriksa {@code getId() == null}. Objek {@link PenjurusanSekolah} yang
     * masih transient karena itu diterima apa adanya, dan
     * {@code CascadeType.PERSIST} akan <i>menyisipkan baris penjurusan baru</i>
     * ketika sesi disimpan. Karena relasi ini tidak dipakai UI mana pun,
     * risikonya laten — tetapi patut diseragamkan bila kolom ini kelak
     * diaktifkan.</p>
     *
     * @param penjurusanSekolah penjurusan sasaran; boleh {@code null}
     */
    public void setPenjurusanSekolah(PenjurusanSekolah penjurusanSekolah) {
        this.penjurusanSekolah = penjurusanSekolah;
    }

    /**
     * Sekolah pemilik sesi — kolom tenant utama.
     *
     * <p>Relasi lazy lewat kolom {@code sekolah_id}, dengan resolusi proxy aman
     * via {@code check(..)}.</p>
     *
     * <p><b>Peringatan cakupan tenant:</b> tidak satu pun query pada
     * {@code InterviewCalonSiswaAction} menyaring berdasarkan kolom ini —
     * {@code muatDaftarSesi(..)} dan {@code tampilkanInterview(..)} membangun
     * {@code Criteria} atas seluruh tabel. Nilai di sini karena itu berfungsi
     * sebagai <i>label</i>, bukan sebagai batas akses. Ditambah lagi,
     * penyimpanan bersifat fail-open: {@code if (skolah != null)
     * data.setSekolah(skolah);} — pengguna yang tidak terhubung ke sekolah
     * menghasilkan baris tanpa tenant.</p>
     *
     * <p><b>Efek samping:</b> menugaskan ulang field dengan hasil resolusi.</p>
     *
     * @return sekolah pemilik, atau {@code null} bila baris tak bertenant
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "sekolah_id")
    public Sekolah getSekolah() {
        sekolah = check(sekolah);
        return sekolah;
    }

    /**
     * Menyetel sekolah pemilik sesi.
     *
     * <p>Objek tanpa id ditolak menjadi {@code null} agar
     * {@code CascadeType.PERSIST} tidak membuat baris sekolah baru.</p>
     *
     * @param sekolah tenant sekolah; {@code null} atau tanpa id disimpan sebagai {@code null}
     */
    public void setSekolah(Sekolah sekolah) {
        this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
    }

    /**
     * Yayasan pemilik sesi — kolom tenant tingkat atas.
     *
     * <p><b>Efek samping — getter destruktif.</b> Berbeda dari empat getter
     * relasi lainnya, method ini tidak sekadar meresolusi proxy: ia
     * <b>menurunkan ulang</b> nilai {@code yayasan} dari
     * {@code getSekolah().getYayasan()} setiap kali dipanggil, dan menimpa
     * apa pun yang tersimpan sebelumnya. Karena pemetaan memakai akses
     * property, nilai turunan itu ikut tertulis ke kolom {@code yayasan_id}
     * pada flush berikutnya. Konsekuensinya:</p>
     * <ul>
     *   <li>yayasan yang ditetapkan berbeda dari yayasan sekolahnya
     *       <b>tidak dapat bertahan</b> — setiap pembacaan mengembalikannya ke
     *       yayasan sekolah;</li>
     *   <li>bila {@link #getSekolah()} bernilai {@code null} (kasus fail-open
     *       yang dijelaskan di sana), nilai lama <i>dipertahankan</i>, bukan
     *       dikosongkan — sehingga baris tanpa sekolah bisa tetap membawa
     *       yayasan warisan yang menyesatkan;</li>
     *   <li>penulisan ulang ini menghasilkan revisi Envers walau tidak ada
     *       perubahan yang disengaja pengguna.</li>
     * </ul>
     *
     * @return yayasan pemilik, diturunkan dari sekolah bila tersedia
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "yayasan_id")
    public Yayasan getYayasan() {
        sekolah = getSekolah();
        if (sekolah != null) { yayasan = sekolah.getYayasan(); }
        yayasan = check(yayasan);
        return yayasan;
    }

    /**
     * Menyetel yayasan pemilik sesi.
     *
     * <p>Objek tanpa id ditolak menjadi {@code null}. Perlu diingat bahwa nilai
     * yang disetel di sini akan <b>ditimpa</b> pada pembacaan berikutnya bila
     * {@link #getSekolah()} tidak {@code null} — lihat {@link #getYayasan()}.</p>
     *
     * @param yayasan tenant yayasan; {@code null} atau tanpa id disimpan sebagai {@code null}
     */
    public void setYayasan(Yayasan yayasan) {
        this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
    }

    // ── Metode bantu ──────────────────────────────────────────────────────

    /**
     * Bangkitkan tautan Jitsi Meet otomatis dari nama sesi dan ID.
     * Tautan dibangkitkan sekali per sesi agar semua peserta masuk ke ruang
     * yang sama. Server Jitsi diambil dari konfigurasi
     * {@code alamat_server_video_conference} (default: {@code https://meet.jit.si}).
     *
     * <h3>Cara nama ruang dibentuk</h3>
     * <ol>
     *   <li>{@code roomId = "GEL_SISWA_" + getNama() + "_" + getId()};</li>
     *   <li>{@code contextPath} aplikasi (tanpa {@code "/"}, ter-URL-encode)
     *       ditempelkan di depan dengan pemisah {@code "_"} — inilah yang
     *       memisahkan ruang antar-instalasi yang berbagi satu server Jitsi;</li>
     *   <li>semua karakter di luar {@code [a-zA-Z0-9 ]} diganti {@code "_"},
     *       hasilnya di-{@code toLowerCase()} lalu dipecah dan disambung ulang
     *       pada spasi;</li>
     *   <li>{@code "__"} diciutkan menjadi {@code "_"} — dilakukan
     *       <b>dua kali</b>, karena satu lintasan {@code replaceAll} tidak
     *       menangani deret garis bawah yang lebih panjang (mis. {@code "____"}
     *       menjadi {@code "__"} pada lintasan pertama). Tiga garis bawah atau
     *       lebih tetap bisa menyisakan sisa setelah dua lintasan;</li>
     *   <li>hasil akhir: {@code <server>/<kodeStream>}.</li>
     * </ol>
     *
     * <h3>Sumber {@code HttpServletRequest}</h3>
     * <p>Diambil dari eksekusi ZK yang sedang berjalan bila ada
     * ({@code ExecutionsCtrl.getCurrent()}), dan jatuh ke
     * {@link ais.common.RequestContext#get()} bila tidak. Fallback inilah yang
     * membuat method ini dapat dipanggil dari thread servlet/JSP biasa —
     * termasuk dari {@code _wawancara_service.jsp}. Bila keduanya {@code null},
     * baris {@code request.getContextPath()} melempar
     * {@code NullPointerException} yang <b>tidak</b> tertangkap blok
     * {@code try} di bawahnya (blok itu hanya membungkus tahap normalisasi),
     * sehingga exception naik ke pemanggil.</p>
     *
     * <h3>Kekekalan dan implikasi keamanan</h3>
     * <p>Nama ruang bersifat <b>deterministik</b>, bukan acak: siapa pun yang
     * mengetahui context path instalasi, nama sesi, dan {@code id}-nya dapat
     * menghitung sendiri URL ruangnya. Karena {@code id} bersifat sekuensial
     * dan nama sesi ikut bocor lewat endpoint pra-otentikasi
     * {@code _wawancara_service.jsp} (field JSON {@code interviewerName}),
     * ruang wawancara calon siswa <b>dapat ditebak dan dimasuki pihak luar</b>
     * — Jitsi publik ({@code meet.jit.si}) tidak menuntut kata sandi ruang
     * kecuali dikonfigurasi khusus.</p>
     * <p>Konsekuensi operasional lain: karena {@link #getNama()} ikut membentuk
     * nama ruang, <b>mengubah nama sesi berarti memindahkan ruang konferensi</b>
     * — peserta yang menyimpan tautan lama akan masuk ke ruang kosong.</p>
     *
     * <p><b>Pemanggil:</b> {@code _wawancara_service.jsp} (jalur pra-otentikasi,
     * hasil dikirim sebagai {@code videoLink}) dan cabang {@link #JITSI} pada
     * {@link #createVideoConrefrence(InterviewCalonSiswa, Component, boolean,
     * boolean, EventListener)}.</p>
     *
     * @return URL ruang Jitsi Meet lengkap untuk sesi ini
     * @throws Exception bila {@code URLEncoder.encode(..)} gagal, atau bila
     *                   tidak ada {@code HttpServletRequest} yang dapat
     *                   ditemukan pada thread ini
     */
    public String generateJitsiLink() throws Exception {
        String roomId = "GEL_SISWA_" + getNama() + "_" + getId();
        HttpServletRequest request = null;
        if (ExecutionsCtrl.getCurrent() != null) {
            request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
        }
        if (request == null) {
            request = RequestContext.get();
        }
        String kodeStream = (URLEncoder.encode(
                org.apache.commons.lang3.StringUtils.replace(request.getContextPath(), "/", ""), "UTF-8")
                + "_") + roomId;
        try {
            String[] words = kodeStream.replaceAll("[^a-zA-Z0-9 ]", "_").toLowerCase().split("\\s+");
            kodeStream = "";
            for (String w : words) {
                kodeStream += kodeStream.isEmpty() ? w : "_" + w;
            }
            kodeStream = kodeStream.replaceAll("__", "_");
            kodeStream = kodeStream.replaceAll("__", "_");
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/InterviewCalonSiswa.java:299");
        }
        return Common.getKonfigurasi("alamat_server_video_conference", "https://meet.jit.si").getNilai()
                + "/" + kodeStream;
    }

    /**
     * Bangkitkan tombol video konferensi ZK sesuai platform yang dipilih.
     * Digunakan oleh {@code InterviewCalonSiswaAction} untuk panel admin.
     *
     * <h3>KOREKSI — status pemakaian sebenarnya</h3>
     * <p>Kalimat "digunakan oleh {@code InterviewCalonSiswaAction}" di atas
     * dipertahankan sebagai catatan maksud awal, tetapi <b>tidak sesuai
     * keadaan kode saat ini</b>: penelusuran menyeluruh atas {@code src/} dan
     * {@code webapp/} tidak menemukan satu pun pemanggil method ini.
     * {@code InterviewCalonSiswaAction} membangun tombolnya sendiri
     * (Edit/Peserta/Hapus) dan tidak pernah memanggil
     * {@code InterviewCalonSiswa.createVideoConrefrence(..)}. Method ini karena
     * itu berstatus <b>kode mati</b> pada revisi ini.</p>
     * <p>Jalur yang benar-benar hidup untuk membuka ruang konferensi sesi
     * wawancara adalah portal calon siswa
     * ({@code /WEB-INF/baru/modul/ppdb/_wawancara.jsp} +
     * {@code _wawancara_service.jsp}), yang menyusun tombolnya di sisi klien
     * dari field JSON {@code videoLink}/{@code videoIconKey} — bukan dari
     * komponen ZK ini. Padanan yang aktif dipakai di modul lain adalah
     * {@code DashboardTimelinePertemuan.createVideoConrefrence(Pertemuan, ..)}
     * dan {@code GelombangPendaftaran.createVideoConrefrence(..)}; keduanya
     * mengikuti bentuk yang sama sehingga method ini tampaknya hasil salin
     * pola tersebut.</p>
     *
     * <h3>Perilaku</h3>
     * <p>Membuat satu {@link Button} berlabel "Online" (varian
     * {@code MyButtonConfig} bila {@code button==true}, atau
     * {@code MyToolbarbuttonConfig} bila {@code false} — perhatikan bahwa tipe
     * kembaliannya tetap {@link Button}, yang berlaku karena
     * {@code MyToolbarbuttonConfig} berada dalam hierarki yang sama), lalu
     * memasang <i>event listener</i> {@code onClick} sesuai
     * {@link #getOnlineMenggunakan()}:</p>
     * <ul>
     *   <li>{@link #JITSI} — URL dihitung saat klik lewat
     *       {@link #generateJitsiLink()};</li>
     *   <li>{@link #GOOGLE_MEET} — URL dari {@link #getLainLink()}, ikon
     *       {@code /img/meet-google.png};</li>
     *   <li>{@link #ZOOM} — URL dari {@link #getZoomLink()}, ikon
     *       {@code /img/zoom.png};</li>
     *   <li>{@link #BBB} — URL dari {@link #getBbbLink()};</li>
     *   <li>{@link #SKYPE} — URL dari {@link #getSkypeLink()};</li>
     *   <li>{@link #WA} — URL dari {@link #getWaLink()}, jendela 800&times;600
     *       (satu-satunya yang berbeda ukuran);</li>
     *   <li>{@link #LAIN} — URL dari {@link #getLainLink()}.</li>
     * </ul>
     * <p>Setiap cabang selain Jitsi memvalidasi tautan lebih dulu dan
     * menampilkan {@code MyMessageboxConfig} bila kosong, lalu berhenti tanpa
     * memanggil {@code eventListener}. Pembukaan tautan memakai
     * {@code sendRedirect(url, "_blank")} pada perangkat mobile
     * ({@code Common.isMobile()}) dan skrip klien {@code popupCenter({..})}
     * pada desktop.</p>
     *
     * <h3>Hal non-obvious</h3>
     * <ul>
     *   <li><b>Tombol selalu dikembalikan, tetapi tidak selalu terpasang.</b>
     *       {@code btn.setParent(parent)} hanya dipanggil di dalam cabang
     *       platform. Untuk {@link #TIDAK_AKTIF} (atau kode di luar 0–7) tombol
     *       dibuat, <i>tidak</i> ditempelkan ke {@code parent}, dan
     *       dikembalikan tanpa listener — pemanggil yang mengabaikan nilai
     *       kembalian aman, tetapi pemanggil yang memasangnya sendiri akan
     *       mendapat tombol mati.</li>
     *   <li><b>URL disisipkan mentah ke JavaScript.</b> Nilai tautan
     *       ditempelkan langsung ke dalam literal string
     *       {@code "popupCenter({url:'" + url + "'..."} tanpa <i>escaping</i>.
     *       Tautan yang mengandung tanda kutip tunggal memutus skrip; nilai
     *       yang dibuat sengaja dapat menyuntikkan JavaScript ke sesi
     *       pengguna yang mengklik. Karena kolom tautan diisi lewat layar admin
     *       yang <b>tanpa gerbang privilese</b> (lihat Javadoc kelas), pintu
     *       masuk untuk nilai jahat itu lebih lebar dari yang tampak. Padanan
     *       yang aktif dipakai di modul lain memiliki pola yang sama, sehingga
     *       ini bersifat pola arsitektur, bukan kekhususan berkas ini.</li>
     *   <li><b>Parameter {@code vertical} hanya berlaku pada orientasi
     *       tombol</b> ({@code btn.setOrient("vertical")}), disetel sebelum
     *       cabang platform sehingga berlaku juga untuk tombol yang tidak
     *       terpasang.</li>
     * </ul>
     *
     * @param ics           sesi interview yang akan ditampilkan tombolnya;
     *                      tidak boleh {@code null} — tidak ada penjagaan
     *                      {@code null} di dalam method
     * @param parent        komponen induk tempat tombol dipasang; boleh
     *                      {@code null} bila pemanggil memasangnya sendiri
     * @param vertical      orientasi tombol (vertikal jika true)
     * @param button        gunakan Button biasa (true) atau Toolbarbutton (false)
     * @param eventListener aksi tambahan setelah klik (boleh null); dipanggil
     *                      dengan argumen {@code null}, jadi implementasinya
     *                      harus tahan terhadap {@code Event} yang {@code null},
     *                      dan <b>tidak</b> dipanggil bila tautan kosong
     * @return tombol yang sudah dikonfigurasi (belum tentu terpasang ke
     *         {@code parent} — lihat catatan di atas)
     * @throws Exception meneruskan kegagalan konstruksi komponen ZK
     */
    public static Button createVideoConrefrence(final InterviewCalonSiswa ics, Component parent,
            boolean vertical, boolean button, final EventListener eventListener) throws Exception {

        Button btn = button
                ? new MyButtonConfig("Online", "/img/svg/user-group.svg")
                : new MyToolbarbuttonConfig("Online", "/img/svg/user-group.svg");

        if (vertical) { btn.setOrient("vertical"); }

        if (JITSI.equals(ics.getOnlineMenggunakan())) {

            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.generateJitsiLink();
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'Video Conference',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (GOOGLE_MEET.equals(ics.getOnlineMenggunakan())) {

            btn.setImage("/img/meet-google.png");
            btn.setStyle("font-size:9px");
            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getLainLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan Google Meet di kolom Link Lain.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'Google Meet',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (ZOOM.equals(ics.getOnlineMenggunakan())) {

            btn.setImage("/img/zoom.png");
            btn.setStyle("font-size:9px");
            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getZoomLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan Zoom di kolom Zoom Link.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'Zoom',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (BBB.equals(ics.getOnlineMenggunakan())) {

            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getBbbLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan BigBlueButton di kolom BBB Link.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'BBB',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (SKYPE.equals(ics.getOnlineMenggunakan())) {

            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getSkypeLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan Skype di kolom Skype Link.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'Skype',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (WA.equals(ics.getOnlineMenggunakan())) {

            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getWaLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan WhatsApp di kolom WA Link.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'WhatsApp',w:800,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });

        } else if (LAIN.equals(ics.getOnlineMenggunakan())) {

            btn.setParent(parent);
            btn.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String url = ics.getLainLink();
                    if (url == null || url.trim().isEmpty()) {
                        MyMessageboxConfig.show(
                                "Harap masukkan tautan konferensi di kolom Link Lain.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    if (Common.isMobile()) {
                        ExecutionsCtrl.getCurrent().sendRedirect(url, "_blank");
                    } else {
                        Clients.evalJavaScript(
                                "popupCenter({url:'" + url + "',title:'Konferensi Video',w:1200,h:600});");
                    }
                    if (eventListener != null) { eventListener.onEvent(null); }
                }
            });
        }

        return btn;
    }
}
