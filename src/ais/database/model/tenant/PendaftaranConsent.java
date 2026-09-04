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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Bukti persetujuan (consent) per dokumen per permohonan tenant.</h3>
 *
 * <p>Checkbox saja tidak cukup (§14.5 dokumen master): disimpan versi dokumen yang disetujui,
 * waktu, IP sumber, user-agent, locale, dan channel. Submit ditolak bila versi dokumen yang
 * dicentang ≠ versi terpublikasi saat submit.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftaran_consent")
public class PendaftaranConsent extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String TYPE_TERMS = "TERMS";
	public static final String TYPE_PRIVACY = "PRIVACY";
	public static final String TYPE_MARKETING = "MARKETING";

	private Long id;
	private PendaftaranTenant pendaftaranTenant;
	private String consentType;
	private String documentVersion;
	private Boolean accepted;
	private Date acceptedAt;
	private String sourceIp;
	private String userAgent;
	private String locale;
	private String channel;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PendaftaranConsent() {
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
		pendaftaranTenant = check(pendaftaranTenant);
		return pendaftaranTenant;
	}

	public void setPendaftaranTenant(PendaftaranTenant pendaftaranTenant) {
		this.pendaftaranTenant = pendaftaranTenant;
	}

	@Column(name = "consent_type", nullable = false, length = 40)
	public String getConsentType() {
		return consentType;
	}

	public void setConsentType(String consentType) {
		this.consentType = consentType;
	}

	@Column(name = "document_version", nullable = false, length = 40)
	public String getDocumentVersion() {
		return documentVersion;
	}

	public void setDocumentVersion(String documentVersion) {
		this.documentVersion = documentVersion;
	}

	@Column(name = "accepted")
	public Boolean getAccepted() {
		return accepted == null ? Boolean.FALSE : accepted;
	}

	public void setAccepted(Boolean accepted) {
		this.accepted = accepted;
	}

	@Column(name = "accepted_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getAcceptedAt() {
		return acceptedAt;
	}

	public void setAcceptedAt(Date acceptedAt) {
		this.acceptedAt = acceptedAt;
	}

	@Column(name = "source_ip", length = 64)
	public String getSourceIp() {
		return sourceIp;
	}

	public void setSourceIp(String sourceIp) {
		this.sourceIp = sourceIp;
	}

	@Column(name = "user_agent", length = 500)
	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	@Column(name = "locale", length = 20)
	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	@Column(name = "channel", length = 40)
	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
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
