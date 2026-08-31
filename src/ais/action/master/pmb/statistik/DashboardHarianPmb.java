package ais.action.master.pmb.statistik;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import ais.database.model.PerguruanTinggi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyHtml;

/**
 * <h1>DashboardHarianPmb — Statistik Pendaftar Harian (Real-time)</h1>
 *
 * <p>Memberikan gambaran <em>terkini</em> tentang laju pendaftaran calon mahasiswa
 * dari hari ke hari. Berbeda dengan dasbor lain yang fokus ke akumulasi total,
 * dasbor ini fokus ke <b>kecepatan dan ritme</b> pendaftaran.</p>
 *
 * <p>Informasi yang disajikan:</p>
 * <ul>
 *   <li><b>Hari ini</b> — berapa pendaftar yang masuk hari ini (real-time).</li>
 *   <li><b>Kemarin</b> — perbandingan langsung dengan hari sebelumnya.</li>
 *   <li><b>Rata-rata 7 hari</b> — baseline laju normal dalam seminggu terakhir.</li>
 *   <li><b>Tren 30 hari</b> — grafik garis yang menampilkan fluktuasi pendaftaran
 *       selama sebulan. Lonjakan tinggi biasanya bertepatan dengan promosi atau
 *       mendekati penutupan gelombang.</li>
 *   <li><b>Minggu ini vs minggu lalu</b> — perbandingan batang per hari-dalam-minggu
 *       untuk melihat pola (misalnya: Senin selalu lebih ramai dari Jumat).</li>
 * </ul>
 *
 * <p>Semua data diambil dari {@code BiodataCalonMahasiswa.tanggalDaftar} dengan
 * filter tahun akademik. Filter semester tidak berpengaruh ke tampilan tren harian
 * karena {@code semesterMulai} dan {@code tanggalDaftar} adalah dua dimensi berbeda.</p>
 *
 * @see DashboardPmbBase
 */
public class DashboardHarianPmb extends DashboardPmbBase {

	// ═══════════════════════════════════════════════════════════════
	// Inner data class
	// ═══════════════════════════════════════════════════════════════

	/**
	 * Tipe implementasi bersarang {@link HariData} milik {@link DashboardHarianPmb}. Kelas ini memberi nama pada
	 * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link DashboardHarianPmb}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
	 * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code int jumlah}.
	 * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see DashboardHarianPmb
	 */
	private static final class HariData {
		String label; // format "DD/MM"
		int    jumlah;
		HariData(String l, int j) { label = l; jumlah = j; }
	}

	// ═══════════════════════════════════════════════════════════════
	// Konstanta
	// ═══════════════════════════════════════════════════════════════

	private static final int WINDOW_DAYS = 30;
	private static final String[] NAMA_HARI = {
			"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"
	};

	// ═══════════════════════════════════════════════════════════════
	// Konstruktor
	// ═══════════════════════════════════════════════════════════════

	public DashboardHarianPmb(PerguruanTinggi pt) {
		super(pt);
	}

	// ═══════════════════════════════════════════════════════════════
	// doRefresh
	// ═══════════════════════════════════════════════════════════════

