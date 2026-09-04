package ais.database.model.hotel;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
 * Folio (tagihan berjalan) satu {@link MenginapTamu}. Total TIDAK disimpan sebagai kolom --
 * dihitung dari SUM({@link FolioTransaksi}) supaya tidak pernah drift; baris transaksi
 * append-only (koreksi = baris ADJUSTMENT baru, bukan edit/hapus baris lama).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_folio")
public class Folio extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439827L;

	public static final String STATUS_OPEN = "OPEN";
	public static final String STATUS_CLOSED = "CLOSED";

	private Long id;
	private PropertiHotel properti;
	private MenginapTamu menginap;
	private String status;
	private Date dibukaPada;
	private Date ditutupPada;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public String toString() {
		return (id == null ? "" : id) + "-" + getStatus();
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = false)
	public PropertiHotel getProperti() { properti = check(properti); return properti; }
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "menginap", nullable = false)
	public MenginapTamu getMenginap() { menginap = check(menginap); return menginap; }
	public void setMenginap(MenginapTamu menginap) { this.menginap = menginap; }

	@Column(name = "status", nullable = false, length = 24)
	public String getStatus() { return status == null ? STATUS_OPEN : status; }
	public void setStatus(String status) { this.status = status; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "dibuka_pada", nullable = false)
	public Date getDibukaPada() { return dibukaPada; }
	public void setDibukaPada(Date dibukaPada) { this.dibukaPada = dibukaPada; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "ditutup_pada", nullable = true)
	public Date getDitutupPada() { return ditutupPada; }
	public void setDitutupPada(Date ditutupPada) { this.ditutupPada = ditutupPada; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
