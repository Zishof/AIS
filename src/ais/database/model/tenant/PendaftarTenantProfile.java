package ais.database.model.tenant;

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
import javax.persistence.Version;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Pendaftar;

/**
 * <h3>Extension profile 1:1 di atas {@link Pendaftar} (control-plane pendaftaran tenant).</h3>
 *
 * <p>SENGAJA tabel extension, BUKAN kolom baru di {@code public.pendaftar}: (1) `Pendaftar`
 * juga dipakai eCampus/eSchool -- jangan membebani entity legacy; (2) menambah kolom ke entity
 * {@code @Audited} existing berisiko INSERT audit gagal krn `hbm2ddl=update` TIDAK menambah
 * kolom ke `new_audit.pendaftar__audit` (lihat peringatan src/hibernate.cfg.xml:41-48);
 * (3) unique `normalized_email` dapat ditegakkan HANYA utk akun self-service tanpa
 * membentur email legacy yang duplikat/kosong secara historis.</p>
 *
 * <h4>Siapa yang membuat dan membaca baris ini</h4>
 *
 * <p>Satu-satunya pembuat adalah {@code PendaftaranTenantService.buatProfile(...)}, dipanggil
 * dari dalam transaksi {@code submit(...)} dan HANYA ketika pendaftar benar-benar baru (jalur
 * anonim publik {@code /pendaftaran}). Pendaftar yang sudah punya akun self-service ditolak lebih
 * dulu dengan {@code EMAIL_ALREADY_REGISTERED} dan diminta login, sehingga tidak pernah ada dua
 * profile untuk satu {@link Pendaftar} -- keunikan itu juga ditegakkan DB lewat
 * {@code @JoinColumn(unique = true)} pada {@link #getPendaftar()}, bukan sekadar pemeriksaan
 * aplikasi.</p>
 *
 * <p>Pembaca baris ini hanya tiga: (1) {@code PendaftaranTenantService.verifikasiEmail}/
 * {@code verifikasiTanpaToken} yang menandai {@link #getEmailVerifiedAt()} +
 * {@link #getAccountStatus()}; (2) {@code TenantProvisioningService} yang mengambil
 * {@link #getTimezone()} sebagai zona waktu tenant baru; (3)
 * {@code ais.action.servlet.api.PendaftarPublicHelper.login} yang mencatat
 * {@link #getLastLoginAt()} secara best-effort.</p>
 *
 * <h4>Peringatan: status akun di sini BUKAN gerbang login</h4>
 *
 * <p>{@link #getAccountStatus()} tampak seperti gerbang keamanan, tetapi audit pemakai
 * menunjukkan tidak ada satu pun kode yang MEMBACANYA sebagai syarat. Login pendaftar
 * ({@code PendaftarPublicHelper.login}) memeriksa {@code Pendaftar.getAktif()}, bukan kolom ini.
 * Konstanta {@link #STATUS_SUSPENDED} bahkan tidak pernah ditulis oleh kode mana pun saat ini
 * (tidak ada fitur suspend akun tingkat pendaftar; pembekuan yang benar-benar ditegakkan ada di
 * tingkat tenant lewat {@link TenantRegistry} + {@code TenantContextResolver}). Konsekuensi
 * praktis yang HARUS diketahui: menyetel {@code account_status = 'SUSPENDED'} langsung di
 * database TIDAK akan memblokir login akun tersebut. Untuk benar-benar menonaktifkan akun,
 * setel {@code Pendaftar.aktif = false}. Bila kelak fitur suspend akun dibangun, gerbangnya
 * harus DITAMBAHKAN di jalur login -- jangan berasumsi kolom ini sudah menahan apa pun.</p>
 *
 * <h4>Kredensial: yang disimpan di sini hanya metadata</h4>
 *
 * <p>Hash dan salt password tetap tinggal di {@code pendaftar.password_hash/password_salt}
 * (kompatibilitas login lama). Tabel ini hanya memotret PARAMETER algoritmanya
 * ({@link #getPasswordAlgorithm()}, {@link #getPasswordVersion()},
 * {@link #getPasswordIterations()}) supaya kelak dimungkinkan upgrade-on-login: saat pengguna
 * berhasil login, hash lama dengan parameter usang dapat dihitung ulang memakai parameter baru.
 * Tidak ada password, salt, OTP, maupun token yang boleh masuk ke entity ini.</p>
 *
 * @see Pendaftar
 * @see PendaftaranTenant
 * @see TenantRegistry
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftar_tenant_profile")
public class PendaftarTenantProfile extends GeneralValueObject {

	/** Versi serialisasi Java standar entity AIS. */
	private static final long serialVersionUID = 1L;

	/**
	 * Status awal setiap profile baru: akun sudah terbentuk tetapi alamat email belum terbukti
	 * dikuasai pendaftar. Nilai ini juga yang dikembalikan {@link #getAccountStatus()} bila kolom
	 * kosong -- default fail-safe ke keadaan paling belum-terpercaya.
	 */
	public static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";
	/**
	 * Email sudah terverifikasi (lewat token publik maupun verifikasi manual admin). Satu-satunya
	 * transisi status yang benar-benar dijalankan kode saat ini adalah
	 * {@link #STATUS_PENDING_VERIFICATION} &rarr; {@code ACTIVE}.
	 */
	public static final String STATUS_ACTIVE = "ACTIVE";
	/**
	 * Akun dibekukan. <strong>Belum diimplementasikan:</strong> tidak ada kode yang menulis nilai
	 * ini dan tidak ada gerbang yang membacanya (lihat peringatan pada Javadoc kelas). Disediakan
	 * sebagai kosakata status untuk fase berikutnya.
	 */
	public static final String STATUS_SUSPENDED = "SUSPENDED";

	/** Primary key surrogate (identity, di-generate database). */
	private Long id;
	/** Akun {@link Pendaftar} yang diperluas profile ini (1:1, unique). */
	private Pendaftar pendaftar;
	/** Email login ternormalisasi, unique di tabel ini. */
	private String normalizedEmail;
	/** Nama orang yang ditampilkan sebagai pemilik akun. */
	private String ownerDisplayName;
	/** Nama badan usaha sesuai dokumen legal. */
	private String legalName;
	/** Nama dagang/merek yang dipakai sehari-hari. */
	private String tradeName;
	/** Bentuk badan usaha (PT, CV, Yayasan, dan sejenisnya). */
	private String legalForm;
	/** Nomor Induk Berusaha. */
	private String nib;
	/** Nomor Pokok Wajib Pajak. */
	private String npwp;
	/** Alamat situs web usaha. */
	private String website;
	/** Kode pos alamat usaha. */
	private String postalCode;
	/** Zona waktu pilihan, dipakai sebagai zona waktu tenant baru. */
	private String timezone;
	/** Locale tampilan pilihan pendaftar. */
	private String preferredLocale;
	/** Asal pendaftaran akun ({@code PUBLIC_FORM}). */
	private String registrationSource;
	/** Status akun -- informatif, bukan gerbang login (lihat Javadoc kelas). */
	private String accountStatus;
	/** Nama algoritma hash password yang dipakai saat akun dibuat. */
	private String passwordAlgorithm;
	/** Nomor versi skema hashing password. */
	private Integer passwordVersion;
	/** Jumlah iterasi turunan kunci (KDF) saat akun dibuat. */
	private Integer passwordIterations;
	/** Penanda wajib ganti password saat login berikutnya (belum ditegakkan). */
	private Boolean mustChangePassword;
	/** Waktu email terverifikasi. */
	private Date emailVerifiedAt;
	/** Waktu login terakhir yang berhasil. */
	private Date lastLoginAt;
	/** Waktu baris profile dibuat. */
	private Date createdAt;
	/** Nomor versi optimistic locking Hibernate. */
	private Integer version;

	/** Nama pengguna terakhir yang mengubah baris (shadow audit AIS). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris (shadow audit AIS). */
	private String olehId;
	/**
	 * Stempel waktu perubahan terakhir + hook {@code @PreUpdate} yang menyegarkannya lewat
	 * {@code AuditTimestampInterceptor}. Trio {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * adalah KEHARUSAN TEKNIS pola entity AIS (dipakai layar audit generik), berdampingan dengan
	 * riwayat Envers, bukan penggantinya.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default wajib JavaBean/Hibernate; pengisian dilakukan service pendaftaran. */
	public PendaftarTenantProfile() {
	}

	/**
	 * Primary key baris profile (identity database). Bukan identitas akun -- identitas akun tetap
	 * {@code Pendaftar.id}; kolom ini hanya kunci teknis tabel extension.
	 *
	 * @return id baris, {@code null} bila belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter primary key -- dipanggil Hibernate. Jangan diisi manual dari kode aplikasi.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Akun {@link Pendaftar} yang diperluas oleh profile ini. Relasi 1:1 ditegakkan constraint DB
	 * {@code unique = true} pada kolom join, sehingga mustahil ada dua profile untuk satu akun
	 * bahkan bila dua submit berjalan bersamaan.
	 *
	 * <p>Getter memanggil {@code check(...)} milik {@link GeneralValueObject}: proxy Hibernate yang
	 * sudah tidak dapat diinisialisasi (sesi tertutup, baris terhapus) dinetralkan menjadi
	 * {@code null} alih-alih melempar {@code LazyInitializationException}.</p>
	 *
	 * @return akun pendaftar pemilik profile, atau {@code null} bila proxy tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar_id", nullable = false, unique = true)
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	/**
	 * Tautkan profile ke akun pendaftar.
	 *
	 * @param pendaftar akun pemilik (wajib saat disimpan)
	 */
	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	/** Email login ter-normalisasi (trim+lowercase). Unique HANYA di tabel ini (akun self-service),
	 *  TIDAK memaksakan unique ke seluruh {@code pendaftar.email} legacy tanpa cleansing.
	 *
	 *  <p>Inilah kunci yang dipakai {@code adaAkunSelfService(...)} untuk menolak pendaftaran ulang
	 *  dengan email yang sudah punya akun (jawaban {@code EMAIL_ALREADY_REGISTERED} yang mengarahkan
	 *  pengguna untuk login). Normalisasi wajib dilakukan pemanggil sebelum menyimpan -- kolom ini
	 *  tidak menormalisasi sendiri, sehingga menyimpan nilai ber-huruf besar akan melumpuhkan
	 *  perlindungan duplikasi tersebut.</p> */
	@Column(name = "normalized_email", unique = true, nullable = false, length = 255)
	public String getNormalizedEmail() {
		return normalizedEmail;
	}

	/**
	 * Simpan email login ternormalisasi.
	 *
	 * @param normalizedEmail email hasil {@code normalisasiEmail} (sudah trim + huruf kecil)
	 */
	public void setNormalizedEmail(String normalizedEmail) {
		this.normalizedEmail = normalizedEmail;
	}

	/**
	 * Nama orang yang ditampilkan sebagai pemilik akun; diisi dari isian PIC pada wizard. Teks
	 * bebas dari internet -- wajib di-escape saat ditampilkan.
	 *
	 * @return nama tampilan pemilik, atau string kosong
	 */
	@Column(name = "owner_display_name", length = 255)
	public String getOwnerDisplayName() {
		return ownerDisplayName;
	}

	/**
	 * Tetapkan nama tampilan pemilik akun.
	 *
	 * @param ownerDisplayName nama PIC/pemilik
	 */
	public void setOwnerDisplayName(String ownerDisplayName) {
		this.ownerDisplayName = ownerDisplayName;
	}

	/**
	 * Nama badan usaha sesuai dokumen legal (akta/izin). Dicatat apa adanya sebagai keterangan
	 * pendaftaran; TIDAK diverifikasi terhadap sumber resmi mana pun oleh sistem.
	 *
	 * @return nama legal, atau string kosong
	 */
	@Column(name = "legal_name", length = 255)
	public String getLegalName() {
		return legalName;
	}

	/**
	 * Tetapkan nama legal badan usaha.
	 *
	 * @param legalName nama sesuai dokumen legal
	 */
	public void setLegalName(String legalName) {
		this.legalName = legalName;
	}

	/**
	 * Nama dagang/merek yang dipakai sehari-hari, boleh berbeda dari {@link #getLegalName()}.
	 *
	 * @return nama dagang, atau string kosong
	 */
	@Column(name = "trade_name", length = 255)
	public String getTradeName() {
		return tradeName;
	}

	/**
	 * Tetapkan nama dagang.
	 *
	 * @param tradeName nama dagang/merek
	 */
	public void setTradeName(String tradeName) {
		this.tradeName = tradeName;
	}

	/**
	 * Bentuk badan usaha (PT, CV, Yayasan, Koperasi, perorangan, dan seterusnya). Teks bebas dari
	 * formulir -- bukan enum tertutup, jadi jangan dipakai sebagai dasar keputusan program.
	 *
	 * @return bentuk badan usaha, atau string kosong
	 */
	@Column(name = "legal_form", length = 100)
	public String getLegalForm() {
		return legalForm;
	}

	/**
	 * Tetapkan bentuk badan usaha.
	 *
	 * @param legalForm teks bentuk badan usaha
	 */
	public void setLegalForm(String legalForm) {
		this.legalForm = legalForm;
	}

	/**
	 * Nomor Induk Berusaha yang diisi pendaftar. Disimpan apa adanya untuk keperluan administrasi;
	 * TIDAK divalidasi formatnya dan TIDAK dicek ke instansi mana pun, sehingga tidak boleh
	 * dijadikan bukti legalitas oleh proses hilir.
	 *
	 * @return NIB, atau string kosong
	 */
	@Column(name = "nib", length = 50)
	public String getNib() {
		return nib;
	}

	/**
	 * Tetapkan NIB.
	 *
	 * @param nib nomor induk berusaha
	 */
	public void setNib(String nib) {
		this.nib = nib;
	}

	/**
	 * NPWP yang diisi pendaftar. Sama seperti {@link #getNib()}: data administratif yang tidak
	 * diverifikasi. Karena tergolong data identitas, perlakukan sebagai data pribadi -- jangan
	 * tampilkan di halaman publik dan jangan ikut sertakan dalam log.
	 *
	 * @return NPWP, atau string kosong
	 */
	@Column(name = "npwp", length = 50)
	public String getNpwp() {
		return npwp;
	}

	/**
	 * Tetapkan NPWP.
	 *
	 * @param npwp nomor pokok wajib pajak
	 */
	public void setNpwp(String npwp) {
		this.npwp = npwp;
	}

	/**
	 * Alamat situs web usaha. Teks bebas dari internet: bila kelak dirender sebagai tautan, wajib
	 * dibatasi skema {@code http/https} agar tidak menjadi vektor {@code javascript:} URL.
	 *
	 * <p>Perhatikan bahwa formulir juga memiliki kolom umpan (honeypot) bernama {@code website_hp};
	 * keduanya berbeda -- yang tersimpan di sini adalah kolom {@code website} yang sah.</p>
	 *
	 * @return alamat situs, atau string kosong
	 */
	@Column(name = "website", length = 255)
	public String getWebsite() {
		return website;
	}

	/**
	 * Tetapkan alamat situs web usaha.
	 *
	 * @param website URL situs
	 */
	public void setWebsite(String website) {
		this.website = website;
	}

	/**
	 * Kode pos alamat usaha (alamat selengkapnya tetap disimpan pada entity {@link Pendaftar}
	 * legacy; hanya kode pos yang belum tersedia di sana yang ditampung di tabel extension ini).
	 *
	 * @return kode pos, atau string kosong
	 */
	@Column(name = "postal_code", length = 20)
	public String getPostalCode() {
		return postalCode;
	}

	/**
	 * Tetapkan kode pos.
	 *
	 * @param postalCode kode pos
	 */
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	/**
	 * Zona waktu pilihan pendaftar; getter mengembalikan {@code Asia/Jakarta} bila kolom
	 * kosong. Default dibaca-saja (tidak ditulis balik ke field), sehingga baris lama tetap utuh
	 * di database.
	 *
	 * <p>Ini satu-satunya field profile yang dikonsumsi provisioning:
	 * {@code TenantProvisioningService} memakainya sebagai zona waktu tenant baru. Karena nilainya
	 * berasal dari formulir publik, pemakai hilir sebaiknya tetap memvalidasi bahwa teksnya
	 * merupakan id zona waktu yang dikenal sebelum meneruskannya ke API tanggal/waktu.</p>
	 *
	 * @return id zona waktu, {@code Asia/Jakarta} bila belum diisi
	 */
	@Column(name = "timezone", length = 64)
	public String getTimezone() {
		return timezone == null || timezone.trim().isEmpty() ? "Asia/Jakarta" : timezone;
	}

	/**
	 * Tetapkan zona waktu pilihan.
	 *
	 * @param timezone id zona waktu (mis. {@code Asia/Makassar})
	 */
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	/**
	 * Locale tampilan pilihan; getter mengembalikan {@code id_ID} bila kolom kosong (default
	 * dibaca-saja, tidak ditulis balik). Belum ada pembaca di jalur pendaftaran saat ini --
	 * disiapkan untuk pelokalan antarmuka tenant.
	 *
	 * @return kode locale, {@code id_ID} bila belum diisi
	 */
	@Column(name = "preferred_locale", length = 20)
	public String getPreferredLocale() {
		return preferredLocale == null || preferredLocale.trim().isEmpty() ? "id_ID" : preferredLocale;
	}

	/**
	 * Tetapkan locale tampilan pilihan.
	 *
	 * @param preferredLocale kode locale
	 */
	public void setPreferredLocale(String preferredLocale) {
		this.preferredLocale = preferredLocale;
	}

	/**
	 * Asal pembuatan akun. Diisi service dengan nilai tetap {@code PUBLIC_FORM} karena profile
	 * hanya dibuat pada jalur pendaftaran anonim; akun yang lahir dari jalur lain tidak memiliki
	 * baris profile sama sekali. Bandingkan dengan
	 * {@link PendaftaranTenant#getRegistrationSource()} yang membedakan {@code PUBLIC_FORM} dan
	 * {@code DASHBOARD} pada tingkat permohonan.
	 *
	 * @return kode asal pendaftaran akun
	 */
	@Column(name = "registration_source", length = 40)
	public String getRegistrationSource() {
		return registrationSource;
	}

	/**
	 * Tetapkan asal pendaftaran akun.
	 *
	 * @param registrationSource kode asal
	 */
	public void setRegistrationSource(String registrationSource) {
		this.registrationSource = registrationSource;
	}

	/**
	 * Status akun self-service; getter mengembalikan {@link #STATUS_PENDING_VERIFICATION} bila
	 * kolom kosong (default dibaca-saja ke keadaan paling belum-terpercaya).
	 *
	 * <p><strong>Bukan gerbang keamanan.</strong> Tidak ada kode yang menjadikan nilai ini syarat
	 * untuk apa pun; login pendaftar memeriksa {@code Pendaftar.aktif}. Kolom ini murni catatan
	 * keadaan akun, dan satu-satunya transisi yang benar-benar terjadi adalah
	 * {@code PENDING_VERIFICATION} &rarr; {@link #STATUS_ACTIVE} saat email terverifikasi.
	 * Rincian dan implikasinya diuraikan pada Javadoc kelas.</p>
	 *
	 * @return status akun, {@link #STATUS_PENDING_VERIFICATION} bila belum diisi
	 */
	@Column(name = "account_status", length = 40)
	public String getAccountStatus() {
		return accountStatus == null || accountStatus.trim().isEmpty() ? STATUS_PENDING_VERIFICATION : accountStatus;
	}

	/**
	 * Tetapkan status akun. Setter polos tanpa validasi transisi -- pemanggil (service pendaftaran)
	 * yang bertanggung jawab atas urutan status.
	 *
	 * @param accountStatus salah satu konstanta {@code STATUS_*}
	 */
	public void setAccountStatus(String accountStatus) {
		this.accountStatus = accountStatus;
	}

	/** Format hash versioned (mis. {@code PBKDF2WithHmacSHA256}); hash+salt sendiri tetap di kolom
	 *  {@code pendaftar.password_hash/password_salt} existing (tidak dipindah -- kompatibel login lama).
	 *
	 *  <p>Diisi dari konstanta {@code PasswordHashService.ALGORITHM} saat akun dibuat, sehingga
	 *  baris lama tetap merekam algoritma yang BENAR-BENAR dipakai waktu itu meski konstanta
	 *  platform kelak diganti. Itulah gunanya: memungkinkan hash lama dikenali dan di-upgrade saat
	 *  login berikutnya.</p> */
	@Column(name = "password_algorithm", length = 64)
	public String getPasswordAlgorithm() {
		return passwordAlgorithm;
	}

	/**
	 * Tetapkan nama algoritma hash password.
	 *
	 * @param passwordAlgorithm nama algoritma saat akun dibuat
	 */
	public void setPasswordAlgorithm(String passwordAlgorithm) {
		this.passwordAlgorithm = passwordAlgorithm;
	}

	/**
	 * Nomor versi skema hashing password (pendamping {@link #getPasswordAlgorithm()}), diisi dari
	 * {@code PasswordHashService.VERSION}. Dipakai untuk membedakan generasi skema bila kelak
	 * parameter atau bentuk penyimpanan hash berubah.
	 *
	 * @return versi skema hashing, atau {@code null} untuk baris lama
	 */
	@Column(name = "password_version")
	public Integer getPasswordVersion() {
		return passwordVersion;
	}

	/**
	 * Tetapkan versi skema hashing password.
	 *
	 * @param passwordVersion nomor versi
	 */
	public void setPasswordVersion(Integer passwordVersion) {
		this.passwordVersion = passwordVersion;
	}

	/**
	 * Jumlah iterasi turunan kunci saat akun dibuat, diisi dari {@code PasswordHashService.ITERASI}
	 * (PBKDF2 120.000 iterasi pada konfigurasi sekarang). Merekam nilai per-akun memungkinkan
	 * platform menaikkan jumlah iterasi tanpa merusak verifikasi hash lama.
	 *
	 * @return jumlah iterasi, atau {@code null} untuk baris lama
	 */
	@Column(name = "password_iterations")
	public Integer getPasswordIterations() {
		return passwordIterations;
	}

	/**
	 * Tetapkan jumlah iterasi KDF.
	 *
	 * @param passwordIterations jumlah iterasi
	 */
	public void setPasswordIterations(Integer passwordIterations) {
		this.passwordIterations = passwordIterations;
	}

	/**
	 * Penanda bahwa pengguna wajib mengganti password pada login berikutnya; getter mengembalikan
	 * {@code FALSE} bila kolom kosong (default dibaca-saja, aman karena tidak membuka akses apa
	 * pun -- hanya menonaktifkan paksaan ganti password).
	 *
	 * <p><strong>FIELD TIDUR.</strong> Service pendaftaran hanya pernah menuliskan {@code FALSE},
	 * dan tidak ada jalur login yang membacanya. Bila kelak dipakai (mis. untuk kredensial admin
	 * tenant yang dikirim otomatis lewat {@link RegistrationCredentialDelivery}), pemaksaan ganti
	 * password harus ditambahkan di alur login -- menyalakan flag ini saja tidak berefek apa
	 * pun.</p>
	 *
	 * @return {@code TRUE} bila wajib ganti password, {@code FALSE} bila tidak/belum diisi
	 */
	@Column(name = "must_change_password")
	public Boolean getMustChangePassword() {
		return mustChangePassword == null ? Boolean.FALSE : mustChangePassword;
	}

	/**
	 * Tetapkan penanda wajib ganti password.
	 *
	 * @param mustChangePassword {@code TRUE} untuk memaksa penggantian
	 */
	public void setMustChangePassword(Boolean mustChangePassword) {
		this.mustChangePassword = mustChangePassword;
	}

	/**
	 * Waktu email akun terbukti terverifikasi. Diisi bersamaan dengan perpindahan
	 * {@link #getAccountStatus()} ke {@link #STATUS_ACTIVE} pada
	 * {@code PendaftaranTenantService.verifikasiEmail}/{@code verifikasiTanpaToken}.
	 *
	 * <p>Karena pengisian dilakukan lewat pencarian profile berdasarkan {@code pendaftar.id} dan
	 * dilewati bila profile tidak ditemukan, akun lama tanpa baris profile tetap dapat
	 * diverifikasi di tingkat permohonan tanpa error -- lihat
	 * {@link PendaftaranTenant#getVerifiedAt()} sebagai jejak verifikasi yang selalu ada.</p>
	 *
	 * @return waktu verifikasi email, atau {@code null} bila belum terverifikasi
	 */
	@Column(name = "email_verified_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getEmailVerifiedAt() {
		return emailVerifiedAt;
	}

	/**
	 * Tetapkan waktu verifikasi email.
	 *
	 * @param emailVerifiedAt waktu token verifikasi dikonsumsi
	 */
	public void setEmailVerifiedAt(Date emailVerifiedAt) {
		this.emailVerifiedAt = emailVerifiedAt;
	}

	/**
	 * Waktu login terakhir yang berhasil, dicatat {@code PendaftarPublicHelper.login} secara
	 * best-effort: pencatatan dibungkus {@code try/catch} sendiri sehingga kegagalannya TIDAK
	 * menggagalkan login. Artinya kolom ini boleh tertinggal/kosong dan tidak layak dipakai sebagai
	 * bukti tunggal aktivitas akun untuk keperluan audit keamanan.
	 *
	 * @return waktu login terakhir, atau {@code null} bila belum pernah login/tercatat
	 */
	@Column(name = "last_login_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getLastLoginAt() {
		return lastLoginAt;
	}

	/**
	 * Tetapkan waktu login terakhir.
	 *
	 * @param lastLoginAt waktu login berhasil
	 */
	public void setLastLoginAt(Date lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

	/**
	 * Waktu baris profile dibuat, diisi service pada transaksi submit yang sama dengan pembuatan
	 * {@link Pendaftar} dan {@link PendaftaranTenant} -- ketiganya sukses bersama atau
	 * di-rollback bersama.
	 *
	 * @return waktu pembuatan profile
	 */
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Tetapkan waktu pembuatan profile.
	 *
	 * @param createdAt waktu pembuatan
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Nomor versi optimistic locking Hibernate ({@code @Version}), naik otomatis setiap update.
	 * Menjaga agar pencatatan {@link #setLastLoginAt(Date)} dari sesi login dan pembaruan status
	 * dari proses verifikasi tidak saling menimpa diam-diam. Jangan diisi manual.
	 *
	 * @return nomor versi baris
	 */
	@Version
	@Column(name = "version")
	public Integer getVersion() {
		return version;
	}

	/**
	 * Setter versi -- eksklusif untuk Hibernate.
	 *
	 * @param version nomor versi
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}

	/**
	 * Nama pengguna yang terakhir menyentuh baris (shadow audit AIS). Diisi literal
	 * {@code "pendaftaran"} pada jalur publik karena tidak ada pengguna login di sana.
	 *
	 * @return penanda pengubah terakhir
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Isi penanda pengubah terakhir. Nilai null/kosong sengaja DIABAIKAN (pola shadow audit AIS)
	 * agar jejak yang sudah ada tidak terhapus oleh pemanggil yang lupa mengisinya.
	 *
	 * @param oleh nama pengubah; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Id pengguna yang terakhir menyentuh baris (pendamping {@link #getOleh()}).
	 *
	 * @return id pengubah terakhir
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Isi id pengubah terakhir; nilai null/kosong diabaikan, sama seperti {@link #setOleh(String)}.
	 *
	 * @param olehId id pengubah; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Stempel waktu perubahan terakhir (shadow audit AIS), disegarkan hook {@code @PreUpdate}
	 * lewat {@code AuditTimestampInterceptor}. Nilai awalnya diisi saat objek dibentuk, sehingga
	 * baris baru pun sudah punya stempel meski belum pernah di-update.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Tetapkan stempel waktu perubahan terakhir (umumnya hanya dipanggil interceptor audit).
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
