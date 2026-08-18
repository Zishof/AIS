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
 * <h1>DashboardRegistrasiUlangPmb — Status Registrasi Ulang &amp; Pembayaran</h1>
 *
 * <p>Menampilkan seberapa jauh calon mahasiswa yang sudah diterima benar-benar
 * menyelesaikan proses administrasi (bayar registrasi dan daftar ulang). Tiga tahap
 * yang dipantau adalah:</p>
 * <ol>
 *   <li><b>Diterima</b> — calon yang sudah dinyatakan lulus seleksi (prodiLulus terisi).</li>
 *   <li><b>Bayar Registrasi</b> — dari yang diterima, berapa yang sudah membayar uang
 *       registrasi (konfirmasi minat untuk mendaftar ulang).</li>
 *   <li><b>Daftar Ulang</b> — dari yang bayar registrasi, berapa yang sudah menyelesaikan
 *       pembayaran daftar ulang (komitmen penuh menjadi mahasiswa).</li>
 * </ol>
 *
 * <p>Selisih antara "Diterima" dan "Daftar Ulang" adalah calon yang berpotensi hilang
 * (dropout sebelum kuliah). Dasbor ini membantu tim PMB untuk segera melakukan
 * follow-up sebelum kursi kosong dan tidak bisa diisi oleh pendaftar gelombang lanjutan.</p>
 *
 * <p>Data pembayaran diambil melalui field {@code pembayaranRegistrasi} dan
 * {@code pembayaranDaftarUlang} di {@code BiodataCalonMahasiswa}, keduanya
 * merupakan @ManyToOne ke entity {@code Kegiatan}.</p>
 *
 * @see DashboardPmbBase
 */
public class DashboardRegistrasiUlangPmb extends DashboardPmbBase {

	// ═══════════════════════════════════════════════════════════════
	// Inner data class
	// ═══════════════════════════════════════════════════════════════

	private static final class ProdiRegData {
		Object id;
		String nama;
		int    diterima;
		int    bayarDafUlang;

