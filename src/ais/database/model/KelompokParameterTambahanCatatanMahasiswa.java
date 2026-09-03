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
 * Entity master <b>kategori/kelompok "form tambahan" (field kustom) untuk data Catatan
 * Mahasiswa</b>, dipetakan ke tabel {@code public.kelompok_parameter_tambahan_catatan_mahasiswa}.
 *
 * <h3>Peran dalam modul Catatan Mahasiswa</h3>
 * <p>{@link CatatanMahasiswa} adalah pencatatan kejadian yang menempel pada seorang mahasiswa
 * (pelanggaran, prestasi, konseling/BK, peringatan, dan sejenisnya). Karena isi catatan sangat
 * berbeda antar perguruan tinggi &mdash; bahkan antar jenis catatan di satu perguruan tinggi
 * &mdash; AIS tidak menambah kolom untuk setiap kebutuhan, melainkan memakai rantai "parameter
 * tambahan" <b>empat</b> lapis:</p>
 * <ol>
 *   <li>{@link ParameterTambahan} &mdash; <b>definisi</b> field kustom generik (label, tipe input,
 *   daftar pilihan, wajib/tidak, perlu lampiran). Dipakai bersama oleh banyak modul.</li>
 *   <li>{@link ParameterTambahanCatatanMahasiswa} &mdash; tabel penghubung yang <b>mengaitkan</b>
 *   sebuah {@link ParameterTambahan} ke satu baris kelas ini.</li>
 *   <li><b>Kelas ini</b> &mdash; <b>kategori/judul kelompok</b> tempat field-field tersebut
 *   ditampilkan berkelompok pada form. Satu baris kelas ini menjadi satu heading section pada
 *   layar isian catatan.</li>
 *   <li>{@link JenisCatatanMahasiswa} &mdash; <b>lapisan tambahan yang TIDAK ada pada varian
 *   Alumni/Mahasiswa/Calon Mahasiswa</b>: relasi {@code @ManyToMany} (tabel
 *   {@code jenis_catatan_mahasiswa_has_parameter}) yang menentukan kelompok mana saja yang muncul
 *   untuk sebuah jenis catatan. Jadi form isian di sini <b>bervariasi per jenis catatan</b>,
 *   bukan seragam satu form seperti pada modul biodata.</li>
 * </ol>
 * <p>Nilai jawaban <b>tidak</b> disimpan di sini melainkan sebagai string terserialisasi pada
 * {@code CatatanMahasiswa.parameterTambahanInds} dengan format baris
 * {@code "kelompok->parameter<=>nilai<=>keterangan"}; lihat
 * {@link ais.action.master.helper.ParameterTambahanCatatanMahasiswaListener}.</p>
 *
 * <h3>Keluarga entity sejenis</h3>
 * <p>Kelas ini satu keluarga dengan {@link ais.database.model.KelompokParameterTambahanAlumni},
 * {@link KelompokParameterTambahanMahasiswa}, dan {@code KelompokParameterTambahanCalonMahasiswa}.
 * Isi kelas <b>identik kata-per-kata</b> dengan varian Alumni/Mahasiswa (diverifikasi lewat diff
 * otomatis atas versi pristine) kecuali tiga hal:</p>
 * <ul>
 *   <li>nama tabel/kelas;</li>
 *   <li>kelas ini <b>tidak</b> punya field ekstra {@code digunakanUntukPenggunaAlumni} (khas
 *   varian Alumni) maupun sisa {@code bolehMengulang} yang dikomentari;</li>
 *   <li><b>{@link #compareTo(GeneralValueObject)} kelas ini VERSI PENDEK</b> &mdash; satu baris
 *   tanpa penjaga {@code instanceof} dan tanpa rantai fallback
 *   {@code nim}&rarr;{@code nama}&rarr;{@code keterangan}. Konsekuensinya dibahas pada
 *   dokumentasi method tersebut; ini satu-satunya beda perilaku yang nyata terhadap keluarganya.</li>
 * </ul>
 *
 * <h3>Auto-seed lewat {@link #checkCreateDefault()}</h3>
 * <p>Kelas ini memakai pola <b>auto-seed</b> yang sekeluarga dengan {@link Konfigurasi}, tetapi
 * versi entity biasa (satu baris = satu kategori), bukan key-value: bila belum ada satu pun baris
 * bertanda {@link #getDefaultData() defaultData}{@code =true}, method statis
 * {@link #checkCreateDefault()} membuatnya sendiri dan meng-{@code commit} langsung ke DB. Efeknya,
 * sekadar <b>membuka layar</b> "Parameter Tambahan Catatan Mahasiswa" sudah menulis baris master
 * baru. Lihat dokumentasi method tersebut untuk kuirk transaksi/session-nya.</p>
 * <p><b>Berbeda dari varian Alumni, di sini hanya ADA SATU mekanisme seed.</b> Layar
 * <i>Kelompok</i> ({@code ais.action.master.KelompokParameterTambahanCatatanMahasiswaAction}) tidak
 * membuat baris bawaan apa pun pada {@code doAfterCompose(...)}, sehingga <b>tidak ada balapan
 * dua-seed</b> seperti yang terjadi pada {@link ais.database.model.KelompokParameterTambahanAlumni}
 * (di sana layar Kelompok menyeed {@code "Data Alumni"} dengan syarat berbeda sehingga instalasi
 * kosong bisa berakhir dengan dua kategori bawaan). Diperiksa dan dikonfirmasi tidak berlaku untuk
 * modul ini.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Atribut kategori:</b> {@link #getNama()}, {@link #getKeterangan()},
 *   {@link #getNomorUrut()}.</li>
 *   <li><b>Penanda perilaku:</b> {@link #getDefaultData()} (penanda baris bawaan, sekaligus
 *   pelindung dari tombol Hapus di layar master), {@link #getAktif()} (tampil/tidak).</li>
 *   <li><b>Utilitas statis:</b> {@link #checkCreateDefault()}.</li>
 *   <li><b>Pengurutan:</b> {@link #compareTo(GeneralValueObject)}.</li>
 * </ul>
 *
 * <h3>Catatan warisan &amp; pemetaan (non-obvious)</h3>
 * <ul>
 *   <li>{@link GeneralValueObject} <b>bukan</b> {@code @MappedSuperclass} dan bukan {@code @Entity},
 *   sehingga Hibernate <b>mengabaikan seluruh property-nya</b>. Itulah sebabnya {@code id},
 *   {@code nama}, {@code keterangan}, {@code nomorUrut}, {@code oleh}, {@code olehId} dideklarasikan
 *   ULANG di kelas ini (field kelas ini <i>membayangi</i> field induk yang bernama sama) &mdash;
 *   ini KEHARUSAN TEKNIS, bukan duplikasi yang keliru. Field induk yang tidak dideklarasikan ulang
 *   &mdash; termasuk {@code nim} &mdash; tetap ada di memori tetapi tidak pernah terisi.</li>
 *   <li>Anotasi berada di <b>getter</b> ({@code @Id} pada {@link #getId()}), jadi Hibernate memakai
 *   <b>property access</b> untuk SEMUA property. Getter yang memodifikasi field karena itu terlihat
 *   oleh dirty-check; dikombinasikan dengan {@code dynamicUpdate=true}, sekadar membaca baris bisa
 *   memicu {@code UPDATE} + revisi Envers. Lihat {@link #getDefaultData()}, {@link #getAktif()},
 *   {@link #getNomorUrut()}.</li>
 *   <li>{@code defaultData}, {@code aktif}, dan {@code nomorUrut} <b>tidak</b> punya {@code @Column}
 *   eksplisit sehingga nama kolomnya mengikuti strategi penamaan default Hibernate (nama property
 *   apa adanya, dilipat ke huruf kecil oleh PostgreSQL).</li>
 *   <li>{@code @Audited} &mdash; setiap perubahan direkam Envers ke tabel revisi.</li>
 * </ul>
 *
 * <p><b>Konsumen utama:</b>
 * {@code ais.action.master.KelompokParameterTambahanCatatanMahasiswaAction} (CRUD master
 * kategori), {@code ais.action.master.ParameterTambahanCatatanMahasiswaAction} (pengaitan field ke
 * kategori; <b>satu-satunya pemanggil</b> {@link #checkCreateDefault()}),
 * {@code ais.action.master.JenisCatatanMahasiswaAction} (pemilihan kategori per jenis catatan),
 * {@code ais.action.master.CatatanMahasiswaAction} (perakitan form isian catatan),
 * {@link ais.action.master.helper.ParameterTambahanCatatanMahasiswaListener} (perender baris
 * parameter tambahan), dan {@link JenisCatatanMahasiswa} (pemilik relasi
 * {@code @ManyToMany}).</p>
 *
 * <p><b>Konkurensi:</b> tidak thread-safe (POJO Hibernate biasa); jangan berbagi instance lintas
 * session/thread.</p>
 *
 * @see ParameterTambahanCatatanMahasiswa
 * @see ParameterTambahan
 * @see JenisCatatanMahasiswa
 * @see CatatanMahasiswa
 * @see ais.database.model.KelompokParameterTambahanAlumni
 * @see KelompokParameterTambahanMahasiswa
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_parameter_tambahan_catatan_mahasiswa")
public class KelompokParameterTambahanCatatanMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar instance lama tetap dapat dideserialisasi
	 * (relevan untuk sesi ZK yang di-passivate ke disk).
	 *
	 * <p><b>Kuirk:</b> nilainya sama persis dengan milik
	 * {@link ais.database.model.KelompokParameterTambahanAlumni} dan
	 * {@link KelompokParameterTambahanMahasiswa} &mdash; sisa salin-tempel. Tidak berbahaya karena
	 * {@code serialVersionUID} hanya dibandingkan antar versi kelas yang <b>sama</b>.</p>
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
	 * ini. Karena itu baris hasil auto-seed {@link #checkCreateDefault()} masuk <b>tanpa jejak</b>
	 * {@code oleh}/{@code olehId}.</p>
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
	 * <p>Ikut terlihat pengguna secara tidak langsung: {@code JenisCatatanMahasiswaAction}
	 * mencetak koleksi kategori terpilih ke {@code System.out} memakai representasi ini.</p>
	 *
	 * @return {@code id} disambung tanda hubung dan {@code nama}; kedua bagian bisa berbunyi
	 *         {@code "null"} bila belum diisi
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kategori; menjadi judul kelompok pada form isian catatan mahasiswa. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas kategori; ditampilkan sebagai kolom kedua pada grid layar master. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda baris bawaan hasil auto-seed. Lihat {@link #getDefaultData()}. */
	private Boolean defaultData;
	/** Penanda kategori masih dipakai/ditampilkan. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Nomor urut tampil kategori pada form. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

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
	 *   dapat membuat dua baris "Form Tambahan". Perhatikan bahwa layar master Kelompok menolak
	 *   nama duplikat lewat validasi aplikasi, tetapi jalur seed ini <b>melewati</b> validasi
	 *   tersebut.</li>
	 *   <li>Baris hasil seed lahir dengan {@code nomorUrut} {@code NULL} (tidak pernah disetel di
	 *   sini) &mdash; lihat konsekuensinya pada {@link #getNomorUrut()} dan
	 *   {@link #compareTo(GeneralValueObject)}.</li>
	 *   <li><b>Nama variabel lokal salah ketik</b>: {@code kelompokParameterTambahanCatatanMahamahasiswa}
	 *   (ada "Maha" berlebih) &mdash; sisa salin-tempel dari varian lain; tidak berpengaruh pada
	 *   perilaku.</li>
	 * </ul>
	 *
	 * <h4>Pemanggil</h4>
	 * <p>Satu-satunya pemanggil di codebase adalah
	 * {@code ais.action.master.ParameterTambahanCatatanMahasiswaAction.doAfterCompose(Component)}
	 * &mdash; layar master "Parameter Tambahan Catatan Mahasiswa". Method dipanggil di awal
	 * {@code doAfterCompose}, sebelum combobox kategori diisi lewat
	 * {@code Common.insertCombo(...)}, agar dropdown tidak pernah kosong pada instalasi baru.</p>
	 * <p>Layar <i>Kelompok</i> ({@code KelompokParameterTambahanCatatanMahasiswaAction})
	 * <b>tidak</b> memanggil method ini dan tidak punya mekanisme seed sendiri &mdash; berbeda dari
	 * varian Alumni, sehingga modul ini bebas dari balapan dua-seed.</p>
	 *
	 * @return baris kategori bawaan (yang ditemukan atau yang baru saja dibuat); tidak pernah
	 *         {@code null}, tetapi dalam keadaan <b>detached</b>
	 */
	public static KelompokParameterTambahanCatatanMahasiswa checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanCatatanMahasiswa kelompokParameterTambahanCatatanMahamahasiswa = (KelompokParameterTambahanCatatanMahasiswa) session
				.createCriteria(KelompokParameterTambahanCatatanMahasiswa.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanCatatanMahamahasiswa == null) {
			kelompokParameterTambahanCatatanMahamahasiswa = new KelompokParameterTambahanCatatanMahasiswa();
			kelompokParameterTambahanCatatanMahamahasiswa.setDefaultData(true);
			kelompokParameterTambahanCatatanMahamahasiswa.setNama("Form Tambahan");
			kelompokParameterTambahanCatatanMahamahasiswa.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanCatatanMahamahasiswa);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanCatatanMahamahasiswa;
	}

	/**
	 * Konstruktor tanpa argumen. Wajib ada untuk Hibernate/ZK data-binding; seluruh field dibiarkan
	 * pada nilai awalnya ({@code null}, kecuali {@code tanggal_dirubah} yang diisi jam aplikasi).
	 *
	 * <p>Dipakai langsung oleh {@link #checkCreateDefault()} dan oleh tombol "Tambah" pada layar
	 * master Kelompok.</p>
	 */
	public KelompokParameterTambahanCatatanMahasiswa() {
	}

	/**
	 * Mengembalikan primary key baris kategori.
	 *
	 * <p>Kolom {@code id} bersifat {@code insertable=false} &mdash; nilainya dihasilkan sepenuhnya
	 * oleh sequence/identity PostgreSQL saat {@code INSERT}, jadi menyetelnya sebelum simpan tidak
	 * berpengaruh.</p>
	 *
	 * <p>Dipakai layar {@code JenisCatatanMahasiswaAction} sebagai kunci pencocokan checkbox
	 * kategori terpilih (bukan {@code equals} entity).</p>
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
	 * <p>Nama inilah yang dirender sebagai judul kelompok pada form isian catatan mahasiswa
	 * ({@link ais.action.master.helper.ParameterTambahanCatatanMahasiswaListener} dan
	 * {@code CatatanMahasiswaAction}), sebagai label checkbox pada layar
	 * {@code JenisCatatanMahasiswaAction}, dan sebagai label pilihan combobox pada layar
	 * {@code ParameterTambahanCatatanMahasiswaAction}. Kolom {@code nama} {@code NOT NULL} di
	 * DB.</p>
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
	 * Menyetel nama kategori. Tanpa validasi &mdash; kewajiban isi dan keunikan nama diperiksa di
	 * lapisan layar
	 * ({@code KelompokParameterTambahanCatatanMahasiswaAction.onSave(...)} dan
	 * {@code checkNamaKelompokParameterTambahanCatatanMahasiswa()}), bukan di sini.
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
	 * tetap wajib memeriksa {@code null} bila baris sesungguhnya bertipe kelas ini.</p>
	 *
	 * <p>Akibat nyata di UI: renderer grid layar master membungkus nilai ini langsung ke
	 * {@code new Label(...)}, sehingga kategori tanpa keterangan tampil sebagai sel kosong (ZK
	 * menerima {@code null}); tidak ada NPE, tetapi juga tidak ada teks pengganti.</p>
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
	 * <p>Diisi oleh {@link #checkCreateDefault()} (nilai {@code "Form Tambahan"}) dan oleh kotak
	 * teks "Keterangan" pada dialog tambah/ubah layar master.</p>
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
	 * <p>Dipakai layar master sebagai <b>pelindung</b>: tombol Hapus dirender dengan
	 * {@code setVisible(delete && !getDefaultData())}, sehingga baris bawaan tidak bisa dihapus
	 * lewat UI.</p>
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
	 * master tidak menyediakan kendali untuk field ini, jadi status "bawaan" tidak dapat
	 * dipindahkan ke kategori lain lewat UI.</p>
	 *
	 * @param defaultData penanda baru; {@code null} akan dinormalkan menjadi {@code false} pada
	 *        pembacaan berikutnya
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan penanda kategori masih aktif (ditampilkan pada form isian).
	 *
	 * <p>Menjadi filter nyata pada perakitan form catatan mahasiswa: baik
	 * {@link ais.action.master.helper.ParameterTambahanCatatanMahasiswaListener} maupun
	 * {@code CatatanMahasiswaAction} menyaring
	 * {@code kelompokParameterTambahanCatatanMahasiswa.aktif = true} pada kriteria pengambilan
	 * {@link ParameterTambahan}. Kategori non-aktif karena itu hilang dari form isian meskipun
	 * masih terkait ke {@link JenisCatatanMahasiswa}.</p>
	 *
	 * <p><b>Getter mutatif &mdash; efek samping.</b> Sama seperti {@link #getDefaultData()}: field
	 * {@code null} <b>ditulisi</b> nilai default (di sini {@code true}) sebelum dikembalikan,
	 * sehingga dapat memicu {@code UPDATE} + revisi Envers pada baris yang sekadar dibaca.
	 * Perhatikan default-nya {@code true}, jadi kategori lama tanpa nilai dianggap AKTIF.</p>
	 *
	 * <p><b>Catatan penyaringan:</b> kriteria di atas memakai {@code Restrictions.eq} yang
	 * <b>tidak</b> cocok dengan {@code NULL} di SQL. Baris yang kolomnya masih {@code NULL} karena
	 * itu tersaring KELUAR dari form isian, meskipun getter ini melaporkannya {@code true} di sisi
	 * Java &mdash; ketidakselarasan yang baru hilang setelah baris tersebut ter-{@code UPDATE}
	 * oleh efek samping getter ini.</p>
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
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Aktif" pada grid layar master, diikuti
	 * {@code Common.refreshSaveOrUpdate(...)} yang langsung menyimpannya.</p>
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
	 * <p>Menjadi <b>satu-satunya</b> kunci {@link #compareTo(GeneralValueObject)}, sekaligus kunci
	 * pertama {@code @OrderBy("nomorUrut asc, nama asc")} pada relasi
	 * {@link JenisCatatanMahasiswa#getKelompokParameterTambahanCatatanMahasiswas()}.</p>
	 *
	 * <p><b>Getter mutatif &mdash; efek samping.</b> Field {@code null} <b>ditulisi</b> {@code 1}
	 * sebelum dikembalikan; berlaku catatan dirty-check/Envers yang sama dengan
	 * {@link #getDefaultData()}.</p>
	 *
	 * <p><b>Kuirk kode mati:</b> ekspresi kembalian {@code nomorUrut == null ? 1 : nomorUrut} sudah
	 * tidak mungkin bercabang, karena {@code null} baru saja diganti {@code 1} tepat di atasnya.</p>
	 *
	 * <p><b>Konsekuensi pengurutan.</b> Nilai default-nya sama untuk semua baris ({@code 1}) dan
	 * layar master <b>tidak</b> menyetelnya saat kategori dibuat &mdash; dialog tambah/ubah hanya
	 * mengisi {@code nama} dan {@code keterangan}, sedangkan {@link #checkCreateDefault()} juga
	 * tidak mengisinya. Nomor urut baru terisi bila admin mengetikkannya pada kotak angka di grid
	 * layar master. Selama belum diisi, semua kategori <b>seri</b>; lihat
	 * {@link #compareTo(GeneralValueObject)} untuk akibatnya yang tidak sekadar kosmetik.</p>
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
	 * Menyetel nomor urut tampil kategori. Tanpa validasi (angka negatif/duplikat/{@code null}
	 * diterima).
	 *
	 * <p>Dipanggil dari listener {@code onChange} kotak angka pada grid layar master, diikuti
	 * {@code Common.refreshSaveOrUpdate(...)} yang langsung menyimpannya.</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} akan dinormalkan menjadi {@code 1} pada
	 *        pembacaan berikutnya
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Urutan alami kategori, meng-override {@link GeneralValueObject#compareTo(GeneralValueObject)}:
	 * membandingkan {@link #getNomorUrut()} saja.
	 *
	 * <h4>Beda dari keluarga sejenis</h4>
	 * <p>Berbeda dengan {@link ais.database.model.KelompokParameterTambahanAlumni} dan
	 * {@link KelompokParameterTambahanMahasiswa} yang memakai bentuk dua cabang
	 * ({@code instanceof} + rantai fallback {@code nim}&rarr;{@code nama}&rarr;{@code keterangan}),
	 * versi di kelas ini <b>langsung meng-cast</b> {@code arg0}. Akibatnya:</p>
	 * <ul>
	 *   <li>Membandingkan baris kelas ini dengan {@link GeneralValueObject} bertipe LAIN melempar
	 *   {@link ClassCastException} &mdash; tidak ada penjaga dan tidak ada {@code try}/{@code catch}
	 *   sama sekali. Aman dalam praktik karena semua pemanggil yang ada bekerja pada koleksi
	 *   homogen ({@code Set<KelompokParameterTambahanCatatanMahasiswa>}), tetapi kontraknya jauh
	 *   lebih sempit daripada saudara-saudaranya &mdash; jangan masukkan entity ini ke koleksi
	 *   terurut campuran.</li>
	 *   <li>Sisi baiknya, cabang {@code getNim()} yang di keluarga lain merupakan kode mati (field
	 *   {@code nim} milik induk tidak pernah terisi untuk entity master seperti ini, karena
	 *   {@link GeneralValueObject} bukan {@code @MappedSuperclass}) memang <b>tidak ada</b> di
	 *   sini.</li>
	 *   <li>Bebas NPE: {@link #getNomorUrut()} tidak pernah mengembalikan {@code null} &mdash;
	 *   tetapi pemanggilannya memicu efek samping mutatif getter tersebut pada KEDUA objek yang
	 *   dibandingkan.</li>
	 * </ul>
	 *
	 * <h4>Kuirk penting &mdash; tidak konsisten dengan {@code equals}</h4>
	 * <p>Dua kategori BERBEDA dengan {@code nomorUrut} sama menghasilkan {@code 0}, sedangkan
	 * {@link GeneralValueObject#equals(Object)} tetap menganggapnya berbeda. Ini menjadi masalah
	 * nyata karena entity ini memang dipakai di dalam {@code TreeSet}:
	 * {@link JenisCatatanMahasiswa} menginisialisasi koleksinya dengan {@code new TreeSet<...>()}
	 * dan {@code CatatanMahasiswaAction} menyalin kategori sebuah jenis catatan ke
	 * {@code new TreeSet<KelompokParameterTambahanCatatanMahasiswa>()} sebelum menyerahkannya ke
	 * {@link ais.action.master.helper.ParameterTambahanCatatanMahasiswaListener}. Karena
	 * {@code TreeSet} membuang elemen yang {@code compareTo}-nya {@code 0}, kategori yang seri
	 * <b>menciut senyap</b> menjadi satu &mdash; dan sebagaimana dijelaskan pada
	 * {@link #getNomorUrut()}, kondisi "semua seri di {@code 1}" adalah keadaan <b>bawaan</b>
	 * selama admin belum mengisi nomor urut secara manual. Dicatat apa adanya; tidak diperbaiki di
	 * sini.</p>
	 *
	 * @param arg0 entity pembanding; secara praktis <b>wajib</b> bertipe
	 *        {@code KelompokParameterTambahanCatatanMahasiswa}
	 * @return negatif/nol/positif hasil perbandingan {@link #getNomorUrut()} kedua objek
	 * @throws ClassCastException bila {@code arg0} bukan
	 *         {@code KelompokParameterTambahanCatatanMahasiswa}
	 * @throws NullPointerException bila {@code arg0} bernilai {@code null}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanCatatanMahasiswa) arg0).getNomorUrut());
	}


}
