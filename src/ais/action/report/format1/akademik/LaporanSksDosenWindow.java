package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanSksDosenWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox searchTahunAjaran;
	private Combobox jenis_semester;

	private Center center = new Center();

	private String tahunAjaran;

	private String jenisSemester;

	private Jurusan jurusanDosen;

	private String program;

	private Combobox searchprogram;

	private Dosen dosen;

	private Fakultas fakultasDosen;

	public LaporanSksDosenWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Sks Dosen Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSksDosenWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	public LaporanSksDosenWindow(String title, String border, boolean closable, String tahunAjaran,
			String jenisSemester, Jurusan jurusanDosen, Fakultas fakultas, String program, Dosen dosen) {
		super(title, border, closable);
		this.tahunAjaran = tahunAjaran;
		this.jenisSemester = jenisSemester;
		this.jurusanDosen = jurusanDosen;
		this.fakultasDosen = fakultas;
		this.program = program;
		this.dosen = dosen;
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(this));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig("SKS Dosen");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("SKS Matakuliah");
		tab2.setVisible(jenisSemester == null);
		tab2.setParent(tabs);

		MyTabConfig tab3 = new MyTabConfig("SKS Kelas");
		tab3.setVisible(jenisSemester == null);
		tab3.setParent(tabs);

		MyTabConfig tab4 = new MyTabConfig("SKS Kurikulum");
		tab4.setVisible(jenisSemester == null);
		tab4.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					LaporanSksMatakuliah laporanRekamanNilai = new LaporanSksMatakuliah();
					laporanRekamanNilai.setHeight("100%");
					laporanRekamanNilai.setWidth("100%");
					laporanRekamanNilai.setParent(tabpanel2);
				}
			}
		});

		final Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
		tabpanel3.setParent(tabpanels);
		tab3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel3.getChildren().size() == 0) {
					LaporanSksKelasWindow laporanRekamanNilai = new LaporanSksKelasWindow();
					laporanRekamanNilai.setHeight("100%");
					laporanRekamanNilai.setWidth("100%");
					laporanRekamanNilai.setParent(tabpanel3);
				}
			}
		});

		final Tabpanel tabpanel4 = new ais.ui.util.MyTabpanel();
		tabpanel4.setParent(tabpanels);
		tab4.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel4.getChildren().size() == 0) {
					LaporanSksKurikulum laporanRekamanNilai = new LaporanSksKurikulum();
					laporanRekamanNilai.setHeight("100%");
					laporanRekamanNilai.setWidth("100%");
					laporanRekamanNilai.setParent(tabpanel4);
				}
			}
		});

		searchTahunAjaran = Common.generateTahunAjaran(searchTahunAjaran = new Combobox());
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		jenis_semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenis_semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenis_semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		jenis_semester.appendChild(comboitem);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		// setTitle("Rekap Pembayaran Host to Host");
		// setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1);

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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(this.searchTahunAjaran);
		searchTahunAjaran.setWidth("90%");

		if (tahunAjaran != null) {
			Common.selectComboItem(true, searchTahunAjaran, tahunAjaran);
			searchTahunAjaran.setDisabled(true);
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(this.jenis_semester);
		jenis_semester.setWidth("90%");
		Common.selectComboItem(jenis_semester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		jenis_semester.setReadonly(true);

		if (jenisSemester != null) {
			Common.selectComboItem(true, jenis_semester, jenisSemester);
			jenis_semester.setDisabled(true);
		}

		searchprogram = Common.initPrograms(null);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(this.searchprogram);
		searchprogram.setWidth("90%");

		if (program != null) {
			Common.selectComboItem(true, searchprogram, program);
			searchprogram.setDisabled(true);
		}

		if (jurusanDosen != null) {
			Common.selectComboItem(true, jurusan, jurusanDosen);
			jurusan.setDisabled(true);

		}
		if (fakultasDosen != null) {
			Common.selectComboItem(true, fakultas, fakultasDosen);
			fakultas.setDisabled(true);
		}

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "DATA_DENDA_ANGGOTA.xlsx");
			}
		});
		print.setParent(toolbar);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
	}

	// private void

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {
		Common.clear(center);

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Criterion criterion = dosen == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("dosen1", dosen), Restrictions.eq("dosen2", dosen));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

		List<Perkuliahan> perkuliahans = criteria

				.add(Restrictions.isNull("perkuliahan_paralel"))

				.add(criterion)

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				.add(jenis_semester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.SP)
								? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
								: Restrictions.and(Restrictions.isNull("statusSemesterPendek"),
										Restrictions.eq("ganjilGenap", jenis_semester.getSelectedItem().getValue())))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

				.createAlias("jurusan", "jurusan")

				.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false))

				.setMaxResults(1048576).addOrder(Order.desc("id")).list();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		if (perkuliahans.size() == 0) {
			return;
		}
		spreadsheet.setMaxcolumns(4);
		spreadsheet.setMaxrows(perkuliahans.size() + 5);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);
		} catch (Exception e4) { ais.common.ErrorAuditUtil.record(e4, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanSksDosenWindow.java:385");

		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, "DATA SKS DOSEN MENGAJAR");

		Utils.setRowHeight(sheet, 1, 50);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Dosen");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "SKS");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Pengajaran");

		Utils.setRowHeight(sheet, rowIndex, 50);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		TreeMap<Dosen, List<Perkuliahan>> dosensMap = new TreeMap<Dosen, List<Perkuliahan>>();
		for (Perkuliahan perkuliahan : perkuliahans) {

			Map<String, Dosen> map = perkuliahan.populateDosen();
			for (Dosen d : map.values()) {
				if (dosensMap.containsKey(d)) {
					dosensMap.get(d).add(perkuliahan);
				} else {
					List<Perkuliahan> itemDetails = new ArrayList<Perkuliahan>();
					itemDetails.add(perkuliahan);
					dosensMap.put(d, itemDetails);
				}
			}

		}

		Double sksTotal = 0.0;
		for (Dosen dosen : dosensMap.keySet()) {
			List<Perkuliahan> perkulishsnasDosen = dosensMap.get(dosen);

			String itemYangDipinjam = "";
			Double sks = 0.0;
			for (Perkuliahan perkul : perkulishsnasDosen) {
				Double sksDibagi = perkul.getMatakuliah().getSks().doubleValue()
						/ perkul.getJumlahDosen().doubleValue();

				sks += sksDibagi;
				String s = perkul.getMatakuliah().getKode() + "-" + perkul.getMatakuliah() + "=> jml dosen: "
						+ perkul.getJumlahDosen() + ", sks mk:" + perkul.getMatakuliah().getSks() + " sks, total: "
						+ Common.numberFormat.get().format(sksDibagi) + "sks";
				itemYangDipinjam += itemYangDipinjam.isEmpty() ? s : " ,\n" + s;
			}

			sksTotal += sks;

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, dosen.getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, sks);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, itemYangDipinjam);

			rowIndex++;
		}

		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "SKS Total");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, sksTotal);

		Utils.setColumnWidth(sheet, 0, 250);
		Utils.setColumnWidth(sheet, 1, 50);
		Utils.setColumnWidth(sheet, 2, 1000);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
