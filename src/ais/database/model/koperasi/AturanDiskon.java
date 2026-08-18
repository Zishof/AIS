package ais.database.model.koperasi;

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

import ais.database.model.Tbmuser;
import ais.database.model.VoKunci;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.sop.DisposisiSop;

/**
 * Class untuk menyimpan Aturan Diskon / Promo Produk
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "aturan_diskon")
public class AturanDiskon extends VoKunci {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String namaAturan; // Contoh: "Promo Akhir Tahun Indomie"
	private String keterangan;

	// --- 1. TARGET PRODUK & TOKO ---
	private Produk produk; // OPSIONAL: Jika null, berarti berlaku untuk SEMUA produk
	private Toko toko; // OPSIONAL: Jika null, berarti berlaku di semua toko/kios

	// --- 2. TARGET MEMBER ---
	private Boolean berlakuSemuaMember; // True = Publik/Semua orang dapat. False = Harus cek tipe/jenis
	private JenisAnggotaKoperasi jenisAnggota; // OPSIONAL: Spesifik untuk jenis tertentu (misal: Guru/Siswa)
	private TipeAnggotaKoperasi tipeAnggota; // OPSIONAL: Spesifik untuk tipe tertentu (misal: VIP/Reguler)

	// --- 3. NILAI DISKON ---
	private Double persentase; // Persentase diskon (Contoh: 10.0 untuk 10%)
	private Double maksimalPotongan; // Batas maksimal potongan rupiah jika pakai persentase
	private Double nominal; // Diskon fix rupiah (Contoh: Rp 2.000)
	// Konflik promo: angka lebih besar dihitung lebih dahulu. Default aman tidak ditumpuk.
	private Integer prioritas;
	private Boolean dapatDigabung;
	private String dasarPerhitungan; // SETELAH_DISKON atau HARGA_AWAL
	private String grupEksklusif; // aturan dengan kode sama tidak boleh dipakai bersamaan

	// --- 4. LOGIKA PENERAPAN (POTONG HARGA vs SALDO/CASHBACK) ---
	private Boolean potonganLangsung; // TRUE = Potong harga di struk. FALSE = Masuk ke saldo diskon (Pencairan)

	private Boolean berlakuPerHariDanPerToko; // TRUE = Hanya berlaku tiap hari 1x dan per toko. FALSE = berlaku semua

	// --- 5. MASA BERLAKU ---
	private Date tanggalMulai;
	private Date tanggalSelesai;
	private String hariAktif; // CSV hari ISO weekday (1=Senin..7=Minggu); null/kosong = semua hari
	private Boolean aktif;

	// TRUE = tidak auto-terapkan di checkout, kasir wajib pilih manual lewat picker promo.
	// null/false = perilaku default (auto-apply, aturan pertama yang cocok langsung dipakai).
	private Boolean aktivasiManual;

	private String oleh;
	private String olehId;

	private Tbmuser dikunci;
	private DisposisiSop disposisiSop;

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

	public AturanDiskon() {
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

	@Column(name = "nama_aturan", nullable = false, length = 255)
	public String getNamaAturan() {
		return namaAturan;
	}

	public void setNamaAturan(String namaAturan) {
		this.namaAturan = namaAturan;
	}

	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	// Relasi Produk (OPSIONAL -- null = berlaku semua produk)
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = true)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	// Relasi Toko (OPSIONAL)
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	@Column(name = "berlaku_semua_member")
	public Boolean getBerlakuSemuaMember() {
		return berlakuSemuaMember == null ? false : berlakuSemuaMember;
	}

	public void setBerlakuSemuaMember(Boolean berlakuSemuaMember) {
		this.berlakuSemuaMember = berlakuSemuaMember;
	}

	// Relasi Jenis Anggota (OPSIONAL)
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_anggota", nullable = true)
	public JenisAnggotaKoperasi getJenisAnggota() {
		jenisAnggota = check(jenisAnggota);
		return jenisAnggota;
	}

	public void setJenisAnggota(JenisAnggotaKoperasi jenisAnggota) {
		this.jenisAnggota = jenisAnggota;
	}

	// Relasi Tipe Anggota (OPSIONAL)
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_anggota", nullable = true)
	public TipeAnggotaKoperasi getTipeAnggota() {
		tipeAnggota = check(tipeAnggota);
		return tipeAnggota;
	}

	public void setTipeAnggota(TipeAnggotaKoperasi tipeAnggota) {
		this.tipeAnggota = tipeAnggota;
	}

	@Column(name = "persentase")
	public Double getPersentase() {
		return persentase == null ? 0.0 : persentase;
	}

	public void setPersentase(Double persentase) {
		this.persentase = persentase;
	}

	@Column(name = "maksimal_potongan")
	public Double getMaksimalPotongan() {
		return maksimalPotongan == null ? 0.0 : maksimalPotongan;
	}

	public void setMaksimalPotongan(Double maksimalPotongan) {
		this.maksimalPotongan = maksimalPotongan;
	}

	@Column(name = "nominal")
	public Double getNominal() {
		return nominal == null ? 0.0 : nominal;
	}

	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	@Column(name = "prioritas", nullable = false)
	public Integer getPrioritas() {
		return prioritas == null ? 100 : prioritas;
	}

	public void setPrioritas(Integer prioritas) {
		this.prioritas = prioritas;
	}

	@Column(name = "dapat_digabung", nullable = false)
	public Boolean getDapatDigabung() {
		return dapatDigabung == null ? false : dapatDigabung;
	}

	public void setDapatDigabung(Boolean dapatDigabung) {
		this.dapatDigabung = dapatDigabung;
	}

	@Column(name = "dasar_perhitungan", nullable = false, length = 30)
	public String getDasarPerhitungan() {
		return dasarPerhitungan == null || dasarPerhitungan.trim().isEmpty()
				? "SETELAH_DISKON" : dasarPerhitungan;
	}

	public void setDasarPerhitungan(String dasarPerhitungan) {
		this.dasarPerhitungan = dasarPerhitungan;
	}

	@Column(name = "grup_eksklusif", length = 100)
	public String getGrupEksklusif() {
		return grupEksklusif;
	}

	public void setGrupEksklusif(String grupEksklusif) {
		this.grupEksklusif = grupEksklusif;
	}

	// Menentukan proses eksekusi (Langsung potong nota ATAU masuk tabungan saldo)
	@Column(name = "potongan_langsung", nullable = false)
	public Boolean getPotonganLangsung() {
		return potonganLangsung == null ? true : potonganLangsung; // Default memotong harga awal
	}

	public void setPotonganLangsung(Boolean potonganLangsung) {
		this.potonganLangsung = potonganLangsung;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_mulai")
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_selesai")
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Gap-closure "Promo Pilih Hari" -- CSV angka hari ISO-8601 ({@code 1}=Senin .. {@code 7}=Minggu,
	 * mis. {@code "1,2,3,4,5"} utk Senin-Jumat). {@code null}/kosong = berlaku SEMUA hari (tanpa
	 * batasan), konsisten dgn konvensi {@link #getTanggalMulai()}/{@link #getTanggalSelesai()} null =
	 * tanpa batas. Dicek lewat {@link ais.common.HariAktifUtil#aktifPadaHari(String, Date)} dari DUA
	 * mesin pencocokan promo yang wajib tetap sinkron: {@code PosKantinAction.evaluasiDiskon} (ZK/JSP,
	 * checkout langsung) dan {@code KantinHelper.evaluasiDiskonItems} (API, dipakai Electron/Flutter).
	 */
	@Column(name = "hari_aktif", length = 20)
	public String getHariAktif() {
		return hariAktif;
	}

	public void setHariAktif(String hariAktif) {
		this.hariAktif = hariAktif;
	}

	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "aktivasi_manual")
	public Boolean getAktivasiManual() {
		return aktivasiManual == null ? false : aktivasiManual;
	}

	public void setAktivasiManual(Boolean aktivasiManual) {
		this.aktivasiManual = aktivasiManual;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci", nullable = true)
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
		this.disposisiSop = disposisiSop;
	}

	// TRUE = Hanya berlaku tiap hari 1x dan per toko. FALSE = berlaku semua
	@Column(name = "berlaku_per_hari_dan_per_toko")
	public Boolean getBerlakuPerHariDanPerToko() {
		return berlakuPerHariDanPerToko == null ? false : berlakuPerHariDanPerToko;
	}

	public void setBerlakuPerHariDanPerToko(Boolean berlakuPerHariDanPerToko) {
		this.berlakuPerHariDanPerToko = berlakuPerHariDanPerToko;
	}

}
