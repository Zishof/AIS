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
 * <h3>Mapping jenis usaha → module code (database-driven, BUKAN switch besar di servlet/JSP).</h3>
 *
 * <p>Satu baris = satu module yang direkomendasikan/diwajibkan utk satu {@link JenisUsahaTenant}.
 * Dipakai (1) wizard utk menampilkan "module bundle yang akan diaktifkan" per kartu jenis usaha,
 * (2) provisioning utk membentuk union {@code TenantModuleEntitlement} (source BUSINESS_TYPE).
 * Entitlement ≠ permission: baris di sini TIDAK memberi izin tindakan pengguna apa pun.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jenis_usaha_tenant_module",
		uniqueConstraints = @UniqueConstraint(columnNames = { "jenis_usaha_tenant_id", "module_code" }))
public class JenisUsahaTenantModule extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private JenisUsahaTenant jenisUsahaTenant;
	private String moduleCode;
	private Boolean defaultEnabled;
	private Boolean required;
	private Integer displayOrder;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public JenisUsahaTenantModule() {
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
	@JoinColumn(name = "jenis_usaha_tenant_id", nullable = false)
	public JenisUsahaTenant getJenisUsahaTenant() {
		jenisUsahaTenant = check(jenisUsahaTenant);
		return jenisUsahaTenant;
	}

	public void setJenisUsahaTenant(JenisUsahaTenant jenisUsahaTenant) {
		this.jenisUsahaTenant = jenisUsahaTenant;
	}

	/** Kode modul stabil huruf besar (mis. {@code POS}, {@code PERSEDIAAN}, {@code KAS_JURNAL}). */
	@Column(name = "module_code", nullable = false, length = 64)
	public String getModuleCode() {
		return moduleCode;
	}

	public void setModuleCode(String moduleCode) {
		this.moduleCode = moduleCode;
	}

	@Column(name = "default_enabled")
	public Boolean getDefaultEnabled() {
		return defaultEnabled == null ? Boolean.TRUE : defaultEnabled;
	}

	public void setDefaultEnabled(Boolean defaultEnabled) {
		this.defaultEnabled = defaultEnabled;
	}

	@Column(name = "required")
	public Boolean getRequired() {
		return required == null ? Boolean.FALSE : required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	@Column(name = "display_order")
	public Integer getDisplayOrder() {
		return displayOrder == null ? Integer.valueOf(0) : displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
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
