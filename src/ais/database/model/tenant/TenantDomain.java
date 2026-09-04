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
 * <h3>Domain/subdomain milik satu tenant ({@code normalized_domain} unique global).</h3>
 *
 * <p>Subdomain bawaan ({@code <slug>.ebisnis.id}) dibuat provisioning; custom domain
 * diverifikasi SETELAH tenant aktif (token hash -- token mentah tidak disimpan).
 * {@code Pendaftar.domain} existing tetap menyimpan slug kompatibilitas tenant PERTAMA;
 * tenant/domain tambahan hidup di tabel ini.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tenant_domain")
public class TenantDomain extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String TYPE_SUBDOMAIN = "SUBDOMAIN";
	public static final String TYPE_CUSTOM = "CUSTOM";

	public static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";
	public static final String STATUS_DISABLED = "DISABLED";

	private Long id;
	private TenantRegistry tenant;
	private String domain;
	private String normalizedDomain;
	private String type;
	private String status;
	private String verificationTokenHash;
	private Date verifiedAt;
	private Boolean primaryDomain;
	private Date createdAt;
	private Integer version;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public TenantDomain() {
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

	@Column(name = "domain", nullable = false, length = 255)
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	/** lowercase + punycode (bila custom IDN) -- kunci unique global. */
	@Column(name = "normalized_domain", unique = true, nullable = false, length = 255)
	public String getNormalizedDomain() {
		return normalizedDomain;
	}

	public void setNormalizedDomain(String normalizedDomain) {
		this.normalizedDomain = normalizedDomain;
	}

	@Column(name = "type", nullable = false, length = 20)
	public String getType() {
		return type == null || type.trim().isEmpty() ? TYPE_SUBDOMAIN : type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_PENDING_VERIFICATION : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "verification_token_hash", length = 64)
	public String getVerificationTokenHash() {
		return verificationTokenHash;
	}

	public void setVerificationTokenHash(String verificationTokenHash) {
		this.verificationTokenHash = verificationTokenHash;
	}

	@Column(name = "verified_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getVerifiedAt() {
		return verifiedAt;
	}

	public void setVerifiedAt(Date verifiedAt) {
		this.verifiedAt = verifiedAt;
	}

	@Column(name = "primary_domain")
	public Boolean getPrimaryDomain() {
		return primaryDomain == null ? Boolean.FALSE : primaryDomain;
	}

	public void setPrimaryDomain(Boolean primaryDomain) {
		this.primaryDomain = primaryDomain;
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
