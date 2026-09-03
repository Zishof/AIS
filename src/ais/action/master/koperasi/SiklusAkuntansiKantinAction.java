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

import ais.action.servlet.api.JurnalPenyesuaianHelper;
import ais.action.servlet.api.SaldoAwalAkunHelper;
import ais.action.servlet.api.TutupBukuHelper;
import ais.common.Common;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Layar ZK untuk siklus akuntansi: <b>Saldo Awal</b> (neraca awal), <b>Jurnal Penyesuaian
 * Berkala</b>, dan <b>Tutup Buku</b> (laba ditahan).
 *
 * <p><b>Satu kelas untuk tiga halaman</b>, jenisnya dibaca dari atribut {@code jenis} pada zul
 * ({@code saldo_awal} | {@code penyesuaian} | {@code tutup_buku}).</p>
 *
 * <p><b>Perhitungan tidak diduplikasi:</b> semuanya memanggil helper yang sama dengan yang dipakai
 * Desktop/Android lewat API ({@link SaldoAwalAkunHelper}, {@link JurnalPenyesuaianHelper},
 * {@link TutupBukuHelper}), sehingga angka di ZK dan di POS mustahil berbeda.</p>
 *
 * <p><b>Batas jujur:</b> layar ZK ini untuk MELIHAT draf dan MEMPOSTING. Pengisian angka saldo awal
 * dan penyusunan template penyesuaian dilakukan di layar Siklus Akuntansi versi Desktop/Android
 * (termasuk unggah Excel), supaya tidak ada dua tempat entri yang bisa berbeda perilaku.</p>
 */
public class SiklusAkuntansiKantinAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	/** Div penampung pada zul (id sama untuk ketiga halaman). */
	private Div siklusHost;

	private String jenis = "saldo_awal";
	private Datebox dpMulai;
	private Datebox dpSampai;
	private Div previewBox;
	private Label lblStatus;

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
		if (siklusHost == null) {
			return;
		}
		Object j = comp.getAttribute("jenis");
		if (j != null && j.toString().trim().length() > 0) {
			jenis = j.toString().trim();
		}

		DashboardUiKit.attachIntro(comp, judul(), penjelasan());

		Vlayout box = new Vlayout();
		box.setWidth("100%");
		box.setStyle("gap:8px;padding:6px;");
		box.setParent(siklusHost);

		Hlayout filter = new Hlayout();
		filter.setStyle("gap:8px;align-items:center;flex-wrap:wrap;");
		filter.setParent(box);

		if (!"saldo_awal".equals(jenis)) {
			filter.appendChild(new Label(Common.getBahasaConfig(
					"tutup_buku".equals(jenis) ? "Periode buku:" : "Periode:")));
			java.util.Calendar cal = java.util.Calendar.getInstance();
			Date sampai = cal.getTime();
			if ("tutup_buku".equals(jenis)) {
				cal.set(java.util.Calendar.MONTH, 0);
				cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
			} else {
				cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
			}
			Date mulai = cal.getTime();
			dpMulai = new Datebox(mulai);
			dpMulai.setFormat("dd-MM-yyyy");
			dpMulai.setWidth("130px");
			filter.appendChild(dpMulai);
			if ("tutup_buku".equals(jenis)) {
				filter.appendChild(new Label("s.d"));
				dpSampai = new Datebox(sampai);
				dpSampai.setFormat("dd-MM-yyyy");
				dpSampai.setWidth("130px");
				filter.appendChild(dpSampai);
			}
		}

		MyToolbarbuttonConfig btnDraf = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tampilkan Draf"));
		btnDraf.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				muatDraf();
			}
		});
		filter.appendChild(btnDraf);

		MyToolbarbuttonConfig btnPosting = new MyToolbarbuttonConfig(Common.getBahasaConfig(labelPosting()));
		btnPosting.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				posting();
			}
		});
		filter.appendChild(btnPosting);

		lblStatus = new Label("");
		lblStatus.setStyle("font-size:12px;color:#475569;");
		box.appendChild(lblStatus);

		previewBox = new Div();
		previewBox.setWidth("100%");
		previewBox.setParent(box);

		muatDraf();
	}

	private String judul() {
		if ("penyesuaian".equals(jenis)) {
			return "Jurnal Penyesuaian Berkala";
		}
		if ("tutup_buku".equals(jenis)) {
			return "Tutup Buku (Laba Ditahan)";
		}
		return "Saldo Awal (Neraca Awal)";
	}

	private String penjelasan() {
		if ("penyesuaian".equals(jenis)) {
			return "Amortisasi biaya dibayar di muka, akrual beban, dan penyisihan piutang dari template. "
					+ "Satu template hanya dapat diposting sekali untuk periode yang sama. "
					+ "Penyusunan template dilakukan di layar Siklus Akuntansi versi Desktop/Android.";
		}
		if ("tutup_buku".equals(jenis)) {
			return "Menutup seluruh akun pendapatan & beban pada periode terpilih dan memindahkan laba/rugi "
					+ "bersihnya ke akun Laba Ditahan (konfigurasi akun_laba_ditahan). Satu periode hanya bisa "
					+ "ditutup sekali.";
		}
		return "Angka pembukaan tiap akun beserta jurnal pembukaannya. Tanpa ini Neraca, Buku Besar, dan "
				+ "Neraca Saldo selalu berangkat dari nol. Selisih debet-kredit ditempatkan pada akun "
				+ "Modal/Ekuitas Awal (konfigurasi akun_modal_awal). Pengisian angka dilakukan di layar "
				+ "Siklus Akuntansi versi Desktop/Android (termasuk unggah Excel).";
	}

	private String labelPosting() {
		if ("penyesuaian".equals(jenis)) {
			return "Posting Semua yang Siap";
		}
		if ("tutup_buku".equals(jenis)) {
			return "Tutup Buku Sekarang";
		}
		return "Posting Jurnal Pembukaan";
	}

	private String tanggal(Datebox d) {
		if (d == null || d.getValue() == null) {
			return "";
		}
		return new java.text.SimpleDateFormat("yyyy-MM-dd").format(d.getValue());
	}

	private String periodeBulan() {
		if (dpMulai == null || dpMulai.getValue() == null) {
			return new java.text.SimpleDateFormat("yyyy-MM").format(ais.ui.util.WaktuUtil.getDate());
		}
		return new java.text.SimpleDateFormat("yyyy-MM").format(dpMulai.getValue());
	}

	/** Panggil helper yang sama dengan API; {@code terapkan=false} hanya menghitung. */
	private JSONObject panggil(boolean terapkan) {
		JSONObject payload = new JSONObject();
		JSONObject hasil = new JSONObject();
		try {
			if ("penyesuaian".equals(jenis)) {
				payload.put("periode", periodeBulan());
				JurnalPenyesuaianHelper.proses(terapkan ? "penyesuaian_posting" : "penyesuaian_draft",
						Common.getCurrentUser(), payload, hasil);
			} else if ("tutup_buku".equals(jenis)) {
				payload.put("mulai", tanggal(dpMulai));
				payload.put("sampai", tanggal(dpSampai));
				TutupBukuHelper.proses(terapkan ? "tutup_buku_posting" : "tutup_buku_draft",
						Common.getCurrentUser(), payload, hasil);
			} else {
				SaldoAwalAkunHelper.proses(terapkan ? "saldo_awal_posting" : "saldo_awal_draft",
						Common.getCurrentUser(), payload, hasil);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit SiklusAkuntansiKantinAction.panggil " + jenis);
			try {
				hasil.put("status", "99");
				hasil.put("message", "Gagal memproses: " + e.getMessage());
			} catch (Exception ignore) {
				// pesan galat gagal disusun; status default sudah memadai.
			}
		}
		return hasil;
	}

	private void muatDraf() {
		JSONObject hasil = panggil(false);
		if (lblStatus != null) {
			lblStatus.setValue(hasil.optString("message", ""));
		}
		gambarGrid(hasil);
	}

	private void posting() {
		JSONObject hasil = panggil(true);
		try {
			String pesan = hasil.optString("message", "");
			JSONArray masalah = hasil.optJSONArray("masalah");
			if (masalah != null && masalah.length() > 0) {
				pesan = pesan + " " + masalah.optString(0);
			}
			ais.ui.util.MyMessageboxConfig.show(pesan, judul(), org.zkoss.zul.Messagebox.OK,
					org.zkoss.zul.Messagebox.INFORMATION);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SiklusAkuntansiKantinAction.posting");
		}
		muatDraf();
	}

	private void gambarGrid(JSONObject hasil) {
		if (previewBox == null) {
			return;
		}
		previewBox.getChildren().clear();
		JSONArray arr = hasil.optJSONArray("rincian");
		if (arr == null || arr.length() == 0) {
			previewBox.appendChild(DashboardUiKit.html(
					"<div style='padding:10px;color:#64748b;'>Tidak ada baris untuk ditampilkan.</div>"));
			return;
		}
		String[] judulKolom;
		String[] lebarKolom;
		if ("penyesuaian".equals(jenis)) {
			judulKolom = new String[] { "Nama", "Debet", "Kredit", "Nilai", "Status" };
			lebarKolom = new String[] { "26%", "20%", "20%", "14%", "20%" };
		} else if ("tutup_buku".equals(jenis)) {
			judulKolom = new String[] { "Kode", "Nama Akun", "Sisi Penutup", "Nilai" };
			lebarKolom = new String[] { "12%", "38%", "32%", "18%" };
		} else {
			judulKolom = new String[] { "Kode", "Nama Akun", "Debet", "Kredit" };
			lebarKolom = new String[] { "14%", "46%", "20%", "20%" };
		}
		org.zkoss.zul.Grid grid = new org.zkoss.zul.Grid();
		grid.setWidth("100%");
		grid.setSpan("true");
		org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
		for (int i = 0; i < judulKolom.length; i++) {
			org.zkoss.zul.Column c = new org.zkoss.zul.Column(Common.getBahasaConfig(judulKolom[i]));
			c.setWidth(lebarKolom[i]);
			cols.appendChild(c);
		}
		grid.appendChild(cols);
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		grid.appendChild(rows);

		List<String> ringkas = new ArrayList<String>();
		for (int i = 0; i < arr.length(); i++) {
			JSONObject b = arr.optJSONObject(i);
			if (b == null) {
				continue;
			}
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setValign("top");
			if ("penyesuaian".equals(jenis)) {
				boolean siap = b.optBoolean("siap", false);
				if (!siap) {
					row.setStyle("background:#fff7ed;");
				}
				row.appendChild(new Label(b.optString("nama", "-")));
				row.appendChild(new Label(b.optString("debet", "-")));
				row.appendChild(new Label(b.optString("kredit", "-")));
				row.appendChild(new Label(DashboardUiKit.money(b.optDouble("nilai", 0))));
				row.appendChild(DashboardUiKit.html(siap
						? "<span style='color:#15803d;font-weight:600;'>Siap diposting</span>"
						: "<span style='color:#b45309;'>" + DashboardUiKit.esc(b.optString("alasan", "")) + "</span>"));
			} else if ("tutup_buku".equals(jenis)) {
				row.appendChild(new Label(b.optString("kodeAkun", "-")));
				row.appendChild(new Label(b.optString("namaAkun", "-")));
				row.appendChild(new Label(b.optString("sisi", "-")));
				row.appendChild(new Label(DashboardUiKit.money(b.optDouble("nilai", 0))));
			} else {
				row.appendChild(new Label(b.optString("kodeAkun", "-")));
				row.appendChild(new Label(b.optString("namaAkun", "-")));
				row.appendChild(new Label(DashboardUiKit.money(b.optDouble("debet", 0))));
				row.appendChild(new Label(DashboardUiKit.money(b.optDouble("kredit", 0))));
			}
			rows.appendChild(row);
		}
		previewBox.appendChild(grid);

		if ("saldo_awal".equals(jenis)) {
			ringkas.add("Total debet " + DashboardUiKit.money(hasil.optDouble("totalDebet", 0))
					+ " | total kredit " + DashboardUiKit.money(hasil.optDouble("totalKredit", 0)));
			if (Math.abs(hasil.optDouble("selisihKeModal", 0)) >= 0.005) {
				ringkas.add("Selisih " + DashboardUiKit.money(hasil.optDouble("selisihKeModal", 0))
						+ " ditempatkan pada " + hasil.optString("akunModal", "-"));
			}
		} else if ("tutup_buku".equals(jenis)) {
			ringkas.add("Pendapatan " + DashboardUiKit.money(hasil.optDouble("totalPendapatan", 0))
					+ " | beban " + DashboardUiKit.money(hasil.optDouble("totalBeban", 0))
					+ " | laba bersih " + DashboardUiKit.money(hasil.optDouble("labaBersih", 0))
					+ " -> " + hasil.optString("akunLabaDitahan", "-"));
		}
		if (!hasil.optBoolean("siap", true) && hasil.optString("alasan", "").length() > 0) {
			ringkas.add("Belum bisa diposting: " + hasil.optString("alasan"));
		}
		for (int i = 0; i < ringkas.size(); i++) {
			previewBox.appendChild(DashboardUiKit.html("<div style='padding:4px 6px;font-size:12px;'>"
					+ DashboardUiKit.esc(ringkas.get(i)) + "</div>"));
		}
	}
}
