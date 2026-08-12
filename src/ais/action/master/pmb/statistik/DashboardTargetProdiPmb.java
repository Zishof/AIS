package ais.action.master.pmb.statistik;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Columns;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.model.PerguruanTinggi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

/**
 * <h1>DashboardTargetProdiPmb — Target vs Realisasi Penerimaan Per Prodi</h1>
 *
 * <p>Menjawab pertanyaan paling kritis pimpinan PMB: <em>"Prodi mana yang sudah mencapai
 * target dan mana yang masih jauh?"</em></p>
 *
 * <p>Target diambil dari {@code KapasitasMahasiswaBaru.jumlahTargetMahasiswaBaru} (data
 * PDDikti/feeder). Realisasi dihitung secara langsung dari {@code BiodataCalonMahasiswa}
 * berdasarkan tiga metrik:</p>
 * <ul>
 *   <li><b>Peminat</b> — yang memilih prodi ini sebagai pilihan pertama.</li>
 *   <li><b>Diterima</b> — yang prodiLulus-nya menunjuk ke prodi ini.</li>
 *   <li><b>Dapat NIM</b> — yang sudah resmi menjadi mahasiswa (dapat NIM).</li>
 * </ul>
 *
 * <p>Semakin kecil "Dapat NIM" dibanding target, semakin besar risiko kekurangan
 * mahasiswa di prodi tersebut pada semester mendatang.</p>
 *
 * @see DashboardPmbBase
 */
public class DashboardTargetProdiPmb extends DashboardPmbBase {

	// ═══════════════════════════════════════════════════════════════
	// Inner data class
	// ═══════════════════════════════════════════════════════════════

	private static final class ProdiTargetData {
		Object id;
		String nama;
		int    target;
		int    peminat;
		int    diterima;
		int    nim;

		ProdiTargetData(Object id, String nama) {
			this.id   = id;
			this.nama = nama;
		}

