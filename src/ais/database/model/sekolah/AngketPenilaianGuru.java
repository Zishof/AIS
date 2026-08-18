package ais.database.model.sekolah;

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

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sekolah", name = "angket_penilaian_guru")
public class AngketPenilaianGuru extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private String isi;
	private String keterangan;
	private String petunjuk;
	private Integer jumlahPilihan;
	private Yayasan yayasan;
	private Sekolah sekolah;
	private String program;
	private String angkatan;
	private Boolean untukSiswa;
	private Boolean untukGuru;
	private Boolean tampilKeterangan;

	public AngketPenilaianGuru() {
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

	@Column(name = "kode", nullable = true)
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
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

	@Column(name = "petunjuk", columnDefinition = "text")
	public String getPetunjuk() {
		String content = Common.getKonfigurasi("keterangan_checklist_penilaian_guru_oleh_siswa",
				"Sesuai dengan yang Saudara ketahui, berilah penilaian secara jujur, objektif, dan penuh tanggung jawab terhadap guru Saudara. Penilaian dilakukan terhadap aspek-aspek dalam tabel berikut dengan cara memilih angka pada kolom skor.\n 1 = sangat tidak baik/sangat rendah/tidak pernah\n 2 = tidak baik/rendah/jarang\n 3 = biasa/cukup/kadang-kadang\n 4 = baik/tinggi/sering\n 5 = sangat baik/sangat tinggi/selalu")
				.getNilai();
		return petunjuk == null || petunjuk.trim().isEmpty() ? content : petunjuk.trim();
	}

	public void setPetunjuk(String petunjuk) {
		this.petunjuk = petunjuk;
	}

	@Column(name = "jumlah_pilihan")
	public Integer getJumlahPilihan() {
		Integer defaultJumlah = 5;
		try {
			defaultJumlah = Integer.parseInt(Common
					.getKonfigurasi("jumlah_pilihan_checklist_penilaian_guru_oleh_siswa", "5").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/AngketPenilaianGuru.java:145");
		}
		return jumlahPilihan == null || jumlahPilihan.intValue() <= 0 ? defaultJumlah : jumlahPilihan;
	}

	public void setJumlahPilihan(Integer jumlahPilihan) {
		this.jumlahPilihan = jumlahPilihan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah;
	}


	@Column(name = "program", nullable = true)
	public String getProgram() {
		return program == null || program.trim().isEmpty() ? null : program.trim();
	}

	public void setProgram(String program) {
		this.program = program;
	}

	@Column(name = "angkatan", nullable = true)
	public String getAngkatan() {
		return angkatan == null ? "" : angkatan.trim();
	}

	public void setAngkatan(String angkatan) {
		this.angkatan = angkatan;
	}

	@Column(name = "untuk_siswa")
	public Boolean getUntukSiswa() {
		return untukSiswa == null ? Boolean.TRUE : untukSiswa;
	}

	public void setUntukSiswa(Boolean untukSiswa) {
		this.untukSiswa = untukSiswa;
	}

	@Column(name = "untuk_guru")
	public Boolean getUntukGuru() {
		return untukGuru == null ? Boolean.FALSE : untukGuru;
	}

	public void setUntukGuru(Boolean untukGuru) {
		this.untukGuru = untukGuru;
	}

	@Column(name = "tampil_keterangan")
	public Boolean getTampilKeterangan() {
		return tampilKeterangan == null ? Boolean.FALSE : tampilKeterangan;
	}

	public void setTampilKeterangan(Boolean tampilKeterangan) {
		this.tampilKeterangan = tampilKeterangan;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (id != null) {
			sb.append(id).append("-");
		}
		if (getKode().length() > 0) {
			sb.append(getKode()).append("-");
		}
		sb.append(isi == null ? "" : isi);
		if (getProgram() != null) {
			sb.append("-").append(getProgram());
		}
		return sb.toString();
	}
}
