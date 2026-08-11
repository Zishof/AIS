package ais.database.model.tenant;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Audit event BISNIS alur pendaftaran/provisioning tenant (§18 dokumen master).</h3>
 *
 * <p>Berbeda dari {@code public.error_log} (exception) dan dari Envers (riwayat baris) --
 * tabel ini mencatat KEJADIAN alur: {@code REGISTRATION_SUBMITTED}, {@code EMAIL_VERIFIED},
 * {@code TENANT_READY}, dst. FK memakai id polos (bukan relasi) supaya insert audit tidak
 * pernah menggagalkan transaksi utama karena lazy-loading/cascade. Password/OTP/token
 * TIDAK PERNAH masuk ke sini.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftaran_audit_event")
public class PendaftaranAuditEvent extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String EV_REGISTRATION_FORM_OPENED = "REGISTRATION_FORM_OPENED";
	public static final String EV_USERNAME_CHECKED = "USERNAME_CHECKED";
	public static final String EV_REGISTRATION_SUBMITTED = "REGISTRATION_SUBMITTED";
	public static final String EV_PENDAFTAR_CREATED = "PENDAFTAR_CREATED";
	public static final String EV_BUSINESS_TYPES_SELECTED = "BUSINESS_TYPES_SELECTED";
	public static final String EV_CONSENT_ACCEPTED = "CONSENT_ACCEPTED";
	public static final String EV_EMAIL_VERIFICATION_SENT = "EMAIL_VERIFICATION_SENT";
	public static final String EV_EMAIL_VERIFIED = "EMAIL_VERIFIED";
	public static final String EV_TENANT_PROVISIONING_QUEUED = "TENANT_PROVISIONING_QUEUED";
	public static final String EV_TENANT_SCHEMA_CREATED = "TENANT_SCHEMA_CREATED";
	public static final String EV_TENANT_MIGRATION_APPLIED = "TENANT_MIGRATION_APPLIED";
	public static final String EV_TENANT_SEEDED = "TENANT_SEEDED";
	public static final String EV_OWNER_CREATED = "OWNER_CREATED";
	public static final String EV_TENANT_READY = "TENANT_READY";
	public static final String EV_TENANT_ACTIVATED = "TENANT_ACTIVATED";
	public static final String EV_PROVISIONING_FAILED = "PROVISIONING_FAILED";
	public static final String EV_PROVISIONING_RETRIED = "PROVISIONING_RETRIED";
	public static final String EV_REGISTRATION_REJECTED = "REGISTRATION_REJECTED";
	public static final String EV_REGISTRATION_CANCELLED = "REGISTRATION_CANCELLED";
	public static final String EV_SECURITY_BLOCKED = "SECURITY_BLOCKED";

	public static final String ACTOR_PUBLIC = "PUBLIC";
	public static final String ACTOR_PENDAFTAR = "PENDAFTAR";
	public static final String ACTOR_ADMIN = "ADMIN";
	public static final String ACTOR_SYSTEM = "SYSTEM";

	private Long id;
	private String eventCode;
	private String actorType;
	private Long pendaftarId;
	private Long registrationId;
	private Long tenantId;
	private String requestId;
	private String correlationId;
	private String sourceIpHash;
	private String userAgent;
	private String detailJson;
	private String reason;
	private String result;
	private Date waktu;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PendaftaranAuditEvent() {
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

	@Column(name = "event_code", nullable = false, length = 64)
	public String getEventCode() {
		return eventCode;
	}

	public void setEventCode(String eventCode) {
		this.eventCode = eventCode;
	}

	@Column(name = "actor_type", length = 20)
	public String getActorType() {
		return actorType == null || actorType.trim().isEmpty() ? ACTOR_SYSTEM : actorType;
	}

	public void setActorType(String actorType) {
		this.actorType = actorType;
	}

	@Column(name = "pendaftar_id")
	public Long getPendaftarId() {
		return pendaftarId;
	}

	public void setPendaftarId(Long pendaftarId) {
		this.pendaftarId = pendaftarId;
	}

	@Column(name = "registration_id")
	public Long getRegistrationId() {
		return registrationId;
	}

	public void setRegistrationId(Long registrationId) {
		this.registrationId = registrationId;
	}

	@Column(name = "tenant_id")
	public Long getTenantId() {
		return tenantId;
	}

	public void setTenantId(Long tenantId) {
		this.tenantId = tenantId;
	}

	@Column(name = "request_id", length = 64)
	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	@Column(name = "correlation_id", length = 64)
	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	@Column(name = "source_ip_hash", length = 64)
	public String getSourceIpHash() {
		return sourceIpHash;
	}

	public void setSourceIpHash(String sourceIpHash) {
		this.sourceIpHash = sourceIpHash;
	}

	@Column(name = "user_agent", length = 500)
	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/** Before/after non-sensitif dalam JSON kecil (TANPA password/OTP/token/hash). */
	@Column(name = "detail_json", columnDefinition = "text")
	public String getDetailJson() {
		return detailJson;
	}

	public void setDetailJson(String detailJson) {
		this.detailJson = detailJson;
	}

	@Column(name = "reason", length = 500)
	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Column(name = "result", length = 40)
	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	@Column(name = "waktu", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
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
