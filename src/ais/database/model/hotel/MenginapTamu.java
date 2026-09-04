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
 * Guest stay -- check-in aktif (analog {@code Pendaftaran} di sirs). Dibuat dari reservasi ATAU
 * walk-in ({@link #getReservasi()} nullable). Satu stay membuka tepat satu {@link Folio};
 * checkout menutup keduanya atomik ({@code HotelApiHelper.checkout}).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_menginap")
public class MenginapTamu extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439826L;

	public static final String STATUS_IN_HOUSE = "IN_HOUSE";
	public static final String STATUS_CHECKED_OUT = "CHECKED_OUT";

	private Long id;
	private PropertiHotel properti;
	private ReservasiKamar reservasi;
	private Tamu tamu;
	private Kamar kamar;
	private Date checkinPada;
	private Date checkoutPada;
	private Double hargaPerMalam;
	private String status;
	private String catatan;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public String toString() {
		return (id == null ? "" : id) + "-" + (tamu == null ? "" : tamu.getNama());
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
	@JoinColumn(name = "reservasi", nullable = true)
	public ReservasiKamar getReservasi() { reservasi = check(reservasi); return reservasi; }
	public void setReservasi(ReservasiKamar reservasi) { this.reservasi = reservasi; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tamu", nullable = false)
	public Tamu getTamu() { tamu = check(tamu); return tamu; }
	public void setTamu(Tamu tamu) { this.tamu = tamu; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kamar", nullable = false)
	public Kamar getKamar() { kamar = check(kamar); return kamar; }
	public void setKamar(Kamar kamar) { this.kamar = kamar; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "checkin_pada", nullable = false)
	public Date getCheckinPada() { return checkinPada; }
	public void setCheckinPada(Date checkinPada) { this.checkinPada = checkinPada; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "checkout_pada", nullable = true)
	public Date getCheckoutPada() { return checkoutPada; }
	public void setCheckoutPada(Date checkoutPada) { this.checkoutPada = checkoutPada; }

	/** Snapshot harga per malam saat check-in (dari reservasi atau tipe kamar saat itu). */
	@Column(name = "harga_per_malam", nullable = true)
	public Double getHargaPerMalam() { return hargaPerMalam; }
	public void setHargaPerMalam(Double hargaPerMalam) { this.hargaPerMalam = hargaPerMalam; }

	@Column(name = "status", nullable = false, length = 24)
	public String getStatus() { return status == null ? STATUS_IN_HOUSE : status; }
	public void setStatus(String status) { this.status = status; }

	@Column(name = "catatan", nullable = true)
	public String getCatatan() { return catatan; }
	public void setCatatan(String catatan) { this.catatan = catatan; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
