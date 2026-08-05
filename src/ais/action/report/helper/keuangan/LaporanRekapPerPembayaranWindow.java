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
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
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
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanRekapPerPembayaranWindow extends MyWindow {

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

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public LaporanRekapPerPembayaranWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Per Pembayaran Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapPerPembayaranWindow(String title, String border, boolean closable) {
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

		MyTabConfig tab1 = new MyTabConfig("Rekap Berdasarkan Tanggal Validasi");
		tab1.setParent(tabs);

		MyTabConfig tab2 = new MyTabConfig("Rekap Berdasarkan Tanggal Pembayaran (Cicilan)");
		tab2.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
		tabpanel2.setParent(tabpanels);
		tab2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanel2.getChildren().size() == 0) {
					LaporanRekapPerPembayaranCicilWindow laporanKHS = new LaporanRekapPerPembayaranCicilWindow();
					laporanKHS.setHeight("100%");
					laporanKHS.setWidth("100%");
					laporanKHS.setParent(tabpanel2);
				}
			}
		});

		jenisPembayaran = Common.createComboJenisPembayaran(jenisPembayaran);

		setClosable(true);
		setTitle("Rekap Per Pembayaran");
		setWidth("800px");
		setHeight("90%");
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

		try {
			initSpreadsheet();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Per Pembayaran Window", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

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
		String sql = "select  to_char(a.tanggal, 'DD-MM-YYYY') as formated_tanggal,  a.amount,  "
				+ "count(a.id) as jumlah,  sum(a.amount) as total_amount  from kegiatan a  "
				+ "left join mahasiswa b on (a.mahasiswa = b.id)  "
				+ "left join biodata_calon_mahasiswa bb on (a.calon_mahasiswa = bb.id)  "
				+ "left join jurusan c on (b.jurusan = c.id or bb.prodi_lulus = c.id)  where  a.aktif and c.fakultas = "
				+ (fakultas == null ? "c.fakultas" : fakultas.getId()) + " and a.tahun_akademik = '"
				+ tahunAkademik.trim() + "' and a.semster % 2 " + (semester.equals(Perkuliahan.GENAP) ? "=" : "!=")
				+ " 0  and "
				+ (mulaiTanggal.getValue() == null ? ""
						: " a.tanggal >= '" + dateFormat.format(mulaiTanggal.getValue()) + " 00:00:00'")
				+ "  and "
				+ (sampaiTanggal.getValue() == null ? ""
						: " a.tanggal <= '" + dateFormat.format(sampaiTanggal.getValue()) + " 23:59:59'")
				+ " and a.jenis_kegiatan = " + jenisKegiatan.getId() + " "
				+ "group by to_char(a.tanggal, 'DD-MM-YYYY'),a.amount  order by max(a.tanggal),a.amount";

		System.out.println("sql => " + sql);

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
		spreadsheet.setMaxcolumns(4);
		spreadsheet.setMaxrows(objs.size() + jumlahBedaTanggal + 4);

		Worksheet sheet = spreadsheet.getSelectedSheet();
		sheet.setDefaultColumnWidth(30);
		ais.ui.util.EcampusUtil.setBold(sheet,
				new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

		ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0,
				"REKAPITULASI KEUANGAN PER PEMBAYARAN\n " + Common.getBahasaConfig("Fakultas") + " "
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
		Utils.setRowHeight(sheet, 2, 50);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				true);

		rowIndex = 3;
		colIndex = 0;
		Double total = 0.0;
		Integer jmls = 0;

		currTgl = "";
		int index = 0;
		for (Object[] obj : objs) {
			if (currTgl.equals(obj[0].toString())) {
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, "");
			} else {
				if (index != 0) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, jmls);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, total);
					try {
						ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, colIndex + 3, rowIndex),
								true);
						ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(colIndex, rowIndex, colIndex + 3, rowIndex),
								BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerPembayaranWindow.java:339");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Pembayaran Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});

					}
					rowIndex++;
					rowIndex++;
					total = 0.0;
					jmls = 0;
				}
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, obj[0]);
			}
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, obj[1]);
			Integer jml = Integer.parseInt(obj[2].toString());
			Double subtotal = Double.parseDouble(obj[3].toString());
			total += subtotal;
			jmls += jml;
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, jml);
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, subtotal);

			try {
				ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(colIndex, rowIndex, colIndex + 3, rowIndex),
						BookHelper.BORDER_FULL, BorderStyle.THIN, color);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerPembayaranWindow.java:360");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Pembayaran Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
		try {
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(colIndex, rowIndex, colIndex + 3, rowIndex), true);

			Utils.setAlignment(sheet, new Rect(colIndex, 3, colIndex, rowIndex), CellStyle.ALIGN_LEFT);
			Utils.setAlignment(sheet, new Rect(colIndex + 1, 3, colIndex + 1, rowIndex), CellStyle.ALIGN_RIGHT);
			ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(colIndex, rowIndex, colIndex + 3, rowIndex),
					BookHelper.BORDER_FULL, BorderStyle.THIN, color);
			Utils.setColumnWidth(sheet, 0, 250);
			Utils.setColumnWidth(sheet, 1, 150);
			Utils.setColumnWidth(sheet, 2, 100);
			Utils.setColumnWidth(sheet, 3, 150);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/keuangan/LaporanRekapPerPembayaranWindow.java:382");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Per Pembayaran Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
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
