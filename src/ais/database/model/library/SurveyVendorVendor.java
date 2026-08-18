package ais.database.model.library;

// Vendor yang dinilai dalam Survey Pemilihan Penilaian Vendor (Data Vendor - Lampiran 1.1 bag. A).

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import ais.database.model.GeneralValueObject;

/** Vendor (I/II/III/...) yang dinilai pada satu survey. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "library", name = "survey_vendor_vendor")
public class SurveyVendorVendor extends GeneralValueObject {

	private static final long serialVersionUID = 7720145511001000002L;

	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) { return; } this.olehId = olehId; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) { return; } this.oleh = oleh; }
	public String getOleh() { return oleh; }

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

	public String toString() { return getNamaVendor(); }

	private SurveyVendor surveyVendor;
	private Penyedia penyedia;
	private Integer urutan;
	private String namaVendor;
	private String alamatKontak;
	private String jenisBarangJasa;
	private String picVendor;
	private Boolean lulusQualification;   // P1: gerbang kualifikasi (default lulus)

	public SurveyVendorVendor() {}

	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "survey_vendor", nullable = true)
	public SurveyVendor getSurveyVendor() { return surveyVendor; }
	public void setSurveyVendor(SurveyVendor v) { this.surveyVendor = v; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penyedia", nullable = true)
	public Penyedia getPenyedia() { penyedia = check(penyedia); return penyedia; }
	public void setPenyedia(Penyedia penyedia) { this.penyedia = penyedia; }

	@Column(name = "urutan") public Integer getUrutan() { return urutan == null ? 0 : urutan; }
	public void setUrutan(Integer urutan) { this.urutan = urutan; }

	@Column(name = "nama_vendor", length = 255)
	public String getNamaVendor() {
		if ((namaVendor == null || namaVendor.trim().isEmpty()) && getPenyedia() != null) { return getPenyedia().getNama(); }
		return namaVendor;
	}
	public void setNamaVendor(String v) { this.namaVendor = v; }

	@Column(name = "alamat_kontak")
	public String getAlamatKontak() {
		if ((alamatKontak == null || alamatKontak.trim().isEmpty()) && getPenyedia() != null) {
			String a = getPenyedia().getAlamat();
			String t = getPenyedia().getTelp();
			String gabung = (a == null ? "" : a) + (t == null || t.trim().isEmpty() ? "" : " / " + t);
			return gabung.trim().isEmpty() ? null : gabung.trim();
		}
		return alamatKontak;
	}
	public void setAlamatKontak(String v) { this.alamatKontak = v; }

	@Column(name = "jenis_barang_jasa") public String getJenisBarangJasa() { return jenisBarangJasa; }
	public void setJenisBarangJasa(String v) { this.jenisBarangJasa = v; }

	@Column(name = "pic_vendor")
	public String getPicVendor() {
		if ((picVendor == null || picVendor.trim().isEmpty()) && getPenyedia() != null) { return getPenyedia().getKontak(); }
		return picVendor;
	}
	public void setPicVendor(String v) { this.picVendor = v; }

	@Column(name = "lulus_qualification") public Boolean getLulusQualification() { return lulusQualification == null || lulusQualification; }
	public void setLulusQualification(Boolean v) { this.lulusQualification = v; }
}
