package ais.action.master.dashboard.admin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Html;
public class DashboardRekapKunjunganPengguna extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private MyDatebox mulai = new MyDatebox();
	private MyDatebox sampai = new MyDatebox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public DashboardRekapKunjunganPengguna() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardRekapKunjunganPengguna(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private String buildDashboardIntroHtml() {
		StringBuilder html = new StringBuilder(12000);
		html.append("<div style='font-family:Arial,Helvetica,sans-serif;color:#0f172a;background:#f8fafc;padding:14px;box-sizing:border-box;'>");
		html.append("<div style='background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);border-radius:20px;padding:20px;color:white;box-shadow:0 18px 45px rgba(15,23,42,.22);'>");
		html.append("<div style='font-size:24px;font-weight:900;margin-bottom:8px;'>Dasbor Rekap Kunjungan Pengguna</div>");
		html.append("<div style='font-size:13px;line-height:1.6;color:#dbeafe;max-width:980px;'>Rekap kunjungan memperlihatkan pola akses pengguna dalam rentang waktu yang dipilih. Tab Data Excel tetap tersedia untuk detail dan download.</div>");
		html.append("</div>");
		html.append("<div style='display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px;margin-top:14px;'>");
		appendDashboardCard(html, "Filter", "Atur rentang tanggal dan pengguna pada tab Data Excel.");
		appendDashboardCard(html, "Tabel Ringkas", "Data utama disajikan dalam laporan yang bisa dibaca langsung.");
		appendDashboardCard(html, "Download", "File Excel asli tetap tersedia dari toolbar laporan.");
		appendDashboardCard(html, "Pemantauan", "Gunakan ringkasan ini untuk melihat perubahan besar tanpa membuka semua baris.");
		html.append("</div>");
		html.append("<div style='display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:14px;'>");
		html.append("<div style='background:white;border:1px solid #e2e8f0;border-radius:18px;padding:16px;box-shadow:0 12px 28px rgba(15,23,42,.06);'>");
		html.append("<div style='font-size:15px;font-weight:900;margin-bottom:8px;'>Alur Penggunaan</div>");
		html.append("<div style='font-size:12px;color:#64748b;line-height:1.7;'>1. Pilih filter pada tab Data Excel. 2. Klik Proses atau Refresh. 3. Periksa tabel ringkas. 4. Download Excel jika membutuhkan file laporan lengkap.</div>");
		html.append("</div>");
		html.append("<div style='background:white;border:1px solid #e2e8f0;border-radius:18px;padding:16px;box-shadow:0 12px 28px rgba(15,23,42,.06);'>");
		html.append("<div style='font-size:15px;font-weight:900;margin-bottom:8px;'>Catatan</div>");
		html.append("<div style='font-size:12px;color:#64748b;line-height:1.7;'>Tampilan lama tidak dihapus. Spreadsheet tetap dipertahankan agar format unduhan dan perhitungan lama tetap sama.</div>");
		html.append("</div>");
		html.append("</div>");
		html.append("</div>");
		return html.toString();
	}

	private void appendDashboardCard(StringBuilder html, String title, String detail) {
		html.append("<div style='background:white;border:1px solid #e2e8f0;border-radius:18px;padding:16px;box-shadow:0 12px 28px rgba(15,23,42,.06);'>");
		html.append("<div style='font-size:13px;font-weight:900;color:#1d4ed8;margin-bottom:8px;'>").append(title).append("</div>");
		html.append("<div style='font-size:12px;color:#64748b;line-height:1.5;'>").append(detail).append("</div>");
		html.append("</div>");
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

		Tabbox dashboardTabbox = new Tabbox();
		dashboardTabbox.setParent(Common.tampilanScroll(this));
		dashboardTabbox.setWidth("100%");
		dashboardTabbox.setHeight("100%");

		Tabs dashboardTabs = new Tabs();
		dashboardTabs.setParent(dashboardTabbox);

		Tab dashboardTab = new Tab("Dasbor");
		dashboardTab.setParent(dashboardTabs);
		dashboardTab.setSelected(true);

		Tab dataTab = new Tab("Data Excel");
		dataTab.setParent(dashboardTabs);

		Tabpanels dashboardTabpanels = new Tabpanels();
		dashboardTabpanels.setParent(dashboardTabbox);

		Tabpanel dashboardPanel = new ais.ui.util.MyTabpanel();
		dashboardPanel.setStyle("overflow:auto;background:#f8fafc;");
		dashboardPanel.setParent(dashboardTabpanels);

		Html dashboardInfo = new Html(buildDashboardIntroHtml());
		dashboardInfo.setParent(dashboardPanel);

		Tabpanel dataPanel = new ais.ui.util.MyTabpanel();
		dataPanel.setStyle("overflow:auto;background:#ffffff;");
		dataPanel.setParent(dashboardTabpanels);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(dataPanel);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		/* FIX 21-08-2026: tinggi panel filter kurang 52px sehingga baris toolbar
		 * (Proses/Download) terpotong di bagian bawah. Ditambah satu tinggi baris
		 * toolbar ZK. Autoscroll tetap aktif sebagai pengaman bila isi filter
		 * bertambah di kemudian hari. */
		north.setHeight("252px");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		Calendar dateMulai = ais.ui.util.WaktuUtil.getCalendar();
		dateMulai.set(Calendar.MONTH, dateMulai.get(Calendar.MONTH) - 1);
		mulai.setValue(dateMulai.getTime());
		row.appendChild(mulai);
		mulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		sampai.setValue(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(sampai);
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

		ais.action.master.helper.LaporanKunjunganPenggunaHelper.pasangTombolLaporan(
				toolbar, this, mulai, sampai, searchfakultas, searchjurusan, null, null);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
						"Rekap_kunjungan_pengguna.xlsx");
			}
		});
		print.setParent(toolbar);

	}

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

		String dateMulai = Common.databaseDateFormat.get().format(mulai.getValue());
		String dateSelesai = Common.databaseDateFormat.get().format(sampai.getValue());

		List<Object[]> jurusans = new ArrayList<Object[]>();

		String sql = "select " + "max(ac.nama) as fakultas, " + "max(ab.nama) as jurusan, "
				+ "max(a.\"login\") as tanggal, " + "sum(case b.nama when b.nama then 1 else 0 end) as mahasiswa, "
				+ "sum(case c.nama when c.nama then 1 else 0 end) as dosen, "
				+ "sum(case d.usernama when d.usernama then 1 else 0 end) as tbmuser, " + "count(a.id) as total "
				+ "from log_login a " + "left join mahasiswa b on (a.mahasiswa = b.id) "
				+ "left join dosen c on (a.dosen = c.id) "
				+ "left join tbmuser d on (a.tbmuser = d.userid and a.dosen is null and a.mahasiswa is null) "
				+ "left join jurusan ab on (a.jurusan = ab.id  ) " + "left join fakultas ac on (ac.id = a.fakultas)  "
				+ "where date(a.\"login\") between date('" + dateMulai + "') and date('" + dateSelesai
				+ "') and success_status = true  " + (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
				+ (fakultas == null ? "" : " and a.fakultas = " + fakultas.getId())
				+ "group by a.fakultas,a.jurusan,to_char(a.\"login\", 'DD-MM-YYYY') "
				+ "order by a.fakultas,a.jurusan,max(a.\"login\")";

		System.out.println(sql);
		jurusans = Common.ambilSql(sql);

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(7);
		spreadsheet.setMaxrows(jurusans.size() + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI DAFTAR KUNJUNGAN PENGGUNA SISTEM \n " + "" + Common.getBahasaConfig("Fakultas") + " "
						+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
						+ Common.getBahasaConfig("Jurusan") + " "
						+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()) + "\n DARI TANGGAL " + dateMulai
						+ " S.D " + dateSelesai);
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
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Tanggal");
		Utils.setColumnWidth(sheet, 2, 120);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.getBahasa("label_mahasiswa"));
		Utils.setColumnWidth(sheet, 3, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.getBahasa("label_dosen"));
		Utils.setColumnWidth(sheet, 4, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Admin");
		Utils.setColumnWidth(sheet, 5, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Total");
		Utils.setColumnWidth(sheet, 6, 80);

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		String namaFakultas = "";
		String namaProdi = "";

		Integer totalMahasiswa = 0;
		Integer totalDosen = 0;
		Integer totalAdmin = 0;
		Integer totalSemua = 0;
		for (Object[] objects : jurusans) {
			if (objects[0] == null || objects[1] == null)
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

			Date nilai0 = ((Date) (objects[2] == null ? 0 : objects[2]));
			Integer nilai1 = ((Number) (objects[3] == null ? 0 : objects[3])).intValue();
			Integer nilai2 = ((Number) (objects[4] == null ? 0 : objects[4])).intValue();
			Integer nilai3 = ((Number) (objects[5] == null ? 0 : objects[5])).intValue();
			Integer nilai4 = ((Number) (objects[6] == null ? 0 : objects[6])).intValue();

			totalMahasiswa += nilai1;
			totalDosen += nilai2;
			totalAdmin += nilai3;
			totalSemua += nilai4;

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.dateFormat2.get().format(nilai0));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, nilai1);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, nilai2);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, nilai3);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, nilai4);

			rowIndex++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, totalMahasiswa);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, totalDosen);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, totalAdmin);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, totalSemua);

		Common.setStyled(sheet);
		spreadsheet.setMaxrows(rowIndex + 1);
		// Excel mentah -> grid ringan (Book tetap hidup utk tombol Download). Pola B PratinjauXlsxHelper.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

		colIndex = 0;
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 1, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/admin/DashboardRekapKunjunganPengguna.java:409");
		}

	}
}
