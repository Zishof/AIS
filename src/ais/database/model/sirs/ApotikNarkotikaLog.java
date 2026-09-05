package ais.database.model.sirs;

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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Register penjualan obat terkendali -- narkotika/psikotropika (FASE A, kunci menu
 * {@code apotik_narkotika}; satu-satunya kebutuhan apotik TANPA padanan SIRS existing).
 *
 * <p>APPEND-ONLY: baris ditulis DALAM transaksi penjualan yang sama -- bila register tidak
 * bisa dibuat (data pembeli/dokter kurang), SELURUH penjualan di-rollback (transaksi DITAHAN,
 * bukan dilanjutkan diam-diam). Tidak ada aksi hapus; koreksi = catatan baru bertanda.</p>
 *
 * <h3>Mengapa register ini ada dan mengapa ia tidak boleh berlubang</h3>
 *
 * <p>Narkotika dan psikotropika adalah obat yang peredarannya diawasi negara.
 * Apotek wajib mencatat setiap penyerahannya — apa, berapa, kepada siapa, atas
 * resep dokter siapa — dan catatan itulah yang dicocokkan dengan stok fisik
 * ketika pemeriksaan dilakukan. Selisih antara stok yang berkurang dan register
 * yang tercatat berarti obat terkendali keluar tanpa jejak, dan itu persoalan
 * hukum, bukan persoalan pembukuan.</p>
 *
 * <p>Sifat itu menjelaskan bentuk penjagaannya yang tidak biasa. Pada hampir
 * seluruh entity lain di paket ini, data yang kurang lengkap menghasilkan
 * peringatan sementara pekerjaannya tetap berjalan. Di sini kebalikannya:
 * {@code ApotikApiHelper.bayar} MENAHAN seluruh penjualan — bukan hanya baris
 * obatnya — ketika nama pembeli kosong, atau ketika tidak ada resep maupun nama
 * dokter penulis. Penjualan yang gagal dicatat registernya tidak boleh terjadi
 * setengah, sebab setengah itulah yang berupa lubang: stok berkurang, register
 * kosong.</p>
 *
 * <p>Aturan yang sama ditegakkan {@code ApotikRacikanProduksiHelper} untuk
 * racikan yang memuat obat terkendali. Keduanya memakai satu predikat yang sama,
 * {@link ApotikItemProfile#terkendali(String)}, sehingga definisi "terkendali"
 * tidak dapat berbeda tergantung jalur mana yang diambil.</p>
 *
 * <h3>Seberapa kuat sifat append-only itu sesungguhnya — pembacaan jujur</h3>
 *
 * <p>Kalimat "tidak ada aksi hapus" pada paragraf pembuka benar untuk keadaan
 * sekarang, dan penting menyebut dengan tepat DARI MANA kekuatannya berasal,
 * supaya tidak ada yang mengira ada penjagaan yang sebenarnya tidak ada.</p>
 *
 * <p><b>Yang benar-benar menahan: ketiadaan jalur.</b>
 * {@code ApotikApiDispatcher} tidak mengenal satu pun aksi
 * {@code apotik_narkotika_*}. Baris hanya LAHIR di dua tempat — penjualan
 * ({@code ApotikApiHelper.bayar}) dan produksi racikan — dan hanya DIBACA di
 * satu tempat, {@code ApotikLaporanHelper.laporanTerkendali}. Tidak ada
 * formulir sunting, tidak ada aksi hapus, tidak ada pembatalan. Selama keadaan
 * itu bertahan, baris register tidak dapat diubah lewat aplikasi.</p>
 *
 * <p><b>Yang merekam tetapi tidak menahan: {@code @Audited}.</b> Envers menulis
 * salinan tiap revisi ke {@code new_audit.apotik_narkotika_log__audit}, sehingga
 * bentuk lama sebuah baris masih dapat dibaca andaikan ia diubah. Perlu
 * dinyatakan tegas: Envers MENCATAT, ia tidak MENOLAK. Ia tidak menghalangi
 * UPDATE maupun DELETE, dan ia sama sekali tidak berlaku bagi perubahan yang
 * dikerjakan langsung ke basis data lewat SQL — jalur yang justru paling
 * mungkin dipakai orang yang ingin menghilangkan jejak.</p>
 *
 * <p><b>Yang TIDAK ada.</b> Tidak ada trigger basis data yang menolak UPDATE
 * atau DELETE atas {@code sirs.apotik_narkotika_log}, tidak ada tanda tangan
 * atau rantai hash antar baris yang membuat penyuntingan dapat dideteksi, dan
 * tidak ada penomoran berurut yang lubangnya akan terlihat bila satu baris
 * dicabut. Untuk register yang menjadi bukti kepatuhan, ketiadaan itu berarti
 * sifat append-only bersandar sepenuhnya pada disiplin kode dan pada
 * pembatasan akses basis data — bukan pada penjagaan teknis yang berdiri
 * sendiri. Siapa pun yang menambahkan jalur ubah atau hapus akan meruntuhkan
 * satu-satunya lapis yang benar-benar menahan, dan ia akan runtuh tanpa
 * suara.</p>
 *
 * <p>Catatan ini ditulis bukan untuk menuntut perubahan segera, melainkan agar
 * pembaca berikutnya tahu persis apa yang ia andalkan ketika ia menyebut
 * register ini "append-only".</p>
 *
 * <h3>Hubungan dengan pelaporan wajib</h3>
 *
 * <p>Isi register ini dibaca {@code ApotikLaporanHelper.laporanTerkendali}
 * (aksi {@code apotik_laporan_terkendali}), yang menyajikan waktu, kode dan
 * nama item, golongan, kuantitas, identitas pembeli, nama dokter, keterangan,
 * dan pelaku — persis unsur-unsur yang dituntut register penyerahan narkotika.
 * Yang perlu diketahui adalah bahwa laporan itu berupa DAFTAR untuk dibaca
 * manusia, bukan berkas dalam format pelaporan resmi mana pun: tidak ada
 * pembangkit berkas untuk sistem pelaporan narkotika nasional, tidak ada
 * pengiriman otomatis, dan tidak ada penandaan periode mana yang sudah
 * dilaporkan. Laporannya juga dibatasi 1000 baris terakhir per pemanggilan,
 * sehingga periode yang ramai dapat terpotong tanpa pemberitahuan. Pelaporan
 * ke pihak berwenang, bila diperlukan, dikerjakan manusia dari daftar ini.</p>
 *
 * <p>Perlu dicatat pula bahwa daftar itu memuat nama dan alamat pembeli —
 * gabungan yang menyatakan siapa mengonsumsi obat golongan apa. Metode yang
 * menyajikannya tidak menerima parameter pengguna sama sekali dan karena itu
 * tidak melakukan pemeriksaan hak apa pun sendiri; gerbangnya hanyalah gerbang
 * menu kasar yang berjalan lebih dulu di lapisan pemanggil.</p>
 *
 * @see ApotikItemProfile#terkendali(String) predikat tunggal yang menentukan register ini wajib
 * @see ApotikBatchKonsumsi buku besar konsumsi lot yang menyertai penjualan yang sama
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_narkotika_log")
public class ApotikNarkotikaLog extends GeneralValueObject {

	/** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
	private static final long serialVersionUID = 1L;

	/** Kunci baris; dibangkitkan basis data. */
	private Long id;

	/** Item medis yang diserahkan. Wajib. */
	private ItemMedis item;

	/** Baris penjualan penyebab — penghubung ke transaksi dan pasiennya. Wajib. */
	private TransaksiMedisDetail transaksiDetail;

	/** Resep yang mendasari penyerahan; boleh kosong bila nama dokter diisi. */
	private Resep resep;

	/** Banyaknya yang diserahkan. */
	private Double qty;

	/** Snapshot golongan saat terjual -- profil item bisa berubah, register tidak. */
	private String golonganObat;

	/** Nama penerima obat. Wajib menurut penjagaan pemanggil. */
	private String namaPembeli;

	/** Alamat penerima obat. */
	private String alamatPembeli;

	/** Nama dokter penulis resep; wajib bila resep tidak ada. */
	private String namaDokter;

	/** Catatan bebas; satu-satunya tempat menandai koreksi. */
	private String keterangan;

	/** Waktu penyerahan. */
	private Date waktu;

	/** Nama tampil pelaku pencatatan (bayangan audit). */
	private String oleh;

	/** Identitas akun pelaku pencatatan (bayangan audit). */
	private String olehId;

	/**
	 * Kait JPA sebelum UPDATE: menyegarkan {@link #getTanggal_dirubah()}.
	 *
	 * <p>Tidak pernah berjalan pada keadaan sekarang, sebab tidak ada jalur yang
	 * menyunting baris register. Ia berguna justru sebagai jaring: bila suatu
	 * saat sebuah baris berubah — lewat jalur yang seharusnya tidak ada —
	 * stempel waktunya ikut bergerak, sehingga baris yang tanggal ubahnya
	 * berbeda jauh dari {@link #getWaktu()} menjadi tanda yang patut
	 * ditanyakan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Konstruktor tanpa argumen yang dituntut JPA.
	 *
	 * <p>Objek yang dihasilkan belum sah untuk disimpan: {@link #getItem()} dan
	 * {@link #getTransaksiDetail()} keduanya {@code nullable = false}.</p>
	 */
	public ApotikNarkotikaLog() {
	}

	/**
	 * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * <p>Berbeda dari sebagian entity apotek lain, kolomnya TIDAK
	 * {@code insertable = false} di sini. Perbedaan itu tanpa akibat praktis
	 * karena strategi {@code IDENTITY} tetap membiarkan basis data yang
	 * menentukan nilainya, tetapi patut disebut agar tidak dikira kelalaian
	 * pembacaan.</p>
	 *
	 * <p>Nilainya berurut naik dan karena itu dapat dipakai memilih baris
	 * terbaru; ia BUKAN penomoran register yang lubangnya dapat dipakai
	 * mendeteksi baris yang dicabut, sebab nomor identitas basis data memang
	 * boleh melompat karena sebab lain.</p>
	 *
	 * @return kunci baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci baris; dipakai Hibernate, bukan kode aplikasi.
	 *
	 * @param id kunci baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Item medis yang diserahkan.
	 *
	 * <p>Getter DESTRUKTIF: hasil {@code check(...)} ditulis balik ke field.
	 * {@code check} menormalkan proksi malas Hibernate yang sudah lepas dari
	 * sesinya menjadi {@code null}, mencegah {@code LazyInitializationException}
	 * ketika objek dibaca di luar sesi. Memanggilnya karena itu dapat mengubah
	 * keadaan objek dan bukan pembacaan murni.</p>
	 *
	 * <p>{@code nullable = false} — register yang tidak menyebut obat apa tidak
	 * bernilai apa pun.</p>
	 *
	 * <p>Perhatikan bahwa relasi ini menunjuk item medisnya, BUKAN golongannya:
	 * golongan yang berlaku saat penyerahan disimpan terpisah di
	 * {@link #getGolonganObat()} justru supaya perubahan profil item di kemudian
	 * hari tidak mengubah bunyi register yang sudah tercatat. Membaca golongan
	 * lewat {@code item} akan mengembalikan golongan HARI INI, bukan golongan
	 * pada hari obat itu diserahkan.</p>
	 *
	 * <p>{@code CascadeType.PERSIST}/{@code MERGE} tanpa {@code REMOVE}:
	 * menghapus baris register tidak akan pernah ikut menghapus item medisnya.</p>
	 *
	 * @return item yang diserahkan, atau {@code null} bila proksinya lepas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = false)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan item medis yang diserahkan.
	 *
	 * @param item item medis; wajib terisi sebelum disimpan
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Baris penjualan penyebab — penghubung ke transaksi dan pasiennya.
	 *
	 * <p>Berlaku catatan getter destruktif yang sama seperti
	 * {@link #getItem()}.</p>
	 *
	 * <p>Inilah yang mengikat register kepada peristiwa nyata. Dari sini
	 * {@code TransaksiMedisDetail} membawa ke transaksinya, dan dari transaksi ke
	 * seluruh keterangan penjualan — termasuk pembayarannya
	 * ({@link ApotikPembayaranTransaksi}) dan lot mana yang dipakai
	 * ({@link ApotikBatchKonsumsi}). Rantai itu yang memungkinkan pemeriksaan
	 * dua arah: dari register ke stok, dan dari lot yang ditarik kembali ke
	 * penerimanya.</p>
	 *
	 * <p>{@code nullable = false}. Sekaligus perlu diketahui bahwa TIDAK ada
	 * batasan unik atas kolom ini: satu baris penjualan boleh punya lebih dari
	 * satu baris register. Kelonggaran itu diperlukan — koreksi dilakukan dengan
	 * catatan baru bertanda, bukan dengan menyunting yang lama — tetapi ia juga
	 * berarti pengiriman ulang permintaan yang gagal separuh dapat melahirkan
	 * register kembar untuk satu penyerahan yang sama. Laporan yang
	 * menjumlahkan kuantitas akan menghitungnya dua kali. Tidak ada penjagaan
	 * idempoten di jalur pembuatan; yang menahan hanyalah kenyataan bahwa
	 * penulisan register berada di dalam transaksi penjualan yang sama,
	 * sehingga kegagalan me-rollback keduanya sekaligus.</p>
	 *
	 * @return baris penjualan penyebab, atau {@code null} bila proksinya lepas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi_detail", nullable = false)
	public TransaksiMedisDetail getTransaksiDetail() {
		transaksiDetail = check(transaksiDetail);
		return transaksiDetail;
	}

	/**
	 * Menetapkan baris penjualan penyebab.
	 *
	 * @param transaksiDetail baris penjualan; wajib terisi sebelum disimpan
	 */
	public void setTransaksiDetail(TransaksiMedisDetail transaksiDetail) {
		this.transaksiDetail = transaksiDetail;
	}

	/**
	 * Resep yang mendasari penyerahan.
	 *
	 * <p>Berlaku catatan getter destruktif yang sama seperti
	 * {@link #getItem()}.</p>
	 *
	 * <p>Sengaja {@code nullable = true}, dan kelonggaran itu berpasangan dengan
	 * {@link #getNamaDokter()}: {@code ApotikApiHelper.bayar} menahan penjualan
	 * obat terkendali bila resep TIDAK ADA <em>dan</em> nama dokter kosong. Salah
	 * satu dari keduanya cukup. Bentuk itu mengikuti kenyataan bahwa apotek
	 * kadang menerima resep yang tidak terdaftar rapi di sistem sementara nama
	 * dokter penulisnya jelas tertera di kertasnya.</p>
	 *
	 * <p>Perlu diketahui akibatnya untuk kekuatan register: cabang nama dokter
	 * adalah TEKS yang diketik pengirim, tidak terhubung ke master dokter mana
	 * pun dan tidak diverifikasi. Register yang bersandar pada cabang itu
	 * membuktikan bahwa seseorang mengetikkan sebuah nama, bukan bahwa dokter
	 * bernama itu benar-benar menuliskan resepnya.</p>
	 *
	 * @return resep yang mendasari, atau {@code null} bila hanya nama dokter yang ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "resep", nullable = true)
	public Resep getResep() {
		resep = check(resep);
		return resep;
	}

	/**
	 * Menetapkan resep yang mendasari penyerahan.
	 *
	 * @param resep resep, boleh {@code null}
	 */
	public void setResep(Resep resep) {
		this.resep = resep;
	}

	/**
	 * Banyaknya yang diserahkan.
	 *
	 * <p>Mengembalikan {@code 0} bila kosong, bukan {@code null} — pilihan yang
	 * tepat untuk kolom yang selalu dijumlahkan, sebab satu {@code null} yang
	 * lolos ke penjumlahan Java akan menggagalkan seluruh perhitungan alih-alih
	 * menghasilkan angka yang meleset.</p>
	 *
	 * <p>Inilah angka yang dicocokkan dengan stok yang berkurang ketika
	 * pemeriksaan dilakukan. Pasangannya ada di {@link ApotikBatchKonsumsi} —
	 * kuantitas yang benar-benar diambil dari lot — dan keduanya ditulis dalam
	 * transaksi yang sama dari nilai yang sama. Yang perlu diketahui: TIDAK ada
	 * apa pun yang membandingkan keduanya belakangan. Kalau salah satu berubah
	 * kemudian, ketidakcocokannya tidak akan terdeteksi sistem.</p>
	 *
	 * <p>Nilai negatif tidak ditolak entity maupun basis data. Baris
	 * berkuantitas negatif akan MENGURANGI jumlah yang tampak terserahkan pada
	 * laporan register — bentuk yang, pada register yang gunanya justru
	 * membuktikan berapa banyak obat keluar, patut dicurigai bila ditemukan.</p>
	 *
	 * @return kuantitas yang diserahkan; {@code 0} bila kosong
	 */
	@Column(name = "qty")
	public Double getQty() {
		return qty == null ? Double.valueOf(0) : qty;
	}

	/**
	 * Menetapkan kuantitas yang diserahkan.
	 *
	 * @param qty kuantitas
	 */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/**
	 * Snapshot golongan obat pada saat penyerahan.
	 *
	 * <p>Salinan yang disengaja dari
	 * {@link ApotikItemProfile#getGolonganObat()} pada detik penjualan. Alasannya
	 * mengikat: profil item dapat disunting kapan saja lewat layar formularium,
	 * dan sebuah obat yang hari ini digolongkan NARKOTIKA dapat besok
	 * digolongkan lain. Kalau register membaca golongan lewat relasi item, maka
	 * mengubah profil hari ini akan mengubah bunyi seluruh register masa lalu —
	 * catatan yang seharusnya menjadi bukti berubah surut mengikuti perubahan
	 * data induknya, dan tidak ada yang menandai bahwa ia berubah.</p>
	 *
	 * <p>Karena itu perbedaan antara kolom ini dan golongan item hari ini BUKAN
	 * ketidakcocokan yang perlu "diperbaiki"; ia justru informasi, yaitu bahwa
	 * profil obat sudah berubah sejak penyerahan tersebut. Jangan pernah
	 * menyelaraskannya dengan master.</p>
	 *
	 * <p>Mengembalikan nilai apa adanya, termasuk {@code null}. Baris yang
	 * golongannya kosong tetap merupakan register yang sah — ia lahir dari jalur
	 * yang hanya berjalan untuk obat terkendali — tetapi akan luput dari
	 * penyaringan laporan yang menyebut golongan tertentu.</p>
	 *
	 * @return golongan pada saat penyerahan, atau {@code null}
	 */
	@Column(name = "golongan_obat", length = 30)
	public String getGolonganObat() {
		return golonganObat;
	}

	/**
	 * Menetapkan snapshot golongan obat.
	 *
	 * @param golonganObat golongan pada saat penyerahan
	 */
	public void setGolonganObat(String golonganObat) {
		this.golonganObat = golonganObat;
	}

	/**
	 * Nama penerima obat.
	 *
	 * <p>Unsur inti register: penyerahan obat terkendali harus menyebut kepada
	 * siapa obat diserahkan. {@code ApotikApiHelper.bayar} MENAHAN seluruh
	 * transaksi bila kolom ini kosong untuk item terkendali — salah satu dari
	 * sedikit tempat di modul ini yang menolak, bukan sekadar memperingatkan.</p>
	 *
	 * <p>Perlu diketahui bahwa penjagaan itu memeriksa keberadaan, bukan
	 * kebenaran. Nilainya teks bebas yang diketik pengirim; tidak terhubung ke
	 * data pasien mana pun, tidak dicocokkan dengan identitas apa pun, dan tidak
	 * ada bentuk yang dituntut. Sebuah nama yang tidak berarti apa-apa akan
	 * lolos dengan cara yang persis sama dengan nama yang sesungguhnya. Register
	 * ini karena itu membuktikan bahwa penyerahannya DICATAT, bukan bahwa
	 * penerimanya benar.</p>
	 *
	 * <p>Data pribadi. Bersama {@link #getAlamatPembeli()} dan golongan obatnya,
	 * baris ini menyatakan siapa mengonsumsi narkotika atau psikotropika —
	 * termasuk keterangan yang paling sensitif yang disimpan modul apotek.</p>
	 *
	 * @return nama penerima, atau {@code null}
	 */
	@Column(name = "nama_pembeli")
	public String getNamaPembeli() {
		return namaPembeli;
	}

	/**
	 * Menetapkan nama penerima obat.
	 *
	 * @param namaPembeli nama penerima
	 */
	public void setNamaPembeli(String namaPembeli) {
		this.namaPembeli = namaPembeli;
	}

	/**
	 * Alamat penerima obat.
	 *
	 * <p>Data pribadi; berlaku catatan kerahasiaan yang sama seperti
	 * {@link #getNamaPembeli()}. Berbeda dari nama, alamat TIDAK dituntut terisi
	 * oleh penjagaan mana pun — penjualan obat terkendali tetap berjalan dengan
	 * alamat kosong. Kolom {@code text} tanpa batas panjang praktis.</p>
	 *
	 * @return alamat penerima, atau {@code null}
	 */
	@Column(name = "alamat_pembeli", columnDefinition = "text")
	public String getAlamatPembeli() {
		return alamatPembeli;
	}

	/**
	 * Menetapkan alamat penerima obat.
	 *
	 * @param alamatPembeli alamat penerima
	 */
	public void setAlamatPembeli(String alamatPembeli) {
		this.alamatPembeli = alamatPembeli;
	}

	/**
	 * Nama dokter penulis resep.
	 *
	 * <p>Alternatif yang sah bagi {@link #getResep()}: penjualan obat terkendali
	 * ditahan hanya bila KEDUANYA kosong. Teks bebas yang diketik pengirim,
	 * tidak terhubung ke master dokter dan tidak diverifikasi — lihat catatan
	 * pada {@link #getResep()} tentang apa arti hal itu bagi kekuatan register
	 * yang bersandar pada cabang ini.</p>
	 *
	 * @return nama dokter penulis resep, atau {@code null}
	 */
	@Column(name = "nama_dokter")
	public String getNamaDokter() {
		return namaDokter;
	}

	/**
	 * Menetapkan nama dokter penulis resep.
	 *
	 * @param namaDokter nama dokter
	 */
	public void setNamaDokter(String namaDokter) {
		this.namaDokter = namaDokter;
	}

	/**
	 * Catatan bebas — satu-satunya tempat menandai koreksi.
	 *
	 * <p>Kolom ini memikul beban yang lebih besar daripada yang tampak dari
	 * namanya. Karena register bersifat append-only dan koreksi dilakukan dengan
	 * "catatan baru bertanda", maka penanda itu tidak punya kolom sendiri —
	 * ia berupa teks di sini. Akibatnya, hubungan antara sebuah baris koreksi
	 * dan baris yang dikoreksinya hanya hidup dalam kalimat yang ditulis
	 * manusia: tidak ada kolom yang menunjuk baris yang diralat, tidak ada
	 * penanda jenis baris, dan tidak ada satu pun tempat yang dapat menyaring
	 * "koreksi" dari "penyerahan sesungguhnya".</p>
	 *
	 * <p>Yang perlu diketahui pembuat laporan: menjumlahkan {@link #getQty()}
	 * seluruh baris akan menghitung baris koreksi sebagai penyerahan tambahan,
	 * kecuali koreksinya ditulis dengan kuantitas berlawanan tanda — kesepakatan
	 * yang tidak ditegakkan apa pun. Bila kelak koreksi menjadi hal yang lazim,
	 * kolom penanda yang tegas jauh lebih baik daripada mengandalkan pembacaan
	 * teks.</p>
	 *
	 * <p>Kolom {@code text}. Isinya diambil pemanggil dari
	 * {@code keterangan_terkendali} pada payload penjualan.</p>
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan catatan bebas.
	 *
	 * @param keterangan keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Waktu penyerahan.
	 *
	 * <p>Mengembalikan waktu SEKARANG bila field kosong, bukan {@code null}.
	 * Perilaku itu perlu dipahami dengan tepat, dan pada register ia lebih
	 * berbahaya daripada di tempat lain: nilai pengganti hanya hidup di memori,
	 * berubah setiap kali dipanggil, dan TIDAK ditulis balik ke field. Baris
	 * yang kolom {@code waktu}-nya kosong karena itu akan tampak "terjadi
	 * barusan" setiap kali dibaca, dan tampak berpindah setiap pembacaan
	 * berikutnya.</p>
	 *
	 * <p>Untuk register yang gunanya menyatakan KAPAN obat terkendali
	 * diserahkan, sifat itu tidak dapat diterima bila sampai terjadi. Yang
	 * menahan adalah pemanggil: baik {@code bayar} maupun produksi racikan
	 * selalu mengisi waktu secara eksplisit, sehingga baris yang lahir dari
	 * jalur normal tidak pernah berkolom kosong. Jalur baru mana pun wajib
	 * melakukan hal yang sama.</p>
	 *
	 * <p>Perhatikan pula bahwa laporan register menyaring periode dengan SQL
	 * atas kolom {@code waktu} apa adanya — di sana kosong tetap kosong, dan
	 * baris berkolom kosong TIDAK akan muncul pada periode mana pun. Getter dan
	 * kolom karena itu menjawab bertentangan untuk baris yang sama: getter
	 * mengatakan "hari ini", laporan mengatakan "tidak ada".</p>
	 *
	 * @return waktu penyerahan, atau waktu sekarang bila field kosong
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menetapkan waktu penyerahan.
	 *
	 * @param waktu waktu penyerahan
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Nama tampil pelaku pencatatan (bayangan audit).
	 *
	 * <p>Pada register obat terkendali, inilah petugas yang menyerahkan obatnya —
	 * bagian dari isi register itu sendiri, bukan sekadar bayangan teknis.
	 * {@code ApotikLaporanHelper.laporanTerkendali} menyajikannya sebagai salah
	 * satu kolom laporan, sejajar dengan nama pembeli dan nama dokter.</p>
	 *
	 * @return nama pelaku, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama pelaku — MENGABAIKAN nilai kosong, tidak menimpanya.
	 *
	 * <p>Menolak {@code null} dan teks berisi spasi saja secara diam. Bentuk ini
	 * seragam di basis kode dan merupakan KEHARUSAN TEKNIS, bukan kelalaian yang
	 * menunggu diperbaiki.</p>
	 *
	 * <p>Kolom {@code oleh}/{@code oleh_id} adalah bayangan audit yang menempel
	 * pada barisnya sendiri, terpisah dari Envers. Entity di basis kode ini
	 * melewati banyak jalur yang menyalin properti secara membabi buta —
	 * pengikatan formulir, pemetaan dari JSON, penyalinan objek — dan sebagian
	 * memanggil setter dengan string kosong dari kolom yang tidak diisi. Kalau
	 * setter menurut, satu penyalinan lugu sudah cukup untuk mengganti nama
	 * petugas yang benar dengan ruang kosong, dan tidak ada tempat lain di baris
	 * itu yang menyimpan nilai sebelumnya.</p>
	 *
	 * <p>Di antara seluruh entity yang memakai pola ini, di sinilah
	 * pertaruhannya paling besar. Register penyerahan narkotika yang kehilangan
	 * nama petugasnya bukan sekadar catatan yang kurang lengkap — ia catatan
	 * yang tidak dapat dipertanggungjawabkan kepada siapa pun, tepat pada
	 * pertanyaan yang paling mungkin diajukan pemeriksa. Menolak penulisan
	 * kosong berarti jejak ini tidak dapat dikosongkan lagi lewat setter;
	 * satu-satunya cara adalah UPDATE langsung ke basis data. Untuk register
	 * kepatuhan, itu persis harga yang benar.</p>
	 *
	 * <p><b>Jangan "memperbaiki" setter ini</b> menjadi penetapan lugas karena
	 * tampak seperti anomali.</p>
	 *
	 * @param oleh nama pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Identitas akun pelaku pencatatan (bayangan audit).
	 *
	 * @return id akun pelaku, atau {@code null}
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id akun pelaku — MENGABAIKAN nilai kosong.
	 *
	 * <p>Berlaku seluruh pertimbangan pada {@link #setOleh(String)}, termasuk
	 * alasan mengapa pertaruhannya di entity ini paling besar.</p>
	 *
	 * @param olehId id akun pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Stempel perubahan terakhir.
	 *
	 * <p>Pada register yang tidak pernah disunting, nilainya selalu sama dengan
	 * waktu pembuatan baris. Justru karena itu ia berguna sebagai tanda: baris
	 * yang stempel ubahnya berbeda jauh dari {@link #getWaktu()} berarti pernah
	 * disentuh sesudah dicatat, dan pada register kepatuhan hal itu patut
	 * ditanyakan.</p>
	 *
	 * @return waktu ubah terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan stempel perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu ubah
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
