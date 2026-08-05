package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.text.DecimalFormat;
import java.text.NumberFormat;
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

import org.hibernate.envers.Audited;
import org.json.JSONObject;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "checklist_penilaian_dosen")
public class ChecklistPenilaianDosen extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;
	private static final NumberFormat NF = new DecimalFormat("00000");
	private static final String JSON_KOSONG = new JSONObject().toString();

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String isi;
	private String keterangan;
	private GrupChecklistPenilaianDosen grupChecklistPenilaianDosen;
	private String pilihan;
	private Double bobot;
	private Boolean aktif;

	public ChecklistPenilaianDosen() {
	}

	public ChecklistPenilaianDosen(Long id) {
		this.id = id;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId.trim();
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh.trim();
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	@Override
	public String toString() {
		return isi == null ? "" : isi;
	}

	public String ambilkey() {
		String isiKey = getIsi() == null ? "" : getIsi().trim();
		String prefix = isiKey.length() > 5 ? isiKey.substring(0, 5) : isiKey;
		Long idChecklist = getId() == null ? Long.valueOf(0L) : getId();
		Long idGrup = getGrupChecklistPenilaianDosen() == null || getGrupChecklistPenilaianDosen().getId() == null
				? Long.valueOf(0L)
				: getGrupChecklistPenilaianDosen().getId();
		return NF.format(idGrup) + "_" + prefix + "_" + NF.format(idChecklist);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "isi", nullable = false, columnDefinition = "text")
	public String getIsi() {
		return this.isi;
	}

	public void setIsi(String isi) {
		this.isi = isi;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_checklist_penilaian_dosen")
	public GrupChecklistPenilaianDosen getGrupChecklistPenilaianDosen() {
		grupChecklistPenilaianDosen = check(grupChecklistPenilaianDosen);
		return grupChecklistPenilaianDosen;
	}

	public void setGrupChecklistPenilaianDosen(GrupChecklistPenilaianDosen grupChecklistPenilaianDosen) {
		this.grupChecklistPenilaianDosen = grupChecklistPenilaianDosen;
	}

	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	public Double getBobot() {
		return bobot == null ? Double.valueOf(1.0D) : bobot;
	}

	public void setBobot(Double bobot) {
		this.bobot = bobot;
	}

	@Column(columnDefinition = "text")
	public String getPilihan() {
		return pilihan == null || pilihan.trim().isEmpty() ? JSON_KOSONG : pilihan;
	}

	public void setPilihan(String pilihan) {
		this.pilihan = pilihan;
	}
}
