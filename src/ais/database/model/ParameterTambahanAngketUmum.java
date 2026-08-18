package ais.database.model;

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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import ais.database.model.sekolah.GrupChecklistPenilaianGuru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Konfigurasi parameter tambahan untuk angket umum, angket dosen, dan angket guru.
 *
 * Catatan kompatibilitas:
 * - Relasi lama ke GrupChecklistPenilaianUmum tetap dipertahankan.
 * - Relasi baru ke GrupChecklistPenilaianDosen dan GrupChecklistPenilaianGuru bersifat optional.
 * - Satu baris cukup mengacu ke salah satu grup target saja.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "parameter_tambahan_angket_umum")
public class ParameterTambahanAngketUmum extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private Fakultas fakultas;
	private Jurusan jurusan;
	private String program;
	private Jenjang jenjang;
	private ParameterTambahan parameterTambahan;
	private Boolean tampilDiSemuaTahunAngkatan;
	private String tahunAngkatans;
	private GrupChecklistPenilaianUmum grupChecklistPenilaianUmum;
	private GrupChecklistPenilaianDosen grupChecklistPenilaianDosen;
	private GrupChecklistPenilaianGuru grupChecklistPenilaianGuru;
	private Integer nomorUrut;
	private Yayasan yayasan;
	private Sekolah sekolah;

	public ParameterTambahanAngketUmum() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan == null || jurusan.getId() == null ? null : jurusan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan == null || parameterTambahan.getId() == null ? null : parameterTambahan;
	}

	@Column(name = "tampil_di_semua_tahun_angkatan")
	public Boolean getTampilDiSemuaTahunAngkatan() {
		return tampilDiSemuaTahunAngkatan == null ? Boolean.TRUE : tampilDiSemuaTahunAngkatan;
	}

	public void setTampilDiSemuaTahunAngkatan(Boolean tampilDiSemuaTahunAngkatan) {
		this.tampilDiSemuaTahunAngkatan = tampilDiSemuaTahunAngkatan;
	}

	@Column(columnDefinition = "text")
	public String getTahunAngkatans() {
		return tahunAngkatans == null ? "" : tahunAngkatans;
	}

	public void setTahunAngkatans(String tahunAngkatans) {
		this.tahunAngkatans = tahunAngkatans;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas == null || fakultas.getId() == null ? null : fakultas;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		ParameterTambahan pt = getParameterTambahan();
		if (pt != null && pt.getYayasan() != null) {
			yayasan = pt.getYayasan();
		}
		return yayasan;
	}

	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		ParameterTambahan pt = getParameterTambahan();
		if (pt != null && pt.getSekolah() != null) {
			sekolah = pt.getSekolah();
		}
		return sekolah;
	}

	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	public String getProgram() {
		return program;
	}

	public void setProgram(String program) {
		this.program = program;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang", nullable = true)
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		return jenjang;
	}

	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang == null || jenjang.getId() == null ? null : jenjang;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_checklist_penilaian_umum", nullable = true)
	public GrupChecklistPenilaianUmum getGrupChecklistPenilaianUmum() {
		grupChecklistPenilaianUmum = check(grupChecklistPenilaianUmum);
		return grupChecklistPenilaianUmum;
	}

	public void setGrupChecklistPenilaianUmum(GrupChecklistPenilaianUmum grupChecklistPenilaianUmum) {
		this.grupChecklistPenilaianUmum = grupChecklistPenilaianUmum == null || grupChecklistPenilaianUmum.getId() == null ? null
				: grupChecklistPenilaianUmum;
		if (this.grupChecklistPenilaianUmum != null) {
			this.grupChecklistPenilaianDosen = null;
			this.grupChecklistPenilaianGuru = null;
		}
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_checklist_penilaian_dosen", nullable = true)
	public GrupChecklistPenilaianDosen getGrupChecklistPenilaianDosen() {
		grupChecklistPenilaianDosen = check(grupChecklistPenilaianDosen);
		return grupChecklistPenilaianDosen;
	}

	public void setGrupChecklistPenilaianDosen(GrupChecklistPenilaianDosen grupChecklistPenilaianDosen) {
		this.grupChecklistPenilaianDosen = grupChecklistPenilaianDosen == null || grupChecklistPenilaianDosen.getId() == null ? null
				: grupChecklistPenilaianDosen;
		if (this.grupChecklistPenilaianDosen != null) {
			this.grupChecklistPenilaianUmum = null;
			this.grupChecklistPenilaianGuru = null;
		}
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_checklist_penilaian_guru", nullable = true)
	public GrupChecklistPenilaianGuru getGrupChecklistPenilaianGuru() {
		grupChecklistPenilaianGuru = check(grupChecklistPenilaianGuru);
		return grupChecklistPenilaianGuru;
	}

	public void setGrupChecklistPenilaianGuru(GrupChecklistPenilaianGuru grupChecklistPenilaianGuru) {
		this.grupChecklistPenilaianGuru = grupChecklistPenilaianGuru == null || grupChecklistPenilaianGuru.getId() == null ? null
				: grupChecklistPenilaianGuru;
		if (this.grupChecklistPenilaianGuru != null) {
			this.grupChecklistPenilaianUmum = null;
			this.grupChecklistPenilaianDosen = null;
		}
	}

	public Integer getNomorUrut() {
		ParameterTambahan pt = getParameterTambahan();
		if (pt != null) {
			nomorUrut = pt.getNomorUrut();
		}
		return nomorUrut == null ? Integer.valueOf(1) : nomorUrut;
	}

	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	@Transient
	public String getTargetGrupLabel() {
		try {
			if (getGrupChecklistPenilaianUmum() != null) {
				return "Umum - " + getGrupChecklistPenilaianUmum().getIsi();
			}
			if (getGrupChecklistPenilaianDosen() != null) {
				return "Dosen - " + getGrupChecklistPenilaianDosen().getIsi();
			}
			if (getGrupChecklistPenilaianGuru() != null) {
				return "Guru - " + getGrupChecklistPenilaianGuru().getIsi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAngketUmum.java:283");
		}
		return "";
	}

	public String toString() {
		ParameterTambahan pt = getParameterTambahan();
		return (getId() == null ? "" : getId() + "-") + getTargetGrupLabel() + " - "
				+ (pt == null ? "" : pt.getLabelInputan());
	}
}
