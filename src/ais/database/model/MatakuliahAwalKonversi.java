package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

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
 * Mata kuliah asal pada proses konversi nilai — catatan mata kuliah yang pernah ditempuh mahasiswa
 * di perguruan tinggi sebelumnya, yang akan dipadankan dengan mata kuliah di kurikulum yang berlaku.
 *
 * <p>Entity ini adalah <b>salinan mandiri</b>, bukan rujukan ke {@link Matakuliah}. Datanya —
 * {@link #getKode()}, {@link #getNama()}, {@link #getSks()}, {@link #getStatus()},
 * {@link #getSemester()} — sengaja disimpan sendiri karena mata kuliah asal berasal dari institusi
 * lain dan tidak akan pernah ada di tabel {@link Matakuliah} milik institusi ini. Pasangannya adalah
 * {@link Konversi}, yang menghubungkan baris di sini dengan mata kuliah tujuan.</p>
 *
 * <h3>Konsekuensi menjadi salinan mandiri</h3>
 * <p>Karena tidak merujuk apa pun, baris di sini tidak ikut berubah bila data mata kuliah di
 * institusi asal berubah — dan itu memang yang diinginkan untuk catatan riwayat. Sebaliknya, tidak
 * ada penjagaan konsistensi apa pun: kode yang sama dapat dimasukkan berkali-kali dengan nama, SKS,
 * atau semester yang berbeda-beda, dan tidak ada batasan keunikan pada {@link #getKode()} — baik
 * secara global maupun per jurusan.</p>
 *
 * <h3>Sisa fitur yang dinonaktifkan</h3>
 * <p>Relasi ke {@code Kurikulum} beserta getter dan setter-nya masih ada di berkas ini dalam bentuk
 * komentar, lengkap dengan pemetaan ke kolom {@code kurikulum}. Karena dinonaktifkan di lapisan Java
 * tetapi kolomnya kemungkinan masih ada di basis data, nilai yang tersimpan di sana tidak lagi
 * terbaca maupun terpelihara aplikasi. Periksa keadaan kolom itu di basis data yang berjalan sebelum
 * menghidupkannya kembali.</p>
 *
 * @see Konversi
 * @see Matakuliah
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "matakuliah_awal_konversi")

public class MatakuliahAwalKonversi extends GeneralValueObject {

	/**
	 * 
	 */
	/** Penanda versi serialisasi Java; dikunci agar objek lama tetap terbaca setelah kelas diubah. */
	private static final long serialVersionUID = 5368566040614586283L;
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
	 * Representasi teks berupa nama mata kuliah asal saja.
	 *
	 * <p>Dipakai komponen daftar dan kotak pilihan ZK. <b>Tidak menyebut kode maupun SKS</b>, padahal
	 * keduanya yang biasanya membedakan dua mata kuliah bernama mirip — dan karena tidak ada batasan
	 * keunikan pada nama, dua baris berbeda dapat tampil identik di layar pemilihan konversi.
	 * Pertimbangkan menampilkan kode di sisi penyaji.</p>
	 *
	 * <p>Membaca field {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga tidak
	 * ikut memangkas spasi. Mengembalikan {@code null} bila nama belum diisi.</p>
	 *
	 * @return nama mata kuliah asal, atau {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Kode mata kuliah di institusi asal. Lihat {@link #getKode()}. */
	private String kode;
	/** Nama mata kuliah di institusi asal. Lihat {@link #getNama()}. */
	private String nama;
	/** Bobot SKS mata kuliah asal. Lihat {@link #getSks()}. */
	private Integer sks;
	/**
	 * Status mata kuliah asal (wajib atau pilihan), dengan nilai awal {@code "Wajib"}.
	 *
	 * <p><b>Nilai awal diberikan di deklarasi field, bukan di getter.</b> Itu berarti objek yang baru
	 * dibentuk sudah berstatus {@code "Wajib"} sejak awal — berbeda dari pola nilai jatuh-tempo di
	 * getter yang dipakai entity lain di paket ini, dan tanpa efek samping penulisan saat dibaca.
	 * Untuk baris yang dimuat dari basis data, Hibernate menimpa nilai awal ini dengan isi kolom,
	 * termasuk bila kolomnya {@code null} — jadi baris lama dapat berstatus {@code null} meskipun
	 * field-nya punya nilai awal.</p>
	 *
	 * <p>Lihat {@link #getStatus()} mengenai ketiadaan pembatasan nilai yang sah.</p>
	 */
	private String status = "Wajib";
	/** Jurusan yang mengelola konversi ini; wajib. Lihat {@link #getJurusan()}. */
	private Jurusan jurusan;
	/** Semester penempuhan mata kuliah di institusi asal. Lihat {@link #getSemester()}. */
	private Integer semester;
	/** Singkatan nama mata kuliah asal. Lihat {@link #getSingkatan()}. */
	private String singkatan;

	// private Kurikulum kurikulum;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public MatakuliahAwalKonversi() {
	}

	/**
	 * Konstruktor praktis yang mengisi kode dan nama mata kuliah asal.
	 *
	 * <p><b>Objek yang dibentuk dengan konstruktor ini belum sah untuk disimpan</b>: relasi
	 * {@link #getJurusan()} dinyatakan {@code nullable = false} tetapi tidak diisi di sini, sehingga
	 * penyimpanan tanpa memanggil {@link #setJurusan(Jurusan)} lebih dulu akan ditolak basis data.
	 * Status akan bernilai awal {@code "Wajib"} dan SKS tetap {@code null}.</p>
	 *
	 * @param kode kode mata kuliah di institusi asal; disimpan apa adanya
	 * @param nama nama mata kuliah di institusi asal; disimpan apa adanya
	 */
	public MatakuliahAwalKonversi(String kode, String nama) {
		this.kode = kode;
		this.nama = nama;
	}

	/**
	 * Konstruktor praktis yang mengisi kode, nama, dan bobot SKS mata kuliah asal.
	 *
	 * <p>Seperti {@link #MatakuliahAwalKonversi(String, String)}, objek yang dihasilkan belum sah
	 * untuk disimpan sampai {@link #setJurusan(Jurusan)} diisi.</p>
	 *
	 * @param kode kode mata kuliah di institusi asal
	 * @param nama nama mata kuliah di institusi asal
	 * @param sks bobot SKS; boleh {@code null}, tidak divalidasi
	 */
	public MatakuliahAwalKonversi(String kode, String nama, Integer sks) {
		this.kode = kode;
		this.nama = nama;
		this.sks = sks;
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
	 * Kode mata kuliah sebagaimana dipakai institusi asal.
	 *
	 * <p><b>Dikembalikan apa adanya, tanpa pemangkasan spasi</b> — berbeda dari {@link #getNama()} di
	 * kelas yang sama yang memangkasnya. Karena kode inilah yang paling sering dipakai untuk
	 * mencocokkan mata kuliah asal, spasi tersembunyi di ujungnya akan membuat pencocokan gagal tanpa
	 * petunjuk yang terlihat di layar. Pangkas di sisi penulisan bila kecocokan persis dibutuhkan.</p>
	 *
	 * <p>Kolomnya dinyatakan {@code nullable = false} dengan panjang 100; keduanya tidak ditegakkan di
	 * Java. <b>Tidak ada batasan keunikan</b> — kode yang sama dapat muncul berkali-kali dengan nama
	 * dan SKS yang berbeda.</p>
	 *
	 * @return kode mata kuliah asal apa adanya, atau {@code null} bila belum diisi
	 */
	@Column(name = "kode", nullable = false, length = 100)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel kode mata kuliah asal.
	 *
	 * @param kode kode mata kuliah; disimpan apa adanya, tidak divalidasi
	 * @see #getKode()
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama mata kuliah asal, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Pemangkasan dilakukan saat membaca, bukan saat menyimpan, sehingga nilai di basis data tetap
	 * membawa spasi apa adanya. Perhatikan bahwa {@link #getKode()} di kelas yang sama <b>tidak</b>
	 * memangkas — dua getter berdampingan dengan perlakuan spasi yang berbeda.</p>
	 *
	 * <p>Kolomnya {@code nullable = false} tanpa batas panjang yang dinyatakan.</p>
	 *
	 * @return nama mata kuliah asal tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama mata kuliah asal.
	 *
	 * @param nama nama mata kuliah; disimpan apa adanya, tidak divalidasi
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Bobot SKS mata kuliah asal.
	 *
	 * <p><b>Tanpa nilai jatuh-tempo</b>: mengembalikan {@code null} apa adanya. Pemanggil wajib
	 * menjaga {@code null} sebelum menjumlahkan — menjumlahkan langsung ke dalam sebuah {@code int}
	 * akan melempar {@code NullPointerException}. Karena SKS asal biasanya dipakai untuk menentukan
	 * berapa banyak beban yang diakui pada konversi, baris yang SKS-nya kosong perlu ditangani secara
	 * sadar, bukan diperlakukan sebagai nol tanpa pemeriksaan.</p>
	 *
	 * <p>Tidak ada pembatasan rentang nilai; SKS negatif sekalipun akan diterima.</p>
	 *
	 * @return bobot SKS, atau {@code null} bila belum diisi
	 */
	@Column(name = "sks")
	public Integer getSks() {
		return this.sks;
	}

	/**
	 * Menyetel bobot SKS mata kuliah asal.
	 *
	 * @param sks bobot SKS; boleh {@code null}, tidak divalidasi
	 * @see #getSks()
	 */
	public void setSks(Integer sks) {
		this.sks = sks;
	}

	/**
	 * Menyetel status mata kuliah asal.
	 *
	 * <p>Menerima teks apa pun, termasuk {@code null} dan nilai di luar himpunan yang dikenali
	 * aplikasi. Lihat {@link #getStatus()}.</p>
	 *
	 * @param status status mata kuliah; tidak divalidasi
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Status mata kuliah asal — umumnya {@code "Wajib"} atau {@code "Pilihan"}.
	 *
	 * <p><b>Disimpan sebagai teks bebas, bukan enum.</b> Tidak ada daftar nilai yang sah di kelas ini
	 * maupun di basis data, tidak ada pemeriksaan huruf besar-kecil, dan tidak ada pemangkasan spasi.
	 * {@code "Wajib"}, {@code "wajib"}, dan {@code "Wajib "} akan tersimpan sebagai tiga nilai yang
	 * berbeda, dan kode yang membandingkannya dengan {@code equals} hanya akan mengenali salah
	 * satunya. Bila status ini dipakai untuk memutuskan sesuatu, bandingkan dengan
	 * {@code equalsIgnoreCase} pada nilai yang sudah dipangkas.</p>
	 *
	 * <p>Nilai awalnya {@code "Wajib"}, diberikan di deklarasi field. Panjang kolomnya dibatasi 10
	 * karakter, tetapi batas itu tidak ditegakkan di Java.</p>
	 *
	 * @return status mata kuliah; {@code "Wajib"} pada objek baru, dapat {@code null} pada baris lama
	 */
	@Column(name = "status", length = 10)
	public String getStatus() {
		return status;
	}

	/**
	 * Menyetel jurusan yang mengelola konversi ini.
	 *
	 * <p>Menerima {@code null} tanpa keberatan meskipun kolomnya wajib; penolakan baru terjadi di
	 * basis data saat penyimpanan.</p>
	 *
	 * @param jurusan jurusan pengelola
	 * @see #getJurusan()
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Jurusan yang mengelola konversi ini — bukan jurusan di institusi asal, melainkan jurusan tujuan
	 * di institusi ini.
	 *
	 * <p>Relasi wajib ({@code nullable = false}) dan satu-satunya pembatas cakupan pada entity ini,
	 * sehingga <b>penyaringan berdasarkan jurusan harus dilakukan setiap kali daftar mata kuliah asal
	 * ditampilkan</b>; kelas ini tidak melakukannya sendiri.</p>
	 *
	 * <p>Dimuat {@code EAGER} lewat {@code SELECT} terpisah — pola N+1 bila dibaca di dalam
	 * perulangan. Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah jurusan.</p>
	 *
	 * @return jurusan pengelola; tidak seharusnya {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = false)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Menyetel semester penempuhan mata kuliah di institusi asal.
	 *
	 * @param semester nomor semester; boleh {@code null}, tidak divalidasi
	 * @see #getSemester()
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Semester saat mata kuliah ini ditempuh di institusi asal.
	 *
	 * <p>Tanpa nilai jatuh-tempo dan tanpa pembatasan rentang; mengembalikan {@code null} apa adanya.</p>
	 *
	 * <p><b>Catatan pemetaan:</b> atribut {@code length = 3} pada anotasi kolom <b>tidak berpengaruh
	 * apa-apa</b> untuk properti bertipe {@link Integer} — {@code length} hanya bermakna bagi kolom
	 * berbasis teks. Nilainya karenanya tidak dibatasi tiga digit; anggapan sebaliknya akan keliru.</p>
	 *
	 * @return nomor semester, atau {@code null} bila belum diisi
	 */
	@Column(name = "semester", length = 3)
	public Integer getSemester() {
		return semester;
	}

	/**
	 * Menyetel singkatan nama mata kuliah asal.
	 *
	 * @param singkatan singkatan; boleh {@code null}, tidak divalidasi
	 * @see #getSingkatan()
	 */
	public void setSingkatan(String singkatan) {
		this.singkatan = singkatan;
	}

	/**
	 * Singkatan nama mata kuliah asal, dipakai bila lebar kolom laporan tidak memuat nama penuh.
	 *
	 * <p>Bersifat pilihan dan sering kosong; dikembalikan apa adanya tanpa pemangkasan spasi. Tidak
	 * ada jalur cadangan ke {@link #getNama()} — penyaji yang ingin selalu menampilkan sesuatu harus
	 * menyediakan cadangannya sendiri.</p>
	 *
	 * <p>Panjang kolomnya dibatasi 100 karakter, tidak ditegakkan di Java.</p>
	 *
	 * @return singkatan, atau {@code null} bila belum diisi
	 */
	@Column(name = "singkatan", length = 100)
	public String getSingkatan() {
		return singkatan;
	}

	// @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	// @Fetch(FetchMode.SELECT)@JoinColumn(name = "kurikulum", nullable = true)
	// public Kurikulum getKurikulum() {
	// return kurikulum;
	// }
	//
	// public void setKurikulum(Kurikulum kurikulum) {
	// this.kurikulum = kurikulum;
	// }

}
