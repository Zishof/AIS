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
 * Reservasi/booking kamar. Transisi status DIVALIDASI SERVER ({@code HotelApiHelper}), jangan
 * percaya klien: BOOKED -&gt; CONFIRMED -&gt; CHECKED_IN, atau BOOKED/CONFIRMED -&gt; CANCELLED/NO_SHOW.
 * {@link #getHargaPerMalam()} adalah SNAPSHOT dari {@link TipeKamar#getHargaDasar()} saat booking
 * dibuat -- perubahan harga master tidak mengubah reservasi berjalan (pola snapshot yang sama
 * dengan cost_snapshot POS).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_reservasi")
public class ReservasiKamar extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439825L;

	public static final String STATUS_BOOKED = "BOOKED";
	public static final String STATUS_CONFIRMED = "CONFIRMED";
	public static final String STATUS_CHECKED_IN = "CHECKED_IN";
	public static final String STATUS_CANCELLED = "CANCELLED";
	public static final String STATUS_NO_SHOW = "NO_SHOW";

	private Long id;
	private PropertiHotel properti;
	private Tamu tamu;
	private TipeKamar tipeKamar;
	private Kamar kamar;
	private String kode;
	private Date tanggalCheckin;
	private Date tanggalCheckout;
	private Integer jumlahTamu;
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
		return (id == null ? "" : id) + "-" + (kode == null ? "" : kode);
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
	@JoinColumn(name = "tamu", nullable = false)
	public Tamu getTamu() { tamu = check(tamu); return tamu; }
	public void setTamu(Tamu tamu) { this.tamu = tamu; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_kamar", nullable = false)
	public TipeKamar getTipeKamar() { tipeKamar = check(tipeKamar); return tipeKamar; }
	public void setTipeKamar(TipeKamar tipeKamar) { this.tipeKamar = tipeKamar; }

	/** Kamar spesifik opsional saat booking; boleh baru ditetapkan saat check-in. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kamar", nullable = true)
	public Kamar getKamar() { kamar = check(kamar); return kamar; }
	public void setKamar(Kamar kamar) { this.kamar = kamar; }

	@Column(name = "kode", nullable = false, length = 64)
	public String getKode() { return kode; }
	public void setKode(String kode) { this.kode = kode; }

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_checkin", nullable = false)
	public Date getTanggalCheckin() { return tanggalCheckin; }
	public void setTanggalCheckin(Date tanggalCheckin) { this.tanggalCheckin = tanggalCheckin; }

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_checkout", nullable = false)
	public Date getTanggalCheckout() { return tanggalCheckout; }
	public void setTanggalCheckout(Date tanggalCheckout) { this.tanggalCheckout = tanggalCheckout; }

	@Column(name = "jumlah_tamu", nullable = true)
	public Integer getJumlahTamu() { return jumlahTamu; }
	public void setJumlahTamu(Integer jumlahTamu) { this.jumlahTamu = jumlahTamu; }

	@Column(name = "harga_per_malam", nullable = true)
	public Double getHargaPerMalam() { return hargaPerMalam; }
	public void setHargaPerMalam(Double hargaPerMalam) { this.hargaPerMalam = hargaPerMalam; }

	@Column(name = "status", nullable = false, length = 24)
	public String getStatus() { return status == null ? STATUS_BOOKED : status; }
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
