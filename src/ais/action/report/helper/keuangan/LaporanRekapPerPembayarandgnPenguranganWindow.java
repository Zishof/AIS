package ais.action.report.helper.keuangan;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekap per pembayarandgn pengurangan window. Kelas ini
 * mengubah data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa
 * memindahkan aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code MyDatebox mulaiTanggal}, {@code MyDatebox
 * sampaiTanggal}, {@code Spreadsheet spreadsheet}, {@code Combobox jenisPembayaran}, {@code PembayaranUtil
 * pembayaranUtil}; inisialisasi/lifecycle ({@code init()}, {@code initSpreadsheet()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapPerPembayarandgnPenguranganWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private MyDatebox mulaiTanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
	private MyDatebox sampaiTanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Combobox jenisPembayaran = new Combobox();
	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	private Center center = new Center();

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public LaporanRekapPerPembayarandgnPenguranganWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Per Pembayarandgn Pengurangan Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapPerPembayarandgnPenguranganWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		jenisPembayaran = Common.createComboJenisPembayaran(jenisPembayaran);

		setClosable(true);
		setTitle("Rekap Per Pembayaran dengan Pengurangan");
		setWidth("800px");
		setHeight("90%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		// MyComboitemConfig comboitem = new
		// MyComboitemConfig("Semua "+"Fakultas");
		// comboitem.setValue(null);
		// searchfakultas.appendChild(comboitem);
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		// searchfakultas.setSelectedItem(comboitem);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
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

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulaiTanggal);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampaiTanggal);

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
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "RekapPerTanggalPembayaran.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	// private void

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());
		String semester = (String) (this.semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? null
						: this.semesterAbsensi.getSelectedItem().getValue());
		JenisKegiatan jenisPembayaran = (JenisKegiatan) (this.jenisPembayaran.getSelectedItem() == null ? null
				: this.jenisPembayaran.getSelectedItem().getValue());
		if (jenisPembayaran == null || tahunAkademik == null || semester == null || mulaiTanggal.getValue() == null
				|| sampaiTanggal.getValue() == null) {
			return;
		}
		JenisKegiatan jenisKegiatan = jenisPembayaran;
		Session session = HibernateUtil.currentSession();
		String sql = "";
		try {
			sql = "select  " + "to_char(a.tanggal, 'DD-MM-YYYY') as formated_tanggal,  "

					+ "a.amount,  " + "count(a.id) as jumlah,  " + "sum(a.amount) as total_amount, " + "a.pengurangan,"
					+ "sum(a.pengurangan) as total_pengurangan "

					+ "from kegiatan a " + "left join mahasiswa b on (a.mahasiswa = b.id)  "
					+ "left join biodata_calon_mahasiswa bb on (a.calon_mahasiswa = bb.id)  "
					+ "left join jurusan c on (b.jurusan = c.id or bb.prodi_lulus = c.id)  " + "where  a.aktif and c.fakultas = "
					+ (fakultas == null ? "c.fakultas" : fakultas.getId()) + " and a.tahun_akademik = '"
					+ tahunAkademik.trim() + "' and a.semster % 2 " + (semester.equals(Perkuliahan.GENAP) ? "=" : "!=")
					+ " 0  " + "and "
					+ (mulaiTanggal.getValue() == null ? ""
							: " a.tanggal >= '" + dateFormat.format(mulaiTanggal.getValue()) + "'")
					+ "  " + "and "
					+ (sampaiTanggal.getValue() == null ? ""
							: " a.tanggal <= '" + dateFormat.format(sampaiTanggal.getValue()) + "'")
					+ " and a.jenis_kegiatan = " + jenisKegiatan.getId() + " "
					+ "group by to_char(a.tanggal, 'DD-MM-YYYY') ,a.amount ,a.pengurangan "
					+ "order by max(a.tanggal),a.amount";

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Pembayarandgn Pengurangan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		List<Object[]> objs = session.createSQLQuery(sql).list();

		int jumlahBedaTanggal = 0;
		String currTgl = "";
		for (Object[] obj : objs) {
			if (!currTgl.equals(obj[0].toString())) {
				jumlahBedaTanggal += 2;
			}
			currTgl = obj[0].toString();
		}

		spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setParent(center);
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(7);
		spreadsheet.setMaxrows(objs.size() + jumlahBedaTanggal + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(30);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI KEUANGAN PER PEMBAYARAN\n " + "" + "Fakultas" + " "
						+ (fakultas == null ? "Semua" : fakultas.getNama().toUpperCase()) + "\n TAHUN AKADEMIK "
						+ tahunAkademik + "\n SEMESTER " + semester);
		Utils.setRowHeight(sheet, 1, 100);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, 1, 1), true);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
		Cell cell = Utils.getCell(sheet, 1, 0);
		cell.getCellStyle().setWrapText(true);
		cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);

		final String color = "#000000";

		int rowIndex = 2;
		int colIndex = 0;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "Tanggal");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Tarif");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Jumlah Pemasukan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "Total Pemasukan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Pengurangan (Diskon)");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Total Pengurangan");
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 6, "Selisih");
		Utils.setRowHeight(sheet, 2, 50);
		try {
			ais.ui.util.EcampusUtil.setBorder(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
					BorderStyle.THIN, color);
			ais.ui.util.EcampusUtil.setBold(sheet,
					new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerPembayarandgnPenguranganWindow.java:283");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Pembayarandgn Pengurangan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});

		}

		rowIndex = 3;
		colIndex = 0;
		Double total = 0.0;
		Integer jmls = 0;
		Double totalp = 0.0;

		currTgl = "";
		int index = 0;
		for (Object[] obj : objs) {
			if (currTgl.equals(obj[0].toString())) {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, "");
			} else {
				if (index != 0) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, jmls);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, total);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 4, totalp);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 6, total - totalp);

					try {
						ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, colIndex + 6, rowIndex),
								true);
						ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(colIndex, rowIndex, colIndex + 6, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerPembayarandgnPenguranganWindow.java:310");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Pembayarandgn Pengurangan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});

					}
					rowIndex++;
					rowIndex++;
					total = 0.0;
					totalp = 0.0;
					jmls = 0;

				}
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, obj[0]);
			}
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, obj[1]);
			Integer jml = Integer.parseInt(obj[2].toString());
			Double subtotal = Double.parseDouble(obj[3].toString());
			Double subtotalp = Double.parseDouble(obj[5].toString());

			total += subtotal;
			jmls += jml;
			totalp += subtotalp;

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, jml);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, subtotal);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 4, obj[4].toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 5, subtotalp);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 6, subtotal - subtotalp);

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(colIndex, rowIndex, colIndex + 6, rowIndex),
						BookHelper.BORDER_FULL, BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerPembayarandgnPenguranganWindow.java:340");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Pembayarandgn Pengurangan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});

			}

			currTgl = obj[0].toString();
			rowIndex++;
			index++;
		}

		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, jmls);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, total);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 5, totalp);
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 6, total - totalp);

		try {
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, colIndex + 6, rowIndex), true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerPembayarandgnPenguranganWindow.java:356");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Pembayarandgn Pengurangan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		try {
			Utils.setAlignment(sheet, new Rect(colIndex, 6, colIndex, rowIndex), CellStyle.ALIGN_LEFT);
			Utils.setAlignment(sheet, new Rect(colIndex + 1, 6, colIndex + 1, rowIndex), CellStyle.ALIGN_RIGHT);
			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(colIndex, rowIndex, colIndex + 6, rowIndex),
					BookHelper.BORDER_FULL, BorderStyle.THIN, color);
			Utils.setColumnWidth(sheet, 0, 250);
			Utils.setColumnWidth(sheet, 1, 150);
			Utils.setColumnWidth(sheet, 2, 100);
			Utils.setColumnWidth(sheet, 3, 150);
			Utils.setColumnWidth(sheet, 4, 150);
			Utils.setColumnWidth(sheet, 5, 150);
			Utils.setColumnWidth(sheet, 6, 150);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerPembayarandgnPenguranganWindow.java:371");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Pembayarandgn Pengurangan Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
