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
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftaran_tenant")
public class PendaftaranTenant extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	// -- Workflow status utama (08. WORKFLOW STATUS dokumen master) --
	public static final String STATUS_DRAFT = "DRAFT";
	public static final String STATUS_SUBMITTED = "SUBMITTED";
	public static final String STATUS_EMAIL_VERIFICATION_PENDING = "EMAIL_VERIFICATION_PENDING";
	public static final String STATUS_VERIFIED = "VERIFIED";
	public static final String STATUS_REVIEW_PENDING = "REVIEW_PENDING";
	public static final String STATUS_PROVISIONING_QUEUED = "PROVISIONING_QUEUED";
	public static final String STATUS_PROVISIONING = "PROVISIONING";
	public static final String STATUS_READY = "READY";
	public static final String STATUS_ACTIVE = "ACTIVE";
	// -- Exception path --
	public static final String STATUS_REJECTED = "REJECTED";
	public static final String STATUS_PROVISIONING_FAILED = "PROVISIONING_FAILED";
	public static final String STATUS_SUSPENDED = "SUSPENDED";
	public static final String STATUS_CANCELLED = "CANCELLED";
	public static final String STATUS_EXPIRED = "EXPIRED";

	private Long id;
	private String registrationCode;
	private Pendaftar pendaftar;
	private TenantRegistry tenantRegistry;
	private String desiredUsername;
	private String normalizedUsername;
	private String requestedDomain;
	private String status;
	private String currentStage;
	private String selectedPlanVersion;
	private Integer trialDaysSnapshot;
	private Date submittedAt;
	private Date verifiedAt;
	private Date approvedAt;
	private Date readyAt;
	private Date rejectedAt;
	private String failureCode;
	private String failureMessageSafe;
	private String idempotencyKey;
	private String requestId;
	private String sourceIpHash;
	private String userAgent;
	private String registrationSource;
	private Date createdAt;
	private Integer version;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PendaftaranTenant() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/** Nomor publik pendaftaran, format {@code REG-<tahun>-<id-padded>} -- aman ditampilkan. */
	@Column(name = "registration_code", unique = true, length = 40)
	public String getRegistrationCode() {
		return registrationCode;
	}

	public void setRegistrationCode(String registrationCode) {
		this.registrationCode = registrationCode;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar_id", nullable = false)
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tenant_registry_id", nullable = true)
	public TenantRegistry getTenantRegistry() {
		tenantRegistry = check(tenantRegistry);
		return tenantRegistry;
	}

	public void setTenantRegistry(TenantRegistry tenantRegistry) {
		this.tenantRegistry = tenantRegistry;
	}

	@Column(name = "desired_username", length = 64)
	public String getDesiredUsername() {
		return desiredUsername;
	}

	public void setDesiredUsername(String desiredUsername) {
		this.desiredUsername = desiredUsername;
	}

	/** Hasil normalisasi {@code ^[a-z][a-z0-9_]{2,30}$} -- kunci benturan global (reservation). */
	@Column(name = "normalized_username", length = 64)
	public String getNormalizedUsername() {
		return normalizedUsername;
	}

	public void setNormalizedUsername(String normalizedUsername) {
		this.normalizedUsername = normalizedUsername;
	}

	@Column(name = "requested_domain", length = 255)
	public String getRequestedDomain() {
		return requestedDomain;
	}

	public void setRequestedDomain(String requestedDomain) {
		this.requestedDomain = requestedDomain;
	}

	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_DRAFT : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "current_stage", length = 64)
	public String getCurrentStage() {
		return currentStage;
	}

	public void setCurrentStage(String currentStage) {
		this.currentStage = currentStage;
	}

	/** Snapshot versi paket terpilih saat submit (teks kode; harga TIDAK di-hard-code di sini). */
	@Column(name = "selected_plan_version", length = 64)
	public String getSelectedPlanVersion() {
		return selectedPlanVersion;
	}

	public void setSelectedPlanVersion(String selectedPlanVersion) {
		this.selectedPlanVersion = selectedPlanVersion;
	}

	/** Snapshot lama trial (hari) saat submit -- perubahan konfigurasi platform kemudian tidak mengubah ini. */
	@Column(name = "trial_days_snapshot")
	public Integer getTrialDaysSnapshot() {
		return trialDaysSnapshot;
	}

	public void setTrialDaysSnapshot(Integer trialDaysSnapshot) {
		this.trialDaysSnapshot = trialDaysSnapshot;
	}

	@Column(name = "submitted_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(Date submittedAt) {
		this.submittedAt = submittedAt;
	}

	@Column(name = "verified_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getVerifiedAt() {
		return verifiedAt;
	}

	public void setVerifiedAt(Date verifiedAt) {
		this.verifiedAt = verifiedAt;
	}

	@Column(name = "approved_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getApprovedAt() {
		return approvedAt;
	}

	public void setApprovedAt(Date approvedAt) {
		this.approvedAt = approvedAt;
	}

	@Column(name = "ready_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getReadyAt() {
		return readyAt;
	}

	public void setReadyAt(Date readyAt) {
		this.readyAt = readyAt;
	}

	@Column(name = "rejected_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getRejectedAt() {
		return rejectedAt;
	}

	public void setRejectedAt(Date rejectedAt) {
		this.rejectedAt = rejectedAt;
	}

	@Column(name = "failure_code", length = 64)
	public String getFailureCode() {
		return failureCode;
	}

	public void setFailureCode(String failureCode) {
		this.failureCode = failureCode;
	}

	/** Pesan gagal yang AMAN ditampilkan publik (tanpa stack trace/nama schema/detail internal). */
	@Column(name = "failure_message_safe", length = 500)
	public String getFailureMessageSafe() {
		return failureMessageSafe;
	}

	public void setFailureMessageSafe(String failureMessageSafe) {
		this.failureMessageSafe = failureMessageSafe;
	}

	@Column(name = "idempotency_key", unique = true, length = 100)
	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}

	@Column(name = "request_id", length = 64)
	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	/** Hash SHA-256 hex dari IP sumber (privasi: IP mentah tidak disimpan di baris permohonan). */
	@Column(name = "source_ip_hash", length = 64)
	public String getSourceIpHash() {
		return sourceIpHash;
	}

	public void setSourceIpHash(String sourceIpHash) {
		this.sourceIpHash = sourceIpHash;
	}

	@Column(name = "user_agent", length = 500)
	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	@Column(name = "registration_source", length = 40)
	public String getRegistrationSource() {
		return registrationSource;
	}

	public void setRegistrationSource(String registrationSource) {
		this.registrationSource = registrationSource;
	}

	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Version
	@Column(name = "version")
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
