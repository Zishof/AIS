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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;

/**
 * Entity master <b>kategori/kelompok "form tambahan" (field kustom) untuk data Alumni</b>, dipetakan
 * ke tabel {@code public.kelompok_parameter_tambahan_alumni}.
 *
 * <h3>Peran dalam modul Alumni</h3>
 * <p>AIS mengizinkan setiap perguruan tinggi menambah pertanyaan/isian sendiri pada biodata alumni
 * tanpa mengubah skema. Rantainya tiga lapis:</p>
 * <ol>
 *   <li>{@link ParameterTambahan} &mdash; <b>definisi</b> field kustom generik (label, tipe input,
 *   daftar pilihan, wajib/tidak). Dipakai bersama oleh banyak modul.</li>
 *   <li>{@link ParameterTambahanAlumni} &mdash; tabel penghubung yang <b>mengaitkan</b> sebuah
 *   {@link ParameterTambahan} ke satu baris kelas ini, plus penyaring tahun angkatan.</li>
 *   <li><b>Kelas ini</b> &mdash; <b>kategori/judul kelompok</b> tempat field-field tersebut
 *   ditampilkan berkelompok pada form. Baris kelas ini menjadi heading section di layar isian.</li>
 * </ol>
 * <p>Nilai jawaban alumni <b>tidak</b> disimpan di sini melainkan sebagai string terserialisasi pada
 * {@code BiodataMahasiswa.parameterTambahanIndsAlumni} (lihat
 * {@link ais.action.master.helper.ParameterTambahanAlumniListener}).</p>
 *
 * <h3>Auto-seed lewat {@link #checkCreateDefault()}</h3>
 * <p>Kelas ini memakai pola <b>auto-seed</b> yang sekeluarga dengan {@link Konfigurasi}, tetapi
 * versi entity biasa (satu baris = satu kategori), bukan key-value: bila belum ada satu pun baris
 * bertanda {@link #getDefaultData() defaultData}{@code =true}, method statis
 * {@link #checkCreateDefault()} membuatnya sendiri dan meng-{@code commit} langsung ke DB. Efeknya,
 * sekadar <b>membuka layar</b> "Parameter Tambahan Alumni" sudah menulis baris baru. Lihat
 * dokumentasi method tersebut untuk kuirk transaksi/session-nya.</p>
 *
 * <p><b>Kuirk penting &mdash; ada DUA mekanisme seed yang berbeda dan saling tidak sadar:</b></p>
 * <ul>
 *   <li>{@link #checkCreateDefault()} (dipanggil dari layar <i>Parameter</i> Tambahan Alumni)
 *   membuat baris bernama {@code "Form Tambahan"} dengan {@code defaultData=true}, tanpa
 *   {@code kode};</li>
 *   <li>{@code ais.action.master.KelompokParameterTambahanAlumniAction.doAfterCompose(...)} (layar
 *   <i>Kelompok</i> Parameter Tambahan Alumni) memakai syarat yang <b>berbeda</b> &mdash; jumlah
 *   baris {@code == 0} &mdash; lalu membuat baris {@code kode="001.000"},
 *   {@code nama="Data Alumni"} <b>tanpa</b> menyetel {@code defaultData}.</li>
 * </ul>
 * <p>Akibatnya, bila layar <i>Kelompok</i> dibuka lebih dahulu pada instalasi kosong, baris
 * {@code "Data Alumni"} terbentuk dengan {@code defaultData} {@code null}; syarat
 * {@code checkCreateDefault()} tetap tidak terpenuhi sehingga layar <i>Parameter</i> lalu menambah
 * baris KEDUA {@code "Form Tambahan"}. Instalasi baru bisa berakhir dengan dua kategori bawaan yang
 * berbeda tergantung urutan klik admin. Ini dicatat apa adanya &mdash; tidak diperbaiki di sini.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Atribut kategori:</b> {@link #getNama()}, {@link #getKeterangan()},
 *   {@link #getNomorUrut()}.</li>
 *   <li><b>Penanda perilaku:</b> {@link #getDefaultData()} (penanda baris bawaan),
 *   {@link #getAktif()} (tampil/tidak), {@link #getDigunakanUntukPenggunaAlumni()} (kategori khusus
 *   portal alumni vs. layar internal staf).</li>
 *   <li><b>Utilitas statis:</b> {@link #checkCreateDefault()}.</li>
 *   <li><b>Pengurutan:</b> {@link #compareTo(GeneralValueObject)}.</li>
 * </ul>
 *
 * <h3>Catatan warisan &amp; pemetaan (non-obvious)</h3>
 * <ul>
 *   <li>{@link GeneralValueObject} <b>bukan</b> {@code @MappedSuperclass} dan bukan {@code @Entity},
 *   sehingga Hibernate <b>mengabaikan seluruh property-nya</b>. Itulah sebabnya {@code id},
 *   {@code nama}, {@code keterangan}, {@code nomorUrut}, {@code oleh}, {@code olehId} dideklarasikan
 *   ULANG di kelas ini (field kelas ini <i>membayangi</i> field induk yang bernama sama). Field
 *   induk yang tidak dideklarasikan ulang &mdash; terutama {@code nim} &mdash; <b>tetap ada di
 *   memori tetapi tidak pernah terisi</b>; lihat {@link #compareTo(GeneralValueObject)}.</li>
 *   <li>Anotasi berada di <b>getter</b> ({@code @Id} pada {@link #getId()}), jadi Hibernate memakai
 *   <b>property access</b> untuk SEMUA property. Getter yang memodifikasi field karena itu terlihat
 *   oleh dirty-check; dikombinasikan dengan {@code dynamicUpdate=true}, sekadar membaca baris bisa
 *   memicu {@code UPDATE} + revisi Envers. Lihat {@link #getDefaultData()}, {@link #getAktif()},
 *   {@link #getNomorUrut()}.</li>
 *   <li>{@code defaultData}, {@code aktif}, {@code nomorUrut}, dan
 *   {@code digunakanUntukPenggunaAlumni} <b>tidak</b> punya {@code @Column} eksplisit sehingga nama
 *   kolomnya mengikuti strategi penamaan default Hibernate (nama property apa adanya, dilipat ke
 *   huruf kecil oleh PostgreSQL).</li>
 *   <li>{@code @Audited} &mdash; setiap perubahan direkam Envers ke tabel revisi.</li>
 * </ul>
 *
 * <p><b>Konsumen utama:</b> {@code ais.action.master.KelompokParameterTambahanAlumniAction} (CRUD
 * master), {@code ais.action.master.ParameterTambahanAlumniAction} (pengaitan field ke kategori),
 * {@link ais.action.master.helper.ParameterTambahanAlumniListener} (perender form isian alumni),
 * {@code ais.action.master.dashboard.helper.DashboardRekapParameterTambahanAlumni} (rekap dasbor),
 * {@link BiodataMahasiswa} (pembacaan jawaban), {@link ParameterTambahan} (seed kuesioner Tracer
 * Kemendikbud), dan {@code ais.common.InitData} (pendaftaran kelas master).</p>
 *
 * <p><b>Konkurensi:</b> tidak thread-safe (POJO Hibernate biasa); jangan berbagi instance lintas
 * session/thread.</p>
 *
 * @see ParameterTambahanAlumni
 * @see ParameterTambahan
 * @see KelompokParameterTambahanMahasiswa
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_parameter_tambahan_alumni")

public class KelompokParameterTambahanAlumni extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar instance lama tetap dapat dideserialisasi
	 * (relevan untuk sesi ZK yang di-passivate ke disk).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris kategori. Lihat {@link #getId()}. */
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
	 * ini. Karena itu baris hasil auto-seed {@link #checkCreateDefault()} maupun seed layar
	 * {@code KelompokParameterTambahanAlumniAction} masuk <b>tanpa jejak</b> {@code oleh}/
	 * {@code olehId}.</p>
	 *
	 * <p>Perlu diingat bahwa {@link #getDefaultData()}, {@link #getAktif()}, dan
	 * {@link #getNomorUrut()} dapat mengotori field saat baris sekadar dibaca, sehingga callback ini
	 * bisa ikut terpicu pada {@code UPDATE} yang <b>tidak diminta pengguna mana pun</b> &mdash;
	 * jejak audit lalu mencatat pengguna yang kebetulan sedang membuka layar.</p>
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
	 * Representasi teks singkat baris: {@code "<id>-<nama>"}.
	 *
	 * <p><b>Beda dari induk:</b> {@link GeneralValueObject#toString()} memakai {@code kode}+
	 * {@code nama}; di sini {@code kode} diganti {@code id}. Method ini juga membaca <b>field</b>
	 * {@code nama} langsung (bukan {@link #getNama()}), sehingga hasilnya <b>tidak di-trim</b> dan
	 * bisa berisi spasi tepi apa adanya dari DB.</p>
	 *
	 * @return {@code id} disambung tanda hubung dan {@code nama}; kedua bagian bisa berbunyi
	 *         {@code "null"} bila belum diisi
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kategori; menjadi judul kelompok pada form isian alumni. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas kategori. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda baris bawaan hasil auto-seed. Lihat {@link #getDefaultData()}. */
	private Boolean defaultData;
	/** Penanda kategori masih dipakai/ditampilkan. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Nomor urut tampil kategori pada form. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	// Field bawaan hbm2java yang DIMATIKAN (beserta pasangan getter/setter-nya di bawah). Tidak ada
	// kolom padanannya di tabel dan tidak ada satu pun pemanggil di codebase. Dibiarkan sebagai
	// jejak sejarah — JANGAN diaktifkan kembali tanpa migrasi skema.
//	private Boolean bolehMengulang;
	/** Penanda kategori khusus portal alumni (bukan layar internal). Lihat {@link #getDigunakanUntukPenggunaAlumni()}. */
	private Boolean digunakanUntukPenggunaAlumni;

	/**
	 * <b>Auto-seed:</b> memastikan selalu ada satu kategori bawaan bertanda
	 * {@link #getDefaultData() defaultData}{@code =true}, dan mengembalikannya.
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>Mengambil session native ThreadLocal lewat
	 *   {@link HibernateUtil#currentNativeSession()}.</li>
	 *   <li>Mencari baris pertama dengan {@code defaultData = true}
	 *   ({@code setMaxResults(1)} + {@code uniqueResult()}).</li>
	 *   <li>Bila <b>tidak</b> ketemu: membuat baris baru dengan {@code defaultData=true},
	 *   {@code nama="Form Tambahan"}, {@code keterangan="Form Tambahan"}, lalu
	 *   {@code begin()} &rarr; {@code save()} &rarr; {@code commit()} transaksi <b>langsung ke
	 *   DB</b>.</li>
	 *   <li>Menutup session ThreadLocal lewat {@link HibernateUtil#closeSession()} &mdash;
	 *   <b>selalu</b>, termasuk pada jalur "sudah ada" yang tidak menulis apa pun.</li>
	 * </ol>
	 *
	 * <h4>Efek samping &amp; kuirk (WAJIB dibaca sebelum memanggil)</h4>
	 * <ul>
	 *   <li><b>Menulis ke DB saat sekadar dibaca.</b> Membuka layar pemanggil sudah cukup untuk
	 *   membuat baris master baru + revisi Envers, tanpa aksi simpan dari pengguna. Ini pola yang
	 *   sekeluarga dengan auto-tulis default pada {@link Konfigurasi}.</li>
	 *   <li><b>Menutup session milik pemanggil.</b> {@code closeSession()} mengeluarkan session dari
	 *   ThreadLocal lalu {@code clear}/{@code rollback}/{@code disconnect}/{@code close}. Kode
	 *   sesudahnya di thread yang sama akan mendapat session BARU, dan entity apa pun yang
	 *   dipegang pemanggil sebelum pemanggilan ini menjadi <b>detached</b> (koleksi lazy-nya tidak
	 *   bisa diinisialisasi lagi). Karena itu method ini aman dipanggil hanya di AWAL sebuah
	 *   request, sebagaimana dilakukan pemanggil satu-satunya.</li>
	 *   <li><b>Objek kembalian selalu detached</b> (session sudah ditutup saat {@code return}).</li>
	 *   <li>{@code begin()} akan melempar bila session ThreadLocal sudah punya transaksi aktif;
	 *   tidak ada {@code try}/{@code rollback} di sini.</li>
	 *   <li>Tidak ada penguncian/keunikan di level DB: dua request bersamaan pada instalasi kosong
	 *   dapat membuat dua baris "Form Tambahan".</li>
	 *   <li>Nama variabel lokal berbunyi {@code kelompokParameterTambahanMahasiswa} &mdash; sisa
	 *   salin-tempel dari varian Mahasiswa; tidak berpengaruh pada perilaku.</li>
	 * </ul>
	 *
	 * <h4>Pemanggil</h4>
	 * <p>Satu-satunya pemanggil di codebase adalah
	 * {@code ais.action.master.ParameterTambahanAlumniAction.doAfterCompose(Component)} &mdash;
	 * layar master "Parameter Tambahan Alumni". Method dipanggil sebelum combobox kategori diisi,
	 * agar dropdown tidak pernah kosong pada instalasi baru. Bandingkan dengan seed berbeda di
	 * {@code KelompokParameterTambahanAlumniAction} yang dibahas pada dokumentasi kelas.</p>
	 *
	 * @return baris kategori bawaan (yang ditemukan atau yang baru saja dibuat); tidak pernah
	 *         {@code null}, tetapi dalam keadaan <b>detached</b>
	 */
	public static KelompokParameterTambahanAlumni checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanAlumni kelompokParameterTambahanMahasiswa = (KelompokParameterTambahanAlumni) session
				.createCriteria(KelompokParameterTambahanAlumni.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanMahasiswa == null) {
			kelompokParameterTambahanMahasiswa = new KelompokParameterTambahanAlumni();
			kelompokParameterTambahanMahasiswa.setDefaultData(true);
			kelompokParameterTambahanMahasiswa.setNama("Form Tambahan");
			kelompokParameterTambahanMahasiswa.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanMahasiswa);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanMahasiswa;
	}

	/**
	 * Konstruktor tanpa argumen. Wajib ada untuk Hibernate/ZK data-binding; seluruh field dibiarkan
	 * pada nilai awalnya ({@code null}, kecuali {@code tanggal_dirubah} yang diisi jam aplikasi).
	 */
	public KelompokParameterTambahanAlumni() {
	}

	/**
	 * Mengembalikan primary key baris kategori.
	 *
	 * <p>Kolom {@code id} bersifat {@code insertable=false} &mdash; nilainya dihasilkan sepenuhnya
	 * oleh sequence/identity PostgreSQL saat {@code INSERT}, jadi menyetelnya sebelum simpan tidak
	 * berpengaruh.</p>
	 *
	 * @return primary key, atau {@code null} untuk objek yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; dipakai jalur load/binding, bukan kode bisnis.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kategori, sudah <b>di-trim</b> spasi tepinya.
	 *
	 * <p>Nama inilah yang dirender sebagai judul kelompok pada form isian alumni
	 * ({@code ParameterTambahanAlumniListener}) dan sebagai label pilihan combobox pada layar
	 * master. Kolom {@code nama} {@code NOT NULL} di DB.</p>
	 *
	 * <p><b>Catatan:</b> trim hanya terjadi pada pembacaan; nilai di field/DB tetap apa adanya, dan
	 * {@link #toString()} yang membaca field langsung tidak ikut ter-trim.</p>
	 *
	 * @return nama kategori tanpa spasi tepi, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kategori. Tanpa validasi &mdash; keunikan nama diperiksa di lapisan layar
	 * ({@code KelompokParameterTambahanAlumniAction.checkNamaKelompokParameterTambahanAlumni()}),
	 * bukan di sini.
	 *
	 * @param nama nama kategori baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas kategori.
	 *
	 * <p><b>Membalik kontrak kelas induk.</b> {@link GeneralValueObject#getKeterangan()}
	 * menormalkan {@code null} menjadi {@code ""} sehingga pemanggil tidak perlu memeriksa
	 * {@code null}; override di sini mengembalikan field <b>apa adanya</b> dan <b>bisa
	 * {@code null}</b>. Kode yang menerima entity lewat tipe {@link GeneralValueObject} karena itu
	 * tetap wajib memeriksa {@code null} bila baris sesungguhnya bertipe kelas ini. Salah satu
	 * akibat langsungnya terlihat pada {@link #compareTo(GeneralValueObject)}.</p>
	 *
	 * @return keterangan kategori, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas kategori. Tanpa validasi; {@code null} diterima.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda "baris bawaan": {@code true} hanya untuk baris hasil auto-seed
	 * {@link #checkCreateDefault()}, yang dipakai sebagai kategori penampung default.
	 *
	 * <p><b>Getter mutatif &mdash; efek samping.</b> Bila field masih {@code null}, method ini
	 * <b>menulis {@code false} ke field</b> sebelum mengembalikannya. Karena kelas ini memakai
	 * property access + {@code dynamicUpdate=true}, perubahan itu terlihat oleh dirty-check
	 * Hibernate: membaca baris lama yang kolomnya {@code NULL} di dalam session aktif dapat memicu
	 * {@code UPDATE} + revisi Envers tanpa aksi pengguna. Sifatnya idempoten/self-healing (nilai
	 * yang ditulis sama dengan nilai yang dikembalikan), tetapi tetap mengotori jejak audit.</p>
	 *
	 * @return {@code true} bila baris ini kategori bawaan; tidak pernah {@code null}
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menyetel penanda baris bawaan. Tanpa validasi.
	 *
	 * <p>Di codebase hanya {@link #checkCreateDefault()} yang menyetelnya ke {@code true}; layar
	 * master tidak menyediakan kendali untuk field ini.</p>
	 *
	 * @param defaultData penanda baru; {@code null} akan dinormalkan menjadi {@code false} pada
	 *        pembacaan berikutnya
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan penanda kategori masih aktif (ditampilkan pada form/dropdown).
	 *
	 * <p>Menjadi filter nyata di beberapa tempat: combobox pencarian layar Parameter Tambahan
	 * Alumni, perakitan form isian di {@code ParameterTambahanAlumniListener}, dan rekap
	 * {@code DashboardRekapParameterTambahanAlumni} &mdash; semuanya menyaring
	 * {@code aktif = true} (sebagian juga menerima {@code NULL}).</p>
	 *
	 * <p><b>Getter mutatif &mdash; efek samping.</b> Sama seperti {@link #getDefaultData()}: field
	 * {@code null} <b>ditulisi</b> nilai default (di sini {@code true}) sebelum dikembalikan,
	 * sehingga dapat memicu {@code UPDATE} + revisi Envers pada baris yang sekadar dibaca.
	 * Perhatikan default-nya {@code true}, jadi kategori lama tanpa nilai dianggap AKTIF.</p>
	 *
	 * @return {@code true} bila kategori aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel penanda kategori aktif. Tanpa validasi.
	 *
	 * @param aktif penanda baru; {@code null} akan dinormalkan menjadi {@code true} pada pembacaan
	 *        berikutnya
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil kategori pada form isian; makin kecil makin atas.
	 *
	 * <p>Menjadi kunci utama {@link #compareTo(GeneralValueObject)}, yaitu urutan yang dipakai
	 * {@code Collections.sort(...)} di {@code ParameterTambahanAlumniListener} dan
	 * {@code DashboardRekapParameterTambahanAlumni}.</p>
	 *
	 * <p><b>Getter mutatif &mdash; efek samping.</b> Field {@code null} <b>ditulisi</b> {@code 1}
	 * sebelum dikembalikan; berlaku catatan dirty-check/Envers yang sama dengan
	 * {@link #getDefaultData()}.</p>
	 *
	 * <p><b>Kuirk kode mati:</b> ekspresi kembalian {@code nomorUrut == null ? 1 : nomorUrut} sudah
	 * tidak mungkin bercabang, karena {@code null} baru saja diganti {@code 1} tepat di atasnya.</p>
	 *
	 * <p><b>Konsekuensi pengurutan:</b> karena default-nya sama untuk semua baris ({@code 1}),
	 * kategori yang belum diberi nomor urut akan <b>seri</b>; {@code Collections.sort} bersifat
	 * stabil sehingga urutan tampilnya jatuh ke urutan hasil query DB, bukan ke urutan yang
	 * bermakna bagi pengguna.</p>
	 *
	 * @return nomor urut tampil; tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil kategori. Tanpa validasi (angka negatif/duplikat diterima).
	 *
	 * @param nomorUrut nomor urut baru; {@code null} akan dinormalkan menjadi {@code 1} pada
	 *        pembacaan berikutnya
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Urutan alami kategori, meng-override {@link GeneralValueObject#compareTo(GeneralValueObject)}.
	 *
	 * <h4>Dua cabang</h4>
	 * <ol>
	 *   <li><b>Pembanding bertipe sama</b> ({@code instanceof KelompokParameterTambahanAlumni}):
	 *   membandingkan {@link #getNomorUrut()} saja. Inilah satu-satunya cabang yang benar-benar
	 *   terpakai di produksi, karena semua pemanggil ({@code Collections.sort} atas
	 *   {@code List<KelompokParameterTambahanAlumni>} di {@code ParameterTambahanAlumniListener} dan
	 *   {@code DashboardRekapParameterTambahanAlumni}) selalu membandingkan sesama kategori. Karena
	 *   {@code getNomorUrut()} tidak pernah {@code null}, cabang ini bebas NPE &mdash; tetapi juga
	 *   memicu efek samping mutatif getter tersebut.</li>
	 *   <li><b>Pembanding bertipe lain:</b> rantai fallback berjenjang {@code nim} &rarr;
	 *   {@code nama} &rarr; {@code keterangan}, meniru implementasi induk.</li>
	 * </ol>
	 *
	 * <h4>Hasil verifikasi {@code getNim()} (pertanyaan yang sering muncul)</h4>
	 * <p>Pemanggilan {@code arg0.getNim()} pada parameter bertipe {@link GeneralValueObject}
	 * <b>SAH dan bukan bug kompilasi maupun {@code ClassCastException}</b>: {@code getNim()} memang
	 * dideklarasikan di {@link GeneralValueObject} sendiri (mengembalikan field {@code nim} milik
	 * induk), sehingga tersedia untuk SEMUA subclass. Yang perlu diketahui justru
	 * <b>semantiknya</b>: kelas ini adalah master kategori, bukan peserta didik, dan {@code nim}
	 * induk <b>tidak pernah diisi</b> untuk entity ini &mdash; tidak dipetakan Hibernate (induk
	 * bukan {@code @MappedSuperclass}) dan tidak pernah disetel kode mana pun. Jadi
	 * {@code getNim()} pada {@code this} <b>selalu {@code null}</b>, dan cabang {@code nim}
	 * <b>tidak pernah dieksekusi sampai perbandingan</b> &mdash; kode mati efektif, sisa
	 * salin-tempel dari implementasi induk.</p>
	 *
	 * <h4>Kuirk lain</h4>
	 * <ul>
	 *   <li>Cabang {@code keterangan} di sini <b>bisa gagal memenuhi syarat</b>, berbeda dengan
	 *   induk: {@link #getKeterangan()} kelas ini boleh mengembalikan {@code null} (lihat
	 *   dokumentasinya), sedangkan versi induk selalu mengembalikan minimal {@code ""}.</li>
	 *   <li>Bila tidak ada kunci yang memenuhi syarat &mdash; atau terjadi exception, yang
	 *   <b>ditelan</b> dan hanya dicatat ke audit &mdash; hasilnya {@code 0}, yang berarti "dianggap
	 *   setara untuk pengurutan", <b>bukan</b> {@code equals}. {@code compareTo} di sini tidak
	 *   konsisten dengan {@link GeneralValueObject#equals(Object)}, jadi hindari
	 *   {@code TreeSet}/{@code TreeMap} berkunci entity ini (risiko penciutan senyap).</li>
	 *   <li>Cabang tipe-sama <b>tidak</b> pernah mengembalikan {@code 0} lewat jalur exception,
	 *   sehingga urutan seri sepenuhnya bergantung kestabilan algoritma pengurut.</li>
	 * </ul>
	 *
	 * @param arg0 entity pembanding; boleh subclass {@link GeneralValueObject} apa pun
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}; {@code 0} bila tidak ada kunci
	 *         pembanding yang tersedia atau terjadi exception
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		if (arg0 instanceof KelompokParameterTambahanAlumni) {
			KelompokParameterTambahanAlumni s = (KelompokParameterTambahanAlumni) arg0;
			return getNomorUrut().compareTo(s.getNomorUrut());
		} else {
			try {
				if (getNim() != null && arg0.getNim() != null) {
					return getNim().compareTo(arg0.getNim());
				} else if (getNama() != null && arg0.getNama() != null) {
					return getNama().compareTo(arg0.getNama());
				} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
					return getKeterangan().compareTo(arg0.getKeterangan());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KelompokParameterTambahanAlumni.java:181");

			}

			return 0;
		}
	}

	// Pasangan getter/setter untuk field {@code bolehMengulang} yang dimatikan di atas. Tidak ada
	// kolom DB maupun pemanggilnya; dipertahankan hanya sebagai jejak sejarah.
//	public Boolean getBolehMengulang() {
//		return bolehMengulang == null ? false : bolehMengulang;
//	}
//
//	public void setBolehMengulang(Boolean bolehMengulang) {
//		this.bolehMengulang = bolehMengulang;
//	}

	/**
	 * Mengembalikan penanda "kategori ini dipakai oleh <b>portal pengguna alumni</b>", yaitu form
	 * yang diisi sendiri oleh alumni, bukan layar internal staf.
	 *
	 * <p>{@link ais.action.master.helper.ParameterTambahanAlumniListener} memakainya sebagai
	 * <b>pemisah dua dunia</b>: saat merender form untuk alumni, hanya kategori bernilai
	 * {@code true} yang ditarik; saat merender untuk petugas, hanya yang bernilai {@code false}
	 * <b>atau {@code NULL}</b> (jadi kategori lama tanpa nilai default-nya masuk ke layar internal,
	 * bukan ke portal alumni &mdash; arah yang lebih aman).</p>
	 *
	 * <p><b>Berbeda dari tiga getter Boolean lainnya di kelas ini</b>, method ini memakai ternary
	 * dan <b>TIDAK</b> menulis balik ke field &mdash; tidak ada efek samping dirty-check di sini.
	 * Inkonsistensi gaya ini disengaja dicatat, bukan diperbaiki.</p>
	 *
	 * @return {@code true} bila kategori ditujukan untuk portal alumni; tidak pernah {@code null}
	 */
	public Boolean getDigunakanUntukPenggunaAlumni() {
		return digunakanUntukPenggunaAlumni == null ? false : digunakanUntukPenggunaAlumni;
	}

	/**
	 * Menyetel penanda kategori khusus portal alumni. Tanpa validasi.
	 *
	 * <p>Nilai {@code null} tetap tersimpan sebagai {@code NULL} di DB (setter tidak menormalkan),
	 * dan {@code NULL} diperlakukan sama dengan {@code false} baik oleh {@link
	 * #getDigunakanUntukPenggunaAlumni()} maupun oleh kriteria penyaring di
	 * {@code ParameterTambahanAlumniListener}.</p>
	 *
	 * @param digunakanUntukPenggunaAlumni penanda baru; {@code null} diterima
	 */
	public void setDigunakanUntukPenggunaAlumni(Boolean digunakanUntukPenggunaAlumni) {
		this.digunakanUntukPenggunaAlumni = digunakanUntukPenggunaAlumni;
	}
}
