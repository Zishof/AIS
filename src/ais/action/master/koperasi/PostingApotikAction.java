package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vlayout;

import ais.action.servlet.api.ApotikPbfPostingHelper;
import ais.action.servlet.api.ApotikPostingHelper;
import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Halaman ZK posting jurnal khusus Apotik.
 *
 * <p>Satu composer melayani Penjualan, HPP, Penerimaan PBF, dan Pembayaran
 * Utang PBF. Perhitungan dan penulisan jurnal didelegasikan langsung ke helper
 * yang sama dengan API Flutter, sehingga ZK tidak mempunyai rumus bisnis
 * kedua. Posting massal selalu mengirim ID eksplisit per 10 dokumen.</p>
 */
public class PostingApotikAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;
	private static final int UKURAN_BATCH = 10;

	private Div postingApotikHost;
	private String jenis = "penjualan";
	private Datebox dpMulai;
	private Datebox dpSampai;
	private Div previewBox;
	private Label lblStatus;
	private org.zkoss.zul.Combobox cbStatusPosting;
	private boolean bolehMenerapkan;

	private final List<JSONObject> draf = new ArrayList<JSONObject>();
	private JSONArray riwayat = new JSONArray();

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (postingApotikHost == null) return;
		Object atribut = comp.getAttribute("jenis");
		if (atribut == null && comp.getPage() != null) atribut = comp.getPage().getAttribute("jenis");
		if (atribut != null && atribut.toString().trim().length() > 0) jenis = atribut.toString().trim();
		bolehMenerapkan = bolehAksi("create");

		DashboardUiKit.attachIntro(comp, judul(), penjelasan());
		Vlayout box = new Vlayout();
		box.setWidth("100%");
		box.setStyle("gap:8px;padding:6px;");
		box.setParent(postingApotikHost);

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
		filter.appendChild(new Label(Common.getBahasaConfig("Status:")));
		cbStatusPosting = PostingStatusZkUtil.buatFilter(new EventListener() {
			@Override public void onEvent(Event event) throws Exception { gambarGrid(); }
		});
		filter.appendChild(cbStatusPosting);

		MyToolbarbuttonConfig btnDraf = new MyToolbarbuttonConfig(Common.getBahasaConfig("Tampilkan Draf"));
		btnDraf.addEventListener("onClick", new EventListener() {
			@Override public void onEvent(Event event) throws Exception { muatDraf(); }
		});
		filter.appendChild(btnDraf);

		MyToolbarbuttonConfig btnPemetaan = new MyToolbarbuttonConfig(Common.getBahasaConfig("Pemetaan Akun Apotik"));
		btnPemetaan.addEventListener("onClick", new EventListener() {
			@Override public void onEvent(Event event) throws Exception {
				Executions.getCurrent().sendRedirect(Common.ROOT + "/pages/master/sirs/apotik_akun_mapping.zul", "_blank");
			}
		});
		filter.appendChild(btnPemetaan);

		if (bolehMenerapkan) {
			MyToolbarbuttonConfig btnSemua = new MyToolbarbuttonConfig(Common.getBahasaConfig("Posting Semua yang Siap"));
			btnSemua.addEventListener("onClick", new EventListener() {
				@Override public void onEvent(Event event) throws Exception { posting(null); }
			});
			filter.appendChild(btnSemua);
		}

		lblStatus = new Label("");
		lblStatus.setStyle("font-size:12px;color:#475569;");
		box.appendChild(lblStatus);
		previewBox = new Div();
		previewBox.setWidth("100%");
		previewBox.setParent(box);
		muatDraf();
	}

	private boolean bolehAksi(String aksi) {
		Tbmuser pengguna = Common.getCurrentUser();
		if (Common.getApakahAdminLain(pengguna)) return true;
		Tbmrole role = pengguna == null ? null : pengguna.hakAkses();
		return role == null || EbisnisMenuKatalog.bolehAksiAkuntansi(
				role.getEbisnisMenu(), role.getRoleId(), kunciHak(), aksi);
	}

	private String kunciHak() {
		if ("hpp".equals(jenis)) return "posting_hpp";
		if ("pbf".equals(jenis)) return "posting_kulakan";
		if ("bayar_hutang_pbf".equals(jenis)) return "posting_bayar_hutang";
		return "posting_penjualan";
	}

	private String judul() {
		if ("hpp".equals(jenis)) return "Posting HPP Penjualan Apotik";
		if ("pbf".equals(jenis)) return "Posting Penerimaan PBF Apotik";
		if ("bayar_hutang_pbf".equals(jenis)) return "Posting Pembayaran Utang PBF Apotik";
		return "Posting Penjualan Apotik";
	}

	private String penjelasan() {
		if ("hpp".equals(jenis)) return "Mencatat HPP ke sisi debet dan Persediaan Apotik ke sisi kredit per transaksi.";
		if ("pbf".equals(jenis)) return "Mencatat penerimaan obat dari PBF: debet Persediaan, kredit Utang PBF.";
		if ("bayar_hutang_pbf".equals(jenis)) return "Mencatat pembayaran vendor: debet Utang PBF, kredit Kas/Bank.";
		return "Mencatat Kas/Bank/Piutang ke sisi debet dan Pendapatan Penjualan Apotik ke sisi kredit.";
	}

	private String namaAksi(boolean terapkan) {
		String akhir = terapkan ? "_terapkan" : "_draft";
		if ("pbf".equals(jenis)) return "apotik_posting_pbf" + akhir;
		if ("bayar_hutang_pbf".equals(jenis)) return "apotik_posting_bayar_hutang_pbf" + akhir;
		return "apotik_posting_" + jenis + akhir;
	}

	private String tanggal(Datebox datebox) {
		if (datebox == null || datebox.getValue() == null) return "";
		return new java.text.SimpleDateFormat("yyyy-MM-dd").format(datebox.getValue());
	}

	private JSONObject panggil(boolean terapkan, List<Long> ids) {
		JSONObject payload = new JSONObject();
		JSONObject hasil = new JSONObject();
		try {
			payload.put("mulai", tanggal(dpMulai));
			payload.put("sampai", tanggal(dpSampai));
			payload.put("batasRiwayat", terapkan ? 100 : 1000);
			if (ids != null && !ids.isEmpty()) payload.put("posting_ids", new JSONArray(ids));
			if ("pbf".equals(jenis) || "bayar_hutang_pbf".equals(jenis)) {
				ApotikPbfPostingHelper.proses(namaAksi(terapkan), Common.getCurrentUser(), payload, hasil);
			} else {
				ApotikPostingHelper.proses(namaAksi(terapkan), Common.getCurrentUser(), payload, hasil);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingApotikAction.panggil " + jenis);
			try { hasil.put("status", "99"); hasil.put("message", "Gagal memproses: " + e.getMessage()); }
			catch (Exception ignore) { }
		}
		return hasil;
	}

	private void muatDraf() {
		JSONObject hasil = panggil(false, null);
		draf.clear();
		riwayat = hasil.optJSONArray("rincianSudahDiposting");
		if (riwayat == null) riwayat = new JSONArray();
		JSONArray arr = hasil.optJSONArray("rincian");
		for (int i = 0; arr != null && i < arr.length(); i++) {
			JSONObject baris = arr.optJSONObject(i);
			if (baris != null) draf.add(baris);
		}
		if (lblStatus != null) lblStatus.setValue(hasil.optString("message", ""));
		gambarGrid();
	}

	private void posting(Long idSatu) {
		List<Long> ids = new ArrayList<Long>();
		if (idSatu != null) {
			ids.add(idSatu);
		} else {
			for (JSONObject baris : draf) {
				if (baris.optBoolean("siap", false) && baris.optLong("id", 0) > 0)
					ids.add(Long.valueOf(baris.optLong("id")));
			}
		}
		if (ids.isEmpty()) {
			tampilkanPesan("Tidak ada dokumen berstatus SIAP untuk diposting.");
			return;
		}
		int sukses = 0;
		JSONArray masalah = new JSONArray();
		for (int awal = 0; awal < ids.size(); awal += UKURAN_BATCH) {
			int akhir = Math.min(awal + UKURAN_BATCH, ids.size());
			JSONObject hasil = panggil(true, new ArrayList<Long>(ids.subList(awal, akhir)));
			if (!"00".equals(hasil.optString("status"))) {
				masalah.put(hasil.optString("message", hasil.optString("description", "Posting ditolak.")));
				break;
			}
			sukses += hasil.optInt("diposting", 0);
			JSONArray gagal = hasil.optJSONArray("masalah");
			for (int i = 0; gagal != null && i < gagal.length(); i++) masalah.put(gagal.opt(i));
		}
		String pesan = sukses + " jurnal Apotik terbentuk";
		if (masalah.length() > 0) pesan += ", " + masalah.length() + " gagal: " + masalah.optString(0);
		else pesan += ".";
		tampilkanPesan(pesan);
		muatDraf();
	}

	private void tampilkanPesan(String pesan) {
		try {
			ais.ui.util.MyMessageboxConfig.show(pesan, judul(), org.zkoss.zul.Messagebox.OK,
					org.zkoss.zul.Messagebox.INFORMATION);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingApotikAction.tampilkanPesan");
		}
	}

	private void gambarGrid() {
		if (previewBox == null) return;
		previewBox.getChildren().clear();
		List<JSONObject> tampil = PostingStatusZkUtil.gabungkan(draf, riwayat,
				PostingStatusZkUtil.nilai(cbStatusPosting));
		if (tampil.isEmpty()) {
			previewBox.appendChild(DashboardUiKit.html(
					"<div style='padding:10px;color:#64748b;'>Tidak ada dokumen untuk filter status yang dipilih.</div>"));
			return;
		}
		Grid grid = new Grid();
		grid.setWidth("100%");
		grid.setSpan("true");
		grid.setMold("paging");
		grid.setPageSize(50);
		org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
		String[] judulKolom = { "Tanggal", "Referensi", "Nilai", "Akun Debet / Kredit", "Status", "Aksi" };
		String[] lebarKolom = { "10%", "18%", "12%", "31%", "19%", "10%" };
		for (int i = 0; i < judulKolom.length; i++) {
			org.zkoss.zul.Column c = new org.zkoss.zul.Column(Common.getBahasaConfig(judulKolom[i]));
			c.setWidth(lebarKolom[i]); cols.appendChild(c);
		}
		grid.appendChild(cols);
		Rows rows = new Rows();
		grid.appendChild(rows);
		double totalSiap = 0;
		int jumlahSiap = 0;
		for (int i = 0; i < tampil.size(); i++) {
			final JSONObject baris = tampil.get(i);
			final boolean sudah = baris.optBoolean("sudahDiposting", false);
			final boolean siap = !sudah && baris.optBoolean("siap", false);
			Row row = new Row();
			row.setValign("top");
			if (sudah) row.setStyle("background:#f0fdf4;");
			else if (!siap) row.setStyle("background:#fff7ed;");
			else { jumlahSiap++; totalSiap += baris.optDouble("nilai", 0); }
			row.appendChild(new Label(baris.optString("tanggal", "-")));
			row.appendChild(new Label(baris.optString("referensi", "-")));
			row.appendChild(new Label("Rp " + DashboardUiKit.money(baris.optDouble("nilai", 0))));
			String akunDebet = baris.optString("akunDebit", baris.optString("debet", "-"));
			String akunKredit = baris.optString("akunKredit", baris.optString("kredit", "-"));
			row.appendChild(DashboardUiKit.html("<div style='font-size:11px;line-height:1.5;'>"
					+ "<div><b>D:</b> " + DashboardUiKit.esc(akunDebet) + "</div>"
					+ "<div><b>K:</b> " + DashboardUiKit.esc(akunKredit) + "</div></div>"));
			String status = baris.optString("statusLabel", siap ? "Belum Diposting - Siap" : "Belum Diposting - Tertahan");
			String detailStatus = sudah
					? status + "<br/><span style='font-size:11px;'>Jurnal "
							+ DashboardUiKit.esc(baris.optString("nomorJurnal", "-")) + " · "
							+ DashboardUiKit.esc(baris.optString("tanggalPosting", "-")) + "</span>"
					: siap ? status : status + ": " + DashboardUiKit.esc(baris.optString("alasan", ""));
			row.appendChild(DashboardUiKit.html("<span style='color:"
					+ (sudah || siap ? "#15803d" : "#b45309") + ";font-weight:600;'>" + detailStatus + "</span>"));
			if (sudah) {
				row.appendChild(new Label(Common.getBahasaConfig("Tercatat di buku besar")));
			} else if (siap && bolehMenerapkan) {
				MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig(Common.getBahasaConfig("Posting"));
				btn.addEventListener("onClick", new EventListener() {
					@Override public void onEvent(Event event) throws Exception { posting(Long.valueOf(baris.optLong("id"))); }
				});
				row.appendChild(btn);
			} else if (!siap) {
				MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig(Common.getBahasaConfig("Perbaiki Pemetaan"));
				btn.addEventListener("onClick", new EventListener() {
					@Override public void onEvent(Event event) throws Exception {
						Executions.getCurrent().sendRedirect(Common.ROOT + "/pages/master/sirs/apotik_akun_mapping.zul", "_blank");
					}
				});
				row.appendChild(btn);
			} else {
				row.appendChild(new Label(Common.getBahasaConfig("Hanya lihat")));
			}
			rows.appendChild(row);
		}
		previewBox.appendChild(grid);
		previewBox.appendChild(DashboardUiKit.html("<div style='padding:6px;font-size:12px;'>"
				+ jumlahSiap + " dokumen siap, total Rp " + DashboardUiKit.money(totalSiap)
				+ " · " + riwayat.length() + " histori dapat ditelusuri.</div>"));
	}
}
