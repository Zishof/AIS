package ais.database.model;

// Generated Oct 23, 2009 4:59:38 PM by Hibernate Tools 3.2.4.CR1

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.employ.JenisJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Entity <b>Grup Pengguna</b> (peran/role) &mdash; tabel {@code public.tbmrole}.
 *
 * <p>Ini adalah sisi <b>"apa yang boleh dilakukan"</b> dari model otorisasi AIS.
 * Pasangannya adalah {@link Tbmuser}, yang merupakan sisi <b>"siapa"</b>. Keduanya
 * dihubungkan oleh satu kunci asing tunggal: kolom {@code userrole} pada tabel
 * {@code tbmuser}, dipetakan sebagai {@code @ManyToOne} pada
 * {@link Tbmuser#getUserRole()}. Peran <i>efektif</i> yang berlaku bagi sebuah akun
 * dibaca lewat {@link Tbmuser#hakAkses()}, yang mengembalikan instance kelas ini.</p>
 *
 * <h2>Hubungan definitif dengan {@link Tbmuser}</h2>
 * <ul>
 *   <li><b>Satu FK aktif.</b> {@code tbmuser.userrole} &rarr; {@code tbmrole.roleid}
 *   ({@code nullable=false} di sisi pemetaan). Inilah satu-satunya peran yang berlaku
 *   pada satu saat.</li>
 *   <li><b>Empat slot peran cadangan.</b> {@link Tbmuser} juga memiliki
 *   {@code userRole2}..{@code userRole5}. Slot-slot itu adalah <b>daftar peran yang
 *   boleh dipilih</b> pengguna, <b>bukan</b> peran yang digabungkan: {@code hakAkses()}
 *   sengaja <b>tidak</b> menggabungkan hak dari slot-slot tersebut, demi prinsip
 *   <i>least privilege</i>. Perpindahan antar slot dilakukan {@code MainAction} /
 *   {@code MainAction2} dan disertai penulisan langsung ke cache peran.</li>
 *   <li><b>Tiga jalur privilese yang TERPISAH.</b> Selain peran ini, {@link Tbmuser}
 *   punya {@link Tbmuser#getRoot()} dan {@code getSuperadmin()} yang bekerja
 *   <b>sepenuhnya di luar</b> mekanisme {@code Tbmrole}. Akun ber-{@code root} tidak
 *   perlu peran apa pun untuk lolos gerbangnya. Mematikan sebuah flag di sini
 *   <b>tidak</b> membatasi akun {@code root}/{@code superadmin}.</li>
 *   <li><b>Lingkup organisasi saling menimpa.</b> Varian {@code ambilXxx()} di
 *   {@link Tbmuser} ({@link Tbmuser#ambilJurusan()}, {@code ambilFakultas()},
 *   {@code ambilYayasan()}, {@code ambilSekolah()}, {@code ambilProgram()}) memberi
 *   <b>prioritas tertinggi</b> pada lingkup yang ditetapkan di sini
 *   ({@link #getJurusan()}, {@link #getFakultas()}, {@link #getYayasan()},
 *   {@link #getSekolah()}, {@link #getProgram()}). Artinya mengisi lingkup pada
 *   sebuah Grup Pengguna akan <b>menimpa</b> lingkup pribadi setiap akun yang
 *   memakainya.</li>
 *   <li><b>Tidak ada FK balik.</b> {@code Tbmrole} tidak memiliki koleksi
 *   {@code Set&lt;Tbmuser&gt;}. Untuk mencari akun pemakai sebuah peran, harus dilakukan
 *   query eksplisit terhadap {@link Tbmuser}.</li>
 * </ul>
 *
 * <h2>TIDAK ADA hierarki peran</h2>
 * <p>Sudah diverifikasi terhadap seluruh isi kelas ini: <b>tidak ada</b> field
 * self-reference {@code Tbmrole}, dan tidak ada field bernama {@code parent},
 * {@code induk}, {@code atasan}, maupun {@code parentRoleId}. Semua relasi keluar
 * menuju entity <i>lain</i> ({@link Menu}, {@link Program}, {@link Jurusan},
 * {@link Fakultas}, {@link Yayasan}, {@link Sekolah}, {@link JenisJabatan},
 * {@link SatuanKerja}), bukan menuju peran lain.</p>
 * <p>Yang kerap disalahartikan sebagai pewarisan adalah properti transien
 * {@code copyDari} (diwarisi dari {@link GeneralValueObject}, dipakai
 * {@code TbmroleAction} saat membuat peran baru). Itu hanya <b>menyalin snapshot</b>
 * daftar {@link Menu} dan baris {@code RolePrivilage} satu kali pada saat pembuatan;
 * setelah tersimpan kedua peran sepenuhnya independen dan perubahan pada peran sumber
 * tidak menular.</p>
 * <p>Multi-peran yang sesungguhnya ada di sisi {@link Tbmuser} (slot
 * {@code userRole2}..{@code userRole5}) &mdash; itu adalah <b>pemilihan</b> peran
 * aktif, bukan pewarisan hak.</p>
 *
 * <h2>Empat lapisan izin yang hidup berdampingan</h2>
 * <p>Kelas ini bukan satu-satunya sumber kebenaran otorisasi. Ada empat lapisan yang
 * saling lepas, dan <b>tidak</b> saling memvalidasi:</p>
 * <ol>
 *   <li><b>Flag Boolean modul</b> (sekitar 36 buah di kelas ini, mis.
 *   {@link #getKeuangan()}, {@link #getPustaka()}). Sebagian besar hanya mengatur
 *   <i>visibilitas</i> pintasan &amp; menu; hanya sebagian yang benar-benar menjadi
 *   gerbang di sisi server. Lihat catatan per-getter.</li>
 *   <li><b>Assignment navigasi</b> lewat koleksi {@link #getMenus()} (tabel
 *   {@code job_has_menu}). Menentukan menu apa yang <i>tampil</i>.</li>
 *   <li><b>Privilege CRUD</b> lewat entity <b>terpisah</b> {@code RolePrivilage}
 *   (baris per pasangan peran&times;menu, dibaca {@code CommonPrivilages}).
 *   Menentukan boleh <i>create/update/delete/approve</i>. Baris
 *   {@code RolePrivilage} <b>dapat eksis untuk menu yang tidak terdaftar</b> di
 *   {@code job_has_menu} &mdash; kedua lapisan tidak dijaga saling konsisten.</li>
 *   <li><b>Katalog JSON</b> {@link #getEbisnisMenu()} (POS/e-Bisnis),
 *   {@link #getTokoAksesJson()}, dan {@link #getJurnalAksesJson()}. Untuk modul
 *   POS/apotek/akuntansi eBisnis, <b>inilah lapisan yang benar-benar menegakkan
 *   izin</b> di sisi server.</li>
 * </ol>
 * <p>Entity {@code RoleAccess}, {@code UserAccess}, {@code UserRole}, dan
 * {@code RoleHasDashboard} yang bernama mirip adalah mekanisme <b>terpisah total</b>
 * dan tidak berkaitan dengan kelas ini maupun {@link Tbmuser}.</p>
 *
 * <h2>Pola default: "kolom {@code null} berarti tebak"</h2>
 * <p>Nyaris seluruh getter flag di kelas ini <b>tidak</b> mengembalikan nilai kolom apa
 * adanya. Bila kolomnya masih {@code null} (administrator belum pernah mencentang apa
 * pun), getter menurunkan nilai bawaan dari <b>{@code roleId}</b> atau bahkan dari
 * <b>{@code roleName}</b>. Ada tiga gaya penurunan, dan perbedaannya penting:</p>
 * <ul>
 *   <li><b>Default MENYALA</b> ({@code null} &rarr; {@code true}): {@link #getPustaka()},
 *   {@link #getDashboard()}, {@link #getWorkflow()}, {@link #getAdministrasi()},
 *   {@link #getPengadaan()}, {@link #getKinerja()}, {@link #getPresensiKehadiran()},
 *   {@link #getBacaRepository()}, {@link #getKalenderAkademik()},
 *   {@link #getInfoKegiatan()}. Grup Pengguna yang baru dibuat <b>langsung</b> memiliki
 *   hak-hak ini tanpa dicentang.</li>
 *   <li><b>Default berbasis COCOK-PERSIS nama</b> &mdash; lihat catatan di bawah.</li>
 *   <li><b>Default MATI</b> ({@code null} &rarr; {@code false}, <i>fail-closed</i>):
 *   {@link #getEmedic()}, {@link #getBolehVerifikasiMemberMelebihiLimit()},
 *   {@link #getBolehLihatSemuaToko()}, {@link #getMengajukanPengajuanPegawaiLain()},
 *   {@link #getBolehEntryTopup()}. Ini pola yang dianjurkan untuk flag baru.</li>
 * </ul>
 *
 * <h3>RIWAYAT: hak yang pernah menyala karena kebetulan penamaan (FIXED)</h3>
 * <p>Sampai dok audit keamanan 2026-09-06, beberapa default di sini diturunkan lewat
 * <b>pencocokan substring</b> pada pengenal atau nama peran, alih-alih pencocokan persis:
 * {@link #getKeuangan()}, {@link #getPembayaran()}, {@link #getAkunting()},
 * {@link #getKantin()}, dan {@link #getBolehEntryTopup()} menyala bila
 * {@code roleId.toLowerCase().contains("keu")}; {@link #getKepegawaian()} menyala bila
 * {@code roleId} mengandung {@code "pegawai"}; {@link #getBolehAksesFeeder()} /
 * {@link #getBolehAksesSister()} menyala bila <b>{@code roleName}</b> mengandung
 * {@code "admin"} atau {@code "akademik"}; dan {@link #getAksesGerbangPesantren()} menyala
 * bila {@code roleName} mengandung {@code "satpam"}, {@code "keamanan pondok"}, atau
 * {@code "keamanan pesantren"}. Akibatnya, membuat Grup Pengguna dengan pengenal seperti
 * {@code "keu_lihat_saja"} &mdash; yang secara niat hanya boleh membaca &mdash; secara
 * diam-diam memberi {@link #getBolehEntryTopup()} bernilai {@code true}, padahal itu adalah
 * gerbang <b>transaksional sungguhan</b> di {@code KantinHelper}/{@code PosApi}.</p>
 * <p><b>Status sekarang:</b> {@link #getBolehEntryTopup()} sudah diubah <i>fail-closed</i>
 * tanpa syarat ({@code null} &rarr; {@code false}), mengikuti pola
 * {@link #getBolehVerifikasiMemberMelebihiLimit()}. Keenam getter lain di atas sudah diganti
 * dari pencocokan substring menjadi pencocokan persis ({@code equalsIgnoreCase} terhadap
 * {@link #KEUANGAN} / {@code "pegawai"} untuk {@code roleId}, {@code equals} terhadap
 * {@code "admin"} / {@code "akademik"} / {@code "satpam"} / {@code "keamanan pondok"} /
 * {@code "keamanan pesantren"} untuk {@code roleName}) sehingga hanya peran yang namanya
 * <b>sama persis</b> dengan kata kunci tersebut yang memperoleh hak secara bawaan. Audit data
 * UAT (57 baris {@code tbmrole}, 2026-09-06) tidak menemukan satu pun peran yang bergantung
 * pada default substring lama, tetapi data produksi belum diverifikasi &mdash; tetap
 * dianjurkan mencentang/mengosongkan flag secara eksplisit alih-alih mengandalkan nilai
 * bawaan.</p>
 *
 * <h2>Getter yang menulis balik field (anti-pola sistemik)</h2>
 * <p>Sebagian besar getter di kelas ini <b>mengubah state object saat sekadar
 * dibaca</b>: {@link #getRoleName()}, {@link #getAktif()}, {@link #getElearning()},
 * {@link #getKegiatanDanPrestasi()}, {@link #getPengadaan()}, {@link #getKeuangan()},
 * {@link #getPembayaran()}, {@link #getAkunting()}, {@link #getKepegawaian()},
 * {@link #getSatuanKerjas()}, serta seluruh getter relasi yang memanggil
 * {@code check(...)}. Ini adalah satu klaster besar dari anti-pola yang tercatat di
 * seluruh {@code ais.database.model}.</p>
 * <p><b>Mengapa ini berbahaya khusus di kelas ini.</b> Kelas ini beranotasi
 * {@link Audited @Audited} (Envers) <i>dan</i>
 * {@code dynamicUpdate = true}. Kombinasinya berarti: begitu sebuah instance
 * {@code Tbmrole} yang <i>ter-attach</i> ke {@link org.hibernate.Session} dibaca oleh
 * kode yang sekadar menampilkan menu, field-nya berubah, Hibernate menganggapnya
 * <i>dirty</i>, dan pada {@code flush} berikutnya akan terbit <b>revisi audit palsu</b>
 * seolah-olah administrator baru saja mengubah hak akses. Jejak audit perubahan
 * <b>izin</b> karena itu tidak dapat dipercaya sepenuhnya: tidak semua revisi mewakili
 * perubahan yang disengaja manusia.</p>
 *
 * <h2>Kaitan dengan anomali cache di {@link Tbmuser#hakAkses()}</h2>
 * <p>{@link Tbmuser#hakAkses()} mendokumentasikan lima kondisi anomali yang membuatnya
 * mengembalikan {@code null} (atau nilai menyesatkan) bagi pengguna yang sah. Kelas ini
 * berkontribusi langsung pada dua di antaranya:</p>
 * <ul>
 *   <li><b>Anomali #1 (resolusi proxy gagal).</b> Relasi {@code Tbmuser.userRole}
 *   dipetakan {@code FetchType.LAZY}, sehingga yang tersimpan sering berupa <i>proxy</i>
 *   {@code Tbmrole}. Bila baris peran sudah dihapus, atau object berasal dari sesi yang
 *   telah ditutup / hasil deserialisasi, {@code check(...)} gagal me-resolve dan
 *   {@link Tbmuser#getUserRole()} menghasilkan {@code null}.</li>
 *   <li><b>Anomali #5 (nilai ter-<i>detach</i> yang tampak sah).</b> Inilah kontribusi
 *   paling khas dari kelas ini. Cache {@code Tbmuser.getUserRoleYgDipakai} menyimpan
 *   instance {@code Tbmrole} <b>selamanya</b> (tidak ada kedaluwarsa). Koleksi
 *   {@link #getMenus()} adalah {@code @ManyToMany} yang <b>lazy secara bawaan</b>,
 *   sehingga instance yang di-cache dapat terlihat sah namun melempar
 *   {@link org.hibernate.LazyInitializationException} begitu {@code getMenus()}
 *   disentuh. Itulah sebabnya {@code MainMenuHelper} memeriksa
 *   {@code Hibernate.isInitialized(...)} lebih dulu, dan {@code MenuHelper.loadTree}
 *   memanggil {@code session.refresh(tbmrole)} sebelum menyalin koleksinya. Setiap kode
 *   baru yang menyentuh {@code getMenus()} dari hasil {@code hakAkses()} <b>wajib</b>
 *   melakukan hal yang sama.</li>
 * </ul>
 * <p>Karena cache itu tidak pernah kedaluwarsa, perubahan pada baris peran ini tidak
 * otomatis terlihat oleh sesi yang sedang berjalan. Dua jalur pemutakhiran yang
 * tersedia adalah {@code Tbmuser.refreshHakAksesUntukRole(Tbmrole)} (untuk perubahan
 * <i>isi</i> peran) dan penulisan langsung ke cache berkunci {@code userId} (untuk
 * <i>perpindahan</i> pengguna ke peran lain). Setiap perubahan peran lewat jalur lain
 * &mdash; SQL langsung, impor massal, atau CRUD generik &mdash; akan menyisakan
 * kewenangan lama sampai proses aplikasi di-<i>restart</i>. Lihat pula
 * {@link Tbmuser#bolehEntryTopupAktif()} yang sengaja memuat ulang baris ini lewat
 * {@link org.hibernate.Session} tersendiri untuk menghindari keusangan itu.</p>
 *
 * <h2>Peran yang di-<i>seed</i> otomatis saat startup</h2>
 * <p>Sejumlah baris peran dibuat idempoten saat aplikasi start, sehingga konstanta di
 * kelas ini praktis dijamin punya baris padanan di basis data: {@link #SPI} oleh
 * {@code MenuHelper.ensureSpiRole()} (dipanggil {@code AppStartupListener}),
 * {@link #DOKTER} oleh {@code MenuHelper.ensureDokterMenus()}, {@link #SISWA} oleh
 * {@code MenuInitializer}, {@link #ORANG_TUA_KODE} oleh
 * {@code CommonLibraryAutoHelper}, serta {@link #PENDUDUK}, {@link #KANTIN},
 * {@link #GURU} dan lainnya oleh {@code InitDataHelper.handleTbmRole(...)}.</p>
 *
 * <h2>Catatan pemeliharaan</h2>
 * <ul>
 *   <li>Menambah kolom {@code Boolean} baru berarti ikut memperbesar tabel audit Envers.
 *   Untuk izin yang jumlahnya akan terus tumbuh, ikuti pola
 *   {@link #getEbisnisMenu()}: satu kolom JSON, bukan kolom per-izin.</li>
 *   <li>Flag baru sebaiknya <i>fail-closed</i> ({@code null} &rarr; {@code false}) dan
 *   <b>tidak</b> menurunkan nilai bawaan dari {@code roleId}/{@code roleName}.</li>
 *   <li>Jangan menambah pemanggilan getter yang menulis balik field; pakai ternary murni
 *   agar entity tidak menjadi <i>dirty</i> hanya karena dibaca.</li>
 * </ul>
 *
 * @see Tbmuser
 * @see Tbmuser#hakAkses()
 * @see Tbmuser#getUserRole()
 * @see Menu
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tbmrole")
public class Tbmrole extends GeneralValueObject implements Comparable<GeneralValueObject> {

	/**
	 * Pengenal peran operator presensi/absensi.
	 *
	 * <p>Satu-satunya pemakainya adalah {@link #getAbsenLangsung()}, yang membandingkannya
	 * dengan {@code equalsIgnoreCase} sehingga baris peran boleh tercatat sebagai
	 * {@code "presensi"} maupun {@code "Presensi"}.</p>
	 *
	 * <p>Perhatikan bahwa penamaan konstanta ini memakai gaya <i>camelCase</i>, berbeda dari
	 * seluruh konstanta lain di kelas ini yang memakai {@code UPPER_SNAKE_CASE}. Ini murni
	 * ketidakkonsistenan historis; nilainya tetap dipakai apa adanya oleh kode yang ada.</p>
	 */
	public static final String Presensi = "Presensi";
	/**
	 * Pengenal peran <b>Administrator</b> &mdash; perhatikan bahwa nilainya adalah
	 * {@code "am"}, <b>bukan</b> {@code "administrator"}.
	 *
	 * <p>Ini konstanta paling berpengaruh di kelas ini: belasan getter memakainya sebagai
	 * satu-satunya peran yang mendapat hak bawaan saat kolomnya masih {@code null} &mdash;
	 * antara lain {@link #getMelihatDataPegawaiLain()}, {@link #getMelihatDataSatkerLain()},
	 * {@link #getMelihatSemuaSurat()}, {@link #getMelihatSemuaSop()},
	 * {@link #getUpdateFormatLaporan()}, {@link #getDashboardKoperasi()},
	 * {@link #getDasborRepository()}, {@link #getDasboardAntarJemput()},
	 * {@link #getTampilkanSpmi()}, dan {@link #getTampilkanGaji()}.</p>
	 *
	 * <p>Ia juga muncul sebagai <i>alternatif</i> pada default berbasis pencocokan persis
	 * dengan {@link #KEUANGAN} di {@link #getKeuangan()}, {@link #getPembayaran()},
	 * {@link #getAkunting()}, dan {@link #getKantin()} &mdash; sejak perbaikan dok audit
	 * 2026-09-06, keempatnya membandingkan {@code roleId} persis, bukan lagi lewat substring.
	 * {@link #getBolehEntryTopup()} sudah tidak lagi membaca konstanta ini sama sekali: nilai
	 * bawaannya kini <i>fail-closed</i> tanpa syarat.</p>
	 *
	 * <p><b>Bukan jalur privilese tertinggi.</b> Peran ini tetap tunduk pada seluruh gerbang
	 * yang membaca kelas ini. Yang benar-benar melewati semuanya adalah
	 * {@link Tbmuser#getRoot()} dan {@code Tbmuser.getSuperadmin()}, yang bekerja di luar
	 * mekanisme {@code Tbmrole} sepenuhnya.</p>
	 *
	 * <p>Karena nilainya sesingkat dua huruf, ia <b>tidak</b> ikut tersentuh oleh pencocokan
	 * (baik dulu secara substring, maupun sekarang secara persis) atas kata {@code "admin"}
	 * pada {@link #defaultAksesFeederSister()} &mdash; peran ini lolos di sana lewat
	 * perbandingan persis pada {@code roleId}, bukan lewat namanya.</p>
	 */
	public static final String ADMINISTRATOR = "am";
	/**
	 * Pengenal peran <b>Keuangan</b>, bernilai {@code "keu"}.
	 *
	 * <p><b>RIWAYAT (FIXED dok audit 2026-09-06).</b> Sampai perbaikan itu, konstanta ini
	 * nyaris tidak pernah dipakai lewat perbandingan persis &mdash; empat getter
	 * ({@link #getKeuangan()}, {@link #getPembayaran()}, {@link #getAkunting()},
	 * {@link #getKantin()}) dan gerbang transaksional {@link #getBolehEntryTopup()} memakai
	 * pencocokan <b>substring</b> {@code getRoleId().toLowerCase().contains("keu")}, sehingga
	 * pengenal apa pun yang sekadar <i>mengandung</i> potongan huruf {@code "keu"} &mdash;
	 * termasuk maksud berbeda seperti {@code "keu_lihat_saja"}, {@code "bekuan"}, atau
	 * {@code "penyekuan"} &mdash; mendapat hak itu secara bawaan.</p>
	 *
	 * <p><b>Sekarang:</b> keempat getter modul memakai
	 * {@code getRoleId().equalsIgnoreCase(KEUANGAN)} (pencocokan persis, tidak peka huruf
	 * besar-kecil), dan {@link #getBolehEntryTopup()} sudah <i>fail-closed</i> tanpa syarat
	 * ({@code null} &rarr; {@code false}) sehingga tidak lagi bergantung pada konstanta ini
	 * sama sekali. Hanya peran berpengenal <b>persis</b> {@code "keu"} (tidak peka huruf
	 * besar-kecil) atau {@link #ADMINISTRATOR} yang memperoleh keempat flag modul itu secara
	 * bawaan.</p>
	 */
	public static final String KEUANGAN = "keu";
	/**
	 * Pengenal peran <b>Akademik</b>.
	 *
	 * <p>Dipakai sebagai peran yang mendapat {@link #getElearning()} dan
	 * {@link #getKegiatanDanPrestasi()} secara bawaan, serta sebagai salah satu dari dua
	 * peran yang lolos perbandingan persis di {@link #defaultAksesFeederSister()} (gerbang
	 * bawaan dasbor Neo Feeder dan SISTER).</p>
	 *
	 * <p>Perhatikan bahwa nilainya berhuruf besar di awal ({@code "Akademik"}), sedangkan
	 * {@link #getElearning()} membandingkannya dengan {@code equalsIgnoreCase} sementara
	 * {@link #defaultAksesFeederSister()} memakai {@code equals} yang peka huruf besar-kecil.
	 * Baris peran yang tersimpan sebagai {@code "akademik"} karena itu tetap mendapat
	 * e-learning namun <b>tidak</b> mendapat akses Feeder/SISTER lewat jalur perbandingan
	 * persis pada {@code roleId} &mdash; ia baru lolos lewat tahap kedua, yang membandingkan
	 * {@code roleName} (atau {@code roleId} sebagai cadangan) persis dengan {@code "akademik"}
	 * setelah dikecilkan hurufnya.</p>
	 */
	public static final String AKADEMIK = "Akademik";
	/**
	 * Pengenal peran <b>Dosen</b>.
	 *
	 * <p>Termasuk kelompok "pengguna akhir" ({@link #roleEndUser()}) bersama
	 * {@link #PEGAWAI}, {@link #GURU}, {@link #SISWA}, dan {@link #MAHASISWA}. Anggota
	 * kelompok ini secara tegas <b>ditolak</b> pada getter berlingkup luas seperti
	 * {@link #getMelihatDataPegawaiLain()}, {@link #getMelihatDataSatkerLain()},
	 * {@link #getMelihatSemuaSurat()}, dan {@link #getMelihatSemuaSop()} &mdash; penolakan
	 * itu <b>mendahului</b> pembacaan kolom, sehingga mencentangnya di layar Grup Pengguna
	 * pun tidak berpengaruh.</p>
	 *
	 * <p>Sebaliknya, peran ini mendapat {@link #getElearning()} dan
	 * {@link #getPresensiKehadiran()} secara paksa (juga mengabaikan nilai kolom), serta
	 * kehilangan {@link #getPengadaan()}, {@link #getKeuangan()}, {@link #getPembayaran()},
	 * {@link #getAkunting()}, dan {@link #getKepegawaian()}.</p>
	 *
	 * <p>Di sisi {@link Tbmuser}, konstanta ini juga dipakai untuk mengenali akun dosen lewat
	 * {@code ambilRolesIdLower()} saat relasi {@code dosen} belum tertaut.</p>
	 */
	public static final String DOSEN = "Dosen";
	/** Role tenaga medis RS/Klinik — mencakup kategori Dokter, Perawat, dan Bidan (satu data {@code sirs.dokter}). */
	public static final String DOKTER = "Dokter";
	/**
	 * Pengenal peran <b>Guru</b> (jenjang sekolah, padanan {@link #DOSEN} di perguruan
	 * tinggi).
	 *
	 * <p>Anggota kelompok {@link #roleEndUser()}, sehingga berlaku baginya seluruh penolakan
	 * paksa dan pemberian paksa yang dijelaskan pada {@link #DOSEN}.</p>
	 *
	 * <p>Baris peran ini termasuk yang di-<i>seed</i> otomatis saat startup oleh
	 * {@code InitDataHelper.handleTbmRole(...)}.</p>
	 */
	public static final String GURU = "Guru";
	/**
	 * Pengenal peran <b>Kantin</b> (operator kasir e-Kantin/POS).
	 *
	 * <p>Peran paling istimewa di kelas ini: ia diperlakukan sebagai <b>daftar-tolak
	 * menyeluruh</b>. Sekitar dua puluh getter diawali blok yang sama persis:</p>
	 * <pre>{@code
	 * if (getRoleId() != null && getRoleId().equals(KANTIN)) {
	 *     return false;
	 * }
	 * }</pre>
	 * <p>Blok itu mengembalikan {@code false} <b>tanpa membaca kolomnya</b>, sehingga
	 * mencentang hak modul apa pun untuk peran ini di layar Grup Pengguna tidak akan
	 * berpengaruh. Maksudnya: akun kasir sengaja dikunci hanya pada modul kantin/POS.</p>
	 *
	 * <p><b>Ketidakkonsistenan yang perlu diketahui.</b> Hampir semua pemakaian di atas
	 * memakai {@code equals} yang <b>peka huruf besar-kecil</b>, sehingga baris peran yang
	 * tersimpan sebagai {@code "kantin"} (huruf kecil) <b>lolos dari seluruh daftar-tolak
	 * itu</b> dan justru memperoleh hak bawaan modul seperti peran biasa. Satu-satunya
	 * pengecualian adalah {@link #getMelihatSemuaSop()}, yang memakai {@link #isRole(String)}
	 * sehingga perbandingannya <i>tidak</i> peka huruf besar-kecil. Bila peran kasir
	 * dibuat manual, pastikan pengenalnya persis {@code "Kantin"}.</p>
	 *
	 * <p>Peran ini juga menjadi satu-satunya yang memperoleh {@link #getHalamanUtama()} dan
	 * {@link #getTampilPos()} secara bawaan.</p>
	 */
	public static final String KANTIN = "Kantin";
	/**
	 * Halaman pendaratan bawaan bagi peran {@link #KANTIN}.
	 *
	 * <p>Dikembalikan {@link #getHalamanUtama()} bila kolom {@code halamanUtama} masih
	 * kosong, lalu dipakai {@code ais.action.servlet.Main} untuk mengalihkan pengguna
	 * seusai login. Ini adalah <i>routing</i>, <b>bukan</b> otorisasi: mengarahkan pengguna
	 * ke halaman ini tidak memberinya hak apa pun, dan sebaliknya mengubah nilai ini tidak
	 * mencabut hak apa pun.</p>
	 */
	public static final String HALAMAN_UTAMA_KANTIN = "/WEB-INF/baru/modul/kantin/index.jsp";
	/**
	 * Pilihan halaman pendaratan modul <b>Apotek</b>.
	 *
	 * <p>Tidak pernah menjadi nilai bawaan; hanya tersedia sebagai salah satu pilihan yang
	 * dapat disimpan administrator ke kolom {@code halamanUtama}. Lihat catatan
	 * {@link #getHalamanUtama()} mengenai mengapa pilihan eksplisit tidak boleh tertimpa
	 * bawaan {@link #KANTIN}.</p>
	 */
	public static final String HALAMAN_UTAMA_APOTIK = "/WEB-INF/baru/modul/apotik/index.jsp";
	/**
	 * Pilihan halaman pendaratan modul <b>eMedik</b> (rumah sakit/klinik).
	 *
	 * <p>Setara {@link #HALAMAN_UTAMA_APOTIK}: pilihan opsional, bukan nilai bawaan.
	 * Perhatikan bahwa halaman yang dituju menegakkan izinnya sendiri lewat
	 * {@link #getEbisnisMenu()}, <b>bukan</b> lewat {@link #getEmedic()}.</p>
	 */
	public static final String HALAMAN_UTAMA_EMEDIK = "/WEB-INF/baru/modul/emedik/index.jsp";
	/**
	 * Pilihan halaman pendaratan modul <b>Inventory</b>.
	 *
	 * <p>Setara {@link #HALAMAN_UTAMA_APOTIK}: pilihan opsional yang disimpan ke kolom
	 * {@code halamanUtama}, bukan nilai bawaan.</p>
	 */
	public static final String HALAMAN_UTAMA_INVENTORY = "/WEB-INF/baru/modul/inventory/index.jsp";
	/**
	 * <b>Pengenal</b> ({@code roleId}) peran Orang Tua/Wali murid, bernilai {@code "ortu"}.
	 *
	 * <p>Perhatikan pemasangannya dengan {@link #ORANG_TUA}: konstanta <i>ini</i> adalah
	 * kunci primer barisnya, sedangkan {@link #ORANG_TUA} adalah nama tampilnya. Keduanya
	 * mudah tertukar karena penamaannya nyaris sama &mdash; gunakan konstanta ini untuk
	 * setiap perbandingan {@code roleId}.</p>
	 *
	 * <p>Baris perannya di-<i>seed</i> otomatis oleh {@code CommonLibraryAutoHelper} dan
	 * {@code InitDataHelper}.</p>
	 */
	public static final String ORANG_TUA_KODE = "ortu";
	/**
	 * <b>Nama tampil</b> ({@code roleName}) peran Orang Tua/Wali murid, bernilai
	 * {@code "Ortu"}.
	 *
	 * <p>Bukan kunci primer &mdash; untuk itu pakai {@link #ORANG_TUA_KODE}. Nilai ini hanya
	 * mengisi kolom {@code rolename} saat baris perannya dibuat oleh penyemai data awal.</p>
	 */
	public static final String ORANG_TUA = "Ortu";
	/**
	 * Pengenal peran <b>Komunitas</b> &mdash; pengguna luar yang terdaftar namun bukan
	 * sivitas akademika maupun pegawai.
	 *
	 * <p>Tidak muncul pada satu pun cabang khusus di getter kelas ini, sehingga seluruh hak
	 * bawaannya mengikuti aturan umum: flag berdefault menyala akan menyala, flag
	 * berdefault mati akan mati.</p>
	 */
	public static final String KOMUNITAS = "Komunitas";
	/**
	 * Pengenal peran <b>Anggota Perpustakaan</b>.
	 *
	 * <p>Peran khusus bagi pemustaka yang tidak memiliki identitas sivitas lain. Seperti
	 * {@link #KOMUNITAS}, ia tidak memiliki cabang khusus di kelas ini; akses pustakanya
	 * datang dari {@link #getPustaka()} yang memang berdefault menyala.</p>
	 */
	public static final String ANGGOTA_PERPUSTAKAAN = "angt_pustaka";
	/**
	 * Pengenal peran <b>Anggota Koperasi</b>.
	 *
	 * <p>Perlu dibedakan dari {@link #getDashboardKoperasi()}, yang merupakan hak
	 * <i>pengelola</i> koperasi dan berdefault hanya untuk {@link #ADMINISTRATOR}. Peran ini
	 * adalah sisi anggota, bukan sisi pengurus.</p>
	 */
	public static final String ANGGOTA_KOPERASI = "angt_koperasi";
	/**
	 * Pengenal peran <b>Peserta Kursus</b> (modul e-learning/kursus).
	 *
	 * <p>Tidak memiliki cabang khusus di kelas ini. Perhatikan bahwa peran ini <b>tidak</b>
	 * termasuk {@link #roleEndUser()}, sehingga &mdash; berbeda dari {@link #SISWA} dan
	 * {@link #MAHASISWA} &mdash; ia tidak ikut ditolak paksa pada getter berlingkup luas.</p>
	 */
	public static final String PESERTA_KURSUS = "peserta";
	/**
	 * Pengenal peran <b>Pegawai</b>, bernilai {@code "peg"} (bukan {@code "pegawai"}).
	 *
	 * <p>Anggota kelompok {@link #roleEndUser()}: ditolak paksa pada
	 * {@link #getMelihatDataPegawaiLain()}, {@link #getMelihatDataSatkerLain()},
	 * {@link #getMelihatSemuaSurat()}, dan {@link #getMelihatSemuaSop()}; diberi paksa
	 * {@link #getPresensiKehadiran()}.</p>
	 *
	 * <p><b>Perhatikan selisih nilai.</b> {@link #getKepegawaian()} menurunkan nilai
	 * bawaannya dari {@code roleId.equalsIgnoreCase("pegawai")} &mdash; perbandingan persis
	 * dengan kata {@code "pegawai"} (sejak perbaikan dok audit 2026-09-06; sebelumnya
	 * pencocokan substring), sedangkan konstanta ini bernilai {@code "peg"}. Karena
	 * {@code "peg"} tidak sama dengan {@code "pegawai"}, peran Pegawai justru <b>tidak</b>
	 * memperoleh hak kepegawaian secara bawaan. Sebelum perbaikan itu, peran bernama seperti
	 * {@code "kepegawaian"} atau {@code "admin_pegawai"} ikut memperolehnya lewat pencocokan
	 * substring; sekarang hanya peran berpengenal <b>persis</b> {@code "pegawai"} (tidak peka
	 * huruf besar-kecil) yang memperolehnya.</p>
	 */
	public static final String PEGAWAI = "peg";
	/**
	 * Pengenal peran <b>Mahasiswa</b>, bernilai {@code "mhs"}.
	 *
	 * <p>Anggota kelompok {@link #roleEndUser()}. Selain penolakan kelompok itu, peran ini
	 * secara khusus dipaksa kehilangan {@link #getPengadaan()}, {@link #getKeuangan()},
	 * {@link #getPembayaran()}, {@link #getAkunting()}, {@link #getKepegawaian()},
	 * {@link #getKinerja()}, dan {@link #getKantin()}; sebaliknya ia dipaksa memperoleh
	 * {@link #getElearning()} dan {@link #getKegiatanDanPrestasi()}.</p>
	 *
	 * <p>Di sisi {@link Tbmuser}, konstanta ini dipakai pada pemeriksaan yang menentukan
	 * apakah akun diperlakukan sebagai mahasiswa &mdash; termasuk jalur yang, pada kondisi
	 * anomali {@link Tbmuser#hakAkses()}, dapat membuat akun mahasiswa yang sah sesaat tidak
	 * dikenali.</p>
	 */
	public static final String MAHASISWA = "mhs";
	/**
	 * Pengenal peran <b>Siswa</b> (jenjang sekolah, padanan {@link #MAHASISWA}).
	 *
	 * <p>Anggota kelompok {@link #roleEndUser()} dengan perlakuan khusus yang sama persis
	 * seperti {@link #MAHASISWA}. Baris perannya di-<i>seed</i> otomatis oleh
	 * {@code MenuInitializer}.</p>
	 */
	public static final String SISWA = "siswa";
	/**
	 * Pengenal peran <b>Penduduk</b> (modul kependudukan/desa).
	 *
	 * <p>Satu-satunya peran yang secara eksplisit <b>dimatikan</b> dari
	 * {@link #getElearning()} dan {@link #getKegiatanDanPrestasi()}. Perhatikan bahwa pada
	 * kedua getter itu penonaktifannya dilakukan dengan <b>menulis {@code false} ke
	 * field</b>, bukan sekadar mengembalikan {@code false} &mdash; salah satu sumber revisi
	 * audit palsu yang dibahas pada dokumentasi kelas.</p>
	 *
	 * <p>Baris perannya di-<i>seed</i> otomatis oleh {@code InitDataHelper}.</p>
	 */
	public static final String PENDUDUK = "penduduk";
	/**
	 * Pengenal peran <b>Penyedia</b> (rekanan/vendor pengadaan).
	 *
	 * <p>Peran ini penting bagi pemahaman anomali otorisasi: {@link Tbmuser#getUserRole()}
	 * memiliki cabang yang <b>menimpa</b> {@code userRole} dengan
	 * {@code ConstantValues.tbmrolePenyedia} bagi akun penyedia aset <b>tanpa memeriksa
	 * {@code null}</b>. Bila cache konstanta belum ter-<i>seed</i>, peran nyata pengguna
	 * terhapus menjadi {@code null} &mdash; jalur paling halus menuju anomali #1 pada
	 * {@link Tbmuser#hakAkses()}.</p>
	 */
	public static final String PENYEDIA = "penyedia";
	/**
	 * Pengenal peran <b>Calon Pegawai</b> (pelamar pada modul rekrutmen).
	 *
	 * <p>Peran berumur pendek yang melekat pada akun pelamar sebelum ia diangkat menjadi
	 * {@link #PEGAWAI}. Tidak memiliki cabang khusus di kelas ini.</p>
	 */
	public static final String CALON_PEGAWAI = "calon_peg";
	/**
	 * Pengenal peran <b>Mahasiswa Pascasarjana</b>, bernilai {@code "mhss2"}.
	 *
	 * <p><b>Perhatian:</b> karena nilainya berbeda dari {@link #MAHASISWA} ({@code "mhs"}),
	 * dan seluruh cabang khusus mahasiswa di kelas ini memakai perbandingan <b>persis</b>
	 * ({@code equals}) terhadap {@code "mhs"}, peran pascasarjana <b>tidak</b> ikut
	 * memperoleh maupun kehilangan hak-hak yang dipaksakan kepada mahasiswa. Ia juga
	 * <b>tidak</b> termasuk {@link #roleEndUser()}. Praktis, peran ini berperilaku seperti
	 * peran biasa dan mengikuti seluruh nilai bawaan umum &mdash; termasuk flag yang
	 * berdefault menyala.</p>
	 */
	public static final String MAHASISWAPASCASARJANA = "mhss2";
	/**
	 * Pengenal peran <b>Dikjar</b> (Pendidikan &amp; Pengajaran).
	 *
	 * <p>Peran struktural pada lingkungan pesantren/yayasan. Tidak memiliki cabang khusus di
	 * kelas ini sehingga sepenuhnya mengikuti nilai bawaan umum.</p>
	 */
	public static final String DIKJAR = "dikjar";
	/**
	 * Role Satuan Pengawasan Internal (SPI/audit internal). Dibuat sebagai role
	 * TERSENDIRI (bukan menumpang di ADMINISTRATOR/AKADEMIK) karena prinsip
	 * "Three Lines Model" (IIA) mensyaratkan fungsi audit internal independen dari
	 * struktur yang diaudit — staf SPI perlu jalur akses sendiri yang tidak otomatis
	 * melekat pada peran akademik/administratif biasa. Baris data role ini di-seed
	 * idempoten saat startup lewat {@code ais.common.MenuHelper.ensureSpiRole()}.
	 */
	public static final String SPI = "SPI";
	/** Role operasional pos keluar/masuk pondok. */
	public static final String KEAMANAN_PONDOK = "keamanan_pondok";

	/**
	 * Membandingkan peran ini dengan {@link GeneralValueObject} lain berdasarkan namanya,
	 * untuk keperluan pengurutan alfabetis di combo/daftar Grup Pengguna.
	 *
	 * <h3>Nilai yang dibandingkan</h3>
	 * <p>Kedua sisi diturunkan secara asimetris:</p>
	 * <ul>
	 *   <li><b>Sisi kiri</b> selalu {@link #getRoleName()} &mdash; yang, ingat, sudah punya
	 *   cadangan jatuh ke {@link #getRoleId()} bila kosong (dan menulis balik hasilnya ke
	 *   field, lihat catatan di bawah).</li>
	 *   <li><b>Sisi kanan</b> memakai {@code getRoleName()} hanya bila argumennya memang
	 *   bertipe {@code Tbmrole}; untuk tipe lain dipakai
	 *   {@link GeneralValueObject#getKeterangan()}.</li>
	 * </ul>
	 * <p>Bila salah satu masih {@code null}, ia diganti dengan {@code getKeterangan()} dari
	 * object yang bersangkutan. Perhatikan bahwa pada cabang penggantian itu sisi kanan
	 * mengambil {@code arg0.getKeterangan()} &mdash; nilai yang sama dengan yang mungkin
	 * sudah dipakai sebelumnya &mdash; sehingga cabang tersebut hanya efektif untuk sisi
	 * kiri.</p>
	 *
	 * <h3>Konsekuensi: urutan yang tidak stabil</h3>
	 * <p>Method ini mengembalikan {@code 0} pada <b>dua</b> keadaan yang sangat berbeda:
	 * ketika kedua nama benar-benar sama, dan ketika salah satunya tidak dapat ditentukan
	 * ({@code null}) atau terjadi exception. Karena {@code 0} berarti "setara", kontrak
	 * {@link Comparable} menjadi tidak konsisten: relasi ini <b>tidak transitif</b> bila ada
	 * peran bernama {@code null} di dalam koleksi (A setara dengan N, N setara dengan B,
	 * padahal A tidak setara dengan B).</p>
	 * <p>Untuk {@link java.util.Collections#sort(java.util.List)} pada Java 7+ yang memakai
	 * TimSort, pelanggaran kontrak semacam ini dapat memicu
	 * {@code IllegalArgumentException: Comparison method violates its general contract!}
	 * pada koleksi berukuran besar. Dalam praktiknya hal itu jarang terjadi di sini karena
	 * {@link #getRoleName()} nyaris selalu berhasil (ia jatuh ke {@code roleId}, yang
	 * merupakan kunci primer dan karena itu tidak boleh kosong).</p>
	 *
	 * <h3>Blok {@code catch} yang menelan exception</h3>
	 * <p>Seluruh badan method dibungkus {@code try/catch} yang hanya mencatat lewat
	 * {@code ErrorAuditUtil} lalu mengembalikan {@code 0}. Ini <i>fail-open</i> terhadap
	 * pengurutan: kegagalan tidak pernah muncul ke pemanggil, hanya menghasilkan urutan yang
	 * tidak terduga. Untuk sebuah daftar pilihan, konsekuensinya kosmetik semata &mdash;
	 * tidak ada keputusan otorisasi yang bergantung pada urutan ini.</p>
	 *
	 * <h3>Efek samping tersembunyi</h3>
	 * <p>Karena memanggil {@link #getRoleName()}, method ini <b>dapat mengubah state</b>
	 * kedua object yang dibandingkan (menulis {@code roleId} ke field {@code roleName} bila
	 * kosong). Artinya mengurutkan sebuah daftar peran yang ter-<i>attach</i> ke
	 * {@link org.hibernate.Session} berpotensi menandai baris-baris itu sebagai
	 * <i>dirty</i>. Lihat pembahasan revisi audit palsu pada dokumentasi kelas.</p>
	 *
	 * <p><b>Tidak konsisten dengan {@code equals}.</b> Kelas ini tidak menimpa
	 * {@code equals}/{@code hashCode}, sehingga kesetaraan object mengikuti perilaku
	 * {@link GeneralValueObject}, sementara {@code compareTo} membandingkan nama. Jangan
	 * memakai {@code Tbmrole} di dalam {@link java.util.TreeSet} atau
	 * {@link java.util.TreeMap} bila duplikasi nama mungkin terjadi &mdash; anggota kedua
	 * akan hilang secara diam-diam.</p>
	 *
	 * @param arg0 object pembanding; boleh {@code null}
	 * @return bilangan negatif/nol/positif sesuai urutan alfabetis nama; {@code 1} bila
	 *         {@code arg0} adalah {@code null}; {@code 0} bila nama tidak dapat ditentukan
	 *         atau terjadi exception
	 * @see #getRoleName()
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			if (arg0 == null) {
				return 1;
			}
			String a = getRoleName();
			String b = arg0 instanceof Tbmrole ? ((Tbmrole) arg0).getRoleName() : arg0.getKeterangan();
			if (a == null) {
				a = getKeterangan();
			}
			if (b == null) {
				b = arg0.getKeterangan();
			}
			if (a != null && b != null) {
				return a.compareTo(b);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tbmrole.java:79");
		}
		return 0;
	}

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya <b>tetap</b> dan tidak boleh diubah. {@link Tbmuser} dan instance
	 * {@code Tbmrole} disimpan di dalam {@link javax.servlet.http.HttpSession} serta pada
	 * beberapa jalur cache, sehingga mengubah nilai ini akan membuat sesi yang sedang
	 * berjalan (dan sesi yang di-<i>persist</i> saat container di-restart) gagal
	 * di-deserialisasi. Kegagalan semacam itu bermuara pada peran yang tidak dapat
	 * di-resolve &mdash; salah satu penyebab anomali {@code null} pada
	 * {@link Tbmuser#hakAkses()}.</p>
	 */
	private static final long serialVersionUID = -4566297457209734092L;
	/**
	 * Kunci primer peran &mdash; kolom {@code roleid}. Lihat {@link #getRoleId()}.
	 *
	 * <p>Bertipe {@link String} dan diisi manual oleh administrator, bukan angka yang
	 * dibangkitkan sistem. Karena nilainya ikut menentukan hak bawaan lewat pencocokan persis
	 * (bukan lagi substring sejak perbaikan dok audit 2026-09-06) pada belasan getter, field
	 * ini bukan sekadar pengenal teknis &mdash; lihat peringatan pada {@link #KEUANGAN} dan
	 * pada dokumentasi kelas.</p>
	 */
	private String roleId;
	/**
	 * Nama pengguna yang terakhir mengubah baris ini &mdash; bagian dari jejak audit
	 * bayangan. Lihat {@link #getOleh()}.
	 */
	private String oleh;
	/**
	 * Pengenal ({@code userId}) pengguna yang terakhir mengubah baris ini &mdash; bagian dari
	 * jejak audit bayangan. Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan pengenal ({@code userId}) pengguna yang terakhir mengubah baris peran
	 * ini.
	 *
	 * <p>Bersama {@link #getOleh()} dan {@link #getTanggal_dirubah()}, method ini membentuk
	 * <b>jejak audit bayangan</b>: tiga kolom "siapa/kapan" yang menempel langsung pada baris
	 * peran, berdampingan dengan riwayat versi lengkap yang sudah dikelola Envers lewat
	 * anotasi {@link Audited @Audited} pada kelas ini. Keduanya berjalan sendiri-sendiri dan
	 * dapat berbeda isi &mdash; lihat {@link #setOlehId(String)} untuk alasannya.</p>
	 *
	 * <p>Getter ini murni: ia mengembalikan field apa adanya tanpa cadangan nilai dan tanpa
	 * menulis balik apa pun, berbeda dari kebanyakan getter lain di kelas ini.</p>
	 *
	 * @return {@code userId} pengubah terakhir, atau {@code null} bila belum pernah terisi
	 * @see #setOlehId(String)
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan pengenal pengguna pengubah terakhir &mdash; <b>menolak nilai kosong secara
	 * diam-diam</b>.
	 *
	 * <p>Bila {@code olehId} bernilai {@code null} atau hanya berisi spasi, setter ini
	 * <b>langsung keluar tanpa melakukan apa pun</b> dan nilai lama tetap bertahan. Perilaku
	 * ini disengaja: ia melindungi jejak audit dari terhapus oleh jalur penyimpanan yang
	 * tidak membawa identitas pengguna (proses latar belakang, penyemai data awal, impor
	 * massal).</p>
	 *
	 * <p><b>Konsekuensi yang harus disadari.</b> Field ini karena itu bersifat
	 * <i>append-only</i> dan <b>tidak dapat dikosongkan lewat API ini</b>. Lebih penting
	 * lagi, nilainya menunjukkan pengubah terakhir <i>yang identitasnya diketahui</i>
	 * &mdash; bukan selalu pengubah terakhir yang sebenarnya. Bila sebuah baris peran diubah
	 * oleh proses tanpa identitas, kolom ini akan tetap menunjuk pengguna sebelumnya,
	 * sehingga <b>menyesatkan bila dibaca sebagai bukti audit</b>. Untuk pertanyaan "siapa
	 * yang mengubah hak akses ini", rujuk tabel revisi Envers, bukan kolom ini.</p>
	 *
	 * @param olehId {@code userId} pengubah; {@code null} atau kosong diabaikan
	 * @see #getOlehId()
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir &mdash; <b>menolak nilai kosong secara
	 * diam-diam</b>.
	 *
	 * <p>Berperilaku persis seperti {@link #setOlehId(String)}: {@code null} atau string
	 * berisi spasi saja diabaikan tanpa peringatan, sehingga nilai lama bertahan. Seluruh
	 * catatan mengenai sifat <i>append-only</i> dan keterbatasannya sebagai bukti audit
	 * berlaku sama di sini.</p>
	 *
	 * @param oleh nama pengubah; {@code null} atau kosong diabaikan
	 * @see #setOlehId(String)
	 * @see #getOleh()
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris peran ini.
	 *
	 * <p>Pasangan {@link #getOlehId()} yang menyimpan nama tampil alih-alih pengenal. Getter
	 * murni tanpa cadangan nilai maupun tulis-balik.</p>
	 *
	 * <p>Karena menyimpan <i>salinan</i> nama, bukan kunci asing ke {@link Tbmuser}, nilainya
	 * tidak ikut berubah bila pengguna yang bersangkutan berganti nama di kemudian hari
	 * &mdash; ini memang sifat yang diinginkan untuk sebuah jejak audit.</p>
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 * @see #setOleh(String)
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat <b>sebelum</b> baris peran ini diperbarui di
	 * basis data.
	 *
	 * <p>Meneruskan ke {@code AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setTanggal_dirubah(Date)} beserta {@link #setOleh(String)} /
	 * {@link #setOlehId(String)} dari konteks pengguna yang sedang berjalan. Inilah yang
	 * membuat jejak audit bayangan terisi otomatis tanpa perlu diingat oleh setiap
	 * pemanggil.</p>
	 *
	 * <p><b>Hanya berlaku untuk pembaruan, bukan penyisipan.</b> Anotasinya adalah
	 * {@code @PreUpdate} saja &mdash; tidak ada {@code @PrePersist} yang menyertainya.
	 * Akibatnya baris peran yang <b>baru dibuat</b> tidak memperoleh {@code oleh}/{@code
	 * olehId} sama sekali; kolom itu baru terisi pada penyuntingan berikutnya. Nilai awal
	 * {@code tanggal_dirubah} tetap ada karena field-nya diinisialisasi langsung pada
	 * deklarasi.</p>
	 *
	 * <p><b>Interaksi dengan getter yang menulis balik field.</b> Karena kelas ini penuh
	 * getter yang mengubah state saat dibaca (lihat dokumentasi kelas), Hibernate dapat
	 * menganggap baris ini <i>dirty</i> meski tidak ada perubahan yang disengaja. Ketika itu
	 * terjadi, kait ini <b>ikut berjalan</b> dan memperbarui stempel waktu serta identitas
	 * pengubah &mdash; menghasilkan jejak audit yang menyatakan seseorang mengubah hak akses
	 * padahal ia hanya membuka sebuah halaman. Ini alasan tambahan mengapa anti-pola
	 * tersebut sangat merugikan pada entity otorisasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir &mdash; kolom {@code tanggal_dirubah}.
	 *
	 * <p>Diinisialisasi langsung pada deklarasi dengan {@code WaktuUtil.getDate()}, sehingga
	 * setiap instance baru (termasuk yang dibuat Hibernate saat memuat baris) membawa waktu
	 * saat itu sebelum nilai sebenarnya dari basis data ditimpakan.</p>
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya <b>tidak</b> dipanggil langsung oleh kode aplikasi &mdash; pengisiannya
	 * ditangani otomatis oleh {@link #onUpdate()} lewat {@code AuditTimestampInterceptor}.
	 * Memanggilnya secara manual akan menimpa stempel yang dikelola sistem.</p>
	 *
	 * <p>Berbeda dari {@link #setOleh(String)} dan {@link #setOlehId(String)}, setter ini
	 * <b>menerima {@code null}</b> tanpa penjagaan apa pun, sehingga stempel waktu justru
	 * <i>dapat</i> dikosongkan sementara identitas pengubah tidak. Ketidaksimetrisan ini
	 * berarti ketiga kolom audit bayangan tidak dijamin konsisten satu sama lain.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; {@code null} diterima
	 * @see #getTanggal_dirubah()
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris peran ini.
	 *
	 * <p>Dipetakan sebagai {@link TemporalType#TIMESTAMP}, sehingga menyimpan tanggal
	 * <i>beserta</i> jam sampai presisi detik/milidetik sesuai kolomnya.</p>
	 *
	 * <p>Getter murni tanpa tulis-balik. Nilainya nyaris tidak pernah {@code null} karena
	 * field-nya sudah terisi sejak deklarasi &mdash; namun ingat bahwa
	 * {@link #setTanggal_dirubah(Date)} memperbolehkan {@code null}, jadi pemanggil tetap
	 * sebaiknya berjaga.</p>
	 *
	 * <p>Sama seperti {@link #getOleh()}, nilai ini dapat bergeser akibat penyimpanan yang
	 * dipicu getter penulis-balik, sehingga ia menunjukkan "kapan baris terakhir ditulis",
	 * bukan selalu "kapan hak akses terakhir diubah manusia".</p>
	 *
	 * @return waktu perubahan terakhir
	 * @see #setTanggal_dirubah(Date)
	 * @see #onUpdate()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nama tampil peran &mdash; kolom {@code rolename}. Lihat {@link #getRoleName()}.
	 *
	 * <p>Bukan sekadar label: {@link #defaultAksesFeederSister()} dan
	 * {@link #getAksesGerbangPesantren()} menurunkan hak bawaannya dengan mencocokkan nilai
	 * field ini persis (tidak peka huruf besar-kecil) terhadap sejumlah kata kunci tetap.</p>
	 */
	private String roleName;
	/**
	 * Daftar kode satuan kerja tambahan dalam satu string dipisah koma &mdash; kolom
	 * bertipe {@code text}. Lihat {@link #getSatuanKerjas()}.
	 *
	 * <p>Berbeda dari relasi tunggal {@link #satuanKerja}, field ini menampung
	 * <b>banyak</b> satuan kerja sekaligus, namun sebagai teks bebas tanpa kunci asing.</p>
	 */
	private String satuanKerjas;
	/**
	 * Kode peran alternatif &mdash; lihat {@link #getKode()}, yang mencadangkannya ke
	 * {@link #getRoleId()} bila kosong.
	 */
	private String kode;
	/**
	 * Penanda peran aktif. Lihat {@link #getAktif()}, yang berdefault menyala.
	 */
	private Boolean aktif;

	/**
	 * Hak modul e-Learning. Lihat {@link #getElearning()} &mdash; bawaan menyala untuk
	 * ADMINISTRATOR/MAHASISWA/DOSEN/AKADEMIK, dipaksa mati untuk PENDUDUK dan KANTIN.
	 */
	private Boolean elearning;
	/**
	 * Hak modul Perpustakaan. Lihat {@link #getPustaka()} &mdash; <b>bawaan menyala untuk
	 * semua peran</b> kecuali KANTIN.
	 */
	private Boolean pustaka;
	/**
	 * Hak melihat dasbor umum &mdash; kolom {@code dashboard_boleh}. Lihat
	 * {@link #getDashboard()}; bawaan menyala untuk semua peran kecuali KANTIN.
	 */
	private Boolean dashboard;
	/**
	 * Hak modul Workflow/persetujuan. Lihat {@link #getWorkflow()} &mdash; bawaan menyala
	 * untuk semua peran kecuali KANTIN.
	 */
	private Boolean workflow;
	/**
	 * Hak modul Kegiatan &amp; Prestasi. Lihat {@link #getKegiatanDanPrestasi()} &mdash; pola
	 * bawaannya sama persis dengan {@link #elearning}.
	 */
	private Boolean kegiatanDanPrestasi;
	/**
	 * Hak modul Administrasi. Lihat {@link #getAdministrasi()} &mdash; bawaan menyala untuk
	 * semua peran kecuali KANTIN.
	 */
	private Boolean administrasi;
	/**
	 * Hak modul Pengadaan. Lihat {@link #getPengadaan()} &mdash; bawaan menyala, namun
	 * dipaksa mati untuk peran peserta didik dan pengajar.
	 */
	private Boolean pengadaan;
	/**
	 * Hak modul Keuangan. Lihat {@link #getKeuangan()} &mdash; bawaannya diturunkan lewat
	 * <b>pencocokan persis</b> ({@code equalsIgnoreCase}) terhadap {@link #KEUANGAN} pada
	 * {@code roleId}; lihat peringatan historis pada {@link #KEUANGAN}.
	 */
	private Boolean keuangan;
	/**
	 * Hak modul Kepegawaian. Lihat {@link #getKepegawaian()} &mdash; bawaannya diturunkan
	 * lewat pencocokan persis ({@code equalsIgnoreCase}) terhadap {@code "pegawai"} pada
	 * {@code roleId}, yang justru <b>tidak</b> cocok dengan konstanta {@link #PEGAWAI}
	 * ({@code "peg"}).
	 */
	private Boolean kepegawaian;
	/**
	 * Hak modul Presensi Kehadiran. Lihat {@link #getPresensiKehadiran()} &mdash; dipaksa
	 * menyala untuk DOSEN/PEGAWAI/GURU tanpa membaca kolom ini.
	 */
	private Boolean presensiKehadiran;
	/**
	 * Hak memproses izin keluar/masuk di pos keamanan pesantren &mdash; kolom
	 * {@code akses_gerbang_pesantren}. Lihat {@link #getAksesGerbangPesantren()}.
	 */
	private Boolean aksesGerbangPesantren;
	/**
	 * Hak menyetujui transaksi member yang melampaui limit &mdash; kolom
	 * {@code boleh_verifikasi_member_melebihi_limit}. Lihat
	 * {@link #getBolehVerifikasiMemberMelebihiLimit()}; <i>fail-closed</i>.
	 */
	private Boolean bolehVerifikasiMemberMelebihiLimit;
	/**
	 * Hak melakukan absensi langsung. Lihat {@link #getAbsenLangsung()} &mdash; bawaan
	 * menyala hanya untuk peran {@link #Presensi}.
	 */
	private Boolean absenLangsung;
	/**
	 * Hak modul Pembayaran. Lihat {@link #getPembayaran()} &mdash; pola bawaannya sama persis
	 * dengan {@link #keuangan}, termasuk pencocokan persis terhadap {@link #KEUANGAN}.
	 */
	private Boolean pembayaran;
	/**
	 * Hak modul Kalender Akademik. Lihat {@link #getKalenderAkademik()} &mdash; bawaan
	 * menyala untuk semua peran <b>kecuali</b> ADMINISTRATOR dan KANTIN.
	 */
	private Boolean kalenderAkademik;
	/**
	 * Hak akses dasbor Neo Feeder &mdash; kolom {@code boleh_akses_feeder}. Lihat
	 * {@link #getBolehAksesFeeder()}; bawaannya diturunkan dari
	 * {@link #defaultAksesFeederSister()} yang mencocokkan <b>nama</b> peran secara persis.
	 */
	private Boolean bolehAksesFeeder;
	/**
	 * Hak akses dasbor SISTER &mdash; kolom {@code boleh_akses_sister}. Lihat
	 * {@link #getBolehAksesSister()}; bawaannya identik dengan {@link #bolehAksesFeeder}.
	 */
	private Boolean bolehAksesSister;
	/**
	 * Hak melihat Info Kegiatan. Lihat {@link #getInfoKegiatan()} &mdash; bawaan menyala
	 * untuk semua peran kecuali ADMINISTRATOR dan KANTIN.
	 */
	private Boolean infoKegiatan;
	/**
	 * Izin <b>MENGELOLA</b> dasbor repository &mdash; kolom {@code repository}. Lihat
	 * {@link #getDasborRepository()}; bawaan hanya untuk ADMINISTRATOR. Bandingkan dengan
	 * {@link #bacaRepository} yang merupakan izin membaca.
	 */
	private Boolean dasborRepository;
	/**
	 * Izin MEMBACA artefak repository di aplikasi mobile/desktop.
	 * Sengaja terpisah dari {@link #dasborRepository} yang merupakan izin
	 * MENGELOLA (bawaannya administrator saja): membaca artefak yang sudah
	 * terpublikasi terbuka untuk semua grup kecuali dimatikan.
	 */
	private Boolean bacaRepository;
	/**
	 * Hak melihat dasbor Antar Jemput &mdash; kolom {@code antar_jemput}. Lihat
	 * {@link #getDasboardAntarJemput()}; bawaan hanya untuk ADMINISTRATOR.
	 *
	 * <p>Perhatikan salah eja historis pada nama field ({@code dasboard}, bukan
	 * {@code dashboard}) yang ikut terbawa ke nama getter/setter publiknya. Jangan
	 * diperbaiki tanpa menyisir seluruh pemanggil dan pemetaan ZK.</p>
	 */
	private Boolean dasboardAntarJemput;
	/**
	 * Hak melihat modul SPMI (penjaminan mutu internal) &mdash; kolom {@code spmi}. Lihat
	 * {@link #getTampilkanSpmi()}; bawaan hanya untuk ADMINISTRATOR.
	 */
	private Boolean tampilkanSpmi;
	/**
	 * Hak melihat modul Gaji &mdash; kolom {@code gaji}. Lihat {@link #getTampilkanGaji()};
	 * bawaan hanya untuk ADMINISTRATOR.
	 */
	private Boolean tampilkanGaji;
	/**
	 * Hak melihat data pegawai selain dirinya sendiri. Lihat
	 * {@link #getMelihatDataPegawaiLain()} &mdash; salah satu flag yang benar-benar menjadi
	 * <b>gerbang pembatas data</b> di sisi server, bukan sekadar visibilitas menu.
	 */
	private Boolean melihatDataPegawaiLain;
	/**
	 * Hak mengajukan pengajuan atas nama pegawai lain. Lihat
	 * {@link #getMengajukanPengajuanPegawaiLain()}; <i>fail-closed</i>.
	 */
	private Boolean mengajukanPengajuanPegawaiLain;
	/**
	 * Hak melihat data satuan kerja selain miliknya. Lihat
	 * {@link #getMelihatDataSatkerLain()} &mdash; gerbang pembatas data lintas unit yang
	 * nyata di sisi server.
	 */
	private Boolean melihatDataSatkerLain;
	/**
	 * Hak melihat seluruh surat, bukan hanya yang dikonsep sendiri. Lihat
	 * {@link #getMelihatSemuaSurat()} &mdash; gerbang pembatas data yang nyata.
	 */
	private Boolean melihatSemuaSurat;
	/**
	 * Hak melihat seluruh SOP. Lihat {@link #getMelihatSemuaSop()} &mdash; gerbang pembatas
	 * data yang nyata.
	 */
	private Boolean melihatSemuaSop;
	/**
	 * Hak menyunting format/template laporan. Lihat {@link #getUpdateFormatLaporan()};
	 * bawaan hanya untuk ADMINISTRATOR.
	 */
	private Boolean updateFormatLaporan;
	/**
	 * Lingkup Program studi yang melekat pada peran ini. Lihat {@link #getProgram()}.
	 *
	 * <p>Bagian dari kelompok lingkup organisasi yang <b>menimpa</b> lingkup pribadi setiap
	 * akun pemakai peran ini &mdash; lihat {@link Tbmuser#ambilProgram()}.</p>
	 */
	private Program program;

	/**
	 * Lingkup Jurusan yang melekat pada peran ini. Lihat {@link #getJurusan()} dan
	 * {@link Tbmuser#ambilJurusan()}.
	 */
	private Jurusan jurusan;
	/**
	 * Lingkup Fakultas yang melekat pada peran ini. Lihat {@link #getFakultas()} &mdash;
	 * nilainya <b>diturunkan dari {@link #jurusan}</b> bila jurusan terisi.
	 */
	private Fakultas fakultas;
	/**
	 * Lingkup Yayasan yang melekat pada peran ini. Lihat {@link #getYayasan()} &mdash;
	 * nilainya <b>diturunkan dari {@link #sekolah}</b> bila sekolah terisi.
	 */
	private Yayasan yayasan;
	/**
	 * Lingkup Sekolah yang melekat pada peran ini. Lihat {@link #getSekolah()}.
	 */
	private Sekolah sekolah;
	/**
	 * Jenis jabatan yang diasosiasikan dengan peran ini. Lihat {@link #getJenisJabatan()}
	 * &mdash; data referensi kepegawaian, bukan gerbang otorisasi.
	 */
	private JenisJabatan jenisJabatan;
	/**
	 * Halaman pendaratan seusai login. Lihat {@link #getHalamanUtama()} &mdash;
	 * <i>routing</i>, bukan otorisasi.
	 */
	private String halamanUtama;
	/**
	 * Dasbor bawaan yang ditampilkan di halaman utama &mdash; kolom
	 * {@code dashboard_default_main}. Lihat {@link #getDashboardDefaultMain()}; pemilihan
	 * tampilan, bukan otorisasi.
	 */
	private String dashboardDefaultMain;

	/**
	 * Mengembalikan nama peran sebagai representasi teks object ini.
	 *
	 * <p>Dipakai luas oleh komponen ZK (combo, daftar pilihan Grup Pengguna) yang menampilkan
	 * object apa adanya, sehingga isinya terlihat langsung oleh pengguna akhir.</p>
	 *
	 * <p><b>Membaca field mentah, bukan getter.</b> Method ini mengembalikan {@code roleName}
	 * secara langsung dan <b>tidak</b> memanggil {@link #getRoleName()}. Konsekuensinya ia
	 * <b>dapat mengembalikan {@code null}</b> untuk peran yang kolom {@code rolename}-nya
	 * kosong &mdash; justru kasus yang sudah ditangani {@link #getRoleName()} lewat cadangan
	 * jatuh ke {@link #getRoleId()}. Pada perangkaian string, {@code null} itu akan tampil
	 * sebagai teks {@code "null"} di layar.</p>
	 *
	 * <p>Perbedaan ini kemungkinan besar disengaja, bukan kelalaian: memanggil
	 * {@link #getRoleName()} akan <b>menulis balik</b> nilai ke field dan menandai entity
	 * sebagai <i>dirty</i>. Karena {@code toString()} dapat terpanggil kapan saja &mdash;
	 * termasuk oleh <i>debugger</i>, kerangka kerja pencatat log, dan Hibernate sendiri
	 * &mdash; efek samping semacam itu akan sangat sulit dilacak. Membaca field mentah adalah
	 * pilihan yang lebih aman di sini.</p>
	 *
	 * <p>Bila membutuhkan nama yang dijamin terisi, panggil {@link #getRoleName()} secara
	 * eksplisit alih-alih mengandalkan method ini.</p>
	 *
	 * @return nama peran, atau {@code null} bila kolom {@code rolename} kosong
	 * @see #getRoleName()
	 */
	public String toString() {
		return roleName;
	}

	/**
	 * Kumpulan {@link Menu} yang di-<i>assign</i> ke peran ini &mdash; tabel penghubung
	 * {@code job_has_menu}. Lihat {@link #getMenus()}.
	 *
	 * <p>Diinisialisasi ke {@link HashSet} kosong agar tidak pernah {@code null} pada object
	 * yang baru dibuat. Perhatikan bahwa untuk object yang <i>dimuat Hibernate</i>, field ini
	 * akan digantikan koleksi <i>lazy</i> milik Hibernate &mdash; sumber
	 * {@link org.hibernate.LazyInitializationException} yang dibahas pada
	 * {@link #getMenus()}.</p>
	 */
	private Set<Menu> menus = new HashSet<Menu>();
	/**
	 * Hak modul Akuntansi. Lihat {@link #getAkunting()} &mdash; pola bawaannya sama persis
	 * dengan {@link #keuangan}, termasuk pencocokan persis terhadap {@link #KEUANGAN}.
	 */
	private Boolean akunting;
	/**
	 * Hak modul Kinerja. Lihat {@link #getKinerja()} &mdash; bawaan menyala untuk semua peran
	 * kecuali KANTIN, SISWA, dan MAHASISWA.
	 */
	private Boolean kinerja;
	/**
	 * Hak modul Kantin/e-Kantin. Lihat {@link #getKantin()} &mdash; bawaannya diturunkan
	 * lewat pencocokan persis terhadap {@link #KEUANGAN}, dan merupakan satu-satunya flag
	 * yang <b>tidak</b> memiliki daftar-tolak {@link #KANTIN}.
	 */
	private Boolean kantin;
	/**
	 * Penanda tampil pintasan Dasbor POS &mdash; kolom {@code tampil_pos}. Lihat
	 * {@link #getTampilPos()}; <b>hanya visibilitas ikon</b>, otorisasi POS yang sebenarnya
	 * ada di {@link #ebisnisMenu}.
	 */
	private Boolean tampilPos;
	/**
	 * Penanda tampil pintasan dasbor Koperasi &mdash; kolom {@code dashboard_koperasi}.
	 * Lihat {@link #getDashboardKoperasi()}; bawaan hanya untuk ADMINISTRATOR.
	 */
	private Boolean dashboardKoperasi;
	/**
	 * Penanda tampil pintasan dasbor eMedic &mdash; lihat {@link #getEmedic()}.
	 * <b>Hanya visibilitas ikon</b>: halaman eMedik menegakkan izinnya lewat
	 * {@link #ebisnisMenu}, bukan lewat flag ini.
	 */
	private Boolean emedic;
	/**
	 * Hak melakukan entri topup saldo. Lihat {@link #getBolehEntryTopup()} &mdash; gerbang
	 * <b>transaksional sungguhan</b>. Sejak perbaikan dok audit 2026-09-06, bawaannya
	 * <i>fail-closed</i> tanpa syarat ({@code null} &rarr; {@code false}) dan tidak lagi
	 * diturunkan dari {@code roleId}; lihat riwayatnya di {@link #KEUANGAN}.
	 */
	private Boolean bolehEntryTopup;
	/**
	 * Satu kolom JSON konsolidasi utk SEMUA hak akses menu POS/e-Kantin (Kasir Desktop, Kasir Android,
	 * JSP e-Kantin) per Grup Pengguna (role) -- MENGGANTIKAN 26 kolom Boolean {@code akses*} terpisah
	 * yang sebelumnya ada di sini (akses_supervisor_kantin, akses_kasir, akses_beranda_kantin,
	 * akses_ringkasan, ..., kantin_member_landing_page) dan TIDAK PERNAH terpakai UI/dispatcher apa pun
	 * (dikonfirmasi kosong sebelum dihapus -- lihat migrasi SQL {@code
	 * migrasi_ebisnis_menu_konsolidasi.sql}). Lihat {@link ais.common.EbisnisMenuKatalog} utk daftar
	 * lengkap kunci menu yang dikenal &amp; makna tiap field JSON-nya.
	 *
	 * <p>Alasan konsolidasi jadi SATU kolom (bukan 26+ kolom terpisah spt sebelumnya): jumlah menu
	 * eBisnis akan terus bertambah seiring platform POS berkembang jadi ERP (lihat dokumen strategi
	 * "MASTER_PROMPT_CODEX_CLAUDE_EBISNIS_ID.md" -- modul Finance/Inventory/HR/CRM/dll direncanakan
	 * menyusul) -- menambah kolom Boolean baru tiap kali ada menu baru tidak scalable &amp; tiap
	 * penambahan wajib migrasi tabel audit Envers (lihat catatan gotcha di hibernate.cfg.xml). Satu
	 * kolom JSON extensible tanpa ALTER TABLE lagi.</p>
	 */
	private String ebisnisMenu;
	/**
	 * Daftar toko yang boleh diakses peran ini, dalam bentuk JSON &mdash; kolom
	 * {@code toko_akses_json}. Lihat {@link #getTokoAksesJson()}; gerbang pembatas data
	 * yang nyata di {@code PosApi}/{@code KantinHelper}.
	 */
	private String tokoAksesJson;
	/**
	 * Hak akses modul Jurnal Ilmiah dalam bentuk JSON &mdash; kolom
	 * {@code jurnal_akses_json}. Lihat {@link #getJurnalAksesJson()}; ditafsirkan
	 * <b>hanya</b> lewat {@code JurnalAksesKatalog}.
	 */
	private String jurnalAksesJson;
	/**
	 * Izin melihat seluruh toko aktif &mdash; kolom {@code boleh_lihat_semua_toko}. Lihat
	 * {@link #getBolehLihatSemuaToko()}; <i>fail-closed</i>.
	 */
	private Boolean bolehLihatSemuaToko;
	/**
	 * Lingkup Satuan Kerja tunggal yang melekat pada peran ini &mdash; kolom
	 * {@code satuan_kerja}. Lihat {@link #getSatuanKerja()}.
	 *
	 * <p>Bandingkan dengan {@link #satuanKerjas} (berakhiran {@code s}) yang menampung
	 * <b>banyak</b> kode satuan kerja sebagai teks dipisah koma. Keduanya berdampingan dan
	 * mudah tertukar.</p>
	 */
	private SatuanKerja satuanKerja;

	/**
	 * Mengembalikan kumpulan {@link Menu} navigasi yang di-<i>assign</i> ke peran ini.
	 *
	 * <p>Ini adalah <b>lapisan izin kedua</b> dari empat yang dijelaskan pada dokumentasi
	 * kelas. Ia menjawab pertanyaan "menu apa yang <i>tampil</i> bagi peran ini", dan
	 * dipetakan ke tabel penghubung {@code job_has_menu} dengan kolom {@code job} menunjuk
	 * {@code roleid} serta kolom {@code menu} menunjuk {@code menu.id}.</p>
	 *
	 * <h3>Yang TIDAK dijamin oleh koleksi ini</h3>
	 * <p>Koleksi ini mengatur <b>visibilitas navigasi</b>, bukan kewenangan aksi. Izin
	 * <i>create/update/delete/approve</i> ditentukan entity <b>terpisah</b>
	 * {@code RolePrivilage} yang dibaca {@code CommonPrivilages}. Kedua lapisan itu
	 * <b>tidak saling memvalidasi</b>: sebuah baris {@code RolePrivilage} dapat eksis untuk
	 * pasangan peran&times;menu yang <i>tidak pernah</i> terdaftar di sini. Karena itu
	 * mencabut sebuah menu dari koleksi ini <b>tidak dengan sendirinya mencabut</b>
	 * kewenangan CRUD atasnya &mdash; ia hanya menghilangkan pintu masuk dari menu. Bila
	 * tujuannya benar-benar mencabut hak, baris {@code RolePrivilage} yang bersangkutan
	 * harus ikut dihapus.</p>
	 *
	 * <h3>Tidak ada pewarisan induk&ndash;anak</h3>
	 * <p>Penyimpanan assignment adalah salinan harfiah dari centang di layar: tidak ada kode
	 * yang menambahkan menu induk karena anaknya dicentang, maupun sebaliknya. {@link Menu}
	 * memang memiliki {@code root} dan {@code child}, tetapi keduanya hanya dipakai
	 * <i>perender</i> untuk menyusun pohon, bukan untuk menurunkan hak. Efek praktisnya
	 * berbeda-beda per perender: menu anak yang di-assign tanpa induknya akan <b>hilang</b>
	 * pada perender mega-menu dan pohon lama, namun diselamatkan ke grup virtual
	 * "Menu Lainnya" pada perender antarmuka baru. Ini kerap disalahartikan sebagai
	 * pewarisan; ia bukan.</p>
	 *
	 * <h3>Pengurutan</h3>
	 * <p>{@link OrderBy @OrderBy} meminta basis data mengurutkan berdasarkan
	 * {@code nomorUrut}, lalu {@code root}, lalu {@code child}. Perhatikan bahwa tipe
	 * kembaliannya adalah {@link Set}, yang <b>tidak menjamin urutan</b> &mdash; urutan hasil
	 * baru benar-benar terpakai bila Hibernate mengembalikan implementasi yang
	 * mempertahankannya. Kode perender yang memerlukan urutan pasti tetap menyalin isinya ke
	 * {@link java.util.List} lalu mengurutkannya sendiri.</p>
	 *
	 * <h3>PERINGATAN: koleksi ini lazy dan sering ter-detach</h3>
	 * <p>{@link javax.persistence.ManyToMany @ManyToMany} bersifat {@code LAZY} secara
	 * bawaan. Inilah sumber langsung <b>anomali #5</b> yang didokumentasikan pada
	 * {@link Tbmuser#hakAkses()}: cache peran {@code Tbmuser.getUserRoleYgDipakai} menyimpan
	 * instance {@code Tbmrole} tanpa batas waktu, sehingga {@code hakAkses()} dapat
	 * mengembalikan object yang <b>tampak sah namun melempar
	 * {@link org.hibernate.LazyInitializationException} begitu method ini disentuh</b>.
	 * Karena banyak pemanggil membungkusnya dalam {@code try/catch} yang menelan exception,
	 * di lapisan atas kondisi itu tidak dapat dibedakan dari "peran tanpa menu".</p>
	 * <p>Pola aman yang sudah dipakai kode yang ada, dan <b>wajib diikuti</b> kode baru:</p>
	 * <ul>
	 *   <li>periksa {@code Hibernate.isInitialized(role.getMenus())} sebelum menyentuhnya
	 *   (dilakukan {@code MainMenuHelper}); atau</li>
	 *   <li>panggil {@code session.refresh(role)} lebih dulu lalu salin isinya ke koleksi
	 *   baru (dilakukan {@code MenuHelper.loadTree}); atau</li>
	 *   <li>muat lewat query eksplisit
	 *   {@code select distinct m from Tbmrole r join r.menus m where r.roleId = :roleId}
	 *   (dilakukan layanan menu antarmuka baru).</li>
	 * </ul>
	 *
	 * <h3>Cascade</h3>
	 * <p>Hanya {@link CascadeType#MERGE}. Tidak ada {@code PERSIST}, {@code REMOVE}, maupun
	 * {@code orphanRemoval}, sehingga menambahkan {@link Menu} yang belum tersimpan ke
	 * koleksi ini tidak akan menyimpannya, dan menghapus peran tidak akan menghapus baris
	 * {@link Menu} mana pun &mdash; hanya baris penghubungnya. Itu memang perilaku yang
	 * diinginkan: {@link Menu} adalah data induk bersama antar peran.</p>
	 *
	 * @return kumpulan menu yang di-assign; tidak pernah {@code null}, tetapi dapat berupa
	 *         koleksi lazy yang belum terinisialisasi
	 * @see #setMenus(Set)
	 * @see Menu
	 * @see Tbmuser#hakAkses()
	 */
	@ManyToMany(targetEntity = Menu.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nomorUrut asc, root asc, child asc")
	@JoinTable(name = "job_has_menu", joinColumns = @JoinColumn(name = "job"), inverseJoinColumns = @JoinColumn(name = "menu"))
	public Set<Menu> getMenus() {
		return menus;
	}

	/**
	 * Menetapkan kumpulan menu yang di-<i>assign</i> ke peran ini.
	 *
	 * <p>Argumen {@code null} diterjemahkan menjadi {@link HashSet} kosong alih-alih
	 * disimpan apa adanya, sehingga {@link #getMenus()} dijamin tidak pernah mengembalikan
	 * {@code null}. Perhatikan bahwa ini berarti {@code null} dan "kosong" tidak dapat
	 * dibedakan &mdash; keduanya berarti <b>peran tanpa satu pun menu</b>, yang pada
	 * perender akan tampil sebagai navigasi kosong.</p>
	 *
	 * <p><b>Menggantikan, bukan menggabungkan.</b> Layar Grup Pengguna memanfaatkan sifat ini
	 * dengan pola "kosongkan lalu isi ulang": ia memanggil setter ini dengan himpunan kosong,
	 * lalu menambahkan kembali setiap menu yang tercentang. Karena itu setiap penyimpanan
	 * adalah gambaran utuh keadaan centang, bukan penambahan bertahap &mdash; menu yang tidak
	 * tercentang otomatis tercabut.</p>
	 *
	 * <p><b>Ingat batas kewenangannya.</b> Seperti dijelaskan pada {@link #getMenus()},
	 * mencabut menu di sini tidak mencabut baris {@code RolePrivilage} yang bersangkutan.
	 * Untuk benar-benar menghapus kewenangan aksi, keduanya harus ditangani.</p>
	 *
	 * @param menus kumpulan menu baru; {@code null} diperlakukan sebagai himpunan kosong
	 * @see #getMenus()
	 */
	public void setMenus(Set<Menu> menus) {
		this.menus = menus == null ? new HashSet<Menu>() : menus;
	}

	/**
	 * Konstruktor tanpa argumen &mdash; <b>wajib ada</b> agar Hibernate/JPA dapat
	 * meng-instansiasi entity ini saat memuat baris dari basis data.
	 *
	 * <p>Object hasil konstruktor ini belum memiliki {@code roleId}, sehingga
	 * {@link #getRoleId()} mengembalikan {@code null}. Perlu diingat bahwa hampir seluruh
	 * getter flag menurunkan nilai bawaannya dari {@code roleId}; dengan {@code roleId}
	 * kosong, semua cabang khusus per-peran terlewati dan yang berlaku adalah nilai bawaan
	 * umum &mdash; termasuk flag yang <b>berdefault menyala</b>. Sebuah {@code new Tbmrole()}
	 * karena itu bukan peran "tanpa hak apa pun".</p>
	 *
	 * @see #Tbmrole(String)
	 */
	public Tbmrole() {
	}

	/**
	 * Membuat peran dengan pengenal tertentu.
	 *
	 * <p>Terutama berguna untuk membentuk object rujukan ringan pada kriteria query
	 * (mis. {@code Restrictions.eq("role", new Tbmrole(Tbmrole.ADMINISTRATOR))}) tanpa perlu
	 * memuat barisnya dari basis data.</p>
	 *
	 * <p>Nilai ditulis <b>langsung ke field</b>, melewati {@link #setRoleId(String)}. Dalam
	 * hal ini tidak ada bedanya karena setter tersebut memang tidak melakukan penjagaan
	 * apa pun.</p>
	 *
	 * <p>Karena {@code roleId} sudah terisi, seluruh cabang penurunan hak bawaan berbasis
	 * pengenal <b>aktif</b> pada object ini &mdash; termasuk pencocokan persis terhadap
	 * {@link #KEUANGAN} ({@code "keu"}) dan {@code "pegawai"}. Sebuah object yang dibuat
	 * hanya sebagai rujukan query dengan demikian tetap akan menjawab pertanyaan hak
	 * seolah-olah ia peran sungguhan, meski tidak ada satu pun kolomnya yang dimuat.</p>
	 *
	 * @param roleId pengenal peran, menjadi kunci primer
	 * @see #Tbmrole(String, String)
	 */
	public Tbmrole(String roleId) {
		this.roleId = roleId;

	}

	/**
	 * Membuat peran dengan pengenal sekaligus nama tampilnya.
	 *
	 * <p>Dipakai jalur penyemai data awal ({@code InitDataHelper}, {@code MenuHelper},
	 * {@code MenuInitializer}) yang membuat baris peran bawaan saat aplikasi pertama kali
	 * dijalankan.</p>
	 *
	 * <p>Kedua nilai ditulis langsung ke field, melewati setter &mdash; tanpa perbedaan
	 * perilaku karena kedua setter itu memang tidak melakukan penjagaan.</p>
	 *
	 * <p>Dengan {@code roleName} terisi, penurunan hak bawaan berbasis <b>nama</b> ikut
	 * aktif: {@link #defaultAksesFeederSister()} dan {@link #getAksesGerbangPesantren()}
	 * mencocokkan nilai itu <b>persis</b> (tidak peka huruf besar-kecil) terhadap kata kunci
	 * tetap seperti {@code "admin"}/{@code "akademik"} atau {@code "satpam"}. Memberi peran
	 * baru nama yang <b>persis sama</b> dengan salah satu kata kunci itu &mdash; mis.
	 * {@code "Admin"} &mdash; karena itu akan diam-diam mengaktifkan akses dasbor Feeder dan
	 * SISTER; nama yang sekadar mengandungnya, seperti {@code "Administrasi Umum"}, tidak lagi
	 * cukup sejak perbaikan dok audit 2026-09-06. Pilih nama dengan sadar, atau isi flag
	 * terkait secara eksplisit.</p>
	 *
	 * @param roleId   pengenal peran, menjadi kunci primer
	 * @param roleName nama tampil peran
	 * @see #Tbmrole(String)
	 */
	public Tbmrole(String roleId, String roleName) {
		this.roleId = roleId;
		this.roleName = roleName;
	}

	/**
	 * Mengembalikan pengenal peran &mdash; kunci primer entity ini.
	 *
	 * <p>Berbeda dari kebanyakan entity AIS yang memakai kunci primer numerik yang
	 * dibangkitkan sistem, kunci di sini adalah {@link String} yang <b>diisi manual oleh
	 * administrator</b> saat membuat Grup Pengguna. Konstanta-konstanta di bagian atas kelas
	 * ini ({@link #ADMINISTRATOR}, {@link #MAHASISWA}, {@link #KANTIN}, dan seterusnya)
	 * adalah nilai-nilai yang sudah ditetapkan sistem.</p>
	 *
	 * <h3>Bukan sekadar pengenal &mdash; ia menentukan hak</h3>
	 * <p>Nilai method ini dibaca oleh <b>hampir seluruh getter flag</b> di kelas ini untuk
	 * menurunkan nilai bawaan ketika kolom yang bersangkutan masih {@code null}. Ada dua
	 * gaya pemakaian saat ini:</p>
	 * <ul>
	 *   <li><b>Perbandingan persis</b> ({@code equals}) &mdash; mis. daftar-tolak
	 *   {@link #KANTIN} dan pemberian hak bawaan bagi {@link #ADMINISTRATOR}. Peka huruf
	 *   besar-kecil.</li>
	 *   <li><b>Perbandingan tanpa peduli huruf</b> ({@code equalsIgnoreCase}) &mdash; lewat
	 *   {@link #isRole(String)}, pada {@link #getElearning()}, dan sejak perbaikan dok audit
	 *   2026-09-06 juga pada {@link #getKeuangan()}, {@link #getPembayaran()},
	 *   {@link #getAkunting()}, {@link #getKantin()}, dan {@link #getKepegawaian()} (lihat
	 *   peringatan historis pada {@link #KEUANGAN}).</li>
	 * </ul>
	 * <p><b>RIWAYAT:</b> sebelum perbaikan itu, kelima getter modul di atas memakai
	 * <b>pencocokan substring</b> ({@code toLowerCase().contains(...)}) alih-alih perbandingan
	 * persis, sehingga pengenal yang sekadar <i>mengandung</i> {@code "keu"} memperoleh lima
	 * hak keuangan secara bawaan &mdash; termasuk gerbang transaksional
	 * {@link #getBolehEntryTopup()}, yang sejak perbaikan itu sudah <i>fail-closed</i> tanpa
	 * syarat dan tidak lagi membaca {@code roleId} sama sekali.</p>
	 * <p>Karena itu <b>mengganti nama pengenal sebuah peran bukan perubahan kosmetik</b>: ia
	 * dapat menambah atau mencabut hak pada setiap kolom yang masih {@code null}.</p>
	 *
	 * <h3>Normalisasi</h3>
	 * <p>Getter ini memangkas spasi di kedua ujung dan mengembalikan {@code null} untuk nilai
	 * kosong. Berbeda dari banyak getter lain di kelas ini, ia <b>tidak menulis balik</b>
	 * hasil pangkasan ke field &mdash; sifat murni yang penting, karena Hibernate memanggil
	 * getter kunci primer di tengah proses memuat entity.</p>
	 *
	 * <p><b>Konsekuensi: normalisasi ini tidak permanen.</b> Karena hasilnya tidak disimpan,
	 * nilai yang <i>ditulis</i> ke basis data tetap versi mentah yang mungkin berspasi
	 * &mdash; {@link #setRoleId(String)} tidak memangkas apa pun. Akibatnya kunci primer di
	 * basis data dapat berisi spasi tersembunyi sementara seluruh perbandingan di dalam Java
	 * memakai versi terpangkas. Untuk pencocokan yang peka huruf (mis. daftar-tolak
	 * {@link #KANTIN}) selisih semacam ini dapat membuat peran lolos dari penjagaan yang
	 * seharusnya berlaku.</p>
	 *
	 * <p>Meski kolomnya {@code nullable=false}, method ini <b>dapat mengembalikan
	 * {@code null}</b> untuk object yang belum tersimpan atau yang dibuat lewat
	 * {@link #Tbmrole()}.</p>
	 *
	 * @return pengenal peran yang sudah dipangkas, atau {@code null} bila kosong
	 * @see #setRoleId(String)
	 * @see #getKode()
	 */
	@Id
	@Column(name = "roleid", unique = true, nullable = false, length = 255)
	public String getRoleId() {
		return this.roleId == null || roleId.trim().isEmpty() ? null : roleId.trim();
	}

	/**
	 * Menetapkan pengenal peran (kunci primer).
	 *
	 * <p>Setter mentah tanpa penjagaan apa pun: tidak memangkas spasi, tidak menolak
	 * {@code null}, dan tidak memvalidasi keunikan. Pemangkasan hanya terjadi di sisi baca
	 * ({@link #getRoleId()}), sehingga nilai berspasi akan tersimpan apa adanya ke basis
	 * data &mdash; lihat peringatan pada getter tersebut.</p>
	 *
	 * <p><b>Mengubah nilai ini pada baris yang sudah tersimpan adalah operasi berbahaya.</b>
	 * Selain merupakan perubahan kunci primer (yang di Hibernate berarti baris baru, bukan
	 * pembaruan), nilai ini dirujuk oleh:</p>
	 * <ul>
	 *   <li>kolom {@code tbmuser.userrole} pada setiap akun yang memakainya;</li>
	 *   <li>kolom {@code job} pada tabel penghubung {@code job_has_menu};</li>
	 *   <li>baris {@code RolePrivilage} yang menyimpan kewenangan CRUD;</li>
	 *   <li>seluruh cabang penurunan hak bawaan di kelas ini &mdash; sehingga pengenal baru
	 *   dapat memberi atau mencabut hak yang sama sekali tidak diniatkan.</li>
	 * </ul>
	 * <p>Untuk mengganti nama yang terlihat pengguna, ubah {@link #setRoleName(String)}
	 * saja.</p>
	 *
	 * @param roleId pengenal peran; disimpan apa adanya
	 * @see #getRoleId()
	 */
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	/**
	 * Mengembalikan nama tampil peran, dengan cadangan jatuh ke {@link #getRoleId()}.
	 *
	 * <p>Bila kolom {@code rolename} kosong atau hanya berisi spasi, method ini memakai
	 * pengenal peran sebagai gantinya sehingga daftar pilihan Grup Pengguna tidak pernah
	 * menampilkan baris kosong. Praktisnya nilai kembalian hampir tidak pernah {@code null},
	 * karena {@code roleId} adalah kunci primer.</p>
	 *
	 * <h3>PERHATIAN: getter ini mengubah state</h3>
	 * <p>Nilai cadangan <b>ditulis balik ke field</b> {@code roleName}, bukan sekadar
	 * dikembalikan. Untuk instance yang ter-<i>attach</i> ke {@link org.hibernate.Session},
	 * itu berarti sekadar <b>membaca</b> nama peran akan menandai barisnya sebagai
	 * <i>dirty</i>, memicu {@code UPDATE} pada {@code flush} berikutnya, menjalankan
	 * {@link #onUpdate()}, dan menerbitkan <b>revisi audit Envers palsu</b>. Karena entity
	 * ini adalah entity otorisasi, jejak audit yang dihasilkannya akan terlihat seperti
	 * "seseorang mengubah hak akses" padahal tidak ada yang berubah.</p>
	 * <p>Efeknya juga <b>permanen di basis data</b>: setelah satu kali pembacaan diikuti
	 * penyimpanan, kolom {@code rolename} yang semula sengaja dikosongkan akan terisi
	 * pengenalnya. Itu tidak berbahaya secara langsung, namun perlu diketahui bahwa
	 * <b>penurunan hak berbasis nama ikut terpengaruh</b>: {@link #defaultAksesFeederSister()}
	 * membaca {@code getRoleName()}, jadi peran berpengenal persis {@code "admin"} yang
	 * semula tanpa nama akan &mdash; setelah tulis-balik ini &mdash; punya nama yang sama
	 * persis dengan {@code "admin"}, sehingga lolos pencocokan persis di sana. Perilaku
	 * bawaannya konsisten sebelum dan sesudah karena getter yang sama sudah dipakai, tetapi
	 * ini menunjukkan betapa berlapisnya ketergantungan antar-nilai di kelas ini.</p>
	 * <p>Bandingkan dengan {@link #toString()}, yang sengaja membaca field mentah justru
	 * untuk menghindari efek samping ini.</p>
	 *
	 * @return nama tampil peran, atau pengenalnya bila nama kosong
	 * @see #setRoleName(String)
	 * @see #toString()
	 * @see #getRoleId()
	 */
	@Column(name = "rolename", nullable = true, length = 255)
	public String getRoleName() {
		if (roleName == null || roleName.trim().isEmpty()) {
			roleName = getRoleId();
		}
		return this.roleName;
	}

	/**
	 * Menetapkan nama tampil peran.
	 *
	 * <p>Setter mentah tanpa penjagaan: menerima {@code null} maupun string kosong, dan tidak
	 * memangkas spasi. Nilai kosong akan langsung "diperbaiki" pada pembacaan berikutnya oleh
	 * {@link #getRoleName()}, yang menggantinya dengan pengenal peran dan menuliskannya
	 * kembali ke field.</p>
	 *
	 * <p><b>Ini bukan sekadar label.</b> Nilai yang disimpan di sini ikut menentukan hak
	 * bawaan pada dua tempat, keduanya lewat pencocokan persis tanpa peduli huruf
	 * besar-kecil (sejak perbaikan dok audit 2026-09-06; sebelumnya keduanya memakai
	 * pencocokan substring):</p>
	 * <ul>
	 *   <li>{@link #defaultAksesFeederSister()} &mdash; nama yang <b>sama persis</b> dengan
	 *   {@code "admin"} atau {@code "akademik"} mengaktifkan {@link #getBolehAksesFeeder()}
	 *   dan {@link #getBolehAksesSister()};</li>
	 *   <li>{@link #getAksesGerbangPesantren()} &mdash; nama yang <b>sama persis</b> dengan
	 *   {@code "satpam"}, {@code "keamanan pondok"}, atau {@code "keamanan pesantren"}
	 *   mengaktifkan hak memproses izin keluar/masuk di pos keamanan.</li>
	 * </ul>
	 * <p>Karena itu <b>mengganti nama sebuah peran dapat mengubah hak aksesnya</b> selama
	 * kolom-kolom itu masih {@code null}. Memberi nama persis "Admin" atau "Akademik" akan
	 * diam-diam membuka akses dasbor Feeder/SISTER; nama yang sekadar memuat kata itu, seperti
	 * "Administrasi Umum" atau "Wakil Kepala Akademik", tidak lagi cukup. Setelah mengganti
	 * nama, isilah flag terkait secara eksplisit bila hak itu tidak dikehendaki.</p>
	 *
	 * @param roleName nama tampil peran; {@code null} atau kosong diterima
	 * @see #getRoleName()
	 */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	/**
	 * Mengembalikan penanda apakah peran ini aktif &mdash; <b>berdefault menyala</b>.
	 *
	 * <p>Bila kolomnya masih {@code null}, method ini menganggapnya aktif. Pilihan
	 * <i>fail-open</i> ini disengaja untuk kompatibilitas mundur: seluruh baris peran yang
	 * sudah ada sebelum kolom {@code aktif} diperkenalkan tetap berfungsi tanpa perlu
	 * pembaruan data massal. Konsekuensinya, sebuah peran hanya menjadi tidak aktif bila
	 * seseorang <b>secara eksplisit</b> menyimpan {@link Boolean#FALSE}.</p>
	 *
	 * <h3>PERHATIAN: getter ini mengubah state</h3>
	 * <p>Nilai bawaan {@code true} <b>ditulis balik ke field</b>, bukan sekadar dikembalikan.
	 * Seperti pada {@link #getRoleName()}, ini menandai entity sebagai <i>dirty</i> hanya
	 * karena dibaca, dan dapat menerbitkan revisi audit Envers palsu. Pola aman yang setara
	 * adalah ternary murni ({@code return aktif == null ? Boolean.TRUE : aktif;}) &mdash;
	 * dipakai mayoritas getter flag lain di kelas ini, dan sebaiknya diikuti bila method ini
	 * suatu saat disunting.</p>
	 *
	 * <h3>Cakupan yang terbatas</h3>
	 * <p>Perlu ditegaskan: <b>menonaktifkan peran di sini tidak dengan sendirinya memblokir
	 * login</b> maupun mencabut kewenangan akun yang memakainya. Tidak ada pemeriksaan
	 * terhadap nilai ini di dalam {@link Tbmuser#hakAkses()} maupun pada getter flag mana pun
	 * di kelas ini &mdash; {@code hakAkses()} akan tetap mengembalikan peran yang sudah
	 * dinonaktifkan, lengkap dengan seluruh haknya. Nilai ini terutama dipakai untuk
	 * menyaring pilihan pada layar administrasi.</p>
	 * <p>Yang memperburuk keadaan: cache peran {@code Tbmuser.getUserRoleYgDipakai} tidak
	 * memiliki kedaluwarsa, sehingga bahkan bila suatu gerbang kelak memeriksa nilai ini,
	 * sesi yang sedang berjalan masih memegang salinan lama. Untuk benar-benar mencabut
	 * akses seseorang, ubah peran akunnya lewat jalur yang memperbarui cache tersebut
	 * &mdash; jangan mengandalkan penonaktifan peran di sini.</p>
	 *
	 * @return {@code true} bila peran aktif atau kolomnya belum pernah diisi
	 * @see #setAktif(Boolean)
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menetapkan penanda aktif peran.
	 *
	 * <p>Setter mentah tanpa penjagaan. Menyimpan {@code null} akan membuat
	 * {@link #getAktif()} kembali menganggap peran ini aktif &mdash; jadi {@code null}
	 * bukanlah cara untuk menonaktifkan; untuk itu simpan {@link Boolean#FALSE} secara
	 * eksplisit.</p>
	 *
	 * <p>Ingat batas cakupannya seperti dijelaskan pada {@link #getAktif()}: penonaktifan di
	 * sini bersifat administratif, bukan gerbang keamanan.</p>
	 *
	 * @param aktif penanda aktif; {@code null} berarti kembali ke bawaan menyala
	 * @see #getAktif()
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>e-Learning</b>.
	 *
	 * <p>Getter flag paling rumit di kelas ini, dan contoh terbaik dari pola "kolom
	 * {@code null} berarti tebak" yang dijelaskan pada dokumentasi kelas. Ia mengevaluasi
	 * <b>empat blok berurutan</b>, dan blok yang belakangan dapat menimpa hasil blok
	 * sebelumnya:</p>
	 * <ol>
	 *   <li><b>Daftar-tolak {@link #KANTIN}.</b> Langsung mengembalikan {@code false} tanpa
	 *   menyentuh kolom. Kasir tidak pernah mendapat e-Learning, bahkan bila dicentang.</li>
	 *   <li><b>Penurunan bawaan bila kolom {@code null}.</b> Menyala untuk
	 *   {@code "am"}, {@code "mhs"}, {@code "dosen"}, dan {@code "Akademik"} &mdash;
	 *   dibandingkan dengan {@code equalsIgnoreCase}. Nilainya <b>ditulis balik ke
	 *   field</b>.</li>
	 *   <li><b>Penolakan {@link #PENDUDUK}.</b> Menulis {@code false} ke field, menimpa
	 *   apa pun dari langkah sebelumnya.</li>
	 *   <li><b>Pemaksaan {@code "mhs"} dan {@code "dosen"}.</b> Menulis {@code true} ke
	 *   field <b>tanpa memeriksa apakah kolomnya sudah diisi</b>.</li>
	 * </ol>
	 *
	 * <h3>Konsekuensi penting: langkah 4 mengabaikan pilihan administrator</h3>
	 * <p>Perhatikan bahwa langkah 4 berada <b>di luar</b> penjagaan
	 * {@code if (elearning == null)}. Artinya bagi peran {@link #MAHASISWA} dan
	 * {@link #DOSEN}, administrator yang secara sadar <b>mematikan</b> e-Learning di layar
	 * Grup Pengguna akan mendapati pilihannya <b>diabaikan diam-diam</b> &mdash; getter
	 * memaksa nilainya kembali {@code true} setiap kali dibaca, dan bahkan
	 * <b>menuliskannya kembali ke basis data</b> pada penyimpanan berikutnya. Praktisnya
	 * e-Learning tidak dapat dicabut dari mahasiswa dan dosen lewat antarmuka. Ini tampak
	 * disengaja (keduanya adalah pengguna inti modul), namun layar administrasi tidak
	 * memberi tanda apa pun bahwa centangnya tidak berpengaruh.</p>
	 *
	 * <h3>PERHATIAN: getter penulis-balik field</h3>
	 * <p>Ketiga langkah terakhir menugaskan nilai ke field {@code elearning}, bukan sekadar
	 * mengembalikannya. Sekadar <b>membaca</b> hak e-Learning karena itu menandai entity
	 * sebagai <i>dirty</i>, memicu {@code UPDATE} beserta {@link #onUpdate()}, dan
	 * menerbitkan <b>revisi audit Envers palsu</b>. Untuk entity otorisasi, ini berarti
	 * riwayat perubahan hak akses memuat catatan yang tidak pernah dilakukan manusia.</p>
	 *
	 * <h3>Cakupan penegakan</h3>
	 * <p>Sebagian besar pemanggil memakai nilai ini untuk mengatur <b>visibilitas</b>
	 * pintasan dan menu ({@code MainAction}, {@code MainAction2}, JSP navigasi). Penegakan
	 * yang lebih dari sekadar tampilan ada di {@code MenuInitializer} dan
	 * {@code ProfileAdminSekolah}. Seperti flag modul lain di kelas ini, ia <b>tidak</b>
	 * melindungi endpoint {@code _service_*.jsp} di belakang layar &mdash; untuk itu andalkan
	 * {@code RolePrivilage} atau {@link #getEbisnisMenu()}.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses modul e-Learning; tidak pernah
	 *         {@code null}
	 * @see #setElearning(Boolean)
	 * @see #getKegiatanDanPrestasi()
	 */
	public Boolean getElearning() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}

		if (elearning == null) {
			if (getRoleId() != null && getRoleId().equalsIgnoreCase("am")) {
				elearning = true;
			} else if (getRoleId() != null && getRoleId().equalsIgnoreCase("mhs")) {
				elearning = true;
			} else if (getRoleId() != null && getRoleId().equalsIgnoreCase("dosen")) {
				elearning = true;
			} else if (getRoleId() != null && getRoleId().equalsIgnoreCase("Akademik")) {
				elearning = true;
			}
		}

		if (getRoleId() != null && getRoleId().equalsIgnoreCase(PENDUDUK)) {
			elearning = false;
		}

		if (getRoleId() != null && getRoleId().equalsIgnoreCase("mhs")) {
			elearning = true;
		} else if (getRoleId() != null && getRoleId().equalsIgnoreCase("dosen")) {
			elearning = true;
		}
		return elearning == null ? false : elearning;
	}

	/**
	 * Menetapkan hak akses modul e-Learning.
	 *
	 * <p>Setter mentah tanpa penjagaan. <b>Nilai yang disimpan di sini belum tentu
	 * berlaku</b>: seperti dijelaskan pada {@link #getElearning()}, peran
	 * {@link #MAHASISWA} dan {@link #DOSEN} akan memaksa nilainya kembali {@code true}, dan
	 * peran {@link #KANTIN} serta {@link #PENDUDUK} akan memaksanya {@code false},
	 * mengabaikan apa pun yang ditetapkan di sini.</p>
	 *
	 * <p>Menyimpan {@code null} berarti mengembalikan flag ke penurunan bawaan berbasis
	 * {@code roleId}, bukan berarti "tidak punya hak".</p>
	 *
	 * @param elearning hak modul e-Learning; {@code null} berarti kembali ke bawaan
	 * @see #getElearning()
	 */
	public void setElearning(Boolean elearning) {
		this.elearning = elearning;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>Kegiatan &amp; Prestasi</b>
	 * (kegiatan kemahasiswaan, lomba, penghargaan).
	 *
	 * <p>Strukturnya sengaja dibuat sejajar dengan {@link #getElearning()}, dengan
	 * <b>satu perbedaan penting</b>: blok pemaksaan terakhir untuk {@code "mhs"} dan
	 * {@code "dosen"} <b>tidak ada</b> di sini. Urutannya:</p>
	 * <ol>
	 *   <li>daftar-tolak {@link #KANTIN} &mdash; kembalikan {@code false};</li>
	 *   <li>bila kolom {@code null}, menyala untuk {@code "am"}, {@code "mhs"},
	 *   {@code "dosen"}, {@code "Akademik"} ({@code equalsIgnoreCase}), ditulis balik ke
	 *   field;</li>
	 *   <li>{@link #PENDUDUK} dipaksa {@code false}, ditulis balik ke field;</li>
	 *   <li>kembalikan kolomnya, dengan {@code null} diperlakukan sebagai {@code false}.</li>
	 * </ol>
	 *
	 * <p>Karena tidak ada blok pemaksaan, administrator <b>dapat</b> mencabut hak ini dari
	 * mahasiswa dan dosen &mdash; berbeda dari {@link #getElearning()}. Ketidaksamaan antara
	 * dua getter yang tampak kembar ini mudah luput saat menyunting salah satunya; bila
	 * mengubah salah satu, periksa apakah yang lain perlu ikut berubah.</p>
	 *
	 * <p>Berbeda pula dari sebagian besar flag "bawaan menyala" di kelas ini, nilai akhirnya
	 * jatuh ke <b>{@code false}</b> bila kolom masih {@code null} dan peran tidak termasuk
	 * keempat pengenal di langkah 2. Jadi peran baru yang bernama bebas <b>tidak</b>
	 * memperoleh hak ini secara bawaan &mdash; perilaku <i>fail-closed</i> yang lebih
	 * disukai.</p>
	 *
	 * <p><b>Getter penulis-balik field</b>: berlaku seluruh peringatan revisi audit palsu
	 * seperti pada {@link #getElearning()}.</p>
	 *
	 * <p>Pemanggilnya seluruhnya berada di lapisan tampilan ({@code MainAction2}, JSP menu
	 * dan akses cepat) &mdash; tidak ada gerbang server-side yang membaca flag ini, sehingga
	 * mematikannya hanya menyembunyikan pintasan.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses modul Kegiatan &amp; Prestasi;
	 *         tidak pernah {@code null}
	 * @see #setKegiatanDanPrestasi(Boolean)
	 * @see #getElearning()
	 */
	public Boolean getKegiatanDanPrestasi() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		if (kegiatanDanPrestasi == null) {
			if (getRoleId() != null && getRoleId().equalsIgnoreCase("am")) {
				kegiatanDanPrestasi = true;
			} else if (getRoleId() != null && getRoleId().equalsIgnoreCase("mhs")) {
				kegiatanDanPrestasi = true;
			} else if (getRoleId() != null && getRoleId().equalsIgnoreCase("dosen")) {
				kegiatanDanPrestasi = true;
			} else if (getRoleId() != null && getRoleId().equalsIgnoreCase("Akademik")) {
				kegiatanDanPrestasi = true;
			}
		}

		if (getRoleId() != null && getRoleId().equalsIgnoreCase(PENDUDUK)) {
			kegiatanDanPrestasi = false;
		}

		return kegiatanDanPrestasi == null ? false : kegiatanDanPrestasi;
	}

	/**
	 * Menetapkan hak akses modul Kegiatan &amp; Prestasi.
	 *
	 * <p>Setter mentah tanpa penjagaan. Berbeda dari {@link #setElearning(Boolean)}, nilai
	 * yang disimpan di sini <b>dihormati</b> untuk mahasiswa dan dosen karena
	 * {@link #getKegiatanDanPrestasi()} tidak memiliki blok pemaksaan. Yang tetap
	 * mengabaikannya hanyalah peran {@link #KANTIN} dan {@link #PENDUDUK}.</p>
	 *
	 * @param kegiatanDanPrestasi hak modul; {@code null} berarti kembali ke bawaan
	 * @see #getKegiatanDanPrestasi()
	 */
	public void setKegiatanDanPrestasi(Boolean kegiatanDanPrestasi) {
		this.kegiatanDanPrestasi = kegiatanDanPrestasi;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>Perpustakaan</b>.
	 *
	 * <p>Salah satu flag paling permisif di kelas ini: bila kolomnya masih {@code null}
	 * &mdash; keadaan setiap Grup Pengguna yang baru dibuat &mdash; hasilnya
	 * <b>{@code true}</b>. Satu-satunya pengecualian adalah daftar-tolak {@link #KANTIN}.
	 * Perpustakaan dengan demikian terbuka secara bawaan bagi seluruh peran, dan baru
	 * tertutup bila administrator secara eksplisit menyimpan {@link Boolean#FALSE}.</p>
	 *
	 * <p>Pilihan <i>fail-open</i> ini masuk akal untuk layanan yang memang ditujukan bagi
	 * seluruh sivitas, namun perlu diingat saat merancang peran berhak-minimum: peran baru
	 * <b>tidak</b> dimulai dari nol hak.</p>
	 *
	 * <p><b>Getter murni</b> &mdash; memakai ternary tanpa menulis balik ke field, sehingga
	 * bebas dari masalah revisi audit palsu. Ini pola yang sebaiknya diikuti getter lain di
	 * kelas ini.</p>
	 *
	 * <p>Penegakannya nyata di sisi server pada {@code PustakaApi}, selain dipakai luas untuk
	 * visibilitas menu.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses perpustakaan; tidak pernah
	 *         {@code null}
	 * @see #setPustaka(Boolean)
	 */
	public Boolean getPustaka() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		return pustaka == null ? true : pustaka;
	}

	/**
	 * Menetapkan hak akses modul Perpustakaan.
	 *
	 * <p>Setter mentah tanpa penjagaan. Menyimpan {@code null} <b>tidak</b> mencabut hak
	 * &mdash; ia mengembalikan flag ke bawaan yang bernilai {@code true}. Untuk benar-benar
	 * menutup akses, simpan {@link Boolean#FALSE} secara eksplisit.</p>
	 *
	 * @param pustaka hak modul; {@code null} berarti kembali ke bawaan menyala
	 * @see #getPustaka()
	 */
	public void setPustaka(Boolean pustaka) {
		this.pustaka = pustaka;
	}

	/**
	 * Menentukan apakah peran ini boleh melihat <b>dasbor umum</b> &mdash; kolom
	 * {@code dashboard_boleh}.
	 *
	 * <p>Nama kolomnya sengaja diberi akhiran {@code _boleh} karena {@code dashboard} adalah
	 * kata kunci yang sudah dipakai di tempat lain pada skema. Perhatikan pula bahwa
	 * {@code length = 255} pada anotasi kolom tidak berpengaruh untuk tipe
	 * {@link Boolean}; ia sisa penyalinan dari kolom teks.</p>
	 *
	 * <p>Sama seperti {@link #getPustaka()}: <b>berdefault menyala</b> bagi seluruh peran
	 * kecuali {@link #KANTIN}, dan merupakan <b>getter murni</b> tanpa tulis-balik.</p>
	 *
	 * <p>Perlu dibedakan dari {@link #getDashboardDefaultMain()}, yang memilih dasbor
	 * <i>mana</i> yang tampil, sedangkan flag ini menentukan apakah dasbor tampil sama
	 * sekali. Perlu dibedakan pula dari flag dasbor per-modul seperti
	 * {@link #getDashboardKoperasi()} dan {@link #getDasborRepository()}.</p>
	 *
	 * <p>Seluruh pemanggilnya berada di lapisan tampilan; tidak ada gerbang server-side yang
	 * membacanya.</p>
	 *
	 * @return {@code true} bila dasbor umum boleh ditampilkan; tidak pernah {@code null}
	 * @see #setDashboard(Boolean)
	 */
	@Column(name = "dashboard_boleh", length = 255)
	public Boolean getDashboard() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		return dashboard == null ? true : dashboard;
	}

	/**
	 * Menetapkan hak melihat dasbor umum.
	 *
	 * <p>Setter mentah tanpa penjagaan; {@code null} mengembalikan flag ke bawaan menyala,
	 * bukan mencabutnya.</p>
	 *
	 * @param dashboard hak melihat dasbor; {@code null} berarti kembali ke bawaan menyala
	 * @see #getDashboard()
	 */
	public void setDashboard(Boolean dashboard) {
		this.dashboard = dashboard;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>Workflow</b> (alur
	 * persetujuan/disposisi).
	 *
	 * <p>Berpola sama dengan {@link #getPustaka()} dan {@link #getDashboard()}:
	 * <b>berdefault menyala</b> untuk seluruh peran kecuali {@link #KANTIN}, dan merupakan
	 * <b>getter murni</b> tanpa tulis-balik.</p>
	 *
	 * <p>Berbeda dari kedua flag itu, yang satu ini benar-benar dipakai sebagai <b>gerbang
	 * persetujuan di sisi server</b> pada {@code PosApi} &mdash; selain pemakaian
	 * visibilitas menu yang biasa. Karena bawaannya menyala, sebuah Grup Pengguna baru
	 * langsung memperoleh kewenangan alur persetujuan di jalur tersebut tanpa pernah
	 * dicentang. Bila merancang peran berhak-minimum, matikan flag ini secara eksplisit.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses workflow; tidak pernah {@code null}
	 * @see #setWorkflow(Boolean)
	 */
	public Boolean getWorkflow() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		return workflow == null ? true : workflow;
	}

	/**
	 * Menetapkan hak akses modul Workflow.
	 *
	 * <p>Setter mentah tanpa penjagaan. Ingat bahwa {@code null} berarti <b>menyala</b>
	 * (lihat {@link #getWorkflow()}), dan bahwa flag ini menjadi gerbang persetujuan nyata di
	 * {@code PosApi} &mdash; jadi menyimpan {@code null} di sini bukan tindakan netral.</p>
	 *
	 * @param workflow hak modul; {@code null} berarti kembali ke bawaan menyala
	 * @see #getWorkflow()
	 */
	public void setWorkflow(Boolean workflow) {
		this.workflow = workflow;
	}

	/**
	 * Menentukan apakah pintasan &amp; dasbor utama <b>eMedic</b> (Rumah Sakit / Klinik) muncul untuk
	 * role ini. Default <b>NONAKTIF untuk SEMUA role — termasuk Administrator</b> bila nilai belum
	 * pernah diset (null); tombol &amp; dasbor baru muncul setelah dicentang di master Hak Akses
	 * (TbmroleAction). Peran KANTIN dikecualikan (selalu nonaktif) mengikuti pola flag modul lain.
	 * Kolom ini <b>DIAUDIT</b> (Envers) sama seperti kolom {@link Tbmrole} lainnya — tidak memakai
	 * {@code @NotAudited}; kolomnya ditambahkan otomatis ke tabel utama maupun tabel audit oleh
	 * {@code hbm2ddl=update} saat startup.
	 *
	 * <h3>PENTING: flag ini hanya mengatur ikon, bukan akses</h3>
	 * <p>Perlu ditegaskan agar tidak menyesatkan administrator maupun penyunting kode
	 * berikutnya: <b>mematikan flag ini tidak mencabut akses ke modul eMedic</b>. Seluruh
	 * pemanggilnya berada di lapisan tampilan ({@code MainAction},
	 * {@code NewUiModuleShortcutService}, layar Grup Pengguna, dan proyeksi
	 * {@code HakAksesApi}). Halaman {@code modul/emedik/index.jsp} sendiri menegakkan izinnya
	 * lewat {@link #getEbisnisMenu()}, <b>bukan</b> lewat flag ini, dan begitu pula
	 * {@code EmedikApi} di sisi server.</p>
	 * <p>Akibatnya flag ini adalah <b>centang yang menyesatkan</b>: mematikannya hanya
	 * menghilangkan pintasan dari halaman utama, sementara pengguna yang mengetahui URL-nya
	 * tetap dapat masuk. Karena modul eMedic menyimpan rekam medis, selisih antara "ikon
	 * hilang" dan "akses tercabut" di sini berkonsekuensi nyata. Untuk benar-benar mencabut
	 * akses, sunting katalog {@link #getEbisnisMenu()}. Hal yang sama berlaku pada
	 * {@link #getTampilPos()}.</p>
	 *
	 * @return {@code true} bila pintasan eMedic ditampilkan; tidak pernah {@code null}
	 * @see #setEmedic(Boolean)
	 * @see #getEbisnisMenu()
	 * @see #getTampilPos()
	 */
	public Boolean getEmedic() {
		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		return emedic == null ? Boolean.FALSE : emedic;
	}

	/**
	 * Menetapkan penanda tampil pintasan &amp; dasbor eMedic.
	 *
	 * <p>Setter mentah tanpa penjagaan. Ingat bahwa &mdash; seperti dijelaskan panjang pada
	 * {@link #getEmedic()} &mdash; nilai ini hanya mengatur <b>visibilitas ikon</b>, sehingga
	 * menyimpan {@link Boolean#FALSE} di sini <b>bukan</b> tindakan pencabutan akses.</p>
	 *
	 * @param emedic penanda tampil; {@code null} berarti kembali ke bawaan mati
	 * @see #getEmedic()
	 */
	public void setEmedic(Boolean emedic) {
		this.emedic = emedic;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>Administrasi</b> (persuratan,
	 * kearsipan, dan tata usaha umum).
	 *
	 * <p>Berpola sama dengan {@link #getPustaka()}, {@link #getDashboard()}, dan
	 * {@link #getWorkflow()}: <b>berdefault menyala</b> bagi seluruh peran kecuali
	 * {@link #KANTIN}, dan merupakan <b>getter murni</b> tanpa tulis-balik ke field.</p>
	 *
	 * <p>Perhatikan bahwa penjagaannya jauh lebih longgar daripada flag administrasi lain di
	 * kelas ini: tidak ada penolakan bagi kelompok pengguna akhir ({@link #roleEndUser()}),
	 * sehingga peran {@link #MAHASISWA}, {@link #SISWA}, {@link #DOSEN}, {@link #GURU}, dan
	 * {@link #PEGAWAI} <b>semuanya memperoleh flag ini secara bawaan</b>. Bandingkan dengan
	 * {@link #getMelihatSemuaSurat()} yang menolak kelompok itu secara tegas &mdash; di
	 * sanalah pembatasan data persuratan yang sesungguhnya berada.</p>
	 *
	 * <p>Seluruh pemanggilnya berada di lapisan tampilan ({@code MainAction2}, JSP navigasi
	 * dan menu seluler); tidak ada gerbang server-side yang membacanya.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses modul Administrasi; tidak pernah
	 *         {@code null}
	 * @see #setAdministrasi(Boolean)
	 * @see #getMelihatSemuaSurat()
	 */
	public Boolean getAdministrasi() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		return administrasi == null ? true : administrasi;
	}

	/**
	 * Menetapkan hak akses modul Administrasi.
	 *
	 * <p>Setter mentah tanpa penjagaan; {@code null} mengembalikan flag ke bawaan menyala,
	 * bukan mencabutnya.</p>
	 *
	 * @param administrasi hak modul; {@code null} berarti kembali ke bawaan menyala
	 * @see #getAdministrasi()
	 */
	public void setAdministrasi(Boolean administrasi) {
		this.administrasi = administrasi;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>Pengadaan</b> (barang dan jasa).
	 *
	 * <p>Menggabungkan dua mekanisme yang berbeda:</p>
	 * <ol>
	 *   <li><b>Daftar-tolak {@link #KANTIN}</b> &mdash; kembalikan {@code false} langsung.</li>
	 *   <li><b>Penolakan kelompok akademik</b> &mdash; {@link #MAHASISWA}, {@link #SISWA},
	 *   {@link #DOSEN}, dan {@link #GURU} <b>ditulisi {@code false}</b> ke field. Perhatikan
	 *   bahwa ini terjadi <b>di luar</b> penjagaan {@code null}, sehingga mencentang
	 *   pengadaan untuk keempat peran itu di layar Grup Pengguna akan diabaikan &mdash; dan
	 *   lebih jauh lagi, nilai {@code false} itu akan <b>tertulis kembali ke basis data</b>
	 *   pada penyimpanan berikutnya, menghapus pilihan administrator secara permanen.</li>
	 *   <li>Selain itu, kolomnya dikembalikan dengan bawaan <b>{@code true}</b>.</li>
	 * </ol>
	 *
	 * <p><b>Perhatikan asimetri daftar penolakannya.</b> Empat peran ditolak, tetapi
	 * {@link #PEGAWAI} <b>tidak</b> &mdash; padahal ia termasuk {@link #roleEndUser()} yang
	 * ditolak pada getter berlingkup luas lainnya. Jadi peran Pegawai memperoleh akses
	 * Pengadaan secara bawaan. Ini tampak disengaja (staf pengadaan umumnya berperan
	 * Pegawai), namun berbeda dari pola {@code roleEndUser()} yang dipakai di tempat lain,
	 * sehingga mudah dikira kelalaian.</p>
	 *
	 * <p>Karena bawaannya {@code true}, setiap Grup Pengguna baru yang bernama bebas langsung
	 * memperoleh akses Pengadaan. Matikan secara eksplisit bila tidak dikehendaki.</p>
	 *
	 * <p><b>Getter penulis-balik field</b> pada langkah 2 &mdash; berlaku seluruh peringatan
	 * revisi audit palsu seperti pada {@link #getElearning()}.</p>
	 *
	 * <p>Seluruh pemanggilnya berada di lapisan tampilan; tidak ada gerbang server-side yang
	 * membacanya.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses modul Pengadaan; tidak pernah
	 *         {@code null}
	 * @see #setPengadaan(Boolean)
	 */
	public Boolean getPengadaan() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		if (getRoleId() != null && getRoleId().equals(Tbmrole.MAHASISWA)) {
			pengadaan = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.SISWA)) {
			pengadaan = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.DOSEN)) {
			pengadaan = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.GURU)) {
			pengadaan = false;
		}
		return pengadaan == null ? true : pengadaan;
	}

	/**
	 * Menetapkan hak akses modul Pengadaan.
	 *
	 * <p>Setter mentah tanpa penjagaan. <b>Nilai yang disimpan tidak berlaku</b> bagi peran
	 * {@link #MAHASISWA}, {@link #SISWA}, {@link #DOSEN}, {@link #GURU}, dan {@link #KANTIN}
	 * &mdash; lihat {@link #getPengadaan()}. Untuk peran lain, {@code null} berarti kembali
	 * ke bawaan menyala.</p>
	 *
	 * @param pengadaan hak modul; {@code null} berarti kembali ke bawaan menyala
	 * @see #getPengadaan()
	 */
	public void setPengadaan(Boolean pengadaan) {
		this.pengadaan = pengadaan;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>Keuangan</b>.
	 *
	 * <p>Getter pertama dari <b>empat bersaudara</b> yang badannya identik baris demi baris
	 * &mdash; bersama {@link #getPembayaran()}, {@link #getAkunting()}, dan
	 * {@link #getKepegawaian()} (yang hanya berbeda pada kata kunci pembandingnya). Struktur
	 * ini juga dipakai {@link #getKantin()} dalam bentuk yang lebih ringkas. Bila menyunting
	 * salah satunya, periksa apakah saudaranya perlu ikut berubah.</p>
	 *
	 * <p>Urutannya:</p>
	 * <ol>
	 *   <li>daftar-tolak {@link #KANTIN} &mdash; kembalikan {@code false};</li>
	 *   <li>{@link #MAHASISWA}, {@link #SISWA}, {@link #DOSEN}, {@link #GURU} <b>ditulisi
	 *   {@code false}</b> ke field, di luar penjagaan {@code null} &mdash; centang
	 *   administrator diabaikan dan terhapus permanen pada penyimpanan berikutnya;</li>
	 *   <li>bila kolom masih {@code null}, nilainya diturunkan dari <b>perbandingan
	 *   persis</b>.</li>
	 * </ol>
	 *
	 * <h3>RIWAYAT: hak yang pernah menyala karena kebetulan penamaan (FIXED)</h3>
	 * <p>Sampai perbaikan dok audit 2026-09-06, nilai bawaan pada langkah 3 dihitung sebagai
	 * {@code getRoleId().toLowerCase().contains("keu") || getRoleId().equals(ADMINISTRATOR)}
	 * &mdash; <b>pencocokan substring, bukan perbandingan persis</b> dengan konstanta
	 * {@link #KEUANGAN}. Akibatnya setiap Grup Pengguna yang pengenalnya sekadar
	 * <i>mengandung</i> potongan huruf {@code "keu"} memperoleh hak ini secara bawaan
	 * &mdash; termasuk pengenal yang maksudnya justru membatasi, seperti
	 * {@code "keu_lihat_saja"} atau {@code "keu_readonly"}, dan bahkan kebetulan murni seperti
	 * {@code "bekuan"} atau {@code "penyekuan"}. Yang membuat pola ini serius bukanlah flag ini
	 * sendiri, melainkan bahwa <b>lima flag memakai kata kunci yang sama</b>, dan salah
	 * satunya &mdash; {@link #getBolehEntryTopup()} &mdash; adalah <b>gerbang transaksional
	 * sungguhan</b> di {@code KantinHelper} dan {@code PosApi}, bukan sekadar penentu
	 * visibilitas.</p>
	 * <p><b>Sekarang:</b> langkah 3 memakai
	 * {@code getRoleId().equalsIgnoreCase(KEUANGAN) || getRoleId().equals(ADMINISTRATOR)}
	 * &mdash; perbandingan persis, tidak peka huruf besar-kecil &mdash; dan
	 * {@link #getBolehEntryTopup()} sudah <i>fail-closed</i> tanpa syarat sehingga tidak lagi
	 * bergantung pada {@code roleId} sama sekali. <b>Tetap isi flag keuangan secara
	 * eksplisit</b> alih-alih membiarkannya {@code null}.</p>
	 *
	 * <p><b>Getter penulis-balik field</b> pada langkah 2 &mdash; berlaku peringatan revisi
	 * audit palsu seperti pada {@link #getElearning()}.</p>
	 *
	 * <p>Penegakannya nyata di sisi server pada dasbor dan laporan Reimbursement
	 * ({@code ReimbursementLaporanAction}, {@code ReimbursementDashboardAction}), selain
	 * pemakaian visibilitas menu yang biasa.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses modul Keuangan; tidak pernah
	 *         {@code null}
	 * @see #setKeuangan(Boolean)
	 * @see #KEUANGAN
	 * @see #getPembayaran()
	 * @see #getAkunting()
	 * @see #getBolehEntryTopup()
	 */
	public Boolean getKeuangan() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		if (getRoleId() != null && getRoleId().equals(Tbmrole.MAHASISWA)) {
			keuangan = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.SISWA)) {
			keuangan = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.DOSEN)) {
			keuangan = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.GURU)) {
			keuangan = false;
		}
		return keuangan == null
				? (getRoleId() != null
						&& (getRoleId().equalsIgnoreCase(Tbmrole.KEUANGAN) || getRoleId().equals(Tbmrole.ADMINISTRATOR)))
				: keuangan;
	}

	/**
	 * Menetapkan hak akses modul Keuangan.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan tidak berlaku bagi
	 * {@link #KANTIN} maupun keempat peran akademik yang ditolak paksa &mdash; lihat
	 * {@link #getKeuangan()}.</p>
	 *
	 * <p><b>Menyimpan {@code null} bukan tindakan netral.</b> Ia mengembalikan flag ke
	 * penurunan berbasis perbandingan persis dengan {@link #KEUANGAN}, yang untuk peran
	 * bernama tepat justru berarti <b>menyala</b>. Untuk menutup akses, simpan
	 * {@link Boolean#FALSE} secara eksplisit.</p>
	 *
	 * @param keuangan hak modul; {@code null} berarti kembali ke bawaan berbasis nama
	 * @see #getKeuangan()
	 */
	public void setKeuangan(Boolean keuangan) {
		this.keuangan = keuangan;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>Pembayaran</b>.
	 *
	 * <p>Badannya <b>identik baris demi baris</b> dengan {@link #getKeuangan()}: daftar-tolak
	 * {@link #KANTIN}, penulisan paksa {@code false} bagi {@link #MAHASISWA},
	 * {@link #SISWA}, {@link #DOSEN}, dan {@link #GURU}, lalu bawaan berbasis perbandingan
	 * persis dengan {@link #KEUANGAN} atau dengan {@link #ADMINISTRATOR}.</p>
	 *
	 * <p>Seluruh catatan pada {@link #getKeuangan()} berlaku sama di sini &mdash; termasuk
	 * riwayat hak yang pernah menyala semata-mata karena pengenal peran <i>mengandung</i>
	 * potongan huruf {@code "keu"} (sudah diperbaiki), dan bahwa getter ini menulis balik ke
	 * field sehingga dapat menerbitkan revisi audit palsu.</p>
	 *
	 * <p>Perlu dibedakan dari {@link #getKeuangan()} (modul keuangan secara umum) dan
	 * {@link #getAkunting()} (pembukuan/jurnal): flag ini menyangkut penerimaan dan
	 * pembayaran tagihan. Karena ketiganya berbagi kata kunci bawaan yang sama, dalam
	 * praktiknya ketiganya nyaris selalu menyala atau mati bersamaan selama kolomnya belum
	 * pernah diisi &mdash; pemisahan ketiga flag ini baru bermakna setelah administrator
	 * mengaturnya secara eksplisit.</p>
	 *
	 * <p>Seluruh pemanggilnya berada di lapisan tampilan ({@code MainAction2},
	 * {@code MobileAction}, JSP navigasi); tidak ada gerbang server-side yang membacanya.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses modul Pembayaran; tidak pernah
	 *         {@code null}
	 * @see #setPembayaran(Boolean)
	 * @see #getKeuangan()
	 */
	public Boolean getPembayaran() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		if (getRoleId() != null && getRoleId().equals(Tbmrole.MAHASISWA)) {
			pembayaran = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.SISWA)) {
			pembayaran = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.DOSEN)) {
			pembayaran = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.GURU)) {
			pembayaran = false;
		}
		return pembayaran == null
				? (getRoleId() != null
						&& (getRoleId().equalsIgnoreCase(Tbmrole.KEUANGAN) || getRoleId().equals(Tbmrole.ADMINISTRATOR)))
				: pembayaran;
	}

	/**
	 * Menetapkan hak akses modul Pembayaran.
	 *
	 * <p>Setter mentah tanpa penjagaan; berperilaku persis seperti
	 * {@link #setKeuangan(Boolean)}, termasuk bahwa {@code null} berarti kembali ke bawaan
	 * berbasis perbandingan persis dengan {@link #KEUANGAN} dan karena itu bukan tindakan
	 * netral.</p>
	 *
	 * @param pembayaran hak modul; {@code null} berarti kembali ke bawaan berbasis nama
	 * @see #getPembayaran()
	 */
	public void setPembayaran(Boolean pembayaran) {
		this.pembayaran = pembayaran;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>Akuntansi</b> (pembukuan, jurnal,
	 * buku besar).
	 *
	 * <p>Getter ketiga dari empat bersaudara: badannya <b>identik baris demi baris</b> dengan
	 * {@link #getKeuangan()} dan {@link #getPembayaran()} &mdash; daftar-tolak
	 * {@link #KANTIN}, penulisan paksa {@code false} bagi keempat peran akademik, lalu bawaan
	 * berbasis perbandingan persis dengan {@link #KEUANGAN} atau {@link #ADMINISTRATOR}.</p>
	 *
	 * <p>Seluruh catatan pada {@link #getKeuangan()} berlaku sama di sini, termasuk sifat
	 * penulis-balik field dan riwayat hak yang pernah menyala karena penamaan (sudah
	 * diperbaiki).</p>
	 *
	 * <p>Perhatikan bahwa modul akuntansi termasuk yang paling sensitif di AIS &mdash; ia
	 * menyentuh jurnal dan buku besar. Meski demikian, pemanggil flag ini seluruhnya berada
	 * di lapisan tampilan ({@code MainAction2}, JSP menu), sehingga <b>mematikannya hanya
	 * menyembunyikan menu</b>. Otorisasi akuntansi yang sesungguhnya ditegakkan lewat
	 * {@code RolePrivilage} dan &mdash; untuk jalur eBisnis &mdash;
	 * {@link #getEbisnisMenu()} melalui {@code EbisnisMenuKatalog.bolehAksiAkuntansi(...)}.
	 * Jangan mengandalkan flag ini sebagai kontrol akses.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses modul Akuntansi; tidak pernah
	 *         {@code null}
	 * @see #setAkunting(Boolean)
	 * @see #getKeuangan()
	 * @see #getEbisnisMenu()
	 */
	public Boolean getAkunting() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		if (getRoleId() != null && getRoleId().equals(Tbmrole.MAHASISWA)) {
			akunting = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.SISWA)) {
			akunting = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.DOSEN)) {
			akunting = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.GURU)) {
			akunting = false;
		}
		return akunting == null
				? (getRoleId() != null
						&& (getRoleId().equalsIgnoreCase(Tbmrole.KEUANGAN) || getRoleId().equals(Tbmrole.ADMINISTRATOR)))
				: akunting;
	}

	/**
	 * Menetapkan hak akses modul Akuntansi.
	 *
	 * <p>Setter mentah tanpa penjagaan; berperilaku persis seperti
	 * {@link #setKeuangan(Boolean)}. Ingat bahwa nilai ini hanya mengatur visibilitas menu
	 * &mdash; menyimpan {@link Boolean#FALSE} <b>tidak</b> mencabut kewenangan akuntansi yang
	 * ditegakkan {@code RolePrivilage} dan {@link #getEbisnisMenu()}.</p>
	 *
	 * @param akunting hak modul; {@code null} berarti kembali ke bawaan berbasis nama
	 * @see #getAkunting()
	 */
	public void setAkunting(Boolean akunting) {
		this.akunting = akunting;
	}

	/**
	 * Menentukan apakah peran ini boleh melihat <b>Kalender Akademik</b>.
	 *
	 * <p>Memiliki nilai bawaan yang <b>terbalik dari kebiasaan</b> di kelas ini: bila kolom
	 * masih {@code null}, hasilnya {@code true} untuk seluruh peran <b>kecuali</b>
	 * {@link #ADMINISTRATOR}, yang justru memperoleh {@code false}. Bandingkan dengan
	 * belasan flag lain yang justru memberi hak bawaan hanya kepada Administrator.</p>
	 *
	 * <p>Ini bukan kekeliruan melainkan pilihan yang masuk akal: kalender akademik adalah
	 * informasi bagi sivitas yang menjalani perkuliahan, sedangkan Administrator adalah
	 * pengelola sistem yang halaman utamanya sengaja tidak dipenuhi pintasan yang tidak
	 * relevan baginya. Pola bawaan "menyala untuk semua kecuali Administrator" yang sama juga
	 * dipakai {@link #getInfoKegiatan()}.</p>
	 *
	 * <p>Perhatikan bahwa nilai bawaan {@code false} bagi Administrator itu <b>hanya
	 * bawaan</b> &mdash; administrator dapat menyalakannya secara eksplisit, dan nilai
	 * tersimpan akan dihormati. Yang mutlak hanyalah daftar-tolak {@link #KANTIN}.</p>
	 *
	 * <p><b>Getter murni</b> &mdash; memakai ternary bersarang tanpa menulis balik ke field,
	 * sehingga bebas dari masalah revisi audit palsu.</p>
	 *
	 * <p>Seluruh pemanggilnya berada di lapisan tampilan; tidak ada gerbang server-side yang
	 * membacanya.</p>
	 *
	 * @return {@code true} bila kalender akademik boleh ditampilkan; tidak pernah
	 *         {@code null}
	 * @see #setKalenderAkademik(Boolean)
	 * @see #getInfoKegiatan()
	 */
	public Boolean getKalenderAkademik() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		return kalenderAkademik == null ? (getRoleId() != null && getRoleId().equals(ADMINISTRATOR) ? false : true)
				: kalenderAkademik;
	}

	/**
	 * Menetapkan hak melihat Kalender Akademik.
	 *
	 * <p>Setter mentah tanpa penjagaan. Menyimpan {@code null} mengembalikan flag ke bawaan
	 * yang bergantung peran: {@code false} untuk {@link #ADMINISTRATOR}, {@code true} untuk
	 * peran lainnya.</p>
	 *
	 * @param kalenderAkademik hak melihat kalender; {@code null} berarti kembali ke bawaan
	 * @see #getKalenderAkademik()
	 */
	public void setKalenderAkademik(Boolean kalenderAkademik) {
		this.kalenderAkademik = kalenderAkademik;
	}

	/**
	 * Apakah role ini boleh mengakses dasbor "Neo Feeder". Default (saat admin belum mengatur): AKTIF untuk
	 * ADMINISTRATOR, AKADEMIK, serta role yang NAMA-nya sama persis dengan "akademik"/"admin"; selain itu nonaktif.
	 * Menggantikan gerbang lama berbasis {@code Common.getKonfigurasi(...)}.
	 *
	 * <p>Neo Feeder adalah kanal pelaporan data pendidikan tinggi ke Kementerian, sehingga
	 * akses ke dasbornya berarti kemampuan melihat &mdash; dan menyinkronkan &mdash; data
	 * akademik institusi secara menyeluruh. Ini termasuk gerbang yang <b>benar-benar
	 * ditegakkan di sisi server</b>, pada {@code CommonCurrentSessionHelper}, bukan sekadar
	 * penentu visibilitas pintasan.</p>
	 *
	 * <p><b>Perhatikan sumber nilai bawaannya.</b> Karena
	 * {@link #defaultAksesFeederSister()} ikut mencocokkan <b>nama</b> peran secara persis,
	 * hak ini dapat menyala semata-mata karena penamaan &mdash; peran bernama tepat "Admin"
	 * atau "Akademik" akan memperolehnya tanpa pernah dicentang, meski nama yang sekadar
	 * memuat kata itu (mis. "Administrasi Umum") sejak perbaikan dok audit 2026-09-06 tidak
	 * lagi cukup. Lihat peringatan lengkapnya pada method tersebut.</p>
	 *
	 * <p><b>Getter murni</b> tanpa tulis-balik ke field.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses dasbor Neo Feeder; tidak pernah
	 *         {@code null}
	 * @see #setBolehAksesFeeder(Boolean)
	 * @see #getBolehAksesSister()
	 * @see #defaultAksesFeederSister()
	 */
	@Column(name = "boleh_akses_feeder")
	public Boolean getBolehAksesFeeder() {
		return bolehAksesFeeder == null ? defaultAksesFeederSister() : bolehAksesFeeder;
	}

	/**
	 * Menetapkan hak akses dasbor Neo Feeder.
	 *
	 * <p>Setter mentah tanpa penjagaan. <b>Menyimpan {@code null} bukan tindakan netral</b>:
	 * ia mengembalikan flag ke {@link #defaultAksesFeederSister()}, yang untuk peran bernama
	 * persis "admin" atau "akademik" justru berarti <b>menyala</b>. Untuk menutup akses,
	 * simpan {@link Boolean#FALSE} secara eksplisit.</p>
	 *
	 * @param bolehAksesFeeder hak akses Feeder; {@code null} berarti kembali ke bawaan
	 *                         berbasis nama
	 * @see #getBolehAksesFeeder()
	 */
	public void setBolehAksesFeeder(Boolean bolehAksesFeeder) {
		this.bolehAksesFeeder = bolehAksesFeeder;
	}

	/**
	 * Apakah role ini boleh mengakses dasbor "SISTER". Default sama dengan {@link #getBolehAksesFeeder()}.
	 *
	 * <p>SISTER adalah kanal pelaporan data sumber daya (dosen dan tenaga kependidikan) ke
	 * Kementerian &mdash; padanan Neo Feeder di sisi kepegawaian. Seperti
	 * {@link #getBolehAksesFeeder()}, flag ini adalah gerbang yang <b>benar-benar
	 * ditegakkan di sisi server</b> pada {@code CommonCurrentSessionHelper}.</p>
	 *
	 * <p>Kedua flag berbagi satu method nilai bawaan, sehingga selama kolomnya belum pernah
	 * diisi keduanya <b>selalu bernilai sama</b>. Pemisahan menjadi dua kolom baru bermakna
	 * setelah administrator mengaturnya secara eksplisit &mdash; itulah gunanya memisahkan
	 * keduanya sejak awal.</p>
	 *
	 * <p>Seluruh peringatan mengenai hak yang menyala karena penamaan berlaku sama di sini;
	 * lihat {@link #defaultAksesFeederSister()}.</p>
	 *
	 * <p><b>Getter murni</b> tanpa tulis-balik ke field.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses dasbor SISTER; tidak pernah
	 *         {@code null}
	 * @see #setBolehAksesSister(Boolean)
	 * @see #getBolehAksesFeeder()
	 */
	@Column(name = "boleh_akses_sister")
	public Boolean getBolehAksesSister() {
		return bolehAksesSister == null ? defaultAksesFeederSister() : bolehAksesSister;
	}

	/**
	 * Menetapkan hak akses dasbor SISTER.
	 *
	 * <p>Setter mentah tanpa penjagaan; berperilaku persis seperti
	 * {@link #setBolehAksesFeeder(Boolean)}, termasuk bahwa {@code null} berarti kembali ke
	 * bawaan berbasis nama dan karena itu bukan tindakan netral.</p>
	 *
	 * @param bolehAksesSister hak akses SISTER; {@code null} berarti kembali ke bawaan
	 *                         berbasis nama
	 * @see #getBolehAksesSister()
	 */
	public void setBolehAksesSister(Boolean bolehAksesSister) {
		this.bolehAksesSister = bolehAksesSister;
	}

	/**
	 * Nilai bawaan akses Feeder/SISTER bila admin belum mengaturnya: AKTIF untuk role ADMINISTRATOR,
	 * AKADEMIK, atau role yang namanya sama persis dengan "akademik"/"admin" (abaikan besar/kecil huruf).
	 *
	 * <p>Dipakai bersama oleh {@link #getBolehAksesFeeder()} dan
	 * {@link #getBolehAksesSister()}, yang karenanya selalu bernilai sama selama kolomnya
	 * masih {@code null}.</p>
	 *
	 * <h3>Dua tahap pemeriksaan</h3>
	 * <ol>
	 *   <li><b>Perbandingan persis {@code roleId}</b> terhadap {@link #ADMINISTRATOR}
	 *   ({@code "am"}) dan {@link #AKADEMIK} ({@code "Akademik"}). Memakai {@code equals}
	 *   sehingga <b>peka huruf besar-kecil</b>: baris peran yang tersimpan sebagai
	 *   {@code "akademik"} huruf kecil tidak lolos di tahap ini.</li>
	 *   <li><b>Perbandingan persis pada {@code roleName}</b> &mdash; bukan {@code roleId}
	 *   &mdash; terhadap {@code "akademik"} dan {@code "admin"}, setelah dikecilkan hurufnya.
	 *   Sejak perbaikan dok audit 2026-09-06 ini memakai {@code equals}, bukan lagi
	 *   {@code contains(...)}.</li>
	 * </ol>
	 *
	 * <h3>Satu-satunya (bersama {@link #getAksesGerbangPesantren()}) penurunan hak berbasis
	 * NAMA TAMPIL</h3>
	 * <p>Method ini adalah satu dari hanya dua tempat di kelas ini yang menurunkan hak dari
	 * {@link #getRoleName()} alih-alih dari {@code roleId}. Ini penting karena nama tampil
	 * adalah teks bebas yang lazim diubah administrator tanpa menganggapnya perubahan
	 * berdampak &mdash; sekadar "memperjelas penamaan".</p>
	 *
	 * <h3>RIWAYAT: pencocokan substring pada nama tampil (FIXED)</h3>
	 * <p>Sampai perbaikan dok audit 2026-09-06, tahap 2 memakai {@code contains(...)}, bukan
	 * {@code equals}, sehingga:</p>
	 * <ul>
	 *   <li>substring {@code "admin"} tercakup oleh kata yang sangat umum dalam bahasa
	 *   Indonesia: <i>Administrasi</i>, <i>Administratur</i>, <i>Tata Usaha Administrasi</i>
	 *   &mdash; sehingga banyak peran ketatausahaan biasa memperoleh akses pelaporan
	 *   Kementerian tanpa diniatkan;</li>
	 *   <li>substring {@code "akademik"} tercakup oleh <i>Staf Akademik</i>,
	 *   <i>Kalender Akademik</i>, <i>Wakil Dekan Bidang Akademik</i>, dan sejenisnya.</li>
	 * </ul>
	 * <p><b>Sekarang</b>, tahap 2 mensyaratkan nama (atau pengenal yang menjadi cadangannya,
	 * lihat di bawah) yang <b>sama persis</b> dengan {@code "admin"} atau {@code "akademik"}
	 * setelah dikecilkan hurufnya, sehingga contoh-contoh di atas tidak lagi lolos.</p>
	 * <p>Yang tidak berubah: {@link #getRoleName()} <b>jatuh ke {@code roleId}</b> bila nama
	 * kosong, sehingga peran tanpa nama pun tetap dinilai berdasarkan pengenalnya di tahap 2
	 * &mdash; pengenal persis {@code "admin"} (tanpa nama tampil terisi) lolos di sini meski
	 * tidak lolos di tahap 1, yang membandingkan terhadap {@link #ADMINISTRATOR}
	 * ({@code "am"}). Perhatikan pula bahwa memanggil {@code getRoleName()} berarti method ini
	 * <b>berpotensi menulis balik field</b> lewat getter tersebut, meski badannya sendiri
	 * tidak menugaskan apa pun.</p>
	 * <p>Karena kedua flag yang memakainya adalah gerbang server-side sungguhan, isilah
	 * keduanya secara eksplisit alih-alih mengandalkan nilai bawaan ini.</p>
	 *
	 * @return {@code true} bila peran ini memperoleh akses Feeder/SISTER secara bawaan
	 * @see #getBolehAksesFeeder()
	 * @see #getBolehAksesSister()
	 * @see #getRoleName()
	 */
	private boolean defaultAksesFeederSister() {
		String rid = getRoleId();
		if (rid != null && (rid.equals(ADMINISTRATOR) || rid.equals(AKADEMIK))) {
			return true;
		}
		String nm = getRoleName();
		if (nm != null) {
			String low = nm.toLowerCase();
			if (low.equals("akademik") || low.equals("admin")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Menentukan apakah peran ini boleh melihat <b>Info Kegiatan</b> (pengumuman dan agenda
	 * kegiatan institusi).
	 *
	 * <p>Berpola bawaan sama persis dengan {@link #getKalenderAkademik()}: menyala untuk
	 * seluruh peran <b>kecuali</b> {@link #ADMINISTRATOR} yang memperoleh {@code false}, dan
	 * dengan daftar-tolak {@link #KANTIN} yang mutlak. Alasannya sama &mdash; ini informasi
	 * bagi sivitas, bukan alat kerja pengelola sistem, sehingga halaman utama Administrator
	 * sengaja tidak dipenuhi pintasannya.</p>
	 *
	 * <p>Nilai bawaan {@code false} bagi Administrator hanya bawaan; menyalakannya secara
	 * eksplisit tetap dihormati.</p>
	 *
	 * <p><b>Getter murni</b> tanpa tulis-balik ke field. Seluruh pemanggilnya berada di
	 * lapisan tampilan ({@code MainAction2}, JSP navigasi); tidak ada gerbang server-side
	 * yang membacanya, sehingga flag ini murni kosmetik.</p>
	 *
	 * @return {@code true} bila Info Kegiatan boleh ditampilkan; tidak pernah {@code null}
	 * @see #setInfoKegiatan(Boolean)
	 * @see #getKalenderAkademik()
	 */
	public Boolean getInfoKegiatan() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		return infoKegiatan == null ? (getRoleId() != null && getRoleId().equals(ADMINISTRATOR) ? false : true)
				: infoKegiatan;
	}

	/**
	 * Menetapkan hak melihat Info Kegiatan.
	 *
	 * <p>Setter mentah tanpa penjagaan. {@code null} mengembalikan flag ke bawaan yang
	 * bergantung peran: {@code false} untuk {@link #ADMINISTRATOR}, {@code true} untuk peran
	 * lainnya.</p>
	 *
	 * @param infoKegiatan hak melihat info kegiatan; {@code null} berarti kembali ke bawaan
	 * @see #getInfoKegiatan()
	 */
	public void setInfoKegiatan(Boolean infoKegiatan) {
		this.infoKegiatan = infoKegiatan;
	}

	/**
	 * Menentukan apakah peran ini boleh melihat <b>data pegawai selain dirinya sendiri</b>.
	 *
	 * <p>Berbeda dari mayoritas flag di kelas ini yang hanya mengatur visibilitas menu, flag
	 * ini adalah <b>gerbang pembatas data yang sesungguhnya</b>. Ia dibaca langsung oleh
	 * {@link Tbmuser} pada jalur penentuan lingkup pusat, oleh {@code KinerjaPegawaiApi},
	 * dan oleh komponen pemilih data pegawai &mdash; masing-masing mempersempit kriteria
	 * query Hibernate, bukan sekadar menyembunyikan tombol. Mematikannya benar-benar
	 * mencabut kemampuan melihat data rekan kerja.</p>
	 *
	 * <h3>Struktur keputusan</h3>
	 * <ol>
	 *   <li><b>Daftar-tolak {@link #KANTIN}</b> &mdash; {@code false}.</li>
	 *   <li><b>Penolakan kelompok pengguna akhir</b> &mdash; {@link #DOSEN},
	 *   {@link #PEGAWAI}, {@link #GURU}, {@link #SISWA}, dan {@link #MAHASISWA} dikembalikan
	 *   {@code false} <b>tanpa membaca kolomnya</b>. Ini penolakan mutlak: mencentangnya di
	 *   layar Grup Pengguna tidak berpengaruh. Sifatnya <i>fail-closed</i> dan tepat &mdash;
	 *   seorang dosen atau pegawai biasa tidak boleh menelusuri data seluruh pegawai.</li>
	 *   <li><b>Bawaan bila kolom {@code null}</b> &mdash; {@code true} hanya untuk
	 *   {@link #ADMINISTRATOR}, {@code false} untuk peran lainnya. Ini pola
	 *   <i>fail-closed</i> yang benar: peran baru <b>tidak</b> memperoleh hak lintas-pegawai
	 *   secara diam-diam.</li>
	 * </ol>
	 *
	 * <p>Perhatikan bahwa langkah 2 di sini memakai {@code return false} langsung, bukan
	 * penulisan {@code false} ke field seperti pada {@link #getPengadaan()} dan
	 * {@link #getKeuangan()}. Karena itu <b>getter ini murni</b> dan tidak menerbitkan revisi
	 * audit palsu &mdash; perbedaan yang layak dicatat sebagai contoh pola yang benar.</p>
	 *
	 * <p>Perhatikan pula bahwa daftar penolakan di langkah 2 ditulis ulang secara harfiah di
	 * sini alih-alih memanggil {@link #roleEndUser()}, yang berisi persis kelima peran yang
	 * sama. {@link #roleEndUser()} baru diperkenalkan belakangan dan hanya dipakai
	 * {@link #getMelihatSemuaSop()}. Keduanya setara secara perilaku kecuali satu hal:
	 * {@link #roleEndUser()} memakai {@link #isRole(String)} yang <b>tidak peka huruf
	 * besar-kecil</b>, sedangkan penulisan harfiah di sini memakai {@code equals} yang peka.
	 * Baris peran yang tersimpan sebagai {@code "dosen"} huruf kecil karena itu
	 * <b>lolos</b> dari penolakan di sini namun tertolak di
	 * {@link #getMelihatSemuaSop()}.</p>
	 *
	 * @return {@code true} bila peran ini boleh melihat data pegawai lain; tidak pernah
	 *         {@code null}
	 * @see #setMelihatDataPegawaiLain(Boolean)
	 * @see #getMelihatDataSatkerLain()
	 * @see #roleEndUser()
	 */
	public Boolean getMelihatDataPegawaiLain() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		if (getRoleId() != null && getRoleId().equals(DOSEN)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(PEGAWAI)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(GURU)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(SISWA)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(MAHASISWA)) {
			return false;
		}

		return melihatDataPegawaiLain == null
				? (getRoleId() != null && getRoleId().equals(ADMINISTRATOR) ? true : false)
				: melihatDataPegawaiLain;
	}

	/**
	 * Menetapkan hak melihat data pegawai lain.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan <b>tidak berlaku</b> bagi
	 * {@link #KANTIN} dan kelima peran pengguna akhir yang ditolak mutlak &mdash; lihat
	 * {@link #getMelihatDataPegawaiLain()}.</p>
	 *
	 * <p>Berbeda dari flag keuangan, menyimpan {@code null} di sini <b>aman</b>: bawaannya
	 * {@code false} untuk semua peran kecuali {@link #ADMINISTRATOR}.</p>
	 *
	 * @param melihatDataPegawaiLain hak melihat data pegawai lain; {@code null} berarti
	 *                               kembali ke bawaan fail-closed
	 * @see #getMelihatDataPegawaiLain()
	 */
	public void setMelihatDataPegawaiLain(Boolean melihatDataPegawaiLain) {
		this.melihatDataPegawaiLain = melihatDataPegawaiLain;
	}

	/**
	 * Menentukan apakah peran ini boleh melihat <b>data satuan kerja selain miliknya</b>.
	 *
	 * <p>Bersama {@link #getMelihatDataPegawaiLain()}, ini adalah salah satu gerbang
	 * pembatas data terpenting di AIS. Satuan kerja adalah lingkup organisasi yang paling
	 * sering dipakai untuk memisahkan data antar unit di modul keuangan, akuntansi,
	 * penganggaran, penggajian, dan persuratan &mdash; sehingga flag ini efektif menentukan
	 * apakah seorang pengguna melihat data unitnya sendiri saja atau data seluruh
	 * institusi.</p>
	 *
	 * <p>Penegakannya nyata dan tersebar luas di sisi server: {@link Pegawai}, seluruh
	 * keluarga laporan penggajian dan KPI, pohon pemilih satuan kerja, serta modul SOP
	 * masing-masing mempersempit kriteria query berdasarkan nilai ini. Ia juga menjadi dasar
	 * {@link Tbmuser#ambilSatuanKerja()}.</p>
	 *
	 * <p><b>Struktur keputusannya identik baris demi baris</b> dengan
	 * {@link #getMelihatDataPegawaiLain()}: daftar-tolak {@link #KANTIN}, penolakan mutlak
	 * bagi kelima peran pengguna akhir ({@link #DOSEN}, {@link #PEGAWAI}, {@link #GURU},
	 * {@link #SISWA}, {@link #MAHASISWA}), lalu bawaan <i>fail-closed</i> yang hanya menyala
	 * untuk {@link #ADMINISTRATOR}. Seluruh catatan pada method tersebut &mdash; termasuk
	 * bahwa getter ini <b>murni</b> (memakai {@code return} langsung, bukan tulis-balik
	 * field) dan bahwa daftar penolakannya peka huruf besar-kecil &mdash; berlaku sama di
	 * sini.</p>
	 *
	 * <p>Perlu dibedakan dari {@link #getSatuanKerja()} dan {@link #getSatuanKerjas()}: flag
	 * ini menjawab "boleh melihat unit lain atau tidak", sedangkan kedua properti itu
	 * menetapkan <i>unit mana</i> yang menjadi lingkupnya.</p>
	 *
	 * @return {@code true} bila peran ini boleh melihat data satuan kerja lain; tidak pernah
	 *         {@code null}
	 * @see #setMelihatDataSatkerLain(Boolean)
	 * @see #getMelihatDataPegawaiLain()
	 * @see #getSatuanKerja()
	 * @see Tbmuser#ambilSatuanKerja()
	 */
	public Boolean getMelihatDataSatkerLain() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		if (getRoleId() != null && getRoleId().equals(DOSEN)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(PEGAWAI)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(GURU)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(SISWA)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(MAHASISWA)) {
			return false;
		}

		return melihatDataSatkerLain == null ? (getRoleId() != null && getRoleId().equals(ADMINISTRATOR) ? true : false)
				: melihatDataSatkerLain;
	}

	/**
	 * Menetapkan hak melihat data satuan kerja lain.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan <b>tidak berlaku</b> bagi
	 * {@link #KANTIN} dan kelima peran pengguna akhir &mdash; lihat
	 * {@link #getMelihatDataSatkerLain()}. Menyimpan {@code null} aman karena bawaannya
	 * <i>fail-closed</i>.</p>
	 *
	 * <p><b>Menyalakan flag ini adalah keputusan berdampak luas.</b> Ia membuka data lintas
	 * unit di modul keuangan, penggajian, penganggaran, dan persuratan sekaligus &mdash;
	 * bukan hanya pada satu layar. Perlakukan sebagai pemberian kewenangan tingkat
	 * institusi.</p>
	 *
	 * @param melihatDataSatkerLain hak melihat data satker lain; {@code null} berarti kembali
	 *                              ke bawaan fail-closed
	 * @see #getMelihatDataSatkerLain()
	 */
	public void setMelihatDataSatkerLain(Boolean melihatDataSatkerLain) {
		this.melihatDataSatkerLain = melihatDataSatkerLain;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses <b>Presensi Kehadiran</b>.
	 *
	 * <p>Getter dengan struktur bertingkat yang memperlakukan tiga kelompok peran secara
	 * berbeda:</p>
	 * <ol>
	 *   <li><b>Daftar-tolak {@link #KANTIN}</b> &mdash; {@code false} mutlak.</li>
	 *   <li><b>Pemaksaan menyala</b> bagi {@link #DOSEN}, {@link #PEGAWAI}, dan
	 *   {@link #GURU} &mdash; dikembalikan {@code true} <b>tanpa membaca kolomnya</b>.
	 *   Ketiganya adalah pihak yang wajib melakukan presensi, sehingga haknya tidak dapat
	 *   dicabut administrator. Perhatikan bahwa ini berbeda dari pola pemaksaan pada
	 *   {@link #getElearning()}: di sini nilainya <b>dikembalikan langsung</b>, tidak ditulis
	 *   ke field, sehingga pilihan administrator yang tersimpan tetap utuh di basis data dan
	 *   akan kembali berlaku bila peran itu suatu saat dikeluarkan dari daftar ini.</li>
	 *   <li><b>Bawaan mati</b> bagi {@link #SISWA} dan {@link #MAHASISWA} &mdash; kolomnya
	 *   tetap dibaca, tetapi bila {@code null} hasilnya {@code false}. Jadi peserta didik
	 *   tidak memperoleh presensi kepegawaian secara bawaan, namun administrator
	 *   <b>dapat</b> menyalakannya secara eksplisit (berguna untuk institusi yang memakai
	 *   presensi terpadu).</li>
	 *   <li><b>Bawaan menyala</b> bagi seluruh peran lainnya.</li>
	 * </ol>
	 *
	 * <p>Perhatikan ketidaksimetrisan yang disengaja: kelima peran {@link #roleEndUser()}
	 * yang ditolak mutlak pada {@link #getMelihatDataPegawaiLain()} di sini justru terbelah
	 * &mdash; tiga dipaksa menyala, dua berdefault mati. Presensi memang urusan pengguna
	 * akhir, kebalikan dari hak lintas-data.</p>
	 *
	 * <p><b>Getter murni</b> tanpa tulis-balik ke field.</p>
	 *
	 * <p>Penegakannya nyata di sisi server pada {@code BiometricApi}, yang memakainya sebagai
	 * gerbang API perangkat presensi biometrik &mdash; selain pemakaian visibilitas menu yang
	 * biasa.</p>
	 *
	 * @return {@code true} bila peran ini memiliki akses presensi kehadiran; tidak pernah
	 *         {@code null}
	 * @see #setPresensiKehadiran(Boolean)
	 * @see #getAbsenLangsung()
	 * @see #getAksesGerbangPesantren()
	 */
	public Boolean getPresensiKehadiran() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		if (getRoleId() != null && getRoleId().equals(DOSEN)) {
			return true;
		} else if (getRoleId() != null && getRoleId().equals(PEGAWAI)) {
			return true;
		} else if (getRoleId() != null && getRoleId().equals(GURU)) {
			return true;
		}

		if (getRoleId() != null && getRoleId().equals(SISWA)) {
			return presensiKehadiran == null ? false : presensiKehadiran;
		} else if (getRoleId() != null && getRoleId().equals(MAHASISWA)) {
			return presensiKehadiran == null ? false : presensiKehadiran;
		}

		return presensiKehadiran == null ? true : presensiKehadiran;
	}

	/**
	 * Menetapkan hak akses Presensi Kehadiran.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan <b>diabaikan</b> bagi
	 * {@link #KANTIN} (selalu mati) serta {@link #DOSEN}, {@link #PEGAWAI}, dan
	 * {@link #GURU} (selalu menyala) &mdash; lihat {@link #getPresensiKehadiran()}. Bagi
	 * {@link #SISWA} dan {@link #MAHASISWA}, nilai tersimpan justru <b>dihormati</b>, dan
	 * di sanalah setter ini paling bermakna.</p>
	 *
	 * @param presensiKehadiran hak presensi; {@code null} berarti kembali ke bawaan yang
	 *                          bergantung peran
	 * @see #getPresensiKehadiran()
	 */
	public void setPresensiKehadiran(Boolean presensiKehadiran) {
		this.presensiKehadiran = presensiKehadiran;
	}

	/**
	 * Hak memproses izin serta verifikasi keluar/kembali di pos keamanan.
	 * Terpisah dari presensi akademik agar prinsip least privilege terjaga.
	 * Role lama bernama Keamanan Pondok/Satpam otomatis aktif saat kolom null.
	 *
	 * <h3>Urutan penurunan nilai</h3>
	 * <ol>
	 *   <li>Bila kolomnya sudah diisi, nilai itu dipakai apa adanya &mdash; berbeda dari
	 *   sebagian besar getter lain, di sini <b>tidak ada</b> daftar-tolak {@link #KANTIN}
	 *   maupun penolakan kelompok pengguna akhir yang mendahuluinya.</li>
	 *   <li>Perbandingan {@code roleId} lewat {@link #isRole(String)} (tidak peka huruf
	 *   besar-kecil) terhadap {@link #ADMINISTRATOR}, {@link #KEAMANAN_PONDOK}, serta literal
	 *   {@code "Keamanan Pondok"}, {@code "Keamanan Pesantren"}, dan {@code "Satpam"}.</li>
	 *   <li><b>Perbandingan persis pada nama peran</b> (setelah dikecilkan hurufnya) terhadap
	 *   {@code "keamanan pondok"}, {@code "keamanan pesantren"}, dan {@code "satpam"} &mdash;
	 *   sejak perbaikan dok audit 2026-09-06 memakai {@code equals}, bukan lagi
	 *   {@code contains(...)}.</li>
	 * </ol>
	 *
	 * <h3>Penurunan hak berbasis nama tampil</h3>
	 * <p>Bersama {@link #defaultAksesFeederSister()}, langkah 3 menjadikan method ini satu
	 * dari hanya dua tempat di kelas ini yang menurunkan hak dari {@link #getRoleName()}
	 * &mdash; teks bebas yang lazim disunting administrator tanpa menganggapnya perubahan
	 * berdampak. Peran bernama <b>persis</b> "Satpam", "Keamanan Pondok", atau "Keamanan
	 * Pesantren" (tidak peka huruf besar-kecil) memperoleh hak ini tanpa pernah dicentang.</p>
	 * <p><b>RIWAYAT (FIXED dok audit 2026-09-06):</b> sebelumnya langkah 3 memakai pencocokan
	 * substring, sehingga nama seperti "Koordinator Satpam", "Keamanan Pondok Putri", atau
	 * "Pengawas Satpam" juga ikut memperoleh hak ini. Ketiga kata kunci di sini cukup spesifik
	 * sehingga risikonya jauh lebih rendah dibanding flag Feeder/SISTER, namun tetap sudah
	 * diperketat menjadi perbandingan persis. Tetap isi kolomnya secara eksplisit bila hak ini
	 * penting, karena {@code getRoleName()} juga <b>jatuh ke {@code roleId}</b> saat nama
	 * kosong sehingga cakupan pencocokannya lebih luas dari yang terlihat.</p>
	 *
	 * <p>Hak ini ditegakkan sungguhan di sisi server oleh {@code GerbangPesantrenApi}, bukan
	 * sekadar visibilitas menu. Ia sengaja dipisahkan dari
	 * {@link #getPresensiKehadiran()} agar prinsip <i>least privilege</i> terjaga: petugas
	 * pos tidak perlu hak presensi kepegawaian, dan sebaliknya.</p>
	 *
	 * <p><b>Getter murni</b> tanpa tulis-balik ke field.</p>
	 *
	 * @return {@code true} bila peran ini boleh memproses izin di pos keamanan; tidak pernah
	 *         {@code null}
	 * @see #setAksesGerbangPesantren(Boolean)
	 * @see #KEAMANAN_PONDOK
	 * @see #getPresensiKehadiran()
	 */
	@Column(name = "akses_gerbang_pesantren")
	public Boolean getAksesGerbangPesantren() {
		if (aksesGerbangPesantren != null) return aksesGerbangPesantren;
		if (isRole(ADMINISTRATOR) || isRole(KEAMANAN_PONDOK)
				|| isRole("Keamanan Pondok") || isRole("Keamanan Pesantren") || isRole("Satpam")) return Boolean.TRUE;
		String namaRole = getRoleName() == null ? "" : getRoleName().trim().toLowerCase(java.util.Locale.ENGLISH);
		return Boolean.valueOf(namaRole.equals("keamanan pondok")
				|| namaRole.equals("keamanan pesantren") || namaRole.equals("satpam"));
	}

	/**
	 * Menetapkan hak memproses izin serta verifikasi keluar/kembali di pos keamanan.
	 *
	 * <p>Setter mentah tanpa penjagaan. Berbeda dari kebanyakan flag lain di kelas ini, nilai
	 * yang disimpan <b>selalu dihormati</b> &mdash; tidak ada peran yang dipaksa menyala atau
	 * mati, karena {@link #getAksesGerbangPesantren()} memeriksa kolomnya lebih dulu sebelum
	 * segala penurunan bawaan.</p>
	 *
	 * <p><b>Menyimpan {@code null} bukan tindakan netral</b>: ia mengembalikan flag ke
	 * penurunan berbasis nama, yang untuk peran bernama persis "satpam", "keamanan pondok",
	 * atau "keamanan pesantren" justru berarti menyala. Untuk menutup akses, simpan
	 * {@link Boolean#FALSE} secara eksplisit.</p>
	 *
	 * @param value hak akses gerbang; {@code null} berarti kembali ke bawaan berbasis nama
	 * @see #getAksesGerbangPesantren()
	 */
	public void setAksesGerbangPesantren(Boolean value) {
		this.aksesGerbangPesantren = value;
	}

	/**
	 * Hak menyetujui/menolak pengajuan transaksi member yang melampaui limit.
	 * Fail-closed: role lama maupun role baru tidak mendapat hak ini sebelum
	 * dicentang eksplisit pada Grup Pengguna.
	 *
	 * <p><b>Contoh terbaik pola yang benar di kelas ini.</b> Badannya satu ternary murni:
	 * tidak ada penurunan berbasis {@code roleId} maupun {@code roleName}, tidak ada
	 * daftar-tolak, dan tidak ada tulis-balik ke field. Nilai bawaannya
	 * {@link Boolean#FALSE} tanpa syarat, sehingga hak ini <b>hanya</b> dimiliki peran yang
	 * secara sadar dicentang administrator &mdash; bahkan {@link #ADMINISTRATOR} pun tidak
	 * memperolehnya secara otomatis.</p>
	 *
	 * <p>Sikap <i>fail-closed</i> itu tepat mengingat konsekuensinya: hak ini
	 * ditegakkan sungguhan di sisi server oleh
	 * {@code PengajuanLimitMemberApiHelper} sebagai gerbang persetujuan transaksi yang
	 * melampaui batas kredit member. Menyalakannya berarti memberi kewenangan meloloskan
	 * transaksi di luar limit yang sudah ditetapkan.</p>
	 *
	 * <p>Flag baru di kelas ini sebaiknya mengikuti bentuk ini, bukan bentuk
	 * {@link #getKeuangan()}.</p>
	 *
	 * @return {@code true} bila peran ini boleh memverifikasi transaksi melebihi limit;
	 *         tidak pernah {@code null}
	 * @see #setBolehVerifikasiMemberMelebihiLimit(Boolean)
	 */
	@Column(name = "boleh_verifikasi_member_melebihi_limit")
	public Boolean getBolehVerifikasiMemberMelebihiLimit() {
		return bolehVerifikasiMemberMelebihiLimit == null
				? Boolean.FALSE : bolehVerifikasiMemberMelebihiLimit;
	}

	/**
	 * Menetapkan hak menyetujui/menolak pengajuan transaksi member yang melampaui limit.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan <b>selalu dihormati</b> &mdash;
	 * tidak ada peran yang dipaksa menyala atau mati.</p>
	 *
	 * <p>Berbeda dari flag keuangan dan Feeder/SISTER, menyimpan {@code null} di sini
	 * <b>aman</b>: bawaannya {@link Boolean#FALSE} tanpa syarat.</p>
	 *
	 * @param value hak verifikasi melebihi limit; {@code null} berarti kembali ke bawaan mati
	 * @see #getBolehVerifikasiMemberMelebihiLimit()
	 */
	public void setBolehVerifikasiMemberMelebihiLimit(Boolean value) {
		this.bolehVerifikasiMemberMelebihiLimit = value;
	}

	/**
	 * Menentukan apakah peran ini boleh melakukan <b>absensi langsung</b> &mdash; mencatat
	 * kehadiran seseorang di tempat, tanpa melalui perangkat biometrik.
	 *
	 * <p>Satu-satunya flag yang nilai bawaannya bergantung pada konstanta
	 * {@link #Presensi}: bila kolomnya masih {@code null}, hasilnya {@code true} hanya untuk
	 * peran berpengenal {@code "Presensi"} (dibandingkan dengan {@code equalsIgnoreCase},
	 * sehingga {@code "presensi"} huruf kecil pun lolos), dan {@code false} untuk seluruh
	 * peran lainnya. Perhatikan bahwa bahkan {@link #ADMINISTRATOR} <b>tidak</b>
	 * memperolehnya secara bawaan &mdash; pengecualian dari kebiasaan di kelas ini.</p>
	 *
	 * <p>Didahului daftar-tolak {@link #KANTIN} yang mutlak. <b>Getter murni</b> tanpa
	 * tulis-balik ke field.</p>
	 *
	 * <p>Perlu dibedakan dari {@link #getPresensiKehadiran()}: flag itu menentukan apakah
	 * seseorang ikut dalam sistem presensi (sebagai subjek yang diabsen), sedangkan flag ini
	 * menentukan apakah ia boleh <i>mengoperasikan</i> pencatatan kehadiran orang lain.</p>
	 *
	 * <p>Meski kewenangan mencatat kehadiran secara manual berkonsekuensi nyata terhadap
	 * penggajian, seluruh pemanggilnya hanya mengatur <b>visibilitas komponen</b>
	 * ({@code KehadiranPegawaiAction} memakainya pada {@code setVisible(...)}). Tidak ada
	 * gerbang server-side yang membacanya, sehingga mematikannya tidak mencegah pemanggilan
	 * langsung.</p>
	 *
	 * @return {@code true} bila peran ini boleh melakukan absensi langsung; tidak pernah
	 *         {@code null}
	 * @see #setAbsenLangsung(Boolean)
	 * @see #Presensi
	 * @see #getPresensiKehadiran()
	 */
	public Boolean getAbsenLangsung() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		return absenLangsung == null ? (getRoleId() != null && getRoleId().equalsIgnoreCase(Presensi)) : absenLangsung;
	}

	/**
	 * Menetapkan hak melakukan absensi langsung.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan dihormati untuk seluruh peran
	 * kecuali {@link #KANTIN}. Menyimpan {@code null} mengembalikan flag ke bawaan yang
	 * hanya menyala bagi peran {@link #Presensi}.</p>
	 *
	 * @param absenLangsung hak absensi langsung; {@code null} berarti kembali ke bawaan
	 * @see #getAbsenLangsung()
	 */
	public void setAbsenLangsung(Boolean absenLangsung) {
		this.absenLangsung = absenLangsung;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>Kinerja</b> (penilaian kinerja
	 * pegawai, KPI, SKP).
	 *
	 * <p>Struktur keputusannya:</p>
	 * <ol>
	 *   <li>daftar-tolak {@link #KANTIN} &mdash; {@code false};</li>
	 *   <li>penolakan {@link #SISWA} dan {@link #MAHASISWA} &mdash; {@code false}, karena
	 *   peserta didik tidak memiliki penilaian kinerja kepegawaian;</li>
	 *   <li>selain itu bawaan <b>menyala</b>.</li>
	 * </ol>
	 *
	 * <p>Perhatikan bahwa daftar penolakannya <b>hanya memuat dua</b> dari lima anggota
	 * {@link #roleEndUser()} &mdash; {@link #DOSEN}, {@link #GURU}, dan {@link #PEGAWAI}
	 * sengaja <i>tidak</i> ditolak, karena justru merekalah subjek penilaian kinerja. Ini
	 * kebalikan dari pola pada {@link #getMelihatDataPegawaiLain()}, dan menunjukkan bahwa
	 * keanggotaan {@code roleEndUser()} tidak boleh dianggap sebagai daftar tolak universal.
	 * </p>
	 *
	 * <p>Karena bawaannya menyala, setiap Grup Pengguna baru langsung memperoleh akses modul
	 * Kinerja. <b>Getter murni</b> tanpa tulis-balik ke field.</p>
	 *
	 * <p>Seluruh pemanggilnya berada di lapisan tampilan ({@code MainAction2}, JSP menu dan
	 * menu seluler); tidak ada gerbang server-side yang membacanya. Pembatasan data kinerja
	 * yang sesungguhnya berasal dari {@link #getMelihatDataPegawaiLain()} dan
	 * {@link #getMelihatDataSatkerLain()}, yang memang dibaca
	 * {@code KinerjaPegawaiApi} beserta laporan-laporan KPI.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses modul Kinerja; tidak pernah
	 *         {@code null}
	 * @see #setKinerja(Boolean)
	 * @see #getMelihatDataPegawaiLain()
	 */
	public Boolean getKinerja() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		if (getRoleId() != null && getRoleId().equals(SISWA)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(MAHASISWA)) {
			return false;
		}

		return kinerja == null ? true : kinerja;
	}

	/**
	 * Menetapkan hak akses modul Kinerja.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan <b>tidak berlaku</b> bagi
	 * {@link #KANTIN}, {@link #SISWA}, dan {@link #MAHASISWA} &mdash; lihat
	 * {@link #getKinerja()}. Menyimpan {@code null} mengembalikan flag ke bawaan
	 * <b>menyala</b>, bukan mencabutnya.</p>
	 *
	 * @param kinerja hak modul; {@code null} berarti kembali ke bawaan menyala
	 * @see #getKinerja()
	 */
	public void setKinerja(Boolean kinerja) {
		this.kinerja = kinerja;
	}

	/**
	 * Mengembalikan <b>lingkup satuan kerja</b> yang melekat pada peran ini.
	 *
	 * <p>Bila terisi, seluruh akun yang memakai peran ini akan dibatasi pada satuan kerja
	 * tersebut &mdash; terlepas dari satuan kerja pribadi masing-masing akun. Ini adalah
	 * salah satu properti paling berdampak di kelas ini, karena satuan kerja adalah lingkup
	 * pemisah data yang paling sering dipakai di modul keuangan, akuntansi, penganggaran,
	 * penggajian, dan persuratan.</p>
	 *
	 * <p>Perlu dibedakan dari dua hal yang bunyinya mirip:</p>
	 * <ul>
	 *   <li>{@link #getMelihatDataSatkerLain()} menjawab "boleh menembus batas unit atau
	 *   tidak"; properti ini menjawab "unit mana batasnya".</li>
	 *   <li>{@link #getSatuanKerjas()} (berakhiran {@code s}) menampung <b>banyak</b> kode
	 *   satuan kerja sebagai teks dipisah koma, tanpa kunci asing. Keduanya berdampingan dan
	 *   sangat mudah tertukar saat menyunting.</li>
	 * </ul>
	 *
	 * <h3>Resolusi proxy lewat {@code check(...)}</h3>
	 * <p>Relasinya {@code LAZY}, sehingga field dapat berisi proxy yang belum ter-inisialisasi
	 * atau sudah ter-<i>detach</i>. Pemanggilan
	 * {@link GeneralValueObject#check(Object) check(...)} mencoba me-resolve-nya lewat cache
	 * dan &mdash; bila perlu &mdash; lewat session baru. Ini pola standar di seluruh entity
	 * AIS.</p>
	 *
	 * <p><b>Getter penulis-balik field.</b> Hasil {@code check(...)} <b>ditugaskan kembali</b>
	 * ke field. Penugasan itu memang disyaratkan pola {@code check(...)} agar resolusi tidak
	 * terulang, namun ia tetap membuat entity berpotensi dianggap <i>dirty</i> oleh Hibernate
	 * &mdash; berlaku peringatan revisi audit palsu seperti pada dokumentasi kelas.</p>
	 *
	 * <p>Perlu diingat bahwa {@code check(...)} dapat mengembalikan argumennya <b>apa
	 * adanya</b> bila keempat tahap resolusinya gagal, sehingga method ini tetap dapat
	 * mengembalikan proxy rusak yang melempar exception saat disentuh. Inilah mekanisme yang
	 * sama dengan yang mendasari anomali resolusi peran pada
	 * {@link Tbmuser#hakAkses()}.</p>
	 *
	 * @return satuan kerja lingkup peran ini, atau {@code null} bila tidak dibatasi
	 * @see #setSatuanKerja(SatuanKerja)
	 * @see #getSatuanKerjas()
	 * @see #getMelihatDataSatkerLain()
	 * @see Tbmuser#ambilSatuanKerja()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menetapkan lingkup satuan kerja peran ini.
	 *
	 * <p>Setter mentah tanpa penjagaan &mdash; berbeda dari {@link #setYayasan(Yayasan)} dan
	 * {@link #setSekolah(Sekolah)} yang menolak object tanpa {@code id}. Object transien
	 * tanpa {@code id} karena itu dapat tersimpan di sini dan menghasilkan lingkup yang tidak
	 * dapat di-resolve.</p>
	 *
	 * <p><b>Menetapkan nilai di sini membatasi setiap akun pemakai peran ini</b>, menimpa
	 * satuan kerja pribadi mereka. Menyimpan {@code null} berarti peran tidak membawa
	 * batasan satuan kerja sendiri, sehingga lingkup pribadi tiap akun yang berlaku.</p>
	 *
	 * @param satuanKerja satuan kerja lingkup; {@code null} berarti tanpa batasan dari peran
	 * @see #getSatuanKerja()
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Menetapkan lingkup jurusan peran ini.
	 *
	 * <p>Setter mentah tanpa penjagaan. <b>Perhatikan efek lanjutannya:</b> mengisi jurusan
	 * di sini akan membuat {@link #getFakultas()} <b>mengabaikan</b> fakultas yang tersimpan
	 * dan menurunkannya dari jurusan ini. Lihat penjelasan lengkap pada getter tersebut.</p>
	 *
	 * <p>Menetapkan nilai di sini membatasi setiap akun pemakai peran ini pada jurusan
	 * tersebut, menimpa jurusan pribadi masing-masing akun.</p>
	 *
	 * @param jurusan jurusan lingkup; {@code null} berarti tanpa batasan dari peran
	 * @see #getJurusan()
	 * @see #getFakultas()
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan <b>lingkup jurusan</b> yang melekat pada peran ini.
	 *
	 * <p>Bila terisi, seluruh akun yang memakai peran ini dibatasi pada jurusan tersebut.
	 * {@link Tbmuser#ambilJurusan()} memberi <b>prioritas tertinggi</b> pada nilai ini
	 * &mdash; ia menimpa jurusan pribadi akun.</p>
	 *
	 * <p>Sekaligus menjadi <b>sumber turunan</b> bagi {@link #getFakultas()}: selama jurusan
	 * terisi, fakultas selalu diambil dari jurusan ini, bukan dari kolom fakultas
	 * sendiri.</p>
	 *
	 * <p>Relasi {@code LAZY} yang di-resolve lewat
	 * {@link GeneralValueObject#check(Object) check(...)}; <b>getter penulis-balik
	 * field</b>. Seluruh catatan pada {@link #getSatuanKerja()} berlaku sama.</p>
	 *
	 * @return jurusan lingkup peran ini, atau {@code null} bila tidak dibatasi
	 * @see #setJurusan(Jurusan)
	 * @see #getFakultas()
	 * @see Tbmuser#ambilJurusan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan lingkup fakultas peran ini.
	 *
	 * <p>Setter mentah tanpa penjagaan, namun <b>nilainya mudah menjadi sia-sia</b>: bila
	 * {@link #getJurusan()} terisi, {@link #getFakultas()} akan mengabaikan apa pun yang
	 * ditetapkan di sini dan menurunkan fakultas dari jurusan tersebut &mdash; bahkan
	 * menimpanya di field. Untuk membatasi peran pada sebuah fakultas, pastikan lingkup
	 * jurusan dikosongkan.</p>
	 *
	 * @param fakultas fakultas lingkup; {@code null} berarti tanpa batasan dari peran
	 * @see #getFakultas()
	 * @see #setJurusan(Jurusan)
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan <b>lingkup fakultas</b> yang melekat pada peran ini &mdash; diturunkan
	 * dari jurusan bila jurusan terisi.
	 *
	 * <h3>Kolom ini tidak selalu menang atas dirinya sendiri</h3>
	 * <p>Urutannya: {@code check(fakultas)} lebih dulu, lalu &mdash; <b>bila
	 * {@link #getJurusan()} tidak {@code null}</b> &mdash; nilainya <b>ditimpa</b> oleh
	 * {@code getJurusan().getFakultas()}. Artinya kolom {@code fakultas} hanya berlaku
	 * ketika kolom {@code jurusan} kosong.</p>
	 * <p>Ini konsisten secara data (sebuah jurusan memang bernaung di bawah satu fakultas,
	 * sehingga menyimpan keduanya secara terpisah berisiko tidak sinkron), namun berarti
	 * administrator yang mengisi <i>keduanya</i> dengan nilai yang berbeda tidak akan
	 * mendapat peringatan apa pun &mdash; fakultas pilihannya diabaikan diam-diam. Karena
	 * hasil turunan itu <b>ditulis balik ke field</b>, ketidaksesuaian tersebut juga akan
	 * <b>tersimpan permanen ke basis data</b> pada penyimpanan berikutnya, menghapus pilihan
	 * asli administrator.</p>
	 *
	 * <p>Pola turunan yang sama persis dipakai {@link #getYayasan()}, yang menurunkan
	 * nilainya dari {@link #getSekolah()}.</p>
	 *
	 * <p>Perhatikan bahwa {@link #getJurusan()} dipanggil <b>dua kali</b> (sekali untuk
	 * pemeriksaan {@code null}, sekali untuk mengambil nilainya), sehingga siklus
	 * {@code check(...)} ikut berjalan dua kali.</p>
	 *
	 * <p><b>Getter penulis-balik field</b>; berlaku peringatan revisi audit palsu seperti
	 * pada {@link #getSatuanKerja()}. {@link Tbmuser#ambilFakultas()} memberi prioritas
	 * tertinggi pada nilai ini.</p>
	 *
	 * @return fakultas lingkup peran ini (dari jurusan bila jurusan terisi), atau
	 *         {@code null} bila tidak dibatasi
	 * @see #setFakultas(Fakultas)
	 * @see #getJurusan()
	 * @see #getYayasan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);

		if (getJurusan() != null) {
			fakultas = getJurusan().getFakultas();
		}

		return fakultas;
	}

	/**
	 * Menetapkan lingkup yayasan peran ini &mdash; <b>menolak object tanpa {@code id}</b>.
	 *
	 * <p>Berbeda dari {@link #setSatuanKerja(SatuanKerja)}, {@link #setJurusan(Jurusan)}, dan
	 * {@link #setFakultas(Fakultas)} yang menerima apa saja, setter ini menormalkan object
	 * transien (yang {@code getId()}-nya {@code null}) menjadi {@code null}. Penjagaan itu
	 * mencegah tersimpannya lingkup yang tidak dapat di-resolve. Hanya
	 * {@link #setSekolah(Sekolah)} yang memiliki penjagaan serupa &mdash;
	 * ketidakkonsistenan yang perlu diketahui, bukan pola yang berlaku menyeluruh di kelas
	 * ini.</p>
	 *
	 * <p><b>Nilainya mudah menjadi sia-sia:</b> bila {@link #getSekolah()} terisi,
	 * {@link #getYayasan()} akan mengabaikan apa pun yang ditetapkan di sini dan
	 * menurunkannya dari sekolah tersebut.</p>
	 *
	 * @param yayasan yayasan lingkup; {@code null} atau object tanpa {@code id} disimpan
	 *                sebagai {@code null}
	 * @see #getYayasan()
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan <b>lingkup yayasan</b> yang melekat pada peran ini &mdash; diturunkan
	 * dari sekolah bila sekolah terisi.
	 *
	 * <p>Memakai pola turunan yang <b>sama persis</b> dengan {@link #getFakultas()}:
	 * {@code check(yayasan)} lebih dulu, lalu ditimpa {@code getSekolah().getYayasan()} bila
	 * sekolah tidak {@code null}. Pasangan sekolah&rarr;yayasan di sini sejajar dengan
	 * pasangan jurusan&rarr;fakultas di sana.</p>
	 *
	 * <p>Seluruh catatan pada {@link #getFakultas()} berlaku sama: kolom {@code yayasan}
	 * hanya berlaku ketika kolom {@code sekolah} kosong; ketidaksesuaian antara keduanya
	 * diabaikan diam-diam dan hasil turunannya <b>ditulis balik ke field</b> sehingga
	 * tersimpan permanen; serta {@link #getSekolah()} dipanggil dua kali.</p>
	 *
	 * <p>{@link Tbmuser#ambilYayasan()} memberi prioritas tertinggi pada nilai ini, di atas
	 * yayasan pribadi akun.</p>
	 *
	 * @return yayasan lingkup peran ini (dari sekolah bila sekolah terisi), atau {@code null}
	 *         bila tidak dibatasi
	 * @see #setYayasan(Yayasan)
	 * @see #getSekolah()
	 * @see #getFakultas()
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
	 * Mengembalikan <b>lingkup sekolah</b> yang melekat pada peran ini.
	 *
	 * <p>Bila terisi, seluruh akun yang memakai peran ini dibatasi pada sekolah tersebut,
	 * menimpa sekolah pribadi masing-masing akun &mdash; lihat
	 * {@link Tbmuser#ambilSekolah()}. Pada instalasi multi-sekolah (yayasan yang menaungi
	 * beberapa satuan pendidikan), inilah pemisah data antar-sekolah.</p>
	 *
	 * <p>Sekaligus menjadi <b>sumber turunan</b> bagi {@link #getYayasan()}: selama sekolah
	 * terisi, yayasan selalu diambil dari sekolah ini.</p>
	 *
	 * <p>Relasi {@code LAZY} yang di-resolve lewat
	 * {@link GeneralValueObject#check(Object) check(...)}; <b>getter penulis-balik
	 * field</b>. Berbeda dari {@link #getFakultas()} dan {@link #getYayasan()}, getter ini
	 * <b>tidak</b> memiliki logika turunan &mdash; ia salah satu dari dua getter relasi
	 * paling sederhana di kelas ini, bersama {@link #getSatuanKerja()}.</p>
	 *
	 * @return sekolah lingkup peran ini, atau {@code null} bila tidak dibatasi
	 * @see #setSekolah(Sekolah)
	 * @see #getYayasan()
	 * @see Tbmuser#ambilSekolah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);

		return sekolah;
	}

	/**
	 * Menetapkan lingkup sekolah peran ini &mdash; <b>menolak object tanpa {@code id}</b>.
	 *
	 * <p>Memiliki penjagaan yang sama dengan {@link #setYayasan(Yayasan)}: object transien
	 * dinormalkan menjadi {@code null}, mencegah tersimpannya lingkup yang tidak dapat
	 * di-resolve.</p>
	 *
	 * <p><b>Menetapkan nilai di sini juga menentukan yayasan</b>, karena
	 * {@link #getYayasan()} menurunkan nilainya dari sekolah ini dan menimpa kolom yayasan
	 * yang tersimpan.</p>
	 *
	 * @param sekolah sekolah lingkup; {@code null} atau object tanpa {@code id} disimpan
	 *                sebagai {@code null}
	 * @see #getSekolah()
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Menetapkan lingkup program studi peran ini.
	 *
	 * <p>Setter mentah tanpa penjagaan &mdash; tidak menolak object transien seperti
	 * {@link #setYayasan(Yayasan)} dan {@link #setSekolah(Sekolah)}.</p>
	 *
	 * <p>Menetapkan nilai di sini membatasi setiap akun pemakai peran ini pada program
	 * tersebut, menimpa program pribadi masing-masing akun &mdash; lihat
	 * {@link Tbmuser#ambilProgram()}.</p>
	 *
	 * @param program program lingkup; {@code null} berarti tanpa batasan dari peran
	 * @see #getProgram()
	 */
	public void setProgram(Program program) {
		this.program = program;
	}

	/**
	 * Mengembalikan <b>lingkup program studi</b> yang melekat pada peran ini.
	 *
	 * <p>Bila terisi, seluruh akun yang memakai peran ini dibatasi pada program tersebut.
	 * {@link Tbmuser#ambilProgram()} memberi prioritas tertinggi pada nilai ini, di atas
	 * program pribadi akun.</p>
	 *
	 * <h3>Satu-satunya getter relasi yang berbeda sendiri</h3>
	 * <p>Dua hal membedakannya dari kelima getter relasi lain di kelas ini:</p>
	 * <ul>
	 *   <li><b>Tidak memanggil {@link GeneralValueObject#check(Object) check(...)}.</b> Ia
	 *   mengembalikan field mentah, sehingga <b>getter murni</b> yang tidak pernah menandai
	 *   entity sebagai <i>dirty</i>. Konsekuensinya, bila field berisi proxy yang sudah
	 *   ter-<i>detach</i>, tidak ada upaya penyelamatan sama sekali &mdash; pemanggil akan
	 *   menerima proxy itu apa adanya dan berpotensi menemui
	 *   {@link org.hibernate.LazyInitializationException} saat menyentuhnya.</li>
	 *   <li><b>Strategi pengambilan yang berbeda.</b> Ia tidak memakai
	 *   {@code fetch = FetchType.LAZY} melainkan {@link Fetch @Fetch}
	 *   {@link FetchMode#SELECT}, sehingga Hibernate memuatnya lewat query {@code SELECT}
	 *   terpisah alih-alih {@code JOIN}. Karena {@code @ManyToOne} bersifat {@code EAGER}
	 *   secara bawaan, relasi ini praktis <b>selalu ikut dimuat</b> &mdash; itulah sebabnya
	 *   {@code check(...)} tidak diperlukan di sini, dan mengapa getter ini aman menjadi
	 *   murni.</li>
	 * </ul>
	 *
	 * @return program studi lingkup peran ini, atau {@code null} bila tidak dibatasi
	 * @see #setProgram(Program)
	 * @see Tbmuser#ambilProgram()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "program", nullable = true)
	public Program getProgram() {
		return program;
	}

	/**
	 * Mengembalikan <b>jenis jabatan</b> yang diasosiasikan dengan peran ini.
	 *
	 * <p>Berbeda dari kelima properti lingkup di sekitarnya ({@link #getSatuanKerja()},
	 * {@link #getJurusan()}, {@link #getFakultas()}, {@link #getYayasan()},
	 * {@link #getSekolah()}, {@link #getProgram()}), relasi ini <b>bukan pembatas data</b>.
	 * Tidak ada varian {@code ambilXxx()} di {@link Tbmuser} yang membacanya, dan tidak ada
	 * gerbang otorisasi yang memeriksanya. Ia adalah data referensi kepegawaian yang
	 * memetakan Grup Pengguna ke jenis jabatan struktural/fungsional, dipakai untuk
	 * pelaporan dan pengisian bawaan formulir.</p>
	 *
	 * <p>Relasi {@code LAZY} yang di-resolve lewat
	 * {@link GeneralValueObject#check(Object) check(...)}; <b>getter penulis-balik
	 * field</b> dengan seluruh catatan pada {@link #getSatuanKerja()} berlaku sama.</p>
	 *
	 * @return jenis jabatan peran ini, atau {@code null} bila tidak diasosiasikan
	 * @see #setJenisJabatan(JenisJabatan)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_jabatan", nullable = true)
	public JenisJabatan getJenisJabatan() {
		jenisJabatan = check(jenisJabatan);
		return jenisJabatan;
	}

	/**
	 * Menetapkan jenis jabatan yang diasosiasikan dengan peran ini.
	 *
	 * <p>Setter mentah tanpa penjagaan. Karena relasi ini bukan pembatas data (lihat
	 * {@link #getJenisJabatan()}), menetapkannya <b>tidak berdampak pada otorisasi</b>
	 * &mdash; berbeda dari setter lingkup di sekitarnya.</p>
	 *
	 * @param jenisJabatan jenis jabatan; {@code null} berarti tanpa asosiasi
	 * @see #getJenisJabatan()
	 */
	public void setJenisJabatan(JenisJabatan jenisJabatan) {
		this.jenisJabatan = jenisJabatan;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>Kepegawaian</b>.
	 *
	 * <p>Getter keempat dari empat bersaudara &mdash; badannya identik dengan
	 * {@link #getKeuangan()}, {@link #getPembayaran()}, dan {@link #getAkunting()}, kecuali
	 * <b>kata kunci pembandingnya</b>: di sini {@code "pegawai"}, bukan {@link #KEUANGAN}
	 * ({@code "keu"}).</p>
	 *
	 * <p>Urutannya: daftar-tolak {@link #KANTIN}; penulisan paksa {@code false} bagi
	 * {@link #MAHASISWA}, {@link #SISWA}, {@link #DOSEN}, dan {@link #GURU} di luar penjagaan
	 * {@code null}; lalu bawaan
	 * {@code roleId.equalsIgnoreCase("pegawai") || roleId.equals(ADMINISTRATOR)} &mdash;
	 * perbandingan persis sejak perbaikan dok audit 2026-09-06 (sebelumnya
	 * {@code roleId.toLowerCase().contains("pegawai")}).</p>
	 *
	 * <h3>Kata kunci yang tidak cocok dengan konstantanya sendiri</h3>
	 * <p>Perhatikan keanehan yang mudah luput: konstanta {@link #PEGAWAI} bernilai
	 * {@code "peg"}, sedangkan pencocokan di sini mensyaratkan {@code roleId} yang sama
	 * persis dengan {@code "pegawai"}. Karena {@code "peg"} <b>tidak sama dengan</b>
	 * {@code "pegawai"}, peran Pegawai justru <b>tidak</b> memperoleh hak kepegawaian secara
	 * bawaan &mdash; dan seandainya pun ia cocok, langkah penolakan sebelumnya tidak
	 * menyebutnya sehingga hasilnya akan berbeda lagi. Hanya peran berpengenal <b>persis</b>
	 * {@code "pegawai"} (tidak peka huruf besar-kecil) atau {@link #ADMINISTRATOR} yang
	 * memperoleh hak ini secara bawaan; sebelum perbaikan dok audit 2026-09-06, peran
	 * berpengenal seperti {@code "kepegawaian"}, {@code "admin_pegawai"}, atau
	 * {@code "datapegawai"} juga ikut memperolehnya lewat pencocokan substring.</p>
	 * <p>Risiko hak menyala karena kebetulan penamaan di sini kini tertutup sepenuhnya oleh
	 * perbandingan persis. Meski begitu, berlaku anjuran yang sama: isi flag secara
	 * eksplisit.</p>
	 *
	 * <p><b>Getter penulis-balik field</b> &mdash; berlaku peringatan revisi audit palsu.</p>
	 *
	 * <p>Seluruh pemanggilnya berada di lapisan tampilan ({@code MainAction2}, JSP menu);
	 * tidak ada gerbang server-side yang membacanya. Pembatasan data kepegawaian yang
	 * sesungguhnya berasal dari {@link #getMelihatDataPegawaiLain()}.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses modul Kepegawaian; tidak pernah
	 *         {@code null}
	 * @see #setKepegawaian(Boolean)
	 * @see #PEGAWAI
	 * @see #getKeuangan()
	 * @see #getMelihatDataPegawaiLain()
	 */
	public Boolean getKepegawaian() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		if (getRoleId() != null && getRoleId().equals(Tbmrole.MAHASISWA)) {
			kepegawaian = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.SISWA)) {
			kepegawaian = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.DOSEN)) {
			kepegawaian = false;
		} else if (getRoleId() != null && getRoleId().equals(Tbmrole.GURU)) {
			kepegawaian = false;
		}
		return kepegawaian == null
				? (getRoleId() != null
						&& (getRoleId().equalsIgnoreCase("pegawai") || getRoleId().equals(Tbmrole.ADMINISTRATOR)))
				: kepegawaian;
	}

	/**
	 * Menetapkan hak akses modul Kepegawaian.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan tidak berlaku bagi
	 * {@link #KANTIN} maupun keempat peran akademik yang ditolak paksa &mdash; lihat
	 * {@link #getKepegawaian()}. Menyimpan {@code null} mengembalikan flag ke bawaan berbasis
	 * perbandingan persis dengan {@code "pegawai"}.</p>
	 *
	 * @param kepegawaian hak modul; {@code null} berarti kembali ke bawaan berbasis nama
	 * @see #getKepegawaian()
	 */
	public void setKepegawaian(Boolean kepegawaian) {
		this.kepegawaian = kepegawaian;
	}

	/**
	 * Menentukan apakah peran ini boleh melihat <b>seluruh surat</b>, bukan hanya surat yang
	 * dikonsepnya sendiri.
	 *
	 * <p>Salah satu gerbang pembatas data yang <b>benar-benar ditegakkan di sisi server</b>.
	 * {@code SuratMasukAction}, {@code SuratKeluarAction}, dan dasbor persuratan
	 * masing-masing memakainya untuk mempersempit kriteria query Hibernate ke konseptor yang
	 * bersangkutan. Mematikannya benar-benar menyembunyikan surat milik orang lain, bukan
	 * sekadar menyembunyikan menu.</p>
	 *
	 * <p><b>Struktur keputusannya identik</b> dengan {@link #getMelihatDataPegawaiLain()} dan
	 * {@link #getMelihatDataSatkerLain()}: daftar-tolak {@link #KANTIN}, penolakan mutlak
	 * bagi kelima peran pengguna akhir ({@link #DOSEN}, {@link #PEGAWAI}, {@link #GURU},
	 * {@link #SISWA}, {@link #MAHASISWA}) tanpa membaca kolom, lalu bawaan
	 * <i>fail-closed</i> yang hanya menyala untuk {@link #ADMINISTRATOR}. <b>Getter
	 * murni</b> tanpa tulis-balik ke field.</p>
	 *
	 * <p>Perlu dibedakan dari {@link #getAdministrasi()}, yang membuka <i>modul</i>
	 * persuratan bagi hampir semua peran secara bawaan. Flag ini yang menentukan <i>cakupan
	 * data</i>-nya &mdash; pemisahan yang tepat: banyak orang boleh memakai modul surat,
	 * sedikit yang boleh melihat surat semua orang.</p>
	 *
	 * @return {@code true} bila peran ini boleh melihat seluruh surat; tidak pernah
	 *         {@code null}
	 * @see #setMelihatSemuaSurat(Boolean)
	 * @see #getMelihatSemuaSop()
	 * @see #getAdministrasi()
	 */
	public Boolean getMelihatSemuaSurat() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		if (getRoleId() != null && getRoleId().equals(DOSEN)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(PEGAWAI)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(GURU)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(SISWA)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(MAHASISWA)) {
			return false;
		}

		return melihatSemuaSurat == null ? (getRoleId() != null && getRoleId().equals(ADMINISTRATOR) ? true : false)
				: melihatSemuaSurat;
	}

	/**
	 * Menetapkan hak melihat seluruh surat.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan <b>tidak berlaku</b> bagi
	 * {@link #KANTIN} dan kelima peran pengguna akhir &mdash; lihat
	 * {@link #getMelihatSemuaSurat()}. Menyimpan {@code null} aman karena bawaannya
	 * <i>fail-closed</i>.</p>
	 *
	 * @param melihatSemuaSurat hak melihat semua surat; {@code null} berarti kembali ke
	 *                          bawaan fail-closed
	 * @see #getMelihatSemuaSurat()
	 */
	public void setMelihatSemuaSurat(Boolean melihatSemuaSurat) {
		this.melihatSemuaSurat = melihatSemuaSurat;
	}

	/**
	 * Menentukan apakah peran ini boleh <b>menyunting format/template laporan</b>.
	 *
	 * <p>Bawaan <i>fail-closed</i> yang benar: menyala hanya untuk {@link #ADMINISTRATOR},
	 * mati untuk seluruh peran lain, dengan daftar-tolak {@link #KANTIN} yang mutlak.
	 * Berbeda dari flag berlingkup luas di sekitarnya, di sini <b>tidak ada</b> penolakan
	 * kelompok pengguna akhir &mdash; tidak diperlukan, karena bawaannya sudah mati untuk
	 * semua kecuali Administrator.</p>
	 *
	 * <p><b>Getter murni</b> tanpa tulis-balik ke field.</p>
	 *
	 * <p>Ditegakkan sungguhan di sisi server oleh {@code CommonReport}, yang memakainya untuk
	 * memutuskan apakah pengguna boleh menyimpan perubahan template laporan. Ini kewenangan
	 * yang lebih berdampak daripada namanya: template laporan menentukan apa yang tercetak
	 * pada dokumen resmi, dan sebagian di antaranya berisi ekspresi yang dieksekusi saat
	 * laporan dijalankan. Berikan hanya kepada peran yang benar-benar mengelola format
	 * pelaporan.</p>
	 *
	 * @return {@code true} bila peran ini boleh menyunting format laporan; tidak pernah
	 *         {@code null}
	 * @see #setUpdateFormatLaporan(Boolean)
	 */
	public Boolean getUpdateFormatLaporan() {

		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return false;
		}
		return updateFormatLaporan == null ? (getRoleId() != null && getRoleId().equals(ADMINISTRATOR) ? true : false)
				: updateFormatLaporan;
	}

	/**
	 * Menetapkan hak menyunting format/template laporan.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan dihormati untuk seluruh peran
	 * kecuali {@link #KANTIN}. Menyimpan {@code null} aman karena bawaannya
	 * <i>fail-closed</i> untuk semua peran selain {@link #ADMINISTRATOR}.</p>
	 *
	 * @param updateFormatLaporan hak menyunting format laporan; {@code null} berarti kembali
	 *                            ke bawaan fail-closed
	 * @see #getUpdateFormatLaporan()
	 */
	public void setUpdateFormatLaporan(Boolean updateFormatLaporan) {
		this.updateFormatLaporan = updateFormatLaporan;
	}

	/**
	 * Mengembalikan kode peran, dengan cadangan jatuh ke {@link #getRoleId()}.
	 *
	 * <p>Kolom {@code kode} adalah pengenal alternatif yang dipakai sebagian modul dan
	 * pertukaran data yang memerlukan kode berbeda dari kunci primer. Bila tidak diisi, kode
	 * dianggap sama dengan pengenal peran &mdash; sehingga pemanggil selalu memperoleh nilai
	 * yang bermakna tanpa perlu memeriksa {@code null} sendiri.</p>
	 *
	 * <p><b>Getter murni</b> &mdash; berbeda dari {@link #getRoleName()} yang berpola serupa,
	 * method ini memakai ternary dan <b>tidak menulis balik</b> nilai cadangan ke field.
	 * Konsekuensinya kolom {@code kode} tetap {@code null} di basis data selama tidak diisi
	 * eksplisit, dan tidak ada risiko revisi audit palsu. Ini pola yang benar; sayangnya
	 * tidak diikuti {@link #getRoleName()}.</p>
	 *
	 * <p>Perhatikan bahwa pemeriksaannya hanya {@code kode == null} &mdash; string
	 * <b>kosong</b> atau berisi spasi saja akan dikembalikan apa adanya, tidak jatuh ke
	 * cadangan. Ini berbeda dari {@link #getRoleName()} yang juga menangani string kosong.
	 * Pemanggil yang mengandalkan nilai tak-kosong sebaiknya tetap berjaga.</p>
	 *
	 * @return kode peran, atau pengenal peran bila kode belum diisi
	 * @see #setKode(String)
	 * @see #getRoleId()
	 */
	public String getKode() {
		return kode == null ? getRoleId() : kode;
	}

	/**
	 * Menetapkan kode peran alternatif.
	 *
	 * <p>Setter mentah tanpa penjagaan: menerima {@code null} maupun string kosong, dan tidak
	 * memangkas spasi. Menyimpan {@code null} mengembalikan {@link #getKode()} ke cadangan
	 * {@link #getRoleId()}; menyimpan string kosong <b>tidak</b> &mdash; ia akan dikembalikan
	 * apa adanya.</p>
	 *
	 * @param kode kode peran; {@code null} berarti kembali ke cadangan pengenal peran
	 * @see #getKode()
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan daftar <b>kode satuan kerja tambahan</b> sebagai satu string dipisah
	 * koma.
	 *
	 * <p>Berbeda dari {@link #getSatuanKerja()} yang merupakan kunci asing tunggal, properti
	 * ini menampung <b>banyak</b> satuan kerja sekaligus &mdash; namun sebagai teks bebas
	 * tanpa relasi. Dipakai {@code SatuanKerjaTreeModel}, komponen pemilih satuan kerja,
	 * {@code SekolahUtil}, dan posting dana talangan untuk menyaring data di sisi server,
	 * sehingga ini <b>gerbang pembatas data yang nyata</b>, bukan sekadar tampilan.</p>
	 *
	 * <h3>Normalisasi dan keterbatasannya</h3>
	 * <p>Method ini memangkas spasi, lalu membersihkan empat bentuk "kosong tapi berisi koma"
	 * &mdash; {@code ","}, {@code ",,"}, {@code ",,,"}, dan {@code ",,,,"} &mdash; menjadi
	 * string kosong. Keempatnya diperiksa satu per satu dengan {@code equals} berantai,
	 * artinya penanganannya <b>harfiah, bukan umum</b>:</p>
	 * <ul>
	 *   <li>lima koma atau lebih ({@code ",,,,,"}) <b>tidak</b> tertangani dan lolos apa
	 *   adanya;</li>
	 *   <li>bentuk campuran seperti {@code ",a,"} atau {@code "a,,b"} juga tidak
	 *   dinormalkan.</li>
	 * </ul>
	 * <p>Karena hasilnya dipakai untuk menyaring data, entri kosong yang lolos dapat
	 * menghasilkan kode satuan kerja bernilai string kosong pada daftar hasil pemecahan
	 * &mdash; yang, bergantung pemanggilnya, dapat mempersempit atau justru memperlebar hasil
	 * saring secara tidak terduga. Pemanggil sebaiknya membuang entri kosong setelah
	 * memecah string ini, alih-alih mengandalkan pembersihan di sini.</p>
	 *
	 * <p><b>Getter penulis-balik field.</b> Hasil pemangkasan dan pembersihan
	 * <b>ditugaskan kembali</b> ke field, sehingga sekadar membaca daftar ini menandai entity
	 * sebagai <i>dirty</i> &mdash; berlaku peringatan revisi audit palsu seperti pada
	 * dokumentasi kelas. Perhatikan pula bahwa method ini <b>tidak pernah mengembalikan
	 * {@code null}</b>: nilai {@code null} dinormalkan menjadi string kosong dan ikut
	 * tertulis ke field.</p>
	 *
	 * @return daftar kode satuan kerja dipisah koma; string kosong bila tidak ada, tidak
	 *         pernah {@code null}
	 * @see #setSatuanKerjas(String)
	 * @see #getSatuanKerja()
	 * @see #getMelihatDataSatkerLain()
	 */
	@Column(columnDefinition = "text")
	public String getSatuanKerjas() {
		satuanKerjas = (satuanKerjas == null ? "" : satuanKerjas.trim());

		if (satuanKerjas.equals(",")) {
			satuanKerjas = "";
		} else if (satuanKerjas.equals(",,")) {
			satuanKerjas = "";
		} else if (satuanKerjas.equals(",,,")) {
			satuanKerjas = "";
		} else if (satuanKerjas.equals(",,,,")) {
			satuanKerjas = "";
		}

		return satuanKerjas;
	}

	/**
	 * Menetapkan daftar kode satuan kerja tambahan (dipisah koma).
	 *
	 * <p>Setter mentah tanpa penjagaan: tidak memangkas spasi, tidak membersihkan koma
	 * berlebih, dan tidak memvalidasi bahwa kode-kode di dalamnya benar-benar ada. Seluruh
	 * normalisasi &mdash; yang terbatas &mdash; terjadi di sisi baca
	 * ({@link #getSatuanKerjas()}).</p>
	 *
	 * <p>Karena nilainya menjadi gerbang pembatas data di sisi server, isi yang keliru di
	 * sini berdampak langsung pada data apa yang terlihat. Pastikan formatnya berupa kode
	 * yang dipisah koma tanpa entri kosong.</p>
	 *
	 * @param satuanKerjas daftar kode dipisah koma; disimpan apa adanya
	 * @see #getSatuanKerjas()
	 */
	public void setSatuanKerjas(String satuanKerjas) {
		this.satuanKerjas = satuanKerjas;
	}

	/**
	 * Menentukan apakah peran ini memiliki akses modul <b>Kantin/e-Kantin</b>.
	 *
	 * <p><b>Satu-satunya flag modul di kelas ini yang TIDAK memiliki daftar-tolak
	 * {@link #KANTIN}</b> &mdash; dan memang seharusnya begitu, karena justru peran kasirlah
	 * yang membutuhkannya. Perhatikan konsekuensinya: peran {@code "Kantin"} mendapat
	 * nilainya dari aturan bawaan biasa, bukan dari pengecualian.</p>
	 *
	 * <p>Struktur keputusannya:</p>
	 * <ol>
	 *   <li>{@link #SISWA} dan {@link #MAHASISWA} dikembalikan {@code false} &mdash;
	 *   peserta didik adalah <i>pembeli</i> di kantin, bukan pengelolanya;</li>
	 *   <li>selain itu bawaan
	 *   {@code roleId.equalsIgnoreCase(KEUANGAN) || roleId.equals(ADMINISTRATOR)} &mdash;
	 *   perbandingan persis sejak perbaikan dok audit 2026-09-06 (sebelumnya
	 *   {@code toLowerCase().contains("keu")}).</li>
	 * </ol>
	 *
	 * <p><b>Perhatikan kejanggalan pada nilai bawaannya.</b> Ia memakai kata kunci pembanding
	 * yang sama dengan keluarga flag keuangan, {@link #KEUANGAN} &mdash; sehingga peran
	 * {@link #KANTIN} sendiri (berpengenal {@code "Kantin"}, yang tidak sama dengan
	 * {@code "keu"} dan bukan {@code "am"}) justru <b>tidak</b> memperoleh hak ini secara
	 * bawaan, sementara peran keuangan memperolehnya. Kekeliruan itu ditutupi di lapisan
	 * atas oleh {@link #getTampilPos()}, yang menyalakan pintasan POS untuk peran
	 * {@link #KANTIN} secara eksplisit sebelum jatuh ke method ini. Bila mengandalkan flag
	 * ini untuk peran kasir, isilah kolomnya secara eksplisit.</p>
	 *
	 * <p><b>Getter murni</b> tanpa tulis-balik ke field.</p>
	 *
	 * <p>Dipakai sebagai gerbang render halaman di {@code modul/kantin/index.jsp} dan
	 * bilah navigasi &mdash; gerbang per-halaman yang nyata, namun tidak melindungi endpoint
	 * layanan di belakangnya. Otorisasi POS yang sesungguhnya ada di
	 * {@link #getEbisnisMenu()}.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengakses modul Kantin; tidak pernah
	 *         {@code null}
	 * @see #setKantin(Boolean)
	 * @see #getTampilPos()
	 * @see #getEbisnisMenu()
	 */
	public Boolean getKantin() {
		if (getRoleId() != null && getRoleId().equals(SISWA)) {
			return false;
		} else if (getRoleId() != null && getRoleId().equals(MAHASISWA)) {
			return false;
		}

		return kantin == null
				? (getRoleId() != null
						&& (getRoleId().equalsIgnoreCase(Tbmrole.KEUANGAN) || getRoleId().equals(Tbmrole.ADMINISTRATOR)))
				: kantin;
	}

	/**
	 * Menetapkan hak akses modul Kantin/e-Kantin.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan <b>tidak berlaku</b> bagi
	 * {@link #SISWA} dan {@link #MAHASISWA} &mdash; lihat {@link #getKantin()}.</p>
	 *
	 * <p><b>Menyimpan {@code null} bukan tindakan netral</b>: ia mengembalikan flag ke bawaan
	 * berbasis perbandingan persis dengan {@link #KEUANGAN}, yang secara janggal
	 * menyalakannya untuk peran keuangan namun tidak untuk peran {@link #KANTIN} sendiri.</p>
	 *
	 * @param kantin hak modul; {@code null} berarti kembali ke bawaan berbasis nama
	 * @see #getKantin()
	 */
	public void setKantin(Boolean kantin) {
		this.kantin = kantin;
	}

	/**
	 * Menentukan apakah pintasan "Dasbor POS" (kasir versi ZK) muncul di halaman
	 * utama untuk role ini. Default mengikuti role kantin/keuangan/administrator
	 * supaya pedagang &amp; kasir langsung punya akses tanpa setup tambahan.
	 *
	 * <p>Urutannya: bila kolom {@code tampil_pos} sudah diisi, nilai itu dipakai apa adanya;
	 * bila belum, peran {@link #KANTIN} memperoleh {@code true} secara eksplisit; selain itu
	 * hasilnya mengikuti {@link #getKantin()}. Cabang {@link #KANTIN} di tengah itulah yang
	 * menambal kejanggalan nilai bawaan {@code getKantin()}, yang &mdash; karena
	 * membandingkan {@code roleId} persis dengan {@link #KEUANGAN} ({@code "keu"}) &mdash;
	 * justru tidak mencakup peran kasir itu sendiri.</p>
	 *
	 * <p><b>Getter murni</b> tanpa tulis-balik ke field.</p>
	 *
	 * <h3>PENTING: flag ini hanya mengatur ikon, bukan akses</h3>
	 * <p>Sama seperti {@link #getEmedic()}, ini adalah <b>centang yang menyesatkan</b>.
	 * Seluruh pemanggilnya berada di lapisan tampilan ({@code MainAction},
	 * {@code MobileAction}, layar Grup Pengguna, dan proyeksi {@code HakAksesApi}). Tidak
	 * ada satu pun gerbang di {@code PosApi} yang membacanya &mdash; otorisasi POS yang
	 * sesungguhnya ditegakkan lewat {@link #getEbisnisMenu()}, dibantu
	 * {@link #getTokoAksesJson()} dan {@link #getBolehLihatSemuaToko()} untuk pembatasan per
	 * toko.</p>
	 * <p>Karena POS memproses penjualan dan penerimaan uang, selisih antara "ikon hilang" dan
	 * "akses tercabut" di sini berkonsekuensi nyata: mematikan flag ini <b>tidak</b>
	 * mencegah pengguna yang mengetahui URL-nya untuk membuka kasir. Untuk benar-benar
	 * mencabut akses, sunting katalog {@link #getEbisnisMenu()}.</p>
	 *
	 * @return {@code true} bila pintasan Dasbor POS ditampilkan; tidak pernah {@code null}
	 * @see #setTampilPos(Boolean)
	 * @see #getKantin()
	 * @see #getEbisnisMenu()
	 * @see #getEmedic()
	 */
	@Column(name = "tampil_pos")
	public Boolean getTampilPos() {
		if (tampilPos != null) {
			return tampilPos;
		}
		if (getRoleId() != null && getRoleId().equals(KANTIN)) {
			return true;
		}
		return getKantin();
	}

	/**
	 * Menetapkan penanda tampil pintasan "Dasbor POS".
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan <b>selalu dihormati</b>, karena
	 * {@link #getTampilPos()} memeriksa kolomnya lebih dulu sebelum segala penurunan
	 * bawaan &mdash; termasuk sebelum cabang khusus {@link #KANTIN}.</p>
	 *
	 * <p>Ingat bahwa &mdash; seperti dijelaskan pada {@link #getTampilPos()} &mdash; nilai
	 * ini hanya mengatur <b>visibilitas ikon</b>, sehingga menyimpan {@link Boolean#FALSE}
	 * di sini <b>bukan</b> tindakan pencabutan akses POS.</p>
	 *
	 * @param tampilPos penanda tampil; {@code null} berarti kembali ke bawaan
	 * @see #getTampilPos()
	 */
	public void setTampilPos(Boolean tampilPos) {
		this.tampilPos = tampilPos;
	}

	/**
	 * Menentukan apakah pintasan dasbor utama <b>"Koperasi"</b> muncul di halaman utama untuk role
	 * ini. Dasbor ini mengumpulkan seluruh dasbor koperasi (Simpan Pinjam, Laporan Simpan Pinjam,
	 * Pembagian SHU, dan Kantin/Toko) dalam satu tempat. Default hanya aktif untuk
	 * {@link #ADMINISTRATOR}; role lain dapat diaktifkan dari pengaturan role.
	 *
	 * <p><b>Getter murni</b> berbentuk ternary tunggal &mdash; tanpa daftar-tolak
	 * {@link #KANTIN}, tanpa penolakan kelompok pengguna akhir, dan tanpa tulis-balik ke
	 * field. Bawaannya <i>fail-closed</i> untuk seluruh peran selain
	 * {@link #ADMINISTRATOR}.</p>
	 *
	 * <p>Perlu dibedakan dari konstanta {@link #ANGGOTA_KOPERASI}: flag ini adalah hak
	 * <i>pengelola</i> koperasi (melihat dasbor Simpan Pinjam, SHU, dan Kantin/Toko),
	 * sedangkan konstanta itu adalah peran <i>anggota</i>.</p>
	 *
	 * <p>Dipakai sebagai gerbang render halaman di {@code modul/koperasi/index.jsp} dan
	 * {@code DashboardKantinAction} &mdash; gerbang per-halaman yang nyata, namun tidak
	 * melindungi endpoint layanan di belakangnya.</p>
	 *
	 * @return {@code true} bila pintasan dasbor Koperasi ditampilkan; tidak pernah
	 *         {@code null}
	 * @see #setDashboardKoperasi(Boolean)
	 * @see #ANGGOTA_KOPERASI
	 */
	@Column(name = "dashboard_koperasi")
	public Boolean getDashboardKoperasi() {
		return dashboardKoperasi == null
				? (getRoleId() != null && getRoleId().equals(Tbmrole.ADMINISTRATOR))
				: dashboardKoperasi;
	}

	/**
	 * Menetapkan penanda tampil pintasan dasbor Koperasi.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan selalu dihormati &mdash; tidak
	 * ada peran yang dipaksa menyala atau mati. Menyimpan {@code null} aman karena bawaannya
	 * <i>fail-closed</i> untuk seluruh peran selain {@link #ADMINISTRATOR}.</p>
	 *
	 * @param dashboardKoperasi penanda tampil; {@code null} berarti kembali ke bawaan
	 *                          fail-closed
	 * @see #getDashboardKoperasi()
	 */
	public void setDashboardKoperasi(Boolean dashboardKoperasi) {
		this.dashboardKoperasi = dashboardKoperasi;
	}

	/**
	 * Mengembalikan <b>halaman pendaratan</b> yang dituju pengguna seusai login.
	 *
	 * <p>Dibaca {@code ais.action.servlet.Main} dan beberapa JSP kerangka untuk mengalihkan
	 * pengguna ke modul yang relevan baginya, alih-alih ke halaman utama umum.</p>
	 *
	 * <p><b>Ini routing, bukan otorisasi.</b> Mengarahkan seseorang ke sebuah halaman tidak
	 * memberinya hak apa pun di sana, dan mengosongkan nilai ini tidak mencabut hak apa pun.
	 * Halaman tujuan tetap menegakkan izinnya sendiri.</p>
	 *
	 * <h3>Urutan penurunan</h3>
	 * <ol>
	 *   <li>Bila kolomnya terisi (bukan kosong/spasi), nilainya dikembalikan setelah
	 *   dipangkas.</li>
	 *   <li>Bila kosong dan perannya {@link #KANTIN}, dikembalikan
	 *   {@link #HALAMAN_UTAMA_KANTIN}.</li>
	 *   <li>Selain itu {@code null}, yang berarti "pakai halaman utama umum".</li>
	 * </ol>
	 *
	 * <p>Perhatikan urutannya: pemeriksaan kolom <b>mendahului</b> bawaan {@link #KANTIN}.
	 * Itu disengaja &mdash; sebagaimana dicatat komentar di dalam badan method &mdash; agar
	 * pilihan eksplisit administrator ({@link #HALAMAN_UTAMA_APOTIK},
	 * {@link #HALAMAN_UTAMA_EMEDIK}, {@link #HALAMAN_UTAMA_INVENTORY}) tidak tertimpa default
	 * historis peran Kantin. Bila urutannya terbalik, peran kasir tidak akan pernah bisa
	 * diarahkan ke POS Apotek.</p>
	 *
	 * <p><b>Getter murni</b> &mdash; memangkas spasi tanpa menulis balik ke field, dan
	 * menormalkan string kosong menjadi {@code null}.</p>
	 *
	 * @return path halaman pendaratan, atau {@code null} bila memakai halaman utama umum
	 * @see #setHalamanUtama(String)
	 * @see #HALAMAN_UTAMA_KANTIN
	 * @see #getDashboardDefaultMain()
	 */
	public String getHalamanUtama() {
		if (halamanUtama != null && !halamanUtama.trim().isEmpty()) {
			return halamanUtama.trim();
		}
		// Pertahankan default historis role Kantin, tetapi jangan menimpa pilihan
		// eksplisit POS Apotik/eMedik/Inventory yang disimpan administrator.
		return getRoleId() != null && getRoleId().equals(KANTIN) ? HALAMAN_UTAMA_KANTIN : null;
	}

	/**
	 * Menetapkan halaman pendaratan seusai login.
	 *
	 * <p>Setter mentah tanpa penjagaan: tidak memangkas spasi dan tidak memvalidasi bahwa
	 * path-nya benar-benar ada. Pemangkasan hanya terjadi di sisi baca
	 * ({@link #getHalamanUtama()}).</p>
	 *
	 * <p>Nilai yang wajar berasal dari konstanta {@code HALAMAN_UTAMA_*} di kelas ini.
	 * Menyimpan {@code null} atau string kosong mengembalikan perilaku ke bawaan &mdash;
	 * yang untuk peran {@link #KANTIN} berarti {@link #HALAMAN_UTAMA_KANTIN}, dan untuk peran
	 * lain berarti halaman utama umum.</p>
	 *
	 * <p>Karena ini murni routing, menetapkan path yang keliru menghasilkan kesalahan
	 * navigasi, bukan celah keamanan.</p>
	 *
	 * @param halamanUtama path halaman pendaratan; disimpan apa adanya
	 * @see #getHalamanUtama()
	 */
	public void setHalamanUtama(String halamanUtama) {
		this.halamanUtama = halamanUtama;
	}

	/**
	 * Mengembalikan <b>dasbor bawaan</b> yang ditampilkan di halaman utama bagi peran ini.
	 *
	 * <p>Dibaca {@code MainAction} untuk memilih dasbor mana yang dimuat pertama kali ketika
	 * pengguna membuka halaman utama. Berbeda dari {@link #getHalamanUtama()} yang
	 * mengalihkan ke <i>halaman</i> lain sama sekali, properti ini memilih <i>isi</i> di
	 * dalam halaman utama yang sama.</p>
	 *
	 * <p>Perlu dibedakan pula dari {@link #getDashboard()}, yang menentukan apakah dasbor
	 * ditampilkan sama sekali. Rangkaiannya: {@code getDashboard()} menentukan "ada dasbor
	 * atau tidak", properti ini menentukan "dasbor yang mana".</p>
	 *
	 * <p><b>Ini pemilihan tampilan, bukan otorisasi.</b> Menyetel sebuah dasbor di sini tidak
	 * memberi hak atasnya &mdash; dasbor yang bersangkutan tetap menegakkan flag-nya sendiri
	 * (mis. {@link #getDashboardKoperasi()}, {@link #getDasborRepository()}).</p>
	 *
	 * <p><b>Getter murni</b> &mdash; memangkas spasi tanpa menulis balik ke field, dan
	 * menormalkan string kosong menjadi {@code null}. Kolomnya berkapasitas 500 karakter,
	 * cukup untuk menampung pengenal dasbor beserta parameternya.</p>
	 *
	 * @return pengenal dasbor bawaan, atau {@code null} bila tidak disetel
	 * @see #setDashboardDefaultMain(String)
	 * @see #getDashboard()
	 * @see #getHalamanUtama()
	 */
	@Column(name = "dashboard_default_main", length = 500)
	public String getDashboardDefaultMain() {
		return dashboardDefaultMain == null || dashboardDefaultMain.trim().isEmpty() ? null
				: dashboardDefaultMain.trim();
	}

	/**
	 * Menetapkan dasbor bawaan yang ditampilkan di halaman utama.
	 *
	 * <p>Setter mentah tanpa penjagaan: tidak memangkas spasi dan tidak memvalidasi bahwa
	 * pengenal dasbornya dikenali. Pemangkasan hanya terjadi di sisi baca
	 * ({@link #getDashboardDefaultMain()}).</p>
	 *
	 * <p>Karena ini pemilihan tampilan, menetapkan nilai yang keliru menghasilkan dasbor
	 * kosong atau halaman utama biasa, bukan celah keamanan.</p>
	 *
	 * @param dashboardDefaultMain pengenal dasbor bawaan; disimpan apa adanya
	 * @see #getDashboardDefaultMain()
	 */
	public void setDashboardDefaultMain(String dashboardDefaultMain) {
		this.dashboardDefaultMain = dashboardDefaultMain;
	}

	/**
	 * Menentukan apakah peran ini boleh <b>mengajukan pengajuan atas nama pegawai lain</b>
	 * (cuti, izin, dan pengajuan kepegawaian sejenis).
	 *
	 * <p>Bawaan <i>fail-closed</i> tanpa syarat: bila kolomnya masih {@code null}, hasilnya
	 * {@code false} untuk <b>seluruh peran, termasuk {@link #ADMINISTRATOR}</b>. Tidak ada
	 * daftar-tolak, tidak ada penurunan berbasis nama, dan <b>getter murni</b> berbentuk
	 * ternary tunggal. Bersama {@link #getBolehVerifikasiMemberMelebihiLimit()} dan
	 * {@link #getBolehLihatSemuaToko()}, ini contoh pola yang benar di kelas ini.</p>
	 *
	 * <p>Sikap tersebut tepat: mengajukan sesuatu atas nama orang lain berarti bertindak
	 * mewakili, sehingga harus merupakan pemberian kewenangan yang disengaja.</p>
	 *
	 * <p>Perlu dibedakan dari {@link #getMelihatDataPegawaiLain()} &mdash; flag itu soal
	 * <i>melihat</i>, flag ini soal <i>bertindak</i>. Keduanya independen: peran dapat
	 * memiliki salah satunya saja.</p>
	 *
	 * <p><b>Cakupan penegakannya terbatas.</b> Satu-satunya pemakainya di luar layar
	 * administrasi dan proyeksi {@code HakAksesApi} adalah {@code CutiDanIzinAction}, yang
	 * memakainya untuk meng-<i>enable</i>/<i>disable</i> komponen formulir &mdash; bukan
	 * untuk menolak penyimpanan di sisi server. Jangan mengandalkannya sebagai kontrol akses
	 * tunggal.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengajukan atas nama pegawai lain; tidak
	 *         pernah {@code null}
	 * @see #setMengajukanPengajuanPegawaiLain(Boolean)
	 * @see #getMelihatDataPegawaiLain()
	 */
	public Boolean getMengajukanPengajuanPegawaiLain() {
		return mengajukanPengajuanPegawaiLain == null ? false : mengajukanPengajuanPegawaiLain;
	}

	/**
	 * Menetapkan hak mengajukan pengajuan atas nama pegawai lain.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan selalu dihormati &mdash; tidak
	 * ada peran yang dipaksa menyala atau mati. Menyimpan {@code null} aman karena bawaannya
	 * {@code false} tanpa syarat.</p>
	 *
	 * @param mengajukanPengajuanPegawaiLain hak mengajukan atas nama orang lain; {@code null}
	 *                                       berarti kembali ke bawaan mati
	 * @see #getMengajukanPengajuanPegawaiLain()
	 */
	public void setMengajukanPengajuanPegawaiLain(Boolean mengajukanPengajuanPegawaiLain) {
		this.mengajukanPengajuanPegawaiLain = mengajukanPengajuanPegawaiLain;
	}

	/**
	 * Menentukan apakah peran ini boleh melakukan <b>entri topup saldo</b> &mdash; menambah
	 * saldo pada kartu/akun member e-Kantin.
	 *
	 * <p>Badannya adalah <b>satu ternary murni</b>: {@code bolehEntryTopup == null ?
	 * Boolean.FALSE : bolehEntryTopup} &mdash; <i>fail-closed</i> tanpa syarat, tanpa
	 * daftar-tolak, tanpa penurunan dari {@code roleId} atau {@code roleName}, dan tanpa
	 * tulis-balik ke field. Ini <b>contoh pola yang paling dianjurkan</b> di kelas ini untuk
	 * flag baru, sejajar dengan {@link #getBolehVerifikasiMemberMelebihiLimit()}.</p>
	 *
	 * <p>Ia adalah <b>gerbang transaksional sungguhan</b> di sisi server &mdash;
	 * {@code KantinHelper}, {@code PenyesuaianSaldoHelper}, dan {@code PosApi} masing-masing
	 * menolak operasi topup bila nilainya {@code false}. Menyalakannya berarti memberi
	 * kewenangan <b>menciptakan saldo</b>, yang setara dengan kewenangan kas &mdash; karena
	 * itu, tidak ada peran yang memperolehnya secara bawaan; harus dicentang eksplisit oleh
	 * administrator pada setiap Grup Pengguna yang membutuhkannya.</p>
	 *
	 * <h3>RIWAYAT: kombinasi paling berisiko di kelas ini (FIXED)</h3>
	 * <p>Sampai perbaikan dok audit 2026-09-06, badan method ini menggabungkan dua sifat yang
	 * seharusnya tidak bertemu: gerbang transaksional sungguhan di atas, dengan nilai bawaan
	 * yang diturunkan lewat <b>pencocokan substring</b>
	 * {@code roleId.toLowerCase().contains("keu") || roleId.equals(ADMINISTRATOR)} &mdash;
	 * sama persis dengan {@link #getKeuangan()} dan saudara-saudaranya pada saat itu.
	 * Akibatnya, sebuah Grup Pengguna baru yang diberi pengenal mengandung {@code "keu"}
	 * &mdash; termasuk yang justru dimaksudkan membatasi, seperti {@code "keu_lihat_saja"}
	 * atau {@code "keu_readonly"} &mdash; diam-diam memperoleh kewenangan menambah saldo
	 * selama kolom ini belum pernah diisi, tanpa pernah muncul sebagai centang yang disengaja
	 * siapa pun. Itu adalah satu-satunya tempat di kelas ini di mana penurunan berbasis nama
	 * bertemu langsung dengan gerbang keuangan.</p>
	 * <p><b>Sekarang:</b> flag ini tidak lagi membaca {@code roleId} sama sekali. Meski
	 * begitu, tetap isi kolom ini secara eksplisit untuk setiap peran yang membutuhkannya,
	 * alih-alih mengandalkan asumsi apa pun mengenai nilai bawaannya.</p>
	 *
	 * <p><b>Perhatikan keusangan cache.</b> Nilai yang dibaca lewat
	 * {@link Tbmuser#hakAkses()} berasal dari cache peran yang tidak pernah kedaluwarsa,
	 * sehingga perubahan flag ini oleh administrator tidak langsung berlaku bagi sesi yang
	 * sedang berjalan. Karena itu {@link Tbmuser#bolehEntryTopupAktif()} sengaja memuat ulang
	 * baris peran lewat {@link org.hibernate.Session} tersendiri dan memanggil
	 * {@code session.refresh(...)} sebelum membaca flag ini &mdash; <b>gunakan method itu</b>,
	 * bukan getter ini secara langsung, bila keputusan yang diambil bersifat transaksional.</p>
	 *
	 * @return {@code true} bila peran ini boleh melakukan entri topup; tidak pernah
	 *         {@code null}
	 * @see #setBolehEntryTopup(Boolean)
	 * @see Tbmuser#bolehEntryTopupAktif()
	 * @see #KEUANGAN
	 * @see #getKeuangan()
	 */
	public Boolean getBolehEntryTopup() {
		return bolehEntryTopup == null ? Boolean.FALSE : bolehEntryTopup;
	}

	/**
	 * Menetapkan hak melakukan entri topup saldo.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan selalu dihormati &mdash; tidak
	 * ada peran yang dipaksa menyala atau mati.</p>
	 *
	 * <p><b>Menyimpan {@code null} kini aman &mdash; ini justru tindakan netral.</b> Sejak
	 * perbaikan dok audit 2026-09-06, {@link #getBolehEntryTopup()} sudah <i>fail-closed</i>
	 * tanpa syarat, sehingga {@code null} di sini berarti kewenangan menambah saldo
	 * <b>tidak menyala</b>, sama seperti bila disimpan {@link Boolean#FALSE} secara eksplisit.
	 * Sebelum perbaikan itu, menyimpan {@code null} justru mengembalikan flag ke penurunan
	 * berbasis substring {@code "keu"} pada {@code roleId}, yang untuk peran bernama demikian
	 * berarti kewenangan menambah saldo menyala secara diam-diam &mdash; konsekuensi paling
	 * serius dari pola itu di seluruh kelas ini. Lihat riwayat lengkapnya pada
	 * {@link #getBolehEntryTopup()}. Untuk menyalakan akses, simpan {@link Boolean#TRUE}
	 * secara eksplisit.</p>
	 *
	 * <p>Perubahan flag ini tidak langsung berlaku bagi sesi yang sedang berjalan karena
	 * cache peran tidak kedaluwarsa; lihat {@link Tbmuser#bolehEntryTopupAktif()}.</p>
	 *
	 * @param bolehEntryTopup hak entri topup; {@code null} berarti kembali ke bawaan
	 *                        fail-closed ({@code false})
	 * @see #getBolehEntryTopup()
	 */
	public void setBolehEntryTopup(Boolean bolehEntryTopup) {
		this.bolehEntryTopup = bolehEntryTopup;
	}

	/**
	 * Mengembalikan katalog izin menu <b>POS/e-Bisnis</b> sebagai JSON mentah.
	 *
	 * <p><b>Ini lapisan izin yang paling nyata di kelas ini.</b> Meski secara struktur hanya
	 * sebuah kolom teks, isinya dibaca oleh <b>lebih dari seratus</b> titik pemeriksaan di
	 * {@code ais.action.servlet.api} lewat {@code EbisnisMenuKatalog.bolehAksi(...)} dan
	 * {@code bolehAksiAkuntansi(...)} &mdash; masing-masing menolak operasi CRUD yang tidak
	 * diizinkan. Untuk modul POS, kasir, apotek, eMedik, inventory, dan akuntansi eBisnis,
	 * <b>inilah yang sesungguhnya menegakkan otorisasi</b>, bukan flag Boolean seperti
	 * {@link #getTampilPos()}, {@link #getEmedic()}, atau {@link #getAkunting()} yang hanya
	 * mengatur visibilitas ikon.</p>
	 * <p>Konsekuensi praktisnya penting bagi administrator: untuk benar-benar mencabut akses
	 * seseorang ke modul-modul tersebut, <b>kolom inilah yang harus disunting</b>.
	 * Mengosongkan centang modul di layar Grup Pengguna hanya menyembunyikan pintasannya.</p>
	 *
	 * <h3>PERHATIAN: satu-satunya kolom yang TIDAK diaudit</h3>
	 * <p>Kolom ini beranotasi
	 * {@link org.hibernate.envers.NotAudited @NotAudited}, sehingga <b>dikecualikan dari
	 * riwayat versi Envers</b> yang berlaku bagi seluruh kolom lain di kelas ini. Perlu
	 * disadari akibatnya: setiap perubahan pada kolom ini &mdash; yaitu perubahan pada
	 * lapisan izin yang paling menentukan &mdash; <b>tidak meninggalkan jejak audit sama
	 * sekali</b>, sementara perubahan pada flag kosmetik seperti {@link #getInfoKegiatan()}
	 * terekam lengkap. Untuk pertanyaan "siapa yang memberi peran ini akses transaksi POS,
	 * dan kapan", riwayat Envers tidak dapat menjawabnya. Ini perlu diperhitungkan saat
	 * melakukan audit kepatuhan atau penelusuran insiden.</p>
	 *
	 * <h3>Cara memakainya</h3>
	 * <p>Method ini mengembalikan <b>teks mentah</b>, bukan struktur terurai. Nilainya dapat
	 * {@code null} (kolom kosong dinormalkan menjadi {@code null}), sehingga <b>jangan</b>
	 * memanggil {@code new JSONObject(role.getEbisnisMenu())} secara langsung &mdash; itu
	 * akan melempar exception untuk peran yang belum pernah dikonfigurasi, yang merupakan
	 * keadaan normal. Selalu urai lewat {@code EbisnisMenuKatalog.urai(String)}, yang
	 * mengembalikan nilai bawaan yang aman.</p>
	 *
	 * <p><b>Getter murni</b> &mdash; memangkas spasi tanpa menulis balik ke field.</p>
	 *
	 * @return JSON mentah (bisa {@code null}) -- pemanggil pakai {@link
	 *         ais.common.EbisnisMenuKatalog#urai(String)} utk parse dgn default aman, JANGAN
	 *         {@code new JSONObject(role.getEbisnisMenu())} langsung (meledak kalau null/kosong).
	 * @see #setEbisnisMenu(String)
	 * @see #getTokoAksesJson()
	 * @see #getBolehLihatSemuaToko()
	 */
	@org.hibernate.envers.NotAudited
	@Column(name = "ebisnis_menu", columnDefinition = "text")
	public String getEbisnisMenu() {
		return ebisnisMenu == null || ebisnisMenu.trim().isEmpty() ? null : ebisnisMenu.trim();
	}

	/**
	 * Menetapkan katalog izin menu POS/e-Bisnis sebagai JSON mentah.
	 *
	 * <p>Setter mentah tanpa penjagaan: tidak memangkas spasi dan &mdash; yang lebih penting
	 * &mdash; <b>tidak memvalidasi bahwa isinya JSON yang sah</b>. Menyimpan teks rusak di
	 * sini tidak akan gagal saat penyimpanan; kegagalannya baru muncul saat penguraian, yang
	 * ditangani {@code EbisnisMenuKatalog.urai(String)} dengan jatuh ke nilai bawaan. Artinya
	 * isi yang rusak dapat <b>diam-diam mengubah izin</b> menjadi nilai bawaan, bukan
	 * memunculkan kesalahan. Selalu bangun JSON-nya lewat katalog, jangan merangkai teks
	 * sendiri.</p>
	 *
	 * <p><b>Perubahan di sini tidak terekam Envers</b> karena kolomnya {@code @NotAudited}
	 * &mdash; lihat peringatan pada {@link #getEbisnisMenu()}. Bila jejak perubahan izin
	 * diperlukan, catat secara terpisah.</p>
	 *
	 * @param ebisnisMenu JSON katalog izin; disimpan apa adanya, tanpa validasi
	 * @see #getEbisnisMenu()
	 */
	public void setEbisnisMenu(String ebisnisMenu) {
		this.ebisnisMenu = ebisnisMenu;
	}

	/**
	 * Mengembalikan daftar <b>toko yang boleh diakses</b> peran ini, sebagai JSON mentah.
	 *
	 * <p>Menyebut toko satu per satu. Ini gerbang pembatas data yang nyata di sisi server:
	 * {@code PosApi}, {@code KantinHelper}, dan {@code DashboardKantinAction} memakainya
	 * untuk mempersempit data transaksi ke toko-toko yang tercantum. Pada instalasi
	 * multi-toko, inilah pemisah data antar unit usaha.</p>
	 *
	 * <p>Bekerja berpasangan dengan {@link #getBolehLihatSemuaToko()}: flag itu berarti
	 * "semua toko aktif, termasuk yang dibuat setelah peran ini disimpan", sedangkan daftar
	 * di sini bersifat eksplisit dan <b>tidak</b> ikut mencakup toko baru. Untuk peran
	 * pengawas yang harus selalu melihat seluruh toko, pakai flag tersebut &mdash; jangan
	 * mengandalkan daftar ini yang akan tertinggal setiap kali toko baru dibuka.</p>
	 *
	 * <p>Berbeda dari {@link #getEbisnisMenu()}, kolom ini <b>ikut diaudit</b> Envers seperti
	 * kolom lain di kelas ini.</p>
	 *
	 * <p><b>Getter murni</b> &mdash; memangkas spasi tanpa menulis balik ke field, dan
	 * menormalkan string kosong menjadi {@code null}. Seperti pada {@link #getEbisnisMenu()},
	 * nilai {@code null} adalah keadaan normal bagi peran yang belum dikonfigurasi, sehingga
	 * pemanggil <b>tidak boleh</b> mengurainya secara langsung tanpa penjagaan.</p>
	 *
	 * @return JSON daftar toko, atau {@code null} bila belum dikonfigurasi
	 * @see #setTokoAksesJson(String)
	 * @see #getBolehLihatSemuaToko()
	 * @see #getEbisnisMenu()
	 */
	@Column(name = "toko_akses_json", columnDefinition = "text")
	public String getTokoAksesJson() {
		return tokoAksesJson == null || tokoAksesJson.trim().isEmpty() ? null : tokoAksesJson.trim();
	}

	/**
	 * Menetapkan daftar toko yang boleh diakses peran ini, sebagai JSON mentah.
	 *
	 * <p>Setter mentah tanpa penjagaan dan <b>tanpa validasi JSON</b>. Seluruh catatan pada
	 * {@link #setEbisnisMenu(String)} berlaku sama: isi yang rusak tidak gagal saat
	 * penyimpanan, melainkan diam-diam jatuh ke nilai bawaan saat penguraian.</p>
	 *
	 * <p>Karena nilainya menjadi gerbang pembatas data lintas toko, isi yang keliru
	 * berdampak langsung pada data transaksi apa yang terlihat.</p>
	 *
	 * @param tokoAksesJson JSON daftar toko; disimpan apa adanya, tanpa validasi
	 * @see #getTokoAksesJson()
	 */
	public void setTokoAksesJson(String tokoAksesJson) {
		this.tokoAksesJson = tokoAksesJson;
	}

	/**
	 * Hak akses jurnal ilmiah; interpretasi hanya melalui JurnalAksesKatalog.
	 *
	 * <p>Katalog izin modul Jurnal Ilmiah (OJS-like) dalam bentuk JSON mentah &mdash; pola
	 * yang sama dengan {@link #getEbisnisMenu()} dan {@link #getTokoAksesJson()}: satu kolom
	 * teks yang menampung banyak izin sekaligus, agar penambahan izin baru tidak memerlukan
	 * {@code ALTER TABLE} beserta migrasi tabel audit Envers.</p>
	 *
	 * <p>Ditegakkan sungguhan di sisi server oleh {@code JurnalAuthorizationService} lewat
	 * {@code bolehWorkflow()} dan {@code bolehMenu()}, yang mengatur peran editorial
	 * (editor, reviewer, penulis) pada alur penerbitan.</p>
	 *
	 * <p>Sebagaimana ditegaskan pada baris pertama: <b>jangan menguraikan JSON ini
	 * sendiri</b>. Seluruh penafsiran harus lewat {@code JurnalAksesKatalog}, yang memegang
	 * definisi kunci dan nilai bawaan yang aman. Menguraikannya langsung berisiko melempar
	 * exception untuk peran yang belum dikonfigurasi ({@code null} adalah keadaan normal) dan
	 * membekukan asumsi tentang bentuk JSON yang dapat berubah.</p>
	 *
	 * <p>Berbeda dari {@link #getEbisnisMenu()}, kolom ini <b>ikut diaudit</b> Envers.</p>
	 *
	 * <p><b>Getter murni</b> &mdash; memangkas spasi tanpa menulis balik ke field.</p>
	 *
	 * @return JSON hak akses jurnal, atau {@code null} bila belum dikonfigurasi
	 * @see #setJurnalAksesJson(String)
	 * @see #getEbisnisMenu()
	 */
	@Column(name = "jurnal_akses_json", columnDefinition = "text")
	public String getJurnalAksesJson() {
		return jurnalAksesJson == null || jurnalAksesJson.trim().isEmpty() ? null : jurnalAksesJson.trim();
	}

	/**
	 * Menetapkan hak akses modul Jurnal Ilmiah sebagai JSON mentah.
	 *
	 * <p>Setter mentah tanpa penjagaan dan <b>tanpa validasi JSON</b>. Bangun isinya lewat
	 * {@code JurnalAksesKatalog}, jangan merangkai teks sendiri &mdash; seluruh catatan pada
	 * {@link #setEbisnisMenu(String)} berlaku sama.</p>
	 *
	 * @param jurnalAksesJson JSON hak akses jurnal; disimpan apa adanya, tanpa validasi
	 * @see #getJurnalAksesJson()
	 */
	public void setJurnalAksesJson(String jurnalAksesJson) {
		this.jurnalAksesJson = jurnalAksesJson;
	}

	/**
	 * Izin melihat SELURUH toko yang aktif.
	 *
	 * <p>Berbeda dgn {@link #getTokoAksesJson()} yang menyebut daftar toko satu
	 * per satu, flag ini berarti "semua toko aktif, termasuk toko yang dibuat
	 * setelah grup ini disimpan". Dipakai oleh peran pengawas/manajemen yang
	 * perlu melihat transaksi lintas toko di SEMUA menu, bukan hanya
	 * dashboard.</p>
	 *
	 * <p>Default {@code false} -- sengaja: memberi akses lintas toko harus
	 * keputusan sadar, bukan sesuatu yang menyala sendiri saat kolom baru
	 * ditambahkan ke basis data.</p>
	 *
	 * <p><b>Getter murni</b> berbentuk ternary tunggal &mdash; tanpa daftar-tolak, tanpa
	 * penurunan berbasis nama, dan tanpa tulis-balik ke field. Bersama
	 * {@link #getBolehVerifikasiMemberMelebihiLimit()} dan
	 * {@link #getMengajukanPengajuanPegawaiLain()}, ini contoh pola yang benar di kelas
	 * ini.</p>
	 *
	 * <p>Ditegakkan sungguhan di sisi server oleh {@code PosApi}. Perhatikan bahwa
	 * menyalakannya membuat {@link #getTokoAksesJson()} menjadi tidak relevan &mdash; daftar
	 * eksplisit di sana tidak lagi membatasi apa pun.</p>
	 *
	 * @return {@code true} bila peran ini boleh melihat seluruh toko aktif; tidak pernah
	 *         {@code null}
	 * @see #setBolehLihatSemuaToko(Boolean)
	 * @see #getTokoAksesJson()
	 */
	@Column(name = "boleh_lihat_semua_toko")
	public Boolean getBolehLihatSemuaToko() {
		return bolehLihatSemuaToko == null ? Boolean.FALSE : bolehLihatSemuaToko;
	}

	/**
	 * Menetapkan izin melihat seluruh toko aktif.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan selalu dihormati. Menyimpan
	 * {@code null} aman karena bawaannya {@link Boolean#FALSE} tanpa syarat.</p>
	 *
	 * <p><b>Menyalakan flag ini adalah keputusan berdampak luas</b>: ia membuka data lintas
	 * toko di SEMUA menu &mdash; bukan hanya dashboard &mdash; dan otomatis mencakup toko
	 * yang dibuka di kemudian hari. Untuk akses yang terbatas dan dapat ditinjau, pakai
	 * {@link #setTokoAksesJson(String)}.</p>
	 *
	 * @param bolehLihatSemuaToko izin lintas toko; {@code null} berarti kembali ke bawaan
	 *                            mati
	 * @see #getBolehLihatSemuaToko()
	 */
	public void setBolehLihatSemuaToko(Boolean bolehLihatSemuaToko) {
		this.bolehLihatSemuaToko = bolehLihatSemuaToko;
	}


	@Column(name = "repository")
	/**
	 * Bawaan MENYALA untuk semua grup: repository berisi artefak yang memang
	 * sudah terpublikasi. Grup lama yang kolomnya masih null ikut menyala
	 * tanpa perlu pembaruan data massal.
	 *
	 * <p>Salah satu dari sedikit flag yang <b>berdefault menyala tanpa syarat</b> &mdash;
	 * tidak ada daftar-tolak {@link #KANTIN} maupun pengecualian peran apa pun. Alasannya
	 * dinyatakan di atas: artefak yang ada di repository memang sudah dipublikasikan,
	 * sehingga membacanya terbuka kecuali sengaja dimatikan.</p>
	 *
	 * <p>Perlu dibedakan dengan tegas dari {@link #getDasborRepository()}, yang merupakan
	 * izin <b>MENGELOLA</b> dan berdefault hanya untuk {@link #ADMINISTRATOR}. Pemisahan
	 * baca/kelola ini sengaja dibuat; jangan menyatukan keduanya.</p>
	 *
	 * <p><b>Getter murni</b> berbentuk ternary tunggal. Ditegakkan sungguhan di sisi server
	 * oleh {@code RepositoryWorkflowService} dan {@code RepositoryPublikApi}.</p>
	 *
	 * @return {@code true} bila peran ini boleh membaca artefak repository; tidak pernah
	 *         {@code null}
	 * @see #setBacaRepository(Boolean)
	 * @see #getDasborRepository()
	 */
	public Boolean getBacaRepository() {
		return bacaRepository == null ? Boolean.TRUE : bacaRepository;
	}

	/**
	 * Menetapkan izin membaca artefak repository.
	 *
	 * <p>Setter mentah tanpa penjagaan. Menyimpan {@code null} <b>tidak</b> mencabut izin
	 * &mdash; ia mengembalikan flag ke bawaan yang bernilai {@link Boolean#TRUE}. Untuk
	 * menutup akses baca, simpan {@link Boolean#FALSE} secara eksplisit.</p>
	 *
	 * @param bacaRepository izin membaca; {@code null} berarti kembali ke bawaan menyala
	 * @see #getBacaRepository()
	 */
	public void setBacaRepository(Boolean bacaRepository) {
		this.bacaRepository = bacaRepository;
	}

	/**
	 * Menentukan apakah peran ini boleh <b>MENGELOLA</b> repository (dasbor pengelolaan
	 * artefak).
	 *
	 * <p>Izin tingkat pengelola, bukan pembaca &mdash; pasangan {@link #getBacaRepository()}
	 * yang mengatur sisi baca. Bawaannya <i>fail-closed</i>: menyala hanya untuk
	 * {@link #ADMINISTRATOR}, mati untuk seluruh peran lain.</p>
	 *
	 * <p>Nilai bawaan dihitung lewat {@link #isRole(String)}, sehingga perbandingannya
	 * <b>tidak peka huruf besar-kecil</b> &mdash; berbeda dari mayoritas getter di kelas ini
	 * yang memakai {@code equals}. Pola {@code Boolean.valueOf(isRole(ADMINISTRATOR))} yang
	 * sama dipakai juga oleh {@link #getDasboardAntarJemput()},
	 * {@link #getTampilkanSpmi()}, {@link #getTampilkanGaji()}, dan
	 * {@link #getMelihatSemuaSop()} &mdash; kelompok getter yang ditulis belakangan dan
	 * konsisten satu sama lain.</p>
	 *
	 * <p><b>Getter murni</b> berbentuk ternary tunggal. Ditegakkan sungguhan di sisi server
	 * oleh {@code RepositoryWorkflowService}.</p>
	 *
	 * @return {@code true} bila peran ini boleh mengelola repository; tidak pernah
	 *         {@code null}
	 * @see #setDasborRepository(Boolean)
	 * @see #getBacaRepository()
	 */
	public Boolean getDasborRepository() {
		return dasborRepository == null ? Boolean.valueOf(isRole(ADMINISTRATOR)) : dasborRepository;
	}

	/**
	 * Menetapkan izin mengelola repository.
	 *
	 * <p>Setter mentah tanpa penjagaan. Menyimpan {@code null} aman karena bawaannya
	 * <i>fail-closed</i> untuk seluruh peran selain {@link #ADMINISTRATOR}.</p>
	 *
	 * @param dasborRepository izin mengelola; {@code null} berarti kembali ke bawaan
	 *                         fail-closed
	 * @see #getDasborRepository()
	 */
	public void setDasborRepository(Boolean dasborRepository) {
		this.dasborRepository = dasborRepository;
	}

	/**
	 * Menentukan apakah peran ini boleh melihat dasbor <b>Antar Jemput</b> (layanan
	 * antar-jemput peserta didik) &mdash; kolom {@code antar_jemput}.
	 *
	 * <p>Bawaan <i>fail-closed</i> yang menyala hanya untuk {@link #ADMINISTRATOR}, memakai
	 * pola {@code Boolean.valueOf(isRole(ADMINISTRATOR))} yang tidak peka huruf besar-kecil
	 * &mdash; sama dengan {@link #getDasborRepository()}, {@link #getTampilkanSpmi()}, dan
	 * {@link #getTampilkanGaji()}.</p>
	 *
	 * <p>Perhatikan <b>salah eja historis</b> pada nama method: {@code Dasboard}, bukan
	 * {@code Dashboard}. Nama itu sudah terlanjur dipakai pemanggil dan pemetaan komponen ZK,
	 * sehingga jangan diperbaiki tanpa menyisir seluruhnya. Nama kolomnya sendiri
	 * ({@code antar_jemput}) tidak terpengaruh.</p>
	 *
	 * <p><b>Getter murni</b> berbentuk ternary tunggal. Dipakai sebagai gerbang render
	 * halaman di {@code modul/antarjemput/index.jsp} &mdash; gerbang per-halaman yang nyata,
	 * namun tidak melindungi endpoint layanan di belakangnya.</p>
	 *
	 * @return {@code true} bila dasbor Antar Jemput boleh ditampilkan; tidak pernah
	 *         {@code null}
	 * @see #setDasboardAntarJemput(Boolean)
	 */
	@Column(name = "antar_jemput")
	public Boolean getDasboardAntarJemput() {
		return dasboardAntarJemput == null ? Boolean.valueOf(isRole(ADMINISTRATOR)) : dasboardAntarJemput;
	}

	/**
	 * Menetapkan izin melihat dasbor Antar Jemput.
	 *
	 * <p>Setter mentah tanpa penjagaan. Menyimpan {@code null} aman karena bawaannya
	 * <i>fail-closed</i>. Perhatikan salah eja historis {@code Dasboard} pada nama method
	 * &mdash; lihat {@link #getDasboardAntarJemput()}.</p>
	 *
	 * @param dasboardAntarJemput izin melihat dasbor; {@code null} berarti kembali ke bawaan
	 *                            fail-closed
	 * @see #getDasboardAntarJemput()
	 */
	public void setDasboardAntarJemput(Boolean dasboardAntarJemput) {
		this.dasboardAntarJemput = dasboardAntarJemput;
	}

	/**
	 * Menentukan apakah peran ini boleh melihat modul <b>SPMI</b> (Sistem Penjaminan Mutu
	 * Internal) &mdash; kolom {@code spmi}.
	 *
	 * <p>Bawaan <i>fail-closed</i> yang menyala hanya untuk {@link #ADMINISTRATOR}, memakai
	 * pola {@link #isRole(String)} yang tidak peka huruf besar-kecil.</p>
	 *
	 * <p>Perhatikan bahwa SPMI adalah fungsi penjaminan mutu, yang secara kelembagaan
	 * berbeda dari fungsi audit internal {@link #SPI}. Keduanya sengaja dipisah: {@link #SPI}
	 * adalah <i>peran</i> tersendiri, sedangkan SPMI adalah <i>flag modul</i> yang dapat
	 * dilekatkan pada peran mana pun.</p>
	 *
	 * <p><b>Getter murni</b> berbentuk ternary tunggal. Dipakai sebagai gerbang render
	 * halaman di {@code modul/spmi/index.jsp} dan oleh {@code MenuHelper} &mdash; gerbang
	 * per-halaman yang nyata, namun tidak melindungi endpoint layanan di belakangnya.</p>
	 *
	 * @return {@code true} bila modul SPMI boleh ditampilkan; tidak pernah {@code null}
	 * @see #setTampilkanSpmi(Boolean)
	 * @see #SPI
	 */
	@Column(name = "spmi")
	public Boolean getTampilkanSpmi() {
		return tampilkanSpmi == null ? Boolean.valueOf(isRole(ADMINISTRATOR)) : tampilkanSpmi;
	}

	/**
	 * Menetapkan izin melihat modul SPMI.
	 *
	 * <p>Setter mentah tanpa penjagaan. Menyimpan {@code null} aman karena bawaannya
	 * <i>fail-closed</i> untuk seluruh peran selain {@link #ADMINISTRATOR}.</p>
	 *
	 * @param tampilkanSpmi izin melihat modul SPMI; {@code null} berarti kembali ke bawaan
	 *                      fail-closed
	 * @see #getTampilkanSpmi()
	 */
	public void setTampilkanSpmi(Boolean tampilkanSpmi) {
		this.tampilkanSpmi = tampilkanSpmi;
	}

	/**
	 * Menentukan apakah peran ini boleh melihat modul <b>Gaji</b> &mdash; kolom
	 * {@code gaji}.
	 *
	 * <p>Bawaan <i>fail-closed</i> yang menyala hanya untuk {@link #ADMINISTRATOR}, memakai
	 * pola {@link #isRole(String)} yang tidak peka huruf besar-kecil. Sikap fail-closed itu
	 * tepat mengingat data penggajian termasuk informasi pribadi yang paling sensitif di
	 * AIS.</p>
	 *
	 * <p><b>Perhatikan batas cakupannya.</b> Flag ini menentukan apakah <i>modul</i>-nya
	 * terlihat, bukan <i>data siapa</i> yang terlihat di dalamnya. Pembatasan data
	 * penggajian per pegawai dan per unit berasal dari {@link #getMelihatDataPegawaiLain()}
	 * dan {@link #getMelihatDataSatkerLain()}, yang memang dibaca seluruh laporan penggajian.
	 * Menyalakan flag ini tanpa membatasi kedua flag tersebut berarti membuka data gaji
	 * seluruh institusi.</p>
	 *
	 * <p><b>Getter murni</b> berbentuk ternary tunggal. Dipakai sebagai gerbang render
	 * halaman di {@code modul/gaji/index.jsp} &mdash; gerbang per-halaman yang nyata, namun
	 * tidak melindungi endpoint layanan di belakangnya.</p>
	 *
	 * @return {@code true} bila modul Gaji boleh ditampilkan; tidak pernah {@code null}
	 * @see #setTampilkanGaji(Boolean)
	 * @see #getMelihatDataPegawaiLain()
	 * @see #getMelihatDataSatkerLain()
	 */
	@Column(name = "gaji")
	public Boolean getTampilkanGaji() {
		return tampilkanGaji == null ? Boolean.valueOf(isRole(ADMINISTRATOR)) : tampilkanGaji;
	}

	/**
	 * Menetapkan izin melihat modul Gaji.
	 *
	 * <p>Setter mentah tanpa penjagaan. Menyimpan {@code null} aman karena bawaannya
	 * <i>fail-closed</i>. Ingat bahwa menyalakannya perlu disertai pembatasan
	 * {@link #setMelihatDataPegawaiLain(Boolean)} dan
	 * {@link #setMelihatDataSatkerLain(Boolean)} agar cakupan datanya ikut terbatas.</p>
	 *
	 * @param tampilkanGaji izin melihat modul Gaji; {@code null} berarti kembali ke bawaan
	 *                      fail-closed
	 * @see #getTampilkanGaji()
	 */
	public void setTampilkanGaji(Boolean tampilkanGaji) {
		this.tampilkanGaji = tampilkanGaji;
	}

	/**
	 * Memeriksa apakah pengenal peran ini sama dengan {@code role}, <b>tanpa memedulikan
	 * huruf besar-kecil</b>.
	 *
	 * <p>Pembantu internal yang menggantikan pola berulang
	 * {@code getRoleId() != null && getRoleId().equals(...)} yang tersebar di sepanjang kelas
	 * ini. Aman terhadap {@code null} di kedua sisi: mengembalikan {@code false} bila
	 * {@link #getRoleId()} atau argumennya {@code null}.</p>
	 *
	 * <h3>PENTING: tidak setara dengan pemeriksaan harfiah di getter lama</h3>
	 * <p>Perbedaan yang halus namun berkonsekuensi: method ini memakai
	 * {@code equalsIgnoreCase}, sedangkan pemeriksaan yang ditulis harfiah di getter-getter
	 * lama memakai {@code equals} yang <b>peka huruf besar-kecil</b>. Akibatnya baris peran
	 * yang tersimpan dengan huruf berbeda dari konstantanya &mdash; misalnya {@code "kantin"}
	 * alih-alih {@code "Kantin"}, atau {@code "dosen"} alih-alih {@code "Dosen"} &mdash;
	 * akan <b>diperlakukan berbeda</b> oleh kedua kelompok getter itu:</p>
	 * <ul>
	 *   <li>getter yang memakai method ini ({@link #getMelihatSemuaSop()},
	 *   {@link #getAksesGerbangPesantren()}, dan kelompok
	 *   {@code Boolean.valueOf(isRole(ADMINISTRATOR))}) <b>mengenalinya</b>;</li>
	 *   <li>getter yang menulis perbandingannya harfiah ({@link #getMelihatDataPegawaiLain()},
	 *   {@link #getKeuangan()}, dan sebagian besar lainnya) <b>tidak</b>.</li>
	 * </ul>
	 * <p>Method ini diperkenalkan belakangan dan merupakan bentuk yang lebih benar; getter
	 * lama belum dimigrasikan. Bila menyunting getter lama, mengganti pemeriksaan harfiahnya
	 * dengan method ini adalah perbaikan &mdash; namun perlu disadari bahwa itu
	 * <b>mengubah perilaku</b> untuk baris peran yang huruf besar-kecilnya menyimpang, jadi
	 * periksa data yang ada lebih dulu.</p>
	 *
	 * @param role pengenal peran pembanding; {@code null} menghasilkan {@code false}
	 * @return {@code true} bila pengenal peran ini sama dengan {@code role} tanpa memedulikan
	 *         huruf besar-kecil
	 * @see #getRoleId()
	 * @see #roleEndUser()
	 */
	private boolean isRole(String role) {
		return getRoleId() != null && role != null && getRoleId().equalsIgnoreCase(role);
	}

	/**
	 * Memeriksa apakah peran ini termasuk kelompok <b>"pengguna akhir"</b> &mdash;
	 * {@link #DOSEN}, {@link #PEGAWAI}, {@link #GURU}, {@link #SISWA}, atau
	 * {@link #MAHASISWA}.
	 *
	 * <p>Kelompok ini mewakili orang yang <i>memakai</i> sistem untuk urusannya sendiri,
	 * sebagai lawan dari peran pengelola yang mengurus data orang lain. Karena itu ia dipakai
	 * untuk menolak hak-hak berlingkup luas.</p>
	 *
	 * <p>Dibangun di atas {@link #isRole(String)}, sehingga perbandingannya <b>tidak peka
	 * huruf besar-kecil</b> &mdash; lihat peringatan pada method tersebut mengenai
	 * ketidaksetaraan dengan pemeriksaan harfiah di getter lama.</p>
	 *
	 * <h3>Hanya dipakai satu tempat</h3>
	 * <p>Meski merangkum pola yang berulang, method ini <b>hanya dipanggil oleh
	 * {@link #getMelihatSemuaSop()}</b>. Getter lain yang menolak kelompok yang sama
	 * &mdash; {@link #getMelihatDataPegawaiLain()}, {@link #getMelihatDataSatkerLain()}, dan
	 * {@link #getMelihatSemuaSurat()} &mdash; masih menuliskan kelima pemeriksaannya secara
	 * harfiah dengan {@code equals}. Ketiganya adalah kandidat migrasi yang jelas, dengan
	 * catatan perubahan perilaku pada baris peran yang huruf besar-kecilnya menyimpang.</p>
	 *
	 * <p><b>Jangan menganggapnya daftar tolak universal.</b> Beberapa getter sengaja hanya
	 * menolak sebagian anggotanya: {@link #getKinerja()} menolak dua dari lima (dosen, guru,
	 * dan pegawai justru subjek penilaian kinerja), sedangkan
	 * {@link #getPresensiKehadiran()} bahkan <b>memaksa menyala</b> untuk tiga di antaranya.
	 * Keanggotaan di sini tidak berarti "selalu ditolak".</p>
	 *
	 * @return {@code true} bila peran ini salah satu dari kelima peran pengguna akhir
	 * @see #isRole(String)
	 * @see #getMelihatSemuaSop()
	 */
	private boolean roleEndUser() {
		return isRole(DOSEN) || isRole(PEGAWAI) || isRole(GURU) || isRole(SISWA) || isRole(MAHASISWA);
	}

	/**
	 * Menentukan apakah peran ini boleh melihat <b>seluruh SOP</b>, bukan hanya SOP yang
	 * berkaitan dengannya.
	 *
	 * <p>Gerbang pembatas data yang <b>benar-benar ditegakkan di sisi server</b>: entity
	 * {@code AktorSop} memakainya untuk mempersempit SOP yang tampil ke aktor yang
	 * bersangkutan. Setara peranannya dengan {@link #getMelihatSemuaSurat()} di modul
	 * persuratan.</p>
	 *
	 * <p>Struktur keputusannya:</p>
	 * <ol>
	 *   <li>{@link #KANTIN} atau salah satu anggota {@link #roleEndUser()} &mdash;
	 *   {@code false} mutlak, tanpa membaca kolom;</li>
	 *   <li>bawaan <i>fail-closed</i> yang menyala hanya untuk {@link #ADMINISTRATOR}.</li>
	 * </ol>
	 *
	 * <h3>Getter paling "modern" di kelas ini</h3>
	 * <p>Ini satu-satunya getter yang memakai <b>kedua</b> pembantu internal
	 * ({@link #isRole(String)} dan {@link #roleEndUser()}) alih-alih menuliskan
	 * perbandingannya secara harfiah. Bentuknya jauh lebih ringkas daripada
	 * {@link #getMelihatDataPegawaiLain()} dan {@link #getMelihatSemuaSurat()} yang secara
	 * perilaku hampir sama, dan merupakan bentuk yang sebaiknya diikuti.</p>
	 * <p>Konsekuensi yang perlu diketahui: karena kedua pembantu itu memakai
	 * {@code equalsIgnoreCase}, penolakan di sini <b>lebih ketat</b> daripada di ketiga
	 * getter sejenis yang memakai {@code equals}. Baris peran yang tersimpan sebagai
	 * {@code "dosen"} huruf kecil akan tertolak di sini namun lolos di sana &mdash;
	 * ketidakkonsistenan nyata antar-getter yang seharusnya setara.</p>
	 *
	 * <p><b>Getter murni</b> tanpa tulis-balik ke field.</p>
	 *
	 * @return {@code true} bila peran ini boleh melihat seluruh SOP; tidak pernah
	 *         {@code null}
	 * @see #setMelihatSemuaSop(Boolean)
	 * @see #getMelihatSemuaSurat()
	 * @see #roleEndUser()
	 */
	public Boolean getMelihatSemuaSop() {
		if (isRole(KANTIN) || roleEndUser()) {
			return false;
		}
		return melihatSemuaSop == null ? Boolean.valueOf(isRole(ADMINISTRATOR)) : melihatSemuaSop;
	}

	/**
	 * Menetapkan hak melihat seluruh SOP.
	 *
	 * <p>Setter mentah tanpa penjagaan. Nilai yang disimpan <b>tidak berlaku</b> bagi
	 * {@link #KANTIN} maupun kelima peran {@link #roleEndUser()} &mdash; lihat
	 * {@link #getMelihatSemuaSop()}. Menyimpan {@code null} aman karena bawaannya
	 * <i>fail-closed</i> untuk seluruh peran selain {@link #ADMINISTRATOR}.</p>
	 *
	 * @param melihatSemuaSop hak melihat semua SOP; {@code null} berarti kembali ke bawaan
	 *                        fail-closed
	 * @see #getMelihatSemuaSop()
	 */
	public void setMelihatSemuaSop(Boolean melihatSemuaSop) {
		this.melihatSemuaSop = melihatSemuaSop;
	}
}
