package ais.action.report.helper.keuangan;
import ais.common.PesanFormalHelper;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapHostToHostCicilanPerItemBulananWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox jenisPembayaran = new Combobox();
	private Combobox jenisSeleksi = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox jenjang = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private MyDatebox start = new MyDatebox();
	private MyDatebox end = new MyDatebox();
	private List<Checkbox> mapItemBiaya = new ArrayList<Checkbox>();
	private Center center = new Center();

	private Combobox semester;

	private Decimalbox angkatan;

	private Combobox statusAwal;

	private Textbox keterangan;

	private MyCheckboxConfig format;

	public LaporanRekapHostToHostCicilanPerItemBulananWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Host To Host Cicilan Per Item Bulanan Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapHostToHostCicilanPerItemBulananWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init() {

		Common.insertComboDanSemua(jenisPembayaran, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(jenisSeleksi, "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertComboDanSemua(jenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

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
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("130px");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("130px");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("130px");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("130px");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas/Prodi"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(searchfakultas);
		searchfakultas.setCols(2);
		hbox.appendChild(searchjurusan);
		searchjurusan.setCols(2);
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TA/Smt"));
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(tahunAkademik);
		tahunAkademik.setCols(5);
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(2);
		hbox.appendChild(semesterAbsensi);
		semesterAbsensi.setCols(5);
		semesterAbsensi.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis/Status Awal"));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(jenisPembayaran);
		jenisPembayaran.setCols(5);
		jenisPembayaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
						: jenisPembayaran.getSelectedItem().getValue());

				jenisSeleksi.setDisabled(jenisKegiatan == null
						|| !jenisKegiatan.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA));

				if (jenisSeleksi.isDisabled()) {
					jenisSeleksi.setSelectedItem(null);
				}

			}
		});

		statusAwal = new Combobox();
		Common.insertComboDanSemua(statusAwal, "nama", StatusAwalMahasiswa.class, Restrictions.eq("aktif", true));
		statusAwal.setReadonly(true);
		hbox.appendChild(statusAwal);
		statusAwal.setCols(5);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Smt/Angkt"));
		hbox = new Hbox();
		row.appendChild(hbox);
		semester = new Combobox();
		comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		semester.appendChild(comboitem);
		for (int i = 1; i < 20; i++) {
			Comboitem itemSmt = new Comboitem();
			itemSmt.setValue(i);
			itemSmt.setLabel(i + "");
			semester.appendChild(itemSmt);
		}
		semester.setReadonly(true);
		semester.setCols(5);
		semester.setSelectedIndex(0);
		hbox.appendChild(semester);

		angkatan = new Decimalbox();
		angkatan.setCols(5);
		hbox.appendChild(angkatan);

		row = new MyFormRow();
		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Seleksi"));
		row.appendChild(jenisSeleksi);
		jenisSeleksi.setDisabled(true);
		jenisSeleksi.setSelectedItem(null);
		jenisSeleksi.setWidth("90%");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) - 1);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		hbox = new Hbox();
		row.appendChild(hbox);
		if (start != null) start.setValue(calendar.getTime());
		hbox.appendChild(start);
		start.setCols(5);
		if (start != null) start.setReadonly(true);

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

		if (end != null) end.setValue(calendar.getTime());
		if (end != null) end.setReadonly(true);
		hbox.appendChild(end);
		end.setCols(5);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang / Ket."));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(jenjang);
		jenjang.setCols(5);
		jenjang.setReadonly(true);
		keterangan = new Textbox();
		hbox.appendChild(keterangan);
		keterangan.setCols(5);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);

		final Vbox vbox = new Vbox();
		vbox.setParent(row);

		EventListener listener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(vbox);
				mapItemBiaya.clear();

				JenisKegiatan jenisKegiatan = (JenisKegiatan) (jenisPembayaran.getSelectedItem() == null ? null
						: jenisPembayaran.getSelectedItem().getValue());

				List<ItemBiaya> itemBiayas = HibernateUtil.currentSession().createCriteria(DetailSettingBiaya.class)
						.createAlias("itemBiaya", "itemBiaya").createAlias("settingBiaya", "settingBiaya")
						.add(jenisKegiatan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("settingBiaya.jenisKegiatan", jenisKegiatan))
						.add(Restrictions.or(Restrictions.eq("itemBiaya.aktif", true),
								Restrictions.isNull("itemBiaya.aktif")))
						.setProjection(Projections.groupProperty("itemBiaya")).addOrder(Order.asc("itemBiaya")).list();
				Hbox hbox1 = new Hbox();
				vbox.appendChild(hbox1);
				int index = 0;
				for (ItemBiaya itemBiaya : itemBiayas) {
					if (index % 15 == 0) {
						hbox1 = new Hbox();
						vbox.appendChild(hbox1);
					}
					index++;
					Checkbox checkbox = new Checkbox(itemBiaya.getNama());
					checkbox.setAttribute("itemBiaya", itemBiaya);
					mapItemBiaya.add(checkbox);
					checkbox.setChecked(true);
					checkbox.setStyle("font-size:8px");
					checkbox.setParent(hbox1);
				}
				itemBiayas = null;

			}
		};

		jenisPembayaran.addEventListener("onChange", listener);
		try {
			listener.onEvent(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBulananWindow.java:367");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Cicilan Per Item Bulanan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		Hbox toolbar = new Hbox();
		toolbar.setParent(row);
		row.setParent(rows);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
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
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "REKAP_PEMBAYARAN.xlsx");
			}
		});
		print.setParent(toolbar);

		format = new MyCheckboxConfig("Format Nilai");
		format.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});
		format.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

	}

	// private void

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {

		int tanggal = ais.action.report.Report.hitungJumlahHariInklusif(start.getValue(), end.getValue());
		if (tanggal > 370) {
			MyMessageboxConfig.show("Tanggal mulai dan sampai pengambilan data tidak boleh lebih dari 370 hari",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());

		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());
		final StatusAwalMahasiswa statusAwal = (StatusAwalMahasiswa) (this.statusAwal.getSelectedItem() == null
				|| this.statusAwal.getSelectedItem().getValue() == null ? null
						: this.statusAwal.getSelectedItem().getValue());

		final JenisKegiatan jenisPembayaran = (JenisKegiatan) (LaporanRekapHostToHostCicilanPerItemBulananWindow.this.jenisPembayaran
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBulananWindow.this.jenisPembayaran.getSelectedItem()
								.getValue());
		final JenisSeleksi jenisSeleksi = (JenisSeleksi) (LaporanRekapHostToHostCicilanPerItemBulananWindow.this.jenisSeleksi
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBulananWindow.this.jenisSeleksi.getSelectedItem()
								.getValue());

		final String tahunAkademik = (String) (LaporanRekapHostToHostCicilanPerItemBulananWindow.this.tahunAkademik
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBulananWindow.this.tahunAkademik.getSelectedItem()
								.getValue());
		final String semester = (String) (LaporanRekapHostToHostCicilanPerItemBulananWindow.this.semesterAbsensi
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBulananWindow.this.semesterAbsensi.getSelectedItem()
								.getValue());

		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final Integer tahunAngkatan = angkatan.getValue() == null ? null : angkatan.getValue().intValue();

		final Jenjang jenjang = (Jenjang) (LaporanRekapHostToHostCicilanPerItemBulananWindow.this.jenjang
				.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBulananWindow.this.jenjang.getSelectedItem().getValue());

		final Integer smt = (Integer) (LaporanRekapHostToHostCicilanPerItemBulananWindow.this.semester
				.getValue() == null
				|| LaporanRekapHostToHostCicilanPerItemBulananWindow.this.semester.getSelectedItem() == null ? null
						: LaporanRekapHostToHostCicilanPerItemBulananWindow.this.semester.getSelectedItem().getValue());

		final TreeMap<String, Object[]> jurusans = new TreeMap<String, Object[]>();
		final TreeMap<String, Object[]> itemBiayas = new TreeMap<String, Object[]>();

		final String fn = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/rekap_"
						+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
						+ ".xlsx");

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				XSSFWorkbook workbook = new XSSFWorkbook();

				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
				hlink_font.setColor(new XSSFColor(Color.BLACK));

				XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				hlink_style.setBorderLeft(BorderStyle.THIN);
				hlink_style.setBorderTop(BorderStyle.THIN);
				hlink_style.setBorderRight(BorderStyle.THIN);
				hlink_style.setBorderBottom(BorderStyle.DOUBLE);

				hlink_style.setBorderColor(BorderSide.TOP, new XSSFColor(new Color(0, 0, 0)));
				hlink_style.setBorderColor(BorderSide.RIGHT, new XSSFColor(new Color(0, 0, 0)));
				hlink_style.setBorderColor(BorderSide.BOTTOM, new XSSFColor(new Color(0, 0, 0)));
				hlink_style.setBorderColor(BorderSide.LEFT, new XSSFColor(new Color(0, 0, 0)));

				XSSFFont bodyfont = workbook.createFont();
				bodyfont.setBoldweight(XSSFFont.BOLDWEIGHT_NORMAL);
				bodyfont.setColor(new XSSFColor(Color.BLACK));

				XSSFCellStyle bodystyle = workbook.createCellStyle();
				bodystyle.setFont(bodyfont);

				bodystyle.setBorderLeft(BorderStyle.THIN);
				bodystyle.setBorderTop(BorderStyle.THIN);
				bodystyle.setBorderRight(BorderStyle.THIN);
				bodystyle.setBorderBottom(BorderStyle.THIN);

				bodystyle.setBorderColor(BorderSide.TOP, new XSSFColor(new Color(0, 0, 0)));
				bodystyle.setBorderColor(BorderSide.RIGHT, new XSSFColor(new Color(0, 0, 0)));
				bodystyle.setBorderColor(BorderSide.BOTTOM, new XSSFColor(new Color(0, 0, 0)));
				bodystyle.setBorderColor(BorderSide.LEFT, new XSSFColor(new Color(0, 0, 0)));

				XSSFSheet sheet = workbook.createSheet("Data");
				sheet.setDefaultColumnWidth(20);
				int rowIndex = 1;
				XSSFRow rowhead = sheet.createRow(rowIndex);
				XSSFCell cell = rowhead.createCell(0);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("REKAPITULASI PEMBAYARAN "
						+ (jenisPembayaran == null ? "SEMUA JENIS PEMBAYARAN"
								: jenisPembayaran.getNamaKegiatan().toUpperCase())
						+ "\n  "
						+ (fakultas == null || fakultas.getId().equals(-1L) ? "SEMUA " + "Fakultas"
								: "Fakultas" + " " + fakultas.getNama().toUpperCase())
						+ "\n " + (tahunAkademik == null ? "SEMUA TAHUN AKADEMIK" : "TAHUN AKADEMIK " + tahunAkademik)
						+ "\n  " + (semester == null ? "SEMUA SEMESTER" : "SEMESTER " + semester.toUpperCase())
						+ (jenisSeleksi == null || jenisSeleksi.getId() == null ? ""
								: "\nJENIS SELEKSI " + jenisSeleksi.getNama().toUpperCase()));

				cell = rowhead.createCell(1);
				cell.setCellStyle(hlink_style);
				cell = rowhead.createCell(2);
				cell.setCellStyle(hlink_style);
				cell = rowhead.createCell(3);
				cell.setCellStyle(hlink_style);
				cell = rowhead.createCell(4);
				cell.setCellStyle(hlink_style);
				int indexCol = 4;
				for (@SuppressWarnings("unused")
				Object[] object : itemBiayas.values()) {

					cell = rowhead.createCell(++indexCol);
					cell.setCellStyle(hlink_style);

				}

				cell = rowhead.createCell(++indexCol);
				cell.setCellStyle(hlink_style);

				rowIndex = 2;
				XSSFRow row = sheet.createRow(rowIndex);
				cell = row.createCell(0);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("NIM");

				cell = row.createCell(1);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("Nama");

				cell = row.createCell(2);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("Prodi");

				cell = row.createCell(3);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("Program");

				cell = row.createCell(4);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("Angkatan");

				indexCol = 4;
				for (Object[] object : itemBiayas.values()) {

					ItemBiaya itemBiaya = (ItemBiaya) object[0];
					Integer bulan = (Integer) object[1];

					cell = row.createCell(++indexCol);
					cell.setCellStyle(hlink_style);
					cell.setCellValue(itemBiaya.getNama() + (bulan == null ? "" : " bln " + bulan));

				}

				cell = row.createCell(++indexCol);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("Total");

				rowIndex = 3;

				Double jumlahTotal = 0.0;

				for (Object[] objects : jurusans.values()) {
					try {
						if (objects[0] == null) {
							continue;
						}

						row = sheet.createRow(rowIndex);

						GeneralValueObject generalValueObject = (GeneralValueObject) objects[0];
						if (generalValueObject instanceof Mahasiswa) {
							Mahasiswa mahasiswa = (Mahasiswa) generalValueObject;
							String nim = mahasiswa.getNim();

							cell = row.createCell(0);
							cell.setCellStyle(bodystyle);
							cell.setCellValue(nim);

							String nama = mahasiswa.getNama();
							cell = row.createCell(1);
							cell.setCellStyle(bodystyle);
							cell.setCellValue(nama);

							Jurusan jurusan = mahasiswa.getJurusan();
							cell = row.createCell(2);
							cell.setCellStyle(bodystyle);
							cell.setCellValue(jurusan.getNama());

							String program = mahasiswa.getProgram();
							cell = row.createCell(3);
							cell.setCellStyle(bodystyle);
							cell.setCellValue(program);

							String angkatan = mahasiswa.getTahunangkatan().toString();
							cell = row.createCell(4);
							cell.setCellStyle(bodystyle);
							cell.setCellValue(angkatan);

						} else if (generalValueObject instanceof BiodataCalonMahasiswa) {

							BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) generalValueObject;
							String nim = biodataCalonMahasiswa.getNoRegistrasi();

							cell = row.createCell(0);
							cell.setCellStyle(bodystyle);
							cell.setCellValue(nim);

							String nama = biodataCalonMahasiswa.getNama();
							cell = row.createCell(1);
							cell.setCellStyle(bodystyle);
							cell.setCellValue(nama);

							Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus() != null
									? biodataCalonMahasiswa.getProdiLulus()
									: biodataCalonMahasiswa.getProdi1();
							cell = row.createCell(2);
							cell.setCellStyle(bodystyle);
							cell.setCellValue(jurusan.getNama());

							String program = biodataCalonMahasiswa.getProgram();
							cell = row.createCell(3);
							cell.setCellStyle(bodystyle);
							cell.setCellValue(program);

							String angkatan = biodataCalonMahasiswa.getTahun().toString();
							cell = row.createCell(4);
							cell.setCellStyle(bodystyle);
							cell.setCellValue(angkatan);

						}

						Map<String, Double> dataTotal = (Map<String, Double>) objects[1];

						indexCol = 4;
						Double total = 0.0;
						for (Object[] object : itemBiayas.values()) {

							ItemBiaya itemBiaya = (ItemBiaya) object[0];
							Integer bulan = (Integer) object[1];

							String kk = itemBiaya.getId() + (bulan == null ? "" : "_" + bulan);
							Double t = dataTotal.get(kk);
							if (t == null) {
								t = 0.0;
							}
							cell = row.createCell(++indexCol);
							cell.setCellStyle(bodystyle);

							if (format.isChecked()) {
								cell.setCellValue(Common.numberFormat.get().format(t));
							} else {
								cell.setCellValue(t);
							}
							total += t;
						}

						cell = row.createCell(++indexCol);
						cell.setCellStyle(hlink_style);
						if (format.isChecked()) {
							cell.setCellValue(Common.numberFormat.get().format(total));
						} else {
							cell.setCellValue(total);
						}
						jumlahTotal += total;

						rowIndex++;
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBulananWindow.java:707");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Cicilan Per Item Bulanan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}
				}

				row = sheet.createRow(rowIndex);
				cell = row.createCell(0);
				cell.setCellStyle(hlink_style);
				cell.setCellValue("TOTAL");

				cell = row.createCell(1);
				cell.setCellStyle(hlink_style);
				if (format.isChecked()) {
					cell.setCellValue(Common.numberFormat.get().format(jumlahTotal));
				} else {
					cell.setCellValue(jumlahTotal);
				}

				File file = new File(fn);

				try {
					FileOutputStream fileOut = new FileOutputStream(file);
					workbook.write(fileOut);
					fileOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

				spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);
				spreadsheet.setParent(center);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../tmp/" + file.getName());
				spreadsheet.setMaxcolumns(6 + itemBiayas.size());
				spreadsheet.setMaxrows(jurusans.size() + 25);
				for (int i = 0; i < spreadsheet.getMaxcolumns(); i++) {
					try {
						sheet.autoSizeColumn(i);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBulananWindow.java:746");
						// TODO: handle exception
					}
				}
				jurusans.clear();

				// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {

				try {

					List<Long> ids = new ArrayList<Long>();
					for (Checkbox checkbox : mapItemBiaya) {
						if (checkbox.isChecked()) {
							ItemBiaya itemBiaya = (ItemBiaya) checkbox.getAttribute("itemBiaya");
							ids.add(itemBiaya.getId());
						}
					}

					List<Long> cicilanPembayarans = new ArrayList<Long>();

					try {
						Session session1 = ais.action.report.Report.openNativeSession();

						cicilanPembayarans = session1.createCriteria(CicilanPembayaran.class)

								.add(keterangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("keterangan", keterangan.getValue().trim(),
												MatchMode.ANYWHERE))

								.setProjection(Projections.property("id"))

								.add(ids.isEmpty() ? Restrictions.sqlRestriction("false")
										: Restrictions.in("itemBiaya.id", ids))

								.add(Restrictions.sqlRestriction("date(this_.tanggal) between date('"
										+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
										+ Common.databaseDateFormat.get().format(end.getValue()) + "')"))
								.list();
						session1.disconnect();
						session1.close();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBulananWindow.java:792");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Cicilan Per Item Bulanan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}

					ais.action.report.Report.closeCurrentSessionQuietly();

					int size = cicilanPembayarans.size();
					int index = 0;

					for (Long cicilanPembayaranId : cicilanPembayarans) {
						try {
							index++;
							Session session1 = ais.action.report.Report.openNativeSession();
							CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session1
									.createCriteria(CicilanPembayaran.class).add(Restrictions.idEq(cicilanPembayaranId))
									.uniqueResult();
							session1.disconnect();
							session1.close();
							ais.action.report.Report.closeCurrentSessionQuietly();
							label.setValue("Ambil data " + cicilanPembayaran.toString() + " ("
									+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

							if ((cicilanPembayaran.getItemBiaya() != null
									&& ids.contains(cicilanPembayaran.getItemBiaya().getId()))) {

								Kegiatan kegiatan = cicilanPembayaran.getKegiatan();

								if (kegiatan != null) {

									if (smt == null || smt.equals(kegiatan.getSemster())) {

										if (semester == null
												|| (semester.equals(Perkuliahan.GENAP) ? kegiatan.getSemster() % 2 == 0
														: kegiatan.getSemster() % 2 == 1)) {

											if (tahunAkademik == null
													|| tahunAkademik.equals(kegiatan.getTahunAkademik())) {

												if (jenisPembayaran == null || jenisPembayaran.getId()
														.equals(kegiatan.getJenisKegiatan().getId())) {

													Mahasiswa mahasiswa = kegiatan.getMahasiswa();
													BiodataCalonMahasiswa biodataCalonMahasiswa = kegiatan
															.getCalonMahasiswa();

													if (tahunAngkatan == null
															|| (biodataCalonMahasiswa != null
																	&& biodataCalonMahasiswa.getTahun() != null
																	&& tahunAngkatan
																			.equals(biodataCalonMahasiswa.getTahun()))

															|| (mahasiswa != null
																	&& mahasiswa.getTahunangkatan() != null
																	&& tahunAngkatan
																			.equals(mahasiswa.getTahunangkatan()))

													) {

														if (program == null
																|| (biodataCalonMahasiswa != null
																		&& biodataCalonMahasiswa.getProgram() != null
																		&& program.equals(
																				biodataCalonMahasiswa.getProgram()))

																|| (mahasiswa != null && mahasiswa.getProgram() != null
																		&& program.equals(mahasiswa.getProgram()))

														) {

															if (statusAwal == null
																	|| (biodataCalonMahasiswa != null
																			&& biodataCalonMahasiswa
																					.getStatusAwalMahasiswa() != null
																			&& statusAwal.getId()
																					.equals(biodataCalonMahasiswa
																							.getStatusAwalMahasiswa()
																							.getId()))

																	|| (mahasiswa != null
																			&& mahasiswa
																					.getStatusAwalMahasiswa() != null
																			&& statusAwal.getId().equals(mahasiswa
																					.getStatusAwalMahasiswa().getId()))

															) {

																if (jenisSeleksi == null
																		|| (biodataCalonMahasiswa != null
																				&& biodataCalonMahasiswa
																						.getJenisSeleksi() != null
																				&& jenisSeleksi.getId()
																						.equals(biodataCalonMahasiswa
																								.getJenisSeleksi()
																								.getId()))

																		|| (mahasiswa != null
																				&& mahasiswa.getJenisSeleksi() != null
																				&& jenisSeleksi.getId().equals(mahasiswa
																						.getJenisSeleksi().getId()))

																) {

																	if (fakultas == null || fakultas.getId().equals(-1L)

																			|| (mahasiswa != null
																					&& mahasiswa.getJurusan() != null
																					&& mahasiswa.getJurusan()
																							.getFakultas().getId()
																							.equals(fakultas.getId()))

																			|| (biodataCalonMahasiswa != null
																					&& biodataCalonMahasiswa
																							.getProdi1() != null
																					&& biodataCalonMahasiswa.getProdi1()
																							.getFakultas().getId()
																							.equals(fakultas.getId()))

																			|| (biodataCalonMahasiswa != null
																					&& biodataCalonMahasiswa
																							.getProdiLulus() != null
																					&& biodataCalonMahasiswa
																							.getProdiLulus()
																							.getFakultas().getId()
																							.equals(fakultas
																									.getId()))) {

																		if (jurusan == null
																				|| jurusan.getId().equals(-1L)

																				|| (mahasiswa != null
																						&& mahasiswa
																								.getJurusan() != null
																						&& mahasiswa.getJurusan()
																								.getId()
																								.equals(jurusan
																										.getId()))

																				|| (biodataCalonMahasiswa != null
																						&& biodataCalonMahasiswa
																								.getProdi1() != null
																						&& biodataCalonMahasiswa
																								.getProdi1().getId()
																								.equals(jurusan
																										.getId()))

																				|| (biodataCalonMahasiswa != null
																						&& biodataCalonMahasiswa
																								.getProdiLulus() != null
																						&& biodataCalonMahasiswa
																								.getProdiLulus().getId()
																								.equals(jurusan
																										.getId()))) {

																			if (jenjang == null
																					|| jenjang.getId().equals(-1L)

																					|| (mahasiswa != null && mahasiswa
																							.getJurusan() != null
																							&& mahasiswa.getJurusan()
																									.getJenjang()
																									.getId()
																									.equals(jenjang
																											.getId()))

																					|| (biodataCalonMahasiswa != null
																							&& biodataCalonMahasiswa
																									.getProdi1() != null
																							&& biodataCalonMahasiswa
																									.getProdi1()
																									.getJenjang()
																									.getId()
																									.equals(jenjang
																											.getId()))

																					|| (biodataCalonMahasiswa != null
																							&& biodataCalonMahasiswa
																									.getProdiLulus() != null
																							&& biodataCalonMahasiswa
																									.getProdiLulus()
																									.getJenjang()
																									.getId()
																									.equals(jenjang
																											.getId()))) {

																				String key = (mahasiswa != null
																						? kegiatan.getMahasiswa()
																								.getId() + "_mhs"
																						: biodataCalonMahasiswa != null
																								? kegiatan
																										.getCalonMahasiswa()
																										.getId()
																										+ "_calon_mhs"
																								: "");

																				Object[] objSbm = jurusans.get(key);
																				Map<String, Double> dataTotal;
																				if (objSbm == null) {
																					dataTotal = new HashMap<String, Double>();
																				} else {
																					dataTotal = (Map<String, Double>) objSbm[1];
																				}

																				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
																						.getPengaturanPembayaranBulanan();

																				String kk = cicilanPembayaran
																						.getItemBiaya().getId()
																						+ (pengaturanPembayaranBulanan == null
																								? ""
																								: "_" + pengaturanPembayaranBulanan
																										.getRealBulanTahun());

																				Double total = dataTotal.get(kk);
																				if (total == null) {
																					total = 0.0;
																				}
																				total += cicilanPembayaran.getNilai();
																				dataTotal.put(kk, total);

																				GeneralValueObject generalValueObject = mahasiswa == null
																						? biodataCalonMahasiswa
																						: mahasiswa;

																				Object[] objects = new Object[] {
																						generalValueObject, dataTotal };
																				jurusans.put(key, objects);

																				itemBiayas.put(kk, new Object[] {
																						cicilanPembayaran
																								.getItemBiaya(),
																						pengaturanPembayaranBulanan == null
																								? ""
																								: pengaturanPembayaranBulanan
																										.getRealBulanTahun() });
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBulananWindow.java:1039");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Cicilan Per Item Bulanan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
					}
					cicilanPembayarans = null;

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/keuangan/LaporanRekapHostToHostCicilanPerItemBulananWindow.java:1045");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Host To Host Cicilan Per Item Bulanan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

	}
}
