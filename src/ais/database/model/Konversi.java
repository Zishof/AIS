package ais.database.model;

// Generated Dec 12, 2009 7:42:38 PM by Hibernate Tools 3.2.4.CR1

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
 * Satu baris konversi nilai: pengakuan sebuah mata kuliah yang pernah ditempuh mahasiswa di
 * institusi lain sebagai mata kuliah di kurikulum yang berlaku, beserta nilai yang diakui.
 *
 * <p>Menghubungkan {@link Mahasiswa}, {@link MatakuliahAwalKonversi} (mata kuliah asal), dan
 * {@link Matakuliah} (mata kuliah tujuan), dengan {@link #getNilaiAngka()} serta
 * {@link #getNilaiHuruf()} sebagai nilai yang diakui.</p>
 *
 * <p><b>Nama kelas dan nama tabel tidak sejalan:</b> kelas bernama {@code Konversi} sedangkan
 * tabelnya {@code konversi_temp}. Akhiran itu menyiratkan tabel sementara, tetapi entity ini
 * menyimpan hasil konversi yang diakui — bukan data buangan. Kueri SQL asli harus memakai nama tabel
 * tersebut.</p>
 *
 * <h3>Sisi asal boleh kosong, sisi tujuan tidak</h3>
 * <p>{@link #getMatakuliah()} (tujuan) dan {@link #getMahasiswa()} wajib, sedangkan
 * {@link #getMatakuliahAwalKonversi()} (asal) dan {@link #getJurusan()} boleh kosong. Sebuah baris
 * konversi karenanya dapat tersimpan <b>tanpa menyebut mata kuliah asal yang dikonversi</b> —
 * padahal justru pemadanan itulah alasan keberadaan entity ini. Baris seperti itu menjadi pengakuan
 * nilai tanpa dasar yang terekam, dan tidak ada yang dapat menelusurinya kembali. Untuk data yang
 * menentukan pengakuan beban studi, kelonggaran itu perlu ditutup di lapisan action yang menyimpan.</p>
 *
 * <p><b>Tidak ada batasan keunikan</b> pada pasangan mahasiswa dan mata kuliah tujuan, sehingga satu
 * mata kuliah dapat dikonversi berkali-kali untuk mahasiswa yang sama — dan bila SKS yang diakui
 * dijumlahkan, beban studi terhitung berlipat.</p>
 *
 * @see MatakuliahAwalKonversi
 * @see Matakuliah
 * @see Mahasiswa
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "konversi_temp")

public class Konversi extends GeneralValueObject {

	/**
	 * 
	 */
	/** Penanda versi serialisasi Java; dikunci agar objek lama tetap terbaca setelah kelas diubah. */
	private static final long serialVersionUID = -6353779208838329539L;
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
	 * Representasi teks berbentuk {@code "<matakuliah>_<jurusan>_<semester>"}.
	 *
	 * <p><b>Tidak menyebut mahasiswa sama sekali</b>, padahal setiap baris konversi adalah milik satu
	 * mahasiswa tertentu. Konversi mata kuliah yang sama untuk dua mahasiswa berbeda akan tampil
	 * identik di komponen daftar ZK dan di pesan log — justru pada entity yang menentukan pengakuan
	 * beban studi seseorang. Penyaji perlu menampilkan mahasiswa secara terpisah.</p>
	 *
	 * <p>Juga tidak menyebut {@link #getMatakuliahAwalKonversi()}, sehingga dua konversi dari mata
	 * kuliah asal berbeda ke mata kuliah tujuan yang sama pun tidak dapat dibedakan.</p>
	 *
	 * <p><b>Dapat memicu pemuatan lazy.</b> Dua bagian pertama memanggil {@code toString()} pada objek
	 * relasi; pada objek yang sudah lepas dari session hal itu dapat melempar
	 * {@code LazyInitializationException}. Tanpa penjagaan {@code null}: relasi yang kosong memunculkan
	 * kata {@code "null"} secara harfiah.</p>
	 *
	 * @return teks gabungan mata kuliah tujuan, jurusan, dan semester
	 */
	public String toString() {
		return matakuliah + "_" + jurusan + "_" + semester;
	}

	/** Mata kuliah tujuan di kurikulum yang berlaku; wajib. Lihat {@link #getMatakuliah()}. */
	private Matakuliah matakuliah;

	/** Jurusan pengelola konversi; boleh kosong. Lihat {@link #getJurusan()}. */
	private Jurusan jurusan;
	/** Semester tempat mata kuliah tujuan ditempatkan. Lihat {@link #getSemester()}. */
	private Integer semester;

	/** Nilai angka yang diakui; primitif, sehingga selalu bernilai. Lihat {@link #getNilaiAngka()}. */
	private double nilaiAngka;
	/** Nilai huruf yang diakui. Lihat {@link #getNilaiHuruf()}. */
	private String nilaiHuruf;

	/** Mata kuliah asal yang dikonversi; boleh kosong. Lihat {@link #getMatakuliahAwalKonversi()}. */
	private MatakuliahAwalKonversi matakuliahAwalKonversi;
	/** Mahasiswa pemilik konversi ini; wajib. Lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public Konversi() {
	}

	/**
	 * Konstruktor yang hanya mengisi kunci utama, untuk membentuk objek acuan.
	 *
	 * <p>Berguna sebagai pembanding pencarian atau sebagai acuan ringan ke sebuah baris tanpa
	 * memuatnya dari basis data.</p>
	 *
	 * <p><b>Jangan menyimpan objek yang dibentuk dengan konstruktor ini.</b> Seluruh field lainnya
	 * kosong — termasuk {@link #getMatakuliah()} dan {@link #getMahasiswa()} yang wajib — sedangkan
	 * {@link #getNilaiAngka()} bertipe primitif sehingga bernilai {@code 0.0}, bukan "tidak diisi".
	 * Menyerahkannya ke {@code merge}/{@code update} berisiko menimpa baris yang sudah ada dengan
	 * data kosong dan nilai nol.</p>
	 *
	 * @param id kunci utama baris yang diacu
	 */
	public Konversi(Long id) {
		this.id = id;
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
	 * Mata kuliah tujuan — mata kuliah di kurikulum yang berlaku yang diakui telah ditempuh lewat
	 * konversi ini.
	 *
	 * <p>Relasi wajib ({@code nullable = false}). Dimuat {@code EAGER} lewat {@code SELECT} terpisah,
	 * sehingga membacanya di dalam perulangan atas daftar konversi menghasilkan pola N+1.</p>
	 *
	 * <p><b>Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah mata kuliah.</b> Menyimpan baris
	 * konversi yang membawa objek mata kuliah baru akan ikut menyimpan mata kuliah itu — pada tabel
	 * kurikulum, memunculkan mata kuliah bayangan secara tidak sengaja berakibat jauh. Selalu tautkan
	 * objek yang sudah dimuat dari basis data.</p>
	 *
	 * @return mata kuliah tujuan; tidak seharusnya {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "matakuliah", nullable = false)
	public Matakuliah getMatakuliah() {
		return this.matakuliah;
	}

	/**
	 * Menyetel mata kuliah tujuan konversi.
	 *
	 * <p>Menerima {@code null} tanpa keberatan meskipun kolomnya wajib. Tidak memeriksa apakah mata
	 * kuliah ini sudah pernah dikonversi untuk mahasiswa yang sama, dan tidak memeriksa apakah SKS-nya
	 * sepadan dengan mata kuliah asal.</p>
	 *
	 * @param matakuliah mata kuliah tujuan
	 * @see #getMatakuliah()
	 */
	public void setMatakuliah(Matakuliah matakuliah) {
		this.matakuliah = matakuliah;
	}

	/**
	 * Semester tempat mata kuliah tujuan ditempatkan pada rencana studi mahasiswa.
	 *
	 * <p>Tanpa nilai jatuh-tempo dan tanpa pembatasan rentang; mengembalikan {@code null} apa adanya.
	 * Pemanggil wajib menjaga {@code null} sebelum membuka kotaknya.</p>
	 *
	 * @return nomor semester, atau {@code null} bila belum diisi
	 */
	@Column(name = "semester")
	public Integer getSemester() {
		return this.semester;
	}

	/**
	 * Menyetel semester penempatan mata kuliah tujuan.
	 *
	 * @param semester nomor semester; boleh {@code null}, tidak divalidasi
	 * @see #getSemester()
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Menyetel jurusan pengelola konversi ini.
	 *
	 * @param jurusan jurusan pengelola; boleh {@code null}
	 * @see #getJurusan()
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Jurusan yang mengelola konversi ini.
	 *
	 * <p><b>Boleh kosong</b> ({@code nullable = true}), berbeda dari
	 * {@code MatakuliahAwalKonversi.getJurusan()} yang mewajibkannya. Karena jurusan adalah pembatas
	 * cakupan yang lazim dipakai untuk menyaring daftar, baris konversi yang jurusannya kosong akan
	 * hilang dari penyaringan per jurusan — bukan muncul di semuanya. Nilai ini juga tidak diturunkan
	 * otomatis dari jurusan mahasiswa maupun dari mata kuliah tujuan, sehingga dapat berbeda dari
	 * keduanya tanpa ada yang memeriksanya.</p>
	 *
	 * <p>Dimuat {@code EAGER} lewat {@code SELECT} terpisah; riam {@code PERSIST} dan {@code MERGE}
	 * berlaku ke arah jurusan.</p>
	 *
	 * @return jurusan pengelola, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Menyetel nilai angka yang diakui.
	 *
	 * <p>Menerima {@code double} primitif, sehingga tidak ada cara menyatakan "belum dinilai" — lihat
	 * {@link #getNilaiAngka()}. Tidak ada pembatasan rentang: nilai negatif atau di atas skala
	 * maksimum akan diterima apa adanya, dan tidak ada pemeriksaan bahwa ia sejalan dengan
	 * {@link #getNilaiHuruf()}.</p>
	 *
	 * @param nilaiAngka nilai angka yang diakui; tidak divalidasi
	 */
	public void setNilaiAngka(double nilaiAngka) {
		this.nilaiAngka = nilaiAngka;
	}

	/**
	 * Nilai angka yang diakui untuk mata kuliah tujuan.
	 *
	 * <p><b>Bertipe {@code double} primitif, bukan {@link Double}.</b> Akibatnya kolom ini tidak
	 * pernah dapat bernilai kosong dari sisi Java: baris yang kolomnya {@code NULL} di basis data akan
	 * terbaca sebagai {@code 0.0}, dan objek baru pun dimulai dari {@code 0.0}. <b>"Belum dinilai" dan
	 * "dinilai nol" karenanya tidak dapat dibedakan</b> — perbedaan yang berarti pada data yang
	 * menentukan indeks prestasi. Bila perbedaan itu perlu, andalkan {@link #getNilaiHuruf()} yang
	 * dapat bernilai {@code null}, atau ubah tipenya menjadi {@link Double} (perubahan yang menyentuh
	 * pemetaan, jadi bukan penyuntingan sepele).</p>
	 *
	 * <p>Perlu dicatat pula bahwa pemetaan primitif membuat Hibernate gagal memuat baris yang kolomnya
	 * {@code NULL} pada sebagian konfigurasi; bila hal itu pernah terjadi, penyebabnya ada di sini.</p>
	 *
	 * @return nilai angka yang diakui; {@code 0.0} bila belum pernah diisi
	 */
	@Column(name = "nilai_angka")
	public double getNilaiAngka() {
		return nilaiAngka;
	}

	/**
	 * Menyetel nilai huruf yang diakui.
	 *
	 * <p>Menerima teks apa pun tanpa pemeriksaan terhadap skala huruf yang berlaku, dan tanpa
	 * memastikan kesesuaiannya dengan {@link #getNilaiAngka()}.</p>
	 *
	 * @param nilaiHuruf nilai huruf; boleh {@code null}, tidak divalidasi
	 * @see #getNilaiHuruf()
	 */
	public void setNilaiHuruf(String nilaiHuruf) {
		this.nilaiHuruf = nilaiHuruf;
	}

	/**
	 * Nilai huruf yang diakui untuk mata kuliah tujuan.
	 *
	 * <p>Disimpan sebagai teks bebas — bukan enum dan tidak dirujukkan ke tabel skala nilai mana pun.
	 * Tidak ada pemangkasan spasi dan tidak ada penyeragaman huruf besar-kecil, sehingga {@code "A"},
	 * {@code "a"}, dan {@code "A "} tersimpan sebagai tiga nilai berbeda.</p>
	 *
	 * <p><b>Tidak ada yang menjamin nilai ini sejalan dengan {@link #getNilaiAngka()}.</b> Keduanya
	 * disetel terpisah, dan tidak ada satu pun jalur di kelas ini yang menurunkan salah satu dari yang
	 * lain. Sebuah baris dapat berakhir dengan nilai angka tinggi dan nilai huruf rendah tanpa ada yang
	 * mengeluh.</p>
	 *
	 * <p>Berbeda dengan {@link #getNilaiAngka()}, properti ini <b>dapat</b> bernilai {@code null},
	 * sehingga inilah satu-satunya cara membedakan baris yang benar-benar belum dinilai.</p>
	 *
	 * @return nilai huruf, atau {@code null} bila belum diisi
	 */
	@Column(name = "nilai_huruf")
	public String getNilaiHuruf() {
		return nilaiHuruf;
	}

	/**
	 * Menyetel mata kuliah asal yang dikonversi.
	 *
	 * <p>Menerima {@code null}, yang berarti konversi tersimpan tanpa dasar yang terekam — lihat
	 * catatan pada Javadoc kelas. Tidak memeriksa bahwa SKS mata kuliah asal sepadan dengan mata
	 * kuliah tujuan.</p>
	 *
	 * @param matakuliahAwalKonversi mata kuliah asal; boleh {@code null}
	 * @see #getMatakuliahAwalKonversi()
	 */
	public void setMatakuliahAwalKonversi(MatakuliahAwalKonversi matakuliahAwalKonversi) {
		this.matakuliahAwalKonversi = matakuliahAwalKonversi;
	}

	/**
	 * Mata kuliah asal — catatan mata kuliah dari institusi sebelumnya yang menjadi dasar konversi
	 * ini.
	 *
	 * <p><b>Boleh kosong</b> ({@code nullable = true}) meskipun inilah sisi yang memberi makna pada
	 * seluruh baris. Konversi tanpa mata kuliah asal adalah pengakuan nilai tanpa dasar yang terekam,
	 * dan tidak ada yang dapat menelusurinya kembali. Lihat catatan pada Javadoc kelas.</p>
	 *
	 * <p>Dimuat {@code EAGER} lewat {@code SELECT} terpisah. Riam {@code PERSIST} dan {@code MERGE}
	 * berlaku ke arah mata kuliah asal — di sini riam itu justru berguna, karena mata kuliah asal
	 * memang lazim dibuat bersamaan dengan baris konversinya.</p>
	 *
	 * @return mata kuliah asal, atau {@code null} bila tidak dicatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "matakuliah_awal_konversi", nullable = true)
	public MatakuliahAwalKonversi getMatakuliahAwalKonversi() {
		return matakuliahAwalKonversi;
	}

	/**
	 * Menyetel mahasiswa pemilik konversi ini.
	 *
	 * <p>Menerima {@code null} tanpa keberatan meskipun kolomnya wajib. Tidak memeriksa bahwa jurusan
	 * mahasiswa sejalan dengan {@link #getJurusan()} maupun dengan jurusan mata kuliah tujuan.</p>
	 *
	 * @param mahasiswa mahasiswa pemilik
	 * @see #getMahasiswa()
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mahasiswa yang memperoleh pengakuan konversi ini.
	 *
	 * <p>Relasi wajib ({@code nullable = false}). <b>Inilah pembatas kepemilikan sesungguhnya pada
	 * entity ini</b>, dan kelas ini tidak melakukan penyaringan apa pun berdasarkan nilainya —
	 * pemeriksaan bahwa pengguna berhak melihat atau mengubah konversi milik mahasiswa tertentu harus
	 * dilakukan di lapisan action. Perhatikan pula bahwa {@link #toString()} tidak menyebut mahasiswa,
	 * sehingga kekeliruan kepemilikan tidak akan terlihat dari layar daftar maupun log.</p>
	 *
	 * <p>Dimuat {@code EAGER} lewat {@code SELECT} terpisah — pola N+1 pada daftar konversi. Riam
	 * {@code PERSIST} dan {@code MERGE} berlaku ke arah mahasiswa; pada tabel sebesar data mahasiswa,
	 * membuat baris bayangan secara tidak sengaja berakibat jauh.</p>
	 *
	 * @return mahasiswa pemilik; tidak seharusnya {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

}
