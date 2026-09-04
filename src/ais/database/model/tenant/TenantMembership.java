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
import ais.database.model.Pendaftar;
import ais.database.model.Tbmuser;

/**
 * <h3>Keanggotaan satu {@link Pendaftar} pada satu tenant (owner/anggota).</h3>
 *
 * <p>Memisahkan registration account (Pendaftar), tenant namespace ({@link TenantRegistry}),
 * dan tenant membership (tabel ini) -- satu Pendaftar bisa punya beberapa tenant, tenant
 * switcher membaca daftar membership AKTIF (invariant #14 ERD). {@code tbmuser} nullable:
 * diisi HANYA bila owner diberi akun aplikasi ZK penuh lewat adapter yang aman (bukan
 * menyalin hash PBKDF2 ke jalur DES).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tenant_membership",
		uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "pendaftar_id" }))
public class TenantMembership extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_SUSPENDED = "SUSPENDED";
	public static final String STATUS_REVOKED = "REVOKED";

	public static final String ROLE_OWNER = "OWNER";

	private Long id;
	private TenantRegistry tenant;
	private Pendaftar pendaftar;
	private Tbmuser tbmuser;
	private String status;
	private Boolean isOwner;
	private String roleCode;
	private Date validFrom;
	private Date validUntil;
	private Date createdAt;
	private Integer version;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public TenantMembership() {
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
		tenant = check(tenant);
		return tenant;
	}

	public void setTenant(TenantRegistry tenant) {
		this.tenant = tenant;
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
	@JoinColumn(name = "tbmuser_id", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_ACTIVE : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "is_owner")
	public Boolean getIsOwner() {
		return isOwner == null ? Boolean.FALSE : isOwner;
	}

	public void setIsOwner(Boolean isOwner) {
		this.isOwner = isOwner;
	}

	@Column(name = "role_code", length = 64)
	public String getRoleCode() {
		return roleCode == null || roleCode.trim().isEmpty() ? ROLE_OWNER : roleCode;
	}

	public void setRoleCode(String roleCode) {
		this.roleCode = roleCode;
	}

	@Column(name = "valid_from")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	@Column(name = "valid_until")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getValidUntil() {
		return validUntil;
	}

	public void setValidUntil(Date validUntil) {
		this.validUntil = validUntil;
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
