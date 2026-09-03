package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Vlayout;

import ais.ui.util.DashboardUiKit;
import ais.action.servlet.api.PostingKantinLanjutanHelper;
import ais.common.Common;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Layar ZK untuk empat jurnal penutup rantai <b>pengadaan &rarr; pembayaran</b> toko:
 * kulakan, pembayaran hutang supplier, penerimaan piutang customer, dan penyesuaian
 * persediaan (retur beli/jual, selisih opname, mutasi antar outlet).
 *
 * <p><b>Satu kelas untuk empat halaman.</b> Jenis posting dibaca dari atribut halaman
 * {@code jenis} ({@code kulakan} | {@code bayar_hutang} | {@code terima_piutang} |
 * {@code penyesuaian}) lewat {@code <custom-attributes>} pada zul-nya, sehingga tidak perlu
 * empat kelas yang isinya nyaris sama.</p>
 *
 * <p><b>Perhitungannya tidak diduplikasi.</b> Draf maupun penulisan jurnal dikerjakan
 * {@link PostingKantinLanjutanHelper} &mdash; mesin yang sama persis dengan yang dipakai
 * Desktop/Android lewat API. Layar ini hanya menyajikan hasilnya, jadi angka di ZK dan di POS
 * mustahil berbeda.</p>
 *
 * <p>Pola tampilan menyusul Posting HPP/Penjualan: draf per dokumen ditampilkan lebih dulu
 * lengkap dengan akun debet/kreditnya, dokumen yang belum siap tetap tampil beserta alasannya
 * (baris ditandai warna) dan tidak menghalangi yang lain, serta posting dapat dilakukan per
 * baris atau sekaligus untuk yang sudah siap.</p>
 */
