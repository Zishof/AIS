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
 * Kamar fisik per {@link PropertiHotel}. {@link #getStatusHunian()} adalah PROYEKSI operasional
 * (VACANT/OCCUPIED/DIRTY/OUT_OF_ORDER) -- sumber kebenaran hunian sesungguhnya adalah
 * MenginapTamu/reservasi (fase berikutnya); kolom ini dipelihara servis, bukan diedit bebas klien.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_kamar")
public class Kamar extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439823L;

	public static final String HUNIAN_VACANT = "VACANT";
	public static final String HUNIAN_OCCUPIED = "OCCUPIED";
	public static final String HUNIAN_DIRTY = "DIRTY";
	public static final String HUNIAN_OUT_OF_ORDER = "OUT_OF_ORDER";

	private Long id;
	private PropertiHotel properti;
	private TipeKamar tipeKamar;
	private String nomor;
	private Integer lantai;
	private String statusHunian;
	private String keterangan;
	private Boolean aktif;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public String toString() {
		return (id == null ? "" : id) + "-" + (nomor == null ? "" : nomor);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = false)
	public PropertiHotel getProperti() { return properti; }
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_kamar", nullable = false)
	public TipeKamar getTipeKamar() { return tipeKamar; }
	public void setTipeKamar(TipeKamar tipeKamar) { this.tipeKamar = tipeKamar; }

	@Column(name = "nomor", nullable = false, length = 50)
	public String getNomor() { return nomor == null ? null : nomor.trim(); }
	public void setNomor(String nomor) { this.nomor = nomor; }

	@Column(name = "lantai", nullable = true)
	public Integer getLantai() { return lantai; }
	public void setLantai(Integer lantai) { this.lantai = lantai; }

	@Column(name = "status_hunian", nullable = true, length = 24)
	public String getStatusHunian() { return statusHunian == null ? HUNIAN_VACANT : statusHunian; }
	public void setStatusHunian(String statusHunian) { this.statusHunian = statusHunian; }

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	public Boolean getAktif() { return aktif == null ? true : aktif; }
	public void setAktif(Boolean aktif) { this.aktif = aktif; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
