package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Anggota;
import ais.database.model.library.KunjunganAnggota;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekap kunjungan anggota window. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataPerpustakaanBanbox
 * perpustakaan}, {@code Spreadsheet spreadsheet}, {@code MyDatebox start}, {@code MyDatebox end}, {@code Center
 * center}; inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}); konfigurasi constructor: {@code
 * perpustakaan}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapKunjunganAnggotaWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private MyDatebox start = new MyDatebox();
	private MyDatebox end = new MyDatebox(ais.ui.util.WaktuUtil.getDate());

	private Center center = new Center();

	public LaporanRekapKunjunganAnggotaWindow() {
		super();
		try {
			perpustakaan = new AmbilDataPerpustakaanBanbox();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Kunjungan Anggota Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapKunjunganAnggotaWindow(String title, String border, boolean closable) {
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
		perpustakaan.setWidth("90%");

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

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "6");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "DATA_KUNJUNGAN_ANGGOTA.xlsx");
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		Perpustakaan perpustakaan = (Perpustakaan) (this.perpustakaan.getAttribute("perpustakaan"));

		Session session = HibernateUtil.currentSession();

		List<KunjunganAnggota> kembaliPengadaanItems = session.createCriteria(KunjunganAnggota.class)
				.add(perpustakaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("perpustakaan", perpustakaan))

				.add(start.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction("(this_.tanggal) >= ('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + " 00:00:00')"))

				.add(end.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction("(this_.tanggal) <= ('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + " 23:59:59')"))

				.setMaxResults(1048576).addOrder(Order.desc("id")).list();

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		if (kembaliPengadaanItems.size() == 0) {
			return;
		}
		spreadsheet.setMaxcolumns(6);
		spreadsheet.setMaxrows(kembaliPengadaanItems.size() + 5);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(40);
		try {
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);
		} catch (Exception e4) { ais.common.ErrorAuditUtil.record(e4, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapKunjunganAnggotaWindow.java:204");

		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"DATA KUNJUNGAN ANGGOTA\n " + (perpustakaan == null ? "SEMUA PERPUSTAKAAN"
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
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Perpustakaan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Anggota");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Jenis");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Fakultas");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Jurusan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Jumlah Kunjungan");

		Utils.setRowHeight(sheet, rowIndex, 50);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;

		Map<String, Map> data = new HashMap<String, Map>();
		for (KunjunganAnggota kunjunganAnggota : kembaliPengadaanItems) {
			Anggota anggota = kunjunganAnggota.getAnggota();
			Perpustakaan pus = kunjunganAnggota.getPerpustakaan();

			String key = (anggota == null || anggota.getId() == null ? -1L : anggota.getId()) + "_" + pus.getId();

			Map d = data.get(key);
			if (d == null) {
				d = new HashMap();
				d.put("anggota", anggota);
				d.put("perpustakaan", pus);
			}
			Integer jml = (Integer) (d.get("jumlah") == null ? 0 : d.get("jumlah"));
			d.put("jumlah", jml + 1);
			data.put(key, d);
		}

		for (Map dataKunjungan : data.values()) {

			Anggota anggota = (Anggota) dataKunjungan.get("anggota");
			Perpustakaan pus = (Perpustakaan) dataKunjungan.get("perpustakaan");
			Integer jml = (Integer) (dataKunjungan.get("jumlah") == null ? 0 : dataKunjungan.get("jumlah"));
			if (anggota == null) {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, pus.getNama());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Pengunjung bukan anggota");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "");
			} else {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, pus.getNama());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, anggota.toString());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2,
						anggota.getTipeAnggota() == null ? "" : anggota.getTipeAnggota().getNama());

				if (anggota.getMahasiswa() != null) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3,
							anggota.getMahasiswa().getJurusan().getFakultas().getNama());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4,
							anggota.getMahasiswa().getJurusan().getNama());
				} else if (anggota.getDosen() != null) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3,
							anggota.getDosen().getJurusan() == null ? ""
									: anggota.getDosen().getJurusan().getFakultas().getNama());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4,
							anggota.getDosen().getJurusan() == null ? "" : anggota.getDosen().getJurusan().getNama());
				} else {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "");
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "");
				}
			}

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, jml);

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet,
						new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
						BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/library/LaporanRekapKunjunganAnggotaWindow.java:297");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Kunjungan Anggota Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});

			}

			rowIndex++;
		}

		data = null;
		Utils.setColumnWidth(sheet, 0, 130);
		Utils.setColumnWidth(sheet, 1, 350);
		Utils.setColumnWidth(sheet, 2, 120);
		Utils.setColumnWidth(sheet, 3, 200);
		Utils.setColumnWidth(sheet, 4, 200);
		Utils.setColumnWidth(sheet, 5, 100);

		// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
		ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
	}
}
