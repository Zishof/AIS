package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapSilabusPerkuliahan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchprogram = new Combobox();

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Div center;

	public DashboardRekapSilabusPerkuliahan() {
		super();
		try {
			init();
			initFakultas();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapSilabusPerkuliahan(String title, String border, boolean closable) {
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

		/* Portal responsif (menumpuk di HP) menggantikan Borderlayout North+Center. */
		Component[] hostPortal = ais.ui.util.DasborResponsifHelper.saringanDanIsi(this,
				"Saringan Data",
				"Pilih saringan untuk menyesuaikan data yang ditampilkan.",
				"Rekap Silabus Perkuliahan",
				"Rekap kelengkapan silabus perkuliahan per kelas, beserta grafiknya.");
		Component saringanHost = hostPortal[0];
		center = (Div) hostPortal[1];

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(saringanHost);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		for (int i = 1; i < 10; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			searchsemester.appendChild(comboitem);
		}

		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchsemester.appendChild(comboitem);
		searchsemester.setSelectedItem(comboitem);
		searchsemester.setReadonly(true);

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheetdata();
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
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_Data.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	public static String generateWhere(Integer semesterKe, String program, Jurusan jurusan, Fakultas fakultas) {

		String sql = " from kurikulum_punya_matakuliah_detail a  "
				+ " inner join kurikulum_punya_matakuliah b on (a.kurikulum_punya_matakuliah=b.id) "
				+ " inner join kurikulum b1 on (b.kurikulum=b1.id)  "
				+ " inner join matakuliah c on (b.matakuliah = c.id)  "
				+ " left join jurusan x on (b1.jurusan = x.id  ) "
				+ " left join (select count(*) as qty,kurikulum_punya_matakuliah  from kurikulum_punya_matakuliah_punya_item group by kurikulum_punya_matakuliah) pi on (a.kurikulum_punya_matakuliah=pi.kurikulum_punya_matakuliah) ";

		sql += " where 1=1 " + (semesterKe == null ? "" : "  and b.semester = " + semesterKe)
				+ (program == null ? "" : " and b1.program = '" + program + "'")
				+ (jurusan == null ? "" : " and b1.jurusan = " + jurusan.getId())
				+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())

				+ " group by b.id order by  max(b1.tahun) desc,max(x.nama), max(b.semester) ";

		return sql;
	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheetdata() {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? null
								: searchfakultas.getSelectedItem().getValue());
				Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
						|| searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? null
								: searchjurusan.getSelectedItem().getValue());

				Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? null
						: searchsemester.getSelectedItem().getValue());
				String program = (String) (searchprogram.getSelectedItem() == null
						|| searchprogram.getSelectedItem().getValue() == null ? null
								: searchprogram.getSelectedItem().getValue());

				List<Object[]> jurusans = new ArrayList<Object[]>();

				String sql = "select b.id, max(x.nama) as prodi,  max(b1.program) as program, max(b1.tahun) as tahun, max(b1.nama) as kurikulum, max(b.semester) as semester, max(c.kode||'-'||c.nama||' ') as info, "
						+ "count(*) as qty_kurikulum_punya_matakuliah_detail, max(pi.qty) as jumlah "
						+ DashboardRekapSilabusPerkuliahan.generateWhere(semesterKe, program, jurusan, fakultas);

				System.out.println(sql);
				jurusans = Common.ambilSql(sql);

				spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);
				spreadsheet.setParent(center);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns(11);
				spreadsheet.setMaxrows(jurusans.size() + 4);

				Worksheet sheet = spreadsheet.getSelectedSheet();
				sheet.setDefaultColumnWidth(40);
				EcampusUtil.setBold(sheet,
						new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

				EcampusUtil.setCellValue(sheet, 1, 0,
						"REKAPITULASI SILABUS PERKULIAHAN \n " + Common.getBahasaConfig("Fakultas") + " "
								+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
								+ Common.getBahasaConfig("Jurusan") + " "
								+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()));
				final String color = "#000000";
				int rowIndex = 2;
				int colIndex = 0;
				Utils.setRowHeight(sheet, 1, 150);
				EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
				Cell cell = Utils.getCell(sheet, 1, 0);
				cell.getCellStyle().setWrapText(true);
				cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

				EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
				EcampusUtil.setCellValue(sheet, rowIndex, 0, Common.getBahasaConfig("Jurusan"));
				Utils.setColumnWidth(sheet, 0, 200);
				EcampusUtil.setCellValue(sheet, rowIndex, 1, "Program");
				Utils.setColumnWidth(sheet, 1, 80);
				EcampusUtil.setCellValue(sheet, rowIndex, 2, "Tahun");
				Utils.setColumnWidth(sheet, 2, 80);
				EcampusUtil.setCellValue(sheet, rowIndex, 3, "Kurikulum");
				Utils.setColumnWidth(sheet, 3, 200);
				EcampusUtil.setCellValue(sheet, rowIndex, 4, "Semester");
				Utils.setColumnWidth(sheet, 4, 200);
				EcampusUtil.setCellValue(sheet, rowIndex, 5, "Matakuliah");
				Utils.setColumnWidth(sheet, 5, 200);
				EcampusUtil.setCellValue(sheet, rowIndex, 6, "Qty Pertemuan");
				Utils.setColumnWidth(sheet, 6, 80);
				EcampusUtil.setCellValue(sheet, rowIndex, 7, "Qty File");
				Utils.setColumnWidth(sheet, 7, 80);
				EcampusUtil.setCellValue(sheet, rowIndex, 8, "Qty Audio");
				Utils.setColumnWidth(sheet, 8, 80);
				EcampusUtil.setCellValue(sheet, rowIndex, 9, "Qty Video");
				Utils.setColumnWidth(sheet, 9, 80);
				EcampusUtil.setCellValue(sheet, rowIndex, 10, "Qty Buku ref");
				Utils.setColumnWidth(sheet, 10, 80);

				EcampusUtil.setBorder(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
						BookHelper.BORDER_FULL, BorderStyle.THIN, color);
				EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
						true);

				rowIndex = 3;
				colIndex = 0;

				Session streamSession = StreamingHibernateUtil.getInstance().currentSession();
				for (Object[] objects : jurusans) {
					if (objects[0] == null)
						continue;
					Long perkulaiahnId = ((Number) objects[0]).longValue();

					EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[1] == null ? "" : objects[1].toString());

					EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[2] == null ? "" : objects[2].toString());

					EcampusUtil.setCellValue(sheet, rowIndex, 2, objects[3] == null ? "" : objects[3].toString());

					EcampusUtil.setCellValue(sheet, rowIndex, 3, objects[4] == null ? "" : objects[4].toString());

					EcampusUtil.setCellValue(sheet, rowIndex, 4, objects[5] == null ? "" : objects[5].toString());

					EcampusUtil.setCellValue(sheet, rowIndex, 5, objects[6] == null ? "" : objects[6].toString());

					EcampusUtil.setCellValue(sheet, rowIndex, 6, objects[7] == null ? "" : objects[7].toString());

					Session session = HibernateUtil.currentSession();
					List<Long> kurikulum_punya_matakuliah_details = session
							.createCriteria(KurikulumPunyaMatakuliahDetail.class)
							.setProjection(Projections.groupProperty("id"))
							.add(Restrictions.eq("kurikulumPunyaMatakuliah.id", perkulaiahnId)).list();

					String pert = "";
					for (Long p : kurikulum_punya_matakuliah_details) {
						pert += pert.isEmpty() ? p + "" : "," + p;
					}

					sql = "select (select count(id) from pertemuan_file_content where kurikulumpunyamatakuliahdetail in("
							+ pert
							+ ")) as file,(select count(id) from audio_pertemuan where kurikulumpunyamatakuliahdetail in("
							+ pert
							+ ")) as audio, (select count(id) from video_pertemuan where kurikulumpunyamatakuliahdetail in("
							+ pert + ")) as video";
					List<Object[]> b = streamSession.createSQLQuery(sql).list();

					if (objects != null && b.size() != 0) {
						Object[] numbers = b.get(0);

						Number file = (Number) numbers[0];
						Number audio = (Number) numbers[1];
						Number video = (Number) numbers[2];
						EcampusUtil.setCellValue(sheet, rowIndex, 7, file);
						EcampusUtil.setCellValue(sheet, rowIndex, 8, audio);
						EcampusUtil.setCellValue(sheet, rowIndex, 9, video);
					}

					EcampusUtil.setCellValue(sheet, rowIndex, 10, objects[8] == null ? "" : objects[8].toString());

					try {
						EcampusUtil.setBorder(sheet,
								new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapSilabusPerkuliahan.java:348");
					}

					rowIndex++;
				}

				StreamingHibernateUtil.getInstance().closeSession();

				Common.setStyled(sheet);
				spreadsheet.setMaxrows(rowIndex + 1);
				// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

				colIndex = 0;
				try {
					EcampusUtil.setBold(sheet,
							new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex),
							true);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapSilabusPerkuliahan.java:366");
				}
			}
		});

	}
}
