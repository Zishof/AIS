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

/**
 * <h3>Job provisioning satu permohonan tenant (job-table + worker locking).</h3>
 *
 * <p>Worker ({@code TenantProvisioningWorker}, pola scheduler {@code DepositoAroScheduler})
 * mengklaim job dgn {@code SELECT ... FOR UPDATE} + lease {@link #getLockedBy()}/
 * {@link #getLockedAt()} -- SENGAJA tanpa {@code SKIP LOCKED} (PostgreSQL deployment bisa 9.3).
 * Satu job tidak diproses dua node sekaligus; retry melanjutkan dari {@link ProvisioningStep}
 * yang belum SUCCESS, bukan mengulang semua (invariant #11 ERD).</p>
 *
 * <h4>Apa yang sebenarnya dikerjakan job ini</h4>
 * <p>
 * Satu baris {@code ProvisioningJob} mewakili satu upaya menjadikan sebuah
 * {@link PendaftaranTenant} (permohonan) menjadi tenant yang benar-benar dapat dipakai. Job
 * dieksekusi {@code TenantProvisioningService.jalankanJob(Long)}, yang berjalan menyusuri urutan
 * kanonik enam belas step (&sect;7.9 dokumen master, lihat konstanta {@code STEP_*} pada
 * {@link ProvisioningStep}): validasi permohonan, memastikan reservasi username, membuat
 * {@link TenantRegistry} beserta subdomain bawaan {@link TenantDomain}, membuat schema data dan
 * schema audit, menjalankan migrasi kanonik, menyemai konfigurasi/modul
 * ({@link TenantModuleEntitlement})/peran, membuat keanggotaan pemilik, memverifikasi schema dan
 * kredensial login, lalu menandai tenant READY.
 * </p>
 * <p>
 * Jadi inilah satu-satunya jalur di seluruh aplikasi yang <b>membuat sumber daya basis data
 * baru</b> atas permintaan orang luar. Karena itu setiap keputusannya dirancang gagal-tertutup;
 * uraian di bawah menjelaskan bagaimana.
 * </p>
 *
 * <h4>Satu transaksi per step, bukan satu transaksi per job</h4>
 * <p>
 * {@code TenantProvisioningService.langkah(Long, String)} membuka Session+transaksi sendiri untuk
 * SETIAP step. Pilihan ini disengaja: DDL PostgreSQL memang transaksional, tetapi membungkus
 * enam belas step (termasuk migrasi tabel dan penyemaian data) dalam satu transaksi panjang akan
 * menahan kunci katalog terlalu lama dan membuat kegagalan di step ke-15 membuang pekerjaan step
 * 1-14. Konsekuensinya, kegagalan di tengah meninggalkan pekerjaan step-step sebelumnya tetap
 * ter-commit -- dan justru itulah yang membuat retry murah: {@link ProvisioningStep} mencatat
 * status per step, step yang sudah SUCCESS/SKIPPED dilewati pada percobaan berikutnya.
 * </p>
 *
 * <h4>Kegagalan tidak menghasilkan tenant setengah jadi yang dapat dipakai</h4>
 * <p>
 * Pertanyaan wajar atas desain di atas: bila step gagal di tengah, bukankah ada baris
 * {@link TenantRegistry} yang sudah terlanjur dibuat? Ada -- tetapi baris itu berstatus
 * {@link TenantRegistry#STATUS_PROVISIONING}, dan hanya step terakhir
 * ({@code MARK_READY}) yang menaikkannya menjadi {@link TenantRegistry#STATUS_READY}. Setiap
 * pembentukan konteks request melewati
 * {@code TenantContextResolver.pastikanTenantDapatDipakai}, yang hanya menerima READY/ACTIVE dan
 * menolak sisanya dengan {@code TENANT_NOT_READY}. Ditambah
 * {@code TenantSchemaLocator.schemaData} yang menolak registry tanpa nama schema pada mode yang
 * menuntut schema, dan {@code pastikanSiap} yang memverifikasi schema-nya benar-benar ada di
 * {@code pg_namespace}, tenant yang provisioning-nya kandas tetap tidak dapat dipakai siapa pun.
 * Ini gagal-tertutup, bukan gagal-terbuka.
 * </p>
 * <p>
 * Efek samping yang tetap perlu diketahui: bila kegagalan terjadi setelah step
 * {@code CREATE_SCHEMA_ERP} berhasil tetapi sebelum registry sempat mencatat nama schema, schema
 * kosong tetap tertinggal di basis data. Ia tidak berbahaya (tidak ada baris registry yang
 * menunjuknya, dan namanya sudah dikunci reservasi milik permohonan yang sama), tetapi retry
 * memang mengandalkan {@code CREATE SCHEMA IF NOT EXISTS} yang idempoten agar dapat memakainya
 * kembali.
 * </p>
 *
 * <h4>Retry, backoff, dan batas percobaan</h4>
 * <p>
 * Saat step melempar exception, {@code TenantProvisioningService.tandaiGagal} berjalan pada
 * transaksi terpisah (transaksi step-nya sendiri sudah di-rollback): step ditandai FAILED,
 * {@link #getAttempt()} job dinaikkan, dan selama attempt masih di bawah konfigurasi
 * {@code pendaftaran_provisioning_max_attempt} (default 3) job dijadwalkan ulang lewat
 * {@link #getRetryAt()}. Setelah batas terlampaui job menjadi {@link #STATUS_FAILED} final dan
 * permohonan ditandai {@code PROVISIONING_FAILED} -- menunggu tindakan admin, bukan mencoba
 * selamanya.
 * </p>
 *
 * <h4>Pesan galat sengaja dipisah dua kolom</h4>
 * <p>
 * {@link #getErrorCode()} untuk mesin/dukungan teknis dan {@link #getErrorMessageSafe()} untuk
 * ditampilkan kepada pendaftar. Pemisahan ini penting pada modul yang mengeksekusi DDL: pesan
 * exception mentah PostgreSQL dapat memuat nama schema/tabel/kolom milik tenant lain atau
 * potongan pernyataan, dan tidak boleh mengalir apa adanya ke halaman status pendaftaran publik.
 * </p>
 *
 * <h4>Catatan Generic CRUD v2</h4>
 * <p>
 * Nama kelas ini mengandung token {@code job}, yang termasuk {@code BLOCKED_CLASS_TOKENS} pada
 * {@code GenericCrudAutoDefinitionFactory}. Artinya entitas ini otomatis ditandai
 * {@code restricted}/{@code READ_ONLY} pada admin model browser dan tidak pernah dapat dimutasi
 * lewat CRUD generik -- kebetulan yang menguntungkan, sebab mengubah {@link #getStatus()} atau
 * {@link #getLockedBy()} dari luar worker dapat membuat dua node mengerjakan job yang sama.
 * </p>
 *
 * @see ProvisioningStep
 * @see TenantRegistry
 * @see PendaftaranTenant
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "provisioning_job")
public class ProvisioningJob extends GeneralValueObject {

	/** Versi serialisasi entitas; tetap {@code 1L} selama bentuk medan tidak berubah. */
	private static final long serialVersionUID = 1L;

	/**
	 * Job menunggu diklaim worker. Status ini pula yang dipakai getter sebagai nilai bawaan saat
	 * kolom kosong -- baris yang datanya tidak lengkap diperlakukan sebagai &quot;belum
	 * dikerjakan&quot;, bukan &quot;sudah berhasil&quot;.
	 */
	public static final String STATUS_QUEUED = "QUEUED";

	/** Job sedang dikerjakan satu worker; lease-nya tercatat pada {@link #getLockedBy()}. */
	public static final String STATUS_RUNNING = "RUNNING";

	/**
	 * Seluruh step pada urutan kanonik berakhir SUCCESS atau SKIPPED. Ditulis
	 * {@code TenantProvisioningService.selesaikanJob} dan bersifat idempoten (job yang sudah
	 * SUCCESS tidak ditulis ulang).
	 */
	public static final String STATUS_SUCCESS = "SUCCESS";

	/**
	 * Job gagal dan batas percobaan sudah terlampaui. Permohonan terkait ditandai
	 * {@code PROVISIONING_FAILED}; pemulihan menjadi urusan admin lewat
	 * {@code PendaftaranTenantAdminService}.
	 */
	public static final String STATUS_FAILED = "FAILED";

	/**
	 * Job dibatalkan (mis. permohonannya dibatalkan). {@code langkah()} memeriksa status ini di
	 * awal setiap step dan berhenti tanpa menandai gagal -- pembatalan bukan galat.
	 */
	public static final String STATUS_CANCELLED = "CANCELLED";

	/** Kunci utama teknis (IDENTITY). */
	private Long id;
	/** Permohonan yang sedang diprovisikan; wajib. */
	private PendaftaranTenant pendaftaranTenant;
	/** Registry hasil provisioning; baru terisi setelah step {@code CREATE_TENANT_REGISTRY}. */
	private TenantRegistry tenant;
	/** Status job: QUEUED/RUNNING/SUCCESS/FAILED/CANCELLED. */
	private String status;
	/** Kode step yang sedang/terakhir dikerjakan, untuk tampilan kemajuan. */
	private String currentStage;
	/** Jumlah percobaan yang sudah dilakukan terhadap job ini. */
	private Integer attempt;
	/** Identitas pemegang lease worker. */
	private String lockedBy;
	/** Saat lease diambil; dasar penentuan lease basi. */
	private Date lockedAt;
	/** Jadwal percobaan berikutnya (backoff). */
	private Date retryAt;
	/** Kode galat stabil untuk dukungan teknis. */
	private String errorCode;
	/** Pesan galat yang aman ditampilkan kepada pendaftar. */
	private String errorMessageSafe;
	/** Saat job pertama kali mulai dikerjakan. */
	private Date startedAt;
	/** Saat job selesai (sukses maupun gagal final). */
	private Date finishedAt;
	/** Saat job diantrekan. */
	private Date createdAt;
	/** Penghitung penguncian optimistik Hibernate ({@code @Version}). */
	private Integer version;

	/** Nama pengguna/proses penulis terakhir (audit shadow); job mengisinya {@code "provisioning"}. */
	private String oleh;
	/** Id pengguna/proses penulis terakhir (audit shadow). */
	private String olehId;
	/**
	 * Cap waktu perubahan terakhir. Callback {@code @PreUpdate} dan deklarasi medan sengaja berada
	 * pada satu baris fisik: itu bentuk baku pola audit shadow di repo ini (disisipkan alat
	 * pembangkit), bukan kelalaian format -- memecahnya akan ditulis kembali oleh penyapu.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public ProvisioningJob() {
	}

	/**
	 * Kunci utama teknis, dibangkitkan basis data ({@code IDENTITY}). Worker menyebarkan id ini
	 * (bukan objek entitasnya) ke {@code jalankanJob}/{@code langkah}, sebab setiap step membuka
	 * Session sendiri dan objek dari Session lain tidak boleh dipakai lintas transaksi.
	 *
	 * @return id job, atau {@code null} bila belum disimpan
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
	 * Permohonan pendaftaran yang diprovisikan job ini (wajib, {@code nullable = false}). Dari
	 * sinilah step mengambil {@code normalizedUsername} (calon slug/nama schema), pilihan jenis
	 * usaha, snapshot paket/trial, dan akun {@code Pendaftar} pemilik.
	 *
	 * <p>Getter memanggil {@code check(...)} milik {@code GeneralValueObject}: proxy Hibernate yang
	 * tidak dapat lagi di-inisialisasi dijadikan {@code null} alih-alih melempar
	 * {@code LazyInitializationException}, dan hasilnya ditulis balik ke medan.</p>
	 *
	 * @return permohonan pendaftaran, atau {@code null} bila relasinya tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftaran_tenant_id", nullable = false)
	public PendaftaranTenant getPendaftaranTenant() {
		pendaftaranTenant = check(pendaftaranTenant);
		return pendaftaranTenant;
	}

	/**
	 * Menetapkan permohonan yang diprovisikan job ini.
	 *
	 * @param pendaftaranTenant permohonan pendaftaran tenant
	 */
	public void setPendaftaranTenant(PendaftaranTenant pendaftaranTenant) {
		this.pendaftaranTenant = pendaftaranTenant;
	}

	/**
	 * Registry tenant hasil provisioning. Sengaja {@code nullable}: saat job diantrekan, tenantnya
	 * belum ada -- baris {@link TenantRegistry} baru dibuat step
	 * {@code CREATE_TENANT_REGISTRY}, yang lalu menautkannya ke sini. Step-step sesudahnya mencari
	 * registry lewat tiga jalur ({@code job.tenant}, {@code permohonan.tenantRegistry}, atau kueri
	 * berdasarkan slug) supaya pemulihan tetap mungkin bila salah satu tautan belum tertulis.
	 *
	 * <p>Sama seperti relasi lain, getter ini menetralkan proxy lewat {@code check(...)}.</p>
	 *
	 * @return registry tenant, atau {@code null} bila belum terbentuk/tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tenant_id", nullable = true)
	public TenantRegistry getTenant() {
		tenant = check(tenant);
		return tenant;
	}

	/**
	 * Menetapkan registry tenant hasil provisioning.
	 *
	 * @param tenant baris registry yang dibuat step {@code CREATE_TENANT_REGISTRY}
	 */
	public void setTenant(TenantRegistry tenant) {
		this.tenant = tenant;
	}

	/**
	 * Status job. Nilai kosong dibaca sebagai {@link #STATUS_QUEUED} -- default yang aman: baris
	 * rusak akan dicoba dikerjakan worker, bukan dianggap sudah berhasil sehingga permohonan
	 * menggantung tanpa tenant.
	 *
	 * @return salah satu konstanta {@code STATUS_*}; minimal {@link #STATUS_QUEUED}
	 */
	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_QUEUED : status;
	}

	/**
	 * Menetapkan status job.
	 *
	 * @param status salah satu konstanta {@code STATUS_*}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Kode step yang sedang dikerjakan (diperbarui {@code langkah()} sebelum step dijalankan).
	 * Murni informatif -- kemajuan yang sesungguhnya ditentukan status per baris
	 * {@link ProvisioningStep}, bukan kolom ini. Halaman status pendaftaran memakainya untuk
	 * menampilkan tahap berjalan.
	 *
	 * @return kode step terakhir yang dimulai, atau {@code null}
	 */
	@Column(name = "current_stage", length = 64)
	public String getCurrentStage() {
		return currentStage;
	}

	/**
	 * Menetapkan kode step yang sedang dikerjakan.
	 *
	 * @param currentStage salah satu konstanta {@code ProvisioningStep.STEP_*}
	 */
	public void setCurrentStage(String currentStage) {
		this.currentStage = currentStage;
	}

	/**
	 * Jumlah percobaan job ini. Getter mengembalikan {@code 0} bila kolom kosong sehingga
	 * pemanggil dapat menaikkannya tanpa memeriksa {@code null} lebih dulu; nilai default itu
	 * tidak ditulis balik ke medan. Dibandingkan dengan konfigurasi
	 * {@code pendaftaran_provisioning_max_attempt} (default 3) untuk memutuskan retry atau
	 * {@link #STATUS_FAILED} final.
	 *
	 * @return jumlah percobaan, minimal {@code 0}
	 */
	@Column(name = "attempt")
	public Integer getAttempt() {
		return attempt == null ? Integer.valueOf(0) : attempt;
	}

	/**
	 * Menetapkan jumlah percobaan.
	 *
	 * @param attempt jumlah percobaan yang sudah dilakukan
	 */
	public void setAttempt(Integer attempt) {
		this.attempt = attempt;
	}

	/**
	 * Identitas node/thread pemegang lease (hostname+thread) -- lease basi (lockedAt tua) boleh diambil alih.
	 *
	 * <p>Lease bukan sekadar catatan: tanpa {@code SKIP LOCKED} (yang belum tersedia di
	 * PostgreSQL 9.3), klaim job dilakukan dengan {@code SELECT ... FOR UPDATE} lalu menuliskan
	 * pemegangnya ke kolom ini. Node lain yang membaca baris ber-lease segar akan melewatinya,
	 * sehingga satu job tidak dikerjakan dua node sekaligus -- penting, sebab dua worker yang
	 * menjalankan step DDL yang sama pada saat bersamaan akan saling menabrak.</p>
	 *
	 * @return identitas pemegang lease, atau {@code null} bila job tidak sedang dipegang
	 */
	@Column(name = "locked_by", length = 128)
	public String getLockedBy() {
		return lockedBy;
	}

	/**
	 * Menetapkan pemegang lease; dikosongkan kembali ({@code null}) saat job selesai.
	 *
	 * @param lockedBy identitas node/thread
	 */
	public void setLockedBy(String lockedBy) {
		this.lockedBy = lockedBy;
	}

	/**
	 * Saat lease diambil. Menjadi dasar penentuan lease basi: worker yang mati mendadak
	 * meninggalkan {@link #getLockedBy()} terisi selamanya, dan tanpa batas waktu job itu tidak
	 * akan pernah dikerjakan lagi. Karena itu lease dengan {@code lockedAt} yang cukup tua boleh
	 * diambil alih node lain.
	 *
	 * @return cap waktu pengambilan lease, atau {@code null}
	 */
	@Column(name = "locked_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getLockedAt() {
		return lockedAt;
	}

	/**
	 * Menetapkan cap waktu pengambilan lease.
	 *
	 * @param lockedAt cap waktu klaim
	 */
	public void setLockedAt(Date lockedAt) {
		this.lockedAt = lockedAt;
	}

	/**
	 * Jadwal percobaan berikutnya sesudah kegagalan (backoff). Worker hanya mengambil job yang
	 * {@code retryAt}-nya sudah lewat, sehingga kegagalan yang berulang cepat (mis. basis data
	 * sedang tidak sehat) tidak berubah menjadi putaran sibuk.
	 *
	 * @return jadwal percobaan berikutnya, atau {@code null} bila job belum pernah gagal
	 */
	@Column(name = "retry_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getRetryAt() {
		return retryAt;
	}

	/**
	 * Menetapkan jadwal percobaan berikutnya.
	 *
	 * @param retryAt cap waktu boleh dicoba lagi
	 */
	public void setRetryAt(Date retryAt) {
		this.retryAt = retryAt;
	}

	/**
	 * Kode galat stabil (bukan pesan bebas) untuk keperluan dukungan/pengujian. Dipisahkan dari
	 * {@link #getErrorMessageSafe()} supaya klien dapat membedakan jenis kegagalan tanpa harus
	 * mencocokkan teks.
	 *
	 * @return kode galat, atau {@code null} bila job belum pernah gagal
	 */
	@Column(name = "error_code", length = 64)
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Menetapkan kode galat.
	 *
	 * @param errorCode kode galat stabil
	 */
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * Pesan galat yang <b>aman ditampilkan</b> kepada pendaftar. Namanya bukan hiasan: modul ini
	 * mengeksekusi DDL, dan pesan exception mentah dari PostgreSQL dapat memuat nama
	 * schema/tabel/kolom serta potongan pernyataan yang tidak layak dibocorkan ke halaman status
	 * pendaftaran publik. Rincian teknis lengkap tetap tercatat lewat
	 * {@code ais.common.ErrorAuditUtil.record}.
	 *
	 * @return pesan galat ringkas untuk pengguna, atau {@code null}
	 */
	@Column(name = "error_message_safe", length = 500)
	public String getErrorMessageSafe() {
		return errorMessageSafe;
	}

	/**
	 * Menetapkan pesan galat yang aman ditampilkan. Jangan mengisinya dengan
	 * {@code exception.getMessage()} apa adanya.
	 *
	 * @param errorMessageSafe pesan ringkas tanpa detail internal
	 */
	public void setErrorMessageSafe(String errorMessageSafe) {
		this.errorMessageSafe = errorMessageSafe;
	}

	/**
	 * Saat job pertama kali mulai dikerjakan worker.
	 *
	 * @return cap waktu mulai, atau {@code null} bila masih QUEUED
	 */
	@Column(name = "started_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getStartedAt() {
		return startedAt;
	}

	/**
	 * Menetapkan cap waktu mulai.
	 *
	 * @param startedAt cap waktu mulai dikerjakan
	 */
	public void setStartedAt(Date startedAt) {
		this.startedAt = startedAt;
	}

	/**
	 * Saat job berakhir, baik SUCCESS maupun FAILED final.
	 *
	 * @return cap waktu selesai, atau {@code null} bila belum berakhir
	 */
	@Column(name = "finished_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getFinishedAt() {
		return finishedAt;
	}

	/**
	 * Menetapkan cap waktu selesai.
	 *
	 * @param finishedAt cap waktu berakhirnya job
	 */
	public void setFinishedAt(Date finishedAt) {
		this.finishedAt = finishedAt;
	}

	/**
	 * Saat job diantrekan (biasanya segera setelah email pendaftar terverifikasi). Selisihnya
	 * dengan {@link #getStartedAt()} menunjukkan lamanya antrean, yang berguna saat menakar apakah
	 * worker cukup sering berjalan.
	 *
	 * @return cap waktu pembuatan job
	 */
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Menetapkan cap waktu pembuatan job.
	 *
	 * @param createdAt cap waktu diantrekan
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Penghitung penguncian optimistik Hibernate. Pada entitas ini perannya nyata: klaim lease,
	 * pembaruan {@code currentStage} tiap step, dan penandaan gagal berjalan pada transaksi yang
	 * berbeda-beda, sehingga penulis yang bekerja atas salinan usang harus gagal, bukan menimpa.
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
	 * Nama pengguna/proses penulis terakhir (pola audit shadow). Diisi {@code "provisioning"} oleh
	 * job, sebab tidak ada pengguna interaktif di balik pekerjaan latar ini.
	 *
	 * @return nama penulis terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama penulis terakhir. Nilai {@code null}/kosong sengaja diabaikan (return lebih
	 * awal) supaya form atau proses yang tidak mengisinya tidak menghapus jejak penulis
	 * sebelumnya.
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
	 * Menetapkan cap waktu perubahan terakhir; umumnya diurus callback {@code @PreUpdate}, bukan
	 * kode aplikasi.
	 *
	 * @param tanggal_dirubah cap waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
