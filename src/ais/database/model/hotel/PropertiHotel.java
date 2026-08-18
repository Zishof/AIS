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
import ais.database.model.Tbmuser;

/**
 * <h3>Properti hotel/penginapan MitraInap -- root scope vertikal hotel (diskriminator baris).</h3>
 *
 * <p>Analog {@link ais.database.model.inventory.Toko} pada vertikal Kantin/POS: SEMUA tabel hotel
 * lain ber-FK ke properti ini; scoping aktor lewat perluasan pola
 * {@code EbisnisActorContextResolver} (fail-closed), BUKAN skema-per-tenant -- keputusan
 * 2.4/5.1 dokumen handover MitraInap 2026-08-18: mode LEGACY (skema tetap) adalah deployment
 * default dan jalur skema-per-tenant masih "schema-only-v0".</p>
 *
 * <p>Multi-properti (keputusan 5.3): satu {@link #getPemilik() pemilik} boleh menaungi BANYAK
 * baris properti -- relasi many-to-one biasa tanpa constraint unik; picker properti di klien
 * menyusul. Tabel di skema {@code public} berprefix {@code hotel_} (bukan skema baru) supaya
 * {@code hbm2ddl.auto=update} membuat tabelnya tanpa CREATE SCHEMA manual -- seluruh DDL
 * diserahkan ke Hibernate sesuai arahan pemilik produk 2026-08-18.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_properti")
public class PropertiHotel extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439821L;

	private Long id;
	private String kode;
	private String nama;
	private String alamat;
	private String kota;
	private String telp;
	private String email;
	private String keterangan;
	private Integer jumlahLantai;
	private Boolean aktif;
	private Tbmuser pemilik;
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

	@Column(name = "kode", nullable = true, length = 100)
	public String getKode() { return kode; }
	public void setKode(String kode) { this.kode = kode; }

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() { return nama == null ? null : nama.trim(); }
	public void setNama(String nama) { this.nama = nama; }

	@Column(name = "alamat", nullable = true)
	public String getAlamat() { return alamat; }
	public void setAlamat(String alamat) { this.alamat = alamat; }

	@Column(name = "kota", nullable = true, length = 100)
	public String getKota() { return kota; }
	public void setKota(String kota) { this.kota = kota; }

	@Column(name = "telp", nullable = true, length = 50)
	public String getTelp() { return telp; }
	public void setTelp(String telp) { this.telp = telp; }

	@Column(name = "email", nullable = true, length = 255)
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	@Column(name = "jumlah_lantai", nullable = true)
	public Integer getJumlahLantai() { return jumlahLantai; }
	public void setJumlahLantai(Integer jumlahLantai) { this.jumlahLantai = jumlahLantai; }

	public Boolean getAktif() { return aktif == null ? true : aktif; }
	public void setAktif(Boolean aktif) { this.aktif = aktif; }

	/** Pemilik/pengurus utama -- nullable; SATU pemilik boleh punya BANYAK properti (5.3). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pemilik", nullable = true)
	public Tbmuser getPemilik() { return pemilik; }
	public void setPemilik(Tbmuser pemilik) { this.pemilik = pemilik; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
