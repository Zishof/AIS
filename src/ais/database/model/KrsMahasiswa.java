package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.Date;
import java.util.Map;

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
import org.json.JSONObject;

import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.IndonesianNumberToWords;

/**
 * Entity tabel {@code public.krs_mahasiswa}: <b>kepala (header) Kartu Rencana Studi</b> seorang
 * mahasiswa untuk satu kombinasi semester.
 *
 * <h3>Apa yang diwakili satu baris</h3>
 * <p>Satu baris {@code krs_mahasiswa} = <b>satu mahasiswa &times; satu semester &times; satu
 * tahapan &times; satu penanda semester pendek</b>. Kombinasi itulah yang dirangkai menjadi kunci
 * alami {@link #getKodeUnik()} ({@code idMahasiswa-semester-tahapan-semesterPendek}) dan dipetakan
 * sebagai kolom {@code unique}. Baris ini <b>bukan</b> pendaftaran ke satu kelas/{@link Perkuliahan}
 * tertentu.</p>
 *
 * <p><b>Koreksi asumsi yang sering keliru.</b> Pengambilan mata kuliah per baris KRS — mahasiswa
 * mengambil mata kuliah X di kelas Y — disimpan di {@link Detailperkuliahan}, bukan di sini. Status
 * <i>persetujuan dosen PA</i> juga tinggal di sana ({@code Detailperkuliahan.getPersetujuan()},
 * nilai {@code BELUM_DISETUJUI}/{@code DISETUJUI}) — kelas ini <b>tidak punya</b> field
 * approve/reject sama sekali. Yang ada di sini hanya {@link #getDikunci()} (pengguna yang mengunci
 * kepala KRS) dan {@link #getAktif()}. Jadi alur akademiknya:</p>
 * <ol>
 *   <li>mahasiswa memilih mata kuliah &rarr; baris-baris {@link Detailperkuliahan} dibuat;</li>
 *   <li>dosen PA menyetujui/menolak per baris {@link Detailperkuliahan};</li>
 *   <li>baris {@code krs_mahasiswa} ini <b>merangkum</b> hasilnya (SKS diambil, SKS kumulatif,
 *       IPS, IPK, dosen PA, kelas, jumlah komentar) supaya layar rekap/KHS/laporan tidak perlu
 *       menghitung ulang dari nol setiap kali;</li>
 *   <li>mahasiswa yang bersangkutan menjadi peserta resmi {@link Perkuliahan} lewat
 *       {@link Detailperkuliahan}, bukan lewat entity ini.</li>
 * </ol>
 *
 * <h3>Peran ganda: rekap KRS sekaligus induk pertemuan bimbingan</h3>
 * <p>Kelas ini {@code extends} {@link VOPembelajaran} dan {@code implements}
 * {@link VOPesertaPembelajaran}. Alasannya bukan karena KRS adalah "mata kuliah", melainkan karena
 * tabel {@code pertemuan} dipakai bersama oleh 16 jenis induk dan salah satunya adalah
 * <b>bimbingan/konsultasi dengan dosen PA</b> yang digantung pada kepala KRS
 * ({@code Pertemuan.getKrsMahasiswa()}). Field {@link #getJenis()},
 * {@link #getTanggalAwalBimbingan()}, {@link #getLewatiTanggalMerahNasional()},
 * {@link #getCourse()}, {@link #getUrutkanotomatis()}, {@link #getNoSk()} dan {@link #getTglSk()}
 * melayani peran kedua ini (dibaca/ditulis oleh {@code PenjadwalanHelper} saat membangkitkan jadwal
 * pertemuan bimbingan), bukan peran rekap nilai.</p>
 *
 * <h3>Siapa yang mengisi baris ini</h3>
 * <p>Hampir tidak pernah dibuat langsung oleh layar. Titik pusatnya:</p>
 * <ul>
 *   <li>{@code KrsDanSkripsiHelper.singkronkanKrsMahasiswa(...)} (dipanggil lewat fasad
 *       {@code Common.singkronkanKrsMahasiswa(...)}) — menghitung ulang seluruh angka rekap dari
 *       {@link Detailperkuliahan} terbaru lalu {@code saveOrUpdate} baris ini, dengan cache JSON
 *       sementara berkunci {@code "KrsMahasiswa_" + idMahasiswa + "-" + semester + "-" + tahapan +
 *       "-" + semesterPendek}. Semua setter SKS/IPK/IPS di kelas ini praktis hanya dipanggil dari
 *       sana;</li>
 *   <li>{@code KrsDanSkripsiHelper.ambilKrsMahasiswaTanpaSinkronisasi(...)} — jalur baca-saja untuk
 *       render layar, sengaja tidak menulis apa pun;</li>
 *   <li>{@link Mahasiswa#ambilDefaultKrsMahasiswa(Integer, Integer, Integer,
 *       org.hibernate.Session)} — pencarian baris berdasarkan {@code kodeUnik}, dengan fallback
 *       pencarian per kolom bila {@code kodeUnik} lama belum terisi.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #getNama()},
 *       {@link #getKodeUnik()}, {@link #generateKodeUnik(Mahasiswa, Integer, Integer, Integer)},
 *       {@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()}, {@link #onUpdate()},
 *       {@link #toString()}.</li>
 *   <li><b>Kunci komposit semester</b> — {@link #getSemester()}, {@link #getTahapan()},
 *       {@link #getSemesterPendek()}, {@link #getTahunAkademik()}.</li>
 *   <li><b>Relasi</b> — {@link #getMahasiswa()}, {@link #getDosenPa()}, {@link #getDikunci()}.</li>
 *   <li><b>Angka rekap hasil sinkronisasi</b> — {@link #getSksYangDiambil()}, {@link #getSksk()},
 *       {@link #getSksYangDiambilLulus()}, {@link #getSkskLulus()}, {@link #getSksKonversi()},
 *       {@link #getSksBukanKonversi()}, {@link #getIps()}, {@link #getIpk()},
 *       {@link #getKomentars()}, plus jejak id mentahnya {@link #getSksYangDiambilS()} dan
 *       {@link #getSkskS()}.</li>
 *   <li><b>Atribut administratif</b> — {@link #getKelas()}, {@link #getCatatan()},
 *       {@link #getCatatanKhs()}, {@link #getKeterangan()}, {@link #getNoUts()},
 *       {@link #getNoUas()}, {@link #getAktif()}, {@link #getFeeder()}.</li>
 *   <li><b>Peran induk pertemuan bimbingan</b> — {@link #getJenis()},
 *       {@link #getTanggalAwalBimbingan()}, {@link #getLewatiTanggalMerahNasional()},
 *       {@link #getNoSk()}, {@link #getTglSk()}, {@link #getCourse()},
 *       {@link #getUrutkanotomatis()}, serta implementasi kontrak
 *       {@link #ambilTahunAkademik()}, {@link #ambilSemester()}, {@link #ambilJenisSemester()},
 *       {@link #ambilVOPembelajaran()}, {@link #ambilJumlahDetailperkuliahanLangsung()}.</li>
 *   <li><b>Laporan</b> — {@link #parameterData(Mahasiswa, Integer, Boolean, Map)} (statis, mengisi
 *       parameter Jasper untuk transkrip/ijazah/rekaman nilai).</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 *   <li><b>Banyak getter TIDAK murni.</b> {@link #getNama()}, {@link #getKodeUnik()},
 *       {@link #getTahunAkademik()}, {@link #getNoUts()}, {@link #getNoUas()},
 *       {@link #getDosenPa()} dan {@link #getKelas()} menulis ulang field yang mereka kembalikan.
 *       Karena semuanya properti ter-<i>map</i> Hibernate, sekadar merender layar dapat membuat
 *       entity kotor dan memicu {@code UPDATE} pada saat flush. {@link #getKelas()} bahkan menulis
 *       ke entity LAIN ({@code mahasiswa.setKelas(...)}) dan memanggil
 *       {@code Common.singkronkanKrsMahasiswa} untuk semester-semester sebelumnya — satu pembacaan
 *       properti bisa menjelma menjadi rentetan query. {@link #getDosenPa()} dan
 *       {@link #setDosenPa(Dosen)} juga menulis berkas cermin lewat
 *       {@link GeneralValueObject#put(String, String)}.</li>
 *   <li><b>Field audit dan identitas di-<i>shadow</i>.</b> {@code id}, {@code nama},
 *       {@code keterangan}, {@code oleh}, {@code olehId} dan {@code tanggal_dirubah} dideklarasikan
 *       ulang di kelas ini padahal {@link GeneralValueObject} sudah punya field bernama sama. Field
 *       milik induk menjadi tidak terpakai untuk entity ini (induk bukan
 *       {@code @MappedSuperclass}); ini pola yang sama di seluruh entity AIS. Konsekuensinya: jangan
 *       mengharapkan {@code super.getOleh()} dan {@code this.getOleh()} bernilai sama.</li>
 *   <li><b>Nama kolom warisan yang menyesatkan.</b> {@link #getSksKonversi()} dipetakan ke kolom
 *       {@code mkbelumdiniali} (perhatikan salah ketiknya) dan {@link #getSksBukanKonversi()} ke
 *       {@code mkkbelumdinilai}. Kolom lama "mata kuliah belum dinilai" didaur ulang untuk arti yang
 *       sama sekali berbeda; jangan menyimpulkan makna kolom dari namanya di basis data.</li>
 *   <li><b>{@code sksYangDiambilS}/{@code skskS} adalah CSV id mentah.</b> Isinya daftar id
 *       {@link Detailperkuliahan} dipisah koma, dan pemakainya
 *       ({@code PenilaianUtil.downloadSemuaKRS}) menyisipkannya apa adanya ke
 *       {@code Restrictions.sqlRestriction("id in (" + sks + ")")}. Aman selama isinya tetap
 *       dihasilkan mesin; jangan pernah mengisinya dari masukan pengguna.</li>
 *   <li><b>Kontrak dasar diwarisi.</b> Perilaku {@code equals}/{@code hashCode}/{@code compareTo},
 *       resolusi proxy lazy {@code check(...)}, serta cermin berkas {@code put}/{@code retreive}
 *       didefinisikan di {@link GeneralValueObject} — baca di sana, jangan diduplikasi di sini.
 *       {@link VOPembelajaran} menambahkan lapisan pertemuan/dosen ({@code populateDosenBuNama()},
 *       {@code infoSimple()}, {@code toIdSmt()}).</li>
 *   <li><b>Field {@code berubah} tidak dipakai.</b> {@code public transient boolean berubah}
 *       dideklarasikan tetapi tidak pernah dibaca maupun ditulis di mana pun pada pohon sumber ini
 *       (sisa refaktor lama). Dibiarkan apa adanya agar serialisasi/kompatibilitas tidak
 *       terganggu.</li>
 * </ul>
 *
 * @see Mahasiswa
 * @see Perkuliahan
 * @see Detailperkuliahan
 * @see VOPembelajaran
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "krs_mahasiswa")
public class KrsMahasiswa extends VOPembelajaran implements VOPesertaPembelajaran {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code krs_mahasiswa} (identity, dibangkitkan basis data). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit sederhana). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit sederhana). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris KRS ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong <b>diabaikan diam-diam</b>
	 * sehingga jejak audit yang sudah ada tidak terhapus oleh jalur simpan tanpa sesi login
	 * (batch/penjadwal).
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong.
	 * @see GeneralValueObject#setOlehId(String)
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penjagaan yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris KRS ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} JPA: mendelegasikan pengisian jejak audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>Implementasi wajib dari satu-satunya method abstrak {@link GeneralValueObject}. Tidak
	 * dipanggil manual — hanya oleh penyedia persistence.</p>
	 *
	 * @see GeneralValueObject#onUpdate()
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object memakai jam server
	 * aplikasi ({@code WaktuUtil.getDate()}), bukan jam basis data.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (disimpan lengkap sampai jam/menit/detik).
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris KRS: {@code "<id>-<nama>"}.
	 *
	 * <p><b>Catatan:</b> memakai <b>field</b> {@code nama} apa adanya, bukan {@link #getNama()},
	 * sehingga aman dipanggil pada object yang relasi {@code mahasiswa}-nya belum terisi (tidak
	 * memicu NPE maupun pemuatan lazy). Nilai bisa berbunyi {@code "null-null"} untuk object yang
	 * belum tersimpan.</p>
	 *
	 * @return gabungan id dan nama KRS.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Pengguna yang sedang mengunci kepala KRS ini; {@code null} berarti tidak terkunci. */
	private Tbmuser dikunci;

	/**
	 * Pengguna yang <b>mengunci</b> kepala KRS ini.
	 *
	 * <p>Implementasi kontrak {@link VoKunci#getDikunci()}. Kunci dipakai layar KRS untuk membekukan
	 * pengisian (mis. setelah masa KRS ditutup atau setelah dosen PA memfinalkan). Berbeda dengan
	 * {@code Perkuliahan.getDikunci()} yang bisa membatalkan kunci sendiri, getter di sini murni:
	 * hanya meresolusi proxy lazy lewat {@link GeneralValueObject#check(Object)} lalu
	 * mengembalikannya.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila KRS tidak terkunci.
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menyetel (atau melepas dengan {@code null}) pengguna pengunci kepala KRS.
	 *
	 * @param dikunci pengguna pengunci; {@code null} berarti membuka kunci.
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/** Label KRS; diisi ulang otomatis oleh {@link #getNama()} dari NIM/nama mahasiswa + semester. */
	private String nama;
	/** Keterangan bebas untuk baris KRS ini. */
	private String keterangan;
	/** Mahasiswa pemilik KRS; bagian pertama kunci alami {@link #getKodeUnik()}. */
	private Mahasiswa mahasiswa;
	/** Tahun akademik bentuk {@code "2023/2024"}; dihitung ulang oleh {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/** Semester ke berapa bagi mahasiswa ini (1, 2, 3, ...), bukan kode semester nasional. */
	private Integer semester;
	/** Tahapan studi (untuk program bertahap); nilai 0 diperlakukan sama dengan {@code null}. */
	private Integer tahapan;
	// private Integer maksSks;
	/** Total SKS yang diambil pada semester ini. Hasil hitung {@code singkronkanKrsMahasiswa}. */
	private Integer sksYangDiambil;
	/** Total SKS kumulatif sampai dengan semester ini. Hasil hitung {@code singkronkanKrsMahasiswa}. */
	private Integer sksk;

	/** Bagian {@link #sksYangDiambil} yang berstatus lulus. */
	private Integer sksYangDiambilLulus;
	/** Bagian {@link #sksk} kumulatif yang berstatus lulus. */
	private Integer skskLulus;

	/** Daftar id {@link Detailperkuliahan} semester ini, dipisah koma. Jejak asal {@link #sksYangDiambil}. */
	private String sksYangDiambilS;
	/** Daftar id {@link Detailperkuliahan} kumulatif, dipisah koma. Jejak asal {@link #sksk}. */
	private String skskS;

	/** SKS hasil konversi/transfer pada semester ini (kolom warisan {@code mkbelumdiniali}). */
	private Integer sksKonversi;
	/** SKS murni (bukan konversi) pada semester ini (kolom warisan {@code mkkbelumdinilai}). */
	private Integer sksBukanKonversi;
	// private Integer mkDinilai;
	// private Integer mkkDinilai;
	/** Penanda semester pendek; bagian keempat kunci alami {@link #getKodeUnik()}. */
	private Integer semesterPendek;
	// private Integer selisih;
	// private Double iplast;
	/** Indeks prestasi kumulatif sampai semester ini. */
	private Double ipk;
	/** Indeks prestasi semester ini saja. */
	private Double ips;
	// private Double minip;
	/** Catatan bebas pada kartu rencana studi (KRS). */
	private String catatan;
	/** Catatan bebas pada kartu hasil studi (KHS). */
	private String catatanKhs;

	/** Nomor peserta UTS; dibangkitkan sekali oleh {@link #getNoUts()}. */
	private String noUts;
	/** Nomor peserta UAS; dibangkitkan sekali oleh {@link #getNoUas()}. */
	private String noUas;

	/** Kunci alami baris ini; selalu dihitung ulang oleh {@link #getKodeUnik()}. */
	private String kodeUnik;

	// private String krs;

	/** Jumlah komentar/konsultasi KRS pada semester ini (hasil {@code Common.loadKomentarUkuran}). */
	private Integer komentars;
	/** Dosen pembimbing akademik (PA) yang menangani KRS semester ini. */
	private Dosen dosenPa;
	/** Kelas/rombongan belajar mahasiswa pada semester ini. */
	private String kelas;

	// private String belumDinilai;
	// private String telahDinilai;

	/**
	 * Penanda sementara "baris ini berubah".
	 *
	 * <p><b>Tidak dipakai:</b> tidak ada satu pun pembaca maupun penulis field ini di seluruh pohon
	 * sumber (sisa refaktor lama). Dibiarkan agar biner/serialisasi lama tetap kompatibel.</p>
	 */
	public transient boolean berubah = false;

	/** Penanda baris KRS aktif; {@code null} dibaca sebagai {@code true} oleh {@link #getAktif()}. */
	private Boolean aktif;
	/** Tanggal mulai bimbingan/konsultasi PA; dipakai pembangkit jadwal pertemuan. */
	private Date tanggalAwalBimbingan;
	/** Pola pengulangan pertemuan bimbingan (bawaan {@code "Mingguan"}). */
	private String jenis;
	/** Bila {@code true}, pembangkit jadwal melewati tanggal merah nasional. */
	private Boolean lewatiTanggalMerahNasional;
	/** {@code id_aktivitas} milik PDDikti Feeder untuk baris aktivitas kuliah ini. */
	private String feeder;
	/** Nomor SK penugasan bimbingan. */
	private String noSk;
	/** Tanggal SK penugasan bimbingan. */
	private Date tglSk;

