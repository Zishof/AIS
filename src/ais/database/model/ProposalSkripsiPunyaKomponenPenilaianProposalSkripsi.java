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

import org.hibernate.envers.Audited;

/**
 * Baris penghubung antara sebuah {@link FormatNilaiProposalSkripsi} dan sebuah
 * {@link KomponenPenilaianProposalSkripsi}: menyatakan bahwa komponen penilaian tertentu termasuk ke
 * dalam format nilai proposal skripsi tertentu, dengan nama dan keterangan yang dapat ditimpa
 * setempat.
 *
 * <p>Nama kelas dan nama tabelnya tidak sepenuhnya sejalan: kelas berakhiran
 * {@code ...ProposalSkripsi} sedangkan tabelnya bernama
 * {@code proposal_skripsi_punya_komponen_penilaian}. Kueri SQL asli harus memakai nama tabel itu.</p>
 *
 * <h3>Entity ini punya urutan id sendiri — jangan tertukar dengan id komponennya</h3>
 * <p>Sebagai tabel penghubung, baris di sini memiliki kunci utama yang berdiri sendiri, terpisah dari
 * id {@link KomponenPenilaianProposalSkripsi} yang ditunjuknya. Sesi pendokumentasian sebelumnya
 * menemukan bahwa {@code MahasiswaRequestTugasAkhir#retreiveDetailVerifikasiNilai(...)} menerima
 * baris <i>entity ini</i> lalu membandingkan {@code getId()}-nya dengan kolom pertama rincian nilai —
 * padahal kolom itu selalu diisi id {@link KomponenPenilaianProposalSkripsi}. Akibatnya bendera
 * verifikasi hanya terbaca benar bila kedua id kebetulan bernilai sama. Bug itu berada di kelas
 * tersebut, bukan di sini; disebut di sini karena entity inilah yang id-nya salah dipakai. Bila kelak
 * ada kode baru yang perlu mencocokkan rincian nilai, pakailah
 * {@code getKomponenPenilaianProposalSkripsi().getId()}, bukan {@link #getId()}.</p>
 *
 * <p>Perlu ditegaskan bahwa kelas ini <b>tidak</b> memiliki cacat penukaran slot dosen yang pernah
 * ditemukan pada {@code Skripsi} dan {@code FormatNilaiSkripsi} — entity ini tidak menyimpan dosen
 * sama sekali.</p>
 *
 * <h3>Tanpa batasan keunikan dan tanpa penanda aktif</h3>
 * <p>Tidak ada batasan yang mencegah komponen penilaian yang sama dimasukkan dua kali ke dalam format
 * nilai yang sama; bila bobot komponen dijumlahkan, entri ganda akan terhitung dua kali. Kelas ini
 * juga tidak memiliki kolom {@code aktif}, sehingga komponen yang tidak lagi dipakai hanya dapat
 * dihapus dari format — bukan dinonaktifkan.</p>
 *
 * @see FormatNilaiProposalSkripsi
 * @see KomponenPenilaianProposalSkripsi
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "proposal_skripsi_punya_komponen_penilaian")
public class ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi extends GeneralValueObject {

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
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
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
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	 * field {@code tanggal_dirubah} — yaitu waktu objek Java dibentuk, bukan waktu penyimpanan. Nilai
	 * awal itu diambil dari {@code WaktuUtil.getDate()}, jam aplikasi, yang dapat berbeda dari jam
	 * basis data; bila keduanya tidak selaras, urutan kejadian yang tersusun dari kolom ini bisa
	 * keliru.</p>
	 *
	 * @see #getTanggal_dirubah()
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir. Lihat {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * Representasi teks berupa nama setempat saja.
	 *
	 * <p>Membaca field {@code nama} secara langsung, <b>bukan</b> lewat {@link #getNama()}. Perbedaan
	 * itu penting di kelas ini: getter tersebut memiliki jalur cadangan yang mengambil nama dari
	 * komponen penilaian bila nama setempat kosong, sedangkan method ini tidak. Baris yang mengandalkan
	 * jalur cadangan itu karenanya tampil sebagai {@code null} di komponen daftar ZK meskipun
	 * {@link #getNama()} mengembalikan nama yang benar.</p>
	 *
	 * <p>Sisi baiknya, karena tidak menyentuh relasi, method ini bebas dari bahaya
	 * {@code NullPointerException} dan {@code LazyInitializationException} yang mengintai
	 * {@link #getNama()}.</p>
	 *
	 * @return nama setempat, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/** Nama setempat komponen pada format ini; bila kosong diambil dari komponennya. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas mengenai baris penghubung ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Format nilai proposal skripsi yang memuat komponen ini; wajib. Lihat {@link #getFormatNilaiProposalSkripsi()}. */
	private FormatNilaiProposalSkripsi formatNilaiProposalSkripsi;
	/** Komponen penilaian yang dimasukkan ke dalam format; wajib. Lihat {@link #getKomponenPenilaianProposalSkripsi()}. */
	private KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi;

	/**
	 * Komponen penilaian yang dimasukkan ke dalam format nilai ini.
	 *
	 * <p>Relasi wajib — kolom {@code komponen_penilaian_proposal_skripsi} dinyatakan
	 * {@code nullable = false}. Dimuat secara {@code LAZY}, sehingga yang tersimpan di field bisa
	 * berupa proksi Hibernate; getter ini menyalurkannya lebih dulu lewat {@code check(...)} yang
	 * berusaha menyelesaikan proksi itu dan, bila gagal, mengembalikan objek apa adanya tanpa
	 * melempar.</p>
	 *
	 * <p><b>Getter ini menulis ke field:</b> hasil {@code check(...)} ditugaskan kembali, dan instance
	 * yang dikembalikannya bisa berbeda untuk baris yang sama. Pada entity yang terikat session, hal
	 * itu dapat terbaca sebagai perubahan properti oleh pemeriksaan kotor Hibernate sehingga
	 * {@code UPDATE} diterbitkan dan Envers mencatat revisi yang tidak diminta.</p>
	 *
	 * <p><b>Id komponen inilah yang dipakai sebagai kunci rincian nilai</b> — bukan {@link #getId()}
	 * milik baris penghubung ini. Lihat catatan pada Javadoc kelas.</p>
	 *
	 * @return komponen penilaian; tidak seharusnya {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "komponen_penilaian_proposal_skripsi", nullable = false)
	public KomponenPenilaianProposalSkripsi getKomponenPenilaianProposalSkripsi() {
		komponenPenilaianProposalSkripsi = check(komponenPenilaianProposalSkripsi);
		return komponenPenilaianProposalSkripsi;
	}

	/**
	 * Menyetel komponen penilaian yang dimasukkan ke dalam format nilai ini.
	 *
	 * <p>Menerima {@code null} tanpa keberatan meskipun kolomnya wajib; penolakan baru terjadi di
	 * basis data. Tidak memeriksa apakah komponen yang sama sudah ada di dalam format tersebut.</p>
	 *
	 * <p>Menyetelnya menjadi {@code null} juga melumpuhkan jalur cadangan {@link #getNama()}: bila
	 * nama setempat ikut kosong, getter itu akan melempar {@code NullPointerException}.</p>
	 *
	 * @param komponenPenilaianProposalSkripsi komponen penilaian
	 * @see #getKomponenPenilaianProposalSkripsi()
	 */
	public void setKomponenPenilaianProposalSkripsi(KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi) {
		this.komponenPenilaianProposalSkripsi = komponenPenilaianProposalSkripsi;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi() {
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
	 * Nama komponen sebagaimana ditampilkan pada format nilai ini, dengan jalur cadangan ke nama
	 * komponen aslinya.
	 *
	 * <p>Bila nama setempat terisi, ia dikembalikan setelah dipangkas spasi. Bila kosong, yang
	 * dikembalikan adalah {@code komponenPenilaianProposalSkripsi.getNama()} — nama komponen aslinya,
	 * <b>tanpa</b> pemangkasan spasi. Rancangan ini memungkinkan satu komponen tampil dengan sebutan
	 * berbeda di format yang berbeda tanpa harus selalu diisi.</p>
	 *
	 * <h3>Tiga jebakan pada jalur cadangan itu</h3>
	 * <ol>
	 *   <li><b>{@code NullPointerException} saat menyimpan objek baru.</b> Field relasi dibaca
	 *       <b>langsung</b>, bukan lewat {@link #getKomponenPenilaianProposalSkripsi()}. Pada objek
	 *       yang baru dibentuk dan belum diberi komponen, membaca getter ini melempar. Karena kelas
	 *       ini dipetakan lewat akses properti, <b>Hibernate sendiri memanggil getter ini saat
	 *       menyimpan</b> — jadi menyimpan baris baru tanpa mengisi nama maupun komponen akan gagal
	 *       dengan {@code NullPointerException} dari dalam Hibernate, bukan dengan pesan validasi yang
	 *       jelas.</li>
	 *   <li><b>{@code LazyInitializationException} pada objek lepas-session.</b> Karena field dibaca
	 *       langsung, penyelesaian proksi lewat {@code check(...)} yang dilakukan getter relasi
	 *       <b>dilewati</b>. Relasi ini {@code LAZY}, sehingga membaca nama dari objek yang sudah lepas
	 *       dari session dapat melempar — tepat pada properti yang paling sering dibaca penyaji.</li>
	 *   <li><b>"Belum diberi nama" dan "namanya sama dengan komponen" tidak dapat dibedakan.</b>
	 *       Keduanya menghasilkan teks yang sama, sehingga kode yang ingin tahu apakah nama setempat
	 *       pernah diisi harus membaca field-nya lewat jalur lain.</li>
	 * </ol>
	 *
	 * <p>Kolomnya dinyatakan {@code nullable = false} dengan panjang 255; keduanya tidak ditegakkan di
	 * Java.</p>
	 *
	 * @return nama setempat yang sudah dipangkas, atau nama komponen aslinya bila nama setempat kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? komponenPenilaianProposalSkripsi.getNama() : this.nama.trim();
	}

	/**
	 * Menyetel nama setempat komponen pada format nilai ini.
	 *
	 * <p>Menyimpan nilai apa adanya, tanpa pemangkasan spasi dan tanpa pemeriksaan panjang. Mengirim
	 * {@code null} berarti mengaktifkan jalur cadangan {@link #getNama()} — beserta seluruh jebakannya.</p>
	 *
	 * @param nama nama setempat; {@code null} berarti memakai nama komponen aslinya
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas mengenai baris penghubung ini.
	 *
	 * <p>Dikembalikan apa adanya, tanpa pemangkasan spasi dan <b>tanpa jalur cadangan</b> ke komponen
	 * aslinya — berbeda dari {@link #getNama()}. Keterangan yang kosong tetap kosong.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan baris penghubung ini.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}, tidak divalidasi
	 * @see #getKeterangan()
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Format nilai proposal skripsi yang memuat komponen ini.
	 *
	 * <p>Relasi wajib — kolom {@code format_nilai_proposal_skripsi} dinyatakan
	 * {@code nullable = false}. Sifat pemuatannya sama dengan
	 * {@link #getKomponenPenilaianProposalSkripsi()}: {@code LAZY}, disalurkan lewat
	 * {@code check(...)}, dan hasilnya ditugaskan kembali ke field. Kedua relasi pada kelas ini
	 * karenanya berperilaku seragam — berbeda dari beberapa entity lain di paket ini yang mencampur
	 * gaya {@code LAZY} berpelindung dengan {@code EAGER} tanpa pelindung.</p>
	 *
	 * <p>Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah format nilai: menyimpan baris
	 * penghubung yang membawa objek format baru akan ikut menyimpan format itu.</p>
	 *
	 * @return format nilai proposal skripsi; tidak seharusnya {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_nilai_proposal_skripsi", nullable = false)
	public FormatNilaiProposalSkripsi getFormatNilaiProposalSkripsi() {
		formatNilaiProposalSkripsi = check(formatNilaiProposalSkripsi);
		return formatNilaiProposalSkripsi;
	}

	/**
	 * Menyetel format nilai proposal skripsi yang memuat komponen ini.
	 *
	 * <p>Menerima {@code null} tanpa keberatan meskipun kolomnya wajib. Tidak memeriksa apakah
	 * komponen yang sama sudah terdaftar pada format tujuan.</p>
	 *
	 * @param formatNilaiProposalSkripsi format nilai tujuan
	 * @see #getFormatNilaiProposalSkripsi()
	 */
	public void setFormatNilaiProposalSkripsi(FormatNilaiProposalSkripsi formatNilaiProposalSkripsi) {
		this.formatNilaiProposalSkripsi = formatNilaiProposalSkripsi;
	}

}
