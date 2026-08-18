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
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Rekap absensi <b>PER KELAS dan MATA PELAJARAN</b> versi <b>SEKOLAH</b> (berbasis
 * JadwalPelajaran / Guru / MataPelajaran). Ini padanan sekolah dari
 * {@link DashboardRekapAbsensiPerKelasDanKuliah} yang masih berbasis perkuliahan
 * (Fakultas/Prodi/Dosen/Matakuliah).
 *
 * <p>
 * Filter dan join data MEMAKAI ULANG pola {@link DashboardRekapAbsensiSiswa}
 * (method {@code generateWhere(...)} yang sama: siswa &rarr; kelas_punya_siswa
 * &rarr; jadwal_pelajaran &rarr; pertemuan), namun hasil dikelompokkan PER JADWAL
 * ({@code jadwal_pelajaran.id} = kombinasi kelas &times; mata pelajaran &times;
 * guru &times; jam), bukan per siswa. Nama kelas diambil dari {@code sekolah.kelas}
 * via subquery pada {@code d.kelas_id}.
 * </p>
 */
public class DashboardRekapAbsensiPerKelasDanMapel extends MyWindow {

	private static final long serialVersionUID = 790038368339375114L;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();
	private Combobox tahunAjaran = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox angkatanMhs = new Combobox();
	private AmbilDataGuruBanbox searchGuru = new AmbilDataGuruBanbox();

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private JadwalPelajaran selectedjadwalPelajaran = null;

	private Textbox siswa = new Textbox();
	private Textbox matapelajaran = new Textbox();

