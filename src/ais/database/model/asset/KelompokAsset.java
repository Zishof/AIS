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

import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.surat.NomorSurat;

/**
 * KELOMPOK ASET BERJENJANG -- klasifikasi di atas katalog, pembawa pemetaan akun dan estimasi
 * umur pakai bagi seluruh jenis barang di bawahnya.
 *
 * <h3>Posisi dalam struktur modul aset</h3>
 *
 * <p>Entitas ini berada satu tingkat DI ATAS {@link MasterAsset}: arah kolom FK-nya jelas --
 * {@code asset.master_asset} memuat kolom {@code kelompok_asset} yang menunjuk ke sini,
 * sedangkan tabel ini tidak memuat kolom apa pun yang menunjuk balik. Susunan lengkap modul aset
 * karena itu berlapis lima: {@code KelompokAsset} (kelompok berjenjang) -&gt;
 * {@link MasterAsset} (katalog jenis barang) -&gt; {@link Asset} (kepemilikan satu jenis oleh
 * satu satuan kerja) -&gt; {@link AssetDetail} (unit fisik ber-barcode) -&gt;
 * {@link PenyusutanAsset} (jadwal penyusutan per unit).</p>
 *
 * <p>Kelompok ini sendiri berjenjang lewat {@link #getInduk()} yang menunjuk ke baris
 * {@code KelompokAsset} lain, sehingga instalasi dapat menyusun pohon klasifikasi sedalam yang
 * dibutuhkan (mis. "Aset Tetap" -&gt; "Peralatan" -&gt; "Peralatan Komputer").</p>
 *
 * <h3>Apa yang diwariskan kelompok ini ke bawah</h3>
 *
 * <ul>
 * <li><b>Pemetaan akun.</b> Tiga bidang akun ({@link #getAkunTransaksi()},
 *     {@link #getAkunPenyusutan()}, {@link #getAkunBiayaPenyusutan()}) dibaca sebagai pilihan
 *     KETIGA -- yakni terakhir -- oleh ketiga metode {@code akun...Efektif()} pada
 *     {@link MasterAsset}. Katalog yang sudah punya akunnya sendiri mengalahkan kelompok;</li>
 * <li><b>Akun beban pokok penjualan.</b> {@link #getAkunBebanPokokPenjualan()} berdiri sendiri
 *     dan tidak punya padanan di {@link MasterAsset};</li>
 * <li><b>Estimasi umur pakai.</b> {@link #getEstimasiUmurPakai()} diambil ALIH oleh
 *     {@link MasterAsset#getUmurEkonomis()} tanpa syarat (selama nilainya di atas {@code 1}),
 *     lalu merambat turun sampai menjadi pembagi beban penyusutan tiap unit fisik;</li>
 * <li><b>Penanda aset tetap.</b> {@link #getMerupakanAssetFix()} menentukan digit terakhir
 *     barcode unit pada {@code AssetDetail.generateBarcode};</li>
 * <li><b>Templat penomoran.</b> {@link #getNomorSurat()} mengalihkan pembuatan barcode unit dari
 *     strategi terstruktur bawaan ke strategi templat nomor surat.</li>
 * </ul>
 *
 * <h3>Tidak ada isolasi tenant di tingkat ini</h3>
 *
 * <p>Seperti {@link MasterAsset}, entitas ini TIDAK memiliki kolom {@code satuan_kerja} maupun
 * penanda tenant lain -- tidak langsung (pola satu tingkat seperti {@code Toko}) maupun tak
 * langsung (pola dua tingkat seperti {@code Koperasi}). Kelompok aset dipakai bersama seluruh
 * satuan kerja dalam satu instalasi; pemisahan per tenant baru mulai berlaku di tingkat
 * {@link Asset}. Pemisahan per satuan kerja pada tingkat ini justru terjadi DI DALAM nilai
 * kolom, bukan di antarbarisnya: tiap kolom akun berisi teks JSON array yang memetakan satu
 * akun per satuan kerja. Entitas ini juga tidak terdaftar pada Generic CRUD v2.</p>
 *
 * <h3>Riwayat versi</h3>
 *
 * <p>{@code @Audited} merekam tiap revisi ke tabel bayangan {@code asset.kelompok_asset_AUD},
 * dengan satu pengecualian yang disengaja pada
 * {@link #getMerupakanPekerjaanDalamPelaksanaan()}.</p>
 *
 * @see MasterAsset katalog jenis barang di bawahnya
 * @see Asset kepemilikan per satuan kerja
 * @see AssetDetail unit fisik individual
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "kelompok_asset")
public class KelompokAsset extends GeneralValueObject {

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
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}

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
	 * <p>Membaca field {@code nama} langsung sehingga tidak memicu efek samping apa pun.</p>
	 *
	 * @return teks berbentuk {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama kelompok.
	 *
	 * <p>Bukan sekadar label: {@link MasterAsset#getTipe()} MENCOCOKKAN potongan kata pada nama
	 * ini ("barang", "inventaris", "asset") untuk menebak apakah katalog di bawahnya tergolong
	 * habis pakai atau tidak. Penamaan kelompok karena itu berpengaruh pada perilaku, bukan hanya
	 * pada tampilan.</p>
	 */
	private String nama;

	/** Keterangan bebas. */
	private String keterangan;

	/** Kelompok induk pada pohon klasifikasi; {@code null} berarti kelompok tingkat teratas. */
	private KelompokAsset induk;

	/** Akun biaya penyusutan per satuan kerja, teks JSON; kolom {@code akun_biaya_penyusutan_str}. */
	private String akunBiayaPenyusutan;

	/** Akun aset/persediaan per satuan kerja, teks JSON; kolom {@code akun_transaksi_str}. */
	private String akunTransaksi;

	/** Akun akumulasi penyusutan per satuan kerja, teks JSON; kolom {@code akun_penyusutan_str}. */
	private String akunPenyusutan;

	/** Akun beban pokok penjualan per satuan kerja, teks JSON; lihat {@link #getAkunBebanPokokPenjualan()}. */
	private String akunBebanPokokPenjualan;

	/** Estimasi umur pakai kelompok; diambil alih {@link MasterAsset#getUmurEkonomis()}. */
	private Double estimasiUmurPakai;

	/** Penanda kelompok merupakan aset tetap; lihat {@link #getMerupakanAssetFix()}. */
	private Boolean merupakanAssetFix;

	/** Penanda pekerjaan dalam pelaksanaan; lihat {@link #getMerupakanPekerjaanDalamPelaksanaan()}. */
	private Boolean merupakanPekerjaanDalamPelaksanaan;

	/** Templat penomoran surat; bila terisi, mengubah cara barcode unit dibuat. */
	private NomorSurat nomorSurat;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan ZK data binding.
	 */
	public KelompokAsset() {
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
	 * Nama kelompok.
	 *
	 * <p>Perlu diingat bahwa nama ini ikut dibaca penebakan tipe pada
	 * {@link MasterAsset#getTipe()}; mengubahnya dapat mengubah tipe yang ditebak untuk katalog
	 * BARU di bawah kelompok ini. Katalog yang tipenya sudah pernah tersimpan tidak terpengaruh,
	 * karena penebakan di sana hanya berjalan sekali.</p>
	 *
	 * @return nama kelompok hasil {@code trim()}, atau {@code null} bila belum terisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama kelompok.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas untuk kelompok ini.
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
	 * Kelompok induk pada pohon klasifikasi -- acuan ke diri sendiri.
	 *
	 * <p>{@code null} berarti kelompok ini berada di tingkat teratas. Perlu diketahui bahwa
	 * penjenjangan ini bersifat MURNI TAMPILAN: tidak ada satu pun perilaku di modul aset yang
	 * menelusuri rantai induk. Pewarisan akun pada {@link MasterAsset} membaca kelompok LANGSUNG
	 * milik katalog saja, tanpa naik ke induknya bila kelompok itu belum memetakan akun; hal yang
	 * sama berlaku untuk estimasi umur pakai dan penanda aset tetap. Kelompok anak karena itu
	 * harus memetakan sendiri akun dan umur pakainya, tidak cukup mengandalkan induknya.</p>
	 *
	 * <p>Tidak ada pula penjagaan terhadap lingkaran acuan: menetapkan kelompok sebagai induknya
	 * sendiri, langsung maupun berputar, tidak akan ditolak entitas ini.</p>
	 *
	 * @return kelompok induk, atau {@code null} bila kelompok ini tingkat teratas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "induk", nullable = true)
	public KelompokAsset getInduk() {
		induk = check(induk);
		return induk;
	}

	/**
	 * Menetapkan kelompok induk.
	 *
	 * @param induk kelompok induk baru, boleh {@code null}
	 */
	public void setInduk(KelompokAsset induk) {
		this.induk = induk;
	}

	/**
	 * Akun Biaya Penyusutan kelompok, format JSON array berisi satu akun per satuan kerja.
	 *
	 * <p>Dibaca sebagai sumber KETIGA -- terakhir -- oleh
	 * {@link MasterAsset#akunBiayaPenyusutanEfektif()}, sesudah akun milik katalog sendiri dan
	 * akun warisan bentuk FK tunggal. Ini adalah akun yang di-DEBIT saat beban penyusutan
	 * periodik dijurnal, berpasangan dengan {@link #getAkunPenyusutan()} di sisi kredit.</p>
	 *
	 * <p>Berbeda dari getter serupa di {@link MasterAsset}, getter di kelas ini tidak diberi
	 * catatan "jangan mengambil dari tingkat di atasnya" -- karena kelompok memang sudah tingkat
	 * teratas pemetaan akun, dan penjenjangan {@link #getInduk()} tidak pernah ditelusuri.
	 * Meski begitu, ia tetap dipetakan {@code @Column} pada entitas ber-akses PROPERTI, sehingga
	 * apa pun yang dikembalikannya berpotensi tertulis ke kolom pada flush berikutnya; itulah
	 * sebabnya getter ini sengaja tidak menyentuh field apa pun.</p>
	 *
	 * @return teks JSON akun biaya penyusutan, atau {@code Pertangungjawaban.DEFAULT_FORMULA}
	 *         bila kosong
	 */
	@Column(name = "akun_biaya_penyusutan_str", columnDefinition = "text")
	public String getAkunBiayaPenyusutan() {
		return akunBiayaPenyusutan == null || akunBiayaPenyusutan.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA
				: akunBiayaPenyusutan;
	}

	/**
	 * Mengisi teks JSON akun biaya penyusutan kelompok.
	 *
	 * @param akunBiayaPenyusutan teks JSON array akun per satuan kerja
	 */
	public void setAkunBiayaPenyusutan(String akunBiayaPenyusutan) {
		this.akunBiayaPenyusutan = akunBiayaPenyusutan;
	}

	/**
	 * Akun Aset / Persediaan kelompok, format JSON array berisi satu akun per satuan kerja.
	 *
	 * <p>Dibaca sebagai sumber KETIGA oleh {@link MasterAsset#akunTransaksiEfektif()}. Inilah
	 * akun yang di-DEBIT saat perolehan aset dijurnal dan di-KREDIT saat barang keluar --
	 * termasuk sebagai lawan kredit posting HPP penjualan kantin, lihat
	 * {@link #getAkunBebanPokokPenjualan()}.</p>
	 *
	 * @return teks JSON akun aset/persediaan, atau {@code Pertangungjawaban.DEFAULT_FORMULA}
	 *         bila kosong
	 */
	@Column(name = "akun_transaksi_str", columnDefinition = "text")
	public String getAkunTransaksi() {
		return akunTransaksi == null || akunTransaksi.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA : akunTransaksi;
	}

	/**
	 * Mengisi teks JSON akun aset / persediaan kelompok.
	 *
	 * @param akunTransaksi teks JSON array akun per satuan kerja
	 */
	public void setAkunTransaksi(String akunTransaksi) {
		this.akunTransaksi = akunTransaksi;
	}

	/**
	 * Akun Akumulasi Penyusutan kelompok, format JSON array berisi satu akun per satuan kerja.
	 *
	 * <p>Dibaca sebagai sumber KETIGA oleh {@link MasterAsset#akunPenyusutanEfektif()}. Akun
	 * kontra-aset ini di-KREDIT saat beban penyusutan periodik dijurnal, berpasangan dengan
	 * {@link #getAkunBiayaPenyusutan()} di sisi debit.</p>
	 *
	 * @return teks JSON akun akumulasi penyusutan, atau
	 *         {@code Pertangungjawaban.DEFAULT_FORMULA} bila kosong
	 */
	@Column(name = "akun_penyusutan_str", columnDefinition = "text")
	public String getAkunPenyusutan() {
		return akunPenyusutan == null || akunPenyusutan.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA : akunPenyusutan;
	}

	/**
	 * Mengisi teks JSON akun akumulasi penyusutan kelompok.
	 *
	 * @param akunPenyusutan teks JSON array akun per satuan kerja
	 */
	public void setAkunPenyusutan(String akunPenyusutan) {
		this.akunPenyusutan = akunPenyusutan;
	}

	/**
	 * Akun Beban Pokok Penjualan (HPP) per satuan kerja, format JSON array (sama dengan
	 * {@link #getAkunTransaksi()} dkk). Dipakai sebagai akun DEBIT saat posting HPP penjualan kantin
	 * (lihat {@code PostingHppKantinAction}); akun KREDIT-nya = {@link #getAkunTransaksi()} (persediaan).
	 *
	 * <p>Bidang ini adalah satu-satunya pemetaan akun di kelas ini yang TIDAK punya padanan di
	 * {@link MasterAsset}, sehingga tidak ada mekanisme "akun efektif" berjenjang untuknya:
	 * pemanggil membacanya langsung dari kelompok. Katalog tidak dapat menimpanya.</p>
	 *
	 * @return teks JSON akun beban pokok penjualan, atau
	 *         {@code Pertangungjawaban.DEFAULT_FORMULA} bila kosong
	 */
	@Column(name = "akun_beban_pokok_penjualan_str", columnDefinition = "text")
	public String getAkunBebanPokokPenjualan() {
		return akunBebanPokokPenjualan == null || akunBebanPokokPenjualan.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA
				: akunBebanPokokPenjualan;
	}

	/**
	 * Mengisi teks JSON akun beban pokok penjualan kelompok.
	 *
	 * @param akunBebanPokokPenjualan teks JSON array akun per satuan kerja
	 */
	public void setAkunBebanPokokPenjualan(String akunBebanPokokPenjualan) {
		this.akunBebanPokokPenjualan = akunBebanPokokPenjualan;
	}

	/**
	 * Estimasi umur pakai kelompok, dalam satuan periode penyusutan.
	 *
	 * <p>Getter ini sendiri murni membaca -- tidak menyentuh field. Namun nilainya diambil ALIH
	 * oleh {@link MasterAsset#getUmurEkonomis()} tanpa syarat selama lebih besar dari {@code 1},
	 * dan getter di sana MENULIS BALIK ke fieldnya, sehingga umur ekonomis yang disunting
	 * pengguna pada layar Master Aset tidak akan bertahan. Perhatikan ambangnya: nilai tepat
	 * {@code 1} diabaikan, hanya nilai di atasnya yang mengambil alih.</p>
	 *
	 * <p>Dari katalog, nilai ini merambat lagi ke {@link AssetDetail#getUmurEkonomis()} lalu
	 * menjadi PEMBAGI beban penyusutan pada {@link PenyusutanAsset#getNilaiPenyusutan()}.
	 * Menyunting satu angka di sini karena itu dapat mengubah beban penyusutan seluruh unit
	 * fisik di bawah kelompok ini -- termasuk untuk periode yang sudah diposting ke buku besar,
	 * karena nilai penyusutan pun dihitung ulang lewat getter, bukan dibaca dari kolom
	 * tersimpan.</p>
	 *
	 * @return estimasi umur pakai; {@code 0.0} bila belum terisi
	 */
	public Double getEstimasiUmurPakai() {
		return estimasiUmurPakai == null ? 0.0 : estimasiUmurPakai;
	}

	/**
	 * Menetapkan estimasi umur pakai kelompok.
	 *
	 * @param estimasiUmurPakai estimasi umur pakai baru, dalam satuan periode penyusutan
	 */
	public void setEstimasiUmurPakai(Double estimasiUmurPakai) {
		this.estimasiUmurPakai = estimasiUmurPakai;
	}

	/**
	 * Penanda "Merupakan Aset Tetap" -- kelompok ini dicatat sebagai aset tetap, bukan
	 * persediaan.
	 *
	 * <p>Nilai cadangannya {@code true} -- LEBIH LONGGAR daripada kebiasaan penanda boolean lain
	 * di modul ini. Kelompok lama yang kolomnya masih {@code NULL} karena itu dianggap aset
	 * tetap, bukan sebaliknya. Perbedaan arah ini perlu diingat saat menelusuri data hasil
	 * migrasi.</p>
	 *
	 * <p>Penanda ini menentukan digit TERAKHIR barcode unit pada strategi penomoran terstruktur
	 * {@code AssetDetail.generateBarcode}: {@code "1"} bila bernilai {@code true}, {@code "2"}
	 * bila {@code false}, dan {@code "0"} bila katalognya sama sekali tidak punya kelompok.
	 * Mengubah penanda ini tidak menomori ulang unit yang barcode-nya sudah tercetak, sehingga
	 * barcode lama dan baru dapat berbeda digit terakhirnya dalam satu kelompok yang sama.</p>
	 *
	 * @return {@code true} bila kelompok merupakan aset tetap
	 */
	public Boolean getMerupakanAssetFix() {
		return merupakanAssetFix == null ? true : merupakanAssetFix;
	}

	/**
	 * Menetapkan penanda aset tetap.
	 *
	 * @param merupakanAssetFix {@code true} bila kelompok merupakan aset tetap
	 */
	public void setMerupakanAssetFix(Boolean merupakanAssetFix) {
		this.merupakanAssetFix = merupakanAssetFix;
	}

	/**
	 * Penanda "Merupakan Pekerjaan Dalam Pelaksanaan" (Construction in Progress). Disimpan sama
	 * seperti {@link #getMerupakanAssetFix()}. {@code @NotAudited}: kolom ditambahkan otomatis ke
	 * tabel utama oleh hbm2ddl=update tanpa perlu sinkron manual tabel audit.
	 *
	 * <p>Berbeda dari {@link #getMerupakanAssetFix()}, nilai cadangannya {@code false} -- lebih
	 * ketat, sehingga kelompok lama tidak mendadak dianggap pekerjaan dalam pelaksanaan.</p>
	 *
	 * <p>Akibat langsung {@code @NotAudited}: perubahan penanda ini TIDAK terekam di tabel
	 * riwayat {@code asset.kelompok_asset_AUD}. Menelusuri kapan sebuah kelompok mulai atau
	 * berhenti ditandai sebagai pekerjaan dalam pelaksanaan karena itu tidak mungkin dilakukan
	 * lewat Envers; hanya keadaan terkininya yang tersedia. Pengecualian ini disengaja demi
	 * menghindari keharusan menyelaraskan skema tabel audit secara manual.</p>
	 *
	 * @return {@code true} bila kelompok merupakan pekerjaan dalam pelaksanaan
	 */
	@org.hibernate.envers.NotAudited
	public Boolean getMerupakanPekerjaanDalamPelaksanaan() {
		return merupakanPekerjaanDalamPelaksanaan == null ? false : merupakanPekerjaanDalamPelaksanaan;
	}

	/**
	 * Menetapkan penanda pekerjaan dalam pelaksanaan.
	 *
	 * @param merupakanPekerjaanDalamPelaksanaan {@code true} bila kelompok merupakan pekerjaan
	 *                                           dalam pelaksanaan
	 */
	public void setMerupakanPekerjaanDalamPelaksanaan(Boolean merupakanPekerjaanDalamPelaksanaan) {
		this.merupakanPekerjaanDalamPelaksanaan = merupakanPekerjaanDalamPelaksanaan;
	}

	/**
	 * Templat penomoran surat yang dipakai kelompok ini.
	 *
	 * <p>Relasi ini bukan sekadar pelengkap: keberadaannya MENGUBAH cara barcode unit fisik
	 * dibuat. Selama kelompok belum punya templat, {@code AssetDetail.generateBarcode} memakai
	 * strategi terstruktur bawaan modul aset (kode satuan kerja, kode katalog, nomor urut tiga
	 * digit, bulan-tahun, dan digit penanda aset tetap). Begitu templat ini terisi, penomoran
	 * beralih ke strategi templat nomor surat -- termasuk seluruh pengaturan reset urutannya per
	 * tahun, per bulan, atau per tanggal tertentu.</p>
	 *
	 * <p>Perlu diketahui bahwa pengaturan reset per tahun dan per bulan tidak berfungsi
	 * sebagaimana mestinya pada aset: pembatas yang dipasang
	 * {@code AssetDetail.getindex(NomorSurat)} menyaring kolom {@code tahun} dan {@code bulan}
	 * milik {@code AssetDetail} yang tidak pernah terisi, sehingga hitungannya selalu nol dan
	 * nomor yang dihasilkan selalu {@code 1}. Rinciannya ada pada
	 * {@link AssetDetail#getTahun()} dan {@link AssetDetail#getindex(NomorSurat)}. Selama
	 * kebutuhan instalasi belum menuntut reset berkala, templat tanpa reset bekerja normal.</p>
	 *
	 * @return templat penomoran surat, atau {@code null} bila kelompok memakai penomoran bawaan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat", nullable = true)
	public NomorSurat getNomorSurat() {
		nomorSurat = check(nomorSurat);
		return nomorSurat;
	}

	/**
	 * Menetapkan templat penomoran surat kelompok.
	 *
	 * @param nomorSurat templat penomoran baru, boleh {@code null}
	 */
	public void setNomorSurat(NomorSurat nomorSurat) {
		this.nomorSurat = nomorSurat;
	}
}
