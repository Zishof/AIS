package ais.action.master.asset.helper;

import java.util.ArrayList;
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
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>BarangDalamProsesDashboard — Rekap &amp; Grafik "Barang Dalam Proses" (CIP)</h3>
 *
 * <p>Dashboard di tab BAST (Penerimaan Barang/Jasa) yang menampilkan REKAP dan GRAFIK atas semua
 * penerimaan (BAST) untuk barang/aset yang <b>kelompok-nya</b> "Merupakan Pekerjaan Dalam Pelaksanaan"
 * (CIP / Construction in Progress). Format mengikuti template
 * {@link TraceStatusPengadaanAssetDashboard} (panel + kartu ringkasan + grafik HTML/CSS + grid).</p>
 *
 * <p>Filter kategori memakai EXISTS ke detail penerimaan → master aset → kelompok aset yang
 * {@code merupakanPekerjaanDalamPelaksanaan = true}. TANPA perubahan skema (kolom dibuat otomatis
 * oleh hbm2ddl=update saat KelompokAsset dideploy).</p>
 */
public class BarangDalamProsesDashboard extends Vbox {

	private static final long serialVersionUID = 5521104880233117742L;

	private static final int GRID_PAGE_SIZE = 50;
	private static final int LIMIT = 3000;

	/** EXISTS: penerimaan ini punya minimal 1 detail yang kelompok aset-nya CIP (dalam pekerjaan). */
	private static final String SQL_EXISTS_KELOMPOK_CIP =
			"exists (select 1 from asset.penerimaan_pengadaan_master_asset_detail d "
			+ "join asset.master_asset m on d.masterasset = m.id "
			+ "join asset.kelompok_asset k on m.kelompok_asset = k.id "
			+ "where d.penerimaan_pengadaan_master_asset = this_.id "
			+ "and coalesce(k.merupakanpekerjaandalampelaksanaan, false) = true)";

	private Textbox keyword;
	private MyDatebox tglMulai;
	private MyDatebox tglSampai;
	private Vbox body;

	private static class Item {
		String kode;
		String vendor;
		String lokasi;
		String kodePo;
		String uraian;
		double nilai;
		Date tanggal;
		boolean disetujui;
		Long bastId;
	}

