package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapStatusMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Label angkatan = new Label();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public DashboardRekapStatusMahasiswa() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapStatusMahasiswa(String title, String border, boolean closable) {
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
		/* FIX 21-08-2026: tinggi panel filter kurang 52px sehingga baris toolbar
		 * (Proses/Download) terpotong di bagian bawah. Ditambah satu tinggi baris
		 * toolbar ZK. Autoscroll tetap aktif sebagai pengaman bila isi filter
		 * bertambah di kemudian hari. */
		north.setHeight("292px");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		Hbox hbox = new Hbox();
		row.appendChild(searchsemester);
		row.appendChild(hbox);
		hbox.appendChild(searchsemester);
		hbox.appendChild(angkatan);
		angkatan.setValue("(tahun angkatan : semua)");

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);
				angkatan.setValue("(tahun angkatan : semua)");
				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
				if (genap) {
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				}
			}
		};
		semesterAbsensi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);

			}
		});

		eventListener.onEvent(null);

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
				Filedownload.save(bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
						"Rekap_status_mahasiswa.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	private void initSpreadsheet() {

		Common.clear(center);
		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
		String semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? Perkuliahan.GANJIL
						: this.semesterAbsensi.getSelectedItem().getValue());

		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
				: searchsemester.getSelectedItem().getValue());

		String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		if (tahunAkademik == null) {
			return;
		}
		Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
		Boolean semuasemester = searchsemester.getSelectedItem() == null;
		if (!semuasemester) {
			angkatan.setValue("(tahun angkatan : " + tahunAngkatan + ")");
		}

		List<Object[]> jurusans = new ArrayList<Object[]>();

		angkatan.setValue("(tahun angkatan : " + (semuasemester ? "semua" : tahunAngkatan) + ")");

		String sql = "select " + "max(c.nama) as fakultas, " + "max(b.nama) as jurusan, "
				+ "sum(case a.status when 1 then 1 else 0 end) as aktif, "
				+ "sum(case a.status when 2 then 1 else 0 end) as cuti, "
				+ "sum(case a.status when 3 then 1 else 0 end) as lulus, "
				+ "sum(case a.status when 4 then 1 else 0 end) as drop_out, "
				+ "sum(case a.status when 5 then 1 else 0 end) as tidak_aktif, " + "count(a.id) as total "
				+ "from mahasiswa a " + "left join jurusan b on (a.jurusan = b.id  ) "
				+ "left join fakultas c on (c.id = b.fakultas)  " + " where 1=1  and a.tahunangkatan = (case "
				+ semuasemester + " when true then a.tahunangkatan else " + tahunAngkatan + " end) "
				+ (program == null ? "" : " and a.program = '" + program + "'")
				+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
				+ (fakultas == null ? "" : " and b.fakultas = " + fakultas.getId()) + " group by b.fakultas,a.jurusan "
				+ " order by max(c.nama), max(b.nama) ";

		System.out.println(sql);
		jurusans = Common.ambilSql(sql);

		// Center hanya boleh 1 anak → bungkus dalam Vlayout: panel CHART (donut) di atas,
		// Spreadsheet (tabel + sumber Download Excel) tetap di bawah.
		org.zkoss.zul.Vlayout centerBox = new org.zkoss.zul.Vlayout();
		centerBox.setWidth("100%");
		centerBox.setHeight("100%");
		centerBox.setStyle("overflow:auto;");
		centerBox.setParent(center);

		// Placeholder chart; diisi setelah total per status selesai dihitung di bawah.
		org.zkoss.zul.Html chartPanel = new org.zkoss.zul.Html();
		chartPanel.setParent(centerBox);

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(centerBox);
		spreadsheet.setWidth("100%");
		spreadsheet.setVflex("1");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(8);
		spreadsheet.setMaxrows(jurusans.size() + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI STATUS MAHASISWA \n " + "" + Common.getBahasaConfig("Fakultas") + " "
						+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
						+ Common.getBahasaConfig("Jurusan") + " "
						+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()) + "\n TAHUN AKADEMIK "
						+ tahunAkademik + "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase())
						+ "\n SEMESTER " + semester.toUpperCase() + "\n ANGKATAN "
						+ (semuasemester ? "SEMUA" : tahunAngkatan));
		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		Utils.setRowHeight(sheet, 1, 150);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Fakultas");
		Utils.setColumnWidth(sheet, 0, 200);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Jurusan");
		Utils.setColumnWidth(sheet, 1, 200);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Aktif");
		Utils.setColumnWidth(sheet, 2, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Cuti");
		Utils.setColumnWidth(sheet, 3, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Lulus");
		Utils.setColumnWidth(sheet, 4, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Drop Out");
		Utils.setColumnWidth(sheet, 5, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Tidak Aktif");
		Utils.setColumnWidth(sheet, 6, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "Total");
		Utils.setColumnWidth(sheet, 7, 80);

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		String namaFakultas = "";
		String namaProdi = "";
		Integer totalAktif = 0;
		Integer totalCuti = 0;
		Integer totalLulus = 0;
		Integer totalDropOut = 0;
		Integer totalTidakAktif = 0;
		Integer totalSemua = 0;
		for (Object[] objects : jurusans) {
			if (objects[0] == null)
				continue;
			if (!namaFakultas.equals(objects[0].toString())) {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
				namaFakultas = objects[0].toString();
			} else {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
			}

			if (!namaProdi.equals(objects[1].toString())) {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[1].toString());
				namaProdi = objects[1].toString();
			} else {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "");
			}

			Integer nilai0 = ((Number) (objects[2] == null ? 0 : objects[2])).intValue();
			Integer nilai1 = ((Number) (objects[3] == null ? 0 : objects[3])).intValue();
			Integer nilai2 = ((Number) (objects[4] == null ? 0 : objects[4])).intValue();
			Integer nilai3 = ((Number) (objects[5] == null ? 0 : objects[5])).intValue();
			Integer nilai4 = ((Number) (objects[6] == null ? 0 : objects[6])).intValue();
			Integer nilai5 = ((Number) (objects[7] == null ? 0 : objects[7])).intValue();

			totalAktif += nilai0;
			totalCuti += nilai1;
			totalLulus += nilai2;
			totalDropOut += nilai3;
			totalTidakAktif += nilai4;
			totalSemua += nilai5;

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, nilai0);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, nilai1);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai2);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, nilai3);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, nilai4);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, nilai5);

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapStatusMahasiswa.java:430");
			}

			rowIndex++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, totalAktif);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, totalCuti);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, totalLulus);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, totalDropOut);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, totalTidakAktif);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, totalSemua);

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		Common.setStyled(sheet);
		spreadsheet.setMaxrows(rowIndex + 1);
		// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

		colIndex = 0;
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapStatusMahasiswa.java:459");
		}

		// ── Panel visual: DONUT komposisi status mahasiswa (HTML/CSS, BUKAN JFreeChart) ──
		// Memakai total yang sudah dihitung di loop di atas. Memberi gambaran cepat proporsi
		// status mahasiswa tanpa harus membaca seluruh tabel.
		try {
			String[] stLabels = { "Aktif", "Cuti", "Lulus", "Drop Out", "Tidak Aktif" };
			double[] stValues = { totalAktif, totalCuti, totalLulus, totalDropOut, totalTidakAktif };
			String[] stColors = { "#22c55e", "#f59e0b", "#2563eb", "#ef4444", "#64748b" };
			chartPanel.setContent("<div style='padding:10px 12px 2px;'>"
					+ ais.ui.util.HtmlChartHelper.donut(
							"Komposisi Status Mahasiswa",
							"Melihat sekilas berapa banyak mahasiswa yang aktif, cuti, sudah lulus, keluar "
									+ "(drop out), dan tidak aktif, sehingga proporsinya mudah dibandingkan.",
							stLabels, stValues, stColors, totalSemua + " mhs")
					+ "</div>");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapStatusMahasiswa.java:476");
			// chart hanya pelengkap; kegagalan menggambar tidak boleh mengganggu tabel/Excel.
		}

	}
}
