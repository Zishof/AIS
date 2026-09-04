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
 * <p>Step code kanonik lihat konstanta {@code STEP_*} (urutan §7.9 dokumen master). Retry job
 * memeriksa status per-step: step SUCCESS dilewati, step gagal diulang -- TIDAK mengulang
 * pekerjaan yang sudah beres (schema/owner/seed tidak pernah dobel). Pada mode LEGACY,
 * step schema/migrasi ditandai {@link #STATUS_SKIPPED} secara sah.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "provisioning_step",
		uniqueConstraints = @UniqueConstraint(columnNames = { "job_id", "step_code" }))
public class ProvisioningStep extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STEP_VALIDATE_REGISTRATION = "VALIDATE_REGISTRATION";
	public static final String STEP_RESERVE_USERNAME = "RESERVE_USERNAME";
	public static final String STEP_CREATE_TENANT_REGISTRY = "CREATE_TENANT_REGISTRY";
	public static final String STEP_CREATE_SCHEMA_ERP = "CREATE_SCHEMA_ERP";
	public static final String STEP_CREATE_SCHEMA_AUDIT = "CREATE_SCHEMA_AUDIT";
	public static final String STEP_RUN_MIGRATIONS = "RUN_MIGRATIONS";
	public static final String STEP_INSTALL_AUDIT = "INSTALL_AUDIT";
	public static final String STEP_SEED_CONFIGURATION = "SEED_CONFIGURATION";
	public static final String STEP_SEED_MODULES = "SEED_MODULES";
	public static final String STEP_SEED_ROLES = "SEED_ROLES";
	public static final String STEP_CREATE_OWNER_USER = "CREATE_OWNER_USER";
	public static final String STEP_CREATE_MEMBERSHIP = "CREATE_MEMBERSHIP";
	public static final String STEP_CREATE_SUBSCRIPTION_TRIAL = "CREATE_SUBSCRIPTION_TRIAL";
	public static final String STEP_VERIFY_SCHEMA = "VERIFY_SCHEMA";
	public static final String STEP_VERIFY_LOGIN = "VERIFY_LOGIN";
	public static final String STEP_MARK_READY = "MARK_READY";

	public static final String STATUS_PENDING = "PENDING";
	public static final String STATUS_RUNNING = "RUNNING";
	public static final String STATUS_SUCCESS = "SUCCESS";
	public static final String STATUS_FAILED = "FAILED";
	public static final String STATUS_SKIPPED = "SKIPPED";
	public static final String STATUS_COMPENSATED = "COMPENSATED";

	private Long id;
	private ProvisioningJob job;
	private String stepCode;
	private String status;
	private Integer attempt;
	private String checksum;
	private Date startedAt;
	private Date finishedAt;
	private String errorCode;
	private String errorMessageSafe;
	private String metadataJson;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public ProvisioningStep() {
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
	@JoinColumn(name = "job_id", nullable = false)
	public ProvisioningJob getJob() {
		job = check(job);
		return job;
	}

	public void setJob(ProvisioningJob job) {
		this.job = job;
	}

	@Column(name = "step_code", nullable = false, length = 64)
	public String getStepCode() {
		return stepCode;
	}

	public void setStepCode(String stepCode) {
		this.stepCode = stepCode;
	}

	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_PENDING : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "attempt")
	public Integer getAttempt() {
		return attempt == null ? Integer.valueOf(0) : attempt;
	}

	public void setAttempt(Integer attempt) {
		this.attempt = attempt;
	}

	/** Checksum/versi artefak yang dijalankan step ini (migration/seed) -- bukti idempotensi. */
	@Column(name = "checksum", length = 128)
	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
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

	@Column(name = "metadata_json", columnDefinition = "text")
	public String getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(String metadataJson) {
		this.metadataJson = metadataJson;
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
