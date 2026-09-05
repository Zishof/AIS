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
import ais.database.model.library.Penyedia;

/**
 * Entitas baris (detail) <b>Permintaan Pembelian</b> item medis pada schema
 * {@code sirs} (tabel {@code permintaan_pembelian_detail}). Setiap baris
 * mewakili satu {@link ItemMedis} yang diminta pada satu dokumen
 * {@link PermintaanPembelian}, lengkap dengan kuantitas
 * ({@link #getJumlah()}), satuan ({@link #getSatuanItem()}), perkiraan harga
 * beli ({@link #getHargaBeli()}) dan — bila sudah diketahui saat mengajukan —
 * vendor yang diusulkan ({@link #getPenyedia()}).
 *
 * <h2>Posisi dalam rantai per item</h2>
 * <p>
 * Baris ini adalah HULU dari rantai pengadaan tingkat baris, dan dirujuk dari
 * hilir oleh {@link PesananPembelianDetail#getPermintaanPembelianDetail()}:
 * </p>
 * <pre>
 * PermintaanPembelianDetail -&gt; PesananPembelianDetail -&gt; PenerimaanOrderDetail -&gt; PenerimaanOrderKembaliDetail
 * </pre>
 * <p>
 * Karena seluruh rujukan mengarah dari hilir ke hulu, entitas ini tidak
 * mengetahui apa pun tentang tindak lanjutnya sendiri: tidak ada kolom
 * "sudah dipesan berapa" maupun "sisa yang belum dipesan". Semua informasi itu
 * hanya bisa diperoleh dengan query balik dari sisi {@link PesananPembelianDetail}.
 * </p>
 *
 * <h2>Catatan integritas kuantitas</h2>
 * <p>
 * Tidak ada constraint yang mencegah satu baris permintaan dirujuk oleh BANYAK
 * baris PO sekaligus, dan tidak ada kolom yang menampung akumulasi kuantitas
 * yang sudah dipesan. Akibatnya baris permintaan yang sama bisa ditindaklanjuti
 * berulang kali di beberapa PO berbeda dengan total jauh melampaui
 * {@link #getJumlah()}, tanpa satu pun mekanisme di skema yang menahannya.
 * Penjaga akumulatif semacam itu hanya bisa hidup di lapisan action.
 * </p>
 * <p>
 * Seluruh relasi pada baris ini OPSIONAL ({@code nullable = true}), termasuk
 * {@link #getPermintaanPembelian()} yang secara bisnis wajib — sehingga baris
 * detail yatim tanpa induk tetap sah secara skema dan tidak akan terjaring
 * constraint apa pun.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "permintaan_pembelian_detail")
public class PermintaanPembelianDetail extends GeneralValueObject {

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
	 * Representasi ringkas baris permintaan ini untuk tampilan/log, berupa
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
	private PermintaanPembelian permintaanPembelian;
	private String keterangan;
	private Penyedia vendor;
	private Double hargaBeli;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PermintaanPembelianDetail() {
	}

	/**
	 * Primary key baris permintaan ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik baris permintaan ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris permintaan ini.
	 *
	 * @param id ID baris permintaan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris permintaan ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris permintaan ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan item medis yang diminta pada baris ini.
	 *
	 * @param item item medis yang diminta.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang diminta pada baris ini — relasi OPSIONAL ke
	 * {@link ItemMedis}. Bersama {@link #getJumlah()} dan
	 * {@link #getSatuanItem()} inilah tiga serangkai yang mendefinisikan apa
	 * yang diminta; skema tidak menghalangi salah satunya kosong.
	 *
	 * @return item medis yang diminta, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		return item;
	}

	/**
	 * Menetapkan satuan item yang dipakai pada baris ini.
	 *
	 * @param satuanItem satuan item permintaan.
	 */
	public void setSatuanItem(SatuanItem satuanItem) {
		this.satuanItem = satuanItem;
	}

	/**
	 * Mengambil satuan item yang dipakai pada baris ini — relasi OPSIONAL ke
	 * {@link SatuanItem}, yang menentukan arti angka {@link #getJumlah()}.
	 * Karena satuan bisa berbeda antara tahap permintaan, pemesanan dan
	 * penerimaan, perbandingan kuantitas antar tahap TIDAK sah dilakukan
	 * langsung tanpa lebih dulu menormalkan lewat konversi satuan (lihat
	 * {@link KonversiSatuanItem}).
	 *
	 * @return satuan item permintaan, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "satuan_item", nullable = true)
	public SatuanItem getSatuanItem() {
		return satuanItem;
	}

	/**
	 * Menetapkan kuantitas yang diminta pada baris ini. Tidak ada penolakan
	 * nilai negatif maupun nol di level entitas.
	 *
	 * @param jumlah kuantitas yang diminta, dinyatakan dalam
	 *               {@link #getSatuanItem()}.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil kuantitas yang diminta pada baris ini, dinyatakan dalam satuan
	 * {@link #getSatuanItem()}. Angka ini adalah kebutuhan yang DIAJUKAN unit;
	 * ia tidak mengikat tahap berikutnya — {@link PesananPembelianDetail}
	 * bebas memesan lebih banyak atau lebih sedikit, dan skema tidak
	 * membandingkan keduanya.
	 *
	 * <p>
	 * Sama seperti pada tahap-tahap berikutnya, entitas ini tidak mencatat
	 * berapa banyak dari jumlah tersebut yang sudah ditindaklanjuti menjadi PO.
	 * Karena satu baris permintaan boleh dirujuk oleh banyak baris PO, angka
	 * pemenuhan hanya bisa diperoleh dengan mengagregasi seluruh
	 * {@link PesananPembelianDetail} yang menunjuk baris ini.
	 * </p>
	 * <p>
	 * Nilainya bisa {@code null} untuk baris yang belum lengkap diisi;
	 * pemanggil yang menjumlahkan kuantitas wajib menangani {@code null} agar
	 * tidak melempar {@link NullPointerException} saat auto-unboxing.
	 * </p>
	 *
	 * @return kuantitas yang diminta, atau {@code null} bila belum diisi.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan dokumen permintaan induk baris ini.
	 *
	 * @param permintaanPembelian dokumen permintaan induk.
	 */
	public void setPermintaanPembelian(PermintaanPembelian permintaanPembelian) {
		this.permintaanPembelian = permintaanPembelian;
	}

	/**
	 * Mengambil dokumen {@link PermintaanPembelian} yang menjadi induk
	 * struktural baris ini. Relasi OPSIONAL ({@code nullable = true}) walaupun
	 * secara bisnis wajib — akibatnya baris detail yatim tanpa induk tetap bisa
	 * tersimpan dan tidak akan pernah muncul di layar mana pun karena selalu
	 * di-query lewat induknya.
	 *
	 * @return dokumen permintaan induk, atau {@code null} bila baris ini yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "permintaan_pembelian", nullable = true)
	public PermintaanPembelian getPermintaanPembelian() {
		return permintaanPembelian;
	}

	/**
	 * Menetapkan vendor/penyedia yang diusulkan pada baris permintaan ini.
	 *
	 * <p>
	 * Perhatikan ketidakselarasan penamaan yang disengaja: nama method memakai
	 * istilah {@code Penyedia} (mengikuti nama kelas {@link Penyedia}),
	 * sedangkan field dan kolom database memakai istilah {@code vendor}. Nama
	 * property JavaBean yang dikenali Hibernate maupun ZK adalah
	 * {@code penyedia}, bukan {@code vendor}.
	 * </p>
	 *
	 * @param vendor penyedia/vendor yang diusulkan.
	 */
	public void setPenyedia(Penyedia vendor) {
		this.vendor = vendor;
	}

	/**
	 * Mengambil vendor/penyedia yang diusulkan pada baris permintaan ini,
	 * dipetakan ke kolom {@code vendor}. Bersifat usulan per baris: tahap
	 * pemesanan menetapkan vendor final di tingkat DOKUMEN lewat
	 * {@link PesananPembelian#getPenyedia()}, dan tidak ada mekanisme di skema
	 * yang menjamin keduanya sama. Sebuah PO karena itu bisa saja diterbitkan
	 * ke vendor yang sama sekali berbeda dari yang diusulkan di sini tanpa
	 * jejak penyimpangan.
	 *
	 * <p>
	 * Getter ini memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy
	 * sebelum mengembalikan nilai, dan hasilnya ditugaskan kembali ke field —
	 * pola resolusi lazy standar di paket model AIS. Efek sampingnya, getter
	 * ini TIDAK murni: ia bisa mengubah state object dan bahkan membuka
	 * koneksi database sendiri saat sesi Hibernate asalnya sudah tertutup.
	 * </p>
	 *
	 * @return penyedia/vendor yang diusulkan, atau {@code null} bila belum
	 *         diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "vendor", nullable = true)
	public Penyedia getPenyedia() {
		vendor = check(vendor);
		return vendor;
	}

	/**
	 * Menetapkan perkiraan harga beli satuan pada baris permintaan ini.
	 *
	 * @param hargaBeli perkiraan harga beli per satuan.
	 */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * Mengambil perkiraan harga beli satuan pada baris permintaan ini. Nilai
	 * ini bersifat estimasi untuk keperluan penganggaran; harga yang disepakati
	 * tercatat di {@link PesananPembelianDetail#getHargaBeli()} dan harga yang
	 * benar-benar ditagih di {@link PenerimaanOrderDetail#getHargaBeli()}.
	 * Ketiganya boleh berbeda dan skema tidak membandingkannya.
	 *
	 * @return perkiraan harga beli per satuan, atau {@code null} bila belum
	 *         diisi.
	 */
	@Column(name = "harga_beli", nullable = true)
	public Double getHargaBeli() {
		return hargaBeli;
	}

}
