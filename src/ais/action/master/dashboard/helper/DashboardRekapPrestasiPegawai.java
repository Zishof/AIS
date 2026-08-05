package ais.action.master.dashboard.helper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PrestasiPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapPrestasiPegawai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private AmbilDataSatuanKerjaBanbox searchparent;
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private String namaKolom;

	private String kolomGroup;

	private MyDatebox mulai;

	private MyDatebox sampai;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	public DashboardRekapPrestasiPegawai(String namaKolom, String kolomGroup) {
		super();
		this.namaKolom = namaKolom;
		this.kolomGroup = kolomGroup;
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void initFakultas() {

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
		north.setHeight("160px");
		north.setAutoscroll(true);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchparent = new AmbilDataSatuanKerjaBanbox());
		searchparent.setWidth("90%");

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, 1);
		calendar.set(Calendar.MONTH, 0);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));

		row.appendChild(mulai = new MyDatebox(calendar.getTime()));
		mulai.setCols(6);
		mulai.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		row.appendChild(sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		sampai.setCols(6);
		sampai.setReadonly(true);

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
						"Rekap_" + namaKolom + ".xlsx");
			}
		});
		print.setParent(toolbar);

		initSpreadsheet();

	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.clear(center);

		String inSatker = "";
		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		if (parent != null) {
			Set<SatuanKerja> temp = new HashSet<SatuanKerja>();
			if (parent != null) {
				temp.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, temp);
			}
			for (SatuanKerja satuanKerja : temp) {
				inSatker += inSatker.isEmpty() ? satuanKerja.getId().toString() : "," + satuanKerja.getId();
			}
		}

		String satker = inSatker.isEmpty() ? " and true" : " and satuan_kerja in (" + inSatker + ")";

		Session session = HibernateUtil.currentSession();
		List<Object[]> jurusans = new ArrayList<Object[]>();

		List<GeneralValueObject> generalValueObjects = session.createCriteria(PrestasiPegawai.class)
				.setProjection(Projections.groupProperty(kolomGroup)).add(Restrictions.isNotNull(kolomGroup))
				.add(Restrictions.between("tanggal", mulai.getValue(), sampai.getValue())).list();

		Collections.sort(generalValueObjects);

		String sql = "select max(y.nama) as satuan_kerja,  ";

		for (GeneralValueObject generalValueObject : generalValueObjects) {
			if (generalValueObject.getNama() != null && !generalValueObject.getNama().isEmpty()) {
				sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
						+ " and m.kelamin='Laki-laki' then 1 else 0 end) as \""
						+ Common.getBahasaConfig(generalValueObject.getNama().trim()) + " Laki-laki\", ";
				sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
						+ " and m.kelamin='Perempuan' then 1 else 0 end) as \""
						+ Common.getBahasaConfig(generalValueObject.getNama().trim()) + " Perempuan\", ";
				sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
						+ " then 1 else 0 end) as \"" + Common.getBahasaConfig(generalValueObject.getNama().trim())
						+ "\", ";
			}
		}

		sql += "sum(case when aaa." + namaKolom
				+ " is not null and m.kelamin='Laki-laki' then 1 else 0 end) as \"Laki-laki\", ";
		sql += "sum(case when aaa." + namaKolom
				+ " is not null and m.kelamin='Perempuan' then 1 else 0 end) as \"Perempuan\", ";

		sql += " sum(case when aaa." + namaKolom
				+ " is not null then 1 else 0 end) as total from prestasi_pegawai aaa  "
				+ " inner join pegawai m on (aaa.pegawai = m.id  ) left join rab.satuan_kerja y on (y.id=m.satuan_kerja) where 1=1 and status='Disetujui' "
				+ satker + " and aaa.tanggal between date('" + Common.databaseDateFormat.get().format(mulai.getValue())
				+ "') and date('" + Common.databaseDateFormat.get().format(sampai.getValue())
				+ "') group by m.satuan_kerja order by max(y.nama) ";

		System.out.println(sql);
		jurusans = Common.ambilSql(sql);

		// Center 1 anak -> Vlayout: panel CHART (donut L/P) di atas, Spreadsheet (tabel + Download Excel) di bawah.
		org.zkoss.zul.Vlayout centerBox = new org.zkoss.zul.Vlayout();
		centerBox.setWidth("100%"); centerBox.setHeight("100%"); centerBox.setStyle("overflow:auto;");
		centerBox.setParent(center);
		org.zkoss.zul.Html chartPanel = new org.zkoss.zul.Html();
		chartPanel.setParent(centerBox);
		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(centerBox);
		spreadsheet.setWidth("100%");
		spreadsheet.setVflex("1");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns((generalValueObjects.size() * 3) + 5);
		spreadsheet.setMaxrows(jurusans.size() + 25);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, "REKAPITULASI PRESTASI PEGAWAI");
		final String color = "#000000";
		int rowIndex = 2;

		Utils.setRowHeight(sheet, 1, 150);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Satuan Kerja");
		Utils.setColumnWidth(sheet, 0, 200);

		int colIndex = 1;
		for (GeneralValueObject generalValueObject : generalValueObjects) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, generalValueObject.getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, generalValueObject.getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, generalValueObject.getNama());

			ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 2, false);

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex, "Laki-laki");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 1, "Perempuan");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 2, "Total");
			Utils.setColumnWidth(sheet, colIndex, 70);
			Utils.setColumnWidth(sheet, colIndex + 1, 70);
			Utils.setColumnWidth(sheet, colIndex + 2, 70);
			colIndex += 3;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, "Total");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, "Total");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, "Total");

		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 2, false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex, "Laki-laki");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 1, "Perempuan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 2, "Total");
		// Utils.setColumnWidth(sheet, colIndex + 1, 100);
		Utils.setColumnWidth(sheet, colIndex, 70);
		Utils.setColumnWidth(sheet, colIndex + 1, 70);
		Utils.setColumnWidth(sheet, colIndex + 2, 70);

		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

		rowIndex = 4;
		colIndex = 0;

		String namaSatuanKerja = "";
		Integer[] nilaisLaki = new Integer[generalValueObjects.size()];
		Integer[] nilaisPerempuan = new Integer[generalValueObjects.size()];
		Integer[] nilais = new Integer[generalValueObjects.size()];
		// Integer tidakTerdefinisi = 0;
		Integer totalLaki = 0;
		Integer totalPerempuan = 0;
		Integer total = 0;
		for (Object[] objects : jurusans) {
			if (objects[0] != null) {
				if (!namaSatuanKerja.equals(objects[0].toString())) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
					namaSatuanKerja = objects[0].toString();
				} else {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
				}

			} else {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Tidak pilih " + "Satuan Kerja");
			}

			colIndex = 1;
			int index = 0;
			for (@SuppressWarnings("unused")
			GeneralValueObject generalValueObject : generalValueObjects) {
				if (nilaisLaki[index] == null) {
					nilaisLaki[index] = 0;
				}
				Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
				nilaisLaki[index] += nilai0;
				colIndex++;

				if (nilaisPerempuan[index] == null) {
					nilaisPerempuan[index] = 0;
				}
				nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
				nilaisPerempuan[index] += nilai0;
				colIndex++;

				if (nilais[index] == null) {
					nilais[index] = 0;
				}
				nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
				nilais[index] += nilai0;
				colIndex++;

				index++;
			}

			Integer td = ((Number) (objects[objects.length - 3] == null ? 0 : objects[objects.length - 3])).intValue();
			totalLaki += td;
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);
			colIndex++;

			td = ((Number) (objects[objects.length - 2] == null ? 0 : objects[objects.length - 2])).intValue();
			totalPerempuan += td;
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);
			colIndex++;

			td = ((Number) (objects[objects.length - 1] == null ? 0 : objects[objects.length - 1])).intValue();
			total += td;
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, td);

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapPrestasiPegawai.java:380");
			}

			rowIndex++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		colIndex = 2;
		for (int i = 0; i < nilais.length; i++) {
			Integer jum = nilaisLaki[i];
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
			colIndex++;

			jum = nilaisPerempuan[i];
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
			colIndex++;

			jum = nilais[i];
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
			colIndex++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, totalLaki);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, totalPerempuan);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, total);

		try {
			ais.ui.util.EcampusUtil.setBorder(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapPrestasiPegawai.java:412");
		}
		Common.setStyled(sheet);
		spreadsheet.setMaxrows(rowIndex + 1);
		// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

		colIndex = 0;
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapPrestasiPegawai.java:423");
		}

		// ── Panel visual: DONUT komposisi gender pegawai berprestasi (HTML/CSS, BUKAN JFreeChart) ──
		try {
			chartPanel.setContent("<div style='padding:10px 12px 2px;'>"
					+ ais.ui.util.HtmlChartHelper.donut(
							"Komposisi Pegawai Berprestasi (Laki-laki vs Perempuan)",
							"Melihat sekilas perbandingan jumlah pegawai berprestasi laki-laki dan perempuan.",
							new String[] { "Laki-laki", "Perempuan" },
							new double[] { totalLaki, totalPerempuan },
							new String[] { "#2563eb", "#ec4899" },
							total + " prestasi")
					+ "</div>");
		} catch (Exception eChart) { ais.common.ErrorAuditUtil.record(eChart, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapPrestasiPegawai.java:437");
		}

	}
}