	public BarangDalamProsesDashboard() {
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

		new Label("Cari Kode BAST / Vendor / uraian: ").setParent(toolbar);
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
			@Override public String judul() { return "Barang / Aset Dalam Proses (CIP)"; }
			@Override public String subjudul() { return "Penerimaan Barang/Jasa - Pekerjaan Dalam Pelaksanaan"; }
			@Override public String deskripsi() { return "Rekap barang/aset yang kelompoknya masih dalam pengerjaan."; }
			@Override public java.util.List<DashboardReportKit.Bagian> bagian() {
				final java.util.List<Item> data = loadData();
				java.util.List<DashboardReportKit.Bagian> b = new java.util.ArrayList<DashboardReportKit.Bagian>();
				b.add(DashboardReportKit.kpi("Ringkasan", "Jumlah BAST dalam proses, yang sudah/belum disetujui, dan total nilainya.",
					new DashboardReportKit.PenyediaBaris() { @Override public java.util.List<Object[]> ambil() {
						int tot=data.size(), setuju=0; double nil=0,nilS=0;
						for (Item x:data){ nil+=x.nilai; if(x.disetujui){setuju++; nilS+=x.nilai;} }
						java.util.List<Object[]> o=new java.util.ArrayList<Object[]>();
						o.add(new Object[]{"Total BAST", DashboardReportKit.fmt(tot), "dokumen"});
						o.add(new Object[]{"Sudah Disetujui", DashboardReportKit.fmt(setuju), money(nilS)});
						o.add(new Object[]{"Belum Disetujui", DashboardReportKit.fmt(tot-setuju), money(nil-nilS)});
						o.add(new Object[]{"Total Nilai", money(nil), "seluruh BAST"});
						return o; } }));
				b.add(DashboardReportKit.batang("Nilai per Lokasi", "Sebaran nilai barang dalam proses per lokasi.",
					new String[]{"Lokasi","Nilai"}, new DashboardReportKit.PenyediaBaris() { @Override public java.util.List<Object[]> ambil() {
						java.util.Map<String,Double> m=new java.util.LinkedHashMap<String,Double>();
						for (Item x:data){ String k=(x.lokasi==null||x.lokasi.trim().isEmpty())?"(Tanpa Lokasi)":x.lokasi.trim(); Double v=m.get(k); m.put(k,(v==null?0:v)+x.nilai); }
						java.util.List<Object[]> o=new java.util.ArrayList<Object[]>();
						for (java.util.Map.Entry<String,Double> e:m.entrySet()) o.add(new Object[]{e.getKey(), Long.valueOf(Math.round(e.getValue()))});
						return o; } }));
				b.add(DashboardReportKit.tabel("Rincian BAST", "Daftar BAST barang/aset dalam proses.",
					new String[]{"Kode BAST","Vendor","Lokasi","No. PO","Uraian","Nilai","Tanggal","Status"},
					new DashboardReportKit.PenyediaBaris() { @Override public java.util.List<Object[]> ambil() {
						java.util.List<Object[]> o=new java.util.ArrayList<Object[]>();
						for (Item x:data) o.add(new Object[]{ dash(x.kode), dash(x.vendor), dash(x.lokasi), dash(x.kodePo), dash(x.uraian), money(x.nilai),
							x.tanggal==null?"-":Common.dateFormat3.get().format(x.tanggal), x.disetujui?"Disetujui":"Belum disetujui" });
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
		Panel panel = createPanel(parent, "Laporan Barang / Aset Dalam Proses (Pekerjaan Dalam Pelaksanaan)", null);
		Panelchildren c = firstChild(panel);
		c.appendChild(new Html("<div style='font-family:Arial,sans-serif;color:#334155;font-size:12px;line-height:1.55;'>"
				+ "Rekapitulasi seluruh Penerimaan Barang/Jasa (BAST) untuk barang/aset yang <b>kelompok-nya</b> "
				+ "ditandai \"Merupakan Pekerjaan Dalam Pelaksanaan\" (CIP). Menampilkan ringkasan nilai, sebaran per "
				+ "lokasi, dan rincian dokumen BAST beserta status persetujuannya."
				+ "</div>"));
	}

	private void renderSummary(Component parent, List<Item> items) {
		int total = items.size();
		int disetujui = 0;
		double totalNilai = 0.0;
		double nilaiDisetujui = 0.0;
		for (Item it : items) {
			totalNilai += it.nilai;
			if (it.disetujui) {
				disetujui++;
				nilaiDisetujui += it.nilai;
			}
		}
		Panel panel = createPanel(parent, "Ringkasan", "margin-top:10px;");
		Panelchildren c = firstChild(panel);
		String htmlStr = "<div style='font-family:Arial,sans-serif;'>"
				+ "<div style='display:flex;gap:10px;flex-wrap:wrap;'>"
				+ card("Total BAST (Dalam Proses)", String.valueOf(total), money(totalNilai) + " total nilai", "#1d4ed8")
				+ card("Sudah Disetujui", String.valueOf(disetujui), money(nilaiDisetujui) + " nilai disetujui", "#15803d")
				+ card("Belum Disetujui", String.valueOf(total - disetujui), money(totalNilai - nilaiDisetujui) + " nilai", "#b45309")
				+ card("Total Nilai", money(totalNilai), "seluruh barang dalam proses", "#4338ca")
				+ "</div></div>";
		c.appendChild(new Html(htmlStr));
	}

	private void renderChart(Component parent, List<Item> items) {
		// Agregasi nilai per Lokasi.
		Map<String, Double> perLokasi = new LinkedHashMap<String, Double>();
		for (Item it : items) {
			String key = it.lokasi == null || it.lokasi.trim().isEmpty() ? "(Tanpa Lokasi)" : it.lokasi.trim();
			Double v = perLokasi.get(key);
			perLokasi.put(key, (v == null ? 0.0 : v) + it.nilai);
		}
		Panel panel = createPanel(parent, "Grafik Nilai per Lokasi", "margin-top:10px;");
		Panelchildren c = firstChild(panel);
		if (perLokasi.isEmpty()) {
			c.appendChild(new Html("<div style='font-size:12px;color:#64748b;'>Belum ada data untuk ditampilkan.</div>"));
			return;
		}
		double max = 0.0;
		for (Double v : perLokasi.values()) {
			if (v != null && v > max) {
				max = v;
			}
		}
		if (max <= 0.0) {
			max = 1.0;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:Arial,sans-serif;'>");
		for (Map.Entry<String, Double> e : perLokasi.entrySet()) {
			double v = e.getValue() == null ? 0.0 : e.getValue();
			int pct = (int) Math.round(v / max * 100.0);
			if (pct < 2 && v > 0) {
				pct = 2;
			}
			sb.append("<div style='margin-bottom:8px;'>")
					.append("<div style='display:flex;justify-content:space-between;font-size:12px;color:#334155;margin-bottom:3px;'>")
					.append("<span style='font-weight:600;'>").append(html(e.getKey())).append("</span>")
					.append("<span>").append(money(v)).append("</span></div>")
					.append("<div style='background:#e2e8f0;border-radius:8px;height:16px;overflow:hidden;'>")
					.append("<div style='width:").append(pct).append("%;height:16px;background:linear-gradient(90deg,#3b82f6,#1d4ed8);border-radius:8px;'></div>")
					.append("</div></div>");
		}
		sb.append("</div>");
		c.appendChild(new Html(sb.toString()));
	}

	private void renderGrid(Component parent, List<Item> items) {
		Panel panel = createPanel(parent, "Rincian BAST Barang Dalam Proses", "margin-top:10px;");
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
		new MyColumnConfig("Vendor").setParent(columns);
		new MyColumnConfig("Lokasi").setParent(columns);
		new MyColumnConfig("No. PO").setParent(columns);
		new MyColumnConfig("Uraian").setParent(columns);
		MyColumnConfig cNilai = new MyColumnConfig("Nilai");
		cNilai.setAlign("right");
		cNilai.setParent(columns);
		new MyColumnConfig("Tanggal").setParent(columns);
		new MyColumnConfig("Status").setParent(columns);
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
			row.appendChild(new Label(dash(it.kode)));
			row.appendChild(new Label(dash(it.vendor)));
			row.appendChild(new Label(dash(it.lokasi)));
			row.appendChild(new Label(dash(it.kodePo)));
			row.appendChild(new Label(dash(it.uraian)));
			Label lblNilai = new Label(money(it.nilai));
			lblNilai.setStyle("text-align:right;display:block;");
			row.appendChild(lblNilai);
			row.appendChild(new Label(it.tanggal == null ? "-" : Common.dateFormat3.get().format(it.tanggal)));
			Label st = new Label(it.disetujui ? "Disetujui" : "Belum disetujui");
			st.setStyle("color:" + (it.disetujui ? "#15803d" : "#b45309") + ";font-weight:600;");
			row.appendChild(st);
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
			row.appendChild(new Label("Tidak ada barang/aset dalam proses pada rentang ini."));
		}
	}

	@SuppressWarnings("unchecked")
	private List<Item> loadData() {
		List<Item> hasil = new ArrayList<Item>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Criteria criteria = session.createCriteria(PenerimaanPengadaanMasterAsset.class)
					.add(Restrictions.sqlRestriction(SQL_EXISTS_KELOMPOK_CIP));

			String q = keyword == null ? "" : safe(keyword.getValue());
			if (!q.isEmpty()) {
				criteria.add(Restrictions.or(Restrictions.ilike("kode", q, MatchMode.ANYWHERE),
						Restrictions.ilike("keterangan", q, MatchMode.ANYWHERE)));
			}
			Date d1 = tglMulai == null ? null : tglMulai.getValue();
			Date d2 = tglSampai == null ? null : tglSampai.getValue();
			if (d1 != null && d2 != null) {
				criteria.add(Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(d1) + "') and date('"
						+ Common.databaseDateFormat.get().format(d2) + "')"));
			}
			criteria.addOrder(Order.desc("id"));
			criteria.setMaxResults(LIMIT);

			List<PenerimaanPengadaanMasterAsset> list = criteria.list();
			for (PenerimaanPengadaanMasterAsset p : list) {
				if (p == null) {
					continue;
				}
				Item it = new Item();
				try { it.kode = p.getKode(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BarangDalamProsesDashboard.java:359"); }
				try { it.uraian = p.getKeterangan(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BarangDalamProsesDashboard.java:360"); }
				try { it.nilai = p.getNilai() == null ? 0.0 : p.getNilai().doubleValue(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BarangDalamProsesDashboard.java:361"); }
				try { it.vendor = p.getPenyedia() == null ? "" : p.getPenyedia().getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BarangDalamProsesDashboard.java:362"); }
				try { it.lokasi = p.getLokasi() == null ? "" : p.getLokasi().getNama(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BarangDalamProsesDashboard.java:363"); }
				try {
					it.kodePo = p.getPemesananPengadaanMasterAsset() == null ? ""
							: p.getPemesananPengadaanMasterAsset().getKode();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BarangDalamProsesDashboard.java:367"); }
				try {
					it.tanggal = p.getTanggalPersetujuan() != null ? p.getTanggalPersetujuan() : p.getTanggalPembuatan();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BarangDalamProsesDashboard.java:370"); }
				try { it.disetujui = p.getDisetujuiOleh() != null; } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BarangDalamProsesDashboard.java:371"); }
				try { it.bastId = p.getId(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BarangDalamProsesDashboard.java:372"); }
				hasil.add(it);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BarangDalamProsesDashboard.java:379"); }
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/BarangDalamProsesDashboard.java:380"); }
			}
		}
		return hasil;
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
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String money(double value) {
		return "Rp " + number(value);
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
