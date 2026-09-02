package ais.common;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Column;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.DetailperkuliahanForPenilaianHelper;
import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.FormatNilaiProposalSkripsi;
import ais.database.model.FormatNilaiSkripsi;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.NamaTugasKelompokPunyaMahasiswa;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.file.TugasFileContent;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Helper terfokus untuk grading. Tipe ini membungkus satu variasi kecil dari alur yang lebih umum
 * agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String EXCEL_MIME_TYPE}, {@code int
 * GRID_PAGE_SIZE}, {@code int BATCH_SIZE}; inisialisasi/lifecycle ({@code buatToolbarAtas()});
 * pembacaan/pencarian ({@code getCellText()}, {@code tampilkanGridDariExcel()}, {@code tampilkanPopupExcel()});
 * validasi/perhitungan ({@code hitungNilaiBerdasarkanJenisItemPenilaianSiswa()}, {@code
 * hitungNilaiBerdasarkanJenisItemPenilaianSiswa()}, {@code hitungNilaiBerdasarkanJenisItemPenilaianSiswa()},
 * {@code hitungNilaiBerdasarkanFormatNilaiObe()}, {@code hitungNilaiBerdasarkanFormatNilai()}, {@code
 * hitungNilaiBerdasarkanFormatNilai()}); operasi domain lain ({@code closeSession()}, {@code
 * closeInputStream()}, {@code trim()}, {@code hasText()}, {@code containsId()}, {@code escapeHtml()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class GradingHelper {

	private static final String EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private static final int GRID_PAGE_SIZE = 15;
	private static final int BATCH_SIZE = 50;

	private static void closeSession(Session session) {
		if (session != null) {
			try {
				session.clear();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/GradingHelper.java:98");
			}
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/GradingHelper.java:102");
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/GradingHelper.java:106");
			}
		}
	}

	private static void closeInputStream(InputStream inputStream) {
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/GradingHelper.java:115");
			}
		}
	}

	private static String trim(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}

	private static boolean hasText(Object value) {
		return trim(value).length() > 0;
	}

	private static boolean containsId(String source, Long id) {
		return id != null && source != null && source.indexOf("," + id + ",") >= 0;
	}

	private static String escapeHtml(Object value) {
		return trim(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private static double safeDivide(double numerator, double denominator) {
		if (denominator <= 0.0) {
			return 0.0;
		}
		return numerator / denominator;
	}

	private static JSONObject createJson(String text) {
		try {
			if (text == null || text.trim().isEmpty()) {
				return new JSONObject();
			}
			return new JSONObject(text);
		} catch (Exception e) {
			return new JSONObject();
		}
	}

	private static String getCellText(XSSFCell cell) {
		if (cell == null) {
			return "";
		}
		try {
			return cell.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private static void appendCell(Row row, String text, String style) {
		try {
			Label label = new Label(text == null ? "" : text);
			label.setStyle("display:block; white-space:normal; line-height:18px;" + (style == null ? "" : style));
			row.appendChild(label);
		} catch (Exception e) {
			row.appendChild(new Label());
		}
	}

	private static void siapkanWindowScrollable(MyWindow window, String height, String width) {
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight(height);
		window.setWidth(width);
		window.setContentStyle("overflow:hidden; padding:0; background:#f4f7fb;");
	}

	private static void siapkanBorderlayout(Borderlayout borderlayout) {
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("overflow:hidden; background:#f4f7fb;");
	}

	private static void siapkanCenterScrollable(Center center) {
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setAutoscroll(true);
		center.setStyle("overflow:auto; background:#f4f7fb;");
	}

	private static Toolbar buatToolbarAtas(Borderlayout borderlayout) {
		North north = new North();
		north.setParent(borderlayout);
		north.setSize("44px");
		north.setStyle("border:0; background:#e9eef4; padding:0;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);
		toolbar.setWidth("100%");
		toolbar.setStyle("padding:8px 12px; background:#e9eef4; border:0; box-sizing:border-box; "
				+ "display:block; white-space:normal;");
		return toolbar;
	}

	private static void styleToolbarButton(MyToolbarbuttonConfig button, String color) {
		button.setStyle(
				"font-weight:bold; color:" + color + "; margin-right:12px; padding:4px 8px; " + "border-radius:4px;");
	}

	/**
	 * Apakah pengguna yang login adalah <b>PESERTA DIDIK</b> — mahasiswa, calon mahasiswa, siswa,
	 * atau calon siswa? Dipakai untuk MENYEMBUNYIKAN tombol input/kelola nilai (mis.
	 * "Masukkan nilai …") pada tampilan Rekap Total Nilai: tombol tersebut hanya untuk
	 * dosen/guru/admin, sementara peserta didik cukup MELIHAT rekap dan tidak boleh memasukkan nilai.
	 * Dibungkus try/catch agar kegagalan pembacaan sesi tidak menggagalkan render.
	 *
	 * @return {@code true} bila pengguna login adalah salah satu jenis peserta didik.
	 */
	private static boolean apakahLoginPesertaDidik() {
		try {
			ais.database.model.Tbmuser u = Common.getCurrentUser();
			return u != null && (u.getMahasiswa() != null || u.getBiodataCalonMahasiswa() != null
					|| u.getSiswa() != null || u.getCalonSiswa() != null);
		} catch (Exception e) {
			return false;
		}
	}

	private static void tampilkanGridDariExcel(Component parent, File file, Intbox intbox, Intbox colSize)
			throws Exception {
		Common.clear(parent);

		Div wrapper = new Div();
		wrapper.setParent(parent);
		wrapper.setWidth("100%");
		wrapper.setHeight("100%");
		wrapper.setStyle("overflow:auto; background:#f7fafc; padding:10px; box-sizing:border-box;");

		int percobaan = 0;
		int maxRetry = 1;
		Exception lastException = null;

		while (percobaan <= maxRetry) {
			Common.clear(wrapper);

			Html info = new Html("<div style='padding:10px 12px; margin-bottom:8px; border:1px solid #d9edf7; "
					+ "background:#eef9ff; color:#31708f; border-radius:6px;'>"
					+ "<b>Pratinjau Grid Nilai</b><br/>Data ditampilkan ringan dalam Grid. "
					+ "Gunakan tombol <b>Preview / Download Excel</b> untuk membuka tampilan Excel."
					+ "</div>");
			info.setParent(wrapper);

			InputStream inputStream = null;
			try {
				// Buka via PATH FILE (OPCPackage/ZipFile = random-access, membaca central directory) —
				// BUKAN InputStream (ZipInputStream). Jalur InputStream gagal
				// "java.io.EOFException: Unexpected end of ZLIB input stream" pada .xlsx yang memakai ZIP
				// data descriptor (umum pada hasil tulis streaming/zss) karena ZipInputStream tak tahu
				// ukuran entry di muka. Pola path-file ini sudah baku di action lain
				// (DosenAction/AsramaAction/ChecklistPenilaianUmumAction). inputStream sengaja dibiarkan
				// null (finally closeInputStream(null) = no-op).
				XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
				XSSFSheet sheet = workbook.getSheetAt(0);

				Grid grid = new Grid();
				grid.setParent(wrapper);
				grid.setMold("paging");
				grid.setPageSize(GRID_PAGE_SIZE);
				grid.setPagingPosition("top");
				grid.getPagingChild().setMold("os");
				grid.setWidth("100%");
				grid.setHeight("100%");
				grid.setStyle("border:1px solid #dfe6ee; background:#ffffff; border-radius:6px; overflow:hidden;");

				Columns columns = new Columns();
				columns.setParent(grid);

				XSSFRow header = sheet.getRow(0);
				int maxColumn = colSize == null || colSize.getValue() == null ? 0 : colSize.getValue().intValue();
				if (header != null && header.getLastCellNum() > maxColumn) {
					maxColumn = header.getLastCellNum();
				}
				if (maxColumn <= 0) {
					maxColumn = 1;
				}

				int i;
				for (i = 0; i < maxColumn; i++) {
					Column column = new Column(getCellText(header == null ? null : header.getCell(i)));
					column.setParent(columns);
					if (i == 0) {
						column.setWidth("55px");
					} else if (i == 1) {
						column.setWidth("130px");
					} else if (i == 2) {
						column.setWidth("230px");
					} else {
						column.setWidth("180px");
					}
					column.setStyle("font-weight:bold; background:#34495e; color:#ffffff; padding:8px;");
				}

				Rows rows = new Rows();
				rows.setParent(grid);

				int maxRow = intbox == null || intbox.getValue() == null ? sheet.getLastRowNum()
						: intbox.getValue().intValue();
				if (sheet.getLastRowNum() > maxRow) {
					maxRow = sheet.getLastRowNum();
				}

				for (i = 1; i <= maxRow; i++) {
					XSSFRow excelRow = sheet.getRow(i);
					if (excelRow == null) {
						continue;
					}

					Row row = new Row();
					row.setParent(rows);
					row.setStyle(i % 2 == 0 ? "background:#fbfdff;" : "background:#ffffff;");

					int j;
					for (j = 0; j < maxColumn; j++) {
						appendCell(row, getCellText(excelRow.getCell(j)), j < 3 ? "font-weight:bold;" : "");
					}
				}

				return;
			} catch (Exception e) {
				lastException = e;
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/GradingHelper.java:335");
				// Berkas .xlsx korup/terpotong atau bukan file Office Open XML (mis. ZipException
				// "invalid bit length repeat", InvalidFormatException, dll.). Mencoba ulang TIDAK
				// akan menolong karena berkasnya memang rusak → hentikan retry & tampilkan pesan
				// yang jelas agar pengguna mengunggah ulang. (Deteksi via rantai cause + nama kelas
				// supaya tidak bergantung langsung pada kelas exception POI.)
				boolean berkasRusak = false;
				Throwable t = e;
				while (t != null) {
					if (t instanceof java.util.zip.ZipException || t instanceof java.io.EOFException
						|| t instanceof NullPointerException) {
						berkasRusak = true;
						break;
					}
					String cn = t.getClass().getName();
					if (cn.indexOf("InvalidFormatException") >= 0 || cn.indexOf("NotOfficeXmlFileException") >= 0
							|| cn.indexOf("EmptyFileException") >= 0 || cn.indexOf("OLE2") >= 0) {
						berkasRusak = true;
						break;
					}
					t = t.getCause();
				}
				if (berkasRusak) {
					// InputStream tetap ditutup oleh blok finally di bawah.
					// JANGAN catat ke audit DB (Common.tampilErrorJikaAdmin -> saveErrorToDatabase):
					// berkas .xlsx korup / bukan Office Open XML adalah kesalahan UNGGAHAN PENGGUNA,
					// bukan kegagalan sistem (mis. POIXMLException "Package should contain a content
					// type part [M1.13]"). Cukup cetak ke konsol server + tampilkan pesan ramah agar
					// tidak mengotori log error ECAMPUS. e.printStackTrace() sudah dipanggil di atas.
					Common.clear(wrapper);
					Html rusak = new Html(
							"<div style='padding:15px; color:#a94442; background:#f2dede; border:1px solid #ebccd1; border-radius:6px;'>"
									+ "<b>Berkas Excel tidak dapat dibaca.</b><br/>"
									+ "Berkas tampak rusak/terpotong atau bukan file <b>.xlsx</b> yang valid. "
									+ "Silakan unggah ulang berkas Excel (.xlsx) yang benar."
									+ "</div>");
					rusak.setParent(wrapper);
					return;
				}
			} finally {
				closeInputStream(inputStream);
			}

			if (percobaan < maxRetry) {
				Common.clear(wrapper);

				Html retryInfo = new Html(
						"<div style='padding:15px; color:#8a6d3b; background:#fcf8e3; border:1px solid #faebcc; border-radius:6px;'>"
								+ "<b>Pratinjau grid gagal dibuka.</b><br/>"
								+ "Sistem akan mencoba ulang otomatis dalam 5 detik."
								+ "</div>");
				retryInfo.setParent(wrapper);

				try {
					Thread.sleep(5000L);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();

					lastException = new Exception("Retry pratinjau grid dibatalkan karena thread di-interrupt.", ie);
					lastException.printStackTrace(); ais.common.ErrorAuditUtil.record(lastException, "auto-audit src/ais/common/GradingHelper.java:394");
					Common.tampilErrorJikaAdmin(lastException);

					Common.clear(wrapper);
					Html errorInterrupted = new Html(
							"<div style='padding:15px; color:#a94442; background:#f2dede; border:1px solid #ebccd1; border-radius:6px;'>"
									+ "Data berhasil diproses, tetapi pratinjau grid gagal dibuka. "
									+ "Silakan gunakan preview Excel atau tutup halaman ini, lalu buka kembali."
									+ "</div>");
					errorInterrupted.setParent(wrapper);
					return;
				}
			}

			percobaan++;
		}

		if (lastException != null) {
			Common.tampilErrorJikaAdmin(lastException);
		}

		Common.clear(wrapper);

		Html error = new Html(
				"<div style='padding:15px; color:#a94442; background:#f2dede; border:1px solid #ebccd1; border-radius:6px;'>"
						+ "Data berhasil diproses, tetapi pratinjau grid gagal dibuka. "
						+ "Silakan gunakan preview Excel atau tutup halaman ini, lalu buka kembali."
						+ "</div>");
		error.setParent(wrapper);
	}

	private static void tampilkanPopupExcel(final File file, final Intbox intbox, final Intbox colSize)
			throws Exception {
		final MyWindow window = new MyWindow("Preview / Download Excel", "none", true);
		siapkanWindowScrollable(window, "97%", "90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		siapkanBorderlayout(borderlayout);
		borderlayout.setParent(window);

		Toolbar toolbar = buatToolbarAtas(borderlayout);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup preview Excel");
		styleToolbarButton(cancel, "#a94442");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Unduh File .xlsx", "/img/excel.png");
		download.setTooltiptext("Download file Excel");
		styleToolbarButton(download, "#1f5d7a");
		download.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					Filedownload.save(new FileInputStream(file), EXCEL_MIME_TYPE, file.getName());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});
		download.setParent(toolbar);

		Center center = new Center();
		siapkanCenterScrollable(center);
		center.setParent(borderlayout);

		// Load langsung dari file (bukan via ZSS Spreadsheet.getBook() yang bisa null
		// saat file belum selesai dimuat widget ZSS secara asynchronous).
		if (file != null && file.exists() && file.length() > 0) {
			org.zkoss.poi.ss.usermodel.Workbook wb = null;
			try {
				wb = new org.zkoss.poi.xssf.usermodel.XSSFWorkbook(file.getAbsolutePath());
				int cols = colSize == null || colSize.getValue() == null ? 10 : colSize.getValue().intValue() + 1;
				int totalBaris = ais.ui.util.PratinjauXlsxHelper.renderWorkbookKeGrid(
						center, wb, ais.ui.util.PratinjauXlsxHelper.MAKS_BARIS_PREVIEW, cols, true);
				if (totalBaris > ais.ui.util.PratinjauXlsxHelper.MAKS_BARIS_PREVIEW) {
					Label infoLabel = new Label("Pratinjau " + ais.ui.util.PratinjauXlsxHelper.MAKS_BARIS_PREVIEW
							+ " baris pertama dari " + totalBaris + " baris. Klik \"Unduh File .xlsx\" untuk data lengkap.");
					infoLabel.setStyle("color:#b45309;font-size:11px;font-weight:bold;padding:4px 8px;display:block;");
					infoLabel.setParent(center);
				}
			} catch (Exception eWb) {
				Common.tampilErrorJikaAdmin(eWb);
			}
		} else {
			Label kosong = new Label("Berkas Excel belum tersedia atau masih diproses. Silakan tunggu lalu coba kembali.");
			kosong.setStyle("color:#dc2626;padding:12px;font-size:12px;");
			kosong.setParent(center);
		}

		window.setVisible(true);
		window.onModal();
	}

	public static void hitungNilaiBerdasarkanJenisItemPenilaianSiswa(JadwalPelajaran jadwalPelajaran,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, GrupPenilaian grupPenilaian,
			JenisItemPenilaianSiswa... fns) throws Exception {
		hitungNilaiBerdasarkanJenisItemPenilaianSiswa(null, jadwalPelajaran, grupKategoriItemPenilaianSiswa,
				grupPenilaian, fns);
	}

	public static void hitungNilaiBerdasarkanJenisItemPenilaianSiswa(Component parent, JadwalPelajaran jadwalPelajaran,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, GrupPenilaian grupPenilaian,
			JenisItemPenilaianSiswa... fns) throws Exception {
		hitungNilaiBerdasarkanJenisItemPenilaianSiswa(parent, jadwalPelajaran, null, grupKategoriItemPenilaianSiswa,
				grupPenilaian, fns);
	}

	@SuppressWarnings("unchecked")
	public static void hitungNilaiBerdasarkanJenisItemPenilaianSiswa(final Component parent, final JadwalPelajaran kul,
			final Siswa siswa, final GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa,
			final GrupPenilaian grupPenilaian, final JenisItemPenilaianSiswa... fns) throws Exception {

		if (fns != null && fns.length > 0 && kul != null) {

			final List<JadwalPelajaran> jadwalPelajarans = new ArrayList<JadwalPelajaran>();
			if (!jadwalPelajarans.contains(kul)) {
				jadwalPelajarans.add(kul);
			}

			final Map<Long, Map<Long, Double>> nilais = new HashMap<Long, Map<Long, Double>>();
			final Tbmuser tbmuser = Common.getCurrentUser();
			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
			final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Hitung Ulang Nilai Siswa");
			final Label downloadPath = new Label("");
			final Intbox intbox = new Intbox(10);
			final Intbox colSize = new Intbox(10);
			Clients.showBusy(label.getValue());

			final String filename = Sessions.getCurrent().getWebApp().getRealPath(
					"/tmp/cetak_data_" + java.util.UUID.randomUUID().toString() + ".xlsx");
			final File file = new File(filename);
			file.createNewFile();

			final Timer timer = new Timer(200);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						Clients.showBusy(label.getValue());

						if (trim(label.getValue()).equalsIgnoreCase("-")) {
							Clients.clearBusy();
							timer.detach();
						} else if (label.getValue().isEmpty()) {
							if (!downloadPath.getValue().isEmpty()) {
								try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); }
								catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan"); }
							}

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							siapkanBorderlayout(borderlayout);

							final MyWindow window = new MyWindow("Rekap Nilai", "none", true);
							Toolbar toolbar = buatToolbarAtas(borderlayout);

							Center center = new Center();
							siapkanCenterScrollable(center);
							center.setParent(borderlayout);

							tampilkanGridDariExcel(center, file, intbox, colSize);
							final MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai",
									"/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							styleToolbarButton(cancel, "#a94442");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();
								}
							});
							cancel.setParent(toolbar);

							MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Preview / Download Excel",
									"/img/excel.png");
							print.setTooltiptext("Buka preview Excel dan unduh file .xlsx");
							styleToolbarButton(print, "#1f5d7a");
							print.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									tampilkanPopupExcel(file, intbox, colSize);
								}
							});
							print.setParent(toolbar);

							for (final JenisItemPenilaianSiswa fn : fns) {
								// Tombol "Masukkan nilai" hanya untuk dosen/guru/admin — SEMBUNYIKAN dari
								// peserta didik (siswa/calon siswa/mahasiswa/calon mahasiswa).
								if (nilais.containsKey(fn.getId()) && !apakahLoginPesertaDidik()) {
									MyToolbarbuttonConfig proses = new MyToolbarbuttonConfig(
											"Masukkan nilai \"" + fn.getNama() + "\"", "/img/svg/check2.svg");
									styleToolbarButton(proses, "#2f6b2f");
									proses.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {

											if (kul.getDikunci() != null) {
												MyMessageboxConfig.show(
														"Penilaian untuk jadwal pelajaran ini telah terkunci",
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
												return;
											}

											Session session = null;
											Transaction tx = null;
											try {
												session = HibernateUtil.getSessionFactory().openSession();
												tx = session.beginTransaction();

												List<KategoriItemPenilaianSiswa> kategoriItemPenilaianSiswasId = ConstantValues
														.simpleList(session
																.createCriteria(
																		DetailGrupKategoriItemPenilaianSiswa.class)
																.add(Restrictions.eq("grupKategoriItemPenilaianSiswa",
																		grupKategoriItemPenilaianSiswa))
																.add(Restrictions.or(Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true)))
																.setProjection(Projections.groupProperty(
																		"kategoriItemPenilaianSiswa.id")),
																KategoriItemPenilaianSiswa.class, false);

												List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas = ConstantValues
														.simpleList(session
																.createCriteria(JenisItemPenilaianSiswa.class)
																.createAlias("kategoriItemPenilaianSiswa",
																		"kategoriItemPenilaianSiswa")
																.addOrder(Order.asc("kategoriItemPenilaianSiswa.kode"))
																.addOrder(Order.asc("nomorUrut"))
																.add(Restrictions.in("kategoriItemPenilaianSiswa",
																		kategoriItemPenilaianSiswasId))
																.add(Restrictions.or(Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true))),
																JenisItemPenilaianSiswa.class);

												int count = 0;
												for (Long kelasSiswaPunyaSiswaId : nilais.get(fn.getId()).keySet()) {
													KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) session
															.get(KelasSiswaPunyaSiswa.class, kelasSiswaPunyaSiswaId);
													if (kelasSiswaPunyaSiswa == null) {
														continue;
													}

													Double jumlah = nilais.get(fn.getId()).get(kelasSiswaPunyaSiswaId);
													Integer smt = kul.getSemester();
													Boolean sesuai = kelasSiswaPunyaSiswa.retreiveDetailVerify(fn,
															grupKategoriItemPenilaianSiswa, kul.getMatapelajaran(),
															smt);

													kelasSiswaPunyaSiswa.populateDetailNilai(fn, kul.getMatapelajaran(),
															grupKategoriItemPenilaianSiswa,
															jumlah == null ? "" : jumlah.toString(), sesuai, smt);

													Date sekarang = WaktuUtil.getDate();
													String formula = grupKategoriItemPenilaianSiswa.getFormula();
													String target = GrupPenilaianUtil.ambilTarget(formula, sekarang);
													Boolean hanyaValid = null;
													Double total = kelasSiswaPunyaSiswa.retreiveTotalNilai(
															jenisItemPenilaianSiswas, target, kul.getMatapelajaran(),
															grupPenilaian, grupKategoriItemPenilaianSiswa, smt,
															hanyaValid);

													kelasSiswaPunyaSiswa.populateDetailNilaiTotal(
															kul.getMatapelajaran(), grupKategoriItemPenilaianSiswa,
															total, sesuai, smt);

													session.update(kelasSiswaPunyaSiswa);

													// OPTIMASI MEMORI: Flush & Clear setiap 50 record
													count++;
													if (count % BATCH_SIZE == 0) {
														session.flush();
														session.clear();
													}
												}
												tx.commit();
											} catch (Exception e) {
												if (tx != null)
													tx.rollback();
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/GradingHelper.java:662");
											} finally {
												closeSession(session);
											}
										}
									});
									proses.setParent(toolbar);
								}
							}

							if (parent != null) {
								cancel.setVisible(false);
								borderlayout.setParent(parent);
								borderlayout.setWidth("100%");
								borderlayout.setHeight("520px");
							} else {
								siapkanWindowScrollable(window, "97%", "90%");
								borderlayout.setParent(window);
								window.setVisible(true);
								window.onModal();
							}

							Clients.clearBusy();
							timer.detach();
						}

					} catch (Exception e) {
						Clients.clearBusy();
					}

				}
			});
			timer.start();

			try {
				Clients.showBusy(label.getValue());

				new Thread(new Runnable() {
					@Override
					public void run() {
						Session session = null;
						FileOutputStream fileOut = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();

							XSSFWorkbook workbook = new XSSFWorkbook();
							XSSFSheet sheet = workbook.createSheet(Common.getBahasaConfig("Nilai"));

							XSSFCellStyle notLocked = workbook.createCellStyle();
							notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
							notLocked.setFillForegroundColor(new XSSFColor(Color.BLUE));

							XSSFCellStyle notLocked2 = workbook.createCellStyle();
							notLocked2.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
							notLocked2.setFillForegroundColor(new XSSFColor(Color.GREEN));

							XSSFCellStyle notLocked3 = workbook.createCellStyle();
							notLocked3.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
							notLocked3.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

							XSSFCellStyle notLocked4 = workbook.createCellStyle();
							notLocked4.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
							notLocked4.setFillForegroundColor(new XSSFColor(Color.YELLOW));

							sheet.setDefaultColumnWidth(20);

							List<Pertemuan> pertemuanTugas1 = new ArrayList<Pertemuan>();
							for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
								pertemuanTugas1.addAll(jadwalPelajaran.ambilPertemuanList());
							}

							XSSFRow rowhead = sheet.createRow((short) 0);
							rowhead.createCell(0).setCellValue(Common.getBahasaConfig("No."));
							rowhead.createCell(1).setCellValue(Common.getBahasaConfig("NIS"));
							rowhead.createCell(2).setCellValue(Common.getBahasaConfig("Nama Siswa"));

							Collection<Long> kelasSiswaPunyaSiswas = session.createCriteria(KelasSiswaPunyaSiswa.class)
									.createAlias("siswa", "siswa").setProjection(Projections.property("id"))
									.add(Restrictions.eq("kelasSiswa", kul.getKelas())).addOrder(Order.asc("nomorUrut"))
									.addOrder(Order.asc("siswa.nama")).list();

							intbox.setValue(kelasSiswaPunyaSiswas.size());

							Map<Long, Object[]> dataNomorKolom = new HashMap<Long, Object[]>();
							int indexTotal = 0;
							for (JenisItemPenilaianSiswa fn : fns) {

								int jumlahIndex = indexTotal;
								int index = (parent != null ? 5 : 3) + indexTotal;
								int jumlahPenmabahan = 0;
								Double totalPersen = 0.0;

								List<TugasKelompok> tugasKelompoks = new ArrayList<TugasKelompok>();
								List<PertemuanPunyaUjian> pertemuanPunyaUjians = new ArrayList<PertemuanPunyaUjian>();
								TreeMap<Long, PertemuanPunyaUjian> pertemuanPunyaUjiansLocal = new TreeMap<Long, PertemuanPunyaUjian>();

								for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {
									List<TugasKelompok> tugasKelompoksLocal = session
											.createCriteria(TugasKelompok.class).addOrder(Order.asc("judul"))
											.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
											.add(Restrictions.eq("jenisItemPenilaianSiswa", fn)).list();

									for (Pertemuan pertemuan : pertemuanTugas1) {
										TreeMap<Long, PertemuanPunyaUjian> pertemuanPunyaUjiansTemp = pertemuan
												.ambilPertemuanPunyaUjianTotal(tbmuser);
										for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjiansTemp
												.values()) {
											if (pertemuanPunyaUjian.getJenisItemPenilaianSiswa() != null && fn != null
													&& pertemuanPunyaUjian.getJenisItemPenilaianSiswa().getId()
															.equals(fn.getId())) {
												pertemuanPunyaUjiansLocal.put(pertemuanPunyaUjian.getId(),
														pertemuanPunyaUjian);
											}
										}
										pertemuanPunyaUjiansTemp = null;
									}
									tugasKelompoks.addAll(tugasKelompoksLocal);
								}

								pertemuanPunyaUjians.addAll(pertemuanPunyaUjiansLocal.values());
								pertemuanPunyaUjiansLocal = null;

								for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
									totalPersen += pertemuanPunyaUjian.getProsentase();
									XSSFCell cell;
									(cell = rowhead.createCell(index)).setCellValue(pertemuanPunyaUjian.getNama()
											+ "(\"" + fn.getNama() + "\", bobot:"
											+ Common.numberFormat.get().format(pertemuanPunyaUjian.getProsentase())
											+ ")");
									cell.setCellStyle(notLocked3);
									index++;
									indexTotal++;
									jumlahPenmabahan++;
								}

								XSSFCell cell;
								for (Pertemuan pertemuan : pertemuanTugas1) {
									if (hasText(pertemuan.getJudultugas())) {
										if (fn != null && pertemuan.getJenisItemPenilaianSiswa() != null && fn != null
												&& pertemuan.getJenisItemPenilaianSiswa().getId().equals(fn.getId())) {
											totalPersen += pertemuan.getProsentase();
											(cell = rowhead.createCell(index)).setCellValue(pertemuan.getJudultugas()
													+ "(\"" + fn.getNama() + "\", bobot:"
													+ Common.numberFormat.get().format(pertemuan.getProsentase())
													+ ")");
											cell.setCellStyle(notLocked3);
											index++;
											indexTotal++;
											jumlahPenmabahan++;
										}
									}

									for (TugasPertemuan tugasPertemuan : pertemuan.ambilTugasPertemuanTotal()
											.values()) {
										if (hasText(tugasPertemuan.getJudultugas())) {
											if (fn != null && tugasPertemuan.getJenisItemPenilaianSiswa() != null
													&& fn != null && tugasPertemuan.getJenisItemPenilaianSiswa().getId()
															.equals(fn.getId())) {
												totalPersen += tugasPertemuan.getProsentase();
												(cell = rowhead.createCell(index))
														.setCellValue(tugasPertemuan.getJudultugas() + "(\""
																+ fn.getNama() + "\", bobot:" + Common.numberFormat
																		.get().format(tugasPertemuan.getProsentase())
																+ ")");
												cell.setCellStyle(notLocked3);
												index++;
												indexTotal++;
												jumlahPenmabahan++;
											}
										}
									}
								}

								for (TugasKelompok tugasKelompok : tugasKelompoks) {
									totalPersen += tugasKelompok.getProsentase();
									(cell = rowhead.createCell(index)).setCellValue(tugasKelompok.getJudul() + "(\""
											+ fn.getNama() + "\", bobot:"
											+ Common.numberFormat.get().format(tugasKelompok.getProsentase()) + ")");
									cell.setCellStyle(notLocked3);
									index++;
									indexTotal++;
									jumlahPenmabahan++;
								}

								if (jumlahPenmabahan > 0) {
									(cell = rowhead.createCell(index + 0)).setCellValue("Total \"" + fn.getNama()
											+ "\", bobot:" + Common.numberFormat.get().format(totalPersen) + ")");
									cell.setCellStyle(notLocked);
									colSize.setValue(index + 1);
									indexTotal++;
								}

								int rowIndex = 0;
								Map<Long, XSSFRow> mapRow = new HashMap<Long, XSSFRow>();
								for (Long kelasSiswaPunyaSiswaid : kelasSiswaPunyaSiswas) {
									KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) session
											.get(KelasSiswaPunyaSiswa.class, kelasSiswaPunyaSiswaid);
									if (kelasSiswaPunyaSiswa != null) {
										if (siswa == null || (siswa != null
												&& siswa.getId().equals(kelasSiswaPunyaSiswa.getSiswa().getId()))) {
											rowIndex++;
											XSSFRow row = sheet.createRow(rowIndex);
											row.createCell(0).setCellValue(rowIndex);
											row.createCell(1).setCellValue(kelasSiswaPunyaSiswa.getSiswa().getNim());
											row.createCell(2).setCellValue(kelasSiswaPunyaSiswa.getSiswa().getNama());
											mapRow.put(kelasSiswaPunyaSiswa.getId(), row);
										}
									}
								}
								dataNomorKolom.put(fn.getId(), new Object[] { jumlahIndex, pertemuanPunyaUjians,
										tugasKelompoks, totalPersen, jumlahPenmabahan, mapRow });
							}

							for (JenisItemPenilaianSiswa fn : fns) {
								Object[] o = dataNomorKolom.get(fn.getId());
								Integer penambahan = (Integer) o[0];
								List<PertemuanPunyaUjian> pertemuanPunyaUjians = (List<PertemuanPunyaUjian>) o[1];
								List<TugasKelompok> tugasKelompoks = (List<TugasKelompok>) o[2];
								Double totalPersen = (Double) o[3];
								Integer jumlahPenmabahan = (Integer) o[4];
								Map<Long, XSSFRow> mapRow = (Map<Long, XSSFRow>) o[5];

								if (jumlahPenmabahan > 0) {
									int rowIndex = 0;
									for (Long kelasSiswaPunyaSiswaid : kelasSiswaPunyaSiswas) {
										KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa = (KelasSiswaPunyaSiswa) session
												.get(KelasSiswaPunyaSiswa.class, kelasSiswaPunyaSiswaid);
										if (kelasSiswaPunyaSiswa != null) {
											try {
												if (siswa == null || (siswa != null && siswa.getId()
														.equals(kelasSiswaPunyaSiswa.getSiswa().getId()))) {
													rowIndex++;
													label.setValue("Sedang memproses data "
															+ kelasSiswaPunyaSiswa.toString() + " ("
															+ Common.numberFormat.get().format(
																	rowIndex * 100.0 / kelasSiswaPunyaSiswas.size())
															+ " %)");

													Double totalPersenTidakIkutUjian = 0.0;
													Long idSiswa = kelasSiswaPunyaSiswa.getSiswa().getId();

													for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
														if (containsId(pertemuanPunyaUjian.getMhsYgTidakIkut(),
																idSiswa)) {
															totalPersenTidakIkutUjian += pertemuanPunyaUjian
																	.getProsentase();
														}
													}

													for (Pertemuan pertemuan : pertemuanTugas1) {
														if (hasText(pertemuan.getJudultugas()) && fn != null
																&& pertemuan.getJenisItemPenilaianSiswa() != null
																&& pertemuan.getJenisItemPenilaianSiswa().getId()
																		.equals(fn.getId())) {
															if (containsId(pertemuan.getMhsYgTidakIkut(), idSiswa)) {
																totalPersenTidakIkutUjian += pertemuan.getProsentase();
															}
														}
														for (TugasPertemuan tugasPertemuan : pertemuan
																.ambilTugasPertemuanTotal().values()) {
															if (hasText(tugasPertemuan.getJudultugas()) && fn != null
																	&& tugasPertemuan
																			.getJenisItemPenilaianSiswa() != null
																	&& tugasPertemuan.getJenisItemPenilaianSiswa()
																			.getId().equals(fn.getId())) {
																if (containsId(tugasPertemuan.getMhsYgTidakIkut(),
																		idSiswa)) {
																	totalPersenTidakIkutUjian += tugasPertemuan
																			.getProsentase();
																}
															}
														}
													}

													for (TugasKelompok tugasKelompok : tugasKelompoks) {
														if (containsId(tugasKelompok.getMhsYgTidakIkut(), idSiswa)) {
															totalPersenTidakIkutUjian += tugasKelompok.getProsentase();
														}
													}

													Double nilaiTotal = 0.0;
													Double nilaiSemua = 0.0;

													XSSFRow row = mapRow.get(kelasSiswaPunyaSiswa.getId());
													int index = (parent != null ? 5 : 3) + penambahan;

													for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
														if (!containsId(pertemuanPunyaUjian.getMhsYgTidakIkut(),
																idSiswa)) {
															Number nilaiUjian = (Number) session
																	.createCriteria(HasilUjianMahasiswa.class)
															.add(Restrictions.or(Restrictions.isNotNull("keyhasil"),
																	Restrictions.isNotNull("nilaiObe")))
																	.setMaxResults(1)
																	.setProjection(Projections.property("nilai"))
																	.add(Restrictions.eq("pertemuanPunyaUjian",
																			pertemuanPunyaUjian))
																	.add(Restrictions.eq("siswa",
																			kelasSiswaPunyaSiswa.getSiswa()))
																	.uniqueResult();
															if (nilaiUjian != null) {
																Double nilai = safeDivide(
																		(nilaiUjian.doubleValue()
																				* pertemuanPunyaUjian.getProsentase()),
																		totalPersen - totalPersenTidakIkutUjian);
																nilaiSemua += nilai;
																nilaiTotal += nilaiUjian.doubleValue();
																XSSFCell cell = row.createCell(index);
																cell.setCellValue(nilaiUjian.doubleValue());
																cell.setCellStyle(notLocked3);
															} else {
																XSSFCell cell = row.createCell(index);
																cell.setCellValue(0.0);
																cell.setCellStyle(notLocked3);
															}
														} else {
															XSSFCell cell = row.createCell(index);
															cell.setCellValue("Tidak perlu ikut");
															cell.setCellStyle(notLocked4);
														}
														index++;
													}

													for (Pertemuan pertemuan : pertemuanTugas1) {
														if (hasText(pertemuan.getJudultugas()) && fn != null
																&& pertemuan.getJenisItemPenilaianSiswa() != null
																&& pertemuan.getJenisItemPenilaianSiswa().getId()
																		.equals(fn.getId())) {
															if (!containsId(pertemuan.getMhsYgTidakIkut(), idSiswa)) {
																TugasFileContent tugasFileContent = pertemuan
																		.ambilTugasFileContent(
																				kelasSiswaPunyaSiswa.getSiswa());
																Double nilaiTugas = tugasFileContent == null ? 0.0
																		: tugasFileContent.getNilai();

																Double nilai = safeDivide(
																		(nilaiTugas.doubleValue()
																				* pertemuan.getProsentase()),
																		totalPersen - totalPersenTidakIkutUjian);
																nilaiSemua += nilai;
																nilaiTotal += nilaiTugas.doubleValue();
																XSSFCell cell = row.createCell(index);
																cell.setCellValue(nilaiTugas.doubleValue());
																cell.setCellStyle(notLocked3);
															} else {
																XSSFCell cell = row.createCell(index);
																cell.setCellValue("Tidak perlu ikut");
																cell.setCellStyle(notLocked4);
															}
															index++;
														}

														for (TugasPertemuan tugasPertemuan : pertemuan
																.ambilTugasPertemuanTotal().values()) {
															if (hasText(tugasPertemuan.getJudultugas()) && fn != null
																	&& tugasPertemuan
																			.getJenisItemPenilaianSiswa() != null
																	&& tugasPertemuan.getJenisItemPenilaianSiswa()
																			.getId().equals(fn.getId())) {
																if (!containsId(tugasPertemuan.getMhsYgTidakIkut(),
																		idSiswa)) {
																	// FIX compile "cannot find symbol: jsonObjectTugas": deklarasi hilang di
																	// titik ini -- pola sama seperti 3 titik lain di file ini (baris ~2309,
																	// ~2440, ~2572), baca dari keteranganNilai milik tugasPertemuan ini.
																	JSONObject jsonObjectTugas = createJson(tugasPertemuan.getKeteranganNilai());
																	TugasFileContent tugasFileContent = tugasPertemuan
																			.ambilTugasFileContent(
																					kelasSiswaPunyaSiswa.getSiswa());
																	Double nilaiTugas = 0.0;
																	if (tugasFileContent != null) {
																		String keyKet = "";
																		if (tugasFileContent.getMahasiswa() != null)
																			keyKet = tugasFileContent.getMahasiswa() + "_mhs";
																		else if (tugasFileContent.getSiswa() != null)
																			keyKet = tugasFileContent.getSiswa() + "_siswa";
																		else if (tugasFileContent.getBiodataCalonMahasiswa() != null)
																			keyKet = tugasFileContent.getBiodataCalonMahasiswa() + "_cal_mhs";
																		else if (tugasFileContent.getCalonSiswa() != null)
																			keyKet = tugasFileContent.getCalonSiswa() + "_cal_siswa";
																		nilaiTugas = (!keyKet.isEmpty() && !jsonObjectTugas.isNull(keyKet + "_nilai"))
																				? jsonObjectTugas.getDouble(keyKet + "_nilai")
																				: tugasFileContent.getNilai();
																	}

																	Double nilai = safeDivide(
																			(nilaiTugas.doubleValue()
																					* tugasPertemuan.getProsentase()),
																			totalPersen - totalPersenTidakIkutUjian);
																	nilaiSemua += nilai;
																	nilaiTotal += nilaiTugas.doubleValue();
																	XSSFCell cell = row.createCell(index);
																	cell.setCellValue(nilaiTugas.doubleValue());
																	cell.setCellStyle(notLocked3);
																} else {
																	XSSFCell cell = row.createCell(index);
																	cell.setCellValue("Tidak perlu ikut");
																	cell.setCellStyle(notLocked4);
																}
																index++;
															}
														}
													}

													for (TugasKelompok tugasKelompok : tugasKelompoks) {
														if (!containsId(tugasKelompok.getMhsYgTidakIkut(), idSiswa)) {
															Number nilaiTugas = (Number) session
																	.createCriteria(
																			NamaTugasKelompokPunyaMahasiswa.class)
																	.setMaxResults(1)
																	.setProjection(Projections.property("nilai"))
																	.createAlias("namaTugasKelompok",
																			"namaTugasKelompok")
																	.add(Restrictions.eq(
																			"namaTugasKelompok.tugasKelompok",
																			tugasKelompok))
																	.add(Restrictions.eq("siswa",
																			kelasSiswaPunyaSiswa.getSiswa()))
																	.uniqueResult();
															if (nilaiTugas != null) {
																Double nilai = safeDivide(
																		(nilaiTugas.doubleValue()
																				* tugasKelompok.getProsentase()),
																		totalPersen - totalPersenTidakIkutUjian);
																nilaiSemua += nilai;
																nilaiTotal += nilaiTugas.doubleValue();
																XSSFCell cell = row.createCell(index);
																cell.setCellValue(nilaiTugas.doubleValue());
																cell.setCellStyle(notLocked3);
															} else {
																XSSFCell cell = row.createCell(index);
																cell.setCellValue(0.0);
																cell.setCellStyle(notLocked3);
															}
														} else {
															XSSFCell cell = row.createCell(index);
															cell.setCellValue("Tidak perlu ikut");
															cell.setCellStyle(notLocked4);
														}
														index++;
													}

													XSSFCell cell = row.createCell(index + 0);
													cell.setCellValue(nilaiSemua);
													cell.setCellStyle(notLocked);

													if (nilais.containsKey(fn.getId())) {
														nilais.get(fn.getId()).put(kelasSiswaPunyaSiswa.getId(),
																nilaiSemua);
													} else {
														Map<Long, Double> map = new HashMap<Long, Double>();
														map.put(kelasSiswaPunyaSiswa.getId(), nilaiSemua);
														nilais.put(fn.getId(), map);
													}
													report.sukses(rowIndex, "NIS: " + kelasSiswaPunyaSiswa.getSiswa().getNim() + " | " + fn.getNama(), "nilai=" + nilaiSemua);
												}
												// OPTIMASI MEMORI BATCH CLEAR PADA LOOPS
												if (rowIndex % BATCH_SIZE == 0) {
													session.flush();
													session.clear();
												}
											} catch (Exception e) {
												report.gagal(rowIndex, kelasSiswaPunyaSiswa != null ? "NIS: " + kelasSiswaPunyaSiswa.getSiswa().getNim() : "id=" + kelasSiswaPunyaSiswaid, e, "Periksa data siswa.");
												Common.tampilErrorJikaAdmin(e);
											}
										}
									}
								}
							}

							dataNomorKolom = null;

							try {
								fileOut = new FileOutputStream(filename);
								workbook.write(fileOut);
							} catch (IOException e) {
								Common.tampilErrorJikaAdmin(e);
							}

							kelasSiswaPunyaSiswas.clear();
							kelasSiswaPunyaSiswas = null;
							try {
								java.io.File rptFile = report.simpanLaporan();
								downloadPath.setValue(rptFile.getAbsolutePath());
							} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) GradingHelper laporan"); }
							label.setValue("");
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							try {
								java.io.File rptFile = report.simpanLaporan();
								downloadPath.setValue(rptFile.getAbsolutePath());
							} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) GradingHelper laporan"); }
							label.setValue("");
						} finally {
							if (fileOut != null) {
								try {
									fileOut.close();
								} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/GradingHelper.java:1130");
								}
							}
							closeSession(session);
						}
					}
				}).start();

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		}
	}

	public static void hitungNilaiBerdasarkanFormatNilaiObe(final Perkuliahan perkuliahan, String formatNilaisData)
			throws Exception {
		List<FormatNilai> obeFormatNilais = new ArrayList<FormatNilai>();
		Session session = null;
		List<FormatNilai> formatNilais = new ArrayList<FormatNilai>();
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			formatNilais = Common.getFormatNilais(session, perkuliahan);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/GradingHelper.java:1154");
		} finally {
			closeSession(session);
		}

		JSONObject jsonObject = createJson(formatNilaisData);
		for (FormatNilai nilai : formatNilais) {
			if (nilai.getStatusPertemuan() != null) {
				if (!jsonObject.isNull(nilai.getId().toString())) {
					obeFormatNilais.add(nilai);
				}
			}
		}

		final MyWindow window = new MyWindow("Singkronkan Nilai OBE", "none", true);
		siapkanWindowScrollable(window, "95%", "95%");
		window.setStyle("overflow:hidden;");
		window.setContentStyle("overflow:hidden; padding:0; background:#f4f7fb;");

		/*
		 * Scroll OBE sengaja tidak memakai Borderlayout/Center lagi. Pada beberapa
		 * versi ZK, Center + child height 100% membuat tinggi konten ikut membesar
		 * mengikuti seluruh section sehingga scrollbar vertical window tidak muncul.
		 * Layout flex di bawah ini memberi tinggi pasti pada area body, lalu body
		 * tersebut yang menjadi container scroll untuk semua grid OBE vertical.
		 */
		Div windowLayout = new Div();
		windowLayout.setParent(window);
		windowLayout.setWidth("100%");
		windowLayout.setHeight("100%");
		windowLayout.setStyle("height:100%; max-height:calc(95vh - 8px); display:flex; flex-direction:column; "
				+ "overflow:hidden; background:#f4f7fb; box-sizing:border-box;");

		Div toolbarWrapper = new Div();
		toolbarWrapper.setParent(windowLayout);
		toolbarWrapper.setWidth("100%");
		toolbarWrapper.setStyle("flex:0 0 auto; background:#e9eef4; border-bottom:1px solid #d7dee8; "
				+ "padding:8px 12px; box-sizing:border-box;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(toolbarWrapper);
		toolbar.setWidth("100%");
		toolbar.setStyle("background:transparent; border:0; padding:0; display:block; white-space:normal;");

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		styleToolbarButton(cancel, "#a94442");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		final java.util.concurrent.ConcurrentHashMap<Long, Map<Long, Double>> nilaisAll
				= new java.util.concurrent.ConcurrentHashMap<Long, Map<Long, Double>>();
		final int totalSectionsBerisi = obeFormatNilais.size();
		final List<FormatNilai> obeFormatNilaisRef = obeFormatNilais;

		// ── Hitung Ulang Ujian & Tugas OBE untuk perkuliahan ini (REUSE HitungUlangNilaiObeHelper) ──
		// Menghitung ulang HasilUjianMahasiswa (PG) + Tugas/Tugas Kelompok OBE lalu MENYIMPAN, kemudian
		// membuka ulang layar ini agar grid ter-update. DISEMBUNYIKAN untuk Siswa/Mahasiswa.
		{
			ais.database.model.Tbmuser penggunaKini = Common.getCurrentUser();
			boolean pesertaDidik = penggunaKini != null
					&& (penggunaKini.getMahasiswa() != null || penggunaKini.getSiswa() != null);
			if (!pesertaDidik && perkuliahan != null && perkuliahan.getId() != null) {
				final String formatNilaisDataFinal = formatNilaisData;
				MyToolbarbuttonConfig btnHitungUlang = new MyToolbarbuttonConfig("Hitung Ulang Ujian dan Tugas",
						"/img/svg/check2-circle.svg");
				btnHitungUlang.setTooltiptext("Hitung ulang semua nilai Ujian (HasilUjianMahasiswa) & Tugas "
						+ "(termasuk Tugas Kelompok) OBE untuk perkuliahan ini, lalu perbarui grid");
				styleToolbarButton(btnHitungUlang, "#2563eb");
				btnHitungUlang.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
								"Hitung ulang semua nilai Ujian & Tugas OBE untuk perkuliahan ini? "
										+ "Nilai akan diperbarui.",
								"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
								MyMessageboxConfig.QUESTION, new EventListener() {
									@Override
									public void onEvent(Event ev) throws Exception {
										if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
											return;
										}
										ais.action.master.obe.HitungUlangNilaiObeHelper.hitungUlangSimpanDialog(
												java.util.Collections.singletonList(perkuliahan.getId()),
												"Hitung Ulang Ujian dan Tugas", new EventListener() {
													@Override
													public void onEvent(Event a) throws Exception {
														// Refresh grid: tutup lalu buka ulang layar ini.
														window.detach();
														GradingHelper.hitungNilaiBerdasarkanFormatNilaiObe(
																perkuliahan, formatNilaisDataFinal);
													}
												});
									}
								});
					}
				});
				btnHitungUlang.setParent(toolbar);
				final Perkuliahan kuliyahSemua = perkuliahan.getMerupakan_paralel()
					&& perkuliahan.getPerkuliahan_paralel() != null
					? perkuliahan.getPerkuliahan_paralel() : perkuliahan;
				MyToolbarbuttonConfig btnMasukkanSemua = new MyToolbarbuttonConfig(
					"Masukkan Semua Nilai", "/img/svg/check2-circle.svg");
				btnMasukkanSemua.setTooltiptext(
					"Masukkan nilai semua Sub-CPMK sekaligus (pastikan semua bagian telah selesai dimuat)");
				styleToolbarButton(btnMasukkanSemua, "#155724");
				btnMasukkanSemua.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						if (nilaisAll.size() < totalSectionsBerisi) {
							MyMessageboxConfig.show(
								"Belum semua bagian selesai dimuat (" + nilaisAll.size() + "/" + totalSectionsBerisi
								+ "). Harap tunggu hingga semua grid tampil.", "Info",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						if (kuliyahSemua.getDikunci() != null) {
							MyMessageboxConfig.show(
								"Penilaian untuk perkuliahan ini telah terkunci", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}
						MyMessageboxConfig.show(
							"Masukkan nilai semua Sub-CPMK (" + nilaisAll.size() + " bagian) sekaligus?",
							"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								@Override
								public void onEvent(Event ev) throws Exception {
									if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
										return;
									}
									Session sessionSemua = null;
									Transaction txSemua = null;
									try {
										sessionSemua = HibernateUtil.getSessionFactory().openSession();
										txSemua = sessionSemua.beginTransaction();
										List<FormatNilai> formatNilaisDb = kuliyahSemua.ambilFormatNilai(sessionSemua);
										if (!Detailperkuliahan.formatNilaiSiapDihitung(formatNilaisDb)) {
											throw new IllegalStateException(
													"Input OBE massal dibatalkan: format nilai kosong atau total bobot bukan 100%. Perkuliahan="
															+ kuliyahSemua.getId());
										}
										Tbmuser tbmuserSemua = Common.getCurrentUser();
										int countSemua = 0;
										for (FormatNilai fn : obeFormatNilaisRef) {
											if (fn.getId() == null || !nilaisAll.containsKey(fn.getId())) {
												continue;
											}
											Map<Long, Double> detailNilaisForFn = nilaisAll.get(fn.getId());
											for (Long detailperkuliahanId : detailNilaisForFn.keySet()) {
												Detailperkuliahan dp = (Detailperkuliahan) sessionSemua
													.get(Detailperkuliahan.class, detailperkuliahanId);
												if (dp == null) { continue; }
												Double nilaiSemua = detailNilaisForFn.get(detailperkuliahanId);
												dp.populateDetailNilai(fn, null,
													nilaiSemua == null ? 0.0 : nilaiSemua.doubleValue(), true, tbmuserSemua);
												Matakuliah matakuliah = dp.getPerkuliahan() != null
													? dp.getPerkuliahan().getMatakuliah() : dp.getMatakuliahKonversi();
												Double total = dp.hitungTotalNilai(true, formatNilaisDb);
												NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
													dp.getMahasiswa().getTahunangkatan(), dp.getMahasiswa().getJurusan(),
													dp.getMahasiswa().getJurusan().getFakultas(), dp.getTahunAkademik(),
													dp.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
													matakuliah == null ? "" : matakuliah.getKode(),
													matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
												dp.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
												dp.setTotalNilai(total);
												dp.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
												dp.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());
												Double totalSementara = dp.hitungTotalNilaiSementara(true, formatNilaisDb);
												NilaiHuruf nhSementara = Common.getNilaiHuruf(totalSementara,
													dp.getMahasiswa().getTahunangkatan(), dp.getMahasiswa().getJurusan(),
													dp.getMahasiswa().getJurusan().getFakultas(), dp.getTahunAkademik(),
													dp.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
													matakuliah == null ? "" : matakuliah.getKode(),
													matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());
												dp.setTotalNilaiSementara(totalSementara);
												dp.setNilaiHurufSementara(nhSementara == null ? "" : nhSementara.getNilaiHuruf());
												dp.setTotalIPSementara(nhSementara == null ? 0.0 : nhSementara.getNilaiDiIPK());
												sessionSemua.update(dp);
												countSemua++;
												if (countSemua % BATCH_SIZE == 0) {
													sessionSemua.flush();
													sessionSemua.clear();
												}
											}
										}
										txSemua.commit();
									} catch (Exception e) {
										if (txSemua != null) txSemua.rollback();
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e,
											"auto-audit src/ais/common/GradingHelper.java:btnMasukkanSemua");
									} finally {
										closeSession(sessionSemua);
									}
									Common.createDefaultTimer(new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											DetailperkuliahanForPenilaianHelper.onLaporan(kuliyahSemua, null, true);
										}
									});
								}
							});
					}
				});
				btnMasukkanSemua.setParent(toolbar);
			}
		}

		Div scrollWrapper = new Div();
		scrollWrapper.setParent(windowLayout);
		scrollWrapper.setWidth("100%");
		scrollWrapper.setHeight("100%");
		scrollWrapper.setStyle("flex:1 1 auto; min-height:0; max-height:calc(95vh - 64px); "
				+ "overflow-y:scroll; overflow-x:auto; padding:12px 16px 28px 16px; "
				+ "box-sizing:border-box; background:#f4f7fb;");

		Html info = new Html("<div style='padding:12px 14px; margin-bottom:12px; border:1px solid #cfe5f2; "
				+ "background:#eef9ff; color:#1f5d7a; border-radius:6px;'>"
				+ "<b>Sinkronkan Nilai OBE</b><br/>Semua komponen nilai ditampilkan dalam satu halaman vertikal. "
				+ "Gunakan tombol <b>Preview / Download Excel</b> dan <b>Masukkan nilai</b> pada bagian atas masing-masing grid."
				+ "</div>");
		info.setParent(scrollWrapper);

		if (obeFormatNilais.isEmpty()) {
			Html kosong = new Html("<div style='padding:16px; border:1px solid #f0d6a8; background:#fff8e8; "
					+ "color:#8a6d3b; border-radius:6px;'>Tidak ada format nilai OBE yang dapat disinkronkan.</div>");
			kosong.setParent(scrollWrapper);
		}

		for (final FormatNilai formatNilai : obeFormatNilais) {
			Div section = new Div();
			section.setParent(scrollWrapper);
			section.setWidth("100%");
			section.setStyle("margin-bottom:18px; border-radius:8px; overflow:hidden; background:#ffffff; "
					+ "border:1px solid #dfe6ee; box-shadow:0 1px 3px rgba(0,0,0,.08); min-width:900px;");

			Html title = new Html("<div style='background:#2f4358; color:#ffffff; padding:10px 12px;'>"
					+ "<div style='font-weight:bold; font-size:14px;'>" + escapeHtml(formatNilai.getNama()) + "</div>"
					+ "<div style='font-size:12px; opacity:.95; margin-top:2px;'>Preview grid nilai dan tombol sinkronisasi tersedia di bagian atas grid.</div>"
					+ "</div>");
			title.setParent(section);

			Div detailRekapNilai = new Div();
			detailRekapNilai.setParent(section);
			detailRekapNilai.setWidth("100%");
			detailRekapNilai.setHeight("540px");
			detailRekapNilai.setStyle("height:540px; min-height:540px; background:#ffffff; overflow:hidden;");

			GradingHelper.hitungNilaiBerdasarkanFormatNilai(detailRekapNilai, perkuliahan, nilaisAll, formatNilai);
		}

		window.setVisible(true);
		window.onModal();
	}

	public static void hitungNilaiBerdasarkanFormatNilai(Perkuliahan perkuliahan, final FormatNilai... fns)
			throws Exception {
		hitungNilaiBerdasarkanFormatNilai(null, perkuliahan, fns);
	}

	public static void hitungNilaiBerdasarkanFormatNilai(final Component parent, final Perkuliahan perkuliahan,
			final FormatNilai... fns) throws Exception {
		hitungNilaiBerdasarkanFormatNilai(parent, perkuliahan, null, fns);
	}

	public static void hitungNilaiBerdasarkanFormatNilai(final Component parent, final Perkuliahan perkuliahan,
			final Map<Long, Map<Long, Double>> sharedNilais, final FormatNilai fn) throws Exception {
		hitungNilaiBerdasarkanFormatNilai(parent, perkuliahan, null, sharedNilais, fn);
	}

	@SuppressWarnings("unchecked")
	public static void hitungNilaiBerdasarkanFormatNilai(final Component parent, Perkuliahan kul,
			final Mahasiswa mahasiswa, final FormatNilai... fns) throws Exception {
		hitungNilaiBerdasarkanFormatNilai(parent, kul, mahasiswa, null, fns);
	}

	@SuppressWarnings("unchecked")
	public static void hitungNilaiBerdasarkanFormatNilai(final Component parent, Perkuliahan kul,
			final Mahasiswa mahasiswa, final Map<Long, Map<Long, Double>> sharedNilais, final FormatNilai... fns) throws Exception {

		if (fns != null && fns.length > 0 && kul != null) {

			final Perkuliahan kuliyah = kul.getMerupakan_paralel() && kul.getPerkuliahan_paralel() != null
					? kul.getPerkuliahan_paralel()
					: kul;
			final List<Perkuliahan> perkuliahans = kuliyah.ambilParalelPerkuliahan();
			if (!perkuliahans.contains(kuliyah)) {
				perkuliahans.add(kuliyah);
			}

			final Map<Long, Map<Long, Double>> nilais = new HashMap<Long, Map<Long, Double>>();
			final Tbmuser tbmuser = Common.getCurrentUser();
			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
			final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Hitung Ulang Nilai Mahasiswa");
			final Label downloadPath = new Label("");
			final Intbox intbox = new Intbox(10);
			final Intbox colSize = new Intbox(10);
			Clients.showBusy(label.getValue());

			final String filename = Sessions.getCurrent().getWebApp().getRealPath(
					"/tmp/cetak_data_" + java.util.UUID.randomUUID().toString() + ".xlsx");
			final File file = new File(filename);
			file.createNewFile();

			final boolean prosentaseMengikutiDefaultPertemuan = Common.bolehKonfigurasi("prosentase_mengikuti_default_jumlah_pertemuan", Konfigurasi.TIDAK_AKTIF);

			final Timer timer = new Timer(200);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						Clients.showBusy(label.getValue());

						if (trim(label.getValue()).equalsIgnoreCase("-")) {
							Clients.clearBusy();
							timer.detach();
						} else if (label.getValue().isEmpty()) {
							if (!downloadPath.getValue().isEmpty()) {
								try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); }
								catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan"); }
							}

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							siapkanBorderlayout(borderlayout);

							final MyWindow window = new MyWindow("Rekap Nilai", "none", true);
							Toolbar toolbar = buatToolbarAtas(borderlayout);

							Center center = new Center();
							siapkanCenterScrollable(center);
							center.setParent(borderlayout);

							tampilkanGridDariExcel(center, file, intbox, colSize);
							final MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai",
									"/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							styleToolbarButton(cancel, "#a94442");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();
								}
							});
							cancel.setParent(toolbar);

							MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Preview / Download Excel",
									"/img/excel.png");
							print.setTooltiptext("Buka preview Excel dan unduh file .xlsx");
							styleToolbarButton(print, "#1f5d7a");
							print.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									tampilkanPopupExcel(file, intbox, colSize);
								}
							});
							print.setParent(toolbar);

							for (final FormatNilai fn : fns) {
								// Tombol "Masukkan nilai" hanya untuk dosen/guru/admin — SEMBUNYIKAN dari
								// peserta didik (mahasiswa/calon mahasiswa/siswa/calon siswa).
								if (nilais.containsKey(fn.getId()) && !apakahLoginPesertaDidik()) {
									MyToolbarbuttonConfig proses = new MyToolbarbuttonConfig(
											"Masukkan nilai \"" + fn.getNama() + "\"", "/img/svg/check2.svg");
									styleToolbarButton(proses, "#2f6b2f");
									proses.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {

											if (kuliyah.getDikunci() != null) {
												MyMessageboxConfig.show(
														"Penilaian untuk perkuliahan ini telah terkunci", "Peringatan",
														MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												return;
											}
											if (fn.getKunci() != null) {
												MyMessageboxConfig.show(
														"Kolom nilai ini telah dikunci dan tidak dapat disinkronkan",
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
												return;
											}

											Session session = null;
											Transaction tx = null;
											try {
												session = HibernateUtil.getSessionFactory().openSession();
												tx = session.beginTransaction();

												List<FormatNilai> formatNilais = kuliyah.ambilFormatNilai(session);
												if (!Detailperkuliahan.formatNilaiSiapDihitung(formatNilais)) {
													throw new IllegalStateException(
															"Sinkronisasi OBE dibatalkan: format nilai kosong atau total bobot bukan 100%. Perkuliahan="
																	+ kuliyah.getId());
												}

												int count = 0;
												for (Long detailperkuliahanId : nilais.get(fn.getId()).keySet()) {
													Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
															.get(Detailperkuliahan.class, detailperkuliahanId);
													if (detailperkuliahan == null) {
														continue;
													}

													Double nilaiSemua = nilais.get(fn.getId()).get(detailperkuliahanId);
													detailperkuliahan.populateDetailNilai(fn, null,
															nilaiSemua == null ? 0.0 : nilaiSemua.doubleValue(), true,
															tbmuser);

													Matakuliah matakuliah = detailperkuliahan == null ? null
															: detailperkuliahan.getPerkuliahan() != null
																	? detailperkuliahan.getPerkuliahan().getMatakuliah()
																	: detailperkuliahan.getMatakuliahKonversi();

													Double total = detailperkuliahan.hitungTotalNilai(true,
															formatNilais);
													NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
															detailperkuliahan.getMahasiswa().getTahunangkatan(),
															detailperkuliahan.getMahasiswa().getJurusan(),
															detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
															detailperkuliahan.getTahunAkademik(),
															detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
																	: Perkuliahan.GANJIL,
															matakuliah == null ? "" : matakuliah.getKode(),
															matakuliah == null ? null
																	: matakuliah.getJenisNilaiHuruf());

													detailperkuliahan.setTotalIP(
															nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
													detailperkuliahan.setTotalNilai(total);
													detailperkuliahan.setNilaiHuruf(
															nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
													detailperkuliahan.setLulus(
															nilaiHuruf == null ? null : nilaiHuruf.getLulus());

													Double totalSementara = detailperkuliahan
															.hitungTotalNilaiSementara(true, formatNilais);
													nilaiHuruf = Common.getNilaiHuruf(totalSementara,
															detailperkuliahan.getMahasiswa().getTahunangkatan(),
															detailperkuliahan.getMahasiswa().getJurusan(),
															detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
															detailperkuliahan.getTahunAkademik(),
															detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
																	: Perkuliahan.GANJIL,
															matakuliah == null ? "" : matakuliah.getKode(),
															matakuliah == null ? null
																	: matakuliah.getJenisNilaiHuruf());

													detailperkuliahan.setTotalNilaiSementara(totalSementara);
													detailperkuliahan.setNilaiHurufSementara(
															nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
													detailperkuliahan.setTotalIPSementara(
															nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

													session.update(detailperkuliahan);

													count++;
													if (count % BATCH_SIZE == 0) {
														session.flush();
														session.clear();
													}
												}
												tx.commit();
											} catch (Exception e) {
												if (tx != null)
													tx.rollback();
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/GradingHelper.java:1487");
											} finally {
												closeSession(session);
											}

											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													DetailperkuliahanForPenilaianHelper.onLaporan(kuliyah, null, true);
												}
											});

										}
									});
									proses.setParent(toolbar);
								}
							}

							if (sharedNilais != null) {
								sharedNilais.putAll(nilais);
							}

							if (parent != null) {
								cancel.setVisible(false);
								borderlayout.setParent(parent);
								borderlayout.setWidth("100%");
								borderlayout.setHeight("520px");
							} else {
								siapkanWindowScrollable(window, "97%", "90%");
								borderlayout.setParent(window);
								window.setVisible(true);
								window.onModal();
							}

							Clients.clearBusy();
							timer.detach();
						}

					} catch (Exception e) {
						Clients.clearBusy();
					}

				}
			});
			timer.start();

			try {
				Clients.showBusy(label.getValue());

				new Thread(new Runnable() {
					@Override
					public void run() {
						Session session = null;
						FileOutputStream fileOut = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();

							Collection<Long> detailperkuliahans = kuliyah.ambilDetailperkuliahan();
							intbox.setValue(detailperkuliahans.size());

							XSSFWorkbook workbook = new XSSFWorkbook();
							XSSFSheet sheet = workbook.createSheet(Common.getBahasaConfig("Nilai"));

							XSSFCellStyle notLocked = workbook.createCellStyle();
							notLocked.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
							notLocked.setFillForegroundColor(new XSSFColor(Color.BLUE));

							XSSFCellStyle notLocked2 = workbook.createCellStyle();
							notLocked2.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
							notLocked2.setFillForegroundColor(new XSSFColor(Color.GREEN));

							XSSFCellStyle notLocked3 = workbook.createCellStyle();
							notLocked3.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
							notLocked3.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));

							XSSFCellStyle notLocked4 = workbook.createCellStyle();
							notLocked4.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
							notLocked4.setFillForegroundColor(new XSSFColor(Color.YELLOW));

							sheet.setDefaultColumnWidth(20);

							List<Pertemuan> pertemuanTugas1 = new ArrayList<Pertemuan>();
							for (Perkuliahan perkuliahan : perkuliahans) {
								pertemuanTugas1.addAll(perkuliahan.ambilPertemuanList());
							}

							XSSFRow rowhead = sheet.createRow((short) 0);
							rowhead.createCell(0).setCellValue(Common.getBahasaConfig("No."));
							rowhead.createCell(1).setCellValue(Common.getBahasaConfig("NIM"));
							rowhead.createCell(2).setCellValue(Common.getBahasaConfig("Nama Mahasiswa"));

							if (parent != null) {
								XSSFCell cellKehadiran;
								(cellKehadiran = rowhead.createCell(3))
										.setCellValue(Common.getBahasaConfig("Rekap Kehadiran"));
								cellKehadiran.setCellStyle(notLocked2);
								(cellKehadiran = rowhead.createCell(4))
										.setCellValue(Common.getBahasaConfig("(%) Kehadiran"));
								cellKehadiran.setCellStyle(notLocked2);
							}

							Map<Long, Object[]> dataNomorKolom = new HashMap<Long, Object[]>();
							int indexTotal = 0;

							for (FormatNilai fnData : fns) {

								int jumlahIndex = indexTotal;
								int index = (parent != null ? 5 : 3) + indexTotal;
								int jumlahPenmabahan = 0;
								Double totalPersen = 0.0;

								List<TugasKelompok> tugasKelompoks = new ArrayList<TugasKelompok>();
								List<PertemuanPunyaUjian> pertemuanPunyaUjians = new ArrayList<PertemuanPunyaUjian>();
								TreeMap<Long, PertemuanPunyaUjian> pertemuanPunyaUjiansLocal = new TreeMap<Long, PertemuanPunyaUjian>();

								for (Perkuliahan perkuliahan : perkuliahans) {

									if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum()
											.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
										List<TugasKelompok> tugasKelompoksLocal = session
												.createCriteria(TugasKelompok.class).addOrder(Order.asc("judul"))
												.add(Restrictions.eq("perkuliahan", perkuliahan)).list();

										for (TugasKelompok tugasKelompok : tugasKelompoksLocal) {
											JSONObject jsonObject = createJson(tugasKelompok.getFormatNilais());
											if (!jsonObject.isNull(fnData.getId().toString())) {
												tugasKelompoks.add(tugasKelompok);
											}
										}

										for (Pertemuan pertemuan : pertemuanTugas1) {
											TreeMap<Long, PertemuanPunyaUjian> pertemuanPunyaUjiansTemp = pertemuan
													.ambilPertemuanPunyaUjianTotal(tbmuser);
											for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjiansTemp
													.values()) {
												JSONObject jsonObject = createJson(
														pertemuanPunyaUjian.getFormatNilais());
												if (!jsonObject.isNull(fnData.getId().toString())) {
													pertemuanPunyaUjiansLocal.put(pertemuanPunyaUjian.getId(),
															pertemuanPunyaUjian);
												}
											}
											pertemuanPunyaUjiansTemp = null;
										}

									} else {

										List<TugasKelompok> tugasKelompoksLocal = session
												.createCriteria(TugasKelompok.class).addOrder(Order.asc("judul"))
												.createAlias("formatNilai", "formatNilai")
												.add(Restrictions.eq("perkuliahan", perkuliahan)).add(Restrictions
														.eq("formatNilai.statusPertemuan", fnData.getStatusPertemuan()))
												.list();

										for (Pertemuan pertemuan : pertemuanTugas1) {
											TreeMap<Long, PertemuanPunyaUjian> pertemuanPunyaUjiansTemp = pertemuan
													.ambilPertemuanPunyaUjianTotal(tbmuser);
											for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjiansTemp
													.values()) {
												if (pertemuanPunyaUjian.getFormatNilai() != null
														&& fnData.getStatusPertemuan() != null
														&& pertemuanPunyaUjian.getFormatNilai()
																.getStatusPertemuan() != null
														&& pertemuanPunyaUjian.getFormatNilai().getStatusPertemuan()
																.getId().equals(fnData.getStatusPertemuan().getId())) {
													pertemuanPunyaUjiansLocal.put(pertemuanPunyaUjian.getId(),
															pertemuanPunyaUjian);
												}
											}
											pertemuanPunyaUjiansTemp = null;
										}
										tugasKelompoks.addAll(tugasKelompoksLocal);
									}
								}

								pertemuanPunyaUjians.addAll(pertemuanPunyaUjiansLocal.values());
								pertemuanPunyaUjiansLocal = null;

								for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
									Perkuliahan perkuliahan = pertemuanPunyaUjian.getPertemuan().getPerkuliahan();
									if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum()
											.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
										JSONObject jsonObject = createJson(pertemuanPunyaUjian.getFormatNilais());
										if (!jsonObject.isNull(fnData.getId().toString())) {
											Double bobot = jsonObject.isNull(fnData.getId().toString() + "_bobot")
													? 100.0
													: jsonObject.getDouble(fnData.getId().toString() + "_bobot");
											totalPersen += bobot;
											XSSFCell cell;
											(cell = rowhead.createCell(index))
													.setCellValue(pertemuanPunyaUjian.getUjian().getNama() + "(\""
															+ fnData.getNama() + "\", bobot:"
															+ Common.numberFormat.get().format(bobot) + ")");
											cell.setCellStyle(notLocked3);
											index++;
											indexTotal++;
											jumlahPenmabahan++;
										}
									} else {
										totalPersen += pertemuanPunyaUjian.getProsentase();
										XSSFCell cell;
										(cell = rowhead.createCell(index))
												.setCellValue(pertemuanPunyaUjian.getUjian().getNama()
														+ "(\"" + fnData.getNama() + "\", bobot:" + Common.numberFormat
																.get().format(pertemuanPunyaUjian.getProsentase())
														+ ")");
										cell.setCellStyle(notLocked3);
										index++;
										indexTotal++;
										jumlahPenmabahan++;
									}
								}

								XSSFCell cell;
								for (Pertemuan pertemuan : pertemuanTugas1) {
									Perkuliahan perkuliahan = pertemuan.getPerkuliahan();

									if (hasText(pertemuan.getJudultugas())) {
										if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().apakahObe(
												perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
											JSONObject jsonObject = createJson(pertemuan.getFormatNilais());
											if (!jsonObject.isNull(fnData.getId().toString())) {
												Double bobot = jsonObject.getDouble(fnData.getId().toString());
												totalPersen += bobot;
												(cell = rowhead.createCell(index))
														.setCellValue(pertemuan.getJudultugas() + "(\""
																+ fnData.getNama() + "\", bobot:"
																+ Common.numberFormat.get().format(bobot) + ")");
												cell.setCellStyle(notLocked3);
												index++;
												indexTotal++;
												jumlahPenmabahan++;
											}
										} else if (fnData != null && pertemuan.getFormatNilai() != null
												&& fnData.getStatusPertemuan() != null
												&& pertemuan.getFormatNilai().getStatusPertemuan() != null
												&& pertemuan.getFormatNilai().getStatusPertemuan().getId()
														.equals(fnData.getStatusPertemuan().getId())) {
											totalPersen += pertemuan.getProsentase();
											(cell = rowhead.createCell(index)).setCellValue(pertemuan.getJudultugas()
													+ "(\"" + fnData.getNama() + "\", bobot:"
													+ Common.numberFormat.get().format(pertemuan.getProsentase())
													+ ")");
											cell.setCellStyle(notLocked3);
											index++;
											indexTotal++;
											jumlahPenmabahan++;
										}
									}

									for (TugasPertemuan tugasPertemuan : pertemuan.ambilTugasPertemuanTotal()
											.values()) {
										if (hasText(tugasPertemuan.getJudultugas())) {
											if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum()
													.apakahObe(perkuliahan.getTahunAjaran(),
															perkuliahan.getGanjilGenap())) {
												JSONObject jsonObject = createJson(tugasPertemuan.getFormatNilais());
												if (!jsonObject.isNull(fnData.getId().toString())) {
													Double bobot = jsonObject.getDouble(fnData.getId().toString());
													totalPersen += bobot;
													(cell = rowhead.createCell(index))
															.setCellValue(tugasPertemuan.getJudultugas() + "(\""
																	+ fnData.getNama() + "\", bobot:"
																	+ Common.numberFormat.get().format(bobot) + ")");
													cell.setCellStyle(notLocked3);
													index++;
													indexTotal++;
													jumlahPenmabahan++;
												}
											} else if (hasText(tugasPertemuan.getJudultugas())) {
												if (fnData != null && tugasPertemuan.getFormatNilai() != null
														&& fnData.getStatusPertemuan() != null
														&& tugasPertemuan.getFormatNilai().getStatusPertemuan() != null
														&& tugasPertemuan.getFormatNilai().getStatusPertemuan().getId()
																.equals(fnData.getStatusPertemuan().getId())) {
													totalPersen += tugasPertemuan.getProsentase();
													(cell = rowhead.createCell(index))
															.setCellValue(
																	tugasPertemuan.getJudultugas() + "(\""
																			+ fnData.getNama() + "\", bobot:"
																			+ Common.numberFormat.get().format(
																					tugasPertemuan.getProsentase())
																			+ ")");
													cell.setCellStyle(notLocked3);
													index++;
													indexTotal++;
													jumlahPenmabahan++;
												}
											}
										}
									}
								}

								for (TugasKelompok tugasKelompok : tugasKelompoks) {
									Perkuliahan perkuliahan = tugasKelompok.getPerkuliahan();
									if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum()
											.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
										JSONObject jsonObject = createJson(tugasKelompok.getFormatNilais());
										if (!jsonObject.isNull(fnData.getId().toString())) {
											Double bobot = jsonObject.getDouble(fnData.getId().toString());
											totalPersen += bobot;
											(cell = rowhead.createCell(index)).setCellValue(
													tugasKelompok.getJudul() + "(\"" + fnData.getNama() + "\", bobot:"
															+ Common.numberFormat.get().format(bobot) + ")");
											cell.setCellStyle(notLocked3);
											index++;
											indexTotal++;
											jumlahPenmabahan++;
										}
									} else {
										totalPersen += tugasKelompok.getProsentase();
										(cell = rowhead.createCell(index)).setCellValue(tugasKelompok.getJudul() + "(\""
												+ fnData.getNama() + "\", bobot:"
												+ Common.numberFormat.get().format(tugasKelompok.getProsentase())
												+ ")");
										cell.setCellStyle(notLocked3);
										index++;
										indexTotal++;
										jumlahPenmabahan++;
									}
								}

								if (jumlahPenmabahan > 0) {
									(cell = rowhead.createCell(index + 0)).setCellValue("Total \"" + fnData.getNama()
											+ "\", bobot:" + Common.numberFormat.get().format(totalPersen) + ")");
									cell.setCellStyle(notLocked);
									colSize.setValue(index + 1);
									indexTotal++;
								}

								int rowIndex = 0;
								Map<Long, XSSFRow> mapRow = new HashMap<Long, XSSFRow>();
								for (Long detailperkuliahanid : detailperkuliahans) {
									Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
											.get(Detailperkuliahan.class, detailperkuliahanid);
									if (detailperkuliahan != null) {
										if (mahasiswa == null || (mahasiswa != null && mahasiswa.getId()
												.equals(detailperkuliahan.getMahasiswa().getId()))) {
											rowIndex++;
											XSSFRow row = sheet.createRow(rowIndex);
											row.createCell(0).setCellValue(rowIndex);
											row.createCell(1).setCellValue(detailperkuliahan.getMahasiswa().getNim());
											row.createCell(2).setCellValue(detailperkuliahan.getMahasiswa().getNama());

											if (parent != null) {
												try {
													ArrayList<String> statusPertemuan = new ArrayList<String>();
													for (Pertemuan pertemuan : pertemuanTugas1) {
														if (hasText(pertemuan.getAbsensi())) {
															statusPertemuan.add(pertemuan.getAbsensi());
														}
													}

													Map<String, Integer> statuses = Perkuliahan.hitungStatus(
															statusPertemuan, detailperkuliahan.getMahasiswa().getId());
													int semua = statuses.get("T") == null ? 0 : statuses.get("T");
													int masuk = statuses.get("M") == null ? 0 : statuses.get("M");

													if (prosentaseMengikutiDefaultPertemuan) {
														semua = 16;
														try {
															semua = Integer.parseInt(Common.getKonfigurasi(
																	"jumlah_pertemuan_perkuliahan_default", "16")
																	.getNilai());
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/GradingHelper.java:1847");
														}
														semua = semua * perkuliahans.size();
													}

													// OPTIMASI: Memanfaatkan StringBuilder untuk String rincian absensi
													StringBuilder sbRinci = new StringBuilder();
													for (String key : statuses.keySet()) {
														if (!key.equals("T")) {
															int v = statuses.get(key);
															if (sbRinci.length() > 0)
																sbRinci.append(", ");
															sbRinci.append(key).append("=").append(v);
														}
													}

													cell = row.createCell(3);
													cell.setCellValue(sbRinci.toString());
													cell.setCellStyle(notLocked2);

													double persen = semua == 0 ? 0.0 : (masuk * 100.0) / semua;

													cell = row.createCell(4);
													cell.setCellValue(Common.numberFormat.get().format(persen) + "%");
													cell.setCellStyle(notLocked2);

													if (fnData != null && fnData.getNama() != null
															&& (fnData.getNama().toLowerCase().trim().contains("absen")
																	|| fnData.getNama().toLowerCase().trim()
																			.contains("presensi"))) {
														if (nilais.containsKey(fnData.getId())) {
															nilais.get(fnData.getId()).put(detailperkuliahan.getId(),
																	persen);
														} else {
															Map<Long, Double> map = new java.util.HashMap<Long, Double>();
															map.put(detailperkuliahan.getId(), persen);
															nilais.put(fnData.getId(), map);
														}
													}

												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/GradingHelper.java:1888");
												}
											}
											mapRow.put(detailperkuliahan.getId(), row);
										}
									}
								}

								dataNomorKolom.put(fnData.getId(), new Object[] { jumlahIndex, pertemuanPunyaUjians,
										tugasKelompoks, totalPersen, jumlahPenmabahan, mapRow });
							}

							for (FormatNilai fnData : fns) {
								Object[] o = dataNomorKolom.get(fnData.getId());
								Integer penambahan = (Integer) o[0];
								List<PertemuanPunyaUjian> pertemuanPunyaUjians = (List<PertemuanPunyaUjian>) o[1];
								List<TugasKelompok> tugasKelompoks = (List<TugasKelompok>) o[2];
								Double totalPersen = (Double) o[3];
								Integer jumlahPenmabahan = (Integer) o[4];
								Map<Long, XSSFRow> mapRow = (Map<Long, XSSFRow>) o[5];

								if (jumlahPenmabahan > 0) {
									int rowIndex = 0;
									for (Long detailperkuliahanid : detailperkuliahans) {
										Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
												.get(Detailperkuliahan.class, detailperkuliahanid);
										if (detailperkuliahan != null) {
											try {

												if (mahasiswa == null || (mahasiswa != null && mahasiswa.getId()
														.equals(detailperkuliahan.getMahasiswa().getId()))) {
													rowIndex++;
													label.setValue("Sedang memproses data "
															+ detailperkuliahan.toString() + " ("
															+ Common.numberFormat.get().format(
																	rowIndex * 100.0 / detailperkuliahans.size())
															+ " %)");

													Double totalPersenTidakIkutUjian = 0.0;
													for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
														Long id = detailperkuliahan.getMahasiswa().getId();
														if (containsId(pertemuanPunyaUjian.getMhsYgTidakIkut(), id)) {
															Perkuliahan pk = pertemuanPunyaUjian.getPertemuan().getPerkuliahan();
															if (pk.getKurikulum() != null && pk.getKurikulum().apakahObe(
																	pk.getTahunAjaran(), pk.getGanjilGenap())) {
																// OBE: subtract same _bobot used when building totalPersen
																JSONObject jObe = createJson(pertemuanPunyaUjian.getFormatNilais());
																if (!jObe.isNull(fnData.getId().toString())) {
																	Double bobot = jObe.isNull(fnData.getId().toString() + "_bobot")
																			? 100.0 : jObe.getDouble(fnData.getId().toString() + "_bobot");
																	totalPersenTidakIkutUjian += bobot;
																}
															} else {
																totalPersenTidakIkutUjian += pertemuanPunyaUjian.getProsentase();
															}
														}
													}

													for (Pertemuan pertemuan : pertemuanTugas1) {
														if (hasText(pertemuan.getJudultugas())) {
															Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
															if (perkuliahan.getKurikulum() != null
																	&& perkuliahan.getKurikulum().apakahObe(
																			perkuliahan.getTahunAjaran(),
																			perkuliahan.getGanjilGenap())) {
																JSONObject jsonObject = createJson(
																		pertemuan.getFormatNilais());
																if (!jsonObject.isNull(fnData.getId().toString())) {
																	Double bobot = jsonObject
																			.getDouble(fnData.getId().toString());
																	Long id = detailperkuliahan.getMahasiswa().getId();
																	if (containsId(pertemuan.getMhsYgTidakIkut(), id)) {
																		totalPersenTidakIkutUjian += bobot;
																	}
																}
															} else if (fnData != null
																	&& pertemuan.getFormatNilai() != null
																	&& pertemuan.getFormatNilai().getId()
																			.equals(fnData.getId())) {
																Long id = detailperkuliahan.getMahasiswa().getId();
																if (containsId(pertemuan.getMhsYgTidakIkut(), id)) {
																	totalPersenTidakIkutUjian += pertemuan
																			.getProsentase();
																}
															}
														}

														for (TugasPertemuan tugasPertemuan : pertemuan
																.ambilTugasPertemuanTotal().values()) {
															if (hasText(tugasPertemuan.getJudultugas())) {
																Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
																if (perkuliahan.getKurikulum() != null
																		&& perkuliahan.getKurikulum().apakahObe(
																				perkuliahan.getTahunAjaran(),
																				perkuliahan.getGanjilGenap())) {
																	JSONObject jsonObject = createJson(
																			tugasPertemuan.getFormatNilais());
																	if (!jsonObject.isNull(fnData.getId().toString())) {
																		Long id = detailperkuliahan.getMahasiswa()
																				.getId();
																		if (containsId(
																				tugasPertemuan.getMhsYgTidakIkut(),
																				id)) {
																			Double bobot = jsonObject.getDouble(
																					fnData.getId().toString());
																			totalPersenTidakIkutUjian += bobot;
																		}
																	}
																} else if (hasText(tugasPertemuan.getJudultugas())) {
																	if (fnData != null
																			&& tugasPertemuan.getFormatNilai() != null
																			&& tugasPertemuan.getFormatNilai().getId()
																					.equals(fnData.getId())) {
																		Long id = detailperkuliahan.getMahasiswa()
																				.getId();
																		if (containsId(
																				tugasPertemuan.getMhsYgTidakIkut(),
																				id)) {
																			totalPersenTidakIkutUjian += tugasPertemuan
																					.getProsentase();
																		}
																	}
																}
															}
														}
													}

													for (TugasKelompok tugasKelompok : tugasKelompoks) {
														Perkuliahan perkuliahan = tugasKelompok.getPerkuliahan();
														if (perkuliahan.getKurikulum() != null && perkuliahan
																.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(),
																		perkuliahan.getGanjilGenap())) {
															JSONObject jsonObject = createJson(
																	tugasKelompok.getFormatNilais());
															if (!jsonObject.isNull(fnData.getId().toString())) {
																Long id = detailperkuliahan.getMahasiswa().getId();
																if (containsId(tugasKelompok.getMhsYgTidakIkut(), id)) {
																	Double bobot = jsonObject
																			.getDouble(fnData.getId().toString());
																	totalPersenTidakIkutUjian += bobot;
																}
															}
														} else {
															Long id = detailperkuliahan.getMahasiswa().getId();
															if (containsId(tugasKelompok.getMhsYgTidakIkut(), id)) {
																totalPersenTidakIkutUjian += tugasKelompok
																		.getProsentase();
															}
														}
													}

													Double nilaiTotal = 0.0;
													Double nilaiSemua = 0.0;

													XSSFRow row = mapRow.get(detailperkuliahan.getId());
													int index = (parent != null ? 5 : 3) + penambahan;

													for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
														Long id = detailperkuliahan.getMahasiswa().getId();
														if (!containsId(pertemuanPunyaUjian.getMhsYgTidakIkut(), id)) {
															Perkuliahan perkuliahan = pertemuanPunyaUjian.getPertemuan()
																	.getPerkuliahan();
															if (perkuliahan.getKurikulum() != null
																	&& perkuliahan.getKurikulum().apakahObe(
																			perkuliahan.getTahunAjaran(),
																			perkuliahan.getGanjilGenap())) {
																JSONObject jsonObject = createJson(
																		pertemuanPunyaUjian.getFormatNilais());
																if (!jsonObject.isNull(fnData.getId().toString())) {
																	Double bobot = jsonObject.isNull(
																			fnData.getId().toString() + "_bobot")
																					? 100.0
																					: jsonObject.getDouble(
																							fnData.getId().toString()
																									+ "_bobot");

																	String nilaiObe = (String) session
																			.createCriteria(HasilUjianMahasiswa.class)
																			.add(Restrictions.isNotNull("keyhasil"))
																			.setMaxResults(1)
																			.setProjection(
																					Projections.property("nilaiObe"))
																			.add(Restrictions.eq("pertemuanPunyaUjian",
																					pertemuanPunyaUjian))
																			.add(Restrictions.eq("mahasiswa",
																					detailperkuliahan.getMahasiswa()))
																			.uniqueResult();
																	try {
																		if (nilaiObe != null
																				&& !nilaiObe.trim().isEmpty()) {
																			JSONObject jsonObjectHasil = createJson(
																					nilaiObe);
																			Double nilaiSkor = jsonObjectHasil
																					.isNull(fnData.getId().toString())
																							? 0.0
																							: jsonObjectHasil.getDouble(
																									fnData.getId()
																											.toString());
																			Double nilaiMax = jsonObjectHasil.isNull(
																					fnData.getId().toString() + "_max")
																							? 0.0
																							: jsonObjectHasil.getDouble(
																									fnData.getId()
																											.toString()
																											+ "_max");

																			Double nilaiDidapat = nilaiMax.equals(0.0)
																					? 0.0
																					: (nilaiSkor * 100.0) / nilaiMax;
																			Double nilai = safeDivide(
																					(nilaiDidapat * bobot), totalPersen
																							- totalPersenTidakIkutUjian);
																			nilaiSemua += nilai;
																			nilaiTotal += nilaiDidapat;
																			XSSFCell cell;
																			(cell = row.createCell(index))
																					.setCellValue(nilaiDidapat);
																			cell.setCellStyle(notLocked3);
																		} else {
																			XSSFCell cell;
																			(cell = row.createCell(index))
																					.setCellValue(0.0);
																			cell.setCellStyle(notLocked3);
																		}
																		index++;
																	} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/GradingHelper.java:2102");
																	}
																}
															} else {
																Number nilaiUjian = (Number) session
																		.createCriteria(HasilUjianMahasiswa.class)
																		.add(Restrictions.isNotNull("keyhasil"))
																		.setMaxResults(1)
																		.setProjection(Projections.property("nilai"))
																		.add(Restrictions.eq("pertemuanPunyaUjian",
																				pertemuanPunyaUjian))
																		.add(Restrictions.eq("mahasiswa",
																				detailperkuliahan.getMahasiswa()))
																		.uniqueResult();
																if (nilaiUjian != null) {
																	Double nilai = safeDivide((nilaiUjian.doubleValue()
																			* pertemuanPunyaUjian.getProsentase()),
																			totalPersen - totalPersenTidakIkutUjian);
																	nilaiSemua += nilai;
																	nilaiTotal += nilaiUjian.doubleValue();
																	XSSFCell cell;
																	(cell = row.createCell(index))
																			.setCellValue(nilaiUjian.doubleValue());
																	cell.setCellStyle(notLocked3);
																} else {
																	XSSFCell cell;
																	(cell = row.createCell(index)).setCellValue(0.0);
																	cell.setCellStyle(notLocked3);
																}
																index++;
															}
														} else {
															XSSFCell cell;
															(cell = row.createCell(index))
																	.setCellValue("Tidak perlu ikut");
															cell.setCellStyle(notLocked4);
															index++;
														}
													}

													for (Pertemuan pertemuan : pertemuanTugas1) {
														Long id = detailperkuliahan.getMahasiswa().getId();
														if (hasText(pertemuan.getJudultugas())) {
															Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
															JSONObject jsonObjectTugas = createJson(
																	pertemuan.getKeteranganNilai());

															if (perkuliahan.getKurikulum() != null
																	&& perkuliahan.getKurikulum().apakahObe(
																			perkuliahan.getTahunAjaran(),
																			perkuliahan.getGanjilGenap())) {
																JSONObject jsonObject = createJson(
																		pertemuan.getFormatNilais());
																if (!jsonObject.isNull(fnData.getId().toString())) {
																	if (!containsId(pertemuan.getMhsYgTidakIkut(),
																			id)) {
																		TugasFileContent tugasFileContent = pertemuan
																				.ambilTugasFileContent(detailperkuliahan
																						.getMahasiswa());
																		Double nilaiTugas = 0.0;
																		if (tugasFileContent != null) {
																			String keyKet = "";
																			if (tugasFileContent.getMahasiswa() != null)
																				keyKet = tugasFileContent.getMahasiswa() + "_mhs";
																			else if (tugasFileContent.getSiswa() != null)
																				keyKet = tugasFileContent.getSiswa() + "_siswa";
																			else if (tugasFileContent.getBiodataCalonMahasiswa() != null)
																				keyKet = tugasFileContent.getBiodataCalonMahasiswa() + "_cal_mhs";
																			else if (tugasFileContent.getCalonSiswa() != null)
																				keyKet = tugasFileContent.getCalonSiswa() + "_cal_siswa";
																			nilaiTugas = (!keyKet.isEmpty() && !jsonObjectTugas.isNull(keyKet + "_nilai"))
																					? jsonObjectTugas.getDouble(keyKet + "_nilai")
																					: tugasFileContent.getNilai();
																		}
																		try {
																			if (tugasFileContent != null) {
																				String key = "";
																				if (tugasFileContent
																						.getMahasiswa() != null)
																					key = tugasFileContent
																							.getMahasiswa() + "_mhs";
																				else if (tugasFileContent
																						.getSiswa() != null)
																					key = tugasFileContent.getSiswa()
																							+ "_siswa";
																				else if (tugasFileContent
																						.getBiodataCalonMahasiswa() != null)
																					key = tugasFileContent
																							.getBiodataCalonMahasiswa()
																							+ "_cal_mhs";
																				else if (tugasFileContent
																						.getCalonSiswa() != null)
																					key = tugasFileContent
																							.getCalonSiswa()
																							+ "_cal_siswa";

																				nilaiTugas = jsonObjectTugas.isNull(key
																						+ "_nilai_" + fnData.getId())
																								? 0.0
																								: jsonObjectTugas
																										.getDouble(key
																												+ "_nilai_"
																												+ fnData.getId());
																			}
																		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/GradingHelper.java:2194");
																		}

																		Double bobot = jsonObject
																				.getDouble(fnData.getId().toString());
																		Double nilai = safeDivide(
																				(nilaiTugas.doubleValue() * bobot),
																				totalPersen
																						- totalPersenTidakIkutUjian);
																		nilaiSemua += nilai;
																		nilaiTotal += nilaiTugas.doubleValue();
																		XSSFCell cell;
																		(cell = row.createCell(index))
																				.setCellValue(nilaiTugas.doubleValue());
																		cell.setCellStyle(notLocked3);
																	} else {
																		XSSFCell cell;
																		(cell = row.createCell(index))
																				.setCellValue("Tidak perlu ikut");
																		cell.setCellStyle(notLocked4);
																	}
																	index++;
																}
															} else if (fnData != null
																	&& pertemuan.getFormatNilai() != null
																	&& pertemuan.getFormatNilai().getId()
																			.equals(fnData.getId())) {
																if (!containsId(pertemuan.getMhsYgTidakIkut(), id)) {
																	TugasFileContent tugasFileContent = pertemuan
																			.ambilTugasFileContent(
																					detailperkuliahan.getMahasiswa());
																	Double nilaiTugas = 0.0;
																	if (tugasFileContent != null) {
																		String keyKet = "";
																		if (tugasFileContent.getMahasiswa() != null)
																			keyKet = tugasFileContent.getMahasiswa() + "_mhs";
																		else if (tugasFileContent.getSiswa() != null)
																			keyKet = tugasFileContent.getSiswa() + "_siswa";
																		else if (tugasFileContent.getBiodataCalonMahasiswa() != null)
																			keyKet = tugasFileContent.getBiodataCalonMahasiswa() + "_cal_mhs";
																		else if (tugasFileContent.getCalonSiswa() != null)
																			keyKet = tugasFileContent.getCalonSiswa() + "_cal_siswa";
																		nilaiTugas = (!keyKet.isEmpty() && !jsonObjectTugas.isNull(keyKet + "_nilai"))
																				? jsonObjectTugas.getDouble(keyKet + "_nilai")
																				: tugasFileContent.getNilai();
																	}

																	Double nilai = safeDivide(
																			(nilaiTugas.doubleValue()
																					* pertemuan.getProsentase()),
																			totalPersen - totalPersenTidakIkutUjian);
																	nilaiSemua += nilai;
																	nilaiTotal += nilaiTugas.doubleValue();
																	XSSFCell cell;
																	(cell = row.createCell(index))
																			.setCellValue(nilaiTugas.doubleValue());
																	cell.setCellStyle(notLocked3);
																} else {
																	XSSFCell cell;
																	(cell = row.createCell(index))
																			.setCellValue("Tidak perlu ikut");
																	cell.setCellStyle(notLocked4);
																}
																index++;
															}
														}

														for (TugasPertemuan tugasPertemuan : pertemuan
																.ambilTugasPertemuanTotal().values()) {
															if (hasText(tugasPertemuan.getJudultugas())) {
																Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
																JSONObject jsonObjectTugas = createJson(
																		tugasPertemuan.getKeteranganNilai());

																if (perkuliahan.getKurikulum() != null
																		&& perkuliahan.getKurikulum().apakahObe(
																				perkuliahan.getTahunAjaran(),
																				perkuliahan.getGanjilGenap())) {
																	JSONObject jsonObject = createJson(
																			tugasPertemuan.getFormatNilais());
																	if (!jsonObject.isNull(fnData.getId().toString())) {
																		if (!containsId(
																				tugasPertemuan.getMhsYgTidakIkut(),
																				id)) {
																			TugasFileContent tugasFileContent = tugasPertemuan
																					.ambilTugasFileContent(
																							detailperkuliahan
																									.getMahasiswa());
																			Double nilaiTugas = tugasFileContent == null
																					? 0.0
																					: tugasFileContent.getNilai();
																			try {
																				if (tugasFileContent != null) {
																					String key = "";
																					if (tugasFileContent
																							.getMahasiswa() != null)
																						key = tugasFileContent
																								.getMahasiswa()
																								+ "_mhs";
																					else if (tugasFileContent
																							.getSiswa() != null)
																						key = tugasFileContent
																								.getSiswa() + "_siswa";
																					else if (tugasFileContent
																							.getBiodataCalonMahasiswa() != null)
																						key = tugasFileContent
																								.getBiodataCalonMahasiswa()
																								+ "_cal_mhs";
																					else if (tugasFileContent
																							.getCalonSiswa() != null)
																						key = tugasFileContent
																								.getCalonSiswa()
																								+ "_cal_siswa";

																					nilaiTugas = jsonObjectTugas
																							.isNull(key + "_nilai_"
																									+ fnData.getId())
																											? 0.0
																											: jsonObjectTugas
																													.getDouble(
																															key + "_nilai_"
																																	+ fnData.getId());
																				}
																			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/GradingHelper.java:2304");
																			}

																			Double bobot = jsonObject.getDouble(
																					fnData.getId().toString());
																			Double nilai = safeDivide(
																					(nilaiTugas.doubleValue() * bobot),
																					totalPersen
																							- totalPersenTidakIkutUjian);
																			nilaiSemua += nilai;
																			nilaiTotal += nilaiTugas.doubleValue();
																			XSSFCell cell;
																			(cell = row.createCell(index)).setCellValue(
																					nilaiTugas.doubleValue());
																			cell.setCellStyle(notLocked3);
																		} else {
																			XSSFCell cell;
																			(cell = row.createCell(index))
																					.setCellValue("Tidak perlu ikut");
																			cell.setCellStyle(notLocked4);
																		}
																		index++;
																	}
																} else if (fnData != null
																		&& tugasPertemuan.getFormatNilai() != null
																		&& tugasPertemuan.getFormatNilai().getId()
																				.equals(fnData.getId())) {
																	if (!containsId(tugasPertemuan.getMhsYgTidakIkut(),
																			id)) {
																		TugasFileContent tugasFileContent = tugasPertemuan
																				.ambilTugasFileContent(detailperkuliahan
																						.getMahasiswa());
																		Double nilaiTugas = 0.0;
																		if (tugasFileContent != null) {
																			String keyKet = "";
																			if (tugasFileContent.getMahasiswa() != null)
																				keyKet = tugasFileContent.getMahasiswa() + "_mhs";
																			else if (tugasFileContent.getSiswa() != null)
																				keyKet = tugasFileContent.getSiswa() + "_siswa";
																			else if (tugasFileContent.getBiodataCalonMahasiswa() != null)
																				keyKet = tugasFileContent.getBiodataCalonMahasiswa() + "_cal_mhs";
																			else if (tugasFileContent.getCalonSiswa() != null)
																				keyKet = tugasFileContent.getCalonSiswa() + "_cal_siswa";
																			nilaiTugas = (!keyKet.isEmpty() && !jsonObjectTugas.isNull(keyKet + "_nilai"))
																					? jsonObjectTugas.getDouble(keyKet + "_nilai")
																					: tugasFileContent.getNilai();
																		}

																		Double nilai = safeDivide(
																				(nilaiTugas.doubleValue()
																						* tugasPertemuan
																								.getProsentase()),
																				totalPersen
																						- totalPersenTidakIkutUjian);
																		nilaiSemua += nilai;
																		nilaiTotal += nilaiTugas.doubleValue();
																		XSSFCell cell;
																		(cell = row.createCell(index))
																				.setCellValue(nilaiTugas.doubleValue());
																		cell.setCellStyle(notLocked3);
																	} else {
																		XSSFCell cell;
																		(cell = row.createCell(index))
																				.setCellValue("Tidak perlu ikut");
																		cell.setCellStyle(notLocked4);
																	}
																	index++;
																}
															}
														}
													}

													for (TugasKelompok tugasKelompok : tugasKelompoks) {
														Perkuliahan perkuliahan = tugasKelompok.getPerkuliahan();
														Long id = detailperkuliahan.getMahasiswa().getId();
														if (!containsId(tugasKelompok.getMhsYgTidakIkut(), id)) {
															if (perkuliahan.getKurikulum() != null
																	&& perkuliahan.getKurikulum().apakahObe(
																			perkuliahan.getTahunAjaran(),
																			perkuliahan.getGanjilGenap())) {
																JSONObject jsonObjectTugas = createJson(
																		tugasKelompok.getKeteranganNilai());
																JSONObject jsonObject = createJson(
																		tugasKelompok.getFormatNilais());
																if (!jsonObject.isNull(fnData.getId().toString())) {
																	String key = "";
																	if (detailperkuliahan.getMahasiswa() != null) {
																		key = detailperkuliahan.getMahasiswa().getId()
																				+ "_mhs";
																	}
																	Double nilaiTugas = jsonObjectTugas
																			.isNull(key + "_nilai_" + fnData.getId())
																					? 0.0
																					: jsonObjectTugas
																							.getDouble(key + "_nilai_"
																									+ fnData.getId());
																	Double bobot = jsonObject
																			.getDouble(fnData.getId().toString());

																	Double nilai = safeDivide(
																			(nilaiTugas.doubleValue() * bobot),
																			totalPersen - totalPersenTidakIkutUjian);
																	nilaiSemua += nilai;
																	nilaiTotal += nilaiTugas.doubleValue();
																	XSSFCell cell;
																	(cell = row.createCell(index))
																			.setCellValue(nilaiTugas.doubleValue());
																	cell.setCellStyle(notLocked3);
																	index++;
																}
															} else {
																Number nilaiTugas = (Number) session
																		.createCriteria(
																				NamaTugasKelompokPunyaMahasiswa.class)
																		.setMaxResults(1)
																		.setProjection(Projections.property("nilai"))
																		.createAlias("namaTugasKelompok",
																				"namaTugasKelompok")
																		.add(Restrictions.eq(
																				"namaTugasKelompok.tugasKelompok",
																				tugasKelompok))
																		.add(Restrictions.eq("mahasiswa",
																				detailperkuliahan.getMahasiswa()))
																		.uniqueResult();
																if (nilaiTugas != null) {
																	Double nilai = safeDivide(
																			(nilaiTugas.doubleValue()
																					* tugasKelompok.getProsentase()),
																			totalPersen - totalPersenTidakIkutUjian);
																	nilaiSemua += nilai;
																	nilaiTotal += nilaiTugas.doubleValue();
																	XSSFCell cell;
																	(cell = row.createCell(index))
																			.setCellValue(nilaiTugas.doubleValue());
																	cell.setCellStyle(notLocked3);
																} else {
																	XSSFCell cell;
																	(cell = row.createCell(index)).setCellValue(0.0);
																	cell.setCellStyle(notLocked3);
																}
																index++;
															}
														} else {
															XSSFCell cell;
															(cell = row.createCell(index))
																	.setCellValue("Tidak perlu ikut");
															cell.setCellStyle(notLocked4);
															index++;
														}
													}

													double nilaiSemuaCapped = Math.min(100.0, Math.max(0.0, nilaiSemua));
													XSSFCell cell;
													(cell = row.createCell(index + 0)).setCellValue(nilaiSemuaCapped);
													cell.setCellStyle(notLocked);

													if (nilais.containsKey(fnData.getId())) {
														nilais.get(fnData.getId()).put(detailperkuliahan.getId(),
																nilaiSemuaCapped);
													} else {
														Map<Long, Double> map = new HashMap<Long, Double>();
														map.put(detailperkuliahan.getId(), nilaiSemuaCapped);
														nilais.put(fnData.getId(), map);
													}
													report.sukses(rowIndex, "NIM: " + detailperkuliahan.getMahasiswa().getNim() + " | " + fnData.getNama(), "nilai=" + nilaiSemuaCapped);
												}
												// OPTIMASI MEMORI
												if (rowIndex % BATCH_SIZE == 0) {
													session.flush();
													session.clear();
												}
											} catch (Exception e) {
												report.gagal(rowIndex, detailperkuliahan != null ? "NIM: " + detailperkuliahan.getMahasiswa().getNim() : "id=" + detailperkuliahanid, e, "Periksa data perkuliahan mahasiswa.");
												Common.tampilErrorJikaAdmin(e);
											}
										}
									}
								}
							}

							dataNomorKolom = null;

							try {
								fileOut = new FileOutputStream(filename);
								workbook.write(fileOut);
							} catch (IOException e) {
								Common.tampilErrorJikaAdmin(e);
							}

							detailperkuliahans.clear();
							detailperkuliahans = null;
							try {
								java.io.File rptFile = report.simpanLaporan();
								downloadPath.setValue(rptFile.getAbsolutePath());
							} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) GradingHelper laporan"); }
							label.setValue("");
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							try {
								java.io.File rptFile = report.simpanLaporan();
								downloadPath.setValue(rptFile.getAbsolutePath());
							} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) GradingHelper laporan"); }
							label.setValue("");
						} finally {
							if (fileOut != null) {
								try {
									fileOut.close();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/GradingHelper.java:2488");
								}
							}
							closeSession(session);
						}

					}
				}).start();

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		}
	}

	@SuppressWarnings("unchecked")
	public static void hitungNilaiBerdasarkanFormatNilaiSkripsi(final Perkuliahan perkuliahan, final FormatNilai fn)
			throws Exception {
		if (fn != null && perkuliahan != null) {

			if (perkuliahan.getDikunci() != null) {
				MyMessageboxConfig.show("Penilaian untuk perkuliahan ini telah terkunci", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}

			final Map<Long, Double> nilais = new HashMap<Long, Double>();

			final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
			final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Hitung Ulang Nilai Skripsi");
			final Label downloadPath = new Label("");
			final Intbox intbox = new Intbox(10);
			final Intbox colSize = new Intbox(10);
			Clients.showBusy(label.getValue());
			final Tbmuser tbmuser = Common.getCurrentUser();
			final String filename = Sessions.getCurrent().getWebApp().getRealPath(
					"/tmp/cetak_data_" + java.util.UUID.randomUUID().toString() + ".xlsx");
			final File file = new File(filename);
			file.createNewFile();

			final Timer timer = new Timer(200);
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			timer.addEventListener("onTimer", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						Clients.showBusy(label.getValue());

						if (trim(label.getValue()).equalsIgnoreCase("-")) {
							Clients.clearBusy();
							timer.detach();
						} else if (label.getValue().isEmpty()) {
							if (!downloadPath.getValue().isEmpty()) {
								try { Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); }
								catch (Exception eDl) { ais.common.ErrorAuditUtil.record(eDl, "auto-audit(empty-catch) download laporan"); }
							}

							final MyWindow window = new MyWindow("Rekap Nilai", "none", true);
							siapkanWindowScrollable(window, "97%", "90%");

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							siapkanBorderlayout(borderlayout);
							borderlayout.setParent(window);

							Toolbar toolbar = buatToolbarAtas(borderlayout);

							Center center = new Center();
							siapkanCenterScrollable(center);
							center.setParent(borderlayout);

							tampilkanGridDariExcel(center, file, intbox, colSize);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							styleToolbarButton(cancel, "#a94442");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									window.detach();
								}
							});
							cancel.setParent(toolbar);

							MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Preview / Download Excel",
									"/img/excel.png");
							print.setTooltiptext("Buka preview Excel dan unduh file .xlsx");
							styleToolbarButton(print, "#1f5d7a");
							print.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									tampilkanPopupExcel(file, intbox, colSize);
								}
							});
							print.setParent(toolbar);

							MyToolbarbuttonConfig proses = new MyToolbarbuttonConfig(
									"Masukkan nilai ke \"" + fn.getNama() + "\"", "/img/svg/check2.svg");
							styleToolbarButton(proses, "#2f6b2f");
							proses.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {

									Session session = null;
									Transaction tx = null;
									try {
										session = HibernateUtil.getSessionFactory().openSession();
										tx = session.beginTransaction();

										List<FormatNilai> formatNilais = perkuliahan.ambilFormatNilai(session);
										if (!Detailperkuliahan.formatNilaiSiapDihitung(formatNilais)) {
											throw new IllegalStateException(
													"Impor nilai dibatalkan: format nilai kosong atau total bobot bukan 100%. Perkuliahan="
															+ perkuliahan.getId());
										}

										int count = 0;
										for (Long detailperkuliahanId : nilais.keySet()) {
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
													.get(Detailperkuliahan.class, detailperkuliahanId);
											if (detailperkuliahan == null) {
												continue;
											}

											Double nilaiSemua = nilais.get(detailperkuliahanId);

											detailperkuliahan.populateDetailNilai(fn, null,
													nilaiSemua == null ? 0.0 : nilaiSemua.doubleValue(), true, tbmuser);

											Matakuliah matakuliah = detailperkuliahan == null ? null
													: detailperkuliahan.getPerkuliahan() != null
															? detailperkuliahan.getPerkuliahan().getMatakuliah()
															: detailperkuliahan.getMatakuliahKonversi();

											Double total = detailperkuliahan.hitungTotalNilai(true, formatNilais);
											NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
													detailperkuliahan.getMahasiswa().getTahunangkatan(),
													detailperkuliahan.getMahasiswa().getJurusan(),
													detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
													detailperkuliahan.getTahunAkademik(),
													detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
															: Perkuliahan.GANJIL,
													matakuliah == null ? "" : matakuliah.getKode(),
													matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

											detailperkuliahan
													.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
											detailperkuliahan.setTotalNilai(total);
											detailperkuliahan.setNilaiHuruf(
													nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
											detailperkuliahan
													.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

											Double totalSementara = detailperkuliahan.hitungTotalNilaiSementara(true,
													formatNilais);
											nilaiHuruf = Common.getNilaiHuruf(totalSementara,
													detailperkuliahan.getMahasiswa().getTahunangkatan(),
													detailperkuliahan.getMahasiswa().getJurusan(),
													detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
													detailperkuliahan.getTahunAkademik(),
													detailperkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP
															: Perkuliahan.GANJIL,
													matakuliah == null ? "" : matakuliah.getKode(),
													matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

											detailperkuliahan.setTotalNilaiSementara(totalSementara);
											detailperkuliahan.setNilaiHurufSementara(
													nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
											detailperkuliahan.setTotalIPSementara(
													nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

											session.update(detailperkuliahan);

											// OPTIMASI MEMORI: Membersihkan cache L1 Hibernate agar tidak Out Of Memory
											count++;
											if (count % BATCH_SIZE == 0) {
												session.flush();
												session.clear();
											}
										}
										tx.commit();
									} catch (Exception e) {
										if (tx != null)
											tx.rollback();
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/GradingHelper.java:2663");
									} finally {
										closeSession(session);
									}

									Common.createDefaultTimer(new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											DetailperkuliahanForPenilaianHelper.onLaporan(perkuliahan);
										}
									});

								}
							});
							proses.setParent(toolbar);

							window.setVisible(true);
							window.onModal();

							Clients.clearBusy();
							timer.detach();
						}

					} catch (Exception e) {
						Clients.clearBusy();
					}

				}
			});
			timer.start();

			try {
				Clients.showBusy(label.getValue());

				new Thread(new Runnable() {
					@Override
					public void run() {

						Session session = null;
						FileOutputStream fileOut = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();

							Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan();

							intbox.setValue(detailperkuliahans.size());

							XSSFWorkbook workbook = new XSSFWorkbook();
							XSSFSheet sheet = workbook.createSheet(Common.getBahasaConfig("Nilai"));

							sheet.setDefaultColumnWidth(20);

							XSSFRow rowhead = sheet.createRow((short) 0);
							rowhead.createCell(0).setCellValue(Common.getBahasaConfig("No."));
							rowhead.createCell(1).setCellValue(Common.getBahasaConfig("NIM"));
							rowhead.createCell(2).setCellValue(Common.getBahasaConfig("Nama Mahasiswa"));

							int index = 3;

							List<FormatNilaiProposalSkripsi> formatNilaiProposalSkripsis = session
									.createCriteria(MahasiswaRequestTugasAkhir.class)
									.setProjection(Projections.groupProperty("formatNilaiProposalSkripsi"))
									.createAlias("detailperkuliahan", "detailperkuliahan")
									.add(Restrictions.eq("detailperkuliahan.perkuliahan", perkuliahan))
									.add(Restrictions.eq("formatNilai", fn)).list();

							for (FormatNilaiProposalSkripsi formatNilaiProposalSkripsi : formatNilaiProposalSkripsis) {
								rowhead.createCell(index).setCellValue(formatNilaiProposalSkripsi.getNama() + "(bobot:"
										+ Common.numberFormat.get().format(formatNilaiProposalSkripsi.getBobot())
										+ ")");
								index++;
							}

							List<FormatNilaiSkripsi> formatNilaiSkripsis = session.createCriteria(Skripsi.class)
									.setProjection(Projections.groupProperty("formatNilaiSkripsi"))
									.createAlias("detailperkuliahan", "detailperkuliahan")
									.add(Restrictions.eq("detailperkuliahan.perkuliahan", perkuliahan))
									.add(Restrictions.eq("formatNilai", fn)).list();

							for (FormatNilaiSkripsi formatNilaiSkripsi : formatNilaiSkripsis) {
								rowhead.createCell(index).setCellValue(formatNilaiSkripsi.getNama() + "(bobot:"
										+ Common.numberFormat.get().format(formatNilaiSkripsi.getBobot()) + ")");
								index++;
							}

							rowhead.createCell(index).setCellValue("Total");
							rowhead.createCell(index + 1).setCellValue("Nilai Akhir");

							int rowIndex = 0;
							for (Long detailperkuliahanid : detailperkuliahans) {
								Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
										.get(Detailperkuliahan.class, detailperkuliahanid);

								if (detailperkuliahan != null) {
									try {
										rowIndex++;

										label.setValue("Sedang memproses data " + detailperkuliahan.toString() + " ("
												+ Common.numberFormat.get()
														.format(rowIndex * 100.0 / detailperkuliahans.size())
												+ " %)");

										Double totalPersen = 0.0;
										Map<String, Number> totals = new HashMap<String, Number>();

										for (FormatNilaiProposalSkripsi formatNilaiProposalSkripsi : formatNilaiProposalSkripsis) {
											Number totalNilai = (Number) session
													.createCriteria(MahasiswaRequestTugasAkhir.class).setMaxResults(1)
													.setProjection(Projections.property("totalNilai"))
													.add(Restrictions.eq("formatNilaiProposalSkripsi",
															formatNilaiProposalSkripsi))
													.add(Restrictions.eq("mahasiswa", detailperkuliahan.getMahasiswa()))
													.uniqueResult();
											if (totalNilai != null) {
												totals.put(FormatNilaiProposalSkripsi.class.getName() + "-"
														+ formatNilaiProposalSkripsi.getId(), totalNilai);
												totalPersen += formatNilaiProposalSkripsi.getBobot();
											}
										}

										for (FormatNilaiSkripsi formatNilaiSkripsi : formatNilaiSkripsis) {
											Number totalNilai = (Number) session.createCriteria(Skripsi.class)
													.setMaxResults(1).setProjection(Projections.property("totalNilai"))
													.add(Restrictions.eq("formatNilaiSkripsi", formatNilaiSkripsi))
													.add(Restrictions.eq("mahasiswa", detailperkuliahan.getMahasiswa()))
													.uniqueResult();
											if (totalNilai != null) {
												totals.put(FormatNilaiSkripsi.class.getName() + "-"
														+ formatNilaiSkripsi.getId(), totalNilai);
												totalPersen += formatNilaiSkripsi.getBobot();
											}
										}

										XSSFRow row = sheet.createRow(rowIndex);
										row.createCell(0).setCellValue(rowIndex);
										row.createCell(1).setCellValue(detailperkuliahan.getMahasiswa().getNim());
										row.createCell(2).setCellValue(detailperkuliahan.getMahasiswa().getNama());

										Double nilaiTotal = 0.0;
										Double nilaiSemua = 0.0;
										index = 3;

										for (FormatNilaiProposalSkripsi formatNilaiProposalSkripsi : formatNilaiProposalSkripsis) {
											Number totalNilai = totals.get(FormatNilaiProposalSkripsi.class.getName()
													+ "-" + formatNilaiProposalSkripsi.getId());
											if (totalNilai != null) {
												Double nilai = safeDivide(totalNilai.doubleValue()
														* formatNilaiProposalSkripsi.getBobot(), totalPersen);
												nilaiSemua += nilai;
												nilaiTotal += totalNilai.doubleValue();
												row.createCell(index).setCellValue(totalNilai.doubleValue());
											} else {
												row.createCell(index).setCellValue("x");
											}
											index++;
										}

										for (FormatNilaiSkripsi formatNilaiSkripsi : formatNilaiSkripsis) {
											Number totalNilai = totals.get(FormatNilaiSkripsi.class.getName() + "-"
													+ formatNilaiSkripsi.getId());
											if (totalNilai != null) {
												Double nilai = safeDivide(
														totalNilai.doubleValue() * formatNilaiSkripsi.getBobot(),
														totalPersen);
												nilaiSemua += nilai;
												nilaiTotal += totalNilai.doubleValue();
												row.createCell(index).setCellValue(totalNilai.doubleValue());
											} else {
												row.createCell(index).setCellValue("x");
											}
											index++;
										}

										row.createCell(index).setCellValue(nilaiTotal);
										row.createCell(index + 1).setCellValue(nilaiSemua);

										nilais.put(detailperkuliahan.getId(), nilaiSemua);
										report.sukses(rowIndex, "NIM: " + detailperkuliahan.getMahasiswa().getNim() + " | " + fn.getNama(), "nilai=" + nilaiSemua);

										// OPTIMASI MEMORI: Membersihkan memori saat proses looping Export Excel yang
										// besar
										if (rowIndex % BATCH_SIZE == 0) {
											session.clear();
										}

									} catch (Exception e) {
										report.gagal(rowIndex, detailperkuliahan != null ? "NIM: " + detailperkuliahan.getMahasiswa().getNim() : "id=" + detailperkuliahanid, e, "Periksa data perkuliahan mahasiswa.");
										Common.tampilErrorJikaAdmin(e);
									}
								}
							}

							try {
								fileOut = new FileOutputStream(filename);
								workbook.write(fileOut);
							} catch (IOException e) {
								Common.tampilErrorJikaAdmin(e);
							}

							detailperkuliahans.clear();
							detailperkuliahans = null;
							try {
								java.io.File rptFile = report.simpanLaporan();
								downloadPath.setValue(rptFile.getAbsolutePath());
							} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) GradingHelper laporan"); }
							label.setValue("");
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							try {
								java.io.File rptFile = report.simpanLaporan();
								downloadPath.setValue(rptFile.getAbsolutePath());
							} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) GradingHelper laporan"); }
							label.setValue("");
						} finally {
							// PENCEGAHAN FILE LOCK LEAK
							if (fileOut != null) {
								try {
									fileOut.close();
								} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/GradingHelper.java:2871");
								}
							}
							// PENCEGAHAN DATABASE CONNECTION LEAK
							closeSession(session);
						}

					}
				}).start();

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		}
	}
}
