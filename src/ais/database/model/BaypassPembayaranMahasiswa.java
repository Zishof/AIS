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

import ais.common.Common;

/**
 * Entity <b>pengecualian (bypass) syarat pembayaran mahasiswa</b> — satu baris tabel
 * {@code public.baypass_pembayaran_mahasiswa} adalah satu "surat sakti" tertulis yang menyatakan
 * bahwa seorang {@link Mahasiswa} tertentu <i>boleh melewati gerbang syarat pembayaran</i> untuk
 * kombinasi (jenis kegiatan, semester, tahap, rentang tanggal) tertentu, meskipun tagihannya belum
 * lunas.
 *
 * <h2>Peran dalam alur bisnis</h2>
 * <p>
 * AIS memasang beberapa <b>gerbang pembayaran</b> di depan aksi akademik: pengisian KRS, persetujuan
 * KRS, KRS semester pendek (SP), entry nilai dan absensi SP, pencetakan kartu ujian UTS/UAS,
 * pengajuan proposal/sidang skripsi, hingga pendaftaran wisuda. Gerbang-gerbang itu pada dasarnya
 * bertanya "apakah mahasiswa ini sudah membayar minimal sekian?". Kelas ini adalah
 * <b>daftar putih (whitelist) resmi</b> yang menjawab "tidak perlu ditanya — mahasiswa ini
 * dikecualikan". Kasus pemakaian nyata di lapangan antara lain:
 * </p>
 * <ul>
 *   <li><b>Beasiswa</b> penuh/parsial yang dananya belum cair atau tidak melewati kanal pembayaran
 *       mahasiswa sama sekali (dibayar langsung oleh pemberi beasiswa ke institusi);</li>
 *   <li><b>Dispensasi</b>/keringanan yang disetujui pimpinan (mis. mahasiswa terdampak musibah,
 *       tunggakan yang sudah diangsur di luar sistem, mahasiswa kerja sama instansi);</li>
 *   <li><b>Koreksi operasional darurat</b> saat rekonsiliasi bank (H2H) terlambat masuk dan
 *       mahasiswa terancam tidak bisa mengisi KRS/ikut ujian pada hari-H;</li>
 *   <li><b>Mahasiswa dengan skema pembayaran khusus</b> yang tidak bisa dimodelkan lewat
 *       {@link JenisPembayaran}/{@link JadwalPembayaran} biasa.</li>
 * </ul>
 * <p>
 * Alasan pemberian pengecualian disimpan bebas-teks di {@link #getKeterangan()} — tidak ada
 * enumerasi/master "jenis dispensasi", sehingga pelaporan pengecualian sepenuhnya bergantung pada
 * disiplin operator mengisi kolom itu.
 * </p>
 *
 * <h2>Bagaimana baris ini dibaca (semantik pencocokan)</h2>
 * <p>
 * Pembaca kanonik satu-satunya adalah
 * {@code ais.common.CommonPaymentHelper#checkBaypassStatusPembayaranMahasiswa} (di-ekspos ulang
 * lewat {@code Common.checkBaypassStatusPembayaranMahasiswa}). Method itu hanya menghitung JUMLAH
 * baris yang cocok — isi barisnya tidak pernah dibaca — dan menganggap "ada minimal satu baris
 * cocok" sebagai izin lewat. Aturan pencocokannya:
 * </p>
 * <ol>
 *   <li>{@link #getMahasiswa()} harus sama persis dengan mahasiswa yang diperiksa (kolom
 *       {@code mahasiswa} <b>tidak boleh null</b>, lihat {@code @JoinColumn(nullable = false)}) —
 *       jadi pengecualian selalu bersifat per-individu, tidak ada bypass massal per angkatan/
 *       jurusan dalam satu baris;</li>
 *   <li>{@link #getJenisKegiatan()} harus salah satu dari jenis kegiatan yang sedang ditanyakan,
 *       <b>ATAU bernilai {@code null}</b>. Nilai {@code null} berarti <b>berlaku untuk SEMUA jenis
 *       kegiatan</b> (di layar ditampilkan sebagai "Semua") — ini adalah bentuk pengecualian
 *       terluas yang bisa dibuat lewat satu baris;</li>
 *   <li>{@link #getSemester()} harus sama dengan semester berjalan, <i>kecuali</i> bila pemanggil
 *       menyertakan {@code tahap} bukan {@code null}/{@code 0} — dalam kasus itu pencocokan
 *       berpindah ke {@link #getTahap()} dan syarat semester dilonggarkan menjadi "selalu cocok";</li>
 *   <li>tanggal hari ini harus berada di dalam rentang {@link #getBerlakuMulai()} —
 *       {@link #getBerlakuSampai()}; ujung yang {@code null} berarti tidak dibatasi. Perbandingan
 *       dilakukan pada level TANGGAL (dibungkus fungsi SQL {@code DATE(...)} di kedua sisi) agar
 *       inklusif di hari mulai maupun hari terakhir berapa pun komponen jamnya. <b>Kedua ujung
 *       null berarti pengecualian berlaku selamanya</b> — tidak ada validasi yang mewajibkan masa
 *       berlaku diisi (lihat {@code onSave} di
 *       {@code ais.action.master.BaypassPembayaranMahasiswaAction}, hanya mahasiswa/tahun
 *       akademik/jenis semester yang wajib).</li>
 * </ol>
 * <p>
 * Perhatikan bahwa {@link #getTahunAkademik()} dan {@link #getGanjilGenap()} <b>TIDAK ikut
 * dipakai</b> sebagai kriteria pencocokan; keduanya hanya bahan untuk MENGHITUNG {@link #semester}
 * (lihat {@link #getSemester()}) dan bahan tampilan/laporan. Yang benar-benar mengunci "kapan"
 * pengecualian berlaku adalah pasangan {@code semester}/{@code tahap} plus rentang tanggal.
 * </p>
 *
 * <h2>Siapa yang membaca entity ini</h2>
 * <p>
 * Sebaran pembacanya luas sekali untuk sebuah tabel sekecil ini — praktis setiap gerbang akademik
 * berbasis pembayaran menghormatinya:
 * {@code CommonHelperClass} (KRS reguler/SP, persetujuan KRS, entry nilai, pengajuan skripsi/
 * sidang/wisuda), {@code UtsDanUasCheckerHelper} (kartu ujian UTS/UAS),
 * {@code GateBayarSpUtil} (absensi &amp; entry nilai semester pendek), {@code SyaratUjianAction},
 * {@code SkripsiAction}, {@code MahasiswaRequestTugasAkhirAction}, dan {@code CommonReportHelper}.
 * Karena itu satu baris dengan {@code jenisKegiatan = null} dan masa berlaku terbuka secara efektif
 * membebaskan mahasiswa bersangkutan dari <b>seluruh</b> gerbang pembayaran akademik sekaligus.
 * </p>
 *
 * <h2>Efek samping saat baris dibuat/dihapus (di luar entity ini)</h2>
 * <p>
 * Entity ini sendiri pasif, tetapi {@code BaypassPembayaranMahasiswaAction} memasang efek samping
 * penting di sekitarnya bila {@link JenisKegiatan#getDigunakanSyaratKeaktifan()} bernilai
 * {@code true} dan konfigurasi {@code mhs_all_lambat_bayar_langsung_tidak_aktif} aktif:
 * </p>
 * <ul>
 *   <li><b>Simpan</b> baris bypass → {@code KegiatanHelper.updateBatasStudiMahasiswa(...)} dipanggil
 *       dan {@link HistoryStatusMahasiswa} periode berjalan di-set {@code AKTIF};</li>
 *   <li><b>Hapus</b> baris bypass → sebaliknya, status mahasiswa di-set {@code TIDAK_AKTIF} (dan
 *       disimpan) <i>sebelum</i> penghapusan baris benar-benar dieksekusi. Bila penghapusan gagal
 *       karena kendala relasi, mahasiswa sudah terlanjur dinonaktifkan sementara baris bypass-nya
 *       masih ada — kondisi tak konsisten yang harus dirapikan manual.</li>
 * </ul>
 * <p>
 * Jadi mencabut sebuah pengecualian bukan operasi netral: ia bisa langsung menonaktifkan status
 * kemahasiswaan seseorang.
 * </p>
 *
 * <h2>Audit</h2>
 * <p>
 * Kelas ditandai {@link Audited} (Hibernate Envers), dan penghapusan pun dilakukan lewat
 * {@code Common.refreshDelete} (Hibernate, bukan SQL native) sehingga riwayat pembuatan, perubahan,
 * maupun pencabutan pengecualian ikut terekam di tabel revisi — layar daftar bahkan menampilkan
 * tombol revisi lewat {@code RevisiHelper}. Ini berbeda (lebih baik) dari
 * {@link CicilanPembayaranGagal} yang penghapusan barisnya memakai SQL native dan lolos dari
 * Envers. Selain Envers, kolom {@link #getOleh()}/{@link #getOlehId()}/
 * {@link #getTanggal_dirubah()} menyimpan jejak "siapa terakhir mengubah".
 * </p>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 * <ol>
 *   <li><b>Jejak audit warisan</b> — {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()};</li>
 *   <li><b>Sasaran pengecualian</b> — {@link #getMahasiswa()}, {@link #getJenisKegiatan()};</li>
 *   <li><b>Cakupan waktu akademik</b> — {@link #getTahunAkademik()}, {@link #getGanjilGenap()},
 *       {@link #getSemester()}, {@link #getTahap()};</li>
 *   <li><b>Cakupan waktu kalender</b> — {@link #getBerlakuMulai()}, {@link #getBerlakuSampai()};</li>
 *   <li><b>Keterangan/administratif</b> — {@link #getKeterangan()}, {@link #toString()}.</li>
 * </ol>
 *
 * <h2>Catatan non-obvious</h2>
 * <ul>
 *   <li><b>Komentar generator salah salin-tempel.</b> Javadoc asli hasil hbm2java berbunyi
 *       "Bank generated by hbm2java" — sisa penyalinan dari entity {@code Bank}, sama sekali tidak
 *       berhubungan dengan tabel ini. Pola salah salin yang sama sudah dikonfirmasi pada beberapa
 *       entity lain di paket ini.</li>
 *   <li><b>Nama kelas memakai ejaan "Baypass"</b> (bukan "Bypass"), termasuk nama tabel dan nama
 *       method helper {@code checkBaypassStatusPembayaranMahasiswa}. Ejaan ini sudah terlanjur
 *       menyebar ke DAO, Action, menu, dan puluhan pemanggil, jadi jangan "diperbaiki" sepihak —
 *       label menu di {@code MenuSnapshotData} sudah dieja "Bypass Pembayaran Mahasiswa" sementara
 *       {@code InitMenuHelper} masih menulis "Baypass Pembayaran Mahasiswa", sehingga kedua ejaan
 *       hidup berdampingan di UI.</li>
 *   <li><b>{@link #getSemester()} bukan getter biasa</b> — ia MENGHITUNG ULANG dan MENIMPA field
 *       {@code semester}. Lihat dokumentasi method tersebut; ini konsekuensi terpenting dalam file
 *       ini.</li>
 *   <li><b>Field audit dideklarasikan ulang.</b> {@link #id}, {@link #oleh}, {@link #olehId}, dan
 *       {@code tanggal_dirubah} sudah ada di {@link GeneralValueObject}, tetapi kelas induk itu
 *       BUKAN {@code @Entity}/{@code @MappedSuperclass} — hanya POJO abstrak biasa — sehingga
 *       Hibernate tidak memetakan properti milik induk. Deklarasi ulang di sini adalah
 *       <b>keharusan teknis</b>, bukan duplikasi yang salah.</li>
 *   <li><b>Nama kolom sebagian mengandalkan default.</b> Hanya {@code id}, {@code keterangan},
 *       {@code mahasiswa}, {@code jenis_kegiatan}, {@code berlaku_mulai}, dan
 *       {@code berlaku_sampai} yang punya {@code @Column}/{@code @JoinColumn} eksplisit;
 *       {@code semester}, {@code tahap}, {@code tahunAkademik}, {@code ganjilGenap}, {@code oleh},
 *       {@code olehId}, dan {@code tanggal_dirubah} dipetakan lewat strategi penamaan bawaan
 *       Hibernate. Konsekuensinya nama kolom untuk properti camelCase mengikuti apa adanya nama
 *       properti (dilipat huruf kecil oleh PostgreSQL), <i>bukan</i> gaya {@code snake_case} —
 *       jangan berasumsi kolomnya bernama {@code tahun_akademik}/{@code ganjil_genap} saat menulis
 *       SQL native.</li>
 *   <li><b>Tidak ada kunci unik alami.</b> Tidak ada apa pun (baik di anotasi maupun di
 *       {@code onSave}) yang mencegah dibuatnya dua atau lebih baris bypass identik untuk mahasiswa
 *       + semester + jenis kegiatan yang sama. Duplikat tidak berbahaya bagi logika pencocokan
 *       (yang hanya menghitung {@code count != 0}), tetapi membuat pencabutan jadi rawan: menghapus
 *       satu baris belum tentu mencabut pengecualiannya.</li>
 *   <li><b>Tidak ada kolom "disetujui oleh"/"status persetujuan".</b> Model ini tidak mengenal alur
 *       pengajuan–persetujuan berjenjang; satu operator yang punya hak CREATE pada menu ini langsung
 *       menerbitkan pengecualian yang berlaku. Satu-satunya jejak "siapa" adalah kolom audit
 *       {@code oleh}/{@code olehId} yang diisi otomatis oleh
 *       {@code ais.database.hibernate.AuditTimestampInterceptor}, plus riwayat Envers.</li>
 *   <li><b>Cakupan otorisasi hanya sebatas menu.</b> Hak akses ditentukan
 *       {@code CommonPrivilages.checkPrevilages(READ/CREATE/UPDATE/DELETE)} terhadap menu
 *       {@code /pages/master/baypass_pembayaran_mahasiswa.zul}; TIDAK ada penyempitan lingkup ke
 *       fakultas/jurusan/unit kerja operator baik pada daftar ({@code initCriteria}) maupun pada
 *       pemilihan mahasiswa di form tambah. Siapa pun yang memegang hak CREATE pada menu ini dapat
 *       menerbitkan pengecualian untuk mahasiswa mana pun di seluruh institusi. Layar yang sama
 *       juga menyediakan tombol unggah massal ({@code Common.uploadData}) yang membuat banyak baris
 *       bypass sekaligus dari berkas.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see Mahasiswa
 * @see JenisKegiatan
 * @see CicilanPembayaran
 * @see JadwalPembayaran
 * @see HistoryStatusMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "baypass_pembayaran_mahasiswa")

public class BaypassPembayaranMahasiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya dihasilkan generator dan dipertahankan apa adanya
	 * supaya instance yang tersimpan di session ZK/HTTP lama tetap dapat dideserialisasi setelah
	 * kelas ini dikompilasi ulang.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama baris (kolom {@code id}, IDENTITY). Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass} — lihat catatan pada Javadoc kelas.
	 */
	private Long id;
	/**
	 * Nama/identitas pengguna terakhir yang mengubah baris ini, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Bagian dari jejak audit warisan
	 * {@link GeneralValueObject}, dideklarasikan ulang karena alasan pemetaan Hibernate.
	 */
	private String oleh;
	/**
	 * Identitas teknis pengguna terakhir yang mengubah baris ini (id pengguna ditambah informasi
	 * pemanggil/IP yang dirangkai {@code AuditTimestampInterceptor.olehId()}). Untuk baris bypass
	 * inilah satu-satunya jejak "siapa yang menerbitkan pengecualian ini" di luar riwayat Envers.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas teknis pengguna terakhir yang mengubah baris ini. Getter murni tanpa
	 * efek samping.
	 *
	 * @return isi kolom {@code olehId}, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas teknis pengguna pengubah. <b>Setter defensif</b>: nilai {@code null} atau
	 * string kosong/hanya spasi <b>diabaikan diam-diam</b> (method langsung {@code return} tanpa
	 * mengubah apa pun), sehingga nilai lama dipertahankan. Tujuannya mencegah jejak audit yang
	 * sudah benar tertimpa nilai kosong oleh jalur pembaruan yang tidak membawa konteks pengguna.
	 * Konsekuensinya: bila sebuah perubahan terjadi tanpa konteks pengguna, kolom ini akan tetap
	 * menunjuk pelaku SEBELUMNYA — jejak audit bisa menyesatkan, bukan sekadar kosong.
	 *
	 * @param olehId identitas teknis pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Berperilaku defensif sama persis dengan
	 * {@link #setOlehId(String)}: {@code null} atau string kosong/hanya spasi diabaikan diam-diam
	 * dan nilai lama dipertahankan.
	 *
	 * @param oleh nama/identitas pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/identitas pengguna terakhir yang mengubah baris ini. Getter murni tanpa
	 * efek samping.
	 *
	 * @return isi kolom {@code oleh}, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Baris gabungan berisi <b>dua</b> anggota kelas yang sengaja ditulis dalam satu baris fisik
	 * (pola penyisipan audit yang dipakai seragam di seluruh paket {@code ais.database.model});
	 * jangan dipecah agar diff lintas entity tetap seragam.
	 *
	 * <ol>
	 *   <li>{@code onUpdate()} — callback JPA {@code @PreUpdate} yang memanggil
	 *       {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate menerbitkan
	 *       {@code UPDATE}. Callback itulah yang mengisi ulang {@code tanggal_dirubah},
	 *       {@code oleh}, dan {@code olehId} dari konteks pengguna yang sedang berjalan. Tidak ada
	 *       pasangan {@code @PrePersist}: pada operasi INSERT, {@code tanggal_dirubah} memakai nilai
	 *       inisialisasi field di bawah (waktu objek dibuat di memori, bukan waktu commit), dan
	 *       {@code oleh}/{@code olehId} diisi lewat jalur interceptor Hibernate global.</li>
	 *   <li>{@code tanggal_dirubah} — field waktu perubahan terakhir, diinisialisasi
	 *       {@code ais.ui.util.WaktuUtil.getDate()} saat objek dibuat.</li>
	 * </ol>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Berbeda dari {@link #setOleh(String)}/
	 * {@link #setOlehId(String)}, setter ini <b>tidak defensif</b> — nilai {@code null} diterima apa
	 * adanya dan akan mengosongkan jejak waktu.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}). Getter murni
	 * tanpa efek samping.
	 *
	 * @return waktu perubahan terakhir; untuk baris yang belum pernah di-{@code UPDATE} nilainya
	 *         adalah waktu objek dibuat di memori
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris pengecualian, dipakai di combobox, pesan, dan label progres
	 * tombol "Singkronkan" pada layar {@code BaypassPembayaranMahasiswaAction}.
	 *
	 * <p>
	 * Formatnya {@code "<mahasiswa>, semester = <semester>"}. Bila {@link #mahasiswa} bernilai
	 * {@code null}, method mengembalikan string kosong — perhatikan bahwa dalam kasus itu bagian
	 * {@code ", semester = ..."} ikut hilang seluruhnya (operator ternary membungkus seluruh
	 * penggabungan string, bukan hanya bagian mahasiswa).
	 * </p>
	 *
	 * <p>
	 * Method membaca <b>field</b> {@code mahasiswa} secara langsung, bukan lewat
	 * {@link #getMahasiswa()}, sehingga tidak memicu resolusi lazy {@code check(...)}. Namun
	 * {@code mahasiswa.toString()} tetap dapat memicu inisialisasi proxy Hibernate dan melempar
	 * {@code LazyInitializationException} bila dipanggil di luar session aktif. Field
	 * {@code semester} juga dibaca langsung — nilai yang ditampilkan adalah nilai tersimpan, bukan
	 * hasil hitung ulang {@link #getSemester()}.
	 * </p>
	 *
	 * @return deskripsi singkat baris pengecualian, atau string kosong bila mahasiswa belum diisi
	 */
	public String toString() {
		return mahasiswa == null ? "" : mahasiswa.toString() + ", semester = " + semester;
	}

	/**
	 * Mahasiswa penerima pengecualian (kolom {@code mahasiswa}, WAJIB terisi). Pengecualian di AIS
	 * selalu per-individu; tidak ada baris "bypass untuk seluruh angkatan".
	 */
	private Mahasiswa mahasiswa;
	/**
	 * Semester berjalan tempat pengecualian berlaku. <b>Bukan nilai murni yang diketik operator</b>
	 * — dihitung ulang dari {@link #tahunAkademik} + {@link #ganjilGenap} + data
	 * {@link #mahasiswa}; lihat {@link #getSemester()}.
	 */
	private Integer semester;
	/**
	 * Tahap pembayaran yang dikecualikan (1..n), hanya relevan bila {@code ConstantValues.aktifkanTahapan}
	 * aktif. Bila terisi, pencocokan bypass berpindah dari {@code semester} ke {@code tahap}.
	 */
	private Integer tahap;
	/**
	 * Alasan/catatan pemberian pengecualian (kolom {@code keterangan}, bebas teks, boleh
	 * {@code null}). Satu-satunya tempat alasan dispensasi/beasiswa didokumentasikan.
	 */
	private String keterangan;
	/**
	 * Tahun akademik dalam format {@code "yyyy/yyyy"} (mis. {@code "2026/2027"}). Tidak dipakai
	 * sebagai kriteria pencocokan bypass; hanya bahan hitung {@link #semester} dan tampilan.
	 */
	private String tahunAkademik;
	/**
	 * Jenis semester, salah satu dari {@code Perkuliahan.GANJIL}/{@code Perkuliahan.GENAP}. Sama
	 * seperti {@link #tahunAkademik}: bahan hitung {@link #semester}, bukan kriteria pencocokan.
	 */
	private String ganjilGenap;
	/**
	 * Jenis kegiatan/pos pembayaran yang dikecualikan (kolom {@code jenis_kegiatan}, boleh
	 * {@code null}). Nilai {@code null} berarti pengecualian berlaku untuk <b>semua</b> jenis
	 * kegiatan — bentuk bypass terluas yang mungkin.
	 */
	private JenisKegiatan jenisKegiatan;

	/**
	 * Tanggal awal masa berlaku pengecualian (kolom {@code berlaku_mulai}, boleh {@code null} =
	 * tidak dibatasi di ujung awal).
	 */
	private Date berlakuMulai;
	/**
	 * Tanggal akhir masa berlaku pengecualian (kolom {@code berlaku_sampai}, boleh {@code null} =
	 * berlaku sampai kapan pun). Bersama {@link #berlakuMulai} yang juga {@code null}, baris menjadi
	 * pengecualian permanen.
	 */
	private Date berlakuSampai;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Seluruh properti dibiarkan {@code null}
	 * kecuali {@code tanggal_dirubah} yang langsung terisi waktu sekarang lewat inisialisasi field.
	 * Dipakai juga oleh {@code BaypassPembayaranMahasiswaAction.onAdd} untuk menyiapkan form kosong.
	 */
	public BaypassPembayaranMahasiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris. Kolom dideklarasikan {@code insertable = false} karena nilai
	 * dibangkitkan basis data (strategi {@code IDENTITY}); artinya {@code id} baru terisi setelah
	 * operasi simpan benar-benar dieksekusi. {@code BaypassPembayaranMahasiswaAction} memakai
	 * "{@code getId() != null}" sebagai pembeda antara alur {@code update} dan {@code save}, juga
	 * sebagai penjaga sebelum menjalankan efek samping status keaktifan mahasiswa.
	 *
	 * @return kunci utama baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris. Umumnya hanya dipanggil Hibernate; pemanggilan manual berisiko
	 * membuat entity dianggap detached.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan alasan/catatan pemberian pengecualian. Getter murni tanpa efek samping.
	 *
	 * <p>
	 * Kolom ini <b>tidak divalidasi</b> di {@code onSave} — pengecualian dapat diterbitkan tanpa
	 * alasan tertulis sama sekali. Karena tidak ada master "jenis dispensasi", isi kolom inilah
	 * satu-satunya penjelasan mengapa seorang mahasiswa dibebaskan dari syarat pembayaran.
	 * </p>
	 *
	 * @return keterangan alasan bypass, atau {@code null}/kosong bila operator tidak mengisinya
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel alasan/catatan pemberian pengecualian.
	 *
	 * @param keterangan teks bebas alasan bypass; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan mahasiswa penerima pengecualian.
	 *
	 * <p>
	 * <b>Getter dengan tulis-balik ke field (verified dari kode di file ini):</b> baris
	 * {@code mahasiswa = check(mahasiswa);} mengganti isi field dengan hasil
	 * {@link GeneralValueObject#check(Object)}. Method {@code check} meresolusi proxy lazy Hibernate
	 * (lewat {@code EntityIdentityMap}, cache, session yang tersedia, atau membuka session baru) dan
	 * mengembalikan instance yang sudah terinisialisasi — <b>bisa berupa object yang berbeda</b>
	 * dari proxy semula. Tulis-balik ini bersifat <i>idempoten</i>: ia hanya menukar referensi ke
	 * entity yang sama (id sama), tidak mengubah nilai kolom, jadi tidak membuat baris ini "kotor"
	 * secara semantik. {@code check} tidak pernah melempar exception dan tidak pernah mengembalikan
	 * {@code null} untuk argumen non-null; kegagalan resolusi bersifat senyap dan proxy dikembalikan
	 * apa adanya.
	 * </p>
	 * <p>
	 * Getter ini <b>tidak</b> menutup session Hibernate (verified: tidak ada pemanggilan
	 * {@code closeSession}/{@code session.close()} di file ini sama sekali).
	 * </p>
	 *
	 * @return mahasiswa penerima pengecualian; secara skema tidak boleh {@code null}
	 *         ({@code @JoinColumn(nullable = false)}), namun bisa {@code null} pada instance baru
	 *         yang belum disimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa penerima pengecualian. Dipanggil {@code onSave} dengan mahasiswa yang
	 * dipilih operator lewat {@code AmbilDataMahasiswaBanbox} — <b>tanpa pembatasan
	 * fakultas/jurusan/unit kerja operator</b>, sehingga mahasiswa mana pun di institusi dapat
	 * dipilih.
	 *
	 * @param mahasiswa mahasiswa penerima pengecualian
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan semester berjalan tempat pengecualian ini berlaku — <b>sekaligus MENGHITUNG
	 * ULANG dan MENIMPA field {@code semester}</b>. Ini method paling non-trivial di kelas ini dan
	 * satu-satunya yang membawa logika bisnis nyata.
	 *
	 * <h4>Perilaku</h4>
	 * <ol>
	 *   <li>Bila {@link #getTahunAkademik()} {@code null} <b>atau</b> field {@code mahasiswa}
	 *       {@code null}, nilai {@code semester} tersimpan dikembalikan apa adanya tanpa dihitung.
	 *       Komentar inline di badan method menegaskan bahwa ini <b>kondisi normal</b> (mis. baris
	 *       baru dari tombol "Tambah" yang belum diisi), bukan error — dahulu jalur ini menabrak NPE
	 *       yang tertangkap {@code catch} dan hanya menghasilkan log audit yang bising.</li>
	 *   <li>Bila keduanya ada, tahun awal diambil dari potongan pertama {@code tahunAkademik} yang
	 *       dipisah {@code "/"} (mis. {@code "2026/2027"} → {@code 2026}), lalu
	 *       {@code Common.getSemester(tahunAngkatan, ganjilGenap, pindahKeKampusIniMasukSemester,
	 *       tahun, semesterMulai)} dipanggil dan hasilnya <b>ditimpakan</b> ke field
	 *       {@code semester}.</li>
	 *   <li>Seluruh kegagalan (mis. {@code tahunAkademik} tidak berformat {@code "yyyy/yyyy"} →
	 *       {@code NumberFormatException}, atau {@code LazyInitializationException} saat proxy
	 *       {@code mahasiswa} diinisialisasi di luar session) ditelan {@code catch (Exception)},
	 *       hanya dicatat ke {@code ErrorAuditUtil}, dan nilai {@code semester} sebelumnya
	 *       dikembalikan.</li>
	 * </ol>
	 *
	 * <h4>Konsekuensi yang perlu diwaspadai</h4>
	 * <ul>
	 *   <li><b>Efek samping persisten.</b> Kelas ini memakai akses properti (anotasi berada pada
	 *       getter), sehingga Hibernate memanggil getter ini saat dirty-checking/flush. Karena
	 *       entity juga {@code dynamicUpdate}, hitung ulang yang menghasilkan nilai berbeda akan
	 *       <b>tersimpan ke basis data</b> tanpa ada aksi pengguna. Nilai {@code semester} sebuah
	 *       baris bypass karenanya dapat "bergeser" belakangan bila data
	 *       {@link Mahasiswa#getTahunangkatan()}/{@code getSemesterMulai()}/
	 *       {@code getPindahKeKampusIniMasukSemester()} dikoreksi. Karena {@code semester} adalah
	 *       kriteria pencocokan utama bypass, pergeseran itu memindahkan berlakunya pengecualian ke
	 *       semester lain — atau membuatnya tidak pernah cocok lagi.</li>
	 *   <li><b>Bisa berbeda dari angka yang dilihat operator.</b> Layar tambah/ubah menghitung
	 *       semester lewat {@code SemesterEventListener} yang memiliki kasus khusus tambahan: untuk
	 *       jenis semester GANJIL, bila tahun akademik sama dengan tahun angkatan mahasiswa, label
	 *       langsung diisi {@code 1}. Getter ini <b>tidak</b> punya kasus khusus tersebut dan selalu
	 *       memakai {@code Common.getSemester(...)}, sehingga untuk mahasiswa baru nilai yang
	 *       akhirnya tersimpan dapat berbeda dari angka yang tampil dan disetujui operator di
	 *       layar.</li>
	 *   <li>Method membaca field {@code mahasiswa} langsung (bukan {@link #getMahasiswa()}),
	 *       sehingga proxy lazy TIDAK diresolusi lebih dulu lewat {@code check(...)}; pemanggilan
	 *       {@code mahasiswa.getTahunangkatan()} di dalam blok {@code try} adalah titik tempat
	 *       inisialisasi lazy benar-benar terjadi.</li>
	 * </ul>
	 *
	 * @return semester berjalan hasil hitung ulang; nilai tersimpan sebelumnya bila data belum
	 *         lengkap atau perhitungan gagal; dapat {@code null} pada baris yang belum diisi
	 */
	public Integer getSemester() {
		// Data belum lengkap (mis. baru dibuat lewat tombol Tambah, Tahun Akademik/Mahasiswa
		// belum dipilih user) -- ini KONDISI NORMAL, bukan error, jadi jangan proses lewat
		// exception (dulu getTahunAkademik().split(...) NPE di sini lalu tertangkap catch,
		// hanya menambah log audit yang bising untuk kasus yang sebenarnya wajar).
		if (getTahunAkademik() == null || mahasiswa == null) {
			return semester;
		}
		try {
			String tahun = getTahunAkademik().split("/")[0];
			Integer tahunint = Integer.valueOf(tahun);
			semester = Common.getSemester(mahasiswa.getTahunangkatan(), getGanjilGenap(),
					mahasiswa.getPindahKeKampusIniMasukSemester(), tahunint, mahasiswa.getSemesterMulai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BaypassPembayaranMahasiswa.java:128");
			// TODO: handle exception
		}
		return semester;
	}

	/**
	 * Menyetel semester berjalan secara eksplisit. Perlu diingat nilai yang disetel di sini
	 * <b>tidak permanen</b>: pemanggilan {@link #getSemester()} berikutnya akan menghitung ulang dan
	 * menimpanya selama {@code tahunAkademik} dan {@code mahasiswa} sudah terisi.
	 * {@code onSave} memakai setter ini dengan angka hasil hitung layar (isi label semester), yang
	 * kemudian dapat ditimpa hasil hitung getter saat flush.
	 *
	 * @param semester semester berjalan; boleh {@code null}
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tahun akademik pengecualian dalam format {@code "yyyy/yyyy"}. Getter murni tanpa
	 * efek samping. Dipakai {@link #getSemester()} sebagai bahan hitung, dan oleh
	 * {@code BaypassPembayaranMahasiswaAction} untuk mencari {@link HistoryStatusMahasiswa} periode
	 * berjalan. Nilai ini <b>tidak</b> ikut menjadi kriteria pencocokan bypass.
	 *
	 * @return tahun akademik, atau {@code null} bila belum diisi
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik pengecualian. Format yang diharapkan {@code "yyyy/yyyy"} — format lain
	 * tidak ditolak di sini, tetapi akan membuat perhitungan di {@link #getSemester()} gagal diam-diam.
	 *
	 * @param tahunAkademik tahun akademik format {@code "yyyy/yyyy"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan jenis semester ({@code Perkuliahan.GANJIL}/{@code Perkuliahan.GENAP}). Getter
	 * murni tanpa efek samping; dipakai {@link #getSemester()} sebagai argumen
	 * {@code Common.getSemester(...)}. Pada layar tambah/ubah kolom ini dibuat {@code readonly}
	 * (dipilih lewat combobox, tidak boleh diketik bebas).
	 *
	 * @return jenis semester, atau {@code null} bila belum diisi
	 */
	public String getGanjilGenap() {
		return ganjilGenap;
	}

	/**
	 * Menyetel jenis semester.
	 *
	 * @param ganjilGenap salah satu dari {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP}
	 */
	public void setGanjilGenap(String ganjilGenap) {
		this.ganjilGenap = ganjilGenap;
	}

	/**
	 * Mengembalikan tahap pembayaran yang dikecualikan. Getter murni tanpa efek samping.
	 *
	 * <p>
	 * Kolom ini hanya muncul di layar bila {@code ConstantValues.aktifkanTahapan} bernilai
	 * {@code true}. Perannya dalam pencocokan bypass bersifat <b>saling menggantikan</b> dengan
	 * {@link #getSemester()}: bila pemanggil gerbang pembayaran menyertakan tahap bukan
	 * {@code null}/{@code 0}, pencocokan memakai {@code tahap} dan syarat {@code semester}
	 * dilonggarkan; bila tidak, sebaliknya. Efeknya, sebuah baris bypass yang tahapnya terisi dapat
	 * dianggap cocok untuk semester berapa pun bila pemanggilnya berbasis tahap.
	 * </p>
	 *
	 * @return nomor tahap pembayaran, atau {@code null} bila pengecualian tidak dibatasi per tahap
	 */
	public Integer getTahap() {
		return tahap;
	}

	/**
	 * Menyetel tahap pembayaran yang dikecualikan.
	 *
	 * @param tahap nomor tahap pembayaran; {@code null} berarti tidak dibatasi per tahap
	 */
	public void setTahap(Integer tahap) {
		this.tahap = tahap;
	}

	/**
	 * Mengembalikan jenis kegiatan/pos pembayaran yang dikecualikan.
	 *
	 * <p>
	 * <b>Getter dengan tulis-balik ke field (verified dari kode di file ini):</b> pola
	 * {@code jenisKegiatan = check(jenisKegiatan);} identik dengan {@link #getMahasiswa()} —
	 * meresolusi proxy lazy dan menukar referensi field, tanpa mengubah nilai kolom, tanpa melempar
	 * exception, dan tanpa menutup session Hibernate.
	 * </p>
	 * <p>
	 * <b>Nilai {@code null} bermakna khusus:</b> berarti pengecualian berlaku untuk SEMUA jenis
	 * kegiatan (layar menampilkannya sebagai "Semua", dan kriteria pencocokan di
	 * {@code CommonPaymentHelper} secara eksplisit menyertakan {@code Restrictions.isNull(
	 * "jenisKegiatan")} sebagai alternatif dari daftar jenis kegiatan yang diminta). Baris seperti
	 * ini membebaskan mahasiswa dari seluruh gerbang pembayaran akademik sekaligus, jadi
	 * membiarkannya kosong bukan pilihan netral.
	 * </p>
	 * <p>
	 * Nilai non-{@code null} juga menjadi pemicu efek samping status keaktifan mahasiswa di
	 * {@code BaypassPembayaranMahasiswaAction} — hanya bila
	 * {@link JenisKegiatan#getDigunakanSyaratKeaktifan()} bernilai {@code true}.
	 * </p>
	 *
	 * @return jenis kegiatan yang dikecualikan, atau {@code null} yang berarti "semua jenis kegiatan"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kegiatan", nullable = true)
	public JenisKegiatan getJenisKegiatan() {
		jenisKegiatan = check(jenisKegiatan);
		return jenisKegiatan;
	}

	/**
	 * Menyetel jenis kegiatan yang dikecualikan.
	 *
	 * @param jenisKegiatan jenis kegiatan; {@code null} berarti pengecualian berlaku untuk semua
	 *                      jenis kegiatan
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * Mengembalikan tanggal awal masa berlaku pengecualian (presisi {@code DATE}, tanpa komponen
	 * jam). Getter murni tanpa efek samping — baris kosong di dalam badan method adalah sisa
	 * pengeditan, bukan penanda logika yang hilang.
	 *
	 * <p>
	 * Pembacaannya di {@code CommonPaymentHelper} memakai predikat SQL
	 * {@code (berlaku_mulai is null or DATE(berlaku_mulai) &lt;= DATE(hari_ini))}: nilai
	 * {@code null} berarti "berlaku sejak kapan pun", dan pembungkus {@code DATE(...)} pada kolom
	 * memastikan pengecualian sudah aktif pada hari mulai walaupun data lama tersimpan sebagai
	 * timestamp berjam bukan tengah malam.
	 * </p>
	 *
	 * @return tanggal awal masa berlaku, atau {@code null} bila tidak dibatasi di ujung awal
	 */
	@Column(name = "berlaku_mulai", nullable = true)
	@Temporal(TemporalType.DATE)
	public Date getBerlakuMulai() {

		return berlakuMulai;
	}

	/**
	 * Menyetel tanggal awal masa berlaku pengecualian. Tidak ada validasi apa pun di sini maupun di
	 * {@code onSave} yang memastikan {@code berlakuMulai} lebih awal dari {@link #getBerlakuSampai()}
	 * — rentang terbalik hanya membuat pengecualian tidak pernah cocok, gagal secara diam-diam.
	 *
	 * @param berlakuMulai tanggal awal masa berlaku; {@code null} berarti tidak dibatasi
	 */
	public void setBerlakuMulai(Date berlakuMulai) {
		this.berlakuMulai = berlakuMulai;
	}

	/**
	 * Mengembalikan tanggal akhir masa berlaku pengecualian (presisi {@code DATE}, tanpa komponen
	 * jam). Getter murni tanpa efek samping — baris kosong di dalam badan method adalah sisa
	 * pengeditan.
	 *
	 * <p>
	 * Pembacaannya memakai predikat {@code (berlaku_sampai is null or DATE(berlaku_sampai) &gt;=
	 * DATE(hari_ini))}, sehingga hari terakhir bersifat <b>inklusif</b> dan nilai {@code null}
	 * berarti <b>berlaku tanpa batas waktu</b>. Kombinasi {@code berlakuMulai == null &amp;&amp;
	 * berlakuSampai == null} — yang lolos validasi simpan — menghasilkan pengecualian permanen yang
	 * hanya bisa dicabut dengan menghapus barisnya.
	 * </p>
	 *
	 * @return tanggal akhir masa berlaku, atau {@code null} bila tidak dibatasi di ujung akhir
	 */
	@Column(name = "berlaku_sampai", nullable = true)
	@Temporal(TemporalType.DATE)
	public Date getBerlakuSampai() {

		return berlakuSampai;
	}

	/**
	 * Menyetel tanggal akhir masa berlaku pengecualian.
	 *
	 * @param berlakuSampai tanggal akhir masa berlaku (inklusif); {@code null} berarti berlaku tanpa
	 *                      batas waktu
	 */
	public void setBerlakuSampai(Date berlakuSampai) {
		this.berlakuSampai = berlakuSampai;
	}

}
