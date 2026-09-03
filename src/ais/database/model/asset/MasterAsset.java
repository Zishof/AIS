package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.Pertangungjawaban;

/**
 * KATALOG jenis barang / jasa -- tingkat TERATAS dari tiga tingkat pencatatan aset AIS.
 *
 * <h3>Posisi dalam hirarki tiga tingkat</h3>
 *
 * <p>Satu baris di sini adalah satu DEFINISI jenis barang, bukan barang fisik: "Laptop Dell XPS
 * 13", lengkap dengan merk, spesifikasi, dimensi, satuan, umur ekonomis, harga beli default,
 * dan pemetaan akun akuntansinya. Arah kolom FK membuktikan posisinya sebagai puncak hirarki:
 * tabel ini TIDAK memuat kolom yang menunjuk ke {@link Asset} maupun {@link AssetDetail};
 * sebaliknya {@code asset.asset} memuat kolom {@code master_asset} yang menunjuk ke sini.</p>
 *
 * <p>Urutan lengkapnya: {@code MasterAsset} (katalog jenis) -&gt; {@link Asset} (kepemilikan satu
 * jenis oleh satu satuan kerja) -&gt; {@link AssetDetail} (unit fisik ber-barcode). Di atas
 * katalog masih ada dua tingkat pengelompokan yang murni klasifikasi: {@link KelompokAsset}
 * (kelompok berjenjang yang membawa pemetaan akun dan estimasi umur pakai), serta
 * {@link JenisAsset} dan {@link KategoriAsset} sebagai penggolongan mendatar.</p>
 *
 * <h3>Tidak ada isolasi tenant di tingkat ini</h3>
 *
 * <p>Entitas ini TIDAK memiliki kolom {@code satuan_kerja}, {@code yayasan}, {@code sekolah},
 * maupun penanda tenant lain -- baik langsung (pola satu tingkat seperti {@code Toko}) maupun
 * tak langsung (pola dua tingkat seperti {@code Koperasi}). Katalog aset karena itu bersifat
 * GLOBAL: satu daftar jenis barang dipakai bersama seluruh satuan kerja dalam satu instalasi.
 * Pemisahan data per tenant baru mulai berlaku di tingkat {@link Asset} ke bawah. Konsekuensinya
 * bagi siapa pun yang membangun penyaringan atau perizinan di modul aset: penyaringan tenant
 * TIDAK dapat dipasang pada entitas ini, harus di tingkat {@code Asset} atau
 * {@code AssetDetail}. Perlu dicatat juga bahwa entitas ini tidak terdaftar pada Generic CRUD
 * v2 -- di klaster aset inti hanya {@link AssetDetail} yang terdaftar, lewat adapter baca-saja
 * penyusutan aset.</p>
 *
 * <h3>Sumber nilai bagi unit di bawahnya</h3>
 *
 * <p>Katalog ini adalah sumber warisan nilai bagi seluruh unit fisik turunannya:
 * {@link AssetDetail#getHargaBeli()} mengambil {@link #getHargaBeliDefault()},
 * {@link AssetDetail#getNilaiMinimal()} mengambil {@link #getNilaiMinimal()}, dan
 * {@link AssetDetail#getUmurEkonomis()} mengambil {@link #getUmurEkonomis()} -- yang pada
 * gilirannya diturunkan lagi dari {@link KelompokAsset#getEstimasiUmurPakai()}. Nama unit pun
 * disalin dari sini lewat {@link Asset#getNama()}. Menyunting satu baris katalog karena itu
 * dapat merambat ke banyak aset sekaligus; lihat peringatan pada masing-masing getter tersebut.</p>
 *
 * <h3>Pemetaan akun</h3>
 *
 * <p>Tiga bidang akun -- akun transaksi (aset/persediaan), akun akumulasi penyusutan, dan akun
 * biaya penyusutan -- masing-masing hadir dalam DUA bentuk: kolom teks JSON per satuan kerja
 * ({@code akun_*_str}) dan kolom FK tunggal warisan ({@code akun_*}). Pemilihan akun yang
 * berlaku untuk posting dilakukan oleh {@link #akunTransaksiEfektif()},
 * {@link #akunPenyusutanEfektif()}, dan {@link #akunBiayaPenyusutanEfektif()}, yang sengaja
 * BUKAN getter JavaBean agar tidak dipetakan Hibernate. Alasan pemisahan itu diuraikan pada
 * {@link #getAkunTransaksi()}.</p>
 *
 * @see Asset kepemilikan per satuan kerja
 * @see AssetDetail unit fisik individual
 * @see KelompokAsset kelompok berjenjang pembawa akun dan estimasi umur pakai
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "master_asset")
public class MasterAsset extends GeneralValueObject {

	/**
	 * Nilai {@link #getTipe()} untuk katalog yang mewakili JASA, bukan barang.
	 *
	 * <p>Dideklarasikan {@code public static} tanpa {@code final} -- warisan gaya lama basis kode
	 * ini -- sehingga secara teknis dapat ditimpa saat berjalan. Perlakukan sebagai konstanta.</p>
	 */
	public static String TIPE_JASA = "Jasa";

	/** Nilai {@link #getTipe()} untuk barang habis pakai (persediaan, tidak disusutkan). */
	public static String TIPE_HABIS_PAKAI = "Barang habis pakai";

	/** Nilai {@link #getTipe()} untuk barang tidak habis pakai yang dicatat sebagai aset tetap. */
	public static String TIPE_TIDAK_HABIS_PAKAI = "Barang tidak habis pakai";

	/**
	 * Nilai {@link #getTipe()} untuk barang tidak habis pakai yang TIDAK dicatat sebagai aset
	 * tetap (mis. karena nilainya di bawah ambang kapitalisasi).
	 *
	 * <p>Perlu dicatat bahwa {@link #getTipe()} tidak pernah menghasilkan nilai ini secara
	 * otomatis; ia hanya dapat ditetapkan pengguna lewat {@link #setTipe(String)}.</p>
	 */
	public static String TIPE_TIDAK_HABIS_PAKAI_NON_ASET = "Barang tidak habis pakai non aset";

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Bernilai sama dengan entitas lain sepaket karena seluruh berkas dihasilkan hbm2java dari
	 * templat yang sama; tidak bermasalah karena nilai ini hanya dibandingkan antar-versi kelas
	 * yang sama, tidak pernah antar-kelas berbeda.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id}; di-generate database (IDENTITY). */
	private Long id;

	/** Nama tampil pengguna terakhir yang menyunting baris ini. */
	private String oleh;

	/** Id pengguna terakhir yang menyunting baris ini. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * <p>Nilai {@code null} atau berisi spasi saja tidak menimpa jejak audit lama, agar proses
	 * batch yang tidak mengenal pengguna aktif tidak menghapus riwayat yang sudah tercatat.</p>
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: memperbarui stempel waktu audit sebelum UPDATE dikirim.
	 *
	 * <p>Didelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} agar aturan penulisan
	 * stempel waktu terpusat untuk seluruh entitas.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu penyuntingan terakhir, bernilai awal waktu server saat objek dibuat.
	 *
	 * <p>Bidang audit ini diulang di tiap entitas AIS sebagai KEHARUSAN TEKNIS: kelas induk
	 * {@link GeneralValueObject} bukan {@code @Entity} maupun {@code @MappedSuperclass}, sehingga
	 * Hibernate tidak mewarisi pemetaan kolom apa pun darinya.</p>
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu penyuntingan terakhir.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu penyuntingan terakhir baris ini.
	 *
	 * @return stempel waktu; tidak pernah {@code null} untuk objek hasil konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks "id-nama" untuk label komponen ZK.
	 *
	 * <p>Membaca field {@code nama} LANGSUNG, bukan lewat {@link #getNama()}, sehingga tidak
	 * memicu efek samping apa pun -- berbeda dari {@link Asset#toString()}.</p>
	 *
	 * @return teks berbentuk {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode katalog; ikut menjadi bagian barcode unit, lihat {@code AssetDetail.generateBarcode}. */
	private String kode;

	/** Nama jenis barang; disalin ke {@link Asset#getNama()} bagi seluruh aset turunannya. */
	private String nama;

	/** Merk / pabrikan barang. */
	private String merk;

	/** Panjang barang dalam satuan {@link #getUnit()}. */
	private Integer panjang = 0;

	/** Lebar barang dalam satuan {@link #getUnit()}. */
	private Integer lebar = 0;

	/** Tinggi barang dalam satuan {@link #getUnit()}. */
	private Integer tinggi = 0;

	/** Berat barang dalam satuan {@link #getUnitberat()}. */
	private Integer berat = 0;

	/** Satuan dimensi panjang/lebar/tinggi; nilai awal {@code "Meter"}. */
	private String unit = "Meter";

	/** Satuan berat; nilai awal {@code "Kg"}. */
	private String unitberat = "Kg";

	/** Satuan hitung barang (buah, unit, rim, dan sebagainya). */
	private SatuanMasterAsset satuanMasterAsset;

	/** Keterangan bebas. */
	private String keterangan;

	/** Spesifikasi teknis rinci; kolom bertipe {@code text}. */
	private String spesifikasi;

	/** Penggolongan jenis (mis. peralatan kantor, kendaraan, tanah, bangunan). */
	private JenisAsset jenisAsset;

	/** Kelompok berjenjang pembawa pemetaan akun dan estimasi umur pakai. */
	private KelompokAsset kelompokAsset;

	/** Penyedia / vendor yang diusulkan sebagai pilihan awal saat pengadaan barang ini. */
	private PenyediaAsset defaultPenyedia;

	/** Penggolongan kategori tambahan, mendatar terhadap {@link #jenisAsset}. */
	private KategoriAsset kategoriAsset;

	/** Penanda harga boleh diubah saat transaksi; lihat {@link #getHargaBolehDiubah()}. */
	private Boolean hargaBolehDiubah;

	/** Umur ekonomis default; DITURUNKAN dari kelompok aset, lihat {@link #getUmurEkonomis()}. */
	private Double umurEkonomis;

	/** Nilai residu default bagi unit turunan. */
	private Double nilaiMinimal;

	/** Harga beli default bagi unit turunan. */
	private Double hargaBeliDefault;

	/** Akun akumulasi penyusutan per satuan kerja, teks JSON; kolom {@code akun_penyusutan_str}. */
	private String akunPenyusutan;

	/** Akun biaya penyusutan per satuan kerja, teks JSON; kolom {@code akun_biaya_penyusutan_str}. */
	private String akunBiayaPenyusutan;

	/** Akun aset/persediaan per satuan kerja, teks JSON; kolom {@code akun_transaksi_str}. */
	private String akunTransaksi;

	/** Penanda barang boleh dipinjam lewat modul peminjaman aset. */
	private Boolean bolehDipinjam;

	/** Penanda barang boleh dijual. */
	private Boolean bolehDijual;

	/** Harga jual default bila barang boleh dijual. */
	private Double hargaJualDefault;

	/** Akun akumulasi penyusutan bentuk WARISAN (FK tunggal), kolom {@code akun_penyusutan}. */
	private Akun akunPenyusutanA;

	/** Akun biaya penyusutan bentuk WARISAN (FK tunggal), kolom {@code akun_biaya_penyusutan}. */
	private Akun akunBiayaPenyusutanA;

	/** Akun aset/persediaan bentuk WARISAN (FK tunggal), kolom {@code akun_transaksi}. */
	private Akun akunTransaksiA;

	/**
	 * Akun Akumulasi Penyusutan bentuk WARISAN -- satu FK tunggal ke {@link Akun}.
	 *
	 * <p>Bentuk lama, dari masa sebelum pemetaan akun dipecah per satuan kerja dalam teks JSON.
	 * Dibaca sebagai pilihan KEDUA oleh {@link #akunPenyusutanEfektif()}, sesudah kolom JSON
	 * milik katalog sendiri dan sebelum nilai milik kelompok aset. Baris katalog lama yang belum
	 * dimigrasikan masih mengandalkan kolom ini.</p>
	 *
	 * @return akun akumulasi penyusutan warisan, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_penyusutan", nullable = true)
	public Akun getAkunPenyusutanA() {
		akunPenyusutanA = check(akunPenyusutanA);
		return akunPenyusutanA;
	}

	/**
	 * Menetapkan akun akumulasi penyusutan bentuk warisan.
	 *
	 * @param akunPenyusutan akun warisan baru, boleh {@code null}
	 */
	public void setAkunPenyusutanA(Akun akunPenyusutan) {
		this.akunPenyusutanA = akunPenyusutan;
	}

	/**
	 * Akun aset / persediaan bentuk WARISAN -- satu FK tunggal ke {@link Akun}.
	 *
	 * <p>Dibaca sebagai pilihan KEDUA oleh {@link #akunTransaksiEfektif()}; lihat
	 * {@link #getAkunPenyusutanA()} untuk latar belakang bentuk warisan ini.</p>
	 *
	 * @return akun transaksi warisan, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_transaksi", nullable = true)
	public Akun getAkunTransaksiA() {
		akunTransaksiA = check(akunTransaksiA);
		return akunTransaksiA;
	}

	/**
	 * Menetapkan akun aset / persediaan bentuk warisan.
	 *
	 * @param akunTransaksi akun warisan baru, boleh {@code null}
	 */
	public void setAkunTransaksiA(Akun akunTransaksi) {
		this.akunTransaksiA = akunTransaksi;
	}

	/**
	 * Akun Biaya Penyusutan bentuk WARISAN -- satu FK tunggal ke {@link Akun}.
	 *
	 * <p>Dibaca sebagai pilihan KEDUA oleh {@link #akunBiayaPenyusutanEfektif()}. Versi lama
	 * metode efektif itu keliru membaca akun warisan bidang lain; lihat catatan di sana.</p>
	 *
	 * @return akun biaya penyusutan warisan, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_biaya_penyusutan", nullable = true)
	public Akun getAkunBiayaPenyusutanA() {
		akunBiayaPenyusutanA = check(akunBiayaPenyusutanA);
		return akunBiayaPenyusutanA;
	}

	/**
	 * Menetapkan akun biaya penyusutan bentuk warisan.
	 *
	 * @param akunBiayaPenyusutan akun warisan baru, boleh {@code null}
	 */
	public void setAkunBiayaPenyusutanA(Akun akunBiayaPenyusutan) {
		this.akunBiayaPenyusutanA = akunBiayaPenyusutan;
	}

	/** Tipe barang (jasa / habis pakai / tidak habis pakai); DITURUNKAN, lihat {@link #getTipe()}. */
	private String tipe;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan ZK data binding.
	 */
	public MasterAsset() {
	}

	/**
	 * Kunci utama baris.
	 *
	 * <p>{@code insertable = false} karena nilainya di-generate database.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya dipanggil Hibernate seusai INSERT.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode katalog.
	 *
	 * <p>Ikut menjadi bagian kedua barcode unit pada strategi penomoran terstruktur
	 * {@code AssetDetail.generateBarcode}; bila kosong, bagian itu beserta titik pemisahnya
	 * dilewati begitu saja.</p>
	 *
	 * @return kode hasil {@code trim()}, atau {@code null} bila belum terisi
	 */
	@Column(name = "kode")
	public String getKode() {
		return this.kode == null ? null : this.kode.trim();
	}

	/**
	 * Mengisi kode katalog.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama jenis barang.
	 *
	 * <p>Nilai ini disalin ke seluruh baris {@link Asset} turunannya oleh
	 * {@link Asset#getNama()}, dan salinan itu tersimpan permanen. Mengubah nama di sini karena
	 * itu ikut mengubah nama seluruh aset turunan begitu masing-masing tersentuh sesi Hibernate.</p>
	 *
	 * @return nama hasil {@code trim()}, atau {@code null} bila belum terisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama jenis barang.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas untuk katalog ini.
	 *
	 * @return keterangan apa adanya, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Panjang barang dalam satuan {@link #getUnit()}.
	 *
	 * <p>Getter ini MENULIS BALIK ke field: bila nilainya {@code null}, field diisi {@code 1}
	 * sebelum dikembalikan. Karena entitas memakai akses PROPERTI, nilai {@code 1} itu ikut
	 * tersimpan permanen pada flush berikutnya. Perhatikan bahwa nilai awal deklarasi field
	 * adalah {@code 0}, bukan {@code 1}, sehingga barang yang memang tidak berdimensi bisa
	 * berakhir tercatat berukuran {@code 1} bila kolomnya pernah bernilai {@code NULL} di basis
	 * data (mis. baris hasil migrasi).</p>
	 *
	 * @return panjang barang; tidak pernah {@code null}
	 */
	public Integer getPanjang() {
		if (panjang == null) {
			panjang = 1;
		}
		return panjang;
	}

	/**
	 * Mengisi panjang barang.
	 *
	 * @param panjang panjang baru
	 */
	public void setPanjang(Integer panjang) {
		this.panjang = panjang;
	}

	/**
	 * Lebar barang dalam satuan {@link #getUnit()}.
	 *
	 * <p>Menulis balik {@code 1} bila {@code null}, sama seperti {@link #getPanjang()}.</p>
	 *
	 * @return lebar barang; tidak pernah {@code null}
	 */
	public Integer getLebar() {
		if (lebar == null) {
			lebar = 1;
		}
		return lebar;
	}

	/**
	 * Mengisi lebar barang.
	 *
	 * @param lebar lebar baru
	 */
	public void setLebar(Integer lebar) {
		this.lebar = lebar;
	}

	/**
	 * Tinggi barang dalam satuan {@link #getUnit()}.
	 *
	 * <p>Menulis balik {@code 1} bila {@code null}, sama seperti {@link #getPanjang()}.</p>
	 *
	 * @return tinggi barang; tidak pernah {@code null}
	 */
	public Integer getTinggi() {
		if (tinggi == null) {
			tinggi = 1;
		}
		return tinggi;
	}

	/**
	 * Mengisi tinggi barang.
	 *
	 * @param tinggi tinggi baru
	 */
	public void setTinggi(Integer tinggi) {
		this.tinggi = tinggi;
	}

	/**
	 * Satuan dimensi panjang / lebar / tinggi.
	 *
	 * <p>Berupa teks bebas (nilai awal {@code "Meter"}), bukan acuan ke tabel satuan, sehingga
	 * tidak ada jaminan keseragaman antar-baris katalog.</p>
	 *
	 * @return satuan dimensi
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Mengisi satuan dimensi.
	 *
	 * @param unit satuan dimensi baru
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * Berat barang dalam satuan {@link #getUnitberat()}.
	 *
	 * <p>Menulis balik {@code 1} bila {@code null}, sama seperti {@link #getPanjang()}.</p>
	 *
	 * @return berat barang; tidak pernah {@code null}
	 */
	public Integer getBerat() {
		if (berat == null) {
			berat = 1;
		}
		return berat;
	}

	/**
	 * Mengisi berat barang.
	 *
	 * @param berat berat baru
	 */
	public void setBerat(Integer berat) {
		this.berat = berat;
	}

	/**
	 * Satuan berat barang (teks bebas, nilai awal {@code "Kg"}).
	 *
	 * @return satuan berat
	 */
	public String getUnitberat() {
		return unitberat;
	}

	/**
	 * Mengisi satuan berat barang.
	 *
	 * @param unitberat satuan berat baru
	 */
	public void setUnitberat(String unitberat) {
		this.unitberat = unitberat;
	}

	/**
	 * Penggolongan jenis barang (peralatan kantor, kendaraan, tanah, bangunan, dan sebagainya).
	 *
	 * <p>Dibaca {@link #getTipe()} bersama {@link #getKelompokAsset()} untuk menebak apakah
	 * barang tergolong habis pakai atau tidak.</p>
	 *
	 * @return jenis aset, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_asset", nullable = true)
	public JenisAsset getJenisAsset() {
		jenisAsset = check(jenisAsset);
		return jenisAsset;
	}

	/**
	 * Menetapkan penggolongan jenis barang.
	 *
	 * @param jenisAsset jenis aset baru, boleh {@code null}
	 */
	public void setJenisAsset(JenisAsset jenisAsset) {
		this.jenisAsset = jenisAsset;
	}

	/**
	 * Kelompok aset berjenjang yang menaungi katalog ini.
	 *
	 * <p>Kelompok inilah pembawa pemetaan akun tingkat atas (dibaca sebagai pilihan KETIGA oleh
	 * ketiga metode {@code akun...Efektif()}), estimasi umur pakai yang diambil
	 * {@link #getUmurEkonomis()}, penanda aset tetap yang menentukan digit terakhir barcode
	 * unit, serta templat {@code NomorSurat} yang mengalihkan penomoran barcode ke strategi
	 * nomor surat.</p>
	 *
	 * @return kelompok aset, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_asset", nullable = true)
	public KelompokAsset getKelompokAsset() {
		kelompokAsset = check(kelompokAsset);
		return kelompokAsset;
	}

	/**
	 * Menetapkan kelompok aset.
	 *
	 * @param kelompokAsset kelompok baru, boleh {@code null}
	 */
	public void setKelompokAsset(KelompokAsset kelompokAsset) {
		this.kelompokAsset = kelompokAsset;
	}

	/**
	 * Merk / pabrikan barang.
	 *
	 * @return merk, atau {@code null} bila belum terisi
	 */
	public String getMerk() {
		return merk;
	}

	/**
	 * Mengisi merk / pabrikan barang.
	 *
	 * @param merk merk baru
	 */
	public void setMerk(String merk) {
		this.merk = merk;
	}

	/**
	 * Penyedia / vendor yang diusulkan sebagai pilihan awal saat pengadaan barang ini.
	 *
	 * <p>Hanya bersifat saran pengisian formulir; tidak mengikat dokumen pengadaan mana pun.</p>
	 *
	 * @return penyedia default, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "default_penyedia", nullable = true)
	public PenyediaAsset getDefaultPenyedia() {
		defaultPenyedia = check(defaultPenyedia);
		return defaultPenyedia;
	}

	/**
	 * Menetapkan penyedia default.
	 *
	 * @param defaultPenyedia penyedia baru, boleh {@code null}
	 */
	public void setDefaultPenyedia(PenyediaAsset defaultPenyedia) {
		this.defaultPenyedia = defaultPenyedia;
	}

	/**
	 * Satuan hitung barang (buah, unit, rim, dan sebagainya).
	 *
	 * <p>Berbeda dari {@link #getUnit()} yang berupa teks bebas, satuan hitung ini mengacu ke
	 * tabel {@link SatuanMasterAsset} sehingga seragam antar-katalog.</p>
	 *
	 * @return satuan hitung, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_master_asset", nullable = true)
	public SatuanMasterAsset getSatuanMasterAsset() {
		satuanMasterAsset = check(satuanMasterAsset);
		return satuanMasterAsset;
	}

	/**
	 * Menetapkan satuan hitung barang.
	 *
	 * @param satuanMasterAsset satuan hitung baru, boleh {@code null}
	 */
	public void setSatuanMasterAsset(SatuanMasterAsset satuanMasterAsset) {
		this.satuanMasterAsset = satuanMasterAsset;
	}

	/**
	 * Umur ekonomis default -- getter DESTRUKTIF yang MENGAMBIL ALIH nilai dari kelompok aset.
	 *
	 * <h3>Yang sebenarnya terjadi</h3>
	 *
	 * <p>Bila katalog ini punya kelompok aset dan {@link KelompokAsset#getEstimasiUmurPakai()}
	 * kelompok itu lebih besar dari {@code 1}, nilai kelompok MENIMPA field {@link #umurEkonomis}
	 * -- tanpa syarat apakah katalog sudah punya nilainya sendiri. Berbeda dari pewarisan
	 * "isi bila kosong" yang dipakai {@link AssetDetail#getUmurEkonomis()}, di sini kelompok
	 * selalu menang.</p>
	 *
	 * <p>Karena entitas ini memakai akses PROPERTI ({@code @Id} dipasang di getter), Hibernate
	 * memanggil getter ini juga saat pemeriksaan perubahan, sehingga nilai kelompok itu
	 * TERTULIS PERMANEN ke kolom umur ekonomis katalog pada flush berikutnya. Akibatnya, umur
	 * ekonomis yang disunting pengguna di layar Master Aset tidak akan bertahan selama
	 * kelompoknya punya estimasi umur pakai di atas {@code 1}. Ini pola yang persis sama dengan
	 * yang sudah diperbaiki untuk bidang akun -- lihat catatan pada {@link #getAkunPenyusutan()}
	 * dan {@link #akunPenyusutanEfektif()} -- tetapi untuk umur ekonomis pemisahan serupa BELUM
	 * dilakukan.</p>
	 *
	 * <h3>Ambang yang perlu diperhatikan</h3>
	 *
	 * <p>Syaratnya {@code > 1}, bukan {@code > 0}. Kelompok dengan estimasi umur pakai tepat
	 * {@code 1} periode karena itu diabaikan dan nilai katalog dipertahankan -- perbedaan halus
	 * yang mudah mengejutkan saat menelusuri mengapa satu katalog mengikuti kelompoknya dan
	 * katalog lain tidak.</p>
	 *
	 * <h3>Rantai warisan ke bawah</h3>
	 *
	 * <p>Nilai ini diambil {@link AssetDetail#getUmurEkonomis()} sebagai warisan bagi unit fisik
	 * yang belum punya umur sendiri, lalu dipakai sebagai PEMBAGI pada
	 * {@link PenyusutanAsset#getNilaiPenyusutan()} ({@code hargaBeli / umurEkonomis}). Jadi
	 * mengubah estimasi umur pakai pada satu kelompok aset dapat merambat sampai mengubah beban
	 * penyusutan seluruh unit di bawahnya -- termasuk periode yang sudah diposting, karena
	 * {@code PenyusutanAsset} juga menghitung ulang lewat getter. Nilai {@code 0.0} yang
	 * dikembalikan saat kosong tidak menyebabkan pembagian dengan nol: {@code PenyusutanAsset}
	 * menjaga diri dengan syarat {@code umurEkonomis > 0.1}, sehingga unit tanpa umur ekonomis
	 * hanya menghasilkan beban penyusutan nol secara diam-diam.</p>
	 *
	 * @return umur ekonomis default; {@code 0.0} bila tidak ada nilai katalog maupun kelompok
	 */
	public Double getUmurEkonomis() {

		if (getKelompokAsset() != null && getKelompokAsset().getEstimasiUmurPakai() > 1) {
			umurEkonomis = getKelompokAsset().getEstimasiUmurPakai();
		}

		return umurEkonomis == null ? 0.0 : umurEkonomis;
	}

	/**
	 * Menetapkan umur ekonomis default.
	 *
	 * <p>Nilai yang diisi akan DITIMPA oleh {@link #getUmurEkonomis()} bila kelompok asetnya
	 * punya estimasi umur pakai di atas {@code 1}.</p>
	 *
	 * @param umurEkonomis umur ekonomis baru
	 */
	public void setUmurEkonomis(Double umurEkonomis) {
		this.umurEkonomis = umurEkonomis;
	}

	/**
	 * Nilai TERSIMPAN kolom ini apa adanya -- tanpa mengambil dari Kelompok Aset, dan tanpa
	 * mengubah field apa pun.
	 *
	 * <p>Getter ini dipetakan {@code @Column} pada entitas ber-akses PROPERTI ({@code @Id}
	 * dipasang di getter), sehingga Hibernate memanggilnya juga ketika memeriksa perubahan.
	 * Bila ia mengembalikan nilai milik Kelompok Aset, nilai itu ikut tertulis PERMANEN ke
	 * kolom aset pada flush berikutnya -- dan asetnya lalu berhenti mengikuti kelompoknya tanpa
	 * ada yang menyentuhnya. Karena itu pemilihan akun efektif dipindahkan ke
	 * {@link #akunPenyusutanEfektif()}, yang sengaja BUKAN getter JavaBean sehingga tidak dipetakan.</p>
	 *
	 * <p>Isinya berupa teks JSON array berisi pasangan akun per satuan kerja; bentuknya sama
	 * dengan {@link #getAkunTransaksi()} dan {@link #getAkunBiayaPenyusutan()}. Bila kosong,
	 * dikembalikan {@code Pertangungjawaban.DEFAULT_FORMULA} sebagai penanda "belum diisi" --
	 * penanda itulah yang diperiksa {@link #adaAkun(String)}.</p>
	 *
	 * @return teks JSON akun akumulasi penyusutan, atau formula default bila kosong
	 */
	@Column(name = "akun_penyusutan_str", columnDefinition = "text")
	public String getAkunPenyusutan() {
		return akunPenyusutan == null || akunPenyusutan.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA
				: akunPenyusutan;
	}

	/**
	 * Mengisi teks JSON akun akumulasi penyusutan milik katalog ini.
	 *
	 * @param akunPenyusutan teks JSON array akun per satuan kerja
	 */
	public void setAkunPenyusutan(String akunPenyusutan) {
		this.akunPenyusutan = akunPenyusutan;
	}

	/**
	 * Nilai TERSIMPAN kolom ini apa adanya -- tanpa mengambil dari Kelompok Aset, dan tanpa
	 * mengubah field apa pun.
	 *
	 * <p>Getter ini dipetakan {@code @Column} pada entitas ber-akses PROPERTI ({@code @Id}
	 * dipasang di getter), sehingga Hibernate memanggilnya juga ketika memeriksa perubahan.
	 * Bila ia mengembalikan nilai milik Kelompok Aset, nilai itu ikut tertulis PERMANEN ke
	 * kolom aset pada flush berikutnya -- dan asetnya lalu berhenti mengikuti kelompoknya tanpa
	 * ada yang menyentuhnya. Karena itu pemilihan akun efektif dipindahkan ke
	 * {@link #akunTransaksiEfektif()}, yang sengaja BUKAN getter JavaBean sehingga tidak dipetakan.</p>
	 *
	 * <p>Bidang ini memetakan akun ASET atau PERSEDIAAN barang -- sisi yang di-DEBIT saat
	 * perolehan aset dijurnal, dan di-KREDIT saat barang keluar. Isinya teks JSON array berisi
	 * pasangan akun per satuan kerja.</p>
	 *
	 * @return teks JSON akun aset/persediaan, atau formula default bila kosong
	 */
	@Column(name = "akun_transaksi_str", columnDefinition = "text")
	public String getAkunTransaksi() {
		return akunTransaksi == null || akunTransaksi.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA
				: akunTransaksi;
	}

	/**
	 * Mengisi teks JSON akun aset / persediaan milik katalog ini.
	 *
	 * @param akunTransaksi teks JSON array akun per satuan kerja
	 */
	public void setAkunTransaksi(String akunTransaksi) {
		this.akunTransaksi = akunTransaksi;
	}

	/**
	 * Nilai residu default bagi unit fisik turunan katalog ini.
	 *
	 * <p>Diambil {@link AssetDetail#getNilaiMinimal()} sebagai warisan bagi unit yang belum punya
	 * nilai residu sendiri, lalu ditambahkan kembali oleh
	 * {@link PenyusutanAsset#getNilaiBuku()} setelah beban penyusutan dikurangkan -- sehingga
	 * nilai buku unit tidak pernah turun di bawah angka ini. Nilai cadangan {@code 1.0}
	 * (bukan {@code 0.0}) dipakai agar aset yang sudah disusutkan penuh tetap tercatat bernilai,
	 * sesuai kebiasaan pencatatan aset tetap.</p>
	 *
	 * @return nilai residu default; {@code 1.0} bila belum terisi
	 */
	public Double getNilaiMinimal() {
		return nilaiMinimal == null ? 1.0 : nilaiMinimal;
	}

	/**
	 * Menetapkan nilai residu default.
	 *
	 * @param nilaiMinimal nilai residu baru
	 */
	public void setNilaiMinimal(Double nilaiMinimal) {
		this.nilaiMinimal = nilaiMinimal;
	}

	/**
	 * Harga beli default bagi unit fisik turunan katalog ini.
	 *
	 * <p>Diambil {@link AssetDetail#getHargaBeli()} sebagai nilai perolehan awal unit yang belum
	 * punya harga sendiri -- tahap kedua dari empat tahap penentuan harga di sana. Harga dari
	 * dokumen pengadaan atau penerimaan yang sudah disetujui akan menimpanya. Berbeda dari
	 * {@link #getNilaiMinimal()}, nilai cadangannya {@code 0.0} sehingga katalog tanpa harga
	 * default tidak memaksakan nilai perolehan apa pun.</p>
	 *
	 * @return harga beli default; {@code 0.0} bila belum terisi
	 */
	public Double getHargaBeliDefault() {
		return hargaBeliDefault == null ? 0.0 : hargaBeliDefault;
	}

	/**
	 * Menetapkan harga beli default.
	 *
	 * @param hargaBeliDefault harga beli default baru
	 */
	public void setHargaBeliDefault(Double hargaBeliDefault) {
		this.hargaBeliDefault = hargaBeliDefault;
	}

	/**
	 * Nilai TERSIMPAN kolom ini apa adanya -- tanpa mengambil dari Kelompok Aset, dan tanpa
	 * mengubah field apa pun.
	 *
	 * <p>Getter ini dipetakan {@code @Column} pada entitas ber-akses PROPERTI ({@code @Id}
	 * dipasang di getter), sehingga Hibernate memanggilnya juga ketika memeriksa perubahan.
	 * Bila ia mengembalikan nilai milik Kelompok Aset, nilai itu ikut tertulis PERMANEN ke
	 * kolom aset pada flush berikutnya -- dan asetnya lalu berhenti mengikuti kelompoknya tanpa
	 * ada yang menyentuhnya. Karena itu pemilihan akun efektif dipindahkan ke
	 * {@link #akunBiayaPenyusutanEfektif()}, yang sengaja BUKAN getter JavaBean sehingga tidak dipetakan.</p>
	 *
	 * <p>Bidang ini memetakan akun BIAYA penyusutan -- sisi yang di-DEBIT saat beban penyusutan
	 * periodik dijurnal, berpasangan dengan {@link #getAkunPenyusutan()} (akumulasi penyusutan)
	 * di sisi kredit.</p>
	 *
	 * @return teks JSON akun biaya penyusutan, atau formula default bila kosong
	 */
	@Column(name = "akun_biaya_penyusutan_str", columnDefinition = "text")
	public String getAkunBiayaPenyusutan() {
		return akunBiayaPenyusutan == null || akunBiayaPenyusutan.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA
				: akunBiayaPenyusutan;
	}

	/**
	 * Mengisi teks JSON akun biaya penyusutan milik katalog ini.
	 *
	 * @param akunBiayaPenyusutan teks JSON array akun per satuan kerja
	 */
	public void setAkunBiayaPenyusutan(String akunBiayaPenyusutan) {
		this.akunBiayaPenyusutan = akunBiayaPenyusutan;
	}

	/**
	 * Pemetaan akun EFEKTIF untuk POSTING, menurut urutan yang ditetapkan pemilik produk:
	 *
	 * <ol>
	 * <li>nilai milik ASET sendiri ({@code akun_*_str}) bila sudah memuat akun;</li>
	 * <li>akun WARISAN pada aset (kolom FK lama {@code akun_*}) bila terisi;</li>
	 * <li>terakhir, nilai milik KELOMPOK ASET.</li>
	 * </ol>
	 *
	 * <p>Sebelumnya urutannya terbalik -- kelompok menimpa aset -- sehingga menyunting akun di
	 * Master Aset tidak berpengaruh apa pun selama kelompoknya terisi.</p>
	 *
	 * <p>Sengaja BUKAN getter JavaBean ({@code get...}) supaya Hibernate tidak memetakannya. Lihat
	 * alasannya di {@link #getAkunTransaksi()}.</p>
	 *
	 * <p>Bidang ini adalah akun ASET / PERSEDIAAN barang. Bila ketiga sumber di atas kosong,
	 * yang dikembalikan adalah {@code Pertangungjawaban.DEFAULT_FORMULA} -- pemanggil perlu
	 * memperlakukannya sebagai "belum dipetakan", bukan sebagai akun yang sah.</p>
	 *
	 * @return teks JSON akun efektif untuk posting, atau formula default bila tidak ada sumber
	 *         yang memuat akun
	 */
	public String akunTransaksiEfektif() {
		return pilihAkun(akunTransaksi, getAkunTransaksiA(),
				getKelompokAsset() == null ? null : getKelompokAsset().getAkunTransaksi());
	}

	/**
	 * Akun Akumulasi Penyusutan efektif; urutan sama dengan {@link #akunTransaksiEfektif()}.
	 *
	 * <p>Perhatikan bahwa sumber pertama yang diperiksa adalah FIELD {@code akunPenyusutan}
	 * langsung, bukan {@link #getAkunPenyusutan()}. Itu disengaja: getter tersebut mengganti
	 * nilai kosong dengan formula default, sedangkan di sini yang dibutuhkan adalah nilai mentah
	 * agar {@link #adaAkun(String)} dapat membedakan "belum diisi" dari "sudah diisi".</p>
	 *
	 * @return teks JSON akun akumulasi penyusutan efektif, atau formula default bila tidak ada
	 */
	public String akunPenyusutanEfektif() {
		return pilihAkun(akunPenyusutan, getAkunPenyusutanA(),
				getKelompokAsset() == null ? null : getKelompokAsset().getAkunPenyusutan());
	}

	/**
	 * Akun Biaya / Biaya Penyusutan efektif; urutan sama dengan {@link #akunTransaksiEfektif()}.
	 *
	 * <p>Versi lama memeriksa {@code getAkunBiayaPenyusutanA()} pada penjaganya tetapi memakai
	 * {@code getAkunPenyusutanA()} di isinya -- salin-tempel yang membuat akun warisan salah satu
	 * bidang terabaikan diam-diam, atau melempar NPE yang tertelan {@code catch}. Di sini tiap
	 * bidang memakai akun warisannya sendiri.</p>
	 *
	 * @return teks JSON akun biaya penyusutan efektif, atau formula default bila tidak ada
	 */
	public String akunBiayaPenyusutanEfektif() {
		return pilihAkun(akunBiayaPenyusutan, getAkunBiayaPenyusutanA(),
				getKelompokAsset() == null ? null : getKelompokAsset().getAkunBiayaPenyusutan());
	}

	/**
	 * Memilih satu dari tiga sumber akun menurut urutan prioritas tetap.
	 *
	 * <p>Inti bersama ketiga metode {@code akun...Efektif()}: nilai milik katalog sendiri lebih
	 * dulu, lalu akun warisan bentuk FK tunggal (dibungkus ke bentuk JSON oleh
	 * {@link #bungkusAkun(Akun)}), lalu nilai milik kelompok aset. Bila ketiganya tidak memuat
	 * akun, dikembalikan {@code Pertangungjawaban.DEFAULT_FORMULA}.</p>
	 *
	 * <p>Metode ini murni membaca -- tidak menyentuh field mana pun -- sehingga aman dipanggil
	 * berkali-kali tanpa risiko menuliskan nilai turunan ke basis data.</p>
	 *
	 * @param milikAset     teks JSON akun milik katalog ini (nilai MENTAH, bukan hasil getter)
	 * @param warisanAset   akun bentuk FK tunggal milik katalog ini; boleh {@code null}
	 * @param milikKelompok teks JSON akun milik kelompok aset; boleh {@code null}
	 * @return teks JSON akun terpilih, atau formula default bila tidak ada yang memuat akun
	 */
	private String pilihAkun(String milikAset, Akun warisanAset, String milikKelompok) {
		if (adaAkun(milikAset)) {
			return milikAset;
		}
		String dariWarisan = bungkusAkun(warisanAset);
		if (dariWarisan != null) {
			return dariWarisan;
		}
		if (adaAkun(milikKelompok)) {
			return milikKelompok;
		}
		return Pertangungjawaban.DEFAULT_FORMULA;
	}

	/**
	 * true bila teks JSON memuat sedikitnya satu entri berakun.
	 *
	 * <p>Diperiksa dengan menguraikan isinya, bukan dengan {@code contains("key")} seperti kode
	 * lama: penanda itu heuristik, dan array berisi akun tanpa {@code key} akan dikira kosong.</p>
	 *
	 * <p>Teks {@code null}, kosong, atau persis sama dengan
	 * {@code Pertangungjawaban.DEFAULT_FORMULA} langsung dianggap tidak berakun tanpa penguraian.
	 * Teks yang gagal diurai (bukan JSON array yang sah) juga dianggap tidak berakun; kegagalan
	 * itu dicatat ke {@code ErrorAuditUtil} alih-alih ditelan diam-diam, sehingga data rusak
	 * tetap meninggalkan jejak yang bisa ditelusuri.</p>
	 *
	 * @param teks teks JSON array akun yang diperiksa; boleh {@code null}
	 * @return {@code true} bila ada sedikitnya satu entri dengan properti {@code akun} terisi
	 */
	private static boolean adaAkun(String teks) {
		if (teks == null || teks.trim().isEmpty()
				|| Pertangungjawaban.DEFAULT_FORMULA.equals(teks.trim())) {
			return false;
		}
		try {
			JSONArray array = new JSONArray(teks);
			for (int i = 0; i < array.length(); i++) {
				JSONObject o = array.optJSONObject(i);
				if (o != null && !o.isNull("akun")) {
					return true;
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "MasterAsset.adaAkun");
		}
		return false;
	}

	/**
	 * Bungkus akun warisan (kolom FK lama) ke bentuk JSON yang sama; null bila tidak ada.
	 *
	 * <p>Menghasilkan array berisi satu objek dengan properti {@code akun} (id akun) dan
	 * {@code key} (bilangan acak positif sebagai penanda baris). Bentuk itu membuat akun warisan
	 * dapat dikonsumsi mesin posting dengan jalur kode yang sama seperti akun JSON per satuan
	 * kerja, tanpa cabang khusus di sisi pemanggil.</p>
	 *
	 * <p>Perlu dicatat bahwa {@code key} dibangkitkan ACAK setiap pemanggilan, sehingga dua
	 * pemanggilan berturut-turut atas akun warisan yang sama menghasilkan teks berbeda. Jangan
	 * pakai hasil metode ini untuk membandingkan kesamaan pemetaan akun.</p>
	 *
	 * @param akun akun bentuk FK tunggal; boleh {@code null}
	 * @return teks JSON array berisi satu entri akun, atau {@code null} bila akun tidak ada,
	 *         belum tersimpan (id {@code null}), atau penyusunan JSON-nya gagal
	 */
	private static String bungkusAkun(Akun akun) {
		if (akun == null || akun.getId() == null) {
			return null;
		}
		try {
			JSONArray array = new JSONArray();
			JSONObject o = new JSONObject();
			o.put("akun", akun.getId());
			o.put("key", Long.valueOf(Math.abs(Common.randLong())));
			array.put(o);
			return array.toString();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "MasterAsset.bungkusAkun");
			return null;
		}
	}


	/**
	 * Tipe barang -- getter DESTRUKTIF yang MENEBAK tipe dari nama kelompok dan nama jenis.
	 *
	 * <h3>Aturan penebakan</h3>
	 *
	 * <p>Bila tipe belum pernah ditetapkan ({@code null}) DAN katalog punya kelompok aset maupun
	 * jenis aset, metode menebak tipe dengan mencocokkan POTONGAN KATA pada nama keduanya, tanpa
	 * memandang besar-kecil huruf. Tipe menjadi {@code TIPE_TIDAK_HABIS_PAKAI} bila salah satu
	 * dari tujuh pola berikut cocok: nama kelompok memuat "barang", "inventaris", atau "asset";
	 * atau nama jenis memuat "kantor", "kendaraan", "tanah", atau "bangun". Bila tidak satu pun
	 * cocok, tipe menjadi {@code TIPE_HABIS_PAKAI}.</p>
	 *
	 * <p>Perhatikan bahwa {@code TIPE_JASA} dan {@code TIPE_TIDAK_HABIS_PAKAI_NON_ASET} TIDAK
	 * pernah dihasilkan penebakan ini; keduanya hanya dapat ditetapkan pengguna lewat
	 * {@link #setTipe(String)}.</p>
	 *
	 * <h3>Kerapuhan yang perlu diketahui</h3>
	 *
	 * <p>Penebakan ini bersandar pada teks bebas yang diketik pengguna saat membuat kelompok dan
	 * jenis aset, sehingga hasilnya bergantung pada kebiasaan penamaan tiap instalasi. Beberapa
	 * akibat yang mudah terlewat: kata "bangun" ikut mencocokkan "pembangunan" dan "bangunan";
	 * kelompok bernama "Barang Habis Pakai" justru memuat kata "barang" sehingga ditebak sebagai
	 * TIDAK habis pakai -- kebalikan dari maksudnya; dan kelompok yang dinamai "Aset" dengan
	 * ejaan Indonesia (satu huruf s) tidak cocok dengan pola "asset". Karena itu, tipe hasil
	 * tebakan sebaiknya diperiksa dan ditetapkan manual untuk katalog yang penting.</p>
	 *
	 * <h3>Efek samping penyimpanan</h3>
	 *
	 * <p>Hasil tebakan ditulis ke field {@link #tipe}. Karena entitas memakai akses PROPERTI,
	 * Hibernate memanggil getter ini saat pemeriksaan perubahan, sehingga tebakan itu tersimpan
	 * PERMANEN pada flush berikutnya. Sifat ini sekaligus menjelaskan mengapa penebakan hanya
	 * berjalan sekali: begitu tersimpan, {@code tipe} tidak lagi {@code null} dan penebakan tidak
	 * pernah diulang -- termasuk bila kelompok atau jenis asetnya kemudian diganti. Metode juga
	 * menugaskan ulang field {@link #kelompokAsset} dari {@link #getKelompokAsset()} di baris
	 * pertamanya, sehingga proxy lazy ikut teresolusi sebagai efek samping.</p>
	 *
	 * <p>Satu kejanggalan yang layak dicatat: penjaga memeriksa {@code jenisAsset} lewat FIELD
	 * langsung, bukan lewat {@link #getJenisAsset()}. Bila jenis aset masih berupa proxy yang
	 * belum teresolusi, field itu tetap terisi sehingga penjaga lolos, tetapi pemanggilan
	 * {@code jenisAsset.getNama()} sesudahnya bergantung pada sesi Hibernate yang masih terbuka.</p>
	 *
	 * @return tipe barang; salah satu konstanta {@code TIPE_*}, atau {@code null} bila belum
	 *         ditetapkan dan kelompok maupun jenis asetnya kosong
	 */
	public String getTipe() {
		kelompokAsset = getKelompokAsset();
		if (tipe == null && kelompokAsset != null && jenisAsset != null) {
			if (kelompokAsset.getNama().toLowerCase().contains("barang")
					|| kelompokAsset.getNama().toLowerCase().contains("inventaris")
					|| jenisAsset.getNama().toLowerCase().contains("kantor")
					|| jenisAsset.getNama().toLowerCase().contains("kendaraan")
					|| jenisAsset.getNama().toLowerCase().contains("tanah")
					|| jenisAsset.getNama().toLowerCase().contains("bangun")
					|| kelompokAsset.getNama().toLowerCase().contains("asset")) {
				tipe = TIPE_TIDAK_HABIS_PAKAI;
			} else {
				tipe = TIPE_HABIS_PAKAI;
			}
		}
		return tipe;
	}

	/**
	 * Menetapkan tipe barang secara eksplisit.
	 *
	 * <p>Menetapkan nilai di sini MEMATIKAN penebakan otomatis {@link #getTipe()} untuk
	 * seterusnya, karena penebakan hanya berjalan saat tipe masih {@code null}. Inilah satu-satunya
	 * cara menetapkan {@code TIPE_JASA} dan {@code TIPE_TIDAK_HABIS_PAKAI_NON_ASET}.</p>
	 *
	 * @param tipe tipe baru; sebaiknya salah satu konstanta {@code TIPE_*}
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Spesifikasi teknis rinci barang (kolom bertipe {@code text}).
	 *
	 * <p>Mengembalikan teks kosong (bukan {@code null}) bila belum terisi, agar komponen ZK yang
	 * terikat langsung ke properti ini tidak menampilkan "null".</p>
	 *
	 * @return spesifikasi teknis; {@code ""} bila belum terisi
	 */
	@Column(name = "spesifikasi", columnDefinition = "text")
	public String getSpesifikasi() {
		return spesifikasi == null ? "" : spesifikasi;
	}

	/**
	 * Mengisi spesifikasi teknis barang.
	 *
	 * @param spesifikasi teks spesifikasi baru
	 */
	public void setSpesifikasi(String spesifikasi) {
		this.spesifikasi = spesifikasi;
	}

	/**
	 * Penggolongan kategori barang, mendatar terhadap {@link #getJenisAsset()}.
	 *
	 * <p>Tidak ikut dibaca penebakan tipe pada {@link #getTipe()}; murni untuk pengelompokan
	 * tampilan dan pelaporan.</p>
	 *
	 * @return kategori aset, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kategori_asset", nullable = true)
	public KategoriAsset getKategoriAsset() {
		kategoriAsset = check(kategoriAsset);
		return kategoriAsset;
	}

	/**
	 * Menetapkan kategori barang.
	 *
	 * @param kategoriAsset kategori baru, boleh {@code null}
	 */
	public void setKategoriAsset(KategoriAsset kategoriAsset) {
		this.kategoriAsset = kategoriAsset;
	}

	/**
	 * Penanda barang boleh dipinjam lewat modul peminjaman aset.
	 *
	 * <p>Nilai cadangannya {@code false} -- lebih ketat, sehingga katalog lama yang kolomnya
	 * masih {@code NULL} tidak otomatis terbuka untuk dipinjam.</p>
	 *
	 * @return {@code true} bila barang boleh dipinjam
	 */
	public Boolean getBolehDipinjam() {
		return bolehDipinjam == null ? false : bolehDipinjam;
	}

	/**
	 * Menetapkan penanda boleh dipinjam.
	 *
	 * @param bolehDipinjam {@code true} bila barang boleh dipinjam
	 */
	public void setBolehDipinjam(Boolean bolehDipinjam) {
		this.bolehDipinjam = bolehDipinjam;
	}

	/**
	 * Penanda harga barang boleh diubah saat transaksi.
	 *
	 * <p>Berbeda dari dua penanda lain di kelas ini, nilai cadangannya {@code true} -- LEBIH
	 * LONGGAR. Katalog lama yang kolomnya masih {@code NULL} karena itu memperbolehkan harga
	 * diubah. Perbedaan arah nilai cadangan ini disengaja: menutup harga secara default akan
	 * memblokir alur pengadaan yang sudah berjalan pada data lama.</p>
	 *
	 * @return {@code true} bila harga boleh diubah saat transaksi
	 */
	public Boolean getHargaBolehDiubah() {
		return hargaBolehDiubah == null ? true : hargaBolehDiubah;
	}

	/**
	 * Menetapkan penanda harga boleh diubah.
	 *
	 * @param hargaBolehDiubah {@code true} bila harga boleh diubah saat transaksi
	 */
	public void setHargaBolehDiubah(Boolean hargaBolehDiubah) {
		this.hargaBolehDiubah = hargaBolehDiubah;
	}

	/**
	 * Penanda barang boleh dijual.
	 *
	 * <p>Nilai cadangannya {@code false}, sehingga katalog lama tidak otomatis terbuka untuk
	 * dijual.</p>
	 *
	 * @return {@code true} bila barang boleh dijual
	 */
	public Boolean getBolehDijual() {
		return bolehDijual == null ? false : bolehDijual;
	}

	/**
	 * Menetapkan penanda boleh dijual.
	 *
	 * @param bolehDijual {@code true} bila barang boleh dijual
	 */
	public void setBolehDijual(Boolean bolehDijual) {
		this.bolehDijual = bolehDijual;
	}

	/**
	 * Harga jual default bila barang boleh dijual.
	 *
	 * <p>Bernilai {@code 0.0} bila belum terisi. Perlu dicatat bahwa nilainya tidak dikaitkan
	 * dengan {@link #getBolehDijual()}: harga jual dapat terisi pada katalog yang penanda
	 * jualnya {@code false}, dan sebaliknya.</p>
	 *
	 * @return harga jual default; {@code 0.0} bila belum terisi
	 */
	public Double getHargaJualDefault() {
		return hargaJualDefault == null ? 0.0 : hargaJualDefault;
	}

	/**
	 * Menetapkan harga jual default.
	 *
	 * @param hargaJualDefault harga jual default baru
	 */
	public void setHargaJualDefault(Double hargaJualDefault) {
		this.hargaJualDefault = hargaJualDefault;
	}

}
