package ais.action.master.payroll.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.payroll.LiburNasional;

/**
 * <h1>AturanCutiHelper — aturan pengajuan &amp; persetujuan cuti pegawai</h1>
 *
 * <p>Menerapkan aturan kepegawaian berikut pada modul <i>Pengajuan Izin &amp; Cuti Pegawai</i>:</p>
 * <ol>
 *   <li><b>Jatah tahunan</b> — pegawai yang telah bekerja {@code N} bulan berturut-turut (default 12)
 *       berhak atas {@code M} hari kerja cuti tahunan (default 12). Dipakai sebagai jatah DEFAULT bila
 *       pegawai belum punya baris {@code JatahCuti}.</li>
 *   <li><b>Batas waktu pengajuan</b> — pengajuan paling lambat {@code H-x} (default 2 hari) sebelum
 *       tanggal mulai cuti; selain itu ditolak.</li>
 *   <li><b>Blokir sekitar libur panjang</b> — pengajuan tidak disetujui bila jatuh pada rentang
 *       {@code H-7} sebelum sampai {@code H+14} sesudah libur panjang (angka dapat diatur).</li>
 *   <li><b>Cuti khusus berdurasi baku</b> — mis. menikah 7 hari, melahirkan 3 bulan. Durasi baku
 *       disimpan pada {@code Statusabsensi.durasiBakuHari} dan menjadi BATAS MAKSIMAL lama cuti.</li>
 * </ol>
 *
 * <p><b>PENTING — semua gerbang DEFAULT NONAKTIF.</b> Aturan 2, 3, dan 4 memblokir penyimpanan/
 * persetujuan dan bergantung pada data historis (tanggal masuk pegawai, kalender libur nasional)
 * yang mungkin belum lengkap di instalasi berjalan. Sesuai pelajaran insiden gerbang enforcement
 * sebelumnya, tiap gerbang memakai default {@link Konfigurasi#TIDAK_AKTIF} sehingga administrator
 * mengaktifkannya sendiri setelah datanya siap. Angka-angkanya tetap bisa disetel lebih dulu.</p>
 *
 * <p>Kompatibel Java 1.6/1.7 (tanpa lambda/stream) dan Hibernate 3.6.</p>
 */
public final class AturanCutiHelper {

	// ── Kunci konfigurasi (didaftarkan di KonfigurasiNewAction, tab Kepegawaian & Payroll) ──

	/** Gerbang aturan 2 — batas paling lambat pengajuan. */
	public static final String KEY_GERBANG_BATAS_PENGAJUAN = "cuti_gerbang_batas_pengajuan";
	/** Aturan 2 — minimal berapa hari sebelum tanggal mulai cuti. */
	public static final String KEY_MINIMAL_HARI_SEBELUM = "cuti_minimal_hari_sebelum_mulai";

	/** Gerbang aturan 3 — blokir persetujuan di sekitar libur panjang. */
	public static final String KEY_GERBANG_LIBUR_PANJANG = "cuti_gerbang_blokir_libur_panjang";
	/** Aturan 3 — ambang panjang rentang libur (hari) agar dianggap "libur panjang". */
	public static final String KEY_LIBUR_PANJANG_MINIMAL_HARI = "cuti_libur_panjang_minimal_hari";
	/** Aturan 3 — jumlah hari blokir SEBELUM libur panjang. */
	public static final String KEY_BLOKIR_SEBELUM = "cuti_blokir_hari_sebelum_libur_panjang";
	/** Aturan 3 — jumlah hari blokir SESUDAH libur panjang. */
	public static final String KEY_BLOKIR_SESUDAH = "cuti_blokir_hari_sesudah_libur_panjang";

	/** Gerbang aturan 4 — batasi lama cuti khusus sesuai durasi baku. */
	public static final String KEY_GERBANG_DURASI_BAKU = "cuti_gerbang_durasi_baku";

