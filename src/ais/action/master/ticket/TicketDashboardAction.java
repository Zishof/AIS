package ais.action.master.ticket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import ais.database.model.ticket.Ticket;
import ais.ui.util.DashboardGridExportHelper;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyWindow;

/**
 * <h3>Dashboard &amp; Report Ticketing</h3>
 *
 * <p>Ringkasan kinerja tiket dalam bentuk KPI + grafik HTML/CSS (tanpa JFreeChart, pola
 * {@link HtmlChartHelper}) + tabel rincian yang bisa dicetak PDF / diekspor Excel
 * ({@link DashboardGridExportHelper}). Data mengikuti scoping yang sama dengan
 * {@link TicketingAction#scopedCriteria(Session, Tbmuser)} — admin/developer melihat semua,
 * selain itu hanya tiket dalam lingkup pengguna. Dipasang lewat menu (url = nama kelas). Java 1.7.
 */
public class TicketDashboardAction extends MyWindow {

	private static final long serialVersionUID = 3120250724012L;

	private static final String[] STATUS_URUT = { Ticket.STATUS_BARU, Ticket.STATUS_DITINJAU, Ticket.STATUS_DIPROSES,
			Ticket.STATUS_MENUNGGU_RESPON, Ticket.STATUS_SELESAI, Ticket.STATUS_DITUTUP, Ticket.STATUS_DITOLAK };
	private static final String[] STATUS_WARNA = { "#0ea5e9", "#6366f1", "#2563eb", "#d97706", "#16a34a", "#64748b",
			"#dc2626" };
	private static final String[] TIPE_URUT = { Ticket.TIPE_KENDALA, Ticket.TIPE_PERMINTAAN, Ticket.TIPE_PROGRESS,
			Ticket.TIPE_INTERAKSI, Ticket.TIPE_LAINNYA };
	private static final String[] PRIORITAS_URUT = { Ticket.PRIORITAS_KRITIS, Ticket.PRIORITAS_TINGGI,
			Ticket.PRIORITAS_SEDANG, Ticket.PRIORITAS_RENDAH };

