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
 * Entitas baris (detail) <b>Pesanan Pembelian</b> item medis pada schema
 * {@code sirs} (tabel {@code pesanan_pembelian_detail}). Setiap baris mewakili
 * satu {@link ItemMedis} yang dipesan pada satu dokumen
 * {@link PesananPembelian}, lengkap dengan kuantitas ({@link #getJumlah()}),
 * satuan ({@link #getSatuanItem()}) dan harga beli yang disepakati
 * ({@link #getHargaBeli()}).
 *
 * <h2>Tautan asal dan lanjutan</h2>
 * <p>
 * Baris ini menyimpan DUA relasi ke atas sekaligus, dan keduanya penting untuk
 * alasan yang berbeda:
 * </p>
 * <ul>
 *   <li>{@link #getPesananPembelian()} — induk struktural, dokumen PO tempat
 *       baris ini berada.</li>
 *   <li>{@link #getPermintaanPembelianDetail()} — asal-usul, baris permintaan
 *       yang memicu pemesanan ini. Inilah tautan FK NYATA tingkat baris yang
 *       membuat rantai pengadaan {@code sirs} bisa ditelusuri per item, bukan
 *       hanya per dokumen.</li>
 * </ul>
 * <p>
 * Ke arah hilir, baris ini menjadi rujukan bagi
 * {@link PenerimaanOrderDetail#getPesananPembelianDetail()}, sehingga rantai
 * lengkap per item adalah:
 * </p>
 * <pre>
 * PermintaanPembelianDetail -&gt; PesananPembelianDetail -&gt; PenerimaanOrderDetail -&gt; PenerimaanOrderKembaliDetail
 * </pre>
 *
 * <h2>Catatan integritas kuantitas</h2>
 * <p>
 * Entitas ini menyimpan HANYA kuantitas yang dipesan. Tidak ada kolom
 * "jumlah sudah diterima", "sisa", atau status pemenuhan. Konsekuensinya:
 * </p>
 * <ul>
 *   <li>Berapa banyak barang yang sudah diterima terhadap baris PO ini hanya
 *       bisa diketahui dengan MENJUMLAHKAN seluruh
 *       {@link PenerimaanOrderDetail#getJumlah()} (dan mungkin
 *       {@link PenerimaanOrderDetail#getJumlahBonus()}) dari semua dokumen
 *       penerimaan yang menunjuk baris ini.</li>
 *   <li>Penjaga anti-lebih-terima (jumlah diterima kumulatif tidak boleh
 *       melebihi {@link #getJumlah()}) TIDAK mungkin ditegakkan oleh entitas
 *       maupun oleh constraint database; ia hanya bisa hidup di lapisan action
 *       yang menyimpan {@link PenerimaanOrderDetail}, dan harus menghitung
 *       akumulasi lintas dokumen penerimaan — bukan sekadar membandingkan satu
 *       dokumen penerimaan terhadap PO-nya.</li>
 *   <li>Tipe {@link Double} untuk kuantitas dan harga membuat perhitungan
 *       akumulasi rawan galat pembulatan biner; perbandingan
 *       "sudah terpenuhi" sebaiknya memakai toleransi, bukan {@code ==}.</li>
 * </ul>
 * <p>
 * Seluruh relasi pada baris ini OPSIONAL ({@code nullable = true}), termasuk
 * {@link #getItem()} dan {@link #getPesananPembelian()}. Baris detail yatim
 * (tanpa induk PO) karena itu SAH secara skema dan tidak akan terjaring oleh
 * constraint apa pun — ia hanya akan menghilang dari tampilan karena tidak
 * pernah ikut ter-query lewat induknya.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "pesanan_pembelian_detail")
public class PesananPembelianDetail extends GeneralValueObject {

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
	 * Representasi ringkas baris PO ini untuk tampilan/log, berupa
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
	private PesananPembelian pesananPembelian;
	private PermintaanPembelianDetail permintaanPembelianDetail;
	private String keterangan;
	private Double hargaBeli;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PesananPembelianDetail() {
	}

	/**
	 * Primary key baris PO ini, auto-increment (IDENTITY) dan diisi database.
	 *
	 * @return ID unik baris PO ini, atau {@code null} untuk baris yang belum
	 *         pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris PO ini.
	 *
	 * @param id ID baris PO.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris PO ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris PO ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan item medis yang dipesan pada baris ini.
	 *
	 * @param item item medis yang dipesan.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang dipesan pada baris ini — relasi OPSIONAL ke
	 * {@link ItemMedis}. Bersama {@link #getJumlah()} dan
	 * {@link #getSatuanItem()}, inilah tiga serangkai yang mendefinisikan apa
	 * yang dipesan; kosongnya salah satu dari ketiganya membuat baris PO tidak
	 * bermakna, namun skema tidak menghalanginya.
	 *
	 * <p>
	 * Tidak ada mekanisme di skema yang menjamin item pada baris ini sama
	 * dengan item pada {@link #getPermintaanPembelianDetail()} yang menjadi
	 * asalnya, maupun sama dengan item pada
	 * {@link PenerimaanOrderDetail#getItem()} yang menerimanya. Ketiganya bisa
	 * berbeda tanpa terdeteksi, sehingga pencocokan item lintas tahap perlu
	 * divalidasi di lapisan action.
	 * </p>
	 *
	 * @return item medis yang dipesan, atau {@code null} bila belum diisi.
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
	 * @param satuanItem satuan item pemesanan.
	 */
	public void setSatuanItem(SatuanItem satuanItem) {
		this.satuanItem = satuanItem;
	}

	/**
	 * Mengambil satuan item yang dipakai pada baris ini — relasi OPSIONAL ke
	 * {@link SatuanItem}. Satuan ini menentukan arti angka
	 * {@link #getJumlah()}: 10 dalam satuan "box" sangat berbeda dengan 10
	 * dalam satuan "tablet". Karena satuan bisa berbeda antara tahap pemesanan
	 * dan tahap penerimaan, perbandingan kuantitas antar tahap TIDAK sah
	 * dilakukan langsung tanpa lebih dulu menormalkan lewat konversi satuan
	 * (lihat {@link KonversiSatuanItem}).
	 *
	 * @return satuan item pemesanan, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "satuan_item", nullable = true)
	public SatuanItem getSatuanItem() {
		return satuanItem;
	}

	/**
	 * Menetapkan kuantitas yang dipesan pada baris ini. Tidak ada penolakan
	 * nilai negatif maupun nol di level entitas — validasi tersebut harus
	 * dilakukan di lapisan action.
	 *
	 * @param jumlah kuantitas yang dipesan, dinyatakan dalam
	 *               {@link #getSatuanItem()}.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil kuantitas yang dipesan pada baris ini, dinyatakan dalam satuan
	 * {@link #getSatuanItem()}. Ini adalah PLAFON pemesanan yang menjadi acuan
	 * bagi penjaga anti-lebih-terima pada tahap penerimaan barang.
	 *
	 * <p>
	 * Perlu ditegaskan bahwa entitas ini TIDAK mencatat berapa banyak dari
	 * jumlah tersebut yang sudah diterima. Satu baris PO boleh dipenuhi oleh
	 * beberapa dokumen {@link PenerimaanOrder} (penerimaan bertahap/parsial),
	 * dan masing-masing {@link PenerimaanOrderDetail} hanya menyimpan jumlah
	 * penerimaannya sendiri. Karena itu pemeriksaan "apakah penerimaan kali
	 * ini membuat total melebihi yang dipesan" mensyaratkan agregasi seluruh
	 * penerimaan terdahulu untuk baris PO ini; membandingkan hanya satu
	 * dokumen penerimaan terhadap {@code jumlah} di sini adalah pemeriksaan
	 * yang bisa ditembus dengan cara memecah penerimaan menjadi beberapa
	 * dokumen.
	 * </p>
	 * <p>
	 * Nilainya bisa {@code null} untuk baris yang belum lengkap diisi;
	 * pemanggil yang menjumlahkan kuantitas wajib menangani {@code null} agar
	 * tidak melempar {@link NullPointerException} saat auto-unboxing.
	 * </p>
	 *
	 * @return kuantitas yang dipesan, atau {@code null} bila belum diisi.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan dokumen PO induk baris ini.
	 *
	 * @param pesananPembelian dokumen PO induk.
	 */
	public void setPesananPembelian(PesananPembelian pesananPembelian) {
		this.pesananPembelian = pesananPembelian;
	}

	/**
	 * Mengambil dokumen {@link PesananPembelian} yang menjadi induk struktural
	 * baris ini. Relasi OPSIONAL ({@code nullable = true}) walaupun secara
	 * bisnis wajib — akibatnya baris detail yatim tanpa induk PO tetap bisa
	 * tersimpan dan tidak akan pernah muncul di layar mana pun karena selalu
	 * di-query lewat induknya.
	 *
	 * @return dokumen PO induk, atau {@code null} bila baris ini yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pesanan_pembelian", nullable = true)
	public PesananPembelian getPesananPembelian() {
		return pesananPembelian;
	}

	/**
	 * Menetapkan baris permintaan pembelian yang menjadi asal baris PO ini.
	 *
	 * @param permintaanPembelianDetail baris permintaan asal.
	 */
	public void setPermintaanPembelianDetail(PermintaanPembelianDetail permintaanPembelianDetail) {
		this.permintaanPembelianDetail = permintaanPembelianDetail;
	}

	/**
	 * Mengambil baris {@link PermintaanPembelianDetail} yang menjadi asal baris
	 * PO ini — tautan FK NYATA tingkat baris yang menjadikan rantai pengadaan
	 * {@code sirs} dapat ditelusuri per item.
	 *
	 * <p>
	 * Karena tautan ini OPSIONAL, sebuah baris PO boleh berdiri tanpa baris
	 * permintaan asal. Selain itu tidak ada constraint yang mencegah satu baris
	 * permintaan dirujuk oleh BANYAK baris PO sekaligus: secara skema baris
	 * permintaan yang sama bisa "dipesan" berulang kali di beberapa PO
	 * berbeda, dan totalnya bisa jauh melampaui kuantitas yang sebenarnya
	 * diminta di
	 * {@link PermintaanPembelianDetail#getJumlah()}. Sama seperti pada sisi
	 * penerimaan, penjaga akumulatif untuk hal ini hanya bisa hidup di lapisan
	 * action.
	 * </p>
	 *
	 * @return baris permintaan asal, atau {@code null} bila baris PO ini tidak
	 *         ditautkan ke permintaan mana pun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "permintaan_pembelian_detail", nullable = true)
	public PermintaanPembelianDetail getPermintaanPembelianDetail() {
		return permintaanPembelianDetail;
	}

	/**
	 * Menetapkan harga beli satuan yang disepakati pada baris ini.
	 *
	 * @param hargaBeli harga beli per satuan.
	 */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * Mengambil harga beli satuan yang disepakati pada baris PO ini. Nilai ini
	 * adalah harga yang DIPESAN; harga yang benar-benar ditagih vendor tercatat
	 * terpisah di {@link PenerimaanOrderDetail#getHargaBeli()}, dan skema tidak
	 * memaksa keduanya sama — selisih harga pesan versus harga terima justru
	 * merupakan informasi yang perlu dipantau, bukan anomali yang dicegah.
	 *
	 * @return harga beli per satuan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "harga_beli", nullable = true)
	public Double getHargaBeli() {
		return hargaBeli;
	}

}
