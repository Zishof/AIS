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
 * Model data untuk satu AKUN institusi/tenant yang mendaftar pada sistem AIS lewat landing page
 * self-service ebisnis.id -- entity BASE/akun anchor yang dipakai DUA jalur registrasi sekaligus:
 * (1) jalur LAMA onboarding oleh staf eCampus/eSchool (relasi {@link #getAdmin()} ke
 * {@link Tbmuser}, dipetakan sebelum ebisnis.id ada), dan (2) jalur BARU registrasi mandiri
 * publik lewat {@code PendaftarPublicHelper} (kredensial {@link #getPasswordHash()}/{@link
 * #getPasswordSalt()} serta field wilayah teks bebas). Baris di kelas ini menjadi ANCHOR bagi
 * entity {@link ais.database.model.tenant.PendaftarTenantProfile} (relasi {@code pendaftar} pada
 * kelas itu) yang menambahkan atribut KHUSUS jalur provisioning tenant modern (mis.
 * {@code lastLoginAt}) tanpa mengubah tabel {@code pendaftar} ini -- pola "extension profile" agar
 * jalur registrasi tenant baru ({@link ais.database.model.tenant.PendaftaranTenant}, workflow
 * permohonan provisioning) tidak mengganggu skema akun lama.
 *
 * <p><b>BUKAN entity yang sama dengan "{@code tenant.Pendaftar}".</b> Tidak ada kelas bernama
 * persis demikian di paket {@code ais.database.model.tenant} -- yang ada adalah
 * {@link ais.database.model.tenant.PendaftaranTenant} (satu baris = satu PERMOHONAN provisioning
 * tenant, dengan status alur {@code STATUS_*}, snapshot paket, timestamp tiap tahap) dan
 * {@link ais.database.model.tenant.PendaftarTenantProfile} (satu baris = profil EKSTENSI 1:1 milik
 * SATU baris {@code Pendaftar} ini, lewat FK {@code pendaftar}). Jadi hubungannya BUKAN "sama"
 * maupun "independen total": kelas ini adalah AKUN INDUK yang keduanya rujuk, sedangkan
 * {@code PendaftaranTenant}/{@code PendaftarTenantProfile} adalah lapisan WORKFLOW dan EKSTENSI
 * PROFIL yang dibangun DI ATAS akun {@link Pendaftar} ini untuk kebutuhan provisioning tenant yang
 * lebih lengkap.</p>
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code String domain}, {@code Tbmuser admin}, {@code String
 * passwordHash}, {@code String passwordSalt}, {@code Date dibuatPada}; pemetaan persistence: tabel
 * {@code public.pendaftar}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()},
 * {@code getTanggal_dirubah()}, {@code getDomain()}, {@code getAdmin()}, {@code getPasswordHash()}); mutasi
 * data ({@code setOlehId()}, {@code setId()}, {@code setOleh()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah()}, {@code setDomain()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Keamanan kredensial:</b> {@link #getPasswordHash()} menyimpan SHA-256(salt+password) heksadesimal,
 * BUKAN password mentah/reversibel -- sengaja tidak memakai {@code Common.desEncrypter} (dua-arah) yang
 * dipakai jalur {@link Tbmuser} lama, karena ini permukaan publik internet-facing baru. Lihat javadoc method
 * di bawah untuk rincian.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see ais.database.model.tenant.PendaftarTenantProfile profil ekstensi 1:1 milik satu baris kelas ini
 * @see ais.database.model.tenant.PendaftaranTenant permohonan provisioning tenant yang merujuk pemilik akun ({@link Pendaftar})
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pendaftar")
public class Pendaftar extends GeneralValueObject {

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

	/** @return representasi ringkas untuk debug/log: {@code "<id>-<nama>"}. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas akun, boleh {@code null}. */
	private String kode;
	/** Nama institusi/bisnis pendaftar. */
	private String nama;
	/** Keterangan bebas. */
	private String keterangan;
	/** Subdomain/domain unik akun ini pada platform (wajib, unik). */
	private String domain;
	/** Alamat surel akun (dipakai login mandiri lewat {@code PendaftarPublicHelper}). */
	private String email;
	/** Alamat institusi. */
	private String alamat;
	/** Nomor telepon institusi. */
	private String telp;
	/** Nama kontak person (PIC). */
	private String kontakperson;
	/** Nomor telepon kontak person. */
	private String telpkontakperson;
	/** Alamat surel kontak person. */
	private String emailkontakperson;
	/** Menandai akun masih aktif; lihat {@link #getAktif()} untuk perilaku default. */
	private Boolean aktif;
	/** Menandai institusi ini berjenis sekolah (vs jenis lain); lihat {@link #getMerupakanSekolah()}. */
	private Boolean merupakanSekolah;
	/** Motto/slogan institusi, tampil di landing page. */
	private String motto;
	/** Path berkas CSS kustom landing page; lihat {@link #getCss()} untuk perilaku default. */
	private String css;

	// ---- Registrasi mandiri ebisnis.id (landing page ebisnis.jsp) ----
	// Kelompok "Pendaftar/Account Owner" pada BRD ebisnis.id -- field wilayah &
	// kredensial login SENGAJA dipisah dari relasi admin/Tbmuser di bawah (itu
	// dipakai alur staf eCampus/eSchool yang sudah ada), supaya jalur registrasi
	// publik baru ini tidak mengganggu jalur lama sama sekali (lihat JavaDoc
	// PendaftarPublicHelper). Wilayah disimpan sbg teks bebas (bukan FK ke tabel
	// master negara/provinsi/kota) -- cukup utk tahap ini, referensial penuh
	// menyusul bila memang dibutuhkan.
	/** Negara (teks bebas, bukan FK); lihat {@link #getNegara()} untuk perilaku default. */
	private String negara;
	/** Provinsi (teks bebas, bukan FK), boleh {@code null}. */
	private String provinsi;
	/** Kota/kabupaten (teks bebas, bukan FK), boleh {@code null}. */
	private String kotaKabupaten;
	/** Kecamatan (teks bebas, bukan FK), boleh {@code null}. */
	private String kecamatan;
	/** Jenis bisnis institusi (teks bebas), boleh {@code null}. */
	private String jenisBisnis;
	/** Hash SHA-256(salt+password) heksadesimal; lihat javadoc {@link #getPasswordHash()}. */
	private String passwordHash;
	/** Salt acak unik per akun, dipakai bersama {@link #passwordHash}. */
	private String passwordSalt;
	/** Waktu akun ini dibuat lewat jalur registrasi mandiri. */
	private Date dibuatPada;

	/** Relasi ke akun {@link Tbmuser} admin -- jalur LAMA onboarding staf eCampus/eSchool. */
	private Tbmuser admin;

	/** Konstruktor kosong, dipakai Hibernate. */
	public Pendaftar() {
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

	/** @return kode ringkas akun, boleh {@code null}. */
	public String getKode() {
		return kode;
	}

	/** @param kode kode ringkas baru. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama institusi/bisnis, sudah di-{@code trim}; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama institusi/bisnis baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan bebas yang baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return {@code true} (default) bila akun masih aktif. */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif penanda aktif yang baru. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return subdomain/domain unik akun ini, {@code null} bila kosong/belum diisi. Wajib unik pada tabel. */
	@Column(unique = true, nullable = false)
	public String getDomain() {
		return domain == null || domain.trim().isEmpty() ? null : domain;
	}

	/** @param domain subdomain/domain unik baru. */
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/** @return alamat institusi, boleh {@code null}. */
	public String getAlamat() {
		return alamat;
	}

	/** @param alamat alamat institusi baru. */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/** @return nomor telepon institusi, boleh {@code null}. */
	public String getTelp() {
		return telp;
	}

	/** @param telp nomor telepon institusi baru. */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/** @return nama kontak person (PIC), boleh {@code null}. */
	public String getKontakperson() {
		return kontakperson;
	}

	/** @param kontakperson nama kontak person baru. */
	public void setKontakperson(String kontakperson) {
		this.kontakperson = kontakperson;
	}

	/** @return alamat surel akun, boleh {@code null}. */
	public String getEmail() {
		return email;
	}

	/** @param email alamat surel akun yang baru. */
	public void setEmail(String email) {
		this.email = email;
	}

	/** @return motto/slogan institusi, string kosong (bukan {@code null}) bila belum diisi. */
	public String getMotto() {
		return motto == null ? "" : motto.trim();
	}

	/** @param motto motto/slogan baru. */
	public void setMotto(String motto) {
		this.motto = motto;
	}

	/** @return {@code true} (default) bila institusi ini berjenis sekolah. */
	public Boolean getMerupakanSekolah() {
		return merupakanSekolah == null ? true : merupakanSekolah;
	}

	/** @param merupakanSekolah penanda jenis sekolah yang baru. */
	public void setMerupakanSekolah(Boolean merupakanSekolah) {
		this.merupakanSekolah = merupakanSekolah;
	}

	/** @return nomor telepon kontak person, boleh {@code null}. */
	public String getTelpkontakperson() {
		return telpkontakperson;
	}

	/** @param telpkontakperson nomor telepon kontak person baru. */
	public void setTelpkontakperson(String telpkontakperson) {
		this.telpkontakperson = telpkontakperson;
	}

	/** @return path berkas CSS kustom; default {@code "/css/ytb.css"} bila belum diisi. */
	public String getCss() {
		return css == null ? "/css/ytb.css" : css;
	}

	/** @param css path berkas CSS kustom baru. */
	public void setCss(String css) {
		this.css = css;
	}

	/**
	 * @return akun {@link Tbmuser} admin -- jalur LAMA onboarding staf eCampus/eSchool, boleh
	 *         {@code null} pada akun yang dibuat lewat registrasi mandiri ebisnis.id; dimuat
	 *         lewat {@link GeneralValueObject#check(Object)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "admin", nullable = true)
	public Tbmuser getAdmin() {
		admin = check(admin);
		return admin;
	}

	/** @param admin akun {@link Tbmuser} admin yang baru. */
	public void setAdmin(Tbmuser admin) {
		this.admin = admin;
	}

	/** @return alamat surel kontak person, boleh {@code null}. */
	public String getEmailkontakperson() {
		return emailkontakperson;
	}

	/** @param emailkontakperson alamat surel kontak person baru. */
	public void setEmailkontakperson(String emailkontakperson) {
		this.emailkontakperson = emailkontakperson;
	}

	/** @return negara (teks bebas, bukan FK); default {@code "Indonesia"} bila kosong. */
	@Column(name = "negara")
	public String getNegara() {
		return negara == null || negara.trim().isEmpty() ? "Indonesia" : negara;
	}

	/** @param negara negara (teks bebas) baru. */
	public void setNegara(String negara) {
		this.negara = negara;
	}

	/** @return provinsi (teks bebas, bukan FK), boleh {@code null}. */
	@Column(name = "provinsi")
	public String getProvinsi() {
		return provinsi;
	}

	/** @param provinsi provinsi (teks bebas) baru. */
	public void setProvinsi(String provinsi) {
		this.provinsi = provinsi;
	}

	/** @return kota/kabupaten (teks bebas, bukan FK), boleh {@code null}. */
	@Column(name = "kota_kabupaten")
	public String getKotaKabupaten() {
		return kotaKabupaten;
	}

	/** @param kotaKabupaten kota/kabupaten (teks bebas) baru. */
	public void setKotaKabupaten(String kotaKabupaten) {
		this.kotaKabupaten = kotaKabupaten;
	}

	/** @return kecamatan (teks bebas, bukan FK), boleh {@code null}. */
	@Column(name = "kecamatan")
	public String getKecamatan() {
		return kecamatan;
	}

	/** @param kecamatan kecamatan (teks bebas) baru. */
	public void setKecamatan(String kecamatan) {
		this.kecamatan = kecamatan;
	}

	/** @return jenis bisnis institusi (teks bebas), boleh {@code null}. */
	@Column(name = "jenis_bisnis")
	public String getJenisBisnis() {
		return jenisBisnis;
	}

	/** @param jenisBisnis jenis bisnis (teks bebas) baru. */
	public void setJenisBisnis(String jenisBisnis) {
		this.jenisBisnis = jenisBisnis;
	}

	/**
	 * Hash SHA-256(salt + password), disimpan HEX -- lihat {@link #getPasswordSalt()}
	 * dan {@link PendaftarPublicHelper#hashPassword}. TIDAK PERNAH menyimpan
	 * password mentah/reversibel (sengaja BUKAN {@code Common.desEncrypter} yang
	 * dipakai jalur Tbmuser lama -- ini permukaan publik internet-facing baru,
	 * jadi memakai hash satu-arah, konsisten dgn pola {@code PosDeviceToken}
	 * yang juga hanya menyimpan hash token, bukan token mentah).
	 */
	@Column(name = "password_hash")
	public String getPasswordHash() {
		return passwordHash;
	}

	/** @param passwordHash hash SHA-256(salt+password) baru; TIDAK PERNAH password mentah. */
	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	/** @return salt acak unik akun ini, dipakai bersama {@link #getPasswordHash()}; boleh {@code null} bila akun belum pernah set password mandiri. */
	@Column(name = "password_salt")
	public String getPasswordSalt() {
		return passwordSalt;
	}

	/** @param passwordSalt salt acak baru. */
	public void setPasswordSalt(String passwordSalt) {
		this.passwordSalt = passwordSalt;
	}

	/** @return waktu akun dibuat lewat jalur registrasi mandiri, boleh {@code null} untuk akun jalur lama (onboarding staf). */
	@Column(name = "dibuat_pada")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDibuatPada() {
		return dibuatPada;
	}

	/** @param dibuatPada waktu pembuatan akun yang baru. */
	public void setDibuatPada(Date dibuatPada) {
		this.dibuatPada = dibuatPada;
	}
}
