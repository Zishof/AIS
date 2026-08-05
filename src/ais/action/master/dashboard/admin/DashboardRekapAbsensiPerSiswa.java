package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapAbsensiPerSiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();
	private Combobox tahunAjaran = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox angkatanMhs = new Combobox();
	private AmbilDataGuruBanbox searchGuru = new AmbilDataGuruBanbox();
	private Textbox siswa = new Textbox();
	private Textbox matakuliah = new Textbox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private Siswa selectedsiswa = null;

	private JadwalPelajaran selectedjadwalPelajaran;

	public DashboardRekapAbsensiPerSiswa() {
		super();
		try {
			init();
			initYayasan();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapAbsensiPerSiswa(Siswa siswa) {
		super();
		this.selectedsiswa = siswa;
		try {
			init();
			initYayasan();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapAbsensiPerSiswa(JadwalPelajaran jadwalPelajaran) {
		super();
		this.selectedjadwalPelajaran = jadwalPelajaran;
		try {
			init();
			initYayasan();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapAbsensiPerSiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initYayasan();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initYayasan() {

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

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
		north.setHeight("200px");
		north.setAutoscroll(true);
		north.setVisible(selectedjadwalPelajaran == null);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");
		searchyayasan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");
		searchsekolah.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran"));
		tahunAjaran = Common.generateTahunAjaran(tahunAjaran);
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");
		tahunAjaran.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru"));
		row.appendChild(searchGuru);
		searchGuru.setWidth("90%");
		searchGuru.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa"));
		row.appendChild(siswa);
		siswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi,
				Common.isNowSemensterGanjil() ? JadwalPelajaran.GANJIL : JadwalPelajaran.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatanMhs);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		angkatanMhs.appendChild(comboitem);
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10; i <= ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR) + 10; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatanMhs.appendChild(comboitem);
		}
		angkatanMhs.setSelectedIndex(0);
		angkatanMhs.setWidth("90%");
		angkatanMhs.setReadonly(true);

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

				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(JadwalPelajaran.GENAP);
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				searchsemester.appendChild(comboitem);
				if (genap) {
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(JadwalPelajaran.GENAP);
					comboitem.setValue(0);
					searchsemester.appendChild(comboitem);
				} else {
					comboitem = new MyComboitemConfig();
					comboitem.setLabel(JadwalPelajaran.GANJIL);
					comboitem.setValue(1);
					searchsemester.appendChild(comboitem);
				}

				searchsemester.setSelectedIndex(0);
				searchsemester.setReadonly(true);
			}
		};
		semesterAbsensi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(arg0);

			}
		});

		eventListener.onEvent(null);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		row.appendChild(matakuliah);
		matakuliah.setWidth("90%");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "6");
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

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_Data.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);
				String tahunAjaran = (String) (DashboardRekapAbsensiPerSiswa.this.tahunAjaran.getSelectedItem() == null
						? null
						: DashboardRekapAbsensiPerSiswa.this.tahunAjaran.getSelectedItem().getValue());
				String semester = (String) (semesterAbsensi.getSelectedItem() == null
						|| semesterAbsensi.getSelectedItem().getValue() == null ? null
								: semesterAbsensi.getSelectedItem().getValue());

				Yayasan yayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
						|| searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? null
								: searchyayasan.getSelectedItem().getValue());
				Sekolah sekolah = (Sekolah) (searchsekolah.getSelectedItem() == null
						|| searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? null
								: searchsekolah.getSelectedItem().getValue());

				Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? null
						: searchsemester.getSelectedItem().getValue());
				String program = (String) (searchprogram.getSelectedItem() == null
						|| searchprogram.getSelectedItem().getValue() == null ? null
								: searchprogram.getSelectedItem().getValue());

				Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
						: angkatanMhs.getSelectedItem().getValue());

				Guru guru = (Guru) searchGuru.getAttribute("guru");

				if (tahunAjaran == null) {
					return;
				}

				Session session = HibernateUtil.currentSession();
				List<Object[]> sekolahs = new ArrayList<Object[]>();

				String sql = "select\n(a.nomor_induk_nasional||' '||a.nama) as siswa,"
						+ "sum(case when c.absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir,\n"
						+ "sum(case when c.absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa,\n"
						+ "sum(case when c.absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit,\n"
						+ "sum(case when c.absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin,\n"
						+ "(a.id) as kode_jadwalPelajaran  \n\n"

						+ DashboardRekapAbsensiSiswa.generateWhere(tahunAjaran, semesterKe, semester, guru, angkatan,
								program, sekolah, yayasan, selectedjadwalPelajaran, siswa.getValue().trim(),
								matakuliah.getValue().trim())
						+ (selectedsiswa == null ? "" : " and a.id = " + selectedsiswa.getId())
						+ " \ngroup by d.id,a.id\norder by (a.nomor_induk_nasional||' '||a.nama)";

				sql = "select max(siswa) siswa, \n"
						+ "sum(hadir) hadir, sum(alpa) alpa, sum(sakit) sakit, sum(izin) izin, kode_jadwalPelajaran\n"
						+ "from (" + sql
						+ ") aaa group by kode_jadwalPelajaran  \n having sum(hadir+alpa+sakit+izin)>0";

				System.out.println(sql);
				sekolahs = session.createSQLQuery(sql).list();

				spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);
				spreadsheet.setParent(center);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns(6);
				spreadsheet.setMaxrows(sekolahs.size() + 4);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, selectedjadwalPelajaran != null
						? "REKAPITULASI ABSENSI SISWA\n" + selectedjadwalPelajaran.info().toUpperCase()
						: "REKAPITULASI ABSENSI PER SISWA \n " + "Yayasan" + " "
								+ (yayasan == null ? "SEMUA" : yayasan.getNama().toUpperCase()) + "\n"
								+ Common.getBahasaConfig("Sekolah") + " "
								+ (sekolah == null ? "SEMUA" : sekolah.getNama().toUpperCase()) + "\n TAHUN AKADEMIK "
								+ tahunAjaran + "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase())
								+ "\n SEMESTER " + semester.toUpperCase() + "\n ANGKATAN "
								+ (angkatan == null ? "SEMUA" : angkatan) + "\n DOSEN "
								+ (guru == null ? "SEMUA" : guru.getNama()));
				final String color = "#000000";
				int rowIndex = 2;
				int colIndex = 0;
				Utils.setRowHeight(sheet, 1, 150);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Siswa");
				Utils.setColumnWidth(sheet, 0, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, Common.getBahasaConfig("Hadir"));
				Utils.setColumnWidth(sheet, 1, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.getBahasaConfig("Alpa"));
				Utils.setColumnWidth(sheet, 2, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.getBahasaConfig("Sakit"));
				Utils.setColumnWidth(sheet, 3, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.getBahasaConfig("Izin"));
				Utils.setColumnWidth(sheet, 4, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Total tidak hadir");
				Utils.setColumnWidth(sheet, 5, 80);

				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

				rowIndex = 3;
				colIndex = 0;

				Integer nilai0Total = 0;
				Integer nilai1Total = 0;
				Integer nilai2Total = 0;
				Integer nilai3Total = 0;
				Integer totalSemua = 0;
				for (Object[] objects : sekolahs) {
					if (objects[0] == null)
						continue;
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());

					Integer nilai0 = ((Number) (objects[1] == null ? 0 : objects[1])).intValue();
					Integer nilai1 = ((Number) (objects[2] == null ? 0 : objects[2])).intValue();
					Integer nilai2 = ((Number) (objects[3] == null ? 0 : objects[3])).intValue();
					Integer nilai3 = ((Number) (objects[4] == null ? 0 : objects[4])).intValue();

					Integer total = nilai1 + nilai2 + nilai3;

					nilai0Total += nilai0;
					nilai1Total += nilai1;
					nilai2Total += nilai2;
					nilai3Total += nilai3;
					totalSemua += total;

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, nilai0);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, nilai1);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, nilai2);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai3);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, total);

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapAbsensiPerSiswa.java:458");
					}

					rowIndex++;
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, nilai0Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, nilai1Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, nilai2Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai3Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, totalSemua);

				Common.setStyled(sheet);
				spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

				colIndex = 0;
				try {
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex),
							true);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapAbsensiPerSiswa.java:481");
				}
			}
		});

	}
}
