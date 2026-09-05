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
 * Entitas baris (detail) <b>Penerimaan Order</b> item medis pada schema
 * {@code sirs} (tabel {@code penerimaan_order_detail}). Setiap baris mencatat
 * satu {@link ItemMedis} yang benar-benar DITERIMA pada satu dokumen
 * {@link PenerimaanOrder}: berapa banyak, dalam satuan apa, dengan harga
 * berapa, dan — khusus untuk bahan medis — sampai kapan barangnya layak pakai
 * ({@link #getTanggalKadaluarsa()}).
 *
 * <h2>Baris inilah yang menambah stok</h2>
 * <p>
 * Baris ini adalah unit terkecil yang berdampak pada persediaan: ketika
 * dokumen induknya disetujui, {@link #getJumlah()} dan
 * {@link #getJumlahBonus()} inilah yang diterjemahkan menjadi mutasi stok
 * bertanda menambah di gudang {@link PenerimaanOrder#getLokasi()}. Ia juga
 * unit terkecil yang berdampak pada nilai persediaan, lewat kombinasi
 * {@link #getHargaBeli()}, {@link #getHargaDiskon()} dan
 * {@link #getHargaPajak()}.
 * </p>
 *
 * <h2>Tautan ke baris pesanan — tersedia, tetapi bukan penjaga</h2>
 * <p>
 * Baris ini menyimpan tautan FK NYATA ke baris PO asalnya lewat
 * {@link #getPesananPembelianDetail()}, melengkapi rantai per item:
 * </p>
 * <pre>
 * PermintaanPembelianDetail -&gt; PesananPembelianDetail -&gt; PenerimaanOrderDetail -&gt; PenerimaanOrderKembaliDetail
 * </pre>
 * <p>
 * Keberadaan tautan ini penting untuk ditegaskan karena sering disalahpahami:
 * bahan untuk membangun penjaga anti-lebih-terima SUDAH ADA di skema. Yang
 * tidak ada adalah penjaganya itu sendiri. Entitas ini tidak membandingkan
 * {@link #getJumlah()} dengan
 * {@link PesananPembelianDetail#getJumlah()} pada baris yang ditunjuk, dan
 * tidak ada constraint database yang melakukannya.
 * </p>
 *
 * <h2>Bentuk penjaga anti-lebih-terima yang benar</h2>
 * <p>
 * Penjaga yang memadai TIDAK cukup berupa perbandingan satu baris penerimaan
 * terhadap baris PO-nya, karena satu baris PO boleh dipenuhi bertahap oleh
 * beberapa dokumen {@link PenerimaanOrder}. Perbandingan satu-lawan-satu bisa
 * ditembus hanya dengan memecah penerimaan menjadi beberapa dokumen yang
 * masing-masing "masih dalam batas". Penjaga yang benar harus:
 * </p>
 * <ol>
 *   <li>mengagregasi seluruh {@link PenerimaanOrderDetail} yang menunjuk baris
 *       PO yang sama (tidak termasuk yang berada di dokumen penerimaan yang
 *       sudah dibatalkan);</li>
 *   <li>menormalkan satuannya lebih dulu bila
 *       {@link #getSatuanItem()} berbeda dari satuan pada baris PO (lihat
 *       {@link KonversiSatuanItem});</li>
 *   <li>memutuskan secara eksplisit apakah {@link #getJumlahBonus()} ikut
 *       dihitung terhadap plafon pesanan atau tidak — barang bonus secara
 *       bisnis biasanya TIDAK dipesan, sehingga memasukkannya ke perbandingan
 *       akan menolak penerimaan yang sah, sementara mengabaikannya membuka
 *       jalan menyelundupkan kelebihan sebagai "bonus";</li>
 *   <li>berjalan pada tahap yang benar-benar mengikat, yaitu saat dokumen
 *       DISETUJUI (titik saat stok bergerak), bukan hanya saat baris diketik —
 *       pemeriksaan yang hanya ada di listener input akan terlewat oleh jalur
 *       simpan lain.</li>
 * </ol>
 *
 * <h2>Catatan lain</h2>
 * <p>
 * Seluruh relasi baris ini OPSIONAL ({@code nullable = true}), termasuk
 * {@link #getPenerimaanOrder()}, sehingga baris detail yatim sah secara skema.
 * Nilai kuantitas dan harga di-default {@code 0.0} (bukan {@code null}),
 * sehingga baris yang lolos tanpa pengisian akan terhitung sebagai nol —
 * lebih aman daripada {@link NullPointerException}, tetapi juga membuat baris
 * kosong tidak terdeteksi sebagai anomali.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "penerimaan_order_detail")
public class PenerimaanOrderDetail extends GeneralValueObject {

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
	 * Representasi ringkas baris penerimaan ini untuk tampilan/log, berupa
	 * {@link #getItem()} yang dirangkai jadi teks. Konkatenasi dengan
	 * {@code ""} dipakai agar item yang belum diisi menghasilkan teks
	 * {@code "null"} alih-alih melempar {@link NullPointerException}.
	 *
	 * <p>
	 * Perhatikan bahwa {@link #getItem()} yang dipanggil di sini adalah getter
	 * ber-{@code check(...)}; namun {@code toString()} membaca FIELD-nya
	 * langsung, bukan lewat getter, sehingga tidak memicu resolusi proxy lazy.
	 * Untuk entity yang di-load lazy, hasil {@code toString()} bisa berupa
	 * representasi proxy alih-alih nama item yang sesungguhnya.
	 * </p>
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
	private Double jumlah = 0.0;
	private Double jumlahBonus = 0.0;
	private Date tanggalKadaluarsa;
	private Double hargaBeli = 0.0;
	private Double hargaDiskon = 0.0;
	private Double hargaPajak = 0.0;
	private PenerimaanOrder penerimaanOrder;
	private PesananPembelianDetail pesananPembelianDetail;
	private String keterangan;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PenerimaanOrderDetail() {
	}

	/**
	 * Primary key baris penerimaan ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik baris penerimaan ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris penerimaan ini.
	 *
	 * @param id ID baris penerimaan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris penerimaan ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris penerimaan ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan item medis yang diterima pada baris ini.
	 *
	 * @param item item medis yang diterima.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang diterima pada baris ini — relasi OPSIONAL ke
	 * {@link ItemMedis}. Inilah item yang stoknya bertambah saat dokumen induk
	 * disetujui.
	 *
	 * <p>
	 * Skema tidak menjamin item di sini sama dengan item pada baris PO yang
	 * ditunjuk {@link #getPesananPembelianDetail()}. Ketidakcocokan semacam itu
	 * berbahaya secara diam-diam: stok item A bertambah sementara plafon yang
	 * "terpakai" adalah plafon item B, sehingga baris PO untuk item B bisa
	 * tampak terpenuhi padahal barangnya tidak pernah datang. Pencocokan item
	 * antara baris penerimaan dan baris PO-nya karena itu perlu divalidasi di
	 * lapisan action.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy
	 * dan menugaskan hasilnya kembali ke field — sehingga bukan getter murni:
	 * ia bisa mengubah state object dan membuka koneksi database sendiri saat
	 * sesi Hibernate asalnya sudah tertutup.
	 * </p>
	 *
	 * @return item medis yang diterima, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan satuan item penerimaan pada baris ini.
	 *
	 * @param satuanItem satuan item penerimaan.
	 */
	public void setSatuanItem(SatuanItem satuanItem) {
		this.satuanItem = satuanItem;
	}

	/**
	 * Mengambil satuan item penerimaan pada baris ini — relasi OPSIONAL ke
	 * {@link SatuanItem}, yang menentukan arti angka {@link #getJumlah()}.
	 *
	 * <p>
	 * Satuan ini TIDAK dijamin sama dengan satuan pada baris PO di
	 * {@link PesananPembelianDetail#getSatuanItem()} — barang lazim dipesan
	 * dalam box tetapi diterima dan disimpan dalam satuan terkecil, atau
	 * sebaliknya. Konsekuensinya setiap perbandingan kuantitas antara tahap
	 * pemesanan dan tahap penerimaan HARUS didahului normalisasi lewat
	 * {@link KonversiSatuanItem}. Penjaga anti-lebih-terima yang membandingkan
	 * angka mentah tanpa menormalkan satuan bukan hanya tidak akurat, tetapi
	 * bisa salah arah: menolak penerimaan yang sah, atau meloloskan penerimaan
	 * yang berlipat-lipat dari yang dipesan.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getItem()}).
	 * </p>
	 *
	 * @return satuan item penerimaan, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_item", nullable = true)
	public SatuanItem getSatuanItem() {
		satuanItem = check(satuanItem);
		return satuanItem;
	}

	/**
	 * Menetapkan kuantitas yang diterima pada baris ini. Tidak ada penolakan
	 * nilai negatif maupun nol di level entitas, dan tidak ada pembandingan
	 * terhadap plafon pesanan — validasi tersebut harus dilakukan di lapisan
	 * action.
	 *
	 * @param jumlah kuantitas yang diterima, dinyatakan dalam
	 *               {@link #getSatuanItem()}.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil kuantitas yang diterima pada baris ini, dinyatakan dalam satuan
	 * {@link #getSatuanItem()} — angka yang akan menambah stok di gudang
	 * {@link PenerimaanOrder#getLokasi()} saat dokumen induk disetujui.
	 *
	 * <p>
	 * Ini adalah angka paling sensitif di seluruh klaster pengadaan medis, dan
	 * skema tidak memasang satu pun batas padanya. Tidak ada pembandingan
	 * terhadap {@link PesananPembelianDetail#getJumlah()} pada baris PO yang
	 * ditunjuk, tidak ada akumulasi terhadap penerimaan-penerimaan sebelumnya,
	 * dan tidak ada penolakan nilai negatif. Nilai negatif khususnya perlu
	 * diwaspadai: ia akan diterjemahkan menjadi mutasi stok bertanda menambah
	 * atas angka negatif, yaitu PENGURANGAN stok lewat dokumen yang secara
	 * bentuk adalah penerimaan barang — pengurangan persediaan tanpa jejak
	 * dokumen pengeluaran.
	 * </p>
	 * <p>
	 * Bentuk penjaga anti-lebih-terima yang memadai diuraikan pada Javadoc
	 * kelas ini. Nilainya di-default {@code 0.0} sehingga jarang {@code null},
	 * tetapi pemanggil tetap sebaiknya menangani {@code null} karena setter-nya
	 * menerima {@code null} tanpa keberatan.
	 * </p>
	 *
	 * @return kuantitas yang diterima, default {@code 0.0}.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan kuantitas bonus yang diterima pada baris ini.
	 *
	 * @param jumlahBonus kuantitas bonus.
	 */
	public void setJumlahBonus(Double jumlahBonus) {
		this.jumlahBonus = jumlahBonus;
	}

	/**
	 * Mengambil kuantitas BONUS yang diterima pada baris ini — barang yang
	 * datang dari vendor tanpa ditagihkan (mis. skema beli-10-gratis-1). Sama
	 * seperti {@link #getJumlah()}, angka ini menambah stok saat dokumen induk
	 * disetujui, tetapi secara nilai ia tidak menambah kewajiban pembayaran.
	 *
	 * <p>
	 * Keberadaan kolom ini menciptakan pertanyaan yang HARUS dijawab eksplisit
	 * oleh penjaga anti-lebih-terima: apakah bonus ikut dihitung terhadap
	 * plafon pesanan? Karena barang bonus secara bisnis memang tidak dipesan,
	 * memasukkannya ke perbandingan akan menolak penerimaan yang sah. Namun
	 * membiarkannya sepenuhnya tanpa batas membuka jalur menyelundupkan
	 * kelebihan penerimaan dengan cara mencatatnya sebagai bonus — kelebihan
	 * yang tetap menambah stok fisik namun luput dari setiap pemeriksaan
	 * terhadap pesanan. Jalan tengah yang lazim adalah membatasi bonus dengan
	 * aturannya sendiri (mis. proporsional terhadap {@link #getJumlah()}),
	 * bukan membiarkannya tak berbatas.
	 * </p>
	 * <p>
	 * Perlu dicatat pula bahwa bonus menambah kuantitas persediaan tanpa
	 * menambah nilai persediaan, sehingga harga rata-rata per unit item ikut
	 * turun. Perhitungan nilai persediaan yang mengabaikan kolom ini akan
	 * menghasilkan nilai per unit yang terlalu tinggi.
	 * </p>
	 *
	 * @return kuantitas bonus, default {@code 0.0}.
	 */
	@Column(name = "jumlah_bonus", nullable = true)
	public Double getJumlahBonus() {
		return jumlahBonus;
	}

	/**
	 * Menetapkan baris PO yang menjadi asal baris penerimaan ini.
	 *
	 * @param pesananPembelianDetail baris PO asal.
	 */
	public void setPesananPembelianDetail(PesananPembelianDetail pesananPembelianDetail) {
		this.pesananPembelianDetail = pesananPembelianDetail;
	}

	/**
	 * Mengambil baris {@link PesananPembelianDetail} yang menjadi asal baris
	 * penerimaan ini — tautan FK NYATA tingkat baris yang menghubungkan apa
	 * yang diterima dengan apa yang dipesan.
	 *
	 * <p>
	 * Tautan inilah bahan baku penjaga anti-lebih-terima: lewat baris PO yang
	 * ditunjuk di sini, plafon pemesanan
	 * ({@link PesananPembelianDetail#getJumlah()}) dapat dijangkau, dan seluruh
	 * penerimaan lain atas baris PO yang sama dapat dikumpulkan dengan query
	 * balik. Dengan kata lain skema TIDAK menghalangi penjaga tersebut ditulis
	 * — ia hanya tidak menyediakannya.
	 * </p>
	 * <p>
	 * Relasi ini OPSIONAL ({@code nullable = true}), berbeda dari tautan
	 * tingkat header {@link PenerimaanOrder#getPesananPembelian()} yang wajib.
	 * Ketidaksimetrisan ini berarti sebuah baris penerimaan boleh tidak
	 * menunjuk baris PO mana pun sekalipun dokumen induknya wajib menunjuk
	 * sebuah PO. Baris semacam itu menambah stok tanpa terhubung ke plafon
	 * pemesanan apa pun, sehingga LOLOS dari penjaga anti-lebih-terima yang
	 * bekerja lewat tautan ini — celah yang perlu ditutup dengan mewajibkan
	 * tautan ini terisi di lapisan action, bukan sekadar dengan memasang
	 * penjaga pada baris-baris yang kebetulan tertaut.
	 * </p>
	 *
	 * @return baris PO asal, atau {@code null} bila baris penerimaan ini tidak
	 *         ditautkan ke baris pesanan mana pun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pesanan_pembelian_detail", nullable = true)
	public PesananPembelianDetail getPesananPembelianDetail() {
		return pesananPembelianDetail;
	}

	/**
	 * Menetapkan dokumen penerimaan induk baris ini.
	 *
	 * @param penerimaanOrder dokumen penerimaan induk.
	 */
	public void setPenerimaanOrder(PenerimaanOrder penerimaanOrder) {
		this.penerimaanOrder = penerimaanOrder;
	}

	/**
	 * Mengambil dokumen {@link PenerimaanOrder} yang menjadi induk struktural
	 * baris ini. Lewat induk inilah baris ini memperoleh konteks yang tidak
	 * disimpannya sendiri: gudang tujuan
	 * ({@link PenerimaanOrder#getLokasi()}), status persetujuan yang menentukan
	 * apakah stok sudah bergerak, dan status pembatalan.
	 *
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}) walaupun secara bisnis wajib,
	 * sehingga baris detail yatim tetap sah secara skema. Baris yatim tidak
	 * hanya tak terlihat di layar, tetapi juga tidak punya lokasi maupun status
	 * — ia tidak akan pernah ikut bergerak saat dokumen mana pun disetujui atau
	 * dibatalkan, namun tetap terhitung bila ada agregasi yang menjumlahkan
	 * baris tanpa menyaring lewat induknya.
	 * </p>
	 *
	 * @return dokumen penerimaan induk, atau {@code null} bila baris ini yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_order", nullable = true)
	public PenerimaanOrder getPenerimaanOrder() {
		return penerimaanOrder;
	}

	/**
	 * Menetapkan tanggal kadaluarsa barang pada baris ini.
	 *
	 * @param tanggalKadaluarsa tanggal kadaluarsa.
	 */
	public void setTanggalKadaluarsa(Date tanggalKadaluarsa) {
		this.tanggalKadaluarsa = tanggalKadaluarsa;
	}

	/**
	 * Mengambil tanggal kadaluarsa barang yang diterima pada baris ini —
	 * dipetakan sebagai {@link TemporalType#DATE} (tanpa komponen jam), sesuai
	 * sifatnya sebagai tanggal kalender.
	 *
	 * <p>
	 * Kolom ini adalah satu-satunya tempat masa berlaku bahan medis tercatat
	 * pada saat barang masuk, sehingga menjadi dasar bagi pemantauan stok
	 * mendekati kadaluarsa. Relasinya OPSIONAL dan skema tidak memeriksa apa
	 * pun terhadapnya: tanggal kadaluarsa yang sudah lewat pada saat barang
	 * diterima akan tersimpan tanpa keluhan, begitu pula baris obat yang sama
	 * sekali tidak mengisinya. Untuk bahan medis, kedua kondisi itu bermakna
	 * risiko klinis, bukan sekadar data kurang rapi — validasinya perlu
	 * ditegakkan di lapisan action saat penerimaan disetujui.
	 * </p>
	 * <p>
	 * Perlu dicatat bahwa tanggal kadaluarsa melekat pada BARIS PENERIMAAN,
	 * bukan pada stok. Satu item medis yang diterima beberapa kali akan punya
	 * beberapa tanggal kadaluarsa berbeda yang tersebar di baris-baris
	 * penerimaan terpisah, tanpa satu pun kolom di sisi stok yang
	 * merangkumnya. Karena itu pemakaian dan transfer tidak bisa mengetahui
	 * batch mana yang keluar; kebijakan first-expired-first-out tidak dapat
	 * ditegakkan dari struktur data ini.
	 * </p>
	 *
	 * @return tanggal kadaluarsa barang, atau {@code null} bila tidak diisi.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_kadaluarsa", nullable = true)
	public Date getTanggalKadaluarsa() {
		return tanggalKadaluarsa;
	}

	/**
	 * Menetapkan harga beli satuan yang ditagihkan pada baris ini.
	 *
	 * @param hargaBeli harga beli per satuan.
	 */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * Mengambil harga beli satuan yang benar-benar ditagihkan vendor pada baris
	 * penerimaan ini — dasar nilai persediaan yang masuk. Berbeda dari
	 * {@link PesananPembelianDetail#getHargaBeli()} yang merupakan harga
	 * DISEPAKATI saat memesan; skema tidak memaksa keduanya sama, dan
	 * selisihnya justru merupakan informasi pengendalian yang berguna
	 * (kenaikan harga sepihak vendor) — bukan anomali yang perlu dicegah,
	 * melainkan yang perlu terlihat.
	 *
	 * @return harga beli per satuan, default {@code 0.0}.
	 */
	@Column(name = "harga_beli", nullable = true)
	public Double getHargaBeli() {
		return hargaBeli;
	}

	/**
	 * Menetapkan nilai diskon tingkat baris pada penerimaan ini.
	 *
	 * @param hargaDiskon nilai diskon baris.
	 */
	public void setHargaDiskon(Double hargaDiskon) {
		this.hargaDiskon = hargaDiskon;
	}

	/**
	 * Mengambil nilai diskon tingkat BARIS pada penerimaan ini. Perlu
	 * dibedakan dari {@link PenerimaanOrder#getDiskon()} yang merupakan diskon
	 * tingkat DOKUMEN. Keduanya bisa terisi bersamaan dan skema tidak
	 * mendefinisikan hubungan di antara keduanya, sehingga risiko diskon
	 * terhitung dua kali (sekali di baris, sekali di dokumen) nyata dan hanya
	 * bisa dicegah oleh kesepakatan di lapisan action.
	 *
	 * <p>
	 * Skema juga tidak menetapkan apakah angka ini berupa nominal atau
	 * persentase, dan tidak memeriksa apakah nilainya melebihi
	 * {@link #getHargaBeli()} — diskon yang lebih besar dari harga akan
	 * menghasilkan nilai persediaan negatif tanpa satu pun peringatan.
	 * </p>
	 *
	 * @return nilai diskon baris, default {@code 0.0}.
	 */
	@Column(name = "harga_diskon", nullable = true)
	public Double getHargaDiskon() {
		return hargaDiskon;
	}

	/**
	 * Menetapkan nilai pajak tingkat baris pada penerimaan ini.
	 *
	 * @param hargaPajak nilai pajak baris.
	 */
	public void setHargaPajak(Double hargaPajak) {
		this.hargaPajak = hargaPajak;
	}

	/**
	 * Mengambil nilai pajak tingkat BARIS pada penerimaan ini — pendamping
	 * {@link #getHargaDiskon()}, dan sama seperti diskon, berdampingan dengan
	 * padanannya di tingkat dokumen ({@link PenerimaanOrder#getPajak()}) tanpa
	 * hubungan yang didefinisikan skema. Nilai ini TERSIMPAN, tidak diturunkan
	 * ulang dari tarif {@link PenerimaanOrder#getJenisPajak()} saat dibaca,
	 * sehingga dokumen historis tidak akan berubah nilainya bila tarif master
	 * diubah kemudian.
	 *
	 * @return nilai pajak baris, default {@code 0.0}.
	 */
	@Column(name = "harga_pajak", nullable = true)
	public Double getHargaPajak() {
		return hargaPajak;
	}

}