	@Override
	protected void doRefresh(Session session, String ta, String sem) {
		Date now = new Date();

		// Batas hari ini
		Calendar calStart = Calendar.getInstance();
		calStart.setTime(now);
		calStart.set(Calendar.HOUR_OF_DAY, 0);
		calStart.set(Calendar.MINUTE, 0);
		calStart.set(Calendar.SECOND, 0);
		calStart.set(Calendar.MILLISECOND, 0);
		Date todayStart = calStart.getTime();

		calStart.add(Calendar.DAY_OF_YEAR, 1);
		Date todayEnd = calStart.getTime();

		// Kemarin
		Calendar calYest = Calendar.getInstance();
		calYest.setTime(now);
		calYest.set(Calendar.HOUR_OF_DAY, 0); calYest.set(Calendar.MINUTE, 0);
		calYest.set(Calendar.SECOND, 0); calYest.set(Calendar.MILLISECOND, 0);
		calYest.add(Calendar.DAY_OF_YEAR, -1);
		Date yesterdayStart = calYest.getTime();

		// 30 hari lalu
		Calendar calWindow = Calendar.getInstance();
		calWindow.setTime(now);
		calWindow.set(Calendar.HOUR_OF_DAY, 0); calWindow.set(Calendar.MINUTE, 0);
		calWindow.set(Calendar.SECOND, 0); calWindow.set(Calendar.MILLISECOND, 0);
		calWindow.add(Calendar.DAY_OF_YEAR, -(WINDOW_DAYS - 1));
		Date windowStart = calWindow.getTime();

		// Awal minggu ini (Senin)
		Calendar calWeek = Calendar.getInstance();
		calWeek.setTime(now);
		calWeek.set(Calendar.HOUR_OF_DAY, 0); calWeek.set(Calendar.MINUTE, 0);
		calWeek.set(Calendar.SECOND, 0); calWeek.set(Calendar.MILLISECOND, 0);
		int dow = calWeek.get(Calendar.DAY_OF_WEEK);
		int toMon = (dow == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dow);
		calWeek.add(Calendar.DAY_OF_YEAR, toMon);
		Date thisWeekStart = calWeek.getTime();
		calWeek.add(Calendar.WEEK_OF_YEAR, -1);
		Date lastWeekStart = calWeek.getTime();

		// Queries
		int hari_ini   = queryCountBetween(session, ta, todayStart,     todayEnd);
		int kemarin    = queryCountBetween(session, ta, yesterdayStart,  todayStart);
		int total7hari = queryCountBetween(session, ta, shiftDays(now, -(7)), todayEnd);
		double rata7   = total7hari / 7.0;

		List<HariData> tren30 = queryTren(session, ta, windowStart, todayEnd);
		int[][] weeklyData    = queryWeekly(session, ta, lastWeekStart, thisWeekStart, todayEnd);

		// Total dalam TA
		int totalTA = queryCount(session, ta, null, null);

		StringBuilder sb = new StringBuilder(5120);

		// Header
		sb.append("<div style=\"margin-bottom:14px;\">")
		  .append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
		  .append("Statistik Pendaftar Harian &#8212; ").append(escHtml(ta)).append("</div>")
		  .append("<div style=\"font-size:12px;color:#9ca3af;\">")
		  .append("Laju pendaftaran real-time: hari ini, kemarin, tren 30 hari, dan perbandingan minggu ini vs minggu lalu.")
		  .append("</div></div>");

		// Tanggal hari ini
		sb.append("<div style=\"font-size:11px;color:#6b7280;margin-bottom:10px;\">")
		  .append("Data per: ").append(new SimpleDateFormat("dd MMMM yyyy HH:mm").format(now))
		  .append("</div>");

		// KPI
		double delta = kemarin > 0 ? ((hari_ini - kemarin) * 100.0 / kemarin) : 0;
		String deltaText = kemarin > 0
				? (delta >= 0 ? "+" : "") + fmt1(delta) + "% vs kemarin" : "";
		sb.append(HtmlChartHelper.kpiCards(
				new String[] { "Hari Ini", "Kemarin", "Total 7 Hari", "Rata-rata/Hari", "Total Dalam TA" },
				new String[] { fmtAngka(hari_ini), fmtAngka(kemarin),
						fmtAngka(total7hari), fmt1(rata7), fmtAngka(totalTA) },
				new String[] { "", "", "7 hari terakhir", "dari 7 hari terakhir", "" },
				new String[] { deltaText, "", "", "", "" },
				new boolean[] { hari_ini >= kemarin, true, true, true, true },
				new String[] { "#1877f2", "#6b7280", "#059669", "#7c3aed", "#f59e0b" }));

		// Tren 30 hari (line chart)
		if (!tren30.isEmpty()) {
			int nt = tren30.size();
			String[] cats   = new String[nt];
			double[][] vals = new double[1][nt];
			for (int i = 0; i < nt; i++) {
				cats[i]    = tren30.get(i).label;
				vals[0][i] = tren30.get(i).jumlah;
			}
			sb.append(fullWidth(HtmlChartHelper.lineMulti(
					"Tren Pendaftar " + WINDOW_DAYS + " Hari Terakhir",
					"Jumlah pendaftar baru tiap hari selama 30 hari terakhir. "
					+ "Lonjakan tiba-tiba biasanya bersamaan dengan kampanye promosi "
					+ "atau mendekati batas akhir pendaftaran gelombang.",
					cats,
					new String[] { "Pendaftar per hari" },
					vals,
					new String[] { "#2563eb" })));
		}

		// Minggu ini vs minggu lalu
		if (weeklyData != null) {
			// weeklyData[0] = last week, weeklyData[1] = this week; 7 elemen (Mon-Sun)
			String[] dayCats  = { "Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min" };
			double[][] wVals  = new double[7][2];
			for (int d = 0; d < 7; d++) {
				wVals[d][0] = weeklyData[0][d]; // minggu lalu
				wVals[d][1] = weeklyData[1][d]; // minggu ini
			}
			sb.append(fullWidth(HtmlChartHelper.barVerticalGrouped(
					"Pendaftar Harian — Minggu Ini vs Minggu Lalu",
					"Perbandingan jumlah pendaftar per hari antara minggu ini dan minggu lalu. "
					+ "Pola ini membantu prediksi hari-hari tersibuk untuk penjadwalan staf PMB.",
					dayCats,
					new String[] { "Minggu Lalu", "Minggu Ini" },
					wVals,
					new String[] { "#93c5fd", "#2563eb" })));
		}

		new MyHtml(sb.toString()).setParent(contentHolder);
	}

