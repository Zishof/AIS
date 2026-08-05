package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * Servlet implementation class LaporanPesananItem
 */
public class Struk extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Struk() {
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {
		String myid = request.getParameter("id");

		try {
			if (myid.startsWith("EE")) {
				myid = Common.desEncrypter.get().decrypt(myid.substring(2));
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Struk.java:77");
		}

		System.out.println("myid = " + myid);

		PembayaranSiswa pembayaranSiswa = null;
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			pembayaranSiswa = (PembayaranSiswa) session.createCriteria(PembayaranSiswa.class)
					.add(Restrictions.idEq(Long.parseLong(myid))).uniqueResult();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Struk.java:90");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Struk.java:91");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Struk.java:92");}
			}
		}

		Map parameters = new HashMap();
		parameters.put("id_pembayaran", pembayaranSiswa == null || pembayaranSiswa.getId() == null ? -1L : pembayaranSiswa.getId());
		parameters.put("id_sekolah", pembayaranSiswa == null ? -1L : pembayaranSiswa.getSekolah().getId());
		parameters.put("id_bri", -1L);
		parameters.put("id_bni", -1L);
		parameters.put("id_bsi", -1L);
		parameters.put("id_va", -1L);

		String kode_transaksi = "0000000000000000000000" + (pembayaranSiswa == null ? "" : pembayaranSiswa.getId());
		kode_transaksi = StringUtils.substring(kode_transaksi, kode_transaksi.length() - 8);
		parameters.put("kode_transaksi", kode_transaksi);

		parameters.put("waktu_cetak", pembayaranSiswa == null ? Common.dateFormat1.get().format(WaktuUtil.getDate())
				: Common.dateFormat1.get().format(pembayaranSiswa.getTanggal()));

		if (pembayaranSiswa.getCalonSiswa() != null) {
			Common.insertProperty(CalonSiswa.class, pembayaranSiswa.getCalonSiswa(), parameters, "");
		} else if (pembayaranSiswa.getSiswa() != null) {
			Common.insertProperty(Siswa.class, pembayaranSiswa.getSiswa(), parameters, "");
		}

		PembayaranSiswaUtil.dataPembayaran(pembayaranSiswa, null, null, null, null, parameters);

		File file = Report.generateFileReport(Report.PDF, parameters, "sekolah/struk_pembayaran",
				ais.ui.util.WaktuUtil.getDate(), Common.locale);

		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=\"struk_pembayaran.pdf\"");

		ServletOutputStream out = resp.getOutputStream();
		FileInputStream in = new FileInputStream(file);
		int length = (int) file.length();

		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		while ((length = in.read(buffer)) != -1) {
			out.write(buffer, 0, length);
		}

		in.close();
		out.flush();
	}

}