//	private String statusKrs;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Semua field dibiarkan pada nilai bawaan;
	 * pengisian normalnya dilakukan {@code KrsDanSkripsiHelper.singkronkanKrsMahasiswa(...)}.
	 */
	public KrsMahasiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris KRS.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan.
	 * @see GeneralValueObject#getId()
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris KRS. Normalnya hanya dipanggil Hibernate.
	 *
	 * @param id nilai primary key baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Label baris KRS berbentuk {@code "<NIM>-<nama mahasiswa>-<semester>"}.
	 *
	 * <p><b>Getter tidak murni:</b> nilai field {@code nama} selalu <b>ditulis ulang</b> dari relasi
	 * mahasiswa setiap kali dipanggil, sehingga membaca properti ini pada entity managed dapat
	 * membuatnya kotor dan memicu {@code UPDATE} saat flush.</p>
	 *
	 * <p><b>Jebakan:</b> method ini menyentuh <b>field</b> {@code mahasiswa} secara langsung, bukan
	 * {@link #getMahasiswa()}. Akibatnya (a) tidak ada resolusi proxy lazy lewat
	 * {@code check(...)}, dan (b) baris KRS yang relasi mahasiswanya belum diisi akan melempar
	 * {@code NullPointerException}. Pemanggil pada jalur pembuatan object baru harus mengisi
	 * {@link #setMahasiswa(Mahasiswa)} lebih dulu.</p>
	 *
	 * @return label KRS yang sudah di-{@code trim}, atau {@code null} bila hasil perakitan kosong.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		nama = mahasiswa.getNim() + "-" + mahasiswa.getNama() + "-" + getSemester();
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel label KRS secara manual. Nilai ini tidak bertahan: {@link #getNama()} akan
	 * menimpanya pada pembacaan berikutnya.
	 *
	 * @param nama label KRS.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Tanggal mulai bimbingan/konsultasi dengan dosen PA untuk semester ini.
	 *
	 * <p>Dibaca {@code PenjadwalanHelper} sebagai titik awal saat membangkitkan deretan
	 * {@link Pertemuan} bimbingan yang bergantung pada kepala KRS ini.</p>
	 *
	 * @return tanggal awal bimbingan, atau {@code null} bila belum dijadwalkan.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalAwalBimbingan() {
		return tanggalAwalBimbingan;
	}

	/**
	 * Menyetel tanggal mulai bimbingan PA.
	 *
	 * @param tanggalAwalBimbingan tanggal awal bimbingan; boleh {@code null}.
	 */
	public void setTanggalAwalBimbingan(Date tanggalAwalBimbingan) {
		this.tanggalAwalBimbingan = tanggalAwalBimbingan;
	}

	/**
	 * Pola pengulangan pertemuan bimbingan PA (mis. {@code "Mingguan"}, {@code "Harian"}).
	 *
	 * <p>Nilai {@code null} dinormalkan menjadi {@code "Mingguan"} supaya radio pilihan pola di
	 * layar penjadwalan selalu punya nilai terpilih.</p>
	 *
	 * @return pola pengulangan; tidak pernah {@code null}.
	 */
	public String getJenis() {
		return jenis == null ? "Mingguan" : jenis;
	}

	/**
	 * Menyetel pola pengulangan pertemuan bimbingan.
	 *
	 * @param jenis label pola sesuai daftar pilihan di layar penjadwalan.
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Apakah pembangkit jadwal bimbingan harus melewati tanggal merah nasional.
	 *
	 * <p>Nilai {@code null} dinormalkan menjadi {@code true} (melewati hari libur adalah perilaku
	 * bawaan untuk data lama yang belum punya kolom ini).</p>
	 *
	 * @return {@code true} bila hari libur nasional dilewati.
	 */
	public Boolean getLewatiTanggalMerahNasional() {
		return lewatiTanggalMerahNasional == null ? true : lewatiTanggalMerahNasional;
	}

	/**
	 * Menyetel penanda "lewati tanggal merah nasional".
	 *
	 * @param lewatiTanggalMerahNasional {@code true} untuk melewati hari libur nasional.
	 */
	public void setLewatiTanggalMerahNasional(Boolean lewatiTanggalMerahNasional) {
		this.lewatiTanggalMerahNasional = lewatiTanggalMerahNasional;
	}

	/**
	 * Keterangan bebas untuk baris KRS ini.
	 *
	 * @return keterangan apa adanya (tidak dinormalkan), atau {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mahasiswa pemilik KRS ini (relasi wajib, kolom {@code mahasiswa} {@code NOT NULL}).
	 *
	 * <p>Meresolusi proxy lazy lewat {@link GeneralValueObject#check(Object)} dan
	 * <b>menugaskan kembali hasilnya ke field</b>, karena {@code check} dapat mengembalikan instance
	 * kanonik yang berbeda dari proxy semula.</p>
	 *
	 * @return mahasiswa pemilik KRS; {@code null} hanya pada object yang belum diisi.
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa pemilik KRS.
	 *
	 * <p>Sering dipakai jalur baca-saja untuk <b>menukar</b> instance hasil query dengan instance
	 * milik layar, supaya object KRS tetap berguna setelah session pembacanya ditutup (lihat
	 * {@code KrsDanSkripsiHelper.ambilKrsMahasiswaTanpaSinkronisasi}).</p>
	 *
	 * @param mahasiswa mahasiswa pemilik KRS.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Semester keberapa KRS ini bagi mahasiswa yang bersangkutan (1, 2, 3, ...).
	 *
	 * <p>Angka ini relatif terhadap mahasiswa, bukan kode semester nasional; ganjil/genapnya
	 * diturunkan di {@link #ambilJenisSemester()} dan tahun akademiknya di
	 * {@link #getTahunAkademik()}.</p>
	 *
	 * @return nomor semester, atau {@code null} bila belum diisi.
	 */
	public Integer getSemester() {
		return semester;
	}

	/**
	 * Menyetel nomor semester KRS.
	 *
	 * @param semester nomor semester (1, 2, 3, ...).
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	// public Integer getMaksSks() {
	// if (maksSks == null) {
	// maksSks = 0;
	// }
	// return maksSks;
	// }
	//
	// public void setMaksSks(Integer maksSks) {
	// this.maksSks = maksSks;
	// }

	/**
	 * Total SKS yang diambil mahasiswa pada semester ini.
	 *
	 * <p>Bukan hasil hitung on-the-fly: nilainya ditulis oleh
	 * {@code KrsDanSkripsiHelper.singkronkanKrsMahasiswa} dari
	 * {@code Mahasiswa.prosesHitungSks(detailSemesterIni, null, true)}. Getter ini hanya
	 * menormalkan {@code null} menjadi {@code 0} — dan penormalan itu ditulis balik ke field,
	 * sehingga baris lama bernilai {@code NULL} bisa ter-{@code UPDATE} menjadi {@code 0} saat
	 * flush berikutnya.</p>
	 *
	 * @return jumlah SKS semester ini; tidak pernah {@code null}.
	 */
	public Integer getSksYangDiambil() {
		if (sksYangDiambil == null) {
			sksYangDiambil = 0;
		}
		return sksYangDiambil;
	}

	/**
	 * Menyetel total SKS semester ini. Dipanggil dari jalur sinkronisasi KRS.
	 *
	 * @param sksYangDiambil jumlah SKS hasil hitung ulang.
	 */
	public void setSksYangDiambil(Integer sksYangDiambil) {
		this.sksYangDiambil = sksYangDiambil;
	}

	// public Integer getSelisih() {
	// selisih = getMaksSks() - getSksYangDiambil();
	// return selisih;
	// }
	//
	// public void setSelisih(Integer selisih) {
	// this.selisih = selisih;
	// }

	// public Double getIplast() {
	// if (iplast == null) {
	// iplast = 0.0;
	// }
	// return iplast;
	// }
	//
	// public void setIplast(Double iplast) {
	// this.iplast = iplast;
	// }

	// public Double getMinip() {
	// return minip;
	// }
	//
	// public void setMinip(Double minip) {
	// this.minip = minip;
	// }

	/**
	 * Catatan bebas yang dicetak pada kartu rencana studi (KRS).
	 *
	 * @return catatan yang sudah di-{@code trim}; string kosong bila belum diisi (tidak pernah
	 *         {@code null}, agar aman langsung dirangkai di laporan).
	 */
	@Column(name = "catatan", nullable = true, columnDefinition = "text")
	public String getCatatan() {
		return catatan == null ? "" : catatan.trim();
	}

	/**
	 * Menyetel catatan KRS.
	 *
	 * @param catatan teks catatan; boleh {@code null}.
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Nomor peserta ujian tengah semester (UTS) mahasiswa ini.
	 *
	 * <p><b>Getter tidak murni — membangkitkan nilai sekali jalan.</b> Bila kolom masih kosong,
	 * nomor dirakit saat itu juga sebagai {@code yyMM} (dua digit tahun + dua digit bulan saat
	 * pemanggilan, lihat {@code Common.simpleDateFormat}) disusul <b>6 digit terakhir</b> dari
	 * {@code Common.randLong()} yang sudah dipadatkan dengan nol di depan. Nilai langsung ditulis ke
	 * field, jadi entity managed menjadi kotor dan nomornya ikut tersimpan pada flush berikutnya —
	 * inilah cara nomor ujian "terbit" tanpa aksi eksplisit dari pengguna.</p>
	 *
	 * <p><b>Kuirk:</b> kolom ini tidak {@code unique} dan nomor dibangkitkan acak, sehingga tabrakan
	 * nomor antar mahasiswa dalam bulan yang sama secara teoretis mungkin terjadi (ruang 10<sup>6</sup>
	 * per bulan) dan tidak dideteksi kode ini.</p>
	 *
	 * @return nomor peserta UTS; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public String getNoUts() {
		if (noUts == null || noUts.trim().isEmpty()) {
			String str = "00000000000000000000" + Common.randLong();
			noUts = Common.simpleDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())
					+ str.substring(str.length() - 6);
		}
		return noUts;
	}

	/**
	 * Menyetel nomor peserta UTS secara manual (mis. saat mengikuti penomoran dari sistem lain).
	 *
	 * @param noUts nomor peserta UTS.
	 */
	public void setNoUts(String noUts) {
		this.noUts = noUts;
	}

	/**
	 * Nomor peserta ujian akhir semester (UAS) mahasiswa ini.
	 *
	 * <p>Perilaku identik dengan {@link #getNoUts()} — dibangkitkan sekali jalan bila masih kosong,
	 * dengan format dan kuirk yang sama.</p>
	 *
	 * @return nomor peserta UAS; tidak pernah {@code null} setelah pemanggilan pertama.
	 * @see #getNoUts()
	 */
	public String getNoUas() {
		if (noUas == null || noUas.trim().isEmpty()) {
			String str = "00000000000000000000" + Common.randLong();
			noUas = Common.simpleDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())
					+ str.substring(str.length() - 6);
		}
		return noUas;
	}

	/**
	 * Menyetel nomor peserta UAS secara manual.
	 *
	 * @param noUas nomor peserta UAS.
	 */
	public void setNoUas(String noUas) {
		this.noUas = noUas;
	}

	/**
	 * Tahapan studi baris KRS ini (dipakai program yang membagi masa studi menjadi beberapa tahap).
	 *
	 * <p>Nilai {@code 0} dinormalkan menjadi {@code null} — sepanjang alur KRS, "tahapan 0" dan
	 * "tanpa tahapan" adalah hal yang sama, dan penyeragaman ini penting karena {@code tahapan} ikut
	 * masuk ke kunci alami {@link #getKodeUnik()} serta ke kriteria pencarian
	 * {@link Mahasiswa#ambilDefaultKrsMahasiswa(Integer, Integer, Integer,
	 * org.hibernate.Session)}.</p>
	 *
	 * @return nomor tahapan, atau {@code null} bila tanpa tahapan (termasuk bila tersimpan
	 *         {@code 0}).
	 */
	public Integer getTahapan() {
		return tahapan == null || tahapan.equals(0) ? null : tahapan;
	}

	/**
	 * Menyetel tahapan studi. Nilai {@code 0} tetap tersimpan apa adanya di field; penormalan
	 * dilakukan di {@link #getTahapan()}.
	 *
	 * @param tahapan nomor tahapan; boleh {@code null}.
	 */
	public void setTahapan(Integer tahapan) {
		this.tahapan = tahapan;
	}

	/**
	 * Merakit kunci alami baris KRS dari komponen-komponennya.
	 *
	 * <p>Bentuknya {@code "<idMahasiswa>-<semester>-<tahapan>-<semesterPendek>"}, atau
	 * {@code "<idMahasiswa>-<semester>-<tahapan>"} bila {@code semesterPendek} bernilai
	 * {@code null}. Perhatikan bahwa hanya {@code semesterPendek} yang mengubah <b>bentuk</b>
	 * kunci; {@code semester} dan {@code tahapan} yang {@code null} tetap ikut dirangkai dan
	 * menghasilkan literal {@code "null"} di dalam kunci (mis. {@code "12-3-null"}) — itu memang
	 * bentuk yang dipakai baris tanpa tahapan, jadi jangan "diperbaiki" tanpa migrasi data.</p>
	 *
	 * <p>Dipakai bersama oleh {@link #getKodeUnik()} dan
	 * {@link Mahasiswa#ambilDefaultKrsMahasiswa(Integer, Integer, Integer,
	 * org.hibernate.Session)} supaya pencarian dan penyimpanan memakai kunci yang identik. Method
	 * murni: tidak menyentuh basis data maupun berkas.</p>
	 *
	 * @param mahasiswa      mahasiswa pemilik KRS; bila {@code null} kunci tidak dapat dibentuk.
	 * @param semester       nomor semester.
	 * @param tahapan        nomor tahapan (sudah dinormalkan pemanggil, biasanya lewat
	 *                       {@link #getTahapan()}).
	 * @param semesterPendek penanda semester pendek; {@code null} memendekkan bentuk kunci.
	 * @return kunci alami, atau {@code null} bila {@code mahasiswa} {@code null}.
	 */
	public static String generateKodeUnik(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek) {
		String kodeUnik;
		if (mahasiswa != null && semesterPendek != null) {
			kodeUnik = mahasiswa.getId() + "-" + semester + "-" + tahapan + "-" + semesterPendek;
		} else if (mahasiswa != null) {
			kodeUnik = mahasiswa.getId() + "-" + semester + "-" + tahapan;
		} else {
			kodeUnik = null;
		}
		return kodeUnik;
	}

	/**
	 * Kunci alami baris KRS, kolom {@code unique} yang menjamin satu mahasiswa hanya punya satu
	 * kepala KRS per kombinasi semester/tahapan/semester pendek.
	 *
	 * <p><b>Getter tidak murni:</b> nilainya <b>selalu dihitung ulang</b> dari keadaan object saat
	 * ini lewat {@link #generateKodeUnik(Mahasiswa, Integer, Integer, Integer)} dan menimpa field,
	 * sehingga mengubah {@code semester}/{@code tahapan}/{@code semesterPendek} sebuah baris yang
	 * sudah tersimpan otomatis memindahkan kunci uniknya pada flush berikutnya (berpotensi
	 * menabrak baris lain dengan kombinasi yang sama).</p>
	 *
	 * <p><b>Jebakan:</b> memakai <b>field</b> {@code mahasiswa} langsung, bukan
	 * {@link #getMahasiswa()}, jadi tidak ada resolusi proxy lazy. Aman pada proxy yang belum
	 * di-init karena hanya {@code getId()} yang disentuh, tapi menghasilkan {@code null} bila relasi
	 * belum diisi.</p>
	 *
	 * @return kunci alami baris KRS, atau {@code null} bila relasi mahasiswa belum diisi.
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = generateKodeUnik(mahasiswa, getSemester(), getTahapan(), getSemesterPendek());
		return kodeUnik;
	}

	/**
	 * Menyetel kunci alami secara manual. Tidak bertahan: {@link #getKodeUnik()} menimpanya pada
	 * pembacaan berikutnya.
	 *
	 * @param kodeUnik kunci alami.
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Tahun akademik baris KRS ini dalam bentuk {@code "2023/2024"}.
	 *
	 * <p><b>Getter tidak murni:</b> bila mahasiswa, tahun angkatannya, dan semester tersedia (serta
	 * semester bukan {@code 0}), nilai dihitung ulang dan menimpa field. Perhitungannya
	 * didelegasikan ke {@code Common.getTahunAkademik(semester, tahunAngkatan, semesterMulai,
	 * awalMasukDiSemester)} dengan {@code semesterMulai} diambil dari
	 * {@code Mahasiswa.getPindahKeKampusIniMasukSemester()} — itulah yang membuat mahasiswa pindahan
	 * tidak dihitung mulai dari semester 1. Hasilnya dirakit menjadi {@code "<tahun>/<tahun+1>"}.</p>
	 *
	 * <p>Bila prasyarat tidak terpenuhi, nilai lama yang tersimpan dikembalikan apa adanya. Berbeda
	 * dengan {@link #getNama()} dan {@link #getKodeUnik()}, method ini memakai
	 * {@link #getMahasiswa()} sehingga proxy lazy ikut diresolusi.</p>
	 *
	 * @return tahun akademik bentuk {@code "2023/2024"}, atau nilai tersimpan/{@code null} bila
	 *         tidak dapat dihitung.
	 */
	public String getTahunAkademik() {
		mahasiswa = getMahasiswa();
		if (mahasiswa != null && mahasiswa.getTahunangkatan() != null && getSemester() != null
				&& !getSemester().equals(0)) {
			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
			Integer tahunAkademikMulai = Common.getTahunAkademik(getSemester(), tahunAngkatanMhs, semesterMulai,
					mahasiswa.getSemesterMulai());
			tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		}
		return tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik secara manual. Akan ditimpa {@link #getTahunAkademik()} bila data
	 * mahasiswa lengkap.
	 *
	 * @param tahunAkademik tahun akademik bentuk {@code "2023/2024"}.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Indeks prestasi kumulatif (IPK) sampai dengan semester ini.
	 *
	 * <p>Disimpan hasil hitung {@code Mahasiswa.prosesHitungIpk(detailSampaiSemesterIni)} oleh jalur
	 * sinkronisasi KRS; getter hanya menormalkan {@code null} menjadi {@code 0.0} dan menulis balik
	 * penormalan itu ke field.</p>
	 *
	 * @return IPK; tidak pernah {@code null}.
	 */
	public Double getIpk() {
		if (ipk == null) {
			ipk = 0.0;
		}
		return ipk;
	}

	/**
	 * Menyetel IPK hasil hitung ulang. Dipanggil dari jalur sinkronisasi KRS.
	 *
	 * @param ipk nilai IPK.
	 */
	public void setIpk(Double ipk) {
		this.ipk = ipk;
	}

	/**
	 * Catatan bebas yang dicetak pada kartu hasil studi (KHS).
	 *
	 * @return catatan KHS yang sudah di-{@code trim}; string kosong bila belum diisi.
	 */
	@Column(name = "catatan_khs", nullable = true, columnDefinition = "text")
	public String getCatatanKhs() {
		return catatanKhs == null ? "" : catatanKhs.trim();
	}

	/**
	 * Menyetel catatan KHS.
	 *
	 * @param catatanKhs teks catatan; boleh {@code null}.
	 */
	public void setCatatanKhs(String catatanKhs) {
		this.catatanKhs = catatanKhs;
	}

	/**
	 * Indeks prestasi semester (IPS) untuk semester ini saja.
	 *
	 * <p>Diisi jalur sinkronisasi KRS dari {@code Mahasiswa.prosesHitungIpk(detailSemesterIni,
	 * tidakMenghitungNilaiKonversi)}; apakah nilai konversi ikut dihitung dan apakah semester pendek
	 * ikut masuk ditentukan konfigurasi ({@code ips_tidak_menghitung_nilai_konversi},
	 * {@code ips_juga_dihitung_dari_sp}).</p>
	 *
	 * @return IPS; {@code 0.0} bila belum pernah dihitung. Berbeda dengan {@link #getIpk()}, getter
	 *         ini <b>tidak</b> menulis balik penormalannya ke field.
	 */
	public Double getIps() {
		return ips == null ? 0.0 : ips;
	}

	/**
	 * Menyetel IPS hasil hitung ulang. Dipanggil dari jalur sinkronisasi KRS.
	 *
	 * @param ips nilai IPS.
	 */
	public void setIps(Double ips) {
		this.ips = ips;
	}

	/**
	 * Total SKS <b>kumulatif</b> sampai dengan semester ini (SKSK).
	 *
	 * <p>Dihitung jalur sinkronisasi dari himpunan {@link Detailperkuliahan} "sampai semester ini"
	 * ({@code KrsDetailHelper.ambilDetailperkuliahanSampai}) yang sudah disaring berdasar nilai;
	 * bila baris ini bukan semester pendek, detail semester pendek dikeluarkan lebih dulu agar tidak
	 * terhitung dua kali.</p>
	 *
	 * @return SKS kumulatif; {@code 0} bila belum pernah dihitung.
	 */
	public Integer getSksk() {
		return sksk == null ? 0 : sksk;
	}

	/**
	 * Menyetel SKS kumulatif. Dipanggil dari jalur sinkronisasi KRS.
	 *
	 * @param sksk jumlah SKS kumulatif.
	 */
	public void setSksk(Integer sksk) {
		this.sksk = sksk;
	}

	/**
	 * Penanda semester pendek untuk baris KRS ini.
	 *
	 * <p>{@code null} berarti semester reguler. Nilai ini ikut membentuk kunci alami
	 * {@link #getKodeUnik()}, sehingga satu mahasiswa dapat memiliki dua kepala KRS pada semester
	 * yang sama: satu reguler dan satu semester pendek.</p>
	 *
	 * @return penanda semester pendek, atau {@code null} untuk semester reguler.
	 * @see Perkuliahan#SEMESTER_PENDEK
	 */
	public Integer getSemesterPendek() {
		return semesterPendek;
	}

	/**
	 * Menyetel penanda semester pendek.
	 *
	 * @param semesterPendek penanda semester pendek; {@code null} untuk semester reguler.
	 */
	public void setSemesterPendek(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

	// @Column(columnDefinition = "text")
	// public String getKrs() {
	//
	// return krs == null ? "" : krs.trim();
	// }
	//
	// public void setKrs(String krs) {
	// this.krs = krs;
	// }

	/**
	 * Jumlah komentar/konsultasi KRS pada semester ini.
	 *
	 * <p>Bukan relasi Hibernate: angkanya dihitung jalur sinkronisasi lewat
	 * {@code Common.loadKomentarUkuran(mahasiswa, semester, tahapan, semesterPendek)} lalu disimpan
	 * di kolom ini supaya layar rekap (mis. {@code DetailPAHelper}, {@code StudiMahasiswaHelper})
	 * bisa menampilkan lencana jumlah komentar tanpa membuka berkas komentar satu per satu.</p>
	 *
	 * @return jumlah komentar; {@code 0} bila belum pernah dihitung.
	 */
	public Integer getKomentars() {
		return komentars == null ? 0 : komentars;
	}

	/**
	 * Menyetel jumlah komentar KRS. Dipanggil dari jalur sinkronisasi KRS.
	 *
	 * @param komentars jumlah komentar.
	 */
	public void setKomentars(Integer komentars) {
		this.komentars = komentars;
	}

	/**
	 * Dosen pembimbing akademik (PA) yang menangani KRS semester ini.
	 *
	 * <p><b>Method paling berat dan paling banyak efek sampingnya di kelas ini.</b> Bukan sekadar
	 * pembaca kolom {@code dosen_pa}: ia menggabungkan tiga sumber (kolom, berkas cermin, dan dosen
	 * PA milik mahasiswa) lalu <b>menulis balik</b> hasilnya.</p>
	 *
	 * <p>Alur:</p>
	 * <ol>
	 *   <li>Memuat {@link #getMahasiswa()} di dalam {@code try}; kegagalan apa pun membuat
	 *       {@code mahasiswa} dianggap {@code null} dan seluruh langkah yang bergantung padanya
	 *       dilewati ({@code mahasiswaSiap} juga menuntut relasi sudah ter-<i>initialize</i>,
	 *       sehingga proxy lazy yang mati tidak dipaksa dimuat di sini).</li>
	 *   <li><b>Mahasiswa yang sudah keluar/lulus:</b> bila {@code statusKeluar} terisi dan jumlah
	 *       semester haknya (semester lulus, atau jumlah semester jenjang jurusannya) lebih kecil
	 *       dari semester baris ini, PA <b>dikosongkan</b> dan berkas cermin bersufiks
	 *       {@code "dosen"} ikut dikosongkan lewat {@link GeneralValueObject#put(String, String)} —
	 *       KRS di luar masa studi tidak boleh lagi menampilkan dosen PA.</li>
	 *   <li>Selain itu: id PA diambil dari field bila ada, kalau tidak dari berkas cermin
	 *       {@link GeneralValueObject#retreive(String)}, lalu object {@link Dosen}-nya diambil dari
	 *       cache konstanta {@code ConstantValues.ambil(...)}.</li>
	 *   <li>Bila masih kosong dan mahasiswa punya dosen PA sendiri, dosen itu dipakai dan
	 *       <b>ditulis</b> ke berkas cermin.</li>
	 *   <li>Setelah proxy diresolusi {@code check(...)}, bila mahasiswa ditandai
	 *       {@code dosenPaSelaluSama} maka PA <b>dipaksa</b> mengikuti dosen PA mahasiswa (menimpa
	 *       apa pun yang tersimpan per semester) dan berkas cermin ditulis lagi.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> menulis berkas cermin (I/O), mengubah field {@code dosenPa} dan
	 * {@code mahasiswa} sehingga entity managed bisa menjadi kotor, serta membaca cache konstanta.
	 * <b>Kuirk:</b> blok {@code catch} utama sengaja dikosongkan tanpa audit
	 * ({@code e.printStackTrace()} pun dikomentari), jadi kegagalan resolusi PA benar-benar senyap;
	 * dan perbandingan {@code jumlah_semester < semester} membaca <b>field</b> {@code semester}
	 * secara langsung sehingga baris tanpa semester akan melempar NPE yang lalu ditelan blok
	 * {@code catch} tersebut.</p>
	 *
	 * @return dosen PA semester ini, atau {@code null} bila tidak ada/di luar masa studi.
	 * @see #setDosenPa(Dosen)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen_pa", nullable = true)
	public Dosen getDosenPa() {
		try {
			mahasiswa = getMahasiswa();
		} catch (Exception e) {
			mahasiswa = null;
		}
		boolean mahasiswaSiap = mahasiswa != null && org.hibernate.Hibernate.isInitialized(mahasiswa);
		try {
			// FIX NPE: mahasiswa bisa null (relasi belum diisi) -- sebelumnya sudah dipakai di
			// baris ini (getSemesterLulus()) sebelum dicek null di kondisi if di bawah.
			Integer jumlah_semester = null;
			if (mahasiswaSiap) {
				jumlah_semester = mahasiswa.getSemesterLulus();
				if (jumlah_semester == null && mahasiswa.getJurusan() != null
						&& mahasiswa.getJurusan().getJenjang() != null) {
					jumlah_semester = mahasiswa.getJurusan().getJenjang().getJumlahSemester();
				}
			}
			if (mahasiswaSiap && mahasiswa.getStatusKeluar() != null && jumlah_semester != null
					&& jumlah_semester < semester) {
				dosenPa = null;
				put("", "dosen");
			} else {
				String s = dosenPa == null || dosenPa.getId() == null ? retreive("dosen") : dosenPa.getId().toString();
				if (s != null && !s.trim().isEmpty()) {
					dosenPa = (Dosen) ConstantValues.ambil(Dosen.class.getName(), Long.parseLong(s.trim()));
				}

				if (dosenPa == null && mahasiswaSiap && mahasiswa.getDosen() != null) {
					dosenPa = (Dosen) ConstantValues.ambil(Dosen.class.getName(), mahasiswa.getDosen());
					if (dosenPa != null) {
						put(dosenPa.getId().toString(), "dosen");
					}
				}
			}
		} catch (Exception e) {
//			e.printStackTrace();
		}

		dosenPa = check(dosenPa);

		if (mahasiswaSiap && Boolean.TRUE.equals(mahasiswa.getDosenPaSelaluSama())) {
			dosenPa = (Dosen) ConstantValues.ambil(Dosen.class.getName(), mahasiswa.getDosen());
			if (dosenPa != null) {
				put(dosenPa.getId().toString(), "dosen");
			}
		}

		return dosenPa;
	}

	/**
	 * Menyetel — atau melepas — dosen PA untuk KRS semester ini.
	 *
	 * <p><b>Efek samping:</b> selain field, berkas cermin bersufiks {@code "dosen"} ikut ditulis
	 * lewat {@link GeneralValueObject#put(String, String)} agar {@link #getDosenPa()} tetap
	 * menemukan PA yang sama walau baris belum sempat ter-flush ke basis data.</p>
	 *
	 * <p>Argumen {@code null} (atau {@link Dosen} tanpa id) diperlakukan sebagai <b>pelepasan PA
	 * yang sah</b>: kolom dan cermin dikosongkan. Lihat komentar di dalam kode — versi lama
	 * mengabaikan {@code null} sehingga tombol "Hapus" di layar tidak pernah benar-benar melepas
	 * dosen PA.</p>
	 *
	 * @param dosenPa dosen PA baru, atau {@code null} untuk melepas PA.
	 * @see #getDosenPa()
	 */
	public void setDosenPa(Dosen dosenPa) {
		if (dosenPa != null && dosenPa.getId() != null) {
			put(dosenPa.getId().toString(), "dosen");
			this.dosenPa = dosenPa;
		} else {
			// Nilai null adalah operasi pelepasan PA yang sah. Implementasi lama
			// mengabaikannya sehingga kolom dosen_pa dan mirror "dosen" tetap berisi
			// dosen sebelumnya walaupun layar sudah menekan tombol Hapus.
			put("", "dosen");
			this.dosenPa = null;
		}
	}

	/**
	 * Kelas/rombongan belajar mahasiswa pada semester ini.
	 *
	 * <p><b>Getter paling mahal di kelas ini — jangan dipanggil di dalam perulangan.</b> Bila kolom
	 * masih kosong, method mencari nilai penggantinya berturut-turut:</p>
	 * <ol>
	 *   <li>dari {@code mahasiswa.getKelas()};</li>
	 *   <li>bila masih kosong dan semester &gt; 1, <b>menelusuri mundur semester demi semester</b>
	 *       dan memanggil {@code Common.singkronkanKrsMahasiswa(mahasiswa, i, null, null, false,
	 *       false, true)} untuk tiap semester sebelumnya sampai menemukan KRS yang punya kelas.
	 *       Argumen terakhir {@code true} ({@code jikaTidakAdaKembali}) menahan pembuatan baris KRS
	 *       baru, tetapi pemanggilan itu tetap dapat membuka session/transaksi Hibernate sendiri dan
	 *       menyentuh cache JSON sementara. Satu pembacaan properti karenanya bisa berubah menjadi
	 *       belasan query.</li>
	 * </ol>
	 *
	 * <p><b>Menulis ke entity lain:</b> bila KRS punya kelas sedangkan {@code mahasiswa} belum,
	 * nilai tersebut disalin ke {@code mahasiswa.setKelas(...)}. Bila mahasiswa ditandai
	 * {@code kelasSelaluSama}, arah salinannya dibalik: kelas KRS dipaksa mengikuti kelas
	 * mahasiswa. Pada mahasiswa yang managed, perubahan ini akan ikut ter-{@code UPDATE} saat
	 * flush.</p>
	 *
	 * <p><b>Kuirk/bug potensial:</b> pemeriksaan {@code mahasiswa != null} baru dilakukan di bagian
	 * akhir method, padahal {@code mahasiswa.getKelas()} sudah dipanggil di baris pertama —
	 * KRS tanpa relasi mahasiswa melempar {@code NullPointerException} sebelum sampai ke
	 * pemeriksaan itu. Kegagalan penelusuran mundur ditangkap dan dicatat ke audit error, lalu
	 * proses dilanjutkan.</p>
	 *
	 * @return nama kelas yang sudah di-{@code trim}; string kosong bila tidak ditemukan.
	 */
	public String getKelas() {
		mahasiswa = getMahasiswa();
		if ((kelas == null || kelas.trim().isEmpty()) && !mahasiswa.getKelas().isEmpty()) {
			kelas = mahasiswa.getKelas();
		}

		if ((kelas == null || kelas.trim().isEmpty()) && getSemester() != null && getSemester() > 1) {
			try {
				for (int i = (getSemester() - 1); i >= 1; i--) {

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, i, null, null, false, false,
							true);
					if (krsMahasiswa != null && krsMahasiswa.getId() != null && krsMahasiswa.kelas != null
							&& !krsMahasiswa.kelas.trim().isEmpty()) {
						kelas = krsMahasiswa.kelas;
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/KrsMahasiswa.java:496");
			}
		}

		if ((kelas != null && !kelas.trim().isEmpty())
				&& (mahasiswa.getKelas() == null || mahasiswa.getKelas().trim().isEmpty())) {
			mahasiswa.setKelas(kelas);
		}

		if (mahasiswa != null && mahasiswa.getKelasSelaluSama()) {
			kelas = mahasiswa.getKelas();
		}

		return kelas == null ? "" : kelas.trim();
	}

	/**
	 * Menyetel kelas/rombongan belajar untuk semester ini.
	 *
	 * @param kelas nama kelas; boleh {@code null}.
	 */
	public void setKelas(String kelas) {
		this.kelas = kelas;
	}

	/**
	 * Penanda baris KRS aktif.
	 *
	 * <p>{@code null} dibaca sebagai {@code true} supaya baris lama yang belum punya kolom ini tetap
	 * dianggap aktif. Jalur sinkronisasi selalu menyetelnya {@code true} setiap kali menghitung
	 * ulang KRS.</p>
	 *
	 * @return {@code true} bila baris KRS aktif.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif baris KRS.
	 *
	 * @param aktif {@code true} bila aktif.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Implementasi kontrak {@link VOPembelajaran}: tahun akademik "pembelajaran" ini.
	 *
	 * <p>Untuk kepala KRS, tahun akademiknya adalah tahun akademik KRS itu sendiri, jadi method ini
	 * hanya meneruskan ke {@link #getTahunAkademik()} — beserta seluruh efek sampingnya (nilai
	 * dihitung ulang dan menimpa field).</p>
	 *
	 * @return tahun akademik bentuk {@code "2023/2024"}.
	 * @see #getTahunAkademik()
	 */
	@Override
	public String ambilTahunAkademik() {
		return getTahunAkademik();
	}

	/**
	 * Implementasi kontrak {@link VOPembelajaran}: nomor semester "pembelajaran" ini.
	 *
	 * <p><b>Kuirk:</b> implementasi induk {@code VOPembelajaran.ambilSemester()} sudah punya cabang
	 * {@code instanceof KrsMahasiswa} yang mengembalikan nilai sama, sehingga override ini
	 * sebenarnya redundan — dipertahankan karena lebih murah (tanpa rantai {@code instanceof}) dan
	 * lebih jelas.</p>
	 *
	 * @return nomor semester KRS.
	 * @see #getSemester()
	 */
	@Override
	public Integer ambilSemester() {
		return getSemester();
	}

	/**
	 * Implementasi kontrak {@link VOPembelajaran}: jenis semester ganjil/genap.
	 *
	 * <p>Diturunkan murni dari paritas nomor semester — semester genap &rarr;
	 * {@link Perkuliahan#GENAP}, ganjil &rarr; {@link Perkuliahan#GANJIL}. Semester pendek
	 * <b>tidak</b> tercermin di sini (lihat {@link #getSemesterPendek()}); pembeda SP ditangani
	 * {@code VOPembelajaran.toIdSmt()}.</p>
	 *
	 * @return {@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}, atau string kosong bila
	 *         semester belum diisi.
	 */
	@Override
	public String ambilJenisSemester() {
		Integer semesterData = getSemester();
		return semesterData == null ? ""
				: semesterData.intValue() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
	}

	/**
	 * Implementasi kontrak {@link VOPesertaPembelajaran}: objek pembelajaran yang diikuti peserta.
	 *
	 * <p>Kepala KRS berperan ganda — ia sekaligus peserta dan induk pembelajaran (bimbingan PA) —
	 * sehingga method ini mengembalikan {@code this}.</p>
	 *
	 * @return object ini sendiri.
	 */
	@Override
	public VOPembelajaran ambilVOPembelajaran() {
		// TODO Auto-generated method stub
		return this;
	}

	/**
	 * Implementasi kontrak {@link VOPembelajaran}: jumlah peserta langsung pembelajaran ini.
	 *
	 * <p>Selalu {@code 1} — bimbingan PA yang digantung pada kepala KRS hanya diikuti satu
	 * mahasiswa, yaitu pemilik KRS. Dipakai layar kalender/pertemuan untuk menampilkan jumlah
	 * peserta dan menghitung tinggi baris.</p>
	 *
	 * @return selalu {@code 1}.
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 1;
	}

	/**
	 * {@code id_aktivitas} milik PDDikti Feeder untuk baris aktivitas kuliah mahasiswa (AKM) yang
	 * bersesuaian dengan KRS ini.
	 *
	 * <p>Diisi {@code FeederExporter} setelah sinkronisasi berhasil, dan dipakai pada pengiriman
	 * berikutnya untuk membedakan <i>insert</i> dari <i>update</i> di sisi Feeder. Nilai kosong
	 * dinormalkan menjadi {@code null} justru supaya pemeriksaan "sudah pernah dikirim?" cukup
	 * memeriksa {@code null}.</p>
	 *
	 * @return id aktivitas Feeder, atau {@code null} bila belum pernah terkirim.
	 */
	@Column(columnDefinition = "text")
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Menyetel {@code id_aktivitas} Feeder hasil sinkronisasi.
	 *
	 * @param feeder id aktivitas dari PDDikti Feeder.
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Nomor SK penugasan bimbingan untuk KRS ini.
	 *
	 * @return nomor SK yang sudah di-{@code trim}; string kosong bila belum diisi.
	 */
	public String getNoSk() {
		return noSk == null ? "" : noSk.trim();
	}

	/**
	 * Menyetel nomor SK penugasan bimbingan.
	 *
	 * @param noSk nomor SK; boleh {@code null}.
	 */
	public void setNoSk(String noSk) {
		this.noSk = noSk;
	}

	/**
	 * Tanggal SK penugasan bimbingan (disimpan tanpa bagian jam).
	 *
	 * @return tanggal SK, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTglSk() {
		return tglSk;
	}

	/**
	 * Menyetel tanggal SK penugasan bimbingan.
	 *
	 * @param tglSk tanggal SK; boleh {@code null}.
	 */
	public void setTglSk(Date tglSk) {
		this.tglSk = tglSk;
	}

	/** Konfigurasi materi/kursus daring (JSON) untuk pembelajaran bimbingan pada KRS ini. */
	private String course;
	/** Penanda urutkan pertemuan otomatis; {@code null} dibaca sebagai {@code true}. */
	private Boolean urutkanotomatis;

	/**
	 * Implementasi kontrak {@link VOPembelajaran}: konfigurasi materi/kursus daring dalam bentuk
	 * teks JSON.
	 *
	 * <p>Nilai kosong dinormalkan menjadi {@code "{}"} ({@code new JSONObject().toString()}) supaya
	 * pemanggil bisa langsung mem-parsing tanpa memeriksa {@code null} lebih dulu.</p>
	 *
	 * @return teks JSON konfigurasi kursus; tidak pernah {@code null} maupun kosong.
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/**
	 * Menyetel konfigurasi materi/kursus daring.
	 *
	 * @param course teks JSON konfigurasi kursus.
	 */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * SKS hasil <b>konversi</b>/transfer pada semester ini.
	 *
	 * <p>Diisi jalur sinkronisasi dari {@code Mahasiswa.prosesHitungSks(det, false, true)}.
	 * Bersama {@link #getSksBukanKonversi()} memilah asal SKS semester ini, dipakai layar studi
	 * mahasiswa dan cetak KRS untuk menuliskan "(x SKS konversi)".</p>
	 *
	 * <p><b>Awas nama kolom:</b> dipetakan ke kolom warisan {@code mkbelumdiniali} (termasuk salah
	 * ketiknya) yang dulu berarti "mata kuliah belum dinilai". Nama kolom tidak lagi mencerminkan
	 * isinya.</p>
	 *
	 * @return SKS konversi; {@code 0} bila belum pernah dihitung.
	 */
	@Column(name = "mkbelumdiniali", nullable = true)
	public Integer getSksKonversi() {
		return sksKonversi == null ? 0 : sksKonversi;
	}

	/**
	 * Menyetel SKS konversi. Dipanggil dari jalur sinkronisasi KRS.
	 *
	 * @param sksKonversi jumlah SKS konversi.
	 */
	public void setSksKonversi(Integer sksKonversi) {
		this.sksKonversi = sksKonversi;
	}

	/**
	 * SKS murni (<b>bukan</b> konversi) pada semester ini.
	 *
	 * <p>Diisi jalur sinkronisasi dari {@code Mahasiswa.prosesHitungSks(det, true, true)}. Inilah
	 * angka yang dikirim ke PDDikti dan disalin ke {@code HistoryStatusMahasiswa.setSks(...)},
	 * bukan {@link #getSksYangDiambil()}.</p>
	 *
	 * <p><b>Awas nama kolom:</b> dipetakan ke kolom warisan {@code mkkbelumdinilai}. Sama seperti
	 * {@link #getSksKonversi()}, nama kolom tidak mencerminkan isinya.</p>
	 *
	 * @return SKS bukan konversi; {@code 0} bila belum pernah dihitung.
	 */
	@Column(name = "mkkbelumdinilai", nullable = true)
	public Integer getSksBukanKonversi() {
		return sksBukanKonversi == null ? 0 : sksBukanKonversi;
	}

	/**
	 * Menyetel SKS bukan konversi. Dipanggil dari jalur sinkronisasi KRS.
	 *
	 * @param sksBukanKonversi jumlah SKS bukan konversi.
	 */
	public void setSksBukanKonversi(Integer sksBukanKonversi) {
		this.sksBukanKonversi = sksBukanKonversi;
	}

	/**
	 * Jejak asal {@link #getSksYangDiambil()}: daftar id {@link Detailperkuliahan} semester ini,
	 * dipisah koma.
	 *
	 * <p>Disimpan supaya tombol "unduh KRS" bisa menampilkan tepat baris-baris yang ikut dihitung,
	 * tanpa mengulang penyaringan nilai. Pemakainya
	 * ({@code PenilaianUtil.downloadSemuaKRS(String, Mahasiswa)}) menyisipkan nilai ini apa adanya
	 * ke {@code Restrictions.sqlRestriction("id in (" + sks + ")")} — <b>jangan pernah</b> mengisi
	 * kolom ini dari masukan pengguna.</p>
	 *
	 * @return CSV id detail perkuliahan; string kosong bila belum pernah dihitung.
	 */
	@Column(name = "sksk_yang_diambil_s", nullable = true, columnDefinition = "text")
	public String getSksYangDiambilS() {
		return sksYangDiambilS == null ? "" : sksYangDiambilS;
	}

	/**
	 * Menyetel CSV id detail perkuliahan semester ini. Dipanggil dari jalur sinkronisasi KRS.
	 *
	 * @param sksYangDiambilS daftar id dipisah koma.
	 */
	public void setSksYangDiambilS(String sksYangDiambilS) {
		this.sksYangDiambilS = sksYangDiambilS;
	}

	/**
	 * Jejak asal {@link #getSksk()}: daftar id {@link Detailperkuliahan} kumulatif sampai semester
	 * ini, dipisah koma.
	 *
	 * <p>Berlaku catatan yang sama dengan {@link #getSksYangDiambilS()} soal pemakaiannya di
	 * {@code sqlRestriction}.</p>
	 *
	 * @return CSV id detail perkuliahan kumulatif; string kosong bila belum pernah dihitung.
	 * @see #getSksYangDiambilS()
	 */
	@Column(name = "sksk_s", nullable = true, columnDefinition = "text")
	public String getSkskS() {
		return skskS == null ? "" : skskS;
	}

	/**
	 * Menyetel CSV id detail perkuliahan kumulatif. Dipanggil dari jalur sinkronisasi KRS.
	 *
	 * @param skskS daftar id dipisah koma.
	 */
	public void setSkskS(String skskS) {
		this.skskS = skskS;
	}

	/**
	 * Bagian {@link #getSksYangDiambil()} yang sudah dinyatakan <b>lulus</b>.
	 *
	 * <p>Yang dihitung hanya detail perkuliahan yang punya nilai huruf, total nilai &gt; 10,0, dan
	 * bendera {@code lulus} bernilai benar. Dipakai laporan rekap nilai per semester.</p>
	 *
	 * @return SKS lulus semester ini; {@code 0} bila belum pernah dihitung.
	 */
	public Integer getSksYangDiambilLulus() {
		return sksYangDiambilLulus == null ? 0 : sksYangDiambilLulus;
	}

	/**
	 * Menyetel SKS lulus semester ini. Dipanggil dari jalur sinkronisasi KRS.
	 *
	 * @param sksYangDiambilLulus jumlah SKS lulus.
	 */
	public void setSksYangDiambilLulus(Integer sksYangDiambilLulus) {
		this.sksYangDiambilLulus = sksYangDiambilLulus;
	}

	/**
	 * Bagian {@link #getSksk()} kumulatif yang sudah dinyatakan <b>lulus</b>, dengan kriteria sama
	 * seperti {@link #getSksYangDiambilLulus()}.
	 *
	 * @return SKS lulus kumulatif; {@code 0} bila belum pernah dihitung.
	 */
	public Integer getSkskLulus() {
		return skskLulus == null ? 0 : skskLulus;
	}

	/**
	 * Menyetel SKS lulus kumulatif. Dipanggil dari jalur sinkronisasi KRS.
	 *
	 * @param skskLulus jumlah SKS lulus kumulatif.
	 */
	public void setSkskLulus(Integer skskLulus) {
		this.skskLulus = skskLulus;
	}

	/**
	 * Implementasi kontrak {@link VOPembelajaran}: apakah daftar pertemuan diurutkan otomatis.
	 *
	 * <p>{@code null} dibaca sebagai {@code true} agar data lama tetap berperilaku seperti
	 * sebelumnya.</p>
	 *
	 * @return {@code true} bila pertemuan diurutkan otomatis.
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/**
	 * Menyetel penanda urutkan pertemuan otomatis.
	 *
	 * @param urutkanotomatis {@code true} untuk mengurutkan otomatis.
	 */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

	/**
	 * Mengisi peta parameter laporan (Jasper) dengan seluruh angka rekap KRS mahasiswa, semester
	 * demi semester, ditambah barcode ijazah dan judisium.
	 *
	 * <p>Dipakai laporan transkrip/rekaman nilai/ijazah/prestasi
	 * ({@code LaporanRekamanNilai}, {@code LaporanRekamanNilai2Kolom},
	 * {@code LaporanIjazahAkademik}, {@code LaporanPrestasiMahasiswa}).</p>
	 *
	 * <p>Yang dikerjakan:</p>
	 * <ol>
	 *   <li>Menaruh {@code "mahasiswa"} = id mahasiswa.</li>
	 *   <li>Bila nomor ijazah 1/2 terisi, <b>membangkitkan berkas PNG QR code</b> di
	 *       {@code Common.ambilREAL_PATH_REPORT()} bernama
	 *       {@code qrcode_ijazah_<n>_<idMahasiswa>.png} lalu menaruh path absolutnya sebagai
	 *       parameter {@code qrcode_ijazah_1}/{@code qrcode_ijazah_2}. Berkas lama akan ditimpa.</li>
	 *   <li>Untuk setiap semester {@code 1..semester}: memanggil
	 *       {@code Common.singkronkanKrsMahasiswa(mahasiswa, i, null, null, hitungUang)} —
	 *       <b>bukan sekadar membaca</b>: dengan {@code hitungUang} bernilai {@code true} seluruh
	 *       KRS dihitung ulang dari basis data dan disimpan (mahal, dan menulis ke
	 *       {@code krs_mahasiswa}) — lalu menaruh parameter bersufiks nomor semester:
	 *       {@code dosen_pa_i}, {@code dosen_nidn_i}, {@code dosen_code_i}, {@code dosen_nip_i},
	 *       {@code sks_i}, {@code semester_i}, {@code sksk_i}, {@code ipk_i} (beserta varian
	 *       {@code _ceil}/{@code _floor}/{@code _round}/{@code _terbilang}), {@code ip_i} (beserta
	 *       variannya) dan {@code mutu_i}.</li>
	 *   <li>Setelah perulangan, KRS semester <b>terakhir</b> dipakai untuk menaruh set parameter
	 *       yang sama tanpa sufiks, ditambah {@code judisium}/{@code judisium_en} dari
	 *       {@code Common.hitungJudisium(mahasiswa, krsMahasiswa)} dan {@code mutu}.</li>
	 * </ol>
	 *
	 * <p><b>Kuirk yang perlu diketahui</b> (didokumentasikan apa adanya, tidak diperbaiki):</p>
	 * <ul>
	 *   <li>{@code ip_round_i} dan {@code ip_round} diisi memakai {@code Math.floor(...)}, bukan
	 *       {@code Math.round(...)} — berbeda dengan pasangan {@code ipk_round} di atasnya.</li>
	 *   <li>{@code getDosenPa()} dipanggil sampai empat kali berturut-turut per semester, dan itu
	 *       adalah getter berefek samping (tulis berkas cermin) — lihat {@link #getDosenPa()}.</li>
	 *   <li>Perulangan berhenti bila {@code semester} {@code null} akan melempar NPE saat
	 *       {@code i <= semester} di-<i>unbox</i>; pemanggil selalu mengirim
	 *       {@code mahasiswa.currentSemester()} atau nilai dari layar.</li>
	 * </ul>
	 *
	 * @param mahasiswa  mahasiswa yang laporannya dicetak; wajib punya id.
	 * @param semester   semester terakhir yang dicetak; parameter dibuat untuk 1 sampai nilai ini.
	 * @param hitungUang bila {@code true}, KRS tiap semester dihitung ulang paksa ke basis data
	 *                   (melewati cache sinkronisasi); bila {@code false}, hasil cache boleh
	 *                   dipakai.
	 * @param parameters peta parameter laporan yang <b>diubah di tempat</b> (Jasper
	 *                   {@code Map<String, Object>}).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void parameterData(Mahasiswa mahasiswa, Integer semester, Boolean hitungUang, Map parameters) {
		parameters.put("mahasiswa", mahasiswa.getId());

		if (mahasiswa.getNoIjazah1() != null && !mahasiswa.getNoIjazah1().trim().isEmpty()) {
			File myfilebarcode = new File(
					Common.ambilREAL_PATH_REPORT() + "/qrcode_ijazah_1_" + mahasiswa.getId() + ".png");
			BarcodeCommon.generateCRCode(mahasiswa.getNoIjazah1().trim(), myfilebarcode);
			parameters.put("qrcode_ijazah_1", myfilebarcode.getAbsolutePath());
		}

		if (mahasiswa.getNoIjazah2() != null && !mahasiswa.getNoIjazah2().trim().isEmpty()) {
			File myfilebarcode = new File(
					Common.ambilREAL_PATH_REPORT() + "/qrcode_ijazah_2_" + mahasiswa.getId() + ".png");
			BarcodeCommon.generateCRCode(mahasiswa.getNoIjazah2().trim(), myfilebarcode);
			parameters.put("qrcode_ijazah_2", myfilebarcode.getAbsolutePath());
		}

		KrsMahasiswa krsMahasiswa = null;
		for (int i = 1; i <= semester; i++) {
			krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, i, null, null, hitungUang);

			parameters.put("dosen_pa_" + i,
					krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());
			parameters.put("dosen_nidn_" + i,
					krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNidn());
			parameters.put("dosen_code_" + i,
					krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getCode());
			parameters.put("dosen_nip_" + i,
					krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getMycode());
			parameters.put("sks_" + i, krsMahasiswa.getSksYangDiambil());
			parameters.put("semester_" + i, krsMahasiswa.getSemester());
			parameters.put("sksk_" + i, krsMahasiswa.getSksk());
			parameters.put("ipk_" + i, krsMahasiswa.getIpk());
			parameters.put("ipk_ceil_" + i, Math.ceil(krsMahasiswa.getIpk()));
			parameters.put("ipk_floor_" + i, Math.floor(krsMahasiswa.getIpk()));
			parameters.put("ipk_round_" + i, Math.round(krsMahasiswa.getIpk()));
			parameters.put("ipk_terbilang_" + i,
					IndonesianNumberToWords.convert(Common.numberFormat2.get().format(krsMahasiswa.getIpk())));
			parameters.put("ip_" + i, krsMahasiswa.getIps());
			parameters.put("ip_ceil_" + i, Math.ceil(krsMahasiswa.getIps()));
			parameters.put("ip_floor_" + i, Math.floor(krsMahasiswa.getIps()));
			parameters.put("ip_round_" + i, Math.floor(krsMahasiswa.getIps()));
			parameters.put("mutu_" + i, mahasiswa.hitungMutuSemester(i, null, null));

		}

		if (krsMahasiswa != null) {
			Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);
			parameters.put("judisium", judisium == null ? "" : judisium.getNama());
			parameters.put("judisium_en", judisium == null ? "" : judisium.getNamaen());
			parameters.put("dosen_pa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());
			parameters.put("dosen_nidn", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNidn());
			parameters.put("dosen_code", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getCode());
			parameters.put("dosen_nip", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getMycode());
			parameters.put("sks", krsMahasiswa.getSksYangDiambil());
			parameters.put("semester", krsMahasiswa.getSemester());
			parameters.put("sksk", krsMahasiswa.getSksk());
			parameters.put("ipk", krsMahasiswa.getIpk());
			parameters.put("ipk_ceil", Math.ceil(krsMahasiswa.getIpk()));
			parameters.put("ipk_floor", Math.floor(krsMahasiswa.getIpk()));
			parameters.put("ipk_round", Math.round(krsMahasiswa.getIpk()));
			parameters.put("ipk_terbilang",
					IndonesianNumberToWords.convert(Common.numberFormat2.get().format(krsMahasiswa.getIpk())));
			parameters.put("ip", krsMahasiswa.getIps());
			parameters.put("ip_ceil", Math.ceil(krsMahasiswa.getIps()));
			parameters.put("ip_floor", Math.floor(krsMahasiswa.getIps()));
			parameters.put("ip_round", Math.floor(krsMahasiswa.getIps()));
			parameters.put("mutu", mahasiswa.hitungMutu());
		}
	}

}
