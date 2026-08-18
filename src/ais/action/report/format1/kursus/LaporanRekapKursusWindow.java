package ais.action.report.format1.kursus;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.List;

import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapKursusWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private MyDatebox start = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
	private MyDatebox end = new MyDatebox(ais.ui.util.WaktuUtil.getDate());

	private Center center = new Center();

	public LaporanRekapKursusWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Kursus Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapKursusWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("Rekap Pendapatan");
		tab1.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		// setTitle("Rekap Pembayaran Host to Host");
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		row.appendChild(start);
		start.setWidth("90%");
		if (start != null) start.setReadonly(true);
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) - 1);
		if (start != null) start.setValue(calendar.getTime());

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		row.appendChild(end);
		end.setWidth("90%");
		if (end != null) end.setReadonly(true);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});
		print.setParent(toolbar);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "REKAP_PENDAPATAN.xlsx");
			}
		});
		print.setParent(toolbar);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
	}

	// private void

	private void initSpreadsheet() throws Exception {
		Common.clear(center);

		String sql = "select  to_char(c.waktubeli, 'DD-MM-YYYY') as tanggal,    " + "max(d.nama) as userid,    "
				+ "count(distinct(c1.peserta_kursus)) as jumlah,    sum(e.hargatotal) as  hargatotal  "
				+ "from peserta_punya_produk_kursus c     "
				+ "inner join produk_peserta c1 on (c1.peserta_punya_produk_kursus=c.id)  "
				+ "inner join peserta_kursus d on (d.id = c1.peserta_kursus)   "
				+ "inner join produk_kursus e on (e.id = c1.produk_kursus)   " + "where 1=1     "
				+ "and date(c.waktubeli) between date('" + Common.databaseDateFormat.get().format(start.getValue())
				+ "')    " + "and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')   "
				+ "group by d.id,to_char(c.waktubeli, 'DD-MM-YYYY')    " + "order by max(c.waktubeli),d.id";

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");

		spreadsheet.setMaxcolumns(7);
		spreadsheet.setMaxrows(jurusans.size() + 7);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);
		} catch (Exception e4) { ais.common.ErrorAuditUtil.record(e4, "auto-audit(empty-catch) src/ais/action/report/format1/kursus/LaporanRekapKursusWindow.java:197");

		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, "REKAPITULASI PEMBALIAN");

		Utils.setRowHeight(sheet, 1, 120);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Tanggal");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Dibeli oleh");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Jumlah Barang");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Tagihan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Jumlah Anggota\nPer Tanggal");
		cell = Utils.getCell(sheet, rowIndex, 4);
		cell.getCellStyle().setWrapText(true);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Tagihan Denda\nPer Tanggal");
		cell = Utils.getCell(sheet, rowIndex, 5);
		cell.getCellStyle().setWrapText(true);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Denda Dibayar\nPer Tanggal");
		cell = Utils.getCell(sheet, rowIndex, 6);
		cell.getCellStyle().setWrapText(true);

		Utils.setRowHeight(sheet, rowIndex, 50);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		String tanggal = "";
		Double jumlahTotal = 0.0;
		Double jumlahTotalPertanggal = 0.0;

//		Double jumlahTotalDibayar = 0.0;
//		Double jumlahTotalPertanggalDibayar = 0.0;

		Integer jumlah = 0;
		Integer jumlahPertanggal = 0;
		for (Object[] objects : jurusans) {
			if (!tanggal.equals(objects[0].toString())) {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
				tanggal = objects[0].toString();

				if (rowIndex != 3) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 4, jumlahPertanggal);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 5, jumlahTotalPertanggal);
//					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 7, jumlahTotalPertanggalDibayar);
				}

//				jumlahTotalPertanggalDibayar = 0.0;
				jumlahTotalPertanggal = 0.0;
				jumlahPertanggal = 0;
			} else {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
			}
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[1] == null ? "" : objects[1].toString());
			Integer c = new Integer(objects[2] == null ? "0" : objects[2].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, c);
			jumlah += c;
			jumlahPertanggal += c;
			Double total = new Double(objects[3] == null ? "0.0" : objects[3].toString());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, total);
//			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, dibayarsejumlah);
			jumlahTotal += total;
			jumlahTotalPertanggal += total;

//			jumlahTotalDibayar += dibayarsejumlah;
//			jumlahTotalPertanggalDibayar += dibayarsejumlah;
			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/kursus/LaporanRekapKursusWindow.java:281");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Kursus Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});

			}

			rowIndex++;
		}

		if (rowIndex != 3) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 4, jumlahPertanggal);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 5, jumlahTotalPertanggal);
//			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 7, jumlahTotalPertanggalDibayar);
		}

		colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, jumlah);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, jumlahTotal);
//		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, jumlahTotalDibayar);
		try {
			ais.ui.util.EcampusUtil.setBorder(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 2, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e3) { ais.common.ErrorAuditUtil.record(e3, "auto-audit(empty-catch) src/ais/action/report/format1/kursus/LaporanRekapKursusWindow.java:309");

		}

		Utils.setColumnWidth(sheet, 0, 130);
		Utils.setColumnWidth(sheet, 1, 150);
		Utils.setColumnWidth(sheet, 2, 150);
		Utils.setColumnWidth(sheet, 3, 150);
		Utils.setColumnWidth(sheet, 4, 150);
		Utils.setColumnWidth(sheet, 5, 150);
		Utils.setColumnWidth(sheet, 6, 150);
		Utils.setColumnWidth(sheet, 7, 150);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
