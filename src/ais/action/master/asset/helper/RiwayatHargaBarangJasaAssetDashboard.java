package ais.action.master.asset.helper;
import ais.ui.util.DashboardGridExportHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.asset.JenisAsset;
import ais.database.model.asset.KategoriAsset;
import ais.database.model.asset.KelompokAsset;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dashboard riwayat harga barang/jasa untuk referensi Permintaan Pembelian.
 * Dibuat ringan untuk ZKoss 5.5: tabel memakai paging, grafik memakai HTML/CSS,
 * dan query dibatasi agar tidak membebani memory.
 */
public class RiwayatHargaBarangJasaAssetDashboard extends Vbox {

	private static final long serialVersionUID = -7113234419294022008L;
	private static final int DEFAULT_LIMIT = 300;
	private static final int GRID_PAGE_SIZE = 10;

	private Textbox keyword;
	private Combobox sumber;
	private Vbox body;

	/**
	 * Tipe implementasi bersarang {@link HargaRow} milik {@link RiwayatHargaBarangJasaAssetDashboard}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * RiwayatHargaBarangJasaAssetDashboard}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String sumber}, {@code Date tanggal},
	 * {@code Long masterAssetId}, {@code String kodeDokumen}, {@code String kodeBarang}, {@code String
	 * namaBarang}, {@code String vendor}, {@code String jenis}. Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 *
	 * @see RiwayatHargaBarangJasaAssetDashboard
	 */
	private static class HargaRow {
		private String sumber;
		private Date tanggal;
		private Long masterAssetId;
		private String kodeDokumen;
		private String kodeBarang;
		private String namaBarang;
		private String vendor;
		private String jenis;
		private String kategori;
		private String kelompok;
		private String akun;
		private double jumlah;
		private double hargaSatuan;
		private double diskon;
		private boolean diskonPersen;
		private double persenPpn;
		private double persenPph;
		private double dpp;
		private double rataRataDpp;
		private double hargaTotal;
		private String keterangan;
	}

	/**
	 * Pembawa data/helper lokal milik {@link RiwayatHargaBarangJasaAssetDashboard} untuk summary harga. Tipe ini
	 * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
	 * jelas.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * RiwayatHargaBarangJasaAssetDashboard}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int jumlahData}, {@code int
	 * jumlahBarang}, {@code int jumlahVendor}, {@code double totalDpp}, {@code double totalHarga}, {@code double
	 * minDpp}, {@code double maxDpp}, {@code HargaRow terbaru}. Aturan bisnis bersama tetap berada pada kelas
	 * induk atau service yang dipanggilnya.</p>
	 *
	 * @see RiwayatHargaBarangJasaAssetDashboard
	 */
	private static class SummaryHarga {
		private int jumlahData;
		private int jumlahBarang;
		private int jumlahVendor;
		private double totalDpp;
		private double totalHarga;
		private double minDpp;
		private double maxDpp;
		private HargaRow terbaru;
		private Map<String, Double> dppPerBulan = new TreeMap<String, Double>(Collections.reverseOrder());
		private Map<String, Integer> jumlahPerBulan = new TreeMap<String, Integer>(Collections.reverseOrder());
		private Map<String, Double> dppPerVendor = new LinkedHashMap<String, Double>();
		private Map<String, Integer> jumlahPerVendor = new LinkedHashMap<String, Integer>();
		private Map<String, Double> dppPerKategori = new LinkedHashMap<String, Double>();
		private Map<String, Integer> jumlahPerKategori = new LinkedHashMap<String, Integer>();
		private Map<Long, String> barangUnik = new LinkedHashMap<Long, String>();
		private Map<String, String> vendorUnik = new LinkedHashMap<String, String>();
	}

	public RiwayatHargaBarangJasaAssetDashboard() {
		setWidth("100%");
		setHeight("100%");
		setStyle("overflow:auto;background:#f8fafc;padding:10px;box-sizing:border-box;");
		buildLayout();
		reload();
	}