	public TicketDashboardAction() {
		super();
		try {
			setHeight("100%");
			setWidth("100%");
			setBorder("none");
			setClosable(false);
			setPosition("center");

			org.zkoss.zul.Borderlayout borderlayout = new org.zkoss.zul.Borderlayout();
			borderlayout.setParent(this);
			org.zkoss.zul.Center center = new org.zkoss.zul.Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.setAutoscroll(true);

			render(center, Common.getCurrentUser());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Dashboard — dua tab: "Ticketing" (asli) dan "CRM" (digabung, lihat {@link CrmDashboardHelper}). */
	private static void render(Component parent, Tbmuser tbmuser) {
		try {
			Common.clear(parent);

			org.zkoss.zul.Tabbox tabbox = new org.zkoss.zul.Tabbox();
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");
			tabbox.setParent(parent);
			org.zkoss.zul.Tabs tabs = new org.zkoss.zul.Tabs();
			tabs.setParent(tabbox);
			new org.zkoss.zul.Tab("Ticketing").setParent(tabs);
			new org.zkoss.zul.Tab("CRM").setParent(tabs);
			org.zkoss.zul.Tabpanels tabpanels = new org.zkoss.zul.Tabpanels();
			tabpanels.setParent(tabbox);
			org.zkoss.zul.Tabpanel panelTiket = new org.zkoss.zul.Tabpanel();
			ais.ui.util.ZkCompat.setFlex(panelTiket, true);
			panelTiket.setParent(tabpanels);
			org.zkoss.zul.Tabpanel panelCrm = new org.zkoss.zul.Tabpanel();
			ais.ui.util.ZkCompat.setFlex(panelCrm, true);
			panelCrm.setParent(tabpanels);

			renderTiket(panelTiket, tbmuser);
			CrmDashboardHelper.display(panelCrm, tbmuser);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static void renderTiket(Component parent, Tbmuser tbmuser) {
		try {
			MyDiv root = new MyDiv();
			root.setStyle("padding:12px;box-sizing:border-box;");
			root.setParent(parent);

			root.appendChild(new MyHtml(
					"<div style='font-size:18px;font-weight:800;color:#0f172a;margin-bottom:10px;'>Dashboard &amp; Report Ticketing</div>"));

			Session session = HibernateUtil.currentSession();
			List<Ticket> tickets = TicketingAction.scopedCriteria(session, tbmuser).addOrder(Order.desc("id"))
					.setMaxResults(5000).list();
			if (tickets == null) {
				tickets = new ArrayList<Ticket>();
			}

			// --- Agregasi ---
			Map<String, Integer> perStatus = hitung(STATUS_URUT);
			Map<String, Integer> perTipe = hitung(TIPE_URUT);
			Map<String, Integer> perPrioritas = hitung(PRIORITAS_URUT);
			int total = tickets.size();
			int selesai = 0;
			int terbuka = 0;
			long sumProgress = 0;
			for (Ticket t : tickets) {
				tambah(perStatus, t.getStatus());
				tambah(perTipe, t.getTipe());
				tambah(perPrioritas, t.getPrioritas());
				if (Ticket.STATUS_SELESAI.equals(t.getStatus()) || Ticket.STATUS_DITUTUP.equals(t.getStatus())) {
					selesai++;
				} else if (!Ticket.STATUS_DITOLAK.equals(t.getStatus())) {
					terbuka++;
				}
				sumProgress += (t.getProgress() == null ? 0 : t.getProgress().intValue());
			}
			int rataProgress = total == 0 ? 0 : (int) (sumProgress / total);
			int persenSelesai = total == 0 ? 0 : (selesai * 100 / total);

			// --- KPI ---
			String[] kpiLabel = { "Total Tiket", "Sedang Berjalan", "Selesai", "Rata-rata Progress" };
			String[] kpiNilai = { String.valueOf(total), String.valueOf(terbuka), String.valueOf(selesai),
					rataProgress + "%" };
			String[] kpiSub = { "seluruh tiket dalam lingkup", "belum selesai/ditutup",
					persenSelesai + "% dari total", "keseluruhan tiket" };
			String[] kpiWarna = { "#2563eb", "#d97706", "#16a34a", "#0ea5e9" };
			root.appendChild(new MyHtml(HtmlChartHelper.kpiCards(kpiLabel, kpiNilai, kpiSub, null, null, kpiWarna)));

			// --- Grafik ---
			MyDiv charts = new MyDiv();
			charts.setStyle("display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:12px;margin-top:12px;");
			charts.setParent(root);

			charts.appendChild(new MyHtml(HtmlChartHelper.donut("Distribusi Status",
					"Jumlah tiket per status penanganan.", STATUS_URUT, nilai(perStatus, STATUS_URUT), STATUS_WARNA,
					total + " tiket")));
			charts.appendChild(new MyHtml(HtmlChartHelper.barHorizontal("Tiket per Tipe",
					"Perbandingan jumlah tiket berdasarkan jenisnya.", TIPE_URUT, nilai(perTipe, TIPE_URUT), "#6366f1")));
			charts.appendChild(new MyHtml(HtmlChartHelper.barHorizontal("Tiket per Prioritas",
					"Sebaran beban berdasarkan tingkat prioritas.", PRIORITAS_URUT,
					nilai(perPrioritas, PRIORITAS_URUT), "#dc2626")));

			// --- Tabel rincian + ekspor ---
			root.appendChild(new MyHtml(
					"<div style='font-size:14px;font-weight:700;color:#0f172a;margin:16px 0 6px;'>Rincian Tiket</div>"));

			Hbox toolbarEkspor = new Hbox();
			toolbarEkspor.setStyle("padding:2px 0 6px;");
			toolbarEkspor.setParent(root);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(root);
			Columns cols = new Columns();
			cols.setParent(grid);
			new Column("Nomor", null, "120px").setParent(cols);
			new Column("Judul").setParent(cols);
			new Column("Tipe", null, "150px").setParent(cols);
			new Column("Prioritas", null, "90px").setParent(cols);
			new Column("Status", null, "130px").setParent(cols);
			new Column("Progress", null, "80px").setParent(cols);
			new Column("Pengaju", null, "160px").setParent(cols);
			Rows rows = new Rows();
			rows.setParent(grid);
			for (Ticket t : tickets) {
				Row r = new Row();
				r.setParent(rows);
				r.appendChild(new Label(safe(t.getNomorTiket())));
				r.appendChild(new Label(safe(t.getJudul())));
				r.appendChild(new Label(safe(t.getTipe())));
				r.appendChild(new Label(safe(t.getPrioritas())));
				r.appendChild(new Label(safe(t.getStatus())));
				r.appendChild(new Label((t.getProgress() == null ? 0 : t.getProgress()) + "%"));
				r.appendChild(new Label(safe(t.getPengajuNama())));
			}

			try {
				DashboardGridExportHelper.pasangGrup(toolbarEkspor, grid, "Laporan Tiket");
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketDashboardAction.export");
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static Map<String, Integer> hitung(String[] kunci) {
		Map<String, Integer> m = new LinkedHashMap<String, Integer>();
		for (String k : kunci) {
			m.put(k, 0);
		}
		return m;
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

	private static String safe(String s) {
		return s == null ? "" : s;
	}
}
