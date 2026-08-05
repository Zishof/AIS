package ais.action.master.pmb.statistik;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

import ais.database.model.PerguruanTinggi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyHtml;

/**
 * <h1>DashboardJalurMasukPmb — Analisis Konversi Per Jalur Penerimaan</h1>
 *
 * <p>Menjawab pertanyaan: <em>"Jalur masuk mana yang paling efektif menghasilkan
 * mahasiswa?"</em> Setiap institusi biasanya memiliki beberapa jalur penerimaan
 * (misalnya: Reguler, Beasiswa, Transfer, Kerjasama, PMDK). Tidak semua jalur
 * memiliki tingkat konversi yang sama dari peminat ke mahasiswa aktif ber-NIM.</p>
 *
 * <p>Tiga metrik per jalur yang ditampilkan:</p>
 * <ul>
 *   <li><b>Peminat</b> — yang mendaftar melalui jalur ini.</li>
 *   <li><b>Diterima</b> — yang dinyatakan lulus seleksi.</li>
 *   <li><b>Dapat NIM</b> — yang resmi menjadi mahasiswa.</li>
 * </ul>
 *
 * <p>Jalur dengan rasio NIM/Peminat tinggi adalah jalur yang paling "efisien" —
 * calon yang mendaftar di jalur tersebut hampir semuanya akhirnya menjadi mahasiswa.
 * Jalur dengan rasio rendah mungkin memiliki proses seleksi ketat, atau calon
 * yang diterima banyak yang tidak melanjutkan.</p>
 *
 * <p>Data diambil dari field {@code jenisSeleksi} (ManyToOne ke {@code JenisSeleksi})
 * di {@code BiodataCalonMahasiswa}. Jalur yang tidak memiliki {@code jenisSeleksi}
 * terisi tidak akan tercantum di dasbor ini.</p>
 *
 * @see DashboardPmbBase
 */
public class DashboardJalurMasukPmb extends DashboardPmbBase {

	// ═══════════════════════════════════════════════════════════════
	// Inner data class
	// ═══════════════════════════════════════════════════════════════

	private static final class JalurData {
		Object id;
		String nama;
		int    peminat;
		int    diterima;
		int    nim;

		JalurData(Object id, String nama, int peminat) {
			this.id      = id;
			this.nama    = nama;
			this.peminat = peminat;
		}

		double konversi() {
			return peminat > 0 ? (nim * 100.0 / peminat) : 0;
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// Konstruktor
	// ═══════════════════════════════════════════════════════════════

	public DashboardJalurMasukPmb(PerguruanTinggi pt) {
		super(pt);
	}

	// ═══════════════════════════════════════════════════════════════
	// doRefresh
	// ═══════════════════════════════════════════════════════════════

	@Override
	protected void doRefresh(Session session, String ta, String sem) {
		List<JalurData> list = queryJalur(session, ta, sem);

		int totalPeminat  = 0;
		int totalDiterima = 0;
		int totalNIM      = 0;
		for (int i = 0; i < list.size(); i++) {
			totalPeminat  += list.get(i).peminat;
			totalDiterima += list.get(i).diterima;
			totalNIM      += list.get(i).nim;
		}

		StringBuilder sb = new StringBuilder(5120);

		// Header
		sb.append("<div style=\"margin-bottom:14px;\">")
		  .append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
		  .append("Analisis Konversi Per Jalur Masuk &#8212; ").append(escHtml(ta)).append("</div>")
		  .append("<div style=\"font-size:12px;color:#9ca3af;\">")
		  .append("Seberapa efektif setiap jalur penerimaan menghasilkan mahasiswa aktif ber-NIM, "
		  		+ "dari pertama mendaftar hingga resmi terdaftar.")
		  .append("</div></div>");

		// KPI
		String jalurTerbaik = "-";
		double maxKonversi  = 0;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).konversi() > maxKonversi) {
				maxKonversi  = list.get(i).konversi();
				jalurTerbaik = list.get(i).nama;
			}
		}
		double konvTotal = totalPeminat > 0 ? (totalNIM * 100.0 / totalPeminat) : 0;
		sb.append(HtmlChartHelper.kpiCards(
				new String[] { "Jalur Aktif", "Total Peminat",
						"Total Dapat NIM", "Konversi Rata-rata", "Jalur Terbaik" },
				new String[] { fmtAngka(list.size()), fmtAngka(totalPeminat),
						fmtAngka(totalNIM), fmt1(konvTotal) + "%", abbrev(jalurTerbaik, 18) },
				new String[] { "", "", "", "",
						"konversi " + fmt1(maxKonversi) + "%" },
				new String[] { "", "", "", "", "" },
				new boolean[] { true, true, true, konvTotal >= 50, true },
				new String[] { "#6366f1", "#1877f2", "#059669", "#f59e0b", "#e4496b" }));

