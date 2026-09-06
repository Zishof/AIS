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
 *
 * <p><b>CATATAN KEAMANAN (task_11e5ae35, DITAMBAL).</b> Whitelist {@link #PAKET_DIIZINKAN}
 * hanya membatasi PAKET class laporan yang boleh dijalankan, bukan gerbang otentikasi; servlet
 * sebelumnya SAMA SEKALI TIDAK memeriksa apakah pemanggil sudah login, dan URL
 * {@code /jalankan-laporan} juga tidak punya {@code intercept-url} khusus di
 * {@code applicationContext-security.xml} (jatuh ke default paling bawah
 * {@code pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY"}), sehingga siapa pun tanpa login
 * bisa menjalankan laporan apa pun di paket whitelist yang sudah mengimplementasikan kontrak ini
 * (mis. {@code LaporanStokItem}, yang parameter-generator-nya juga tidak melakukan scoping
 * satker/perpustakaan). Sudah ditambal dengan gerbang {@code Common.getCurrentUser(request) != null}
 * fail-closed di awal {@link #proses} plus {@code intercept-url} eksplisit
 * {@code IS_AUTHENTICATED_REMEMBERED} untuk {@code /jalankan-laporan} di
 * {@code applicationContext-security.xml}. Gerbang ini HANYA memastikan pemanggil sudah login;
 * scoping satker/kepemilikan data per-laporan (mis. {@code LaporanStokItem} menerima parameter
 * {@code perpustakaan} apa pun dari request tanpa validasi kepemilikan) TIDAK ikut ditambal di
 * sini dan perlu ditinjau per-class laporan.</p>
 */
public class JalankanLaporanJsp extends HttpServlet {

	/** Versi serialisasi tetap 1L; servlet tidak pernah benar-benar diserialisasi ke stream. */
	private static final long serialVersionUID = 1L;

	/**
	 * Daftar awalan (prefix) nama paket Java yang boleh dijalankan lewat {@code kelas}. Ini
	 * HANYA membatasi paket, bukan gerbang otentikasi -- lihat catatan keamanan di javadoc kelas.
	 */
	private static final String[] PAKET_DIIZINKAN = new String[] { "ais.action.report.", "ais.action.master.sapto.",
			"ais.action.master.dashboard." };

	/**
	 * Menangani permintaan GET; seluruh logika didelegasikan ke {@link #proses}.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP keluar
	 * @throws ServletException tidak pernah dilempar, hanya dideklarasikan oleh kontrak servlet
	 * @throws java.io.IOException diteruskan dari {@link #proses} (lewat {@link #tampilPesan})
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, java.io.IOException {
		proses(request, response);
	}

	/**
	 * Menangani permintaan POST dengan perilaku identik dengan
	 * {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP keluar
	 * @throws ServletException tidak pernah dilempar, hanya dideklarasikan oleh kontrak servlet
	 * @throws java.io.IOException diteruskan dari {@link #proses} (lewat {@link #tampilPesan})
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, java.io.IOException {
		proses(request, response);
	}

	/**
	 * Memeriksa apakah nama class laporan berada di salah satu paket yang diizinkan
	 * ({@link #PAKET_DIIZINKAN}). Ini hanya pemeriksaan awalan string paket, BUKAN pemeriksaan
	 * hak akses pengguna.
	 *
	 * @param kelas nama class lengkap (fully-qualified) laporan yang diminta; boleh {@code null}
	 * @return {@code true} jika {@code kelas} diawali salah satu paket yang diizinkan
	 */
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

	/**
	 * Logika inti: memvalidasi paket class laporan yang diminta ({@link #diizinkan}), memuat
	 * class lewat refleksi, memanggil kontrak statis {@code namaTemplateLaporanJsp()} dan
	 * {@code generateParameterDariRequestJsp(HttpServletRequest)} milik class tersebut, lalu
	 * men-generate berkas laporan (PDF/XLS/HTML/DOCX/RTF, default PDF jika format tak dikenal)
	 * dan menstream-nya ke respons. Galat apa pun (termasuk class tak ditemukan, kontrak belum
	 * diimplementasikan, atau kegagalan generate) ditampilkan sebagai pesan HTML sederhana,
	 * bukan stack trace mentah.
	 *
	 * <p>Lihat catatan keamanan pada javadoc kelas: method ini menggerbangi pemanggil lewat
	 * {@code Common.getCurrentUser(request) != null} (fail-closed) sebelum memvalidasi paket
	 * class ({@link #diizinkan}) dan menjalankan laporan.</p>
	 *
	 * @param request permintaan HTTP masuk; parameter {@code kelas}, {@code format},
	 *        {@code unduh}, dan {@code locale} dibaca di sini, sisanya diteruskan apa adanya ke
	 *        {@code generateParameterDariRequestJsp} milik class laporan
	 * @param response respons HTTP keluar
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void proses(HttpServletRequest request, HttpServletResponse response) {
		if (Common.getCurrentUser(request) == null) {
			tampilPesan(response, "Anda harus login terlebih dahulu untuk menjalankan laporan ini.");
			return;
		}
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

	/**
	 * Menulis pesan galat sederhana bergaya HTML (bukan stack trace mentah) ke respons, dipakai
	 * saat laporan gagal dijalankan atau tidak diizinkan.
	 *
	 * @param response respons HTTP keluar
	 * @param pesan pesan yang ditampilkan ke pengguna; boleh {@code null} (ditampilkan sebagai kosong)
	 */
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
