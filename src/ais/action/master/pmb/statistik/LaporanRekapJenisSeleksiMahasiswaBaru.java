package ais.action.master.pmb.statistik;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapJenisSeleksiMahasiswaBaru extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7781970414204679926L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox searchjenjang = new Combobox();
	private Label angkatan = new Label();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	public LaporanRekapJenisSeleksiMahasiswaBaru() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public LaporanRekapJenisSeleksiMahasiswaBaru(String title, String border, boolean closable) {
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
		System.out.println("initFakultas()");

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		// Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
		// Restrictions.or(Restrictions.isNull("aktif"),
		// Restrictions.eq("aktif", true)));
		System.out.println("init()");

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("pilih_jenjang")));
		row.appendChild(searchjenjang);
		searchjenjang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatan);
		angkatan.setValue("(tahun angkatan : semua)");

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
				Filedownload.save(bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
						"Rekap_jenis_seleksi_mahasiswa_baru.xlsx");
			}
		});
		print.setParent(toolbar);

		// initSpreadsheet();

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
		Jenjang jenjang = (Jenjang) (searchjenjang.getSelectedItem() == null
				|| searchjenjang.getSelectedItem().getValue() == null ? null
						: searchjenjang.getSelectedItem().getValue());

		if (tahunAkademik == null) {
			return;
		}

		String tahun = tahunAkademik.substring(0, 4);
		this.angkatan.setValue(tahun);

		Session session = HibernateUtil.currentSession();
		List<Object[]> jurusans = new ArrayList<Object[]>();

		List<JenisSeleksi> jenisSeleksis = ConstantValues.simpleList(
				session.createCriteria(JenisSeleksi.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				JenisSeleksi.class);

		// List<JenisSekolahMahasiswaBaru> jenisSekolahMahasiswaBarus = session
		// .createCriteria(JenisSekolahMahasiswaBaru.class).add(Restrictions.or(Restrictions.isNull("aktif"),
		// Restrictions.eq("aktif", true))).list();

		String sql = "select max(y.nama) as fakultas, " + "max(x.nama) as jurusan, ";

		for (JenisSeleksi jenisSeleksi : jenisSeleksis) {
			sql += "sum(case a.jenis_seleksi when " + jenisSeleksi.getId() + " then 1 else 0 end) as \""
					+ jenisSeleksi.getNama().trim() + "\", ";
		}

		// for (JenisSekolahMahasiswaBaru jenisSekolahMahasiswaBaru :
		// jenisSekolahMahasiswaBarus) {
		// sql += "sum(case a.jenis_sekolah_mahasiswa_baru when "
		// + jenisSekolahMahasiswaBaru.getId()
		// + " then 1 else 0 end) as \""
		// + jenisSekolahMahasiswaBaru.getNama().trim() + "\", ";
		// }

		sql += "count(*) as total  " + "from biodata_calon_mahasiswa a  "
				+ "left join jenis_seleksi b on (a.jenis_seleksi = b.id) "
				+ "left join jurusan x on (a.prodi_lulus = x.id  )  " + "left join fakultas y on (y.id = x.fakultas)  "
				+ " where 1=1 and a.nim is not null "
				+ (jenjang == null ? "" : " and a.jenjang= '" + jenjang.getId() + "'")
				+ (program == null ? "" : " and a.program = '" + program + "'")
				+ (jurusan == null ? "" : " and a.prodi_lulus = " + jurusan.getId())
				+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId()) + " and a.tahun = " + tahun
				+ " group by x.fakultas,a.prodi_lulus order by max(y.nama), max(x.nama) ";

		System.out.println(sql);
		jurusans = Common.ambilSql(sql);

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(jenisSeleksis.size() + 3);
		spreadsheet.setMaxrows(jurusans.size() + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI JENIS SELEKSI MAHASISWA BARU \n " + "" + Common.getBahasa("upper_jenjang") + " "
						+ (jenjang == null ? "SEMUA" : jenjang.getNama().toUpperCase()) + "\n"
						+ Common.getBahasaConfig("Fakultas") + " "
						+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
						+ Common.getBahasaConfig("Jurusan") + " "
						+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()) + "\n TAHUN AKADEMIK "
						+ tahunAkademik + "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase()));
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
		for (JenisSeleksi jenisSeleksi : jenisSeleksis) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jenisSeleksi.getNama());
			Utils.setColumnWidth(sheet, colIndex, 100);
			colIndex++;
		}

		// ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex,
		// "Tidak Terdefinisi");
		// Utils.setColumnWidth(sheet, colIndex, 100);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, "Total");
		Utils.setColumnWidth(sheet, colIndex, 100);

		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

		rowIndex = 3;
		colIndex = 0;

		String namaFakultas = "";
		String namaProdi = "";
		Integer[] nilais = new Integer[jenisSeleksis.size()];
		// Integer tidakTerdefinisi = 0;
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
			for (@SuppressWarnings("unused")
			JenisSeleksi jenisSeleksi : jenisSeleksis) {
				if (nilais[colIndex - 2] == null) {
					nilais[colIndex - 2] = 0;
				}
				Integer nilai0 = ((Number) (objects[colIndex] == null ? 0 : objects[colIndex])).intValue();
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, nilai0);
				nilais[colIndex - 2] += nilai0;
				colIndex++;
			}
			Integer tot = ((Number) (objects[objects.length - 1] == null ? 0 : objects[objects.length - 1])).intValue();

			// tidakTerdefinisi += td;
			// tot = tot + td;
			total += tot;

			// ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex,
			// td);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, tot);

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/statistik/LaporanRekapJenisSeleksiMahasiswaBaru.java:387");
			}

			rowIndex++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		colIndex = 2;
		for (Integer jum : nilais) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jum);
			colIndex++;
		}
		// ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex,
		// tidakTerdefinisi);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, total);

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		Common.setStyled(sheet);
		spreadsheet.setMaxrows(rowIndex + 1);

		colIndex = 0;
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/statistik/LaporanRekapJenisSeleksiMahasiswaBaru.java:416");
		}

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
