package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardStatistikJadwalKurikulum extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Div center;

	public DashboardStatistikJadwalKurikulum() {
		super();
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
				"Pilih saringan untuk menyesuaikan data jadwal & kurikulum yang ditampilkan.",
				"Statistik Jadwal Kurikulum",
				"Sebaran jadwal/kurikulum per program studi, beserta grafiknya.");
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
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_Kurikulum.xlsx");
			}
		});
		print.setParent(toolbar);

		initSpreadsheet();

	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.clear(center);

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

		Session session = HibernateUtil.currentSession();
		List<Object[]> jurusans = new ArrayList<Object[]>();

		List<String> tahunAkademiks = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.groupProperty("tahunAjaran")).add(Restrictions.isNotNull("tahunAjaran"))
				.addOrder(Order.asc("tahunAjaran")).list();

		String sql = "select max(y.nama) as fakultas, max(x.nama) as jurusan, max(z.nama) as kurikulum, ";

		String[] jenisSemester = new String[] { Perkuliahan.GANJIL, Perkuliahan.GENAP };

		for (String tahunAkademik : tahunAkademiks) {
			for (String j : jenisSemester) {
				sql += "sum(case when aaa.tahun_ajaran = '" + tahunAkademik + "' and aaa.semester"
						+ (j.equals(Perkuliahan.GANJIL) ? "%2=1" : "%2=0") + "  then 1 else 0 end) as \""
						+ tahunAkademik + j + "\", ";
			}
		}

		String pilihan = "jurusan";

		sql += " sum(case when aaa.tahun_ajaran is not null then 1 else 0 end) as total from perkuliahan aaa  "
				+ " inner join jurusan x on (aaa." + pilihan + " = x.id  )    "
				+ " inner join fakultas y on (y.id = x.fakultas)  "
				+ " inner join kurikulum z on (z.id = aaa.kurikulum)   where 1=1 "
				+ (program == null ? "" : " and aaa.program = '" + program + "'")
				+ (jurusan == null ? "" : " and aaa." + pilihan + " = " + jurusan.getId())
				+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId()) + "  group by x.fakultas,aaa."
				+ pilihan + ",aaa.kurikulum  having sum(case when aaa.tahun_ajaran is not null then 1 else 0 end)>0 "
				+ " order by max(y.nama), max(x.nama),max(z.nama) ";

		System.out.println(sql);
		jurusans = Common.ambilSql(sql);

		// Center hanya boleh 1 anak → bungkus Vlayout: panel CHART (bar per periode) di atas,
		// Spreadsheet (tabel + sumber Download Excel) tetap di bawah.
		org.zkoss.zul.Vlayout centerBox = new org.zkoss.zul.Vlayout();
		centerBox.setWidth("100%");
		centerBox.setHeight("100%");
		centerBox.setStyle("overflow:auto;");
		centerBox.setParent(center);

		org.zkoss.zul.Html chartPanel = new org.zkoss.zul.Html();
		chartPanel.setParent(centerBox);

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(centerBox);
		spreadsheet.setWidth("100%");
		spreadsheet.setVflex("1");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns((tahunAkademiks.size() * 1 * 2) + 5);
		spreadsheet.setMaxrows(jurusans.size() + 25);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI KURIKULUM\n" + (fakultas == null ? "" : fakultas.getNama().toUpperCase() + "\n")
						+ (jurusan == null ? "" : jurusan.getNama().toUpperCase() + "\n") + "\nPROGRAM "
						+ (program == null ? "SEMUA" : program.toUpperCase()));
		final String color = "#000000";
		int rowIndex = 2;

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
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Kurikulum");
		Utils.setColumnWidth(sheet, 2, 300);

		int colIndex = 3;
		for (String tahunAkademik : tahunAkademiks) {
			for (String j : jenisSemester) {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, tahunAkademik + "/" + j);
				Utils.setColumnWidth(sheet, colIndex, 80);
				colIndex += 1;
			}
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, "Total");
		Utils.setColumnWidth(sheet, colIndex, 80);

		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

		rowIndex = 3;
		colIndex = 0;

		String namaFakultas = "";
		String namaProdi = "";
		Integer[] nilais = new Integer[tahunAkademiks.size() * 2];

		Integer total = 0;
		for (Object[] objects : jurusans) {
			if (objects[0] != null) {
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
			} else {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Tidak pilih " + "Fakultas");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
						"Tidak pilih " + Common.getBahasaConfig("Jurusan"));
			}

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, objects[2].toString());

			colIndex = 3;
			int index = 0;
			for (@SuppressWarnings("unused")
			String tahunAkademik : tahunAkademiks) {
				for (@SuppressWarnings("unused")
				String j : jenisSemester) {

					Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);

					if (nilais[index] == null) {
						nilais[index] = 0;
					}

					nilais[index] += nilai0;
					colIndex++;

					index++;
				}
			}

			Integer td = ((Number) (objects[objects.length - 1] == null ? 0 : objects[objects.length - 1])).intValue();
			total += td;
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardStatistikJadwalKurikulum.java:343");
			}

			rowIndex++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		colIndex = 3;
		for (int i = 0; i < nilais.length; i++) {
			int jum = nilais[i];
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
			if (jum == 0) {
				try {
					Utils.setColumnWidth(sheet, colIndex, 0);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardStatistikJadwalKurikulum.java:357");
				}

			}
			colIndex++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, total);

		try {
			ais.ui.util.EcampusUtil.setBorder(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardStatistikJadwalKurikulum.java:372");
		}
		Common.setStyled(sheet);spreadsheet.setMaxrows(rowIndex + 1);
		// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

		colIndex = 0;
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardStatistikJadwalKurikulum.java:382");
		}

		// ── Panel visual: BAR jumlah jadwal per periode (HTML/CSS, BUKAN JFreeChart) ──
		// nilais[] = total jadwal per periode (tahun akademik x jenis semester), urut sama
		// dengan loop pengisian di atas. Memberi gambaran cepat periode mana paling padat.
		try {
			java.util.List<String> periodeLabel = new java.util.ArrayList<String>();
			for (String ta : tahunAkademiks) {
				for (String js : jenisSemester) {
					periodeLabel.add(ta + " " + js);
				}
			}
			double[] periodeVal = new double[nilais.length];
			for (int i = 0; i < nilais.length; i++) {
				periodeVal[i] = nilais[i] == null ? 0 : nilais[i];
			}
			chartPanel.setContent("<div style='padding:10px 12px 2px;'>"
					+ ais.ui.util.HtmlChartHelper.barHorizontal(
							"Jumlah Jadwal Perkuliahan per Periode",
							"Melihat pada tahun akademik dan semester mana paling banyak jadwal perkuliahan "
									+ "dibuka, sehingga mudah dibandingkan antar periode.",
							periodeLabel.toArray(new String[0]), periodeVal, "#2563eb")
					+ "</div>");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardStatistikJadwalKurikulum.java:406");
			// chart hanya pelengkap; kegagalannya tidak boleh mengganggu tabel/Excel.
		}

	}
}
