package ais.action.master.koperasi.gudang;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import ais.action.master.inventory.StokLokasiUtil;
import ais.action.master.koperasi.helper.LokasiKantinUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.JenisLokasi;
import ais.database.model.asset.Lokasi;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * <h2>LaporanPergudanganZkAction — dasbor Laporan Outlet &amp; Gudang versi <b>ZKoss</b> (ZK Grid 5.5).</h2>
 *
 * <p>
 * Padanan ZKoss dari dasbor JSP "Laporan Pergudangan". Menyajikan data stok &amp; nilai persediaan
 * sebagai <b>grid/tabel ZK 5.5</b> (bukan sekadar dump Excel), dilengkapi <b>tombol Download Excel</b>
 * dan empat visual <b>HTML/CSS (SVG) tanpa JFreeChart</b>: batang nilai per lokasi, donut komposisi
 * per jenis, tren mutasi harian, dan radar profil lokasi (dibuat oleh {@link GudangChartSvg}). Nilai
 * persediaan ditampilkan <b>dua versi</b> berdampingan: harga beli acuan dan rata-rata biaya masuk.
 * </p>
 *
 * <p>
 * Seluruh data berasal dari mesin pusat {@link StokLokasiUtil} ({@code rekapStokLokasiLengkap} dan
 * {@code trenMutasiHarian}) — <b>tanpa menduplikasi logika</b> dengan versi JSP. Filter Jenis Lokasi
 * dan Lokasi mengambil daftar dari {@link LokasiKantinUtil}. Bersifat baca-saja sehingga aman untuk
 * semua pengguna yang boleh melihat. Sesi memakai {@link HibernateUtil#currentSession()} (tidak
 * ditutup manual). Tiap panel diberi kalimat penjelas ringkas untuk pengguna awam. Kompatibel Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see StokLokasiUtil
 * @see GudangChartSvg
 */
public class LaporanPergudanganZkAction extends MyWindow {

	private static final long serialVersionUID = 4419021551123456004L;

	private static final DecimalFormat NF = new DecimalFormat("#,##0");

	private Combobox cbJenis;
	private Combobox cbLokasi;
	private Textbox cari;
	private Html dash;
	private MyGrid grid;
	private List<Object[]> lastRekap = new ArrayList<Object[]>();

