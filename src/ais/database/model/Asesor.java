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
 * Penugasan seorang pengguna sebagai asesor pada penilaian Beban Kerja Dosen (BKD) — menghubungkan
 * akun {@link Tbmuser} dengan sebuah {@link AsesorPenunjangKinerjaDosen}.
 *
 * <p>Entity penghubung sederhana dengan penanda aktif dan keterangan bebas. Tidak memuat aturan
 * perilaku; siapa yang boleh menjadi asesor dan apa yang boleh dinilainya ditentukan sepenuhnya di
 * lapisan action BKD.</p>
 *
 * <h3>Kedua sisi penghubung boleh kosong</h3>
 * <p>{@link #getTbmuser()} maupun {@link #getAsesorPenunjangKinerjaDosen()} sama-sama dipetakan
 * {@code nullable = true}. Sebuah baris asesor karenanya dapat tersimpan tanpa pengguna, tanpa objek
 * penilaian, atau tanpa keduanya — menjadi baris yatim yang tetap dihitung sebagai asesor oleh kode
 * yang sekadar mencacah tabel ini. Untuk entity yang menentukan <b>siapa berhak menilai</b>,
 * kelonggaran itu perlu ditutup di lapisan action yang menyimpan: pastikan kedua sisi terisi sebelum
 * baris dibuat.</p>
 *
 * <p><b>Tidak ada batasan keunikan</b> pada pasangan pengguna dan objek penilaian, sehingga orang
 * yang sama dapat didaftarkan berkali-kali sebagai asesor untuk objek yang sama. Bila jumlah asesor
 * dipakai sebagai dasar perhitungan — misalnya rata-rata nilai antar asesor — pendaftaran ganda akan
 * memiringkan hasilnya.</p>
 *
 * @see Tbmuser
 * @see AsesorPenunjangKinerjaDosen
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "asesor")

