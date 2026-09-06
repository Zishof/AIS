package ais.database.model;

// Generated Dec 29, 2009 1:21:01 AM by Hibernate Tools 3.2.4.CR1

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

/**
 * Konsentrasi (peminatan) di dalam sebuah jurusan — cabang keahlian yang dapat dipilih mahasiswa,
 * misalnya "Rekayasa Perangkat Lunak" di bawah jurusan Teknik Informatika.
 *
 * <p>Entity master sederhana: sebuah nama, padanan nama dalam bahasa Inggris, dan relasi wajib ke
 * {@link Jurusan} yang menaunginya. Tidak memuat aturan perilaku apa pun.</p>
 *
 * <h3>Tanpa penanda aktif</h3>
 * <p>Kelas ini <b>tidak memiliki kolom {@code aktif}</b>. Konsentrasi yang sudah tidak dibuka lagi
 * hanya dapat dihapus, tidak dapat dipensiunkan — sementara menghapusnya akan memutus rujukan dari
 * mahasiswa angkatan lama yang pernah memilihnya. Bandingkan dengan {@link JenisEvaluasi} dan
 * {@link Asesor} di paket yang sama, yang keduanya menyediakan penanda tersebut.</p>
 *
 * <h3>Sisa fitur yang dinonaktifkan</h3>
 * <p>Field {@code dibukaUntukPMB} beserta pasangan getter dan setter-nya masih ada di berkas ini
 * dalam bentuk komentar, lengkap dengan pemetaan ke kolom {@code dibuka_untuk_pmb}. Fitur itu
 * dimaksudkan untuk menandai konsentrasi mana yang ditawarkan pada penerimaan mahasiswa baru.
 * Karena dinonaktifkan di lapisan Java tetapi kolomnya kemungkinan masih ada di basis data, nilai
 * yang tersimpan di sana tidak lagi terbaca maupun terpelihara oleh aplikasi. Jangan menghidupkannya
 * kembali tanpa memeriksa lebih dulu keadaan kolom itu di basis data yang berjalan.</p>
 *
 * @see Jurusan
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "konsentrasi")

public class Konsentrasi extends GeneralValueObject {

	/**
	 * 
	 */
	/** Penanda versi serialisasi Java; dikunci agar objek lama tetap terbaca setelah kelas diubah. */
	private static final long serialVersionUID = -6686134812861637295L;
	/** Kunci utama, dibangkitkan basis data. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Bagian dari trio jejak audit ringan {@code oleh}/{@code olehId}/{@code tanggal_dirubah} yang
	 * ditempelkan ke hampir seluruh entity paket ini. Jejak ini terpisah dari — dan jauh lebih miskin
	 * daripada — riwayat Envers yang dihasilkan anotasi {@code @Audited} pada kelas ini: Envers
	 * menyimpan setiap revisi, sedangkan trio ini hanya menyimpan pengubah terakhir.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}. Karena kelas ini memakai pemetaan berbasis
	 * properti, Hibernate tetap memperlakukannya sebagai properti yang dipersistensi dengan nama
	 * kolom bawaan. Jangan mengganti nama getter tanpa memeriksa nama kolom yang sebenarnya ada di
	 * basis data.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila baris belum pernah diubah lewat
	 *         jalur yang mengisinya
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir — <b>tetapi menolak nilai kosong secara diam-diam</b>.
	 *
	 * <p>Bila argumennya {@code null} atau hanya berisi spasi, method langsung selesai tanpa mengubah
	 * apa pun dan tanpa melempar. Akibatnya jejak audit ini bersifat <b>satu arah</b>: nilainya dapat
	 * ditimpa oleh id lain, tetapi <b>tidak pernah dapat dikosongkan kembali</b>. Sekali terisi, ia
	 * bertahan selamanya kecuali diganti dengan id yang lain.</p>
	 *
	 * <p>Dua akibat yang perlu diketahui pemanggil. Pertama, kode yang bermaksud membersihkan jejak —
	 * misalnya saat menganonimkan data atau menyalin baris sebagai cetakan baru — akan gagal tanpa
	 * pesan; baris salinan tetap membawa id pengubah dari baris asalnya. Kedua, karena penolakan itu
	 * senyap, pemanggil tidak dapat membedakan "berhasil disetel" dari "diabaikan"; periksa lewat
	 * {@link #getOlehId()} bila hasilnya penting.</p>
	 *
	 * <p>Pola yang sama dipakai {@link #setOleh(String)} dan berulang di hampir seluruh entity paket
	 * ini.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null} atau kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir — menolak nilai kosong secara diam-diam.
	 *
	 * <p>Berperilaku persis seperti {@link #setOlehId(String)}: {@code null} atau string kosong
	 * diabaikan tanpa pesan, sehingga jejak ini hanya dapat ditimpa dan tidak pernah dikosongkan.
	 * Lihat uraian lengkapnya di sana.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null} atau kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Pasangan {@link #getOlehId()} yang menyimpan nama, bukan id. Keduanya diisi terpisah dan
	 * <b>tidak ada yang menjamin keduanya menunjuk orang yang sama</b> — bila satu jalur hanya
	 * mengisi salah satunya, yang lain tetap membawa nilai lama. Untuk penelusuran yang andal, id
	 * lebih dapat dipercaya karena nama pengguna dapat berubah.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang memperbarui stempel waktu perubahan tepat sebelum baris disimpan.
	 *
	 * <p>Dijalankan Hibernate pada peristiwa {@code @PreUpdate} dan mendelegasikan pekerjaannya ke
	 * {@code AuditTimestampInterceptor.ubah(this)}. Karena kaitnya hanya {@code @PreUpdate} dan bukan
	 * {@code @PrePersist}, stempel waktu pada baris yang <b>baru dibuat</b> berasal dari nilai awal
	 * field — yaitu waktu objek Java dibentuk, bukan waktu penyimpanan. Untuk objek yang dibentuk
	 * lalu baru disimpan jauh kemudian, selisihnya nyata.</p>
	 *
	 * <p><b>Catatan bentuk kode:</b> deklarasi field {@code tanggal_dirubah} berbagi baris yang sama
	 * dengan method ini. Ini hasil penyisipan otomatis, bukan kesengajaan gaya. Field itu adalah
	 * stempel waktu perubahan terakhir dan nilai awalnya diambil dari {@code WaktuUtil.getDate()} —
	 * jam aplikasi, yang dapat berbeda dari jam basis data. Bila kedua jam itu tidak selaras, urutan
	 * kejadian yang tersusun dari kolom ini bisa keliru.</p>
	 *
	 * @see #getTanggal_dirubah()
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir secara langsung.
	 *
	 * <p>Berbeda dengan {@link #setOleh(String)} dan {@link #setOlehId(String)}, setter ini menerima
	 * {@code null} tanpa penolakan — jejak waktu <b>dapat</b> dikosongkan, sedangkan jejak pelakunya
	 * tidak. Ketimpangan itu berarti sebuah baris dapat berakhir dengan "siapa" yang terisi dan
	 * "kapan" yang kosong.</p>
	 *
	 * <p>Nilai yang disetel di sini akan ditimpa oleh {@link #onUpdate()} pada penyimpanan berikutnya,
	 * jadi menyetelnya secara manual hanya bermakna untuk impor data historis.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini, dengan ketelitian sampai detik.
	 *
	 * <p>Diperbarui otomatis oleh {@link #onUpdate()} pada setiap pembaruan. Mengembalikan objek
	 * {@link Date} yang dapat diubah — pemanggil yang memanggil {@code setTime(...)} pada hasilnya
	 * ikut mengubah keadaan entity ini. Salin dulu bila nilainya akan dimanipulasi.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibentuk karena
	 *         field-nya diberi nilai awal
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai komponen daftar dan kotak pilihan ZK, sehingga id ikut terlihat pengguna akhir.
	 * Menyertakan id di sini memang membedakan dua konsentrasi bernama sama — tidak ada batasan
	 * keunikan pada nama — tetapi juga membocorkan nomor internal ke layar.</p>
	 *
	 * <p>Membaca field {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga tidak
	 * ikut memangkas spasi seperti getter itu. Pada baris yang belum tersimpan, id masih
	 * {@code null} dan hasilnya diawali kata {@code "null-"}.</p>
	 *
	 * @return teks gabungan id dan nama konsentrasi
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama konsentrasi. Lihat {@link #getNama()}. */
	private String nama;
	/** Jurusan yang menaungi konsentrasi ini; wajib. Lihat {@link #getJurusan()}. */
	private Jurusan jurusan;
	/** Padanan nama konsentrasi dalam bahasa Inggris. Lihat {@link #getNamaEnglish()}. */
	private String namaEnglish;
	// private Integer dibukaUntukPMB;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public Konsentrasi() {
	}

	/**
	 * Konstruktor praktis yang hanya mengisi nama konsentrasi.
	 *
	 * <p><b>Objek yang dibentuk dengan konstruktor ini belum sah untuk disimpan</b>: relasi
	 * {@link #getJurusan()} dinyatakan {@code nullable = false} tetapi tidak diisi di sini, sehingga
	 * penyimpanan tanpa memanggil {@link #setJurusan(Jurusan)} lebih dulu akan ditolak basis data.
	 * Konstruktor ini karenanya lebih cocok untuk objek sementara — pembanding pencarian, nilai
	 * pengujian — daripada untuk membuat data baru.</p>
	 *
	 * @param nama nama konsentrasi; disimpan apa adanya tanpa pemangkasan spasi
	 */
	public Konsentrasi(String nama) {
		this.nama = nama;
	}

	/**
	 * Kunci utama baris ini, dibangkitkan basis data dengan strategi {@code IDENTITY}.
	 *
	 * <p>Bernilai {@code null} sampai entity benar-benar tersimpan. Karena strategi {@code IDENTITY}
	 * memerlukan penyisipan nyata untuk memperoleh nomor, Hibernate tidak dapat menunda
	 * {@code save(...)} pada entity ini sebagaimana yang dilakukannya untuk strategi berbasis
	 * urutan.</p>
	 *
	 * <p>Angka ini hanya unik di dalam tabelnya sendiri. Id yang sama muncul kembali di tabel lain
	 * untuk baris yang sama sekali berbeda, jadi jangan pernah membandingkan id lintas entity atau
	 * memakainya sebagai pengenal tunggal pada peta gabungan.</p>
	 *
	 * @return kunci utama, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama secara langsung.
	 *
	 * <p>Disediakan untuk Hibernate dan untuk alur impor data yang memuat objek lepas. <b>Jangan
	 * memanggilnya pada entity yang sedang terikat session</b>: mengubah pengenal objek yang dikelola
	 * membingungkan cache tingkat pertama dan dapat berujung pada pembaruan baris yang salah.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama konsentrasi, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Pemangkasan dilakukan <b>saat membaca, bukan saat menyimpan</b>. Nilai di basis data tetap
	 * membawa spasi apa adanya seperti yang dikirim {@link #setNama(String)}, sehingga kueri yang
	 * mencocokkan kolom {@code nama} secara langsung dapat gagal menemukan baris yang lewat getter
	 * ini terlihat cocok.</p>
	 *
	 * <p>Kolomnya tidak menyatakan panjang maupun {@code nullable}, jadi batasnya sepenuhnya
	 * ditentukan tipe kolom di basis data. <b>Tidak ada batasan keunikan</b>, baik secara global
	 * maupun per jurusan: dua konsentrasi bernama sama dalam satu jurusan dapat hidup
	 * berdampingan.</p>
	 *
	 * @return nama konsentrasi tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama konsentrasi.
	 *
	 * <p>Menyimpan nilai apa adanya, tanpa pemangkasan spasi dan tanpa pemeriksaan panjang. Lihat
	 * {@link #getNama()} untuk akibat ketidaksimetrisan itu.</p>
	 *
	 * @param nama nama konsentrasi; tidak divalidasi
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Jurusan yang menaungi konsentrasi ini.
	 *
	 * <p>Relasi wajib — kolom {@code jurusan} dinyatakan {@code nullable = false}. Inilah satu-satunya
	 * hal yang menempatkan konsentrasi ini di dalam struktur akademik, sehingga <b>penyaringan
	 * berdasarkan jurusan harus dilakukan setiap kali daftar konsentrasi ditampilkan</b>; kelas ini
	 * tidak melakukannya sendiri.</p>
	 *
	 * <p>Dimuat lewat {@code SELECT} terpisah, sehingga membacanya di dalam perulangan atas banyak
	 * konsentrasi menghasilkan pola N+1. Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah
	 * jurusan: menyimpan konsentrasi yang membawa objek jurusan baru akan ikut menyimpan jurusan itu,
	 * jadi tetapkan relasi dengan objek jurusan yang sudah dimuat dari basis data.</p>
	 *
	 * @return jurusan penaung; tidak seharusnya {@code null} pada baris yang tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = false)
	public Jurusan getJurusan() {
		return this.jurusan;
	}

	/**
	 * Menyetel jurusan yang menaungi konsentrasi ini.
	 *
	 * <p>Menerima {@code null} tanpa keberatan meskipun kolomnya wajib; penolakan baru terjadi di
	 * basis data saat penyimpanan.</p>
	 *
	 * <p><b>Memindahkan konsentrasi ke jurusan lain tidak memindahkan mahasiswa yang sudah
	 * memilihnya.</b> Rujukan dari data mahasiswa menunjuk konsentrasi ini, bukan pasangan
	 * jurusan-konsentrasi, sehingga mengubah nilai di sini membuat mahasiswa tersebut seolah memilih
	 * konsentrasi dari jurusan yang bukan jurusannya. Perlakukan sebagai perubahan data induk yang
	 * memerlukan penyesuaian menyeluruh, bukan sekadar penyuntingan biasa.</p>
	 *
	 * @param jurusan jurusan penaung
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Menyetel padanan nama konsentrasi dalam bahasa Inggris.
	 *
	 * @param namaEnglish nama dalam bahasa Inggris; boleh {@code null}, tidak divalidasi
	 * @see #getNamaEnglish()
	 */
	public void setNamaEnglish(String namaEnglish) {
		this.namaEnglish = namaEnglish;
	}

	/**
	 * Padanan nama konsentrasi dalam bahasa Inggris, dipakai pada dokumen dan transkrip berbahasa
	 * Inggris.
	 *
	 * <p>Bersifat pilihan dan sering kosong. <b>Berbeda dengan {@link #getNama()}, nilai di sini
	 * dikembalikan apa adanya tanpa pemangkasan spasi</b> — ketidakseragaman antara dua getter nama
	 * pada kelas yang sama. Penyaji yang menampilkan salah satunya secara bergantian akan melihat
	 * perlakuan spasi yang berbeda.</p>
	 *
	 * <p>Dibatasi 100 karakter, lebih pendek daripada {@link #getNama()} yang tidak dibatasi kelas
	 * ini. Batas itu tidak ditegakkan di Java; kelebihan panjang baru ditolak basis data.</p>
	 *
	 * @return nama dalam bahasa Inggris, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_english", nullable = true, length = 100)
	public String getNamaEnglish() {
		return namaEnglish;
	}

	// public void setDibukaUntukPMB(Integer dibukaUntukPMB) {
	// this.dibukaUntukPMB = dibukaUntukPMB;
	// }
	//
	// @Column(name = "dibuka_untuk_pmb", nullable = true, length = 1)
	// public Integer getDibukaUntukPMB() {
	// return dibukaUntukPMB;
	// }
}
