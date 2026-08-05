package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
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
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanSksKelasWindow extends MyWindow {

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

	public LaporanSksKelasWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Sks Kelas Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSksKelasWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		searchTahunAjaran = Common.generateTahunAjaran(searchTahunAjaran = new Combobox());
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		jenis_semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenis_semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenis_semester.appendChild(comboitem);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		// setTitle("Rekap Pembayaran Host to Host");
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(this.jenis_semester);
		jenis_semester.setWidth("90%");
		Common.selectComboItem(jenis_semester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		jenis_semester.setReadonly(true);

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "DATA_SKS.xlsx");
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

		List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(Restrictions.isNull("perkuliahan_paralel"))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				.add(jenis_semester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("this_.semester % 2 = "
								+ (jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "0")))

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
		} catch (Exception e4) { ais.common.ErrorAuditUtil.record(e4, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanSksKelasWindow.java:215");

		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, "DATA SKS KELAS");

		Utils.setRowHeight(sheet, 1, 50);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Kelas");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "SKS");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Perkuliahan");

		Utils.setRowHeight(sheet, rowIndex, 50);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		TreeMap<String, List<Perkuliahan>> dosensMap = new TreeMap<String, List<Perkuliahan>>();
		for (Perkuliahan perkuliahan : perkuliahans) {

			String kelas = perkuliahan.getSemester() + " " + perkuliahan.getKelas().trim();
			if (dosensMap.containsKey(kelas)) {
				dosensMap.get(kelas).add(perkuliahan);
			} else {
				List<Perkuliahan> itemDetails = new ArrayList<Perkuliahan>();
				itemDetails.add(perkuliahan);
				dosensMap.put(kelas, itemDetails);
			}

		}

		Integer sksTotal = 0;
		for (String key : dosensMap.keySet()) {
			List<Perkuliahan> dosens = dosensMap.get(key);

			String itemYangDipinjam = "";
			Integer sks = 0;
			for (Perkuliahan kembali : dosens) {
				sks += kembali.getMatakuliah().getSks();
				String s = kembali.getMatakuliah().getKode() + "-" + kembali.getMatakuliah() + "=>"
						+ kembali.getMatakuliah().getSks() + "sks";
				itemYangDipinjam += itemYangDipinjam.isEmpty() ? s : " ," + s;
			}

			sksTotal += sks;

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, key);
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
