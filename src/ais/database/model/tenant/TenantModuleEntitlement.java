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
 * <h3>Module entitlement per tenant (entitlement ≠ permission -- invariant #13 ERD).</h3>
 *
 * <p>Union modul dari jenis usaha terpilih ({@code source=BUSINESS_TYPE}) + paket
 * ({@code SUBSCRIPTION_PLAN}) + add-on/override/trial. Entitlement menentukan MODUL apa
 * tersedia bagi tenant; IZIN tindakan pengguna tetap urusan role
 * ({@code Tbmrole.ebisnis_menu} dsb.). Modul yang source-nya belum operasional tercatat
 * dgn status {@link #STATUS_PLANNED} -- UI wajib jujur, bukan tombol semu.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tenant_module_entitlement",
		uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "module_code", "source" }))
public class TenantModuleEntitlement extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String SOURCE_BUSINESS_TYPE = "BUSINESS_TYPE";
	public static final String SOURCE_SUBSCRIPTION_PLAN = "SUBSCRIPTION_PLAN";
	public static final String SOURCE_ADD_ON = "ADD_ON";
	public static final String SOURCE_ADMIN_OVERRIDE = "ADMIN_OVERRIDE";
	public static final String SOURCE_TRIAL = "TRIAL";

	public static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_DISABLED = "DISABLED";
	/** Modul di-entitle tapi implementasi source-nya belum tersedia/operasional. */
	public static final String STATUS_PLANNED = "PLANNED";

	private Long id;
	private TenantRegistry tenant;
	private String moduleCode;
	private String source;
	private String status;
	private Date effectiveFrom;
	private Date effectiveUntil;
	private Long limitValue;
	private JenisUsahaTenant selectedJenisUsaha;
	private String planVersion;
	private Date createdAt;
	private Integer version;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public TenantModuleEntitlement() {
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
	@JoinColumn(name = "tenant_id", nullable = false)
	public TenantRegistry getTenant() {
		return tenant;
	}

	public void setTenant(TenantRegistry tenant) {
		this.tenant = tenant;
	}

	@Column(name = "module_code", nullable = false, length = 64)
	public String getModuleCode() {
		return moduleCode;
	}

	public void setModuleCode(String moduleCode) {
		this.moduleCode = moduleCode;
	}

	@Column(name = "source", nullable = false, length = 40)
	public String getSource() {
		return source == null || source.trim().isEmpty() ? SOURCE_BUSINESS_TYPE : source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_ACTIVE : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "effective_from")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getEffectiveFrom() {
		return effectiveFrom;
	}

	public void setEffectiveFrom(Date effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	@Column(name = "effective_until")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getEffectiveUntil() {
		return effectiveUntil;
	}

	public void setEffectiveUntil(Date effectiveUntil) {
		this.effectiveUntil = effectiveUntil;
	}

	/** Limit numerik opsional (mis. jumlah mesin POS utk billing per-device). */
	@Column(name = "limit_value")
	public Long getLimitValue() {
		return limitValue;
	}

	public void setLimitValue(Long limitValue) {
		this.limitValue = limitValue;
	}

	/** Jenis usaha asal entitlement ini (audit trail source=BUSINESS_TYPE). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_usaha_tenant_id", nullable = true)
	public JenisUsahaTenant getSelectedJenisUsaha() {
		return selectedJenisUsaha;
	}

	public void setSelectedJenisUsaha(JenisUsahaTenant selectedJenisUsaha) {
		this.selectedJenisUsaha = selectedJenisUsaha;
	}

	@Column(name = "plan_version", length = 64)
	public String getPlanVersion() {
		return planVersion;
	}

	public void setPlanVersion(String planVersion) {
		this.planVersion = planVersion;
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
