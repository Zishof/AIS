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
 * <h3>Permohonan pembuatan satu tenant/workspace (control-plane).</h3>
 *
 * <p>{@link Pendaftar} = akun/pihak yang mendaftar (1) --- (N) {@code PendaftaranTenant} =
 * permohonan workspace. Satu permohonan menghasilkan 0..1 {@link TenantRegistry}. Workflow
 * status lihat konstanta {@code STATUS_*} + {@code docs/pendaftaran-tenant/05-workflow.md};
 * transisi HANYA lewat {@code ais.service.registration.PendaftaranTenantService} (bukan
 * setter bebas dari servlet/JSP).</p>
 *
 * <p>Idempotency: {@link #getIdempotencyKey()} unique -- submit ulang dgn key sama
 * mengembalikan permohonan yang sudah ada (registrationCode sama), tidak membuat baris kedua.</p>
 *
 * <h4>Siapa yang boleh membuat baris ini (hasil audit pemakai, per 2026-09)</h4>
 *
 * <p>Baris {@code PendaftaranTenant} dibuat di SATU tempat saja:
 * {@code PendaftaranTenantService.submit(JSONObject, Long, String)}, yang dipanggil dari
 * {@code ais.action.servlet.PendaftaranTenantServlet} (dipetakan di {@code web.xml} sebagai
 * servlet {@code PendaftaranTenant}, route {@code Common.ROOT + "/pendaftaran"}). Route itu
 * <strong>PUBLIK-ANONIM</strong>: lolos catch-all Spring Security
 * {@code /** = IS_AUTHENTICATED_ANONYMOUSLY} -- artinya siapa pun di internet dapat membuat baris
 * permohonan tanpa login. Itu memang DESAIN (self-service signup), bukan kelalaian; yang menahan
 * penyalahgunaan adalah lapisan berikut, semuanya server-side dan semuanya di jalur POST
 * {@code action=submit_registration}:</p>
 * <ol>
 * <li>Token CSRF per-session ({@code PendaftaranCsrfUtil}, SecureRandom 32 byte, perbandingan
 * constant-time) wajib pada SEMUA POST -- token dibagikan hanya saat GET wizard.</li>
 * <li>Honeypot {@code website_hp} + pemeriksaan elapsed-time terhadap {@code formInstanceId}
 * yang dibagikan saat GET (submit terlalu cepat ditolak diam-diam sbg {@code REQUEST_REJECTED}).</li>
 * <li>{@code PublicRegistrationRateLimiter} sliding window in-JVM: submit 10/jam/IP,
 * 5/jam/email, 5/jam/username (catatan risiko diterima: per-node, belum cluster-wide).</li>
 * <li>Validasi bisnis penuh di service (username, email, password, jenis usaha aktif, consent
 * versi terpublikasi) dalam SATU transaction -- gagal apa pun = rollback total, tidak ada
 * permohonan/reservation yatim.</li>
 * </ol>
 *
 * <p>Membuat baris permohonan <strong>TIDAK sama dengan</strong> mendapat tenant. Baris baru
 * SELALU lahir pada {@link #STATUS_EMAIL_VERIFICATION_PENDING} (di-hardcode di
 * {@code submit()}, bukan diambil dari request), dan satu-satunya jalur publik yang memajukannya
 * adalah konsumsi token verifikasi email sekali-pakai ({@code verifikasiEmail(String)}; DB hanya
 * menyimpan SHA-256 hex dari token). Jalur non-publik satu-satunya adalah backoffice
 * ({@code verifikasiTanpaToken(Long)} / {@code admin_approve} / {@code admin_reject} /
 * {@code admin_retry}) yang digerbangi {@code PendaftaranTenantServlet.adminBerwenang(Tbmuser)}
 * -- root atau role {@code Tbmrole.ADMINISTRATOR}, fail-closed (exception = tolak).</p>
 *
 * <h4>Gerbang tahapan = server-side, bukan UI-only</h4>
 *
 * <p>Penting untuk dicatat karena pola "bypass persetujuan UI-only" berulang di modul lain:
 * di sini TIDAK ada bypass semacam itu. Field {@link #getStatus()} tidak pernah diisi dari
 * parameter request mana pun -- tidak ada dalam whitelist field yang disalin servlet ke payload
 * JSON service. Setiap transisi diperiksa ulang terhadap status yang sedang TERSIMPAN:
 * {@code verifikasiEmail} hanya memajukan bila status masih
 * {@link #STATUS_EMAIL_VERIFICATION_PENDING}/{@link #STATUS_SUBMITTED} (klik tautan dua kali =
 * no-op), {@code resendVerifikasi} menolak di luar dua status itu, {@code cancel} hanya menerima
 * {@link #STATUS_DRAFT}/{@link #STATUS_SUBMITTED}/{@link #STATUS_EMAIL_VERIFICATION_PENDING},
 * dan provisioning tidak pernah dimulai sebelum ada {@link SchemaNameReservation} berstatus
 * RESERVED milik permohonan itu. Kolom {@link #getVersion()} ({@code @Version}) menambah
 * optimistic locking sehingga dua transisi paralel atas baris yang sama tidak saling menimpa
 * diam-diam.</p>
 *
 * <h4>Entitlement modul TIDAK ditentukan pendaftar</h4>
 *
 * <p>Pendaftar hanya mengirim {@code jenisUsahaIds} (divalidasi harus ada + {@code aktif=true}).
 * Daftar modul yang akhirnya aktif dibentuk server dari join DB
 * {@link PendaftaranTenantJenisUsaha} &times; {@link JenisUsahaTenantModule} ({@code defaultEnabled=true})
 * di {@code TenantEntitlementService.terapkanDariJenisUsaha}. Tidak ada parameter request bernama
 * "module"/"entitlement" di mana pun jalur ini. Lihat juga catatan pada
 * {@link #getSelectedPlanVersion()} soal {@code planCode} yang TIDAK divalidasi terhadap katalog
 * paket (saat ini murni label, tidak memberi hak modul apa pun).</p>
 *
 * <h4>Field jejak & privasi</h4>
 *
 * <p>{@link #getSourceIpHash()}/{@link #getUserAgent()} diisi servlet, BUKAN dari field form:
 * servlet menyalin whitelist parameter klien lebih dulu lalu menimpanya dengan
 * {@code p.put("sourceIp", ip)} dan header {@code User-Agent} sebenarnya, sehingga klien tidak
 * dapat memalsukan jejak. IP disimpan sebagai hash di baris ini; IP mentah hanya ada di
 * {@code pendaftaran_consent} sebagai bukti persetujuan.</p>
 *
 * <p>Entity ini {@code @Audited} (Hibernate Envers) sehingga seluruh riwayat transisi status
 * tersimpan di schema {@code new_audit}. Konsekuensi teknisnya: menambah kolom baru ke entity ini
 * TIDAK otomatis menambah kolom ke tabel audit-nya bila {@code hbm2ddl=update} (peringatan
 * {@code src/hibernate.cfg.xml}), jadi perubahan struktur wajib disertai migrasi tabel audit.</p>
 *
 * @see TenantRegistry
 * @see PendaftaranTenantJenisUsaha
 * @see PendaftarTenantProfile
 * @see SchemaNameReservation
 * @see ProvisioningJob
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftaran_tenant")
public class PendaftaranTenant extends GeneralValueObject {

	/** Versi serialisasi Java standar entity AIS. */
	private static final long serialVersionUID = 1L;

	// -- Workflow status utama (08. WORKFLOW STATUS dokumen master) --
	/**
	 * Permohonan tersimpan tapi belum disubmit. Juga nilai efektif bila kolom status kosong
	 * (lihat {@link #getStatus()}). Jalur wizard publik saat ini TIDAK pernah menulis DRAFT --
	 * submit langsung melompat ke {@link #STATUS_EMAIL_VERIFICATION_PENDING}; DRAFT dipertahankan
	 * untuk fitur "simpan dulu, lanjutkan nanti" dan tetap diterima oleh {@code cancel}.
	 */
	public static final String STATUS_DRAFT = "DRAFT";
	/**
	 * Formulir sudah dikirim namun tantangan verifikasi email belum terbentuk/terkirim (mis.
	 * pengiriman email platform sedang dinonaktifkan). Diperlakukan setara
	 * {@link #STATUS_EMAIL_VERIFICATION_PENDING} oleh {@code verifikasiEmail}, {@code resendVerifikasi},
	 * dan {@code cancel} sehingga permohonan tidak tersangkut bila email gagal terkirim.
	 */
	public static final String STATUS_SUBMITTED = "SUBMITTED";
	/**
	 * Status LAHIR setiap permohonan dari wizard publik: menunggu pendaftar mengklik tautan
	 * verifikasi email. Ini gerbang anti-abuse terpenting -- tanpa membuktikan penguasaan atas
	 * alamat email, permohonan tidak akan pernah masuk antrean provisioning.
	 */
	public static final String STATUS_EMAIL_VERIFICATION_PENDING = "EMAIL_VERIFICATION_PENDING";
	/**
	 * Email terbukti dikuasai pendaftar dan permohonan dinyatakan layak lanjut. Pada implementasi
	 * sekarang status ini praktis transien: {@code verifikasiEmail} langsung menulis
	 * {@link #STATUS_REVIEW_PENDING} atau {@link #STATUS_PROVISIONING_QUEUED}; VERIFIED tetap
	 * dikenali oleh pemetaan status publik sebagai "sedang diproses".
	 */
	public static final String STATUS_VERIFIED = "VERIFIED";
	/**
	 * Menunggu keputusan manual admin platform. Dipilih bila konfigurasi
	 * {@code pendaftaran_wajib_review_manual} aktif ATAU salah satu jenis usaha terpilih
	 * ber-{@link JenisUsahaTenant#getRequiresManualReview()} {@code true}. Keluar dari status ini
	 * hanya lewat {@code admin_approve}/{@code admin_reject} yang digerbangi admin platform.
	 */
	public static final String STATUS_REVIEW_PENDING = "REVIEW_PENDING";
	/**
	 * Sudah dibuatkan {@link ProvisioningJob} berstatus QUEUED; worker latar
	 * ({@code TenantProvisioningWorker}) akan mengambilnya. Retry provisioning yang gagal juga
	 * mengembalikan permohonan ke status ini.
	 */
	public static final String STATUS_PROVISIONING_QUEUED = "PROVISIONING_QUEUED";
	/** Job provisioning sedang berjalan (schema/kredensial/entitlement sedang dibentuk). */
	public static final String STATUS_PROVISIONING = "PROVISIONING";
	/**
	 * Seluruh step provisioning non-SKIPPED sukses; tenant siap dipakai dan jendela trial mulai
	 * dihitung ({@code trial_start_at = ready_at}, {@code trial_end_at = ready_at +
	 * } {@link #getTrialDaysSnapshot()}). Pada titik inilah {@code Pendaftar.aktif} dinaikkan
	 * menjadi {@code true} untuk tenant PERTAMA milik akun tersebut.
	 */
	public static final String STATUS_READY = "READY";
	/** Owner sudah login pertama kali setelah READY -- tenant benar-benar dipakai. */
	public static final String STATUS_ACTIVE = "ACTIVE";
	// -- Exception path --
	/**
	 * Ditolak admin pada tahap review manual. Alasan internal disimpan di audit event, sedangkan
	 * yang boleh dilihat publik hanya {@link #getFailureMessageSafe()}.
	 */
	public static final String STATUS_REJECTED = "REJECTED";
	/**
	 * Job provisioning gagal. Data pendaftar TIDAK dihapus; admin dapat memicu retry yang
	 * melewati step yang sudah SUCCESS (idempoten per step) dan mengembalikan permohonan ke
	 * {@link #STATUS_PROVISIONING_QUEUED}.
	 */
	public static final String STATUS_PROVISIONING_FAILED = "PROVISIONING_FAILED";
	/**
	 * Tenant aktif yang dibekukan (mis. tunggakan/pelanggaran). Suspend TIDAK menghapus data yang
	 * sudah diposting -- pembekuan ditegakkan pada level {@link TenantRegistry} oleh
	 * {@code TenantContextResolver}.
	 */
	public static final String STATUS_SUSPENDED = "SUSPENDED";
	/**
	 * Dibatalkan pendaftar sendiri lewat {@code cancel_draft}. Kepemilikan dibuktikan dengan
	 * daftar {@code registrationCode} pada HttpSession browser pengirim (bukan sekadar menebak
	 * kode), dan reservation username yang belum CONSUMED ikut dilepas.
	 */
	public static final String STATUS_CANCELLED = "CANCELLED";
	/** Tantangan verifikasi email kedaluwarsa tanpa pernah diklik -- permohonan basi. */
	public static final String STATUS_EXPIRED = "EXPIRED";

	/** Primary key surrogate (identity, di-generate database). */
	private Long id;
	/** Nomor publik permohonan yang dipakai pendaftar untuk mengecek status. */
	private String registrationCode;
	/** Akun pendaftar pemilik permohonan (wajib). */
	private Pendaftar pendaftar;
	/** Tenant hasil provisioning; null selama permohonan belum sampai READY. */
	private TenantRegistry tenantRegistry;
	/** Username/subdomain yang diminta pendaftar apa adanya. */
	private String desiredUsername;
	/** Bentuk ternormalisasi dari {@link #desiredUsername}, kunci benturan global. */
	private String normalizedUsername;
	/** Domain kustom yang diminta (belum dipakai jalur mana pun -- lihat getter). */
	private String requestedDomain;
	/** Status workflow utama, salah satu konstanta {@code STATUS_*}. */
	private String status;
	/** Label tahap terbaca-manusia yang mendampingi {@link #status}. */
	private String currentStage;
	/** Snapshot kode paket yang dipilih saat submit. */
	private String selectedPlanVersion;
	/** Snapshot lama trial (hari) saat submit. */
	private Integer trialDaysSnapshot;
	/** Waktu formulir dikirim. */
	private Date submittedAt;
	/** Waktu email terverifikasi. */
	private Date verifiedAt;
	/** Waktu admin menyetujui pada jalur review manual. */
	private Date approvedAt;
	/** Waktu seluruh step provisioning selesai sukses. */
	private Date readyAt;
	/** Waktu penolakan admin. */
	private Date rejectedAt;
	/** Kode mesin penyebab kegagalan/penolakan. */
	private String failureCode;
	/** Pesan kegagalan yang aman ditampilkan ke publik. */
	private String failureMessageSafe;
	/** Kunci idempotensi submit (unique). */
	private String idempotencyKey;
	/** Korelasi satu request lintas log/audit. */
	private String requestId;
	/** Hash SHA-256 IP sumber submit. */
	private String sourceIpHash;
	/** User agent browser pengirim. */
	private String userAgent;
	/** Asal permohonan: {@code PUBLIC_FORM} atau {@code DASHBOARD}. */
	private String registrationSource;
	/** Waktu baris dibuat. */
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
	 * adalah KEHARUSAN TEKNIS pola entity AIS (dipakai layar audit generik), bukan duplikasi
	 * kolom Envers yang bisa dihapus begitu saja.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default wajib JavaBean/Hibernate; seluruh pengisian dilakukan service. */
	public PendaftaranTenant() {
	}

	/**
	 * Primary key baris permohonan (identity database). Dipakai sebagai bahan
	 * {@link #getRegistrationCode()} dan sebagai kunci join oleh {@link PendaftaranTenantJenisUsaha},
	 * {@link PendaftaranEmailVerification}, {@link PendaftaranConsent}, {@link ProvisioningJob},
	 * dan {@link PendaftaranAuditEvent}.
	 *
	 * @return id baris, {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter primary key -- dipanggil Hibernate saat memuat/menyimpan baris. Jangan diisi manual
	 * dari kode aplikasi.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** Nomor publik pendaftaran, format {@code REG-<tahun>-<id-padded>} -- aman ditampilkan.
	 *  Dibangkitkan service SETELAH {@code flush()} pertama (butuh {@link #getId()}), lalu dipakai
	 *  pendaftar untuk halaman status, kirim ulang verifikasi, dan pembatalan draft. Unique, dan
	 *  sengaja tidak menyandikan data pribadi apa pun. */
	@Column(name = "registration_code", unique = true, length = 40)
	public String getRegistrationCode() {
		return registrationCode;
	}

	/**
	 * Isi nomor publik pendaftaran. Hanya {@code PendaftaranTenantService} yang berhak
	 * memanggilnya; kode dibentuk dari id + tahun submit, bukan dari input pendaftar.
	 *
	 * @param registrationCode kode permohonan
	 */
	public void setRegistrationCode(String registrationCode) {
		this.registrationCode = registrationCode;
	}

	/**
	 * Akun {@link Pendaftar} pemilik permohonan (wajib, {@code nullable=false}). Untuk pendaftar
	 * anonim, akun dibuat di transaksi submit yang sama dengan {@code aktif=false} eksplisit; untuk
	 * pendaftar yang sudah login, akun existing dipakai ulang sehingga TIDAK ada duplikasi akun.
	 *
	 * <p>Getter memanggil {@code check(...)} milik {@link GeneralValueObject} -- pola standar AIS
	 * untuk menetralkan proxy Hibernate yang sudah tidak dapat di-inisialisasi (mis. sesi sudah
	 * ditutup) menjadi {@code null} alih-alih melempar {@code LazyInitializationException}.</p>
	 *
	 * @return pendaftar pemilik, atau {@code null} bila proxy tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar_id", nullable = false)
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	/**
	 * Tetapkan akun pemilik permohonan.
	 *
	 * @param pendaftar akun pendaftar (tidak boleh null saat disimpan)
	 */
	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	/**
	 * Tenant hasil permohonan ini. {@code null} sepanjang permohonan belum diprovisikan --
	 * itulah sebabnya kolom {@code nullable=true}. Terisi oleh {@code TenantProvisioningService}
	 * ketika baris {@link TenantRegistry} dibuat, sehingga pasangan
	 * permohonan &harr; tenant dapat ditelusuri dua arah.
	 *
	 * <p>Getter memakai {@code check(...)} (lihat {@link #getPendaftar()}) sehingga proxy yatim
	 * menjadi {@code null}; artinya {@code null} berarti "belum diprovisikan ATAU proxy tidak
	 * dapat dimuat" -- jangan jadikan satu-satunya bukti bahwa provisioning belum jalan, periksa
	 * {@link #getStatus()} juga.</p>
	 *
	 * @return tenant hasil provisioning, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tenant_registry_id", nullable = true)
	public TenantRegistry getTenantRegistry() {
		tenantRegistry = check(tenantRegistry);
		return tenantRegistry;
	}

	/**
	 * Tautkan permohonan ke tenant hasil provisioning.
	 *
	 * @param tenantRegistry tenant yang terbentuk
	 */
	public void setTenantRegistry(TenantRegistry tenantRegistry) {
		this.tenantRegistry = tenantRegistry;
	}

	/**
	 * Username/subdomain persis seperti diketik pendaftar (belum dinormalisasi) -- disimpan untuk
	 * ditampilkan kembali dan untuk audit "apa yang sebenarnya diminta". Yang menjadi kunci
	 * keunikan adalah {@link #getNormalizedUsername()}, bukan field ini.
	 *
	 * @return username permintaan apa adanya
	 */
	@Column(name = "desired_username", length = 64)
	public String getDesiredUsername() {
		return desiredUsername;
	}

	/**
	 * Simpan username permintaan mentah.
	 *
	 * @param desiredUsername teks dari formulir
	 */
	public void setDesiredUsername(String desiredUsername) {
		this.desiredUsername = desiredUsername;
	}

	/** Hasil normalisasi {@code ^[a-z][a-z0-9_]{2,30}$} -- kunci benturan global (reservation).
	 *  Nilai ini yang dicek {@code UsernameReservationService} dan yang diserialisasi lewat baris
	 *  {@link SchemaNameReservation}; pemeriksaan ketersediaan saat submit BUKAN jaminan akhir --
	 *  jaminan akhirnya adalah constraint unik pada tabel reservation (mencegah race dua submit
	 *  bersamaan atas username yang sama). */
	@Column(name = "normalized_username", length = 64)
	public String getNormalizedUsername() {
		return normalizedUsername;
	}

	/**
	 * Simpan username ternormalisasi.
	 *
	 * @param normalizedUsername hasil normalisasi service (huruf kecil, karakter dibatasi)
	 */
	public void setNormalizedUsername(String normalizedUsername) {
		this.normalizedUsername = normalizedUsername;
	}

	/**
	 * Domain kustom yang diminta pendaftar (parameter form {@code customDomain}).
	 *
	 * <p><strong>FIELD TIDUR.</strong> Per audit pemakai, nilai ini hanya DITULIS saat submit dan
	 * tidak pernah dibaca oleh kode mana pun: pembentukan {@link TenantDomain} pada provisioning
	 * memakai {@link #getNormalizedUsername()} + subdomain base platform. Konsekuensinya, isi
	 * kolom ini adalah teks bebas dari internet yang hanya dirapikan panjangnya (maksimal 255),
	 * belum divalidasi sebagai nama domain dan belum diverifikasi kepemilikannya. Bila kelak
	 * dipasang ke {@link TenantDomain}, WAJIB divalidasi ulang (format + verifikasi kepemilikan
	 * domain), jangan dipercaya karena "sudah tersimpan di DB".</p>
	 *
	 * @return domain yang diminta, atau string kosong bila tidak diisi
	 */
	@Column(name = "requested_domain", length = 255)
	public String getRequestedDomain() {
		return requestedDomain;
	}

	/**
	 * Simpan domain kustom yang diminta.
	 *
	 * @param requestedDomain teks domain dari formulir
	 */
	public void setRequestedDomain(String requestedDomain) {
		this.requestedDomain = requestedDomain;
	}

	/**
	 * Status workflow permohonan, salah satu konstanta {@code STATUS_*}.
	 *
	 * <p>Getter mengembalikan {@link #STATUS_DRAFT} bila kolom {@code null}/kosong. Ini default
	 * BACA yang tidak menulis balik ke field (bukan getter destruktif); karena Hibernate membaca
	 * properti lewat getter, nilai default itu ikut tersimpan saat insert sehingga kolom
	 * {@code nullable=false} tidak pernah dilanggar meski pemanggil lupa mengisi status. Perilaku
	 * ini juga berarti status "kosong" tidak akan pernah terlihat sebagai tahap lanjut -- fail-safe
	 * ke tahap paling awal, bukan ke tahap paling permisif.</p>
	 *
	 * <p>Nilai TIDAK pernah berasal dari request: servlet hanya menyalin whitelist field formulir
	 * (nama usaha, alamat, jenis usaha, kredensial, consent, dst.) dan status tidak termasuk di
	 * dalamnya. Semua perpindahan status diputuskan service setelah memeriksa status tersimpan.</p>
	 *
	 * @return status saat ini, {@link #STATUS_DRAFT} bila kolom belum diisi
	 */
	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_DRAFT : status;
	}

	/**
	 * Tetapkan status workflow. Hanya boleh dipanggil dari
	 * {@code ais.service.registration.PendaftaranTenantService} /
	 * {@code PendaftaranTenantAdminService} / {@code ais.service.tenant.TenantProvisioningService}
	 * yang memeriksa keabsahan transisi lebih dulu -- setter ini sendiri sengaja polos dan TIDAK
	 * memvalidasi apa pun, jadi memanggilnya dari layer UI/servlet akan melewati seluruh aturan
	 * workflow.
	 *
	 * @param status salah satu konstanta {@code STATUS_*}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Label tahap terbaca manusia yang mendampingi {@link #getStatus()} (mis. {@code VERIFY_EMAIL},
	 * {@code MANUAL_REVIEW}, {@code PROVISIONING}). Bersifat informatif untuk tampilan status dan
	 * backoffice; keputusan program SELALU memakai {@code status}, bukan field ini.
	 *
	 * @return kode tahap, atau {@code null}
	 */
	@Column(name = "current_stage", length = 64)
	public String getCurrentStage() {
		return currentStage;
	}

	/**
	 * Tetapkan label tahap berjalan (selalu diubah berbarengan dengan {@link #setStatus(String)}).
	 *
	 * @param currentStage kode tahap
	 */
	public void setCurrentStage(String currentStage) {
		this.currentStage = currentStage;
	}

	/** Snapshot versi paket terpilih saat submit (teks kode; harga TIDAK di-hard-code di sini).
	 *
	 *  <p><strong>Catatan keamanan:</strong> nilai berasal langsung dari parameter form
	 *  {@code planCode} (default {@code TRIAL}) dan hanya dirapikan panjangnya -- TIDAK
	 *  dicocokkan dengan katalog paket {@code pendaftaran_paket_json}. Pendaftar karena itu dapat
	 *  mengirim kode paket sembarang. Dampaknya saat ini NIHIL secara hak akses: paket tidak
	 *  memberi modul apa pun (entitlement dibentuk dari jenis usaha lewat
	 *  {@link JenisUsahaTenantModule}) dan lama trial diambil dari konfigurasi platform, bukan dari
	 *  paket. Nilai ini hanya ikut tersalin sebagai label ke
	 *  {@code TenantModuleEntitlement.planVersion} dan ke ringkasan step provisioning. Begitu kelak
	 *  ada penagihan/pembatasan yang MEMBACA kolom ini, validasi terhadap katalog paket menjadi
	 *  wajib -- jangan asumsikan nilainya tepercaya.</p> */
	@Column(name = "selected_plan_version", length = 64)
	public String getSelectedPlanVersion() {
		return selectedPlanVersion;
	}

	/**
	 * Simpan kode paket terpilih.
	 *
	 * @param selectedPlanVersion kode paket dari formulir
	 */
	public void setSelectedPlanVersion(String selectedPlanVersion) {
		this.selectedPlanVersion = selectedPlanVersion;
	}

	/** Snapshot lama trial (hari) saat submit -- perubahan konfigurasi platform kemudian tidak mengubah ini.
	 *  Diisi service dari konfigurasi {@code trialHari()} (default 30), BUKAN dari input pendaftar,
	 *  lalu dipakai saat READY untuk menghitung {@code trial_end_at = ready_at + n hari}. */
	@Column(name = "trial_days_snapshot")
	public Integer getTrialDaysSnapshot() {
		return trialDaysSnapshot;
	}

	/**
	 * Simpan snapshot lama trial.
	 *
	 * @param trialDaysSnapshot jumlah hari trial saat submit
	 */
	public void setTrialDaysSnapshot(Integer trialDaysSnapshot) {
		this.trialDaysSnapshot = trialDaysSnapshot;
	}

	/**
	 * Waktu formulir dikirim (diisi bersamaan dengan {@link #getCreatedAt()} pada transaksi submit).
	 * Dipakai halaman status dan backoffice untuk menghitung usia permohonan.
	 *
	 * @return waktu submit, atau {@code null} untuk draft yang belum dikirim
	 */
	@Column(name = "submitted_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSubmittedAt() {
		return submittedAt;
	}

	/**
	 * Tetapkan waktu submit.
	 *
	 * @param submittedAt waktu pengiriman formulir
	 */
	public void setSubmittedAt(Date submittedAt) {
		this.submittedAt = submittedAt;
	}

	/**
	 * Waktu email pendaftar terbukti terverifikasi -- diisi tepat saat token verifikasi dikonsumsi
	 * (atau saat admin memverifikasi manual dari backoffice). Terisinya kolom ini adalah bukti
	 * bahwa gerbang verifikasi sudah dilewati, sehingga berguna untuk audit "apakah tenant ini
	 * pernah melalui verifikasi email".
	 *
	 * @return waktu verifikasi, atau {@code null} bila belum terverifikasi
	 */
	@Column(name = "verified_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getVerifiedAt() {
		return verifiedAt;
	}

	/**
	 * Tetapkan waktu verifikasi email.
	 *
	 * @param verifiedAt waktu token dikonsumsi
	 */
	public void setVerifiedAt(Date verifiedAt) {
		this.verifiedAt = verifiedAt;
	}

	/**
	 * Waktu admin platform menyetujui permohonan pada jalur {@link #STATUS_REVIEW_PENDING}.
	 * Tetap {@code null} untuk permohonan yang lolos otomatis tanpa review manual -- jadi
	 * kolom ini bukan penanda "boleh diprovisikan", melainkan jejak keputusan manusia.
	 *
	 * @return waktu persetujuan admin, atau {@code null}
	 */
	@Column(name = "approved_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getApprovedAt() {
		return approvedAt;
	}

	/**
	 * Tetapkan waktu persetujuan admin.
	 *
	 * @param approvedAt waktu approve
	 */
	public void setApprovedAt(Date approvedAt) {
		this.approvedAt = approvedAt;
	}

	/**
	 * Waktu seluruh step provisioning selesai sukses. Menjadi titik awal jendela trial
	 * ({@code trial_start_at}) dan acuan {@code trial_end_at} bersama
	 * {@link #getTrialDaysSnapshot()}.
	 *
	 * @return waktu tenant siap pakai, atau {@code null}
	 */
	@Column(name = "ready_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getReadyAt() {
		return readyAt;
	}

	/**
	 * Tetapkan waktu tenant siap pakai.
	 *
	 * @param readyAt waktu READY
	 */
	public void setReadyAt(Date readyAt) {
		this.readyAt = readyAt;
	}

	/**
	 * Waktu permohonan ditolak admin. Berpasangan dengan {@link #getFailureCode()} dan
	 * {@link #getFailureMessageSafe()}; alasan lengkap/internal tidak disimpan di sini melainkan
	 * pada {@link PendaftaranAuditEvent} agar tidak bocor ke halaman status publik.
	 *
	 * @return waktu penolakan, atau {@code null}
	 */
	@Column(name = "rejected_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getRejectedAt() {
		return rejectedAt;
	}

	/**
	 * Tetapkan waktu penolakan.
	 *
	 * @param rejectedAt waktu reject
	 */
	public void setRejectedAt(Date rejectedAt) {
		this.rejectedAt = rejectedAt;
	}

	/**
	 * Kode mesin penyebab kegagalan/penolakan (mis. kode step provisioning yang gagal). Ditujukan
	 * untuk operator/log; pasangan yang boleh dilihat pendaftar adalah
	 * {@link #getFailureMessageSafe()}.
	 *
	 * @return kode kegagalan, atau {@code null}
	 */
	@Column(name = "failure_code", length = 64)
	public String getFailureCode() {
		return failureCode;
	}

	/**
	 * Tetapkan kode kegagalan.
	 *
	 * @param failureCode kode mesin
	 */
	public void setFailureCode(String failureCode) {
		this.failureCode = failureCode;
	}

	/** Pesan gagal yang AMAN ditampilkan publik (tanpa stack trace/nama schema/detail internal).
	 *  Pemisahan sengaja: detail teknis (exception, identifier schema, query) hanya masuk audit
	 *  event/error log, sedangkan kolom inilah satu-satunya teks kegagalan yang dikembalikan
	 *  endpoint status publik. Jangan pernah menyalin pesan exception mentah ke sini. */
	@Column(name = "failure_message_safe", length = 500)
	public String getFailureMessageSafe() {
		return failureMessageSafe;
	}

	/**
	 * Tetapkan pesan kegagalan versi publik.
	 *
	 * @param failureMessageSafe kalimat yang sudah disaring dari detail internal
	 */
	public void setFailureMessageSafe(String failureMessageSafe) {
		this.failureMessageSafe = failureMessageSafe;
	}

	/**
	 * Kunci idempotensi submit (unique di seluruh tabel). Nilai dibangkitkan server saat GET
	 * wizard dan dikirim balik oleh formulir, sehingga klik ganda/retry jaringan atas submit yang
	 * sama akan mengembalikan permohonan yang SUDAH ada beserta {@code registrationCode} lamanya,
	 * tanpa membuat baris kedua dan tanpa membakar kuota kirim ulang verifikasi.
	 *
	 * <p>Keunikan ditegakkan constraint DB, bukan sekadar pengecekan di aplikasi -- itulah yang
	 * membuatnya tetap aman terhadap dua submit paralel.</p>
	 *
	 * @return kunci idempotensi, atau {@code null} untuk baris lama
	 */
	@Column(name = "idempotency_key", unique = true, length = 100)
	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	/**
	 * Tetapkan kunci idempotensi.
	 *
	 * @param idempotencyKey kunci dari formulir (dibangkitkan server saat GET)
	 */
	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}

	/**
	 * Id korelasi satu request agar baris permohonan, audit event, dan entri error log dapat
	 * disatukan saat menelusuri insiden. Bila formulir tidak mengirimnya, servlet mengisi nilai
	 * acak sendiri sehingga korelasi tetap ada.
	 *
	 * @return id request, atau {@code null}
	 */
	@Column(name = "request_id", length = 64)
	public String getRequestId() {
		return requestId;
	}

	/**
	 * Tetapkan id korelasi request.
	 *
	 * @param requestId id korelasi
	 */
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	/** Hash SHA-256 hex dari IP sumber (privasi: IP mentah tidak disimpan di baris permohonan).
	 *  Nilai IP-nya diambil servlet dari koneksi/header proxy dan DITIMPA ke payload setelah
	 *  whitelist parameter klien disalin, sehingga pendaftar tidak dapat memalsukan jejak ini
	 *  lewat field form. Dipakai untuk korelasi anti-abuse (banyak permohonan dari satu sumber)
	 *  tanpa menyimpan data pribadi berupa IP mentah. */
	@Column(name = "source_ip_hash", length = 64)
	public String getSourceIpHash() {
		return sourceIpHash;
	}

	/**
	 * Tetapkan hash IP sumber.
	 *
	 * @param sourceIpHash hash SHA-256 hex, bukan IP mentah
	 */
	public void setSourceIpHash(String sourceIpHash) {
		this.sourceIpHash = sourceIpHash;
	}

	/**
	 * Header {@code User-Agent} browser pengirim (dipotong 500 karakter), diisi servlet dari header
	 * sebenarnya. Berguna untuk analisis penyalahgunaan; karena isinya teks bebas dari klien,
	 * WAJIB di-escape saat ditampilkan di backoffice.
	 *
	 * @return user agent, atau string kosong
	 */
	@Column(name = "user_agent", length = 500)
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * Tetapkan user agent pengirim.
	 *
	 * @param userAgent header User-Agent
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * Asal permohonan: {@code PUBLIC_FORM} bila datang dari wizard anonim, {@code DASHBOARD} bila
	 * dibuat pendaftar yang sudah login (tenant tambahan). Ditentukan server dari ada/tidaknya
	 * principal sesi -- bukan dari parameter form -- sehingga dapat dipercaya sebagai penanda
	 * jalur mana yang dipakai.
	 *
	 * @return kode asal permohonan
	 */
	@Column(name = "registration_source", length = 40)
	public String getRegistrationSource() {
		return registrationSource;
	}

	/**
	 * Tetapkan asal permohonan.
	 *
	 * @param registrationSource {@code PUBLIC_FORM} atau {@code DASHBOARD}
	 */
	public void setRegistrationSource(String registrationSource) {
		this.registrationSource = registrationSource;
	}

	/**
	 * Waktu baris dibuat, diisi service dengan stempel yang sama seperti
	 * {@link #getSubmittedAt()} pada jalur submit publik. Dipertahankan terpisah agar draft yang
	 * kelak dibuat tanpa submit tetap punya waktu pembuatan.
	 *
	 * @return waktu pembuatan baris
	 */
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Tetapkan waktu pembuatan baris.
	 *
	 * @param createdAt waktu pembuatan
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Nomor versi optimistic locking Hibernate ({@code @Version}). Dinaikkan otomatis setiap
	 * update; bila dua proses (mis. klik tautan verifikasi bersamaan dengan aksi admin) mencoba
	 * memindahkan status baris yang sama, salah satunya gagal dengan
	 * {@code StaleObjectStateException} alih-alih menimpa diam-diam. Jangan pernah diisi manual.
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
	 * Nama pengguna yang terakhir menyentuh baris (shadow audit AIS). Pada jalur pendaftaran
	 * publik tidak ada pengguna login, sehingga service mengisi nilai literal
	 * {@code "pendaftaran"}; aksi backoffice mengisinya dengan identitas admin.
	 *
	 * @return penanda pengubah terakhir
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Isi penanda pengubah terakhir. Setter sengaja MENGABAIKAN nilai null/kosong (pola shadow
	 * audit AIS) supaya jejak yang sudah ada tidak terhapus oleh pemanggil yang lupa mengisinya --
	 * bukan bug, melainkan sifat yang harus diketahui: memanggil dengan string kosong TIDAK
	 * membersihkan kolom.
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
	 * Isi id pengubah terakhir; sama seperti {@link #setOleh(String)}, nilai null/kosong diabaikan.
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
	 * Stempel waktu perubahan terakhir (shadow audit AIS), disegarkan otomatis oleh hook
	 * {@code @PreUpdate} lewat {@code AuditTimestampInterceptor}. Berbeda dari riwayat Envers yang
	 * menyimpan seluruh revisi, kolom ini hanya menahan satu nilai terakhir dan dipakai layar
	 * audit generik AIS.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Tetapkan stempel waktu perubahan terakhir (biasanya hanya dipanggil interceptor audit).
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
