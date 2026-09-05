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
 * Entitas baris (detail) <b>Transfer Item</b> pada schema {@code sirs} (tabel
 * {@code transfer_item_detail}). Setiap baris mencatat satu {@link ItemMedis}
 * yang dipindahkan pada satu dokumen {@link TransferItem}, beserta kuantitas
 * yang DIKIRIM ({@link #getJumlah()}), kuantitas yang DITERIMA
 * ({@link #getJumlahDiterima()}) dan selisih di antara keduanya
 * ({@link #getSelisih()}).
 *
 * <h2>Satu-satunya baris detail berkuantitas ganda</h2>
 * <p>
 * Baris ini unik di seluruh klaster pengadaan dan inventaris {@code sirs}
 * karena menyimpan DUA kuantitas untuk satu item, mencerminkan sifat dua tahap
 * dokumen induknya: {@link #getJumlah()} berkurang dari
 * {@link TransferItem#getLokasi()} saat pengiriman disetujui, sedangkan
 * {@link #getJumlahDiterima()} bertambah di
 * {@link TransferItem#getLokasiTujuan()} saat penerimaan dicatat. Ketika
 * keduanya berbeda, selisihnya adalah barang yang keluar dari gudang asal namun
 * tidak pernah sampai di gudang tujuan.
 * </p>
 * <p>
 * Bandingkan dengan {@link PenerimaanOrderDetail} pada jalur pembelian, yang
 * menyimpan jumlah diterima saja dan menyerahkan pembandingan terhadap jumlah
 * dipesan pada tautan ke baris PO. Di sini kedua sisi berada pada BARIS YANG
 * SAMA, sehingga penjaga "diterima tidak melebihi dikirim" jauh lebih mudah
 * ditulis — cukup membandingkan dua kolom pada satu object, tanpa query balik
 * maupun agregasi. Kemudahan itu tidak dimanfaatkan: entitas tidak
 * membandingkan keduanya, dan tidak ada constraint database yang melakukannya.
 * </p>
 *
 * <h2>Selisih yang direkam tetapi tidak diperlakukan</h2>
 * <p>
 * {@link #getSelisih()} adalah kolom TERSIMPAN yang merekam beda antara yang
 * dikirim dan yang diterima. Ia tidak diturunkan ulang saat dibaca, tidak
 * dijamin konsisten dengan kedua kolom kuantitas, dan yang terpenting: skema
 * tidak menetapkan APA yang terjadi pada selisih tersebut. Barang yang tercatat
 * sebagai selisih tidak berada di gudang asal (sudah dikurangi) dan tidak
 * berada di gudang tujuan (tidak ditambahkan) — ia menghilang dari persediaan
 * tanpa dokumen susut, tanpa pembebanan, dan tanpa persetujuan tersendiri.
 * Perlakuan atas selisih ini adalah keputusan yang harus diambil lapisan
 * action; bila tidak diambil, transfer menjadi jalur penyusutan persediaan yang
 * tidak terkendali.
 * </p>
 *
 * <h2>Tidak ada rekaman stok sama sekali</h2>
 * <p>
 * Berbeda dari {@link PemakaianItemDetail},
 * {@link PemakaianReturItemDetail} dan {@link KoreksiItemMedisDetail} yang
 * masing-masing menyimpan potret {@code stok} dan {@code stokmenjadi}, baris
 * transfer ini TIDAK memiliki kolom stok apa pun. Akibatnya tidak ada jejak
 * berapa stok gudang asal pada saat transfer disusun, sehingga transfer yang
 * mengirim lebih banyak dari yang dimiliki tidak meninggalkan petunjuk apa pun
 * di dokumennya sendiri. Penjaga kecukupan stok untuk dokumen ini karena itu
 * harus sepenuhnya membaca stok terkini dari akumulasi mutasi pada saat
 * pengiriman disetujui.
 * </p>
 * <p>
 * Seluruh relasi baris ini OPSIONAL ({@code nullable = true}), termasuk
 * {@link #getTransferItem()}, sehingga baris detail yatim sah secara skema.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "transfer_item_detail")
public class TransferItemDetail extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris transfer ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris transfer ini. Nilai
	 * kosong/blank SENGAJA diabaikan (early return) agar field audit ini tidak
	 * pernah ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas baris transfer ini untuk tampilan/log, berupa
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
	 * Menetapkan nama pengguna yang mengubah baris transfer ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris transfer ini.
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
	private Double jumlahDiterima;
	private Double selisih;
	private TransferItem transferItem;
	private String keterangan;
	private Double hargaJual;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public TransferItemDetail() {
	}

	/**
	 * Primary key baris transfer ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik baris transfer ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris transfer ini.
	 *
	 * @param id ID baris transfer.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris transfer ini. Karena skema tidak punya
	 * kolom terstruktur untuk sebab selisih kirim-terima, teks bebas inilah
	 * satu-satunya tempat penjelasan selisih dapat dicatat.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris transfer ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan item medis yang dipindahkan pada baris ini.
	 *
	 * @param item item medis yang dipindahkan.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang dipindahkan pada baris ini — relasi OPSIONAL ke
	 * {@link ItemMedis}. Item yang sama berkurang di gudang asal dan bertambah
	 * di gudang tujuan; satu kolom ini melayani kedua sisi mutasi, sehingga
	 * tidak mungkin terjadi ketidakcocokan item antara sisi keluar dan sisi
	 * masuk — salah satu dari sedikit hal yang justru dijamin oleh struktur di
	 * klaster ini.
	 *
	 * <p>
	 * Getter ini memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy
	 * dan menugaskan hasilnya kembali ke field — sehingga bukan getter murni:
	 * ia bisa mengubah state object dan membuka koneksi database sendiri saat
	 * sesi Hibernate asalnya sudah tertutup.
	 * </p>
	 *
	 * @return item medis yang dipindahkan, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan satuan item transfer pada baris ini.
	 *
	 * @param satuanItem satuan item transfer.
	 */
	public void setSatuanItem(SatuanItem satuanItem) {
		this.satuanItem = satuanItem;
	}

	/**
	 * Mengambil satuan item transfer pada baris ini — relasi OPSIONAL ke
	 * {@link SatuanItem}, yang menentukan arti {@link #getJumlah()},
	 * {@link #getJumlahDiterima()} dan {@link #getSelisih()} sekaligus. Karena
	 * satu kolom satuan melayani ketiga kuantitas pada baris yang sama,
	 * perbandingan di antara ketiganya sah dilakukan langsung tanpa konversi —
	 * berbeda dari perbandingan lintas dokumen di jalur pembelian yang selalu
	 * memerlukan normalisasi lewat {@link KonversiSatuanItem}.
	 *
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getItem()}).
	 * </p>
	 *
	 * @return satuan item transfer, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_item", nullable = true)
	public SatuanItem getSatuanItem() {
		satuanItem = check(satuanItem);
		return satuanItem;
	}

	/**
	 * Menetapkan kuantitas yang DIKIRIM pada baris ini. Tidak ada penolakan
	 * nilai negatif maupun nol, dan tidak ada pemeriksaan kecukupan stok, di
	 * level entitas.
	 *
	 * @param jumlah kuantitas yang dikirim, dinyatakan dalam
	 *               {@link #getSatuanItem()}.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil kuantitas yang DIKIRIM pada baris ini — angka yang MENGURANGI
	 * stok {@link #getItem()} di gudang {@link TransferItem#getLokasi()} saat
	 * pengiriman disetujui.
	 *
	 * <p>
	 * Tidak ada pemeriksaan bahwa stok gudang asal mencukupi angka ini.
	 * Berbeda dari dokumen pemakaian dan koreksi yang setidaknya merekam
	 * potret stok pada barisnya, baris transfer tidak menyimpan jejak stok apa
	 * pun, sehingga pengiriman yang melampaui kepemilikan tidak meninggalkan
	 * petunjuk di dokumennya sendiri dan hanya terlihat sebagai stok negatif
	 * pada perhitungan akumulasi mutasi.
	 * </p>
	 * <p>
	 * Nilai NEGATIF perlu diwaspadai: ia akan menjadi pengurangan atas angka
	 * negatif, yaitu PENAMBAHAN stok di gudang asal lewat dokumen yang
	 * bentuknya pengiriman keluar. Nilainya bisa {@code null} karena tidak
	 * di-default; pemanggil yang memakainya untuk mutasi stok WAJIB menangani
	 * {@code null} agar tidak melempar {@link NullPointerException}.
	 * </p>
	 *
	 * @return kuantitas yang dikirim, atau {@code null} bila belum diisi.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan dokumen transfer induk baris ini.
	 *
	 * @param transferItem dokumen transfer induk.
	 */
	public void setTransferItem(TransferItem transferItem) {
		this.transferItem = transferItem;
	}

	/**
	 * Mengambil dokumen {@link TransferItem} yang menjadi induk struktural
	 * baris ini. Lewat induk inilah baris ini memperoleh konteks yang tidak
	 * disimpannya sendiri: gudang asal dan gudang tujuan, serta kedua tahap
	 * pengesahannya (pengiriman dan penerimaan) yang menentukan kapan
	 * masing-masing kuantitas pada baris ini berdampak pada stok.
	 *
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}) walaupun secara bisnis wajib,
	 * sehingga baris detail yatim tetap sah secara skema. Baris yatim pada
	 * dokumen transfer kehilangan KEDUA lokasinya sekaligus, sehingga tidak
	 * dapat diterjemahkan menjadi mutasi apa pun.
	 * </p>
	 *
	 * @return dokumen transfer induk, atau {@code null} bila baris ini yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transfer_item", nullable = true)
	public TransferItem getTransferItem() {
		return transferItem;
	}

	/**
	 * Menetapkan harga jual satuan item pada baris transfer ini.
	 *
	 * @param hargaJual harga jual per satuan.
	 */
	public void setHargaJual(Double hargaJual) {
		this.hargaJual = hargaJual;
	}

	/**
	 * Mengambil harga JUAL satuan item pada baris transfer ini — perhatikan
	 * bahwa kolom ini memakai harga jual, berbeda dari baris-baris lain di
	 * klaster pengadaan yang memakai harga BELI
	 * ({@link PesananPembelianDetail#getHargaBeli()},
	 * {@link PenerimaanOrderDetail#getHargaBeli()},
	 * {@link ProduksiDetail#getHargaBeli()}).
	 *
	 * <p>
	 * Perbedaan ini bermakna: perpindahan barang antar gudang di dalam
	 * organisasi yang sama pada dasarnya tidak menimbulkan laba, sehingga
	 * menilainya pada harga jual akan menaikkan nilai persediaan di gudang
	 * tujuan di atas biaya perolehannya. Bila nilai pada kolom ini benar-benar
	 * dipakai sebagai dasar penilaian persediaan penerima, laba akan tercipta
	 * hanya karena barang berpindah tempat. Kode yang menghitung nilai
	 * persediaan lintas gudang perlu menyadari asal-usul angka ini dan tidak
	 * memperlakukannya setara dengan harga beli pada dokumen lain.
	 * </p>
	 *
	 * @return harga jual per satuan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "harga_jual", nullable = true)
	public Double getHargaJual() {
		return hargaJual;
	}

	/**
	 * Menetapkan kuantitas yang DITERIMA di gudang tujuan pada baris ini.
	 * Tidak ada pembandingan terhadap {@link #getJumlah()} di level entitas.
	 *
	 * @param jumlahDiterima kuantitas yang diterima.
	 */
	public void setJumlahDiterima(Double jumlahDiterima) {
		this.jumlahDiterima = jumlahDiterima;
	}

	/**
	 * Mengambil kuantitas yang DITERIMA di gudang tujuan pada baris ini —
	 * angka yang MENAMBAH stok {@link #getItem()} di
	 * {@link TransferItem#getLokasiTujuan()} saat penerimaan dicatat.
	 *
	 * <p>
	 * Inilah kolom yang menjadikan transfer satu-satunya dokumen di klaster ini
	 * yang menyimpan kedua sisi mutasinya pada baris yang sama. Karena
	 * {@link #getJumlah()} yang dikirim berada tepat di sebelahnya dan memakai
	 * satuan yang sama, penjaga "diterima tidak melebihi dikirim" cukup berupa
	 * satu perbandingan sederhana pada object yang sama — tanpa query balik,
	 * tanpa agregasi lintas dokumen, tanpa konversi satuan. Meski demikian
	 * skema tidak melakukan perbandingan itu, dan tidak ada constraint database
	 * yang menahannya.
	 * </p>
	 * <p>
	 * Menerima lebih banyak daripada yang dikirim adalah keadaan yang secara
	 * fisik mustahil namun secara skema sepenuhnya sah, dan akibatnya adalah
	 * penciptaan stok bersih: gudang tujuan bertambah lebih besar daripada
	 * pengurangan gudang asal. Nilai NEGATIF di sini menghasilkan kebalikannya,
	 * yaitu pengurangan stok di gudang tujuan lewat dokumen yang bentuknya
	 * penerimaan.
	 * </p>
	 * <p>
	 * Nilai {@code null} pada kolom ini bermakna berbeda dari nol dan perlu
	 * dibedakan dengan hati-hati: {@code null} umumnya berarti penerimaan belum
	 * dicatat sama sekali (barang masih dalam perjalanan), sedangkan nol
	 * berarti penerimaan sudah dicatat namun tidak ada satu pun barang yang
	 * sampai. Kode yang menganggap {@code null} sebagai nol akan mencatat
	 * seluruh barang dalam perjalanan sebagai barang yang hilang.
	 * </p>
	 *
	 * @return kuantitas yang diterima, atau {@code null} bila penerimaan belum
	 *         dicatat.
	 */
	@Column(name = "jumlah_diterima", nullable = true)
	public Double getJumlahDiterima() {
		return jumlahDiterima;
	}

	/**
	 * Menetapkan selisih antara kuantitas dikirim dan diterima pada baris ini.
	 *
	 * @param selisih selisih kirim-terima.
	 */
	public void setSelisih(Double selisih) {
		this.selisih = selisih;
	}

	/**
	 * Mengambil selisih antara kuantitas yang dikirim
	 * ({@link #getJumlah()}) dan yang diterima
	 * ({@link #getJumlahDiterima()}) — nilai TERSIMPAN yang dituliskan lapisan
	 * action, bukan hasil pengurangan yang dihitung ulang saat dibaca.
	 *
	 * <p>
	 * Karena tersimpan terpisah, angka ini tidak dijamin konsisten dengan kedua
	 * kuantitas di sebelahnya: mengubah salah satu dari keduanya tidak
	 * menyesuaikan selisih di sini, sehingga baris dapat menampilkan tiga angka
	 * yang tidak saling cocok tanpa satu pun mekanisme yang mengeluh. Kode yang
	 * memerlukan selisih untuk perhitungan sebaiknya menurunkannya sendiri dari
	 * kedua kuantitas, dan memperlakukan kolom ini sebagai catatan saja.
	 * </p>
	 * <p>
	 * Yang lebih penting: skema TIDAK menetapkan apa yang terjadi pada barang
	 * yang tercatat sebagai selisih. Barang tersebut sudah dikurangkan dari
	 * gudang asal namun tidak ditambahkan ke gudang tujuan, sehingga ia
	 * menghilang dari persediaan tanpa dokumen susut, tanpa pembebanan biaya,
	 * dan tanpa persetujuan tersendiri. Selisih transfer karena itu merupakan
	 * jalur penyusutan persediaan yang, bila tidak diberi perlakuan eksplisit
	 * di lapisan action, tidak terkendali dan tidak terlihat pada pelaporan
	 * susut mana pun.
	 * </p>
	 *
	 * @return selisih kirim-terima, atau {@code null} bila tidak direkam.
	 */
	@Column(name = "selisih", nullable = true)
	public Double getSelisih() {
		return selisih;
	}

}
