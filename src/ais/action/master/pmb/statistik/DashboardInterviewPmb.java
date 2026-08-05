package ais.action.master.pmb.statistik;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zul.Tabpanel;

import ais.database.model.PerguruanTinggi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyHtml;

/**
 * <h1>DashboardInterviewPmb — Pantauan Seleksi &amp; Interview</h1>
 *
 * <p>Menampilkan ringkasan proses interview calon mahasiswa dari tiga sudut pandang:</p>
 * <ol>
 *   <li><b>Cakupan penjadwalan</b> — berapa calon sudah mendapat jadwal vs yang belum,
 *       sehingga panitia tahu apakah proses penjadwalan sudah selesai.</li>
 *   <li><b>Tingkat kehadiran</b> — dari yang sudah dijadwalkan, berapa yang benar-benar
 *       hadir (konfirmasi {@code siap=true}). Angka ini mencerminkan keseriusan calon.</li>
 *   <li><b>Beban pewawancara</b> — berapa calon yang harus ditangani oleh tiap pewawancara.
 *       Berguna untuk pemerataan beban kerja.</li>
 *   <li><b>Distribusi per program studi</b> — prodi mana yang paling banyak peserta
 *       interviewnya, berguna untuk alokasi ruang dan jadwal.</li>
 * </ol>
 *
 * <p>Data diambil dari entity {@code InterviewPunyaCalonMahasiswa} yang menghubungkan
 * setiap jadwal interview ({@code InterviewCalonMahasiswa}) dengan calon mahasiswa
 * ({@code BiodataCalonMahasiswa}). Filter tahun akademik diterapkan lewat
 * {@code InterviewCalonMahasiswa.tahunAkademik}.</p>
 *
 * @see DashboardPmbBase
 */
public class DashboardInterviewPmb extends DashboardPmbBase {

	// ═══════════════════════════════════════════════════════════════
	// Inner data classes
	// ═══════════════════════════════════════════════════════════════

	private static final class KpiInterview {
		int terjadwal;
		int hadir;

		KpiInterview(int t, int h) { terjadwal = t; hadir = h; }
	}

	private static final class PewawancaraData {
		String nama;
		int    jumlah;
		PewawancaraData(String n, int j) { nama = n; jumlah = j; }
	}

	private static final class ProdiInterviewData {
		String nama;
		int    jumlah;
		ProdiInterviewData(String n, int j) { nama = n; jumlah = j; }
	}

	// ═══════════════════════════════════════════════════════════════
	// Konstanta
	// ═══════════════════════════════════════════════════════════════

	private static final int MAX_PEWAWANCARA = 8;
	private static final int MAX_PRODI       = 8;

	// ═══════════════════════════════════════════════════════════════
	// Konstruktor
	// ═══════════════════════════════════════════════════════════════

	/**
	 * @param pt perguruan tinggi konteks; null = semua PT.
	 */
	public DashboardInterviewPmb(PerguruanTinggi pt) {
		super(pt);
	}

	// ═══════════════════════════════════════════════════════════════
	// doRefresh
	// ═══════════════════════════════════════════════════════════════

