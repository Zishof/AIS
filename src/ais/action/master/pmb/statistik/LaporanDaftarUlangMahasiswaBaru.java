package ais.action.master.pmb.statistik;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanDaftarUlangMahasiswaBaru extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7781970414204679926L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchtahunAngkatan = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox searchjenjang = new Combobox();
	private Combobox searchjenisseleksi = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private List<BiodataCalonMahasiswa> biodataCalonMahasiswas = new ArrayList<BiodataCalonMahasiswa>();

	public LaporanDaftarUlangMahasiswaBaru() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public LaporanDaftarUlangMahasiswaBaru(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initFakultas() {
		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.generateTahunAngkatan(searchtahunAngkatan);
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	@SuppressWarnings("deprecation")
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
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("240px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		searchtahunAngkatan = Common.generateTahunAngkatan(searchtahunAngkatan);
		row.appendChild(searchtahunAngkatan);
		searchtahunAngkatan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("pilih_jenjang")));
		row.appendChild(searchjenjang);
		searchjenjang.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Jenis Seleksi"));
		Common.insertCombo(searchjenisseleksi, "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row.appendChild(searchjenisseleksi);
		searchjenisseleksi.setWidth("90%");

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
						"Laporan_daftar_ulang_mahasiswa_baru.xlsx");
			}
		});
		print.setParent(toolbar);
	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.clear(center);

		// if (searchfakultas.getSelectedItem() == null ||
		// searchfakultas.getSelectedItem().getValue() == null) {
		// return;
		// }
		//
		// if (searchjurusan.getSelectedItem() == null ||
		// searchjurusan.getSelectedItem().getValue() == null) {
		// return;
		// }
		//
		if (searchtahunAngkatan.getSelectedItem() == null) {
			return;
		}

		JenisSeleksi jenisSeleksi = (JenisSeleksi) (searchjenisseleksi.getSelectedItem() == null ? null
				: searchjenisseleksi.getSelectedItem().getValue());
		Integer tahunAngkatan = (Integer) (this.searchtahunAngkatan.getSelectedItem() == null ? null
				: this.searchtahunAngkatan.getSelectedItem().getValue());
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());
		Jenjang jenjang = (Jenjang) (searchjenjang.getSelectedItem() == null
				|| searchjenjang.getSelectedItem().getValue() == null ? null
						: searchjenjang.getSelectedItem().getValue());

		// String tahun = tahunAkademik.substring(0, 4);
		// this.angkatan.setValue(tahun);

		biodataCalonMahasiswas = null;
		Session session = HibernateUtil.currentSession();
		biodataCalonMahasiswas = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
				.add(Restrictions.isNotNull("prodiLulus")).add(Restrictions.isNotNull("nim"))
				// .add(Restrictions.eq("jenisSeleksi", jenisSeleksi))

				.add(searchtahunAngkatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahun", searchtahunAngkatan.getSelectedItem().getValue()))

				.add(searchjenisseleksi.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisSeleksi", searchjenisseleksi.getSelectedItem().getValue()))

				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("prodiLulus", searchjurusan.getSelectedItem().getValue()))
				.createCriteria("prodiLulus")
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.list();
		// System.out.println(biodataCalonMahasiswas.toString());
		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(9);
		spreadsheet.setMaxrows(biodataCalonMahasiswas.size() + 6);
		final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 4;
		int colIndex = 3;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "No.");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "No Ujian");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "NIM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Nama");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Jenis\nKelamin");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Jenis Seleksi");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "Fakultas");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "Jurusan");

		Utils.setColumnWidth(sheet, 8, 200);
		Utils.setColumnWidth(sheet, 7, 200);
		Utils.setColumnWidth(sheet, 6, 100);
		Utils.setColumnWidth(sheet, 5, 100);
		Utils.setColumnWidth(sheet, 4, 200);
		Utils.setColumnWidth(sheet, 3, 120);
		Utils.setColumnWidth(sheet, 2, 100);
		Utils.setColumnWidth(sheet, 1, 30);
		Utils.setColumnWidth(sheet, 0, 0);
		Utils.setRowHeight(sheet, 4, 50);
		Utils.setRowHeight(sheet, 2, 1);
		try {

			ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1,
					"DATA DAFTAR ULANG MAHASISWA BARU\n " + "" + Common.getBahasa("upper_jenjang") + " "
							+ (jenjang == null ? "SEMUA" : jenjang.getNama().toUpperCase()) + "\n"
							+ Common.getBahasaConfig("Fakultas") + " "
							+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
							+ Common.getBahasaConfig("Jurusan") + " "
							+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()) + "\n TAHUN AKADEMIK "
							+ tahunAngkatan + "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase())
							+ "\nJENIS SELEKSI "
							+ (jenisSeleksi == null ? "SEMUA" : jenisSeleksi.getNama().toUpperCase()));
			Utils.setRowHeight(sheet, 1, 150);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
			Cell cell = Utils.getCell(sheet, 1, 1);
			cell.getCellStyle().setWrapText(true);
			cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		try {
			ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					BookHelper.BORDER_FULL, BorderStyle.THIN, color);
			ais.ui.util.EcampusUtil.setBorder(sheet,
					new Rect(1, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/pmb/statistik/LaporanDaftarUlangMahasiswaBaru.java:338");
		}

		rowIndex = 6;
		colIndex = 2;
		int index = 1;
		for (BiodataCalonMahasiswa biodataCalonMahasiswa : biodataCalonMahasiswas) {

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, biodataCalonMahasiswa.getId());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, index);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, biodataCalonMahasiswa.getNoUjian());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, biodataCalonMahasiswa.getNim());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2,
					biodataCalonMahasiswa.getNama().toUpperCase());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3,
					biodataCalonMahasiswa.getJenisKelamin());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 4,
					biodataCalonMahasiswa.getJenisSeleksi().getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 5,
					biodataCalonMahasiswa.getProdiLulus().getFakultas().getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 6,
					biodataCalonMahasiswa.getProdiLulus().getNama());

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/statistik/LaporanDaftarUlangMahasiswaBaru.java:365");
			}
			index++;
			rowIndex++;
		}

		try {
			Range range = Utils.getRange(sheet, 4, 1, 4, spreadsheet.getMaxcolumns() - 1);
			Cell cell = Utils.getCell(sheet, 4, 2);
			CellStyle cellStyle = cell.getCellStyle();
			cellStyle.setWrapText(true);
			cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
			range.setStyle(cellStyle);
			// Utils.setAlignment(
			// sheet,
			// new Rect(1, 5, spreadsheet.getMaxcolumns() - 1, spreadsheet
			// .getMaxrows() - 1), CellStyle.ALIGN_LEFT);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