	/** Aturan 1 — minimal bulan kerja berturut-turut agar berhak cuti tahunan. */
	public static final String KEY_TAHUNAN_MINIMAL_BULAN_KERJA = "cuti_tahunan_minimal_bulan_kerja";
	/** Aturan 1 — jumlah hari kerja cuti tahunan. */
	public static final String KEY_TAHUNAN_JUMLAH_HARI = "cuti_tahunan_jumlah_hari";

	private AturanCutiHelper() {
	}

	// ── Pembacaan konfigurasi ───────────────────────────────────────────────────────────────

	/** Gerbang aktif? DEFAULT NONAKTIF (opt-in) — lihat catatan kelas. */
	public static boolean gerbangAktif(String kunci) {
		try {
			return Common.bolehKonfigurasi(kunci, Konfigurasi.TIDAK_AKTIF);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/payroll/helper/AturanCutiHelper.java:gerbangAktif " + kunci);
			return false;
		}
	}

	/** Ambil nilai konfigurasi berupa angka; kembali ke {@code standar} bila kosong/tak valid. */
	public static int nilaiAngka(String kunci, int standar) {
		try {
			Konfigurasi k = Common.getKonfigurasi(kunci, String.valueOf(standar));
			if (k == null || k.getNilai() == null || k.getNilai().trim().length() == 0) {
				return standar;
			}
			return Integer.parseInt(k.getNilai().trim());
		} catch (Exception e) {
			return standar;
		}
	}

	// ── Aturan 1: jatah cuti tahunan berdasarkan masa kerja ─────────────────────────────────

	/**
	 * Jatah cuti tahunan DEFAULT menurut aturan: berhak {@code M} hari bila masa kerja sudah
	 * mencapai {@code N} bulan berturut-turut. Dipakai hanya sebagai cadangan ketika pegawai belum
	 * memiliki baris {@code JatahCuti} sendiri (baris JatahCuti tetap diprioritaskan).
	 *
	 * @return jumlah hari jatah (0 bila masa kerja belum memenuhi)
	 */
	public static int jatahTahunanMenurutMasaKerja(Pegawai pegawai, Date perTanggal) {
		int minimalBulan = nilaiAngka(KEY_TAHUNAN_MINIMAL_BULAN_KERJA, 12);
		int jumlahHari = nilaiAngka(KEY_TAHUNAN_JUMLAH_HARI, 12);
		int totalBulan = totalBulanKerja(pegawai, perTanggal);
		if (totalBulan < minimalBulan) {
			return 0;
		}
		return jumlahHari;
	}

	/** Total masa kerja (bulan) pegawai per tanggal tertentu. 0 bila data tanggal masuk kosong. */
	public static int totalBulanKerja(Pegawai pegawai, Date perTanggal) {
		try {
			if (pegawai == null || pegawai.getTanggalmasuk() == null) {
				return 0;
			}
			Date sampai = perTanggal == null ? ais.ui.util.WaktuUtil.getDate() : perTanggal;
			int thn = pegawai.ambilMasaKerjaTahun(pegawai.getTanggalmasuk(), sampai);
			int bln = pegawai.ambilMasaKerjaBulan(pegawai.getTanggalmasuk(), sampai);
			return (thn * 12) + bln;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/payroll/helper/AturanCutiHelper.java:totalBulanKerja");
			return 0;
		}
	}

	// ── Aturan 2: batas paling lambat pengajuan ─────────────────────────────────────────────

	/**
	 * Pengajuan paling lambat {@code H-x} sebelum tanggal mulai.
	 *
	 * @return pesan penolakan bila melanggar, atau {@code null} bila lolos / gerbang nonaktif
	 */
	public static String validasiBatasPengajuan(Date mulai) {
		if (!gerbangAktif(KEY_GERBANG_BATAS_PENGAJUAN) || mulai == null) {
			return null;
		}
		int minimalHari = nilaiAngka(KEY_MINIMAL_HARI_SEBELUM, 2);
		if (minimalHari <= 0) {
			return null;
		}
		Date hariIni = awalHari(ais.ui.util.WaktuUtil.getDate());
		Date batasPalingLambat = tambahHari(awalHari(mulai), -minimalHari);
		if (hariIni.after(batasPalingLambat)) {
			return "Pengajuan cuti paling lambat " + minimalHari + " hari sebelum tanggal mulai cuti. "
					+ "Untuk cuti mulai " + tanggalIndo(mulai) + ", pengajuan paling lambat "
					+ tanggalIndo(batasPalingLambat) + ". Langkah yang dapat dilakukan: "
					+ "(1) majukan tanggal pengajuan; atau (2) pilih tanggal mulai cuti yang lebih jauh; "
					+ "atau (3) hubungi admin kepegawaian bila ada keadaan mendesak.";
		}
		return null;
	}

