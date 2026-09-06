package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Jenis evaluasi pembelajaran — kategori penilaian seperti kognitif, afektif, atau psikomotorik —
 * yang dipakai untuk menggolongkan komponen nilai.
 *
 * <p>Entity master kecil dengan nama, keterangan, penanda aktif, dan sebuah kode pemetaan ke
 * Feeder. Tidak memuat aturan perilaku apa pun.</p>
 *
 * <h3>Dua getter berpenjaga-default, hanya satu yang murni</h3>
 * <p>Kelas ini memuat contoh berdampingan dari sebuah pola yang berulang di seluruh basis kode ini,
 * sekaligus contoh cara menuliskannya dengan benar:</p>
 * <ul>
 *   <li>{@link #getAktif()} memakai ternary dan <b>tidak mengubah apa pun</b> — pembacaan murni.</li>
 *   <li>{@link #getFeeder()} <b>menulis ke field saat dibaca</b> bila nilainya masih {@code null}.
 *       Sekadar membaca properti ini pada entity yang terikat session menjadikannya kotor, sehingga
 *       Hibernate menerbitkan {@code UPDATE} dan Envers mencatat revisi baru untuk perubahan yang
 *       tidak pernah diminta siapa pun.</li>
 * </ul>
 * <p>Keduanya menghasilkan nilai jatuh-tempo yang sama gunanya; yang membedakan hanya efek
 * sampingnya. Bila salah satu perlu diseragamkan, {@link #getAktif()}-lah bentuk yang benar.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jenis_evaluasi")
public class JenisEvaluasi extends GeneralValueObject {

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
	 * Representasi teks berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai komponen daftar dan kotak pilihan ZK, sehingga id ikut terlihat pengguna akhir.
	 * Membaca field {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga tidak ikut
	 * memangkas spasi. Pada baris yang belum tersimpan, hasilnya diawali kata {@code "null-"}.</p>
	 *
	 * @return teks gabungan id dan nama jenis evaluasi
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama jenis evaluasi. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas mengenai jenis evaluasi ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda apakah jenis evaluasi ini masih dipakai. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Kode padanan jenis evaluasi ini pada Feeder. Lihat {@link #getFeeder()}. */
	private Long feeder;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public JenisEvaluasi() {
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
	 * Nama jenis evaluasi, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Pemangkasan dilakukan saat membaca, bukan saat menyimpan — nilai di basis data tetap membawa
	 * spasi apa adanya, sehingga kueri yang mencocokkan kolom {@code nama} secara langsung dapat
	 * gagal menemukan baris yang lewat getter ini terlihat cocok.</p>
	 *
	 * <p><b>Perhatikan blok yang dikomentari di dalam badan method ini.</b> Sebelumnya nama akan
	 * <i>ditimpa</i> menjadi {@code "Kognitif/ Pengetahuan"} setiap kali {@link #getFeeder()}
	 * bernilai {@code 4}. Itu adalah getter yang menulis: membaca nama sudah cukup untuk mengubah
	 * data yang tersimpan. Kode itu kini nonaktif dan getter ini menjadi pembacaan murni. Jangan
	 * menghidupkannya kembali — bila pemetaan nama untuk kode Feeder tertentu memang dibutuhkan,
	 * lakukan di lapisan penyaji, bukan di dalam getter yang dipanggil Hibernate.</p>
	 *
	 * <p>Kolomnya dinyatakan {@code nullable = false} dengan panjang 255; keduanya tidak ditegakkan
	 * di Java. Tidak ada batasan keunikan pada nama.</p>
	 *
	 * @return nama jenis evaluasi tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
//		if (getFeeder() != null && getFeeder().equals(4L)) {
//			nama = "Kognitif/ Pengetahuan";
//		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis evaluasi.
	 *
	 * <p>Menyimpan nilai apa adanya, tanpa pemangkasan spasi dan tanpa pemeriksaan panjang.</p>
	 *
	 * @param nama nama jenis evaluasi; tidak divalidasi
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas mengenai jenis evaluasi ini.
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
	 * Menyetel keterangan jenis evaluasi.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}, tidak divalidasi
	 * @see #getKeterangan()
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kode padanan jenis evaluasi ini pada Feeder (pangkalan data pendidikan tinggi), dengan nilai
	 * jatuh-tempo {@code 1}.
	 *
	 * <p><b>Getter ini menulis ke field saat dibaca.</b> Bila {@code feeder} masih {@code null}, ia
	 * menetapkannya menjadi {@code 1L} lalu mengembalikannya. Pada entity yang terikat session
	 * Hibernate, hal itu menjadikan objek kotor: {@code UPDATE} diterbitkan pada flush berikutnya dan
	 * — karena kelas ini dianotasi {@code @Audited} — Envers mencatat sebuah revisi baru untuk
	 * perubahan yang tidak pernah diminta siapa pun. Cukup dengan menampilkan daftar jenis evaluasi
	 * di layar, seluruh baris yang kolom feeder-nya masih kosong akan tertulis ulang dan memenuhi
	 * riwayat audit.</p>
	 *
	 * <p>Akibat lain yang lebih halus: karena penulisan terjadi diam-diam, tidak ada cara membedakan
	 * "kode Feeder-nya memang 1" dari "kode Feeder-nya belum pernah diisi". Keduanya tampak sama
	 * setelah pembacaan pertama.</p>
	 *
	 * <p>Bandingkan dengan {@link #getAktif()} pada kelas yang sama, yang mencapai tujuan serupa
	 * memakai ternary tanpa menyentuh field. Itulah bentuk yang benar; getter ini sebaiknya
	 * diseragamkan ke sana — perubahannya setara secara perilaku baca dan menghilangkan penulisan
	 * yang tidak diniatkan.</p>
	 *
	 * @return kode Feeder; {@code 1} bila belum pernah diisi
	 */
	public Long getFeeder() {
		if (feeder == null) {
			feeder = 1L;
		}
		return feeder;
	}

	/**
	 * Menyetel kode padanan Feeder untuk jenis evaluasi ini.
	 *
	 * <p>Menerima {@code null}, tetapi {@link #getFeeder()} akan segera menggantinya dengan {@code 1}
	 * pada pembacaan berikutnya — jadi mengosongkan nilai ini tidak bertahan lama.</p>
	 *
	 * @param feeder kode Feeder; tidak divalidasi terhadap daftar kode yang sah
	 * @see #getFeeder()
	 */
	public void setFeeder(Long feeder) {
		this.feeder = feeder;
	}

	/**
	 * Apakah jenis evaluasi ini masih dipakai; {@code null} dianggap aktif.
	 *
	 * <p><b>Pembacaan murni</b> — memakai ternary dan tidak menyentuh field, berbeda dengan
	 * {@link #getFeeder()} pada kelas yang sama. Inilah bentuk penjaga nilai jatuh-tempo yang benar
	 * untuk entity yang dianotasi {@code @Audited}.</p>
	 *
	 * <p><b>Penanda ini dua arah dengan condong ke "aktif".</b> Hanya nilai {@code false} yang
	 * benar-benar menonaktifkan; {@code null} maupun {@code true} sama-sama berarti aktif.
	 * Konsekuensinya, mengosongkan kolom ini bukan cara menonaktifkan sebuah jenis evaluasi —
	 * setelah dikosongkan ia kembali aktif. Baris lama yang dibuat sebelum kolom ini ada pun otomatis
	 * dianggap aktif.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}, sehingga Hibernate memetakannya ke kolom bernama
	 * {@code aktif} secara bawaan.</p>
	 *
	 * @return {@code true} bila masih dipakai; hanya {@code false} yang berarti nonaktif
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif jenis evaluasi ini.
	 *
	 * <p>Kirim {@code false} untuk menonaktifkan. Mengirim {@code null} <b>tidak</b> menonaktifkan —
	 * {@link #getAktif()} akan membacanya sebagai aktif.</p>
	 *
	 * @param aktif {@code false} untuk menonaktifkan; {@code true} atau {@code null} berarti aktif
	 * @see #getAktif()
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
