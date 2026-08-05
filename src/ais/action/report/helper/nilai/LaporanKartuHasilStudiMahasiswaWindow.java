package ais.action.report.helper.nilai;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Range;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanKartuHasilStudiMahasiswaWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private AmbilDataMahasiswaBanbox ambilDataMahasiswaBanbox = new AmbilDataMahasiswaBanbox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private Combobox semesterAbsensi = new Combobox();

	public LaporanKartuHasilStudiMahasiswaWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kartu Hasil Studi Mahasiswa Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		try {
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kartu Hasil Studi Mahasiswa Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKartuHasilStudiMahasiswaWindow(String title, String border, boolean closable) {
		super(title, border, closable);

		init();
		try {
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kartu Hasil Studi Mahasiswa Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings("deprecation")
	private void init() {

		for (int i = 1; i <= 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterAbsensi.appendChild(comboitem);
		}

		setClosable(true);
		setTitle("Kartu Hasil Strudi");
		setWidth("90%");
		setHeight("90%");
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("200px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("70%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Mahasiswa"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(ambilDataMahasiswaBanbox);
		ambilDataMahasiswaBanbox.setWidth("90%");
		

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Semester"));
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");

		
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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "kartu_hasil_studi_mahasiswa.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		Mahasiswa mahasiswa = (Mahasiswa) ambilDataMahasiswaBanbox.getAttribute("myValue");
		Integer semester = (Integer) (semesterAbsensi.getSelectedItem() == null || semesterAbsensi.getSelectedItem().getValue() == null ? null
				: semesterAbsensi.getSelectedItem().getValue());
		if (mahasiswa == null || semester == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();

		ProjectionList projectionList = Projections.projectionList();
		projectionList.add(Projections.property("perkuliahan"));
		projectionList.add(Projections.property("totalIP"));
		projectionList.add(Projections.property("matakuliahKonversi"));

		List<Object[]> matakuliahs = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.isNull("ikutiPerkuliahan")).add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.eq("semester", semester))
				.addOrder(Order.desc("totalNilai")).setProjection(projectionList).add(Restrictions
						.or(Restrictions.isNotNull("perkuliahan"), Restrictions.isNotNull("matakuliahKonversi")))
				.list();

		if (matakuliahs == null || matakuliahs.size() == 0) {
			return;
		}

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(7);
		spreadsheet.setMaxrows(matakuliahs.size() + 20);
		final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1, "KARTU HASIL STUDI");
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Utils.setRowHeight(sheet, 1, 70);
		Cell cell = Utils.getCell(sheet, 1, 1);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 3, 1, "Peringkat");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 4, 1, "Nama Mahasiswa");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 5, 1, "Nama Stambuk");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 6, 1, "Jurusan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 7, 1, "Tingkat");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 8, 1, "Semester");

		ais.ui.util.EcampusUtil.mergeCells(sheet, 3, 1, 3, 2, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 4, 1, 4, 2, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 5, 1, 5, 2, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 6, 1, 6, 2, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 7, 1, 7, 2, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 8, 1, 8, 2, false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 3, 3, ": ");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 4, 3, ": " + mahasiswa.getNama());
		ais.ui.util.EcampusUtil.setCellValue(sheet, 5, 3, ": ");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 6, 3, ": " + mahasiswa.getJurusan().getNama());
		ais.ui.util.EcampusUtil.setCellValue(sheet, 7, 3, ": ");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 8, 3, ": " + semester);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 3, 3, 3, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 4, 3, 4, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 5, 3, 5, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 6, 3, 6, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 7, 3, 7, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, 8, 3, 8, spreadsheet.getMaxcolumns() - 1, false);

		Utils.setAlignment(sheet, new Rect(1, 3, spreadsheet.getMaxcolumns() - 1, 8), CellStyle.ALIGN_LEFT);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 10, 1, "No");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 10, 2, "Kode");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 10, 3, Common.getBahasaConfig("Matakuliah"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, 10, 4, "Bobot (B)");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 10, 5, "Nilai (N)");
		ais.ui.util.EcampusUtil.setCellValue(sheet, 10, 6, "B x N");
		// Styles.setFontBoldWeight(sheet, row, col, boldWeight)

		Utils.setColumnWidth(sheet, 3, 500);
		Utils.setColumnWidth(sheet, 2, 100);
		Utils.setColumnWidth(sheet, 1, 40);
		Utils.setColumnWidth(sheet, 0, 0);
		Utils.setRowHeight(sheet, 10, 70);

		int rowIndex = 11;
		int colIndex = 2;
		List<Long> matkul = new ArrayList<Long>();
		int sks = 0;
		int index = 1;
		double total = 0.0;
		for (Object[] objs : matakuliahs) {
			Perkuliahan perkuliahan = (Perkuliahan) objs[0];
			Matakuliah matakuliah = (Matakuliah) (perkuliahan.getMatakuliah() == null ? objs[2]
					: perkuliahan.getMatakuliah());
			if (matakuliah == null)
				continue;
			if (matkul.contains(matakuliah.getId()))
				continue;
			Number nilai = (Number) objs[1];
			// spreadsheet.setRowfreeze(rowIndex);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, matakuliah.getId());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, index);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, matakuliah.getKode());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, matakuliah.getNama().toUpperCase());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, matakuliah.getSks());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, nilai.doubleValue());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 4,
					nilai.doubleValue() * matakuliah.getSks().doubleValue());
			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					BookHelper.BORDER_FULL, BorderStyle.THIN, color);
			sks += matakuliah.getSks();
			total += nilai.doubleValue();
			matkul.add(matakuliah.getId());
			rowIndex++;
			index++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 3, 1, "Jumlah Matakuliah");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 4, 1, "Bobot seluruh matakuliah");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 5, 1, "Indek Prestasi");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 6, 1, "Predikat");

		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex + 3, 1, rowIndex + 3, 2, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex + 4, 1, rowIndex + 4, 2, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex + 5, 1, rowIndex + 5, 2, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex + 6, 1, rowIndex + 6, 2, false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 3, 3, ": " + matkul.size());
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 4, 3, ": " + sks);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 5, 3, ": " + (total / (double) sks));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 6, 3, ": " + mahasiswa.getJurusan().getNama());

		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex + 3, 3, rowIndex + 3, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex + 4, 3, rowIndex + 4, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex + 5, 3, rowIndex + 5, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex + 6, 3, rowIndex + 6, spreadsheet.getMaxcolumns() - 1, false);

		Utils.setAlignment(sheet, new Rect(1, 10, 3, spreadsheet.getMaxrows() - 1), CellStyle.ALIGN_LEFT);

		Utils.setAlignment(sheet, new Rect(4, 10, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1),
				CellStyle.ALIGN_CENTER);

		Utils.setAlignment(sheet,
				new Rect(1, rowIndex + 3, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1),
				CellStyle.ALIGN_LEFT);

		Range range = Utils.getRange(sheet, 10, 1, 10, spreadsheet.getMaxcolumns() - 1);
		// cell = Utils.getCell(sheet, 1, 1);
		// CellStyle cellStyle = cell.getCellStyle();
		// cellStyle.setWrapText(true);
		// cellStyle.setAlignment(CellStyle.ALIGN_CENTER);

		// range.setStyle(cellStyle);
		range.setBorders(BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		// range.

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
