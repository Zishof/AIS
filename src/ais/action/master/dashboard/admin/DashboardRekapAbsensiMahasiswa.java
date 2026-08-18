package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
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
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.common.Common;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapAbsensiMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	public static boolean debug = false;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox angkatanMhs = new Combobox();
	private AmbilDataDosenBanbox searchDosen = new AmbilDataDosenBanbox();

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private Mahasiswa selectedmahasiswa = null;

	private Perkuliahan selectedperkuliahan = null;

	private Textbox mahasiswa = new Textbox();
	private Textbox matakuliah = new Textbox();

	private Combobox comboTampilkan;

	public DashboardRekapAbsensiMahasiswa() {
		super();
		try {
			init();
			initFakultas();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapAbsensiMahasiswa(Mahasiswa mahasiswa) {
		super();
		this.selectedmahasiswa = mahasiswa;
		try {
			init();
			initFakultas();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapAbsensiMahasiswa(Perkuliahan perkuliahan) {
		super();
		this.selectedperkuliahan = perkuliahan;
		try {
			init();
			initFakultas();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapAbsensiMahasiswa(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initFakultas();
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
		north.setHeight("200px");
		north.setAutoscroll(true);

		north.setVisible(selectedperkuliahan == null);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(searchDosen);
		searchDosen.setWidth("90%");
		searchDosen.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(mahasiswa);
		mahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		semesterAbsensi.appendChild(comboitem);

		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

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

				if (DashboardRekapAbsensiMahasiswa.this.selectedmahasiswa != null) {

					MyComboitemConfig comboitem = new MyComboitemConfig();
					comboitem.setLabel("Semua");
					comboitem.setValue(null);
					searchsemester.appendChild(comboitem);
					searchsemester.setSelectedItem(comboitem);
					searchsemester.setDisabled(true);

					angkatanMhs.setDisabled(true);
					searchfakultas.setDisabled(true);
					searchjurusan.setDisabled(true);
					searchDosen.setDisabled(true);
					searchprogram.setDisabled(true);
					return;
				}

				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);

				searchsemester.setDisabled(false);
				if (semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.SP)) {
					MyComboitemConfig comboitem = new MyComboitemConfig();
					comboitem.setLabel("Semua");
					comboitem.setValue(null);
					searchsemester.appendChild(comboitem);
					searchsemester.setDisabled(true);
				} else if (genap) {
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						MyComboitemConfig comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						MyComboitemConfig comboitem = new MyComboitemConfig();
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
		// if (searchDosen.getAttribute("dosen") == null) {
		// Common.selectComboItem(angkatanMhs,
		// ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
		// searchsemester.setSelectedIndex(1);
		// }

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		row.appendChild(matakuliah);
		matakuliah.setWidth("90%");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "10");
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

		comboTampilkan = new Combobox();
		Integer[] dataCombo = new Integer[] { 10, 30, 50, 100, 300, 500, 1000, 2000, 5000, 10000 };
		for (Integer d : dataCombo) {
			comboitem = new MyComboitemConfig(d + " tampilan");
			comboitem.setValue(d);
			comboTampilkan.appendChild(comboitem);
		}
		// Opsi "Semua" → tampilkan seluruh data (nilai besar dipakai sebagai batas setMaxResults).
		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(Integer.valueOf(1000000));
		comboTampilkan.appendChild(comboitem);
		comboTampilkan.setReadonly(true);
		Common.selectComboItem(comboTampilkan, 30);
		comboTampilkan.setParent(toolbar);
		comboTampilkan.setCols(7);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.setTooltiptext("Buka preview Excel terlebih dahulu, lalu download dari popup preview.");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				tampilkanPopupPreviewExcel();
			}
		});
		print.setParent(toolbar);

		if (DashboardRekapAbsensiMahasiswa.this.selectedmahasiswa != null
				&& DashboardRekapAbsensiMahasiswa.this.selectedmahasiswa.getId() != null) {
			mahasiswa.setValue(DashboardRekapAbsensiMahasiswa.this.selectedmahasiswa.getNim());
			mahasiswa.setReadonly(true);
			mahasiswa.setDisabled(true);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					initSpreadsheet();
				}
			});
		}

	}

	public static String generateWhere(String tahunAkademik, Integer semesterKe, String semester, Dosen dosen,
			Integer angkatan, String program, Jurusan jurusan, Fakultas fakultas, String mhs, String mk) {
		return generateWhere(tahunAkademik, semesterKe, semester, dosen, angkatan, program, jurusan, fakultas, null,
				mhs, mk);
	}

	public static String generateWhere(String tahunAkademik, Integer semesterKe, String semester, Dosen dosen,
			Integer angkatan, String program, Jurusan jurusan, Fakultas fakultas, Perkuliahan kul, String mhs,
			String mk) {
		String sql = " from mahasiswa a\n  inner join jurusan x on (a.jurusan = x.id  )  "
				+ "inner join detailperkuliahan d1 on (d1.mahasiswa=a.id)\n "
				+ "inner join perkuliahan d on (d.id=d1.perkuliahan)\n "
				+ "inner join pertemuan c on (c.perkuliahan=d.id)\n"
				+ " left join dosen dp on ( dp.id = d.dosen1 )\n left join dosen dp2 on ( dp2.id = d.dosen2 )\n "
				+ "left join dosen dp3 on ( dp3.id = d.dosen3 )\n left join dosen dp4 on ( dp4.id = d.dosen4 )\n"
				+ "left join dosen dp5 on ( dp5.id = d.dosen5 )\n"
				+ "inner join matakuliah e on (e.id=d.matakuliah)\n\n ";

		if (kul != null) {
			Perkuliahan kuliyah = Boolean.TRUE.equals(kul.getMerupakan_paralel()) && kul.getPerkuliahan_paralel() != null
					? kul.getPerkuliahan_paralel()
					: kul;
			List<Long> perkuliahans = kuliyah.ambilParalel();
			if (perkuliahans == null) {
				perkuliahans = new ArrayList<Long>();
			}
			if (kuliyah.getId() != null && !perkuliahans.contains(kuliyah.getId())) {
				perkuliahans.add(kuliyah.getId());
			}

			String in = "(-1111";
			for (Long id : perkuliahans) {
				if (id != null) {
					in += "," + id;
				}
			}
			in += ")";
			sql += "where d1.perkuliahan in " + in;
		} else {
			String tahun = sqlText(tahunAkademik);
			String jenisSemester = sqlText(semester);
			sql += "where d1.persetujuan=1 and d.tahun_ajaran='" + tahun + "'"
					+ (Perkuliahan.SP.equals(semester)
							? " and d.status_semesterpendek=" + Perkuliahan.SEMESTER_PENDEK
							: (semesterKe == null ? "" : "  and d.semester = " + semesterKe) + " and d.ganjil_genap= '"
									+ jenisSemester + "'")
					+ (dosen == null || dosen.getId() == null ? ""
							: (" and (d.dosen1 = " + dosen.getId() + " or d.dosen2 = " + dosen.getId()
									+ " or d.dosen3 = " + dosen.getId() + " or d.dosen4 = " + dosen.getId()
									+ " or d.dosen5 = " + dosen.getId() + " or d.dosen6 = " + dosen.getId()
									+ " or d.dosen7 = " + dosen.getId() + " or d.dosen8 = " + dosen.getId()
									+ " or d.dosen9 = " + dosen.getId() + " or d.dosen10 = " + dosen.getId()
									+ ")"))
					+ (angkatan == null ? "" : " and  a.tahunangkatan = " + angkatan)
					+ (program == null || program.trim().isEmpty() ? "" : " and a.program = '" + sqlText(program) + "'")
					+ (jurusan == null || jurusan.getId() == null ? "" : " and a.jurusan = " + jurusan.getId())
					+ (fakultas == null || fakultas.getId() == null ? "" : " and x.fakultas = " + fakultas.getId())
					+ (mhs == null || mhs.trim().isEmpty() ? ""
							: " and (a.nama ilike '%" + sqlLike(mhs) + "%' or a.nim ilike '%" + sqlLike(mhs)
									+ "%')")
					+ (mk == null || mk.trim().isEmpty() ? ""
							: " and (e.nama ilike '%" + sqlLike(mk) + "%' or e.kode ilike '%" + sqlLike(mk)
									+ "%')");
		}
		return sql;
	}

	private static String sqlText(String value) {
		return value == null ? "" : value.trim().replace("'", "''");
	}

	private static String sqlLike(String value) {
		return sqlText(value).replace("%", "\\%").replace("_", "\\_");
	}


	private void initSpreadsheet() {
		Common.clear(center);
		tampilkanLoading("Mengambil dan menghitung rekap absensi mahasiswa...");

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String tahunAkademik = (String) (DashboardRekapAbsensiMahasiswa.this.tahunAkademik.getSelectedItem() == null
						? null
						: DashboardRekapAbsensiMahasiswa.this.tahunAkademik.getSelectedItem().getValue());
				String semester = (String) (semesterAbsensi.getSelectedItem() == null
						|| semesterAbsensi.getSelectedItem().getValue() == null ? null
								: semesterAbsensi.getSelectedItem().getValue());

				Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? null
								: searchfakultas.getSelectedItem().getValue());
				Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? null
								: searchjurusan.getSelectedItem().getValue());

				Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? null
						: searchsemester.getSelectedItem().getValue());
				String program = (String) (searchprogram.getSelectedItem() == null
						|| searchprogram.getSelectedItem().getValue() == null ? null
								: searchprogram.getSelectedItem().getValue());

				Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
						: angkatanMhs.getSelectedItem().getValue());

				Dosen dosen = (Dosen) searchDosen.getAttribute("dosen");

				if (tahunAkademik == null || semester == null) {
					Common.clear(center);
					new Html(buildEmptyHtml("Tahun akademik dan jenis semester wajib dipilih sebelum memproses rekap absensi."))
							.setParent(center);
					return;
				}

				List<Object[]> jurusans = new ArrayList<Object[]>();
				int limit = getSelectedLimit();

				String sql = "select\n(a.nim||' '||a.nama) as mahasiswa,\n max(e.kode||' '||e.nama) as matakuliah,\n"
						+ " max((case when d.perkuliahan_paralel is null then '' else '(P) ' end) || trim((case when d.hari is null then '' else d.hari end)||' '||(case when d.waktu_mulai is null then '' else d.waktu_mulai end)||(case when d.waktu_selesai is null then ' ' else ' - '||d.waktu_selesai||', ' end)||(case when dp.id is null then '' else dp.nama end) ||(case when dp2.id is null then '' else dp2.nama end)||(case when dp3.id is null then '' else dp3.nama end)||(case when dp4.id is null then '' else dp4.nama end)||(case when dp5.id is null then '' else dp5.nama end)  )) as perkuliahan,\n"
						+ "sum(case when absensi ilike '%'||a.id||',1%' then 1 else 0 end) as hadir,\n"
						+ "sum(case when absensi ilike '%'||a.id||',2%' then 1 else 0 end) as alpa,\n"
						+ "sum(case when absensi ilike '%'||a.id||',3%' then 1 else 0 end) as sakit,\n"
						+ "sum(case when absensi ilike '%'||a.id||',4%' then 1 else 0 end) as izin,\n"
						+ "(a.id||'-'||d.id) as kode_perkuliahan  \n\n"
						+ DashboardRekapAbsensiMahasiswa.generateWhere(tahunAkademik, semesterKe, semester, dosen,
								angkatan, program, jurusan, fakultas, selectedperkuliahan, mahasiswa.getValue().trim(),
								matakuliah.getValue().trim())
						+ (selectedmahasiswa == null || selectedmahasiswa.getId() == null ? ""
								: " and a.id = " + selectedmahasiswa.getId())
						+ " \ngroup by d.id,a.id\norder by (a.nim||' '||a.nama)";

				sql = "select max(mahasiswa) mahasiswa, max(matakuliah) matakuliah, max(perkuliahan) perkuliahan,\n"
						+ "sum(hadir) hadir, sum(alpa) alpa, sum(sakit) sakit, sum(izin) izin, kode_perkuliahan\n"
						+ "from (" + sql
						+ ") aaa group by kode_perkuliahan \n having sum(hadir+alpa+sakit+izin)>0 limit " + limit;

				if (debug) {
					System.out.println(sql);
				}
				jurusans = Common.ambilSql(sql);

				AttendanceSummary summary = hitungSummary(jurusans, true);
				Common.clear(center);
				Vbox resultContainer = new Vbox();
				resultContainer.setWidth("100%");
				resultContainer.setHeight("100%");
				resultContainer.setSpacing("12px");
				resultContainer.setStyle("padding:14px;background:#f5f7fb;box-sizing:border-box;");
				resultContainer.setParent(center);
				new Html(buildDashboardHtml("Dashboard Rekap Absensi Mahasiswa", selectedperkuliahan != null
						? selectedperkuliahan.info()
						: "Filter: " + nvl(fakultas == null ? null : fakultas.getNama(), "Semua Fakultas") + " / "
								+ nvl(jurusan == null ? null : jurusan.getNama(), "Semua Prodi") + " / "
								+ tahunAkademik + " / " + semester,
						summary,
						"Membantu staf melihat rekap kehadiran per mahasiswa dan per mata kuliah. Angka hadir, alpa, sakit, dan izin diringkas agar dosen atau operator cepat mengetahui mahasiswa yang perlu perhatian."))
						.setParent(resultContainer);

				renderRekapGrid(resultContainer, jurusans);

				spreadsheet = new ais.ui.util.MySpreadsheet();
				spreadsheet.setParent(resultContainer);
				spreadsheet.setVisible(false);
				spreadsheet.setWidth("1px");
				spreadsheet.setHeight("1px");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns(11);
				spreadsheet.setMaxrows(jurusans.size() + 4);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, selectedperkuliahan != null
						? "REKAPITULASI ABSENSI\n" + selectedperkuliahan.info().toUpperCase()
						: "REKAPITULASI ABSENSI MAHASISWA \n " + Common.getBahasaConfig("Fakultas") + " "
								+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
								+ Common.getBahasaConfig("Jurusan") + " "
								+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase())
								+ "\n TAHUN AKADEMIK " + tahunAkademik + "\nPROGRAM "
								+ (program == null ? "SEMUA" : program.toUpperCase()) + "\n SEMESTER "
								+ semester.toUpperCase() + "\n ANGKATAN " + (angkatan == null ? "SEMUA" : angkatan)
								+ "\n DOSEN " + (dosen == null ? "SEMUA" : dosen.getNama()));
				final String color = "#000000";
				int rowIndex = 2;
				int colIndex = 0;
				Utils.setRowHeight(sheet, 1, 150);
				ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Mahasiswa");
				Utils.setColumnWidth(sheet, 0, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Matakuliah");
				Utils.setColumnWidth(sheet, 1, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Jadwal Perkuliahan/Dosen");
				Utils.setColumnWidth(sheet, 2, 200);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.getBahasaConfig("Hadir"));
				Utils.setColumnWidth(sheet, 3, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.getBahasaConfig("Alpa"));
				Utils.setColumnWidth(sheet, 4, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, Common.getBahasaConfig("Sakit"));
				Utils.setColumnWidth(sheet, 5, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, Common.getBahasaConfig("Izin"));
				Utils.setColumnWidth(sheet, 6, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, Common.getBahasaConfig("%Hadir"));
				Utils.setColumnWidth(sheet, 7, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, Common.getBahasaConfig("%Hadir dr Alpa"));
				Utils.setColumnWidth(sheet, 8, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, Common.getBahasaConfig("Jml Pertemuan"));
				Utils.setColumnWidth(sheet, 9, 80);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "Total tidak hadir");
				Utils.setColumnWidth(sheet, 10, 80);

				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

				rowIndex = 3;
				colIndex = 0;

				for (Object[] objects : jurusans) {
					if (objects == null || objects.length == 0 || objects[0] == null) {
						continue;
					}
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, String.valueOf(objects[0]));
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, String.valueOf(objects[1]));
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, String.valueOf(objects[2]));

					Integer nilai0 = toInteger(objects[3]);
					Integer nilai1 = toInteger(objects[4]);
					Integer nilai2 = toInteger(objects[5]);
					Integer nilai3 = toInteger(objects[6]);
					Integer total = Integer.valueOf(nilai1.intValue() + nilai2.intValue() + nilai3.intValue());
					Integer totalP = Integer.valueOf(nilai0.intValue() + nilai1.intValue() + nilai2.intValue() + nilai3.intValue());

					Double persenHadir = Double.valueOf(totalP.intValue() <= 0 ? 0.0
							: (nilai0.intValue() * 100.0) / totalP.intValue());
					Double persenHadirDrAlpa = Double.valueOf(totalP.intValue() <= 0 ? 0.0
							: ((nilai0.intValue() + nilai2.intValue() + nilai3.intValue()) * 100.0) / totalP.intValue());

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, nilai0);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai1);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, nilai2);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, nilai3);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7,
							Common.numberFormat.get().format(persenHadir) + "%");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8,
							Common.numberFormat.get().format(persenHadirDrAlpa) + "%");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, totalP);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, total);

					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) {
						if (debug) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardRekapAbsensiMahasiswa.java:643");
						}
					}

					rowIndex++;
				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Integer.valueOf(summary.hadir));
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Integer.valueOf(summary.alpa));
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, Integer.valueOf(summary.sakit));
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, Integer.valueOf(summary.izin));
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, Integer.valueOf(summary.totalPertemuan));
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, Integer.valueOf(summary.totalTidakHadir));

				Common.setStyled(sheet);
				spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

				try {
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex),
							true);
				} catch (Exception e) {
					if (debug) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardRekapAbsensiMahasiswa.java:671");
					}
				}
			}
		});
	}


	private void renderRekapGrid(Vbox parent, List<Object[]> data) {
		new Html("<div style='background:#ffffff;border:1px solid #e2e8f0;border-radius:16px;padding:14px;"
				+ "box-shadow:0 10px 24px rgba(15,23,42,.06);'>"
				+ "<div style='font-size:16px;font-weight:900;color:#0f172a;'>Tabel Rekap Absensi</div>"
				+ "<div style='font-size:12px;color:#64748b;line-height:1.55;margin-top:5px;'>"
				+ "Data ditampilkan lebih dulu sebagai tabel agar pengguna dapat membaca dan memeriksa hasil rekap tanpa membuka Excel. Gunakan tombol Download untuk melihat preview Excel, lalu unduh file jika datanya sudah sesuai."
				+ "</div></div>").setParent(parent);

		org.zkoss.zul.Grid grid = new org.zkoss.zul.Grid();
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setWidth("100%");
		grid.setStyle("border:1px solid #e2e8f0;border-radius:14px;background:#ffffff;overflow:hidden;");
		grid.setParent(parent);

		Columns columns = new Columns();
		columns.setParent(grid);
		new MyColumnConfig("Mahasiswa").setParent(columns);
		new MyColumnConfig("Mata Kuliah").setParent(columns);
		new MyColumnConfig("Jadwal / Dosen").setParent(columns);
		new MyColumnConfig("Hadir").setParent(columns);
		new MyColumnConfig("Alpa").setParent(columns);
		new MyColumnConfig("Sakit").setParent(columns);
		new MyColumnConfig("Izin").setParent(columns);
		new MyColumnConfig("% Hadir").setParent(columns);
		new MyColumnConfig("% Hadir dr Alpa").setParent(columns);
		new MyColumnConfig("Jml Pertemuan").setParent(columns);
		new MyColumnConfig("Tidak Hadir").setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);
		if (data == null || data.isEmpty()) {
			MyFormRow empty = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(empty, "11");
			empty.setParent(rows);
			new Label(ais.common.Common.getBahasaConfig("Tidak ada data absensi yang sesuai dengan filter.")).setParent(empty);
			return;
		}

		AttendanceSummary summary = hitungSummary(data, true);
		for (Object[] objects : data) {
			if (objects == null || objects.length == 0 || objects[0] == null) {
				continue;
			}
			Integer hadir = toInteger(objects[3]);
			Integer alpa = toInteger(objects[4]);
			Integer sakit = toInteger(objects[5]);
			Integer izin = toInteger(objects[6]);
			Integer tidakHadir = Integer.valueOf(alpa.intValue() + sakit.intValue() + izin.intValue());
			Integer totalPertemuan = Integer.valueOf(hadir.intValue() + tidakHadir.intValue());
			Double persenHadir = Double.valueOf(totalPertemuan.intValue() <= 0 ? 0.0
					: (hadir.intValue() * 100.0) / totalPertemuan.intValue());
			Double persenHadirDrAlpa = Double.valueOf(totalPertemuan.intValue() <= 0 ? 0.0
					: ((hadir.intValue() + sakit.intValue() + izin.intValue()) * 100.0) / totalPertemuan.intValue());

			MyFormRow row = new MyFormRow();
			row.setParent(rows);
			new Label(String.valueOf(objects[0])).setParent(row);
			new Label(String.valueOf(objects[1])).setParent(row);
			new Label(String.valueOf(objects[2])).setParent(row);
			new Label(String.valueOf(hadir)).setParent(row);
			new Label(String.valueOf(alpa)).setParent(row);
			new Label(String.valueOf(sakit)).setParent(row);
			new Label(String.valueOf(izin)).setParent(row);
			Label pct = new Label(Common.numberFormat.get().format(persenHadir) + "%");
			pct.setStyle("font-weight:800;color:" + (persenHadir.doubleValue() >= 75.0 ? "#166534" : "#991b1b") + ";");
			pct.setParent(row);
			new Label(Common.numberFormat.get().format(persenHadirDrAlpa) + "%").setParent(row);
			new Label(String.valueOf(totalPertemuan)).setParent(row);
			new Label(String.valueOf(tidakHadir)).setParent(row);
		}

		MyFormRow total = new MyFormRow();
		total.setStyle("font-weight:900;background:#f8fafc;");
		total.setParent(rows);
		new Label(ais.common.Common.getBahasaConfig("TOTAL")).setParent(total);
		new Label("").setParent(total);
		new Label("").setParent(total);
		new Label(String.valueOf(summary.hadir)).setParent(total);
		new Label(String.valueOf(summary.alpa)).setParent(total);
		new Label(String.valueOf(summary.sakit)).setParent(total);
		new Label(String.valueOf(summary.izin)).setParent(total);
		new Label(Common.numberFormat.get().format(summary.persenHadir) + "%").setParent(total);
		new Label("").setParent(total);
		new Label(String.valueOf(summary.totalPertemuan)).setParent(total);
		new Label(String.valueOf(summary.totalTidakHadir)).setParent(total);
	}

	private void tampilkanPopupPreviewExcel() {
		try {
			if (spreadsheet == null || spreadsheet.getBook() == null) {
				Common.clear(center);
				new Html(buildEmptyHtml("Silakan klik Proses terlebih dahulu agar data rekap tersedia sebelum download Excel."))
						.setParent(center);
				return;
			}
			final MyWindow popup = new MyWindow();
			popup.setTitle("Preview Excel Rekap Absensi Mahasiswa");
			popup.setBorder("normal");
			popup.setClosable(true);
			popup.setWidth("92%");
			popup.setHeight("88%");
			popup.setPosition("center");
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(popup);

			Vbox box = new Vbox();
			box.setWidth("100%");
			box.setHeight("100%");
			box.setStyle("padding:10px;box-sizing:border-box;background:#f8fafc;");
			box.setParent(popup);

			Toolbar toolbar = new Toolbar();
			toolbar.setStyle("background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;padding:8px;margin-bottom:8px;");
			toolbar.setParent(box);
			MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download Excel", "/img/print.png");
			download.setTooltiptext("Download file Excel sesuai preview yang sedang tampil.");
			download.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					spreadsheet.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_Data.xlsx");
				}
			});
			download.setParent(toolbar);

			if (spreadsheet.getParent() != null) {
				spreadsheet.detach();
			}
			spreadsheet.setVisible(true);
			spreadsheet.setWidth("100%");
			spreadsheet.setHeight("100%");
			spreadsheet.setParent(box);
			popup.setVisible(true);
			popup.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}


	private void tampilkanLoading(String message) {
		Common.clear(center);
		new Html("<div style='padding:18px;text-align:center;color:#475569;background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;margin:14px;'>"
				+ "<div style='font-size:18px;font-weight:800;color:#0f172a;margin-bottom:6px;'>Memproses Data</div>"
				+ "<div style='font-size:12px;line-height:1.5;'>" + escapeHtml(message) + "</div>"
				+ "<div style='height:8px;background:#e2e8f0;border-radius:999px;margin-top:14px;overflow:hidden;'>"
				+ "<div style='width:66%;height:8px;background:linear-gradient(90deg,#2563eb,#38bdf8);border-radius:999px;'></div></div>"
				+ "</div>").setParent(center);
	}

	private int getSelectedLimit() {
		try {
			if (comboTampilkan != null && comboTampilkan.getSelectedItem() != null
					&& comboTampilkan.getSelectedItem().getValue() != null) {
				return ((Number) comboTampilkan.getSelectedItem().getValue()).intValue();
			}
		} catch (Exception e) {
			if (debug) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/admin/DashboardRekapAbsensiMahasiswa.java:840");
			}
		}
		return 30;
	}

	private static AttendanceSummary hitungSummary(List<Object[]> rows, boolean detailMahasiswa) {
		AttendanceSummary summary = new AttendanceSummary();
		if (rows == null) {
			return summary;
		}
		for (Object[] row : rows) {
			if (row == null) {
				continue;
			}
			int offset = detailMahasiswa ? 3 : 2;
			summary.hadir += toInteger(row[offset]).intValue();
			summary.alpa += toInteger(row[offset + 1]).intValue();
			summary.sakit += toInteger(row[offset + 2]).intValue();
			summary.izin += toInteger(row[offset + 3]).intValue();
			summary.jumlahBaris++;
		}
		summary.totalTidakHadir = summary.alpa + summary.sakit + summary.izin;
		summary.totalPertemuan = summary.hadir + summary.totalTidakHadir;
		summary.persenHadir = summary.totalPertemuan <= 0 ? 0.0 : (summary.hadir * 100.0) / summary.totalPertemuan;
		return summary;
	}

	private static String buildDashboardHtml(String title, String subtitle, AttendanceSummary summary, String description) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='border-radius:18px;overflow:hidden;background:#fff;box-shadow:0 16px 30px rgba(15,23,42,.08);border:1px solid #e2e8f0;'>");
		sb.append("<div style='padding:18px 20px;background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);color:#fff;'>");
		sb.append("<div style='font-size:12px;letter-spacing:1px;text-transform:uppercase;font-weight:800;opacity:.78;'>Rekap Absensi</div>");
		sb.append("<div style='font-size:23px;font-weight:900;line-height:1.25;margin-top:4px;'>").append(escapeHtml(title)).append("</div>");
		sb.append("<div style='font-size:12px;opacity:.9;margin-top:5px;'>").append(escapeHtml(nvl(subtitle, "-"))).append("</div>");
		sb.append("<div style='font-size:12px;line-height:1.55;opacity:.9;margin-top:10px;max-width:920px;'>").append(escapeHtml(description)).append("</div>");
		sb.append("</div>");
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(145px,1fr));gap:12px;padding:14px;'>");
		sb.append(cardHtml("Baris Data", String.valueOf(summary.jumlahBaris), "Data hasil filter", "#2563eb"));
		sb.append(cardHtml("Hadir", String.valueOf(summary.hadir), "Total hadir", "#16a34a"));
		sb.append(cardHtml("Alpa", String.valueOf(summary.alpa), "Tanpa keterangan", "#dc2626"));
		sb.append(cardHtml("Sakit", String.valueOf(summary.sakit), "Izin sakit", "#ca8a04"));
		sb.append(cardHtml("Izin", String.valueOf(summary.izin), "Izin resmi", "#7c3aed"));
		sb.append(cardHtml("% Hadir", Common.numberFormat.get().format(summary.persenHadir) + "%", "Dari seluruh pertemuan", "#0d9488"));
		sb.append("</div>");
		sb.append("<div style='padding:0 14px 14px 14px;'>").append(barHtml(summary)).append("</div>");
		sb.append("</div>");
		return sb.toString();
	}

	private static String cardHtml(String title, String value, String description, String color) {
		return "<div style='border:1px solid #e2e8f0;border-radius:14px;background:#f8fafc;padding:12px;'>"
				+ "<div style='font-size:11px;color:#64748b;text-transform:uppercase;letter-spacing:.7px;font-weight:800;'>"
				+ escapeHtml(title) + "</div><div style='font-size:24px;font-weight:900;color:" + color + ";margin-top:4px;'>"
				+ escapeHtml(value) + "</div><div style='font-size:11px;color:#64748b;margin-top:5px;'>"
				+ escapeHtml(description) + "</div></div>";
	}

	private static String barHtml(AttendanceSummary summary) {
		int total = summary.totalPertemuan <= 0 ? 1 : summary.totalPertemuan;
		int hadirWidth = (int) Math.round(summary.hadir * 100.0 / total);
		int alpaWidth = (int) Math.round(summary.alpa * 100.0 / total);
		int sakitWidth = (int) Math.round(summary.sakit * 100.0 / total);
		int izinWidth = 100 - hadirWidth - alpaWidth - sakitWidth;
		if (izinWidth < 0) {
			izinWidth = 0;
		}
		return "<div style='font-size:12px;font-weight:800;color:#0f172a;margin-bottom:8px;'>Komposisi absensi</div>"
				+ "<div style='height:16px;border-radius:999px;overflow:hidden;background:#e2e8f0;display:flex;'>"
				+ "<div style='width:" + hadirWidth + "%;background:#16a34a;'></div>"
				+ "<div style='width:" + alpaWidth + "%;background:#dc2626;'></div>"
				+ "<div style='width:" + sakitWidth + "%;background:#ca8a04;'></div>"
				+ "<div style='width:" + izinWidth + "%;background:#7c3aed;'></div></div>"
				+ "<div style='display:flex;gap:10px;flex-wrap:wrap;margin-top:8px;font-size:11px;color:#64748b;'>"
				+ "<span>Hijau: hadir</span><span>Merah: alpa</span><span>Kuning: sakit</span><span>Ungu: izin</span></div>";
	}

	private static String buildEmptyHtml(String message) {
		return "<div style='padding:20px;margin:14px;border:1px dashed #cbd5e1;border-radius:14px;background:#f8fafc;color:#64748b;text-align:center;'>"
				+ escapeHtml(message) + "</div>";
	}

	private static Integer toInteger(Object value) {
		if (value == null) {
			return Integer.valueOf(0);
		}
		if (value instanceof Number) {
			return Integer.valueOf(((Number) value).intValue());
		}
		try {
			return Integer.valueOf(String.valueOf(value));
		} catch (Exception e) {
			return Integer.valueOf(0);
		}
	}

	private static String nvl(String value, String fallback) {
		if (value == null || value.trim().length() == 0) {
			return fallback;
		}
		return value;
	}

	private static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		String data = value;
		data = data.replace("&", "&amp;");
		data = data.replace("<", "&lt;");
		data = data.replace(">", "&gt;");
		data = data.replace("\"", "&quot;");
		data = data.replace("'", "&#39;");
		return data;
	}

	private static class AttendanceSummary {
		int hadir;
		int alpa;
		int sakit;
		int izin;
		int totalTidakHadir;
		int totalPertemuan;
		int jumlahBaris;
		double persenHadir;
	}

}