	// ── Aturan 3: blokir persetujuan di sekitar libur panjang ───────────────────────────────

	/**
	 * Cuti tidak boleh disetujui bila bersinggungan dengan rentang {@code H-x} sebelum sampai
	 * {@code H+y} sesudah sebuah <b>libur panjang</b>.
	 *
	 * <p>Sebuah periode {@link LiburNasional} dianggap "libur panjang" bila ditandai manual
	 * ({@code liburPanjang = true}) ATAU rentang {@code tanggal..sampai} mencapai ambang hari.</p>
	 *
	 * @return pesan penolakan bila melanggar, atau {@code null} bila lolos / gerbang nonaktif
	 */
	public static String validasiLiburPanjang(Date mulai, Date sampai) {
		if (!gerbangAktif(KEY_GERBANG_LIBUR_PANJANG) || mulai == null) {
			return null;
		}
		int ambangHari = nilaiAngka(KEY_LIBUR_PANJANG_MINIMAL_HARI, 3);
		int hSebelum = nilaiAngka(KEY_BLOKIR_SEBELUM, 7);
		int hSesudah = nilaiAngka(KEY_BLOKIR_SESUDAH, 14);

		Date cutiAwal = awalHari(mulai);
		Date cutiAkhir = awalHari(sampai == null ? mulai : sampai);

		List<LiburNasional> daftar = ambilLiburSekitar(cutiAwal, cutiAkhir, hSebelum, hSesudah);
		if (daftar == null || daftar.isEmpty()) {
			return null;
		}
		for (int i = 0; i < daftar.size(); i++) {
			LiburNasional libur = daftar.get(i);
			if (libur == null || libur.getTanggal() == null) {
				continue;
			}
			if (!apakahLiburPanjang(libur, ambangHari)) {
				continue;
			}
			Date liburAwal = awalHari(libur.getTanggal());
			Date liburAkhir = awalHari(libur.getSampai() == null ? libur.getTanggal() : libur.getSampai());
			Date blokirAwal = tambahHari(liburAwal, -hSebelum);
			Date blokirAkhir = tambahHari(liburAkhir, hSesudah);

			// Bersinggungan bila rentang cuti dan rentang blokir saling tumpang tindih.
			if (!cutiAkhir.before(blokirAwal) && !cutiAwal.after(blokirAkhir)) {
				String namaLibur = libur.getNama() == null ? "libur panjang" : libur.getNama().trim();
				return "Cuti tidak dapat disetujui karena berada dalam masa " + hSebelum + " hari sebelum sampai "
						+ hSesudah + " hari sesudah libur panjang \"" + namaLibur + "\" ("
						+ tanggalIndo(liburAwal) + (liburAkhir.equals(liburAwal) ? "" : " s.d " + tanggalIndo(liburAkhir))
						+ "). Masa yang diblokir: " + tanggalIndo(blokirAwal) + " s.d " + tanggalIndo(blokirAkhir)
						+ ". Langkah yang dapat dilakukan: (1) ajukan cuti di luar rentang tersebut; atau "
						+ "(2) hubungi admin kepegawaian bila ada kebijakan pengecualian.";
			}
		}
		return null;
	}

