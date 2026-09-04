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
 * <h3>Registry tenant yang sudah diprovision -- scope data & identitas workspace.</h3>
 *
 * <p>{@link #getSlug()} = username tenant publik (immutable setelah ACTIVE);
 * {@link #getSchemaName()}/{@link #getAuditSchemaName()} HANYA ditulis
 * {@code TenantProvisioningService} dari reservation -- TIDAK PERNAH dari request
 * (invariant #3 ERD). Pada mode {@code LEGACY} kolom schema dibiarkan null (data tenant
 * tetap di schema existing, scope per-Pendaftar seperti sekarang).</p>
 *
 * <p>Trial: {@code trialStartAt = readyAt}, {@code trialEndAt = readyAt + trialDaysSnapshot}
 * (mulai saat READY, BUKAN saat form dibuka -- invariant #7 ERD).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tenant_registry")
public class TenantRegistry extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_PROVISIONING = "PROVISIONING";
	public static final String STATUS_READY = "READY";
	public static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_SUSPENDED = "SUSPENDED";

	public static final String MODE_LEGACY = "LEGACY";
	public static final String MODE_HYBRID = "HYBRID";
	public static final String MODE_TENANT_ONLY = "TENANT_ONLY";

	private Long id;
	private String code;
	private String nama;
	private String slug;
	private Pendaftar ownerPendaftar;
	private String status;
	private String tenantMode;
	private String schemaName;
	private String auditSchemaName;
	private String schemaVersion;
	private String defaultLocale;
	private String timezone;
	private Date trialStartAt;
	private Date trialEndAt;
	private Date activatedAt;
	private Date suspendedAt;
	private Date createdAt;
	private Integer version;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public TenantRegistry() {
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

	/** Kode publik tenant (mis. {@code TEN-2026-000012}) -- aman ditampilkan/di-filter admin. */
	@Column(name = "code", unique = true, length = 40)
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return nama == null ? null : nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	/** Username/slug tenant global-unique, IMMUTABLE setelah tenant aktif. */
	@Column(name = "slug", unique = true, nullable = false, length = 64)
	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_pendaftar_id", nullable = false)
	public Pendaftar getOwnerPendaftar() {
		ownerPendaftar = check(ownerPendaftar);
		return ownerPendaftar;
	}

	public void setOwnerPendaftar(Pendaftar ownerPendaftar) {
		this.ownerPendaftar = ownerPendaftar;
	}

	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_PROVISIONING : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "tenant_mode", nullable = false, length = 20)
	public String getTenantMode() {
		return tenantMode == null || tenantMode.trim().isEmpty() ? MODE_LEGACY : tenantMode;
	}

	public void setTenantMode(String tenantMode) {
		this.tenantMode = tenantMode;
	}

	/** Nama schema ERP ({@code <slug>}). Null pada mode LEGACY. Hanya ditulis provisioning service. */
	@Column(name = "schema_name", unique = true, length = 64)
	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	/** Nama schema audit ({@code <slug>__audit}). Null pada mode LEGACY. */
	@Column(name = "audit_schema_name", unique = true, length = 70)
	public String getAuditSchemaName() {
		return auditSchemaName;
	}

	public void setAuditSchemaName(String auditSchemaName) {
		this.auditSchemaName = auditSchemaName;
	}

	@Column(name = "schema_version", length = 40)
	public String getSchemaVersion() {
		return schemaVersion;
	}

	public void setSchemaVersion(String schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	@Column(name = "default_locale", length = 20)
	public String getDefaultLocale() {
		return defaultLocale == null || defaultLocale.trim().isEmpty() ? "id_ID" : defaultLocale;
	}

	public void setDefaultLocale(String defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	@Column(name = "timezone", length = 64)
	public String getTimezone() {
		return timezone == null || timezone.trim().isEmpty() ? "Asia/Jakarta" : timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	@Column(name = "trial_start_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTrialStartAt() {
		return trialStartAt;
	}

	public void setTrialStartAt(Date trialStartAt) {
		this.trialStartAt = trialStartAt;
	}

	@Column(name = "trial_end_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTrialEndAt() {
		return trialEndAt;
	}

	public void setTrialEndAt(Date trialEndAt) {
		this.trialEndAt = trialEndAt;
	}

	@Column(name = "activated_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getActivatedAt() {
		return activatedAt;
	}

	public void setActivatedAt(Date activatedAt) {
		this.activatedAt = activatedAt;
	}

	@Column(name = "suspended_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSuspendedAt() {
		return suspendedAt;
	}

	public void setSuspendedAt(Date suspendedAt) {
		this.suspendedAt = suspendedAt;
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
