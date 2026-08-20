package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>PengadaanTahapDashboard — Dasbor satu tahap Pengadaan (PR, PO, BAST, Terima
 * Tagihan, Pembayaran Vendor, Pajak)</h3>
 *
 * <p>Dipasang sebagai TAB pertama pada halaman pengadaan yang belum memiliki dasbor,
 * berdampingan dengan tab CRUD-nya. Bentuknya mengikuti dasbor yang sudah ada
 * ({@code TraceStatusPengadaanAssetDashboard}, {@code DasboardPajak}): kartu ringkasan,
 * grafik tren, komposisi, peringkat, dan tabel "perlu perhatian" — seluruhnya dirangkai
 * dengan {@link DashboardUiKit}.</p>
 *
 * <p><b>Sumber angkanya sengaja BUKAN query tersendiri.</b> Komponen ini memanggil aksi
 * {@code PengadaanPosApiHelper.dasbor} yang juga melayani dasbor POS (Desktop, Android,
 * JSP). Dengan begitu angka di layar ZKoss dan di POS tidak mungkin berbeda — kalau
 * perhitungannya disalin dua kali, cepat atau lambat keduanya akan menyimpang tanpa ada
 * yang menyadarinya.</p>
 *
 * <p><b>Pemeliharaan:</b> menambah kartu atau grafik cukup dilakukan sekali di
 * {@code PengadaanPosApiHelper.dasbor}; komponen ini merender apa pun yang dikirim.</p>
 */
public class PengadaanTahapDashboard extends Vbox {

	private static final long serialVersionUID = 8123456700099001L;

	/** pr, po, bast, tagihan, dpc, atau pajak. */
	private final String tahap;
	private final Html slot = new Html("");
	private Combobox pilihBulan;

	public PengadaanTahapDashboard(String tahap) {
		this.tahap = tahap == null ? "pr" : tahap;
		setWidth("100%");
		setHeight("100%");
		setStyle("overflow:auto;background:#f8fafc;padding:10px;box-sizing:border-box;");
		bangun();
		muat();
	}

