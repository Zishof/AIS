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
 * Entitas baris (detail) <b>Saldo Awal Medis</b> pada schema {@code sirs}
 * (tabel {@code saldo_awal_detail_medis} — perhatikan urutan katanya, yang
 * TIDAK mengikuti pola {@code <dokumen>_detail} seperti tabel detail lain di
 * paket ini). Setiap baris menyatakan stok awal
 * satu {@link ItemMedis} pada satu dokumen {@link SaldoAwalMedis}: berapa
 * banyak ({@link #getJumlah()}), dalam satuan apa
 * ({@link #getSatuanItem()}), bernilai berapa ({@link #getHarga()}), dan sampai
 * kapan layak pakai ({@link #getTanggalKadaluarsa()}).
 *
 * <h2>Baris yang menetapkan titik nol persediaan</h2>
 * <p>
 * Saat dokumen induknya disetujui, {@link #getJumlah()} pada baris ini menjadi
 * stok pembuka {@link #getItem()} di gudang
 * {@link SaldoAwalMedis#getLokasi()}. Tidak ada dokumen sebelumnya yang menjadi
 * dasarnya — angka di sini adalah pernyataan, bukan hasil perhitungan.
 * Konsekuensinya seluruh perhitungan stok di kemudian hari mewarisi kebenaran
 * atau kekeliruan angka ini: sebuah baris saldo awal yang salah tidak akan
 * pernah terkoreksi dengan sendirinya oleh transaksi berikutnya, ia hanya akan
 * bergeser bersama seluruh mutasi di atasnya.
 * </p>
 *
 * <h2>Bentuknya paling menyerupai baris penerimaan barang</h2>
 * <p>
 * Di antara seluruh baris detail di klaster inventaris {@code sirs}, baris ini
 * paling mirip {@link PenerimaanOrderDetail}: sama-sama punya
 * {@code satuanItem} DAN {@code tanggalKadaluarsa} — dua kolom yang tidak
 * dimiliki baris pemakaian, retur pemakaian, koreksi, maupun produksi.
 * Kemiripan itu masuk akal, karena keduanya sama-sama merupakan pintu MASUK
 * barang ke dalam persediaan, dan pintu masuklah satu-satunya tempat masa
 * berlaku bahan medis dapat dicatat.
 * </p>
 * <p>
 * Yang membedakan keduanya adalah adanya pembanding: baris penerimaan menunjuk
 * baris pesanan lewat
 * {@link PenerimaanOrderDetail#getPesananPembelianDetail()}, sedangkan baris
 * ini tidak menunjuk apa pun dan memang tidak bisa. Karena itu tidak ada, dan
 * tidak mungkin ada, penjaga kuantitas untuk baris saldo awal — pengendalian
 * satu-satunya yang tersisa adalah pemisahan wewenang pada dokumen induknya
 * dan pembatasan agar saldo awal tidak ditetapkan berulang untuk lokasi dan
 * item yang sama.
 * </p>
 * <p>
 * Seluruh relasi baris ini OPSIONAL ({@code nullable = true}), termasuk
 * {@link #getSaldoAwal()}, sehingga baris detail yatim sah secara skema.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "saldo_awal_detail_medis")
public class SaldoAwalMedisDetail extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris saldo awal ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris saldo awal ini. Nilai
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
	 * Representasi ringkas baris saldo awal ini untuk tampilan/log, berupa
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
	 * Menetapkan nama pengguna yang mengubah baris saldo awal ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris saldo awal ini.
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
	private Date tanggalKadaluarsa;
	private Double jumlah;
	private Double harga;
	private SaldoAwalMedis saldoAwal;
	private String keterangan;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public SaldoAwalMedisDetail() {
	}

	/**
	 * Primary key baris saldo awal ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik baris saldo awal ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris saldo awal ini.
	 *
	 * @param id ID baris saldo awal.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris saldo awal ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris saldo awal ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan item medis yang ditetapkan saldo awalnya pada baris ini.
	 *
	 * @param item item medis saldo awal.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang ditetapkan saldo awalnya pada baris ini —
	 * relasi OPSIONAL ke {@link ItemMedis}. Inilah item yang memperoleh stok
	 * pembuka di gudang {@link SaldoAwalMedis#getLokasi()} saat dokumen
	 * disetujui.
	 *
	 * <p>
	 * Skema tidak mencegah item yang sama muncul berkali-kali dalam satu
	 * dokumen saldo awal, maupun muncul lagi di dokumen saldo awal berikutnya
	 * untuk lokasi yang sama. Kedua kondisi itu akan menjumlahkan stok pembuka
	 * berlipat tanpa satu pun mekanisme yang mempertanyakannya, dan karena
	 * saldo awal tidak punya dokumen pembanding, kelebihan tersebut tidak akan
	 * pernah terdeteksi lewat rekonsiliasi apa pun selain penghitungan fisik.
	 * </p>
	 *
	 * @return item medis saldo awal, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		return item;
	}

	/**
	 * Menetapkan satuan item pada baris saldo awal ini.
	 *
	 * @param satuanItem satuan item saldo awal.
	 */
	public void setSatuanItem(SatuanItem satuanItem) {
		this.satuanItem = satuanItem;
	}

	/**
	 * Mengambil satuan item pada baris saldo awal ini — relasi OPSIONAL ke
	 * {@link SatuanItem}, yang menentukan arti angka {@link #getJumlah()}.
	 *
	 * <p>
	 * Kolom ini termasuk yang membedakan baris saldo awal dari baris
	 * pemakaian, retur pemakaian, koreksi dan produksi — yang seluruhnya tidak
	 * punya kolom satuan dan karenanya selalu mengacu pada satuan dasar item.
	 * Karena baris ini BOLEH memakai satuan lain, angka stok pembuka di sini
	 * tidak dapat dijumlahkan langsung dengan mutasi-mutasi berikutnya tanpa
	 * lebih dulu dinormalkan lewat {@link KonversiSatuanItem}. Saldo awal yang
	 * dinyatakan dalam box sementara pemakaiannya dicatat dalam tablet akan
	 * menghasilkan perhitungan stok yang keliru berlipat-lipat bila
	 * normalisasi itu terlewat — dan karena kekeliruannya ada di titik nol,
	 * seluruh riwayat stok item tersebut ikut salah.
	 * </p>
	 *
	 * @return satuan item saldo awal, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "satuan_item", nullable = true)
	public SatuanItem getSatuanItem() {
		return satuanItem;
	}

	/**
	 * Menetapkan jumlah stok awal pada baris ini. Tidak ada penolakan nilai
	 * negatif maupun nol di level entitas.
	 *
	 * @param jumlah jumlah stok awal, dinyatakan dalam
	 *               {@link #getSatuanItem()}.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil jumlah stok awal pada baris ini, dinyatakan dalam satuan
	 * {@link #getSatuanItem()} — angka yang menjadi stok pembuka
	 * {@link #getItem()} di gudang {@link SaldoAwalMedis#getLokasi()} saat
	 * dokumen disetujui.
	 *
	 * <p>
	 * Angka ini adalah PERNYATAAN, bukan hasil perhitungan dari dokumen mana
	 * pun, sehingga tidak ada nilai apa pun yang dapat dipakai untuk
	 * memvalidasinya. Tidak ada penjaga kuantitas di sini, dan berbeda dari
	 * baris penerimaan atau baris retur, di sini penjaga semacam itu memang
	 * tidak mungkin dibuat: tidak ada dokumen sebelumnya untuk dibandingkan.
	 * Pengendalian yang tersisa sepenuhnya bersifat organisatoris —
	 * pemisahan wewenang pembuat dan penyetuju pada
	 * {@link SaldoAwalMedis}, serta pembatasan agar saldo awal tidak ditetapkan
	 * lebih dari sekali untuk item dan lokasi yang sama.
	 * </p>
	 * <p>
	 * Nilai NEGATIF tersimpan tanpa keberatan dan akan menghasilkan stok
	 * pembuka negatif, yaitu keadaan yang menyatakan gudang berutang barang
	 * sejak sebelum transaksi pertama. Nilai nol juga diterima. Nilainya bisa
	 * {@code null} karena tidak di-default; pemanggil yang memakainya untuk
	 * mutasi stok WAJIB menangani {@code null} agar tidak melempar
	 * {@link NullPointerException} saat auto-unboxing.
	 * </p>
	 *
	 * @return jumlah stok awal, atau {@code null} bila belum diisi.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan dokumen saldo awal induk baris ini.
	 *
	 * @param saldoAwal dokumen saldo awal induk.
	 */
	public void setSaldoAwal(SaldoAwalMedis saldoAwal) {
		this.saldoAwal = saldoAwal;
	}

	/**
	 * Mengambil dokumen {@link SaldoAwalMedis} yang menjadi induk struktural
	 * baris ini, dipetakan ke kolom {@code saldo_awal}. Lewat induk inilah
	 * baris ini memperoleh gudang yang stoknya ditetapkan dan status
	 * persetujuan yang menentukan apakah stok pembuka sudah tertulis.
	 *
	 * <p>
	 * Perhatikan ketidakselarasan penamaan: nama property JavaBean dan nama
	 * kolomnya memakai bentuk pendek {@code saldoAwal}/{@code saldo_awal},
	 * sedangkan kelas dan tabel yang ditunjuknya bernama lengkap
	 * {@link SaldoAwalMedis}/{@code sirs.saldo_awal_medis}. Kode SQL mentah
	 * yang mengira nama tabelnya {@code saldo_awal} akan menyasar tabel yang
	 * keliru atau gagal, tergantung apa yang ada di schema yang aktif.
	 * </p>
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}) walaupun secara bisnis wajib,
	 * sehingga baris detail yatim tetap sah secara skema dan tidak akan pernah
	 * ikut bergerak saat dokumen mana pun disetujui.
	 * </p>
	 *
	 * @return dokumen saldo awal induk, atau {@code null} bila baris ini yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "saldo_awal", nullable = true)
	public SaldoAwalMedis getSaldoAwal() {
		return saldoAwal;
	}

	/**
	 * Menetapkan harga satuan item pada baris saldo awal ini.
	 *
	 * @param harga harga satuan item.
	 */
	public void setHarga(Double harga) {
		this.harga = harga;
	}

	/**
	 * Mengambil harga satuan item pada baris saldo awal ini — dasar penilaian
	 * rupiah dari persediaan pembuka, dan karenanya titik awal nilai persediaan
	 * yang diwarisi seluruh perhitungan berikutnya.
	 *
	 * <p>
	 * Sama seperti kuantitasnya, angka ini adalah pernyataan tanpa pembanding:
	 * tidak ada dokumen pembelian yang menjadi dasarnya dan tidak ada
	 * pemeriksaan kewajaran terhadapnya. Nilai persediaan awal seluruh gudang
	 * karena itu sepenuhnya ditentukan oleh angka-angka yang diketik pada saat
	 * dokumen saldo awal disusun.
	 * </p>
	 *
	 * @return harga satuan item, atau {@code null} bila belum diisi.
	 */
	public Double getHarga() {
		return harga;
	}

	/**
	 * Menetapkan tanggal kadaluarsa barang pada baris saldo awal ini.
	 *
	 * @param tanggalKadaluarsa tanggal kadaluarsa.
	 */
	public void setTanggalKadaluarsa(Date tanggalKadaluarsa) {
		this.tanggalKadaluarsa = tanggalKadaluarsa;
	}

	/**
	 * Mengambil tanggal kadaluarsa barang pada baris saldo awal ini —
	 * dipetakan sebagai {@link TemporalType#DATE} (tanpa komponen jam), sesuai
	 * sifatnya sebagai tanggal kalender.
	 *
	 * <p>
	 * Bersama {@link PenerimaanOrderDetail#getTanggalKadaluarsa()}, kolom
	 * inilah satu-satunya tempat masa berlaku bahan medis tercatat di klaster
	 * inventaris ini — keduanya berada di pintu MASUK barang, dan tidak ada
	 * padanannya pada dokumen pemakaian, transfer, koreksi maupun produksi.
	 * Untuk stok yang sudah ada sebelum sistem dipakai, kolom di sinilah
	 * satu-satunya kesempatan mencatat masa berlakunya; bila dilewatkan, masa
	 * berlaku stok lama tidak akan pernah dapat dipulihkan dari data.
	 * </p>
	 * <p>
	 * Relasinya OPSIONAL dan skema tidak memeriksa apa pun terhadapnya:
	 * tanggal yang sudah lewat pada saat saldo awal ditetapkan akan tersimpan
	 * tanpa keluhan, begitu pula baris obat yang mengosongkannya. Perlu dicatat
	 * pula bahwa tanggal kadaluarsa melekat pada BARIS, bukan pada stok,
	 * sehingga satu item dengan beberapa masa berlaku memerlukan beberapa baris
	 * — dan setelah itu tidak ada mekanisme yang menghubungkan pemakaian
	 * berikutnya dengan baris mana yang keluar, sehingga kebijakan
	 * first-expired-first-out tidak dapat ditegakkan dari struktur data ini.
	 * </p>
	 *
	 * @return tanggal kadaluarsa barang, atau {@code null} bila tidak diisi.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_kadaluarsa", nullable = true)
	public Date getTanggalKadaluarsa() {
		return tanggalKadaluarsa;
	}

}
