package ais.database.model.sirs;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas baris (detail) <b>Transaksi Retur</b> pada schema {@code sirs}
 * (tabel {@code transaksi_retur_detail}). Setiap baris mencatat satu item yang
 * dikembalikan pasien pada satu dokumen {@link TransaksiRetur}, beserta
 * kuantitas ({@link #getQty()}) dan nilai uangnya ({@link #getAmount()}).
 *
 * <h2>Dua bentuk barang yang diretur: item lepas atau racikan</h2>
 * <p>
 * Baris ini punya DUA relasi berbeda untuk menyatakan apa yang dikembalikan:
 * {@link #getItem()} untuk item medis lepas, dan {@link #getRacikan()} untuk
 * obat racikan. Keduanya OPSIONAL dan skema tidak menyatakan hubungan di antara
 * keduanya — tidak ada yang mencegah sebuah baris mengisi KEDUANYA sekaligus,
 * dan tidak ada pula yang mencegah baris mengosongkan keduanya. Baris yang
 * mengisi keduanya ambigu (barang mana yang sebenarnya kembali), sedangkan
 * baris yang mengosongkan keduanya menyatakan nilai uang yang dikembalikan
 * tanpa barang apa pun yang menyertainya. Aturan bahwa tepat satu di antara
 * keduanya harus terisi — bila memang itu maksudnya — harus ditegakkan lapisan
 * action.
 * </p>
 *
 * <h2>Barang dan uang pada baris yang sama</h2>
 * <p>
 * Berbeda dari baris-baris detail di klaster pengadaan yang menyimpan harga
 * SATUAN ({@code hargaBeli}, {@code hargaJual}, {@code harga}), baris ini
 * menyimpan {@link #getAmount()}, yaitu nilai yang dikembalikan. Bersama
 * {@link #getQty()}, kedua angka itu berdiri sendiri-sendiri: skema tidak
 * menghubungkan keduanya lewat perhitungan apa pun, sehingga nilai yang
 * dikembalikan tidak harus sebanding dengan kuantitas yang dikembalikan.
 * </p>
 *
 * <h2>Tautan ke baris transaksi — tersedia, tetapi bukan penjaga</h2>
 * <p>
 * {@link #getTransaksiDetail()} menautkan baris ini ke baris penjualan asalnya
 * di {@link TransaksiMedisDetail}, sehingga kuantitas dan nilai yang dulu
 * dijual dapat dijangkau. Sama seperti pada seluruh pasangan serupa di klaster
 * ini, tautan yang tersedia itu tidak dengan sendirinya menjadi penjaga:
 * entitas tidak membandingkan {@link #getQty()} dengan kuantitas yang dijual,
 * tidak membandingkan {@link #getAmount()} dengan nilai yang dibayar, dan tidak
 * ada constraint database yang melakukannya. Penjaga yang memadai harus
 * mengagregasi seluruh baris retur atas baris penjualan yang sama, karena satu
 * penjualan boleh diretur bertahap.
 * </p>
 * <p>
 * Tautan ini OPSIONAL, seperti halnya tautan tingkat header
 * {@link TransaksiRetur#getTransaksi()} yang juga opsional. Berbeda dari jalur
 * pembelian — di mana tautan headernya WAJIB sehingga setidaknya ada satu titik
 * yang dijamin — pada jalur retur penjualan ini KEDUA tingkat tautan boleh
 * kosong sekaligus. Sebuah dokumen retur penjualan karena itu dapat berdiri
 * sepenuhnya lepas dari penjualan mana pun, mengembalikan uang dan menambah
 * stok tanpa satu pun nilai pembanding yang dapat dijangkau.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "transaksi_retur_detail")
public class TransaksiReturDetail extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris retur ini. Field audit
	 * shadow yang diisi lewat {@link #setOlehId(String)}. Karena
	 * {@link TransaksiRetur} tidak punya kolom pembuat maupun penyetuju, field
	 * audit shadow inilah satu-satunya jejak pelaku pada jalur ini.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris retur ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan KEHARUSAN
	 * TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas baris retur ini untuk tampilan/log, memakai
	 * {@link #getKeterangan()} sebagai label — konsisten dengan
	 * {@link TransaksiRetur#toString()} pada headernya, namun berbeda dari
	 * baris-baris detail lain di klaster ini yang memakai item sebagai label.
	 * Karena keterangan tidak wajib diisi, baris retur kerap tampil sebagai
	 * teks kosong atau {@code null}.
	 *
	 * @return teks keterangan baris ini.
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris retur ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris retur ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui otomatis
	 * oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String keterangan;
	private Date tanggal = new Date();
	private Double qty = 0.0;
	private ItemMedis item;
	private Racikan racikan;
	private Double amount = 0.0;
	private TransaksiRetur transaksiRetur;
	private TransaksiMedisDetail transaksiDetail;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public TransaksiReturDetail() {
	}

	/**
	 * Primary key baris retur ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik baris retur ini, atau {@code null} untuk baris yang belum
	 *         pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris retur ini.
	 *
	 * @param id ID baris retur.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris retur ini. Karena entitas tidak punya
	 * kolom terstruktur untuk alasan pengembalian per item, teks bebas inilah
	 * satu-satunya tempatnya. Nilainya juga dipakai {@link #toString()} sebagai
	 * label tampilan.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris retur ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil tanggal baris retur ini. Field-nya di-inisialisasi
	 * {@code new Date()} pada saat object dibuat, sehingga baris baru otomatis
	 * bertanggal saat ini kecuali ditimpa secara eksplisit.
	 *
	 * <p>
	 * Perhatikan bahwa baris ini punya tanggalnya SENDIRI, terpisah dari
	 * {@link TransaksiRetur#getTanggal()} milik dokumen induknya, dan skema
	 * tidak menjamin keduanya sama. Baris-baris pada satu dokumen retur karena
	 * itu dapat memiliki tanggal yang berbeda-beda satu sama lain maupun
	 * berbeda dari tanggal dokumennya — sesuatu yang perlu disadari saat
	 * menyusun laporan retur per periode, karena hasilnya akan berbeda
	 * tergantung tanggal mana yang dipakai sebagai dasar penyaringan.
	 * </p>
	 *
	 * @return timestamp baris retur, default waktu pembuatan object.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menetapkan tanggal baris retur ini.
	 *
	 * @param tanggal timestamp baris retur.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengambil kuantitas yang dikembalikan pada baris ini — angka yang
	 * menambah kembali stok barang yang diretur.
	 *
	 * <p>
	 * Kolom ini dipetakan {@code nullable = false}, satu-satunya kuantitas di
	 * klaster ini yang dijamin tidak kosong oleh database — dan field-nya
	 * di-default {@code 0.0} sehingga jaminan itu terpenuhi bahkan tanpa
	 * pengisian eksplisit. Namun jaminan tersebut hanya menyangkut KEBERADAAN
	 * nilai, bukan kewajarannya: nol dan bilangan negatif sama-sama diterima,
	 * dan nilai negatif akan membalik arah mutasi sehingga baris retur justru
	 * mengurangi stok.
	 * </p>
	 * <p>
	 * Tidak ada pembandingan terhadap kuantitas yang dulu dijual pada
	 * {@link #getTransaksiDetail()}, dan tidak ada akumulasi terhadap
	 * retur-retur sebelumnya atas baris penjualan yang sama — sehingga satu
	 * penjualan dapat diretur berkali-kali dengan total melampaui yang pernah
	 * dijual. Penjaga untuk itu hanya dapat hidup di lapisan action, dan hanya
	 * bekerja pada baris yang tautannya terisi.
	 * </p>
	 *
	 * @return kuantitas yang dikembalikan, default {@code 0.0}.
	 */
	@Column(name = "qty", nullable = false)
	public Double getQty() {
		return qty;
	}

	/**
	 * Menetapkan kuantitas yang dikembalikan pada baris ini. Tidak ada
	 * penolakan nilai negatif maupun nol di level entitas; nilai {@code null}
	 * akan ditolak database karena kolomnya {@code NOT NULL}.
	 *
	 * @param qty kuantitas yang dikembalikan.
	 */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/**
	 * Mengambil item medis lepas yang dikembalikan pada baris ini — relasi
	 * OPSIONAL ke {@link ItemMedis}, alternatif dari {@link #getRacikan()}
	 * untuk obat racikan.
	 *
	 * <p>
	 * Skema tidak mengatur hubungan antara kolom ini dan
	 * {@link #getRacikan()}: keduanya boleh terisi bersamaan (ambigu — barang
	 * mana yang sebenarnya kembali) maupun kosong bersamaan (nilai uang
	 * dikembalikan tanpa barang). Skema juga tidak memeriksa bahwa item di sini
	 * sama dengan item pada {@link #getTransaksiDetail()} yang menjadi baris
	 * penjualan asalnya, sehingga item yang tidak pernah dijual pada transaksi
	 * tersebut tetap dapat "dikembalikan" — menambah stok item itu sekaligus
	 * memakai plafon nilai milik penjualan barang lain.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy
	 * dan menugaskan hasilnya kembali ke field — sehingga bukan getter murni:
	 * ia bisa mengubah state object dan membuka koneksi database sendiri saat
	 * sesi Hibernate asalnya sudah tertutup.
	 * </p>
	 *
	 * @return item medis yang dikembalikan, atau {@code null} bila baris ini
	 *         mengembalikan racikan atau tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan item medis lepas yang dikembalikan pada baris ini.
	 *
	 * @param item item medis yang dikembalikan.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil nilai uang yang dikembalikan pada baris ini. Berbeda dari
	 * baris-baris detail di klaster pengadaan yang menyimpan harga SATUAN,
	 * angka di sini adalah NILAI baris — jumlah rupiah yang menjadi kewajiban
	 * dikembalikan kepada pasien.
	 *
	 * <p>
	 * Skema tidak menghubungkan angka ini dengan {@link #getQty()} lewat
	 * perhitungan apa pun, sehingga nilai yang dikembalikan tidak harus
	 * sebanding dengan kuantitas yang dikembalikan — dua baris dengan kuantitas
	 * sama dapat mengembalikan uang yang jauh berbeda tanpa satu pun mekanisme
	 * yang mempertanyakannya. Tidak ada pula pembandingan terhadap nilai yang
	 * dulu benar-benar dibayar pada {@link #getTransaksiDetail()}, sehingga
	 * pengembalian uang dapat melebihi pembayarannya.
	 * </p>
	 * <p>
	 * Di-default {@code 0.0}. Nilai negatif diterima dan bermakna kebalikan
	 * dari pengembalian uang, yaitu tagihan tambahan lewat dokumen yang
	 * bentuknya retur.
	 * </p>
	 *
	 * @return nilai uang yang dikembalikan, default {@code 0.0}.
	 */
	public Double getAmount() {
		return amount;
	}

	/**
	 * Menetapkan nilai uang yang dikembalikan pada baris ini. Tidak ada
	 * pembandingan terhadap nilai yang dulu dibayar, dan tidak ada penolakan
	 * nilai negatif, di level entitas.
	 *
	 * @param amount nilai uang yang dikembalikan.
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}

	/**
	 * Menetapkan dokumen retur induk baris ini.
	 *
	 * @param transaksiRetur dokumen retur induk.
	 */
	public void setTransaksiRetur(TransaksiRetur transaksiRetur) {
		this.transaksiRetur = transaksiRetur;
	}

	/**
	 * Mengambil dokumen {@link TransaksiRetur} yang menjadi induk struktural
	 * baris ini. Lewat induk inilah baris ini memperoleh konteks yang tidak
	 * disimpannya sendiri: lokasi, shift dan bagian kasir, serta flag
	 * {@link TransaksiRetur#getValidasi()} dan
	 * {@link TransaksiRetur#getLunas()} yang menentukan apakah retur sudah sah
	 * dan uangnya sudah dikembalikan.
	 *
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}) walaupun secara bisnis wajib,
	 * sehingga baris detail yatim tetap sah secara skema dan tidak akan pernah
	 * ikut bergerak saat dokumen mana pun divalidasi.
	 * </p>
	 *
	 * @return dokumen retur induk, atau {@code null} bila baris ini yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transaksi_retur", nullable = true)
	public TransaksiRetur getTransaksiRetur() {
		return transaksiRetur;
	}

	/**
	 * Menetapkan baris transaksi penjualan yang menjadi asal baris retur ini.
	 *
	 * @param transaksiDetail baris transaksi penjualan asal.
	 */
	public void setTransaksiDetail(TransaksiMedisDetail transaksiDetail) {
		this.transaksiDetail = transaksiDetail;
	}

	/**
	 * Mengambil baris {@link TransaksiMedisDetail} yang menjadi asal baris
	 * retur ini — tautan tingkat baris ke penjualan yang barangnya
	 * dikembalikan, pelengkap dari tautan tingkat header
	 * {@link TransaksiRetur#getTransaksi()}.
	 *
	 * <p>
	 * Tautan inilah bahan baku penjaga jumlah dan nilai retur: lewat baris
	 * penjualan yang ditunjuk di sini, kuantitas dan nilai yang dulu benar-benar
	 * dijual dapat dijangkau, dan seluruh retur lain atas baris penjualan yang
	 * sama dapat dikumpulkan dengan query balik. Penjaga itu sendiri tidak ada
	 * — entitas tidak membandingkan apa pun, dan tidak ada constraint database
	 * yang melakukannya.
	 * </p>
	 * <p>
	 * Relasi ini OPSIONAL, dan yang membedakan jalur retur penjualan dari jalur
	 * retur pembelian: pada jalur pembelian, tautan tingkat HEADER
	 * ({@link PenerimaanOrderKembali#getPenerimaanOrder()}) bersifat WAJIB
	 * sehingga setidaknya ada satu titik yang dijamin terhubung ke dokumen
	 * dasarnya. Di sini KEDUA tingkat tautan boleh kosong sekaligus — baris ini
	 * dan {@link TransaksiRetur#getTransaksi()} sama-sama opsional. Akibatnya
	 * sebuah retur penjualan dapat berdiri sepenuhnya lepas dari penjualan mana
	 * pun: menambah stok dan menimbulkan kewajiban pengembalian uang tanpa satu
	 * pun nilai pembanding yang dapat dijangkau, dan tanpa cara memverifikasi
	 * bahwa barangnya memang pernah dijual.
	 * </p>
	 * <p>
	 * Skema juga tidak menjaga konsistensi antara tautan baris ini dengan
	 * tautan header: secara skema baris ini bisa menunjuk baris milik transaksi
	 * A sementara dokumen induknya menunjuk transaksi B.
	 * </p>
	 *
	 * @return baris transaksi penjualan asal, atau {@code null} bila baris
	 *         retur ini tidak ditautkan ke baris penjualan mana pun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transaksi_detail", nullable = true)
	public TransaksiMedisDetail getTransaksiDetail() {
		return transaksiDetail;
	}

	/**
	 * Menetapkan racikan yang dikembalikan pada baris ini.
	 *
	 * @param racikan racikan yang dikembalikan.
	 */
	public void setRacikan(Racikan racikan) {
		this.racikan = racikan;
	}

	/**
	 * Mengambil {@link Racikan} yang dikembalikan pada baris ini — relasi
	 * OPSIONAL, alternatif dari {@link #getItem()} untuk barang yang berupa
	 * obat racikan alih-alih item lepas.
	 *
	 * <p>
	 * Keberadaan dua kolom alternatif tanpa aturan yang mengikat keduanya
	 * menuntut kehati-hatian pada kode yang memproses baris ini: kode yang
	 * hanya memeriksa {@link #getItem()} akan melewatkan seluruh baris racikan,
	 * sedangkan kode yang memeriksa salah satu lebih dulu tanpa menangani kasus
	 * keduanya terisi akan diam-diam mengabaikan yang lain. Karena pengembalian
	 * racikan berdampak pada stok bahan-bahan penyusunnya dan bukan pada satu
	 * item tunggal, kedua kasus itu memerlukan perlakuan yang berbeda dan tidak
	 * dapat dipertukarkan.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getItem()}).
	 * </p>
	 *
	 * @return racikan yang dikembalikan, atau {@code null} bila baris ini
	 *         mengembalikan item lepas atau tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "racikan", nullable = true)
	public Racikan getRacikan() {
		racikan = check(racikan);
		return racikan;
	}

}
