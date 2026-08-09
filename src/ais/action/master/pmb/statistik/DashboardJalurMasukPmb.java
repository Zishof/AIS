package ais.action.master.pmb.statistik;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisSeleksi;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyHtml;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

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

	private static final String KONFIG_PEMINAT_ASLI = "dashboard_pmb_peminat_asli_jalur";
	private static final String NILAI_OTOMATIS = "AUTO";
	private static final String SEMUA_SEMESTER = "SEMUA";

	// ═══════════════════════════════════════════════════════════════
	// Inner data class
	// ═══════════════════════════════════════════════════════════════

	private static final class JalurData {
		Object id;
		String nama;
		int    peminat;
		int    peminatEcampus;
		Integer peminatAsliEksternal;
		int    diterima;
		int    nim;

		JalurData(Object id, String nama, int peminat) {
			this.id      = id;
			this.nama    = nama;
			this.peminat = peminat;
			this.peminatEcampus = peminat;
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

	@Override
	protected void buildExtraFilter(Div filterBar) {
		MyToolbarbuttonConfig input = new MyToolbarbuttonConfig("Input Peminat Asli", "/img/edit.png");
		input.setTooltiptext("Masukkan jumlah peminat dari portal seleksi nasional/regional di luar eCampus");
		input.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE));
		input.setParent(filterBar);
		input.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				tampilkanInputPeminatAsli();
			}
		});
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

		int jumlahOverride = 0;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).peminatAsliEksternal != null) {
				jumlahOverride++;
			}
		}
		sb.append("<div style=\"margin-top:10px;padding:9px 12px;border-radius:8px;"
				+ "background:#eff6ff;border:1px solid #bfdbfe;color:#1e40af;font-size:12px;\">"
				+ "<b>Sumber data peminat:</b> " + jumlahOverride
				+ " jalur memakai angka peminat asli eksternal; jalur lainnya memakai pendaftaran eCampus. "
				+ "Angka eksternal tidak dijumlahkan dengan eCampus, tetapi menjadi angka peminat resmi untuk jalur/periode tersebut."
				+ "</div>");

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
		sb.append(fullWidth(buildSumberDataTable(list)));

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

	private String buildSumberDataTable(List<JalurData> list) {
		StringBuilder sb = new StringBuilder(2048);
		sb.append("<div class=\"ais-chart-card\"><div class=\"ais-chart-title\">Sumber Data Peminat Per Jalur</div>")
		  .append("<div style=\"font-size:12px;color:#6b7280;margin-bottom:9px;\">")
		  .append("Memperlihatkan angka eCampus, angka eksternal bila diisi, dan angka efektif yang dipakai seluruh perhitungan dasbor.")
		  .append("</div><div style=\"overflow-x:auto;\"><table style=\"width:100%;border-collapse:collapse;font-size:12px;\">")
		  .append("<thead><tr style=\"background:#f8fafc;color:#475569;\">")
		  .append("<th style=\"text-align:left;padding:8px;border-bottom:1px solid #e5e7eb;\">Jalur</th>")
		  .append("<th style=\"text-align:right;padding:8px;border-bottom:1px solid #e5e7eb;\">eCampus</th>")
		  .append("<th style=\"text-align:right;padding:8px;border-bottom:1px solid #e5e7eb;\">Peminat Asli Eksternal</th>")
		  .append("<th style=\"text-align:right;padding:8px;border-bottom:1px solid #e5e7eb;\">Dipakai Dasbor</th>")
		  .append("<th style=\"text-align:left;padding:8px;border-bottom:1px solid #e5e7eb;\">Sumber</th></tr></thead><tbody>");
		for (int i = 0; i < list.size(); i++) {
			JalurData d = list.get(i);
			sb.append("<tr><td style=\"padding:8px;border-bottom:1px solid #f1f5f9;\">").append(escHtml(d.nama)).append("</td>")
			  .append("<td style=\"text-align:right;padding:8px;border-bottom:1px solid #f1f5f9;\">").append(fmtAngka(d.peminatEcampus)).append("</td>")
			  .append("<td style=\"text-align:right;padding:8px;border-bottom:1px solid #f1f5f9;\">")
			  .append(d.peminatAsliEksternal == null ? "-" : fmtAngka(d.peminatAsliEksternal.intValue())).append("</td>")
			  .append("<td style=\"text-align:right;font-weight:700;padding:8px;border-bottom:1px solid #f1f5f9;\">").append(fmtAngka(d.peminat)).append("</td>")
			  .append("<td style=\"padding:8px;border-bottom:1px solid #f1f5f9;color:")
			  .append(d.peminatAsliEksternal == null ? "#64748b" : "#047857").append(";\">")
			  .append(d.peminatAsliEksternal == null ? "eCampus" : "Eksternal (resmi)").append("</td></tr>");
		}
		return sb.append("</tbody></table></div></div>").toString();
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

		for (JalurData data : map.values()) {
			data.peminatEcampus = data.peminat;
		}
		terapkanPeminatAsliEksternal(session, ta, sem, map);
		return new ArrayList<JalurData>(map.values());
	}

	@SuppressWarnings("unchecked")
	private List<JenisSeleksi> getJalurAktif(Session session) {
		return session.createCriteria(JenisSeleksi.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")).list();
	}

	private String semesterKey(String sem) {
		return hasSem(sem) ? sem.trim().toUpperCase() : SEMUA_SEMESTER;
	}

	private Konfigurasi findPeminatConfig(Session session, String ta, String sem, Long jenisSeleksiId) {
		if (session == null || jenisSeleksiId == null) {
			return null;
		}
		return (Konfigurasi) session.createCriteria(Konfigurasi.class)
				.add(Restrictions.eq("nama", KONFIG_PEMINAT_ASLI))
				.add(Restrictions.eq("tahunAkademik", ta))
				.add(Restrictions.eq("info1", jenisSeleksiId.toString()))
				.add(Restrictions.eq("info2", semesterKey(sem)))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
	}

	private Integer parsePeminatAsli(Konfigurasi konfigurasi) {
		if (konfigurasi == null || konfigurasi.getNilai() == null
				|| NILAI_OTOMATIS.equalsIgnoreCase(konfigurasi.getNilai().trim())) {
			return null;
		}
		try {
			int nilai = Integer.parseInt(konfigurasi.getNilai().trim());
			return nilai < 0 ? null : Integer.valueOf(nilai);
		} catch (Exception e) {
			return null;
		}
	}

	private void terapkanPeminatAsliEksternal(Session session, String ta, String sem,
			Map<Object, JalurData> map) {
		try {
			List<JenisSeleksi> jalur = getJalurAktif(session);
			for (int i = 0; i < jalur.size(); i++) {
				JenisSeleksi js = jalur.get(i);
				Integer eksternal = parsePeminatAsli(findPeminatConfig(session, ta, sem, js.getId()));
				if (eksternal == null) {
					continue;
				}
				JalurData data = map.get(js.getId());
				if (data == null) {
					data = new JalurData(js.getId(), js.getNama(), 0);
					map.put(js.getId(), data);
				}
				data.peminatAsliEksternal = eksternal;
				data.peminat = eksternal.intValue();
			}
		} catch (Exception e) {
			logErr("menerapkan peminat asli eksternal ta=" + ta, e);
		}
	}

	private void tampilkanInputPeminatAsli() throws Exception {
		final String ta = getSelectedTA();
		final String sem = getSelectedSem();
		final Session session = HibernateUtil.currentSession();
		final List<JalurData> dataDasbor = queryJalur(session, ta, sem);
		final Map<Object, JalurData> dataPerJalur = new LinkedHashMap<Object, JalurData>();
		for (int i = 0; i < dataDasbor.size(); i++) {
			dataPerJalur.put(dataDasbor.get(i).id, dataDasbor.get(i));
		}

		final MyWindow window = new MyWindow("Input Peminat Asli Eksternal", "normal", true);
		window.setWidth("900px");
		window.setHeight("88%");
		window.setSizable(true);
		window.setMaximizable(true);

		Vbox root = new Vbox();
		root.setWidth("100%");
		root.setStyle("padding:12px;box-sizing:border-box;overflow:auto;");
		root.setParent(window);
		Label info = new Label("Periode " + ta + " / " + (hasSem(sem) ? sem : "Semua semester")
				+ ". Isi hanya jalur yang pendaftarannya berlangsung di luar eCampus. "
				+ "Kosongkan agar kembali memakai hitungan eCampus.");
		info.setMultiline(true);
		info.setStyle("padding:10px;background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;"
				+ "color:#1e40af;margin-bottom:10px;");
		info.setParent(root);

		Grid grid = new Grid();
		grid.setWidth("100%");
		grid.setSclass("dgrid");
		grid.setParent(root);
		Columns columns = new Columns();
		columns.setParent(grid);
		new MyColumnConfig("Jalur Seleksi", "270px").setParent(columns);
		new MyColumnConfig("Peminat eCampus", "120px").setParent(columns);
		new MyColumnConfig("Peminat Asli Eksternal", "170px").setParent(columns);
		new MyColumnConfig("Dipakai Saat Ini", "130px").setParent(columns);
		new MyColumnConfig("Sumber", "130px").setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);

		final Map<Long, Intbox> inputPerJalur = new LinkedHashMap<Long, Intbox>();
		final Map<Long, JalurData> validasiPerJalur = new LinkedHashMap<Long, JalurData>();
		List<JenisSeleksi> jalur = getJalurAktif(session);
		for (int i = 0; i < jalur.size(); i++) {
			JenisSeleksi js = jalur.get(i);
			JalurData data = dataPerJalur.get(js.getId());
			if (data == null) {
				data = new JalurData(js.getId(), js.getNama(), 0);
			}
			Row row = new Row();
			row.setValign("middle");
			row.setParent(rows);
			new Label(js.getNama()).setParent(row);
			new Label(fmtAngka(data.peminatEcampus)).setParent(row);
			Intbox nilai = new Intbox();
			nilai.setWidth("95%");
			nilai.setConstraint("no negative");
			nilai.setTooltiptext("Kosong = otomatis dari eCampus");
			nilai.setValue(data.peminatAsliEksternal);
			nilai.setParent(row);
			new Label(fmtAngka(data.peminat)).setParent(row);
			new Label(data.peminatAsliEksternal == null ? "eCampus" : "Eksternal").setParent(row);
			inputPerJalur.put(js.getId(), nilai);
			validasiPerJalur.put(js.getId(), data);
		}

		Hbox actions = new Hbox();
		actions.setStyle("margin-top:12px;gap:8px;justify-content:center;");
		actions.setParent(root);
		MyButtonConfig simpan = new MyButtonConfig("Simpan & Muat Ulang", "/img/save.gif");
		simpan.setParent(actions);
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				for (Map.Entry<Long, Intbox> entry : inputPerJalur.entrySet()) {
					Integer nilai = entry.getValue().getValue();
					JalurData data = validasiPerJalur.get(entry.getKey());
					int minimum = Math.max(data == null ? 0 : data.peminatEcampus,
							Math.max(data == null ? 0 : data.diterima, data == null ? 0 : data.nim));
					if (nilai != null && nilai.intValue() < minimum) {
						MyMessageboxConfig.show("Peminat asli untuk " + data.nama + " minimal " + fmtAngka(minimum)
								+ " karena tidak boleh lebih kecil dari data eCampus/diterima/NIM yang sudah ada.");
						entry.getValue().focus();
						return;
					}
				}
				for (Map.Entry<Long, Intbox> entry : inputPerJalur.entrySet()) {
					simpanPeminatAsli(session, ta, sem, entry.getKey(), entry.getValue().getValue());
				}
				session.flush();
				window.detach();
				refresh();
				MyMessageboxConfig.show("Data peminat asli berhasil disimpan dan Dasbor Jalur Masuk telah dimuat ulang.");
			}
		});
		MyButtonConfig batal = new MyButtonConfig("Batal", "/img/cancel.gif");
		batal.setParent(actions);
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setVisible(true);
		window.onModal();
	}

	private void simpanPeminatAsli(Session session, String ta, String sem, Long jenisSeleksiId, Integer nilai) {
		Konfigurasi konfigurasi = findPeminatConfig(session, ta, sem, jenisSeleksiId);
		if (konfigurasi == null) {
			if (nilai == null) {
				return;
			}
			konfigurasi = new Konfigurasi(KONFIG_PEMINAT_ASLI, NILAI_OTOMATIS);
			konfigurasi.setTahunAkademik(ta);
			konfigurasi.setInfo1(jenisSeleksiId.toString());
			konfigurasi.setInfo2(semesterKey(sem));
			konfigurasi.setInfo3("SUMBER_PEMINAT_DASBOR_PMB");
		}
		konfigurasi.setNilai(nilai == null ? NILAI_OTOMATIS : nilai.toString());
		konfigurasi.setKeterangan("Jumlah peminat asli jalur seleksi dari portal eksternal; "
				+ "AUTO berarti memakai hitungan Biodata Calon Mahasiswa eCampus.");
		Common.refreshUpdate(session, konfigurasi, false);
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
