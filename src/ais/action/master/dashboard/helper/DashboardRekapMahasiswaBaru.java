package ais.action.master.dashboard.helper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
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
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapMahasiswaBaru extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	protected Combobox searchJenisSemester = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Label angkatan = new Label();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private Combobox searchpilihan;

	private String namaKolom;

	private String kolomGroup;

	public DashboardRekapMahasiswaBaru(String namaKolom, String kolomGroup) {
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		Common.selectComboItem(tahunAkademik, tahunAkademikPenerimaanMahasiswaBaru);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		searchJenisSemester.appendChild(comboitem);

		searchJenisSemester.setSelectedItem(comboitem);
		searchJenisSemester.setReadonly(true);
		row.appendChild(searchJenisSemester);
		searchJenisSemester.setWidth("90%");

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		searchpilihan = new Combobox();
		comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Prodi Lulus");
		comboitem.setValue("prodi_lulus");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi I");
		comboitem.setValue("prodi_1");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi II");
		comboitem.setValue("prodi_2");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi III");
		comboitem.setValue("prodi3");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi IV");
		comboitem.setValue("prodi4");
		searchpilihan.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Prodi V");
		comboitem.setValue("prodi5");
		searchpilihan.appendChild(comboitem);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilihan"));
		row.appendChild(searchpilihan);
		searchpilihan.setWidth("90%");

		searchpilihan.setReadonly(true);
		searchpilihan.setSelectedIndex(1);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan);
		angkatan.setValue("(tahun angkatan : semua)");

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Rekap_" + namaKolom + ".xlsx");
			}
		});
		print.setParent(toolbar);

		initSpreadsheet();

	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.clear(center);
		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
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

		if (tahunAkademik == null) {
			return;
		}

		String tahun = tahunAkademik.substring(0, 4);
		this.angkatan.setValue(tahun);

		Session session = HibernateUtil.currentSession();
		List<Object[]> jurusans = new ArrayList<Object[]>();

		List<GeneralValueObject> generalValueObjects = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.groupProperty(kolomGroup)).add(Restrictions.isNotNull(kolomGroup))
				.add(tahun == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahun", Integer.parseInt(tahun)))
				.list();

		Collections.sort(generalValueObjects);

		String sql = "select max(y.nama) as fakultas, max(x.nama) as jurusan, ";

		for (GeneralValueObject generalValueObject : generalValueObjects) {
			if (generalValueObject.getNama() != null && !generalValueObject.getNama().isEmpty()) {
				sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
						+ " and aaa.jenis_kelamin='Laki-laki' then 1 else 0 end) as \""
						+ Common.getBahasaConfig(generalValueObject.getNama().trim()) + " Laki-laki\", ";
				sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
						+ " and aaa.jenis_kelamin='Perempuan' then 1 else 0 end) as \""
						+ Common.getBahasaConfig(generalValueObject.getNama().trim()) + " Perempuan\", ";
				sql += "sum(case when aaa." + namaKolom + " = " + generalValueObject.getId()
						+ " then 1 else 0 end) as \"" + Common.getBahasaConfig(generalValueObject.getNama().trim())
						+ "\", ";
			}
		}

		sql += "sum(case when aaa." + namaKolom
				+ " is not null and aaa.jenis_kelamin='Laki-laki' then 1 else 0 end) as \"Laki-laki\", ";
		sql += "sum(case when aaa." + namaKolom
				+ " is not null and aaa.jenis_kelamin='Perempuan' then 1 else 0 end) as \"Perempuan\", ";

		String pilihan = (String) searchpilihan.getSelectedItem().getValue();

		sql += " sum(case when aaa." + namaKolom
				+ " is not null then 1 else 0 end) as total from biodata_calon_mahasiswa aaa  "
				+ " inner join jurusan x on (aaa." + pilihan + " = x.id  )    "
				+ " inner join fakultas y on (y.id = x.fakultas)      where 1=1 "
				+ (searchJenisSemester.getSelectedItem().getValue() == null ? ""
						: "and aaa.semester_mulai='" + searchJenisSemester.getSelectedItem().getValue() + "' ")
				+ (program == null ? "" : " and aaa.program = '" + program + "'")
				+ (jurusan == null ? "" : " and aaa." + pilihan + " = " + jurusan.getId())
				+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId()) + " and aaa.tahun = " + tahun
				+ " group by x.fakultas,aaa." + pilihan + " order by max(y.nama), max(x.nama) ";

		System.out.println(sql);
		jurusans = Common.ambilSql(sql);

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns((generalValueObjects.size() * 3) + 5);
		spreadsheet.setMaxrows(jurusans.size() + 25);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI MAHASISWA BARU " + searchpilihan.getValue().toUpperCase() + "\n"
						+ (fakultas == null ? "" : fakultas.getNama().toUpperCase() + "\n")
						+ (jurusan == null ? "" : jurusan.getNama().toUpperCase() + "\n") + " TAHUN AKADEMIK "
						+ tahunAkademik + " "
						+ (searchJenisSemester.getSelectedItem().getValue() == null ? ""
								: searchJenisSemester.getSelectedItem().getValue())
						+ "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase()));
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

		int colIndex = 2;
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

		String namaFakultas = "";
		String namaProdi = "";
		Integer[] nilaisLaki = new Integer[generalValueObjects.size()];
		Integer[] nilaisPerempuan = new Integer[generalValueObjects.size()];
		Integer[] nilais = new Integer[generalValueObjects.size()];
		// Integer tidakTerdefinisi = 0;
		Integer totalLaki = 0;
		Integer totalPerempuan = 0;
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

			colIndex = 2;
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswaBaru.java:470");
			}

			rowIndex++;
		}

		try {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
			colIndex = 2;
			for (int i = 0; i < nilais.length; i++) {
				int jum = nilaisLaki[i];
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswaBaru.java:496");
		}

		try {
			ais.ui.util.EcampusUtil.setBorder(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswaBaru.java:505");
		}
		Common.setStyled(sheet);spreadsheet.setMaxrows(rowIndex + 1);
		// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

		colIndex = 0;
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/helper/DashboardRekapMahasiswaBaru.java:515");
		}

	}
}