	public LaporanPergudanganZkAction() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() {
		setWidth("100%");
		setHeight("100%");
		setBorder("none");
		Session session = HibernateUtil.currentSession();

		Vlayout root = new Vlayout();
		root.setStyle("padding:12px");
		root.setParent(this);
		Label ttl = new Label(ais.common.Common.getBahasaConfig("Laporan Outlet & Gudang"));
		ttl.setStyle("font-weight:800;font-size:16px;display:block");
		ttl.setParent(root);
		Label desc = new Label(ais.common.Common.getBahasaConfig("Ringkasan stok & nilai barang di tiap tempat, lengkap grafik supaya cepat dibaca."));
		desc.setStyle("display:block;color:#64748b;font-size:12px;margin-bottom:8px");
		desc.setParent(root);

		Hlayout filter = new Hlayout();
		filter.setStyle("gap:8px;align-items:center;flex-wrap:wrap;margin-bottom:8px");
		filter.setParent(root);
		new Label(ais.common.Common.getBahasaConfig("Jenis:")).setParent(filter);
		cbJenis = new Combobox();
		cbJenis.setReadonly(true);
		cbJenis.setParent(filter);
		Comboitem js = new Comboitem("Semua jenis");
		js.setValue(null);
		js.setParent(cbJenis);
		cbJenis.setSelectedItem(js);
		List<JenisLokasi> jenisList = LokasiKantinUtil.daftarJenisLokasi(session, true);
		for (int i = 0; i < jenisList.size(); i++) {
			JenisLokasi j = jenisList.get(i);
			Comboitem ci = new Comboitem(j.getNama());
			ci.setValue(j.getId());
			ci.setParent(cbJenis);
		}
		new Label(ais.common.Common.getBahasaConfig("Lokasi:")).setParent(filter);
		cbLokasi = new Combobox();
		cbLokasi.setReadonly(true);
		cbLokasi.setParent(filter);
		Comboitem ls = new Comboitem("Semua lokasi");
		ls.setValue(null);
		ls.setParent(cbLokasi);
		cbLokasi.setSelectedItem(ls);
		List<Lokasi> lokasiList = LokasiKantinUtil.daftarLokasi(session, null, null, true);
		for (int i = 0; i < lokasiList.size(); i++) {
			Lokasi l = lokasiList.get(i);
			Comboitem ci = new Comboitem(l.getNama());
			ci.setValue(l.getId());
			ci.setParent(cbLokasi);
		}
		cari = new Textbox();
		cari.setWidth("150px");
		cari.setTooltiptext("Cari produk…");
		cari.setParent(filter);
		Button terapkan = new Button("Terapkan");
		terapkan.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception {
				muat();
			}
		});
		terapkan.setParent(filter);
		Button unduh = new Button("Download Excel");
		unduh.setStyle("background:#16a34a;color:#fff");
		unduh.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event e) throws Exception {
				unduhExcel();
			}
		});
		unduh.setParent(filter);

		dash = new Html("");
		dash.setParent(root);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setStyle("margin-top:10px");
		grid.setParent(root);
		Columns cols = new Columns();
		cols.setParent(grid);
		new Column("Lokasi / Produk").setParent(cols);
		kolomKanan(cols, "Qty");
		kolomKanan(cols, "Harga Beli");
		kolomKanan(cols, "Nilai (Beli)");
		kolomKanan(cols, "Rata2 Biaya");
		kolomKanan(cols, "Nilai (Rata2)");
		new Rows().setParent(grid);

		muat();
	}

	private void kolomKanan(Columns cols, String label) {
		Column c = new Column(label);
		c.setAlign("right");
		c.setParent(cols);
	}

	private Long idCombo(Combobox cb) {
		if (cb == null || cb.getSelectedItem() == null || cb.getSelectedItem().getValue() == null) {
			return null;
		}
		try {
			return Long.valueOf(cb.getSelectedItem().getValue().toString());
		} catch (Exception e) {
			return null;
		}
	}

	/** Memuat data (rekap + tren), lalu menggambar KPI+grafik dan mengisi grid rincian. */
	private void muat() {
		Session session = HibernateUtil.currentSession();
		Long jenisId = idCombo(cbJenis);
		Long lokasiId = idCombo(cbLokasi);
		String q = cari.getValue();
		List<Object[]> rekap = StokLokasiUtil.rekapStokLokasiLengkap(session, jenisId, lokasiId, q);
		lastRekap = rekap;
		List<Object[]> tren = StokLokasiUtil.trenMutasiHarian(session, jenisId, 30);
		// Fase 2: rollup TAMBAHAN per Gudang (hierarki pusat/cabang) -- tidak menyaring by lokasiId
		// (rollup ini justru dimaksudkan utk melihat SELURUH gudang sekaligus, bukan satu lokasi).
		List<Object[]> perGudang = StokLokasiUtil.rekapStokPerGudang(session, jenisId, q);

		dash.setContent(bangunDash(rekap, tren, perGudang));
		isiGrid(rekap);
	}

	private String bangunDash(List<Object[]> rekap, List<Object[]> tren, List<Object[]> perGudang) {
		Set<Object> lokSet = new HashSet<Object>();
		Set<Object> prdSet = new HashSet<Object>();
		double totQty = 0, totNB = 0, totNA = 0;
		Map<String, double[]> lok = new LinkedHashMap<String, double[]>(); // nama -> [nilai, qty]
		Map<String, Set<Object>> lokProd = new LinkedHashMap<String, Set<Object>>();
		Map<String, Double> jns = new LinkedHashMap<String, Double>();
		Map<String, String> jnsWarna = new LinkedHashMap<String, String>();

		for (int i = 0; i < rekap.size(); i++) {
			Object[] r = rekap.get(i);
			String lokNama = str(r[1]);
			String jnsNama = str(r[3]);
			String warna = str(r[4]);
			double qty = num(r[8]);
			double nb = num(r[10]);
			double na = qty * num(r[11]);
			lokSet.add(r[0]);
			prdSet.add(r[5]);
			totQty += qty;
			totNB += nb;
			totNA += na;
			double[] lv = lok.get(lokNama);
			if (lv == null) {
				lv = new double[] { 0, 0 };
				lok.put(lokNama, lv);
				lokProd.put(lokNama, new HashSet<Object>());
			}
			lv[0] += nb;
			lv[1] += qty;
			lokProd.get(lokNama).add(r[5]);
			Double jv = jns.get(jnsNama);
			jns.put(jnsNama, Double.valueOf((jv == null ? 0 : jv.doubleValue()) + nb));
			if (!jnsWarna.containsKey(jnsNama)) {
				jnsWarna.put(jnsNama, warna);
			}
		}

		StringBuilder h = new StringBuilder();
		h.append("<style>.pan{border:1px solid #eef2f7;border-radius:14px;background:#fff;padding:12px;margin-bottom:4px}")
				.append(".pan h6{font-weight:800;margin:0 0 2px;font-size:13px}.pan .d{font-size:11px;color:#64748b;margin-bottom:8px}")
				.append(".kp{display:grid;grid-template-columns:repeat(auto-fit,minmax(130px,1fr));gap:8px;margin-bottom:10px}")
				.append(".kp .c{border:1px solid #eef2f7;border-radius:12px;padding:10px 12px;background:#fff}")
				.append(".kp .n{font-size:18px;font-weight:800;color:#0f172a}.kp .l{font-size:11px;color:#64748b}</style>");
		h.append("<div class='kp'>");
		h.append(kartu(String.valueOf(lokSet.size()), "Lokasi"));
		h.append(kartu(String.valueOf(prdSet.size()), "Jenis Barang"));
		h.append(kartu(NF.format(totQty), "Total Qty"));
		h.append(kartu("Rp " + NF.format(totNB), "Nilai (Harga Beli)"));
		h.append(kartu("Rp " + NF.format(totNA), "Nilai (Rata2 Biaya)"));
		h.append("</div>");

		h.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:12px'>");
		// Bar: nilai per lokasi (top 10)
		List<String> lkeys = new ArrayList<String>(lok.keySet());
		urutDesc(lkeys, lok);
		int nb = Math.min(10, lkeys.size());
		String[] blab = new String[nb];
		double[] bval = new double[nb];
		for (int i = 0; i < nb; i++) {
			blab[i] = lkeys.get(i);
			bval[i] = lok.get(lkeys.get(i))[0];
		}
		h.append(panel("Nilai Stok per Lokasi", "Tempat mana yang menyimpan barang paling banyak nilainya.",
				GudangChartSvg.barHorizontal(blab, bval, null)));
		// Donut per jenis
		int nj = jns.size();
		String[] jlab = new String[nj];
		double[] jval = new double[nj];
		String[] jcol = new String[nj];
		int ji = 0;
		for (Map.Entry<String, Double> en : jns.entrySet()) {
			jlab[ji] = en.getKey();
			jval[ji] = en.getValue().doubleValue();
			jcol[ji] = jnsWarna.get(en.getKey());
			ji++;
		}
		h.append(panel("Komposisi per Jenis", "Perbandingan nilai barang antar jenis tempat.",
				GudangChartSvg.donut(jlab, jval, jcol)));
		// Trend
		int nt = tren.size();
		String[] tlab = new String[nt];
		double[] tin = new double[nt];
		double[] tout = new double[nt];
		for (int i = 0; i < nt; i++) {
			Object[] t = tren.get(i);
			tlab[i] = str(t[0]);
			tin[i] = num(t[1]);
			tout[i] = num(t[2]);
		}
		h.append(panel("Tren Mutasi Harian (30 hari)", "Naik-turun barang masuk dan keluar tiap hari sebulan terakhir.",
				GudangChartSvg.trend(tlab, tin, tout)));
		// Radar top 4 lokasi: [Nilai, Ragam, Qty]
		int nr = Math.min(4, lkeys.size());
		String[] rn = new String[nr];
		double[][] rv = new double[nr][3];
		double mN = 1, mR = 1, mQ = 1;
		for (int i = 0; i < nr; i++) {
			String k = lkeys.get(i);
			mN = Math.max(mN, lok.get(k)[0]);
			mR = Math.max(mR, lokProd.get(k).size());
			mQ = Math.max(mQ, lok.get(k)[1]);
		}
		for (int i = 0; i < nr; i++) {
			String k = lkeys.get(i);
			rn[i] = k;
			rv[i][0] = lok.get(k)[0] / mN;
			rv[i][1] = lokProd.get(k).size() / mR;
			rv[i][2] = lok.get(k)[1] / mQ;
		}
		h.append(panel("Profil Lokasi (Radar)", "Membandingkan beberapa tempat dari sisi nilai, ragam barang, dan stok.",
				GudangChartSvg.radar(new String[] { "Nilai", "Ragam", "Qty" }, rn, rv, null)));
		h.append("</div>");

		// Fase 2: rollup per Gudang (hierarki pusat/cabang) -- panel penuh-lebar, TABEL bukan chart
		// (jumlah gudang biasanya sedikit, tabel lebih jelas menunjukkan pasangan gudang/induknya).
		h.append(panel("Rekap Nilai Stok per Gudang (Hierarki Pusat/Cabang)",
				"Nilai persediaan dikelompokkan per Gudang -- kolom \"Induk\" menunjukkan gudang pusatnya bila ini gudang cabang. "
						+ "Baris \"(Tanpa Gudang)\" berarti Lokasi belum ditautkan ke Gudang manapun (atur lewat tombol \"Kelola Gudang\" pada Master Lokasi).",
				tabelPerGudang(perGudang)));
		return h.toString();
	}

	/** Fase 2: tabel HTML ringkas rollup {@link StokLokasiUtil#rekapStokPerGudang}. */
	private String tabelPerGudang(List<Object[]> perGudang) {
		if (perGudang == null || perGudang.isEmpty()) {
			return "<div style='font-size:12px;color:#94a3b8;padding:8px 0'>Belum ada data stok.</div>";
		}
		StringBuilder t = new StringBuilder();
		t.append("<div style='overflow-x:auto'><table style='width:100%;border-collapse:collapse;font-size:12.5px'>");
		t.append("<thead><tr style='text-align:left;border-bottom:2px solid #eef2f7;color:#64748b'>")
				.append("<th style='padding:6px 8px'>Gudang</th><th style='padding:6px 8px'>Induk</th>")
				.append("<th style='padding:6px 8px;text-align:right'>Lokasi</th>")
				.append("<th style='padding:6px 8px;text-align:right'>Jenis Barang</th>")
				.append("<th style='padding:6px 8px;text-align:right'>Nilai (Rp)</th></tr></thead><tbody>");
		for (int i = 0; i < perGudang.size(); i++) {
			Object[] r = perGudang.get(i);
			boolean tanpaGudang = r[0] == null;
			String namaGudang = str(r[1]);
			String indukNama = str(r[2]);
			t.append("<tr style='border-bottom:1px solid #f1f5f9").append(tanpaGudang ? ";color:#94a3b8" : "").append("'>")
					.append("<td style='padding:6px 8px;font-weight:700'>").append(esc(namaGudang)).append("</td>")
					.append("<td style='padding:6px 8px'>")
					.append(indukNama.length() == 0 ? "<span style='color:#cbd5e1'>—</span>" : esc(indukNama))
					.append("</td>").append("<td style='padding:6px 8px;text-align:right'>").append(NF.format(num(r[3])))
					.append("</td>").append("<td style='padding:6px 8px;text-align:right'>").append(NF.format(num(r[4])))
					.append("</td>").append("<td style='padding:6px 8px;text-align:right;font-weight:700'>Rp ")
					.append(NF.format(num(r[5]))).append("</td></tr>");
		}
		t.append("</tbody></table></div>");
		return t.toString();
	}

	private void isiGrid(List<Object[]> rekap) {
		Rows rows = grid.getRows();
		rows.getChildren().clear();
		String curr = null;
		double sq = 0, snb = 0, sna = 0, gq = 0, gnb = 0, gna = 0;
		for (int i = 0; i < rekap.size(); i++) {
			Object[] r = rekap.get(i);
			String lokNama = str(r[1]);
			double qty = num(r[8]);
			double hb = num(r[9]);
			double nbeli = num(r[10]);
			double avg = num(r[11]);
			double navg = qty * avg;
			if (curr != null && !lokNama.equals(curr)) {
				subtotal(rows, curr, sq, snb, sna);
				sq = 0;
				snb = 0;
				sna = 0;
			}
			if (!lokNama.equals(curr)) {
				curr = lokNama;
				Row hd = new Row();
				hd.setStyle("background:#f1f5f9;font-weight:700");
				hd.setParent(rows);
				org.zkoss.zul.Cell cell = new org.zkoss.zul.Cell();
				cell.setColspan(6);
				cell.setParent(hd);
				Label hl = new Label(lokNama + (str(r[3]).length() > 0 ? "  [" + str(r[3]) + "]" : ""));
				hl.setParent(cell);
			}
			Row row = new Row();
			row.setParent(rows);
			new Label(str(r[7]) + (str(r[6]).length() > 0 ? " (" + str(r[6]) + ")" : "")).setParent(row);
			kanan(row, NF.format(qty));
			kanan(row, NF.format(hb));
			kanan(row, NF.format(nbeli));
			kanan(row, NF.format(avg));
			kanan(row, NF.format(navg));
			sq += qty;
			snb += nbeli;
			sna += navg;
			gq += qty;
			gnb += nbeli;
			gna += navg;
		}
		if (curr != null) {
			subtotal(rows, curr, sq, snb, sna);
		}
		Row total = new Row();
		total.setStyle("background:#dbeafe;font-weight:800");
		total.setParent(rows);
		new Label(ais.common.Common.getBahasaConfig("TOTAL")).setParent(total);
		kanan(total, NF.format(gq));
		kanan(total, "");
		kanan(total, NF.format(gnb));
		kanan(total, "");
		kanan(total, NF.format(gna));
	}

	private void subtotal(Rows rows, String nama, double q, double nb, double na) {
		Row r = new Row();
		r.setStyle("background:#f8fafc;font-weight:600");
		r.setParent(rows);
		new Label("Σ " + nama).setParent(r);
		kanan(r, NF.format(q));
		kanan(r, "");
		kanan(r, NF.format(nb));
		kanan(r, "");
		kanan(r, NF.format(na));
	}

	private void kanan(Row r, String v) {
		Label l = new Label(v);
		l.setStyle("text-align:right;display:block");
		l.setParent(r);
	}

	/** Menyusun berkas .xls (tabel HTML) dari rekap terakhir lalu mengirim unduhan. */
	private void unduhExcel() throws Exception {
		StringBuilder t = new StringBuilder(
				"<html xmlns:x=\"urn:schemas-microsoft-com:office:excel\"><head><meta charset=\"UTF-8\"></head><body><table border=\"1\">");
		t.append("<tr><th>Lokasi</th><th>Jenis</th><th>Kode</th><th>Produk</th><th>Qty</th><th>Harga Beli</th>")
				.append("<th>Nilai (Beli)</th><th>Rata2 Biaya</th><th>Nilai (Rata2)</th></tr>");
		for (int i = 0; i < lastRekap.size(); i++) {
			Object[] r = lastRekap.get(i);
			double qty = num(r[8]);
			t.append("<tr><td>").append(esc(str(r[1]))).append("</td><td>").append(esc(str(r[3]))).append("</td><td>")
					.append(esc(str(r[6]))).append("</td><td>").append(esc(str(r[7]))).append("</td><td>").append(qty)
					.append("</td><td>").append(num(r[9])).append("</td><td>").append(num(r[10])).append("</td><td>")
					.append(Math.round(num(r[11]))).append("</td><td>").append(Math.round(qty * num(r[11])))
					.append("</td></tr>");
		}
		t.append("</table></body></html>");
		byte[] data = ("﻿" + t.toString()).getBytes("UTF-8");
		Filedownload.save(data, "application/vnd.ms-excel", "laporan_pergudangan.xls");
	}

	// ---- util kecil ----
	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static double num(Object o) {
		return o == null ? 0d : ((Number) o).doubleValue();
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String kartu(String n, String l) {
		return "<div class='c'><div class='n'>" + esc(n) + "</div><div class='l'>" + esc(l) + "</div></div>";
	}

	private static String panel(String judul, String desc, String isi) {
		return "<div class='pan'><h6>" + esc(judul) + "</h6><div class='d'>" + esc(desc) + "</div>" + isi + "</div>";
	}

	private static void urutDesc(List<String> keys, final Map<String, double[]> lok) {
		java.util.Collections.sort(keys, new java.util.Comparator<String>() {
			public int compare(String a, String b) {
				double da = lok.get(a)[0], db = lok.get(b)[0];
				return da < db ? 1 : (da > db ? -1 : 0);
			}
		});
	}
}
