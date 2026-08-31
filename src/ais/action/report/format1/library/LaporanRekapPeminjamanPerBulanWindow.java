package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.common.Common;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekap peminjaman per bulan window. Kelas ini mengubah
 * data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataPerpustakaanBanbox
 * perpustakaan}, {@code Spreadsheet spreadsheet}, {@code Combobox tahun}, {@code Combobox bulan}, {@code Center
 * center}; inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initSpreadsheet()});
 * konfigurasi constructor: {@code perpustakaan}. Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapPeminjamanPerBulanWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Combobox tahun = new Combobox();
	private Combobox bulan = new Combobox();

	private Center center = new Center();

	public LaporanRekapPeminjamanPerBulanWindow() {
		super();
		try {
			perpustakaan = new AmbilDataPerpustakaanBanbox();
			initFakultas();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Peminjaman Per Bulan Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void initFakultas() {

	}

	public LaporanRekapPeminjamanPerBulanWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun);
		tahun.setWidth("90%");
		tahun.setReadonly(true);
		int y = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = (y - 50); i <= y; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}
		Common.selectComboItem(tahun, y);

		row.appendChild(new ais.ui.util.MyLabelConfig("Bulan"));
		row.appendChild(bulan);
		bulan.setWidth("90%");
		bulan.setReadonly(true);

		for (int i = 1; i <= 12; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			bulan.appendChild(comboitem);
		}
		Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);

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
						"REKAP_PEMINJAMAN_ANGGOTA_PER_BULAN.xlsx");
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

		Integer thn = (Integer) tahun.getSelectedItem().getValue();
		Integer bln = (Integer) bulan.getSelectedItem().getValue();
		String blnStr = "0000" + bln;
		blnStr = blnStr.substring(blnStr.length() - 2);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, bln - 1);
		calendar.set(Calendar.YEAR, thn);
		int maxDate = calendar.getActualMaximum(Calendar.DATE);
		System.out.println("maxDate = " + maxDate);

		String subStr = "";
		for (int i = 1; i <= maxDate; i++) {
			String tglStr = "0000" + i;
			tglStr = tglStr.substring(tglStr.length() - 2);
			subStr += " sum(case when date(a.tanggal_pembuatan) = date('" + thn + "-" + blnStr + "-" + tglStr
					+ "') and a.id is not null then 1 else 0 end) as tgl" + tglStr + ",";
		}

		String sql = "select " + " e.nama as tipe_anggota, " + " i.nama as fakultas, f.nama as jurusan, ";

		sql += subStr;

		sql += " sum(case when a.tanggal_pembuatan is not null and a.id is not null then 1 else 0 end) as total " +

				" from library.tipe_anggota e  " + " left join library.anggota b on (e.id = b.tipe_anggota) "
				+ " left join library.peminjaman_pengadaan_item a on (b.id = a.anggota)   "
				+ " left join library.perpustakaan d on (d.id = a.perpustakaan)    "
				+ " left join mahasiswa g on (g.id = b.mahasiswa) " + " left join jurusan f on (f.id = g.jurusan) "
				+ " left join fakultas i on (i.id = f.fakultas) where 1=1   "

				+ (perpustakaan == null ? " " : " and a.perpustakaan = " + perpustakaan.getId() + " ")

				+ " and date(a.tanggal_pembuatan) between date('" + thn + "-" + blnStr + "-01') and  date('" + thn + "-"
				+ blnStr + "-" + maxDate + "') "

				+ " group by e.nama,f.nama, i.nama  order by e.nama";

		System.out.println(sql);
		List<Object[]> jurusans = Common.ambilSql(sql);

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");

		spreadsheet.setMaxcolumns(36);
		spreadsheet.setMaxrows(jurusans.size() + 25);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);
		} catch (Exception e4) { ais.common.ErrorAuditUtil.record(e4, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapPeminjamanPerBulanWindow.java:231");

		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI PEMINJAMAN ANGGOTA PER BULAN\n " + (perpustakaan == null ? "SEMUA PERPUSTAKAAN"
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
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Tipe Anggota");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Fakultas");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Jurusan");

		for (int i = 3; i <= (maxDate + 2); i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, i, (i - 2) + "-" + blnStr + "-" + thn);
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (maxDate + 3), "Sub Total");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (maxDate + 4), "Total");

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
		Integer[] totals = new Integer[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0 };
		Integer jumlahPertanggal = 0;
		for (Object[] objects : jurusans) {
			if (!tanggal.equals(objects[0].toString())) {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
				tanggal = objects[0].toString();

				if (rowIndex != 3) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, (maxDate + 4), jumlahPertanggal);
				}

				jumlahPertanggal = 0;
			} else {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
			}

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, objects[1] == null ? "" : objects[1].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, objects[2] == null ? "" : objects[2].toString());

			for (int i = 3; i <= (maxDate + 2); i++) {
				String ss = objects[i] == null ? "0" : objects[i].toString();
				Integer c = new Integer(ss);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, i, c);

				totals[i - 2] += c;
			}

			Integer c = new Integer(objects[(maxDate + 3)] == null ? "0" : objects[(maxDate + 3)].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (maxDate + 3), c);
			jumlah += c;
			jumlahPertanggal += c;

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapPeminjamanPerBulanWindow.java:309");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Peminjaman Per Bulan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});

			}

			rowIndex++;
		}

		if (rowIndex != 3) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, (maxDate + 4), jumlahPertanggal);
		}

		colIndex = 0;

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");

		for (int i = 3; i <= (maxDate + 2); i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, i, totals[i - 2]);
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (maxDate + 4), jumlah);
		try {
			ais.ui.util.EcampusUtil.setBorder(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(spreadsheet.getMaxcolumns() - 2, 3, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e3) { ais.common.ErrorAuditUtil.record(e3, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapPeminjamanPerBulanWindow.java:338");

		}

		Utils.setColumnWidth(sheet, 0, 100);
		Utils.setColumnWidth(sheet, 1, 150);
		Utils.setColumnWidth(sheet, 2, 200);
		for (int i = 3; i <= (maxDate + 2); i++) {
			Utils.setColumnWidth(sheet, i, 70);
		}
		Utils.setColumnWidth(sheet, (maxDate + 3), 60);
		Utils.setColumnWidth(sheet, (maxDate + 4), 60);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
