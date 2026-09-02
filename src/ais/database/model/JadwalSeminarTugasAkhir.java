package ais.database.model;

// Generated Apr 12, 2010 11:30:55 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Entity <b>periode/gelombang penyelenggaraan seminar proposal</b> tugas akhir&nbsp;/ skripsi
 * (tabel {@code public.jadwal_seminar_tugas_akhir}).
 *
 * <p>Satu baris <b>bukan</b> jadwal seminar satu mahasiswa, melainkan satu <i>wadah</i>
 * penyelenggaraan: sebuah nama gelombang, rentang tanggal berlakunya
 * ({@link #getMulai()}&ndash;{@link #getSampai()}), tahun akademik, cakupan
 * fakultas/prodi/program, satu ruang seminar, dan sebuah <b>daftar agenda rinci</b> yang
 * disimpan terpadat di satu kolom teks ({@link #getJadwalRinci()}). Mahasiswa dikaitkan ke
 * gelombang ini dari sisi seberang: {@link MahasiswaRequestTugasAkhir} yang memegang kolom
 * FK {@code jadwal_seminar_tugas_akhir} &mdash; <b>class ini sendiri tidak menyimpan satu pun
 * referensi ke mahasiswa maupun dosen</b>.</p>
 *
 * <h3>Tempatnya dalam alur tugas akhir</h3>
 * <ol>
 *   <li>Operator akademik membuat baris gelombang di layar <i>"Tambah/Ubah Jadwal Seminar"</i>
 *       ({@code ais.action.master.JadwalSeminarTugasAkhirAction},
 *       {@code /pages/master/jadwal_seminar_tugas_akhir.zul}). Layar yang sama juga muncul
 *       sebagai tab "manajemen seminar" di dalam layar pengajuan tugas akhir
 *       ({@code MahasiswaRequestTugasAkhirAction.onJadwal}).</li>
 *   <li>Pada pengajuan tugas akhir seorang mahasiswa, gelombang dipilih lewat picker
 *       {@code ais.action.master.helper.AmbilJadwalSeminarTugasAkhirBanbox} dan disimpan ke
 *       {@link MahasiswaRequestTugasAkhir#setJadwalSeminarTugasAkhir(JadwalSeminarTugasAkhir)}.</li>
 *   <li><b>Efek samping penting:</b> begitu kolom itu terisi,
 *       {@link MahasiswaRequestTugasAkhir#getStatus()} menaikkan status pengajuan dari
 *       {@code AKTIF}/{@code REQUEST} menjadi {@code SEMINAR}. Jadi <i>menautkan sebuah baris
 *       jadwal sudah dianggap "mahasiswa masuk tahap seminar"</i>, tanpa perlu tanggal seminar
 *       maupun berita acara.</li>
 *   <li>Rekap dan daftar pesertanya dicetak oleh {@code LaporanSeminar},
 *       {@code LaporanRekapitulasiSeminar}, dan {@code LaporanRekapitulasiGelombangSeminar}
 *       (ketiganya mengirim {@link #getId()} sebagai parameter JasperReports {@code "jadwal"};
 *       {@code -1L} berarti "semua gelombang").</li>
 * </ol>
 *
 * <h3>Beda dengan {@link JadwalSidangTugasAkhir}</h3>
 * <p>Keduanya adalah <b>kembar hasil salin-tempel</b>: susunan field, anotasi, konstruktor,
 * bahkan nilai {@code serialVersionUID} dan komentar hbm2java yang salah persis sama. Bedanya
 * hanya empat:</p>
 * <ul>
 *   <li>tabel {@code jadwal_seminar_tugas_akhir} vs {@code jadwal_sidang_tugas_akhir};</li>
 *   <li>properti {@code ruangSeminar} (kolom {@code ruang_seminar}) vs {@code ruangSidang}
 *       (kolom {@code ruang_sidang});</li>
 *   <li>teks default {@link #getNama()} ("Jadwal seminar &hellip;" vs "Jadwal sidang &hellip;");</li>
 *   <li><b>tahap alur yang diwakili</b>: class ini dipakai pada tahap <i>seminar proposal</i>
 *       (pengajuan judul, sebelum bimbingan tuntas), sedangkan {@link JadwalSidangTugasAkhir}
 *       dipakai pada tahap <i>sidang akhir</i>.</li>
 * </ul>
 * <p>Yang <b>tidak</b> simetris adalah pemakaiannya. {@link JadwalSidangTugasAkhir} ditunjuk
 * langsung oleh {@link Skripsi}, dan {@code Skripsi} <i>menyalin</i> tanggal serta ruang dari
 * jadwal itu ke kolomnya sendiri ({@code Skripsi.getTanggalSidang()},
 * {@code Skripsi.getRuangSidang()}). Di sisi seminar tidak ada penyalinan semacam itu sama
 * sekali: {@code Skripsi} tidak punya properti bertipe class ini,
 * {@code MahasiswaRequestTugasAkhir.getTanggalSeminar()} adalah kolom mandiri yang tidak pernah
 * membaca {@link #getMulai()}, dan {@link #getRuangSeminar()} hanya dibaca oleh layar
 * pengelolanya sendiri (lihat catatan pada getter tersebut).</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit</b> &mdash; {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, kait {@link #onUpdate()},
 *       {@link #toString()}.</li>
 *   <li><b>Atribut periode</b> &mdash; {@link #getNama()}, {@link #getMulai()},
 *       {@link #getSampai()}, {@link #getTahunAkademik()}, {@link #getKeterangan()}.</li>
 *   <li><b>Cakupan</b> &mdash; {@link #getFakultas()}, {@link #getJurusan()},
 *       {@link #getProgram()}. Ketiganya <b>hanya penyaring pencarian</b>, bukan aturan yang
 *       ditegakkan: picker meloloskan pula baris yang kolomnya {@code NULL}, dan tidak ada satu
 *       pun kode yang menolak mahasiswa dari prodi lain saat gelombang ditautkan.</li>
 *   <li><b>Ruang</b> &mdash; {@link #getRuangSeminar()}, satu-satunya relasi lazy di class
 *       ini.</li>
 *   <li><b>Agenda rinci</b> &mdash; {@link #getJadwalRinci()} beserta tiga method
 *       pengelolanya: {@link #populateJadwal(String, Date, Date, String)},
 *       {@link #hapusJadwal(String)}, dan {@link #daftarJadwal()}.</li>
 * </ul>
 *
 * <h3>Hal yang tidak terlihat dari nama methodnya</h3>
 * <ul>
 *   <li><b>Empat getter menulis balik ke field terpetakan.</b> {@link #getNama()},
 *       {@link #getMulai()}, {@link #getSampai()}, dan {@link #getRuangSeminar()} (lewat
 *       {@code check()}). Karena entity ini {@code dynamicUpdate = true} dan {@code @Audited},
 *       sekadar <i>membaca</i> instance yang masih managed dapat memicu {@code UPDATE} berikut
 *       satu revisi Envers, tanpa aksi simpan dari pengguna. Rinciannya ada di Javadoc
 *       masing-masing getter.</li>
 *   <li><b>Tidak ada satu pun properti {@code Dosen} atau {@code Mahasiswa}.</b> Class ini
 *       murni wadah periode; penetapan dosen penguji/pembimbing seminar seluruhnya berada di
 *       {@link MahasiswaRequestTugasAkhir} (slot {@code dosen1}&hellip;{@code dosen6} yang
 *       labelnya dikonfigurasi {@link FormatNilaiProposalSkripsi}). Karena itu <b>pola "slot
 *       dosen tertukar" yang ditemukan pada {@code FormatNilaiSkripsi}/{@code Skripsi} tidak
 *       mungkin terjadi di sini</b> &mdash; tidak ada slot untuk ditukar.</li>
 *   <li><b>Format {@code jadwalRinci} rapuh.</b> Kolom {@code text} berisi banyak record yang
 *       dirangkai dengan {@code "||"} dan {@code "<>"}, diurai dengan
 *       {@code org.apache.commons.lang.StringUtils.split} yang memperlakukan pemisah sebagai
 *       <b>himpunan karakter</b>. Lihat {@link #getJadwalRinci()} untuk format lengkap dan
 *       daftar kuirknya (termasuk akibat nama agenda yang kosong).</li>
 *   <li><b>Tidak ada akses database langsung.</b> Class ini tidak meng-{@code import}
 *       {@code Session}, {@code HibernateUtil}, {@code Criteria}, maupun {@code Restrictions},
 *       dan <b>tidak punya satu pun method query statis</b>. Satu-satunya sentuhan tak langsung
 *       ke database adalah {@code check(...)} milik induk (yang dapat membuka dan menutup
 *       session sendiri) di {@link #getRuangSeminar()}. Tidak ada getter destruktif
 *       (penghapus data) di class ini.</li>
 *   <li><b>{@code GeneralValueObject} bukan {@code @MappedSuperclass}.</b> Induknya
 *       ({@link ais.database.model.GeneralValueObject}) POJO abstrak tanpa anotasi JPA, jadi
 *       Hibernate tidak memetakan properti induk. Deklarasi ulang {@code id}, {@code oleh},
 *       {@code olehId}, dan {@code tanggal_dirubah} di sini <b>keharusan teknis</b>, bukan
 *       duplikasi ceroboh; yang diwarisi adalah <i>perilaku</i>-nya, terutama
 *       {@link GeneralValueObject#check(Object)}.</li>
 *   <li><b>Penamaan kolom default.</b> Properti tanpa {@code @Column} ({@code nama},
 *       {@code mulai}, {@code sampai}, {@code program}, {@code tahunAkademik},
 *       {@code keterangan}) memakai {@code ais.database.hibernate.MyNamingStrategy}, turunan
 *       {@code DefaultNamingStrategy}: nama kolom = nama properti apa adanya (PostgreSQL
 *       melipatnya jadi huruf kecil, sehingga {@code tahunAkademik} &rarr;
 *       {@code tahunakademik}).</li>
 *   <li><b>Komentar hbm2java salah.</b> Blok komentar asli di atas class berbunyi
 *       <i>"JamPerkuliahan generated by hbm2java"</i> &mdash; sisa salin-tempel dari entity
 *       lain, tidak ada hubungannya dengan jam perkuliahan.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see JadwalSidangTugasAkhir
 * @see MahasiswaRequestTugasAkhir
 * @see FormatNilaiProposalSkripsi
 * @see Skripsi
 * @see Ruang
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jadwal_seminar_tugas_akhir")

public class JadwalSeminarTugasAkhir extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi. Nilai ini <b>kembar persis</b> dengan milik
	 * {@link JadwalSidangTugasAkhir} akibat salin-tempel; tidak berbahaya (dievaluasi per class)
	 * dan bukan petunjuk kekerabatan.
	 */
	private static final long serialVersionUID = -8842945307087672400L;

	/** Kunci utama, kolom {@code id} (IDENTITY/serial PostgreSQL). Lihat {@link #getId()}. */
	private Long id;

	/**
	 * Nama tampil pengguna yang terakhir mengubah baris ini. Diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan oleh form.
	 */
	private String oleh;

	/**
	 * Identitas (login/NIP/NIM) pengguna yang terakhir mengubah baris ini. Pasangan teknis dari
	 * {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir, dengan <b>penjagaan anti-timpa</b>: nilai
	 * {@code null} atau kosong/whitespace diabaikan diam-diam (method langsung {@code return}).
	 * Konsekuensinya setter ini <b>tidak bisa dipakai untuk mengosongkan kolom</b>.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir, dengan penjagaan anti-timpa yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 * @see #setOlehId(String)
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b> baris ini
	 * di-{@code UPDATE}, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} untuk memperbarui
	 * {@link #tanggal_dirubah} beserta {@link #oleh}/{@link #olehId} dari konteks pengguna yang
	 * sedang aktif.
	 *
	 * <p>Tidak pernah dipanggil manual dari kode aplikasi. Tidak ada pasangan
	 * {@code @PrePersist}: pada baris baru, stempel waktu berasal dari inisialisasi field
	 * {@link #tanggal_dirubah} ({@code WaktuUtil.getDate()}) yang dieksekusi saat konstruktor
	 * berjalan.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan deklarasi field {@code tanggal_dirubah}
	 * sengaja berbagi satu baris fisik &mdash; pola salin-tempel yang sama ditemukan di ratusan
	 * entity paket ini. Jangan dipecah tanpa alasan; perubahan kosmetik pada baris ini memicu
	 * konflik di banyak sesi paralel.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya <b>tidak perlu dipanggil dari kode aplikasi</b>: nilainya diisi otomatis saat
	 * pembuatan object dan diperbarui oleh {@link #onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; pada object baru berisi waktu pembuatan object, bukan
	 *         {@code null}
	 * @see #onUpdate()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas berbentuk {@code mulai_sampai_jurusan}.
	 *
	 * <p><b>Bukan</b> teks yang dilihat pengguna: layar dan picker menampilkan
	 * {@link #getNama()}. Method ini membaca <b>field langsung</b>, bukan getter, sehingga
	 * pengisian nilai default {@link #getMulai()}/{@link #getSampai()} tidak berjalan dan
	 * komponen yang {@code null} tercetak sebagai teks {@code "null"}. Tanggal dicetak dengan
	 * {@link Date#toString()} (format Java baku, bukan format lokal AIS), dan {@code jurusan}
	 * dicetak lewat {@code Jurusan.toString()} &mdash; yang pada instance lazy dapat memicu
	 * inisialisasi proxy.</p>
	 *
	 * @return gabungan tanggal mulai, tanggal sampai, dan jurusan dipisah garis bawah
	 */
	public String toString() {
		return mulai + "_" + sampai + "_" + jurusan;
	}

	/**
	 * Nama gelombang/jadwal seminar (label layar "Nama Jadwal *", wajib diisi di form).
	 * Null-safe dengan efek samping &mdash; lihat {@link #getNama()}.
	 */
	private String nama;

	/**
	 * Tanggal awal berlakunya gelombang (label layar "Mulai"). Diinisialisasi ke tanggal hari
	 * ini saat object dibuat.
	 */
	private Date mulai = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Tanggal akhir berlakunya gelombang (label layar "Sampai"). Diinisialisasi ke tanggal hari
	 * ini saat object dibuat; bila kosong saat dibaca, dihitung enam bulan setelah
	 * {@link #mulai} (lihat {@link #getSampai()}).
	 */
	private Date sampai = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Program studi pemilik gelombang (label layar "Prodi"), opsional. Hanya dipakai sebagai
	 * penyaring pencarian, tidak menegakkan pembatasan peserta.
	 */
	private Jurusan jurusan;

	/**
	 * Fakultas pemilik gelombang (label layar "Fakultas"), opsional. Sama seperti
	 * {@link #jurusan}: penyaring pencarian saja.
	 */
	private Fakultas fakultas;

	/**
	 * Kode program/jenjang penyelenggaraan (label layar "Program"), opsional. Isinya berasal dari
	 * daftar {@code Common.initPrograms(...)} dan disimpan sebagai teks, bukan FK.
	 */
	private String program;

	/**
	 * Tahun akademik penyelenggaraan (label layar "Tahun Akademik"), wajib diisi oleh validasi
	 * layar. Disimpan sebagai teks periode, bukan FK.
	 */
	private String tahunAkademik;

	/** Catatan bebas operator (label layar "Keterangan"). */
	private String keterangan;

	/**
	 * Ruang tempat seminar diselenggarakan (label layar "Ruang"), opsional dan {@code LAZY}.
	 * Nilainya hanya informatif &mdash; lihat {@link #getRuangSeminar()}.
	 */
	private Ruang ruangSeminar;

	/**
	 * Daftar agenda rinci gelombang, disimpan <b>terpadat dalam satu kolom teks</b>
	 * ({@code jadwal_rinci}) alih-alih sebagai tabel anak. Format dan kuirknya dijelaskan pada
	 * {@link #getJadwalRinci()}.
	 */
	private String jadwalRinci;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JavaBeans.
	 *
	 * <p>Tidak melakukan apa pun secara eksplisit, tetapi <b>inisialisasi field</b> tetap
	 * berjalan: {@link #tanggal_dirubah}, {@link #mulai}, dan {@link #sampai} langsung terisi
	 * waktu/tanggal saat ini. Jadi object baru sudah punya rentang tanggal "hari ini sampai hari
	 * ini" sebelum form diisi.</p>
	 */
	public JadwalSeminarTugasAkhir() {

	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dipakai luas sebagai penanda "sudah tersimpan atau belum": layar pengelola hanya
	 * menyimpan agenda rinci ke database bila {@code getId() != null}, dan ketiga laporan seminar
	 * mengirim nilai ini sebagai parameter {@code "jadwal"} ({@code -1L} bila {@code null}).</p>
	 *
	 * @return id baris, atau {@code null} bila object belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Diisi Hibernate setelah {@code INSERT}; jangan disetel manual.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama gelombang seminar, yaitu teks yang dipakai di seluruh layar dan picker.
	 *
	 * <p><b>Getter berefek samping.</b> Bila field masih {@code null}/kosong, method ini
	 * <b>mengisi field</b> dengan teks default {@code "Jadwal seminar skripsi / tugas akhir"}
	 * lalu mengembalikannya. Pada instance yang masih managed, pengisian itu adalah perubahan
	 * state yang &mdash; karena {@code dynamicUpdate = true} &mdash; dapat ikut ter-{@code flush}
	 * ke database beserta satu revisi Envers, walaupun pengguna hanya membuka layar daftar.
	 * Akibat praktisnya: baris lama yang namanya kosong akan "mendapat nama" begitu pertama kali
	 * ditampilkan, dan semuanya bernama sama.</p>
	 *
	 * @return nama gelombang; tidak pernah {@code null} maupun kosong
	 */
	public String getNama() {
		if (nama == null || nama.trim().isEmpty()) {
			nama = "Jadwal seminar skripsi / tugas akhir";
		}
		return nama;
	}

	/**
	 * Menyetel nama gelombang seminar apa adanya, tanpa validasi maupun pemotongan panjang.
	 * Layar pengelola sudah menolak nama kosong sebelum memanggil setter ini.
	 *
	 * @param nama nama gelombang; {@code null}/kosong diterima, tetapi akan digantikan teks
	 *             default pada pembacaan berikutnya oleh {@link #getNama()}
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan program studi pemilik gelombang (label layar "Prodi").
	 *
	 * <p>Relasi {@code EAGER} dengan {@code FetchMode.SELECT}, jadi berbeda dari
	 * {@link #getRuangSeminar()} getter ini tidak perlu &mdash; dan tidak melakukan &mdash;
	 * resolusi proxy lewat {@code check()}.</p>
	 *
	 * <p>Nilainya <b>tidak membatasi siapa yang boleh memakai gelombang ini</b>: picker
	 * {@code AmbilJadwalSeminarTugasAkhirBanbox} memakainya sebagai kriteria pencarian opsional
	 * dan sengaja tetap meloloskan baris yang kolomnya {@code NULL}.</p>
	 *
	 * @return prodi pemilik gelombang, atau {@code null} bila gelombang berlaku lintas prodi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Menyetel program studi pemilik gelombang.
	 *
	 * @param jurusan prodi pemilik; {@code null} berarti berlaku lintas prodi
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan fakultas pemilik gelombang (label layar "Fakultas"). Sama seperti
	 * {@link #getJurusan()}: {@code EAGER}, tanpa efek samping, dan hanya berperan sebagai
	 * penyaring pencarian.
	 *
	 * @return fakultas pemilik gelombang, atau {@code null} bila berlaku lintas fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		return fakultas;
	}

	/**
	 * Menyetel fakultas pemilik gelombang.
	 *
	 * @param fakultas fakultas pemilik; {@code null} berarti berlaku lintas fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan catatan bebas operator (label layar "Keterangan").
	 *
	 * @return keterangan gelombang, atau {@code null} bila tidak diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas operator.
	 *
	 * @param keterangan keterangan gelombang; {@code null} diterima
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tanggal awal berlakunya gelombang (kolom bertipe {@code DATE}, tanpa jam).
	 *
	 * <p><b>Getter berefek samping:</b> bila field {@code null} &mdash; yang terjadi ketika baris
	 * lama di database menyimpan {@code NULL} &mdash; field diisi <b>tanggal hari ini</b> lalu
	 * dikembalikan. Pada instance managed hal itu bisa ter-{@code flush} ke database. Artinya
	 * baris lama tanpa tanggal mulai akan diam-diam "dimulai hari ini" pada saat pertama kali
	 * dibaca layar atau laporan.</p>
	 *
	 * <p>Nilainya murni informatif/penyaring: tidak ada kode yang memvalidasi bahwa tanggal
	 * seminar seorang mahasiswa berada di dalam rentang
	 * {@code mulai}&ndash;{@code sampai} gelombangnya.</p>
	 *
	 * @return tanggal mulai; tidak pernah {@code null} setelah getter ini dipanggil
	 * @see #getSampai()
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		if (mulai == null) {
			mulai = ais.ui.util.WaktuUtil.getDate();
		}
		return mulai;
	}

	/**
	 * Menyetel tanggal awal berlakunya gelombang.
	 *
	 * @param mulai tanggal mulai; {@code null} diterima, tetapi akan diganti tanggal hari ini
	 *              pada pembacaan berikutnya oleh {@link #getMulai()}
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal akhir berlakunya gelombang (kolom bertipe {@code DATE}).
	 *
	 * <p><b>Getter berefek samping:</b> bila field {@code null}, field diisi hasil perhitungan
	 * <b>{@link #getMulai()} + 6 bulan</b> lalu dikembalikan &mdash; dan karena {@code getMulai()}
	 * sendiri berefek samping, satu pembacaan dapat menulis <i>dua</i> field sekaligus pada baris
	 * lama yang kedua kolomnya {@code NULL}.</p>
	 *
	 * <p><b>Kuirk perhitungan.</b> Penambahan dilakukan dengan
	 * {@code calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6)}, bukan
	 * {@code calendar.add(...)}. Pada {@code Calendar} lenient nilai bulan &gt; 11 memang
	 * digulung ke tahun berikutnya sehingga hasilnya benar untuk kasus umum, tetapi
	 * <b>tanggal ikut bergeser bila bulan tujuan lebih pendek</b>: 31 Agustus + 6 bulan menjadi
	 * 31 Februari yang digulung menjadi awal Maret, bukan akhir Februari.</p>
	 *
	 * @return tanggal sampai; tidak pernah {@code null} setelah getter ini dipanggil
	 * @see #getMulai()
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		if (sampai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getMulai());
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 6);
			sampai = calendar.getTime();
		}
		return sampai;
	}

	/**
	 * Menyetel tanggal akhir berlakunya gelombang.
	 *
	 * @param sampai tanggal sampai; {@code null} diterima, tetapi akan diganti hasil hitungan
	 *               "mulai + 6 bulan" pada pembacaan berikutnya oleh {@link #getSampai()}
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan tahun akademik penyelenggaraan gelombang (label layar "Tahun Akademik").
	 *
	 * <p>Getter polos, tanpa nilai default: berbeda dari sebagian entity lain di paket ini,
	 * tahun akademik <b>tidak</b> diisi otomatis dari periode berjalan. Layar pengelola menolak
	 * penyimpanan bila combobox tahun akademik belum dipilih, tetapi baris lama tetap bisa
	 * bernilai {@code null}.</p>
	 *
	 * @return tahun akademik dalam bentuk teks, atau {@code null}
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik penyelenggaraan gelombang.
	 *
	 * @param tahunAkademik tahun akademik dalam bentuk teks; {@code null} diterima
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan ruang tempat seminar diselenggarakan (label layar "Ruang").
	 *
	 * <p><b>Getter berefek samping (resolusi proxy).</b> Relasinya {@code LAZY}, sehingga getter
	 * memanggil {@link GeneralValueObject#check(Object)} milik induk lalu <b>menulis hasilnya
	 * kembali ke field</b>. {@code check()} dapat membuka dan menutup session Hibernate sendiri
	 * bila object diakses di luar session aslinya; inilah satu-satunya jalur akses database di
	 * class ini.</p>
	 *
	 * <p><b>Cakupan pemakaian yang perlu diketahui:</b> nilai ini hanya dibaca oleh layar
	 * pengelola gelombang itu sendiri (kolom grid "Ruang" dan form tambah/ubah). Tidak ada modul
	 * lain &mdash; termasuk {@link Skripsi}, {@link MahasiswaRequestTugasAkhir}, dan ketiga
	 * laporan seminar &mdash; yang mengambil ruang seminar dari sini. Bandingkan dengan sisi
	 * sidang, tempat {@code Skripsi.getRuangSidang()} benar-benar menyalin ruang dari
	 * {@link JadwalSidangTugasAkhir}.</p>
	 *
	 * @return ruang seminar yang sudah teresolusi, atau {@code null} bila belum ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang_seminar", nullable = true)
	public Ruang getRuangSeminar() {
		ruangSeminar = check(ruangSeminar);
		return ruangSeminar;
	}

	/**
	 * Menyetel ruang tempat seminar diselenggarakan.
	 *
	 * @param ruangSeminar ruang seminar; {@code null} untuk melepas
	 * @see #getRuangSeminar()
	 */
	public void setRuangSeminar(Ruang ruangSeminar) {
		this.ruangSeminar = ruangSeminar;
	}

	/**
	 * Mengembalikan kode program/jenjang penyelenggaraan (label layar "Program").
	 *
	 * <p>Isinya teks kode dari daftar {@code Common.initPrograms(...)}, bukan FK. Seperti
	 * fakultas dan prodi, nilai ini hanya dipakai sebagai kriteria pencarian
	 * ({@code Restrictions.eq("program", ...)}) di layar pengelola, tidak menegakkan pembatasan
	 * apa pun terhadap mahasiswa yang ditautkan.</p>
	 *
	 * @return kode program, atau {@code null} bila tidak dibatasi
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * Menyetel kode program/jenjang penyelenggaraan.
	 *
	 * @param program kode program; {@code null} berarti tidak dibatasi
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan <b>daftar agenda rinci</b> gelombang ini dalam bentuk string terpadat, sudah
	 * di-{@code trim} dan null-safe ke string kosong. Kolomnya bertipe {@code text}.
	 *
	 * <h3>Format</h3>
	 * <p>Satu nilai memuat <b>banyak record</b> yang dirangkai dengan pemisah {@code "||"}; tiap
	 * record memuat <b>empat ruas</b> yang dirangkai dengan pemisah {@code "<>"}:</p>
	 * <pre>
	 * nama&lt;&gt;tanggalMulai&lt;&gt;tanggalSampai&lt;&gt;keterangan
	 * </pre>
	 * <ol start="0">
	 *   <li><b>nama</b> &mdash; nama acara/agenda (kolom grid "Nama Acara / Jadwal"). Sekaligus
	 *       <b>kunci</b> record: {@link #populateJadwal(String, Date, Date, String)} dan
	 *       {@link #hapusJadwal(String)} mencocokkannya tanpa peduli besar-kecil huruf.</li>
	 *   <li><b>tanggalMulai</b> &mdash; waktu mulai acara, diformat
	 *       {@code Common.datetimeFormat1s} yaitu pola padat {@code "ddMMyyHHmmss"} (tahun dua
	 *       digit, tanpa pemisah).</li>
	 *   <li><b>tanggalSampai</b> &mdash; waktu selesai acara, format sama.</li>
	 *   <li><b>keterangan</b> &mdash; catatan bebas per agenda.</li>
	 * </ol>
	 *
	 * <h3>Kuirk yang perlu diketahui</h3>
	 * <ul>
	 *   <li><b>Pemisahnya rapuh.</b> Pemecahan memakai
	 *       {@code org.apache.commons.lang.StringUtils.split(teks, pemisah)} (commons-lang 2),
	 *       yang memperlakukan argumen kedua sebagai <b>himpunan karakter</b>, bukan string
	 *       pemisah utuh. Jadi {@code split(s, "||")} sebenarnya memecah pada karakter
	 *       {@code '|'} tunggal, dan {@code split(s, "<>")} memecah pada {@code '<'} maupun
	 *       {@code '>'}. Itulah sebabnya {@code populateJadwal} membersihkan {@code "||"} dan
	 *       {@code "<>"} dari masukan &mdash; tetapi karakter {@code |}, {@code <}, atau
	 *       {@code >} yang berdiri sendiri tetap merusak record.</li>
	 *   <li><b>Nama agenda kosong merusak record.</b> Karena {@code split} membuang pemisah di
	 *       awal dan menganggap pemisah berturut-turut sebagai satu, record yang ruas namanya
	 *       kosong akan <i>bergeser</i> saat dibaca ulang: tanggal mulai terbaca sebagai nama,
	 *       dan {@link #daftarJadwal()} kemudian membuang seluruh record itu karena penguraian
	 *       tanggalnya gagal. Layar pengelola tidak memvalidasi nama agenda per baris (hanya nama
	 *       gelombang), jadi agenda tanpa nama <b>hilang diam-diam</b> setelah disimpan.</li>
	 *   <li><b>Nama agenda bersifat unik.</b> Dua baris agenda bernama sama pada satu gelombang
	 *       akan saling menimpa &mdash; yang belakangan menggantikan yang lebih dulu, sehingga
	 *       barisnya berkurang tanpa peringatan.</li>
	 * </ul>
	 *
	 * <p>Getter ini murni: tidak menulis balik ke field. Jangan mengurai string ini sendiri
	 * &mdash; gunakan {@link #daftarJadwal()}.</p>
	 *
	 * @return string terpadat daftar agenda, atau string kosong; tidak pernah {@code null}
	 * @see #daftarJadwal()
	 * @see #populateJadwal(String, Date, Date, String)
	 * @see #hapusJadwal(String)
	 */
	@Column(name = "jadwal_rinci", columnDefinition = "text")
	public String getJadwalRinci() {
		return jadwalRinci == null ? "" : jadwalRinci.trim();
	}

	/**
	 * Menyetel string terpadat daftar agenda secara mentah, <b>tanpa validasi format apa pun</b>.
	 *
	 * <p>Dipakai layar pengelola untuk mengosongkan isi ({@code setJadwalRinci("")}) sebelum
	 * menulis ulang seluruh baris grid lewat
	 * {@link #populateJadwal(String, Date, Date, String)} saat penyimpanan. Untuk penambahan atau
	 * pengubahan satu agenda, selalu pakai {@code populateJadwal} agar formatnya terjaga.</p>
	 *
	 * @param jadwalRinci string terpadat sesuai format di {@link #getJadwalRinci()};
	 *                    {@code null} diterima
	 * @see #getJadwalRinci()
	 */
	public void setJadwalRinci(String jadwalRinci) {
		this.jadwalRinci = jadwalRinci;
	}

	/**
	 * Menambahkan <b>atau memperbarui</b> satu agenda pada daftar agenda rinci gelombang ini
	 * ("upsert" berdasarkan nama agenda).
	 *
	 * <p>Langkah kerjanya:</p>
	 * <ol>
	 *   <li>{@code nama} dan {@code keterangan} dibersihkan dari kedua pemisah ({@code "||"} dan
	 *       {@code "<>"} diganti spasi) lalu di-{@code trim}, agar tidak merusak format
	 *       terpadat;</li>
	 *   <li>record baru dirangkai sebagai
	 *       {@code nama<>tanggalMulai<>tanggalSampai<>keterangan} dengan tanggal diformat
	 *       {@code Common.datetimeFormat1s} ({@code "ddMMyyHHmmss"});</li>
	 *   <li>daftar lama diurai; record yang <b>namanya sama</b> (tanpa peduli besar-kecil huruf)
	 *       digantikan record baru, record lain disalin apa adanya;</li>
	 *   <li>bila tidak ada yang cocok, record baru ditambahkan di akhir.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> hasilnya ditulis <b>langsung ke field</b> {@code jadwalRinci}
	 * (bukan lewat setter), dan isi barunya di-{@code print} ke {@code System.out} &mdash; sisa
	 * kode penelusuran yang membanjiri log server setiap kali agenda diubah. Method ini hanya
	 * mengubah state di memori; penyimpanan ke database dilakukan pemanggil, yaitu layar
	 * pengelola lewat {@code Common.refreshUpdate(...)} pada tiap perubahan sel grid dan lewat
	 * {@code Common.refreshSaveOrUpdate(...)} saat tombol simpan ditekan.</p>
	 *
	 * <p><b>Batasan:</b> {@code nama} dan {@code keterangan} tidak boleh {@code null}
	 * ({@code trim()} atas hasil {@code StringUtils.replace} akan melempar
	 * {@code NullPointerException}), demikian pula kedua tanggal karena langsung diformat.
	 * Nama kosong diterima tetapi menghasilkan record cacat &mdash; lihat kuirk pada
	 * {@link #getJadwalRinci()}.</p>
	 *
	 * @param nama           nama acara/agenda; menjadi kunci pencocokan, tidak boleh {@code null}
	 * @param tanggalMulai   waktu mulai acara, tidak boleh {@code null}
	 * @param tanggalSampai  waktu selesai acara, tidak boleh {@code null}
	 * @param keterangan     catatan bebas agenda, tidak boleh {@code null} (boleh string kosong)
	 * @see #getJadwalRinci()
	 * @see #daftarJadwal()
	 * @see #hapusJadwal(String)
	 */
	public void populateJadwal(String nama, Date tanggalMulai, Date tanggalSampai, String keterangan) {
		String r = "";
		nama = org.apache.commons.lang3.StringUtils
				.replace(org.apache.commons.lang3.StringUtils.replace(nama, "||", " "), "<>", " ").trim();
		keterangan = org.apache.commons.lang3.StringUtils
				.replace(org.apache.commons.lang3.StringUtils.replace(keterangan, "||", " "), "<>", " ").trim();

		String gabungan = nama + "<>" + Common.datetimeFormat1s.get().format(tanggalMulai) + "<>"
				+ Common.datetimeFormat1s.get().format(tanggalSampai) + "<>" + keterangan;

		boolean ada = false;
		String[] spl = StringUtils.split(getJadwalRinci(), "||");
		for (String s : spl) {

			String[] subS = StringUtils.split(s, "<>");
			String n = subS.length > 0 ? subS[0].trim() : "";
			if (n.equalsIgnoreCase(nama)) {
				r += r.isEmpty() ? gabungan : "||" + gabungan;
				ada = true;
			} else {
				r += r.isEmpty() ? s : "||" + s;
			}
		}

		if (!ada) {
			r += r.isEmpty() ? gabungan : "||" + gabungan;
		}

		jadwalRinci = r;
		System.out.println("jadwalRinci -> " + jadwalRinci);
	}

	/**
	 * Menghapus agenda dengan nama tertentu dari daftar agenda rinci.
	 *
	 * <p>Argumen dibersihkan dari pemisah dengan cara yang sama seperti
	 * {@link #populateJadwal(String, Date, Date, String)} agar cocok dengan bentuk nama yang
	 * benar-benar tersimpan, lalu seluruh record yang namanya sama (tanpa peduli besar-kecil
	 * huruf) dibuang dan sisanya dirangkai ulang. Hasilnya ditulis <b>langsung ke field</b>
	 * {@code jadwalRinci}; bila tidak ada yang cocok, isinya tetap sama (hanya dirangkai ulang).</p>
	 *
	 * <p>Dipanggil dari tombol "Hapus" pada tiap baris grid agenda di layar pengelola, dan hanya
	 * bila gelombangnya sudah tersimpan ({@code getId() != null}); penyimpanan ke database
	 * dilakukan pemanggil lewat {@code Common.refreshUpdate(...)}. Untuk baris grid yang belum
	 * pernah tersimpan, tombol hapus cukup melepas baris dari layar.</p>
	 *
	 * @param nama nama agenda yang akan dihapus; tidak boleh {@code null} (akan melempar
	 *             {@code NullPointerException})
	 * @see #populateJadwal(String, Date, Date, String)
	 * @see #daftarJadwal()
	 */
	public void hapusJadwal(String nama) {
		String r = "";
		nama = org.apache.commons.lang3.StringUtils
				.replace(org.apache.commons.lang3.StringUtils.replace(nama, "||", " "), "<>", " ").trim();

		String[] spl = StringUtils.split(getJadwalRinci(), "||");
		for (String s : spl) {

			String[] subS = StringUtils.split(s, "<>");
			String n = subS.length > 0 ? subS[0].trim() : "";
			if (!n.equalsIgnoreCase(nama)) {
				r += r.isEmpty() ? s : "||" + s;
			}
		}

		jadwalRinci = r;
	}

	/**
	 * Mengurai {@link #getJadwalRinci()} menjadi daftar agenda yang siap dipakai layar dan
	 * laporan. <b>Inilah satu-satunya cara yang benar untuk membaca daftar agenda</b>; jangan
	 * mengurai string terpadatnya sendiri.
	 *
	 * <p>Tiap elemen hasil adalah {@code Object[]} berukuran empat:</p>
	 * <ol start="0">
	 *   <li>{@code String} nama acara/agenda;</li>
	 *   <li>{@code Date} waktu mulai, atau {@code null} bila ruasnya tidak ada;</li>
	 *   <li>{@code Date} waktu selesai, atau {@code null} bila ruasnya tidak ada;</li>
	 *   <li>{@code String} keterangan, atau string kosong.</li>
	 * </ol>
	 *
	 * <p><b>Ketahanan terhadap data rusak:</b> penguraian tiap record dibungkus
	 * {@code try/catch}. Record yang tanggalnya tidak dapat diurai <b>dibuang diam-diam</b> dari
	 * hasil (jejaknya hanya berupa {@code stack trace} dan catatan
	 * {@code ais.common.ErrorAuditUtil}), sehingga agenda cacat tampak "hilang" di layar padahal
	 * datanya masih ada di kolom. Method ini <b>tidak</b> mengubah state entity.</p>
	 *
	 * <p>Dipanggil saat merender grid agenda di layar pengelola dan saat menyusun ulang isi grid
	 * sebelum penyimpanan.</p>
	 *
	 * @return daftar agenda; kosong bila kolom {@code jadwal_rinci} kosong. Tidak pernah
	 *         {@code null}
	 * @see #getJadwalRinci()
	 * @see #populateJadwal(String, Date, Date, String)
	 */
	public List<Object[]> daftarJadwal() {
		List<Object[]> list = new ArrayList<Object[]>();
		String[] spl = StringUtils.split(getJadwalRinci(), "||");
		for (String s : spl) {
			try {
				String[] subS = StringUtils.split(s, "<>");
				String n = subS.length > 0 ? subS[0].trim() : "";
				Date tanggalMulai = subS.length > 1 ? Common.datetimeFormat1s.get().parse(subS[1].trim()) : null;
				Date tanggalSampai = subS.length > 2 ? Common.datetimeFormat1s.get().parse(subS[2].trim()) : null;
				String keterangan = subS.length > 3 ? subS[3].trim() : "";
				list.add(new Object[] { n, tanggalMulai, tanggalSampai, keterangan });
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/JadwalSeminarTugasAkhir.java:276");
			}
		}
		return list;
	}
}