		if (list.isEmpty()) {
			sb.append(fullWidth(emptyCard("Analisis Jalur Masuk",
					"Belum ada data jenis seleksi/jalur masuk untuk periode ini.")));
			new MyHtml(sb.toString()).setParent(contentHolder);
			return;
		}

		// Donut komposisi peminat per jalur
		int n = list.size();
		String[] lblJalur  = new String[n];
		double[] valPeminat = new double[n];
		String[] donutColors = { "#3b82f6", "#10b981", "#f59e0b", "#ef4444",
				"#8b5cf6", "#06b6d4", "#e4496b", "#84cc16" };
		String[] usedColors = new String[n];
		for (int i = 0; i < n; i++) {
			lblJalur[i]   = list.get(i).nama;
			valPeminat[i] = list.get(i).peminat;
			usedColors[i] = donutColors[i % donutColors.length];
		}
		String donutHtml = HtmlChartHelper.donut(
				"Komposisi Peminat Per Jalur",
				"Proporsi jumlah pendaftar dari masing-masing jalur masuk. "
				+ "Jalur yang mendominasi bisa jadi karena lebih dikenal atau memiliki syarat lebih mudah.",
				lblJalur, valPeminat, usedColors, "");

		// Bar konversi per jalur
		String[] lblKonv   = new String[n];
		double[] valKonv   = new double[n];
		for (int i = 0; i < n; i++) {
			lblKonv[i] = list.get(i).nama;
			valKonv[i] = list.get(i).konversi();
		}
		// Urutkan konversi dari terbesar
		sortDesc(lblKonv, valKonv);
		String barKonv = HtmlChartHelper.barHorizontal(
				"Tingkat Konversi Per Jalur (NIM/Peminat %)",
				"Persentase pendaftar yang akhirnya resmi ber-NIM, per jalur masuk. "
				+ "Semakin panjang batang, semakin efektif jalur tersebut menghasilkan mahasiswa.",
				lblKonv, valKonv, "#059669");

		sb.append(grid2col(donutHtml, barKonv));

		// Grouped bar: Peminat vs Diterima vs NIM per jalur
		double[][] vals = new double[n][3];
		for (int i = 0; i < n; i++) {
			JalurData d = list.get(i);
			vals[i][0]  = d.peminat;
			vals[i][1]  = d.diterima;
			vals[i][2]  = d.nim;
		}
		sb.append(fullWidth(HtmlChartHelper.barVerticalGrouped(
				"Peminat vs Diterima vs Dapat NIM Per Jalur",
				"Tiga batang per jalur masuk: peminat (biru), diterima (hijau), dapat NIM (ungu). "
				+ "Jalur yang ketiga batangnya hampir sama tinggi berarti sangat efisien "
				+ "— hampir semua yang mendaftar akhirnya menjadi mahasiswa.",
				lblJalur,
				new String[] { "Peminat", "Diterima", "Dapat NIM" },
				vals,
				new String[] { "#3b82f6", "#10b981", "#8b5cf6" })));

