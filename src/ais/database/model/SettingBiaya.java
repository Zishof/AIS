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
	 * Daftar NIM yang tidak boleh memakai setting biaya ini. Format NIM biasa berlaku untuk
	 * seluruh semester; format {@code NIM:SMT_MULAI:SMT_SAMPAI} hanya berlaku pada rentang
	 * semester tersebut. Antarentri dapat dipisahkan koma, titik koma, spasi, tab, atau baris baru.
	 */
	@Column(name = "pengecualian_mahasiswa", columnDefinition = "text")
	public String getPengecualianMahasiswa() {
		return pengecualianMahasiswa == null ? "" : pengecualianMahasiswa.trim();
	}

	public void setPengecualianMahasiswa(String pengecualianMahasiswa) {
		this.pengecualianMahasiswa = pengecualianMahasiswa == null ? null : pengecualianMahasiswa.trim();
	}

	/**
	 * Memvalidasi format sebelum disimpan dari UI. Nilai kosong sah karena berarti tidak ada
	 * pengecualian. Method ini sengaja tidak dipanggil dari setter agar data legacy yang pernah
	 * tersimpan tidak membuat Hibernate gagal memuat entity.
	 */
	public static void validasiFormatPengecualianMahasiswa(String daftar) {
		if (daftar == null || daftar.trim().length() == 0) {
			return;
		}
		String nilai = daftar.trim().replaceAll("\\s*:\\s*", ":");
		String[] entri = nilai.split("[\\s,;]+");
		for (int i = 0; i < entri.length; i++) {
			String item = entri[i] == null ? "" : entri[i].trim();
			if (item.length() == 0) {
				continue;
			}
			String[] bagian = item.split(":", -1);
			if (bagian.length == 1) {
				continue;
			}
			if (bagian.length != 3 || bagian[0].trim().length() == 0
					|| bagian[1].trim().length() == 0 || bagian[2].trim().length() == 0) {
				throw new IllegalArgumentException("Format pengecualian tidak valid pada '" + item
						+ "'. Gunakan NIM atau NIM:SMT_MULAI:SMT_SAMPAI.");
			}
			try {
				int semesterMulai = Integer.parseInt(bagian[1].trim());
				int semesterSampai = Integer.parseInt(bagian[2].trim());
				if (semesterMulai < 1 || semesterSampai < semesterMulai) {
					throw new IllegalArgumentException("Rentang semester tidak valid pada '" + item
							+ "'. Semester mulai minimal 1 dan tidak boleh melebihi semester sampai.");
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Semester pada '" + item + "' harus berupa angka.");
			}
		}
	}

	/** Pemeriksaan kompatibel format lama; entri berentang diabaikan bila semester tidak tersedia. */
	public boolean isMahasiswaDikecualikan(String nim) {
		return isMahasiswaDikecualikan(nim, null);
	}

	/** Pemeriksaan tunggal agar seluruh jalur billing memakai aturan pengecualian semester yang sama. */
	public boolean isMahasiswaDikecualikan(String nim, Integer semesterAktif) {
		if (nim == null || nim.trim().length() == 0 || getPengecualianMahasiswa().length() == 0) {
			return false;
		}
		String nimDicari = nim.trim();
		String nilai = getPengecualianMahasiswa().replaceAll("\\s*:\\s*", ":");
		String[] daftarNim = nilai.split("[\\s,;]+");
		for (int i = 0; i < daftarNim.length; i++) {
			String[] bagian = daftarNim[i].trim().split(":", -1);
			if (bagian.length == 1 && nimDicari.equalsIgnoreCase(bagian[0].trim())) {
				return true;
			}
			if (bagian.length == 3 && nimDicari.equalsIgnoreCase(bagian[0].trim())
					&& semesterAktif != null) {
				try {
					int semesterMulai = Integer.parseInt(bagian[1].trim());
					int semesterSampai = Integer.parseInt(bagian[2].trim());
					if (semesterAktif.intValue() >= semesterMulai
							&& semesterAktif.intValue() <= semesterSampai) {
						return true;
					}
				} catch (NumberFormatException e) {
					// Data legacy salah format tidak boleh menghentikan proses penagihan.
				}
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
