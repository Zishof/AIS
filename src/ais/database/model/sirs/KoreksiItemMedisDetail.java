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
 * Entitas baris (detail) <b>Koreksi Item Medis</b> pada schema {@code sirs}
 * (tabel {@code koreksi_item_medis_detail}). Setiap baris mencatat penyesuaian
 * stok satu {@link ItemMedis} pada satu dokumen {@link KoreksiItemMedis}:
 * berapa banyak dikoreksi ({@link #getJumlah()}), ke arah mana
 * ({@link #getKodeTransaksi()}), dan sebagai rekaman keadaan, berapa stok
 * sebelum ({@link #getStok()}) dan sesudahnya ({@link #getStokmenjadi()}).
 *
 * <h2>Arah koreksi tidak ada di baris ini</h2>
 * <p>
 * {@link #getJumlah()} pada baris ini adalah BESARAN tanpa arah. Apakah
 * koreksi menambah atau mengurangi stok ditentukan oleh
 * {@link #getKodeTransaksi()}, yaitu oleh baris data pada master
 * {@link KodeTransaksiMedis} — bukan oleh tanda angka di sini dan bukan pula
 * oleh kode program.
 * </p>
 * <p>
 * Rancangan ini punya konsekuensi yang perlu disadari betul: kebenaran arah
 * mutasi stok menjadi persoalan DATA, bukan persoalan kode. Satu baris master
 * kode transaksi yang salah tandanya akan membalik arah seluruh koreksi yang
 * memakainya, secara diam-diam dan konsisten, tanpa satu baris kode pun yang
 * keliru dan tanpa satu pun mekanisme di level model yang dapat
 * mendeteksinya. Setiap verifikasi kebenaran arah koreksi karena itu harus
 * mencakup pemeriksaan isi tabel master tersebut, bukan hanya penelaahan kode.
 * </p>
 *
 * <h2>{@code stok} dan {@code stokmenjadi} adalah REKAMAN, bukan penjaga</h2>
 * <p>
 * Kedua kolom itu menyimpan potret angka stok pada saat baris koreksi disusun.
 * Keduanya BUKAN sumber kebenaran stok — stok sesungguhnya dihitung dari
 * akumulasi seluruh mutasi — dan bukan pula mekanisme penjagaan. Sifatnya
 * sebagai potret membawa dua akibat:
 * </p>
 * <ul>
 *   <li><b>Bisa basi.</b> Antara saat baris disusun dan saat dokumen
 *       disetujui, transaksi lain di gudang yang sama dapat mengubah stok.
 *       {@link #getStok()} yang tersimpan tidak ikut berubah, sehingga
 *       {@link #getStokmenjadi()} yang diturunkan darinya pun tidak lagi
 *       menggambarkan hasil sebenarnya.</li>
 *   <li><b>Tidak menahan apa pun.</b> Nilai {@link #getStokmenjadi()} yang
 *       negatif tersimpan begitu saja; skema tidak menolaknya. Karena itu
 *       keberadaan kolom ini TIDAK boleh disalahartikan sebagai adanya penjaga
 *       keseimbangan stok — penjaga yang sesungguhnya harus membaca stok
 *       terkini pada saat persetujuan dan menolak koreksi yang membuat stok
 *       jatuh di bawah nol.</li>
 * </ul>
 *
 * <h2>Catatan lain</h2>
 * <p>
 * Entitas ini tidak memiliki kolom {@code satuanItem}, sehingga
 * {@link #getJumlah()} hanya bermakna bila disepakati mengacu pada satuan dasar
 * item. Seluruh relasinya OPSIONAL ({@code nullable = true}) — termasuk
 * {@link #getKoreksiItem()} dan, yang lebih berisiko,
 * {@link #getKodeTransaksi()} yang menentukan arah koreksi. Baris tanpa kode
 * transaksi adalah baris yang besarannya diketahui tetapi arahnya tidak,
 * sehingga tidak dapat diterjemahkan menjadi mutasi stok yang bermakna.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "koreksi_item_medis_detail")
public class KoreksiItemMedisDetail extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris koreksi ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris koreksi ini. Nilai
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
	 * Representasi ringkas baris koreksi ini untuk tampilan/log, berupa
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
	 * Menetapkan nama pengguna yang mengubah baris koreksi ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris koreksi ini.
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
	private KoreksiItemMedis koreksiItem;
	private String keterangan;
	private KodeTransaksiMedis kodeTransaksi;
	private Double stok;
	private Double stokmenjadi;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public KoreksiItemMedisDetail() {
	}

	/**
	 * Primary key baris koreksi ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik baris koreksi ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris koreksi ini.
	 *
	 * @param id ID baris koreksi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris koreksi ini. Bersama
	 * {@link #getKodeTransaksi()} yang mengelompokkan jenis koreksi, teks bebas
	 * inilah tempat alasan koreksi per item dicatat.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris koreksi ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan item medis yang dikoreksi pada baris ini.
	 *
	 * @param item item medis yang dikoreksi.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang dikoreksi pada baris ini — relasi OPSIONAL ke
	 * {@link ItemMedis}. Inilah item yang stoknya berubah di gudang
	 * {@link KoreksiItemMedis#getLokasi()} saat dokumen disetujui.
	 *
	 * <p>
	 * Getter ini memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy
	 * dan menugaskan hasilnya kembali ke field — sehingga bukan getter murni:
	 * ia bisa mengubah state object dan membuka koneksi database sendiri saat
	 * sesi Hibernate asalnya sudah tertutup.
	 * </p>
	 *
	 * @return item medis yang dikoreksi, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan besaran koreksi pada baris ini. Tidak ada penolakan nilai
	 * negatif maupun nol, dan tidak ada pemeriksaan kecukupan stok, di level
	 * entitas.
	 *
	 * @param jumlah besaran koreksi.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil BESARAN koreksi pada baris ini — perhatikan bahwa angka ini
	 * tidak membawa arah. Apakah stok bertambah atau berkurang ditentukan oleh
	 * {@link #getKodeTransaksi()}, bukan oleh tanda angka di sini.
	 *
	 * <p>
	 * Karena arah dan besaran terpisah, nilai NEGATIF pada kolom ini bermakna
	 * pembalikan arah yang tersembunyi: besaran negatif yang dikalikan tanda
	 * "menambah" akan menghasilkan pengurangan stok pada baris yang di layar
	 * tampak sebagai koreksi penambahan, dan sebaliknya. Baris semacam itu
	 * menyesatkan pembacanya, bukan sekadar salah nilai — penolakan nilai
	 * negatif dan nol karena itu perlu ditegakkan lapisan action.
	 * </p>
	 * <p>
	 * Tidak ada pula pemeriksaan bahwa koreksi ke arah pengurangan masih dalam
	 * batas stok yang tersedia. Kolom {@link #getStokmenjadi()} yang menyimpan
	 * hasil perhitungannya tidak menahan nilai negatif, sehingga penjaga
	 * keseimbangan stok harus dipasang terpisah di lapisan action, dengan
	 * membaca stok terkini pada saat dokumen DISETUJUI — bukan bersandar pada
	 * potret yang tersimpan di baris ini.
	 * </p>
	 * <p>
	 * Karena entitas ini tidak punya kolom satuan, angka ini hanya bermakna
	 * bila disepakati mengacu pada satuan dasar item. Nilainya bisa
	 * {@code null} karena tidak di-default; pemanggil yang memakainya untuk
	 * mutasi stok WAJIB menangani {@code null}.
	 * </p>
	 *
	 * @return besaran koreksi, atau {@code null} bila belum diisi.
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan dokumen koreksi induk baris ini.
	 *
	 * @param koreksiItem dokumen koreksi induk.
	 */
	public void setKoreksiItem(KoreksiItemMedis koreksiItem) {
		this.koreksiItem = koreksiItem;
	}

	/**
	 * Mengambil dokumen {@link KoreksiItemMedis} yang menjadi induk struktural
	 * baris ini, dipetakan ke kolom {@code koreksi_item}. Lewat induk inilah
	 * baris ini memperoleh gudang yang stoknya dikoreksi dan status persetujuan
	 * yang menentukan apakah koreksi sudah berdampak.
	 *
	 * <p>
	 * Perhatikan ketidakselarasan penamaan: nama property JavaBean di sini
	 * adalah {@code koreksiItem} (dan nama kolomnya {@code koreksi_item}),
	 * sedangkan nama kelas yang ditunjuknya adalah {@link KoreksiItemMedis} dan
	 * nama tabelnya {@code sirs.koreksi_item_medis}. Nama pendek
	 * "koreksi item" juga dipakai oleh entitas milik modul {@code library} yang
	 * sama sekali berbeda, sehingga kode SQL mentah maupun kriteria Hibernate
	 * yang menyebut nama tanpa prefix schema mudah menyasar tabel yang keliru
	 * alih-alih gagal dengan jelas.
	 * </p>
	 * <p>
	 * Relasi OPSIONAL ({@code nullable = true}) walaupun secara bisnis wajib,
	 * sehingga baris koreksi yatim tetap sah secara skema dan tidak akan pernah
	 * ikut bergerak saat dokumen mana pun disetujui.
	 * </p>
	 *
	 * @return dokumen koreksi induk, atau {@code null} bila baris ini yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "koreksi_item", nullable = true)
	public KoreksiItemMedis getKoreksiItem() {
		return koreksiItem;
	}

	/**
	 * Menetapkan harga satuan item pada baris koreksi ini.
	 *
	 * @param harga harga satuan item.
	 */
	public void setHarga(Double harga) {
		this.harga = harga;
	}

	/**
	 * Mengambil harga satuan item pada baris koreksi ini — dasar penilaian
	 * rupiah dari koreksi yang dilakukan. Angka inilah yang menentukan berapa
	 * besar nilai persediaan yang bertambah atau menyusut akibat koreksi,
	 * sehingga ia relevan bagi akuntansi meskipun {@link KoreksiItemMedis}
	 * tidak memiliki relasi ke {@code PostingHistory}.
	 *
	 * <p>
	 * Skema tidak menautkan angka ini ke harga beli historis item yang
	 * bersangkutan maupun ke baris penerimaan yang dulu memasukkannya, dan
	 * tidak memeriksa kewajarannya. Nilai persediaan yang hilang atau bertambah
	 * lewat koreksi karena itu sepenuhnya ditentukan oleh angka yang diketik
	 * pada saat dokumen dibuat.
	 * </p>
	 *
	 * @return harga satuan item, atau {@code null} bila belum diisi.
	 */
	public Double getHarga() {
		return harga;
	}

	/**
	 * Menetapkan kode transaksi yang menentukan arah dan klasifikasi koreksi
	 * pada baris ini.
	 *
	 * @param kodeTransaksi kode transaksi medis.
	 */
	public void setKodeTransaksi(KodeTransaksiMedis kodeTransaksi) {
		this.kodeTransaksi = kodeTransaksi;
	}

	/**
	 * Mengambil {@link KodeTransaksiMedis} yang menentukan ARAH dan
	 * klasifikasi koreksi pada baris ini — kolom paling menentukan pada
	 * entitas ini, karena ia yang memutuskan apakah {@link #getJumlah()}
	 * menambah atau mengurangi stok.
	 *
	 * <p>
	 * Perlu digarisbawahi bahwa arah tersebut berasal dari ISI baris master
	 * yang ditunjuk, bukan dari kode program. Artinya kebenaran arah seluruh
	 * koreksi bergantung pada kebenaran data master {@link KodeTransaksiMedis}.
	 * Satu baris master yang salah tandanya akan membalik arah setiap koreksi
	 * yang memakai kode tersebut — konsisten, senyap, dan tanpa satu baris kode
	 * pun yang keliru. Verifikasi kebenaran arah karena itu harus dilakukan
	 * terhadap isi tabel master, dan sebaiknya diulang setelah setiap migrasi
	 * atau penyalinan data antar lingkungan, karena penelaahan kode saja tidak
	 * akan pernah menemukan kesalahannya.
	 * </p>
	 * <p>
	 * Relasi ini OPSIONAL ({@code nullable = true}). Baris tanpa kode transaksi
	 * adalah baris yang besarannya diketahui tetapi arahnya tidak, sehingga
	 * tidak dapat diterjemahkan menjadi mutasi stok yang bermakna — kondisi
	 * yang perlu ditolak lapisan action sebelum dokumen disetujui.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getItem()}).
	 * </p>
	 *
	 * @return kode transaksi medis, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kode_transaksi", nullable = true)
	public KodeTransaksiMedis getKodeTransaksi() {
		kodeTransaksi = check(kodeTransaksi);
		return kodeTransaksi;
	}

	/**
	 * Menetapkan potret stok sebelum koreksi pada baris ini.
	 *
	 * @param stok angka stok sebelum koreksi.
	 */
	public void setStok(Double stok) {
		this.stok = stok;
	}

	/**
	 * Mengambil POTRET stok item ini di gudang yang bersangkutan pada saat
	 * baris koreksi disusun — sebuah rekaman keadaan, bukan sumber kebenaran
	 * stok. Stok yang sesungguhnya selalu dihitung dari akumulasi seluruh
	 * mutasi; angka di sini hanya membantu pembaca dokumen memahami konteks
	 * koreksi yang dilakukan.
	 *
	 * <p>
	 * Sifatnya sebagai potret berarti angka ini BISA BASI: transaksi lain di
	 * gudang yang sama antara saat baris disusun dan saat dokumen disetujui
	 * akan mengubah stok sesungguhnya tanpa mengubah angka di sini. Karena
	 * {@link #getStokmenjadi()} diturunkan dari angka ini, keduanya bisa
	 * sama-sama tidak menggambarkan keadaan yang sebenarnya terjadi.
	 * </p>
	 * <p>
	 * Justru pada dokumen koreksilah kebasian itu paling mudah terjadi:
	 * koreksi lazimnya disusun setelah stok opname, yaitu proses yang memakan
	 * waktu, sehingga jarak antara penyusunan baris dan persetujuan dokumen
	 * cenderung panjang. Kode yang memakai angka ini untuk perhitungan apa pun
	 * — bukan sekadar untuk ditampilkan — perlu membaca ulang stok terkini
	 * alih-alih memercayai potret ini.
	 * </p>
	 *
	 * @return potret stok sebelum koreksi, atau {@code null} bila tidak
	 *         direkam.
	 */
	public Double getStok() {
		return stok;
	}

	/**
	 * Menetapkan potret stok setelah koreksi pada baris ini.
	 *
	 * @param stokmenjadi angka stok setelah koreksi.
	 */
	public void setStokmenjadi(Double stokmenjadi) {
		this.stokmenjadi = stokmenjadi;
	}

	/**
	 * Mengambil POTRET stok yang diperkirakan setelah koreksi ini diterapkan —
	 * secara logis merupakan {@link #getStok()} yang disesuaikan dengan
	 * {@link #getJumlah()} menurut arah dari {@link #getKodeTransaksi()}.
	 * Sama seperti {@link #getStok()}, ini nilai TERSIMPAN yang dihitung sekali
	 * saat baris disusun dan tidak diturunkan ulang saat dibaca.
	 *
	 * <p>
	 * PENTING: keberadaan kolom ini TIDAK boleh disalahartikan sebagai adanya
	 * penjaga keseimbangan stok. Kolom ini merekam hasil perhitungan, ia tidak
	 * menolak apa pun. Nilai NEGATIF di sini tersimpan begitu saja — skema
	 * tidak mengeluh, dan tidak ada constraint yang menahannya. Baris koreksi
	 * yang membuat stok jatuh di bawah nol akan tetap tersimpan, tetap dapat
	 * disetujui, dan tetap menuliskan mutasi stoknya.
	 * </p>
	 * <p>
	 * Penjaga keseimbangan stok yang sesungguhnya harus (a) membaca stok
	 * TERKINI, bukan potret {@link #getStok()} yang bisa basi, (b) berjalan
	 * pada saat dokumen DISETUJUI, karena itulah titik ketika mutasi
	 * benar-benar tertulis dan bukan saat baris diketik, dan (c) menolak
	 * koreksi yang membuat stok akhir negatif — atau, bila stok negatif memang
	 * dikehendaki sebagai kondisi sah dalam kebijakan tertentu, memastikan hal
	 * itu keputusan yang eksplisit dan bukan sekadar akibat dari tiadanya
	 * pemeriksaan.
	 * </p>
	 *
	 * @return potret stok setelah koreksi, atau {@code null} bila tidak
	 *         direkam.
	 */
	public Double getStokmenjadi() {
		return stokmenjadi;
	}

}
