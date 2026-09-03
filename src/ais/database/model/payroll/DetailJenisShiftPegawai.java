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

	/** Representasi numerik jam.menit mulai shift (mis. 8.30), dihitung ulang oleh {@link #getJarakMulai()}. */
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

	public Integer getKe() {
		return ke;
	}

	public void setKe(Integer ke) {
		this.ke = ke;
	}

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

	public void setJumlahSecond(Integer jumlahSecond) {
		this.jumlahSecond = jumlahSecond;
	}

	public Double getJarakMulai() {
		if (getMulai() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getMulai());
			String da = calendar.get(Calendar.HOUR_OF_DAY) + "." + calendar.get(Calendar.MINUTE);
			jarakMulai = Double.parseDouble(da);
		}
		return jarakMulai;
	}

	public void setJarakMulai(Double jarakMulai) {
		this.jarakMulai = jarakMulai;
	}

	public Double getMenitTelat1() {
		return menitTelat1;
	}

	public void setMenitTelat1(Double menitTelat1) {
		this.menitTelat1 = menitTelat1;
	}

	public Double getPotonganTelat1() {
		return potonganTelat1;
	}

	public void setPotonganTelat1(Double potonganTelat1) {
		this.potonganTelat1 = potonganTelat1;
	}

	public Double getMenitTelat2() {
		return menitTelat2;
	}

	public void setMenitTelat2(Double menitTelat2) {
		this.menitTelat2 = menitTelat2;
	}

	public Double getPotonganTelat2() {
		return potonganTelat2;
	}

	public void setPotonganTelat2(Double potonganTelat2) {
		this.potonganTelat2 = potonganTelat2;
	}

	public Double getMenitTelat3() {
		return menitTelat3;
	}

	public void setMenitTelat3(Double menitTelat3) {
		this.menitTelat3 = menitTelat3;
	}

	public Double getPotonganTelat3() {
		return potonganTelat3;
	}

	public void setPotonganTelat3(Double potonganTelat3) {
		this.potonganTelat3 = potonganTelat3;
	}

	public Double getMenitTelat4() {
		return menitTelat4;
	}

	public void setMenitTelat4(Double menitTelat4) {
		this.menitTelat4 = menitTelat4;
	}

	public Double getPotonganTelat4() {
		return potonganTelat4;
	}

	public void setPotonganTelat4(Double potonganTelat4) {
		this.potonganTelat4 = potonganTelat4;
	}

	public Double getMenitCepat1() {
		return menitCepat1;
	}

	public void setMenitCepat1(Double menitCepat1) {
		this.menitCepat1 = menitCepat1;
	}

	public Double getPotonganCepat1() {
		return potonganCepat1;
	}

	public void setPotonganCepat1(Double potonganCepat1) {
		this.potonganCepat1 = potonganCepat1;
	}

	public Double getMenitCepat2() {
		return menitCepat2;
	}

	public void setMenitCepat2(Double menitCepat2) {
		this.menitCepat2 = menitCepat2;
	}

	public Double getPotonganCepat2() {
		return potonganCepat2;
	}

	public void setPotonganCepat2(Double potonganCepat2) {
		this.potonganCepat2 = potonganCepat2;
	}

	public Double getMenitCepat3() {
		return menitCepat3;
	}

	public void setMenitCepat3(Double menitCepat3) {
		this.menitCepat3 = menitCepat3;
	}

	public Double getPotonganCepat3() {
		return potonganCepat3;
	}

	public void setPotonganCepat3(Double potonganCepat3) {
		this.potonganCepat3 = potonganCepat3;
	}

	public Double getMenitCepat4() {
		return menitCepat4;
	}

	public void setMenitCepat4(Double menitCepat4) {
		this.menitCepat4 = menitCepat4;
	}

	public Double getPotonganCepat4() {
		return potonganCepat4;
	}

	public void setPotonganCepat4(Double potonganCepat4) {
		this.potonganCepat4 = potonganCepat4;
	}

	public Double getPotonganTidakMasuk() {
		return potonganTidakMasuk;
	}

	public void setPotonganTidakMasuk(Double potonganTidakMasuk) {
		this.potonganTidakMasuk = potonganTidakMasuk;
	}

	@Temporal(TemporalType.TIME)
	public Date getLemburMulai() {
		return lemburMulai;
	}

	public void setLemburMulai(Date lemburMulai) {
		this.lemburMulai = lemburMulai;
	}

	public Double getLemburMaks() {
		return lemburMaks;
	}

	public void setLemburMaks(Double lemburMaks) {
		this.lemburMaks = lemburMaks;
	}

	public String getHari() {
		return hari;
	}

	public void setHari(String hari) {
		this.hari = hari;
	}

	public Boolean getAktifkanAbsenFoto() {
		return aktifkanAbsenFoto == null ? true : aktifkanAbsenFoto;
	}

	public void setAktifkanAbsenFoto(Boolean aktifkanAbsenFoto) {
		this.aktifkanAbsenFoto = aktifkanAbsenFoto;
	}

	public Double getMenitSebelumJamMulai() {
		return menitSebelumJamMulai == null ? 30.0 : menitSebelumJamMulai;
	}

	public void setMenitSebelumJamMulai(Double menitSebelumJamMulai) {
		this.menitSebelumJamMulai = menitSebelumJamMulai;
	}

	public Double getMenitSetelahJamMulai() {
		return menitSetelahJamMulai == null ? 30.0 : menitSetelahJamMulai;
	}

	public void setMenitSetelahJamMulai(Double menitSetelahJamMulai) {
		this.menitSetelahJamMulai = menitSetelahJamMulai;
	}

	public Double getMenitSebelumJamSampai() {
		return menitSebelumJamSampai == null ? 30.0 : menitSebelumJamSampai;
	}

	public void setMenitSebelumJamSampai(Double menitSebelumJamSampai) {
		this.menitSebelumJamSampai = menitSebelumJamSampai;
	}

	public Double getMenitSetelahJamSampai() {
		return menitSetelahJamSampai == null ? 30.0 : menitSetelahJamSampai;
	}

	public void setMenitSetelahJamSampai(Double menitSetelahJamSampai) {
		this.menitSetelahJamSampai = menitSetelahJamSampai;
	}

	public Double getJamSebelumJamMulai() {
		return jamSebelumJamMulai == null ? 0.5 : jamSebelumJamMulai;
	}

	public void setJamSebelumJamMulai(Double jamSebelumJamMulai) {
		this.jamSebelumJamMulai = jamSebelumJamMulai;
	}

	public Double getJamSetelahJamMulai() {
		return jamSetelahJamMulai == null ? 0.5 : jamSetelahJamMulai;
	}

	public void setJamSetelahJamMulai(Double jamSetelahJamMulai) {
		this.jamSetelahJamMulai = jamSetelahJamMulai;
	}

	public Double getJamSebelumJamSampai() {
		return jamSebelumJamSampai == null ? 0.5 : jamSebelumJamSampai;
	}

	public void setJamSebelumJamSampai(Double jamSebelumJamSampai) {
		this.jamSebelumJamSampai = jamSebelumJamSampai;
	}

	public Double getJamSetelahJamSampai() {
		return jamSetelahJamSampai == null ? 0.5 : jamSetelahJamSampai;
	}

	public void setJamSetelahJamSampai(Double jamSetelahJamSampai) {
		this.jamSetelahJamSampai = jamSetelahJamSampai;
	}

	@Column(name = "konversi_jam_lembur", columnDefinition = "text")
	public String getKonversiJamLembur() {
		return konversiJamLembur == null ? "" : konversiJamLembur;
	}

	public void setKonversiJamLembur(String konversiJamLembur) {
		this.konversiJamLembur = konversiJamLembur;
	}

	public Boolean getKhususBuatHariLibur() {

		if (getJenisShiftPegawai() != null && !getJenisShiftPegawai().getHariLiburDitentukan()) {
			khususBuatHariLibur = false;
		}

		return khususBuatHariLibur == null ? false : khususBuatHariLibur;
	}

	public void setKhususBuatHariLibur(Boolean khususBuatHariLibur) {
		this.khususBuatHariLibur = khususBuatHariLibur;
	}

	public Boolean getLemburDihitungDariAwalMasuk() {
		return lemburDihitungDariAwalMasuk == null ? false : lemburDihitungDariAwalMasuk;
	}

	public void setLemburDihitungDariAwalMasuk(Boolean lemburDihitungDariAwalMasuk) {
		this.lemburDihitungDariAwalMasuk = lemburDihitungDariAwalMasuk;
	}

	public Boolean getJamMasukDanPulangOtomatisMenyesuakanWaktuShift() {
		return jamMasukDanPulangOtomatisMenyesuakanWaktuShift == null ? false
				: jamMasukDanPulangOtomatisMenyesuakanWaktuShift;
	}

	public void setJamMasukDanPulangOtomatisMenyesuakanWaktuShift(
			Boolean jamMasukDanPulangOtomatisMenyesuakanWaktuShift) {
		this.jamMasukDanPulangOtomatisMenyesuakanWaktuShift = jamMasukDanPulangOtomatisMenyesuakanWaktuShift;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "waktu_shift", nullable = true)
	public WaktuShift getWaktuShift() {
		waktuShift = check(waktuShift);
		return waktuShift;
	}

	public void setWaktuShift(WaktuShift waktuShift) {
		this.waktuShift = waktuShift;
	}

	public Integer getHariKe() {
		if (getJenisShiftPegawai() != null && getJenisShiftPegawai().getJumlahHariSamaDenganJumlahShift()) {
			hariKe = getKe();
		}
		return hariKe == null ? 1 : hariKe;
	}

	public void setHariKe(Integer hariKe) {
		this.hariKe = hariKe;
	}

	public Boolean getJadikanDefault() {
		return jadikanDefault == null ? false : jadikanDefault;
	}

	public void setJadikanDefault(Boolean jadikanDefault) {
		this.jadikanDefault = jadikanDefault;
	}

	@Transient
	public JenisShiftPunyaPegawai getJenisShiftPunyaPegawai() {
		return jenisShiftPunyaPegawai;
	}

	public void setJenisShiftPunyaPegawai(JenisShiftPunyaPegawai jenisShiftPunyaPegawai) {
		this.jenisShiftPunyaPegawai = jenisShiftPunyaPegawai;
	}

}