		ProdiRegData(Object id, String nama, int diterima) {
			this.id       = id;
			this.nama     = nama;
			this.diterima = diterima;
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// Konstanta
	// ═══════════════════════════════════════════════════════════════

	private static final int MAX_PRODI  = 8;
	private static final int MAX_ABBREV = 20;

	// ═══════════════════════════════════════════════════════════════
	// Konstruktor
	// ═══════════════════════════════════════════════════════════════

	public DashboardRegistrasiUlangPmb(PerguruanTinggi pt) {
		super(pt);
	}

	// ═══════════════════════════════════════════════════════════════
	// doRefresh
	// ═══════════════════════════════════════════════════════════════

	@Override
	protected void doRefresh(Session session, String ta, String sem) {
		int diterima      = queryCount(session, ta, sem, "bcm.prodiLulus IS NOT NULL");
		int bayarReg      = queryCount(session, ta, sem, "bcm.pembayaranRegistrasi IS NOT NULL");
		int bayarDafUlang = queryCount(session, ta, sem, "bcm.pembayaranDaftarUlang IS NOT NULL");
		int menunggak     = diterima - bayarDafUlang;

		List<ProdiRegData> prodiList = queryPerProdi(session, ta, sem);

		StringBuilder sb = new StringBuilder(5120);

		// Header
		sb.append("<div style=\"margin-bottom:14px;\">")
		  .append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
		  .append("Status Registrasi Ulang &#8212; ").append(escHtml(ta)).append("</div>")
		  .append("<div style=\"font-size:12px;color:#9ca3af;\">")
		  .append("Pantau berapa calon yang diterima sudah menyelesaikan pembayaran registrasi dan daftar ulang.")
		  .append("</div></div>");

		// KPI
		double pctReg = diterima > 0 ? (bayarReg * 100.0 / diterima) : 0;
		double pctDU  = diterima > 0 ? (bayarDafUlang * 100.0 / diterima) : 0;
		sb.append(HtmlChartHelper.kpiCards(
				new String[] { "Diterima", "Bayar Registrasi", "Daftar Ulang",
						"Belum Daftar Ulang", "Konversi Daftar Ulang" },
				new String[] { fmtAngka(diterima), fmtAngka(bayarReg) + " (" + fmt1(pctReg) + "%)",
						fmtAngka(bayarDafUlang), fmtAngka(menunggak), fmt1(pctDU) + "%" },
				new String[] { "lulus seleksi", "dari diterima", "dari diterima",
						"perlu follow-up", "" },
				new String[] { "", "", "", "", "" },
				new boolean[] { true, true, true, menunggak == 0, pctDU >= 80 },
				new String[] { "#1877f2", "#059669", "#7c3aed", "#dc2626", "#f59e0b" }));

		// Corong pembayaran
		sb.append(fullWidth(HtmlChartHelper.barHorizontal(
				"Corong Registrasi — Diterima sampai Daftar Ulang",
				"Tiga tahap yang harus dilalui setelah dinyatakan lulus: diterima, bayar "
				+ "registrasi, lalu daftar ulang. Selisih besar di tiap tahap = "
				+ "calon yang perlu segera dihubungi agar tidak kehilangan kursi.",
				new String[] { "Diterima", "Bayar Registrasi", "Daftar Ulang" },
				new double[] { diterima, bayarReg, bayarDafUlang },
				"#7c3aed")));

		// Grouped bar per prodi
		if (!prodiList.isEmpty()) {
			int n = Math.min(prodiList.size(), MAX_PRODI);
			String[] cats   = new String[n];
			double[][] vals = new double[n][2];
			for (int i = 0; i < n; i++) {
				ProdiRegData d = prodiList.get(i);
				cats[i]    = abbrev(d.nama, MAX_ABBREV);
				vals[i][0] = d.diterima;
				vals[i][1] = d.bayarDafUlang;
			}
			sb.append(fullWidth(HtmlChartHelper.barVerticalGrouped(
					"Diterima vs Daftar Ulang Per Program Studi",
					"Per prodi: batang biru = yang diterima, batang ungu = yang sudah daftar ulang. "
					+ "Prodi dengan selisih besar perlu perhatian lebih dalam follow-up calon mahasiswanya.",
					cats,
					new String[] { "Diterima", "Daftar Ulang" },
					vals,
					new String[] { "#3b82f6", "#7c3aed" })));
		}

		new MyHtml(sb.toString()).setParent(contentHolder);

		// Tabel
		buildTable(prodiList, diterima, bayarDafUlang);
	}

	// ═══════════════════════════════════════════════════════════════
	// Tabel ZK Grid
	// ═══════════════════════════════════════════════════════════════

	private void buildTable(List<ProdiRegData> list, int totDiterima, int totDafUlang) {
		if (list.isEmpty()) { return; }

		Div hdr = new Div();
		hdr.setStyle("display:flex;align-items:center;justify-content:space-between;"
				+ "flex-wrap:wrap;gap:8px;margin-bottom:8px;");
		hdr.setParent(tableHolder);

		Div titleDiv = new Div();
		titleDiv.setParent(hdr);
		new MyLabelBoldAja("Status Daftar Ulang Per Program Studi").setParent(titleDiv);
		Label lbl = new Label(ais.common.Common.getBahasaConfig("Rincian diterima vs sudah daftar ulang per prodi."));
		lbl.setStyle("font-size:11px;color:#6b7280;display:block;margin-top:2px;");
		lbl.setParent(titleDiv);

		final MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setFixedLayout(true);
		grid.setParent(tableHolder);

		Columns cols = new Columns();
		cols.setParent(grid);
		addCol(cols, "No.",            "40px");
		addCol(cols, "Program Studi",  null);
		addCol(cols, "Diterima",       "90px");
		addCol(cols, "Daftar Ulang",   "100px");
		addCol(cols, "Belum DU",       "85px");
		addCol(cols, "Konversi (%)",   "100px");

		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < list.size(); i++) {
			ProdiRegData d   = list.get(i);
			int belum        = d.diterima - d.bayarDafUlang;
			double pct       = d.diterima > 0 ? (d.bayarDafUlang * 100.0 / d.diterima) : 0;
			Row row = new Row();
			row.setParent(rows);
			new Label(String.valueOf(i + 1)).setParent(row);
			new Label(d.nama).setParent(row);
			new Label(fmtAngka(d.diterima)).setParent(row);
			new Label(fmtAngka(d.bayarDafUlang)).setParent(row);
			Label lblBelum = new Label(fmtAngka(belum));
			if (belum > 0) { lblBelum.setStyle("color:#dc2626;font-weight:600;"); }
			lblBelum.setParent(row);
			Label lblPct = new Label(fmt1(pct) + "%");
			lblPct.setStyle("color:" + (pct >= 80 ? "#16a34a" : pct >= 50 ? "#d97706" : "#dc2626")
					+ ";font-weight:600;");
			lblPct.setParent(row);
		}

		double totPct = totDiterima > 0 ? (totDafUlang * 100.0 / totDiterima) : 0;
		Row rowTotal = new Row();
		rowTotal.setStyle("font-weight:bold;background:#f0f4ff;");
		rowTotal.setParent(rows);
		new Label("").setParent(rowTotal);
		new Label(ais.common.Common.getBahasaConfig("TOTAL")).setParent(rowTotal);
		new Label(fmtAngka(totDiterima)).setParent(rowTotal);
		new Label(fmtAngka(totDafUlang)).setParent(rowTotal);
		new Label(fmtAngka(totDiterima - totDafUlang)).setParent(rowTotal);
		new Label(fmt1(totPct) + "%").setParent(rowTotal);

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
	// Query methods
	// ═══════════════════════════════════════════════════════════════

	/** Query COUNT dengan kondisi tambahan di WHERE clause. */
	@SuppressWarnings("unchecked")
	private int queryCount(Session session, String ta, String sem, String extraWhere) {
		try {
			String hql = "SELECT COUNT(bcm) FROM BiodataCalonMahasiswa bcm "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause()
					+ "AND " + extraWhere;
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta);
			applySemParam(q, sem);
			applyJenjangParam(q);
			Number n = (Number) q.uniqueResult();
			return n == null ? 0 : n.intValue();
		} catch (Exception e) {
			logErr("queryCount ta=" + ta + " where=" + extraWhere, e);
			return 0;
		}
	}

