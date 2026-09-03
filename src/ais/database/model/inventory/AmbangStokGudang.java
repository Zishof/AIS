package ais.database.model.inventory;

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
import ais.database.model.sirs.Gudang;

/**
 * Ambang stok minimum sebuah {@link Produk} (biasanya bahan baku) PER {@link Gudang} -- fitur
 * "Purchase: notifikasi stok minimum otomatis 2 tingkat" (gap analisis PDF klien 2026-07-26).
 *
 * <p><b>Kenapa per-Gudang, bukan pakai {@link Produk#getStokMinimum()} yang sudah ada.</b>
 * {@code Produk.stokMinimum} adalah SATU angka datar dipakai murni sbg label peringatan "stok
 * menipis" di layar kasir/dashboard (tidak memicu apa pun secara otomatis) -- tidak bisa mewakili
 * kebutuhan PDF klien: ambang batas BERBEDA di tiap gudang untuk BAHAN BAKU yang SAMA (contoh
 * literal PDF: Tepung Terigu 10kg di gudang cabang vs 50kg di gudang pusat). Baris di sini SENGAJA
 * terpisah dari {@code Produk.stokMinimum} (yang tetap dipakai apa adanya utk peringatan kasir)
 * supaya tidak menimpa makna field lama itu.</p>
 *
 * <p>Dicek berkala oleh {@link ais.common.StokThresholdScheduler} -- lihat javadoc kelas itu untuk
 * alur lengkap (bagaimana ambang ini memicu {@link PengajuanPembelianGudang} otomatis).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "ambang_stok_gudang")
public class AmbangStokGudang extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Primary key baris ambang. Digenerasi database ({@code IDENTITY}, kolom {@code insertable = false}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Userid/nama yang terakhir MENGISI baris ini via {@link #setOleh(String)} -- lihat javadoc method tersebut untuk perilaku guard terhadap nilai kosong. */
	private String oleh;
	/** Id user yang terakhir mengisi baris ini via {@link #setOlehId(String)} -- pelengkap {@link #oleh} untuk pencarian presisi berbasis id. */
	private String olehId;

	/**
	 * Id user yang terakhir mengisi baris ambang ini. Lihat javadoc {@link #setOlehId(String)} untuk
	 * perilaku setter yang mengabaikan nilai kosong.
	 * @return id user pengisi terakhir, atau {@code null} bila belum pernah diisi dengan nilai valid.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id user yang mengisi baris ambang ini -- BUKAN setter pasif biasa. Nilai {@code null}
	 * atau string kosong/berisi-spasi-saja DIABAIKAN secara diam-diam (method langsung {@code return}
	 * tanpa mengubah field, tanpa melempar exception, tanpa log). Efek praktisnya: sekali field ini
	 * terisi nilai valid, memanggil setter ini dengan nilai kosong TIDAK PERNAH bisa mengosongkannya
	 * lagi -- berbeda dari kebanyakan setter lain di model-model klaster ini yang menerima {@code null}
	 * apa adanya. Pemanggil yang bermaksud MENGOSONGKAN field ini harus memanipulasi field secara
	 * langsung (refleksi) atau lewat query, bukan lewat setter publik ini.
	 * @param olehId id user pengisi; nilai kosong/hanya-spasi diabaikan secara diam-diam.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi userid/nama yang mengisi baris ambang ini. Perilaku guard SAMA seperti {@link
	 * #setOlehId(String)}: nilai {@code null} atau kosong/hanya-spasi diabaikan diam-diam, field tidak
	 * pernah dikosongkan kembali lewat setter ini setelah pernah terisi nilai valid.
	 * @param oleh userid/nama pengisi; nilai kosong/hanya-spasi diabaikan secara diam-diam.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Userid/nama yang terakhir mengisi baris ambang ini. Lihat javadoc {@link #setOleh(String)} untuk
	 * perilaku setter yang mengabaikan nilai kosong.
	 * @return userid/nama pengisi terakhir, atau {@code null} bila belum pernah diisi dengan nilai valid.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook lifecycle Hibernate dipanggil otomatis sebelum setiap {@code UPDATE} terhadap baris ambang ini
	 * (dipicu anotasi {@link javax.persistence.PreUpdate}, BUKAN dipanggil manual). Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menstempel ulang {@link
	 * #tanggal_dirubah}. Method ini murni hook siklus hidup entity -- tidak melakukan validasi ambang
	 * (mis. memastikan {@link #ambangMinimum} tidak melebihi {@link #maxQty}); validasi semacam itu,
	 * bila ada, berada di lapisan service/action pemanggil.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu terakhir baris ini diubah -- field audit shadow diisi otomatis oleh {@link
	 * #onUpdate()} pada tiap {@code UPDATE}, terpisah dari riwayat versi penuh envers ({@code @Audited}).
	 * Inisialisasi default memakai waktu konstruksi objek Java, bukan waktu commit transaksi.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu terakhir diubah. Normalnya TIDAK diisi manual oleh kode aplikasi --
	 *                        dikelola otomatis oleh {@link #onUpdate()}; setter ini ada untuk kebutuhan
	 *                        Hibernate (property accessor) dan skenario migrasi/backfill data historis.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu terakhir baris ini diubah, diisi otomatis oleh {@link #onUpdate()} pada tiap
	 * {@code UPDATE}.
	 * @return waktu terakhir diubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas baris ambang ini untuk kebutuhan log/debug, format {@code "id-produk@gudang"}.
	 *
	 * <p><b>Efek samping tersembunyi:</b> method ini MEMANGGIL {@link #getProduk()} dan {@link
	 * #getGudang()} (bukan membaca field {@link #produk}/{@link #gudang} mentah), yang berarti bila
	 * salah satu proxy lazy belum ter-inisialisasi, memanggil {@code toString()} pada objek ini DI LUAR
	 * sesi Hibernate aktif dapat melempar {@code LazyInitializationException} -- kejutan umum bagi kode
	 * yang memakai {@code toString()} untuk logging setelah sesi ditutup (mis. di blok {@code finally}
	 * atau exception handler yang berjalan setelah transaksi selesai). {@code getProduk()}/{@code
	 * getGudang()} juga memicu normalisasi via {@code check()} milik {@link GeneralValueObject} sebagai
	 * efek samping pemanggilan, sehingga {@code toString()} bukan operasi murni baca-saja terhadap state
	 * in-memory.
	 * @return string ringkas {@code "<id>-<produk>@<gudang>"} memakai representasi {@code toString()} dari kedua relasi.
	 */
	public String toString() {
		produk = getProduk();
		gudang = getGudang();
		return id + "-" + produk + "@" + gudang;
	}

	/** Produk (bahan baku) yang ambang stoknya diatur pada baris ini. Bersama {@link #gudang}, membentuk kunci logis (produk, gudang) untuk satu ambang -- lihat javadoc kelas untuk alasan desain per-gudang ini. */
	private Produk produk;
	/** Gudang tempat ambang ini berlaku. Bersama {@link #produk}, membentuk kunci logis (produk, gudang) -- produk yang sama bisa punya ambang berbeda di gudang berbeda (contoh PDF klien: Tepung Terigu 10kg di cabang vs 50kg di pusat). */
	private Gudang gudang;
	/** Ambang batas minimum -- lihat javadoc {@link #getAmbangMinimum()} untuk mekanisme pemicu pengajuan otomatis. */
	private Double ambangMinimum;
	/** Target stok maksimum (Fase C) -- lihat javadoc {@link #getMaxQty()} untuk pengaruhnya pada perhitungan saran qty pengajuan. */
	private Double maxQty;
	/** Penanda aktif/nonaktif baris ambang ini -- lihat javadoc {@link #getAktif()} untuk default dan efeknya pada scheduler. */
	private Boolean aktif;
	/** Catatan bebas teks untuk baris ambang ini, opsional. */
	private String keterangan;

	/** Constructor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via refleksi saat memuat baris dari database; kode aplikasi yang membuat ambang baru juga memakainya lalu mengisi field lewat setter. */
	public AmbangStokGudang() {
	}

	/**
	 * Primary key baris ambang ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
	 * pada objek yang belum pernah di-{@code save}. Kolom dideklarasikan {@code insertable = false} --
	 * konsisten dengan penggunaan {@code IDENTITY} standar Hibernate: nilai kolom diserahkan sepenuhnya
	 * ke database saat insert, Hibernate tidak pernah menyertakan kolom {@code id} secara eksplisit
	 * dalam statement {@code INSERT} yang dihasilkannya.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris. Normalnya TIDAK diisi manual -- dibiarkan Hibernate mengisinya via {@code IDENTITY} saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Produk (bahan baku) yang ambang stoknya diatur pada baris ini. Relasi {@code LAZY}, wajib diisi;
	 * getter memanggil {@code check(produk)} milik {@link GeneralValueObject} yang menormalisasi
	 * proxy/nilai kosong sebelum dikembalikan.
	 * @return produk terkait ambang ini (bisa proxy lazy, dinormalisasi via {@code check()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/** @param produk produk (bahan baku) terkait ambang ini. */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Gudang tempat ambang ini berlaku. Relasi {@code LAZY}, wajib diisi; getter memanggil {@code
	 * check(gudang)} milik {@link GeneralValueObject} yang menormalisasi proxy/nilai kosong sebelum
	 * dikembalikan. Bersama {@link #getProduk()}, pasangan (produk, gudang) adalah kunci logis satu
	 * ambang -- tidak ada {@code unique constraint} eksplisit di level entity yang menegakkan
	 * keunikan pasangan ini, jadi secara teknis lebih dari satu baris ambang untuk pasangan
	 * (produk, gudang) yang sama bisa eksis bersamaan bila proses pembuatan tidak mengecek duplikat
	 * lebih dulu.
	 * @return gudang terkait ambang ini (bisa proxy lazy, dinormalisasi via {@code check()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gudang", nullable = false)
	public Gudang getGudang() {
		gudang = check(gudang);
		return gudang;
	}

	/** @param gudang gudang tempat ambang ini berlaku. */
	public void setGudang(Gudang gudang) {
		this.gudang = gudang;
	}

	/**
	 * Ambang batas minimum (satuan sama dgn stok Produk) -- di bawah/sama dgn nilai ini memicu pengajuan
	 * otomatis. {@code null} dinormalisasi menjadi {@code 0.0}.
	 *
	 * <p><b>Verifikasi mekanisme pemicu -- BUKAN field pasif.</b> Berbeda dari kekhawatiran awal bahwa
	 * kelas ini mungkin berisi field yang dibuat tapi tidak pernah benar-benar dibaca (pola "entity
	 * yatim/tidur" yang sudah ditemukan berkali-kali di audit domain lain codebase ini), penelusuran
	 * kode pemanggil MENGKONFIRMASI field ini AKTIF dipakai: {@code ais.common.StokThresholdScheduler}
	 * berjalan berkala (dipicu dari {@code AppStartupListener}), memuat SELURUH baris {@code
	 * AmbangStokGudang} yang {@link #getAktif() aktif} lewat {@code
	 * ais.action.master.inventory.PengajuanPembelianGudangAction} (method {@code prosesSatuAmbang}),
	 * membandingkan total stok produk pada gudang bersangkutan terhadap nilai {@code ambangMinimum} ini,
	 * dan bila stok berada di bawah/sama dengan ambang, secara OTOMATIS: (1) menerbitkan draf pengajuan
	 * pembelian ({@code terbitkanWoDraf}) ke {@link PengajuanPembelianGudang} dengan saran kuantitas yang
	 * memperhitungkan {@link #getMaxQty()} bila terisi (kebijakan min-max) atau buffer 2x ambang bila
	 * {@code maxQty} kosong (perilaku lama), dan (2) mengirim notifikasi ({@code kirimNotifikasi}) ke
	 * pihak terkait. Jadi field ini adalah PEMICU nyata bagi alur bisnis pengadaan otomatis -- fitur
	 * "Purchase: notifikasi stok minimum otomatis 2 tingkat" dari gap analisis PDF klien 2026-07-26 --
	 * bukan sekadar label informatif yang tidak pernah dibaca kembali.</p>
	 *
	 * <p><b>Implikasi keakuratan data.</b> Karena scheduler bergantung sepenuhnya pada nilai ini untuk
	 * memutuskan kapan draf pembelian diterbitkan, ambang yang salah diisi (terlalu rendah = kehabisan
	 * stok tanpa peringatan tepat waktu; terlalu tinggi = draf pengajuan berlebihan/spam notifikasi) akan
	 * langsung berdampak pada operasional pengadaan riil, bukan sekadar tampilan dashboard pasif seperti
	 * {@code Produk.stokMinimum} (lihat javadoc kelas untuk perbandingan keduanya). Tidak ada validasi
	 * pada level model ini yang mencegah {@code ambangMinimum} negatif atau lebih besar dari {@link
	 * #getMaxQty()} (kombinasi yang secara logis tidak masuk akal jika kebijakan min-max Fase C dipakai)
	 * -- validasi semacam itu, bila ada, berada di lapisan UI/action pemanggil, bukan di setter/getter
	 * model ini.</p>
	 *
	 * @return ambang batas minimum, tidak pernah {@code null} (default {@code 0.0}).
	 */
	@Column(name = "ambang_minimum", nullable = false)
	public Double getAmbangMinimum() {
		return ambangMinimum == null ? 0.0 : ambangMinimum;
	}

	/** @param ambangMinimum ambang batas minimum baris ini; boleh {@code null} (dibaca sebagai {@code 0.0}). */
	public void setAmbangMinimum(Double ambangMinimum) {
		this.ambangMinimum = ambangMinimum;
	}

	/**
	 * Target stok maksimum (Fase C dok. 48 P3). Bila terisi, saran qty pengajuan/WO otomatis =
	 * {@code maxQty - stokSaatIni} (kebijakan min-max PDF klien); {@code null} = perilaku lama
	 * (buffer sederhana 2x ambang). Satuan sama dengan stok Produk (satuan dasar).
	 *
	 * <p>Dibaca oleh {@code PengajuanPembelianGudangAction.terbitkanWoDraf} bersama {@link
	 * #getAmbangMinimum()} saat menyusun kuantitas draf pengajuan -- field ini bersifat opsional
	 * ({@code nullable = true}) sehingga baris ambang lama (dibuat sebelum kebijakan min-max Fase C ada)
	 * tetap berfungsi dengan fallback buffer 2x ambang tanpa perlu migrasi data.</p>
	 *
	 * @return target stok maksimum, atau {@code null} bila memakai perilaku lama (buffer 2x ambang).
	 */
	@Column(name = "max_qty", nullable = true)
	public Double getMaxQty() {
		return maxQty;
	}

	/** @param maxQty target stok maksimum baris ini; {@code null} berarti memakai perilaku lama (buffer 2x ambang). */
	public void setMaxQty(Double maxQty) {
		this.maxQty = maxQty;
	}

	/**
	 * Penanda aktif/nonaktif baris ambang ini. {@code null} dinormalisasi menjadi {@code true} (default
	 * AKTIF) -- baris baru yang belum pernah men-set field ini eksplisit dianggap aktif secara implisit.
	 * Dibaca langsung oleh {@code PengajuanPembelianGudangAction} sebagai filter query ({@code
	 * semuaAmbang} hanya memuat baris dengan {@code aktif = true}) sebelum scheduler memprosesnya --
	 * men-set {@code false} adalah cara resmi menonaktifkan sementara satu ambang tanpa menghapus
	 * barisnya (mempertahankan riwayat/histori konfigurasi lewat envers {@code @Audited}).
	 * @return {@code true} bila baris ini aktif diproses scheduler; default {@code true} bila belum diisi.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif {@code false} untuk menonaktifkan sementara baris ambang ini dari pemrosesan scheduler tanpa menghapusnya. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Catatan bebas teks untuk baris ambang ini (mis. alasan penetapan angka ambang, referensi
	 * kebijakan). Opsional, tanpa batas panjang keras.
	 * @return catatan/keterangan baris ambang, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan catatan/keterangan baris ambang ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}
}
