package ais.action.master.dashboard.akunting;

/*
 * ENHANCED_DASHBOARD_UIUX_HTML_CSS_2026_06_06
 * Baseline dari file upload terbaru, disusun ulang sesuai package /java/ais/...
 * Catatan: openSession tetap ditutup pada finally; currentSession tidak ditutup manual.
 * Grafik, tren, radar, dan spider web dipertahankan sebagai HTML/CSS agar ringan dan aman di ZK 5.5.
 */


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.MainAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Memantau pengajuan transfer dari tahap masuk, proses persetujuan, sampai realisasi agar pembayaran lebih mudah diprioritaskan.
 * Semua tabel besar diarahkan memakai paging 10 baris agar tampilan ringan dan mudah dibaca.
 */

public class DasboardDaftarPengajuanTransfer extends MyWindow {

	private static final long serialVersionUID = 3557603220165512688L;
	private static final int PAGE_SIZE = 10;

	private Center center;
	private Vbox body;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private MyDatebox start;
	private MyDatebox end;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private int dashboardPagerCounter = 0;

	public DasboardDaftarPengajuanTransfer() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DasboardDaftarPengajuanTransfer(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	protected boolean isJenisDashboard() {
		return false;
	}

	protected String getJudulDashboard() {
		return isJenisDashboard() ? "Dasbor Pengajuan Transfer per Jenis" : "Dasbor Pengajuan Transfer";
	}

	protected String getSubJudulDashboard() {
		return isJenisDashboard()
				? "Melihat jenis pengajuan yang paling banyak masuk agar prioritas pembayaran lebih mudah ditentukan."
				: "Memantau pengajuan transfer dari tahap belum diproses, sedang diajukan, sampai selesai direalisasikan.";
	}

	private void init() throws Exception {
		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("border:0;background:#f8fafc;min-height:" + ambilTinggiMinimal() + "px;");

		North north = new North();
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setParent(borderlayout);
		north.setStyle("border:0;background:#ffffff;");

		org.zkoss.zul.Grid filterGrid = new org.zkoss.zul.Grid();
		filterGrid.setWidth("100%");
		filterGrid.setSclass("dgrid");
		filterGrid.setStyle("border:0;background:#ffffff;padding:8px 10px;box-shadow:0 1px 5px rgba(15,23,42,0.08);");
		filterGrid.setParent(north);

		Rows rows = new Rows();
		rows.setParent(filterGrid);

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				reload();
			}
		};

		Row row = new Row();
		row.setValign("middle");
		row.setStyle("border:0;background:transparent;");
		row.setParent(rows);

