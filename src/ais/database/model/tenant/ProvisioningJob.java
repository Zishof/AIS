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
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "provisioning_job")
public class ProvisioningJob extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_QUEUED = "QUEUED";
	public static final String STATUS_RUNNING = "RUNNING";
	public static final String STATUS_SUCCESS = "SUCCESS";
	public static final String STATUS_FAILED = "FAILED";
	public static final String STATUS_CANCELLED = "CANCELLED";

	private Long id;
	private PendaftaranTenant pendaftaranTenant;
	private TenantRegistry tenant;
	private String status;
	private String currentStage;
	private Integer attempt;
	private String lockedBy;
	private Date lockedAt;
	private Date retryAt;
	private String errorCode;
	private String errorMessageSafe;
	private Date startedAt;
	private Date finishedAt;
	private Date createdAt;
	private Integer version;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public ProvisioningJob() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftaran_tenant_id", nullable = false)
	public PendaftaranTenant getPendaftaranTenant() {
		return pendaftaranTenant;
	}

	public void setPendaftaranTenant(PendaftaranTenant pendaftaranTenant) {
		this.pendaftaranTenant = pendaftaranTenant;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tenant_id", nullable = true)
	public TenantRegistry getTenant() {
		return tenant;
	}

	public void setTenant(TenantRegistry tenant) {
		this.tenant = tenant;
	}

	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_QUEUED : status;
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

	@Column(name = "attempt")
	public Integer getAttempt() {
		return attempt == null ? Integer.valueOf(0) : attempt;
	}

	public void setAttempt(Integer attempt) {
		this.attempt = attempt;
	}

	/** Identitas node/thread pemegang lease (hostname+thread) -- lease basi (lockedAt tua) boleh diambil alih. */
	@Column(name = "locked_by", length = 128)
	public String getLockedBy() {
		return lockedBy;
	}

	public void setLockedBy(String lockedBy) {
		this.lockedBy = lockedBy;
	}

	@Column(name = "locked_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getLockedAt() {
		return lockedAt;
	}

	public void setLockedAt(Date lockedAt) {
		this.lockedAt = lockedAt;
	}

	@Column(name = "retry_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getRetryAt() {
		return retryAt;
	}

	public void setRetryAt(Date retryAt) {
		this.retryAt = retryAt;
	}

	@Column(name = "error_code", length = 64)
	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	@Column(name = "error_message_safe", length = 500)
	public String getErrorMessageSafe() {
		return errorMessageSafe;
	}

	public void setErrorMessageSafe(String errorMessageSafe) {
		this.errorMessageSafe = errorMessageSafe;
	}

	@Column(name = "started_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Date startedAt) {
		this.startedAt = startedAt;
	}

	@Column(name = "finished_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(Date finishedAt) {
		this.finishedAt = finishedAt;
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
