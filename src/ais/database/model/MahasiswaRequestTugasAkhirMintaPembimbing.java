package ais.database.model;

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

/**
 * Satu baris permintaan dosen pembimbing pada pengajuan tugas akhir seorang mahasiswa: pasangan
 * {@link MahasiswaRequestTugasAkhir} (pengajuannya) dengan {@link Dosen} (yang diminta), disertai
 * nama usulan dan keterangan.
 *
 * <h3>Mekanisme kedua, berdampingan dengan slot dosen1..dosen6 di induknya</h3>
 * <p>{@link MahasiswaRequestTugasAkhir} sudah menyimpan pembimbing yang diminta pada <b>enam slot
 * kolom tetap</b> {@code dosen1} sampai {@code dosen6}. Tabel ini adalah cara <i>kedua</i> untuk
 * menyatakan hal yang sama, kali ini sebagai baris-baris terpisah tanpa batas jumlah. Keduanya hidup
 * berdampingan dan <b>tidak ada apa pun yang menyinkronkan keduanya</b>: menambah baris di sini tidak
 * mengisi slot di induk, dan mengisi slot di induk tidak menghasilkan baris di sini. Kode yang ingin
 * mengetahui "siapa saja yang diminta" karenanya harus tahu mekanisme mana yang dipakai layar yang
 * bersangkutan; membaca salah satu saja dapat menghasilkan jawaban yang tidak lengkap.</p>
 *
 * <h3>Tautan ke pengajuan induk bersifat opsional</h3>
 * <p>{@link #getMahasiswaRequestTugasAkhir()} dipetakan {@code nullable = true}, sedangkan
 * {@link #getDosen()} wajib. Sebuah baris permintaan pembimbing karenanya dapat tersimpan
 * <b>tanpa pengajuan induk</b> — menjadi baris yatim yang menyebut seorang dosen tanpa konteks
 * mahasiswa mana pun. Baris seperti itu tidak akan muncul saat pengajuan ditelusuri dari induknya,
 * tetapi tetap terhitung oleh kode yang mencacah tabel ini secara langsung.</p>
 *
 * <p><b>Tidak ada batasan keunikan</b> pada pasangan pengajuan dan dosen, sehingga dosen yang sama
 * dapat diminta berkali-kali untuk pengajuan yang sama.</p>
 *
 * <h3>Catatan terkait pada induknya</h3>
 * <p>Sesi pendokumentasian sebelumnya mencatat sebuah bug ruang-id di
 * {@code MahasiswaRequestTugasAkhir#retreiveDetailVerifikasiNilai(...)} — id baris penghubung
 * dibandingkan dengan id komponen penilaian. Bug itu berada di jalur penilaian proposal dan
 * <b>tidak menyentuh kelas ini</b>; disebut di sini hanya agar keduanya tidak tertukar saat
 * menelusuri keluarga entity yang sama.</p>
 *
 * @see MahasiswaRequestTugasAkhir
 * @see Dosen
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "mahasiswa_request_tugas_akhir_minta_pembimbing")

public class MahasiswaRequestTugasAkhirMintaPembimbing extends GeneralValueObject {

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
	 * Representasi teks berupa nama usulan saja.
	 *
	 * <p>Dipakai komponen daftar dan kotak pilihan ZK. <b>Tidak menyebut dosen maupun pengajuan
	 * induknya</b>, sehingga dua baris permintaan untuk dosen berbeda pada pengajuan yang sama akan
	 * tampil identik di layar bila nama usulannya kebetulan sama.</p>
	 *
	 * <p>Membaca field {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga tidak
	 * ikut memangkas spasi. Mengembalikan {@code null} bila nama belum diisi.</p>
	 *
	 * @return nama usulan, atau {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Pengajuan tugas akhir yang menaungi permintaan ini; boleh kosong. Lihat {@link #getMahasiswaRequestTugasAkhir()}. */
	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;
	/** Dosen yang diminta menjadi pembimbing; wajib. Lihat {@link #getDosen()}. */
	private Dosen dosen;
	/** Nama usulan pada permintaan ini. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas mengenai permintaan ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public MahasiswaRequestTugasAkhirMintaPembimbing() {
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
	 * Nama usulan pada permintaan pembimbing ini — umumnya judul atau topik tugas akhir yang
	 * diajukan — sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Pemangkasan dilakukan saat membaca, bukan saat menyimpan. Nilai di basis data tetap membawa
	 * spasi apa adanya seperti yang dikirim {@link #setNama(String)}, sehingga kueri yang mencocokkan
	 * kolom {@code nama} secara langsung dapat gagal menemukan baris yang lewat getter ini terlihat
	 * cocok.</p>
	 *
	 * <p>Kolomnya dinyatakan {@code nullable = false} dengan panjang 255; keduanya tidak ditegakkan
	 * di Java dan baru ditolak basis data saat penyimpanan.</p>
	 *
	 * @return nama usulan tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama usulan pada permintaan ini.
	 *
	 * <p>Menyimpan nilai apa adanya, tanpa pemangkasan spasi dan tanpa pemeriksaan panjang.</p>
	 *
	 * @param nama nama usulan; tidak divalidasi
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas mengenai permintaan pembimbing ini.
	 *
	 * <p>Dikembalikan apa adanya, tanpa pemangkasan spasi — berbeda dari {@link #getNama()}. Tidak
	 * ada panjang maksimum yang dinyatakan, sehingga batasnya ditentukan tipe kolom di basis data.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan permintaan pembimbing.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}, tidak divalidasi
	 * @see #getKeterangan()
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Pengajuan tugas akhir yang menaungi permintaan pembimbing ini.
	 *
	 * <p>Dimuat secara {@code LAZY}, jadi yang tersimpan di field bisa berupa proksi Hibernate. Getter
	 * ini menyalurkannya lebih dulu lewat {@code check(...)}, yang berusaha menyelesaikan proksi itu
	 * dari cache identitas, dari session yang tersedia, atau dengan membuka session baru — dan bila
	 * semuanya gagal, mengembalikan objek apa adanya tanpa melempar. Itulah yang membuat properti ini
	 * tetap dapat dibaca dari objek yang sudah lepas dari session.</p>
	 *
	 * <p><b>Getter ini menulis ke field.</b> Hasil {@code check(...)} ditugaskan kembali ke field, dan
	 * objek yang dikembalikannya bisa merupakan instance Java yang berbeda untuk baris yang sama. Pada
	 * entity yang terikat session, pertukaran instance itu dapat terbaca sebagai perubahan properti
	 * oleh pemeriksaan kotor Hibernate, sehingga {@code UPDATE} diterbitkan dan — karena kelas ini
	 * {@code @Audited} — Envers mencatat revisi untuk perubahan yang tidak pernah diminta.</p>
	 *
	 * <p><b>Boleh {@code null}.</b> Lihat catatan baris yatim pada Javadoc kelas: permintaan
	 * pembimbing tanpa pengajuan induk dapat tersimpan dan tidak akan terlihat saat ditelusuri dari
	 * induknya.</p>
	 *
	 * @return pengajuan tugas akhir penaung, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa_request_tugas_akhir", nullable = true)
	public MahasiswaRequestTugasAkhir getMahasiswaRequestTugasAkhir() {
		mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
		return mahasiswaRequestTugasAkhir;
	}

	/**
	 * Menyetel pengajuan tugas akhir yang menaungi permintaan ini.
	 *
	 * <p>Tidak menyentuh slot {@code dosen1..dosen6} pada objek induk — lihat catatan dua mekanisme
	 * berdampingan pada Javadoc kelas. Menautkan baris ini ke sebuah pengajuan tidak membuat dosen
	 * yang diminta muncul pada slot-slot itu.</p>
	 *
	 * @param mahasiswaRequestTugasAkhir pengajuan penaung; boleh {@code null}
	 * @see #getMahasiswaRequestTugasAkhir()
	 */
	public void setMahasiswaRequestTugasAkhir(MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir) {
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
	}

	/**
	 * Dosen yang diminta menjadi pembimbing.
	 *
	 * <p>Relasi wajib — kolom {@code dosen} dinyatakan {@code nullable = false}. Berbeda dengan
	 * {@link #getMahasiswaRequestTugasAkhir()}, relasi ini dimuat dengan sifat bawaan
	 * {@code ManyToOne} — yaitu {@code EAGER} — lewat {@code SELECT} terpisah, dan getter-nya
	 * <b>tidak</b> menyalurkan hasil lewat {@code check(...)}. Dua relasi pada kelas yang sama
	 * karenanya berperilaku berbeda terhadap objek lepas-session.</p>
	 *
	 * <p>Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah dosen: menyimpan permintaan yang
	 * membawa objek dosen baru akan ikut menyimpan dosen itu. Tetapkan relasi dengan objek dosen yang
	 * sudah dimuat dari basis data, bukan dengan objek yang baru dibentuk di lapisan UI — pada entity
	 * yang menyebut nama dosen, membuat baris dosen bayangan secara tidak sengaja berakibat jauh.</p>
	 *
	 * @return dosen yang diminta; tidak seharusnya {@code null} pada baris yang tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		return dosen;
	}

	/**
	 * Menyetel dosen yang diminta menjadi pembimbing.
	 *
	 * <p>Menerima {@code null} tanpa keberatan meskipun kolomnya wajib; penolakan baru terjadi di
	 * basis data saat penyimpanan. Tidak memeriksa apakah dosen itu sudah diminta pada pengajuan yang
	 * sama, dan tidak memeriksa kuota bimbingan dosen tersebut.</p>
	 *
	 * @param dosen dosen yang diminta
	 * @see #getDosen()
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

}
