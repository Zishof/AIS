package ais.action.report.helper.keuangan;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
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
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapBiayaEcampusWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private MyDatebox start = new MyDatebox();
	private MyDatebox end = new MyDatebox();
	private MyDoublebox biaya = new MyDoublebox(ambilBiayaTransaksiEcampusDefault());

	private Center center = new Center();

	// KE-FIX (NumberFormatException "tidak aktif"/dll): field initializer di atas berjalan
	// SEBELUM try/catch di badan konstruktor sempat aktif (field initializer termasuk
	// prolog konstruktor), jadi kalau admin mengisi Konfigurasi "biaya_transaksi_ecampus"
	// dgn nilai non-angka (mis. "tidak aktif" utk menonaktifkan fitur, atau kesalahan input)
	// Double.parseDouble melempar exception MENTAH yg tidak pernah tertangkap try/catch
	// manapun di kelas ini, membatalkan pembukaan seluruh layar laporan. Parse dgn aman di
	// sini, default ke 0.0 bila nilai konfigurasi bukan angka yang valid.
	private static double ambilBiayaTransaksiEcampusDefault() {
		String nilai = Common.getKonfigurasi("biaya_transaksi_ecampus", "0.0").getNilai();
		try {
			return Double.parseDouble(nilai == null ? "0.0" : nilai.trim());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapBiayaEcampusWindow.java:ambilBiayaTransaksiEcampusDefault");
			return 0.0;
		}
	}

	public LaporanRekapBiayaEcampusWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Biaya Ecampus Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapBiayaEcampusWindow(String title, String border, boolean closable) {
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, 1);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		if (start != null) start.setValue(calendar.getTime());
		row.appendChild(start);
		start.setWidth("90%");
		if (start != null) start.setReadonly(true);

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		if (end != null) end.setValue(calendar.getTime());
		if (end != null) end.setReadonly(true);
		row.appendChild(end);
		end.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya Per Transaksi"));
		row.appendChild(biaya);
		biaya.setWidth("90%");
		biaya.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Konfigurasi konfigurasi = Common.getKonfigurasi("biaya_transaksi_ecampus", "0.0");
				konfigurasi.setNilai(biaya.getValue() == null ? "0.0" : biaya.getValue().toString());
				Session session = ais.action.report.Report.openNativeSession();
				session.getTransaction().begin();
				session.update(konfigurasi);
				session.getTransaction().commit();

				ais.action.report.Report.closeCurrentSessionQuietly();
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
			}
		});

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "6");
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

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(),
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "REKAP_BIAYA.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	// private void

	private void initSpreadsheet() throws Exception {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(center);

				Double b = biaya.getValue() == null ? 0.0 : biaya.getValue();

//				Session session = HibernateUtil.currentSession();

				String sql = "select to_char(a1.tanggal, 'DD-MM-YYYY') as tanggal,  "
						+ "(case a1.validator when a1.validator then a1.validator else 'Tidak ada validator' end) as nama_bank, "
						+ "COUNT(a1.id) as jumlah,  COUNT(a1.id)*" + b + " as total from log_pembayaran a1 "
						+ "inner join kegiatan a on (a1.kegiatan = a.id) "
						+ "left join mahasiswa f on (f.id = a.mahasiswa) "
						+ "left join biodata_calon_mahasiswa g on (g.id = a.calon_mahasiswa) where 1=1 and a1.olehid not ilike '%ais.database.model.Mahasiswa%'  and a1.olehid not ilike '%ais.database.model.BiodataCalonMahasiswa%' "

						+ (start.getValue() == null ? " "
								: " and (a1.tanggal) >= ('" + Common.databaseDateFormat.get().format(start.getValue())
										+ " 00:00:00') ")

						+ (end.getValue() == null ? " "
								: " and (a1.tanggal) <= ('" + Common.databaseDateFormat.get().format(end.getValue())
										+ " 23:59:59') ")

						+ "group by a1.validator,to_char(a1.tanggal, 'DD-MM-YYYY') order by max(a1.tanggal),a1.validator";
				System.out.println(sql);
				List<Object[]> jurusans = Common.ambilSql(sql);

				spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);
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
				} catch (Exception e4) { ais.common.ErrorAuditUtil.record(e4, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapBiayaEcampusWindow.java:216");

				}

				ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, "REKAPITULASI BIAYA ADMINISTRASI PEMBAYARAN ");

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
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Validator");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Jumlah Transaksi");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Total Biaya");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Jumlah Transaksi\nPer Tanggal");
				cell = Utils.getCell(sheet, rowIndex, 4);
				cell.getCellStyle().setWrapText(true);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Total Transaksi\nPer Tanggal");
				cell = Utils.getCell(sheet, rowIndex, 5);
				cell.getCellStyle().setWrapText(true);

				Utils.setRowHeight(sheet, rowIndex, 50);
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
				ais.ui.util.EcampusUtil.setBold(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

				rowIndex = 3;
				colIndex = 0;

				String tanggal = "";
				Double jumlahTotal = 0.0;
				Double jumlahTotalPertanggal = 0.0;
				Integer jumlah = 0;
				Integer jumlahPertanggal = 0;
				for (Object[] objects : jurusans) {
					if (objects[0] == null) {
						continue;
					}
					if (!tanggal.equals(objects[0].toString())) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, objects[0].toString());
						tanggal = objects[0].toString();

						if (rowIndex != 3) {
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 4,
									Common.numberFormat.get().format(jumlahPertanggal));
							ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 5,
									Common.numberFormat.get().format(jumlahTotalPertanggal));
						}

						jumlahTotalPertanggal = 0.0;
						jumlahPertanggal = 0;
					} else {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "");
					}
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
							objects[1] == null ? "" : objects[1].toString());
					Integer c = new Integer(objects[2] == null ? "0" : objects[2].toString());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.numberFormat.get().format(c));
					jumlah += c;
					jumlahPertanggal += c;
					Double total = new Double(objects[3] == null ? "0.0" : objects[3].toString());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.numberFormat.get().format(total));
					jumlahTotal += total;
					jumlahTotalPertanggal += total;
					try {
						ais.ui.util.EcampusUtil.setBorder(sheet,
								new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapBiayaEcampusWindow.java:292");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Biaya Ecampus Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});

					}

					rowIndex++;
				}

				if (rowIndex != 3) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 4,
							Common.numberFormat.get().format(jumlahPertanggal));
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex - 1, 5,
							Common.numberFormat.get().format(jumlahTotalPertanggal));
				}

				colIndex = 0;

				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.numberFormat.get().format(jumlah));
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, spreadsheet.getMaxcolumns() - 1,
						Common.numberFormat.get().format(jumlahTotal));
				try {
					ais.ui.util.EcampusUtil.setBorder(sheet,
							new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
							BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(spreadsheet.getMaxcolumns() - 2, 3, spreadsheet.getMaxcolumns() - 1, rowIndex),
							true);
				} catch (Exception e3) { ais.common.ErrorAuditUtil.record(e3, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapBiayaEcampusWindow.java:322");

				}

				sql = "select "
						+ "(case a1.validator when a1.validator then a1.validator else 'Tidak ada validator' end) as nama_bank, "
						+ "COUNT(a1.id) as jumlah,  COUNT(a1.id)*" + b + " as total "
						+ "from log_pembayaran a1 left join kegiatan a on (a1.kegiatan = a.id) "
						+ "left join mahasiswa f on (f.id = a.mahasiswa) "
						+ "left join biodata_calon_mahasiswa g on (g.id = a.calon_mahasiswa) where 1=1  and a1.olehid not ilike '%ais.database.model.Mahasiswa%'  and a1.olehid not ilike '%ais.database.model.BiodataCalonMahasiswa%' "

						+ (start.getValue() == null ? " "
								: " and (a1.tanggal) >= ('" + Common.databaseDateFormat.get().format(start.getValue())
										+ " 00:00:00') ")

						+ (end.getValue() == null ? " "
								: " and (a1.tanggal) <= ('" + Common.databaseDateFormat.get().format(end.getValue())
										+ " 23:59:59') ")

						+ " group by a1.validator order by a1.validator";
				System.out.println(sql);
				jurusans = Common.ambilSql(sql);

				rowIndex += 3;
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "REKAP TOTAL");
				try {
					ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, 0, rowIndex, spreadsheet.getMaxcolumns() - 1,
							false);
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
				} catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapBiayaEcampusWindow.java:352");

				}
				++rowIndex;
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Validator");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Jumlah Transaksi");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Jumlah Total");
				try {
					ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, 2, rowIndex), BookHelper.BORDER_FULL,
							BorderStyle.THIN, color);
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(0, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
				} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapBiayaEcampusWindow.java:364");
				}
				rowIndex++;
				jumlahTotal = 0.0;
				jumlah = 0;
				for (Object[] objects : jurusans) {

					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0,
							objects[0] == null ? "" : objects[0].toString());
					Integer c = new Integer(objects[1] == null ? "0" : objects[1].toString());
					Double total = new Double(objects[2] == null ? "0.0" : objects[2].toString());
					jumlahTotal += total;
					jumlah += c;
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, Common.numberFormat.get().format(c));
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.numberFormat.get().format(total));
					try {
						ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, 2, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapBiayaEcampusWindow.java:382");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Biaya Ecampus Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}
					rowIndex++;
				}
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "TOTAL");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, Common.numberFormat.get().format(jumlah));
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.numberFormat.get().format(jumlahTotal));
				try {
					ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(0, rowIndex, 2, rowIndex), BookHelper.BORDER_FULL,
							BorderStyle.THIN, color);
					Common.setStyled(sheet);
					spreadsheet.setMaxrows(rowIndex + 2);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapBiayaEcampusWindow.java:394");
					// Common.tampilErrorJikaAdmin(e);
				}

				Utils.setColumnWidth(sheet, 0, 130);
				Utils.setColumnWidth(sheet, 1, 150);
				Utils.setColumnWidth(sheet, 2, 150);
				Utils.setColumnWidth(sheet, 3, 150);
				Utils.setColumnWidth(sheet, 4, 150);
				Utils.setColumnWidth(sheet, 5, 150);

				// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

			}
		});
	}
}
