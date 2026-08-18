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
 * Tamu hotel. SENGAJA ber-scope per {@link PropertiHotel} (isolasi fail-closed antar pemilik --
 * MitraInap adalah platform banyak pemilik independen); profil tamu lintas-properti milik satu
 * pemilik adalah keputusan fase berikutnya, BUKAN default diam-diam.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_tamu")
public class Tamu extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439824L;

	private Long id;
	private PropertiHotel properti;
	private String nama;
	private String jenisIdentitas;
	private String noIdentitas;
	private String telp;
	private String email;
	private String alamat;
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

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() { return nama == null ? null : nama.trim(); }
	public void setNama(String nama) { this.nama = nama; }

	@Column(name = "jenis_identitas", nullable = true, length = 24)
	public String getJenisIdentitas() { return jenisIdentitas; }
	public void setJenisIdentitas(String jenisIdentitas) { this.jenisIdentitas = jenisIdentitas; }

	@Column(name = "no_identitas", nullable = true, length = 100)
	public String getNoIdentitas() { return noIdentitas; }
	public void setNoIdentitas(String noIdentitas) { this.noIdentitas = noIdentitas; }

	@Column(name = "telp", nullable = true, length = 50)
	public String getTelp() { return telp; }
	public void setTelp(String telp) { this.telp = telp; }

	@Column(name = "email", nullable = true, length = 255)
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	@Column(name = "alamat", nullable = true)
	public String getAlamat() { return alamat; }
	public void setAlamat(String alamat) { this.alamat = alamat; }

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