		searchparent = new AmbilDataSatuanKerjaBanbox();
		searchparent.setEventListener(eventListener);
		searchparent.setCols(8);

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		row.appendChild(new MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchparent);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal")));
		start = new MyDatebox();
		if (start != null) start.setReadonly(true);
		start.setCols(8);
		row.appendChild(start);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		end = new MyDatebox();
		if (end != null) end.setReadonly(true);
		end.setCols(8);
		row.appendChild(end);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 12);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		start.addEventListener("onChange", eventListener);
		end.addEventListener("onChange", eventListener);

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("border:0;background:transparent;");
		row.appendChild(toolbar);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
		refresh.setTooltiptext("Muat ulang data dasbor");
		refresh.addEventListener("onClick", eventListener);
		refresh.setParent(toolbar);

		center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);
		center.setStyle("border:0;background:#f8fafc;overflow:auto;");

		body = new Vbox();
		body.setWidth("100%");
		body.setHeight("100%");
		body.setStyle("overflow:auto;background:#f8fafc;padding:12px;box-sizing:border-box;min-height:"
				+ (ambilTinggiMinimal() - 80) + "px;");
		body.setParent(center);

		Common.createDefaultTimer(eventListener);
	}

	private int ambilTinggiMinimal() {
		try {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && tbmuser.getUserId() != null) {
				Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
				if (desktopHeight != null && desktopHeight.intValue() > 300) {
					return Math.max(900, (int) (desktopHeight.intValue() * 0.90));
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return 900;
	}

	private void reload() {
		try {
			if (body == null) {
				return;
			}
			Common.clear(body);

			DashboardData data = loadDashboardData();
			body.appendChild(new Html(renderDashboardHtml(data)));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private DashboardData loadDashboardData() {
		DashboardData data = new DashboardData();
		Session session = HibernateUtil.currentSession();

		Date mulai = start == null ? null : start.getValue();
		Date sampai = end == null ? null : end.getValue();

		String filter = buildWhereClause("d");
		String dateFilter = "";
		if (mulai != null && sampai != null) {
			dateFilter = " and d.waktu >= :mulai and d.waktu <= :sampai ";
		}

		String sqlOverview = "select count(d.id) as total_data, "
				+ "coalesce(sum(coalesce(d.nominal, 0)), 0) as total_nominal, "
				+ "sum(case when d.proses_transfer is null then 1 else 0 end) as belum, "
				+ "sum(case when d.proses_transfer is not null and pt.realisasikan_oleh is null then 1 else 0 end) as diajukan, "
				+ "sum(case when coalesce(d.transfer, false) = true and d.proses_transfer is not null and pt.realisasikan_oleh is not null then 1 else 0 end) as transfer, "
				+ "sum(case when coalesce(d.transitori, false) = true and d.proses_transfer is not null and pt.realisasikan_oleh is not null then 1 else 0 end) as transitori "
				+ "from akunting.daftar_pengajuan_transfer d "
				+ "left join akunting.proses_transfer pt on pt.id = d.proses_transfer "
				+ "where (d.aktif = true or d.aktif is null) " + dateFilter + filter;

		Object[] overview = uniqueRow(session, sqlOverview, mulai, sampai);
		if (overview != null) {
			data.total = toLong(overview[0]);
			data.totalNominal = toDouble(overview[1]);
			data.belumDiproses = toLong(overview[2]);
			data.diajukan = toLong(overview[3]);
			data.transfer = toLong(overview[4]);
			data.transitori = toLong(overview[5]);
		}

		String sqlJenis = "select "
				+ "sum(case when d.uang_muka is not null then 1 else 0 end) as uang_muka, "
				+ "sum(case when d.pertangungjawaban is not null then 1 else 0 end) as lpj, "
				+ "sum(case when d.penggantian_kas_kecil is not null then 1 else 0 end) as kas_kecil, "
				+ "sum(case when d.kas_besar is not null then 1 else 0 end) as kas_besar, "
				+ "sum(case when d.pembayaran_pengadaan_master_asset_detail is not null then 1 else 0 end) as barang_jasa, "
				+ "sum(case when d.pembayaran_termin_master_asset_detail is not null then 1 else 0 end) as pekerjaan, "
				+ "sum(case when d.pembayaran_dp_master_asset_detail is not null then 1 else 0 end) as dp, "
				+ "sum(case when d.diskon_tagihan is not null then 1 else 0 end) as diskon, "
				+ "sum(case when d.pajak is not null then 1 else 0 end) as pajak, "
				+ "sum(case when d.uang_muka is null and d.pertangungjawaban is null and d.penggantian_kas_kecil is null "
				+ "and d.kas_besar is null and d.pembayaran_pengadaan_master_asset_detail is null "
				+ "and d.pembayaran_termin_master_asset_detail is null and d.pembayaran_dp_master_asset_detail is null "
				+ "and d.diskon_tagihan is null and d.pajak is null then 1 else 0 end) as lain_lain "
				+ "from akunting.daftar_pengajuan_transfer d "
				+ "where (d.aktif = true or d.aktif is null) " + dateFilter + filter;

		Object[] jenis = uniqueRow(session, sqlJenis, mulai, sampai);
		if (jenis != null) {
			String[] labels = new String[] { "Uang Muka", "LPJ", "Kas Kecil", "Kas Besar", "Barang/Jasa",
					"Pekerjaan/Termin", "DP", "Diskon", "Pajak", "Lain-Lain" };
			for (int i = 0; i < labels.length; i++) {
				data.jenisLabels.add(labels[i]);
				data.jenisValues.add(Long.valueOf(toLong(jenis[i])));
			}
		}

		String sqlSatker = "select coalesce(sk.nama, 'Tidak Ditentukan') as nama_satker, count(d.id) as total_data, "
				+ "sum(case when d.proses_transfer is null then 1 else 0 end) as belum, "
				+ "sum(case when d.proses_transfer is not null and pt.realisasikan_oleh is null then 1 else 0 end) as diajukan, "
				+ "sum(case when d.proses_transfer is not null and pt.realisasikan_oleh is not null then 1 else 0 end) as selesai, "
				+ "coalesce(sum(coalesce(d.nominal, 0)), 0) as total_nominal "
				+ "from akunting.daftar_pengajuan_transfer d "
				+ "left join rab.satuan_kerja sk on sk.id = d.satuan_kerja "
				+ "left join akunting.proses_transfer pt on pt.id = d.proses_transfer "
				+ "where (d.aktif = true or d.aktif is null) " + dateFilter + filter
				+ " group by coalesce(sk.nama, 'Tidak Ditentukan') order by count(d.id) desc, coalesce(sk.nama, 'Tidak Ditentukan') asc";

		List rows = listRows(session, sqlSatker, mulai, sampai);
		for (Object rowObj : rows) {
			Object[] row = (Object[]) rowObj;
			SatkerRow sr = new SatkerRow();
			sr.nama = str(row[0]);
			sr.total = toLong(row[1]);
			sr.belum = toLong(row[2]);
			sr.diajukan = toLong(row[3]);
			sr.selesai = toLong(row[4]);
			sr.nominal = toDouble(row[5]);
			data.satkerRows.add(sr);
		}

		String sqlTrend = "select to_char(date_trunc('month', d.waktu), 'YYYY-MM') as bulan, count(d.id) as total_data, "
				+ "coalesce(sum(coalesce(d.nominal, 0)), 0) as total_nominal "
				+ "from akunting.daftar_pengajuan_transfer d "
				+ "where (d.aktif = true or d.aktif is null) and d.waktu is not null " + dateFilter + filter
				+ " group by date_trunc('month', d.waktu) order by date_trunc('month', d.waktu) asc";

		rows = listRows(session, sqlTrend, mulai, sampai);
		for (Object rowObj : rows) {
			Object[] row = (Object[]) rowObj;
			TrendRow tr = new TrendRow();
			tr.label = str(row[0]);
			tr.total = toLong(row[1]);
			tr.nominal = toDouble(row[2]);
			data.trendRows.add(tr);
		}

		String sqlDisposisiSop = "select d.disposisi_sop, "
				+ "coalesce(nullif(trim(ds.keterangan), ''), nullif(trim(s.nama), ''), 'Tanpa Disposisi SOP') as nama_sop, "
				+ "count(d.id) as total_data, coalesce(sum(coalesce(d.nominal, 0)), 0) as total_nominal, "
				+ "sum(case when d.proses_transfer is null then 1 else 0 end) as belum, "
				+ "sum(case when d.proses_transfer is not null and pt.realisasikan_oleh is null then 1 else 0 end) as diajukan, "
				+ "sum(case when d.proses_transfer is not null and pt.realisasikan_oleh is not null then 1 else 0 end) as selesai "
				+ "from akunting.daftar_pengajuan_transfer d "
				+ "left join akunting.proses_transfer pt on pt.id = d.proses_transfer "
				+ "left join public.disposisi_sop ds on ds.id = d.disposisi_sop "
				+ "left join public.sop s on s.id = ds.sop "
				+ "where (d.aktif = true or d.aktif is null) " + dateFilter + filter
				+ " group by d.disposisi_sop, coalesce(nullif(trim(ds.keterangan), ''), nullif(trim(s.nama), ''), 'Tanpa Disposisi SOP') "
				+ "order by count(d.id) desc, nama_sop asc";

		rows = listRows(session, sqlDisposisiSop, mulai, sampai);
		for (Object rowObj : rows) {
			Object[] row = (Object[]) rowObj;
			DisposisiSopRow dr = new DisposisiSopRow();
			dr.disposisiSopId = toLong(row[0]);
			dr.nama = str(row[1]);
			dr.total = toLong(row[2]);
			dr.nominal = toDouble(row[3]);
			dr.belum = toLong(row[4]);
			dr.diajukan = toLong(row[5]);
			dr.selesai = toLong(row[6]);
			data.disposisiSopRows.add(dr);
		}

		String dateFilterProsesTransitori = "";
		if (mulai != null && sampai != null) {
			dateFilterProsesTransitori = " and p.tanggal_pembuatan >= :mulai and p.tanggal_pembuatan <= :sampai ";
		}

		String sqlProsesTransitoriSop = "select p.disposisi_sop, "
				+ "coalesce(nullif(trim(ds.keterangan), ''), nullif(trim(s.nama), ''), 'Tanpa Disposisi SOP') as nama_sop, "
				+ "count(p.id) as total_data, coalesce(sum(coalesce(p.nilai, 0)), 0) as total_nominal, "
				+ "sum(case when p.disetujui_oleh is null then 1 else 0 end) as menunggu_persetujuan, "
				+ "sum(case when p.disetujui_oleh is not null then 1 else 0 end) as sudah_disetujui "
				+ "from akunting.proses_transitori p "
				+ "left join public.disposisi_sop ds on ds.id = p.disposisi_sop "
				+ "left join public.sop s on s.id = ds.sop "
				+ "where (p.aktif = true or p.aktif is null) " + dateFilterProsesTransitori
				+ " group by p.disposisi_sop, coalesce(nullif(trim(ds.keterangan), ''), nullif(trim(s.nama), ''), 'Tanpa Disposisi SOP') "
				+ "order by count(p.id) desc, nama_sop asc";

		rows = listRows(session, sqlProsesTransitoriSop, mulai, sampai);
		for (Object rowObj : rows) {
			Object[] row = (Object[]) rowObj;
			ProsesTransitoriSopRow pr = new ProsesTransitoriSopRow();
			pr.disposisiSopId = toLong(row[0]);
			pr.nama = str(row[1]);
			pr.total = toLong(row[2]);
			pr.nominal = toDouble(row[3]);
			pr.menungguPersetujuan = toLong(row[4]);
			pr.sudahDisetujui = toLong(row[5]);
			data.prosesTransitoriSopRows.add(pr);
		}

		return data;
	}

	private Object[] uniqueRow(Session session, String sql, Date mulai, Date sampai) {
		SQLQuery query = session.createSQLQuery(sql);
		isiParameterTanggal(query, mulai, sampai);
		Object result = query.uniqueResult();
		return result instanceof Object[] ? (Object[]) result : null;
	}

	private List listRows(Session session, String sql, Date mulai, Date sampai) {
		SQLQuery query = session.createSQLQuery(sql);
		isiParameterTanggal(query, mulai, sampai);
		return query.list();
	}

	private void isiParameterTanggal(SQLQuery query, Date mulai, Date sampai) {
		if (mulai != null && sampai != null) {
			query.setParameter("mulai", mulai);
			query.setParameter("sampai", sampai);
		}
	}

	private String buildWhereClause(String alias) {
		try {
			if (searchparent == null) {
				return "";
			}
			SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
			if (parent == null) {
				return "";
			}
			java.util.Set<SatuanKerja> satuanKerjas = new java.util.HashSet<SatuanKerja>();
			satuanKerjas.add(parent);
			if (satuanKerjaTreeModel != null) {
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
			}
			String ids = "";
			for (SatuanKerja satuanKerja : satuanKerjas) {
				if (satuanKerja != null && satuanKerja.getId() != null) {
					ids += ids.length() == 0 ? satuanKerja.getId().toString() : "," + satuanKerja.getId();
				}
			}
			if (ids.length() == 0) {
				return "";
			}
			return " and (" + alias + ".satuan_kerja in (" + ids + ") or " + alias + ".satuan_kerja is null) ";
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return "";
		}
	}

	private String renderDashboardHtml(DashboardData data) {
		dashboardPagerCounter = 0;
		StringBuilder sb = new StringBuilder();
		sb.append("<style>");
		sb.append(".tf-wrap{font-family:Arial,Helvetica,sans-serif;color:#0f172a;}");
		sb.append(".tf-hero{background:linear-gradient(135deg,#0f766e,#2563eb);color:white;border-radius:18px;padding:18px 22px;margin-bottom:14px;box-shadow:0 14px 28px rgba(15,23,42,.16);}");
		sb.append(".tf-title{font-size:20px;font-weight:800;margin-bottom:6px;letter-spacing:.2px;}");
		sb.append(".tf-desc{font-size:12.5px;line-height:1.55;max-width:980px;opacity:.95;}");
		sb.append(".tf-cards{display:flex;flex-wrap:wrap;gap:10px;margin:12px 0;}");
		sb.append(".tf-card{background:white;border:1px solid #e2e8f0;border-radius:16px;padding:13px 15px;min-width:170px;flex:1;box-shadow:0 8px 18px rgba(15,23,42,.07);}");
		sb.append(".tf-card .num{font-size:24px;font-weight:800;margin-bottom:4px;}");
		sb.append(".tf-card .lbl{font-size:12px;color:#475569;font-weight:700;}");
		sb.append(".tf-card .note{font-size:11px;color:#64748b;margin-top:6px;line-height:1.45;}");
		sb.append(".tf-grid{display:flex;flex-wrap:wrap;gap:12px;align-items:stretch;}");
		sb.append(".tf-panel{background:white;border:1px solid #e2e8f0;border-radius:16px;padding:14px;box-shadow:0 8px 18px rgba(15,23,42,.06);flex:1 1 420px;min-width:320px;}");
		sb.append(".tf-panel-title{font-size:15px;font-weight:800;margin-bottom:4px;}");
		sb.append(".tf-panel-desc{font-size:12px;color:#64748b;line-height:1.55;margin-bottom:12px;}");
		sb.append(".tf-bar-row{display:flex;align-items:center;gap:10px;margin:8px 0;}");
		sb.append(".tf-bar-label{width:130px;font-size:12px;font-weight:700;color:#334155;}");
		sb.append(".tf-bar-track{height:12px;background:#e2e8f0;border-radius:999px;flex:1;overflow:hidden;}");
		sb.append(".tf-bar-fill{height:12px;background:linear-gradient(90deg,#22c55e,#0ea5e9);border-radius:999px;}");
		sb.append(".tf-bar-val{width:80px;text-align:right;font-size:12px;font-weight:800;}");
		sb.append(".tf-table{width:100%;border-collapse:separate;border-spacing:0 6px;font-size:12px;}");
		sb.append(".tf-table th{text-align:left;color:#475569;padding:7px 9px;background:#f1f5f9;}");
		sb.append(".tf-table td{padding:8px 9px;background:#f8fafc;border-top:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0;}");
		sb.append(".tf-table td:first-child{border-left:1px solid #e2e8f0;border-radius:10px 0 0 10px;font-weight:700;}");
		sb.append(".tf-table td:last-child{border-right:1px solid #e2e8f0;border-radius:0 10px 10px 0;text-align:right;}");
		sb.append(".tf-muted{color:#64748b;}");
		sb.append(".tf-pill{display:inline-block;border-radius:999px;padding:3px 8px;font-size:11px;font-weight:800;background:#e0f2fe;color:#075985;}");
		sb.append(".tf-pill-warn{background:#fef3c7;color:#92400e;}");
		sb.append(".tf-page-view{border:1px solid #e2e8f0;border-radius:14px;margin:8px 0;background:#ffffff;overflow:hidden;}");
		sb.append(".tf-page-title{background:#ecfdf5;color:#065f46;padding:9px 11px;font-size:12px;font-weight:800;border-bottom:1px solid #bbf7d0;}");
		sb.append(".tf-page-info{font-size:11px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;border-radius:12px;padding:7px 10px;margin-bottom:8px;line-height:1.45;}");
		sb.append(".tf-pager{display:flex;flex-wrap:wrap;align-items:center;gap:6px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;padding:8px;margin:8px 0;}");
		sb.append(".tf-pager-info{font-size:11px;font-weight:800;color:#334155;margin-right:4px;white-space:nowrap;}");
		sb.append(".tf-pager-btn,.tf-pager-num{border:1px solid #cbd5e1;background:#ffffff;color:#0f172a;border-radius:9px;padding:5px 9px;font-size:11px;font-weight:800;cursor:pointer;}");
		sb.append(".tf-pager-btn:hover,.tf-pager-num:hover{background:#e0f2fe;border-color:#38bdf8;color:#075985;}");
		sb.append(".tf-pager-btn:disabled{opacity:.45;cursor:not-allowed;background:#f1f5f9;}");
		sb.append(".tf-pager-num.active{background:#0ea5e9;color:#ffffff;border-color:#0284c7;}");
		sb.append(".tf-pager-dots{font-size:11px;color:#64748b;font-weight:800;padding:0 2px;}");
		sb.append("</style>");
		sb.append("<script type='text/javascript'>");
		sb.append("function tfDashboardShowPage(id,page){var bar=document.getElementById(id+'_bar');if(!bar){return false;}var total=parseInt(bar.getAttribute('data-total'),10)||1;var rows=parseInt(bar.getAttribute('data-rows'),10)||0;var size=parseInt(bar.getAttribute('data-size'),10)||10;if(page<1){page=1;}if(page>total){page=total;}for(var i=1;i<=total;i++){var p=document.getElementById(id+'_p'+i);if(p){p.style.display=(i==page?'block':'none');}}bar.setAttribute('data-current',page);var info=document.getElementById(id+'_info');if(info){var a=((page-1)*size)+1;var b=Math.min(page*size,rows);info.innerHTML='Halaman '+page+'/'+total+' (data '+a+'-'+b+' dari '+rows+')';}var prev=document.getElementById(id+'_prev');if(prev){prev.disabled=page<=1;}var next=document.getElementById(id+'_next');if(next){next.disabled=page>=total;}var nums=document.getElementById(id+'_nums');if(nums){var html='';var win=10;var start=Math.floor((page-1)/win)*win+1;var end=Math.min(start+win-1,total);if(start>1){html+='<button type=\"button\" class=\"tf-pager-num\" onclick=\"return tfDashboardShowPage(&quot;'+id+'&quot;,'+(start-1)+')\">...</button>';}for(i=start;i<=end;i++){html+='<button type=\"button\" class=\"tf-pager-num '+(i==page?'active':'')+'\" onclick=\"return tfDashboardShowPage(&quot;'+id+'&quot;,'+i+')\">'+i+'</button>';}if(end<total){html+='<button type=\"button\" class=\"tf-pager-num\" onclick=\"return tfDashboardShowPage(&quot;'+id+'&quot;,'+(end+1)+')\">...</button>';}nums.innerHTML=html;}return false;}");
		sb.append("function tfDashboardMovePage(id,delta){var bar=document.getElementById(id+'_bar');if(!bar){return false;}var page=parseInt(bar.getAttribute('data-current'),10)||1;return tfDashboardShowPage(id,page+delta);}");
		sb.append("function tfDashboardInit(){var bars=document.getElementsByClassName('tf-pager');for(var i=0;i<bars.length;i++){var id=bars[i].id.replace('_bar','');tfDashboardShowPage(id,1);}}if(window.addEventListener){window.addEventListener('load',tfDashboardInit,false);}else if(window.attachEvent){window.attachEvent('onload',tfDashboardInit);}setTimeout(tfDashboardInit,80);");
		sb.append("</script>");

		sb.append("<div class='tf-wrap'>");
		sb.append("<div class='tf-hero'><div class='tf-title'>").append(esc(getJudulDashboard())).append("</div>");
		sb.append("<div class='tf-desc'>").append(esc(getSubJudulDashboard()))
				.append(" Periode data mengikuti filter tanggal dan satuan kerja di atas.</div></div>");

		sb.append("<div class='tf-cards'>");
		appendCard(sb, "Total Pengajuan", data.total, "Semua pengajuan transfer yang masuk pada periode ini.");
		appendCard(sb, "Belum Diproses", data.belumDiproses, "Masih menunggu dibuatkan proses transfer.");
		appendCard(sb, "Sedang Diajukan", data.diajukan, "Sudah masuk proses, tetapi belum direalisasikan.");
		appendCard(sb, "Sudah Transfer", data.transfer, "Sudah selesai melalui proses transfer.");
		appendCard(sb, "Transitori", data.transitori, "Dipindahkan sementara ke proses transitori.");
		appendCardText(sb, "Total Nominal", formatUang(data.totalNominal), "Akumulasi nilai pengajuan pada periode ini.");
		sb.append("</div>");

		sb.append("<div class='tf-grid'>");
		if (isJenisDashboard()) {
			appendBarPanel(sb, "Jenis Pengajuan",
					"Menunjukkan sumber pengajuan yang paling sering membutuhkan pembayaran.",
					data.jenisLabels, data.jenisValues);
			appendStatusPanel(sb, data);
		} else {
			appendStatusPanel(sb, data);
			appendBarPanel(sb, "Jenis Pengajuan",
					"Membaca kelompok transaksi yang paling banyak diajukan, misalnya uang muka, kas kecil, atau pembayaran barang/jasa.",
					data.jenisLabels, data.jenisValues);
		}
		appendDisposisiSopPanel(sb, data);
		appendProsesTransitoriSopPanel(sb, data);
		appendSatkerPanel(sb, data);
		appendTrendPanel(sb, data);
		sb.append("</div></div>");
		return sb.toString();
	}

	private void appendStatusPanel(StringBuilder sb, DashboardData data) {
		List labels = new ArrayList();
		List values = new ArrayList();
		labels.add("Belum Diproses");
		values.add(Long.valueOf(data.belumDiproses));
		labels.add("Sedang Diajukan");
		values.add(Long.valueOf(data.diajukan));
		labels.add("Sudah Transfer");
		values.add(Long.valueOf(data.transfer));
		labels.add("Transitori");
		values.add(Long.valueOf(data.transitori));
		appendBarPanel(sb, "Posisi Proses Transfer",
				"Memperlihatkan posisi pengajuan saat ini, sehingga pekerjaan yang belum diproses dan yang sudah selesai cepat terlihat.",
				labels, values);
	}

	private void appendBarPanel(StringBuilder sb, String title, String desc, List labels, List values) {
		long max = 0L;
		for (Object value : values) {
			max = Math.max(max, toLong(value));
		}
		sb.append("<div class='tf-panel'><div class='tf-panel-title'>").append(esc(title)).append("</div>");
		sb.append("<div class='tf-panel-desc'>").append(esc(desc)).append("</div>");
		if (labels.isEmpty()) {
			sb.append("<div class='tf-muted'>Belum ada data pada filter yang dipilih.</div>");
		}
		for (int i = 0; i < labels.size(); i++) {
			long value = toLong(values.get(i));
			long percent = max <= 0L ? 0L : Math.max(2L, Math.round((value * 100.0) / max));
			sb.append("<div class='tf-bar-row'><div class='tf-bar-label'>").append(esc(str(labels.get(i))))
					.append("</div><div class='tf-bar-track'><div class='tf-bar-fill' style='width:")
					.append(percent).append("%'></div></div><div class='tf-bar-val'>")
					.append(value).append("</div></div>");
		}
		sb.append("</div>");
	}

	private void appendSatkerPanel(StringBuilder sb, DashboardData data) {
		sb.append("<div class='tf-panel'>");
		sb.append("<div class='tf-panel-title'>Pengajuan per Satuan Kerja</div>");
		sb.append("<div class='tf-panel-desc'>Memperlihatkan unit yang paling banyak mengajukan pembayaran, lengkap dengan posisi proses dan total nilainya.</div>");
		appendPagingInfo(sb, data.satkerRows.size(), PAGE_SIZE);
		if (data.satkerRows.isEmpty()) {
			sb.append("<table class='tf-table'><tr><th>Satuan Kerja</th><th>Total</th><th>Belum</th><th>Diajukan</th><th>Selesai</th><th>Nominal</th></tr>");
			sb.append("<tr><td colspan='6' class='tf-muted'>Belum ada data pada filter yang dipilih.</td></tr></table>");
		} else {
			String pagerId = nextPagerId("tf_satker");
			String header = "<tr><th>Satuan Kerja</th><th>Total</th><th>Belum</th><th>Diajukan</th><th>Selesai</th><th>Nominal</th></tr>";
			appendPager(sb, pagerId, data.satkerRows.size(), PAGE_SIZE);
			int rowIndex = 0;
			for (SatkerRow row : data.satkerRows) {
				appendPagedTableStart(sb, pagerId, rowIndex, data.satkerRows.size(), PAGE_SIZE,
						"Pengajuan per Satuan Kerja", header);
				sb.append("<tr><td>").append(esc(row.nama)).append("</td><td>").append(row.total)
						.append("</td><td>").append(row.belum).append("</td><td>").append(row.diajukan)
						.append("</td><td>").append(row.selesai).append("</td><td>")
						.append(esc(formatUang(row.nominal))).append("</td></tr>");
				rowIndex++;
			}
			appendPagedTableEnd(sb, data.satkerRows.size());
		}
		sb.append("</div>");
	}

	private void appendTrendPanel(StringBuilder sb, DashboardData data) {
		List labels = new ArrayList();
		List values = new ArrayList();
		for (TrendRow row : data.trendRows) {
			labels.add(row.label);
			values.add(Long.valueOf(row.total));
		}
		appendBarPanel(sb, "Tren Bulanan",
				"Menampilkan jumlah pengajuan setiap bulan agar kenaikan atau penurunan pekerjaan bisa terlihat lebih cepat.",
				labels, values);

		sb.append("<div class='tf-panel'>");
		sb.append("<div class='tf-panel-title'>Nominal per Bulan</div>");
		sb.append("<div class='tf-panel-desc'>Memperlihatkan total nilai pengajuan per bulan dalam bentuk tabel ringkas.</div>");
		appendPagingInfo(sb, data.trendRows.size(), PAGE_SIZE);
		if (data.trendRows.isEmpty()) {
			sb.append("<table class='tf-table'><tr><th>Bulan</th><th>Jumlah</th><th>Nominal</th></tr>");
			sb.append("<tr><td colspan='3' class='tf-muted'>Belum ada data pada filter yang dipilih.</td></tr></table>");
		} else {
			String pagerId = nextPagerId("tf_trend");
			String header = "<tr><th>Bulan</th><th>Jumlah</th><th>Nominal</th></tr>";
			appendPager(sb, pagerId, data.trendRows.size(), PAGE_SIZE);
			int rowIndex = 0;
			for (TrendRow row : data.trendRows) {
				appendPagedTableStart(sb, pagerId, rowIndex, data.trendRows.size(), PAGE_SIZE, "Nominal per Bulan", header);
				sb.append("<tr><td>").append(esc(row.label)).append("</td><td>").append(row.total)
						.append("</td><td>").append(esc(formatUang(row.nominal))).append("</td></tr>");
				rowIndex++;
			}
			appendPagedTableEnd(sb, data.trendRows.size());
		}
		sb.append("</div>");
	}

	private void appendDisposisiSopPanel(StringBuilder sb, DashboardData data) {
		sb.append("<div class='tf-panel'>");
		sb.append("<div class='tf-panel-title'>Pengajuan Berdasarkan Disposisi SOP</div>");
		sb.append("<div class='tf-panel-desc'>Menunjukkan pengajuan yang sudah terhubung dengan alur SOP dan yang belum memiliki jejak persetujuan.</div>");
		appendPagingInfo(sb, data.disposisiSopRows.size(), PAGE_SIZE);
		if (data.disposisiSopRows.isEmpty()) {
			sb.append("<table class='tf-table'><tr><th>Disposisi SOP</th><th>Total</th><th>Belum</th><th>Diajukan</th><th>Selesai</th><th>Nominal</th></tr>");
			sb.append("<tr><td colspan='6' class='tf-muted'>Belum ada data pada filter yang dipilih.</td></tr></table>");
		} else {
			String pagerId = nextPagerId("tf_sop");
			String header = "<tr><th>Disposisi SOP</th><th>Total</th><th>Belum</th><th>Diajukan</th><th>Selesai</th><th>Nominal</th></tr>";
			appendPager(sb, pagerId, data.disposisiSopRows.size(), PAGE_SIZE);
			int rowIndex = 0;
			for (DisposisiSopRow row : data.disposisiSopRows) {
				appendPagedTableStart(sb, pagerId, rowIndex, data.disposisiSopRows.size(), PAGE_SIZE,
						"Pengajuan berdasarkan Disposisi SOP", header);
				boolean tanpaSop = row.disposisiSopId <= 0L;
				sb.append("<tr><td><span class='tf-pill").append(tanpaSop ? " tf-pill-warn" : "")
						.append("'>").append(tanpaSop ? "Tanpa SOP" : "SOP").append("</span> ")
						.append(esc(row.nama)).append("</td><td>").append(row.total).append("</td><td>")
						.append(row.belum).append("</td><td>").append(row.diajukan).append("</td><td>")
						.append(row.selesai).append("</td><td>").append(esc(formatUang(row.nominal)))
						.append("</td></tr>");
				rowIndex++;
			}
			appendPagedTableEnd(sb, data.disposisiSopRows.size());
		}
		sb.append("</div>");
	}

	private void appendProsesTransitoriSopPanel(StringBuilder sb, DashboardData data) {
		sb.append("<div class='tf-panel'>");
		sb.append("<div class='tf-panel-title'>Proses Transitori Berdasarkan SOP</div>");
		sb.append("<div class='tf-panel-desc'>Memperlihatkan proses transitori yang sudah memiliki alur SOP dan yang masih perlu dilengkapi.</div>");
		appendPagingInfo(sb, data.prosesTransitoriSopRows.size(), PAGE_SIZE);
		if (data.prosesTransitoriSopRows.isEmpty()) {
			sb.append("<table class='tf-table'><tr><th>Disposisi SOP</th><th>Total</th><th>Menunggu</th><th>Disetujui</th><th>Nominal</th></tr>");
			sb.append("<tr><td colspan='5' class='tf-muted'>Belum ada data proses transitori pada filter tanggal yang dipilih.</td></tr></table>");
		} else {
			String pagerId = nextPagerId("tf_trans_sop");
			String header = "<tr><th>Disposisi SOP</th><th>Total</th><th>Menunggu</th><th>Disetujui</th><th>Nominal</th></tr>";
			appendPager(sb, pagerId, data.prosesTransitoriSopRows.size(), PAGE_SIZE);
			int rowIndex = 0;
			for (ProsesTransitoriSopRow row : data.prosesTransitoriSopRows) {
				appendPagedTableStart(sb, pagerId, rowIndex, data.prosesTransitoriSopRows.size(), PAGE_SIZE,
						"Proses transitori berdasarkan SOP", header);
				boolean tanpaSop = row.disposisiSopId <= 0L;
				sb.append("<tr><td><span class='tf-pill").append(tanpaSop ? " tf-pill-warn" : "")
						.append("'>").append(tanpaSop ? "Tanpa SOP" : "SOP").append("</span> ")
						.append(esc(row.nama)).append("</td><td>").append(row.total).append("</td><td>")
						.append(row.menungguPersetujuan).append("</td><td>").append(row.sudahDisetujui)
						.append("</td><td>").append(esc(formatUang(row.nominal))).append("</td></tr>");
				rowIndex++;
			}
			appendPagedTableEnd(sb, data.prosesTransitoriSopRows.size());
		}
		sb.append("</div>");
	}

	private String nextPagerId(String prefix) {
		dashboardPagerCounter++;
		return prefix + "_" + System.currentTimeMillis() + "_" + dashboardPagerCounter;
	}

	private void appendPagingInfo(StringBuilder sb, int totalRows, int pageSize) {
		int totalPage = totalRows <= 0 ? 1 : (totalRows + pageSize - 1) / pageSize;
		sb.append("<div class='tf-page-info'>Tabel menampilkan ").append(pageSize)
				.append(" data per halaman. Tombol halaman dibuat per 10 nomor agar daftar tetap pendek. Total data: ")
				.append(totalRows).append(", total halaman: ").append(totalPage).append(".</div>");
	}

	private void appendPager(StringBuilder sb, String pagerId, int totalRows, int pageSize) {
		int totalPage = totalRows <= 0 ? 1 : (totalRows + pageSize - 1) / pageSize;
		if (totalPage <= 1) {
			return;
		}
		sb.append("<div class='tf-pager' id='").append(pagerId).append("_bar' data-current='1' data-total='")
				.append(totalPage).append("' data-rows='").append(totalRows).append("' data-size='").append(pageSize)
				.append("'>");
		sb.append("<button type='button' class='tf-pager-btn' onclick=\"return tfDashboardShowPage('")
				.append(pagerId).append("',1)\">Awal</button>");
		sb.append("<button type='button' class='tf-pager-btn' id='").append(pagerId)
				.append("_prev' onclick=\"return tfDashboardMovePage('").append(pagerId)
				.append("',-1)\">Sebelumnya</button>");
		sb.append("<span class='tf-pager-info' id='").append(pagerId).append("_info'>Halaman 1/")
				.append(totalPage).append("</span>");
		sb.append("<span class='tf-pager-numbers' id='").append(pagerId).append("_nums'></span>");
		sb.append("<button type='button' class='tf-pager-btn' id='").append(pagerId)
				.append("_next' onclick=\"return tfDashboardMovePage('").append(pagerId)
				.append("',1)\">Berikutnya</button>");
		sb.append("<button type='button' class='tf-pager-btn' onclick=\"return tfDashboardShowPage('")
				.append(pagerId).append("',").append(totalPage).append(")\">Akhir</button>");
		sb.append("</div>");
	}

	private void appendPagedTableStart(StringBuilder sb, String pagerId, int rowIndex, int totalRows, int pageSize,
			String title, String headerHtml) {
		if (rowIndex % pageSize == 0) {
			if (rowIndex > 0) {
				sb.append("</table></div>");
			}
			int page = (rowIndex / pageSize) + 1;
			int totalPage = (totalRows + pageSize - 1) / pageSize;
			int startRow = rowIndex + 1;
			int endRow = Math.min(rowIndex + pageSize, totalRows);
			sb.append("<div class='tf-page-view' id='").append(pagerId).append("_p").append(page)
					.append("' style='display:").append(page == 1 ? "block" : "none").append(";'>");
			sb.append("<div class='tf-page-title'>").append(esc(title)).append(" - Halaman ").append(page)
					.append("/").append(totalPage).append(" (data ").append(startRow).append("-").append(endRow)
					.append(")</div><table class='tf-table'>").append(headerHtml);
		}
	}

	private void appendPagedTableEnd(StringBuilder sb, int totalRows) {
		if (totalRows > 0) {
			sb.append("</table></div>");
		}
	}

	private void appendCard(StringBuilder sb, String label, long value, String note) {
		sb.append("<div class='tf-card'><div class='num'>").append(value)
				.append("</div><div class='lbl'>").append(esc(label))
				.append("</div><div class='note'>").append(esc(note)).append("</div></div>");
	}

	private void appendCardText(StringBuilder sb, String label, String value, String note) {
		sb.append("<div class='tf-card'><div class='num' style='font-size:19px;'>").append(esc(value))
				.append("</div><div class='lbl'>").append(esc(label))
				.append("</div><div class='note'>").append(esc(note)).append("</div></div>");
	}

	private long toLong(Object value) {
		if (value == null) {
			return 0L;
		}
		if (value instanceof BigInteger) {
			return ((BigInteger) value).longValue();
		}
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).longValue();
		}
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		try {
			return Long.parseLong(value.toString());
		} catch (Exception e) {
			return 0L;
		}
	}

	private double toDouble(Object value) {
		if (value == null) {
			return 0.0;
		}
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).doubleValue();
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return Double.parseDouble(value.toString());
		} catch (Exception e) {
			return 0.0;
		}
	}

	private String str(Object value) {
		return value == null ? "" : value.toString();
	}

	private String formatUang(double value) {
		return Common.numberFormat.get().format(value);
	}

	private String esc(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static class DashboardData {
		long total;
		double totalNominal;
		long belumDiproses;
		long diajukan;
		long transfer;
		long transitori;
		List jenisLabels = new ArrayList();
		List jenisValues = new ArrayList();
		List<SatkerRow> satkerRows = new ArrayList<SatkerRow>();
		List<TrendRow> trendRows = new ArrayList<TrendRow>();
		List<DisposisiSopRow> disposisiSopRows = new ArrayList<DisposisiSopRow>();
		List<ProsesTransitoriSopRow> prosesTransitoriSopRows = new ArrayList<ProsesTransitoriSopRow>();
	}

	private static class SatkerRow {
		String nama;
		long total;
		long belum;
		long diajukan;
		long selesai;
		double nominal;
	}

	private static class TrendRow {
		String label;
		long total;
		double nominal;
	}

	private static class DisposisiSopRow {
		long disposisiSopId;
		String nama;
		long total;
		double nominal;
		long belum;
		long diajukan;
		long selesai;
	}

	private static class ProsesTransitoriSopRow {
		long disposisiSopId;
		String nama;
		long total;
		double nominal;
		long menungguPersetujuan;
		long sudahDisetujui;
	}
}