	@Override
	protected void doRefresh(Session session, String ta, String sem) {
		KpiInterview kpi              = queryKpi(session, ta, sem);
		List<PewawancaraData> pewList  = queryPerPewawancara(session, ta, sem);
		List<ProdiInterviewData> prList = queryPerProdi(session, ta, sem);

		StringBuilder sb = new StringBuilder(4096);

		// Header
		sb.append("<div style=\"margin-bottom:14px;\">")
		  .append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
		  .append("Pantauan Interview &amp; Seleksi &#8212; ").append(escHtml(ta)).append("</div>")
		  .append("<div style=\"font-size:12px;color:#9ca3af;\">")
		  .append("Status penjadwalan dan kehadiran calon mahasiswa dalam proses seleksi/interview.")
		  .append("</div></div>");

		// KPI
		int belum = kpi.terjadwal - kpi.hadir;
		double rate = kpi.terjadwal > 0 ? (kpi.hadir * 100.0 / kpi.terjadwal) : 0;
		sb.append(HtmlChartHelper.kpiCards(
				new String[] { "Terjadwal Interview", "Sudah Hadir", "Belum/Tidak Hadir", "Tingkat Kehadiran" },
				new String[] { fmtAngka(kpi.terjadwal), fmtAngka(kpi.hadir), fmtAngka(belum), fmt1(rate) + "%" },
				new String[] { "", "", "", "" },
				new String[] { "", "", "", "" },
				new boolean[] { true, true, false, rate >= 75 },
				new String[] { "#1877f2", "#059669", "#dc2626", "#7c3aed" }));

		// Donut kehadiran + Bar pewawancara
		String donutHadir = kpi.terjadwal > 0
				? HtmlChartHelper.donut(
						"Status Kehadiran",
						"Perbandingan yang sudah hadir dan yang belum hadir dari total yang dijadwalkan.",
						new String[] { "Sudah Hadir", "Belum/Tidak Hadir" },
						new double[] { kpi.hadir, belum },
						new String[] { "#059669", "#ef4444" },
						fmt1(rate) + "%\nkehadiran")
				: emptyCard("Status Kehadiran", "Belum ada jadwal interview untuk periode ini.");

		String barPewawancara;
		if (pewList.isEmpty()) {
			barPewawancara = emptyCard("Beban Per Pewawancara",
					"Belum ada data pewawancara untuk periode ini.");
		} else {
			int n = pewList.size();
			String[] lbl = new String[n];
			double[] val = new double[n];
			for (int i = 0; i < n; i++) {
				lbl[i] = pewList.get(i).nama;
				val[i] = pewList.get(i).jumlah;
			}
			barPewawancara = HtmlChartHelper.barHorizontal(
					"Jumlah Calon Per Pewawancara",
					"Berapa calon yang harus diwawancarai oleh tiap pewawancara. "
					+ "Berguna untuk memastikan beban kerja terdistribusi merata.",
					lbl, val, "#7c3aed");
		}
		sb.append(grid2col(donutHadir, barPewawancara));

		// Bar per prodi
		if (!prList.isEmpty()) {
			int n = prList.size();
			String[] lbl = new String[n];
			double[] val = new double[n];
			for (int i = 0; i < n; i++) {
				lbl[i] = prList.get(i).nama;
				val[i] = prList.get(i).jumlah;
			}
			sb.append(fullWidth(HtmlChartHelper.barHorizontal(
					"Peserta Interview Per Program Studi",
					"Berapa calon yang mengikuti interview di setiap program studi. "
					+ "Prodi dengan banyak peserta mungkin perlu ruang atau jadwal tambahan.",
					lbl, val, "#2563eb")));
		}

		new MyHtml(sb.toString()).setParent(contentHolder);
	}

	// ═══════════════════════════════════════════════════════════════
	// Query methods
	// ═══════════════════════════════════════════════════════════════

