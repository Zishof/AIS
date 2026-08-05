package ais.action.report.helper.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
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

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanDaftarHadirDosen extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox searchhari = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Label angkatan = new Label();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public LaporanDaftarHadirDosen() {
		super();
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Hadir Dosen", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDaftarHadirDosen(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Hadir Dosen", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
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
			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
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
		north.setHeight("280px");
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

		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			searchhari.appendChild(comboitem);
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Hari"));
		row.appendChild(searchhari);
		searchhari.setWidth("90%");


		searchhari.setSelectedIndex(1);

		Common.initPrograms(searchprogram);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");



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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "absensi_dosen.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.clear(center);
		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null || this.tahunAkademik.getSelectedItem().getValue() == null ? null
				: this.tahunAkademik.getSelectedItem().getValue());
		String semester = (String) (this.semesterAbsensi.getSelectedItem() == null || semesterAbsensi.getSelectedItem().getValue() == null ? Perkuliahan.GANJIL
				: this.semesterAbsensi.getSelectedItem().getValue());

		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? null
				: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? null
				: searchjurusan.getSelectedItem().getValue());

		Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
				: searchsemester.getSelectedItem().getValue());

		String hari = (String) (searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null ? null : searchhari.getSelectedItem().getValue());

		String program = (String) (searchprogram.getSelectedItem() == null||searchprogram.getSelectedItem().getValue() == null ? null
				: searchprogram.getSelectedItem().getValue());

		if (tahunAkademik == null || hari == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();
		List<Object[]> jurusans = new ArrayList<Object[]>();

		String sql = "select " + "(a.waktu_mulai || ' s.d ' || a.waktu_selesai) as waktu, " + "UPPER(b.nama) as mk, "
				+ "UPPER(c.nama) as dosen, " + "a.semester, " + "b.sks, " + "f.kode_ruangan as ruang "
				+ "from perkuliahan a " + "left join matakuliah b on (a.matakuliah = b.id) "
				+ "left join dosen c on (a.dosen1 = c.id) " + "left join jurusan d on (d.id = a.jurusan) "
				+ "left join ruang f on (f.id = a.ruang)  " + " where 1=1 and a.hari = ? "
				+ (program == null ? "" : " and a.program = '" + program + "'")
				+ (semesterKe == null || semesterKe.equals(-1) ? "" : " and a.semester = " + semesterKe)
				+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
				+ (fakultas == null ? "" : " and d.fakultas = " + fakultas.getId()) + " and a.tahun_ajaran = '"
				+ tahunAkademik + "' and a.semester "
				+ ((semester.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 ")
						+ " order by CAST(a.waktu_mulai as double precision) asc, CAST(a.waktu_selesai as double precision) asc, c.nama ");

		System.out.println(sql);
		jurusans = session.createSQLQuery(sql).setString(0, hari).list();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(23);
		spreadsheet.setMaxrows(jurusans.size() + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"ABSENSI DOSEN\n " + "" + Common.getBahasaConfig("Fakultas") + " "
						+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
						+ Common.getBahasaConfig("Jurusan") + " "
						+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()) + "\n TAHUN AKADEMIK "
						+ tahunAkademik + "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase()) + "\nHARI "
						+ (hari == null ? "SEMUA" : hari.toUpperCase()) + "\n SEMESTER " + semester.toUpperCase()
						+ "\n " + (semesterKe.equals(-1) ? "" : "SEMESTER KE-" + semesterKe + ""));
		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		Utils.setRowHeight(sheet, 1, 170);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Waktu");
		Utils.setColumnWidth(sheet, 0, 100);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, Common.getBahasaConfig("Matakuliah"));
		Utils.setColumnWidth(sheet, 1, 200);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.getBahasa("label_dosen"));
		Utils.setColumnWidth(sheet, 2, 200);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Smt");
		Utils.setColumnWidth(sheet, 3, 30);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "SKS");
		Utils.setColumnWidth(sheet, 4, 30);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Ruang");
		Utils.setColumnWidth(sheet, 5, 100);

		int j = 1;
		for (int i = 6; i < 6 + 17; i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, i, "Minggu ke-" + j);
			Utils.setColumnWidth(sheet, i, 100);
			j++;
		}

		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

		rowIndex = 3;
		colIndex = 0;

		for (Object[] objects : jurusans) {
			if (objects[0] == null)
				continue;

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0] == null ? "" : objects[0].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[1] == null ? "" : objects[1].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, objects[2] == null ? "" : objects[2].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, objects[3] == null ? "" : objects[3].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, objects[4] == null ? "" : objects[4].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, objects[5] == null ? "" : objects[5].toString());

			Utils.setRowHeight(sheet, rowIndex, 50);
			rowIndex++;
		}

		Common.setStyled(sheet);spreadsheet.setMaxrows(rowIndex + 1);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

	}
}
