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
 * <h3>Registry tenant yang sudah diprovision -- scope data &amp; identitas workspace.</h3>
 *
 * <p>{@link #getSlug()} = username tenant publik (immutable setelah ACTIVE);
 * {@link #getSchemaName()}/{@link #getAuditSchemaName()} HANYA ditulis
 * {@code TenantProvisioningService} dari reservation -- TIDAK PERNAH dari request
 * (invariant #3 ERD). Pada mode {@code LEGACY} kolom schema dibiarkan null (data tenant
 * tetap di schema existing, scope per-Pendaftar seperti sekarang).</p>
 *
 * <p>Trial: {@code trialStartAt = readyAt}, {@code trialEndAt = readyAt + trialDaysSnapshot}
 * (mulai saat READY, BUKAN saat form dibuka -- invariant #7 ERD).</p>
 *
 * <h4>Kedudukan kelas ini dalam paket pendaftaran mandiri</h4>
 * <p>
 * Paket {@code ais.database.model.tenant} adalah lapisan modern &quot;bolt-on&quot; untuk
 * pendaftaran mandiri (self-service signup) yang memprovisikan tenant baru lengkap. Alurnya:
 * calon pelanggan mengisi formulir publik &rarr; {@code PendaftaranTenant} (permohonan) dibuat
 * beserta {@link SchemaNameReservation} (kunci username) &rarr; email diverifikasi &rarr;
 * {@link ProvisioningJob} diantrekan &rarr; worker menjalankan urutan {@link ProvisioningStep}
 * &rarr; step {@code CREATE_TENANT_REGISTRY} membuat <b>baris kelas ini</b> &rarr; step-step
 * berikutnya membuat schema, memigrasikan, menyemai modul/peran/keanggotaan &rarr; step terakhir
 * {@code MARK_READY} menaikkan {@link #getStatus()} menjadi {@link #STATUS_READY}.
 * </p>
 * <p>
 * Dengan kata lain: satu baris {@code TenantRegistry} adalah <b>identitas satu ruang kerja
 * pelanggan</b>. Hampir semua entitas tenant lain menunjuk ke sini
 * ({@link TenantDomain#getTenant()}, {@link TenantModuleEntitlement#getTenant()},
 * {@code TenantMembership.tenant}, {@link ProvisioningJob#getTenant()}), dan
 * {@code ais.service.tenant.TenantContextResolver} memakai baris ini sebagai titik awal
 * pembentukan {@code TenantContext} setiap request.
 * </p>
 *
 * <h4>Mengapa slug/schemaName adalah medan paling sensitif di seluruh paket</h4>
 * <p>
 * {@link #getSlug()} adalah satu-satunya nilai asal-pengguna (username yang diminta pendaftar)
 * yang akhirnya berakhir sebagai <b>identifier SQL</b>: {@code TenantSchemaService.buatSchema}
 * menyusun {@code CREATE SCHEMA IF NOT EXISTS "&lt;slug&gt;"} dan
 * {@code "&lt;slug&gt;__audit"}, dan seluruh DML data-plane
 * ({@code TenantDataPlaneService}, {@code TenantAuditWriter}) menyisipkan nama schema ke dalam
 * SQL native. Rantai pengamanannya berlapis dan sengaja diulang di setiap titik:
 * </p>
 * <ol>
 * <li>saat submit, {@code PendaftaranValidationService.normalisasiUsername} (NFKC + lowercase +
 * trim) lalu {@code usernameValid} (pola {@code ^[a-z][a-z0-9_]{2,30}$}) dan
 * {@code usernameReserved} menolak nilai di luar pola/kata terlarang;</li>
 * <li>ketersediaan diperiksa {@code UsernameReservationService.alasanTidakTersedia}, dan
 * kebenarannya dikunci INSERT {@link SchemaNameReservation} yang unique;</li>
 * <li>provisioning menyalin nilai itu ke {@link #setSlug(String)} -- bukan mengambil ulang dari
 * request;</li>
 * <li>SETIAP pemakaian dalam SQL memanggil ulang
 * {@code TenantSchemaService.pastikanAman(String)} (pola yang sama + daftar reserved), lalu nama
 * itu tetap dikutip ganda. Jadi karakter kutip, titik koma, spasi, dan tanda hubung sama sekali
 * tidak dapat lolos ke DDL -- lihat catatan verifikasi pada
 * {@link SchemaNameReservation#getNormalizedName()}.</li>
 * </ol>
 * <p>
 * Konsekuensi praktisnya: <b>jangan pernah</b> menambah jalur yang menulis {@link #setSlug(String)},
 * {@link #setSchemaName(String)}, atau {@link #setAuditSchemaName(String)} dari nilai request.
 * Selama penulisnya hanya provisioning, pola whitelist di atas cukup; begitu ada form CRUD yang
 * mengizinkan pengubahan medan ini, seluruh lapisan pengaman di atas dilewati sekaligus.
 * </p>
 *
 * <h4>Catatan Generic CRUD v2</h4>
 * <p>
 * Nama kelas ini tidak mengandung satu pun token pada {@code BLOCKED_CLASS_TOKENS}
 * ({@code password}, {@code user}, {@code role}, {@code job}, {@code audit}, dst.), sehingga
 * daftar {@code GenericCrudAutoDefinitionFactory.listAdministrativeModels()} menampilkannya
 * sebagai model biasa. Yang menahannya saat ini bukan daftar blokir, melainkan tidak adanya
 * Action ZK sumber: {@code mutable = !restricted &amp;&amp; (actionCreate || actionUpdate)}, dan
 * tidak ada {@code TenantRegistryAction} yang dapat mendukung mutasi. Bila kelak seseorang
 * membuat Action semacam itu, medan {@code slug}/{@code schemaName}/{@code status} mendadak dapat
 * diubah lewat CRUD generik -- itulah cara paling mudah melanggar invariant #3. Selain itu
 * properti pembatas kepemilikan entitas ini adalah {@link #getOwnerPendaftar()}, yang <b>tidak
 * termasuk</b> whitelist {@code GenericCrudAutoEntityAdapter.scopeBindings()}
 * ({@code yayasan}/{@code sekolah}/{@code program}/{@code fakultas}/{@code jurusan}/
 * {@code satuanKerja}), sehingga pembatasan cakupan otomatis tidak akan pernah terpasang untuk
 * kelas ini.
 * </p>
 *
 * @see ProvisioningJob
 * @see ProvisioningStep
 * @see SchemaNameReservation
 * @see TenantDomain
 * @see TenantModuleEntitlement
 * @see ais.service.tenant.TenantSchemaLocator
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tenant_registry")
public class TenantRegistry extends GeneralValueObject {

	/** Versi serialisasi entitas; tetap {@code 1L} selama bentuk medan tidak berubah. */
	private static final long serialVersionUID = 1L;

	/**
	 * Status awal: baris registry sudah ada, tetapi urutan {@link ProvisioningStep} belum tuntas.
	 * {@code TenantContextResolver.pastikanTenantDapatDipakai} menolak status ini dengan
	 * {@code TENANT_NOT_READY} -- sehingga tenant setengah jadi TIDAK dapat dipakai (fail-closed).
	 */
	public static final String STATUS_PROVISIONING = "PROVISIONING";

	/**
	 * Provisioning selesai seluruhnya (step {@code MARK_READY} sukses): schema ada, modul disemai,
	 * keanggotaan owner dibuat, dan jendela trial mulai dihitung. Sudah boleh dipakai.
	 */
	public static final String STATUS_READY = "READY";

	/**
	 * Tenant berlangganan/aktif penuh (kelanjutan {@link #STATUS_READY} setelah aktivasi
	 * langganan). Sama-sama diterima {@code TenantContextResolver}. Sejak status ini
	 * {@link #getSlug()} diperlakukan IMMUTABLE.
	 */
	public static final String STATUS_ACTIVE = "ACTIVE";

	/**
	 * Akses dihentikan sementara (mis. tunggakan/pelanggaran). Dibedakan dari &quot;belum siap&quot;
	 * karena klien perlu pesan berbeda: yang satu menyuruh menghubungi admin, yang satu menyuruh
	 * menunggu ({@code TENANT_SUSPENDED} vs {@code TENANT_NOT_READY}).
	 */
	public static final String STATUS_SUSPENDED = "SUSPENDED";

	/**
	 * Mode data-plane bawaan deployment: data tenant tetap tinggal di schema existing dan
	 * dipisahkan per-{@code Pendaftar} seperti sebelumnya. Pada mode ini {@link #getSchemaName()}
	 * dan {@link #getAuditSchemaName()} sengaja dibiarkan {@code null}, dan step
	 * {@code CREATE_SCHEMA_*}/{@code RUN_MIGRATIONS}/{@code VERIFY_SCHEMA} ditandai
	 * {@code SKIPPED} secara sah -- bukan gagal.
	 */
	public static final String MODE_LEGACY = "LEGACY";

	/**
	 * Tenant memperoleh schema PostgreSQL sendiri ({@code &lt;slug&gt;} + {@code &lt;slug&gt;__audit})
	 * sambil tetap berdampingan dengan data lama. Mode ini yang benar-benar menjalankan DDL, jadi
	 * seluruh validasi identifier pada {@code TenantSchemaService} berlaku penuh.
	 */
	public static final String MODE_HYBRID = "HYBRID";

	/** Seluruh data tenant hidup di schema sendiri (tahap akhir migrasi arsitektur). */
	public static final String MODE_TENANT_ONLY = "TENANT_ONLY";

	/** Kunci utama teknis (IDENTITY). */
	private Long id;
	/** Kode publik tenant, dibentuk provisioning dari id ({@code TEN-&lt;tahun&gt;-&lt;id&gt;}). */
	private String code;
	/** Nama tampilan tenant, disalin dari nama pendaftar/instansi saat registry dibuat. */
	private String nama;
	/** Username tenant global-unique; sumber nama schema pada mode HYBRID/TENANT_ONLY. */
	private String slug;
	/** Pemilik tenant (akun {@link Pendaftar} yang mengajukan permohonan). */
	private Pendaftar ownerPendaftar;
	/** Status siklus hidup: PROVISIONING/READY/ACTIVE/SUSPENDED. */
	private String status;
	/** Mode data-plane: LEGACY/HYBRID/TENANT_ONLY. */
	private String tenantMode;
	/** Nama schema data; hanya terisi pada mode HYBRID/TENANT_ONLY. */
	private String schemaName;
	/** Nama schema audit ({@code &lt;schema&gt;__audit}); hanya terisi pada HYBRID/TENANT_ONLY. */
	private String auditSchemaName;
	/** Versi migrasi schema yang sudah diterapkan pada schema tenant ini. */
	private String schemaVersion;
	/** Locale bawaan tenant; default {@code id_ID} bila kosong. */
	private String defaultLocale;
	/** Zona waktu tenant; default {@code Asia/Jakarta} bila kosong. */
	private String timezone;
	/** Awal jendela trial = saat tenant dinyatakan READY (invariant #7). */
	private Date trialStartAt;
	/** Akhir jendela trial = {@code trialStartAt + trialDaysSnapshot} hari. */
	private Date trialEndAt;
	/** Saat tenant benar-benar diaktifkan (berlangganan), bukan sekadar READY. */
	private Date activatedAt;
	/** Saat tenant disuspensi terakhir kali. */
	private Date suspendedAt;
	/** Saat baris registry dibuat oleh step {@code CREATE_TENANT_REGISTRY}. */
	private Date createdAt;
	/** Penghitung penguncian optimistik Hibernate ({@code @Version}). */
	private Integer version;

	/** Nama pengguna/proses penulis terakhir (audit shadow); diisi {@code "provisioning"} oleh job. */
	private String oleh;
	/** Id pengguna/proses penulis terakhir (audit shadow). */
	private String olehId;
	/**
	 * Cap waktu perubahan terakhir. Baris ini menggabungkan callback {@code @PreUpdate} dan
	 * deklarasi medan dalam satu baris fisik: itu KEHARUSAN TEKNIS pola audit shadow di seluruh
	 * repo ini (alat pembangkit menyisipkannya apa adanya), bukan gaya penulisan yang perlu
	 * dirapikan. Memecahnya menjadi beberapa baris membuat penyapu otomatis menulisnya kembali.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public TenantRegistry() {
	}

	/**
	 * Kunci utama teknis, dibangkitkan basis data ({@code IDENTITY}). Provisioning memanggil
	 * {@code session.flush()} tepat setelah {@code save} supaya nilai ini tersedia untuk membentuk
	 * {@link #getCode()}.
	 *
	 * @return id baris, atau {@code null} bila entitas belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Hanya Hibernate yang seharusnya memanggil ini.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode publik tenant (mis. {@code TEN-2026-000012}) -- aman ditampilkan/di-filter admin.
	 *
	 * <p>Dibentuk provisioning dari id baris setelah flush, jadi selalu deterministik dan tidak
	 * pernah berasal dari input pengguna. Dipakai {@code TenantContextResolver.resolveByCode}
	 * sebagai penunjuk tenant alternatif selain id numerik.</p>
	 *
	 * @return kode publik tenant, atau {@code null} sebelum step registry menuliskannya
	 */
	@Column(name = "code", unique = true, length = 40)
	public String getCode() {
		return code;
	}

	/**
	 * Menetapkan kode publik tenant.
	 *
	 * @param code kode terbentuk {@code TEN-&lt;tahun&gt;-&lt;id&gt;}
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Nama tampilan tenant. Getter ini <b>membersihkan spasi tepi</b> pada nilai yang dikembalikan
	 * tanpa mengubah medan yang tersimpan -- perhatikan bedanya dengan getter destruktif di modul
	 * lain, yang menulis balik hasil olahannya ke medan sehingga nilai kolom ikut berubah saat
	 * entitas di-flush. Di sini medan {@link #nama} dibiarkan apa adanya.
	 *
	 * @return nama tenant tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return nama == null ? null : nama.trim();
	}

	/**
	 * Menetapkan nama tampilan tenant (disalin dari nama pendaftar saat registry dibuat).
	 *
	 * @param nama nama instansi/usaha
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Username/slug tenant global-unique, IMMUTABLE setelah tenant aktif.
	 *
	 * <p>Inilah nilai yang dipakai provisioning sebagai <b>nama schema</b> pada mode
	 * HYBRID/TENANT_ONLY ({@code TenantSchemaService.buatSchema(session, tenant.getSlug())}) dan
	 * sebagai label subdomain bawaan ({@code &lt;slug&gt;.&lt;base&gt;} pada {@link TenantDomain}).
	 * Nilainya berasal dari {@code PendaftaranTenant.normalizedUsername} yang sudah lolos pola
	 * {@code ^[a-z][a-z0-9_]{2,30}$} + daftar reserved, dan tetap divalidasi ulang setiap kali
	 * hendak masuk SQL. Lihat uraian lengkap pada Javadoc kelas.</p>
	 *
	 * @return slug tenant (huruf kecil, tanpa spasi), tidak pernah {@code null} pada baris tersimpan
	 */
	@Column(name = "slug", unique = true, nullable = false, length = 64)
	public String getSlug() {
		return slug;
	}

	/**
	 * Menetapkan slug tenant. HANYA boleh dipanggil provisioning dengan nilai
	 * {@code PendaftaranTenant.normalizedUsername} yang sudah tervalidasi; jangan pernah dari
	 * parameter request.
	 *
	 * @param slug username tenant yang sudah dinormalisasi dan direservasi
	 */
	public void setSlug(String slug) {
		this.slug = slug;
	}

	/**
	 * Pemilik tenant: akun {@link Pendaftar} yang mengajukan permohonan. Dipakai
	 * {@code TenantContextResolver.daftarTenantAktif} sebagai jalur kompatibilitas -- pemilik
	 * selalu melihat tenantnya meskipun baris {@code TenantMembership}-nya hilang.
	 *
	 * <p>Getter memanggil {@code check(...)} milik {@code GeneralValueObject}, yang menetralkan
	 * proxy Hibernate yang sudah tidak dapat di-inisialisasi (session tertutup) menjadi
	 * {@code null} alih-alih melempar {@code LazyInitializationException}. Karena hasilnya
	 * ditulis balik ke medan, relasi yang gagal dimuat menjadi {@code null} secara permanen pada
	 * instance ini.</p>
	 *
	 * @return pemilik tenant, atau {@code null} bila relasinya tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_pendaftar_id", nullable = false)
	public Pendaftar getOwnerPendaftar() {
		ownerPendaftar = check(ownerPendaftar);
		return ownerPendaftar;
	}

	/**
	 * Menetapkan pemilik tenant.
	 *
	 * @param ownerPendaftar akun pendaftar pemilik
	 */
	public void setOwnerPendaftar(Pendaftar ownerPendaftar) {
		this.ownerPendaftar = ownerPendaftar;
	}

	/**
	 * Status siklus hidup tenant. Getter menerapkan <b>default yang aman</b>: nilai kosong
	 * dianggap {@link #STATUS_PROVISIONING}, yaitu status yang DITOLAK
	 * {@code TenantContextResolver.pastikanTenantDapatDipakai}. Baris yang datanya rusak/setengah
	 * tertulis dengan demikian tidak dapat dipakai, bukan malah lolos sebagai tenant siap pakai.
	 *
	 * @return salah satu dari {@link #STATUS_PROVISIONING}, {@link #STATUS_READY},
	 *         {@link #STATUS_ACTIVE}, {@link #STATUS_SUSPENDED}
	 */
	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_PROVISIONING : status;
	}

	/**
	 * Menetapkan status siklus hidup tenant. READY hanya boleh ditulis step {@code MARK_READY}
	 * setelah seluruh step sebelumnya SUCCESS/SKIPPED.
	 *
	 * @param status salah satu konstanta {@code STATUS_*}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mode data-plane tenant. Sama seperti {@link #getStatus()}, nilai kosong jatuh ke pilihan
	 * paling aman yaitu {@link #MODE_LEGACY} -- mode yang TIDAK menyentuh DDL sama sekali, sehingga
	 * baris yang tidak lengkap tidak pernah menyebabkan schema dibuat/dipakai secara tak sengaja.
	 * {@code TenantSchemaLocator.butuhSchema} membaca nilai ini untuk memutuskan apakah nama schema
	 * wajib ada.
	 *
	 * @return {@link #MODE_LEGACY}, {@link #MODE_HYBRID}, atau {@link #MODE_TENANT_ONLY}
	 */
	@Column(name = "tenant_mode", nullable = false, length = 20)
	public String getTenantMode() {
		return tenantMode == null || tenantMode.trim().isEmpty() ? MODE_LEGACY : tenantMode;
	}

	/**
	 * Menetapkan mode data-plane; diisi provisioning dari konfigurasi
	 * {@code pendaftaran_tenant_mode}.
	 *
	 * @param tenantMode salah satu konstanta {@code MODE_*}
	 */
	public void setTenantMode(String tenantMode) {
		this.tenantMode = tenantMode;
	}

	/**
	 * Nama schema ERP ({@code <slug>}). Null pada mode LEGACY. Hanya ditulis provisioning service.
	 *
	 * <p>Pembaca yang benar bukan getter ini secara langsung, melainkan
	 * {@code TenantSchemaLocator.schemaData(TenantRegistry)}: di sanalah nilai ini divalidasi ulang
	 * ({@code pastikanAman}) dan kekosongan pada mode yang menuntut schema diterjemahkan menjadi
	 * {@code TENANT_NOT_READY}. Menyebarkan pemakaian getter mentah ke banyak tempat berarti setiap
	 * tempat itu harus ingat memvalidasi -- persis kesalahan yang ingin dihindari desain ini.</p>
	 *
	 * @return nama schema data, atau {@code null} pada mode LEGACY / sebelum schema dibuat
	 */
	@Column(name = "schema_name", unique = true, length = 64)
	public String getSchemaName() {
		return schemaName;
	}

	/**
	 * Menetapkan nama schema data. Ditulis satu tempat saja: step {@code CREATE_SCHEMA_AUDIT} pada
	 * {@code TenantProvisioningService}, dengan nilai {@link #getSlug()} yang sudah lolos
	 * {@code TenantSchemaService.pastikanAman} saat schema-nya dibuat.
	 *
	 * @param schemaName nama schema data hasil provisioning
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * Nama schema audit ({@code <slug>__audit}). Null pada mode LEGACY.
	 *
	 * <p>Perhatikan bahwa nama turunan ini <b>tidak boleh</b> divalidasi dengan
	 * {@code TenantSchemaService.pastikanAman} apa adanya: pola itu membatasi 31 karakter,
	 * sedangkan akhiran {@code __audit} menambah tujuh karakter. Validatornya adalah
	 * {@code TenantSchemaLocator.pastikanAmanAudit}, yang memvalidasi basisnya lalu memastikan
	 * akhirannya.</p>
	 *
	 * @return nama schema audit, atau {@code null} pada mode LEGACY
	 */
	@Column(name = "audit_schema_name", unique = true, length = 70)
	public String getAuditSchemaName() {
		return auditSchemaName;
	}

	/**
	 * Menetapkan nama schema audit; ditulis provisioning bersamaan dengan
	 * {@link #setSchemaName(String)}.
	 *
	 * @param auditSchemaName nama schema audit ({@code &lt;slug&gt;__audit})
	 */
	public void setAuditSchemaName(String auditSchemaName) {
		this.auditSchemaName = auditSchemaName;
	}

	/**
	 * Versi migrasi schema yang sudah terpasang pada schema tenant ini. Diisi
	 * {@code TenantSchemaService.SCHEMA_VERSION_AWAL} saat schema baru dibuat, lalu dinaikkan ke
	 * {@code TenantSchemaMigrations.VERSI_TERKINI} setelah step {@code RUN_MIGRATIONS}.
	 * {@code TenantContextResolver.pastikanSchemaMutakhir} membandingkannya dengan versi yang
	 * dituntut aplikasi dan menolak request bila tertinggal -- lebih baik menolak daripada
	 * mengkueri tabel/kolom yang belum ada.
	 *
	 * @return kode versi schema, atau {@code null} pada mode LEGACY
	 */
	@Column(name = "schema_version", length = 40)
	public String getSchemaVersion() {
		return schemaVersion;
	}

	/**
	 * Menetapkan versi migrasi schema tenant.
	 *
	 * @param schemaVersion kode versi migrasi
	 */
	public void setSchemaVersion(String schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	/**
	 * Locale bawaan tenant. Nilai kosong dijawab {@code "id_ID"} supaya lapisan penyaji tidak perlu
	 * menangani {@code null}; nilai default ini TIDAK ditulis balik ke medan, jadi kolom tetap
	 * kosong sampai step {@code SEED_CONFIGURATION} mengisinya.
	 *
	 * @return kode locale, minimal {@code "id_ID"}
	 */
	@Column(name = "default_locale", length = 20)
	public String getDefaultLocale() {
		return defaultLocale == null || defaultLocale.trim().isEmpty() ? "id_ID" : defaultLocale;
	}

	/**
	 * Menetapkan locale bawaan tenant.
	 *
	 * @param defaultLocale kode locale (mis. {@code id_ID})
	 */
	public void setDefaultLocale(String defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * Zona waktu tenant, dipakai seluruh tampilan/laporan tenant. Nilai kosong dijawab
	 * {@code "Asia/Jakarta"} (tanpa menulis balik ke medan). Step {@code SEED_CONFIGURATION}
	 * mengisinya dari profil pendaftar bila tersedia.
	 *
	 * @return id zona waktu, minimal {@code "Asia/Jakarta"}
	 */
	@Column(name = "timezone", length = 64)
	public String getTimezone() {
		return timezone == null || timezone.trim().isEmpty() ? "Asia/Jakarta" : timezone;
	}

	/**
	 * Menetapkan zona waktu tenant.
	 *
	 * @param timezone id zona waktu IANA (mis. {@code Asia/Jakarta})
	 */
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	/**
	 * Awal jendela trial. Invariant #7: dihitung dari saat tenant READY, BUKAN saat formulir
	 * pendaftaran dibuka -- pendaftar yang provisioning-nya tertunda tidak kehilangan masa
	 * percobaan.
	 *
	 * @return awal trial, atau {@code null} sebelum step {@code MARK_READY}
	 */
	@Column(name = "trial_start_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTrialStartAt() {
		return trialStartAt;
	}

	/**
	 * Menetapkan awal jendela trial (diisi step {@code MARK_READY}).
	 *
	 * @param trialStartAt cap waktu tenant dinyatakan READY
	 */
	public void setTrialStartAt(Date trialStartAt) {
		this.trialStartAt = trialStartAt;
	}

	/**
	 * Akhir jendela trial = {@link #getTrialStartAt()} + {@code trialDaysSnapshot} hari (snapshot
	 * diambil dari permohonan, sehingga perubahan konfigurasi lama kelamaan tidak mengubah janji
	 * yang sudah diberikan kepada pendaftar).
	 *
	 * @return akhir trial, atau {@code null} sebelum step {@code MARK_READY}
	 */
	@Column(name = "trial_end_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTrialEndAt() {
		return trialEndAt;
	}

	/**
	 * Menetapkan akhir jendela trial.
	 *
	 * @param trialEndAt cap waktu berakhirnya masa percobaan
	 */
	public void setTrialEndAt(Date trialEndAt) {
		this.trialEndAt = trialEndAt;
	}

	/**
	 * Saat tenant benar-benar diaktifkan (berlangganan berbayar), berbeda dari sekadar READY.
	 *
	 * @return cap waktu aktivasi, atau {@code null} bila belum pernah diaktifkan
	 */
	@Column(name = "activated_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getActivatedAt() {
		return activatedAt;
	}

	/**
	 * Menetapkan cap waktu aktivasi tenant.
	 *
	 * @param activatedAt cap waktu aktivasi
	 */
	public void setActivatedAt(Date activatedAt) {
		this.activatedAt = activatedAt;
	}

	/**
	 * Saat tenant terakhir kali disuspensi. Medan ini bersifat informatif; yang benar-benar
	 * menggerbang akses adalah {@link #getStatus()} bernilai {@link #STATUS_SUSPENDED}.
	 *
	 * @return cap waktu suspensi terakhir, atau {@code null}
	 */
	@Column(name = "suspended_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSuspendedAt() {
		return suspendedAt;
	}

	/**
	 * Menetapkan cap waktu suspensi.
	 *
	 * @param suspendedAt cap waktu suspensi
	 */
	public void setSuspendedAt(Date suspendedAt) {
		this.suspendedAt = suspendedAt;
	}

	/**
	 * Saat baris registry dibuat (step {@code CREATE_TENANT_REGISTRY}). Berbeda dari
	 * {@code PendaftaranTenant.createdAt}, yang mencatat saat permohonan diajukan.
	 *
	 * @return cap waktu pembuatan registry
	 */
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Menetapkan cap waktu pembuatan registry.
	 *
	 * @param createdAt cap waktu pembuatan
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Penghitung penguncian optimistik Hibernate. Karena baris ini disentuh beberapa step
	 * provisioning dalam transaksi terpisah (dan mungkin oleh backoffice admin secara bersamaan),
	 * kolom versi mencegah dua penulis saling menimpa diam-diam.
	 *
	 * @return nomor versi baris
	 */
	@Version
	@Column(name = "version")
	public Integer getVersion() {
		return version;
	}

	/**
	 * Menetapkan nomor versi; dikelola Hibernate, bukan kode aplikasi.
	 *
	 * @param version nomor versi baris
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}

	/**
	 * Nama pengguna/proses penulis terakhir (bagian pola audit shadow repo ini). Provisioning
	 * mengisinya dengan {@code "provisioning"}.
	 *
	 * @return nama penulis terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama penulis terakhir. Perhatikan setter ini <b>menolak nilai kosong secara
	 * diam-diam</b> (return lebih awal): pemanggil yang mengirim string kosong tidak menghapus
	 * nilai lama. Itu memang disengaja pada pola audit shadow -- jejak penulis sebelumnya lebih
	 * berguna daripada kolom yang dikosongkan oleh form yang tidak mengisinya.
	 *
	 * @param oleh nama penulis; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Id pengguna/proses penulis terakhir (audit shadow).
	 *
	 * @return id penulis terakhir, atau {@code null}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id penulis terakhir; sama seperti {@link #setOleh(String)}, nilai kosong
	 * diabaikan sehingga jejak lama tidak terhapus.
	 *
	 * @param olehId id penulis; nilai {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Cap waktu perubahan terakhir, diperbarui otomatis lewat {@code @PreUpdate}
	 * ({@code AuditTimestampInterceptor.ubah}). Nilai awalnya diisi saat objek dibuat, sehingga
	 * baris baru pun selalu punya cap waktu.
	 *
	 * @return cap waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan cap waktu perubahan terakhir. Umumnya tidak dipanggil kode aplikasi -- callback
	 * {@code @PreUpdate} yang mengurusnya.
	 *
	 * @param tanggal_dirubah cap waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
