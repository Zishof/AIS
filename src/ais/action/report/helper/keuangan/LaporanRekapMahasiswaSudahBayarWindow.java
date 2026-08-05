package ais.action.report.helper.keuangan;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
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
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapMahasiswaSudahBayarWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Label angkatan = new Label();
	private Combobox jenisPembayaran = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private Combobox angkatanMhsMulai = new Combobox();private Combobox angkatanMhs = new Combobox();

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	private Combobox searchstatus = new Combobox();
	private Combobox searchwnawni = new Combobox();
	private Textbox nomorref = new Textbox();
	private Textbox nim = new Textbox();
	private Combobox searchprogram = new Combobox();

	private MyDatebox tanggalMulai = new MyDatebox();
	private MyDatebox tanggalSampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate());

	public LaporanRekapMahasiswaSudahBayarWindow() {
		super();
		try {

			init();
			initFakultas();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Mahasiswa Sudah Bayar Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapMahasiswaSudahBayarWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
		initFakultas();
		// initSpreadsheet();
	}

	private void initFakultas() {

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		jenisPembayaran = Common.createComboJenisPembayaran(jenisPembayaran);

		Common.insertCombo(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		MyComboitemConfig comboitem = new MyComboitemConfig(Mahasiswa.WNI);
		comboitem.setValue(Mahasiswa.WNI);
		searchwnawni.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Mahasiswa.WNA);
		comboitem.setValue(Mahasiswa.WNA);
		searchwnawni.appendChild(comboitem);

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
	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		// setTitle("Rekap mahasiswa yang sudah melakukan pembayaran");
		// setWidth("98%");
		// setHeight("90%");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kewarganegaraan"));
		row.appendChild(searchwnawni);
		searchwnawni.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		row.appendChild(searchstatus);
		searchstatus.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setReadonly(true);
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Referensi"));
		row.appendChild(nomorref);
		nomorref.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
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
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM/No.Reg/No.Ujian"));
		row.appendChild(nim);
		nim.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatanMhs);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		angkatanMhs.appendChild(comboitem);
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10; i <= ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR) + 10; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatanMhs.appendChild(comboitem);
		}
		angkatanMhs.setSelectedIndex(0);
		angkatanMhs.setWidth("90%");
		angkatanMhs.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		row.appendChild(tanggalMulai);
		tanggalMulai.setWidth("90%");

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		if (tanggalMulai != null) tanggalMulai.setValue(calendar.getTime());
		tanggalMulai.setFormat(Common.dateFormat1.get().toPattern());
		if (tanggalMulai != null) tanggalMulai.setReadonly(true);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		row.appendChild(tanggalSampai);
		tanggalSampai.setWidth("90%");
		tanggalSampai.setFormat(Common.dateFormat1.get().toPattern());
		if (tanggalSampai != null) tanggalSampai.setReadonly(true);
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (tanggalSampai != null) tanggalSampai.setValue(calendar.getTime());

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
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					spreadsheet.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"Rekap_mahasiswa_yang_sudah_melakukan_pembayaran.xlsx");
				} catch (Exception e) {
					MyMessageboxConfig.show("Klik dulu tombol Tampilkan", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
				}
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.clear(center);
		final Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		final String tahunAkademik = (String) (LaporanRekapMahasiswaSudahBayarWindow.this.tahunAkademik
				.getSelectedItem() == null ? null
						: LaporanRekapMahasiswaSudahBayarWindow.this.tahunAkademik.getSelectedItem().getValue());
		final String semester = (String) (LaporanRekapMahasiswaSudahBayarWindow.this.semesterAbsensi
				.getSelectedItem() == null ? Perkuliahan.GANJIL
						: LaporanRekapMahasiswaSudahBayarWindow.this.semesterAbsensi.getSelectedItem().getValue());
		final Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());
		final JenisKegiatan jenisPembayaran = (JenisKegiatan) (LaporanRekapMahasiswaSudahBayarWindow.this.jenisPembayaran
				.getSelectedItem() == null ? null
						: LaporanRekapMahasiswaSudahBayarWindow.this.jenisPembayaran.getSelectedItem().getValue());

		final Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? null
				: searchsemester.getSelectedItem().getValue());

		final String program = (String) (searchprogram.getSelectedItem() == null
				|| searchprogram.getSelectedItem().getValue() == null ? null
						: searchprogram.getSelectedItem().getValue());

		final StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());
		final String wnawni = (String) (searchwnawni.getSelectedItem() == null ? null
				: searchwnawni.getSelectedItem().getValue());
		final String nim = LaporanRekapMahasiswaSudahBayarWindow.this.nim.getValue().trim();
		final String nomorref = LaporanRekapMahasiswaSudahBayarWindow.this.nomorref.getValue().trim();

		final Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
				: angkatanMhs.getSelectedItem().getValue());

		if (jenisPembayaran == null || tahunAkademik == null) {
			return;
		}

		final Date mulai = tanggalMulai.getValue();
		final Date sampai = tanggalSampai.getValue();

		final JenisKegiatan jenisKegiatan = jenisPembayaran;

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(22);

		final Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, "REKAPITULASI MAHASISWA YANG SUDAH MELAKUKAN "
				+ (jenisPembayaran.getNamaKegiatan().toUpperCase()) + "\n " + "" + Common.getBahasaConfig("Fakultas")
				+ " " + (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
				+ Common.getBahasaConfig("Jurusan") + " "
				+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()) + "\n TAHUN AKADEMIK " + tahunAkademik
				+ "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase()) + "\n SEMESTER "
				+ semester.toUpperCase() + "\n ANGKATAN " + (angkatan == null ? "SEMUA" : angkatan));
		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		Utils.setRowHeight(sheet, 1, 150);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "NIM/No.Reg/No.Ujian");
		Utils.setColumnWidth(sheet, 0, 130);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Nama");
		Utils.setColumnWidth(sheet, 1, 200);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Angkatan");
		Utils.setColumnWidth(sheet, 2, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Telp/Hp");
		Utils.setColumnWidth(sheet, 3, 50);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Email");
		Utils.setColumnWidth(sheet, 4, 50);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Alamat");
		Utils.setColumnWidth(sheet, 5, 50);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Cara Pembayaran");
		Utils.setColumnWidth(sheet, 6, 200);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7, "Tanggal Pembayaran");
		Utils.setColumnWidth(sheet, 7, 200);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, "Item Biaya");
		Utils.setColumnWidth(sheet, 8, 200);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, "Nominal");
		Utils.setColumnWidth(sheet, 9, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, "WNI/WNA");
		Utils.setColumnWidth(sheet, 10, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "Status");
		Utils.setColumnWidth(sheet, 11, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "Fakultas");
		Utils.setColumnWidth(sheet, 12, 120);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "Jurusan");
		Utils.setColumnWidth(sheet, 13, 120);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, "Program");
		Utils.setColumnWidth(sheet, 14, 70);

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15, "Waktu");
		Utils.setColumnWidth(sheet, 15, 130);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, "Validator");
		Utils.setColumnWidth(sheet, 16, 100);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, "No. Referensi");
		Utils.setColumnWidth(sheet, 17, 130);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, "Terbayar");
		Utils.setColumnWidth(sheet, 18, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, "Terhutang");
		Utils.setColumnWidth(sheet, 19, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20, "Lunas");
		Utils.setColumnWidth(sheet, 20, 80);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, "%");
		Utils.setColumnWidth(sheet, 21, 80);

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		final Intbox tinggi = new Intbox(0);
		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
