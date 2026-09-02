package ais.database.model.sekolah;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Kelompok tampilan untuk item biaya sekolah. Relasi anggota disimpan pada
 * {@link ItemBiayaSekolah}; satu grup dapat dipakai oleh banyak item, sedangkan
 * satu item hanya berada pada paling banyak satu grup.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "grup_item_biaya_sekolah", schema = "sekolah")
public class GrupItemBiayaSekolah extends GeneralValueObject {

	private static final long serialVersionUID = 4800716294061911034L;
	private Long id;
	private String kode;
	private String nama;
	private String keterangan;
	private Boolean aktif;
	private Yayasan yayasan;
	private Sekolah sekolah;
	private Set<ItemBiayaSekolah> itemBiayaSekolahs = new HashSet<ItemBiayaSekolah>();
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "kode", nullable = false)
	public String getKode() { return kode == null ? "" : kode.trim(); }
	public void setKode(String kode) { this.kode = kode; }

	@Column(name = "nama", nullable = false)
	public String getNama() { return nama == null ? "" : nama.trim(); }
	public void setNama(String nama) { this.nama = nama; }

	@Column(name = "keterangan")
	public String getKeterangan() { return keterangan == null ? "" : keterangan.trim(); }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	@Column(name = "aktif")
	public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
	public void setAktif(Boolean aktif) { this.aktif = aktif; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() { yayasan = check(yayasan); return yayasan; }
	public void setYayasan(Yayasan yayasan) { this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() { sekolah = check(sekolah); return sekolah; }
	public void setSekolah(Sekolah sekolah) { this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah; }

	@OneToMany(mappedBy = "grupItemBiayaSekolah", fetch = FetchType.LAZY)
	public Set<ItemBiayaSekolah> getItemBiayaSekolahs() { return itemBiayaSekolahs; }
	public void setItemBiayaSekolahs(Set<ItemBiayaSekolah> itemBiayaSekolahs) {
		this.itemBiayaSekolahs = itemBiayaSekolahs == null
				? new HashSet<ItemBiayaSekolah>() : itemBiayaSekolahs;
	}

	public String getLabelTampilan() {
		return (getKode().isEmpty() ? "" : getKode() + " - ") + getNama();
	}

	public String toString() { return getLabelTampilan(); }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId != null && !olehId.trim().isEmpty()) this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
