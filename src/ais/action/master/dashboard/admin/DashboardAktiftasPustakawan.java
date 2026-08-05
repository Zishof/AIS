package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardAktiftasPustakawan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private MyDatebox mulai = new MyDatebox();
	private MyDatebox sampai = new MyDatebox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	public DashboardAktiftasPustakawan() {
		super();
		try {
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardAktiftasPustakawan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Vbox div = new Vbox();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("border:0px;background: transparent;");

		Hbox toolbar = new Hbox();
		toolbar.setStyle("border:0px;background: transparent;");
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
						"Rekap_status_mahasiswa.xlsx");
			}
		});
		print.setParent(toolbar);

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("15%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("15%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("15%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		Calendar dateMulai = ais.ui.util.WaktuUtil.getCalendar();
		dateMulai.set(Calendar.YEAR, dateMulai.get(Calendar.YEAR) - 1);
		mulai.setValue(dateMulai.getTime());
		row.appendChild(mulai);
		mulai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		sampai.setValue(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(sampai);
		sampai.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// South south = new South();
		// ais.ui.util.ZkCompat.setFlex(south, true);
		// south.setParent(borderlayout);

		initSpreadsheet();

	}

	@SuppressWarnings({ "unchecked" })
	private void initSpreadsheet() {

		Common.clear(center);

		String dateMulai = mulai.getValue() == null ? Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())
				: Common.databaseDateFormat.get().format(mulai.getValue());
		Calendar tglSampai = ais.ui.util.WaktuUtil.getCalendar();
		tglSampai.setTime(sampai.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : sampai.getValue());
		tglSampai.set(Calendar.DATE, tglSampai.get(Calendar.DATE) + 1);
		String dateSelesai = Common.databaseDateFormat.get().format(tglSampai.getTime());

		if (dateMulai == null || dateSelesai == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();

		String sql = "select cc.nama as perpustakaan,aa.oleh, bb.usernama, (case when a.qty is null then 0 else a.qty end) + (case when b.qty is null then 0 else b.qty end) as input_baru, "
				+ " (case when c.qty is null then 0 else c.qty end) as copy_item, "
				+ " (case when d.qty is null then 0 else d.qty end) as input_anggota, "
				+ " (case when e.qty is null then 0 else e.qty end) as peminjaman, "
				+ " (case when f.qty is null then 0 else f.qty end) as kembali " +

				" from ( " + " 	select oleh,perpustakaan " + " 	from( "
				+ " 		select dibuat_oleh as oleh,perpustakaan from library.saldo_awal " + " 		union all  "
				+ " 		select dibuat_oleh as oleh,perpustakaan from library.batch_item_punya_barcode "
				+ " 		union all  " + " 		select dibuat_oleh as oleh,perpustakaan from library.anggota "
				+ " 		union all  "
				+ " 		select dibuat_oleh as oleh,perpustakaan from library.peminjaman_pengadaan_item "
				+ " 		union all  "
				+ " 		select dibuat_oleh as oleh,perpustakaan from library.kembali_pengadaan_item " + " 	) a "
				+ " 	where oleh is not null and oleh != 'external_update' " + " 	and perpustakaan is not null "
				+ " 	group by oleh,perpustakaan " + " ) as aa " + " inner join tbmuser bb on (aa.oleh = bb.userid) "
				+ " inner join library.perpustakaan cc on (aa.perpustakaan = cc.id) " + " left join ( "
				+ " 	select a.dibuat_oleh as oleh, count(*) as qty, perpustakaan from library.saldo_awal a  "
				+ " 	inner join library.saldo_awal_detail b on (a.id = b.saldo_awal) "
				+ " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
				+ " 	group by a.dibuat_oleh,perpustakaan "
				+ " ) a on (a.oleh = aa.oleh and a.perpustakaan = aa.perpustakaan) " + " left join ( "
				+ " 	select a.dibuat_oleh as oleh, count(*) as qty, perpustakaan from library.terima_pengadaan_item a  "
				+ " 	inner join library.terima_pengadaan_item_detail b on (a.id = b.terima_pengadaan_item) "
				+ " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
				+ " 	group by a.dibuat_oleh, perpustakaan "
				+ " ) b on (b.oleh = aa.oleh and b.perpustakaan = aa.perpustakaan) " + " left join ( "
				+ " 	select a.dibuat_oleh as oleh, count(*) as qty, b.perpustakaan from library.batch_item_punya_barcode a  "
				+ " 	inner join library.item_punya_barcode b on (a.id = b.batch_item_punya_barcode) "
				+ " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
				+ " 	group by a.dibuat_oleh, b.perpustakaan "
				+ " ) c on (c.oleh = aa.oleh and c.perpustakaan = aa.perpustakaan) " + " left join ( "
				+ " 	select a.dibuat_oleh as oleh, count(*) as qty, perpustakaan from library.anggota a  "
				+ " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
				+ " 	group by a.dibuat_oleh, perpustakaan "
				+ " ) d on (d.oleh = aa.oleh and d.perpustakaan = aa.perpustakaan) " + " left join ( "
				+ " 	select a.dibuat_oleh as oleh, count(*) as qty, perpustakaan from library.peminjaman_pengadaan_item a  "
				+ " 	inner join library.peminjaman_pengadaan_item_detail b on (a.id = b.peminjaman_pengadaan_item) "
				+ " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
				+ " 	group by a.dibuat_oleh, perpustakaan "
				+ " ) e on (e.oleh = aa.oleh and e.perpustakaan = aa.perpustakaan) " + " left join ( "
				+ " 	select a.dibuat_oleh as oleh, count(*) as qty, perpustakaan from library.kembali_pengadaan_item a  "
				+ " 	inner join library.kembali_pengadaan_item_detail b on (a.id = b.kembali_pengadaan_item) "
				+ " 	where a.tanggal_dirubah between date('" + dateMulai + "') and date('" + dateSelesai + "') "
				+ " 	group by a.dibuat_oleh, perpustakaan "
				+ " ) f on (f.oleh = aa.oleh and f.perpustakaan = aa.perpustakaan)";

		List<Object[]> data = session.createSQLQuery(sql).list();

		if (data.size() == 0) {
			return;
		}

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(8);
		spreadsheet.setMaxrows(data.size() + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"AKTIFITAS PUSTAKAWAN \n " + "" + "DARI TANGGAL " + dateMulai + " S.D " + dateSelesai);
		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		Utils.setRowHeight(sheet, 1, 150);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Perpustakaan");
		Utils.setColumnWidth(sheet, 0, 150);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Login Pustakawan");
		Utils.setColumnWidth(sheet, 1, 150);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Nama Pustakawan");
		Utils.setColumnWidth(sheet, 2, 150);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Input Item Baru");
		Utils.setColumnWidth(sheet, 3, 120);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Input Eksemplar");
		Utils.setColumnWidth(sheet, 4, 120);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Input Anggota");
		Utils.setColumnWidth(sheet, 5, 120);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Input Peminjaman");
		Utils.setColumnWidth(sheet, 6, 120);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "Input Pengembalian");
		Utils.setColumnWidth(sheet, 7, 120);

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		for (Object[] objects : data) {
			if (objects == null)
				continue;

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0] == null ? "" : objects[0].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[1] == null ? "" : objects[1].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, objects[2] == null ? "" : objects[2].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, objects[3] == null ? "" : objects[3].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, objects[4] == null ? "" : objects[4].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, objects[5] == null ? "" : objects[5].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, objects[6] == null ? "" : objects[6].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, objects[7] == null ? "" : objects[7].toString());

			rowIndex++;
		}

		Common.setStyled(sheet);
		spreadsheet.setMaxrows(rowIndex + 1);
		// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
