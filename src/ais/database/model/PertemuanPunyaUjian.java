package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * PertemuanPunyaUjian &ndash; Entitas bridging antara satu sesi perkuliahan ({@link Pertemuan})
 * dengan satu konfigurasi ujian ({@link Ujian}) dalam sistem CBT (Computer-Based Test)
 * e-Learning kampus AIS.
 *
 * <p>Dalam arsitektur akademik sistem AIS, sebuah pertemuan kuliah dapat memiliki satu atau
 * lebih ujian yang dijadwalkan secara independen. Entitas ini berperan sebagai tabel penghubung
 * (bridge/join table) yang tidak sekadar menyimpan foreign key ke {@link Pertemuan} dan
 * {@link Ujian}, melainkan juga menyimpan seluruh konfigurasi yang bersifat spesifik untuk
 * kombinasi pertemuan-ujian tersebut. Ini berarti satu kumpulan soal ({@link Ujian}) yang sama
 * dapat dipasang pada beberapa pertemuan dengan pengaturan berbeda-beda: waktu buka-tutup
 * berbeda, mode anti-curang berbeda, format penilaian berbeda, dan sebagainya.</p>
 *
 * <h3>Alur Penggunaan dalam Sistem Ujian Online</h3>
 * <p>Alur umum entitas ini dalam sistem ujian online adalah sebagai berikut:</p>
 * <ol>
 *   <li>Dosen atau admin membuat sebuah pertemuan ({@link Pertemuan}) untuk mata kuliah
 *       tertentu pada jadwal kuliah yang telah ditentukan.</li>
 *   <li>Bank soal ({@link Ujian}) dibuat atau dipilih dari kumpulan soal yang sudah tersedia
 *       dalam sistem.</li>
 *   <li>{@code PertemuanPunyaUjian} dibuat untuk menghubungkan keduanya sekaligus menyimpan
 *       semua pengaturan ujian: waktu buka-tutup, durasi pengerjaan, jumlah soal yang
 *       ditampilkan, mode random soal, bobot nilai OBE, dan konfigurasi anti-curang.</li>
 *   <li>Setiap mahasiswa yang mengikuti ujian memiliki satu catatan {@link HasilUjianMahasiswa}
 *       yang mereferensikan entitas {@code PertemuanPunyaUjian} ini.</li>
 *   <li>Setelah ujian selesai, hasil dikompilasi dan nilainya dapat dihubungkan ke
 *       {@link FormatNilai} (Sub-CPMK dalam skema OBE) untuk keperluan pelaporan
 *       Capaian Pembelajaran Lulusan (CPL).</li>
 * </ol>
 *
 * <h3>Konfigurasi Waktu Ujian</h3>
 * <p>Entitas ini mendukung dua mode waktu ujian:</p>
 * <ul>
 *   <li><strong>Dibatasi waktu</strong> ({@link #getDibatasiWaktu()} {@code = true}):
 *       Ujian hanya dapat diakses antara {@link #getMulaiUjian()} dan {@link #getSampaiUjian()}.
 *       Jika belum dikonfigurasi secara eksplisit, waktu mulai dan selesai diambil dari
 *       {@link Pertemuan} induknya secara otomatis sebagai fallback.</li>
 *   <li><strong>Tidak dibatasi waktu</strong>: Ujian dapat dikerjakan kapan saja selama
 *       masih berstatus aktif, tanpa batasan rentang timestamp.</li>
 * </ul>
 * <p>Durasi pengerjaan per peserta dikonfigurasi melalui {@link #getLama()} dalam format
 * {@code TemporalType.TIME}. Nilai default adalah 1 jam ({@value #DURASI_DEFAULT_UJIAN})
 * sesuai standar yang ditetapkan. Ujian lama (sebelum kolom ini ditambahkan) otomatis
 * mendapatkan nilai default ini.</p>
 *
 * <h3>Konfigurasi Anti-Curang (CBT) Per-Ujian</h3>
 * <p>Fitur anti-curang sebelumnya diatur secara GLOBAL melalui tabel Konfigurasi sistem
 * (14 kunci yang dibaca oleh {@code ProsesUjianHelper.buildCbtAntiCheatScript}).
 * Mulai versi 2.0, setiap {@code PertemuanPunyaUjian} menyimpan konfigurasi anti-curangnya
 * sendiri pada kolom-kolom bertanda {@code ac_*}. Ini memungkinkan kuis harian tanpa
 * pengawasan menggunakan pengaturan ringan, sementara UTS/UAS dapat mengaktifkan semua
 * mekanisme proteksi secara penuh.</p>
 * <p>Kontrak <em>backward-compatible</em> dipertahankan: setiap getter mengembalikan nilai
 * default yang identik dengan konfigurasi global lama ketika kolom {@code ac_*} masih
 * {@code null}, sehingga ujian-ujian yang sudah ada tidak terpengaruh oleh migrasi.</p>
 * <p>Fitur anti-curang yang tersedia per-ujian:</p>
 * <ul>
 *   <li>{@link #getAntiCurangAktif()} &ndash; Master toggle. Default: {@code false} (opt-in per ujian).</li>
 *   <li>{@link #getAntiCurangBatasPelanggaran()} &ndash; Batas pelanggaran sebelum ujian berakhir otomatis. Default: 3.</li>
 *   <li>{@link #getAntiCurangAktifkanFullscreen()} &ndash; Paksa mode layar penuh. Default: {@code true}.</li>
 *   <li>{@link #getAntiCurangDeteksiPindahTab()} &ndash; Deteksi berpindah tab (Page Visibility API). Default: {@code true}.</li>
 *   <li>{@link #getAntiCurangDeteksiBlurJendela()} &ndash; Deteksi window blur (Alt+Tab, dsb). Default: {@code true}.</li>
 *   <li>{@link #getAntiCurangCooldownBlurMs()} &ndash; Debounce deteksi blur dalam ms. Default: 5000 ms.</li>
 *   <li>{@link #getAntiCurangDeteksiKeluarFullscreen()} &ndash; Deteksi keluar fullscreen. Default: {@code true}.</li>
 *   <li>{@link #getAntiCurangBlokirKlikKanan()} &ndash; Blokir klik kanan / context menu. Default: {@code true}.</li>
 *   <li>{@link #getAntiCurangBlokirShortcut()} &ndash; Blokir shortcut berbahaya (Ctrl+W/T/N/R, F5, F12). Default: {@code true}.</li>
 *   <li>{@link #getAntiCurangPeringatanKeluarHalaman()} &ndash; Konfirmasi beforeunload. Default: {@code true}.</li>
 *   <li>{@link #getAntiCurangBlokirTangkapLayar()} &ndash; Blokir PrintScreen. Default: {@code true}.</li>
 *   <li>{@link #getAntiCurangLarangMultiDevice()} &ndash; Larang sesi bersamaan di perangkat berbeda. Default: {@code true}.</li>
 *   <li>{@link #getAntiCurangLokasiLatitude()}, {@link #getAntiCurangLokasiLongitude()},
 *       {@link #getAntiCurangLokasiRadius()} &ndash; Restriksi lokasi GPS. Default: {@code null} (nonaktif).</li>
 * </ul>
 *
 * <h3>Integrasi OBE (Outcome-Based Education)</h3>
 * <p>Untuk mendukung sistem OBE, entitas ini menyimpan pemetaan nomor soal ke Sub-CPMK
 * ({@link FormatNilai}) melalui {@link #getFormatNilais()} dalam format JSON. Pada ujian
 * remedial, {@link #getSubCpmkPerPeserta()} memungkinkan setiap peserta hanya mengerjakan
 * Sub-CPMK tertentu yang berbeda antar-peserta. Nilai manual override untuk peserta yang
 * tidak ikut ujian dapat disimpan di {@link #getNilaiManualJson()}.</p>
 *
 * <h3>Cache Hasil Ujian Mahasiswa</h3>
 * <p>Untuk menghindari query N+1 pada pemuatan daftar peserta ujian, entitas ini menggunakan
 * mekanisme cache berbasis file. Cache disimpan sebagai JSON berisi ID dari setiap
 * {@link HasilUjianMahasiswa} yang terkait. Cache harus di-reinisialisasi dengan
 * {@link #reInitHasilUjianMahasiswa(Session)} jika ada perubahan pada daftar peserta.</p>
 *
 * <h3>Relasi ke Entitas Lain</h3>
 * <ul>
 *   <li>{@link Pertemuan} &ndash; Sesi perkuliahan induk (FetchMode.SELECT, PERSIST+MERGE).</li>
 *   <li>{@link Ujian} &ndash; Kumpulan soal yang digunakan (LAZY, PERSIST+MERGE).</li>
 *   <li>{@link FormatNilai} &ndash; Format penilaian / Sub-CPMK dalam OBE (FetchMode.SELECT, PERSIST+MERGE).</li>
 *   <li>{@link Fakultas}, {@link Jurusan} &ndash; Diwarisi otomatis dari {@link Ujian} (LAZY).</li>
 *   <li>{@link Yayasan}, {@link Sekolah} &ndash; Diwarisi otomatis dari {@link Ujian} untuk multi-tenant (LAZY).</li>
 *   <li>{@link JenisItemPenilaianSiswa}, {@link GrupKategoriItemPenilaianSiswa},
 *       {@link GrupPenilaian}, {@link DetailGrupPenilaian} &ndash; Relasi ke sistem penilaian sekolah (LAZY).</li>
 * </ul>
 *
 * <h3>Catatan Penting untuk Developer</h3>
 * <ul>
 *   <li>Kelas ini menggunakan {@code @org.hibernate.annotations.Entity(dynamicInsert=true,
 *       dynamicUpdate=true)} sehingga Hibernate hanya menyertakan kolom yang benar-benar
 *       berubah pada query INSERT/UPDATE, menghemat bandwidth dan menghindari konflik update.</li>
 *   <li>Semua perubahan data diaudit via Hibernate Envers ({@code @Audited}). Tabel audit
 *       {@code pertemuan_punya_ujian_aud} dibuat otomatis oleh hbm2ddl.</li>
 *   <li>Getter {@link #getFakultas()}, {@link #getJurusan()}, {@link #getYayasan()}, dan
 *       {@link #getSekolah()} bersifat derived: nilainya selalu diambil dari {@link Ujian}
 *       terkait jika tersedia, menimpa nilai lokal yang tersimpan di kolom.</li>
 *   <li>Kompatibel dengan Java 1.6/1.7 dan Hibernate 3.x. Tidak menggunakan fitur Java 8+
 *       (lambda, Stream API, try-with-resources, diamond operator, multi-catch).</li>
 *   <li>Timestamp {@link #tanggal_dirubah} diperbarui otomatis oleh lifecycle callback
 *       {@link #onUpdate()} melalui {@code AuditTimestampInterceptor}.</li>
 * </ul>
 *
 * @author AIS System
 * @version 2.0
 * @since 2010
 * @see Pertemuan
 * @see Ujian
 * @see HasilUjianMahasiswa
 * @see FormatNilai
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pertemuan_punya_ujian")
public class PertemuanPunyaUjian extends GeneralValueObject {

    /** Serial version untuk kompatibilitas Serializable lintas-versi. */
    private static final long serialVersionUID = 2463821577548439808L;

    /** Durasi default pengerjaan ujian jika belum dikonfigurasi: 1 jam (standar STIKSAM). */
    private static final String DURASI_DEFAULT_UJIAN = "01:00:00";

    // ─── Teks Default Peringatan Anti-Curang ──────────────────────────────────────────────

    /** Teks peringatan default: terdeteksi berpindah tab atau aplikasi lain. */
    public static final String DEF_AC_PESAN_PINDAH_TAB =
            "Anda terdeteksi berpindah tab atau aplikasi lain! Tetaplah di halaman ujian ini.";

    /** Teks peringatan default: beralih jendela (Alt+Tab, dsb). */
    public static final String DEF_AC_PESAN_BLUR =
            "Anda terdeteksi beralih ke jendela lain (contoh: Alt+Tab, Ctrl+Alt+Del). Segera kembali ke ujian!";

    /** Teks peringatan default: keluar dari mode layar penuh. */
    public static final String DEF_AC_PESAN_KELUAR_FS =
            "Anda keluar dari mode layar penuh! Klik tombol di bawah untuk kembali ke ujian.";

    /** Teks peringatan default: sebelum meninggalkan halaman ujian (beforeunload). */
    public static final String DEF_AC_PESAN_KELUAR_HAL = "Jika Anda keluar, ujian akan dianggap selesai!";

    // ─── Informasi Dasar ──────────────────────────────────────────────────────────────────

    private Long id;
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    // ─── Relasi Utama ─────────────────────────────────────────────────────────────────────

    private Pertemuan pertemuan;
    private Ujian ujian;

    // ─── Konfigurasi Umum Ujian ───────────────────────────────────────────────────────────

    private Boolean aktif;
    private String nama;
    private String keterangan;
    private Boolean random;
    private Boolean tiapSoal;
    private Boolean otomatisMunculKetikaBelumSelesai;
    private Boolean tidakDiaktifkanTombolKembali;
    private Integer jmlDitampilkan;
    private Double prosentase;
    private String mhsYgTidakIkut;
    private Integer jumlahBolehIkut;

    // ─── Konfigurasi Waktu Ujian ──────────────────────────────────────────────────────────

    private Boolean dibatasiWaktu = false;
    private Boolean tidakDitampilkanJikaWaktuSudahTerlewat = false;
    private Date lama;
    private Date mulaiUjian = ais.ui.util.WaktuUtil.getDate();
    private Date sampaiUjian = ais.ui.util.WaktuUtil.getDate();

    // ─── Konfigurasi Nilai & Format ───────────────────────────────────────────────────────

    private Boolean lihatJawabanSetelahUjian = false;
    private Boolean lihatNilaiSetelahUjian = false;
    private FormatNilai formatNilai;
    private String formatNilais;
    private String subCpmkPerPeserta;
    private String nilaiManualJson;

    /**
     * JSON untuk menyimpan hasil review Penjaminan Mutu terhadap Analisis Butir Soal ujian ini.
     * Format: {@code {"status":"menunggu|disetujui|perlu_revisi","catatan":"...","reviewerId":123,"reviewTime":"..."}}
     */
    private String analisisCatatanJson;

    // ─── Relasi Organisasi (diwarisi dari Ujian) ──────────────────────────────────────────

    private Fakultas fakultas;
    private Jurusan jurusan;
    private Yayasan yayasan;
    private Sekolah sekolah;

    // ─── Relasi Penilaian Sekolah ─────────────────────────────────────────────────────────

    private JenisItemPenilaianSiswa jenisItemPenilaianSiswa;
    private GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa;
    private GrupPenilaian grupPenilaian;
    private DetailGrupPenilaian detailGrupPenilaian;

    // ─── Konfigurasi Anti-Curang (CBT) Per-Ujian ─────────────────────────────────────────
    // Dipindah dari konfigurasi GLOBAL ke sini agar tiap ujian dapat mengatur sendiri.
    // Semua getter mengembalikan DEFAULT lama saat nilai null → ujian lama tetap terlindungi.

    private Boolean antiCurangAktif;
    private Integer antiCurangBatasPelanggaran;
    private Boolean antiCurangAktifkanFullscreen;
    private Boolean antiCurangDeteksiPindahTab;
    private Boolean antiCurangDeteksiBlurJendela;
    private Integer antiCurangCooldownBlurMs;
    private Boolean antiCurangDeteksiKeluarFullscreen;
    private Boolean antiCurangBlokirKlikKanan;
    private Boolean antiCurangBlokirShortcut;
    private Boolean antiCurangPeringatanKeluarHalaman;
    private String antiCurangPesanPindahTab;
    private String antiCurangPesanBlurJendela;
    private String antiCurangPesanKeluarFullscreen;
    private String antiCurangPesanKeluarHalaman;
    private Boolean antiCurangBlokirTangkapLayar;
    private Boolean antiCurangLarangMultiDevice;
    private Double antiCurangLokasiLatitude;
    private Double antiCurangLokasiLongitude;
    private Integer antiCurangLokasiRadius;

    // ─── Konstruktor ──────────────────────────────────────────────────────────────────────

    /**
     * Konstruktor default yang diperlukan oleh Hibernate untuk instansiasi entitas
     * melalui refleksi saat memuat data dari database.
     */
    public PertemuanPunyaUjian() {
    }

    /**
     * Konstruktor berparameter untuk membuat referensi ringan ke entitas berdasarkan ID-nya,
     * berguna saat hanya ID yang dibutuhkan tanpa memuat seluruh data dari database.
     *
     * @param id primary key entitas yang dirujuk
     */
    public PertemuanPunyaUjian(Long id) {
        this.id = id;
    }

    // ─── Lifecycle Callbacks ──────────────────────────────────────────────────────────────

    /**
     * Callback Hibernate yang dipanggil otomatis sebelum setiap operasi UPDATE ke database.
     * Memperbarui timestamp audit ({@link #tanggal_dirubah}) dan informasi pengubah terakhir
     * melalui {@code AuditTimestampInterceptor}.
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    // ─── toString ─────────────────────────────────────────────────────────────────────────

    /**
     * Representasi teks entitas ini untuk keperluan logging dan debugging.
     * Mengembalikan kombinasi id, pertemuan, dan ujian yang terkait.
     *
     * @return String berformat "id-pertemuan-ujian"
     */
    public String toString() {
        pertemuan = getPertemuan();
        ujian = getUjian();
        return id + "-" + pertemuan + "-" + ujian;
    }

    // ─── Getters & Setters: Informasi Dasar ──────────────────────────────────────────────

    /**
     * Mengembalikan primary key entitas ini yang di-generate otomatis oleh database
     * menggunakan strategi IDENTITY (auto-increment).
     *
     * @return Long primary key, atau null jika entitas belum pernah disimpan ke database
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() {
        return this.id;
    }

    /**
     * Menetapkan primary key entitas. Biasanya hanya dipanggil oleh Hibernate secara internal
     * setelah INSERT ke database. Hindari memanggil method ini secara manual.
     *
     * @param id nilai primary key yang akan di-set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Mengembalikan nama pengguna (username atau nama lengkap) yang terakhir kali mengubah
     * entitas ini, untuk keperluan audit trail tampilan.
     *
     * @return String nama pengubah terakhir, atau null jika belum pernah diubah secara eksplisit
     */
    public String getOleh() {
        return oleh;
    }

    /**
     * Menetapkan nama pengubah terakhir. Input null atau kosong diabaikan untuk mencegah
     * penimpaan data audit yang valid dengan nilai kosong.
     *
     * @param oleh nama pengguna pengubah; diabaikan jika null atau blank
     */
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) {
            return;
        }
        this.oleh = oleh;
    }

    /**
     * Mengembalikan ID pengguna (user ID dari tabel pengguna sistem) yang terakhir kali
     * mengubah entitas ini, melengkapi informasi audit dari {@link #getOleh()}.
     *
     * @return String user ID pengubah terakhir, atau null jika belum dikonfigurasi
     */
    public String getOlehId() {
        return olehId;
    }

    /**
     * Menetapkan ID pengguna pengubah terakhir. Input null atau kosong diabaikan
     * untuk menjaga konsistensi data audit.
     *
     * @param olehId user ID pengguna; diabaikan jika null atau blank
     */
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) {
            return;
        }
        this.olehId = olehId;
    }

    /**
     * Mengembalikan timestamp terakhir entitas ini diubah. Diperbarui otomatis melalui
     * lifecycle callback {@link #onUpdate()} setiap kali entitas di-UPDATE ke database.
     *
     * @return Date timestamp perubahan terakhir
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    /**
     * Menetapkan timestamp perubahan terakhir. Umumnya diatur oleh
     * {@code AuditTimestampInterceptor} secara otomatis; hindari memanggil secara manual
     * kecuali untuk keperluan migrasi data.
     *
     * @param tanggal_dirubah nilai timestamp yang akan di-set
     */
    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }

    // ─── Getters & Setters: Relasi Utama ──────────────────────────────────────────────────

    /**
     * Mengembalikan pertemuan (sesi kuliah) induk yang memiliki ujian ini.
     * Relasi ini menggunakan {@code FetchMode.SELECT} sehingga Pertemuan di-load
     * secara terpisah (bukan JOIN) untuk menghindari masalah fetch pada query kompleks.
     *
     * @return Pertemuan entitas pertemuan induk, atau null jika belum dikonfigurasi
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "pertemuan", nullable = true)
    public Pertemuan getPertemuan() {
        return pertemuan;
    }

    /**
     * Menetapkan pertemuan induk untuk ujian ini. Relasi wajib untuk konteks normal;
     * nullable untuk mendukung ujian mandiri (standalone exam) tanpa pertemuan.
     *
     * @param pertemuan entitas Pertemuan yang akan dijadikan induk
     */
    public void setPertemuan(Pertemuan pertemuan) {
        this.pertemuan = pertemuan;
    }

    /**
     * Mengembalikan entitas Ujian (bank soal) yang digunakan pada ujian ini.
     * Relasi di-load secara LAZY untuk efisiensi memori; pemanggilan getter ini
     * akan memicu query ke database jika session masih aktif.
     *
     * @return Ujian entitas bank soal yang terkait, atau null jika belum dikonfigurasi
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "ujian", nullable = true)
    public Ujian getUjian() {
        ujian = check(ujian);
        return ujian;
    }

    /**
     * Menetapkan entitas Ujian (bank soal) untuk ujian ini.
     *
     * @param ujian entitas Ujian yang akan digunakan
     */
    public void setUjian(Ujian ujian) {
        this.ujian = ujian;
    }

    // ─── Getters & Setters: Konfigurasi Umum Ujian ────────────────────────────────────────

    /**
     * Mengembalikan status aktif ujian ini. Ujian yang tidak aktif tidak akan ditampilkan
     * kepada mahasiswa meskipun berada dalam rentang waktu yang valid. Default: {@code true}.
     *
     * @return Boolean true jika ujian aktif dan dapat diakses mahasiswa
     */
    public Boolean getAktif() {
        return aktif == null ? true : aktif;
    }

    /**
     * Menetapkan status aktif ujian. Set ke {@code false} untuk menyembunyikan ujian
     * sementara tanpa menghapusnya dari sistem.
     *
     * @param aktif true untuk mengaktifkan, false untuk menonaktifkan
     */
    public void setAktif(Boolean aktif) {
        this.aktif = aktif;
    }

    /**
     * Mengembalikan nama tampilan ujian ini. Jika nama belum dikonfigurasi secara eksplisit,
     * nama diambil otomatis dari {@link Ujian} terkait sebagai fallback yang nyaman.
     *
     * @return String nama ujian, tidak pernah null jika Ujian terkait memiliki nama
     */
    public String getNama() {
        ujian = getUjian();
        if (ujian != null && (nama == null || nama.trim().isEmpty())) {
            nama = ujian.getNama();
        }
        return nama;
    }

    /**
     * Menetapkan nama tampilan khusus untuk ujian ini, menimpa nama dari {@link Ujian} terkait.
     * Berguna ketika satu bank soal digunakan di beberapa pertemuan dengan nama berbeda.
     *
     * @param nama nama tampilan yang akan digunakan; null atau blank akan menyebabkan
     *             nama diambil otomatis dari Ujian
     */
    public void setNama(String nama) {
        this.nama = nama;
    }

    /**
     * Mengembalikan keterangan atau deskripsi tambahan untuk ujian ini.
     * Dapat digunakan untuk petunjuk pengerjaan atau instruksi khusus per-pertemuan.
     *
     * @return String keterangan, atau null jika tidak ada
     */
    @Column(name = "keterangan", nullable = true)
    public String getKeterangan() {
        return this.keterangan;
    }

    /**
     * Menetapkan keterangan atau deskripsi tambahan untuk ujian ini.
     *
     * @param keterangan teks keterangan yang akan disimpan
     */
    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    /**
     * Mengembalikan apakah urutan soal diacak (random) untuk setiap peserta ujian.
     * Pengacakan soal meningkatkan integritas ujian dengan mencegah contekan antar peserta.
     * Default: {@code true} (soal diacak).
     *
     * @return Boolean true jika soal ditampilkan dalam urutan acak per peserta
     */
    public Boolean getRandom() {
        return random == null ? true : random;
    }

    /**
     * Menetapkan apakah urutan soal diacak untuk setiap peserta ujian.
     *
     * @param random true untuk mengacak urutan soal, false untuk urutan tetap
     */
    public void setRandom(Boolean random) {
        this.random = random;
    }

    /**
     * Mengembalikan apakah navigasi ujian bersifat satu-soal-per-halaman (tiap soal
     * ditampilkan satu per satu) atau semua soal sekaligus dalam satu halaman.
     * Default: {@code false} (semua soal tampil sekaligus).
     *
     * @return Boolean true jika mode satu-soal-per-halaman diaktifkan
     */
    public Boolean getTiapSoal() {
        return tiapSoal == null ? false : tiapSoal;
    }

    /**
     * Menetapkan mode navigasi soal: satu per satu atau semua sekaligus.
     *
     * @param tiapSoal true untuk mode satu-soal-per-halaman
     */
    public void setTiapSoal(Boolean tiapSoal) {
        this.tiapSoal = tiapSoal;
    }

    /**
     * Mengembalikan apakah ujian ini otomatis muncul kembali ke mahasiswa ketika
     * mereka belum menyelesaikannya. Fitur ini mengingatkan mahasiswa untuk
     * menyelesaikan ujian yang tertunda. Default: {@code true}.
     *
     * @return Boolean true jika ujian otomatis muncul kembali ke peserta yang belum selesai
     */
    public Boolean getOtomatisMunculKetikaBelumSelesai() {
        return otomatisMunculKetikaBelumSelesai == null ? true : otomatisMunculKetikaBelumSelesai;
    }

    /**
     * Menetapkan apakah ujian otomatis muncul kembali untuk peserta yang belum selesai.
     *
     * @param otomatisMunculKetikaBelumSelesai true untuk mengaktifkan fitur ini
     */
    public void setOtomatisMunculKetikaBelumSelesai(Boolean otomatisMunculKetikaBelumSelesai) {
        this.otomatisMunculKetikaBelumSelesai = otomatisMunculKetikaBelumSelesai;
    }

    /**
     * Mengembalikan apakah tombol "Kembali" (navigasi ke soal sebelumnya) dinonaktifkan
     * selama ujian berlangsung. Menonaktifkan tombol ini memaksa peserta mengerjakan soal
     * secara berurutan maju tanpa bisa kembali. Default: {@code false} (tombol kembali aktif).
     *
     * @return Boolean true jika tombol kembali dinonaktifkan
     */
    public Boolean getTidakDiaktifkanTombolKembali() {
        return tidakDiaktifkanTombolKembali == null ? false : tidakDiaktifkanTombolKembali;
    }

    /**
     * Menetapkan apakah tombol "Kembali" ke soal sebelumnya dinonaktifkan selama ujian.
     *
     * @param tidakDiaktifkanTombolKembali true untuk menonaktifkan navigasi mundur
     */
    public void setTidakDiaktifkanTombolKembali(Boolean tidakDiaktifkanTombolKembali) {
        this.tidakDiaktifkanTombolKembali = tidakDiaktifkanTombolKembali;
    }

    /**
     * Mengembalikan jumlah soal yang ditampilkan kepada peserta dari keseluruhan bank soal.
     * Nilai 0 berarti seluruh soal dalam bank ditampilkan. Dikombinasikan dengan
     * {@link #getRandom()}, ini memungkinkan setiap peserta mendapat subset soal yang berbeda.
     * Default: {@code 0} (tampilkan semua soal).
     *
     * @return Integer jumlah soal yang ditampilkan, 0 berarti semua soal
     */
    @Column(name = "jml_ditampilkan", nullable = true)
    public Integer getJmlDitampilkan() {
        return jmlDitampilkan == null ? 0 : jmlDitampilkan;
    }

    /**
     * Menetapkan jumlah soal yang ditampilkan kepada peserta dari keseluruhan bank soal.
     *
     * @param jmlDitampilkan jumlah soal; set 0 atau null untuk menampilkan semua soal
     */
    public void setJmlDitampilkan(Integer jmlDitampilkan) {
        this.jmlDitampilkan = jmlDitampilkan;
    }

    /**
     * Mengembalikan bobot prosentase nilai ujian ini terhadap nilai akhir mata kuliah.
     * Misalnya, nilai 30.0 berarti ujian ini berkontribusi 30% terhadap nilai akhir.
     * Default: {@code 100.0} (penuh, tidak ada pembobotan).
     *
     * @return Double prosentase bobot nilai, antara 0.0 hingga 100.0
     */
    public Double getProsentase() {
        return prosentase == null ? 100.0 : prosentase;
    }

    /**
     * Menetapkan bobot prosentase nilai ujian ini terhadap nilai akhir mata kuliah.
     *
     * @param prosentase nilai bobot antara 0.0 hingga 100.0
     */
    public void setProsentase(Double prosentase) {
        this.prosentase = prosentase;
    }

    /**
     * Mengembalikan daftar NIM mahasiswa yang dikecualikan dari ujian ini, disimpan
     * sebagai string dipisah koma. Mahasiswa dalam daftar ini tidak akan dapat mengakses ujian
     * meskipun terdaftar di pertemuan. Getter membersihkan format koma ganda secara otomatis.
     *
     * @return String daftar NIM yang tidak ikut, berformat ",NIM1,NIM2,", atau "" jika tidak ada
     */
    @Column(columnDefinition = "text")
    public String getMhsYgTidakIkut() {
        mhsYgTidakIkut = (mhsYgTidakIkut == null || mhsYgTidakIkut.trim().equalsIgnoreCase(",") ? ""
                : "," + mhsYgTidakIkut.trim() + ",")
                .replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

        if (mhsYgTidakIkut.equals(",")) {
            mhsYgTidakIkut = "";
        } else if (mhsYgTidakIkut.equals(",,")) {
            mhsYgTidakIkut = "";
        } else if (mhsYgTidakIkut.equals(",,,")) {
            mhsYgTidakIkut = "";
        }
        return mhsYgTidakIkut == null ? "" : mhsYgTidakIkut.trim();
    }

    /**
     * Menetapkan daftar NIM mahasiswa yang dikecualikan dari ujian ini.
     * Format: NIM dipisahkan koma, contoh: "12345,67890".
     *
     * @param mhsYgTidakIkut string NIM yang dikecualikan, dipisahkan koma
     */
    public void setMhsYgTidakIkut(String mhsYgTidakIkut) {
        this.mhsYgTidakIkut = mhsYgTidakIkut;
    }

    /**
     * Mengembalikan jumlah maksimum percobaan (boleh ikut) ujian yang diizinkan per peserta.
     * Jika belum dikonfigurasi atau kurang dari 1, mengambil nilai dari konfigurasi sistem
     * ({@code get_default_jumlah_boleh_ikut_ujian}). Default sistem: 5 kali percobaan.
     *
     * @return Integer jumlah maksimum percobaan yang diizinkan
     */
    public Integer getJumlahBolehIkut() {
        return jumlahBolehIkut == null || jumlahBolehIkut < 1 ? getDefaultJumlahBolehIkut() : jumlahBolehIkut;
    }

    /**
     * Menetapkan jumlah maksimum percobaan ujian yang diizinkan per peserta.
     * Set nilai kurang dari 1 atau null untuk menggunakan nilai default sistem.
     *
     * @param jumlahBolehIkut jumlah maksimum percobaan; null atau &lt;1 pakai default sistem
     */
    public void setJumlahBolehIkut(Integer jumlahBolehIkut) {
        this.jumlahBolehIkut = jumlahBolehIkut;
    }

    // ─── Getters & Setters: Konfigurasi Waktu Ujian ───────────────────────────────────────

    /**
     * Mengembalikan apakah akses ujian dibatasi berdasarkan rentang waktu
     * ({@link #getMulaiUjian()} hingga {@link #getSampaiUjian()}). Ketika {@code true},
     * mahasiswa hanya dapat mengakses ujian dalam rentang waktu tersebut.
     * Default: {@code true} (ujian dibatasi waktu).
     *
     * @return Boolean true jika ujian hanya dapat diakses dalam rentang waktu tertentu
     */
    public Boolean getDibatasiWaktu() {
        return dibatasiWaktu == null ? true : dibatasiWaktu;
    }

    /**
     * Menetapkan apakah akses ujian dibatasi berdasarkan rentang waktu.
     *
     * @param dibatasiWaktu true untuk membatasi akses berdasarkan rentang waktu
     */
    public void setDibatasiWaktu(Boolean dibatasiWaktu) {
        this.dibatasiWaktu = dibatasiWaktu;
    }

    /**
     * Mengembalikan apakah ujian ini disembunyikan dari daftar mahasiswa setelah
     * waktu ujian terlewat. Berguna untuk membersihkan tampilan dashboard mahasiswa
     * dari ujian-ujian yang sudah kadaluarsa. Default: {@code false}.
     *
     * @return Boolean true jika ujian disembunyikan setelah waktu berakhir
     */
    public Boolean getTidakDitampilkanJikaWaktuSudahTerlewat() {
        return tidakDitampilkanJikaWaktuSudahTerlewat == null ? false : tidakDitampilkanJikaWaktuSudahTerlewat;
    }

    /**
     * Menetapkan apakah ujian disembunyikan dari daftar mahasiswa setelah waktu berakhir.
     *
     * @param tidakDitampilkanJikaWaktuSudahTerlewat true untuk menyembunyikan ujian kadaluarsa
     */
    public void setTidakDitampilkanJikaWaktuSudahTerlewat(Boolean tidakDitampilkanJikaWaktuSudahTerlewat) {
        this.tidakDitampilkanJikaWaktuSudahTerlewat = tidakDitampilkanJikaWaktuSudahTerlewat;
    }

    /**
     * Mengembalikan batas waktu durasi pengerjaan ujian per peserta. Hanya bagian TIME
     * yang digunakan dari nilai Date ini (jam:menit:detik). Jika belum dikonfigurasi,
     * mengembalikan nilai default {@value #DURASI_DEFAULT_UJIAN} sesuai standar.
     *
     * @return Date representasi durasi pengerjaan (hanya komponen TIME yang digunakan)
     */
    @Temporal(TemporalType.TIME)
    @Column(name = "lama", nullable = true)
    public Date getLama() {
        if (lama == null) {
            try {
                lama = Common.timeFormat1.get().parse(DURASI_DEFAULT_UJIAN);
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.getLama");
            }
        }
        return lama;
    }

    /**
     * Menetapkan durasi pengerjaan ujian per peserta. Hanya komponen TIME (jam:menit:detik)
     * yang relevan dari nilai Date ini; komponen tanggal diabaikan oleh Hibernate.
     *
     * @param lama Date yang merepresentasikan durasi pengerjaan
     */
    public void setLama(Date lama) {
        this.lama = lama;
    }

    /**
     * Mengembalikan waktu mulai akses ujian. Jika belum dikonfigurasi secara eksplisit
     * dan pertemuan induk tersedia, mengambil waktu mulai dari {@link Pertemuan#getMulai()}
     * sebagai fallback yang praktis. Hanya berlaku jika {@link #getDibatasiWaktu()} adalah true.
     *
     * @return Date timestamp waktu mulai ujian, atau null jika tidak relevan
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "mulai_ujian", nullable = true)
    public Date getMulaiUjian() {
        if (mulaiUjian == null && pertemuan != null && pertemuan.getMulai() != null) {
            mulaiUjian = pertemuan.getMulai();
        }
        return mulaiUjian;
    }

    /**
     * Menetapkan waktu mulai akses ujian.
     *
     * @param mulaiUjian timestamp waktu buka ujian
     */
    public void setMulaiUjian(Date mulaiUjian) {
        this.mulaiUjian = mulaiUjian;
    }

    /**
     * Mengembalikan waktu tutup akses ujian. Jika belum dikonfigurasi dan pertemuan induk
     * tersedia, mengambil dari {@link Pertemuan#getSelesai()} sebagai fallback.
     * Jika ujian tidak dibatasi waktu ({@link #getDibatasiWaktu()} = false), mengembalikan
     * null untuk menandakan tidak ada batas waktu.
     *
     * @return Date timestamp waktu tutup ujian, atau null jika tidak dibatasi waktu
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "sampai_ujian", nullable = true)
    public Date getSampaiUjian() {
        if (sampaiUjian == null && pertemuan != null && pertemuan.getSelesai() != null) {
            sampaiUjian = pertemuan.getSelesai();
        }
        if (!getDibatasiWaktu()) {
            sampaiUjian = null;
        }
        return sampaiUjian;
    }

    /**
     * Menetapkan waktu tutup akses ujian.
     *
     * @param sampaiUjian timestamp waktu tutup ujian
     */
    public void setSampaiUjian(Date sampaiUjian) {
        this.sampaiUjian = sampaiUjian;
    }

    // ─── Getters & Setters: Konfigurasi Nilai & Format ────────────────────────────────────

    /**
     * Mengembalikan apakah peserta dapat melihat jawaban yang benar setelah ujian selesai.
     * Fitur ini membantu mahasiswa belajar dari kesalahan namun berisiko soal bocor.
     * Default: {@code false} (jawaban tidak ditampilkan setelah ujian).
     *
     * @return Boolean true jika jawaban benar ditampilkan setelah peserta selesai ujian
     */
    public Boolean getLihatJawabanSetelahUjian() {
        return lihatJawabanSetelahUjian == null ? false : lihatJawabanSetelahUjian;
    }

    /**
     * Menetapkan apakah peserta dapat melihat jawaban benar setelah ujian selesai.
     *
     * @param lihatJawabanSetelahUjian true untuk menampilkan jawaban benar setelah ujian
     */
    public void setLihatJawabanSetelahUjian(Boolean lihatJawabanSetelahUjian) {
        this.lihatJawabanSetelahUjian = lihatJawabanSetelahUjian;
    }

    /**
     * Mengembalikan apakah peserta dapat melihat nilai akhir mereka segera setelah ujian
     * selesai tanpa harus menunggu dosen memublikasikan nilai. Default: {@code false}.
     *
     * @return Boolean true jika nilai ditampilkan langsung setelah ujian selesai
     */
    public Boolean getLihatNilaiSetelahUjian() {
        return lihatNilaiSetelahUjian == null ? false : lihatNilaiSetelahUjian;
    }

    /**
     * Menetapkan apakah peserta dapat melihat nilai mereka segera setelah ujian selesai.
     *
     * @param lihatNilaiSetelahUjian true untuk menampilkan nilai langsung setelah ujian
     */
    public void setLihatNilaiSetelahUjian(Boolean lihatNilaiSetelahUjian) {
        this.lihatNilaiSetelahUjian = lihatNilaiSetelahUjian;
    }

    /**
     * Mengembalikan format nilai (Sub-CPMK tunggal dalam OBE) yang ditetapkan sebagai
     * format penilaian default untuk ujian ini. Untuk pemetaan soal ke beberapa Sub-CPMK,
     * gunakan {@link #getFormatNilais()} (JSON).
     *
     * @return FormatNilai entitas format nilai yang terkait, atau null jika tidak dikonfigurasi
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "format_nilai", nullable = true)
    public FormatNilai getFormatNilai() {
        return formatNilai;
    }

    /**
     * Menetapkan format nilai default untuk ujian ini.
     *
     * @param formatNilai entitas FormatNilai yang akan digunakan sebagai format penilaian default
     */
    public void setFormatNilai(FormatNilai formatNilai) {
        this.formatNilai = formatNilai;
    }

    /**
     * Mengembalikan pemetaan nomor soal ke Sub-CPMK ({@link FormatNilai}) dalam format JSON.
     * Format: {@code {"formatNilaiId": "1,2,3-5", ...}} di mana nilai adalah nomor soal
     * (bisa rentang seperti "3-5" atau satuan seperti "1,2"). Jika belum dikonfigurasi,
     * mengembalikan JSON kosong ({@code {}}) dari {@link Tugas#JSON}.
     *
     * @return String JSON pemetaan nomor soal ke Sub-CPMK; tidak pernah null
     */
    @Column(columnDefinition = "text")
    public String getFormatNilais() {
        return formatNilais == null || formatNilais.trim().isEmpty() ? Tugas.JSON : formatNilais;
    }

    /**
     * Menetapkan pemetaan nomor soal ke Sub-CPMK dalam format JSON.
     *
     * @param formatNilais JSON pemetaan nomor soal ke Sub-CPMK
     */
    public void setFormatNilais(String formatNilais) {
        this.formatNilais = formatNilais;
    }

    /**
     * Mengembalikan peta Sub-CPMK yang dikerjakan per peserta dalam format JSON, digunakan
     * pada ujian remedial OBE. Format: {@code {"mahasiswaId": ["formatNilaiId", ...]}}. Peserta
     * yang tidak punya entri di sini dianggap mengerjakan SEMUA Sub-CPMK yang terpilih di ujian
     * (default). Dipakai agar pada ujian remedial, nilai OBE seorang peserta hanya dihitung pada
     * Sub-CPMK yang benar-benar ia ulang. Jika belum dikonfigurasi, mengembalikan JSON kosong.
     *
     * @return String JSON pemetaan Sub-CPMK per peserta; tidak pernah null
     */
    @Column(name = "sub_cpmk_per_peserta", columnDefinition = "text")
    public String getSubCpmkPerPeserta() {
        return subCpmkPerPeserta == null || subCpmkPerPeserta.trim().isEmpty() ? Tugas.JSON : subCpmkPerPeserta;
    }

    /**
     * Menetapkan pemetaan Sub-CPMK per peserta dalam format JSON untuk ujian remedial.
     *
     * @param subCpmkPerPeserta JSON pemetaan Sub-CPMK per peserta; null diizinkan
     */
    public void setSubCpmkPerPeserta(String subCpmkPerPeserta) {
        this.subCpmkPerPeserta = subCpmkPerPeserta;
    }

    /**
     * Mengembalikan nilai manual per peserta per Sub-CPMK dalam format JSON, untuk kasus
     * peserta yang tidak ikut ujian tetapi membutuhkan nilai dari dosen.
     * Format: {@code {pesertaId: {fnId: nilai, paksa: bool}}}.
     *
     * @return String JSON nilai manual, atau null jika tidak ada nilai manual
     */
    @Column(name = "nilai_manual_json", columnDefinition = "text")
    public String getNilaiManualJson() {
        return nilaiManualJson;
    }

    /**
     * Menetapkan nilai manual per peserta per Sub-CPMK dalam format JSON.
     *
     * @param v JSON nilai manual; null untuk menghapus semua nilai manual
     */
    public void setNilaiManualJson(String v) {
        this.nilaiManualJson = v;
    }

    /**
     * Mengembalikan hasil review Penjaminan Mutu terhadap Analisis Butir Soal ujian ini
     * dalam format JSON. Format: {@code {"status":"menunggu|disetujui|perlu_revisi",
     * "catatan":"...","reviewerId":123,"reviewTime":"..."}}.
     *
     * @return String JSON hasil review PM, atau null jika belum pernah direview
     */
    @Column(name = "analisis_catatan_json", columnDefinition = "text")
    public String getAnalisisCatatanJson() {
        return analisisCatatanJson;
    }

    /**
     * Menetapkan hasil review Penjaminan Mutu dalam format JSON.
     *
     * @param analisisCatatanJson JSON hasil review; null untuk menghapus status review
     */
    public void setAnalisisCatatanJson(String analisisCatatanJson) {
        this.analisisCatatanJson = analisisCatatanJson;
    }

    // ─── Getters & Setters: Relasi Organisasi ─────────────────────────────────────────────

    /**
     * Mengembalikan fakultas yang terkait dengan ujian ini. Nilai selalu diambil dari
     * {@link Ujian#getFakultas()} jika tersedia, menimpa nilai lokal yang tersimpan.
     * Ini memastikan konsistensi organisasi antara bank soal dan konfigurasi ujian.
     *
     * @return Fakultas entitas fakultas yang terkait, atau null jika tidak tersedia
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "fakultas", nullable = true)
    public Fakultas getFakultas() {
        ujian = getUjian();
        if (ujian != null && ujian.getFakultas() != null) {
            fakultas = ujian.getFakultas();
        }
        fakultas = check(fakultas);
        return fakultas;
    }

    /**
     * Menetapkan fakultas untuk ujian ini. Perhatikan bahwa nilai ini akan ditimpa
     * oleh {@link Ujian#getFakultas()} saat getter dipanggil jika Ujian memiliki fakultas.
     *
     * @param fakultas entitas Fakultas yang akan dikaitkan
     */
    public void setFakultas(Fakultas fakultas) {
        this.fakultas = fakultas;
    }

    /**
     * Mengembalikan jurusan/program studi yang terkait dengan ujian ini. Nilai selalu
     * diambil dari {@link Ujian#getJurusan()} jika tersedia, memastikan konsistensi
     * organisasi antara bank soal dan konfigurasi ujian per-pertemuan.
     *
     * @return Jurusan entitas jurusan yang terkait, atau null jika tidak tersedia
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "jurusan", nullable = true)
    public Jurusan getJurusan() {
        ujian = getUjian();
        if (ujian != null && ujian.getJurusan() != null) {
            jurusan = ujian.getJurusan();
        }
        jurusan = check(jurusan);
        return jurusan;
    }

    /**
     * Menetapkan jurusan untuk ujian ini.
     *
     * @param jurusan entitas Jurusan yang akan dikaitkan
     */
    public void setJurusan(Jurusan jurusan) {
        this.jurusan = jurusan;
    }

    /**
     * Mengembalikan yayasan pemilik ujian ini dalam konteks multi-tenant. Nilai diambil
     * dari {@link Ujian#getYayasan()} jika tersedia untuk konsistensi kepemilikan data.
     * Relasi ini mendukung isolasi data antar-yayasan dalam instalasi multi-tenant.
     *
     * @return Yayasan entitas yayasan yang terkait, atau null jika tidak ada
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "yayasan", nullable = true)
    public Yayasan getYayasan() {
        yayasan = check(yayasan);
        ujian = getUjian();
        if (ujian != null && ujian.getYayasan() != null) {
            yayasan = ujian.getYayasan();
        }
        return yayasan;
    }

    /**
     * Menetapkan yayasan untuk ujian ini. Nilai null atau ID null akan disimpan sebagai null
     * untuk menjaga integritas foreign key.
     *
     * @param yayasan entitas Yayasan; null atau yayasan tanpa ID akan menghasilkan null
     */
    public void setYayasan(Yayasan yayasan) {
        this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
    }

    /**
     * Mengembalikan sekolah yang terkait dengan ujian ini dalam konteks multi-tenant sekolah.
     * Nilai diambil dari {@link Ujian#getSekolah()} jika tersedia untuk konsistensi data.
     *
     * @return Sekolah entitas sekolah yang terkait, atau null jika tidak ada
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "sekolah", nullable = true)
    public Sekolah getSekolah() {
        sekolah = check(sekolah);
        ujian = getUjian();
        if (ujian != null && ujian.getSekolah() != null) {
            sekolah = ujian.getSekolah();
        }
        return sekolah;
    }

    /**
     * Menetapkan sekolah untuk ujian ini. Null atau sekolah tanpa ID akan disimpan sebagai null.
     *
     * @param sekolah entitas Sekolah; null atau tanpa ID akan menghasilkan null
     */
    public void setSekolah(Sekolah sekolah) {
        this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
    }

    // ─── Getters & Setters: Relasi Penilaian Sekolah ──────────────────────────────────────

    /**
     * Mengembalikan jenis item penilaian siswa yang dikaitkan dengan ujian ini,
     * untuk integrasi dengan sistem penilaian sekolah (bukan perguruan tinggi).
     *
     * @return JenisItemPenilaianSiswa entitas yang terkait, atau null jika tidak ada
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "jenis_item_penilaian_siswa", nullable = true)
    public JenisItemPenilaianSiswa getJenisItemPenilaianSiswa() {
        jenisItemPenilaianSiswa = check(jenisItemPenilaianSiswa);
        return jenisItemPenilaianSiswa;
    }

    /**
     * Menetapkan jenis item penilaian siswa untuk integrasi dengan sistem penilaian sekolah.
     *
     * @param jenisItemPenilaianSiswa entitas yang akan dikaitkan
     */
    public void setJenisItemPenilaianSiswa(JenisItemPenilaianSiswa jenisItemPenilaianSiswa) {
        this.jenisItemPenilaianSiswa = jenisItemPenilaianSiswa;
    }

    /**
     * Mengembalikan grup kategori item penilaian siswa yang dikaitkan dengan ujian ini,
     * untuk pengelompokan penilaian dalam sistem raport sekolah.
     *
     * @return GrupKategoriItemPenilaianSiswa entitas yang terkait, atau null jika tidak ada
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "grup_kategori_item_penilaian_siswa", nullable = true)
    public GrupKategoriItemPenilaianSiswa getGrupKategoriItemPenilaianSiswa() {
        grupKategoriItemPenilaianSiswa = check(grupKategoriItemPenilaianSiswa);
        return grupKategoriItemPenilaianSiswa;
    }

    /**
     * Menetapkan grup kategori item penilaian siswa untuk integrasi raport sekolah.
     *
     * @param grupKategoriItemPenilaianSiswa entitas yang akan dikaitkan
     */
    public void setGrupKategoriItemPenilaianSiswa(GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa) {
        this.grupKategoriItemPenilaianSiswa = grupKategoriItemPenilaianSiswa;
    }

    /**
     * Mengembalikan grup penilaian yang dikaitkan dengan ujian ini, mengelompokkan
     * ujian dalam hierarki penilaian sekolah (misalnya: Pengetahuan, Keterampilan).
     *
     * @return GrupPenilaian entitas yang terkait, atau null jika tidak ada
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "grup_penilaian", nullable = true)
    public GrupPenilaian getGrupPenilaian() {
        grupPenilaian = check(grupPenilaian);
        return grupPenilaian;
    }

    /**
     * Menetapkan grup penilaian untuk ujian ini.
     *
     * @param grupPenilaian entitas GrupPenilaian yang akan dikaitkan
     */
    public void setGrupPenilaian(GrupPenilaian grupPenilaian) {
        this.grupPenilaian = grupPenilaian;
    }

    /**
     * Mengembalikan detail grup penilaian yang dikaitkan dengan ujian ini, berisi
     * informasi lebih rinci tentang sub-grup dalam hierarki penilaian sekolah.
     *
     * @return DetailGrupPenilaian entitas yang terkait, atau null jika tidak ada
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "detail_grup_penilaian", nullable = true)
    public DetailGrupPenilaian getDetailGrupPenilaian() {
        detailGrupPenilaian = check(detailGrupPenilaian);
        return detailGrupPenilaian;
    }

    /**
     * Menetapkan detail grup penilaian untuk ujian ini.
     *
     * @param detailGrupPenilaian entitas DetailGrupPenilaian yang akan dikaitkan
     */
    public void setDetailGrupPenilaian(DetailGrupPenilaian detailGrupPenilaian) {
        this.detailGrupPenilaian = detailGrupPenilaian;
    }

    // ─── Getters & Setters: Anti-Curang (CBT) Per-Ujian ──────────────────────────────────

    /**
     * Master toggle anti-curang untuk ujian ini. Ketika {@code false}, tidak ada sub-fitur
     * anti-curang yang berjalan meskipun sub-fiturnya dikonfigurasi aktif. Ini memungkinkan
     * dosen menonaktifkan semua proteksi dengan satu klik. <b>Default: {@code false}</b>
     * (anti-curang harus di-opt-in secara eksplisit per ujian).
     *
     * @return Boolean false jika anti-curang tidak aktif; true jika seluruh sistem anti-curang berjalan
     */
    @Column(name = "ac_aktif")
    public Boolean getAntiCurangAktif() {
        return antiCurangAktif == null ? Boolean.FALSE : antiCurangAktif;
    }

    /**
     * Menetapkan master toggle anti-curang untuk ujian ini.
     *
     * @param antiCurangAktif true untuk mengaktifkan seluruh sistem anti-curang
     */
    public void setAntiCurangAktif(Boolean antiCurangAktif) {
        this.antiCurangAktif = antiCurangAktif;
    }

    /**
     * Mengembalikan batas jumlah pelanggaran anti-curang yang diizinkan sebelum ujian
     * dianggap selesai secara otomatis. Nilai 0 berarti pelanggaran hanya dicatat tanpa
     * batas (tidak ada terminasi otomatis). Default: 3 pelanggaran.
     *
     * @return Integer batas pelanggaran; 0 berarti tanpa batas, hanya dicatat
     */
    @Column(name = "ac_batas_pelanggaran")
    public Integer getAntiCurangBatasPelanggaran() {
        return antiCurangBatasPelanggaran == null ? Integer.valueOf(3) : antiCurangBatasPelanggaran;
    }

    /**
     * Menetapkan batas jumlah pelanggaran sebelum ujian berakhir otomatis.
     *
     * @param antiCurangBatasPelanggaran batas pelanggaran; 0 untuk hanya mencatat tanpa batas
     */
    public void setAntiCurangBatasPelanggaran(Integer antiCurangBatasPelanggaran) {
        this.antiCurangBatasPelanggaran = antiCurangBatasPelanggaran;
    }

    /**
     * Mengembalikan apakah browser masuk ke mode layar penuh (fullscreen) secara otomatis
     * saat peserta memulai ujian. Fullscreen mencegah peserta mengakses jendela lain
     * dengan mudah. Default: {@code true} (aktifkan fullscreen otomatis).
     *
     * @return Boolean true jika fullscreen otomatis diaktifkan saat ujian dimulai
     */
    @Column(name = "ac_aktifkan_fullscreen")
    public Boolean getAntiCurangAktifkanFullscreen() {
        return antiCurangAktifkanFullscreen == null ? Boolean.TRUE : antiCurangAktifkanFullscreen;
    }

    /**
     * Menetapkan apakah browser masuk ke mode fullscreen otomatis saat ujian dimulai.
     *
     * @param v true untuk mengaktifkan fullscreen otomatis
     */
    public void setAntiCurangAktifkanFullscreen(Boolean v) {
        this.antiCurangAktifkanFullscreen = v;
    }

    /**
     * Mengembalikan apakah sistem mendeteksi ketika peserta berpindah tab atau aplikasi lain
     * menggunakan Page Visibility API browser. Setiap perpindahan tab dihitung sebagai
     * satu pelanggaran. Default: {@code true} (deteksi aktif).
     *
     * @return Boolean true jika deteksi perpindahan tab diaktifkan
     */
    @Column(name = "ac_deteksi_pindah_tab")
    public Boolean getAntiCurangDeteksiPindahTab() {
        return antiCurangDeteksiPindahTab == null ? Boolean.TRUE : antiCurangDeteksiPindahTab;
    }

    /**
     * Menetapkan apakah perpindahan tab atau aplikasi dideteksi sebagai pelanggaran.
     *
     * @param v true untuk mengaktifkan deteksi perpindahan tab
     */
    public void setAntiCurangDeteksiPindahTab(Boolean v) {
        this.antiCurangDeteksiPindahTab = v;
    }

    /**
     * Mengembalikan apakah sistem mendeteksi ketika jendela browser kehilangan fokus
     * (window blur), misalnya saat peserta menekan Alt+Tab atau mengklik area di luar browser.
     * Default: {@code true} (deteksi aktif).
     *
     * @return Boolean true jika deteksi window blur diaktifkan
     */
    @Column(name = "ac_deteksi_blur_jendela")
    public Boolean getAntiCurangDeteksiBlurJendela() {
        return antiCurangDeteksiBlurJendela == null ? Boolean.TRUE : antiCurangDeteksiBlurJendela;
    }

    /**
     * Menetapkan apakah kehilangan fokus jendela (window blur) dideteksi sebagai pelanggaran.
     *
     * @param v true untuk mengaktifkan deteksi window blur
     */
    public void setAntiCurangDeteksiBlurJendela(Boolean v) {
        this.antiCurangDeteksiBlurJendela = v;
    }

    /**
     * Mengembalikan jeda (debounce) waktu dalam milidetik untuk deteksi blur, mencegah satu
     * kejadian Alt+Tab dihitung ganda akibat event blur dan visibilitychange yang muncul
     * bersamaan. Default: 5000 ms (5 detik).
     *
     * @return Integer jeda debounce dalam milidetik
     */
    @Column(name = "ac_cooldown_blur_ms")
    public Integer getAntiCurangCooldownBlurMs() {
        return antiCurangCooldownBlurMs == null ? Integer.valueOf(5000) : antiCurangCooldownBlurMs;
    }

    /**
     * Menetapkan jeda debounce untuk deteksi blur dalam milidetik.
     *
     * @param v jeda dalam milidetik; nilai lebih kecil berarti lebih sensitif
     */
    public void setAntiCurangCooldownBlurMs(Integer v) {
        this.antiCurangCooldownBlurMs = v;
    }

    /**
     * Mengembalikan apakah sistem mendeteksi ketika peserta keluar dari mode layar penuh
     * selama ujian berlangsung. Keluar fullscreen dihitung sebagai pelanggaran.
     * Default: {@code true} (deteksi aktif).
     *
     * @return Boolean true jika deteksi keluar fullscreen diaktifkan
     */
    @Column(name = "ac_deteksi_keluar_fullscreen")
    public Boolean getAntiCurangDeteksiKeluarFullscreen() {
        return antiCurangDeteksiKeluarFullscreen == null ? Boolean.TRUE : antiCurangDeteksiKeluarFullscreen;
    }

    /**
     * Menetapkan apakah keluar dari mode fullscreen selama ujian dideteksi sebagai pelanggaran.
     *
     * @param v true untuk mengaktifkan deteksi keluar fullscreen
     */
    public void setAntiCurangDeteksiKeluarFullscreen(Boolean v) {
        this.antiCurangDeteksiKeluarFullscreen = v;
    }

    /**
     * Mengembalikan apakah menu klik kanan (context menu) diblokir selama ujian berlangsung.
     * Memblokir klik kanan mencegah peserta menggunakan opsi "Inspect", "View Source",
     * atau fitur browser lain yang dapat dimanfaatkan untuk curang. Default: {@code true}.
     *
     * @return Boolean true jika klik kanan diblokir selama ujian
     */
    @Column(name = "ac_blokir_klik_kanan")
    public Boolean getAntiCurangBlokirKlikKanan() {
        return antiCurangBlokirKlikKanan == null ? Boolean.TRUE : antiCurangBlokirKlikKanan;
    }

    /**
     * Menetapkan apakah menu klik kanan diblokir selama ujian berlangsung.
     *
     * @param v true untuk memblokir klik kanan
     */
    public void setAntiCurangBlokirKlikKanan(Boolean v) {
        this.antiCurangBlokirKlikKanan = v;
    }

    /**
     * Mengembalikan apakah shortcut keyboard berbahaya diblokir selama ujian, termasuk
     * Ctrl+W (tutup tab), Ctrl+T (tab baru), Ctrl+N (jendela baru), Ctrl+R (refresh),
     * F5 (refresh), F12 (DevTools), dan Alt+F4 (tutup jendela). Default: {@code true}.
     *
     * @return Boolean true jika shortcut berbahaya diblokir selama ujian
     */
    @Column(name = "ac_blokir_shortcut")
    public Boolean getAntiCurangBlokirShortcut() {
        return antiCurangBlokirShortcut == null ? Boolean.TRUE : antiCurangBlokirShortcut;
    }

    /**
     * Menetapkan apakah shortcut keyboard berbahaya diblokir selama ujian.
     *
     * @param v true untuk memblokir shortcut berbahaya
     */
    public void setAntiCurangBlokirShortcut(Boolean v) {
        this.antiCurangBlokirShortcut = v;
    }

    /**
     * Mengembalikan apakah dialog konfirmasi ditampilkan saat peserta mencoba meninggalkan
     * halaman ujian (event beforeunload browser). Dialog ini memberi kesempatan peserta
     * untuk membatalkan navigasi yang tidak disengaja. Default: {@code true}.
     *
     * @return Boolean true jika konfirmasi sebelum meninggalkan halaman diaktifkan
     */
    @Column(name = "ac_peringatan_keluar_halaman")
    public Boolean getAntiCurangPeringatanKeluarHalaman() {
        return antiCurangPeringatanKeluarHalaman == null ? Boolean.TRUE : antiCurangPeringatanKeluarHalaman;
    }

    /**
     * Menetapkan apakah dialog konfirmasi ditampilkan saat peserta akan meninggalkan halaman.
     *
     * @param v true untuk menampilkan dialog konfirmasi sebelum meninggalkan halaman ujian
     */
    public void setAntiCurangPeringatanKeluarHalaman(Boolean v) {
        this.antiCurangPeringatanKeluarHalaman = v;
    }

    /**
     * Mengembalikan teks pesan peringatan yang ditampilkan ketika sistem mendeteksi peserta
     * berpindah tab atau aplikasi lain. Jika belum dikonfigurasi, mengembalikan
     * {@link #DEF_AC_PESAN_PINDAH_TAB} sebagai teks default.
     *
     * @return String teks peringatan perpindahan tab; tidak pernah null
     */
    @Column(name = "ac_pesan_pindah_tab", columnDefinition = "text")
    public String getAntiCurangPesanPindahTab() {
        return antiCurangPesanPindahTab == null || antiCurangPesanPindahTab.trim().isEmpty()
                ? DEF_AC_PESAN_PINDAH_TAB
                : antiCurangPesanPindahTab;
    }

    /**
     * Menetapkan teks pesan peringatan untuk deteksi perpindahan tab atau aplikasi.
     *
     * @param v teks peringatan; null atau blank akan menggunakan teks default
     */
    public void setAntiCurangPesanPindahTab(String v) {
        this.antiCurangPesanPindahTab = v;
    }

    /**
     * Mengembalikan teks pesan peringatan yang ditampilkan ketika sistem mendeteksi
     * jendela browser kehilangan fokus (window blur). Jika belum dikonfigurasi,
     * mengembalikan {@link #DEF_AC_PESAN_BLUR} sebagai teks default.
     *
     * @return String teks peringatan window blur; tidak pernah null
     */
    @Column(name = "ac_pesan_blur_jendela", columnDefinition = "text")
    public String getAntiCurangPesanBlurJendela() {
        return antiCurangPesanBlurJendela == null || antiCurangPesanBlurJendela.trim().isEmpty()
                ? DEF_AC_PESAN_BLUR
                : antiCurangPesanBlurJendela;
    }

    /**
     * Menetapkan teks pesan peringatan untuk deteksi window blur.
     *
     * @param v teks peringatan; null atau blank akan menggunakan teks default
     */
    public void setAntiCurangPesanBlurJendela(String v) {
        this.antiCurangPesanBlurJendela = v;
    }

    /**
     * Mengembalikan teks pesan peringatan yang ditampilkan ketika peserta keluar dari
     * mode layar penuh. Jika belum dikonfigurasi, mengembalikan {@link #DEF_AC_PESAN_KELUAR_FS}.
     *
     * @return String teks peringatan keluar fullscreen; tidak pernah null
     */
    @Column(name = "ac_pesan_keluar_fullscreen", columnDefinition = "text")
    public String getAntiCurangPesanKeluarFullscreen() {
        return antiCurangPesanKeluarFullscreen == null || antiCurangPesanKeluarFullscreen.trim().isEmpty()
                ? DEF_AC_PESAN_KELUAR_FS
                : antiCurangPesanKeluarFullscreen;
    }

    /**
     * Menetapkan teks pesan peringatan untuk deteksi keluar dari mode fullscreen.
     *
     * @param v teks peringatan; null atau blank akan menggunakan teks default
     */
    public void setAntiCurangPesanKeluarFullscreen(String v) {
        this.antiCurangPesanKeluarFullscreen = v;
    }

    /**
     * Mengembalikan teks pesan konfirmasi yang ditampilkan saat peserta mencoba meninggalkan
     * halaman ujian. Jika belum dikonfigurasi, mengembalikan {@link #DEF_AC_PESAN_KELUAR_HAL}.
     *
     * @return String teks konfirmasi meninggalkan halaman ujian; tidak pernah null
     */
    @Column(name = "ac_pesan_keluar_halaman", columnDefinition = "text")
    public String getAntiCurangPesanKeluarHalaman() {
        return antiCurangPesanKeluarHalaman == null || antiCurangPesanKeluarHalaman.trim().isEmpty()
                ? DEF_AC_PESAN_KELUAR_HAL
                : antiCurangPesanKeluarHalaman;
    }

    /**
     * Menetapkan teks konfirmasi untuk event beforeunload saat peserta meninggalkan halaman.
     *
     * @param v teks konfirmasi; null atau blank akan menggunakan teks default
     */
    public void setAntiCurangPesanKeluarHalaman(String v) {
        this.antiCurangPesanKeluarHalaman = v;
    }

    /**
     * Mengembalikan apakah tombol PrintScreen (tangkap layar) diblokir selama ujian.
     * Memblokir screenshot mencegah peserta mendokumentasikan soal untuk disebarkan.
     * Default: {@code true} (PrintScreen diblokir).
     *
     * @return Boolean true jika tombol tangkap layar diblokir selama ujian
     */
    @Column(name = "ac_blokir_tangkap_layar")
    public Boolean getAntiCurangBlokirTangkapLayar() {
        return antiCurangBlokirTangkapLayar == null ? Boolean.TRUE : antiCurangBlokirTangkapLayar;
    }

    /**
     * Menetapkan apakah tombol tangkap layar (PrintScreen) diblokir selama ujian.
     *
     * @param v true untuk memblokir tangkap layar
     */
    public void setAntiCurangBlokirTangkapLayar(Boolean v) {
        this.antiCurangBlokirTangkapLayar = v;
    }

    /**
     * Mengembalikan apakah peserta dilarang mengerjakan ujian dari lebih dari satu perangkat
     * secara bersamaan. Pembatasan multi-device mencegah berbagi sesi ujian. Default: {@code true}.
     *
     * @return Boolean true jika penggunaan perangkat bersamaan dilarang
     */
    @Column(name = "ac_larang_multi_device")
    public Boolean getAntiCurangLarangMultiDevice() {
        return antiCurangLarangMultiDevice == null ? Boolean.TRUE : antiCurangLarangMultiDevice;
    }

    /**
     * Menetapkan apakah pengerjaan ujian dari beberapa perangkat bersamaan dilarang.
     *
     * @param v true untuk melarang multi-device
     */
    public void setAntiCurangLarangMultiDevice(Boolean v) {
        this.antiCurangLarangMultiDevice = v;
    }

    /**
     * Mengembalikan latitude koordinat GPS pusat lokasi yang diizinkan untuk mengerjakan ujian.
     * Digunakan bersama {@link #getAntiCurangLokasiLongitude()} dan
     * {@link #getAntiCurangLokasiRadius()} untuk restriksi lokasi. Null berarti tidak ada
     * restriksi lokasi.
     *
     * @return Double latitude koordinat pusat lokasi, atau null jika tidak ada restriksi
     */
    @Column(name = "ac_lokasi_latitude")
    public Double getAntiCurangLokasiLatitude() {
        return antiCurangLokasiLatitude;
    }

    /**
     * Menetapkan latitude koordinat GPS pusat lokasi yang diizinkan untuk ujian.
     *
     * @param v nilai latitude; null untuk menonaktifkan restriksi lokasi
     */
    public void setAntiCurangLokasiLatitude(Double v) {
        this.antiCurangLokasiLatitude = v;
    }

    /**
     * Mengembalikan longitude koordinat GPS pusat lokasi yang diizinkan untuk mengerjakan ujian.
     * Digunakan bersama {@link #getAntiCurangLokasiLatitude()} dan
     * {@link #getAntiCurangLokasiRadius()}. Null berarti tidak ada restriksi lokasi.
     *
     * @return Double longitude koordinat pusat lokasi, atau null jika tidak ada restriksi
     */
    @Column(name = "ac_lokasi_longitude")
    public Double getAntiCurangLokasiLongitude() {
        return antiCurangLokasiLongitude;
    }

    /**
     * Menetapkan longitude koordinat GPS pusat lokasi yang diizinkan untuk ujian.
     *
     * @param v nilai longitude; null untuk menonaktifkan restriksi lokasi
     */
    public void setAntiCurangLokasiLongitude(Double v) {
        this.antiCurangLokasiLongitude = v;
    }

    /**
     * Mengembalikan radius (dalam meter) dari titik pusat GPS yang masih diizinkan untuk
     * mengerjakan ujian. Peserta di luar radius ini akan ditolak aksesnya. Nilai 0 atau null
     * berarti restriksi lokasi tidak aktif.
     *
     * @return Integer radius dalam meter, atau null jika restriksi lokasi tidak aktif
     */
    @Column(name = "ac_lokasi_radius_meter")
    public Integer getAntiCurangLokasiRadius() {
        return antiCurangLokasiRadius;
    }

    /**
     * Menetapkan radius dalam meter untuk restriksi lokasi ujian berbasis GPS.
     *
     * @param v radius dalam meter; null atau 0 untuk menonaktifkan restriksi lokasi
     */
    public void setAntiCurangLokasiRadius(Integer v) {
        this.antiCurangLokasiRadius = v;
    }

    // ─── Helper Methods ───────────────────────────────────────────────────────────────────

    /**
     * Mengembalikan {@code true} jika fitur anti-curang secara efektif aktif untuk ujian ini.
     * Method ini adalah shortcut yang lebih ekspresif dibanding memanggil {@link #getAntiCurangAktif()}
     * secara langsung, berguna dalam kondisional logika bisnis.
     *
     * @return boolean true jika anti-curang aktif dan tidak null; false dalam semua kasus lain
     */
    @javax.persistence.Transient
    public boolean isAntiCurangAktifEfektif() {
        return antiCurangAktif != null && antiCurangAktif;
    }

    /**
     * Mengembalikan {@code true} jika waktu ujian saat ini masih dalam rentang waktu yang valid
     * (antara {@link #getMulaiUjian()} dan {@link #getSampaiUjian()}). Berguna untuk menentukan
     * apakah peserta masih bisa mengakses ujian tanpa harus memeriksa setiap field secara manual.
     * Jika ujian tidak dibatasi waktu ({@link #getDibatasiWaktu()} = false), selalu mengembalikan
     * {@code true} karena tidak ada batas waktu yang perlu diperiksa.
     *
     * @return boolean true jika waktu masih dalam rentang valid atau ujian tidak dibatasi waktu
     */
    @javax.persistence.Transient
    public boolean isWaktuUjianAktif() {
        if (!getDibatasiWaktu()) {
            return true;
        }
        Date sekarang = ais.ui.util.WaktuUtil.getCalendar().getTime();
        Date mulai = getMulaiUjian();
        Date sampai = getSampaiUjian();
        if (mulai != null && sekarang.before(mulai)) {
            return false;
        }
        if (sampai != null && sekarang.after(sampai)) {
            return false;
        }
        return true;
    }

    /**
     * Membangun peta (TreeMap) nomor soal ke {@link FormatNilai} berdasarkan konfigurasi
     * {@link #getFormatNilais()} JSON. Mendukung format nomor soal satuan (misalnya "1,2,3")
     * maupun rentang (misalnya "3-5" yang berarti soal 3, 4, dan 5). Nomor yang dimasukkan
     * terbalik (misalnya "9-1") ditangani otomatis menggunakan Math.min/max. Baris yang tidak
     * valid diabaikan agar proses tetap berjalan untuk soal-soal berikutnya.
     *
     * @param formatNilais daftar Sub-CPMK ({@link FormatNilai}) yang akan dipetakan
     * @return TreeMap berisi pemetaan nomor soal (Integer) ke FormatNilai yang sesuai
     * @throws Exception jika terjadi kesalahan parsing JSON dari {@link #getFormatNilais()}
     */
    public TreeMap<Integer, FormatNilai> ambilMapNomor(List<FormatNilai> formatNilais) throws Exception {
        TreeMap<Integer, FormatNilai> treeMap = new TreeMap<Integer, FormatNilai>();
        JSONObject jsonObject = new JSONObject(this.getFormatNilais());

        for (FormatNilai nilai : formatNilais) {
            if (nilai.getStatusPertemuan() != null) {
                String nilaiData = jsonObject.isNull(nilai.getId().toString()) ? ""
                        : (jsonObject.get(nilai.getId().toString()) + "");

                for (String part : nilaiData.split(",")) {
                    part = part.trim();
                    if (part.isEmpty()) {
                        continue;
                    }

                    try {
                        if (part.contains("-")) {
                            String[] range = part.split("-");
                            if (range.length == 2) {
                                int start = Integer.parseInt(range[0].trim());
                                int end = Integer.parseInt(range[1].trim());
                                int min = Math.min(start, end);
                                int max = Math.max(start, end);
                                for (int i = min; i <= max; i++) {
                                    treeMap.put(i, nilai);
                                }
                            }
                        } else {
                            Integer noData = Integer.parseInt(part);
                            treeMap.put(noData, nilai);
                        }
                    } catch (Exception e) {
                        ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.ambilMapNomor");
                    }
                }
            }
        }
        return treeMap;
    }

    /**
     * Mengembalikan himpunan ID Sub-CPMK ({@link FormatNilai}) yang dikerjakan oleh peserta
     * dengan ID {@code mahasiswaId} pada ujian ini, berdasarkan data {@link #getSubCpmkPerPeserta()}.
     * Mengembalikan {@code null} bila peserta tidak punya entri dalam JSON tersebut, yang berarti
     * peserta mengerjakan SEMUA Sub-CPMK yang terpilih di ujian (tidak ada batasan).
     * Himpunan kosong setelah parsing juga dikembalikan sebagai null dengan semantik yang sama.
     *
     * @param mahasiswaId ID mahasiswa peserta ujian yang akan dicari Sub-CPMK-nya
     * @return Set berisi ID Sub-CPMK yang dikerjakan, atau null jika tanpa batasan (semua Sub-CPMK)
     */
    public java.util.Set<Long> ambilSubCpmkPeserta(Long mahasiswaId) {
        if (mahasiswaId == null) {
            return null;
        }
        try {
            org.json.JSONObject j = new org.json.JSONObject(getSubCpmkPerPeserta());
            String key = mahasiswaId.toString();
            if (j.isNull(key)) {
                return null;
            }
            org.json.JSONArray arr = j.getJSONArray(key);
            java.util.Set<Long> set = new java.util.HashSet<Long>();
            for (int i = 0; i < arr.length(); i++) {
                try {
                    set.add(Long.valueOf(arr.get(i).toString()));
                } catch (Exception e) {
                    ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.ambilSubCpmkPeserta.parseId");
                }
            }
            if (set.isEmpty()) {
                return null;
            }
            return set;
        } catch (Exception e) {
            return null;
        }
    }

    // ─── Cache Hasil Ujian Mahasiswa ──────────────────────────────────────────────────────

    /**
     * Membaca isi cache hasil ujian mahasiswa dari file sistem. Cache disimpan sebagai JSON
     * berisi ID dari setiap {@link HasilUjianMahasiswa} yang terkait dengan ujian ini.
     * Jika file tidak ditemukan atau terjadi kesalahan, mengembalikan JSON kosong
     * ({@link VOMahasiswa#dataJSON}) sebagai fallback aman.
     *
     * @return String isi JSON cache; tidak pernah null, minimal berupa JSON kosong
     */
    public String ambilLokasiHasilUjianMahasiswa() {
        File file = Common.getFileLocation(this, "pertemuanPunyaUjian_punya_detail_" + getId().toString());
        try {
            String data = ais.common.BacaTulisUtil.baca(file);
            return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.ambilLokasiHasilUjianMahasiswa");
        }
        return VOMahasiswa.dataJSON;
    }

    /**
     * Menulis data JSON ke file cache hasil ujian mahasiswa. Cache ini berisi pemetaan
     * ID {@link HasilUjianMahasiswa} yang terkait dengan ujian ini, digunakan untuk
     * menghindari query N+1 saat memuat daftar peserta ujian.
     *
     * @param data String JSON yang akan ditulis ke file cache
     */
    public void tulisLokasiHasilUjianMahasiswa(String data) {
        File file = Common.getFileLocation(this, "pertemuanPunyaUjian_punya_detail_" + getId().toString());
        try {
            ais.common.BacaTulisUtil.tulis(file, data);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.tulisLokasiHasilUjianMahasiswa");
        }
    }

    /**
     * Menghapus file cache hasil ujian mahasiswa untuk memaksa reload data dari database
     * pada akses berikutnya. Harus dipanggil setiap kali ada perubahan pada daftar peserta
     * ujian agar cache tidak stale.
     */
    public void bersihkanLokasiHasilUjianMahasiswa() {
        File file = Common.getFileLocation(this, "pertemuanPunyaUjian_punya_detail_" + getId().toString());
        BacaTulisUtil.doHapus(file, "pertemuanPunyaUjian_punya_detail");
    }

    /**
     * Mereinisialisasi cache hasil ujian mahasiswa dengan membaca ulang seluruh data peserta
     * dari database melalui session Hibernate yang diberikan. Proses ini membersihkan cache
     * lama terlebih dahulu, lalu memuat ulang semua {@link HasilUjianMahasiswa} yang terkait
     * dan menulisnya kembali ke file cache. Dipanggil saat cache dianggap stale atau pertama kali
     * diinisialisasi.
     *
     * @param session Session Hibernate aktif yang akan digunakan untuk query; jika null, operasi diabaikan
     */
    @SuppressWarnings("unchecked")
    public void reInitHasilUjianMahasiswa(Session session) {
        if (session != null) {
            List<Long> hasilUjianMahasiswasId = session.createCriteria(HasilUjianMahasiswa.class)
                    .addOrder(Order.asc("id"))
                    .add(Restrictions.eq("pertemuanPunyaUjian", this))
                    .setProjection(Projections.property("id"))
                    .list();
            bersihkanLokasiHasilUjianMahasiswa();
            tulisLokasiHasilUjianMahasiswa(new JSONObject().toString());
            for (Long hasilUjianMahasiswaId : hasilUjianMahasiswasId) {
                HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) ambilData(
                        HasilUjianMahasiswa.class, hasilUjianMahasiswaId.toString());
                if (hasilUjianMahasiswa == null) {
                    hasilUjianMahasiswa = (HasilUjianMahasiswa) session
                            .createCriteria(HasilUjianMahasiswa.class)
                            .add(Restrictions.idEq(hasilUjianMahasiswaId))
                            .uniqueResult();
                    masukkanData(HasilUjianMahasiswa.class, hasilUjianMahasiswa);
                }
                populateHasilUjianMahasiswa(hasilUjianMahasiswa, true);
            }
            hasilUjianMahasiswasId.clear();
            hasilUjianMahasiswasId = null;
        }
    }

    /**
     * Menghapus entri {@link HasilUjianMahasiswa} dengan ID tertentu dari cache file JSON.
     * Entri dihapus dengan menetapkan nilainya menjadi string kosong dalam JSON, yang dikenali
     * sebagai "tidak valid" saat cache dibaca kembali.
     *
     * @param id ID dari HasilUjianMahasiswa yang akan dihapus dari cache
     */
    public void removeHasilUjianMahasiswa(Serializable id) {
        try {
            JSONObject c = new JSONObject(ambilLokasiHasilUjianMahasiswa());
            c.put(id.toString(), "");
            tulisLokasiHasilUjianMahasiswa(c.toString());
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.removeHasilUjianMahasiswa");
        }
    }

    /**
     * Menambahkan atau memperbarui entri {@link HasilUjianMahasiswa} dalam cache file JSON.
     * Cache menggunakan ID sebagai kunci dan nilai, sehingga entri yang sama tidak terduplikasi.
     * Parameter {@code tulisUlang} tersedia untuk keperluan masa depan namun saat ini diabaikan.
     *
     * @param hasilUjianMahasiswa objek HasilUjianMahasiswa yang akan ditambahkan ke cache; diabaikan jika null
     * @param tulisUlang          flag untuk keperluan masa depan; saat ini tidak mempengaruhi perilaku
     */
    public void populateHasilUjianMahasiswa(HasilUjianMahasiswa hasilUjianMahasiswa, boolean tulisUlang) {
        try {
            if (hasilUjianMahasiswa == null) {
                return;
            }
            JSONObject c = new JSONObject(ambilLokasiHasilUjianMahasiswa());
            c.put(hasilUjianMahasiswa.getId().toString(), hasilUjianMahasiswa.getId().toString());
            tulisLokasiHasilUjianMahasiswa(c.toString());
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.populateHasilUjianMahasiswa");
        }
    }

    /**
     * Mengembalikan daftar ID semua {@link HasilUjianMahasiswa} yang terkait dengan ujian ini,
     * menggunakan cache file dan melakukan refresh jika diperlukan. Shortcut untuk
     * {@link #ambilHasilUjianMahasiswa(Mahasiswa, BiodataCalonMahasiswa, boolean, boolean)}
     * dengan semua parameter default (semua peserta, tanpa refresh paksa).
     *
     * @return List berisi ID HasilUjianMahasiswa; tidak pernah null, bisa kosong
     */
    public List<Long> ambilHasilUjianMahasiswa() {
        return ambilHasilUjianMahasiswa(null, null, false, false);
    }

    /**
     * Mengembalikan daftar ID semua {@link HasilUjianMahasiswa} yang terkait, dengan opsi
     * untuk memaksa refresh cache dari database. Digunakan saat ada perubahan data yang
     * perlu segera tercermin tanpa menunggu invalidasi cache otomatis.
     *
     * @param refresh true untuk memaksa reload cache dari database; false menggunakan cache yang ada
     * @return List berisi ID HasilUjianMahasiswa; tidak pernah null, bisa kosong
     */
    public List<Long> ambilHasilUjianMahasiswa(boolean refresh) {
        return ambilHasilUjianMahasiswa(null, null, refresh, false);
    }

    /**
     * Mengembalikan daftar ID {@link HasilUjianMahasiswa} yang difilter berdasarkan mahasiswa
     * atau calon mahasiswa tertentu, dengan opsi untuk hanya mengambil peserta yang sudah
     * benar-benar mengerjakan ujian (jumlah ikut lebih dari 0).
     *
     * @param mahasiswa              filter berdasarkan entitas Mahasiswa; null untuk semua peserta
     * @param biodataCalonMahasiswa  filter berdasarkan calon mahasiswa; null untuk semua peserta
     * @param hanyaYgIkut            true untuk hanya mengambil peserta yang sudah ikut ujian
     * @return List berisi ID HasilUjianMahasiswa yang sesuai filter; tidak pernah null
     */
    public List<Long> ambilHasilUjianMahasiswa(Mahasiswa mahasiswa,
            BiodataCalonMahasiswa biodataCalonMahasiswa, boolean hanyaYgIkut) {
        return ambilHasilUjianMahasiswa(mahasiswa, biodataCalonMahasiswa, false, hanyaYgIkut);
    }

    /**
     * Mengembalikan jumlah peserta yang sudah benar-benar ikut mengerjakan ujian ini
     * (mulai mengerjakan, bukan sekadar terdaftar). Setelah menghitung, daftar dibebaskan
     * dari memori untuk efisiensi. Shortcut yang berguna untuk tampilan statistik ujian.
     *
     * @param refresh true untuk memaksa reload data dari database sebelum menghitung
     * @return int jumlah peserta yang telah memulai mengerjakan ujian
     */
    public int ambilJumlahHasilUjianMahasiswaTelahIkut(boolean refresh) {
        List<Long> telahIkt = ambilHasilUjianMahasiswaTelahIkut(refresh);
        int size = telahIkt.size();
        telahIkt.clear();
        telahIkt = null;
        return size;
    }

    /**
     * Mengembalikan daftar ID {@link HasilUjianMahasiswa} untuk peserta yang sudah benar-benar
     * memulai mengerjakan ujian ini (memiliki {@code mulaiPada} yang tidak null). Berbeda dengan
     * {@link #ambilHasilUjianMahasiswa(boolean)} yang mengembalikan semua peserta terdaftar
     * termasuk yang belum mulai.
     *
     * @param refresh true untuk memaksa reload dari database; false menggunakan cache
     * @return List berisi ID HasilUjianMahasiswa peserta yang telah mulai ujian; tidak pernah null
     */
    public List<Long> ambilHasilUjianMahasiswaTelahIkut(boolean refresh) {
        List<Long> hasilUjianMahasiswasa = ambilHasilUjianMahasiswa(null, null, refresh, false);
        List<Long> telahIkt = new ArrayList<Long>();
        for (Long id : hasilUjianMahasiswasa) {
            try {
                if (id != null) {
                    HasilUjianMahasiswa generalValueObject = (HasilUjianMahasiswa) ambilData(
                            HasilUjianMahasiswa.class, id.toString());
                    if (generalValueObject != null && generalValueObject.getMulaiPada() != null) {
                        telahIkt.add(id);
                    }
                }
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.ambilHasilUjianMahasiswaTelahIkut");
            }
        }
        hasilUjianMahasiswasa.clear();
        hasilUjianMahasiswasa = null;
        return telahIkt;
    }

    /**
     * Implementasi utama untuk mengambil daftar ID {@link HasilUjianMahasiswa}. Jika cache belum
     * ada atau diminta refresh, mereinisialisasi cache dari database menggunakan session Hibernate
     * native thread-bound. Kemudian membaca cache, memfilter berdasarkan mahasiswa/calon mahasiswa
     * jika diperlukan, dan mengembalikan daftar ID yang sesuai. Untuk entri cache yang tidak ada
     * di in-memory cache, dilakukan query per-ID ke database sebagai fallback.
     *
     * @param mahasiswa             filter mahasiswa; null berarti semua peserta
     * @param biodataCalonMahasiswa filter calon mahasiswa; null berarti semua peserta
     * @param refresh               true untuk memaksa reinisialisasi cache dari database
     * @param hanyaYgIkut           true untuk hanya mengambil peserta yang jumlahIkut lebih dari 0
     * @return List berisi ID HasilUjianMahasiswa yang sesuai semua filter; tidak pernah null
     */
    @SuppressWarnings("unchecked")
    public List<Long> ambilHasilUjianMahasiswa(Mahasiswa mahasiswa,
            BiodataCalonMahasiswa biodataCalonMahasiswa, boolean refresh, boolean hanyaYgIkut) {
        if (!udah("baru") || refresh) {
            Session session = HibernateUtil.currentNativeSession();
            try {
                reInitHasilUjianMahasiswa(session);
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.ambilHasilUjianMahasiswa.reInit");
            }
            HibernateUtil.closeSession();
        }

        List<Long> hasilUjianMahasiswasa = new ArrayList<Long>();
        try {
            String ss = ambilLokasiHasilUjianMahasiswa();
            if (!ss.isEmpty()) {
                JSONObject c = new JSONObject(ss);
                Iterator<String> keys = c.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        String s = c.getString(key);
                        if (!s.trim().isEmpty()) {
                            GeneralValueObject generalValueObject = ambilData(HasilUjianMahasiswa.class, key);
                            if (generalValueObject != null) {
                                HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) generalValueObject;
                                hasilUjianMahasiswa.setPertemuanPunyaUjian(this);
                                if (mahasiswa == null
                                        || (hasilUjianMahasiswa.getMahasiswa() != null
                                                && mahasiswa.getId().equals(hasilUjianMahasiswa.getMahasiswa().getId()))
                                        || (hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null
                                                && biodataCalonMahasiswa.getId().equals(
                                                        hasilUjianMahasiswa.getBiodataCalonMahasiswa().getId()))) {
                                    if (hanyaYgIkut) {
                                        if (hasilUjianMahasiswa.getJumlahIkut() > 0) {
                                            hasilUjianMahasiswasa.add(hasilUjianMahasiswa.getId());
                                        }
                                    } else {
                                        hasilUjianMahasiswasa.add(hasilUjianMahasiswa.getId());
                                    }
                                }
                            } else {
                                Long hasilUjianMahasiswaId = Long.parseLong(key);
                                Session session = HibernateUtil.currentNativeSession();
                                try {
                                    HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) session
                                            .createCriteria(HasilUjianMahasiswa.class)
                                            .add(Restrictions.idEq(hasilUjianMahasiswaId))
                                            .uniqueResult();
                                    if (hasilUjianMahasiswa != null) {
                                        hasilUjianMahasiswa.setPertemuanPunyaUjian(this);
                                        masukkanData(HasilUjianMahasiswa.class, hasilUjianMahasiswa);
                                        if (mahasiswa == null
                                                || (hasilUjianMahasiswa.getMahasiswa() != null
                                                        && mahasiswa.getId().equals(
                                                                hasilUjianMahasiswa.getMahasiswa().getId()))
                                                || (hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null
                                                        && biodataCalonMahasiswa.getId().equals(
                                                                hasilUjianMahasiswa.getBiodataCalonMahasiswa().getId()))) {
                                            if (hanyaYgIkut) {
                                                if (hasilUjianMahasiswa.getJumlahIkut() > 0) {
                                                    hasilUjianMahasiswasa.add(hasilUjianMahasiswa.getId());
                                                }
                                            } else {
                                                hasilUjianMahasiswasa.add(hasilUjianMahasiswa.getId());
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.ambilHasilUjianMahasiswa.fallbackQuery");
                                }
                                HibernateUtil.closeSession();
                            }
                        }
                    } catch (Exception e) {
                        ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.ambilHasilUjianMahasiswa.iterKey");
                    }
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.ambilHasilUjianMahasiswa.parseCache");
        }
        return hasilUjianMahasiswasa;
    }

    /**
     * Mencari dan mengembalikan satu {@link HasilUjianMahasiswa} untuk mahasiswa atau calon
     * mahasiswa tertentu. Menggunakan cache in-memory terlebih dahulu; jika tidak ditemukan,
     * melakukan query ke database menggunakan session yang diberikan (atau session native thread-bound
     * sebagai fallback). Mengembalikan null jika parameter mahasiswa dan calon mahasiswa keduanya null,
     * atau jika tidak ditemukan hasil yang cocok.
     *
     * @param session               Session Hibernate untuk query fallback; bisa null (pakai currentSession)
     * @param mahasiswa             mahasiswa yang dicari hasil ujiannya; bisa null jika biodataCalonMahasiswa diisi
     * @param biodataCalonMahasiswa calon mahasiswa yang dicari; bisa null jika mahasiswa diisi
     * @return HasilUjianMahasiswa entitas hasil ujian yang ditemukan, atau null jika tidak ada
     */
    @SuppressWarnings("unchecked")
    public HasilUjianMahasiswa ambilHasilUjian(Session session, Mahasiswa mahasiswa,
            BiodataCalonMahasiswa biodataCalonMahasiswa) {
        if (mahasiswa == null && biodataCalonMahasiswa == null) {
            return null;
        }

        if (!udah()) {
            reInitHasilUjianMahasiswa(session);
        }

        HasilUjianMahasiswa hasil = null;
        try {
            JSONObject c = new JSONObject(ambilLokasiHasilUjianMahasiswa());
            Iterator<String> keys = c.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    String s = c.getString(key);
                    if (!s.trim().isEmpty()) {
                        GeneralValueObject generalValueObject = ambilData(HasilUjianMahasiswa.class, key);
                        if (generalValueObject != null) {
                            HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) generalValueObject;
                            hasilUjianMahasiswa.setPertemuanPunyaUjian(this);
                            if (mahasiswa != null && hasilUjianMahasiswa.getMahasiswa() != null
                                    && mahasiswa.getId().equals(hasilUjianMahasiswa.getMahasiswa().getId())) {
                                hasil = hasilUjianMahasiswa;
                            } else if (biodataCalonMahasiswa != null
                                    && hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null
                                    && biodataCalonMahasiswa.getId().equals(
                                            hasilUjianMahasiswa.getBiodataCalonMahasiswa().getId())) {
                                hasil = hasilUjianMahasiswa;
                            }
                        } else {
                            Long hasilUjianMahasiswaId = Long.parseLong(key);
                            if (session == null) {
                                session = HibernateUtil.currentSession();
                            }
                            HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) session
                                    .createCriteria(HasilUjianMahasiswa.class)
                                    .add(Restrictions.idEq(hasilUjianMahasiswaId))
                                    .uniqueResult();
                            if (hasilUjianMahasiswa != null) {
                                hasilUjianMahasiswa.setPertemuanPunyaUjian(this);
                                masukkanData(HasilUjianMahasiswa.class, hasilUjianMahasiswa);
                                if (mahasiswa != null && hasilUjianMahasiswa.getMahasiswa() != null
                                        && mahasiswa.getId().equals(hasilUjianMahasiswa.getMahasiswa().getId())) {
                                    hasil = hasilUjianMahasiswa;
                                } else if (biodataCalonMahasiswa != null
                                        && hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null
                                        && biodataCalonMahasiswa.getId().equals(
                                                hasilUjianMahasiswa.getBiodataCalonMahasiswa().getId())) {
                                    hasil = hasilUjianMahasiswa;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.ambilHasilUjian.iterKey");
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.ambilHasilUjian.parseCache");
        }
        return hasil;
    }

    // ─── Private Static Helpers ───────────────────────────────────────────────────────────

    /**
     * Membaca nilai default jumlah boleh ikut ujian dari konfigurasi sistem dengan kunci
     * {@code get_default_jumlah_boleh_ikut_ujian}. Jika konfigurasi tidak ditemukan atau
     * terjadi kesalahan parsing, mengembalikan nilai hardcoded 5 sebagai fallback aman.
     *
     * @return Integer jumlah default percobaan yang diizinkan per peserta; tidak pernah null
     */
    private static Integer getDefaultJumlahBolehIkut() {
        Integer jml = 5;
        try {
            jml = Integer.parseInt(
                    Common.getKonfigurasi("get_default_jumlah_boleh_ikut_ujian", jml + "").getNilai());
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PertemuanPunyaUjian.getDefaultJumlahBolehIkut");
        }
        return jml;
    }

}
