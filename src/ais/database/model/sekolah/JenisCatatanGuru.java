package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity master <b>jenis (kategori) Catatan Guru</b> pada modul SEKOLAH, dipetakan ke tabel
 * {@code sekolah.jenis_catatan_guru}.
 *
 * <h3>Peran dalam modul Catatan Guru</h3>
 * <p>&#8220;Catatan Guru&#8221; adalah berkas riwayat/kejadian kepegawaian seorang guru
 * (entity transaksinya {@link CatatanGuru}, yang menyimpan relasi ke {@code Guru}, tahun ajaran,
 * semester, waktu, keterangan, dan jawaban field kustom). Baris kelas ini adalah <b>jenis</b>
 * berkas tersebut &mdash; mis. &#8220;Pembinaan&#8221;, &#8220;Surat Peringatan&#8221;,
 * &#8220;Penghargaan&#8221;, &#8220;Penilaian Kinerja&#8221; &mdash; dan sekaligus menjadi
 * <b>penentu bentuk formulir</b> yang muncul saat catatan diisi.</p>
 * <p>Kelas ini adalah <b>lapis paling atas</b> dari rantai konfigurasi field kustom Catatan Guru
 * yang terdiri atas empat lapis:</p>
 * <ol>
 *   <li>{@link ais.database.model.ParameterTambahan} &mdash; <b>definisi</b> field kustom generik
 *   (label, tipe input, daftar pilihan, wajib/tidak). Dipakai bersama banyak modul.</li>
 *   <li>{@link ParameterTambahanCatatanGuru} &mdash; tabel <b>penghubung</b> yang mengaitkan satu
 *   {@code ParameterTambahan} ke satu kategori.</li>
 *   <li>{@link KelompokParameterTambahanCatatanGuru} &mdash; <b>kategori/heading</b> tempat
 *   field-field tersebut ditampilkan berkelompok pada formulir.</li>
 *   <li><b>Kelas ini</b> &mdash; setiap jenis catatan <b>memilih kategori mana saja</b> yang akan
 *   muncul, lewat relasi {@code @ManyToMany}
 *   {@link #getKelompokParameterTambahanCatatanGurus()}. Sebuah kategori karena itu
 *   <b>tidak</b> otomatis tampil di formulir mana pun; ia harus <b>dicentang lebih dulu</b> pada
 *   layar Jenis Catatan Guru ({@code ais.action.master.sekolah.JenisCatatanGuruAction}, method
 *   {@code initKelompokParameterTambahanCatatanGuru(Rows)}). Terverifikasi dari kode: daftar
 *   checkbox pada layar itu dibangun dari seluruh {@code KelompokParameterTambahanCatatanGuru}
 *   yang ada, dan id kategori yang tercentang diambil dari relasi kelas ini.</li>
 * </ol>
 * <p>Nilai jawaban tidak disimpan di sini maupun di kategori, melainkan sebagai <b>satu string
 * terserialisasi</b> pada {@code CatatanGuru.parameterTambahanInds}, dengan kunci berformat
 * {@code "<idKelompok>-><idParameter>"} dan pemisah baris {@code "\n"} serta pemisah nilai
 * {@code "<=>"} (lihat {@code CatatanGuru.initData(CatatanGuru)} dan
 * {@code ais.action.master.sekolah.helper.ParameterTambahanCatatanGuruListener}).</p>
 *
 * <h3>Yang dibawa jenis catatan selain daftar kategori</h3>
 * <p>Selain memilih kategori, satu baris jenis catatan juga memegang dua <b>berkas template
 * laporan</b> (JasperReports) yang dilampirkan lewat {@code LampiranLain} dengan {@code ref} =
 * {@link #getId()}:</p>
 * <ul>
 *   <li>{@code LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_GURU} &mdash; layout cetak
 *   <i>form</i> per catatan;</li>
 *   <li>{@code LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_GURU} &mdash; layout cetak
 *   <i>rekap</i>.</li>
 * </ul>
 * <p>Ditambah galeri gambar pendukung ber-{@code jenis} berawalan {@code "Catatan_Guru_"}.
 * Ketiganya dirender {@code ais.action.report.format1.sekolah.LaporanCatatanGuru}.</p>
 *
 * <h3>Kuirk terbesar &mdash; cache statis {@link #mapParameters} berumur JVM</h3>
 * <p>{@link #mapParameters} adalah {@code public static} {@link HashMap} yang meng-cache himpunan
 * kategori per id jenis catatan untuk <b>seluruh JVM</b>. {@link #getKelompokParameterTambahanCatatanGurus()}
 * <b>menimpa</b> field instance dengan isi cache ini bila tersedia, dan
 * {@link #setKelompokParameterTambahanCatatanGurus(Set)} menuliskannya kembali. Konsekuensi yang
 * terverifikasi dari kode:</p>
 * <ul>
 *   <li>Tidak ada satu pun pemanggil {@code remove(...)}/{@code clear()} atas map ini di seluruh
 *   codebase &mdash; entri masuk dan tidak pernah keluar (kebocoran memori bertahap, sebanding
 *   jumlah jenis catatan).</li>
 *   <li>Berbeda dari saudaranya {@code JenisCatatanSiswa.mapParameters} (diisi juga oleh
 *   {@code ais.common.InitDataHelper} dan dibaca {@code ais.common.ManajemenProperty}), map milik
 *   kelas ini <b>tidak punya penulis/pembaca di luar berkas ini sendiri</b> &mdash; satu-satunya
 *   jalur pengisiannya adalah setter di kelas ini.</li>
 *   <li>{@link HashMap} <b>tidak</b> thread-safe. Ditulis dari event thread ZK mana pun tanpa
 *   sinkronisasi; penulisan bersamaan bisa merusak struktur internalnya.</li>
 *   <li>Himpunan yang di-cache biasanya berupa {@code PersistentSet} milik session Hibernate yang
 *   sudah <b>ditutup</b> saat permintaan berakhir. Pembacaan berikutnya dari sesi pengguna
 *   <b>lain</b> mendapat referensi yang sama, dengan risiko {@code LazyInitializationException}
 *   atau data basi lintas pengguna.</li>
 * </ul>
 *
 * <h3>Kuirk kedua &mdash; tombol &#8220;Batal&#8221; tidak membatalkan pencentangan</h3>
 * <p>{@code JenisCatatanGuruAction.initKelompokParameterTambahanCatatanGuru(Rows)} menyimpan hasil
 * {@link #getKelompokParameterTambahanCatatanGurus()} ke variabel layarnya <b>sebagai alias
 * langsung</b> (bukan salinan), lalu listener {@code onClick} tiap checkbox memanggil
 * {@code add(...)}/{@code remove(...)} pada himpunan itu. Karena himpunan tersebut adalah koleksi
 * milik entity <b>terkelola</b> (baris sebelumnya memanggil {@code session.refresh(...)}), setiap
 * klik checkbox sudah mengubah state persisten; tombol &#8220;Batal&#8221; hanya menyembunyikan
 * jendela ({@code addWindow.setVisible(false)}) dan <b>tidak</b> mengembalikan apa pun. Pola yang
 * sama pernah dicatat pada {@link ais.database.model.sekolah.PelanggaranDanHukuman}. Di sini efeknya
 * diperkuat {@link #mapParameters}: perubahan yang &#8220;dibatalkan&#8221; tetap terlihat oleh
 * seluruh JVM lewat getter. Dicatat apa adanya &mdash; tidak diperbaiki di sini.</p>
 *
 * <h3>Kuirk ketiga &mdash; {@code TreeSet} dan penciutan senyap kategori</h3>
 * <p>Field {@link #kelompokParameterTambahanCatatanGurus} diinisialisasi sebagai
 * {@code new TreeSet<KelompokParameterTambahanCatatanGuru>()}, sementara
 * {@link KelompokParameterTambahanCatatanGuru#compareTo(GeneralValueObject)} <b>hanya</b>
 * membandingkan {@code nomorUrut} (yang bernilai bawaan {@code 1} untuk semua baris dan tidak
 * disediakan isiannya di layar Tambah/Ubah). {@code TreeSet} memakai {@code compareTo} sebagai
 * definisi kesamaan, sehingga dua kategori berbeda dengan nomor urut sama akan <b>saling menelan
 * secara senyap</b>.</p>
 * <p><b>Nuansa penting yang diverifikasi</b>: pada entity yang sudah <b>dimuat</b> Hibernate, field
 * ini diganti {@code PersistentSet} (berurut sesuai {@code @OrderBy} di tingkat SQL), sehingga
 * penciutan <i>tidak</i> terjadi pada layar Jenis Catatan Guru maupun pada
 * {@code CatatanGuru.initData(CatatanGuru)} dan {@code LaporanCatatanGuru} yang mengiterasi relasi
 * ini langsung. Penciutan <b>benar-benar terjadi</b> di
 * {@code ais.action.master.sekolah.CatatanGuruAction}, yang secara eksplisit menyalin ulang isi
 * relasi ke {@code new TreeSet<...>()} baru sebelum menyerahkannya ke
 * {@code ParameterTambahanCatatanGuruListener} &mdash; yaitu tepat pada <b>formulir pengisian
 * catatan guru</b>, jalur yang paling sering dipakai pengguna. Akibatnya seluruh section field
 * kustom milik kategori yang tertelan hilang dari formulir tanpa pesan kesalahan.</p>
 *
 * <h3>Pola getter mutatif (write-back) &mdash; hasil verifikasi pada kelas ini</h3>
 * <p>Anotasi pemetaan berada pada <b>getter</b> ({@code @Id} pada {@link #getId()}), jadi Hibernate
 * memakai <b>property access</b> untuk semua property kelas ini. Getter yang memodifikasi field
 * karena itu terlihat oleh dirty-check dan, dengan {@code dynamicUpdate=true}, sekadar membaca
 * baris bisa memicu {@code UPDATE} + revisi Envers. Pada kelas ini terverifikasi:</p>
 * <ul>
 *   <li><b>ADA:</b> {@link #getSekolah()} (menulis balik hasil {@code check(...)}),
 *   {@link #getYayasan()} (menulis balik <i>dua</i> field: {@code sekolah} dan {@code yayasan},
 *   sekaligus <b>menurunkan</b> yayasan dari sekolah), dan
 *   {@link #getKelompokParameterTambahanCatatanGurus()} (menimpa field dari cache statis).</li>
 *   <li><b>TIDAK ADA</b> pada {@link #getKode()}, {@link #getNama()}, {@link #getAktif()}: ketiganya
 *   hanya menormalkan nilai kembalian tanpa menulis balik ke field.</li>
 * </ul>
 * <p>Karena property access dipakai juga saat {@code INSERT}, hasil normalisasi ketiga getter di
 * atas <b>ikut tertulis</b> ke DB pada baris baru: {@code kode} tersimpan sebagai {@code ""}
 * (layar master tidak menyediakan isian kode sama sekali), {@code aktif} tersimpan {@code true},
 * dan {@code nama} tersimpan sudah ter-{@code trim}. Kolom {@code yayasan_id} praktis selalu
 * mencerminkan {@code sekolah.yayasan} apa pun yang dipilih pengguna pada combo Yayasan.</p>
 *
 * <h3>Pola berulang lain &mdash; hasil verifikasi</h3>
 * <ul>
 *   <li><b>{@code getKeterangan()} membalik kontrak induk: ADA.</b>
 *   {@link GeneralValueObject#getKeterangan()} menjamin <i>tidak pernah</i> {@code null}
 *   (mengubah {@code null} menjadi {@code ""}); override di kelas ini mengembalikan field apa
 *   adanya sehingga <b>bisa</b> {@code null}. Lihat {@link #getKeterangan()}.</li>
 *   <li><b>{@code getKode()} juga membalik kontrak induk, ke arah sebaliknya:</b> induk
 *   mengembalikan {@code null} apa adanya, kelas ini mengembalikan {@code ""} + {@code trim()}.
 *   Tidak berdampak pada {@link GeneralValueObject#toString()} karena {@link #toString()}
 *   di-override.</li>
 *   <li><b>{@code compareTo()} dipangkas: TIDAK ADA.</b> Kelas ini <b>tidak</b> meng-override
 *   {@link GeneralValueObject#compareTo(GeneralValueObject)}; urutan alaminya tetap berantai penuh
 *   {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr; {@code keterangan}. Karena
 *   {@code nomorUrut} dan {@code nim} milik induk tidak dipetakan Hibernate (lihat catatan warisan)
 *   dan tidak pernah diisi, perbandingan praktis jatuh ke {@link #getNama()}.</li>
 *   <li><b>Penciutan {@code TreeSet}: ADA</b>, tetapi berasal dari {@code compareTo} milik
 *   {@link KelompokParameterTambahanCatatanGuru}, bukan milik kelas ini &mdash; lihat bagian
 *   &#8220;Kuirk ketiga&#8221; di atas.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}/{@link #setId(Long)},
 *   {@link #getOleh()}/{@link #setOleh(String)}, {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()},
 *   {@link #toString()}.</li>
 *   <li><b>Atribut jenis catatan:</b> {@link #getKode()}/{@link #setKode(String)},
 *   {@link #getNama()}/{@link #setNama(String)},
 *   {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 *   <li><b>Penanda perilaku:</b> {@link #getAktif()}/{@link #setAktif(Boolean)} (menentukan apakah
 *   jenis ini muncul pada combo pemilihan jenis di formulir Catatan Guru dan di laporan).</li>
 *   <li><b>Relasi cakupan:</b> {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 *   {@link #getYayasan()}/{@link #setYayasan(Yayasan)}.</li>
 *   <li><b>Konfigurasi formulir:</b> {@link #getKelompokParameterTambahanCatatanGurus()}/
 *   {@link #setKelompokParameterTambahanCatatanGurus(Set)} dan cache statis
 *   {@link #mapParameters}.</li>
 *   <li><b>Konstruktor:</b> {@link #JenisCatatanGuru()}.</li>
 * </ul>
 * <p>Kelas ini <b>tidak</b> memiliki method utilitas/query statis (tidak ada
 * {@code checkCreateDefault()} seperti pada {@link KelompokParameterTambahanCatatanGuru}); satu-satunya
 * anggota {@code static} adalah {@link #mapParameters}.</p>
 *
 * <h3>Catatan warisan &amp; pemetaan (non-obvious)</h3>
 * <ul>
 *   <li>{@link GeneralValueObject} <b>bukan</b> {@code @MappedSuperclass} dan bukan {@code @Entity}
 *   &mdash; ia POJO abstrak biasa, sehingga Hibernate <b>mengabaikan seluruh property-nya</b>.
 *   Itulah sebabnya {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah},
 *   {@code kode}, {@code nama}, {@code keterangan} dideklarasikan <b>ULANG</b> di kelas ini (field
 *   kelas ini <i>membayangi</i> field induk yang bernama sama). Ini <b>keharusan teknis, bukan
 *   bug</b>. Sebaliknya, {@code nomorUrut} dan {@code nim} yang <i>tidak</i> dideklarasikan ulang
 *   memang tidak punya kolom di tabel ini.</li>
 *   <li>{@code kode} dan {@code aktif} tidak punya {@code @Column} eksplisit, sehingga nama
 *   kolomnya mengikuti strategi penamaan default Hibernate (nama property apa adanya, dilipat ke
 *   huruf kecil oleh PostgreSQL).</li>
 *   <li>{@code @Audited} &mdash; setiap perubahan direkam Envers ke tabel revisi; layar master
 *   menampilkan tombol riwayat lewat {@code RevisiHelper.createNewRevisi(...)}.</li>
 *   <li>{@code dynamicInsert}/{@code dynamicUpdate} aktif: SQL hanya menyebut kolom yang benar-benar
 *   berubah.</li>
 *   <li>{@code serialVersionUID} kelas ini <b>identik</b> dengan milik
 *   {@link KelompokParameterTambahanCatatanGuru} &mdash; sisa salin-tempel; tidak berpengaruh karena
 *   berbeda kelas.</li>
 *   <li>Komentar generator di atas deklarasi kelas berbunyi &#8220;Bank generated by hbm2java&#8221;
 *   &mdash; sisa salin-tempel dari {@code ais.database.model.Bank} yang menular ke puluhan entity
 *   AIS; kelas ini tidak ada hubungannya dengan Bank.</li>
 * </ul>
 *
 * <h3>Catatan keamanan pada layar masternya</h3>
 * <p>{@code JenisCatatanGuruAction} <b>lebih baik</b> dari mayoritas keluarga
 * {@code ParameterTambahan*Action}: ia memanggil {@code Common.doCheckSecurity()} di
 * {@code doBeforeCompose}, menggerbangi tombol Tambah dengan {@code CREATE}, tombol Ubah/Hapus
 * dengan {@code UPDATE}/{@code DELETE} lewat {@code Common.copyEditDeleteButtons(...)}, dan
 * checkbox &#8220;Aktif&#8221; pada grid dengan {@code setDisabled(!edit)}. Tidak ada
 * {@code Intbox} nomor urut di layar ini, sehingga pola &#8220;guard Intbox bolong&#8221; tidak
 * berlaku. Yang tetap perlu dicatat:</p>
 * <ul>
 *   <li><b>{@code initCriteria(boolean)} fail-open lintas tenant.</b> Bila combo pencarian
 *   Yayasan/Sekolah tidak dipilih (kondisi bawaan saat layar dibuka), kriteria yang ditambahkan
 *   adalah {@code Restrictions.sqlRestriction("1=1")} &mdash; tanpa penyempitan ke sekolah/yayasan
 *   milik pengguna. Grid karena itu menampilkan jenis catatan guru <b>seluruh sekolah dan
 *   yayasan</b>, dan karena hak {@code UPDATE}/{@code DELETE} bersifat global (bukan per tenant),
 *   pengguna yang berhak ubah di sekolahnya sendiri dapat <b>mengubah dan menghapus</b> baris milik
 *   sekolah lain, termasuk mengunggah ulang template laporan {@code .jrxml}/{@code .jasper}
 *   miliknya. Bentuknya sama dengan temuan fail-open yang sudah didaftarkan pada audit lintas modul
 *   proyek ini.</li>
 *   <li><b>Daftar checkbox kategori juga fail-open.</b> Pada
 *   {@code initKelompokParameterTambahanCatatanGuru(Rows)}, bila konteks yayasan tidak dapat
 *   ditentukan (combo belum terpilih <i>dan</i> {@code SekolahUtil.getSekolah()} mengembalikan
 *   {@code null} &mdash; kondisi normal saat menekan &#8220;Tambah&#8221;), <b>seluruh</b> kategori
 *   dari semua yayasan ikut ditampilkan namanya.</li>
 *   <li><b>Kuirk cakupan auto-seed (sama seperti versi Siswa).</b> Filter pada layar itu
 *   mensyaratkan {@code kelompok.getYayasan() != null} dan {@code kelompok.getSekolah() != null},
 *   sedangkan baris bawaan hasil {@link KelompokParameterTambahanCatatanGuru#checkCreateDefault()}
 *   lahir dengan {@code yayasan}/{@code sekolah} bernilai {@code null}. Kategori bawaan karena itu
 *   <b>tidak pernah bisa dicentang</b> selama konteks sekolah aktif, sampai admin mengisi cakupannya
 *   secara manual.</li>
 *   <li>Checkbox kategori dibuat tanpa {@code setDisabled(!edit)}, namun jendela tempatnya berada
 *   hanya dapat dibuka lewat tombol yang sudah bergerbang {@code CREATE}/{@code UPDATE}.</li>
 * </ul>
 * <p>Dicatat apa adanya &mdash; tidak diperbaiki di sini.</p>
 *
 * <p><b>Konsumen utama:</b> {@code ais.action.master.sekolah.JenisCatatanGuruAction} (CRUD master +
 * pencentangan kategori), {@code ais.action.master.sekolah.CatatanGuruAction} (layar transaksi;
 * pemilih jenis dan perakit formulir field kustom), {@link CatatanGuru} (pemilik data isian,
 * {@code initData}), {@code ais.action.report.format1.sekolah.LaporanCatatanGuru} (laporan form dan
 * rekap), {@code ais.action.master.catatan.DasbordCatatan} (dasbor lintas jenis catatan),
 * {@code ais.action.master.dashboard.admin.DasboardGuru} (statistik jumlah jenis &amp; jenis
 * terbanyak), dan {@code ais.action.servlet.api.LaporanApi} (endpoint laporan berbasis id jenis).</p>
 *
 * <p><b>Konkurensi:</b> instance tidak thread-safe (POJO Hibernate biasa); jangan berbagi lintas
 * session/thread. Perhatikan terutama {@link #mapParameters} yang justru <b>sengaja</b> dibagikan
 * lintas seluruh JVM &mdash; lihat bagian kuirk pertama.</p>
 *
 * @see KelompokParameterTambahanCatatanGuru
 * @see ParameterTambahanCatatanGuru
 * @see CatatanGuru
 * @see ais.database.model.ParameterTambahan
 * @see ais.database.model.JenisCatatanPegawai
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jenis_catatan_guru")
public class JenisCatatanGuru extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar instance lama tetap dapat dideserialisasi (relevan
	 * untuk sesi ZK yang di-passivate ke disk).
	 *
	 * <p>Nilainya <b>identik</b> dengan milik {@link KelompokParameterTambahanCatatanGuru} dan
	 * saudara sekeluarga lainnya &mdash; sisa salin-tempel; tidak berpengaruh karena berbeda
	 * kelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris jenis catatan guru. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi jalur audit. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; diisi jalur audit. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di kelas ini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; lihat catatan warisan pada dokumentasi kelas.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Kuirk:</b> nilai {@code null}, kosong, atau hanya spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return}), sehingga nilai lama bertahan dan jejak audit tidak pernah
	 * bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Kuirk:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong/spasi
	 * diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di kelas ini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; lihat catatan warisan pada dokumentasi kelas.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE}
	 * baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan {@link #setTanggal_dirubah(Date)}
	 * dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati callback
	 * ini, sehingga jenis catatan yang baru dibuat masuk <b>tanpa jejak</b> {@code oleh}/
	 * {@code olehId}.</p>
	 *
	 * <p>Perlu diingat bahwa {@link #getSekolah()}, {@link #getYayasan()}, dan
	 * {@link #getKelompokParameterTambahanCatatanGurus()} dapat mengotori field saat baris sekadar
	 * dibaca, sehingga callback ini bisa ikut terpicu pada {@code UPDATE} yang <b>tidak diminta
	 * pengguna mana pun</b> &mdash; jejak audit lalu mencatat pengguna yang kebetulan sedang membuka
	 * layar.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * <p>Umumnya diisi otomatis lewat {@link #onUpdate()}; pemanggilan manual akan tertimpa pada
	 * {@code UPDATE} berikutnya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; {@code null} diterima
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * <p>Tidak pernah {@code null} pada objek yang baru dibuat di JVM (diinisialisasi saat
	 * konstruksi), tetapi <b>bisa</b> {@code null} untuk baris lama hasil impor/migrasi.</p>
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini, meng-override {@link GeneralValueObject#toString()}.
	 *
	 * <p>Formatnya {@code "<id>-<nama>"}, bukan {@code "<kode> - <nama>"} milik induk. Perhatikan
	 * dua hal: method ini membaca <b>field</b> {@code nama} secara langsung (bukan lewat
	 * {@link #getNama()}), sehingga nilainya <i>tidak</i> ter-{@code trim}; dan baris yang belum
	 * disimpan menghasilkan awalan {@code "null-"}.</p>
	 *
	 * <p>Tidak dipakai sebagai label combo pada layar mana pun &mdash; {@code Common.insertCombo(...)}
	 * di {@code CatatanGuruAction} dan {@code LaporanCatatanGuru} menyebut property
	 * {@code "nama"}/{@code "kode"} secara eksplisit. Praktis hanya muncul di log debug.</p>
	 *
	 * @return teks {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode ringkas jenis catatan. Lihat {@link #getKode()} &mdash; tidak ada isiannya di layar
	 * master, sehingga praktis selalu kosong.
	 */
	private String kode;

	/** Nama jenis catatan guru; satu-satunya isian wajib di layar master. Lihat {@link #getNama()}. */
	private String nama;
	/** Sekolah pemilik baris (cakupan). Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Yayasan pemilik baris (cakupan); diturunkan dari sekolah. Lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Keterangan bebas jenis catatan. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda aktif/tidak. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Cache <b>statis, berumur JVM</b> yang memetakan {@link #getId()} sebuah jenis catatan ke
	 * himpunan kategori field kustom miliknya.
	 *
	 * <p>Dibaca {@link #getKelompokParameterTambahanCatatanGurus()} (menimpa field instance) dan
	 * ditulis {@link #setKelompokParameterTambahanCatatanGurus(Set)}. <b>Tidak ada penulis maupun
	 * pembaca lain di seluruh codebase</b> &mdash; berbeda dari
	 * {@code JenisCatatanSiswa.mapParameters} yang juga diisi {@code ais.common.InitDataHelper} dan
	 * dibaca {@code ais.common.ManajemenProperty}.</p>
	 *
	 * <p><b>Peringatan:</b> {@code public}, mutable, {@link HashMap} (tidak thread-safe), tidak
	 * pernah dibersihkan, dan menyimpan koleksi Hibernate yang mungkin sudah terlepas dari
	 * session-nya. Lihat pembahasan lengkapnya pada dokumentasi kelas. Jangan menambah pemakaian
	 * baru; gunakan relasi {@code @ManyToMany}-nya langsung.</p>
	 */
	public static Map<Long, Set<KelompokParameterTambahanCatatanGuru>> mapParameters = new HashMap<Long, Set<KelompokParameterTambahanCatatanGuru>>();

	/**
	 * Himpunan kategori field kustom yang dicentang untuk jenis catatan ini.
	 *
	 * <p>Diinisialisasi sebagai {@code TreeSet} &mdash; relevan hanya untuk instance baru yang belum
	 * dimuat Hibernate; pada entity terkelola field ini diganti {@code PersistentSet}. Lihat
	 * {@link #getKelompokParameterTambahanCatatanGurus()} dan bagian kuirk {@code TreeSet} pada
	 * dokumentasi kelas.</p>
	 */
	private Set<KelompokParameterTambahanCatatanGuru> kelompokParameterTambahanCatatanGurus = new TreeSet<KelompokParameterTambahanCatatanGuru>();

	/**
	 * Mengembalikan himpunan {@link KelompokParameterTambahanCatatanGuru} (kategori/heading field
	 * kustom) yang <b>dicentang</b> untuk jenis catatan ini. Inilah mekanisme yang menentukan
	 * section mana saja yang muncul pada formulir isian Catatan Guru: sebuah kategori tidak tampil
	 * di formulir apa pun sampai ia tercentang di sini.
	 *
	 * <p>Dipetakan sebagai {@code @ManyToMany} lewat tabel penghubung
	 * {@code sekolah.jenis_catatan_guru_has_parameter} (kolom {@code jenis_catatan_guru} &rarr;
	 * kelas ini, kolom {@code parameter} &rarr; kategori), dengan {@code cascade = MERGE} saja
	 * &mdash; menghapus jenis catatan <b>tidak</b> menghapus kategorinya (benar), dan menyimpan
	 * jenis catatan tidak akan menyimpan kategori yang belum pernah dipersistenkan.
	 * {@code @OrderBy("nomorUrut asc, nama asc")} mengurutkan hasil di tingkat SQL.</p>
	 *
	 * <p><b>Efek samping (getter mutatif):</b> bila {@link #getId()} sudah terisi <i>dan</i>
	 * {@link #mapParameters} memuat entri untuk id tersebut, isi cache statis itu <b>menimpa</b>
	 * field instance sebelum dikembalikan. Artinya nilai yang dikembalikan belum tentu berasal dari
	 * DB maupun dari objek ini: ia bisa berasal dari sesi pengguna lain yang lebih dulu memanggil
	 * setternya, bahkan setelah session Hibernate asalnya ditutup.</p>
	 *
	 * <p><b>Peringatan alias:</b> yang dikembalikan adalah koleksi <b>hidup</b> milik entity, bukan
	 * salinan. Pemanggil yang memodifikasinya (mis. daftar checkbox di
	 * {@code JenisCatatanGuruAction}) langsung mengubah state persisten &mdash; lihat kuirk
	 * &#8220;Batal tidak membatalkan&#8221; pada dokumentasi kelas. Bila hanya perlu membaca,
	 * salinlah dulu.</p>
	 *
	 * <p><b>Pemanggil:</b> {@code JenisCatatanGuruAction.initKelompokParameterTambahanCatatanGuru(Rows)}
	 * (menandai checkbox), {@code CatatanGuruAction} (dua tempat: renderer grid dan perakit
	 * formulir), {@code CatatanGuru.initData(CatatanGuru)} (perakit map laporan), dan
	 * {@code LaporanCatatanGuru}.</p>
	 *
	 * @return himpunan kategori yang tercentang; tidak pernah {@code null}, bisa kosong
	 */
	@ManyToMany(targetEntity = KelompokParameterTambahanCatatanGuru.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nomorUrut asc, nama asc")
	@JoinTable(name = "jenis_catatan_guru_has_parameter", schema = "sekolah", joinColumns = @JoinColumn(name = "jenis_catatan_guru"), inverseJoinColumns = @JoinColumn(name = "parameter"))
	public Set<KelompokParameterTambahanCatatanGuru> getKelompokParameterTambahanCatatanGurus() {
		if (id != null) {
			Set<KelompokParameterTambahanCatatanGuru> temp = mapParameters.get(id);
			if (temp != null) {
				kelompokParameterTambahanCatatanGurus = temp;
			}
		}
		return kelompokParameterTambahanCatatanGurus;
	}

	/**
	 * Menyetel himpunan kategori field kustom yang dicentang untuk jenis catatan ini.
	 *
	 * <p><b>Efek samping penting:</b> selain mengisi field instance, method ini <b>menuliskan
	 * referensi himpunan yang sama ke cache statis {@link #mapParameters}</b> bila {@link #getId()}
	 * sudah terisi. Sejak saat itu setiap instance {@code JenisCatatanGuru} ber-id sama di seluruh
	 * JVM akan mengembalikan himpunan ini lewat
	 * {@link #getKelompokParameterTambahanCatatanGurus()}, dan setiap mutasi terhadapnya terlihat
	 * oleh semua pengguna &mdash; termasuk mutasi yang belum (atau tidak jadi) disimpan.</p>
	 *
	 * <p>Tidak ada penyalinan defensif: himpunan milik pemanggil dipegang apa adanya. Untuk baris
	 * baru yang {@code id}-nya masih {@code null}, cache tidak diisi (dan tidak pernah diisi
	 * belakangan setelah {@code id} terbentuk, karena tidak ada pemanggil yang mengulang setter
	 * sesudah simpan).</p>
	 *
	 * <p><b>Pemanggil:</b> hanya {@code JenisCatatanGuruAction.onSave(Event)}.</p>
	 *
	 * @param kelompokParameterTambahanCatatanGurus himpunan kategori baru; disimpan apa adanya,
	 *        {@code null} diterima tetapi akan membuat getter mengembalikan {@code null} pada baris
	 *        yang belum ter-cache
	 */
	public void setKelompokParameterTambahanCatatanGurus(
			Set<KelompokParameterTambahanCatatanGuru> kelompokParameterTambahanCatatanGurus) {
		this.kelompokParameterTambahanCatatanGurus = kelompokParameterTambahanCatatanGurus;
		if (id != null) {
			mapParameters.put(id, kelompokParameterTambahanCatatanGurus);
		}
	}

	/**
	 * Konstruktor tanpa argumen. Wajib ada untuk Hibernate dan dipakai
	 * {@code JenisCatatanGuruAction.onAdd(Event)} saat membuat baris baru.
	 *
	 * <p>Seluruh field dibiarkan pada nilai bawaannya, kecuali {@code tanggal_dirubah} (diisi jam
	 * aplikasi) dan {@link #kelompokParameterTambahanCatatanGurus} (himpunan kosong bertipe
	 * {@code TreeSet}).</p>
	 */
	public JenisCatatanGuru() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Dibangkitkan DB dengan strategi {@code IDENTITY}, karena itu {@code insertable = false}.
	 * Bernilai {@code null} selama baris belum disimpan &mdash; kondisi yang dipakai
	 * {@link #getKelompokParameterTambahanCatatanGurus()}/
	 * {@link #setKelompokParameterTambahanCatatanGurus(Set)} untuk memutuskan apakah cache statis
	 * dipakai, dan dipakai layar master untuk memilih judul &#8220;Tambah&#8221; atau
	 * &#8220;Ubah&#8221;.</p>
	 *
	 * <p>Nilai ini juga menjadi {@code ref} bagi lampiran {@code LampiranLain} (template
	 * {@code .jrxml}/{@code .jasper} dan galeri gambar) milik jenis catatan ini.</p>
	 *
	 * @return primary key, atau {@code null} bila baris belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key baris ini. Tanpa validasi.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate sesudah {@code INSERT}; pemanggilan manual berisiko
	 * membuat entity dianggap baris lain.</p>
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode ringkas jenis catatan, dengan <b>normalisasi</b>: {@code null} menjadi
	 * {@code ""} dan sisanya di-{@code trim}.
	 *
	 * <p><b>Kontrak induk dibalik:</b> {@link GeneralValueObject#getKode()} mengembalikan nilai apa
	 * adanya (bisa {@code null}); override di sini menjamin tidak pernah {@code null}. Karena
	 * {@link #toString()} juga di-override, pembalikan ini tidak berdampak pada representasi teks
	 * induk.</p>
	 *
	 * <p><b>Kuirk:</b> layar master {@code JenisCatatanGuruAction} <b>tidak menyediakan isian
	 * kode</b> sama sekali, dan {@code onSave(Event)} tidak pernah memanggil {@link #setKode(String)}.
	 * Karena Hibernate memakai property access, nilai hasil normalisasi ({@code ""}) yang tertulis
	 * ke kolom {@code kode} pada {@code INSERT}. Property ini tetap disebut sebagai kandidat label
	 * combo di {@code CatatanGuruAction} dan {@code LaporanCatatanGuru}
	 * ({@code new String[] { "nama", "kode" }}), sehingga bagian kode pada label selalu kosong.</p>
	 *
	 * @return kode jenis catatan yang sudah di-trim, atau {@code ""}; tidak pernah {@code null}
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode ringkas jenis catatan. Tanpa validasi dan tanpa normalisasi.
	 *
	 * <p>Tidak dipanggil dari layar mana pun; hanya relevan bagi Hibernate dan jalur impor
	 * ({@code Common.uploadData(...)} pada layar master).</p>
	 *
	 * @param kode kode baru; {@code null} diterima
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis catatan guru, sudah di-{@code trim}.
	 *
	 * <p>Kolom {@code nama} bertipe {@code text} dan {@code NOT NULL} di DB; validasi
	 * &#8220;harus diisi&#8221; dilakukan di {@code JenisCatatanGuruAction.onSave(Event)}. Getter
	 * ini <b>tetap</b> bisa mengembalikan {@code null} untuk objek yang belum diisi di memori.</p>
	 *
	 * <p>Nilai inilah yang tampil sebagai label pada combo pemilih jenis di formulir Catatan Guru,
	 * pada laporan ({@code parameters.put("jenisCatatanGuru", j.getNama())}), pada dasbor
	 * ({@code DasbordCatatan}, {@code DasboardGuru}), dan sebagai judul entri riwayat Envers.</p>
	 *
	 * <p>Karena property access dipakai, hasil {@code trim()} inilah yang benar-benar tertulis ke
	 * DB &mdash; spasi di ujung yang diketik pengguna tidak pernah tersimpan.</p>
	 *
	 * @return nama jenis catatan yang sudah di-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis catatan guru. Tanpa validasi &mdash; pemeriksaan &#8220;tidak boleh
	 * kosong&#8221; berada di {@code JenisCatatanGuruAction.onSave(Event)}.
	 *
	 * @param nama nama baru; {@code null} diterima di memori tetapi akan ditolak DB
	 *        ({@code NOT NULL}) saat flush
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas jenis catatan ini, <b>apa adanya</b>.
	 *
	 * <p><b>Kontrak induk dibalik:</b> {@link GeneralValueObject#getKeterangan()} menjamin hasilnya
	 * <i>tidak pernah</i> {@code null} (nilai {@code null} dinormalkan menjadi {@code ""}); override
	 * di kelas ini menghilangkan jaminan tersebut. Pemanggil <b>wajib</b> memeriksa {@code null}
	 * sendiri.</p>
	 *
	 * <p>Konsekuensi yang perlu diketahui:</p>
	 * <ul>
	 *   <li>Cabang {@code keterangan} pada {@link GeneralValueObject#compareTo(GeneralValueObject)}
	 *   (yang dijaga {@code != null}) kini benar-benar bisa dilewati untuk entity ini &mdash;
	 *   berbeda dari entity lain yang memakai getter induk.</li>
	 *   <li>{@code JenisCatatanGuruAction} merender nilainya langsung ke {@code new Label(...)}, dan
	 *   {@code Common.insertCombo(...)} memakainya sebagai tooltip combo; keduanya menerima
	 *   {@code null}.</li>
	 * </ul>
	 *
	 * @return keterangan jenis catatan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas jenis catatan ini. Tanpa validasi.
	 *
	 * <p>Diisi dari {@code Textbox} keterangan pada layar master; nilai {@code ""} dari textbox
	 * kosong tersimpan sebagai string kosong, bukan {@code null}.</p>
	 *
	 * @param keterangan keterangan baru; {@code null} diterima
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda aktif jenis catatan ini, dengan <b>normalisasi</b>: {@code null}
	 * dianggap {@code true} (aktif).
	 *
	 * <p>Penanda ini menentukan apakah jenis catatan muncul di combo pemilih pada
	 * {@code CatatanGuruAction} dan {@code LaporanCatatanGuru} &mdash; keduanya menyaring dengan
	 * {@code Restrictions.eq("aktif", true)} di tingkat SQL.</p>
	 *
	 * <p><b>Catatan konsistensi:</b> filter SQL {@code aktif = true} <i>tidak</i> menjaring baris
	 * ber-{@code aktif} {@code NULL}, padahal getter ini menyebutnya aktif &mdash; grid master akan
	 * menampilkan checkbox tercentang untuk baris yang justru tidak pernah muncul di combo. Kondisi
	 * itu <b>tidak</b> tercapai lewat jalur aplikasi: karena Hibernate memakai property access,
	 * nilai hasil normalisasi ({@code true}) ikut tertulis saat {@code INSERT} meski
	 * {@code JenisCatatanGuruAction.onSave(Event)} tidak pernah memanggil
	 * {@link #setAktif(Boolean)}. Baris ber-{@code NULL} hanya mungkin lahir dari SQL mentah atau
	 * migrasi.</p>
	 *
	 * <p>Getter ini <b>tidak</b> menulis balik ke field &mdash; berbeda dari
	 * {@code getAktif()} pada {@link KelompokParameterTambahanCatatanGuru}.</p>
	 *
	 * @return {@code true} bila jenis catatan aktif atau penandanya belum diisi; {@code false} bila
	 *         dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif jenis catatan ini. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox &#8220;Aktif&#8221; pada grid master,
	 * yang langsung disusul {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan tersimpan
	 * seketika tanpa tombol Simpan. Checkbox itu sendiri sudah bergerbang hak {@code UPDATE}
	 * ({@code setDisabled(!edit)}).</p>
	 *
	 * @param aktif penanda baru; {@code null} akan terbaca sebagai {@code true} oleh
	 *        {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}


	/**
	 * Mengembalikan sekolah pemilik jenis catatan ini (cakupan/tenant).
	 *
	 * <p><b>Efek samping (getter mutatif):</b> memanggil {@link GeneralValueObject#check(Object)}
	 * dan <b>menulis hasilnya kembali</b> ke field {@code sekolah}. {@code check(...)} meresolusi
	 * proxy lazy lewat cache identitas entity; instance yang dikembalikan bisa berbeda dari yang
	 * disimpan sebelumnya. Karena Hibernate memakai property access, pembacaan ini terlihat oleh
	 * dirty-check.</p>
	 *
	 * <p>Dipakai sebagai filter utama pemilihan jenis catatan pada formulir dan laporan
	 * ({@code Restrictions.eq("sekolah", s)}), dan ditampilkan sebagai kolom pada grid master.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik jenis catatan ini.
	 *
	 * <p><b>Kuirk:</b> objek {@code Sekolah} yang belum tersimpan (id-nya {@code null}) dianggap
	 * sama dengan {@code null} dan <b>dibuang diam-diam</b>. Ini mencegah {@code cascade PERSIST}
	 * ikut menyimpan sekolah baru yang tidak diinginkan, tetapi juga berarti kesalahan pemanggil
	 * tidak pernah dilaporkan &mdash; relasi hanya jadi kosong.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id disimpan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik jenis catatan ini (cakupan/tenant tingkat atas).
	 *
	 * <p><b>Efek samping paling destruktif di kelas ini.</b> Method ini tidak sekadar membaca:</p>
	 * <ol>
	 *   <li>memanggil {@link #getSekolah()} &mdash; yang sudah menulis balik field {@code sekolah};</li>
	 *   <li>bila sekolah ada, <b>menimpa</b> field {@code yayasan} dengan
	 *   {@code sekolah.getYayasan()}, mengabaikan nilai yang tersimpan di kolom
	 *   {@code yayasan_id};</li>
	 *   <li>menjalankan {@link GeneralValueObject#check(Object)} atas hasilnya dan menulis balik
	 *   sekali lagi.</li>
	 * </ol>
	 * <p>Akibatnya, sekadar <b>membaca</b> yayasan pada baris yang {@code yayasan_id}-nya berbeda
	 * dari yayasan sekolahnya sudah cukup untuk membuat entity kotor, memicu {@code UPDATE}
	 * (dengan {@code dynamicUpdate}) beserta revisi Envers dan callback {@link #onUpdate()} &mdash;
	 * tanpa ada pengguna yang meminta perubahan. Nilai yang dipilih pengguna pada combo Yayasan di
	 * layar master karena itu <b>tidak pernah bertahan</b> bila berbeda dari yayasan milik sekolah
	 * yang dipilih; kolom {@code yayasan_id} praktis selalu turunan {@code sekolah.yayasan}.</p>
	 * <p>Kuirk yang sama ada pada {@link KelompokParameterTambahanCatatanGuru#getYayasan()} dan
	 * tidak ada pada padanan versi perguruan tinggi. Dicatat apa adanya &mdash; tidak diperbaiki di
	 * sini.</p>
	 *
	 * @return yayasan pemilik (diturunkan dari sekolah bila sekolah terisi), atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik jenis catatan ini.
	 *
	 * <p><b>Kuirk ganda:</b> (1) objek {@code Yayasan} tanpa id dianggap {@code null} dan dibuang
	 * diam-diam, sama seperti {@link #setSekolah(Sekolah)}; (2) nilai apa pun yang disetel di sini
	 * akan <b>ditimpa</b> oleh {@link #getYayasan()} pada pembacaan berikutnya selama
	 * {@link #getSekolah()} tidak {@code null}. Setter ini karena itu hanya efektif untuk baris
	 * tanpa sekolah.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id disimpan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

}
