package ais.database.model;

/*
 * author: Zulkifli, April 17, 2010
 */

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.Date;
import java.util.Map;

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
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DisposisiSop;

@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "perguruan_tinggi")
public class PerguruanTinggi extends VoKunci {
	private static final long serialVersionUID = -7550455125892447098L;
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

	public String toString() {
		return nama;
	}

	private String kodeYayasan;
	private String kodePerguruanTinggi;
	private String nama;
	private String namaSingkat;

	private String alamat1;
	private String alamat2;

	private String dusun;
	private String kelurahan;
	private String rt;
	private String rw;
	private String kota;
	private String propinsi;
	private String kodePos;
	private String telepon;
	private String faksimili;

	private Date tanggalAkta;
	private Date tanggalAwalPendirian;
	private String nomorAkta;
	private String status;
	private String email;
	private String website;
	private String domain;
	private String motto;
	private String css;
	private String kodeSinta;
	private String piilhanTampilan;

	public static final String TAMPILAN_DEFAULT = "default";
	public static final String TAMPILAN_KLASIK = "klasik";
	public static final String TAMPILAN_BARU = "baru";

	private Double luasTanahTotal;
	private Double luasKebunLahanPercobaanTotal;
	private Double luasTotalRuangKuliah;
	private Integer jumlahRuangKuliah;
	private Double luasTotalLabStudio;
	private Integer jumlahRuangLab;
	private Double luasTotalRuangDosenTetap;
	private Double luasTotalRuangAdministrasi;
	private Double luasTotalRuangSeminar;
	private Double luasTotalRuangEkskul;
	private Double luasTotalPusatKomputer;
	private Double luasTotalRuangPerpustakaan;
	private Integer jumlahJudulBuku;
	private Integer jumlahEksemplarBuku;
	private String deskripsi;

	private String skIzinOperasi;
	private Date tglSkIzinOperasi;
	private String pejabatIzinOperasi;
	private String noRek;
	private String nmBank;
	private String unitCabang;
	private String nmRek;
	private Double luasTanahMilik;
	private Double luasTanahBukanMilik;

	private Integer tahunPertamaMenerimaMahasiswa;

	private String peringkatAkreditasi;
	private String akreditasi;
	private String noSkAkreditasi;
	private Date tanggalAkreditasi;

	private Boolean aktif;
	private String rektor;
	private String rektorNip;
	private String feeder;
	private SatuanKerja satuanKerja;
	private Pegawai kepala;
	private Pegawai wakil1;
	private Pegawai wakil2;
	private Pegawai wakil3;

	private Pegawai pejabat1;
	private Pegawai pejabat2;
	private Pegawai pejabat3;
	private Pegawai pejabat4;
	private Pegawai pejabat5;

	private String wa;
	private Boolean dosenHarusPakaiSatuanKerja;
	private String labelPejabat1;
	private String labelPejabat2;
	private String labelPejabat3;
	private String labelPejabat4;
	private String labelPejabat5;
	private Pendaftar pendaftar;
	private String headerpmb;
	private Tbmuser dikunci;
	private DisposisiSop disposisiSop;

	public PerguruanTinggi() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	@Column(name = "kode_yayasan", nullable = false, length = 50)
	public String getKodeYayasan() {
		if (getPendaftar() != null) {
			kodeYayasan = pendaftar.getKode();
		}
		return this.kodeYayasan;
	}

	public void setKodeYayasan(String kodeYayasan) {
		this.kodeYayasan = kodeYayasan;
	}

	@Column(name = "kode_perguruan_tinggi", nullable = false, length = 50)
	public String getKodePerguruanTinggi() {
		if (getPendaftar() != null) {
			kodePerguruanTinggi = pendaftar.getKode();
		}
		return this.kodePerguruanTinggi;
	}

	public void setKodePerguruanTinggi(String kodePerguruanTinggi) {
		this.kodePerguruanTinggi = kodePerguruanTinggi;
	}

	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {

		if (getPendaftar() != null) {
			nama = pendaftar.getNama();
		}

		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "alamat1", nullable = false, length = 150)
	public String getAlamat1() {
		if (getPendaftar() != null) {
			alamat1 = pendaftar.getAlamat();
		}
		return this.alamat1 == null ? "" : this.alamat1.trim();
	}

	public void setAlamat1(String alamat1) {
		this.alamat1 = alamat1;
	}

	@Column(name = "alamat2", length = 150)
	public String getAlamat2() {
		return this.alamat2 == null ? "" : this.alamat2.trim();
	}

	public void setAlamat2(String alamat2) {
		this.alamat2 = alamat2;
	}

