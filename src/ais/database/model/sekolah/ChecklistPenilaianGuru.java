package ais.database.model.sekolah;

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

import ais.database.model.GeneralValueObject;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sekolah", name = "checklist_penilaian_guru")
public class ChecklistPenilaianGuru extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;
	private static final ThreadLocal<NumberFormat> NF = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			return new DecimalFormat("00000");
		}
	};
	private static final String PILIHAN_DEFAULT = "{}";

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String isi;
	private String keterangan;
	private GrupChecklistPenilaianGuru grupChecklistPenilaianGuru;
	private String pilihan;
	private Double bobot;
	private Boolean aktif;

	public ChecklistPenilaianGuru() {
	}

	public ChecklistPenilaianGuru(Long id) {
		this.id = id;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Column(name = "isi", nullable = false, columnDefinition = "text")
	public String getIsi() {
		return isi;
	}

	public void setIsi(String isi) {
		this.isi = isi;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_checklist_penilaian_guru")
	public GrupChecklistPenilaianGuru getGrupChecklistPenilaianGuru() {
		grupChecklistPenilaianGuru = check(grupChecklistPenilaianGuru);
		return grupChecklistPenilaianGuru;
	}

	public void setGrupChecklistPenilaianGuru(GrupChecklistPenilaianGuru grupChecklistPenilaianGuru) {
		this.grupChecklistPenilaianGuru = grupChecklistPenilaianGuru;
	}

	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "bobot")
	public Double getBobot() {
		return bobot == null ? Double.valueOf(1.0) : bobot;
	}

	public void setBobot(Double bobot) {
		this.bobot = bobot;
	}

	@Column(name = "pilihan", columnDefinition = "text")
	public String getPilihan() {
		if (pilihan == null || pilihan.trim().isEmpty()) {
			return PILIHAN_DEFAULT;
		}
		try {
			new JSONObject(pilihan);
			return pilihan;
		} catch (Exception e) {
			return PILIHAN_DEFAULT;
		}
	}

	public void setPilihan(String pilihan) {
		this.pilihan = pilihan;
	}

	public String ambilkey() {
		String nama = getIsi() == null ? "" : getIsi().trim();
		String key = nama.length() > 5 ? nama.substring(0, 5) : nama;
		Long myId = getId() == null ? Long.valueOf(0L) : getId();
		GrupChecklistPenilaianGuru grup = getGrupChecklistPenilaianGuru();
		Long grupId = grup == null || grup.getId() == null ? Long.valueOf(0L) : grup.getId();
		return NF.get().format(grupId) + "_" + key + "_" + NF.get().format(myId);
	}

	public String toString() {
		return isi == null ? "" : isi;
	}
}
