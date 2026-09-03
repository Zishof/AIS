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
 * Master <b>jenis catatan siswa</b> &mdash; lapis PALING LUAR (puncak) rantai konfigurasi field
 * kustom modul <b>Catatan Siswa</b> jenjang SEKOLAH.
 *
 * <p>Satu baris entity ini mewakili satu <i>kategori catatan</i> yang dipilih guru/wali kelas saat
 * membuat catatan untuk seorang siswa (mis. lewat combobox "Jenis" pada layar Catatan Siswa).
 * Selain memberi label pada catatan, baris ini <b>menentukan formulir mana yang muncul</b>: hanya
 * kategori field kustom ({@link KelompokParameterTambahanCatatanSiswa}) yang <b>DICENTANG</b> pada
 * jenis catatan inilah yang akan dirender sebagai isian tambahan.</p>
 *
 * <h2>Rantai empat lapis</h2>
 * <ol>
 *   <li>{@link ais.database.model.ParameterTambahan} &mdash; definisi field kustom generik (label,
 *       tipe isian, daftar pilihan) yang dipakai bersama SELURUH modul AIS.</li>
 *   <li>{@link KelompokParameterTambahanCatatanSiswa} &mdash; kategori/heading tempat field-field
 *       tersebut dikelompokkan pada formulir.</li>
 *   <li>{@link ParameterTambahanCatatanSiswa} &mdash; tabel penghubung yang memetakan pasangan
 *       (kelompok &rarr; parameter).</li>
 *   <li><b>{@code JenisCatatanSiswa}</b> (kelas ini) &mdash; jenis catatan; relasi
 *       {@code @ManyToMany} {@link #getKelompokParameterTambahanCatatanSiswas()} adalah daftar
 *       centang kelompok mana yang berlaku untuk jenis ini.</li>
 * </ol>
 *
 * <p>Nilai isian yang diketik pengguna <b>tidak</b> disimpan di sini, melainkan didenormalisasi ke
 * kolom teks pada entity pemilik data {@link CatatanSiswa} ({@code parameterTambahanInds} dan
 * kembarannya), dengan format ruas dipisah {@code "\n"} antarbaris dan {@code "<=>"} antar-ruas.
 * {@link CatatanSiswa#getJenisCatatanSiswa()} adalah sisi seberang relasi ini
 * ({@code @ManyToOne}, kolom FK {@code jenis_catatan_siswa}).</p>
 *
 * <h2>Layar &amp; jalur pemakai (terverifikasi)</h2>
 * <ul>
 *   <li>Master: {@code ais.action.master.sekolah.JenisCatatanSiswaAction} +
 *       {@code /pages/master/sekolah/jenis_catatan_siswa.zul} &mdash; juga di-<i>embed</i> sebagai
 *       tab di layar Catatan Siswa ({@code CatatanSiswaAction.onJenisCatatanSiswa()}).</li>
 *   <li>Transaksi: {@code ais.action.master.sekolah.CatatanSiswaAction} (combobox pilihan jenis).</li>
 *   <li>Dasbor: {@code ais.action.master.catatan.DasbordCatatan} (label jenis pada baris catatan).</li>
 *   <li>Laporan: {@code ais.action.report.format1.sekolah.LaporanCatatanSiswa} (filter + judul
 *       kelompok parameter) dan {@code ...LaporanRaporSiswa} (blok catatan pada rapor).</li>
 *   <li>REST/mobile: {@code ais.action.servlet.api.CatatanApi} (rute {@code catatan_siswa_jenis},
 *       {@code catatan_siswa_parameter}, {@code catatan_siswa_daftar}, {@code catatan_siswa_detail},
 *       {@code catatan_siswa_simpan}, {@code catatan_siswa_hapus} pada
 *       {@code ApiRouteRegistry}) dan {@code ais.action.servlet.api.LaporanApi}.</li>
 *   <li>Startup: {@code ais.common.InitData} (kelas ini terdaftar di {@code initClasses(...)}) dan
 *       {@code ais.common.InitDataHelper.handleJenisCatatanSiswa()} yang mengisi cache
 *       {@link #mapParameters}.</li>
 *   <li>Ekspor properti/JSON: {@code ais.common.ManajemenProperty} punya <i>special case</i>
 *       khusus untuk kelas ini (lihat catatan pada {@link #mapParameters}).</li>
 * </ul>
 *
 * <h2>Tidak ada data bawaan (auto-seed)</h2>
 * <p>Berbeda dari lapis kelompok di bawahnya, kelas ini <b>tidak punya</b>
 * {@code checkCreateDefault()} dan tidak ada satu pun skrip SQL/XML di repositori yang menyisipkan
 * baris ke {@code sekolah.jenis_catatan_siswa}. Artinya <b>seluruh nama jenis catatan diketik
 * sendiri oleh admin sekolah</b> &mdash; tidak ada daftar bawaan semacam
 * "Pelanggaran"/"Prestasi"/"Konseling" yang dijamin ada. Konsekuensi praktisnya: instalasi baru
 * membuka layar Catatan Siswa dengan combobox jenis yang KOSONG sampai admin mengisi master ini.</p>
 *
 * <h2>Warisan {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa, sehingga Hibernate <b>tidak memetakan properti induknya sama sekali</b>.
 * Karena itu {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()}, dan
 * {@link #getTanggal_dirubah()} <b>harus</b> dideklarasikan ulang di kelas ini; hal yang sama
 * berlaku untuk field {@code nama} dan {@code keterangan} yang <i>menutupi</i> (shadow) field
 * senama milik induk. Duplikasi tersebut adalah KEHARUSAN TEKNIS, bukan bug &mdash; jangan
 * "dirapikan" dengan menghapusnya.</p>
 * <p>Efek lanjutannya pada pengurutan: {@link GeneralValueObject#compareTo(GeneralValueObject)}
 * <b>tidak</b> di-{@code override} di sini, dan implementasi induk memanggil <i>getter</i> (bukan
 * field) sehingga tetap bekerja secara virtual. Kunci {@code nomorUrut} dan {@code nim} milik
 * induk selalu {@code null} pada instance ini (tidak pernah diisi Hibernate), jadi pengurutan
 * praktis selalu jatuh ke kunci ketiga, yaitu {@link #getNama()}.</p>
 *
 * <h2>Kelompok method</h2>
 * <ul>
 *   <li><b>Jejak audit (deklarasi ulang):</b> {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #onUpdate()},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}.</li>
 *   <li><b>Identitas &amp; representasi:</b> {@link #JenisCatatanSiswa()}, {@link #getId()},
 *       {@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Atribut jenis:</b> {@link #getKode()}, {@link #setKode(String)}, {@link #getNama()},
 *       {@link #setNama(String)}, {@link #getKeterangan()}, {@link #setKeterangan(String)},
 *       {@link #getAktif()}, {@link #setAktif(Boolean)}.</li>
 *   <li><b>Daftar centang kategori field kustom (relasi inti):</b> {@link #mapParameters},
 *       {@link #getKelompokParameterTambahanCatatanSiswas()},
 *       {@link #setKelompokParameterTambahanCatatanSiswas(Set)}.</li>
 *   <li><b>Cakupan multi-tenant:</b> {@link #getSekolah()}, {@link #setSekolah(Sekolah)},
 *       {@link #getYayasan()}, {@link #setYayasan(Yayasan)}.</li>
 * </ul>
 *
 * <h2>Kuirk &amp; catatan penting</h2>
 * <ul>
 *   <li><b>{@link #getKeterangan()} MEMBALIK kontrak base class.</b>
 *       {@code GeneralValueObject.getKeterangan()} menormalkan {@code null} menjadi {@code ""} dan
 *       menjanjikan hasil non-null; override di sini mengembalikan field mentah, jadi pemanggil
 *       <b>wajib</b> menyiapkan diri terhadap {@code null}. Ini kebalikan dari
 *       {@link #getKode()}/{@link #getAktif()} pada kelas yang sama yang justru menormalkan
 *       {@code null}. Salah satu jalur pemakai sudah "tahu diri" dan memasang penjaga sendiri
 *       ({@code CatatanApi.jenisSiswa()} membungkusnya dengan {@code safeString(...)}).</li>
 *   <li><b>Cache statis {@link #mapParameters} berumur JVM</b> &mdash; getter relasi dapat
 *       MENIMPA hasil muat Hibernate dengan isi cache. Lihat uraian lengkap pada field tersebut;
 *       ini sumber bug paling berbahaya di berkas ini.</li>
 *   <li><b>Getter relasi berefek samping (menulis field).</b> {@link #getSekolah()} menimpa
 *       {@code sekolah} dengan hasil de-proxy, {@link #getYayasan()} bahkan menimpa {@code yayasan}
 *       dari {@code getSekolah().getYayasan()} &mdash; membaca baris ini bisa memicu {@code UPDATE}
 *       yang tidak diminta siapa pun (entity {@code @Audited}, jadi ikut tercatat Envers).</li>
 *   <li><b>Kolom {@code aktif} tidak pernah diisi layar master</b> &mdash; bug nyata dengan akibat
 *       "jenis baru tidak muncul di formulir". Lihat {@link #getAktif()}.</li>
 *   <li><b>Kolom {@code kode} praktis mati</b> &mdash; dipetakan dan dipakai sebagai kolom tampilan
 *       combobox, tetapi tak satu pun jalur penulis mengisinya. Lihat {@link #getKode()}.</li>
 *   <li><b>Penamaan kolom join yang menyesatkan:</b> kolom {@code parameter} pada tabel
 *       {@code sekolah.jenis_catatan_siswa_has_parameter} sebenarnya menyimpan id
 *       <i>KELOMPOK</i> ({@link KelompokParameterTambahanCatatanSiswa}), bukan id
 *       {@link ais.database.model.ParameterTambahan}. Pola salah-nama yang sama muncul di seluruh
 *       keluarga {@code Jenis*} (bandingkan {@link ais.database.model.JenisCatatanMahasiswa} versi
 *       PT).</li>
 * </ul>
 *
 * <h2>Catatan keamanan (hasil telusur jalur pemakai)</h2>
 * <ul>
 *   <li><b>Master tanpa penyaring tenant.</b> {@code JenisCatatanSiswaAction.initCriteria()} tidak
 *       memasang batasan {@code sekolah}/{@code yayasan} apa pun selain nilai combobox pencarian
 *       yang <i>opsional</i>; bila kosong, kriterianya {@code 1=1}. Operator satu sekolah dapat
 *       melihat (dan, karena tombol edit/hapus hanya dijaga hak akses menu, menyunting) master
 *       jenis catatan milik sekolah/yayasan lain. Konsisten dengan pola broken access control yang
 *       sudah didaftar untuk keluarga master {@code sekolah}.</li>
 *   <li><b>Rute REST {@code catatan_siswa_*} hanya memeriksa "token valid".</b> Tidak ada
 *       pemeriksaan kepemilikan/tenant: {@code CatatanApi.daftar()} menghormati id {@code siswa}
 *       yang dikirim penyerang, sedangkan {@code detail()}/{@code simpan()}/{@code hapus()}
 *       menerima id {@link CatatanSiswa} sembarang. Karena id memakai {@code IDENTITY} berurutan,
 *       seluruh catatan siswa (termasuk catatan bernada disiplin/konseling atas anak di bawah
 *       umur) dapat dibaca, diubah, bahkan DIHAPUS lintas sekolah/yayasan oleh pemegang token apa
 *       pun &mdash; termasuk akun siswa dan orang tua. Endpoint {@code catatan_siswa_jenis} sendiri
 *       hanya membocorkan metadata master (nama/keterangan jenis) lintas tenant, dampaknya jauh
 *       lebih ringan.</li>
 * </ul>
 *
 * @see KelompokParameterTambahanCatatanSiswa
 * @see ParameterTambahanCatatanSiswa
 * @see CatatanSiswa
 * @see ais.database.model.JenisCatatanMahasiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jenis_catatan_siswa")
public class JenisCatatanSiswa extends GeneralValueObject {

	/** 
	 * 
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama; nilai dibangkitkan {@code IDENTITY} oleh basis data. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi otomatis oleh {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; diisi otomatis oleh {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah di-{@code UPDATE}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah.
	 *
	 * <p><b>Setter defensif:</b> nilai {@code null} maupun string kosong/spasi <b>diabaikan</b>
	 * (method langsung {@code return}), sehingga jejak audit lama tidak pernah terhapus oleh
	 * konteks pengguna yang gagal diresolusi.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah.
	 *
	 * <p><b>Setter defensif:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong/spasi diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila baris belum pernah di-{@code UPDATE}
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
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati
	 * callback ini, jadi baris yang baru dibuat masuk <b>tanpa jejak</b> {@code oleh}/{@code
	 * olehId} sampai ada penyuntingan pertama.</p>
	 *
	 * <p>Perhatikan bahwa {@link #getSekolah()} dan {@link #getYayasan()} dapat mengotori field
	 * saat baris sekadar DIBACA, sehingga callback ini bisa ikut terpicu pada {@code UPDATE} yang
	 * <b>tidak diminta pengguna mana pun</b> &mdash; jejak audit (dan revisi Envers, karena kelas
	 * ini {@code @Audited}) lalu mencatat pengguna yang kebetulan sedang membuka layar.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}; pemanggilan
	 * manual akan ditimpa pada flush berikutnya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP}. Nilai awal objek baru adalah waktu instansiasi
	 * (jam aplikasi), bukan {@code null}.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris: {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca field {@code id} dan {@code nama} secara LANGSUNG (bukan lewat getter), sehingga
	 * pemangkasan spasi {@link #getNama()} tidak berlaku di sini dan hasilnya dapat berbentuk
	 * {@code "null-null"} untuk objek yang belum diisi. Dipakai antara lain oleh keluaran debug
	 * {@code JenisCatatanSiswaAction} dan oleh {@code ManajemenProperty} saat men-{@code toString()}
	 * koleksi.</p>
	 *
	 * @return gabungan id dan nama dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode jenis catatan. Dipetakan ke kolom {@code kode}, namun TIDAK PERNAH diisi UI mana pun. */
	private String kode;

	/** Nama jenis catatan; satu-satunya atribut wajib yang benar-benar diisi pengguna. */
	private String nama;
	/** Sekolah pemilik baris (cakupan multi-tenant); wajib diisi layar master. */
	private Sekolah sekolah;
	/** Yayasan pemilik baris; selalu diturunkan ulang dari {@link #getSekolah()} saat dibaca. */
	private Yayasan yayasan;
	/** Keterangan bebas; boleh {@code null} &mdash; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Bendera aktif; TIDAK PERNAH diisi layar master sehingga umumnya {@code null} di basis data. */
	private Boolean aktif;

	/**
	 * Cache statis berumur JVM: pemetaan {@code id jenis catatan} &rarr; himpunan kategori field
	 * kustum yang tercentang untuknya.
	 *
	 * <p><b>Ini kuirk terbesar berkas ini.</b> Isi map dipakai oleh
	 * {@link #getKelompokParameterTambahanCatatanSiswas()} untuk <b>MENIMPA</b> koleksi hasil muat
	 * Hibernate, dan diisi oleh {@link #setKelompokParameterTambahanCatatanSiswas(Set)} setiap kali
	 * layar master menyimpan. Tujuan aslinya: menghindari
	 * {@code LazyInitializationException} pada objek detached &mdash; itu pula sebabnya
	 * {@code ais.common.ManajemenProperty} memasang <i>special case</i> khusus yang membaca
	 * {@code mapParameters} alih-alih properti terpetakan saat mengekspor entity ini ke JSON
	 * (bila cache kosong, hasil ekspor menjadi string kosong, bukan daftar sebenarnya).</p>
	 *
	 * <p>Siklus hidupnya:</p>
	 * <ol>
	 *   <li>Saat startup, {@code InitDataHelper.handleJenisCatatanSiswa()} memuat SELURUH baris
	 *       jenis catatan dan menyalin kategori tiap baris ke dalam <b>{@code TreeSet}</b> baru,
	 *       lalu menaruhnya di sini.</li>
	 *   <li>Selanjutnya, setiap pembacaan getter mengembalikan salinan cache tersebut, bukan isi
	 *       basis data.</li>
	 *   <li>Tidak ada satu pun pemanggil {@code remove(...)}/{@code clear()} di seluruh basis kode
	 *       &mdash; entri hidup sampai JVM mati, termasuk untuk baris yang sudah dihapus.</li>
	 * </ol>
	 *
	 * <p><b>Akibat 1 &mdash; penciutan senyap {@code TreeSet} (potensi KEHILANGAN DATA).</b>
	 * {@link KelompokParameterTambahanCatatanSiswa#compareTo(GeneralValueObject)} hanya
	 * membandingkan {@code nomorUrut} dan getter-nya mengembalikan {@code 1} untuk nilai
	 * {@code null}. Maka semua kategori yang bernomor urut sama (termasuk semua kategori yang
	 * nomor urutnya belum pernah diisi) dianggap "duplikat" dan hanya SATU yang bertahan di
	 * {@code TreeSet} yang dibangun saat startup. Karena layar master membaca daftar tercentang
	 * lewat getter yang sama, kotak centang yang lenyap itu ikut tersimpan kembali saat admin
	 * menekan Simpan &mdash; barisan pada tabel join ikut terhapus permanen. Gejalanya khas:
	 * "kemarin semua kategori tampil, sesudah restart tinggal satu".</p>
	 *
	 * <p><b>Akibat 2 &mdash; keterbacaan lintas sesi dan lintas tenant.</b> Map ini {@code public
	 * static} tanpa sinkronisasi apa pun ({@link HashMap} biasa) sehingga: (a) perubahan centang
	 * oleh satu admin langsung terlihat oleh semua sesi lain tanpa menunggu commit; (b) bila
	 * penyimpanan gagal atau di-rollback, cache tetap memegang nilai baru sampai restart;
	 * (c) penulisan bersamaan dari dua thread berpotensi merusak struktur internal {@code HashMap}.
	 * Kuncinya hanya {@code id}, jadi tidak ada pemisahan per sekolah/yayasan &mdash; namun karena
	 * id bersifat global, tidak terjadi tabrakan antar-tenant.</p>
	 *
	 * <p>Pola identik dipakai seluruh keluarga {@code Jenis*}; lihat
	 * {@link ais.database.model.JenisPengaduan#mapParameters} yang sudah didokumentasikan panjang
	 * lebar.</p>
	 */
	public static Map<Long, Set<KelompokParameterTambahanCatatanSiswa>> mapParameters = new HashMap<Long, Set<KelompokParameterTambahanCatatanSiswa>>();

	/**
	 * Himpunan kategori field kustom yang tercentang untuk jenis catatan ini.
	 *
	 * <p>Nilai awalnya {@code TreeSet} (lihat peringatan penciutan pada {@link #mapParameters});
	 * pada objek terkelola Hibernate menggantinya dengan koleksi persistennya sendiri.</p>
	 */
	private Set<KelompokParameterTambahanCatatanSiswa> kelompokParameterTambahanCatatanSiswas = new TreeSet<KelompokParameterTambahanCatatanSiswa>();

	/**
	 * Mengembalikan daftar kategori field kustom yang <b>dicentang</b> untuk jenis catatan ini.
	 *
	 * <p>Relasi {@code @ManyToMany} ke {@link KelompokParameterTambahanCatatanSiswa} lewat tabel
	 * join {@code sekolah.jenis_catatan_siswa_has_parameter} (kolom {@code jenis_catatan_siswa}
	 * &rarr; kolom {@code parameter}; ingat kolom {@code parameter} menyimpan id KELOMPOK, bukan
	 * id {@link ais.database.model.ParameterTambahan}). {@code cascade = MERGE} saja: menyimpan
	 * jenis catatan tidak akan membuat/menghapus baris kategori, hanya menyinkronkan tabel join.
	 * {@code @OrderBy("nomorUrut asc, nama asc")} mengatur urutan pada level SQL &mdash; kunci
	 * kedua {@code nama} praktis tidak berpengaruh bila hasilnya kemudian disalin ke {@code
	 * TreeSet} yang hanya membandingkan {@code nomorUrut}.</p>
	 *
	 * <p><b>Efek samping &mdash; getter yang MENULIS dan mengabaikan basis data.</b> Bila
	 * {@code id} sudah terisi DAN {@link #mapParameters} memuat entri untuk id tersebut, isi cache
	 * statis itu <b>menimpa</b> field instance ini; nilai yang baru saja dimuat Hibernate dari
	 * tabel join dibuang. Setelah startup, cache selalu terisi untuk seluruh baris, sehingga jalur
	 * cache-lah yang berlaku pada praktiknya &mdash; dengan segala akibat yang diuraikan pada
	 * {@link #mapParameters}, termasuk risiko kehilangan kategori.</p>
	 *
	 * <p>Pemanggil terverifikasi: {@code JenisCatatanSiswaAction.initKelompokParameterTambahanCatatanSiswa()}
	 * (menyalin daftar tercentang ke {@code HashSet} untuk kotak centang),
	 * {@code LaporanCatatanSiswa}/{@code LaporanRaporSiswa} (merender heading kelompok pada
	 * cetakan), {@code CatatanApi.parameter()} (menyusun formulir dinamis untuk aplikasi mobile),
	 * dan {@code InitDataHelper.handleJenisCatatanSiswa()} saat mengisi cache.</p>
	 *
	 * @return himpunan kategori tercentang; tidak pernah {@code null}, tetapi bisa kosong dan bisa
	 *         berasal dari cache statis alih-alih dari basis data
	 */
	@ManyToMany(targetEntity = KelompokParameterTambahanCatatanSiswa.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nomorUrut asc, nama asc")
	@JoinTable(name = "jenis_catatan_siswa_has_parameter", schema = "sekolah", joinColumns = @JoinColumn(name = "jenis_catatan_siswa"), inverseJoinColumns = @JoinColumn(name = "parameter"))
	public Set<KelompokParameterTambahanCatatanSiswa> getKelompokParameterTambahanCatatanSiswas() {
		if (id != null) {
			Set<KelompokParameterTambahanCatatanSiswa> temp = mapParameters.get(id);
			if (temp != null) {
				kelompokParameterTambahanCatatanSiswas = temp;
			}
		}
		return kelompokParameterTambahanCatatanSiswas;
	}

	/**
	 * Mengganti seluruh daftar kategori tercentang untuk jenis catatan ini.
	 *
	 * <p><b>Efek samping &mdash; menulis ke cache statis {@link #mapParameters}</b> bila
	 * {@link #getId()} sudah terisi. Penulisan terjadi SEKETIKA, sebelum transaksi di-commit,
	 * sehingga seluruh sesi lain di JVM yang sama langsung melihat daftar baru; sebaliknya, bila
	 * penyimpanan gagal atau dibatalkan, cache tetap memegang nilai baru sampai aplikasi
	 * di-restart. Untuk baris BARU ({@code id} masih {@code null}) cache tidak diisi &mdash; entri
	 * untuk baris itu baru muncul pada startup berikutnya.</p>
	 *
	 * <p>Referensi himpunan disimpan apa adanya (tidak disalin), jadi perubahan lanjutan pada
	 * himpunan milik pemanggil ikut terlihat lewat cache. {@code JenisCatatanSiswaAction.onSave()}
	 * memanfaatkan hal ini: ia mengirim {@code HashSet} hasil kotak centang, sehingga tepat setelah
	 * penyimpanan daftar tidak lagi mengalami penciutan {@code TreeSet} &mdash; penciutan baru
	 * muncul lagi setelah restart berikutnya membangun ulang cache sebagai {@code TreeSet}.</p>
	 *
	 * @param kelompokParameterTambahanCatatanSiswas himpunan kategori yang dicentang; sebaiknya
	 *        tidak {@code null} karena nilai tersebut akan ikut tersimpan ke cache dan dikembalikan
	 *        apa adanya oleh getter
	 */
	public void setKelompokParameterTambahanCatatanSiswas(
			Set<KelompokParameterTambahanCatatanSiswa> kelompokParameterTambahanCatatanSiswas) {
		this.kelompokParameterTambahanCatatanSiswas = kelompokParameterTambahanCatatanSiswas;
		if (id != null) {
			mapParameters.put(id, kelompokParameterTambahanCatatanSiswas);
		}
	}

	/** Konstruktor kosong wajib Hibernate; seluruh atribut diisi lewat setter. */
	public JenisCatatanSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}), kolom {@code id} bersifat
	 * {@code insertable = false}, unik, dan tidak boleh {@code null}. Nilai berurutan &mdash;
	 * perhatikan implikasinya pada enumerasi id di jalur REST (lihat catatan keamanan pada Javadoc
	 * class).</p>
	 *
	 * @return id baris, atau {@code null} untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama.
	 *
	 * <p>Dipakai Hibernate setelah {@code INSERT}. Mengubahnya secara manual juga mengubah kunci
	 * yang dipakai {@link #mapParameters}, sehingga entri cache lama menjadi yatim.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode jenis catatan dalam bentuk sudah dipangkas spasinya.
	 *
	 * <p><b>Menormalkan {@code null} menjadi {@code ""}</b> &mdash; kebalikan dari
	 * {@link #getKeterangan()} pada kelas yang sama.</p>
	 *
	 * <p><b>Kolom praktis mati.</b> Tidak ada satu pun jalur penulis yang memanggil
	 * {@link #setKode(String)}: layar master {@code JenisCatatanSiswaAction} tidak menyediakan
	 * isian kode, dan rute REST pun tidak mengisinya. Namun {@code CatatanSiswaAction} dan
	 * {@code LaporanCatatanSiswa} menyusun label combobox dari pasangan kolom
	 * <code>{"nama", "kode"}</code>, sehingga bagian kode pada label selalu kosong.</p>
	 *
	 * @return kode terpangkas, atau {@code ""} bila belum diisi (tidak pernah {@code null})
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode jenis catatan. Disimpan apa adanya, tanpa pemangkasan.
	 *
	 * @param kode kode jenis catatan; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis catatan dalam bentuk sudah dipangkas spasinya.
	 *
	 * <p>Dipetakan ke kolom {@code nama} bertipe {@code text} dan {@code nullable = false}.
	 * Berbeda dari {@link #getKode()}, nilai {@code null} <b>tidak</b> dinormalkan menjadi
	 * {@code ""} &mdash; hanya dilewatkan apa adanya untuk menghindari {@code NullPointerException}
	 * pada pemangkasan.</p>
	 *
	 * <p>Method ini juga menjadi kunci pengurutan efektif kelas ini, karena
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} memanggilnya secara virtual
	 * sementara kunci {@code nomorUrut}/{@code nim} milik induk selalu {@code null} di sini.
	 * Perbandingannya peka besar-kecil huruf, sehingga nama berawalan huruf kapital selalu
	 * mendahului yang berhuruf kecil.</p>
	 *
	 * @return nama terpangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis catatan. Disimpan apa adanya, tanpa pemangkasan.
	 *
	 * <p>Validasi "harus diisi" berada di lapis UI ({@code JenisCatatanSiswaAction.onSave()}),
	 * bukan di sini.</p>
	 *
	 * @param nama nama jenis catatan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas jenis catatan.
	 *
	 * <p><b>MEMBALIK kontrak base class.</b> {@code GeneralValueObject.getKeterangan()} menormalkan
	 * {@code null} menjadi {@code ""} dan menjanjikan hasil non-null; override ini mengembalikan
	 * field mentah, sehingga <b>bisa {@code null}</b> dan pemanggil wajib memasang penjaga sendiri.
	 * Nilainya juga tidak dipangkas spasinya, berbeda dari {@link #getNama()}/{@link #getKode()}
	 * pada kelas yang sama.</p>
	 *
	 * <p>Dampak lanjutan pada pengurutan: cabang {@code keterangan} di
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} yang pada base class SELALU
	 * terpakai, di sini bisa terlewat sepenuhnya bila salah satu objek keterangannya {@code null}
	 * &mdash; walau dalam praktiknya cabang {@code nama} sudah lebih dulu menang.</p>
	 *
	 * @return keterangan apa adanya; dapat {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas jenis catatan.
	 *
	 * @param keterangan keterangan; boleh {@code null} (kolom {@code nullable})
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif jenis catatan, dengan {@code null} dianggap <b>aktif</b>.
	 *
	 * <p><b>Bug nyata &mdash; "jenis baru tidak muncul di formulir".</b> Layar master
	 * {@code JenisCatatanSiswaAction} tidak menyediakan kotak centang aktif dan tidak pernah
	 * memanggil {@link #setAktif(Boolean)}, sehingga baris baru masuk ke basis data dengan
	 * {@code aktif = NULL}. Grid master tetap menampilkannya sebagai aktif karena membaca lewat
	 * getter ini, tetapi seluruh pemakai hilir menyaring di level SQL dengan
	 * {@code Restrictions.eq("aktif", true)} &mdash; dan {@code NULL = true} bernilai tidak-benar
	 * di SQL. Akibatnya jenis catatan yang baru dibuat TIDAK PERNAH muncul pada combobox
	 * {@code CatatanSiswaAction} maupun filter {@code LaporanCatatanSiswa}, meski di layar master
	 * terlihat aktif. Satu-satunya jalur yang selamat adalah {@code CatatanApi.jenisSiswa()} yang
	 * sengaja memakai {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}; jadi aplikasi
	 * mobile melihat jenis catatan yang tidak terlihat di aplikasi web.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; {@code false} bila dinonaktifkan
	 *         secara eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif jenis catatan.
	 *
	 * <p>Tidak ada pemanggil di dalam basis kode &mdash; nilainya hanya bisa berubah lewat
	 * pengeditan data langsung. Lihat {@link #getAktif()}.</p>
	 *
	 * @param aktif {@code true}/{@code false}; {@code null} akan dibaca sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
	
	
	/**
	 * Mengembalikan sekolah pemilik jenis catatan ini (cakupan multi-tenant).
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke kolom {@code sekolah_id}, {@code cascade = {PERSIST,
	 * MERGE}}.</p>
	 *
	 * <p><b>Efek samping &mdash; getter yang MENULIS.</b> Field {@code sekolah} ditimpa dengan
	 * hasil {@link GeneralValueObject#check(Object)}, yaitu proxy lazy yang sudah diresolusi ke
	 * instance nyata (bisa objek yang berbeda dari sebelumnya). Karena penulisan terjadi saat baris
	 * sekadar DIBACA, Hibernate dapat menganggap entity kotor dan memicu {@code UPDATE} beserta
	 * revisi Envers yang tidak diminta pengguna.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila belum ditentukan
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
	 * <p><b>Kuirk:</b> sekolah yang {@code null} <i>atau</i> yang belum punya {@code id} (mis.
	 * item pilihan "Semua" pada combobox) disimpan sebagai {@code null}. Objek transien tanpa id
	 * sengaja dibuang agar {@code cascade = PERSIST} tidak mencoba menyimpan sekolah baru secara
	 * tidak sengaja.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id berarti tanpa cakupan
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik jenis catatan ini.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke kolom {@code yayasan_id}.</p>
	 *
	 * <p><b>Efek samping &mdash; getter DESTRUKTIF.</b> Method ini tidak sekadar membaca: bila
	 * {@link #getSekolah()} tidak {@code null}, field {@code yayasan} DITIMPA dengan
	 * {@code getSekolah().getYayasan()}, lalu hasilnya masih dilewatkan
	 * {@link GeneralValueObject#check(Object)}. Jadi nilai kolom {@code yayasan_id} yang tersimpan
	 * di basis data <b>diabaikan</b> setiap kali baris dibaca, dan nilai turunan itulah yang akan
	 * ikut tersimpan pada penyimpanan berikutnya &mdash; termasuk memicu {@code UPDATE}/revisi
	 * Envers pada operasi yang seharusnya baca-saja. Karena
	 * {@code JenisCatatanSiswaAction.onSave()} juga mewajibkan yayasan dan sekolah diisi bersamaan,
	 * praktisnya {@code yayasan} selalu konsisten dengan induk {@code sekolah}; ketidakcocokan yang
	 * sengaja dibuat lewat pengeditan data langsung akan "diperbaiki" diam-diam.</p>
	 *
	 * <p>Perhatikan pula method ini memanggil {@link #getSekolah()}, yang sendirinya juga menulis
	 * field &mdash; satu pembacaan {@code getYayasan()} dapat mengotori DUA field sekaligus.</p>
	 *
	 * @return yayasan pemilik (diturunkan dari sekolah bila ada), atau {@code null}
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
	 * <p><b>Kuirk:</b> sama seperti {@link #setSekolah(Sekolah)}, yayasan {@code null} atau tanpa
	 * {@code id} disimpan sebagai {@code null}. Nilai apa pun yang disetel di sini akan ditimpa
	 * kembali oleh {@link #getYayasan()} bila {@code sekolah} terisi.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id berarti tanpa cakupan
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

}
