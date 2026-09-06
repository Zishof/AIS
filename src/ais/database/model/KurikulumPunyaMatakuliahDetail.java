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

import ais.common.Common;

/**
 * Satu baris rincian rencana pembelajaran semester (RPS) untuk sebuah mata kuliah dalam sebuah
 * kurikulum — yaitu satu pertemuan, dengan topik, indikator, pengalaman belajar, tugas dan
 * penilaian, metode, waktu, serta buku rujukannya.
 *
 * <p>Induknya adalah {@link KurikulumPunyaMatakuliah}; nomor pertemuannya disimpan pada
 * {@link #getNomorUrut()}.</p>
 *
 * <h3>Getter berjalur cadangan yang MENULIS ke basis data</h3>
 * <p><b>Ini sifat paling penting dari kelas ini.</b> Empat getter — {@link #getIndikator()},
 * {@link #getPengalamanBelajar()}, {@link #getTugasDanPenilaian()}, dan
 * {@link #getWaktupembelajaran()} — mengambil nilai cadangannya dari
 * {@code Common.getKonfigurasi(kunci, nilaiBawaan)}. Method konfigurasi itu <b>menulis nilai bawaan
 * ke tabel konfigurasi bila kuncinya belum ada</b>. Artinya membaca sebuah baris RPS yang bidangnya
 * masih kosong dapat <b>menyisipkan baris baru ke tabel konfigurasi</b> — sebuah penulisan basis
 * data yang dipicu oleh apa yang terlihat seperti pembacaan biasa.</p>
 * <p>Karena kelas ini dipetakan lewat akses properti, Hibernate sendiri memanggil getter-getter itu
 * saat menyimpan, sehingga jalur ini terpicu pada alur yang sangat lazim. Dua akibat praktisnya:
 * pertama, teks bawaan yang tertulis di kode sumber hanya berlaku sekali seumur instalasi — sesudah
 * itu yang menang adalah isi tabel konfigurasi, sehingga <b>mengubah teks bawaan di kode tidak akan
 * mengubah perilaku instalasi yang sudah berjalan</b>. Kedua, jangan memanggil getter-getter ini dari
 * jalur yang tidak boleh menulis ke basis data.</p>
 *
 * <h3>Nilai cadangan tidak dapat dibedakan dari nilai yang diisi</h3>
 * <p>Kelima getter berjalur cadangan di kelas ini — termasuk {@link #getTopik()},
 * {@link #getBukuRujukan1()}, dan {@link #getMetodePembelajaran()} — mengembalikan teks pengganti
 * ketika kolomnya kosong. Pemanggil tidak dapat membedakan "dosen memang menuliskan teks itu" dari
 * "kolomnya belum pernah diisi". Untuk laporan yang perlu menandai RPS yang belum lengkap, keadaan
 * kosong harus diperiksa lewat jalur lain.</p>
 *
 * <h3>Pemotongan senyap pada kolom lama</h3>
 * <p>Sebagian setter menyalurkan nilainya lewat pembantu privat yang memotong teks lebih dari 255
 * karakter <b>tanpa peringatan apa pun</b>. Lihat {@link #setTopik(String)} dan saudara-saudaranya.</p>
 *
 * <h3>Tautan ke induk bersifat opsional</h3>
 * <p>{@link #getKurikulumPunyaMatakuliah()} dipetakan {@code nullable = true}, sehingga baris rincian
 * dapat tersimpan tanpa induk — pertemuan yang tidak menjadi bagian dari RPS mana pun, tidak muncul
 * saat ditelusuri dari induknya, tetapi tetap terhitung oleh kode yang mencacah tabel ini.</p>
 *
 * @see KurikulumPunyaMatakuliah
 * @see StatusPertemuan
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kurikulum_punya_matakuliah_detail")

public class KurikulumPunyaMatakuliahDetail extends GeneralValueObject {

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
	 * Representasi teks berbentuk {@code "<id>-<topik>"}.
	 *
	 * <p>Membaca field {@code topik} secara langsung, <b>bukan</b> lewat {@link #getTopik()}, sehingga
	 * jalur cadangan {@code "Pertemuan ke N ..."} tidak berlaku di sini: baris yang topiknya belum
	 * diisi tampil sebagai {@code "<id>-null"} di komponen daftar ZK, meskipun {@link #getTopik()}
	 * mengembalikan teks yang rapi. Sisi baiknya, method ini bebas dari efek samping penulisan yang
	 * mengintai getter-getter lain di kelas ini.</p>
	 *
	 * <p>Tidak menyebut nomor pertemuan maupun mata kuliah induknya.</p>
	 *
	 * @return teks gabungan id dan topik
	 */
	public String toString() {
		return id + "-" + topik;
	}

	/** RPS mata kuliah yang menaungi rincian ini; boleh kosong. Lihat {@link #getKurikulumPunyaMatakuliah()}. */
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;

	/** Topik bahasan pertemuan ini. Lihat {@link #getTopik()}. */
	private String topik;
	/** Indikator capaian pembelajaran pertemuan ini. Lihat {@link #getIndikator()}. */
	private String indikator;
	/** Alokasi waktu pembelajaran pertemuan ini. Lihat {@link #getWaktupembelajaran()}. */
	private String waktupembelajaran;
	/** Pengalaman belajar yang dirancang untuk pertemuan ini. Lihat {@link #getPengalamanBelajar()}. */
	private String pengalamanBelajar;
	/** Tugas dan kriteria penilaian pertemuan ini. Lihat {@link #getTugasDanPenilaian()}. */
	private String tugasDanPenilaian;
	/** Buku rujukan utama; kolom lama berbatas 255 karakter. Lihat {@link #getBukuRujukan1()}. */
	private String bukuRujukan1;
	/** Buku rujukan tambahan; kolom teks tanpa batas panjang. Lihat {@link #getBukuRujukan2()}. */
	private String bukuRujukan2;

	/** Status pertemuan ini; boleh kosong. Lihat {@link #getStatusPertemuan()}. */
	private StatusPertemuan statusPertemuan;
	/**
	 * Nomor pertemuan, dengan nilai awal {@code 1} yang diberikan di deklarasi field.
	 *
	 * <p>Dipetakan ke kolom {@code pertemuan_ke} — nama properti dan nama kolom berbeda. Lihat
	 * {@link #getNomorUrut()}.</p>
	 */
	private Integer nomorUrut = 1;

	/** Metode pembelajaran pertemuan ini. Lihat {@link #getMetodePembelajaran()}. */
	private String metodePembelajaran;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Tidak mengisi satu pun field. Objek yang dibentuk lewat konstruktor ini belum tentu sah untuk
	 * disimpan: kolom yang dinyatakan {@code nullable = false} pada getter-getter di bawah harus diisi
	 * lebih dulu, dan penolakannya datang dari basis data, bukan dari kelas ini.</p>
	 */
	public KurikulumPunyaMatakuliahDetail() {
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
	 * RPS mata kuliah yang menaungi rincian pertemuan ini.
	 *
	 * <p><b>Opsional</b> ({@code nullable = true}) meskipun tanpa induk baris ini kehilangan
	 * maknanya — lihat catatan baris yatim pada Javadoc kelas.</p>
	 *
	 * <p>Dimuat secara {@code LAZY} dan disalurkan lewat {@code check(...)} yang berusaha menyelesaikan
	 * proksi, lalu <b>menugaskan hasilnya kembali ke field</b> — pada entity terikat session hal itu
	 * dapat terbaca sebagai perubahan properti dan menerbitkan {@code UPDATE} beserta revisi Envers
	 * yang tidak diminta.</p>
	 *
	 * <p>Perhatikan bahwa {@link #getWaktupembelajaran()} membaca field yang sama secara
	 * <b>langsung</b>, tanpa melewati getter ini — sehingga tidak memperoleh perlindungan
	 * {@code check(...)}; lihat catatan di sana.</p>
	 *
	 * @return RPS mata kuliah penaung, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kurikulum_punya_matakuliah", nullable = true)
	public KurikulumPunyaMatakuliah getKurikulumPunyaMatakuliah() {
		kurikulumPunyaMatakuliah = check(kurikulumPunyaMatakuliah);
		return kurikulumPunyaMatakuliah;
	}

	/**
	 * Menyetel RPS mata kuliah yang menaungi rincian ini.
	 *
	 * <p>Mengirim {@code null} juga melumpuhkan jalur cadangan {@link #getWaktupembelajaran()} yang
	 * menurunkan alokasi waktu dari SKS mata kuliah induk.</p>
	 *
	 * @param kurikulumPunyaMatakuliah RPS penaung; boleh {@code null}
	 * @see #getKurikulumPunyaMatakuliah()
	 */
	public void setKurikulumPunyaMatakuliah(KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah) {
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliah;
	}

	/**
	 * Topik bahasan pertemuan ini, dengan jalur cadangan berupa teks rintisan.
	 *
	 * <p>Bila kolomnya kosong, yang dikembalikan adalah {@code "Pertemuan ke <nomorUrut> ..."} —
	 * teks rintisan yang siap disunting dosen. Bila terisi, nilainya dikembalikan setelah dipangkas
	 * spasi.</p>
	 *
	 * <p>Berbeda dari empat getter berjalur cadangan lain di kelas ini, cadangan di sini
	 * <b>tidak</b> memanggil {@code Common.getKonfigurasi(...)} dan karenanya <b>tidak menulis apa pun
	 * ke basis data</b>. Ini bentuk jalur cadangan yang aman.</p>
	 *
	 * <p>Tetap berlaku catatan umum kelas ini: "topiknya memang berbunyi seperti itu" dan "topiknya
	 * belum diisi" tidak dapat dibedakan lewat getter ini. Perhatikan pula bahwa {@link #toString()}
	 * <b>tidak</b> memakai jalur cadangan ini.</p>
	 *
	 * @return topik bahasan yang sudah dipangkas, atau teks rintisan bila belum diisi
	 */
	@Column(name = "topik")
	public String getTopik() {
		return this.topik == null ? "Pertemuan ke " + getNomorUrut() + " ..." : topik.trim();
	}

	/**
	 * Menyetel topik bahasan pertemuan ini.
	 *
	 * <p><b>Memotong nilai yang lebih dari 255 karakter secara diam-diam</b> lewat pembantu
	 * {@code batasiKolomLama(...)}. Tidak ada pengecualian, tidak ada peringatan, dan tidak ada cara
	 * bagi pemanggil mengetahui bahwa teksnya terpangkas selain dengan membandingkan sendiri panjang
	 * sebelum dan sesudah.</p>
	 *
	 * @param topik topik bahasan; dipotong pada 255 karakter bila lebih panjang
	 * @see #getTopik()
	 */
	public void setTopik(String topik) {
		this.topik = batasiKolomLama(topik);
	}

	/**
	 * Buku rujukan utama pertemuan ini.
	 *
	 * <p>Mengembalikan string kosong — bukan {@code null} — bila kolomnya belum diisi, sehingga
	 * penyaji dapat menuliskannya langsung. Nilai yang terisi dipangkas spasinya.</p>
	 *
	 * <p>Berpasangan dengan {@link #getBukuRujukan2()} yang <b>berperilaku berbeda</b>: yang itu
	 * mengembalikan {@code null} apa adanya dan tidak memangkas spasi. Dua properti bersaudara dengan
	 * kontrak yang tidak seragam — periksa masing-masing sebelum memakainya.</p>
	 *
	 * <p>Kolom ini termasuk "kolom lama" yang dibatasi 255 karakter oleh
	 * {@link #setBukuRujukan1(String)}.</p>
	 *
	 * @return buku rujukan utama yang sudah dipangkas; string kosong bila belum diisi, tidak pernah
	 *         {@code null}
	 */
	@Column(name = "buku_rujukan1")
	public String getBukuRujukan1() {
		return this.bukuRujukan1 == null ? "" : bukuRujukan1.trim();
	}

	/**
	 * Menyetel buku rujukan utama.
	 *
	 * <p><b>Memotong nilai yang lebih dari 255 karakter secara diam-diam.</b> Daftar pustaka yang
	 * panjang akan terpangkas di tengah tanpa peringatan; pakai {@link #setBukuRujukan2(String)} yang
	 * dipetakan ke kolom teks tanpa batas untuk rujukan yang panjang.</p>
	 *
	 * @param bukuRujukan1 buku rujukan utama; dipotong pada 255 karakter bila lebih panjang
	 * @see #getBukuRujukan1()
	 */
	public void setBukuRujukan1(String bukuRujukan1) {
		this.bukuRujukan1 = batasiKolomLama(bukuRujukan1);
	}

	/**
	 * Nomor pertemuan dalam rangkaian RPS; {@code null} dibaca sebagai {@code 1}.
	 *
	 * <p><b>Pembacaan murni</b> — memakai ternary tanpa menyentuh field.</p>
	 *
	 * <p><b>Nama properti dan nama kolom berbeda:</b> properti {@code nomorUrut}, kolom
	 * {@code pertemuan_ke}. Kueri HQL memakai nama properti, SQL asli memakai nama kolom.</p>
	 *
	 * <p>Atribut {@code length = 10} pada anotasi kolom <b>tidak berpengaruh</b> untuk properti
	 * bertipe {@link Integer} — {@code length} hanya bermakna bagi kolom berbasis teks.</p>
	 *
	 * <p><b>Tidak ada batasan keunikan</b> pada pasangan RPS dan nomor pertemuan: dua rincian dapat
	 * mengaku sebagai pertemuan ke-3 pada mata kuliah yang sama. Karena nilai jatuh-tempo {@code 1}
	 * berlaku untuk setiap baris yang belum diberi nomor, baris-baris itu semua mengaku sebagai
	 * pertemuan pertama.</p>
	 *
	 * @return nomor pertemuan; {@code 1} bila belum diisi
	 */
	@Column(name = "pertemuan_ke", length = 10)
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor pertemuan.
	 *
	 * <p>Menerima {@code null}, yang dibaca sebagai {@code 1}. Tidak memeriksa tabrakan dengan nomor
	 * pertemuan lain pada RPS yang sama.</p>
	 *
	 * @param nomorUrut nomor pertemuan; boleh {@code null}
	 * @see #getNomorUrut()
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Menyetel status pertemuan ini.
	 *
	 * @param statusPertemuan status pertemuan; boleh {@code null}
	 * @see #getStatusPertemuan()
	 */
	public void setStatusPertemuan(StatusPertemuan statusPertemuan) {
		this.statusPertemuan = statusPertemuan;
	}

	/**
	 * Status pertemuan ini — misalnya pertemuan biasa, ujian tengah semester, atau ujian akhir.
	 *
	 * <p>Opsional ({@code nullable = true}); rincian tanpa status akan hilang dari pengelompokan per
	 * status, bukan masuk kelompok "biasa".</p>
	 *
	 * <p>Dimuat secara {@code LAZY} dan disalurkan lewat {@code check(...)} yang hasilnya ditugaskan
	 * kembali ke field — perilaku dan akibatnya sama dengan
	 * {@link #getKurikulumPunyaMatakuliah()}.</p>
	 *
	 * @return status pertemuan, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_pertemuan", nullable = true)
	public StatusPertemuan getStatusPertemuan() {
		statusPertemuan = check(statusPertemuan);
		return statusPertemuan;
	}

	/**
	 * Menyetel metode pembelajaran pertemuan ini.
	 *
	 * <p><b>Memotong nilai yang lebih dari 255 karakter secara diam-diam.</b></p>
	 *
	 * @param metodePembelajaran metode pembelajaran; dipotong pada 255 karakter bila lebih panjang
	 * @see #getMetodePembelajaran()
	 */
	public void setMetodePembelajaran(String metodePembelajaran) {
		this.metodePembelajaran = batasiKolomLama(metodePembelajaran);
	}

	/**
	 * Metode pembelajaran pertemuan ini — misalnya ceramah, diskusi, atau praktikum.
	 *
	 * <p>Mengembalikan string kosong bila belum diisi; nilai yang terisi dipangkas spasinya. Disimpan
	 * sebagai teks bebas, bukan daftar tertutup.</p>
	 *
	 * <p>Cadangannya berupa string kosong, <b>tanpa</b> memanggil {@code Common.getKonfigurasi(...)} —
	 * jadi properti ini bebas dari efek samping penulisan yang mengintai empat getter lain di kelas
	 * ini.</p>
	 *
	 * @return metode pembelajaran yang sudah dipangkas; string kosong bila belum diisi
	 */
	@Column(name = "metode_pembelajaran", length = 255)
	public String getMetodePembelajaran() {
		return metodePembelajaran == null ? "" : metodePembelajaran.trim();
	}

	/**
	 * Indikator capaian pembelajaran pertemuan ini, dengan jalur cadangan dari tabel konfigurasi.
	 *
	 * <p>Bila kolomnya kosong, nilai diambil dari konfigurasi berkunci
	 * {@code default_indikator_pembelajaran}. <b>Pemanggilan itu menulis ke basis data</b>: bila kunci
	 * tersebut belum ada di tabel konfigurasi, teks bawaan yang tertulis di kode sumber akan
	 * <i>disisipkan</i> ke sana. Sekali tersisip, isi tabel konfigurasilah yang berlaku selamanya —
	 * mengubah teks bawaan di kode sumber <b>tidak lagi mengubah perilaku instalasi yang sudah
	 * berjalan</b>.</p>
	 *
	 * <p>Karena kelas ini dipetakan lewat akses properti, Hibernate memanggil getter ini saat
	 * menyimpan, sehingga penulisan itu dapat terpicu pada alur yang tampak sepenuhnya biasa. Jangan
	 * memanggil getter ini dari jalur yang tidak boleh menulis ke basis data, dan jangan memanggilnya
	 * di dalam perulangan besar — setiap panggilan melewati pencarian konfigurasi.</p>
	 *
	 * <p>Nilai yang terisi dipangkas spasinya. Dipetakan sebagai {@code text} tanpa batas panjang, dan
	 * setter-nya <b>tidak</b> memotong nilai — berbeda dari {@link #setTopik(String)} dan
	 * saudara-saudaranya.</p>
	 *
	 * @return indikator capaian pembelajaran; teks bawaan dari konfigurasi bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getIndikator() {
		return indikator == null ? Common
				.getKonfigurasi("default_indikator_pembelajaran", "Mahasiswa mampu menjelaskan dan mendiskusikan ....")
				.getNilai() : indikator.trim();
	}

	/**
	 * Menyetel indikator capaian pembelajaran.
	 *
	 * <p>Menyimpan nilai apa adanya — <b>tanpa</b> pemotongan 255 karakter, karena kolomnya bertipe
	 * {@code text}. Mengirim {@code null} mengaktifkan jalur cadangan {@link #getIndikator()} beserta
	 * efek samping penulisannya.</p>
	 *
	 * @param indikator indikator capaian; boleh {@code null}, tidak divalidasi
	 * @see #getIndikator()
	 */
	public void setIndikator(String indikator) {
		this.indikator = indikator;
	}

	/**
	 * Alokasi waktu pembelajaran pertemuan ini, dengan jalur cadangan berlapis dua.
	 *
	 * <p>Bila kolomnya kosong, urutan cadangannya:</p>
	 * <ol>
	 *   <li>Bila RPS induknya ada, diturunkan dari SKS mata kuliahnya menjadi
	 *       {@code "<sks> x 50 menit"}.</li>
	 *   <li>Bila tidak, diambil dari konfigurasi berkunci {@code default_waktu_pembelajaran} —
	 *       <b>dengan efek samping penulisan ke tabel konfigurasi</b> seperti dijelaskan pada
	 *       {@link #getIndikator()}.</li>
	 * </ol>
	 *
	 * <p><b>Jalur pertama menyimpan dua bahaya.</b> Field {@code kurikulumPunyaMatakuliah} dibaca
	 * <b>langsung</b>, bukan lewat {@link #getKurikulumPunyaMatakuliah()}, sehingga penyelesaian
	 * proksi lewat {@code check(...)} dilewati — padahal relasi itu {@code LAZY}. Pada objek yang
	 * sudah lepas dari session, membacanya dapat melempar {@code LazyInitializationException}.
	 * Selanjutnya, rangkaian {@code getMatakuliah().getSks()} <b>tidak dijaga {@code null}</b>: RPS
	 * yang belum ditautkan ke mata kuliah membuat getter ini melempar
	 * {@code NullPointerException}. Karena Hibernate memanggil getter ini saat menyimpan, kegagalan
	 * itu muncul dari dalam Hibernate, bukan sebagai pesan validasi yang jelas.</p>
	 *
	 * <p>Perhatikan pula bahwa angka 50 menit tertanam di kode, bukan diambil dari konfigurasi —
	 * berbeda dari nilai bawaan lain di kelas ini.</p>
	 *
	 * @return alokasi waktu pembelajaran; diturunkan dari SKS atau dari konfigurasi bila belum diisi
	 */
	public String getWaktupembelajaran() {
		return waktupembelajaran == null
				? (kurikulumPunyaMatakuliah != null ? kurikulumPunyaMatakuliah.getMatakuliah().getSks() + " x 50 menit"
						: Common.getKonfigurasi("default_waktu_pembelajaran", "... x 50 menit").getNilai())
				: waktupembelajaran.trim();
	}

	/**
	 * Menyetel alokasi waktu pembelajaran.
	 *
	 * <p><b>Memotong nilai yang lebih dari 255 karakter secara diam-diam.</b> Mengisi properti ini
	 * juga menonaktifkan seluruh jalur cadangan {@link #getWaktupembelajaran()} — termasuk bahayanya —
	 * sehingga mengisinya secara eksplisit adalah cara paling aman memakai properti ini.</p>
	 *
	 * @param waktupembelajaran alokasi waktu; dipotong pada 255 karakter bila lebih panjang
	 * @see #getWaktupembelajaran()
	 */
	public void setWaktupembelajaran(String waktupembelajaran) {
		this.waktupembelajaran = batasiKolomLama(waktupembelajaran);
	}

	/**
	 * Memotong teks pada 255 karakter agar muat di kolom-kolom lama yang belum dinaikkan menjadi
	 * {@code text}.
	 *
	 * <p>Dipakai {@link #setTopik(String)}, {@link #setBukuRujukan1(String)},
	 * {@link #setMetodePembelajaran(String)}, dan {@link #setWaktupembelajaran(String)} — yaitu setter
	 * bagi kolom yang panjangnya masih terbatas. Setter bagi kolom bertipe {@code text}
	 * ({@code indikator}, {@code pengalamanBelajar}, {@code tugasDanPenilaian}, {@code bukuRujukan2})
	 * sengaja <b>tidak</b> memakainya.</p>
	 *
	 * <p><b>Pemotongan bersifat senyap</b>: tidak ada pengecualian, tidak ada nilai balik penanda, dan
	 * tidak ada pencatatan. Pemanggil yang perlu tahu apakah teksnya terpangkas harus membandingkan
	 * sendiri panjang sebelum dan sesudah. Pemotongan juga dilakukan di tengah karakter ke-256 tanpa
	 * memperhatikan batas kata, sehingga hasilnya dapat terputus di tengah kata.</p>
	 *
	 * <p>Nilai {@code null} diteruskan apa adanya, sehingga jalur cadangan getter tetap berfungsi.</p>
	 *
	 * @param nilai teks yang akan dibatasi; boleh {@code null}
	 * @return teks yang sama bila 255 karakter atau kurang; potongan 255 karakter pertama bila lebih
	 */
	private static String batasiKolomLama(String nilai) {
		return nilai != null && nilai.length() > 255 ? nilai.substring(0, 255) : nilai;
	}

	/**
	 * Pengalaman belajar yang dirancang untuk pertemuan ini, dengan jalur cadangan dari tabel
	 * konfigurasi berkunci {@code default_pengalaman_belajar}.
	 *
	 * <p><b>Jalur cadangan itu menulis ke basis data</b> bila kuncinya belum ada — lihat uraian
	 * lengkapnya pada {@link #getIndikator()}. Seluruh catatan di sana berlaku sama di sini.</p>
	 *
	 * <p>Nilai yang terisi dipangkas spasinya. Dipetakan sebagai {@code text} tanpa batas panjang, dan
	 * setter-nya tidak memotong nilai.</p>
	 *
	 * @return pengalaman belajar; teks bawaan dari konfigurasi bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getPengalamanBelajar() {
		return pengalamanBelajar == null ? Common
				.getKonfigurasi("default_pengalaman_belajar", "Menyimak, Mengamati, Mendiskusikan, dan Menjawab soal")
				.getNilai() : pengalamanBelajar.trim();
	}

	/**
	 * Menyetel pengalaman belajar pertemuan ini.
	 *
	 * <p>Menyimpan nilai apa adanya, tanpa pemotongan karena kolomnya bertipe {@code text}.</p>
	 *
	 * @param pengalamanBelajar pengalaman belajar; boleh {@code null}, tidak divalidasi
	 * @see #getPengalamanBelajar()
	 */
	public void setPengalamanBelajar(String pengalamanBelajar) {
		this.pengalamanBelajar = pengalamanBelajar;
	}

	/**
	 * Tugas dan kriteria penilaian pertemuan ini, dengan jalur cadangan dari tabel konfigurasi
	 * berkunci {@code default_tugas_dan_penilaian}.
	 *
	 * <p><b>Jalur cadangan itu menulis ke basis data</b> bila kuncinya belum ada — lihat uraian
	 * lengkapnya pada {@link #getIndikator()}.</p>
	 *
	 * <p>Nilai yang terisi dipangkas spasinya. Dipetakan sebagai {@code text} tanpa batas panjang.</p>
	 *
	 * @return tugas dan kriteria penilaian; teks bawaan dari konfigurasi bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getTugasDanPenilaian() {
		return tugasDanPenilaian == null
				? Common.getKonfigurasi("default_tugas_dan_penilaian",
						"Ketepatan menjelaskan...., Ketepatan menyebutkan..., dan lain sebagainya").getNilai()
				: tugasDanPenilaian.trim();
	}

	/**
	 * Menyetel tugas dan kriteria penilaian pertemuan ini.
	 *
	 * <p>Menyimpan nilai apa adanya, tanpa pemotongan karena kolomnya bertipe {@code text}.</p>
	 *
	 * @param tugasDanPenilaian tugas dan kriteria penilaian; boleh {@code null}, tidak divalidasi
	 * @see #getTugasDanPenilaian()
	 */
	public void setTugasDanPenilaian(String tugasDanPenilaian) {
		this.tugasDanPenilaian = tugasDanPenilaian;
	}

	/**
	 * Buku rujukan tambahan pertemuan ini.
	 *
	 * <p><b>Satu-satunya getter teks di kelas ini yang benar-benar polos:</b> tidak ada jalur
	 * cadangan, tidak ada pemangkasan spasi, dan mengembalikan {@code null} apa adanya. Bandingkan
	 * {@link #getBukuRujukan1()} yang mengembalikan string kosong dan memangkas spasi — dua properti
	 * bersaudara dengan kontrak yang berbeda, sehingga pemanggil tidak boleh memperlakukan keduanya
	 * secara seragam.</p>
	 *
	 * <p>Dipetakan sebagai {@code text} tanpa batas panjang, dan setter-nya tidak memotong nilai —
	 * inilah properti yang tepat untuk daftar pustaka yang panjang.</p>
	 *
	 * @return buku rujukan tambahan apa adanya, atau {@code null} bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getBukuRujukan2() {
		return bukuRujukan2;
	}

	/**
	 * Menyetel buku rujukan tambahan.
	 *
	 * <p>Menyimpan nilai apa adanya, tanpa pemotongan karena kolomnya bertipe {@code text}.</p>
	 *
	 * @param bukuRujukan2 buku rujukan tambahan; boleh {@code null}, tidak divalidasi
	 * @see #getBukuRujukan2()
	 */
	public void setBukuRujukan2(String bukuRujukan2) {
		this.bukuRujukan2 = bukuRujukan2;
	}

}