	@Column(name = "kota", length = 50)
	public String getKota() {
		return this.kota;
	}

	public void setKota(String kota) {
		this.kota = kota;
	}

	@Column(name = "kode_pos", length = 10)
	public String getKodePos() {
		return this.kodePos;
	}

	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	@Column(name = "telepon", length = 50)
	public String getTelepon() {
		if (getPendaftar() != null) {
			telepon = pendaftar.getTelp();
		}
		return this.telepon == null ? "" : telepon.trim();
	}

	public void setTelepon(String telepon) {
		this.telepon = telepon;
	}

	@Column(name = "faksimili", length = 50)
	public String getFaksimili() {
		return this.faksimili;
	}

	public void setFaksimili(String faksimili) {
		this.faksimili = faksimili;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_akta", length = 0)
	public Date getTanggalAkta() {
		return this.tanggalAkta;
	}

	public void setTanggalAkta(Date tanggalAkta) {
		this.tanggalAkta = tanggalAkta;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_awal_pendirian", length = 0)
	public Date getTanggalAwalPendirian() {
		return this.tanggalAwalPendirian;
	}

	public void setTanggalAwalPendirian(Date tanggalAwalPendirian) {
		this.tanggalAwalPendirian = tanggalAwalPendirian;
	}

	@Column(name = "nomor_akta", length = 30)
	public String getNomorAkta() {
		return this.nomorAkta;
	}

	public void setNomorAkta(String nomorAkta) {
		this.nomorAkta = nomorAkta;
	}

	@Column(name = "email", length = 255)
	public String getEmail() {

		if (getPendaftar() != null) {
			email = pendaftar.getEmail();
		}

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
		return this.email==null?"":email;
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

	@Column(name = "website", length = 150)
	public String getWebsite() {
		return this.website == null ? "" : this.website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	@Column(name = "luas_tanah_total")
	public Double getLuasTanahTotal() {
		return luasTanahTotal;
	}

	public void setLuasTanahTotal(Double luasTanahTotal) {
		this.luasTanahTotal = luasTanahTotal;
	}

	@Column(name = "luas_kebun_lahan_percobaan_total")
	public Double getLuasKebunLahanPercobaanTotal() {
		return luasKebunLahanPercobaanTotal;
	}

	public void setLuasKebunLahanPercobaanTotal(Double luasKebunLahanPercobaanTotal) {
		this.luasKebunLahanPercobaanTotal = luasKebunLahanPercobaanTotal;
	}

	@Column(name = "luas_total_ruang_kuliah")
	public Double getLuasTotalRuangKuliah() {
		return luasTotalRuangKuliah;
	}

	public void setLuasTotalRuangKuliah(Double luasTotalRuangKuliah) {
		this.luasTotalRuangKuliah = luasTotalRuangKuliah;
	}

	@Column(name = "jumlah_ruang_kuliah")
	public Integer getJumlahRuangKuliah() {
		return jumlahRuangKuliah;
	}

	public void setJumlahRuangKuliah(Integer jumlahRuangKuliah) {
		this.jumlahRuangKuliah = jumlahRuangKuliah;
	}

	@Column(name = "luas_total_lab_studio")
	public Double getLuasTotalLabStudio() {
		return luasTotalLabStudio;
	}

	public void setLuasTotalLabStudio(Double luasTotalLabStudio) {
		this.luasTotalLabStudio = luasTotalLabStudio;
	}

	@Column(name = "jumlah_ruang_lab")
	public Integer getJumlahRuangLab() {
		return jumlahRuangLab;
	}

	public void setJumlahRuangLab(Integer jumlahRuangLab) {
		this.jumlahRuangLab = jumlahRuangLab;
	}

	@Column(name = "luas_total_ruang_dosen_tetap")
	public Double getLuasTotalRuangDosenTetap() {
		return luasTotalRuangDosenTetap;
	}

	public void setLuasTotalRuangDosenTetap(Double luasTotalRuangDosenTetap) {
		this.luasTotalRuangDosenTetap = luasTotalRuangDosenTetap;
	}

	@Column(name = "luas_total_ruang_administrasi")
	public Double getLuasTotalRuangAdministrasi() {
		return luasTotalRuangAdministrasi;
	}

	public void setLuasTotalRuangAdministrasi(Double luasTotalRuangAdministrasi) {
		this.luasTotalRuangAdministrasi = luasTotalRuangAdministrasi;
	}

	@Column(name = "luas_total_ruang_seminar")
	public Double getLuasTotalRuangSeminar() {
		return luasTotalRuangSeminar;
	}

	public void setLuasTotalRuangSeminar(Double luasTotalRuangSeminar) {
		this.luasTotalRuangSeminar = luasTotalRuangSeminar;
	}

	@Column(name = "luas_total_ruang_ekskul")
	public Double getLuasTotalRuangEkskul() {
		return luasTotalRuangEkskul;
	}

	public void setLuasTotalRuangEkskul(Double luasTotalRuangEkskul) {
		this.luasTotalRuangEkskul = luasTotalRuangEkskul;
	}

	@Column(name = "luas_total_pusat_komputer")
	public Double getLuasTotalPusatKomputer() {
		return luasTotalPusatKomputer;
	}

	public void setLuasTotalPusatKomputer(Double luasTotalPusatKomputer) {
		this.luasTotalPusatKomputer = luasTotalPusatKomputer;
	}

	@Column(name = "luas_total_ruang_perpustakaan")
	public Double getLuasTotalRuangPerpustakaan() {
		return luasTotalRuangPerpustakaan;
	}

	public void setLuasTotalRuangPerpustakaan(Double luasTotalRuangPerpustakaan) {
		this.luasTotalRuangPerpustakaan = luasTotalRuangPerpustakaan;
	}

	@Column(name = "jumlah_judul_buku")
	public Integer getJumlahJudulBuku() {
		return jumlahJudulBuku;
	}

	public void setJumlahJudulBuku(Integer jumlahJudulBuku) {
		this.jumlahJudulBuku = jumlahJudulBuku;
	}

	@Column(name = "jumlah_eksemplar_buku")
	public Integer getJumlahEksemplarBuku() {
		return jumlahEksemplarBuku;
	}

	public void setJumlahEksemplarBuku(Integer jumlahEksemplarBuku) {
		this.jumlahEksemplarBuku = jumlahEksemplarBuku;
	}

	@Column(name = "deskripsi", columnDefinition = "text")
	public String getDeskripsi() {
		if (deskripsi == null) {
			deskripsi = "";
		}
		return deskripsi;
	}

	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	public String getNamaSingkat() {
		return namaSingkat;
	}

	public void setNamaSingkat(String namaSingkat) {
		this.namaSingkat = namaSingkat;
	}

	public String getDusun() {
		return dusun;
	}

	public void setDusun(String dusun) {
		this.dusun = dusun;
	}

	public String getRt() {
		return rt;
	}

	public void setRt(String rt) {
		this.rt = rt;
	}

	public String getRw() {
		return rw;
	}

	public void setRw(String rw) {
		this.rw = rw;
	}

	public String getKelurahan() {
		return kelurahan;
	}

	public void setKelurahan(String kelurahan) {
		this.kelurahan = kelurahan;
	}

	public String getSkIzinOperasi() {
		return skIzinOperasi;
	}

	public void setSkIzinOperasi(String skIzinOperasi) {
		this.skIzinOperasi = skIzinOperasi;
	}

	@Temporal(TemporalType.DATE)
	public Date getTglSkIzinOperasi() {
		return tglSkIzinOperasi;
	}

	public void setTglSkIzinOperasi(Date tglSkIzinOperasi) {
		this.tglSkIzinOperasi = tglSkIzinOperasi;
	}

	public String getNoRek() {
		return noRek;
	}

	public void setNoRek(String noRek) {
		this.noRek = noRek;
	}

	public String getNmBank() {
		return nmBank;
	}

	public void setNmBank(String nmBank) {
		this.nmBank = nmBank;
	}

	public String getUnitCabang() {
		return unitCabang;
	}

	public void setUnitCabang(String unitCabang) {
		this.unitCabang = unitCabang;
	}

	public String getNmRek() {
		return nmRek;
	}

	public void setNmRek(String nmRek) {
		this.nmRek = nmRek;
	}

	public Double getLuasTanahMilik() {
		return luasTanahMilik;
	}

	public void setLuasTanahMilik(Double luasTanahMilik) {
		this.luasTanahMilik = luasTanahMilik;
	}

	public Double getLuasTanahBukanMilik() {
		return luasTanahBukanMilik;
	}

	public void setLuasTanahBukanMilik(Double luasTanahBukanMilik) {
		this.luasTanahBukanMilik = luasTanahBukanMilik;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	public String getPejabatIzinOperasi() {
		return pejabatIzinOperasi;
	}

	public void setPejabatIzinOperasi(String pejabatIzinOperasi) {
		this.pejabatIzinOperasi = pejabatIzinOperasi;
	}

	public Integer getTahunPertamaMenerimaMahasiswa() {
		return tahunPertamaMenerimaMahasiswa == null ? 0 : tahunPertamaMenerimaMahasiswa;
	}

	public void setTahunPertamaMenerimaMahasiswa(Integer tahunPertamaMenerimaMahasiswa) {
		this.tahunPertamaMenerimaMahasiswa = tahunPertamaMenerimaMahasiswa;
	}

	public String getPeringkatAkreditasi() {
		return peringkatAkreditasi;
	}

	public void setPeringkatAkreditasi(String peringkatAkreditasi) {
		this.peringkatAkreditasi = peringkatAkreditasi;
	}

	public String getAkreditasi() {
		return akreditasi;
	}

	public void setAkreditasi(String akreditasi) {
		this.akreditasi = akreditasi;
	}

	public String getNoSkAkreditasi() {
		return noSkAkreditasi;
	}

	public void setNoSkAkreditasi(String noSkAkreditasi) {
		this.noSkAkreditasi = noSkAkreditasi;
	}

	public Date getTanggalAkreditasi() {
		return tanggalAkreditasi;
	}

	public void setTanggalAkreditasi(Date tanggalAkreditasi) {
		this.tanggalAkreditasi = tanggalAkreditasi;
	}

	public String getDomain() {
		if (getPendaftar() != null) {
			domain = pendaftar.getDomain();
		}
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getMotto() {
		if (getPendaftar() != null) {
			motto = pendaftar.getMotto();
		}
		return motto == null ? "" : motto.trim();
	}

	public void setMotto(String motto) {
		this.motto = motto;
	}

	public String getRektor() {
		return rektor;
	}

	public void setRektor(String rektor) {
		this.rektor = rektor;
	}

	public String getRektorNip() {
		return rektorNip;
	}

	public void setRektorNip(String rektorNip) {
		this.rektorNip = rektorNip;
	}

	public String getKodeSinta() {
		return kodeSinta == null ? "" : kodeSinta.trim();
	}

	public void setKodeSinta(String kodeSinta) {
		this.kodeSinta = kodeSinta;
	}

	public String getCss() {
		return css == null ? "" : css;
	}

	public void setCss(String css) {
		this.css = css;
	}

	@Column(name = "pilihan_tampilan", length = 30)
	public String getPiilhanTampilan() {
		return piilhanTampilan == null ? TAMPILAN_DEFAULT : piilhanTampilan;
	}

	public void setPiilhanTampilan(String piilhanTampilan) {
		this.piilhanTampilan = piilhanTampilan;
	}

	public Boolean getDosenHarusPakaiSatuanKerja() {
		return dosenHarusPakaiSatuanKerja == null ? false : dosenHarusPakaiSatuanKerja;
	}

	public void setDosenHarusPakaiSatuanKerja(Boolean dosenHarusPakaiSatuanKerja) {
		this.dosenHarusPakaiSatuanKerja = dosenHarusPakaiSatuanKerja;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kepala", nullable = true)
	public Pegawai getKepala() {
		kepala = check(kepala);
		return kepala;
	}

	public void setKepala(Pegawai kepala) {
		this.kepala = kepala;
	}

	public String getWa() {
		return wa == null ? "" : wa.trim();
	}

	public void setWa(String wa) {
		this.wa = wa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "wakil1", nullable = true)
	public Pegawai getWakil1() {
		wakil1 = check(wakil1);
		return wakil1;
	}

	public void setWakil1(Pegawai wakil1) {
		this.wakil1 = wakil1;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "wakil2", nullable = true)
	public Pegawai getWakil2() {
		wakil2 = check(wakil2);
		return wakil2;
	}

	public void setWakil2(Pegawai wakil2) {
		this.wakil2 = wakil2;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "wakil3", nullable = true)
	public Pegawai getWakil3() {
		wakil3 = check(wakil3);
		return wakil3;
	}

	public void setWakil3(Pegawai wakil3) {
		this.wakil3 = wakil3;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pejabat1", nullable = true)
	public Pegawai getPejabat1() {
		pejabat1 = check(pejabat1);
		return pejabat1;
	}

	public void setPejabat1(Pegawai pejabat1) {
		this.pejabat1 = pejabat1;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pejabat2", nullable = true)
	public Pegawai getPejabat2() {
		pejabat2 = check(pejabat2);
		return pejabat2;
	}

	public void setPejabat2(Pegawai pejabat2) {
		this.pejabat2 = pejabat2;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pejabat3", nullable = true)
	public Pegawai getPejabat3() {
		pejabat3 = check(pejabat3);
		return pejabat3;
	}

	public void setPejabat3(Pegawai pejabat3) {
		this.pejabat3 = pejabat3;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pejabat4", nullable = true)
	public Pegawai getPejabat4() {
		pejabat4 = check(pejabat4);
		return pejabat4;
	}

	public void setPejabat4(Pegawai pejabat4) {
		this.pejabat4 = pejabat4;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pejabat5", nullable = true)
	public Pegawai getPejabat5() {
		pejabat5 = check(pejabat5);
		return pejabat5;
	}

	public void setPejabat5(Pegawai pejabat5) {
		this.pejabat5 = pejabat5;
	}

	public String getLabelPejabat1() {
		return labelPejabat1 == null ? "Pejabat I" : labelPejabat1;
	}

	public void setLabelPejabat1(String labelPejabat1) {
		this.labelPejabat1 = labelPejabat1;
	}

	public String getLabelPejabat2() {
		return labelPejabat2 == null ? "Pejabat II" : labelPejabat2;
	}

	public void setLabelPejabat2(String labelPejabat2) {
		this.labelPejabat2 = labelPejabat2;
	}

	public String getLabelPejabat3() {
		return labelPejabat3 == null ? "Pejabat III" : labelPejabat3;
	}

	public void setLabelPejabat3(String labelPejabat3) {
		this.labelPejabat3 = labelPejabat3;
	}

	public String getLabelPejabat4() {
		return labelPejabat4 == null ? "Pejabat IV" : labelPejabat4;
	}

	public void setLabelPejabat4(String labelPejabat4) {
		this.labelPejabat4 = labelPejabat4;
	}

	public String getLabelPejabat5() {
		return labelPejabat5 == null ? "Pejabat V" : labelPejabat5;
	}

	public void setLabelPejabat5(String labelPejabat5) {
		this.labelPejabat5 = labelPejabat5;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar")
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	@Column(columnDefinition = "text")
	public String getHeaderpmb() {
		return headerpmb;
	}

	public void setHeaderpmb(String headerpmb) {
		this.headerpmb = headerpmb;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putFile(Map parameters) {
		PerguruanTinggi perguruanTinggi = this;

		File file = FileFoto.fileAdaDiFolder(LampiranLain.KOP_PT, perguruanTinggi.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("KOP_PT", file.getAbsolutePath());
			parameters.put("KOP_PT_" + perguruanTinggi.getId(), file.getAbsolutePath());
			parameters.put("KOP_PT_" + perguruanTinggi.getNama(), file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, perguruanTinggi.getId(), LampiranLain.KOP_PT);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("KOP_PT", fileKop.getAbsolutePath());
					parameters.put("KOP_PT_" + perguruanTinggi.getId(), fileKop.getAbsolutePath());
					parameters.put("KOP_PT_" + perguruanTinggi.getNama(), fileKop.getAbsolutePath());
				}
			}
		}

		file = FileFoto.fileAdaDiFolder(LampiranLain.STEMPEL_PT, perguruanTinggi.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("STEMPEL_PT", file.getAbsolutePath());
			parameters.put("STEMPEL_PT_" + perguruanTinggi.getId(), file.getAbsolutePath());
			parameters.put("STEMPEL_PT_" + perguruanTinggi.getNama(), file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, perguruanTinggi.getId(), LampiranLain.STEMPEL_PT);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("STEMPEL_PT", fileKop.getAbsolutePath());
					parameters.put("STEMPEL_PT_" + perguruanTinggi.getId(), fileKop.getAbsolutePath());
					parameters.put("STEMPEL_PT_" + perguruanTinggi.getNama(), fileKop.getAbsolutePath());
				}
			}
		}

		file = FileFoto.fileAdaDiFolder(LampiranLain.KOP_BAWAH_PT, perguruanTinggi.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("KOP_BAWAH_PT", file.getAbsolutePath());
			parameters.put("KOP_BAWAH_PT_" + perguruanTinggi.getId(), file.getAbsolutePath());
			parameters.put("KOP_BAWAH_PT_" + perguruanTinggi.getNama(), file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("KOP_BAWAH_PT", fileKop.getAbsolutePath());
					parameters.put("KOP_BAWAH_PT_" + perguruanTinggi.getId(), fileKop.getAbsolutePath());
					parameters.put("KOP_BAWAH_PT_" + perguruanTinggi.getNama(), fileKop.getAbsolutePath());
				}
			}
		}
	}

	public String getStatus() {
		return status == null ? "Swasta" : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPropinsi() {
		return propinsi;
	}

	public void setPropinsi(String propinsi) {
		this.propinsi = propinsi;
	}
}
