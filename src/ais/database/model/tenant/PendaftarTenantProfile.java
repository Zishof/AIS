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
 * <h3>Extension profile 1:1 di atas {@link Pendaftar} (control-plane pendaftaran tenant).</h3>
 *
 * <p>SENGAJA tabel extension, BUKAN kolom baru di {@code public.pendaftar}: (1) `Pendaftar`
 * juga dipakai eCampus/eSchool -- jangan membebani entity legacy; (2) menambah kolom ke entity
 * {@code @Audited} existing berisiko INSERT audit gagal krn `hbm2ddl=update` TIDAK menambah
 * kolom ke `new_audit.pendaftar__audit` (lihat peringatan src/hibernate.cfg.xml:41-48);
 * (3) unique `normalized_email` dapat ditegakkan HANYA utk akun self-service tanpa
 * membentur email legacy yang duplikat/kosong secara historis.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftar_tenant_profile")
public class PendaftarTenantProfile extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";
	public static final String STATUS_ACTIVE = "ACTIVE";
	public static final String STATUS_SUSPENDED = "SUSPENDED";

	private Long id;
	private Pendaftar pendaftar;
	private String normalizedEmail;
	private String ownerDisplayName;
	private String legalName;
	private String tradeName;
	private String legalForm;
	private String nib;
	private String npwp;
	private String website;
	private String postalCode;
	private String timezone;
	private String preferredLocale;
	private String registrationSource;
	private String accountStatus;
	private String passwordAlgorithm;
	private Integer passwordVersion;
	private Integer passwordIterations;
	private Boolean mustChangePassword;
	private Date emailVerifiedAt;
	private Date lastLoginAt;
	private Date createdAt;
	private Integer version;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PendaftarTenantProfile() {
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
	@JoinColumn(name = "pendaftar_id", nullable = false, unique = true)
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	/** Email login ter-normalisasi (trim+lowercase). Unique HANYA di tabel ini (akun self-service),
	 *  TIDAK memaksakan unique ke seluruh {@code pendaftar.email} legacy tanpa cleansing. */
	@Column(name = "normalized_email", unique = true, nullable = false, length = 255)
	public String getNormalizedEmail() {
		return normalizedEmail;
	}

	public void setNormalizedEmail(String normalizedEmail) {
		this.normalizedEmail = normalizedEmail;
	}

	@Column(name = "owner_display_name", length = 255)
	public String getOwnerDisplayName() {
		return ownerDisplayName;
	}

	public void setOwnerDisplayName(String ownerDisplayName) {
		this.ownerDisplayName = ownerDisplayName;
	}

	@Column(name = "legal_name", length = 255)
	public String getLegalName() {
		return legalName;
	}

	public void setLegalName(String legalName) {
		this.legalName = legalName;
	}

	@Column(name = "trade_name", length = 255)
	public String getTradeName() {
		return tradeName;
	}

	public void setTradeName(String tradeName) {
		this.tradeName = tradeName;
	}

	@Column(name = "legal_form", length = 100)
	public String getLegalForm() {
		return legalForm;
	}

	public void setLegalForm(String legalForm) {
		this.legalForm = legalForm;
	}

	@Column(name = "nib", length = 50)
	public String getNib() {
		return nib;
	}

	public void setNib(String nib) {
		this.nib = nib;
	}

	@Column(name = "npwp", length = 50)
	public String getNpwp() {
		return npwp;
	}

	public void setNpwp(String npwp) {
		this.npwp = npwp;
	}

	@Column(name = "website", length = 255)
	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	@Column(name = "postal_code", length = 20)
	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	@Column(name = "timezone", length = 64)
	public String getTimezone() {
		return timezone == null || timezone.trim().isEmpty() ? "Asia/Jakarta" : timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	@Column(name = "preferred_locale", length = 20)
	public String getPreferredLocale() {
		return preferredLocale == null || preferredLocale.trim().isEmpty() ? "id_ID" : preferredLocale;
	}

	public void setPreferredLocale(String preferredLocale) {
		this.preferredLocale = preferredLocale;
	}

	@Column(name = "registration_source", length = 40)
	public String getRegistrationSource() {
		return registrationSource;
	}

	public void setRegistrationSource(String registrationSource) {
		this.registrationSource = registrationSource;
	}

	@Column(name = "account_status", length = 40)
	public String getAccountStatus() {
		return accountStatus == null || accountStatus.trim().isEmpty() ? STATUS_PENDING_VERIFICATION : accountStatus;
	}

	public void setAccountStatus(String accountStatus) {
		this.accountStatus = accountStatus;
	}

	/** Format hash versioned (mis. {@code PBKDF2WithHmacSHA256}); hash+salt sendiri tetap di kolom
	 *  {@code pendaftar.password_hash/password_salt} existing (tidak dipindah -- kompatibel login lama). */
	@Column(name = "password_algorithm", length = 64)
	public String getPasswordAlgorithm() {
		return passwordAlgorithm;
	}

	public void setPasswordAlgorithm(String passwordAlgorithm) {
		this.passwordAlgorithm = passwordAlgorithm;
	}

	@Column(name = "password_version")
	public Integer getPasswordVersion() {
		return passwordVersion;
	}

	public void setPasswordVersion(Integer passwordVersion) {
		this.passwordVersion = passwordVersion;
	}

	@Column(name = "password_iterations")
	public Integer getPasswordIterations() {
		return passwordIterations;
	}

	public void setPasswordIterations(Integer passwordIterations) {
		this.passwordIterations = passwordIterations;
	}

	@Column(name = "must_change_password")
	public Boolean getMustChangePassword() {
		return mustChangePassword == null ? Boolean.FALSE : mustChangePassword;
	}

	public void setMustChangePassword(Boolean mustChangePassword) {
		this.mustChangePassword = mustChangePassword;
	}

	@Column(name = "email_verified_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getEmailVerifiedAt() {
		return emailVerifiedAt;
	}

	public void setEmailVerifiedAt(Date emailVerifiedAt) {
		this.emailVerifiedAt = emailVerifiedAt;
	}

	@Column(name = "last_login_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(Date lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
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