		/** Persentase pencapaian target (NIM / target). */
		double persenTarget() {
			return target > 0 ? (nim * 100.0 / target) : 0;
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// Konstanta
	// ═══════════════════════════════════════════════════════════════

	private static final int MAX_PRODI   = 8;
	private static final int MAX_ABBREV  = 20;

	// ═══════════════════════════════════════════════════════════════
	// Konstruktor
	// ═══════════════════════════════════════════════════════════════

	public DashboardTargetProdiPmb(PerguruanTinggi pt) {
		super(pt);
	}

	// ═══════════════════════════════════════════════════════════════
	// doRefresh
	// ═══════════════════════════════════════════════════════════════

	@Override
	protected void doRefresh(Session session, String ta, String sem) {
		List<ProdiTargetData> list = queryData(session, ta, sem);

		int totalTarget   = 0;
		int totalPeminat  = 0;
		int totalDiterima = 0;
		int totalNIM      = 0;
		int overTarget    = 0;
		for (int i = 0; i < list.size(); i++) {
			ProdiTargetData d = list.get(i);
			totalTarget   += d.target;
			totalPeminat  += d.peminat;
			totalDiterima += d.diterima;
			totalNIM      += d.nim;
			if (d.target > 0 && d.nim >= d.target) { overTarget++; }
		}

		StringBuilder sb = new StringBuilder(5120);

		// Header
		sb.append("<div style=\"margin-bottom:14px;\">")
		  .append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
		  .append("Target vs Realisasi Per Prodi &#8212; ").append(escHtml(ta)).append("</div>")
		  .append("<div style=\"font-size:12px;color:#9ca3af;\">")
		  .append("Perbandingan jumlah target penerimaan dengan realisasi mahasiswa yang sudah ber-NIM, per program studi.")
		  .append("</div></div>");

		// KPI
		double pctRealisasi = totalTarget > 0 ? (totalNIM * 100.0 / totalTarget) : 0;
		sb.append(HtmlChartHelper.kpiCards(
				new String[] { "Total Target", "Total Peminat", "Total Diterima",
						"Total Dapat NIM", "Realisasi Target" },
				new String[] { fmtAngka(totalTarget), fmtAngka(totalPeminat),
						fmtAngka(totalDiterima), fmtAngka(totalNIM), fmt1(pctRealisasi) + "%" },
				new String[] { "dari feeder PDDikti", "", "", "", "" },
				new String[] { "", "", "", "",
						pctRealisasi >= 100 ? "Sudah tercapai" :
						pctRealisasi >= 75 ? "Mendekati target" : "Masih di bawah target" },
				new boolean[] { true, true, true, true, pctRealisasi >= 100 },
				new String[] { "#6366f1", "#1877f2", "#059669", "#7c3aed", "#f59e0b" }));

		// Chart grouped bar
		if (!list.isEmpty()) {
			int n = Math.min(list.size(), MAX_PRODI);
			String[] cats  = new String[n];
			double[][] vals = new double[n][4];
			for (int i = 0; i < n; i++) {
				ProdiTargetData d = list.get(i);
				cats[i]    = abbrev(d.nama, MAX_ABBREV);
				vals[i][0] = d.target;
				vals[i][1] = d.peminat;
				vals[i][2] = d.diterima;
				vals[i][3] = d.nim;
			}
			sb.append(fullWidth(HtmlChartHelper.barVerticalGrouped(
					"Target vs Peminat vs Diterima vs NIM Per Prodi",
					"Empat batang per prodi: target (abu-abu), peminat pilihan 1 (biru), "
					+ "diterima (hijau), dan yang sudah ber-NIM (ungu). "
					+ "Batang NIM yang mendekati atau melewati target berarti prodi tersebut berhasil.",
					cats,
					new String[] { "Target", "Peminat", "Diterima", "Dapat NIM" },
					vals,
					new String[] { "#9ca3af", "#3b82f6", "#10b981", "#8b5cf6" })));
		} else {
			sb.append(fullWidth(emptyCard("Target vs Realisasi",
					"Belum ada data target (KapasitasMahasiswaBaru) untuk tahun akademik ini.")));
		}

		new MyHtml(sb.toString()).setParent(contentHolder);

		// Tabel ZK
		buildTable(list, totalTarget, totalPeminat, totalDiterima, totalNIM);
	}

	// ═══════════════════════════════════════════════════════════════
	// Tabel ZK Grid
	// ═══════════════════════════════════════════════════════════════

	private void buildTable(List<ProdiTargetData> list,
			int totTarget, int totPeminat, int totDiterima, int totNIM) {
		if (list.isEmpty()) { return; }

		Div headerDiv = new Div();
		headerDiv.setStyle("display:flex;align-items:center;justify-content:space-between;"
				+ "flex-wrap:wrap;gap:8px;margin-bottom:8px;");
		headerDiv.setParent(tableHolder);

		Div titleDiv = new Div();
		titleDiv.setParent(headerDiv);
		new MyLabelBoldAja("Rincian Target vs Realisasi Per Program Studi").setParent(titleDiv);
		Label lblSub = new Label(ais.common.Common.getBahasaConfig("Tabel lengkap semua prodi, dapat diunduh ke Excel."));
		lblSub.setStyle("font-size:11px;color:#6b7280;display:block;margin-top:2px;");
		lblSub.setParent(titleDiv);

		final MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setFixedLayout(true);
		grid.setParent(tableHolder);

		Columns cols = new Columns();
		cols.setParent(grid);
		addCol(cols, "No.",           "40px");
		addCol(cols, "Program Studi", null);
		addCol(cols, "Target",        "80px");
		addCol(cols, "Peminat",       "80px");
		addCol(cols, "Diterima",      "80px");
		addCol(cols, "Dapat NIM",     "85px");
		addCol(cols, "Realisasi (%)", "100px");

		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < list.size(); i++) {
			ProdiTargetData d = list.get(i);
			double pct = d.persenTarget();
			Row row = new Row();
			row.setParent(rows);
			new Label(String.valueOf(i + 1)).setParent(row);
			new Label(d.nama).setParent(row);
			new Label(fmtAngka(d.target)).setParent(row);
			new Label(fmtAngka(d.peminat)).setParent(row);
			new Label(fmtAngka(d.diterima)).setParent(row);
			new Label(fmtAngka(d.nim)).setParent(row);
			Label lblPct = new Label(fmt1(pct) + "%");
			lblPct.setStyle("color:" + (pct >= 100 ? "#16a34a" : pct >= 75 ? "#d97706" : "#dc2626")
					+ ";font-weight:600;");
			lblPct.setParent(row);
		}

		// Baris total
		double pctTotal = totTarget > 0 ? (totNIM * 100.0 / totTarget) : 0;
		Row rowTotal = new Row();
		rowTotal.setStyle("font-weight:bold;background:#f0f4ff;");
		rowTotal.setParent(rows);
		new Label("").setParent(rowTotal);
		new Label(ais.common.Common.getBahasaConfig("TOTAL")).setParent(rowTotal);
		new Label(fmtAngka(totTarget)).setParent(rowTotal);
		new Label(fmtAngka(totPeminat)).setParent(rowTotal);
		new Label(fmtAngka(totDiterima)).setParent(rowTotal);
		new Label(fmtAngka(totNIM)).setParent(rowTotal);
		new Label(fmt1(pctTotal) + "%").setParent(rowTotal);

		MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig("Unduh Excel", "/img/excel.png");
		btnDownload.setStyle("margin-top:8px;");
		btnDownload.setParent(tableHolder);
		btnDownload.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UIUtil.downloadGrid(grid);
			}
		});
	}

	private void addCol(Columns cols, String label, String width) {
		MyColumnConfig col = new MyColumnConfig();
		col.setLabel(label);
		if (width != null) { col.setWidth(width); }
		col.setParent(cols);
	}

	// ═══════════════════════════════════════════════════════════════
	// Query
	// ═══════════════════════════════════════════════════════════════

	/**
	 * Menggabungkan data target (KapasitasMahasiswaBaru) dengan data aktual (BCM).
	 * Pivot key = id jurusan.
	 */
	@SuppressWarnings("unchecked")
	private List<ProdiTargetData> queryData(Session session, String ta, String sem) {
		Map<Object, ProdiTargetData> map = new LinkedHashMap<Object, ProdiTargetData>();

		// 1. Target dari KapasitasMahasiswaBaru
		try {
			boolean filterJenjang = getSelectedJenjang() != null;
			String hqlTarget = "SELECT j.id, j.nama, SUM(kmb.jumlahTargetMahasiswaBaru) "
					+ "FROM KapasitasMahasiswaBaru kmb JOIN kmb.jurusan j "
					+ "WHERE kmb.tahunAkademik = :ta "
					+ (filterJenjang ? "AND j.jenjang = :jenjang " : "")
					+ "GROUP BY j.id, j.nama "
					+ "ORDER BY SUM(kmb.jumlahTargetMahasiswaBaru) DESC";
			org.hibernate.Query qTarget = session.createQuery(hqlTarget).setParameter("ta", ta);
			applyJenjangParam(qTarget);
			List<?> rows = qTarget.list();
			for (Object obj : rows) {
				Object[] r  = (Object[]) obj;
				Object   id = r[0];
				String   nm = r[1] == null ? "-" : r[1].toString();
				int   tgt   = r[2] == null ? 0 : ((Number) r[2]).intValue();
				ProdiTargetData d = new ProdiTargetData(id, nm);
				d.target = tgt;
				map.put(id, d);
			}
		} catch (Exception e) {
			logErr("queryTarget ta=" + ta, e);
		}

		// Jika target kosong, ambil prodi dari BCM saja (untuk peminat/diterima/NIM)
		boolean targetKosong = map.isEmpty();

		// 2. Peminat per prodi (pilihan 1)
		try {
			String hqlPem = "SELECT j.id, j.nama, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.prodi1 j "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause()
					+ "GROUP BY j.id, j.nama "
					+ "ORDER BY COUNT(bcm) DESC";
			org.hibernate.Query q = session.createQuery(hqlPem).setParameter("ta", ta)
					.setMaxResults(MAX_PRODI);
			applySemParam(q, sem);
			applyJenjangParam(q);
			for (Object obj : q.list()) {
				Object[] r  = (Object[]) obj;
				Object   id = r[0];
				String   nm = r[1] == null ? "-" : r[1].toString();
				int     cnt = r[2] == null ? 0 : ((Number) r[2]).intValue();
				if (!map.containsKey(id)) {
					if (targetKosong) { map.put(id, new ProdiTargetData(id, nm)); }
					else { continue; }
				}
				map.get(id).peminat = cnt;
			}
		} catch (Exception e) {
			logErr("queryPeminat ta=" + ta, e);
		}

		// 3. Diterima per prodiLulus
		try {
			String hqlDit = "SELECT j.id, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.prodiLulus j "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause()
					+ "GROUP BY j.id";
			org.hibernate.Query q = session.createQuery(hqlDit).setParameter("ta", ta);
			applySemParam(q, sem);
			applyJenjangParam(q);
			for (Object obj : q.list()) {
				Object[] r  = (Object[]) obj;
				Object   id = r[0];
				int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
				if (map.containsKey(id)) { map.get(id).diterima = cnt; }
			}
		} catch (Exception e) {
			logErr("queryDiterima ta=" + ta, e);
		}

		// 4. Dapat NIM per prodiLulus
		try {
			String hqlNIM = "SELECT j.id, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.prodiLulus j "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause()
					+ "AND bcm.mahasiswa IS NOT NULL "
					+ "GROUP BY j.id";
			org.hibernate.Query q = session.createQuery(hqlNIM).setParameter("ta", ta);
			applySemParam(q, sem);
			applyJenjangParam(q);
			for (Object obj : q.list()) {
				Object[] r  = (Object[]) obj;
				Object   id = r[0];
				int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
				if (map.containsKey(id)) { map.get(id).nim = cnt; }
			}
		} catch (Exception e) {
			logErr("queryNIM ta=" + ta, e);
		}

		List<ProdiTargetData> result = new ArrayList<ProdiTargetData>(map.values());
		// Urutkan: yang paling jauh dari target (berdasarkan NIM) di atas
		// Urutkan sederhana berdasarkan peminat DESC (LinkedHashMap sudah ordered by target)
		return result;
	}
}
