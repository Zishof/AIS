package ais.action.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.GajiPokok;
import ais.ui.util.WaktuUtil;

/**
 * Servlet implementation class AmbilLaporanMahasiswa
 */
public class AmbilLaporanDaftarRiwayatHidup extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLaporanDaftarRiwayatHidup() {
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
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Enumeration<String> enumeration = request.getParameterNames();
		while (enumeration.hasMoreElements()) {
			String param = enumeration.nextElement();
			System.out.print("  " + param + " = " + request.getParameter(param));
		}

		String type = request.getParameter("type") == null ? Report.HTML : request.getParameter("type");

		String email = request.getParameter("email") == null ? "-11111111" : request.getParameter("email");
		String userId = request.getParameter("userId") == null ? "-11111111" : request.getParameter("userId");

		if (email.equals("default@liferay.com") || request.getParameter("email") == null) {
			response.setContentType("text/html;charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println(
					"<font style=\"font-family: serif;font-size: x-large;color: blue;\">Anda harus login terlebih dahulu sebelum melihat daftar riwayat hidup pegawai.</font></body></html");
			out.close();
			return;
		}

		Session mySession = null;
		Tbmuser tbmuser = null;
		Pegawai pegawai = null;
		try {
			mySession = HibernateUtil.getSessionFactory().openSession();
			tbmuser = (Tbmuser) mySession.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.eq("userId", userId), Restrictions.eq("email", email)))
					.setMaxResults(1).uniqueResult();
			if (tbmuser == null || tbmuser.getUserId() == null) {
				pegawai = (Pegawai) mySession.createCriteria(Pegawai.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.add(Restrictions.eq("email", email)).setMaxResults(1).uniqueResult();
				if (pegawai != null) {
					tbmuser = (Tbmuser) mySession.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("pegawai", pegawai))
							.setMaxResults(1).uniqueResult();
					if (tbmuser == null || tbmuser.getUserId() == null) {
						tbmuser = new Tbmuser();
						tbmuser.setPegawai(pegawai);
						tbmuser.setUserId(email);
						tbmuser.setUserNama(pegawai.getNama());
						tbmuser.setEmail(email);
						tbmuser.setIs_encripted(false);
						tbmuser.setRoot(true);
						tbmuser.setSatuanKerja(pegawai.getSatuanKerja());
						tbmuser.setUserRole(new Tbmrole(Tbmrole.PEGAWAI));
						tbmuser.setUserPassword(email);
						tbmuser.setUserShow(1);
						mySession.getTransaction().begin();
						mySession.save(tbmuser);
						mySession.getTransaction().commit();
					}
				}
			}

			if (pegawai == null) {
				pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
			}
		} finally {
			if (mySession != null) {
				try { mySession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanDaftarRiwayatHidup.java:134");}
				try { mySession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanDaftarRiwayatHidup.java:135");}
				try { mySession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanDaftarRiwayatHidup.java:136");}
			}
		}

		if (pegawai == null) {
			response.setContentType("text/html;charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println("<font style=\"font-family: serif;font-size: xx-large;color: red;\">Akun anda "
					+ (request.getParameter("email") == null ? "" : "(" + email + ")")
					+ " belum terhubung ke data pegawai. Harap segera menghubungi administrator untuk menghubungkan data anda ke data pegawai.</font></body></html");
			out.close();
			return;
		}

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		parameters.put("id", pegawai.getId());
		String myfile = CommonMedia.loadPathFileFotoLangsung(new Tbmuser(pegawai));
		parameters.put("foto", myfile);
		Common.insertProperty(Pegawai.class, pegawai, parameters, "", 2);
		GajiPokok gajiPokok = pegawai.ambilGajiPokok(WaktuUtil.getDate());
		if(gajiPokok != null) {
			Common.insertProperty(GajiPokok.class, gajiPokok, parameters, "gp", 1);
		}
		File file = Report.generateFileReport(type, parameters, "employ/daftar_riwayat_hidup",
				ais.ui.util.WaktuUtil.getDate(), Common.locale);

		if (type.equals(Report.HTML)) {
			String textAll = "";
			String text = "";
			Reader reader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(reader);
			try {
				while ((text = bufferedReader.readLine()) != "") {
					textAll += text;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanDaftarRiwayatHidup.java:171");
			}
			bufferedReader.close();
			reader.close();

			response.setContentType("text/html;charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println(textAll);
			out.close();
			return;
		}

		response.setContentType("application/" + type);

		ServletOutputStream out = response.getOutputStream();
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
