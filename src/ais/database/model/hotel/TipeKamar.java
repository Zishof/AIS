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

/** Tipe kamar per {@link PropertiHotel} (mis. Standard/Deluxe/Suite) + harga dasar per malam. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_tipe_kamar")
public class TipeKamar extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439822L;

	private Long id;
	private PropertiHotel properti;
	private String kode;
	private String nama;
	private String keterangan;
	private Double hargaDasar;
	private Integer kapasitas;
	private Boolean aktif;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public String toString() {
		return (id == null ? "" : id) + "-" + (nama == null ? "" : nama);
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

	@Column(name = "kode", nullable = true, length = 100)
	public String getKode() { return kode; }
	public void setKode(String kode) { this.kode = kode; }

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() { return nama == null ? null : nama.trim(); }
	public void setNama(String nama) { this.nama = nama; }

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/** Harga dasar per malam; harga musiman/promo menyusul fase berikutnya. */
	@Column(name = "harga_dasar", nullable = true)
	public Double getHargaDasar() { return hargaDasar; }
	public void setHargaDasar(Double hargaDasar) { this.hargaDasar = hargaDasar; }

	@Column(name = "kapasitas", nullable = true)
	public Integer getKapasitas() { return kapasitas; }
	public void setKapasitas(Integer kapasitas) { this.kapasitas = kapasitas; }

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
