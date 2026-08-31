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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.epsbed.EpsbedFrekuensiKurikulum;
import ais.database.model.epsbed.EpsbedPelaksanaanKurikulum;
import ais.database.model.epsbed.EpsbedStatus;
import ais.database.model.epsbed.EpsbedStatusAkreditasi;

/**
 * Model data untuk jenjang program studi. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code String nama}, {@code Jurusan jurusan}, {@code Jenjang
 * jenjang}, {@code Date tanggalBerdiri}; pemetaan persistence: tabel {@code public.jenjang_program_studi};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getNama()},
 * {@code getId()}, {@code getJurusan()}); mutasi data ({@code setOlehId()}, {@code setOleh()}, {@code
 * onUpdate()}, {@code setTanggal_dirubah()}, {@code setNama()}, {@code setId()}); operasi domain lain ({@code
 * toString()}, {@code appendEmail()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenjang_program_studi")
public class JenjangProgramStudi extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2314772569384463271L;

	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	// FIX LazyInitializationException: "jenjang" bisa berupa proxy Hibernate
	// lazy yang belum di-initialize saat toString() dipanggil dari luar sesi
	// (mis. via ManajemenProperty.safeToString() pada thread report/API login
	// setelah sesi ditutup). "jenjang + \"\"" memaksa toString() proxy tsb
	// tanpa cek -> LazyInitializationException mentah yg lolos ke caller.
	// Cek isInitialized dulu; jika belum, kembalikan representasi aman.
	public String toString() {
		if (jenjang == null) {
			return "";
		}
		if (!Hibernate.isInitialized(jenjang)) {
			return "";
		}
		return jenjang + "";
	}
	
	private String nama;
	
	@Column(name = "nama", length = 50)
	public String getNama() {
		if (jenjang != null) {
			nama = "-";
		}
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	private Jurusan jurusan;
	private Jenjang jenjang;

	private Date tanggalBerdiri;
	private String email;
	private String sksLulus;
	private Integer sksPerSemester;
	private Integer sksWajibLulus;
	private Integer sksPilihanLulus;
	private String status;
	private String dimulaiDariSemester;
	private String nidnKaPS;
	private String nmKaPS;
	private String telpKaPS;
	private String telpPS;
	private String faxPS;
	private String homepagePS;
	private String namaOperator;
	private String hpOperator;
	private String frekuensiKurikulum;
	private String pelaksanaanKurikulum;
	private String noSKDikti;
	private Date tglMulaiSKDikti;
	private Date tglAkhirSKDikti;
	private Date tglMulaiOperasional;
	private String noSKAkreditasi;
	private Date tglMulaiSKAkreditasi;
	private Date tglAkhirSKAkreditasi;
	private String pejabatSkBerdiri;
	private String statusAkreditasi;
	private Integer standardToefl;
	private Integer standardToafl;

	private EpsbedStatus epsbedStatus;
	private String epsbedTahunHapus;
	private EpsbedStatusAkreditasi epsbedStatusAkreditasi;
	private EpsbedFrekuensiKurikulum epsbedFrekuensiKurikulum;
	private EpsbedPelaksanaanKurikulum epsbedPelaksanaanKurikulum;

	public JenjangProgramStudi() {
	}

	public JenjangProgramStudi(Long id) {
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

	/*
	 * @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	 * 
	 * @Column(name="fakultas",nullable=false) public Fakultas getFakultas() {
	 * return fakultas; }
	 * 
	 * public void setFakultas(Fakultas fakultas) { this.fakultas = fakultas; }
	 */

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = false)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang")
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		return jenjang;
	}

	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	@Column(name = "tanggal_berdiri")
	public Date getTanggalBerdiri() {
		return tanggalBerdiri;
	}

	public void setTanggalBerdiri(Date tanggalBerdiri) {
		this.tanggalBerdiri = tanggalBerdiri;
	}

	@Column(name = "email", length = 255)
	public String getEmail() {
		if (email != null && email.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				email = email.replaceAll(",,", ",");
			}
		}
		if (email == null) {
			email = "";
		}
		if (email.trim().equals(",")) {
			email = "";
		}
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	@Column(name = "sks_lulus", length = 20)
	public String getSksLulus() {
		return sksLulus;
	}

	public void setSksLulus(String sksLulus) {
		this.sksLulus = sksLulus;
	}

	@Column(name = "status", length = 20)
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "dimulai_dari_semester", length = 20)
	public String getDimulaiDariSemester() {
		return dimulaiDariSemester;
	}

	public void setDimulaiDariSemester(String dimulaiDariSemester) {
		this.dimulaiDariSemester = dimulaiDariSemester;
	}

	@Column(name = "nidn_ka_ps", length = 50)
	public String getNidnKaPS() {
		return nidnKaPS;
	}

	public void setNidnKaPS(String nidnKaPS) {
		this.nidnKaPS = nidnKaPS;
	}

	@Column(name = "telp_ka_ps", length = 20)
	public String getTelpKaPS() {
		return telpKaPS;
	}

	public void setTelpKaPS(String telpKaPS) {
		this.telpKaPS = telpKaPS;
	}

	@Column(name = "telp_ps", length = 20)
	public String getTelpPS() {
		return telpPS;
	}

	public void setTelpPS(String telpPS) {
		this.telpPS = telpPS;
	}

	@Column(name = "fax_ps", length = 20)
	public String getFaxPS() {
		return faxPS;
	}

	public void setFaxPS(String faxPS) {
		this.faxPS = faxPS;
	}

	@Column(name = "nama_operator", length = 255)
	public String getNamaOperator() {
		return namaOperator;
	}

	public void setNamaOperator(String namaOperator) {
		this.namaOperator = namaOperator;
	}

	@Column(name = "hp_operator", length = 20)
	public String getHpOperator() {
		return hpOperator;
	}

	public void setHpOperator(String hpOperator) {
		this.hpOperator = hpOperator;
	}

	@Column(name = "frekuensi_kurikulum", length = 20)
	public String getFrekuensiKurikulum() {
		return frekuensiKurikulum;
	}

	public void setFrekuensiKurikulum(String frekuensiKurikulum) {
		this.frekuensiKurikulum = frekuensiKurikulum;
	}

	@Column(name = "pelaksanaan_kurikulum", length = 20)
	public String getPelaksanaanKurikulum() {
		return pelaksanaanKurikulum;
	}

	public void setPelaksanaanKurikulum(String pelaksanaanKurikulum) {
		this.pelaksanaanKurikulum = pelaksanaanKurikulum;
	}

	@Column(name = "no_sk_dikti", length = 20)
	public String getNoSKDikti() {
		return noSKDikti;
	}

	public void setNoSKDikti(String noSKDikti) {
		this.noSKDikti = noSKDikti;
	}

	@Column(name = "tgl_mulai_sk_dikti")
	public Date getTglMulaiSKDikti() {
		return tglMulaiSKDikti;
	}

	public void setTglMulaiSKDikti(Date tglMulaiSKDikti) {
		this.tglMulaiSKDikti = tglMulaiSKDikti;
	}

	@Column(name = "tgl_akhir_sk_dikti")
	public Date getTglAkhirSKDikti() {
		return tglAkhirSKDikti;
	}

	public void setTglAkhirSKDikti(Date tglAkhirSKDikti) {
		this.tglAkhirSKDikti = tglAkhirSKDikti;
	}

	@Column(name = "no_sk_akreditasi", length = 30)
	public String getNoSKAkreditasi() {
		return noSKAkreditasi;
	}

	public void setNoSKAkreditasi(String noSKAkreditasi) {
		this.noSKAkreditasi = noSKAkreditasi;
	}

	@Column(name = "tgl_mulai_sk_akreditasi")
	public Date getTglMulaiSKAkreditasi() {
		return tglMulaiSKAkreditasi;
	}

	public void setTglMulaiSKAkreditasi(Date tglMulaiSKAkreditasi) {
		this.tglMulaiSKAkreditasi = tglMulaiSKAkreditasi;
	}

	@Column(name = "tgl_akhir_sk_akreditasi")
	public Date getTglAkhirSKAkreditasi() {
		return tglAkhirSKAkreditasi;
	}

	public void setTglAkhirSKAkreditasi(Date tglAkhirSKAkreditasi) {
		this.tglAkhirSKAkreditasi = tglAkhirSKAkreditasi;
	}

	@Column(name = "status_akreditasi", length = 200)
	public String getStatusAkreditasi() {
		return statusAkreditasi;
	}

	public void setStatusAkreditasi(String statusAkreditasi) {
		this.statusAkreditasi = statusAkreditasi;
	}

	@Column(name = "nm_ka_ps", length = 100)
	public String getNmKaPS() {
		return nmKaPS;
	}

	public void setNmKaPS(String nmKaPS) {
		this.nmKaPS = nmKaPS;
	}

	@Column(name = "standard_toefl", length = 100)
	public Integer getStandardToefl() {
		return standardToefl;
	}

	public void setStandardToefl(Integer standardToefl) {
		this.standardToefl = standardToefl;
	}

	@Column(name = "standard_toafl", length = 100)
	public Integer getStandardToafl() {
		return standardToafl;
	}

	public void setStandardToafl(Integer standardToafl) {
		this.standardToafl = standardToafl;
	}

	public void setEpsbedStatus(EpsbedStatus epsbedStatus) {
		this.epsbedStatus = epsbedStatus;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "epsbed_status_jurusan")
	public EpsbedStatus getEpsbedStatus() {
		return epsbedStatus;
	}

	public void setEpsbedTahunHapus(String epsbedTahunHapus) {
		this.epsbedTahunHapus = epsbedTahunHapus;
	}

	@Column(name = "epsbed_tahun_hapus")
	public String getEpsbedTahunHapus() {
		return epsbedTahunHapus;
	}

	public void setEpsbedStatusAkreditasi(EpsbedStatusAkreditasi epsbedStatusAkreditasi) {
		this.epsbedStatusAkreditasi = epsbedStatusAkreditasi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "epsbed_status_akreditasi")
	public EpsbedStatusAkreditasi getEpsbedStatusAkreditasi() {
		return epsbedStatusAkreditasi;
	}

	public void setEpsbedFrekuensiKurikulum(EpsbedFrekuensiKurikulum epsbedFrekuensiKurikulum) {
		this.epsbedFrekuensiKurikulum = epsbedFrekuensiKurikulum;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "epsbed_frekuensi_kurikulum")
	public EpsbedFrekuensiKurikulum getEpsbedFrekuensiKurikulum() {
		return epsbedFrekuensiKurikulum;
	}

	public void setEpsbedPelaksanaanKurikulum(EpsbedPelaksanaanKurikulum epsbedPelaksanaanKurikulum) {
		this.epsbedPelaksanaanKurikulum = epsbedPelaksanaanKurikulum;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "epsbed_pelaksanaan_kurikulum")
	public EpsbedPelaksanaanKurikulum getEpsbedPelaksanaanKurikulum() {
		return epsbedPelaksanaanKurikulum;
	}

	@Column(name = "sks_per_semester")
	public Integer getSksPerSemester() {
		return sksPerSemester == null ? 20 : sksPerSemester;
	}

	public void setSksPerSemester(Integer sksPerSemester) {
		this.sksPerSemester = sksPerSemester;
	}

	@Temporal(TemporalType.DATE)
	public Date getTglMulaiOperasional() {
		return tglMulaiOperasional;
	}

	public void setTglMulaiOperasional(Date tglMulaiOperasional) {
		this.tglMulaiOperasional = tglMulaiOperasional;
	}

	public String getHomepagePS() {
		return homepagePS;
	}

	public void setHomepagePS(String homepagePS) {
		this.homepagePS = homepagePS;
	}

	public String getPejabatSkBerdiri() {
		return pejabatSkBerdiri;
	}

	public void setPejabatSkBerdiri(String pejabatSkBerdiri) {
		this.pejabatSkBerdiri = pejabatSkBerdiri;
	}

	public Integer getSksWajibLulus() {
		return sksWajibLulus == null ? 0 : sksWajibLulus;
	}

	public void setSksWajibLulus(Integer sksWajibLulus) {
		this.sksWajibLulus = sksWajibLulus;
	}

	public Integer getSksPilihanLulus() {
		return sksPilihanLulus == null ? 0 : sksPilihanLulus;
	}

	public void setSksPilihanLulus(Integer sksPilihanLulus) {
		this.sksPilihanLulus = sksPilihanLulus;
	}

}
