package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.A;
import org.zkoss.zul.Label;
import ais.common.PagingApi;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.BacaTulisUtil;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.AuditListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.Pendidikan;
import ais.database.model.epsbed.EpsbedJabatanAkademik;
import ais.database.model.epsbed.EpsbedStatusAktivitasDosen;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.TipePenelitianDanPengabdian;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Entity Hibernate untuk <b>dosen</b> (tenaga pendidik) pada tabel {@code public.dosen} — pusat
 * seluruh data kepegawaian akademik sekaligus titik masuk hampir semua fitur "sisi dosen" di AIS:
 * pengampuan perkuliahan, pertemuan/kelas, bimbingan tugas akhir, KKN/PKL, perwalian (KRS),
 * kegiatan kedosenan, penelitian &amp; pengabdian, artikel, buku bahan ajar, prestasi dan
 * penghargaan.
 *
 * <p>Rangka kelas: {@code Dosen} adalah subclass konkret {@link Karyawan} (yang sendiri turunan
 * {@link GeneralValueObject}) dan mengimplementasikan {@link VOMahasiswaDosen} sehingga dapat
 * diperlakukan seragam dengan {@link Mahasiswa} pada layar-layar pembelajaran. Kontrak umum
 * {@code id}/{@code equals}/{@code hashCode}/{@code compareTo}/{@code check(...)} beserta
 * mekanisme cache berkas ({@code put}/{@code retreive}/{@code udah}/{@code write}) diwarisi dari
 * {@link GeneralValueObject} dan tidak diulang di sini — lihat Javadoc kelas induk tersebut.
 * Anotasi {@code @Audited} (Hibernate Envers) membuat setiap perubahan baris dosen terekam di
 * tabel audit, sedangkan {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya
 * menulis kolom yang benar-benar berubah.</p>
 *
 * <h3>Pengelompokan data (kolom)</h3>
 * <ul>
 * <li><b>Identitas &amp; kontak</b> — {@code nama}, {@code gelarDepan}/{@code gelarBelakang},
 * {@code alamat}, {@code email} (boleh berisi beberapa alamat dipisah koma, lihat
 * {@link #appendEmail(String)}), {@code telp}, {@code hp}, {@code kelamin}, {@code tempatlahir},
 * {@code tanggallahir}, {@code ktp}, {@code npwp}.</li>
 * <li><b>Nomor induk &amp; pelaporan eksternal</b> — {@code nidn}, {@code niyNigk}, {@code nuptk},
 * {@code code}/{@code mycode}, {@code nira}, {@code nomorSertifikasi}, {@code feeder} dan
 * {@code idRegPtk} (kunci sinkronisasi PDDikti/Feeder), {@code kodeSinta}, {@code googleScholar}.</li>
 * <li><b>Kepegawaian</b> — {@code pangkat}, {@code golongan} (turunan, lihat {@link #getGolongan()}),
 * {@link Golongan} pegawai dan {@link GolonganPns}, {@link StatusPegawai}, {@link StatusKepegawaian},
 * {@link IkatanKerjaDosen} beserta bendera turunan {@code tetap}, {@link LembagaPengangkat},
 * {@link SumberGaji}, {@link JenisPendidikDanTenagaKependidikan}, {@code skCpns}/{@code tglSkCpns},
 * {@code tmtPns}, {@code skAngkat}/{@code tmtSkAngkat}, {@code milikUniversitas},
 * {@code pegawaiId} (jembatan ke modul kepegawaian {@link Pegawai}), {@code atasanlangsung}
 * (ID {@code Dosen} lain, bukan relasi Hibernate).</li>
 * <li><b>Jabatan</b> — {@link JabatanFungsionalDosen} (asisten ahli s.d. guru besar),
 * {@link EpsbedJabatanAkademik} untuk pelaporan, {@link Jabatan} struktural
 * ({@code spesifikasiJabatan}) dan jabatan di perguruan tinggi lain
 * ({@code spesifikasiJabatanPtLain}), {@code jabatan} bebas-teks.</li>
 * <li><b>Unit akademik</b> — {@link Jurusan}, {@link Fakultas} (diturunkan dari jurusan bila ada),
 * {@link PerguruanTinggi} (dengan rantai fallback, lihat {@link #getPerguruanTinggi()}),
 * {@link Ruang} kantor.</li>
 * <li><b>Kompetensi &amp; beban</b> — {@code spesialisasi1..3}, {@link Pendidikan} dan
 * {@code pendidikans1..3}, {@code sertifikasi}, {@code sesuaiBidangKeilmuan},
 * {@link StatusKewajibanBebanDosen}, {@link EpsbedStatusAktivitasDosen}. Beberapa kolom warisan
 * domain sekolah/pengawas ikut menumpang di sini: {@code aLisensiKepsek}, {@code jmlSekolahBinaan},
 * {@code aDiklatAwas}, {@code aktaIjinAjar}, {@code aBraille}, {@code aBhsIsyarat}.</li>
 * <li><b>Operasional</b> — {@code idfinger} (ID pada mesin sidik jari), {@code lockId},
 * {@code bahasa} antarmuka, {@code onlineMenggunakan} + {@code onlineLink} untuk kelas daring
 * (lihat konstanta {@link #JITSI}, {@link #GOOGLE_MEET}, {@link #ZOOM}, {@link #BBB},
 * {@link #SKYPE}, {@link #WA}, {@link #LAIN}), {@code formula} (JSON rekap kehadiran/SKS per
 * periode, ditulis {@code ais.action.master.helper.ProsesKehadiranDosen}), {@code oleh}/{@code olehId}
 * dan {@code tanggal_dirubah} sebagai jejak audit ringan.</li>
 * </ul>
 *
 * <h3>Indeks berkas JSON per dosen (pola paling penting di kelas ini)</h3>
 * <p>Relasi "satu dosen ke banyak X" di kelas ini <b>tidak</b> dipetakan sebagai koleksi Hibernate.
 * Sebagai gantinya setiap dosen memiliki berkas indeks JSON di direktori kerja aplikasi (via
 * {@code Common.getFileLocation(this, "&lt;nama&gt;_" + id)} dan {@link ais.common.BacaTulisUtil}),
 * berisi peta {@code "id" -&gt; "id"} (nilai kosong berarti dihapus). Setiap domain punya lima
 * method dengan pola nama yang sama:</p>
 * <ol>
 * <li>{@code ambilLokasiX()} — membaca isi berkas indeks mentah;</li>
 * <li>{@code tulisLokasiX(String)} — menimpa berkas indeks;</li>
 * <li>{@code populateX(...)} — menambah satu ID ke indeks;</li>
 * <li>{@code removeX(Serializable)} — mengosongkan satu ID di indeks;</li>
 * <li>{@code reInitX(Session)} — membangun ulang indeks dari query Hibernate;</li>
 * <li>{@code ambilX()} — membaca indeks menjadi {@code List&lt;Long&gt;}, memanggil
 * {@code reInitX} lebih dulu bila penanda sekali-jalan {@code udah("X")} belum ada.</li>
 * </ol>
 * <p>Domain yang memakai pola ini: perkuliahan, pertemuan, kegiatan kedosenan, organisasi dosen,
 * prestasi, penghargaan, pengajuan penelitian &amp; pengabdian, artikel, dan buku bahan ajar.
 * Konsekuensinya: memanggil {@code ambilX()} pada dosen yang indeksnya belum pernah dibangun akan
 * memicu query berat sekali jalan; menghapus/menambah relasi di tempat lain <b>wajib</b> disertai
 * pemanggilan {@code populateX}/{@code removeX} agar indeks tidak basi (lihat
 * {@link ais.database.hibernate.AuditListener}).</p>
 *
 * <h3>Hal-hal non-obvious</h3>
 * <ul>
 * <li><b>Getter tidak murni.</b> Banyak getter di kelas ini melakukan normalisasi, penulisan balik
 * ke field, pembacaan berkas cache, bahkan query/penyimpanan ke basis data (mis.
 * {@link #getPerguruanTinggi()}, {@link #getTetap()}, {@link #getGolonganPns()},
 * {@link #ambilBiodata()}). Jangan asumsikan getter bebas efek samping.</li>
 * <li><b>Field bayangan.</b> {@code code}, {@code mycode}, {@code nama}, {@code alamat},
 * {@code email}, {@code telp}, {@code kelamin}, {@code tempatlahir}, {@code jurusan},
 * {@code fakultas}, {@code tetap}, {@code idfinger} dsb. dideklarasikan ulang di sini meskipun
 * sudah ada di {@link Karyawan}; getter/setter Dosen menutupi versi induk (mis. {@code getNama()}
 * induk mengembalikan {@code code + "-" + nama}, versi Dosen tidak). Yang dipetakan Hibernate
 * adalah accessor pada kelas ini.</li>
 * <li><b>Konkurensi.</b> Satu dosen yang sama bisa punya lebih dari satu instance Hibernate,
 * sehingga penguncian read-modify-write indeks pertemuan memakai peta kunci global
 * {@code KUNCI_PERTEMUAN_DOSEN} berbasis ID, bukan {@code synchronized(this)}.</li>
 * <li><b>Pemulihan data rusak.</b> {@link #ambilLokasiPertemuanJsonAman()} dan
 * {@link #removePerkuliahan(Serializable)} sengaja memulihkan diri dari berkas indeks JSON yang
 * terpotong alih-alih melempar {@link org.json.JSONException}.</li>
 * <li><b>Anti-rekursi.</b> {@link #ambilBiodata(boolean)} dijaga {@code ThreadLocal} karena
 * auto-flush Hibernate dapat memanggil balik getter yang memanggilnya (StackOverflowError).</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see Karyawan
 * @see VOMahasiswaDosen
 * @see BiodataDosen
 * @see Perkuliahan
 * @see Pertemuan
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "dosen")
public class Dosen extends Karyawan implements VOMahasiswaDosen {
	/**
	 * Kunci read-modify-write cache pertemuan per dosen. Entity Dosen untuk ID yang sama dapat
	 * mempunyai lebih dari satu instance Hibernate, jadi sinkronisasi pada {@code this} tidak cukup.
	 */
	private static final java.util.concurrent.ConcurrentHashMap<String, Object> KUNCI_PERTEMUAN_DOSEN =
			new java.util.concurrent.ConcurrentHashMap<String, Object>();

	/**
	 * 
	 */
	private static final long serialVersionUID = -5130925140455694214L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris dosen ini (jejak audit ringan).
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna pengubah terakhir. Nilai {@code null} atau kosong <b>diabaikan</b> supaya
	 * jejak audit yang sudah ada tidak tertimpa oleh proses yang tidak membawa konteks pengguna.
	 *
	 * @param olehId ID pengguna pengubah; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * null/kosong diabaikan agar tidak menghapus jejak yang sudah tercatat.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris dosen ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pencatatan stempel waktu/pengguna ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum Hibernate
	 * menulis UPDATE untuk entity ini. Tidak dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (kolom {@code TIMESTAMP}). Nilai awalnya diisi
	 * waktu pembuatan objek lewat {@link ais.ui.util.WaktuUtil#getDate()} dan diperbarui oleh
	 * {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log dan komponen ZK berformat {@code id-nidn-nama}. Membaca field
	 * mentah (bukan getter) sehingga aman dipanggil pada objek yang belum sepenuhnya terinisialisasi.
	 *
	 * @return gabungan {@code id}, {@code nidn}, dan {@code nama} dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nidn + "-" + nama;
	}

	private String code;
	private String mycode;
	private String nidn;
	private String niyNigk;
	private String nuptk;
	private String gelarDepan;
	private String nomorSertifikasi;
	private String gelarBelakang;
	private String lockId;
	private Boolean aktif;

	private String skCpns;
	private Date tglSkCpns;
	private Date tmtPns;
	private String skAngkat;
	private Date tmtSkAngkat;

	private Boolean aLisensiKepsek;
	private Integer jmlSekolahBinaan;
	private Boolean aDiklatAwas;
	private String aktaIjinAjar;
	private String nira;
	private Boolean aBraille;
	private Boolean aBhsIsyarat;
	private String npwp;

	private String ktp;
	private String nama;
	private String alamat;
	private String email;
	private String telp;
	private String hp;
	private String kelamin;
	private String tempatlahir;
	private String pangkat;
	private String golongan;
	private Golongan golonganPegawai;
	private GolonganPns golonganPns;
	private Pendidikan pendidikan;

	private String jabatan;
	private String spesialisasi1;
	private String spesialisasi2;
	private String spesialisasi3;
	private Date tanggallahir;
	private Boolean milikUniversitas;

	// ini Tambahan kolom
	private Jurusan jurusan;
	private Fakultas fakultas;
	private PerguruanTinggi perguruanTinggi;
	private EpsbedJabatanAkademik jabatanAkademik;
	private EpsbedStatusAktivitasDosen statusAktivitasDosen;
	private IkatanKerjaDosen ikatanKerjaDosen;
	private StatusKepegawaian statusKepegawaian;
	private JenisPendidikDanTenagaKependidikan jenisPendidikDanTenagaKependidikan;
	private LembagaPengangkat lembagaPengangkat;
	private SumberGaji sumberGaji;
	private JabatanFungsionalDosen jabatanFungsionalDosen;

	private Integer tetap = 1;
	private Boolean sertifikasi;
	private Jabatan spesifikasiJabatan;
	private Jabatan spesifikasiJabatanPtLain;

	private StatusPegawai statusPegawai;

	private String feeder;
	private String idRegPtk;
	private String bahasa;

	private String pendidikans1;
	private String pendidikans2;
	private String pendidikans3;

	// private Pegawai pegawai;
	private Long pegawaiId;
	private StatusKewajibanBebanDosen statusKewajibanBebanDosen;
	private Boolean sesuaiBidangKeilmuan;

	private Ruang ruang;
	private Long atasanlangsung;

	private String idfinger;
	private String googleScholar;

	private Integer onlineMenggunakan;
	private String onlineLink;
	private String kodeSinta;
	public BiodataDosen biodataDosen;
	private String formula;

	public static Integer TIDAK_AKTIF = 0;
	public static Integer JITSI = 1;
	public static Integer GOOGLE_MEET = 2;
	public static Integer ZOOM = 3;
	public static Integer BBB = 4;
	public static Integer SKYPE = 5;
	public static Integer WA = 6;
	public static Integer LAIN = 7;

	/**
	 * Mengembalikan platform kelas daring yang dipakai dosen ini. Bila kolom masih kosong, field
	 * diisi (write-back) dengan {@link #TIDAK_AKTIF} lebih dulu supaya pemanggil tidak perlu
	 * menangani {@code null}.
	 *
	 * @return salah satu konstanta {@link #TIDAK_AKTIF}, {@link #JITSI}, {@link #GOOGLE_MEET},
	 *         {@link #ZOOM}, {@link #BBB}, {@link #SKYPE}, {@link #WA}, atau {@link #LAIN}
	 */
	public Integer getOnlineMenggunakan() {
		if (onlineMenggunakan == null) {
			onlineMenggunakan = TIDAK_AKTIF;
		}
		return onlineMenggunakan;
	}

	/**
	 * Mengisi platform kelas daring yang dipakai dosen ini.
	 *
	 * @param onlineMenggunakan salah satu konstanta platform pada kelas ini
	 */
	public void setOnlineMenggunakan(Integer onlineMenggunakan) {
		this.onlineMenggunakan = onlineMenggunakan;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 */
	public Dosen() {
	}

	/**
	 * Konstruktor pintasan yang hanya mengisi kunci primer — dipakai sebagai objek referensi ringan
	 * pada kriteria pencarian dan pembandingan tanpa memuat seluruh baris.
	 *
	 * @param id kunci primer dosen
	 */
	public Dosen(Long id) {
		this.id = id;
	}

	/**
	 * Konstruktor peninggalan hbm2java untuk kolom-kolom wajib versi awal skema. Hanya mengisi data
	 * identitas dasar; seluruh relasi (jurusan, jabatan, kepegawaian) tetap kosong.
	 *
	 * @param nama         nama dosen
	 * @param alamat       alamat tempat tinggal
	 * @param email        alamat surel
	 * @param telp         nomor telepon
	 * @param kelamin      jenis kelamin (dinormalkan oleh {@link #getKelamin()})
	 * @param tempatlahir  tempat lahir
	 * @param tanggallahir tanggal lahir
	 */
	public Dosen(String nama, String alamat, String email, String telp, String kelamin, String tempatlahir,
			Date tanggallahir) {
		this.nama = nama;
		this.alamat = alamat;
		this.email = email;
		this.telp = telp;
		this.kelamin = kelamin;
		this.tempatlahir = tempatlahir;
		this.tanggallahir = tanggallahir;
	}

	/**
	 * Kunci primer dosen (kolom {@code id}, {@code IDENTITY}).
	 *
	 * @return kunci primer, atau {@code null} bila entity belum tersimpan
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci primer. Umumnya hanya dipanggil Hibernate atau saat membuat objek referensi.
	 *
	 * @param id kunci primer dosen
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama dosen yang sudah dipangkas spasi, tidak pernah {@code null}.
	 *
	 * <p>Bila kolom nama kosong, nilai dicari ke berkas cache {@code GeneralValueObject.retreive("nama")}
	 * sebagai cadangan — berguna untuk baris lama yang kolom {@code nama}-nya belum terisi. Method ini
	 * <b>menutupi</b> {@link Karyawan#getNama()} yang mengembalikan {@code code + "-" + nama}.</p>
	 *
	 * @return nama dosen tanpa spasi berlebih, atau string kosong bila tidak ada
	 */
	public String getNama() {
		try {
			String s = nama == null || nama.trim().isEmpty() ? retreive("nama") : nama;
			return s == null ? "" : s.trim();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:277");
			// TODO: handle exception
		}
		return nama == null ? "" : nama.trim();
	}

	/**
	 * Mengisi nama dosen sekaligus mencerminkannya ke berkas cache lewat
	 * {@code GeneralValueObject.put(nilai, "nama")} agar {@link #getNama()} tetap punya cadangan.
	 * Nilai null/kosong diabaikan (tidak menghapus nama yang sudah ada).
	 *
	 * @param nama nama dosen; diabaikan bila null/kosong
	 */
	public void setNama(String nama) {
		if (nama != null && !nama.trim().isEmpty()) {
			put(nama.trim(), "nama");
			this.nama = nama;
		}
	}

	/**
	 * Alamat tempat tinggal dosen (kolom {@code alamat}).
	 *
	 * @return alamat, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat")
	public String getAlamat() {
		return this.alamat;
	}

	/**
	 * Mengisi alamat tempat tinggal dosen.
	 *
	 * @param alamat alamat tempat tinggal
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan alamat surel dosen, yang dapat berisi <b>beberapa</b> alamat dipisah koma.
	 *
	 * <p>Getter ini membersihkan diri: koma ganda hasil penggabungan berulang (lihat
	 * {@link #appendEmail(String)}) dirapikan sampai lima kali lipat, {@code null} diubah menjadi
	 * string kosong, dan nilai yang hanya berisi {@code ","} dikosongkan. Perubahan tersebut
	 * <b>ditulis balik</b> ke field sehingga ikut tersimpan pada flush Hibernate berikutnya.</p>
	 *
	 * @return daftar surel dipisah koma; string kosong bila tidak ada
	 */
	@Column(name = "email", length = 255)
	public String getEmail() {
		if (email != null && email.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				email = email.replaceAll(",,", ",");
			}
		}
		if (email == null) {
			email = "";
		}
		if (email.trim().equals(",")) {
			email = "";
		}
		return this.email;
	}

	/**
	 * Mengganti seluruh isi kolom surel (menimpa daftar yang ada).
	 *
	 * @param email satu alamat surel atau beberapa alamat dipisah koma
	 * @see #appendEmail(String)
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Menambahkan satu alamat surel ke daftar tanpa menghapus yang sudah ada.
	 *
	 * <p>Penambahan dilewati bila alamat null/kosong, sudah terkandung di daftar (cek
	 * {@code StringUtils.contains}), tidak lolos {@code Common.isValidEmailAddress}, atau diawali
	 * {@code "@"}. Alamat pertama disimpan apa adanya, selebihnya digabung dengan pemisah koma.</p>
	 *
	 * @param email alamat surel yang akan ditambahkan
	 * @see #getEmail()
	 */
	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	/**
	 * Nomor telepon dosen (kolom {@code telp}).
	 *
	 * @return nomor telepon, atau {@code null} bila belum diisi
	 */
	@Column(name = "telp", length = 100)
	public String getTelp() {
		return this.telp;
	}

	/**
	 * Mengisi nomor telepon dosen.
	 *
	 * @param telp nomor telepon
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Mengembalikan jenis kelamin dalam bentuk baku {@code "Laki-laki"} atau {@code "Perempuan"}.
	 *
	 * <p>Data historis di kolom ini beragam ({@code "L"}, {@code "P"}, {@code "Laki-Laki"}, teks bebas
	 * yang memuat "laki"/"puan"). Getter menormalkannya dan <b>menulis balik</b> hasil normalisasi ke
	 * field, sehingga pembacaan saja dapat memicu UPDATE pada flush berikutnya. Nilai {@code null}
	 * dijadikan string kosong agar pemanggil aman memakai {@code toLowerCase()}.</p>
	 *
	 * @return {@code "Laki-laki"}, {@code "Perempuan"}, atau string kosong bila tidak dikenali
	 */
	@Column(name = "kelamin", length = 20)
	public String getKelamin() {

		if (kelamin != null && (kelamin.trim().equalsIgnoreCase("L") || kelamin.trim().equals("Laki-Laki"))) {
			kelamin = "Laki-laki";
		} else if (kelamin != null && kelamin.trim().equalsIgnoreCase("P")) {
			kelamin = "Perempuan";
		} else if (kelamin == null) {
			kelamin = "";
		}

		if (kelamin.toLowerCase().contains("laki")) {
			kelamin = "Laki-laki";
		} else if (kelamin.toLowerCase().contains("puan")) {
			kelamin = "Perempuan";
		}

		return this.kelamin;
	}

	/**
	 * Mengisi jenis kelamin apa adanya; pembakuan dilakukan saat dibaca.
	 *
	 * @param kelamin jenis kelamin (bentuk apa pun, lihat {@link #getKelamin()})
	 */
	public void setKelamin(String kelamin) {
		this.kelamin = kelamin;
	}

	/**
	 * Tempat lahir dosen (kolom {@code tempatlahir}).
	 *
	 * @return tempat lahir, atau {@code null} bila belum diisi
	 */
	@Column(name = "tempatlahir", length = 100)
	public String getTempatlahir() {
		return this.tempatlahir;
	}

	/**
	 * Mengisi tempat lahir dosen.
	 *
	 * @param tempatlahir tempat lahir
	 */
	public void setTempatlahir(String tempatlahir) {
		this.tempatlahir = tempatlahir;
	}

	/**
	 * Tanggal lahir dosen (kolom {@code tanggallahir}, bertipe {@code DATE}).
	 *
	 * @return tanggal lahir, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggallahir", length = 0)
	public Date getTanggallahir() {
		return this.tanggallahir;
	}

	/**
	 * Mengisi tanggal lahir dosen.
	 *
	 * @param tanggallahir tanggal lahir
	 */
	public void setTanggallahir(Date tanggallahir) {
		this.tanggallahir = tanggallahir;
	}

	/**
	 * Mengisi pangkat kepegawaian (teks bebas, mis. "Penata Muda Tingkat I").
	 *
	 * @param pangkat nama pangkat
	 */
	public void setPangkat(String pangkat) {
		this.pangkat = pangkat;
	}

	/**
	 * Pangkat kepegawaian dosen (kolom {@code pangkat}).
	 *
	 * @return nama pangkat, atau {@code null} bila belum diisi
	 * @see #getGolongan()
	 */
	@Column(name = "pangkat", length = 255)
	public String getPangkat() {
		return pangkat;
	}

	/**
	 * Menetapkan jurusan/program studi tempat dosen bernaung.
	 *
	 * @param jurusan jurusan induk dosen
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Jurusan/program studi induk dosen (relasi {@code ManyToOne} lazy ke kolom {@code jurusan}).
	 * Nilai dilewatkan {@code check(...)} milik {@link GeneralValueObject} agar proxy lazy yang sudah
	 * tidak terhubung ke session tetap aman dipakai.
	 *
	 * @return jurusan induk, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan fakultas dosen secara eksplisit. Perlu diketahui bahwa {@link #getFakultas()} akan
	 * menimpanya dengan fakultas milik {@link #getJurusan()} bila jurusan terisi.
	 *
	 * @param fakultas fakultas induk dosen
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Fakultas induk dosen, dengan <b>penurunan otomatis dari jurusan</b>: bila {@link #getJurusan()}
	 * tidak null, nilai kolom {@code fakultas} ditimpa oleh {@code jurusan.getFakultas()} dan ditulis
	 * balik ke field. Jadi jurusan adalah sumber kebenaran; kolom fakultas hanya cadangan untuk dosen
	 * yang tidak terikat jurusan.
	 *
	 * @return fakultas induk, atau {@code null} bila tidak dapat ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		jurusan = getJurusan();
		if (jurusan != null) {
			fakultas = jurusan.getFakultas();
		}
		return fakultas;
	}

	/**
	 * Mengisi kode dosen (kolom {@code code}, mis. kode singkat pada jadwal).
	 *
	 * @param code kode dosen
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Kode dosen yang sudah dipangkas spasi dan tidak pernah {@code null}.
	 *
	 * @return kode dosen, atau string kosong bila belum diisi
	 */
	@Column(name = "code", length = 50)
	public String getCode() {
		return code == null ? "" : code.trim();
	}

	/**
	 * Mengisi penanda dosen tetap secara langsung. Perlu diketahui bahwa {@link #getTetap()} akan
	 * menimpanya berdasarkan {@link #getIkatanKerjaDosen()} bila relasi itu terisi.
	 *
	 * @param tetap {@code 1} untuk dosen tetap, {@code 0} untuk tidak tetap
	 */
	public void setTetap(Integer tetap) {
		this.tetap = tetap;
	}

	/**
	 * Penanda dosen tetap ({@code 1}) atau tidak tetap ({@code 0}), <b>diturunkan</b> dari
	 * {@link #getIkatanKerjaDosen()} bila relasi tersebut ada — nilai kolom lama otomatis diselaraskan
	 * dan ditulis balik ke field. Bila ikatan kerja gagal dibaca (mis. proxy lazy putus session)
	 * kegagalan direkam ke {@code ErrorAuditUtil} dan nilai kolom dipakai apa adanya; bila kolom pun
	 * kosong, nilai baku {@code 1} (tetap) dipakai.
	 *
	 * @return {@code 1} bila dosen tetap, {@code 0} bila tidak tetap; tidak pernah {@code null}
	 */
	@Column(name = "tetap", length = 1)
	public Integer getTetap() {
		try {
			ikatanKerjaDosen = getIkatanKerjaDosen();
			if (ikatanKerjaDosen != null) {
				tetap = ikatanKerjaDosen.getTetap() ? 1 : 0;
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:436");

		}

		if (tetap == null) {
			tetap = 1;
		}

		return tetap;
	}

	/**
	 * Mengisi kode internal alternatif dosen (kolom {@code mycode}), dipakai sebagai kode cadangan
	 * oleh {@link #ambilKode()} ketika NIDN kosong.
	 *
	 * @param mycode kode internal dosen
	 */
	public void setMycode(String mycode) {
		this.mycode = mycode;
	}

	/**
	 * Kode internal alternatif dosen yang sudah dipangkas spasi dan tidak pernah {@code null}.
	 *
	 * @return kode internal, atau string kosong bila belum diisi
	 * @see #ambilKode()
	 */
	public String getMycode() {
		return mycode == null ? "" : mycode.trim();
	}

	/**
	 * Mengisi NIDN (Nomor Induk Dosen Nasional).
	 *
	 * @param nidn NIDN dosen
	 */
	public void setNidn(String nidn) {
		this.nidn = nidn;
	}

	/**
	 * NIDN (Nomor Induk Dosen Nasional) yang sudah dipangkas spasi dan tidak pernah {@code null}.
	 * Dipakai sebagai identitas utama pada pelaporan dan pada QR tanda tangan ({@link #ttdQr()}).
	 *
	 * @return NIDN, atau string kosong bila belum diisi
	 */
	public String getNidn() {
		return nidn == null ? "" : nidn.trim();
	}

	/**
	 * Mengisi keterangan jabatan berbentuk teks bebas (berbeda dari relasi
	 * {@link #getSpesifikasiJabatan()} dan {@link #getJabatanFungsionalDosen()}).
	 *
	 * @param jabatan keterangan jabatan
	 */
	public void setJabatan(String jabatan) {
		this.jabatan = jabatan;
	}

	/**
	 * Keterangan jabatan berbentuk teks bebas (kolom {@code jabatan}).
	 *
	 * @return keterangan jabatan, atau {@code null} bila belum diisi
	 */
	public String getJabatan() {
		return jabatan;
	}

	/**
	 * Mengisi bidang spesialisasi/keahlian pertama.
	 *
	 * @param spesialisasi1 bidang keahlian pertama
	 */
	public void setSpesialisasi1(String spesialisasi1) {
		this.spesialisasi1 = spesialisasi1;
	}

	/**
	 * Bidang spesialisasi/keahlian pertama, tidak pernah {@code null}.
	 *
	 * @return bidang keahlian pertama, atau string kosong bila belum diisi
	 */
	public String getSpesialisasi1() {
		return spesialisasi1 == null ? "" : spesialisasi1;
	}

	/**
	 * Mengisi bidang spesialisasi/keahlian kedua.
	 *
	 * @param spesialisasi2 bidang keahlian kedua
	 */
	public void setSpesialisasi2(String spesialisasi2) {
		this.spesialisasi2 = spesialisasi2;
	}

	/**
	 * Bidang spesialisasi/keahlian kedua, tidak pernah {@code null}.
	 *
	 * @return bidang keahlian kedua, atau string kosong bila belum diisi
	 */
	public String getSpesialisasi2() {
		return spesialisasi2 == null ? "" : spesialisasi2;
	}

	/**
	 * Mengisi bidang spesialisasi/keahlian ketiga.
	 *
	 * @param spesialisasi3 bidang keahlian ketiga
	 */
	public void setSpesialisasi3(String spesialisasi3) {
		this.spesialisasi3 = spesialisasi3;
	}

	/**
	 * Bidang spesialisasi/keahlian ketiga, tidak pernah {@code null}.
	 *
	 * @return bidang keahlian ketiga, atau string kosong bila belum diisi
	 */
	public String getSpesialisasi3() {
		return spesialisasi3 == null ? "" : spesialisasi3;
	}

	/**
	 * Menetapkan jabatan struktural dosen di perguruan tinggi sendiri.
	 *
	 * @param spesifikasiJabatan jabatan struktural
	 */
	public void setSpesifikasiJabatan(Jabatan spesifikasiJabatan) {
		this.spesifikasiJabatan = spesifikasiJabatan;
	}

	/**
	 * Jabatan struktural dosen di perguruan tinggi sendiri (relasi lazy ke kolom
	 * {@code spesifikasi_jabatan}), dilewatkan {@code check(...)} agar proxy lazy aman dipakai.
	 *
	 * @return jabatan struktural, atau {@code null} bila tidak menjabat
	 * @see #getSpesifikasiJabatanPtLain()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "spesifikasi_jabatan", nullable = true)
	public Jabatan getSpesifikasiJabatan() {
		spesifikasiJabatan = check(spesifikasiJabatan);
		return spesifikasiJabatan;
	}

	/**
	 * Mengisi nama golongan sebagai teks. Perlu diketahui bahwa {@link #getGolongan()} akan
	 * menimpanya dari relasi golongan bila salah satunya terisi.
	 *
	 * @param golongan nama golongan (mis. "III/b")
	 */
	public void setGolongan(String golongan) {
		this.golongan = golongan;
	}

	/**
	 * Nama golongan kepegawaian sebagai teks, <b>diturunkan</b> dengan urutan prioritas:
	 * {@link #getGolonganPns()} lebih dulu, lalu {@link #getGolonganPegawai()}; bila keduanya kosong
	 * barulah nilai kolom teks dipakai. Hasil turunan ditulis balik ke field.
	 *
	 * @return nama golongan, atau {@code null} bila tidak dapat ditentukan
	 */
	public String getGolongan() {
		if (getGolonganPns() != null) {
			golongan = getGolonganPns().getNama();
		} else if (getGolonganPegawai() != null) {
			golongan = getGolonganPegawai().getNama();
		}
		return golongan;
	}

	/**
	 * Golongan kepegawaian internal (modul {@code employ}) pada relasi lazy kolom
	 * {@code golongan_pegawai}, dilewatkan {@code check(...)}.
	 *
	 * @return golongan pegawai, atau {@code null} bila tidak diisi
	 * @see #getGolonganPns()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "golongan_pegawai", nullable = true)
	public Golongan getGolonganPegawai() {
		golonganPegawai = check(golonganPegawai);
		return golonganPegawai;
	}

	/**
	 * Menetapkan golongan kepegawaian internal dosen.
	 *
	 * @param golonganPegawai golongan pegawai
	 */
	public void setGolonganPegawai(Golongan golonganPegawai) {
		this.golonganPegawai = golonganPegawai;
	}

	/**
	 * Membandingkan dua dosen berdasarkan nama secara <b>menurun</b> (perhatikan urutan operand:
	 * {@code o.nama.compareTo(nama)}), sehingga {@code Collections.sort} tanpa pembalik menghasilkan
	 * daftar Z→A. Objek yang bukan {@code Dosen} atau yang salah satu namanya {@code null} dianggap
	 * setara ({@code 0}) agar pengurutan tidak melempar exception pada data tidak lengkap.
	 *
	 * @param arg0 objek pembanding
	 * @return nilai negatif/nol/positif hasil pembandingan nama secara terbalik
	 * @see GeneralValueObject#compareTo(GeneralValueObject)
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		if (arg0 instanceof Dosen) {
			Dosen o = (Dosen) arg0;
			if (nama == null || o.nama == null) {
				return 0;
			} else {
				return o.nama.compareTo(nama);
			}
		}
		return 0;
	}

	/**
	 * Menetapkan jabatan akademik versi kodifikasi EPSBED/PDDikti untuk keperluan pelaporan.
	 *
	 * @param jabatanAkademik jabatan akademik EPSBED
	 */
	public void setJabatanAkademik(EpsbedJabatanAkademik jabatanAkademik) {
		this.jabatanAkademik = jabatanAkademik;
	}

	/**
	 * Jabatan akademik menurut kodifikasi EPSBED/PDDikti (relasi lazy kolom
	 * {@code epsbed_jabatan_akademik}), dilewatkan {@code check(...)} agar proxy lazy aman dipakai.
	 * Berbeda dari {@link #getJabatanFungsionalDosen()} yang merupakan master jabatan fungsional
	 * internal; keduanya dipakai bersama pada layar biodata dan berkas pelaporan.
	 *
	 * @return jabatan akademik EPSBED, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "epsbed_jabatan_akademik")
	public EpsbedJabatanAkademik getJabatanAkademik() {
		jabatanAkademik = check(jabatanAkademik);
		return jabatanAkademik;
	}

	/**
	 * Menetapkan status aktivitas dosen versi kodifikasi EPSBED/PDDikti (aktif, tugas belajar,
	 * cuti, keluar, dan sejenisnya).
	 *
	 * @param statusAktivitasDosen status aktivitas EPSBED
	 */
	public void setStatusAktivitasDosen(EpsbedStatusAktivitasDosen statusAktivitasDosen) {
		this.statusAktivitasDosen = statusAktivitasDosen;
	}

	/**
	 * Status aktivitas dosen menurut kodifikasi EPSBED/PDDikti (relasi lazy kolom
	 * {@code epsbed_status_aktivitas}), dilewatkan {@code check(...)}. Dipakai pada berkas pelaporan
	 * dan penyaringan daftar dosen yang layak mengampu.
	 *
	 * @return status aktivitas EPSBED, atau {@code null} bila belum diisi
	 * @see #getStatusPegawai()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "epsbed_status_aktivitas")
	public EpsbedStatusAktivitasDosen getStatusAktivitasDosen() {
		statusAktivitasDosen = check(statusAktivitasDosen);
		return statusAktivitasDosen;
	}

	/**
	 * Nomor Kartu Tanda Penduduk (NIK) dosen.
	 *
	 * @return nomor KTP, atau {@code null} bila belum diisi
	 */
	public String getKtp() {
		return ktp;
	}

	/**
	 * Mengisi nomor Kartu Tanda Penduduk (NIK) dosen.
	 *
	 * @param ktp nomor KTP
	 */
	public void setKtp(String ktp) {
		this.ktp = ktp;
	}

	/**
	 * Status kepegawaian umum dosen (relasi lazy kolom {@code status_pegawai}).
	 *
	 * <p>Bila kolom kosong, nilai <b>dijatuhkan ke</b> {@code ConstantValues.AKTIF_PEGAWAI} dan
	 * ditulis balik ke field, sehingga dosen lama yang belum diisi statusnya otomatis dianggap aktif.
	 * Jangan mengandalkan getter ini untuk membedakan "belum diisi" dari "aktif".</p>
	 *
	 * @return status pegawai; tidak pernah {@code null} selama master konstanta sudah dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_pegawai")
	public StatusPegawai getStatusPegawai() {

		statusPegawai = check(statusPegawai);
		if (statusPegawai == null) {
			statusPegawai = ConstantValues.AKTIF_PEGAWAI;
		}
		return statusPegawai;
	}

	/**
	 * Menetapkan status kepegawaian umum dosen (aktif, keluar, pensiun, dan sejenisnya).
	 *
	 * @param statusPegawai status pegawai
	 */
	public void setStatusPegawai(StatusPegawai statusPegawai) {
		this.statusPegawai = statusPegawai;
	}

	/**
	 * Penanda apakah dosen ini milik universitas sendiri (bukan dosen luar/tamu).
	 *
	 * @return {@code true} bila dosen milik universitas; dapat {@code null} bila belum diisi
	 */
	@Column(name = "milik_universitas")
	public Boolean getMilikUniversitas() {
		return milikUniversitas;
	}

	/**
	 * Mengisi penanda kepemilikan dosen oleh universitas sendiri.
	 *
	 * @param milikUniversitas {@code true} bila dosen milik universitas
	 */
	public void setMilikUniversitas(Boolean milikUniversitas) {
		this.milikUniversitas = milikUniversitas;
	}

	/**
	 * Kunci sinkronisasi dosen pada layanan Feeder/PDDikti, sudah dipangkas spasi.
	 * Nilai kosong sengaja dikembalikan sebagai {@code null} agar pemanggil cukup memeriksa null
	 * untuk mengetahui bahwa dosen ini belum tersinkron.
	 *
	 * @return kunci Feeder, atau {@code null} bila belum tersinkron
	 * @see #getIdRegPtk()
	 */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Mengisi kunci sinkronisasi dosen pada layanan Feeder/PDDikti.
	 *
	 * @param feeder kunci Feeder
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Ikatan kerja dosen (relasi lazy kolom {@code ikatan_kerja_dosen}) — sumber kebenaran bagi
	 * penanda {@link #getTetap()}.
	 *
	 * <p>Bila kolom kosong, nilai <b>dijatuhkan ke</b> master {@code ConstantValues.DOSEN_TETAP} atau
	 * {@code ConstantValues.DOSEN_HONORER} berdasarkan nilai kolom {@code tetap} yang lama, lalu
	 * ditulis balik ke field. Ini yang membuat data warisan (yang hanya punya kolom {@code tetap})
	 * tetap tampil benar setelah skema berpindah ke relasi master.</p>
	 *
	 * @return ikatan kerja dosen; tidak pernah {@code null} selama master konstanta sudah dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ikatan_kerja_dosen")
	public IkatanKerjaDosen getIkatanKerjaDosen() {

		ikatanKerjaDosen = check(ikatanKerjaDosen);
		if (ikatanKerjaDosen == null) {
			if (tetap != null && tetap.equals(1)) {
				ikatanKerjaDosen = ConstantValues.DOSEN_TETAP;
			} else {
				ikatanKerjaDosen = ConstantValues.DOSEN_HONORER;
			}
		}
		return ikatanKerjaDosen;
	}

	/**
	 * Menetapkan ikatan kerja dosen (tetap, honorer, dan sejenisnya).
	 *
	 * @param ikatanKerjaDosen ikatan kerja dosen
	 * @see #getTetap()
	 */
	public void setIkatanKerjaDosen(IkatanKerjaDosen ikatanKerjaDosen) {
		this.ikatanKerjaDosen = ikatanKerjaDosen;
	}

	/**
	 * Status kepegawaian menurut master pelaporan (PNS, tetap yayasan, honorer, dan sejenisnya) pada
	 * relasi lazy kolom {@code status_kepegawaian}, dilewatkan {@code check(...)}.
	 *
	 * @return status kepegawaian, atau {@code null} bila belum diisi
	 * @see #getStatusPegawai()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_kepegawaian")
	public StatusKepegawaian getStatusKepegawaian() {
		statusKepegawaian = check(statusKepegawaian);
		return statusKepegawaian;
	}

	/**
	 * Menetapkan status kepegawaian menurut master pelaporan.
	 *
	 * @param statusKepegawaian status kepegawaian
	 */
	public void setStatusKepegawaian(StatusKepegawaian statusKepegawaian) {
		this.statusKepegawaian = statusKepegawaian;
	}

	/**
	 * Jenis pendidik dan tenaga kependidikan (dosen, tenaga kependidikan, instruktur, dan sejenisnya)
	 * pada relasi lazy kolom {@code jenis_pendidik_dan_tenaga_kependidikan}, dilewatkan
	 * {@code check(...)}. Dipakai pada pelaporan Feeder/PDDikti.
	 *
	 * @return jenis pendidik dan tenaga kependidikan, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pendidik_dan_tenaga_kependidikan")
	public JenisPendidikDanTenagaKependidikan getJenisPendidikDanTenagaKependidikan() {
		jenisPendidikDanTenagaKependidikan = check(jenisPendidikDanTenagaKependidikan);
		return jenisPendidikDanTenagaKependidikan;
	}

	/**
	 * Menetapkan jenis pendidik dan tenaga kependidikan.
	 *
	 * @param jenisPendidikDanTenagaKependidikan jenis pendidik dan tenaga kependidikan
	 */
	public void setJenisPendidikDanTenagaKependidikan(
			JenisPendidikDanTenagaKependidikan jenisPendidikDanTenagaKependidikan) {
		this.jenisPendidikDanTenagaKependidikan = jenisPendidikDanTenagaKependidikan;
	}

	/**
	 * Nomor Induk Yayasan / Nomor Induk Guru dan Karyawan (NIY/NIGK) dosen.
	 *
	 * @return NIY/NIGK, atau {@code null} bila belum diisi
	 */
	public String getNiyNigk() {
		return niyNigk;
	}

	/**
	 * Mengisi Nomor Induk Yayasan / Nomor Induk Guru dan Karyawan.
	 *
	 * @param niyNigk NIY/NIGK dosen
	 */
	public void setNiyNigk(String niyNigk) {
		this.niyNigk = niyNigk;
	}

	/**
	 * Nomor Unik Pendidik dan Tenaga Kependidikan (NUPTK) dosen.
	 *
	 * @return NUPTK, atau {@code null} bila belum diisi
	 */
	public String getNuptk() {
		return nuptk;
	}

	/**
	 * Mengisi Nomor Unik Pendidik dan Tenaga Kependidikan (NUPTK).
	 *
	 * @param nuptk NUPTK dosen
	 */
	public void setNuptk(String nuptk) {
		this.nuptk = nuptk;
	}

	/**
	 * Nomor Surat Keputusan pengangkatan sebagai CPNS.
	 *
	 * @return nomor SK CPNS, atau {@code null} bila bukan/belum PNS
	 * @see #getTglSkCpns()
	 */
	public String getSkCpns() {
		return skCpns;
	}

	/**
	 * Mengisi nomor Surat Keputusan pengangkatan CPNS.
	 *
	 * @param skCpns nomor SK CPNS
	 */
	public void setSkCpns(String skCpns) {
		this.skCpns = skCpns;
	}

	/**
	 * Tanggal Surat Keputusan pengangkatan CPNS (kolom bertipe {@code DATE}).
	 *
	 * @return tanggal SK CPNS, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTglSkCpns() {
		return tglSkCpns;
	}

	/**
	 * Mengisi tanggal Surat Keputusan pengangkatan CPNS.
	 *
	 * @param tglSkCpns tanggal SK CPNS
	 */
	public void setTglSkCpns(Date tglSkCpns) {
		this.tglSkCpns = tglSkCpns;
	}

	/**
	 * Nomor Surat Keputusan pengangkatan sebagai dosen (bukan SK CPNS).
	 *
	 * @return nomor SK pengangkatan, atau {@code null} bila belum diisi
	 * @see #getTmtSkAngkat()
	 * @see #getLembagaPengangkat()
	 */
	public String getSkAngkat() {
		return skAngkat;
	}

	/**
	 * Mengisi nomor Surat Keputusan pengangkatan sebagai dosen.
	 *
	 * @param skAngkat nomor SK pengangkatan
	 */
	public void setSkAngkat(String skAngkat) {
		this.skAngkat = skAngkat;
	}

	/**
	 * TMT (terhitung mulai tanggal) berlakunya Surat Keputusan pengangkatan sebagai dosen.
	 *
	 * @return tanggal mulai berlaku SK pengangkatan, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTmtSkAngkat() {
		return tmtSkAngkat;
	}

	/**
	 * Mengisi TMT berlakunya Surat Keputusan pengangkatan sebagai dosen.
	 *
	 * @param tmtSkAngkat tanggal mulai berlaku SK pengangkatan
	 */
	public void setTmtSkAngkat(Date tmtSkAngkat) {
		this.tmtSkAngkat = tmtSkAngkat;
	}

	/**
	 * Lembaga yang mengangkat dosen ini (kementerian, yayasan, pemerintah daerah, dan sejenisnya)
	 * pada relasi lazy kolom {@code lembaga_pengangkat}, dilewatkan {@code check(...)}.
	 *
	 * @return lembaga pengangkat, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lembaga_pengangkat")
	public LembagaPengangkat getLembagaPengangkat() {
		lembagaPengangkat = check(lembagaPengangkat);
		return lembagaPengangkat;
	}

	/**
	 * Menetapkan lembaga yang mengangkat dosen ini.
	 *
	 * @param lembagaPengangkat lembaga pengangkat
	 */
	public void setLembagaPengangkat(LembagaPengangkat lembagaPengangkat) {
		this.lembagaPengangkat = lembagaPengangkat;
	}

	/**
	 * Sumber gaji dosen (APBN, yayasan, mandiri, dan sejenisnya) pada relasi lazy kolom
	 * {@code sumber_gaji}, dilewatkan {@code check(...)}. Termasuk data wajib pelaporan PDDikti.
	 *
	 * @return sumber gaji, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sumber_gaji")
	public SumberGaji getSumberGaji() {
		sumberGaji = check(sumberGaji);
		return sumberGaji;
	}

	/**
	 * Menetapkan sumber gaji dosen.
	 *
	 * @param sumberGaji sumber gaji
	 */
	public void setSumberGaji(SumberGaji sumberGaji) {
		this.sumberGaji = sumberGaji;
	}

	/**
	 * TMT (terhitung mulai tanggal) status PNS dosen.
	 *
	 * @return tanggal mulai status PNS, atau {@code null} bila bukan PNS
	 */
	@Temporal(TemporalType.DATE)
	public Date getTmtPns() {
		return tmtPns;
	}

	/**
	 * Mengisi TMT status PNS dosen.
	 *
	 * @param tmtPns tanggal mulai status PNS
	 */
	public void setTmtPns(Date tmtPns) {
		this.tmtPns = tmtPns;
	}

	/**
	 * Penanda kepemilikan lisensi kepala sekolah (kolom warisan domain sekolah/pengawas yang ikut
	 * menumpang pada tabel dosen). Bila kolom kosong, nilai diisi {@code false} dan ditulis balik ke
	 * field sehingga tidak pernah mengembalikan {@code null}.
	 *
	 * @return {@code true} bila dosen memiliki lisensi kepala sekolah
	 */
	public Boolean getaLisensiKepsek() {
		if (aLisensiKepsek == null) {
			aLisensiKepsek = false;
		}
		return aLisensiKepsek;
	}

	/**
	 * Mengisi penanda kepemilikan lisensi kepala sekolah.
	 *
	 * @param aLisensiKepsek {@code true} bila memiliki lisensi kepala sekolah
	 */
	public void setaLisensiKepsek(Boolean aLisensiKepsek) {
		this.aLisensiKepsek = aLisensiKepsek;
	}

	/**
	 * Jumlah sekolah binaan (kolom warisan domain pengawas sekolah). Bila kolom kosong, nilai diisi
	 * {@code 0} dan ditulis balik ke field sehingga tidak pernah {@code null}.
	 *
	 * @return jumlah sekolah binaan; minimal {@code 0}
	 */
	public Integer getJmlSekolahBinaan() {
		if (jmlSekolahBinaan == null) {
			jmlSekolahBinaan = 0;
		}
		return jmlSekolahBinaan;
	}

	/**
	 * Mengisi jumlah sekolah binaan.
	 *
	 * @param jmlSekolahBinaan jumlah sekolah binaan
	 */
	public void setJmlSekolahBinaan(Integer jmlSekolahBinaan) {
		this.jmlSekolahBinaan = jmlSekolahBinaan;
	}

	/**
	 * Penanda pernah mengikuti diklat pengawas (kolom warisan domain sekolah/pengawas). Bila kolom
	 * kosong, nilai diisi {@code false} dan ditulis balik ke field.
	 *
	 * @return {@code true} bila pernah mengikuti diklat pengawas
	 */
	public Boolean getaDiklatAwas() {
		if (aDiklatAwas == null) {
			aDiklatAwas = false;
		}
		return aDiklatAwas;
	}

	/**
	 * Mengisi penanda pernah mengikuti diklat pengawas.
	 *
	 * @param aDiklatAwas {@code true} bila pernah mengikuti diklat pengawas
	 */
	public void setaDiklatAwas(Boolean aDiklatAwas) {
		this.aDiklatAwas = aDiklatAwas;
	}

	/**
	 * Nomor akta/izin mengajar yang dimiliki dosen.
	 *
	 * @return nomor akta izin mengajar, atau {@code null} bila belum diisi
	 */
	public String getAktaIjinAjar() {
		return aktaIjinAjar;
	}

	/**
	 * Mengisi nomor akta/izin mengajar.
	 *
	 * @param aktaIjinAjar nomor akta izin mengajar
	 */
	public void setAktaIjinAjar(String aktaIjinAjar) {
		this.aktaIjinAjar = aktaIjinAjar;
	}

	/**
	 * Nomor Induk Registrasi Asesor (NIRA) bila dosen berperan sebagai asesor.
	 *
	 * @return NIRA, atau {@code null} bila bukan asesor
	 */
	public String getNira() {
		return nira;
	}

	/**
	 * Mengisi Nomor Induk Registrasi Asesor (NIRA).
	 *
	 * @param nira NIRA dosen
	 */
	public void setNira(String nira) {
		this.nira = nira;
	}

	/**
	 * Penanda penguasaan huruf Braille (data kompetensi pendidikan inklusif). Bila kolom kosong,
	 * nilai diisi {@code false} dan ditulis balik ke field.
	 *
	 * @return {@code true} bila dosen menguasai huruf Braille
	 */
	public Boolean getaBraille() {
		if (aBraille == null) {
			aBraille = false;
		}
		return aBraille;
	}

	/**
	 * Mengisi penanda penguasaan huruf Braille.
	 *
	 * @param aBraille {@code true} bila menguasai huruf Braille
	 */
	public void setaBraille(Boolean aBraille) {
		this.aBraille = aBraille;
	}

	/**
	 * Penanda penguasaan bahasa isyarat (data kompetensi pendidikan inklusif). Bila kolom kosong,
	 * nilai diisi {@code false} dan ditulis balik ke field.
	 *
	 * @return {@code true} bila dosen menguasai bahasa isyarat
	 */
	public Boolean getaBhsIsyarat() {
		if (aBhsIsyarat == null) {
			aBhsIsyarat = false;
		}
		return aBhsIsyarat;
	}

	/**
	 * Mengisi penanda penguasaan bahasa isyarat.
	 *
	 * @param aBhsIsyarat {@code true} bila menguasai bahasa isyarat
	 */
	public void setaBhsIsyarat(Boolean aBhsIsyarat) {
		this.aBhsIsyarat = aBhsIsyarat;
	}

	/**
	 * Nomor Pokok Wajib Pajak (NPWP) dosen.
	 *
	 * @return NPWP, atau {@code null} bila belum diisi
	 */
	public String getNpwp() {
		return npwp;
	}

	/**
	 * Mengisi Nomor Pokok Wajib Pajak (NPWP).
	 *
	 * @param npwp NPWP dosen
	 */
	public void setNpwp(String npwp) {
		this.npwp = npwp;
	}

	/**
	 * Perguruan tinggi tempat dosen bernaung, dengan <b>rantai fallback berlapis</b> karena kolom ini
	 * sering kosong pada data warisan:
	 *
	 * <ol>
	 * <li>nilai kolom {@code perguruan_tinggi} (dilewatkan {@code check(...)});</li>
	 * <li>bila kosong, perguruan tinggi aktif dari
	 * {@link ais.action.master.helper.util.PerguruanTinggiUtil#getPerguruanTinggi()};</li>
	 * <li>bila {@link #getFakultas()} punya perguruan tinggi, nilai itu <b>menimpa</b> hasil langkah
	 * sebelumnya (fakultas dianggap lebih spesifik);</li>
	 * <li>bila masih kosong, {@code PerguruanTinggiUtil} dicoba sekali lagi.</li>
	 * </ol>
	 *
	 * <p>Kegagalan pada langkah util direkam ke {@code ErrorAuditUtil} dan tidak dilempar. Hasil
	 * ditulis balik ke field; perguruan tinggi yang belum punya {@code id} dianggap tidak ada dan
	 * dikembalikan sebagai {@code null}.</p>
	 *
	 * @return perguruan tinggi dosen, atau {@code null} bila tidak dapat ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi")
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:815");
		}
		if (getFakultas() != null && getFakultas().getPerguruanTinggi() != null) {
			perguruanTinggi = getFakultas().getPerguruanTinggi();
		}
		if (perguruanTinggi == null) {
			try {
				perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:823");
				// TODO: handle exception
			}
		}
		return perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi;
	}

	/**
	 * Menetapkan perguruan tinggi dosen secara eksplisit. Perhatikan bahwa
	 * {@link #getPerguruanTinggi()} dapat menimpanya dengan perguruan tinggi milik fakultas.
	 *
	 * @param perguruanTinggi perguruan tinggi dosen
	 */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/**
	 * ID registrasi PTK (pendidik dan tenaga kependidikan) pada layanan pelaporan, sudah dipangkas
	 * spasi; nilai kosong dikembalikan sebagai {@code null}.
	 *
	 * @return ID registrasi PTK, atau {@code null} bila belum tersinkron
	 * @see #getFeeder()
	 */
	public String getIdRegPtk() {
		return idRegPtk == null || idRegPtk.trim().isEmpty() ? null : idRegPtk.trim();
	}

	/**
	 * Mengisi ID registrasi PTK pada layanan pelaporan.
	 *
	 * @param idRegPtk ID registrasi PTK
	 */
	public void setIdRegPtk(String idRegPtk) {
		this.idRegPtk = idRegPtk;
	}

	/**
	 * Gelar akademik yang ditulis di depan nama (mis. "Dr.", "Prof.").
	 *
	 * @return gelar depan, atau {@code null} bila tidak ada
	 */
	public String getGelarDepan() {
		return gelarDepan;
	}

	/**
	 * Mengisi gelar akademik yang ditulis di depan nama.
	 *
	 * @param gelarDepan gelar depan
	 */
	public void setGelarDepan(String gelarDepan) {
		this.gelarDepan = gelarDepan;
	}

	/**
	 * Gelar akademik yang ditulis di belakang nama (mis. "S.Kom., M.T.").
	 *
	 * @return gelar belakang, atau {@code null} bila tidak ada
	 */
	public String getGelarBelakang() {
		return gelarBelakang;
	}

	/**
	 * Mengisi gelar akademik yang ditulis di belakang nama.
	 *
	 * @param gelarBelakang gelar belakang
	 */
	public void setGelarBelakang(String gelarBelakang) {
		this.gelarBelakang = gelarBelakang;
	}

	/**
	 * Jabatan fungsional dosen (asisten ahli, lektor, lektor kepala, guru besar) pada relasi lazy
	 * kolom {@code jabatan_fungsional_dosen}, dilewatkan {@code check(...)}. Master inilah yang
	 * membawa bobot/angka kredit dan dipakai pada perhitungan kepangkatan serta pelaporan.
	 *
	 * @return jabatan fungsional dosen, atau {@code null} bila belum memiliki jabatan fungsional
	 * @see #getJabatanAkademik()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_fungsional_dosen", nullable = true)
	public JabatanFungsionalDosen getJabatanFungsionalDosen() {
		jabatanFungsionalDosen = check(jabatanFungsionalDosen);
		return jabatanFungsionalDosen;
	}

	/**
	 * Menetapkan jabatan fungsional dosen.
	 *
	 * @param jabatanFungsionalDosen jabatan fungsional dosen
	 */
	public void setJabatanFungsionalDosen(JabatanFungsionalDosen jabatanFungsionalDosen) {
		this.jabatanFungsionalDosen = jabatanFungsionalDosen;
	}

	/**
	 * Penanda dosen masih aktif dipakai sistem. Bila kolom kosong, nilai diisi {@code true} dan
	 * ditulis balik ke field sehingga tidak pernah {@code null}.
	 *
	 * <p>Blok komentar di dalam method menyimpan turunan lama dari {@link #getStatusPegawai()} yang
	 * sengaja dinonaktifkan; status kepegawaian dan penanda aktif kini dikelola terpisah.</p>
	 *
	 * @return {@code true} bila dosen dianggap aktif
	 */
	public Boolean getAktif() {

		// if (statusPegawai != null && statusPegawai.getNama() != null &&
		// !statusPegawai.getNama().trim().isEmpty()) {
		// aktif = statusPegawai.getNama().toLowerCase().startsWith("aktif");
		// }

		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * ID baris {@link Pegawai} yang merepresentasikan dosen ini di modul kepegawaian — jembatan antar
	 * modul yang sengaja disimpan sebagai angka, bukan relasi Hibernate, agar tidak memaksa kedua
	 * modul saling bergantung. Dipakai antara lain oleh layar biodata dosen untuk menampilkan data
	 * asesor pegawai yang bersangkutan.
	 *
	 * @return ID pegawai terkait, atau {@code null} bila dosen tidak punya padanan pegawai
	 */
	@Column(name = "pegawai_id")
	public Long getPegawaiId() {
		return pegawaiId;
	}

	/**
	 * Mengisi ID baris {@link Pegawai} yang merepresentasikan dosen ini di modul kepegawaian.
	 *
	 * @param pegawaiId ID pegawai terkait
	 */
	public void setPegawaiId(Long pegawaiId) {
		this.pegawaiId = pegawaiId;
	}

	/**
	 * Mengisi penanda dosen aktif.
	 *
	 * @param aktif {@code true} bila dosen dianggap aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Bahasa antarmuka pilihan dosen. Bila kolom kosong, nilai baku
	 * {@code Tbmuser.INDONESIA} dikembalikan (tanpa ditulis balik ke field). Kolom ini
	 * {@code @NotAudited} sehingga perubahannya tidak membanjiri tabel audit Envers.
	 *
	 * @return kode bahasa antarmuka; tidak pernah {@code null}
	 */
	@NotAudited
	public String getBahasa() {
		return bahasa == null ? Tbmuser.INDONESIA : bahasa;
	}

	/**
	 * Mengisi bahasa antarmuka pilihan dosen.
	 *
	 * @param bahasa kode bahasa antarmuka
	 */
	public void setBahasa(String bahasa) {
		this.bahasa = bahasa;
	}

	/**
	 * Riwayat pendidikan jenjang S1 dosen dalam bentuk teks bebas (nama program studi/perguruan
	 * tinggi), pelengkap relasi {@link #getPendidikan()} yang hanya menyimpan jenjang tertinggi.
	 *
	 * @return keterangan pendidikan S1, atau {@code null} bila belum diisi
	 */
	public String getPendidikans1() {
		return pendidikans1;
	}

	/**
	 * Mengisi keterangan riwayat pendidikan jenjang S1.
	 *
	 * @param pendidikans1 keterangan pendidikan S1
	 */
	public void setPendidikans1(String pendidikans1) {
		this.pendidikans1 = pendidikans1;
	}

	/**
	 * Riwayat pendidikan jenjang S2 dosen dalam bentuk teks bebas.
	 *
	 * @return keterangan pendidikan S2, atau {@code null} bila belum diisi
	 */
	public String getPendidikans2() {
		return pendidikans2;
	}

	/**
	 * Mengisi keterangan riwayat pendidikan jenjang S2.
	 *
	 * @param pendidikans2 keterangan pendidikan S2
	 */
	public void setPendidikans2(String pendidikans2) {
		this.pendidikans2 = pendidikans2;
	}

	/**
	 * Riwayat pendidikan jenjang S3 dosen dalam bentuk teks bebas.
	 *
	 * @return keterangan pendidikan S3, atau {@code null} bila belum diisi
	 */
	public String getPendidikans3() {
		return pendidikans3;
	}

	/**
	 * Mengisi keterangan riwayat pendidikan jenjang S3.
	 *
	 * @param pendidikans3 keterangan pendidikan S3
	 */
	public void setPendidikans3(String pendidikans3) {
		this.pendidikans3 = pendidikans3;
	}

	/**
	 * Status kewajiban beban mengajar dosen (relasi lazy kolom {@code status_kewajiban_beban_dosen}).
	 * Master ini yang membedakan dosen biasa dari dosen dengan tugas tambahan (rektor, dekan, ketua
	 * program studi) yang beban wajibnya berbeda. Bila kolom kosong, {@code ConstantValues.DOSEN_BIASA}
	 * dikembalikan sebagai nilai baku (tanpa ditulis balik ke field).
	 *
	 * @return status kewajiban beban dosen; tidak pernah {@code null} selama master sudah dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_kewajiban_beban_dosen", nullable = true)
	public StatusKewajibanBebanDosen getStatusKewajibanBebanDosen() {
		statusKewajibanBebanDosen = check(statusKewajibanBebanDosen);
		return statusKewajibanBebanDosen == null ? ConstantValues.DOSEN_BIASA : statusKewajibanBebanDosen;
	}

	/**
	 * Menetapkan status kewajiban beban mengajar dosen.
	 *
	 * @param statusKewajibanBebanDosen status kewajiban beban dosen
	 */
	public void setStatusKewajibanBebanDosen(StatusKewajibanBebanDosen statusKewajibanBebanDosen) {
		this.statusKewajibanBebanDosen = statusKewajibanBebanDosen;
	}

	/**
	 * Penanda dosen sudah tersertifikasi (serdos). Nilai {@code null} dianggap {@code false} tanpa
	 * ditulis balik ke field.
	 *
	 * @return {@code true} bila dosen sudah tersertifikasi
	 * @see #getNomorSertifikasi()
	 */
	public Boolean getSertifikasi() {
		return sertifikasi == null ? false : sertifikasi;
	}

	/**
	 * Mengisi penanda sertifikasi dosen.
	 *
	 * @param sertifikasi {@code true} bila dosen sudah tersertifikasi
	 */
	public void setSertifikasi(Boolean sertifikasi) {
		this.sertifikasi = sertifikasi;
	}

	/**
	 * Penanda apakah penugasan mengajar dosen ini sesuai bidang keilmuannya — dipakai pada rekap
	 * kesesuaian bidang. Nilai {@code null} dianggap {@code true} (dianggap sesuai) tanpa ditulis
	 * balik ke field.
	 *
	 * @return {@code true} bila penugasan dianggap sesuai bidang keilmuan
	 */
	public Boolean getSesuaiBidangKeilmuan() {
		return sesuaiBidangKeilmuan == null ? true : sesuaiBidangKeilmuan;
	}

	/**
	 * Mengisi penanda kesesuaian penugasan dengan bidang keilmuan dosen.
	 *
	 * @param sesuaiBidangKeilmuan {@code true} bila sesuai bidang keilmuan
	 */
	public void setSesuaiBidangKeilmuan(Boolean sesuaiBidangKeilmuan) {
		this.sesuaiBidangKeilmuan = sesuaiBidangKeilmuan;
	}

	/**
	 * Jabatan struktural dosen di <b>perguruan tinggi lain</b> (relasi lazy kolom
	 * {@code spesifikasi_jabatan_pt_lain}), dilewatkan {@code check(...)}. Dipakai untuk dosen yang
	 * berhome-base di tempat lain namun mengajar di sini.
	 *
	 * @return jabatan di perguruan tinggi lain, atau {@code null} bila tidak ada
	 * @see #getSpesifikasiJabatan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "spesifikasi_jabatan_pt_lain", nullable = true)
	public Jabatan getSpesifikasiJabatanPtLain() {
		spesifikasiJabatanPtLain = check(spesifikasiJabatanPtLain);
		return spesifikasiJabatanPtLain;
	}

	/**
	 * Menetapkan jabatan struktural dosen di perguruan tinggi lain.
	 *
	 * @param spesifikasiJabatanPtLain jabatan di perguruan tinggi lain
	 */
	public void setSpesifikasiJabatanPtLain(Jabatan spesifikasiJabatanPtLain) {
		this.spesifikasiJabatanPtLain = spesifikasiJabatanPtLain;
	}

	/**
	 * Jenjang pendidikan tertinggi dosen menurut master {@code employ} (relasi lazy kolom
	 * {@code pendidikan}), dilewatkan {@code check(...)}. Rincian per jenjang disimpan terpisah pada
	 * {@link #getPendidikans1()}, {@link #getPendidikans2()}, dan {@link #getPendidikans3()}.
	 *
	 * @return jenjang pendidikan tertinggi, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan", nullable = true)
	public Pendidikan getPendidikan() {
		pendidikan = check(pendidikan);
		return pendidikan;
	}

	/**
	 * Menetapkan jenjang pendidikan tertinggi dosen.
	 *
	 * @param pendidikan jenjang pendidikan tertinggi
	 */
	public void setPendidikan(Pendidikan pendidikan) {
		this.pendidikan = pendidikan;
	}

	/**
	 * Ruang kantor dosen (relasi lazy kolom {@code ruang}), dilewatkan {@code check(...)}. Perlu
	 * dibedakan dari ruang kelas yang melekat pada {@link Perkuliahan}.
	 *
	 * @return ruang kantor dosen, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		return ruang;
	}

	/**
	 * Menetapkan ruang kantor dosen.
	 *
	 * @param ruang ruang kantor
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Penanda penguncian baris dosen (dipakai proses impor/sinkronisasi agar baris tertentu tidak
	 * ikut ditimpa). Kolom sejenis juga ada pada {@link Mahasiswa}.
	 *
	 * @return nilai penanda kunci, atau {@code null} bila baris tidak dikunci
	 */
	public String getLockId() {
		return lockId;
	}

	/**
	 * Mengisi penanda penguncian baris dosen.
	 *
	 * @param lockId nilai penanda kunci
	 */
	public void setLockId(String lockId) {
		this.lockId = lockId;
	}

	/**
	 * Nomor sertifikat pendidik (serdos) dosen.
	 *
	 * @return nomor sertifikasi, atau {@code null} bila belum tersertifikasi
	 * @see #getSertifikasi()
	 */
	public String getNomorSertifikasi() {
		return nomorSertifikasi;
	}

	/**
	 * Mengisi nomor sertifikat pendidik (serdos).
	 *
	 * @param nomorSertifikasi nomor sertifikasi
	 */
	public void setNomorSertifikasi(String nomorSertifikasi) {
		this.nomorSertifikasi = nomorSertifikasi;
	}

	/**
	 * ID {@code Dosen} lain yang menjadi atasan langsung dosen ini. Disimpan sebagai angka, bukan
	 * relasi Hibernate, sehingga pemanggil harus memuatnya sendiri (pola yang dipakai
	 * {@code ais.action.master.BiodataDosenAction}:
	 * {@code ConstantValues.ambil(Dosen.class.getName(), dosen.getAtasanlangsung())}).
	 *
	 * @return ID dosen atasan langsung, atau {@code null} bila tidak ditetapkan
	 * @see #yangLoginMerupakanAtasan()
	 */
	public Long getAtasanlangsung() {
		return atasanlangsung;
	}

	/**
	 * Menetapkan ID dosen yang menjadi atasan langsung dosen ini.
	 *
	 * @param atasanlangsung ID dosen atasan langsung
	 */
	public void setAtasanlangsung(Long atasanlangsung) {
		this.atasanlangsung = atasanlangsung;
	}

	/**
	 * Memeriksa apakah pengguna yang sedang login adalah atasan langsung dosen ini — dipakai sebagai
	 * <b>gerbang otorisasi</b> pada layar persetujuan/verifikasi.
	 *
	 * <p>Bernilai {@code true} hanya bila keempat syarat terpenuhi: {@link #getAtasanlangsung()}
	 * terisi, ada pengguna aktif ({@code Common.getCurrentUser()}), pengguna itu terhubung ke sebuah
	 * dosen dan hak aksesnya berperan {@code "dosen"}, serta ID dosen pengguna sama dengan
	 * {@code getAtasanlangsung()}. Semua kegagalan pembacaan (mis. tidak ada session ZK aktif) direkam
	 * ke {@code ErrorAuditUtil} dan menghasilkan {@code false} — gerbang ini gagal-tertutup.</p>
	 *
	 * <p>Dipanggil dari helper persetujuan artikel, kegiatan kedosenan, serta pengajuan penelitian
	 * dan pengabdian (mis. {@code ais.action.master.helper.DetailArtikelHelper},
	 * {@code ais.action.master.helper.KegiatanKedosenanPunyaDosenHelper}).</p>
	 *
	 * @return {@code true} bila pengguna yang login adalah atasan langsung dosen ini
	 */
	public boolean yangLoginMerupakanAtasan() {
		try {
			if (getAtasanlangsung() != null) {
				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.ambilDosen() != null
						&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
						&& tbmuser.getDosen().getId().equals(getAtasanlangsung())) {
					return true;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1022");
			// TODO: handle exception
		}
		return false;
	}

	/**
	 * Membaca isi mentah berkas indeks JSON perkuliahan milik dosen ini
	 * ({@code dosen_punya_perkuliahan_&lt;id&gt;}).
	 *
	 * @return isi berkas indeks, atau {@code VOMahasiswa.dataJSON} (objek JSON kosong) bila berkas
	 *         tidak ada, kosong, atau gagal dibaca
	 * @see #tulisLokasiPerkuliahan(String)
	 */
	public String ambilLokasiPerkuliahan() {
		File file = Common.getFileLocation(this, "dosen_punya_perkuliahan_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1034");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas indeks JSON perkuliahan milik dosen ini dengan {@code data}. Kegagalan tulis
	 * direkam ke {@code ErrorAuditUtil} dan tidak dilempar.
	 *
	 * @param data isi berkas indeks yang baru (biasanya {@code JSONObject.toString()})
	 */
	public void tulisLokasiPerkuliahan(String data) {
		File file = Common.getFileLocation(this, "dosen_punya_perkuliahan_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1043");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Menghapus berkas indeks JSON perkuliahan milik dosen ini. Dipakai sebelum indeks dibangun ulang
	 * oleh {@link #reInitPerkuliahan(Session)}.
	 */
	public void bersihkanLokasiPerkuliahan() {
		File file = Common.getFileLocation(this, "dosen_punya_perkuliahan_" + getId().toString());
		BacaTulisUtil.doHapus(file, "dosen_punya_perkuliahan");

	}

	/**
	 * Membangun ulang indeks perkuliahan dosen ini dari basis data.
	 *
	 * <p>Mengambil seluruh {@link Perkuliahan} yang aktif (kolom {@code aktif} null atau {@code true})
	 * dan menempatkan dosen ini pada salah satu dari sepuluh slot pengampu
	 * ({@code dosen1} s.d. {@code dosen10}), mengurutkannya menurun, lalu <b>menghapus</b> berkas
	 * indeks lama dan menuliskannya kembali satu per satu lewat
	 * {@link #populatePerkuliahan(Perkuliahan, boolean)}.</p>
	 *
	 * <p><b>Efek samping:</b> berkas indeks di disk ditulis ulang total; operasi ini berat dan
	 * sebaiknya hanya dipicu oleh {@link #ambilPerkuliahan(Session)} saat penanda
	 * {@code udah("perkuliahan")} belum ada, atau saat administrator memaksa penyegaran.</p>
	 *
	 * @param session session Hibernate yang dipakai untuk query perkuliahan
	 */
	@SuppressWarnings("unchecked")
	public void reInitPerkuliahan(Session session) {

		Criterion criterion = Restrictions.or(Restrictions.eq("dosen1", this), Restrictions.eq("dosen2", this));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", this));

		List<Perkuliahan> perkuliahans = ConstantValues.simpleList(session.createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(criterion),
				Perkuliahan.class);
		try {
			Collections.sort(perkuliahans, Collections.reverseOrder());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1074");
		}

		bersihkanLokasiPerkuliahan();
		tulisLokasiPerkuliahan(new JSONObject().toString());
		for (Perkuliahan perkuliahan : perkuliahans) {
			populatePerkuliahan(perkuliahan, true);
		}
		perkuliahans = null;
	}

	/**
	 * Membangun ulang indeks e-learning untuk seluruh {@link Skripsi} yang melibatkan dosen ini
	 * sebagai pembimbing, ketua sidang, penguji 1/2/3, atau pembimbing 3.
	 *
	 * <p>Berbeda dari {@link #reInitPerkuliahan(Session)} yang menulis indeks milik dosen sendiri,
	 * method ini menyerahkan hasil query ke
	 * {@code AuditListener.prosesUntukElearning(...)} sehingga indeks yang diperbarui adalah indeks
	 * milik setiap {@code VOMahasiswaDosen} yang terlibat (mahasiswa maupun dosen), lalu setiap objek
	 * tersebut menuliskan indeksnya lewat {@code tulisPutBaru(Skripsi.class.getName())}.</p>
	 *
	 * @param session session Hibernate yang dipakai untuk query skripsi
	 */
	@SuppressWarnings("unchecked")
	public void reInitSkripsi(Session session) {

		Criterion criterion = Restrictions.or(Restrictions.eq("pembimbing", this),
				Restrictions.eq("ketuaSidang", this));

		criterion = Restrictions.or(criterion, Restrictions.eq("penguji1", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji2", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji3", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("pembimbing3", this));

		List<Skripsi> skripsis = ConstantValues.simpleList(
				session.createCriteria(Skripsi.class).add(criterion).addOrder(Order.asc("id")), Skripsi.class);

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (Skripsi skripsi : skripsis) {
			AuditListener.prosesUntukElearning(skripsi, "", skripsi.getId(), voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(Skripsi.class.getName());
		}
		voMahasiswaDosens = null;

		skripsis = null;
	}

	/**
	 * Membangun ulang indeks e-learning untuk seluruh {@link MahasiswaRequestTugasAkhir} (pengajuan
	 * bimbingan tugas akhir) yang melibatkan dosen ini pada salah satu dari enam slot pembimbing
	 * ({@code dosen1} s.d. {@code dosen6}). Pola pemrosesannya sama dengan
	 * {@link #reInitSkripsi(Session)}.
	 *
	 * @param session session Hibernate yang dipakai untuk query pengajuan bimbingan
	 */
	@SuppressWarnings("unchecked")
	public void reInitBimbingan(Session session) {

		Criterion criterion = Restrictions.or(Restrictions.eq("dosen1", this), Restrictions.eq("dosen2", this));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", this));

		List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = ConstantValues.simpleList(
				session.createCriteria(MahasiswaRequestTugasAkhir.class).add(criterion).addOrder(Order.asc("id")),
				MahasiswaRequestTugasAkhir.class);

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : mahasiswaRequestTugasAkhirs) {
			AuditListener.prosesUntukElearning(mahasiswaRequestTugasAkhir, "", mahasiswaRequestTugasAkhir.getId(),
					voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(MahasiswaRequestTugasAkhir.class.getName());
		}
		voMahasiswaDosens = null;
		mahasiswaRequestTugasAkhirs = null;
	}

	/**
	 * Membangun ulang indeks e-learning untuk seluruh {@link MahasiswaDapatKelompokKkn} yang kelompok
	 * KKN-nya dibimbing dosen ini (slot {@code dosen_pembimbing1} s.d. {@code dosen_pembimbing5} pada
	 * {@code kelompokKkn}). Pola pemrosesannya sama dengan {@link #reInitSkripsi(Session)}.
	 *
	 * @param session session Hibernate yang dipakai untuk query peserta KKN
	 */
	@SuppressWarnings("unchecked")
	public void reInitKkn(Session session) {

		Criterion criterion = Restrictions.or(Restrictions.eq("kelompokKkn.dosen_pembimbing1", this),
				Restrictions.eq("kelompokKkn.dosen_pembimbing2", this));

		criterion = Restrictions.or(criterion, Restrictions.eq("kelompokKkn.dosen_pembimbing3", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("kelompokKkn.dosen_pembimbing4", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("kelompokKkn.dosen_pembimbing5", this));

		List<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKkns = session
				.createCriteria(MahasiswaDapatKelompokKkn.class).createAlias("kelompokKkn", "kelompokKkn")
				.add(criterion).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : mahasiswaDapatKelompokKkns) {
			AuditListener.prosesUntukElearning(mahasiswaDapatKelompokKkn, "", mahasiswaDapatKelompokKkn.getId(),
					voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(MahasiswaDapatKelompokKkn.class.getName());
		}
		voMahasiswaDosens = null;
		mahasiswaDapatKelompokKkns = null;
	}

	/**
	 * Membangun ulang indeks e-learning untuk seluruh {@link MahasiswaDapatKelompokPkl} yang kelompok
	 * PKL-nya dibimbing dosen ini (slot {@code dosen_pembimbing1} s.d. {@code dosen_pembimbing5} pada
	 * {@code kelompokPkl}). Pola pemrosesannya sama dengan {@link #reInitSkripsi(Session)}.
	 *
	 * @param session session Hibernate yang dipakai untuk query peserta PKL
	 */
	@SuppressWarnings("unchecked")
	public void reInitPkl(Session session) {

		Criterion criterion = Restrictions.or(Restrictions.eq("kelompokPkl.dosen_pembimbing1", this),
				Restrictions.eq("kelompokPkl.dosen_pembimbing2", this));

		criterion = Restrictions.or(criterion, Restrictions.eq("kelompokPkl.dosen_pembimbing3", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("kelompokPkl.dosen_pembimbing4", this));
		criterion = Restrictions.or(criterion, Restrictions.eq("kelompokPkl.dosen_pembimbing5", this));

		List<MahasiswaDapatKelompokPkl> mahasiswaDapatKelompokPkls = session
				.createCriteria(MahasiswaDapatKelompokPkl.class).createAlias("kelompokPkl", "kelompokPkl")
				.add(criterion).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : mahasiswaDapatKelompokPkls) {
			AuditListener.prosesUntukElearning(mahasiswaDapatKelompokPkl, "", mahasiswaDapatKelompokPkl.getId(),
					voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(MahasiswaDapatKelompokPkl.class.getName());
		}
		voMahasiswaDosens = null;
		mahasiswaDapatKelompokPkls = null;
	}

	/**
	 * Membangun ulang indeks e-learning untuk seluruh {@link KrsMahasiswa} yang dosen pembimbing
	 * akademiknya ({@code dosenPa}) adalah dosen ini — yaitu daftar mahasiswa perwalian.
	 *
	 * <p>Query dibatasi pada semester 1 s.d. 14 dan hanya mahasiswa yang belum keluar
	 * ({@code mahasiswa.statusKeluar} null), diurutkan menurut NIM. Pola pemrosesannya sama dengan
	 * {@link #reInitSkripsi(Session)}.</p>
	 *
	 * @param session session Hibernate yang dipakai untuk query KRS
	 */
	@SuppressWarnings("unchecked")
	public void reInitKrs(Session session) {
		List<KrsMahasiswa> krsMahasiswas = session.createCriteria(KrsMahasiswa.class)
				.add(Restrictions.le("semester", 14)).createAlias("mahasiswa", "mahasiswa")
				.add(Restrictions.isNull("mahasiswa.statusKeluar")).addOrder(Order.asc("mahasiswa.nim"))
				.add(Restrictions.eq("dosenPa", this)).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (KrsMahasiswa krsMahasiswa : krsMahasiswas) {
			AuditListener.prosesUntukElearning(krsMahasiswa, "", krsMahasiswa.getId(), voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(KrsMahasiswa.class.getName());
		}
		voMahasiswaDosens = null;
		krsMahasiswas = null;
	}

	/**
	 * Membangun ulang indeks e-learning untuk seluruh {@link FormulirKegiatanPeserta} yang diikuti
	 * dosen ini. Pola pemrosesannya sama dengan {@link #reInitSkripsi(Session)}.
	 *
	 * @param session session Hibernate yang dipakai untuk query peserta kegiatan
	 */
	@SuppressWarnings("unchecked")
	public void reInitFormulirKegiatanPeserta(Session session) {
		List<FormulirKegiatanPeserta> formulirKegiatanPesertas = session.createCriteria(FormulirKegiatanPeserta.class)
				.add(Restrictions.eq("dosen", this)).addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (FormulirKegiatanPeserta formulirKegiatanPeserta : formulirKegiatanPesertas) {
			AuditListener.prosesUntukElearning(formulirKegiatanPeserta, "", formulirKegiatanPeserta.getId(),
					voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(FormulirKegiatanPeserta.class.getName());
		}
		voMahasiswaDosens = null;
		formulirKegiatanPesertas = null;
	}

	/**
	 * Membangun ulang indeks e-learning untuk seluruh {@link PertemuanPunyaGrupPertemuan} yang grup
	 * pertemuannya (konsultasi/bimbingan lain) diampu dosen ini. Pola pemrosesannya sama dengan
	 * {@link #reInitSkripsi(Session)}.
	 *
	 * @param session session Hibernate yang dipakai untuk query grup pertemuan
	 */
	@SuppressWarnings("unchecked")
	public void reInitKonsultasi(Session session) {
		List<PertemuanPunyaGrupPertemuan> pertemuanPunyaGrupPertemuans = session
				.createCriteria(PertemuanPunyaGrupPertemuan.class).createAlias("grupPertemuan", "grupPertemuan")
				.add(Restrictions.eq("grupPertemuan.dosen", this)).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan : pertemuanPunyaGrupPertemuans) {
			AuditListener.prosesUntukElearning(pertemuanPunyaGrupPertemuan, "", pertemuanPunyaGrupPertemuan.getId(),
					voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(PertemuanPunyaGrupPertemuan.class.getName());
		}
		voMahasiswaDosens = null;
		pertemuanPunyaGrupPertemuans = null;
	}

	/**
	 * Mengeluarkan satu perkuliahan dari indeks dosen ini dengan cara menyetel nilainya menjadi string
	 * kosong (baris indeks tetap ada, tetapi diabaikan pembaca).
	 *
	 * <p>Method ini <b>memulihkan diri</b> bila berkas indeks JSON rusak/terpotong: kegagalan
	 * {@code new JSONObject(...)} tidak dilempar, melainkan dicatat ke {@code ErrorAuditUtil} dan
	 * indeks dimulai ulang dari objek kosong — sebelumnya kondisi ini menyebabkan
	 * {@code NullPointerException} pada {@code c.put(...)}.</p>
	 *
	 * @param id ID perkuliahan yang akan dikeluarkan dari indeks; {@code null} diabaikan
	 */
	public void removePerkuliahan(Serializable id) {
		if (id == null) {
			return;
		}
		try {
			JSONObject c;
			try {
				c = new JSONObject(ambilLokasiPerkuliahan());
			} catch (JSONException je) {
				// Data JSON rusak/terpotong (mis. tersimpan tak lengkap sebelumnya) -> self-heal:
				// mulai dari objek kosong, bukan biarkan c null lalu NPE di c.put(...) di bawah.
				ais.common.ErrorAuditUtil.record(je,
						"auto-heal(json-corrupt) src/ais/database/model/Dosen.java removePerkuliahan - reset ke JSON kosong");
				c = null;
			}
			if (c == null) {
				c = new JSONObject();
			}
			c.put(id.toString(), "");
			tulisLokasiPerkuliahan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1247");

		}
	}

	/**
	 * Menambahkan (atau menyegarkan) satu perkuliahan pada indeks dosen ini.
	 *
	 * @param perkuliahan perkuliahan yang akan dicatat; {@code null} diabaikan
	 * @param tulisUlang parameter warisan yang saat ini <b>tidak dipakai</b> — berkas indeks selalu
	 *                   ditulis ulang. Dipertahankan agar tanda tangan method tetap sama dengan
	 *                   pemanggil lama
	 * @see #removePerkuliahan(Serializable)
	 */
	public void populatePerkuliahan(Perkuliahan perkuliahan, boolean tulisUlang) {
		try {
			if (perkuliahan == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiPerkuliahan());
			c.put(perkuliahan.getId().toString(), perkuliahan.getId().toString());
			tulisLokasiPerkuliahan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1261");
		}
	}

	/**
	 * Varian nyaman {@link #ambilPerkuliahan(Session)} yang membuka session Hibernate native sendiri
	 * dan menutupnya kembali (disconnect + close, masing-masing dijaga try/catch) pada blok
	 * {@code finally}. Pakai varian bersession bila pemanggil sudah memegang session aktif.
	 *
	 * @return daftar ID perkuliahan yang diampu dosen ini
	 */
	public List<Long> ambilPerkuliahan() {
		Session session = HibernateUtil.currentNativeSession();
		try {
			return ambilPerkuliahan(session);
		} finally {
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1272");
				// TODO: handle exception
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1277");
				// TODO: handle exception
			}
		}
	}

	/**
	 * Mengembalikan daftar ID perkuliahan yang diampu dosen ini, termasuk kelas paralelnya.
	 *
	 * <p>Alur: bila penanda sekali-jalan {@code udah("perkuliahan")} belum ada, indeks dibangun lebih
	 * dulu lewat {@link #reInitPerkuliahan(Session)}. Setiap kunci pada indeks dimuat lewat
	 * {@code ConstantValues.ambil(Perkuliahan.class.getName(), id)}, lalu disaring: hanya perkuliahan
	 * yang punya mata kuliah dan yang benar-benar masih memuat dosen ini
	 * ({@code perkuliahan.ada(this)}) yang diambil. Untuk setiap perkuliahan yang lolos, ID jadwal
	 * paralelnya ({@code perkuliahan.ambilParalel(this)}) ikut ditambahkan tanpa duplikat.</p>
	 *
	 * <p><b>Efek samping:</b> pemanggilan pertama untuk seorang dosen dapat memicu query berat dan
	 * penulisan ulang berkas indeks. Semua kegagalan per item hanya dicatat
	 * ({@code e.printStackTrace()} + {@code ErrorAuditUtil}) supaya satu baris indeks yang rusak tidak
	 * menggagalkan seluruh daftar.</p>
	 *
	 * @param session session Hibernate yang dipakai bila indeks perlu dibangun ulang
	 * @return daftar ID perkuliahan (termasuk paralel); kosong bila dosen tidak mengampu apa pun
	 */
	@SuppressWarnings("unchecked")
	public List<Long> ambilPerkuliahan(Session session) {
		if (!udah("perkuliahan")) {
			reInitPerkuliahan(session);
		}

		List<Long> perkuliahansa = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiPerkuliahan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
								Long.parseLong(key));
						if (perkuliahan != null && perkuliahan.getMatakuliah() != null && perkuliahan.ada(this)) {
							perkuliahansa.add(perkuliahan.getId());

							List<Long> jadwalParalels = perkuliahan.ambilParalel(this);
							for (Long jadwal : jadwalParalels) {
								if (!perkuliahansa.contains(jadwal)) {
									perkuliahansa.add(jadwal);
								}
							}
							jadwalParalels = null;
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:1313");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:1317");
		}
		return perkuliahansa;
	}

	/**
	 * Varian ringkas {@link #ambilPerkuliahanDanParalel(Session, String, String, String, String,
	 * String, boolean, Integer, boolean, boolean, boolean, boolean, boolean, boolean, boolean,
	 * boolean, boolean, boolean, boolean, boolean, Integer, int, int, boolean, JenisFormulirKegiatan)}
	 * dengan {@code keywordJadiPembatas = false} dan tanpa penyaringan jenis formulir kegiatan.
	 * Seluruh parameter diteruskan apa adanya; lihat method utama untuk maknanya.
	 *
	 * @return larik tiga elemen: halaman data, jumlah total, dan seluruh data sebelum dipotong
	 */
	public Object[] ambilPerkuliahanDanParalel(Session session, String tahunAkademik, String jenisSemester, String hari,
			String keyword, String kelas, boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean paralel,
			boolean remedial, boolean paralelAja,

			boolean requestStatus, boolean aktifStatus, boolean seminarStatus, boolean mengulangStatus,
			boolean lulusStatus, boolean gagalStatus,

			boolean belumStatus, boolean setujuStatus, boolean sidangStatus,

			Integer ditampilkanHanya, int mulai, int banyak) {
		return ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hari, keyword, kelas,
				merupakanPraPerkuliahan, ekstrakurikuler, paralel, remedial, paralelAja, requestStatus, aktifStatus,
				seminarStatus, mengulangStatus, lulusStatus, gagalStatus, belumStatus, setujuStatus, sidangStatus,
				ditampilkanHanya, mulai, banyak, false, null);
	}

	/**
	 * Varian {@link #ambilPerkuliahanDanParalel(Session, String, String, String, String, String,
	 * boolean, Integer, boolean, boolean, boolean, boolean, boolean, boolean, boolean, boolean,
	 * boolean, boolean, boolean, boolean, Integer, int, int, boolean, JenisFormulirKegiatan)} dengan
	 * {@code keywordJadiPembatas = false} namun tetap menyaring menurut
	 * {@code jenisFormulirKegiatan}. Lihat method utama untuk makna parameter.
	 *
	 * @param jenisFormulirKegiatan jenis formulir kegiatan yang disaring; {@code null} berarti hanya
	 *                              kegiatan tanpa jenis yang diambil
	 * @return larik tiga elemen: halaman data, jumlah total, dan seluruh data sebelum dipotong
	 */
	public Object[] ambilPerkuliahanDanParalel(Session session, String tahunAkademik, String jenisSemester, String hari,
			String keyword, String kelas, boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean paralel,
			boolean remedial, boolean paralelAja,

			boolean requestStatus, boolean aktifStatus, boolean seminarStatus, boolean mengulangStatus,
			boolean lulusStatus, boolean gagalStatus,

			boolean belumStatus, boolean setujuStatus, boolean sidangStatus,

			Integer ditampilkanHanya, int mulai, int banyak, JenisFormulirKegiatan jenisFormulirKegiatan) {
		return ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hari, keyword, kelas,
				merupakanPraPerkuliahan, ekstrakurikuler, paralel, remedial, paralelAja, requestStatus, aktifStatus,
				seminarStatus, mengulangStatus, lulusStatus, gagalStatus, belumStatus, setujuStatus, sidangStatus,
				ditampilkanHanya, mulai, banyak, false, jenisFormulirKegiatan);
	}

	/**
	 * Varian {@link #ambilPerkuliahanDanParalel(Session, String, String, String, String, String,
	 * boolean, Integer, boolean, boolean, boolean, boolean, boolean, boolean, boolean, boolean,
	 * boolean, boolean, boolean, boolean, Integer, int, int, boolean, JenisFormulirKegiatan)} yang
	 * meneruskan {@code keywordJadiPembatas} apa adanya namun tanpa penyaringan jenis formulir
	 * kegiatan. Lihat method utama untuk makna parameter.
	 *
	 * @param keywordJadiPembatas bila {@code true} dan kata kunci kosong, penelusuran indeks berhenti
	 *                            setelah cukup baris terkumpul (pembatas kinerja)
	 * @return larik tiga elemen: halaman data, jumlah total, dan seluruh data sebelum dipotong
	 */
	public Object[] ambilPerkuliahanDanParalel(Session session, String tahunAkademik, String jenisSemester, String hari,
			String keyword, String kelas, boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean paralel,
			boolean remedial, boolean paralelAja,

			boolean requestStatus, boolean aktifStatus, boolean seminarStatus, boolean mengulangStatus,
			boolean lulusStatus, boolean gagalStatus,

			boolean belumStatus, boolean setujuStatus, boolean sidangStatus,

			Integer ditampilkanHanya, int mulai, int banyak, boolean keywordJadiPembatas) {
		JenisFormulirKegiatan jenisFormulirKegiatan = null;
		return ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hari, keyword, kelas,
				merupakanPraPerkuliahan, ekstrakurikuler, paralel, remedial, paralelAja,

				requestStatus, aktifStatus, seminarStatus, mengulangStatus, lulusStatus, gagalStatus,

				belumStatus, setujuStatus, sidangStatus,

				ditampilkanHanya, mulai, banyak, keywordJadiPembatas, jenisFormulirKegiatan);
	}

	/**
	 * Mesin pengambilan data "aktivitas pembelajaran dosen" untuk layar e-learning dan profil dosen —
	 * satu-satunya implementasi nyata dari empat overload {@code ambilPerkuliahanDanParalel}.
	 *
	 * <p>Bergantung pada {@code ditampilkanHanya}, method ini membaca <b>satu</b> jenis aktivitas dari
	 * indeks berkas milik dosen (lihat pola indeks pada Javadoc kelas) lalu mengubahnya menjadi daftar
	 * {@link VOPembelajaran}:</p>
	 * <ul>
	 * <li>{@code TampilanELearningAction.SKRIPSI} → {@link Skripsi}, disaring menurut
	 * {@code belumStatus}/{@code setujuStatus}/{@code sidangStatus};</li>
	 * <li>{@code BIMBINGAN} → {@link MahasiswaRequestTugasAkhir}, disaring menurut kombinasi
	 * {@code requestStatus}, {@code aktifStatus}, {@code seminarStatus}, {@code mengulangStatus},
	 * {@code lulusStatus}, {@code gagalStatus};</li>
	 * <li>{@code KKN} → {@link ais.database.model.kkn.KelompokKkn} unik dari peserta KKN;</li>
	 * <li>{@code PKL} → {@link ais.database.model.pkl.KelompokPkl} unik dari peserta PKL;</li>
	 * <li>{@code KRS} → {@link KrsMahasiswa} milik mahasiswa perwalian. Setiap KRS <b>dimuat ulang
	 * lewat {@code session}</b> (bukan dipakai dari cache) karena objek cache yang detached memicu
	 * {@code LazyInitializationException} saat relasi mahasiswa dibaca. Status persetujuan diambil
	 * lewat satu query SQL native ke {@code detailperkuliahan} sehingga tidak perlu N+1 pembacaan;</li>
	 * <li>{@code KEGIATAN} → {@link FormulirKegiatan} unik, dicocokkan dengan
	 * {@code jenisFormulirKegiatan};</li>
	 * <li>{@code KONSULTASI} → {@link PertemuanPunyaGrupPertemuan};</li>
	 * <li>{@code PERKULIAHAN} → {@link Perkuliahan} hasil {@link #ambilPerkuliahan(Session)}.</li>
	 * </ul>
	 *
	 * <p>Setelah terkumpul, data diurutkan terbalik lalu disaring lagi secara seragam menurut tahun
	 * akademik, hari, kata kunci ({@code VOPembelajaran.ambilKeyword()}), kelas, ekstrakurikuler,
	 * status pra-perkuliahan, dan jenis semester (termasuk perlakuan khusus semester pendek
	 * {@code Perkuliahan.SP}). Khusus item bertipe {@link Perkuliahan} berlaku saringan tambahan
	 * {@code paralel}/{@code paralelAja}/{@code remedial}. Pemotongan halaman dilakukan manual dengan
	 * {@code mulai} dan {@code banyak}.</p>
	 *
	 * <p><b>Ketahanan:</b> {@code ditampilkanHanya} yang {@code null} langsung mengembalikan hasil
	 * kosong (dulu memicu NPE karena semua cabang memanggil {@code equals}), dan {@code keyword} yang
	 * {@code null} dijadikan string kosong. Kegagalan per item hanya dicatat ke {@code ErrorAuditUtil}.</p>
	 *
	 * <p>Dipanggil antara lain dari {@code ais.action.master.TampilanELearningAction},
	 * {@code ais.action.master.helper.profile.ProfileDosen}, {@code PertemuanAction}, dan
	 * {@code AbsensiAction}.</p>
	 *
	 * @param session                 session Hibernate aktif; wajib, dipakai memuat ulang KRS dan
	 *                                perkuliahan
	 * @param tahunAkademik           tahun akademik yang disaring; {@code null} berarti semua
	 * @param jenisSemester           jenis semester (ganjil/genap/{@code Perkuliahan.SP});
	 *                                {@code null} berarti semua
	 * @param hari                    nama hari yang disaring; kosong berarti semua
	 * @param keyword                 kata kunci pencarian bebas; {@code null}/kosong berarti semua
	 * @param kelas                   penggalan nama kelas; kosong berarti semua
	 * @param merupakanPraPerkuliahan bila {@code true} hanya aktivitas pra-perkuliahan yang diambil
	 * @param ekstrakurikuler         penanda ekstrakurikuler; {@code null} berarti semua
	 * @param paralel                 bila {@code false}, kelas paralel disembunyikan
	 * @param remedial                bila {@code true}, hanya perkuliahan remedial yang diambil
	 * @param paralelAja              bila {@code true}, hanya kelas paralel yang diambil
	 * @param requestStatus           sertakan bimbingan berstatus pengajuan
	 * @param aktifStatus             sertakan bimbingan berstatus aktif
	 * @param seminarStatus           sertakan bimbingan berstatus seminar
	 * @param mengulangStatus         sertakan bimbingan berstatus mengulang
	 * @param lulusStatus             sertakan bimbingan berstatus lulus
	 * @param gagalStatus             sertakan bimbingan berstatus gagal
	 * @param belumStatus             sertakan skripsi/KRS yang belum disetujui
	 * @param setujuStatus            sertakan skripsi/KRS yang sudah disetujui
	 * @param sidangStatus            sertakan skripsi yang sudah sidang
	 * @param ditampilkanHanya        jenis aktivitas yang diambil (konstanta
	 *                                {@code TampilanELearningAction}); {@code null} menghasilkan
	 *                                hasil kosong
	 * @param mulai                   indeks awal halaman (berbasis nol)
	 * @param banyak                  jumlah baris per halaman
	 * @param keywordJadiPembatas     bila {@code true} dan kata kunci kosong, penelusuran indeks
	 *                                dihentikan lebih awal demi kinerja
	 * @param jenisFormulirKegiatan   jenis formulir kegiatan yang disaring; {@code null} berarti hanya
	 *                                kegiatan tanpa jenis
	 * @return larik tiga elemen: {@code [0]} {@code List&lt;VOPembelajaran&gt;} halaman berjalan,
	 *         {@code [1]} {@code Integer} jumlah baris yang lolos saringan, {@code [2]}
	 *         {@code List&lt;VOPembelajaran&gt;} seluruh data sebelum dipotong halaman
	 */
	@SuppressWarnings("unchecked")
	public Object[] ambilPerkuliahanDanParalel(Session session, String tahunAkademik, String jenisSemester, String hari,
			String keyword, String kelas, boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean paralel,
			boolean remedial, boolean paralelAja,

			boolean requestStatus, boolean aktifStatus, boolean seminarStatus, boolean mengulangStatus,
			boolean lulusStatus, boolean gagalStatus,

			boolean belumStatus, boolean setujuStatus, boolean sidangStatus,

			Integer ditampilkanHanya, int mulai, int banyak, boolean keywordJadiPembatas,
			JenisFormulirKegiatan jenisFormulirKegiatan) {

		/* Parameter ini berasal dari pilihan tab UI dan pada request lama dapat belum
		 * terisi. Semua cabang di bawah memanggil equals()/trim() secara langsung. */
		if (ditampilkanHanya == null) {
			return new Object[] { new ArrayList<VOPembelajaran>(), Integer.valueOf(0),
					new ArrayList<VOPembelajaran>() };
		}
		if (keyword == null) {
			keyword = "";
		}
		int max = banyak + (mulai * banyak);
		max = max + 1;
		List<VOPembelajaran> dataDiambil = new ArrayList<VOPembelajaran>();

		if (ditampilkanHanya.equals(TampilanELearningAction.SKRIPSI)) {
			List<String> ss = retreiveAll(Skripsi.class.getName());
			List<Long> ids = new ArrayList<Long>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && ids.size() > max) {
						break;
					}

					ids.add(Long.parseLong(s));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:1410");
				}
			}

			List<Skripsi> skripsis = ConstantValues.ambilBanyak(Skripsi.class.getName(), ids);
			for (Skripsi skripsi : skripsis) {
				if (skripsi != null) {
					if ((belumStatus && !skripsi.getSetujuiSidang()) || (setujuStatus && skripsi.getSetujuiSidang())
							|| (sidangStatus && skripsi.getTelahSidang().equals(1))) {
//						System.out.println("Skripsi -> " + skripsi);
						dataDiambil.add(skripsi);
					}
				}
			}
			ids = null;
			skripsis = null;
		}

		if (ditampilkanHanya.equals(TampilanELearningAction.BIMBINGAN)) {
			List<String> ss = retreiveAll(MahasiswaRequestTugasAkhir.class.getName());

			List<String> statuses = new ArrayList<String>();
			if (requestStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.REQUEST_STATUS);
			}
			if (aktifStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.AKTIF_STATUS);
			}
			if (seminarStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.SEMINAR_STATUS);
			}
			if (mengulangStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.MENGULANG_STATUS);
			}
			if (lulusStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.LULUS_STATUS);
			}
			if (gagalStatus) {
				statuses.add(MahasiswaRequestTugasAkhir.GAGAL_STATUS);
			}

			List<Long> ids = new ArrayList<Long>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && ids.size() > max) {
						break;
					}

					ids.add(Long.parseLong(s));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:1467");
				}
			}

			List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = ConstantValues
					.ambilBanyak(MahasiswaRequestTugasAkhir.class.getName(), ids);
			for (MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : mahasiswaRequestTugasAkhirs) {
				if (mahasiswaRequestTugasAkhir != null) {
					if (statuses.contains(mahasiswaRequestTugasAkhir.getStatus())) {
//						System.out.println("mahasiswaRequestTugasAkhir -> " + mahasiswaRequestTugasAkhir);
						dataDiambil.add(mahasiswaRequestTugasAkhir);
					}
				}
			}
			ids = null;
			mahasiswaRequestTugasAkhirs = null;
		}

		if (ditampilkanHanya.equals(TampilanELearningAction.KKN)) {
			List<String> ss = retreiveAll(MahasiswaDapatKelompokKkn.class.getName());

			List<String> kknIdsData = new ArrayList<String>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && kknIdsData.size() > max) {
						break;
					}

					Long id = Long.parseLong(s);
					kknIdsData.add(id.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:1505");
				}
			}

			List<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKkns = GeneralValueObject
					.ambilDataBanyak(MahasiswaDapatKelompokKkn.class, kknIdsData);
			List<Long> kknIds = new ArrayList<Long>();
			for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : mahasiswaDapatKelompokKkns) {
				if (mahasiswaDapatKelompokKkn != null) {
					KelompokKkn kelompokKkn = mahasiswaDapatKelompokKkn.getKelompokKkn();
					if (!kknIds.contains(kelompokKkn.getId())) {
//						System.out.println("kelompokKkn -> " + kelompokKkn);
						kknIds.add(kelompokKkn.getId());
						dataDiambil.add(kelompokKkn);
					}
				}
			}
			mahasiswaDapatKelompokKkns = null;
			kknIds = null;
			kknIdsData = null;
		}

		if (ditampilkanHanya.equals(TampilanELearningAction.PKL)) {
			List<String> ss = retreiveAll(MahasiswaDapatKelompokPkl.class.getName());

			List<String> pklIdsData = new ArrayList<String>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && pklIdsData.size() > max) {
						break;
					}

					Long id = Long.parseLong(s);
					pklIdsData.add(id.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:1547");
				}
			}

			List<MahasiswaDapatKelompokPkl> mahasiswaDapatKelompokPkls = GeneralValueObject
					.ambilDataBanyak(MahasiswaDapatKelompokPkl.class, pklIdsData);
			List<Long> pklIds = new ArrayList<Long>();
			for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : mahasiswaDapatKelompokPkls) {
				if (mahasiswaDapatKelompokPkl != null) {
					KelompokPkl kelompokPkl = mahasiswaDapatKelompokPkl.getKelompokPkl();
					if (kelompokPkl != null && kelompokPkl.getId() != null && !pklIds.contains(kelompokPkl.getId())) {
//						System.out.println("kelompokPkl -> " + kelompokPkl);
						pklIds.add(kelompokPkl.getId());
						dataDiambil.add(kelompokPkl);
					}
				}
			}

			mahasiswaDapatKelompokPkls = null;
			pklIds = null;
			pklIdsData = null;
		}
		if (ditampilkanHanya.equals(TampilanELearningAction.KRS)) {
			List<String> ss = retreiveAll(KrsMahasiswa.class.getName());

			List<String> krsMahasiswaIdsData = new ArrayList<String>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && krsMahasiswaIdsData.size() > max) {
						break;
					}

					Long id = Long.parseLong(s);
					krsMahasiswaIdsData.add(id.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:1589");
				}
			}

			/*
			 * KRS pada penyimpanan indeks dosen dapat berupa object cache yang detached.
			 * Membaca relasi mahasiswa dari object tersebut membuat LazyInitializationException
			 * dan sebelumnya seluruh item diam-diam dilewati. Muat ulang setiap KRS melalui
			 * session pemanggil agar relasi lazy tetap dapat dibaca selama render profil.
			 */
			List<KrsMahasiswa> krsMahasiswas = new ArrayList<KrsMahasiswa>();
			for (String krsMahasiswaId : krsMahasiswaIdsData) {
				try {
					KrsMahasiswa krsMahasiswa = (KrsMahasiswa) session.get(KrsMahasiswa.class,
							Long.valueOf(krsMahasiswaId));
					if (krsMahasiswa != null) {
						krsMahasiswas.add(krsMahasiswa);
					}
				} catch (Exception eKrs) {
					ais.common.ErrorAuditUtil.record(eKrs,
							"muat KRS pembimbing akademik src/ais/database/model/Dosen.java");
				}
			}

			Set<Long> krsBelumDisetujui = null;
			if (!(belumStatus && setujuStatus) && !krsMahasiswaIdsData.isEmpty()) {
				krsBelumDisetujui = new HashSet<Long>();
				StringBuilder daftarIdKrs = new StringBuilder();
				for (String idKrs : krsMahasiswaIdsData) {
					if (daftarIdKrs.length() > 0) {
						daftarIdKrs.append(',');
					}
					daftarIdKrs.append(idKrs);
				}
				List<Number> idsBelum = session.createSQLQuery(
						"select distinct k.id from krs_mahasiswa k "
						+ "inner join detailperkuliahan d on (d.mahasiswa=k.mahasiswa "
						+ "and d.tahunakademik=k.tahunakademik and d.semester=k.semester) "
						+ "where d.persetujuan=0 and k.id in (" + daftarIdKrs.toString() + ")").list();
				for (Number idBelum : idsBelum) {
					if (idBelum != null) {
						krsBelumDisetujui.add(Long.valueOf(idBelum.longValue()));
					}
				}
			}
			for (KrsMahasiswa krsMahasiswa : krsMahasiswas) {
				try {
					boolean masihBelumDisetujui = krsMahasiswa != null && krsBelumDisetujui != null
							&& krsBelumDisetujui.contains(krsMahasiswa.getId());
					boolean statusPersetujuanCocok = krsBelumDisetujui == null
							|| (belumStatus && masihBelumDisetujui)
							|| (setujuStatus && !masihBelumDisetujui);
					if (krsMahasiswa != null && krsMahasiswa.getTahunAkademik() != null
							&& krsMahasiswa.getSemester() != null && krsMahasiswa.getSemester() > 0
							&& krsMahasiswa.getMahasiswa() != null
							&& krsMahasiswa.getMahasiswa().getStatusKeluar() == null
							&& statusPersetujuanCocok) {

					if ((tahunAkademik == null || tahunAkademik.equals(krsMahasiswa.getTahunAkademik()))

							&&

							(jenisSemester == null || krsMahasiswa.ambilJenisSemester().equals(jenisSemester))

					) {

//						System.out.println("krsMahasiswa -> " + krsMahasiswa);
						dataDiambil.add(krsMahasiswa);
					}
				}
				} catch (Exception eItemKrs) {
					ais.common.ErrorAuditUtil.record(eItemKrs,
							"filter KRS pembimbing akademik src/ais/database/model/Dosen.java");
				}
			}

//			System.out.println("krsMahasiswa dataDiambil -> " + dataDiambil.size());

			krsMahasiswas = null;
			krsMahasiswaIdsData = null;
		}

		if (ditampilkanHanya.equals(TampilanELearningAction.KEGIATAN)) {
			List<String> ss = retreiveAll(FormulirKegiatanPeserta.class.getName());

			List<String> kegiatanIdData = new ArrayList<String>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && kegiatanIdData.size() > max) {
						break;
					}

					Long id = Long.parseLong(s);
					kegiatanIdData.add(id.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:1648");
				}
			}

			List<FormulirKegiatanPeserta> formulirKegiatanPesertas = GeneralValueObject
					.ambilDataBanyak(FormulirKegiatanPeserta.class, kegiatanIdData);
			List<Long> formulirKegiatanIds = new ArrayList<Long>();
			for (FormulirKegiatanPeserta formulirKegiatanPeserta : formulirKegiatanPesertas) {
				if (formulirKegiatanPeserta != null) {
					FormulirKegiatan formulirKegiatan = formulirKegiatanPeserta.getFormulirKegiatan();
					if (!formulirKegiatanIds.contains(formulirKegiatan.getId())) {

						if (jenisFormulirKegiatan == null && formulirKegiatan.getJenisFormulirKegiatan() == null) {
							formulirKegiatanIds.add(formulirKegiatan.getId());
							dataDiambil.add(formulirKegiatan);
						} else if ((formulirKegiatan.getJenisFormulirKegiatan() != null && jenisFormulirKegiatan != null
								&& jenisFormulirKegiatan.getId()
										.equals(formulirKegiatan.getJenisFormulirKegiatan().getId()))) {
							formulirKegiatanIds.add(formulirKegiatan.getId());
							dataDiambil.add(formulirKegiatan);
						}
					}
				}
			}

			formulirKegiatanPesertas = null;
			formulirKegiatanIds = null;
			kegiatanIdData = null;

		}

		if (ditampilkanHanya.equals(TampilanELearningAction.KONSULTASI)) {
			List<String> ss = retreiveAll(PertemuanPunyaGrupPertemuan.class.getName());

			List<String> pertemuanPunyaGrupPertemuanIdsData = new ArrayList<String>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty()
							&& pertemuanPunyaGrupPertemuanIdsData.size() > max) {
						break;
					}

					Long id = Long.parseLong(s);
					pertemuanPunyaGrupPertemuanIdsData.add(id.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:1700");
				}
			}

			List<PertemuanPunyaGrupPertemuan> pertemuanPunyaGrupPertemuans = GeneralValueObject
					.ambilDataBanyak(PertemuanPunyaGrupPertemuan.class, pertemuanPunyaGrupPertemuanIdsData);
			for (PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan : pertemuanPunyaGrupPertemuans) {
				if (pertemuanPunyaGrupPertemuan != null) {
					dataDiambil.add(pertemuanPunyaGrupPertemuan);
				}
			}

			pertemuanPunyaGrupPertemuans = null;
			pertemuanPunyaGrupPertemuanIdsData = null;

		}

		if (ditampilkanHanya.equals(TampilanELearningAction.PERKULIAHAN)) {

			Map<Long, Perkuliahan> map = new java.util.HashMap<Long, Perkuliahan>();
			for (Long perkuliahanid : ambilPerkuliahan(session)) {
				Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
						perkuliahanid);
				map.put(perkuliahan.getId(), perkuliahan);
			}
			dataDiambil.addAll(map.values());
		}

		try {
			Collections.sort(dataDiambil, Collections.reverseOrder());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1730");
			// TODO: handle exception
		}

		List<VOPembelajaran> diambil = new ArrayList<VOPembelajaran>();
		int index = 0;
		for (VOPembelajaran vaPembelajaran : dataDiambil) {
			if (vaPembelajaran != null) {
				String key = "";
				try {
					if (keyword != null && !keyword.trim().isEmpty()) {
						key = vaPembelajaran.ambilKeyword().toLowerCase();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:1744");
				}
				if ((tahunAkademik == null || tahunAkademik.equals(vaPembelajaran.ambilTahunAjaran()))

						&& (hari == null || hari.trim().isEmpty()
								|| (vaPembelajaran.ambilHari() != null
										&& hari.trim().equalsIgnoreCase(vaPembelajaran.ambilHari().trim())))

						&& (keyword == null || keyword.trim().isEmpty() || key.contains(keyword.toLowerCase().trim()))

						&& (kelas == null || kelas.trim().isEmpty()

								|| vaPembelajaran.ambilKelas().toLowerCase().contains(kelas.toLowerCase().trim())

						)

						&& (ekstrakurikuler == null
								|| (ekstrakurikuler != null && vaPembelajaran.ambilExtraKulikuler() != null
										&& vaPembelajaran.ambilExtraKulikuler().equals(ekstrakurikuler)))

						&& (!merupakanPraPerkuliahan
								|| (merupakanPraPerkuliahan && vaPembelajaran.ambilMerupakanPraPerkuliahan()))

						&& (merupakanPraPerkuliahan || jenisSemester == null
								|| (!jenisSemester.equals(Perkuliahan.SP)
										&& vaPembelajaran.ambilJenisSemester().equals(jenisSemester)
										&& !vaPembelajaran.ambilMerupakanSP())
								|| (jenisSemester.equals(Perkuliahan.SP) && vaPembelajaran.ambilMerupakanSP()))

				) {

					if (vaPembelajaran instanceof Perkuliahan) {

						if (paralel || (!paralel && !vaPembelajaran.ambilMerupakanParalel())) {

							if (!remedial || (remedial && vaPembelajaran.ambilMerupakanRemedial())) {

								if (!paralelAja || (paralelAja && (vaPembelajaran.ambilMerupakanParalel()))) {
									if (index >= mulai && index < (mulai + banyak)) {
										diambil.add(vaPembelajaran);
									}
									index++;
								}
							}
						}
					} else {
						if (index >= mulai && index < (mulai + banyak)) {
							diambil.add(vaPembelajaran);
						}
						index++;
					}
				}
			}
		}

		return new Object[] { diambil, index, dataDiambil };
	}

	/**
	 * Membaca isi mentah berkas indeks JSON pertemuan milik dosen ini
	 * ({@code dosen_punya_pertemuan_&lt;id&gt;}).
	 *
	 * <p>Pemanggil sebaiknya memakai {@link #ambilLokasiPertemuanJsonAman()} yang sudah menangani
	 * berkas terpotong dan mengunci akses; method ini mengembalikan teks apa adanya.</p>
	 *
	 * @return isi berkas indeks, atau {@code VOMahasiswa.dataJSON} (objek JSON kosong) bila berkas
	 *         tidak ada, kosong, atau gagal dibaca
	 */
	public String ambilLokasiPertemuan() {
		File file = Common.getFileLocation(this, "dosen_punya_pertemuan_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1808");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Mengambil objek kunci sinkronisasi untuk indeks pertemuan dosen ini.
	 *
	 * <p>Kunci diambil dari peta statis {@code KUNCI_PERTEMUAN_DOSEN} berdasarkan ID dosen — bukan
	 * {@code this} — karena satu dosen yang sama dapat diwakili beberapa instance Hibernate pada
	 * session berbeda, sehingga mengunci {@code this} tidak mencegah dua thread menulis berkas indeks
	 * yang sama. Dosen yang belum punya ID memakai kunci per-instance
	 * ({@code "baru_" + identityHashCode}). Penyisipan memakai {@code putIfAbsent} agar semua
	 * pemanggil untuk ID yang sama mendapat objek kunci yang identik.
	 *
	 * @return objek kunci yang harus dipakai pada blok {@code synchronized} sebelum membaca-mengubah-
	 *         menulis berkas indeks pertemuan
	 */
	private Object kunciPertemuanDosen() {
		String key = getId() == null ? "baru_" + System.identityHashCode(this) : getId().toString();
		Object baru = new Object();
		Object lama = KUNCI_PERTEMUAN_DOSEN.putIfAbsent(key, baru);
		return lama == null ? baru : lama;
	}

	/**
	 * Membaca indeks pertemuan dengan pemulihan data lama yang pernah terpotong. Pasangan ID yang
	 * masih lengkap dipertahankan; hanya fragmen yang tidak lagi membentuk pasangan JSON yang dibuang.
	 */
	private JSONObject ambilLokasiPertemuanJsonAman() {
		Object kunci = kunciPertemuanDosen();
		synchronized (kunci) {
			String mentah = ambilLokasiPertemuan();
			try {
				return new JSONObject(mentah);
			} catch (JSONException rusak) {
				JSONObject pulih = new JSONObject();
				java.util.regex.Matcher pasangan = java.util.regex.Pattern
						.compile("\\\"([0-9]+)\\\"\\s*:\\s*\\\"([0-9]*)\\\"").matcher(mentah == null ? "" : mentah);
				int jumlahPulih = 0;
				while (pasangan.find()) {
					try {
						pulih.put(pasangan.group(1), pasangan.group(2));
						jumlahPulih++;
					} catch (JSONException abaikanPasangan) {
						System.err.println("[Dosen] Pasangan cache pertemuan tidak dapat dipulihkan: "
								+ abaikanPasangan.getMessage());
					}
				}
				tulisLokasiPertemuan(pulih.toString());
				System.err.println("[Dosen] Cache pertemuan dosen " + getId()
						+ " terpotong; " + jumlahPulih + " pasangan ID dipulihkan secara tersinkron.");
				return pulih;
			}
		}
	}

	/**
	 * Menimpa berkas indeks JSON pertemuan milik dosen ini dengan {@code data}. Kegagalan tulis
	 * direkam ke {@code ErrorAuditUtil} dan tidak dilempar.
	 *
	 * <p>Pemanggil bertanggung jawab memegang kunci {@link #kunciPertemuanDosen()} bila melakukan
	 * siklus baca-ubah-tulis.</p>
	 *
	 * @param data isi berkas indeks yang baru (biasanya {@code JSONObject.toString()})
	 */
	public void tulisLokasiPertemuan(String data) {
		File file = Common.getFileLocation(this, "dosen_punya_pertemuan_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1817");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Menghapus berkas indeks JSON pertemuan milik dosen ini. Dipakai sebelum indeks dibangun ulang
	 * oleh {@link #reInitPertemuan(Session, Label, Date, Date)}.
	 */
	public void bersihkanLokasiPertemuan() {
		File file = Common.getFileLocation(this, "dosen_punya_pertemuan_" + getId().toString());
		BacaTulisUtil.doHapus(file, "dosen_punya_pertemuan");

	}

	/**
	 * Varian {@link #reInitPertemuan(Session, Label, Date, Date)} yang menerima {@link Calendar}.
	 *
	 * <p><b>Perhatikan urutan parameter:</b> {@code sampai} berada sebelum {@code mulai} pada tanda
	 * tangan ini, sedangkan pemanggilan ke varian {@code Date} tetap meneruskannya dalam urutan
	 * (mulai, sampai) yang benar. Urutan ini dipertahankan demi pemanggil lama.</p>
	 *
	 * @param session session Hibernate untuk query pertemuan
	 * @param label   label ZK yang diperbarui sebagai indikator progres
	 * @param sampai  batas akhir rentang tanggal
	 * @param mulai   batas awal rentang tanggal
	 */
	public void reInitPertemuan(Session session, Label label, Calendar sampai, Calendar mulai) {
		reInitPertemuan(session, label, mulai.getTime(), sampai.getTime());
	}

	/**
	 * Membangun ulang indeks pertemuan dosen ini untuk rentang tanggal tertentu.
	 *
	 * <p>Kriteria pertemuan yang diambil: masih aktif (kolom {@code aktif} null atau {@code true}),
	 * bukan pertemuan wisuda, terkait salah satu konteks pembelajaran (perkuliahan, bimbingan tugas
	 * akhir, KKN, PKL, skripsi, KRS, jadwal pelajaran, atau grup pertemuan), dan tanggalnya berada di
	 * dalam rentang {@code mulai}–{@code sampai} (dibandingkan lewat {@code sqlRestriction} pada
	 * {@code date(this_.tanggal)}). Penyaringan keterlibatan dosen didelegasikan ke
	 * {@code DashboardTimelinePertemuan.createCriteriaDosen(...)} dengan seluruh jenis jadwal
	 * diaktifkan.
	 *
	 * <p><b>Efek samping:</b> berkas indeks lama dihapus lalu ditulis ulang dari nol; setiap pertemuan
	 * yang belum ada di cache objek dimuat ({@code session.load}) dan dimasukkan ke cache. Selama
	 * proses berjalan, {@code label} diperbarui dengan persentase kemajuan sehingga method ini harus
	 * dipanggil dari konteks yang punya komponen ZK hidup (mis. tombol "segarkan" di dasbor), bukan
	 * dari batch tanpa UI.</p>
	 *
	 * @param session session Hibernate untuk query dan pemuatan pertemuan
	 * @param label   label ZK yang diperbarui sebagai indikator progres
	 * @param mulai   batas awal rentang tanggal (inklusif)
	 * @param sampai  batas akhir rentang tanggal (inklusif)
	 */
	@SuppressWarnings("unchecked")
	public void reInitPertemuan(Session session, Label label, Date mulai, Date sampai) {

		Criteria criteria = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNull("wisuda"))
				.add(Restrictions.or(Restrictions.isNotNull("perkuliahan"), Restrictions.or(
						Restrictions.isNotNull("mahasiswaRequestTugasAkhir"),
						Restrictions.or(Restrictions.isNotNull("kelompokKkn"), Restrictions.or(
								Restrictions.isNotNull("kelompokPkl"),
								Restrictions.or(Restrictions.isNotNull("skripsi"),
										Restrictions.or(Restrictions.isNotNull("krsMahasiswa"),
												Restrictions.or(Restrictions.isNotNull("jadwalPelajaran"),
														Restrictions.isNotNull("pertemuanPunyaGrupPertemuan")))))))))

				.add(Restrictions
						.sqlRestriction("date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(mulai)
								+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"));
		DashboardTimelinePertemuan.createCriteriaDosen(this, session, criteria, true, true, true, true, true, true,
				true, true, true);

		List<Long> pertemuans = criteria.setProjection(Projections.property("id")).list();

		bersihkanLokasiPertemuan();
		tulisLokasiPertemuan(new JSONObject().toString());
		int i = 0;
		int size = pertemuans.size();
		for (Long pertemuanId : pertemuans) {
			Pertemuan pertemuan = (Pertemuan) ambilData(Pertemuan.class, pertemuanId.toString());
			if (pertemuan == null) {
				pertemuan = (Pertemuan) session.load(Pertemuan.class, pertemuanId);
				masukkanData(Pertemuan.class, pertemuan);
			}
			populatePertemuan(pertemuan, true);
			label.setValue("harap tunggu, sedang mengambil data " + pertemuan.toString() + " ("
					+ Common.numberFormat.get().format((i * 100.0) / size) + "%)");
			i++;
		}
		pertemuans = null;
	}

	/**
	 * Mengeluarkan satu pertemuan dari indeks dosen ini dengan menyetel nilainya menjadi string kosong.
	 *
	 * <p>Seluruh siklus baca-ubah-tulis dibungkus {@code synchronized} pada
	 * {@link #kunciPertemuanDosen()} dan membaca indeks lewat {@link #ambilLokasiPertemuanJsonAman()},
	 * sehingga aman terhadap penulisan serentak maupun berkas yang sebelumnya terpotong.</p>
	 *
	 * @param id ID pertemuan yang dikeluarkan dari indeks; {@code null} diabaikan
	 */
	public void removePertemuan(Serializable id) {
		if (id == null) {
			return;
		}
		Object kunci = kunciPertemuanDosen();
		synchronized (kunci) {
			try {
				JSONObject c = ambilLokasiPertemuanJsonAman();
				c.put(id.toString(), "");
				tulisLokasiPertemuan(c.toString());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1879");
			}
		}
	}

	/**
	 * Menambahkan (atau menyegarkan) satu pertemuan pada indeks dosen ini, dengan penguncian dan
	 * pembacaan aman yang sama seperti {@link #removePertemuan(Serializable)}.
	 *
	 * @param pertemuan  pertemuan yang dicatat; diabaikan bila {@code null} atau belum punya ID
	 * @param tulisUlang parameter warisan yang saat ini <b>tidak dipakai</b> — berkas indeks selalu
	 *                   ditulis ulang. Dipertahankan agar tanda tangan tetap sama dengan pemanggil lama
	 */
	public void populatePertemuan(Pertemuan pertemuan, boolean tulisUlang) {
		if (pertemuan == null || pertemuan.getId() == null) {
			return;
		}
		Object kunci = kunciPertemuanDosen();
		synchronized (kunci) {
			try {
				JSONObject c = ambilLokasiPertemuanJsonAman();
				c.put(pertemuan.getId().toString(), pertemuan.getId().toString());
				tulisLokasiPertemuan(c.toString());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1893");
			}
		}
	}

	/**
	 * Membaca indeks pertemuan dosen ini menjadi peta terurut waktu, sekaligus melengkapi cache objek
	 * untuk pertemuan yang belum termuat.
	 *
	 * <p>Kunci peta dibentuk {@code yyyyMMdd_HH.mm-HH.mm_id} (tanggal, jam mulai–jam selesai, lalu ID)
	 * sehingga {@link TreeMap} otomatis mengurutkan pertemuan secara kronologis dan pemanggil dapat
	 * mencari posisi "hari ini" cukup dengan membandingkan awalan kunci — inilah yang dipakai
	 * {@link #ambilPertemuan(TreeMap, boolean, boolean, boolean, boolean, boolean, boolean, boolean,
	 * boolean, boolean, boolean, String, boolean, boolean, boolean, boolean, boolean, boolean, Date,
	 * String, String, String, String, String, String, String, String, String, String, boolean,
	 * Integer, boolean, boolean, boolean, StatusPertemuan, Integer, PagingApi, boolean, int,
	 * MyToolbarbuttonConfig, Tbmuser, String)} untuk melompat ke halaman berjalan.</p>
	 *
	 * <p>Pertemuan yang belum ada di cache dikumpulkan lebih dulu ke {@code idsBelumAda} lalu dimuat
	 * sekaligus dengan satu query {@code in (...)} (menghindari N+1), dan hanya yang masih aktif yang
	 * dimasukkan ke cache serta ke hasil. Pertemuan tanpa tanggal <b>dilewati</b> di kedua jalur —
	 * dulu kondisi ini memicu {@code NullPointerException} pada pemformatan tanggal.</p>
	 *
	 * @param session session Hibernate untuk memuat pertemuan yang belum ada di cache
	 * @return peta pertemuan terurut waktu dengan nilai berupa ID pertemuan; kosong bila indeks kosong
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<String, Long> ambilPertemuan(Session session) {

		TreeMap<String, Long> pertemuansa = new TreeMap<String, Long>();
		List<Long> idsBelumAda = new ArrayList<Long>();
		try {
			JSONObject c = ambilLokasiPertemuanJsonAman();
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(Pertemuan.class, key);
						if (generalValueObject != null) {
							Pertemuan pertemuan = (Pertemuan) generalValueObject;

							// KE-FIX (NPE Calendar.setTime/SimpleDateFormat.format): pertemuan.getTanggal()
							// null (data tak lengkap) sebelumnya langsung diformat tanpa jaga-jaga. Lewati
							// baris ini (sama seperti perlakuan "belum ada" di bawah), bukan biarkan NPE.
							if (pertemuan.getTanggal() != null) {
								String keyPert = Common.dateFormat8.get().format(pertemuan.getTanggal());

								keyPert += ("_" + (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null
										? "00.00-00.00"
										: (pertemuan.getWaktuMulai() == null ? "00.00" : pertemuan.getWaktuMulai()) + "-"
												+ (pertemuan.getWaktuSelesai() == null ? "00.00"
														: pertemuan.getWaktuSelesai())));

								pertemuansa.put(keyPert + "_" + pertemuan.getId(), pertemuan.getId());
							}
						} else {
							idsBelumAda.add(Long.parseLong(key));
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1928");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:1932");

		}
		if (!idsBelumAda.isEmpty()) {
			// System.out.println("idsBelumAda Pertemuan -> " + idsBelumAda);
			List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.in("id", idsBelumAda)).list();
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan.getAktif()) {
					masukkanData(Pertemuan.class, pertemuan);
					try {
						// KE-FIX (NPE Calendar.setTime/SimpleDateFormat.format): lihat catatan di atas —
						// pertemuan.getTanggal() null harus melewati baris ini, bukan memformat null.
						if (pertemuan.getTanggal() == null) {
							continue;
						}
						String keyPert = Common.dateFormat8.get().format(pertemuan.getTanggal());

						keyPert += ("_" + (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null
								? "00.00-00.00"
								: (pertemuan.getWaktuMulai() == null ? "00.00" : pertemuan.getWaktuMulai()) + "-"
										+ (pertemuan.getWaktuSelesai() == null ? "00.00"
												: pertemuan.getWaktuSelesai())));

						pertemuansa.put(keyPert + "_" + pertemuan.getId(), pertemuan.getId());
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:1954");
					}
				}
			}
		}
		return pertemuansa;
	}

	/**
	 * Varian {@link #ambilPertemuan(TreeMap, boolean, boolean, boolean, boolean, boolean, boolean,
	 * boolean, boolean, boolean, boolean, String, boolean, boolean, boolean, boolean, boolean,
	 * boolean, Date, String, String, String, String, String, String, String, String, String, String,
	 * boolean, Integer, boolean, boolean, boolean, StatusPertemuan, Integer, PagingApi, boolean, int,
	 * MyToolbarbuttonConfig, Tbmuser, String)} dengan urutan {@code "asc"} (kronologis naik).
	 * Seluruh parameter diteruskan apa adanya; lihat method utama untuk maknanya.
	 *
	 * @return daftar ID pertemuan pada halaman berjalan
	 */
	public List<Long> ambilPertemuan(TreeMap<String, Long> pertemuansa, boolean jadwalPerkuliahan, boolean jadwalKkn,
			boolean jadwalPkl, boolean jadwalKegiatan, boolean jadwalRevisi, boolean jadwalKonsultasi,
			boolean jadwalBimbingan, boolean jadwalKonsultasiLain,

			boolean tdpDiskusi, boolean tdpUjian, String namaUjian,

			boolean tdpMateri,

			boolean tdpTugas, boolean tdpCatatan,

			boolean tdpAudio, boolean tdpVideo,

			boolean tdpDosenPengganti, Date tanggal,

			String mk, String dsn,

			String mul, String sam,

			String topik, String catatan,

			String hari, String cariMahasiswa, String cariKelas, String cariRuang,

			boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean remedial, boolean paralelAja,

			boolean online,

			StatusPertemuan statusPertemuan,

			Integer ke,

			PagingApi paging, boolean refresh, int banyak, MyToolbarbuttonConfig back, Tbmuser tbmuser) {
		return ambilPertemuan(pertemuansa, jadwalPerkuliahan, jadwalKkn, jadwalPkl, jadwalKegiatan, jadwalRevisi,
				jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain,

				tdpDiskusi, tdpUjian, namaUjian,

				tdpMateri,

				tdpTugas, tdpCatatan,

				tdpAudio, tdpVideo,

				tdpDosenPengganti, tanggal,

				mk, dsn,

				mul, sam,

				topik, catatan,

				hari, cariMahasiswa, cariKelas, cariRuang,

				merupakanPraPerkuliahan, ekstrakurikuler, remedial, paralelAja,

				online,

				statusPertemuan,

				ke,

				paging, refresh, banyak, back, tbmuser, "asc");
	}

	/**
	 * Jembatan (adapter) drop-in untuk pemanggil dasbor ZK yang masih memakai widget
	 * {@code org.zkoss.zul.Paging} ASLI (bukan {@link PagingApi} ringan versi servlet/API — lihat
	 * javadoc {@link PagingApi}). Status widget yang relevan (halaman aktif + atribut
	 * "mulaiParam"/"sampaiParam" yang dipakai fitur "muat lebih banyak") disalin ke sebuah
	 * {@link PagingApi} sementara, lalu hasil akhirnya (totalSize/pageSize/activePage) disalin balik
	 * ke widget ASLI beserta replikasi {@code setMold}/{@code setPageIncrement} (nilainya tetap/
	 * konstan pada kedua versi) sehingga tampilan paging ZK di layar tetap ter-update persis seperti
	 * sebelum ada overload {@link PagingApi}.
	 */
	public List<Long> ambilPertemuan(TreeMap<String, Long> pertemuansa, boolean jadwalPerkuliahan, boolean jadwalKkn,
			boolean jadwalPkl, boolean jadwalKegiatan, boolean jadwalRevisi, boolean jadwalKonsultasi,
			boolean jadwalBimbingan, boolean jadwalKonsultasiLain,

			boolean tdpDiskusi, boolean tdpUjian, String namaUjian,

			boolean tdpMateri,

			boolean tdpTugas, boolean tdpCatatan,

			boolean tdpAudio, boolean tdpVideo,

			boolean tdpDosenPengganti, Date tanggal,

			String mk, String dsn,

			String mul, String sam,

			String topik, String catatan,

			String hari, String cariMahasiswa, String cariKelas, String cariRuang,

			boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean remedial, boolean paralelAja,

			boolean online,

			StatusPertemuan statusPertemuan,

			Integer ke,

			org.zkoss.zul.Paging paging, boolean refresh, int banyak, MyToolbarbuttonConfig back, Tbmuser tbmuser) {

		PagingApi pagingApi = new PagingApi();
		pagingApi.setActivePage(paging.getActivePage());
		if (paging.getAttribute("mulaiParam") != null) {
			pagingApi.setAttribute("mulaiParam", paging.getAttribute("mulaiParam"));
		}
		if (paging.getAttribute("sampaiParam") != null) {
			pagingApi.setAttribute("sampaiParam", paging.getAttribute("sampaiParam"));
		}

		List<Long> hasil = ambilPertemuan(pertemuansa, jadwalPerkuliahan, jadwalKkn, jadwalPkl, jadwalKegiatan,
				jadwalRevisi, jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain,

				tdpDiskusi, tdpUjian, namaUjian,

				tdpMateri,

				tdpTugas, tdpCatatan,

				tdpAudio, tdpVideo,

				tdpDosenPengganti, tanggal,

				mk, dsn,

				mul, sam,

				topik, catatan,

				hari, cariMahasiswa, cariKelas, cariRuang,

				merupakanPraPerkuliahan, ekstrakurikuler, remedial, paralelAja,

				online,

				statusPertemuan,

				ke,

				pagingApi, refresh, banyak, back, tbmuser, "asc");

		paging.setTotalSize(pagingApi.getTotalSize());
		try {
			paging.setPageSize(pagingApi.getPageSize());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-guard(paging-bridge) src/ais/database/model/Dosen.java"); }
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");
		try {
			paging.setActivePage(pagingApi.getActivePage());
		} catch (Exception e) {
			try {
				paging.setActivePage(pagingApi.getActivePage() - 1);
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-guard(paging-bridge) src/ais/database/model/Dosen.java"); }
		}
		return hasil;
	}

	/**
	 * Mesin penyaringan dan pemenggalan halaman untuk daftar pertemuan dosen — implementasi nyata dari
	 * seluruh overload {@code ambilPertemuan(TreeMap, ...)}.
	 *
	 * <p>Masukannya adalah peta terurut hasil {@link #ambilPertemuan(Session)}. Setiap pertemuan
	 * dimuat dari cache objek, lalu harus lolos berlapis-lapis syarat sebelum masuk hasil:</p>
	 * <ol>
	 * <li><b>Keterlibatan dosen:</b> pertemuan tanpa perkuliahan selalu lolos; pertemuan dengan
	 * perkuliahan hanya lolos bila dosen ini tercatat sebagai dosen pengganti pertemuan tersebut atau
	 * termasuk dalam {@code perkuliahan.populateDosenBuId()};</li>
	 * <li><b>Kelengkapan:</b> pertemuan tanpa tanggal dilewati;</li>
	 * <li><b>Penanda konten:</b> {@code tdpTugas}, {@code tdpCatatan}, {@code tdpDosenPengganti},
	 * {@code tdpMateri}, {@code tdpDiskusi}, {@code tdpUjian}/{@code namaUjian}, {@code tdpAudio},
	 * {@code tdpVideo} — masing-masing membuang pertemuan yang tidak memiliki konten bersangkutan;</li>
	 * <li><b>Jenis jadwal:</b> minimal satu dari {@code jadwalPerkuliahan}, {@code jadwalKkn},
	 * {@code jadwalPkl}, {@code jadwalKegiatan}, {@code jadwalRevisi} (skripsi),
	 * {@code jadwalKonsultasi} (KRS), {@code jadwalBimbingan}, {@code jadwalKonsultasiLain} harus
	 * cocok dengan relasi yang dimiliki pertemuan;</li>
	 * <li><b>Pencarian teks:</b> {@code mk} dicocokkan ke kode/nama mata kuliah, {@code dsn} ke kode/
	 * nama mata kuliah maupun nama sepuluh slot dosen pengampu, {@code topik} ke topik dan judul tugas,
	 * {@code catatan} ke catatan, {@code cariMahasiswa} ke NIM/nama mahasiswa pada skripsi, bimbingan,
	 * atau KRS, {@code cariKelas} ke kelas perkuliahan, {@code cariRuang} ke kode/nama ruang;</li>
	 * <li><b>Rentang jam</b> ({@code mul}–{@code sam}), <b>hari</b> (dengan penyeragaman
	 * {@code "Jum'at"} menjadi {@code "Jumat"}), <b>remedial</b>, <b>paralel</b>,
	 * <b>pra-perkuliahan</b>, <b>ekstrakurikuler</b>, <b>daring</b> ({@code pertemuan.apakahSedang("online")}),
	 * <b>status pertemuan</b>, dan <b>pertemuan ke-</b>.</li>
	 * </ol>
	 *
	 * <p>Hasil yang lolos dikumpulkan ke {@link TreeMap} baru dengan urutan sesuai {@code order}.
	 * Setelah itu {@code paging} diperbarui (total, ukuran halaman, {@code pageIncrement} 5 pada
	 * tampilan mobile atau 10 pada desktop, mold {@code "os"}). Bila {@code refresh} bernilai
	 * {@code true}, halaman aktif dilompatkan ke posisi tanggal hari ini sehingga pengguna langsung
	 * melihat jadwal terkini. Atribut {@code "mulaiParam"}/{@code "sampaiParam"} pada {@code paging}
	 * menimpa perhitungan halaman biasa (dipakai fitur "muat lebih banyak"), dan tombol {@code back}
	 * diberi label jumlah pertemuan sebelumnya serta disembunyikan bila sudah berada di awal daftar.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah state komponen ZK ({@code paging}, {@code back}) — jadi method
	 * ini terikat pada konteks UI, bukan murni pengolah data.</p>
	 *
	 * @param pertemuansa          peta pertemuan terurut hasil {@link #ambilPertemuan(Session)}
	 * @param jadwalPerkuliahan    sertakan pertemuan perkuliahan
	 * @param jadwalKkn            sertakan pertemuan KKN
	 * @param jadwalPkl            sertakan pertemuan PKL
	 * @param jadwalKegiatan       sertakan pertemuan formulir kegiatan
	 * @param jadwalRevisi         sertakan pertemuan revisi/skripsi
	 * @param jadwalKonsultasi     sertakan pertemuan konsultasi KRS (perwalian)
	 * @param jadwalBimbingan      sertakan pertemuan bimbingan tugas akhir
	 * @param jadwalKonsultasiLain sertakan pertemuan grup konsultasi lain
	 * @param tdpDiskusi           hanya pertemuan yang punya diskusi
	 * @param tdpUjian             hanya pertemuan yang punya ujian
	 * @param namaUjian            nama ujian yang dicari; kosong berarti tidak menyaring
	 * @param tdpMateri            hanya pertemuan yang punya berkas materi
	 * @param tdpTugas             hanya pertemuan yang punya judul tugas
	 * @param tdpCatatan           hanya pertemuan yang punya catatan
	 * @param tdpAudio             hanya pertemuan yang punya rekaman audio
	 * @param tdpVideo             hanya pertemuan yang punya rekaman video
	 * @param tdpDosenPengganti    hanya pertemuan yang diampu dosen pengganti
	 * @param tanggal              parameter warisan yang saat ini tidak dipakai dalam penyaringan
	 * @param mk                   penggalan kode/nama mata kuliah
	 * @param dsn                  penggalan kode/nama mata kuliah atau nama dosen pengampu
	 * @param mul                  batas bawah jam mulai (angka desimal sebagai teks); boleh null
	 * @param sam                  batas atas jam selesai (angka desimal sebagai teks); boleh null
	 * @param topik                penggalan topik atau judul tugas
	 * @param catatan              penggalan catatan pertemuan
	 * @param hari                 nama hari; {@code "Jum'at"} otomatis diseragamkan menjadi {@code "Jumat"}
	 * @param cariMahasiswa        penggalan NIM atau nama mahasiswa terkait
	 * @param cariKelas            penggalan nama kelas perkuliahan
	 * @param cariRuang            penggalan kode atau nama ruang
	 * @param merupakanPraPerkuliahan hanya pertemuan pra-perkuliahan
	 * @param ekstrakurikuler      penanda ekstrakurikuler; {@code null} berarti tidak menyaring
	 * @param remedial             hanya perkuliahan remedial
	 * @param paralelAja           hanya kelas paralel
	 * @param online               hanya pertemuan daring
	 * @param statusPertemuan      status pertemuan yang disaring; {@code null} berarti semua
	 * @param ke                   nomor pertemuan ke-; {@code null} berarti semua
	 * @param paging               objek paging yang <b>diperbarui</b> (total, ukuran, halaman aktif)
	 * @param refresh              bila {@code true}, halaman aktif dilompatkan ke tanggal hari ini
	 * @param banyak               jumlah baris per halaman
	 * @param back                 tombol "tampilkan pertemuan sebelumnya" yang label dan visibilitasnya
	 *                             <b>diubah</b> method ini
	 * @param tbmuser              pengguna aktif, dipakai saat memeriksa hak lihat ujian
	 * @param order                {@code "asc"} untuk urutan kronologis naik, selain itu menurun
	 * @return daftar ID pertemuan pada halaman berjalan
	 */
	public List<Long> ambilPertemuan(TreeMap<String, Long> pertemuansa, boolean jadwalPerkuliahan, boolean jadwalKkn,
			boolean jadwalPkl, boolean jadwalKegiatan, boolean jadwalRevisi, boolean jadwalKonsultasi,
			boolean jadwalBimbingan, boolean jadwalKonsultasiLain,

			boolean tdpDiskusi, boolean tdpUjian, String namaUjian,

			boolean tdpMateri,

			boolean tdpTugas, boolean tdpCatatan,

			boolean tdpAudio, boolean tdpVideo,

			boolean tdpDosenPengganti, Date tanggal,

			String mk, String dsn,

			String mul, String sam,

			String topik, String catatan,

			String hari, String cariMahasiswa, String cariKelas, String cariRuang,

			boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean remedial, boolean paralelAja,

			boolean online,

			StatusPertemuan statusPertemuan,

			Integer ke,

			PagingApi paging, boolean refresh, int banyak, MyToolbarbuttonConfig back, Tbmuser tbmuser, String order) {

		TreeMap<String, Long> dikoleksi = order.equalsIgnoreCase("asc") ? new TreeMap<String, Long>()
				: new TreeMap<String, Long>(Collections.reverseOrder());

		for (String key : pertemuansa.keySet()) {
			Long pertemuanId = pertemuansa.get(key);
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanId.toString());
			if (pertemuan != null) {
				Perkuliahan perkuliahan = pertemuan.getPerkuliahan();

				if (perkuliahan == null || (perkuliahan != null
						&& ((pertemuan.getDosenPengganti() != null && pertemuan.getDosenPengganti().equals(getId()))
								|| perkuliahan.populateDosenBuId().contains(this.getId())))) {

					if (pertemuan.getTanggal() != null) {

						if (tdpTugas && pertemuan.getJudultugas().isEmpty()) {
							continue;
						}
						if (tdpCatatan && pertemuan.getCatatan().isEmpty()) {
							continue;
						}
						if (tdpDosenPengganti && pertemuan.getDosenPengganti() == null) {
							continue;
						}
						if (tdpMateri) {
							if (pertemuan.ambilJumlahPertemuanFileContent() == 0) {
								continue;
							}
						}
						if (tdpDiskusi) {
							if (!pertemuan.punyaDiskusi()) {
								continue;
							}
						}
						if (tdpUjian || !namaUjian.trim().isEmpty()) {
							if (!pertemuan.punyaUjian(namaUjian, tbmuser)) {
								continue;
							}
						}

						if ((jadwalPerkuliahan && pertemuan.getPerkuliahan() != null)
								|| (jadwalKkn && pertemuan.getKelompokKkn() != null)
								|| (jadwalPkl && pertemuan.getKelompokPkl() != null)
								|| (jadwalKegiatan && pertemuan.getFormulirKegiatan() != null)
								|| (jadwalRevisi && pertemuan.getSkripsi() != null)
								|| (jadwalKonsultasi && pertemuan.getKrsMahasiswa() != null)
								|| (jadwalBimbingan && pertemuan.getMahasiswaRequestTugasAkhir() != null)
								|| (jadwalKonsultasiLain && pertemuan.getPertemuanPunyaGrupPertemuan() != null)) {

							if (mk.trim().isEmpty() || (perkuliahan != null && perkuliahan.getMatakuliah() != null
									&& perkuliahan.getMatakuliah().getNama() != null
									&& ((perkuliahan.getMatakuliah().getKode().toLowerCase()
											.contains(mk.toLowerCase().trim()))
											|| (perkuliahan.getMatakuliah().getNama() != null
													&& perkuliahan.getMatakuliah().getNama().toLowerCase()
															.contains(mk.toLowerCase().trim()))))) {

								if (dsn == null || dsn.trim().isEmpty()
										|| (perkuliahan != null && perkuliahan.getMatakuliah() != null
												&& (perkuliahan.getMatakuliah().getKode().toLowerCase()
														.contains(dsn.toLowerCase().trim())
														|| (perkuliahan.getMatakuliah().getNama() != null
																&& perkuliahan.getMatakuliah().getNama().toLowerCase()
																		.contains(dsn.toLowerCase().trim()))

														|| (perkuliahan.getDosen1() != null
																&& perkuliahan.getDosen1().getNama() != null
																&& perkuliahan.getDosen1().getNama().toLowerCase()
																		.contains(dsn.toLowerCase().trim()))

														|| (perkuliahan.getDosen2() != null
																&& perkuliahan.getDosen2().getNama() != null
																&& perkuliahan.getDosen2().getNama().toLowerCase()
																		.contains(dsn.toLowerCase().trim()))

														|| (perkuliahan.getDosen3() != null
																&& perkuliahan.getDosen3().getNama() != null
																&& perkuliahan.getDosen3().getNama().toLowerCase()
																		.contains(dsn.toLowerCase().trim()))

														|| (perkuliahan.getDosen4() != null
																&& perkuliahan.getDosen4().getNama() != null
																&& perkuliahan.getDosen4().getNama().toLowerCase()
																		.contains(dsn.toLowerCase().trim()))

														|| (perkuliahan.getDosen5() != null
																&& perkuliahan.getDosen5().getNama() != null
																&& perkuliahan.getDosen5().getNama().toLowerCase()
																		.contains(dsn.toLowerCase().trim()))

														|| (perkuliahan.getDosen6() != null
																&& perkuliahan.getDosen6().getNama() != null
																&& perkuliahan.getDosen6().getNama().toLowerCase()
																		.contains(dsn.toLowerCase().trim()))

														|| (perkuliahan.getDosen7() != null
																&& perkuliahan.getDosen7().getNama() != null
																&& perkuliahan.getDosen7().getNama().toLowerCase()
																		.contains(dsn.toLowerCase().trim()))

														|| (perkuliahan.getDosen8() != null
																&& perkuliahan.getDosen8().getNama() != null
																&& perkuliahan.getDosen8().getNama().toLowerCase()
																		.contains(dsn.toLowerCase().trim()))

														|| (perkuliahan.getDosen9() != null
																&& perkuliahan.getDosen9().getNama() != null
																&& perkuliahan.getDosen9().getNama().toLowerCase()
																		.contains(dsn.toLowerCase().trim()))

														|| (perkuliahan.getDosen10() != null
																&& perkuliahan.getDosen10().getNama() != null
																&& perkuliahan.getDosen10().getNama().toLowerCase()
																		.contains(dsn.toLowerCase().trim()))

												))) {

									if (

									((mul == null && sam == null)

											|| (mul != null && sam != null && pertemuan.getWaktuMulai() != null
													&& Common.isNumber(pertemuan.getWaktuMulai())
													&& pertemuan.getWaktuSelesai() != null
													&& Common.isNumber(pertemuan.getWaktuSelesai())
													&& Double.parseDouble(mul) <= Double
															.parseDouble(pertemuan.getWaktuMulai())
													&& Double.parseDouble(sam) >= Double
															.parseDouble(pertemuan.getWaktuSelesai()))

											|| (mul != null && sam == null && pertemuan.getWaktuMulai() != null
													&& Common.isNumber(pertemuan.getWaktuMulai())
													&& Double.parseDouble(mul) <= Double
															.parseDouble(pertemuan.getWaktuMulai()))

											|| (mul == null && sam != null && pertemuan.getWaktuSelesai() != null
													&& Common.isNumber(pertemuan.getWaktuSelesai())
													&& Double.parseDouble(sam) >= Double
															.parseDouble(pertemuan.getWaktuSelesai())))

											||

											((mul == null && sam == null)

													|| (mul != null && sam != null
															&& pertemuan.getWaktuSelesai() != null
															&& Common.isNumber(pertemuan.getWaktuSelesai())
															&& pertemuan.getWaktuSelesai() != null
															&& Common.isNumber(pertemuan.getWaktuSelesai())
															&& Double.parseDouble(mul) <= Double
																	.parseDouble(pertemuan.getWaktuSelesai())
															&& Double.parseDouble(sam) >= Double
																	.parseDouble(pertemuan.getWaktuSelesai()))

													|| (mul != null && sam == null
															&& pertemuan.getWaktuSelesai() != null
															&& Common.isNumber(pertemuan.getWaktuSelesai())
															&& Double.parseDouble(mul) <= Double
																	.parseDouble(pertemuan.getWaktuSelesai()))

													|| (mul == null && sam != null
															&& pertemuan.getWaktuSelesai() != null
															&& Common.isNumber(pertemuan.getWaktuSelesai())
															&& Double.parseDouble(sam) >= Double
																	.parseDouble(pertemuan.getWaktuSelesai())))

									) {

										if (catatan == null || catatan.trim().isEmpty() || pertemuan.getCatatan()
												.toLowerCase().contains(catatan.trim().toLowerCase()))

											if (topik == null || topik.trim().isEmpty()
													|| pertemuan.getTopik().toLowerCase()
															.contains(topik.trim().toLowerCase())
													|| pertemuan.getJudultugas().toLowerCase()
															.contains(topik.trim().toLowerCase())) {

												if (hari != null && hari.equals("Jum'at")) {
													hari = "Jumat";
												}

												if (hari == null || hari.trim().isEmpty()
														|| (pertemuan.getTanggal() != null
																&& Common.dateFormatHari.get().format(pertemuan.getTanggal())
																		.equalsIgnoreCase(hari))) {

													if (!remedial || (remedial && perkuliahan != null
															&& perkuliahan.getMerupakanRemedial())) {

														if (!paralelAja || (paralelAja && (perkuliahan != null
																&& (perkuliahan.getPerkuliahan_paralel() != null
																		|| perkuliahan.flagParalel)))) {

															if ((!merupakanPraPerkuliahan || (merupakanPraPerkuliahan
																	&& perkuliahan != null
																	&& perkuliahan.getMerupakanPraPerkuliahan()))) {

																if (ekstrakurikuler == null || ((ekstrakurikuler == null
																		&& perkuliahan != null
																		&& !perkuliahan.getMatakuliah()
																				.getExtraKulikuler())
																		|| (ekstrakurikuler.equals(Perkuliahan.EKSTRA)
																				&& perkuliahan != null
																				&& perkuliahan.getMatakuliah()
																						.getExtraKulikuler())

																)) {

																	if (!online || (online
																			&& pertemuan.apakahSedang("online"))) {

																		if (statusPertemuan == null
																				|| (statusPertemuan != null && pertemuan
																						.getStatusPertemuan() != null
																						&& pertemuan
																								.getStatusPertemuan()
																								.getId()
																								.equals(statusPertemuan
																										.getId()))) {

																			if (ke == null || pertemuan.getPertemuanKe()
																					.equals(ke)) {

																				if (!tdpAudio || (tdpAudio && pertemuan
																						.audioPertemuan())) {

																					if (!tdpVideo
																							|| (tdpVideo && pertemuan
																									.videoPertemuan())) {

																						if (cariMahasiswa.trim()
																								.isEmpty()
																								|| (pertemuan
																										.getSkripsi() != null
																										&& pertemuan
																												.getSkripsi()
																												.getMahasiswa() != null
																										&& pertemuan
																												.getSkripsi()
																												.getMahasiswa()
																												.getNim() != null
																										&& pertemuan
																												.getSkripsi()
																												.getMahasiswa()
																												.getNama() != null
																										&& (

																										pertemuan
																												.getSkripsi()
																												.getMahasiswa()
																												.getNim()
																												.toLowerCase()
																												.contains(
																														cariMahasiswa
																																.toLowerCase())

																												||

																												pertemuan
																														.getSkripsi()
																														.getMahasiswa()
																														.getNama()
																														.toLowerCase()
																														.contains(
																																cariMahasiswa
																																		.toLowerCase()))

																								)

																								|| (pertemuan
																										.getMahasiswaRequestTugasAkhir() != null
																										&& pertemuan
																												.getMahasiswaRequestTugasAkhir()
																												.getMahasiswa() != null
																										&& pertemuan
																												.getMahasiswaRequestTugasAkhir()
																												.getMahasiswa()
																												.getNim() != null
																										&& pertemuan
																												.getMahasiswaRequestTugasAkhir()
																												.getMahasiswa()
																												.getNama() != null
																										&& (

																										pertemuan
																												.getMahasiswaRequestTugasAkhir()
																												.getMahasiswa()
																												.getNim()
																												.toLowerCase()
																												.contains(
																														cariMahasiswa
																																.toLowerCase())

																												||

																												pertemuan
																														.getMahasiswaRequestTugasAkhir()
																														.getMahasiswa()
																														.getNama()
																														.toLowerCase()
																														.contains(
																																cariMahasiswa
																																		.toLowerCase()))

																								)

																								|| (pertemuan
																										.getKrsMahasiswa() != null
																										&& pertemuan
																												.getKrsMahasiswa()
																												.getMahasiswa() != null
																										&& pertemuan
																												.getKrsMahasiswa()
																												.getMahasiswa()
																												.getNim() != null
																										&& pertemuan
																												.getKrsMahasiswa()
																												.getMahasiswa()
																												.getNama() != null
																										&& (

																										pertemuan
																												.getKrsMahasiswa()
																												.getMahasiswa()
																												.getNim()
																												.toLowerCase()
																												.contains(
																														cariMahasiswa
																																.toLowerCase())

																												||

																												pertemuan
																														.getKrsMahasiswa()
																														.getMahasiswa()
																														.getNama()
																														.toLowerCase()
																														.contains(
																																cariMahasiswa
																																		.toLowerCase()))

																								)

																						) {

																							if (cariKelas.trim()
																									.isEmpty()
																									|| (perkuliahan != null
																											&& perkuliahan
																													.getKelas() != null
																											&& perkuliahan
																													.getKelas()
																													.trim()
																													.toLowerCase()
																													.contains(
																															cariKelas
																																	.toLowerCase()
																																	.trim()))) {

																								if (cariRuang.trim()
																										.isEmpty()
																										|| (perkuliahan != null
																												&& perkuliahan
																														.getRuang() != null
																												&&

																												(perkuliahan
																														.getRuang()
																														.getKodeRuangan()
																														.trim()
																														.toLowerCase()
																														.contains(
																																cariRuang
																																		.toLowerCase()
																																		.trim())
																														|| (perkuliahan
																																.getRuang()
																																.getNama() != null
																																&& perkuliahan
																																		.getRuang()
																																		.getNama()
																																		.trim()
																																		.toLowerCase()
																																		.contains(
																																				cariRuang
																																						.toLowerCase()
																																						.trim())))

																										)

																								) {

																									dikoleksi.put(key,
																											pertemuanId);
																								}
																							}
																						}
																					}
																				}
																			}

																		}
																	}
																}
															}
														}
													}
												}
											}
									}
								}
							}
						}
					}
				}
			}
		}

		paging.setTotalSize(dikoleksi.size());
		// paging.setVisible(dikoleksi.size() > banyak);
		try {
			paging.setPageSize(banyak);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2479");
		}
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");

		if (refresh) {
			int tengahTengah = 0;
			Date tanggalSekarang = WaktuUtil.getDate();
			String format = Common.dateFormat8.get().format(tanggalSekarang);
			for (String a : dikoleksi.keySet()) {
				try {
					String s = a.split("_")[0];
					if (format.equals(s)) {
						break;
					}
					Date tgl = Common.dateFormat8.get().parse(s);

					if (tgl.before(tanggalSekarang)) {
						tengahTengah = tengahTengah + 1;
					} else {
						tengahTengah = tengahTengah + 1;
						break;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:2503");
				}
			}

			try {
				paging.setActivePage((int) (tengahTengah / banyak));
			} catch (Exception e) {
				try {
					paging.setActivePage((int) ((tengahTengah / banyak) - 1));
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2512");
//					ee.printStackTrace();
				}
			}
		}

		List<Long> diambil = new ArrayList<Long>();
		int index = 0;
		int mulai = banyak * paging.getActivePage();

		if (paging.getAttribute("mulaiParam") != null) {
			mulai = (Integer) paging.getAttribute("mulaiParam");
		}
		if (paging.getAttribute("sampaiParam") != null) {
			banyak = (Integer) paging.getAttribute("sampaiParam");
		}

		if (back.getParent() != null)
			back.getParent().setVisible(mulai > 0);

		int jml = 0;
		for (Long o : dikoleksi.values()) {

			if (index < mulai) {
				jml++;
				back.setLabel("Tampilkan pertemuan sebelumnya.. (" + jml + " pertemuan)");
			}

			if (index >= mulai && index < (mulai + banyak)) {
				diambil.add(o);
			}
			index++;
		}
		dikoleksi.clear();
		dikoleksi = null;
		return diambil;

	}

	/**
	 * ID dosen ini pada mesin absensi sidik jari. Bila kolom kosong, nilai dicari ke berkas cache
	 * {@code retreive("idfinger")} sebagai cadangan, lalu dipangkas spasi.
	 *
	 * @return ID mesin sidik jari, atau {@code null} bila tidak ada di kolom maupun cache
	 */
	public String getIdfinger() {
		String s = idfinger == null || idfinger.trim().isEmpty() ? retreive("idfinger") : idfinger;
		return s == null ? null : s.trim();
	}

	/**
	 * Mengisi ID mesin sidik jari sekaligus mencerminkannya ke berkas cache lewat
	 * {@code put(nilai, "idfinger")}. Nilai null/kosong diabaikan agar ID yang sudah ada tidak hilang.
	 *
	 * @param idfinger ID pada mesin sidik jari; diabaikan bila null/kosong
	 */
	public void setIdfinger(String idfinger) {
		if (idfinger != null && !idfinger.trim().isEmpty()) {
			put(idfinger.trim(), "idfinger");
			this.idfinger = idfinger;
		}
	}

	/**
	 * Implementasi {@link VOMahasiswaDosen#ambilKode()}: kode identitas dosen untuk tampilan dan
	 * laporan — NIDN bila terisi, selain itu {@link #getMycode()}.
	 *
	 * @return NIDN atau kode internal sebagai identitas dosen
	 */
	public String ambilKode() {
		return nidn == null || nidn.trim().isEmpty() ? getMycode() : nidn;
	}

	/**
	 * ID atau tautan profil Google Scholar dosen. Bila kolom kosong, nilai dicari ke berkas cache
	 * {@code retreive("googleScholar")} sebagai cadangan, lalu dipangkas spasi.
	 *
	 * @return ID/tautan Google Scholar, atau {@code null} bila tidak ada
	 * @see #getKodeSinta()
	 */
	public String getGoogleScholar() {
		String s = googleScholar == null || googleScholar.trim().isEmpty() ? retreive("googleScholar") : googleScholar;
		return s == null ? null : s.trim();
	}

	/**
	 * Mengisi ID/tautan Google Scholar dan mencerminkannya ke berkas cache bila nilainya tidak kosong.
	 *
	 * <p>Berbeda dari {@link #setIdfinger(String)}, baris terakhir method ini <b>tetap</b> menyalin
	 * {@code googleScholar} ke field walaupun nilainya null/kosong — jadi setter ini memang dapat
	 * mengosongkan kolom, hanya saja cerminan di berkas cache tidak ikut dihapus.</p>
	 *
	 * @param googleScholar ID/tautan Google Scholar; boleh null untuk mengosongkan kolom
	 */
	public void setGoogleScholar(String googleScholar) {
		if (googleScholar != null && !googleScholar.trim().isEmpty()) {
			put(googleScholar.trim(), "googleScholar");
			this.googleScholar = googleScholar;
		}
		this.googleScholar = googleScholar;
	}

	/**
	 * Tautan ruang kelas daring pribadi dosen (kolom bertipe {@code text}), sudah dipangkas spasi dan
	 * tidak pernah {@code null}. Dipakai bersama {@link #getOnlineMenggunakan()} yang menentukan
	 * platformnya.
	 *
	 * @return tautan kelas daring, atau string kosong bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getOnlineLink() {
		return onlineLink == null ? "" : onlineLink.trim();
	}

	/**
	 * Mengisi tautan ruang kelas daring pribadi dosen.
	 *
	 * @param onlineLink tautan kelas daring
	 */
	public void setOnlineLink(String onlineLink) {
		this.onlineLink = onlineLink;
	}

	/**
	 * Kode profil SINTA (Science and Technology Index) dosen, sudah dipangkas spasi dan tidak pernah
	 * {@code null}.
	 *
	 * @return kode SINTA, atau string kosong bila belum diisi
	 * @see #getGoogleScholar()
	 */
	public String getKodeSinta() {
		return kodeSinta == null ? "" : kodeSinta.trim();
	}

	/**
	 * Mengisi kode profil SINTA dosen.
	 *
	 * @param kodeSinta kode SINTA
	 */
	public void setKodeSinta(String kodeSinta) {
		this.kodeSinta = kodeSinta;
	}

	/**
	 * Implementasi {@link VOMahasiswaDosen#ambilMateri(TreeMap, boolean, Label)}: mengumpulkan berkas
	 * materi dari sekumpulan pertemuan, didelegasikan sepenuhnya ke
	 * {@link ais.database.model.file.PertemuanFileContent}. Pengguna aktif diambil sendiri lewat
	 * {@code Common.getCurrentUser()} sehingga method ini hanya boleh dipanggil dari konteks yang
	 * punya session pengguna.
	 *
	 * @param pertemuans peta pertemuan (kunci terurut waktu) yang materinya dikumpulkan
	 * @param refresh    bila {@code true}, materi diambil ulang tanpa memakai cache
	 * @param label      label ZK indikator progres
	 * @return peta materi per pertemuan
	 * @see #ambilMateri(TreeMap, boolean, Label, boolean, Tbmuser)
	 */
	@Override
	public TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label) {
		Tbmuser tbmuser = Common.getCurrentUser();
		return PertemuanFileContent.ambilMateri(pertemuans, refresh, label, tbmuser);
	}

	/**
	 * Varian {@link #ambilMateri(TreeMap, boolean, Label)} dengan pengguna diberikan secara eksplisit
	 * (berguna di luar konteks session ZK) dan pilihan pengurutan menurut nama berkas.
	 *
	 * @param pertemuans            peta pertemuan yang materinya dikumpulkan
	 * @param refresh               bila {@code true}, materi diambil ulang tanpa memakai cache
	 * @param label                 label ZK indikator progres
	 * @param urutBerdasarkanNama   bila {@code true}, materi diurutkan menurut nama berkas
	 * @param tbmuser               pengguna yang hak aksesnya dipakai saat menyaring materi
	 * @return peta materi per pertemuan
	 */
	public TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label,
			boolean urutBerdasarkanNama, Tbmuser tbmuser) {
		return PertemuanFileContent.ambilMateri(pertemuans, refresh, label, urutBerdasarkanNama, tbmuser);
	}

	/**
	 * Penanda anti-rekursi per-thread. {@link #getAgama()}, {@link #getStatusPerkawinan()},
	 * dll memanggil {@code ambilBiodata()}; query di dalamnya memicu auto-flush Hibernate
	 * yang membaca lagi {@code getAgama()} → {@code ambilBiodata()} → query → ... sehingga
	 * terjadi rekursi tak terhingga (StackOverflowError). Penanda ini memutus siklus tersebut.
	 */
	private static final ThreadLocal<Boolean> AMBIL_BIODATA_AKTIF = new ThreadLocal<Boolean>();

	/**
	 * Varian {@link #ambilBiodata(boolean)} dengan {@code jikaTidakAdaSimpan = true}: bila dosen ini
	 * belum punya baris biodata, satu baris kosong <b>dibuat dan disimpan ke basis data</b>.
	 *
	 * @return biodata dosen; {@code null} hanya bila dosen belum punya ID
	 */
	public BiodataDosen ambilBiodata() {
		return ambilBiodata(true);
	}

	/**
	 * Mengambil {@link BiodataDosen} milik dosen ini — data pelengkap (agama, status perkawinan,
	 * nomor HP/telepon rumah, dan sebagainya) yang disimpan pada tabel terpisah.
	 *
	 * <p>Urutan pencarian: field {@code biodataDosen} yang sudah terisi lengkap dipakai langsung;
	 * bila belum, seluruh biodata di cache {@code ConstantValues.ambilBerdasarClass(BiodataDosen.class)}
	 * ditelusuri untuk mencari yang dosennya cocok; bila masih belum ketemu, dijalankan query
	 * Hibernate memakai session native (diambil baris ber-ID terbesar).</p>
	 *
	 * <p><b>Efek samping penting:</b> bila {@code jikaTidakAdaSimpan} bernilai {@code true} dan biodata
	 * tetap tidak ditemukan, method ini <b>membuka transaksi sendiri lalu menyimpan baris
	 * {@code BiodataDosen} baru</b> — sebuah operasi tulis yang tersembunyi di balik nama "ambil".
	 * Pemanggil yang hanya ingin membaca wajib memakai {@code false}.</p>
	 *
	 * <p><b>Penjaga anti-rekursi:</b> {@code ThreadLocal} {@code AMBIL_BIODATA_AKTIF} memutus siklus
	 * {@code getAgama()} → {@code ambilBiodata()} → query → auto-flush Hibernate → {@code getAgama()}
	 * yang sebelumnya menyebabkan {@code StackOverflowError}. Saat penanda aktif, method langsung
	 * mengembalikan nilai field apa adanya tanpa query maupun penyimpanan.</p>
	 *
	 * @param jikaTidakAdaSimpan bila {@code true}, baris biodata kosong dibuat dan disimpan ketika
	 *                           belum ada; bila {@code false}, method murni membaca
	 * @return biodata dosen, atau {@code null} bila dosen belum punya ID atau biodata tidak ada dan
	 *         tidak diminta dibuat
	 */
	@SuppressWarnings("unchecked")
	public BiodataDosen ambilBiodata(boolean jikaTidakAdaSimpan) {

		if (biodataDosen != null && biodataDosen.getId() != null && biodataDosen.getDosen() != null) {
			return biodataDosen;
		}

		if (getId() == null) {
			return null;
		}

		// Bila method ini sudah berjalan di thread yang sama (dipanggil ulang oleh auto-flush
		// Hibernate saat getAgama() dibaca), kembalikan nilai apa adanya tanpa query/simpan
		// agar tidak terjadi rekursi tak terhingga.
		if (Boolean.TRUE.equals(AMBIL_BIODATA_AKTIF.get())) {
			return biodataDosen;
		}
		AMBIL_BIODATA_AKTIF.set(Boolean.TRUE);
		try {

		Map<Long, GeneralValueObject> map = ConstantValues.ambilBerdasarClass(BiodataDosen.class);
		if (map != null) {

			boolean ketemu = false;
			for (Long generalValueObjectid : map.keySet()) {
				BiodataDosen b = (BiodataDosen) ConstantValues.ambil(BiodataDosen.class.getName(),
						generalValueObjectid);
				if (b != null && b.getDosen() != null && b.getDosen().getId().equals(getId())) {
					biodataDosen = b;
					ketemu = true;
					break;
				}

				if (ketemu) {
					break;
				}
			}
		}

		if (biodataDosen == null || biodataDosen.getId() == null) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				biodataDosen = (BiodataDosen) session.createCriteria(BiodataDosen.class)
						.add(Restrictions.eq("dosen", this)).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:2665");
			}
			HibernateUtil.closeSession();
		}

		if (jikaTidakAdaSimpan) {
			if (biodataDosen == null || biodataDosen.getId() == null) {
				try {
					biodataDosen = new BiodataDosen();
					biodataDosen.setDosen(this);
					Session session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.save(biodataDosen);
					session.getTransaction().commit();
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2684");
//					e.printStackTrace();
				}
				HibernateUtil.closeSession();
			}
		}

		return biodataDosen;

		} finally {
			AMBIL_BIODATA_AKTIF.remove();
		}
	}

	/**
	 * Merender alamat surel dosen sebagai tombol ZK di dalam {@code vbox}. Bila surel terisi, tombol
	 * diberi ikon amplop dan tautan {@code mailto:} yang terbuka di tab baru; bila kosong, tombol tetap
	 * dibuat namun tanpa tautan.
	 *
	 * <p><b>Efek samping:</b> membentuk dan menempelkan komponen UI — hanya untuk dipanggil dari
	 * lapisan tampilan.</p>
	 *
	 * @param vbox komponen induk tempat tombol ditempelkan
	 */
	public void tampilkanEmail(Component vbox) {
		String email = getEmail();
		Toolbarbutton a;
		(a = new ais.ui.util.MyToolbarbuttonConfig(email)).setParent(vbox);
		if (email != null && !email.trim().isEmpty()) {
			a.setImage("/img/svg/mail-send-line.svg");
			a.setStyle("font-size:9px;");
			a.setTarget("_blank");
			a.setHref("mailto:" + email);
		}

	}

	/**
	 * Merender nomor HP/telepon dosen sebagai tombol ZK bertaut WhatsApp di dalam {@code vbox}.
	 *
	 * <p>Nomor diambil dari {@link BiodataDosen} ({@code hp} dan {@code teleponRumah}). Nomor
	 * "pengisi" yang lazim ditemui pada data warisan — {@code 08100000000000000000},
	 * {@code 0000000000}, {@code 00000000000000000000}, {@code 000000000} — diperlakukan sebagai
	 * kosong. Bila HP kosong, nomor telepon rumah dipakai sebagai gantinya. Nomor kemudian
	 * dinormalkan ke format internasional ({@code 08...} dan {@code 0...} menjadi {@code +62...},
	 * selain itu diberi awalan {@code +62}) sebelum dipasang pada tautan
	 * {@code https://web.whatsapp.com/send}.
	 *
	 * <p>Bila pembacaan biodata gagal, blok {@code catch} membangun ulang tombol dengan nomor dari
	 * {@link #getTelp()} memakai normalisasi yang sama — jadi tampilan tetap muncul meski biodata
	 * tidak tersedia.</p>
	 *
	 * <p><b>Efek samping:</b> membentuk dan menempelkan komponen UI; pemanggilan
	 * {@link #ambilBiodata()} di dalamnya dapat membuat baris biodata baru di basis data.</p>
	 *
	 * @param vbox komponen induk tempat tombol ditempelkan
	 */
	public void tampilkanHp(Component vbox) {
		try {
			BiodataDosen biodataDosen = ambilBiodata();

			String hp = biodataDosen.getHp();
			String telp = biodataDosen.getTeleponRumah();

			Toolbarbutton a;
			(a = new ais.ui.util.MyToolbarbuttonConfig(
					(hp == null || hp.toString().trim().equals("08100000000000000000")
							|| hp.toString().trim().equals("0000000000") ? "" : hp)
							+ (telp == null || telp.toString().trim().isEmpty()
									|| telp.toString().trim().equals("00000000000000000000")
									|| telp.toString().trim().equals("000000000")
											? ""
											: (hp == null || hp.toString().trim().isEmpty()
													|| hp.toString().trim().equals("08100000000000000000")
													|| hp.toString().trim().equals("0000000000") ? "" : " / ") + telp)))
					.setParent(vbox);

			if (telp != null && !telp.trim().isEmpty() && hp != null && hp.equals(telp)) {
				a.setLabel(hp);
			}

			if (hp == null || hp.toString().trim().isEmpty() || hp.toString().trim().equals("08100000000000000000")
					|| hp.toString().trim().equals("0000000000")) {
				hp = telp;
			}

			if (hp != null && !hp.trim().isEmpty()
					&& !(hp == null || hp.toString().trim().isEmpty()
							|| hp.toString().trim().equals("00000000000000000000")
							|| hp.toString().trim().equals("000000000"))) {
				hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
				hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
				hp = !hp.startsWith("+") ? "+62" + hp : hp;
				a.setStyle("font-size:9px;");
				a.setImage("/img/svg/whats.svg");
				a.setTarget("_blank");
				a.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text=Halo,+apa+kabar?");
			}
		} catch (Exception e) {
			A a;
			String hp = getTelp();
			(a = new A(hp)).setParent(vbox);
			if (hp != null && !hp.trim().isEmpty()
					&& !(hp == null || hp.toString().trim().isEmpty()
							|| hp.toString().trim().equals("00000000000000000000")
							|| hp.toString().trim().equals("000000000"))) {
				hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
				hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
				hp = !hp.startsWith("+") ? "+62" + hp : hp;
				a.setStyle("font-size:9px;");
				a.setImage("/img/svg/whats.svg");
				a.setTarget("_blank");
				a.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text=Halo,+apa+kabar?");
			}
		}
	}

	// KEGIATAN KEDOSENAN

	public String ambilLokasiKegiatanKedosenanPunyaDosen() {
		File file = Common.getFileLocation(this, "kegiatanKedosenanPunyaDosen_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2779");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiKegiatanKedosenanPunyaDosen(String data) {
		File file = Common.getFileLocation(this, "kegiatanKedosenanPunyaDosen_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2788");
		}
	}

	public void populateKegiatanKedosenanPunyaDosen(KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen) {
		try {
			JSONObject c = new JSONObject(ambilLokasiKegiatanKedosenanPunyaDosen());
			kegiatanKedosenanPunyaDosen.write();
			c.put(kegiatanKedosenanPunyaDosen.getId().toString(), kegiatanKedosenanPunyaDosen.getId().toString());
			tulisLokasiKegiatanKedosenanPunyaDosen(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2798");
		}
	}

	@SuppressWarnings("unchecked")
	public void reInitKegiatanKedosenanPunyaDosen(Session session) {

		List<KegiatanKedosenanPunyaDosen> kegiatanKedosenanPunyaDosens = session
				.createCriteria(KegiatanKedosenanPunyaDosen.class).add(Restrictions.eq("dosen", this))
				.addOrder(Order.asc("id")).list();
		tulisLokasiKegiatanKedosenanPunyaDosen(new JSONObject().toString());
		for (KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen : kegiatanKedosenanPunyaDosens) {
			populateKegiatanKedosenanPunyaDosen(kegiatanKedosenanPunyaDosen);
		}
		kegiatanKedosenanPunyaDosens = null;
	}

	public void removeKegiatanKedosenanPunyaDosen(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiKegiatanKedosenanPunyaDosen());
			c.put(id.toString(), "");
			tulisLokasiKegiatanKedosenanPunyaDosen(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2820");

		}
	}

	@SuppressWarnings("unchecked")
	public List<Long> ambilKegiatanKedosenanPunyaDosen() {

		if (!udah("KegiatanKedosenanPunyaDosen")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitKegiatanKedosenanPunyaDosen(session);
			HibernateUtil.closeSession();
		}

		List<Long> kegiatanKedosenanPunyaDosens = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiKegiatanKedosenanPunyaDosen());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen = (KegiatanKedosenanPunyaDosen) ambilData(
								KegiatanKedosenanPunyaDosen.class, key, true);
						if (kegiatanKedosenanPunyaDosen != null) {
							kegiatanKedosenanPunyaDosens.add(kegiatanKedosenanPunyaDosen.getId());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:2850");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:2854");
		}

		return kegiatanKedosenanPunyaDosens;
	}

	// ORGANISASI DOSEN

	public String ambilLokasiOrganisasiDosenPunyaDosen() {
		File file = Common.getFileLocation(this, "organisasiDosenPunyaDosen_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2868");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiOrganisasiDosenPunyaDosen(String data) {
		File file = Common.getFileLocation(this, "organisasiDosenPunyaDosen_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2877");
		}
	}

	public void populateOrganisasiDosenPunyaDosen(OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen) {
		try {
			JSONObject c = new JSONObject(ambilLokasiOrganisasiDosenPunyaDosen());
			organisasiDosenPunyaDosen.write();
			c.put(organisasiDosenPunyaDosen.getId().toString(), organisasiDosenPunyaDosen.getId().toString());
			tulisLokasiOrganisasiDosenPunyaDosen(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2887");
		}
	}

	@SuppressWarnings("unchecked")
	public void reInitOrganisasiDosenPunyaDosen(Session session) {

		List<OrganisasiDosenPunyaDosen> organisasiDosenPunyaDosens = session
				.createCriteria(OrganisasiDosenPunyaDosen.class).add(Restrictions.eq("dosen", this))
				.addOrder(Order.asc("id")).list();
		tulisLokasiOrganisasiDosenPunyaDosen(new JSONObject().toString());
		for (OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen : organisasiDosenPunyaDosens) {
			populateOrganisasiDosenPunyaDosen(organisasiDosenPunyaDosen);
		}
		organisasiDosenPunyaDosens = null;
	}

	public void removeOrganisasiDosenPunyaDosen(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiOrganisasiDosenPunyaDosen());
			c.put(id.toString(), "");
			tulisLokasiOrganisasiDosenPunyaDosen(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2909");

		}
	}

	@SuppressWarnings("unchecked")
	public List<Long> ambilOrganisasiDosenPunyaDosen() {

		if (!udah("OrganisasiDosenPunyaDosen")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitOrganisasiDosenPunyaDosen(session);
			HibernateUtil.closeSession();
		}

		List<Long> organisasiDosenPunyaDosens = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiOrganisasiDosenPunyaDosen());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen = (OrganisasiDosenPunyaDosen) ambilData(
								OrganisasiDosenPunyaDosen.class, key, true);
						if (organisasiDosenPunyaDosen != null) {
							organisasiDosenPunyaDosens.add(organisasiDosenPunyaDosen.getId());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:2939");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:2943");
		}

		return organisasiDosenPunyaDosens;
	}

	// PRESTASI DOSEN

	public String ambilLokasiPrestasiDosen() {
		File file = Common.getFileLocation(this, "prestasiDosen_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2957");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiPrestasiDosen(String data) {
		File file = Common.getFileLocation(this, "prestasiDosen_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2966");
		}
	}

	public void populatePrestasiDosen(PrestasiDosen prestasiDosen) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPrestasiDosen());
			prestasiDosen.write();
			c.put(prestasiDosen.getId().toString(), prestasiDosen.getId().toString());
			tulisLokasiPrestasiDosen(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2976");
		}
	}

	@SuppressWarnings("unchecked")
	public void reInitPrestasiDosen(Session session) {

		List<PrestasiDosen> prestasiDosens = session.createCriteria(PrestasiDosen.class)
				.add(Restrictions.eq("dosen", this)).addOrder(Order.asc("id")).list();
		tulisLokasiPrestasiDosen(new JSONObject().toString());
		for (PrestasiDosen prestasiDosen : prestasiDosens) {
			populatePrestasiDosen(prestasiDosen);
		}
		prestasiDosens = null;
	}

	public void removePrestasiDosen(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPrestasiDosen());
			c.put(id.toString(), "");
			tulisLokasiPrestasiDosen(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:2997");

		}
	}

	@SuppressWarnings("unchecked")
	public List<Long> ambilPrestasiDosen() {

		if (!udah("PrestasiDosen")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitPrestasiDosen(session);
			HibernateUtil.closeSession();
		}

		List<Long> prestasiDosens = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiPrestasiDosen());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						PrestasiDosen prestasiDosen = (PrestasiDosen) ambilData(PrestasiDosen.class, key, true);
						if (prestasiDosen != null) {
							prestasiDosens.add(prestasiDosen.getId());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:3026");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:3030");
		}

		return prestasiDosens;
	}

	// KARYA DOSEN

	public String ambilLokasiPenghargaanDosen() {
		File file = Common.getFileLocation(this, "penghargaanDosen_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3044");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiPenghargaanDosen(String data) {
		File file = Common.getFileLocation(this, "penghargaanDosen_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3053");
		}
	}

	public void populatePenghargaanDosen(PenghargaanDosen penghargaanDosen) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPenghargaanDosen());
			penghargaanDosen.write();
			c.put(penghargaanDosen.getId().toString(), penghargaanDosen.getId().toString());
			tulisLokasiPenghargaanDosen(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3063");
		}
	}

	@SuppressWarnings("unchecked")
	public void reInitPenghargaanDosen(Session session) {

		List<PenghargaanDosen> penghargaanDosens = session.createCriteria(PenghargaanDosen.class)
				.add(Restrictions.eq("dosen", this)).addOrder(Order.asc("id")).list();
		tulisLokasiPenghargaanDosen(new JSONObject().toString());
		for (PenghargaanDosen penghargaanDosen : penghargaanDosens) {
			populatePenghargaanDosen(penghargaanDosen);
		}
		penghargaanDosens = null;
	}

	public void removePenghargaanDosen(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPenghargaanDosen());
			c.put(id.toString(), "");
			tulisLokasiPenghargaanDosen(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3084");

		}
	}

	@SuppressWarnings("unchecked")
	public List<Long> ambilPenghargaanDosen() {

		if (!udah("PenghargaanDosen")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitPenghargaanDosen(session);
			HibernateUtil.closeSession();
		}

		List<Long> penghargaanDosens = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiPenghargaanDosen());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						PenghargaanDosen penghargaanDosen = (PenghargaanDosen) ambilData(PenghargaanDosen.class, key,
								true);
						if (penghargaanDosen != null) {
							penghargaanDosens.add(penghargaanDosen.getId());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:3114");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:3118");
		}

		return penghargaanDosens;
	}

	// PENELITIAN DAN PENGABDIAN

	public String ambilLokasiPengajuanPenelitianDanPengabdian() {
		File file = Common.getFileLocation(this, "pengajuanPenelitianDanPengabdian_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3132");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiPengajuanPenelitianDanPengabdian(String data) {
		File file = Common.getFileLocation(this, "pengajuanPenelitianDanPengabdian_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3141");
		}
	}

	public void populatePengajuanPenelitianDanPengabdian(
			PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPengajuanPenelitianDanPengabdian());
			pengajuanPenelitianDanPengabdian.write();
			c.put(pengajuanPenelitianDanPengabdian.getId().toString(),
					pengajuanPenelitianDanPengabdian.getId().toString());
			tulisLokasiPengajuanPenelitianDanPengabdian(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3153");
		}
	}

	@SuppressWarnings("unchecked")
	public void reInitPengajuanPenelitianDanPengabdian(Session session) {

		List<PengajuanPenelitianDanPengabdian> pengajuanPenelitianDanPengabdians = session
				.createCriteria(PengajuanPenelitianDanPengabdian.class).createAlias("tbmuser", "tbmuser")
				.add(Restrictions.eq("tbmuser.dosen", this)).addOrder(Order.asc("id")).list();
		tulisLokasiPengajuanPenelitianDanPengabdian(new JSONObject().toString());
		for (PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian : pengajuanPenelitianDanPengabdians) {
			populatePengajuanPenelitianDanPengabdian(pengajuanPenelitianDanPengabdian);
		}
		pengajuanPenelitianDanPengabdians = null;
	}

	public void removePengajuanPenelitianDanPengabdian(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPengajuanPenelitianDanPengabdian());
			c.put(id.toString(), "");
			tulisLokasiPengajuanPenelitianDanPengabdian(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3175");

		}
	}

	@SuppressWarnings("unchecked")
	public List<Long> ambilPengajuanPenelitianDanPengabdian(TipePenelitianDanPengabdian tipePenelitianDanPengabdian) {

		if (!udah("PengajuanPenelitianDanPengabdian")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitPengajuanPenelitianDanPengabdian(session);
			HibernateUtil.closeSession();
		}

		List<Long> pengajuanPenelitianDanPengabdians = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiPengajuanPenelitianDanPengabdian());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian = (PengajuanPenelitianDanPengabdian) ambilData(
								PengajuanPenelitianDanPengabdian.class, key, true);
						if (pengajuanPenelitianDanPengabdian != null) {
							if (tipePenelitianDanPengabdian == null
									|| tipePenelitianDanPengabdian.getId().equals(pengajuanPenelitianDanPengabdian
											.getPenelitianDanPengabdian().getTipePenelitianDanPengabdian().getId())) {
								pengajuanPenelitianDanPengabdians.add(pengajuanPenelitianDanPengabdian.getId());
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:3209");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:3213");
		}

		return pengajuanPenelitianDanPengabdians;
	}

	// ARTIKEL

	public String ambilLokasiArtikel() {
		File file = Common.getFileLocation(this, "artikel_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3227");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiArtikel(String data) {
		File file = Common.getFileLocation(this, "artikel_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3236");
		}
	}

	public void populateArtikel(Artikel artikel) {
		try {
			JSONObject c = new JSONObject(ambilLokasiArtikel());
			artikel.write();
			c.put(artikel.getId().toString(), artikel.getId().toString());
			tulisLokasiArtikel(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3246");
		}
	}

	@SuppressWarnings("unchecked")
	public void reInitArtikel(Session session) {

		List<Artikel> artikels = session.createCriteria(Artikel.class).createAlias("tbmuser", "tbmuser")
				.add(Restrictions.eq("tbmuser.dosen", this)).addOrder(Order.asc("id")).list();
		tulisLokasiArtikel(new JSONObject().toString());
		for (Artikel artikel : artikels) {
			populateArtikel(artikel);
		}
		artikels = null;
	}

	public void removeArtikel(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiArtikel());
			c.put(id.toString(), "");
			tulisLokasiArtikel(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3267");

		}
	}

	@SuppressWarnings("unchecked")
	public List<Long> ambilArtikel() {

		if (!udah("Artikel")) {
			Session session = HibernateUtil.currentNativeSession();
			try {
				reInitArtikel(session);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3279");
				// TODO: handle exception
			} finally {
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3284");
					// TODO: handle exception
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3289");
					// TODO: handle exception
				}
				HibernateUtil.closeSession();
			}

		}

		List<Long> artikels = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiArtikel());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						Artikel artikel = (Artikel) ambilData(Artikel.class, key, true);
						if (artikel != null) {
							artikels.add(artikel.getId());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:3312");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:3316");
		}

		return artikels;
	}

	// BUKU DOSEN

	public String ambilLokasiBukuBahanAjar() {
		File file = Common.getFileLocation(this, "bukuBahanAjar_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3330");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiBukuBahanAjar(String data) {
		File file = Common.getFileLocation(this, "bukuBahanAjar_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3339");
		}
	}

	public void populateBukuBahanAjar(BukuBahanAjar bukuBahanAjar) {
		try {
			JSONObject c = new JSONObject(ambilLokasiBukuBahanAjar());
			bukuBahanAjar.write();
			c.put(bukuBahanAjar.getId().toString(), bukuBahanAjar.getId().toString());
			tulisLokasiBukuBahanAjar(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3349");
		}
	}

	@SuppressWarnings("unchecked")
	public void reInitBukuBahanAjar(Session session) {

		Criterion c = Restrictions.eq("dosenPengarang1", this);
		c = Restrictions.or(c, Restrictions.eq("dosenPengarang2", this));
		c = Restrictions.or(c, Restrictions.eq("dosenPengarang3", this));
		c = Restrictions.or(c, Restrictions.eq("dosenPengarang4", this));
		c = Restrictions.or(c, Restrictions.eq("dosenPengarang5", this));

		List<BukuBahanAjar> bukuBahanAjars = session.createCriteria(BukuBahanAjar.class).add(c)
				.addOrder(Order.asc("id")).list();
		tulisLokasiBukuBahanAjar(new JSONObject().toString());
		for (BukuBahanAjar bukuBahanAjar : bukuBahanAjars) {
			populateBukuBahanAjar(bukuBahanAjar);
		}
		bukuBahanAjars = null;
	}

	public void removeBukuBahanAjar(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiBukuBahanAjar());
			c.put(id.toString(), "");
			tulisLokasiBukuBahanAjar(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Dosen.java:3376");

		}
	}

	@SuppressWarnings("unchecked")
	public List<Long> ambilBukuBahanAjar() {

		if (!udah("BukuBahanAjar")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitBukuBahanAjar(session);
			HibernateUtil.closeSession();
		}

		List<Long> bukuBahanAjars = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiBukuBahanAjar());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						BukuBahanAjar bukuBahanAjar = (BukuBahanAjar) ambilData(BukuBahanAjar.class, key, true);
						if (bukuBahanAjar != null) {
							bukuBahanAjars.add(bukuBahanAjar.getId());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:3405");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Dosen.java:3409");
		}

		return bukuBahanAjars;
	}

	public static String DEFAULT_FORMULA = new JSONArray().toString();

	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? DEFAULT_FORMULA : formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "golongan_pns", nullable = true)
	public GolonganPns getGolonganPns() {
		golonganPns = check(golonganPns);

		if (golonganPns == null && getGolonganPegawai() != null) {
			for (Object g : ConstantValues.ambilBerdasarClass(GolonganPns.class).values()) {
				GolonganPns gol = (GolonganPns) g;
				if (gol.getKode().equalsIgnoreCase(getGolonganPegawai().getNama())) {
					golonganPns = gol;
					break;
				}
			}
		}
		return golonganPns;
	}

	public void setGolonganPns(GolonganPns golonganPns) {
		this.golonganPns = golonganPns;
	}

	public String getHp() {
		return hp == null || hp.isEmpty() ? "" : hp.trim().replaceAll("[^\\d.]", "");
	}

	public void setHp(String hp) {
		this.hp = hp;
	}

	public String ttdQr() {

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/ttd_dsn_" + getId() + ".png");
		if (!myfilebarcode.exists()) {
			String code = (getNidn() == null || getNidn().trim().isEmpty() ? "" : getNidn() + "\n")
					+ (getMycode() == null || getMycode().trim().isEmpty() ? "" : getMycode() + "\n")
					+ (getKode() == null || getKode().trim().isEmpty() ? "" : getKode() + "\n") + getNama() + "\n"
					+ (getJurusan() == null ? "" : getJurusan().getNama() + "\n")
					+ (getFakultas() == null ? "" : getFakultas().getNama() + "\n")
					+ (getPerguruanTinggi() == null ? "" : getPerguruanTinggi().getNama() + "\n")
					+ Common.getRequestHostWithProtocol();
			BarcodeCommon.generateCRCode(code, myfilebarcode);
		}
		return myfilebarcode.getAbsolutePath();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putPhoto(Map parameters) {
		try {
			Dosen dosen = this;

			FileFotoLain fotodosen = FileFotoLain.ambil(dosen.getId(), FotoDosen.DEFAULT_JENIS, FotoDosen.class);

			if (fotodosen != null && fotodosen.ambilFile() != null) {
				parameters.put("foto", fotodosen.ambilFile().getAbsolutePath());
				parameters.put("foto_dosen", fotodosen.ambilFile().getAbsolutePath());
				parameters.put("foto_pegawai", fotodosen.ambilFile().getAbsolutePath());
			} else if (fotodosen != null && fotodosen.getLink() != null
					&& fotodosen.getLink().toLowerCase().contains("dropbox")) {
				parameters.put("foto", fotodosen.dropboxLinkRaw());
				parameters.put("foto_dosen", fotodosen.dropboxLinkRaw());
				parameters.put("foto_pegawai", fotodosen.dropboxLinkRaw());
			} else if (fotodosen != null && fotodosen.getGdrive() != null) {
				parameters.put("foto", fotodosen.exportGDriveUrl());
				parameters.put("foto_dosen", fotodosen.exportGDriveUrl());
				parameters.put("foto_pegawai", fotodosen.exportGDriveUrl());
			} else if (fotodosen != null) {
				parameters.put("foto", fotodosen.createLinkUri());
				parameters.put("foto_dosen", fotodosen.createLinkUri());
				parameters.put("foto_pegawai", fotodosen.createLinkUri());
			} else {
				File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
				parameters.put("foto", file.getAbsolutePath());
				parameters.put("foto_dosen", file.getAbsolutePath());
				parameters.put("foto_pegawai", file.getAbsolutePath());
			}

			LampiranLain lampiranLain = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
			if (lampiranLain != null && lampiranLain.ambilFile() != null) {
				parameters.put("ttd_dosen", lampiranLain.ambilFile().getAbsolutePath());
			}
			parameters.put("ttd_dosen_qrcode", dosen.ttdQr());

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/Dosen.java:3509");
		}
	}
}
