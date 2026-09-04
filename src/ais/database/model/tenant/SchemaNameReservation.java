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
 * <h3>Reservasi atomik username/schema-name tenant (unique {@code normalized_name}).</h3>
 *
 * <p>Reservasi TIDAK dibuat dgn pola query-then-insert polos: INSERT baris di sini di dalam
 * transaction submit adalah titik serialisasi -- dua submit bersamaan dgn username sama akan
 * membuat SATU yang menang dan yang lain kena unique-violation (ditangkap service → balasan
 * "Username tidak tersedia"). {@code normalizedUsername} pada permohonan hanyalah salinan;
 * kebenaran reservasi ada di tabel ini (invariant #2 ERD).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "schema_name_reservation")
public class SchemaNameReservation extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_RESERVED = "RESERVED";
	public static final String STATUS_CONSUMED = "CONSUMED";
	public static final String STATUS_RELEASED = "RELEASED";
	public static final String STATUS_EXPIRED = "EXPIRED";

	private Long id;
	private String normalizedName;
	private PendaftaranTenant pendaftaranTenant;
	private String status;
	private Date reservedAt;
	private Date expiresAt;
	private Date consumedAt;
	private Date releasedAt;
	private String reservationTokenHash;
	private Integer version;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SchemaNameReservation() {
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

	@Column(name = "normalized_name", unique = true, nullable = false, length = 64)
	public String getNormalizedName() {
		return normalizedName;
	}

	public void setNormalizedName(String normalizedName) {
		this.normalizedName = normalizedName;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftaran_tenant_id", nullable = true)
	public PendaftaranTenant getPendaftaranTenant() {
		pendaftaranTenant = check(pendaftaranTenant);
		return pendaftaranTenant;
	}

	public void setPendaftaranTenant(PendaftaranTenant pendaftaranTenant) {
		this.pendaftaranTenant = pendaftaranTenant;
	}

	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_RESERVED : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "reserved_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getReservedAt() {
		return reservedAt;
	}

	public void setReservedAt(Date reservedAt) {
		this.reservedAt = reservedAt;
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

	@Column(name = "released_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getReleasedAt() {
		return releasedAt;
	}

	public void setReleasedAt(Date releasedAt) {
		this.releasedAt = releasedAt;
	}

	/** Hash SHA-256 token kepemilikan reservasi (token mentah tidak disimpan). */
	@Column(name = "reservation_token_hash", length = 64)
	public String getReservationTokenHash() {
		return reservationTokenHash;
	}

	public void setReservationTokenHash(String reservationTokenHash) {
		this.reservationTokenHash = reservationTokenHash;
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
