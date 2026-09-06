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

/**
 * Cetakan (template) perkuliahan: kumpulan pengaturan bernama yang dapat dipakai ulang saat
 * menyiapkan perkuliahan baru, dengan cakupan opsional ke sebuah fakultas dan/atau jurusan.
 *
 * <p>Kelas ini adalah kepala dari pasangannya, {@code TemplatePerkuliahanDetail}, yang memuat baris
 * rincian cetakan. Entity ini sendiri hanya menyimpan identitas cetakan — nama, keterangan, dan
 * cakupan — tanpa satu pun aturan perilaku.</p>
 *
 * <h3>Cakupan fakultas dan jurusan tidak saling diperiksa</h3>
 * <p>{@link #getFakultas()} dan {@link #getJurusan()} keduanya {@code nullable} dan berdiri
 * sendiri-sendiri. Tidak ada apa pun di kelas ini — maupun pada anotasi tabelnya — yang memastikan
 * bahwa jurusan yang dipilih benar-benar bernaung di bawah fakultas yang dipilih. Sebuah cetakan
 * dapat tersimpan dengan pasangan fakultas dan jurusan yang tidak berhubungan, dan pemakainya tidak
 * akan diberi tahu. Karena kedua field itulah yang membatasi siapa yang melihat cetakan ini,
 * pemeriksaan kesesuaiannya harus ditegakkan di lapisan action yang menyimpan — dan penyaringan
 * berdasarkan keduanya juga harus dilakukan di sana, bukan diandalkan dari model.</p>
 *
 * <p>Keduanya bernilai {@code null} berarti cetakan berlaku umum. Perhatikan bahwa "berlaku umum"
 * dan "cakupannya belum diisi" tidak dapat dibedakan: sebuah cetakan yang lupa diberi cakupan akan
 * terlihat oleh semua orang.</p>
 *
 * <h3>Tanpa penanda aktif</h3>
 * <p>Berbeda dengan banyak entity master lain di paket ini, kelas ini <b>tidak memiliki kolom
 * {@code aktif}</b>. Cetakan yang tidak lagi dipakai hanya dapat dihapus, tidak dapat dipensiunkan
 * — dan penghapusan akan menghilangkan jejaknya dari baris yang pernah memakainya.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "template_perkuliahan")

public class TemplatePerkuliahan extends GeneralValueObject {

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
	 * Representasi teks berupa nama cetakan saja.
	 *
	 * <p>Dipakai komponen daftar dan kotak pilihan ZK, sehingga isinya terlihat pengguna akhir.
	 * Membaca field {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga
	 * <b>tidak</b> ikut memangkas spasi seperti getter itu — dua cetakan yang namanya hanya berbeda
	 * pada spasi di ujung akan tampil serupa di layar.</p>
	 *
	 * <p>Mengembalikan {@code null} bila nama belum diisi, bukan string kosong. Pemanggil yang
	 * menggabungkannya dengan teks lain akan memunculkan kata {@code "null"} secara harfiah.</p>
	 *
	 * @return nama cetakan, atau {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Nama cetakan perkuliahan. Lihat {@link #getNama()}. */
	private String nama;
	/** Fakultas yang menjadi cakupan cetakan ini; boleh kosong. Lihat {@link #getFakultas()}. */
	private Fakultas fakultas;
	/** Jurusan yang menjadi cakupan cetakan ini; boleh kosong. Lihat {@link #getJurusan()}. */
	private Jurusan jurusan;
	/** Keterangan bebas mengenai cetakan ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public TemplatePerkuliahan() {
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
	 * Nama cetakan perkuliahan, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Pemangkasan dilakukan <b>saat membaca, bukan saat menyimpan</b>. Nilai yang tersimpan di
	 * basis data tetap membawa spasi apa adanya seperti yang dikirim
	 * {@link #setNama(String)}. Akibatnya kueri yang mencocokkan kolom {@code nama} secara langsung
	 * — HQL maupun SQL asli — dapat gagal menemukan baris yang lewat getter ini terlihat cocok.
	 * Pangkas juga di sisi penulisan bila kecocokan persis dibutuhkan.</p>
	 *
	 * <p>Kolomnya dinyatakan {@code nullable = false} dengan panjang 255, tetapi kelas ini tidak
	 * menegakkan keduanya: nama {@code null} atau lebih panjang dari 255 baru ditolak oleh basis data
	 * saat penyimpanan. <b>Tidak ada batasan keunikan pada nama</b>, sehingga dua cetakan bernama
	 * sama dapat hidup berdampingan dan hanya dapat dibedakan pengguna lewat keterangannya.</p>
	 *
	 * @return nama cetakan tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama cetakan.
	 *
	 * <p>Menyimpan nilai apa adanya — tanpa pemangkasan spasi dan tanpa pemeriksaan panjang. Lihat
	 * {@link #getNama()} untuk akibat ketidaksimetrisan itu.</p>
	 *
	 * @param nama nama cetakan; tidak divalidasi
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas mengenai cetakan ini.
	 *
	 * <p>Berbeda dengan {@link #getNama()}, nilai di sini dikembalikan apa adanya tanpa pemangkasan
	 * spasi. Tidak ada panjang maksimum yang dinyatakan, sehingga batasnya ditentukan tipe kolom di
	 * basis data.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan cetakan.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}, tidak divalidasi
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel jurusan yang menjadi cakupan cetakan ini.
	 *
	 * <p><b>Tidak memeriksa bahwa jurusan ini bernaung di bawah {@link #getFakultas()}.</b> Pasangan
	 * yang tidak berhubungan akan diterima dan tersimpan; lihat uraian pada Javadoc kelas.</p>
	 *
	 * @param jurusan jurusan cakupan; {@code null} berarti tanpa pembatasan jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Jurusan yang menjadi cakupan cetakan ini.
	 *
	 * <p>Bernilai {@code null} berarti cetakan tidak dibatasi pada jurusan tertentu. Dimuat lewat
	 * {@code SELECT} terpisah, sehingga membacanya di dalam perulangan atas banyak cetakan
	 * menghasilkan pola N+1.</p>
	 *
	 * <p><b>Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah jurusan.</b> Menyimpan cetakan
	 * yang membawa objek jurusan baru akan ikut menyimpan jurusan itu. Tetapkan relasi dengan objek
	 * jurusan yang sudah dimuat dari basis data, bukan dengan objek yang baru dibentuk di lapisan
	 * UI.</p>
	 *
	 * @return jurusan cakupan, atau {@code null} bila cetakan berlaku lintas jurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Menyetel fakultas yang menjadi cakupan cetakan ini.
	 *
	 * <p>Tidak memeriksa kesesuaian dengan {@link #getJurusan()}; lihat uraian pada Javadoc kelas.</p>
	 *
	 * @param fakultas fakultas cakupan; {@code null} berarti tanpa pembatasan fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Fakultas yang menjadi cakupan cetakan ini.
	 *
	 * <p>Bernilai {@code null} berarti cetakan tidak dibatasi pada fakultas tertentu. Sifat pemuatan
	 * dan riam penyimpanannya sama persis dengan {@link #getJurusan()} — lihat catatan di sana.</p>
	 *
	 * @return fakultas cakupan, atau {@code null} bila cetakan berlaku lintas fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		return fakultas;
	}

}
