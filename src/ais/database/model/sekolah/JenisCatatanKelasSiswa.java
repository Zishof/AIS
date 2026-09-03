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
 * Master <b>jenis catatan kelas siswa</b> pada modul SEKOLAH — tabel
 * {@code sekolah.jenis_catatan_kelas_siswa}.
 *
 * <p>Entity ini adalah <b>lapis puncak</b> rantai konfigurasi field kustom Catatan Kelas Siswa.
 * Satu barisnya mewakili satu jenis/kategori catatan (mis. "Catatan Wali Kelas", "Catatan
 * Pembinaan") yang nantinya dipilih pengguna saat mengisi
 * {@link CatatanKelasSiswa}. Yang membuatnya menjadi "puncak" adalah relasi
 * {@code @ManyToMany} {@link #getKelompokParameterTambahanCatatanKelasSiswas()}: kelompok/seksi
 * field kustom hanya benar-benar muncul di formulir bila <b>dicentang</b> pada jenis catatan yang
 * sedang dipakai.</p>
 *
 * <p><b>Domain — catatan tingkat KELAS, bukan per siswa.</b> Meski namanya memuat kata "Siswa",
 * entity pemilik datanya ({@link CatatanKelasSiswa}) berelasi ke {@link KelasSiswa} (rombongan
 * belajar), bukan ke {@code Siswa} perorangan — lihat pembahasan lengkap pada Javadoc
 * {@link KelompokParameterTambahanCatatanKelasSiswa}. Jadi bacaan yang benar adalah
 * "Jenis [Catatan Kelas Siswa]", bukan "[Jenis Catatan Siswa] per kelas". Entity
 * {@code ais.database.model.sekolah.JenisCatatanSiswa} adalah kerabat <b>terpisah</b> untuk
 * catatan per siswa; keduanya kerap dipakai berdampingan di layar yang sama
 * ({@code CatatanSiswaAction}) sehingga mudah tertukar.</p>
 *
 * <h2>Posisi dalam rantai konfigurasi field kustom (4 lapis)</h2>
 * <ol>
 *   <li>{@link ais.database.model.ParameterTambahan} — definisi teknis satu field kustom (label,
 *   tipe isian, wajib/tidak, perlu lampiran, {@code nomorUrut}).</li>
 *   <li>{@link KelompokParameterTambahanCatatanKelasSiswa} — kategori/heading yang mengelompokkan
 *   field-field tersebut menjadi seksi pada formulir.</li>
 *   <li>{@link ParameterTambahanCatatanKelasSiswa} — tabel penghubung {@code ParameterTambahan}
 *   &times; kelompok; menentukan field mana masuk kelompok mana.</li>
 *   <li><b>Entity ini</b> — jenis catatan; memilih kelompok mana yang aktif untuk jenis tersebut
 *   lewat tabel gabung {@code sekolah.jenis_catatan_kelas_siswa_has_parameter}.</li>
 * </ol>
 *
 * <p>Nilai isian yang diketik pengguna <b>tidak</b> disimpan di sini maupun di lapis mana pun di
 * atas, melainkan didenormalisasi ke dua kolom {@code text} milik {@link CatatanKelasSiswa}
 * ({@code parameterTambahan} berlabel dan {@code parameterTambahanInds} ber-ID), dengan pemisah
 * {@code "\n"} antar baris dan {@code "<=>"} antar ruas, memakai kunci gabungan
 * {@code idKelompok + "->" + idParameter}.</p>
 *
 * <h2>Pembaca runtime yang sudah terverifikasi</h2>
 * <ul>
 *   <li>{@code ais.action.master.sekolah.JenisCatatanKelasSiswaAction} — layar master CRUD entity
 *   ini, sekaligus <b>satu-satunya</b> pemanggil
 *   {@link #setKelompokParameterTambahanCatatanKelasSiswas(Set)} (daftar centang kelompok).</li>
 *   <li>{@code ais.action.master.sekolah.CatatanKelasSiswaAction} — combobox pemilih jenis pada
 *   formulir isian, plus perakitan seksi field kustomnya.</li>
 *   <li>{@code ais.action.master.sekolah.CatatanSiswaAction} — layar catatan <i>per siswa</i>
 *   ikut mencari jenis catatan kelas <b>berdasarkan kesamaan nama</b> dengan jenis catatan siswa
 *   (lihat kuirk di {@link #getNama()}).</li>
 *   <li>{@code ais.action.report.format1.sekolah.LaporanCatatanKelasSiswa} dan
 *   {@code ais.action.report.format1.sekolah.LaporanRaporSiswa} — jalur cetak, termasuk rapor.</li>
 *   <li>{@code ais.action.master.catatan.DasbordCatatan} — hanya membaca {@link #getNama()}.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota kelas ini</h2>
 * <ul>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@code onUpdate()}.</li>
 *   <li><b>Identitas &amp; label:</b> {@link #getId()}/{@link #setId(Long)},
 *   {@link #getKode()}/{@link #setKode(String)}, {@link #getNama()}/{@link #setNama(String)},
 *   {@link #getKeterangan()}/{@link #setKeterangan(String)}, {@link #toString()}.</li>
 *   <li><b>Bendera perilaku:</b> {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 *   <li><b>Cakupan multi-tenant:</b> {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 *   {@link #getYayasan()}/{@link #setYayasan(Yayasan)}.</li>
 *   <li><b>Relasi konfigurasi:</b> {@link #getKelompokParameterTambahanCatatanKelasSiswas()},
 *   {@link #setKelompokParameterTambahanCatatanKelasSiswas(Set)}, serta cache statis
 *   {@link #mapParameters}.</li>
 *   <li><b>Konstruktor:</b> {@link #JenisCatatanKelasSiswa()}.</li>
 * </ul>
 *
 * <h2>Catatan penting soal kelas induk</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — ia
 * POJO abstrak biasa, sehingga Hibernate tidak memetakan satu pun properti induknya. Karena itu
 * deklarasi ULANG {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas
 * ini <b>bukan bug atau duplikasi ceroboh</b>, melainkan keharusan teknis agar keempat kolom itu
 * ikut terpetakan. Field bernama sama di induk ({@code nama}, {@code keterangan},
 * {@code nomorUrut}, {@code nim}) tetap ada dan selalu {@code null} pada instance kelas ini.</p>
 *
 * <p>Kelas ini <b>tidak</b> meng-override {@link GeneralValueObject#compareTo(GeneralValueObject)},
 * sehingga urutan alaminya tetap berjenjang. Karena {@code nomorUrut} dan {@code nim} milik induk
 * selalu {@code null}, kunci yang benar-benar terpakai adalah {@link #getNama()} (override kelas
 * ini, dipanggil secara virtual), lalu {@link #getKeterangan()} sebagai cadangan. Jadi pola
 * "{@code compareTo()} dipangkas jadi satu baris" yang ada di
 * {@link KelompokParameterTambahanCatatanKelasSiswa} <b>TIDAK</b> ada di sini.</p>
 *
 * <h2>Kuirk terverifikasi yang sengaja TIDAK diperbaiki di sini</h2>
 * <ul>
 *   <li>{@link #mapParameters} — cache {@code public static} berumur JVM, tak pernah dibersihkan,
 *   tidak thread-safe, dan <b>menimpa</b> hasil {@code session.refresh()}. Lihat Javadoc field-nya.</li>
 *   <li>{@link #getKelompokParameterTambahanCatatanKelasSiswas()} — getter dengan efek samping
 *   (menulis balik ke field dari cache statis), sekaligus mengembalikan koleksi terkelola apa
 *   adanya sehingga tombol <b>Batal</b> di layar master tidak membatalkan perubahan centang.</li>
 *   <li>{@link #getKeterangan()} — membalik kontrak non-null kelas induk (bisa mengembalikan
 *   {@code null}).</li>
 *   <li>{@link #getYayasan()} — nilai turunan yang ditulis balik ke field lewat getter.</li>
 *   <li>Field {@code kelompokParameterTambahanCatatanKelasSiswas} diinisialisasi
 *   {@code new TreeSet<>()} — prasyarat struktural bug penciutan senyap yang didokumentasikan di
 *   {@link KelompokParameterTambahanCatatanKelasSiswa#compareTo(GeneralValueObject)}. Lihat
 *   Javadoc field tersebut untuk analisis kapan penciutan benar-benar terjadi.</li>
 * </ul>
 *
 * <p>Komentar generator "Bank generated by hbm2java" pada header aslinya adalah artefak
 * salin-tempel dari {@code ais.database.model.Bank} yang tersebar ke puluhan entity AIS; entity ini
 * tidak punya hubungan apa pun dengan bank.</p>
 *
 * @see GeneralValueObject
 * @see CatatanKelasSiswa
 * @see KelompokParameterTambahanCatatanKelasSiswa
 * @see ParameterTambahanCatatanKelasSiswa
 * @see ais.database.model.ParameterTambahan
 * @see ais.database.model.JenisCatatanMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jenis_catatan_kelas_siswa")
public class JenisCatatanKelasSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya <b>sama persis</b> dengan yang dipakai
	 * {@link CatatanKelasSiswa}, {@link KelompokParameterTambahanCatatanKelasSiswa}, dan
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
	 * (hanya {@code @PreUpdate}), sehingga baris yang baru dibuat lahir tanpa jejak {@code oleh}
	 * sampai perubahan pertamanya.</p>
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
	 * sehingga hasilnya tidak ter-{@code trim} dan berbunyi {@code "null-null"} untuk instance yang
	 * baru dibuat. Terlihat antara lain pada keluaran {@code System.out.println} di
	 * {@code JenisCatatanKelasSiswaAction} saat pengguna mencentang kelompok.</p>
	 *
	 * @return string {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode singkat jenis catatan; dipetakan ke kolom {@code kode} (tanpa {@code @Column} eksplisit,
	 * jadi memakai penamaan bawaan). <b>Praktis mati:</b> formulir Tambah/Ubah maupun daftar
	 * properti impor/ekspor Excel di layar master tidak memuat kolom ini, sehingga nilainya selalu
	 * {@code null} pada data yang lahir lewat UI. Lihat {@link #getKode()}.
	 */
	private String kode;

	/** Nama jenis catatan sebagaimana tampil di grid dan combobox; wajib isi (divalidasi di lapis Action). */
	private String nama;
	/** Cakupan sekolah; wajib diisi lewat formulir layar master. */
	private Sekolah sekolah;
	/** Cakupan yayasan; selalu diturunkan ulang dari {@link #sekolah} saat dibaca — lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Keterangan bebas; boleh {@code null} — lihat kuirk pada {@link #getKeterangan()}. */
	private String keterangan;
	/** Bendera aktif; jenis non-aktif disaring keluar dari combobox pemilih jenis. */
	private Boolean aktif;

	/**
	 * Cache <b>{@code public static}</b> berumur JVM yang memetakan {@link #getId()} ke himpunan
	 * kelompok parameter tambahan yang tercentang untuk jenis tersebut.
	 *
	 * <p><b>Cara kerja:</b> {@link #setKelompokParameterTambahanCatatanKelasSiswas(Set)} menulis
	 * entri ke sini setiap kali dipanggil pada entity yang sudah punya id, dan
	 * {@link #getKelompokParameterTambahanCatatanKelasSiswas()} membacanya lebih dulu — bila ada,
	 * isi cache <b>menimpa</b> field instance. Tujuan aslinya adalah menjembatani perubahan centang
	 * antar instance entity yang berbeda di dalam satu layar ZK.</p>
	 *
	 * <p><b>Konsekuensi yang perlu diwaspadai (semua sudah diverifikasi dari kode):</b></p>
	 * <ul>
	 *   <li><b>Menetralkan {@code session.refresh()}.</b> {@code CatatanKelasSiswaAction},
	 *   {@code CatatanSiswaAction}, {@code LaporanCatatanKelasSiswa}, dan {@code LaporanRaporSiswa}
	 *   semuanya memanggil {@code session.refresh(jenis)} tepat sebelum membaca koleksi ini, justru
	 *   agar mendapat data terbaru dari database. Bila cache memuat entri untuk id tersebut,
	 *   seluruh kerja {@code refresh} itu <b>dibuang</b> dan yang dipakai adalah himpunan lama.</li>
	 *   <li><b>Tidak pernah dibersihkan.</b> Tidak ada satu pun pemanggilan {@code remove(...)}
	 *   maupun {@code clear()} di seluruh basis kode, dan tidak ada batas ukuran — entri (beserta
	 *   entity kelompok yang mungkin sudah <i>detached</i>) menumpuk sampai JVM di-restart.</li>
	 *   <li><b>Tidak thread-safe.</b> {@link HashMap} biasa yang ditulis dari thread event ZK mana
	 *   pun tanpa sinkronisasi; penulisan bersamaan berisiko merusak struktur internal map.</li>
	 *   <li><b>Berbagi lintas pengguna dan lintas tenant.</b> Isinya milik seluruh JVM, bukan per
	 *   session. Kunci {@code id} memang unik global sehingga barisnya sama, tetapi perubahan yang
	 *   dibuat admin satu sekolah langsung terlihat oleh semua pengguna lain sebelum tersimpan.</li>
	 * </ul>
	 *
	 * <p><b>Catatan:</b> berbeda dari kembarannya di {@code JenisCatatanSiswa} dan {@code AlurSop},
	 * cache milik kelas ini <b>tidak</b> diisi {@code ais.common.InitDataHelper} dan <b>tidak</b>
	 * punya penanganan khusus di {@code ais.common.ManajemenProperty} — satu-satunya penulisnya
	 * adalah setter di kelas ini.</p>
	 */
	public static Map<Long, Set<KelompokParameterTambahanCatatanKelasSiswa>> mapParameters = new HashMap<Long, Set<KelompokParameterTambahanCatatanKelasSiswa>>();

	/**
	 * Himpunan kelompok/seksi field kustom yang dicentang untuk jenis catatan ini.
	 *
	 * <p><b>Inisialisasi {@code TreeSet} — prasyarat bug penciutan senyap.</b>
	 * {@link KelompokParameterTambahanCatatanKelasSiswa#compareTo(GeneralValueObject)} hanya
	 * membandingkan {@code nomorUrut} dan mengembalikan {@code 0} untuk dua kelompok berbeda yang
	 * nomor urutnya sama — kondisi <b>bawaan</b>, karena nomor urut default bernilai {@code 1} dan
	 * formulir Tambah/Ubah kelompok tidak menyediakan isiannya. Setiap {@code TreeSet} yang
	 * menampung entity itu karena itu membuang anggota "duplikat" tanpa peringatan.</p>
	 *
	 * <p><b>Kapan penciutan benar-benar terjadi (hasil penelusuran):</b> pada instance
	 * <i>transient</i>/<i>detached</i> — mis. object hasil {@code new} atau hasil impor Excel —
	 * koleksi ini benar-benar sebuah {@code TreeSet} sehingga penambahan anggota sudah menciut sejak
	 * di memori. Pada instance terkelola, Hibernate mengganti koleksi ini dengan implementasi
	 * miliknya sendiri saat memuat relasi (pemetaan memakai {@code @OrderBy}, bukan {@code @Sort}),
	 * jadi isi yang dibaca dari database utuh. Penciutan tetap muncul di hilir karena
	 * <b>konsumen menyalin ulang koleksi ini ke {@code TreeSet} baru</b>:
	 * {@code CatatanKelasSiswaAction} dan {@code CatatanSiswaAction} melakukannya persis sebelum
	 * merakit formulir, sehingga dari beberapa kelompok yang dicentang hanya <b>satu</b> yang
	 * tampil. Jalur cetak {@code LaporanRaporSiswa} menyalin ke {@code ArrayList} dan
	 * {@code LaporanCatatanKelasSiswa} melakukan iterasi langsung — keduanya <b>tidak</b>
	 * terdampak.</p>
	 *
	 * @see #getKelompokParameterTambahanCatatanKelasSiswas()
	 * @see #mapParameters
	 */
	private Set<KelompokParameterTambahanCatatanKelasSiswa> kelompokParameterTambahanCatatanKelasSiswas = new TreeSet<KelompokParameterTambahanCatatanKelasSiswa>();

	/**
	 * Mengembalikan himpunan kelompok/seksi field kustom yang <b>dicentang</b> untuk jenis catatan
	 * ini — inilah mekanisme yang menentukan seksi mana muncul di formulir Catatan Kelas Siswa dan
	 * di rapor.
	 *
	 * <p><b>Pemetaan:</b> {@code @ManyToMany} lewat tabel gabung
	 * {@code sekolah.jenis_catatan_kelas_siswa_has_parameter} (kolom
	 * {@code jenis_catatan_kelas_siswa} &rarr; {@code parameter}), dengan
	 * {@code cascade = MERGE} saja — menyimpan jenis catatan <b>tidak</b> ikut membuat baris
	 * kelompok baru. {@code @OrderBy("nomorUrut asc, nama asc")} membuat database mengurutkan
	 * anggota koleksi; urutan itu ikut dipertahankan pada koleksi terkelola.</p>
	 *
	 * <p><b>Efek samping — getter yang menulis balik ke field.</b> Bila {@link #getId()} sudah
	 * terisi dan {@link #mapParameters} memuat entri untuk id tersebut, isi cache statis itu
	 * <b>menimpa</b> field instance ini sebelum dikembalikan. Karena entity dipetakan dengan
	 * property access dan {@code dynamicUpdate = true}, penggantian referensi koleksi pada instance
	 * persistent dapat memicu Hibernate menulis ulang isi tabel gabung. Lihat Javadoc
	 * {@link #mapParameters} untuk daftar konsekuensinya, termasuk fakta bahwa mekanisme ini
	 * menetralkan {@code session.refresh()} yang dipanggil hampir semua konsumen.</p>
	 *
	 * <p><b>Mengembalikan koleksi hidup, bukan salinan.</b>
	 * {@code JenisCatatanKelasSiswaAction.initKelompokParameterTambahanCatatanKelasSiswa()}
	 * menyimpan hasil method ini apa adanya ke field {@code selected...}, lalu setiap klik checkbox
	 * memanggil {@code add}/{@code remove} pada koleksi itu. Karena yang dipegang adalah koleksi
	 * terkelola milik entity (bukan salinan), <b>menekan tombol Batal tidak membatalkan perubahan
	 * centang</b>: mutasinya sudah melekat pada entity dan ikut ter-{@code flush}. Pola yang sama
	 * sudah ditemukan pada {@code PelanggaranDanHukuman}.</p>
	 *
	 * <p><b>Pembaca lain:</b> {@code CatatanKelasSiswaAction} (perakit formulir dan renderer grid),
	 * {@code CatatanSiswaAction}, {@code LaporanCatatanKelasSiswa}, dan {@code LaporanRaporSiswa}.
	 * Tiga di antaranya menyalin hasilnya ke {@code TreeSet} baru — lihat peringatan penciutan pada
	 * field {@code kelompokParameterTambahanCatatanKelasSiswas}.</p>
	 *
	 * @return himpunan kelompok yang tercentang; tidak pernah {@code null}, tetapi bisa kosong dan
	 *         bisa berasal dari {@link #mapParameters} alih-alih dari database
	 */
	@ManyToMany(targetEntity = KelompokParameterTambahanCatatanKelasSiswa.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nomorUrut asc, nama asc")
	@JoinTable(name = "jenis_catatan_kelas_siswa_has_parameter", schema = "sekolah", joinColumns = @JoinColumn(name = "jenis_catatan_kelas_siswa"), inverseJoinColumns = @JoinColumn(name = "parameter"))
	public Set<KelompokParameterTambahanCatatanKelasSiswa> getKelompokParameterTambahanCatatanKelasSiswas() {
		if (id != null) {
			Set<KelompokParameterTambahanCatatanKelasSiswa> temp = mapParameters.get(id);
			if (temp != null) {
				kelompokParameterTambahanCatatanKelasSiswas = temp;
			}
		}
		return kelompokParameterTambahanCatatanKelasSiswas;
	}

	/**
	 * Menyetel himpunan kelompok yang tercentang, <b>sekaligus menulis salinannya ke cache statis
	 * {@link #mapParameters}</b> bila {@link #getId()} sudah terisi.
	 *
	 * <p>Penulisan ke cache itulah efek samping yang paling penting: sejak saat itu, setiap
	 * pembacaan {@link #getKelompokParameterTambahanCatatanKelasSiswas()} pada instance mana pun
	 * dengan id yang sama — di request, session, bahkan layar yang berbeda — akan memakai himpunan
	 * ini dan mengabaikan isi database, sampai JVM di-restart. Yang disimpan adalah
	 * <b>referensi</b>, bukan salinan defensif, sehingga mutasi lanjutan pada himpunan yang
	 * diserahkan pemanggil juga langsung terlihat lewat cache.</p>
	 *
	 * <p><b>Pemanggil nyata (satu-satunya, sudah diverifikasi):</b>
	 * {@code JenisCatatanKelasSiswaAction.onSave()} — dipanggil tepat sebelum
	 * {@code Common.refreshSaveOrUpdate(...)} dengan himpunan yang selama ini dimutasi oleh
	 * checkbox. Karena himpunan itu sendiri diperoleh dari getter (koleksi terkelola), pemanggilan
	 * ini pada praktiknya menyetel field dengan nilai yang sudah dipegangnya sendiri; nilai
	 * tambahnya semata-mata pengisian {@link #mapParameters}.</p>
	 *
	 * @param kelompokParameterTambahanCatatanKelasSiswas himpunan kelompok baru; {@code null}
	 *        diterima apa adanya dan ikut tersimpan ke cache — getter akan mengembalikan
	 *        {@code null} karena penjaga {@code temp != null} di sana hanya melindungi dari entri
	 *        cache yang kosong, bukan dari field yang sudah terlanjur di-{@code null}-kan
	 */
	public void setKelompokParameterTambahanCatatanKelasSiswas(
			Set<KelompokParameterTambahanCatatanKelasSiswa> kelompokParameterTambahanCatatanKelasSiswas) {
		this.kelompokParameterTambahanCatatanKelasSiswas = kelompokParameterTambahanCatatanKelasSiswas;
		if (id != null) {
			mapParameters.put(id, kelompokParameterTambahanCatatanKelasSiswas);
		}
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Tidak melakukan inisialisasi apa pun
	 * selain yang sudah melekat pada deklarasi field: {@code tanggal_dirubah} terisi waktu server,
	 * dan {@code kelompokParameterTambahanCatatanKelasSiswas} berupa {@code TreeSet} kosong.
	 */
	public JenisCatatanKelasSiswa() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code IDENTITY}). Id ini juga menjadi kunci {@link #mapParameters} dan dipakai
	 * {@code LaporanRaporSiswa} sebagai bagian nama parameter laporan
	 * ({@code "catatanKelasSiswas_..._" + idJenis}).</p>
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
	 * Mengembalikan kode jenis catatan, sudah di-{@code trim} dan <b>dinormalisasi ke string
	 * kosong</b> bila belum diisi (tanpa menulis balik ke field).
	 *
	 * <p><b>Kuirk — kontraknya berbeda dari {@link #getNama()}</b> yang justru mengembalikan
	 * {@code null} untuk nilai kosong; dua getter bertetangga di kelas yang sama memakai konvensi
	 * yang berlawanan.</p>
	 *
	 * <p><b>Field praktis mati.</b> Layar master tidak menyediakan isian kode, dan daftar properti
	 * impor/ekspor Excel-nya ({@code {"id", "nama", "sekolah", "keterangan", "aktif"}}) juga tidak
	 * memuatnya. Satu-satunya pembaca yang ditemukan adalah
	 * {@code CatatanKelasSiswaAction}, yang menyusun label combobox dari
	 * {@code new String[] { "nama", "kode" }} — karena kode selalu kosong, label yang tampil
	 * praktis hanya berisi nama.</p>
	 *
	 * @return kode jenis catatan tanpa spasi tepi, atau {@code ""} bila belum diisi; tidak pernah
	 *         {@code null}
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode jenis catatan. Tanpa validasi dan tanpa {@code trim} — pemangkasan hanya terjadi
	 * saat pembacaan lewat {@link #getKode()}. Tidak ada pemanggil dari UI (lihat catatan pada
	 * getter-nya).
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis catatan, sudah di-{@code trim}.
	 *
	 * <p>Nama inilah yang tampil di grid layar master, di combobox pemilih jenis pada formulir
	 * Catatan Kelas Siswa, di dasbor catatan, dan sebagai parameter judul pada rapor.</p>
	 *
	 * <p><b>Kuirk penting — nama dipakai sebagai kunci pencocokan lintas entity.</b>
	 * {@code CatatanSiswaAction} mencari jenis catatan <i>kelas</i> yang namanya <b>persis sama</b>
	 * ({@code ilike} MatchMode.EXACT) dengan nama jenis catatan <i>siswa</i> yang sedang dipilih,
	 * lalu menempelkan seksi field kustom milik catatan kelas ke layar catatan siswa. Akibatnya,
	 * mengganti nama di salah satu dari dua master itu <b>memutus</b> penggabungan tersebut secara
	 * senyap, tanpa pesan kesalahan. Tidak ada foreign key maupun constraint yang menjaga pasangan
	 * nama ini.</p>
	 *
	 * <p>Nama juga menjadi kunci urut alami entity ini, karena
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} yang diwarisi memakai
	 * {@code nomorUrut} dan {@code nim} lebih dulu — keduanya selalu {@code null} di sini — lalu
	 * jatuh ke {@code getNama()}.</p>
	 *
	 * @return nama jenis catatan tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis catatan. Tanpa validasi dan <b>tanpa {@code trim}</b> — pemangkasan hanya
	 * terjadi saat pembacaan. Kewajiban isi diperiksa di
	 * {@code JenisCatatanKelasSiswaAction.onSave()}; tidak ada constraint keunikan di database,
	 * sehingga dua jenis bernama sama dapat hidup berdampingan dan membuat pencocokan nama di
	 * {@code CatatanSiswaAction} memilih salah satunya secara sembarang ({@code setMaxResults(1)}).
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan jenis catatan apa adanya.
	 *
	 * <p><b>Kuirk penting — membalik kontrak kelas induk.</b>
	 * {@link GeneralValueObject#getKeterangan()} menjamin nilai kembalian tidak pernah {@code null}
	 * (mengembalikan {@code ""} bila kosong). Override di sini <b>menghapus jaminan itu</b> dan
	 * bisa mengembalikan {@code null}. Pemanggil yang mengandalkan kontrak induk — termasuk cabang
	 * terakhir {@link GeneralValueObject#compareTo(GeneralValueObject)} — harus memeriksa
	 * {@code null} sendiri.</p>
	 *
	 * @return keterangan jenis catatan, <b>bisa {@code null}</b>
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan. Tanpa validasi; {@code null} diterima dan akan terbaca kembali sebagai
	 * {@code null} (lihat {@link #getKeterangan()}).
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan bendera aktif, dengan <b>normalisasi murni-baca</b>: {@code null} dilaporkan
	 * sebagai {@code true} (default "aktif") <b>tanpa</b> menulis balik ke field.
	 *
	 * <p>Perbedaan kecil tapi berarti dari {@link KelompokParameterTambahanCatatanKelasSiswa#getAktif()},
	 * yang menormalkan dengan cara menugaskan nilai ke field dan karena itu berisiko memicu
	 * {@code UPDATE} tak terduga. Getter di sini bebas dari efek samping tersebut.</p>
	 *
	 * <p><b>Penegakan:</b> combobox pemilih jenis di {@code CatatanKelasSiswaAction} dan di
	 * {@code LaporanCatatanKelasSiswa} menyaring {@code aktif = true}, sehingga menonaktifkan satu
	 * jenis menyembunyikannya dari formulir dan laporan. Perhatikan bahwa penyaringan itu bekerja
	 * di tingkat SQL atas <b>kolom</b>, bukan lewat getter ini: baris lama yang kolom
	 * {@code aktif}-nya masih {@code NULL} akan dianggap "tidak aktif" oleh
	 * {@code Restrictions.eq("aktif", true)} meskipun getter melaporkannya {@code true}.
	 * ({@code CatatanSiswaAction} memakai {@code OR(isNull, eq(true))} dan karena itu bebas dari
	 * ketidaksesuaian ini.)</p>
	 *
	 * @return {@code true} bila jenis catatan aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel bendera aktif. Tanpa validasi. Dipanggil dari checkbox "Aktif" pada renderer grid
	 * layar master maupun dari impor Excel.
	 *
	 * @param aktif bendera baru; {@code null} berarti "belum pernah diatur" dan akan dibaca sebagai
	 *        aktif oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	
	/**
	 * Mengembalikan sekolah pemilik jenis catatan ini, setelah resolusi proxy lazy lewat
	 * {@code check(...)} milik kelas induk (hasilnya ditugaskan kembali ke field — pola wajib agar
	 * proxy yang sudah ter-resolve tidak diambil ulang).
	 *
	 * <p>Kolom penghubungnya {@code sekolah_id} — perhatikan bedanya dengan
	 * {@link KelompokParameterTambahanCatatanKelasSiswa} yang memakai nama kolom {@code sekolah}
	 * tanpa akhiran; ketidakseragaman ini penting saat menulis SQL manual atau migrasi.</p>
	 *
	 * <p>Berbeda dari {@link #getYayasan()}, getter ini murni membaca — tidak ada nilai turunan
	 * yang ditulis balik.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila baris belum bercakupan sekolah (formulir
	 *         layar master mewajibkan isian ini, tetapi impor Excel dan data lama bisa saja
	 *         menyisakan {@code null})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel cakupan sekolah, dengan <b>normalisasi</b>: object yang {@code null} <i>atau</i> yang
	 * belum punya id (belum tersimpan) disimpan sebagai {@code null}.
	 *
	 * <p>Perilaku ini penting karena combobox ZK dapat mengirim instance kosong untuk pilihan
	 * pembuka; tanpa normalisasi Hibernate akan mencoba menyimpan relasi ke baris yang tidak ada.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau object tanpa id disimpan sebagai
	 *        {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik jenis catatan ini.
	 *
	 * <p><b>Kuirk — nilai turunan yang menimpa isi kolom.</b> Bila {@link #getSekolah()} tidak
	 * {@code null}, yayasan <b>selalu</b> diambil ulang dari sekolah tersebut dan menimpa field,
	 * berapa pun nilai yang tersimpan di kolom {@code yayasan_id}. Karena entity ini memakai
	 * property access dengan {@code dynamicUpdate = true}, penulisan itu bisa ikut ter-{@code flush}
	 * sebagai {@code UPDATE} (plus revisi Envers) hanya karena baris kebetulan dibaca di dalam sesi
	 * aktif — pola write-back via getter yang berulang di seluruh turunan
	 * {@link GeneralValueObject}. Efeknya di sini bersifat <i>self-healing</i>: nilai yang ditulis
	 * memang nilai yang konsisten dengan sekolahnya, sehingga pilihan yayasan pengguna di formulir
	 * praktis diabaikan bila sekolah sudah dipilih.</p>
	 *
	 * <p><b>Urutan operasi yang perlu diperhatikan:</b> {@code check(yayasan)} dijalankan
	 * <i>setelah</i> penurunan dari sekolah, sehingga proxy yang baru saja diambil dari
	 * {@code sekolah.getYayasan()} tetap ikut di-resolve.</p>
	 *
	 * @return yayasan pemilik — hasil turunan dari {@link #getSekolah()} bila sekolah terisi;
	 *         {@code null} bila keduanya kosong
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
	 * Menyetel cakupan yayasan, dengan <b>normalisasi</b> yang sama seperti
	 * {@link #setSekolah(Sekolah)}: object {@code null} atau yang belum punya id disimpan sebagai
	 * {@code null}.
	 *
	 * <p>Perlu diingat bahwa nilai yang disetel di sini dapat ditimpa kembali oleh
	 * {@link #getYayasan()} pada pembacaan berikutnya bila {@link #getSekolah()} terisi.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau object tanpa id disimpan sebagai
	 *        {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

}
