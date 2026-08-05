package ais.database.model;

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

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "penumuman_website")
public class PenumumanWebsite extends GeneralValueObject {
	private static final long serialVersionUID = 1L;

	private Long id;
	private String judul;
	private String ringkasan;
	private String isi;
	private String kategori;
	private Date tanggal;
	private Boolean aktif;
	private PerguruanTinggi perguruanTinggi;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "judul", nullable = false, columnDefinition = "text")
	public String getJudul() {
		return judul == null ? "" : judul.trim();
	}

	public void setJudul(String judul) {
		this.judul = judul;
	}

	@Column(name = "ringkasan", columnDefinition = "text")
	public String getRingkasan() {
		return ringkasan == null ? "" : ringkasan;
	}

	public void setRingkasan(String ringkasan) {
		this.ringkasan = ringkasan;
	}

	@Column(name = "isi", columnDefinition = "text")
	public String getIsi() {
		return isi == null ? "" : isi;
	}

	public void setIsi(String isi) {
		this.isi = isi;
	}

	@Column(name = "kategori")
	public String getKategori() {
		return kategori == null || kategori.trim().isEmpty() ? "Berita Kampus" : kategori.trim();
	}

	public void setKategori(String kategori) {
		this.kategori = kategori;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi")
	public PerguruanTinggi getPerguruanTinggi() {
		return perguruanTinggi;
	}

	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh != null && !oleh.trim().isEmpty()) {
			this.oleh = oleh;
		}
	}

	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId != null && !olehId.trim().isEmpty()) {
			this.olehId = olehId;
		}
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	public String toString() {
		return getJudul();
	}
}
