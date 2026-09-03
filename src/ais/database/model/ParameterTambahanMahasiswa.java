package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

/**
 * Tabel <b>penghubung</b> yang memasang sebuah definisi field kustom generik
 * ({@link ParameterTambahan}) ke modul biodata mahasiswa, sekaligus menempatkannya pada sebuah
 * kelompok/seksi ({@link KelompokParameterTambahanMahasiswa}) — tabel
 * {@code public.parameter_tambahan_mahasiswa}.
 *
 * <h2>Posisi dalam rantai "Form Tambahan" mahasiswa</h2>
 * <p>Field kustom mahasiswa di AIS tersusun tiga lapis, dan kelas ini adalah lapis
 * <b>kedua</b> (penghubung):</p>
 * <ol>
 *   <li>{@link ParameterTambahan} — definisi field generik yang dipakai bersama banyak modul:
 *   label inputan, tipe data inputan, daftar nilai pilihan, bendera {@code wajibDiisi},
 *   {@code harusMenyertakanLampiran}, {@code lampiranWajibDiisi}, {@code aktif}, dan
 *   {@code nomorUrut}. Definisi ini <b>tidak tahu</b> modul mana yang memakainya.</li>
 *   <li><b>kelas ini</b> — satu baris = "field X dipakai di modul mahasiswa, ditempatkan di
 *   kelompok Y, berlaku untuk tahun angkatan Z". Menyimpan FK wajib ke
 *   {@link #getParameterTambahan()} dan {@link #getKelompokParameterTambahanMahasiswa()},
 *   ditambah penyaring cakupan ({@link #getTampilDiSemuaTahunAngkatan()},
 *   {@link #getTahunAngkatans()}, dan empat kolom cakupan akademik yang ternyata tidak pernah
 *   ditegakkan — lihat di bawah).</li>
 *   <li>{@link KelompokParameterTambahanMahasiswa} — kategori/judul seksi tempat field-field
 *   tadi dirender berurutan pada formulir biodata.</li>
 * </ol>
 *
 * <h2>Nilai isian mahasiswa TIDAK disimpan di entity manapun pada rantai ini</h2>
 * <p>Tidak ada satu pun tabel transaksi turunan. Jawaban mahasiswa disimpan sebagai
 * <b>teks gabungan multi-baris</b> pada dua kolom bertipe {@code text} milik
 * {@link BiodataMahasiswa}, ditulis oleh
 * {@link BiodataMahasiswa#populateParameterTambahan(java.util.List)} yang membaca balik
 * komponen ZK dari baris formulir:</p>
 * <ul>
 *   <li>{@code BiodataMahasiswa.parameterTambahan} — versi <b>berlabel</b> (untuk
 *   ditampilkan/dicetak). Satu baris per isian, dipisah {@code "\n"}, delapan ruas dipisah
 *   <code>&lt;=&gt;</code>:
 *   <pre>namaKelompok-&gt;labelInputan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt;
 *nomorUrutParameter &lt;=&gt; idParameter &lt;=&gt; idKelompok &lt;=&gt; indexKe &lt;=&gt; keterangan</pre></li>
 *   <li>{@code BiodataMahasiswa.parameterTambahanInds} — versi <b>ber-ID</b> (untuk mengisi
 *   ulang form dan memvalidasi). Satu baris per isian, empat ruas:
 *   <pre>&lt;idKelompok&gt;-&gt;&lt;idParameter&gt; &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; keterangan</pre></li>
 * </ul>
 *
 * <p><b>Kunci gabungan {@code "<idKelompok>-><idParameter>"}</b> (di kode disebut variabel
 * {@code jenis}) dirakit di empat tempat berbeda dengan cara yang persis sama:
 * {@code kelompokParameterTambahanMahasiswa.getId() + "->" + parameterTambahan.getId()} — yaitu
 * di {@code BiodataMahasiswa.populateParameterTambahan}, serta di
 * {@code ParameterTambahanMahasiswaListener.onEvent}, {@code .validate}, dan pembacaan nilai
 * awal field. Pembacaan balik dilakukan manual dengan
 * {@code getParameterTambahanInds().split("\n")} lalu {@code split("<=>")} dan mencocokkan ruas
 * ke-0 terhadap kunci tersebut (perbandingan {@code equalsIgnoreCase}). Perhatikan: karena
 * pemisah ruas adalah <code>&lt;=&gt;</code> dan pemisah baris adalah {@code "\n"}, isian
 * mahasiswa yang kebetulan memuat kedua pola itu akan merusak struktur seluruh kolom — tidak
 * ada escaping sama sekali.</p>
 *
 * <p><b>Keterkaitan dengan {@link ais.database.model.file.LampiranLain}.</b> Kunci gabungan yang
 * sama dipakai sebagai kolom {@code jenis} lampiran, dengan {@code ref} berisi <b>id
 * {@link BiodataMahasiswa}</b>, bukan id baris entity ini. Berkas diambil lewat
 * {@code LampiranLain.ambil(biodataMahasiswa.getId(), jenis)} — dipanggil dari
 * {@code populateParameterTambahan} (untuk mengisi ruas {@code urlLampiran} lewat
 * {@code createLinkUri()}), dari {@code ParameterTambahanMahasiswaListener.validate} (memeriksa
 * lampiran wajib), dan dari {@code ParameterTambahanAstract.initComponent} (merender tombol
 * unduh/unggah). Konsekuensinya: <b>menghapus atau mengganti id baris kelompok memutus tautan
 * lampiran secara permanen</b>, karena id itu adalah bagian kiri kunci dan tidak ada FK yang
 * menjaganya.</p>
 *
 * <h2>Cakupan/penyaringan — apa yang benar-benar ditegakkan</h2>
 * <p>Seluruh konsumen (listener biodata dan rekap dasbor) memakai kombinasi filter yang sama
 * persis dan hanya itu:</p>
 * <ul>
 *   <li>{@code tampilDiSemuaTahunAngkatan = true} <b>ATAU</b> {@code tahunAngkatans}
 *   mengandung {@code ";<tahunAngkatanMahasiswa>;"} ({@code ilike ... MatchMode.ANYWHERE});
 *   bila mahasiswa tidak punya tahun angkatan sama sekali, cabang kedua diganti
 *   {@code sqlRestriction("false")} sehingga hanya field "semua angkatan" yang muncul;</li>
 *   <li>{@code parameterTambahan.aktif = true};</li>
 *   <li>{@code kelompokParameterTambahanMahasiswa.aktif = true}.</li>
 * </ul>
 * <p><b>Empat kolom cakupan akademik {@code fakultas}, {@code jurusan}, {@code program}, dan
 * {@code jenjang} TIDAK PERNAH dipakai sebagai filter oleh pembaca manapun.</b> Sudah
 * diverifikasi ke seluruh pemanggil {@code createCriteria(ParameterTambahanMahasiswa.class)} di
 * codebase: keempatnya hanya (a) ditulis dari form Tambah/Ubah, (b) ditampilkan sebagai kolom
 * grid pada layar admin, dan (c) dipakai sebagai kriteria pencarian di layar admin itu sendiri
 * ({@code ParameterTambahanMahasiswaAction.initCriteria}). Artinya admin yang membatasi sebuah
 * field ke satu fakultas/jurusan/program/jenjang tetap akan melihat field itu muncul di
 * formulir biodata <b>semua</b> mahasiswa — pembatasan bersifat dokumentatif belaka.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <p>(a) Jejak audit manual {@code oleh}/{@code olehId}/{@code tanggal_dirubah} beserta hook
 * {@link #onUpdate()}; (b) identitas {@link #getId()}; (c) relasi inti
 * {@link #getParameterTambahan()} dan {@link #getKelompokParameterTambahanMahasiswa()}
 * (keduanya {@code nullable = false}); (d) cakupan akademik {@link #getFakultas()},
 * {@link #getJurusan()}, {@link #getProgram()}, {@link #getJenjang()} (semua opsional dan tidak
 * ditegakkan); (e) cakupan angkatan {@link #getTampilDiSemuaTahunAngkatan()} dan
 * {@link #getTahunAngkatans()}; (f) {@link #getNomorUrut()} yang <b>bukan</b> getter biasa —
 * lihat catatannya. Kelas ini tidak punya satu pun method bisnis atau utility statis, dan tidak
 * meng-override {@code compareTo}.</p>
 *
 * <h2>Catatan pemetaan yang mudah disalahpahami</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu pun property induknya. Karena
 * itu deklarasi ULANG {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, dan
 * {@code nomorUrut} di kelas ini <b>bukan bug melainkan keharusan teknis</b> agar kolom-kolom
 * itu benar-benar tersimpan. Efek sampingnya: field kelas ini membayangi (<i>shadow</i>) field
 * bernama sama di induk, dan property induk yang TIDAK dideklarasikan ulang di sini — terutama
 * {@code nama}, {@code keterangan}, {@code nim}, dan {@code kode} — selamanya bernilai
 * {@code null} untuk entity ini. Konsekuensi langsung pada
 * {@link GeneralValueObject#compareTo(GeneralValueObject)}: karena {@link #getNomorUrut()} di
 * sini <b>tidak pernah</b> mengembalikan {@code null}, cabang pertama selalu diambil dan
 * seluruh cabang berikutnya ({@code nim}/{@code nama}/{@code keterangan}) menjadi kode mati
 * untuk tipe ini.</p>
 *
 * <p><b>Akses property, bukan field.</b> {@code @Id} dipasang pada {@link #getId()} sehingga
 * Hibernate memakai <i>getter</i> untuk membaca seluruh property. Digabung dengan
 * {@code dynamicUpdate = true} dan {@code @Audited}, getter yang menambal/menurunkan nilai
 * ({@link #getNomorUrut()}, {@link #getTampilDiSemuaTahunAngkatan()},
 * {@link #getTahunAngkatans()}) dapat memicu {@code UPDATE} beserta revisi Envers baru pada
 * baris yang sekadar terbaca dalam sesi aktif. Property tanpa {@code @Column} eksplisit
 * ({@code tampilDiSemuaTahunAngkatan}, {@code program}, {@code nomorUrut}) memakai nama kolom
 * apa adanya sesuai {@code ais.database.hibernate.MyNamingStrategy}.</p>
 *
 * <p><b>Sisa generator.</b> Komentar kelas asli berbunyi "Bank generated by hbm2java" dan
 * {@code serialVersionUID}-nya identik dengan milik {@link KelompokParameterTambahanMahasiswa}
 * serta {@link KelompokParameterTambahanAlumni} — sisa salin-tempel berkas, tidak berdampak
 * karena tipe-tipe itu tidak pernah saling dideserialisasi.</p>
 *
 * <h2>Konsumen</h2>
 * <ul>
 *   <li>{@code ais.action.master.helper.ParameterTambahanMahasiswaListener} — membangun blok
 *   field kustom pada formulir biodata ({@code onEvent}), memvalidasi isian/lampiran wajib
 *   ({@code validate}), menghitung apakah blok perlu ditampilkan ({@code check}), dan
 *   menyerahkan pengumpulan nilai ke {@code BiodataMahasiswa.populateParameterTambahan}
 *   ({@code onSave});</li>
 *   <li>{@code ais.action.master.ParameterTambahanMahasiswaAction} — layar CRUD pemetaan ini
 *   (juga diwarisi {@code ParameterTambahanMahasiswaAlumniAction});</li>
 *   <li>{@code ais.action.master.dashboard.helper.DashboardRekapParameterTambahanMahasiswa} —
 *   rekap dasbor per kelompok/parameter;</li>
 *   <li>{@code ais.common.InitData} — kelas ini terdaftar pada salah satu panggilan
 *   {@code initClasses(...)}, sehingga seluruh barisnya ikut di-preload ke cache memori saat
 *   bootstrap aplikasi.</li>
 * </ul>
 *
 * @see ParameterTambahan
 * @see KelompokParameterTambahanMahasiswa
 * @see BiodataMahasiswa#populateParameterTambahan(java.util.List)
 * @see ais.database.model.file.LampiranLain
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "parameter_tambahan_mahasiswa")

public class ParameterTambahanMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilainya identik dengan milik
	 * {@link KelompokParameterTambahanMahasiswa} dan {@link KelompokParameterTambahanAlumni} —
	 * sisa penggandaan berkas, tidak berdampak karena tipe-tipe itu tidak pernah saling
	 * dideserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code id}, IDENTITY; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}, bukan oleh kode layar.</p>
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat
	 *         jalur ber-interceptor
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau string kosong/whitespace <b>diabaikan
	 * diam-diam</b> — field lama dipertahankan. Jadi nilai yang sudah pernah terisi tidak bisa
	 * dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong/whitespace diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang menyerahkan pengisian jejak audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(Object)} tepat sebelum baris
	 * di-{@code UPDATE}.
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris yang sama oleh
	 * generator; nilai awalnya {@code ais.ui.util.WaktuUtil.getDate()} sehingga baris baru
	 * sudah bertanggal meski belum pernah di-{@code UPDATE}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada instance yang dibuat di
	 *         memori karena field-nya diinisialisasi saat deklarasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Cakupan fakultas (opsional, TIDAK ditegakkan pembaca); lihat {@link #getFakultas()}. */
	private Fakultas fakultas;
	/** Cakupan jurusan (opsional, TIDAK ditegakkan pembaca); lihat {@link #getJurusan()}. */
	private Jurusan jurusan;
	/** Cakupan program berupa teks bebas (opsional, TIDAK ditegakkan); lihat {@link #getProgram()}. */
	private String program;
	/** Cakupan jenjang (opsional, TIDAK ditegakkan pembaca); lihat {@link #getJenjang()}. */
	private Jenjang jenjang;
	/** Definisi field kustom yang dipetakan; wajib. Lihat {@link #getParameterTambahan()}. */
	private ParameterTambahan parameterTambahan;
	/** Bendera "berlaku untuk semua angkatan"; lihat {@link #getTampilDiSemuaTahunAngkatan()}. */
	private Boolean tampilDiSemuaTahunAngkatan;
	/** Daftar angkatan berformat {@code ";2020;;2021;"}; lihat {@link #getTahunAngkatans()}. */
	private String tahunAngkatans;
	/** Kelompok/seksi tempat field ini dirender; wajib. Lihat {@link #getKelompokParameterTambahanMahasiswa()}. */
	private KelompokParameterTambahanMahasiswa kelompokParameterTambahanMahasiswa;

	/** Nomor urut hasil turunan dari {@link ParameterTambahan}; lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/**
	 * Mengembalikan nomor urut field — <b>bukan getter biasa</b>.
	 *
	 * <p>Method ini <b>menimpa</b> field {@code nomorUrut} milik baris ini dengan
	 * {@code getParameterTambahan().getNomorUrut()} setiap kali dipanggil, lalu menambal
	 * {@code null} menjadi {@code 1}. Jadi kolom {@code nomorUrut} tabel ini sama sekali bukan
	 * nilai mandiri, melainkan salinan turunan dari definisi field induknya; nilai apa pun yang
	 * pernah ditulis lewat {@link #setNomorUrut(Integer)} akan dibuang pada pembacaan
	 * berikutnya selama relasi {@code parameterTambahan} bisa diresolusi.</p>
	 *
	 * <p><b>Efek samping.</b> Karena kelas ini memakai akses property + {@code dynamicUpdate}
	 * + {@code @Audited}, penimpaan itu terjadi juga saat Hibernate <i>membaca</i> baris dalam
	 * sesi aktif — sekadar membuka layar admin dapat menghasilkan {@code UPDATE} dan revisi
	 * Envers baru tanpa ada perubahan yang diminta pengguna (pola getter destruktif yang sudah
	 * dikenal di keluarga {@link GeneralValueObject}). Efek tambahannya: panggilan ini memicu
	 * resolusi proxy lazy {@code parameterTambahan} lewat {@link #getParameterTambahan()},
	 * yang bisa memunculkan query tambahan atau pengambilan dari cache {@code check()}.</p>
	 *
	 * <p><b>Pemakaian nyata.</b> Tidak ada pemanggil di luar kelas ini — urutan tampilan field
	 * pada formulir biodata ditentukan dengan menyortir daftar {@link ParameterTambahan}
	 * langsung ({@code Collections.sort} di
	 * {@code ParameterTambahanMahasiswaListener.onEvent}), bukan daftar entity ini. Satu-satunya
	 * pemakaian implisit adalah lewat
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)}, yang karena method ini tidak
	 * pernah mengembalikan {@code null} selalu berhenti di cabang pertama.</p>
	 *
	 * @return nomor urut turunan dari {@link ParameterTambahan}, atau {@code 1} bila tidak
	 *         tersedia; tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut. Tanpa validasi.
	 *
	 * <p><b>Praktis tidak berguna:</b> {@link #getNomorUrut()} menimpa kembali nilai ini dari
	 * {@link ParameterTambahan} pada pembacaan berikutnya. Tidak ada pemanggil di codebase.</p>
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public ParameterTambahanMahasiswa() {
	}

	/**
	 * Mengembalikan primary key baris.
	 *
	 * <p>Kolom {@code id} bertanda {@code insertable = false} dan
	 * {@code GenerationType.IDENTITY} — nilainya dibangkitkan basis data (sequence/serial), jadi
	 * baris baru masih bernilai {@code null} sampai berhasil di-{@code flush}.</p>
	 *
	 * @return primary key, atau {@code null} untuk instance yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate; kode aplikasi tidak perlu menyetelnya sendiri.</p>
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan cakupan jurusan pemetaan ini, setelah proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Tidak ditegakkan.</b> Nilai ini hanya ditampilkan sebagai kolom grid pada layar
	 * admin (kosong dirender sebagai "Semua") dan dipakai sebagai kriteria pencarian di layar
	 * itu; tidak ada satu pun pembaca formulir biodata yang menyaring berdasarkan kolom ini —
	 * lihat catatan cakupan pada dokumentasi kelas.</p>
	 *
	 * @return jurusan cakupan, atau {@code null} bila berlaku untuk semua jurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel cakupan jurusan. Tanpa validasi; {@code null} berarti "semua jurusan".
	 *
	 * @param jurusan jurusan cakupan, boleh {@code null}
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan definisi field kustom yang dipetakan baris ini, setelah proxy lazy
	 * diresolusi lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Relasi inti entity ini dan satu-satunya sumber label, tipe inputan, daftar nilai
	 * pilihan, serta bendera {@code wajibDiisi}/{@code harusMenyertakanLampiran}/
	 * {@code lampiranWajibDiisi}/{@code aktif} yang dipakai saat merender dan memvalidasi
	 * formulir biodata. Kolom FK {@code parameter_tambahan} bertanda {@code nullable = false},
	 * jadi secara skema selalu terisi — namun pemanggil di
	 * {@code ParameterTambahanMahasiswaListener} tetap memeriksa {@code != null} karena
	 * {@code check()} bisa mengembalikan {@code null} untuk baris yatim (FK menunjuk baris yang
	 * sudah terhapus).</p>
	 *
	 * <p>Juga dipanggil dari {@link #getNomorUrut()}, sehingga membaca nomor urut ikut memicu
	 * resolusi proxy ini.</p>
	 *
	 * @return definisi field kustom; secara praktis tidak pernah {@code null} kecuali data
	 *         yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * Menyetel definisi field kustom yang dipetakan. Tanpa validasi.
	 *
	 * <p>Diisi dari combobox "Parameter Tambahan" pada form Tambah/Ubah layar
	 * {@code ParameterTambahanMahasiswaAction}. Mengganti nilainya <b>mengubah bagian kanan
	 * kunci</b> {@code "<idKelompok>-><idParameter>"}, sehingga isian dan lampiran mahasiswa
	 * yang sudah tersimpan dengan kunci lama menjadi tidak terjangkau.</p>
	 *
	 * @param parameterTambahan definisi field kustom; kolomnya {@code nullable = false} sehingga
	 *        {@code null} akan gagal saat {@code flush}
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan bendera "field ini berlaku untuk semua tahun angkatan", dengan
	 * <b>normalisasi</b>: {@code null} ditambal menjadi {@code true}.
	 *
	 * <p>Konsekuensi penting: baris yang kolomnya masih {@code null} — misalnya hasil impor
	 * Excel atau baris lama sebelum kolom ini ada — otomatis dianggap berlaku untuk
	 * <b>semua</b> angkatan, bukan tidak berlaku untuk siapa pun. Default ini "membuka", bukan
	 * "menutup".</p>
	 *
	 * <p>Penambalan terjadi pada field instance, sehingga pembacaan dalam sesi Hibernate aktif
	 * dapat memicu {@code UPDATE} + revisi Envers pada baris yang sekadar terbaca.</p>
	 *
	 * <p>Bersama {@link #getTahunAngkatans()} inilah satu-satunya penyaring cakupan yang
	 * benar-benar dipakai pembaca: {@code Restrictions.or(eq("tampilDiSemuaTahunAngkatan",
	 * true), ilike("tahunAngkatans", ";" + angkatan + ";", ANYWHERE))}.</p>
	 *
	 * @return {@code true} bila berlaku untuk semua angkatan; tidak pernah {@code null}
	 */
	public Boolean getTampilDiSemuaTahunAngkatan() {
		if (tampilDiSemuaTahunAngkatan == null) {
			tampilDiSemuaTahunAngkatan = true;
		}
		return tampilDiSemuaTahunAngkatan;
	}

	/**
	 * Menyetel bendera "berlaku untuk semua tahun angkatan". Tanpa validasi.
	 *
	 * <p>Disetel langsung dari checkbox "Semua" pada baris grid layar admin, yang segera
	 * memanggil {@code Common.refreshSaveOrUpdate} — perubahan tersimpan tanpa tombol Simpan.
	 * Perhatikan bahwa nilai {@code false} tidak otomatis mengosongkan
	 * {@link #getTahunAngkatans()}, dan sebaliknya nilai {@code true} membuat isi
	 * {@code tahunAngkatans} diabaikan sepenuhnya (kedua kondisi digabung dengan {@code OR}).</p>
	 *
	 * @param tampilDiSemuaTahunAngkatan bendera baru
	 */
	public void setTampilDiSemuaTahunAngkatan(Boolean tampilDiSemuaTahunAngkatan) {
		this.tampilDiSemuaTahunAngkatan = tampilDiSemuaTahunAngkatan;
	}

	/**
	 * Mengembalikan daftar tahun angkatan tempat field ini berlaku, dengan <b>normalisasi</b>:
	 * {@code null} dikembalikan sebagai string kosong.
	 *
	 * <p><b>Format:</b> setiap angkatan dibungkus titik koma di kedua sisi lalu dirangkai tanpa
	 * pemisah tambahan, misalnya {@code ";2020;;2021;;2022;"}. Bentuk berbungkus itu disengaja
	 * supaya pencocokan {@code ilike ";2021;" ANYWHERE} tidak salah mengenali {@code "2021"}
	 * sebagai bagian dari {@code "12021"}. Pembacaan baliknya di layar admin memakai
	 * {@code split(";")} lalu menyaring token kosong dan non-numerik.</p>
	 *
	 * <p>Penambalan {@code null} menjadi {@code ""} terjadi pada field instance, sehingga
	 * pembacaan dalam sesi aktif dapat memicu {@code UPDATE} + revisi Envers.</p>
	 *
	 * <p>Nilai ini hanya berpengaruh bila {@link #getTampilDiSemuaTahunAngkatan()} bernilai
	 * {@code false}; layar admin bahkan hanya merender daftar checkbox angkatan dalam kondisi
	 * itu.</p>
	 *
	 * @return daftar angkatan berformat {@code ";t1;;t2;"}, atau string kosong; tidak pernah
	 *         {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getTahunAngkatans() {
		if (tahunAngkatans == null) {
			tahunAngkatans = "";
		}
		return tahunAngkatans;
	}

	/**
	 * Menyetel daftar tahun angkatan. Tanpa validasi format sama sekali.
	 *
	 * <p>Dirakit ulang dari nol oleh listener {@code onCheck} checkbox angkatan pada layar admin
	 * (menyusun {@code ";" + angkatan + ";"} untuk tiap checkbox tercentang) lalu langsung
	 * disimpan lewat {@code Common.refreshSaveOrUpdate}. String yang tidak mengikuti format
	 * berbungkus titik koma tidak akan pernah cocok dengan pencarian {@code ilike} pembaca —
	 * field-nya diam-diam tidak muncul untuk siapa pun.</p>
	 *
	 * @param tahunAngkatans daftar angkatan baru
	 */
	public void setTahunAngkatans(String tahunAngkatans) {
		this.tahunAngkatans = tahunAngkatans;
	}

	/**
	 * Mengembalikan cakupan fakultas pemetaan ini, setelah proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Tidak ditegakkan</b> — sama seperti {@link #getJurusan()}, hanya dipakai untuk
	 * tampilan dan pencarian di layar admin.</p>
	 *
	 * @return fakultas cakupan, atau {@code null} bila berlaku untuk semua fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menyetel cakupan fakultas. Tanpa validasi; {@code null} berarti "semua fakultas".
	 *
	 * @param fakultas fakultas cakupan, boleh {@code null}
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan cakupan program studi sebagai teks bebas.
	 *
	 * <p>Berbeda dari {@link #getFakultas()}/{@link #getJurusan()}/{@link #getJenjang()}, program
	 * disimpan sebagai <b>string</b>, bukan relasi — pola yang sama dipakai di banyak entity AIS
	 * lain. Nilainya berasal dari combobox yang diisi {@code Common.initPrograms}. Tidak ada
	 * normalisasi: bisa {@code null} maupun string kosong, dan layar admin memperlakukan
	 * keduanya sama ("Semua").</p>
	 *
	 * <p><b>Tidak ditegakkan</b> oleh pembaca formulir biodata.</p>
	 *
	 * @return nama program studi, atau {@code null}/string kosong bila berlaku untuk semua
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * Menyetel cakupan program studi. Tanpa validasi terhadap daftar program yang sah.
	 *
	 * @param program nama program studi, boleh {@code null}
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan cakupan jenjang pemetaan ini, setelah proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Tidak ditegakkan</b> — sama seperti {@link #getFakultas()}.</p>
	 *
	 * @return jenjang cakupan, atau {@code null} bila berlaku untuk semua jenjang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang", nullable = true)
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		return jenjang;
	}

	/**
	 * Menyetel cakupan jenjang. Tanpa validasi; {@code null} berarti "semua jenjang".
	 *
	 * @param jenjang jenjang cakupan, boleh {@code null}
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	/**
	 * Mengembalikan kelompok/seksi tempat field ini dirender, setelah proxy lazy diresolusi
	 * lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Relasi inti kedua entity ini. Id yang dikembalikan menjadi <b>bagian kiri kunci
	 * gabungan</b> {@code "<idKelompok>-><idParameter>"} yang dipakai untuk menyimpan nilai
	 * isian di {@link BiodataMahasiswa} sekaligus menautkan berkas
	 * {@link ais.database.model.file.LampiranLain}; {@link KelompokParameterTambahanMahasiswa#getNama()}
	 * dipakai sebagai judul seksi dan sebagai bagian kiri versi berlabel. Bendera
	 * {@code aktif} kelompok ikut menyaring: menonaktifkan satu kelompok menyembunyikan
	 * seluruh field di dalamnya sekaligus.</p>
	 *
	 * <p>Kolom FK {@code kelompok_parameter_tambahan_mahasiswa} bertanda
	 * {@code nullable = false}, namun pemanggil tetap memeriksa {@code != null} karena
	 * {@code check()} bisa mengembalikan {@code null} untuk baris yatim.</p>
	 *
	 * @return kelompok pemilik; secara praktis tidak pernah {@code null} kecuali data yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_mahasiswa", nullable = false)
	public KelompokParameterTambahanMahasiswa getKelompokParameterTambahanMahasiswa() {
		kelompokParameterTambahanMahasiswa = check(kelompokParameterTambahanMahasiswa);
		return kelompokParameterTambahanMahasiswa;
	}

	/**
	 * Menyetel kelompok/seksi pemilik. Tanpa validasi.
	 *
	 * <p>Diisi dari combobox "Kelompok" pada form Tambah/Ubah layar admin, yang isinya dijamin
	 * tidak pernah kosong karena {@code doAfterCompose} memanggil
	 * {@link KelompokParameterTambahanMahasiswa#checkCreateDefault()} lebih dulu.</p>
	 *
	 * <p><b>Efek data:</b> memindahkan field ke kelompok lain mengubah bagian kiri kunci
	 * gabungan, sehingga seluruh isian dan lampiran mahasiswa yang sudah tersimpan dengan kunci
	 * lama menjadi yatim — nilainya tetap ada di kolom teks {@link BiodataMahasiswa} tetapi
	 * tidak akan pernah dicocokkan lagi, dan field tampil kosong.</p>
	 *
	 * @param kelompokParameterTambahanMahasiswa kelompok pemilik; kolomnya
	 *        {@code nullable = false} sehingga {@code null} akan gagal saat {@code flush}
	 */
	public void setKelompokParameterTambahanMahasiswa(
			KelompokParameterTambahanMahasiswa kelompokParameterTambahanMahasiswa) {
		this.kelompokParameterTambahanMahasiswa = kelompokParameterTambahanMahasiswa;
	}

}
