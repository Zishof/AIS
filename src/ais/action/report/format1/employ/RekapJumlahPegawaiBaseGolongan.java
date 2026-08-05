package ais.action.report.format1.employ;
import ais.common.PesanFormalHelper;

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
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.StatusPegawai;
import ais.database.model.employ.Golongan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class RekapJumlahPegawaiBaseGolongan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchgolongan = new Combobox();
	private Combobox searchstatus = new Combobox();

	// private Combobox searchfakultas = new Combobox();
	// private Combobox searchjurusan = new Combobox();
	// private Combobox tahunAkademik = new Combobox();
	// private Combobox semesterAbsensi = new Combobox();
	// private Combobox searchsemester = new Combobox();
	// private Combobox searchprogram = new Combobox();
	// private Label angkatan = new Label();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	// public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public RekapJumlahPegawaiBaseGolongan() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Rekap Jumlah Pegawai Base Golongan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public RekapJumlahPegawaiBaseGolongan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Rekap Jumlah Pegawai Base Golongan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void initFakultas() {
		Common.insertCombo(searchgolongan, new String[] { "nama", "id" }, Golongan.class,
				Restrictions.eq("aktif", true));
		Common.insertCombo(searchstatus, new String[] { "nama", "id" }, StatusPegawai.class);
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

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Status Pegawai"));
		row.appendChild(searchstatus);
		searchstatus.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Golongan"));
		row.appendChild(searchgolongan);
		searchgolongan.setWidth("90%");

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
						"Rekap_jumlah_pegawai_base_golongan.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.clear(center);

		StatusPegawai statusPegawai = (StatusPegawai) (searchstatus.getSelectedItem() == null ? null
				: searchstatus.getSelectedItem().getValue());
		Golongan golongan = (Golongan) (searchgolongan.getSelectedItem() == null ? null
				: searchgolongan.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		List<Object[]> unitkerja = new ArrayList<Object[]>();

		String sql = "select " + "b.nama as golongan, " + "count(a.id) as jumlah " + "from pegawai a "
				+ "left join employ.golongan b on (a.golongan_pegawai=b.id) "
				+ "left join status_pegawai c on (a.status_pegawai=b.id) " + " where 1=1 "
				+ (statusPegawai == null ? "" : " and a.status_pegawai= " + statusPegawai.getId())
				+ (golongan == null ? "" : " and a.golongan_pegawai = " + golongan.getId()) + " group by b.nama "
				+ " order by b.nama ";

		System.out.println(sql);
		unitkerja = session.createSQLQuery(sql).list();

		if (unitkerja.size() == 0) {
			return;
		}

		// Center 1 anak -> Vlayout: panel CHART (bar per golongan) di atas, Spreadsheet di bawah.
		org.zkoss.zul.Vlayout centerBox = new org.zkoss.zul.Vlayout();
		centerBox.setWidth("100%"); centerBox.setHeight("100%"); centerBox.setStyle("overflow:auto;");
		centerBox.setParent(center);
		org.zkoss.zul.Html chartPanel = new org.zkoss.zul.Html();
		chartPanel.setParent(centerBox);
		java.util.List<String> chartLabel = new java.util.ArrayList<String>();
		java.util.List<Double> chartVal = new java.util.ArrayList<Double>();
		spreadsheet = new ais.ui.util.MySpreadsheet();
		// Wadah khusus untuk spreadsheet (bukan langsung centerBox) supaya saat diganti Grid
		// nanti (gantiSpreadsheetDenganGrid membersihkan children parent-nya), chartPanel di
		// atasnya TIDAK ikut terhapus.
		org.zkoss.zul.Div spreadsheetBox = new org.zkoss.zul.Div();
		spreadsheetBox.setWidth("100%");
		spreadsheetBox.setVflex("1");
		spreadsheetBox.setParent(centerBox);
		spreadsheet.setParent(spreadsheetBox);
		spreadsheet.setWidth("100%");
		spreadsheet.setVflex("1");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(2);
		spreadsheet.setMaxrows(unitkerja.size() + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(30);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI JUMLAH PEGAWAI \n " + "STATUS PEGAWAI "
						+ (statusPegawai == null ? "SEMUA" : statusPegawai.getNama().toUpperCase()) + "\n GOLONGAN "
						+ (golongan == null ? "SEMUA" : golongan.getNama().toUpperCase()));
		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		Utils.setRowHeight(sheet, 1, 100);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Golongan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, Common.getBahasa("Jumlah"));

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		String namaUnitKejra = "";
		Integer totalSemua = 0;
		for (Object[] objects : unitkerja) {
			if (objects[0] == null)
				continue;
			if (!namaUnitKejra.equals(objects[0].toString())) {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
				namaUnitKejra = objects[0].toString();
			} else {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
			}

			Integer nilai0 = ((Number) (objects[1] == null ? 0 : objects[1])).intValue();
			totalSemua += nilai0;

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, nilai0);
			chartLabel.add(objects[0].toString());
			chartVal.add((double) nilai0);

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/employ/RekapJumlahPegawaiBaseGolongan.java:268");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Rekap Jumlah Pegawai Base Golongan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}

			rowIndex++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, totalSemua);

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		Common.setStyled(sheet);
		spreadsheet.setMaxrows(rowIndex + 1);

		// ── Panel visual: BAR jumlah pegawai per golongan (HTML/CSS, BUKAN JFreeChart) ──
		try {
			double[] cv = new double[chartVal.size()];
			for (int i = 0; i < cv.length; i++) {
				cv[i] = chartVal.get(i);
			}
			chartPanel.setContent("<div style='padding:10px 12px 2px;'>"
					+ ais.ui.util.HtmlChartHelper.barHorizontal(
							"Jumlah Pegawai per Golongan",
							"Melihat berapa banyak pegawai pada tiap golongan, dan golongan mana yang paling banyak.",
							chartLabel.toArray(new String[0]), cv, "#2563eb")
					+ "</div>");
		} catch (Exception eChart) { ais.common.ErrorAuditUtil.record(eChart, "auto-audit(empty-catch) src/ais/action/report/format1/employ/RekapJumlahPegawaiBaseGolongan.java:298");
		}

		colIndex = 0;
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/employ/RekapJumlahPegawaiBaseGolongan.java:305");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Rekap Jumlah Pegawai Base Golongan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
