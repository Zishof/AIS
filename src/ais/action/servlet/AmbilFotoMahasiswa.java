package ais.action.servlet;

import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FotoBiodataMahasiswa;
import ais.database.model.file.FotoMahasiswa;

/**
 * Servlet implementation class AmbilFotoMahasiswa
 */
public class AmbilFotoMahasiswa extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilFotoMahasiswa() {
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

	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {

		String nim = request.getParameter("nim");
		ServletContext sc = getServletContext();
		if (nim == null || nim.trim().equals("")) {
			sc.log("nim harus diisi !");
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		Session session = null;
		Session streamingSession = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("nim", nim == null ? null : nim.trim())).setMaxResults(1).uniqueResult();

			streamingSession = StreamingHibernateUtil.getInstance().openSession();

			BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1)
					.uniqueResult();

			FileFoto fileFoto = (FotoBiodataMahasiswa) streamingSession.createCriteria(FotoBiodataMahasiswa.class)
					.add(Restrictions.eq("biodataMahasiswa",
							biodataMahasiswa == null ? null : biodataMahasiswa.getId()))
					.add(Restrictions.eq("fotoUtama", true)).setMaxResults(1).uniqueResult();

			// Blob blob = fotoBiodataMahasiswa == null ? null
			// : fotoBiodataMahasiswa.getFoto();

			if (fileFoto == null) {
				fileFoto = (FotoMahasiswa) streamingSession.createCriteria(FotoMahasiswa.class)
						.addOrder(Order.desc("id")).add(Restrictions.eq("mahasiswa", mahasiswa.getId()))
						.setMaxResults(1).uniqueResult();

			}

			if (fileFoto != null) {
				String headerKey = "Content-Disposition";
				String headerValue = String.format("attachment; filename=\"%s\"", fileFoto.getNama());
				resp.setHeader(headerKey, headerValue);
			}

			resp.setContentType("image/png");

			ServletOutputStream out = resp.getOutputStream();
			FileInputStream in = new FileInputStream(fileFoto.ambilFile());
			int length = (int) in.available();
			// int length = (int) photo.length();

			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];

			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}

			in.close();
			out.flush();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (streamingSession != null) {
				try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:131");}
				try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:132");}
				try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:133");}
			}
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:136");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:137");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:138");}
			}
		}

	}

}
