package ais.action.servlet;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.report.Report;
import ais.action.report.helper.LaporanJspUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;

/**
 * Runner generic untuk menu yang URL-nya berupa NAMA CLASS laporan (bukan .zul),
 * dikonversi ke JSP namun tetap memakai class tersebut.
 *
 * Pemakaian (dari index.jsp tiap menu, biasanya via iframe):
 *   {ROOT}/jalankan-laporan?kelas=ais.action.report.format1.library.LaporanStokItem
 *          &format=pdf&unduh=0&<param-filter...>
 *
 * Kontrak class laporan (REUSABLE dengan ZK):
 *   public static String namaTemplateLaporanJsp();                            // key jrxml, mis "library/stok_item"
 *   public static java.util.Map generateParameterDariRequestJsp(HttpServletRequest req);
 *
 * Servlet ini hanya boleh menjalankan class di paket laporan (whitelist) yang
 * mengekspos kedua method itu; selain itu ditolak.
 */
public class JalankanLaporanJsp extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String[] PAKET_DIIZINKAN = new String[] { "ais.action.report.", "ais.action.master.sapto.",
			"ais.action.master.dashboard." };

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, java.io.IOException {
		proses(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, java.io.IOException {
		proses(request, response);
	}

	private boolean diizinkan(String kelas) {
		if (kelas == null) {
			return false;
		}
		for (int i = 0; i < PAKET_DIIZINKAN.length; i++) {
			if (kelas.startsWith(PAKET_DIIZINKAN[i])) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void proses(HttpServletRequest request, HttpServletResponse response) {
		String kelas = request.getParameter("kelas");
		String format = request.getParameter("format");
		if (format == null || format.trim().length() == 0) {
			format = Report.PDF;
		}
		format = format.trim().toLowerCase();
		boolean unduh = "1".equals(request.getParameter("unduh")) || "true".equalsIgnoreCase(request.getParameter("unduh"));

		org.hibernate.Session hibernateSession = null;
		try {
			if (!diizinkan(kelas)) {
				tampilPesan(response, "Kelas laporan tidak diizinkan: " + kelas);
				return;
			}
			// pastikan ada sesi Hibernate utk query param-gen + fill report
			hibernateSession = HibernateUtil.getSessionFactory().openSession();

			Class<?> c = Class.forName(kelas.trim());
			Method mJrxml;
			Method mParam;
			try {
				mJrxml = c.getMethod("namaTemplateLaporanJsp");
				mParam = c.getMethod("generateParameterDariRequestJsp", HttpServletRequest.class);
			} catch (NoSuchMethodException nsm) {
				tampilPesan(response, "Laporan ini belum dikonversi ke JSP (method namaTemplateLaporanJsp / "
						+ "generateParameterDariRequestJsp belum ada di " + kelas + ").");
				return;
			}

			String jrxml = (String) mJrxml.invoke(null);
			Map parameters = (Map) mParam.invoke(null, request);
			if (parameters == null) {
				tampilPesan(response, "Parameter laporan kosong / filter belum lengkap.");
				return;
			}

			String fmt = format;
			if (!fmt.equals(Report.PDF) && !fmt.equals(Report.XLS) && !fmt.equals(Report.HTML)
					&& !fmt.equals(Report.DOCX) && !fmt.equals(Report.RTF)) {
				fmt = Report.PDF;
			}

			java.util.Locale locale = Common.locale;
			String loc = request.getParameter("locale");
			if (loc != null && loc.equalsIgnoreCase("en")) {
				locale = Common.localeEn;
			}

			File file = Report.generateFileReport(fmt, parameters, jrxml, ais.ui.util.WaktuUtil.getDate(), locale);
			LaporanJspUtil.stream(file, response, fmt, unduh);

		} catch (java.lang.reflect.InvocationTargetException ite) {
			Throwable cause = ite.getCause() == null ? ite : ite.getCause();
			tampilPesan(response, "Gagal membuat laporan: " + cause.getMessage());
			Common.tampilErrorJikaAdmin(cause instanceof Exception ? (Exception) cause : new Exception(cause));
		} catch (Exception e) {
			tampilPesan(response, "Gagal membuat laporan: " + e.getMessage());
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (hibernateSession != null) {
				try { hibernateSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JalankanLaporanJsp.java:123");}
				try { hibernateSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JalankanLaporanJsp.java:124");}
				try { hibernateSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JalankanLaporanJsp.java:125");}
			}
		}
	}

	private void tampilPesan(HttpServletResponse response, String pesan) {
		try {
			response.setContentType("text/html; charset=UTF-8");
			response.getWriter().print("<div style=\"font-family:Arial,sans-serif;padding:24px;color:#b91c1c;\">"
					+ "<h4>Tidak dapat menampilkan laporan</h4><p>" + (pesan == null ? "" : pesan) + "</p></div>");
			response.getWriter().flush();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/JalankanLaporanJsp.java:136");
		}
	}
}
