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
 * Tiket dapur (kitchen ticket) satu nota POS outlet -- LANGKAH 5 MitraInap. Satu
 * {@link ais.database.model.koperasi.PembelianAnggotaKoperasi} maksimal SATU tiket
 * (kolom pembelian unik = idempotensi pembuatan via retry outbox). Transisi status
 * DIVALIDASI SERVER ({@code HotelApiHelper.kitchenTicketUpdate}, jangan percaya klien):
 * QUEUED -&gt; PREPARING -&gt; READY -&gt; SERVED, QUEUED/PREPARING -&gt; CANCELLED --
 * referensi urutan: KITCHEN_TRANSITIONS pos-hospitality.service.ts versi Node (yang di
 * sana justru TIDAK memvalidasi transisi; kesenjangan itu yang ditutup di sini).
 * Timestamp per-fase diisi SEKALI (pola COALESCE Node): tidak ditimpa saat status maju.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_tiket_dapur")
public class TiketDapur extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439829L;

	public static final String STATUS_QUEUED = "QUEUED";
	public static final String STATUS_PREPARING = "PREPARING";
	public static final String STATUS_READY = "READY";
	public static final String STATUS_SERVED = "SERVED";
	public static final String STATUS_CANCELLED = "CANCELLED";

	private Long id;
	private PropertiHotel properti;
	private ais.database.model.koperasi.PembelianAnggotaKoperasi pembelian;
	private String status;
	private String catatan;
	private Date mulaiPada;
	private Date siapPada;
	private Date disajikanPada;
	private Date dibatalkanPada;
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

	/** Nullable: nota outlet tanpa konteks properti (mis. toko campuran) tetap boleh bertiket. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = true)
	public PropertiHotel getProperti() { properti = check(properti); return properti; }
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pembelian", nullable = false, unique = true)
	public ais.database.model.koperasi.PembelianAnggotaKoperasi getPembelian() { pembelian = check(pembelian); return pembelian; }
	public void setPembelian(ais.database.model.koperasi.PembelianAnggotaKoperasi pembelian) { this.pembelian = pembelian; }

	@Column(name = "status", nullable = false, length = 24)
	public String getStatus() { return status == null ? STATUS_QUEUED : status; }
	public void setStatus(String status) { this.status = status; }

	@Column(name = "catatan", nullable = true)
	public String getCatatan() { return catatan; }
	public void setCatatan(String catatan) { this.catatan = catatan; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "mulai_pada", nullable = true)
	public Date getMulaiPada() { return mulaiPada; }
	public void setMulaiPada(Date mulaiPada) { this.mulaiPada = mulaiPada; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "siap_pada", nullable = true)
	public Date getSiapPada() { return siapPada; }
	public void setSiapPada(Date siapPada) { this.siapPada = siapPada; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "disajikan_pada", nullable = true)
	public Date getDisajikanPada() { return disajikanPada; }
	public void setDisajikanPada(Date disajikanPada) { this.disajikanPada = disajikanPada; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "dibatalkan_pada", nullable = true)
	public Date getDibatalkanPada() { return dibatalkanPada; }
	public void setDibatalkanPada(Date dibatalkanPada) { this.dibatalkanPada = dibatalkanPada; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