	/** Periode libur tergolong panjang: ditandai manual ATAU rentangnya mencapai ambang hari. */
	public static boolean apakahLiburPanjang(LiburNasional libur, int ambangHari) {
		if (libur == null || libur.getTanggal() == null) {
			return false;
		}
		if (Boolean.TRUE.equals(libur.getLiburPanjang())) {
			return true; // ditandai manual oleh admin
		}
		if (ambangHari <= 1) {
			return true;
		}
		Date awal = awalHari(libur.getTanggal());
		Date akhir = awalHari(libur.getSampai() == null ? libur.getTanggal() : libur.getSampai());
		long selisihHari = (akhir.getTime() - awal.getTime()) / (24L * 60L * 60L * 1000L);
		return (selisihHari + 1) >= ambangHari;
	}

	/** Ambil libur nasional yang mungkin bersinggungan dengan rentang cuti + margin blokir. */
	@SuppressWarnings("unchecked")
	private static List<LiburNasional> ambilLiburSekitar(Date cutiAwal, Date cutiAkhir, int hSebelum, int hSesudah) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			// Perlebar jendela pencarian: libur yang jauh pun bisa memblokir lewat margin H+/H-.
			Date batasBawah = tambahHari(cutiAwal, -(hSesudah + 400));
			Date batasAtas = tambahHari(cutiAkhir, (hSebelum + 400));
			return session.createCriteria(LiburNasional.class).add(Restrictions.isNotNull("tanggal"))
					.add(Restrictions.ge("tanggal", batasBawah)).add(Restrictions.le("tanggal", batasAtas)).list();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/action/master/payroll/helper/AturanCutiHelper.java:ambilLiburSekitar");
			return null;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ── Aturan 4: cuti khusus berdurasi baku ────────────────────────────────────────────────

	/** Durasi baku (hari) untuk status izin tertentu; {@code null} bila tidak diatur. */
	public static Integer durasiBaku(Statusabsensi statusabsensi) {
		if (statusabsensi == null) {
			return null;
		}
		Integer durasi = statusabsensi.getDurasiBakuHari();
		if (durasi == null || durasi.intValue() <= 0) {
			return null;
		}
		return durasi;
	}

	/**
	 * Lama cuti tidak boleh melebihi durasi baku status izin (bila diatur).
	 *
	 * @return pesan penolakan bila melanggar, atau {@code null} bila lolos / gerbang nonaktif
	 */
	public static String validasiDurasiBaku(Statusabsensi statusabsensi, int jumlahHariDiambil) {
		if (!gerbangAktif(KEY_GERBANG_DURASI_BAKU)) {
			return null;
		}
		Integer durasi = durasiBaku(statusabsensi);
		if (durasi == null) {
			return null;
		}
		if (jumlahHariDiambil > durasi.intValue()) {
			String namaStatus = statusabsensi.getNama() == null ? "cuti khusus" : statusabsensi.getNama().trim();
			return "Lama cuti \"" + namaStatus + "\" paling banyak " + durasi + " hari sesuai ketentuan, "
					+ "sedangkan yang diajukan " + jumlahHariDiambil + " hari. "
					+ "Langkah yang dapat dilakukan: (1) persingkat rentang tanggal cuti; atau "
					+ "(2) centang tanggal yang dikecualikan; atau (3) hubungi admin kepegawaian.";
		}
		return null;
	}

	// ── Util tanggal ────────────────────────────────────────────────────────────────────────

	/** Tanggal pada jam 00:00 (buang komponen waktu agar perbandingan hari akurat). */
	public static Date awalHari(Date tanggal) {
		if (tanggal == null) {
			return null;
		}
		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		cal.setTime(tanggal);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	/** Geser tanggal sebanyak {@code jumlah} hari (boleh negatif). */
	public static Date tambahHari(Date tanggal, int jumlah) {
		if (tanggal == null) {
			return null;
		}
		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		cal.setTime(tanggal);
		cal.add(Calendar.DATE, jumlah);
		return cal.getTime();
	}

	private static String tanggalIndo(Date tanggal) {
		try {
			return tanggal == null ? "-" : Common.dateFormat1.get().format(tanggal);
		} catch (Exception e) {
			return String.valueOf(tanggal);
		}
	}
}
