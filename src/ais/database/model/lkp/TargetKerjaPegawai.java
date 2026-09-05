package ais.database.model.lkp;

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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

/**
 * Entity header modul LKP (Laporan Kinerja Pegawai) yang mendefinisikan satu <b>target kerja</b>
 * — penugasan satu {@link KegiatanTugasJabatan} kepada satu {@link Pegawai} untuk satu periode
 * ({@link #getBulan()}/{@link #getTahun()}), lengkap dengan target kuantitas, kualitas, waktu, dan
 * biaya yang harus dipenuhi. Tabel {@code public.target_kerja_pegawai}; keunikan penugasan dijaga
 * lewat {@link #getKodeUnik()} (kombinasi pegawai + tahun + bulan + kegiatan).
 *
 * <p><b>Relasi dengan realisasi — header/detail, bukan 1:1.</b> Banyak baris {@link
 * ais.database.model.lkp.RealisasiKerjaPegawai} (detail/log kerja aktual) dapat menunjuk ke satu
 * target ini dalam periode yang sama; nilai {@link #getKuantitas()} dan {@link #getWaktu()} pada
 * entity ini adalah target yang dibandingkan terhadap <b>jumlah</b> kuantitas/waktu seluruh
 * realisasi terverifikasi milik target ini (dihitung di layar aksi, bukan di model). Sebaliknya,
 * {@link #getKualitas()} (target kualitas) dibandingkan terhadap {@link #getKualitasRealisasi()}
 * (satu angka yang dinilai langsung oleh asesor di level target ini) — bukan dijumlah dari baris
 * realisasi mana pun, karena {@code RealisasiKerjaPegawai} sengaja tidak memiliki field kualitas
 * tersendiri.</p>
 *
 * <p><b>Nilai default mengalir dari kegiatan.</b> Selama field lokal ({@link #kuantitas}, {@link
 * #kualitas}, {@link #waktu}, {@link #biaya}, {@link #kualitasRealisasi}) belum diisi eksplisit,
 * getter-nya jatuh ke nilai default yang didefinisikan pada {@link #getKegiatanTugasJabatan()}
 * (mis. {@link KegiatanTugasJabatan#getKuantitasDefault()}) — sehingga membuat target baru untuk
 * kegiatan yang sudah punya default cukup menautkan kegiatannya saja tanpa mengisi ulang
 * angka-angka target satu per satu.</p>
 *
 * @see KegiatanTugasJabatan
 * @see ais.database.model.lkp.RealisasiKerjaPegawai
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "target_kerja_pegawai")
public class TargetKerjaPegawai extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan/mengubah baris ini (field audit shadow,
	 * pasangan {@link #getOleh()}, diisi manual — bukan oleh interceptor otomatis).
	 *
	 * @return id pengguna terakhir, dapat {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna yang melakukan perubahan. Nilai {@code null} atau kosong/blank
	 * diabaikan secara diam-diam.
	 *
	 * @param olehId id pengguna; diabaikan jika {@code null} atau kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama/label pengguna yang melakukan perubahan (pasangan {@link #setOlehId(String)}).
	 * Nilai {@code null} atau kosong/blank diabaikan secara diam-diam.
	 *
	 * @param oleh nama pengguna; diabaikan jika {@code null} atau kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/label pengguna yang terakhir menyimpan/mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, dapat {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} melalui {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini diupdate.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan timestamp perubahan terakhir secara eksplisit.
	 *
	 * @param tanggal_dirubah timestamp perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan timestamp perubahan terakhir baris ini, diperbarui otomatis oleh {@link
	 * #onUpdate()}.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk keperluan tampilan/log/debug, berupa gabungan {@code id} dan
	 * {@link #getKegiatanTugasJabatan() kegiatan tugas jabatan} terkait.
	 *
	 * @return string {@code "<id>-<kegiatanTugasJabatan>"}.
	 */
	public String toString() {
		return id + "-" + kegiatanTugasJabatan;
	}

	private Integer bulan;
	private Integer tahun;

	private Pegawai pegawai;
	private KegiatanTugasJabatan kegiatanTugasJabatan;
	private Double kuantitas;
	private Double kualitas;
	private Double kualitasRealisasi;
	private Double waktu;
	private Double biaya;
	private String keterangan;
	private String kodeUnik;

	private Boolean verifikasi;
	private String catatan;

	/** Konstruktor default (dibutuhkan Hibernate/JPA); field diinisialisasi ke nilai default. */
	public TargetKerjaPegawai() {
	}

	/**
	 * Mengembalikan id primary key target ini. Dipetakan {@code insertable = false} karena nilai
	 * dibangkitkan basis data (identity).
	 *
	 * @return id target, atau {@code null} untuk instance yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan id target ini.
	 *
	 * @param id id target.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan/deskripsi bebas untuk target ini.
	 *
	 * @return keterangan target, dapat {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan/deskripsi bebas untuk target ini.
	 *
	 * @param keterangan keterangan target.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan {@link Pegawai} pemilik target kerja ini. Relasi lazy, di-refresh lewat {@link
	 * #check(Object)}.
	 *
	 * @return pegawai pemilik target, dapat {@code null} bila belum ditautkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menetapkan pegawai pemilik target kerja ini.
	 *
	 * @param pegawai pegawai pemilik baru.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan {@link KegiatanTugasJabatan} yang ditargetkan pada baris ini — sumber nilai
	 * default bagi {@link #getKuantitas()}, {@link #getKualitas()}, {@link #getWaktu()}, {@link
	 * #getBiaya()}, dan {@link #getKualitasRealisasi()} selama field lokalnya belum diisi. Relasi
	 * lazy, di-refresh lewat {@link #check(Object)}.
	 *
	 * @return kegiatan tugas jabatan yang ditargetkan, dapat {@code null} bila belum ditautkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kegiatan_tugas_jabatan", nullable = true)
	public KegiatanTugasJabatan getKegiatanTugasJabatan() {
		kegiatanTugasJabatan = check(kegiatanTugasJabatan);
		return kegiatanTugasJabatan;
	}

	/**
	 * Menetapkan kegiatan tugas jabatan yang ditargetkan pada baris ini.
	 *
	 * @param kegiatanTugasJabatan kegiatan tugas jabatan baru.
	 */
	public void setKegiatanTugasJabatan(KegiatanTugasJabatan kegiatanTugasJabatan) {
		this.kegiatanTugasJabatan = kegiatanTugasJabatan;
	}

	/**
	 * Mengembalikan target kuantitas yang harus dipenuhi pada periode ini. Bila belum diisi
	 * eksplisit, jatuh ke {@link KegiatanTugasJabatan#getKuantitasDefault()} milik {@link
	 * #getKegiatanTugasJabatan()} (atau {@code 0.0} bila kegiatan belum ditautkan).
	 *
	 * @return target kuantitas.
	 */
	public Double getKuantitas() {
		return kuantitas == null ? kegiatanTugasJabatan == null ? 0.0 : kegiatanTugasJabatan.getKuantitasDefault()
				: kuantitas;
	}

	/**
	 * Menetapkan target kuantitas periode ini secara eksplisit, menimpa default dari kegiatan.
	 *
	 * @param kuantitas target kuantitas baru.
	 */
	public void setKuantitas(Double kuantitas) {
		this.kuantitas = kuantitas;
	}

	/**
	 * Mengembalikan target kualitas (skala 0-100) yang harus dipenuhi pada periode ini — dibanding
	 * terhadap {@link #getKualitasRealisasi()} (angka realisasi kualitas yang dinilai asesor) untuk
	 * menghitung persentase capaian kualitas. Bila belum diisi eksplisit, jatuh ke {@link
	 * KegiatanTugasJabatan#getKualitasDefault()} milik {@link #getKegiatanTugasJabatan()} (atau
	 * {@code 0.0} bila kegiatan belum ditautkan).
	 *
	 * @return target kualitas.
	 */
	public Double getKualitas() {
		return kualitas == null ? kegiatanTugasJabatan == null ? 0.0 : kegiatanTugasJabatan.getKualitasDefault()
				: kualitas;
	}

	/**
	 * Menetapkan target kualitas periode ini secara eksplisit, menimpa default dari kegiatan.
	 *
	 * @param kualitas target kualitas baru.
	 */
	public void setKualitas(Double kualitas) {
		this.kualitas = kualitas;
	}

	/**
	 * Mengembalikan target waktu (dalam satuan {@link KegiatanTugasJabatan#getSatuanWaktu()}) yang
	 * harus dipenuhi pada periode ini. Bila belum diisi eksplisit, jatuh ke {@link
	 * KegiatanTugasJabatan#getWaktuDefault()} milik {@link #getKegiatanTugasJabatan()} (atau {@code
	 * 0.0} bila kegiatan belum ditautkan).
	 *
	 * @return target waktu.
	 */
	public Double getWaktu() {
		return waktu == null ? kegiatanTugasJabatan == null ? 0.0 : kegiatanTugasJabatan.getWaktuDefault() : waktu;
	}

	/**
	 * Menetapkan target waktu periode ini secara eksplisit, menimpa default dari kegiatan.
	 *
	 * @param waktu target waktu baru.
	 */
	public void setWaktu(Double waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan target biaya yang harus dipenuhi pada periode ini. Bila belum diisi eksplisit,
	 * jatuh ke {@link KegiatanTugasJabatan#getBiayaDefault()} milik {@link
	 * #getKegiatanTugasJabatan()} (atau {@code 0.0} bila kegiatan belum ditautkan).
	 *
	 * @return target biaya.
	 */
	public Double getBiaya() {
		return biaya == null ? kegiatanTugasJabatan == null ? 0.0 : kegiatanTugasJabatan.getBiayaDefault() : biaya;
	}

	/**
	 * Menetapkan target biaya periode ini secara eksplisit, menimpa default dari kegiatan.
	 *
	 * @param biaya target biaya baru.
	 */
	public void setBiaya(Double biaya) {
		this.biaya = biaya;
	}

	/**
	 * Mengembalikan nomor bulan periode target ini (0-based mengikuti {@link Calendar#MONTH},
	 * yaitu Januari = 0). Bila belum diisi, jatuh ke bulan berjalan saat ini.
	 *
	 * @return nomor bulan periode target; bulan berjalan bila belum diisi.
	 */
	public Integer getBulan() {
		return bulan == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) : bulan;
	}

	/**
	 * Menetapkan nomor bulan periode target ini.
	 *
	 * @param bulan nomor bulan baru (0-based).
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan tahun periode target ini. Bila belum diisi, jatuh ke tahun berjalan saat ini.
	 *
	 * @return tahun periode target; tahun berjalan bila belum diisi.
	 */
	public Integer getTahun() {
		return tahun == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) : tahun;
	}

	/**
	 * Menetapkan tahun periode target ini.
	 *
	 * @param tahun tahun baru.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Menghitung dan mengembalikan kode unik penugasan target ini, dibentuk dari kombinasi
	 * {@code idPegawai-tahun-bulan-idKegiatan}, dipetakan sebagai kolom {@code unique = true} untuk
	 * mencegah duplikasi penugasan kegiatan yang sama kepada pegawai yang sama pada periode yang
	 * sama di level basis data. Kode hanya dihitung ulang bila keempat komponennya (pegawai,
	 * kegiatan, tahun, bulan — memakai field lokal langsung, <b>bukan</b> getter dengan fallback
	 * default) sudah tersedia sekaligus; bila salah satu belum lengkap, method mengembalikan nilai
	 * {@link #kodeUnik} yang tersimpan sebelumnya (bisa {@code null}) tanpa mencoba menghitung
	 * ulang dengan nilai default dari {@link #getTahun()}/{@link #getBulan()}.
	 *
	 * @return kode unik penugasan {@code "idPegawai-tahun-bulan-idKegiatan"}, atau nilai lama/{@code
	 *         null} bila salah satu komponen belum lengkap.
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		if (pegawai != null && pegawai.getId() != null && kegiatanTugasJabatan != null
				&& kegiatanTugasJabatan.getId() != null && tahun != null && bulan != null) {
			kodeUnik = pegawai.getId() + "-" + tahun + "-" + bulan + "-" + kegiatanTugasJabatan.getId();
		}
		return kodeUnik;
	}

	/**
	 * Menetapkan kode unik penugasan target ini secara eksplisit.
	 *
	 * @param kodeUnik kode unik baru.
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan nilai realisasi kualitas (skala 0-100) yang dinilai langsung oleh asesor untuk
	 * target ini — dibandingkan terhadap {@link #getKualitas()} untuk menghitung persentase capaian
	 * kualitas ({@code (kualitasRealisasi * 100.0) / kualitas}, dihitung di layar aksi). Bila belum
	 * diisi eksplisit (asesor belum menilai), jatuh ke {@link
	 * KegiatanTugasJabatan#getKuantitasRealisasiDefault()} milik {@link
	 * #getKegiatanTugasJabatan()} sebagai asumsi awal — <b>perhatikan penamaan field default yang
	 * dipakai ulang</b>: {@code kuantitasRealisasiDefault} pada {@link KegiatanTugasJabatan} secara
	 * literal berarti "default realisasi kuantitas", namun di sinilah satu-satunya nilai default
	 * yang tersedia juga dipakai sebagai asumsi awal capaian <i>kualitas</i> (tidak ada field
	 * {@code kualitasRealisasiDefault} terpisah). Ini bukan bug kalkulasi — begitu asesor mengisi
	 * {@link #setKualitasRealisasi(Double)}, nilai eksplisit tersebutlah yang dipakai — namun
	 * penamaan field default yang ambigu ini patut diperhatikan bila suatu saat kuantitas dan
	 * kualitas memerlukan asumsi default yang berbeda.
	 *
	 * @return realisasi kualitas; default dari kegiatan (biasanya {@code 90.0}) bila asesor belum
	 *         menilai, atau {@code 0.0} bila kegiatan juga belum ditautkan.
	 */
	public Double getKualitasRealisasi() {
		return kualitasRealisasi == null
				? kegiatanTugasJabatan == null ? 0.0 : kegiatanTugasJabatan.getKuantitasRealisasiDefault()
				: kualitasRealisasi;
	}

	/**
	 * Menetapkan nilai realisasi kualitas target ini, biasanya diisi oleh asesor lewat layar
	 * penilaian ({@code ais.action.master.lkp.RealisasiKerjaPegawaiAction}).
	 *
	 * @param kualitasRealisasi realisasi kualitas baru.
	 */
	public void setKualitasRealisasi(Double kualitasRealisasi) {
		this.kualitasRealisasi = kualitasRealisasi;
	}

	/**
	 * Mengembalikan status verifikasi target ini oleh asesor. Berbeda dari status verifikasi pada
	 * {@link ais.database.model.lkp.RealisasiKerjaPegawai#getVerifikasi()} (yang bisa mewarisi
	 * status ini), field ini murni lokal tanpa turunan dari entity lain.
	 *
	 * @return {@code true} bila target sudah diverifikasi; {@code false} bila belum diisi/belum
	 *         diverifikasi.
	 */
	public Boolean getVerifikasi() {
		return verifikasi == null ? false : verifikasi;
	}

	/**
	 * Menetapkan status verifikasi target ini. Mengubah nilai ini memengaruhi status verifikasi
	 * yang dilaporkan oleh seluruh baris {@link ais.database.model.lkp.RealisasiKerjaPegawai} yang
	 * menunjuk ke target ini (lihat catatan pewarisan satu-arah pada {@link
	 * ais.database.model.lkp.RealisasiKerjaPegawai#getVerifikasi()}).
	 *
	 * @param verifikasi status verifikasi baru.
	 */
	public void setVerifikasi(Boolean verifikasi) {
		this.verifikasi = verifikasi;
	}

	/**
	 * Mengembalikan catatan bebas (mis. catatan asesor) untuk target ini (kolom teks).
	 *
	 * @return catatan target, dapat {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getCatatan() {
		return catatan;
	}

	/**
	 * Menetapkan catatan bebas untuk target ini.
	 *
	 * @param catatan catatan baru.
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

}
