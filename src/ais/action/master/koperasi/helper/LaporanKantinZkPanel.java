package ais.action.master.koperasi.helper;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.action.servlet.LaporanKantinPdf;
import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>Katalog "Laporan-Laporan e-Kantin" -- versi ZK NATIF.</h2>
 *
 * <p><b>Kenapa kelas ini ada.</b> {@code DashboardKantinAction} sebelumnya menautkan tab
 * "Laporan-Laporan" ke {@code laporan_laporan.jsp} lewat {@code <iframe>} (JSP dimuat apa adanya).
 * Per permintaan eksplisit (2026-07-26): TIDAK boleh ada lagi iframe yang mengarah ke halaman JSP di
 * mana pun di Dasbor Kantin. Kelas ini membangun ULANG antarmuka katalog+jalankan+unduh laporan itu
 * sepenuhnya di ZK, TANPA menduplikasi logika query SATU PUN -- seluruh ~157 laporan tetap dihitung
 * oleh mesin yang SAMA PERSIS dengan versi JSP/Desktop: {@link LaporanKatalogData#katalog()} (daftar
 * kategori/laporan) dan {@link LaporanKantinUtil#build(HttpServletRequest)} (eksekusi query generik,
 * mengembalikan bentuk tabel {@code kolom+baris} yang SAMA dipakai ketiga penyaji). Karena bentuknya
 * generik, SATU renderer grid di sini otomatis menangani seluruh 157 laporan tanpa perlu kode
 * per-laporan.</p>
 *
 * <p><b>Filter → eksekusi.</b> {@link LaporanKantinUtil#build} membaca filter lewat
 * {@code HttpServletRequest.getParameter(...)} (kontrak lama, dipakai bersama JSP yang submit form
 * biasa) -- di sini native request ZK ({@link ExecutionsCtrl#getCurrent()}) dibungkus
 * {@link ParamRequestWrapper} yang menjawab {@code getParameter} dari nilai form ZK saat tombol
 * "Jalankan"/"Unduh" diklik, pola SAMA PERSIS dengan adaptor {@code PosApi.ParamRequestWrapper}
 * (Desktop) -- bedanya di sini TIDAK perlu trik sesi/token krn ZK sudah berjalan dalam
 * {@code HttpSession} cookie yang sama dgn login web, jadi {@code Common.getCurrentUser(request)} di
 * dalam {@code LaporanKantinUtil.build} otomatis mengenali pengguna yang sedang login tanpa adaptor
 * tambahan.</p>
 *
 * <p><b>Unduh.</b> PDF memakai ULANG {@link LaporanKantinPdf#generate} (method itu diubah dari
 * package-private ke {@code public} khusus supaya bisa dipanggil dari package ini -- TIDAK ada
 * perubahan perilaku). Excel memakai CSV ber-BOM UTF-8 (persis strategi "(Excel-friendly)" JSP-nya,
 * lihat {@code laporan_laporan.jsp} fungsi {@code dataToCsv}) -- dibangun dari {@code H.kolom}/
 * {@code H.baris} yang SAMA, bukan query terpisah.</p>
 */
public final class LaporanKantinZkPanel {

	private LaporanKantinZkPanel() {
	}

	private static final ThreadLocal<NumberFormat> NUM = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			return NumberFormat.getNumberInstance(new Locale("id", "ID"));
		}
	};
	private static final ThreadLocal<SimpleDateFormat> TGL_TAMPIL = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("dd-MM-yyyy");
		}
	};
	private static final ThreadLocal<SimpleDateFormat> TGL_PARAM = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd");
		}
	};

	/** Titik masuk -- dipanggil {@code DashboardKantinAction.buildLaporanLaporan}. */
	public static void build(Div panel) {
		build(panel, null);
	}

	/** Varian dengan katalog kustom (mis. {@link LaporanKatalogData#katalogKeuangan()} untuk menu Laporan Keuangan). */
	public static void build(Div panel, JSONArray katalogParam) {
		try {
			final JSONArray katalog = katalogParam != null ? katalogParam : LaporanKatalogData.katalog();

			Hbox cariBox = new Hbox();
			cariBox.setWidth("100%");
			cariBox.setAlign("center");
			cariBox.setSpacing("8px");
			cariBox.setStyle("margin:6px 0 10px;");
			cariBox.setParent(panel);
			new Label("Cari laporan:").setParent(cariBox);
			final ais.ui.util.MyTextbox cari = new MyTextbox("");
			// ZK 5.5 di proyek ini tidak mendukung setPlaceholder -- petunjuk cukup lewat label di
			// samping + tooltip (pola sama dgn catatan di PembayaranKoperasiOnline.java).
			cari.setTooltiptext("Ketik judul/keterangan laporan untuk menyaring katalog di bawah.");
			cari.setWidth("100%");
			cari.setParent(cariBox);

			final Div daftarKategori = new Div();
			daftarKategori.setParent(panel);
			renderKatalog(daftarKategori, katalog, "");

			cari.addEventListener(Events.ON_CHANGING, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					String kw = event.getData() == null ? "" : event.getData().toString();
					renderKatalog(daftarKategori, katalog, kw);
				}
			});
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			panel.appendChild(new Label("Gagal memuat katalog laporan: " + e.getMessage()));
		}
	}

	@SuppressWarnings("unchecked")
	private static void renderKatalog(Div container, JSONArray katalog, String keyword) throws Exception {
		container.getChildren().clear();
		String kw = keyword == null ? "" : keyword.trim().toLowerCase();
		for (int i = 0; i < katalog.length(); i++) {
			JSONObject kat = katalog.getJSONObject(i);
			JSONArray items = kat.getJSONArray("items");
			List<JSONObject> cocok = new ArrayList<JSONObject>();
			for (int j = 0; j < items.length(); j++) {
				JSONObject item = items.getJSONObject(j);
				String judul = item.optString("judul", "");
				String ket = item.optString("ket", "");
				if (kw.isEmpty() || judul.toLowerCase().contains(kw) || ket.toLowerCase().contains(kw)) {
					cocok.add(item);
				}
			}
			if (cocok.isEmpty()) {
				continue;
			}

			// Judul kategori. SENGAJA memakai Html biasa, BUKAN MyCaptionStyled: kelas itu turunan
			// org.zkoss.zul.Caption yang secara aturan ZK hanya boleh menjadi anak Groupbox/Window --
			// menempelkannya ke Div (container di sini) melempar "Wrong parent: <Div ...>" saat render
			// dan membuat seluruh katalog gagal tampil.
			Html judulKategori = new Html("<div style=\"font-size:13px;font-weight:800;color:#0f172a;"
					+ "margin:14px 0 6px;padding-left:9px;border-left:4px solid #0e7490;\">"
					+ esc(kat.getString("kat")) + "</div>");
			judulKategori.setParent(container);
			Div grid = new Div();
			grid.setStyle(
					"display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:8px;margin-bottom:16px;");
			grid.setParent(container);
			for (final JSONObject item : cocok) {
				Div kartu = new Div();
				kartu.setSclass("laporan-kartu-zk");
				kartu.setStyle("padding:10px 12px;border-radius:10px;background:#f8fafc;border:1px solid #e2e8f0;"
						+ "cursor:pointer;transition:box-shadow .15s;");
				kartu.setParent(grid);
				Html html = new Html("<div style=\"font-weight:600;color:#0f172a;font-size:12.5px;\">"
						+ esc(item.optString("judul")) + "</div>"
						+ "<div style=\"color:#64748b;font-size:11px;margin-top:3px;line-height:1.4;\">"
						+ esc(item.optString("ket")) + "</div>");
				html.setParent(kartu);
				kartu.addEventListener(Events.ON_CLICK, new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						bukaFormLaporan(item);
					}
				});
			}
		}
	}

	private static String esc(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/** Modal filter + jalankan + unduh -- satu laporan yang dipilih dari katalog. */
	private static void bukaFormLaporan(final JSONObject item) throws Exception {
		final String idLaporan = item.getString("id");
		boolean butuhProduk = item.optBoolean("produk", false);
		boolean butuhPelanggan = item.optBoolean("pelanggan", false);
		boolean butuhPerToko = item.optBoolean("perToko", false);
		// Laporan berbasis jurnal bergantung Satuan Kerja: satu instalasi melayani banyak unit
		// usaha yang masing-masing menuntut paket laporannya sendiri plus konsolidasi.
		boolean butuhSatker = item.optBoolean("satker", false);

		Tbmuser current = Common.getCurrentUser();
		final boolean lockToko = current != null && current.getPedagang() != null
				&& current.getPedagang().getToko() != null;

		final MyWindow win = new MyWindow();
		win.setTitle(item.optString("judul"));
		win.setWidth("92%");
		win.setClosable(true);
		win.setSizable(true);
		win.setMaximizable(true);
		win.setBorder("normal");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(win);

		Div isi = new Div();
		isi.setStyle("padding:10px;");
		isi.setParent(win);

		if (item.has("ket") && !item.optString("ket").isEmpty()) {
			Html ketHtml = new Html("<div style=\"color:#64748b;font-size:11.5px;margin-bottom:8px;\">"
					+ esc(item.optString("ket")) + "</div>");
			ketHtml.setParent(isi);
		}

		Div filterBox = new Div();
		filterBox.setStyle("display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;padding:10px;"
				+ "background:#f8fafc;border-radius:10px;border:1px solid #e2e8f0;margin-bottom:10px;");
		filterBox.setParent(isi);

		final Datebox tglMulai = campoDatebox(filterBox, "Tanggal Mulai");
		final Datebox tglSampai = campoDatebox(filterBox, "Tanggal Sampai");

		final ais.ui.util.MyTextbox txtProduk = butuhProduk ? campoTextbox(filterBox, "Cari Produk") : null;
		final ais.ui.util.MyTextbox txtPelanggan = butuhPelanggan ? campoTextbox(filterBox, "Cari Pelanggan") : null;

		final Combobox comboToko;
		if (!lockToko) {
			Vbox v = new Vbox();
			v.setSpacing("2px");
			v.setParent(filterBox);
			new Label("Toko").setParent(v);
			comboToko = new Combobox();
			Common.insertComboDanSemua(comboToko, new String[] { "nama" }, "kode", Toko.class, "== Semua Toko ==",
					Restrictions.eq("aktif", true));
			comboToko.setWidth("160px");
			comboToko.setReadonly(true);
			comboToko.setParent(v);
		} else {
			comboToko = null;
		}

		final Combobox comboSatker;
		if (butuhSatker) {
			Vbox v = new Vbox();
			v.setSpacing("2px");
			v.setParent(filterBox);
			new Label("Unit / Satuan Kerja").setParent(v);
			comboSatker = new Combobox();
			comboSatker.setWidth("220px");
			comboSatker.setReadonly(true);
			comboSatker.setParent(v);
			try {
				org.json.JSONArray daftarSatker = LaporanKatalogData.daftarSatuanKerja();
				long bawaan = LaporanKatalogData.satuanKerjaBawaan();
				for (int i = 0; i < daftarSatker.length(); i++) {
					org.json.JSONObject s = daftarSatker.getJSONObject(i);
					org.zkoss.zul.Comboitem ci = new org.zkoss.zul.Comboitem(s.optString("nama"));
					ci.setValue(Long.valueOf(s.optLong("id")));
					ci.setParent(comboSatker);
					// Baris pertama (id 0 = Semua Unit) dipakai bila unit bawaan belum diatur.
					if (i == 0 || s.optLong("id") == bawaan) {
						comboSatker.setSelectedItem(ci);
					}
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit LaporanKantinZkPanel.daftarSatuanKerja");
			}
		} else {
			comboSatker = null;
		}

		final Checkbox chkPerToko;
		if (butuhPerToko && !lockToko) {
			Vbox v = new Vbox();
			v.setSpacing("2px");
			v.setParent(filterBox);
			new Label("Rincian").setParent(v);
			chkPerToko = new Checkbox("Per Toko");
			chkPerToko.setParent(v);
		} else {
			chkPerToko = null;
		}

		final Div hasilArea = new Div();
		hasilArea.setStyle("max-height:60vh;overflow:auto;");
		hasilArea.setParent(isi);
		hasilArea.appendChild(new Label("Atur filter (opsional) lalu klik \"Jalankan\"."));

		// Baris tombol aksi. SENGAJA memakai Div biasa di dalam `isi`, BUKAN South: South adalah
		// LayoutRegion yang secara aturan ZK hanya boleh menjadi anak Borderlayout -- menempelkannya
		// langsung ke Window melempar "Wrong parent: <Window ...>" begitu form laporan dibuka.
		Div south = new Div();
		south.setStyle("border-top:1px solid #e2e8f0;margin-top:8px;");
		south.setParent(isi);
		Hbox aksi = new Hbox();
		aksi.setSpacing("8px");
		aksi.setStyle("padding:8px;");
		aksi.setParent(south);

		MyToolbarbuttonConfig btnJalankan = new MyToolbarbuttonConfig("Jalankan", "/img/svg/refresh.svg");
		final LaporanKantinUtil.Hasil[] hasilTerakhir = new LaporanKantinUtil.Hasil[1];
		btnJalankan.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Map<String, String> p = kumpulkanParam(idLaporan, tglMulai, tglSampai, txtProduk, txtPelanggan,
							comboToko, chkPerToko, comboSatker);
					HttpServletRequest wrapped = new ParamRequestWrapper(nativeRequest(), p);
					LaporanKantinUtil.Hasil H = LaporanKantinUtil.build(wrapped);
					hasilTerakhir[0] = H;
					if (!"00".equals(H.status)) {
						hasilArea.getChildren().clear();
						hasilArea.appendChild(new Label(
								H.message == null || H.message.isEmpty() ? "Laporan ini belum tersedia." : H.message));
						return;
					}
					renderHasil(hasilArea, H, urlRincianTransaksi(p));
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					MyMessageboxConfig.show("Gagal menjalankan laporan: " + e.getMessage(), "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			}
		});
		btnJalankan.setParent(aksi);

		// Ikon WAJIB menunjuk berkas yang benar-benar ada di webapp/img -- ZK tidak memvalidasi path
		// ikon, jadi salah nama hanya terlihat sbg gambar rusak di layar, bukan error. "/img/svg/pdf.svg"
		// TIDAK ADA di repo ini; yang tersedia (dan sudah dipakai layar lain) adalah file-pdf.svg.
		MyToolbarbuttonConfig btnPdf = new MyToolbarbuttonConfig("Unduh PDF", "/img/svg/file-pdf.svg");
		btnPdf.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Map<String, String> p = kumpulkanParam(idLaporan, tglMulai, tglSampai, txtProduk, txtPelanggan,
							comboToko, chkPerToko, comboSatker);
					HttpServletRequest wrapped = new ParamRequestWrapper(nativeRequest(), p);
					LaporanKantinUtil.Hasil H = LaporanKantinUtil.build(wrapped);
					if (!"00".equals(H.status)) {
						MyMessageboxConfig.show(
								H.message == null || H.message.isEmpty() ? "Laporan ini belum tersedia." : H.message,
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					new LaporanKantinPdf().generate(wrapped, H, bos);
					Filedownload.save(bos.toByteArray(), "application/pdf", namaBerkas(H.judul) + ".pdf");
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					MyMessageboxConfig.show("Gagal membuat PDF: " + e.getMessage(), "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			}
		});
		btnPdf.setParent(aksi);

		MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig("Unduh Excel", "/img/excel.png");
		btnExcel.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Map<String, String> p = kumpulkanParam(idLaporan, tglMulai, tglSampai, txtProduk, txtPelanggan,
							comboToko, chkPerToko, comboSatker);
					HttpServletRequest wrapped = new ParamRequestWrapper(nativeRequest(), p);
					LaporanKantinUtil.Hasil H = LaporanKantinUtil.build(wrapped);
					if (!"00".equals(H.status)) {
						MyMessageboxConfig.show(
								H.message == null || H.message.isEmpty() ? "Laporan ini belum tersedia." : H.message,
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					byte[] csv = bangunCsv(H);
					Filedownload.save(csv, "text/csv", namaBerkas(H.judul) + ".csv");
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					MyMessageboxConfig.show("Gagal membuat Excel: " + e.getMessage(), "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			}
		});
		btnExcel.setParent(aksi);

		// "/img/svg/close.svg" juga TIDAK ADA di repo ini; ikon tutup/batal yang baku dipakai di
		// ~1.500 tempat adalah /img/cancel.gif.
		MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		btnTutup.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				win.detach();
			}
		});
		btnTutup.setParent(aksi);

		win.doModal();
	}

	private static Datebox campoDatebox(Div parent, String label) {
		Vbox v = new Vbox();
		v.setSpacing("2px");
		v.setParent(parent);
		new Label(label).setParent(v);
		Datebox db = new Datebox();
		db.setFormat("dd-MM-yyyy");
		db.setWidth("130px");
		db.setParent(v);
		return db;
	}

	private static ais.ui.util.MyTextbox campoTextbox(Div parent, String label) {
		Vbox v = new Vbox();
		v.setSpacing("2px");
		v.setParent(parent);
		new Label(label).setParent(v);
		ais.ui.util.MyTextbox tb = new MyTextbox("");
		tb.setWidth("160px");
		tb.setParent(v);
		return tb;
	}

	private static Map<String, String> kumpulkanParam(String idLaporan, Datebox tglMulai, Datebox tglSampai,
			ais.ui.util.MyTextbox txtProduk, ais.ui.util.MyTextbox txtPelanggan, Combobox comboToko,
			Checkbox chkPerToko, Combobox comboSatker) {
		Map<String, String> p = new HashMap<String, String>();
		p.put("r", idLaporan);
		if (tglMulai.getValue() != null) {
			p.put("tglMulai", TGL_PARAM.get().format(tglMulai.getValue()));
		}
		if (tglSampai.getValue() != null) {
			p.put("tglSampai", TGL_PARAM.get().format(tglSampai.getValue()));
		}
		if (txtProduk != null && txtProduk.getValue() != null && !txtProduk.getValue().trim().isEmpty()) {
			p.put("qProduk", txtProduk.getValue().trim());
		}
		if (txtPelanggan != null && txtPelanggan.getValue() != null && !txtPelanggan.getValue().trim().isEmpty()) {
			p.put("qPelanggan", txtPelanggan.getValue().trim());
		}
		if (comboToko != null && comboToko.getSelectedIndex() > 0
				&& comboToko.getSelectedItem().getValue() instanceof Toko) {
			Toko t = (Toko) comboToko.getSelectedItem().getValue();
			p.put("tokoId", String.valueOf(t.getId()));
		}
		if (chkPerToko != null) {
			p.put("perToko", chkPerToko.isChecked() ? "true" : "false");
		}
		if (comboSatker != null && comboSatker.getSelectedItem() != null
				&& comboSatker.getSelectedItem().getValue() instanceof Long) {
			p.put("satkerId", String.valueOf(comboSatker.getSelectedItem().getValue()));
		}
		return p;
	}

	private static HttpServletRequest nativeRequest() {
		return (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
	}

	private static String namaBerkas(String judul) {
		String s = judul == null || judul.isEmpty() ? "laporan" : judul;
		s = s.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
		return "laporan_" + (s.isEmpty() ? "kantin" : s);
	}

	/**
	 * Render {@code H.kolom}/{@code H.baris} sbg tabel HTML polos (bukan {@code MyGrid}/{@code Grid}
	 * ZK -- jumlah kolom laporan BERAGAM per jenis laporan, tabel HTML manual lebih simpel drpd
	 * membangun ulang {@code Columns} dinamis tiap kali laporan dijalankan) -- termasuk subtotal per
	 * grup ({@code H.grup}) dan grand total ({@code H.grandTotal}), meniru tampilan versi JSP.
	 */
	private static void renderHasil(Div hasilArea, LaporanKantinUtil.Hasil H, String urlRincian) {
		hasilArea.getChildren().clear();
		if (H.baris.isEmpty()) {
			hasilArea.appendChild(new Label("Tidak ada data untuk filter yang dipilih."));
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"font-weight:700;color:#0f172a;margin-bottom:6px;\">").append(esc(H.judul))
				.append("</div>");
		if (H.catatan != null && !H.catatan.isEmpty()) {
			sb.append("<div style=\"color:#64748b;font-size:11px;margin-bottom:8px;\">").append(esc(H.catatan))
					.append("</div>");
		}
		sb.append("<table style=\"width:100%;border-collapse:collapse;font-size:11.5px;\">");
		sb.append("<thead><tr style=\"background:#f1f5f9;\">");
		for (LaporanKantinUtil.Kolom k : H.kolom) {
			String align = "num".equals(k.tipe) ? "right" : "left";
			sb.append("<th style=\"padding:6px 8px;text-align:").append(align)
					.append(";border-bottom:2px solid #e2e8f0;color:#334155;\">").append(esc(k.label))
					.append("</th>");
		}
		sb.append("</tr></thead><tbody>");

		double[] subtotal = H.grup >= 0 ? new double[H.tipe.length] : null;
		double[] grand = H.grandTotal ? new double[H.tipe.length] : null;
		// Baris penyusun subtotal grup berjalan -- dipakai popup "asal angka" pada
		// baris subtotal, sama seperti data-grup di versi JSP.
		java.util.List<Object[]> anggotaGrup = new java.util.ArrayList<Object[]>();
		Object nilaiGrupSebelumnya = null;
		boolean grupPertama = true;

		for (Object[] row : H.baris) {
			Object nilaiGrupIni = H.grup >= 0 && H.grup < row.length ? row[H.grup] : null;
			if (H.grup >= 0 && !grupPertama && !samaGrup(nilaiGrupSebelumnya, nilaiGrupIni)) {
				sb.append(barisSubtotal(H, subtotal, "Subtotal", anggotaGrup));
				subtotal = new double[H.tipe.length];
				anggotaGrup = new java.util.ArrayList<Object[]>();
			}
			anggotaGrup.add(row);
			sb.append("<tr>");
			// Setiap angka dibuat DAPAT DIKLIK: menampilkan data penghitungannya (seluruh
			// kolom baris ini + catatan rumus laporan) -- padanan drill-down di JSP/Desktop,
			// dikerjakan dgn HTML+JS polos karena panel ini memang merender HTML mentah.
			String rincianBaris = rincianBarisHtml(H, row);
			// Dimensi baris (kode/nama produk, kasir, metode, pelanggan) ikut dibawa
			// supaya popup dapat menarik NOTA penyusun angka -- bukan sekadar
			// mengulang isi baris ringkasan.
			String dimensiBaris = esc(dimensiQuery(H, row));
			for (int i = 0; i < H.tipe.length; i++) {
				Object v = i < row.length ? row[i] : null;
				String teksSel = esc(formatSel(H.tipe[i], v));
				boolean selAngka = "num".equals(H.tipe[i]) && v != null;
				sb.append("<td style=\"padding:5px 8px;border-bottom:1px solid #f1f5f9;text-align:")
						.append("num".equals(H.tipe[i]) ? "right" : "left").append(";\">");
				if (selAngka) {
					String judulSel = esc(row.length > 0 && row[0] != null ? String.valueOf(row[0]).trim()
							: (i < H.kolom.size() ? H.kolom.get(i).label : "Rincian"));
					sb.append("<a href=\"javascript:void(0)\" class=\"lkzk-num\"")
							.append(" style=\"text-decoration:underline dotted;cursor:pointer;color:inherit;\"")
							.append(" title=\"Klik untuk melihat data penghitungannya\"")
							.append(" data-judul=\"").append(judulSel).append("\"")
							.append(" data-rincian=\"").append(rincianBaris).append("\"")
							.append(" data-dim=\"").append(dimensiBaris).append("\">")
							.append(teksSel).append("</a>");
				} else {
					sb.append(teksSel);
				}
				sb.append("</td>");
				if ("num".equals(H.tipe[i]) && v instanceof Number) {
					if (subtotal != null) {
						subtotal[i] += ((Number) v).doubleValue();
					}
					if (grand != null) {
						grand[i] += ((Number) v).doubleValue();
					}
				}
			}
			sb.append("</tr>");
			nilaiGrupSebelumnya = nilaiGrupIni;
			grupPertama = false;
		}
		if (H.grup >= 0 && subtotal != null && H.baris.size() > 0) {
			sb.append(barisSubtotal(H, subtotal, "Subtotal", anggotaGrup));
		}
		if (grand != null) {
			sb.append(barisSubtotal(H, grand, "Grand Total", H.baris));
		}
		sb.append("</tbody></table>");
		sb.append(skripRincianAngka(urlRincian));
		Html html = new Html(sb.toString());
		hasilArea.appendChild(html);
	}

	private static boolean samaGrup(Object a, Object b) {
		if (a == null) {
			return b == null;
		}
		return a.equals(b);
	}

	/**
	 * Ringkasan "asal angka" satu baris laporan: pasangan Kolom: Nilai dipisah " | ",
	 * ditambah catatan rumus laporan bila ada. Dipakai sbg isi popup saat angka diklik
	 * (atribut data-rincian), sudah di-escape agar aman disisipkan ke atribut HTML.
	 */
	private static String rincianBarisHtml(LaporanKantinUtil.Hasil H, Object[] row) {
		StringBuilder r = new StringBuilder();
		for (int i = 0; i < H.tipe.length && i < H.kolom.size(); i++) {
			Object v = i < row.length ? row[i] : null;
			if (r.length() > 0) {
				r.append(" | ");
			}
			r.append(H.kolom.get(i).label).append(": ").append(formatSel(H.tipe[i], v));
		}
		if (H.catatan != null && H.catatan.trim().length() > 0) {
			r.append("  ||  ").append(H.catatan.trim());
		}
		return esc(r.toString());
	}

	/**
	 * Skrip popup rincian (sekali per render). Sengaja membangun DOM lewat API
	 * (createElement/textContent), BUKAN merangkai string HTML: selain bebas isu
	 * escape kutip di dalam sumber Java, nilai sel otomatis aman dari injeksi.
	 */
	/**
	 * Alamat layanan rincian transaksi berikut filter yang SEDANG aktif (rentang
	 * tanggal + toko). Endpoint-nya sama dgn yang dipakai layar Laporan versi JSP,
	 * sehingga ZKoss tidak punya query sendiri yang bisa menyimpang.
	 */
	/** Literal string JavaScript yang aman (kutip tunggal + escape minimal). */
	private static String jsString(String v) {
		String x = v == null ? "" : v;
		x = x.replace("\\", "\\\\").replace("'", "\'").replace("<", "\u003c");
		return "'" + x + "'";
	}

	private static String urlRincianTransaksi(Map<String, String> p) {
		StringBuilder u = new StringBuilder(ais.common.Common.ROOT);
		u.append("/baru?hanya_tampil_jsp=true&p=kantin&s=laporan_laporan_service&rincianTransaksi=1");
		tambahParam2(u, "tglMulai", p.get("tglMulai"));
		tambahParam2(u, "tglSampai", p.get("tglSampai"));
		tambahParam2(u, "tokoId", p.get("tokoId"));
		return u.toString();
	}

	private static void tambahParam2(StringBuilder u, String nama, String nilai) {
		if (nilai == null || nilai.trim().length() == 0) {
			return;
		}
		try {
			u.append("&").append(nama).append("=")
					.append(java.net.URLEncoder.encode(nilai.trim(), "UTF-8"));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "LaporanKantinZkPanel.tambahParam2: gagal encode " + nama);
		}
	}

	/**
	 * Rangkai dimensi penyaring satu baris menjadi query-string, dikenali dari LABEL
	 * kolomnya -- sama persis dgn aturan di POS Desktop/Android dan JSP, sehingga
	 * ketiga kanal menyaring transaksi dengan cara yang sama.
	 *
	 * @return mis. {@code "kodeProduk=ABC-1&kasir=Hadi"}, atau string kosong bila baris
	 *         ini tidak punya dimensi apa pun (mis. laporan stok / baris total)
	 */
	private static String dimensiQuery(LaporanKantinUtil.Hasil H, Object[] row) {
		String kodeProduk = null, namaProduk = null, kasir = null, metode = null, pelanggan = null;
		String kodePelanggan = null;
		boolean laporanPiutang = "Daftar Saldo Piutang Customer".equals(H.judul);
		for (int i = 0; i < H.tipe.length && i < H.kolom.size() && i < row.length; i++) {
			if (!"text".equals(H.tipe[i])) {
				continue;
			}
			String nilai = row[i] == null ? "" : String.valueOf(row[i]).trim();
			if (nilai.length() == 0 || "-".equals(nilai)) {
				continue;
			}
			String label = H.kolom.get(i).label == null ? "" : H.kolom.get(i).label.toLowerCase();
			if (label.indexOf("kode") >= 0 && laporanPiutang && kodePelanggan == null) {
				kodePelanggan = nilai;
			} else if (label.indexOf("kode") >= 0 && kodeProduk == null) {
				kodeProduk = nilai;
			} else if ((label.indexOf("nama produk") >= 0 || label.indexOf("barang") >= 0) && namaProduk == null) {
				namaProduk = nilai;
			} else if (label.indexOf("kasir") >= 0 && kasir == null) {
				kasir = nilai;
			} else if ((label.indexOf("metode") >= 0 || label.indexOf("kas/bank") >= 0) && metode == null) {
				metode = nilai;
			} else if ((label.indexOf("pelanggan") >= 0 || label.indexOf("anggota") >= 0
					|| label.indexOf("member") >= 0) && pelanggan == null) {
				pelanggan = nilai;
			}
		}
		StringBuilder q = new StringBuilder();
		if (laporanPiutang) { tambahParam(q, "hanyaPiutang", "true"); }
		tambahParam(q, "kodePelanggan", kodePelanggan);
		tambahParam(q, "kodeProduk", kodeProduk);
		tambahParam(q, "namaProduk", namaProduk);
		tambahParam(q, "kasir", kasir);
		tambahParam(q, "metode", metode);
		tambahParam(q, "pelanggan", pelanggan);
		return q.toString();
	}

	private static void tambahParam(StringBuilder q, String nama, String nilai) {
		if (nilai == null || nilai.trim().length() == 0) {
			return;
		}
		try {
			if (q.length() > 0) {
				q.append("&");
			}
			q.append(nama).append("=").append(java.net.URLEncoder.encode(nilai.trim(), "UTF-8"));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "LaporanKantinZkPanel.tambahParam: gagal encode " + nama);
		}
	}

	private static String skripRincianAngka(String urlRincian) {
		return "<script>(function(){"
				+ "window.__lkzkUrlRincian = " + jsString(urlRincian) + ";"
				+ "if (window.__lkzkRincian) return; window.__lkzkRincian = true;"
				+ "document.addEventListener('click', function(ev){"
				+ "  var a = ev.target && ev.target.closest ? ev.target.closest('a.lkzk-num') : null;"
				+ "  if (!a) return; ev.preventDefault();"
				+ "  var judul = a.getAttribute('data-judul') || 'Rincian';"
				+ "  var isi = a.getAttribute('data-rincian') || '';"
				+ "  var bagian = isi.split('  ||  ');"
				+ "  var baris = (bagian[0] || '').split(' | ');"
				+ "  var ov = document.createElement('div');"
				+ "  ov.id = 'lkzkOverlay';"
				+ "  ov.style.cssText = 'position:fixed;inset:0;background:rgba(15,23,42,.45);z-index:99999;display:flex;align-items:center;justify-content:center';"
				+ "  var box = document.createElement('div');"
				+ "  box.style.cssText = 'background:#fff;border-radius:10px;max-width:900px;width:92%;max-height:80vh;overflow:auto;padding:18px 20px;box-shadow:0 10px 30px rgba(0,0,0,.25)';"
				+ "  var h = document.createElement('div');"
				+ "  h.style.cssText = 'font-weight:700;font-size:15px;margin-bottom:10px';"
				+ "  h.textContent = 'Data Penghitungan: ' + judul; box.appendChild(h);"
				+ "  var tb = document.createElement('table'); tb.style.cssText = 'width:100%;font-size:12.5px';"
				+ "  for (var i=0;i<baris.length;i++){"
				+ "    var p = baris[i].split(': ');"
				+ "    var tr = document.createElement('tr');"
				+ "    var td1 = document.createElement('td'); td1.style.cssText='padding:3px 6px;color:#475569;width:45%'; td1.textContent = p[0] || '';"
				+ "    var td2 = document.createElement('td'); td2.style.cssText='padding:3px 6px;font-weight:600'; td2.textContent = p.slice(1).join(': ');"
				+ "    tr.appendChild(td1); tr.appendChild(td2); tb.appendChild(tr);"
				+ "  }"
				+ "  box.appendChild(tb);"
				+ "  if (bagian.length > 1) {"
				+ "    var ct = document.createElement('div');"
				+ "    ct.style.cssText = 'margin-top:10px;font-size:11.5px;font-style:italic;color:#64748b';"
				+ "    ct.textContent = bagian[1]; box.appendChild(ct);"
				+ "  }"
				+ "  var dim = a.getAttribute('data-dim') || '';"
				+ "  var trx = document.createElement('div'); trx.style.cssText='margin-top:12px'; box.appendChild(trx);"
				+ "  if (!dim) {"
				+ "    trx.style.cssText='margin-top:12px;font-size:11.5px;color:#64748b';"
				+ "    trx.textContent = 'Baris ini tidak berasal dari transaksi penjualan, sehingga tidak ada nota penyusun.';"
				+ "  } else {"
				+ "    trx.textContent = 'Memuat rincian transaksi...';"
				+ "    trx.style.fontSize = '12px'; trx.style.color = '#64748b';"
				+ "    fetch(window.__lkzkUrlRincian + '&' + dim).then(function(r){ return r.json(); }).then(function(res){"
				+ "      var rows = (res && res.data) ? res.data : [];"
				+ "      trx.textContent = '';"
				+ "      trx.style.color = 'inherit';"
				+ "      if (!rows.length) { trx.style.color='#64748b'; trx.textContent = 'Tidak ada transaksi yang cocok pada rentang tanggal ini.'; return; }"
				+ "      var jud = document.createElement('div');"
				+ "      jud.style.cssText='font-weight:700;font-size:12.5px;margin-bottom:6px';"
				+ "      jud.textContent = 'Transaksi penyusun angka ini'; trx.appendChild(jud);"
				+ "      var t2 = document.createElement('table'); t2.style.cssText='width:100%;font-size:11.5px;border-collapse:collapse';"
				+ "      var rincianPiutang = dim.indexOf('hanyaPiutang=true') >= 0;"
				+ "      var judulKolom = ['Waktu','No. Nota','Kasir','Pelanggan','Produk'];"
				+ "      if (rincianPiutang) { judulKolom.push('Metode Pembayaran'); judulKolom.push('Jenis Piutang'); judulKolom.push('Piutang Faktur'); }"
				+ "      judulKolom.push('Qty'); judulKolom.push('Harga'); judulKolom.push('Total Produk');"
				+ "      var numerikMulai = rincianPiutang ? 7 : 5;"
				+ "      var trh = document.createElement('tr');"
				+ "      for (var k=0;k<judulKolom.length;k++){"
				+ "        var th = document.createElement('th');"
				+ "        th.style.cssText='text-align:'+(k>=numerikMulai?'right':'left')+';padding:3px 5px;border-bottom:1px solid #e2e8f0;color:#475569';"
				+ "        th.textContent = judulKolom[k]; trh.appendChild(th);"
				+ "      }"
				+ "      t2.appendChild(trh);"
				+ "      var fmt = function(v){ return new Intl.NumberFormat('id-ID',{maximumFractionDigits:2}).format(Number(v)||0); };"
				+ "      for (var j=0;j<rows.length;j++){"
				+ "        var r2 = rows[j]; var tr2 = document.createElement('tr');"
				+ "        var isi = [String(r2.waktu||'').split('.')[0], r2.nota||'', r2.kasir||'', r2.pelanggan||'', r2.produk||''];"
				+ "        if (rincianPiutang) { isi.push(r2.metode||''); isi.push(r2.jenisPiutang||''); isi.push(fmt(r2.nilaiPiutang)); }"
				+ "        isi.push(fmt(r2.qty)); isi.push(fmt(r2.harga)); isi.push(fmt(r2.total));"
				+ "        for (var c=0;c<isi.length;c++){"
				+ "          var td = document.createElement('td');"
				+ "          td.style.cssText='padding:3px 5px;border-bottom:1px solid #f1f5f9;text-align:'+(c>=numerikMulai?'right':'left');"
				+ "          td.textContent = isi[c]; tr2.appendChild(td);"
				+ "        }"
				+ "        t2.appendChild(tr2);"
				+ "      }"
				+ "      trx.appendChild(t2);"
				+ "      var ket = document.createElement('div');"
				+ "      ket.style.cssText='margin-top:6px;font-size:11px;font-style:italic;color:#64748b';"
				+ "      ket.textContent = rows.length + ' baris produk · total nilai produk ' + fmt(res.totalNilai)"
				+ "        + (rincianPiutang ? ' · total piutang unik per faktur ' + fmt(res.totalPiutang) : '')"
				+ "        + (res.dibatasi ? ' (dibatasi, masih ada baris lain)' : '');"
				+ "      trx.appendChild(ket);"
				+ "    }).catch(function(){ trx.style.color='#b91c1c'; trx.textContent = 'Rincian transaksi tidak dapat dimuat.'; });"
				+ "  }"
				+ "  var wrap = document.createElement('div'); wrap.style.cssText = 'text-align:right;margin-top:14px';"
				+ "  var btn = document.createElement('button');"
				+ "  btn.type = 'button'; btn.textContent = 'Tutup';"
				+ "  btn.style.cssText = 'padding:6px 16px;border:0;border-radius:6px;background:#1e3a5f;color:#fff;cursor:pointer';"
				+ "  btn.onclick = function(){ ov.remove(); };"
				+ "  wrap.appendChild(btn); box.appendChild(wrap); ov.appendChild(box);"
				+ "  ov.addEventListener('click', function(e){ if (e.target === ov) ov.remove(); });"
				+ "  document.body.appendChild(ov);"
				+ "});"
				+ "})();</script>";
	}

	private static String barisSubtotal(LaporanKantinUtil.Hasil H, double[] nilai, String label,
			java.util.List<Object[]> penyusun) {
		StringBuilder sb = new StringBuilder();
		sb.append("<tr style=\"background:#f8fafc;font-weight:700;\">");
		for (int i = 0; i < H.tipe.length; i++) {
			boolean angka = "num".equals(H.tipe[i]);
			String isi = i == 0 ? label : (angka ? NUM.get().format(nilai[i]) : "");
			sb.append("<td style=\"padding:5px 8px;border-top:2px solid #e2e8f0;text-align:")
					.append(angka ? "right" : "left").append(";\">");
			// ANGKA SUBTOTAL/GRAND TOTAL IKUT DAPAT DIKLIK: popup memperlihatkan baris
			// yang dijumlahkan menjadi angka itu. Tidak ada data-dim karena angka
			// agregat tidak menunjuk satu dimensi transaksi tertentu.
			if (angka && penyusun != null && !penyusun.isEmpty()) {
				String labelKolom = i < H.kolom.size() ? H.kolom.get(i).label : "";
				sb.append("<a href=\"javascript:void(0)\" class=\"lkzk-num\"")
						.append(" style=\"text-decoration:underline dotted;cursor:pointer;color:inherit;\"")
						.append(" title=\"Klik untuk melihat baris penyusunnya\"")
						.append(" data-judul=\"").append(esc(label + " " + labelKolom)).append("\"")
						.append(" data-rincian=\"").append(penyusunRingkas(H, penyusun, i, labelKolom, nilai[i]))
						.append("\">").append(esc(isi)).append("</a>");
			} else {
				sb.append(esc(isi));
			}
			sb.append("</td>");
		}
		sb.append("</tr>");
		return sb.toString();
	}

	/** Batas baris penyusun yang disisipkan ke atribut supaya HTML-nya tidak membengkak. */
	private static final int BATAS_PENYUSUN = 200;

	/**
	 * Membuang pemisah yang dipakai skrip popup dari teks yang berasal dari DATA
	 * pengguna. Nama produk atau uraian boleh saja memuat {@code " | "} atau
	 * {@code ": "}; tanpa dibersihkan, satu baris popup dapat terbelah dua atau
	 * nilainya tampak berpindah kolom.
	 */
	private static String tanpaPemisah(String teks) {
		return teks.replace(" | ", " / ").replace(": ", " - ");
	}

	/**
	 * Ringkasan baris penyusun satu angka agregat, dalam format pasangan
	 * {@code "Label: Nilai"} yang sudah dipahami skrip popup ZK. Bagian setelah
	 * {@code "  ||  "} menjadi catatan di bawah tabel popup.
	 */
	private static String penyusunRingkas(LaporanKantinUtil.Hasil H, java.util.List<Object[]> penyusun,
			int kolAngka, String labelKolom, double total) {
		StringBuilder isi = new StringBuilder();
		int dipakai = 0;
		for (int r = 0; r < penyusun.size() && dipakai < BATAS_PENYUSUN; r++) {
			Object[] row = penyusun.get(r);
			String labelBaris = row.length > 0 && row[0] != null ? String.valueOf(row[0]).trim() : "";
			if (labelBaris.length() == 0) {
				labelBaris = "(tanpa label)";
			}
			labelBaris = tanpaPemisah(labelBaris);
			Object v = kolAngka < row.length ? row[kolAngka] : null;
			if (isi.length() > 0) {
				isi.append(" | ");
			}
			isi.append(labelBaris).append(": ").append(formatSel(H.tipe[kolAngka], v));
			dipakai++;
		}
		StringBuilder catatan = new StringBuilder();
		catatan.append("TOTAL (").append(labelKolom).append("): ").append(NUM.get().format(total))
				.append(" -- ").append(penyusun.size()).append(" baris penyusun.");
		if (penyusun.size() > dipakai) {
			catatan.append(" Ditampilkan ").append(dipakai).append(" baris pertama.");
		}
		return esc(isi.toString() + "  ||  " + catatan.toString());
	}

	private static String formatSel(String tipe, Object v) {
		if (v == null) {
			return "";
		}
		if ("num".equals(tipe) && v instanceof Number) {
			return NUM.get().format(((Number) v).doubleValue());
		}
		if ("tgl".equals(tipe) && v instanceof java.util.Date) {
			return TGL_TAMPIL.get().format((java.util.Date) v);
		}
		return v.toString();
	}

	/** CSV ber-BOM UTF-8 (Excel-friendly) -- pola SAMA dgn {@code dataToCsv} versi JSP. */
	private static byte[] bangunCsv(LaporanKantinUtil.Hasil H) {
		StringBuilder sb = new StringBuilder("﻿");
		for (int i = 0; i < H.kolom.size(); i++) {
			if (i > 0) {
				sb.append(';');
			}
			sb.append(csvSel(H.kolom.get(i).label));
		}
		sb.append("\r\n");
		for (Object[] row : H.baris) {
			for (int i = 0; i < H.tipe.length; i++) {
				if (i > 0) {
					sb.append(';');
				}
				Object v = i < row.length ? row[i] : null;
				sb.append(csvSel(formatSel(H.tipe[i], v)));
			}
			sb.append("\r\n");
		}
		return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
	}

	private static String csvSel(String s) {
		if (s == null) {
			return "";
		}
		if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
			return "\"" + s.replace("\"", "\"\"") + "\"";
		}
		return s;
	}

	/**
	 * Bungkus native request ZK supaya {@code getParameter} menjawab dari nilai form ZK -- pola SAMA
	 * PERSIS dgn {@code PosApi.ParamRequestWrapper} (Desktop), duplikasi kecil disengaja krn kelas
	 * asalnya package-private di package servlet yang berbeda.
	 */
	private static final class ParamRequestWrapper extends HttpServletRequestWrapper {
		private final Map<String, String> params;

		ParamRequestWrapper(HttpServletRequest request, Map<String, String> params) {
			super(request);
			this.params = params;
		}

		@Override
		public String getParameter(String name) {
			if (params.containsKey(name)) {
				return params.get(name);
			}
			return super.getParameter(name);
		}
	}

}