public class Asesor extends GeneralValueObject {

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
	 * Representasi teks berbentuk {@code "<id>-<tbmuser>-<asesorPenunjangKinerjaDosen>"}.
	 *
	 * <p>Dipakai komponen daftar dan kotak pilihan ZK, sehingga isinya terlihat pengguna akhir —
	 * termasuk id internal baris ini dan identitas akun pengguna yang menjadi asesor.</p>
	 *
	 * <p><b>Dapat memicu pemuatan lazy dan gagal di luar session.</b> Kedua bagian terakhir memanggil
	 * {@code toString()} pada objek relasi. Field dibaca <b>secara langsung</b>, bukan lewat
	 * {@link #getTbmuser()}, sehingga penyelesaian proksi lewat {@code check(...)} yang biasanya
	 * dilakukan getter itu <b>dilewati</b>. Pada objek yang sudah lepas dari session, hasilnya bisa
	 * berupa {@code LazyInitializationException} — justru pada method yang paling sering dipanggil
	 * dari dalam pesan log dan penyaji daftar.</p>
	 *
	 * <p>Tanpa penjagaan {@code null}: relasi yang kosong memunculkan kata {@code "null"} secara
	 * harfiah di layar.</p>
	 *
	 * @return teks gabungan id, pengguna, dan objek penilaian
	 */
	public String toString() {
		return id + "-" + tbmuser + "-" + asesorPenunjangKinerjaDosen;
	}

	/** Akun pengguna yang bertindak sebagai asesor. Lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;
	/** Objek penilaian penunjang kinerja dosen yang ditangani asesor ini. Lihat {@link #getAsesorPenunjangKinerjaDosen()}. */
	private AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen;
	/** Keterangan bebas mengenai penugasan asesor ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda apakah penugasan asesor ini masih berlaku. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public Asesor() {
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
	 * Keterangan bebas mengenai penugasan asesor ini.
	 *
	 * <p>Dikembalikan apa adanya, tanpa pemangkasan spasi. Tidak ada panjang maksimum yang dinyatakan,
	 * sehingga batasnya ditentukan tipe kolom di basis data.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan penugasan asesor.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}, tidak divalidasi
	 * @see #getKeterangan()
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Akun pengguna yang bertindak sebagai asesor.
	 *
	 * <p>Dimuat secara {@code LAZY}, jadi yang tersimpan di field bisa berupa proksi Hibernate.
	 * Karena itu getter ini menyalurkannya lebih dulu lewat {@code check(...)}, yang berusaha
	 * menyelesaikan proksi tersebut dari cache identitas, dari session yang tersedia, atau dengan
	 * membuka session baru — dan bila semuanya gagal, mengembalikan objek apa adanya tanpa melempar.
	 * Inilah yang membuat properti ini tetap dapat dibaca dari objek yang sudah lepas dari
	 * session.</p>
	 *
	 * <p><b>Getter ini menulis ke field.</b> Hasil {@code check(...)} ditugaskan kembali ke
	 * {@code tbmuser}, dan objek yang dikembalikannya <i>bisa merupakan instance Java yang berbeda</i>
	 * untuk baris yang sama. Pada entity yang terikat session, pertukaran instance itu dapat terbaca
	 * sebagai perubahan properti oleh pemeriksaan kotor Hibernate, sehingga {@code UPDATE} diterbitkan
	 * dan — karena kelas ini {@code @Audited} — Envers mencatat revisi untuk perubahan yang tidak
	 * pernah diminta. Pola ini memang harga yang dibayar untuk penyelesaian proksi yang aman, tetapi
	 * perlu diketahui saat menelusuri revisi audit yang tampak muncul tanpa sebab.</p>
	 *
	 * <p>Boleh {@code null}: lihat catatan baris yatim pada Javadoc kelas.</p>
	 *
	 * @return akun pengguna asesor, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menyetel akun pengguna yang bertindak sebagai asesor.
	 *
	 * <p>Tidak memeriksa bahwa pengguna itu memang berhak menjadi asesor, dan tidak memeriksa apakah
	 * pasangan pengguna dan objek penilaian ini sudah pernah didaftarkan. Keduanya harus ditegakkan
	 * di lapisan action BKD.</p>
	 *
	 * @param tbmuser akun pengguna; boleh {@code null}
	 * @see #getTbmuser()
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Objek penilaian penunjang kinerja dosen yang ditangani asesor ini.
	 *
	 * <p>Berbeda dengan {@link #getTbmuser()}, relasi ini dimuat dengan sifat bawaan {@code ManyToOne}
	 * — yaitu {@code EAGER} — lewat {@code SELECT} terpisah, dan getter-nya <b>tidak</b> menyalurkan
	 * hasil lewat {@code check(...)}. Dua relasi pada kelas yang sama karenanya berperilaku berbeda:
	 * yang satu tahan terhadap objek lepas-session, yang lain tidak. Ketidakseragaman ini perlu
	 * diingat saat memindahkan objek {@code Asesor} keluar dari session.</p>
	 *
	 * <p>Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah objek penilaian: menyimpan
	 * {@code Asesor} yang membawa objek baru akan ikut menyimpan objek itu. Boleh {@code null} —
	 * lihat catatan baris yatim pada Javadoc kelas.</p>
	 *
	 * @return objek penilaian penunjang kinerja dosen, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "asesor_penunjang_kinerja_dosen", nullable = true)
	public AsesorPenunjangKinerjaDosen getAsesorPenunjangKinerjaDosen() {
		return asesorPenunjangKinerjaDosen;
	}

	/**
	 * Menyetel objek penilaian penunjang kinerja dosen yang ditangani asesor ini.
	 *
	 * @param asesorPenunjangKinerjaDosen objek penilaian; boleh {@code null}
	 * @see #getAsesorPenunjangKinerjaDosen()
	 */
	public void setAsesorPenunjangKinerjaDosen(AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen) {
		this.asesorPenunjangKinerjaDosen = asesorPenunjangKinerjaDosen;
	}

	/**
	 * Apakah penugasan asesor ini masih berlaku; {@code null} dianggap berlaku.
	 *
	 * <p><b>Getter ini menulis ke field saat dibaca.</b> Bila {@code aktif} masih {@code null}, ia
	 * menetapkannya menjadi {@code true} lalu mengembalikannya. Pada entity yang terikat session
	 * Hibernate, hal itu menjadikan objek kotor sehingga {@code UPDATE} diterbitkan pada flush
	 * berikutnya dan Envers mencatat revisi baru untuk perubahan yang tidak pernah diminta siapa pun.
	 * Sekadar menampilkan daftar asesor di layar sudah cukup untuk menulis ulang setiap baris yang
	 * kolom aktifnya masih kosong.</p>
	 * <p>Bandingkan dengan {@code JenisEvaluasi.getAktif()} di paket yang sama, yang mencapai hasil
	 * baca yang sama persis memakai ternary tanpa menyentuh field. Itulah bentuk yang benar; getter
	 * ini sebaiknya diseragamkan ke sana.</p>
	 *
	 * <p><b>Penanda ini dua arah dengan condong ke "aktif".</b> Hanya {@code false} yang benar-benar
	 * mencabut penugasan; {@code null} maupun {@code true} sama-sama berarti berlaku. Mengosongkan
	 * kolom bukan cara mencabut hak asesor — setelah dikosongkan penugasan kembali berlaku. Untuk
	 * entity yang menentukan siapa berhak menilai, arah bawaan ini adalah <b>gagal-membuka</b>: baris
	 * yang datanya tidak lengkap tetap dianggap berwenang.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}, sehingga Hibernate memetakannya ke kolom bernama
	 * {@code aktif} secara bawaan.</p>
	 *
	 * @return {@code true} bila penugasan masih berlaku; hanya {@code false} yang mencabutnya
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel penanda berlaku-tidaknya penugasan asesor ini.
	 *
	 * <p>Kirim {@code false} untuk mencabut penugasan. Mengirim {@code null} <b>tidak</b> mencabutnya
	 * — {@link #getAktif()} akan membacanya sebagai berlaku, lalu menuliskan {@code true} kembali ke
	 * field pada pembacaan pertama.</p>
	 *
	 * @param aktif {@code false} untuk mencabut; {@code true} atau {@code null} berarti berlaku
	 * @see #getAktif()
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
