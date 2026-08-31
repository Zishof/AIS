package ais.action.report.helper.statistik;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
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
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan proporsi jumlahmahasiswapendaftar. Kelas ini mengubah
 * data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjenisujian}, {@code Spreadsheet spreadsheet}, {@code Center center}; inisialisasi/lifecycle
 * ({@code init()}, {@code initSpreadsheet()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanProporsiJumlahmahasiswapendaftar extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjenisujian = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

	public LaporanProporsiJumlahmahasiswapendaftar() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Proporsi Jumlahmahasiswapendaftar", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanProporsiJumlahmahasiswapendaftar(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		setClosable(true);
		setTitle("Laporan Proporsi Jumlah mahasiswa pendaftar yang diterima dan registrasi");
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
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Jenis Seleksi"));
		Common.insertCombo(searchjenisujian, "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row.appendChild(searchjenisujian);
		searchjenisujian.setWidth("90%");
		searchjenisujian.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
						"Proporsi_Jumlah_mahasiswa_pendaftar_yang_diterima_dan_registrasi.xlsx");
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
		JenisSeleksi jenisSeleksi = (JenisSeleksi) (searchjenisujian.getSelectedItem() == null ? null
				: searchjenisujian.getSelectedItem().getValue());
		if (fakultas == null || jenisSeleksi == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();
		List<Jurusan> jurusans = session.createCriteria(Jurusan.class).add(Restrictions.eq("fakultas", fakultas))
				.list();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(16);
		spreadsheet.setMaxrows(jurusans.size() + 8);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"Proporsi Jumlah mahasiswa pendaftar yang diterima dan registrasi  melalui ".toUpperCase()
						+ jenisSeleksi.getNama().toUpperCase() + "\n " + "" + Common.getBahasaConfig("Fakultas") + " "
						+ fakultas.getNama().toUpperCase());
		Utils.setRowHeight(sheet, 1, 100);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
		final String color = "#000000";

		int rowIndex = 2;
		int colIndex = 1;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Jurusan");
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 4; i <= ais.ui.util.WaktuUtil.getCalendar()
				.get(Calendar.YEAR); i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, (i - 1) + "/" + i);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex, "D");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 1, "T");
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, colIndex + 2, "R");

			Utils.setColumnWidth(sheet, colIndex, 50);
			Utils.setColumnWidth(sheet, colIndex + 1, 50);
			Utils.setColumnWidth(sheet, colIndex + 2, 50);

			ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 2, false);

			colIndex += 3;
		}

		// ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, 0, rowIndex + 1, 1, false);

		Utils.setRowHeight(sheet, rowIndex, 50);
		Utils.setRowHeight(sheet, rowIndex + 1, 30);

		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), true);

		rowIndex = 4;

		for (Jurusan jurusan : jurusans) {
			colIndex = 0;
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, jurusan.getNama());

			colIndex = 1;
			for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 4; i <= ais.ui.util.WaktuUtil.getCalendar()
					.get(Calendar.YEAR); i++) {
				Long d = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.eq("prodi1", jurusan), Restrictions.eq("prodi2", jurusan)))
						.add(Restrictions.eq("tahun", i)).add(Restrictions.eq("jenisSeleksi", jenisSeleksi))
						.setProjection(Projections.rowCount()).uniqueResult()).longValue();
				Long t = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("prodiLulus", jurusan)).add(Restrictions.eq("tahun", i))
						.add(Restrictions.eq("jenisSeleksi", jenisSeleksi)).setProjection(Projections.rowCount())
						.uniqueResult()).longValue();
				Long r = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("prodiLulus", jurusan)).add(Restrictions.isNotNull("nim"))
						.add(Restrictions.not(Restrictions.eq("nim", "")))
						.add(Restrictions.eq("jenisSeleksi", jenisSeleksi)).add(Restrictions.eq("tahun", i))
						.setProjection(Projections.rowCount()).uniqueResult()).longValue();
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, d);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, t);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, r);
				colIndex += 3;
			}

			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
					BookHelper.BORDER_FULL, BorderStyle.THIN, color);

			rowIndex++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 1, 0, "D = Mahasiswa yang mendaftar tes masuk");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 2, 0, "T = Mahasiswa yang diterima");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex + 3, 0, "R = Mahasiswa yang registrasi ulang");

		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex + 1, 0, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex + 2, 0, rowIndex + 2, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex + 3, 0, rowIndex + 3, spreadsheet.getMaxcolumns() - 1, false);

		Utils.setAlignment(sheet, new Rect(colIndex, 4, colIndex, rowIndex), CellStyle.ALIGN_LEFT);
		Utils.setAlignment(sheet, new Rect(colIndex + 1, 4, spreadsheet.getMaxcolumns() - 1, rowIndex),
				CellStyle.ALIGN_RIGHT);
		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(colIndex, rowIndex, colIndex + 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, 2, spreadsheet.getMaxcolumns() - 1, 2), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, 3, spreadsheet.getMaxcolumns() - 1, 3), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		Utils.setColumnWidth(sheet, 0, 450);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
