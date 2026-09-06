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
 * Nama sebuah kelompok pada tugas kelompok — satu regu yang dibentuk untuk mengerjakan sebuah
 * {@link TugasKelompok}, dengan kode, kuota anggota, dan keterangannya.
 *
 * <p>Anggotanya dicatat pada {@link NamaTugasKelompokPunyaMahasiswa}. Kelas ini hanya mendefinisikan
 * kelompoknya, tidak memuat daftar anggota maupun penegakan kuota.</p>
 *
 * <h3>Kuota tidak ditegakkan di sini</h3>
 * <p>{@link #getKuota()} menyatakan berapa banyak anggota yang boleh bergabung, tetapi kelas ini —
 * maupun {@link NamaTugasKelompokPunyaMahasiswa} — tidak memeriksanya. Tidak ada batasan basis data
 * yang membatasi jumlah baris keanggotaan per kelompok. Penegakan kuota sepenuhnya menjadi tanggung
 * jawab lapisan action, dan bila jalur pendaftaran baru ditambahkan, ia harus memeriksanya sendiri.</p>
 *
 * <h3>Kode dibangkitkan saat dibaca</h3>
 * <p>{@link #getKode()} membangkitkan kode baru dan menuliskannya ke field bila kolomnya masih
 * kosong — sebuah penulisan yang dipicu oleh pembacaan, dengan nilai yang berbeda pada setiap
 * pemanggilan. Lihat uraian di sana.</p>
 *
 * <h3>Tautan ke tugas bersifat opsional</h3>
 * <p>{@link #getTugasKelompok()} dipetakan {@code nullable = true}, sehingga kelompok dapat tersimpan
 * tanpa tugas induk — kelompok yang tidak mengerjakan apa pun, tidak muncul saat ditelusuri dari
 * tugasnya, tetapi tetap terhitung oleh kode yang mencacah tabel ini.</p>
 *
 * @see TugasKelompok
 * @see NamaTugasKelompokPunyaMahasiswa
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "nama_tugas_kelompok")

public class NamaTugasKelompok extends GeneralValueObject {

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
	 * Representasi teks berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai komponen daftar dan kotak pilihan ZK, sehingga nomor internal ikut terlihat pengguna
	 * akhir. Tidak menyebut tugas induknya, sehingga kelompok bernama sama pada dua tugas berbeda
	 * hanya dapat dibedakan lewat id.</p>
	 *
	 * <p>Membaca field {@code nama} secara langsung. Sisi baiknya, karena tidak menyentuh
	 * {@link #getKode()}, method ini tidak memicu pembangkitan kode.</p>
	 *
	 * @return teks gabungan id dan nama kelompok
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode kelompok; dibangkitkan otomatis saat dibaca bila kosong. Lihat {@link #getKode()}. */
	private String kode;

	/** Nama kelompok. Lihat {@link #getNama()}. */
	private String nama;
	/** Tugas kelompok yang dikerjakan; boleh kosong. Lihat {@link #getTugasKelompok()}. */
	private TugasKelompok tugasKelompok;
	/** Keterangan bebas mengenai kelompok ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Kuota jumlah anggota kelompok. Lihat {@link #getKuota()}. */
	private Integer kuota;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public NamaTugasKelompok() {
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
	 * Keterangan bebas mengenai kelompok ini.
	 *
	 * <p>Dikembalikan apa adanya, tanpa pemangkasan spasi. Tidak ada panjang maksimum yang dinyatakan.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan kelompok.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}, tidak divalidasi
	 * @see #getKeterangan()
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel tugas kelompok yang dikerjakan kelompok ini.
	 *
	 * <p>Memindahkan kelompok ke tugas lain <b>tidak menyentuh daftar anggotanya</b> pada
	 * {@link NamaTugasKelompokPunyaMahasiswa}, sehingga anggota ikut terbawa ke tugas yang baru tanpa
	 * pemberitahuan.</p>
	 *
	 * @param tugasKelompok tugas kelompok; boleh {@code null}
	 * @see #getTugasKelompok()
	 */
	public void setTugasKelompok(TugasKelompok tugasKelompok) {
		this.tugasKelompok = tugasKelompok;
	}

	/**
	 * Tugas kelompok yang dikerjakan kelompok ini.
	 *
	 * <p><b>Opsional</b> ({@code nullable = true}) meskipun tanpa tugas induk kelompok ini kehilangan
	 * maknanya — lihat catatan pada Javadoc kelas. Ini pula satu-satunya pembatas cakupan pada entity
	 * ini, sehingga penyaringan berdasarkan tugas harus dilakukan setiap kali daftar kelompok
	 * ditampilkan.</p>
	 *
	 * <p>Dimuat {@code EAGER} lewat {@code SELECT} terpisah dan tanpa perlindungan {@code check(...)} —
	 * pola N+1 pada daftar kelompok. Riam {@code PERSIST} dan {@code MERGE} berlaku ke arah tugas
	 * kelompok.</p>
	 *
	 * @return tugas kelompok, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "tugas_kelompok", nullable = true)
	public TugasKelompok getTugasKelompok() {
		return tugasKelompok;
	}

	/**
	 * Nama kelompok — misalnya "Kelompok 1" atau nama yang dipilih anggotanya.
	 *
	 * <p>Dikembalikan <b>apa adanya, tanpa pemangkasan spasi</b> dan tanpa nilai bawaan; berbeda dari
	 * getter nama pada kebanyakan entity lain di paket ini yang memangkasnya. Dua kelompok yang
	 * namanya hanya berbeda pada spasi di ujung akan tampil serupa di layar tetapi tidak cocok saat
	 * dibandingkan.</p>
	 *
	 * <p>Tidak ada batasan keunikan, termasuk di dalam satu tugas yang sama: dua kelompok bernama
	 * persis sama dapat hidup berdampingan dan hanya dapat dibedakan lewat {@link #getKode()} atau
	 * id-nya.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code nama} secara bawaan.</p>
	 *
	 * @return nama kelompok apa adanya, atau {@code null} bila belum diisi
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Menyetel nama kelompok.
	 *
	 * @param nama nama kelompok; disimpan apa adanya, tidak divalidasi
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Kode pengenal kelompok, dibangkitkan otomatis pada pembacaan pertama.
	 *
	 * <p><b>Getter ini menulis ke field saat dibaca — dan nilai yang ditulisnya baru setiap kali.</b>
	 * Bila {@code kode} masih {@code null}, ia memanggil pembangkit kode batang lalu menyimpan hasilnya
	 * ke field. Ini bukan sekadar penjaga nilai bawaan seperti {@code aktif = true}: nilai yang
	 * dihasilkan <b>berbeda pada setiap pemanggilan</b>, sehingga sekadar membaca properti ini
	 * menciptakan data baru yang sebelumnya tidak ada.</p>
	 *
	 * <p>Akibat yang perlu diketahui:</p>
	 * <ul>
	 *   <li>Karena kelas ini dipetakan lewat akses properti, <b>Hibernate memanggil getter ini saat
	 *       menyimpan</b>, sehingga kode terbangkitkan dan tersimpan pada alur yang tampak biasa. Pada
	 *       entity yang terikat session, membacanya di luar alur simpan pun menjadikan objek kotor
	 *       sehingga {@code UPDATE} diterbitkan dan Envers mencatat revisi yang tidak diminta.</li>
	 *   <li><b>Tidak ada batasan keunikan pada kolom kode.</b> Pembangkit tidak memeriksa tabrakan
	 *       dengan kode yang sudah ada, dan basis data tidak menolaknya — jadi keunikan kode
	 *       sepenuhnya bergantung pada kualitas pembangkitnya. Bandingkan dengan
	 *       {@code DosenPembimbingAkademikTemporary.getUnique_id()} yang dijaga indeks unik nyata;
	 *       di sini penjagaan seperti itu tidak ada.</li>
	 *   <li>Dua thread yang membaca properti ini bersamaan pada objek yang sama dapat menghasilkan dua
	 *       kode berbeda, dan yang tersimpan adalah yang menang terakhir.</li>
	 * </ul>
	 *
	 * <p>Bila kode memang harus selalu ada, membangkitkannya sekali di jalur pembuatan baris — bukan
	 * di dalam getter — akan jauh lebih dapat diramalkan.</p>
	 *
	 * @return kode kelompok; kode yang baru dibangkitkan bila sebelumnya kosong
	 */
	public String getKode() {
		if (kode == null) {
			kode = Common.getGeneratedBarCode();
		}
		return kode;
	}

	/**
	 * Menyetel kode pengenal kelompok.
	 *
	 * <p>Mengisinya secara eksplisit sebelum baris pernah dibaca adalah cara menghindari pembangkitan
	 * otomatis pada {@link #getKode()}. Tidak memeriksa tabrakan dengan kode kelompok lain.</p>
	 *
	 * @param kode kode kelompok; tidak divalidasi dan tidak diperiksa keunikannya
	 * @see #getKode()
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Kuota jumlah anggota kelompok; {@code null} dibaca sebagai {@code 10}.
	 *
	 * <p><b>Pembacaan murni</b> — memakai ternary tanpa menyentuh field, berbeda dari
	 * {@link #getKode()} pada kelas yang sama.</p>
	 *
	 * <p><b>Kuota ini tidak ditegakkan di mana pun pada lapisan model.</b> Tidak ada pemeriksaan di
	 * kelas ini, tidak ada di {@link NamaTugasKelompokPunyaMahasiswa}, dan tidak ada batasan basis
	 * data yang membatasi jumlah baris keanggotaan per kelompok. Angka ini murni nasihat bagi lapisan
	 * action; jalur pendaftaran anggota mana pun yang tidak memeriksanya akan mengisi kelompok melebihi
	 * kuota tanpa keluhan.</p>
	 *
	 * <p>Angka bawaan {@code 10} tertanam di kode, bukan diambil dari konfigurasi. Tidak ada
	 * pembatasan rentang: kuota nol atau negatif akan diterima, dan karena tidak ditegakkan, keduanya
	 * pun tidak menghalangi siapa pun bergabung.</p>
	 *
	 * <p>Getter ini tidak dianotasi {@code @Column}; Hibernate memetakannya ke kolom bernama
	 * {@code kuota} secara bawaan.</p>
	 *
	 * @return kuota anggota; {@code 10} bila belum diisi
	 */
	public Integer getKuota() {
		return kuota == null ? 10 : kuota;
	}

	/**
	 * Menyetel kuota jumlah anggota kelompok.
	 *
	 * <p>Menerima {@code null}, yang dibaca sebagai {@code 10}. Menurunkan kuota <b>tidak
	 * mengeluarkan anggota yang sudah melebihi</b> — tidak ada penegakan di lapisan model sama sekali.</p>
	 *
	 * @param kuota kuota anggota; boleh {@code null}, tidak divalidasi
	 * @see #getKuota()
	 */
	public void setKuota(Integer kuota) {
		this.kuota = kuota;
	}

}