public class PostingTokoKantinAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	/** Div penampung pada zul (id sama untuk keempat halaman). */
	private Div postingTokoHost;

	private String jenis = "kulakan";
	private Datebox dpMulai;
	private Datebox dpSampai;
	private Div previewBox;
	private Label lblStatus;

	private final List<JSONObject> draf = new ArrayList<JSONObject>();

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (postingTokoHost == null) {
			return;
		}
		Object j = comp.getAttribute("jenis");
		if (j == null && comp.getPage() != null) {
			j = comp.getPage().getAttribute("jenis");
		}
		if (j != null && j.toString().trim().length() > 0) {
			jenis = j.toString().trim();
		}

		DashboardUiKit.attachIntro(comp, judul(), penjelasan());

		Vlayout box = new Vlayout();
		box.setWidth("100%");
		box.setStyle("gap:8px;padding:6px;");
		box.setParent(postingTokoHost);

		Hlayout filter = new Hlayout();
		filter.setStyle("gap:8px;align-items:center;flex-wrap:wrap;");
		filter.setParent(box);
		filter.appendChild(new Label(Common.getBahasaConfig("Periode:")));

		java.util.Calendar cal = java.util.Calendar.getInstance();
		Date sampai = cal.getTime();
		cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
		Date mulai = cal.getTime();

		dpMulai = new Datebox(mulai);
		dpMulai.setFormat("dd-MM-yyyy");
		dpMulai.setWidth("130px");
		filter.appendChild(dpMulai);
		filter.appendChild(new Label("s.d"));
		dpSampai = new Datebox(sampai);
		dpSampai.setFormat("dd-MM-yyyy");
		dpSampai.setWidth("130px");
		filter.appendChild(dpSampai);

		MyToolbarbuttonConfig btnDraf = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tampilkan Draf"));
		btnDraf.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				muatDraf();
			}
		});
		filter.appendChild(btnDraf);

		PostingAkunCrudNavigator.tambahkan(filter, jenis);

		MyToolbarbuttonConfig btnSemua = new MyToolbarbuttonConfig(
				Common.getBahasaConfig("Posting Semua yang Siap"));
		btnSemua.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				posting(null);
			}
		});
		filter.appendChild(btnSemua);

		lblStatus = new Label("");
		lblStatus.setStyle("font-size:12px;color:#475569;");
		box.appendChild(lblStatus);

		previewBox = new Div();
		previewBox.setWidth("100%");
		previewBox.setParent(box);

		muatDraf();
	}

	private String judul() {
		if ("bayar_hutang".equals(jenis)) {
			return "Posting Pembayaran Hutang Supplier";
		}
		if ("terima_piutang".equals(jenis)) {
			return "Posting Penerimaan Piutang Customer";
		}
		if ("penyesuaian".equals(jenis)) {
			return "Posting Penyesuaian Persediaan";
		}
		return "Posting Kulakan (Pembelian Persediaan)";
	}

	private String penjelasan() {
		if ("bayar_hutang".equals(jenis)) {
			return "Mencatat pembayaran ke pemasok toko: debet Utang Supplier, kredit Kas/Bank. "
					+ "Akun utang diambil dari master Penyedia (cadangan: konfigurasi akun_utang_supplier_toko).";
		}
		if ("terima_piutang".equals(jenis)) {
			return "Mencatat pelunasan piutang pelanggan: debet Kas/Bank, kredit Piutang Usaha.";
		}
		if ("penyesuaian".equals(jenis)) {
			return "Retur pembelian, retur penjualan, selisih stok opname, dan mutasi antar outlet. "
					+ "Mutasi hanya dijurnal bila akun persediaan kedua outlet berbeda.";
		}
		return "Mencatat pembelian persediaan: debet Persediaan per barang, kredit Utang Supplier "
				+ "(bagian kredit) dan Kas (bagian dibayar di muka/tunai). Tanpa jurnal ini, akun "
				+ "Persediaan hanya pernah dikredit oleh jurnal HPP sehingga saldonya minus.";
	}

	private String tanggal(Datebox d) {
		if (d == null || d.getValue() == null) {
			return "";
		}
		return new java.text.SimpleDateFormat("yyyy-MM-dd").format(d.getValue());
	}

	/** Panggil mesin bersama; {@code terapkan=false} hanya menghitung. */
	private JSONObject panggil(boolean terapkan, List<Long> ids) {
		JSONObject payload = new JSONObject();
		JSONObject hasil = new JSONObject();
		try {
			payload.put("mulai", tanggal(dpMulai));
			payload.put("sampai", tanggal(dpSampai));
			if (ids != null && !ids.isEmpty()) {
				payload.put("posting_ids", new JSONArray(ids));
			}
			PostingKantinLanjutanHelper.proses("posting_" + jenis + (terapkan ? "_terapkan" : "_draft"),
					Common.getCurrentUser(), payload, hasil);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PostingTokoKantinAction.panggil " + jenis);
			try {
				hasil.put("status", "99");
				hasil.put("message", "Gagal memproses: " + e.getMessage());
			} catch (Exception ignore) {
				// pesan galat gagal disusun; status default sudah cukup utk pemakai.
			}
		}
		return hasil;
	}

	private void muatDraf() {
		JSONObject hasil = panggil(false, null);
		draf.clear();
		JSONArray arr = hasil.optJSONArray("rincian");
		for (int i = 0; arr != null && i < arr.length(); i++) {
			JSONObject b = arr.optJSONObject(i);
			if (b != null) {
				draf.add(b);
			}
		}
		if (lblStatus != null) {
			lblStatus.setValue(hasil.optString("message", ""));
		}
		gambarGrid();
	}

	private void posting(Long idSatu) {
		List<Long> ids = new ArrayList<Long>();
		if (idSatu != null) {
			ids.add(idSatu);
		}
		JSONObject hasil = panggil(true, ids);
		String pesan = hasil.optString("message", "");
		JSONArray masalah = hasil.optJSONArray("masalah");
		if (masalah != null && masalah.length() > 0) {
			pesan = pesan + " " + masalah.optString(0);
		}
		try {
			org.zkoss.zul.Messagebox.show(pesan, judul(), org.zkoss.zul.Messagebox.OK,
					org.zkoss.zul.Messagebox.INFORMATION);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PostingTokoKantinAction.posting pesan");
		}
		muatDraf();
	}

	private void gambarGrid() {
		if (previewBox == null) {
			return;
		}
		previewBox.getChildren().clear();
		if (draf.isEmpty()) {
			previewBox.appendChild(DashboardUiKit.html(
					"<div style='padding:10px;color:#64748b;'>Tidak ada dokumen yang belum diposting pada periode ini.</div>"));
			return;
		}
		org.zkoss.zul.Grid grid = new org.zkoss.zul.Grid();
		grid.setWidth("100%");
		grid.setSpan("true");
		org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
		String[] judulKolom = new String[] { "Tanggal", "Referensi", "Nilai", "Draf Jurnal", "Status", "Aksi" };
		String[] lebarKolom = new String[] { "10%", "18%", "12%", "34%", "16%", "10%" };
		for (int i = 0; i < judulKolom.length; i++) {
			org.zkoss.zul.Column c = new org.zkoss.zul.Column(Common.getBahasaConfig(judulKolom[i]));
			c.setWidth(lebarKolom[i]);
			cols.appendChild(c);
		}
		grid.appendChild(cols);
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		grid.appendChild(rows);

		double totalSiap = 0;
		int jumlahSiap = 0;
		for (int i = 0; i < draf.size(); i++) {
			final JSONObject baris = draf.get(i);
			final boolean siap = baris.optBoolean("siap", false);
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setValign("top");
			if (!siap) {
				row.setStyle("background:#fff7ed;");
			} else {
				jumlahSiap++;
				totalSiap += baris.optDouble("nilai", 0);
			}
			row.appendChild(new Label(baris.optString("tanggal", "-")));
			row.appendChild(new Label(baris.optString("referensi", "-")));
			row.appendChild(new Label("Rp " + DashboardUiKit.money(baris.optDouble("nilai", 0))));
			StringBuilder j = new StringBuilder("<div style='font-size:11px;line-height:1.5;'>");
			j.append("<div><b>D:</b> ").append(DashboardUiKit.esc(baris.optString("debet", "-"))).append("</div>");
			j.append("<div><b>K:</b> ").append(DashboardUiKit.esc(baris.optString("kredit", "-"))).append("</div>");
			j.append("<div style='color:#64748b;'>").append(DashboardUiKit.esc(baris.optString("keterangan", "")))
					.append("</div></div>");
			row.appendChild(DashboardUiKit.html(j.toString()));
			row.appendChild(DashboardUiKit.html(siap
					? "<span style='color:#15803d;font-weight:600;'>Siap diposting</span>"
					: "<span style='color:#b45309;'>" + DashboardUiKit.esc(baris.optString("alasan", "")) + "</span>"));
			if (siap) {
				MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig(Common.getBahasaConfig("Posting"));
				btn.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						posting(Long.valueOf(baris.optLong("id")));
					}
				});
				row.appendChild(btn);
			} else {
				row.appendChild(PostingAkunCrudNavigator.panel(jenis, baris.optString("alasan", "")));
			}
			rows.appendChild(row);
		}
		previewBox.appendChild(grid);
		previewBox.appendChild(DashboardUiKit.html("<div style='padding:6px;font-size:12px;'>"
				+ jumlahSiap + " dokumen siap, total Rp " + DashboardUiKit.money(totalSiap) + "</div>"));
	}
}
