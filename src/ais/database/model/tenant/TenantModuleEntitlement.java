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
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Module entitlement per tenant (entitlement &ne; permission -- invariant #13 ERD).</h3>
 *
 * <p>Union modul dari jenis usaha terpilih ({@code source=BUSINESS_TYPE}) + paket
 * ({@code SUBSCRIPTION_PLAN}) + add-on/override/trial. Entitlement menentukan MODUL apa
 * tersedia bagi tenant; IZIN tindakan pengguna tetap urusan role
 * ({@code Tbmrole.ebisnis_menu} dsb.). Modul yang source-nya belum operasional tercatat
 * dgn status {@link #STATUS_PLANNED} -- UI wajib jujur, bukan tombol semu.</p>
 *
 * <h4>Dua pertanyaan yang tidak boleh tertukar</h4>
 * <p>
 * &quot;Apakah tenant ini berlangganan modul kasir?&quot; dijawab tabel ini. &quot;Bolehkah
 * pengguna ini membatalkan transaksi kasir?&quot; dijawab peran. Menggabungkan keduanya adalah
 * kesalahan yang mahal ke dua arah: entitlement yang dipakai sebagai izin membuat setiap anggota
 * tenant otomatis boleh melakukan apa pun di modul yang dilanggan, sedangkan izin yang dipakai
 * sebagai entitlement membuat modul yang tidak dilanggan tetap terbuka bagi pengguna berperan
 * tinggi. Karena itu {@code TenantContextResolver.pastikanModulAktif} sengaja dipanggil
 * <i>terpisah</i> dari pemeriksaan peran, dan hanya untuk aksi yang memang terikat modul.
 * </p>
 *
 * <h4>Bagaimana baris ini dibaca saat request</h4>
 * <p>
 * {@code TenantContextResolver.muatModul} mengambil {@code moduleCode} milik tenant yang
 * berstatus {@link #STATUS_ACTIVE}, lalu menyaring lagi berdasarkan jendela waktu
 * {@link #getEffectiveFrom()}/{@link #getEffectiveUntil()} terhadap waktu sekarang, dan
 * menormalkan kodenya ke HURUF BESAR sebelum dimasukkan ke {@code TenantContext}. Perhatikan
 * urutan itu: penyaringan jendela waktu dilakukan di Java, bukan di HQL, sehingga baris yang
 * belum berlaku atau sudah lewat tetap terbawa dari basis data namun tidak pernah masuk ke
 * himpunan modul aktif. Modul berstatus {@link #STATUS_PLANNED} juga tidak pernah ikut -- itulah
 * yang membuat kejujuran UI dapat ditegakkan tanpa cabang tambahan.
 * </p>
 *
 * <h4>Kolom {@code source} dan kunci uniknya</h4>
 * <p>
 * Constraint {@code UNIQUE(tenant_id, module_code, source)} disengaja memuat {@code source}:
 * satu modul boleh di-entitle dari beberapa asal sekaligus (mis. sudah termasuk jenis usaha,
 * lalu ditambahkan lagi lewat paket langganan). Modelnya union, bukan satu baris yang
 * ditimpa-timpa; mencabut satu asal karena itu tidak diam-diam mencabut asal lainnya, dan
 * riwayat &quot;dari mana modul ini datang&quot; tetap terbaca. Konsekuensinya, pembaca yang
 * ingin tahu apakah sebuah modul aktif harus memeriksa keberadaan SETIDAKNYA SATU baris yang
 * memenuhi syarat -- persis yang dilakukan {@code muatModul} dengan mengumpulkannya ke dalam
 * himpunan.
 * </p>
 *
 * <h4>Catatan Generic CRUD v2: properti {@code tenant} tidak dikenali whitelist</h4>
 * <p>
 * Pembatas kepemilikan baris ini adalah {@link #getTenant()}. Whitelist
 * {@code GenericCrudAutoEntityAdapter.scopeBindings()} hanya mengenal properti bernama persis
 * {@code yayasan}, {@code sekolah}, {@code program}, {@code fakultas}, {@code jurusan},
 * {@code satuanKerja} (ditambah beberapa properti bersyarat peran seperti {@code mahasiswa},
 * {@code siswa}, {@code dosen}, {@code guru}), sehingga <b>{@code tenant} tidak pernah dipasang
 * sebagai pembatas cakupan otomatis</b>. Bila entitas ini kelak terjangkau CRUD generik bagi
 * pengguna non-admin, daftar yang tampil adalah entitlement SELURUH tenant, bukan milik tenant
 * pengguna itu -- pola celah yang sama sudah tercatat pada beberapa modul lain
 * ({@code library.DetailTransaksi}, {@code inventory.Produk}/{@code Toko}).
 * </p>
 * <p>
 * Yang menahannya saat ini: nama kelas tidak memicu {@code BLOCKED_CLASS_TOKENS}, tetapi mutasi
 * lewat adapter otomatis menuntut adanya Action ZK sumber
 * ({@code mutable = !restricted &amp;&amp; (actionCreate || actionUpdate)}), dan tidak ada
 * {@code TenantModuleEntitlementAction}. Jadi entitas ini praktis hanya dapat dibaca lewat admin
 * model browser. Bila kelak Action semacam itu dibuat, pemasangan pembatas cakupan harus
 * ditangani adapter eksplisit -- jangan mengandalkan {@code scopeBindings()} generik, sebab ia
 * memang tidak mengenal konsep tenant.
 * </p>
 *
 * @see TenantRegistry
 * @see JenisUsahaTenant
 * @see ProvisioningStep#STEP_SEED_MODULES
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tenant_module_entitlement",
		uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "module_code", "source" }))
public class TenantModuleEntitlement extends GeneralValueObject {

	/** Versi serialisasi entitas; tetap {@code 1L} selama bentuk medan tidak berubah. */
	private static final long serialVersionUID = 1L;

	/**
	 * Modul datang dari jenis usaha yang dipilih pendaftar; disemai step
	 * {@link ProvisioningStep#STEP_SEED_MODULES}. Sekaligus nilai bawaan getter {@code source}
	 * untuk kolom kosong.
	 */
	public static final String SOURCE_BUSINESS_TYPE = "BUSINESS_TYPE";

	/** Modul datang dari paket langganan yang dipilih tenant. */
	public static final String SOURCE_SUBSCRIPTION_PLAN = "SUBSCRIPTION_PLAN";

	/** Modul dibeli terpisah sebagai tambahan di luar paket. */
	public static final String SOURCE_ADD_ON = "ADD_ON";

	/**
	 * Modul dinyalakan/dimatikan admin platform secara manual. Karena kunci uniknya memuat
	 * {@code source}, override ini hidup sebagai baris tersendiri dan tidak menghapus jejak asal
	 * lain -- pencabutannya pun tidak menyentuh entitlement dari paket/jenis usaha.
	 */
	public static final String SOURCE_ADMIN_OVERRIDE = "ADMIN_OVERRIDE";

	/** Modul diberikan sementara selama masa percobaan tenant. */
	public static final String SOURCE_TRIAL = "TRIAL";

	/**
	 * Entitlement berlaku (masih tunduk jendela {@code effectiveFrom}/{@code effectiveUntil}).
	 * Sekaligus nilai bawaan getter status untuk kolom kosong -- perhatikan bahwa di sini default
	 * kosong berarti AKTIF, kebalikan dari pola gagal-tertutup pada entitas lain di paket ini.
	 * Itu dapat diterima karena entitlement bukan mekanisme izin: modul yang keliru aktif tetap
	 * dijaga peran, sedangkan entitlement yang keliru mati akan tampak sebagai gangguan layanan
	 * bagi pelanggan yang sudah membayar.
	 */
	public static final String STATUS_ACTIVE = "ACTIVE";

	/** Entitlement dicabut/dinonaktifkan; barisnya tetap tersimpan sebagai riwayat. */
	public static final String STATUS_DISABLED = "DISABLED";

	/** Modul di-entitle tapi implementasi source-nya belum tersedia/operasional. */
	public static final String STATUS_PLANNED = "PLANNED";

	/** Kunci utama teknis (IDENTITY). */
	private Long id;
	/** Tenant pemilik entitlement; wajib, dan bagian kunci unik. */
	private TenantRegistry tenant;
	/** Kode modul yang di-entitle; bagian kunci unik. */
	private String moduleCode;
	/** Asal entitlement; bagian kunci unik sehingga satu modul boleh punya beberapa asal. */
	private String source;
	/** Status entitlement: ACTIVE/DISABLED/PLANNED. */
	private String status;
	/** Awal jendela berlaku (opsional). */
	private Date effectiveFrom;
	/** Akhir jendela berlaku (opsional). */
	private Date effectiveUntil;
	/** Batas numerik opsional yang menyertai entitlement. */
	private Long limitValue;
	/** Jenis usaha asal entitlement, untuk jejak audit. */
	private JenisUsahaTenant selectedJenisUsaha;
	/** Versi paket saat entitlement dibuat (snapshot). */
	private String planVersion;
	/** Saat baris entitlement dibuat. */
	private Date createdAt;
	/** Penghitung penguncian optimistik Hibernate ({@code @Version}). */
	private Integer version;

	/** Nama pengguna/proses penulis terakhir (audit shadow). */
	private String oleh;
	/** Id pengguna/proses penulis terakhir (audit shadow). */
	private String olehId;
	/**
	 * Cap waktu perubahan terakhir. Callback {@code @PreUpdate} dan deklarasi medan pada satu baris
	 * fisik adalah bentuk baku pola audit shadow repo ini (disisipkan alat pembangkit), bukan
	 * kelalaian format.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public TenantModuleEntitlement() {
	}

	/**
	 * Kunci utama teknis, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * @return id baris entitlement, atau {@code null} bila belum disimpan
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
	 * Tenant pemilik entitlement ini (wajib) -- pembatas kepemilikan baris. Lihat catatan pada
	 * Javadoc kelas: properti bernama {@code tenant} TIDAK dikenali whitelist
	 * {@code GenericCrudAutoEntityAdapter.scopeBindings()}, sehingga pembatasan cakupan otomatis
	 * pada jalur CRUD generik tidak akan pernah terpasang untuk entitas ini.
	 *
	 * <p>Getter memakai {@code check(...)} milik {@code GeneralValueObject}: proxy Hibernate yang
	 * tidak dapat lagi di-inisialisasi menjadi {@code null} (hasilnya ditulis balik ke medan)
	 * alih-alih melempar {@code LazyInitializationException}.</p>
	 *
	 * @return tenant pemilik, atau {@code null} bila relasinya tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tenant_id", nullable = false)
	public TenantRegistry getTenant() {
		tenant = check(tenant);
		return tenant;
	}

	/**
	 * Menetapkan tenant pemilik entitlement.
	 *
	 * @param tenant registry tenant pemilik
	 */
	public void setTenant(TenantRegistry tenant) {
		this.tenant = tenant;
	}

	/**
	 * Kode modul yang di-entitle. Pembanding di lapisan atas bekerja atas bentuk HURUF BESAR --
	 * {@code TenantContextResolver.muatModul} menormalkannya saat memuat, dan
	 * {@code TenantContext.punyaModul} membandingkan bentuk ternormalisasi itu -- sehingga
	 * perbedaan besar-kecil huruf pada kolom tidak menyebabkan modul luput dikenali.
	 *
	 * @return kode modul; tidak pernah {@code null} pada baris tersimpan
	 */
	@Column(name = "module_code", nullable = false, length = 64)
	public String getModuleCode() {
		return moduleCode;
	}

	/**
	 * Menetapkan kode modul.
	 *
	 * @param moduleCode kode modul (mis. {@code POS}, {@code INVENTORY})
	 */
	public void setModuleCode(String moduleCode) {
		this.moduleCode = moduleCode;
	}

	/**
	 * Asal entitlement, bagian dari kunci unik {@code (tenant_id, module_code, source)}. Kolom
	 * kosong dibaca {@link #SOURCE_BUSINESS_TYPE} (tanpa menulis balik ke medan), yaitu asal yang
	 * disemai provisioning.
	 *
	 * @return salah satu konstanta {@code SOURCE_*}; minimal {@link #SOURCE_BUSINESS_TYPE}
	 */
	@Column(name = "source", nullable = false, length = 40)
	public String getSource() {
		return source == null || source.trim().isEmpty() ? SOURCE_BUSINESS_TYPE : source;
	}

	/**
	 * Menetapkan asal entitlement.
	 *
	 * @param source salah satu konstanta {@code SOURCE_*}
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * Status entitlement. Kolom kosong dibaca {@link #STATUS_ACTIVE}; alasan mengapa default
	 * &quot;terbuka&quot; ini dapat diterima -- entitlement bukan mekanisme izin -- dijelaskan pada
	 * Javadoc konstanta {@link #STATUS_ACTIVE}.
	 *
	 * @return salah satu konstanta {@code STATUS_*}; minimal {@link #STATUS_ACTIVE}
	 */
	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_ACTIVE : status;
	}

	/**
	 * Menetapkan status entitlement.
	 *
	 * @param status salah satu konstanta {@code STATUS_*}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Awal jendela berlaku. {@code null} berarti tidak ada batas awal (berlaku sejak dibuat).
	 * Disaring {@code TenantContextResolver.muatModul} di sisi Java: baris yang belum berlaku tidak
	 * dimasukkan ke himpunan modul aktif.
	 *
	 * @return awal jendela berlaku, atau {@code null}
	 */
	@Column(name = "effective_from")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getEffectiveFrom() {
		return effectiveFrom;
	}

	/**
	 * Menetapkan awal jendela berlaku.
	 *
	 * @param effectiveFrom cap waktu mulai berlaku, boleh {@code null}
	 */
	public void setEffectiveFrom(Date effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	/**
	 * Akhir jendela berlaku. {@code null} berarti tidak berbatas waktu. Dipakai antara lain untuk
	 * entitlement bersumber {@link #SOURCE_TRIAL}, yang berakhir bersama masa percobaan tenant.
	 *
	 * @return akhir jendela berlaku, atau {@code null}
	 */
	@Column(name = "effective_until")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getEffectiveUntil() {
		return effectiveUntil;
	}

	/**
	 * Menetapkan akhir jendela berlaku.
	 *
	 * @param effectiveUntil cap waktu berakhir, boleh {@code null}
	 */
	public void setEffectiveUntil(Date effectiveUntil) {
		this.effectiveUntil = effectiveUntil;
	}

	/**
	 * Limit numerik opsional (mis. jumlah mesin POS utk billing per-device).
	 *
	 * <p>Nilainya hanya tercatat di sini; penegakan batas adalah urusan modul yang bersangkutan.
	 * {@code TenantContext} tidak membawanya, jadi jangan menganggap kehadiran baris entitlement
	 * sudah berarti kuotanya terjaga.</p>
	 *
	 * @return batas numerik, atau {@code null} bila entitlement tidak berkuota
	 */
	@Column(name = "limit_value")
	public Long getLimitValue() {
		return limitValue;
	}

	/**
	 * Menetapkan batas numerik entitlement.
	 *
	 * @param limitValue batas numerik, boleh {@code null}
	 */
	public void setLimitValue(Long limitValue) {
		this.limitValue = limitValue;
	}

	/**
	 * Jenis usaha asal entitlement ini (audit trail source=BUSINESS_TYPE).
	 *
	 * <p>Bersifat penjelas, bukan penentu: yang menentukan tetap {@link #getSource()} dan
	 * {@link #getStatus()}. Relasinya {@code nullable} karena entitlement dari paket, add-on,
	 * override admin, maupun trial tidak berasal dari jenis usaha mana pun. Getter memakai
	 * {@code check(...)} seperti relasi lain di paket ini.</p>
	 *
	 * @return jenis usaha asal, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_usaha_tenant_id", nullable = true)
	public JenisUsahaTenant getSelectedJenisUsaha() {
		selectedJenisUsaha = check(selectedJenisUsaha);
		return selectedJenisUsaha;
	}

	/**
	 * Menetapkan jenis usaha asal entitlement.
	 *
	 * @param selectedJenisUsaha jenis usaha sumber, boleh {@code null}
	 */
	public void setSelectedJenisUsaha(JenisUsahaTenant selectedJenisUsaha) {
		this.selectedJenisUsaha = selectedJenisUsaha;
	}

	/**
	 * Versi paket saat entitlement ini dibuat. Disimpan sebagai snapshot supaya perubahan definisi
	 * paket di kemudian hari tidak mengubah secara surut apa yang sudah dijanjikan kepada tenant --
	 * pola yang sama dipakai {@code trialDaysSnapshot} pada permohonan.
	 *
	 * @return kode versi paket, atau {@code null}
	 */
	@Column(name = "plan_version", length = 64)
	public String getPlanVersion() {
		return planVersion;
	}

	/**
	 * Menetapkan versi paket saat entitlement dibuat.
	 *
	 * @param planVersion kode versi paket
	 */
	public void setPlanVersion(String planVersion) {
		this.planVersion = planVersion;
	}

	/**
	 * Saat baris entitlement dibuat.
	 *
	 * @return cap waktu pembuatan
	 */
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Menetapkan cap waktu pembuatan baris entitlement.
	 *
	 * @param createdAt cap waktu pembuatan
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Penghitung penguncian optimistik Hibernate; entitlement dapat disentuh provisioning,
	 * pembaruan langganan, dan override admin pada waktu yang berbeda-beda.
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
	 * Nama pengguna/proses penulis terakhir (pola audit shadow). Untuk entitlement bersumber
	 * {@link #SOURCE_ADMIN_OVERRIDE}, kolom inilah jejak siapa yang menyalakan modul secara manual.
	 *
	 * @return nama penulis terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama penulis terakhir. Nilai {@code null}/kosong sengaja diabaikan supaya jejak
	 * penulis sebelumnya tidak terhapus oleh pemanggil yang tidak mengisinya.
	 *
	 * @param oleh nama penulis; nilai kosong diabaikan
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
	 * Menetapkan id penulis terakhir; nilai kosong diabaikan, sama seperti {@link #setOleh(String)}.
	 *
	 * @param olehId id penulis; nilai kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Cap waktu perubahan terakhir, diperbarui otomatis lewat {@code @PreUpdate}
	 * ({@code AuditTimestampInterceptor.ubah}); nilai awal diisi saat objek dibuat.
	 *
	 * @return cap waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan cap waktu perubahan terakhir; umumnya diurus callback {@code @PreUpdate}.
	 *
	 * @param tanggal_dirubah cap waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
