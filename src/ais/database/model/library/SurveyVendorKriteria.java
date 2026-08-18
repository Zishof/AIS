package ais.database.model.library;

// Kriteria penilaian survey (configurable per survey) + bobot. P2/P3 saran AI: bobot & pertanyaan
// tidak di-hard-code, disimpan di DB sehingga admin bisa mengubah tanpa ubah source.

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

/** Kriteria + bobot penilaian (configurable) untuk satu survey. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "library", name = "survey_vendor_kriteria")
public class SurveyVendorKriteria extends GeneralValueObject {

	private static final long serialVersionUID = 7720145511001000003L;

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

	public String toString() { return nama; }

	private SurveyVendor surveyVendor;
	private Integer urutan;
	private String nama;
	private String pertanyaan;
	private Double bobot;      // persen
	private Boolean aktif;

	public SurveyVendorKriteria() {}

	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "survey_vendor", nullable = true)
	public SurveyVendor getSurveyVendor() { return surveyVendor; }
	public void setSurveyVendor(SurveyVendor v) { this.surveyVendor = v; }

	@Column(name = "urutan") public Integer getUrutan() { return urutan == null ? 0 : urutan; }
	public void setUrutan(Integer urutan) { this.urutan = urutan; }

	@Column(name = "nama", length = 255) public String getNama() { return nama; }
	public void setNama(String nama) { this.nama = nama; }

	@Column(name = "pertanyaan") public String getPertanyaan() { return pertanyaan; }
	public void setPertanyaan(String pertanyaan) { this.pertanyaan = pertanyaan; }

	@Column(name = "bobot") public Double getBobot() { return bobot == null ? 0.0 : bobot; }
	public void setBobot(Double bobot) { this.bobot = bobot; }

	@Column(name = "aktif") public Boolean getAktif() { return aktif == null || aktif; }
	public void setAktif(Boolean aktif) { this.aktif = aktif; }
}
