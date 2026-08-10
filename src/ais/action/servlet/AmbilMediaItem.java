package ais.action.servlet;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoGambarItem;

/**
 * Servlet implementation class AmbilMedia
 */
public class AmbilMediaItem extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilMediaItem() {
		super();
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

		Session streamingSession = null;
		ServletContext sc = getServletContext();
		// File filexx = new File(sc.getRealPath("/img/book.jpg"));
		String filename = "";
		try {
			streamingSession = StreamingHibernateUtil.getInstance().openSession();
			filename = loadFile(request, resp, streamingSession).getAbsolutePath();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (streamingSession != null) {
				try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMediaItem.java:78");}
				try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMediaItem.java:79");}
				try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMediaItem.java:80");}
			}
		}

		try {
			// Get the MIME type of the image
			String mimeType = sc==null?null:sc.getMimeType(filename);
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

			OutputStream out = resp.getOutputStream();

			// Copy the contents of the file to the output stream
			byte[] buf = new byte[1024];
			int count = 0;
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

		File file = new File(sc.getRealPath("/img/book.jpg"));

		String strid = request.getParameter("id");
		String height = request.getParameter("height");
		String width = request.getParameter("width");
		String myName = (String) streamingSession.createCriteria(FotoGambarItem.class)
				.add(Restrictions.eq("item", Long.parseLong(strid.trim()))).addOrder(Order.desc("id"))
				.setProjection(Projections.property("nama")).setMaxResults(1).uniqueResult();

		if (strid != null && myName != null) {
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + myName + "\"");

			File myfile = new File(mediaDic + "/" + strid + "_" + FotoGambarItem.class.getName() + "_"
					+ myName.toString().replaceAll(" ", "_"));

			System.out.println("myfile = " + myfile + ", " + myfile.exists());

			if (!myfile.exists()) {
				// KE-FIX (Large Objects may not be used in auto-commit mode): kolom "foto" adalah
				// Large Object (oid) Postgres -- kursor LO-nya terikat pada transaksi yang AKTIF
				// SAAT BARIS DIAMBIL, bukan saat isi-nya dibaca. Membungkus transaksi hanya di
				// sekitar writeBlobToFile() (versi sebelumnya) TIDAK cukup: query createCriteria()
				// di bawah sudah terlanjur jalan di mode autocommit sebelum transaksi dimulai,
				// sehingga blob.getBinaryStream() tetap gagal. Transaksi HARUS sudah aktif SEBELUM
				// query yang mengambil kolom Blob dijalankan.
				org.hibernate.Transaction txLo = null;
				try {
					txLo = streamingSession.beginTransaction();
					Blob blob = (Blob) streamingSession.createCriteria(FotoGambarItem.class)
							.add(Restrictions.eq("item", Long.parseLong(strid.trim()))).addOrder(Order.desc("id"))
							.setProjection(Projections.property("foto")).setMaxResults(1).uniqueResult();
					if (blob == null) {
						txLo.commit();
						txLo = null;
						if (!Common.isImage(file)) {
							return new File(Common.REAL_PATH + "/img/book.jpg");
						}
						return file;
					}
					writeBlobToFile(blob, myfile);
					txLo.commit();
					txLo = null;
				} catch (Exception exLo) {
					if (txLo != null) {
						try { txLo.rollback(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMediaItem.java:180");}
					}
					Common.tampilErrorJikaAdmin(exLo);
				}
			}
			file = myfile;
		}

		if (height != null && width != null) {
			File filekecil = new File(
					mediaDic + "/" + height + "px_" + width + "px_" + strid + "_" + FotoGambarItem.class.getName() + "_"
							+ (myName == null ? "__" : myName.toString().replaceAll(" ", "_")));

			System.out.println("filekecil = " + filekecil + ", " + filekecil.exists());

			if (filekecil.exists()) {
				if (!Common.isImage(filekecil)) {
					return new File(Common.REAL_PATH + "/img/book.jpg");
				}
				return filekecil;
			}

			try {
				BufferedImage originalImage = ImageIO.read(file);
				int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

				BufferedImage resizeImagePng = resizeImage(originalImage, Integer.parseInt(width),
						Integer.parseInt(height), type);
				ImageIO.write(resizeImagePng, "jpg", filekecil);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			file = filekecil;
		}

		if (!Common.isImage(file)) {
			return new File(Common.REAL_PATH + "/img/book.jpg");
		}

		return file;

	}

	private void writeBlobToFile(Blob blob, File file) {

		InputStream inputStream = null;

		if (file != null && file.exists()) {
			return;
		} else {
			try {
				file.createNewFile();
				inputStream = blob.getBinaryStream();

				FileOutputStream outputStream = new FileOutputStream(file);

				// get an channel from the stream
				final ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
				final WritableByteChannel outputChannel = Channels.newChannel(outputStream);
				// copy the channels
				fastChannelCopy(inputChannel, outputChannel);
				// closing the channels
				inputChannel.close();
				outputChannel.close();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
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

	public BufferedImage resizeImage(BufferedImage originalImage, int IMG_WIDTH, int IMG_HEIGHT, int type) {
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
		g.dispose();

		return resizedImage;
	}
}
