package ais.action.master.dashboard.keuangan;

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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardRekapPembayaranMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox jenisPembayaran = new Combobox();
	private Label angkatan = new Label();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	private Combobox searchstatus = new Combobox();
	private Combobox searchwnawni = new Combobox();

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	private Combobox searchprogram = new Combobox();

	public DashboardRekapPembayaranMahasiswa() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapPembayaranMahasiswa(String title, String border, boolean closable) {
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

		jenisPembayaran = Common.createComboJenisPembayaranDanSemua(jenisPembayaran);

		Common.insertCombo(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		MyComboitemConfig comboitem = new MyComboitemConfig(Mahasiswa.WNI);
		comboitem.setValue(Mahasiswa.WNI);
		searchwnawni.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Mahasiswa.WNA);
		comboitem.setValue(Mahasiswa.WNA);
		searchwnawni.appendChild(comboitem);

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
		north.setHeight("360px");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kewarganegaraan"));
		row.appendChild(searchwnawni);
		searchwnawni.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		row.appendChild(searchstatus);
		searchstatus.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setWidth("90%");
		jenisPembayaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jenisPembayaran.getSelectedItem() == null)
					return;
				if (jenisPembayaran.getValue().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {
					searchfakultas.setDisabled(true);
					searchfakultas.setSelectedItem(null);
					searchjurusan.setDisabled(true);
					searchjurusan.setSelectedItem(null);
					semesterAbsensi.setDisabled(true);
					semesterAbsensi.setSelectedItem(null);
					searchsemester.setDisabled(true);
					searchsemester.setSelectedItem(null);
					searchstatus.setDisabled(true);
					angkatan.setValue("");
				} else if (jenisPembayaran.getValue().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
					searchfakultas.setDisabled(false);
					searchjurusan.setDisabled(false);
					semesterAbsensi.setDisabled(true);
					semesterAbsensi.setSelectedItem(null);
					searchsemester.setDisabled(true);
					searchsemester.setSelectedItem(null);
					searchstatus.setDisabled(true);
					angkatan.setValue("");
				} else {
					searchfakultas.setDisabled(false);
					searchjurusan.setDisabled(false);
					semesterAbsensi.setDisabled(false);
					searchsemester.setDisabled(false);
					searchstatus.setDisabled(false);
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

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
				searchsemester.setSelectedIndex(0);
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
						"Rekap_pembayaran_mahasiswa.xlsx");
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

		JenisKegiatan jenisPembayaran = (JenisKegiatan) (this.jenisPembayaran.getSelectedItem() == null ? null
				: this.jenisPembayaran.getSelectedItem().getValue());

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());
		String wnawni = (String) (searchwnawni.getSelectedItem() == null ? null
				: searchwnawni.getSelectedItem().getValue());

		String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		if (jenisPembayaran == null || tahunAkademik == null) {
			return;
		}
		Boolean semuasemester = searchsemester.getSelectedItem() == null;
		Integer tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
		if (!semuasemester) {
			angkatan.setValue("(tahun angkatan : " + tahunAngkatan + ")");
		}
		List<Object[]> jurusans = new ArrayList<Object[]>();

		if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA)) {

			angkatan.setValue("(tahun angkatan : " + (semuasemester ? "semua" : tahunAngkatan) + ")");

			String sql = "select " + "max(y.nama) as fakultas, " + "max(x.nama) as jurusan, "
					+ "sum(case a.id when b.mahasiswa then 1 else 0 end) as jumlah_sudah_bayar, "
					+ "sum(case a.id when b.mahasiswa then 0 else 1 end) as jumlah_belum_bayar, "
					+ "count(a.id) as total  " +

					"from mahasiswa a " + "left join kegiatan b on (b.jenis_kegiatan = " + jenisPembayaran.getId() + " "
					+ (statusMahasiswa == null ? "" : " and b.status_mahasiswa = " + statusMahasiswa.getId())
					+ " and  a.id = b.mahasiswa and b.tahun_akademik = '" + tahunAkademik + "'  and b.semster "
					+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 ")) + " and b.semster = (case "
					+ semesterKe + " when -1 then b.semster else " + semesterKe + " end) )  "
					+ "left join jurusan x on (a.jurusan = x.id  )  " + "left join fakultas y on (y.id = x.fakultas)  "
					+ "where 1=1 " + (wnawni == null ? "" : "and a.warganegara = '" + wnawni + "'") + " "
					+ (statusMahasiswa == null ? "" : "  and  a.status = " + statusMahasiswa.getId())
					+ "  and a.tahunangkatan = (case " + semuasemester + " when true then a.tahunangkatan else "
					+ tahunAngkatan + " end) " + (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
					+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
					+ (program == null ? "" : " and a.program = '" + program + "'") + " group by x.fakultas,a.jurusan "
					+ " order by max(y.nama), max(x.nama) ";

			System.out.println(sql);
			jurusans = Common.ambilSql(sql);
		} else if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
			tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
			angkatan.setValue("(tahun angkatan : " + (tahunAngkatan) + ")");
			String sql = "select " + "max(y.nama) as fakultas, " + "max(x.nama) as jurusan, "
					+ "sum(case a.id when b.calon_mahasiswa then 1 else 0 end) as jumlah_sudah_bayar, "
					+ "sum(case a.id when b.calon_mahasiswa then 0 else 1 end ) as jumlah_belum_bayar, "
					+ "count(a.id) as total  " +

					"from biodata_calon_mahasiswa a " + "left join kegiatan b on (b.jenis_kegiatan = "
					+ jenisPembayaran.getId() + " and  a.id = b.calon_mahasiswa and b.tahun_akademik = '"
					+ tahunAkademik + "' )  " + "left join jurusan x on (a.prodi_lulus = x.id  )  "
					+ "left join fakultas y on (y.id = x.fakultas)  " + "where 1=1 "
					+ (wnawni == null ? "" : "and a.kewarganegaraan = '" + wnawni + "'") + " "

					+ " and a.tahun = (case " + tahunAngkatan + " when -1 then a.tahun else " + tahunAngkatan + " end) "
					+ (jurusan == null ? "" : " and a.prodi_lulus = " + jurusan.getId())
					+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
					+ (program == null ? "" : " and a.program = '" + program + "'")
					+ " group by x.fakultas,a.prodi_lulus " + " order by max(y.nama), max(x.nama) ";

			System.out.println(sql);
			jurusans = Common.ambilSql(sql);

		} else if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {
			tahunAngkatan = Common.getTahunAngkatan(semesterKe, semester, tahunAkademik);
			angkatan.setValue("(tahun angkatan : " + (tahunAngkatan) + ")");
			String sql = "select " + "sum(case a.id when b.calon_mahasiswa then 1 else 0 end) as jumlah_sudah_bayar, "
					+ "sum(case a.id when b.calon_mahasiswa then 0 else 1 end) as jumlah_belum_bayar " +

					"from biodata_calon_mahasiswa a " + "left join kegiatan b on (b.jenis_kegiatan = "
					+ jenisPembayaran.getId() + " and  a.id = b.calon_mahasiswa and b.tahun_akademik = '"
					+ tahunAkademik + "' )  " + "where a.tahun = (case " + tahunAngkatan + " when -1 then a.tahun else "
					+ tahunAngkatan + " end)";

			System.out.println(sql);
			jurusans = Common.ambilSql(sql);

		} else {
			angkatan.setValue("(tahun angkatan : " + (semuasemester ? "semua" : tahunAngkatan) + ")");

			String sql = "select " + "max(y.nama) as fakultas, " + "max(x.nama) as jurusan, "
					+ "sum(case a.id when b.mahasiswa then 1 else 0 end) as jumlah_sudah_bayar, "
					+ "sum(case a.id when b.mahasiswa then 0 else 1 end) as jumlah_belum_bayar, "
					+ "count(a.id) as total  " +

					"from mahasiswa a " + "left join kegiatan b on (b.jenis_kegiatan = " + jenisPembayaran.getId() + " "
					+ (statusMahasiswa == null ? "" : " and b.status_mahasiswa = " + statusMahasiswa.getId())
					+ " and  a.id = b.mahasiswa and b.tahun_akademik = '" + tahunAkademik + "'  and b.semster "
					+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 ")) + " and b.semster = (case "
					+ semesterKe + " when -1 then b.semster else " + semesterKe + " end) )  "
					+ "left join jurusan x on (a.jurusan = x.id  )  " + "left join fakultas y on (y.id = x.fakultas)  "
					+ "where 1=1 " + (wnawni == null ? "" : "and a.warganegara = '" + wnawni + "'") + " "
					+ (statusMahasiswa == null ? "" : "  and  a.status = " + statusMahasiswa.getId())
					+ "  and a.tahunangkatan = (case " + semuasemester + " when true then a.tahunangkatan else "
					+ tahunAngkatan + " end) " + (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
					+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
					+ (program == null ? "" : " and a.program = '" + program + "'") + " group by x.fakultas,a.jurusan "
					+ " order by max(y.nama), max(x.nama) ";

			System.out.println(sql);
			jurusans = Common.ambilSql(sql);
		}

		// Center 1 anak → Vlayout: panel CHART (donut sudah/belum bayar) di atas,
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
		spreadsheet.setMaxcolumns(5);
		spreadsheet.setMaxrows(jurusans.size() + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI PERBANDINGAN PEMBAYARAN \n " + "" + Common.getBahasaConfig("Fakultas") + " "
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
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Sudah Bayar");
		Utils.setColumnWidth(sheet, 2, 90);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Belum Bayar");
		Utils.setColumnWidth(sheet, 3, 90);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Total");
		Utils.setColumnWidth(sheet, 4, 90);

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		String namaFakultas = "";
		String namaProdi = "";
		Integer totalSudahbayar = 0;
		Integer totalBelumbayar = 0;
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

			totalSudahbayar += nilai0;
			totalBelumbayar += nilai1;
			totalSemua += nilai2;

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, nilai0);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, nilai1);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai2);

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DashboardRekapPembayaranMahasiswa.java:564");
			}

			rowIndex++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, totalSudahbayar);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, totalBelumbayar);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, totalSemua);

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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DashboardRekapPembayaranMahasiswa.java:590");
		}

		// ── Panel visual: DONUT proporsi sudah/belum bayar (HTML/CSS, BUKAN JFreeChart) ──
		try {
			chartPanel.setContent("<div style='padding:10px 12px 2px;'>"
					+ ais.ui.util.HtmlChartHelper.donut(
							"Proporsi Mahasiswa Sudah vs Belum Bayar",
							"Melihat sekilas berapa banyak mahasiswa yang sudah membayar dan yang belum.",
							new String[] { "Sudah Bayar", "Belum Bayar" },
							new double[] { totalSudahbayar, totalBelumbayar },
							new String[] { "#22c55e", "#ef4444" },
							totalSemua + " mhs")
					+ "</div>");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DashboardRekapPembayaranMahasiswa.java:604");
			// chart hanya pelengkap; kegagalannya tidak boleh mengganggu tabel/Excel.
		}

	}
}
