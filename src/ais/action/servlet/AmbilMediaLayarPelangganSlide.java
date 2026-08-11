package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.sql.Blob;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.LayarPelangganSlide;

/**
 * Servlet publik (tanpa otentikasi -- lihat pola {@code AmbilMediaProduk}) yang
 * menyajikan gambar {@link LayarPelangganSlide} lewat {@code GET
 * /AmbilMediaLayarPelangganSlide?id=&lt;id&gt;}. Blob dibaca sekali dari sesi
 * StreamingHibernateUtil, disalin ke cache berkas datar di {@code /media/}, lalu
 * disajikan langsung dari disk pada permintaan berikutnya -- pola SAMA persis dgn
 * {@link AmbilMediaProduk}, dipertahankan sengaja supaya kedua servlet konsisten
 * bila salah satunya perlu diperbaiki di masa depan.
 */
public class AmbilMediaLayarPelangganSlide extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	private void process(HttpServletRequest request, HttpServletResponse resp) {
		Session streamingSession = null;
		ServletContext sc = getServletContext();
		String filename = "";
		try {
			streamingSession = StreamingHibernateUtil.getInstance().openSession();
			filename = loadFile(request, resp, streamingSession).getAbsolutePath();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (streamingSession != null) {
				try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMediaLayarPelangganSlide.java:clear"); }
				try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMediaLayarPelangganSlide.java:disconnect"); }
				try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMediaLayarPelangganSlide.java:close"); }
			}
		}

		try {
			String mimeType = sc == null ? null : sc.getMimeType(filename);
			if (mimeType == null) {
				if (filename.toLowerCase().endsWith("png")) {
					mimeType = "image/png";
				} else if (filename.toLowerCase().endsWith("gif")) {
					mimeType = "image/gif";
				} else {
					mimeType = "image/jpg";
				}
			}
			resp.setContentType(mimeType);

			File file = new File(filename);
			resp.setContentLength((int) file.length());

			FileInputStream in = new FileInputStream(file);
			OutputStream out = resp.getOutputStream();
			byte[] buf = new byte[1024];
			int count;
			while ((count = in.read(buf)) >= 0) {
				out.write(buf, 0, count);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			Common.tampilErrorJikaAdmin(e);
		} catch (IOException e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private File loadFile(HttpServletRequest request, HttpServletResponse resp, Session streamingSession)
			throws Exception {
		ServletContext sc = getServletContext();
		String path = new File(sc.getRealPath("/")).getParentFile().getAbsolutePath() + "/media/";
		File mediaDic = new File(path);
		if (!mediaDic.exists()) {
			mediaDic.mkdirs();
		}

		File file = new File(sc.getRealPath("/img/Package-Warning-icon.png"));

		String strid = request.getParameter("id");
		if (strid == null || strid.trim().isEmpty()) {
			return file;
		}
		Long id = Long.parseLong(strid.trim());

		String myName = (String) streamingSession.createCriteria(LayarPelangganSlide.class)
				.add(Restrictions.eq("id", id)).setProjection(Projections.property("namaFile")).uniqueResult();
		if (myName == null) {
			myName = "slide_" + id + ".jpg";
		}

		File myfile = new File(mediaDic + "/" + strid + "_" + LayarPelangganSlide.class.getName() + "_"
				+ myName.replaceAll(" ", "_"));

		if (!myfile.exists()) {
			Blob blob = (Blob) streamingSession.createCriteria(LayarPelangganSlide.class)
					.add(Restrictions.eq("id", id)).setProjection(Projections.property("gambar")).uniqueResult();
			if (blob == null) {
				return file;
			}
			writeBlobToFile(blob, myfile);
		}

		if (!Common.isImage(myfile)) {
			return new File(Common.REAL_PATH + "/img/Package-Warning-icon.png");
		}
		return myfile;
	}

	private void writeBlobToFile(Blob blob, File file) {
		if (file != null && file.exists()) {
			return;
		}
		InputStream inputStream = null;
		try {
			file.createNewFile();
			inputStream = blob.getBinaryStream();
			FileOutputStream outputStream = new FileOutputStream(file);
			final ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
			final WritableByteChannel outputChannel = Channels.newChannel(outputStream);
			fastChannelCopy(inputChannel, outputChannel);
			inputChannel.close();
			outputChannel.close();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
		while (src.read(buffer) != -1) {
			buffer.flip();
			dest.write(buffer);
			buffer.compact();
		}
		buffer.flip();
		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
	}
}
