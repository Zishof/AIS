package ais.database.model.inventory;

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

import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.MasterAsset;

/**
 * <h3>Katalog barang -- master produk PALING SENTRAL modul retail/kantin/koperasi.</h3>
 *
 * <p>Satu baris {@code koperasi.produk} mewakili satu item yang dapat dijual, dibeli, diproduksi,
 * diopname, atau dipakai sebagai bahan baku. Hampir seluruh entity di paket
 * {@code ais.database.model.inventory} menunjuk balik ke sini: {@link ProdukBatch} (lot fisik),
 * {@link Pembelian}/{@link PengadaanProduk}/{@link DraftPembelian} (penerimaan &amp; kulakan),
 * {@link StokOpname}/{@link SesiStokOpname} (pencacahan), {@link MutasiStokToko} &amp;
 * {@link MutasiStokProduksi} (buku besar pergerakan), {@link ReturPembelian}/{@link ReturPenjualan},
 * serta master pendukung {@link GrupProduk}, {@link JenisProduk}, {@link SatuanProduk},
 * {@link PemasokProduk}, dan {@link KebijakanRetur}. Perubahan bentuk kelas ini berdampak luas --
 * perlakukan sebagai antarmuka publik paket, bukan sebagai POJO internal.</p>
 *
 * <p><b>Dimensi tenant.</b> Satu-satunya sumbu partisi data adalah {@link #getToko()}; TIDAK ada
 * field {@code satuanKerja}/{@code yayasan}/{@code sekolah}/{@code koperasi} di kelas ini (maupun
 * di entity mana pun pada paket {@code inventory} -- sudah diverifikasi menyeluruh). Artinya
 * isolasi antar-instansi untuk seluruh katalog barang bertumpu SEPENUHNYA pada nilai kolom
 * {@code toko}, dan {@code toko} itu sendiri {@code nullable = true}: baris produk boleh berdiri
 * tanpa toko sama sekali (produk "global"/warisan). Pola ini BERBEDA dari {@code Koperasi} yang
 * tenant-nya berantai dua tingkat (anak &rarr; {@code Koperasi} &rarr; {@code SatuanKerja}); di
 * sini rantainya SATU tingkat dan berhenti di {@link Toko}, yang -- lihat javadoc {@link Toko} --
 * juga tidak menyimpan induk organisasi apa pun. Konsekuensinya penegakan lingkup harus dilakukan
 * eksplisit oleh tiap pemanggil; tidak ada mekanisme kerangka kerja yang menambahkannya otomatis.</p>
 *
 * <p><b>Generic CRUD v2.</b> Per audit revisi berjalan, {@code Produk.class} TIDAK terdaftar
 * sebagai {@code entityClass} pada adapter mana pun di bawah
 * {@code ais.action.master.generic.v2.adapter} -- katalog produk hanya dilayani Action ZK klasik
 * ({@code ProdukAction}) dan jalur helper/API ({@code KantinHelper}, {@code PriceTagUtil}, dst).
 * Perlu dicatat untuk rencana pendaftaran di masa depan: whitelist {@code scopeBindings()} pada
 * {@code GenericCrudAutoEntityAdapter} hanya mengenal properti {@code yayasan}, {@code sekolah},
 * {@code program}, {@code fakultas}, {@code jurusan}, {@code satuanKerja}, {@code mahasiswa},
 * {@code siswa}, {@code dosen}, {@code guru}, {@code orangTua}, dan {@code anggotaKoperasi} --
 * {@code toko} TIDAK ADA di dalamnya. Karena {@code addScope(...)} diam-diam melewati properti yang
 * tidak dimiliki entity target, mendaftarkan {@code Produk} ke Generic CRUD v2 apa adanya akan
 * menghasilkan scoping yang menjadi NO-OP TOTAL (nol filter): seluruh katalog lintas toko akan
 * terbaca oleh role non-admin mana pun yang punya hak READ menunya. Ini penguatan pola whitelist
 * berbasis-refleksi yang sudah tercatat, bukan celah baru.</p>
 *
 * <p><b>Stok agregat vs stok batch -- SENGAJA tidak direkonsiliasi dua arah.</b>
 * {@link #getStok()} adalah saldo AGREGAT satu angka per baris produk. {@link ProdukBatch#getStok()}
 * adalah saldo per lot fisik (produk + toko + nomor batch + tanggal expired). Keduanya berjalan
 * sebagai catatan TERPISAH: menambah/mengurangi salah satu tidak otomatis menyesuaikan yang lain,
 * dan tidak ada proses yang memaksa Σ(batch aktif) == {@code Produk.stok}. Pelacakan batch bersifat
 * OPSIONAL per produk (baris {@code ProdukBatch} hanya lahir bila payload penerimaan menyertakan
 * nomor batch DAN tanggal expired sekaligus), sehingga mayoritas produk memang hanya punya stok
 * agregat. Saat konsumsi FEFO kehabisan saldo lot, kekurangannya dibiarkan jatuh ke stok agregat --
 * lihat javadoc {@link ProdukBatch} untuk mekanisme lengkapnya. Jangan menulis kode yang
 * mengasumsikan kedua angka itu setara.</p>
 *
 * <p><b>Harga: disimpan vs dihitung.</b> Yang DISIMPAN langsung sebagai kolom adalah
 * {@link #getHargaBeli()}, {@link #getHargaJual()}, dan {@link #getHargaPack()}. Yang TIDAK disimpan
 * dan selalu dihitung/diambil saat dibutuhkan: HPP dari resep ({@link #getBahanBaku()}, Σ qty ×
 * harga snapshot), biaya pokok posting penjualan (lihat {@link #getMetodeHpp()} -- rata-rata atau
 * kulakan terakhir, dihitung dari faktur, bukan dari field), dan harga produk ekstra
 * ({@link #getEkstraPilihan()} menyimpan id mentah saja, harganya live dari produk ekstra itu).
 * Selain itu {@code hargaBeli} dapat DITIMPA otomatis oleh faktur kulakan tervalidasi kecuali
 * {@link #getHargaBeliManual()} bernilai {@code true}, dan {@code hargaBeli}+{@code hargaJual}
 * dapat DITIMPA massal oleh {@link GrupProduk} bila {@link #getGrupProduk()} terisi -- dua jalur
 * penimpaan yang berjalan independen.</p>
 *
 * <p><b>Envers.</b> Kelas ber-{@code @Audited}; setiap kolom BARU yang ditambahkan di sini wajib
 * diikuti ALTER manual pada tabel audit {@code new_audit.produk__audit}, karena
 * {@code hbm2ddl.auto=update} terbukti hanya menyinkron tabel utama. Bila dilewatkan, UPDATE apa pun
 * pada baris produk akan gagal saat menulis baris auditnya. Dua field sengaja
 * {@code @NotAudited} untuk menghindari beban itu: {@link #getKebijakanRetur()} dan
 * {@link #getMetodeHpp()}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "produk")
public class Produk extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}. Nilainya
	 * identik dengan {@code serialVersionUID} beberapa entity lain hasil generate hbm2java pada
	 * batch yang sama (mis. {@link Toko}) -- itu artefak copy-paste template generator, BUKAN
	 * indikasi kekerabatan atau kompatibilitas biner antar-kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer; lihat {@link #getId()}. */
	private Long id;

	/** Jejak audit nama pembuat/pengubah; lihat {@link #getOleh()}. */
	private String oleh;

	/** Jejak audit id pengguna pembuat/pengubah; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * {@code Tbmuser.userId} pengguna yang terakhir membuat/mengubah baris produk ini -- jejak audit
	 * ringan yang berdiri SENDIRI di samping histori Envers ({@code new_audit.produk__audit}).
	 * Keduanya sengaja hidup berdampingan: Envers menyimpan revisi lengkap tapi hanya dapat dibaca
	 * lewat query API-nya, sedangkan kolom ini ikut terbawa pada SELECT biasa sehingga grid/laporan
	 * bisa menampilkan "diubah oleh" tanpa join ke skema audit. Duplikasi ini KEHARUSAN TEKNIS,
	 * bukan redundansi yang perlu dibersihkan.
	 *
	 * @return userId pengubah terakhir, atau {@code null} bila baris belum pernah diisi jejaknya
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()} dengan PENJAGA masukan kosong: argumen {@code null} atau yang
	 * hanya berisi spasi DIABAIKAN DIAM-DIAM -- nilai lama dipertahankan, tidak ada exception dan
	 * tidak ada log. Perilaku ini disengaja supaya jalur simpan yang kebetulan tidak membawa
	 * identitas pengguna (impor Excel katalog, auto-create produk dari Pembelian/Tabungan Siswa,
	 * job terjadwal) tidak MENGHAPUS jejak audit yang sudah benar dengan menimpanya jadi kosong.
	 * Konsekuensi yang harus disadari pemanggil: field ini TIDAK BISA dikosongkan lagi lewat setter
	 * ini setelah sekali terisi. Pola penjaga yang sama dipakai {@link #setOleh(String)}, dan
	 * berbeda dari {@link ProdukBatch#setOleh(String)} yang sengaja tanpa penjaga.
	 *
	 * @param olehId userId pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Setter {@link #getOleh()} dengan penjaga masukan kosong yang sama persis dengan
	 * {@link #setOlehId(String)} -- {@code null}/spasi diabaikan diam-diam sehingga jejak audit
	 * lama tidak terhapus oleh jalur simpan yang tidak membawa identitas pengguna. Lihat javadoc
	 * setter tersebut untuk alasan dan konsekuensinya.
	 *
	 * @param oleh nama/identitas pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama/identitas tampilan pengguna yang terakhir membuat/mengubah baris ini -- pendamping
	 * {@link #getOlehId()} yang menyimpan id teknisnya. Disimpan sebagai teks beku (BUKAN relasi ke
	 * {@code Tbmuser}) supaya nama yang tercatat tetap seperti saat perubahan terjadi walau akun
	 * penggunanya kemudian diganti nama atau dihapus.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum setiap {@code UPDATE}
	 * baris produk ini, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah} yang menyetel
	 * {@link #getTanggal_dirubah()} ke waktu saat itu. Karena berjalan di tingkat penyedia
	 * persistensi, cap waktu ikut diperbarui APA PUN jalur yang mengubah baris (form ZK, helper
	 * kantin, impor Excel, API) tanpa pemanggil perlu mengingatnya. Perhatikan hook ini HANYA
	 * bereaksi pada UPDATE -- pada INSERT pertama, nilai awal berasal dari inisialisasi field
	 * {@link #tanggal_dirubah} saat objek dibuat.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Kunci unik ternormalisasi (gap-closure "Katalog Barang banyak yang double" -- lihat JavaDoc
	 * lengkap {@link ais.common.ProdukKunciUnikUtil}) -- dihitung ULANG OTOMATIS di sini SETIAP
	 * baris disimpan (insert MAUPUN update), TERLEPAS lewat jalur mana ({@code KantinHelper.produkSimpan},
	 * impor Excel, form ZK lama {@code ProdukAction}, auto-create dari Pembelian/Tabungan Siswa, dst)
	 * -- satu tempat ini menjamin kolom SELALU sinkron tanpa perlu tiap pemanggil mengingat
	 * menghitungnya sendiri. {@code toko} SENGAJA dibaca langsung dari field (bukan
	 * {@code getToko()}, yang bisa memicu lazy-load proxy tak perlu di sini) -- id-nya cukup
	 * diambil kalau objeknya sudah ada di memori.
	 */
	@javax.persistence.PrePersist
	@javax.persistence.PreUpdate
	protected void hitungKunciUnik() {
		this.kunciUnik = ais.common.ProdukKunciUnikUtil.hitung(kode, barcode, nama, toko == null ? null : toko.getId());
	}

	/**
	 * Waktu baris ini terakhir diubah. Diinisialisasi ke waktu instansiasi objek pada deklarasi
	 * field ini (bukan pada konstruktor), sehingga produk yang baru dibuat sudah punya cap waktu
	 * masuk akal sebelum {@code INSERT} pertama; sesudahnya diperbarui otomatis oleh
	 * {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setter {@link #getTanggal_dirubah()}. Normalnya TIDAK dipanggil kode aplikasi -- nilainya
	 * dikelola otomatis oleh {@link #onUpdate()}. Menyetelnya manual akan tertimpa hook tersebut
	 * pada UPDATE berikutnya.
	 *
	 * @param tanggal_dirubah cap waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu perubahan terakhir baris produk ini (presisi {@code TIMESTAMP}). Dipakai grid
	 * katalog dan sinkronisasi klien Desktop/Android untuk menentukan baris mana yang perlu ditarik
	 * ulang. Nama field/properti sengaja dipertahankan bergaya {@code snake_case}
	 * ({@code tanggal_dirubah}, bukan {@code tanggalDirubah}) karena nama kolomnya diturunkan
	 * implisit dari nama properti -- mengganti namanya menjadi camelCase akan mengubah nama kolom
	 * dan memutus pemetaan pada basis data yang sudah berjalan.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         konstruktor kelas ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris produk dalam format {@code "<id>-<kode>-<nama>"}, dengan tiap bagian
	 * yang {@code null} diganti string kosong sehingga hasilnya tidak pernah memuat literal
	 * {@code "null"} dan method ini tidak pernah melempar {@code NullPointerException}.
	 *
	 * <p>Perhatikan ketiga bagian dibaca LANGSUNG dari field, bukan lewat getter-nya. Untuk
	 * {@code nama} ini berarti hasil {@code toString()} bisa berbeda tipis dari
	 * {@link #getNama()}: getter tersebut melakukan {@code trim()} dan menormalkan {@code null}
	 * menjadi string kosong, sedangkan di sini nilai mentah dipakai apa adanya. Pembacaan langsung
	 * disengaja -- {@code toString()} kerap dipanggil pada objek proxy/detached (mis. saat logging
	 * atau debugging), dan melewati getter berisiko memicu resolusi lazy atau efek samping
	 * normalisasi di tempat yang tidak seharusnya.</p>
	 *
	 * @return ringkasan identitas produk untuk log, combobox, dan pesan kesalahan
	 */
	public String toString() {
		return (id == null ? "" : id) + "-" + (kode == null ? "" : kode) + "-" + (nama == null ? "" : nama);
	}

	/** Kode internal toko; lihat {@link #getKode()}. */
	private String kode;

	/** Barcode/UPC fisik kemasan; lihat {@link #getBarcode()}. */
	private String barcode;

	/** Nama produk; lihat {@link #getNama()}. */
	private String nama;

	/** Kunci unik ternormalisasi, dihitung otomatis; lihat {@link #hitungKunciUnik()}. */
	private String kunciUnik;

	/** Catatan bebas; lihat {@link #getCatatan()}. */
	private String catatan;

	/** Keterangan singkat (dipotong 253 karakter oleh getter); lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Kategori produk; lihat {@link #getJenisProduk()}. */
	private JenisProduk jenisProduk;

	/** Toko pemilik -- SATU-SATUNYA sumbu tenant kelas ini; lihat {@link #getToko()}. */
	private Toko toko;

	/** Harga beli/HPP dasar; lihat {@link #getHargaBeli()}. */
	private Double hargaBeli;

	/** Harga jual satuan dasar; lihat {@link #getHargaJual()}. */
	private Double hargaJual;

	/** URL gambar produk; lihat {@link #getImageUrl()}. */
	private String imageUrl;

	/** Path gambar produk di penyimpanan server; lihat {@link #getImagePath()}. */
	private String imagePath;

	/** Penanda aktif (soft delete); lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Penanda keberadaan berkas gambar; lihat {@link #getAdaFileGambar()}. */
	private Boolean adaFileGambar;

	/** Saldo stok AGREGAT -- terpisah dari saldo per lot {@link ProdukBatch}; lihat {@link #getStok()}. */
	private Double stok;

	/** Override per-produk atas gerbang oversell toko; lihat {@link #getIzinkanJualMinusStok()}. */
	private Boolean izinkanJualMinusStok;

	/** Resep/BOM dalam JSON; lihat {@link #getBahanBaku()}. */
	private String bahanBaku;

	/** Tautan opsional ke persediaan modul Aset; lihat {@link #getMasterAsset()}. */
	private MasterAsset masterAsset;

	/** Pemasok utama opsional; lihat {@link #getPemasok()}. */
	private PemasokProduk pemasok;

	/** Satuan dasar pencatatan stok; lihat {@link #getSatuan()}. */
	private SatuanProduk satuan;

	/** UOM bawaan pembelian/PO; lihat {@link #getSatuanPembelian()}. */
	private SatuanProduk satuanPembelian;

	/** Kebijakan retur ({@code @NotAudited}); lihat {@link #getKebijakanRetur()}. */
	private KebijakanRetur kebijakanRetur;

	/** Grup harga terpusat lintas toko; lihat {@link #getGrupProduk()}. */
	private GrupProduk grupProduk;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk menginstansiasi entity saat
	 * memuat baris dari basis data, dan dipakai jalur aplikasi untuk membuat produk baru sebelum
	 * field-nya diisi. Tidak menyetel nilai apa pun kecuali inisialisasi field
	 * {@link #tanggal_dirubah} yang berjalan otomatis pada deklarasinya. Semua getter berpenjaga
	 * pada kelas ini ({@link #getStok()}, {@link #getAktif()}, {@link #getJenisItem()}, dst.)
	 * memastikan objek hasil konstruktor ini sudah aman dibaca meski seluruh kolomnya masih
	 * {@code null}.
	 */
	public Produk() {
	}

	/**
	 * Grup harga terpusat lintas toko (opsional, {@code null} = harga dikelola per toko seperti
	 * sebelumnya -- SEMUA baris lama otomatis null, perilaku tidak berubah). Bila diisi, HPP dan
	 * harga jual produk ini akan DITIMPA setiap kali grupnya disimpan (lihat javadoc
	 * {@link GrupProduk}).
	 * <p><b>Kolom BARU pada entitas ber-{@code @Audited}</b>: {@code hbm2ddl.auto=update} menambah
	 * kolomnya ke tabel utama {@code koperasi.produk} otomatis, tetapi TIDAK ke tabel audit Envers
	 * -- WAJIB jalankan {@code webapp/sql/migrasi_grup_produk_audit.sql} SEBELUM deploy (kalau
	 * tidak, UPDATE apa pun pada produk gagal menulis baris auditnya). Pola gotcha sama dgn
	 * kolom profil {@link Toko} (lihat javadoc di sana).</p>
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "grup_produk", nullable = true)
	public GrupProduk getGrupProduk() {
		return grupProduk;
	}

	/**
	 * Setter {@link #getGrupProduk()}. Menyetel grup di sini TIDAK langsung mengubah
	 * {@link #getHargaBeli()}/{@link #getHargaJual()} produk ini -- penimpaan harga baru terjadi
	 * saat grup yang bersangkutan disimpan dari layar Grup Produk. Menyetel {@code null}
	 * mengembalikan produk ke pengelolaan harga per toko.
	 *
	 * @param grupProduk grup harga terpusat, boleh {@code null}
	 */
	public void setGrupProduk(GrupProduk grupProduk) {
		this.grupProduk = grupProduk;
	}

	/**
	 * Konstruktor pintasan yang hanya menyetel {@link #getId()} -- dipakai untuk membentuk
	 * REFERENSI ringan ke produk yang sudah ada tanpa memuat barisnya dari basis data (mis. sebagai
	 * nilai relasi pada entity lain, atau parameter kriteria query). Objek hasil konstruktor ini
	 * BUKAN entity terkelola dan seluruh field lainnya {@code null}: jangan membacanya seolah
	 * berisi data produk sebenarnya, dan jangan menyimpannya lewat {@code saveOrUpdate} karena
	 * dapat menimpa baris nyata dengan kolom-kolom kosong.
	 *
	 * @param id kunci primer produk yang dirujuk
	 */
	public Produk(Long id) {
		this.id = id;
	}

	/**
	 * Kunci primer produk (strategi {@code IDENTITY} -- nilainya dibangkitkan basis data, bukan
	 * aplikasi). {@code null} selama objek belum pernah disimpan; setelah {@code INSERT} pertama
	 * Hibernate mengisinya kembali ke objek yang sama.
	 *
	 * <p>{@code insertable = false} pada {@code @Column} disengaja dan penting: kolom {@code id}
	 * sengaja TIDAK ikut disertakan pada pernyataan {@code INSERT} sehingga basis data selalu yang
	 * menentukan nilainya lewat sequence/serial-nya sendiri. Akibatnya id yang disetel manual lewat
	 * {@link #setId(Long)} pada objek BARU akan diabaikan saat penyimpanan pertama -- itu bukan
	 * cara membuat produk dengan id tertentu.</p>
	 *
	 * @return kunci primer, atau {@code null} bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter {@link #getId()} -- normalnya hanya dipanggil Hibernate saat memuat baris, atau oleh
	 * kode yang sengaja membangun referensi ringan seperti {@link #Produk(Long)}. Lihat catatan
	 * {@code insertable = false} pada javadoc getter: menyetel id pada objek baru tidak membuat
	 * baris tersimpan dengan id tersebut.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode internal produk menurut penomoran toko (SKU) -- berbeda dari {@link #getBarcode()} yang
	 * merupakan UPC/barcode fisik dari pabrik. Diisi lewat form Tambah/Ubah Produk keempat kanal
	 * (ZK/JSP/Desktop/Android) atau impor Excel katalog, dan ikut disaring secara OR bersama
	 * barcode dan nama pada pencarian/scan produk di Kasir.
	 *
	 * <p>Perhatikan {@code unique = false} yang EKSPLISIT: basis data TIDAK menjamin keunikan kode,
	 * bahkan dalam satu toko. Pencegahan duplikat katalog ditangani di tingkat aplikasi lewat
	 * {@link #getKunciUnik()} yang menormalkan kombinasi kode+barcode+nama+toko (lihat
	 * {@link #hitungKunciUnik()} dan {@link ais.common.ProdukKunciUnikUtil}) -- itulah gap-closure
	 * atas keluhan "Katalog Barang banyak yang double". Getter ini mengembalikan nilai APA ADANYA
	 * tanpa {@code trim()} maupun normalisasi huruf besar/kecil; jangan membandingkan kode dengan
	 * {@code equals} lurus untuk menentukan duplikat, pakai kunci unik.</p>
	 *
	 * @return kode internal produk, atau {@code null} bila belum diisi
	 */
	@Column(unique = false)
	public String getKode() {
		return kode;
	}

	/**
	 * Setter {@link #getKode()} -- menyimpan nilai apa adanya tanpa normalisasi maupun pemeriksaan
	 * duplikat. Karena kode ikut membentuk {@link #getKunciUnik()}, mengubahnya akan mengubah kunci
	 * unik baris ini otomatis pada penyimpanan berikutnya.
	 *
	 * @param kode kode internal produk
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * UPC/Barcode fisik pada kemasan (opsional, TERPISAH dari {@link #getKode()} yang merupakan
	 * kode internal toko) -- diisi lewat impor Excel katalog (kolom "UPC/Barcode") ATAU form
	 * Tambah/Ubah Produk (JSP/ZK/Desktop/Android), lihat JavaDoc {@code KantinHelper.produkSimpan}.
	 * Ikut disaring (OR) pada pencarian/scan produk bersama {@link #getKode()} dan nama -- lihat
	 * {@code PriceTagUtil.listProduk} (katalog POS Desktop/Android + Cetak Price Tag) dan
	 * {@code StokOpnameScanUtil.cariProdukByBarcode} (scan Stok Opname).
	 */
	@Column(name = "barcode", nullable = true, length = 100)
	public String getBarcode() {
		return barcode;
	}

	/**
	 * Setter {@link #getBarcode()} -- nilai disimpan apa adanya, tanpa validasi format UPC/EAN
	 * maupun pemeriksaan bentrok dengan produk lain. Karena barcode ikut membentuk
	 * {@link #getKunciUnik()}, mengubahnya otomatis mengubah kunci unik baris pada penyimpanan
	 * berikutnya.
	 *
	 * @param barcode UPC/barcode fisik kemasan, boleh {@code null}
	 */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/**
	 * Kunci unik ternormalisasi -- setter WAJIB ada supaya Hibernate bisa memuat nilai kolom ini
	 * saat SELECT (strategi akses PROPERTY dipakai di seluruh entity ini), TAPI kode aplikasi
	 * TIDAK PERNAH boleh memanggilnya manual -- nilainya SELALU dihitung ulang otomatis oleh
	 * {@link #hitungKunciUnik()} tiap kali baris disimpan, apa pun yang di-set lewat sini akan
	 * TERTIMPA sebelum benar-benar tersimpan. Lihat JavaDoc lengkap di sana dan
	 * {@link ais.common.ProdukKunciUnikUtil}.
	 */
	@Column(name = "kunci_unik", length = 600, nullable = true)
	public String getKunciUnik() {
		return kunciUnik;
	}

	/**
	 * Setter {@link #getKunciUnik()} -- WAJIB ADA agar Hibernate dapat memuat nilai kolom saat
	 * SELECT (kelas ini memakai strategi akses PROPERTY), TAPI kode aplikasi TIDAK PERNAH boleh
	 * memanggilnya. Apa pun yang disetel di sini akan TERTIMPA oleh {@link #hitungKunciUnik()}
	 * sebelum baris benar-benar tersimpan.
	 *
	 * @param kunciUnik nilai dari basis data; jangan disetel manual
	 */
	public void setKunciUnik(String kunciUnik) {
		this.kunciUnik = kunciUnik;
	}

	/**
	 * Nama produk sebagaimana ditampilkan di katalog, struk, dan hasil pencarian Kasir. Kolomnya
	 * {@code nullable = false} dan bertipe {@code text} (tanpa batas panjang praktis).
	 *
	 * <p><b>Getter ini DESTRUKTIF terhadap state objek</b>, bukan pembaca murni: bila field
	 * {@code nama} bernilai {@code null}, getter MENULISNYA menjadi string kosong sebelum
	 * mengembalikan nilai. Efek sampingnya nyata pada entity terkelola -- sekadar MEMBACA nama
	 * sebuah produk yang kolomnya {@code NULL} akan menandai entity itu kotor (dirty), sehingga
	 * Hibernate menerbitkan {@code UPDATE} saat flush dan, karena kelas ini {@code @Audited}, ikut
	 * menulis satu revisi Envers baru yang sebetulnya bukan perubahan yang diniatkan pengguna.
	 * Normalisasi {@code null} &rarr; {@code ""} ini juga yang membuat cabang
	 * {@code this.nama == null ? null : ...} pada baris {@code return} menjadi mati secara logika:
	 * setelah blok di atasnya, {@code nama} dipastikan tidak {@code null}, sehingga method ini pada
	 * praktiknya tidak pernah mengembalikan {@code null}. Pemanggil yang hanya ingin mengintip
	 * nilai mentah tanpa menyentuh state sebaiknya membaca lewat {@link #toString()} yang memang
	 * mengakses field secara langsung. Pola getter destruktif yang sama muncul pada
	 * {@link #getKeterangan()} dan {@link #getJenisProduk()}.</p>
	 *
	 * <p>Nilai kembalian selalu di-{@code trim()}; spasi di ujung yang tersimpan di basis data
	 * tidak akan terlihat pemanggil, tetapi TIDAK ikut ditulis balik ke field -- yang tersimpan
	 * tetap nilai asli beserta spasinya. Karena nama ikut membentuk {@link #getKunciUnik()},
	 * mengubahnya mengubah kunci unik baris pada penyimpanan berikutnya.</p>
	 *
	 * @return nama produk yang sudah di-{@code trim()}; praktis tidak pernah {@code null}
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		if (nama == null) {
			nama = "";
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Setter {@link #getNama()} -- menyimpan nilai apa adanya, tanpa {@code trim()} maupun
	 * pemeriksaan duplikat. Normalisasi baru terjadi saat dibaca kembali lewat getter dan saat
	 * kunci unik dihitung ulang oleh {@link #hitungKunciUnik()}.
	 *
	 * @param nama nama produk
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan singkat produk (satu baris, mis. untuk kolom tambahan pada grid katalog) --
	 * berbeda dari {@link #getCatatan()} yang bertipe {@code text} dan ditujukan untuk uraian
	 * panjang.
	 *
	 * <p><b>Getter ini DESTRUKTIF dan MEMOTONG data secara permanen.</b> Dua efek samping terjadi
	 * pada field sebelum nilainya dikembalikan: {@code null} ditulis menjadi string kosong, dan
	 * nilai yang panjangnya lebih dari 254 karakter DIPANGKAS menjadi 253 karakter
	 * ({@code substring(0, 253)}) lalu hasil pangkasan itu ditulis balik ke field. Karena yang
	 * dipangkas adalah field-nya sendiri (bukan sekadar nilai kembalian), pemangkasan tersebut ikut
	 * TERSIMPAN begitu entity di-flush: keterangan panjang akan kehilangan ekornya secara permanen
	 * hanya karena pernah dibaca, dan pada entity {@code @Audited} ini pembacaan tersebut juga
	 * menerbitkan {@code UPDATE} plus satu revisi Envers. Perhatikan pula ambang dan hasilnya tidak
	 * konsisten (memeriksa {@code > 254} tetapi memotong ke 253), serta kolomnya sendiri tidak
	 * dideklarasikan dengan {@code length} eksplisit sehingga batas 254 ini murni aturan aplikasi
	 * yang diwarisi dari lebar kolom historis, bukan batasan basis data.</p>
	 *
	 * @return keterangan produk, dipastikan tidak {@code null} dan maksimal 253 karakter
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		if (keterangan == null) {
			keterangan = "";
		}
		if (keterangan.length() > 254) {
			keterangan = keterangan.substring(0, 253);
		}
		return this.keterangan;
	}

	/**
	 * Setter {@link #getKeterangan()} -- menyimpan nilai apa adanya, TANPA pemangkasan. Pemangkasan
	 * ke 253 karakter baru terjadi saat getter dipanggil; lihat javadoc getter untuk konsekuensinya
	 * terhadap data yang tersimpan.
	 *
	 * @param keterangan keterangan singkat produk
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kategori/jenis produk (lihat {@link JenisProduk}) -- dipakai untuk pengelompokan katalog,
	 * filter laporan, dan penentuan perlakuan khusus per kategori.
	 *
	 * <p>Getter melakukan dua hal di luar sekadar membaca field. Pertama,
	 * {@link ais.database.model.GeneralValueObject#check(Object)} meresolusi proxy lazy yang mungkin
	 * sudah <i>detached</i> dari session asalnya -- pola getter relasi standar di seluruh entity
	 * AIS. Kedua, dan ini yang perlu diwaspadai, bila hasilnya tetap {@code null} getter MENULIS
	 * field dengan konstanta bersama {@link ConstantValues#PRODUK_UMUM} sebagai default. Karena
	 * penulisan itu mengenai field entity terkelola, sekadar MEMBACA jenis produk pada baris yang
	 * kolomnya {@code NULL} akan menandai entity kotor dan menerbitkan {@code UPDATE} beserta satu
	 * revisi Envers -- produk yang semula sengaja tanpa kategori diam-diam menjadi berkategori
	 * "umum" secara permanen. Ini pola getter destruktif yang sama dengan {@link #getNama()} dan
	 * {@link #getKeterangan()}.</p>
	 *
	 * <p>Perhatikan juga bahwa yang ditulis adalah REFERENSI ke objek konstanta global yang
	 * dibagikan seluruh aplikasi, bukan salinan; jangan pernah memutasi objek yang dikembalikan
	 * getter ini. {@code cascade PERSIST/MERGE} berarti menyimpan produk ikut menyimpan/merge
	 * jenis produk yang tertaut bila belum tersimpan.</p>
	 *
	 * @return kategori produk; tidak pernah {@code null} (jatuh ke {@code PRODUK_UMUM})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_produk", nullable = true)
	public JenisProduk getJenisProduk() {
		jenisProduk = check(jenisProduk);
		if (jenisProduk == null) {
			jenisProduk = ConstantValues.PRODUK_UMUM;
		}
		return jenisProduk;
	}

	/**
	 * Setter {@link #getJenisProduk()}. Menyetel {@code null} TIDAK membuat produk permanen tanpa
	 * kategori -- pembacaan berikutnya lewat getter akan mengisinya kembali dengan
	 * {@link ConstantValues#PRODUK_UMUM}.
	 *
	 * @param jenisProduk kategori produk, boleh {@code null}
	 */
	public void setJenisProduk(JenisProduk jenisProduk) {
		this.jenisProduk = jenisProduk;
	}

	/**
	 * Catatan bebas berbentuk uraian panjang tentang produk ({@code columnDefinition = "text"},
	 * tanpa batas panjang praktis) -- berbeda dari {@link #getKeterangan()} yang singkat dan
	 * dipangkas 253 karakter. Berbeda pula dari getter destruktif lain di kelas ini, getter ini
	 * TIDAK memodifikasi state: blok {@code if (catatan == null) catatan = null;} di dalamnya
	 * adalah penugasan tak berefek (menyetel {@code null} menjadi {@code null}) -- sisa
	 * penyuntingan lama yang aman dibiarkan, dan yang membuat method ini pembaca murni. Nilai
	 * dikembalikan apa adanya, termasuk {@code null} bila belum pernah diisi.
	 *
	 * @return catatan produk, atau {@code null} bila kosong
	 */
	@Column(name = "catatan", columnDefinition = "text", nullable = true)
	public String getCatatan() {
		if (catatan == null) {
			catatan = null;
		}
		return catatan;
	}

	/**
	 * Setter {@link #getCatatan()} -- menyimpan nilai apa adanya, termasuk {@code null} untuk
	 * mengosongkan catatan.
	 *
	 * @param catatan uraian bebas tentang produk, boleh {@code null}
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Saldo stok AGREGAT produk ini -- satu angka per baris produk, TANPA rincian per lot dan
	 * TANPA rincian per toko selain lewat kolom {@link #getToko()} baris ini sendiri.
	 *
	 * <p><b>Hubungan dengan {@link ProdukBatch} -- sengaja TIDAK direkonsiliasi dua arah.</b>
	 * {@link ProdukBatch#getStok()} menyimpan saldo per lot fisik (kombinasi produk + toko + nomor
	 * batch + tanggal kedaluwarsa). Kedua angka itu berjalan sebagai catatan TERPISAH: tidak ada
	 * pemicu, trigger, atau job yang memaksa Σ(saldo batch aktif) sama dengan nilai di sini, dan
	 * mengubah salah satunya tidak menyesuaikan yang lain. Hal ini disengaja karena pelacakan batch
	 * bersifat OPSIONAL per produk -- baris {@code ProdukBatch} hanya tercipta bila payload
	 * penerimaan menyertakan nomor batch DAN tanggal expired sekaligus, sehingga mayoritas produk
	 * memang hanya memiliki stok agregat ini dan penjualannya murni bersandar padanya, persis
	 * seperti sebelum fitur batch ada. Saat mesin konsumsi FEFO kehabisan saldo lot, kekurangannya
	 * SENGAJA dibiarkan jatuh ke angka agregat ini alih-alih memaksa saldo batch menjadi negatif.
	 * Konsekuensi praktisnya: jangan menulis laporan atau validasi yang mengasumsikan kedua angka
	 * setara, dan jangan "memperbaiki" selisih di antaranya secara otomatis -- selisih itu bagian
	 * dari desain, bukan kerusakan data.</p>
	 *
	 * <p>Getter null-safe: {@code null} dibaca sebagai {@code 0.0}, konsisten dengan
	 * {@code default 0.0} pada definisi kolom, sehingga baris lama yang kolomnya belum terisi tidak
	 * menyebabkan {@code NullPointerException} pada aritmetika stok. Nilai bertipe {@code Double}
	 * (bukan bilangan bulat) karena satuan produk bisa pecahan -- lihat {@link SatuanProduk}.
	 * Angka ini dapat bernilai NEGATIF: penjualan melampaui stok diizinkan bila gerbang toko
	 * {@link Toko#getBolehTransaksiStokHabis()} atau override per-produk
	 * {@link #getIzinkanJualMinusStok()} mengizinkannya.</p>
	 *
	 * @return saldo stok agregat; {@code 0.0} bila kolom {@code null}, boleh negatif
	 */
	@Column(name = "stok", columnDefinition = "float8 default 0.0")
	public Double getStok() {
		return stok == null ? 0.0 : stok;
	}

	/**
	 * Setter {@link #getStok()} -- menulis saldo agregat LANGSUNG tanpa mencatat mutasi pendamping
	 * dan tanpa menyentuh saldo {@link ProdukBatch} mana pun. Jalur perubahan stok yang benar
	 * (penjualan, kulakan, opname, transfer antar-outlet, retur) berjalan lewat helper yang selalu
	 * mengiringi perubahan ini dengan baris {@link MutasiStokToko}; memanggil setter ini langsung
	 * akan membuat saldo dan buku besar pergerakan stok tidak sinkron.
	 *
	 * @param stok saldo stok agregat baru
	 */
	public void setStok(Double stok) {
		this.stok = stok;
	}

	/**
	 * Harga beli / harga pokok dasar per SATUAN DASAR produk ({@link #getSatuan()}, bukan satuan
	 * pembelian). Dipakai sebagai dasar HPP pada beberapa metode posting, sebagai harga snapshot
	 * saat produk ditambahkan sebagai bahan baku pada resep produk lain, dan sebagai nilai cadangan
	 * ketika metode HPP berbasis kulakan tidak menemukan riwayat pembelian.
	 *
	 * <p><b>Nilai ini dapat DITIMPA oleh dua jalur otomatis yang berjalan independen.</b> Pertama,
	 * faktur kulakan/BAST tervalidasi memperbarui harga beli mengikuti biaya perolehan terakhir,
	 * sudah dikonversi ke satuan dasar lewat rasio UOM pembelian (contoh: Rp 1.200.000 per DUS isi
	 * 6 menjadi Rp 200.000 per botol) -- penimpaan ini dapat dimatikan per produk dengan menyetel
	 * {@link #getHargaBeliManual()} menjadi {@code true}. Kedua, bila {@link #getGrupProduk()}
	 * terisi, penyimpanan grup harga terpusat menimpa harga beli seluruh anggotanya lintas toko;
	 * jalur kedua ini TIDAK memeriksa flag harga beli manual. Karena itu produk yang harganya
	 * "dikunci manual" tetap bisa berubah bila ia juga anggota sebuah grup harga.</p>
	 *
	 * <p>Getter null-safe: {@code null} dibaca sebagai {@code 0.0} sehingga perhitungan margin dan
	 * HPP tidak meledak untuk produk yang harga belinya belum pernah diisi. Konsekuensinya nilai
	 * "belum pernah diisi" tidak dapat dibedakan dari "memang nol" lewat getter ini.</p>
	 *
	 * @return harga beli per satuan dasar; {@code 0.0} bila kolom {@code null}
	 */
	public Double getHargaBeli() {
		return hargaBeli == null ? 0.0 : hargaBeli;
	}

	/**
	 * Setter {@link #getHargaBeli()} -- menyimpan nilai apa adanya tanpa memeriksa hak akses
	 * pengguna. Kebijakan siapa yang boleh mengubah harga ditegakkan di lapisan pemanggil
	 * berdasarkan {@link Toko#getSemuaBolehUbahHarga()}, {@link Toko#getUserBolehUbahHarga()}, dan
	 * {@link Toko#getRoleBolehUbahHarga()}; entity ini tidak menegakkan apa pun.
	 *
	 * @param hargaBeli harga beli per satuan dasar
	 */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * Harga jual per SATUAN DASAR produk -- harga yang dipakai Kasir saat pembeli membeli dalam
	 * satuan terkecil. Penjualan per pack memakai {@link #getHargaPack()} yang merupakan harga
	 * TETAP per pack dan sengaja bukan hasil perkalian angka ini dengan isi pack.
	 *
	 * <p>Sama seperti {@link #getHargaBeli()}, nilai ini dapat DITIMPA massal oleh penyimpanan
	 * {@link GrupProduk} bila produk ini anggota sebuah grup harga terpusat. Harga jual produk yang
	 * dipakai sebagai "ekstra"/add-on juga dibaca live dari sini saat checkout -- lihat
	 * {@link #getEkstraPilihan()}, yang sengaja hanya menyimpan id dan tidak membekukan harga.</p>
	 *
	 * <p>Getter null-safe: {@code null} dibaca sebagai {@code 0.0}. Perlu disadari akibatnya di
	 * jalur penjualan: produk yang harga jualnya belum pernah diisi tidak akan ditolak, melainkan
	 * terjual seharga nol tanpa peringatan dari entity ini. Validasi kewajaran harga, bila
	 * diperlukan, harus dilakukan di lapisan form/helper.</p>
	 *
	 * @return harga jual per satuan dasar; {@code 0.0} bila kolom {@code null}
	 */
	public Double getHargaJual() {
		return hargaJual == null ? 0.0 : hargaJual;
	}

	/**
	 * Setter {@link #getHargaJual()} -- menyimpan nilai apa adanya tanpa memeriksa hak akses ubah
	 * harga maupun kewajaran nilai (nol dan negatif diterima). Lihat catatan penegakan kebijakan
	 * pada {@link #setHargaBeli(Double)}.
	 *
	 * @param hargaJual harga jual per satuan dasar
	 */
	public void setHargaJual(Double hargaJual) {
		this.hargaJual = hargaJual;
	}

	/**
	 * Penanda produk aktif dalam katalog -- mekanisme SOFT DELETE: produk yang "dihapus" dari layar
	 * admin sebenarnya hanya disetel {@code false} sehingga transaksi historis yang menunjuk
	 * kepadanya tetap utuh dan laporan lama tidak kehilangan nama barang.
	 *
	 * <p>Getter null-safe dengan default {@code true} (FAIL-OPEN): baris lama yang kolomnya
	 * {@code NULL} -- termasuk seluruh baris yang dibuat sebelum kolom ini ada -- diperlakukan
	 * sebagai AKTIF. Pilihan ini menjaga kompatibilitas mundur (katalog lama tidak mendadak
	 * kosong), tetapi punya konsekuensi yang harus diingat saat menulis query: filter aktif TIDAK
	 * boleh ditulis sebagai {@code eq("aktif", true)} polos, karena di Postgres pembandingan
	 * dengan {@code NULL} tidak pernah menghasilkan benar sehingga baris lama akan hilang diam-diam
	 * dari hasil. Pola yang benar adalah {@code OR IS NULL}, persis seperti yang dipakai layar Grup
	 * Produk. Ini pola yang sama dengan {@link #getJenisItem()} dan flag aktif pada master lain di
	 * paket ini.</p>
	 *
	 * @return {@code true} bila produk aktif; {@code true} pula untuk baris yang kolomnya {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Setter {@link #getAktif()}. Menyetel {@code false} adalah cara resmi "menghapus" produk dari
	 * katalog. Menyetel {@code null} secara efektif sama dengan {@code true} karena default
	 * fail-open pada getter.
	 *
	 * @param aktif status aktif produk
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Override PER-PRODUK atas gerbang toko {@code Konfigurasi.KANTIN_POS_CEGAH_OVERSELL} (lihat
	 * {@code KantinHelper.validasiStokCukupDenganLock}) -- {@code null} (default) berarti "ikut
	 * pengaturan toko" (fail-open, boleh minus, sama seperti sebelum field ini ada). {@code false}
	 * berarti produk ini WAJIB diblokir dari penjualan begitu stoknya tidak cukup, TERLEPAS dari
	 * gerbang toko (mis. barang mahal/gampang basi yang admin sengaja tak mau dijual minus).
	 * {@code true} berarti produk ini SELALU boleh dijual minus walau gerbang toko AKTIF.
	 */
	@Column(name = "izinkan_jual_minus_stok")
	public Boolean getIzinkanJualMinusStok() {
		return izinkanJualMinusStok;
	}

	/**
	 * Setter {@link #getIzinkanJualMinusStok()}. Perhatikan ketiga keadaannya bermakna berbeda dan
	 * {@code null} BUKAN sekadar "belum diisi" melainkan "ikut pengaturan toko" -- jangan
	 * menormalkannya menjadi {@code false} pada jalur simpan, karena itu akan mengubah produk dari
	 * "mengikuti kebijakan toko" menjadi "wajib diblokir saat stok kurang".
	 *
	 * @param izinkanJualMinusStok {@code null} ikut toko, {@code true} selalu boleh minus,
	 *        {@code false} selalu diblokir
	 */
	public void setIzinkanJualMinusStok(Boolean izinkanJualMinusStok) {
		this.izinkanJualMinusStok = izinkanJualMinusStok;
	}

	/**
	 * Toko/outlet pemilik baris produk ini -- SATU-SATUNYA sumbu tenant kelas ini dan, karena tidak
	 * ada entity lain di paket {@code inventory} yang menyimpan induk organisasi, sumbu tenant
	 * efektif seluruh modul retail/kantin/koperasi.
	 *
	 * <p><b>Nullable secara sengaja.</b> {@code nullable = true} berarti sebuah produk boleh
	 * berdiri TANPA toko sama sekali. Baris seperti itu adalah produk "global"/warisan yang tidak
	 * dimiliki outlet mana pun. Konsekuensinya untuk penulisan query: filter lingkup berbentuk
	 * {@code eq("toko", tokoAktif)} tidak akan pernah mencocokkan baris ber-{@code toko} NULL,
	 * sehingga produk global otomatis hilang dari layar yang memfilter ketat; sebaliknya layar yang
	 * sengaja menyertakan produk global harus menulis {@code OR toko IS NULL} secara eksplisit dan
	 * dengan itu menerima bahwa baris tersebut terlihat oleh semua toko. Tidak ada satu pun default
	 * di tingkat entity yang menentukan pilihan itu -- setiap pemanggil memutuskan sendiri.</p>
	 *
	 * <p><b>Penegakan lingkup ada di pemanggil, bukan di sini.</b> Kelas ini tidak memiliki
	 * mekanisme apa pun yang memaksa produk hanya terbaca oleh tokonya. Pada layar ZK
	 * {@code ProdukAction}, pembatasan toko diterapkan dengan menyetel {@code setDisabled(...)}
	 * pada combobox toko berdasarkan {@link Toko#getBolehMelihatTokolain()} -- yaitu di tingkat
	 * TAMPILAN. Ini konsisten dengan pola filter tenant lemah yang sudah tercatat di domain lain
	 * dan bukan temuan baru; disebutkan di sini semata agar pengembang berikutnya tidak keliru
	 * menyimpulkan bahwa entity sudah aman dengan sendirinya. Lihat pula catatan Generic CRUD v2
	 * pada javadoc kelas: properti {@code toko} tidak termasuk whitelist {@code scopeBindings()},
	 * sehingga scoping otomatis kerangka kerja generik tidak akan pernah memfilter berdasarkan
	 * kolom ini.</p>
	 *
	 * <p>Getter memanggil {@link ais.database.model.GeneralValueObject#check(Object)} untuk
	 * meresolusi proxy lazy yang mungkin sudah <i>detached</i>; berbeda dari
	 * {@link #getJenisProduk()}, tidak ada pengisian nilai default sehingga getter ini TIDAK
	 * destruktif dan boleh dipanggil bebas. {@code cascade PERSIST/MERGE}: menyimpan produk ikut
	 * menyimpan/merge toko yang tertaut bila belum tersimpan.</p>
	 *
	 * @return toko pemilik, atau {@code null} untuk produk global/warisan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Setter {@link #getToko()} -- memindahkan kepemilikan produk ke toko lain TANPA pemeriksaan
	 * hak akses apa pun, dan tanpa memindahkan data turunannya. Perlu disadari saldo
	 * {@link ProdukBatch} terikat pada pasangan produk+toko masing-masing dan TIDAK ikut berpindah,
	 * demikian pula riwayat {@link MutasiStokToko}; mengubah field ini pada produk yang sudah
	 * bertransaksi akan membuat data historisnya menunjuk toko yang berbeda dari kepemilikan
	 * sekarang. Karena toko ikut membentuk {@link #getKunciUnik()}, perubahan di sini juga mengubah
	 * kunci unik baris pada penyimpanan berikutnya.
	 *
	 * @param toko toko pemilik, boleh {@code null} untuk produk global
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * URL gambar produk yang dapat diakses klien (katalog Kasir Desktop/Android, layar pelanggan,
	 * cetak price tag). Berpasangan dengan {@link #getImagePath()} yang menyimpan lokasi berkasnya
	 * di server. Bertipe {@code text} tanpa batas panjang praktis, dan tanpa validasi format URL --
	 * nilai apa pun tersimpan apa adanya.
	 *
	 * <p>Perhatikan nama kolomnya ditulis {@code "Image_url"} dengan huruf kapital di awal, berbeda
	 * dari konvensi {@code snake_case} huruf kecil kolom lain di kelas ini. Penamaan itu diwarisi
	 * dari skema lama dan sengaja dipertahankan; jangan "dirapikan" tanpa migrasi, karena pada
	 * basis data yang membedakan huruf besar/kecil untuk pengenal terkutip, perubahan ini memutus
	 * pemetaan kolom.</p>
	 *
	 * @return URL gambar produk, atau {@code null} bila produk tidak bergambar
	 */
	@Column(name = "Image_url", columnDefinition = "text")
	public String getImageUrl() {
		return imageUrl;
	}

	/**
	 * Setter {@link #getImageUrl()} -- menyimpan nilai apa adanya, tanpa validasi format dan tanpa
	 * menyesuaikan {@link #getAdaFileGambar()} secara otomatis. Ketiga field gambar
	 * ({@code imageUrl}, {@code imagePath}, {@code adaFileGambar}) harus dijaga konsisten oleh
	 * pemanggil.
	 *
	 * @param imageUrl URL gambar produk, boleh {@code null}
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	/**
	 * Penanda bahwa produk ini benar-benar memiliki berkas gambar tersimpan -- dipakai layar
	 * katalog untuk memutuskan menampilkan gambar atau placeholder TANPA perlu memeriksa keberadaan
	 * berkas di cakram atau menembak URL-nya lebih dulu, sehingga grid berisi ratusan produk tidak
	 * menimbulkan ratusan operasi berkas atau permintaan gagal.
	 *
	 * <p>Nilainya adalah penanda TURUNAN yang harus dijaga konsisten oleh pemanggil terhadap
	 * {@link #getImageUrl()}/{@link #getImagePath()} -- tidak ada mekanisme di entity ini yang
	 * menyinkronkannya, sehingga penanda ini bisa berbohong bila berkas gambarnya dihapus dari
	 * cakram tanpa memperbarui baris produk. Getter null-safe dengan default {@code false}
	 * (FAIL-CLOSED, kebalikan dari {@link #getAktif()}): baris lama yang kolomnya {@code NULL}
	 * dianggap tidak bergambar, yang merupakan pilihan aman karena kegagalannya hanya berupa
	 * placeholder yang tampil, bukan tautan rusak.</p>
	 *
	 * @return {@code true} bila produk dinyatakan memiliki berkas gambar; {@code false} untuk kolom
	 *         {@code null}
	 */
	public Boolean getAdaFileGambar() {
		return adaFileGambar == null ? false : adaFileGambar;
	}

	/**
	 * Setter {@link #getAdaFileGambar()} -- wajib dipanggil pemanggil yang mengunggah atau menghapus
	 * berkas gambar produk, karena penanda ini tidak dihitung otomatis dari
	 * {@link #getImageUrl()}/{@link #getImagePath()}.
	 *
	 * @param adaFileGambar penanda keberadaan berkas gambar
	 */
	public void setAdaFileGambar(Boolean adaFileGambar) {
		this.adaFileGambar = adaFileGambar;
	}

	/**
	 * Lokasi berkas gambar produk pada penyimpanan server -- pendamping {@link #getImageUrl()} yang
	 * menyimpan alamat aksesnya untuk klien. Dipisahkan menjadi dua kolom karena lokasi fisik
	 * berkas dan URL publiknya tidak selalu dapat diturunkan satu dari yang lain (berbeda root
	 * konteks, penyajian lewat servlet, atau pemindahan direktori penyimpanan antar-pemasangan).
	 *
	 * <p>Seperti {@link #getImageUrl()}, nama kolomnya ditulis {@code "Image_path"} dengan huruf
	 * kapital di awal mengikuti skema lama dan sengaja dipertahankan. Nilai disimpan apa adanya
	 * tanpa normalisasi pemisah direktori maupun validasi bahwa berkasnya benar-benar ada.</p>
	 *
	 * @return path berkas gambar di server, atau {@code null} bila produk tidak bergambar
	 */
	@Column(name = "Image_path", columnDefinition = "text")
	public String getImagePath() {
		return imagePath;
	}

	/**
	 * Setter {@link #getImagePath()} -- menyimpan nilai apa adanya, tanpa memvalidasi keberadaan
	 * berkas dan tanpa menyesuaikan {@link #getAdaFileGambar()} maupun {@link #getImageUrl()}.
	 *
	 * @param imagePath path berkas gambar di server, boleh {@code null}
	 */
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	/**
	 * Resep / bahan baku (Bill of Materials) produk ini dalam format JSON (string text), berisi
	 * daftar bahan baku yang berasal dari produk lain. HPP (Harga Pokok Penjualan) dapat dihitung
	 * dari total bahan baku ini. Format: {@code [{"produk":<id>,"nama":"...","qty":<angka>,"harga":<angka>}]}
	 * dengan {@code harga} = harga beli/satuan bahan baku saat ditambahkan; HPP = Σ(qty × harga).
	 */
	@Column(name = "bahanbaku", columnDefinition = "text", nullable = true)
	public String getBahanBaku() {
		return bahanBaku;
	}

	public void setBahanBaku(String bahanBaku) {
		this.bahanBaku = bahanBaku;
	}

	private Double stokMinimum;
	private java.util.Date tanggalExpired;
	private String batch;

	/** Batas minimum stok untuk peringatan reorder/pembelian ulang (opsional; 0 = tidak dipantau). */
	@Column(name = "stok_minimum")
	public Double getStokMinimum() {
		return stokMinimum == null ? 0.0 : stokMinimum;
	}

	public void setStokMinimum(Double stokMinimum) {
		this.stokMinimum = stokMinimum;
	}

	/** Tanggal kedaluwarsa produk (opsional; MVP per-produk untuk laporan expired). */
	@javax.persistence.Temporal(javax.persistence.TemporalType.DATE)
	@Column(name = "tanggal_expired")
	public java.util.Date getTanggalExpired() {
		return tanggalExpired;
	}

	public void setTanggalExpired(java.util.Date tanggalExpired) {
		this.tanggalExpired = tanggalExpired;
	}

	/** Nomor batch/lot (opsional). */
	@Column(name = "batch", length = 100)
	public String getBatch() {
		return batch;
	}

	public void setBatch(String batch) {
		this.batch = batch;
	}

	/**
	 * (Opsional) Tautan ke barang persediaan di modul Aset/Logistik ({@code asset.master_asset},
	 * umumnya {@code tipe = "Barang habis pakai"}). Dipakai untuk menelusuri asal barang (mis. dari
	 * BAST/penerimaan pengadaan) dan merekonsiliasi stok kantin vs stok persediaan aset. Nullable —
	 * produk yang tidak bersumber dari pengadaan terpusat boleh dibiarkan kosong (mode hibrida).
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "master_asset", nullable = true)
	public MasterAsset getMasterAsset() {
		masterAsset = check(masterAsset);
		return masterAsset;
	}

	public void setMasterAsset(MasterAsset masterAsset) {
		this.masterAsset = masterAsset;
	}

	/**
	 * Pemasok utama (opsional) -- diisi lewat form Katalog Barang Kasir Desktop/Android atau impor
	 * Excel katalog (kolom "Nama Pemasok Utama"), lihat JavaDoc {@link PemasokProduk}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pemasok", nullable = true)
	public PemasokProduk getPemasok() {
		return pemasok;
	}

	public void setPemasok(PemasokProduk pemasok) {
		this.pemasok = pemasok;
	}

	/**
	 * Satuan/unit-of-measure (opsional) -- diisi lewat form Katalog Barang Kasir Desktop/Android atau
	 * impor Excel katalog (kolom "Satuan"), lihat JavaDoc {@link SatuanProduk}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan", nullable = true)
	public SatuanProduk getSatuan() {
		return satuan;
	}

	public void setSatuan(SatuanProduk satuan) {
		this.satuan = satuan;
	}

	/** UOM bawaan PO. Harus satu kategori dengan {@link #getSatuan()}; stok
	 * tetap dicatat dalam satuan dasar dan qty pembelian dikonversi lewat rasio UOM. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_pembelian", nullable = true)
	public SatuanProduk getSatuanPembelian() {
		satuanPembelian = check(satuanPembelian);
		return satuanPembelian;
	}

	public void setSatuanPembelian(SatuanProduk satuanPembelian) {
		this.satuanPembelian = satuanPembelian;
	}

	/** Rute pemenuhan {@link #getRute()}: beli ke vendor/gudang induk (perilaku lama). */
	public static final String RUTE_BELI = "BELI";
	/** Rute pemenuhan {@link #getRute()}: produksi sendiri (ambang stok memicu draf WO). */
	public static final String RUTE_PRODUKSI = "PRODUKSI";
	/** Rute MTO (Fase E): konfirmasi SalesOrderLapangan memicu pengajuan pembelian. */
	public static final String RUTE_MTO_BELI = "MTO_BELI";
	/** Rute MTO (Fase E): konfirmasi SalesOrderLapangan memicu draf Work Order. */
	public static final String RUTE_MTO_PRODUKSI = "MTO_PRODUKSI";

	private String rute;

	/**
	 * Rute pemenuhan kembali stok (Fase C dok. 48 P3): {@link #RUTE_BELI} atau
	 * {@link #RUTE_PRODUKSI}. {@code null}/kosong = BELI = perilaku hari ini -- katalog lama tidak
	 * berubah makna. Dibaca {@link ais.common.StokThresholdScheduler} untuk memilih keluaran
	 * otomatis: pengajuan pembelian (BELI) atau draf Work Order produksi (PRODUKSI).
	 */
	@Column(name = "rute", nullable = true, length = 20)
	public String getRute() {
		return rute;
	}

	public void setRute(String rute) {
		this.rute = rute;
	}

	private Boolean perluQc;

	/**
	 * QC hasil produksi (Fase E dok. 48 P6): {@code true} = tiap dokumen OUTPUT POSTED yang
	 * memuat produk ini otomatis menerbitkan dokumen Quality Alert dan MENGKARANTINA batch
	 * ber-lot yang sama ({@code ProdukBatch.STATUS_KARANTINA}) sampai didisposisi.
	 * {@code null}/false = perilaku lama, tanpa QC -- katalog lama tidak berubah makna.
	 */
	@Column(name = "perlu_qc", nullable = true)
	public Boolean getPerluQc() {
		return perluQc;
	}

	public void setPerluQc(Boolean perluQc) {
		this.perluQc = perluQc;
	}

	private Boolean hargaBeliManual;

	/**
	 * Kebijakan harga beli master (PDF "stok & uom" 30-08): {@code null}/false = harga beli
	 * OTOMATIS mengikuti faktur kulakan/BAST tervalidasi (per satuan DASAR hasil konversi UOM
	 * pembelian -- contoh: Rp 1.200.000/DUS isi 6 -> Rp 200.000/botol); {@code true} = harga
	 * beli dikunci MANUAL, faktur tidak menimpanya.
	 */
	@Column(name = "harga_beli_manual", nullable = true)
	public Boolean getHargaBeliManual() {
		return hargaBeliManual;
	}

	public void setHargaBeliManual(Boolean hargaBeliManual) {
		this.hargaBeliManual = hargaBeliManual;
	}

	private Boolean packAktif;
	private SatuanProduk satuanPack;
	private Double hargaPack;

	/**
	 * Pack/Combo (PDF 31-08): {@code true} = produk boleh dijual di POS per PACK -- kasir
	 * mendapat pilihan satuan dasar vs pack saat menjual. Stok TETAP turun per satuan dasar
	 * (mesin Fase B); baris penjualan menyimpan snapshot satuan pack utk akunting/struk.
	 */
	@Column(name = "pack_aktif", nullable = true)
	public Boolean getPackAktif() {
		return packAktif;
	}

	public void setPackAktif(Boolean packAktif) {
		this.packAktif = packAktif;
	}

	/** UOM Pack (mis. Dus isi 6) -- wajib sekategori dengan {@link #getSatuan()}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_pack", nullable = true)
	public SatuanProduk getSatuanPack() {
		satuanPack = check(satuanPack);
		return satuanPack;
	}

	public void setSatuanPack(SatuanProduk satuanPack) {
		this.satuanPack = satuanPack;
	}

	/**
	 * Harga jual TETAP per pack (mis. Rp 65.000/Dus -- sengaja BUKAN isi x harga satuan).
	 * Server menimpanya ke baris saat kasir memilih satuan pack; total per pack selalu
	 * persis nilai ini.
	 */
	@Column(name = "harga_pack", nullable = true)
	public Double getHargaPack() {
		return hargaPack;
	}

	public void setHargaPack(Double hargaPack) {
		this.hargaPack = hargaPack;
	}

	/** Kebijakan retur produk; data kosong dimaknai sebagai kebijakan baku tanpa retur. */
	@org.hibernate.envers.NotAudited
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kebijakan_retur", nullable = true)
	public KebijakanRetur getKebijakanRetur() {
		kebijakanRetur = check(kebijakanRetur);
		return kebijakanRetur;
	}

	public void setKebijakanRetur(KebijakanRetur kebijakanRetur) {
		this.kebijakanRetur = kebijakanRetur;
	}

	private String jenisItem;

	/**
	 * Diskriminator "Jenis Item" -- {@code "JUAL"} (default, produk jadi yang bisa dijual/dicari di
	 * Kasir) vs {@code "BAHAN"} (bahan baku, HANYA muncul di picker "Pilih Bahan Baku" pada
	 * editor Resep/HPP produk lain, TIDAK muncul di katalog/pencarian Kasir). Baris lama (dibuat
	 * sebelum field ini ada) tetap {@code NULL} di DB dan diperlakukan sebagai {@code "JUAL"} oleh
	 * getter ini -- TIDAK di-backfill otomatis, admin re-tag manual lewat form Tambah/Ubah Produk.
	 * Setiap query/filter yang meng-exclude {@code "BAHAN"} WAJIB pola {@code OR IS NULL} (bukan
	 * {@code <>}/{@code ne} polos, yang tidak match NULL di Postgres) supaya baris lama tidak
	 * hilang diam-diam dari Kasir. Reserved untuk masa depan: {@code "EKSTRA"} (Produk Ekstra/modifier).
	 */
	@Column(name = "jenis_item", length = 20)
	public String getJenisItem() {
		return (jenisItem == null || jenisItem.trim().isEmpty()) ? "JUAL" : jenisItem;
	}

	public void setJenisItem(String jenisItem) {
		this.jenisItem = jenisItem;
	}

	private String ekstraPilihan;

	/**
	 * Gap-closure "Produk Ekstra" (modifier/add-on) -- HANYA berlaku pada baris produk DASAR
	 * (jenisItem {@code "JUAL"}): JSON array berisi id-id produk lain yang boleh dipilih pembeli
	 * sebagai ekstra saat memesan produk ini (mis. {@code [601,602,603]}). Setiap id di dalamnya
	 * SEHARUSNYA menunjuk ke produk dengan {@code jenisItem = "EKSTRA"}, tapi tidak divalidasi
	 * server-side -- form admin yang menjaga ini (picker hanya menampilkan produk ber-jenisItem
	 * EKSTRA).
	 *
	 * <p><b>Beda dengan {@link #getBahanBaku()}</b>: {@code bahanBaku} menyimpan SNAPSHOT
	 * {@code {produk,nama,qty,harga}} (HPP harus beku sejak resep dibuat), sedangkan field ini
	 * HANYA menyimpan id mentah -- harga ekstra yang ditampilkan/dibayar pembeli SELALU diambil
	 * live dari {@code hargaJual} milik produk ekstra itu sendiri saat checkout, tidak pernah
	 * dibekukan di sini.</p>
	 */
	@Column(name = "ekstra_pilihan", columnDefinition = "text", nullable = true)
	public String getEkstraPilihan() {
		return ekstraPilihan;
	}

	public void setEkstraPilihan(String ekstraPilihan) {
		this.ekstraPilihan = ekstraPilihan;
	}

	private String kemasan;

	/**
	 * Preset kemasan spesifik produk, terpisah dari UOM akuntansi. Format JSON:
	 * {@code [{"nama":"Dus 24","barcode":"...","qtyDasar":24,"aktif":true}]}.
	 * Pemindaian barcode kemasan menambah qty dasar sesuai preset, tetapi transaksi
	 * dan persediaan tetap dibukukan dalam {@link #getSatuan()}.
	 */
	@Column(name = "kemasan", columnDefinition = "text", nullable = true)
	public String getKemasan() {
		return kemasan;
	}

	public void setKemasan(String kemasan) {
		this.kemasan = kemasan;
	}

	private String metodeHpp;

	/**
	 * Metode penentuan HARGA POKOK (biaya) per unit saat Posting HPP Penjualan Kantin (per produk).
	 * Nilai:
	 * <ul>
	 *   <li>{@code null}/kosong = <b>Default</b>: rata-rata biaya kulakan; bila produk belum pernah
	 *       kulakan → jatuh ke {@code hargaBeli} produk.</li>
	 *   <li>{@code "RATA_RATA_KULAKAN"} = rata-rata biaya kulakan (fallback {@code hargaBeli}).</li>
	 *   <li>{@code "KULAKAN_TERAKHIR"} = biaya kulakan terakhir per tanggal (fallback {@code hargaBeli}).</li>
	 *   <li>{@code "HARGA_BELI_PRODUK"} = memakai {@code hargaBeli} produk apa adanya (statis).</li>
	 * </ul>
	 * {@code @NotAudited}: field pengaturan, tak perlu histori audit (hindari sinkron tabel
	 * {@code new_audit.produk__audit}). {@code @Column} eksplisit agar nama kolom tidak "digabung"
	 * oleh implicit naming Hibernate deployment ini.
	 */
	@org.hibernate.envers.NotAudited
	@Column(name = "metode_hpp", length = 30)
	public String getMetodeHpp() {
		return metodeHpp == null || metodeHpp.trim().isEmpty() ? null : metodeHpp.trim();
	}

	public void setMetodeHpp(String metodeHpp) {
		this.metodeHpp = metodeHpp;
	}

}
