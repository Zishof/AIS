package ais.database.model;

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

import ais.common.Common;

/**
 * Catatan kegiatan mengajar seorang dosen di perguruan tinggi lain — dipakai pada pelaporan beban
 * kerja dosen dan penilaian angka kredit, tempat kegiatan di luar institusi sendiri tetap diakui.
 *
 * <p>Menyimpan nama kegiatan atau mata kuliah, nama perguruan tinggi tujuan, bobot SKS, serta
 * periode berupa tahun akademik dan semester.</p>
 *
 * <h3>Dua getter yang menulis nilai bawaan BERGANTUNG WAKTU</h3>
 * <p><b>Ini sifat paling berbahaya dari kelas ini.</b> {@link #getTahunAkademik()} dan
 * {@link #getSemester()} sama-sama menulis ke field saat dibaca bila nilainya masih {@code null} —
 * dan nilai yang dituliskan diambil dari <b>keadaan saat ini</b>: tahun akademik yang sedang
 * berjalan dan semester yang sedang berjalan.</p>
 * <p>Berbeda dari penjaga nilai bawaan yang lazim (misalnya {@code aktif = true}) yang nilainya
 * tetap, nilai di sini <b>bergantung pada kapan baris itu kebetulan pertama kali dibaca</b>. Sebuah
 * catatan mengajar dari tahun akademik lampau yang kolom periodenya kosong akan dicap dengan periode
 * <i>hari ini</i> pada pembacaan pertama, lalu — karena kelas ini dipetakan lewat akses properti dan
 * Hibernate memanggil getter saat menyimpan — cap itu ikut tersimpan. Dua baris yang isinya identik
 * dapat berakhir dengan periode berbeda hanya karena dibuka pada semester yang berbeda.</p>
 * <p>Karena tahun akademik dan semester justru merupakan <b>kunci pengelompokan</b> laporan beban
 * kerja, kekeliruan ini langsung memindahkan beban seorang dosen ke periode yang salah. Isi kedua
 * properti itu secara eksplisit saat membuat baris, dan jangan mengandalkan nilai bawaannya. Lihat
 * uraian rinci pada masing-masing getter.</p>
 *
 * <h3>Dosen boleh kosong</h3>
 * <p>{@link #getDosen()} dipetakan {@code nullable = true}, sehingga catatan mengajar dapat tersimpan
 * tanpa menyebut dosennya — beban kerja yang tidak dapat diatribusikan kepada siapa pun, tetapi tetap
 * terhitung oleh kode yang mencacah tabel ini.</p>
 *
 * @see Dosen
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "mengajar_di_perguruan_tinggi_lain")

public class MengajarDiPerguruanTinggiLain extends GeneralValueObject {

	/**
	 * 
	 */
	/** Penanda versi serialisasi Java; dikunci agar objek lama tetap terbaca setelah kelas diubah. */
	private static final long serialVersionUID = 2463821577548439808L;
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
	 * Representasi teks berupa nama kegiatan saja.
	 *
	 * <p>Dipakai komponen daftar dan kotak pilihan ZK. <b>Tidak menyebut dosen, perguruan tinggi
	 * tujuan, maupun periode</b> — padahal keempatnya yang membedakan satu catatan dari catatan
	 * lainnya. Mengajar mata kuliah bernama sama di dua perguruan tinggi berbeda, atau pada dua
	 * semester berbeda, akan tampil identik di layar.</p>
	 *
	 * <p>Membaca field {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga tidak
	 * ikut memangkas spasi. Sisi baiknya, karena tidak menyentuh properti periode, method ini bebas
	 * dari efek samping penulisan yang mengintai {@link #getTahunAkademik()} dan
	 * {@link #getSemester()}.</p>
	 *
	 * @return nama kegiatan mengajar, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/** Nama kegiatan atau mata kuliah yang diajarkan. Lihat {@link #getNama()}. */
	private String nama;
	/** Dosen yang mengajar; boleh kosong. Lihat {@link #getDosen()}. */
	private Dosen dosen;

	/** Nama perguruan tinggi tempat mengajar, sebagai teks bebas. Lihat {@link #getNamaPerguruanTinggi()}. */
	private String namaPerguruanTinggi;
	/** Bobot SKS kegiatan mengajar ini. Lihat {@link #getSks()}. */
	private Integer sks;

	/** Tahun akademik kegiatan; nilai bawaannya bergantung waktu. Lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/** Semester kegiatan; nilai bawaannya bergantung waktu. Lihat {@link #getSemester()}. */
	private String semester;
	/** Keterangan bebas mengenai kegiatan ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public MengajarDiPerguruanTinggiLain() {
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
	 * Nama kegiatan atau mata kuliah yang diajarkan, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Pemangkasan dilakukan saat membaca, bukan saat menyimpan, sehingga nilai di basis data tetap
	 * membawa spasi apa adanya. Kolomnya {@code nullable = false} dengan panjang 255; keduanya tidak
	 * ditegakkan di Java.</p>
	 *
	 * @return nama kegiatan tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kegiatan atau mata kuliah yang diajarkan.
	 *
	 * @param nama nama kegiatan; disimpan apa adanya, tidak divalidasi
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas mengenai kegiatan mengajar ini.
	 *
	 * <p>Dikembalikan apa adanya, tanpa pemangkasan spasi — berbeda dari {@link #getNama()}.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan kegiatan mengajar.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}, tidak divalidasi
	 * @see #getKeterangan()
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Semester kegiatan mengajar ini — {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP}.
	 *
	 * <p><b>Getter ini menulis ke field saat dibaca, dan nilai yang ditulisnya bergantung pada waktu
	 * pemanggilan.</b> Bila {@code semester} masih {@code null}, ia mengisinya dengan semester yang
	 * <i>sedang berjalan hari ini</i>, lalu mengembalikannya.</p>
	 *
	 * <p>Ini lebih berbahaya daripada penjaga nilai bawaan biasa. Nilai seperti {@code aktif = true}
	 * selalu sama kapan pun dibaca; nilai di sini berbeda-beda tergantung kapan baris itu pertama kali
	 * dibuka. Sebuah catatan mengajar dari semester ganjil yang periodenya belum diisi akan dicap
	 * "genap" bila kebetulan baru dibaca pada semester genap — dan cap itu ikut tersimpan, karena
	 * kelas ini dipetakan lewat akses properti sehingga Hibernate memanggil getter ini saat menyimpan.
	 * Karena kelas ini juga {@code @Audited}, Envers akan mencatatnya sebagai revisi yang seolah-olah
	 * dilakukan seseorang.</p>
	 *
	 * <p>Semester adalah kunci pengelompokan laporan beban kerja dosen, sehingga cap yang keliru
	 * memindahkan beban ke periode yang salah tanpa jejak niat siapa pun. <b>Isi properti ini secara
	 * eksplisit lewat {@link #setSemester(String)} saat membuat baris</b>, dan hindari membaca getter
	 * ini pada baris lama yang periodenya diketahui kosong sampai nilainya diperbaiki.</p>
	 *
	 * <p>Nilai disimpan sebagai teks bebas tanpa daftar tertutup, sehingga nilai di luar kedua
	 * konstanta itu juga dapat tersimpan.</p>
	 *
	 * @return semester kegiatan; semester yang sedang berjalan bila belum pernah diisi
	 */
	public String getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return semester;
	}

	/**
	 * Menyetel semester kegiatan mengajar.
	 *
	 * <p><b>Selalu panggil setter ini secara eksplisit saat membuat baris</b>, agar nilai bawaan yang
	 * bergantung waktu pada {@link #getSemester()} tidak sempat terpakai. Mengirim {@code null}
	 * mengembalikan properti ke jalur bawaan itu beserta seluruh akibatnya.</p>
	 *
	 * <p>Menerima teks apa pun tanpa pemeriksaan terhadap daftar semester yang sah.</p>
	 *
	 * @param semester semester kegiatan; tidak divalidasi
	 * @see #getSemester()
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * Tahun akademik kegiatan mengajar ini.
	 *
	 * <p><b>Getter ini menulis ke field saat dibaca, dan nilai yang ditulisnya bergantung pada waktu
	 * pemanggilan.</b> Bila {@code tahunAkademik} masih {@code null}, ia mengisinya dengan tahun
	 * akademik yang <i>sedang berjalan hari ini</i>, lalu mengembalikannya.</p>
	 *
	 * <p>Seluruh akibat yang diuraikan pada {@link #getSemester()} berlaku sama persis di sini, dan
	 * di properti ini dampaknya lebih besar: kekeliruan semester memindahkan beban satu periode,
	 * kekeliruan tahun akademik memindahkannya satu tahun penuh. Catatan mengajar lampau yang tahun
	 * akademiknya belum pernah diisi akan dicap dengan tahun berjalan pada pembacaan pertama, dan cap
	 * itu ikut tersimpan.</p>
	 *
	 * <p><b>Isi properti ini secara eksplisit lewat {@link #setTahunAkademik(String)} saat membuat
	 * baris.</b> Disimpan sebagai teks bebas tanpa pemeriksaan bentuk.</p>
	 *
	 * @return tahun akademik kegiatan; tahun akademik berjalan bila belum pernah diisi
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik kegiatan mengajar.
	 *
	 * <p><b>Selalu panggil setter ini secara eksplisit saat membuat baris</b>, agar nilai bawaan yang
	 * bergantung waktu pada {@link #getTahunAkademik()} tidak sempat terpakai.</p>
	 *
	 * <p>Menerima teks apa pun tanpa pemeriksaan bentuk maupun rujukan ke daftar tahun akademik yang
	 * ada.</p>
	 *
	 * @param tahunAkademik tahun akademik; tidak divalidasi
	 * @see #getTahunAkademik()
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Dosen yang melaksanakan kegiatan mengajar ini.
	 *
	 * <p><b>Boleh kosong</b> ({@code nullable = true}) meskipun inilah pemilik beban kerja yang
	 * dicatat. Baris tanpa dosen adalah beban yang tidak dapat diatribusikan kepada siapa pun, tetapi
	 * tetap terhitung oleh kode yang mencacah tabel ini. Untuk data yang menjadi dasar penilaian
	 * angka kredit, kelonggaran itu perlu ditutup di lapisan action yang menyimpan.</p>
	 *
	 * <p>Ini pula pembatas kepemilikan sesungguhnya pada entity ini, dan kelas ini tidak menyaring apa
	 * pun berdasarkan nilainya — pemeriksaan bahwa pengguna berhak melihat atau mengubah catatan milik
	 * dosen tertentu harus dilakukan di lapisan action. Perhatikan pula bahwa {@link #toString()}
	 * tidak menyebut dosen, sehingga kekeliruan kepemilikan tidak terlihat dari layar daftar.</p>
	 *
	 * <p>Dimuat {@code EAGER} lewat {@code SELECT} terpisah — pola N+1 pada daftar. Riam
	 * {@code PERSIST} dan {@code MERGE} berlaku ke arah dosen.</p>
	 *
	 * @return dosen pelaksana, atau {@code null} bila tidak dicatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		return dosen;
	}

	/**
	 * Menyetel dosen pelaksana kegiatan mengajar ini.
	 *
	 * @param dosen dosen pelaksana; boleh {@code null}
	 * @see #getDosen()
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Nama perguruan tinggi tempat dosen ini mengajar.
	 *
	 * <p>Disimpan sebagai <b>teks bebas</b>, bukan rujukan ke tabel perguruan tinggi mana pun.
	 * Konsekuensinya institusi yang sama dapat tertulis dengan berbagai ejaan dan singkatan, sehingga
	 * pengelompokan atau penghitungan per institusi tidak dapat diandalkan tanpa penyeragaman
	 * manual.</p>
	 *
	 * <p>Dikembalikan apa adanya — tanpa pemangkasan spasi, berbeda dari {@link #getNama()}. Getter
	 * ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code namaPerguruanTinggi} secara bawaan.</p>
	 *
	 * @return nama perguruan tinggi, atau {@code null} bila belum diisi
	 */
	public String getNamaPerguruanTinggi() {
		return namaPerguruanTinggi;
	}

	/**
	 * Menyetel nama perguruan tinggi tempat mengajar.
	 *
	 * @param namaPerguruanTinggi nama institusi; disimpan apa adanya, tidak divalidasi
	 * @see #getNamaPerguruanTinggi()
	 */
	public void setNamaPerguruanTinggi(String namaPerguruanTinggi) {
		this.namaPerguruanTinggi = namaPerguruanTinggi;
	}

	/**
	 * Bobot SKS kegiatan mengajar ini; {@code null} dibaca sebagai {@code 0}.
	 *
	 * <p><b>Pembacaan murni</b> — memakai ternary tanpa menyentuh field, berbeda dari
	 * {@link #getSemester()} dan {@link #getTahunAkademik()} pada kelas yang sama. Ketiga getter
	 * berpenjaga-default itu berdampingan, dan hanya yang ini yang bebas efek samping.</p>
	 *
	 * <p>Nilai jatuh-tempo {@code 0} berarti kegiatan yang SKS-nya belum diisi <b>tidak menyumbang
	 * beban apa pun</b> pada rekapitulasi, tanpa peringatan bahwa datanya belum lengkap — pada
	 * pelaporan beban kerja, baris yang terlewat lebih baik dikenali daripada dihitung nol. Tidak ada
	 * pembatasan rentang: nilai negatif akan diterima dan justru mengurangi total.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code sks} secara bawaan.</p>
	 *
	 * @return bobot SKS; {@code 0} bila belum diisi
	 */
	public Integer getSks() {
		return sks == null ? 0 : sks;
	}

	/**
	 * Menyetel bobot SKS kegiatan mengajar ini.
	 *
	 * @param sks bobot SKS; boleh {@code null}, dibaca sebagai {@code 0}; tidak divalidasi
	 * @see #getSks()
	 */
	public void setSks(Integer sks) {
		this.sks = sks;
	}

}
