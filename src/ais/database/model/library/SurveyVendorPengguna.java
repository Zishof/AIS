package ais.database.model.library;

// Pengguna (individu) yang di-assign pada satu survey: penilai (boleh menilai) atau pengamat (lihat saja).
// Akses survey dibatasi hanya ke pengguna yang terdaftar di sini (kecuali bolehLihatSemua).

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/** Pengguna yang di-assign ke sebuah survey (penilai/pengamat). */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "library", name = "survey_vendor_pengguna")
public class SurveyVendorPengguna extends GeneralValueObject {

	private static final long serialVersionUID = 7720145511001000004L;

	public static final String PENILAI = "Penilai";
	public static final String PENGAMAT = "Pengamat";

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

	public String toString() { return pengguna == null ? "" : pengguna.getUserNama(); }

	private SurveyVendor surveyVendor;
	private Tbmuser pengguna;
	private String peran;
	private Boolean bolehLihatSemua;
	private Boolean sudahNotifikasi;
	private Boolean sudahMenilai;

	public SurveyVendorPengguna() {}

	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "survey_vendor", nullable = true)
	public SurveyVendor getSurveyVendor() { return surveyVendor; }
	public void setSurveyVendor(SurveyVendor v) { this.surveyVendor = v; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengguna", nullable = true)
	public Tbmuser getPengguna() { return pengguna; }
	public void setPengguna(Tbmuser pengguna) { this.pengguna = pengguna; }

	@Column(name = "peran", length = 30) public String getPeran() { return peran == null ? PENILAI : peran; }
	public void setPeran(String peran) { this.peran = peran; }

	@Column(name = "boleh_lihat_semua") public Boolean getBolehLihatSemua() { return bolehLihatSemua != null && bolehLihatSemua; }
	public void setBolehLihatSemua(Boolean v) { this.bolehLihatSemua = v; }

	@Column(name = "sudah_notifikasi") public Boolean getSudahNotifikasi() { return sudahNotifikasi != null && sudahNotifikasi; }
	public void setSudahNotifikasi(Boolean v) { this.sudahNotifikasi = v; }

	@Column(name = "sudah_menilai") public Boolean getSudahMenilai() { return sudahMenilai != null && sudahMenilai; }
	public void setSudahMenilai(Boolean v) { this.sudahMenilai = v; }
}