	/** Data per prodi: diterima (prodiLulus) vs sudah daftar ulang. */
	@SuppressWarnings("unchecked")
	private List<ProdiRegData> queryPerProdi(Session session, String ta, String sem) {
		Map<Object, ProdiRegData> map = new LinkedHashMap<Object, ProdiRegData>();

		// Query 1: diterima per prodiLulus
		try {
			String hql = "SELECT j.id, j.nama, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.prodiLulus j "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause()
					+ "GROUP BY j.id, j.nama ORDER BY COUNT(bcm) DESC";
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta)
					.setMaxResults(MAX_PRODI);
			applySemParam(q, sem);
			applyJenjangParam(q);
			for (Object obj : q.list()) {
				Object[] r  = (Object[]) obj;
				Object   id = r[0];
				String   nm = r[1] == null ? "-" : r[1].toString();
				int     cnt = r[2] == null ? 0 : ((Number) r[2]).intValue();
				map.put(id, new ProdiRegData(id, nm, cnt));
			}
		} catch (Exception e) {
			logErr("queryPerProdi diterima ta=" + ta, e);
		}

		// Query 2: bayar daftar ulang per prodiLulus
		try {
			String hql = "SELECT j.id, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.prodiLulus j "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause()
					+ "AND bcm.pembayaranDaftarUlang IS NOT NULL "
					+ "GROUP BY j.id";
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta);
			applySemParam(q, sem);
			applyJenjangParam(q);
			for (Object obj : q.list()) {
				Object[] r  = (Object[]) obj;
				Object   id = r[0];
				int     cnt = r[1] == null ? 0 : ((Number) r[1]).intValue();
				if (map.containsKey(id)) { map.get(id).bayarDafUlang = cnt; }
			}
		} catch (Exception e) {
			logErr("queryPerProdi dafUlang ta=" + ta, e);
		}

		return new ArrayList<ProdiRegData>(map.values());
	}
}