	private void buildLayout() {
		DashboardGridExportHelper.pasang(this, "Riwayat Harga Barang Jasa Asset Dashboard");
		Toolbar toolbar = new Toolbar();
		toolbar.setWidth("100%");
		toolbar.setStyle("padding:10px;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;margin-bottom:10px;");
		toolbar.setParent(this);

		new Label("Cari barang/jasa: ").setParent(toolbar);
		keyword = new Textbox();
		keyword.setWidth("260px");
		keyword.setParent(toolbar);
		keyword.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload();
			}
		});

		new Label(ais.common.Common.getBahasaConfig(" Sumber: ")).setParent(toolbar);
		sumber = new Combobox();
		sumber.setReadonly(true);
		sumber.setWidth("150px");
		appendCombo(sumber, "Semua", "SEMUA");
		appendCombo(sumber, "PO", "PO");
		appendCombo(sumber, "BAST", "BAST");
		appendCombo(sumber, "Tagihan Vendor", "TAGIHAN_VENDOR");
		sumber.setSelectedIndex(0);
		sumber.setParent(toolbar);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Tampilkan", "/img/search.gif");
		cari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload();
			}
		});
		cari.setParent(toolbar);

		body = new Vbox();
		body.setWidth("100%");
		body.setParent(this);
	}

	private void appendCombo(Combobox combo, String label, String value) {
		Comboitem item = new Comboitem(label);
		item.setValue(value);
		item.setParent(combo);
	}

	private void reload() {
		Common.clear(body);
		String q = keyword == null ? "" : safe(keyword.getValue());
		String src = sumber != null && sumber.getSelectedItem() != null ? String.valueOf(sumber.getSelectedItem().getValue()) : "SEMUA";
		List<HargaRow> rows = loadData(q, src);
		SummaryHarga summary = buildSummary(rows);

		renderHeader(body);
		renderSummary(body, summary);
		renderGrid(body, rows, summary);
		renderTrend(body, summary);
		renderVendorKategori(body, summary);
		renderRadar(body, summary);
	}

	private void renderHeader(Component parent) {
		Panel panel = createPanel(parent, "Riwayat Harga Barang/Jasa", null);
		Panelchildren c = firstChild(panel);
		c.appendChild(new Html("<div style='font-family:Arial,sans-serif;color:#334155;font-size:12px;line-height:1.55;'>"
				+ "Harga terakhir dan rata-rata dari Tagihan Vendor, PO, dan BAST membantu petugas menyusun harga permintaan pembelian baru. "
				+ "Nilai DPP ditonjolkan agar harga dasar barang/jasa mudah dibandingkan sebelum pajak dan potongan lanjutan diperiksa."
				+ "</div>"));
	}

	private void renderSummary(Component parent, SummaryHarga s) {
		Panel panel = createPanel(parent, "Ringkasan Harga", "margin-top:10px;");
		Panelchildren c = firstChild(panel);
		double avgDpp = s.jumlahData == 0 ? 0.0 : s.totalDpp / s.jumlahData;
		double avgTotal = s.jumlahData == 0 ? 0.0 : s.totalHarga / s.jumlahData;
		String latestName = s.terbaru == null ? "-" : s.terbaru.namaBarang;
		String latestDpp = s.terbaru == null ? "Rp 0" : money(s.terbaru.dpp);
		String html = "<div style='font-family:Arial,sans-serif;'>"
				+ "<div style='font-size:12px;color:#64748b;line-height:1.5;margin-bottom:10px;'>Ringkasan ini memperlihatkan harga DPP terbaru dan rata-rata DPP dari Tagihan Vendor, PO, dan BAST.</div>"
				+ "<div style='display:flex;gap:10px;flex-wrap:wrap;'>"
				+ card("DPP Terbaru", latestDpp, latestName, "#0f766e")
				+ card("Rata-rata DPP", money(avgDpp), s.jumlahData + " riwayat", "#1d4ed8")
				+ card("Rata-rata Total", money(avgTotal), "Termasuk PPN/PPH sesuai data", "#7c3aed")
				+ card("Barang/Jasa", String.valueOf(s.jumlahBarang), s.jumlahVendor + " penyedia", "#b45309")
				+ "</div>"
				+ "<div style='margin-top:10px;background:#ecfdf5;border:1px solid #bbf7d0;border-radius:12px;padding:10px;color:#065f46;font-size:12px;line-height:1.5;'>"
				+ "DPP adalah harga dasar setelah potongan. Angka ini paling aman dipakai sebagai pembanding awal saat membuat permintaan pembelian baru."
				+ "</div></div>";
		c.appendChild(new Html(html));
	}

	private void renderGrid(Component parent, List<HargaRow> rowsData, SummaryHarga summary) {
		Panel panel = createPanel(parent, "Tabel Riwayat Harga", "margin-top:10px;");
		Panelchildren c = firstChild(panel);
		c.appendChild(new Html("<div style='font-size:12px;color:#64748b;line-height:1.5;margin-bottom:8px;'>Daftar ini menampilkan harga dari Tagihan Vendor, PO, dan BAST terbaru. Gunakan kolom DPP dan rata-rata DPP sebagai patokan cepat, lalu bandingkan dengan harga total jika pajak perlu diperhitungkan.</div>"));

		MyGrid grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(GRID_PAGE_SIZE);
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setParent(c);

		Columns columns = new Columns();
		columns.setParent(grid);
		new MyColumnConfig("Sumber").setParent(columns);
		new MyColumnConfig("Tanggal").setParent(columns);
		new MyColumnConfig("Dokumen").setParent(columns);
		new MyColumnConfig("Barang/Jasa").setParent(columns);
		new MyColumnConfig("Penyedia").setParent(columns);
		MyColumnConfig cQty = new MyColumnConfig("Jumlah");
		cQty.setAlign("right");
		cQty.setParent(columns);
		MyColumnConfig cHarga = new MyColumnConfig("Harga Satuan");
		cHarga.setAlign("right");
		cHarga.setParent(columns);
		MyColumnConfig cDpp = new MyColumnConfig("DPP");
		cDpp.setAlign("right");
		cDpp.setParent(columns);
		MyColumnConfig cAvgDpp = new MyColumnConfig("Rata-rata DPP");
		cAvgDpp.setAlign("right");
		cAvgDpp.setParent(columns);
		MyColumnConfig cTotal = new MyColumnConfig("Total");
		cTotal.setAlign("right");
		cTotal.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);
		for (HargaRow data : rowsData) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			new Label(data.sumber).setParent(row);
			new Label(formatDate(data.tanggal)).setParent(row);
			new Label(data.kodeDokumen).setParent(row);
			new Label(joinKodeNama(data.kodeBarang, data.namaBarang)).setParent(row);
			new Label(data.vendor).setParent(row);
			new Label(number(data.jumlah)).setParent(row);
			new Label(money(data.hargaSatuan)).setParent(row);
			Label dpp = new Label(money(data.dpp));
			dpp.setStyle("font-weight:bold;color:#0f766e;");
			dpp.setParent(row);
			Label avgDpp = new Label(money(data.rataRataDpp));
			avgDpp.setStyle("font-weight:bold;color:#1d4ed8;");
			avgDpp.setParent(row);
			new Label(money(data.hargaTotal)).setParent(row);
		}
		renderFooter(grid, summary);
	}

	private void renderFooter(MyGrid grid, SummaryHarga summary) {
		Foot foot = new Foot();
		foot.setParent(grid);
		Footer f1 = new Footer();
		f1.setParent(foot);
		Footer f2 = new Footer();
		f2.setParent(foot);
		Footer f3 = new Footer();
		f3.setParent(foot);
		Footer f4 = new Footer();
		f4.setParent(foot);
		Footer f5 = new Footer();
		f5.setAlign("right");
		f5.setParent(foot);
		Label total = new Label(ais.common.Common.getBahasaConfig("RATA-RATA :"));
		total.setStyle("font-weight:bold;");
		f5.appendChild(total);
		Footer f6 = new Footer();
		f6.setParent(foot);
		Footer f7 = new Footer();
		f7.setParent(foot);
		Footer f8 = new Footer();
		f8.setAlign("right");
		f8.setParent(foot);
		Label avgDpp = new Label(money(summary.jumlahData == 0 ? 0.0 : summary.totalDpp / summary.jumlahData));
		avgDpp.setStyle("font-weight:bold;color:#0f766e;");
		f8.appendChild(avgDpp);
		Footer f9 = new Footer();
		f9.setAlign("right");
		f9.setParent(foot);
		Label avgDppAll = new Label(money(summary.jumlahData == 0 ? 0.0 : summary.totalDpp / summary.jumlahData));
		avgDppAll.setStyle("font-weight:bold;color:#1d4ed8;");
		f9.appendChild(avgDppAll);
		Footer f10 = new Footer();
		f10.setAlign("right");
		f10.setParent(foot);
		Label avgTotal = new Label(money(summary.jumlahData == 0 ? 0.0 : summary.totalHarga / summary.jumlahData));
		avgTotal.setStyle("font-weight:bold;color:#0f172a;");
		f10.appendChild(avgTotal);
	}

	private void renderTrend(Component parent, SummaryHarga s) {
		Panel panel = createPanel(parent, "Tren Harga Bulanan", "margin-top:10px;");
		Panelchildren c = firstChild(panel);
		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;'>");
		html.append("<div style='font-size:12px;color:#64748b;line-height:1.5;margin-bottom:10px;'>Perubahan rata-rata DPP ditampilkan dari periode terbaru ke periode lama agar kenaikan atau penurunan harga lebih cepat terlihat.</div>");
		html.append(buildMonthlyTrend(s));
		html.append("</div>");
		c.appendChild(new Html(html.toString()));
	}

	private void renderVendorKategori(Component parent, SummaryHarga s) {
		Hbox hbox = new Hbox();
		hbox.setWidth("100%");
		hbox.setStyle("gap:10px;align-items:stretch;margin-top:10px;");
		hbox.setParent(parent);
		Panel vendor = createPanel(hbox, "Penyedia dengan Harga Tercatat", "width:50%;");
		firstChild(vendor).appendChild(new Html("<div style='font-size:12px;color:#64748b;line-height:1.5;margin-bottom:8px;'>Perbandingan penyedia membantu petugas melihat vendor mana yang paling sering muncul dan rata-rata DPP yang pernah digunakan.</div>" + buildTopList(s.dppPerVendor, s.jumlahPerVendor, true)));
		Panel kategori = createPanel(hbox, "Komposisi Kategori Barang/Jasa", "width:50%;");
		firstChild(kategori).appendChild(new Html("<div style='font-size:12px;color:#64748b;line-height:1.5;margin-bottom:8px;'>Komposisi ini memperlihatkan kelompok pengadaan yang paling banyak menyerap nilai harga.</div>" + buildTopList(s.dppPerKategori, s.jumlahPerKategori, true)));
	}

	private void renderRadar(Component parent, SummaryHarga s) {
		Panel panel = createPanel(parent, "Spider Kewajaran Harga", "margin-top:10px;");
		Panelchildren c = firstChild(panel);
		double avg = s.jumlahData == 0 ? 0.0 : s.totalDpp / s.jumlahData;
		double last = s.terbaru == null ? 0.0 : s.terbaru.dpp;
		int kedekatanRata = percent(100.0 - (avg <= 0.0 ? 0.0 : Math.abs(last - avg) * 100.0 / avg));
		int aktivitas = percent(Math.min(100.0, s.jumlahData * 7.5));
		int variasiVendor = percent(Math.min(100.0, s.jumlahVendor * 12.5));
		int stabilitas = percent(s.maxDpp <= 0.0 ? 0.0 : 100.0 - ((s.maxDpp - s.minDpp) * 100.0 / s.maxDpp));
		int kelengkapan = percent((s.jumlahBarang > 0 ? 25.0 : 0.0) + (s.jumlahVendor > 0 ? 25.0 : 0.0)
				+ (s.jumlahData > 0 ? 25.0 : 0.0) + (s.dppPerBulan.size() > 1 ? 25.0 : 0.0));

		String html = "<div style='font-family:Arial,sans-serif;'>"
				+ "<div style='font-size:12px;color:#64748b;line-height:1.5;margin-bottom:10px;'>Spider ini memberi gambaran cepat apakah harga terbaru masih dekat dengan rata-rata, punya riwayat cukup, dan memiliki pembanding dari beberapa penyedia.</div>"
				+ buildRadarHtml(kedekatanRata, aktivitas, variasiVendor, stabilitas, kelengkapan)
				+ "</div>";
		c.appendChild(new Html(html));
	}

	@SuppressWarnings("unchecked")
	private List<HargaRow> loadData(String keyword, String sumber) {
		Session session = null;
		List<HargaRow> rows = new ArrayList<HargaRow>();
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			if (!"BAST".equalsIgnoreCase(sumber) && !"TAGIHAN_VENDOR".equalsIgnoreCase(sumber)) {
				Criteria criteriaPo = session.createCriteria(PemesananPengadaanMasterAssetDetail.class);
				criteriaPo.createAlias("masterAsset", "ma");
				criteriaPo.createAlias("pemesananPengadaanMasterAsset", "po");
				criteriaPo.add(Restrictions.isNotNull("masterAsset"));
				applyKeyword(criteriaPo, keyword, "PO");
				criteriaPo.addOrder(Order.desc("id"));
				criteriaPo.setMaxResults(DEFAULT_LIMIT);
				List<PemesananPengadaanMasterAssetDetail> dataPo = criteriaPo.list();
				for (PemesananPengadaanMasterAssetDetail d : dataPo) {
					HargaRow row = fromPo(d);
					if (row != null) {
						rows.add(row);
					}
				}
			}
			if (!"PO".equalsIgnoreCase(sumber) && !"TAGIHAN_VENDOR".equalsIgnoreCase(sumber)) {
				Criteria criteriaBast = session.createCriteria(PenerimaanPengadaanMasterAssetDetail.class);
				criteriaBast.createAlias("masterAsset", "ma");
				criteriaBast.createAlias("penerimaanPengadaanMasterAsset", "bast");
				criteriaBast.add(Restrictions.isNotNull("masterAsset"));
				applyKeyword(criteriaBast, keyword, "BAST");
				criteriaBast.addOrder(Order.desc("id"));
				criteriaBast.setMaxResults(DEFAULT_LIMIT);
				List<PenerimaanPengadaanMasterAssetDetail> dataBast = criteriaBast.list();
				for (PenerimaanPengadaanMasterAssetDetail d : dataBast) {
					HargaRow row = fromBast(d);
					if (row != null) {
						rows.add(row);
					}
				}
			}
			if (!"PO".equalsIgnoreCase(sumber) && !"BAST".equalsIgnoreCase(sumber)) {
				Criteria criteriaTagihanVendor = session.createCriteria(SaldoAwalMasterAssetDetail.class);
				criteriaTagihanVendor.createAlias("masterAsset", "ma");
				criteriaTagihanVendor.createAlias("saldoAwal", "saldo");
				criteriaTagihanVendor.add(Restrictions.isNotNull("masterAsset"));
				applyKeyword(criteriaTagihanVendor, keyword, "TAGIHAN_VENDOR");
				criteriaTagihanVendor.addOrder(Order.desc("id"));
				criteriaTagihanVendor.setMaxResults(DEFAULT_LIMIT);
				List<SaldoAwalMasterAssetDetail> dataTagihanVendor = criteriaTagihanVendor.list();
				for (SaldoAwalMasterAssetDetail d : dataTagihanVendor) {
					HargaRow row = fromTagihanVendor(d);
					if (row != null) {
						rows.add(row);
					}
				}
			}
			applyAverageDppPerBarang(rows);
			Collections.sort(rows, new Comparator<HargaRow>() {
				@Override
				public int compare(HargaRow o1, HargaRow o2) {
					Date d1 = o1 == null ? null : o1.tanggal;
					Date d2 = o2 == null ? null : o2.tanggal;
					if (d1 == null && d2 == null) {
						return 0;
					}
					if (d1 == null) {
						return 1;
					}
					if (d2 == null) {
						return -1;
					}
					return d2.compareTo(d1);
				}
			});
			if (rows.size() > DEFAULT_LIMIT) {
				return new ArrayList<HargaRow>(rows.subList(0, DEFAULT_LIMIT));
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
		return rows;
	}

	private void applyKeyword(Criteria criteria, String keyword, String sourceType) {
		if (criteria == null || keyword == null || keyword.trim().length() == 0) {
			return;
		}
		Disjunction disjunction = Restrictions.disjunction();
		disjunction.add(Restrictions.ilike("ma.nama", keyword.trim(), MatchMode.ANYWHERE));
		disjunction.add(Restrictions.ilike("ma.kode", keyword.trim(), MatchMode.ANYWHERE));
		if ("BAST".equalsIgnoreCase(sourceType)) {
			disjunction.add(Restrictions.ilike("bast.kode", keyword.trim(), MatchMode.ANYWHERE));
			disjunction.add(Restrictions.ilike("bast.kodeTagihan", keyword.trim(), MatchMode.ANYWHERE));
		} else if ("TAGIHAN_VENDOR".equalsIgnoreCase(sourceType)) {
			disjunction.add(Restrictions.ilike("saldo.kode", keyword.trim(), MatchMode.ANYWHERE));
			disjunction.add(Restrictions.ilike("saldo.kodeTagihan", keyword.trim(), MatchMode.ANYWHERE));
		} else {
			disjunction.add(Restrictions.ilike("po.kode", keyword.trim(), MatchMode.ANYWHERE));
			disjunction.add(Restrictions.ilike("po.kodeInvoice", keyword.trim(), MatchMode.ANYWHERE));
		}
		criteria.add(disjunction);
	}

	private void applyAverageDppPerBarang(List<HargaRow> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Map<Long, Double> totalByAsset = new LinkedHashMap<Long, Double>();
		Map<Long, Integer> countByAsset = new LinkedHashMap<Long, Integer>();
		for (HargaRow row : rows) {
			if (row == null || row.masterAssetId == null || row.dpp <= 0.0d) {
				continue;
			}
			Double total = totalByAsset.get(row.masterAssetId);
			Integer count = countByAsset.get(row.masterAssetId);
			totalByAsset.put(row.masterAssetId, Double.valueOf((total == null ? 0.0d : total.doubleValue()) + row.dpp));
			countByAsset.put(row.masterAssetId, Integer.valueOf((count == null ? 0 : count.intValue()) + 1));
		}
		for (HargaRow row : rows) {
			if (row == null || row.masterAssetId == null) {
				continue;
			}
			Double total = totalByAsset.get(row.masterAssetId);
			Integer count = countByAsset.get(row.masterAssetId);
			row.rataRataDpp = total == null || count == null || count.intValue() <= 0 ? row.dpp
					: total.doubleValue() / count.intValue();
		}
	}

	private HargaRow fromTagihanVendor(SaldoAwalMasterAssetDetail d) {
		if (d == null || d.getMasterAsset() == null) {
			return null;
		}
		HargaRow row = new HargaRow();
		row.sumber = "TAGIHAN VENDOR";
		row.masterAssetId = d.getMasterAsset().getId();
		row.kodeBarang = safe(d.getMasterAsset().getKode());
		row.namaBarang = safe(d.getMasterAsset().getNama());
		row.jenis = namaJenis(d.getMasterAsset());
		row.kategori = namaKategori(d.getMasterAsset());
		row.kelompok = namaKelompok(d.getMasterAsset());
		row.akun = namaAkun(d.getMasterAsset());
		row.jumlah = nz(d.getJumlah());
		row.hargaSatuan = nz(d.getHarga());
		row.diskon = nz(d.getHargaPotongan());
		row.diskonPersen = d.getDiskonDalamBentukPersen() != null && d.getDiskonDalamBentukPersen().booleanValue();
		row.persenPpn = nz(d.getPersenPpn());
		row.persenPph = nz(d.getPersenPph());
		row.dpp = calculateDpp(row.jumlah, row.hargaSatuan, row.diskon, row.diskonPersen);
		row.hargaTotal = nz(d.getHargaTotal());
		row.keterangan = safe(d.getKeterangan());
		SaldoAwalMasterAsset saldoAwal = d.getSaldoAwal();
		if (saldoAwal != null) {
			row.kodeDokumen = safe(saldoAwal.getKodeTagihan());
			if (row.kodeDokumen == null || row.kodeDokumen.length() == 0) {
				row.kodeDokumen = safe(saldoAwal.getKode());
			}
			row.tanggal = saldoAwal.getTanggalTagihan() == null
					? (saldoAwal.getTanggalPersetujuan() == null ? saldoAwal.getTanggalPembuatan()
							: saldoAwal.getTanggalPersetujuan())
					: saldoAwal.getTanggalTagihan();
			row.vendor = namaPenyedia(saldoAwal.getPenyedia());
		}
		if (row.kodeDokumen == null || row.kodeDokumen.length() == 0) {
			row.kodeDokumen = "TAGIHAN-" + d.getId();
		}
		return row;
	}

	private HargaRow fromPo(PemesananPengadaanMasterAssetDetail d) {
		if (d == null || d.getMasterAsset() == null) {
			return null;
		}
		HargaRow row = new HargaRow();
		row.sumber = "PO";
		row.masterAssetId = d.getMasterAsset().getId();
		row.kodeBarang = safe(d.getMasterAsset().getKode());
		row.namaBarang = safe(d.getMasterAsset().getNama());
		row.jenis = namaJenis(d.getMasterAsset());
		row.kategori = namaKategori(d.getMasterAsset());
		row.kelompok = namaKelompok(d.getMasterAsset());
		row.akun = namaAkun(d.getMasterAsset());
		row.jumlah = nz(d.getJumlah());
		row.hargaSatuan = nz(d.getHargaBeli());
		row.diskon = nz(d.getHargaPotongan());
		row.diskonPersen = d.getDiskonDalamBentukPersen() != null && d.getDiskonDalamBentukPersen().booleanValue();
		row.persenPpn = nz(d.getPersenPpn());
		row.persenPph = nz(d.getPersenPph());
		row.dpp = calculateDpp(row.jumlah, row.hargaSatuan, row.diskon, row.diskonPersen);
		row.hargaTotal = nz(d.getHargaTotal());
		row.keterangan = safe(d.getKeterangan());
		PemesananPengadaanMasterAsset po = d.getPemesananPengadaanMasterAsset();
		if (po != null) {
			row.kodeDokumen = safe(po.getKode());
			row.tanggal = po.getTanggalPersetujuan() == null ? po.getTanggalPembuatan() : po.getTanggalPersetujuan();
			row.vendor = namaPenyedia(po.getPenyedia());
		}
		if (row.kodeDokumen == null || row.kodeDokumen.length() == 0) {
			row.kodeDokumen = "PO-" + d.getId();
		}
		return row;
	}

	private HargaRow fromBast(PenerimaanPengadaanMasterAssetDetail d) {
		if (d == null || d.getMasterAsset() == null) {
			return null;
		}
		HargaRow row = new HargaRow();
		row.sumber = "BAST";
		row.masterAssetId = d.getMasterAsset().getId();
		row.kodeBarang = safe(d.getMasterAsset().getKode());
		row.namaBarang = safe(d.getMasterAsset().getNama());
		row.jenis = namaJenis(d.getMasterAsset());
		row.kategori = namaKategori(d.getMasterAsset());
		row.kelompok = namaKelompok(d.getMasterAsset());
		row.akun = namaAkun(d.getMasterAsset());
		row.jumlah = nz(d.getDiterima());
		row.hargaSatuan = nz(d.getHargaBeli());
		row.diskon = nz(d.getHargaPotongan());
		row.diskonPersen = d.getDiskonDalamBentukPersen() != null && d.getDiskonDalamBentukPersen().booleanValue();
		row.persenPpn = nz(d.getPersenPpn());
		row.persenPph = nz(d.getPersenPph());
		row.dpp = calculateDpp(row.jumlah, row.hargaSatuan, row.diskon, row.diskonPersen);
		row.hargaTotal = nz(d.getHargaTotal());
		row.keterangan = safe(d.getKeterangan());
		PenerimaanPengadaanMasterAsset bast = d.getPenerimaanPengadaanMasterAsset();
		if (bast != null) {
			row.kodeDokumen = safe(bast.getKode());
			row.tanggal = bast.getTanggalPersetujuan() == null ? bast.getTanggalPembuatan() : bast.getTanggalPersetujuan();
			row.vendor = namaPenyedia(bast.getPenyedia());
		}
		if (row.kodeDokumen == null || row.kodeDokumen.length() == 0) {
			row.kodeDokumen = "BAST-" + d.getId();
		}
		return row;
	}

	private SummaryHarga buildSummary(List<HargaRow> rows) {
		SummaryHarga s = new SummaryHarga();
		if (rows == null) {
			return s;
		}
		s.minDpp = rows.isEmpty() ? 0.0 : Double.MAX_VALUE;
		for (HargaRow row : rows) {
			if (row == null) {
				continue;
			}
			s.jumlahData++;
			s.totalDpp += row.dpp;
			s.totalHarga += row.hargaTotal;
			if (s.terbaru == null) {
				s.terbaru = row;
			}
			if (row.dpp < s.minDpp) {
				s.minDpp = row.dpp;
			}
			if (row.dpp > s.maxDpp) {
				s.maxDpp = row.dpp;
			}
			if (row.masterAssetId != null) {
				s.barangUnik.put(row.masterAssetId, row.namaBarang);
			}
			if (row.vendor != null && row.vendor.trim().length() > 0) {
				s.vendorUnik.put(row.vendor, row.vendor);
			}
			String month = monthKey(row.tanggal);
			addValue(s.dppPerBulan, month, row.dpp);
			addInt(s.jumlahPerBulan, month, 1);
			String vendor = row.vendor == null || row.vendor.length() == 0 ? "Tidak ada penyedia" : row.vendor;
			addValue(s.dppPerVendor, vendor, row.dpp);
			addInt(s.jumlahPerVendor, vendor, 1);
			String kategori = row.kategori == null || row.kategori.length() == 0 ? row.jenis : row.kategori;
			if (kategori == null || kategori.length() == 0) {
				kategori = "Tidak ada kategori";
			}
			addValue(s.dppPerKategori, kategori, row.dpp);
			addInt(s.jumlahPerKategori, kategori, 1);
		}
		s.jumlahBarang = s.barangUnik.size();
		s.jumlahVendor = s.vendorUnik.size();
		if (s.minDpp == Double.MAX_VALUE) {
			s.minDpp = 0.0;
		}
		return s;
	}

	private String buildMonthlyTrend(SummaryHarga s) {
		if (s.dppPerBulan.isEmpty()) {
			return emptyBox("Belum ada riwayat harga pada filter ini.");
		}
		StringBuilder sb = new StringBuilder();
		double max = 0.0;
		Map<String, Double> avgMap = new TreeMap<String, Double>(Collections.reverseOrder());
		for (String key : s.dppPerBulan.keySet()) {
			double avg = s.dppPerBulan.get(key).doubleValue() / Math.max(1, s.jumlahPerBulan.get(key).intValue());
			avgMap.put(key, Double.valueOf(avg));
			if (avg > max) {
				max = avg;
			}
		}
		if (max <= 0.0) {
			max = 1.0;
		}
		sb.append("<div style='background:white;border:1px solid #e2e8f0;border-radius:14px;padding:12px;'>");
		for (String key : avgMap.keySet()) {
			double avg = avgMap.get(key).doubleValue();
			sb.append("<div style='margin:8px 0;'>");
			sb.append("<div style='display:flex;justify-content:space-between;font-size:11px;color:#334155;font-weight:700;'><span>").append(html(displayMonthKey(key))).append("</span><span>").append(money(avg)).append("</span></div>");
			sb.append("<div style='height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:4px;'><div style='height:12px;width:").append(percent(avg * 100.0 / max)).append("%;background:linear-gradient(90deg,#0ea5e9,#2563eb);border-radius:999px;'></div></div>");
			sb.append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String buildTopList(Map<String, Double> data, Map<String, Integer> counter, boolean average) {
		if (data == null || data.isEmpty()) {
			return emptyBox("Belum ada data yang bisa dibandingkan.");
		}
		List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(data.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			@Override
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		double max = list.isEmpty() ? 1.0 : list.get(0).getValue().doubleValue();
		if (max <= 0.0) {
			max = 1.0;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='background:white;border:1px solid #e2e8f0;border-radius:14px;padding:12px;'>");
		int limit = Math.min(8, list.size());
		for (int i = 0; i < limit; i++) {
			Map.Entry<String, Double> e = list.get(i);
			int count = counter == null || counter.get(e.getKey()) == null ? 1 : counter.get(e.getKey()).intValue();
			double value = average ? e.getValue().doubleValue() / Math.max(1, count) : e.getValue().doubleValue();
			sb.append("<div style='margin:8px 0;'>");
			sb.append("<div style='display:flex;justify-content:space-between;gap:8px;font-size:11px;color:#334155;font-weight:700;'><span>").append(html(e.getKey())).append("</span><span>").append(money(value)).append(" • ").append(count).append("x</span></div>");
			sb.append("<div style='height:10px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:4px;'><div style='height:10px;width:").append(percent(e.getValue().doubleValue() * 100.0 / max)).append("%;background:#14b8a6;border-radius:999px;'></div></div>");
			sb.append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String buildRadarHtml(int kedekatan, int aktivitas, int vendor, int stabilitas, int kelengkapan) {
		int p1 = clamp(kedekatan);
		int p2 = clamp(aktivitas);
		int p3 = clamp(vendor);
		int p4 = clamp(stabilitas);
		int p5 = clamp(kelengkapan);
		String polygon = "50% " + (50 - p1 / 2) + "%, " + (50 + p2 / 2) + "% " + (50 - p2 / 5) + "%, "
				+ (50 + p3 / 3) + "% " + (50 + p3 / 2) + "%, " + (50 - p4 / 3) + "% " + (50 + p4 / 2)
				+ "%, " + (50 - p5 / 2) + "% " + (50 - p5 / 5) + "%";
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex;gap:16px;align-items:center;flex-wrap:wrap;background:white;border:1px solid #e2e8f0;border-radius:14px;padding:14px;'>");
		sb.append("<div style='position:relative;width:220px;height:220px;border-radius:50%;background:repeating-radial-gradient(circle,#f8fafc 0,#f8fafc 22px,#e2e8f0 23px,#e2e8f0 24px);border:1px solid #cbd5e1;'>");
		sb.append("<div style='position:absolute;left:0;top:0;width:220px;height:220px;clip-path:polygon(").append(polygon).append(");background:linear-gradient(135deg,rgba(20,184,166,.65),rgba(37,99,235,.45));'></div>");
		sb.append("<div style='position:absolute;left:88px;top:6px;font-size:10px;color:#334155;font-weight:700;'>Rata-rata</div>");
		sb.append("<div style='position:absolute;right:2px;top:82px;font-size:10px;color:#334155;font-weight:700;'>Aktif</div>");
		sb.append("<div style='position:absolute;right:14px;bottom:24px;font-size:10px;color:#334155;font-weight:700;'>Vendor</div>");
		sb.append("<div style='position:absolute;left:16px;bottom:24px;font-size:10px;color:#334155;font-weight:700;'>Stabil</div>");
		sb.append("<div style='position:absolute;left:2px;top:82px;font-size:10px;color:#334155;font-weight:700;'>Lengkap</div>");
		sb.append("</div>");
		sb.append("<div style='flex:1;min-width:260px;'>");
		sb.append(gauge("Dekat dengan rata-rata", p1, "#0f766e"));
		sb.append(gauge("Riwayat cukup", p2, "#2563eb"));
		sb.append(gauge("Pembanding penyedia", p3, "#7c3aed"));
		sb.append(gauge("Harga stabil", p4, "#f97316"));
		sb.append(gauge("Data lengkap", p5, "#14b8a6"));
		sb.append("</div></div>");
		return sb.toString();
	}

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
		return "<div style='background:white;border:1px solid #e2e8f0;border-radius:14px;padding:13px;box-shadow:0 2px 8px rgba(15,23,42,0.07);min-width:180px;flex:1;'>"
				+ "<div style='font-size:11px;color:#64748b;font-weight:700;text-transform:uppercase;'>" + html(title) + "</div>"
				+ "<div style='font-size:19px;color:" + color + ";font-weight:800;margin-top:5px;'>" + html(value) + "</div>"
				+ "<div style='font-size:11px;color:#64748b;margin-top:4px;line-height:1.35;'>" + html(desc) + "</div></div>";
	}

	private String gauge(String label, int percent, String color) {
		return "<div style='margin-bottom:10px;'><div style='display:flex;justify-content:space-between;font-size:11px;font-weight:700;color:#334155;'><span>"
				+ html(label) + "</span><span>" + percent + "%</span></div>"
				+ "<div style='height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:4px;'>"
				+ "<div style='height:12px;width:" + percent + "%;background:" + color + ";border-radius:999px;'></div></div></div>";
	}

	private String emptyBox(String message) {
		return "<div style='padding:12px;color:#64748b;background:#ffffff;border:1px dashed #cbd5e1;border-radius:10px;font-size:12px;'>"
				+ html(message) + "</div>";
	}

	private String namaJenis(MasterAsset asset) {
		try {
			JenisAsset jenis = asset == null ? null : asset.getJenisAsset();
			return jenis == null ? "" : safe(jenis.getNama());
		} catch (Exception e) {
			return "";
		}
	}

	private String namaKategori(MasterAsset asset) {
		try {
			KategoriAsset kategori = asset == null ? null : asset.getKategoriAsset();
			return kategori == null ? "" : safe(kategori.getNama());
		} catch (Exception e) {
			return "";
		}
	}

	private String namaKelompok(MasterAsset asset) {
		try {
			KelompokAsset kelompok = asset == null ? null : asset.getKelompokAsset();
			return kelompok == null ? "" : safe(kelompok.getNama());
		} catch (Exception e) {
			return "";
		}
	}

	private String namaAkun(MasterAsset asset) {
		try {
			Akun akun = asset == null ? null : asset.getAkunTransaksiA();
			return akun == null ? "" : safe(akun.toString());
		} catch (Exception e) {
			return "";
		}
	}

	private String namaPenyedia(PenyediaAsset penyedia) {
		try {
			return penyedia == null ? "" : safe(penyedia.getNama());
		} catch (Exception e) {
			return "";
		}
	}

	private double calculateDpp(double jumlah, double harga, double diskon, boolean diskonPersen) {
		double bruto = jumlah * harga;
		double potongan = diskonPersen ? (diskon / 100.0d) * bruto : diskon;
		double dpp = bruto - potongan;
		return dpp < 0.0 ? 0.0 : dpp;
	}

	private double nz(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}

	private int percent(double value) {
		return clamp((int) Math.round(value));
	}

	private int clamp(int value) {
		if (value < 0) {
			return 0;
		}
		if (value > 100) {
			return 100;
		}
		return value;
	}

	private void addValue(Map<String, Double> map, String key, double value) {
		if (key == null || key.trim().length() == 0) {
			key = "Tidak diketahui";
		}
		Double old = map.get(key);
		map.put(key, Double.valueOf((old == null ? 0.0 : old.doubleValue()) + value));
	}

	private void addInt(Map<String, Integer> map, String key, int value) {
		if (key == null || key.trim().length() == 0) {
			key = "Tidak diketahui";
		}
		Integer old = map.get(key);
		map.put(key, Integer.valueOf((old == null ? 0 : old.intValue()) + value));
	}

	private String monthKey(Date date) {
		if (date == null) {
			return "000000|Tanpa Tanggal";
		}
		try {
			String sortKey = new java.text.SimpleDateFormat("yyyyMM").format(date);
			return sortKey + "|" + Common.dateFormat4.get().format(date);
		} catch (Exception e) {
			return "000000|Tanpa Tanggal";
		}
	}

	private String displayMonthKey(String key) {
		if (key == null || key.trim().length() == 0) {
			return "Tanpa Tanggal";
		}
		int index = key.indexOf('|');
		return index >= 0 && index + 1 < key.length() ? key.substring(index + 1) : key;
	}

	private String formatDate(Date date) {
		if (date == null) {
			return "-";
		}
		try {
			return Common.dateFormat4.get().format(date);
		} catch (Exception e) {
			return "-";
		}
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

	private String joinKodeNama(String kode, String nama) {
		kode = safe(kode);
		nama = safe(nama);
		if (kode.length() == 0) {
			return nama;
		}
		if (nama.length() == 0) {
			return kode;
		}
		return kode + " - " + nama;
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

	private void closeSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/RiwayatHargaBarangJasaAssetDashboard.java:928");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/RiwayatHargaBarangJasaAssetDashboard.java:932");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/RiwayatHargaBarangJasaAssetDashboard.java:938");
		}
	}
}
