package ais.database.model.payroll;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.Lokasi;

/**
 * Satu baris "detail" (item) dari sebuah definisi shift pegawai ({@link JenisShiftPegawai}), yaitu satu
 * segmen jam kerja konkret lengkap dengan toleransi keterlambatan/pulang cepat, aturan lembur, dan
 * jendela absen-foto yang berlaku untuk segmen tersebut.
 *
 * <p><b>Relasi terhadap {@link JenisShiftPegawai} (header/detail).</b> Entity ini adalah baris "detail"
 * (child) yang menunjuk balik ke {@link JenisShiftPegawai} sebagai "header" (parent) definisi shift lewat
 * field {@link #jenisShiftPegawai} (kolom FK {@code jenis_shift_pegawai}, {@code @ManyToOne}). Relasi TIDAK
 * bidirectional: {@code JenisShiftPegawai} tidak memiliki koleksi {@code List<DetailJenisShiftPegawai>} —
 * baris detail dicari lewat query Hibernate {@code Criteria} (lihat {@link ais.common.DetailJenisShiftPegawaiHelper})
 * yang memfilter berdasarkan {@code jenisShiftPegawai} dan urutan {@link #ke}. Satu header {@code JenisShiftPegawai}
 * dapat memiliki BANYAK baris detail:</p>
 * <ul>
 * <li>Untuk shift majemuk dalam satu hari (mis. shift pagi lalu shift siang), {@link #ke} membedakan urutan
 * shift ke berapa dalam hari yang sama (dipakai juga untuk label otomatis "Shift ke N" pada {@link #getNama()}).</li>
 * <li>Untuk shift yang berotasi lintas beberapa hari (lihat {@code JenisShiftPegawai.getBerotasi()}),
 * {@link #hariKe} menandai hari ke berapa dalam siklus rotasi; {@link #getHariKe()} otomatis menyamakan
 * nilai ini dengan {@link #ke} bila header menandai {@code jumlahHariSamaDenganJumlahShift}.</li>
 * <li>Untuk pola mingguan, {@link #hari} menyimpan nama hari spesifik (Senin, Selasa, dst.) — bila
 * {@code null}, baris detail dianggap berlaku untuk hari apa saja (dipakai sebagai fallback pencarian).</li>
 * </ul>
 *
 * <p><b>Bukan bagian langsung dari rantai penggajian.</b> Berdasarkan penelusuran kode di file ini dan
 * pemanggilnya, entity ini murni milik modul ABSENSI/JADWAL KERJA — tidak ada relasi (FK, field, maupun
 * query) dari class ini ke {@code ItemGajiPegawai} atau {@code TransaksiPegawai}. Pemakainya adalah
 * pipeline absensi ({@code ais.action.master.payroll.helper.ProsesAbsensiPegawai},
 * {@code ais.action.servlet.api.AbsensiApiAction}, {@code ais.action.master.ScanBerhasilAction},
 * {@code ais.database.model.StatuskehadiranKaryawanHarian}) yang membaca field toleransi/potongan di
 * sini untuk MENGHITUNG status kehadiran harian (telat, pulang cepat, tidak masuk, lembur). Nilai
 * potongan (mis. {@link #potonganTelat1}) adalah ATURAN/PARAMETER yang disimpan di entity ini; class ini
 * sendiri tidak melakukan posting ke tabel gaji — kalau nilai tersebut akhirnya memengaruhi komponen gaji,
 * itu terjadi lewat modul lain di luar file ini (di luar cakupan verifikasi berkas ini) yang membaca hasil
 * perhitungan {@code StatuskehadiranKaryawanHarian}.</p>
 *
 * <p><b>Peringatan pola getter destruktif.</b> Sejumlah getter di kelas ini menulis balik ke field instance
 * sebagai efek samping saat dipanggil (bukan sekadar membaca) — lihat javadoc masing-masing method:
 * {@link #getNama()} (menimpa nama dengan label "Shift ke N"), {@link #getMulai()}/{@link #getSampai()}
 * (menyalin jam dari {@link #waktuShift} jika ada), {@link #getJenisShiftPegawai()}/{@link #getWaktuShift()}
 * (resolusi proxy lazy lewat {@code GeneralValueObject.check(Object)}), {@link #getJumlah()}/
 * {@link #getJumlahSecond()}/{@link #getJarakMulai()} (hitung ulang dari jam mulai/sampai terkini), dan
 * {@link #getKhususBuatHariLibur()}/{@link #getHariKe()} (menyesuaikan diri dengan flag pada header). Pola
 * ini konsisten dengan pola berulang lain di codebase AIS dan BUKAN sesuatu yang unik pada file ini.</p>
 *
 * <p>Field audit {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} adalah pola shadow-audit standar:
 * karena {@link GeneralValueObject} bukan {@code @Entity} JPA (tidak bisa memaksakan listener audit generik
 * di superclass), setiap entity anak menyalin sendiri triplet field ini plus hook {@link #onUpdate()}. Ini
 * adalah keharusan teknis pada arsitektur AIS, bukan duplikasi yang keliru.</p>
 *
 * Bank generated by hbm2java
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "payroll", name = "detail_jenis_shift_pegawai")
public class DetailJenisShiftPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} antar build. Nilai ini
	 * warisan generator hbm2java dan sengaja tidak diubah kecuali struktur field berubah tak-kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key baris detail shift ini, di-generate database (identity) — lihat {@link #getId()}. */
	private Long id;

	/** Nama/username user yang terakhir membuat atau mengubah baris ini (field audit shadow). */
	private String oleh;

	/** ID user yang terakhir membuat atau mengubah baris ini (field audit shadow), pasangan {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan ID user yang terakhir mengubah baris ini.
	 *
	 * @return ID user audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID user audit ({@link #olehId}). Guard: nilai {@code null} atau string kosong/hanya-spasi
	 * diabaikan diam-diam agar baris audit yang sudah ada tidak tertimpa kosong oleh pemanggil yang lupa
	 * menyertakan identitas user (mis. proses batch/background).
	 *
	 * @param olehId ID user yang melakukan perubahan; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks ringkas baris detail shift ini untuk log/debug/tampilan combo-box, berformat
	 * {@code "<JenisShiftPegawai> - <nama>, <jam mulai> s.d <jam sampai>, <jumlah jam> jam"}.
	 *
	 * <p><b>Efek samping:</b> method ini memanggil {@link #getJenisShiftPegawai()} yang menulis balik field
	 * {@link #jenisShiftPegawai} hasil resolusi proxy lazy (lihat javadoc method tersebut) — bukan operasi
	 * baca murni. Jam mulai/sampai diformat lewat {@code Common.timeFormat}; bila {@code null} dicetak
	 * string kosong. Jumlah jam memanggil {@link #getJumlah()} yang JUGA menghitung ulang durasi dari jam
	 * mulai/sampai saat ini (lihat javadoc method itu untuk detail perhitungan lintas-hari).</p>
	 *
	 * @return string deskriptif baris shift, tidak pernah {@code null}
	 */
	public String toString() {
		jenisShiftPegawai = getJenisShiftPegawai();
		return jenisShiftPegawai + " - " + nama + ", " + (mulai == null ? "" : Common.timeFormat.get().format(mulai))
				+ " s.d " + (sampai == null ? "" : Common.timeFormat.get().format(sampai)) + ", "
				+ Common.numberFormat.get().format(getJumlah()) + " jam";
	}

	/**
	 * Mengisi nama/username user audit ({@link #oleh}). Guard sama seperti {@link #setOlehId(String)}:
	 * nilai {@code null} atau kosong/hanya-spasi diabaikan diam-diam supaya nilai audit lama tidak
	 * tertimpa kosong.
	 *
	 * @param oleh username user yang melakukan perubahan; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan username user yang terakhir mengubah baris ini.
	 *
	 * @return username user audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus-hidup JPA {@code @PreUpdate} — dipanggil otomatis oleh Hibernate tepat sebelum statement
	 * {@code UPDATE} dikirim ke database untuk entity ini. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang mengisi
	 * {@link #tanggal_dirubah} (dan field audit sejenis bila ada) dengan waktu saat ini, memastikan jejak
	 * "kapan terakhir diubah" selalu konsisten tanpa bergantung pada pemanggil yang mengeset manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Timestamp perubahan terakhir baris ini (field audit shadow). Diinisialisasi ke waktu saat object
	 * dibuat di JVM dan ditimpa otomatis oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi timestamp perubahan terakhir secara manual. Biasanya tidak perlu dipanggil langsung karena
	 * {@link #onUpdate()} sudah mengelolanya otomatis pada setiap update.
	 *
	 * @param tanggal_dirubah timestamp baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan timestamp perubahan terakhir baris ini.
	 *
	 * @return timestamp audit, dipetakan sebagai {@code TIMESTAMP}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama tampilan baris shift ini. Ditimpa otomatis oleh {@link #getNama()} menjadi "Shift ke {@link #ke}". */
	private String nama;

	/** Keterangan bebas (opsional) untuk baris shift ini. */
	private String keterangan;

	/** Nama hari (mis. "Senin") tempat baris shift ini berlaku; {@code null} berarti berlaku semua hari. */
	private String hari;

	/** Jam mulai shift. Ditimpa otomatis oleh {@link #getMulai()} dari {@link #waktuShift} bila terpasang. */
	private Date mulai = ais.ui.util.WaktuUtil.getDate();

	/** Jam selesai shift. Ditimpa otomatis oleh {@link #getSampai()} dari {@link #waktuShift} bila terpasang. */
	private Date sampai = ais.ui.util.WaktuUtil.getDate();

	/** Durasi shift dalam jam (desimal), dihitung ulang oleh {@link #getJumlah()} dari {@link #mulai}/{@link #sampai}. */
	private Double jumlah = 0.0;

	/** Durasi shift dalam detik, dihitung ulang oleh {@link #getJumlahSecond()} dari {@link #mulai}/{@link #sampai}. */
	private Integer jumlahSecond = 0;

	/** Header definisi shift (parent) yang memiliki baris detail ini — lihat {@link #getJenisShiftPegawai()}. */
	private JenisShiftPegawai jenisShiftPegawai;

	/** Urutan shift ke berapa dalam satu hari/siklus (1-based); dipakai untuk label nama dan pencarian rotasi. */
	private Integer ke = 1;

	/** Hari ke berapa dalam siklus rotasi shift; lihat {@link #getHariKe()} untuk aturan penyamaan otomatis. */
	private Integer hariKe = 1;

	/** Jumlah menit sejak tengah malam untuk jam mulai shift (mis. 08:30 -> 510.0), dihitung ulang oleh {@link #getJarakMulai()}. */
	private Double jarakMulai = 0.0;

	/** Sumber jam mulai/sampai kanonik (opsional); bila terpasang, menimpa {@link #mulai}/{@link #sampai}. */
	private WaktuShift waktuShift;

	/** Ambang menit keterlambatan tahap 1 yang memicu {@link #potonganTelat1}. */
	private Double menitTelat1;

	/** Besaran potongan yang diterapkan bila keterlambatan melewati {@link #menitTelat1}. */
	private Double potonganTelat1;

	/** Ambang menit keterlambatan tahap 2 yang memicu {@link #potonganTelat2}. */
	private Double menitTelat2;

	/** Besaran potongan yang diterapkan bila keterlambatan melewati {@link #menitTelat2}. */
	private Double potonganTelat2;

	/** Ambang menit keterlambatan tahap 3 yang memicu {@link #potonganTelat3}. */
	private Double menitTelat3;

	/** Besaran potongan yang diterapkan bila keterlambatan melewati {@link #menitTelat3}. */
	private Double potonganTelat3;

	/** Ambang menit keterlambatan tahap 4 (tertinggi) yang memicu {@link #potonganTelat4}. */
	private Double menitTelat4;

	/** Besaran potongan yang diterapkan bila keterlambatan melewati {@link #menitTelat4}. */
	private Double potonganTelat4;

	/** Ambang menit pulang cepat tahap 1 yang memicu {@link #potonganCepat1}. */
	private Double menitCepat1;

	/** Besaran potongan yang diterapkan bila pulang cepat melewati {@link #menitCepat1}. */
	private Double potonganCepat1;

	/** Ambang menit pulang cepat tahap 2 yang memicu {@link #potonganCepat2}. */
	private Double menitCepat2;

	/** Besaran potongan yang diterapkan bila pulang cepat melewati {@link #menitCepat2}. */
	private Double potonganCepat2;

	/** Ambang menit pulang cepat tahap 3 yang memicu {@link #potonganCepat3}. */
	private Double menitCepat3;

	/** Besaran potongan yang diterapkan bila pulang cepat melewati {@link #menitCepat3}. */
	private Double potonganCepat3;

	/** Ambang menit pulang cepat tahap 4 (tertinggi) yang memicu {@link #potonganCepat4}. */
	private Double menitCepat4;

	/** Besaran potongan yang diterapkan bila pulang cepat melewati {@link #menitCepat4}. */
	private Double potonganCepat4;

	/** Besaran potongan yang diterapkan bila pegawai sama sekali tidak masuk pada shift ini. */
	private Double potonganTidakMasuk;

	/** Titik waktu mulai dihitungnya lembur untuk shift ini. */
	private Date lemburMulai = ais.ui.util.WaktuUtil.getDate();

	/** Batas maksimum jam lembur yang diakui untuk shift ini. */
	private Double lemburMaks;

	/**
	 * Flag: shift ini khusus dipakai pada hari libur. Nilainya dipaksa {@code false} secara otomatis oleh
	 * {@link #getKhususBuatHariLibur()} bila header {@link #jenisShiftPegawai} tidak menandai hari libur
	 * sebagai konsep yang ditentukan ({@code hariLiburDitentukan}).
	 */
	private Boolean khususBuatHariLibur;

	/** Flag: lembur dihitung sejak jam masuk awal, bukan sejak {@link #lemburMulai}. */
	private Boolean lemburDihitungDariAwalMasuk;

	/** Teks/aturan konversi jam lembur (mis. pemetaan tingkat lembur ke pengali), disimpan sebagai kolom {@code text}. */
	private String konversiJamLembur;

	/** Flag: fitur validasi lokasi/foto saat absen diaktifkan untuk shift ini; default {@code true} bila {@code null}. */
	private Boolean aktifkanAbsenFoto;

	/** Toleransi menit sebelum jam mulai shift yang masih dianggap tepat waktu; default 30 menit bila {@code null}. */
	private Double menitSebelumJamMulai;

	/** Toleransi menit setelah jam mulai shift yang masih dianggap tepat waktu; default 30 menit bila {@code null}. */
	private Double menitSetelahJamMulai;

	/** Toleransi menit sebelum jam selesai shift untuk keperluan absen pulang; default 30 menit bila {@code null}. */
	private Double menitSebelumJamSampai;

	/** Toleransi menit setelah jam selesai shift untuk keperluan absen pulang; default 30 menit bila {@code null}. */
	private Double menitSetelahJamSampai;

	/** Toleransi jam (desimal) sebelum jam mulai shift; default 0.5 jam bila {@code null}. */
	private Double jamSebelumJamMulai;

	/** Toleransi jam (desimal) setelah jam mulai shift; default 0.5 jam bila {@code null}. */
	private Double jamSetelahJamMulai;

	/** Toleransi jam (desimal) sebelum jam selesai shift; default 0.5 jam bila {@code null}. */
	private Double jamSebelumJamSampai;

	/** Toleransi jam (desimal) setelah jam selesai shift; default 0.5 jam bila {@code null}. */
	private Double jamSetelahJamSampai;

	/** Flag: jam masuk dan pulang otomatis disesuaikan mengikuti {@link #waktuShift} tanpa input manual. */
	private Boolean jamMasukDanPulangOtomatisMenyesuakanWaktuShift;

	/** Flag: baris detail ini dijadikan shift default (fallback terakhir) bila tidak ada shift lain yang cocok. */
	private Boolean jadikanDefault;

	/**
	 * Asosiasi transient (tidak dipetakan JPA — lihat anotasi {@code @Transient} pada getter) yang
	 * menyimpan baris kepemilikan shift pegawai terkait, diisi manual oleh pemanggil (mis.
	 * {@code DetailJenisShiftPegawaiHelper}) untuk membawa konteks pemilik tanpa query tambahan.
	 */
	private JenisShiftPunyaPegawai jenisShiftPunyaPegawai;

	/**
	 * Konstruktor default tanpa argumen, dibutuhkan oleh Hibernate untuk instansiasi entity lewat
	 * reflection saat hydrating hasil query. Semua field dibiarkan pada nilai default deklarasinya.
	 */
	public DetailJenisShiftPegawai() {
	}

	/**
	 * Mengembalikan primary key baris detail shift ini.
	 *
	 * @return ID baris, {@code null} untuk instance yang belum dipersistensikan; kolom identity
	 *         database ({@code insertable = false}) sehingga tidak boleh diisi manual saat insert
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi ID baris. Karena kolom database bersifat {@code insertable = false} (identity), setter ini
	 * pada praktiknya hanya relevan untuk keperluan Hibernate hydration/testing, bukan untuk menetapkan ID
	 * baru secara manual sebelum insert.
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama tampilan baris shift ini.
	 *
	 * <p><b>Efek samping (getter destruktif):</b> setiap dipanggil, method ini MENIMPA field {@link #nama}
	 * dengan label yang dihasilkan otomatis dari {@link #ke}, berformat {@code "Shift ke " + ke} — nilai
	 * {@link #nama} apa pun yang sebelumnya di-set lewat {@link #setNama(String)} atau dimuat dari kolom
	 * {@code nama} di database akan HILANG begitu getter ini dipanggil sekali saja. Ini adalah pola getter
	 * destruktif yang berulang di codebase AIS: nama sesungguhnya bersifat kosmetik/derivatif dari urutan
	 * ({@link #ke}), bukan nilai independen yang bisa dikustomisasi bebas oleh user meski kolomnya
	 * {@code nullable = false} dan tampak seperti field biasa.</p>
	 *
	 * @return string "Shift ke N", dengan N adalah nilai {@link #ke} saat ini
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		nama = "Shift ke " + (ke);
		return this.nama;
	}

	/**
	 * Mengisi nama tampilan secara manual. Perlu dicatat bahwa nilai ini akan ditimpa kembali oleh
	 * {@link #getNama()} pada pemanggilan berikutnya (lihat javadoc getter), sehingga setter ini efeknya
	 * hanya sementara/kosmetik hingga getter dipanggil ulang.
	 *
	 * @param nama nama yang diinginkan; akan ditimpa oleh {@link #getNama()}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk baris shift ini.
	 *
	 * @return teks keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas untuk baris shift ini.
	 *
	 * @param keterangan teks keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jam mulai shift, dipetakan sebagai kolom {@code TIME} (hanya komponen jam, tanggal
	 * diabaikan oleh Hibernate).
	 *
	 * <p><b>Efek samping (getter destruktif):</b> bila {@link #waktuShift} terpasang (bukan {@code null}
	 * setelah resolusi lazy via {@link #getWaktuShift()}), method ini MENIMPA field {@link #mulai} dengan
	 * jam mulai dari {@link WaktuShift#getMulai()} setiap kali dipanggil — nilai {@link #mulai} yang di-set
	 * langsung lewat {@link #setMulai(Date)} hanya bertahan selama {@link #waktuShift} kosong. Ini membuat
	 * {@link #waktuShift}, bila diisi, menjadi sumber kebenaran (source of truth) untuk jam mulai, dan
	 * {@link #mulai} sendiri berfungsi sebagai cache/fallback lokal.</p>
	 *
	 * @return jam mulai shift efektif, tidak pernah {@code null} kecuali belum pernah diinisialisasi
	 */
	@Temporal(TemporalType.TIME)
	public Date getMulai() {
		if (getWaktuShift() != null) {
			mulai = getWaktuShift().getMulai();
		}
		return mulai;
	}

	/**
	 * Mengisi jam mulai shift secara langsung. Nilai ini akan ditimpa kembali oleh {@link #getMulai()} bila
	 * {@link #waktuShift} terpasang (lihat javadoc getter) — setter ini efektif hanya bila baris shift
	 * tidak memakai {@link WaktuShift} kanonik.
	 *
	 * @param mulai jam mulai baru
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan jam selesai shift, dipetakan sebagai kolom {@code TIME}.
	 *
	 * <p><b>Efek samping (getter destruktif):</b> pola identik dengan {@link #getMulai()} — bila
	 * {@link #waktuShift} terpasang, field {@link #sampai} ditimpa dari {@link WaktuShift#getSampai()}
	 * setiap pemanggilan.</p>
	 *
	 * @return jam selesai shift efektif, tidak pernah {@code null} kecuali belum pernah diinisialisasi
	 */
	@Temporal(TemporalType.TIME)
	public Date getSampai() {
		if (getWaktuShift() != null) {
			sampai = getWaktuShift().getSampai();
		}
		return sampai;
	}

	/**
	 * Mengisi jam selesai shift secara langsung. Nilai ini akan ditimpa kembali oleh {@link #getSampai()}
	 * bila {@link #waktuShift} terpasang (lihat javadoc getter).
	 *
	 * @param sampai jam selesai baru
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Menghitung ulang dan mengembalikan durasi shift ini dalam jam (desimal), berdasarkan selisih antara
	 * {@link #getMulai()} dan {@link #getSampai()}.
	 *
	 * <p><b>Cara kerja dan efek samping (getter destruktif).</b> Method ini TIDAK sekadar membaca field
	 * {@link #jumlah} — setiap dipanggil, ia menghitung ulang durasi dari jam mulai/sampai saat ini dan
	 * MENIMPA {@link #jumlah} dengan hasil barunya. Nilai apa pun yang di-set lewat
	 * {@link #setJumlah(Double)} atau dimuat dari database akan tertimpa begitu getter ini dipanggil.</p>
	 *
	 * <p><b>Algoritma.</b> Jika {@link #getMulai()} dan {@link #getSampai()} sama-sama tidak {@code null}:</p>
	 * <ol>
	 * <li>Ambil komponen jam:menit:detik dari {@link #mulai} dan {@link #sampai} — keduanya dipetakan
	 * sebagai kolom {@code TIME}, jadi komponen tanggalnya biasanya bukan tanggal shift sesungguhnya
	 * (mis. epoch atau tanggal saat baris dibuat).</li>
	 * <li>Untuk menghindari perbandingan yang terpengaruh tanggal sisa dari kolom {@code TIME} yang
	 * berbeda-beda, kedua {@link Calendar} ({@code start} dan {@code end}) dinormalisasi ke TANGGAL YANG
	 * SAMA — yaitu tanggal hari ini ({@code now}) — sebelum dibandingkan. Ini membuat perbandingan murni
	 * berbasis jam:menit:detik, terlepas dari tanggal aslinya di database.</li>
	 * <li><b>Penanganan shift lintas-hari (overnight):</b> bila {@code end} lebih awal dari {@code start}
	 * setelah dinormalisasi (mis. mulai 22:00, sampai 06:00), maka {@code endDateTime} digeser
	 * {@code +1} hari sebelum dihitung selisihnya — ini yang membuat shift malam yang melewati tengah
	 * malam tetap dihitung sebagai durasi positif yang benar (8 jam pada contoh di atas), bukan negatif
	 * atau 24 jam dikurangi selisih. Waspadai: aturan ini murni berbasis "end sebelum start setelah
	 * normalisasi ke tanggal yang sama" — untuk shift yang sengaja berdurasi lebih dari 24 jam (kasus
	 * tidak lazim), logika ini akan salah menganggapnya sebagai shift lintas-hari 1 hari saja.</li>
	 * <li>Selisih dihitung dalam detik lewat {@code Seconds.secondsBetween} (Joda-Time), lalu dikonversi
	 * ke jam dengan pembagian {@code / 3600.0} (pembagian floating-point, bukan integer, sehingga durasi
	 * pecahan seperti 7.5 jam terrepresentasi dengan benar).</li>
	 * </ol>
	 *
	 * <p>Jika salah satu dari {@link #getMulai()}/{@link #getSampai()} {@code null}, {@link #jumlah}
	 * di-set ke {@code 0.0}.</p>
	 *
	 * @return durasi shift dalam jam (desimal), tidak pernah {@code null}
	 */
	public Double getJumlah() {
		if (getMulai() != null && getSampai() != null) {
			Calendar now = ais.ui.util.WaktuUtil.getCalendar();

			Calendar start = ais.ui.util.WaktuUtil.getCalendar();
			start.setTime(getMulai());
			start.set(Calendar.YEAR, now.get(Calendar.YEAR));
			start.set(Calendar.MONTH, now.get(Calendar.MONTH));
			start.set(Calendar.DATE, now.get(Calendar.DATE));

			Calendar end = ais.ui.util.WaktuUtil.getCalendar();
			end.setTime(getSampai());
			end.set(Calendar.YEAR, now.get(Calendar.YEAR));
			end.set(Calendar.MONTH, now.get(Calendar.MONTH));
			end.set(Calendar.DATE, now.get(Calendar.DATE));

			DateTime startDateTime = new DateTime(start.getTime());
			DateTime endDateTime;
			if (end.before(start)) {
				endDateTime = new DateTime(end.getTime()).plusDays(1);
			} else {
				endDateTime = new DateTime(end.getTime());
			}

			Seconds hours = Seconds.secondsBetween(startDateTime, endDateTime);
			jumlah = hours.getSeconds() / 3600.0;
		} else {
			jumlah = 0.0;
		}
		return jumlah;
	}

	/**
	 * Mengisi durasi shift secara manual. Nilai ini akan ditimpa kembali oleh {@link #getJumlah()} pada
	 * pemanggilan berikutnya (lihat javadoc getter untuk algoritma perhitungan ulang), sehingga setter ini
	 * pada praktiknya hanya efektif sementara/untuk hydration awal.
	 *
	 * @param jumlah durasi jam yang diinginkan; akan ditimpa oleh {@link #getJumlah()}
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan header definisi shift ({@link JenisShiftPegawai}) yang memiliki baris detail ini —
	 * relasi {@code @ManyToOne} lewat kolom FK {@code jenis_shift_pegawai}.
	 *
	 * <p><b>Efek samping:</b> memanggil {@code GeneralValueObject.check(Object)} yang meresolusi proxy lazy
	 * Hibernate (bila {@link #jenisShiftPegawai} masih berupa proxy yang belum diinisialisasi) dan
	 * berpotensi menggantinya dengan instance kanonik dari {@code EntityIdentityMap} — hasil resolusi ini
	 * ditulis balik ke field {@link #jenisShiftPegawai}. Ini bukan getter baca-murni, tetapi pola standar di
	 * seluruh entity AIS untuk memastikan satu object Java per ID entity di JVM yang sama.</p>
	 *
	 * @return header shift pemilik baris detail ini, atau {@code null} bila kolom FK kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_shift_pegawai", nullable = true)
	public JenisShiftPegawai getJenisShiftPegawai() {
		jenisShiftPegawai = check(jenisShiftPegawai);
		return jenisShiftPegawai;
	}

	/**
	 * Mengaitkan baris detail ini dengan header definisi shift tertentu.
	 *
	 * @param jenisShiftPegawai header shift baru; {@code null} melepas asosiasi (kolom FK bersifat
	 *                          {@code nullable = true})
	 */
	public void setJenisShiftPegawai(JenisShiftPegawai jenisShiftPegawai) {
		this.jenisShiftPegawai = jenisShiftPegawai;
	}

	/**
	 * Mencari lokasi absen (dari hingga 10 slot lokasi yang dikonfigurasi pada header
	 * {@link #getJenisShiftPegawai()}) yang jaraknya PALING DEKAT dengan koordinat GPS yang diberikan.
	 *
	 * <p><b>Konteks pemakaian.</b> Method ini dipanggil dari alur validasi absen (lihat
	 * {@code ais.action.master.ScanBerhasilAction}) setelah pegawai melakukan scan/absen dari perangkat
	 * mobile yang mengirim koordinat lat/lng saat ini. Header {@link JenisShiftPegawai} dapat menyimpan
	 * hingga 10 titik lokasi berbeda ({@code getLokasi()} s.d. {@code getLokasi10()}) — misalnya untuk
	 * organisasi dengan beberapa cabang/gedung yang berbagi definisi shift yang sama. Method ini menentukan
	 * lokasi mana dari daftar tersebut yang paling relevan untuk validasi jarak (mis. menolak absen bila
	 * jarak ke lokasi terdekat melebihi radius yang diizinkan) tanpa pemanggil perlu tahu berapa banyak
	 * slot lokasi yang benar-benar terisi.</p>
	 *
	 * <p><b>Algoritma.</b></p>
	 * <ol>
	 * <li>Kumpulkan semua slot {@code Lokasi} yang tidak {@code null} dari header (lokasi 1 s.d. 10) ke
	 * dalam satu {@code List}. Slot yang kosong dilewati begitu saja — tidak semua organisasi memakai
	 * seluruh 10 slot.</li>
	 * <li><b>Guard input:</b> {@code lat}/{@code lng} berasal dari hasil scan perangkat pengguna yang bisa
	 * saja {@code null}, string kosong, atau format tidak valid (perangkat GPS gagal mendapat fix, input
	 * dimanipulasi, dsb.). Tanpa guard ini, {@code Double.parseDouble(null)} akan melempar
	 * {@code NullPointerException} yang tidak informatif. Guard memeriksa {@code null}/kosong lebih dulu,
	 * lalu membungkus {@code parseDouble} dalam {@code try/catch NumberFormatException} — bila salah satu
	 * gagal, method mengembalikan {@code null} secara eksplisit (bukan melempar exception) sehingga
	 * pemanggil ({@code ScanBerhasilAction}) dapat menangani kasus "tidak dapat menghitung jarak" dengan
	 * anggun (mis. menolak absen dengan pesan yang jelas) alih-alih crash.</li>
	 * <li>Parsing koordinat pemanggil dipindahkan ke LUAR loop lokasi (dilakukan sekali saja) karena
	 * nilainya invariant untuk semua kandidat lokasi yang dibandingkan — optimasi kecil dibanding
	 * memparse ulang string yang sama di setiap iterasi.</li>
	 * <li>Untuk tiap kandidat lokasi, hitung jarak lewat
	 * {@code Common.getDistanceBetweenPointsNew(lat1, lng1, lat2, lng2)} dan simpan pasangan
	 * (jarak → lokasi) ke dalam {@code TreeMap<Double, Lokasi>}. {@code TreeMap} secara otomatis
	 * mengurutkan berdasarkan key (jarak) menaik.</li>
	 * <li>Entry PALING KECIL dari {@code TreeMap} ({@code firstEntry()}) adalah lokasi terdekat — inilah
	 * yang dikembalikan. Bila tidak ada satu pun lokasi terkonfigurasi (semua slot kosong), {@code TreeMap}
	 * kosong dan method mengembalikan {@code null}.</li>
	 * </ol>
	 *
	 * <p><b>Catatan kewaspadaan:</b> bila dua lokasi kebetulan memiliki jarak yang PERSIS SAMA (secara
	 * {@code Double}), {@code TreeMap} hanya akan menyimpan salah satu (entry kedua menimpa entry pertama
	 * pada key yang identik) — dalam praktiknya jarak GPS riil nyaris tidak pernah benar-benar identik
	 * hingga presisi {@code double}, jadi risiko ini rendah tapi bukan nol.</p>
	 *
	 * @param lat  latitude posisi pemanggil saat ini (string mentah dari perangkat), boleh {@code null}/tidak valid
	 * @param lng  longitude posisi pemanggil saat ini (string mentah dari perangkat), boleh {@code null}/tidak valid
	 * @return pasangan (jarak dalam km, {@link Lokasi} terdekat), atau {@code null} bila koordinat input
	 *         tidak valid ATAU tidak ada satu pun lokasi terkonfigurasi pada header shift ini
	 */
	public Entry<Double, Lokasi> ambilJarakDanLokasiTerdekat(String lat, String lng) {
		DetailJenisShiftPegawai jenis = this;
		List<Lokasi> lokasis = new ArrayList<Lokasi>();
		if (jenis.getJenisShiftPegawai().getLokasi() != null) {
			lokasis.add(jenis.getJenisShiftPegawai().getLokasi());
		}
		if (jenis.getJenisShiftPegawai().getLokasi2() != null) {
			lokasis.add(jenis.getJenisShiftPegawai().getLokasi2());
		}
		if (jenis.getJenisShiftPegawai().getLokasi3() != null) {
			lokasis.add(jenis.getJenisShiftPegawai().getLokasi3());
		}
		if (jenis.getJenisShiftPegawai().getLokasi4() != null) {
			lokasis.add(jenis.getJenisShiftPegawai().getLokasi4());
		}
		if (jenis.getJenisShiftPegawai().getLokasi5() != null) {
			lokasis.add(jenis.getJenisShiftPegawai().getLokasi5());
		}
		if (jenis.getJenisShiftPegawai().getLokasi6() != null) {
			lokasis.add(jenis.getJenisShiftPegawai().getLokasi6());
		}
		if (jenis.getJenisShiftPegawai().getLokasi7() != null) {
			lokasis.add(jenis.getJenisShiftPegawai().getLokasi7());
		}
		if (jenis.getJenisShiftPegawai().getLokasi8() != null) {
			lokasis.add(jenis.getJenisShiftPegawai().getLokasi8());
		}
		if (jenis.getJenisShiftPegawai().getLokasi9() != null) {
			lokasis.add(jenis.getJenisShiftPegawai().getLokasi9());
		}
		if (jenis.getJenisShiftPegawai().getLokasi10() != null) {
			lokasis.add(jenis.getJenisShiftPegawai().getLokasi10());
		}

		// Guard: lat/lng dari hasil scan perangkat bisa null/kosong/tidak valid. Double.parseDouble(null)
		// melempar NullPointerException (di sun.misc.FloatingDecimal). Tanpa koordinat yang valid, jarak
		// tidak dapat dihitung; kembalikan null (pemanggil ScanBerhasilAction sudah menangani entry == null).
		// Parse dipindah ke luar loop karena nilainya tetap (invariant) untuk semua lokasi.
		if (lat == null || lat.trim().length() == 0 || lng == null || lng.trim().length() == 0) {
			return null;
		}
		double latitude2;
		double longitude2;
		try {
			latitude2 = Double.parseDouble(lat.trim());
			longitude2 = Double.parseDouble(lng.trim());
		} catch (NumberFormatException e) {
			return null;
		}

		TreeMap<Double, Lokasi> treeMapLokasi = new TreeMap<Double, Lokasi>();
		for (Lokasi lokasi : lokasis) {

			double latitude1 = lokasi.getLat();
			double longitude1 = lokasi.getLng();

			Double jarakKm = Common.getDistanceBetweenPointsNew(latitude1, longitude1, latitude2, longitude2);

			treeMapLokasi.put(jarakKm, lokasi);
		}

		return treeMapLokasi.isEmpty() ? null : treeMapLokasi.firstEntry();
	}

	/**
	 * Mengembalikan urutan shift ke berapa dalam satu hari/siklus rotasi (1-based).
	 *
	 * @return nilai urutan {@link #ke}, dipakai juga sebagai basis label {@link #getNama()} dan sebagai
	 *         kunci pencarian baris detail pada shift yang berotasi (lihat
	 *         {@code DetailJenisShiftPegawaiHelper.shiftDetail})
	 */
	public Integer getKe() {
		return ke;
	}

	/**
	 * Mengisi urutan shift ke berapa.
	 *
	 * @param ke nilai urutan baru (1-based)
	 */
	public void setKe(Integer ke) {
		this.ke = ke;
	}

	/**
	 * Menghitung ulang dan mengembalikan durasi shift ini dalam DETIK — versi granularitas lebih halus dari
	 * {@link #getJumlah()} (yang mengembalikan jam desimal), memakai algoritma normalisasi tanggal dan
	 * penanganan shift lintas-hari (overnight, {@code end.before(start)} → {@code +1} hari) yang identik
	 * persis dengan {@link #getJumlah()} — lihat javadoc method tersebut untuk penjelasan lengkap algoritma
	 * dan kaveatnya (termasuk batasan pada shift yang sengaja berdurasi lebih dari 24 jam).
	 *
	 * <p><b>Efek samping (getter destruktif):</b> sama seperti {@link #getJumlah()}, method ini MENIMPA
	 * field {@link #jumlahSecond} setiap dipanggil, bukan getter baca-murni.</p>
	 *
	 * @return durasi shift dalam detik (bulat), {@code 0} bila {@link #getMulai()} atau {@link #getSampai()}
	 *         {@code null}
	 */
	public Integer getJumlahSecond() {
		if (getMulai() != null && getSampai() != null) {
			Calendar now = ais.ui.util.WaktuUtil.getCalendar();

			Calendar start = ais.ui.util.WaktuUtil.getCalendar();
			start.setTime(getMulai());
			start.set(Calendar.YEAR, now.get(Calendar.YEAR));
			start.set(Calendar.MONTH, now.get(Calendar.MONTH));
			start.set(Calendar.DATE, now.get(Calendar.DATE));

			Calendar end = ais.ui.util.WaktuUtil.getCalendar();
			end.setTime(getSampai());
			end.set(Calendar.YEAR, now.get(Calendar.YEAR));
			end.set(Calendar.MONTH, now.get(Calendar.MONTH));
			end.set(Calendar.DATE, now.get(Calendar.DATE));

			DateTime startDateTime = new DateTime(start.getTime());
			DateTime endDateTime;
			if (end.before(start)) {
				endDateTime = new DateTime(end.getTime()).plusDays(1);
			} else {
				endDateTime = new DateTime(end.getTime());
			}

			Seconds hours = Seconds.secondsBetween(startDateTime, endDateTime);
			jumlahSecond = hours.getSeconds();
		} else {
			jumlahSecond = 0;
		}
		return jumlahSecond;
	}

	/**
	 * Mengisi durasi shift dalam detik secara manual. Ditimpa kembali oleh {@link #getJumlahSecond()} pada
	 * pemanggilan berikutnya (lihat javadoc getter).
	 *
	 * @param jumlahSecond durasi detik yang diinginkan; akan ditimpa oleh {@link #getJumlahSecond()}
	 */
	public void setJumlahSecond(Integer jumlahSecond) {
		this.jumlahSecond = jumlahSecond;
	}

	/**
	 * Menghitung ulang dan mengembalikan jumlah menit sejak tengah malam dari {@link #getMulai()}
	 * ({@code HOUR_OF_DAY * 60 + MINUTE}), dipakai sebagai "jarak" kasar untuk membandingkan seberapa
	 * dekat dua jam mulai shift satu sama lain (lihat pemakaiannya di
	 * {@code DetailJenisShiftPegawaiHelper.shiftDetail} dan {@code ais.action.master.payroll.util.CommonPayroll.getDetailJenisShiftPegawai}
	 * untuk memilih baris detail "terdekat" — {@code next} vs {@code back} — terhadap jam absen aktual
	 * pegawai; kedua pemanggil menghitung sisi "jam absen aktual" dengan rumus menit-sejak-tengah-malam
	 * yang sama secara terpisah, jadi representasi di sini harus tetap konsisten dengan keduanya).
	 *
	 * <p><b>Efek samping (getter destruktif):</b> bila {@link #getMulai()} tidak {@code null}, method ini
	 * MENIMPA field {@link #jarakMulai} setiap dipanggil.</p>
	 *
	 * <p><b>Riwayat bug (diperbaiki):</b> sebelum perbaikan ini, nilai dibentuk dengan menggabungkan
	 * {@code HOUR_OF_DAY} dan {@code MINUTE} sebagai string desimal tanpa zero-padding menit lalu diparse
	 * sebagai {@code double} ({@code hour + "." + minute}), sehingga mis. 08:05 dan 08:50 sama-sama
	 * menghasilkan {@code 8.5} (trailing zero diabaikan {@code Double.parseDouble}) — jam yang terpaut 45
	 * menit dianggap identik. Representasi menit-sejak-tengah-malam saat ini tidak ambigu: setiap jam:menit
	 * yang berbeda selalu menghasilkan nilai yang berbeda.</p>
	 *
	 * @return jumlah menit sejak tengah malam dari jam mulai, atau nilai {@link #jarakMulai} sebelumnya
	 *         (default {@code 0.0}) bila {@link #getMulai()} {@code null}
	 */
	public Double getJarakMulai() {
		if (getMulai() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getMulai());
			jarakMulai = (double) (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE));
		}
		return jarakMulai;
	}

	/**
	 * Mengisi nilai jarak-mulai secara manual. Ditimpa kembali oleh {@link #getJarakMulai()} pada
	 * pemanggilan berikutnya bila {@link #getMulai()} tidak {@code null} (lihat javadoc getter).
	 *
	 * @param jarakMulai nilai jarak-mulai yang diinginkan; berpotensi ditimpa oleh {@link #getJarakMulai()}
	 */
	public void setJarakMulai(Double jarakMulai) {
		this.jarakMulai = jarakMulai;
	}

	/**
	 * Mengembalikan ambang menit keterlambatan tahap 1. Bagian dari tangga potongan telat 4-tahap
	 * ({@link #menitTelat1}..{@link #menitTelat4}) yang dipasangkan dengan {@link #getPotonganTelat1()};
	 * dibaca oleh pipeline absensi untuk menentukan besaran potongan sesuai seberapa telat pegawai masuk.
	 *
	 * @return ambang menit tahap 1, boleh {@code null} bila tahap ini tidak dikonfigurasi
	 */
	public Double getMenitTelat1() {
		return menitTelat1;
	}

	/**
	 * Mengisi ambang menit keterlambatan tahap 1.
	 *
	 * @param menitTelat1 ambang menit baru
	 */
	public void setMenitTelat1(Double menitTelat1) {
		this.menitTelat1 = menitTelat1;
	}

	/**
	 * Mengembalikan besaran potongan untuk keterlambatan tahap 1 (dipicu setelah {@link #getMenitTelat1()}).
	 *
	 * @return besaran potongan tahap 1, boleh {@code null}
	 */
	public Double getPotonganTelat1() {
		return potonganTelat1;
	}

	/**
	 * Mengisi besaran potongan untuk keterlambatan tahap 1.
	 *
	 * @param potonganTelat1 besaran potongan baru
	 */
	public void setPotonganTelat1(Double potonganTelat1) {
		this.potonganTelat1 = potonganTelat1;
	}

	/**
	 * Mengembalikan ambang menit keterlambatan tahap 2, dipasangkan dengan {@link #getPotonganTelat2()}.
	 *
	 * @return ambang menit tahap 2, boleh {@code null}
	 */
	public Double getMenitTelat2() {
		return menitTelat2;
	}

	/**
	 * Mengisi ambang menit keterlambatan tahap 2.
	 *
	 * @param menitTelat2 ambang menit baru
	 */
	public void setMenitTelat2(Double menitTelat2) {
		this.menitTelat2 = menitTelat2;
	}

	/**
	 * Mengembalikan besaran potongan untuk keterlambatan tahap 2 (dipicu setelah {@link #getMenitTelat2()}).
	 *
	 * @return besaran potongan tahap 2, boleh {@code null}
	 */
	public Double getPotonganTelat2() {
		return potonganTelat2;
	}

	/**
	 * Mengisi besaran potongan untuk keterlambatan tahap 2.
	 *
	 * @param potonganTelat2 besaran potongan baru
	 */
	public void setPotonganTelat2(Double potonganTelat2) {
		this.potonganTelat2 = potonganTelat2;
	}

	/**
	 * Mengembalikan ambang menit keterlambatan tahap 3, dipasangkan dengan {@link #getPotonganTelat3()}.
	 *
	 * @return ambang menit tahap 3, boleh {@code null}
	 */
	public Double getMenitTelat3() {
		return menitTelat3;
	}

	/**
	 * Mengisi ambang menit keterlambatan tahap 3.
	 *
	 * @param menitTelat3 ambang menit baru
	 */
	public void setMenitTelat3(Double menitTelat3) {
		this.menitTelat3 = menitTelat3;
	}

	/**
	 * Mengembalikan besaran potongan untuk keterlambatan tahap 3 (dipicu setelah {@link #getMenitTelat3()}).
	 *
	 * @return besaran potongan tahap 3, boleh {@code null}
	 */
	public Double getPotonganTelat3() {
		return potonganTelat3;
	}

	/**
	 * Mengisi besaran potongan untuk keterlambatan tahap 3.
	 *
	 * @param potonganTelat3 besaran potongan baru
	 */
	public void setPotonganTelat3(Double potonganTelat3) {
		this.potonganTelat3 = potonganTelat3;
	}

	/**
	 * Mengembalikan ambang menit keterlambatan tahap 4 (tertinggi), dipasangkan dengan
	 * {@link #getPotonganTelat4()}.
	 *
	 * @return ambang menit tahap 4, boleh {@code null}
	 */
	public Double getMenitTelat4() {
		return menitTelat4;
	}

	/**
	 * Mengisi ambang menit keterlambatan tahap 4.
	 *
	 * @param menitTelat4 ambang menit baru
	 */
	public void setMenitTelat4(Double menitTelat4) {
		this.menitTelat4 = menitTelat4;
	}

	/**
	 * Mengembalikan besaran potongan untuk keterlambatan tahap 4 (dipicu setelah {@link #getMenitTelat4()}).
	 *
	 * @return besaran potongan tahap 4, boleh {@code null}
	 */
	public Double getPotonganTelat4() {
		return potonganTelat4;
	}

	/**
	 * Mengisi besaran potongan untuk keterlambatan tahap 4.
	 *
	 * @param potonganTelat4 besaran potongan baru
	 */
	public void setPotonganTelat4(Double potonganTelat4) {
		this.potonganTelat4 = potonganTelat4;
	}

	/**
	 * Mengembalikan ambang menit pulang cepat tahap 1. Bagian dari tangga potongan pulang-cepat 4-tahap
	 * ({@link #menitCepat1}..{@link #menitCepat4}), dipasangkan dengan {@link #getPotonganCepat1()}.
	 *
	 * @return ambang menit tahap 1, boleh {@code null}
	 */
	public Double getMenitCepat1() {
		return menitCepat1;
	}

	/**
	 * Mengisi ambang menit pulang cepat tahap 1.
	 *
	 * @param menitCepat1 ambang menit baru
	 */
	public void setMenitCepat1(Double menitCepat1) {
		this.menitCepat1 = menitCepat1;
	}

	/**
	 * Mengembalikan besaran potongan untuk pulang cepat tahap 1 (dipicu setelah {@link #getMenitCepat1()}).
	 *
	 * @return besaran potongan tahap 1, boleh {@code null}
	 */
	public Double getPotonganCepat1() {
		return potonganCepat1;
	}

	/**
	 * Mengisi besaran potongan untuk pulang cepat tahap 1.
	 *
	 * @param potonganCepat1 besaran potongan baru
	 */
	public void setPotonganCepat1(Double potonganCepat1) {
		this.potonganCepat1 = potonganCepat1;
	}

	/**
	 * Mengembalikan ambang menit pulang cepat tahap 2, dipasangkan dengan {@link #getPotonganCepat2()}.
	 *
	 * @return ambang menit tahap 2, boleh {@code null}
	 */
	public Double getMenitCepat2() {
		return menitCepat2;
	}

	/**
	 * Mengisi ambang menit pulang cepat tahap 2.
	 *
	 * @param menitCepat2 ambang menit baru
	 */
	public void setMenitCepat2(Double menitCepat2) {
		this.menitCepat2 = menitCepat2;
	}

	/**
	 * Mengembalikan besaran potongan untuk pulang cepat tahap 2 (dipicu setelah {@link #getMenitCepat2()}).
	 *
	 * @return besaran potongan tahap 2, boleh {@code null}
	 */
	public Double getPotonganCepat2() {
		return potonganCepat2;
	}

	/**
	 * Mengisi besaran potongan untuk pulang cepat tahap 2.
	 *
	 * @param potonganCepat2 besaran potongan baru
	 */
	public void setPotonganCepat2(Double potonganCepat2) {
		this.potonganCepat2 = potonganCepat2;
	}

	/**
	 * Mengembalikan ambang menit pulang cepat tahap 3, dipasangkan dengan {@link #getPotonganCepat3()}.
	 *
	 * @return ambang menit tahap 3, boleh {@code null}
	 */
	public Double getMenitCepat3() {
		return menitCepat3;
	}

	/**
	 * Mengisi ambang menit pulang cepat tahap 3.
	 *
	 * @param menitCepat3 ambang menit baru
	 */
	public void setMenitCepat3(Double menitCepat3) {
		this.menitCepat3 = menitCepat3;
	}

	/**
	 * Mengembalikan besaran potongan untuk pulang cepat tahap 3 (dipicu setelah {@link #getMenitCepat3()}).
	 *
	 * @return besaran potongan tahap 3, boleh {@code null}
	 */
	public Double getPotonganCepat3() {
		return potonganCepat3;
	}

	/**
	 * Mengisi besaran potongan untuk pulang cepat tahap 3.
	 *
	 * @param potonganCepat3 besaran potongan baru
	 */
	public void setPotonganCepat3(Double potonganCepat3) {
		this.potonganCepat3 = potonganCepat3;
	}

	/**
	 * Mengembalikan ambang menit pulang cepat tahap 4 (tertinggi), dipasangkan dengan
	 * {@link #getPotonganCepat4()}.
	 *
	 * @return ambang menit tahap 4, boleh {@code null}
	 */
	public Double getMenitCepat4() {
		return menitCepat4;
	}

	/**
	 * Mengisi ambang menit pulang cepat tahap 4.
	 *
	 * @param menitCepat4 ambang menit baru
	 */
	public void setMenitCepat4(Double menitCepat4) {
		this.menitCepat4 = menitCepat4;
	}

	/**
	 * Mengembalikan besaran potongan untuk pulang cepat tahap 4 (dipicu setelah {@link #getMenitCepat4()}).
	 *
	 * @return besaran potongan tahap 4, boleh {@code null}
	 */
	public Double getPotonganCepat4() {
		return potonganCepat4;
	}

	/**
	 * Mengisi besaran potongan untuk pulang cepat tahap 4.
	 *
	 * @param potonganCepat4 besaran potongan baru
	 */
	public void setPotonganCepat4(Double potonganCepat4) {
		this.potonganCepat4 = potonganCepat4;
	}

	/**
	 * Mengembalikan besaran potongan bila pegawai sama sekali tidak masuk pada shift ini (berbeda dari
	 * tangga telat/cepat yang bertahap — ini potongan flat untuk ketidakhadiran total).
	 *
	 * @return besaran potongan tidak masuk, boleh {@code null}
	 */
	public Double getPotonganTidakMasuk() {
		return potonganTidakMasuk;
	}

	/**
	 * Mengisi besaran potongan untuk ketidakhadiran total pada shift ini.
	 *
	 * @param potonganTidakMasuk besaran potongan baru
	 */
	public void setPotonganTidakMasuk(Double potonganTidakMasuk) {
		this.potonganTidakMasuk = potonganTidakMasuk;
	}

	/**
	 * Mengembalikan jam mulai dihitungnya lembur, dipetakan sebagai kolom {@code TIME}. Berbeda dari
	 * {@link #getLemburDihitungDariAwalMasuk()} yang bila {@code true} mengabaikan field ini demi jam
	 * masuk aktual pegawai.
	 *
	 * @return jam mulai lembur, boleh {@code null}
	 */
	@Temporal(TemporalType.TIME)
	public Date getLemburMulai() {
		return lemburMulai;
	}

	/**
	 * Mengisi jam mulai dihitungnya lembur.
	 *
	 * @param lemburMulai jam mulai lembur baru
	 */
	public void setLemburMulai(Date lemburMulai) {
		this.lemburMulai = lemburMulai;
	}

	/**
	 * Mengembalikan batas maksimum jam lembur yang diakui untuk shift ini.
	 *
	 * @return batas jam lembur maksimum, boleh {@code null} (berarti tidak dibatasi)
	 */
	public Double getLemburMaks() {
		return lemburMaks;
	}

	/**
	 * Mengisi batas maksimum jam lembur yang diakui.
	 *
	 * @param lemburMaks batas jam lembur maksimum baru
	 */
	public void setLemburMaks(Double lemburMaks) {
		this.lemburMaks = lemburMaks;
	}

	/**
	 * Mengembalikan nama hari spesifik tempat baris shift ini berlaku.
	 *
	 * @return nama hari (mis. "Senin"), atau {@code null} bila baris ini berlaku untuk hari apa saja
	 *         (dipakai sebagai kandidat fallback saat pencarian shift tidak menemukan baris untuk hari
	 *         spesifik — lihat {@code DetailJenisShiftPegawaiHelper.findDetailShift})
	 */
	public String getHari() {
		return hari;
	}

	/**
	 * Mengisi nama hari spesifik tempat baris shift ini berlaku.
	 *
	 * @param hari nama hari baru, atau {@code null} untuk menjadikannya berlaku semua hari
	 */
	public void setHari(String hari) {
		this.hari = hari;
	}

	/**
	 * Mengembalikan flag aktif/tidaknya validasi absen-foto (dan implisit, validasi lokasi lewat
	 * {@link #ambilJarakDanLokasiTerdekat(String, String)}) untuk shift ini.
	 *
	 * @return {@code true} bila fitur diaktifkan; default {@code true} bila belum pernah diset — perilaku
	 *         "aman secara default" yaitu validasi dianggap AKTIF kecuali secara eksplisit dimatikan
	 */
	public Boolean getAktifkanAbsenFoto() {
		return aktifkanAbsenFoto == null ? true : aktifkanAbsenFoto;
	}

	/**
	 * Mengisi flag aktif/tidaknya validasi absen-foto.
	 *
	 * @param aktifkanAbsenFoto nilai flag baru; {@code null} akan diperlakukan sebagai {@code true} oleh
	 *                          {@link #getAktifkanAbsenFoto()}
	 */
	public void setAktifkanAbsenFoto(Boolean aktifkanAbsenFoto) {
		this.aktifkanAbsenFoto = aktifkanAbsenFoto;
	}

	/**
	 * Mengembalikan toleransi menit SEBELUM jam mulai shift yang masih dianggap tepat waktu (mis. untuk
	 * jendela absen masuk yang diperbolehkan).
	 *
	 * @return toleransi menit; default {@code 30.0} bila belum pernah diset
	 */
	public Double getMenitSebelumJamMulai() {
		return menitSebelumJamMulai == null ? 30.0 : menitSebelumJamMulai;
	}

	/**
	 * Mengisi toleransi menit sebelum jam mulai shift.
	 *
	 * @param menitSebelumJamMulai toleransi menit baru
	 */
	public void setMenitSebelumJamMulai(Double menitSebelumJamMulai) {
		this.menitSebelumJamMulai = menitSebelumJamMulai;
	}

	/**
	 * Mengembalikan toleransi menit SETELAH jam mulai shift yang masih dianggap tepat waktu.
	 *
	 * @return toleransi menit; default {@code 30.0} bila belum pernah diset
	 */
	public Double getMenitSetelahJamMulai() {
		return menitSetelahJamMulai == null ? 30.0 : menitSetelahJamMulai;
	}

	/**
	 * Mengisi toleransi menit setelah jam mulai shift.
	 *
	 * @param menitSetelahJamMulai toleransi menit baru
	 */
	public void setMenitSetelahJamMulai(Double menitSetelahJamMulai) {
		this.menitSetelahJamMulai = menitSetelahJamMulai;
	}

	/**
	 * Mengembalikan toleransi menit SEBELUM jam selesai shift untuk keperluan absen pulang.
	 *
	 * @return toleransi menit; default {@code 30.0} bila belum pernah diset
	 */
	public Double getMenitSebelumJamSampai() {
		return menitSebelumJamSampai == null ? 30.0 : menitSebelumJamSampai;
	}

	/**
	 * Mengisi toleransi menit sebelum jam selesai shift.
	 *
	 * @param menitSebelumJamSampai toleransi menit baru
	 */
	public void setMenitSebelumJamSampai(Double menitSebelumJamSampai) {
		this.menitSebelumJamSampai = menitSebelumJamSampai;
	}

	/**
	 * Mengembalikan toleransi menit SETELAH jam selesai shift untuk keperluan absen pulang.
	 *
	 * @return toleransi menit; default {@code 30.0} bila belum pernah diset
	 */
	public Double getMenitSetelahJamSampai() {
		return menitSetelahJamSampai == null ? 30.0 : menitSetelahJamSampai;
	}

	/**
	 * Mengisi toleransi menit setelah jam selesai shift.
	 *
	 * @param menitSetelahJamSampai toleransi menit baru
	 */
	public void setMenitSetelahJamSampai(Double menitSetelahJamSampai) {
		this.menitSetelahJamSampai = menitSetelahJamSampai;
	}

	/**
	 * Mengembalikan toleransi jam (desimal) SEBELUM jam mulai shift — varian granularitas jam dari
	 * {@link #getMenitSebelumJamMulai()}, dipakai pada alur yang bekerja dalam satuan jam bukan menit.
	 *
	 * @return toleransi jam; default {@code 0.5} bila belum pernah diset
	 */
	public Double getJamSebelumJamMulai() {
		return jamSebelumJamMulai == null ? 0.5 : jamSebelumJamMulai;
	}

	/**
	 * Mengisi toleransi jam sebelum jam mulai shift.
	 *
	 * @param jamSebelumJamMulai toleransi jam baru
	 */
	public void setJamSebelumJamMulai(Double jamSebelumJamMulai) {
		this.jamSebelumJamMulai = jamSebelumJamMulai;
	}

	/**
	 * Mengembalikan toleransi jam (desimal) SETELAH jam mulai shift.
	 *
	 * @return toleransi jam; default {@code 0.5} bila belum pernah diset
	 */
	public Double getJamSetelahJamMulai() {
		return jamSetelahJamMulai == null ? 0.5 : jamSetelahJamMulai;
	}

	/**
	 * Mengisi toleransi jam setelah jam mulai shift.
	 *
	 * @param jamSetelahJamMulai toleransi jam baru
	 */
	public void setJamSetelahJamMulai(Double jamSetelahJamMulai) {
		this.jamSetelahJamMulai = jamSetelahJamMulai;
	}

	/**
	 * Mengembalikan toleransi jam (desimal) SEBELUM jam selesai shift.
	 *
	 * @return toleransi jam; default {@code 0.5} bila belum pernah diset
	 */
	public Double getJamSebelumJamSampai() {
		return jamSebelumJamSampai == null ? 0.5 : jamSebelumJamSampai;
	}

	/**
	 * Mengisi toleransi jam sebelum jam selesai shift.
	 *
	 * @param jamSebelumJamSampai toleransi jam baru
	 */
	public void setJamSebelumJamSampai(Double jamSebelumJamSampai) {
		this.jamSebelumJamSampai = jamSebelumJamSampai;
	}

	/**
	 * Mengembalikan toleransi jam (desimal) SETELAH jam selesai shift.
	 *
	 * @return toleransi jam; default {@code 0.5} bila belum pernah diset
	 */
	public Double getJamSetelahJamSampai() {
		return jamSetelahJamSampai == null ? 0.5 : jamSetelahJamSampai;
	}

	/**
	 * Mengisi toleransi jam setelah jam selesai shift.
	 *
	 * @param jamSetelahJamSampai toleransi jam baru
	 */
	public void setJamSetelahJamSampai(Double jamSetelahJamSampai) {
		this.jamSetelahJamSampai = jamSetelahJamSampai;
	}

	/**
	 * Mengembalikan teks/aturan konversi jam lembur (mis. pemetaan tingkat lembur ke pengali gaji), kolom
	 * database bertipe {@code text} bebas format.
	 *
	 * @return teks konversi jam lembur; string kosong ({@code ""}) bila belum pernah diisi — TIDAK PERNAH
	 *         {@code null}, berbeda dari kebanyakan getter String lain di kelas ini yang membolehkan
	 *         {@code null}
	 */
	@Column(name = "konversi_jam_lembur", columnDefinition = "text")
	public String getKonversiJamLembur() {
		return konversiJamLembur == null ? "" : konversiJamLembur;
	}

	/**
	 * Mengisi teks/aturan konversi jam lembur.
	 *
	 * @param konversiJamLembur teks konversi baru, boleh {@code null} (akan tampil sebagai string kosong
	 *                          lewat {@link #getKonversiJamLembur()})
	 */
	public void setKonversiJamLembur(String konversiJamLembur) {
		this.konversiJamLembur = konversiJamLembur;
	}

	/**
	 * Mengembalikan flag apakah baris shift ini khusus dipakai pada hari libur.
	 *
	 * <p><b>Efek samping (getter destruktif, flag satu-arah bersyarat).</b> Sebelum mengembalikan nilai,
	 * method ini memeriksa header {@link #getJenisShiftPegawai()}: bila header ADA dan header TIDAK
	 * menandai konsep hari libur sebagai sesuatu yang ditentukan ({@code !getHariLiburDitentukan()}), maka
	 * field {@link #khususBuatHariLibur} DIPAKSA menjadi {@code false} dan DITULIS BALIK ke field instance
	 * — menimpa nilai apa pun yang sebelumnya di-set lewat {@link #setKhususBuatHariLibur(Boolean)} atau
	 * dimuat dari database. Ini adalah pola "flag satu-arah bersyarat" yang berulang di codebase AIS: flag
	 * hanya bisa dipaksa dari {@code true}/tak-tentu MENJADI {@code false} berdasarkan kondisi pada entity
	 * lain (di sini, header shift), tidak pernah sebaliknya (getter ini tidak pernah memaksa flag menjadi
	 * {@code true}). Jika header {@code null} atau {@code getHariLiburDitentukan()} bernilai {@code true},
	 * field TIDAK diubah dan nilai tersimpan sebelumnya (atau default {@code false}) yang dikembalikan.</p>
	 *
	 * @return {@code true} bila baris ini khusus untuk hari libur DAN header masih menandai konsep hari
	 *         libur sebagai berlaku; {@code false} dalam semua kasus lain (termasuk saat dipaksa oleh
	 *         efek samping di atas)
	 */
	public Boolean getKhususBuatHariLibur() {

		if (getJenisShiftPegawai() != null && !getJenisShiftPegawai().getHariLiburDitentukan()) {
			khususBuatHariLibur = false;
		}

		return khususBuatHariLibur == null ? false : khususBuatHariLibur;
	}

	/**
	 * Mengisi flag "khusus untuk hari libur" secara manual. Perlu diperhatikan bahwa nilai {@code true} di
	 * sini dapat DIPAKSA kembali menjadi {@code false} oleh {@link #getKhususBuatHariLibur()} pada
	 * pemanggilan berikutnya, tergantung konfigurasi header {@link #jenisShiftPegawai} (lihat javadoc
	 * getter).
	 *
	 * @param khususBuatHariLibur nilai flag baru
	 */
	public void setKhususBuatHariLibur(Boolean khususBuatHariLibur) {
		this.khususBuatHariLibur = khususBuatHariLibur;
	}

	/**
	 * Mengembalikan flag apakah lembur dihitung sejak jam masuk aktual pegawai (bukan sejak
	 * {@link #getLemburMulai()} yang telah dikonfigurasi tetap).
	 *
	 * @return {@code true} bila lembur dihitung dari awal masuk; default {@code false} bila belum pernah
	 *         diset (perilaku default: pakai {@link #lemburMulai} tetap)
	 */
	public Boolean getLemburDihitungDariAwalMasuk() {
		return lemburDihitungDariAwalMasuk == null ? false : lemburDihitungDariAwalMasuk;
	}

	/**
	 * Mengisi flag "lembur dihitung dari awal masuk".
	 *
	 * @param lemburDihitungDariAwalMasuk nilai flag baru
	 */
	public void setLemburDihitungDariAwalMasuk(Boolean lemburDihitungDariAwalMasuk) {
		this.lemburDihitungDariAwalMasuk = lemburDihitungDariAwalMasuk;
	}

	/**
	 * Mengembalikan flag apakah jam masuk dan pulang otomatis mengikuti {@link #getWaktuShift()} tanpa
	 * memerlukan input/override manual dari pengguna.
	 *
	 * @return {@code true} bila penyesuaian otomatis aktif; default {@code false} bila belum pernah diset
	 */
	public Boolean getJamMasukDanPulangOtomatisMenyesuakanWaktuShift() {
		return jamMasukDanPulangOtomatisMenyesuakanWaktuShift == null ? false
				: jamMasukDanPulangOtomatisMenyesuakanWaktuShift;
	}

	/**
	 * Mengisi flag penyesuaian otomatis jam masuk/pulang terhadap {@link WaktuShift}.
	 *
	 * @param jamMasukDanPulangOtomatisMenyesuakanWaktuShift nilai flag baru
	 */
	public void setJamMasukDanPulangOtomatisMenyesuakanWaktuShift(
			Boolean jamMasukDanPulangOtomatisMenyesuakanWaktuShift) {
		this.jamMasukDanPulangOtomatisMenyesuakanWaktuShift = jamMasukDanPulangOtomatisMenyesuakanWaktuShift;
	}

	/**
	 * Mengembalikan sumber jam mulai/sampai kanonik ({@link WaktuShift}) yang, bila terpasang, menjadi
	 * acuan untuk {@link #getMulai()} dan {@link #getSampai()} — relasi {@code @ManyToOne} lewat kolom FK
	 * {@code waktu_shift}.
	 *
	 * <p><b>Efek samping:</b> memanggil {@code GeneralValueObject.check(Object)} yang meresolusi proxy
	 * lazy Hibernate dan menulis balik hasil resolusi ke field {@link #waktuShift} — pola identik dengan
	 * {@link #getJenisShiftPegawai()}, lihat javadoc method itu untuk penjelasan mekanisme
	 * {@code check()}.</p>
	 *
	 * @return object {@link WaktuShift} terkait, atau {@code null} bila kolom FK kosong (dalam hal ini
	 *         {@link #mulai}/{@link #sampai} dipakai apa adanya tanpa ditimpa)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "waktu_shift", nullable = true)
	public WaktuShift getWaktuShift() {
		waktuShift = check(waktuShift);
		return waktuShift;
	}

	/**
	 * Mengaitkan baris detail ini dengan sumber jam kanonik {@link WaktuShift} tertentu.
	 *
	 * @param waktuShift object {@link WaktuShift} baru; {@code null} melepas asosiasi
	 */
	public void setWaktuShift(WaktuShift waktuShift) {
		this.waktuShift = waktuShift;
	}

	/**
	 * Mengembalikan hari ke berapa dalam siklus rotasi shift tempat baris detail ini berlaku.
	 *
	 * <p><b>Efek samping (getter destruktif, sinkronisasi bersyarat dengan header).</b> Bila header
	 * {@link #getJenisShiftPegawai()} ADA dan header menandai bahwa jumlah hari dalam siklus SAMA DENGAN
	 * jumlah shift ({@code getJumlahHariSamaDenganJumlahShift()} — kasus umum: satu shift per hari dalam
	 * siklus, tidak ada hari dengan shift ganda atau hari kosong), maka field {@link #hariKe} DITIMPA
	 * dengan {@link #getKe()} — asumsinya, dalam konfigurasi seperti ini, urutan shift ({@link #ke}) dan
	 * urutan hari ({@link #hariKe}) selalu identik sehingga tidak perlu dikonfigurasi terpisah. Bila header
	 * {@code null} atau flag tersebut {@code false} (siklus punya struktur hari/shift yang lebih kompleks,
	 * mis. 2 shift dalam 1 hari lalu 1 hari kosong), {@link #hariKe} TIDAK diubah dan nilai yang tersimpan
	 * (hasil {@link #setHariKe(Integer)} atau dari database) yang dipakai.</p>
	 *
	 * @return hari ke berapa dalam siklus rotasi; default {@code 1} bila belum pernah diset
	 */
	public Integer getHariKe() {
		if (getJenisShiftPegawai() != null && getJenisShiftPegawai().getJumlahHariSamaDenganJumlahShift()) {
			hariKe = getKe();
		}
		return hariKe == null ? 1 : hariKe;
	}

	/**
	 * Mengisi hari ke berapa dalam siklus rotasi secara manual. Nilai ini dapat ditimpa kembali oleh
	 * {@link #getHariKe()} tergantung konfigurasi header (lihat javadoc getter).
	 *
	 * @param hariKe nilai hari-ke baru
	 */
	public void setHariKe(Integer hariKe) {
		this.hariKe = hariKe;
	}

	/**
	 * Mengembalikan flag apakah baris detail ini dijadikan shift default — fallback TERAKHIR yang dipakai
	 * oleh {@code DetailJenisShiftPegawaiHelper.getDetailJenisShiftPegawai} bila seluruh strategi pencarian
	 * shift lain (kepemilikan langsung, hierarki sekolah/yayasan/jurusan/fakultas, default per-kategori)
	 * gagal menemukan baris yang cocok.
	 *
	 * @return {@code true} bila baris ini adalah shift default; default {@code false} bila belum pernah
	 *         diset
	 */
	public Boolean getJadikanDefault() {
		return jadikanDefault == null ? false : jadikanDefault;
	}

	/**
	 * Mengisi flag "jadikan default" untuk baris detail ini.
	 *
	 * @param jadikanDefault nilai flag baru
	 */
	public void setJadikanDefault(Boolean jadikanDefault) {
		this.jadikanDefault = jadikanDefault;
	}

	/**
	 * Mengembalikan baris kepemilikan shift pegawai ({@link JenisShiftPunyaPegawai}) yang diasosiasikan
	 * secara manual dengan baris detail ini.
	 *
	 * <p>Field ini bertanda {@code @Transient} — TIDAK dipetakan ke kolom database apa pun dan tidak
	 * pernah dimuat otomatis oleh Hibernate saat query. Nilainya harus diisi eksplisit oleh pemanggil lewat
	 * {@link #setJenisShiftPunyaPegawai(JenisShiftPunyaPegawai)} — lihat pemakaiannya di
	 * {@code DetailJenisShiftPegawaiHelper.getDetailJenisShiftPegawai}, yang menempelkan
	 * {@link JenisShiftPunyaPegawai} hasil pencarian kepemilikan langsung ke object detail shift yang
	 * dikembalikan, sehingga pemanggil di lapisan atas dapat mengetahui BARIS KEPEMILIKAN mana yang
	 * menghasilkan detail shift ini tanpa perlu query ulang.</p>
	 *
	 * @return object kepemilikan shift yang ditempelkan manual, atau {@code null} bila belum pernah diisi
	 *         pada instance ini
	 */
	@Transient
	public JenisShiftPunyaPegawai getJenisShiftPunyaPegawai() {
		return jenisShiftPunyaPegawai;
	}

	/**
	 * Mengisi/menempelkan baris kepemilikan shift pegawai terkait ke instance ini. Murni state transient
	 * di sisi Java, tidak pernah dipersistensikan.
	 *
	 * @param jenisShiftPunyaPegawai object kepemilikan shift yang ingin ditempelkan, boleh {@code null}
	 */
	public void setJenisShiftPunyaPegawai(JenisShiftPunyaPegawai jenisShiftPunyaPegawai) {
		this.jenisShiftPunyaPegawai = jenisShiftPunyaPegawai;
	}

}
