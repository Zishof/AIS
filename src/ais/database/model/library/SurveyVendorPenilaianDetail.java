package ais.database.model.library;

// Sel skor: nilai (1..5) satu pengguna untuk satu kriteria pada satu vendor. Matriks penilaian =
// (jumlah kriteria) x (jumlah vendor) baris per penilaian pengguna.

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

/** Skor (1..5) pengguna untuk (kriteria x vendor). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "library", name = "survey_vendor_penilaian_detail")
public class SurveyVendorPenilaianDetail extends GeneralValueObject {

	private static final long serialVersionUID = 7720145511001000006L;

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

	private SurveyVendorPenilaian penilaian;
	private SurveyVendorKriteria kriteria;
	private SurveyVendorVendor vendor;
	private Integer nilai;
	private String ket;

	public SurveyVendorPenilaianDetail() {}

	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penilaian", nullable = true)
	public SurveyVendorPenilaian getPenilaian() { return penilaian; }
	public void setPenilaian(SurveyVendorPenilaian v) { this.penilaian = v; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kriteria", nullable = true)
	public SurveyVendorKriteria getKriteria() { return kriteria; }
	public void setKriteria(SurveyVendorKriteria v) { this.kriteria = v; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "vendor", nullable = true)
	public SurveyVendorVendor getVendor() { return vendor; }
	public void setVendor(SurveyVendorVendor v) { this.vendor = v; }

	@Column(name = "nilai") public Integer getNilai() { return nilai; }
	public void setNilai(Integer nilai) { this.nilai = nilai; }

	@Column(name = "ket") public String getKet() { return ket; }
	public void setKet(String ket) { this.ket = ket; }
}
