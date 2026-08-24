package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kegiatan;

/**
 * Servlet implementation class LaporanPesananItem
 */
public class StrukM extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public StrukM() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings({ })
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {
		String myid = request.getParameter("id");

		try {
			if (myid.startsWith("EE")) {
				myid = Common.desEncrypter.get().decrypt(myid.substring(2));
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/StrukM.java:70");
		}

		System.out.println("myid = " + myid);

		// FIX NumberFormatException "For input string: \"\"": parameter "id" boleh kosong/hilang
		// (mis. tautan struk lama/rusak), atau dekripsi "EE..." di atas gagal diam-diam sehingga
		// myid tersisa bukan angka. JANGAN Long.parseLong mentah -- guard dulu (pola sama dgn
		// Document.java#parseLong / #downloadDocument) dan balas 400, jangan sampai servlet crash.
		Long idKegiatan = parseLong(myid);
		if (idKegiatan == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID struk tidak valid.");
			return;
		}

		Kegiatan pembayaranSiswa = null;
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			pembayaranSiswa = (Kegiatan) session.createCriteria(Kegiatan.class)
					.add(Restrictions.idEq(idKegiatan)).uniqueResult();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/StrukM.java:83");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/StrukM.java:84");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/StrukM.java:85");}
			}
		}
		if (pembayaranSiswa == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Data struk tidak ditemukan.");
			return;
		}

		File file = CommonReportHelper.cetakBuktipembayaranMahasiswa(pembayaranSiswa, false);

		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=\"struk_pembayaran.pdf\"");

		ServletOutputStream out = resp.getOutputStream();
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			int length;
			byte[] buffer = new byte[1024];
			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
			out.flush();
		} finally {
			if (in != null) {
				try { in.close(); } catch (Exception ignored) { }
			}
		}
	}

	/** Parse aman: null (bukan exception) bila value kosong/tidak berupa angka. */
	private static Long parseLong(String value) {
		try {
			return value == null || value.trim().length() == 0 ? null : Long.valueOf(value.trim());
		} catch (Exception e) {
			return null;
		}
	}

}