	// ═══════════════════════════════════════════════════════════════
	// Query methods
	// ═══════════════════════════════════════════════════════════════

	/** Jumlah pendaftar dalam rentang tanggal tertentu. */
	@SuppressWarnings("unchecked")
	private int queryCountBetween(Session session, String ta, Date from, Date to) {
		try {
			String hql = "SELECT COUNT(bcm) FROM BiodataCalonMahasiswa bcm "
					+ "WHERE bcm.tahunAkademik = :ta " + jenjangClause()
					+ "AND bcm.tanggalDaftar >= :from AND bcm.tanggalDaftar < :to";
			org.hibernate.Query q = session.createQuery(hql)
					.setParameter("ta", ta)
					.setParameter("from", from)
					.setParameter("to", to);
			applyJenjangParam(q);
			Number n = (Number) q.uniqueResult();
			return n == null ? 0 : n.intValue();
		} catch (Exception e) {
			logErr("queryCountBetween", e);
			return 0;
		}
	}

	/** Jumlah pendaftar total dalam TA (tanpa batas tanggal). */
	@SuppressWarnings("unchecked")
	private int queryCount(Session session, String ta, Date from, Date to) {
		try {
			String hql = "SELECT COUNT(bcm) FROM BiodataCalonMahasiswa bcm "
					+ "WHERE bcm.tahunAkademik = :ta " + jenjangClause();
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta);
			applyJenjangParam(q);
			Number n = (Number) q.uniqueResult();
			return n == null ? 0 : n.intValue();
		} catch (Exception e) {
			logErr("queryCount ta=" + ta, e);
			return 0;
		}
	}

	/**
	 * Tren harian selama 30 hari terakhir.
	 * Membangun map tanggal → jumlah, lalu mengisi 0 untuk hari tanpa pendaftar.
	 */
	@SuppressWarnings("unchecked")
	private List<HariData> queryTren(Session session, String ta, Date from, Date to) {
		List<HariData> result = new ArrayList<HariData>();
		try {
			// Query per-hari dari DB
			String hql = "SELECT YEAR(bcm.tanggalDaftar), MONTH(bcm.tanggalDaftar), "
					+ "DAY(bcm.tanggalDaftar), COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm "
					+ "WHERE bcm.tahunAkademik = :ta " + jenjangClause()
					+ "AND bcm.tanggalDaftar >= :from AND bcm.tanggalDaftar < :to "
					+ "GROUP BY YEAR(bcm.tanggalDaftar), MONTH(bcm.tanggalDaftar), DAY(bcm.tanggalDaftar) "
					+ "ORDER BY YEAR(bcm.tanggalDaftar), MONTH(bcm.tanggalDaftar), DAY(bcm.tanggalDaftar)";
			org.hibernate.Query q = session.createQuery(hql)
					.setParameter("ta", ta)
					.setParameter("from", from)
					.setParameter("to", to);
			applyJenjangParam(q);
			List<?> rows = q.list();

			// Bangun map "yyyy-MM-dd" → count
			Map<String, Integer> dbMap = new LinkedHashMap<String, Integer>();
			for (Object obj : rows) {
				Object[] r = (Object[]) obj;
				int yr  = r[0] == null ? 0 : ((Number) r[0]).intValue();
				int mo  = r[1] == null ? 0 : ((Number) r[1]).intValue();
				int dy  = r[2] == null ? 0 : ((Number) r[2]).intValue();
				int cnt = r[3] == null ? 0 : ((Number) r[3]).intValue();
				if (yr > 0 && mo > 0 && dy > 0) {
					String key = String.format("%04d-%02d-%02d", yr, mo, dy);
					dbMap.put(key, cnt);
				}
			}

			// Isi semua hari dalam window (termasuk yang 0)
			Calendar cal = Calendar.getInstance();
			cal.setTime(from);
			SimpleDateFormat sdfKey = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat sdfLbl = new SimpleDateFormat("dd/MM");
			while (!cal.getTime().after(to)) {
				Date d = cal.getTime();
				String key = sdfKey.format(d);
				String lbl = sdfLbl.format(d);
				int cnt    = dbMap.containsKey(key) ? dbMap.get(key) : 0;
				result.add(new HariData(lbl, cnt));
				cal.add(Calendar.DAY_OF_YEAR, 1);
			}
		} catch (Exception e) {
			logErr("queryTren ta=" + ta, e);
		}
		return result;
	}

	/**
	 * Menghitung jumlah pendaftar per hari-dalam-minggu untuk dua minggu:
	 * minggu lalu dan minggu ini. Mengembalikan int[2][7]:
	 * [0] = minggu lalu (Mon-Sun), [1] = minggu ini.
	 */
	@SuppressWarnings("unchecked")
	private int[][] queryWeekly(Session session, String ta,
			Date lastWeekStart, Date thisWeekStart, Date thisWeekEnd) {
		int[][] result = new int[2][7]; // [minggu][hari 0=Mon..6=Sun]
		try {
			// Minggu lalu
			String hql = "SELECT YEAR(bcm.tanggalDaftar), MONTH(bcm.tanggalDaftar), "
					+ "DAY(bcm.tanggalDaftar), COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm "
					+ "WHERE bcm.tahunAkademik = :ta " + jenjangClause()
					+ "AND bcm.tanggalDaftar >= :from AND bcm.tanggalDaftar < :to "
					+ "GROUP BY YEAR(bcm.tanggalDaftar), MONTH(bcm.tanggalDaftar), DAY(bcm.tanggalDaftar)";

			for (int week = 0; week < 2; week++) {
				Date wStart = week == 0 ? lastWeekStart : thisWeekStart;
				Date wEnd   = week == 0 ? thisWeekStart : thisWeekEnd;

				org.hibernate.Query q = session.createQuery(hql)
						.setParameter("ta", ta)
						.setParameter("from", wStart)
						.setParameter("to", wEnd);
				applyJenjangParam(q);
				List<?> rows = q.list();

				for (Object obj : rows) {
					Object[] r  = (Object[]) obj;
					int yr  = r[0] == null ? 0 : ((Number) r[0]).intValue();
					int mo  = r[1] == null ? 0 : ((Number) r[1]).intValue();
					int dy  = r[2] == null ? 0 : ((Number) r[2]).intValue();
					int cnt = r[3] == null ? 0 : ((Number) r[3]).intValue();
					if (yr <= 0 || mo <= 0 || dy <= 0) { continue; }

					Calendar c = Calendar.getInstance();
					c.set(yr, mo - 1, dy);
					int javaDoW = c.get(Calendar.DAY_OF_WEEK); // 1=Sun, 2=Mon...7=Sat
					// Konversi ke 0=Mon..6=Sun
					int idx = (javaDoW == Calendar.SUNDAY) ? 6 : (javaDoW - Calendar.MONDAY);
					if (idx >= 0 && idx < 7) { result[week][idx] += cnt; }
				}
			}
		} catch (Exception e) {
			logErr("queryWeekly ta=" + ta, e);
		}
		return result;
	}

	// ═══════════════════════════════════════════════════════════════
	// Utility
	// ═══════════════════════════════════════════════════════════════

	/** Geser tanggal sejumlah hari. Positif = masa depan, negatif = masa lalu. */
	private static Date shiftDays(Date base, int days) {
		Calendar c = Calendar.getInstance();
		c.setTime(base);
		c.add(Calendar.DAY_OF_YEAR, days);
		return c.getTime();
	}
}
