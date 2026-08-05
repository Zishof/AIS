package ais.action.master.dashboard.library;
import ais.ui.util.DashboardGridExportHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Html;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;
import ais.action.report.format1.library.LaporanRekapPeminjamanWindow;

import ais.ui.util.DashboardModernHtmlUtil;
public class DashboardStatistikPeminjamanAnggota extends MyWindow {

	private static final long serialVersionUID = -28636873241676666L;

	
	private int width;
	private int height;
	private Div center;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private MyDatebox mulai = new MyDatebox();
	private MyDatebox sampai = new MyDatebox();
	private boolean tampilRinci = false;

	public DashboardStatistikPeminjamanAnggota() throws Exception {
		super();
		initFakultas();
		init();
		initChart();
	}

	public DashboardStatistikPeminjamanAnggota(int width, int height) throws Exception {
		super();
		tampilRinci = true;
		reinit(width, height);
	}

	public void reinit(int width, int height) throws Exception {
		this.width = width;
		this.height = height;
		initFakultas();
		init();
		initChart();
	}

	public DashboardStatistikPeminjamanAnggota(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initFakultas();
		init();
		initChart();
	}

	private void initFakultas() {
	}

	@SuppressWarnings("deprecation")
	private void init() {
		DashboardGridExportHelper.pasang(this, "Statistik Peminjaman Anggota");
		setHeight("100%");
		setWidth("100%");
		setBorder("none");

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Atur saringan untuk menyesuaikan data yang ditampilkan.",
				"Statistik Peminjaman Anggota",
				"Jumlah peminjaman buku oleh anggota perpustakaan, beserta grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setStyle("border:0;background:transparent;");
		grid.setParent(saringanHost);
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		Calendar dateMulai = ais.ui.util.WaktuUtil.getCalendar();
		dateMulai.set(Calendar.MONTH, dateMulai.get(Calendar.MONTH) - 1);
		mulai.setValue(dateMulai.getTime());
		mulai.setReadonly(true);
		row.appendChild(mulai);
		mulai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("s.d"));
		sampai.setValue(ais.ui.util.WaktuUtil.getDate());
		sampai.setReadonly(true);
		row.appendChild(sampai);
		sampai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initChart();
			}
		});

		if (tampilRinci) {
			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "4");
			MyButtonConfig btn = new MyButtonConfig("Lihat Rinci", "/img/12123-eyes-icon.png");
			btn.setParent(row);
			btn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					LaporanRekapPeminjamanWindow laporan = new LaporanRekapPeminjamanWindow();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Rekap Peminjaman");
					laporan.setClosable(true);
					laporan.setBorder("none");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});
		}

	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private VisualData loadData() {
		VisualData data = new VisualData();
		String dateMulai = mulai.getValue() == null ? Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())
				: Common.databaseDateFormat.get().format(mulai.getValue());
		Calendar tglSampai = ais.ui.util.WaktuUtil.getCalendar();
		tglSampai.setTime(sampai.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : sampai.getValue());
		tglSampai.set(Calendar.DATE, tglSampai.get(Calendar.DATE) + 1);
		String dateSelesai = Common.databaseDateFormat.get().format(tglSampai.getTime());

		Fakultas fakultas = searchfakultas.getSelectedItem() == null ? null : (Fakultas) searchfakultas.getSelectedItem().getValue();
		Jurusan jurusan = searchjurusan.getSelectedItem() == null ? null : (Jurusan) searchjurusan.getSelectedItem().getValue();

		Session session = HibernateUtil.currentSession();
		List<Perpustakaan> perpustakaans = ConstantValues.simpleList(
				session.createCriteria(Perpustakaan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				Perpustakaan.class);
		for (Perpustakaan p : perpustakaans) {
			if (p != null && p.getNama() != null) {
				data.perpustakaan.add(p.getNama());
			}
		}

		String sql = buildSql(dateMulai, dateSelesai, fakultas, jurusan);
		List<Object[]> rows = Common.ambilSql(sql);
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		for (Object[] row : rows) {
			Date tanggal = row[0] instanceof Date ? (Date) row[0] : null;
			String tanggalKey = tanggal == null ? "Tanpa Tanggal" : sdf.format(tanggal);
			String namaPerpus = row[1] == null ? "Tanpa Perpustakaan" : String.valueOf(row[1]);
			double qty = row[2] == null ? 0.0 : ((Number) row[2]).doubleValue();
			Map<String, Double> perDate = data.rows.get(tanggalKey);
			if (perDate == null) {
				perDate = new LinkedHashMap<String, Double>();
				data.rows.put(tanggalKey, perDate);
			}
			Double old = perDate.get(namaPerpus);
			perDate.put(namaPerpus, Double.valueOf((old == null ? 0.0 : old.doubleValue()) + qty));
			Double total = data.totalPerpustakaan.get(namaPerpus);
			data.totalPerpustakaan.put(namaPerpus, Double.valueOf((total == null ? 0.0 : total.doubleValue()) + qty));
			data.total += qty;
			if (qty > data.max) data.max = qty;
		}
		if (data.max <= 0.0) data.max = 1.0;
		return data;
	}

	private String buildSql(String dateMulai, String dateSelesai, Fakultas fakultas, Jurusan jurusan) {
		StringBuilder sql = new StringBuilder();
		sql.append("select date(a.tanggal_pembuatan) as tanggal, coalesce(d.nama,'Tanpa Perpustakaan') as perpustakaan, count(*) as qty ");
		sql.append("from library.peminjaman_pengadaan_item a inner join library.anggota b on (b.id = a.anggota) ");
		sql.append(" ");
		sql.append("left join library.perpustakaan d on (d.id = a.perpustakaan) ");
		sql.append("where date(a.tanggal_pembuatan) >= '").append(dateMulai).append("' and date(a.tanggal_pembuatan) <= '").append(dateSelesai).append("' ");
		if (jurusan != null && jurusan.getId() != null) {
			sql.append(" and c.jurusan = ").append(jurusan.getId());
		}
		if (fakultas != null && fakultas.getId() != null) {
			sql.append(" and e.fakultas = ").append(fakultas.getId());
		}
		sql.append(" group by date(a.tanggal_pembuatan), coalesce(d.nama,'Tanpa Perpustakaan') order by date(a.tanggal_pembuatan), coalesce(d.nama,'Tanpa Perpustakaan') ");
		return sql.toString();
	}

	private void initChart() throws Exception {
		Common.clear(center);
		VisualData data = loadData();
		center.appendChild(new Html(buildHtml(data)));
	}

	private String buildHtml(VisualData data) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:Arial,sans-serif;padding:12px;'>");
		sb.append("<div style='background:#fff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;box-shadow:0 2px 8px rgba(15,23,42,0.07);margin-bottom:12px;'>");
		sb.append("<div style='font-size:18px;font-weight:800;color:#0f172a;margin-bottom:4px;'>Statistik Peminjaman Anggota Perpustakaan</div>");
		sb.append("<div style='font-size:12px;color:#64748b;line-height:1.5;'>Menampilkan jumlah aktivitas perpustakaan sesuai tanggal dan filter yang dipilih. Grafik HTML/CSS ini ringan, mudah dibaca, dan tidak memakai grafik lama.</div>");
		sb.append("</div>");
		sb.append("<div style='display:flex;gap:10px;flex-wrap:wrap;margin-bottom:12px;'>");
		sb.append(card("Total Aktivitas", String.valueOf((long) data.total), "Jumlah seluruh aktivitas"));
		sb.append(card("Jumlah Tanggal", String.valueOf(data.rows.size()), "Hari yang memiliki data"));
		sb.append(card("Perpustakaan", String.valueOf(data.totalPerpustakaan.size()), "Unit yang muncul di grafik"));
		sb.append(card("Tertinggi", getTopLabel(data), "Aktivitas terbanyak"));
		sb.append("</div>");
		sb.append(buildTrend(data));
		sb.append(buildComposition(data));
		sb.append(buildRadar(data));
		sb.append("</div>");
		return sb.toString();
	}

	private String buildTrend(VisualData data) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#fff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;margin-bottom:12px;'>");
		sb.append("<div style='font-size:15px;font-weight:800;color:#0f172a;margin-bottom:6px;'>Tren Harian</div>");
		sb.append("<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>Setiap baris memperlihatkan aktivitas per tanggal dan per perpustakaan.</div>");
		if (data.rows.isEmpty()) {
			sb.append("<div style='font-size:12px;color:#64748b;border:1px dashed #cbd5e1;border-radius:10px;padding:12px;background:#f8fafc;'>Belum ada data pada filter ini.</div>");
		} else {
			int index = 0;
			for (String tanggal : data.rows.keySet()) {
				Map<String, Double> map = data.rows.get(tanggal);
				sb.append("<div style='margin:10px 0;padding-bottom:8px;border-bottom:1px dashed #e2e8f0;'>");
				sb.append("<div style='font-size:12px;font-weight:800;color:#334155;margin-bottom:5px;'>").append(html(tanggal)).append("</div>");
				for (String nama : map.keySet()) {
					double val = map.get(nama) == null ? 0.0 : map.get(nama).doubleValue();
					int width = percent(val, data.max);
					sb.append("<div style='display:flex;align-items:center;gap:7px;font-size:11px;color:#64748b;margin:3px 0;'>");
					sb.append("<span style='width:160px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>").append(html(nama)).append("</span>");
					sb.append("<div style='height:10px;background:#e2e8f0;border-radius:999px;flex:1;overflow:hidden;'><div style='height:10px;width:").append(width).append("%;background:").append(color(index)).append(";border-radius:999px;'></div></div>");
					sb.append("<b style='width:45px;text-align:right;color:#0f172a;'>").append((long) val).append("</b>");
					sb.append("</div>");
					index++;
				}
				sb.append("</div>");
			}
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String buildComposition(VisualData data) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#fff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;margin-bottom:12px;'>");
		sb.append("<div style='font-size:15px;font-weight:800;color:#0f172a;margin-bottom:6px;'>Komposisi Perpustakaan</div>");
		sb.append("<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>Memperlihatkan unit perpustakaan yang paling banyak menerima aktivitas.</div>");
		if (data.totalPerpustakaan.isEmpty()) {
			sb.append("<div style='font-size:12px;color:#64748b;'>Belum ada komposisi data.</div>");
		} else {
			int index = 0;
			for (String nama : data.totalPerpustakaan.keySet()) {
				double val = data.totalPerpustakaan.get(nama) == null ? 0.0 : data.totalPerpustakaan.get(nama).doubleValue();
				int width = percent(val, data.total <= 0.0 ? 1.0 : data.total);
				sb.append("<div style='margin:8px 0;'>");
				sb.append("<div style='display:flex;justify-content:space-between;font-size:11px;color:#334155;font-weight:700;'><span>").append(html(nama)).append("</span><span>").append((long) val).append(" • ").append(width).append("%</span></div>");
				sb.append("<div style='height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:4px;'><div style='height:12px;width:").append(width).append("%;background:").append(color(index)).append(";border-radius:999px;'></div></div>");
				sb.append("</div>");
				index++;
			}
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String buildRadar(VisualData data) {
		int aktivitas = percent(data.total, Math.max(data.total, 1.0));
		int persebaran = percent(data.totalPerpustakaan.size(), Math.max(data.totalPerpustakaan.size(), 1));
		int tanggal = percent(data.rows.size(), 30.0);
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:#fff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;'>");
		sb.append("<div style='font-size:15px;font-weight:800;color:#0f172a;margin-bottom:6px;'>Radar Aktivitas</div>");
		sb.append("<div style='font-size:11px;color:#64748b;margin-bottom:8px;'>Membantu membaca aktivitas dari sisi jumlah transaksi, persebaran unit, dan jumlah hari aktif.</div>");
		sb.append(gauge("Aktivitas", aktivitas));
		sb.append(gauge("Sebaran Perpustakaan", persebaran));
		sb.append(gauge("Hari Aktif", tanggal));
		sb.append("</div>");
		return sb.toString();
	}

	private String gauge(String label, int percent) {
		return "<div style='margin:8px 0;'><div style='display:flex;justify-content:space-between;font-size:11px;color:#334155;font-weight:700;'><span>" + html(label) + "</span><span>" + percent + "%</span></div>"
				+ "<div style='height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:4px;'><div style='height:12px;width:" + percent + "%;background:linear-gradient(90deg,#2563eb,#22c55e);border-radius:999px;'></div></div></div>";
	}

	private String card(String title, String value, String note) {
		title = ais.common.Common.getBahasaConfig(title);
		note = ais.common.Common.getBahasaConfig(note);
		return "<div style='background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:12px;box-shadow:0 2px 8px rgba(15,23,42,0.07);min-width:145px;flex:1;'>"
				+ "<div style='font-size:10px;color:#64748b;font-weight:800;text-transform:uppercase;'>" + html(title) + "</div>"
				+ "<div style='font-size:18px;color:#0f172a;font-weight:800;margin-top:5px;'>" + html(value) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;margin-top:4px;'>" + html(note) + "</div></div>";
	}

	private String getTopLabel(VisualData data) {
		String key = "-";
		double max = 0.0;
		for (String nama : data.totalPerpustakaan.keySet()) {
			double val = data.totalPerpustakaan.get(nama) == null ? 0.0 : data.totalPerpustakaan.get(nama).doubleValue();
			if (val > max || "-".equals(key)) {
				max = val;
				key = nama;
			}
		}
		return key;
	}

	private String color(int index) {
		String[] colors = new String[] { "var(--ais-theme-primary,#2563eb)", "#16a34a", "#f97316", "#9333ea", "#dc2626", "#0891b2", "#ca8a04", "#0f766e" };
		return colors[index % colors.length];
	}

	private int percent(double value, double max) {
		if (max <= 0.0) return 0;
		int p = (int) Math.round(value * 100.0 / max);
		if (p < 0) return 0;
		if (p > 100) return 100;
		return p;
	}

	private String html(String value) {
		if (value == null) return "";
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	private static class VisualData {
		private TreeMap<String, Map<String, Double>> rows = new TreeMap<String, Map<String, Double>>();
		private LinkedHashMap<String, Double> totalPerpustakaan = new LinkedHashMap<String, Double>();
		private List<String> perpustakaan = new ArrayList<String>();
		private double total;
		private double max = 1.0;
	}
}
