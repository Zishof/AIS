package ais.action.report.helper.mahasiswa;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanDataMahasiswaWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368331375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Decimalbox tahunangkatan;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();

	public LaporanDataMahasiswaWindow() {
		super();
		try {
			initFakultas();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Data Mahasiswa Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Data Mahasiswa Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDataMahasiswaWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		initFakultas();
		init();
		try {
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Data Mahasiswa Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void initFakultas() {

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

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
		setTitle("Data Mahasiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunangkatan = new Decimalbox(new BigDecimal(Calendar.getInstance().get(Calendar.YEAR))));
		tahunangkatan.setWidth("90%");

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data_mahasiswa.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initMahasiswa() throws Exception {
		mahasiswas = null;
		Session session = HibernateUtil.currentSession();
		mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))
				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
				.createCriteria("jurusan", Criteria.LEFT_JOIN)
				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)).list();

	}

	// private void

	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
			// MyMessageboxConfig.show("Fakultas" +
			// " harus dipilih", "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null) {
			// MyMessageboxConfig.show("Prodi harus dipilih", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (tahunangkatan.getValue() == null) {
			// MyMessageboxConfig.show("Tahun angkatan harus diisi", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.EXCLAMATION);
			return;
		}
		initMahasiswa();
		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(13);
		spreadsheet.setMaxrows(mahasiswas.size() + 12);
		final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 4;
		int colIndex = 3;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "No.");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "NIM");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Nama");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "TTL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Jenis\nKelamin");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Agama");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "TB/BB\n(Cm/Kg)");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "Gol.\nDarah");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "Nama\nOrang\nTua");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "Pekerjaan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "Alamat");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "Keterangan");

		Utils.setColumnWidth(sheet, 12, 150);
		Utils.setColumnWidth(sheet, 11, 200);
		Utils.setColumnWidth(sheet, 10, 200);
		Utils.setColumnWidth(sheet, 9, 150);
		Utils.setColumnWidth(sheet, 8, 100);
		Utils.setColumnWidth(sheet, 7, 100);
		Utils.setColumnWidth(sheet, 6, 100);
		Utils.setColumnWidth(sheet, 5, 100);
		Utils.setColumnWidth(sheet, 4, 100);
		Utils.setColumnWidth(sheet, 3, 450);
		Utils.setColumnWidth(sheet, 2, 100);
		Utils.setColumnWidth(sheet, 1, 30);
		Utils.setColumnWidth(sheet, 0, 0);
		Utils.setRowHeight(sheet, 4, 50);
		Utils.setRowHeight(sheet, 2, 1);
		try {
			Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
					|| searchfakultas.getSelectedItem().getValue() == null
					|| searchfakultas.getSelectedItem().getValue() == null ? null
							: searchfakultas.getSelectedItem().getValue());
			Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
					|| searchjurusan.getSelectedItem().getValue() == null
					|| searchjurusan.getSelectedItem().getValue() == null ? null
							: searchjurusan.getSelectedItem().getValue());
			Integer tahunangkatan = (this.tahunangkatan.getValue() == null ? null
					: this.tahunangkatan.getValue().intValue());

			ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1,
					"DATA MAHASISWA\n " + "" + Common.getBahasaConfig("Fakultas") + " "
							+ fakultas.getNama().toUpperCase() + "\n " + Common.getBahasaConfig("Jurusan") + " "
							+ jurusan.getNama().toUpperCase() + "\n TAHUN ANGKATAN " + tahunangkatan);
			Utils.setRowHeight(sheet, 1, 100);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
			Cell cell = Utils.getCell(sheet, 1, 1);
			cell.getCellStyle().setWrapText(true);
			cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Data Mahasiswa Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		try {
			ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					BookHelper.BORDER_FULL, BorderStyle.THIN, color);
			ais.ui.util.EcampusUtil.setBorder(sheet,
					new Rect(1, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/report/helper/mahasiswa/LaporanDataMahasiswaWindow.java:336");
		}

		rowIndex = 6;
		colIndex = 2;
		int index = 1;
		Session session = HibernateUtil.currentSession();
		for (Mahasiswa mahasiswa : mahasiswas) {
			BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1)
					.uniqueResult();
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, mahasiswa.getId());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, index);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, mahasiswa.getNim());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, mahasiswa.getNama().toUpperCase());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2,
					(mahasiswa.getTempatlahir() == null ? "" : mahasiswa.getTempatlahir().toUpperCase()) + ", "
							+ (mahasiswa.getTanggallahir() == null ? ""
									: Common.dateFormat2.get().format(mahasiswa.getTanggallahir())));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, mahasiswa.getKelamin());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 4,
					mahasiswa.getAgama() == null ? "" : mahasiswa.getAgama().getNama());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 5,
					(mahasiswa.getTinggi_badan() == null ? "" : mahasiswa.getTinggi_badan()) + "/"
							+ (mahasiswa.getBerat_badan() == null ? "" : mahasiswa.getBerat_badan()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 6, mahasiswa.getGolongan_darah());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 7,
					(biodataMahasiswa == null || biodataMahasiswa.getNamaAyah() == null ? ""
							: biodataMahasiswa.getNamaAyah()) + " - "
							+ (biodataMahasiswa == null || biodataMahasiswa.getNamaIbu() == null ? ""
									: biodataMahasiswa.getNamaIbu()));
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 8,
					(biodataMahasiswa == null || biodataMahasiswa.getPekerjaanAyah() == null ? ""
							: biodataMahasiswa.getPekerjaanAyah()) + " - "
							+ (biodataMahasiswa == null || biodataMahasiswa.getPekerjaanIbu() == null ? ""
									: biodataMahasiswa.getPekerjaanIbu()));

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 9, mahasiswa.getAlamat());

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 10, mahasiswa.getKeterangan());

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/mahasiswa/LaporanDataMahasiswaWindow.java:381");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Data Mahasiswa Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
			index++;
			rowIndex++;
		}

		try {
			Range range = Utils.getRange(sheet, 4, 1, 4, spreadsheet.getMaxcolumns() - 1);
			Cell cell = Utils.getCell(sheet, 4, 2);
			CellStyle cellStyle = cell.getCellStyle();
			cellStyle.setWrapText(true);
			cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
			range.setStyle(cellStyle);
			Utils.setAlignment(sheet, new Rect(1, 5, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1),
					CellStyle.ALIGN_LEFT);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Data Mahasiswa Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
