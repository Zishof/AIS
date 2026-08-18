package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
import org.zkoss.zul.Toolbar;

import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.KembaliPengadaanItem;
import ais.database.model.library.KembaliPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanDendaItemWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private MyDatebox start = new MyDatebox();
	private MyDatebox end = new MyDatebox(ais.ui.util.WaktuUtil.getDate());

	private Center center = new Center();

	public LaporanDendaItemWindow() {
		super();
		try {
			perpustakaan = new AmbilDataPerpustakaanBanbox();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Denda Item Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDendaItemWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		// setTitle("Rekap Pembayaran Host to Host");
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

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

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) - 1);
		if (start != null) start.setValue(calendar.getTime());
		if (start != null) start.setReadonly(true);

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "DATA_DENDA_ITEM.xlsx");
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

		Session session = HibernateUtil.currentSession();

		List<KembaliPengadaanItemDetail> kembaliPengadaanItemDetails = session
				.createCriteria(KembaliPengadaanItemDetail.class).add(Restrictions.gt("denda", 0.01))

				.add(start.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction(
								"date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(start.getValue())
										+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')"))

				.createAlias("kembaliPengadaanItem", "kembaliPengadaanItem")

				.add(perpustakaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kembaliPengadaanItem.perpustakaan", perpustakaan))

				.add(Restrictions.isNotNull("kembaliPengadaanItem.disetujuiOleh"))

				.setMaxResults(1048576).addOrder(Order.desc("kembaliPengadaanItem.id")).addOrder(Order.desc("id")).list();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		if (kembaliPengadaanItemDetails.size() == 0) {
			return;
		}
		spreadsheet.setMaxcolumns(11);
		spreadsheet.setMaxrows(kembaliPengadaanItemDetails.size() + 5);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);
		} catch (Exception e4) { ais.common.ErrorAuditUtil.record(e4, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanDendaItemWindow.java:200");

		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"DATA DENDA ANGGOTA PER ITEM\n " + (perpustakaan == null ? "SEMUA PERPUSTAKAAN"
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
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Perpustakaan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Anggota");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Prodi");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Waktu Dipinjam");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Harus Kembali (hari)");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Waktu Kembali");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Item / Buku");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "Denda");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "Dibayar");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "Kode Pengembalian");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "Di-input oleh");

		Utils.setRowHeight(sheet, rowIndex, 50);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		Double dendaTotal = 0.0;
		Double dibayarTotal = 0.0;
		for (KembaliPengadaanItemDetail kembaliPengadaanItemDetail : kembaliPengadaanItemDetails) {
			dendaTotal += kembaliPengadaanItemDetail.getDenda();
			dibayarTotal += kembaliPengadaanItemDetail.getDibayarSejumlah();
			KembaliPengadaanItem kembaliPengadaanItem = kembaliPengadaanItemDetail.getKembaliPengadaanItem();
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, kembaliPengadaanItem.getPerpustakaan().getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
					kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().toString());
			
			String jurusan = "";
			if (kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getMahasiswa() != null
					&& kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getMahasiswa()
							.getJurusan() != null) {
				jurusan = kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getMahasiswa().getJurusan()
						.getNama();
			} else if (kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getDosen() != null
					&& kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getDosen().getJurusan() != null) {
				jurusan = kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getDosen().getJurusan()
						.getNama();
			} else if (kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getSiswa() != null
					&& kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getSiswa().getSekolah() != null) {
				jurusan = kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getSiswa().getSekolah()
						.getNama();
			} else if (kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getGuru() != null
					&& kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getGuru().getSekolah() != null) {
				jurusan = kembaliPengadaanItem.getPeminjamanPengadaanItem().getAnggota().getGuru().getSekolah()
						.getNama();
			} 
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, jurusan);

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3,
					kembaliPengadaanItem.getPeminjamanPengadaanItem() == null ? ""
							: Common.dateFormat5.get()
									.format(kembaliPengadaanItem.getPeminjamanPengadaanItem().getTanggalPembuatan()));

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4,
					kembaliPengadaanItem.getPeminjamanPengadaanItem() == null ? ""
							: Common.numberFormat.get()
									.format(kembaliPengadaanItem.getPeminjamanPengadaanItem().getJumlahHariBatas()));

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5,
					kembaliPengadaanItemDetail.getTanggal() == null ? ""
							: Common.dateFormat5.get().format(kembaliPengadaanItemDetail.getTanggal()));

			String s = kembaliPengadaanItemDetail.getItemPunyaBarcode() == null
					? kembaliPengadaanItemDetail.getItem().getNama()
					: kembaliPengadaanItemDetail.getItemPunyaBarcode().getBarcode() + "-"
							+ kembaliPengadaanItemDetail.getItem().getNama();

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, s);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, kembaliPengadaanItemDetail.getDenda());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, kembaliPengadaanItemDetail.getDibayarSejumlah());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, kembaliPengadaanItem.getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10,
					kembaliPengadaanItem.getDisetujuiOleh().getUserNama() + " ("
							+ kembaliPengadaanItem.getDisetujuiOleh().getUserId() + ")");

			rowIndex++;
		}

		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Denda Total");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, dendaTotal);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, dibayarTotal);

		Utils.setColumnWidth(sheet, 0, 130);
		Utils.setColumnWidth(sheet, 1, 250);
		Utils.setColumnWidth(sheet, 2, 180);
		Utils.setColumnWidth(sheet, 3, 180);
		Utils.setColumnWidth(sheet, 4, 150);
		Utils.setColumnWidth(sheet, 5, 180);
		Utils.setColumnWidth(sheet, 6, 250);
		Utils.setColumnWidth(sheet, 7, 70);
		Utils.setColumnWidth(sheet, 8, 70);
		Utils.setColumnWidth(sheet, 9, 130);
		Utils.setColumnWidth(sheet, 10, 130);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
