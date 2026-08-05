package ais.action.report.helper;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Utilitas bersama untuk menjalankan laporan (yang asalnya hanya tampil di ZK)
 * dari konteks JSP / Servlet.
 *
 * Pola konversi menu "URL = nama class" -> JSP:
 *  - Tiap class laporan menambah 2 method static yang REUSABLE dengan ZK:
 *      public static String namaTemplateLaporanJsp();                       // key jrxml
 *      public static java.util.Map generateParameterDariRequestJsp(HttpServletRequest req);
 *    (param-gen-nya dipakai bersama oleh jalur ZK & JSP).
 *  - Servlet generic {@code ais.action.servlet.JalankanLaporanJsp} memantulkan kedua
 *    method itu lalu memanggil {@code Report.generateFileReport(...)} dan stream PDF/XLS/HTML.
 *
 * Tidak ada dependensi ZK di sini, jadi aman dipanggil dari servlet/JSP.
 */
public class LaporanJspUtil {

	private LaporanJspUtil() {
	}

	/** Format tanggal yang diterima dari form JSP (input HTML date = yyyy-MM-dd lebih dulu). */
	private static final String[] POLA_TANGGAL = new String[] { "yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy",
			"yyyy/MM/dd", "MM/dd/yyyy" };

	/**
	 * Parse tanggal dari parameter form JSP. Kosong/null -> null (pemanggil yang
	 * menentukan default).
	 */
	public static Date parseTanggal(String s) {
		if (s == null) {
			return null;
		}
		String v = s.trim();
		if (v.length() == 0) {
			return null;
		}
		for (int i = 0; i < POLA_TANGGAL.length; i++) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(POLA_TANGGAL[i]);
				sdf.setLenient(false);
				return sdf.parse(v);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/LaporanJspUtil.java:51");
				// coba pola berikutnya
			}
		}
		return null;
	}

	/** Long dari parameter (kosong/null -> null). */
	public static Long parseLong(String s) {
		if (s == null || s.trim().length() == 0) {
			return null;
		}
		try {
			return Long.valueOf(s.trim());
		} catch (Exception e) {
			return null;
		}
	}

	/** Content-type sesuai format Report (pdf/xls/html/docx/rtf). */
	public static String contentType(String format) {
		String f = format == null ? "pdf" : format.trim().toLowerCase();
		if (f.equals("xls") || f.equals("xlsx")) {
			return "application/vnd.ms-excel";
		}
		if (f.equals("html")) {
			return "text/html; charset=UTF-8";
		}
		if (f.equals("docx")) {
			return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
		}
		if (f.equals("rtf")) {
			return "application/rtf";
		}
		return "application/pdf";
	}

	/** Stream file hasil report ke response (tanpa menutup response). */
	public static void stream(File file, HttpServletResponse resp, String format, boolean unduh) throws Exception {
		resp.setContentType(contentType(format));
		if (file != null) {
			resp.setContentLength((int) file.length());
			String ext = format == null ? "pdf" : format.trim().toLowerCase();
			if (unduh) {
				resp.setHeader("Content-Disposition", "attachment; filename=laporan_" + System.currentTimeMillis() + "."
						+ ext);
			} else {
				resp.setHeader("Content-Disposition", "inline; filename=laporan." + ext);
			}
		}
		ServletOutputStream out = resp.getOutputStream();
		FileInputStream in = new FileInputStream(file);
		try {
			byte[] buffer = new byte[8192];
			int length;
			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
		} finally {
			try {
				in.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/LaporanJspUtil.java:112");
			}
		}
		out.flush();
	}
}
