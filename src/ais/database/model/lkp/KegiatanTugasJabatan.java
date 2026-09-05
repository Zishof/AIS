package ais.database.model.lkp;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmrole;
import ais.database.model.rab.SatuanKerja;

/**
 * Entity utama modul LKP (Laporan Kinerja Pegawai) yang mendefinisikan satu <b>kegiatan/tugas
 * jabatan</b> — unit pekerjaan baku yang dapat ditugaskan berulang (bulanan atau tahunan) kepada
 * pegawai pemegang jabatan/{@link Tbmrole} tertentu, lengkap dengan nilai default untuk kuantitas,
 * kualitas, waktu, dan biaya. Tabel {@code public.kegiatan_tugas_jabatan} ini adalah master data;
 * penugasan riil per pegawai per periode dicatat di {@link
 * ais.database.model.lkp.TargetKerjaPegawai} (target) dan {@link
 * ais.database.model.lkp.RealisasiKerjaPegawai} (realisasi berkala terhadap target tersebut).
 *
 * <p><b>Bukan sistem KPI berbasis indikator.</b> Modul {@code ais.database.model.kpi} (paket
 * {@code kpi}, penilaian kinerja berbasis {@code ItemKpi}/{@code FormatKpi}) dan modul LKP ini
 * (paket {@code lkp}, berbasis kegiatan/tugas dengan target kuantitas-kualitas-waktu-biaya) adalah
 * dua sistem penilaian kinerja pegawai yang <b>berdiri sendiri dan tidak saling terhubung</b> —
 * tidak ada kelas di paket {@code lkp} yang mereferensikan {@code ItemKpi}/{@code FormatKpi} atau
 * sebaliknya. Kegiatan di sini justru menautkan target capaiannya ke {@link
 * ais.database.model.rab.Indikator} milik modul RAB (lihat {@link
 * KegiatanTugasJabatanPunyaIndikator}), bukan ke indikator KPI. Perhatikan potensi duplikasi upaya
 * penilaian kinerja bila kedua modul dipakai bersamaan untuk populasi pegawai yang sama.</p>
 *
 * <p><b>Hierarki dan urutan.</b> Kegiatan dapat membentuk pohon melalui {@link #getInduk()}
 * (kegiatan induk) dan {@link #getDeep()} (kedalaman level), serta diurutkan tampil lewat {@link
 * #getNoUrut()}. Setiap kegiatan boleh memiliki nol atau lebih {@link
 * KelompokParameterTambahanKegiatan} (form isian tambahan dinamis) melalui tabel penghubung
 * many-to-many {@code kegiatan_has_parameter}.</p>
 *
 * <p><b>Angka kredit.</b> {@link #getAngkaKredit()} menyimpan poin angka kredit (skema penilaian
 * kepegawaian/jabatan fungsional ala PNS/dosen) yang melekat pada kegiatan ini — dipakai untuk
 * akumulasi capaian angka kredit pegawai, terlepas dari kuantitas/kualitas/waktu/biaya yang
 * menjadi dasar perhitungan capaian LKP itu sendiri.</p>
 *
 * @see TargetKerjaPegawai
 * @see RealisasiKerjaPegawai
 * @see KegiatanTugasJabatanPunyaIndikator
 * @see KegiatanTugasJabatanPunyaSasaran
 * @see KegiatanTugasJabatanPunyaPredecessor
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kegiatan_tugas_jabatan")
public class KegiatanTugasJabatan extends GeneralValueObject {

	/** Nilai {@link #getPeriode()} untuk kegiatan yang ditugaskan/direalisasikan setiap bulan. */
	public static final String BULANAN = "Bulanan";
	/** Nilai {@link #getPeriode()} untuk kegiatan yang ditugaskan/direalisasikan setiap tahun. */
	public static final String TAHUNAN = "Tahunan";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan pengenal (id) pengguna yang terakhir menyimpan/mengubah baris ini. Field audit
	 * shadow yang dipertukarkan bersama {@link #getOleh()}; diisi manual oleh lapisan pemanggil
	 * (bukan interceptor), berbeda dari {@link #getTanggal_dirubah()} yang otomatis lewat
	 * {@code @PreUpdate}.
	 *
	 * @return id pengguna terakhir, dapat {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna yang melakukan perubahan. Nilai {@code null} atau string kosong/blank
	 * diabaikan secara diam-diam (nilai lama dipertahankan) — pemanggil yang tidak menyertakan
	 * identitas pengguna tidak akan menimpa jejak audit yang sudah ada.
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
	 * @return nama pengguna terakhir, dapat {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} melalui {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini diupdate.
	 * Dipanggil otomatis oleh provider persistence, bukan untuk dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan timestamp perubahan terakhir secara eksplisit. Umumnya tidak perlu dipanggil manual
	 * karena {@link #onUpdate()} sudah memperbarui nilai ini otomatis pada setiap update.
	 *
	 * @param tanggal_dirubah timestamp perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan timestamp perubahan terakhir baris ini, dipetakan sebagai kolom timestamp.
	 * Diinisialisasi ke waktu saat ini pada konstruksi objek dan diperbarui otomatis oleh {@link
	 * #onUpdate()} setiap kali entity ini diupdate.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk keperluan tampilan/log/debug, berupa gabungan {@code id} dan
	 * {@link #getNama() nama} kegiatan (mis. dropdown, log, atau pesan error).
	 *
	 * @return string {@code "<id>-<nama>"}.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private SatuanKerja satuanKerja;
	private Tbmrole userRole;
	private String nama;
	private Double angkaKredit;
	private SatuanKegiatanTugasJabatan satuanKuantitas;
	private Double kuantitasDefault;
	private Double kuantitasRealisasiDefault;
	private Double kualitasDefault;
	private Double waktuDefault;
	private Double biayaDefault;
	private String satuanWaktu;
	private String keterangan;
	private Boolean aktif;
	private Boolean wajib;
	private Double noUrut;

	private KegiatanTugasJabatan induk;
	private Integer deep;
	private Long jmlDipakai;
	private String periode;

	private Set<KelompokParameterTambahanKegiatan> kelompokParameterTambahanKegiatans = new TreeSet<KelompokParameterTambahanKegiatan>();

	/**
	 * Mengembalikan himpunan {@link KelompokParameterTambahanKegiatan} (kelompok form isian
	 * tambahan) yang terpasang pada kegiatan ini, terurut menaik menurut {@code nomorUrut} lewat
	 * {@code @OrderBy}. Relasi many-to-many via tabel penghubung {@code kegiatan_has_parameter};
	 * ditandai {@code @NotAudited} sehingga perubahan keanggotaan set ini tidak masuk riwayat
	 * Envers kegiatan induk.
	 *
	 * @return set kelompok parameter tambahan terpasang; tidak pernah {@code null} (default set
	 *         kosong).
	 */
	@ManyToMany(targetEntity = KelompokParameterTambahanKegiatan.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST })
	@OrderBy(value = "nomorUrut asc")
	@JoinTable(name = "kegiatan_has_parameter", joinColumns = @JoinColumn(name = "kegiatan_tugas_jabatan"), inverseJoinColumns = @JoinColumn(name = "kelompok_parameter_tambahan_kegiatan"))
	@NotAudited
	public Set<KelompokParameterTambahanKegiatan> getKelompokParameterTambahanKegiatans() {
		return kelompokParameterTambahanKegiatans;
	}

	/**
	 * Mengganti seluruh himpunan kelompok parameter tambahan yang terpasang pada kegiatan ini.
	 *
	 * @param kelompokParameterTambahanKegiatans set kelompok parameter tambahan baru.
	 */
	public void setKelompokParameterTambahanKegiatans(
			Set<KelompokParameterTambahanKegiatan> kelompokParameterTambahanKegiatans) {
		this.kelompokParameterTambahanKegiatans = kelompokParameterTambahanKegiatans;
	}

	/** Konstruktor default (dibutuhkan Hibernate/JPA); field diinisialisasi ke nilai default. */
	public KegiatanTugasJabatan() {
	}

	/**
	 * Mengembalikan id primary key kegiatan ini. Dipetakan {@code insertable = false} karena nilai
	 * dibangkitkan basis data (identity) dan tidak pernah dikirim ulang pada statement insert.
	 *
	 * @return id kegiatan, atau {@code null} untuk instance yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan id kegiatan. Umumnya hanya dipanggil oleh Hibernate; kode aplikasi seharusnya
	 * tidak menetapkan id secara manual karena kolomnya {@code insertable = false}.
	 *
	 * @param id id kegiatan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kegiatan/tugas jabatan, di-trim dari whitespace di kedua ujung. Kolom
	 * wajib diisi ({@code nullable = false}) dengan panjang maksimum 255 karakter.
	 *
	 * @return nama kegiatan yang sudah di-trim, atau {@code null} bila field belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama kegiatan/tugas jabatan. Tidak melakukan trim maupun validasi pada saat set;
	 * pemangkasan whitespace baru terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama kegiatan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/deskripsi bebas untuk kegiatan ini.
	 *
	 * @return keterangan kegiatan, dapat {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan/deskripsi bebas untuk kegiatan ini.
	 *
	 * @param keterangan keterangan kegiatan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan {@link SatuanKerja} (unit kerja) pemilik kegiatan ini — kolom wajib diisi yang
	 * menjadi batas kepemilikan/scoping kegiatan antar unit kerja. Relasi lazy; nilai di-refresh
	 * lewat {@link #check(Object)} warisan {@link GeneralValueObject} sebelum dikembalikan agar
	 * aman diakses lintas sesi Hibernate.
	 *
	 * @return satuan kerja pemilik kegiatan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = false)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pemilik kegiatan ini.
	 *
	 * @param satuanKerja satuan kerja pemilik.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan poin angka kredit yang melekat pada kegiatan ini (skema penilaian jabatan
	 * fungsional/kepegawaian, terpisah dari perhitungan capaian kuantitas/kualitas/waktu/biaya LKP).
	 *
	 * @return angka kredit; {@code 0.0} bila belum diisi.
	 */
	public Double getAngkaKredit() {
		return angkaKredit == null ? 0.0 : angkaKredit;
	}

	/**
	 * Menetapkan poin angka kredit kegiatan ini.
	 *
	 * @param angkaKredit poin angka kredit.
	 */
	public void setAngkaKredit(Double angkaKredit) {
		this.angkaKredit = angkaKredit;
	}

	/**
	 * Mengembalikan nilai kuantitas default yang diwariskan ke {@link TargetKerjaPegawai} baru bila
	 * target belum menetapkan kuantitasnya sendiri (lihat {@link
	 * TargetKerjaPegawai#getKuantitas()}).
	 *
	 * @return kuantitas default; {@code 0.0} bila belum diisi.
	 */
	public Double getKuantitasDefault() {
		return kuantitasDefault == null ? 0.0 : kuantitasDefault;
	}

	/**
	 * Menetapkan nilai kuantitas default kegiatan.
	 *
	 * @param kuantitasDefault kuantitas default baru.
	 */
	public void setKuantitasDefault(Double kuantitasDefault) {
		this.kuantitasDefault = kuantitasDefault;
	}

	/**
	 * Mengembalikan nilai kualitas default (skala 0-100, default {@code 100.0} bila belum diisi)
	 * yang diwariskan ke {@link TargetKerjaPegawai} baru bila target belum menetapkan kualitasnya
	 * sendiri (lihat {@link TargetKerjaPegawai#getKualitas()}).
	 *
	 * @return kualitas default.
	 */
	public Double getKualitasDefault() {
		return kualitasDefault == null ? 100.0 : kualitasDefault;
	}

	/**
	 * Menetapkan nilai kualitas default kegiatan.
	 *
	 * @param kualitasDefault kualitas default baru.
	 */
	public void setKualitasDefault(Double kualitasDefault) {
		this.kualitasDefault = kualitasDefault;
	}

	/**
	 * Mengembalikan {@link SatuanKegiatanTugasJabatan} (satuan ukur, mis. dokumen/laporan/kegiatan)
	 * yang dipakai untuk menyatakan kuantitas kegiatan ini. Relasi lazy, opsional.
	 *
	 * @return satuan kuantitas, dapat {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kuantitas", nullable = true)
	public SatuanKegiatanTugasJabatan getSatuanKuantitas() {
		satuanKuantitas = check(satuanKuantitas);
		return satuanKuantitas;
	}

	/**
	 * Menetapkan satuan kuantitas kegiatan ini.
	 *
	 * @param satuanKuantitas satuan kuantitas baru.
	 */
	public void setSatuanKuantitas(SatuanKegiatanTugasJabatan satuanKuantitas) {
		this.satuanKuantitas = satuanKuantitas;
	}

	/**
	 * Mengembalikan nilai waktu default (dalam satuan {@link #getSatuanWaktu()}) yang diwariskan ke
	 * {@link TargetKerjaPegawai} baru bila target belum menetapkan waktunya sendiri.
	 *
	 * @return waktu default; {@code 0.0} bila belum diisi.
	 */
	public Double getWaktuDefault() {
		return waktuDefault == null ? 0.0 : waktuDefault;
	}

	/**
	 * Menetapkan nilai waktu default kegiatan.
	 *
	 * @param waktuDefault waktu default baru.
	 */
	public void setWaktuDefault(Double waktuDefault) {
		this.waktuDefault = waktuDefault;
	}

	/**
	 * Mengembalikan label satuan waktu bebas-teks (mis. {@code "Menit"}, {@code "Jam"}, {@code
	 * "Hari"}, {@code "Minggu"}, {@code "Bulan"}) yang dipakai kegiatan ini; dibandingkan
	 * case-insensitive oleh {@link RealisasiKerjaPegawai#getTanggalWaktuSampai()} untuk menghitung
	 * tanggal selesai realisasi. Default {@code "Jam"} bila belum diisi/kosong.
	 *
	 * @return label satuan waktu, di-trim; {@code "Jam"} bila kosong/belum diisi.
	 */
	public String getSatuanWaktu() {
		return satuanWaktu == null || satuanWaktu.isEmpty() ? "Jam" : satuanWaktu.trim();
	}

	/**
	 * Menetapkan label satuan waktu kegiatan.
	 *
	 * @param satuanWaktu label satuan waktu baru.
	 */
	public void setSatuanWaktu(String satuanWaktu) {
		this.satuanWaktu = satuanWaktu;
	}

	/**
	 * Mengembalikan nilai biaya default yang diwariskan ke {@link TargetKerjaPegawai} baru bila
	 * target belum menetapkan biayanya sendiri.
	 *
	 * @return biaya default; {@code 0.0} bila belum diisi.
	 */
	public Double getBiayaDefault() {
		return biayaDefault == null ? 0.0 : biayaDefault;
	}

	/**
	 * Menetapkan nilai biaya default kegiatan.
	 *
	 * @param biayaDefault biaya default baru.
	 */
	public void setBiayaDefault(Double biayaDefault) {
		this.biayaDefault = biayaDefault;
	}

	/**
	 * Mengembalikan status aktif kegiatan ini; kegiatan tidak aktif umumnya disembunyikan dari
	 * picker penugasan baru namun riwayat target/realisasi lama tetap utuh.
	 *
	 * @return {@code true} bila aktif atau belum diisi (default aktif); {@code false} bila
	 *         dinonaktifkan eksplisit.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif kegiatan ini.
	 *
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan persentase realisasi kuantitas yang diasumsikan sebagai default (skala 0-100,
	 * default {@code 90.0} bila belum diisi). Field ini <b>dipakai ulang</b> di dua konteks: sebagai
	 * label "realisasi kuantitas default" pada layar {@code KegiatanTugasJabatanAction}, dan sebagai
	 * nilai fallback capaian kualitas oleh {@link TargetKerjaPegawai#getKualitasRealisasi()} ketika
	 * asesor belum mengisi realisasi kualitas manual — bukan bug (tidak ada kolom terpisah untuk
	 * default realisasi kualitas), namun perhatikan nama field yang menyiratkan "kuantitas" padahal
	 * juga dipakai sebagai asumsi awal capaian "kualitas".
	 *
	 * @return persentase realisasi default; {@code 90.0} bila belum diisi.
	 */
	public Double getKuantitasRealisasiDefault() {
		return kuantitasRealisasiDefault == null ? 90.0 : kuantitasRealisasiDefault;
	}

	/**
	 * Menetapkan persentase realisasi default kegiatan ini.
	 *
	 * @param kuantitasRealisasiDefault persentase realisasi default baru.
	 */
	public void setKuantitasRealisasiDefault(Double kuantitasRealisasiDefault) {
		this.kuantitasRealisasiDefault = kuantitasRealisasiDefault;
	}

	/**
	 * Mengembalikan status wajib kegiatan ini — menandai apakah kegiatan harus selalu ditugaskan
	 * (mis. tidak boleh dilewati saat menyusun target kerja pegawai untuk periode berjalan).
	 *
	 * @return {@code true} bila wajib atau belum diisi (default wajib); {@code false} bila opsional.
	 */
	public Boolean getWajib() {
		return wajib == null ? true : wajib;
	}

	/**
	 * Menetapkan status wajib kegiatan ini.
	 *
	 * @param wajib status wajib baru.
	 */
	public void setWajib(Boolean wajib) {
		this.wajib = wajib;
	}

	/**
	 * Mengembalikan nomor urut tampil kegiatan ini pada daftar/hierarki.
	 *
	 * @return nomor urut; {@code 0.0} bila belum diisi.
	 */
	public Double getNoUrut() {
		return noUrut == null ? 0.0 : noUrut;
	}

	/**
	 * Menetapkan nomor urut tampil kegiatan ini.
	 *
	 * @param noUrut nomor urut baru.
	 */
	public void setNoUrut(Double noUrut) {
		this.noUrut = noUrut;
	}

	/**
	 * Mengembalikan kegiatan induk (parent) dalam hierarki pohon kegiatan tugas jabatan yang
	 * membentuk struktur sub-kegiatan. Relasi lazy self-referencing, opsional (kegiatan tingkat
	 * teratas tidak memiliki induk).
	 *
	 * @return kegiatan induk, atau {@code null} bila kegiatan ini berada di tingkat teratas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "induk", nullable = true)
	public KegiatanTugasJabatan getInduk() {
		induk = check(induk);
		return induk;
	}

	/**
	 * Menetapkan kegiatan induk (parent) kegiatan ini dalam hierarki pohon.
	 *
	 * @param induk kegiatan induk baru.
	 */
	public void setInduk(KegiatanTugasJabatan induk) {
		this.induk = induk;
	}

	/**
	 * Mengembalikan kedalaman (level) kegiatan ini dalam hierarki pohon, dihitung dari akar
	 * (kegiatan tanpa induk); dipakai untuk indentasi tampilan tree.
	 *
	 * @return kedalaman level, dapat {@code null} bila belum dihitung/diisi.
	 */
	public Integer getDeep() {
		return deep;
	}

	/**
	 * Menetapkan kedalaman level kegiatan ini dalam hierarki pohon.
	 *
	 * @param deep kedalaman level baru.
	 */
	public void setDeep(Integer deep) {
		this.deep = deep;
	}

	/**
	 * Mengembalikan jumlah pemakaian (cache count) kegiatan ini — indikasi berapa kali kegiatan ini
	 * sudah dirujuk (mis. oleh {@link TargetKerjaPegawai}), dipakai antara lain untuk mencegah
	 * penghapusan kegiatan yang masih dipakai.
	 *
	 * @return jumlah pemakaian, dapat {@code null} bila belum dihitung.
	 */
	public Long getJmlDipakai() {
		return jmlDipakai;
	}

	/**
	 * Menetapkan jumlah pemakaian (cache count) kegiatan ini.
	 *
	 * @param jmlDipakai jumlah pemakaian baru.
	 */
	public void setJmlDipakai(Long jmlDipakai) {
		this.jmlDipakai = jmlDipakai;
	}

	/**
	 * Mengembalikan tipe periode penugasan kegiatan ini, salah satu dari {@link #BULANAN} atau
	 * {@link #TAHUNAN}. Default {@link #BULANAN} bila belum diisi/kosong.
	 *
	 * @return tipe periode; {@link #BULANAN} bila kosong/belum diisi.
	 */
	public String getPeriode() {
		return periode == null || periode.trim().isEmpty() ? BULANAN : periode;
	}

	/**
	 * Menetapkan tipe periode penugasan kegiatan ini.
	 *
	 * @param periode tipe periode baru, sebaiknya {@link #BULANAN} atau {@link #TAHUNAN}.
	 */
	public void setPeriode(String periode) {
		this.periode = periode;
	}

	/**
	 * Mengembalikan {@link Tbmrole} (peran/jabatan) yang menjadi pemegang tugas kegiatan ini —
	 * dipakai untuk menentukan populasi pegawai mana yang relevan ditugaskan kegiatan ini. Relasi
	 * lazy, opsional.
	 *
	 * @return peran pemegang tugas, dapat {@code null} bila kegiatan berlaku lintas peran.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "userrole", nullable = true)
	public Tbmrole getUserRole() {
		userRole = check(userRole);
		return this.userRole;
	}

	/**
	 * Menetapkan peran/jabatan pemegang tugas kegiatan ini. Nilai {@code null} diabaikan secara
	 * diam-diam (peran yang sudah tersimpan dipertahankan).
	 *
	 * @param userRole peran baru; diabaikan jika {@code null}.
	 */
	public void setUserRole(Tbmrole userRole) {
		if (userRole == null) {
			return;
		}
		this.userRole = userRole;
	}

}
