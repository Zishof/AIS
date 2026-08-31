package ais.database.model;

/*
 * author: Zulkifli, April 17, 2010
 */

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Entitas Hibernate untuk tabel {@code public.perguruan_tinggi_lain}, merepresentasikan satu
 * perguruan tinggi <b>eksternal</b> (di luar institusi induk AIS sendiri, yang datanya
 * tersimpan pada entitas terpisah {@link PerguruanTinggi}). Baris pada tabel ini berfungsi
 * sebagai data referensi/master perguruan tinggi lain, dipakai antara lain untuk mencatat:
 * <ul>
 * <li>asal perguruan tinggi calon mahasiswa/mahasiswa pindahan (lihat
 * {@link ais.database.model.BiodataCalonMahasiswa} dan {@link ais.database.model.Mahasiswa}),</li>
 * <li>perguruan tinggi tempat dosen mengajar di luar institusi induk (lihat
 * {@link ais.database.model.MengajarDiPerguruanTinggiLain}), dan</li>
 * <li>data hasil impor feeder PDDikti ({@code ais.action.master.feeder.util.FeederJSONImport}).</li>
 * </ul>
 * <p>
 * Struktur field-nya sengaja meniru profil {@link PerguruanTinggi} (identitas legal, alamat,
 * kontak, data sarana-prasarana untuk pelaporan EPSBED/PDDikti, akreditasi BAN-PT, rekening
 * bank, dan pejabat rektor) namun tanpa relasi ke entitas internal AIS lain (tidak ada
 * {@code @ManyToOne} ke {@link Pendaftar}/{@link Pegawai}/dsb.) karena institusi yang dicatat
 * di sini bukan bagian dari organisasi yang dikelola AIS.
 * <p>
 * Perubahan (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate
 * Envers), dan setiap update otomatis memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "perguruan_tinggi_lain")

public class PerguruanTinggiLain extends GeneralValueObject {
	private static final long serialVersionUID = -7550455125892447098L;
	private Long id;
	private String oleh;
	private String olehId;

	/** Id pengguna yang terakhir membuat/mengubah baris ini (audit jejak perubahan). */
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** Nama pengguna yang terakhir membuat/mengubah baris ini (audit jejak perubahan). */
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
	private String kodePos;
	private String telepon;
	private String faksimili;

	private Date tanggalAkta;
	private Date tanggalAwalPendirian;
	private String nomorAkta;

	private String email;
	private String website;
	private String domain;
	private String motto;
	private String kodeSinta;

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

	public PerguruanTinggiLain() {
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

	@Column(name = "kode_yayasan", nullable = false, length = 50)
	public String getKodeYayasan() {
		return this.kodeYayasan;
	}

	public void setKodeYayasan(String kodeYayasan) {
		this.kodeYayasan = kodeYayasan;
	}

	@Column(name = "kode_perguruan_tinggi", nullable = false, length = 50)
	public String getKodePerguruanTinggi() {
		return this.kodePerguruanTinggi;
	}

	public void setKodePerguruanTinggi(String kodePerguruanTinggi) {
		this.kodePerguruanTinggi = kodePerguruanTinggi;
	}

	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "alamat1", nullable = false, length = 150)
	public String getAlamat1() {
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
		return this.telepon;
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

	@Column(name = "website", length = 150)
	public String getWebsite() {
		return this.website;
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

	/** Id/kode perguruan tinggi ini pada sistem feeder PDDikti, bila hasil impor dari feeder. */
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
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getMotto() {
		return motto;
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

	/** Kode institusi pada SINTA (Science and Technology Index) Kemdikbudristek. */
	public String getKodeSinta() {
		return kodeSinta == null ? "" : kodeSinta.trim();
	}

	public void setKodeSinta(String kodeSinta) {
		this.kodeSinta = kodeSinta;
	}

}
