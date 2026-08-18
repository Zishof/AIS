package ais.action.master.ticket;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.crm.CrmLead;
import ais.ui.util.DashboardGridExportHelper;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;

/**
 * <h3>CrmDashboardHelper — laporan pipeline CRM dasar (tab "CRM" di {@link TicketDashboardAction})</h3>
 *
 * <p>KPI + grafik HTML/CSS (pola {@link HtmlChartHelper}, tanpa JFreeChart) + tabel rincian
 * exportable ({@link DashboardGridExportHelper}). Fase 1 — tanpa Forecast/Cohort/Atribusi Marketing.
 * Java 1.7.</p>
 */
public final class CrmDashboardHelper {

	private CrmDashboardHelper() {
	}

	@SuppressWarnings("unchecked")
	public static void display(Component parent, Tbmuser tbmuser) {
		try {
			Common.clear(parent);
			MyDiv root = new MyDiv();
			root.setStyle("padding:12px;box-sizing:border-box;");
			root.setParent(parent);

			root.appendChild(new MyHtml(
					"<div style='font-size:18px;font-weight:800;color:#0f172a;margin-bottom:10px;'>Dashboard & Report CRM</div>"));

			Session session = HibernateUtil.currentSession();
			List<CrmLead> leads = session.createCriteria(CrmLead.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.desc("id")).setMaxResults(5000).list();
			if (leads == null) {
				leads = new ArrayList<CrmLead>();
			}

			int totalOpen = 0;
			int totalWon = 0;
			int totalLost = 0;
			int wonBulanIni = 0;
			BigDecimal estimasiOpen = BigDecimal.ZERO;
			BigDecimal nilaiWonBulanIni = BigDecimal.ZERO;
			Map<String, Integer> perTipe = new LinkedHashMap<String, Integer>();
			perTipe.put("Lead", 0);
			perTipe.put("Peluang", 0);
			Map<String, Integer> perPipeline = new LinkedHashMap<String, Integer>();
			Map<String, Integer> perTim = new LinkedHashMap<String, Integer>();

			Calendar awalBulan = Calendar.getInstance();
			awalBulan.set(Calendar.DAY_OF_MONTH, 1);
			awalBulan.set(Calendar.HOUR_OF_DAY, 0);
			awalBulan.set(Calendar.MINUTE, 0);
			awalBulan.set(Calendar.SECOND, 0);

			for (CrmLead l : leads) {
				tambah(perTipe, CrmLead.TIPE_PELUANG.equals(l.getTipe()) ? "Peluang" : "Lead");
				if (l.getPipelineType() != null) {
					tambah(perPipeline, safe(l.getPipelineType().getNama()));
				}
				if (l.getSalesTeam() != null) {
					tambah(perTim, safe(l.getSalesTeam().getNama()));
				}
				String status = l.getStatusMenangKalah();
				if (CrmLead.STATUS_WON.equals(status)) {
					totalWon++;
					if (l.getTanggalDitutup() != null && l.getTanggalDitutup().after(awalBulan.getTime())) {
						wonBulanIni++;
						if (l.getNilaiEstimasi() != null) {
							nilaiWonBulanIni = nilaiWonBulanIni.add(l.getNilaiEstimasi());
						}
					}
				} else if (CrmLead.STATUS_LOST.equals(status)) {
					totalLost++;
				} else {
					totalOpen++;
					if (l.getNilaiEstimasi() != null) {
						estimasiOpen = estimasiOpen.add(l.getNilaiEstimasi());
					}
				}
			}
			int totalTutup = totalWon + totalLost;
			int winRate = totalTutup == 0 ? 0 : (totalWon * 100 / totalTutup);

			String[] kpiLabel = { "Lead/Peluang Terbuka", "Win Rate", "Estimasi Nilai Terbuka", "Menang Bulan Ini" };
			String[] kpiNilai = { String.valueOf(totalOpen), winRate + "%", formatRupiah(estimasiOpen),
					String.valueOf(wonBulanIni) };
			String[] kpiSub = { "belum menang/kalah", totalTutup + " pipeline sudah ditutup",
					"total dari pipeline terbuka", formatRupiah(nilaiWonBulanIni) + " nilai menang bulan ini" };
			String[] kpiWarna = { "#2563eb", "#16a34a", "#0ea5e9", "#d97706" };
			root.appendChild(new MyHtml(HtmlChartHelper.kpiCards(kpiLabel, kpiNilai, kpiSub, null, null, kpiWarna)));

			MyDiv charts = new MyDiv();
			charts.setStyle("display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:12px;margin-top:12px;");
			charts.setParent(root);

			String[] tipeKey = perTipe.keySet().toArray(new String[0]);
			charts.appendChild(new MyHtml(HtmlChartHelper.donut("Lead vs Peluang",
					"Perbandingan jumlah data mentah (Lead) dan yang sudah dikualifikasi (Peluang).", tipeKey,
					nilai(perTipe, tipeKey), new String[] { "#94a3b8", "#2563eb" }, leads.size() + " data")));

			String[] pipelineKey = perPipeline.keySet().toArray(new String[0]);
			if (pipelineKey.length > 0) {
				charts.appendChild(new MyHtml(HtmlChartHelper.barHorizontal("Per Jenis Pipeline",
						"Sebaran jumlah data berdasarkan jenis pipeline.", pipelineKey, nilai(perPipeline, pipelineKey),
						"#6366f1")));
			}
			String[] timKey = perTim.keySet().toArray(new String[0]);
			if (timKey.length > 0) {
				charts.appendChild(new MyHtml(HtmlChartHelper.barHorizontal("Per Tim Penjualan",
						"Sebaran jumlah data berdasarkan tim yang menangani.", timKey, nilai(perTim, timKey), "#0ea5e9")));
			}

			root.appendChild(new MyHtml(
					"<div style='font-size:14px;font-weight:700;color:#0f172a;margin:16px 0 6px;'>Rincian Pipeline</div>"));

			Hbox toolbarEkspor = new Hbox();
			toolbarEkspor.setStyle("padding:2px 0 6px;");
			toolbarEkspor.setParent(root);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(root);
			Columns cols = new Columns();
			cols.setParent(grid);
			new Column("Judul").setParent(cols);
			new Column("Pipeline", null, "150px").setParent(cols);
			new Column("Tahap", null, "120px").setParent(cols);
			new Column("Tim", null, "130px").setParent(cols);
			new Column("PIC", null, "140px").setParent(cols);
			new Column("Estimasi", null, "120px").setParent(cols);
			new Column("Status", null, "90px").setParent(cols);
			Rows rows = new Rows();
			rows.setParent(grid);
			for (CrmLead l : leads) {
				Row r = new Row();
				r.setParent(rows);
				r.appendChild(new Label(safe(l.getJudul())));
				r.appendChild(new Label(l.getPipelineType() == null ? "" : safe(l.getPipelineType().getNama())));
				r.appendChild(new Label(l.getStage() == null ? "" : safe(l.getStage().getNama())));
				r.appendChild(new Label(l.getSalesTeam() == null ? "" : safe(l.getSalesTeam().getNama())));
				r.appendChild(new Label(l.getDitugaskanUser() == null ? "" : safe(l.getDitugaskanUser().getUserNama())));
				r.appendChild(new Label(formatRupiah(l.getNilaiEstimasi())));
				r.appendChild(new Label(labelStatus(l.getStatusMenangKalah())));
			}

			try {
				DashboardGridExportHelper.pasangGrup(toolbarEkspor, grid, "Laporan Pipeline CRM");
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmDashboardHelper.export");
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void tambah(Map<String, Integer> m, String kunci) {
		if (kunci == null) {
			return;
		}
		Integer v = m.get(kunci);
		m.put(kunci, (v == null ? 0 : v) + 1);
	}

	private static double[] nilai(Map<String, Integer> m, String[] urut) {
		double[] v = new double[urut.length];
		for (int i = 0; i < urut.length; i++) {
			Integer n = m.get(urut[i]);
			v[i] = n == null ? 0 : n.doubleValue();
		}
		return v;
	}

	private static String labelStatus(String status) {
		if (CrmLead.STATUS_WON.equals(status)) {
			return "Menang";
		}
		if (CrmLead.STATUS_LOST.equals(status)) {
			return "Kalah";
		}
		return "Terbuka";
	}

	private static String formatRupiah(BigDecimal v) {
		if (v == null) {
			v = BigDecimal.ZERO;
		}
		try {
			return "Rp " + String.format("%,.0f", v);
		} catch (Exception e) {
			return "Rp " + v.toString();
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}
}
