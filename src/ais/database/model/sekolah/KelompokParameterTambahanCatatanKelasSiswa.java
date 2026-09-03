package ais.database.model.sekolah;

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




import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;



import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Master <b>kelompok (kategori) field kustom untuk Catatan Kelas Siswa</b> pada modul SEKOLAH —
 * tabel {@code sekolah.kelompok_parameter_tambahan_catatan_kelas_siswa}.
 *
 * <p><b>Domain — TERVERIFIKASI dari kode, bukan dugaan.</b> Meski namanya memuat kata "Siswa",
 * entity pemilik datanya adalah {@link CatatanKelasSiswa}, yang berelasi {@code @ManyToOne} ke
 * {@link KelasSiswa} (rombongan belajar) dan <b>bukan</b> ke {@code Siswa} perorangan. Jadi ini
 * benar-benar catatan tingkat <b>KELAS</b> ("Catatan [Kelas Siswa]"), bukan "[Catatan Siswa] per
 * kelas": satu baris {@code CatatanKelasSiswa} berlaku untuk seluruh anggota rombel pada satu
 * tahun ajaran + semester. Bukti tambahan dari {@link CatatanKelasSiswa}: {@code getKode()} dan
 * {@code getNama()} di sana menyalin kode/nama <i>kelas</i>, bukan identitas siswa.</p>
 *
 * <p>Karena bertumpu pada konsep "kelas/rombel", keluarga entity ini <b>tidak punya padanan</b> di
 * versi non-sekolah (perguruan tinggi) — bandingkan
 * {@link ais.database.model.KelompokParameterTambahanCatatanMahasiswa} yang bersandar pada
 * mahasiswa perorangan.</p>
 *
 * <h2>Posisi dalam rantai konfigurasi field kustom (4 lapis)</h2>
 * <ol>
 *   <li>{@link ais.database.model.ParameterTambahan} — definisi teknis satu field kustom (label,
 *   tipe isian, wajib/tidak, perlu lampiran atau tidak, {@code nomorUrut}).</li>
 *   <li><b>Entity ini</b> — kategori/seksi yang mengelompokkan field-field tersebut di formulir.</li>
 *   <li>{@link ParameterTambahanCatatanKelasSiswa} — tabel penghubung
 *   {@code ParameterTambahan} &times; kelompok, menentukan field mana masuk kelompok mana.</li>
 *   <li>{@link JenisCatatanKelasSiswa} — jenis catatan (mis. "Catatan Wali Kelas"); punya relasi
 *   {@code @ManyToMany} ke entity ini lewat tabel {@code sekolah.jenis_catatan_kelas_siswa_has_parameter}.
 *   Sebuah kelompok baru benar-benar muncul di formulir bila <b>dicentang</b> pada jenis catatan
 *   yang dipilih.</li>
 * </ol>
 *
 * <p>Nilai isian yang diketik pengguna <b>tidak</b> disimpan di sini, melainkan didenormalisasi ke
 * dua kolom {@code text} milik {@link CatatanKelasSiswa} ({@code parameterTambahan} berlabel dan
 * {@code parameterTambahanInds} ber-ID) dengan pemisah {@code "\n"} antar baris dan {@code "<=>"}
 * antar ruas. Id entity inilah yang menjadi ruas pertama kunci gabungan
 * {@code idKelompok + "->" + idParameter} — kunci yang sama juga dipakai sebagai argumen
 * {@code jenis} pada {@code LampiranLain.ambil(idCatatan, jenis)}. <b>Konsekuensi:</b> menghapus
 * atau mengubah id baris kelompok memutus tautan ke nilai isian <i>dan</i> ke lampiran yang sudah
 * tersimpan.</p>
 *
 * <h2>Pembaca runtime yang sudah terverifikasi</h2>
 * <ul>
 *   <li>{@code ais.action.master.sekolah.KelompokParameterTambahanCatatanKelasSiswaAction} —
 *   layar master CRUD entity ini.</li>
 *   <li>{@code ais.action.master.sekolah.ParameterTambahanCatatanKelasSiswaAction} — layar
 *   pemetaan field ke kelompok; <b>satu-satunya pemanggil</b> {@link #checkCreateDefault()}.</li>
 *   <li>{@code ais.action.master.sekolah.JenisCatatanKelasSiswaAction} — daftar centang kelompok
 *   per jenis catatan.</li>
 *   <li>{@code ais.action.master.sekolah.CatatanKelasSiswaAction} dan
 *   {@code ais.action.master.sekolah.helper.ParameterTambahanCatatanKelasSiswaListener} — perakit
 *   formulir isian.</li>
 *   <li>{@code ais.action.master.sekolah.CatatanSiswaAction} — layar catatan <i>per siswa</i> juga
 *   ikut memuat catatan tingkat kelas milik kelas siswa tersebut, sehingga kelompok ini muncul di
 *   dua layar berbeda.</li>
 *   <li>{@code ais.action.report.format1.sekolah.LaporanCatatanKelasSiswa} dan
 *   {@code ais.action.report.format1.sekolah.LaporanRaporSiswa} — jalur cetak (rapor).</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota kelas ini</h2>
 * <ul>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@code onUpdate()}.</li>
 *   <li><b>Identitas &amp; label:</b> {@link #getId()}/{@link #setId(Long)},
 *   {@link #getNama()}/{@link #setNama(String)},
 *   {@link #getKeterangan()}/{@link #setKeterangan(String)}, {@link #toString()}.</li>
 *   <li><b>Bendera perilaku:</b> {@link #getDefaultData()}/{@link #setDefaultData(Boolean)},
 *   {@link #getAktif()}/{@link #setAktif(Boolean)},
 *   {@link #getNomorUrut()}/{@link #setNomorUrut(Integer)}, {@link #compareTo(GeneralValueObject)}.</li>
 *   <li><b>Cakupan multi-tenant:</b> {@link #getYayasan()}/{@link #setYayasan(Yayasan)},
 *   {@link #getSekolah()}/{@link #setSekolah(Sekolah)}.</li>
 *   <li><b>Auto-seed:</b> {@link #checkCreateDefault()}.</li>
 * </ul>
 *
 * <h2>Catatan penting soal kelas induk</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu pun properti induknya.
 * Karena itu deklarasi ULANG {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah}
 * di kelas ini <b>bukan bug atau duplikasi ceroboh</b>, melainkan keharusan teknis agar keempat
 * kolom itu ikut terpetakan. Efek sampingnya: field bernama sama di induk (mis. {@code nomorUrut},
 * {@code nama}, {@code keterangan} milik induk) tetap ada dan selalu bernilai {@code null} pada
 * instance kelas ini — relevan saat membaca perilaku
 * {@link GeneralValueObject#compareTo(GeneralValueObject)} versi induk.</p>
 *
 * <p><b>Kuirk yang sudah terverifikasi dan sengaja TIDAK diperbaiki di sini</b> (lihat Javadoc
 * masing-masing method): {@link #getKeterangan()} membalik kontrak non-null kelas induk;
 * {@link #compareTo(GeneralValueObject)} dipangkas jadi satu baris tanpa fallback sehingga
 * {@code TreeSet} yang menampung entity ini dapat <i>menciutkan</i> baris secara senyap;
 * {@link #checkCreateDefault()} memakai session native dan menutupnya di tengah konteks ZK.</p>
 *
 * <p>Komentar generator "Bank generated by hbm2java" pada header aslinya adalah artefak
 * salin-tempel dari {@code ais.database.model.Bank} yang tersebar ke puluhan entity AIS; entity ini
 * tidak punya hubungan apa pun dengan bank.</p>
 *
 * @see GeneralValueObject
 * @see CatatanKelasSiswa
 * @see JenisCatatanKelasSiswa
 * @see ParameterTambahanCatatanKelasSiswa
 * @see ais.database.model.ParameterTambahan
 * @see ais.database.model.KelompokParameterTambahanCatatanMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "kelompok_parameter_tambahan_catatan_kelas_siswa")
public class KelompokParameterTambahanCatatanKelasSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya <b>sama persis</b> dengan yang dipakai
	 * {@link CatatanKelasSiswa}, {@link JenisCatatanKelasSiswa}, dan
	 * {@link ParameterTambahanCatatanKelasSiswa} — jejak salin-tempel antar entity modul sekolah,
	 * bukan nilai hasil perhitungan. Jangan diubah: entity ini disimpan ZK ke dalam state
	 * desktop/session.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code IDENTITY}; dipetakan lewat {@link #getId()}. */
	private Long id;
	/** Nama tampil pengguna terakhir yang mengubah baris; dipetakan lewat {@link #getOleh()}. */
	private String oleh;
	/** Id/username pengguna terakhir yang mengubah baris; dipetakan lewat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id/username pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila baris belum pernah disentuh
	 *         interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>penolakan senyap</b>: nilai {@code null}
	 * maupun string kosong/whitespace diabaikan sepenuhnya sehingga nilai lama tetap dipertahankan.
	 *
	 * <p>Tujuannya menjaga jejak audit agar tidak terhapus oleh pemanggil yang menyalin properti
	 * secara borongan (mis. {@code BeanUtils.copyProperties}) dari object kosong.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan <b>penolakan senyap</b> yang sama seperti
	 * {@link #setOlehId(String)}: {@code null} atau string kosong/whitespace diabaikan.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pengisian jejak audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate
	 * menerbitkan {@code UPDATE}.
	 *
	 * <p><b>Efek samping:</b> memutasi state instance ini. Tidak dipanggil pada {@code INSERT}
	 * (hanya {@code @PreUpdate}), sehingga baris hasil {@link #checkCreateDefault()} lahir tanpa
	 * jejak {@code oleh}.</p>
	 *
	 * <p><b>Perhatian format:</b> baris sumber ini juga memuat deklarasi field
	 * {@code tanggal_dirubah} yang diinisialisasi ke waktu server saat instance dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) — konsekuensinya, entity yang baru dibuat sudah
	 * memiliki stempel waktu meski belum pernah disimpan. Penggabungan method + field dalam satu
	 * baris adalah pola penyisipan otomatis yang dipakai di seluruh entity AIS; jangan dirapikan
	 * tanpa alasan kuat karena banyak perkakas repo mencocokkannya secara harfiah.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; umumnya dipanggil interceptor
	 * audit, bukan kode aplikasi.
	 *
	 * @param tanggal_dirubah stempel waktu baru; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (kolom {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang dibuat lewat
	 *         konstruktor karena field-nya sudah diinisialisasi ke waktu server
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas berformat {@code "<id>-<nama>"}.
	 *
	 * <p><b>Kuirk:</b> membaca field {@code nama} <b>langsung</b>, bukan lewat {@link #getNama()},
	 * sehingga hasilnya tidak ter-{@code trim} dan bisa berbunyi {@code "null-null"} untuk instance
	 * baru. Dipakai antara lain oleh keluaran debug
	 * {@code JenisCatatanKelasSiswaAction.initKelompokParameterTambahanCatatanKelasSiswa()}.</p>
	 *
	 * @return string {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kelompok/seksi sebagaimana tampil di formulir; wajib isi &amp; unik (dijaga di lapis Action). */
	private String nama;
	/** Keterangan bebas; boleh {@code null} — lihat kuirk pada {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda baris bawaan hasil {@link #checkCreateDefault()}; baris ber-{@code true} tidak boleh dihapus lewat UI. */
	private Boolean defaultData;
	/** Bendera aktif; kelompok non-aktif disaring keluar dari perakitan formulir. */
	private Boolean aktif;
	/** Urutan tampil seksi di formulir; satu-satunya kunci {@link #compareTo(GeneralValueObject)}. */
	private Integer nomorUrut;
	/** Cakupan sekolah ({@code null} = berlaku untuk semua sekolah). */
	private Sekolah sekolah;
	/** Cakupan yayasan ({@code null} = berlaku untuk semua yayasan); diturunkan dari {@link #sekolah} bila ada. */
	private Yayasan yayasan;

	/**
	 * Memastikan minimal ada satu kelompok bawaan di database, dan membuatnya bila belum ada
	 * (mekanisme <b>auto-seed</b>).
	 *
	 * <p><b>Alur:</b> mencari satu baris dengan {@code defaultData = true}. Bila tidak ditemukan,
	 * membuat baris baru dengan {@code defaultData = true}, {@code nama = "Form Tambahan"}, dan
	 * {@code keterangan = "Form Tambahan"}, lalu menyimpannya dalam transaksi tersendiri
	 * ({@code begin()}/{@code commit()}). Baris hasil seed sengaja <b>tidak</b> diberi
	 * {@code sekolah}/{@code yayasan}, sehingga cakupannya "Semua" dan terlihat oleh seluruh
	 * tenant.</p>
	 *
	 * <p><b>Pemanggil nyata (satu-satunya, sudah diverifikasi):</b>
	 * {@code ais.action.master.sekolah.ParameterTambahanCatatanKelasSiswaAction.doAfterCompose()} —
	 * artinya seed berjalan setiap kali layar "Parameter Tambahan Catatan Kelas Siswa" dibuka,
	 * bukan lewat migrasi/instalasi. Layar master kelompok ini sendiri
	 * ({@code KelompokParameterTambahanCatatanKelasSiswaAction}) <b>tidak</b> memanggilnya, jadi
	 * berbeda dari varian Alumni yang punya dua mekanisme seed bersaing.</p>
	 *
	 * <p><b>Efek samping yang perlu diketahui:</b></p>
	 * <ul>
	 *   <li>Menulis ke database (INSERT + revisi Envers) pada operasi yang secara kasat mata hanya
	 *   "membuka layar".</li>
	 *   <li>Memakai {@link HibernateUtil#currentNativeSession()} lalu menutupnya lewat
	 *   {@link HibernateUtil#closeSession()} — padahal Javadoc {@code currentNativeSession()}
	 *   secara eksplisit meminta method itu <b>tidak</b> dipakai di konteks request ZK. Session
	 *   ThreadLocal request yang sedang berjalan ikut ditutup di tengah {@code doAfterCompose},
	 *   dan pemanggilan berikutnya harus membuka session baru.</li>
	 *   <li>Object yang dikembalikan berstatus <b>detached</b> (session-nya sudah ditutup);
	 *   relasi lazy padanya tidak bisa diinisialisasi tanpa {@code check()}/re-attach. Pemanggil
	 *   satu-satunya memang mengabaikan nilai kembalian.</li>
	 * </ul>
	 *
	 * @return baris kelompok bawaan — yang sudah ada, atau yang baru saja dibuat; tidak pernah
	 *         {@code null} pada jalur normal
	 */
	public static KelompokParameterTambahanCatatanKelasSiswa checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanCatatanKelasSiswa kelompokParameterTambahanCatatanSiswa = (KelompokParameterTambahanCatatanKelasSiswa) session
				.createCriteria(KelompokParameterTambahanCatatanKelasSiswa.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanCatatanSiswa == null) {
			kelompokParameterTambahanCatatanSiswa = new KelompokParameterTambahanCatatanKelasSiswa();
			kelompokParameterTambahanCatatanSiswa.setDefaultData(true);
			kelompokParameterTambahanCatatanSiswa.setNama("Form Tambahan");
			kelompokParameterTambahanCatatanSiswa.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanCatatanSiswa);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanCatatanSiswa;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Tidak melakukan inisialisasi apa pun
	 * selain yang sudah melekat pada deklarasi field ({@code tanggal_dirubah} terisi waktu server).
	 */
	public KelompokParameterTambahanCatatanKelasSiswa() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code IDENTITY}). Id ini merangkap sebagai ruas pertama kunci gabungan
	 * {@code idKelompok + "->" + idParameter} yang dipakai untuk menyimpan nilai isian dan mencari
	 * lampiran — lihat Javadoc kelas.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; normalnya hanya dipanggil Hibernate.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kelompok, sudah di-{@code trim}.
	 *
	 * <p>Nama inilah yang dirender sebagai judul seksi di formulir Catatan Kelas Siswa, dan juga
	 * ikut ditulis ke kolom denormalisasi berlabel {@code CatatanKelasSiswa.parameterTambahan}
	 * dalam bentuk {@code "<namaKelompok>-><labelInputan>"}. <b>Konsekuensi:</b> mengganti nama
	 * kelompok membuat data lama yang sudah tersimpan tidak lagi cocok dengan label barunya
	 * (kolom berlabel bersifat snapshot, tidak ikut diperbarui).</p>
	 *
	 * @return nama kelompok tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kelompok. Tanpa validasi dan <b>tanpa {@code trim}</b> — pemangkasan hanya
	 * terjadi saat pembacaan lewat {@link #getNama()}. Keunikan nama divalidasi di lapis Action
	 * ({@code checkNamaKelompokParameterTambahanCatatanKelasSiswa()}), bukan oleh constraint
	 * database.
	 *
	 * @param nama nama kelompok baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan kelompok apa adanya.
	 *
	 * <p><b>Kuirk penting — membalik kontrak kelas induk.</b>
	 * {@link GeneralValueObject#getKeterangan()} menjamin nilai kembalian tidak pernah
	 * {@code null} (mengembalikan {@code ""} bila kosong). Override di sini
	 * <b>menghapus jaminan itu</b> dan bisa mengembalikan {@code null}. Dampak nyata yang sudah
	 * diverifikasi: renderer grid layar master merender
	 * {@code new Label(...getKeterangan())} langsung, sehingga baris tanpa keterangan menampilkan
	 * sel kosong (ZK menerima {@code null}), dan pemanggil lain yang mengandalkan kontrak induk
	 * harus melakukan pengecekan null sendiri.</p>
	 *
	 * @return keterangan kelompok, <b>bisa {@code null}</b>
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan kelompok. Tanpa validasi; {@code null} diterima dan akan terbaca kembali
	 * sebagai {@code null} (lihat {@link #getKeterangan()}).
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda "baris bawaan sistem", dengan <b>normalisasi menulis ke field</b>:
	 * nilai {@code null} diganti {@code false} sebelum dikembalikan.
	 *
	 * <p>Karena entity ini dipetakan dengan property access dan {@code dynamicUpdate = true},
	 * penulisan ke field di dalam getter dapat ikut ter-{@code flush} sebagai {@code UPDATE} bila
	 * instance sedang persistent — pola yang dikenal di seluruh turunan
	 * {@link GeneralValueObject}. Di sini dampaknya jinak: {@code null} → {@code false} adalah
	 * nilai yang memang dimaksudkan.</p>
	 *
	 * <p><b>Pemakaian:</b> {@link #checkCreateDefault()} memakainya sebagai kriteria pencarian, dan
	 * renderer grid layar master memakai {@code !getDefaultData()} untuk menyembunyikan tombol
	 * Hapus pada baris bawaan.</p>
	 *
	 * @return {@code true} bila baris ini kelompok bawaan hasil auto-seed; tidak pernah {@code null}
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menyetel penanda baris bawaan. Tanpa validasi. Dalam praktik hanya dipanggil
	 * {@link #checkCreateDefault()}; layar master tidak menyediakan kontrol untuk mengubahnya,
	 * sehingga status "bawaan" praktis permanen sekali terbentuk.
	 *
	 * @param defaultData penanda baru
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan bendera aktif, dengan <b>normalisasi menulis ke field</b>: {@code null}
	 * diganti {@code true} (default "aktif") sebelum dikembalikan — berlaku peringatan write-back
	 * yang sama seperti pada {@link #getDefaultData()}.
	 *
	 * <p>Bendera ini benar-benar ditegakkan: query perakit formulir di
	 * {@code CatatanKelasSiswaAction} dan {@code LaporanRaporSiswa} menyaring
	 * {@code kelompokParameterTambahanCatatanKelasSiswa.aktif = true} bersama
	 * {@code parameterTambahan.aktif = true}, sehingga menonaktifkan kelompok akan menyembunyikan
	 * seluruh seksi beserta field di dalamnya — termasuk dari rapor.</p>
	 *
	 * @return {@code true} bila kelompok aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel bendera aktif. Tanpa validasi. Dipanggil dari checkbox "Aktif" di renderer grid
	 * layar master, yang menyimpan perubahan seketika lewat {@code Common.refreshSaveOrUpdate}.
	 *
	 * @param aktif bendera baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil seksi, dengan <b>normalisasi menulis ke field</b>:
	 * {@code null} diganti {@code 1}.
	 *
	 * <p><b>Kuirk:</b> baris {@code return} masih memuat pengecekan {@code nomorUrut == null}
	 * kedua yang <b>mustahil bernilai benar</b> karena blok {@code if} di atasnya sudah menjamin
	 * field terisi — kode mati yang tidak berbahaya, dibiarkan apa adanya.</p>
	 *
	 * <p><b>Dampak penting:</b> nilai bawaan {@code 1} berarti setiap kelompok yang belum pernah
	 * diatur nomor urutnya bernilai sama. Dipadukan dengan {@link #compareTo(GeneralValueObject)}
	 * yang hanya membandingkan nomor urut, seluruh kelompok "belum diatur" saling dianggap setara —
	 * lihat peringatan {@code TreeSet} di sana.</p>
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
	 * Menyetel nomor urut tampil. Tanpa validasi (nilai negatif atau duplikat diterima).
	 *
	 * <p>Satu-satunya jalur UI yang memanggilnya adalah {@code Intbox} kolom "Nomor Urut" di
	 * renderer grid layar master, yang langsung menyimpan lewat
	 * {@code Common.refreshSaveOrUpdate}. Formulir Tambah/Ubah <b>tidak</b> memuat isian nomor
	 * urut sama sekali, sehingga baris baru selalu lahir dengan {@code nomorUrut} kosong.</p>
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Urutan alami entity ini: <b>semata-mata berdasarkan {@link #getNomorUrut()}</b>.
	 *
	 * <p>Override ini <b>memangkas</b> logika berjenjang milik
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} (nomorUrut → nim → nama →
	 * keterangan). Tidak ada fallback ke {@code nama}, tidak ada penanganan exception, dan
	 * argumen di-cast langsung tanpa {@code instanceof} — membandingkan dengan entity bertipe lain
	 * melempar {@link ClassCastException}.</p>
	 *
	 * <p><b>Bug penciutan senyap {@code TreeSet} — TERVERIFIKASI ADA.</b> {@code compareTo} ini
	 * tidak konsisten dengan {@link GeneralValueObject#equals(Object)} (yang membandingkan id).
	 * Dua kelompok berbeda dengan nomor urut sama menghasilkan {@code 0}, sehingga
	 * {@code TreeSet} menganggapnya duplikat dan <b>membuang salah satunya tanpa peringatan</b>.
	 * Ini bukan kasus langka melainkan kondisi <b>bawaan</b>: {@link #getNomorUrut()}
	 * mengembalikan {@code 1} untuk setiap baris yang belum diatur, sementara formulir Tambah/Ubah
	 * tidak menyediakan isian nomor urut. Titik yang terdampak (semua memakai
	 * {@code TreeSet}/relasi ber-{@code TreeSet}):</p>
	 * <ul>
	 *   <li>{@code JenisCatatanKelasSiswa.kelompokParameterTambahanCatatanKelasSiswas} — koleksi
	 *   {@code @ManyToMany} yang diinisialisasi sebagai {@code new TreeSet<>()}.</li>
	 *   <li>{@code CatatanKelasSiswaAction} dan {@code CatatanSiswaAction} — menyalin koleksi itu
	 *   ke {@code TreeSet} baru sebelum membangun formulir.</li>
	 *   <li>{@code LaporanRaporSiswa} — {@code TreeSet} serupa pada jalur cetak rapor.</li>
	 * </ul>
	 * <p>Akibatnya, dari beberapa kelompok yang dicentang pada satu jenis catatan, hanya
	 * <b>satu</b> yang muncul di formulir dan di rapor selama nomor urutnya belum dibedakan.
	 * ({@code LaporanCatatanKelasSiswa} melakukan iterasi langsung atas koleksi entity dan karena
	 * itu tidak terdampak dengan cara yang sama.)</p>
	 *
	 * @param arg0 entity pembanding; <b>harus</b> bertipe
	 *             {@code KelompokParameterTambahanCatatanKelasSiswa}
	 * @return hasil {@link Integer#compareTo(Integer)} atas nomor urut kedua entity
	 * @throws ClassCastException bila {@code arg0} bukan
	 *         {@code KelompokParameterTambahanCatatanKelasSiswa}
	 * @throws NullPointerException bila {@code arg0} bernilai {@code null}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanCatatanKelasSiswa) arg0).getNomorUrut());
	}

	/**
	 * Menyetel cakupan yayasan, dengan <b>normalisasi</b>: object yang {@code null} <i>atau</i>
	 * yang belum punya id (belum tersimpan) disimpan sebagai {@code null}, yang berarti
	 * "berlaku untuk semua yayasan".
	 *
	 * <p>Perilaku ini penting karena combobox ZK dapat mengirim instance kosong untuk pilihan
	 * "Semua"; tanpa normalisasi Hibernate akan mencoba menyimpan relasi ke baris yang tidak ada.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau object tanpa id berarti "semua yayasan"
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan yayasan pemilik kelompok ini, setelah resolusi proxy lazy lewat
	 * {@code check(...)}.
	 *
	 * <p><b>Kuirk — nilai turunan yang menimpa isi kolom.</b> Bila {@link #getSekolah()} tidak
	 * {@code null}, yayasan <b>selalu</b> diambil ulang dari sekolah tersebut
	 * ({@code getSekolah().getYayasan()}) dan menimpa field. Karena entity ini memakai property
	 * access dengan {@code dynamicUpdate = true}, penulisan itu bisa ikut ter-{@code flush} sebagai
	 * {@code UPDATE} (plus revisi Envers) hanya karena baris kebetulan dibaca dalam sesi aktif —
	 * pola write-back via getter yang sudah berulang kali ditemukan di seluruh turunan
	 * {@link GeneralValueObject}. Dalam kasus ini efeknya bersifat <i>self-healing</i>: nilai yang
	 * ditulis memang nilai yang konsisten dengan sekolahnya.</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila kelompok berlaku untuk semua yayasan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		if (getSekolah() != null) {
			yayasan = getSekolah().getYayasan();
		}
		return yayasan;
	}

	/**
	 * Mengembalikan sekolah pemilik kelompok ini, setelah resolusi proxy lazy lewat
	 * {@code check(...)}. Berbeda dari {@link #getYayasan()}, getter ini murni membaca — tidak ada
	 * nilai turunan yang ditulis balik.
	 *
	 * <p>Nilai {@code null} berarti kelompok berlaku lintas sekolah. Filter pencarian di layar
	 * master memang menyusun kriteria {@code OR(isNull("sekolah"), eq(...))}, sehingga kelompok
	 * bercakupan "Semua" selalu ikut terlihat oleh setiap sekolah — termasuk baris bawaan hasil
	 * {@link #checkCreateDefault()}.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila kelompok berlaku untuk semua sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menyetel cakupan sekolah, dengan <b>normalisasi</b> yang sama seperti
	 * {@link #setYayasan(Yayasan)}: object {@code null} atau yang belum punya id disimpan sebagai
	 * {@code null} ("berlaku untuk semua sekolah").
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau object tanpa id berarti "semua sekolah"
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}
}
