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

import org.hibernate.envers.Audited;

import ais.common.Common;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "angket_penilaian_dosen")
public class AngketPenilaianDosen extends GeneralValueObject {

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
	private Fakultas fakultas;
	private Jurusan jurusan;
	private String program;
	private String angkatan;
	private Boolean untukDosen;
	private Boolean untukMahasiswa;
	private Boolean tampilKeterangan;

	public AngketPenilaianDosen() {
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
		StringBuilder sb = new StringBuilder();
		sb.append(id == null ? "" : id.toString()).append("-").append(kode == null ? "" : kode).append("-")
				.append(isi == null ? "" : isi);
		Fakultas f = getFakultas();
		Jurusan j = getJurusan();
		if (f != null && f.getNama() != null) {
			sb.append("-").append(f.getNama());
		}
		if (j != null && j.getNama() != null) {
			sb.append("-").append(j.getNama());
		}
		if (program != null && !program.trim().isEmpty()) {
			sb.append("-").append(program.trim());
		}
		return sb.toString();
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

	public String getKode() {
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "petunjuk", columnDefinition = "text")
	public String getPetunjuk() {
		if (petunjuk != null && !petunjuk.trim().isEmpty()) {
			return petunjuk.trim();
		}
		try {
			return Common.getKonfigurasi("keterangan_checklist_penilaian_dosen_oleh_mahasiswa",
					"Sesuai dengan yang Saudara ketahui, berilah penilaian secara jujur, objektif, dan penuh tanggung jawab terhadap dosen Saudara. Informasi yang Saudara berikan hanya akan dipergunakan dalam proses sertifikasi dosen dan tidak akan berpengaruh terhadap status Saudara sebagai mahasiswa. Penilaian dilakukan terhadap aspek-aspek dalam tabel berikut dengan cara memilih angka (1-5) pada kolom skor.  "
							+ "\n 1 = sangat tidak baik/sangat rendah/tidak pernah"
							+ "\n 2 = tidak baik/rendah/jarang " + "\n 3 = biasa/cukup/kadang-kadang "
							+ "\n 4 = baik/tinggi/sering " + "\n 5 = sangat baik/sangat tinggi/selalu")
					.getNilai();
		} catch (Exception e) {
			return "";
		}
	}

	public void setPetunjuk(String petunjuk) {
		this.petunjuk = petunjuk;
	}

	public Integer getJumlahPilihan() {
		int jumlahChecklist = 5;
		try {
			jumlahChecklist = Integer.parseInt(Common
					.getKonfigurasi("jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa", "5").getNilai().trim());
		} catch (Exception e) {
			jumlahChecklist = 5;
		}
		if (jumlahChecklist <= 0) {
			jumlahChecklist = 5;
		}
		return jumlahPilihan == null || jumlahPilihan.intValue() <= 0 ? Integer.valueOf(jumlahChecklist) : jumlahPilihan;
	}

	public void setJumlahPilihan(Integer jumlahPilihan) {
		this.jumlahPilihan = jumlahPilihan;
	}

	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	public String getProgram() {
		return program == null || program.trim().isEmpty() ? null : program.trim();
	}

	public void setProgram(String program) {
		this.program = program;
	}

	public String getAngkatan() {
		return angkatan == null || angkatan.trim().isEmpty() ? null : angkatan.trim();
	}

	public void setAngkatan(String angkatan) {
		this.angkatan = angkatan;
	}

	public Boolean getUntukDosen() {
		return untukDosen == null ? Boolean.FALSE : untukDosen;
	}

	public void setUntukDosen(Boolean untukDosen) {
		this.untukDosen = untukDosen;
	}

	public Boolean getUntukMahasiswa() {
		return untukMahasiswa == null ? Boolean.TRUE : untukMahasiswa;
	}

	public void setUntukMahasiswa(Boolean untukMahasiswa) {
		this.untukMahasiswa = untukMahasiswa;
	}

	public Boolean getTampilKeterangan() {
		return tampilKeterangan == null ? Boolean.TRUE : tampilKeterangan;
	}

	public void setTampilKeterangan(Boolean tampilKeterangan) {
		this.tampilKeterangan = tampilKeterangan;
	}
}
