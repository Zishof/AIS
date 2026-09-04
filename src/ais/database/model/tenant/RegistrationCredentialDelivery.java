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
 * <h3>Bukti penyerahan kredensial generated (TANPA menyimpan plaintext apa pun).</h3>
 *
 * <p>Mencatat KAPAN dan lewat KANAL apa kredensial generated diserahkan/diakui pemiliknya
 * (invariant #12 ERD: hanya ditampilkan sekali; tidak pernah bisa dilihat ulang). Baris di
 * sini adalah bukti audit, bukan tempat penyimpanan kredensial.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "registration_credential_delivery")
public class RegistrationCredentialDelivery extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String CHANNEL_SCREEN_ONCE = "SCREEN_ONCE";
	public static final String CHANNEL_EMAIL = "EMAIL";

	private Long id;
	private PendaftaranTenant pendaftaranTenant;
	private String deliveryChannel;
	private Date deliveredAt;
	private Date acknowledgedAt;
	private Integer credentialVersion;
	private Date createdAt;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public RegistrationCredentialDelivery() {
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

	@Column(name = "delivery_channel", nullable = false, length = 40)
	public String getDeliveryChannel() {
		return deliveryChannel;
	}

	public void setDeliveryChannel(String deliveryChannel) {
		this.deliveryChannel = deliveryChannel;
	}

	@Column(name = "delivered_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDeliveredAt() {
		return deliveredAt;
	}

	public void setDeliveredAt(Date deliveredAt) {
		this.deliveredAt = deliveredAt;
	}

	@Column(name = "acknowledged_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getAcknowledgedAt() {
		return acknowledgedAt;
	}

	public void setAcknowledgedAt(Date acknowledgedAt) {
		this.acknowledgedAt = acknowledgedAt;
	}

	@Column(name = "credential_version")
	public Integer getCredentialVersion() {
		return credentialVersion == null ? Integer.valueOf(1) : credentialVersion;
	}

	public void setCredentialVersion(Integer credentialVersion) {
		this.credentialVersion = credentialVersion;
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
