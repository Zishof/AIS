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

import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;

/**
 * Model data untuk satu ENTRI LOG akses API mobile: siapa yang mengakses (salah satu dari
 * dosen/pegawai/mahasiswa/siswa, atau akun staf generik {@link Tbmuser}), dari IP/hostname mana,
 * kapan login/logout, status sukses, deskripsi aksi, serta payload request ({@link
 * #getLinkProfile()}) dan response ({@link #getHeader()}) yang TERPOTONG (truncate).
 *
 * <p><b>Catatan keamanan data sensitif (task_78a5b1ab-style, SUDAH DIMITIGASI).</b> Nama field
 * {@link #getHeader()} MENYESATKAN: berdasarkan satu-satunya pemanggil di
 * {@code ais.action.servlet.api.ApiMobileLogger}, kolom ini sebenarnya menyimpan {@code
 * responseBody} (badan RESPONS API) yang sudah dipotong ({@code truncate}), BUKAN header HTTP
 * mentah. {@link #getLinkProfile()} menyimpan payload REQUEST (dalam bentuk JSON) yang sudah
 * disaring lewat {@code redactSensitive(...)} SEBELUM disimpan -- template/probe biometrik
 * ({@code template_base64}, {@code probe_base64}, {@code biometric_template}) diganti
 * {@code "[REDACTED]"} secara eksplisit walau dalam mode debug. Ini pola MITIGASI yang SUDAH ADA
 * (bukan celah baru): sebelum menambah pemanggil baru ke tabel ini, pastikan payload yang
 * dikirim melalui jalur penyaringan yang sama, karena model ini sendiri TIDAK melakukan
 * penyaringan apa pun -- tanggung jawab itu sepenuhnya ada di pemanggil.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code String ip}, {@code Dosen dosen}, {@code Pegawai
 * pegawai}, {@code Mahasiswa mahasiswa}, {@code Siswa siswa}, {@code Tbmuser tbmuser}, {@code String
 * sessionid}, {@code String linkProfile}, {@code String header}; pemetaan persistence: tabel
 * {@code public.log_mobile} (TANPA {@code @Audited} -- berbeda dari kebanyakan entity lain di klaster ini,
 * perubahan pada baris log ini TIDAK dicatat Envers); pembacaan/pencarian ({@code getOlehId()}, {@code
 * getId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getIp()}, {@code getTbmuser()}); mutasi
 * data ({@code setOlehId()}, {@code setId()}, {@code setOleh()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah()}, {@code setIp()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>{@link #getTbmuser()} menolak dirinya sendiri</b> bila {@link #mahasiswa} atau {@link #siswa}
 * terisi -- pola yang sama seperti {@link DiskusiPengumumanAkademis#getTbmuser()}, menegakkan
 * "satu jenis akun saja" di level getter.</p>
 * <p><b>Retensi:</b> ada layanan {@code LogMobileCleanupService}/{@code LogMobileCleanupListener} terpisah
 * yang membersihkan baris lama tabel ini secara berkala -- data log mobile TIDAK disimpan permanen.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see LogLogin pola log akses sejenis untuk login web (bukan API mobile)
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(schema = "public", name = "log_mobile")
public class LogMobile extends GeneralValueObject {

	/** Versi serialisasi Java untuk kompatibilitas {@code Serializable}. */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key entity (kolom {@code id}, identity/auto-increment). */
	private Long id;
	/**
	 * Nama pengguna pengubah terakhir. Field ini MENIMPA (shadow) field bernama sama pada
	 * {@link GeneralValueObject}; getter/setter di bawah beroperasi pada field lokal ini.
	 */
	private String oleh;
	/** Id pengguna pengubah terakhir; shadow dari field sama pada {@link GeneralValueObject}. */
	private String olehId;

	/**
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link GeneralValueObject#setOlehId(String)}.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA sebelum UPDATE: memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir yang baru. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas untuk debug/log: {@link #getNama()} apa adanya. */
	public String toString() {
		return nama;
	}

	/** Nama aksi API yang diakses (mis. nama endpoint/action). */
	private String nama;
	/** Alamat IP klien pemanggil. */
	private String ip;
	/** Keterangan bebas. */
	private String keterangan;
	/** Waktu login/akses; default saat object dibuat. */
	private Date login = ais.ui.util.WaktuUtil.getDate();
	/** Waktu logout, boleh {@code null}. */
	private Date logout;
	/** Dosen pengakses (bila akun dosen). */
	private Dosen dosen;
	/** Pegawai pengakses (bila akun pegawai). */
	private Pegawai pegawai;
	/** Mahasiswa pengakses (bila akun mahasiswa). */
	private Mahasiswa mahasiswa;
	/** Akun staf generik pengakses, boleh ternullkan oleh {@link #getTbmuser()}. */
	private Tbmuser tbmuser;

	/** Jurusan terkait pengakses (opsional). */
	private Jurusan jurusan;
	/** Fakultas terkait pengakses (opsional). */
	private Fakultas fakultas;

	/** Siswa pengakses (bila akun siswa). */
	private Siswa siswa;
	/** Sekolah terkait pengakses (opsional). */
	private Sekolah sekolah;
	/** Yayasan terkait pengakses (opsional). */
	private Yayasan yayasan;

	/** Hostname server yang menerima request. */
	private String hostname;
	/** Menandai request berhasil diproses. */
	private Boolean success_status;
	/** Deskripsi bebas mengenai aksi/hasil. */
	private String description;
	/** Id sesi HTTP klien. */
	private String sessionid;
	/** Payload REQUEST (JSON, sudah disaring/redacted) yang dipotong; lihat catatan keamanan pada javadoc class. */
	private String linkProfile;
	/** Nama menyesatkan -- sebenarnya menyimpan badan RESPONS API yang dipotong; lihat catatan keamanan pada javadoc class. */
	private String header;

	/** Konstruktor kosong, dipakai Hibernate. */
	public LogMobile() {
	}

	/**
	 * Membangun entri log minimal dengan id sesi dan (yang sebenarnya adalah) badan respons.
	 *
	 * @param sessionid id sesi HTTP klien.
	 * @param header    badan respons API yang dipotong (lihat catatan keamanan pada javadoc class).
	 */
	public LogMobile(String sessionid, String header) {
		this.sessionid = sessionid;
		this.header = header;
	}

	/** @return primary key entity, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama aksi API, sudah di-{@code trim}; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama aksi API baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan bebas yang baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param dosen dosen pengakses yang baru. */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/** @return dosen pengakses, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/** @param mahasiswa mahasiswa pengakses yang baru. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return mahasiswa pengakses, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/** @param jurusan jurusan terkait yang baru. */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/** @return jurusan terkait pengakses, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/** @param fakultas fakultas terkait yang baru. */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/** @return fakultas terkait pengakses, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/** @param yayasan yayasan terkait yang baru; diabaikan menjadi {@code null} bila argumen {@code null} atau belum punya id. */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/** @return yayasan terkait pengakses, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/** @return sekolah terkait pengakses, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/** @param sekolah sekolah terkait yang baru; diabaikan menjadi {@code null} bila argumen {@code null} atau belum punya id. */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/** @param tbmuser akun staf generik pengakses yang baru. */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * @return akun staf generik pengakses, atau {@code null} bila {@link #mahasiswa} atau
	 *         {@link #siswa} terisi -- getter ini SENGAJA mengosongkan field bila salah satu
	 *         relasi peserta didik itu terisi, pola sama seperti
	 *         {@link DiskusiPengumumanAkademis#getTbmuser()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		if (mahasiswa != null) {
			tbmuser = null;
		}
		if (siswa != null) {
			tbmuser = null;
		}
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/** @param ip alamat IP klien yang baru. */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/** @return alamat IP klien, sudah di-{@code trim}; string kosong (bukan {@code null}) bila belum diisi. */
	public String getIp() {
		return ip == null ? "" : ip.trim();
	}

	/** @param logout waktu logout yang baru. */
	public void setLogout(Date logout) {
		this.logout = logout;
	}

	/** @return waktu logout, boleh {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getLogout() {
		return logout;
	}

	/** @param login waktu login/akses yang baru. */
	public void setLogin(Date login) {
		this.login = login;
	}

	/** @return waktu login/akses. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getLogin() {
		return login;
	}

	/** @param hostname hostname server yang baru. */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/** @return hostname server yang menerima request, boleh {@code null}. */
	public String getHostname() {
		return hostname;
	}

	/** @param success_status penanda sukses yang baru. */
	public void setSuccess_status(Boolean success_status) {
		this.success_status = success_status;
	}

	/** @return {@code true} bila request berhasil diproses, boleh {@code null} bila belum diisi. */
	public Boolean getSuccess_status() {
		return success_status;
	}

	/** @param description deskripsi bebas yang baru. */
	public void setDescription(String description) {
		this.description = description;
	}

	/** @return deskripsi bebas mengenai aksi/hasil, boleh {@code null}. */
	public String getDescription() {
		return description;
	}

	/** @return pegawai pengakses, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/** @param pegawai pegawai pengakses yang baru. */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * @return payload REQUEST (JSON, sudah disaring/redacted oleh pemanggil) yang dipotong,
	 *         boleh {@code null}. Lihat catatan keamanan pada javadoc class -- nama field ini
	 *         menyimpan konten permintaan, BUKAN tautan profil.
	 */
	@Column(name = "link_profile", nullable = true, columnDefinition = "text")
	public String getLinkProfile() {
		return linkProfile;
	}

	/** @param linkProfile payload request (JSON, sudah disaring) yang baru. */
	public void setLinkProfile(String linkProfile) {
		this.linkProfile = linkProfile;
	}

	/** @return siswa pengakses, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/** @param siswa siswa pengakses yang baru. */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/** @return id sesi HTTP klien, boleh {@code null}. */
	public String getSessionid() {
		return sessionid;
	}

	/** @param sessionid id sesi HTTP klien yang baru. */
	public void setSessionid(String sessionid) {
		this.sessionid = sessionid;
	}

	/**
	 * @return badan RESPONS API yang dipotong, boleh {@code null}. Lihat catatan keamanan pada
	 *         javadoc class -- nama field ini menyesatkan (bukan header HTTP).
	 */
	@Column(name = "header", nullable = true, columnDefinition = "text")
	public String getHeader() {
		return header;
	}

	/** @param header badan respons API (dipotong) yang baru. */
	public void setHeader(String header) {
		this.header = header;
	}

}
