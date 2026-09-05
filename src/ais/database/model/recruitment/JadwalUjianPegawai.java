package ais.database.model.recruitment;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;

/**
 * Entity JPA/Hibernate untuk tabel {@code public.jadwal_ujian_pegawai}: jadwal pelaksanaan
 * konkret (waktu mulai/sampai pada tanggal tertentu) untuk satu {@link UjianPegawai} di modul
 * rekrutmen calon pegawai. Sementara {@link UjianPegawai} adalah definisi ujian secara umum
 * (nama, lokasi, sampai 10 tanggal pelaksanaan), kelas ini adalah baris jadwal per-slot waktu yang
 * terikat wajib ({@code nullable = false}) ke satu {@link #getUjianPegawai()}.
 *
 * <p>Kelas ini extends {@link VOPembelajaran} — superclass yang sama dipakai entity "sesi
 * pembelajaran"/e-learning di modul akademik AIS — sehingga jadwal ujian pegawai diperlakukan
 * sebagai satu jenis "sesi pembelajaran" generik dari sudut pandang infrastruktur pembelajaran
 * (course/materi, urutan otomatis, dsb.), meski secara bisnis ini murni jadwal ujian seleksi
 * rekrutmen, bukan perkuliahan. Field abstrak superclass yang diimplementasikan di sini ({@link
 * #getCourse()}, {@link #getUrutkanotomatis()}, {@link #ambilJumlahDetailperkuliahanLangsung()})
 * diisi dengan nilai minimal/placeholder (lihat Javadoc masing-masing) karena modul rekrutmen
 * tidak benar-benar memakai mekanisme course/materi — reuse superclass ini murni untuk
 * memanfaatkan infrastruktur penjadwalan yang sudah ada, bukan karena jadwal ujian pegawai punya
 * konten pembelajaran.</p>
 *
 * <p><b>Relasi:</b></p>
 * <ul>
 * <li>{@link #getUjianPegawai()} — {@code @ManyToOne} wajib ke {@link UjianPegawai}, ujian yang
 * dijadwalkan slot waktu ini.</li>
 * <li>{@link #getGelombangPendaftaranPegawai()} — getter turunan (bukan kolom independen secara
 * efektif) yang selalu mengambil ulang gelombang dari {@link #ujianPegawai}, lihat catatan pada
 * Javadoc method tersebut.</li>
 * <li>{@link #getDikunci()} — {@code @ManyToOne} lazy ke {@link Tbmuser}, mencatat pengguna yang
 * mengunci baris jadwal ini (mis. agar tidak diedit user lain secara bersamaan), dengan resolusi
 * proxy lewat {@link ais.database.model.GeneralValueObject#check(Object)}.</li>
 * </ul>
 *
 * <p>Diaudit oleh Hibernate Envers ({@code @Audited}); setiap INSERT/UPDATE/DELETE tercatat ke
 * tabel revisi historis terpisah.</p>
 *
 * @see UjianPegawai
 * @see GelombangPendaftaranPegawai
 * @see VOPembelajaran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jadwal_ujian_pegawai")

public class JadwalUjianPegawai extends VOPembelajaran {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} lintas deployment.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Primary key baris ini pada tabel {@code jadwal_ujian_pegawai}, dihasilkan otomatis oleh
	 * database ({@code IDENTITY}). Lihat {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama tampilan pengguna yang terakhir membuat/mengubah baris ini. Lihat {@link #getOleh()}/
	 * {@link #setOleh(String)}.
	 */
	private String oleh;
	/**
	 * ID pengguna yang terakhir membuat/mengubah baris ini, pasangan dari {@link #oleh}. Lihat
	 * {@link #getOlehId()}/{@link #setOlehId(String)}.
	 */
	private String olehId;

	/**
	 * Mengambil ID pengguna audit terakhir.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diset.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengeset ID pengguna audit. Menolak (no-op) nilai {@code null}/kosong-whitespace — pola
	 * audit-shadow-field yang berulang di seluruh entity AIS agar jejak "olehId" tidak pernah
	 * tertimpa kosong.
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengeset nama pengguna audit. Guard yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna audit terakhir.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diset.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pembaruan timestamp audit ke {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum UPDATE
	 * dijalankan. Tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengeset timestamp perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah timestamp baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp terakhir baris ini diubah.
	 *
	 * @return timestamp perubahan terakhir; tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string entity ini untuk keperluan tampilan/log, berupa gabungan ID dan nama.
	 *
	 * @return string berformat {@code "<id>-<nama>"}.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Pengguna yang mengunci baris jadwal ini (mis. agar tidak diedit bersamaan pengguna lain).
	 * Lihat {@link #getDikunci()}.
	 */
	private Tbmuser dikunci;

	/**
	 * Mengambil pengguna yang mengunci baris jadwal ini, dengan resolusi proxy lazy lewat {@link
	 * ais.database.model.GeneralValueObject#check(Object)}.
	 *
	 * @return {@link Tbmuser} yang mengunci, atau {@code null} bila jadwal tidak sedang dikunci.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Mengeset {@link #dikunci}.
	 *
	 * @param dikunci nilai baru untuk {@link #dikunci}.
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Nama jadwal ujian, ditampilkan ke admin/peserta. Lihat {@link #getNama()}.
	 */
	private String nama;
	/**
	 * Catatan/keterangan bebas untuk jadwal ini. Lihat {@link #getKeterangan()}.
	 */
	private String keterangan;

	/**
	 * Ujian induk yang dijadwalkan pada slot waktu ini. Lihat {@link #getUjianPegawai()}.
	 */
	private UjianPegawai ujianPegawai;
	/**
	 * Gelombang pendaftaran; field ini disinkronkan ulang dari {@link #ujianPegawai} setiap kali
	 * {@link #getGelombangPendaftaranPegawai()} dipanggil, lihat catatan pada Javadoc getter
	 * tersebut.
	 */
	private GelombangPendaftaranPegawai gelombangPendaftaranPegawai;

	/**
	 * Waktu mulai pelaksanaan slot jadwal ini. Lihat {@link #getWaktuMulai()}.
	 */
	private Date waktuMulai;
	/**
	 * Waktu selesai pelaksanaan slot jadwal ini. Lihat {@link #getWaktuSampai()}.
	 */
	private Date waktuSampai;

	/**
	 * Konstruktor kosong yang disyaratkan Hibernate/JPA untuk instansiasi lewat refleksi. Field
	 * lain (nama, ujian induk, waktu mulai/sampai) harus diisi terpisah lewat setter.
	 */
	public JadwalUjianPegawai() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID jadwal, atau {@code null} untuk instance transient.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengeset {@link #id}.
	 *
	 * @param id nilai baru untuk {@link #id}.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama jadwal ujian, dengan whitespace di kedua ujung dipangkas ({@link
	 * String#trim()}).
	 *
	 * @return nama jadwal yang sudah di-trim, atau {@code null} bila field belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengeset {@link #nama}.
	 *
	 * @param nama nilai baru untuk {@link #nama}.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil catatan/keterangan bebas jadwal ini. Kolom bersifat opsional ({@code nullable =
	 * true}) tanpa default apa pun (berbeda dari {@link UjianPegawai#getKeterangan()} yang punya
	 * placeholder informatif).
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengeset {@link #keterangan}.
	 *
	 * @param keterangan nilai baru untuk {@link #keterangan}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil ujian induk yang dijadwalkan pada slot waktu ini. Relasi {@code @ManyToOne} wajib
	 * ({@code nullable = false}) dengan {@code FetchMode.SELECT} (query terpisah saat diakses).
	 *
	 * @return {@link UjianPegawai} induk; secara skema tidak boleh {@code null} pada baris yang
	 * sudah dipersist, meski tidak ada validasi null eksplisit di level Java sebelum persist.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "ujian_pegawai", nullable = false)
	public UjianPegawai getUjianPegawai() {
		return ujianPegawai;
	}

	/**
	 * Mengeset {@link #ujianPegawai}.
	 *
	 * @param ujianPegawai nilai baru untuk {@link #ujianPegawai}.
	 */
	public void setUjianPegawai(UjianPegawai ujianPegawai) {
		this.ujianPegawai = ujianPegawai;
	}

	/**
	 * Mengambil (dan bila perlu menormalkan) waktu mulai pelaksanaan slot jadwal ini. <b>Efek
	 * samping:</b> bila field {@link #waktuMulai} masih {@code null}, ditulis ke waktu saat ini
	 * ({@link ais.ui.util.WaktuUtil#getDate()}) sebelum dikembalikan — bukan getter murni; nilai
	 * "waktu sekarang" yang dihasilkan bergantung kapan getter ini pertama kali dipanggil, bukan
	 * kapan baris dibuat.
	 *
	 * @return waktu mulai; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	@Column(nullable = false)
	public Date getWaktuMulai() {
		if (waktuMulai == null) {
			waktuMulai = ais.ui.util.WaktuUtil.getDate();
		}
		return waktuMulai;
	}

	/**
	 * Mengeset {@link #waktuMulai}.
	 *
	 * @param waktuMulai nilai baru untuk {@link #waktuMulai}.
	 */
	public void setWaktuMulai(Date waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	/**
	 * Mengambil (dan bila perlu menormalkan) waktu selesai pelaksanaan slot jadwal ini. Pola efek
	 * samping sama seperti {@link #getWaktuMulai()}: field ditulis ke waktu saat ini bila masih
	 * {@code null}. Perlu diperhatikan bahwa tanpa pengisian eksplisit, {@link #getWaktuMulai()}
	 * dan getter ini bisa menghasilkan dua timestamp "sekarang" yang sedikit berbeda (dipanggil
	 * pada saat berbeda), bukan rentang waktu yang berarti — kode pemanggil yang membutuhkan
	 * rentang waktu valid wajib mengeset kedua field ini secara eksplisit, tidak boleh
	 * mengandalkan default getter.
	 *
	 * @return waktu selesai; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	@Column(nullable = false)
	public Date getWaktuSampai() {
		if (waktuSampai == null) {
			waktuSampai = ais.ui.util.WaktuUtil.getDate();
		}
		return waktuSampai;
	}

	/**
	 * Mengeset {@link #waktuSampai}.
	 *
	 * @param waktuSampai nilai baru untuk {@link #waktuSampai}.
	 */
	public void setWaktuSampai(Date waktuSampai) {
		this.waktuSampai = waktuSampai;
	}

	/**
	 * Mengambil gelombang pendaftaran yang menaungi jadwal ini secara tidak langsung. <b>Bukan
	 * getter murni dan bukan sumber kebenaran independen:</b> setiap pemanggilan menimpa field
	 * {@link #gelombangPendaftaranPegawai} dengan hasil {@link UjianPegawai#getGelombangPendaftaranPegawai()}
	 * dari {@link #ujianPegawai} (bila {@link #ujianPegawai} tidak {@code null}) — jadi nilai yang
	 * pernah diset lewat {@link #setGelombangPendaftaranPegawai(GelombangPendaftaranPegawai)}
	 * hanya bertahan sampai getter ini dipanggil berikutnya, dan sepenuhnya ditimpa mengikuti
	 * gelombang ujian induknya. Kolom FK {@code gelombang_pendaftaran_pegawai} opsional ({@code
	 * nullable = true}) tetap ada di skema tabel, tetapi secara efektif nilainya selalu mengikuti
	 * relasi lewat {@link #ujianPegawai}, bukan nilai independen yang tersimpan sendiri di baris
	 * ini — kode yang query langsung ke kolom database (bukan lewat getter Java) perlu menyadari
	 * potensi ketidaksesuaian ini bila {@link #ujianPegawai} berubah tanpa getter ini pernah
	 * dipanggil ulang untuk menyinkronkan.
	 *
	 * @return {@link GelombangPendaftaranPegawai} dari ujian induk bila {@link #ujianPegawai}
	 * terisi, atau nilai field {@link #gelombangPendaftaranPegawai} apa adanya (kemungkinan hasil
	 * pemanggilan sebelumnya, atau {@code null}) bila {@link #ujianPegawai} {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "gelombang_pendaftaran_pegawai", nullable = true)
	public GelombangPendaftaranPegawai getGelombangPendaftaranPegawai() {
		if (ujianPegawai != null) {
			gelombangPendaftaranPegawai = ujianPegawai.getGelombangPendaftaranPegawai();
		}
		return gelombangPendaftaranPegawai;
	}

	/**
	 * Mengeset {@link #gelombangPendaftaranPegawai}.
	 *
	 * @param gelombangPendaftaranPegawai nilai baru untuk {@link #gelombangPendaftaranPegawai}.
	 */
	public void setGelombangPendaftaranPegawai(GelombangPendaftaranPegawai gelombangPendaftaranPegawai) {
		this.gelombangPendaftaranPegawai = gelombangPendaftaranPegawai;
	}

	/**
	 * Implementasi kontrak {@link VOPembelajaran#ambilJumlahDetailperkuliahanLangsung()} untuk
	 * jadwal ujian pegawai: selalu mengembalikan {@code 1}, karena satu baris jadwal ujian di
	 * modul ini dianggap satu "detail perkuliahan langsung" tunggal — modul rekrutmen tidak
	 * memecah satu slot jadwal menjadi beberapa sesi detail seperti pada entity pembelajaran
	 * akademik yang sesungguhnya.
	 *
	 * @return selalu {@code 1}.
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 1;
	}

	/**
	 * Data course/materi mentah dalam bentuk string JSON, bagian dari kontrak {@link
	 * VOPembelajaran}. Lihat {@link #getCourse()} — tidak benar-benar dipakai untuk konten
	 * pembelajaran di modul rekrutmen, hanya diisi untuk memenuhi kontrak superclass.
	 */
	private String course;
	/**
	 * Penanda pengurutan otomatis, bagian dari kontrak {@link VOPembelajaran}. Lihat {@link
	 * #getUrutkanotomatis()} — default {@code true}.
	 */
	private Boolean urutkanotomatis;

	/**
	 * Implementasi kontrak {@link VOPembelajaran#getCourse()}: mengembalikan data course/materi
	 * dalam bentuk string JSON (kolom {@code text}). <b>Efek samping semu:</b> tidak menulis field,
	 * hanya menghitung ulang placeholder setiap pemanggilan bila field masih kosong.
	 *
	 * @return nilai field {@link #course} apa adanya bila terisi non-kosong; jika {@code null}
	 * atau kosong (setelah trim), mengembalikan string JSON objek kosong ({@code "{}"} — hasil
	 * {@code new JSONObject().toString()}) sebagai placeholder aman untuk kode yang mengasumsikan
	 * struktur JSON valid.
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	@Override
	/**
	 * Mengeset {@link #course}.
	 *
	 * @param course nilai baru untuk {@link #course}.
	 */
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * Implementasi kontrak {@link VOPembelajaran#getUrutkanotomatis()}.
	 *
	 * @return {@code true} sebagai default bila field {@link #urutkanotomatis} belum pernah
	 * diset, atau nilai eksplisit yang tersimpan.
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	@Override
	/**
	 * Mengeset {@link #urutkanotomatis}.
	 *
	 * @param urutkanotomatis nilai baru untuk {@link #urutkanotomatis}.
	 */
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}
}
