package ais.action.report.format1.payroll;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonPayroll;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CutiBersama;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.JenisPengajuanPegawai;
import ais.database.model.KelompokParameterTambahanPengajuanPegawai;
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPengajuanPegawai;
import ais.database.model.Pegawai;
import ais.database.model.PengajuanPegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.rab.SatuanKerja;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Layar laporan payroll "Laporan Presensi/Absensi Pegawai Per Orang": menghasilkan PDF rekap
 * kehadiran harian setiap pegawai terpilih pada rentang tanggal tertentu (satu baris per hari
 * per pegawai), termasuk pengajuan cuti/izin yang menimpa hari tersebut. Kelas ini adalah window
 * ZK mandiri di atas {@link MyWindow}, dengan panel filter di sisi barat (fakultas/satuan kerja,
 * pilihan hari aktif dalam seminggu, jenis pegawai — dosen/guru/pegawai umum, ikatan dinas
 * dosen, rentang tanggal) dan area pratinjau PDF di tengah.
 *
 * <p>
 * Perhitungan data ({@link #generateDataDanImageAlbum(ProgressContext)}) adalah inti kelas ini:
 * untuk setiap pegawai yang termasuk cakupan presensi ({@link #pegawaiMasukPresensi(Pegawai)} —
 * dikecualikan bila {@code TipePegawai.masukPresensi} bernilai {@code false}), setiap hari dalam
 * rentang tanggal diperiksa terhadap {@link PengajuanPegawai} (cuti/izin/dinas) yang berlaku pada
 * hari tersebut, memakai peta parameter tambahan per {@link JenisPengajuanPegawai} yang di-cache
 * agar tidak query berulang. Proses per pegawai dijalankan paralel lewat
 * {@link ParallelTaskExecutor} (maksimum thread dari
 * {@code ParallelTaskExecutor#getDefaultReportMaxThreads()}), dengan hasil dikumpulkan ke
 * {@link java.util.Map} tersinkronisasi dan progres dilaporkan ke {@link ProgressContext} secara
 * live lewat server push ZK ({@code AsyncTaskManager#jalankanDenganPush}) agar UI tidak
 * terblokir selama query berat berjalan. {@link #onKHS(Event)} adalah pemicu utama: menampilkan
 * kartu progress, memproses data, lalu membuat dan menampilkan PDF akhir — dijaga anti
 * tumpang-tindih lewat flag {@link #sedangMemprosesLaporan}.
 * </p>
 *
 * <p>
 * Kelompok method privat pendek di bagian bawah kelas (mis. {@link #isCutiDisetujui},
 * {@link #isStatus}, {@link #awalHari}, {@link #hitungJumlahHariInklusif}) adalah helper
 * null-safe kecil yang dipakai berulang selama perhitungan data harian, menormalkan nilai
 * cuti/status/tanggal agar logika utama tidak dipenuhi pengecekan {@code null} berulang.
 * </p>
 */
public class LaporanAbsensiPegawaiPerOrang extends MyWindow {

	private static final long serialVersionUID = -397946194166101691L;

	private static final String REPORT_NAME = "LPresensi1";

	private static final String STYLE_INFO_CARD = "background:#ffffff;border:1px solid #e5e7eb;"
			+ "border-radius:14px;padding:18px 20px;margin:14px;"
			+ "box-shadow:0 8px 24px rgba(15,23,42,0.08);color:#111827;";

	private static final String STYLE_PROGRESS_CARD = "background:#ffffff;border:1px solid #dbeafe;"
			+ "border-radius:16px;padding:22px 24px;margin:18px;"
			+ "box-shadow:0 12px 34px rgba(37,99,235,0.14);color:#111827;";

	private AmbilDataPegawaiBanbox searchparent;
	private MyCheckboxConfig[] haris;

	private Center center;

	private Toolbar toolbar;

	private Pegawai pegawai;

	private AmbilDataSatuanKerjaBanbox searchSatker;

	private MyCheckboxConfig hanyaDosen;

	private MyCheckboxConfig hanyaPegawai;

	private MyCheckboxConfig hanyaGuru;

	private MyCheckboxConfig abaikanKehadiranJikaHariTidakTerpilih;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Combobox ikatanDinasDosen;

	private MyDatebox mulai;

	private MyDatebox sampai;

	@SuppressWarnings("rawtypes")
	private ArrayList maps;

	@SuppressWarnings("rawtypes")
	private Map parameters;

	private Desktop desktop;

	private volatile boolean sedangMemprosesLaporan;

	/** Kumpulan komponen ZK dan status yang membentuk kartu progress selama laporan diproses (dibangun oleh {@link #tampilkanProgressBar()}, diperbarui oleh {@link #updateProgress}). */
	private static class ProgressContext {
		private Vbox box;
		private Progressmeter progressmeter;
		private Label judul;
		private Label detail;
		private volatile boolean selesai;
		private volatile boolean gagal;
	}

	/** Membangun window laporan dalam konfigurasi baku, tanpa pegawai terikat. */
	public LaporanAbsensiPegawaiPerOrang() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai Per Orang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/** Membangun window laporan terikat pada satu {@code pegawai} tertentu. */
	public LaporanAbsensiPegawaiPerOrang(Pegawai pegawai) {
		super();
		this.pegawai = pegawai;
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai Per Orang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	/** Membangun window laporan dengan judul, tipe border, dan status closable yang dapat diatur eksplisit. */
	public LaporanAbsensiPegawaiPerOrang(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	/** Menyiapkan model hierarki satuan kerja yang dipakai filter satuan kerja. */
	private void initKHS() throws Exception {
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
	}

	/** Membangun tata letak window: panel filter di barat (satuan kerja, hari aktif, jenis pegawai, rentang tanggal, tombol proses) dan area pratinjau PDF beserta toolbar ekspor di tengah. */
	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		MyGrid grid = new MyGrid();
		Rows rows = new Rows();
		rows.setParent(grid);

		West west = new West();
		west.setTitle("Filter Laporan");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("360px");
		west.setStyle("background:#f8fafc;border-right:1px solid #e5e7eb;");

		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("background:#f8fafc;border:0;padding:8px;");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(pegawai == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Satuan Kerja")));
		row.appendChild(searchSatker = new AmbilDataSatuanKerjaBanbox());
		searchSatker.setWidth("90%");
		searchSatker.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(pegawai == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Pegawai")));
		row.appendChild(searchparent = new AmbilDataPegawaiBanbox(true));
		searchparent.setWidth("90%");
		searchparent.setReadonly(true);

		if (pegawai == null) {
			Common.initKeterangan(rows, "Kosongkan data pegawai untuk mencetak data semua pegawai");
		}

		boolean tampilanPilihanHanyaDosenDanGuruSaja = Common.bolehKonfigurasi("tampilan_pilihan_hanya_dosen_dan_guru_saja");

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaDosen = new MyCheckboxConfig("Hanya dosen saja"));

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaGuru = new MyCheckboxConfig("Hanya guru saja"));

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaPegawai = new MyCheckboxConfig("Hanya pegawai, bukan dosen dan guru"));

		row = new MyFormRow();
		row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ikatan Kerja"));
		row.appendChild(ikatanDinasDosen = new Combobox());
		Common.insertComboDanSemua(ikatanDinasDosen, "nama", IkatanKerjaDosen.class, Restrictions.eq("aktif", true));

		int tanggalMulaiAbsensi = 1;
		try {
			tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiPerOrang.java:241");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Absensi Pegawai Per Orang", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
		calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) - 1);
		calendarUtama.set(Calendar.DATE, tanggalMulaiAbsensi);

		row = new MyFormRow();
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

		row = new MyFormRow();
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

		abaikanKehadiranJikaHariTidakTerpilih = ais.action.master.helper.KehadiranPresensiUtil
				.buatCheckboxAbaikanKehadiranHariTidakTerpilih(rows);

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
		center.setStyle("background:#f1f5f9;overflow:auto;");
		tampilkanInfoAwal();

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, REPORT_NAME, null, new EventListener() { 

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}


	/** Menampilkan kartu informasi awal (petunjuk penggunaan) di panel tengah sebelum laporan pernah dijalankan. */
	private void tampilkanInfoAwal() {
		if (center == null) {
			return;
		}
		center.getChildren().clear();
		Vbox box = new Vbox();
		box.setWidth("96%");
		box.setStyle(STYLE_INFO_CARD);
		box.setParent(center);

		Html html = new Html();
		html.setContent("<div style='font-family:Arial,sans-serif;'>"
				+ "<div style='font-size:18px;font-weight:bold;margin-bottom:8px;color:#0f172a;'>Laporan Presensi dan Absensi Kehadiran</div>"
				+ "<div style='font-size:13px;line-height:1.7;color:#475569;'>"
				+ "Pilih pegawai, satuan kerja, rentang tanggal, dan hari aktif. "
				+ "Klik <b>Tampilkan</b> untuk memproses data kehadiran, cuti, izin, tugas, dan ringkasan keterlambatan. "
				+ "Tanggal akhir laporan dihitung tepat sampai tanggal <b>Sampai</b>, sehingga tidak menambah satu hari lagi."
				+ "</div>"
				+ "<div style='margin-top:14px;display:flex;gap:10px;flex-wrap:wrap;'>"
				+ "<span style='background:#dbeafe;color:#1d4ed8;padding:7px 11px;border-radius:999px;font-size:12px;'>Progress loading jelas</span>"
				+ "<span style='background:#dcfce7;color:#166534;padding:7px 11px;border-radius:999px;font-size:12px;'>Query data diprefetch</span>"
				+ "<span style='background:#fef3c7;color:#92400e;padding:7px 11px;border-radius:999px;font-size:12px;'>Aman untuk rentang tanggal inklusif</span>"
				+ "</div>"
				+ "</div>");
		html.setParent(box);
	}

	private ProgressContext tampilkanProgressBar() {
		ProgressContext progress = new ProgressContext();
		center.getChildren().clear();

		Vbox box = new Vbox();
		box.setWidth("96%");
		box.setStyle(STYLE_PROGRESS_CARD);
		box.setParent(center);
		progress.box = box;

		Html header = new Html();
		header.setContent("<div style='font-family:Arial,sans-serif;'>"
				+ "<div style='font-size:18px;font-weight:bold;color:#0f172a;margin-bottom:6px;'>Sedang menyiapkan laporan</div>"
				+ "<div style='font-size:13px;color:#64748b;line-height:1.6;'>"
				+ "Data presensi, cuti, izin, tugas, dan rekap pegawai sedang dihitung. "
				+ "Tunggu sampai proses selesai, indikator akan hilang otomatis."
				+ "</div></div>");
		header.setParent(box);

		Label judul = new Label(ais.common.Common.getBahasaConfig("Memulai proses..."));
		judul.setStyle("font-size:13px;font-weight:bold;color:#1d4ed8;margin-top:14px;");
		judul.setParent(box);
		progress.judul = judul;

		Progressmeter progressmeter = new Progressmeter();
		progressmeter.setWidth("100%");
		progressmeter.setHeight("18px");
		progressmeter.setValue(0);
		progressmeter.setStyle("margin-top:8px;border-radius:999px;");
		progressmeter.setParent(box);
		progress.progressmeter = progressmeter;

		Label detail = new Label(ais.common.Common.getBahasaConfig("0% - menunggu data diproses"));
		detail.setStyle("font-size:12px;color:#475569;margin-top:8px;");
		detail.setParent(box);
		progress.detail = detail;

		return progress;
	}

	private void updateProgress(final ProgressContext progress, final int persen, final String judul,
			final String detail) {
		if (progress == null || progress.selesai || desktop == null) {
			return;
		}
		try {
			Executions.activate(desktop);
			try {
				int nilai = persen;
				if (nilai < 0) {
					nilai = 0;
				}
				if (nilai > 100) {
					nilai = 100;
				}
				if (progress.progressmeter != null) {
					progress.progressmeter.setValue(nilai);
				}
				if (progress.judul != null) {
					progress.judul.setValue(judul == null ? "Memproses data..." : judul);
				}
				if (progress.detail != null) {
					progress.detail.setValue(nilai + "% - " + (detail == null ? "" : detail));
				}
			} finally {
				Executions.deactivate(desktop);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiPerOrang.java:419");
			// Desktop dapat tidak aktif saat user menutup halaman. Proses data tetap boleh lanjut.
		}
	}

	private void sembunyikanProgressDanTampilkanPdf(final ProgressContext progress, final File file) {
		if (progress != null) {
			progress.selesai = true;
		}
		if (desktop == null) {
			return;
		}
		try {
			Executions.activate(desktop);
			try {
				if (center != null) {
					center.getChildren().clear();
				}
				if (file == null || !file.exists()) {
					tampilkanPesanSelesaiTanpaFile();
				} else {
					CommonReport.tampilkanReportPDF(center, file);
				}
			} finally {
				Executions.deactivate(desktop);
			}
		} catch (Exception e) {
			if (progress != null) {
				progress.selesai = false;
			}
			tampilkanErrorProgress(progress, e);
		}
	}

	private void tampilkanPesanSelesaiTanpaFile() {
		if (center == null) {
			return;
		}
		center.getChildren().clear();
		Vbox box = new Vbox();
		box.setWidth("96%");
		box.setStyle(STYLE_INFO_CARD);
		box.setParent(center);

		Html html = new Html();
		html.setContent("<div style='font-family:Arial,sans-serif;'>"
				+ "<div style='font-size:18px;font-weight:bold;margin-bottom:8px;color:#0f172a;'>Data selesai diproses</div>"
				+ "<div style='font-size:13px;line-height:1.7;color:#475569;'>"
				+ "Data laporan sudah selesai dihitung, tetapi file PDF tidak ditemukan atau gagal dibuat. "
				+ "Silakan ulangi proses atau periksa konfigurasi Jasper/report di server."
				+ "</div></div>");
		html.setParent(box);
	}

	private void pastikanProgressBerhenti(final ProgressContext progress, final String pesan) {
		if (progress == null || progress.selesai || desktop == null) {
			return;
		}
		try {
			Executions.activate(desktop);
			try {
				progress.selesai = true;
				if (progress.progressmeter != null) {
					progress.progressmeter.setValue(100);
				}
				if (progress.judul != null) {
					progress.judul.setValue("Proses selesai");
				}
				if (progress.detail != null) {
					progress.detail.setValue("100% - " + (pesan == null ? "Proses laporan telah selesai." : pesan));
				}
			} finally {
				Executions.deactivate(desktop);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiPerOrang.java:493");
			// Abaikan; biasanya terjadi jika halaman sudah ditutup oleh user.
		}
	}

	private void tampilkanErrorProgress(final ProgressContext progress, final Exception error) {
		if (progress != null) {
			progress.gagal = true;
		}
		if (desktop == null) {
			return;
		}
		try {
			Executions.activate(desktop);
			try {
				if (progress != null) {
					if (progress.progressmeter != null) {
						progress.progressmeter.setValue(100);
					}
					if (progress.judul != null) {
						progress.judul.setValue("Proses laporan gagal");
						progress.judul.setStyle("font-size:13px;font-weight:bold;color:#b91c1c;margin-top:14px;");
					}
					if (progress.detail != null) {
						progress.detail.setValue(error == null ? "Terjadi kesalahan saat memproses laporan."
								: error.getMessage());
						progress.detail.setStyle("font-size:12px;color:#991b1b;margin-top:8px;");
					}
				}
				Common.tampilErrorJikaAdmin(error);
			} finally {
				Executions.deactivate(desktop);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiPerOrang.java:526");
			// Abaikan error UI tambahan agar tidak menutup informasi error utama.
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map buatParameterBaru() {
		Map map = ais.common.HashMapGenerator.getRand();
		if (map == null) {
			map = new HashMap();
		}
		return Collections.synchronizedMap(map);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void putParameter(String key, Object value) {
		if (key == null) {
			return;
		}
		if (parameters == null) {
			parameters = buatParameterBaru();
		}
		parameters.put(key, value == null ? "" : value);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void putAllParameter(Map values) {
		if (values == null || values.isEmpty()) {
			return;
		}
		if (parameters == null) {
			parameters = buatParameterBaru();
		}
		parameters.putAll(values);
	}

	private static boolean pegawaiMasukPresensi(Pegawai pegawai) {
		return pegawai == null || pegawai.getTipePegawai() == null || pegawai.getTipePegawai().getMasukPresensi() == null
				|| Boolean.TRUE.equals(pegawai.getTipePegawai().getMasukPresensi());
	}

	private static boolean isCutiDisetujui(CutiDanIzin cutiDanIzin) {
		return cutiDanIzin != null && Boolean.TRUE.equals(cutiDanIzin.getSetujui());
	}

	private static boolean isMemotongJatahCuti(CutiDanIzin cutiDanIzin) {
		return cutiDanIzin != null && Boolean.TRUE.equals(cutiDanIzin.getMemotongJatahCuti());
	}

	private static boolean isStatus(Statusabsensi statusabsensi, Long id) {
		return statusabsensi != null && statusabsensi.getId() != null && statusabsensi.getId().equals(id);
	}

	private static int intValue(Integer value) {
		return value == null ? 0 : value.intValue();
	}

	private static double doubleValue(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}

	private static String stringValue(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Map parseParameterTambahan(String rawParams) {
		Map hasil = new HashMap();
		if (rawParams == null || rawParams.trim().length() == 0) {
			return hasil;
		}
		String[] spl = rawParams.split("\n");
		for (int i = 0; i < spl.length; i++) {
			String baris = spl[i];
			if (baris == null || baris.trim().length() == 0) {
				continue;
			}
			String[] value = baris.split("<=>");
			if (value.length > 0 && value[0] != null) {
				hasil.put(value[0].trim().toLowerCase(), value.length > 1 && value[1] != null ? value[1].trim() : "");
			}
		}
		return hasil;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (parameters == null) {
			parameters = buatParameterBaru();
		}

		int i = 0;
		for (MyCheckboxConfig checkbox : haris) {
			Integer hari = (Integer) checkbox.getAttribute("hari");
			putParameter("hari" + i, checkbox.isChecked() ? hari : -1);
			i++;
		}
		if (maps != null) {
			putParameter("maps", maps);
		}

		Date tanggalMulai = awalHari(mulai.getValue());
		Date tanggalSampai = awalHari(sampai.getValue());
		putParameter("mulai", Common.dateFormat1.get().format(tanggalMulai));
		putParameter("sampai", Common.dateFormat1.get().format(tanggalSampai));

		return parameters;
	}

	private static Date awalHari(Date date) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(date == null ? ais.ui.util.WaktuUtil.getDate() : date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	private static Date akhirHari(Date date) {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(awalHari(date));
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	private static int hitungJumlahHariInklusif(Date mulai, Date sampai) {
		Calendar cMulai = ais.ui.util.WaktuUtil.getCalendar();
		cMulai.setTime(awalHari(mulai));
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(cMulai);

		Calendar cSampai = ais.ui.util.WaktuUtil.getCalendar();
		cSampai.setTime(awalHari(sampai));
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(cSampai);

		if (cMulai.getTime().after(cSampai.getTime())) {
			return 0;
		}

		int jumlah = 0;
		while (!cMulai.getTime().after(cSampai.getTime())) {
			jumlah++;
			cMulai.add(Calendar.DATE, 1);
		}
		return jumlah;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void isiParameterTanggalGlobal(Date mulai, Date sampai) {
		if (parameters == null) {
			parameters = buatParameterBaru();
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(awalHari(mulai));
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calendar);

		Calendar batas = ais.ui.util.WaktuUtil.getCalendar();
		batas.setTime(awalHari(sampai));
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(batas);
		batas.add(Calendar.DATE, 1);

		int index = 1;
		while (calendar.getTime().before(batas.getTime())) {
			putParameter("tanggal_" + index, calendar.getTime());
			calendar.add(Calendar.DATE, 1);
			index++;
		}
		putParameter("jumlah_hari", Integer.valueOf(index - 1));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generateDataDanImageAlbum(final ProgressContext progress) throws Exception {

		Session session = null;
		final Map finalMapPerPegawai = Collections.synchronizedMap(new HashMap());

		try {
			session = ais.action.report.Report.openNativeSession();

			// 1. FILTER SATUAN KERJA
			SatuanKerja parent = (SatuanKerja) searchSatker.getAttribute("satuanKerja");
			Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
			if (parent != null && pegawai == null) {
				satuanKerjas.clear();
				satuanKerjas.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
			}

			final Date[] rangeTanggal = ais.action.master.helper.KehadiranPresensiUtil.normalisasiRentangTanggal(mulai.getValue(), sampai.getValue());
			final Date dateMulaiQuery = awalHari(rangeTanggal[0]);
			final Date dateSampaiQuery = akhirHari(rangeTanggal[1]);

			// Khusus untuk iterasi kolom/tanggal laporan, gunakan batas tanggal murni 00:00.
			// Jika memakai dateSampaiQuery yang sudah 23:59:59 lalu ditambah 1 hari,
			// tanggal setelah "Sampai" ikut terproses. Contoh 16-05-2026 s/d 15-06-2026
			// menjadi ikut menampilkan 16-06-2026. Inilah sumber selisih +1 hari.
			final Date dateMulai = awalHari(rangeTanggal[0]);
			final Date dateSampai = awalHari(rangeTanggal[1]);

			if (dateMulai.after(dateSampai)) {
				throw new IllegalArgumentException("Tanggal mulai tidak boleh lebih besar dari tanggal sampai.");
			}

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(dateMulai);
			ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calendar);
			final Integer selectedtahun = calendar.get(Calendar.YEAR);

			// 2. FETCH PEGAWAI SECARA OPTIMAL
			Pegawai peg = pegawai == null ? (Pegawai) searchparent.getAttribute("pegawai") : pegawai;
			Criteria critPeg = session.createCriteria(Pegawai.class)
					.add(Restrictions.eq("statusPegawai", ConstantValues.AKTIF_PEGAWAI))
					.createAlias("tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.isNull("tipePegawai.masukPresensi"),
							Restrictions.eq("tipePegawai.masukPresensi", true)))
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

			if (peg != null) {
				critPeg.add(Restrictions.or(Restrictions.eq("id", peg.getId()),
						Restrictions.or(
								peg.getGuru() == null ? Restrictions.sqlRestriction("false")
										: Restrictions.eq("guru.id", peg.getGuru().getId()),
								peg.getDosen() == null ? Restrictions.sqlRestriction("false")
										: Restrictions.eq("dosen.id", peg.getDosen().getId()))));
			} else {
				if (!satuanKerjas.isEmpty()) {
					critPeg.add(Restrictions.in("satuanKerja", satuanKerjas));
				}
				if (hanyaDosen.isChecked())
					critPeg.add(Restrictions.isNotNull("dosen"));
				if (hanyaGuru.isChecked())
					critPeg.add(Restrictions.isNotNull("guru"));
				if (hanyaPegawai.isChecked()) {
					critPeg.add(Restrictions.and(Restrictions.isNull("guru"), Restrictions.isNull("dosen")));
				}
				if (ikatanDinasDosen.getSelectedItem() != null
						&& ikatanDinasDosen.getSelectedItem().getValue() != null) {
					critPeg.add(Restrictions.eq("ikatanKerjaDosen", ikatanDinasDosen.getSelectedItem().getValue()));
				}
			}

			final List<Pegawai> pegawais = ConstantValues.simpleList(critPeg.addOrder(Order.asc("satuanKerja"))
					.addOrder(Order.asc("dosen")).addOrder(Order.asc("guru")).addOrder(Order.asc("nama")),
					Pegawai.class);

			if (pegawais.isEmpty()) {
				this.maps = new ArrayList();
				return;
			}

			// 3. PRE-FETCH DATA PENDUKUNG (Menghindari N+1 Query di dalam Loop)
			CutiBersama cb = (CutiBersama) session.createCriteria(CutiBersama.class)
					.add(Restrictions.eq("tahun", selectedtahun)).setMaxResults(1).uniqueResult();
			final CutiBersama cutiBersama = (cb != null) ? cb : new CutiBersama();

			final List<CutiDanIzin> cutiDanIzinsSemua = session.createCriteria(CutiDanIzin.class)
					// Overlap tanggal: mulai <= akhir laporan DAN sampai >= awal laporan.
					// Ini lebih aman dibanding hanya between, karena cuti yang dimulai sebelum periode
					// dan selesai setelah periode tetap ikut terbaca.
					.add(Restrictions.le("mulai", dateSampaiQuery))
					.add(Restrictions.ge("sampai", dateMulaiQuery))
					.addOrder(Order.asc("mulai")).add(Restrictions.in("pegawai", pegawais))
					.add(Restrictions.eq("setujui", true)).list();

			final Map<Long, List<CutiDanIzin>> mapCutiPerPegawai = new HashMap<Long, List<CutiDanIzin>>();
			for (CutiDanIzin c : cutiDanIzinsSemua) {
				Long pId = c.getPegawai().getId();
				List<CutiDanIzin> listC = mapCutiPerPegawai.get(pId);
				if (listC == null) {
					listC = new ArrayList<CutiDanIzin>();
					mapCutiPerPegawai.put(pId, listC);
				}
				listC.add(c);
			}

			final Map<String, StatuskehadiranKaryawanHarian> statuskehadiranKaryawanHarians = CommonPayroll
					.getDefaultStatuskehadiranKaryawanHarian(cutiDanIzinsSemua, dateMulaiQuery, dateSampaiQuery, pegawais,
							session, true);

			final List<PengajuanPegawai> pengajuanPegawaisSemua = session.createCriteria(PengajuanPegawai.class)
					.add(Restrictions.or(
							Restrictions.sqlRestriction("date('" + Common.databaseDateFormat.get().format(WaktuUtil.getDate())
									+ "') between date(this_.waktu) and date(this_.waktusampai)"),
							Restrictions.and(Restrictions.le("waktu", dateSampaiQuery),
									Restrictions.ge("waktuSampai", dateMulaiQuery))))
					.addOrder(Order.asc("waktu")).add(Restrictions.in("pegawai", pegawais))
					.add(Restrictions.eq("setujui", true)).list();

			final Map<Long, List<PengajuanPegawai>> mapPengajuanPerPegawai = new HashMap<Long, List<PengajuanPegawai>>();
			for (PengajuanPegawai p : pengajuanPegawaisSemua) {
				Long pId = p.getPegawai().getId();
				List<PengajuanPegawai> listP = mapPengajuanPerPegawai.get(pId);
				if (listP == null) {
					listP = new ArrayList<PengajuanPegawai>();
					mapPengajuanPerPegawai.put(pId, listP);
				}
				listP.add(p);
			}

			// =========================================================================
			// [OPTIMASI 1]: PRE-FETCH DB CACHE UNTUK PARAMETER TAMBAHAN
			// Mencegah pembukaan subSession database di dalam perulangan ParallelTask
			// =========================================================================
			List<JenisPengajuanPegawai> jenisPengajuanAktif = session.createCriteria(JenisPengajuanPegawai.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

			final List<Long> pengajuans = new ArrayList<Long>();
			final Map<Long, Map<String, String>> cacheParamMappers = new HashMap<Long, Map<String, String>>();

			for (JenisPengajuanPegawai j : jenisPengajuanAktif) {
				pengajuans.add(j.getId());
				Map<String, String> paramLabels = new HashMap<String, String>();

				for (KelompokParameterTambahanPengajuanPegawai klp : j
						.getKelompokParameterTambahanPengajuanPegawais()) {
					if (klp.getAktif() == null || Boolean.TRUE.equals(klp.getAktif())) {
						List<ParameterTambahanPengajuanPegawai> ptpps = session
								.createCriteria(ParameterTambahanPengajuanPegawai.class)
								.add(Restrictions.eq("kelompokParameterTambahanPengajuanPegawai", klp))
								.createAlias("parameterTambahan", "pt")
								.add(Restrictions.eq("pt.aktif", true)).list();

						for (ParameterTambahanPengajuanPegawai ptpp : ptpps) {
							ParameterTambahan pt = ptpp.getParameterTambahan();
							// Key jenis sesuai format mapping data lama
							String keyJenis = klp.getId() + "->" + pt.getId();
							paramLabels.put(keyJenis, pt.getLabelInputan());
						}
					}
				}
				cacheParamMappers.put(j.getId(), paramLabels);
			}

			// 4. HITUNG TOTAL SIZE UNTUK PROGRESS BAR UI
			// Gunakan jumlah hari inklusif dari tanggal murni, bukan dateSampaiQuery 23:59:59.
			final int jumlahHariLaporan = hitungJumlahHariInklusif(dateMulai, dateSampai);
			isiParameterTanggalGlobal(dateMulai, dateSampai);

			int tempSize = 0;
			for (Pegawai p : pegawais) {
				if (!pegawaiMasukPresensi(p)) {
					continue;
				}
				tempSize += jumlahHariLaporan;
			}
			final int size = tempSize <= 0 ? 1 : tempSize;
			final java.util.concurrent.atomic.AtomicInteger currentIndex = new java.util.concurrent.atomic.AtomicInteger(
					0);
			final Date sekarang = WaktuUtil.getDate();

			// 5. PROSES PARALEL MENGGUNAKAN HELPER (MAX 100 THREADS)
			ParallelTaskExecutor.process(pegawais, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Pegawai>() { 
				@Override 
				public void execute(final Pegawai pegawai) throws Exception {

					if (!pegawaiMasukPresensi(pegawai)) {
						return;
					}

					Map map = new java.util.HashMap();
					Map parameterPegawai = new java.util.HashMap();
					Common.insertProperty(Pegawai.class, pegawai, map, "");
					map.put("id", pegawai.getId());

					for (Long p : pengajuans) {
						map.put("pengajuan_" + p, 0);
					}

					Map<String, List<PengajuanPegawai>> tglsTugas = new HashMap<String, List<PengajuanPegawai>>();
					HashMap<Long, Integer> counts = new HashMap<Long, Integer>();

					List<PengajuanPegawai> pengajuanPegawais = mapPengajuanPerPegawai.get(pegawai.getId());
					if (pengajuanPegawais == null)
						pengajuanPegawais = new ArrayList<PengajuanPegawai>();

					for (PengajuanPegawai pengajuanPegawai : pengajuanPegawais) {
						if (pengajuanPegawai == null || pengajuanPegawai.getWaktu() == null) {
							continue;
						}
						Date mulaiPengajuan = awalHari(pengajuanPegawai.getWaktu());
						Date sampaiPengajuan = pengajuanPegawai.getWaktuSampai() == null ? mulaiPengajuan
								: akhirHari(pengajuanPegawai.getWaktuSampai());

						if (mulaiPengajuan.before(dateMulai)) {
							mulaiPengajuan = dateMulai;
						}
						if (sampaiPengajuan.after(dateSampaiQuery)) {
							sampaiPengajuan = dateSampaiQuery;
						}
						if (mulaiPengajuan.after(sampaiPengajuan)) {
							continue;
						}

						Calendar calendarSub = ais.ui.util.WaktuUtil.getCalendar();
						calendarSub.setTime(mulaiPengajuan);
						ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calendarSub);
						int indexD = 0;

						Calendar calSampai = ais.ui.util.WaktuUtil.getCalendar();
						calSampai.setTime(sampaiPengajuan);
						ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calSampai);

						while (!calendarSub.getTime().after(calSampai.getTime())) {
							indexD++;
							if (indexD > 60)
								break;

							try {
								Integer hari = calendarSub.get(Calendar.DAY_OF_WEEK);
								if (!haris[hari - 1].isChecked()) {
									calendarSub.add(Calendar.DATE, 1);
									continue;
								}

								Date valLocal = calendarSub.getTime();
								StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = statuskehadiranKaryawanHarians
										.get(Common.dateFormat83.get().format(valLocal) + "_" + pegawai.getId());

								if (statuskehadiranKaryawanHarian != null) {
									Statusabsensi statusabsensi = statuskehadiranKaryawanHarian.getStatusabsensi();
									if (ConstantValues.kehadiranHarusMulaiDanSampai) {
										if (statuskehadiranKaryawanHarian.getMasukjam() == null
												|| statuskehadiranKaryawanHarian.getPulangJam() == null) {
											statusabsensi = ConstantValues.BELUM_ABSEN;
										}
									}

									CutiDanIzin cutiDanIzin = statuskehadiranKaryawanHarian.getCutiDanIzin();
									if (!isCutiDisetujui(cutiDanIzin)) {
										if (isStatus(statusabsensi, Long.valueOf(1L))) {
											JenisPengajuanPegawai jenisPengajuanPegawai = pengajuanPegawai
													.getJenisPengajuanPegawai();
											if (jenisPengajuanPegawai == null || jenisPengajuanPegawai.getId() == null) {
												calendarSub.add(Calendar.DATE, 1);
												continue;
											}
											Integer countD = counts.get(jenisPengajuanPegawai.getId());
											if (countD == null)
												countD = 0;

											counts.put(jenisPengajuanPegawai.getId(), countD + 1);

											String keyTanggal = Common.dateFormat85.get().format(valLocal);
											List<PengajuanPegawai> pengajuanPegawaisData = tglsTugas.get(keyTanggal);
											if (pengajuanPegawaisData == null) {
												pengajuanPegawaisData = new ArrayList<PengajuanPegawai>();
												tglsTugas.put(keyTanggal, pengajuanPegawaisData);
											}
											pengajuanPegawaisData.add(pengajuanPegawai);
										}
									}
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiPerOrang.java:984");
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Absensi Pegawai Per Orang", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
							}
							calendarSub.add(Calendar.DATE, 1);
						}
					}

					for (Long p : counts.keySet()) {
						map.put("pengajuan_" + p, counts.get(p));
					}

					pegawai.putPhoto(map);

					int jumlahCutiBersama = intValue(cutiBersama.getJumlahCutiBersama());
					int jumlahCuti = pegawai.getJatahCutiTahunan() == null ? intValue(cutiBersama.getJumlahCuti())
							: intValue(pegawai.getJatahCutiTahunan());
					int jumlahCutiYangBisaDiambil = jumlahCuti - jumlahCutiBersama;

					map.put("jumlahCutiTotal", jumlahCuti);
					map.put("jumlahCutiBersama", jumlahCutiBersama);
					map.put("jumlahCutiYangBisaDiambil", jumlahCutiYangBisaDiambil);
					map.put("jumlah_cuti", jumlahCuti);
					map.put("cuti_bersama", jumlahCutiBersama);
					map.put("cuti_bisa_diambil", jumlahCutiYangBisaDiambil);

					List<CutiDanIzin> cutiDanIzins = mapCutiPerPegawai.get(pegawai.getId());
					if (cutiDanIzins == null)
						cutiDanIzins = new ArrayList<CutiDanIzin>();

					LaporanCutiPegawai.generateCutiDanIzinParameter(map, cutiDanIzins, selectedtahun, haris,
							cutiBersama, jumlahCutiYangBisaDiambil);

					ArrayList mapsCuti = new ArrayList();
					int jmlCuti = 0;
					int jmlCutiDanIzin = 0;
					int indexCuti = 1;
					Map<String, Integer> m = new HashMap<String, Integer>();

					for (CutiDanIzin cutiDanIzin : cutiDanIzins) {
						Map myMap = new HashMap();
						myMap.put("status_cuti",
								cutiDanIzin.getStatusabsensi() == null ? "" : cutiDanIzin.getStatusabsensi().getNama());
						myMap.put("disetujui_oleh", cutiDanIzin.getDisetujuiOleh() == null ? ""
								: cutiDanIzin.getDisetujuiOleh().getUserNama());
						myMap.put("disetujui_tgl",
								cutiDanIzin.getSetujuiTanggal() == null ? null : cutiDanIzin.getSetujuiTanggal());
						myMap.put("mulai_cuti", cutiDanIzin.getMulai());
						myMap.put("sampai_cuti", cutiDanIzin.getSampai());
						myMap.put("keterangan_cuti", cutiDanIzin.getKeterangan());
						mapsCuti.add(myMap);

						if (cutiDanIzin.getStatusabsensi() != null) {
							Integer ss = m.get(cutiDanIzin.getStatusabsensi().getNama());
							m.put(cutiDanIzin.getStatusabsensi().getNama(), (ss == null ? 0 : ss) + 1);
						}

						Calendar cCuti = ais.ui.util.WaktuUtil.getCalendar();
						cCuti.setTime(cutiDanIzin.getMulai());

						while (Common.dateFormat8.get().format(cCuti.getTime())
								.equals(Common.dateFormat8.get().format(cutiDanIzin.getSampai()))
								|| cCuti.getTime().before(cutiDanIzin.getSampai())) {

							if (Common.isHolidayMerahDanAtauHariLibur(cCuti.getTime(), pegawai)) {
								cCuti.add(Calendar.DATE, 1);
								continue;
							}

							int tahun = cCuti.get(Calendar.YEAR);
							if (selectedtahun == null || tahun == selectedtahun.intValue()) {
								if (isMemotongJatahCuti(cutiDanIzin)) {
									map.put("tanggal_cuti_" + indexCuti, cCuti.getTime());
									indexCuti++;
									jmlCuti++;
								} else {
									jmlCutiDanIzin++;
								}
							}
							cCuti.add(Calendar.DATE, 1);
						}
					}

					map.put("jmlCutiDanIzin", jmlCutiDanIzin);
					map.put("jumlahCuti", jmlCuti);
					for (String key : m.keySet()) {
						map.put("jumlah_" + key, m.get(key));
					}
					parameterPegawai.put("mapsCuti_" + pegawai.getId(), mapsCuti);

					Calendar cDaily = ais.ui.util.WaktuUtil.getCalendar();
					cDaily.setTime(awalHari(dateMulai));
					ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(cDaily);

					Calendar sDaily = ais.ui.util.WaktuUtil.getCalendar();
					sDaily.setTime(awalHari(dateSampai));
					ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(sDaily);
					sDaily.add(Calendar.DATE, 1);

					long masuk = 0L, alpa = 0L, tidak_hadir = 0L, cuti_memotong = 0L;
					Map<String, Long> cutis = new HashMap<String, Long>();
					long sakit = 0L, izin = 0L, belum = 0L, lain = 0L;
					long jumlahTerlambatTotal = 0L, jumlahCepatKeluarTotal = 0L;
					double jmljamMasuk = 0.0, jmljamLembur = 0.0, jmljamTerlambat = 0.0, jmljamCepat = 0.0;
					long tidakAbsenPulang = 0L, tepatWaktu = 0L, tepatWaktuBanget = 0L, terlambat = 0L,
							pulangcepat = 0L;
					long aktif = 0L, jumlahHariEfektif = 0L, tidakHadir = 0L, tidakHadirTanpaHoliday = 0L;

					List mapKehadiran = new ArrayList();
					int indexTgl = 1;

					// ITERASI HARIAN
					while (cDaily.getTime().before(sDaily.getTime())) {
						int currIdx = currentIndex.incrementAndGet();
						Date tanggal = cDaily.getTime();

						boolean holiday = Common.isHoliday(tanggal);
						Integer hari = cDaily.get(Calendar.DAY_OF_WEEK);

						StatuskehadiranKaryawanHarian skh = statuskehadiranKaryawanHarians
								.get(Common.dateFormat83.get().format(tanggal) + "_" + pegawai.getId());

						boolean adaHadir = (skh != null && isStatus(skh.getStatusabsensi(), Long.valueOf(1L)));
						cDaily.add(Calendar.DATE, 1);

						if ((!holiday && haris[hari - 1].isChecked())) {
							aktif++;
						}

						if (ais.action.master.helper.KehadiranPresensiUtil
								.harusLewatiTanggalKarenaHariTidakDipilih(haris, hari, adaHadir, abaikanKehadiranJikaHariTidakTerpilih)) {
							continue;
						}

						// =========================================================================
						// [OPTIMASI 3]: MENGURANGI UI BOTTLENECK 
						// Modulo diperbesar ke 100 agar locking ServerPush tidak menguras performa
						// =========================================================================
						if (currIdx % 100 == 0 || currIdx == size) {
							int persen = (int) Math.min(99, Math.round((currIdx * 100.0) / size));
							updateProgress(progress, persen, "Memproses " + pegawai.getNama(),
									"Tanggal " + Common.dateFormat6.get().format(tanggal));
						}

						if (skh == null) {
							skh = new StatuskehadiranKaryawanHarian();
							skh.setTanggal(tanggal);
							skh.setPegawai(pegawai);
							skh.setKeterangan("");
							skh.setMasukjam(null);
							skh.setPulangJam(null);
							skh.setMinggu(hari);
							skh.setStatusabsensi(tanggal.before(sekarang) ? ConstantValues.TIDAK_ADA_ALASAN
									: ConstantValues.BELUM_ABSEN);
						}

						CutiDanIzin cutiDanIzin = skh.getCutiDanIzin();

						if (skh.getDetailJenisShiftPegawai() != null
								&& skh.getDetailJenisShiftPegawai().getJenisShiftPegawai() != null
								&& Boolean.TRUE.equals(skh.getDetailJenisShiftPegawai().getJenisShiftPegawai()
										.getHariLiburDitentukan())) {
							holiday = Boolean.TRUE.equals(skh.getDetailJenisShiftPegawai().getKhususBuatHariLibur());
						}

						if (skh.isTidakHadirTanpaHoliday(adaHadir, cutiDanIzin, skh.getLiburNasional())) {
							tidakHadirTanpaHoliday++;
						}

						if (skh.isTidakHadirEfektif(adaHadir, holiday, cutiDanIzin, skh.getLiburNasional())) {
							tidakHadir++;
						}

						HashMap mapS = new HashMap();
						mapS.put("libur", holiday);
						mapS.put("foto_datang", skh.getFotoAbsenDatang());
						mapS.put("foto_pulang", skh.getFotoAbsenPulang());
						mapS.put("lokasi_datang", skh.getLokasiAbsenDatang());
						mapS.put("lokasi_pulang", skh.getLokasiAbsenPulang());

						if (cutiDanIzin != null) {
							mapS.put("status_cuti", cutiDanIzin.getStatusabsensi() == null ? ""
									: cutiDanIzin.getStatusabsensi().getNama());
							mapS.put("disetujui_oleh_cuti", cutiDanIzin.getDisetujuiOleh() == null ? ""
									: cutiDanIzin.getDisetujuiOleh().getUserNama());
							mapS.put("disetujui_tgl_cuti",
									cutiDanIzin.getSetujuiTanggal() == null ? null : cutiDanIzin.getSetujuiTanggal());
							mapS.put("mulai_cuti", cutiDanIzin.getMulai());
							mapS.put("sampai_cuti", cutiDanIzin.getSampai());
							mapS.put("keterangan_cuti", cutiDanIzin.getKeterangan());
						}

						mapS.put("cutiDanIzin", skh.getCutiDanIzin());
						mapS.put("apakah_dosen", pegawai.getDosen() != null);
						mapS.put("nama_satuan_kerja",
								pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama());
						mapS.put("pegawai", pegawai.getId());
						mapS.put("nama", pegawai.getNama());
						mapS.put("nip", pegawai.getMycode());

						Statusabsensi statusabsensi = skh.getStatusabsensi();
						if (ConstantValues.kehadiranHarusMulaiDanSampai) {
							if (skh.getMasukjam() == null || skh.getPulangJam() == null) {
								statusabsensi = ConstantValues.BELUM_ABSEN;
							}
						}

						mapS.put("keterangan",
								(statusabsensi == null ? "" : statusabsensi.getNama()) + " " + skh.getKeterangan());

						Double jumlahJamMasuk = skh.getJumlahJamMasuk();
						Double jumlahJamTerlambat = skh.getJumlahTerlambat();
						Double jumlahJamCepat = skh.getJumlahCepatKeluar();
						jmljamMasuk += doubleValue(jumlahJamMasuk);
						jmljamTerlambat += doubleValue(jumlahJamTerlambat);
						jmljamCepat += doubleValue(jumlahJamCepat);

						mapS.put("jumlahJamMasuk", doubleValue(jumlahJamMasuk));
						mapS.put("jumlahTerlambat", doubleValue(jumlahJamTerlambat));
						mapS.put("jumlahCepatKeluar", doubleValue(jumlahJamCepat));
						mapS.put("jumlahMasukSebelumWaktunya", skh.getJumlahMasukSebelumWaktunya());
						mapS.put("jumlahPulangSetelahWaktunya", skh.getJumlahPulangSetelahWaktunya());

						mapS.put("masuk",
								skh.ambilMasukjam() == null ? "" : Common.timeFormat.get().format(skh.ambilMasukjam()));
						mapS.put("pulang",
								skh.ambilPulangjam() == null ? "" : Common.timeFormat.get().format(skh.ambilPulangjam()));

						Double lemburDa = skh.getJumlahLemburMasuk();
						Date lemburMulai = skh.getLamburMulai();
						Date lemburSampai = skh.getLamburSampai();

						mapS.put("lemburMulai", lemburMulai == null ? "" : Common.timeFormat.get().format(lemburMulai));
						mapS.put("lemburSampai", lemburSampai == null ? "" : Common.timeFormat.get().format(lemburSampai));

						jmljamLembur += doubleValue(lemburDa);
						mapS.put("jumlahLemburMasuk", doubleValue(lemburDa));

						if (Boolean.TRUE.equals(skh.getDatangTerlambat()) && (!isCutiDisetujui(cutiDanIzin))) {
							jumlahTerlambatTotal++;
						}
						if (Boolean.TRUE.equals(skh.getPulangCepat()) && (!isCutiDisetujui(cutiDanIzin))) {
							jumlahCepatKeluarTotal++;
						}

						StatuskehadiranKaryawanHarian.status(holiday, mapS, skh, cutiDanIzin);

						if (skh.ambilMasukjam() == null && (!isCutiDisetujui(cutiDanIzin))) {
							if (skh.getLiburNasional() == null && (cutiDanIzin == null
									|| cutiDanIzin.getStatusabsensi() == null || !isCutiDisetujui(cutiDanIzin))) {
								tidak_hadir++;
							}
						}

						if (cutiDanIzin != null && cutiDanIzin.getStatusabsensi() != null && isCutiDisetujui(cutiDanIzin)) {
							Long c = cutis.get(cutiDanIzin.getStatusabsensi().getNama());
							cutis.put(cutiDanIzin.getStatusabsensi().getNama(), (c == null ? 0L : c) + 1);
						}

						if (cutiDanIzin != null && cutiDanIzin.getStatusabsensi() != null && isCutiDisetujui(cutiDanIzin)
								&& isMemotongJatahCuti(cutiDanIzin)) {
							if (skh.getLiburNasional() == null) {
								cuti_memotong++;
							}
						}

						if (skh.ambilMasukjam() != null && skh.ambilPulangjam() == null) {
							tidakAbsenPulang++;
						}

						if (skh.getLiburNasional() == null) {
							jumlahHariEfektif++;
						}

						mapS.put("hari", Common.dateFormat41.get().format(tanggal));
						mapS.put("tanggal", Common.dateFormat1.get().format(tanggal));
						mapS.put("tanggal_1", Common.simpleDateFormat1.get().format(tanggal));
						mapS.put("tanggal_2", Common.simpleDateFormat2.get().format(tanggal));
						mapS.put("jam_kerja", skh.getDetailJenisShiftPegawai() == null ? ""
								: Common.timeFormat.get().format(skh.getDetailJenisShiftPegawai().getMulai()) + " - "
										+ Common.timeFormat.get().format(skh.getDetailJenisShiftPegawai().getSampai()));

						// =========================================================================
						// [OPTIMASI 4]: ANTI NULL POINTER EXCEPTION
						// Mencegah error put() pada ConcurrentHashMap global
						// =========================================================================
						for (Object key : mapS.keySet()) {
							Object value = mapS.get(key);
							if (value == null) {
								value = ""; // Default kosong jika tidak ada data
							}
							if (key != null) {
								parameterPegawai.put(key + "_" + pegawai.getId() + "_" + indexTgl, value);
							}
						}

						for (int i = 1; i <= 10; i++) {
							mapS.put("log_waktu_" + i, "");
							mapS.put("log_waktu1_" + i, "");
						}

						int indexLog = 1;
						String logAbsensi = skh.getLogAbsensi();
						String[] d = logAbsensi == null ? new String[0] : logAbsensi.split(";");
						for (String ss : d) {
							try {
								// FIX ParseException rutin: logAbsensi bisa mengandung segmen
								// KOSONG (mis. ";;" berurutan) yg lolos cek startsWith("700")
								// (kosong != diawali "700") lalu melempar ParseException di
								// parse("") -- kondisi yg genuinely LANGKA seharusnya, bukan
								// rutin. Saring segmen kosong dulu.
								if (!ss.trim().isEmpty() && !ss.startsWith("700")) {
									Date dss = Common.dateFormat84.get().parse(ss);
									mapS.put("log_waktu_" + indexLog, Common.timeFormat.get().format(dss));
									mapS.put("log_waktu1_" + indexLog, Common.timeFormat2.get().format(dss));
									indexLog++;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiPerOrang.java:1294");
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Absensi Pegawai Per Orang", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
							}
						}

						mapKehadiran.add(mapS);

						// MEMPROSES TAMBAHAN PARAMETER PENGAJUAN (Memanfaatkan Cache RAM)
						String keyTanggal = Common.dateFormat85.get().format(tanggal);
						List<PengajuanPegawai> pengajuanPegawaisData = tglsTugas.get(keyTanggal);

						if (pengajuanPegawaisData != null && !pengajuanPegawaisData.isEmpty()) {
							for (PengajuanPegawai pengajuanPegawai : pengajuanPegawaisData) {
								Long idJenis = pengajuanPegawai.getJenisPengajuanPegawai().getId();
								Map<String, String> paramLabels = cacheParamMappers.get(idJenis);

								if (paramLabels != null) {
									Map nilaiTambahan = parseParameterTambahan(pengajuanPegawai.getParameterTambahanInds());

									for (String jenis : paramLabels.keySet()) {
										String labelInputan = paramLabels.get(jenis);
										Object nilai = nilaiTambahan.get(jenis == null ? "" : jenis.toLowerCase());
										String val = stringValue(nilai);

										// Memasukkan parameter tambahan langsung ke mapS dengan proteksi null.
										mapS.put("pengajuan_" + idJenis + "_" + labelInputan, val);
										mapS.put("pengajuan_" + jenis, val);
									}
								}
							}
						}

						indexTgl++;

						mapS.put("terlambat", false);
						mapS.put("pulangcepat", false);
						mapS.put("tepatWaktu", false);

						if (adaHadir || !isCutiDisetujui(cutiDanIzin)) {
							if (Boolean.TRUE.equals(skh.getDatangTerlambat())) {
								mapS.put("terlambat", true);
								terlambat++;
							} else if (Boolean.TRUE.equals(skh.getDatangCepat())) {
								mapS.put("pulangcepat", true);
								pulangcepat++;
							} else {
								mapS.put("tepatWaktu", true);
								tepatWaktu++;
							}

							if (skh.getMasukjam() != null && skh.getDetailJenisShiftPegawai() != null
									&& skh.getDetailJenisShiftPegawai().getMulai() != null
									&& Double.parseDouble(Common.timeFormat2.get()
											.format(skh.getDetailJenisShiftPegawai().getMulai())) >= Double
													.parseDouble(Common.timeFormat2.get().format(skh.getMasukjam()))) {
								tepatWaktuBanget++;
							}

							if (isStatus(statusabsensi, Long.valueOf(1L)))
								masuk++;
							else if (!holiday && isStatus(statusabsensi, Long.valueOf(2L)))
								alpa++;
							else if (isStatus(statusabsensi, Long.valueOf(3L)))
								sakit++;
							else if (isStatus(statusabsensi, Long.valueOf(4L)))
								izin++;
							else if (isStatus(statusabsensi, Long.valueOf(5L)))
								belum++;
							else
								lain++;
						}
					} // End while loop (Daily)

					map.put("aktif", aktif);
					map.put("terlambat", terlambat);
					map.put("pulangcepat", pulangcepat);
					map.put("tepatWaktu", tepatWaktu);
					map.put("tepatWaktuBanget", tepatWaktuBanget);

					for (Object o : ConstantValues.ambilBerdasarClass(Statusabsensi.class).values()) {
						try {
							Statusabsensi sa = (Statusabsensi) o;
							map.put("jml_" + sa.getNama(), 0L);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiPerOrang.java:1376");
						}
					}

					for (String skey : cutis.keySet()) {
						map.put("jml_" + skey, cutis.get(skey));
					}

					map.put("jumlahHariEfektif", jumlahHariEfektif);
					map.put("tidakAbsenPulang", tidakAbsenPulang);
					map.put("mapKehadiran", mapKehadiran);

					// Gunakan reflection JRMapCollectionDataSource secara aman sesuai original code
					Object dataSource = new net.sf.jasperreports.engine.data.JRMapCollectionDataSource(mapKehadiran);
					map.put("jrMapKehadiran", dataSource);

					map.put("masuk", masuk);
					map.put("alpa", alpa);
					map.put("sakit", sakit);
					map.put("izin", izin);
					map.put("belum", belum);
					map.put("lain", lain);
					map.put("jumlahTerlambat", jumlahTerlambatTotal);
					map.put("jumlahCepatKeluar", jumlahCepatKeluarTotal);

					map.put("tidakHadir", tidakHadir);
					map.put("tidakHadirTanpaHoliday", tidakHadirTanpaHoliday);
					map.put("tidak_hadir", tidak_hadir);
					map.put("cuti_memotong", cuti_memotong);

					map.put("jmljamMasuk", jmljamMasuk);
					map.put("jmljamLembur", jmljamLembur);
					map.put("jmljamTerlambat", jmljamTerlambat);
					map.put("jmljamCepat", jmljamCepat);

					putAllParameter(parameterPegawai);
					finalMapPerPegawai.put(pegawai.getId(), map);

				} // End execute method
			}); // End parallel task

			// Set data akhir sesuai urutan pegawai dari query agar hasil cetak tetap stabil.
			ArrayList hasilAkhir = new ArrayList();
			for (Pegawai p : pegawais) {
				if (p != null && finalMapPerPegawai.containsKey(p.getId())) {
					hasilAkhir.add(finalMapPerPegawai.get(p.getId()));
				}
			}
			this.maps = hasilAkhir;
			statuskehadiranKaryawanHarians.clear();
			finalMapPerPegawai.clear();

			updateProgress(progress, 100, "Data selesai diproses", "Menyiapkan file PDF laporan");

		} catch (Exception e) {
			throw e; // Lemparkan error agar ditangani level atas. UI error ditampilkan oleh pemanggil.
		} finally {
			ais.action.report.Report.closeNativeSession(session);
			ais.action.report.Report.closeCurrentSessionQuietly();
		}
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		if (sedangMemprosesLaporan) {
			return;
		}

		sedangMemprosesLaporan = true;
		parameters = buatParameterBaru();
		desktop = Executions.getCurrent().getDesktop();

		final ProgressContext progress = tampilkanProgressBar();

		/* OPTIMASI FASE 5: server push dulu dinyalakan di sini tetapi TIDAK PERNAH dimatikan,
		 * sehingga browser terus polling (menahan thread Tomcat) selama tab terbuka walau
		 * laporan sudah selesai. Tugas juga dijalankan pada thread MENTAH tanpa batas.
		 * jalankanDenganPush() menyalakan push ber-reference-count, menjalankan tugas pada pool
		 * daemon berbatas milik AsyncTaskManager, lalu MELEPAS push di finally. */
		ais.common.AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() {

			@Override
			public void run() {
				try {
					updateProgress(progress, 1, "Membaca filter laporan",
							"Menyiapkan rentang tanggal dan pilihan hari aktif");

					generateDataDanImageAlbum(progress);

					updateProgress(progress, 99, "Membuat file PDF",
							"Data sudah lengkap, laporan sedang dibuat");

					File file = null;
					Executions.activate(desktop);
					try {
						file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), REPORT_NAME,
								ais.ui.util.WaktuUtil.getDate(), null, toolbar);
					} finally {
						Executions.deactivate(desktop);
					}

					sembunyikanProgressDanTampilkanPdf(progress, file);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanAbsensiPegawaiPerOrang.java:1478");
					tampilkanErrorProgress(progress, e);
				} finally {
					sedangMemprosesLaporan = false;
					if (progress != null && !progress.selesai && !progress.gagal) {
						pastikanProgressBerhenti(progress,
								"Data sudah selesai diproses. Jika PDF belum tampil, silakan klik Tampilkan kembali.");
					}
				}
			}
		});

	}


}