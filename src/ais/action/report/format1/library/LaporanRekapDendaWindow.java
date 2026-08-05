package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
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

import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapDendaWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private MyDatebox start = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
	private MyDatebox end = new MyDatebox(ais.ui.util.WaktuUtil.getDate());

	private Center center = new Center();

	public LaporanRekapDendaWindow() {
		super();
		try {
			perpustakaan = new AmbilDataPerpustakaanBanbox();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Denda Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapDendaWindow(String title, String border, boolean closable) {
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

		MyTabConfig tab1 = new MyTabConfig("Rekap Denda Anggota Per-Tanggal");
		tab1.setParent(tabs);

		MyTabConfig tab12 = new MyTabConfig("Rekap Denda Item Per-Tanggal");
		tab12.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Data Denda Anggota");
		tab2.setParent(tabs);

		MyTabConfig tab21 = new MyTabConfig("Data Denda Per Item");
		tab21.setParent(tabs);

		MyTabConfig tab22 = new MyTabConfig("Denda Belum Lunas");
		tab22.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		final Tabpanel tabpanel12 = new ais.ui.util.MyTabpanel();
		tabpanel12.setParent(tabpanels);
		tab12.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel12.getChildren().size() == 0) {
					LaporanRekapDendaPerItemWindow laporanKHS = new LaporanRekapDendaPerItemWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel12);
				}
			}
		});

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					LaporanDendaWindow laporanKHS = new LaporanDendaWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel2);
				}
			}
		});

		final Tabpanel tabpanel21 = new ais.ui.util.MyTabpanel();
		tabpanel21.setParent(tabpanels);
		tab21.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel21.getChildren().size() == 0) {
					LaporanDendaItemWindow laporanKHS = new LaporanDendaItemWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel21);
				}
			}
		});

		final Tabpanel tabpanel22 = new ais.ui.util.MyTabpanel();
		tabpanel22.setParent(tabpanels);
		tab22.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel22.getChildren().size() == 0) {
					LaporanDendaBelumLunasWindow laporanKHS = new LaporanDendaBelumLunasWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel22);
				}
			}
		});

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan);

		perpustakaan.setWidth("90%");
		perpustakaan.setWidth("90%");

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "REKAP_DENDA_ANGGOTA.xlsx");
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

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		Perpustakaan perpustakaan = (Perpustakaan) (this.perpustakaan.getAttribute("perpustakaan"));

		

		String sql = "select  to_char(c1.tanggal, 'DD-MM-YYYY') as tanggal,    "
				+ "max(d.usernama || ' (' || d.userid || ')') as userid,    "
				+ "count(distinct(e.anggota)) as jumlah,    sum(c1.denda) as denda,    sum(c1.dibayarsejumlah) as dibayarsejumlah   "
				+ "from library.kembali_pengadaan_item c     "
				+ "inner join library.kembali_pengadaan_item_detail c1 on (c1.kembali_pengadaan_item=c.id)  "
				+ "inner join tbmuser d on (d.userid = c.disetujui_oleh)   "
				+ "inner join library.peminjaman_pengadaan_item e on (c.peminjaman_pengadaan_item=e.id)  "
				+ "where 1=1     "
				+ (perpustakaan == null ? " " : " and c.perpustakaan = " + perpustakaan.getId() + " ")
				+ "and date(c1.tanggal) between date('" + Common.databaseDateFormat.get().format(start.getValue()) + "')    "
				+ "and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')   "
				+ "group by d.userid,to_char(c1.tanggal, 'DD-MM-YYYY')    "
				+ "order by max(c.tanggal_pembuatan),d.userid";

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");

		spreadsheet.setMaxcolumns(8);
		spreadsheet.setMaxrows(jurusans.size() + 7);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);
		} catch (Exception e4) { ais.common.ErrorAuditUtil.record(e4, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapDendaWindow.java:288");

		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI DENDA ANGGOTA\n " + (perpustakaan == null ? "SEMUA PERPUSTAKAAN"
						: "PERPUSTAKAAN " + perpustakaan.getNama().toUpperCase()));

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
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Di-input oleh");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Jumlah Anggota");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Tagihan Denda");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Denda Dibayar");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Jumlah Anggota\nPer Tanggal");
		cell = Utils.getCell(sheet, rowIndex, 5);
		cell.getCellStyle().setWrapText(true);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Tagihan Denda\nPer Tanggal");
		cell = Utils.getCell(sheet, rowIndex, 6);
		cell.getCellStyle().setWrapText(true);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "Denda Dibayar\nPer Tanggal");
		cell = Utils.getCell(sheet, rowIndex, 7);
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

		Double jumlahTotalDibayar = 0.0;
		Double jumlahTotalPertanggalDibayar = 0.0;

		Integer jumlah = 0;
		Integer jumlahPertanggal = 0;
		for (Object[] objects : jurusans) {
			if (!tanggal.equals(objects[0].toString())) {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
				tanggal = objects[0].toString();

				if (rowIndex != 3) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 5, jumlahPertanggal);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 6, jumlahTotalPertanggal);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 7, jumlahTotalPertanggalDibayar);
				}

				jumlahTotalPertanggalDibayar = 0.0;
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
			Double dibayarsejumlah = new Double(objects[4] == null ? "0.0" : objects[4].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, total);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, dibayarsejumlah);
			jumlahTotal += total;
			jumlahTotalPertanggal += total;

			jumlahTotalDibayar += dibayarsejumlah;
			jumlahTotalPertanggalDibayar += dibayarsejumlah;
			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapDendaWindow.java:375");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Denda Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});

			}

			rowIndex++;
		}

		if (rowIndex != 3) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 5, jumlahPertanggal);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 6, jumlahTotalPertanggal);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 7, jumlahTotalPertanggalDibayar);
		}

		colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, jumlah);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, jumlahTotal);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, jumlahTotalDibayar);
		try {
			ais.ui.util.EcampusUtil.setBorder(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 2, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e3) { ais.common.ErrorAuditUtil.record(e3, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapDendaWindow.java:403");

		}

		sql = "select  " + "max(d.usernama || ' (' || d.userid || ')') as userid,    "
				+ "count(distinct(e.anggota)) as jumlah,    sum(c1.denda) as denda,sum(c1.dibayarsejumlah) as dibayarsejumlah    "
				+ "from library.kembali_pengadaan_item c     "
				+ "inner join library.kembali_pengadaan_item_detail c1 on (c1.kembali_pengadaan_item=c.id)  "
				+ "inner join tbmuser d on (d.userid = c.disetujui_oleh)   "
				+ "inner join library.peminjaman_pengadaan_item e on (c.peminjaman_pengadaan_item=e.id)  "
				+ "where 1=1     "
				+ (perpustakaan == null ? " " : " and c.perpustakaan = " + perpustakaan.getId() + " ")
				+ "and c1.tanggal between ('" + Common.databaseDateFormat.get().format(start.getValue()) + "')    "
				+ "and ('" + Common.databaseDateFormat.get().format(end.getValue()) + "')   "
				+ "group by d.userid    " + "order by d.userid";

		System.out.println(sql);
		Session session = HibernateUtil.currentSession();
		jurusans = Common.ambilSql(sql);

		rowIndex += 3;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "REKAP TOTAL");
		try {
			ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, 0, rowIndex, spreadsheet.getMaxcolumns() - 1, false);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					true);
		} catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapDendaWindow.java:429");

		}
		++rowIndex;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Di-input oleh");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Jumlah Anggota");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Tagihan Denda");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Denda Dibayar");
		try {
			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, 3, rowIndex), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					true);
		} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapDendaWindow.java:442");
		}
		rowIndex++;
		jumlahTotal = 0.0;
		jumlahTotalDibayar = 0.0;
		jumlah = 0;
		for (Object[] objects : jurusans) {

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0] == null ? "" : objects[0].toString());
			Integer c = new Integer(objects[1] == null ? "0" : objects[1].toString());
			Double total = new Double(objects[2] == null ? "0.0" : objects[2].toString());
			Double dibayarsejumlah = new Double(objects[3] == null ? "0.0" : objects[3].toString());
			jumlahTotal += total;
			jumlahTotalDibayar += dibayarsejumlah;
			jumlah += c;
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, c);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, total);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, dibayarsejumlah);
			try {
				ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, 3, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapDendaWindow.java:463");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Denda Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
			rowIndex++;
		}
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, jumlah);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, jumlahTotal);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, jumlahTotalDibayar);
		try {
			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, 3, rowIndex), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
			Common.setStyled(sheet);spreadsheet.setMaxrows(rowIndex + 2);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapDendaWindow.java:475");
			// Common.tampilErrorJikaAdmin(e);
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
