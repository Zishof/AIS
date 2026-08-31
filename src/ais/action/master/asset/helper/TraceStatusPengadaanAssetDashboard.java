package ais.action.master.asset.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.ProsesTransfer;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

/**
 * Dashboard "Trace Status Barang/Jasa" — Laporan Proses Pengajuan Sarpras.
 *
 * <p>Menelusuri sejauh mana sebuah Permintaan Pembelian (PR) berjalan pada alur
 * pengadaan sarana-prasarana:</p>
 * <ol>
 *   <li>Permintaan (PR)</li>
 *   <li>Uang Muka (UM) — {@code UangMuka} / UangMukaAction</li>
 *   <li>Pemesanan (PO) — {@code PemesananPengadaanMasterAsset}</li>
 *   <li>Penerimaan (BAST) — {@code PenerimaanPengadaanMasterAsset}</li>
 *   <li>Terima Tagihan — {@code SaldoAwalMasterAsset}</li>
 *   <li>Sudah Dibayar di DPC — {@code ProsesTransfer} (realisasikanOleh != null)</li>
 * </ol>
 *
 * <p>Dibuat ringan untuk ZKoss 5.5: query dibatasi, relasi dimuat secara batch,
 * tabel memakai paging, dan seluruh pemrosesan dilakukan di dalam satu session
 * yang dibuka sendiri lalu ditutup di {@code finally}.</p>
 */
public class TraceStatusPengadaanAssetDashboard extends Vbox {

	private static final long serialVersionUID = 6631472004531190021L;
	// Batas jumlah PR yang ditelusuri. Dinaikkan dari 150 → 3000 agar laporan bisa menampilkan data
	// LINTAS TAHUN sekaligus (mis. 2023–2026); dengan 150 dulu, rentang multi-tahun terpotong ke 150
	// PR terbaru sehingga terasa "hanya per tahun". Ringkasan Tahapan pun ikut lengkap dalam batas ini.
	private static final int DEFAULT_LIMIT = 3000;
	private static final int GRID_PAGE_SIZE = 15;

	// Rank tahapan alur pengadaan (semakin besar = semakin maju).
	private static final int STAGE_PERMINTAAN = 0;
	private static final int STAGE_UM = 1;
	private static final int STAGE_PO = 2;
	private static final int STAGE_BAST = 3;
	private static final int STAGE_TAGIHAN = 4;
	private static final int STAGE_DIBAYAR = 5;

	private Textbox keyword;
	private Combobox tahap;
	private MyDatebox tglMulai;
	private MyDatebox tglSampai;
	private Vbox body;

	// Data hasil pemuatan terakhir — dipakai tombol "Cetak" agar isi PDF sama persis dengan tabel.
	private List<TraceRow> lastRows = new ArrayList<TraceRow>();
	private Summary lastSummary = new Summary();

	/**
	 * Tipe implementasi bersarang {@link TraceRow} milik {@link TraceStatusPengadaanAssetDashboard}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * TraceStatusPengadaanAssetDashboard}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int no}, {@code Date tanggal}, {@code
	 * String prKode}, {@code String anggaran}, {@code String satuanKerja}, {@code double nilaiPengajuan}, {@code
	 * String jenisPemesanan}, {@code String jenis}. Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 *
	 * @see TraceStatusPengadaanAssetDashboard
	 */
	private static class TraceRow {
		private int no;
		private Date tanggal;
		private String prKode;
		private String anggaran;
		private String satuanKerja;
		private double nilaiPengajuan;
		private String jenisPemesanan;
		private String jenis;
		private String umKode;
		private double umNilai;
		private String poKode;
		private double poNilai;
		private String bastKode;
		private String tagihanKode;
		private double tagihanNilai;
		private String pksKode;
		private String dpcKode;
		private boolean hasUm;
		private boolean hasPo;
		private boolean hasBast;
		private boolean hasTagihan;
		private boolean paid;
		private double nominalLunas;
		private double nominalBelumLunas;
		private int stage;
	}

	/**
	 * Pembawa data/helper lokal milik {@link TraceStatusPengadaanAssetDashboard} untuk summary. Tipe ini
	 * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
	 * jelas.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * TraceStatusPengadaanAssetDashboard}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
	 * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
	 * kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int total}, {@code int reachUm},
	 * {@code int reachPo}, {@code int reachBast}, {@code int reachTagihan}, {@code int reachDibayar}, {@code int
	 * belumProses}, {@code double totalNilai}. Aturan bisnis bersama tetap berada pada kelas induk atau service
	 * yang dipanggilnya.</p>
	 *
	 * @see TraceStatusPengadaanAssetDashboard
	 */
	private static class Summary {
		private int total;
		private int reachUm;
		private int reachPo;
		private int reachBast;
		private int reachTagihan;
		private int reachDibayar;
		private int belumProses;
		private double totalNilai;
	}

