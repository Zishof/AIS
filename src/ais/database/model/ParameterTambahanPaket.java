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
 * Entity <b>PENGHUBUNG</b> rantai "Form Tambahan" PMB/Calon Mahasiswa, dipetakan ke tabel
 * {@code public.parameter_tambahan_paket}.
 *
 * <p>Satu baris di tabel ini <b>bukan</b> definisi field kustom, dan <b>bukan pula</b> judul
 * seksinya, melainkan <i>keputusan penempatan</i>: "field kustom X dipasang di seksi Y, untuk paket
 * pendaftaran Z, dan berlaku pada gelombang-gelombang tertentu". Rantai lengkapnya:</p>
 *
 * <pre>
 *   ParameterTambahan                          (definisi 1 field kustom: label, tipe, pilihan, aktif)
 *        &uarr; FK parameter_tambahan (NOT NULL)
 *   ParameterTambahanPaket                     &lt;&mdash; KELAS INI (penempatan/penugasan)
 *        &darr; FK paket (nullable)                         &rarr; {@link Paket}
 *        &darr; FK kelompok_parameter_tambahan_calon_mahasiswa (nullable)
 *   KelompokParameterTambahanCalonMahasiswa    (judul seksi pada formulir)
 * </pre>
 *
 * <p>Dengan kata lain, kelas ini memegang <b>kedua</b> ujung relasi: {@code @ManyToOne} ke
 * {@link ParameterTambahan} (apa yang ditanyakan) dan {@code @ManyToOne} ke
 * {@link KelompokParameterTambahanCalonMahasiswa} (di seksi mana ditanyakan). Kolom {@code paket}
 * yang boleh {@code NULL} berarti "berlaku untuk semua paket"; seluruh pembaca runtime
 * mengekspresikannya sebagai {@code Restrictions.or(isNull("paket"), eq("paket", pp))}.</p>
 *
 * <h3>Kaitan dengan gelombang pendaftaran &mdash; BUKAN {@code @ManyToMany}</h3>
 * <p>Kelas ini <b>tidak punya relasi objek apa pun</b> ke {@link GelombangPendaftaran}: nol
 * {@code @ManyToMany}, nol {@code @OneToMany}, nol koleksi. Cakupan gelombang disimpan
 * <i>terdenormalisasi</i> pada dua properti skalar:</p>
 * <ul>
 *   <li>{@link #getTampilDiSemuaGelombang()} &mdash; sakelar "berlaku untuk semua gelombang";</li>
 *   <li>{@link #getGelombangs()} &mdash; kolom {@code text} berisi daftar id gelombang
 *       terserialisasi dengan format berpembatas <code>";id;"</code> yang ditempel
 *       berurutan, contoh nyata untuk gelombang 7 dan 12: <code>;7;;12;</code>.</li>
 * </ul>
 * <p>Relasi {@code @ManyToMany} yang sesungguhnya ada di tempat lain dan memiliki arti berbeda:
 * {@code GelombangPendaftaran#getKelompokParameterTambahanCalonMahasiswas()} (tabel penghubung
 * {@code gelombang_kelompok_parameter}) memilih <b>seksi</b>, bukan field. Bila koleksi itu tidak
 * kosong, {@code ais.action.master.pmb.ParameterTambahanListener} memakai HANYA daftar seksi
 * tersebut dan mengabaikan flag {@code tampilDiForm*}; filter per-gelombang milik kelas ini
 * ({@code tampilDiSemuaGelombang}/{@code gelombangs}) pun ikut <b>tidak diterapkan</b> pada jalur
 * itu. Jadi ada dua jalur pemilihan yang saling eksklusif, dan kolom {@code gelombangs} baris ini
 * hanya berpengaruh pada jalur kedua (gelombang tidak memilih seksi secara eksplisit).</p>
 *
 * <h3>Di mana nilai isian calon mahasiswa sebenarnya disimpan (non-obvious)</h3>
 * <p>Entity ini murni <b>konfigurasi</b>; ia tidak pernah menyimpan jawaban pendaftar. Jawaban
 * calon mahasiswa disimpan pada {@link BiodataCalonMahasiswa} (BUKAN {@code BiodataMahasiswa}
 * seperti pada keluarga Alumni/Mahasiswa), dalam <b>dua kolom teks</b> yang ditulis sekaligus oleh
 * {@code BiodataCalonMahasiswa#populateParameterTambahan(java.util.List)}:</p>
 * <ul>
 *   <li>{@code parameterTambahanInds} &mdash; versi ber-ID, satu jawaban per baris dipisah
 *       {@code \n}, ruas dipisah {@code <=>} dengan urutan
 *       <code>idKelompok-&gt;idParameter &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt;
 *       keterangan</code>. Inilah versi yang dibaca ulang saat form di-prefill.</li>
 *   <li>{@code parameterTambahan} &mdash; versi berlabel (7 ruas: nama kelompok/label, nilai, url,
 *       nomor urut, id parameter, id kelompok, keterangan), dipakai untuk tampilan/ekspor.</li>
 * </ul>
 * <p>Kunci gabungan <code>idKelompok + "-&gt;" + idParameter</code> juga dipakai sebagai
 * {@code jenis} pada {@code ais.database.model.file.LampiranLain#ambil(Long, String)} untuk
 * mengambil berkas lampiran per jawaban. Perhatikan bahwa kunci itu <b>tidak memuat id baris kelas
 * ini</b>: menghapus lalu membuat ulang baris penempatan (mis. lewat tombol Hapus + Tambah) tidak
 * merusak jawaban yang sudah tersimpan, selama pasangan kelompok/parameter tetap sama.</p>
 *
 * <h3>Pemakai runtime</h3>
 * <ul>
 *   <li>{@code ais.action.master.pmb.ParameterTambahanListener} &mdash; pembaca utama: merender
 *       form tambahan pada pendaftaran publik maupun form biodata setelah calon login.</li>
 *   <li>{@code ais.action.master.ParameterTambahanPaketAction} &mdash; layar admin CRUD-nya.</li>
 *   <li>{@code ais.action.maintenance.TbmuserAction},
 *       {@code ais.action.master.KelompokCalonMahasiswaAction},
 *       {@code ais.action.master.TampilanPengumumanAkademisAction},
 *       {@code ais.action.master.dashboard.helper.DashboardRekapParameterTambahanMahasiswaBaru}
 *       &mdash; memakai tabel ini sebagai <i>katalog kolom dinamis</i> untuk rekap/ekspor
 *       Excel.</li>
 * </ul>
 *
 * <h3>Migrasi SQL mentah setiap layar dibuka (TERVERIFIKASI)</h3>
 * <p>Tabel <b>ini</b>-lah target SQL mentah yang dijalankan
 * {@code ParameterTambahanPaketAction#doAfterCompose(org.zkoss.zk.ui.Component)}. Setelah
 * memanggil {@code KelompokParameterTambahanCalonMahasiswa#checkCreateDefault()}, method itu
 * mengeksekusi &mdash; <b>setiap kali layar dibuka</b>, tanpa syarat dan tanpa transaksi
 * eksplisit:</p>
 * <pre>
 *   update parameter_tambahan_paket
 *      set kelompok_parameter_tambahan_calon_mahasiswa = &lt;id kelompok default&gt;
 *    where kelompok_parameter_tambahan_calon_mahasiswa is null;
 * </pre>
 * <p>Konsekuensi yang perlu diketahui: (a) baris yatim &mdash; termasuk hasil impor Excel atau
 * penyisipan langsung ke DB &mdash; diadopsi diam-diam ke kelompok default; (b) karena berupa
 * {@code createSQLQuery(...).executeUpdate()}, perubahan itu <b>melewati Hibernate Envers</b>
 * sehingga tidak muncul di riwayat revisi meski kelas ini {@code @Audited}, dan juga melewati
 * callback {@link #onUpdate()} sehingga {@code oleh}/{@code tanggal_dirubah} tidak diperbarui;
 * (c) cache level-1/level-2 tidak tahu baris sudah berubah dalam session yang sama.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, callback
 *       {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b> &mdash; {@link #getId()}/{@link #setId(Long)}, konstruktor
 *       {@link #ParameterTambahanPaket()}.</li>
 *   <li><b>Relasi penempatan</b> &mdash; {@link #getParameterTambahan()},
 *       {@link #getKelompokParameterTambahanCalonMahasiswa()}, {@link #getPaket()}.</li>
 *   <li><b>Cakupan gelombang</b> &mdash; {@link #getTampilDiSemuaGelombang()},
 *       {@link #getGelombangs()}.</li>
 *   <li><b>Urutan tampil</b> &mdash; {@link #getNomorUrut()}/{@link #setNomorUrut(Integer)}
 *       (cermin dari {@link ParameterTambahan#getNomorUrut()}, lihat catatan di getter-nya).</li>
 * </ul>
 * <p>Kelas ini <b>tidak</b> punya properti {@code nama} maupun {@code keterangan}, tidak
 * meng-override {@code toString()}, dan tidak mengimplementasikan {@code compareTo(...)} sendiri
 * &mdash; pengurutan pada layar/form dilakukan atas {@link ParameterTambahan} atau
 * {@link KelompokParameterTambahanCalonMahasiswa}, bukan atas baris kelas ini.</p>
 *
 * <h3>Catatan pemetaan yang mudah salah paham</h3>
 * <ul>
 *   <li><b>Field induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} BUKAN
 *       {@code @Entity} maupun {@code @MappedSuperclass} &mdash; hanya POJO abstrak biasa &mdash;
 *       sehingga Hibernate tidak memetakan properti milik induk. Deklarasi ulang {@code id},
 *       {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini <b>bukan bug</b>,
 *       melainkan keharusan teknis agar kolom-kolom itu benar-benar terpetakan. Efek sampingnya:
 *       field induk yang senama ter-<i>shadow</i> dan selamanya bernilai {@code null}; semua kode
 *       wajib lewat getter, bukan membaca field induk langsung.</li>
 *   <li><b>Property access.</b> Anotasi JPA dipasang pada getter ({@code @Id} di
 *       {@link #getId()}), jadi Hibernate memakai <i>property access</i>: setiap getter dipanggil
 *       Hibernate saat memuat, dirty-check, dan flush. Getter yang mengubah field karena itu ikut
 *       mengubah baris di database &mdash; lihat {@link #getNomorUrut()},
 *       {@link #getTampilDiSemuaGelombang()}, dan {@link #getGelombangs()}.</li>
 *   <li><b>Nama kolom implisit.</b> Hanya {@code id}, ketiga kolom FK, dan {@code gelombangs} yang
 *       punya anotasi kolom eksplisit. Properti {@code tampilDiSemuaGelombang} dan
 *       {@code nomorUrut} memakai penamaan default Hibernate (nama properti apa adanya, yang di
 *       PostgreSQL menjadi identifier huruf kecil: {@code tampildisemuagelombang},
 *       {@code nomorurut}).</li>
 *   <li><b>{@code dynamicInsert}/{@code dynamicUpdate} aktif</b>
 *       ({@code @org.hibernate.annotations.Entity}), sehingga hanya kolom yang benar-benar berubah
 *       yang ikut ditulis &mdash; inilah yang membuat write-back diam-diam dari getter di atas
 *       benar-benar sampai ke tabel.</li>
 *   <li><b>{@code @Audited}</b>: setiap perubahan lewat session Hibernate direkam Envers ke tabel
 *       revisi &mdash; termasuk write-back tak sengaja dari getter, yang melahirkan <i>revisi
 *       audit palsu</i> seolah dilakukan pengguna. Sebaliknya, migrasi SQL mentah di atas justru
 *       <i>tidak</i> tercatat sama sekali.</li>
 *   <li><b>Komentar generator menyesatkan.</b> Javadoc bawaan pada kelas ini semula berbunyi
 *       "Bank generated by hbm2java" &mdash; sisa salin-tempel {@code hbm2java} yang tidak ada
 *       kaitannya dengan bank; sudah diganti oleh dokumentasi ini.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see ParameterTambahan
 * @see KelompokParameterTambahanCalonMahasiswa
 * @see Paket
 * @see GelombangPendaftaran
 * @see BiodataCalonMahasiswa
 * @see ais.action.master.ParameterTambahanPaketAction
 * @see ais.action.master.pmb.ParameterTambahanListener
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "parameter_tambahan_paket")
public class ParameterTambahanPaket extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sama dengan ratusan entity lain di paket ini karena semua
	 * lahir dari templat {@code hbm2java} yang sama; tidak berbahaya (kelasnya berbeda), tapi
	 * menjelaskan kemiripan struktur antar berkas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris, kolom {@code id} (identity PostgreSQL). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pengubah terakhir; diisi otomatis oleh {@link #onUpdate()}. */
	private String oleh;
	/** Id/kode pengguna pengubah terakhir; diisi otomatis oleh {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Id/kode pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} maupun string kosong/spasi <b>diabaikan diam-diam</b>
	 * &mdash; method langsung {@code return} tanpa mengubah apa pun. Jadi jejak audit lama tidak
	 * pernah bisa dihapus lewat setter ini (perilaku sengaja, seragam di seluruh keluarga
	 * {@link GeneralValueObject}).</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong
	 * diabaikan diam-diam sehingga jejak audit lama tidak dapat ditimpa dengan nilai kosong.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir mengubah baris ini.
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
	 * <p><b>Efek samping:</b> mengubah state objek di tengah siklus flush. Jangan dipanggil manual
	 * dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati
	 * callback ini. Baris yang dibuat massal lewat {@code ParameterTambahanPaketAction#onAdd(...)}
	 * karena itu lahir tanpa jejak {@code oleh}/{@code olehId} sama sekali. Migrasi SQL mentah yang
	 * dijelaskan pada Javadoc kelas juga melewati callback ini sepenuhnya.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul. Karena diinisialisasi di
	 * deklarasi, setiap instance baru &mdash; termasuk yang dibuat
	 * {@link #ParameterTambahanPaket()} &mdash; sudah membawa timestamp saat objek dibentuk, bukan
	 * saat disimpan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Umumnya dipanggil oleh {@link #onUpdate()}, bukan oleh
	 * kode aplikasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada instance baru karena field
	 *         diinisialisasi saat deklarasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Seksi/judul kelompok tempat field kustom ini dipasang. Lihat {@link #getKelompokParameterTambahanCalonMahasiswa()}. */
	private KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa;
	/** Paket pendaftaran yang dicakup; {@code null} berarti berlaku untuk SEMUA paket. Lihat {@link #getPaket()}. */
	private Paket paket;
	/** Definisi field kustom yang ditempatkan (wajib, kolom FK {@code NOT NULL}). Lihat {@link #getParameterTambahan()}. */
	private ParameterTambahan parameterTambahan;
	/** Sakelar "berlaku untuk semua gelombang". Lihat {@link #getTampilDiSemuaGelombang()}. */
	private Boolean tampilDiSemuaGelombang;
	/** Daftar id gelombang terserialisasi berformat <code>;id;;id;</code>. Lihat {@link #getGelombangs()}. */
	private String gelombangs;

	/** Cermin nomor urut milik {@link ParameterTambahan}; lihat catatan pada {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/**
	 * Nomor urut tampil field kustom ini.
	 *
	 * <p><b>Bukan getter polos &mdash; MENULIS ke state objek.</b> Method ini memanggil
	 * {@link #getParameterTambahan()} (yang juga meresolusi proxy lazy lewat {@code check(...)}),
	 * lalu bila parameter berhasil dimuat, <b>menimpa</b> field {@code nomorUrut} milik baris ini
	 * dengan {@link ParameterTambahan#getNomorUrut()}. Karena kelas ini memakai <i>property
	 * access</i> + {@code dynamicUpdate = true}, penimpaan itu terdeteksi dirty-check dan
	 * benar-benar tertulis ke kolom {@code nomorurut} pada flush berikutnya &mdash; lengkap dengan
	 * revisi Envers baru yang seolah dibuat pengguna, padahal baris hanya dibaca. Ini varian dari
	 * pola "getter destruktif via property access" yang tersebar di banyak turunan
	 * {@link GeneralValueObject}.</p>
	 *
	 * <p><b>Kuirk kedua:</b> kolom {@code nomorurut} milik tabel ini praktis <b>yatim</b> &mdash;
	 * tidak ada satu pun pemanggil {@code getNomorUrut()}/{@code setNomorUrut(...)} atas entity ini
	 * di luar kelas ini sendiri, dan tidak ada query yang mengurutkan memakainya. Pengurutan yang
	 * benar-benar dipakai form PMB mengambil {@code nomorUrut} langsung dari
	 * {@link ParameterTambahan} lewat {@code Collections.sort(...)}. Jadi kolom ini murni
	 * write-only: nilainya ditulis diam-diam tapi tidak pernah dibaca kembali.</p>
	 *
	 * <p>Efek samping tambahan: penugasan {@code parameterTambahan = getParameterTambahan()} juga
	 * mengganti isi field relasi dengan hasil resolusi proxy, yang dapat mengubah instance yang
	 * dipegang objek ini.</p>
	 *
	 * @return nomor urut hasil cermin dari {@link ParameterTambahan}, atau {@code 1} bila keduanya
	 *         belum terisi (tidak pernah mengembalikan {@code null})
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil.
	 *
	 * <p>Praktis tidak berguna: nilai apa pun yang disetel akan <b>ditimpa kembali</b> oleh
	 * {@link #getNomorUrut()} pada pembacaan berikutnya selama relasi {@code parameterTambahan}
	 * dapat dimuat. Disediakan agar Hibernate dapat mengisi field saat memuat baris.</p>
	 *
	 * @param nomorUrut nomor urut tampil
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate/JPA, sekaligus dipakai
	 * {@code ParameterTambahanPaketAction#onAdd(org.zkoss.zk.ui.event.Event)} saat membuat
	 * penempatan baru secara massal.
	 *
	 * <p>Instance baru sudah membawa {@code tanggal_dirubah} terisi (lihat {@link #onUpdate()}),
	 * tetapi seluruh relasi masih {@code null} &mdash; termasuk {@code parameterTambahan} yang
	 * kolomnya {@code NOT NULL}, sehingga penyimpanan tanpa mengisi relasi itu akan gagal di
	 * tingkat database.</p>
	 */
	public ParameterTambahanPaket() {
	}

	/**
	 * Kunci utama baris.
	 *
	 * <p>Kolom {@code id} dideklarasikan {@code insertable = false}: nilainya sepenuhnya dihasilkan
	 * database (strategi {@code IDENTITY}) dan tidak pernah ikut dalam pernyataan {@code INSERT}.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Dipakai Hibernate saat memuat/menyimpan baris; kode aplikasi umumnya
	 * tidak perlu memanggilnya.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Paket pendaftaran yang dicakup penempatan ini.
	 *
	 * <p><b>{@code null} bermakna "semua paket"</b>, bukan "data belum diisi". Semua pembaca
	 * runtime menyaring dengan {@code Restrictions.or(isNull("paket"), eq("paket", pp))}, sehingga
	 * baris tanpa paket selalu ikut muncul pada paket apa pun.</p>
	 *
	 * <p>Relasi lazy; sebelum dikembalikan, proxy diresolusi lewat
	 * {@code GeneralValueObject#check(Object)} agar aman dipakai di luar session yang memuatnya.
	 * Method ini juga menulis kembali hasil resolusi ke field (mengganti proxy dengan instance
	 * nyata), tetapi tidak mengubah nilai kolom apa pun.</p>
	 *
	 * <p><b>Kuirk layar admin:</b> filter "Paket" pada
	 * {@code ParameterTambahanPaketAction#initCriteria(boolean)} memakai
	 * {@code Restrictions.isNull("paket")} ketika combobox pencarian kosong &mdash; artinya bila
	 * layar dibuka tanpa parameter URL {@code paket}, daftar HANYA menampilkan baris yang berlaku
	 * untuk semua paket, bukan seluruh baris.</p>
	 *
	 * @return paket pendaftaran, atau {@code null} bila penempatan berlaku untuk semua paket
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket", nullable = true)
	public Paket getPaket() {
		paket = check(paket);
		return paket;
	}

	/**
	 * Menyetel paket pendaftaran yang dicakup; {@code null} berarti berlaku untuk semua paket.
	 *
	 * @param paket paket pendaftaran, boleh {@code null}
	 */
	public void setPaket(Paket paket) {
		this.paket = paket;
	}

	/**
	 * Definisi field kustom yang ditempatkan oleh baris ini &mdash; label, tipe input, daftar
	 * pilihan, wajib/tidak, wajib lampiran, dan nomor urut semuanya berasal dari sini.
	 *
	 * <p>Kolom FK {@code parameter_tambahan} bersifat {@code NOT NULL}: setiap baris penempatan
	 * WAJIB menunjuk satu definisi. Seluruh pembaca runtime men-{@code createAlias} relasi ini dan
	 * menyaring {@code parameterTambahan.aktif = true}, sehingga menonaktifkan satu definisi cukup
	 * untuk menyembunyikan field dari semua paket/gelombang sekaligus tanpa menghapus baris
	 * penempatan.</p>
	 *
	 * <p>Relasi lazy; proxy diresolusi lewat {@code GeneralValueObject#check(Object)} dan hasilnya
	 * ditulis kembali ke field sebelum dikembalikan.</p>
	 *
	 * @return definisi field kustom; secara praktis tidak pernah {@code null} untuk baris yang
	 *         sudah tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * Menyetel definisi field kustom yang ditempatkan. Wajib diisi sebelum baris disimpan.
	 *
	 * @param parameterTambahan definisi field kustom
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Menandai apakah penempatan ini berlaku untuk SEMUA gelombang pendaftaran (nilai {@code true},
	 * checkbox "Semua" pada layar admin) atau hanya untuk gelombang yang dipilih pada
	 * {@link #getGelombangs()} (nilai {@code false}).
	 *
	 * <p><b>Non-obvious &mdash; getter menulis nilai default.</b> Bila field masih {@code null},
	 * method ini mengisinya {@code true}. Karena kelas memakai <i>property access</i> +
	 * {@code dynamicUpdate = true}, pengisian itu ikut tersimpan ke kolom pada flush berikutnya
	 * (plus revisi Envers) meski baris hanya dibaca. Dampaknya jinak &mdash; nilai yang ditulis
	 * sama dengan default yang dipakai kode &mdash; tetapi menjelaskan munculnya revisi audit
	 * "tanpa sebab" pada baris lama yang kolomnya masih {@code NULL}.</p>
	 *
	 * <p><b>Bawaan yang perlu diperhatikan:</b> karena defaultnya {@code true}, penempatan baru
	 * hasil {@code onAdd(...)} langsung berlaku untuk seluruh gelombang tanpa admin memilih apa
	 * pun &mdash; kebalikan dari asimetri "aman secara bawaan" milik
	 * {@link KelompokParameterTambahanCalonMahasiswa} yang lahir dengan
	 * {@code tampilDiFormPendaftaran = false}.</p>
	 *
	 * @return {@code true} bila berlaku untuk semua gelombang; tidak pernah {@code null}
	 */
	public Boolean getTampilDiSemuaGelombang() {
		if (tampilDiSemuaGelombang == null) {
			tampilDiSemuaGelombang = true;
		}
		return tampilDiSemuaGelombang;
	}

	/**
	 * Menyetel sakelar "berlaku untuk semua gelombang".
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Semua" pada renderer layar admin, yang
	 * langsung menyusulkan {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan tersimpan
	 * seketika tanpa tombol Simpan.</p>
	 *
	 * @param tampilDiSemuaGelombang {@code true} bila berlaku untuk semua gelombang
	 */
	public void setTampilDiSemuaGelombang(Boolean tampilDiSemuaGelombang) {
		this.tampilDiSemuaGelombang = tampilDiSemuaGelombang;
	}

	/**
	 * Daftar id {@link GelombangPendaftaran} yang dicakup penempatan ini, terserialisasi sebagai
	 * satu kolom {@code text} &mdash; dipakai hanya bila {@link #getTampilDiSemuaGelombang()}
	 * bernilai {@code false}.
	 *
	 * <p><b>Format:</b> setiap id ditulis sebagai <code>";" + id + ";"</code> lalu ditempel
	 * berurutan tanpa pemisah tambahan, jadi gelombang 7 dan 12 menghasilkan
	 * <code>;7;;12;</code>. Dua cara pembacaan yang berbeda dipakai di codebase:</p>
	 * <ul>
	 *   <li>renderer layar admin memakai {@code split(";")} lalu menyaring potongan kosong dan
	 *       non-numerik &mdash; menghasilkan daftar id;</li>
	 *   <li>{@code ParameterTambahanListener} memakai
	 *       {@code Restrictions.ilike("gelombangs", ";" + id + ";", MatchMode.ANYWHERE)} &mdash;
	 *       pembatas titik-koma di kedua sisi itulah yang mencegah id {@code 1} ikut cocok dengan
	 *       {@code ;12;}.</li>
	 * </ul>
	 *
	 * <p><b>Non-obvious &mdash; getter menulis nilai default.</b> Bila field masih {@code null},
	 * method mengisinya string kosong; sama seperti {@link #getTampilDiSemuaGelombang()},
	 * pengisian itu dapat ikut tertulis ke database dan melahirkan revisi Envers pada baris yang
	 * hanya dibaca.</p>
	 *
	 * <p><b>Kuirk layar admin yang perlu diketahui:</b> daftar checkbox gelombang pada layar hanya
	 * memuat gelombang untuk SATU tahun akademik terpilih, sedangkan listener {@code onCheck}
	 * menyusun ulang seluruh isi kolom ini dari checkbox yang sedang terlihat saja. Akibatnya
	 * mencentang/melepas satu gelombang setelah berganti tahun akademik akan menghapus pilihan
	 * gelombang tahun lain yang sudah tersimpan. Dicatat apa adanya sebagai perilaku terkini, di
	 * luar cakupan dokumentasi ini.</p>
	 *
	 * @return daftar id gelombang terserialisasi; tidak pernah {@code null} (minimal string kosong)
	 */
	@Column(columnDefinition = "text")
	public String getGelombangs() {
		if (gelombangs == null) {
			gelombangs = "";
		}
		return gelombangs;
	}

	/**
	 * Menyetel daftar id gelombang terserialisasi.
	 *
	 * <p>Pemanggil wajib menjaga format <code>;id;</code> per entri (lihat
	 * {@link #getGelombangs()}); string dengan format lain tidak akan pernah cocok dengan filter
	 * {@code ilike} pada form PMB, sehingga penempatan menjadi tak pernah tampil tanpa pesan
	 * kesalahan apa pun.</p>
	 *
	 * @param gelombangs daftar id gelombang terserialisasi
	 */
	public void setGelombangs(String gelombangs) {
		this.gelombangs = gelombangs;
	}

	/**
	 * Seksi/judul kelompok tempat field kustom ini muncul pada formulir calon mahasiswa.
	 *
	 * <p>Kolom FK {@code kelompok_parameter_tambahan_calon_mahasiswa} secara pemetaan boleh
	 * {@code NULL}, tetapi dalam praktiknya baris yatim tidak bertahan lama: setiap kali layar
	 * "Parameter Tambahan Paket" dibuka, SQL mentah di
	 * {@code ParameterTambahanPaketAction#doAfterCompose(org.zkoss.zk.ui.Component)} mengadopsi
	 * semua baris ber-FK {@code NULL} ke kelompok default hasil
	 * {@link KelompokParameterTambahanCalonMahasiswa#checkCreateDefault()} &mdash; melewati Envers
	 * dan callback {@link #onUpdate()} (lihat Javadoc kelas).</p>
	 *
	 * <p>Renderer layar admin memanggil {@code getNama()} atas hasil method ini <b>tanpa
	 * pengecekan null</b>, sehingga baris yang benar-benar yatim (mis. baru disisipkan langsung ke
	 * DB dan layar belum pernah dibuka sejak itu) akan memicu {@code NullPointerException} saat
	 * dirender.</p>
	 *
	 * <p>Berbeda dari dua relasi lain, relasi ini <b>tidak</b> {@code FetchType.LAZY} melainkan
	 * memakai default {@code EAGER} milik {@code @ManyToOne}, dengan
	 * {@code @Fetch(FetchMode.SELECT)} sehingga dimuat lewat query terpisah alih-alih ikut dalam
	 * {@code JOIN} query induk. Konsekuensinya: query daftar atas entity ini menghasilkan satu
	 * SELECT tambahan per baris (pola N+1) &mdash; sengaja dipilih agar {@code Criteria} yang
	 * memakai {@code Projections.groupProperty} tetap bekerja.</p>
	 *
	 * @return seksi/judul kelompok; {@code null} hanya untuk baris yatim yang belum termigrasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kelompok_parameter_tambahan_calon_mahasiswa", nullable = true)
	public KelompokParameterTambahanCalonMahasiswa getKelompokParameterTambahanCalonMahasiswa() {
		kelompokParameterTambahanCalonMahasiswa = check(kelompokParameterTambahanCalonMahasiswa);
		return kelompokParameterTambahanCalonMahasiswa;
	}

	/**
	 * Menyetel seksi/judul kelompok tempat field kustom ini muncul.
	 *
	 * <p>Diisi dari combobox "Kelompok" pada dialog Tambah/Ubah layar admin, dan dari kelompok yang
	 * sedang dipilih di toolbar pencarian saat penempatan dibuat massal lewat
	 * {@code ParameterTambahanPaketAction#onAdd(org.zkoss.zk.ui.event.Event)}.</p>
	 *
	 * @param kelompokParameterTambahanCalonMahasiswa seksi/judul kelompok, boleh {@code null}
	 *                                                (baris akan diadopsi ke kelompok default pada
	 *                                                pembukaan layar berikutnya)
	 */
	public void setKelompokParameterTambahanCalonMahasiswa(
			KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa) {
		this.kelompokParameterTambahanCalonMahasiswa = kelompokParameterTambahanCalonMahasiswa;
	}

}
