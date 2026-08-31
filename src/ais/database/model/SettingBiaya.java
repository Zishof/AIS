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

/**
 * Model data untuk setting biaya. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code JenisKegiatan jenisKegiatan}, {@code Integer angkatan},
 * {@code Jenjang jenjang}, {@code StatusAwalMahasiswa statusAwalMahasiswa}; pemetaan persistence: tabel {@code
 * public.setting_biaya}; inisialisasi/lifecycle ({@code getKhususBuatMahasiswaTertentu()}, {@code
 * setKhususBuatMahasiswaTertentu()}); pembacaan/pencarian ({@code getOlehId()}, {@code getOleh()}, {@code
 * getTanggal_dirubah()}, {@code getId()}, {@code getAngkatan()}, {@code getJenisKegiatan()}); mutasi data
 * ({@code setOlehId()}, {@code setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setId()},
 * {@code setAngkatan()}); operasi domain lain ({@code toString()}, {@code isMahasiswaDikecualikan()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "setting_biaya")
public class SettingBiaya extends GeneralValueObject {
	private static final long serialVersionUID = -7050466125892447098L;
	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		jenisKegiatan = getJenisKegiatan();
		jenjang = getJenjang();
		statusAwalMahasiswa = getStatusAwalMahasiswa();
		program = getProgram();
		jurusan = getJurusan();
		gelombangPendaftaran = getGelombangPendaftaran();
		return jenisKegiatan + "_" + angkatan + "_" + jenjang + "_" + statusAwalMahasiswa + "_" + program + "_"
				+ jurusan + "_" + gelombangPendaftaran;
	}

	private JenisKegiatan jenisKegiatan;
	private Integer angkatan;
	private Jenjang jenjang;
	private StatusAwalMahasiswa statusAwalMahasiswa;
	private StatusMahasiswa statusMahasiswa;
	private String program;
	private JenisSeleksi jenisSeleksi;
	private Jurusan jurusan;
	private Boolean gunakanBiayaDefault;
	private Boolean tampilkanPerProdi;
	private GelombangPendaftaran gelombangPendaftaran;
	private Paket paket;
	private Integer minSmt;
	private Integer maxSmt;
	// Bila true: rentang semester (minSmt/maxSmt) DIATUR DI SINI (SettingBiaya),
	// dan rentang semester (minSmt/maxSmt) milik JenisKegiatan TIDAK BERLAKU utk setting ini.
	private Boolean smtIkutiSettinganDisini;
	private Integer jumlahPembayaran;
	private Boolean khususBuatMahasiswaTertentu;
	private Boolean batasiMahasiswaTertentu;
	private String templateNotifikasi;
	private Date waktuNotifikasi;
	private Boolean aktifkanNotifikasi;
	private Date batasWaktuPembayaran;
	private String kelamin;
	private AfiliasiCalonMahasiswa afiliasiCalonMahasiswa;
	private Integer ta;
	private String semester;
	private String tahunAkademik;
	private String pengecualianMahasiswa;
	private Integer prioritas = 10;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "angkatan")
	public Integer getAngkatan() {
//		if (getKhususBuatMahasiswaTertentu()) {
//			angkatan = null;
//		}
		return this.angkatan;
	}

	public void setAngkatan(Integer angkatan) {
		this.angkatan = angkatan;
	}

	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kegiatan", nullable = true)
	public JenisKegiatan getJenisKegiatan() {
		jenisKegiatan = check(jenisKegiatan);
		return jenisKegiatan;
	}

	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang", nullable = true)
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		if (getKhususBuatMahasiswaTertentu()) {
			jenjang = null;
		}
		return jenjang;
	}

	public Boolean getGunakanBiayaDefault() {
		if (getKhususBuatMahasiswaTertentu()) {
			gunakanBiayaDefault = true;
		}
		return gunakanBiayaDefault == null ? true : gunakanBiayaDefault;
	}

	public void setGunakanBiayaDefault(Boolean gunakanBiayaDefault) {
		this.gunakanBiayaDefault = gunakanBiayaDefault;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswa() {
		statusAwalMahasiswa = check(statusAwalMahasiswa);
		if (getKhususBuatMahasiswaTertentu()) {
			statusAwalMahasiswa = null;
		}
		return statusAwalMahasiswa;
	}

	public void setStatusAwalMahasiswa(StatusAwalMahasiswa statusAwalMahasiswa) {
		this.statusAwalMahasiswa = statusAwalMahasiswa;
	}

	public String getProgram() {
		if (getKhususBuatMahasiswaTertentu()) {
			program = null;
		}
		return program == null || program.trim().isEmpty() ? null : program;
	}

	public void setProgram(String program) {
		this.program = program;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_seleksi", nullable = true)
	public JenisSeleksi getJenisSeleksi() {
		jenisSeleksi = check(jenisSeleksi);
		if (getKhususBuatMahasiswaTertentu()) {
			jenisSeleksi = null;
		}
		return jenisSeleksi;
	}

	public void setJenisSeleksi(JenisSeleksi jenisSeleksi) {
		this.jenisSeleksi = jenisSeleksi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		if (getKhususBuatMahasiswaTertentu()) {
			jurusan = null;
		}
		return jurusan;
	}

	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	public Integer getMinSmt() {
		return minSmt == null ? 0 : minSmt;
	}

	public void setMinSmt(Integer minSmt) {
		this.minSmt = minSmt;
	}

	public Integer getMaxSmt() {
		if (maxSmt == null) {
			maxSmt = 30;
		}
		return maxSmt;
	}

	public void setMaxSmt(Integer maxSmt) {
		this.maxSmt = maxSmt;
	}

	/**
	 * Bila {@code true}, rentang Minimal/Maksimal Semester pada SettingBiaya ini yang dipakai,
	 * dan rentang semester (minSmt/maxSmt) pada {@link JenisKegiatan} TIDAK diberlakukan untuk
	 * setting ini. Bila {@code false} (default), perilaku lama dipertahankan: rentang JenisKegiatan
	 * tetap menjadi penentu.
	 */
	public Boolean getSmtIkutiSettinganDisini() {
		return smtIkutiSettinganDisini == null ? false : smtIkutiSettinganDisini;
	}

	public void setSmtIkutiSettinganDisini(Boolean smtIkutiSettinganDisini) {
		this.smtIkutiSettinganDisini = smtIkutiSettinganDisini;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran", nullable = true)
	public GelombangPendaftaran getGelombangPendaftaran() {
		gelombangPendaftaran = check(gelombangPendaftaran);
		if (getKhususBuatMahasiswaTertentu()) {
			gelombangPendaftaran = null;
		}
		return gelombangPendaftaran;
	}

	public void setGelombangPendaftaran(GelombangPendaftaran gelombangPendaftaran) {
		this.gelombangPendaftaran = gelombangPendaftaran;
	}

	public Boolean getKhususBuatMahasiswaTertentu() {
		return khususBuatMahasiswaTertentu == null ? false : khususBuatMahasiswaTertentu;
	}

	public void setKhususBuatMahasiswaTertentu(Boolean khususBuatMahasiswaTertentu) {
		this.khususBuatMahasiswaTertentu = khususBuatMahasiswaTertentu;
	}

	/**
	 * Membatasi setting biaya umum/bulanan hanya untuk mahasiswa yang tercatat pada
	 * SettingBiayaDetail. Berbeda dengan khususBuatMahasiswaTertentu, flag ini tidak
	 * mengubah tagihan menjadi nilai default/insidentil dan tetap memakai billing bulanan.
	 */
	@Column(name = "batasi_mahasiswa_tertentu")
	public Boolean getBatasiMahasiswaTertentu() {
		return batasiMahasiswaTertentu == null ? false : batasiMahasiswaTertentu;
	}

	public void setBatasiMahasiswaTertentu(Boolean batasiMahasiswaTertentu) {
		this.batasiMahasiswaTertentu = batasiMahasiswaTertentu;
	}

	/**
	 * Daftar NIM yang tidak boleh memakai setting biaya ini. Pemisah yang didukung:
	 * koma, titik koma, spasi, tab, atau baris baru.
	 */
	@Column(name = "pengecualian_mahasiswa", columnDefinition = "text")
	public String getPengecualianMahasiswa() {
		return pengecualianMahasiswa == null ? "" : pengecualianMahasiswa.trim();
	}

	public void setPengecualianMahasiswa(String pengecualianMahasiswa) {
		this.pengecualianMahasiswa = pengecualianMahasiswa == null ? null : pengecualianMahasiswa.trim();
	}

	/**
	 * Urutan pemilihan Setting Biaya. Angka yang lebih kecil didahulukan,
	 * sedangkan nilai 10 menjaga perilaku seluruh data lama dan data baru.
	 */
	@Column(name = "prioritas")
	public Integer getPrioritas() {
		return prioritas == null ? Integer.valueOf(10) : prioritas;
	}

	public void setPrioritas(Integer prioritas) {
		this.prioritas = prioritas == null ? Integer.valueOf(10) : prioritas;
	}

	/** Pemeriksaan tunggal agar seluruh jalur billing memakai aturan pengecualian yang sama. */
	public boolean isMahasiswaDikecualikan(String nim) {
		if (nim == null || nim.trim().length() == 0 || getPengecualianMahasiswa().length() == 0) {
			return false;
		}
		String nimDicari = nim.trim();
		String[] daftarNim = getPengecualianMahasiswa().split("[\\s,;]+");
		for (int i = 0; i < daftarNim.length; i++) {
			if (nimDicari.equalsIgnoreCase(daftarNim[i].trim())) {
				return true;
			}
		}
		return false;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket", nullable = true)
	public Paket getPaket() {
		paket = check(paket);
		if (getKhususBuatMahasiswaTertentu()) {
			paket = null;
		}
		return paket;
	}

	public void setPaket(Paket paket) {
		this.paket = paket;
	}

	public Integer getJumlahPembayaran() {
		return jumlahPembayaran == null || jumlahPembayaran < 1 ? 1 : jumlahPembayaran;
	}

	public void setJumlahPembayaran(Integer jumlahPembayaran) {
		this.jumlahPembayaran = jumlahPembayaran;
	}

	public Boolean getTampilkanPerProdi() {
		return tampilkanPerProdi == null ? false : tampilkanPerProdi;
	}

	public void setTampilkanPerProdi(Boolean tampilkanPerProdi) {
		this.tampilkanPerProdi = tampilkanPerProdi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_mahasiswa", nullable = true)
	public StatusMahasiswa getStatusMahasiswa() {
		statusMahasiswa = check(statusMahasiswa);
		return statusMahasiswa;
	}

	public void setStatusMahasiswa(StatusMahasiswa statusMahasiswa) {
		this.statusMahasiswa = statusMahasiswa;
	}

	public Boolean getAktifkanNotifikasi() {
		return aktifkanNotifikasi == null ? false : aktifkanNotifikasi;
	}

	public void setAktifkanNotifikasi(Boolean aktifkanNotifikasi) {
		this.aktifkanNotifikasi = aktifkanNotifikasi;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuNotifikasi() {
		if (!getAktifkanNotifikasi()) {
			waktuNotifikasi = null;
		}
		return waktuNotifikasi;
	}

	public void setWaktuNotifikasi(Date waktuNotifikasi) {
		this.waktuNotifikasi = waktuNotifikasi;
	}

	@Column(name = "template_notifikasi", columnDefinition = "text")
	public String getTemplateNotifikasi() {
		if (!getAktifkanNotifikasi()) {
			templateNotifikasi = null;
		}
		return templateNotifikasi == null ? "" : templateNotifikasi.trim();
	}

	public void setTemplateNotifikasi(String templateNotifikasi) {
		this.templateNotifikasi = templateNotifikasi;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getBatasWaktuPembayaran() {
		return batasWaktuPembayaran;
	}

	public void setBatasWaktuPembayaran(Date batasWaktuPembayaran) {
		this.batasWaktuPembayaran = batasWaktuPembayaran;
	}

	public String getKelamin() {
		if (kelamin != null && !(kelamin.equals("Laki-laki") || kelamin.equals("Perempuan"))) {
			kelamin = null;
		}
		return kelamin;
	}

	public void setKelamin(String kelamin) {
		this.kelamin = kelamin;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "afiliasi_calon_mahasiswa", nullable = true)
	public AfiliasiCalonMahasiswa getAfiliasiCalonMahasiswa() {
		afiliasiCalonMahasiswa = check(afiliasiCalonMahasiswa);
		return afiliasiCalonMahasiswa;
	}

	public void setAfiliasiCalonMahasiswa(AfiliasiCalonMahasiswa afiliasiCalonMahasiswa) {
		this.afiliasiCalonMahasiswa = afiliasiCalonMahasiswa;
	}

	public String getTahunAkademik() {
		return tahunAkademik;
	}

	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	public String getSemester() {
		return semester;
	}

	public void setSemester(String semester) {
		this.semester = semester;
	}

	public Integer getTa() {
		String id_smt = (getTahunAkademik() == null || getTahunAkademik().trim().isEmpty() ? "0"
				: getTahunAkademik().split("/")[0])
				+ (getSemester() == null || getSemester().trim().isEmpty() ? "0"
						: getSemester().equals(Perkuliahan.GENAP) ? "2" : "1");
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/SettingBiaya.java:403");

		}
		if (ta == null) {
			ta = 0;
		}
		return ta;
	}

	public void setTa(Integer ta) {
		this.ta = ta;
	}
}
