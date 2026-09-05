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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Satu langkah provisioning dalam {@link ProvisioningJob} (idempoten per step).</h3>
 *
 * <p>Step code kanonik lihat konstanta {@code STEP_*} (urutan &sect;7.9 dokumen master). Retry job
 * memeriksa status per-step: step SUCCESS dilewati, step gagal diulang -- TIDAK mengulang
 * pekerjaan yang sudah beres (schema/owner/seed tidak pernah dobel). Pada mode LEGACY,
 * step schema/migrasi ditandai {@link #STATUS_SKIPPED} secara sah.</p>
 *
 * <h4>Mengapa idempotensi di sini adalah soal keamanan, bukan kerapian</h4>
 * <p>
 * {@code TenantProvisioningService.langkah} membuka transaksi terpisah untuk tiap step, sehingga
 * kegagalan di tengah meninggalkan sebagian pekerjaan ter-commit. Tanpa catatan per-step, retry
 * berarti mengulang seluruh urutan dari awal: membuat registry kedua, menyemai entitlement ganda,
 * atau membuat keanggotaan pemilik dua kali. Baris kelas inilah yang mencegahnya -- sebelum
 * menjalankan sebuah step, {@code langkah()} mencari baris {@code (job, stepCode)}; bila statusnya
 * sudah {@link #STATUS_SUCCESS} atau {@link #STATUS_SKIPPED}, step langsung dianggap beres.
 * Constraint {@code UNIQUE(job_id, step_code)} pada tabel menjamin pencarian itu tidak pernah
 * bertemu dua baris untuk step yang sama, bahkan bila dua worker sempat berlomba.
 * </p>
 *
 * <h4>SKIPPED bukan kegagalan yang disamarkan</h4>
 * <p>
 * Beberapa step memang tidak berlaku pada konfigurasi tertentu. Pada mode
 * {@link TenantRegistry#MODE_LEGACY}, step pembuatan schema, migrasi, pemasangan audit, dan
 * verifikasi schema tidak relevan karena data tenant tetap tinggal di schema existing; step
 * {@code CREATE_OWNER_USER} selalu dilewati karena akun pemilik memakai {@code Pendaftar} yang
 * sudah ada (menyalin hash PBKDF2 ke jalur kredensial lama dilarang). Logika step menyatakan hal
 * itu lewat pengecualian internal {@code LewatiStep}, dan hasilnya ditandai
 * {@link #STATUS_SKIPPED} -- berbeda tegas dari {@link #STATUS_FAILED}. Perbedaan ini yang
 * membuat catatan provisioning tetap jujur: pembaca dapat melihat step mana yang memang tidak
 * dijalankan, bukan menebak-nebak dari ketiadaan baris.
 * </p>
 * <p>
 * Sebaliknya, {@code stepCode} yang tidak dikenali sama sekali melempar
 * {@code IllegalStateException} -- gagal-tertutup, bukan diam-diam dianggap berhasil.
 * </p>
 *
 * <h4>Kolom yang sengaja tidak ada: version</h4>
 * <p>
 * Berbeda dari {@link ProvisioningJob}, {@link TenantRegistry}, dan
 * {@link TenantModuleEntitlement}, entitas ini tidak memiliki kolom {@code @Version}. Itu
 * konsisten dengan cara pakainya: baris step hanya disentuh worker pemegang lease job-nya, di
 * dalam transaksi step yang bersangkutan, sehingga tidak ada dua penulis interaktif yang perlu
 * dijaga penguncian optimistik. Pelindungnya adalah lease job ditambah constraint unik
 * {@code (job_id, step_code)}.
 * </p>
 *
 * @see ProvisioningJob
 * @see TenantRegistry
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "provisioning_step",
		uniqueConstraints = @UniqueConstraint(columnNames = { "job_id", "step_code" }))
public class ProvisioningStep extends GeneralValueObject {

	/** Versi serialisasi entitas; tetap {@code 1L} selama bentuk medan tidak berubah. */
	private static final long serialVersionUID = 1L;

	/**
	 * Step 1 -- memastikan status permohonan masih salah satu status antrean/provisioning/gagal
	 * yang sah, lalu menaikkannya ke {@code PROVISIONING}. Gerbang pertama yang mencegah
	 * permohonan yang sudah dibatalkan/ditolak ikut diprovisikan.
	 */
	public static final String STEP_VALIDATE_REGISTRATION = "VALIDATE_REGISTRATION";

	/**
	 * Step 2 -- memastikan {@link SchemaNameReservation} milik permohonan ini ada dan berstatus
	 * RESERVED/CONSUMED. Tanpa reservasi yang sah, provisioning berhenti: nama schema tidak boleh
	 * diambil tanpa penguncian yang memenangkan lomba dua pendaftar bernama sama.
	 */
	public static final String STEP_RESERVE_USERNAME = "RESERVE_USERNAME";

	/**
	 * Step 3 -- membuat baris {@link TenantRegistry} (bila belum ada), kode publik
	 * {@code TEN-&lt;tahun&gt;-&lt;id&gt;}, serta subdomain bawaan {@code &lt;slug&gt;.&lt;base&gt;}
	 * pada {@link TenantDomain}. Idempoten: registry yang sudah ada dipakai kembali.
	 */
	public static final String STEP_CREATE_TENANT_REGISTRY = "CREATE_TENANT_REGISTRY";

	/**
	 * Step 4 -- {@code CREATE SCHEMA IF NOT EXISTS "&lt;slug&gt;"}. Nama schema divalidasi ulang
	 * {@code TenantSchemaService.pastikanAman} (pola {@code ^[a-z][a-z0-9_]{2,30}$} + daftar
	 * reserved) lalu dikutip ganda. Dilewati pada mode LEGACY.
	 */
	public static final String STEP_CREATE_SCHEMA_ERP = "CREATE_SCHEMA_ERP";

	/**
	 * Step 5 -- memastikan schema audit {@code &lt;slug&gt;__audit} ada, lalu mencatat kedua nama
	 * schema pada registry. Pencatatan sengaja dilakukan di step ini, bukan step sebelumnya:
	 * registry baru mengaku punya schema setelah pasangan data+audit lengkap. Dilewati pada LEGACY.
	 */
	public static final String STEP_CREATE_SCHEMA_AUDIT = "CREATE_SCHEMA_AUDIT";

	/**
	 * Step 6 -- menerapkan migrasi kanonik ber-riwayat+checksum ke schema data
	 * ({@code TenantSchemaService.terapkanMigrasi}, target ERP). Definisi migrasi yang berubah
	 * setelah terpasang menyebabkan galat, bukan penerapan diam-diam. Dilewati pada LEGACY.
	 */
	public static final String STEP_RUN_MIGRATIONS = "RUN_MIGRATIONS";

	/** Step 7 -- migrasi kanonik untuk schema audit (target AUDIT). Dilewati pada LEGACY. */
	public static final String STEP_INSTALL_AUDIT = "INSTALL_AUDIT";

	/** Step 8 -- menetapkan locale dan zona waktu tenant (diambil dari profil pendaftar bila ada). */
	public static final String STEP_SEED_CONFIGURATION = "SEED_CONFIGURATION";

	/**
	 * Step 9 -- menyemai {@link TenantModuleEntitlement} bawaan dari jenis usaha yang dipilih
	 * pendaftar ({@code TenantEntitlementService.terapkanDariJenisUsaha}).
	 */
	public static final String STEP_SEED_MODULES = "SEED_MODULES";

	/**
	 * Step 10 -- menyemai peran bawaan ke {@code role_tenant} pada schema tenant
	 * (HYBRID/TENANT_ONLY). Pada LEGACY dilewati dengan catatan bahwa peran pemilik cukup melalui
	 * {@code tenant_membership.role_code}; peran global {@code Tbmrole} sengaja TIDAK disentuh,
	 * sebab peran tenant tidak boleh mengubah hak platform.
	 */
	public static final String STEP_SEED_ROLES = "SEED_ROLES";

	/**
	 * Step 11 -- SELALU dilewati. Akun login pemilik memakai {@code Pendaftar} yang sudah ada
	 * (hash PBKDF2); membuat {@code Tbmuser} baru berarti menyalin kredensial ke jalur hash lama
	 * yang lebih lemah, dan itu dilarang.
	 */
	public static final String STEP_CREATE_OWNER_USER = "CREATE_OWNER_USER";

	/**
	 * Step 12 -- membuat {@code TenantMembership} pemilik. Idempoten lewat pemeriksaan
	 * {@code count} lebih dulu, sehingga retry tidak menghasilkan dua keanggotaan.
	 */
	public static final String STEP_CREATE_MEMBERSHIP = "CREATE_MEMBERSHIP";

	/**
	 * Step 13 -- tidak membuat baris apa pun; hanya mencatat snapshot paket/trial pada metadata.
	 * Tanggal trial sesungguhnya dihitung saat {@link #STEP_MARK_READY} (invariant #7).
	 */
	public static final String STEP_CREATE_SUBSCRIPTION_TRIAL = "CREATE_SUBSCRIPTION_TRIAL";

	/**
	 * Step 14 -- verifikasi menyeluruh: schema ada, tabel wajib ada, riwayat migrasi cocok
	 * ({@code TenantSchemaService.verifikasiLengkap}). Dilewati pada LEGACY.
	 */
	public static final String STEP_VERIFY_SCHEMA = "VERIFY_SCHEMA";

	/**
	 * Step 15 -- memastikan akun pemilik benar-benar memiliki hash kredensial. Tenant yang siap
	 * dipakai tetapi pemiliknya tidak dapat masuk adalah kegagalan, bukan keberhasilan.
	 */
	public static final String STEP_VERIFY_LOGIN = "VERIFY_LOGIN";

	/**
	 * Step 16 (terakhir) -- menaikkan tenant dan permohonan menjadi READY, menghitung jendela
	 * trial dari saat itu, menandai reservasi username CONSUMED, mengaktifkan akun pendaftar bila
	 * ini tenant pertamanya, dan mencatat audit {@code TENANT_READY}. Sebelum step ini sukses,
	 * tenant tidak dapat dipakai request mana pun.
	 */
	public static final String STEP_MARK_READY = "MARK_READY";

	/**
	 * Step tercatat tetapi belum dijalankan. Sekaligus nilai bawaan getter status untuk kolom
	 * kosong -- baris yang datanya tidak lengkap akan dikerjakan ulang, bukan dianggap selesai.
	 */
	public static final String STATUS_PENDING = "PENDING";

	/**
	 * Step sedang dijalankan. Perhatikan bahwa status ini TIDAK membuat step dilewati saat retry:
	 * hanya SUCCESS dan SKIPPED yang dilewati, sehingga step yang tertinggal RUNNING karena worker
	 * mati akan dijalankan ulang.
	 */
	public static final String STATUS_RUNNING = "RUNNING";

	/** Step berhasil; pada percobaan berikutnya dilewati tanpa dijalankan ulang. */
	public static final String STATUS_SUCCESS = "SUCCESS";

	/**
	 * Step gagal. Job-nya ditandai gagal pula dan dijadwalkan retry selama batas percobaan belum
	 * terlampaui; step ini akan dijalankan lagi, sedangkan step yang sudah SUCCESS tidak.
	 */
	public static final String STATUS_FAILED = "FAILED";

	/**
	 * Step tidak berlaku pada konfigurasi ini (mis. step schema pada mode LEGACY, atau
	 * {@link #STEP_CREATE_OWNER_USER} yang memang selalu dilewati). Diperlakukan sama seperti
	 * SUCCESS oleh mekanisme retry, tetapi dicatat berbeda supaya riwayat provisioning tetap jujur.
	 */
	public static final String STATUS_SKIPPED = "SKIPPED";

	/**
	 * Step sudah dibatalkan efeknya (kompensasi) -- disediakan untuk pemulihan bergaya saga.
	 * Belum ada penulis di kode saat ini; nilainya tercatat agar kode status tetap stabil bila
	 * kompensasi otomatis kelak ditambahkan.
	 */
	public static final String STATUS_COMPENSATED = "COMPENSATED";

	/** Kunci utama teknis (IDENTITY). */
	private Long id;
	/** Job pemilik step ini; wajib, dan bersama {@link #stepCode} membentuk kunci unik. */
	private ProvisioningJob job;
	/** Kode step kanonik; salah satu konstanta {@code STEP_*}. */
	private String stepCode;
	/** Status step: PENDING/RUNNING/SUCCESS/FAILED/SKIPPED/COMPENSATED. */
	private String status;
	/** Jumlah percobaan step ini (dinaikkan setiap kali step dimulai). */
	private Integer attempt;
	/** Checksum artefak yang dijalankan step (migrasi/seed) sebagai bukti idempotensi. */
	private String checksum;
	/** Saat percobaan terakhir step ini dimulai. */
	private Date startedAt;
	/** Saat percobaan terakhir step ini berakhir. */
	private Date finishedAt;
	/** Kode galat stabil bila step gagal. */
	private String errorCode;
	/** Pesan galat yang aman ditampilkan bila step gagal. */
	private String errorMessageSafe;
	/** Ringkasan hasil step dalam bentuk JSON ({@code {"info": ...}}). */
	private String metadataJson;

	/** Nama pengguna/proses penulis terakhir (audit shadow); diisi {@code "provisioning"}. */
	private String oleh;
	/** Id pengguna/proses penulis terakhir (audit shadow). */
	private String olehId;
	/**
	 * Cap waktu perubahan terakhir. Gabungan callback {@code @PreUpdate} dan deklarasi medan pada
	 * satu baris fisik adalah bentuk baku pola audit shadow repo ini (disisipkan alat pembangkit),
	 * bukan kelalaian format.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public ProvisioningStep() {
	}

	/**
	 * Kunci utama teknis, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * @return id baris step, atau {@code null} bila belum disimpan
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
	 * Job pemilik step ini (wajib). Bersama {@link #getStepCode()} membentuk constraint unik
	 * {@code (job_id, step_code)} yang menjamin satu step hanya punya satu baris riwayat per job --
	 * pondasi mekanisme &quot;lewati step yang sudah beres&quot;.
	 *
	 * <p>Getter memanggil {@code check(...)} milik {@code GeneralValueObject}, yang menetralkan
	 * proxy Hibernate yang tidak dapat lagi di-inisialisasi menjadi {@code null} (dan menulis
	 * balik hasilnya ke medan) alih-alih melempar {@code LazyInitializationException}.</p>
	 *
	 * @return job pemilik, atau {@code null} bila relasinya tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "job_id", nullable = false)
	public ProvisioningJob getJob() {
		job = check(job);
		return job;
	}

	/**
	 * Menetapkan job pemilik step ini.
	 *
	 * @param job job provisioning induk
	 */
	public void setJob(ProvisioningJob job) {
		this.job = job;
	}

	/**
	 * Kode step kanonik, salah satu konstanta {@code STEP_*}. Nilainya dipakai
	 * {@code TenantProvisioningService.logikaUntuk} untuk memilih logika yang dijalankan; kode
	 * yang tidak dikenali melempar {@code IllegalStateException} alih-alih dianggap berhasil.
	 *
	 * @return kode step, tidak pernah {@code null} pada baris tersimpan
	 */
	@Column(name = "step_code", nullable = false, length = 64)
	public String getStepCode() {
		return stepCode;
	}

	/**
	 * Menetapkan kode step.
	 *
	 * @param stepCode salah satu konstanta {@code STEP_*}
	 */
	public void setStepCode(String stepCode) {
		this.stepCode = stepCode;
	}

	/**
	 * Status step. Kolom kosong dibaca {@link #STATUS_PENDING} -- default aman: step yang catatannya
	 * tidak lengkap akan dijalankan ulang, bukan dilewati seolah sudah berhasil. Hanya
	 * {@link #STATUS_SUCCESS} dan {@link #STATUS_SKIPPED} yang membuat step dilewati saat retry.
	 *
	 * @return salah satu konstanta {@code STATUS_*}; minimal {@link #STATUS_PENDING}
	 */
	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_PENDING : status;
	}

	/**
	 * Menetapkan status step.
	 *
	 * @param status salah satu konstanta {@code STATUS_*}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Jumlah percobaan step ini; dinaikkan setiap kali step dimulai. Getter mengembalikan
	 * {@code 0} untuk kolom kosong (tanpa menulis balik ke medan) supaya pemanggil dapat langsung
	 * menaikkannya. Berguna saat menelusuri step mana yang berulang kali gagal.
	 *
	 * @return jumlah percobaan, minimal {@code 0}
	 */
	@Column(name = "attempt")
	public Integer getAttempt() {
		return attempt == null ? Integer.valueOf(0) : attempt;
	}

	/**
	 * Menetapkan jumlah percobaan step.
	 *
	 * @param attempt jumlah percobaan
	 */
	public void setAttempt(Integer attempt) {
		this.attempt = attempt;
	}

	/**
	 * Checksum/versi artefak yang dijalankan step ini (migration/seed) -- bukti idempotensi.
	 *
	 * <p>Checksum yang sama pada percobaan berikutnya berarti artefak yang dijalankan benar-benar
	 * identik. Pemeriksaan yang sesungguhnya menolak perubahan diam-diam ada pada riwayat migrasi
	 * per-schema ({@code tenant_schema_migration}), yang melempar galat bila definisi kanonik
	 * berubah setelah terpasang; kolom ini melengkapinya dari sisi riwayat job.</p>
	 *
	 * @return checksum artefak, atau {@code null} untuk step yang tidak memiliki artefak
	 */
	@Column(name = "checksum", length = 128)
	public String getChecksum() {
		return checksum;
	}

	/**
	 * Menetapkan checksum artefak step.
	 *
	 * @param checksum checksum migrasi/seed yang dijalankan
	 */
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	/**
	 * Saat percobaan terakhir step ini dimulai. Ditimpa setiap percobaan baru -- yang tersimpan
	 * adalah percobaan terakhir, bukan riwayat lengkap semua percobaan.
	 *
	 * @return cap waktu mulai, atau {@code null} bila step belum pernah dijalankan
	 */
	@Column(name = "started_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getStartedAt() {
		return startedAt;
	}

	/**
	 * Menetapkan cap waktu mulai step.
	 *
	 * @param startedAt cap waktu mulai
	 */
	public void setStartedAt(Date startedAt) {
		this.startedAt = startedAt;
	}

	/**
	 * Saat percobaan terakhir step ini berakhir (SUCCESS maupun SKIPPED). Selisihnya dengan
	 * {@link #getStartedAt()} adalah durasi step -- ukuran praktis untuk menemukan step migrasi
	 * yang mahal.
	 *
	 * @return cap waktu selesai, atau {@code null} bila belum berakhir
	 */
	@Column(name = "finished_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getFinishedAt() {
		return finishedAt;
	}

	/**
	 * Menetapkan cap waktu selesai step.
	 *
	 * @param finishedAt cap waktu selesai
	 */
	public void setFinishedAt(Date finishedAt) {
		this.finishedAt = finishedAt;
	}

	/**
	 * Kode galat stabil bila step gagal; dipisahkan dari pesannya supaya dukungan teknis dapat
	 * mengenali jenis kegagalan tanpa mencocokkan teks bebas.
	 *
	 * @return kode galat, atau {@code null} bila step tidak gagal
	 */
	@Column(name = "error_code", length = 64)
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Menetapkan kode galat step.
	 *
	 * @param errorCode kode galat stabil
	 */
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * Pesan galat yang aman ditampilkan kepada pendaftar. Sama seperti pada
	 * {@link ProvisioningJob#getErrorMessageSafe()}, kolom ini TIDAK boleh diisi pesan exception
	 * PostgreSQL mentah: step di sini menjalankan DDL, dan pesannya dapat memuat nama
	 * schema/tabel/kolom serta potongan pernyataan. Rincian teknis lengkap tetap tercatat lewat
	 * {@code ais.common.ErrorAuditUtil.record}.
	 *
	 * @return pesan galat ringkas, atau {@code null}
	 */
	@Column(name = "error_message_safe", length = 500)
	public String getErrorMessageSafe() {
		return errorMessageSafe;
	}

	/**
	 * Menetapkan pesan galat yang aman ditampilkan.
	 *
	 * @param errorMessageSafe pesan ringkas tanpa detail internal
	 */
	public void setErrorMessageSafe(String errorMessageSafe) {
		this.errorMessageSafe = errorMessageSafe;
	}

	/**
	 * Ringkasan hasil step dalam bentuk JSON, diisi {@code TenantProvisioningService.langkah}
	 * sebagai {@code {"info": "..."}} dari nilai kembalian logika step (mis. &quot;registry #12
	 * slug abadi_jaya&quot;, &quot;ERP applied=9 skipped=0&quot;, atau alasan step dilewati).
	 * Kolomnya bertipe {@code text} sehingga tidak ada batas panjang praktis.
	 *
	 * <p>Karena isinya turut memuat nama slug/schema, perlakukan sebagai informasi operasional:
	 * layak dilihat admin platform, tetapi bukan bahan yang dilempar mentah ke halaman publik.</p>
	 *
	 * @return metadata JSON, atau {@code null} bila step tidak menghasilkan ringkasan
	 */
	@Column(name = "metadata_json", columnDefinition = "text")
	public String getMetadataJson() {
		return metadataJson;
	}

	/**
	 * Menetapkan metadata ringkas hasil step.
	 *
	 * @param metadataJson dokumen JSON ringkas
	 */
	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
	}

	/**
	 * Nama pengguna/proses penulis terakhir (pola audit shadow); diisi {@code "provisioning"} sebab
	 * penulisnya pekerjaan latar, bukan pengguna interaktif.
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
