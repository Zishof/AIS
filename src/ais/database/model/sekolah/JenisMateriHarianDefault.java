package ais.database.model.sekolah;
// Berkas ini semula hasil pembangkitan Hibernate Tools 5.2.3.Final (hbm2java),
// lalu disunting manual. Blok komentar kelas di bawah sudah diganti dengan
// dokumentasi domain yang sesungguhnya; lihat catatan "Komentar generator" di
// Javadoc kelas.

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

import ais.database.model.GeneralValueObject;

/**
 * Master <b>Jenis Materi Harian Default</b> — daftar baku nama materi/pelajaran harian
 * yang dipakai untuk <i>mengisi otomatis</i> tabel <b>MATERI</b> pada Form Aktivitas
 * Harian Siswa (buku penghubung / catatan pembinaan harian siswa).
 *
 * <h2>Peran yang terverifikasi</h2>
 * <p>Entity ini <b>bukan</b> penyimpan nilai/penilaian siswa dan <b>bukan</b> mata pelajaran
 * kurikulum ({@code Matapelajaran} dipakai untuk itu). Perannya murni <b>template pengisian
 * awal</b>: sebuah katalog berisi baris-baris {@code nama} yang, ketika seorang guru/pembina
 * membuka formulir "Form Aktivitas Harian Siswa" untuk membuat catatan <b>baru</b>, dibaca
 * satu kali lalu dijadikan baris-baris kosong pada grid MATERI supaya guru tinggal mengisi
 * kolom "PENILAIAN" di sebelahnya.</p>
 *
 * <p>Jejak verifikasi (empat sumber independen):</p>
 * <ol>
 *   <li>{@code ais.action.master.sekolah.AktiftasHarianSiswaAction#init(GeneralValueObject)} —
 *       satu-satunya pembaca nyata. Untuk catatan baru ({@code currentObj.getId() == null})
 *       ia menjalankan {@code createCriteria(JenisMateriHarianDefault.class)} diurutkan
 *       {@code nomorUrut}, lalu {@code nama}, disaring {@code aktif = true}, dan memanggil
 *       {@code addRowMat(d.getNama(), "")} untuk setiap baris.</li>
 *   <li>{@code AktiftasHarianSiswaAction#initDefaultMasterData()} — penyemai bawaan yang
 *       menulis empat baris bila tabel masih kosong: <b>Tahfidz, Hadits, Bahasa Arab,
 *       Fiqih</b>. Isi seed ini menegaskan konteks sekolah/pesantren Islam.</li>
 *   <li>{@code ais.action.master.sekolah.JenisMateriHarianDefaultAction} + berkas
 *       {@code /pages/master/sekolah/jenis_materi_harian_default.zul} — layar master CRUD-nya,
 *       berjudul "Tambah/Ubah Jenis Materi Harian Default".</li>
 *   <li>{@code hibernate.cfg.xml} baris pemetaan {@code jenis_materi_harian_default}, dan
 *       {@code ais.common.InitData} yang mendaftarkannya pada {@code initClasses(...)}
 *       (pemanasan cache entity, bukan penyemaian data).</li>
 * </ol>
 *
 * <h2>Pasangan kembarnya: {@code JenisAktiftasHarianDefault}</h2>
 * <p>Kelas ini punya saudara kembar struktural {@code JenisAktiftasHarianDefault} (tabel
 * {@code sekolah.jenis_aktiftas_harian_default}) — bidang, anotasi, bahkan nilai
 * {@code serialVersionUID}-nya identik karena keduanya lahir dari salin-tempel yang sama.
 * Keduanya dibaca berdampingan di formulir yang sama, tetapi mengisi <b>dua grid berbeda</b>
 * dengan bentuk masukan yang berbeda:</p>
 * <ul>
 *   <li>{@code JenisAktiftasHarianDefault} → grid <b>AKTIVITAS</b>, kolom jawabnya sepasang
 *       radio <b>YA/TIDAK</b> (seed bawaan: Shalat Jamaah, Membaca Al-Quran, Membantu Orang
 *       Tua, Olahraga) — cocok untuk kebiasaan/ibadah yang dicentang terjadi atau tidak.</li>
 *   <li><b>kelas ini</b> → grid <b>MATERI</b>, kolom jawabnya {@code Textbox} bebas berlabel
 *       "PENILAIAN" — cocok untuk capaian materi yang perlu ditulis (mis. "Juz 30 hal. 4",
 *       "B+", "belum lancar").</li>
 * </ul>
 * <p>Karena itu jangan menyamakan keduanya: memindahkan sebuah nama dari satu katalog ke
 * katalog lain mengubah bentuk isian yang didapat guru di layar.</p>
 *
 * <h2>Hubungan ke catatan harian: snapshot teks, BUKAN foreign key</h2>
 * <p>Ini titik non-obvious paling penting. Baris master ini <b>tidak pernah dirujuk</b> oleh
 * {@code AktiftasHarianSiswa}. Yang terjadi hanyalah {@code nama} disalin sebagai <b>teks</b>
 * ke dalam dokumen JSON yang disimpan pada kolom komposit {@code AktiftasHarianSiswa.materi}
 * (bentuknya {@code {"1":{"nama":...,"nilai":...}, ...}}). Konsekuensinya:</p>
 * <ul>
 *   <li><b>Sekali pakai.</b> Katalog hanya dibaca saat catatan <i>baru</i> dibuat. Membuka
 *       kembali catatan lama membaca JSON, bukan tabel ini.</li>
 *   <li><b>Tidak retroaktif.</b> Mengganti nama, menonaktifkan, atau menghapus baris di sini
 *       sama sekali tidak mengubah catatan harian yang sudah tersimpan — nama lama tetap
 *       terbaca di riwayat, laporan, dan Buku Penghubung Siswa.</li>
 *   <li><b>Tidak ada integritas referensial.</b> Menghapus baris master aman dari sisi FK,
 *       tetapi juga berarti tak ada cara memetakan kembali catatan lama ke katalog.</li>
 *   <li><b>Guru boleh menyimpang.</b> Nama hasil isian awal tampil di {@code Textbox} yang
 *       bisa disunting, dan barisnya bisa dihapus atau ditambah manual. Katalog ini karena itu
 *       adalah <i>saran</i>, bukan aturan yang ditegakkan.</li>
 * </ul>
 *
 * <h2>Lingkup sekolah/yayasan</h2>
 * <p>{@link #getSekolah()} dan {@link #getYayasan()} boleh {@code null}, dan {@code null}
 * di sini bermakna <b>berlaku global</b>. Query pembaca di formulir aktivitas harian menyaring
 * dengan {@code Restrictions.or(isNull("sekolah"), eq("sekolah", siswa.getSekolah()))},
 * sehingga baris global ikut muncul untuk semua sekolah, sedangkan baris bersekolah hanya
 * muncul untuk sekolah tersebut. Empat baris seed bawaan ditulis <b>tanpa</b> sekolah/yayasan,
 * jadi seluruh instalasi awalnya berbagi katalog yang sama.</p>
 *
 * <h2>Kuirk dan jebakan yang sudah terverifikasi dari kode</h2>
 * <ul>
 *   <li><b>Baris baru buatan admin tidak pernah muncul di formulir.</b>
 *       {@code JenisMateriHarianDefaultAction#onSave(Event)} hanya menulis {@code nama},
 *       {@code sekolah}, {@code yayasan}, dan {@code keterangan} — kolom {@code aktif}
 *       <i>tidak pernah</i> disentuh, sehingga tersimpan {@code NULL}. Sementara itu pembaca
 *       menyaring dengan {@code Restrictions.eq("aktif", true)} yang dievaluasi di SQL dan
 *       <b>tidak cocok dengan NULL</b>. Karena {@link #getAktif()} menganggap {@code null}
 *       sebagai {@code true}, checkbox "Aktif" di grid master tetap tampak <b>tercentang</b> —
 *       admin tidak punya petunjuk apa pun bahwa barisnya mati. Cara tak sengaja untuk
 *       "memperbaikinya" adalah membuka centang lalu mencentangnya lagi di grid, karena
 *       listener {@code onCheck}-lah satu-satunya penulis kolom ini. Hanya empat baris seed
 *       (yang memanggil {@code setAktif(true)} eksplisit) yang lolos sejak awal. Pola divergensi
 *       "getter default vs filter SQL" ini sudah dikenal di proyek ini
 *       ({@code JenisCatatanSiswa}, {@code JenisNilaiSiswa}, {@code KurikulumPunyaMatapelajaran}).</li>
 *   <li><b>Menyunting baris bawaan diam-diam mempersempit jangkauannya.</b> Empat baris seed
 *       bersifat global ({@code sekolah = null}), tetapi formulir master <b>mewajibkan</b>
 *       Yayasan dan Sekolah diisi. Begitu admin membuka salah satu baris seed sekadar untuk
 *       memperbaiki ejaan lalu menyimpannya, baris itu berubah menjadi milik satu sekolah dan
 *       <b>lenyap dari formulir sekolah lain</b> pada instalasi multi-sekolah.</li>
 *   <li><b>Filter "Tampilkan hanya yang aktif" adalah hiasan.</b> Berkas {@code .zul}
 *       mendeklarasikan {@code <checkbox id="searchaktif" checked="true" ...>}, tetapi
 *       {@code JenisMateriHarianDefaultAction} tidak pernah mendeklarasikan field
 *       {@code searchaktif} dan {@code initCriteria(boolean)} tidak pernah membaca nilainya.
 *       Mencentang atau melepasnya tidak mengubah hasil daftar sama sekali.</li>
 *   <li><b>Hak akses diwarisi dari menu induk.</b> Tidak ada entri menu tersendiri untuk layar
 *       ini ({@code MenuInitializer}/{@code MenuSnapshotData} hanya mengenal
 *       {@code /pages/master/sekolah/aktiftas_harian_siswa.zul}). Layar master disisipkan
 *       sebagai tab lewat {@code MyInclude} dari {@code AktiftasHarianSiswaAction}, sehingga
 *       {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)} yang dievaluasi adalah
 *       hak menu <b>Aktifitas Harian Siswa</b>. Siapa pun yang boleh mengisi catatan harian
 *       otomatis juga boleh mengubah katalog master ini. Pola "pewarisan hak lewat menu induk"
 *       ini sudah tercatat sebelumnya pada {@code PaketPsb},
 *       {@code KategoriItemPenilaianSiswa}, dan {@code SubMatapelajaran}.</li>
 *   <li><b>Daftar master tidak tersaring tenant secara default.</b>
 *       {@code initCriteria(boolean)} memasang {@code Restrictions.sqlRestriction("1=1")}
 *       bila combo Yayasan/Sekolah pencarian tidak terpilih. Bagi pengguna yang tidak terikat
 *       sekolah/yayasan (mis. akun tanpa konteks sekolah aktif), daftar menampilkan katalog
 *       seluruh instalasi. Dampaknya rendah — isinya metadata katalog ("Tahfidz", "Fiqih"),
 *       bukan data pribadi — tetapi mekanismenya sama dengan pola fail-open cakupan tenant
 *       yang sudah dikenal di modul sekolah.</li>
 * </ul>
 *
 * <h2>Komentar generator yang salah salin</h2>
 * <p>Sampai revisi sebelum dokumentasi ini, blok Javadoc kelas berbunyi
 * <i>"JenisGuru generated by hbm2java"</i>. Itu <b>keliru</b>: teks tersebut milik
 * {@code ais.database.model.sekolah.JenisGuru} dan tersebar ke belasan berkas lain lewat
 * salin-tempel (didokumentasikan saat {@code JenisGuru} dikerjakan). Berkas ini tidak pernah
 * berhubungan dengan entity guru. Komentar tersebut kini diganti dengan keterangan yang benar,
 * dan asal-usul hbm2java-nya dipindahkan ke komentar di bawah deklarasi {@code package}
 * supaya jejak sejarahnya tidak hilang.</p>
 *
 * <h2>Catatan pemetaan</h2>
 * <ul>
 *   <li>{@code @org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)}
 *       — anotasi lama yang sudah usang di Hibernate modern, tetapi masih dipakai seragam di
 *       seluruh model AIS. {@code dynamicInsert} membuat kolom bernilai {@code null} (termasuk
 *       {@code aktif} dan {@code nomorUrut}) tidak ikut disertakan pada {@code INSERT}.</li>
 *   <li>{@link org.hibernate.envers.Audited} — setiap perubahan direkam Envers; layar master
 *       menampilkan riwayatnya lewat {@code RevisiHelper.createNewRevisi(...)}.</li>
 *   <li>{@code nomorUrut} dan {@code aktif} <b>tidak</b> memakai {@code @Column}, jadi nama
 *       kolomnya mengikuti strategi penamaan bawaan Hibernate (nama properti apa adanya).
 *       Kolomnya dibuat oleh {@code hbm2ddl.auto=update}, tanpa nilai bawaan di sisi basis
 *       data — inilah sebab {@code aktif} bisa berisi {@code NULL} seperti dijelaskan di
 *       atas.</li>
 * </ul>
 *
 * <h2>Pewarisan dari {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}
 * — ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan properti apa pun miliknya.
 * Karena itu {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>wajib</b>
 * dideklarasikan ulang di kelas ini. Pengulangan tersebut <b>bukan bug dan bukan kelalaian</b>,
 * melainkan keharusan teknis; menghapusnya akan membuat kolom-kolom itu hilang dari tabel.
 * Yang benar-benar diwarisi adalah perilaku, terutama {@code check(...)} untuk resolusi proxy
 * lazy yang dipakai {@link #getSekolah()} dan {@link #getYayasan()}.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}, {@code onUpdate()}.</li>
 *   <li><b>Identitas</b> — {@link #getId()}, {@link #setId(Long)}, dua konstruktor.</li>
 *   <li><b>Cakupan tenant</b> — {@link #getSekolah()}, {@link #setSekolah(Sekolah)},
 *       {@link #getYayasan()}, {@link #setYayasan(Yayasan)}.</li>
 *   <li><b>Isi katalog</b> — {@link #getNama()}, {@link #setNama(String)},
 *       {@link #getKeterangan()}, {@link #setKeterangan(String)}.</li>
 *   <li><b>Kendali tampil</b> — {@link #getAktif()}, {@link #setAktif(Boolean)},
 *       {@link #getNomorUrut()}, {@link #setNomorUrut(Integer)}.</li>
 * </ol>
 *
 * <p>Kelas ini tidak memiliki method bisnis, kalkulasi, maupun query statis — seluruh logika
 * seleksi dan pengurutan berada di sisi pemanggil.</p>
 *
 * @see ais.action.master.sekolah.JenisMateriHarianDefaultAction
 * @see ais.action.master.sekolah.AktiftasHarianSiswaAction
 * @see AktiftasHarianSiswa
 * @see JenisAktiftasHarianDefault
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "jenis_materi_harian_default", schema = "sekolah")
public class JenisMateriHarianDefault extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya kebetulan <b>identik</b> dengan
	 * {@code JenisAktiftasHarianDefault} karena kedua kelas lahir dari salin-tempel yang sama;
	 * hal itu tidak menimbulkan masalah (identitas serialisasi ditentukan per kelas, bukan per
	 * nilai) tetapi berguna sebagai penanda asal-usul kedua berkas.
	 */
	private static final long serialVersionUID = -7490758846785025664L;

	/**
	 * Kunci utama, dipetakan ke kolom {@code id} dan diisi basis data ({@code IDENTITY}).
	 * Dideklarasikan ulang di sini karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh interseptor audit.
	 * Dideklarasikan ulang karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private String oleh;

	/**
	 * Id pengguna terakhir yang mengubah baris ini (pasangan teknis dari {@link #oleh});
	 * diisi otomatis oleh interseptor audit.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> setter ini <b>menolak secara diam-diam</b> nilai {@code null}
	 * maupun teks kosong/spasi — nilai lama dipertahankan. Akibatnya jejak audit tidak pernah
	 * bisa dikosongkan lewat setter ini, yang memang disengaja agar penyimpanan ulang tanpa
	 * konteks pengguna tidak menghapus jejak yang sudah ada.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null} atau blanko diabaikan.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau teks kosong/spasi
	 * <b>diabaikan diam-diam</b> sehingga jejak audit lama tidak tertimpa nilai hampa.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null} atau blanko diabaikan.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum {@code UPDATE}.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #tanggal_dirubah} serta {@link #oleh}/{@link #olehId} dari konteks pengguna aktif.
	 * Tidak dipanggil pada {@code INSERT} — untuk baris baru {@link #tanggal_dirubah} sudah
	 * terisi sejak inisialisasi field.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah state object ini (stempel waktu dan identitas pengubah)
	 * di dalam siklus flush Hibernate. Jangan dipanggil manual dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat object dibuat
	 * ({@code WaktuUtil.getDate()}) sehingga baris baru pun selalu punya nilai, lalu diperbarui
	 * oleh {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis oleh interseptor audit lewat {@link #onUpdate()}; pemanggilan
	 * manual hanya wajar pada proses migrasi/impor data.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang dibuat
	 *         lewat konstruktor kelas ini, kecuali sengaja ditimpa {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Sekolah pemilik baris katalog ini. {@code null} berarti <b>berlaku global</b> untuk
	 * semua sekolah — lihat bagian "Lingkup sekolah/yayasan" pada Javadoc kelas.
	 */
	private Sekolah sekolah;

	/**
	 * Yayasan pemilik baris katalog ini. Diturunkan otomatis dari {@link #sekolah} setiap kali
	 * {@link #getYayasan()} dipanggil; {@code null} berarti berlaku global.
	 */
	private Yayasan yayasan;

	/**
	 * Keterangan bebas untuk admin (mis. cakupan materi, jenjang yang dituju). Hanya tampil di
	 * layar master; tidak pernah ikut disalin ke catatan harian siswa.
	 */
	private String keterangan;

	/**
	 * Nama materi harian — satu-satunya field yang benar-benar dipakai hilir. Nilainya disalin
	 * sebagai teks ke grid MATERI formulir aktivitas harian dan, setelah disimpan, membeku di
	 * dalam JSON kolom {@code AktiftasHarianSiswa.materi}.
	 */
	private String nama;

	/**
	 * Urutan tampil baris pada grid MATERI. Dipakai sebagai kunci pengurutan pertama oleh
	 * pembaca ({@code addOrder(Order.asc("nomorUrut"))}), dengan {@code nama} sebagai pemecah
	 * seri. Boleh {@code null} di basis data — lihat {@link #getNomorUrut()}.
	 */
	private Integer nomorUrut;

	/**
	 * Saklar aktif/tidak. Hanya baris {@code aktif = true} <b>di basis data</b> yang ikut mengisi
	 * formulir. Boleh {@code null}, dan justru itulah sumber jebakan yang diuraikan pada Javadoc
	 * kelas: baris yang dibuat dari layar master tidak pernah menulis kolom ini.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA, sekaligus dipakai layar master
	 * ({@code onAdd}) dan penyemai bawaan untuk membuat baris kosong.
	 */
	public JenisMateriHarianDefault() {
	}

	/**
	 * Konstruktor ringkas untuk membentuk object dengan kunci dan nama sekaligus.
	 *
	 * <p>Peninggalan pembangkit hbm2java (mewakili kolom-kolom {@code NOT NULL}); tidak ada
	 * pemanggil di basis kode saat ini. Perhatikan bahwa parameter {@code id} bertipe primitif
	 * {@code long} sehingga tidak bisa dipakai membuat baris baru yang id-nya belum ada.</p>
	 *
	 * @param id kunci utama yang sudah diketahui.
	 * @param nama nama materi harian.
	 */
	public JenisMateriHarianDefault(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolomnya ditandai {@code insertable = false} karena nilainya dihasilkan basis data
	 * ({@code IDENTITY}). Layar master memakai {@code getId() == null} sebagai penanda
	 * "tambah" versus "ubah".</p>
	 *
	 * @return kunci utama, atau {@code null} untuk object yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Umumnya hanya dipakai Hibernate; kode aplikasi memuat baris lewat
	 * {@code session.load(...)} alih-alih menyusun id sendiri.
	 *
	 * @param id kunci utama.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik baris katalog ini, {@code null} bila baris berlaku global.
	 *
	 * <p><b>Getter dengan efek samping (pola yang dikenal di basis kode ini).</b> Nilai field
	 * dilewatkan {@code check(...)} milik {@link GeneralValueObject} lalu <b>ditulis kembali</b>
	 * ke field. Tujuannya meresolusi proxy lazy Hibernate agar object tetap terbaca meski
	 * session asalnya sudah tertutup (misalnya ketika grid dirender di luar transaksi).
	 * Efeknya bersifat menyembuhkan, bukan merusak: tidak ada data yang ditimpa nilai lain —
	 * berbeda dari {@link #getYayasan()} di bawah.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila katalog ini berlaku untuk semua sekolah.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik baris katalog ini.
	 *
	 * <p><b>Normalisasi diam-diam:</b> object {@link Sekolah} yang {@code null} <i>atau</i> yang
	 * id-nya masih {@code null} (mis. pilihan "=sekolah=" pada combo) diperlakukan sama, yaitu
	 * disimpan sebagai {@code null} — sehingga baris menjadi berlaku global. Dengan begitu
	 * object transien tanpa kunci tidak pernah bocor ke {@code INSERT} dan memicu galat FK.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau object tanpa id berarti berlaku global.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris katalog ini, {@code null} bila berlaku global.
	 *
	 * <p><b>Getter destruktif — perhatikan baik-baik.</b> Method ini tidak sekadar membaca:
	 * ia memanggil {@link #getSekolah()} lebih dulu dan, bila sekolah terisi, <b>menimpa</b>
	 * field {@link #yayasan} dengan {@code sekolah.getYayasan()}. Artinya yayasan yang pernah
	 * disetel manual lewat {@link #setYayasan(Yayasan)} akan hilang begitu getter ini dipanggil
	 * pada object yang punya sekolah. Perilaku ini <b>disengaja</b> sebagai penjaga konsistensi:
	 * yayasan selalu mengikuti sekolah, tidak pernah bertentangan dengannya — dan sejalan dengan
	 * layar master yang mengunci combo Yayasan ({@code setReadonly(true)}) agar admin tidak bisa
	 * memasangkan sekolah dengan yayasan yang bukan induknya.</p>
	 *
	 * <p><b>Efek samping tambahan:</b> karena field ditulis ulang, pemanggilan getter ini di
	 * dalam session yang aktif dapat memunculkan revisi Envers "palsu" bila nilai lama di basis
	 * data berbeda dan object ikut ter-flush. Setelahnya hasil masih dilewatkan {@code check(...)}
	 * untuk meresolusi proxy lazi, sama seperti {@link #getSekolah()}.</p>
	 *
	 * @return yayasan pemilik — diturunkan dari sekolah bila sekolah terisi, {@code null} bila
	 *         katalog berlaku global.
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
	 * Menetapkan yayasan pemilik baris katalog ini.
	 *
	 * <p>Menerapkan normalisasi yang sama dengan {@link #setSekolah(Sekolah)}: {@code null}
	 * maupun object tanpa id disimpan sebagai {@code null}. Ingat bahwa nilai yang disetel di
	 * sini <b>tidak bertahan</b> bila {@link #sekolah} terisi — {@link #getYayasan()} akan
	 * menurunkannya ulang dari sekolah.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau object tanpa id berarti berlaku global.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan keterangan bebas baris katalog ini.
	 *
	 * <p>Getter murni tanpa efek samping — <b>tidak</b> mengikuti pola "getKeterangan() membalik
	 * kontrak" yang ditemukan pada sebagian entity lain di basis kode ini (di sana getter
	 * bernama sama justru menyusun teks turunan atau menulis balik field). Di kelas ini nilainya
	 * dikembalikan apa adanya.</p>
	 *
	 * <p>Hanya ditampilkan sebagai kolom {@code Label} pada grid layar master dan disunting lewat
	 * {@code Textbox} tiga baris pada formulirnya; tidak pernah ikut ke catatan harian siswa.</p>
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris katalog ini.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama materi harian.
	 *
	 * <p>Inilah satu-satunya nilai yang mengalir ke hilir: dipakai
	 * {@code AktiftasHarianSiswaAction} sebagai isi awal kolom "MATERI", dan dipakai
	 * {@code RevisiHelper} sebagai judul entri riwayat Envers pada layar master.</p>
	 *
	 * @return nama materi; kolomnya {@code NOT NULL} sehingga baris tersimpan selalu punya nilai.
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama materi harian.
	 *
	 * <p>Kolomnya {@code NOT NULL}; layar master menegakkan hal ini lewat validasi "Nama harus
	 * diisi" sebelum menyimpan. Tidak ada batasan keunikan — dua baris bernama sama bisa
	 * berdampingan dan akan muncul dua kali di formulir.</p>
	 *
	 * @param nama nama materi harian.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan saklar aktif, dengan {@code null} <b>dianggap aktif</b>.
	 *
	 * <p><b>Peringatan penting.</b> Nilai bawaan {@code true} untuk {@code null} ini hanya
	 * berlaku di sisi Java. Pembaca sesungguhnya ({@code AktiftasHarianSiswaAction}) menyaring
	 * di sisi SQL dengan {@code Restrictions.eq("aktif", true)}, dan SQL <b>tidak</b>
	 * memperlakukan {@code NULL} sebagai {@code true}. Akibatnya baris dengan kolom
	 * {@code aktif} berisi {@code NULL} tampak <b>tercentang</b> pada grid layar master
	 * (karena grid memanggil getter ini) tetapi <b>tidak pernah</b> muncul di formulir aktivitas
	 * harian. Karena {@code onSave} layar master tidak pernah menulis kolom ini, seluruh baris
	 * yang dibuat admin lewat tombol "Tambah" berada dalam kondisi tersebut. Lihat uraian
	 * lengkap pada Javadoc kelas.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} bila sengaja
	 *         dinonaktifkan.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan saklar aktif.
	 *
	 * <p>Satu-satunya penulis di alur normal adalah listener {@code onCheck} pada checkbox
	 * "Aktif" di grid layar master, yang langsung menyimpan lewat
	 * {@code Common.refreshSaveOrUpdate(...)} tanpa membuka formulir. Penyemai bawaan
	 * {@code initDefaultMasterData()} juga memanggilnya dengan {@code true} secara eksplisit.</p>
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk menyembunyikan baris
	 *        dari formulir aktivitas harian; {@code null} sebaiknya dihindari karena akan
	 *        menghasilkan perilaku "tampak aktif tapi tidak pernah terpakai".
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil, dengan {@code null} dipetakan ke {@code 1}.
	 *
	 * <p>Nilai bawaan ini hanya berlaku di sisi Java dan dipakai layar master untuk mengisi
	 * {@code Intbox} pada grid. Pengurutan sesungguhnya dilakukan basis data
	 * ({@code ORDER BY nomorUrut ASC, nama ASC}), sehingga baris ber-{@code NULL} ikut aturan
	 * urutan NULL milik basis data — bukan diperlakukan sebagai 1. Sama seperti {@code aktif},
	 * kolom ini tidak pernah ditulis {@code onSave} layar master; nilainya hanya berubah lewat
	 * {@code Intbox} pada grid yang menyimpan seketika saat {@code onChange}.</p>
	 *
	 * @return nomor urut tampil, atau {@code 1} bila belum pernah disetel.
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menetapkan nomor urut tampil baris katalog ini.
	 *
	 * <p>Tidak ada validasi keunikan maupun rentang; nomor kembar dibiarkan dan pemecah serinya
	 * adalah {@code nama} secara menaik.</p>
	 *
	 * @param nomorUrut nomor urut tampil; boleh {@code null}.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}
}