		new MyHtml(sb.toString()).setParent(contentHolder);
	}

	// ═══════════════════════════════════════════════════════════════
	// Query
	// ═══════════════════════════════════════════════════════════════

	/**
	 * Mengambil data per jalur (jenisSeleksi) dari BCM, menggabungkan 3 query
	 * (peminat, diterima, NIM) via LinkedHashMap keyed by jenisSeleksi.id.
	 */
	@SuppressWarnings("unchecked")
	private List<JalurData> queryJalur(Session session, String ta, String sem) {
		Map<Object, JalurData> map = new LinkedHashMap<Object, JalurData>();

		// 1. Peminat per jalur
		try {
			String hql = "SELECT js.id, js.nama, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.jenisSeleksi js "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem)
					+ "GROUP BY js.id, js.nama ORDER BY COUNT(bcm) DESC";
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta);
			applySemParam(q, sem);
			for (Object obj : q.list()) {
				Object[] r  = (Object[]) obj;
				Object   id = r[0];
				String   nm = r[1] == null ? "-" : r[1].toString();
				int     cnt = r[2] == null ? 0 : ((Number) r[2]).intValue();
				map.put(id, new JalurData(id, nm, cnt));
			}
		} catch (Exception e) {
			logErr("queryJalur peminat ta=" + ta, e);
		}

		// Juga coba jenisSeleksiDipilih untuk yang mengoverride
		try {
			String hql2 = "SELECT js.id, js.nama, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.jenisSeleksiDipilih js "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem)
					+ "GROUP BY js.id, js.nama";
			org.hibernate.Query q2 = session.createQuery(hql2).setParameter("ta", ta);
			applySemParam(q2, sem);
			for (Object obj : q2.list()) {
				Object[] r  = (Object[]) obj;
				Object   id = r[0];
				String   nm = r[1] == null ? "-" : r[1].toString();
				int     cnt = r[2] == null ? 0 : ((Number) r[2]).intValue();
				if (!map.containsKey(id)) {
					map.put(id, new JalurData(id, nm, 0));
				}
				map.get(id).peminat += cnt;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/statistik/DashboardJalurMasukPmb.java:233");
			// jenisSeleksiDipilih mungkin tidak ada datanya, abaikan
		}

		if (map.isEmpty()) {
			return new ArrayList<JalurData>();
		}

		// 2. Diterima per jalur (dari jenisSeleksi BCM yang diterima)
		try {
			String hql = "SELECT js.id, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.jenisSeleksi js "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem)
					+ "AND bcm.prodiLulus IS NOT NULL "
					+ "GROUP BY js.id";
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta);
			applySemParam(q, sem);
			for (Object obj : q.list()) {
				Object[] r  = (Object[]) obj;
				Object   id = r[0];
				int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
				if (map.containsKey(id)) { map.get(id).diterima += cnt; }
			}
		} catch (Exception e) {
			logErr("queryJalur diterima ta=" + ta, e);
		}

		// 3. Dapat NIM per jalur
		try {
			String hql = "SELECT js.id, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.jenisSeleksi js "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem)
					+ "AND bcm.mahasiswa IS NOT NULL "
					+ "GROUP BY js.id";
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta);
			applySemParam(q, sem);
			for (Object obj : q.list()) {
				Object[] r  = (Object[]) obj;
				Object   id = r[0];
				int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
				if (map.containsKey(id)) { map.get(id).nim += cnt; }
			}
		} catch (Exception e) {
			logErr("queryJalur nim ta=" + ta, e);
		}

		return new ArrayList<JalurData>(map.values());
	}

	// ═══════════════════════════════════════════════════════════════
	// Utility: sort descending parallel arrays
	// ═══════════════════════════════════════════════════════════════

	/** Mengurutkan pasangan (String[], double[]) berdasarkan nilai double DESC (bubble sort). */
	private static void sortDesc(String[] labels, double[] values) {
		int n = labels.length;
		for (int i = 0; i < n - 1; i++) {
			for (int j = 0; j < n - i - 1; j++) {
				if (values[j] < values[j + 1]) {
					double tmpD = values[j]; values[j] = values[j + 1]; values[j + 1] = tmpD;
					String tmpS = labels[j]; labels[j] = labels[j + 1]; labels[j + 1] = tmpS;
				}
			}
		}
	}
}
