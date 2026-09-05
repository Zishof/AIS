package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
 * Entitas baris (detail) <b>Penerimaan Order Kembali</b> (retur pembelian) item
 * medis pada schema {@code sirs} (tabel
 * {@code penerimaan_order_kembali_detail}). Setiap baris mencatat satu
 * {@link ItemMedis} yang dikembalikan ke vendor pada satu dokumen
 * {@link PenerimaanOrderKembali}, beserta kuantitas dan satuannya.
 *
 * <h2>Baris inilah yang mengurangi stok</h2>
 * <p>
 * Baris ini adalah unit terkecil yang mengurangi persediaan pada jalur retur
 * pembelian: ketika dokumen induknya disetujui, {@link #getJumlah()} inilah
 * yang diterjemahkan menjadi mutasi stok bertanda mengurangi di gudang
 * {@link PenerimaanOrderKembali#getLokasi()}. Ia adalah ujung terakhir rantai
 * pengadaan tingkat baris:
 * </p>
 * <pre>
 * PermintaanPembelianDetail -&gt; PesananPembelianDetail -&gt; PenerimaanOrderDetail -&gt; PenerimaanOrderKembaliDetail
 * </pre>
 *
 * <h2>Tautan ke baris penerimaan — tersedia, tetapi bukan penjaga</h2>
 * <p>
 * Baris ini menyimpan tautan FK NYATA ke baris penerimaan asalnya lewat
 * {@link #getPenerimaanOrderDetail()}, sehingga jumlah yang pernah diterima
 * ({@link PenerimaanOrderDetail#getJumlah()}) dapat dijangkau langsung. Namun
 * sama seperti pada hubungan penerimaan-pesanan, tautan yang tersedia itu
 * TIDAK dengan sendirinya menjadi penjaga: entitas ini tidak membandingkan
 * {@link #getJumlah()} dengan jumlah yang diterima, dan tidak ada constraint
 * database yang melakukannya.
 * </p>
 * <p>
 * Penjaga jumlah-retur yang memadai harus mengagregasi seluruh
 * {@link PenerimaanOrderKembaliDetail} yang menunjuk baris penerimaan yang sama
 * (satu penerimaan boleh diretur bertahap lewat beberapa dokumen retur),
 * mengecualikan baris yang berada di dokumen retur yang sudah dibatalkan, dan
 * menormalkan satuan bila {@link #getSatuanItem()} berbeda dari satuan pada
 * baris penerimaan. Tanpa penjaga demikian, retur yang melebihi jumlah yang
 * pernah diterima akan menghasilkan stok negatif, atau diam-diam menghapus
 * stok yang sebenarnya berasal dari penerimaan lain.
 * </p>
 *
 * <h2>Catatan lain</h2>
 * <p>
 * Berbeda dari {@link PenerimaanOrderDetail} yang menyimpan harga beli, diskon
 * dan pajak, baris retur ini TIDAK menyimpan nilai apa pun — hanya kuantitas.
 * Nilai barang yang diretur karena itu hanya bisa diperoleh dari baris
 * penerimaan asalnya lewat {@link #getPenerimaanOrderDetail()}, dan menjadi
 * mustahil ditelusuri bila tautan opsional itu dibiarkan kosong. Baris ini
 * juga tidak menyimpan {@code tanggalKadaluarsa}, sehingga tidak dapat
 * diketahui batch mana yang dikembalikan.
 * </p>
 * <p>
 * Seluruh relasi baris ini OPSIONAL ({@code nullable = true}), termasuk
 * {@link #getPenerimaanOrderKembali()}, sehingga baris detail yatim sah secara
 * skema. Entitas ini juga tidak memiliki field {@code index} seperti
 * kebanyakan header di klaster ini.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "penerimaan_order_kembali_detail")
public class PenerimaanOrderKembaliDetail extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini. Field audit
	 * shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
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
	 * Representasi ringkas baris retur ini untuk tampilan/log, berupa
	 * {@link #getItem()} yang dirangkai jadi teks. Konkatenasi dengan
	 * {@code ""} dipakai agar item yang belum diisi menghasilkan teks
	 * {@code "null"} alih-alih melempar {@link NullPointerException}.
	 *
	 * @return teks representasi item pada baris ini.
	 */
	public String toString() {
		return item + "";
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
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

	private ItemMedis item;
	private SatuanItem satuanItem;
	private Double jumlah;
	private PenerimaanOrderKembali penerimaanOrderKembali;
	private PenerimaanOrderDetail penerimaanOrderDetail;
	private String keterangan;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PenerimaanOrderKembaliDetail() {
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
	 * kolom terstruktur untuk alasan retur per item, kolom bebas inilah
	 * satu-satunya tempat alasan tersebut dicatat pada tingkat baris.
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
	 * Menetapkan item medis yang diretur pada baris ini.
	 *
	 * @param item item medis yang diretur.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang diretur pada baris ini — relasi OPSIONAL ke
	 * {@link ItemMedis}. Inilah item yang stoknya berkurang saat dokumen induk
	 * disetujui.
	 *
	 * <p>
	 * Skema tidak menjamin item di sini sama dengan item pada baris penerimaan
	 * yang ditunjuk {@link #getPenerimaanOrderDetail()}, maupun sama dengan
	 * salah satu item pada dokumen penerimaan yang menjadi dasar returnya.
	 * Ketidakcocokan tersebut memungkinkan pengurangan stok atas item yang
	 * tidak pernah diterima dari vendor tersebut, dengan berkedok dokumen
	 * retur yang tampak sah — pencocokan item karena itu perlu divalidasi di
	 * lapisan action.
	 * </p>
	 *
	 * @return item medis yang diretur, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		return item;
	}

	/**
	 * Menetapkan satuan item retur pada baris ini.
	 *
	 * @param satuanItem satuan item retur.
	 */
	public void setSatuanItem(SatuanItem satuanItem) {
		this.satuanItem = satuanItem;
	}

	/**
	 * Mengambil satuan item retur pada baris ini — relasi OPSIONAL ke
	 * {@link SatuanItem}, yang menentukan arti angka {@link #getJumlah()}.
	 * Tidak dijamin sama dengan satuan pada baris penerimaan di
	 * {@link PenerimaanOrderDetail#getSatuanItem()}, sehingga setiap
	 * perbandingan kuantitas antara retur dan penerimaannya harus didahului
	 * normalisasi lewat {@link KonversiSatuanItem}. Membandingkan angka mentah
	 * dengan satuan yang berbeda bisa meloloskan retur yang berlipat-lipat dari
	 * yang pernah diterima.
	 *
	 * @return satuan item retur, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "satuan_item", nullable = true)
	public SatuanItem getSatuanItem() {
		return satuanItem;
	}

	/**
	 * Menetapkan kuantitas yang diretur pada baris ini. Tidak ada penolakan
	 * nilai negatif maupun nol di level entitas, dan tidak ada pembandingan
	 * terhadap jumlah yang pernah diterima — validasi tersebut harus dilakukan
	 * di lapisan action.
	 *
	 * @param jumlah kuantitas yang diretur, dinyatakan dalam
	 *               {@link #getSatuanItem()}.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil kuantitas yang diretur pada baris ini, dinyatakan dalam satuan
	 * {@link #getSatuanItem()} — angka yang akan MENGURANGI stok di gudang
	 * {@link PenerimaanOrderKembali#getLokasi()} saat dokumen induk disetujui.
	 *
	 * <p>
	 * Skema tidak memasang satu pun batas pada angka ini. Tidak ada
	 * pembandingan terhadap {@link PenerimaanOrderDetail#getJumlah()} pada
	 * baris penerimaan yang ditunjuk, tidak ada akumulasi terhadap retur-retur
	 * sebelumnya atas baris penerimaan yang sama, dan tidak ada penolakan nilai
	 * negatif maupun nol. Konsekuensinya berlapis: retur berlebih menghasilkan
	 * stok negatif atau menghapus stok milik penerimaan lain, sementara nilai
	 * NEGATIF di kolom ini akan diterjemahkan menjadi pengurangan atas angka
	 * negatif — yaitu PENAMBAHAN stok lewat dokumen yang bentuknya retur,
	 * penambahan persediaan tanpa jejak dokumen penerimaan.
	 * </p>
	 * <p>
	 * Nilainya bisa {@code null} karena tidak di-default seperti pada
	 * {@link PenerimaanOrderDetail#getJumlah()}; pemanggil yang menjumlahkan
	 * atau memakainya untuk mutasi stok WAJIB menangani {@code null} agar tidak
	 * melempar {@link NullPointerException} saat auto-unboxing.
	 * </p>
	 *
	 * @return kuantitas yang diretur, atau {@code null} bila belum diisi.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan dokumen retur induk baris ini.
	 *
	 * @param penerimaanOrderKembali dokumen retur induk.
	 */
	public void setPenerimaanOrderKembali(PenerimaanOrderKembali penerimaanOrderKembali) {
		this.penerimaanOrderKembali = penerimaanOrderKembali;
	}

	/**
	 * Mengambil dokumen {@link PenerimaanOrderKembali} yang menjadi induk
	 * struktural baris ini. Lewat induk inilah baris ini memperoleh konteks
	 * yang tidak disimpannya sendiri: gudang asal, status persetujuan yang
	 * menentukan apakah stok sudah berkurang, dan status pembatalan.
	 *
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}) walaupun secara bisnis wajib,
	 * sehingga baris detail yatim tetap sah secara skema dan tidak akan pernah
	 * ikut bergerak saat dokumen mana pun disetujui atau dibatalkan.
	 * </p>
	 *
	 * @return dokumen retur induk, atau {@code null} bila baris ini yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_order_kembali", nullable = true)
	public PenerimaanOrderKembali getPenerimaanOrderKembali() {
		return penerimaanOrderKembali;
	}

	/**
	 * Menetapkan baris penerimaan yang menjadi asal baris retur ini.
	 *
	 * @param penerimaanOrderDetail baris penerimaan asal.
	 */
	public void setPenerimaanOrderDetail(PenerimaanOrderDetail penerimaanOrderDetail) {
		this.penerimaanOrderDetail = penerimaanOrderDetail;
	}

	/**
	 * Mengambil baris {@link PenerimaanOrderDetail} yang menjadi asal baris
	 * retur ini — tautan FK NYATA tingkat baris, mata rantai terakhir yang
	 * melengkapi penelusuran per item dari permintaan sampai pengembalian.
	 *
	 * <p>
	 * Tautan inilah bahan baku penjaga jumlah-retur: lewat baris penerimaan
	 * yang ditunjuk di sini, jumlah yang pernah diterima
	 * ({@link PenerimaanOrderDetail#getJumlah()}) dapat dijangkau, seluruh
	 * retur lain atas baris penerimaan yang sama dapat dikumpulkan dengan query
	 * balik, dan nilai barang yang diretur dapat diperoleh dari
	 * {@link PenerimaanOrderDetail#getHargaBeli()} — satu-satunya sumber nilai,
	 * karena baris retur ini tidak menyimpan harga sama sekali.
	 * </p>
	 * <p>
	 * Relasi ini OPSIONAL ({@code nullable = true}), berbeda dari tautan
	 * tingkat header {@link PenerimaanOrderKembali#getPenerimaanOrder()} yang
	 * wajib. Baris retur yang membiarkan tautan ini kosong akan mengurangi stok
	 * tanpa terhubung ke penerimaan mana pun: ia LOLOS dari penjaga
	 * jumlah-retur yang bekerja lewat tautan ini, dan nilai barangnya tidak
	 * dapat ditelusuri sama sekali. Karena itu penjaga yang benar tidak cukup
	 * memeriksa baris-baris yang kebetulan tertaut — tautan ini perlu
	 * diwajibkan terisi di lapisan action.
	 * </p>
	 *
	 * @return baris penerimaan asal, atau {@code null} bila baris retur ini
	 *         tidak ditautkan ke baris penerimaan mana pun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_order_detail", nullable = true)
	public PenerimaanOrderDetail getPenerimaanOrderDetail() {
		return penerimaanOrderDetail;
	}

}
