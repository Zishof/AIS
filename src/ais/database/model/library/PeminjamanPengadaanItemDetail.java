package ais.database.model.library;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;

/**
 * Entity <b>baris rincian</b> dokumen peminjaman buku oleh anggota (tabel
 * {@code library.peminjaman_pengadaan_item_detail}). Satu baris menyatakan satu eksemplar yang
 * dipinjam, dan menunjuk balik ke header {@link PeminjamanPengadaanItem}. Sama seperti headernya,
 * <b>kelas ini adalah bagian dari sirkulasi, bukan pengadaan</b> &mdash; kata "Pengadaan" pada
 * namanya hanya warisan template penamaan modul {@code library}.
 *
 * <h3>Kelas ini menghitung, bukan sekadar menyimpan</h3>
 * <p>Berbeda dari baris rincian dokumen lain di paket ini yang hampir seluruhnya berupa
 * <i>value object</i> pasif, kelas ini memuat mesin perhitungan tenggat dan keterlambatan. Empat
 * besaran diturunkan setiap kali dibaca, bukan diambil apa adanya dari basis data:</p>
 * <ul>
 *   <li>{@link #getJumlahHariBatas()} = {@code (jumlahPerpanjangan + 1) &times;
 *       header.jumlahHariBatas} &mdash; durasi pinjam efektif setelah perpanjangan;</li>
 *   <li>{@link #getJumlahSelisihHari()} = cacah hari kerja antara tanggal pinjam dan tanggal
 *       kembali (atau hari ini bila belum kembali);</li>
 *   <li>{@link #getJumlahHariTerlambat()} = {@code selisih - batas}, dibatasi bawah pada nol;</li>
 *   <li>{@link #getBatasWaktupengembalian()} = tanggal jatuh tempo, dihitung
 *       {@link #hitungBatasWaktupengembalian(Perpustakaan)} dari tanggal pinjam ditambah durasi
 *       efektif dalam hari kerja, lalu digeser mundur melewati akhir pekan dan hari libur sesuai
 *       konfigurasi.</li>
 * </ul>
 *
 * <p><b>Seluruh perhitungan itu bersifat destruktif dan ikut tersimpan.</b> Setiap getter di atas
 * menuliskan hasilnya balik ke field, dan karena Hibernate memetakan entity ini dengan
 * <i>property access</i>, nilai hasil hitungan terakhir itulah yang ditulis ke kolomnya pada
 * <i>flush</i> berikutnya. Kolom {@code batas_waktu_pengembalian},
 * {@code jumlah_hari_terlambat}, {@code jumlah_hari_batas}, dan {@code jumlah_selisih_hari}
 * karenanya <b>bukan catatan historis</b> melainkan cache dari perhitungan paling akhir.
 * Konsekuensi yang perlu disadari:</p>
 * <ul>
 *   <li>selama eksemplar belum dikembalikan, {@link #getJumlahSelisihHari()} memakai
 *       <em>waktu server saat ini</em> sebagai tanggal selesai, sehingga nilai yang tersimpan
 *       bertambah setiap hari;</li>
 *   <li>mengubah konfigurasi hari libur, atau menambah entri
 *       {@code Common.hariLiburPerpustakaans}, akan menggeser tanggal jatuh tempo peminjaman
 *       yang <em>sudah berjalan</em> begitu barisnya dibaca ulang &mdash; tenggat yang sudah
 *       diberitahukan kepada anggota dapat berubah tanpa jejak;</li>
 *   <li>sekadar membuka daftar peminjaman di layar sudah cukup untuk menandai baris sebagai
 *       kotor dan memicu {@code UPDATE}.</li>
 * </ul>
 *
 * <p><b>Perpanjangan.</b> {@link #getJumlahPerpanjangan() jumlahPerpanjangan} adalah cacah
 * perpanjangan yang sudah dipakai dan langsung menggandakan durasi pinjam lewat rumus di atas.
 * Batasnya disimpan pada {@link #getJumlahMaxPerpanjangan() jumlahMaxPerpanjangan}, tetapi
 * <b>entity ini tidak menegakkannya sendiri</b>: perbandingan dilakukan di luar, oleh
 * {@code LibraryMemberApi} dan {@code helper/KembaliPengadaanItemPunyaItemHelper}, yang mengisi
 * batasnya dari {@link BatasWaktuPeminjamanItem#getJumlahMaksimalPerpanjanganPeminjaman()}.
 * Pemanggil yang menaikkan {@code jumlahPerpanjangan} tanpa melewati kedua tempat itu dapat
 * memperpanjang pinjaman tanpa batas.</p>
 *
 * <p><b>Efek samping.</b> Sebagian besar getter pada kelas ini mengubah state objek; lihat
 * catatan pada masing-masing. Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap
 * menjadi tanggung jawab DAO/service dengan session aktif; jangan menaruh query duplikat pada
 * model.</p>
 *
 * @see PeminjamanPengadaanItem
 * @see KembaliPengadaanItemDetail
 * @see BatasWaktuPeminjamanItem
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "library", name = "peminjaman_pengadaan_item_detail")
public class PeminjamanPengadaanItemDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja disamakan di seluruh entity modul
	 * {@code library} karena kelas-kelas ini dibangkitkan dari template yang sama; jangan
	 * diubah agar sesi ZK/HTTP yang sudah terserialisasi tetap dapat dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (surrogate key) baris ini, dibangkitkan oleh database. */
	private Long id;
	/** Nama pengguna aplikasi yang terakhir mengubah baris ini (jejak audit ringan). */
	private String oleh;
	/** ID pengguna aplikasi yang terakhir mengubah baris ini (jejak audit ringan). */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna aplikasi yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir. Bersifat <b>no-op bila nilai baru kosong atau
	 * hanya berisi spasi</b> agar jejak audit lama tidak tertimpa oleh pemanggil tanpa konteks
	 * pengguna.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris untuk grid, combobox, dan log.
	 *
	 * <p>Membaca field {@link #item} secara langsung dan merangkainya dengan {@code ""}
	 * sehingga aman terhadap {@code null}. Barcode eksemplar dan tenggat tidak ikut tampil,
	 * sehingga dua baris yang meminjam eksemplar berbeda dari judul yang sama terlihat
	 * identik.</p>
	 *
	 * @return representasi teks item pada baris ini.
	 */
	public String toString() {
		return item + "";
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir; no-op bila nilai baru kosong/blank.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna aplikasi yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}. Dipanggil Hibernate tepat sebelum {@code UPDATE},
	 * lalu mendelegasikan pengisian trio field audit kepada
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat dan
	 * diperbarui oleh {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah cap waktu baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Judul/koleksi yang dipinjam pada baris ini (denormalisasi dari eksemplarnya). */
	private Item item;
	/** Kuantitas yang dipinjam; praktis selalu satu karena baris terikat pada satu eksemplar. */
	private Double jumlah;
	/** Header dokumen peminjaman pemilik baris ini. */
	private PeminjamanPengadaanItem peminjamanPengadaanItem;
	/** Catatan bebas pada tingkat baris. */
	private String keterangan;
	/** Eksemplar fisik (barcode) yang dipinjam. */
	private ItemPunyaBarcode itemPunyaBarcode;
	/** Pesanan/reservasi anggota yang dipenuhi oleh peminjaman ini; {@code null} bila peminjaman langsung. */
	private PesananAnggota pesananAnggota;
	/** Baris pengembalian yang menutup baris peminjaman ini; {@code null} bila belum kembali. */
	private KembaliPengadaanItemDetail kembaliPengadaanItemDetail;

	/** Cache hasil hitung hari keterlambatan ({@code selisih - batas}, minimal nol). */
	private Integer jumlahHariTerlambat;
	/** Cacah perpanjangan yang sudah dipakai; menggandakan durasi pinjam. */
	private Integer jumlahPerpanjangan;
	/** Batas perpanjangan yang diizinkan; ditegakkan di luar entity ini. */
	private Integer jumlahMaxPerpanjangan;
	/** Cache durasi pinjam efektif ({@code (perpanjangan + 1) &times; batas header}). */
	private Integer jumlahHariBatas;
	/** Cache cacah hari kerja antara tanggal pinjam dan tanggal kembali/hari ini. */
	private Integer jumlahSelisihHari;

	/** Cache tanggal jatuh tempo hasil {@link #hitungBatasWaktupengembalian(Perpustakaan)}. */
	private Date batasWaktupengembalian;
	/** Tanggal eksemplar dikembalikan, disalin dari baris pengembalian bila ada. */
	private Date tanggalKembali;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 */
	public PeminjamanPengadaanItemDetail() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return ID baris, atau {@code null} bila objek belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Umumnya hanya dipanggil Hibernate setelah {@code INSERT}.
	 *
	 * @param id ID baris baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan baris.
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan baris.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel judul/koleksi yang dipinjam pada baris ini.
	 *
	 * @param item judul yang dipinjam.
	 */
	public void setItem(Item item) {
		this.item = item;
	}

	/**
	 * Mengembalikan judul/koleksi yang dipinjam pada baris ini.
	 *
	 * <p>Getter menjalankan {@code check(...)} milik {@link GeneralValueObject} untuk menukar
	 * proxy Hibernate yang sudah terlepas session dengan instance yang aman dibaca, lalu
	 * <b>menulis hasilnya balik ke field</b> (getter destruktif ringan).</p>
	 *
	 * @return judul yang dipinjam, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menyetel kuantitas yang dipinjam pada baris ini.
	 *
	 * @param jumlah kuantitas yang dipinjam.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan kuantitas yang dipinjam pada baris ini, dinormalkan ke {@code 1.0} bila
	 * belum diisi.
	 *
	 * <p>Nilai baku satu &mdash; bukan nol &mdash; masuk akal karena setiap baris terikat pada
	 * satu {@link #getItemPunyaBarcode() eksemplar} tertentu. Nilai ini bukan sekadar hiasan:
	 * {@code LibraryUtil} dan helper pengembalian mengalikan tarif denda per eksemplar dengan
	 * angka ini, sehingga kuantitas yang keliru menggandakan denda anggota. Normalisasi ditulis
	 * balik ke field, sehingga getter ini mengubah state objek.</p>
	 *
	 * @return kuantitas yang dipinjam; tidak pernah {@code null}.
	 */
	public Double getJumlah() {
		if (jumlah == null) {
			jumlah = 1.0;
		}
		return jumlah;
	}

	/**
	 * Menyetel header dokumen peminjaman pemilik baris ini.
	 *
	 * <p>Header adalah sumber tanggal pinjam, durasi pinjam dasar, dan perpustakaan yang
	 * dipakai seluruh perhitungan pada kelas ini; menyetelnya {@code null} membuat
	 * {@link #getJumlahHariBatas()}, {@link #getJumlahSelisihHari()}, dan
	 * {@link #getBatasWaktupengembalian()} tidak dapat menghitung apa pun.</p>
	 *
	 * @param peminjamanPengadaanItem header dokumen peminjaman.
	 */
	public void setPeminjamanPengadaanItem(PeminjamanPengadaanItem peminjamanPengadaanItem) {
		this.peminjamanPengadaanItem = peminjamanPengadaanItem;
	}

	/**
	 * Mengembalikan header dokumen peminjaman pemilik baris ini.
	 *
	 * <p>Relasi dipetakan {@link FetchMode#SELECT} (eager per baris) sehingga aman dibaca dari
	 * renderer. Kolomnya {@code nullable}; baris tanpa header adalah baris yatim yang tidak
	 * pernah sah secara bisnis.</p>
	 *
	 * @return header dokumen peminjaman, atau {@code null} bila baris belum dikaitkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peminjaman_pengadaan_item", nullable = true)
	public PeminjamanPengadaanItem getPeminjamanPengadaanItem() {
		return peminjamanPengadaanItem;
	}

	/**
	 * Mengembalikan eksemplar fisik (barcode) yang dipinjam pada baris ini.
	 *
	 * <p>Inilah data yang dipindai petugas di meja sirkulasi dan yang membedakan dua baris atas
	 * judul yang sama. Relasi dipetakan {@link FetchMode#SELECT} sehingga aman dibaca dari
	 * renderer ZK.</p>
	 *
	 * @return eksemplar yang dipinjam, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item_punya_barcode", nullable = true)
	public ItemPunyaBarcode getItemPunyaBarcode() {
		return itemPunyaBarcode;
	}

	/**
	 * Menyetel eksemplar fisik (barcode) yang dipinjam pada baris ini.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak memeriksa apakah eksemplar tersebut sedang
	 * dipinjam anggota lain, sedang ditransfer, atau berstatus tidak dapat dipinjam.
	 * Pemeriksaan itu berada di {@code helper/PeminjamanPengadaanItemPunyaItemHelper}.</p>
	 *
	 * @param itemPunyaBarcode eksemplar yang dipinjam.
	 */
	public void setItemPunyaBarcode(ItemPunyaBarcode itemPunyaBarcode) {
		this.itemPunyaBarcode = itemPunyaBarcode;
	}

	/**
	 * Mengembalikan pesanan/reservasi anggota yang dipenuhi oleh peminjaman ini.
	 *
	 * <p>Terisi bila anggota sebelumnya memesan judul yang sedang dipinjam orang lain dan
	 * pesanan itu kini dilayani. Bernilai {@code null} untuk peminjaman langsung dari rak.</p>
	 *
	 * @return pesanan anggota terkait, atau {@code null} bila peminjaman langsung.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pesanan_anggota", nullable = true)
	public PesananAnggota getPesananAnggota() {
		return pesananAnggota;
	}

	/**
	 * Menyetel pesanan/reservasi anggota yang dipenuhi oleh peminjaman ini.
	 *
	 * @param pesananAnggota pesanan anggota terkait.
	 */
	public void setPesananAnggota(PesananAnggota pesananAnggota) {
		this.pesananAnggota = pesananAnggota;
	}

	/**
	 * Menghitung dan mengembalikan jumlah hari keterlambatan eksemplar ini.
	 *
	 * <p>Rumusnya sederhana: {@link #getJumlahSelisihHari() lama penahanan} dikurangi
	 * {@link #getJumlahHariBatas() durasi pinjam efektif}, lalu dibatasi bawah pada nol
	 * sehingga pengembalian yang tepat waktu atau lebih awal menghasilkan {@code 0}, bukan
	 * angka negatif. Kedua operand adalah hasil hitung ulang, bukan nilai tersimpan, sehingga
	 * pemanggilan method ini <b>selalu menghitung ulang dari awal</b>.</p>
	 *
	 * <p><b>Angka ini menentukan tarif denda.</b> {@code LibraryUtil.hitungDendaItem(...)}
	 * memakainya sebagai ambang: ia memilih baris {@link DendaKeterlambatanItem} yang
	 * {@code jumlah_hari}-nya paling besar namun masih kurang dari atau sama dengan nilai ini,
	 * di antara aturan yang cocok dengan profil anggota. Dengan kata lain,
	 * {@link DendaKeterlambatanItem} berfungsi sebagai tabel bertingkat &mdash; makin lama
	 * terlambat, makin tinggi tarif yang berlaku &mdash; dan method inilah yang menentukan
	 * anak tangga mana yang dipakai.</p>
	 *
	 * <p><b>Nilai selalu bergerak selama buku belum kembali.</b> Karena
	 * {@link #getJumlahSelisihHari()} memakai waktu server saat ini bila belum ada tanggal
	 * kembali, jumlah hari terlambat bertambah setiap hari. Ini benar untuk tampilan tunggakan
	 * berjalan, tetapi berarti nilai yang ikut tersimpan ke kolomnya hanyalah potret pembacaan
	 * terakhir dan tidak boleh diperlakukan sebagai catatan historis.</p>
	 *
	 * <p><b>Getter destruktif.</b> Hasil hitungan ditulis balik ke field
	 * {@link #jumlahHariTerlambat}, sehingga memanggil method ini pada objek yang dikelola
	 * session dapat menandai entity kotor dan memicu {@code UPDATE} pada flush berikutnya.
	 * Pemeriksaan {@code jumlahHariTerlambat == null} pada baris terakhir sudah tidak pernah
	 * benar karena nilai baru saja disetel dari hasil pengurangan dua {@code int}; ia sisa dari
	 * versi sebelumnya.</p>
	 *
	 * <p><b>Batas hari kerja, bukan hari kalender.</b> Karena kedua operand dihitung dalam hari
	 * kerja (lihat {@link #getJumlahSelisihHari()}), akhir pekan tidak menambah keterlambatan.
	 * Bandingkan dengan tanggal jatuh tempo pada {@link #getBatasWaktupengembalian()} yang juga
	 * digeser melewati akhir pekan dan hari libur; keduanya memakai basis yang sama sehingga
	 * konsisten satu sama lain.</p>
	 *
	 * @return jumlah hari kerja keterlambatan; tidak pernah negatif dan tidak pernah
	 *         {@code null}.
	 */
	public Integer getJumlahHariTerlambat() {

		int selisih = getJumlahSelisihHari();
		int batas = getJumlahHariBatas();
		jumlahHariTerlambat = (selisih - batas);

//		System.out.println(
//				"selisih => " + selisih + ", batas = " + batas + ", jumlahHariTerlambat = " + jumlahHariTerlambat);

		if (jumlahHariTerlambat == null || jumlahHariTerlambat < 0) {
			jumlahHariTerlambat = 0;
		}
		return jumlahHariTerlambat;
	}

	/**
	 * Menyetel jumlah hari keterlambatan secara manual.
	 *
	 * <p>Praktis tidak berguna sebagai penyimpan nilai: {@link #getJumlahHariTerlambat()}
	 * menghitung ulang dan menimpa apa pun yang disetel di sini pada pembacaan berikutnya.
	 * Setter ini tetap ada karena dibutuhkan Hibernate untuk memuat kolomnya.</p>
	 *
	 * @param jumlahHariTerlambat jumlah hari keterlambatan.
	 */
	public void setJumlahHariTerlambat(Integer jumlahHariTerlambat) {
		this.jumlahHariTerlambat = jumlahHariTerlambat;
	}

	/**
	 * Mengembalikan cacah perpanjangan yang sudah dipakai pada baris ini, dinormalkan ke
	 * {@code 0} bila belum diisi.
	 *
	 * <p>Setiap perpanjangan menggandakan durasi pinjam lewat rumus pada
	 * {@link #getJumlahHariBatas()}. Normalisasi ditulis balik ke field, sehingga getter ini
	 * mengubah state objek.</p>
	 *
	 * @return cacah perpanjangan terpakai; tidak pernah {@code null}.
	 */
	public Integer getJumlahPerpanjangan() {
		if (jumlahPerpanjangan == null) {
			jumlahPerpanjangan = 0;
		}
		return jumlahPerpanjangan;
	}

	/**
	 * Menyetel cacah perpanjangan yang sudah dipakai pada baris ini.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak membandingkan nilai baru dengan
	 * {@link #getJumlahMaxPerpanjangan() batas perpanjangan} dan tidak menolak nilai negatif.
	 * Penegakan batas berada di {@code LibraryMemberApi} dan
	 * {@code helper/KembaliPengadaanItemPunyaItemHelper}; jalur lain yang memanggil setter ini
	 * langsung dapat memperpanjang pinjaman tanpa batas, dan karena
	 * {@link #getJumlahHariBatas()} mengalikan durasi dengan {@code (nilai + 1)}, nilai besar
	 * membuat tenggat melar dan denda tidak pernah muncul.</p>
	 *
	 * @param jumlahPerpanjangan cacah perpanjangan terpakai.
	 */
	public void setJumlahPerpanjangan(Integer jumlahPerpanjangan) {
		this.jumlahPerpanjangan = jumlahPerpanjangan;
	}

	/**
	 * Menghitung dan mengembalikan durasi pinjam efektif baris ini dalam hari kerja.
	 *
	 * <p>Rumusnya {@code (jumlahPerpanjangan + 1) &times;
	 * peminjamanPengadaanItem.getJumlahHariBatas()}: durasi dasar dari header dikalikan banyak
	 * periode yang berlaku, di mana periode pertama adalah peminjaman awal dan sisanya adalah
	 * perpanjangan. Jadi peminjaman 7 hari yang diperpanjang sekali memberi tenggat 14 hari
	 * kerja terhitung dari <em>tanggal pinjam</em>, bukan 7 hari terhitung dari tanggal
	 * perpanjangan.</p>
	 *
	 * <p>Bila header belum tersedia, hitung ulang dilewati dan nilai yang tersimpan
	 * dipertahankan; bila nilai itu pun {@code null}, hasilnya {@code 0}. Perhatikan bahwa
	 * <b>nol berarti tanpa tenggat sama sekali</b> &mdash; seluruh masa pinjam langsung
	 * terhitung sebagai keterlambatan pada {@link #getJumlahHariTerlambat()}. Karena
	 * {@link PeminjamanPengadaanItem#getJumlahHariBatas()} juga mengembalikan {@code 0} ketika
	 * aturan {@link BatasWaktuPeminjamanItem} tidak ditemukan, kegagalan konfigurasi merambat
	 * ke sini secara senyap dan bermanifestasi sebagai denda maksimal, bukan sebagai galat.</p>
	 *
	 * <p><b>Getter destruktif:</b> hasil hitungan ditulis balik ke field
	 * {@link #jumlahHariBatas}, sehingga pembacaan dapat menandai entity kotor.</p>
	 *
	 * @return durasi pinjam efektif dalam hari kerja; tidak pernah {@code null}.
	 */
	public Integer getJumlahHariBatas() {

		if (peminjamanPengadaanItem != null) {
			jumlahHariBatas = ((getJumlahPerpanjangan() + 1) * peminjamanPengadaanItem.getJumlahHariBatas());
		}

		if (jumlahHariBatas == null) {
			jumlahHariBatas = 0;
		}
		return jumlahHariBatas;
	}

	/**
	 * Menyetel durasi pinjam efektif secara manual.
	 *
	 * <p>Nilai yang disetel di sini akan ditimpa {@link #getJumlahHariBatas()} pada pembacaan
	 * berikutnya selama header masih tersedia. Setter ini tetap ada karena dibutuhkan Hibernate
	 * untuk memuat kolomnya.</p>
	 *
	 * @param jumlahHariBatas durasi pinjam efektif dalam hari kerja.
	 */
	public void setJumlahHariBatas(Integer jumlahHariBatas) {
		this.jumlahHariBatas = jumlahHariBatas;
	}

	/**
	 * Mengembalikan tanggal jatuh tempo pengembalian eksemplar ini.
	 *
	 * <p><b>Getter ini menghitung ulang, bukan membaca.</b> Ia memanggil
	 * {@link #hitungBatasWaktupengembalian()} lebih dahulu, yang menulis ulang field
	 * {@link #batasWaktupengembalian}, baru kemudian mengembalikannya. Karena kolomnya dipetakan
	 * ({@code batas_waktu_pengembalian}) dan Hibernate membaca lewat getter, <b>nilai yang
	 * tersimpan di basis data selalu merupakan hasil perhitungan paling akhir</b>, bukan tenggat
	 * yang ditetapkan saat peminjaman terjadi.</p>
	 *
	 * <p>Konsekuensi praktisnya: mengubah konfigurasi hari libur, menambah entri
	 * {@code Common.hariLiburPerpustakaans}, atau menaikkan
	 * {@link #getJumlahPerpanjangan() cacah perpanjangan} akan menggeser tenggat peminjaman yang
	 * <em>sudah berjalan</em> begitu barisnya dibaca ulang. Tenggat yang tercetak pada bukti
	 * peminjaman anggota karenanya dapat berbeda dari yang tersimpan kemudian, tanpa jejak
	 * perubahan apa pun selain revisi Envers.</p>
	 *
	 * @return tanggal jatuh tempo hasil perhitungan terkini; dapat {@code null} bila header
	 *         belum tersedia sehingga perhitungan dilewati.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "batas_waktu_pengembalian")
	public Date getBatasWaktupengembalian() {
		hitungBatasWaktupengembalian();
		return batasWaktupengembalian;
	}

	/**
	 * Menghitung ulang tanggal jatuh tempo memakai perpustakaan milik header.
	 *
	 * <p>Pembungkus aman untuk {@link #hitungBatasWaktupengembalian(Perpustakaan)}: bila header
	 * belum tersedia, perhitungan dilewati sepenuhnya sehingga tidak terjadi
	 * {@code NullPointerException}. Method berargumen yang dipanggilnya <em>tidak</em> punya
	 * penjagaan serupa, jadi inilah pintu masuk yang seharusnya dipakai pemanggil biasa.</p>
	 */
	public void hitungBatasWaktupengembalian() {
		if (peminjamanPengadaanItem != null) {
			hitungBatasWaktupengembalian(peminjamanPengadaanItem.getPerpustakaan());
		}
	}

	/**
	 * Menghitung ulang tanggal jatuh tempo pengembalian dan menyimpannya ke
	 * {@link #batasWaktupengembalian}.
	 *
	 * <p>Alurnya empat tahap:</p>
	 * <ol>
	 *   <li><b>Titik awal.</b> {@code Common.getDateWorkingDays(tanggalPinjam,
	 *       jumlahHariBatas)} memajukan tanggal peminjaman sebanyak
	 *       {@link #getJumlahHariBatas() durasi pinjam efektif} dalam <em>hari kerja</em>,
	 *       sehingga akhir pekan yang terlewati sudah tidak ikut terhitung sejak awal.</li>
	 *   <li><b>Geser mundur bila jatuh di akhir pekan.</b> Bila konfigurasi
	 *       {@code sabtu_dan_minggu_hari_libur_tanggal_kembali_mundur} aktif, tanggal yang jatuh
	 *       pada Sabtu dimajukan dua hari dan yang jatuh pada Minggu dimajukan satu hari.
	 *       Selain itu, bila konfigurasi {@code minggu_hari_libur_tanggal_kembali_mundur} aktif,
	 *       hanya Minggu yang dimajukan satu hari. Kedua cabang bersifat eksklusif
	 *       ({@code else if}), jadi konfigurasi pertama menang bila keduanya aktif.</li>
	 *   <li><b>Geser mundur melewati hari libur nasional.</b> Bila konfigurasi
	 *       {@code libur_nasional_hari_libur_tanggal_kembali_mundur} aktif dan peta
	 *       {@code Common.hariLiburPerpustakaans} tidak kosong, tanggal dimajukan satu per satu
	 *       selama masih jatuh pada hari libur, dengan <b>pembatas 29 iterasi</b>. Rangkaian
	 *       libur yang lebih panjang dari itu akan berhenti digeser dan tenggat tetap jatuh pada
	 *       hari libur.</li>
	 *   <li><b>Simpan.</b> Hasil akhir disetel lewat
	 *       {@link #setBatasWaktupengembalian(Date)}.</li>
	 * </ol>
	 *
	 * <p><b>Parameter {@code currentPerpustakaan} tidak dipakai sama sekali.</b> Ini penting
	 * dan mudah luput: meski tanda tangannya menyiratkan bahwa kalender libur dapat berbeda per
	 * perpustakaan, badan method sepenuhnya membaca sumber global &mdash;
	 * {@code Common.bolehKonfigurasi(...)} untuk ketiga saklar konfigurasi dan
	 * {@code Common.hariLiburPerpustakaans} untuk daftar tanggal libur. Artinya
	 * <b>aturan akhir pekan dan hari libur berlaku seragam untuk seluruh perpustakaan</b> dalam
	 * satu instalasi; perpustakaan fakultas tidak dapat memiliki kalender libur sendiri meski
	 * nama peta globalnya mengandung kata "Perpustakaan". Pemanggil yang mengoper perpustakaan
	 * berbeda akan memperoleh hasil yang persis sama.</p>
	 *
	 * <p><b>Tanpa penjagaan {@code null}.</b> Baris pertama langsung memanggil
	 * {@code peminjamanPengadaanItem.getTanggalPembuatan()}, sehingga memanggil method ini pada
	 * baris yang belum punya header akan melempar {@code NullPointerException}. Gunakan
	 * {@link #hitungBatasWaktupengembalian()} tanpa argumen yang sudah menjaga kondisi itu.</p>
	 *
	 * <p><b>Ketiga saklar konfigurasi dibaca berbeda.</b> Saklar pertama diperiksa dengan
	 * nilai baku {@link Konfigurasi#TIDAK_AKTIF} sehingga instalasi yang belum pernah
	 * mengaturnya memperoleh perilaku "tidak menggeser"; dua saklar lainnya memakai nilai baku
	 * bawaan {@code Common.bolehKonfigurasi(String)}. Perlu diingat bahwa pembacaan konfigurasi
	 * AIS bersifat <i>auto-seed</i>: memanggilnya untuk kunci yang belum ada akan menuliskan
	 * nilai baku ke basis data, jadi jangan mengubah nilai baku di kode tanpa memeriksa data
	 * yang sudah terlanjur tertulis.</p>
	 *
	 * <p>Method ini juga mencetak keluaran diagnostik ke {@code System.out} pada setiap cabang
	 * yang aktif. Karena {@link #getBatasWaktupengembalian()} memanggilnya pada setiap
	 * pembacaan, menampilkan satu halaman daftar peminjaman dapat menghasilkan ratusan baris
	 * log.</p>
	 *
	 * @param currentPerpustakaan perpustakaan konteks; <b>tidak dipakai</b>, seluruh aturan
	 *                            libur dibaca dari konfigurasi global.
	 */
	public void hitungBatasWaktupengembalian(Perpustakaan currentPerpustakaan) {
		Date batas = Common.getDateWorkingDays(peminjamanPengadaanItem.getTanggalPembuatan(), getJumlahHariBatas());

//		System.out.println("batas default => " + Common.dateFormat1.get().format(batas));
		Calendar c = ais.ui.util.WaktuUtil.getCalendar();
		c.setTime(batas);

		if (Common.bolehKonfigurasi("sabtu_dan_minggu_hari_libur_tanggal_kembali_mundur", Konfigurasi.TIDAK_AKTIF)) {
			if (Calendar.SATURDAY == c.get(Calendar.DAY_OF_WEEK)) {
				c.set(Calendar.DATE, c.get(Calendar.DATE) + 2);
			} else if (Calendar.SUNDAY == c.get(Calendar.DAY_OF_WEEK)) {
				c.set(Calendar.DATE, c.get(Calendar.DATE) + 1);
			}
			System.out.println(
					"sabtu_dan_minggu_hari_libur_tanggal_kembali_mundur  => " + Common.dateFormat1.get().format(batas));
		} else if (Common.bolehKonfigurasi("minggu_hari_libur_tanggal_kembali_mundur")) {
			if (Calendar.SUNDAY == c.get(Calendar.DAY_OF_WEEK)) {
				c.set(Calendar.DATE, c.get(Calendar.DATE) + 1);
			}
			System.out.println("minggu_hari_libur_tanggal_kembali_mundur  => " + Common.dateFormat1.get().format(batas));
		}

		if (Common.bolehKonfigurasi("libur_nasional_hari_libur_tanggal_kembali_mundur")) {
			if (!Common.hariLiburPerpustakaans.isEmpty()) {
				List<String> libursNanti = new ArrayList<String>();
				for (Date date : Common.hariLiburPerpustakaans.keySet()) {
					libursNanti.add(Common.dateFormat1.get().format(date));
				}

				for (int i = 1; i < 30; i++) {
					if (!libursNanti.contains(Common.dateFormat1.get().format(c.getTime()))) {
						break;
					}
					c.set(Calendar.DATE, c.get(Calendar.DATE) + 1);
				}
				libursNanti = null;

				System.out.println(
						"libur_nasional_hari_libur_tanggal_kembali_mundur => " + Common.dateFormat1.get().format(batas));
			}

		}

		setBatasWaktupengembalian(c.getTime());
	}

	/**
	 * Menyetel tanggal jatuh tempo pengembalian secara langsung.
	 *
	 * <p>Nilai yang disetel di sini <b>tidak bertahan</b>: {@link #getBatasWaktupengembalian()}
	 * menghitung ulang dan menimpanya pada pembacaan berikutnya. Setter ini pada praktiknya
	 * hanya dipakai oleh {@link #hitungBatasWaktupengembalian(Perpustakaan)} sendiri dan oleh
	 * Hibernate saat memuat baris.</p>
	 *
	 * @param batasWaktupengembalian tanggal jatuh tempo.
	 */
	public void setBatasWaktupengembalian(Date batasWaktupengembalian) {
		this.batasWaktupengembalian = batasWaktupengembalian;
	}

	/**
	 * Menghitung dan mengembalikan lama penahanan eksemplar ini dalam hari kerja.
	 *
	 * <p>Alurnya:</p>
	 * <ol>
	 *   <li><b>Tentukan tanggal selesai.</b> Field {@link #tanggalKembali} disetel ulang dari
	 *       {@link KembaliPengadaanItemDetail#getTanggal()} pada baris pengembalian yang
	 *       tertaut; bila tautan itu belum ada atau tanggalnya kosong, {@code tanggalKembali}
	 *       <b>dikosongkan</b> menjadi {@code null}. Perhatikan bahwa penulisan ini terjadi
	 *       tanpa syarat &mdash; nilai yang sebelumnya disetel lewat
	 *       {@link #setTanggalKembali(Date)} akan tertimpa.</li>
	 *   <li><b>Berhenti bila tidak ada titik awal.</b> Bila header atau tanggal pinjamnya
	 *       kosong, method mengembalikan nilai tersimpan (atau {@code 0}) tanpa menghitung.</li>
	 *   <li><b>Normalkan ke tanggal.</b> Kedua ujung diformat lalu diurai ulang dengan
	 *       {@code Common.databaseDateFormat} agar komponen jam/menit terbuang, sehingga
	 *       peminjaman pagi dan pengembalian sore pada hari yang sama tidak terhitung sebagai
	 *       satu hari penuh. Galat penguraian ditelan dan hanya ditampilkan kepada admin lewat
	 *       {@code Common.tampilErrorJikaAdmin}, sehingga perhitungan tetap berlanjut dengan
	 *       nilai yang belum ternormalkan.</li>
	 *   <li><b>Hitung.</b> {@code Common.getWorkingDaysBetweenTwoDates(mulai, selesai)}
	 *       mengembalikan cacah <em>hari kerja</em> di antara keduanya, sehingga akhir pekan
	 *       tidak menambah lama penahanan.</li>
	 * </ol>
	 *
	 * <p><b>Bila belum dikembalikan, tanggal selesai adalah hari ini.</b> Inilah yang membuat
	 * lama penahanan &mdash; dan lewat {@link #getJumlahHariTerlambat()}, tunggakan denda
	 * &mdash; bertambah setiap hari untuk pinjaman yang masih berjalan. Perilaku ini benar untuk
	 * tampilan tagihan berjalan, tetapi berarti nilai yang ikut tersimpan ke kolomnya hanyalah
	 * potret pembacaan terakhir.</p>
	 *
	 * <p><b>Getter destruktif ganda.</b> Method ini menulis ke dua field sekaligus
	 * ({@link #tanggalKembali} dan {@link #jumlahSelisihHari}), sehingga memanggilnya pada objek
	 * yang dikelola session dapat menandai entity kotor. Ia juga mencetak dua baris diagnostik
	 * ke {@code System.out} pada setiap pemanggilan; karena
	 * {@link #getJumlahHariTerlambat()} memanggilnya, menampilkan satu halaman daftar
	 * peminjaman menghasilkan keluaran log yang banyak.</p>
	 *
	 * <p><b>Ketergantungan pada tautan pengembalian.</b> Karena tanggal selesai hanya diambil
	 * dari {@link #getKembaliPengadaanItemDetail()}, baris peminjaman yang bukunya sudah
	 * dikembalikan namun tautannya belum diisi akan terus dihitung sebagai belum kembali dan
	 * dendanya terus bertambah. Lihat catatan pada
	 * {@link #setKembaliPengadaanItemDetail(KembaliPengadaanItemDetail)} mengenai satu kondisi
	 * yang membuat tautan itu diam-diam tidak tersimpan.</p>
	 *
	 * @return cacah hari kerja antara tanggal pinjam dan tanggal kembali (atau hari ini);
	 *         {@code 0} bila titik awal tidak tersedia.
	 */
	public Integer getJumlahSelisihHari() {
		// System.out.println("kembaliPengadaanItemDetail = " +
		// kembaliPengadaanItemDetail);

		tanggalKembali = kembaliPengadaanItemDetail == null || kembaliPengadaanItemDetail.getTanggal() == null ? null
				: kembaliPengadaanItemDetail.getTanggal();

		if (peminjamanPengadaanItem == null || peminjamanPengadaanItem.getTanggalPembuatan() == null) {
			return jumlahSelisihHari == null ? 0 : jumlahSelisihHari;
		}
		Date tanggalMulai = peminjamanPengadaanItem.getTanggalPembuatan();
		Date tanggalSelesai = tanggalKembali == null ? ais.ui.util.WaktuUtil.getDate() : tanggalKembali;

		try {
			tanggalMulai = Common.databaseDateFormat.get().parse(Common.databaseDateFormat.get().format(tanggalMulai));
			tanggalSelesai = Common.databaseDateFormat.get().parse(Common.databaseDateFormat.get().format(tanggalSelesai));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

		try {
			System.out.println("tanggalMulai=>" + Common.dateFormat4.get().format(tanggalMulai));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/library/PeminjamanPengadaanItemDetail.java:314");

		}

		try {
			System.out.println("tanggalSelesai=>" + Common.dateFormat4.get().format(tanggalSelesai));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/library/PeminjamanPengadaanItemDetail.java:320");

		}

		jumlahSelisihHari = Common.getWorkingDaysBetweenTwoDates(tanggalMulai, tanggalSelesai);

		// System.out.println("tanggalMulai = " +
		// Common.dateFormat3.get().format(tanggalMulai) + ", tanggalSelesai = "
		// + Common.dateFormat3.get().format(tanggalSelesai) + ", jumlahSelisihHari =
		// " + jumlahSelisihHari);

		return jumlahSelisihHari;
	}

	/**
	 * Menyetel lama penahanan dalam hari kerja secara manual.
	 *
	 * <p>Nilai yang disetel di sini akan ditimpa {@link #getJumlahSelisihHari()} pada pembacaan
	 * berikutnya selama header dan tanggal pinjamnya tersedia. Setter ini tetap ada karena
	 * dibutuhkan Hibernate untuk memuat kolomnya, dan menjadi nilai balikan darurat ketika
	 * perhitungan dilewati.</p>
	 *
	 * @param jumlahSelisihHari lama penahanan dalam hari kerja.
	 */
	public void setJumlahSelisihHari(Integer jumlahSelisihHari) {
		this.jumlahSelisihHari = jumlahSelisihHari;
	}

	/**
	 * Mengembalikan baris pengembalian yang menutup baris peminjaman ini.
	 *
	 * <p>Tautan inilah penentu apakah eksemplar dianggap sudah kembali:
	 * {@link #getJumlahSelisihHari()} dan {@link #getTanggalKembali()} keduanya membacanya, dan
	 * selama {@code null} seluruh perhitungan memperlakukan pinjaman sebagai masih berjalan.
	 * Relasi dipetakan {@link FetchMode#SELECT} sehingga aman dibaca dari renderer.</p>
	 *
	 * @return baris pengembalian penutup, atau {@code null} bila eksemplar belum kembali.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kembali_pengadaan_item_detail", nullable = true)
	public KembaliPengadaanItemDetail getKembaliPengadaanItemDetail() {
		return kembaliPengadaanItemDetail;
	}

	/**
	 * Menyetel baris pengembalian yang menutup baris peminjaman ini.
	 *
	 * <p><b>Setter ini menolak objek yang belum tersimpan.</b> Bila argumennya {@code null}
	 * <em>atau</em> {@code getId()}-nya masih {@code null} &mdash; artinya baris pengembalian
	 * baru dibuat di memori dan belum pernah di-<i>flush</i> &mdash; field disetel {@code null},
	 * bukan diisi objek tersebut. Ini penjagaan yang masuk akal terhadap kunci asing yang
	 * menunjuk baris tanpa identitas, tetapi <b>kegagalannya senyap</b>: pemanggil yang menyusun
	 * dokumen pengembalian lengkap di memori lalu menyimpan seluruhnya sekaligus akan mendapati
	 * tautan ini tetap kosong, dan eksemplarnya terus terhitung sebagai belum kembali dengan
	 * denda yang bertambah setiap hari. Pola pemakaian yang benar adalah menyimpan
	 * {@link KembaliPengadaanItemDetail} lebih dahulu sehingga ia punya ID, baru memanggil
	 * setter ini.</p>
	 *
	 * <p>Pemanggil juga bertanggung jawab mengisi sisi sebaliknya
	 * ({@code kembaliPengadaanItemDetail.setPeminjamanPengadaanItemDetail(this)}) karena kedua
	 * penunjuk adalah kolom {@code ManyToOne} mandiri yang tidak disinkronkan Hibernate.</p>
	 *
	 * @param kembaliPengadaanItemDetail baris pengembalian penutup; diabaikan (disetel
	 *                                   {@code null}) bila belum punya ID.
	 */
	public void setKembaliPengadaanItemDetail(KembaliPengadaanItemDetail kembaliPengadaanItemDetail) {
		this.kembaliPengadaanItemDetail = kembaliPengadaanItemDetail == null
				|| kembaliPengadaanItemDetail.getId() == null ? null : kembaliPengadaanItemDetail;
	}

	/**
	 * Mengembalikan batas perpanjangan yang diizinkan untuk baris ini, dinormalkan ke {@code 0}
	 * bila belum diisi.
	 *
	 * <p>Nilai diisi dari luar &mdash; {@code helper/KembaliPengadaanItemPunyaItemHelper}
	 * menyalinnya dari
	 * {@link BatasWaktuPeminjamanItem#getJumlahMaksimalPerpanjanganPeminjaman()} lewat
	 * {@code LibraryUtil.getJumlahMaksimalPerpanjanganPeminjaman(...)}. <b>Entity ini tidak
	 * menegakkannya:</b> tidak ada satu pun tempat di kelas ini yang membandingkan
	 * {@link #getJumlahPerpanjangan()} dengan nilai ini. Penegakan berada pada
	 * {@code LibraryMemberApi} (yang menolak perpanjangan bila kuota terpakai sudah mencapai
	 * batas) dan pada helper pengembalian.</p>
	 *
	 * <p><b>Nilai baku nol berarti perpanjangan tidak diizinkan.</b> Baris lama yang kolomnya
	 * belum pernah diisi karenanya tidak dapat diperpanjang lewat jalur yang memeriksa nilai ini
	 * &mdash; gagal ke sisi yang aman, tetapi juga senyap.</p>
	 *
	 * <p>Normalisasi ditulis balik ke field, sehingga getter ini mengubah state objek.</p>
	 *
	 * @return batas perpanjangan yang diizinkan; tidak pernah {@code null}.
	 */
	public Integer getJumlahMaxPerpanjangan() {
		if (jumlahMaxPerpanjangan == null) {
			jumlahMaxPerpanjangan = 0;
		}
		return jumlahMaxPerpanjangan;
	}

	/**
	 * Menyetel batas perpanjangan yang diizinkan untuk baris ini.
	 *
	 * @param jumlahMaxPerpanjangan batas perpanjangan.
	 */
	public void setJumlahMaxPerpanjangan(Integer jumlahMaxPerpanjangan) {
		this.jumlahMaxPerpanjangan = jumlahMaxPerpanjangan;
	}

	/**
	 * Mengembalikan tanggal eksemplar ini dikembalikan.
	 *
	 * <p>Bila baris pengembalian tertaut dan punya tanggal, nilainya <b>disalin ulang</b> ke
	 * field dari sana; bila tidak, nilai yang sudah tersimpan dipertahankan. Perhatikan
	 * perbedaan halus dengan {@link #getJumlahSelisihHari()}, yang pada kondisi yang sama
	 * justru <em>mengosongkan</em> field menjadi {@code null}. Karena kedua method menulis ke
	 * field yang sama, urutan pemanggilannya menentukan nilai apa yang akhirnya tersimpan
	 * &mdash; membaca {@code getJumlahSelisihHari()} setelah {@code getTanggalKembali()} dapat
	 * menghapus tanggal yang baru saja diisi manual lewat {@link #setTanggalKembali(Date)}.</p>
	 *
	 * <p>Dipetakan {@link TemporalType#DATE} sehingga hanya komponen tanggal yang tersimpan,
	 * berbeda dari {@link KembaliPengadaanItemDetail#getTanggal()} yang menyimpan cap waktu
	 * penuh.</p>
	 *
	 * @return tanggal pengembalian, atau {@code null} bila eksemplar belum kembali.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalKembali() {
		if (kembaliPengadaanItemDetail != null && kembaliPengadaanItemDetail.getTanggal() != null) {
			tanggalKembali = kembaliPengadaanItemDetail.getTanggal();
		}

		return tanggalKembali;
	}

	/**
	 * Menyetel tanggal eksemplar dikembalikan.
	 *
	 * <p>Nilai yang disetel di sini rapuh: {@link #getJumlahSelisihHari()} menimpanya tanpa
	 * syarat dari baris pengembalian yang tertaut (atau mengosongkannya bila tautan itu tidak
	 * ada). Untuk mencatat pengembalian secara andal, buat dan simpan
	 * {@link KembaliPengadaanItemDetail} lalu tautkan lewat
	 * {@link #setKembaliPengadaanItemDetail(KembaliPengadaanItemDetail)}.</p>
	 *
	 * @param tanggalKembali tanggal pengembalian.
	 */
	public void setTanggalKembali(Date tanggalKembali) {
		this.tanggalKembali = tanggalKembali;
	}

}
