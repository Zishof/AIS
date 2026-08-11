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
 * <h3>Tantangan verifikasi email/OTP satu permohonan tenant.</h3>
 *
 * <p>HANYA hash token/OTP yang disimpan (SHA-256 hex) -- token mentah dikirim lewat email dan
 * tidak pernah masuk database/log/audit (invariant #8 ERD). Token baru meng-invalidate token
 * lama (status {@link #STATUS_SUPERSEDED}); expiry + attempt count + resend rate-limit
 * ditegakkan {@code EmailVerificationService}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftaran_email_verification")
public class PendaftaranEmailVerification extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String CHANNEL_EMAIL = "EMAIL";

	public static final String STATUS_PENDING = "PENDING";
	public static final String STATUS_CONSUMED = "CONSUMED";
	public static final String STATUS_EXPIRED = "EXPIRED";
	public static final String STATUS_SUPERSEDED = "SUPERSEDED";

	private Long id;
	private PendaftaranTenant pendaftaranTenant;
	private String channel;
	private String destinationNormalized;
	private String tokenHash;
	private String otpHash;
	private String status;
	private Integer attemptCount;
	private Date sentAt;
	private Date expiresAt;
	private Date consumedAt;
	private Date createdAt;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PendaftaranEmailVerification() {
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

	@Column(name = "channel", nullable = false, length = 20)
	public String getChannel() {
		return channel == null || channel.trim().isEmpty() ? CHANNEL_EMAIL : channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	/** Email tujuan ter-normalisasi (utk audit resend/rate-limit; bukan rahasia). */
	@Column(name = "destination_normalized", nullable = false, length = 255)
	public String getDestinationNormalized() {
		return destinationNormalized;
	}

	public void setDestinationNormalized(String destinationNormalized) {
		this.destinationNormalized = destinationNormalized;
	}

	@Column(name = "token_hash", length = 64)
	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	@Column(name = "otp_hash", length = 64)
	public String getOtpHash() {
		return otpHash;
	}

	public void setOtpHash(String otpHash) {
		this.otpHash = otpHash;
	}

	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_PENDING : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "attempt_count")
	public Integer getAttemptCount() {
		return attemptCount == null ? Integer.valueOf(0) : attemptCount;
	}

	public void setAttemptCount(Integer attemptCount) {
		this.attemptCount = attemptCount;
	}

	@Column(name = "sent_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSentAt() {
		return sentAt;
	}

	public void setSentAt(Date sentAt) {
		this.sentAt = sentAt;
	}

	@Column(name = "expires_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Date expiresAt) {
		this.expiresAt = expiresAt;
	}

	@Column(name = "consumed_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getConsumedAt() {
		return consumedAt;
	}

	public void setConsumedAt(Date consumedAt) {
		this.consumedAt = consumedAt;
	}

	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
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
