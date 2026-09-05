package ais.database.model.sirs;

// Generated Apr 12, 2010 1:48:52 AM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.GeneralValueObject;
import ais.database.model.library.Penyedia;

/**
 * Entitas katalog master item medis pada schema {@code sirs} (tabel
 * {@code item_medis}). Satu baris merepresentasikan satu jenis barang yang
 * bisa berupa obat, alat kesehatan (alkes), maupun bahan habis pakai (BHP)
 * yang dipakai dalam transaksi rumah sakit (resep, tindakan, penjualan
 * apotek, dsb).
 *
 * <p>
 * Katalog ini SEPENUHNYA TERPISAH dari katalog {@code inventory.Produk}
 * (beserta {@code GrupProduk}/{@code JenisProduk}/{@code SatuanProduk})
 * yang dipakai modul koperasi/inventory umum lain — sudah diverifikasi
 * langsung dari kode: tidak ada satupun kelas di paket
 * {@code ais.database.model.sirs} yang mengimpor
 * {@code ais.database.model.inventory.*}, dan tidak ada kelas di paket
 * {@code inventory} yang mengimpor {@link ItemMedis} atau
 * {@link AlatMedis}. Dengan kata lain, item medis SIRS punya siklus hidup,
 * ID, dan tabel referensi sendiri; tidak ada foreign key silang ke katalog
 * inventory umum.
 * </p>
 *
 * <p>
 * Klasifikasi item mengikuti EMPAT dimensi independen yang masing-masing
 * adalah tabel lookup datar (tanpa hierarki maupun keterkaitan satu sama
 * lain — diverifikasi dari kode, bukan diasumsikan dari nama kelas):
 * </p>
 * <ul>
 * <li>{@link #getJenisItem()} ({@link JenisItemMedis}) — WAJIB diisi;
 * membedakan OBAT vs BAHAN_MEDIS (lihat konstanta di
 * {@link JenisItemMedis}).</li>
 * <li>{@link #getKelompokItem()} ({@link KelompokItem}) — opsional;
 * pengelompokan item (mis. kelompok pengadaan/anggaran), sekadar
 * pasangan id/nama/keterangan tanpa makna bisnis tertanam di kelasnya.</li>
 * <li>{@link #getKelasItem()} ({@link KelasItem}) — opsional; "kelas"
 * item dalam arti klasifikasi katalog, TIDAK terkait dengan
 * {@code KelasPerawatan} (kelas rawat pasien) yang dipakai
 * {@link HargaJualItem}.</li>
 * <li>{@link #getGenerikItem()} ({@link GenerikItem}) — opsional; nama
 * generik/kandungan obat, terpisah dari field bebas teks
 * {@link #getKandungan()}.</li>
 * </ul>
 *
 * <p>
 * Field {@code default*} ({@link #getDefaultPenyedia()},
 * {@link #getDefaultPermintaan()}, {@link #getDefaultHargaJual()},
 * {@link #getDefaultHargaBeli()}) adalah nilai bawaan yang disalin ke baris
 * transaksi baru (permintaan/pembelian/penjualan) sebagai titik awal, BUKAN
 * nilai yang dipaksakan — harga aktual per transaksi tetap bisa disimpan
 * di {@link HargaBeliItem}/{@link HargaJualItem}. Flag
 * {@link #getSemuahargasama()} menandakan apakah harga jual item ini sama
 * untuk semua kelas perawatan (lihat pola serupa di
 * {@link HargaJualItem#getKelasPerawatan()}).
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "item_medis")
public class ItemMedis extends GeneralValueObject {

	/**
	 * Nilai default ambang batas stok minimum ({@link #getBatasMinimalStok()})
	 * yang dipakai sebagai fallback ketika item belum diisi batas
	 * minimal-nya sendiri. Dipakai oleh
	 * {@code ais.action.master.sirs.ItemMedisAction} (bukan di kelas ini
	 * sendiri) melalui pola
	 * {@code item.getBatasMinimalStok() == null ? ItemMedis.DEFAULT_MINIMAL_STOK : item.getBatasMinimalStok()}
	 * saat menghitung status/alert stok rendah untuk laporan/dasbor.
	 */
	public static final Integer DEFAULT_MINIMAL_STOK = 100;

	/**
	 *
	 */
	private static final long serialVersionUID = -3088213612931036389L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna (username/oleh-id) yang terakhir mengubah baris
	 * ini. Field audit shadow yang diisi lewat {@link #setOlehId(String)};
	 * lihat javadoc setter untuk perilaku guard-nya.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong oleh pemanggil yang lalai mengisi konteks
	 * pengguna — ini KEHARUSAN TEKNIS (pola field audit shadow yang sudah
	 * berulang kali ditemukan di paket model AIS), bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai
	 *               tersimpan.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas item ini untuk keperluan tampilan/log, memuat
	 * kode, nama, dan seluruh empat dimensi klasifikasi (satuan, jenis,
	 * kelompok, kelas, generik) dipisah tanda hubung.
	 *
	 * <p>
	 * PERHATIAN: method ini memanggil {@link #check(Object)} (lazy-load
	 * guard milik {@code GeneralValueObject}) pada kelima relasi
	 * ({@code satuanItem}, {@code kelompokItem}, {@code generikItem},
	 * {@code jenisItem}, {@code kelasItem}) dan MENIMPA field instance
	 * dengan hasilnya sebelum membangun string — sama seperti pola getter
	 * lain di kelas ini yang punya efek samping menulis balik ke field.
	 * Karena dipanggil di {@code toString()}, efek samping ini bisa
	 * ter-trigger hanya dengan me-log atau mencetak objek ini.
	 * </p>
	 *
	 * @return string gabungan {@code kode - nama-satuan-jenis-kelompok-kelas-generik}.
	 */
	public String toString() {
		satuanItem = check(satuanItem);
		kelompokItem = check(kelompokItem);
		generikItem = check(generikItem);
		jenisItem = check(jenisItem);
		kelasItem = check(kelasItem);

		return kode + " - " + nama + "-" + (satuanItem == null ? "" : satuanItem) + "-"
				+ (jenisItem == null ? "" : jenisItem) + "-" + (kelompokItem == null ? "" : kelompokItem) + "-"
				+ (kelasItem == null ? "" : kelasItem) + "-" + (generikItem == null ? "" : generikItem);
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini (pasangan lengkap
	 * dari {@link #olehId}). Sama seperti {@link #setOlehId(String)}, nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa
	 * kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau
	 *             kosong/whitespace-saja tidak akan mengubah nilai
	 *             tersimpan.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang dipanggil container sebelum baris
	 * ini di-UPDATE ke database. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * untuk memperbarui {@link #tanggal_dirubah} secara otomatis, sehingga
	 * pemanggil tidak perlu menyetel timestamp perubahan secara manual di
	 * setiap titik penyimpanan.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 * Umumnya nilai ini diisi otomatis oleh {@link #onUpdate()} pada setiap
	 * UPDATE; setter ini tersedia untuk kasus di mana nilai perlu diisi
	 * eksplisit (mis. saat insert awal atau migrasi data).
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini. Diinisialisasi ke
	 * waktu pembuatan objek ({@code new Date()}) dan diperbarui otomatis
	 * oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String nama;
	private String kode;
	private String barcode = "";
	private Boolean bolehretur;
	private Integer batasMinimalStok;
	private SatuanItem satuanItem;
	private JenisItemMedis jenisItem;
	private KelompokItem kelompokItem;

	private KelasItem kelasItem;
	private GenerikItem generikItem;

	private String kandungan;

	private Penyedia defaultPenyedia;
	private Double defaultPermintaan = 0.0;
	private Double defaultHargaJual = 0.0;
	private Double defaultHargaBeli = 0.0;
	private Boolean semuahargasama = true;

	private String keterangan;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate). Seluruh
	 * field diisi lewat setter atau lazy-init di getter masing-masing.
	 */
	public ItemMedis() {
	}

	/**
	 * Primary key baris item medis, auto-increment (IDENTITY) dan
	 * diisi database (bukan aplikasi).
	 *
	 * @return ID unik item medis ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID item medis. Karena kolom {@code id} bersifat
	 * {@code insertable = false}, nilai ini hanya relevan setelah baris
	 * dimuat dari database (mis. untuk referensi FK), bukan untuk
	 * mengendalikan nilai yang di-generate saat insert.
	 *
	 * @param id ID item medis.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan nama item medis (nama dagang/nama tampil).
	 *
	 * @param nama nama item medis.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil nama item medis.
	 *
	 * @return nama item medis.
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Menetapkan kode item medis.
	 *
	 * @param kode kode unik item medis.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil kode item medis. Kolom {@code kode} bersifat
	 * {@code unique} dan {@code nullable = false} di database, sehingga
	 * setiap item medis wajib punya kode yang tidak boleh duplikat —
	 * inilah pengenal bisnis utama yang dipakai di transaksi, bukan
	 * {@link #getId()}.
	 *
	 * @return kode item medis.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan satuan dasar item medis (mis. Tablet, Botol, Ampul).
	 *
	 * @param satuanItem satuan dasar item.
	 */
	public void setSatuanItem(SatuanItem satuanItem) {
		this.satuanItem = satuanItem;
	}

	/**
	 * Mengambil satuan dasar item medis ini. Relasi WAJIB
	 * ({@code nullable = false}) — setiap item medis harus punya satu
	 * satuan dasar. Konversi ke satuan lain (mis. Box ke Tablet) dicatat
	 * terpisah lewat {@link KonversiSatuanItem}, bukan di sini.
	 *
	 * <p>
	 * Getter ini memanggil {@link #check(Object)} (lazy-load guard) dan
	 * menulis-balik hasilnya ke field {@link #satuanItem} sebelum
	 * mengembalikannya — pola getter dengan efek samping tulis-balik yang
	 * konsisten dipakai di seluruh relasi {@code @ManyToOne} kelas ini.
	 * </p>
	 *
	 * @return satuan dasar item medis.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_item", nullable = false)
	public SatuanItem getSatuanItem() {
		satuanItem = check(satuanItem);
		return satuanItem;
	}

	/**
	 * Menetapkan jenis item medis (klasifikasi obat vs bahan medis).
	 *
	 * @param jenisItem jenis item medis, lihat {@link JenisItemMedis}.
	 */
	public void setJenisItem(JenisItemMedis jenisItem) {
		this.jenisItem = jenisItem;
	}

	/**
	 * Mengambil jenis item medis ini. Relasi WAJIB
	 * ({@code nullable = false}) yang membedakan OBAT vs BAHAN_MEDIS
	 * (lihat konstanta {@link JenisItemMedis#OBAT} dan
	 * {@link JenisItemMedis#BAHAN_MEDIS}). Ini adalah SATU-SATUNYA dari
	 * keempat dimensi klasifikasi item yang wajib diisi; tiga lainnya
	 * ({@link #getKelompokItem()}, {@link #getKelasItem()},
	 * {@link #getGenerikItem()}) bersifat opsional.
	 *
	 * @return jenis item medis.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_item", nullable = false)
	public JenisItemMedis getJenisItem() {
		jenisItem = check(jenisItem);
		return jenisItem;
	}

	/**
	 * Menetapkan barcode item medis (dipakai untuk pemindaian di kasir
	 * apotek/gudang).
	 *
	 * @param barcode kode barcode; boleh string kosong (nilai default
	 *                field ini adalah {@code ""}, bukan {@code null}).
	 */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/**
	 * Mengambil barcode item medis.
	 *
	 * @return barcode item medis; string kosong jika belum pernah
	 *         diset (bukan {@code null}, karena field diinisialisasi ke
	 *         {@code ""} saat deklarasi).
	 */
	public String getBarcode() {
		return barcode;
	}

	/**
	 * Menetapkan apakah item medis ini boleh diretur (dikembalikan) oleh
	 * pasien/pembeli.
	 *
	 * @param bolehretur {@code true} jika boleh diretur.
	 */
	public void setBolehretur(Boolean bolehretur) {
		this.bolehretur = bolehretur;
	}

	/**
	 * Mengambil flag boleh-retur item medis ini. Flag SATU-ARAH:
	 * {@code null} otomatis dibaca sebagai {@code true} (boleh retur)
	 * lewat lazy-init di getter, dan nilai default ini DITULIS-BALIK ke
	 * field instance (bukan sekadar dikembalikan sebagai hasil hitung).
	 * Berarti item medis lama yang belum pernah eksplisit diset
	 * {@code bolehretur}-nya akan otomatis dianggap boleh diretur begitu
	 * getter ini dipanggil sekali saja.
	 *
	 * @return {@code true} jika item boleh diretur; default {@code true}
	 *         bila belum pernah diset.
	 */
	public Boolean getBolehretur() {
		if (bolehretur == null) {
			bolehretur = true;
		}
		return bolehretur;
	}

	/**
	 * Menetapkan batas minimal stok item medis ini secara eksplisit.
	 *
	 * @param batasMinimalStok ambang stok minimum sebelum dianggap perlu
	 *                         restock; {@code null} berarti pakai fallback
	 *                         {@link #DEFAULT_MINIMAL_STOK} di sisi
	 *                         pemanggil (lihat {@code ItemMedisAction}).
	 */
	public void setBatasMinimalStok(Integer batasMinimalStok) {
		this.batasMinimalStok = batasMinimalStok;
	}

	/**
	 * Mengambil batas minimal stok item medis ini apa adanya (TANPA
	 * fallback ke {@link #DEFAULT_MINIMAL_STOK} — fallback itu diterapkan
	 * di kode pemanggil, bukan di getter ini).
	 *
	 * @return ambang stok minimum, atau {@code null} jika belum diisi.
	 */
	@Column(name = "batas_minimal_stok")
	public Integer getBatasMinimalStok() {
		return batasMinimalStok;
	}

	/**
	 * Menetapkan penyedia (vendor) default untuk pengadaan item medis ini.
	 *
	 * @param defaultPenyedia vendor default.
	 */
	public void setDefaultPenyedia(Penyedia defaultPenyedia) {
		this.defaultPenyedia = defaultPenyedia;
	}

	/**
	 * Mengambil penyedia (vendor) default item medis ini — relasi
	 * opsional yang dipakai sebagai nilai awal saat membuat dokumen
	 * pengadaan baru untuk item ini, bukan pembatasan vendor yang sah.
	 *
	 * @return vendor default, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "default_vendor", nullable = true)
	public Penyedia getDefaultPenyedia() {
		defaultPenyedia = check(defaultPenyedia);
		return defaultPenyedia;
	}

	/**
	 * Menetapkan apakah harga jual item ini sama untuk semua kelas
	 * perawatan.
	 *
	 * @param semuahargasama {@code true} jika harga seragam lintas kelas
	 *                       perawatan.
	 */
	public void setSemuahargasama(Boolean semuahargasama) {
		this.semuahargasama = semuahargasama;
	}

	/**
	 * Mengambil flag "semua harga sama" item medis ini. Menentukan apakah
	 * baris {@link HargaJualItem} untuk item ini perlu didefinisikan per
	 * {@code KelasPerawatan} atau cukup satu harga yang berlaku untuk
	 * semua kelas — pola flag yang sama dipakai juga di
	 * {@code Tindakan}, {@code AlatMedis}, dan entitas tarif khusus
	 * lain di paket {@code sirs}.
	 *
	 * @return {@code true} jika harga seragam lintas kelas perawatan.
	 */
	public Boolean getSemuahargasama() {
		return semuahargasama;
	}

	/**
	 * Menetapkan jumlah permintaan default untuk item medis ini.
	 *
	 * @param defaultPermintaan jumlah permintaan default.
	 */
	public void setDefaultPermintaan(Double defaultPermintaan) {
		this.defaultPermintaan = defaultPermintaan;
	}

	/**
	 * Mengambil jumlah permintaan default item medis ini — nilai awal
	 * yang disalin ke baris permintaan/pesanan baru sebagai kuantitas
	 * bawaan.
	 *
	 * @return jumlah permintaan default; {@code 0.0} bila belum pernah
	 *         diubah dari nilai inisialisasi field.
	 */
	@Column(name = "default_permintaan")
	public Double getDefaultPermintaan() {
		return defaultPermintaan;
	}

	/**
	 * Menetapkan kelompok item medis ini (dimensi klasifikasi opsional).
	 *
	 * @param kelompokItem kelompok item, lihat {@link KelompokItem}.
	 */
	public void setKelompokItem(KelompokItem kelompokItem) {
		this.kelompokItem = kelompokItem;
	}

	/**
	 * Mengambil kelompok item medis ini — relasi OPSIONAL
	 * ({@code nullable = true}), berbeda dari {@link #getJenisItem()}
	 * yang wajib. {@link KelompokItem} sendiri hanyalah tabel lookup
	 * datar (id/nama/keterangan) tanpa keterkaitan ke
	 * {@link JenisItemMedis}, {@link KelasItem}, atau
	 * {@link GenerikItem} — keempatnya independen satu sama lain,
	 * diverifikasi langsung dari kode masing-masing kelas.
	 *
	 * @return kelompok item, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_item", nullable = true)
	public KelompokItem getKelompokItem() {
		kelompokItem = check(kelompokItem);
		return kelompokItem;
	}

	/**
	 * Menetapkan kandungan zat aktif item medis ini (teks bebas,
	 * BERBEDA dari relasi {@link #getGenerikItem()} yang menunjuk ke
	 * tabel nama generik terstruktur).
	 *
	 * @param kandungan deskripsi kandungan zat aktif.
	 */
	public void setKandungan(String kandungan) {
		this.kandungan = kandungan;
	}

	/**
	 * Mengambil kandungan zat aktif item medis ini (field teks bebas).
	 *
	 * @return deskripsi kandungan, atau {@code null} jika belum diisi.
	 */
	public String getKandungan() {
		return kandungan;
	}

	/**
	 * Menetapkan kelas item medis ini (dimensi klasifikasi opsional).
	 *
	 * @param kelasItem kelas item, lihat {@link KelasItem}.
	 */
	public void setKelasItem(KelasItem kelasItem) {
		this.kelasItem = kelasItem;
	}

	/**
	 * Mengambil kelas item medis ini — relasi OPSIONAL. Nama kelas
	 * {@link KelasItem} di sini adalah klasifikasi katalog item (mis.
	 * kelas mutu/tingkat item), TIDAK boleh dikacaukan dengan
	 * {@code KelasPerawatan} (kelas rawat inap pasien) yang dipakai di
	 * {@link HargaJualItem#getKelasPerawatan()} — dua konsep "kelas" yang
	 * sama sekali berbeda meski namanya mirip.
	 *
	 * @return kelas item, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_item", nullable = true)
	public KelasItem getKelasItem() {
		kelasItem = check(kelasItem);
		return kelasItem;
	}

	/**
	 * Menetapkan nama generik item medis ini (dimensi klasifikasi
	 * opsional, relevan terutama untuk obat).
	 *
	 * @param generikItem nama generik, lihat {@link GenerikItem}.
	 */
	public void setGenerikItem(GenerikItem generikItem) {
		this.generikItem = generikItem;
	}

	/**
	 * Mengambil nama generik item medis ini — relasi OPSIONAL ke tabel
	 * lookup {@link GenerikItem}, terpisah dari field teks bebas
	 * {@link #getKandungan()}.
	 *
	 * @return nama generik, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "generik_item", nullable = true)
	public GenerikItem getGenerikItem() {
		generikItem = check(generikItem);
		return generikItem;
	}

	/**
	 * Menetapkan harga jual default item medis ini.
	 *
	 * @param defaultHargaJual harga jual default.
	 */
	public void setDefaultHargaJual(Double defaultHargaJual) {
		this.defaultHargaJual = defaultHargaJual;
	}

	/**
	 * Mengambil harga jual default item medis ini — nilai awal yang
	 * disalin ke baris transaksi penjualan baru, TIDAK dijaga tetap
	 * sinkron dengan baris {@link HargaJualItem} aktual (perlu disamakan
	 * manual/lewat proses tersendiri bila harga jual berubah).
	 *
	 * @return harga jual default; {@code 0.0} bila belum pernah diubah.
	 */
	@Column(name = "default_harga_jual")
	public Double getDefaultHargaJual() {
		return defaultHargaJual;
	}

	/**
	 * Menetapkan harga beli default item medis ini.
	 *
	 * @param defaultHargaBeli harga beli default.
	 */
	public void setDefaultHargaBeli(Double defaultHargaBeli) {
		this.defaultHargaBeli = defaultHargaBeli;
	}

	/**
	 * Mengambil harga beli default item medis ini — nilai awal yang
	 * disalin ke dokumen pengadaan baru, TIDAK dijaga tetap sinkron
	 * dengan baris {@link HargaBeliItem} aktual per vendor.
	 *
	 * @return harga beli default; {@code 0.0} bila belum pernah diubah.
	 */
	@Column(name = "default_harga_beli")
	public Double getDefaultHargaBeli() {
		return defaultHargaBeli;
	}

	/**
	 * Mengambil keterangan bebas untuk item medis ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan keterangan bebas untuk item medis ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
