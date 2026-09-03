package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;

/**
 * <b>Surat tugas mengajar</b> seorang guru untuk satu periode akademik — tabel
 * {@code sekolah.penugasan_guru_mengajar}.
 *
 * <h2>Peran sebenarnya: kepala surat tugas, bukan daftar jadwal</h2>
 * <p>Nama kelas ini mudah tertukar dengan {@link GuruMengajar} maupun {@link JadwalPelajaran},
 * padahal isinya berbeda jauh. {@code GuruMengajar} dan {@code JadwalPelajaran} menyimpan
 * <i>apa</i> yang diajarkan (mata pelajaran, kelas, ruang, jam pelajaran, sampai 12–25 slot
 * guru per baris). Entity ini <b>tidak memuat satu pun kolom mata pelajaran, kelas, jam, atau
 * ruang</b>. Yang disimpannya hanya administrasi kepegawaian dari satu lembar SK:</p>
 * <ul>
 *   <li>siapa gurunya ({@link #getGuru() guru}, wajib),</li>
 *   <li>untuk periode mana ({@link #getTahunAkademik() tahunAkademik} +
 *   {@link #getSemester() semester}, ditambah {@link #getProgram() program} sebagai pembeda
 *   jalur/kelompok penyelenggaraan),</li>
 *   <li>nomor SK-nya ({@link #getKode() kode} — label layarnya persis "No. SK"),</li>
 *   <li>tanggal SK ({@link #getTanggalSuratTugas() tanggalSuratTugas}) dan tanggal mulai
 *   berlakunya ({@link #getTmtSuratTugas() tmtSuratTugas}; "TMT" = terhitung mulai tanggal),</li>
 *   <li>{@link #getKeterangan() keterangan} bebas.</li>
 * </ul>
 * <p>Dengan kata lain: satu baris = <b>satu SK mengajar per guru per sekolah per program per
 * tahun akademik per semester</b>. Rincian beban mengajarnya sendiri tetap dihitung dari
 * {@code JadwalPelajaran} pada saat layar/laporan dirender, bukan disimpan di sini.</p>
 *
 * <p>Berkas pindaian SK-nya juga tidak disimpan di tabel ini, melainkan sebagai baris
 * {@link ais.database.model.file.LampiranLain} dengan jenis {@code "sk_penugasan_pengajaran_guru"}
 * dan {@code ref} = {@link #getId() id} baris ini (lihat pemasangan tombol unggah/unduh di
 * {@code ais.action.master.sekolah.GuruAction}). Template cetaknya dipilih lewat
 * {@link JenisSKGuru}.</p>
 *
 * <h2>Tiga titik masuk UI (hasil verifikasi kode)</h2>
 * <ol>
 *   <li><b>Layar master</b> — {@code ais.action.master.sekolah.PenugasanGuruMengajarAction}
 *   atas {@code webapp/WEB-INF/z/x/y/pages/master/sekolah/penugasan_guru_mengajar.zul}. Grid
 *   daftar + form popup "Tambah/Ubah Penugasan Guru" berisi Guru, No. SK, Tanggal SK, TMT SK,
 *   Tahun Ajaran, Semester, Keterangan.</li>
 *   <li><b>Tab per-guru</b> — {@code GuruAction} merender satu {@code MyDetail} per pasangan
 *   tahun ajaran/semester dengan caption "Penugasan Guru Mengajar", memuat kolom Jml Matpel /
 *   Jml Jdw / Jml JP (dihitung dari {@code JadwalPelajaran}) berdampingan dengan No./Tgl./TMT
 *   Surat Tugas yang bisa disunting langsung, ditambah kolom "Upload SK".</li>
 *   <li><b>Laporan</b> — {@code ais.action.report.format1.sekolah.LaporanSKGuru} (tab
 *   <i>SK Mengajar</i>), lihat bagian berikutnya.</li>
 * </ol>
 * <p>Selain itu {@code ais.action.master.dashboard.admin.DasboardGuru} hanya <i>membaca</i>
 * entity ini untuk kartu "total penugasan", grafik tren, dan tabel rincian per guru.</p>
 *
 * <h2>Relasi dengan cetak SK Guru mode "borongan" — TERVERIFIKASI</h2>
 * <p>Dugaan yang tercatat pada Javadoc {@link JenisSKGuru} <b>benar</b>.
 * {@code LaporanSKGuru.generateParameter()} bercabang tepat pada
 * {@code if (sk != null && sk.getGlondongan())}:</p>
 * <ul>
 *   <li><b>{@code glondongan = true} (borongan)</b> — filter Guru yang dipilih pengguna
 *   <i>dibuang</i> ({@code guru = null}), lalu sumber datanya adalah
 *   {@code session.createCriteria(PenugasanGuruMengajar.class)} yang disaring
 *   {@code tahunAkademik}, {@code semester}, {@code sekolah}, {@code yayasan} dan diurutkan
 *   {@code guru.namaGuru}. Setiap baris entity ini menjadi <b>satu record cetak</b>; nilai
 *   {@link #getKode() kode}/{@link #getTanggalSuratTugas() tanggalSuratTugas}/
 *   {@link #getTmtSuratTugas() tmtSuratTugas}/{@link #getKeterangan() keterangan} dipetakan ke
 *   parameter JasperReports {@code sk_mengajar}, {@code tanggal_mengajar}, {@code tmt_mengajar}
 *   (plus varian format {@code _1}/{@code _2}/{@code _3}) dan {@code sk_mengajar_keterangan},
 *   sedangkan {@link #getTahunAkademik()}/{@link #getSemester()} mengisi {@code ta}/{@code smt}.</li>
 *   <li><b>{@code glondongan = false} (per-guru)</b> — sumber datanya {@code JadwalPelajaran}
 *   (dengan 10 slot guru yang di-{@code OR}-kan), dan entity ini tetap dipakai, hanya saja
 *   diambil satu per satu lewat {@code Common.getPenugasanGuruMengajar(...)} untuk mengisi
 *   kelompok parameter {@code sk_mengajar*} yang sama.</li>
 * </ul>
 * <p>Jadi entity ini bukan sekadar "sumber data mode borongan": ia adalah <b>satu-satunya</b>
 * pemasok nomor/tanggal SK pada <b>kedua</b> mode; yang dialihkan oleh {@code glondongan}
 * adalah <i>granularitas baris cetak</i> (satu baris per SK vs satu baris per jadwal).</p>
 *
 * <h2>Baris dibuat otomatis — termasuk oleh operasi yang tampak "hanya membaca"</h2>
 * <p>{@code Common.getPenugasanGuruMengajar(idSekolah, program, tahun, jenisSemester, guru)}
 * (delegasi ke {@code ais.common.CommonAcademicSyncHelper}) bersifat <b>get-or-create</b>: bila
 * kombinasi sekolah+program+tahunAkademik+semester+guru belum ada, ia langsung
 * {@code session.save(...)} baris baru — bila perlu membuka transaksi sendiri dan
 * meng-{@code commit}-nya. Tiga pemanggilnya:</p>
 * <ol>
 *   <li>tombol <i>"Generate No. SK Berdasarkan Jadwal"</i> di layar master (memindai seluruh
 *   {@code JadwalPelajaran} pada TA/semester terpilih, lalu memanggil helper untuk setiap guru
 *   di setiap jadwal) — ini memang operasi tulis yang disengaja;</li>
 *   <li>membuka {@code MyDetail} "Penugasan Guru Mengajar" di layar Guru — sekadar
 *   <b>mengklik detail</b> sudah menyisipkan baris SK kosong;</li>
 *   <li>{@code LaporanSKGuru} pada mode per-guru — <b>mencetak laporan menyisipkan baris</b> ke
 *   tabel ini untuk setiap pasangan guru/jadwal yang belum punya SK.</li>
 * </ol>
 * <p>Konsekuensinya: tabel ini bisa tumbuh sebagai efek samping penelusuran atau pencetakan,
 * bukan hanya dari input administrasi. Karena entity dianotasi {@code @Audited}, setiap baris
 * bayangan itu juga menghasilkan revisi Envers.</p>
 *
 * <h2>Struktur anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; jejak audit</b> — {@link #getId() id}, {@link #getOleh() oleh},
 *   {@link #getOlehId() olehId}, {@link #getTanggal_dirubah() tanggal_dirubah},
 *   {@link #onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Identitas periode</b> — {@link #getTahunAkademik() tahunAkademik},
 *   {@link #getSemester() semester}, {@link #getProgram() program}, {@link #getTahun() tahun}
 *   (turunan), {@link #getNama() nama} (turunan).</li>
 *   <li><b>Isi SK</b> — {@link #getKode() kode}, {@link #getTanggalSuratTugas() tanggalSuratTugas},
 *   {@link #getTmtSuratTugas() tmtSuratTugas}, {@link #getKeterangan() keterangan}.</li>
 *   <li><b>Relasi</b> — {@link #getGuru() guru} (wajib, kolom {@code guru}),
 *   {@link #getSekolah() sekolah}, {@link #getYayasan() yayasan} (dua terakhir diturunkan
 *   dari guru).</li>
 *   <li><b>Utilitas</b> — konstruktor {@link #PenugasanGuruMengajar()}. Tidak ada method bisnis,
 *   query statis, maupun {@code populate*}/{@code sinkronkan*} di kelas ini: seluruh logika
 *   sinkronisasi berada di {@code CommonAcademicSyncHelper} dan Action pemanggil.</li>
 * </ul>
 *
 * <h2>Kenapa {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} dideklarasikan ulang</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu pun properti miliknya.
 * Pengulangan deklarasi field dan getter/setter di sini <b>bukan duplikasi keliru</b>, melainkan
 * keharusan teknis agar kolom-kolom tersebut benar-benar terpetakan ke tabel. Hal yang sama
 * berlaku untuk {@code nama} dan {@code keterangan} yang <i>membayangi</i> (shadow) field senama
 * di kelas induk.</p>
 *
 * <h2>Pemetaan kolom</h2>
 * <p>Pemetaan memakai <b>property access</b> (anotasi ditaruh di getter), dan strategi penamaan
 * {@code ais.database.hibernate.MyNamingStrategy} adalah turunan {@code DefaultNamingStrategy}
 * — nama kolom = nama properti <b>apa adanya</b>, tanpa konversi ke {@code snake_case}. Jadi
 * kolomnya benar-benar bernama {@code tahunAkademik}, {@code tanggalSuratTugas},
 * {@code tmtSuratTugas}, dan {@code olehId}. Dua akibat penting dari property access:</p>
 * <ol>
 *   <li>nilai yang tersimpan ke database adalah <b>apa yang dikembalikan getter</b>, bukan isi
 *   field. Getter yang menghitung nilai (lihat di bawah) karena itu ikut menentukan isi kolom;</li>
 *   <li>{@code @JoinColumn(name = "guru")} membuat kolom FK guru bernama {@code guru} saja —
 *   menyimpang dari konvensi {@code sekolah_id}/{@code yayasan_id} pada relasi lain di kelas ini.</li>
 * </ol>
 *
 * <h2>Hal non-obvious yang wajib diketahui sebelum menyunting</h2>
 * <ol>
 *   <li><b>Empat getter bersifat destruktif (menulis balik ke field).</b> {@link #getNama()},
 *   {@link #getSekolah()}, {@link #getYayasan()}, dan {@link #getTahun()} semuanya menghitung
 *   nilai lalu <i>menimpa</i> field yang bersangkutan. Digabung dengan property access, artinya
 *   nilai hasil hitungan itulah yang tertulis ke database pada flush berikutnya —
 *   {@link #setNama(String)}, {@link #setSekolah(Sekolah)}, {@link #setYayasan(Yayasan)}, dan
 *   {@link #setTahun(Integer)} praktis tidak bisa mempertahankan nilai yang menyimpang dari
 *   hitungan tersebut.</li>
 *   <li><b>{@code sekolah} adalah cerminan sekolah guru, bukan kolom mandiri.</b> Lihat
 *   {@link #getSekolah()}: bila guru punya sekolah, {@code sekolah} selalu ditimpa dengan
 *   sekolah guru. Ini berinteraksi buruk dengan kunci pencarian
 *   {@code CommonAcademicSyncHelper} yang mencari berdasarkan {@code sekolah.id} — lihat
 *   catatan pada method tersebut.</li>
 *   <li><b>{@code program} tidak punya kendali di form mana pun.</b> Baris yang dibuat lewat
 *   layar master selalu tersimpan dengan {@code program} {@code NULL}, sedangkan baris yang
 *   dibuat otomatis mewarisi {@code program} dari {@code JadwalPelajaran}. Karena helper
 *   pencari menambahkan {@code Restrictions.eq("program", program)} ketika program tidak null,
 *   baris manual (program {@code NULL}) <b>tidak pernah cocok</b> untuk jadwal ber-program —
 *   sistem akan membuat baris kedua. Tidak ada indeks unik yang mencegahnya, sehingga satu guru
 *   bisa punya beberapa baris SK untuk periode yang sama, dan mode borongan akan mencetak
 *   semuanya.</li>
 *   <li><b>{@link #getNama()} bukan nama yang bisa diisi pengguna.</b> Nilainya selalu
 *   dihitung ulang menjadi {@code "<tahunAkademik>-<semester>"}. Grid layar master menampilkan
 *   kolom "TA/Semester" dari sini, dan tautan riwayat revisi memakainya sebagai label.</li>
 *   <li><b>Semua baris pada satu periode "sama besar" bagi {@code compareTo}.</b>
 *   {@link GeneralValueObject#compareTo(GeneralValueObject)} mencoba {@code nomorUrut} (tidak
 *   pernah diisi di sini), lalu {@code nim} (idem), lalu {@code nama} — yang di kelas ini selalu
 *   non-null tetapi <b>identik</b> untuk semua guru pada TA/semester yang sama. Sebuah
 *   {@code TreeSet}/{@code TreeMap} berisi entity ini karenanya akan menciut menjadi satu
 *   elemen per periode. <b>Verifikasi: tidak ada koleksi terurut semacam itu di jalur kode saat
 *   ini</b> (semua pemanggil memakai {@code List} dari {@code ConstantValues.simpleList} atau
 *   {@code Criteria.list()}), jadi ini risiko laten, bukan bug aktif — tapi jangan pernah
 *   memasukkan entity ini ke koleksi berbasis {@code compareTo}.</li>
 *   <li><b>{@link #getKode()} tidak pernah mengembalikan {@code null}.</b> Karena property
 *   access, kolom {@code kode} pada akhirnya berisi string kosong, bukan {@code NULL}. Query
 *   yang mencari SK yang belum diisi harus menguji {@code kode = ''}, bukan {@code IS NULL}.</li>
 *   <li><b>{@link #getTahunAkademik()} dan {@link #getSemester()} punya nilai cadangan
 *   dinamis</b> (tahun akademik berjalan dan semester berjalan). Baris yang belum pernah diisi
 *   akan "ikut bergerak" mengikuti kalender akademik sampai flush pertama membekukannya.</li>
 * </ol>
 *
 * <h2>Hak akses &amp; cakupan tenant (hasil verifikasi kode)</h2>
 * <ul>
 *   <li><b>Gerbang login ADA.</b> {@code PenugasanGuruMengajarAction.doBeforeCompose()}
 *   memanggil {@code Common.doCheckSecurity()}; tombol Tambah dikawal
 *   {@code CommonPrivilages.checkPrevilages(CREATE)}, tombol Ubah/Hapus per baris dikawal
 *   {@code UPDATE}/{@code DELETE}, dan tombol impor Excel dikawal keduanya sekaligus.</li>
 *   <li><b>Namun hak itu diwarisi dari menu induk.</b> {@code penugasan_guru_mengajar.zul}
 *   <b>tidak pernah</b> dirujuk sebagai target menu; satu-satunya rujukannya di seluruh aplikasi
 *   adalah {@code <include>} pada sebuah tab di
 *   {@code webapp/WEB-INF/z/x/y/pages/master/sekolah/guru.zul}. Karena
 *   {@code checkPrevilages(kode)} me-resolve privilese terhadap {@code Common.getCurrentMenu()},
 *   menu aktifnya selalu <i>Manajemen Guru</i> — siapa pun yang diberi CREATE/UPDATE/DELETE pada
 *   menu Guru otomatis memperoleh hak penuh atas SK mengajar seluruh guru, dan tidak ada objek
 *   hak akses tersendiri yang bisa dicabut administrator. Ini instance lanjutan dari pola
 *   <i>pewarisan hak lewat menu induk</i> yang sudah tercatat pada banyak layar lain (kembar
 *   persis {@link JenisSKGuru}, yang menumpang di tab lain pada berkas ZUL yang sama).</li>
 *   <li><b>Tombol "Generate No. SK Berdasarkan Jadwal" TIDAK bergerbang sama sekali.</b> Di
 *   {@code doAfterCompose()} tombol impor memang diberi {@code setVisible(edit && delete)},
 *   tetapi tombol generate ini dipasang lewat {@code Common.appendKeToolbar(...)} tanpa satu pun
 *   pemeriksaan {@code checkPrevilages}. Padahal ia memicu penulisan massal: sebuah thread latar
 *   memindai {@code JadwalPelajaran} yang hanya disaring <b>tahun ajaran dan semester</b> — tanpa
 *   pembatasan sekolah maupun yayasan — lalu memanggil get-or-create untuk setiap guru pada
 *   setiap jadwal. Hak <b>BACA</b> saja karena itu cukup untuk menyisipkan baris SK ke seluruh
 *   sekolah dalam satu instalasi sekaligus.</li>
 *   <li><b>Kendali sunting di dalam grid juga tidak bergerbang.</b> Pada
 *   {@code PenugasanGuruMengajarRenderer} (No. SK, Tgl. SK, TMT SK, Keterangan) maupun pada
 *   detail per-guru di {@code GuruAction} (No./Tgl./TMT Surat Tugas), kendali dibuat tanpa
 *   {@code setDisabled(!edit)} dan {@code onChange}-nya langsung memanggil
 *   {@code Common.refreshUpdate(...)}. Bandingkan dengan tombol Ubah/Hapus di baris yang sama
 *   yang justru dikawal benar — pola "kendali inline lolos gerbang" yang sama seperti
 *   {@code Intbox} nomor urut pada beberapa katalog. Akibat praktisnya: nomor dan tanggal SK —
 *   dokumen kepegawaian resmi — dapat ditulis ulang oleh pemegang hak baca.</li>
 *   <li><b>Tombol cetak/ekspor juga tanpa gerbang</b> ({@code Common.cetakData(...)} dipasang
 *   tanpa {@code setVisible}), sementara pasangannya, tombol unggah, digerbangi. Ekspornya
 *   memuat kolom guru, sekolah, program, TA/semester, dan seluruh isi SK.</li>
 *   <li><b>Cakupan tenant fail-open.</b> {@code initCriteria()} menyaring
 *   {@code sekolah}/{@code yayasan} <i>hanya</i> bila combobox pencarian punya item terpilih;
 *   bila tidak, yang ditambahkan adalah {@code Restrictions.sqlRestriction("1=1")}. Combobox itu
 *   diisi {@code Common.initYayasanDanSekolahDanSemua(...)} yang mengunci pilihan ke konteks
 *   sekolah/yayasan aktif <i>bila ada</i>; ketika konteks maupun penugasan pengguna sama-sama
 *   kosong, tidak ada pembatasan apa pun yang tersisa. Pola identik terulang di
 *   {@code LaporanSKGuru}, sehingga cetak SK mode borongan pun dapat melintasi seluruh
 *   instalasi.</li>
 * </ul>
 *
 * <h2>Kuirk lain yang terverifikasi di jalur pemanggil</h2>
 * <ul>
 *   <li>{@code LaporanSKGuru} memakai literal {@code "Semua"} sebagai nilai bawaan tahun
 *   akademik ketika combobox tidak punya item terpilih, tetapi tetap memasangnya sebagai
 *   {@code Restrictions.eq("tahunAkademik", "Semua")} tanpa penanganan khusus (berbeda dari
 *   filter semester yang menangani {@code "Semua"} dengan benar). Dalam keadaan itu laporan SK —
 *   baik borongan maupun per-guru — akan kosong tanpa pesan galat.</li>
 *   <li>Pada mode borongan, variabel sekolah pencetak (dipakai untuk kop: nama kepala sekolah,
 *   NIP, yayasan, jenjang) hanya ditimpa bila {@code guru.getSekolah()} tidak null; bila null,
 *   kop <b>guru sebelumnya pada iterasi</b> yang terbawa ke record berikutnya.</li>
 *   <li>{@code GuruAction} menyusun query native untuk mendaftar sekolah/program milik seorang
 *   guru dengan merangkai tahun ajaran dan nomor semester langsung ke string SQL. Keduanya
 *   berasal dari daftar periode yang dibangkitkan aplikasi (bukan isian bebas pengguna),
 *   sehingga ini risiko laten dan bukan sink injeksi yang bisa dijangkau dari layar itu — namun
 *   pola perangkaiannya sama dengan sink SQLi yang sudah tercatat di tempat lain.</li>
 * </ul>
 *
 * <h2>Catatan jejak generator</h2>
 * <p>Javadoc lama berkas ini berbunyi {@code "Bank generated by hbm2java"} — sisa salin-tempel
 * dari entity {@code Bank} yang tersebar ke ratusan berkas model lain; kelas ini tidak ada
 * hubungannya dengan bank. Jejak serupa masih ada di ZUL: judul bawaan jendela popup berbunyi
 * {@code "Tambah Agama"} (ditimpa saat runtime menjadi "Tambah/Ubah Penugasan Guru", jadi tidak
 * terlihat pengguna).</p>
 *
 * @see ais.action.master.sekolah.PenugasanGuruMengajarAction
 * @see ais.action.master.sekolah.GuruAction
 * @see ais.action.report.format1.sekolah.LaporanSKGuru
 * @see ais.common.CommonAcademicSyncHelper
 * @see JenisSKGuru
 * @see JadwalPelajaran
 * @see Guru
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "penugasan_guru_mengajar")
public class PenugasanGuruMengajar extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya dibangkitkan otomatis oleh perkakas dan sengaja dipertahankan agar instance
	 * yang tersimpan di session ZK atau cache tetap kompatibel setelah kelas disunting. Jangan
	 * diubah. (Nilai yang sama juga muncul di beberapa entity lain hasil generator yang sama —
	 * itu tidak berpengaruh apa pun karena keunikan {@code serialVersionUID} hanya relevan
	 * per kelas.)</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci primer baris, kolom {@code id}. Dibangkitkan basis data ({@code IDENTITY}), berurutan
	 * dan mudah ditebak. Dipakai juga sebagai {@code ref} pada baris
	 * {@link ais.database.model.file.LampiranLain} yang menyimpan pindaian SK
	 * ({@code jenis = "sk_penugasan_pengajaran_guru"}).
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini, kolom {@code oleh}. Diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan oleh layar master.
	 */
	private String oleh;

	/**
	 * Id pengguna terakhir yang mengubah baris ini, kolom {@code olehId} (nama kolom mengikuti
	 * nama properti apa adanya — lihat catatan pemetaan pada Javadoc kelas). Pasangan teknis
	 * dari {@link #oleh}, juga diisi oleh interceptor audit.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila baris belum pernah melewati
	 *         interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyunting terakhir.
	 *
	 * <p><b>Perhatikan:</b> setter ini <b>menolak secara diam-diam</b> nilai {@code null} maupun
	 * string kosong/spasi — nilai lama dipertahankan tanpa pesan apa pun. Jejak audit karena itu
	 * tidak bisa dikosongkan lewat setter ini; perilaku ini disengaja agar penyimpanan ulang
	 * dari layar yang tidak membawa konteks pengguna tidak menghapus jejak sebelumnya.</p>
	 *
	 * @param olehId id pengguna penyunting; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyunting terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau kosong/spasi
	 * <b>diabaikan diam-diam</b> sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna penyunting; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang menyegarkan jejak audit tepat sebelum baris ini
	 * di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan seluruhnya ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String) oleh}/{@link #setOlehId(String) olehId} dari pengguna yang sedang
	 * login dan memutakhirkan {@link #setTanggal_dirubah(Date) tanggal_dirubah}. Dipanggil oleh
	 * penyedia persistensi, <b>bukan</b> oleh kode aplikasi — jangan memanggilnya langsung.</p>
	 *
	 * <p><b>Efek samping:</b> memutasi tiga field audit pada instance ini. Karena entity dianotasi
	 * {@code @Audited} (Hibernate Envers), setiap {@code UPDATE} juga melahirkan satu revisi baru
	 * di tabel riwayat — termasuk update yang dipicu penyuntingan inline di grid, yang
	 * ditampilkan kembali oleh {@code RevisiHelper} di kolom pertama layar master.</p>
	 *
	 * <p><b>Catatan bentuk kode:</b> field {@code tanggal_dirubah} sengaja dibiarkan berbagi baris
	 * fisik dengan deklarasi method ini — bentuk warisan generator yang tidak dirapikan agar diff
	 * berkas ini tetap murni Javadoc. Field tersebut menyimpan waktu perubahan terakhir (kolom
	 * {@code tanggal_dirubah}) dan diberi nilai awal {@code ais.ui.util.WaktuUtil.getDate()}
	 * sehingga baris baru selalu punya stempel waktu meski belum pernah disunting; pemetaan
	 * {@code TIMESTAMP}-nya dideklarasikan pada {@link #getTanggal_dirubah()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini. Tanpa validasi; normalnya hanya dipanggil
	 * {@link #onUpdate()} lewat interceptor audit.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini, dipetakan sebagai kolom
	 * {@code TIMESTAMP}.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang
	 *         dibuat lewat konstruktor karena field-nya diberi nilai awal waktu saat ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas baris ini dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca field {@link #nama} <b>secara langsung</b>, bukan lewat {@link #getNama()}.
	 * Perbedaannya nyata di kelas ini: {@code getNama()} selalu menghitung ulang nilainya menjadi
	 * {@code "<tahunAkademik>-<semester>"}, sedangkan {@code toString()} menampilkan isi field
	 * apa adanya. Untuk instance yang belum pernah dibaca lewat getter dan belum disimpan,
	 * hasilnya {@code "null-null"}.</p>
	 *
	 * <p>Dipakai untuk log/debug dan menjadi label cadangan pada komponen ZK yang tidak menyetel
	 * label secara eksplisit. Tidak dipakai untuk keputusan bisnis apa pun.</p>
	 *
	 * @return {@code id} dan field {@code nama} yang digabung dengan tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Label periode baris ini, kolom {@code nama} ({@code NOT NULL}, maks. 255 karakter).
	 *
	 * <p><b>Bukan isian pengguna.</b> Isinya selalu dihitung ulang oleh {@link #getNama()}
	 * menjadi {@code "<tahunAkademik>-<semester>"}, mis. {@code "2025/2026-Ganjil"}. Membayangi
	 * (shadow) field senama di {@link GeneralValueObject}, yang tidak dipetakan Hibernate.</p>
	 */
	private String nama;

	/**
	 * Catatan bebas pada SK, kolom {@code keterangan} (nullable). Diisi lewat form popup maupun
	 * kotak teks inline di grid layar master, dan dikirim ke laporan sebagai parameter
	 * {@code sk_mengajar_keterangan}. Membayangi field senama di {@link GeneralValueObject}.
	 */
	private String keterangan;

	/**
	 * Sekolah pemilik SK, kolom FK {@code sekolah_id}.
	 *
	 * <p><b>Bukan kolom mandiri:</b> {@link #getSekolah()} selalu menimpanya dengan sekolah milik
	 * {@link #guru} bila guru tersebut punya sekolah. Lihat Javadoc getter untuk konsekuensinya
	 * terhadap pencarian baris.</p>
	 */
	private Sekolah sekolah;

	/**
	 * Nomor SK/surat tugas, kolom {@code kode}. Label layarnya "No. SK" / "No Surat Tugas" dan
	 * dikirim ke template JasperReports sebagai parameter {@code sk_mengajar}. Perhatikan bahwa
	 * {@link #getKode()} tidak pernah mengembalikan {@code null}, sehingga kolomnya berisi string
	 * kosong (bukan {@code NULL}) untuk SK yang belum bernomor.
	 */
	private String kode;

	/**
	 * Tanggal penerbitan SK, kolom {@code tanggalSuratTugas} (tipe {@code DATE}). Dikirim ke
	 * laporan sebagai {@code tanggal_mengajar} beserta tiga varian format
	 * {@code tanggal_mengajar_1..3}.
	 */
	private Date tanggalSuratTugas;

	/**
	 * Tanggal mulai berlakunya penugasan — "TMT" (terhitung mulai tanggal), kolom
	 * {@code tmtSuratTugas} (tipe {@code DATE}). Dikirim ke laporan sebagai {@code tmt_mengajar}
	 * beserta tiga varian format {@code tmt_mengajar_1..3}. Tidak ada validasi apa pun yang
	 * memastikan TMT tidak mendahului {@link #tanggalSuratTugas}.
	 */
	private Date tmtSuratTugas;

	/**
	 * Program/jalur penyelenggaraan tempat penugasan ini berlaku, kolom {@code program}.
	 *
	 * <p><b>Tidak punya kendali di form mana pun.</b> Hanya terisi pada baris yang dibangkitkan
	 * otomatis (disalin dari {@code JadwalPelajaran.getProgram()}); baris yang dibuat lewat layar
	 * master selalu {@code NULL}. Karena {@code CommonAcademicSyncHelper} memakai kolom ini
	 * sebagai bagian kunci pencarian, perbedaan itu bisa melahirkan baris ganda untuk periode
	 * yang sama — lihat Javadoc kelas.</p>
	 */
	private String program;

	/**
	 * Tahun akademik penugasan dalam format {@code "2025/2026"}, kolom {@code tahunAkademik}.
	 * Bersama {@link #semester}, {@link #program}, {@link #sekolah}, dan {@link #guru} membentuk
	 * kunci logis baris ini. Lihat {@link #getTahunAkademik()} untuk nilai cadangannya.
	 */
	private String tahunAkademik;

	/**
	 * Tahun kalender awal dari {@link #tahunAkademik}, kolom {@code tahun}. Nilai turunan yang
	 * dihitung ulang setiap kali {@link #getTahun()} dipanggil; disimpan agar bisa dipakai
	 * sebagai kriteria/pengelompokan numerik tanpa mem-parsing string tahun akademik.
	 */
	private Integer tahun;

	/**
	 * Semester penugasan, kolom {@code semester}. Berisi salah satu dari
	 * {@link Perkuliahan#GANJIL} ({@code "Ganjil"}) atau {@link Perkuliahan#GENAP}
	 * ({@code "Genap"}) — <b>teks</b>, berbeda dari {@code JadwalPelajaran.semester} yang berupa
	 * angka 1/2. Konversi antara keduanya dilakukan di Action pemanggil.
	 */
	private String semester;

	/**
	 * Guru penerima SK, kolom FK {@code guru} ({@code NOT NULL}). Satu-satunya relasi wajib pada
	 * entity ini, dan sumber turunan untuk {@link #sekolah} maupun {@link #yayasan}. Dipilih di
	 * form lewat komponen pencari {@code AmbilDataGuruBanbox}.
	 */
	private Guru guru;

	/**
	 * Yayasan pemilik SK, kolom FK {@code yayasan_id}.
	 *
	 * <p><b>Bukan kolom mandiri:</b> {@link #getYayasan()} selalu menimpanya dengan yayasan milik
	 * {@link #sekolah}, yang pada gilirannya diturunkan dari {@link #guru}.</p>
	 */
	private Yayasan yayasan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Semua field dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang sudah diberi
	 * nilai awal waktu saat ini pada deklarasinya. Dipakai pula oleh
	 * {@code PenugasanGuruMengajarAction.onAdd()} untuk menyiapkan form "Tambah Penugasan Guru"
	 * dan oleh {@code CommonAcademicSyncHelper.getPenugasanGuruMengajar(...)} saat membuat baris
	 * otomatis.</p>
	 *
	 * <p>Perhatikan bahwa instance kosong <b>tidak</b> berperilaku kosong sepenuhnya:
	 * {@link #getTahunAkademik()}, {@link #getSemester()}, {@link #getNama()}, dan
	 * {@link #getTahun()} langsung mengembalikan nilai turunan berbasis kalender akademik
	 * berjalan.</p>
	 */
	public PenugasanGuruMengajar() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Selain sebagai identitas persistensi, nilai ini menjadi {@code ref} berkas pindaian SK
	 * di {@link ais.database.model.file.LampiranLain} dan pembeda "baris baru vs baris lama" di
	 * {@code PenugasanGuruMengajarAction.onSave()}.</p>
	 *
	 * @return kunci primer, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris ini. Tanpa validasi.
	 *
	 * <p>Normalnya hanya dipanggil Hibernate setelah {@code INSERT}. Kolomnya dipetakan
	 * {@code insertable = false} sehingga nilai yang disetel manual tidak akan ikut dikirim saat
	 * penyisipan.</p>
	 *
	 * @param id kunci primer yang baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan label periode SK ini, <b>selalu dihitung ulang</b> menjadi
	 * {@code "<tahunAkademik>-<semester>"}.
	 *
	 * <p><b>Getter destruktif.</b> Method ini menimpa field {@link #nama} dengan hasil
	 * {@code getTahunAkademik() + "-" + getSemester()} sebelum mengembalikannya. Karena pemetaan
	 * memakai property access, nilai hasil hitungan itulah yang tertulis ke kolom {@code nama}
	 * pada flush berikutnya — apa pun yang pernah disetel lewat {@link #setNama(String)} hilang
	 * begitu baris ini dibaca. Efek sampingnya juga membuat nilai kolom "bergerak": untuk baris
	 * yang {@code tahunAkademik}/{@code semester}-nya masih {@code null}, nilai cadangan dari
	 * kalender akademik berjalanlah yang ikut tertulis.</p>
	 *
	 * <p>Kedua operand tidak pernah {@code null} (lihat {@link #getTahunAkademik()} dan
	 * {@link #getSemester()}), sehingga kolom {@code NOT NULL} ini selalu terisi. Hasilnya
	 * dipangkas spasi ujungnya sebelum dikembalikan.</p>
	 *
	 * <p><b>Dipakai dari:</b> kolom "TA/Semester" pada grid layar master, dan label tautan
	 * riwayat revisi {@code RevisiHelper.createNewRevisi(...)}.</p>
	 *
	 * @return label periode {@code "<tahun akademik>-<semester>"}; tidak pernah {@code null}
	 *         dalam praktik
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		nama = getTahunAkademik() + "-" + getSemester();
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel label periode baris ini.
	 *
	 * <p><b>Praktis tidak berpengaruh:</b> nilai apa pun yang disetel di sini akan ditimpa oleh
	 * {@link #getNama()} pada pembacaan berikutnya. Satu-satunya pemanggil nyata,
	 * {@code CommonAcademicSyncHelper.getPenugasanGuruMengajar(...)}, kebetulan menyetel nilai
	 * yang sama persis dengan yang dihitung getter, sehingga tidak ada perbedaan yang teramati.</p>
	 *
	 * @param nama label periode yang diinginkan (akan ditimpa saat dibaca)
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas pada SK ini.
	 *
	 * <p><b>Membalik kontrak kelas induk:</b> {@link GeneralValueObject#getKeterangan()} dijamin
	 * tidak pernah {@code null} (mengembalikan {@code ""}), sedangkan override ini mengembalikan
	 * field mentah sehingga <b>bisa {@code null}</b>. Pemanggil yang meneruskannya ke komponen ZK
	 * atau {@code String.trim()} harus mengantisipasinya.</p>
	 *
	 * @return catatan bebas, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas pada SK ini. Tanpa validasi; {@code null} diterima apa adanya.
	 *
	 * <p>Dipanggil dari form popup "Tambah/Ubah Penugasan Guru" ({@code onSave()}) dan dari
	 * kotak teks inline kolom "Keterangan" di grid layar master, yang langsung menyimpan lewat
	 * {@code Common.refreshUpdate(...)}.</p>
	 *
	 * @param keterangan catatan bebas yang baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan sekolah pemilik SK ini — <b>diturunkan dari guru, bukan disimpan mandiri</b>.
	 *
	 * <p><b>Getter destruktif.</b> Bila {@link #getGuru()} tidak null dan guru itu punya sekolah,
	 * field {@link #sekolah} <i>ditimpa</i> dengan sekolah guru tersebut; hanya bila guru atau
	 * sekolahnya null, nilai yang tersimpan dipertahankan (setelah diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} agar aman dari {@code LazyInitializationException}
	 * pada instance yang sudah detached).</p>
	 *
	 * <p><b>Konsekuensi yang perlu diketahui:</b> karena pemetaan memakai property access, kolom
	 * {@code sekolah_id} pada akhirnya selalu berisi sekolah guru. Padahal
	 * {@code CommonAcademicSyncHelper.getPenugasanGuruMengajar(...)} mencari baris dengan kunci
	 * {@code sekolah.id = <sekolah jadwal>}. Bila seorang guru mengajar di sekolah yang berbeda
	 * dari sekolah induknya (mis. guru lintas unit dalam satu yayasan), baris yang disimpan akan
	 * memakai sekolah induk guru sehingga pencarian berikutnya tidak pernah menemukannya — dan
	 * helper akan menyisipkan baris baru lagi setiap kali dipanggil. Jalur otomatis (tombol
	 * generate, buka detail guru, cetak laporan) karena itu berpotensi menumpuk duplikat pada
	 * konfigurasi lintas-unit.</p>
	 *
	 * @return sekolah pemilik SK, atau {@code null} bila baik guru maupun kolom tersimpan tidak
	 *         menunjuk sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		if (getGuru() != null && getGuru().getSekolah() != null) {
			sekolah = getGuru().getSekolah();
		} else {
			sekolah = check(sekolah);
		}
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik SK ini.
	 *
	 * <p><b>Menyaring diam-diam:</b> argumen {@code null} <i>atau</i> objek {@code Sekolah} yang
	 * belum punya id disimpan sebagai {@code null}. Ini mencegah Hibernate mencoba mem-persist
	 * objek transien lewat {@code CascadeType.PERSIST}, tetapi juga berarti penugasan sekolah
	 * yang belum tersimpan hilang tanpa pesan.</p>
	 *
	 * <p>Nilai yang disetel di sini hanya bertahan sampai {@link #getSekolah()} dipanggil pada
	 * baris yang gurunya punya sekolah.</p>
	 *
	 * @param sekolah sekolah pemilik SK; {@code null} atau instance tanpa id disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik SK ini — <b>diturunkan dari sekolah</b>.
	 *
	 * <p><b>Getter destruktif dan berantai.</b> Method ini memanggil {@link #getSekolah()} (yang
	 * sendiri menimpa field {@code sekolah} dari guru), lalu bila sekolah tidak null menimpa
	 * field {@link #yayasan} dengan {@code sekolah.getYayasan()}. Hasilnya diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}. Rantai lengkapnya: guru → sekolah → yayasan,
	 * sehingga yayasan yang berbeda dari induk sekolah guru mustahil bertahan di kolom
	 * {@code yayasan_id}.</p>
	 *
	 * <p>Kolom ini dipakai sebagai filter tenant di layar master maupun di
	 * {@code LaporanSKGuru}.</p>
	 *
	 * @return yayasan pemilik SK, atau {@code null} bila rantai guru→sekolah tidak sampai ke
	 *         yayasan mana pun
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
	 * Menyetel yayasan pemilik SK ini.
	 *
	 * <p>Sama seperti {@link #setSekolah(Sekolah)}: {@code null} atau instance tanpa id disimpan
	 * sebagai {@code null}. Nilainya juga akan ditimpa oleh {@link #getYayasan()} pada pembacaan
	 * berikutnya selama rantai guru→sekolah menghasilkan yayasan.</p>
	 *
	 * @param yayasan yayasan pemilik SK; {@code null} atau instance tanpa id disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan nomor SK/surat tugas, <b>tidak pernah {@code null}</b>.
	 *
	 * <p>Nilai {@code null} dikembalikan sebagai string kosong, dan nilai non-null dipangkas
	 * spasi ujungnya. Karena pemetaan memakai property access, hasil inilah yang tersimpan:
	 * kolom {@code kode} akhirnya berisi {@code ''} untuk SK yang belum bernomor, bukan
	 * {@code NULL}. Query pelaporan yang ingin mendeteksi SK belum bernomor harus menguji
	 * {@code kode = ''}.</p>
	 *
	 * <p>Nilai ini dikirim ke template JasperReports sebagai parameter {@code sk_mengajar}, dan
	 * juga menjadi target filter pencarian "No. SK" ({@code ilike ... ANYWHERE}) di layar
	 * master.</p>
	 *
	 * @return nomor SK yang sudah dipangkas, atau {@code ""} bila belum diisi
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel nomor SK/surat tugas. Tanpa validasi format maupun pemeriksaan keunikan.
	 *
	 * <p>Dipanggil dari form popup ({@code onSave()}), dari kotak teks inline kolom "No. SK" di
	 * grid layar master, dan dari kolom "No Surat Tugas" pada detail per-guru di
	 * {@code GuruAction} — dua yang terakhir langsung menyimpan lewat
	 * {@code Common.refreshUpdate(...)} tanpa tombol Simpan.</p>
	 *
	 * @param kode nomor SK yang baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan tanggal penerbitan SK, dipetakan sebagai kolom {@code DATE} (tanpa komponen
	 * jam).
	 *
	 * @return tanggal SK, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSuratTugas() {
		return tanggalSuratTugas;
	}

	/**
	 * Menyetel tanggal penerbitan SK. Tanpa validasi.
	 *
	 * <p>Dipanggil dari form popup dan dari {@code MyDatebox} inline kolom "Tgl. SK" / "Tgl.
	 * Surat Tugas" pada kedua layar; keduanya menyimpan seketika.</p>
	 *
	 * @param tanggalSuratTugas tanggal SK yang baru
	 */
	public void setTanggalSuratTugas(Date tanggalSuratTugas) {
		this.tanggalSuratTugas = tanggalSuratTugas;
	}

	/**
	 * Mengembalikan tanggal mulai berlakunya penugasan (TMT), dipetakan sebagai kolom
	 * {@code DATE}.
	 *
	 * <p>Tidak ada aturan yang memaksanya lebih besar atau sama dengan
	 * {@link #getTanggalSuratTugas()}; laporan mencetak keduanya apa adanya.</p>
	 *
	 * @return tanggal mulai berlaku penugasan, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTmtSuratTugas() {
		return tmtSuratTugas;
	}

	/**
	 * Menyetel tanggal mulai berlakunya penugasan (TMT). Tanpa validasi.
	 *
	 * @param tmtSuratTugas tanggal mulai berlaku yang baru
	 */
	public void setTmtSuratTugas(Date tmtSuratTugas) {
		this.tmtSuratTugas = tmtSuratTugas;
	}

	/**
	 * Mengembalikan program/jalur penyelenggaraan tempat penugasan ini berlaku.
	 *
	 * <p>Hanya terisi pada baris yang dibangkitkan otomatis (disalin dari
	 * {@code JadwalPelajaran.getProgram()}); baris hasil input manual selalu {@code null} karena
	 * tidak ada kendali form untuknya. Nilai ini ikut menjadi kunci pencarian di
	 * {@code CommonAcademicSyncHelper}, sehingga perbedaan tersebut dapat melahirkan baris ganda
	 * — lihat Javadoc kelas.</p>
	 *
	 * @return nama program, atau {@code null} bila baris tidak terikat program tertentu
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * Menyetel program/jalur penyelenggaraan. Tanpa validasi.
	 *
	 * <p>Satu-satunya pemanggil nyata adalah
	 * {@code CommonAcademicSyncHelper.getPenugasanGuruMengajar(...)} saat membuat baris baru.</p>
	 *
	 * @param program nama program yang baru
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan tahun akademik penugasan, dengan <b>nilai cadangan dinamis</b>.
	 *
	 * <p>Bila field-nya masih {@code null}, yang dikembalikan adalah
	 * {@code Common.getCurrentTahunAkademik()} — tahun akademik yang sedang berjalan menurut
	 * kalender akademik institusi. Berbeda dari {@link #getNama()}, method ini <b>tidak</b>
	 * menulis balik ke field; namun karena pemetaan memakai property access, nilai cadangan itu
	 * tetap ikut tersimpan ke kolom {@code tahunAkademik} pada flush berikutnya. Sebelum flush
	 * pertama, nilai yang dilaporkan bisa berubah mengikuti pergantian tahun akademik.</p>
	 *
	 * <p>Dipakai oleh {@link #getNama()}, {@link #getTahun()}, laporan (parameter {@code ta}),
	 * dan menjadi kunci pencocokan di {@code CommonAcademicSyncHelper}.</p>
	 *
	 * @return tahun akademik dalam format {@code "2025/2026"}; tidak pernah {@code null} selama
	 *         konfigurasi kalender akademik terisi
	 */
	public String getTahunAkademik() {
		return tahunAkademik == null ? Common.getCurrentTahunAkademik() : tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik penugasan. Tanpa validasi format.
	 *
	 * <p>Menyetel {@code null} berarti mengembalikan properti ini ke perilaku cadangan dinamis
	 * pada {@link #getTahunAkademik()}. Dipanggil dari combobox "Tahun Ajaran" pada form popup
	 * dan dari {@code CommonAcademicSyncHelper}.</p>
	 *
	 * @param tahunAkademik tahun akademik baru, mis. {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan semester penugasan, dengan <b>nilai cadangan dinamis</b>.
	 *
	 * <p>Bila field-nya masih {@code null}, yang dikembalikan adalah {@link Perkuliahan#GANJIL}
	 * atau {@link Perkuliahan#GENAP} sesuai {@code Common.isNowSemensterGanjil()}. Seperti
	 * {@link #getTahunAkademik()}, nilai cadangan itu tidak ditulis ke field tetapi tetap ikut
	 * tersimpan ke kolom {@code semester} karena property access.</p>
	 *
	 * <p>Nilainya berupa <b>teks</b> ({@code "Ganjil"}/{@code "Genap"}) — perhatikan bahwa
	 * {@code JadwalPelajaran.semester} justru berupa angka 1/2, sehingga setiap jalur yang
	 * menjembatani keduanya harus mengonversi (dan memang melakukannya di
	 * {@code PenugasanGuruMengajarAction}, {@code GuruAction}, dan {@code LaporanSKGuru}).</p>
	 *
	 * @return {@code "Ganjil"} atau {@code "Genap"}; tidak pernah {@code null}
	 */
	public String getSemester() {
		return semester == null ? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP) : semester;
	}

	/**
	 * Menyetel semester penugasan. Tanpa validasi — tidak ada pemeriksaan bahwa nilainya salah
	 * satu dari {@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}, padahal seluruh filter
	 * pencarian membandingkannya dengan kedua konstanta itu secara persis.
	 *
	 * <p>Menyetel {@code null} mengembalikan properti ini ke perilaku cadangan dinamis pada
	 * {@link #getSemester()}.</p>
	 *
	 * @param semester {@code "Ganjil"} atau {@code "Genap"}
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan guru penerima SK ini.
	 *
	 * <p>Nilainya diresolusi lebih dulu lewat {@link GeneralValueObject#check(Object)} — pola
	 * getter standar entity AIS yang menjaga relasi {@code LAZY} tetap aman dibaca meski
	 * instance sudah lepas dari {@code Session} yang memuatnya. Hasil {@code check()} ditugaskan
	 * kembali ke field karena instance yang dikembalikan bisa berbeda (kanonik dari identity map
	 * atau hasil reload).</p>
	 *
	 * <p>Relasi ini <b>wajib</b> ({@code nullable = false} pada kolom {@code guru}) dan menjadi
	 * sumber turunan untuk {@link #getSekolah()} serta {@link #getYayasan()}. Di layar master,
	 * {@code onSave()} menolak menyimpan bila guru belum dipilih.</p>
	 *
	 * @return guru penerima SK, atau {@code null} untuk instance baru yang belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = false)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menyetel guru penerima SK ini. Tanpa validasi.
	 *
	 * <p>Berbeda dari {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)}, setter ini
	 * <b>tidak</b> menyaring instance tanpa id — objek {@code Guru} transien akan ikut dicoba
	 * di-persist lewat {@code CascadeType.PERSIST}. Seluruh pemanggil nyata menyetel guru yang
	 * sudah tersimpan (hasil pencari {@code AmbilDataGuruBanbox} atau hasil query), sehingga
	 * perbedaan ini tidak pernah terpicu di jalur kode saat ini.</p>
	 *
	 * <p><b>Efek samping tak langsung:</b> mengganti guru juga mengganti sekolah dan yayasan
	 * baris ini pada pembacaan berikutnya.</p>
	 *
	 * @param guru guru penerima SK
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan tahun kalender awal dari tahun akademik baris ini, mis. {@code 2025} untuk
	 * {@code "2025/2026"}.
	 *
	 * <p><b>Getter destruktif dengan dua tingkat cadangan.</b> Alurnya:</p>
	 * <ol>
	 *   <li>bila {@link #getTahunAkademik()} tidak null (dalam praktik hampir selalu), potongan
	 *   sebelum tanda {@code "/"} di-parse menjadi {@code Integer} dan <b>ditulis ke field</b>
	 *   {@link #tahun} — menimpa apa pun yang pernah disetel lewat {@link #setTahun(Integer)};</li>
	 *   <li>kegagalan parsing (format tahun akademik tidak memuat {@code "/"}, memuat huruf, dan
	 *   sebagainya) <b>ditelan</b>: exception hanya dicatat ke
	 *   {@code ais.common.ErrorAuditUtil}, dan field dibiarkan bernilai sebelumnya;</li>
	 *   <li>bila setelah itu field masih {@code null}, dipakai tahun kalender saat ini dari
	 *   {@code ais.ui.util.WaktuUtil.getCalendar()}.</li>
	 * </ol>
	 *
	 * <p>Karena property access, nilai hasil hitungan inilah yang tersimpan ke kolom
	 * {@code tahun}. Konsekuensi yang perlu diingat: baris dengan tahun akademik berformat tidak
	 * lazim akan diam-diam menyimpan <i>tahun berjalan</i> — bukan tahun akademiknya — dan tidak
	 * ada tanda apa pun di layar bahwa itu terjadi.</p>
	 *
	 * @return tahun kalender awal periode ini; tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (getTahunAkademik() != null) {
			try {
				tahun = Integer.parseInt(StringUtils.split(getTahunAkademik(), "/")[0]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PenugasanGuruMengajar.java:221");

			}
		}
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun kalender awal periode ini.
	 *
	 * <p><b>Praktis tidak berpengaruh:</b> {@link #getTahun()} menghitung ulang nilainya dari
	 * {@link #getTahunAkademik()} setiap kali dipanggil. Tidak ada pemanggil nyata di seluruh
	 * basis kode — setter ini hanya ada agar properti turunan tetap memenuhi kontrak JavaBean
	 * yang dibutuhkan Hibernate.</p>
	 *
	 * @param tahun tahun kalender yang diinginkan (akan ditimpa saat dibaca)
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

}