Common.clear(center);spreadsheet.setParent(center);
				spreadsheet.setMaxrows(tinggi.getValue() + 6);

				// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {

				Session session = ais.action.report.Report.openNativeSession();
				try {
					List<Kegiatan> kegiatans = new ArrayList<Kegiatan>();
					if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
						kegiatans = session.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true)).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
								.add(semesterKe == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("semster", semesterKe))
								.add(mulai == null || sampai == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.between("tanggal", mulai, sampai))
								.add(nomorref == null || nomorref.trim().equals("") ? Restrictions.sqlRestriction("1=1")
										: Restrictions.ilike("refNumber", nomorref, MatchMode.ANYWHERE))

								.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
								.createAlias("calonMahasiswa", "calonMahasiswa", Criteria.INNER_JOIN)
								.createAlias("calonMahasiswa.prodiLulus", "jurusan", Criteria.LEFT_JOIN)
								.add(wnawni == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("calonMahasiswa.kewarganegaraan", wnawni))
								.add(program == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("calonMahasiswa.program", program))
								.add(nim == null || nim.trim().equals("") ? Restrictions.sqlRestriction("1=1")
										: Restrictions.ilike("calonMahasiswa.noUjian", nim, MatchMode.ANYWHERE))
								.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("calonMahasiswa.prodiLulus", jurusan))
								.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jurusan.fakultas", fakultas))
								.add(angkatan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("calonMahasiswa.tahun", angkatan))
								.add(Restrictions.eq("tahunAkademik", tahunAkademik)).addOrder(Order.desc("id")).list();

					} else if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {
						kegiatans = session.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true)).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
								.add(semesterKe == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("semster", semesterKe))
								.add(mulai == null || sampai == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.between("tanggal", mulai, sampai))
								.add(nomorref == null || nomorref.trim().equals("") ? Restrictions.sqlRestriction("1=1")
										: Restrictions.ilike("refNumber", nomorref, MatchMode.ANYWHERE))
								.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
								.createAlias("calonMahasiswa", "calonMahasiswa", Criteria.INNER_JOIN)
								.add(nim == null || nim.trim().equals("") ? Restrictions.sqlRestriction("1=1")
										: Restrictions.ilike("calonMahasiswa.noRegistrasi", nim, MatchMode.ANYWHERE))
								.add(program == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("calonMahasiswa.program", program))
								.add(wnawni == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("calonMahasiswa.kewarganegaraan", wnawni))
								.add(angkatan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("calonMahasiswa.tahun", angkatan))
								.add(Restrictions.eq("tahunAkademik", tahunAkademik)).addOrder(Order.desc("id")).list();
					} else {
						kegiatans = session.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true)).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
								.add(semesterKe == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("semster", semesterKe))
								.add(mulai == null || sampai == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.between("tanggal", mulai, sampai))
								.add(nomorref == null || nomorref.trim().equals("") ? Restrictions.sqlRestriction("1=1")
										: Restrictions.ilike("refNumber", nomorref, MatchMode.ANYWHERE))
								.add(statusMahasiswa == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("statusMahasiswa", statusMahasiswa))
								.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
								.createAlias("mahasiswa", "mahasiswa", Criteria.INNER_JOIN)
								.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
								.add(nim == null || nim.trim().equals("") ? Restrictions.sqlRestriction("1=1")
										: Restrictions.ilike("mahasiswa.nim", nim, MatchMode.ANYWHERE))
								.add(wnawni == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("mahasiswa.warganegara", wnawni))
								.add(program == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("mahasiswa.program", program))
								.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("mahasiswa.jurusan", jurusan))
								.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("jurusan.fakultas", fakultas))
								.add(Restrictions.eq("tahunAkademik", tahunAkademik))

								.add(angkatan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("mahasiswa.tahunangkatan", angkatan))
								.add(semester.equals(Perkuliahan.GENAP) ? Restrictions.in("semster", Common.genap)
										: Restrictions.in("semster", Common.ganjil))
								.addOrder(Order.desc("id")).list();

					}

					int rowIndex = 3;

					int s = 1;
					for (Kegiatan kegiatan : kegiatans) {
						label.setValue("Memproses data " + kegiatan + " ("
								+ Common.numberFormat.get().format(s * 100.0 / kegiatans.size()) + " %)");
						s++;
						List<CicilanPembayaran> cicilanPembayarans = session.createCriteria(CicilanPembayaran.class)
								.addOrder(Order.asc("tanggal")).add(Restrictions.eq("kegiatan", kegiatan)).list();

						if (!cicilanPembayarans.isEmpty()) {
							CicilanPembayaran cicilanPembayaran = cicilanPembayarans.get(0);
							if (cicilanPembayaran != null) {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6,
										cicilanPembayaran.getJenisPembayaran() == null ? ""
												: cicilanPembayaran.getJenisPembayaran().getNama());

								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7,
										cicilanPembayaran.getTanggal() == null ? ""
												: Common.dateFormat3.get().format(cicilanPembayaran.getTanggal()));
								String desc = cicilanPembayaran.getItemBiaya().getNama();
								if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
									PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
											.getPengaturanPembayaranBulanan();
									desc = pengaturanPembayaranBulanan.getKeterangan();

									desc = (desc.isEmpty()
											? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama())
											: desc) + ",  " + pengaturanPembayaranBulanan.getNamaBulan() + " ";

								}
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, desc);
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, cicilanPembayaran.getNilai());
							}
						}

						String lunas = (kegiatan.getLunas() == null ? "" : kegiatan.getLunas() ? "Ya" : "Tidak");
						String persen = Common.numberFormat.get().format(kegiatan.getPersentaseLunas()) + "%";
						if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA)) {
							Mahasiswa mahasiswa = kegiatan.getMahasiswa();
							if (mahasiswa == null) {
								continue;
							}

							HistoryStatusMahasiswa status = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, kegiatan.getTahunAkademik(),
									kegiatan.getSemster());

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, mahasiswa.getNim());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
									mahasiswa.getNama() == null ? "" : mahasiswa.getNama().toUpperCase());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, mahasiswa.getTahunangkatan() + "");

							try {
								BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
								Object[] hp = new Object[] { biodataMahasiswa.getHp(),
										biodataMahasiswa.getTeleponRumah() };
								ais.ui.util.EcampusUtil
										.setCellValue(sheet, rowIndex, 3,
												(hp[0] == null || hp[0].toString().trim().equals("08100000000000000000")
														|| hp[0].toString().trim().equals("0000000000")
																? ""
																: hp[0])
														+ (hp[1] == null || hp[1].toString().trim().isEmpty()
																|| hp[1].toString().trim()
																		.equals("00000000000000000000")
																|| hp[1].toString().trim().equals("000000000")
																		? ""
																		: (hp[0] == null
																				|| hp[0].toString().trim().isEmpty()
																				|| hp[0].toString().trim()
																						.equals("08100000000000000000")
																				|| hp[0].toString().trim()
																						.equals("0000000000") ? ""
																								: " / ")
																				+ hp[1]));
							} catch (Exception e) {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, mahasiswa.getTelp());
							}

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, mahasiswa.getEmail());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, mahasiswa.getAlamat());

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, mahasiswa.getWarganegara());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11,
									status == null || status.getStatusMahasiswa() == null ? ""
											: status.getStatusMahasiswa().getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12,
									mahasiswa.getJurusan().getFakultas().getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, mahasiswa.getJurusan().getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, mahasiswa.getProgram());

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15,
									Common.dateFormat3.get().format(kegiatan.getTanggal()));
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, kegiatan.getValidator());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, kegiatan.getRefNumber());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, kegiatan.getAmount());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, kegiatan.getAmountTerhutang());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20, lunas);
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, persen);
							rowIndex++;
						} else if (jenisPembayaran.getNamaKegiatan()
								.equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
							BiodataCalonMahasiswa mahasiswa = kegiatan.getCalonMahasiswa();
							if (mahasiswa == null) {
								continue;
							}
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, mahasiswa.getNoUjian());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, mahasiswa.getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, mahasiswa.getTahun() + "");

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, mahasiswa.getHp());

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, mahasiswa.getEmail());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, mahasiswa.getAlamat());

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, mahasiswa.getKewarganegaraan());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11,
									kegiatan.getStatusMahasiswa() == null ? ""
											: kegiatan.getStatusMahasiswa().getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12,
									mahasiswa.getProdiLulus() == null ? ""
											: mahasiswa.getProdiLulus().getFakultas().getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13,
									mahasiswa.getProdiLulus() == null ? "" : mahasiswa.getProdiLulus().getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, mahasiswa.getProgram());

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15,
									Common.dateFormat3.get().format(kegiatan.getTanggal()));

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, kegiatan.getValidator());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, kegiatan.getRefNumber());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, kegiatan.getAmount());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, kegiatan.getAmountTerhutang());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20, lunas);
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, persen);
							rowIndex++;
						} else if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {
							BiodataCalonMahasiswa mahasiswa = kegiatan.getCalonMahasiswa();
							if (mahasiswa == null) {
								continue;
							}
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, mahasiswa.getNoRegistrasi());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, mahasiswa.getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, mahasiswa.getTahun() + "");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, mahasiswa.getHp());

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, mahasiswa.getEmail());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, mahasiswa.getAlamat());

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, mahasiswa.getKewarganegaraan());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11, "N/A");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12, "N/A");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, "N/A");
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, mahasiswa.getProgram());

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15,
									Common.dateFormat3.get().format(kegiatan.getTanggal()));
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, kegiatan.getValidator());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, kegiatan.getRefNumber());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, kegiatan.getAmount());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, kegiatan.getAmountTerhutang());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20, lunas);
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, persen);
							rowIndex++;
						} else {
							Mahasiswa mahasiswa = kegiatan.getMahasiswa();
							if (mahasiswa == null) {
								continue;
							}
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, mahasiswa.getNim());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
									mahasiswa.getNama() == null ? "" : mahasiswa.getNama().toUpperCase());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, mahasiswa.getTahunangkatan() + "");
							try {
								BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
								Object[] hp = new Object[] { biodataMahasiswa.getHp(),
										biodataMahasiswa.getTeleponRumah() };
								ais.ui.util.EcampusUtil
										.setCellValue(sheet, rowIndex, 3,
												(hp[0] == null || hp[0].toString().trim().equals("08100000000000000000")
														|| hp[0].toString().trim().equals("0000000000")
																? ""
																: hp[0])
														+ (hp[1] == null || hp[1].toString().trim().isEmpty()
																|| hp[1].toString().trim()
																		.equals("00000000000000000000")
																|| hp[1].toString().trim().equals("000000000")
																		? ""
																		: (hp[0] == null
																				|| hp[0].toString().trim().isEmpty()
																				|| hp[0].toString().trim()
																						.equals("08100000000000000000")
																				|| hp[0].toString().trim()
																						.equals("0000000000") ? ""
																								: " / ")
																				+ hp[1]));
							} catch (Exception e) {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, mahasiswa.getTelp());
							}

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, mahasiswa.getEmail());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, mahasiswa.getAlamat());

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 10, mahasiswa.getWarganegara());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 11,
									kegiatan.getStatusMahasiswa() == null ? ""
											: kegiatan.getStatusMahasiswa().getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 12,
									mahasiswa.getJurusan().getFakultas().getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 13, mahasiswa.getJurusan().getNama());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 14, mahasiswa.getProgram());

							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 15,
									Common.dateFormat3.get().format(kegiatan.getTanggal()));
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16, kegiatan.getValidator());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, kegiatan.getRefNumber());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, kegiatan.getAmount());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, kegiatan.getAmountTerhutang());
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 20, lunas);
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 21, persen);
							rowIndex++;
						}

						for (int i = 1; i < cicilanPembayarans.size(); i++) {
							CicilanPembayaran cicilanPembayaran = cicilanPembayarans.get(i);
							if (cicilanPembayaran != null) {
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6,
										cicilanPembayaran.getJenisPembayaran() == null ? ""
												: cicilanPembayaran.getJenisPembayaran().getNama());

								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 7,
										cicilanPembayaran.getTanggal() == null ? ""
												: Common.dateFormat3.get().format(cicilanPembayaran.getTanggal()));
								String desc = cicilanPembayaran.getItemBiaya().getNama();
								if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
									PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
											.getPengaturanPembayaranBulanan();
									desc = pengaturanPembayaranBulanan.getKeterangan();

									desc = (desc.isEmpty()
											? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama())
											: desc) + ",  " + pengaturanPembayaranBulanan.getNamaBulan() + " ";

								}
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 8, desc);
								ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 9, cicilanPembayaran.getNilai());
								rowIndex++;
							}
						}

					}
					tinggi.setValue(rowIndex);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Mahasiswa Sudah Bayar Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
				ais.action.report.Report.closeCurrentSessionQuietly();
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

	}
}
