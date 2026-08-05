package ais.action.report.helper.statistik;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.List;

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
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.StatusAwalMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanDaftarStatusAwalMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	public LaporanDaftarStatusAwalMahasiswa() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Status Awal Mahasiswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDaftarStatusAwalMahasiswa(String title, String border,
			boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		setClosable(true);
		setTitle("Laporan Data mahasiswa reguler dan mahasiswa transfer untuk masing-masing program studi");
		setWidth("750px");
		setHeight("90%");
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("200px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");
		

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("70%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.insertCombo(searchfakultas, new String[]{"nama", "kode"}, Fakultas.class, Restrictions.eq("aktif", true));
		row.appendChild(searchfakultas);searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		
		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(
						bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
						"Data_mahasiswa_reguler_dan_mahasiswa_transfer_untuk_masing_masing_program_studi.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	// private void

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? null
				: searchfakultas.getSelectedItem().getValue());
		String tahunAjaran = (String) (tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null ? null
				: tahunAkademik.getSelectedItem().getValue());
		if (fakultas == null || tahunAjaran == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();
		List<Jurusan> jurusans = session.createCriteria(Jurusan.class)
				.add(Restrictions.eq("fakultas", fakultas)).list();
		List<StatusAwalMahasiswa> awalMahasiswas = session
				.createCriteria(StatusAwalMahasiswa.class)
				.addOrder(Order.asc("nama")).list();
		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(jurusans.size() + 3);
		spreadsheet
				.setMaxrows((awalMahasiswas.size() * Common.programs.size()) + 8);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 0,
				spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1),
				false);

		ais.ui.util.EcampusUtil.setCellValue(
				sheet,
				1,
				0,
				"Data mahasiswa reguler dan mahasiswa transfer untuk masing-masing program studi tahun akademik "
						.toUpperCase()
						+ tahunAjaran.toUpperCase()
						+ "\n "
						+ ""+"Fakultas"+" " + fakultas.getNama().toUpperCase());
		Utils.setRowHeight(sheet, 1, 100);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1,
				spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		final String color = "#000000";

		int rowIndex = 2;
		int colIndex = 2;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Program");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Jenis Mahasiswa");

		for (Jurusan jurusan : jurusans) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jurusan.getNama());
			Utils.setColumnWidth(sheet, colIndex, 100);
			colIndex++;
		}
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, "Total");
		Utils.setColumnWidth(sheet, colIndex, 100);

		Utils.setRowHeight(sheet, rowIndex, 50);

		ais.ui.util.EcampusUtil.setBold(
				sheet,
				new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;

		for (String program : Common.programs.keySet()) {
			int myIndex = 0;
			for (StatusAwalMahasiswa awalMahasiswa : awalMahasiswas) {
				colIndex = 0;
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex,
						myIndex == 0 ? program : "");
				colIndex = 1;
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex,
						awalMahasiswa.getNama());

				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1,
								rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);

				rowIndex++;
				myIndex++;
			}

		}

		Utils.setAlignment(sheet, new Rect(colIndex, 4, colIndex, rowIndex),
				CellStyle.ALIGN_LEFT);
		Utils.setAlignment(sheet,
				new Rect(colIndex + 1, 4, spreadsheet.getMaxcolumns() - 1,
						rowIndex), CellStyle.ALIGN_RIGHT);
		ais.ui.util.EcampusUtil.setBorder(
				sheet,
				new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);

		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, 2, spreadsheet.getMaxcolumns() - 1,
				2), BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, 3, spreadsheet.getMaxcolumns() - 1,
				3), BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		Utils.setColumnWidth(sheet, 1, 200);
		Utils.setColumnWidth(sheet, 0, 200);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
