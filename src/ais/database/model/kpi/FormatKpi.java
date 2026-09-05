package ais.database.model.kpi;

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

import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Entitas JPA/Hibernate untuk tabel {@code public.format_kpi} — template/format struktur
 * penilaian KPI, dilingkupi (scoped) ke satu atau beberapa unit organisasi ({@link #jurusan},
 * {@link #fakultas}, {@link #yayasan}, {@link #sekolah}, {@link #satuanKerja}).
 *
 * <p><b>Peran dalam model KPI:</b> {@code FormatKpi} adalah template tingkat-organisasi yang
 * menentukan susunan indikator KPI apa saja yang berlaku untuk satu ruang lingkup (mis. satu
 * jurusan/fakultas/sekolah/satuan kerja tertentu). Template ini kemudian ditugaskan ke pegawai
 * perorangan melalui {@link FormatKpiDetail} (satu baris {@code format_kpi_detail} = satu
 * penugasan template ke satu {@code Pegawai}, efektif sejak tanggal tertentu), dan baris-baris
 * item konkret di bawah penugasan tersebut direpresentasikan oleh {@link ItemKpi}. Lihat javadoc
 * {@link ItemKpi} untuk diagram alur relasi lengkap
 * ({@code FormatKpi -> FormatKpiDetail -> ItemKpi -> Kpi}).</p>
 *
 * <p>Selain lingkup organisasi, kelas ini juga menyimpan daftar username yang berwenang mengisi
 * nilai target ({@link #usernamePenggunaTarget}) dan nilai realisasi
 * ({@link #usernamePenggunaRealisasi}), serta jenis pengguna terkait
 * ({@link #jenisPengguna}/{@link #jenisPenggunaRealisasi}) — keempatnya disimpan sebagai string
 * berformat daftar dipisah koma yang DIBUNGKUS koma di kedua ujung (mis. {@code ",user1,user2,"})
 * agar pencarian keanggotaan dengan {@code contains(",usernameX,")} di kode pemanggil tidak
 * salah cocok pada substring nama pengguna lain (mis. {@code "budi"} vs {@code "budianto"}).</p>
 *
 * <p><b>Pola arsitektur berulang yang perlu diwaspadai:</b></p>
 * <ul>
 *   <li><b>Getter destruktif normalisasi CSV:</b> {@link #getUsernamePenggunaTarget()},
 *   {@link #getUsernamePenggunaRealisasi()}, {@link #getJenisPengguna()}, dan
 *   {@link #getJenisPenggunaRealisasi()} tidak sekadar membaca field — setiap pemanggilan
 *   menormalisasi ulang string (menambah/menghapus pembungkus koma, melipat koma ganda) dan
 *   MENIMPA field instance dengan hasil normalisasi tersebut sebelum dikembalikan. Karena
 *   entitas ini memakai {@code dynamicUpdate = true}, hasil normalisasi inilah yang akan ikut
 *   tersimpan ke kolom terkait pada operasi simpan berikutnya — bukan cuma efek tampilan.
 *   Konsisten dengan pola getter destruktif yang tercatat berulang di paket ini.</li>
 *   <li><b>Field relasi yang di-"check()" (shadow re-resolve):</b> {@link #getJurusan()},
 *   {@link #getFakultas()}, {@link #getYayasan()}, {@link #getSekolah()},
 *   {@link #getSatuanKerja()} — lihat {@link ais.database.model.GeneralValueObject#check(Object)};
 *   KEHARUSAN TEKNIS, bukan bug.</li>
 *   <li><b>Flag {@code aktif} satu-arah:</b> {@link #getAktif()} men-default {@code null} ke
 *   {@code true} tanpa menuliskannya kembali ke field — konsisten dengan {@link Kpi},
 *   {@link ItemKpi}, {@link FormatKpiDetail}.</li>
 *   <li><b>Field bayangan audit:</b> {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *   dideklarasikan ulang lokal per entitas ber-{@code @Audited} — KEHARUSAN TEKNIS untuk
 *   Hibernate Envers, bukan duplikasi keliru.</li>
 * </ul>
 *
 * @see FormatKpiDetail
 * @see ItemKpi
 * @see Kpi
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "format_kpi")
public class FormatKpi extends GeneralValueObject {

	/**
	 * Versi serialisasi untuk kompatibilitas {@link java.io.Serializable}. Nilai identik dengan
	 * entitas-entitas lain dalam paket {@code kpi} — peninggalan hasil generate hbm2java.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer (identity, auto-increment) baris {@code format_kpi}. */
	private Long id;

	/**
	 * Nama/username pengguna yang melakukan perubahan terakhir pada baris ini. Field bayangan
	 * audit, diisi oleh interceptor Hibernate — lihat catatan kelas.
	 */
	private String oleh;

	/**
	 * Id/identifier pengguna yang melakukan perubahan terakhir pada baris ini. Pasangan dari
	 * {@link #oleh}, diisi oleh mekanisme audit yang sama.
	 */
	private String olehId;

	/**
	 * Mengembalikan id/identifier pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Menolak (no-op) bila argumen {@code null} atau
	 * kosong/spasi saja, sehingga nilai lama tetap dipertahankan.
	 *
	 * @param olehId id pengguna baru; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi string untuk debugging/log: nama format KPI ini.
	 *
	 * @return nilai {@link #getNama()} (diakses langsung lewat field, bukan lewat getter)
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Mengisi nama/username pengguna yang melakukan perubahan terakhir. Menolak (no-op) bila
	 * argumen {@code null} atau kosong/spasi saja.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/username pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat
	 * sebelum operasi UPDATE dieksekusi, mendelegasikan pencatatan stempel waktu perubahan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu saat ini pada saat objek dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir secara eksplisit. Biasanya dipanggil oleh
	 * mekanisme audit ({@link #onUpdate()}), bukan oleh kode aplikasi.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir pada baris ini.
	 *
	 * @return tanggal/waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode identifikasi format KPI ini. */
	private String kode;
	/** Nama/judul format KPI ini. */
	private String nama;
	/** Keterangan/catatan bebas untuk format KPI ini. */
	private String keterangan;

	/** Penanda aktif/tidak; default {@code true} bila belum diisi — lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Daftar username (format {@code ,user1,user2,}) yang berwenang mengisi nilai TARGET pada
	 * item-item KPI di bawah format ini.
	 */
	private String usernamePenggunaTarget;
	/**
	 * Daftar username (format {@code ,user1,user2,}) yang berwenang mengisi nilai REALISASI pada
	 * item-item KPI di bawah format ini.
	 */
	private String usernamePenggunaRealisasi;

	/** Jurusan yang menjadi lingkup format ini, bila format dibatasi ke satu jurusan tertentu. */
	private Jurusan jurusan;
	/** Fakultas yang menjadi lingkup format ini, bila format dibatasi ke satu fakultas tertentu. */
	private Fakultas fakultas;
	/** Yayasan yang menjadi lingkup format ini, bila format dibatasi ke satu yayasan tertentu. */
	private Yayasan yayasan;
	/** Sekolah yang menjadi lingkup format ini, bila format dibatasi ke satu sekolah tertentu. */
	private Sekolah sekolah;
	/** Satuan kerja yang menjadi lingkup format ini, bila format dibatasi ke satu satuan kerja tertentu. */
	private SatuanKerja satuanKerja;
	/** Jenis/kategori pengguna (format {@code ,jenis1,jenis2,}) yang berwenang mengisi nilai target. */
	private String jenisPengguna;
	/** Jenis/kategori pengguna (format {@code ,jenis1,jenis2,}) yang berwenang mengisi nilai realisasi. */
	private String jenisPenggunaRealisasi;

	/** Konstruktor tanpa argumen, dipakai Hibernate untuk membentuk instance via reflection. */
	public FormatKpi() {
	}

	/**
	 * Mengembalikan kunci primer baris {@code format_kpi}. Kolom identity
	 * ({@code insertable = false}) — nilainya dibuat oleh basis data saat INSERT.
	 *
	 * @return id baris ini, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id secara manual. Jarang dipakai aplikasi karena kolom bersifat
	 * {@code insertable = false}.
	 *
	 * @param id id baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama/judul format KPI ini.
	 *
	 * @return nama format KPI
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Mengisi nama/judul format KPI ini.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/catatan bebas untuk format KPI ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan/catatan bebas untuk format KPI ini.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi jurusan yang menjadi lingkup format ini.
	 *
	 * @param jurusan jurusan baru; {@code null} bila format tidak dibatasi ke jurusan tertentu
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan jurusan yang menjadi lingkup format ini. Field di-refresh lewat
	 * {@code check()} sebelum dikembalikan — lihat catatan shadow re-resolve pada javadoc kelas.
	 *
	 * @return jurusan lingkup format ini, atau {@code null} bila tidak dibatasi ke jurusan tertentu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Mengisi fakultas yang menjadi lingkup format ini.
	 *
	 * @param fakultas fakultas baru; {@code null} bila format tidak dibatasi ke fakultas tertentu
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan fakultas yang menjadi lingkup format ini. Field di-refresh lewat
	 * {@code check()} sebelum dikembalikan — lihat catatan shadow re-resolve pada javadoc kelas.
	 *
	 * @return fakultas lingkup format ini, atau {@code null} bila tidak dibatasi ke fakultas tertentu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Mengisi yayasan yang menjadi lingkup format ini. Berbeda dari setter relasi lain di kelas
	 * ini, setter ini secara eksplisit menormalisasi argumen bertipe "objek tanpa id" (proxy
	 * kosong/objek baru yang belum tersimpan) menjadi {@code null} — mencegah field menyimpan
	 * referensi ke entitas {@link Yayasan} yang belum punya identitas basis data.
	 *
	 * @param yayasan yayasan baru; disimpan sebagai {@code null} bila argumen {@code null} atau
	 *                {@code yayasan.getId()} bernilai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan yayasan yang menjadi lingkup format ini. Field di-refresh lewat
	 * {@code check()} sebelum dikembalikan — lihat catatan shadow re-resolve pada javadoc kelas.
	 *
	 * @return yayasan lingkup format ini, atau {@code null} bila tidak dibatasi ke yayasan tertentu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Mengembalikan sekolah yang menjadi lingkup format ini. Field di-refresh lewat
	 * {@code check()} sebelum dikembalikan — lihat catatan shadow re-resolve pada javadoc kelas.
	 *
	 * @return sekolah lingkup format ini, atau {@code null} bila tidak dibatasi ke sekolah tertentu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Mengisi sekolah yang menjadi lingkup format ini. Sama seperti {@link #setYayasan(Yayasan)},
	 * argumen "objek tanpa id" dinormalisasi menjadi {@code null}.
	 *
	 * @param sekolah sekolah baru; disimpan sebagai {@code null} bila argumen {@code null} atau
	 *                {@code sekolah.getId()} bernilai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan satuan kerja yang menjadi lingkup format ini. Field di-refresh lewat
	 * {@code check()} sebelum dikembalikan — lihat catatan shadow re-resolve pada javadoc kelas.
	 *
	 * @return satuan kerja lingkup format ini, atau {@code null} bila tidak dibatasi ke satuan kerja tertentu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja yang menjadi lingkup format ini.
	 *
	 * @param satuanKerja satuan kerja baru; {@code null} bila format tidak dibatasi ke satuan kerja tertentu
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan kode identifikasi format KPI ini. Berbeda dari {@link Kpi#getKode()}, getter
	 * ini TIDAK melakukan normalisasi apa pun — nilai field dikembalikan apa adanya.
	 *
	 * @return kode format KPI, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Mengisi kode identifikasi format KPI ini.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan status aktif format KPI ini.
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; nilai field bila sudah eksplisit di-set
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi status aktif format KPI ini.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan daftar username yang berwenang mengisi nilai TARGET pada item-item KPI di
	 * bawah format ini, dalam bentuk string dipisah koma DIBUNGKUS koma di kedua ujung (mis.
	 * {@code ",budi,siti,"}) — format ini memudahkan pengecekan keanggotaan yang aman-substring
	 * di kode pemanggil dengan {@code contains(",usernameX,")}.
	 *
	 * <p><b>Getter destruktif:</b> setiap pemanggilan MENORMALISASI ULANG field
	 * {@code usernamePenggunaTarget} — menambahkan pembungkus koma bila belum ada, lalu melipat
	 * koma berturut-turut ganda/tiga/empat menjadi satu koma tunggal (dieksekusi berulang untuk
	 * menangani sisa lipatan), dan mengosongkan hasil bila ternyata hanya berisi kombinasi koma
	 * kosong. Hasil normalisasi ini DITUGASKAN KEMBALI ke field sebelum dikembalikan — karena
	 * entitas memakai {@code dynamicUpdate = true}, versi ternormalisasi inilah yang tersimpan
	 * ke kolom {@code username_pengguna_target} pada operasi simpan berikutnya, bukan string
	 * asli yang di-set lewat {@link #setUsernamePenggunaTarget(String)}.</p>
	 *
	 * @return daftar username dibungkus koma di kedua ujung, atau string kosong bila tidak ada
	 *         username yang terdaftar
	 */
	@Column(name = "username_pengguna_target", nullable = true, columnDefinition = "text")
	public String getUsernamePenggunaTarget() {

		usernamePenggunaTarget = (usernamePenggunaTarget == null || usernamePenggunaTarget.trim().equalsIgnoreCase(",")
				? ""
				: "," + usernamePenggunaTarget.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (usernamePenggunaTarget.equals(",")) {
			usernamePenggunaTarget = "";
		} else if (usernamePenggunaTarget.equals(",,")) {
			usernamePenggunaTarget = "";
		} else if (usernamePenggunaTarget.equals(",,,")) {
			usernamePenggunaTarget = "";
		} else if (usernamePenggunaTarget.equals(",,,,")) {
			usernamePenggunaTarget = "";
		}

		return usernamePenggunaTarget == null ? "" : usernamePenggunaTarget.trim();
	}

	/**
	 * Mengisi daftar username yang berwenang mengisi nilai TARGET secara manual (mentah, belum
	 * dinormalisasi). Normalisasi baru terjadi pada pemanggilan {@link #getUsernamePenggunaTarget()}
	 * berikutnya.
	 *
	 * @param usernamePenggunaTarget daftar username baru, format bebas (akan dinormalisasi oleh getter)
	 */
	public void setUsernamePenggunaTarget(String usernamePenggunaTarget) {
		this.usernamePenggunaTarget = usernamePenggunaTarget;
	}

	/**
	 * Mengembalikan daftar username yang berwenang mengisi nilai REALISASI pada item-item KPI di
	 * bawah format ini. Perilaku normalisasi identik dengan
	 * {@link #getUsernamePenggunaTarget()} — lihat javadoc method tersebut untuk penjelasan
	 * lengkap mengenai efek samping getter destruktif dan alasan pembungkusan koma.
	 *
	 * @return daftar username dibungkus koma di kedua ujung, atau string kosong bila tidak ada
	 *         username yang terdaftar
	 */
	@Column(name = "username_pengguna_realisasi", nullable = true, columnDefinition = "text")
	public String getUsernamePenggunaRealisasi() {

		usernamePenggunaRealisasi = (usernamePenggunaRealisasi == null
				|| usernamePenggunaRealisasi.trim().equalsIgnoreCase(",") ? ""
						: "," + usernamePenggunaRealisasi.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (usernamePenggunaRealisasi.equals(",")) {
			usernamePenggunaRealisasi = "";
		} else if (usernamePenggunaRealisasi.equals(",,")) {
			usernamePenggunaRealisasi = "";
		} else if (usernamePenggunaRealisasi.equals(",,,")) {
			usernamePenggunaRealisasi = "";
		} else if (usernamePenggunaRealisasi.equals(",,,,")) {
			usernamePenggunaRealisasi = "";
		}

		return usernamePenggunaRealisasi == null ? "" : usernamePenggunaRealisasi.trim();
	}

	/**
	 * Mengisi daftar username yang berwenang mengisi nilai REALISASI secara manual (mentah,
	 * belum dinormalisasi).
	 *
	 * @param usernamePenggunaRealisasi daftar username baru, format bebas (akan dinormalisasi oleh getter)
	 */
	public void setUsernamePenggunaRealisasi(String usernamePenggunaRealisasi) {
		this.usernamePenggunaRealisasi = usernamePenggunaRealisasi;
	}

	/**
	 * Mengembalikan daftar jenis/kategori pengguna yang berwenang mengisi nilai target, dengan
	 * normalisasi pembungkusan-koma yang sama seperti {@link #getUsernamePenggunaTarget()}.
	 * Tambahan di sini: nilai literal {@code ",-,"} (satu entri berisi tanda hubung saja, sisa
	 * dari penghapusan pilihan di UI) juga dianggap kosong dan dikosongkan.
	 *
	 * @return daftar jenis pengguna dibungkus koma di kedua ujung, atau string kosong bila tidak ada
	 */
	@Column(name = "jenis_pengguna", nullable = true, columnDefinition = "text")
	public String getJenisPengguna() {

		jenisPengguna = (jenisPengguna == null || jenisPengguna.trim().equalsIgnoreCase(",") ? ""
				: "," + jenisPengguna.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (jenisPengguna.equals(",")) {
			jenisPengguna = "";
		} else if (jenisPengguna.equals(",,")) {
			jenisPengguna = "";
		} else if (jenisPengguna.equals(",,,")) {
			jenisPengguna = "";
		} else if (jenisPengguna.equals(",,,,")) {
			jenisPengguna = "";
		}

		if (jenisPengguna.equals(",-,")) {
			jenisPengguna = "";
		}

		return jenisPengguna == null || jenisPengguna.trim().isEmpty() ? "" : jenisPengguna.trim();
	}

	/**
	 * Mengisi daftar jenis pengguna target secara manual (mentah, belum dinormalisasi).
	 *
	 * @param jenisPengguna daftar jenis pengguna baru
	 */
	public void setJenisPengguna(String jenisPengguna) {
		this.jenisPengguna = jenisPengguna;
	}

	/**
	 * Mengembalikan daftar jenis/kategori pengguna yang berwenang mengisi nilai realisasi.
	 * Perilaku normalisasi identik dengan {@link #getJenisPengguna()}, termasuk penanganan
	 * literal {@code ",-,"} sebagai nilai kosong.
	 *
	 * @return daftar jenis pengguna realisasi dibungkus koma di kedua ujung, atau string kosong bila tidak ada
	 */
	@Column(name = "jenis_pengguna_realisasi", nullable = true, columnDefinition = "text")
	public String getJenisPenggunaRealisasi() {
		jenisPenggunaRealisasi = (jenisPenggunaRealisasi == null || jenisPenggunaRealisasi.trim().equalsIgnoreCase(",")
				? ""
				: "," + jenisPenggunaRealisasi.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (jenisPenggunaRealisasi.equals(",")) {
			jenisPenggunaRealisasi = "";
		} else if (jenisPenggunaRealisasi.equals(",,")) {
			jenisPenggunaRealisasi = "";
		} else if (jenisPenggunaRealisasi.equals(",,,")) {
			jenisPenggunaRealisasi = "";
		} else if (jenisPenggunaRealisasi.equals(",,,,")) {
			jenisPenggunaRealisasi = "";
		}

		if (jenisPenggunaRealisasi.equals(",-,")) {
			jenisPenggunaRealisasi = "";
		}

		return jenisPenggunaRealisasi == null || jenisPenggunaRealisasi.trim().isEmpty() ? ""
				: jenisPenggunaRealisasi.trim();
	}

	/**
	 * Mengisi daftar jenis pengguna realisasi secara manual (mentah, belum dinormalisasi).
	 *
	 * @param jenisPenggunaRealisasi daftar jenis pengguna baru
	 */

	public void setJenisPenggunaRealisasi(String jenisPenggunaRealisasi) {
		this.jenisPenggunaRealisasi = jenisPenggunaRealisasi;
	}
}
