package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;

/**
 * Servlet implementation class AmbilMedia
 */
public class AmbilFile extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilFile() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	private void process(HttpServletRequest request, HttpServletResponse resp) {

		ServletContext sc = getServletContext();
		File file = new File(sc.getRealPath("/img/administrator-icon_default.png"));
		try {
			file = loadFile(request, resp);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/AmbilFile.java:61");
		}

		// loadFile() mengembalikan null bila response sudah di-redirect (mis. berkas
		// tersimpan di GDrive) -> tidak ada lagi yang perlu ditulis ke stream.
		if (file == null) {
			return;
		}

		// Berkas ditemukan di DB tapi file fisiknya sudah hilang dari disk (terhapus
		// manual/migrasi server) -> balas 404 ramah, jangan lempar FileNotFoundException
		// mentah saat FileInputStream dibuka di bawah. Pola sama seperti Document.java /
		// AmbilPenelitianDanPengabdian.java.
		if (!file.exists() || file.length() <= 0L) {
			try {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File tidak ditemukan.");
			} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFile.java:notfound"); }
			return;
		}

		try {
			// Get the MIME type of the image
			String filename = file.getName();
			String mimeType = sc == null ? null : sc.getMimeType(filename);
			if (mimeType == null) {
				if (filename.toLowerCase().endsWith("png")) {
					mimeType = "image/png";
				} else if (filename.toLowerCase().endsWith("jpg")) {
					mimeType = "image/jpg";
				} else if (filename.toLowerCase().endsWith("gif")) {
					mimeType = "image/gif";
				} else {
					mimeType = "image/jpg";
				}
				// sc.log("Could not get MIME type of " + filename);
				// resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				// return;
			}

			// Set content type
			resp.setContentType(mimeType);
			String headerKey = "Content-Disposition";
			String headerValue = String.format("attachment; filename=\"%s\"", filename);
			resp.setHeader(headerKey, headerValue);

			// Set content size
			resp.setContentLength((int) file.length());

			// Open the file and output streams
			FileInputStream fileInputStream = new FileInputStream(file);

			OutputStream out = resp.getOutputStream();

			int i;
			while ((i = fileInputStream.read()) != -1) {
				out.write(i);
			}
			fileInputStream.close();
			out.close();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFile.java:103");
//			Common.tampilErrorJikaAdmin(e);
		}

	}

	@SuppressWarnings("rawtypes")
	private File loadFile(HttpServletRequest request, HttpServletResponse resp) throws Exception {

		ServletContext sc = getServletContext();

		String clazzData = request.getParameter("clazz");
		Class clazz = LampiranLain.class;
		try {
			clazz = Class.forName(clazzData);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFile.java:118");
			// TODO: handle exception
		}
		File file = new File(sc.getRealPath("/img/" + FileFotoLain.iconNggakAda(clazz)));

		String strid = Common.desEncrypter.get().decrypt(request.getParameter("id"));
//		FileFoto data = (FileFoto) streamingSession.createCriteria(clazz).addOrder(Order.desc("id"))
//				.add(Restrictions.idEq(Long.parseLong(strid))).setMaxResults(1).uniqueResult();
		FileFoto data = null;
		try {
			data = (FileFoto) FileFotoLain.ambil(true, Long.parseLong(strid), "", clazz, true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFile.java:129");
			// TODO: handle exception
		}

		if (data != null) {
			if (data.getGdrive() != null) {
				resp.sendRedirect(data.forwardGDriveUrl());
				return null;
			}
			try {
				file = data.ambilFile();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		System.out.println("data -> " + data + " -> strid " + strid + ", clazz => " + clazz + ", file " + file);

		return file;
	}

	public void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
		while (src.read(buffer) != -1) {
			// prepare the buffer to be drained
			buffer.flip();
			// write to the channel, may block
			dest.write(buffer);
			// If partial transfer, shift remainder down
			// If buffer is empty, same as doing clear()
			buffer.compact();
		}
		// EOF will leave buffer in fill state
		buffer.flip();
		// make sure the buffer is fully drained.
		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
	}

}
