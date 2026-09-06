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

/**
 * Entitas Hibernate untuk tabel {@code public.angket_penilaian_dosen}, merepresentasikan satu
 * butir/item pertanyaan pada angket (kuesioner) penilaian kinerja dosen — dipakai antara lain
 * dalam proses sertifikasi dosen di mana mahasiswa atau dosen sendiri mengisi checklist penilaian
 * (lihat entitas terkait {@link ChecklistPenilaianDosen} dan
 * {@link ChecklistPenilaianDosenOlehMahasiswa}). Setiap butir angket dapat dibatasi cakupannya ke
 * {@link #getFakultas()} dan/atau {@link #getJurusan()} tertentu (keduanya {@code @ManyToOne}
 * lazy, opsional/nullable — bila kosong berarti berlaku umum), serta ke program studi/angkatan
 * tertentu via field teks {@link #getProgram()}/{@link #getAngkatan()}. Flag
 * {@link #getUntukDosen()} dan {@link #getUntukMahasiswa()} menentukan target pengisi angket
 * (dosen menilai diri sendiri/rekan, atau mahasiswa menilai dosen).
 * <p>
 * {@link #getPetunjuk()} dan {@link #getJumlahPilihan()} memiliki nilai default yang diambil dari
 * {@link ais.common.Common#getKonfigurasi(String, String)} bila field lokal kosong, sehingga
 * petunjuk pengisian dan skala penilaian (default 1-5) dapat diatur secara global lewat
 * konfigurasi aplikasi tanpa mengubah tiap baris data.
 * <p>
 * Perubahan tercatat historisnya lewat {@link Audited} (Hibernate Envers).
 */
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
	/** Teks pertanyaan/pernyataan butir angket. */
	private String isi;
	private String keterangan;
	/** Petunjuk pengisian angket; bila kosong diambil dari konfigurasi aplikasi (lihat {@link #getPetunjuk()}). */
	private String petunjuk;
	/** Jumlah pilihan/skala skor penilaian (mis. skala 1-5); bila kosong diambil dari konfigurasi aplikasi. */
	private Integer jumlahPilihan;
	/** Fakultas cakupan berlakunya butir angket ini; kosong berarti berlaku untuk semua fakultas. */
	private Fakultas fakultas;
	/** Jurusan cakupan berlakunya butir angket ini; kosong berarti berlaku untuk semua jurusan. */
	private Jurusan jurusan;
	/** Program studi cakupan berlakunya butir angket ini (teks bebas, bukan relasi). */
	private String program;
	/** Angkatan mahasiswa cakupan berlakunya butir angket ini. */
	private String angkatan;
	/** Menandakan butir ini berlaku untuk penilaian oleh/terhadap dosen. */
	private Boolean untukDosen;
	/** Menandakan butir ini berlaku untuk penilaian oleh mahasiswa. */
	private Boolean untukMahasiswa;
	/** Menentukan apakah kolom keterangan ditampilkan saat pengisian angket. */
	private Boolean tampilKeterangan;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public AngketPenilaianDosen() {
	}

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa
	 * nilai lama) — write-guard satu-arah, konsisten dengan pola arsip lain.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId.trim();
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa
	 * nilai lama) — write-guard satu-arah, konsisten dengan pola arsip lain.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh.trim();
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah}
	 * setiap kali baris ini di-update. Field ini adalah kebutuhan teknis, bukan bug.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas gabungan id, kode, isi, nama fakultas/jurusan
	 *         (bila ada), dan program studi — dipakai untuk keperluan log/debug.
	 */
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

	/**
	 * @return id unik baris (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return teks pertanyaan/pernyataan butir angket ini.
	 */
	@Column(name = "isi", nullable = false, columnDefinition = "text")
	public String getIsi() {
		return this.isi;
	}

	/**
	 * @param isi teks pertanyaan/pernyataan butir angket.
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/**
	 * @return keterangan tambahan (opsional) untuk butir angket ini; ditampilkan
	 *         saat pengisian bila {@link #getTampilKeterangan()} bernilai true.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan tambahan (opsional) untuk butir angket.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return kode singkat butir angket (mis. untuk pengelompokan kategori penilaian).
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * @param kode kode singkat butir angket.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return petunjuk pengisian angket. Bila field lokal {@link #petunjuk} kosong,
	 *         nilai diambil dari konfigurasi aplikasi
	 *         {@code keterangan_checklist_penilaian_dosen_oleh_mahasiswa} (dengan teks
	 *         default bawaan) sehingga petunjuk dapat diatur secara global tanpa
	 *         mengubah data per baris; mengembalikan string kosong bila konfigurasi
	 *         gagal diambil.
	 */
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

	/**
	 * @param petunjuk petunjuk pengisian angket (kosongkan untuk memakai nilai
	 *                 default dari konfigurasi aplikasi, lihat {@link #getPetunjuk()}).
	 */
	public void setPetunjuk(String petunjuk) {
		this.petunjuk = petunjuk;
	}

	/**
	 * @return jumlah pilihan/skala skor penilaian (mis. skala 1-5). Bila field lokal
	 *         {@link #jumlahPilihan} kosong atau tidak positif, nilai diambil dari
	 *         konfigurasi aplikasi {@code jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa}
	 *         (default 5), sehingga skala penilaian dapat diatur secara global.
	 */
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

	/**
	 * @param jumlahPilihan jumlah pilihan/skala skor penilaian (kosongkan/nol atau
	 *                      negatif untuk memakai nilai default dari konfigurasi
	 *                      aplikasi, lihat {@link #getJumlahPilihan()}).
	 */
	public void setJumlahPilihan(Integer jumlahPilihan) {
		this.jumlahPilihan = jumlahPilihan;
	}

	/**
	 * @param fakultas fakultas cakupan berlakunya butir angket ini (kosongkan
	 *                 untuk berlaku bagi semua fakultas).
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return fakultas cakupan berlakunya butir angket ini (relasi lazy,
	 *         di-refresh/divalidasi via {@code check(...)} saat dibaca); null
	 *         berarti berlaku untuk semua fakultas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * @param jurusan jurusan cakupan berlakunya butir angket ini (kosongkan
	 *                untuk berlaku bagi semua jurusan).
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return jurusan cakupan berlakunya butir angket ini (relasi lazy,
	 *         di-refresh/divalidasi via {@code check(...)} saat dibaca); null
	 *         berarti berlaku untuk semua jurusan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * @return program studi cakupan berlakunya butir angket ini (teks bebas), di-trim
	 *         saat dibaca; null bila belum diisi/hanya spasi.
	 */
	public String getProgram() {
		return program == null || program.trim().isEmpty() ? null : program.trim();
	}

	/**
	 * @param program program studi cakupan berlakunya butir angket (teks bebas).
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * @return angkatan mahasiswa cakupan berlakunya butir angket ini, di-trim saat
	 *         dibaca; null bila belum diisi/hanya spasi.
	 */
	public String getAngkatan() {
		return angkatan == null || angkatan.trim().isEmpty() ? null : angkatan.trim();
	}

	/**
	 * @param angkatan angkatan mahasiswa cakupan berlakunya butir angket.
	 */
	public void setAngkatan(String angkatan) {
		this.angkatan = angkatan;
	}

	/**
	 * @return true bila butir ini berlaku untuk penilaian oleh/terhadap dosen;
	 *         default false bila belum diisi.
	 */
	public Boolean getUntukDosen() {
		return untukDosen == null ? Boolean.FALSE : untukDosen;
	}

	/**
	 * @param untukDosen tandai butir ini berlaku untuk penilaian oleh/terhadap dosen.
	 */
	public void setUntukDosen(Boolean untukDosen) {
		this.untukDosen = untukDosen;
	}

	/**
	 * @return true bila butir ini berlaku untuk penilaian oleh mahasiswa; default
	 *         true bila belum diisi.
	 */
	public Boolean getUntukMahasiswa() {
		return untukMahasiswa == null ? Boolean.TRUE : untukMahasiswa;
	}

	/**
	 * @param untukMahasiswa tandai butir ini berlaku untuk penilaian oleh mahasiswa.
	 */
	public void setUntukMahasiswa(Boolean untukMahasiswa) {
		this.untukMahasiswa = untukMahasiswa;
	}

	/**
	 * @return true bila kolom keterangan ditampilkan saat pengisian angket; default
	 *         true bila belum diisi.
	 */
	public Boolean getTampilKeterangan() {
		return tampilKeterangan == null ? Boolean.TRUE : tampilKeterangan;
	}

	/**
	 * @param tampilKeterangan tampilkan/sembunyikan kolom keterangan saat pengisian angket.
	 */
	public void setTampilKeterangan(Boolean tampilKeterangan) {
		this.tampilKeterangan = tampilKeterangan;
	}
}
