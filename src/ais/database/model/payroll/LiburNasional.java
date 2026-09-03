package ais.database.model.payroll;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.envers.Audited;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate katalog <b>hari libur nasional/merah</b> — dipetakan ke tabel
 * {@code payroll.libur_nasional}. Satu baris merepresentasikan satu periode libur (bisa satu hari
 * atau rentang, lihat {@link #getTanggal()}/{@link #getSampai()}), misalnya tanggal merah
 * pemerintah, cuti bersama, atau libur panjang institusi (Idul Fitri, Natal &amp; Tahun Baru).
 *
 * <h2>Master data ter-cache penuh di memori JVM</h2>
 * <p>
 * Seluruh baris tabel dimuat sekali ke {@link #liburNasionalsMaster} (lihat
 * {@link #reInitLiburNasional()}) dan dipakai ulang oleh seluruh pencarian statis di kelas ini
 * ({@link #ambilLiburNasional(Date)}, {@link #ambilLiburNasional(Date, Date)}) — BUKAN query
 * database per akses. Konsekuensinya: perubahan data libur nasional lewat layar admin tidak
 * langsung terlihat pemanggil lain sampai {@link #reInitLiburNasional()} dipanggil ulang (mis.
 * setelah simpan/hapus di {@code LiburNasionalAction}) atau proses JVM di-restart.
 * </p>
 *
 * <h2>Pemakaian lintas modul</h2>
 * <p>
 * Meski berada di paket {@code ais.database.model.payroll} dan tabel bernaung di skema
 * {@code payroll}, katalog ini dikonsumsi LINTAS MODUL lewat {@code ais.common.Common} — util
 * generik non-payroll (mis. {@code Common#isHoliday}, {@code Common#isHolidayMerahDanAtauHariLibur})
 * memindai {@link #ambilLiburNasionalsMaster()} untuk menentukan apakah suatu tanggal adalah hari
 * libur merah. Laporan kehadiran modul akademik (guru/pegawai sekolah, mis.
 * {@code LaporanAbsensiPegawaiPerHari}, {@code LaporanAbsensiPegawaiPerHariGuru}) turut memakainya
 * secara tidak langsung lewat relasi {@code StatuskehadiranKaryawanHarian#getLiburNasional()} —
 * pola pemakaian lintas payroll+akademik yang sama seperti {@link LiburRutin} (lihat Javadoc kelas
 * tsb), walau {@code LiburNasional} berfokus pada tanggal merah nasional/institusi, sedangkan
 * {@code LiburRutin} pada pola libur rutin berulang (mis. akhir pekan).
 * </p>
 *
 * <h2>Dipakai aturan cuti ({@link #getLiburPanjang()})</h2>
 * <p>
 * Selain sebagai sumber "apakah tanggal X libur" untuk perhitungan hari kerja efektif (dipakai
 * antara lain oleh {@link ais.database.model.payroll.CutiDanIzin#getJumlahHariCuti()} untuk
 * mengecualikan hari libur dari hitungan hari cuti), kolom {@link #getLiburPanjang()} dipakai aturan
 * bisnis pengajuan cuti untuk membatasi pengajuan di sekitar periode libur panjang — lihat Javadoc
 * pada getter tsb.
 * </p>
 *
 * @see LiburRutin
 * @see ais.database.model.payroll.CutiDanIzin
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "payroll", name = "libur_nasional")
public class LiburNasional extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris libur nasional ini, di-generate database (IDENTITY). */
	private Long id;
	/** Nama pengguna yang terakhir membuat/mengubah baris ini (audit shadow field — lihat {@link #onUpdate()}). */
	private String oleh;
	/** Id pengguna yang terakhir membuat/mengubah baris ini (audit shadow field, pasangan {@link #oleh}). */
	private String olehId;

	/** @return {@link #olehId} — id pengguna audit terakhir yang mengubah baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Meng-set {@link #olehId}; dilewati (no-op) bila {@code olehId} {@code null} atau string
	 * kosong/berisi spasi saja, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param olehId id pengguna yang melakukan perubahan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** @return representasi teks baris ini, yaitu {@link #nama} hari libur (dipakai kombo/pencarian di layar ZK). */
	public String toString() {
		return nama;
	}

	/**
	 * Meng-set {@link #oleh}; dilewati (no-op) bila {@code oleh} {@code null} atau string
	 * kosong/berisi spasi saja, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param oleh nama pengguna yang melakukan perubahan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return {@link #oleh} — nama pengguna audit terakhir yang mengubah baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-UPDATE, mendelegasikan pembaruan timestamp audit ({@link #tanggal_dirubah}) ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak dipanggil manual
	 * dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Timestamp terakhir baris ini diubah (audit); default nilai awal saat konstruksi objek adalah waktu sekarang, diperbarui otomatis lewat {@link #onUpdate()} saat UPDATE. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah nilai timestamp audit baru; dipanggil manual maupun otomatis oleh
	 *                         {@link #onUpdate()}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return {@link #tanggal_dirubah} — timestamp terakhir baris ini diubah. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Cache statis seluruh baris {@code libur_nasional} di memori JVM — lihat "Master data ter-cache penuh" pada Javadoc kelas. Dibagikan oleh SELURUH thread/sesi (bukan per-request). */
	private static List<LiburNasional> liburNasionalsMaster = new ArrayList<LiburNasional>();
	/** Penanda apakah {@link #liburNasionalsMaster} sudah pernah dimuat dari database sejak JVM ini hidup. */
	private static boolean isMasterDiinisialisasi = false;

	/**
	 * @return {@link #liburNasionalsMaster} — seluruh baris libur nasional dari cache statis;
	 *         memuatnya dari database lebih dulu (lewat {@link #reInitLiburNasional()}) bila cache
	 *         belum pernah diinisialisasi sejak JVM ini hidup. List yang dikembalikan adalah
	 *         referensi langsung ke cache statis (bukan salinan) — pemanggil sebaiknya tidak
	 *         memodifikasinya.
	 */
	public static List<LiburNasional> ambilLiburNasionalsMaster() {
		// Pastikan data master sudah ditarik dari database
		if (!isMasterDiinisialisasi) {
			reInitLiburNasional();
		}

		return liburNasionalsMaster;
	}

	/**
	 * Memuat ULANG seluruh baris {@code libur_nasional} dari database ke {@link #liburNasionalsMaster},
	 * menggantikan isi cache sebelumnya, lalu menandai {@link #isMasterDiinisialisasi} {@code true}.
	 * Membuka sesi Hibernate baru sendiri ({@code HibernateUtil.getSessionFactory().openSession()})
	 * dan SELALU men-disconnect &amp; menutupnya di blok {@code finally}, sehingga aman dipanggil dari
	 * luar siklus request/thread-session biasa (mis. saat startup aplikasi via {@code InitData}, atau
	 * setelah admin menyimpan/menghapus data di {@code LiburNasionalAction}).
	 *
	 * <p>Bila query database gagal, exception ditangkap, dicatat ke
	 * {@link ais.common.ErrorAuditUtil}, dan method kembali TANPA melempar — {@link #liburNasionalsMaster}
	 * bisa jadi tertinggal (data lama) atau kosong tergantung kapan kegagalan terjadi, dan
	 * {@link #isMasterDiinisialisasi} TETAP tidak berubah menjadi {@code true} bila exception terjadi
	 * sebelum baris penanda tsb — sehingga pemanggilan berikutnya lewat
	 * {@link #ambilLiburNasionalsMaster()} akan mencoba memuat ulang lagi.</p>
	 */
	@SuppressWarnings("unchecked")
	public static void reInitLiburNasional() {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			liburNasionalsMaster = ConstantValues.simpleList(session.createCriteria(LiburNasional.class),
					LiburNasional.class);

			// Tandai bahwa data master sudah berhasil dimuat ke memori
			isMasterDiinisialisasi = true;

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/payroll/LiburNasional.java:110");
		} finally {
			if (session != null) {
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/LiburNasional.java:115");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/LiburNasional.java:119");
				}
			}
		}
	}

	/**
	 * Mencari SATU baris {@link LiburNasional} (dari cache {@link #liburNasionalsMaster}) yang
	 * periodenya ({@link #getTanggal()}..{@link #getSampai()}, kedua ujung inklusif) mencakup
	 * {@code tanggal} yang diberikan — jam/menit/detik pada kedua sisi dibuang lebih dulu lewat
	 * {@link #truncateTime(Date)} sebelum dibandingkan, sehingga pembandingan murni berbasis
	 * tanggal kalender.
	 *
	 * @param tanggal tanggal yang diperiksa; {@code null} langsung mengembalikan {@code null}
	 * @return baris {@code LiburNasional} PERTAMA (menurut urutan cache, tidak dijamin terurut
	 *         berdasarkan tanggal) yang periodenya mencakup {@code tanggal}, atau {@code null} bila
	 *         tidak ada yang cocok
	 */
	public static LiburNasional ambilLiburNasional(Date tanggal) {
		if (tanggal == null) {
			return null;
		}
		// Pastikan data master sudah ditarik dari database
		if (!isMasterDiinisialisasi) {
			reInitLiburNasional();
		}

		// Bersihkan jam/menit dari input agar murni tanggal
		Date dateClean = truncateTime(tanggal);

		// Looping pencarian data di dalam memori (List)
		for (LiburNasional libur : liburNasionalsMaster) {
			Date liburStart = truncateTime(libur.getTanggal());
			Date liburEnd = truncateTime(libur.getSampai());

			if (liburStart != null && liburEnd != null) {
				// Logic: Libur Start <= Input AND Libur End >= Input
				if (liburStart.compareTo(dateClean) <= 0 && liburEnd.compareTo(dateClean) >= 0) {
					return libur; // Kembalikan data pertama yang cocok
				}
			}
		}

		return null; // Tidak ditemukan libur pada tanggal tersebut
	}

	/**
	 * Mencari SEMUA baris {@link LiburNasional} (dari cache {@link #liburNasionalsMaster}) yang
	 * periodenya BERTUMPANG-TINDIH (overlap) dengan rentang {@code [tanggal, sampai]} yang
	 * diberikan — dua rentang tanggal dianggap overlap bila
	 * {@code liburStart <= sampai AND liburEnd >= tanggal}. Jam/menit/detik dibuang lebih dulu lewat
	 * {@link #truncateTime(Date)} pada kedua sisi sebelum dibandingkan.
	 *
	 * @param tanggal awal rentang yang diperiksa; {@code null} (atau {@code sampai} {@code null})
	 *                 langsung mengembalikan {@code null} (BUKAN list kosong)
	 * @param sampai   akhir rentang yang diperiksa
	 * @return list baris {@code LiburNasional} yang periodenya overlap dengan rentang tsb; list
	 *         kosong (bukan {@code null}) bila tidak ada yang cocok, {@code null} bila salah satu
	 *         parameter {@code null}
	 */
	public static List<LiburNasional> ambilLiburNasional(Date tanggal, Date sampai) {
		if (tanggal == null || sampai == null) {
			return null;
		}
		// Pastikan data master sudah ditarik dari database
		if (!isMasterDiinisialisasi) {
			reInitLiburNasional();
		}

		// Bersihkan jam/menit
		Date startClean = truncateTime(tanggal);
		Date endClean = truncateTime(sampai);

		List<LiburNasional> hasilPencarian = new ArrayList<LiburNasional>();

		// Looping pencarian data di dalam memori (List)
		for (LiburNasional libur : liburNasionalsMaster) {
			Date liburStart = truncateTime(libur.getTanggal());
			Date liburEnd = truncateTime(libur.getSampai());

			if (liburStart != null && liburEnd != null) {
				// Gunakan Logika Overlap Universal
				// Libur dimulai SEBELUM/SAMA DENGAN akhir pencarian AND Libur berakhir
				// SETELAH/SAMA DENGAN awal pencarian
				if (liburStart.compareTo(endClean) <= 0 && liburEnd.compareTo(startClean) >= 0) {
					hasilPencarian.add(libur);
				}
			}
		}

		return hasilPencarian;
	}

	/**
	 * Helper kecil pembanding tanggal murni (tanpa library eksternal semacam Joda-Time): mengembalikan
	 * salinan {@code date} dengan komponen jam/menit/detik/milidetik dinolkan, memakai
	 * {@link Calendar} default JVM (zona waktu &amp; locale sistem).
	 *
	 * @param date tanggal-waktu sumber; {@code null} mengembalikan {@code null}
	 * @return tanggal murni (00:00:00.000) hasil pemotongan komponen waktu dari {@code date}
	 */
	// Helper kecil jika tidak menggunakan library eksternal
	private static Date truncateTime(Date date) {
		if (date == null)
			return null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	/** Tanggal mulai periode libur ini (inklusif); lihat getter {@link #getTanggal()} untuk perilaku default saat {@code null}. */
	private Date tanggal;
	/** Tanggal akhir periode libur ini (inklusif); lihat getter {@link #getSampai()} untuk perilaku default saat {@code null}. */
	private Date sampai;
	/** Nama/label hari libur ini (mis. "Hari Kemerdekaan RI", "Cuti Bersama Idul Fitri"); juga dipakai {@link #toString()}. */
	private String nama;
	/** Tahun kalender periode libur ini; dihitung ulang dari {@link #tanggal} setiap dipanggil, lihat {@link #getTahun()} — field ini hanya cache hasil hitung terakhir. */
	private Integer tahun;
	/** Keterangan/catatan bebas mengenai hari libur ini. */
	private String keterangan;
	/** Penanda apakah tanggal ini dihitung sebagai ketidakhadiran bila pegawai absen pada hari libur ini; {@code null} diperlakukan sebagai {@code true}, lihat {@link #getDihitungKetidakhadiran()}. */
	private Boolean dihitungKetidakhadiran;
	/** Penanda manual "libur panjang"; lihat Javadoc lengkap pada getter {@link #getLiburPanjang()}. */
	private Boolean liburPanjang;

	/** Konstruktor default (dibutuhkan Hibernate); tidak menginisialisasi field apa pun secara eksplisit selain default deklarasi field. */
	public LiburNasional() {
	}

	/** @return {@link #id} — primary key baris libur nasional ini. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru; normalnya di-generate database, jarang di-set manual dari kode aplikasi. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return {@link #nama} — nama/label hari libur ini. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama;
	}

	/** @param nama nama/label hari libur baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return {@link #keterangan} — catatan bebas mengenai hari libur ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan bebas baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return {@link #tanggal} — tanggal mulai periode libur ini; bila belum pernah di-set
	 *         ({@code null}), method ini SEKALIGUS meng-inisialisasi field ke tanggal hari ini
	 *         ({@code ais.ui.util.WaktuUtil.getDate()}) sebagai efek samping sebelum
	 *         mengembalikannya — bukan getter murni.
	 */
	@Temporal(TemporalType.DATE)
	@Column(nullable = false)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	/** @param tanggal tanggal mulai periode libur baru. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * @return tahun kalender dari {@link #getTanggal()} (memakai {@code Calendar.YEAR} pada
	 *         kalender/locale sistem via {@code ais.ui.util.WaktuUtil.getCalendar()}) — nilai
	 *         SELALU dihitung ulang dari {@link #tanggal} setiap dipanggil (menimpa field
	 *         {@link #tahun} sebagai efek samping); nilai yang di-set manual lewat
	 *         {@link #setTahun(Integer)} akan tertimpa pada pemanggilan getter berikutnya.
	 */
	public Integer getTahun() {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(getTanggal());
		tahun = calendar.get(Calendar.YEAR);
		return tahun;
	}

	/** @param tahun nilai cache tahun; akan tertimpa pada pemanggilan {@link #getTahun()} berikutnya (lihat catatan pada getter). */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * @return {@link #sampai} — tanggal akhir periode libur ini; bila belum pernah di-set
	 *         ({@code null}), method ini SEKALIGUS meng-inisialisasi field ke nilai
	 *         {@link #getTanggal()} (menjadikan periode satu hari) sebagai efek samping sebelum
	 *         mengembalikannya — bukan getter murni.
	 */
	@Temporal(TemporalType.DATE)
	@Column(nullable = true)
	public Date getSampai() {
		if (sampai == null) {
			sampai = getTanggal();
		}
		return sampai;
	}

	/** @param sampai tanggal akhir periode libur baru. */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/** @return {@link #dihitungKetidakhadiran}, atau {@code true} bila belum pernah di-set (default aman: hari libur ini dihitung sebagai ketidakhadiran bila pegawai absen). */
	public Boolean getDihitungKetidakhadiran() {
		return dihitungKetidakhadiran == null ? true : dihitungKetidakhadiran;
	}

	/** @param dihitungKetidakhadiran nilai baru penanda perhitungan ketidakhadiran pada hari libur ini. */
	public void setDihitungKetidakhadiran(Boolean dihitungKetidakhadiran) {
		this.dihitungKetidakhadiran = dihitungKetidakhadiran;
	}

	/**
	 * Penanda MANUAL bahwa periode libur ini tergolong <b>libur panjang</b> (mis. Idul Fitri,
	 * Natal &amp; Tahun Baru). Dipakai aturan cuti: pengajuan tidak disetujui pada rentang H-x
	 * sebelum dan H+y sesudah libur panjang.
	 *
	 * <p>Bersifat PELENGKAP deteksi otomatis: secara default periode dianggap "panjang" bila
	 * rentang {@code tanggal..sampai} mencapai ambang hari tertentu (dapat diatur). Menandai
	 * kolom ini memaksa periode tsb dihitung sebagai libur panjang walau rentangnya pendek.
	 * Kolom aditif &amp; nullable; pembuatannya diserahkan ke Hibernate ({@code hbm2ddl.auto=update}).
	 * Karena entity ini {@code @Audited}, tabel audit {@code new_audit.libur_nasional__audit}
	 * diselaraskan oleh {@code ais.common.AturanCutiSchemaFix}.</p>
	 */
	@Column(name = "libur_panjang", nullable = true)
	public Boolean getLiburPanjang() {
		return liburPanjang == null ? false : liburPanjang;
	}

	/** @param liburPanjang penanda manual baru apakah periode libur ini tergolong libur panjang — lihat Javadoc pada {@link #getLiburPanjang()}. */
	public void setLiburPanjang(Boolean liburPanjang) {
		this.liburPanjang = liburPanjang;
	}
}
