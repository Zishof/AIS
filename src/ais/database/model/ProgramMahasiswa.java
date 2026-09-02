package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Master <b>jadwal perpindahan program/kelas mahasiswa</b> berbasis rentang semester &mdash; tabel
 * {@code public.program_mahasiswa}.
 *
 * <p><b>Konsep sebenarnya (NAMA ENTITY MENYESATKAN, sudah diverifikasi dari kode).</b> Meski
 * namanya terdengar seperti tabel penghubung "satu baris per mahasiswa", entity ini <b>bukan</b>
 * tabel pivot dan <b>tidak</b> memuat kolom mahasiswa sama sekali. Ia adalah baris <b>master yang
 * dapat dipakai ulang</b>: satu baris bernama (mis. "Karyawan 2 Tahun Awal") menyimpan hingga
 * <b>TIGA</b> aturan berbentuk <i>(nama program, semester mulai, semester sampai)</i>. Banyak
 * {@link Mahasiswa} dapat menunjuk ke baris yang sama lewat relasi
 * {@link Mahasiswa#getProgramMahasiswa()} ({@code @ManyToOne}, kolom {@code program_mahasiswa} pada
 * tabel {@code mahasiswa}) &mdash; arah kepemilikannya ada di sisi {@code Mahasiswa}, bukan di
 * sini.</p>
 *
 * <p><b>Untuk apa aturan itu dipakai.</b> Konsumen utamanya adalah
 * {@code HistoryStatusMahasiswa.ambilProgram(Mahasiswa, Integer, String)}: diberi seorang mahasiswa
 * dan sebuah semester, method itu memeriksa slot I, lalu II, lalu III, dan memakai
 * <b>slot pertama yang rentangnya mencakup semester tersebut</b> (sisanya tidak dievaluasi lagi).
 * Hasilnya menjadi nilai program/kelas yang berlaku pada semester itu &mdash; sehingga seorang
 * mahasiswa dapat tercatat "Reguler" pada semester 1&ndash;4 lalu otomatis menjadi "Karyawan" pada
 * semester 5&ndash;8 tanpa perlu menyunting riwayat per semester satu per satu. Bila tidak ada slot
 * yang cocok, {@code ambilProgram} mengembalikan argumennya apa adanya (tidak ada mekanisme
 * cadangan di dalamnya); pengambilan nilai cadangan dari data utama mahasiswa dilakukan pemanggil,
 * yaitu {@code HistoryStatusMahasiswa.getProgram()}.</p>
 *
 * <p><b>Empat nama mirip yang gampang tertukar</b> (semuanya sudah diverifikasi dari kode):</p>
 * <ol>
 *   <li>{@link Program} (tabel {@code program}) &mdash; master nama program itu sendiri.
 *       Keanggotaan mahasiswa di sana disimpan sebagai <b>teks</b> pada {@code Mahasiswa.program},
 *       bukan foreign key.</li>
 *   <li><b>{@code ProgramMahasiswa}</b> (kelas ini) &mdash; master aturan rentang semester.
 *       Keanggotaan disimpan sebagai <b>foreign key</b> {@code Mahasiswa.programMahasiswa}.</li>
 *   <li>{@code ais.action.master.helper.ProgramMahasiswaDetailAction} &mdash; meski namanya persis
 *       kelas ini, panel itu mengelola anggota sebuah {@link Program}, <b>bukan</b> entity ini.</li>
 *   <li>{@code ais.action.master.helper.ProgramDataMahasiswaDetailAction} &mdash; panel yang
 *       benar-benar mengelola daftar mahasiswa yang menunjuk ke baris {@code ProgramMahasiswa}.</li>
 * </ol>
 * <p>Perhatikan bahwa nomor 3 dan 4 praktis <b>tertukar</b> dibanding dugaan dari namanya. Kolom
 * {@code program}/{@code program2}/{@code program3} di sini menyimpan <b>NAMA program sebagai
 * String</b> (nilai kunci {@code Common.programs}, dipilih lewat combobox
 * {@code Common.initPrograms}), jadi kaitan ke {@link Program} bersifat longgar berbasis teks
 * &mdash; tidak ada foreign key dan tidak ada penjagaan integritas bila nama program di master
 * diubah/dihapus.</p>
 *
 * <p><b>Pengelompokan anggota kelas ini:</b></p>
 * <ul>
 *   <li><i>Identitas &amp; audit</i> &mdash; {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()},
 *       {@link #toString()}.</li>
 *   <li><i>Deskripsi baris</i> &mdash; {@link #getNama()} (label aturan, wajib diisi di layar) dan
 *       {@link #getKeterangan()} (catatan bebas).</li>
 *   <li><i>Slot aturan I</i> &mdash; {@link #getProgram()}, {@link #getSmtMulai()},
 *       {@link #getSmtSampai()} (satu-satunya slot yang wajib diisi di layar).</li>
 *   <li><i>Slot aturan II</i> &mdash; {@link #getProgram2()}, {@link #getSmtMulai2()},
 *       {@link #getSmtSampai2()}.</li>
 *   <li><i>Slot aturan III</i> &mdash; {@link #getProgram3()}, {@link #getSmtMulai3()},
 *       {@link #getSmtSampai3()}.</li>
 * </ul>
 * <p>Tidak ada method bisnis, query statis, maupun validasi apa pun di kelas ini &mdash; seluruh
 * logika pemilihan slot berada di {@code HistoryStatusMahasiswa.ambilProgram(...)}, dan seluruh
 * validasi masukan berada di {@code ProgramMahasiswaAction.onSave(Event)}.</p>
 *
 * <p><b>Pemetaan &amp; kuirk teknis yang penting:</b></p>
 * <ul>
 *   <li>Anotasi {@code @Id} berada pada <b>getter</b>, sehingga Hibernate memakai <b>property
 *       access</b> untuk seluruh entity ini: nilai yang ditulis ke basis data adalah nilai yang
 *       dikembalikan getter, bukan isi field mentah. Konsekuensinya lihat butir berikut.</li>
 *   <li>Enam getter semester ({@link #getSmtMulai()} dan kawan-kawan) <b>menormalkan {@code null}
 *       menjadi {@code 0}</b>. Digabung dengan property access di atas, memuat lalu mem-flush baris
 *       yang kolom semesternya masih {@code NULL} akan <b>menuliskan {@code 0}</b> ke kolom
 *       tersebut &mdash; nilai berubah hanya karena baris dibaca. Karena
 *       {@code dynamicUpdate = true}, hanya kolom itu saja yang ikut dalam pernyataan
 *       {@code UPDATE}-nya.</li>
 *   <li>Normalisasi itu juga membuat pemeriksaan {@code getSmtMulai() != null} /
 *       {@code getSmtSampai() != null} di {@code HistoryStatusMahasiswa.ambilProgram(...)}
 *       <b>selalu bernilai benar</b> (kondisi mati). Efek nyatanya: slot yang programnya diisi tapi
 *       rentang semesternya dibiarkan kosong menjadi rentang {@code 0..0} yang tidak pernah cocok
 *       dengan semester perkuliahan mana pun &mdash; aturan "berlaku sejak semester 3 sampai
 *       selamanya" harus ditulis dengan batas atas eksplisit, tidak boleh dikosongkan.</li>
 *   <li>Field yang <b>tidak</b> beranotasi {@code @Column} dipetakan oleh
 *       {@code ais.database.hibernate.MyNamingStrategy} (turunan {@code DefaultNamingStrategy}),
 *       yaitu nama kolom = nama properti apa adanya tanpa konversi ke {@code snake_case}
 *       ({@code smtMulai}, {@code program2}, {@code tanggal_dirubah}, dan seterusnya).</li>
 *   <li>Kelas ini {@code @Audited} (Hibernate Envers), sehingga setiap perubahan baris tercatat di
 *       skema audit dan dapat ditelusuri dari layar lewat {@code RevisiHelper.createNewRevisi}.</li>
 *   <li>Terdaftar pada {@code InitData.initClasses(...)}, sehingga metadata/data awalnya disiapkan
 *       saat aplikasi mulai.</li>
 *   <li>Javadoc bawaan hbm2java pada berkas ini sebelumnya berbunyi "Bank generated by hbm2java"
 *       &mdash; sisa salin-tempel dari {@code Bank.java}, tidak ada hubungannya dengan perbankan.</li>
 * </ul>
 *
 * <p><b>Induk {@link GeneralValueObject}.</b> Induk ini <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass}, melainkan POJO abstrak biasa, sehingga Hibernate <b>tidak</b>
 * memetakan properti apa pun miliknya. Karena itu deklarasi ulang {@link #id}, {@link #oleh},
 * {@link #olehId}, dan {@link #tanggal_dirubah} di kelas ini <b>bukan duplikasi keliru</b>,
 * melainkan keharusan teknis agar kolom-kolom tersebut benar-benar terpetakan. Jangan dihapus.</p>
 *
 * <p><b>Layar terkait &amp; catatan kontrol akses.</b> Layar masternya
 * {@code /pages/master/program_mahasiswa.zul} dengan controller
 * {@code ais.action.master.ProgramMahasiswaAction}. Controller itu memanggil
 * {@code Common.doCheckSecurity()} pada {@code doBeforeCompose}, namun halaman ini <b>tidak
 * termasuk</b> daftar putih {@code CommonPrivilages.MUST_CHECKED} (12 halaman) sehingga panggilan
 * tersebut tidak menegakkan apa pun &mdash; pola yang sama dengan temuan-temuan sebelumnya. Tombol
 * Tambah/Ubah/Hapus baris master sendiri <b>sudah</b> dijaga
 * {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)}. Sebaliknya panel
 * {@code ProgramDataMahasiswaDetailAction} yang tertanam di setiap baris grid yang sama &mdash;
 * yang dapat menetapkan atau melepas keanggotaan program hingga 5.000 mahasiswa sekaligus lewat
 * "Ambil Data Mahasiswa", "Hapus Semua", dan unggah berkas Excel &mdash; <b>tidak memiliki satu pun
 * pemeriksaan hak akses</b>. Ini instance lain dari pola inversi hak akses yang berulang di
 * codebase ini: operasi massal yang jauh lebih berdampak justru lebih longgar dijaga daripada
 * penyuntingan satu baris master.</p>
 *
 * <p><b>Efek hilir yang perlu diketahui.</b> Begitu seorang mahasiswa ditautkan ke baris
 * {@code ProgramMahasiswa}, combobox "Program" per semester pada layar riwayat studi
 * ({@code TampilStudiMahasiswaHelper}) otomatis di-<i>disable</i>, karena nilainya kini ditentukan
 * aturan di sini dan bukan lagi masukan manual.</p>
 *
 * @see Mahasiswa#getProgramMahasiswa()
 * @see Program
 * @see HistoryStatusMahasiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "program_mahasiswa")
public class ProgramMahasiswa extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama baris ini (kolom {@code id}, {@code IDENTITY}). Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass} sehingga field induknya tidak
	 * dipetakan Hibernate.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang menyimpan baris ini, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Dideklarasikan ulang karena alasan yang sama dengan
	 * {@link #id}.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang menyimpan baris ini, pendamping {@link #oleh}. Dideklarasikan ulang
	 * karena alasan yang sama dengan {@link #id}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Kuirk:</b> nilai {@code null} maupun string kosong/spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return} tanpa menyentuh field). Jejak audit lama karena itu tidak
	 * pernah bisa dikosongkan lewat setter ini &mdash; disengaja, agar penyimpanan dari jalur yang
	 * tidak mengetahui pengguna aktif tidak menghapus jejak sebelumnya.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan sehingga jejak
	 * audit lama dipertahankan.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini
	 * dikirim ke basis data, lalu mendelegasikan pembaruan stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p><b>Efek samping:</b> mengubah {@link #tanggal_dirubah} (dan, tergantung implementasi
	 * interceptor, kolom {@code oleh}/{@code olehId}) pada objek yang sedang di-flush. Tidak pernah
	 * dipanggil manual dari kode aplikasi; juga tidak berjalan untuk {@code INSERT} maupun untuk
	 * perubahan lewat SQL native.</p>
	 *
	 * <p><b>Catatan tata letak:</b> pada baris yang sama juga dideklarasikan field
	 * {@link #tanggal_dirubah} beserta nilai awalnya ({@code WaktuUtil.getDate()}, yaitu waktu
	 * pembuatan objek di JVM, bukan waktu basis data) &mdash; gaya penulisan padat yang dipakai
	 * konsisten di seluruh entity repo ini.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Umumnya tidak dipanggil dari kode aplikasi &mdash; nilainya diisi otomatis oleh
	 * {@link #onUpdate()} lewat {@code AuditTimestampInterceptor} saat flush.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, presisi
	 * {@code TIMESTAMP}).
	 *
	 * <p>Tanpa {@code @Column}, sehingga nama kolomnya mengikuti penamaan default
	 * {@code MyNamingStrategy}, yaitu {@code tanggal_dirubah} apa adanya.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} karena field-nya diinisialisasi
	 *         saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini dalam bentuk {@code "<id>-<program slot I>"}.
	 *
	 * <p><b>Kuirk:</b> baris pertama {@code program = getProgram();} adalah penugasan field ke
	 * dirinya sendiri &mdash; {@link #getProgram()} pada kelas ini hanya mengembalikan field apa
	 * adanya tanpa perhitungan, sehingga pernyataan itu tidak berefek apa pun (sisa pola salin-tempel
	 * dari entity lain yang getter-nya memang menghitung ulang dan menulis balik). Perhatikan pula
	 * bahwa {@link #getNama()} &mdash; label yang sebenarnya dilihat pengguna &mdash; justru
	 * <b>tidak</b> ikut ditampilkan, dan hanya slot I yang diwakili; dua slot lainnya tidak terlihat
	 * sama sekali pada representasi ini.</p>
	 *
	 * @return gabungan id dan nama program slot I, dipisah tanda hubung
	 */
	public String toString() {
		program = getProgram();
		return id + "-" + program;
	}

	/** Label/nama aturan ini, ditampilkan di grid dan wajib diisi layar master. Kolom {@code nama}. */
	private String nama;
	/** Catatan bebas untuk baris ini. Kolom {@code keterangan}, boleh {@code null}. */
	private String keterangan;
	/** Nama program yang berlaku pada slot aturan I. Kolom {@code program}. */
	private String program;
	/** Semester awal berlakunya slot aturan I (inklusif). Kolom {@code smtMulai}. */
	private Integer smtMulai;
	/** Semester akhir berlakunya slot aturan I (inklusif). Kolom {@code smtSampai}. */
	private Integer smtSampai;

	/** Nama program yang berlaku pada slot aturan II. Kolom {@code program2}. */
	private String program2;
	/** Semester awal berlakunya slot aturan II (inklusif). Kolom {@code smtMulai2}. */
	private Integer smtMulai2;
	/** Semester akhir berlakunya slot aturan II (inklusif). Kolom {@code smtSampai2}. */
	private Integer smtSampai2;

	/** Nama program yang berlaku pada slot aturan III. Kolom {@code program3}. */
	private String program3;
	/** Semester awal berlakunya slot aturan III (inklusif). Kolom {@code smtMulai3}. */
	private Integer smtMulai3;
	/** Semester akhir berlakunya slot aturan III (inklusif). Kolom {@code smtSampai3}. */
	private Integer smtSampai3;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk instansiasi entity. Juga dipakai
	 * {@code ProgramMahasiswaAction.onAdd(Event)} sebagai objek kosong untuk form "Tambah".
	 */
	public ProgramMahasiswa() {
	}

	/**
	 * Kunci utama baris ini (kolom {@code id}).
	 *
	 * <p>Dihasilkan basis data ({@code IDENTITY}) sehingga {@code insertable = false}; bernilai
	 * {@code null} selama objek belum pernah disimpan &mdash; kondisi inilah yang dipakai layar
	 * master untuk membedakan judul dialog "Tambah" dari "Ubah".</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris ini.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama/label aturan ini, mis. "Karyawan Mulai Semester 5" (kolom {@code nama}).
	 *
	 * <p>Inilah satu-satunya pengenal yang dilihat pengguna: dipakai sebagai judul kolom pertama
	 * grid, sebagai label riwayat revisi Envers ({@code RevisiHelper.createNewRevisi}), dan sebagai
	 * kunci pengurutan serta kata kunci pencarian ({@code ilike}, {@code MatchMode.ANYWHERE}) pada
	 * {@code ProgramMahasiswaAction.initCriteria(boolean)}. Wajib diisi &mdash; {@code onSave}
	 * menolak penyimpanan bila kosong.</p>
	 *
	 * <p>Tanpa {@code @Column}, sehingga dipetakan ke kolom {@code nama} apa adanya oleh
	 * {@code MyNamingStrategy}.</p>
	 *
	 * @return nama aturan; bisa {@code null} untuk objek yang belum diisi
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Menetapkan nama/label aturan ini.
	 *
	 * @param nama nama aturan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Catatan bebas mengenai aturan ini (kolom {@code keterangan}, boleh {@code null}).
	 *
	 * <p>Murni informatif: ditampilkan sebagai kolom terakhir grid dan disunting lewat textbox di
	 * dialog, tetapi tidak pernah ikut dalam logika pemilihan slot mana pun.</p>
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas untuk aturan ini.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nama program yang berlaku pada <b>slot aturan I</b> (kolom {@code program}).
	 *
	 * <p>Berisi <b>teks nama program</b>, yaitu nilai kunci peta {@code Common.programs} yang dipilih
	 * lewat combobox {@code Common.initPrograms} &mdash; bukan foreign key ke {@link Program}, jadi
	 * tidak ada penjagaan integritas bila nama di master program berubah.</p>
	 *
	 * <p>Slot I adalah satu-satunya slot yang <b>wajib</b> diisi
	 * ({@code ProgramMahasiswaAction.onSave} menolak penyimpanan bila combobox Program (I) kosong)
	 * dan slot pertama yang diperiksa {@code HistoryStatusMahasiswa.ambilProgram(...)}; bila
	 * rentangnya cocok, slot II dan III tidak dievaluasi sama sekali.</p>
	 *
	 * <p>Getter ini murni pembaca &mdash; tidak ada perhitungan, penulisan balik, maupun penutupan
	 * sesi Hibernate di dalamnya. (Verifikasi ini penting karena banyak entity lain di repo ini
	 * memiliki getter yang menulis balik ke field/basis data; kelas ini tidak.)</p>
	 *
	 * @return nama program slot I, atau {@code null} bila belum diisi
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * Menetapkan nama program untuk slot aturan I.
	 *
	 * @param program nama program (nilai kunci {@code Common.programs})
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Semester awal (inklusif) berlakunya slot aturan I (kolom {@code smtMulai}).
	 *
	 * <p><b>Kuirk penting &mdash; normalisasi {@code null} menjadi {@code 0}.</b> Getter ini tidak
	 * pernah mengembalikan {@code null}. Karena entity ini memakai property access (anotasi
	 * {@code @Id} berada pada getter), nilai inilah yang dibaca Hibernate saat flush: baris yang
	 * kolomnya masih {@code NULL} akan ter-{@code UPDATE} menjadi {@code 0} hanya karena pernah
	 * dimuat. Akibat lain, pemeriksaan {@code getSmtMulai() != null} di
	 * {@code HistoryStatusMahasiswa.ambilProgram(...)} tidak pernah gagal, dan slot yang rentangnya
	 * dikosongkan menjadi rentang {@code 0..0} yang tidak akan pernah cocok dengan semester mana
	 * pun.</p>
	 *
	 * <p>Sifat "tidak pernah {@code null}" ini juga menjadi tumpuan layar master, yang membungkus
	 * hasilnya langsung dengan {@code new BigDecimal(...)} untuk mengisi {@code Decimalbox} tanpa
	 * pemeriksaan {@code null}.</p>
	 *
	 * @return semester awal slot I, atau {@code 0} bila kolomnya kosong
	 */
	public Integer getSmtMulai() {
		return smtMulai == null ? 0 : smtMulai;
	}

	/**
	 * Menetapkan semester awal slot aturan I.
	 *
	 * <p>Berbeda dari getter-nya, setter ini menerima {@code null} apa adanya &mdash; layar master
	 * memang meneruskan {@code null} bila {@code Decimalbox}-nya dikosongkan.</p>
	 *
	 * @param smtMulai semester awal, boleh {@code null}
	 */
	public void setSmtMulai(Integer smtMulai) {
		this.smtMulai = smtMulai;
	}

	/**
	 * Semester akhir (inklusif) berlakunya slot aturan I (kolom {@code smtSampai}).
	 *
	 * <p>{@code null} dinormalkan menjadi {@code 0} dengan seluruh konsekuensi yang dijelaskan pada
	 * {@link #getSmtMulai()} &mdash; termasuk bahwa batas atas <b>tidak boleh dikosongkan</b> untuk
	 * memaksudkan "tanpa batas", karena hasilnya justru rentang yang tidak pernah cocok.</p>
	 *
	 * @return semester akhir slot I, atau {@code 0} bila kolomnya kosong
	 */
	public Integer getSmtSampai() {
		return smtSampai == null ? 0 : smtSampai;
	}

	/**
	 * Menetapkan semester akhir slot aturan I.
	 *
	 * <p>Tidak ada validasi bahwa nilainya &ge; {@link #getSmtMulai()}, dan tidak ada pemeriksaan
	 * tumpang-tindih dengan slot II/III baik di sini maupun di
	 * {@code ProgramMahasiswaAction.onSave(Event)}. Rentang yang tumpang-tindih diterima diam-diam
	 * dan diselesaikan secara "slot pertama yang cocok menang".</p>
	 *
	 * @param smtSampai semester akhir, boleh {@code null}
	 */
	public void setSmtSampai(Integer smtSampai) {
		this.smtSampai = smtSampai;
	}

	/**
	 * Nama program yang berlaku pada <b>slot aturan II</b> (kolom {@code program2}).
	 *
	 * <p>Bersifat opsional dan hanya dievaluasi
	 * {@code HistoryStatusMahasiswa.ambilProgram(...)} bila rentang slot I <b>tidak</b> mencakup
	 * semester yang dinilai. Semantik penyimpanannya identik dengan {@link #getProgram()}: teks nama
	 * program, bukan foreign key.</p>
	 *
	 * @return nama program slot II, atau {@code null} bila slot ini tidak dipakai
	 */
	public String getProgram2() {
		return program2;
	}

	/**
	 * Menetapkan nama program untuk slot aturan II.
	 *
	 * @param program2 nama program, atau {@code null} bila slot ini tidak dipakai
	 */
	public void setProgram2(String program2) {
		this.program2 = program2;
	}

	/**
	 * Semester awal (inklusif) berlakunya slot aturan II (kolom {@code smtMulai2}).
	 *
	 * <p>{@code null} dinormalkan menjadi {@code 0}; lihat {@link #getSmtMulai()} untuk konsekuensi
	 * penulisan balik ke basis data dan kondisi mati di pemanggilnya.</p>
	 *
	 * @return semester awal slot II, atau {@code 0} bila kolomnya kosong
	 */
	public Integer getSmtMulai2() {
		return smtMulai2 == null ? 0 : smtMulai2;
	}

	/**
	 * Menetapkan semester awal slot aturan II.
	 *
	 * @param smtMulai2 semester awal, boleh {@code null}
	 */
	public void setSmtMulai2(Integer smtMulai2) {
		this.smtMulai2 = smtMulai2;
	}

	/**
	 * Semester akhir (inklusif) berlakunya slot aturan II (kolom {@code smtSampai2}).
	 *
	 * <p>{@code null} dinormalkan menjadi {@code 0}; lihat {@link #getSmtMulai()}.</p>
	 *
	 * @return semester akhir slot II, atau {@code 0} bila kolomnya kosong
	 */
	public Integer getSmtSampai2() {
		return smtSampai2 == null ? 0 : smtSampai2;
	}

	/**
	 * Menetapkan semester akhir slot aturan II.
	 *
	 * @param smtSampai2 semester akhir, boleh {@code null}
	 */
	public void setSmtSampai2(Integer smtSampai2) {
		this.smtSampai2 = smtSampai2;
	}

	/**
	 * Nama program yang berlaku pada <b>slot aturan III</b> (kolom {@code program3}).
	 *
	 * <p>Slot terakhir dan paling jarang dipakai: hanya dievaluasi bila rentang slot I <b>dan</b>
	 * slot II sama-sama tidak mencakup semester yang dinilai. Bila slot III pun tidak cocok,
	 * {@code HistoryStatusMahasiswa.ambilProgram(...)} mengembalikan nilai masukannya apa adanya
	 * &mdash; tidak ada slot keempat dan tidak ada nilai cadangan di dalam method itu.</p>
	 *
	 * @return nama program slot III, atau {@code null} bila slot ini tidak dipakai
	 */
	public String getProgram3() {
		return program3;
	}

	/**
	 * Menetapkan nama program untuk slot aturan III.
	 *
	 * @param program3 nama program, atau {@code null} bila slot ini tidak dipakai
	 */
	public void setProgram3(String program3) {
		this.program3 = program3;
	}

	/**
	 * Semester awal (inklusif) berlakunya slot aturan III (kolom {@code smtMulai3}).
	 *
	 * <p>{@code null} dinormalkan menjadi {@code 0}; lihat {@link #getSmtMulai()}.</p>
	 *
	 * @return semester awal slot III, atau {@code 0} bila kolomnya kosong
	 */
	public Integer getSmtMulai3() {
		return smtMulai3 == null ? 0 : smtMulai3;
	}

	/**
	 * Menetapkan semester awal slot aturan III.
	 *
	 * @param smtMulai3 semester awal, boleh {@code null}
	 */
	public void setSmtMulai3(Integer smtMulai3) {
		this.smtMulai3 = smtMulai3;
	}

	/**
	 * Semester akhir (inklusif) berlakunya slot aturan III (kolom {@code smtSampai3}).
	 *
	 * <p>{@code null} dinormalkan menjadi {@code 0}; lihat {@link #getSmtMulai()}.</p>
	 *
	 * @return semester akhir slot III, atau {@code 0} bila kolomnya kosong
	 */
	public Integer getSmtSampai3() {
		return smtSampai3 == null ? 0 : smtSampai3;
	}

	/**
	 * Menetapkan semester akhir slot aturan III.
	 *
	 * @param smtSampai3 semester akhir, boleh {@code null}
	 */
	public void setSmtSampai3(Integer smtSampai3) {
		this.smtSampai3 = smtSampai3;
	}

}
