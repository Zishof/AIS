package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoDosen;

/**
 * Servlet implementation class AmbilFotoDosen
 */
public class AmbilFotoDosen extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilFotoDosen() {
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

		String id = request.getParameter("id");
		ServletContext sc = getServletContext();
		if (id == null || id.trim().equals("")) {
			sc.log("id harus diisi !");
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		Long idDosen = -1L;
		try {
			idDosen = Long.parseLong(id.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoDosen.java:76");
		}

		try {
			FotoDosen fotoDosen = null;
			Session streamingSession = null;
			try {
				streamingSession = StreamingHibernateUtil.getInstance().openSession();
				fotoDosen = (FotoDosen) streamingSession.createCriteria(FotoDosen.class)
						.add(Restrictions.eq("dosen", idDosen)).setMaxResults(1).uniqueResult();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (streamingSession != null) {
					try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoDosen.java:90");}
					try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoDosen.java:91");}
					try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoDosen.java:92");}
				}
			}

			InputStream inputStream;
			if (fotoDosen == null) {
				String filename = sc.getRealPath("/img/administrator-icon.png");

				// Get the MIME type of the image
				String mimeType = sc==null?null:sc.getMimeType(filename);
				if (mimeType == null) {
					sc.log("Could not get MIME type of " + filename);
					resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}

				// Set content type
				resp.setContentType(mimeType);
				String headerKey = "Content-Disposition";
				String headerValue = String.format("attachment; filename=\"%s\"", filename);
				resp.setHeader(headerKey, headerValue);

				// Set content size
				File file = new File(filename);
				resp.setContentLength((int) file.length());

				// Open the file and output streams
				FileInputStream in = new FileInputStream(file);
				inputStream = in;

				OutputStream out = resp.getOutputStream();

				// Copy the contents of the file to the output stream
				byte[] buf = new byte[1024];
				int count = 0;
				while ((count = inputStream.read(buf)) >= 0) {
					out.write(buf, 0, count);
				}
				inputStream.close();
				out.close();
			} else {
				try {
					// Blob photo = fotoDosen.getFoto();
					// Set content type
					resp.setContentType("image/png");
					// resp.setContentLength((int) inputStream.available());

					ServletOutputStream out = resp.getOutputStream();
					FileInputStream in = new FileInputStream(fotoDosen.ambilFile());
					int length = (int) in.available();

					int bufferSize = 1024;
					byte[] buffer = new byte[bufferSize];

					while ((length = in.read(buffer)) != -1) {
//						System.out.println("writing " + length + " bytes");
						out.write(buffer, 0, length);
					}

					in.close();
					out.flush();
				} catch (Exception e) {
					String filename = sc.getRealPath("/img/administrator-icon.png");

					// Get the MIME type of the image
					String mimeType = sc==null?null:sc.getMimeType(filename);
					if (mimeType == null) {
						sc.log("Could not get MIME type of " + filename);
						resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						return;
					}

					// Set content type
					resp.setContentType(mimeType);

					// Set content size
					File file = new File(filename);
					resp.setContentLength((int) file.length());

					// Open the file and output streams
					FileInputStream in = new FileInputStream(file);
					inputStream = in;

					OutputStream out = resp.getOutputStream();

					// Copy the contents of the file to the output stream
					byte[] buf = new byte[1024];
					int count = 0;
					while ((count = inputStream.read(buf)) >= 0) {
						out.write(buf, 0, count);
					}
					inputStream.close();
					out.close();
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

}