	private void bangun() {
		Toolbar toolbar = new Toolbar();
		toolbar.setWidth("100%");
		toolbar.setStyle("padding:10px;background:#ffffff;border:1px solid #e2e8f0;"
				+ "border-radius:12px;margin-bottom:10px;");
		toolbar.setParent(this);

		new Label(Common.getBahasaConfig("Periode: ")).setParent(toolbar);
		pilihBulan = new Combobox();
		pilihBulan.setReadonly(true);
		pilihBulan.setWidth("120px");
		int[] pilihan = { 3, 6, 12, 24 };
		for (int i = 0; i < pilihan.length; i++) {
			Comboitem item = new Comboitem(pilihan[i] + " bulan");
			item.setValue(Integer.valueOf(pilihan[i]));
			item.setParent(pilihBulan);
			if (pilihan[i] == 12) {
				pilihBulan.setSelectedItem(item);
			}
		}
		pilihBulan.setParent(toolbar);
		pilihBulan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				muat();
			}
		});

		MyToolbarbuttonConfig segarkan =
				new MyToolbarbuttonConfig(Common.getBahasaConfig("Tampilkan"), "/img/search.gif");
		segarkan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				muat();
			}
		});
		segarkan.setParent(toolbar);

		slot.setParent(this);
	}

	private int bulanTerpilih() {
		try {
			if (pilihBulan != null && pilihBulan.getSelectedItem() != null) {
				return ((Integer) pilihBulan.getSelectedItem().getValue()).intValue();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PengadaanTahapDashboard.bulanTerpilih");
		}
		return 12;
	}

	private void muat() {
		slot.setContent("<div style='padding:18px;color:#64748b;font-size:12px;'>"
				+ Common.getBahasaConfig("Memuat dasbor...") + "</div>");
		try {
			JSONObject permintaan = new JSONObject();
			permintaan.put("tahap", tahap);
			permintaan.put("bulan", bulanTerpilih());
			JSONObject hasil = new JSONObject();
			ais.action.servlet.api.PengadaanPosApiHelper.dasbor(Common.getCurrentUser(),
					permintaan, hasil);
			if (!"00".equals(hasil.optString("status"))) {
				slot.setContent("<div style='padding:18px;border-radius:14px;background:#fff7ed;"
						+ "border:1px solid #fed7aa;color:#9a3412;font-size:12px;'>"
						+ DashboardUiKit.esc(hasil.optString("description", "Gagal memuat dasbor."))
						+ "</div>");
				return;
			}
			slot.setContent(rangkai(hasil));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PengadaanTahapDashboard.muat tahap=" + tahap);
			slot.setContent("<div style='padding:18px;border-radius:14px;background:#fef2f2;"
					+ "border:1px solid #fecaca;color:#991b1b;font-size:12px;'>"
					+ DashboardUiKit.esc("Dasbor gagal dimuat: " + e.getMessage()) + "</div>");
		}
	}

	private String rangkai(JSONObject d) throws Exception {
		StringBuilder sb = new StringBuilder();

		// --- Kartu ringkasan -----------------------------------------------------
		JSONArray kpi = d.optJSONArray("kpi");
		List<DashboardUiKit.Stat> stats = new ArrayList<DashboardUiKit.Stat>();
		for (int i = 0; kpi != null && i < kpi.length(); i++) {
			JSONObject k = kpi.getJSONObject(i);
			stats.add(new DashboardUiKit.Stat(k.optString("label", ""), k.optString("nilai", ""),
					k.optString("catatan", ""), k.optString("warna", "#2563eb")));
		}
		if (!stats.isEmpty()) {
			sb.append(DashboardUiKit.cards(stats));
		}

		sb.append(DashboardUiKit.openGrid(320));

		// --- Corong tahapan (khusus PR) ------------------------------------------
		JSONArray corong = d.optJSONArray("corong");
		if (corong != null && corong.length() > 0) {
			LinkedHashMap<String, Double> peta = new LinkedHashMap<String, Double>();
			for (int i = 0; i < corong.length(); i++) {
				JSONObject t = corong.getJSONObject(i);
				peta.put(t.optString("label", ""), Double.valueOf(t.optDouble("nilai", 0)));
			}
			sb.append(DashboardUiKit.barList("Ringkasan Tahapan",
					"Sejauh mana permintaan berjalan: PR, PO, BAST, Tagihan, sampai Dibayar.",
					peta, "#1d4ed8", "dokumen", false, "Belum ada permintaan pada periode ini."));
		}

		// --- Tren bulanan: nilai dan jumlah dokumen ------------------------------
		JSONArray tren = d.optJSONArray("tren");
		if (tren != null && tren.length() > 0) {
			List<String> label = new ArrayList<String>();
			List<Double> seriNilai = new ArrayList<Double>();
			List<Double> seriJumlah = new ArrayList<Double>();
			for (int i = 0; i < tren.length(); i++) {
				JSONObject t = tren.getJSONObject(i);
				label.add(t.optString("label", ""));
				seriNilai.add(Double.valueOf(t.optDouble("nilai", 0)));
				seriJumlah.add(Double.valueOf(t.optDouble("jumlah", 0)));
			}
			sb.append(DashboardUiKit.dualLineChart(
					d.optString("trenJudul", "Tren per Bulan"),
					"Nilai dokumen dibaca pada sumbu kiri; jumlah dokumen sebagai pembanding.",
					label, seriNilai, "Nilai", "#2563eb", seriJumlah, "Jumlah Dokumen", "#94a3b8"));
		}

		// --- Komposisi status ----------------------------------------------------
		sb.append(donutDari(d, "komposisi", d.optString("komposisiJudul", "Komposisi Status"),
				"Sebaran dokumen menurut statusnya.", false));

		// --- Peringkat -----------------------------------------------------------
		JSONArray peringkat = d.optJSONArray("peringkat");
		if (peringkat != null && peringkat.length() > 0) {
			LinkedHashMap<String, Double> peta = new LinkedHashMap<String, Double>();
			for (int i = 0; i < peringkat.length(); i++) {
				JSONObject t = peringkat.getJSONObject(i);
				peta.put(t.optString("label", ""), Double.valueOf(t.optDouble("nilai", 0)));
			}
			sb.append(DashboardUiKit.barList(d.optString("peringkatJudul", "Peringkat"),
					"Diurutkan dari yang terbesar.", peta, "#0ea5e9", "", true,
					"Belum ada data pada periode ini."));
		}

		// --- Komposisi cara transfer (khusus Pembayaran Vendor) ------------------
		sb.append(donutDari(d, "caraBayar", "Komposisi Cara Transfer",
				"Nilai pembayaran menurut cara transfer yang dipakai.", true));

		sb.append(DashboardUiKit.closeGrid());

		// --- Tabel perlu perhatian ----------------------------------------------
		sb.append(tabelPerhatian(d));
		return sb.toString();
	}

	private String donutDari(JSONObject d, String kunci, String judul, String deskripsi,
			boolean uang) throws Exception {
		JSONArray arr = d.optJSONArray(kunci);
		if (arr == null || arr.length() == 0) {
			return "";
		}
		LinkedHashMap<String, Double> peta = new LinkedHashMap<String, Double>();
		for (int i = 0; i < arr.length(); i++) {
			JSONObject t = arr.getJSONObject(i);
			peta.put(t.optString("label", ""), Double.valueOf(t.optDouble("nilai", 0)));
		}
		return DashboardUiKit.donut(judul, deskripsi, peta, uang, "Belum ada data.");
	}

	/**
	 * Tabel "perlu perhatian" -- padanan Tabel Proses Pengajuan pada
	 * TraceStatusPengadaanAssetDashboard: dokumen yang paling lama tertahan pada
	 * tahap ini, lengkap dengan umur dan nilainya.
	 */
	private String tabelPerhatian(JSONObject d) throws Exception {
		JSONArray arr = d.optJSONArray("daftar");
		if (arr == null || arr.length() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin-top:10px;background:#ffffff;border:1px solid #e2e8f0;")
				.append("border-radius:14px;padding:14px;font-family:Arial,sans-serif;'>");
		sb.append("<div style='font-weight:700;font-size:13px;color:#0f172a;margin-bottom:8px;'>")
				.append(DashboardUiKit.esc(d.optString("daftarJudul", "Perlu Perhatian")))
				.append("</div>");
		sb.append("<table style='width:100%;border-collapse:collapse;font-size:12px;color:#334155;'>");
		for (int i = 0; i < arr.length(); i++) {
			JSONObject b = arr.getJSONObject(i);
			long umur = (long) b.optDouble("umurHari", 0);
			sb.append("<tr style='border-bottom:1px solid #f1f5f9;'>");
			sb.append("<td style='padding:6px 4px;'><span style='font-weight:600;'>")
					.append(DashboardUiKit.esc(b.optString("kode", "-"))).append("</span>");
			String ket = b.optString("keterangan", "");
			if (ket.length() > 0) {
				sb.append("<div style='font-size:10px;color:#94a3b8;'>")
						.append(DashboardUiKit.esc(ket)).append("</div>");
			}
			sb.append("</td>");
			sb.append("<td style='padding:6px 4px;text-align:right;color:#ea580c;'>")
					.append(umur > 0 ? (umur + " hari") : "").append("</td>");
			sb.append("<td style='padding:6px 4px;text-align:right;font-weight:600;'>")
					.append(DashboardUiKit.money(b.optDouble("nilai", 0))).append("</td>");
			sb.append("</tr>");
		}
		sb.append("</table></div>");
		return sb.toString();
	}
}
