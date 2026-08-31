package ais.action.report;
import ais.common.PesanFormalHelper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Serializable;
import java.net.URLEncoder;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.surat.util.SuratUtil;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonFileUtil;
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.common.ResponseContext;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.PendukungReport;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.SubReport;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.ReportHistory;
import ais.ui.util.MyIframe;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import it.businesslogic.ireport.export.JRTxtExporter;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.engine.export.JRGraphics2DExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.util.FileResolver;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

/**
 * Penyusun/penyaji laporan untuk report. Kelas ini mengubah data domain menjadi bentuk laporan
 * yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String PDF}, {@code String XLS}, {@code
 * String RTF}, {@code String HTML}, {@code String DOCX}, {@code String PPTX}, {@code String ODT}, {@code String
 * ODS}; inisialisasi/lifecycle ({@code initDefaultParameter()}); pembacaan/pencarian ({@code ambilLockJasper()},
 * {@code getProgressContext()}, {@code isReportErrorDownloadEnabled()}, {@code getKonfigurasiNilai()}, {@code
 * tampilErrorSessionQuietly()}, {@code getRootCause()}); validasi/perhitungan ({@code hitungPersen()}, {@code
 * hitungJumlahHariInklusif()}, {@code isPathGambarTidakValid()}, {@code kosongkanParameterGambarTidakValid()},
 * {@code bolehKosongSebagaiNullDiJasper()}); mutasi data ({@code updateProgress()}, {@code
 * setPreviewPdfDefaultSekali()}, {@code setReportKey()}, {@code setLaporanDefaultPdf()}, {@code
 * saveReportHistory()}, {@code setlogo()}); penghapusan/pembatalan ({@code removeProgressContext()});
 * pelaporan/ekspor ({@code isReportProgressAutoHide()}, {@code isReportErrorFriendlyEnabled()}, {@code
 * isReportErrorStackTraceEnabled()}, {@code isReportErrorParameterEnabled()}, {@code
 * isReportErrorLogConsoleEnabled()}, {@code buildReportErrorDetailText()}); operasi domain lain ({@code nvl()},
 * {@code normalizePercent()}, {@code putProgressContext()}, {@code isKonfigurasiAktif()}, {@code
 * createProgress()}, {@code finishProgress()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
@SuppressWarnings("deprecation")
public class Report extends GenericAutowireComposer {

	private static final long serialVersionUID = 7702912791441013522L;

	public static String PDF = "pdf";
	public static String XLS = "xls";
	public static String RTF = "rtf";
	public static String HTML = "html";
	public static String DOCX = "docx";
	public static String PPTX = "pptx";
	public static String ODT = "odt";
	public static String ODS = "ods";
	public static String TXT = "txt";
	public static String CSV = "csv";

	// FIX RACE CONDITION recompile/fillReport JasperReports (NoClassDefFoundError "wrong name"):
	// dua thread yang mencetak laporan DENGAN NAMA SAMA (mis. banyak calon mahasiswa cetak
	// "KartuUjianSpmbMandiri"/"Keterangan_Lulus" bersamaan saat periode pengumuman PMB) bisa
	// saling tabrakan saat salah satu thread menimpa file .jasper (recompileJasperJikaJrxmlLebihBaru)
	// di waktu yang sama thread lain sedang memuat bytecode ekspresi dari file yang sama
	// (JRClassLoader.loadClassFromBytes) -> nama kelas hasil kompilasi in-memory tidak lagi cocok.
	// Lock di-scope PER NAMA FILE .jasper (bukan lock global) supaya laporan LAIN tetap bisa
	// diproses paralel tanpa saling menunggu.
	private static final ConcurrentMap<String, Object> lockKompilasiJasperPerNama = new ConcurrentHashMap<String, Object>();

	private static Object ambilLockJasper(File fileJasper) {
		String key = fileJasper == null ? "" : fileJasper.getAbsolutePath();
		Object lock = lockKompilasiJasperPerNama.get(key);
		if (lock == null) {
			Object baru = new Object();
			Object sebelumnya = lockKompilasiJasperPerNama.putIfAbsent(key, baru);
			lock = sebelumnya == null ? baru : sebelumnya;
		}
		return lock;
	}

	private static List<String> tetapDimasukkan = new ArrayList<String>();
	static {
		tetapDimasukkan.add("ttd_nama_khs_pasca");
		tetapDimasukkan.add("ttd_nip_khs_pasca");
		tetapDimasukkan.add("ttd_nip_khs");
		tetapDimasukkan.add("ttd_nama_khs");
		tetapDimasukkan.add("ttd_nip_khs_kiri");
		tetapDimasukkan.add("ttd_nama_khs_kiri");
		tetapDimasukkan.add("nama_persetujuan_uts");
		tetapDimasukkan.add("nama_persetujuan_uas");
		tetapDimasukkan.add("nip_persetujuan_uts");
		tetapDimasukkan.add("nip_persetujuan_uas");
		tetapDimasukkan.add("nip_ttd_kartu_mahasiswa");
		tetapDimasukkan.add("tulisan_nip");
		tetapDimasukkan.add("label_kota");
		tetapDimasukkan.add("tata_tertib_kartu_uts");
		tetapDimasukkan.add("tata_tertib_kartu_uas");
		tetapDimasukkan.add("tata_tertib_kartu_mahasiswa");
		tetapDimasukkan.add("label_jabatan_kartu_mahasiswa");
		tetapDimasukkan.add("label_ttd_kartu_mahasiswa");
		tetapDimasukkan.add("nip_ttd_kartu_mahasiswa");
		tetapDimasukkan.add("masa_berlaku_kartu_mahasiswa");
		tetapDimasukkan.add("apakah_tampilan_cr_code");
		tetapDimasukkan.add("jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa");
	}

	static {
		DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();
		JRPropertiesUtil.getInstance(context).setProperty("net.sf.jasperreports.xpath.executer.factory",
				"net.sf.jasperreports.engine.util.xml.JaxenXPathExecuterFactory");
		// Font yang dirujuk di jrxml (mis. "arial") sering TIDAK terpasang di JVM/server Linux,
		// sehingga JRFontNotFoundException menggagalkan SELURUH pembuatan laporan. Set agar font
		// yang hilang DIABAIKAN (otomatis fallback ke font default) → laporan tetap tergenerate.
		JRPropertiesUtil.getInstance(context).setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
	}


	/*
	 * =============================================================================
	 * = REPORT PROGRESS, DATE RANGE, AND SAFE RESOURCE HELPERS
	 * =============================================================================
	 * Dipusatkan di Report.java agar semua class laporan dapat memakai pola yang sama
	 * tanpa membuat progress bar, normalisasi tanggal, dan penutupan session berulang.
	 */
	public static final String PARAM_REPORT_PROGRESS = "_REPORT_PROGRESS_CONTEXT";

	/**
	 * Tipe implementasi bersarang {@link ProgressContext} milik {@link Report}. Kelas ini memberi nama pada state
	 * atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link Report}. Dependensi
	 * yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code org.zkoss.zul.Vbox box}, {@code
	 * org.zkoss.zul.Progressmeter progressmeter}, {@code org.zkoss.zul.Label title}, {@code org.zkoss.zul.Label
	 * detail}, {@code org.zkoss.zul.Vbox errorBox}, {@code org.zkoss.zk.ui.Desktop desktop}, {@code Component
	 * parent}, {@code boolean autoCreated}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 *
	 * @see Report
	 */
	public static class ProgressContext {
		private org.zkoss.zul.Vbox box;
		private org.zkoss.zul.Progressmeter progressmeter;
		private org.zkoss.zul.Label title;
		private org.zkoss.zul.Label detail;
		private org.zkoss.zul.Vbox errorBox;
		private org.zkoss.zk.ui.Desktop desktop;
		private Component parent;
		private boolean autoCreated;
		private File errorDetailFile;
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link Report}. Tipe ini memisahkan satu variasi perilaku lokal
	 * tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link Report}. Dependensi
	 * yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini merupakan
	 * detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code execute}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see Report
	 */
	private static interface FileReportExecutor {
		File execute() throws Exception;
	}

	/**
	 * Tipe implementasi bersarang {@link ReportGenerationException} milik {@link Report}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link Report}. Dependensi
	 * yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code File detailFile}; operasi lokal:
	 * {@code getDetailFile}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see Report
	 */
	public static class ReportGenerationException extends Exception {
		private static final long serialVersionUID = 3416043229438988841L;
		private File detailFile;

		public ReportGenerationException(String message, Throwable cause, File detailFile) {
			super(message, cause);
			this.detailFile = detailFile;
		}

		public File getDetailFile() {
			return detailFile;
		}
	}

	private static String nvl(String value, String defaultValue) {
		return value == null || value.trim().length() == 0 ? defaultValue : value;
	}

	private static int normalizePercent(int percent) {
		if (percent < 0) {
			return 0;
		}
		if (percent > 100) {
			return 100;
		}
		return percent;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void putProgressContext(Map parameters, ProgressContext progress) {
		if (parameters != null && progress != null) {
			parameters.put(PARAM_REPORT_PROGRESS, progress);
		}
	}

	@SuppressWarnings("rawtypes")
	public static ProgressContext getProgressContext(Map parameters) {
		if (parameters == null) {
			return null;
		}
		Object progress = parameters.get(PARAM_REPORT_PROGRESS);
		return progress instanceof ProgressContext ? (ProgressContext) progress : null;
	}

	@SuppressWarnings("rawtypes")
	public static void removeProgressContext(Map parameters) {
		if (parameters != null) {
			try {
				parameters.remove(PARAM_REPORT_PROGRESS);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:225");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}
	}

	private static boolean isKonfigurasiAktif(String key, String defaultValue) {
		try {
			return Konfigurasi.AKTIF.equalsIgnoreCase(Common.getKonfigurasi(key, defaultValue).getNilai());
		} catch (Exception e) {
			return Konfigurasi.AKTIF.equalsIgnoreCase(defaultValue);
		}
	}

	private static boolean isReportProgressAutoHide() {
		return isKonfigurasiAktif("report_progress_hide_setelah_selesai", Konfigurasi.AKTIF);
	}

	private static boolean isReportErrorFriendlyEnabled() { return isKonfigurasiAktif("report_error_notifikasi_ramah_aktif", Konfigurasi.AKTIF); }
	private static boolean isReportErrorDownloadEnabled() { return isKonfigurasiAktif("report_error_download_detail_aktif", Konfigurasi.AKTIF); }
	private static boolean isReportErrorStackTraceEnabled() { return isKonfigurasiAktif("report_error_detail_tampilkan_stacktrace", Konfigurasi.AKTIF); }
	private static boolean isReportErrorParameterEnabled() { return isKonfigurasiAktif("report_error_detail_tampilkan_parameter", Konfigurasi.TIDAK_AKTIF); }
	private static boolean isReportErrorLogConsoleEnabled() { return isKonfigurasiAktif("report_error_log_teknis_ke_console", Konfigurasi.AKTIF); }

	private static String getKonfigurasiNilai(String key, String defaultValue) {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
			return konfigurasi == null || konfigurasi.getNilai() == null ? defaultValue : konfigurasi.getNilai();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static ProgressContext createProgress(Component parent, String title, String detail) {
		ProgressContext progress = new ProgressContext();
		progress.parent = parent;
		try {
			if (org.zkoss.zk.ui.Executions.getCurrent() != null) {
				progress.desktop = org.zkoss.zk.ui.Executions.getCurrent().getDesktop();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:264");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		Component progressParent = parent;
		if (progressParent == null) {
			try {
				if (ExecutionsCtrl.getCurrentCtrl() != null && ExecutionsCtrl.getCurrentCtrl().getCurrentPage() != null
						&& ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot() instanceof Component) {
					progressParent = (Component) ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:274");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}

		if (progressParent == null) {
			return progress;
		}

		try {
			if (progressParent instanceof Center) {
				progressParent.getChildren().clear();
			}

			org.zkoss.zul.Vbox box = new org.zkoss.zul.Vbox();
			box.setWidth("96%");
			box.setStyle("margin:16px auto;padding:18px 20px;border-radius:18px;"
					+ "background:linear-gradient(135deg,#ffffff,#eff6ff);"
					+ "border:1px solid #bfdbfe;box-shadow:0 14px 32px rgba(15,23,42,0.12);"
					+ "font-family:Arial,sans-serif;");
			box.setParent(progressParent);
			progress.box = box;
			progress.autoCreated = parent == null;

			org.zkoss.zul.Html header = new org.zkoss.zul.Html();
			header.setContent("<div style='font-family:Arial,sans-serif;'>"
					+ "<div style='font-size:18px;font-weight:bold;color:#0f172a;margin-bottom:6px;'>"
					+ escapeHtml(nvl(title, "Sedang menyiapkan laporan")) + "</div>"
					+ "<div style='font-size:13px;color:#64748b;line-height:1.6;'>"
					+ escapeHtml(nvl(detail, "Data laporan sedang diproses. Tunggu sampai indikator selesai."))
					+ "</div></div>");
			header.setParent(box);

			org.zkoss.zul.Label titleLabel = new org.zkoss.zul.Label(nvl(title, "Memulai proses laporan"));
			titleLabel.setStyle("font-size:13px;font-weight:bold;color:#1d4ed8;margin-top:14px;");
			titleLabel.setParent(box);
			progress.title = titleLabel;

			org.zkoss.zul.Progressmeter meter = new org.zkoss.zul.Progressmeter();
			meter.setWidth("100%");
			meter.setHeight("18px");
			meter.setValue(0);
			meter.setStyle("margin-top:8px;border-radius:999px;");
			meter.setParent(box);
			progress.progressmeter = meter;

			org.zkoss.zul.Label detailLabel = new org.zkoss.zul.Label("0% - " + nvl(detail, "menunggu data diproses"));
			detailLabel.setStyle("font-size:12px;color:#475569;margin-top:8px;");
			detailLabel.setParent(box);
			progress.detail = detailLabel;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:323");
			// Progress bar hanya pendukung tampilan. Jika gagal dibuat, laporan tetap diproses.
		}
		return progress;
	}

	public static void updateProgress(final ProgressContext progress, final int percent, final String title,
			final String detail) {
		if (progress == null) {
			return;
		}
		boolean activated = false;
		try {
			if (progress.desktop != null && org.zkoss.zk.ui.Executions.getCurrent() == null) {
				org.zkoss.zk.ui.Executions.activate(progress.desktop);
				activated = true;
			}
			int value = normalizePercent(percent);
			if (progress.progressmeter != null) {
				progress.progressmeter.setValue(value);
			}
			if (progress.title != null) {
				progress.title.setValue(nvl(title, "Memproses laporan"));
			}
			if (progress.detail != null) {
				progress.detail.setValue(value + "% - " + nvl(detail, "data sedang diproses"));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:350");
			// Desktop bisa tidak aktif jika halaman sudah ditutup. Proses laporan tidak boleh gagal karena UI progress.
		} finally {
			if (activated) {
				try {
					org.zkoss.zk.ui.Executions.deactivate(progress.desktop);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:356");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}
	}

	public static void finishProgress(final ProgressContext progress) {
		if (progress == null) {
			return;
		}
		boolean activated = false;
		try {
			if (progress.desktop != null && org.zkoss.zk.ui.Executions.getCurrent() == null) {
				org.zkoss.zk.ui.Executions.activate(progress.desktop);
				activated = true;
			}
			if (progress.box != null) {
				progress.box.setVisible(false);
				try {
					progress.box.detach();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:376");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:379");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		} finally {
			if (activated) {
				try {
					org.zkoss.zk.ui.Executions.deactivate(progress.desktop);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:384");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}
	}

	public static void errorProgress(final ProgressContext progress, final Exception error) {
		File detailFile = null;
		if (error instanceof ReportGenerationException) {
			detailFile = ((ReportGenerationException) error).getDetailFile();
		}
		errorProgress(progress, error, detailFile);
	}

	public static void errorProgress(final ProgressContext progress, final Exception error, final File detailFile) {
		if (progress == null) return;
		boolean activated = false;
		try {
			if (progress.desktop != null && org.zkoss.zk.ui.Executions.getCurrent() == null) {
				org.zkoss.zk.ui.Executions.activate(progress.desktop);
				activated = true;
			}
			progress.errorDetailFile = detailFile;
			// Satu kode rujukan per kejadian: dicetak pada pesan yang dilihat pengguna DAN pada
			// detail teknis, agar laporan pengguna dapat dicocokkan dengan catatan di server.
			final String kodeRujukan = ais.common.DetailTeknisHelper.kodeRujukan();
			if (progress.progressmeter != null) progress.progressmeter.setValue(100);
			if (progress.title != null) {
				progress.title.setValue("Laporan belum bisa dibuat");
				progress.title.setStyle("font-size:14px;font-weight:bold;color:#b91c1c;margin-top:14px;");
			}
			if (progress.detail != null) {
				progress.detail.setValue("Data tidak berubah. Silakan coba kembali atau hubungi admin jika masih muncul.");
				progress.detail.setStyle("font-size:12px;color:#991b1b;margin-top:8px;");
			}
			if (progress.box != null && isReportErrorFriendlyEnabled()) {
				if (progress.errorBox != null) { try { progress.errorBox.detach(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:417"); } }
				org.zkoss.zul.Vbox errorBox = new org.zkoss.zul.Vbox();
				errorBox.setWidth("100%");
				errorBox.setStyle("margin-top:14px;padding:14px 16px;border-radius:14px;background:#fff7ed;border:1px solid #fed7aa;color:#7c2d12;font-family:Arial,sans-serif;");
				errorBox.setParent(progress.box);
				progress.errorBox = errorBox;
				org.zkoss.zul.Html message = new org.zkoss.zul.Html();
				String detailInfo = "";
				if (ais.common.DetailTeknisHelper.tombolAktif()) {
					detailInfo += "<div style='font-size:12px;line-height:1.5;margin-top:8px;color:#9a3412;'>Tekan <b>Lihat Detail Error</b> di bawah untuk melihat dan menyalin rincian kesalahan, lalu kirimkan kepada admin.</div>";
				}
				if (detailFile != null && detailFile.exists() && isReportErrorDownloadEnabled()) {
					detailInfo += "<div style='font-size:12px;line-height:1.5;margin-top:6px;color:#9a3412;'>Jika perlu bantuan admin, gunakan tombol Download detail error di bawah ini.</div>";
				}
				detailInfo += "<div style='font-size:12px;line-height:1.5;margin-top:6px;color:#9a3412;'>Kode rujukan: <b>" + escapeHtml(kodeRujukan) + "</b></div>";
				message.setContent("<div style='font-family:Arial,sans-serif;display:flex;gap:10px;align-items:flex-start;'>"
						+ "<div style='width:24px;height:24px;border-radius:50%;background:#ffedd5;color:#c2410c;text-align:center;font-weight:bold;line-height:24px;flex:0 0 24px;'>!</div>"
						+ "<div style='min-width:0;'><div style='font-size:15px;font-weight:bold;margin-bottom:4px;'>Laporan belum siap ditampilkan</div>"
						+ "<div style='font-size:13px;line-height:1.55;'>" + escapeHtml(getFriendlyReportErrorMessage(error)) + "</div>"
						+ detailInfo + "</div></div>");
				message.setParent(errorBox);

				// Tombol "Lihat Detail Error": exception yang sebenarnya dapat dibuka lalu
				// disalin pengguna untuk dikirim ke admin. Sebelumnya satu-satunya jalur
				// teknis adalah tombol Download di bawah, dan tombol itu hanya muncul bila berkas
				// detail sempat ditulis ke disk -- pada banyak kegagalan berkas itu tidak ada,
				// sehingga pengguna tidak punya jejak teknis apa pun untuk dilaporkan.
				org.zkoss.zul.Hbox barisTombol = ais.common.DetailTeknisHelper.pasangPanel(
						errorBox, "pembuatan laporan", error, detailFile, kodeRujukan);

				if (detailFile != null && detailFile.exists() && isReportErrorDownloadEnabled()) {
					// Pakai baris tombol yang sama bila panel detail sudah membuatnya, agar
					// Download berdampingan dengan Lihat Detail Error, bukan menumpuk.
					org.zkoss.zul.Hbox buttons = barisTombol;
					if (buttons == null) {
						buttons = new org.zkoss.zul.Hbox();
						buttons.setSpacing("8px");
						buttons.setStyle("margin-top:12px;");
						buttons.setParent(errorBox);
					}
					org.zkoss.zul.Button download = new org.zkoss.zul.Button("Download detail error");
					download.setStyle("padding:8px 14px;border-radius:999px;border:1px solid #fb923c;background:#ea580c;color:#ffffff;font-weight:bold;cursor:pointer;");
					download.setParent(buttons);
					download.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							try { Filedownload.save(new FileInputStream(detailFile), "text/plain", detailFile.getName()); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
						}
					});
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:442");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		} finally {
			if (activated) { try { org.zkoss.zk.ui.Executions.deactivate(progress.desktop); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:444"); } }
		}
	}

	public static void finishProgressAndDisplayPdf(final ProgressContext progress, final Center center, final File file) {
		boolean activated = false;
		try {
			if (progress != null && progress.desktop != null && org.zkoss.zk.ui.Executions.getCurrent() == null) {
				org.zkoss.zk.ui.Executions.activate(progress.desktop);
				activated = true;
			}
			if (progress != null && progress.box != null) {
				try {
					progress.box.setVisible(false);
					progress.box.detach();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:459");
					PesanFormalHelper.tampilkanGagalLaporan(center, "pembuatan berkas PDF Report", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
			CommonReport.tampilkanReportPDF(center, file);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalLaporan(center, "pembuatan berkas PDF Report", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		} finally {
			if (activated) {
				try {
					org.zkoss.zk.ui.Executions.deactivate(progress.desktop);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:469");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}
	}

	public static int hitungPersen(int selesai, int total, int minimal, int maksimal) {
		if (total <= 0) {
			return normalizePercent(maksimal);
		}
		if (maksimal < minimal) {
			int tmp = maksimal;
			maksimal = minimal;
			minimal = tmp;
		}
		int range = maksimal - minimal;
		return normalizePercent(minimal + (int) Math.round((selesai * 1.0D / total) * range));
	}

	public static Date awalHari(Date date) {
		if (date == null) {
			return null;
		}
		java.util.Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(date);
		calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
		calendar.set(java.util.Calendar.MINUTE, 0);
		calendar.set(java.util.Calendar.SECOND, 0);
		calendar.set(java.util.Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	public static Date akhirHari(Date date) {
		if (date == null) {
			return null;
		}
		java.util.Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(date);
		calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
		calendar.set(java.util.Calendar.MINUTE, 59);
		calendar.set(java.util.Calendar.SECOND, 59);
		calendar.set(java.util.Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	public static Date tambahHari(Date date, int jumlahHari) {
		if (date == null) {
			return null;
		}
		java.util.Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(date);
		calendar.add(java.util.Calendar.DATE, jumlahHari);
		return calendar.getTime();
	}

	public static int hitungJumlahHariInklusif(Date mulai, Date sampai) {
		if (mulai == null || sampai == null) {
			return 0;
		}
		Date awal = awalHari(mulai);
		Date akhir = awalHari(sampai);
		if (awal.after(akhir)) {
			Date tmp = awal;
			awal = akhir;
			akhir = tmp;
		}
		long selisih = akhir.getTime() - awal.getTime();
		return (int) (selisih / (24L * 60L * 60L * 1000L)) + 1;
	}

	public static Date[] normalisasiRentangTanggalInklusif(Date mulai, Date sampai) {
		Date awal = awalHari(mulai);
		Date akhir = akhirHari(sampai);
		if (awal != null && akhir != null && awal.after(akhir)) {
			Date tmp = awal;
			awal = awalHari(akhir);
			akhir = akhirHari(tmp);
		}
		return new Date[] { awal, akhir };
	}


	public static Session openNativeSession() {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			if (isSessionOpen(session)) {
				return session;
			}
		} catch (Exception e) {
			tampilErrorSessionQuietly(e);
		}
		closeNativeSession(session);

		try {
			session = HibernateUtil.currentNativeSession();
			if (isSessionOpen(session)) {
				return session;
			}
		} catch (Exception e) {
			tampilErrorSessionQuietly(e);
		}
		closeNativeSession(session);
		throw new RuntimeException("Tidak dapat membuka session database baru untuk proses laporan.");
	}

	public static Session ensureOpenSession(Session session) {
		if (isSessionOpen(session)) {
			return session;
		}
		closeNativeSession(session);
		return openNativeSession();
	}

	public static boolean isSessionOpen(Session session) {
		try {
			return session != null && session.isOpen();
		} catch (Exception e) {
			return false;
		}
	}

	public static void closeCurrentSessionQuietly() {
		try {
			String aktif = Common.getKonfigurasi("report_tutup_current_session_di_finally", Konfigurasi.TIDAK_AKTIF).getNilai();
			if (Konfigurasi.AKTIF.equalsIgnoreCase(aktif)) {
				HibernateUtil.closeSession();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:597");
			// Default aman: jangan menutup currentSession milik request lain dari proses laporan background.
		}
	}

	private static void tampilErrorSessionQuietly(Exception e) {
		try {
			Common.tampilErrorJikaAdmin(e);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/Report.java:605");
		}
	}

	public static void closeNativeSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:615");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:619");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		try {
			session.close();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:623");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	private static String escapeHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private static String stackTraceToString(Throwable throwable) {
		if (throwable == null) return "";
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}

	private static Throwable getRootCause(Throwable throwable) {
		Throwable result = throwable;
		while (result != null && result.getCause() != null && result.getCause() != result) result = result.getCause();
		return result == null ? throwable : result;
	}

	private static boolean containsIgnoreCase(String text, String pattern) { return text != null && pattern != null && text.toLowerCase().indexOf(pattern.toLowerCase()) >= 0; }

	private static boolean containsInThrowable(Throwable throwable, String pattern) {
		Throwable current = throwable;
		while (current != null) {
			if (containsIgnoreCase(current.getClass().getName(), pattern) || containsIgnoreCase(current.getMessage(), pattern)) return true;
			current = current.getCause();
		}
		return containsIgnoreCase(stackTraceToString(throwable), pattern);
	}

	private static String getFriendlyReportErrorMessage(Throwable throwable) {
		String defaultMessage = getKonfigurasiNilai("report_error_pesan_user", "Data laporan belum bisa dimuat. Cek kembali filter/periode dan kelengkapan data, lalu klik Tampilkan atau Cetak lagi. Data yang Anda pilih tetap aman dan tidak berubah.");
		if (containsInThrowable(throwable, "FileNotFoundException")
				|| (throwable != null && throwable.getMessage() != null
						&& throwable.getMessage().contains("template tidak ditemukan"))) {
			return getKonfigurasiNilai("report_error_pesan_template_hilang",
					"Template laporan belum tersedia di server. Silakan hubungi admin dan sertakan screenshot halaman ini agar template dapat segera dicek.");
		}
		if (containsInThrowable(throwable, "Invalid UUID string")) return getKonfigurasiNilai("report_error_pesan_invalid_uuid", "Template laporan perlu diperbaiki oleh admin karena formatnya belum sesuai.");
		if (containsInThrowable(throwable, "JRXmlLoader") || containsInThrowable(throwable, "SAXParseException") || containsInThrowable(throwable, "JasperCompileManager")) return getKonfigurasiNilai("report_error_pesan_template_rusak", "Template laporan belum dapat dibaca. Silakan hubungi admin dan sertakan screenshot halaman ini.");
		if (containsInThrowable(throwable, "Parameter") || containsInThrowable(throwable, "parameter")) return getKonfigurasiNilai("report_error_pesan_parameter", "Ada filter atau parameter laporan yang belum lengkap. Periksa pilihan pada menu laporan, lalu coba tampilkan lagi.");
		return defaultMessage;
	}

	private static String safeFileName(String value) {
		if (value == null || value.trim().length() == 0) return "laporan";
		String result = value.replace('\\', '_').replace('/', '_').replace(':', '_').replace('*', '_').replace('?', '_').replace('"', '_').replace('<', '_').replace('>', '_').replace('|', '_').replace(' ', '_');
		return result.length() > 80 ? result.substring(0, 80) : result;
	}

	@SuppressWarnings("rawtypes")
	private static String buildReportErrorDetailText(String formatLaporan, String fileD, String namaAsli, Throwable throwable, Map parameters) {
		StringBuilder sb = new StringBuilder(8192);
		Throwable root = getRootCause(throwable);
		sb.append("DETAIL ERROR PEMBUATAN LAPORAN\n============================================================\n\n");
		sb.append("Penjelasan untuk pengguna:\n").append(getFriendlyReportErrorMessage(throwable)).append("\n\n");
		sb.append("Saran cepat untuk pengguna:\n1. Periksa kembali filter laporan yang dipilih.\n2. Coba buat laporan kembali beberapa saat lagi.\n3. Jika tetap gagal, kirim file detail error ini kepada admin/TI.\n\n");
		sb.append("Informasi teknis ringkas:\n");
		sb.append("- Waktu: ").append(Common.datetimeFormat2s.get().format(WaktuUtil.getDate())).append("\n");
		sb.append("- Nama laporan: ").append(nvl(namaAsli, fileD)).append("\n- File/template: ").append(fileD).append("\n- Format output: ").append(formatLaporan).append("\n");
		sb.append("- Tipe error utama: ").append(throwable == null ? "" : throwable.getClass().getName()).append("\n- Pesan error utama: ").append(throwable == null ? "" : throwable.getMessage()).append("\n");
		sb.append("- Tipe penyebab terdalam: ").append(root == null ? "" : root.getClass().getName()).append("\n- Pesan penyebab terdalam: ").append(root == null ? "" : root.getMessage()).append("\n\n");
		sb.append("Analisis awal untuk admin/TI:\n");
		if (containsInThrowable(throwable, "Invalid UUID string")) {
			sb.append("- JasperReports menemukan UUID tidak valid di JRXML. Contoh yang sering terjadi: uuid='gh-rect-001'.\n- Perbaiki atribut uuid pada elemen JRXML menjadi UUID valid, misalnya hasil java.util.UUID.randomUUID().\n- Buka ulang template di iReport/Jaspersoft Studio yang sesuai, lalu simpan kembali.\n");
		} else if (containsInThrowable(throwable, "JRXmlLoader") || containsInThrowable(throwable, "SAXParseException")) {
			sb.append("- Template JRXML tidak dapat dibaca. Periksa XML, atribut elemen, encoding, dan kompatibilitas JasperReports.\n");
		} else if (containsInThrowable(throwable, "Session is closed")) {
			sb.append("- Session Hibernate tertutup. Pastikan laporan background memakai Report.openNativeSession() dan ditutup di finally.\n");
		} else {
			sb.append("- Periksa stack trace, template laporan, parameter, dan query/data yang digunakan.\n");
		}
		sb.append("\n");
		if (isReportErrorParameterEnabled() && parameters != null) {
			sb.append("Parameter laporan yang dikirim:\n");
			Iterator iterator = parameters.keySet().iterator();
			while (iterator.hasNext()) {
				Object key = iterator.next(); Object value = parameters.get(key); String keyText = String.valueOf(key);
				if (containsIgnoreCase(keyText, "password") || containsIgnoreCase(keyText, "token") || containsIgnoreCase(keyText, "secret")) value = "***disembunyikan***";
				sb.append("- ").append(keyText).append(" = ").append(value).append("\n");
			}
			sb.append("\n");
		} else if (parameters != null) {
			sb.append("Daftar key parameter laporan:\n");
			Iterator iterator = parameters.keySet().iterator(); while (iterator.hasNext()) sb.append("- ").append(String.valueOf(iterator.next())).append("\n");
			sb.append("\n");
		}
		if (isReportErrorStackTraceEnabled()) sb.append("Stack trace teknis:\n------------------------------------------------------------\n").append(stackTraceToString(throwable));
		return sb.toString();
	}

	@SuppressWarnings("rawtypes")
	private static File createReportErrorDetailFile(String formatLaporan, String fileD, String namaAsli, Throwable throwable, Map parameters) {
		OutputStreamWriter writer = null;
		try {
			String configuredDir = getKonfigurasiNilai("report_error_detail_folder", "");
			File dir = configuredDir == null || configuredDir.trim().length() == 0 ? new File(Common.ambilREAL_PATH_REPORT(), "error_report") : new File(configuredDir);
			if (!dir.exists()) dir.mkdirs();
			if (!dir.exists() || !dir.isDirectory() || !dir.canWrite()) dir = new File(System.getProperty("java.io.tmpdir"));
			String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(WaktuUtil.getDate());
			File file = new File(dir, "ERROR_REPORT_" + safeFileName(nvl(namaAsli, fileD)) + "_" + timestamp + ".txt");
			writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			writer.write(buildReportErrorDetailText(formatLaporan, fileD, namaAsli, throwable, parameters)); writer.flush(); return file;
		} catch (Exception e) {
			if (isReportErrorLogConsoleEnabled()) e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/Report.java:729"); return null;
		} finally { if (writer != null) { try { writer.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:730"); } } }
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static File generateFileReportWithProgressInternal(String formatLaporan, Map parameters, String fileD,
			FileReportExecutor executor) throws Exception {
		ProgressContext progress = getProgressContext(parameters);
		boolean autoProgress = false;
		if (progress == null && isKonfigurasiAktif("report_progress_aktif", Konfigurasi.AKTIF)) {
			progress = createProgress(null, "Sedang membuat laporan",
					"Data sedang disiapkan, lalu file " + nvl(formatLaporan, "laporan") + " akan dibuat.");
			autoProgress = true;
			putProgressContext(parameters, progress);
		}
		try {
			updateProgress(progress, 1, "Membaca filter laporan", "Menyiapkan parameter dan pilihan laporan");
			File file = executor.execute();
			updateProgress(progress, 100, "Laporan selesai", "File laporan sudah selesai dibuat");
			if (isReportProgressAutoHide()) {
				finishProgress(progress);
			}
			return file;
		} catch (Exception e) {
			File detailFile = null;
			if (e instanceof ReportGenerationException) detailFile = ((ReportGenerationException) e).getDetailFile();
			errorProgress(progress, e, detailFile);
			throw e;
		} finally {
			if (autoProgress && parameters != null) {
				try {
					parameters.remove(PARAM_REPORT_PROGRESS);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:761");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}
	}

	/*
	 * =============================================================================
	 * = HELPER METHODS (OPTIMASI)
	 * =============================================================================
	 * =
	 */

	/**
	 * Helper untuk memusatkan ekspor JasperPrint. Menggantikan puluhan if-else di
	 * kode lama.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void exportJasperPrint(JasperPrint jasperPrint, String formatLaporan, File myFile) throws Exception {
		if (HTML.equals(formatLaporan)) {
			// HTML "seperti PDF" & MANDIRI: pakai HtmlExporter modern dengan gambar/grafik
			// di-embed sebagai data URI base64 (satu file utuh, layout sama dgn PDF karena
			// memakai hasil isi/fill yang sama). Bila exporter modern gagal, fallback ke
			// exporter HTML klasik agar perilaku lama tetap aman (tidak ada regresi).
			try {
				exportJasperPrintToHtmlMandiri(jasperPrint, myFile);
			} catch (Throwable t) {
				JasperExportManager.exportReportToHtmlFile(jasperPrint, myFile.getAbsolutePath());
			}
			return;
		}

		JRAbstractExporter exporterPDF;
		if (PDF.equals(formatLaporan)) {
			exporterPDF = new JRPdfExporter();
		} else if (XLS.equals(formatLaporan)) {
			exporterPDF = new JRXlsExporter();
		} else if (DOCX.equals(formatLaporan)) {
			exporterPDF = new JRDocxExporter();
		} else if (PPTX.equals(formatLaporan)) {
			exporterPDF = new JRPptxExporter();
		} else if (ODT.equals(formatLaporan)) {
			exporterPDF = new JROdtExporter();
		} else if (ODS.equals(formatLaporan)) {
			exporterPDF = new JROdsExporter();
		} else if (TXT.equals(formatLaporan)) {
			exporterPDF = new JRTxtExporter();
		} else if (CSV.equals(formatLaporan)) {
			exporterPDF = new JRCsvExporter();
		} else if (RTF.equals(formatLaporan)) {
			exporterPDF = new JRRtfExporter();
		} else {
			exporterPDF = new JRPdfExporter();
		}

		exporterPDF.setExporterInput(new SimpleExporterInput(jasperPrint));
		exporterPDF.setExporterOutput(new SimpleOutputStreamExporterOutput(myFile));
		exporterPDF.exportReport();

		// Untuk format PDF: buat pendamping HTML "seperti PDF" agar pratinjau di layar
		// dapat tampil sebagai HTML (default). Best-effort, dilewati untuk unduhan.
		if (PDF.equals(formatLaporan)) {
			tulisSiblingHtmlPreview(jasperPrint, myFile);
		}
	}

	/**
	 * Ekspor {@link JasperPrint} ke HTML MANDIRI (self-contained) yang tampilannya
	 * menyerupai PDF: memakai {@code HtmlExporter} modern, dan SEMUA gambar/grafik
	 * di-embed langsung sebagai data URI base64 di dalam satu berkas HTML (tanpa
	 * folder gambar terpisah seperti exporter HTML klasik). Karena memakai hasil
	 * isi/fill yang SAMA dengan ekspor PDF, tata letak, font, dan posisi elemen
	 * identik dengan versi PDF — hanya format keluarannya HTML.
	 *
	 * <p>Tetap kompatibel Java 1.6/1.7 (tanpa lambda/diamond/stream). Pemanggil
	 * membungkus pemanggilan ini dengan try-catch + fallback ke exporter HTML klasik
	 * sehingga tidak ada regresi bila exporter modern bermasalah.</p>
	 */
	private static void exportJasperPrintToHtmlMandiri(JasperPrint jasperPrint, File myFile) throws Exception {
		net.sf.jasperreports.engine.export.HtmlExporter exporter = new net.sf.jasperreports.engine.export.HtmlExporter();
		exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

		// Peta id-gambar -> data URI base64; diisi saat handleResource, dibaca saat getResourcePath.
		final java.util.Map<String, String> petaGambar = new java.util.HashMap<String, String>();
		net.sf.jasperreports.export.SimpleHtmlExporterOutput output = new net.sf.jasperreports.export.SimpleHtmlExporterOutput(
				myFile);
		output.setImageHandler(new net.sf.jasperreports.engine.export.HtmlResourceHandler() {
			@Override
			public String getResourcePath(String id) {
				return petaGambar.get(id);
			}

			@Override
			public void handleResource(String id, byte[] data) {
				String mime = "image/png";
				try {
					mime = net.sf.jasperreports.engine.util.JRTypeSniffer.getImageTypeValue(data).getMimeType();
				} catch (Throwable t) {
					// Beberapa Jasper element bukan image biner valid. Untuk HTML preview,
					// fallback PNG cukup; jangan jadikan ini error aplikasi.
				}
				petaGambar.put(id, "data:" + mime + ";base64,"
						+ org.apache.commons.codec.binary.Base64.encodeBase64String(data));
			}
		});
		exporter.setExporterOutput(output);

		net.sf.jasperreports.export.SimpleHtmlReportConfiguration config = new net.sf.jasperreports.export.SimpleHtmlReportConfiguration();
		config.setRemoveEmptySpaceBetweenRows(Boolean.TRUE);
		config.setWhitePageBackground(Boolean.FALSE);
		exporter.setConfiguration(config);

		exporter.exportReport();
	}

	// ============================================================
	//  PREVIEW HTML "SEPERTI PDF" (default) — pendamping berkas PDF
	// ============================================================

	/**
	 * Penanda bahwa proses generate saat ini untuk UNDUHAN (bukan pratinjau di
	 * layar). Saat true, pendamping HTML tidak dibuat agar unduhan tidak terbebani.
	 * Di-set di {@link #generateDownloadReport} (sinkron, satu thread dengan
	 * {@code exportJasperPrint}).
	 */
	private static final ThreadLocal<Boolean> MODE_UNDUH = new ThreadLocal<Boolean>();

	/**
	 * Penanda SEKALI-PAKAI: pratinjau berikutnya default ke PDF (bukan HTML), walau
	 * konfigurasi global preview_laporan_html aktif. Dipakai mis. oleh Rapor Siswa.
	 * Di-set tepat sebelum menampilkan pratinjau; dikonsumsi (di-remove) oleh {@link #tampil}.
	 */
	private static final ThreadLocal<Boolean> PREVIEW_PDF_DEFAULT_SEKALI = new ThreadLocal<Boolean>();

	/** Set agar pratinjau berikutnya default ke PDF (sekali pakai). */
	public static void setPreviewPdfDefaultSekali() {
		PREVIEW_PDF_DEFAULT_SEKALI.set(Boolean.TRUE);
	}

	/**
	 * Penanda identitas laporan yang sedang akan dibuka (nama template jrxml, mis. {@code df243403}).
	 * Di-set tepat sebelum {@link #tampil(File, Component)} dipanggil dan dibaca SINKRON di awalnya
	 * (sama pola dengan {@link #PREVIEW_PDF_DEFAULT_SEKALI}). Dipakai untuk: (a) menentukan apakah
	 * jenis laporan ini di-default-kan ke PDF lewat konfigurasi {@code laporan_default_pdf}; dan
	 * (b) memberi identitas ke sakelar admin "Default PDF" agar pengaturannya per-jenis-laporan.
	 */
	private static final ThreadLocal<String> CURRENT_REPORT_KEY = new ThreadLocal<String>();

	/** Set identitas laporan (nama template) untuk pratinjau berikutnya. Aman-null. */
	public static void setReportKey(String key) {
		if (key == null || key.trim().isEmpty()) {
			CURRENT_REPORT_KEY.remove();
		} else {
			CURRENT_REPORT_KEY.set(key.trim());
		}
	}

	/** Kunci konfigurasi daftar jenis laporan yang default-nya PDF (CSV nama template). */
	private static final String KONFIG_LAPORAN_DEFAULT_PDF = "laporan_default_pdf";

	/**
	 * True bila jenis laporan {@code key} di-default-kan tampil sebagai PDF (masuk daftar konfigurasi
	 * {@link #KONFIG_LAPORAN_DEFAULT_PDF}). Perbandingan tidak peka huruf besar/kecil.
	 */
	private static boolean laporanDefaultPdf(String key) {
		if (key == null || key.trim().isEmpty()) {
			return false;
		}
		try {
			String csv = Common.getKonfigurasi(KONFIG_LAPORAN_DEFAULT_PDF, "").getNilai();
			if (csv == null || csv.trim().isEmpty()) {
				return false;
			}
			String cari = key.trim();
			String[] bagian = csv.split(",");
			for (int i = 0; i < bagian.length; i++) {
				if (bagian[i] != null && bagian[i].trim().equalsIgnoreCase(cari)) {
					return true;
				}
			}
		} catch (Throwable t) {
			ais.common.ErrorAuditUtil.record(t, "laporanDefaultPdf src/ais/action/report/Report.java");
		}
		return false;
	}

	/**
	 * Menambah/menghapus sebuah jenis laporan dari daftar "default PDF" pada konfigurasi
	 * {@link #KONFIG_LAPORAN_DEFAULT_PDF}, lalu menyimpannya. Idempoten. Hanya dipanggil dari sakelar
	 * admin di bar pratinjau.
	 *
	 * @param key      nama template laporan.
	 * @param defaultPdf {@code true} = laporan ini default PDF; {@code false} = kembali default HTML.
	 */
	private static void setLaporanDefaultPdf(String key, boolean defaultPdf) {
		if (key == null || key.trim().isEmpty()) {
			return;
		}
		try {
			String cari = key.trim();
			Konfigurasi konfig = Common.getKonfigurasi(KONFIG_LAPORAN_DEFAULT_PDF, "");
			String csv = konfig.getNilai() == null ? "" : konfig.getNilai();

			java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<String>();
			String[] bagian = csv.split(",");
			for (int i = 0; i < bagian.length; i++) {
				if (bagian[i] != null && !bagian[i].trim().isEmpty()
						&& !bagian[i].trim().equalsIgnoreCase(cari)) {
					set.add(bagian[i].trim());
				}
			}
			if (defaultPdf) {
				set.add(cari);
			}

			StringBuilder sb = new StringBuilder();
			for (java.util.Iterator<String> it = set.iterator(); it.hasNext();) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(it.next());
			}
			konfig.setNilai(sb.toString());
			MemoryDbUtil.getKonfigurasi().put(KONFIG_LAPORAN_DEFAULT_PDF, konfig);
			Common.refreshUpdate(konfig);
		} catch (Throwable t) {
			ais.common.ErrorAuditUtil.record(t, "setLaporanDefaultPdf src/ais/action/report/Report.java");
		}
	}

	/**
	 * Apakah pratinjau laporan ditampilkan sebagai HTML (yang dibuat semirip PDF)?
	 * Dikendalikan konfigurasi {@code preview_laporan_html} — DEFAULT AKTIF.
	 * Set ke "0"/TIDAK_AKTIF untuk kembali ke pratinjau PDF lama tanpa redeploy.
	 */
	private static boolean previewHtmlAktif() {
		try {
			return Konfigurasi.AKTIF.equals(
					Common.getKonfigurasi("preview_laporan_html", Konfigurasi.AKTIF).getNilai());
		} catch (Throwable t) {
			return false;
		}
	}

	/** {@link Common#isMobile()} yang aman (tak melempar) untuk dipakai saat generate laporan. */
	private static boolean isMobileAman() {
		try {
			return Common.isMobile();
		} catch (Throwable t) {
			return false;
		}
	}

	/** Nama berkas HTML pendamping untuk sebuah berkas PDF pratinjau. */
	private static File berkasHtmlPendamping(File pdfFile) {
		return pdfFile == null ? null : new File(pdfFile.getAbsolutePath() + ".html");
	}

	/**
	 * Tulis berkas HTML pendamping (self-contained, mirip PDF) di sebelah berkas
	 * PDF pratinjau. Best-effort: kegagalan apa pun TIDAK boleh mengganggu ekspor
	 * utama. Dilewati untuk unduhan ({@link #MODE_UNDUH}), saat konfigurasi mati,
	 * atau laporan sangat besar (jaga performa).
	 */
	private static void tulisSiblingHtmlPreview(JasperPrint jasperPrint, File pdfFile) {
		try {
			if (pdfFile == null || jasperPrint == null) {
				return;
			}
			if (Boolean.TRUE.equals(MODE_UNDUH.get())) {
				return;
			}
			// HTML pendamping dibuat bila konfigurasi aktif ATAU pengguna mobile (browser HP tak bisa
			// menampilkan PDF dalam iframe → pratinjau WAJIB punya versi HTML untuk ditampilkan/dicetak).
			if (!previewHtmlAktif() && !isMobileAman()) {
				return;
			}
			try {
				if (jasperPrint.getPages() != null && jasperPrint.getPages().size() > 300) {
					return; // laporan sangat besar: lewati agar pratinjau tetap ringan
				}
			} catch (Throwable t) {
				return;
			}
			exportJasperPrintToHtmlMandiri(jasperPrint, berkasHtmlPendamping(pdfFile));
		} catch (Throwable t) {
			// best-effort — abaikan, pratinjau akan jatuh-balik ke PDF
		}
	}

	/** Baca seluruh isi berkas menjadi byte[] (gaya Java 1.6/1.7, tanpa java.nio.file). */
	private static byte[] bacaSemuaByte(File f) throws Exception {
		java.io.FileInputStream in = null;
		java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
		try {
			in = new java.io.FileInputStream(f);
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) != -1) {
				bos.write(buf, 0, n);
			}
			return bos.toByteArray();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/report/Report.java:973");
				}
			}
		}
	}

	/**
	 * Ambil snapshot PDF untuk pratinjau asynchronous. Jika berkas belum siap,
	 * pemanggil tetap dapat memakai jalur File lama sebagai fallback.
	 */
	private static byte[] bacaPdfPratinjau(File file) {
		if (file == null || !file.isFile() || file.length() <= 0L) {
			return null;
		}
		try {
			byte[] data = bacaSemuaByte(file);
			if (data == null || data.length < 5) {
				return null;
			}
			if (data[0] != '%' || data[1] != 'P' || data[2] != 'D'
					|| data[3] != 'F' || data[4] != '-') {
				throw new java.io.IOException("Berkas pratinjau bukan dokumen PDF yang valid: "
						+ file.getAbsolutePath());
			}
			return data;
		} catch (Exception gagalBaca) {
			ais.common.ErrorAuditUtil.record(gagalBaca,
					"Gagal membuat snapshot PDF untuk pratinjau report");
			return null;
		}
	}

	/**
	 * Tampilkan berkas HTML pendamping ke dalam {@code center} via Iframe
	 * {@code setContent} (text/html). Karena HTML sudah self-contained (gambar
	 * base64), tidak butuh servlet PDF dan tampil mirip versi PDF.
	 *
	 * @return true bila berhasil menampilkan HTML; false bila gagal (pemanggil
	 *         lanjut ke jalur PDF lama).
	 */
	private static boolean tampilHtmlPendamping(File htmlFile, Component center) {
		try {
			if (htmlFile == null || !htmlFile.exists() || htmlFile.length() <= 0 || center == null) {
				return false;
			}
			Iframe include;
			if (center instanceof Iframe) {
				include = (Iframe) center;
			} else {
				include = new MyIframe();
				include.setParent(center);
				include.setWidth("100%");
			}
			// Tinggi awal aman + scroll; lalu di-AUTO-FIT ke tinggi konten via JS
			// (pasangAutoFitTinggiIframe) → tak ada ruang kosong utk laporan pendek, dan wadah
			// pratinjau bisa di-scroll utuh utk laporan panjang. JANGAN pakai height !important
			// agar JS bisa menimpa tinggi.
			// Tinggi MINIMAL 2000px (permintaan: semua report tampil BESAR, tidak mengecil seperti
			// hasil auto-fit yang keliru mengukur konten). AUTO-FIT hanya MEMBESARKAN bila konten
			// > 2000px (laporan panjang → wadah bisa di-scroll utuh); laporan pendek tetap 2000px.
			// Iframe pratinjau HTML ber-SCROLL SENDIRI (tinggi relatif viewport, sama spt jalur PDF
			// 74vh) → PASTI bisa digulir di dalam popup TANPA bergantung rantai tinggi window→
			// borderlayout→Center yang rapuh (penyebab scrollbar tak muncul). Tidak lagi 2000px (yg
			// butuh wadah induk ikut scroll). Tetap tampil BESAR (≈80% layar) seperti tampilan PDF.
			String htmlAsli = new String(bacaSemuaByte(htmlFile), "UTF-8");
			byte[] htmlRapi = bungkusHtmlPratinjau(htmlAsli).getBytes("UTF-8");
			include.setHeight("2000px");
			include.setStyle("width:100% !important; height:2000px; min-height:2000px; border:0; overflow:auto; background:#e9eef5;");
			include.setScrolling("auto");
			include.setSrc(null);
			include.setContent(new AMedia(htmlFile.getName(), "html", "text/html;charset=UTF-8", htmlRapi));
			pasangAutoFitTinggiIframe(include);
			return true;
		} catch (Throwable t) {
			Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
			return false;
		}
	}

	/**
	 * Bungkus HTML laporan (hasil HtmlExporter Jasper) agar tampil RAPI di pratinjau:
	 * di-tengah horizontal (align center), rata-ATAS (valign top), diberi "kertas"
	 * putih ber-shadow supaya beda dengan latar, dan TIDAK terpotong (lebar alami +
	 * latar bisa di-scroll bila laporan lebih lebar dari area). Style & isi {@code
	 * <body>} laporan asli dipertahankan. Bila laporan sudah berupa gabungan kartu
	 * ({@code ais-surat-bagian}), tidak dibungkus kertas lagi (cukup latar+tengah).
	 */
	/**
	 * Menyetel tinggi {@code <iframe>} pratinjau HTML agar PAS dengan tinggi kontennya (diukur
	 * {@code scrollHeight} dokumen iframe setelah {@code load}). Tanpa ini, iframe bertinggi tetap
	 * menyisakan area kosong besar (laporan pendek) atau memotong (laporan panjang) sehingga scroll
	 * wadah tak mencerminkan isi. Murni sisi klien; same-origin (konten via {@code setContent}).
	 */
	private static void pasangAutoFitTinggiIframe(final Iframe include) {
		try {
			if (include == null) {
				return;
			}
			final String uuid = include.getUuid();
			StringBuilder js = new StringBuilder(1024);
			js.append("(function(){var id='").append(uuid).append("';");
			js.append("function fit(){var f=document.getElementById(id);if(!f)return;");
			js.append("try{var w=f.contentWindow;if(!w)return;var d=w.document;if(!d)return;");
			js.append("var b=d.body,e=d.documentElement;");
			js.append("var h=Math.max(b?b.scrollHeight:0,b?b.offsetHeight:0,e?e.scrollHeight:0);");
			js.append("if(h>2000){f.style.setProperty('height',(h+28)+'px','important');f.style.setProperty('min-height',(h+28)+'px','important');}}catch(ex){}}");
			js.append("var f0=document.getElementById(id);");
			js.append("if(f0){f0.addEventListener('load',function(){fit();setTimeout(fit,120);setTimeout(fit,400);setTimeout(fit,1000);});");
			js.append("try{if(f0.contentWindow)f0.contentWindow.addEventListener('resize',fit);}catch(ig){}}");
			js.append("setTimeout(fit,150);setTimeout(fit,500);setTimeout(fit,1200);");
			js.append("})();");
			Clients.evalJavaScript(js.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1059");
			// Auto-fit hanya penyempurnaan tampilan; bila gagal, iframe tetap tampil dgn tinggi awal+scroll.
		}
	}

	private static String bungkusHtmlPratinjau(String html) {
		try {
			if (html == null) {
				return "";
			}
			boolean sudahBerkartu = html.indexOf("ais-surat-bagian") >= 0;
			StringBuilder styles = new StringBuilder();
			int cari = 0;
			while (true) {
				int s = html.indexOf("<style", cari);
				if (s < 0) {
					break;
				}
				int e = html.indexOf("</style>", s);
				if (e < 0) {
					break;
				}
				styles.append(html.substring(s, e + 8)).append("\n");
				cari = e + 8;
			}
			String isi = html;
			int b = html.indexOf("<body");
			if (b >= 0) {
				int gt = html.indexOf('>', b);
				int akhir = html.indexOf("</body>", b);
				if (gt >= 0 && akhir > gt) {
					isi = html.substring(gt + 1, akhir);
				}
			}
			StringBuilder out = new StringBuilder(isi.length() + styles.length() + 900);
			out.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
			out.append(styles);
			out.append("<style>html,body{margin:0;padding:0;height:auto;}");
			// align=center + valign=top + latar abu (pembeda) + bisa di-scroll (tak terpotong).
			out.append("body{background:#e9eef5;padding:24px 16px;box-sizing:border-box;text-align:center;}");
			if (!sudahBerkartu) {
				// "kertas" laporan: shrink-to-content (display:table) + margin auto = di-tengah
				// bila muat, geser-scroll bila lebih lebar (tak terpotong); shadow pembeda latar.
				out.append(".ais-report-paper{display:table;margin:0 auto;text-align:left;background:#ffffff;");
				out.append("box-shadow:0 10px 30px rgba(15,23,42,.30);padding:16px 18px;border:1px solid #e2e8f0;}");
			}
			out.append("</style></head><body>");
			if (sudahBerkartu) {
				out.append(isi);
			} else {
				out.append("<div class='ais-report-paper'>").append(isi).append("</div>");
			}
			out.append("</body></html>");
			return out.toString();
		} catch (Throwable t) {
			return html;
		}
	}

	/**
	 * Gabungkan beberapa berkas HTML mandiri (self-contained) menjadi SATU berkas
	 * HTML — dipakai untuk dokumen multi-bagian yang di-PDF-merge (mis. surat keluar
	 * dengan beberapa layout). Isi {@code <body>} tiap bagian ditumpuk dengan pemisah
	 * halaman; blok {@code <style>} tiap bagian dipertahankan. Best-effort: bila tidak
	 * ada bagian valid mengembalikan false (pemanggil tetap punya versi PDF merge).
	 *
	 * @return true bila {@code output} berhasil ditulis.
	 */
	public static boolean gabungHtmlMandiri(java.util.List<File> bagianHtml, File output) {
		try {
			if (bagianHtml == null || bagianHtml.isEmpty() || output == null) {
				return false;
			}
			StringBuilder styles = new StringBuilder();
			StringBuilder bodies = new StringBuilder();
			int jml = 0;
			for (int i = 0; i < bagianHtml.size(); i++) {
				File f = bagianHtml.get(i);
				if (f == null || !f.exists() || f.length() <= 0) {
					continue;
				}
				String html = new String(bacaSemuaByte(f), "UTF-8");
				// Kumpulkan semua blok <style>...</style> dari bagian ini.
				int cari = 0;
				while (true) {
					int s = html.indexOf("<style", cari);
					if (s < 0) {
						break;
					}
					int e = html.indexOf("</style>", s);
					if (e < 0) {
						break;
					}
					styles.append(html.substring(s, e + 8)).append("\n");
					cari = e + 8;
				}
				// Ambil isi di antara <body ...> dan </body>; bila tak ada, pakai apa adanya.
				String isi = html;
				int b = html.indexOf("<body");
				if (b >= 0) {
					int tutupTag = html.indexOf('>', b);
					int akhir = html.indexOf("</body>", b);
					if (tutupTag >= 0 && akhir > tutupTag) {
						isi = html.substring(tutupTag + 1, akhir);
					}
				}
				if (jml > 0) {
					bodies.append("<div style='page-break-before:always;height:16px'></div>");
				}
				bodies.append("<div class='ais-surat-bagian'>").append(isi).append("</div>");
				jml++;
			}
			if (jml == 0) {
				return false;
			}
			StringBuilder out = new StringBuilder(bodies.length() + styles.length() + 512);
			out.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
			out.append(styles);
			out.append("<style>html,body{margin:0;padding:0;background:#eef2f7;}"
					+ ".ais-surat-bagian{background:#ffffff;margin:10px auto;display:inline-block;"
					+ "box-shadow:0 1px 6px rgba(15,23,42,.15);}</style>");
			out.append("</head><body style='text-align:center;'>");
			out.append(bodies);
			out.append("</body></html>");

			java.io.FileOutputStream fo = null;
			try {
				fo = new java.io.FileOutputStream(output);
				fo.write(out.toString().getBytes("UTF-8"));
			} finally {
				if (fo != null) {
					try {
						fo.close();
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/report/Report.java:1192");
					}
				}
			}
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Helper untuk mengisi Jasper Report dan MENGAMANKAN Connection agar tidak
	 * bocor ke memori!
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static JasperPrint fillJasperReport(File fileJasper, Map parameters, List maps) throws Exception {
		// FIX RACE CONDITION (NoClassDefFoundError "wrong name"): kalau percobaan pertama gagal
		// karena tabrakan compile/class-loading .jasper antar-thread (lihat catatan di
		// lockKompilasiJasperPerNama di atas), coba SEKALI LAGI -- percobaan kedua nyaris selalu
		// berhasil karena jendela race sudah lewat begitu thread lain selesai menimpa file.
		try {
			return fillJasperReportSekali(fileJasper, parameters, maps);
		} catch (NoClassDefFoundError ncdfe) {
			ais.common.ErrorAuditUtil.record(new Exception(ncdfe), "auto-audit(retry-race-jasper) src/ais/action/report/Report.java:fillJasperReport");
			try {
				return fillJasperReportSekali(fileJasper, parameters, maps);
			} catch (NoClassDefFoundError ncdfe2) {
				throw new Exception("Gagal memuat kelas hasil kompilasi Jasper untuk " + fileJasper
						+ " walau sudah dicoba ulang (kemungkinan tabrakan kompilasi bersamaan): " + ncdfe2.getMessage(), ncdfe2);
			}
		} catch (ClassNotFoundException cnfe) {
			ais.common.ErrorAuditUtil.record(cnfe, "auto-audit(retry-race-jasper) src/ais/action/report/Report.java:fillJasperReport");
			try {
				return fillJasperReportSekali(fileJasper, parameters, maps);
			} catch (ClassNotFoundException cnfe2) {
				throw new Exception("Gagal memuat kelas hasil kompilasi Jasper untuk " + fileJasper
						+ " walau sudah dicoba ulang (kemungkinan tabrakan kompilasi bersamaan): " + cnfe2.getMessage(), cnfe2);
			}
		} catch (Exception fillError) {
			/*
			 * Gambar pada sebagian template berupa URL AmbilLampiran internal. Jasper membuka
			 * URL itu sebagai request server baru tanpa cookie login, sehingga endpoint yang
			 * benar mengembalikan 401/403 dan proses fill berhenti sebelum mekanisme pembersih
			 * gambar pada tahap export sempat berjalan. Kosongkan hanya parameter gambar lalu
			 * ulangi fill satu kali; isi laporan lainnya tetap diterbitkan.
			 */
			if (isImageFormatError(fillError) && kosongkanParameterGambarTidakValid(parameters)) {
				return fillJasperReportSekali(fileJasper, parameters, maps);
			}
			throw fillError;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static JasperPrint fillJasperReportSekali(File fileJasper, Map parameters, List maps) throws Exception {
		if (parameters == null) {
			parameters = new HashMap();
		}
		JasperPrint jp = null;
		Connection conn = null;
		Session session = openNativeSession();
		try {
			// FIX RACE CONDITION: recompile (tulis file .jasper) dan fillReport (baca +
			// class-load bytecode ekspresi dari file .jasper yang SAMA) DIKUNCI per-nama-file
			// supaya dua thread yang mencetak laporan bernama sama tidak saling tabrakan
			// (salah satu menimpa file selagi yang lain masih memuat kelas dari isi lama).
			// Kunci di-scope per-nama file (bukan global) -- laporan LAIN tetap paralel.
			Object lockJasper = ambilLockJasper(fileJasper);
			synchronized (lockJasper) {
				// FIX (.jasper basi): kalau .jrxml sumber lebih baru dari .jasper terkompilasi
				// (mis. jrxml baru saja diperbaiki tapi belum ada yang kompilasi ulang manual),
				// .jasper lama TETAP bisa dimuat JVM (tidak melempar UnsupportedClassVersionError)
				// tapi berisi bytecode ekspresi LAMA yang bisa salah tipe (mis. field yang dulu
				// java.lang.Boolean lalu diubah ke java.lang.Object/String di jrxml) -> exception
				// rutin di runtime (ClassCastException dsb) padahal sumbernya sudah benar.
				// Recompile proaktif berdasar mtime supaya .jasper selalu sinkron dgn .jrxml,
				// bukan cuma reaktif menunggu UnsupportedClassVersionError seperti di bawah.
				recompileJasperJikaJrxmlLebihBaru(fileJasper);
				conn = session.connection();
				pasangFileResolverReport(parameters, fileJasper);
				normalisasiDataJasper(parameters, maps);
				try {
					if (maps != null) {
						JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(maps);
						parameters.put("REPORT_CONNECTION", conn);
						jp = JasperFillManager.fillReport(fileJasper.getAbsolutePath(), parameters, dataSource);
					} else {
						parameters.put("REPORT_CONNECTION", conn);
						jp = JasperFillManager.fillReport(fileJasper.getAbsolutePath(), parameters, conn);
					}
				} catch (UnsupportedClassVersionError ucve) {
					// .jasper dikompilasi JDK lebih baru dari server; hapus dan kompilasi ulang dari .jrxml
					String jasperPath = fileJasper.getAbsolutePath();
					String jrxmlPath = jasperPath.substring(0, jasperPath.length() - ".jasper".length()) + ".jrxml";
					File fileJrxml = new File(jrxmlPath);
					if (fileJasper.exists()) fileJasper.delete();
					if (!fileJrxml.exists()) {
						throw new Exception("File .jrxml tidak ditemukan untuk recompile: " + jrxmlPath, ucve);
					}
					compileJasperAtomically(fileJrxml, fileJasper);
					normalisasiDataJasper(parameters, maps);
					if (maps != null) {
						JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(maps);
						parameters.put("REPORT_CONNECTION", conn);
						jp = JasperFillManager.fillReport(fileJasper.getAbsolutePath(), parameters, dataSource);
					} else {
						parameters.put("REPORT_CONNECTION", conn);
						jp = JasperFillManager.fillReport(fileJasper.getAbsolutePath(), parameters, conn);
					}
				} catch (Exception loadError) {
					if (!isCorruptJasper(loadError)) {
						throw loadError;
					}
					File fileJrxml = pasanganJrxml(fileJasper);
					if (fileJrxml == null || !fileJrxml.exists()) {
						throw loadError;
					}
					compileJasperAtomically(fileJrxml, fileJasper);
					normalisasiDataJasper(parameters, maps);
					if (maps != null) {
						JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(maps);
						parameters.put("REPORT_CONNECTION", conn);
						jp = JasperFillManager.fillReport(fileJasper.getAbsolutePath(), parameters, dataSource);
					} else {
						parameters.put("REPORT_CONNECTION", conn);
						jp = JasperFillManager.fillReport(fileJasper.getAbsolutePath(), parameters, conn);
					}
				}
			}
		} finally {
			// SANGAT PENTING UNTUK MEMORI: Mengembalikan koneksi ke pool!
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1246");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Report", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
				parameters.remove("REPORT_CONNECTION");
			}
			closeNativeSession(session);
		}
		return jp;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void pasangFileResolverReport(final Map parameters, final File fileJasper) {
		if (parameters == null || parameters.get(JRParameter.REPORT_FILE_RESOLVER) != null) {
			return;
		}
		parameters.put(JRParameter.REPORT_FILE_RESOLVER, new FileResolver() {
			public File resolveFile(String fileName) {
				if (fileName == null || fileName.trim().length() == 0) {
					return null;
				}
				File langsung = new File(fileName);
				if (langsung.exists()) {
					return langsung;
				}
				File file = cariFileReport(fileJasper == null ? null : fileJasper.getParentFile(), fileName);
				if (file != null) {
					return file;
				}
				file = cariFileReport(new File(Common.REAL_PATH, "img"), fileName);
				if (file != null) {
					return file;
				}
				File reportDir = new File(Common.ambilREAL_PATH_REPORT());
				file = cariFileReport(reportDir, fileName);
				if (file != null) {
					return file;
				}
				return cariFileReport(reportDir.getParentFile(), fileName);
			}
		});
	}

	private static File cariFileReport(File folder, String fileName) {
		if (folder == null || fileName == null) {
			return null;
		}
		File file = new File(folder, fileName);
		return file.exists() ? file : null;
	}

	/**
	 * Kompilasi ulang {@code .jasper} dari {@code .jrxml} sumbernya bila {@code .jrxml}
	 * lebih baru (berdasar {@code lastModified()}) daripada {@code .jasper} yang sudah
	 * ada. Dipanggil proaktif sebelum tiap {@code fillReport} supaya template yang baru
	 * diperbaiki di {@code .jrxml} tidak terus memakai bytecode ekspresi LAMA yang masih
	 * tersimpan di {@code .jasper} basi (yang tetap bisa dimuat JVM tanpa error, sehingga
	 * jalur {@code UnsupportedClassVersionError} di bawah tidak pernah terpicu untuk
	 * kasus ini). Aman dipanggil berulang: no-op kalau {@code .jrxml} tidak ada atau
	 * {@code .jasper} sudah sama baru/lebih baru.
	 */
	private static void recompileJasperJikaJrxmlLebihBaru(File fileJasper) {
		try {
			if (fileJasper == null) {
				return;
			}
			String jasperPath = fileJasper.getAbsolutePath();
			if (!jasperPath.toLowerCase().endsWith(".jasper")) {
				return;
			}
			String jrxmlPath = jasperPath.substring(0, jasperPath.length() - ".jasper".length()) + ".jrxml";
			File fileJrxml = new File(jrxmlPath);
			if (!fileJrxml.exists()) {
				return;
			}
			if (!fileJasper.exists() || fileJrxml.lastModified() > fileJasper.lastModified()) {
				compileJasperAtomically(fileJrxml, fileJasper);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:recompileJasperJikaJrxmlLebihBaru");
			// Gagal recompile proaktif -> biarkan lanjut pakai .jasper lama; jalur
			// UnsupportedClassVersionError di fillJasperReport tetap jadi jaring pengaman
			// terakhir utk kasus JDK mismatch, dan error lain (mis. jrxml rusak) akan
			// tetap muncul lewat fillReport seperti biasa (tidak disembunyikan).
		}
	}

	private static File pasanganJrxml(File fileJasper) {
		if (fileJasper == null) return null;
		String path = fileJasper.getAbsolutePath();
		if (!path.toLowerCase().endsWith(".jasper")) return null;
		return new File(path.substring(0, path.length() - ".jasper".length()) + ".jrxml");
	}

	private static boolean isCorruptJasper(Throwable error) {
		Throwable current = error;
		while (current != null) {
			String name = current.getClass().getName();
			String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
			if (name.indexOf("EOFException") >= 0 || name.indexOf("StreamCorruptedException") >= 0
					|| message.indexOf("error loading object from file") >= 0) return true;
			current = current.getCause();
		}
		return false;
	}

	private static void compileJasperAtomically(File jrxml, File jasper) throws Exception {
		CommonFileUtil.ensureWritableFile(jasper, 20L * 1024L * 1024L);
		// FIX RACE CONDITION (akar NoClassDefFoundError "wrong name" & FileNotFoundException
		// pada .jasper.tmp): sebelumnya nama file sementara hanya "<jasper>.tmp" -- SAMA untuk
		// SEMUA thread yang kebetulan mengompilasi target .jasper yang SAMA secara bersamaan
		// (mis. subreport bersama dipicu oleh beberapa permintaan cetak paralel yang belum
		// terkunci, lihat pemanggil TANPA ambilLockJasper() di initDefaultParameter). Dua thread
		// menulis/menghapus file sementara yang SAMA menghasilkan: hasil kompilasi terbaca kosong
		// (temp.length()<=0, ditimpa thread lain baru mulai menulis), file sumber rename hilang
		// diambil/dihapus thread lain lebih dulu (FileNotFoundException di FileUtils.copyFile),
		// atau -- paling parah -- isi .jasper akhir tercampur (nama kelas hasil kompilasi yang
		// tertanam di bytecode berasal dari SATU thread sementara metadata JasperReport di
		// sekitarnya dari thread LAIN) yang memicu NoClassDefFoundError "wrong name" saat
		// subreport dimuat. Perbaikan: setiap PEMANGGILAN memakai nama file sementara UNIK
		// (thread id + nanoTime), sehingga kompilasi paralel utk target yang SAMA tidak pernah
		// lagi berbagi file sementara -- rename akhir ke path FINAL tetap "atomic" per-thread
		// (last-writer-wins yang aman, karena sumbernya .jrxml yang sama).
		File temp = new File(jasper.getAbsolutePath() + ".tmp" + Thread.currentThread().getId() + "_" + System.nanoTime());
		try {
			JasperCompileManager.compileReportToFile(jrxml.getAbsolutePath(), temp.getAbsolutePath());
			if (!temp.isFile() || temp.length() <= 0L) {
				throw new java.io.IOException("Hasil kompilasi template Jasper kosong: " + jrxml);
			}
			if (jasper.exists() && !jasper.delete()) {
				throw new java.io.IOException("Template Jasper lama tidak dapat diganti: " + jasper);
			}
			if (!temp.renameTo(jasper)) {
				org.apache.commons.io.FileUtils.copyFile(temp, jasper);
			}
		} finally {
			if (temp.exists()) temp.delete();
		}
	}

	private static boolean isImageFormatError(Throwable e) {
		while (e != null) {
			String msg = e.getMessage();
			if (msg != null) {
				String lower = msg.toLowerCase();
				if (lower.indexOf("imageformat") >= 0
						|| lower.indexOf("error opening input stream from url") >= 0
						|| lower.indexOf("error loading image") >= 0) {
					return true;
				}
			}
			e = e.getCause();
		}
		return false;
	}

	/**
	 * SATU gambar rusak TIDAK BOLEH menggagalkan SELURUH laporan.
	 *
	 * <p><b>Masalah yang diperbaiki.</b> {@link #kosongkanParameterGambarTidakValid(Map)} hanya
	 * menjangkau gambar yang dikirim sebagai PARAMETER laporan (logo, kop, ttd, barcode). Gambar
	 * yang berasal dari DATA/blob -- mis. foto mahasiswa pada Kartu Mahasiswa yang diambil
	 * langsung oleh query/field di dalam template -- tidak tersentuh sama sekali, sehingga satu
	 * blob foto yang korup/kosong tetap membuat
	 * {@code JRPdfExporter} melempar {@code JRException: java.io.IOException: The byte array is
	 * not a recognized imageformat} dan pengguna TIDAK mendapat kartu sama sekali.</p>
	 *
	 * <p><b>Cara kerja.</b> Bekerja pada hasil isi/fill ({@link JasperPrint}) sehingga mencakup
	 * SEMUA gambar, apapun asalnya (parameter maupun data/blob):</p>
	 * <ol>
	 * <li><b>Pakai mekanisme bawaan JasperReports.</b> Setiap {@code JRPrintImage} di-set
	 * {@code onErrorType=BLANK}. Exporter JasperReports (lihat {@code JRPdfExporter.exportImage}
	 * yang memanggil {@code RendererUtil.handleImageError(e, printImage.getOnErrorTypeValue())})
	 * menghormati setelan ini: gambar yang gagal didekode DILEWATI (dibiarkan kosong) dan proses
	 * ekspor tetap berjalan, bukan dilempar sebagai error. Default template biasanya
	 * {@code ERROR}, itulah sebabnya satu gambar rusak menggagalkan seluruh PDF.</li>
	 * <li><b>Jaring pengaman tambahan.</b> Renderer yang datanya memang tidak dapat didekode
	 * sebagai gambar dibuang (di-set null), sehingga aman juga untuk exporter/jalur yang tidak
	 * memeriksa {@code onErrorType}.</li>
	 * </ol>
	 *
	 * <p>Sengaja HANYA dipanggil pada jalur PERCOBAAN ULANG (setelah ekspor pertama gagal karena
	 * gambar), supaya jalur normal tidak terbebani menelusuri seluruh halaman (halaman laporan
	 * besar bisa ter-virtualisasi ke swap file, lihat {@code ReportThrottle}).</p>
	 *
	 * @param jasperPrint hasil isi laporan (boleh null)
	 * @return true bila ada yang diubah (ada gunanya mencoba ekspor ulang)
	 */
	@SuppressWarnings("rawtypes")
	private static boolean bersihkanGambarRusakDiJasperPrint(JasperPrint jasperPrint) {
		if (jasperPrint == null) {
			return false;
		}
		boolean berubah = false;
		try {
			List pages = jasperPrint.getPages();
			if (pages == null || pages.isEmpty()) {
				return false;
			}
			for (int i = 0; i < pages.size(); i++) {
				Object page = pages.get(i);
				if (page instanceof net.sf.jasperreports.engine.JRPrintPage) {
					if (bersihkanGambarRusakDiElemen(((net.sf.jasperreports.engine.JRPrintPage) page).getElements(),
							0)) {
						berubah = true;
					}
				}
			}
		} catch (Throwable t) {
			ais.common.ErrorAuditUtil.record(t,
					"auto-audit(bersihkanGambarRusakDiJasperPrint) src/ais/action/report/Report.java");
		}
		return berubah;
	}

	/**
	 * Penelusuran rekursif elemen hasil cetak (frame bisa bersarang). Lihat
	 * {@link #bersihkanGambarRusakDiJasperPrint(JasperPrint)}.
	 */
	@SuppressWarnings("rawtypes")
	private static boolean bersihkanGambarRusakDiElemen(List elements, int kedalaman) {
		if (elements == null || elements.isEmpty() || kedalaman > 20) {
			return false;
		}
		boolean berubah = false;
		for (int i = 0; i < elements.size(); i++) {
			Object el;
			try {
				el = elements.get(i);
			} catch (Throwable t) {
				continue;
			}
			if (el instanceof net.sf.jasperreports.engine.JRPrintFrame) {
				if (bersihkanGambarRusakDiElemen(((net.sf.jasperreports.engine.JRPrintFrame) el).getElements(),
						kedalaman + 1)) {
					berubah = true;
				}
			} else if (el instanceof net.sf.jasperreports.engine.JRPrintImage) {
				net.sf.jasperreports.engine.JRPrintImage gambar = (net.sf.jasperreports.engine.JRPrintImage) el;
				try {
					if (net.sf.jasperreports.engine.type.OnErrorTypeEnum.BLANK != gambar.getOnErrorTypeValue()) {
						gambar.setOnErrorType(net.sf.jasperreports.engine.type.OnErrorTypeEnum.BLANK);
						berubah = true;
					}
				} catch (Throwable t) {
					// Best-effort saja; renderer rusak tetap ditangani di bawah.
				}
				/* AKAR MASALAH (JRException "The byte array is not a recognized imageformat."
				 * saat ekspor PDF kartu mahasiswa): pemeriksaan "gambar rusak" di bawah memakai
				 * ImageIO, sedangkan yang menyematkan gambar ke PDF adalah iText. Keduanya TIDAK
				 * mendukung format yang sama. BMP dan WebP terbaca sempurna oleh ImageIO sehingga
				 * dinyatakan sehat dan dibiarkan lewat, lalu ditolak iText dan menggagalkan
				 * SELURUH laporan. Foto yang diunggah pengguna kerap berformat seperti itu.
				 *
				 * Daripada mengosongkan fotonya, data dikonversi lebih dulu ke PNG supaya foto
				 * TETAP tercetak. Bila konversi pun tidak mungkin, barulah gambar dikosongkan --
				 * lebih baik satu foto hilang daripada seluruh laporan gagal. */
				byte[] gambarPng = konversiGambarTakDidukungPdf(gambar);
				if (gambarPng != null) {
					try {
						gambar.setRenderer(
								net.sf.jasperreports.renderers.SimpleDataRenderer.getInstance(gambarPng));
						berubah = true;
					} catch (Throwable t) {
						ais.common.ErrorAuditUtil.record(t,
								"auto-audit(konversi gambar laporan ke PNG) src/ais/action/report/Report.java");
					}
				} else if (rendererGambarRusak(gambar) || gambarTakDapatDisematkanPdf(gambar)) {
					try {
						gambar.setRenderer(null);
						berubah = true;
					} catch (Throwable t) {
						ais.common.ErrorAuditUtil.record(t,
								"auto-audit(setRenderer null gambar rusak) src/ais/action/report/Report.java");
					}
				}
			}
		}
		return berubah;
	}

	/**
	 * Apakah data gambar pada sebuah elemen hasil cetak BENAR-BENAR tidak dapat didekode.
	 * Data SVG (mis. barcode/QR dari komponen Barcode4J) sengaja TIDAK dianggap rusak walaupun
	 * {@link ImageIO} tidak bisa membacanya.
	 */
	private static boolean rendererGambarRusak(net.sf.jasperreports.engine.JRPrintImage gambar) {
		net.sf.jasperreports.renderers.Renderable renderer;
		try {
			renderer = gambar.getRenderer();
		} catch (Throwable t) {
			return true;
		}
		if (renderer == null) {
			return false;
		}
		if (!(renderer instanceof net.sf.jasperreports.renderers.DataRenderable)) {
			// Renderer non-data (mis. grafik/chart yang digambar langsung ke Graphics2D) tidak
			// bisa diperiksa lewat byte array -- biarkan apa adanya.
			return false;
		}
		byte[] data;
		try {
			data = ((net.sf.jasperreports.renderers.DataRenderable) renderer)
					.getData(DefaultJasperReportsContext.getInstance());
		} catch (Throwable t) {
			return true;
		}
		if (data == null || data.length == 0) {
			return true;
		}
		try {
			if (net.sf.jasperreports.renderers.util.RendererUtil.getInstance(DefaultJasperReportsContext.getInstance())
					.isSvgData(data)) {
				return false;
			}
		} catch (Throwable t) {
			ais.common.ErrorAuditUtil.record(t,
					"auto-audit(cek data SVG gambar laporan) src/ais/action/report/Report.java");
		}
		try {
			return ImageIO.read(new java.io.ByteArrayInputStream(data)) == null;
		} catch (Throwable t) {
			return true;
		}
	}

	/**
	 * Apakah data gambar dapat <b>disematkan langsung ke PDF oleh iText</b> (pustaka yang
	 * dipakai JasperReports untuk ekspor PDF), dikenali dari magic bytes-nya.
	 *
	 * <p>Daftar ini sengaja disusun dari sudut pandang iText, BUKAN {@link ImageIO}. Keduanya
	 * berbeda: ImageIO membaca BMP dengan baik, sementara {@code com.lowagie.text.Image
	 * .getInstance(byte[])} menolaknya dengan "The byte array is not a recognized
	 * imageformat.". Memakai ImageIO sebagai penentu kelayakan PDF itulah yang membuat
	 * laporan gagal di produksi meskipun gambarnya sebetulnya "terbaca".</p>
	 */
	static boolean formatGambarDidukungPdf(byte[] data) {
		if (data == null || data.length < 4) {
			return false;
		}
		int b0 = data[0] & 0xFF;
		int b1 = data[1] & 0xFF;
		int b2 = data[2] & 0xFF;
		int b3 = data[3] & 0xFF;
		if (b0 == 0xFF && b1 == 0xD8) return true;                             // JPEG
		if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return true; // PNG
		if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46) return true;               // GIF
		if (b0 == 0x49 && b1 == 0x49 && b2 == 0x2A && b3 == 0x00) return true; // TIFF little-endian
		if (b0 == 0x4D && b1 == 0x4D && b2 == 0x00 && b3 == 0x2A) return true; // TIFF big-endian
		if (b0 == 0xD7 && b1 == 0xCD && b2 == 0xC6 && b3 == 0x9A) return true; // WMF
		if (b0 == 0x00 && b1 == 0x00 && b2 == 0x00 && b3 == 0x0C) return true; // JPEG 2000 (JP2)
		if (b0 == 0xFF && b1 == 0x4F && b2 == 0xFF && b3 == 0x51) return true; // JPEG 2000 codestream
		return false;
	}

	/**
	 * Konversi data gambar yang tidak didukung iText menjadi PNG.
	 *
	 * @return byte PNG hasil konversi, atau {@code null} bila tidak perlu dikonversi (format
	 *         sudah didukung) atau tidak dapat dikonversi (data bukan gambar yang terbaca).
	 */
	static byte[] konversiGambarKePng(byte[] data) {
		if (data == null || data.length == 0 || formatGambarDidukungPdf(data)) {
			return null;
		}
		try {
			java.awt.image.BufferedImage gambar = ImageIO.read(new java.io.ByteArrayInputStream(data));
			if (gambar == null) {
				return null;
			}
			java.io.ByteArrayOutputStream keluaran = new java.io.ByteArrayOutputStream();
			if (!ImageIO.write(gambar, "png", keluaran)) {
				return null;
			}
			byte[] hasil = keluaran.toByteArray();
			return hasil.length == 0 ? null : hasil;
		} catch (Throwable t) {
			return null;
		}
	}

	/** Ambil byte data gambar dari elemen cetak Jasper; {@code null} bila bukan renderer data. */
	private static byte[] dataGambarCetak(net.sf.jasperreports.engine.JRPrintImage gambar) {
		try {
			net.sf.jasperreports.renderers.Renderable renderer = gambar.getRenderer();
			if (!(renderer instanceof net.sf.jasperreports.renderers.DataRenderable)) {
				return null;
			}
			byte[] data = ((net.sf.jasperreports.renderers.DataRenderable) renderer)
					.getData(DefaultJasperReportsContext.getInstance());
			if (data == null || data.length == 0) {
				return null;
			}
			try {
				/* SVG disematkan lewat Batik, bukan iText -- jangan diutak-atik. */
				if (net.sf.jasperreports.renderers.util.RendererUtil
						.getInstance(DefaultJasperReportsContext.getInstance()).isSvgData(data)) {
					return null;
				}
			} catch (Throwable t) {
				/* Gagal memeriksa SVG: lanjut saja, pemeriksaan magic bytes tetap aman. */
			}
			return data;
		} catch (Throwable t) {
			return null;
		}
	}

	/** Versi {@link #konversiGambarKePng(byte[])} untuk elemen cetak Jasper. */
	private static byte[] konversiGambarTakDidukungPdf(net.sf.jasperreports.engine.JRPrintImage gambar) {
		return konversiGambarKePng(dataGambarCetak(gambar));
	}

	/**
	 * Gambar yang formatnya TIDAK dapat disematkan iText dan konversinya pun gagal. Gambar
	 * seperti ini harus dikosongkan, kalau tidak ekspor PDF akan gagal seluruhnya.
	 */
	private static boolean gambarTakDapatDisematkanPdf(net.sf.jasperreports.engine.JRPrintImage gambar) {
		byte[] data = dataGambarCetak(gambar);
		return data != null && !formatGambarDidukungPdf(data);
	}

	/**
	 * Cek apakah sebuah path berkas "terlihat" seperti gambar (berdasarkan
	 * ekstensi) namun sebenarnya TIDAK VALID (tidak ada, kosong/0-byte, atau
	 * gagal dibaca sebagai gambar oleh {@link ImageIO}). Dipakai sebagai jaring
	 * pengaman tambahan di {@link #kosongkanParameterGambarTidakValid(Map)} untuk
	 * parameter gambar yang di-passing sebagai path String (konvensi yang dipakai
	 * di seluruh kelas ini, mis. logo, barcode, kop fakultas, ttd) sehingga tidak
	 * lolos hanya karena nama parameter tidak mengandung kata kunci umum
	 * (contoh: parameter "barcode").
	 */
	private static boolean isPathGambarTidakValid(String path) {
		if (path == null) {
			return false;
		}
		String lower = path.toLowerCase();
		boolean ekstensiGambar = lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
				|| lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".tif")
				|| lower.endsWith(".tiff");
		if (!ekstensiGambar) {
			return false;
		}
		try {
			File f = new File(path);
			if (!f.exists() || f.length() == 0) {
				return true;
			}
			return ImageIO.read(f) == null;
		} catch (Exception e) {
			return true;
		}
	}

	private static boolean kosongkanParameterGambarTidakValid(Map parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return false;
		}
		boolean berubah = false;
		Iterator iterator = parameters.keySet().iterator();
		while (iterator.hasNext()) {
			Object key = iterator.next();
			Object value = parameters.get(key);
			String nama = key == null ? "" : key.toString().toLowerCase();
			boolean kandidatNama = nama.indexOf("foto") >= 0 || nama.indexOf("photo") >= 0
					|| nama.indexOf("image") >= 0 || nama.indexOf("gambar") >= 0
					|| nama.indexOf("logo") >= 0 || nama.indexOf("ttd") >= 0
					|| nama.indexOf("tanda_tangan") >= 0 || nama.indexOf("stempel") >= 0
					|| nama.indexOf("barcode") >= 0 || nama.indexOf("qr_code") >= 0
					|| nama.indexOf("qrcode") >= 0 || nama.indexOf("cr_code") >= 0
					|| nama.indexOf("kop") >= 0;
			boolean kandidatNilai = value instanceof BufferedImage || value instanceof InputStream
					|| value instanceof byte[] || value instanceof File;
			// Jaring pengaman tambahan: gambar yang di-passing sebagai path String (bukan
			// byte[]/File/BufferedImage) dan namanya tidak mengandung kata kunci di atas,
			// tetap dicek langsung berkasnya agar gambar yang benar-benar rusak/kosong
			// tetap terjaring meski nama parameternya tidak lazim.
			boolean kandidatPathRusak = !kandidatNama && !kandidatNilai && value instanceof String
					&& isPathGambarTidakValid((String) value);
			if (kandidatNama || kandidatNilai || kandidatPathRusak) {
				parameters.put(key, null);
				berubah = true;
			}
		}
		return berubah;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void normalisasiDataJasper(Map parameters, List maps) {
		normalisasiParameterTeksJasper(parameters);
		normalisasiMapDataSourceJasper(maps);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void normalisasiParameterTeksJasper(Map parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return;
		}
		Iterator iterator = parameters.keySet().iterator();
		while (iterator.hasNext()) {
			Object key = iterator.next();
			Object value = parameters.get(key);
			if (value == null) {
				if (bolehKosongSebagaiNullDiJasper(key) || !namaParameterTeks(key)) {
					continue;
				}
				parameters.put(key, "");
			} else if (value instanceof String && "null".equalsIgnoreCase(((String) value).trim())) {
				parameters.put(key, "");
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void normalisasiMapDataSourceJasper(List maps) {
		if (maps == null || maps.isEmpty()) {
			return;
		}
		for (Object data : maps) {
			if (!(data instanceof Map)) {
				continue;
			}
			Map row = (Map) data;
			Iterator iterator = row.keySet().iterator();
			while (iterator.hasNext()) {
				Object key = iterator.next();
				Object value = row.get(key);
				if (value == null) {
					row.put(key, nilaiDefaultMapJasper(key));
				} else if (value instanceof String && "null".equalsIgnoreCase(((String) value).trim())) {
					row.put(key, "");
				}
			}
		}
	}

	private static Object nilaiDefaultMapJasper(Object key) {
		if (namaParameterTanggal(key)) {
			return null;
		}
		if (namaParameterBoolean(key)) {
			return Boolean.FALSE;
		}
		if (namaParameterAngkaBulat(key)) {
			return Integer.valueOf(0);
		}
		if (namaParameterAngkaPecahan(key)) {
			return Double.valueOf(0);
		}
		return "";
	}

	private static boolean namaParameterTanggal(Object key) {
		String nama = key == null ? "" : key.toString().toLowerCase();
		return nama.indexOf("tanggal") >= 0 || nama.startsWith("tgl") || nama.indexOf("_tgl") >= 0
				|| nama.indexOf("date") >= 0 || nama.indexOf("_jam") >= 0;
	}

	private static boolean bolehKosongSebagaiNullDiJasper(Object key) {
		String nama = key == null ? "" : key.toString().toLowerCase();
		return PARAM_REPORT_PROGRESS.equals(key) || "report_connection".equals(nama)
				|| nama.indexOf("foto") >= 0 || nama.indexOf("photo") >= 0
				|| nama.indexOf("image") >= 0 || nama.indexOf("gambar") >= 0
				|| nama.indexOf("logo") >= 0 || nama.indexOf("ttd") >= 0
				|| nama.indexOf("tanda_tangan") >= 0 || nama.indexOf("stempel") >= 0
				|| nama.indexOf("barcode") >= 0 || nama.indexOf("qr_code") >= 0
				|| nama.indexOf("qrcode") >= 0 || nama.indexOf("cr_code") >= 0
				|| nama.indexOf("kop") >= 0;
	}

	private static boolean namaParameterTeks(Object key) {
		String nama = key == null ? "" : key.toString().toLowerCase();
		return nama.indexOf("nama") >= 0 || nama.indexOf("kode") >= 0 || nama.indexOf("label") >= 0
				|| nama.indexOf("ket") >= 0 || nama.indexOf("judul") >= 0 || nama.indexOf("alamat") >= 0
				|| nama.indexOf("nim") >= 0 || nama.indexOf("npm") >= 0 || nama.indexOf("nip") >= 0
				|| nama.indexOf("nidn") >= 0 || nama.indexOf("no_") >= 0 || nama.startsWith("no")
				|| nama.indexOf("nomor") >= 0 || nama.indexOf("tempat") >= 0 || nama.indexOf("ttl") >= 0
				|| nama.indexOf("jenjang") >= 0 || nama.indexOf("jurusan") >= 0
				|| nama.indexOf("fakultas") >= 0 || nama.indexOf("prodi") >= 0
				|| nama.indexOf("matakuliah") >= 0 || nama.indexOf("mata_kuliah") >= 0
				|| nama.indexOf("nilai_huruf") >= 0 || nama.indexOf("yudisium") >= 0
				|| nama.indexOf("gelar") >= 0 || nama.indexOf("konsentrasi") >= 0;
	}

	private static boolean namaParameterBoolean(Object key) {
		String nama = key == null ? "" : key.toString().toLowerCase();
		return nama.indexOf("aktif") >= 0 || nama.indexOf("valid") >= 0 || nama.indexOf("lulus") >= 0
				|| nama.indexOf("checked") >= 0 || nama.indexOf("terpilih") >= 0
				|| nama.startsWith("is_") || nama.startsWith("is")
				|| nama.startsWith("ada_") || nama.startsWith("ada");
	}

	private static boolean namaParameterAngkaBulat(Object key) {
		String nama = key == null ? "" : key.toString().toLowerCase();
		return nama.indexOf("semester") >= 0 || nama.indexOf("smt") >= 0
				|| nama.indexOf("sks") >= 0 || nama.indexOf("tahun") >= 0
				|| nama.indexOf("bulan") >= 0 || nama.indexOf("hari") >= 0
				|| nama.indexOf("urut") >= 0 || nama.indexOf("ke_") >= 0
				|| nama.equals("no") || nama.equals("nomor");
	}

	private static boolean namaParameterAngkaPecahan(Object key) {
		String nama = key == null ? "" : key.toString().toLowerCase();
		return nama.indexOf("nilai") >= 0 || nama.indexOf("bobot") >= 0 || nama.indexOf("mutu") >= 0
				|| nama.indexOf("ipk") >= 0 || nama.indexOf("ips") >= 0 || nama.indexOf("ip_") >= 0
				|| nama.indexOf("total") >= 0 || nama.indexOf("jumlah") >= 0 || nama.indexOf("jml") >= 0
				|| nama.indexOf("tagihan") >= 0 || nama.indexOf("dibayar") >= 0 || nama.indexOf("bayar") >= 0
				|| nama.indexOf("nominal") >= 0 || nama.indexOf("biaya") >= 0 || nama.indexOf("harga") >= 0
				|| nama.indexOf("saldo") >= 0 || nama.indexOf("debet") >= 0 || nama.indexOf("kredit") >= 0
				|| nama.indexOf("persen") >= 0 || nama.indexOf("prosentase") >= 0;
	}

	/**
	 * Helper untuk memusatkan penyimpanan History ke Database dengan try-finally
	 * yang aman.
	 */
	private static void saveReportHistory(String bar, String formatLaporan, String fileName) {
		Session session = null;
		try {
			session = StreamingHibernateUtil.getInstance().currentSession();
			session.getTransaction().begin();
			ReportHistory reportHistory = new ReportHistory();
			reportHistory.setFile(null);
			reportHistory.setBarcode(bar);
			reportHistory.setKeterangan(formatLaporan);
			reportHistory.setNama(fileName);
			session.save(reportHistory);
			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/Report.java:1307");
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// SANGAT PENTING: Mencegah Session Leak
			try {
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1314");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}
	}

	/**
	 * Menyusun pesan kegagalan laporan yang lebih informatif. Bila akar masalah adalah berkas
	 * template tidak ditemukan (mis. file {@code .jasper} belum ter-deploy ke server), pesan akan
	 * menyebutkan nama berkas yang hilang agar admin tahu persis apa yang perlu disediakan. Bila
	 * akar masalah adalah gambar yang rusak/bukan format gambar yang dikenali (mis. logo, barcode,
	 * foto, atau tanda tangan yang korup/kosong) -- termasuk saat percobaan ulang dengan gambar
	 * dikosongkan (lihat {@link #kosongkanParameterGambarTidakValid(Map)}) tetap gagal -- pesan
	 * juga dibuat spesifik. Selain itu dipakai pesan umum. Penelusuran rantai sebab dibatasi agar aman.
	 *
	 * @param e exception penyebab kegagalan (boleh null)
	 * @return pesan untuk {@link ReportGenerationException}
	 */
	private static String pesanErrorLaporan(Throwable e) {
		Throwable t = e;
		int guard = 0;
		while (t != null && guard < 30) {
			if (t instanceof java.io.FileNotFoundException) {
				String berkas = t.getMessage() == null ? "" : t.getMessage().trim();
				// Cari apakah .jrxml pendamping tersedia (auto-compile sudah dicoba sebelumnya)
				String infoJrxml = "";
				if (berkas.toLowerCase(java.util.Locale.ENGLISH).endsWith(".jasper")) {
					String jrxmlPath = berkas.substring(0, berkas.length() - ".jasper".length()) + ".jrxml";
					boolean jrxmlAda = new java.io.File(jrxmlPath).exists();
					infoJrxml = jrxmlAda
							? " File sumber kompilasi (" + jrxmlPath + ") tersedia namun kompilasi otomatis gagal."
							: " File sumber kompilasi (" + jrxmlPath + ") juga tidak ditemukan di server.";
				}
				return "Laporan belum dapat dibuat karena berkas template tidak ditemukan"
						+ (berkas.isEmpty() ? "" : ": " + berkas) + "."
						+ infoJrxml
						+ " Langkah yang perlu dilakukan administrator:"
						+ " (1) Pastikan file .jasper atau .jrxml tersedia di folder report server;"
						+ " (2) Jika file .jrxml tersedia, sistem akan otomatis mengompilasinya menjadi .jasper"
						+ " (atau lakukan kompilasi manual menggunakan JasperReports);"
						+ " (3) Jika file template belum ada sama sekali, upload via menu Admin → Template Laporan"
						+ " atau hubungi pengembang sistem."
						+ " Mohon sertakan tangkapan layar (screenshot) pesan ini saat menghubungi administrator atau pengembang.";
			}
			t = t.getCause();
			guard++;
		}
		if (isImageFormatError(e)) {
			return "Laporan belum dapat dibuat karena salah satu gambar pada laporan (mis. logo, barcode, "
					+ "foto, atau tanda tangan) rusak/kosong atau bukan format gambar yang dikenali. "
					+ "Mohon hubungi administrator untuk memeriksa berkas gambar tersebut.";
		}
		// Pesan throttle antrean cetak (ReportThrottle) sudah ramah pengguna — teruskan apa adanya
		// agar pengguna tahu cukup mencoba ulang, bukan menganggap laporannya rusak.
		String pesanAsli = e == null ? null : e.getMessage();
		if (pesanAsli != null && (pesanAsli.contains("antrean cetak") || pesanAsli.contains("banyak laporan secara bersamaan"))) {
			return pesanAsli;
		}
		return "Laporan belum dapat dibuat. Klik Lihat Detail Error lalu Copy Error untuk Admin.";
	}

	/**
	 * Mencegah konfigurasi sebuah tombol laporan menunjuk ke jenis dokumen lain.
	 * Kasus nyata: Report_Cetak_KRS_Mahasiswa terisi Jadwal_UTS sehingga tombol
	 * KRS menampilkan jadwal ujian. Template KRS kustom tetap diperbolehkan selama
	 * nama filenya tidak jelas-jelas merupakan template kartu/jadwal ujian.
	 */
	private static boolean konfigurasiLaporanMenyilangJenis(String laporanDiminta, String laporanKonfigurasi) {
		if (laporanDiminta == null || laporanKonfigurasi == null) {
			return false;
		}
		String diminta = new File(laporanDiminta).getName().toLowerCase(Locale.ENGLISH);
		String konfigurasi = new File(laporanKonfigurasi).getName().toLowerCase(Locale.ENGLISH);
		if (diminta.endsWith(".jasper")) diminta = diminta.substring(0, diminta.length() - 7);
		if (diminta.endsWith(".jrxml")) diminta = diminta.substring(0, diminta.length() - 6);
		if (konfigurasi.endsWith(".jasper")) konfigurasi = konfigurasi.substring(0, konfigurasi.length() - 7);
		if (konfigurasi.endsWith(".jrxml")) konfigurasi = konfigurasi.substring(0, konfigurasi.length() - 6);

		return "cetak_krs_mahasiswa".equals(diminta)
				&& (konfigurasi.contains("jadwal_uts") || konfigurasi.contains("jadwal_uas")
						|| konfigurasi.contains("cetak_kuts") || konfigurasi.contains("cetak_kuas"));
	}

	/**
	 * CORE HELPER: Semua method generateFileReport diarahkan ke sini untuk
	 * efisiensi dan kerapian.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static File generateFileReportCore(String formatLaporan, Map parameters, String fileD, Date t, List maps,
			String bar, Locale locale) throws Exception {
		if (parameters == null) {
			// Kondisi normal untuk layar report yang belum lengkap filternya. Caller
			// yang butuh pesan pengguna menangani null ini di level UI.
			return null;
		}

		ProgressContext progress = getProgressContext(parameters);
		updateProgress(progress, 10, "Membaca konfigurasi laporan", "Menyiapkan template dan parameter dasar");

		String namaAsli = fileD;
		String fileDiminta = fileD;
		File myFile = null;

		try {
			if (parameters.get("nama_laporan") != null) {
				fileD = (String) parameters.get("nama_laporan");
				if (fileD.endsWith(".jrxml")) {
					File fileJrxml = new File(fileD);
					File fileJasper = new File(fileD.replaceAll(".jrxml", "") + ".jasper");
					if (fileJrxml.exists()) {
						try {
							// FIX RACE CONDITION: dikunci per-nama-file .jasper (pola sama dengan
							// fillJasperReportSekali/ambilLockJasper) supaya permintaan cetak PARALEL
							// yang menunjuk ke .jrxml/.jasper yang SAMA tidak saling tabrakan kompilasi.
							synchronized (ambilLockJasper(fileJasper)) {
								compileJasperAtomically(fileJrxml, fileJasper);
							}
							fileD = fileJasper.getAbsolutePath();
						} catch (Exception e) {
							if (isReportErrorLogConsoleEnabled()) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/Report.java:1373");
								}
						}
					}
				}
			}

			String fileSebelumnya = fileD;
			if (fileD != null && fileD.toLowerCase().endsWith(".jasper")) {
				File fileJasper = new File(fileD);
				namaAsli = StringUtils.replaceIgnoreCase(fileJasper.getName(), ".jasper", "");
			} else {
				String fileKonfigurasi = Common
						.getKonfigurasi("Report_" + fileD, Konfigurasi.AKTIF, fileD, "", "").getInfo1();
				if (konfigurasiLaporanMenyilangJenis(namaAsli, fileKonfigurasi)) {
					System.err.println("Konfigurasi laporan diabaikan karena menyilang jenis: Report_" + namaAsli
							+ " -> " + fileKonfigurasi);
					fileD = fileDiminta;
				} else {
					fileD = fileKonfigurasi;
				}
			}

			if (parameters.get("maps") != null)
				maps = (List) parameters.get("maps");
			if (parameters.get("bar") != null)
				bar = (String) parameters.get("bar");

			if (bar == null || bar.trim().length() == 0) {
				bar = Common.getGeneratedBarCode();
			}
			bar = URLEncoder.encode(bar, "UTF-8");

			// --- MODIFIKASI SHARED DIRECTORY ---
			String reportPath = Common.ambilREAL_PATH_REPORT();

			myFile = new File(reportPath + "/" + bar + "." + formatLaporan);
			CommonFileUtil.ensureWritableFile(myFile, 50L * 1024L * 1024L);
			myFile.createNewFile();

			File myfilebarcode = new File(reportPath + "/barcode_" + bar + ".png");
			CommonFileUtil.ensureWritableFile(myfilebarcode, 64L * 1024L);

			// FIX: Pencegahan NullPointerException dari Barbecue
			try {
				if (bar == null || bar.trim().isEmpty()) {
					myfilebarcode = new File(reportPath + "/putih.png");
				} else if (Common.bolehKonfigurasi("nggak_usah_pakai_barcode_di_report", Konfigurasi.TIDAK_AKTIF)) {
					myfilebarcode = new File(reportPath + "/putih.png");
				} else {
					Barcode mybarcode = BarcodeFactory.createCode128B(bar);
					BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
				}
				parameters.put("barcode", myfilebarcode.getAbsolutePath());
			} catch (Exception e) {
				parameters.put("barcode", reportPath + "/putih.png");
			}

			updateProgress(progress, 25, "Menyiapkan identitas laporan", "Mengisi logo, kop, bahasa, dan data pendukung");
			initDefaultParameter(parameters, locale);
			setlogo(parameters);
			parameters.put("REPORT_LOCALE", locale);

			File lastFileJasper = null;
			JasperPrint jasperPrint = null;

			// OPTIMASI RAM FASE 6: (1) batas jumlah laporan paralel per JVM via semaphore —
			// sebelumnya tak terbatas sehingga N request cetak bersamaan = N JasperPrint penuh
			// di heap; (2) virtualizer swap-file agar halaman laporan besar tidak seluruhnya
			// ditahan di heap. Keduanya WAJIB dilepas di finally di bawah (virtualizer baru
			// boleh cleanup SETELAH export karena halaman dibaca ulang dari swap saat export).
			boolean izinReportDiperoleh = ReportThrottle.ambilIzin();
			net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer virtualizerReport = ReportThrottle
					.pasangVirtualizer(parameters);
			try {

			try {
				updateProgress(progress, 50, "Membaca template Jasper", "Membuka file desain laporan");
				File fileJasper = CommonReport.generateFileJasper(fileD, namaAsli);
				lastFileJasper = fileJasper;
				updateProgress(progress, 70, "Mengisi data laporan", "Menggabungkan data dengan template laporan");
				jasperPrint = fillJasperReport(fileJasper, parameters, maps);
			} catch (Exception e) {
					if (isReportErrorLogConsoleEnabled()) e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/Report.java:1436");
				updateProgress(progress, 55, "Membaca template cadangan", "Template utama gagal, mencoba template laporan cadangan");
				File fileJasper = CommonReport.generateFileJasper(fileSebelumnya, namaAsli);
				lastFileJasper = fileJasper;
				updateProgress(progress, 72, "Mengisi data laporan", "Menggabungkan data dengan template cadangan");
				jasperPrint = fillJasperReport(fileJasper, parameters, maps);
			}

			updateProgress(progress, 88, "Membentuk file " + formatLaporan, "Menyimpan hasil laporan ke file");
			try {
				exportJasperPrint(jasperPrint, formatLaporan, myFile);
			} catch (Exception exportEx) {
				// Jika gagal karena gambar tidak valid (foto/logo/ttd/barcode korup, atau URL
				// eksternal mengembalikan bukan gambar), laporan HARUS tetap terbit dengan
				// gambar bermasalah dikosongkan -- bukan gagal total.
				if (!isImageFormatError(exportEx)) {
					throw exportEx;
				}
				if (isReportErrorLogConsoleEnabled()) exportEx.printStackTrace(); ais.common.ErrorAuditUtil.record(exportEx, "auto-audit src/ais/action/report/Report.java:1452");

				boolean berhasilTanpaGambarRusak = false;

				// TAHAP 1 (baru): bereskan langsung di hasil isi/fill. Ini menjangkau gambar dari
				// DATA/blob (mis. foto mahasiswa pada Kartu Mahasiswa) yang TIDAK pernah lewat
				// parameter, sekaligus mengaktifkan mekanisme bawaan JasperReports
				// (onErrorType=BLANK) agar gambar rusak dilewati saat ekspor.
				if (jasperPrint != null && bersihkanGambarRusakDiJasperPrint(jasperPrint)) {
					try {
						exportJasperPrint(jasperPrint, formatLaporan, myFile);
						berhasilTanpaGambarRusak = true;
					} catch (Exception exportEx2) {
						if (!isImageFormatError(exportEx2)) {
							throw exportEx2;
						}
						ais.common.ErrorAuditUtil.record(exportEx2,
								"auto-audit(ekspor ulang setelah gambar rusak dikosongkan tetap gagal) src/ais/action/report/Report.java");
					}
				}

				// TAHAP 2 (perilaku lama, tetap dipertahankan): kosongkan PARAMETER gambar yang
				// tidak valid lalu isi ulang laporan dari template.
				if (!berhasilTanpaGambarRusak) {
					if (lastFileJasper != null && kosongkanParameterGambarTidakValid(parameters)) {
						JasperPrint jasperPrintRetry = fillJasperReport(lastFileJasper, parameters, maps);
						bersihkanGambarRusakDiJasperPrint(jasperPrintRetry);
						exportJasperPrint(jasperPrintRetry, formatLaporan, myFile);
					} else {
						throw exportEx;
					}
				}
			}
			updateProgress(progress, 96, "Menyimpan riwayat laporan", "Mencatat riwayat file laporan yang dibuat");
			saveReportHistory(bar, formatLaporan, myFile.getName());

			} finally {
				ReportThrottle.bersihkanVirtualizer(virtualizerReport, parameters);
				ReportThrottle.lepasIzin(izinReportDiperoleh);
			}

		} catch (Exception e) {
				if (isReportErrorLogConsoleEnabled()) e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/Report.java:1463");
				try { if (myFile != null && myFile.exists() && myFile.length() == 0) myFile.delete(); } catch (Exception deleteError) { ais.common.ErrorAuditUtil.record(deleteError, "auto-audit(empty-catch) src/ais/action/report/Report.java:1464"); }
				File detailFile = createReportErrorDetailFile(formatLaporan, fileD, namaAsli, e, parameters);
				throw new ReportGenerationException(pesanErrorLaporan(e), e, detailFile);
			}
		if (myFile != null) {
			System.out.println("cetak di -> " + myFile.getAbsolutePath() + ", ada -> " + myFile.exists());
		} else {
			System.out.println("cetak gagal -> " + namaAsli);
		}
		return myFile;
	}

	/*
	 * =============================================================================
	 * = PUBLIC METHODS
	 * =============================================================================
	 * =
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void initDefaultParameter(Map parameters, Locale locale) throws Exception {
		// Logika ini sama dengan sebelumnya karena sudah cukup efisien.
		System.out.println("initDefaultParameter I -> " + parameters.size());

		Tbmuser tbmuser = null;
		try {
			tbmuser = Common.getCurrentUser();
			// FIX NPE rutin: getCurrentUser() balik null pada konteks TANPA sesi
			// login (mis. laporan VA dipicu callback webhook bank di
			// VirtualAccountBank.bayarSiswa) -- kondisi NORMAL utk jalur itu,
			// bukan langka. Tanpa guard, tbmuser.hakAkses() NPE & MEMBATALKAN
			// baris 1492-1495 (username/nama/satker) sebelum sempat jalan.
			if (tbmuser != null) {
				parameters.put("JENIS_PENGGUNA_SISTEM",
						tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleId());
				parameters.put("USERNAME_PENGGUNA_SISTEM", tbmuser.getUserId());
				parameters.put("NAMA_PENGGUNA_SISTEM", tbmuser.getUserNama());
				if (tbmuser.getSatuanKerja() != null)
					SuratUtil.initSatker(tbmuser.getSatuanKerja(), parameters);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1496");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Report", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		if (parameters.containsKey("currentLang")) {
			String currentLang = null;
			try {
				// FIX NPE: Sessions.getCurrent(true) BUKAN sekadar mengembalikan null bila
				// dipanggil TANPA Execution ZK aktif -- implementasinya mengakses Execution
				// milik thread saat ini secara internal, sehingga bisa NPE (pola sama seperti
				// Common.currentLang(), lihat [[bahasa-jsp-murni-currentlang-httpsession]]).
				// generateDownloadReport dipicu dari JSP MURNI (mis. _cetak_bukti_diterima.jsp
				// PMB) yang TIDAK punya Execution ZK sama sekali -- cek Executions.getCurrent()
				// != null lebih dulu supaya Sessions.getCurrent(true) hanya dipanggil ketika
				// benar-benar ada Execution; kalau tidak ada, langsung fallback ke bahasa
				// pengguna/default di bawah tanpa perlu lempar-tangkap exception.
				if (org.zkoss.zk.ui.Executions.getCurrent() != null) {
					org.zkoss.zk.ui.Session currentSession = Sessions.getCurrent(true);
					if (currentSession != null) {
						currentLang = (String) currentSession.getAttribute("current_lang");
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1503");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
			if (currentLang == null)
				currentLang = tbmuser == null ? null : tbmuser.getBahasa();
			if (currentLang == null)
				currentLang = Tbmuser.INDONESIA;
			parameters.put("currentLang", currentLang);
		}

		try {
			if (locale == null)
				locale = Common.locale;

			for (Object o : ConstantValues.ambilBerdasarClass(SubReport.class).values()) {
				SubReport subReport = (SubReport) o;
				if (subReport.getAktif()) {
					LampiranLain lampiranLain = LampiranLain.ambil(subReport.getId(), SubReport.class.getName());
					try {
						if (lampiranLain != null && lampiranLain.getNama().toLowerCase().endsWith(".jasper")) {
							parameters.put(subReport.getKode(), lampiranLain.ambilFile().getAbsolutePath());
						} else if (lampiranLain != null && lampiranLain.getNama().toLowerCase().endsWith(".jrxml")) {
							File fileJrxml = lampiranLain.ambilFile();
							File fileJasper = new File(
									fileJrxml.getAbsolutePath().replaceAll(".jrxml", "") + ".jasper");
							if (!fileJasper.exists()) {
								// FIX RACE CONDITION (akar NoClassDefFoundError "wrong name" pada
								// subreport bersama, mis. Rekaman_Nilai_Kelompok_type_2_subreport1 dipakai
								// Transkrip_Akademik): subreport ini DIPAKAI BERSAMA oleh banyak laporan,
								// dan method ini dipanggil di AWAL SETIAP generateFileReportCore -- kalau
								// beberapa permintaan cetak PARALEL sama-sama menemui fileJasper belum ada
								// (cache dingin), semuanya lolos cek exists() di atas dan tanpa lock di
								// bawah ini akan sama-sama menulis file .jasper TARGET yang SAMA secara
								// bersamaan. Dikunci per-nama-file .jasper (pola sama dengan
								// fillJasperReportSekali/ambilLockJasper), dengan cek ulang exists() di
								// dalam lock supaya thread yang menunggu tidak lagi mengompilasi ulang
								// begitu thread pertama selesai.
								synchronized (ambilLockJasper(fileJasper)) {
									if (!fileJasper.exists()) {
										fileJasper.getParentFile().mkdirs();
										compileJasperAtomically(fileJrxml, fileJasper);
									}
								}
							}
							parameters.put(subReport.getKode(), fileJasper.getAbsolutePath());
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1534");
						PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Report", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}
				}
			}

			for (Object o : ConstantValues.ambilBerdasarClass(PendukungReport.class).values()) {
				PendukungReport pendukungReport = (PendukungReport) o;
				if (pendukungReport.getAktif()) {
					LampiranLain lampiranLain = LampiranLain.ambil(pendukungReport.getId(),
							PendukungReport.class.getName());
					if (lampiranLain != null)
						parameters.put(pendukungReport.getKode(), lampiranLain.ambilFile().getAbsolutePath());
				}
			}

			Map properties = new HashMap();
			for (Object o : MemoryDbUtil.getKonfigurasi().values()) {
				Konfigurasi konfigurasi = (Konfigurasi) o;
				String nama = konfigurasi.getNama() == null ? "" : konfigurasi.getNama().toLowerCase().trim();
				if (!nama.isEmpty() && (nama.startsWith("label_") || tetapDimasukkan.contains(nama))
						&& konfigurasi.getNilai() != null && !konfigurasi.getNilai().trim().isEmpty()) {
					properties.put(nama, konfigurasi.getNilai());
					parameters.put(nama, konfigurasi.getNilai());
				}
			}

			parameters.put("REPORT_LOCALE", locale);
			parameters.put("REPORT_PARAMETERS_MAP", properties);
			parameters.put("current_ta", Common.getCurrentTahunAkademik());
			parameters.put("current_semester", Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

			if (Common.bolehKonfigurasi("menggunakan_lokal_url_di_report", Konfigurasi.TIDAK_AKTIF)) {
				String local = Common.getKonfigurasi("CURRENT_LOCAL_URL", "http://localhost/ecampus").getNilai();
				parameters.put("current_url", local);
				parameters.put("current_local_url", local);
				parameters.put("CURRENT_URL", local);
			} else {
				parameters.put("current_url", Common.getRequestHostWithProtocol());
				parameters.put("current_local_url", Common.getRequestHostWithProtocol());
				parameters.put("CURRENT_URL", Common.getRequestHostWithProtocol());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

		try {
			SuratUtil.initDefaultKop(parameters, tbmuser);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1581");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Report", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		try {
			Object d = parameters.get("SUBREPORT_DIR");
			if (d == null || d.toString().toLowerCase().contains("fauzi")) {
				parameters.put("SUBREPORT_DIR", Common.ambilREAL_PATH_REPORT() + "/");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1588");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		System.out.println("initDefaultParameter II -> " + parameters.size());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void setlogo(Map parameters) {
		try {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				parameters.put("label_universitas", perguruanTinggi.getNama());
				parameters.put("label_instansi", perguruanTinggi.getNama());
				parameters.put("alamat_kampus", perguruanTinggi.getAlamat1() + " " + perguruanTinggi.getAlamat2());
				parameters.put("alamat_instansi", perguruanTinggi.getAlamat1() + " " + perguruanTinggi.getAlamat2());
				parameters.put("label_telp_kampus", perguruanTinggi.getTelepon());
				parameters.put("label_email_kampus", perguruanTinggi.getEmail());
				parameters.put("label_website_kampus", perguruanTinggi.getWebsite());

				LampiranLain logo = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.LOGO_PT);
				if (logo != null && logo.ambilFile() != null) {
					parameters.put("logo", logo.ambilFile().getAbsolutePath());
				} else {
					parameters.put("logo", Common.REAL_PATH + "/img/logo.png");
				}
			} else {
				parameters.put("logo", Common.REAL_PATH + "/img/logo.png");
			}
		} catch (Exception e) {
			parameters.put("logo", Common.REAL_PATH + "/img/logo.png");
		}
	}



	@SuppressWarnings("rawtypes")
	public static File generateFileReportWithProgress(final String file, final String formatLaporan,
			final Map parameters, final String string, final Date date, final WebApp webApp) throws Exception {
		return generateFileReportWithProgressInternal(formatLaporan, parameters, file, new FileReportExecutor() {
			public File execute() throws Exception {
				return Report.generateFileReport(file, formatLaporan, parameters, string, date, webApp);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	public static File generateFileReportWithProgress(final String formatLaporan, final Map parameters,
			final String file, final Date t, final List maps, final Toolbar toolbar) throws Exception {
		return generateFileReportWithProgressInternal(formatLaporan, parameters, file, new FileReportExecutor() {
			public File execute() throws Exception {
				return Report.generateFileReport(formatLaporan, parameters, file, t, maps, toolbar);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	public static File generateFileReportWithProgress(final String formatLaporan, final Map parameters,
			final String fileD, final Date t, final List maps, final String bar, final Locale locale) throws Exception {
		return generateFileReportWithProgressInternal(formatLaporan, parameters, fileD, new FileReportExecutor() {
			public File execute() throws Exception {
				return Report.generateFileReport(formatLaporan, parameters, fileD, t, maps, bar, locale);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	public static File generateFileReportWithProgress(final String formatLaporan, final String file,
			final Map parameters, final String a, final Date t) throws Exception {
		return generateFileReportWithProgressInternal(formatLaporan, parameters, file, new FileReportExecutor() {
			public File execute() throws Exception {
				return Report.generateFileReport(formatLaporan, file, parameters, a, t);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	public static File generateFileReportWithProgress(final String formatLaporan, final Map parameters,
			final String file, final Date t, final Toolbar toolbar) throws Exception {
		return generateFileReportWithProgressInternal(formatLaporan, parameters, file, new FileReportExecutor() {
			public File execute() throws Exception {
				return Report.generateFileReport(formatLaporan, parameters, file, t, toolbar);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	public static File generateFileReportWithProgress(final String formatLaporan, final Map parameters,
			final String file, final Date t, final List maps, final Locale locale) throws Exception {
		return generateFileReportWithProgressInternal(formatLaporan, parameters, file, new FileReportExecutor() {
			public File execute() throws Exception {
				return Report.generateFileReport(formatLaporan, parameters, file, t, maps, locale);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	public static File generateFileReportWithProgress(final String formatLaporan, final Map parameters,
			final String fileD, final Date t, final String bar, final Locale locale) throws Exception {
		return generateFileReportWithProgressInternal(formatLaporan, parameters, fileD, new FileReportExecutor() {
			public File execute() throws Exception {
				return Report.generateFileReport(formatLaporan, parameters, fileD, t, bar, locale);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	public static File generateFileReportWithProgress(final String formatLaporan, final Map parameters,
			final String fileD) throws Exception {
		return generateFileReportWithProgressInternal(formatLaporan, parameters, fileD, new FileReportExecutor() {
			public File execute() throws Exception {
				return Report.generateFileReport(formatLaporan, parameters, fileD);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	public static File generateFileReportWithProgress(final String formatLaporan, final Map parameters,
			final String fileD, final Date t, final Locale locale) throws Exception {
		return generateFileReportWithProgressInternal(formatLaporan, parameters, fileD, new FileReportExecutor() {
			public File execute() throws Exception {
				return Report.generateFileReport(formatLaporan, parameters, fileD, t, locale);
			}
		});
	}

	/*
	 * =============================================================================
	 * = OVERLOAD METHODS (Sangat Rapi & Dialihkan ke Core Helper)
	 * =============================================================================
	 * =
	 */

	@SuppressWarnings("rawtypes")
	public static File generateFileReport(String file, String formatLaporan, Map parameters, String string, Date date,
			WebApp webApp) throws Exception {
		return generateFileReportCore(formatLaporan, parameters, file, date, null, Common.getGeneratedBarCode(),
				Common.locale);
	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateFileReport(String formatLaporan, Map parameters, String file, Date t, List maps,
			Toolbar toolbar) throws Exception {
		Radiogroup localeCombobox = (Radiogroup) (toolbar == null ? null : toolbar.getFirstChild());
		Locale locale = (Locale) (localeCombobox == null ? Common.locale
				: (localeCombobox.getSelectedItem() == null
						|| localeCombobox.getSelectedItem().getAttribute("val") == null ? Common.locale
								: localeCombobox.getSelectedItem().getAttribute("val")));
		return generateFileReportCore(formatLaporan, parameters, file, t, maps, Common.getGeneratedBarCode(), locale);
	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateFileReport(String formatLaporan, Map parameters, String fileD, Date t, List maps,
			String bar, Locale locale) throws Exception {
		return generateFileReportCore(formatLaporan, parameters, fileD, t, maps, bar, locale);
	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateFileReport(String formatLaporan, String file, Map parameters, String a, Date t)
			throws Exception {
		return generateFileReportCore(formatLaporan, parameters, file, t, null, Common.getGeneratedBarCode(),
				Common.locale);
	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateFileReport(String formatLaporan, Map parameters, String file, Date t, Toolbar toolbar)
			throws Exception {
		Component component = (Component) (toolbar == null ? null : toolbar.getFirstChild());
		Locale locale = Common.locale;
		if (component instanceof Radiogroup) {
			Radiogroup localeCombobox = (Radiogroup) (toolbar == null ? null : toolbar.getFirstChild());
			locale = (Locale) (localeCombobox == null || localeCombobox.getSelectedItem() == null
					|| localeCombobox.getSelectedItem().getAttribute("val") == null ? Common.locale
							: localeCombobox.getSelectedItem().getAttribute("val"));
		}
		return generateFileReportCore(formatLaporan, parameters, file, t, null, Common.getGeneratedBarCode(), locale);
	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateFileReport(String formatLaporan, Map parameters, String file, Date t, List maps,
			Locale locale) throws Exception {
		return generateFileReportCore(formatLaporan, parameters, file, t, maps, Common.getGeneratedBarCode(), locale);
	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateFileReport(String formatLaporan, Map parameters, String fileD, Date t, String bar,
			Locale locale) throws Exception {
		return generateFileReportCore(formatLaporan, parameters, fileD, t, null, bar, locale);
	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateFileReportSimple(String formatLaporan, Map parameters, String fileD) throws Exception {
		return generateFileReportCore(formatLaporan, parameters, fileD, new Date(), null, Common.getGeneratedBarCode(),
				Common.locale);
	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateFileReport(String formatLaporan, Map parameters, String fileD) throws Exception {
		return generateFileReportCore(formatLaporan, parameters, fileD, new Date(), null, Common.getGeneratedBarCode(),
				Common.locale);
	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateFileReport(String formatLaporan, Map parameters, String fileD, Date t, Locale locale)
			throws Exception {
		return generateFileReportCore(formatLaporan, parameters, fileD, t, null, Common.getGeneratedBarCode(), locale);
	}

	/*
	 * =============================================================================
	 * = OTHER CORE METHODS
	 * =============================================================================
	 * =
	 */

	@SuppressWarnings({ "rawtypes" })
	public static File generateDownloadReport(String formatLaporan, Map parameters, String file, List map, Date t)
			throws Exception {
		return generateDownloadReport(formatLaporan, parameters, file, map, t, Common.locale, true);
	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateDownloadReport(String formatLaporan, Map parameters, String file, List map, Date t,
			Locale locale) throws Exception {
		return generateDownloadReport(formatLaporan, parameters, file, map, t, locale, true);
	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateDownloadReport(String formatLaporan, Map parameters, String fileD, List map, Date t,
			Locale locale, boolean download) throws Exception {
		String namaAsli = fileD;
		if (fileD != null && fileD.toLowerCase().endsWith(".jasper")) {
			namaAsli = StringUtils.replaceIgnoreCase(new File(fileD).getName(), ".jasper", "");
		}

		// Tandai mode UNDUH agar pendamping HTML pratinjau tidak dibuat (hemat waktu unduhan).
		File myFile;
		MODE_UNDUH.set(Boolean.TRUE);
		try {
			myFile = generateFileReportCore(formatLaporan, parameters, fileD, t, map, Common.getGeneratedBarCode(),
					locale);
		} finally {
			MODE_UNDUH.remove();
		}

		if (download) {
			// FIX NPE: Filedownload.save() melempar NullPointerException kalau file hasil
			// generate laporan null/tidak ada (generateFileReportCore gagal secara senyap,
			// mis. parameters==null, atau file tidak berhasil ditulis). Jangan lanjut ke
			// Filedownload.save pada kondisi ini -- lempar ReportGenerationException supaya
			// caller menampilkan pesan error yang konsisten ke user, bukan NPE mentah.
			if (myFile == null || !myFile.exists() || myFile.length() <= 0L) {
				// Diagnostik (tanpa ubah logika): sebelumnya pesan ini tidak menyertakan
				// konteks apapun sehingga sulit ditelusuri laporan/jrxml mana yang gagal
				// dan kenapa. Sertakan nama laporan yang diminta, format, dan path/kondisi
				// berkas yang dicek supaya admin bisa langsung menelusuri.
				String kondisiBerkas = myFile == null
						? "berkas hasil tidak pernah dibuat (kemungkinan parameter laporan kosong/null atau template tidak ditemukan -- lihat auto-audit generateFileReportCore)"
						: (!myFile.exists() ? "berkas hasil tidak ditemukan di path yang dicek"
								: "berkas hasil ditemukan tapi kosong (0 byte)");
				String pathDicek = myFile != null ? myFile.getAbsolutePath() : "(tidak diketahui -- berkas belum sempat dibuat)";
				throw new ReportGenerationException(
						"Laporan gagal dibuat. Berkas hasil laporan tidak ditemukan. Laporan diminta=" + fileD
								+ (namaAsli != null && !namaAsli.equals(fileD) ? " (" + namaAsli + ")" : "")
								+ "; format=" + formatLaporan + "; kondisi=" + kondisiBerkas + "; path dicek="
								+ pathDicek + ".",
						null, null);
			}

			String currentDateTime = t == null ? "" : new SimpleDateFormat("dd_MMMMM_yyyy", Common.locale).format(t);

			FileInputStream fis = null;
			try {
				fis = new FileInputStream(myFile);
				AMedia amedia = new AMedia(
						Common.getBahasaConfig(namaAsli) + "_" + currentDateTime + "." + formatLaporan, formatLaporan,
						"application/" + formatLaporan, fis);
				Filedownload.save(amedia);
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1842");
				// FIX NPE (ERROR 25/26 / id 40 & 39): org.zkoss.zul.Filedownload.save() melempar
				// NullPointerException mentah kalau tidak ada Execution/Desktop ZK aktif pada
				// thread saat dipanggil -- mis. dipicu dari CommonReport$6$1.onEvent lewat
				// CommonTimerHelper (tombol cetak async), yang bisa berjalan di thread berbeda
				// dari thread request ZK semula. Jangan biarkan NPE ini bocor mentah ke caller --
				// siapkan berkas detail error (sama seperti jalur error generateFileReportCore)
				// supaya pesan "Detail error sudah disiapkan untuk admin" benar-benar akurat.
				File detailFile = createReportErrorDetailFile(formatLaporan, fileD, namaAsli, e, parameters);
				throw new ReportGenerationException(pesanErrorLaporan(e), e, detailFile);
			} finally {
				// fis.close();
				// Tidak menutup stream AMedia karena akan ditutup oleh ZK, cukup aman jika
				// ditinggal
			}
		}
		return myFile;
	}

//	@SuppressWarnings({})
//	public static void displayCompileFileReport(String formatLaporan, Map<String, Object> parameters, String file,
//			Date t, File... filesLain) throws Exception {
//		displayCompileFileReport(formatLaporan, parameters, file, t, true, filesLain);
//	}

//	@SuppressWarnings({})
//	public static void displayCompileFileReport(String formatLaporan, Map<String, Object> parameters, String file,
//			Date t, boolean bawah, File... filesLain) throws Exception {
//		File myfile = generateCompileFileReport(formatLaporan, parameters, file, t);
//
//		if (myfile != null && myfile.exists()) {
//			MyWindow window = new MyWindow("Laporan", "none", true);
//			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
//			window.setHeight("90%");
//			window.setWidth("900px");
//
//			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
//			borderlayout.setParent(window);
//			Center center = new Center();
//			ais.ui.util.ZkCompat.setFlex(center, true);
//			center.setParent(borderlayout);
//
//			if (filesLain != null && filesLain.length > 0) {
//				File filePdfBaru = new File(
//						myfile.getParentFile().getAbsolutePath() + "/" + Common.getGeneratedBarCode() + ".pdf");
//				PDFMergerUtility ut = new PDFMergerUtility();
//
//				if (!bawah)
//					for (File file2 : filesLain)
//						ut.addSource(file2);
//				ut.addSource(myfile);
//				if (bawah)
//					for (File file2 : filesLain)
//						ut.addSource(file2);
//
//				FileOutputStream fos = null;
//				try {
//					fos = new FileOutputStream(filePdfBaru);
//					ut.setDestinationStream(fos);
//					ut.mergeDocuments();
//				} finally {
//					// AMAN: Tutup output PDF Merge Stream
//					if (fos != null) {
//						try {
//							fos.close();
//						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1899");
//						}
//					}
//				}
//				Report.tampil(filePdfBaru, center);
//			} else {
//				Report.tampil(myfile, center);
//			}
//			window.setVisible(true);
//			window.onModal();
//		}
//	}

	@SuppressWarnings({ "rawtypes" })
	public static File generateCompileFileReport(String formatLaporan, Map<String, Object> parameters, String fileD,
			Date t, boolean connection) throws Exception {
		// Logika compile khusus ini dialihkan juga ke helper yang aman secara memori
		List maps = null;
		if (parameters != null && parameters.get("maps") != null)
			maps = (List) parameters.get("maps");

//		boolean useConnection = connection;
//		if (parameters != null && parameters.containsKey("tidak_usah_pakai_connection")) useConnection = false;

		return generateFileReportCore(formatLaporan, parameters, fileD, t, maps, Common.getGeneratedBarCode(),
				Common.locale);
	}

	@SuppressWarnings({})
	public static File generateCompileFileReport(String formatLaporan, Map<String, Object> parameters, String file,
			Date t) throws Exception {
		return generateCompileFileReport(formatLaporan, parameters, file, t, true);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static File generateFileImageReport(String formatLaporan, Map parameters, String fileD, Date t,
			Locale locale) throws Exception {
		// Generate Base Report
		File myFile = generateFileReportCore("pdf", parameters, fileD, t, null, Common.getGeneratedBarCode(), locale);
		if (myFile == null)
			return null;

		File finalJpg = new File(myFile.getAbsolutePath().replace(".pdf", ".jpg"));
		FileOutputStream out = null;
		Connection conn = null;
		// OPTIMASI RAM FASE 6: jalur render-gambar juga dihitung sebagai satu job laporan
		// (fill kedua + hingga 100 BufferedImage halaman penuh di heap).
		boolean izinReportDiperoleh = ReportThrottle.ambilIzin();
		Session session = openNativeSession();
		try {
			File fileJasper = CommonReport.generateFileJasper(fileD, fileD);

			conn = session.connection();
			parameters.put("REPORT_CONNECTION", conn);
			JasperPrint jasperPrint = JasperFillManager.fillReport(fileJasper.getAbsolutePath(), parameters, conn);

			List<BufferedImage> bufferedImages = new ArrayList<BufferedImage>();
			for (int i = 0; i < 100; i++) {
				try {
					BufferedImage pageImage = new BufferedImage(jasperPrint.getPageWidth() + 1,
							jasperPrint.getPageHeight() + 1, BufferedImage.TYPE_INT_RGB);
					JRGraphics2DExporter exporter = new JRGraphics2DExporter();
					exporter.setParameter(JRGraphics2DExporterParameter.GRAPHICS_2D, pageImage.getGraphics());
					exporter.setParameter(JRExporterParameter.PAGE_INDEX, i);
					exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
					exporter.exportReport();
					bufferedImages.add(pageImage);
				} catch (Exception e) {
					break;
				}
			}

			if (!bufferedImages.isEmpty()) {
				int rows = bufferedImages.size();
				int chunkWidth = bufferedImages.get(0).getWidth();
				int chunkHeight = bufferedImages.get(0).getHeight();

				BufferedImage finalImg = new BufferedImage(chunkWidth, chunkHeight * rows, BufferedImage.TYPE_INT_RGB);
				for (int i = 0; i < rows; i++) {
					finalImg.createGraphics().drawImage(bufferedImages.get(i), 0, chunkHeight * i, null);
				}

				out = new FileOutputStream(finalJpg);
				ImageIO.write(finalImg, "jpeg", out);
				bufferedImages.clear(); // Bersihkan referensi image
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e, "", false);
		} finally {
			// AMAN: Stream gambar & koneksi db ditutup
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1989");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:1995");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}

			closeNativeSession(session);
			parameters.remove("REPORT_CONNECTION");
			ReportThrottle.lepasIzin(izinReportDiperoleh);
		}
		return finalJpg;
	}

	public static void ConvertToPDF(String docPath, String pdfPath) {
		try {
			// Method asli dalam mode di-comment out, tetap biarkan namun disediakan finally
			// template.
			// InputStream doc = new FileInputStream(new File(docPath)); ...
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	public static File laporanHTML(String html, WebApp application) throws Exception {
		html = html == null ? "" : html;
		html = "<html><head></head><body>" + html + "</body></html>";
		File myFile = new File(application.getRealPath("/tmp/" + Common.getGeneratedBarCode() + ".html"));
		myFile.getParentFile().mkdirs();
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(myFile);
			fileWriter.write(html);
		} finally {
			// AMAN: fileWriter ditutup agar file HTML bisa dihapus OS (No Lock Leak)
			if (fileWriter != null) {
				try {
					fileWriter.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:2029");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}
		return myFile;
	}

	public static File cetakLaporanHTML(String html, WebApp application) throws Exception {
		html = html == null ? "" : html;
		html = "<html><head></head><body>" + html
				+ "<script type=\"text/javascript\">window.print();window.close();</script></body></html>";
		File myFile = new File(application.getRealPath("/tmp/" + Common.getGeneratedBarCode() + ".html"));
		myFile.getParentFile().mkdirs();
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(myFile);
			fileWriter.write(html);
		} finally {
			// AMAN: Tutup file write stream
			if (fileWriter != null) {
				try {
					fileWriter.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:2051");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}
		return myFile;
	}

	/*
	 * =============================================================================
	 * = UI RENDERING METHODS
	 * =============================================================================
	 * =
	 */

	@SuppressWarnings("rawtypes")
	public static File generateWindowReport(String formatLaporan, Map parameters, String file, Date t)
			throws Exception {
		return generatePDFReport(formatLaporan, parameters, file, t, null, Common.locale);
	}

	@SuppressWarnings("rawtypes")
	public static File generatePDFReport(String formatLaporan, Map parameters, String file, Date t, List maps)
			throws Exception {
		return generatePDFReport(formatLaporan, parameters, file, t, maps, Common.locale, null);
	}

	@SuppressWarnings("rawtypes")
	public static File generatePDFReport(String formatLaporan, Map parameters, String file, Date t, List maps,
			Locale locale) throws Exception {
		return generatePDFReport(formatLaporan, parameters, file, t, maps, locale, null);
	}

	@SuppressWarnings("rawtypes")
	public static File generatePDFReport(final String formatLaporan, final Map parameters, final String file,
			final Date t, final List maps, final Locale locale, Component parent) throws Exception {
		if (parameters == null)
			return null;

		File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters, file, t, maps,
				Common.getGeneratedBarCode(), locale);

		try {
			Component rootPage = null;
			if (ExecutionsCtrl.getCurrentCtrl() != null && ExecutionsCtrl.getCurrentCtrl().getCurrentPage() != null
					&& ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot() instanceof Component) {
				rootPage = (Component) ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
			}
			if (rootPage == null && parent == null) {
				// FIX NPE (ERROR 1 / id 115): ExecutionsCtrl.getCurrentCtrl()/getCurrentPage()
				// bisa null kalau method ini dipanggil dari konteks tanpa Execution/Page ZK
				// aktif -- mis. dari JSP murni pembayaran_online_mhs_services.jsp yang hanya
				// mengambil nilai balik File hasil laporan, bukan menampilkan popup. Baris
				// window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot())
				// sebelumnya melempar NPE mentah di sini. Karena berkas laporan sudah berhasil
				// dibuat di atas (myfile), lewati saja pembuatan window popup UI (tak ada
				// tempat menampilkannya) dan langsung kembalikan berkasnya ke caller.
				return myfile;
			}

			// FIX konten "Laporan" tampil KOSONG bila dipanggil dgn `parent` (mis. tab
			// "Kehadiran" di sub-tab Laporan): sebelumnya MyWindow "Laporan" TETAP dibuat &
			// di-parent ke `parent` walau kontennya (borderlayout/center) di-parent LANGSUNG
			// ke `parent` juga (bypass window) -- window kosong itu jadi anak-tak-terpakai
			// yang menumpuk/menutupi konten asli di dalam tabpanel. Hanya buat MyWindow saat
			// benar-benar dibutuhkan sbg popup modal (parent == null).
			MyWindow window = parent == null ? new MyWindow("Laporan", "none", true) : null;
			if (window != null) {
				window.setParent(rootPage);
				window.setHeight("90%");
				window.setWidth("900px");
			}

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setWidth("100%");
			borderlayout.setHeight("100%");
			borderlayout.setParent(parent == null ? window : parent);

			final Center center = new Center();
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.setAutoscroll(true);
			center.setParent(borderlayout);

			// Beri identitas jenis laporan agar pratinjau tahu apakah laporan ini di-default-kan PDF
			// (konfigurasi per-jenis-laporan) dan agar sakelar admin "Default PDF" mengenai laporan ini.
			Report.setReportKey(file);
			Report.tampil(myfile, center);

			if (parameters == null || parameters.get("tidak_tampil_pilihan_export") == null) {
				org.zkoss.zul.North north = new org.zkoss.zul.North();
				north.setParent(borderlayout);
				north.appendChild(CommonReport.exportReport(new ParameterListener() {
					@SuppressWarnings("unchecked")
					@Override
					public Map<String, Serializable> generateParameters() throws Exception {
						return parameters;
					}
				}, file, maps, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters, file, t, maps,
								Common.getGeneratedBarCode(), (Locale) arg0.getData());
						Report.setReportKey(file);
						Report.tampil(myfile, center);
					}
				}));
			}

			if (parent == null) {
				window.setVisible(true);
				window.onModal();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:2133");
			// TODO: handle exception
		}

		return myfile;
	}

	@SuppressWarnings({ "rawtypes" })
	public static EventListener generatePDFReport(String formatLaporan, Map parameters, String file, Date t)
			throws Exception {
		return generatePDFReport(formatLaporan, parameters, file, t, Common.locale);
	}

	@SuppressWarnings({ "rawtypes" })
	public static EventListener generatePDFReport(String formatLaporan, Map[] parameters, String[] file, String[] names,
			Date t) throws Exception {
		return generatePDFReport(formatLaporan, parameters, file, names, t, Common.locale);
	}

	@SuppressWarnings("rawtypes")
	public static EventListener generatePDFReport(final String formatLaporan, final Map[] parameters,
			final String[] file, final String[] names, final Date t, final Locale locale) throws Exception {
		if (parameters == null)
			return null;

		MyWindow window = new MyWindow("Laporan", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth("90%");

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(window));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tab1 = new MyTabConfig(names[0]);
		tab1.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		for (int i = 1; i < file.length; i++) {
			final int index = i;
			final Map par = parameters[i];
			MyTabConfig tab = new MyTabConfig(names[i]);
			tab.setParent(tabs);

			final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
			tabpanel2.setParent(tabpanels);
			tab.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel2.getChildren().size() == 0) {
						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(tabpanel2);
						final Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(borderlayout);

						EventListener eventListener = new EventListener() {
							@SuppressWarnings({ "unchecked" })
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (arg0 != null && arg0.getData() instanceof Map)
									par.putAll((Map) arg0.getData());
								File myfile = Report.generateFileReportWithProgress(formatLaporan, par, file[index].toString(),
										t, null, Common.getGeneratedBarCode(), locale);
								Report.tampil(myfile, center);
							}
						};

						eventListener.onEvent(null);

						if (par == null || par.get("tidak_tampil_pilihan_export") == null) {
							org.zkoss.zul.North north = new org.zkoss.zul.North();
							north.setParent(borderlayout);
							north.appendChild(CommonReport.exportReport(new ParameterListener() {
								@SuppressWarnings("unchecked")
								@Override
								public Map generateParameters() throws Exception {
									return par;
								}
							}, file[index].toString(), null, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									File myfile = Report.generateFileReportWithProgress(formatLaporan, par,
											file[index].toString(), t, null, Common.getGeneratedBarCode(),
											(Locale) arg0.getData());
									Report.tampil(myfile, center);
								}
							}));
						}
					}
				}
			});
		}

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1);
		final Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		EventListener eventListener = new EventListener() {
			@SuppressWarnings({ "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getData() instanceof Map)
					parameters[0].putAll((Map) arg0.getData());
				File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters[0], file[0].toString(), t, null,
						Common.getGeneratedBarCode(), locale);
				Report.tampil(myfile, center);
			}
		};

		eventListener.onEvent(null);

		if (parameters.length == 0 || parameters[0] == null
				|| parameters[0].get("tidak_tampil_pilihan_export") == null) {
			org.zkoss.zul.North north = new org.zkoss.zul.North();
			north.setParent(borderlayout);
			north.appendChild(CommonReport.exportReport(new ParameterListener() {
				@SuppressWarnings("unchecked")
				@Override
				public Map generateParameters() throws Exception {
					return parameters[0];
				}
			}, file[0].toString(), null, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters[0], file[0].toString(), t,
							null, Common.getGeneratedBarCode(), (Locale) arg0.getData());
					Report.tampil(myfile, center);
				}
			}));
		}

		window.setVisible(true);
		window.onModal();
		return eventListener;
	}

	@SuppressWarnings({ "rawtypes" })
	public static Tabbox generatePDFReportKembaliTab(String formatLaporan, Map[] parameters, String[] file,
			String[] names, Date t) throws Exception {
		return generatePDFReportKembaliTab(formatLaporan, parameters, file, names, t, Common.locale);
	}

	@SuppressWarnings("rawtypes")
	public static void tampil(File filePdfBaru, final Map par, final String... namaReport) throws Exception {
		// Guard: method ini menampilkan popup ZK (butuh Execution/Page/Desktop aktif).
		// Bila dipanggil dari alur tanpa konteks UI (mis. thread background dari JSP
		// _ikut_ujian_online_service.jsp), ExecutionsCtrl.getCurrentCtrl() bernilai null
		// sehingga .getCurrentPage() akan NPE. Dalam kondisi ini tidak ada Desktop untuk
		// ditempeli popup, jadi cukup lewati tampilan popup (file laporan sudah dibuat
		// dan proses lanjutan pemanggil, mis. kirim email, tetap berjalan).
		if (ExecutionsCtrl.getCurrentCtrl() == null || ExecutionsCtrl.getCurrentCtrl().getCurrentPage() == null) {
			ais.common.ErrorAuditUtil.record(
					new IllegalStateException("Report.tampil dipanggil tanpa konteks Execution/Page ZK aktif (mis. thread background) - popup laporan dilewati untuk file: "
							+ (filePdfBaru == null ? "null" : filePdfBaru.getAbsolutePath())),
					"Report.tampil: tidak ada konteks UI aktif, lewati tampilan popup");
			return;
		}

		MyWindow window = new MyWindow("Laporan", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth("900px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setParent(window);

		Center centerUtama = new Center();
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);
		// Tinggi PASTI (borderlayout 100% dalam window 90%) + autoscroll → konten min 2000px bisa
		// di-scroll di dalam popup (overflow:auto pada region butuh autoscroll, bukan sekadar style).
		centerUtama.setAutoscroll(true);
		centerUtama.setParent(borderlayout);

		if (namaReport == null || namaReport.length == 0) {
			Report.tampil(filePdfBaru, centerUtama);
		} else {
			Tabbox tabbox = new Tabbox();
			tabbox.setParent(centerUtama);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tab1 = new MyTabConfig("Laporan");
			tab1.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
			tabpanel1.setParent(tabpanels);

			Report.tampil(filePdfBaru, tabpanel1);

			for (int i = 0; i < namaReport.length; i++) {
				final int index = i;
				MyTabConfig tab = new MyTabConfig(namaReport[i]);
				tab.setParent(tabs);

				final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
				tabpanel2.setParent(tabpanels);

				tab.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanel2.getChildren().size() == 0) {

							Borderlayout borderlayoutTab = new ais.ui.util.MyBorderlayout();
							borderlayoutTab.setParent(tabpanel2);

							final Center centerTab = new Center();
							ais.ui.util.ZkCompat.setFlex(centerTab, true);
							centerTab.setParent(borderlayoutTab);

							EventListener eventListener = new EventListener() {
								@SuppressWarnings({ "unchecked" })
								@Override
								public void onEvent(Event arg0) throws Exception {
									if (arg0 != null && arg0.getData() instanceof Map) {
										Map maps = (Map) arg0.getData();
										par.putAll(maps);
									}

									// Menggunakan Core Helper yang sudah dioptimasi memorinya
									File myfile = Report.generateFileReportWithProgress(Report.PDF, par,
											namaReport[index].toString(), WaktuUtil.getDate(), null,
											Common.getGeneratedBarCode(), Common.locale);
									Report.tampil(myfile, centerTab);
								}
							};

							eventListener.onEvent(null);

							if (par == null || par.get("tidak_tampil_pilihan_export") == null) {
								org.zkoss.zul.North north = new org.zkoss.zul.North();
								north.setParent(borderlayoutTab);
								north.appendChild(CommonReport.exportReport(new ParameterListener() {
									@SuppressWarnings("unchecked")
									@Override
									public Map<String, Serializable> generateParameters() throws Exception {
										return par;
									}
								}, namaReport[index].toString(), null, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										// Menggunakan Core Helper yang sudah dioptimasi memorinya
										File myfile = Report.generateFileReportWithProgress(Report.PDF, par,
												namaReport[index].toString(), WaktuUtil.getDate(), null,
												Common.getGeneratedBarCode(), (Locale) arg0.getData());
										Report.tampil(myfile, centerTab);
									}
								}));
							}
						}
					}
				});
			}
		}

		window.setVisible(true);
		window.onModal();
	}

	/**
	 * Sama dengan {@link #tampil(File, Map, String...)} tanpa tab tambahan, namun dengan
	 * TOOLBAR TAMBAHAN di sisi atas (North) — dipakai layar pemanggil untuk menyisipkan
	 * tombol khusus (mis. Parameter/Download JRXML/Upload JRXML/History untuk admin di
	 * laporan disposisi SOP) tanpa mengubah viewer bawaan.
	 */
	@SuppressWarnings("rawtypes")
	public static void tampil(File filePdfBaru, final Map par, final org.zkoss.zul.Toolbar toolbarTambahan)
			throws Exception {
		if (toolbarTambahan == null) {
			tampil(filePdfBaru, par);
			return;
		}
		if (ExecutionsCtrl.getCurrentCtrl() == null || ExecutionsCtrl.getCurrentCtrl().getCurrentPage() == null) {
			ais.common.ErrorAuditUtil.record(
					new IllegalStateException("Report.tampil(toolbar) dipanggil tanpa konteks Execution/Page ZK aktif"),
					"Report.tampil: tidak ada konteks UI aktif, lewati tampilan popup");
			return;
		}

		MyWindow window = new MyWindow("Laporan", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth("900px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setParent(window);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbarTambahan);

		Center centerUtama = new Center();
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);
		centerUtama.setAutoscroll(true);
		centerUtama.setParent(borderlayout);

		Report.tampil(filePdfBaru, centerUtama);

		window.setVisible(true);
		window.onModal();
	}

	public static void tampil(final File myfile) throws Exception {
		MyWindow window = new MyWindow("Laporan", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		final Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setStyle("overflow:auto;");
		center.setParent(borderlayout);

		window.setVisible(true);
		window.onModal();
		// Delegasi ke chokepoint pratinjau (otomatis: toggle HTML/PDF, default HTML).
		Report.tampil(myfile, center);
	}

	@SuppressWarnings("rawtypes")
	public static Tabbox generatePDFReportKembaliTab(final String formatLaporan, final Map[] parameters,
			final String[] file, final String[] names, final Date t, final Locale locale) throws Exception {
		if (parameters == null)
			return null;

		MyWindow window = new MyWindow("Laporan", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth("90%");

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScrollTabbox(window));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		MyTabConfig tab1 = new MyTabConfig(names[0]);
		tab1.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
		tabpanel1.setParent(tabpanels);

		for (int i = 1; i < file.length; i++) {
			final int index = i;
			final Map par = parameters[i];
			MyTabConfig tab = new MyTabConfig(names[i]);
			tab.setParent(tabs);

			final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
			tabpanel2.setParent(tabpanels);
			tab.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel2.getChildren().size() == 0) {
						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(tabpanel2);
						final Center center = new Center();
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setParent(borderlayout);

						EventListener eventListener = new EventListener() {
							@SuppressWarnings({ "unchecked" })
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (arg0 != null && arg0.getData() instanceof Map)
									par.putAll((Map) arg0.getData());
								File myfile = Report.generateFileReportWithProgress(formatLaporan, par, file[index].toString(),
										t, null, Common.getGeneratedBarCode(), locale);
								Report.tampil(myfile, center);
							}
						};
						eventListener.onEvent(null);

						if (par == null || par.get("tidak_tampil_pilihan_export") == null) {
							org.zkoss.zul.North north = new org.zkoss.zul.North();
							north.setParent(borderlayout);
							north.appendChild(CommonReport.exportReport(new ParameterListener() {
								@SuppressWarnings("unchecked")
								@Override
								public Map generateParameters() throws Exception {
									return par;
								}
							}, file[index].toString(), null, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									File myfile = Report.generateFileReportWithProgress(formatLaporan, par,
											file[index].toString(), t, null, Common.getGeneratedBarCode(),
											(Locale) arg0.getData());
									Report.tampil(myfile, center);
								}
							}));
						}
					}
				}
			});
		}

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1);
		final Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		EventListener eventListener = new EventListener() {
			@SuppressWarnings({ "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getData() instanceof Map)
					parameters[0].putAll((Map) arg0.getData());
				File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters[0], file[0].toString(), t, null,
						Common.getGeneratedBarCode(), locale);
				Report.tampil(myfile, center);
			}
		};
		eventListener.onEvent(null);

		if (parameters.length == 0 || parameters[0] == null
				|| parameters[0].get("tidak_tampil_pilihan_export") == null) {
			org.zkoss.zul.North north = new org.zkoss.zul.North();
			north.setParent(borderlayout);
			north.appendChild(CommonReport.exportReport(new ParameterListener() {
				@SuppressWarnings("unchecked")
				@Override
				public Map generateParameters() throws Exception {
					return parameters[0];
				}
			}, file[0].toString(), null, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters[0], file[0].toString(), t,
							null, Common.getGeneratedBarCode(), (Locale) arg0.getData());
					Report.tampil(myfile, center);
				}
			}));
		}

		window.setVisible(true);
		window.onModal();
		return tabbox;
	}

	@SuppressWarnings("rawtypes")
	public static EventListener generatePDFReport(final String formatLaporan, final Map parameters, final String file,
			final Date t, final Locale locale) throws Exception {
		if (parameters == null)
			return null;

		EventListener eventListener = null;
		try {
			if (ExecutionsCtrl.getCurrentCtrl() != null) {
				MyWindow window = new MyWindow("Laporan", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("90%");
				window.setWidth("900px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				final Center center = new Center();
				ais.ui.util.ZkCompat.setFlex(center, true);
				center.setParent(borderlayout);

				eventListener = new EventListener() {
					@SuppressWarnings({ "unchecked" })
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (arg0 != null && arg0.getData() instanceof Map)
							parameters.putAll((Map) arg0.getData());
						File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters, file, t, null,
								Common.getGeneratedBarCode(), locale);
						Report.tampil(myfile, center);
					}
				};
				eventListener.onEvent(null);

				if (parameters == null || parameters.get("tidak_tampil_pilihan_export") == null
						|| Common.getApakahAdmin()) {
					org.zkoss.zul.North north = new org.zkoss.zul.North();
					north.setParent(borderlayout);
					north.appendChild(CommonReport.exportReport(new ParameterListener() {
						@SuppressWarnings("unchecked")
						@Override
						public Map generateParameters() throws Exception {
							return parameters;
						}
					}, file, null, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters, file, t, null,
									Common.getGeneratedBarCode(), (Locale) arg0.getData());
							Report.tampil(myfile, center);
						}
					}));
				}

				window.setVisible(true);
				window.onModal();
			} else {

				// ==========================================
				// MODE 2: Eksekusi Raw Servlet / JSP Response
				// ==========================================
//				HttpServletRequest request = RequestContext.get();
				HttpServletResponse response = ResponseContext.get();

				File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters, file, t, null,
						Common.getGeneratedBarCode(), locale);

				String encryptedName = Common.desEncrypter.get().encrypt(myfile.getName());
				String pathUri = "/pdf?p=" + URLEncoder.encode(encryptedName, "UTF-8");
				String urlPdf = Common.ROOT + pathUri;

				// Pengecekan View Mobile via Google Docs (Sama dengan logika di method tampil)
				String hostProtocol = Common.getRequestHostWithProtocol();
				if (!hostProtocol.toLowerCase().contains("localhost")
						&& !hostProtocol.toLowerCase().contains("127.0.0.1")
						&& Common.bolehKonfigurasi("gunakan_google_view_saat_mobile", Konfigurasi.TIDAK_AKTIF)
						&& (Common.isAsliMobile() || Common.bolehKonfigurasi("gunakan_google_view_saat_tampilkan_pdf", Konfigurasi.TIDAK_AKTIF))) {

					String fullPath = hostProtocol + Common.ROOT + pathUri;
					urlPdf = "https://docs.google.com/gview?embedded=true&url=" + URLEncoder.encode(fullPath, "UTF-8");
				}

				// Generate Modal HTML & JS mengandalkan DOM Insert (Efisien Memori dengan
				// StringBuilder)
				String modalId = "modalPdf_" + System.currentTimeMillis();
				StringBuilder sb = new StringBuilder();

				sb.append("<script>");
				sb.append("var modalHtml = '");
				sb.append("<div class=\"modal fade\" id=\"").append(modalId)
						.append("\" tabindex=\"-1\" aria-hidden=\"true\" style=\"z-index: 106000;\">");
				sb.append("<div class=\"modal-dialog modal-xl modal-dialog-centered\">");
				sb.append("<div class=\"modal-content shadow-lg border-0 rounded-4\">");

				sb.append("<div class=\"modal-header border-bottom-0 bg-light\">");
				sb.append(
						"<h5 class=\"modal-title fw-bold text-dark\"><i class=\"fas fa-file-pdf text-danger me-2\"></i>Laporan / Dokumen</h5>");
				sb.append(
						"<button type=\"button\" class=\"btn-close\" data-bs-dismiss=\"modal\" aria-label=\"Close\"></button>");
				sb.append("</div>");

				sb.append(
						"<div class=\"modal-body p-0\" style=\"height: 80vh; min-height: 500px; background: #525659;\">");
				sb.append("<iframe src=\"").append(urlPdf)
						.append("\" style=\"width: 100%; height: 100%; border: none;\"></iframe>");
				sb.append("</div>");

				sb.append("<div class=\"modal-footer border-top-0 bg-light\">");
				sb.append(
						"<button type=\"button\" class=\"btn btn-secondary px-4 rounded-pill\" data-bs-dismiss=\"modal\">Tutup</button>");
				sb.append("</div>");

				sb.append("</div></div></div>';");

				// Injeksi Modal ke Body & Panggil Instance Bootstrap
				sb.append("document.body.insertAdjacentHTML('beforeend', modalHtml);");
				sb.append("var myModalEl = document.getElementById('").append(modalId).append("');");
				sb.append("var myModal = new bootstrap.Modal(myModalEl);");
				sb.append("myModal.show();");

				// OPTIMASI DOM: Hapus elemen HTML dari Browser setelah ditutup agar tidak
				// membebani RAM Client
				sb.append("myModalEl.addEventListener('hidden.bs.modal', function () { myModalEl.remove(); });");
				sb.append("</script>");

				if (response != null) {
					response.setContentType("text/html;charset=UTF-8");
					response.getWriter().print(sb.toString());
					response.getWriter().flush();
				}

			}
		} catch (Exception e) {
			if (isReportErrorLogConsoleEnabled()) e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/Report.java:2674");
			if (!(e instanceof ReportGenerationException)) {
				File detailFile = createReportErrorDetailFile(formatLaporan, file, file, e, parameters);
				errorProgress(getProgressContext(parameters), e, detailFile);
			}
		}

		return eventListener;
	}

	/**
	 * Pastikan wadah pratinjau dapat di-scroll. Pada popup modal report (Borderlayout → Center)
	 * region tidak otomatis autoscroll sehingga konten yang lebih tinggi dari window (iframe min
	 * 2000px) terpotong tanpa scrollbar. LayoutRegion → setAutoscroll(true); komponen HTML lain →
	 * tambahkan {@code overflow:auto} pada style bila belum ada.
	 */
	private static void pastikanWadahBisaScroll(Component center) {
		try {
			if (center == null) {
				return;
			}
			if (center instanceof org.zkoss.zul.LayoutRegion) {
				((org.zkoss.zul.LayoutRegion) center).setAutoscroll(true);
			} else if (center instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
				org.zkoss.zk.ui.HtmlBasedComponent h = (org.zkoss.zk.ui.HtmlBasedComponent) center;
				String st = h.getStyle();
				if (st == null) {
					st = "";
				}
				if (st.indexOf("overflow") < 0) {
					h.setStyle(st + (st.isEmpty() || st.trim().endsWith(";") ? "" : ";") + "overflow:auto;");
				}
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/report/Report.java:2707");
		}
	}

	public static void tampil(final File myfile, final Component center) {
		/*
		 * Ambil snapshot PDF sebelum berpindah ke event timer ZK. Berkas hasil Jasper
		 * berada di direktori sementara dan pada beberapa instalasi dapat dibersihkan
		 * atau tidak terlihat lagi ketika request iframe media berikutnya dijalankan.
		 * Snapshot byte[] juga membuat AMedia dapat diputar ulang tanpa bergantung pada
		 * File/InputStream transient. Ini adalah jalur yang sama dengan tombol Download,
		 * tetapi disimpan khusus selama popup pratinjau masih hidup.
		 */
		final byte[] pdfPratinjau = bacaPdfPratinjau(myfile);
		// Konsumsi penanda sekali-pakai SECARA SINKRON (sebelum timer) agar nilainya benar.
		final boolean pdfDefaultSekali = Boolean.TRUE.equals(PREVIEW_PDF_DEFAULT_SEKALI.get());
		PREVIEW_PDF_DEFAULT_SEKALI.remove();
		// Identitas jenis laporan (dibaca SINKRON lalu dibersihkan) untuk: (a) default PDF per-jenis
		// laporan lewat konfigurasi, dan (b) sakelar admin "Default PDF".
		final String reportKey = CURRENT_REPORT_KEY.get();
		CURRENT_REPORT_KEY.remove();
		// Laporan ini tampil default PDF bila: penanda sekali-pakai aktif ATAU jenis laporannya masuk
		// daftar konfigurasi laporan_default_pdf.
		final boolean pdfDefaultLaporan = pdfDefaultSekali || laporanDefaultPdf(reportKey);
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				/* Window report bisa sudah ditutup/detach saat timer menyala;
				 * setParent/Common.clear pada komponen tanpa page melempar NPE
				 * (addMoved). Lewati saja - tidak ada lagi yang perlu dirender. */
				if (center == null || center.getDesktop() == null || center.getPage() == null) {
					return;
				}

				// Pastikan WADAH pratinjau bisa di-scroll. Pada popup modal "Laporan" (Borderlayout →
				// Center) region tak otomatis autoscroll → konten (iframe min 2000px) terpotong tanpa
				// scrollbar. Set autoscroll utk LayoutRegion, atau overflow:auto utk komponen HTML biasa.
				pastikanWadahBisaScroll(center);

				// Komponen sudah berupa Iframe (dipakai ulang) → render langsung mode default,
				// tanpa bar toggle (tidak ada wadah untuk menaruh bar). Di MOBILE browser tak bisa
				// menampilkan PDF dalam iframe → PAKSA HTML bila pendamping HTML tersedia.
				if (center instanceof Iframe) {
					boolean htmlMode = Common.isMobile()
							? adaPendampingHtml(myfile)
							: (modeHtmlDefault(myfile) && !pdfDefaultLaporan);
					renderAreaPratinjau(myfile, center, htmlMode, pdfPratinjau);
					return;
				}

				File htmlFile = berkasHtmlPendamping(myfile);
				boolean adaHtml = htmlFile != null && htmlFile.exists() && htmlFile.length() > 0;

				// Tidak ada pendamping HTML → PDF biasa (perilaku lama), tanpa toggle.
				if (!adaHtml) {
					org.zkoss.zul.Div area = new org.zkoss.zul.Div();
					pasangGridPratinjau(center, null, area);
					renderAreaPratinjau(myfile, area, false, pdfPratinjau);
					return;
				}

				// Ada pendamping HTML → sediakan bar tampilan.
				// Desktop: TOGGLE "HTML (mirip PDF) / PDF asli" (default ikut konfigurasi preview_laporan_html).
				// Mobile: toggle DISEMBUNYIKAN & dipaksa HTML (browser HP tak bisa menampilkan PDF dalam
				//         iframe). Pada kedua mode disediakan tombol "Cetak" untuk mencetak pratinjau HTML.
				final org.zkoss.zul.Div area = new org.zkoss.zul.Div();
				// PERBAIKAN lebar pratinjau "tidak full":
				// Sebelumnya wadah toggle+pratinjau memakai Vbox. ZK Vbox (align default "start")
				// TIDAK meregangkan anak secara horizontal — sel-nya menyusut selebar bar toggle,
				// sehingga area pratinjau (iframe PDF/HTML) ikut menyempit (~bagian kiri saja) dan
				// menyisakan ruang putih besar di kanan. Jalur tanpa-pendamping-HTML menaruh iframe
				// langsung di Center (100%) sehingga selalu penuh — itulah bedanya. Pakai Div blok
				// (anak otomatis selebar 100% Center) agar pratinjau memenuhi lebar penuh.
				boolean mobile = Common.isMobile();
				boolean htmlDefault = mobile ? true : (previewHtmlAktif() && !pdfDefaultLaporan);
				Component bar = bangunBarToggleTampilan(myfile, area, htmlDefault, mobile, reportKey,
						pdfPratinjau);
				pasangGridPratinjau(center, bar, area);

				renderAreaPratinjau(myfile, area, htmlDefault, pdfPratinjau);
			}
		});
	}

	/**
	 * Wadah baku pratinjau laporan. Gunakan Div flex langsung dan jangan Grid/Row:
	 * sel Grid ZK menghitung tinggi dari region induk lalu memotong iframe pada sekitar
	 * 190-250px (terlihat pada KHS dan Surat Aktif Kuliah), walaupun iframe memiliki
	 * tinggi sendiri. Div flex memberi area sisa viewport secara nyata dan scrollbar
	 * tetap berada di dalam area pratinjau.
	 */
	private static void pasangGridPratinjau(Component center, Component bar, org.zkoss.zul.Div area) {
		if (center == null || area == null) {
			return;
		}
		Common.clear(center);
		org.zkoss.zul.Div wrapper = new org.zkoss.zul.Div();
		wrapper.setWidth("100%");
		wrapper.setHeight("100%");
		wrapper.setStyle("width:100%; height:calc(100vh - 180px); min-height:560px; "
				+ "display:flex; flex-direction:column; overflow:hidden; box-sizing:border-box; "
				+ "border:0; margin:0; padding:0; background:transparent;");
		wrapper.setParent(center);

		if (bar != null) {
			bar.setParent(wrapper);
		}

		area.setWidth("100%");
		area.setVflex("1");
		area.setStyle("width:100%; flex:1 1 auto; min-height:520px; overflow:auto; "
				+ "box-sizing:border-box; background:#e9eef5;");
		area.setParent(wrapper);
	}

	/** Mode pratinjau default: HTML bila konfigurasi aktif DAN pendamping HTML tersedia. */
	private static boolean modeHtmlDefault(File myfile) {
		if (!previewHtmlAktif()) {
			return false;
		}
		File h = berkasHtmlPendamping(myfile);
		return h != null && h.exists() && h.length() > 0;
	}

	/**
	 * Render isi pratinjau ke {@code target} sesuai mode: {@code html=true} →
	 * pendamping HTML (mirip PDF); selain itu → PDF via servlet. Aman dipanggil
	 * ulang saat toggle (membersihkan lalu membangun ulang iframe).
	 */
	private static void renderAreaPratinjau(File myfile, Component target, boolean html,
			byte[] pdfPratinjau) {
		try {
			// Bersihkan area dulu: hapus iframe lama (PDF/HTML) agar toggle benar-benar berganti.
			if (target != null && !(target instanceof Iframe)) {
				try {
					Common.clear(target);
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/report/Report.java:2799");
				}
			}
			boolean ok = false;
			if (html) {
				File htmlFile = berkasHtmlPendamping(myfile);
				if (htmlFile != null && htmlFile.exists() && htmlFile.length() > 0) {
					ok = tampilHtmlPendamping(htmlFile, target);
				}
			}
			if (!ok) {
				tampilPdfServletKe(myfile, target, pdfPratinjau);
			}
		} catch (Throwable t) {
			try {
				tampilPdfServletKe(myfile, target, pdfPratinjau);
			} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/report/Report.java:2815");
			}
		}
	}

	/**
	 * Tampilkan PDF ke {@code center}. Untuk browser desktop, layani langsung dari
	 * objek {@link File} hasil generate melalui media ZK. Jangan meminta ulang file
	 * hanya berdasarkan namanya ke servlet {@code /pdf}: pada instalasi multi-node
	 * atau saat direktori report berbeda antar request, servlet dapat mencari di
	 * node/folder lain dan menghasilkan 404 walaupun file yang dipegang di sini valid
	 * (tombol Download tetap berhasil karena memakai objek File ini).
	 *
	 * Jalur servlet tetap dipakai khusus Google View karena layanan eksternal itu
	 * membutuhkan URL HTTP yang dapat diakses publik.
	 */
	private static void tampilPdfServletKe(File myfile, Component center, byte[] pdfPratinjau)
			throws Exception {
		if ((pdfPratinjau == null || pdfPratinjau.length == 0)
				&& (myfile == null || !myfile.isFile() || myfile.length() <= 0L)) {
			throw new java.io.FileNotFoundException("Berkas PDF pratinjau tidak ditemukan atau kosong");
		}
		Iframe include;
		if (center instanceof Iframe) {
			include = (Iframe) center;
		} else {
			include = new MyIframe();
			Common.clear(center);
			include.setParent(center);
			include.setWidth("100%");
		}
		include.setHeight("78vh");
		include.setStyle("width:100% !important; height:78vh !important; min-height:520px !important; border:0; overflow:auto; background:#e9eef5;");
		include.setScrolling("auto");

		String h = Common.getRequestHostWithProtocol();
		boolean gunakanGoogleView = !h.toLowerCase().contains("localhost") && !h.toLowerCase().contains("127.0.0.1")
				&& Common.bolehKonfigurasi("gunakan_google_view_saat_mobile", Konfigurasi.TIDAK_AKTIF)
				&& (Common.isAsliMobile() || Common.bolehKonfigurasi("gunakan_google_view_saat_tampilkan_pdf", Konfigurasi.TIDAK_AKTIF));
		if (gunakanGoogleView) {
			String path = "/pdf?p="
					+ URLEncoder.encode(Common.desEncrypter.get().encrypt(myfile.getName()), "UTF-8");
			path = Common.getRequestHostWithProtocol() + path;
			include.setSrc(
					"https://docs.google.com/gview?embedded=true&url=" + URLEncoder.encode(path, "UTF-8"));
		} else {
			// File yang sama dengan sumber tombol Download; ZK membuat URL media
			// desktop-scoped sehingga tidak ada pencarian ulang berdasarkan nama file.
			include.setSrc(null);
			if (pdfPratinjau != null && pdfPratinjau.length > 0) {
				include.setContent(new AMedia(myfile.getName(), "pdf", "application/pdf", pdfPratinjau));
			} else {
				include.setContent(new AMedia(myfile.getName(), "pdf", "application/pdf", myfile, true));
			}
		}
		// Tampilkan progress bar "Memuat dokumen PDF" sampai iframe selesai memuat (async sisi klien).
		pasangScrollWadahPdf(include);
		pasangOverlayMuatPdf(include);
	}

	private static void pasangScrollWadahPdf(final Iframe include) {
		try {
			if (include == null) {
				return;
			}
			String uuid = include.getUuid();
			StringBuilder js = new StringBuilder(1024);
			js.append("(function(){var f=document.getElementById('").append(uuid).append("');");
			js.append("if(!f)return;");
			js.append("f.style.overflow='auto';f.setAttribute('scrolling','auto');");
			js.append("var p=f.parentNode;var n=0;");
			js.append("while(p&&n<8){");
			js.append("if(p.style){p.style.overflow='auto';");
			js.append("if((p.className||'').toString().indexOf('z-center')>=0){p.style.height='100%';}");
			js.append("}");
			js.append("p=p.parentNode;n++;");
			js.append("}");
			js.append("})();");
			Clients.evalJavaScript(js.toString());
		} catch (Exception ex) {
			ais.common.ErrorAuditUtil.record(ex,
					"auto-audit(empty-catch) src/ais/action/report/Report.java:pasangScrollWadahPdf");
		}
	}

	/**
	 * Bar kecil berisi sakelar on/off "Tampilkan sebagai HTML (mirip PDF)". Aktif =
	 * HTML (default), non-aktif = PDF asli. Saat diubah, hanya area pratinjau yang
	 * dirender ulang (tanpa membuat ulang laporan). Sakelar di-style oleh CSS
	 * {@code .ais-toggle-switch} (css_utama.css).
	 */
	private static Component bangunBarToggleTampilan(final File myfile, final org.zkoss.zul.Div area,
			boolean htmlDefault, boolean mobile, final String reportKey, final byte[] pdfPratinjau) {
		org.zkoss.zul.Div bar = new org.zkoss.zul.Div();
		bar.setWidth("100%");
		bar.setStyle("display:flex; align-items:center; gap:10px; flex-wrap:wrap; padding:8px 12px;"
				+ " background:#f8fafc; border-bottom:1px solid #e5e7eb; box-sizing:border-box;");

		if (!mobile) {
			// Desktop: label + sakelar HTML/PDF (perilaku lama).
			org.zkoss.zul.Html lbl = new org.zkoss.zul.Html(
					"<span style='font-size:12px;font-weight:800;color:#334155;'>Tampilan pratinjau</span>");
			lbl.setParent(bar);

			final org.zkoss.zul.Checkbox sw = new org.zkoss.zul.Checkbox("Tampilkan sebagai HTML (mirip PDF)");
			sw.setSclass("ais-toggle-switch");
			sw.setChecked(htmlDefault);
			sw.setTooltiptext("Aktif: tampil HTML yang dibuat mirip PDF. Non-aktif: tampil PDF asli.");
			sw.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					renderAreaPratinjau(myfile, area, sw.isChecked(), pdfPratinjau);
				}
			});
			sw.setParent(bar);

			// Sakelar KHUSUS ADMIN: "Default PDF" — bila diaktifkan, JENIS laporan ini (reportKey)
			// akan default tampil PDF setiap kali dibuka (disimpan ke konfigurasi laporan_default_pdf,
			// per-jenis-laporan). Laporan lain tetap default HTML (lebih ringan). Hanya tampil bila
			// pengguna admin dan identitas laporan diketahui.
			if (reportKey != null && !reportKey.trim().isEmpty() && Common.getApakahAdmin()) {
				final org.zkoss.zul.Checkbox swDefaultPdf = new org.zkoss.zul.Checkbox("Default PDF");
				swDefaultPdf.setSclass("ais-toggle-switch");
				swDefaultPdf.setChecked(laporanDefaultPdf(reportKey));
				swDefaultPdf.setStyle("margin-left:8px;");
				swDefaultPdf.setTooltiptext("Aktif: laporan JENIS ini selalu terbuka default PDF. "
						+ "Non-aktif: default HTML (lebih ringan). Berlaku hanya untuk laporan ini.");
				swDefaultPdf.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event e) throws Exception {
						setLaporanDefaultPdf(reportKey, swDefaultPdf.isChecked());
						// Terapkan langsung ke pratinjau yang sedang tampil: PDF bila diaktifkan.
						boolean htmlSekarang = !swDefaultPdf.isChecked();
						sw.setChecked(htmlSekarang);
						renderAreaPratinjau(myfile, area, htmlSekarang, pdfPratinjau);
					}
				});
				swDefaultPdf.setParent(bar);
			}
		} else {
			// Mobile: TANPA sakelar (browser HP tak bisa menampilkan PDF di iframe) → selalu HTML.
			org.zkoss.zul.Html lbl = new org.zkoss.zul.Html(
					"<span style='font-size:12px;font-weight:800;color:#334155;'>Pratinjau laporan</span>");
			lbl.setParent(bar);
		}

		// Kontrol zoom berlaku untuk HTML maupun PDF. PDF yang lebar (mis. Buku Besar)
		// sebelumnya selalu dibuka dengan mode "fit page", sehingga huruf menjadi sangat
		// kecil. Fragment #zoom dipakai untuk viewer PDF browser; untuk HTML pendamping,
		// zoom diterapkan langsung pada dokumen iframe yang same-origin.
		final int[] zoomPersen = new int[] { 100 };
		final org.zkoss.zul.Label labelZoom = new org.zkoss.zul.Label("100%");
		labelZoom.setStyle("min-width:42px; text-align:center; font-weight:800; color:#334155;");

		final org.zkoss.zul.Button perkecil = new org.zkoss.zul.Button("-");
		perkecil.setTooltiptext("Perkecil tampilan laporan");
		perkecil.setStyle("min-width:32px; padding:4px 9px; font-weight:900;");
		perkecil.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				zoomPersen[0] = Math.max(50, zoomPersen[0] - 25);
				labelZoom.setValue(zoomPersen[0] + "%");
				aturZoomIframePratinjau(area, zoomPersen[0]);
			}
		});
		perkecil.setParent(bar);
		labelZoom.setParent(bar);

		final org.zkoss.zul.Button perbesar = new org.zkoss.zul.Button("+");
		perbesar.setTooltiptext("Perbesar tampilan laporan");
		perbesar.setStyle("min-width:32px; padding:4px 9px; font-weight:900;");
		perbesar.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				zoomPersen[0] = Math.min(250, zoomPersen[0] + 25);
				labelZoom.setValue(zoomPersen[0] + "%");
				aturZoomIframePratinjau(area, zoomPersen[0]);
			}
		});
		perbesar.setParent(bar);

		final org.zkoss.zul.Button resetZoom = new org.zkoss.zul.Button("Reset zoom");
		resetZoom.setTooltiptext("Kembalikan ukuran tampilan ke 100%");
		resetZoom.setStyle("padding:4px 10px; font-weight:700;");
		resetZoom.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				zoomPersen[0] = 100;
				labelZoom.setValue("100%");
				aturZoomIframePratinjau(area, 100);
			}
		});
		resetZoom.setParent(bar);

		final org.zkoss.zul.Button layarPenuh = new org.zkoss.zul.Button("Layar penuh");
		layarPenuh.setTooltiptext("Buka pratinjau selebar layar agar lebih mudah dibaca");
		layarPenuh.setStyle("padding:4px 10px; font-weight:700;");
		layarPenuh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				bukaLayarPenuhPratinjau(area);
			}
		});
		layarPenuh.setParent(bar);

		// Tombol "Cetak" → langsung mencetak isi pratinjau (HTML) yang sedang tampil di iframe.
		final org.zkoss.zul.Button cetak = new org.zkoss.zul.Button("Cetak");
		cetak.setStyle("margin-left:auto; background:#2563eb; color:#fff; border:0; border-radius:8px;"
				+ " padding:6px 16px; font-weight:800; font-size:12px; cursor:pointer; line-height:1.4;");
		cetak.setTooltiptext("Cetak halaman pratinjau ini");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				cetakIframePratinjau(area);
			}
		});
		cetak.setParent(bar);
		return bar;
	}

	/** Atur pembesaran isi iframe pratinjau tanpa mengubah ukuran hasil cetak/unduhan. */
	private static void aturZoomIframePratinjau(org.zkoss.zul.Div area, int zoomPersen) {
		try {
			if (area == null) {
				return;
			}
			int zoom = Math.max(50, Math.min(250, zoomPersen));
			StringBuilder js = new StringBuilder(1024);
			js.append("(function(){var a=document.getElementById('").append(area.getUuid()).append("');");
			js.append("if(!a)return;var f=a.querySelector('iframe');if(!f)return;");
			js.append("var z=").append(zoom).append(";");
			js.append("try{var d=f.contentWindow&&f.contentWindow.document;");
			js.append("if(d&&d.body&&(!d.contentType||d.contentType.toLowerCase().indexOf('html')>=0)){d.body.style.zoom=(z/100);return;}}catch(ex){}");
			js.append("var s=f.getAttribute('src')||f.src||'';if(!s)return;");
			js.append("s=s.replace(/#.*$/,'');f.setAttribute('src',s+'#zoom='+z);})();");
			Clients.evalJavaScript(js.toString());
		} catch (Exception ex) {
			ais.common.ErrorAuditUtil.record(ex,
					"auto-audit(empty-catch) src/ais/action/report/Report.java:aturZoomIframePratinjau");
		}
	}

	/** Memakai Fullscreen API browser; fallback membuka sumber pratinjau di tab baru. */
	private static void bukaLayarPenuhPratinjau(org.zkoss.zul.Div area) {
		try {
			if (area == null) {
				return;
			}
			StringBuilder js = new StringBuilder(768);
			js.append("(function(){var a=document.getElementById('").append(area.getUuid()).append("');");
			js.append("if(!a)return;var f=a.querySelector('iframe');if(!f)return;");
			js.append("try{var p=f.requestFullscreen||f.webkitRequestFullscreen||f.msRequestFullscreen;");
			js.append("if(p){p.call(f);return;}}catch(ex){}");
			js.append("var s=f.getAttribute('src')||f.src||'';if(s)window.open(s,'_blank');})();");
			Clients.evalJavaScript(js.toString());
		} catch (Exception ex) {
			ais.common.ErrorAuditUtil.record(ex,
					"auto-audit(empty-catch) src/ais/action/report/Report.java:bukaLayarPenuhPratinjau");
		}
	}

	/** True bila berkas pendamping HTML (mirip PDF) untuk {@code myfile} tersedia & berisi. */
	private static boolean adaPendampingHtml(File myfile) {
		File h = berkasHtmlPendamping(myfile);
		return h != null && h.exists() && h.length() > 0;
	}

	/**
	 * Cetak isi pratinjau yang sedang tampil. Mencari {@code <iframe>} di dalam {@code area} lalu
	 * memanggil {@code contentWindow.print()} (same-origin untuk pendamping HTML, sehingga halaman
	 * HTML itu langsung tercetak). Bila iframe lintas-asal/tak ada → fallback {@code window.print()}.
	 */
	private static void cetakIframePratinjau(org.zkoss.zul.Div area) {
		try {
			if (area == null) {
				return;
			}
			String uuid = area.getUuid();
			StringBuilder js = new StringBuilder(512);
			js.append("(function(){var a=document.getElementById('").append(uuid).append("');");
			js.append("if(!a){try{window.print();}catch(e){}return;}");
			js.append("var f=a.querySelector('iframe');");
			js.append("if(!f){try{window.print();}catch(e){}return;}");
			js.append("try{f.contentWindow.focus();f.contentWindow.print();}");
			js.append("catch(e){try{window.print();}catch(ee){}}");
			js.append("})();");
			Clients.evalJavaScript(js.toString());
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/Report.java:2930");
			// Cetak bersifat pendukung; bila gagal, pratinjau tetap tampil normal.
		}
	}

	/**
	 * Memasang overlay progres "Memuat dokumen PDF" (spinner + progress bar berjalan) yang muncul bila
	 * pemuatan iframe PDF lebih dari ~250 ms, dan otomatis hilang saat iframe selesai memuat (event
	 * {@code load}) atau setelah batas aman 15 detik. Murni sisi klien, tidak mengubah tata letak
	 * laporan; berlaku untuk semua laporan karena semuanya tampil lewat {@link #tampil(File, Component)}.
	 */
	private static void pasangOverlayMuatPdf(final Iframe include) {
		try {
			if (include == null) {
				return;
			}
			final String uuid = include.getUuid();
			StringBuilder js = new StringBuilder(2048);
			js.append("(function(){var d=document;var f=d.getElementById('").append(uuid).append("');var done=false;");
			js.append("var s=d.getElementById('rpvPdfLoadS');if(!s){s=d.createElement('style');s.id='rpvPdfLoadS';");
			js.append("s.innerHTML='@keyframes rpvSpin{to{transform:rotate(360deg)}}@keyframes rpvSld{0%{left:-40%}100%{left:100%}}';d.head.appendChild(s);}");
			js.append("function show(){if(done||d.getElementById('rpvPdfLoad'))return;var o=d.createElement('div');o.id='rpvPdfLoad';");
			js.append("o.style.cssText='position:fixed;inset:0;z-index:2147483646;background:rgba(15,23,42,.5);display:flex;align-items:center;justify-content:center;pointer-events:none';");
			js.append("o.innerHTML=\"<div style='background:#fff;border-radius:16px;padding:22px 26px;min-width:300px;max-width:88%;box-shadow:0 24px 60px rgba(0,0,0,.35);font-family:Segoe UI,Arial,sans-serif'>");
			js.append("<div style='font-size:14px;font-weight:800;color:#0f172a;display:flex;align-items:center;gap:10px'>");
			js.append("<span style=\\\"display:inline-block;width:22px;height:22px;border:3px solid #bfdbfe;border-top-color:#2563eb;border-radius:50%;animation:rpvSpin .8s linear infinite\\\"></span>");
			js.append("Memuat dokumen PDF&hellip;</div>");
			js.append("<div style='font-size:12px;color:#64748b;margin:8px 0 14px'>Dokumen sedang dimuat ke tampilan. Mohon tunggu sebentar.</div>");
			js.append("<div style='position:relative;height:8px;background:#e2e8f0;border-radius:999px;overflow:hidden'>");
			js.append("<div style='position:absolute;top:0;height:100%;width:40%;border-radius:999px;background:linear-gradient(90deg,#2563eb,#0ea5e9);animation:rpvSld 1.1s infinite ease-in-out'></div></div></div>\";");
			js.append("d.body.appendChild(o);}");
			js.append("function hide(){done=true;var x=d.getElementById('rpvPdfLoad');if(x&&x.parentNode)x.parentNode.removeChild(x);}");
			js.append("var t=setTimeout(show,250);");
			js.append("if(f){f.addEventListener('load',function(){clearTimeout(t);hide();});}");
			js.append("setTimeout(function(){clearTimeout(t);hide();},4000);");
			js.append("})();");
			Clients.evalJavaScript(js.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/Report.java:2967");
			// Overlay hanya pendukung tampilan; jika gagal, laporan tetap tampil normal.
		}
	}

	@SuppressWarnings("rawtypes")
	public static EventListener generatePDFReport(String formatLaporan, Map parameters, String file, Date t, West west)
			throws Exception {
		return generatePDFReport(formatLaporan, parameters, file, t, Common.locale, west, null);
	}

	@SuppressWarnings("rawtypes")
	public static EventListener generatePDFReport(String formatLaporan, Map parameters, String file, Date t,
			Locale locale, West west) throws Exception {
		return generatePDFReport(formatLaporan, parameters, file, t, locale, west, null);
	}

	@SuppressWarnings("rawtypes")
	public static EventListener generatePDFReport(final String formatLaporan, final Map parameters, final String file,
			final Date t, final Locale locale, West west, Component parent) throws Exception {
		if (parameters == null)
			return null;

		MyWindow window = new MyWindow("Laporan", "none", true);
		window.setParent(parent == null ? ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot() : parent);
		window.setHeight(parent == null ? "90%" : "100%");
		window.setWidth(parent == null ? "900px" : "100%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		if (west != null) {
			window.setHeight("95%");
			window.setWidth("90%");
			borderlayout.appendChild(west);
		}

		final Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		EventListener eventListener = new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Map<String, Serializable> maps = null;
				if (arg0 != null && arg0.getData() instanceof Map) {
					maps = (Map<String, Serializable>) arg0.getData();
					parameters.putAll(maps);
				}

				File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters, file, t, null,
						Common.getGeneratedBarCode(), locale);
				Report.tampil(myfile, center);

				if (maps != null)
					maps.put("report_file", myfile.getAbsolutePath());
			}
		};
		eventListener.onEvent(null);

		if (parameters == null || parameters.get("tidak_tampil_pilihan_export") == null) {
			org.zkoss.zul.North north = new org.zkoss.zul.North();
			north.setParent(borderlayout);
			north.appendChild(CommonReport.exportReport(new ParameterListener() {
				@SuppressWarnings("unchecked")
				@Override
				public Map<String, Serializable> generateParameters() throws Exception {
					return parameters;
				}
			}, file, null, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					File myfile = Report.generateFileReportWithProgress(formatLaporan, parameters, file, t, null,
							Common.getGeneratedBarCode(), (Locale) arg0.getData());
					Report.tampil(myfile, center);
				}
			}));
		}

		if (parent == null) {
			window.setVisible(true);
			window.onModal();
		} else {
			window.setTitle("");
			window.setClosable(false);
		}
		return eventListener;
	}

	public void onBantuan(Event event) throws Exception {
		File file = new File(application.getRealPath("/help/User_Manual__Sistem_Informasi_Akademik.pdf"));
		final InputStream mediais = new FileInputStream(file);

		// InputStream ini tidak ditutup secara manual karena akan digunakan oleh object
		// AMedia (ZK Framework) yang akan memanggil close() secara otomatis
		final AMedia amedia = new AMedia("Bantuan_Sistem_Informasi_Akademik.pdf", "pdf", "application/pdf", mediais);
		Filedownload.save(amedia);
	}
}
