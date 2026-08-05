package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanQrCodeAbsensiPegawai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private MyDatebox mulai;
	private MyDatebox sampai;

	private Center center;

	private Toolbar toolbar;
	private String location = "";
	// private Combobox tahun;

	private Date date = null;

	@SuppressWarnings("rawtypes")
	private List maps = null;

	private MyCheckboxConfig[] haris;

	private Desktop desktop;

	public LaporanQrCodeAbsensiPegawai() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Qr Code Absensi Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanQrCodeAbsensiPegawai(Date date) {
		super();
		this.date = date;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Qr Code Absensi Pegawai", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanQrCodeAbsensiPegawai(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {
		location = Common.ambilREAL_PATH_REPORT();
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
		if (date != null) {
			calendarUtama.setTime(date);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
			row.appendChild(mulai = new MyDatebox(calendarUtama.getTime()));
			mulai.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
			row.appendChild(sampai = new MyDatebox(WaktuUtil.getDate()));
			sampai.setReadonly(true);
		} else {
			int tanggalMulaiAbsensi = 1;
			try {
				tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanQrCodeAbsensiPegawai.java:136");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Qr Code Absensi Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
			calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) - 1);
			calendarUtama.set(Calendar.DATE, tanggalMulaiAbsensi);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
			row.appendChild(mulai = new MyDatebox(calendarUtama.getTime()));
			mulai.setReadonly(true);

			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) + 1);
			calendarUtama.set(Calendar.DATE, calendarUtama.get(Calendar.DATE) - 1);
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
			row.appendChild(sampai = new MyDatebox(calendarUtama.getTime()));
			sampai.setReadonly(true);
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari Aktif"));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		String hariDefaultTidakAktif = Common.getKonfigurasi("hari_default_tidak_aktif", ",1,7,").getNilai();

		haris = new MyCheckboxConfig[Common.haris.length];
		int hari = 1;
		for (String h : Common.haris) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(haris[hari - 1] = new MyCheckboxConfig(h));
			haris[hari - 1].setChecked(!hariDefaultTidakAktif.contains("," + hari + ","));
			haris[hari - 1].setValue(h);
			haris[hari - 1].setAttribute("hari", hari);
			hari++;
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);
			}
		});
		print.setParent(row);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Laporan_QR_Code_Per_Hari", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRand();
		if (maps != null) {
			parameters.put("maps", maps);
		}
		parameters.put("mulai", Common.dateFormat1.get().format(mulai.getValue()));
		parameters.put("sampai", Common.dateFormat1.get().format(sampai.getValue()));

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generateDataDanImageAlbum(final Label label) throws Exception {

		final Date dateMulai = mulai.getValue();
		final Date dateSampai = sampai.getValue();

		// 1. PRA-PENGUMPULAN DATA (Menyaring hanya hari yang diceklis)
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(dateMulai);
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calendar);

		Calendar s = ais.ui.util.WaktuUtil.getCalendar();
		s.setTime(dateSampai);
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(s);
		s.add(Calendar.DATE, 1);

		final List<Date> validDates = new ArrayList<Date>();
		while (calendar.getTime().before(s.getTime())) {
			Date tanggal = calendar.getTime();
			Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

			// Memajukan kalender sebelum evaluasi
			calendar.add(Calendar.DATE, 1);

			if (!haris[hari - 1].isChecked()) {
				continue;
			}
			validDates.add(tanggal);
		}

		final int size = validDates.size();
		if (size == 0) {
			this.maps = new ArrayList();
			if (label != null)
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			return;
		}

		// 2. PERSIAPAN MULTI-THREADING DAN PRESERVASI URUTAN DATA (ORDERING)
		final Map[] orderedMaps = new Map[size];
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			listIndex.add(i);
		}

		final java.util.concurrent.atomic.AtomicInteger progressCounter = new java.util.concurrent.atomic.AtomicInteger(
				0);

		// 3. EKSEKUSI PEMBUATAN QR-CODE SECARA PARALEL (Max 100 Thread)
		ParallelTaskExecutor.process(listIndex, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Integer>() {
			@Override
			public void execute(final Integer idx) throws Exception {
				try {
					Date tanggal = validDates.get(idx);

					// Pembaruan Progress UI yang Aman dari Macet/Freeze
					int currentCount = progressCounter.incrementAndGet();
					if (label != null && desktop != null) {
						if (currentCount % 5 == 0 || currentCount == size) { // Update tiap 5 iterasi
							try {
								org.zkoss.zk.ui.Executions.activate(desktop);
								try {
									String strTgl;
									strTgl = Common.dateFormat6.get().format(tanggal);

									label.setValue("Memproses QR-Code tanggal " + strTgl + " ("
											+ Common.numberFormat.get().format((currentCount * 100.0) / size) + "%)");
								} finally {
									org.zkoss.zk.ui.Executions.deactivate(desktop);
								}
							} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanQrCodeAbsensiPegawai.java:300");
								// Abaikan jika interruptedException
							}
						}
					}

					String tglFormat8;
					String tglFormat6;
					String tglFormat1;
					String encrypted;

					tglFormat8 = Common.dateFormat8.get().format(tanggal);
					tglFormat6 = Common.dateFormat6.get().format(tanggal);
					tglFormat1 = Common.dateFormat1.get().format(tanggal);
					encrypted = Common.desEncrypter.get().encrypt(tglFormat8);

					String code = "P-" + encrypted;
					File myfilebarcode1 = new File(
							location + "/crcode_" + java.net.URLEncoder.encode(code, "UTF-8") + ".png");

					// Proses rendering gambar QR Code (Berjalan murni secara paralel)
					BarcodeCommon.generateCRCode(code, myfilebarcode1);

					Map map = new java.util.HashMap();
					map.put("qr_code", myfilebarcode1.getAbsolutePath());
					map.put("hari", tglFormat6);
					map.put("tanggal", tglFormat1);

					// MENGAMANKAN URUTAN (QR Code tetap urut sesuai tanggal dari Awal ke Akhir)
					orderedMaps[idx] = map;

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Qr Code Absensi Pegawai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

		// 4. MEMASUKKAN KEMBALI HASIL KE LIST GLOBAL
		this.maps = new ArrayList();
		for (Map m : orderedMaps) {
			if (m != null) {
				this.maps.add(m);
			}
		}

		if (label != null) {
			ais.action.report.helper.LoadingReportUtil.selesai(label);
		}
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Map parameters = generateParameter();
				parameters.put("maps", maps);
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Laporan_QR_Code_Per_Hari",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		// [ZK 5.5] Setup Server Push untuk Update Label Progress
		desktop = org.zkoss.zk.ui.Executions.getCurrent().getDesktop();

		if (desktop != null && !desktop.isServerPushEnabled()) {
			desktop.enableServerPush(true);
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanQrCodeAbsensiPegawai.java:380");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Qr Code Absensi Pegawai", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}).start();

	}

}