	public TraceStatusPengadaanAssetDashboard() {
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

		new Label("Cari No. PR / keterangan: ").setParent(toolbar);
		keyword = new Textbox();
		keyword.setWidth("240px");
		keyword.setParent(toolbar);
		keyword.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload();
			}
		});

		new Label(ais.common.Common.getBahasaConfig(" Tahapan: ")).setParent(toolbar);
		tahap = new Combobox();
		tahap.setReadonly(true);
		tahap.setWidth("190px");
		appendCombo(tahap, "Semua", "SEMUA");
		appendCombo(tahap, "Baru Permintaan", "PERMINTAAN");
		appendCombo(tahap, "Sudah Uang Muka", "UM");
		appendCombo(tahap, "Sudah PO", "PO");
		appendCombo(tahap, "Sudah BAST", "BAST");
		appendCombo(tahap, "Sudah Terima Tagihan", "TAGIHAN");
		appendCombo(tahap, "Sudah Dibayar (DPC)", "DIBAYAR");
		tahap.setSelectedIndex(0);
		tahap.setParent(toolbar);

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

		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		cetak.setTooltiptext("Cetak laporan ke PDF");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onCetakPdf();
			}
		});
		cetak.setParent(toolbar);

		MyToolbarbuttonConfig excel = new MyToolbarbuttonConfig("Excel", "/img/excel.png");
		excel.setTooltiptext("Download laporan ke Excel (xlsx)");
		excel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onDownloadExcel();
			}
		});
		excel.setParent(toolbar);

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
		String filter = tahap != null && tahap.getSelectedItem() != null
				? String.valueOf(tahap.getSelectedItem().getValue()) : "SEMUA";
		List<TraceRow> rows = loadData(q, filter);
		Summary summary = buildSummary(rows);
		lastRows = rows;
		lastSummary = summary;
		renderHeader(body);
		renderSummary(body, summary);
		renderGrid(body, rows);
	}

	private void renderHeader(Component parent) {
		Panel panel = createPanel(parent, "Laporan Proses Pengajuan Sarpras", null);
		Panelchildren c = firstChild(panel);
		c.appendChild(new Html("<div style='font-family:Arial,sans-serif;color:#334155;font-size:12px;line-height:1.55;'>"
				+ "Laporan ini menelusuri sejauh mana setiap Permintaan Pembelian (PR) berjalan pada alur pengadaan: "
				+ "<b>Permintaan &rarr; Uang Muka &rarr; PO &rarr; BAST &rarr; Terima Tagihan &rarr; Dibayar (DPC)</b>. "
				+ "Kolom No. UM, No. PO, No. BAST, No. Tagihan dan status pembayaran DPC memperlihatkan dokumen yang sudah terbit untuk barang/jasa tersebut."
				+ "</div>"));
	}

	private void renderSummary(Component parent, Summary s) {
		Panel panel = createPanel(parent, "Ringkasan Tahapan", "margin-top:10px;");
		Panelchildren c = firstChild(panel);
		String html = "<div style='font-family:Arial,sans-serif;'>"
				+ "<div style='display:flex;gap:10px;flex-wrap:wrap;'>"
				+ card("Total PR", String.valueOf(s.total), money(s.totalNilai) + " nilai pengajuan", "#1d4ed8")
				+ card("Baru Permintaan", String.valueOf(s.belumProses), "belum ada UM/PO", "#64748b")
				+ card("Sudah Uang Muka", String.valueOf(s.reachUm), "punya UM", "#b45309")
				+ card("Sudah PO", String.valueOf(s.reachPo), "PO terbit", "#4338ca")
				+ card("Sudah BAST", String.valueOf(s.reachBast), "barang diterima", "#0369a1")
				+ card("Sudah Tagihan", String.valueOf(s.reachTagihan), "terima tagihan", "#0f766e")
				+ card("Sudah Dibayar", String.valueOf(s.reachDibayar), "lunas di DPC", "#15803d")
				+ "</div></div>";
		c.appendChild(new Html(html));
	}

	private void renderGrid(Component parent, List<TraceRow> rowsData) {
		Panel panel = createPanel(parent, "Tabel Proses Pengajuan", "margin-top:10px;");
		Panelchildren c = firstChild(panel);
		c.appendChild(new Html("<div style='font-size:12px;color:#64748b;line-height:1.5;margin-bottom:8px;'>"
				+ "Tanda \"-\" berarti dokumen pada tahap tersebut belum ada. Kolom <b>Status</b> menunjukkan tahap terjauh yang sudah dicapai."
				+ "</div>"));

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
		new MyColumnConfig("No. PKS").setParent(columns);
		new MyColumnConfig("No. PR").setParent(columns);
		new MyColumnConfig("Anggaran").setParent(columns);
		new MyColumnConfig("Satuan Kerja").setParent(columns);
		MyColumnConfig cNilai = new MyColumnConfig("Nilai Pengajuan");
		cNilai.setAlign("right");
		cNilai.setParent(columns);
		new MyColumnConfig("Item Pemesanan").setParent(columns);
		new MyColumnConfig("Jenis").setParent(columns);
		new MyColumnConfig("No. UM").setParent(columns);
		MyColumnConfig cUm = new MyColumnConfig("Nilai UM");
		cUm.setAlign("right");
		cUm.setParent(columns);
		new MyColumnConfig("No. PO").setParent(columns);
		MyColumnConfig cPo = new MyColumnConfig("Nilai PO");
		cPo.setAlign("right");
		cPo.setParent(columns);
		new MyColumnConfig("No. BAST").setParent(columns);
		new MyColumnConfig("No. Tagihan").setParent(columns);
		MyColumnConfig cLunas = new MyColumnConfig("Telah Dibayar");
		cLunas.setAlign("right");
		cLunas.setParent(columns);
		MyColumnConfig cBelumLunas = new MyColumnConfig("Belum Dibayar");
		cBelumLunas.setAlign("right");
		cBelumLunas.setParent(columns);
		new MyColumnConfig("Status").setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);
		if (rowsData.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			Html kosong = new Html("<div style='padding:10px;color:#64748b;'>Tidak ada data pada filter ini.</div>");
			kosong.setParent(row);
			return;
		}
		for (TraceRow data : rowsData) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			new Label(String.valueOf(data.no)).setParent(row);
			new Label(dash(data.pksKode)).setParent(row);
			Label pr = new Label(dash(data.prKode));
			pr.setStyle("font-weight:bold;color:#0f172a;");
			pr.setParent(row);
			Html anggaran = new Html("<div style='white-space:normal;font-size:12px;color:#334155;'>"
					+ html(dash(data.anggaran)) + "</div>");
			anggaran.setParent(row);
			Html satker = new Html("<div style='white-space:normal;font-size:12px;color:#334155;'>"
					+ html(dash(data.satuanKerja)) + "</div>");
			satker.setParent(row);
			new Label(money(data.nilaiPengajuan)).setParent(row);
			Html jenisPemesanan = new Html("<div style='white-space:normal;font-size:12px;color:#334155;'>"
					+ html(dash(data.jenisPemesanan)) + "</div>");
			jenisPemesanan.setParent(row);
			new Label(dash(data.jenis)).setParent(row);
			new Label(dash(data.umKode)).setParent(row);
			new Label(data.hasUm ? money(data.umNilai) : "-").setParent(row);
			new Label(dash(data.poKode)).setParent(row);
			new Label(data.hasPo ? money(data.poNilai) : "-").setParent(row);
			new Label(dash(data.bastKode)).setParent(row);
			new Label(dash(data.tagihanKode)).setParent(row);
			Label lblLunas = new Label(money(data.nominalLunas));
			lblLunas.setStyle("color:#166534;font-weight:bold;");
			lblLunas.setParent(row);
			Label lblBelum = new Label(money(data.nominalBelumLunas));
			lblBelum.setStyle("color:#b91c1c;");
			lblBelum.setParent(row);
			Html status = new Html(buildStatusBadge(data));
			status.setParent(row);
		}
	}

	private String stageLabel(int stage) {
		switch (stage) {
		case STAGE_DIBAYAR:
			return "Dibayar (DPC)";
		case STAGE_TAGIHAN:
			return "Terima Tagihan";
		case STAGE_BAST:
			return "BAST / Diterima";
		case STAGE_PO:
			return "PO Terbit";
		case STAGE_UM:
			return "Uang Muka";
		default:
			return "Baru Permintaan";
		}
	}

	private String stageColor(int stage) {
		switch (stage) {
		case STAGE_DIBAYAR:
			return "#15803d";
		case STAGE_TAGIHAN:
			return "#0f766e";
		case STAGE_BAST:
			return "#0369a1";
		case STAGE_PO:
			return "#4338ca";
		case STAGE_UM:
			return "#b45309";
		default:
			return "#64748b";
		}
	}

	private String buildStatusBadge(TraceRow data) {
		String label = stageLabel(data.stage);
		String bg = stageColor(data.stage);
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:Arial,sans-serif;'>");
		sb.append("<span style='display:inline-block;padding:2px 8px;border-radius:999px;background:")
				.append(bg).append(";color:#fff;font-size:11px;font-weight:700;white-space:nowrap;'>")
				.append(html(label)).append("</span>");
		if (data.paid && data.dpcKode != null && data.dpcKode.length() > 0) {
			sb.append("<div style='font-size:10px;color:#15803d;margin-top:3px;'>").append(html(data.dpcKode))
					.append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	// ==================== Cetak PDF (Laporan Proses Pengajuan Sarpras) ====================

	/**
	 * Membangun file PDF (A4 landscape) dari data yang sedang ditampilkan lalu memicu unduhan.
	 * Isi PDF mengikuti {@code lastRows}/{@code lastSummary} agar identik dengan tabel di layar.
	 */
	private void onCetakPdf() {
		try {
			// Segarkan dulu tabel sesuai filter/keyword terkini agar isi PDF identik dengan yang tampil.
			reload();
			String namaFile = "Laporan_Proses_Pengajuan_Sarpras_"
					+ new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(ais.ui.util.WaktuUtil.getDate()) + ".pdf";
			File file = new File(Common.REAL_PATH + "/tmp/" + namaFile);
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			buildPdf(file, lastRows, lastSummary);
			Filedownload.save(file, "application/pdf");
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void buildPdf(File file, List<TraceRow> rows, Summary s) throws Exception {
		Document document = new Document(PageSize.A4.rotate(), 24, 24, 30, 24);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			PdfWriter.getInstance(document, fos);
			document.open();

			Font titleFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD);
			Font subFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(90, 90, 90));
			Font headFont = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, BaseColor.WHITE);
			Font bodyFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL);

			Paragraph title = new Paragraph("LAPORAN PROSES PENGAJUAN SARPRAS", titleFont);
			title.setAlignment(Element.ALIGN_CENTER);
			document.add(title);

			Paragraph sub = new Paragraph(
					"Alur: Permintaan → Uang Muka → PO → BAST → Terima Tagihan → Dibayar (DPC)   |   "
							+ "Total PR: " + s.total + "   |   Nilai Pengajuan: " + money(s.totalNilai) + "   |   Dicetak: "
							+ new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm").format(ais.ui.util.WaktuUtil.getDate()),
					subFont);
			sub.setAlignment(Element.ALIGN_CENTER);
			sub.setSpacingAfter(8f);
			document.add(sub);

			String[] headers = { "No", "No. PKS", "No. PR", "Anggaran", "Satuan Kerja", "Nilai Pengajuan",
					"Item Pemesanan", "Jenis", "No. UM", "Nilai UM", "No. PO", "Nilai PO", "No. BAST", "No. Tagihan",
					"Telah Dibayar", "Belum Dibayar", "Status" };
			float[] widths = { 2.3f, 6f, 7.5f, 10f, 9f, 7.5f, 12f, 6.5f, 7.5f, 7f, 7.5f, 7f, 7.5f, 7.5f, 7.5f, 7.5f, 7f };

			PdfPTable table = new PdfPTable(headers.length);
			table.setWidthPercentage(100);
			table.setWidths(widths);
			table.setHeaderRows(1);

			BaseColor headerBg = new BaseColor(37, 99, 235);
			for (int i = 0; i < headers.length; i++) {
				PdfPCell cell = new PdfPCell(new Phrase(headers[i], headFont));
				cell.setBackgroundColor(headerBg);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				cell.setPadding(4);
				table.addCell(cell);
			}

			BaseColor zebra = new BaseColor(241, 245, 249);
			if (rows == null || rows.isEmpty()) {
				PdfPCell kosong = new PdfPCell(new Phrase("Tidak ada data pada filter ini.", bodyFont));
				kosong.setColspan(headers.length);
				kosong.setHorizontalAlignment(Element.ALIGN_CENTER);
				kosong.setPadding(8);
				table.addCell(kosong);
			} else {
				int idx = 0;
				for (TraceRow r : rows) {
					boolean shade = (idx++ % 2) == 1;
					addCell(table, String.valueOf(r.no), bodyFont, Element.ALIGN_CENTER, shade, zebra);
					addCell(table, dash(r.pksKode), bodyFont, Element.ALIGN_LEFT, shade, zebra);
					addCell(table, dash(r.prKode), bodyFont, Element.ALIGN_LEFT, shade, zebra);
					addCell(table, dash(r.anggaran), bodyFont, Element.ALIGN_LEFT, shade, zebra);
					addCell(table, dash(r.satuanKerja), bodyFont, Element.ALIGN_LEFT, shade, zebra);
					addCell(table, money(r.nilaiPengajuan), bodyFont, Element.ALIGN_RIGHT, shade, zebra);
					addCell(table, dash(r.jenisPemesanan), bodyFont, Element.ALIGN_LEFT, shade, zebra);
					addCell(table, dash(r.jenis), bodyFont, Element.ALIGN_LEFT, shade, zebra);
					addCell(table, dash(r.umKode), bodyFont, Element.ALIGN_LEFT, shade, zebra);
					addCell(table, r.hasUm ? money(r.umNilai) : "-", bodyFont, Element.ALIGN_RIGHT, shade, zebra);
					addCell(table, dash(r.poKode), bodyFont, Element.ALIGN_LEFT, shade, zebra);
					addCell(table, r.hasPo ? money(r.poNilai) : "-", bodyFont, Element.ALIGN_RIGHT, shade, zebra);
					addCell(table, dash(r.bastKode), bodyFont, Element.ALIGN_LEFT, shade, zebra);
					addCell(table, dash(r.tagihanKode), bodyFont, Element.ALIGN_LEFT, shade, zebra);
					addCell(table, money(r.nominalLunas), bodyFont, Element.ALIGN_RIGHT, shade, zebra);
					addCell(table, money(r.nominalBelumLunas), bodyFont, Element.ALIGN_RIGHT, shade, zebra);
					String status = stageLabel(r.stage)
							+ (r.paid && r.dpcKode != null && r.dpcKode.length() > 0 ? " (" + r.dpcKode + ")" : "");
					addCell(table, status, bodyFont, Element.ALIGN_LEFT, shade, zebra);
				}
			}
			document.add(table);
		} finally {
			try {
				document.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/TraceStatusPengadaanAssetDashboard.java:528");
			}
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/TraceStatusPengadaanAssetDashboard.java:534");
			}
		}
	}

	private void addCell(PdfPTable table, String text, Font font, int align, boolean shade, BaseColor shadeColor) {
		PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
		cell.setHorizontalAlignment(align);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setPadding(3);
		if (shade) {
			cell.setBackgroundColor(shadeColor);
		}
		table.addCell(cell);
	}

	// ==================== Download Excel (xlsx) ====================

	/**
	 * Membangun file Excel (xlsx) dari data yang sedang ditampilkan lalu memicu unduhan.
	 * Kolom nilai ditulis sebagai angka agar mudah dijumlahkan di Excel.
	 */
	private void onDownloadExcel() {
		try {
			// Segarkan dulu tabel sesuai filter/keyword terkini agar isi Excel identik dengan yang tampil.
			reload();
			String namaFile = "Laporan_Proses_Pengajuan_Sarpras_"
					+ new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(ais.ui.util.WaktuUtil.getDate()) + ".xlsx";
			File file = new File(Common.REAL_PATH + "/tmp/" + namaFile);
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			buildExcel(file, lastRows);
			Filedownload.save(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void buildExcel(File file, List<TraceRow> rows) throws Exception {
		XSSFWorkbook workbook = new XSSFWorkbook();
		FileOutputStream fos = null;
		try {
			XSSFCellStyle headStyle = UIUtil.solid_LIGHT_GRAY(workbook);
			XSSFCellStyle bodyStyle = UIUtil.solid_WHITE(workbook);

			XSSFSheet sheet = workbook.createSheet("Proses Pengajuan Sarpras");
			sheet.setDefaultColumnWidth(18);

			String[] headers = { "No", "No. PKS", "No. PR", "Anggaran", "Satuan Kerja", "Nilai Pengajuan",
					"Item Pemesanan", "Jenis", "No. UM", "Nilai UM", "No. PO", "Nilai PO", "No. BAST", "No. Tagihan",
					"Telah Dibayar", "Belum Dibayar", "Status" };
			XSSFRow head = sheet.createRow(0);
			for (int i = 0; i < headers.length; i++) {
				XSSFCell cell = head.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headStyle);
			}

			int r = 1;
			if (rows != null) {
				for (TraceRow row : rows) {
					XSSFRow xr = sheet.createRow(r++);
					setNum(xr, 0, row.no, bodyStyle);
					setStr(xr, 1, dash(row.pksKode), bodyStyle);
					setStr(xr, 2, dash(row.prKode), bodyStyle);
					setStr(xr, 3, dash(row.anggaran), bodyStyle);
					setStr(xr, 4, dash(row.satuanKerja), bodyStyle);
					setNum(xr, 5, row.nilaiPengajuan, bodyStyle);
					setStr(xr, 6, dash(row.jenisPemesanan), bodyStyle);
					setStr(xr, 7, dash(row.jenis), bodyStyle);
					setStr(xr, 8, dash(row.umKode), bodyStyle);
					if (row.hasUm) {
						setNum(xr, 9, row.umNilai, bodyStyle);
					} else {
						setStr(xr, 9, "-", bodyStyle);
					}
					setStr(xr, 10, dash(row.poKode), bodyStyle);
					if (row.hasPo) {
						setNum(xr, 11, row.poNilai, bodyStyle);
					} else {
						setStr(xr, 11, "-", bodyStyle);
					}
					setStr(xr, 12, dash(row.bastKode), bodyStyle);
					setStr(xr, 13, dash(row.tagihanKode), bodyStyle);
					setNum(xr, 14, row.nominalLunas, bodyStyle);
					setNum(xr, 15, row.nominalBelumLunas, bodyStyle);
					String status = stageLabel(row.stage)
							+ (row.paid && row.dpcKode != null && row.dpcKode.length() > 0 ? " (" + row.dpcKode + ")" : "");
					setStr(xr, 16, status, bodyStyle);
				}
			}

			for (int i = 0; i < headers.length; i++) {
				try {
					sheet.autoSizeColumn(i);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/TraceStatusPengadaanAssetDashboard.java:630");
				}
			}

			fos = new FileOutputStream(file);
			workbook.write(fos);
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/TraceStatusPengadaanAssetDashboard.java:641");
			}
		}
	}

	private void setStr(XSSFRow row, int col, String value, XSSFCellStyle style) {
		XSSFCell cell = row.createCell(col);
		cell.setCellValue(value == null ? "" : value);
		cell.setCellStyle(style);
	}

	private void setNum(XSSFRow row, int col, double value, XSSFCellStyle style) {
		XSSFCell cell = row.createCell(col);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}

	@SuppressWarnings("unchecked")
	private List<TraceRow> loadData(String keyword, String filter) {
		Session session = null;
		List<TraceRow> rows = new ArrayList<TraceRow>();
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			Criteria criteria = session.createCriteria(PermintaanPengadaanMasterAsset.class);
			if (keyword != null && keyword.trim().length() > 0) {
				Disjunction disjunction = Restrictions.disjunction();
				disjunction.add(Restrictions.ilike("kode", keyword.trim(), MatchMode.ANYWHERE));
				disjunction.add(Restrictions.ilike("keterangan", keyword.trim(), MatchMode.ANYWHERE));
				criteria.add(disjunction);
			}
			// ---- Filter rentang tanggal (berdasarkan tanggal pembuatan PR) ----
			Date dMulai = (tglMulai == null) ? null : tglMulai.getValue();
			Date dSampai = (tglSampai == null) ? null : tglSampai.getValue();
			if (dMulai != null) {
				java.util.Calendar cal = java.util.Calendar.getInstance();
				cal.setTime(dMulai);
				cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
				cal.set(java.util.Calendar.MINUTE, 0);
				cal.set(java.util.Calendar.SECOND, 0);
				cal.set(java.util.Calendar.MILLISECOND, 0);
				criteria.add(Restrictions.ge("tanggalPembuatan", cal.getTime()));
			}
			if (dSampai != null) {
				java.util.Calendar cal = java.util.Calendar.getInstance();
				cal.setTime(dSampai);
				cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
				cal.set(java.util.Calendar.MINUTE, 59);
				cal.set(java.util.Calendar.SECOND, 59);
				cal.set(java.util.Calendar.MILLISECOND, 999);
				criteria.add(Restrictions.le("tanggalPembuatan", cal.getTime()));
			}
			criteria.addOrder(Order.desc("id"));
			criteria.setMaxResults(DEFAULT_LIMIT);
			List<PermintaanPengadaanMasterAsset> prs = criteria.list();
			if (prs.isEmpty()) {
				return rows;
			}

			// ---- Muat detail PR secara batch (item, uang muka, PKS) ----
			Map<Long, List<PermintaanPengadaanMasterAssetDetail>> detailByPr =
					new LinkedHashMap<Long, List<PermintaanPengadaanMasterAssetDetail>>();
			List<PermintaanPengadaanMasterAssetDetail> allDetails = session
					.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.in("permintaanPengadaanMasterAsset", prs)).addOrder(Order.asc("id")).list();
			for (PermintaanPengadaanMasterAssetDetail d : allDetails) {
				Long prId = idOfParentPr(d);
				if (prId == null) {
					continue;
				}
				List<PermintaanPengadaanMasterAssetDetail> list = detailByPr.get(prId);
				if (list == null) {
					list = new ArrayList<PermintaanPengadaanMasterAssetDetail>();
					detailByPr.put(prId, list);
				}
				list.add(d);
			}

			// ---- Kumpulkan PO & UM dari PR untuk lookup BAST secara batch ----
			Map<Long, PemesananPengadaanMasterAsset> poById = new LinkedHashMap<Long, PemesananPengadaanMasterAsset>();
			Map<Long, UangMuka> umById = new LinkedHashMap<Long, UangMuka>();
			for (PermintaanPengadaanMasterAsset pr : prs) {
				PemesananPengadaanMasterAsset po = safePo(pr);
				if (po != null && po.getId() != null) {
					poById.put(po.getId(), po);
				}
				List<PermintaanPengadaanMasterAssetDetail> details = detailByPr.get(pr.getId());
				if (details != null) {
					for (PermintaanPengadaanMasterAssetDetail d : details) {
						UangMuka um = safeUm(d);
						if (um != null && um.getId() != null) {
							umById.put(um.getId(), um);
						}
					}
				}
			}

			// ---- BAST terbaru per PO dan per UM (satu query masing-masing) ----
			Map<Long, PenerimaanPengadaanMasterAsset> bastByPo = new LinkedHashMap<Long, PenerimaanPengadaanMasterAsset>();
			if (!poById.isEmpty()) {
				List<PenerimaanPengadaanMasterAsset> basts = session
						.createCriteria(PenerimaanPengadaanMasterAsset.class)
						.add(Restrictions.in("pemesananPengadaanMasterAsset", poById.values()))
						.addOrder(Order.desc("id")).list();
				for (PenerimaanPengadaanMasterAsset b : basts) {
					Long poId = idOfPoOfBast(b);
					if (poId != null && !bastByPo.containsKey(poId)) {
						bastByPo.put(poId, b);
					}
				}
			}
			Map<Long, PenerimaanPengadaanMasterAsset> bastByUm = new LinkedHashMap<Long, PenerimaanPengadaanMasterAsset>();
			if (!umById.isEmpty()) {
				List<PenerimaanPengadaanMasterAsset> basts = session
						.createCriteria(PenerimaanPengadaanMasterAsset.class)
						.add(Restrictions.in("uangMuka", umById.values())).addOrder(Order.desc("id")).list();
				for (PenerimaanPengadaanMasterAsset b : basts) {
					Long umId = idOfUmOfBast(b);
					if (umId != null && !bastByUm.containsKey(umId)) {
						bastByUm.put(umId, b);
					}
				}
			}

			int no = 0;
			for (PermintaanPengadaanMasterAsset pr : prs) {
				TraceRow row = buildRow(pr, detailByPr.get(pr.getId()), bastByPo, bastByUm);
				if (!matchFilter(row, filter)) {
					continue;
				}
				row.no = ++no;
				rows.add(row);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
		return rows;
	}

	private TraceRow buildRow(PermintaanPengadaanMasterAsset pr,
			List<PermintaanPengadaanMasterAssetDetail> details,
			Map<Long, PenerimaanPengadaanMasterAsset> bastByPo,
			Map<Long, PenerimaanPengadaanMasterAsset> bastByUm) {
		TraceRow row = new TraceRow();
		row.prKode = safe(pr.getKode());
		row.nilaiPengajuan = nz(pr.getNilai());
		row.anggaran = namaWorkspace(pr);
		row.satuanKerja = namaSatuanKerja(pr);
		try {
			row.tanggal = pr.getTanggalPembuatan();
		} catch (Exception e) {
			row.tanggal = null;
		}

		// ---- Item PR (Item Pemesanan) + Jenis Aset + Uang Muka + PKS dari detail ----
		StringBuilder items = new StringBuilder();
		java.util.LinkedHashSet<String> jenisSet = new java.util.LinkedHashSet<String>();
		UangMuka um = null;
		PerjanjianKerjasamaMasterAsset pks = null;
		if (details != null) {
			for (PermintaanPengadaanMasterAssetDetail d : details) {
				MasterAsset ma = d.getMasterAsset();
				String nama = namaAsset(ma);
				if (nama.length() > 0) {
					if (items.length() > 0) {
						items.append(", ");
					}
					items.append("(").append(qty(d.getJumlah())).append(") ").append(nama);
				}
				String jenisNama = namaJenisAsset(ma);
				if (jenisNama.length() > 0) {
					jenisSet.add(jenisNama);
				}
				if (um == null) {
					um = safeUm(d);
				}
				if (pks == null) {
					pks = safePks(d);
				}
			}
		}
		row.jenisPemesanan = items.toString();
		row.jenis = join(jenisSet, ", ");

		// ---- Uang Muka (UM) ----
		if (um != null) {
			row.hasUm = true;
			row.umKode = safe(getKodeQuietly(um));
			row.umNilai = nz(getNilaiUmQuietly(um));
		}

		// ---- PO ----
		PemesananPengadaanMasterAsset po = safePo(pr);
		if (po != null) {
			row.hasPo = true;
			row.poKode = safe(getKodeQuietly(po));
			row.poNilai = nz(getNilaiPoQuietly(po));
		}

		// ---- BAST (lewat PO, atau lewat UM bila PO tidak ada) ----
		PenerimaanPengadaanMasterAsset bast = null;
		if (po != null && po.getId() != null) {
			bast = bastByPo.get(po.getId());
		}
		if (bast == null && um != null && um.getId() != null) {
			bast = bastByUm.get(um.getId());
		}
		if (bast != null) {
			row.hasBast = true;
			row.bastKode = safe(getKodeQuietly(bast));
		}

		// ---- Terima Tagihan (SaldoAwalMasterAsset) ----
		SaldoAwalMasterAsset tagihan = null;
		if (bast != null) {
			try {
				tagihan = bast.getSaldoAwalMasterAsset();
			} catch (Exception e) {
				tagihan = null;
			}
		}
		if (tagihan != null) {
			row.hasTagihan = true;
			row.tagihanKode = safe(kodeTagihan(tagihan));
			row.tagihanNilai = nz(getNilaiTagihanQuietly(tagihan));
		}

		// ---- PKS (dari detail PR; fallback ke PKS pada PO) ----
		// Banyak alur pengadaan menyimpan Perjanjian Kerjasama di level PO
		// (PemesananPengadaanMasterAsset.perjanjianKerjasamaMasterAsset), bukan di detail PR,
		// sehingga tanpa fallback ini kolom No. PKS bisa tampak kosong padahal PKS-nya ada.
		if (pks == null) {
			pks = safePksFromPo(po);
		}
		if (pks != null) {
			row.pksKode = safe(getKodeQuietly(pks));
		}

		// ---- Sudah dibayar di DPC? (via UM atau via Tagihan) ----
		ProsesTransfer pt = prosesTransferRealisasi(um);
		if (pt == null) {
			pt = prosesTransferRealisasi(tagihan);
		}
		if (pt != null) {
			row.paid = true;
			row.dpcKode = safe(getKodeQuietly(pt));
		}

		// Nominal lunas / belum lunas dibayarkan. Dasar nilai: nilai tagihan bila tagihan sudah terbit,
		// jika belum pakai nilai pengajuan. Sudah dibayar (DPC) = lunas; selain itu = belum lunas.
		double nilaiDasarBayar = row.hasTagihan ? row.tagihanNilai : row.nilaiPengajuan;
		row.nominalLunas = row.paid ? nilaiDasarBayar : 0.0;
		row.nominalBelumLunas = row.paid ? 0.0 : nilaiDasarBayar;

		row.stage = computeStage(row);
		return row;
	}

	private int computeStage(TraceRow row) {
		int stage = STAGE_PERMINTAAN;
		if (row.hasUm) {
			stage = Math.max(stage, STAGE_UM);
		}
		if (row.hasPo) {
			stage = Math.max(stage, STAGE_PO);
		}
		if (row.hasBast) {
			stage = Math.max(stage, STAGE_BAST);
		}
		if (row.hasTagihan) {
			stage = Math.max(stage, STAGE_TAGIHAN);
		}
		if (row.paid) {
			stage = STAGE_DIBAYAR;
		}
		return stage;
	}

	private boolean matchFilter(TraceRow row, String filter) {
		if (filter == null || "SEMUA".equalsIgnoreCase(filter)) {
			return true;
		}
		if ("PERMINTAAN".equalsIgnoreCase(filter)) {
			return !row.hasUm && !row.hasPo && !row.hasBast && !row.hasTagihan && !row.paid;
		}
		if ("UM".equalsIgnoreCase(filter)) {
			return row.hasUm;
		}
		if ("PO".equalsIgnoreCase(filter)) {
			return row.hasPo;
		}
		if ("BAST".equalsIgnoreCase(filter)) {
			return row.hasBast;
		}
		if ("TAGIHAN".equalsIgnoreCase(filter)) {
			return row.hasTagihan;
		}
		if ("DIBAYAR".equalsIgnoreCase(filter)) {
			return row.paid;
		}
		return true;
	}

	private Summary buildSummary(List<TraceRow> rows) {
		Summary s = new Summary();
		if (rows == null) {
			return s;
		}
		for (TraceRow row : rows) {
			if (row == null) {
				continue;
			}
			s.total++;
			s.totalNilai += row.nilaiPengajuan;
			if (row.hasUm) {
				s.reachUm++;
			}
			if (row.hasPo) {
				s.reachPo++;
			}
			if (row.hasBast) {
				s.reachBast++;
			}
			if (row.hasTagihan) {
				s.reachTagihan++;
			}
			if (row.paid) {
				s.reachDibayar++;
			}
			if (!row.hasUm && !row.hasPo && !row.hasBast && !row.hasTagihan && !row.paid) {
				s.belumProses++;
			}
		}
		return s;
	}

	// ==================== Navigasi relasi yang aman (null-safe) ====================

	private Long idOfParentPr(PermintaanPengadaanMasterAssetDetail d) {
		try {
			PermintaanPengadaanMasterAsset pr = d.getPermintaanPengadaanMasterAsset();
			return pr == null ? null : pr.getId();
		} catch (Exception e) {
			return null;
		}
	}

	private Long idOfPoOfBast(PenerimaanPengadaanMasterAsset b) {
		try {
			PemesananPengadaanMasterAsset po = b.getPemesananPengadaanMasterAsset();
			return po == null ? null : po.getId();
		} catch (Exception e) {
			return null;
		}
	}

	private Long idOfUmOfBast(PenerimaanPengadaanMasterAsset b) {
		try {
			UangMuka um = b.getUangMuka();
			return um == null ? null : um.getId();
		} catch (Exception e) {
			return null;
		}
	}

	private PemesananPengadaanMasterAsset safePo(PermintaanPengadaanMasterAsset pr) {
		try {
			return pr == null ? null : pr.getPemesananPengadaanMasterAsset();
		} catch (Exception e) {
			return null;
		}
	}

	private UangMuka safeUm(PermintaanPengadaanMasterAssetDetail d) {
		try {
			return d == null ? null : d.getUangMuka();
		} catch (Exception e) {
			return null;
		}
	}

	private PerjanjianKerjasamaMasterAsset safePks(PermintaanPengadaanMasterAssetDetail d) {
		try {
			return d == null ? null : d.getPerjanjianKerjasamaMasterAsset();
		} catch (Exception e) {
			return null;
		}
	}

	private PerjanjianKerjasamaMasterAsset safePksFromPo(PemesananPengadaanMasterAsset po) {
		try {
			return po == null ? null : po.getPerjanjianKerjasamaMasterAsset();
		} catch (Exception e) {
			return null;
		}
	}

	private ProsesTransfer prosesTransferRealisasi(UangMuka um) {
		if (um == null) {
			return null;
		}
		try {
			return prosesTransferRealisasi(um.getDaftarPengajuanTransfer());
		} catch (Exception e) {
			return null;
		}
	}

	private ProsesTransfer prosesTransferRealisasi(SaldoAwalMasterAsset tagihan) {
		if (tagihan == null) {
			return null;
		}
		try {
			return prosesTransferRealisasi(tagihan.getDaftarPengajuanTransfer());
		} catch (Exception e) {
			return null;
		}
	}

	private ProsesTransfer prosesTransferRealisasi(DaftarPengajuanTransfer dpt) {
		if (dpt == null) {
			return null;
		}
		try {
			ProsesTransfer pt = dpt.getProsesTransfer();
			if (pt != null && pt.getRealisasikanOleh() != null) {
				return pt;
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	private String getKodeQuietly(Object entity) {
		if (entity == null) {
			return "";
		}
		try {
			if (entity instanceof UangMuka) {
				return ((UangMuka) entity).getKode();
			}
			if (entity instanceof PemesananPengadaanMasterAsset) {
				return ((PemesananPengadaanMasterAsset) entity).getKode();
			}
			if (entity instanceof PenerimaanPengadaanMasterAsset) {
				return ((PenerimaanPengadaanMasterAsset) entity).getKode();
			}
			if (entity instanceof PerjanjianKerjasamaMasterAsset) {
				return ((PerjanjianKerjasamaMasterAsset) entity).getKode();
			}
			if (entity instanceof ProsesTransfer) {
				return ((ProsesTransfer) entity).getKode();
			}
		} catch (Exception e) {
			return "";
		}
		return "";
	}

	private String kodeTagihan(SaldoAwalMasterAsset tagihan) {
		try {
			String k = tagihan.getKodeTagihan();
			if (k == null || k.trim().length() == 0) {
				k = tagihan.getKode();
			}
			return k;
		} catch (Exception e) {
			return "";
		}
	}

	private Double getNilaiUmQuietly(UangMuka um) {
		try {
			return um.getNilai();
		} catch (Exception e) {
			return 0.0;
		}
	}

	private Double getNilaiPoQuietly(PemesananPengadaanMasterAsset po) {
		try {
			return po.getNilai();
		} catch (Exception e) {
			return 0.0;
		}
	}

	private Double getNilaiTagihanQuietly(SaldoAwalMasterAsset tagihan) {
		try {
			return tagihan.getNilai();
		} catch (Exception e) {
			return 0.0;
		}
	}

	private String namaAsset(MasterAsset asset) {
		try {
			if (asset == null) {
				return "";
			}
			String nama = asset.getNama();
			return nama == null ? "" : nama.trim();
		} catch (Exception e) {
			return "";
		}
	}

	/** Jenis aset item (mis. "Inventari"/"Consumable") dari MasterAsset.getJenisAsset(); null-safe. */
	private String namaJenisAsset(MasterAsset asset) {
		try {
			if (asset == null) {
				return "";
			}
			ais.database.model.asset.JenisAsset jenis = asset.getJenisAsset();
			return jenis == null ? "" : safe(jenis.getNama());
		} catch (Exception e) {
			return "";
		}
	}

	private String join(java.util.Collection<String> values, String sep) {
		if (values == null || values.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (String v : values) {
			if (v == null || v.trim().length() == 0) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(sep);
			}
			sb.append(v.trim());
		}
		return sb.toString();
	}

	/** Anggaran = Workspace yang dipakai PR, ditampilkan sebagai "kode - nama" (null-safe). */
	private String namaWorkspace(PermintaanPengadaanMasterAsset pr) {
		try {
			ais.database.model.rab.Workspace w = pr.getWorkspace();
			if (w == null) {
				return "";
			}
			String kode = safe(w.getKode());
			String nama = safe(w.getNama());
			if (kode.length() == 0) {
				return nama;
			}
			if (nama.length() == 0) {
				return kode;
			}
			return kode + " - " + nama;
		} catch (Exception e) {
			return "";
		}
	}

	/** Satuan Kerja yang dipakai PR (memakai nama, fallback ke kode); null-safe. */
	private String namaSatuanKerja(PermintaanPengadaanMasterAsset pr) {
		try {
			ais.database.model.rab.SatuanKerja sk = pr.getSatuanKerja();
			if (sk == null) {
				return "";
			}
			String nama = safe(sk.getNama());
			if (nama.length() > 0) {
				return nama;
			}
			return safe(sk.getKode());
		} catch (Exception e) {
			return "";
		}
	}

	// ==================== Util tampilan ====================

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

	private String qty(Double value) {
		double v = value == null ? 1.0 : value.doubleValue();
		if (v == Math.floor(v) && !Double.isInfinite(v)) {
			return String.valueOf((long) v);
		}
		return number(v);
	}

	private double nz(Double value) {
		return value == null ? 0.0 : value.doubleValue();
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

	private void closeSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/TraceStatusPengadaanAssetDashboard.java:1293");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/TraceStatusPengadaanAssetDashboard.java:1297");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/TraceStatusPengadaanAssetDashboard.java:1303");
		}
	}
}
