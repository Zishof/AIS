package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.List;

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

import ais.action.master.dashboard.library.DashboardStatistikKunjunganAnggota;
import ais.action.master.dashboard.library.DashboardStatistikKunjunganMahasiswa;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekap kunjungan window. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataPerpustakaanBanbox
 * perpustakaan}, {@code Spreadsheet spreadsheet}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code MyDatebox start}, {@code MyDatebox end}, {@code Center center}; inisialisasi/lifecycle
 * ({@code initFakultas()}, {@code init()}, {@code initSpreadsheet()}); konfigurasi constructor: {@code
 * perpustakaan}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapKunjunganWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private MyDatebox start = new MyDatebox();
	private MyDatebox end = new MyDatebox();

	private Center center = new Center();

	public LaporanRekapKunjunganWindow() {
		super();
		try {
			perpustakaan = new AmbilDataPerpustakaanBanbox();
			initFakultas();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Kunjungan Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void initFakultas() {

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));

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

	public LaporanRekapKunjunganWindow(String title, String border, boolean closable) {
		super(title, border, closable);
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

		MyTabConfig tab1 = new MyTabConfig("Rekap Kunjungan Mahasiswa");
		tab1.setParent(tabs);

		MyTabConfig tab11 = new MyTabConfig("Statistik Kunjungan Mahasiswa");
		tab11.setParent(tabs);

		MyTabConfig tab12 = new MyTabConfig("Rekap Kunjungan Anggota");
		tab12.setParent(tabs);

		MyTabConfig tab121 = new MyTabConfig("Statistik Kunjungan Anggota");
		tab121.setParent(tabs);

		MyTabConfig tab13 = new MyTabConfig("Rekap Kunjungan Per Bulan");
		tab13.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Data Kunjungan Anggota");
		tab2.setParent(tabs);

		MyTabConfig tab20 = new MyTabConfig("Rekap Pengunjung");
		tab20.setParent(tabs);

		MyTabConfig tab21 = new MyTabConfig("Data Kunjungan Item");
		tab21.setParent(tabs);
		
		MyTabConfig tab31 = new MyTabConfig("Statistik Kunjungan Aktif");
		tab31.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		final Tabpanel tabpanel11 = new ais.ui.util.MyTabpanel();
		tabpanel11.setParent(tabpanels);
		tab11.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel11.getChildren().size() == 0) {
					DashboardStatistikKunjunganMahasiswa laporanKHS = new DashboardStatistikKunjunganMahasiswa();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel11);
				}
			}
		});

		final Tabpanel tabpanel12 = new ais.ui.util.MyTabpanel();
		tabpanel12.setParent(tabpanels);
		tab12.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel12.getChildren().size() == 0) {
					LaporanRekapKunjunganPerPerpustakaanWindow laporanKHS = new LaporanRekapKunjunganPerPerpustakaanWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel12);
				}
			}
		});

		final Tabpanel tabpanel121 = new ais.ui.util.MyTabpanel();
		tabpanel121.setParent(tabpanels);
		tab121.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel121.getChildren().size() == 0) {
					DashboardStatistikKunjunganAnggota laporanKHS = new DashboardStatistikKunjunganAnggota();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel121);
				}
			}
		});

		final Tabpanel tabpanel13 = new ais.ui.util.MyTabpanel();
		tabpanel13.setParent(tabpanels);
		tab13.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel13.getChildren().size() == 0) {
					LaporanRekapKunjunganPerBulanWindow laporanKHS = new LaporanRekapKunjunganPerBulanWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel13);
				}
			}
		});

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					LaporanKunjunganWindow laporanKHS = new LaporanKunjunganWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel2);
				}
			}
		});

		final Tabpanel tabpanel20 = new ais.ui.util.MyTabpanel();
		tabpanel20.setParent(tabpanels);
		tab20.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel20.getChildren().size() == 0) {
					LaporanRekapKunjunganAnggotaWindow laporanKHS = new LaporanRekapKunjunganAnggotaWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel20);
				}
			}
		});

		final Tabpanel tabpanel21 = new ais.ui.util.MyTabpanel();
		tabpanel21.setParent(tabpanels);
		tab21.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel21.getChildren().size() == 0) {
					LaporanKunjunganItemWindow laporanKHS = new LaporanKunjunganItemWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel21);
				}
			}
		});
		

		final Tabpanel tabpanel31 = new ais.ui.util.MyTabpanel();
		tabpanel31.setParent(tabpanels);
		tab31.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel31.getChildren().size() == 0) {
					LaporanRekapKunjunganAnggotaAktifWindow laporanKHS = new LaporanRekapKunjunganAnggotaAktifWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel31);
				}
			}
		});
		
		
		

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		// setTitle("Rekap Pembayaran Host to Host");
		setPosition("center");

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan);

		perpustakaan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		row.appendChild(start);
		start.setWidth("90%");
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		row.appendChild(end);
		end.setWidth("90%");

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "10");
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
						"REKAP_KUNJUNGAN_ANGGOTA.xlsx");
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

	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		Perpustakaan perpustakaan = (Perpustakaan) (this.perpustakaan.getAttribute("perpustakaan"));
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		String sql = "select  to_char(a.tgl, 'DD-MM-YYYY') as tanggal,  " + "max(d.nama) as perpustakaan,    "
				+ "max(f.nama) as fakultas,    " + "max(e.nama) as jurusan,    " + "count(c.id) as jumlah    "
				+ "from library.kunjungan_anggota a    " + "inner join library.anggota b on (b.id = a.anggota)    "
				+ "inner join mahasiswa c on (b.mahasiswa = c.id)    "
				+ "inner join library.perpustakaan d on (d.id = a.perpustakaan)    "
				+ "inner join jurusan e on (e.id = c.jurusan)    " + "inner join fakultas f on (f.id = e.fakultas) "

				+ (perpustakaan == null ? " " : " and a.perpustakaan = " + perpustakaan.getId() + " ")

				+ (fakultas == null ? " " : " and f.id = " + fakultas.getId() + " ")

				+ (jurusan == null ? " " : " and e.id = " + jurusan.getId() + " ")

				+ (start.getValue() == null ? " "
						: " and (a.tanggal) >= ('" + Common.databaseDateFormat.get().format(start.getValue())
								+ " 00:00:00') ")

				+ (end.getValue() == null ? " "
						: " and (a.tanggal) <= ('" + Common.databaseDateFormat.get().format(end.getValue()) + " 23:59:59') ")

				+ " group by d.id,f.id,e.id,to_char(a.tgl, 'DD-MM-YYYY')  " + " order by max(a.tgl),max(d.nama)";

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");

		spreadsheet.setMaxcolumns(6);
		spreadsheet.setMaxrows(jurusans.size() + 25);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);
		} catch (Exception e4) { ais.common.ErrorAuditUtil.record(e4, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapKunjunganWindow.java:444");

		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI KUNJUNGAN MAHASISWA\n " + (perpustakaan == null ? "SEMUA PERPUSTAKAAN"
						: "PERPUSTAKAAN " + perpustakaan.getNama().toUpperCase()));

		Utils.setRowHeight(sheet, 1, 120);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		final String color = "#000000";
		int rowIndex = 2;
		int colIndex = 0;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Tanggal");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Perpustakaan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Fakultas");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Jurusan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Jumlah Pengunjung");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Jumlah Pengunjung\nPer Tanggal");
		cell = Utils.getCell(sheet, rowIndex, 5);
		cell.getCellStyle().setWrapText(true);

		Utils.setRowHeight(sheet, rowIndex, 50);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		String tanggal = "";
		Integer jumlah = 0;
		Integer jumlahPertanggal = 0;
		for (Object[] objects : jurusans) {
			if (!tanggal.equals(objects[0].toString())) {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
				tanggal = objects[0].toString();

				if (rowIndex != 3) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 5, jumlahPertanggal);
				}

				jumlahPertanggal = 0;
			} else {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
			}
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[1] == null ? "" : objects[1].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, objects[2] == null ? "" : objects[2].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, objects[3] == null ? "" : objects[3].toString());

			Integer c = new Integer(objects[4] == null ? "0" : objects[4].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, c);
			jumlah += c;
			jumlahPertanggal += c;

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapKunjunganWindow.java:510");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Kunjungan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});

			}

			rowIndex++;
		}

		if (rowIndex != 3) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 5, jumlahPertanggal);
		}

		colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, jumlah);
		try {
			ais.ui.util.EcampusUtil.setBorder(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 2, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e3) { ais.common.ErrorAuditUtil.record(e3, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapKunjunganWindow.java:534");

		}

		Utils.setColumnWidth(sheet, 0, 130);
		Utils.setColumnWidth(sheet, 1, 350);
		Utils.setColumnWidth(sheet, 2, 250);
		Utils.setColumnWidth(sheet, 3, 350);
		Utils.setColumnWidth(sheet, 4, 100);
		Utils.setColumnWidth(sheet, 5, 150);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