	public DashboardRekapAbsensiPerKelasDanMapel() {
		super();
		try {
			init();
			initYayasan();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapAbsensiPerKelasDanMapel(JadwalPelajaran jadwalPelajaran) {
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

	public DashboardRekapAbsensiPerKelasDanMapel(String title, String border, boolean closable) {
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
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
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Matapelajaran"));
		row.appendChild(matapelajaran);
		matapelajaran.setWidth("90%");

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
				String tahunAjaran = (String) (DashboardRekapAbsensiPerKelasDanMapel.this.tahunAjaran
						.getSelectedItem() == null ? null
								: DashboardRekapAbsensiPerKelasDanMapel.this.tahunAjaran.getSelectedItem().getValue());
				String semester = (String) (semesterAbsensi.getSelectedItem() == null
						|| semesterAbsensi.getSelectedItem().getValue() == null ? null
								: semesterAbsensi.getSelectedItem().getValue());

				Yayasan yayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? null
								: searchyayasan.getSelectedItem().getValue());
				Sekolah sekolah = (Sekolah) (searchsekolah.getSelectedItem() == null
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

				// Kolom kelas diambil dari sekolah.kelas (d.kelas_id). Dikelompokkan PER JADWAL
				// (d.id) sehingga setiap baris = satu kombinasi kelas x mata pelajaran x guru.
				String sql = "select\n(select k.nama from sekolah.kelas k where k.id = d.kelas_id) as kelas,\n max(e.kode||' '||e.nama) as matapelajaran,\n"
						+ " max(trim((case when d.hari is null then '' else d.hari end)||' '||(case when d.waktumulai is null then '' else d.waktumulai end)||(case when d.waktuselesai is null then ' ' else ' - '||d.waktuselesai||', ' end)||(case when dp.id is null then '' else dp.nama end) ||(case when dp2.id is null then '' else dp2.nama end)||(case when dp3.id is null then '' else dp3.nama end)||(case when dp4.id is null then '' else dp4.nama end)||(case when dp5.id is null then '' else dp5.nama end)  )) as jadwalPelajaran,\n"
						+ "sum(case when c.absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir,\n"
						+ "sum(case when c.absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa,\n"
						+ "sum(case when c.absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit,\n"
						+ "sum(case when c.absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin,\n"
						+ "(d.id) as kode_jadwalPelajaran  \n\n"
						+ DashboardRekapAbsensiSiswa.generateWhere(tahunAjaran, semesterKe, semester, guru, angkatan,
								program, sekolah, yayasan, selectedjadwalPelajaran, siswa.getValue().trim(),
								matapelajaran.getValue().trim())
						+ " \ngroup by d.id";

				sql = "select max(kelas) kelas, max(matapelajaran) matapelajaran, max(jadwalPelajaran) jadwalPelajaran,\n"
						+ "sum(hadir) hadir, sum(alpa) alpa, sum(sakit) sakit, sum(izin) izin, kode_jadwalPelajaran\n"
						+ "from (" + sql
						+ ") aaa group by kode_jadwalPelajaran \n having sum(hadir+alpa+sakit+izin)>0\norder by max(kelas), max(matapelajaran)";

				System.out.println(sql);
				sekolahs = session.createSQLQuery(sql).list();

				spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);
				org.zkoss.zul.Div pembungkusVisual = new org.zkoss.zul.Div();
				pembungkusVisual.setWidth("100%");
				pembungkusVisual.setStyle("height:100%;overflow:auto;box-sizing:border-box;");
				pembungkusVisual.setParent(center);
				new org.zkoss.zul.Html(
						ais.action.master.dashboard.helper.DashboardVisualHelper.kehadiran(sekolahs, 0, 3, 4, 5, 6))
						.setParent(pembungkusVisual);
				spreadsheet.setParent(pembungkusVisual);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns(8);
				spreadsheet.setMaxrows(sekolahs.size() + 4);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, selectedjadwalPelajaran != null
						? "REKAPITULASI ABSENSI KELAS\n" + selectedjadwalPelajaran.info().toUpperCase()
						: "REKAPITULASI ABSENSI KELAS \n " + "Yayasan" + " "
								+ (yayasan == null ? "SEMUA" : yayasan.getNama().toUpperCase()) + "\n"
								+ Common.getBahasaConfig("Sekolah") + " "
								+ (sekolah == null ? "SEMUA" : sekolah.getNama().toUpperCase()) + "\n TAHUN AJARAN "
								+ tahunAjaran + "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase())
								+ "\n SEMESTER " + (semester == null ? "SEMUA" : semester.toUpperCase()) + "\n ANGKATAN "
								+ (angkatan == null ? "SEMUA" : angkatan) + "\n GURU "
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
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Kelas");
				Utils.setColumnWidth(sheet, 0, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Matapelajaran");
				Utils.setColumnWidth(sheet, 1, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Jadwal Pelajaran/Guru");
				Utils.setColumnWidth(sheet, 2, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.getBahasaConfig("Hadir"));
				Utils.setColumnWidth(sheet, 3, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.getBahasaConfig("Alpa"));
				Utils.setColumnWidth(sheet, 4, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, Common.getBahasaConfig("Sakit"));
				Utils.setColumnWidth(sheet, 5, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, Common.getBahasaConfig("Izin"));
				Utils.setColumnWidth(sheet, 6, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "Total tidak hadir");
				Utils.setColumnWidth(sheet, 7, 80);

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

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[1] == null ? "" : objects[1].toString());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, objects[2] == null ? "" : objects[2].toString());

					Integer nilai0 = ((Number) (objects[3] == null ? 0 : objects[3])).intValue();
					Integer nilai1 = ((Number) (objects[4] == null ? 0 : objects[4])).intValue();
					Integer nilai2 = ((Number) (objects[5] == null ? 0 : objects[5])).intValue();
					Integer nilai3 = ((Number) (objects[6] == null ? 0 : objects[6])).intValue();

					Integer total = nilai1 + nilai2 + nilai3;

					nilai0Total += nilai0;
					nilai1Total += nilai1;
					nilai2Total += nilai2;
					nilai3Total += nilai3;
					totalSemua += total;

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, nilai0);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai1);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, nilai2);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, nilai3);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, total);

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapAbsensiPerKelasDanMapel.java:471");
					}

					rowIndex++;
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, nilai0Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai1Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, nilai2Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, nilai3Total);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, totalSemua);

				Common.setStyled(sheet);
				spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

				colIndex = 0;
				try {
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex),
							true);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapAbsensiPerKelasDanMapel.java:494");
				}
			}
		});

	}
}
