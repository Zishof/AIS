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
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
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

public class DashboardRekapPerkuliahan extends MyWindow {

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
	private Div center;

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public DashboardRekapPerkuliahan() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapPerkuliahan(String title, String border, boolean closable) {
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

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Rekap Perkuliahan",
				"Rekap pelaksanaan perkuliahan per kelas, beserta grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(saringanHost);
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
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_perkuliahan.xlsx");
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

		List<Object[]> jurusans = new ArrayList<Object[]>();

		String sql = "select " + "max(y.nama) as fakultas, " + "max(x.nama) as jurusan, "
				+ "sum(case g.perkuliahan when g.perkuliahan then 0 else 1 end) belum_diambil, "
				+ "sum(case b.perkuliahan when b.perkuliahan then 1 else 0 end) jumlah_pengambil_krs_0_sd_10, "
				+ "sum(case c.perkuliahan when c.perkuliahan then 1 else 0 end) jumlah_pengambil_krs_11_sd_20, "
				+ "sum(case d.perkuliahan when d.perkuliahan then 1 else 0 end) jumlah_pengambil_krs_21_sd_30, "
				+ "sum(case e.perkuliahan when e.perkuliahan then 1 else 0 end) jumlah_pengambil_krs_31_sd_40, "
				+ "sum(case f.perkuliahan when f.perkuliahan then 1 else 0 end) jumlah_pengambil_krs_lebih_besar_40, "
				+ "count(a.id) as total  " + "from perkuliahan a "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan having count(id) between 1 and 10) b on (a.id = b.perkuliahan) "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan having count(id) between 11 and 20) c on (a.id = c.perkuliahan) "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan having count(id) between 21 and 30) d on (a.id = d.perkuliahan) "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan having count(id) between 31 and 40) e on (a.id = e.perkuliahan) "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan having count(id) > 40) f on (a.id = f.perkuliahan) "
				+ "left join (select max(perkuliahan) as perkuliahan from detailperkuliahan group by perkuliahan) g on (a.id = g.perkuliahan)  "
				+ "left join jurusan x on (a.jurusan = x.id  )  " + "left join fakultas y on (y.id = x.fakultas)  "
				+ " where 1=1 " + (semesterKe == null || semesterKe.equals(-1) ? "" : " and a.semester = " + semesterKe)
				+ (program == null ? "" : " and a.program = '" + program + "'")
				+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
				+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId()) + " and tahun_ajaran = '"
				+ tahunAkademik + "' and (a.merupakan_paralel is null or a.merupakan_paralel = false) and semester "
				+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 ")
						+ " group by x.fakultas,a.jurusan order by max(y.nama), max(x.nama) ");

		System.out.println(sql);
		jurusans = Common.ambilSql(sql);

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(9);
		spreadsheet.setMaxrows(jurusans.size() + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI PERKULIAHAN \n " + "" + Common.getBahasaConfig("Fakultas") + " "
						+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
						+ Common.getBahasaConfig("Jurusan") + " "
						+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()) + "\n TAHUN AKADEMIK "
						+ tahunAkademik + "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase())
						+ "\n SEMESTER " + semester.toUpperCase() + "\n SEMESTER KE "
						+ (semesterKe.equals(-1) ? "SEMUA" : semesterKe + ""));
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
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "0 Mhs");
		Utils.setColumnWidth(sheet, 2, 100);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "1 s.d 10 Mhs");
		Utils.setColumnWidth(sheet, 3, 100);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "11 s.d 20 Mhs");
		Utils.setColumnWidth(sheet, 4, 100);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "21 s.d 30 Mhs");
		Utils.setColumnWidth(sheet, 5, 100);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "31 s.d 40 Mhs");
		Utils.setColumnWidth(sheet, 6, 100);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "> 40 Mhs");
		Utils.setColumnWidth(sheet, 7, 100);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "Total");
		Utils.setColumnWidth(sheet, 8, 100);

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		String namaFakultas = "";
		String namaProdi = "";
		Integer belumAdaMhs = 0;
		Integer diambil1sd10 = 0;
		Integer diambil11sd20 = 0;
		Integer diambi21sd30 = 0;
		Integer diambi31sd40 = 0;
		Integer diambilLebih40 = 0;
		Integer total = 0;
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
			Integer nilai6 = ((Number) (objects[8] == null ? 0 : objects[8])).intValue();

			belumAdaMhs += nilai0;
			diambil1sd10 += nilai1;
			diambil11sd20 += nilai2;
			diambi21sd30 += nilai3;
			diambi31sd40 += nilai4;
			diambilLebih40 += nilai5;
			total += nilai6;

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, nilai0);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, nilai1);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai2);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, nilai3);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, nilai4);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, nilai5);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, nilai6);

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapPerkuliahan.java:425");
			}

			rowIndex++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, belumAdaMhs);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, diambil1sd10);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, diambil11sd20);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, diambi21sd30);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, diambi31sd40);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, diambilLebih40);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, total);

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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapPerkuliahan.java:455");
		}

	}
}
