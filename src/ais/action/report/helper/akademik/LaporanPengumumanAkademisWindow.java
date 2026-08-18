package ais.action.report.helper.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Range;
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
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanPengumumanAkademisWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368331375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private List<PengumumanAkademis> pengumumanAkademis = new ArrayList<PengumumanAkademis>();

	public LaporanPengumumanAkademisWindow() {
		super();
		try {
			initFakultas();
			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pengumuman Akademis Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		try {
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pengumuman Akademis Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPengumumanAkademisWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		initFakultas();
		init();
		try {
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pengumuman Akademis Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void initFakultas() {
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			Common.clear(searchjurusan);
			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			searchfakultas.setDisabled(true);
		} else {
			searchfakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			searchjurusan.setDisabled(true);
		} else {
			searchjurusan.setDisabled(false);
		}

	}

	@SuppressWarnings("deprecation")
	private void init() {

		setClosable(true);
		setTitle("Kalender Akademik");
		setWidth("90%");
		setHeight("90%");
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
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "daftar_hadir_mahasiswa.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initMahasiswa() throws Exception {
		pengumumanAkademis = null;
		if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				|| tahunAkademik.getSelectedItem() == null) {
			return;
		}
		Session session = HibernateUtil.currentSession();

		Fakultas fakultas = (Fakultas) searchfakultas.getSelectedItem().getValue();
		Jurusan jurusan = (Jurusan) searchjurusan.getSelectedItem().getValue();
		String tahunAjaran = (String) tahunAkademik.getSelectedItem().getValue();
		pengumumanAkademis = session.createCriteria(PengumumanAkademis.class).addOrder(Order.asc("tanggal"))
				.add(Restrictions.eq("fakultas", fakultas)).add(Restrictions.eq("tahunAjaran", tahunAjaran))
				.add(Restrictions.eq("jurusan", jurusan)).list();
	}

	// private void

	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				|| tahunAkademik.getSelectedItem() == null) {
			return;
		}
		Fakultas fakultas = (Fakultas) searchfakultas.getSelectedItem().getValue();
		Jurusan jurusan = (Jurusan) searchjurusan.getSelectedItem().getValue();
		String tahunAjaran = (String) tahunAkademik.getSelectedItem().getValue();
		initMahasiswa();
		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(6);
		spreadsheet.setMaxrows(pengumumanAkademis.size() + 12);
		final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 4;
		int colIndex = 3;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "No.");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Kegiatan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Tanggal Mulai");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Tanggal Selesai");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Waktu");

		Utils.setColumnWidth(sheet, 5, 100);
		Utils.setColumnWidth(sheet, 4, 200);
		Utils.setColumnWidth(sheet, 3, 200);
		Utils.setColumnWidth(sheet, 2, 400);
		Utils.setColumnWidth(sheet, 1, 30);
		Utils.setColumnWidth(sheet, 0, 0);
		Utils.setRowHeight(sheet, 4, 50);
		Utils.setRowHeight(sheet, 2, 1);
		try {
			String tahunAkademik = tahunAjaran;
			ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1,
					"KALENDER AKADEMIK\n " + "" + "Fakultas" + " " + fakultas.getNama().toUpperCase() + "\n "
							+ "Jurusan" + " " + jurusan.getNama().toUpperCase() + "\n TAHUN AKADEMIK " + tahunAkademik);
			Utils.setRowHeight(sheet, 1, 100);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
			Cell cell = Utils.getCell(sheet, 1, 1);
			cell.getCellStyle().setWrapText(true);
			cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pengumuman Akademis Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(1, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		rowIndex = 6;
		colIndex = 2;
		int index = 1;
		for (PengumumanAkademis pengumumanAkademis : this.pengumumanAkademis) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, pengumumanAkademis.getId());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, index);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, pengumumanAkademis.getCatatan());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1,
					Common.dateFormat2.get().format(pengumumanAkademis.getTanggal()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2,
					Common.dateFormat2.get().format(pengumumanAkademis.getSampai()));
			long diff = 0L;
			if (pengumumanAkademis.getTanggal() != null && pengumumanAkademis.getSampai() != null) {
				diff = pengumumanAkademis.getSampai().getTime() - pengumumanAkademis.getTanggal().getTime();
				diff = (diff / (1000 * 60 * 60 * 24));
			}
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, diff + " hari");
			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					BookHelper.BORDER_FULL, BorderStyle.THIN, color);
			index++;
			rowIndex++;
		}

		Range range = Utils.getRange(sheet, 4, 1, 4, spreadsheet.getMaxcolumns() - 1);
		Cell cell = Utils.getCell(sheet, 4, 2);
		CellStyle cellStyle = cell.getCellStyle();
		cellStyle.setWrapText(true);
		cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
		range.setStyle(cellStyle);
		Utils.setAlignment(sheet, new Rect(1, 5, 3, spreadsheet.getMaxrows() - 1), CellStyle.ALIGN_LEFT);
		Utils.setAlignment(sheet, new Rect(4, 5, spreadsheet.getMaxrows() - 1, spreadsheet.getMaxrows() - 1),
				CellStyle.ALIGN_RIGHT);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