	@SuppressWarnings("unchecked")
	private KpiInterview queryKpi(Session session, String ta, String sem) {
		try {
			String joinSem = hasSem(sem)
					? "JOIN ipcm.biodataCalonMahasiswa bcm "
					: "";
			String whereSem = hasSem(sem)
					? "AND bcm.semesterMulai = :sem "
					: "";

			String hqlTotal = "SELECT COUNT(ipcm) "
					+ "FROM InterviewPunyaCalonMahasiswa ipcm "
					+ "JOIN ipcm.interviewCalonMahasiswa icm "
					+ joinSem
					+ "WHERE icm.tahunAkademik = :ta " + whereSem;
			org.hibernate.Query qTotal = session.createQuery(hqlTotal)
					.setParameter("ta", ta);
			if (hasSem(sem)) { qTotal.setParameter("sem", sem); }
			int total = ((Number) qTotal.uniqueResult()).intValue();

			String hqlHadir = "SELECT COUNT(ipcm) "
					+ "FROM InterviewPunyaCalonMahasiswa ipcm "
					+ "JOIN ipcm.interviewCalonMahasiswa icm "
					+ joinSem
					+ "WHERE icm.tahunAkademik = :ta AND ipcm.siap = true " + whereSem;
			org.hibernate.Query qHadir = session.createQuery(hqlHadir)
					.setParameter("ta", ta);
			if (hasSem(sem)) { qHadir.setParameter("sem", sem); }
			int hadir = ((Number) qHadir.uniqueResult()).intValue();

			return new KpiInterview(total, hadir);
		} catch (Exception e) {
			logErr("queryKpi ta=" + ta, e);
			return new KpiInterview(0, 0);
		}
	}

	@SuppressWarnings("unchecked")
	private List<PewawancaraData> queryPerPewawancara(Session session, String ta, String sem) {
		List<PewawancaraData> result = new ArrayList<PewawancaraData>();
		try {
			String joinSem = hasSem(sem) ? "JOIN ipcm.biodataCalonMahasiswa bcm " : "";
			String whereSem = hasSem(sem) ? "AND bcm.semesterMulai = :sem " : "";

			String hql = "SELECT p.nama, COUNT(ipcm) "
					+ "FROM InterviewPunyaCalonMahasiswa ipcm "
					+ "JOIN ipcm.interviewCalonMahasiswa icm "
					+ "JOIN icm.pegawai p "
					+ joinSem
					+ "WHERE icm.tahunAkademik = :ta " + whereSem
					+ "GROUP BY p.id, p.nama "
					+ "ORDER BY COUNT(ipcm) DESC";
			org.hibernate.Query q = session.createQuery(hql)
					.setParameter("ta", ta)
					.setMaxResults(MAX_PEWAWANCARA);
			if (hasSem(sem)) { q.setParameter("sem", sem); }
			for (Object obj : q.list()) {
				Object[] r = (Object[]) obj;
				String nm = r[0] == null ? "-" : r[0].toString();
				int cnt   = r[1] == null ? 0 : ((Number) r[1]).intValue();
				result.add(new PewawancaraData(nm, cnt));
			}
		} catch (Exception e) {
			logErr("queryPerPewawancara ta=" + ta, e);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<ProdiInterviewData> queryPerProdi(Session session, String ta, String sem) {
		List<ProdiInterviewData> result = new ArrayList<ProdiInterviewData>();
		try {
			String joinSem = hasSem(sem) ? "JOIN ipcm.biodataCalonMahasiswa bcm " : "";
			String whereSem = hasSem(sem) ? "AND bcm.semesterMulai = :sem " : "";

			String hql = "SELECT j.nama, COUNT(ipcm) "
					+ "FROM InterviewPunyaCalonMahasiswa ipcm "
					+ "JOIN ipcm.interviewCalonMahasiswa icm "
					+ "JOIN icm.jurusan j "
					+ joinSem
					+ "WHERE icm.tahunAkademik = :ta " + whereSem
					+ "GROUP BY j.id, j.nama "
					+ "ORDER BY COUNT(ipcm) DESC";
			org.hibernate.Query q = session.createQuery(hql)
					.setParameter("ta", ta)
					.setMaxResults(MAX_PRODI);
			if (hasSem(sem)) { q.setParameter("sem", sem); }
			for (Object obj : q.list()) {
				Object[] r = (Object[]) obj;
				String nm = r[0] == null ? "-" : r[0].toString();
				int cnt   = r[1] == null ? 0 : ((Number) r[1]).intValue();
				result.add(new ProdiInterviewData(nm, cnt));
			}
		} catch (Exception e) {
			logErr("queryPerProdi ta=" + ta, e);
		}
		return result;
	}
}
