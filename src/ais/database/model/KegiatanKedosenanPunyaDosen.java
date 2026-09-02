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

/**
 * Baris <b>keikutsertaan seorang dosen pada satu kegiatan kedosenan</b> &mdash; tabel penghubung
 * (<i>join entity</i>) antara {@link KegiatanKedosenan} (kegiatan tridharma: seminar, pelatihan,
 * kepanitiaan, penyuluhan, penulisan di media massa, dan seterusnya) dan {@link Dosen} pesertanya,
 * dengan atribut tambahan milik pasangan itu sendiri: peran/jabatan dalam kegiatan, skala kegiatan,
 * rentang tanggal keikutsertaan, keterangan bebas, dan bendera persetujuan.
 *
 * <p>Tabel: {@code public.kegiatan_kedosenan_punya_dosen}. Kelas ini adalah pasangan sisi-dosen
 * dari {@link KegiatanKemahasiswaanPunyaMahasiswa} (sisi-mahasiswa) dan mengikuti bentuk yang sama
 * persis dengan {@link OrganisasiDosenPunyaDosen} (keanggotaan organisasi profesi dosen) &mdash;
 * termasuk mewarisi kuirk-kuirknya, lihat {@link #getTbmuser()}.</p>
 *
 * <h2>PERINGATAN NAMA: tidak ada hubungannya dengan {@link Kegiatan}/{@link DetailKegiatan}</h2>
 *
 * <p>Repo ini memuat beberapa kelas berawalan "Kegiatan" yang <b>tidak berkerabat sama sekali</b>.
 * Verifikasi langsung dari kode kelas ini (bukan dari kemiripan nama):</p>
 *
 * <ul>
 *   <li><b>Nol {@code @JoinColumn} ke rantai billing.</b> Seluruh relasi kelas ini menunjuk
 *       {@code kegiatan_kedosenan}, {@code dosen}, {@code tbmuser},
 *       {@code jabatan_kegiatan_kedosenan}, dan {@code skala_kegiatan_kedosenan}. Tidak satu pun
 *       menunjuk tabel {@code kegiatan} ({@link Kegiatan}, wadah tagihan mahasiswa per semester)
 *       maupun {@code detail_kegiatan} ({@link DetailKegiatan}, baris tagihan di dalamnya). Arah
 *       sebaliknya juga nihil: {@link Kegiatan}/{@link DetailKegiatan} tidak punya properti
 *       bertipe kelas ini.</li>
 *   <li><b>Nol properti nominal.</b> Kelas ini tidak punya {@code amount}, {@code denda},
 *       {@code diskon}, {@code lunas}, {@code itemBiaya}, atau jejak posting jurnal apa pun.
 *       Domainnya kepegawaian/tridharma, bukan keuangan.</li>
 *   <li><b>Perhatian tambahan.</b> {@link PembayaranMahasiswa} juga dipetakan ke tabel
 *       {@code kegiatan} yang sama dengan {@link Kegiatan} &mdash; sekali lagi tanpa kaitan apa pun
 *       dengan kelas ini.</li>
 * </ul>
 *
 * <p>Kerabat sebenarnya kelas ini adalah himpunan master kedosenan:
 * {@link KelompokKegiatanKedosenan} &rarr; {@link DetailKelompokKegiatanKedosenan} &rarr;
 * ({@link JabatanKegiatanKedosenan}, {@link SkalaKegiatanKedosenan}), plus
 * {@link KegiatanKedosenan} sebagai kegiatan induk.</p>
 *
 * <p><b>Nama kelas di sini TIDAK menyesatkan.</b> Label UI/menu yang mengarah ke modul ini
 * memang berbunyi "Kegiatan Dosen"/"Kegiatan Kedosenan" (menu dasbor admin
 * {@code DashboardKegiatanKedosenanAdmin}, tab "Kegiatan Dosen" pada
 * {@code BiodataDosenAction}, layar {@code /pages/master/kegiatan_kedosenan.zul}), dan data
 * awalnya ({@code InitDataHelper}) memang berisi kelompok tridharma dosen.</p>
 *
 * <h2>Siapa yang membuat baris ini</h2>
 *
 * <ul>
 *   <li><b>"Ambil Dosen"</b> pada {@code KegiatanKedosenanPunyaDosenHelper} &rarr;
 *       {@code AmbilDataDosenForKegiatanKedosenanHelper}: admin mencentang banyak dosen sekaligus;
 *       {@link #setOleh(String)}, {@link #setTbmuser(Tbmuser)}, dan {@link #setDiubahDari(String)}
 *       (bernilai {@code "DosenAction"}) diisi di sana. Baris lahir tanpa persetujuan.</li>
 *   <li><b>Formulir kegiatan</b> ({@code FormulirKegiatanPesertaHelper}): peserta formulir yang
 *       sudah di-{@code acc} dipromosikan massal menjadi baris di sini. Perhatikan jalur ini
 *       menyetel {@link #setPersetujuan(Boolean) persetujuan = true} langsung, jadi alur
 *       persetujuan per baris dilewati sepenuhnya.</li>
 *   <li><b>Impor Excel</b> ({@code Common.uploadData}, hanya untuk admin) dengan kolom
 *       {@code id, kegiatanKedosenan, dosen, mulai, sampai, jabatanKegiatanKedosenan,
 *       skalaKegiatanKedosenan, persetujuan, keterangan}.</li>
 * </ul>
 *
 * <h2>Siapa yang membaca baris ini</h2>
 *
 * <ul>
 *   <li><b>BKD (Beban Kerja Dosen)</b> &mdash; {@code BkdKegiatanDosenHelper.populate} menyaring
 *       baris {@code persetujuan=true} yang jabatan DAN skalanya terisi, lalu menurunkan SKS-nya
 *       dari {@code ParameterUmum} ke {@link AsesemenPenilaian}. Perlu dicatat: <b>kelas ini
 *       sendiri tidak menyimpan poin/SKS apa pun</b>; bobot ditentukan di luar, dari kombinasi
 *       {@link JabatanKegiatanKedosenan} &times; {@link SkalaKegiatanKedosenan} &times;
 *       {@link DetailKelompokKegiatanKedosenan}.</li>
 *   <li><b>Penilaian asesor</b> &mdash; {@link PenilaianAsesor} berjenis
 *       {@link PenilaianAsesor#KEGIATAN_DOSEN} menunjuk baris ini sebagai objek yang dinilai.</li>
 *   <li><b>Profil dosen &amp; borang</b> &mdash; {@code ProfileDosen}, {@code ProfileUiHelper},
 *       {@code LaporanProfileDosen_A_4_5_3}, {@code DashboardRekapKegiatanDosenan},
 *       {@code DasborPerguruanTinggiTerpadu}.</li>
 *   <li><b>Sertifikat</b> &mdash; {@code SertifikatAction.cetakSertifikat} (tombol hanya muncul
 *       bila baris sudah disetujui DAN kegiatan induknya punya {@link Sertifikat}).</li>
 *   <li><b>Repository DSpace</b> &mdash; ekspor metadata Dublin Core per baris, bila konfigurasi
 *       {@code terhubung_ke_dspace} dan {@code kegiatan_dosen_terhubung_ke_dspace} aktif.</li>
 *   <li><b>Lampiran bukti</b> &mdash; {@code LampiranLain} memakai nama kelas ini sebagai
 *       diskriminator {@code jenis} untuk berkas "Bukti Kegiatan Dosen".</li>
 * </ul>
 *
 * <h2>Efek samping tersembunyi di luar kelas ini</h2>
 *
 * <p>Setiap {@code INSERT}/{@code UPDATE}/{@code DELETE} baris ini dicegat
 * {@code ais.database.hibernate.AuditListener}, yang memanggil
 * {@code Dosen.populateKegiatanKedosenanPunyaDosen(...)} atau
 * {@code Dosen.removeKegiatanKedosenanPunyaDosen(...)}. Keduanya menulis <b>berkas indeks JSON di
 * disk</b> milik dosen bersangkutan (dipakai {@code Dosen.ambilKegiatanKedosenanPunyaDosen()}).
 * Jadi menyimpan objek ini bukan sekadar operasi basis data.</p>
 *
 * <p>Kelas ini {@link Audited @Audited}: setiap versi baris disalin ke tabel riwayat Envers,
 * termasuk versi yang kolom {@code tbmuser}-nya sudah telanjur ternol (lihat
 * {@link #getTbmuser()}).</p>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ol>
 *   <li><b>Jejak audit</b> (dideklarasikan ulang, lihat catatan warisan di bawah):
 *   {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()},
 *   dan {@link #getDiubahDari()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}, {@link #toString()}.</li>
 *   <li><b>Relasi inti</b>: {@link #getKegiatanKedosenan()}, {@link #getDosen()},
 *   {@link #getTbmuser()}.</li>
 *   <li><b>Atribut keikutsertaan</b>: {@link #getJabatanKegiatanKedosenan()},
 *   {@link #getSkalaKegiatanKedosenan()}, {@link #getMulai()}, {@link #getSampai()},
 *   {@link #getKeterangan()}, {@link #getPersetujuan()}.</li>
 * </ol>
 *
 * <p><b>Tidak ada method query statis maupun method bisnis</b> di kelas ini; seluruh logika
 * pencarian/persetujuan hidup di {@code KegiatanKedosenanPunyaDosenHelper},
 * {@code DosenPunyaKegiatanKedosenanHelper}, dan {@code BkdKegiatanDosenHelper}.</p>
 *
 * <h2>Kuirk yang wajib diketahui pemanggil</h2>
 *
 * <p>Empat getter di kelas ini <b>tidak murni membaca</b> &mdash; dokumentasi masing-masing
 * merinci akibatnya, dan karena Hibernate memakai <i>property access</i> (anotasi menempel pada
 * getter, bukan field) apa yang dikembalikan getter itulah yang ditulis ke basis data:</p>
 * <ul>
 *   <li>{@link #getTbmuser()} &mdash; <b>destruktif</b>: bisa mengembalikan {@code null} untuk
 *   baris yang kolomnya terisi, sehingga penyimpanan berikutnya menghapus jejak pembuat baris.</li>
 *   <li>{@link #getPersetujuan()} &mdash; menulis balik {@code false} ke field bila kegiatan
 *   induknya belum disetujui.</li>
 *   <li>{@link #getJabatanKegiatanKedosenan()} dan {@link #getSkalaKegiatanKedosenan()} &mdash;
 *   menyalin nilai dari kegiatan induk ke field kosong (denormalisasi saat baca).</li>
 * </ul>
 *
 * <p>Sementara {@link #getMulai()}/{@link #getSampai()} memakai fallback ke kegiatan induk
 * <b>tanpa</b> menulis balik &mdash; asimetri yang disengaja atau tidak, tetapi nyata.</p>
 *
 * <h2>Catatan warisan {@link GeneralValueObject}</h2>
 *
 * <p>Kelas induk {@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu
 * pun propertinya. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di sini; itu keharusan teknis, bukan
 * duplikasi yang keliru.</p>
 *
 * <p><b>Pengecualian kontrak base class:</b> {@link GeneralValueObject#getKeterangan()} menjamin
 * hasil non-{@code null} (mengembalikan {@code ""}), sedangkan {@link #getKeterangan()} di sini
 * mengembalikan field apa adanya sehingga <b>bisa {@code null}</b>. Konsekuensi nyatanya
 * terdokumentasi pada method itu.</p>
 *
 * @see KegiatanKedosenan
 * @see Dosen
 * @see KegiatanKemahasiswaanPunyaMahasiswa
 * @see OrganisasiDosenPunyaDosen
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kegiatan_kedosenan_punya_dosen")

public class KegiatanKedosenanPunyaDosen extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris, {@code IDENTITY} basis data. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris. Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di kelas ini (bukan diwarisi secara terpetakan) karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass}; lihat catatan warisan pada
	 * dokumentasi kelas.</p>
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk:</b> nilai {@code null}, kosong, atau hanya berisi spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return} tanpa mengubah apa pun), sehingga jejak audit yang sudah ada
	 * tidak bisa dikosongkan lewat setter ini. Biasanya diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.</p>
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang dengan
	 * alasan yang sama seperti {@link #getOlehId()}.
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
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati
	 * callback ini. Untuk baris hasil "Ambil Dosen" jejak audit awal diisi manual oleh
	 * {@code AmbilDataDosenForKegiatanKedosenanHelper} ({@code setOleh}/{@code setTbmuser}),
	 * sedangkan baris hasil promosi {@code FormulirKegiatanPesertaHelper} hanya mendapat
	 * {@link #setDiubahDari(String)} berisi nama kelas + userId pemicunya.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Berbeda dari {@link #setOleh(String)}, setter ini
	 * menerima {@code null} apa adanya.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}). Tidak pernah
	 * {@code null} untuk objek yang baru dibuat di JVM ini, karena field-nya diinisialisasi saat
	 * konstruksi; baris lama hasil {@code SELECT} mengikuti isi kolom basis data.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris dalam bentuk "&lt;kegiatan&gt; - &lt;dosen&gt;", dipakai antara lain
	 * sebagai teks progres pada ekspor DSpace di {@code KegiatanKedosenanPunyaDosenHelper} dan
	 * pada populasi BKD di {@code BkdKegiatanDosenHelper}.
	 *
	 * <p><b>Kuirk penting:</b> method ini membaca <b>field</b> {@code kegiatanKedosenan} dan
	 * {@code dosen} secara langsung, BUKAN lewat {@link #getKegiatanKedosenan()}/
	 * {@link #getDosen()}. Artinya {@code check(...)} tidak dijalankan, sehingga untuk objek yang
	 * relasinya masih berupa proxy lazy dan sesinya sudah tertutup, hasilnya bisa berupa teks
	 * proxy Hibernate atau memicu {@code LazyInitializationException}.</p>
	 *
	 * <p>Sisi yang {@code null} tercetak sebagai teks {@code "null"} karena memakai perangkaian
	 * string biasa.</p>
	 *
	 * @return teks gabungan kegiatan dan dosen
	 */
	public String toString() {
		return kegiatanKedosenan + " - " + dosen;
	}

	/** Kegiatan induk yang diikuti. Wajib terisi. Lihat {@link #getKegiatanKedosenan()}. */
	private KegiatanKedosenan kegiatanKedosenan;
	/** Dosen peserta kegiatan. Wajib terisi. Lihat {@link #getDosen()}. */
	private Dosen dosen;
	/** Penanda jalur/kelas asal pembuatan baris. Lihat {@link #getDiubahDari()}. */
	private String diubahDari;

	/** Akun pengguna yang mendaftarkan dosen ini. Lihat peringatan pada {@link #getTbmuser()}. */
	private Tbmuser tbmuser;
	/** Peran dosen dalam kegiatan (peserta/narasumber/ketua/...). Lihat {@link #getJabatanKegiatanKedosenan()}. */
	private JabatanKegiatanKedosenan jabatanKegiatanKedosenan;
	/** Skala kegiatan (fakultas/institut/regional/nasional/internasional). Lihat {@link #getSkalaKegiatanKedosenan()}. */
	private SkalaKegiatanKedosenan skalaKegiatanKedosenan;

	/** Keterangan bebas per peserta (kolom {@code text}). Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Tanggal mulai keikutsertaan; boleh kosong. Lihat {@link #getMulai()}. */
	private Date mulai;
	/** Tanggal selesai keikutsertaan; boleh kosong. Lihat {@link #getSampai()}. */
	private Date sampai;
	/** Bendera persetujuan atasan/admin atas keikutsertaan ini. Lihat {@link #getPersetujuan()}. */
	private Boolean persetujuan;

	/**
	 * Konstruktor tanpa argumen; wajib ada untuk Hibernate dan dipakai langsung oleh helper
	 * "Ambil Dosen" serta promosi formulir kegiatan. Tidak menginisialisasi apa pun kecuali
	 * {@code tanggal_dirubah} lewat inisialisasi field.
	 */
	public KegiatanKedosenanPunyaDosen() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} dipetakan {@code insertable = false} dengan strategi {@code IDENTITY},
	 * jadi nilainya ditentukan basis data dan baru terisi setelah {@code save}/flush. Nilai ini
	 * juga dipakai sebagai {@code ref} lampiran bukti kegiatan pada {@code LampiranLain} serta
	 * sebagai kunci berkas indeks JSON milik {@link Dosen}.</p>
	 *
	 * @return ID baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris. Praktis hanya dipakai Hibernate saat memuat baris; kode aplikasi
	 * tidak boleh mengubahnya karena ID juga menjadi kunci lampiran dan indeks disk.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kegiatan kedosenan yang diikuti dosen pada baris ini, setelah diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} sehingga aman dipakai walau relasi tadinya masih
	 * berupa proxy lazy dan sesi asalnya sudah tertutup.
	 *
	 * <p><b>Efek samping (pola standar entity AIS, bukan bug):</b> hasil {@code check(...)}
	 * ditugaskan kembali ke field, karena instance yang dikembalikan bisa berbeda dari proxy
	 * semula (instance kanonik dari {@code EntityIdentityMap}, cache, atau hasil reload).</p>
	 *
	 * <p>Kolom {@code kegiatan_kedosenan} {@code nullable = false}, jadi untuk baris yang sudah
	 * tersimpan hasilnya tidak pernah {@code null}. Untuk objek yang belum diisi pemanggil, tentu
	 * saja masih bisa {@code null}.</p>
	 *
	 * @return kegiatan induk keikutsertaan ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kegiatan_kedosenan", nullable = false)
	public KegiatanKedosenan getKegiatanKedosenan() {
		kegiatanKedosenan = check(kegiatanKedosenan);
		return kegiatanKedosenan;
	}

	/**
	 * Menyetel kegiatan induk. Wajib diisi sebelum penyimpanan (kolom {@code NOT NULL}). Tidak ada
	 * validasi bahwa kegiatan tersebut masih boleh dipilih
	 * ({@link KegiatanKedosenan#getBolehDipilih()}) &mdash; penyaringan itu hanya ada di UI.
	 *
	 * @param kegiatanKedosenan kegiatan induk
	 */
	public void setKegiatanKedosenan(KegiatanKedosenan kegiatanKedosenan) {
		this.kegiatanKedosenan = kegiatanKedosenan;
	}

	/**
	 * Mengembalikan dosen peserta kegiatan pada baris ini, diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dengan penugasan balik ke field (pola standar
	 * entity AIS, lihat {@link #getKegiatanKedosenan()}).
	 *
	 * <p>Nilai inilah yang dipakai {@code AuditListener} untuk memutakhirkan berkas indeks JSON
	 * dosen setiap kali baris disimpan atau dihapus, dan yang dibaca modul BKD untuk menemukan
	 * {@code Pegawai} pemilik beban kerja.</p>
	 *
	 * <p>Kolom {@code dosen} {@code nullable = false}.</p>
	 *
	 * @return dosen peserta
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menyetel dosen peserta. Wajib diisi sebelum penyimpanan (kolom {@code NOT NULL}). Tidak ada
	 * pemeriksaan duplikat pasangan dosen+kegiatan di sini; pencegahan duplikasi dilakukan
	 * pemanggil dengan query {@code uniqueResult()} lebih dulu.
	 *
	 * @param dosen dosen peserta
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan akun pengguna yang mendaftarkan dosen ini ke kegiatan &mdash; <b>tetapi hanya
	 * bila pendaftarnya BUKAN akun dosen</b>.
	 *
	 * <p><b>PERINGATAN: getter destruktif.</b> Setelah {@code check(...)}, method ini
	 * mengembalikan {@code null} bila akun tersebut punya data dosen
	 * ({@code Tbmuser.ambilDosen() != null}) DAN {@code roleId}-nya {@code "dosen"}. Karena
	 * Hibernate memetakan kelas ini lewat <i>property access</i> (anotasi menempel pada getter),
	 * <b>yang dibaca saat flush adalah hasil getter ini, bukan isi field</b>. Konsekuensinya:
	 * begitu baris seperti itu disimpan ulang &mdash; dan itu terjadi pada operasi rutin, mis.
	 * mencentang "Setujui" atau mengubah tanggal/keterangan lewat
	 * {@code KegiatanKedosenanPunyaDosenHelper} &mdash; kolom {@code tbmuser} <b>ditulis
	 * {@code NULL} secara permanen</b>, dan identitas pendaftarnya hilang. Field di memori tetap
	 * berisi nilai lama, sehingga kehilangan itu tidak terlihat di layar.</p>
	 *
	 * <p>Justru kasus itulah yang lazim: {@code FormulirKegiatanPesertaHelper} menyetel
	 * {@link #setTbmuser(Tbmuser)} dengan pengguna yang sedang login, yang pada alur formulir
	 * kegiatan biasanya memang seorang dosen.</p>
	 *
	 * <p>Perilaku ini <b>identik kata-per-kata</b> dengan {@code OrganisasiDosenPunyaDosen
	 * .getTbmuser()}. Dicatat apa adanya, tidak diperbaiki di sini.</p>
	 *
	 * <p>Catatan tambahan: karena entity ini {@link Audited @Audited}, versi yang sudah ternol ikut
	 * tersalin ke tabel riwayat Envers. {@code Tbmuser.hakAkses()} juga dipanggil tanpa penjaga
	 * {@code null}, sehingga akun tanpa hak akses berpotensi memicu {@code NullPointerException}
	 * di sini.</p>
	 *
	 * @return akun pendaftar bila bukan akun dosen; {@code null} bila akun dosen atau memang belum
	 *         diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen") ? null : tbmuser;
	}

	/**
	 * Menyetel akun pengguna pendaftar. Diisi oleh
	 * {@code AmbilDataDosenForKegiatanKedosenanHelper} dan {@code FormulirKegiatanPesertaHelper}
	 * dengan pengguna yang sedang login.
	 *
	 * <p>Perhatikan bahwa nilai yang disimpan di sini belum tentu bertahan; lihat peringatan pada
	 * {@link #getTbmuser()}.</p>
	 *
	 * @param tbmuser akun pendaftar; boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan penanda asal-usul baris: nama kelas (kadang ditambah {@code " oleh <userId>"})
	 * yang membuat atau terakhir mengubah baris ini lewat jalur otomatis.
	 *
	 * <p>Nilai yang benar-benar dipakai di kode: {@code "DosenAction"} (dari helper "Ambil Dosen")
	 * dan {@code "ais.action.master.helper.FormulirKegiatanPesertaHelper oleh <userId>"} (dari
	 * promosi peserta formulir). Baris hasil impor Excel atau penyuntingan manual biasa tidak
	 * mengisinya.</p>
	 *
	 * <p>Tanpa anotasi kolom eksplisit, jadi dipetakan ke kolom {@code diubahDari} bertipe
	 * {@code varchar(255)} default. Hanya informasional; tidak ada logika yang bercabang atas
	 * nilainya.</p>
	 *
	 * @return penanda asal-usul, atau {@code null} bila tidak diisi
	 */
	public String getDiubahDari() {
		return diubahDari;
	}

	/**
	 * Menyetel penanda asal-usul baris. Tanpa validasi format apa pun.
	 *
	 * @param diubahDari nama kelas/jalur pembuat baris
	 */
	public void setDiubahDari(String diubahDari) {
		this.diubahDari = diubahDari;
	}

	/**
	 * Mengembalikan peran/jabatan dosen dalam kegiatan (mis. Peserta, Narasumber, Ketua Tim,
	 * Pembina, Penulis &mdash; lihat data awal pada {@code InitDataHelper}).
	 *
	 * <p><b>Efek samping &mdash; denormalisasi saat baca:</b> bila field masih kosong sedangkan
	 * field {@code kegiatanKedosenan} sudah terisi, nilai diambil dari
	 * {@link KegiatanKedosenan#getJabatanKegiatanKedosenan()} lalu <b>ditugaskan ke field ini</b>.
	 * Karena Hibernate membaca lewat getter, penyimpanan berikutnya akan menuliskan nilai warisan
	 * itu ke kolom {@code jabatan_kegiatan_kedosenan} &mdash; jadi sekadar membuka layar daftar
	 * peserta dapat mengubah isi basis data. Baru setelah itu {@code check(...)} dijalankan
	 * seperti getter relasi lain.</p>
	 *
	 * <p><b>Kuirk:</b> pemeriksaan memakai <b>field</b> {@code kegiatanKedosenan} langsung, bukan
	 * {@link #getKegiatanKedosenan()}. Untuk objek yang relasinya masih proxy dan sesinya sudah
	 * tertutup, pemanggilan {@code kegiatanKedosenan.getJabatanKegiatanKedosenan()} berpotensi
	 * melempar {@code LazyInitializationException}.</p>
	 *
	 * <p>Bersama {@link #getSkalaKegiatanKedosenan()}, nilai ini menentukan bobot SKS kegiatan di
	 * modul BKD; baris yang salah satunya kosong <b>tidak diikutkan</b> oleh
	 * {@code BkdKegiatanDosenHelper.populate}.</p>
	 *
	 * @return peran dosen dalam kegiatan, atau {@code null} bila tidak ada di baris ini maupun di
	 *         kegiatan induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_kegiatan_kedosenan", nullable = true)
	public JabatanKegiatanKedosenan getJabatanKegiatanKedosenan() {
		if (kegiatanKedosenan != null && jabatanKegiatanKedosenan == null) {
			jabatanKegiatanKedosenan = kegiatanKedosenan.getJabatanKegiatanKedosenan();
		}
		jabatanKegiatanKedosenan = check(jabatanKegiatanKedosenan);
		return jabatanKegiatanKedosenan;
	}

	/**
	 * Menyetel peran/jabatan dosen dalam kegiatan.
	 *
	 * <p>Di UI, combobox pemilihnya hanya berisi jabatan yang terdaftar pada
	 * {@link DetailKelompokKegiatanKedosenan} milik kegiatan induk, dan dikunci begitu baris
	 * disetujui &mdash; tetapi setter ini sendiri tidak menegakkan satu pun aturan tersebut.</p>
	 *
	 * @param jabatanKegiatanKedosenan peran dosen; boleh {@code null}
	 */
	public void setJabatanKegiatanKedosenan(JabatanKegiatanKedosenan jabatanKegiatanKedosenan) {
		this.jabatanKegiatanKedosenan = jabatanKegiatanKedosenan;
	}

	/**
	 * Mengembalikan keterangan bebas peserta (kolom {@code text}, diisi lewat kotak teks 2 baris
	 * pada grid daftar peserta; dikunci setelah baris disetujui).
	 *
	 * <p><b>Pengecualian kontrak {@link GeneralValueObject}:</b> base class menjamin
	 * {@code getKeterangan()} tidak pernah {@code null} (mengembalikan {@code ""}), sedangkan
	 * override di sini mengembalikan field apa adanya sehingga <b>bisa {@code null}</b>. Dua
	 * konsekuensi nyata:</p>
	 * <ul>
	 *   <li>{@code KegiatanKedosenanPunyaDosenHelper.getDspace} memanggil
	 *   {@code new StringReader(getKeterangan())} tanpa penjaga {@code null}; ekspor DSpace atas
	 *   baris tanpa keterangan akan melempar {@code NullPointerException}.</li>
	 *   <li>{@link GeneralValueObject#compareTo(GeneralValueObject)} memakai {@code keterangan}
	 *   sebagai kunci urut terakhir; karena kelas ini tidak punya {@code nomorUrut}/{@code nim}/
	 *   {@code nama}, pengurutan alami baris tanpa keterangan selalu jatuh ke {@code 0}
	 *   (dianggap setara).</li>
	 * </ul>
	 *
	 * @return keterangan peserta, atau {@code null} bila tidak diisi
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas peserta. Tanpa validasi panjang maupun pembersihan HTML; nilai
	 * yang tersimpan diproses {@code Html2Text} saat diekspor ke DSpace.
	 *
	 * @param keterangan keterangan peserta; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status persetujuan keikutsertaan ini, dengan <b>default {@code false}</b> bila
	 * kolomnya masih {@code null}. Pemanggil karena itu aman menuliskan
	 * {@code if (getPersetujuan())} tanpa penjaga {@code null} &mdash; dan memang begitulah
	 * seluruh pemakaiannya di helper.
	 *
	 * <p><b>Efek samping &mdash; getter yang menulis balik:</b> bila field {@code kegiatanKedosenan}
	 * terisi dan status kegiatan induk BUKAN "Disetujui", field {@code persetujuan}
	 * <b>dipaksa {@code false}</b>. Karena Hibernate membaca lewat getter, pemaksaan itu ikut
	 * tertulis ke basis data pada penyimpanan berikutnya: menarik kembali persetujuan sebuah
	 * kegiatan induk secara efektif membatalkan persetujuan seluruh pesertanya, satu per satu,
	 * saat masing-masing baris kebetulan dibaca lalu disimpan.</p>
	 *
	 * <p><b>Kuirk konstanta:</b> perbandingan memakai {@code PrestasiDosen.DISETUJUI}, padahal
	 * yang dibandingkan adalah status milik {@link KegiatanKedosenan}, yang punya konstanta
	 * {@link KegiatanKedosenan#DISETUJUI} sendiri. Keduanya kebetulan bernilai sama persis
	 * ({@code "Disetujui"}), jadi hasilnya benar &mdash; tetapi kebenarannya bergantung pada
	 * kebetulan itu, dan {@code KegiatanKedosenanPunyaDosenHelper} sendiri memakai konstanta
	 * {@link KegiatanKedosenan#DISETUJUI} untuk pemeriksaan yang setara.</p>
	 *
	 * <p><b>Kuirk lazy:</b> {@code kegiatanKedosenan.getStatus()} dipanggil pada field mentah,
	 * tanpa melewati {@link #getKegiatanKedosenan()}/{@code check(...)}; berpotensi
	 * {@code LazyInitializationException} pada objek detached.</p>
	 *
	 * <p>Nilai {@code true} mengunci baris di UI (tanggal, jabatan, skala, keterangan menjadi
	 * disabled), menyembunyikan tombol Hapus, memunculkan tombol cetak Sertifikat bila kegiatan
	 * induk punya {@link Sertifikat}, membuat baris ikut diproses BKD/asesmen, dan membuatnya
	 * memenuhi syarat ekspor DSpace. Sebaliknya baris yang belum disetujui adalah satu-satunya
	 * yang bisa disapu tombol "Bersihkan" (SQL {@code DELETE} massal per kegiatan).</p>
	 *
	 * @return {@code true} bila keikutsertaan sudah disetujui; {@code false} bila belum atau bila
	 *         kegiatan induknya sendiri belum disetujui
	 */
	public Boolean getPersetujuan() {
		if (kegiatanKedosenan != null && !kegiatanKedosenan.getStatus().equals(PrestasiDosen.DISETUJUI)) {
			persetujuan = false;
		}
		return persetujuan == null ? false : persetujuan;
	}

	/**
	 * Menyetel status persetujuan keikutsertaan.
	 *
	 * <p>Di UI dipanggil dari checkbox "Setujui" yang hanya dirender untuk atasan langsung dosen
	 * bersangkutan ({@code Dosen.yangLoginMerupakanAtasan()}) atau pengguna non-dosen saat
	 * kegiatan induk sudah berstatus {@link KegiatanKedosenan#DISETUJUI}; pengguna lain hanya
	 * melihat label "Ya"/"Belum". Setter ini sendiri <b>tidak menegakkan satu pun</b> pemeriksaan
	 * itu &mdash; jalur promosi {@code FormulirKegiatanPesertaHelper} memang memanggilnya
	 * langsung dengan {@code true} tanpa persetujuan siapa pun.</p>
	 *
	 * @param persetujuan status persetujuan; boleh {@code null} (dibaca sebagai {@code false})
	 */
	public void setPersetujuan(Boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Mengembalikan skala kegiatan bagi peserta ini (mis. Fak./Jur., Institut, Regional, Nasional,
	 * Internasional, atau "setiap kali tampil" &mdash; lihat data awal pada
	 * {@code InitDataHelper}).
	 *
	 * <p>Berperilaku persis seperti {@link #getJabatanKegiatanKedosenan()}: bila field kosong dan
	 * kegiatan induk terisi, nilai <b>disalin dari kegiatan induk ke field ini</b> (denormalisasi
	 * saat baca yang ikut tertulis ke basis data pada penyimpanan berikutnya), lalu diresolusi
	 * dengan {@code check(...)}. Pemeriksaan juga memakai field {@code kegiatanKedosenan} mentah,
	 * dengan risiko {@code LazyInitializationException} yang sama.</p>
	 *
	 * <p>Nilai ini dipakai sebagai {@code dc.subject} pada ekspor DSpace dan sebagai salah satu
	 * dimensi penentu bobot SKS di modul BKD.</p>
	 *
	 * @return skala kegiatan, atau {@code null} bila tidak ada di baris ini maupun di kegiatan
	 *         induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "skala_kegiatan_kedosenan", nullable = true)
	public SkalaKegiatanKedosenan getSkalaKegiatanKedosenan() {
		if (kegiatanKedosenan != null && skalaKegiatanKedosenan == null) {
			skalaKegiatanKedosenan = kegiatanKedosenan.getSkalaKegiatanKedosenan();
		}
		skalaKegiatanKedosenan = check(skalaKegiatanKedosenan);
		return skalaKegiatanKedosenan;
	}

	/**
	 * Menyetel skala kegiatan bagi peserta ini. Sama seperti
	 * {@link #setJabatanKegiatanKedosenan(JabatanKegiatanKedosenan)}, pembatasan pilihan ke skala
	 * milik {@link DetailKelompokKegiatanKedosenan} kegiatan induk hanya ada di UI.
	 *
	 * @param skalaKegiatanKedosenan skala kegiatan; boleh {@code null}
	 */
	public void setSkalaKegiatanKedosenan(SkalaKegiatanKedosenan skalaKegiatanKedosenan) {
		this.skalaKegiatanKedosenan = skalaKegiatanKedosenan;
	}

	/**
	 * Mengembalikan tanggal mulai keikutsertaan, dengan <b>fallback ke tanggal mulai kegiatan
	 * induk</b> bila kolomnya kosong (presisi {@code DATE}).
	 *
	 * <p><b>Berbeda dari {@link #getJabatanKegiatanKedosenan()}/
	 * {@link #getSkalaKegiatanKedosenan()}, fallback di sini TIDAK ditulis balik ke field.</b>
	 * Nilai warisan hanya dikembalikan ke pemanggil; kolom {@code mulai} tetap {@code NULL} di
	 * basis data. Asimetri ini nyata dan disengaja atau tidak &mdash; dicatat apa adanya.</p>
	 *
	 * <p><b>Kuirk lazy yang sama:</b> {@code kegiatanKedosenan} dibaca sebagai field mentah, bukan
	 * lewat {@link #getKegiatanKedosenan()}.</p>
	 *
	 * <p>Nilai ini menjadi {@code dc.date.issued} pada ekspor DSpace (yang melewatkannya bila
	 * {@code null}).</p>
	 *
	 * @return tanggal mulai keikutsertaan, tanggal mulai kegiatan induk sebagai cadangan, atau
	 *         {@code null} bila keduanya kosong
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai == null ? (kegiatanKedosenan == null ? null : kegiatanKedosenan.getMulai()) : mulai;
	}

	/**
	 * Menyetel tanggal mulai keikutsertaan. Boleh {@code null} untuk mengikuti tanggal kegiatan
	 * induk. Tidak ada validasi bahwa {@code mulai} mendahului {@link #getSampai()}, maupun bahwa
	 * rentangnya berada di dalam rentang kegiatan induk.
	 *
	 * @param mulai tanggal mulai; boleh {@code null}
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Menyetel tanggal selesai keikutsertaan. Boleh {@code null} untuk mengikuti tanggal kegiatan
	 * induk. Tanpa validasi rentang, sama seperti {@link #setMulai(Date)}.
	 *
	 * @param sampai tanggal selesai; boleh {@code null}
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan tanggal selesai keikutsertaan, dengan <b>fallback ke tanggal selesai kegiatan
	 * induk</b> bila kolomnya kosong (presisi {@code DATE}). Sama seperti {@link #getMulai()},
	 * fallback ini tidak ditulis balik ke field dan membaca {@code kegiatanKedosenan} sebagai
	 * field mentah.
	 *
	 * @return tanggal selesai keikutsertaan, tanggal selesai kegiatan induk sebagai cadangan, atau
	 *         {@code null} bila keduanya kosong
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai == null ? (kegiatanKedosenan == null ? null : kegiatanKedosenan.getSampai()) : sampai;
	}

}
