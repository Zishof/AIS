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
 * Entitas baris (detail) <b>Pemakaian Item</b> medis pada schema {@code sirs}
 * (tabel {@code pemakaian_item_detail}). Setiap baris mencatat satu
 * {@link ItemMedis} yang dikeluarkan untuk dipakai pada satu dokumen
 * {@link PemakaianItem}, beserta kuantitas ({@link #getJumlah()}), nilainya
 * ({@link #getHarga()}), dan potret stok sebelum
 * ({@link #getStok()}) serta sesudahnya ({@link #getStokmenjadi()}).
 *
 * <h2>Baris ini mengeluarkan barang dari persediaan secara permanen</h2>
 * <p>
 * Saat dokumen induknya disetujui, {@link #getJumlah()} pada baris ini
 * MENGURANGI stok {@link #getItem()} di gudang
 * {@link PemakaianItem#getLokasi()}, dan barangnya tidak bertambah di mana pun
 * — nilainya menjadi beban. Pasangan kebalikannya adalah
 * {@link PemakaianReturItemDetail}, yang mengembalikan barang yang terlanjur
 * dikeluarkan namun tidak jadi dipakai.
 * </p>
 * <p>
 * Kedua entitas detail itu <b>identik struktur kolomnya</b>: sama-sama punya
 * {@code item}, {@code jumlah}, {@code harga}, {@code keterangan},
 * {@code stok} dan {@code stokmenjadi}, dan hanya berbeda pada nama relasi
 * induk ({@code pemakaianItem} di sini, {@code pemakaianReturItem} di sana)
 * serta pada arah mutasi stoknya. Kemiripan sedekat ini menuntut kehati-hatian
 * saat memelihara kode yang menanganinya: sepotong logika yang disalin dari
 * satu sisi ke sisi lain akan tetap dapat dikompilasi dan tetap berjalan
 * meskipun nama tabel, nama kolom induk, atau arah stoknya lupa disesuaikan —
 * dan akibatnya berupa mutasi stok pada dokumen yang keliru atau ke arah yang
 * terbalik, tanpa satu pun galat yang terlihat.
 * </p>
 *
 * <h2>{@code stok} dan {@code stokmenjadi} adalah REKAMAN, bukan penjaga</h2>
 * <p>
 * Sama seperti pada {@link KoreksiItemMedisDetail}, kedua kolom itu menyimpan
 * potret angka stok pada saat baris disusun. Keduanya BUKAN sumber kebenaran
 * stok — stok sesungguhnya dihitung dari akumulasi seluruh mutasi — dan bukan
 * pula mekanisme penjagaan:
 * </p>
 * <ul>
 *   <li>potret bisa BASI, karena transaksi lain di gudang yang sama dapat
 *       mengubah stok antara saat baris disusun dan saat dokumen disetujui;</li>
 *   <li>nilai {@link #getStokmenjadi()} yang NEGATIF tersimpan begitu saja —
 *       skema tidak menolaknya, sehingga baris yang membuat stok jatuh di
 *       bawah nol tetap dapat disetujui dan tetap menuliskan mutasinya.</li>
 * </ul>
 * <p>
 * Keberadaan kedua kolom ini karena itu TIDAK boleh disalahartikan sebagai
 * adanya penjaga kecukupan stok. Penjaga yang sesungguhnya harus membaca stok
 * TERKINI pada saat dokumen DISETUJUI, dan menolak pemakaian yang melampaui
 * stok tersedia.
 * </p>
 * <p>
 * Entitas ini tidak memiliki kolom {@code satuanItem}, sehingga
 * {@link #getJumlah()} hanya bermakna bila disepakati mengacu pada satuan dasar
 * item. Seluruh relasinya OPSIONAL ({@code nullable = true}), termasuk
 * {@link #getPemakaianItem()}, sehingga baris detail yatim sah secara skema.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "pemakaian_item_detail")
public class PemakaianItemDetail extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris pemakaian ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris pemakaian ini. Nilai
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
	 * Representasi ringkas baris pemakaian ini untuk tampilan/log, berupa
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
	 * Menetapkan nama pengguna yang mengubah baris pemakaian ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris pemakaian ini.
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
	private Double jumlah;
	private Double harga;
	private PemakaianItem pemakaianItem;
	private String keterangan;
	private Double stok;
	private Double stokmenjadi;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PemakaianItemDetail() {
	}

	/**
	 * Primary key baris pemakaian ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik baris pemakaian ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris pemakaian ini.
	 *
	 * @param id ID baris pemakaian.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris pemakaian ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris pemakaian ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan item medis yang dipakai pada baris ini.
	 *
	 * @param item item medis yang dipakai.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang dipakai pada baris ini — relasi OPSIONAL ke
	 * {@link ItemMedis}. Inilah item yang stoknya BERKURANG secara permanen di
	 * gudang {@link PemakaianItem#getLokasi()} saat dokumen disetujui.
	 *
	 * <p>
	 * Getter ini memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy
	 * dan menugaskan hasilnya kembali ke field — sehingga bukan getter murni:
	 * ia bisa mengubah state object dan membuka koneksi database sendiri saat
	 * sesi Hibernate asalnya sudah tertutup.
	 * </p>
	 *
	 * @return item medis yang dipakai, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan kuantitas yang dipakai pada baris ini. Tidak ada penolakan
	 * nilai negatif maupun nol, dan tidak ada pemeriksaan kecukupan stok, di
	 * level entitas.
	 *
	 * @param jumlah kuantitas yang dipakai.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil kuantitas yang dipakai pada baris ini — angka yang MENGURANGI
	 * stok {@link #getItem()} di gudang {@link PemakaianItem#getLokasi()} saat
	 * dokumen induk disetujui, secara permanen.
	 *
	 * <p>
	 * Skema tidak memasang batas apa pun pada angka ini. Tidak ada pemeriksaan
	 * bahwa stok tersedia mencukupi — kolom {@link #getStokmenjadi()} yang
	 * merekam hasil perhitungannya tidak menolak nilai negatif, sehingga
	 * keberadaannya tidak boleh dianggap sebagai penjaga. Tidak ada pula
	 * penolakan nilai nol, sehingga baris pemakaian berkuantitas nol dapat ikut
	 * tersimpan dan ikut diproses tanpa menghasilkan apa pun.
	 * </p>
	 * <p>
	 * Nilai NEGATIF perlu diwaspadai secara khusus: ia akan menjadi pengurangan
	 * atas angka negatif, yaitu PENAMBAHAN stok lewat dokumen yang bentuknya
	 * pemakaian barang — penciptaan persediaan tanpa jejak dokumen penerimaan,
	 * dan sekaligus beban negatif pada pembukuan. Karena entitas ini tidak
	 * punya kolom satuan, angka ini hanya bermakna bila disepakati mengacu pada
	 * satuan dasar item.
	 * </p>
	 * <p>
	 * Nilainya bisa {@code null} karena tidak di-default; pemanggil yang
	 * memakainya untuk mutasi stok WAJIB menangani {@code null} agar tidak
	 * melempar {@link NullPointerException} saat auto-unboxing.
	 * </p>
	 *
	 * @return kuantitas yang dipakai, atau {@code null} bila belum diisi.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan dokumen pemakaian induk baris ini.
	 *
	 * @param pemakaianItem dokumen pemakaian induk.
	 */
	public void setPemakaianItem(PemakaianItem pemakaianItem) {
		this.pemakaianItem = pemakaianItem;
	}

	/**
	 * Mengambil dokumen {@link PemakaianItem} yang menjadi induk struktural
	 * baris ini, dipetakan ke kolom {@code pemakaian_item}. Lewat induk inilah
	 * baris ini memperoleh gudang yang stoknya berkurang dan status persetujuan
	 * yang menentukan apakah pengeluaran sudah terjadi.
	 *
	 * <p>
	 * Perlu diperhatikan bahwa nama kolom {@code pemakaian_item} di sini
	 * berbeda hanya satu kata dari {@code pemakaian_retur_item} milik
	 * {@link PemakaianReturItemDetail}, dan nama tabelnya
	 * ({@code sirs.pemakaian_item_detail}) berbeda hanya satu kata dari
	 * {@code sirs.pemakaian_retur_item_detail}. Kode SQL mentah yang menyebut
	 * pasangan nama tabel dan nama kolom milik jenis dokumen yang keliru TIDAK
	 * akan gagal dengan jelas: karena PK kedua tabel berjalan pada sequence
	 * masing-masing, ID dokumen dari satu jenis dapat kebetulan cocok dengan
	 * ID dokumen jenis lainnya, sehingga query akan diam-diam mengenai baris
	 * milik dokumen yang sama sekali berbeda.
	 * </p>
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}) walaupun secara bisnis wajib,
	 * sehingga baris detail yatim tetap sah secara skema dan tidak akan pernah
	 * ikut bergerak saat dokumen mana pun disetujui.
	 * </p>
	 *
	 * @return dokumen pemakaian induk, atau {@code null} bila baris ini yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemakaian_item", nullable = true)
	public PemakaianItem getPemakaianItem() {
		return pemakaianItem;
	}

	/**
	 * Menetapkan harga satuan item pada baris pemakaian ini.
	 *
	 * @param harga harga satuan item.
	 */
	public void setHarga(Double harga) {
		this.harga = harga;
	}

	/**
	 * Mengambil harga satuan item pada baris pemakaian ini — dasar penilaian
	 * rupiah dari barang yang dikeluarkan, dan karenanya dasar besarnya BEBAN
	 * yang timbul dari pemakaian ini.
	 *
	 * <p>
	 * Skema tidak menautkan angka ini ke harga beli historis item yang
	 * bersangkutan maupun ke baris penerimaan yang dulu memasukkannya, dan
	 * tidak memeriksa kewajarannya. Nilai persediaan yang keluar karena itu
	 * sepenuhnya ditentukan oleh angka yang diisikan pada saat dokumen dibuat,
	 * dan tidak mengikuti metode penilaian persediaan tertentu.
	 * </p>
	 *
	 * @return harga satuan item, atau {@code null} bila belum diisi.
	 */
	public Double getHarga() {
		return harga;
	}

	/**
	 * Menetapkan potret stok sebelum pemakaian pada baris ini.
	 *
	 * @param stok angka stok sebelum pemakaian.
	 */
	public void setStok(Double stok) {
		this.stok = stok;
	}

	/**
	 * Mengambil POTRET stok item ini di gudang yang bersangkutan pada saat
	 * baris pemakaian disusun — sebuah rekaman keadaan untuk membantu pembaca
	 * dokumen, BUKAN sumber kebenaran stok. Stok yang sesungguhnya selalu
	 * dihitung dari akumulasi seluruh mutasi.
	 *
	 * <p>
	 * Sifatnya sebagai potret berarti angka ini BISA BASI: transaksi lain di
	 * gudang yang sama antara saat baris disusun dan saat dokumen disetujui
	 * akan mengubah stok sesungguhnya tanpa mengubah angka di sini. Karena
	 * {@link #getStokmenjadi()} diturunkan dari angka ini, keduanya bisa
	 * sama-sama tidak menggambarkan keadaan yang benar-benar terjadi. Kode yang
	 * memakai angka ini untuk perhitungan apa pun — bukan sekadar untuk
	 * ditampilkan — perlu membaca ulang stok terkini alih-alih memercayai
	 * potret ini.
	 * </p>
	 *
	 * @return potret stok sebelum pemakaian, atau {@code null} bila tidak
	 *         direkam.
	 */
	public Double getStok() {
		return stok;
	}

	/**
	 * Menetapkan potret stok setelah pemakaian pada baris ini.
	 *
	 * @param stokmenjadi angka stok setelah pemakaian.
	 */
	public void setStokmenjadi(Double stokmenjadi) {
		this.stokmenjadi = stokmenjadi;
	}

	/**
	 * Mengambil POTRET stok yang diperkirakan setelah pemakaian ini diterapkan
	 * — secara logis merupakan {@link #getStok()} dikurangi
	 * {@link #getJumlah()}. Sama seperti {@link #getStok()}, ini nilai
	 * TERSIMPAN yang dihitung sekali saat baris disusun dan tidak diturunkan
	 * ulang saat dibaca.
	 *
	 * <p>
	 * PENTING: keberadaan kolom ini TIDAK boleh disalahartikan sebagai adanya
	 * penjaga kecukupan stok. Kolom ini merekam hasil perhitungan, ia tidak
	 * menolak apa pun. Nilai NEGATIF di sini tersimpan begitu saja — skema
	 * tidak mengeluh dan tidak ada constraint yang menahannya. Baris pemakaian
	 * yang membuat stok jatuh di bawah nol akan tetap tersimpan, tetap dapat
	 * disetujui, dan tetap menuliskan mutasi stoknya.
	 * </p>
	 * <p>
	 * Penjaga kecukupan stok yang sesungguhnya harus (a) membaca stok TERKINI,
	 * bukan potret {@link #getStok()} yang bisa basi, (b) berjalan pada saat
	 * dokumen DISETUJUI, karena itulah titik ketika mutasi benar-benar
	 * tertulis dan bukan saat baris diketik, dan (c) menolak pemakaian yang
	 * membuat stok akhir negatif. Pada bahan medis, stok negatif bukan sekadar
	 * kejanggalan angka: ia berarti sistem mencatat pemakaian barang yang
	 * menurut catatannya sendiri tidak ada, sehingga baik penelusuran batch
	 * maupun perencanaan pengadaan berikutnya bertumpu pada data yang salah.
	 * </p>
	 *
	 * @return potret stok setelah pemakaian, atau {@code null} bila tidak
	 *         direkam.
	 */
	public Double getStokmenjadi() {
		return stokmenjadi;
	}

}
