package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.DashboardReportKit;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>InventarisDashboard — Rekap &amp; Grafik "Draft Inventaris" (Barang Tidak Habis Pakai)</h3>
 *
 * <p>Tampilan dashboard <i>eye-catching</i> untuk sub-tab "Draft Inventaris" pada menu BAST
 * (Penerimaan Barang/Jasa), menggantikan grid polos {@code monitor_barang_tidak_habis_pakai_asset.zul}.
 * Format mengikuti template {@link TraceStatusPengadaanAssetDashboard} (panel + kartu ringkasan +
 * grafik HTML/CSS + grid).</p>
 *
 * <p>Data = {@link PenerimaanPengadaanMasterAssetDetail} untuk BAST yang sudah disetujui dan
 * {@code masterAsset.tipe = }{@link MasterAsset#TIPE_TIDAK_HABIS_PAKAI} (barang durable / inventaris) —
 * sama persis dengan filter {@code MonitorBarangTidakHabisPakaiAction.initCriteria}.</p>
 */
public class InventarisDashboard extends Vbox {

	private static final long serialVersionUID = 8931551047723390014L;

	private static final int GRID_PAGE_SIZE = 50;
	private static final int LIMIT = 5000;

	private Textbox keyword;
	private MyDatebox tglMulai;
	private MyDatebox tglSampai;
	private Vbox body;

	private static class Item {
		String kodeBast;
		String kodePo;
		String kodeBarang;
		String namaBarang;
		String penyedia;
		String satuanKerja;
		double diterima;
		Date tanggal;
		Long bastId;
	}

	public InventarisDashboard() {
		setWidth("100%");
		setHeight("100%");
		setStyle("overflow:auto;background:#f8fafc;padding:10px;box-sizing:border-box;");
		buildLayout();
		reload();
	}

	private void buildLayout() {
		Toolbar toolbar = new Toolbar();
		toolbar.setWidth("100%");
		toolbar.setStyle("padding:10px;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;margin-bottom:10px;");
		toolbar.setParent(this);

		new Label("Cari Kode/Nama Barang / Kode BAST: ").setParent(toolbar);
		keyword = new Textbox();
		keyword.setWidth("240px");
		keyword.setParent(toolbar);
		keyword.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload();
			}
		});

		new Label(ais.common.Common.getBahasaConfig(" Tgl Mulai: ")).setParent(toolbar);
		tglMulai = new MyDatebox();
		tglMulai.setWidth("130px");
		tglMulai.setParent(toolbar);
		new Label(" s/d Tgl Sampai: ").setParent(toolbar);
		tglSampai = new MyDatebox();
		tglSampai.setWidth("130px");
		tglSampai.setParent(toolbar);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Tampilkan", "/img/search.gif");
		cari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload();
			}
		});
		cari.setParent(toolbar);

		// Cetak PDF-ready + Ekspor Excel + progress bar (mesin reuse DashboardReportKit).
		DashboardReportKit.pasangTombol(toolbar, this, buatSumberLaporan());

		body = new Vbox();
		body.setWidth("100%");
		body.setParent(this);
	}

	/** Deskripsi laporan (Cetak PDF + Ekspor Excel + progress) via {@link DashboardReportKit}. */
	private DashboardReportKit.SumberLaporan buatSumberLaporan() {
		return new DashboardReportKit.SumberLaporan() {
			@Override public String judul() { return "Draft Inventaris"; }
			@Override public String subjudul() { return "Barang Tidak Habis Pakai (Durable)"; }
			@Override public String deskripsi() { return "Rekap penerimaan barang inventaris (aset tetap) dari BAST yang sudah disetujui."; }
			@Override public java.util.List<DashboardReportKit.Bagian> bagian() {
				final java.util.List<Item> data = loadData();
				java.util.List<DashboardReportKit.Bagian> b = new java.util.ArrayList<DashboardReportKit.Bagian>();
				b.add(DashboardReportKit.kpi("Ringkasan", "Jumlah item inventaris, total unit diterima, jumlah BAST dan penyedia.",
					new DashboardReportKit.PenyediaBaris() { @Override public java.util.List<Object[]> ambil() {
						double unit=0; java.util.Set<String> bast=new java.util.HashSet<String>(), ven=new java.util.HashSet<String>(), sk=new java.util.HashSet<String>();
						for (Item x:data){ unit+=x.diterima; if(x.kodeBast!=null&&!x.kodeBast.trim().isEmpty())bast.add(x.kodeBast.trim()); if(x.penyedia!=null&&!x.penyedia.trim().isEmpty())ven.add(x.penyedia.trim()); if(x.satuanKerja!=null&&!x.satuanKerja.trim().isEmpty())sk.add(x.satuanKerja.trim()); }
						java.util.List<Object[]> o=new java.util.ArrayList<Object[]>();
						o.add(new Object[]{"Total Item", DashboardReportKit.fmt(data.size()), "baris barang"});
						o.add(new Object[]{"Total Unit Diterima", number(unit), "akumulasi unit"});
						o.add(new Object[]{"Jumlah BAST", DashboardReportKit.fmt(bast.size()), "dokumen"});
						o.add(new Object[]{"Jumlah Penyedia", DashboardReportKit.fmt(ven.size()), "vendor"});
						o.add(new Object[]{"Satuan Kerja", DashboardReportKit.fmt(sk.size()), "unit"});
						return o; } }));
				b.add(DashboardReportKit.batang("Jumlah Item per Satuan Kerja", "Unit kerja mana yang paling banyak menerima barang inventaris.",
					new String[]{"Satuan Kerja","Jumlah Item"}, new DashboardReportKit.PenyediaBaris() { @Override public java.util.List<Object[]> ambil() {
						java.util.Map<String,Integer> m=new java.util.LinkedHashMap<String,Integer>();
						for (Item x:data){ String k=(x.satuanKerja==null||x.satuanKerja.trim().isEmpty())?"(Tanpa Satuan Kerja)":x.satuanKerja.trim(); Integer v=m.get(k); m.put(k,(v==null?0:v)+1); }
						java.util.List<Object[]> o=new java.util.ArrayList<Object[]>();
						for (java.util.Map.Entry<String,Integer> e:m.entrySet()) o.add(new Object[]{e.getKey(), Long.valueOf(e.getValue())});
						return o; } }));
				b.add(DashboardReportKit.tabel("Rincian Barang Inventaris", "Daftar barang inventaris yang diterima.",
					new String[]{"Kode BAST","No. PO","Kode Barang","Nama Barang","Penyedia","Satuan Kerja","Diterima","Tanggal"},
					new DashboardReportKit.PenyediaBaris() { @Override public java.util.List<Object[]> ambil() {
						java.util.List<Object[]> o=new java.util.ArrayList<Object[]>();
						for (Item x:data) o.add(new Object[]{ dash(x.kodeBast), dash(x.kodePo), dash(x.kodeBarang), dash(x.namaBarang), dash(x.penyedia), dash(x.satuanKerja), number(x.diterima),
							x.tanggal==null?"-":Common.dateFormat3.get().format(x.tanggal) });
						return o; } }));
				return b;
			}
		};
	}

	private void reload() {
		Common.clear(body);
		List<Item> items = loadData();
		renderHeader(body);
		renderSummary(body, items);
		renderChart(body, items);
		renderGrid(body, items);
	}

	private void renderHeader(Component parent) {
		Panel panel = createPanel(parent, "Draft Inventaris — Barang Tidak Habis Pakai (Durable)", null);
		Panelchildren c = firstChild(panel);
		c.appendChild(new Html("<div style='font-family:Arial,sans-serif;color:#334155;font-size:12px;line-height:1.55;'>"
				+ "Rekapitulasi penerimaan barang <b>tidak habis pakai</b> (inventaris/aset tetap) dari BAST yang sudah "
				+ "disetujui. Menampilkan jumlah item, total unit diterima, sebaran per satuan kerja, dan rincian barang."
				+ "</div>"));
	}

	private void renderSummary(Component parent, List<Item> items) {
		int totalItem = items.size();
		double totalUnit = 0.0;
		java.util.Set<String> bast = new java.util.HashSet<String>();
		java.util.Set<String> vendor = new java.util.HashSet<String>();
		java.util.Set<String> satker = new java.util.HashSet<String>();
		for (Item it : items) {
			totalUnit += it.diterima;
			if (it.kodeBast != null && !it.kodeBast.trim().isEmpty()) {
				bast.add(it.kodeBast.trim());
			}
			if (it.penyedia != null && !it.penyedia.trim().isEmpty()) {
				vendor.add(it.penyedia.trim());
			}
			if (it.satuanKerja != null && !it.satuanKerja.trim().isEmpty()) {
				satker.add(it.satuanKerja.trim());
			}
		}
		Panel panel = createPanel(parent, "Ringkasan", "margin-top:10px;");
		Panelchildren c = firstChild(panel);
		String htmlStr = "<div style='font-family:Arial,sans-serif;'>"
				+ "<div style='display:flex;gap:10px;flex-wrap:wrap;'>"
				+ card("Total Item Inventaris", String.valueOf(totalItem), "baris barang diterima", "#1d4ed8")
				+ card("Total Unit Diterima", number(totalUnit), "akumulasi jumlah unit", "#0369a1")
				+ card("Jumlah BAST", String.valueOf(bast.size()), "dokumen penerimaan", "#4338ca")
				+ card("Jumlah Penyedia", String.valueOf(vendor.size()), "vendor terlibat", "#0f766e")
				+ card("Satuan Kerja", String.valueOf(satker.size()), "unit tujuan barang", "#b45309")
				+ "</div></div>";
		c.appendChild(new Html(htmlStr));
	}

	private void renderChart(Component parent, List<Item> items) {
		// Agregasi jumlah item per Satuan Kerja.
		Map<String, Integer> perSatker = new LinkedHashMap<String, Integer>();
		for (Item it : items) {
			String key = it.satuanKerja == null || it.satuanKerja.trim().isEmpty() ? "(Tanpa Satuan Kerja)"
					: it.satuanKerja.trim();
			Integer v = perSatker.get(key);
			perSatker.put(key, (v == null ? 0 : v) + 1);
		}
		Panel panel = createPanel(parent, "Grafik Jumlah Item per Satuan Kerja", "margin-top:10px;");
		Panelchildren c = firstChild(panel);
		if (perSatker.isEmpty()) {
			c.appendChild(new Html("<div style='font-size:12px;color:#64748b;'>Belum ada data untuk ditampilkan.</div>"));
			return;
		}
		int max = 0;
		for (Integer v : perSatker.values()) {
			if (v != null && v.intValue() > max) {
				max = v.intValue();
			}
		}
		if (max <= 0) {
			max = 1;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:Arial,sans-serif;'>");
		for (Map.Entry<String, Integer> e : perSatker.entrySet()) {
			int v = e.getValue() == null ? 0 : e.getValue().intValue();
			int pct = (int) Math.round((double) v / max * 100.0);
			if (pct < 2 && v > 0) {
				pct = 2;
			}
			sb.append("<div style='margin-bottom:8px;'>")
					.append("<div style='display:flex;justify-content:space-between;font-size:12px;color:#334155;margin-bottom:3px;'>")
					.append("<span style='font-weight:600;'>").append(html(e.getKey())).append("</span>")
					.append("<span>").append(v).append(" item</span></div>")
					.append("<div style='background:#e2e8f0;border-radius:8px;height:16px;overflow:hidden;'>")
					.append("<div style='width:").append(pct).append("%;height:16px;background:linear-gradient(90deg,#22c55e,#15803d);border-radius:8px;'></div>")
					.append("</div></div>");
		}
		sb.append("</div>");
		c.appendChild(new Html(sb.toString()));
	}

	private void renderGrid(Component parent, List<Item> items) {
		Panel panel = createPanel(parent, "Rincian Barang Inventaris Diterima", "margin-top:10px;");
		Panelchildren c = firstChild(panel);

		MyGrid grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(GRID_PAGE_SIZE);
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setParent(c);

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig cNo = new MyColumnConfig("No");
		cNo.setWidth("45px");
		cNo.setAlign("center");
		cNo.setParent(columns);
		new MyColumnConfig("Kode BAST").setParent(columns);
		new MyColumnConfig("No. PO").setParent(columns);
		new MyColumnConfig("Kode Barang").setParent(columns);
		new MyColumnConfig("Nama Barang").setParent(columns);
		new MyColumnConfig("Penyedia").setParent(columns);
		new MyColumnConfig("Satuan Kerja").setParent(columns);
		MyColumnConfig cDiterima = new MyColumnConfig("Diterima");
		cDiterima.setAlign("right");
		cDiterima.setParent(columns);
		new MyColumnConfig("Tanggal").setParent(columns);
		MyColumnConfig cAksi = new MyColumnConfig("Aksi");
		cAksi.setWidth("150px");
		cAksi.setAlign("center");
		cAksi.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);
		int no = 0;
		for (Item it : items) {
			no++;
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(String.valueOf(no)));
			row.appendChild(new Label(dash(it.kodeBast)));
			row.appendChild(new Label(dash(it.kodePo)));
			row.appendChild(new Label(dash(it.kodeBarang)));
			row.appendChild(new Label(dash(it.namaBarang)));
			row.appendChild(new Label(dash(it.penyedia)));
			row.appendChild(new Label(dash(it.satuanKerja)));
			Label lblDiterima = new Label(number(it.diterima));
			lblDiterima.setStyle("text-align:right;display:block;");
			row.appendChild(lblDiterima);
			row.appendChild(new Label(it.tanggal == null ? "-" : Common.dateFormat3.get().format(it.tanggal)));
			final Long bastId = it.bastId;
			MyToolbarbuttonConfig btnInv = new MyToolbarbuttonConfig("Jadikan Inventaris", "/img/svg/edit-box-line.svg");
			btnInv.setTooltiptext("Kelola / Jadikan Inventaris untuk BAST ini");
			btnInv.setDisabled(bastId == null);
			btnInv.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					KelolaInventarisModalHelper.buka(bastId);
				}
			});
			row.appendChild(btnInv);
		}
		if (items.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "10");
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada barang inventaris pada rentang ini.")));
		}
	}

	@SuppressWarnings("unchecked")
	private List<Item> loadData() {
		List<Item> hasil = new ArrayList<Item>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Criteria criteria = session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
					.createAlias("masterAsset", "masterAsset")
					.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset")
					.add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset.disetujuiOleh"))
					.add(Restrictions.eq("masterAsset.tipe", MasterAsset.TIPE_TIDAK_HABIS_PAKAI));

			String q = keyword == null ? "" : safe(keyword.getValue());
			if (!q.isEmpty()) {
				criteria.add(Restrictions.or(
						Restrictions.ilike("penerimaanPengadaanMasterAsset.kode", q, MatchMode.ANYWHERE),
						Restrictions.or(Restrictions.ilike("masterAsset.kode", q, MatchMode.ANYWHERE),
								Restrictions.ilike("masterAsset.nama", q, MatchMode.ANYWHERE))));
			}
			Date d1 = tglMulai == null ? null : tglMulai.getValue();
			Date d2 = tglSampai == null ? null : tglSampai.getValue();
			if (d1 != null) {
				criteria.add(Restrictions.ge("penerimaanPengadaanMasterAsset.tanggalPembuatan", awalHari(d1)));
			}
			if (d2 != null) {
				criteria.add(Restrictions.le("penerimaanPengadaanMasterAsset.tanggalPembuatan", akhirHari(d2)));
			}
			criteria.addOrder(Order.desc("id"));
			criteria.setMaxResults(LIMIT);

			List<PenerimaanPengadaanMasterAssetDetail> list = criteria.list();
			for (PenerimaanPengadaanMasterAssetDetail d : list) {
				if (d == null) {
					continue;
				}
				Item it = new Item();
				try { it.kodeBast = d.getPenerimaanPengadaanMasterAsset().getKode(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/InventarisDashboard.java:365"); }
				try {
					it.kodePo = d.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset() == null ? ""
							: d.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getKode();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/InventarisDashboard.java:369"); }
				try { it.kodeBarang = d.getMasterAsset().getKode(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/InventarisDashboard.java:370"); }
				try { it.namaBarang = d.getMasterAsset().getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/InventarisDashboard.java:371"); }
				try {
					it.penyedia = d.getPenerimaanPengadaanMasterAsset().getPenyedia() == null ? ""
							: d.getPenerimaanPengadaanMasterAsset().getPenyedia().getNama();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/InventarisDashboard.java:375"); }
				try {
					it.satuanKerja = d.getPenerimaanPengadaanMasterAsset().getSatuanKerja() == null ? ""
							: d.getPenerimaanPengadaanMasterAsset().getSatuanKerja().getNama();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/InventarisDashboard.java:379"); }
				try { it.diterima = d.getDiterima() == null ? 0.0 : d.getDiterima().doubleValue(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/InventarisDashboard.java:380"); }
				try {
					it.tanggal = d.getPenerimaanPengadaanMasterAsset().getTanggalPersetujuan() != null
							? d.getPenerimaanPengadaanMasterAsset().getTanggalPersetujuan()
							: d.getPenerimaanPengadaanMasterAsset().getTanggalPembuatan();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/InventarisDashboard.java:385"); }
				try { it.bastId = d.getPenerimaanPengadaanMasterAsset().getId(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/InventarisDashboard.java:386"); }
				hasil.add(it);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/InventarisDashboard.java:393"); }
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/InventarisDashboard.java:394"); }
			}
		}
		return hasil;
	}

	private Date awalHari(Date d) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	private Date akhirHari(Date d) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
		return cal.getTime();
	}

	// ---------------------------------------------------------------- helper tampilan (pola Trace)

	private Panel createPanel(Component parent, String title, String style) {
		Panel panel = new Panel();
		panel.setTitle(title);
		panel.setBorder("normal");
		panel.setCollapsible(true);
		panel.setStyle((style == null ? "" : style) + "background:white;border-radius:14px;overflow:hidden;");
		panel.setParent(parent);
		Panelchildren children = new Panelchildren();
		children.setStyle("padding:12px;background:#f8fafc;");
		children.setParent(panel);
		return panel;
	}

	private Panelchildren firstChild(Panel panel) {
		return (Panelchildren) panel.getChildren().get(0);
	}

	private String card(String title, String value, String desc, String color) {
		title = ais.common.Common.getBahasaConfig(title);
		desc = ais.common.Common.getBahasaConfig(desc);
		return "<div style='background:white;border:1px solid #e2e8f0;border-radius:14px;padding:13px;box-shadow:0 2px 8px rgba(15,23,42,0.07);min-width:150px;flex:1;'>"
				+ "<div style='font-size:11px;color:#64748b;font-weight:700;text-transform:uppercase;'>" + html(title) + "</div>"
				+ "<div style='font-size:19px;color:" + color + ";font-weight:800;margin-top:5px;'>" + html(value) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;margin-top:4px;line-height:1.35;'>" + html(desc) + "</div></div>";
	}

	private String number(double value) {
		try {
			if (value == Math.floor(value) && !Double.isInfinite(value)) {
				return Common.numberFormat.get().format((long) value);
			}
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String dash(String value) {
		return value == null || value.trim().length() == 0 ? "-" : value.trim();
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private String html(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
