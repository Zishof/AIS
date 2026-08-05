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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoItem;
import ais.database.model.library.Item;

/**
 * Servlet implementation class AmbilLampiranItem
 */
public class AmbilLampiranItem extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLampiranItem() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

	private void process(HttpServletRequest request, HttpServletResponse resp) {

		Session streamingSession = StreamingHibernateUtil.getInstance()
				.openSession();
		String filename = "";
		try {
			filename = loadFile(request, resp, streamingSession)
					.getAbsolutePath();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e); 
		} finally {
			// FIX bocor: streamingSession = getSessionFactory().openSession() (dedikasi, TIDAK di MAP),
			// sehingga StreamingHibernateUtil.closeSession() (menutup sesi MAP) TIDAK menutupnya. Tutup manual.
			if (streamingSession != null) {
				try { streamingSession.clear(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranItem.java:80");}
				try { streamingSession.disconnect(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranItem.java:81");}
				try { streamingSession.close(); } catch (Exception eF) { ais.common.ErrorAuditUtil.record(eF, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranItem.java:82");}
			}
		}
		StreamingHibernateUtil.getInstance().closeSession();

		try {

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

	private File loadFile(HttpServletRequest request, HttpServletResponse resp,
			Session streamingSession) throws Exception {

		ServletContext sc = getServletContext();
		String path = new File(sc.getRealPath("/")).getParentFile()
				.getAbsolutePath() + "/media/";
		File mediaDic = new File(path);
		if (!mediaDic.exists()) {
			mediaDic.mkdirs();
		}

		File file = new File(sc.getRealPath("/img/book.jpg"));

		String strid = request.getParameter("id");

		String myName = (String) streamingSession
				.createCriteria(FotoItem.class)
				.add(Restrictions.idEq(Long.parseLong(strid.trim())))
				.setProjection(Projections.property("nama")).setMaxResults(1)
				.uniqueResult();

		System.out.println("myName = " + myName);

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Item item = (Item) session.createCriteria(Item.class)
					.add(Restrictions.idEq(Long.parseLong(strid)))
					.uniqueResult();
			Long jumlahDidownload = item.getJumlahDidownload();
			jumlahDidownload++;
			System.out.println("myName = " + myName + ", jumlahDidownload = "
					+ jumlahDidownload);
			item.setJumlahDidownload(jumlahDidownload);
			session.getTransaction().begin();
			Common.refreshUpdate(session, (item));
			session.getTransaction().commit();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranItem.java:155");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranItem.java:156");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranItem.java:157");}
			}
		}

		if (strid != null && myName != null) {

			File myfile = new File(mediaDic + "/" + strid + "_"
					+ FotoItem.class.getName() + "_"
					+ myName.toString().replaceAll(" ", "_"));
			if (!myfile.exists()) {
				Blob blob = (Blob) streamingSession
						.createCriteria(FotoItem.class)
						.add(Restrictions.idEq(Long.parseLong(strid.trim())))
						.setProjection(Projections.property("foto"))
						.setMaxResults(1).uniqueResult();
				if (blob == null) {
					return file;
				}

				writeBlobToFile(blob, myfile);
				file = myfile;
			}
			resp.setHeader("Content-Disposition", "attachment; filename=\""
					+ myName + "\"");
		}

		String keterangan = (String) streamingSession
				.createCriteria(FotoItem.class)
				.add(Restrictions.idEq(Long.parseLong(strid.trim())))
				.setProjection(Projections.property("keterangan"))
				.setMaxResults(1).uniqueResult();
		System.out.println("keterangan = " + keterangan);
		if (keterangan != null) {
			resp.setContentType(keterangan);
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
				final ReadableByteChannel inputChannel = Channels
						.newChannel(inputStream);
				final WritableByteChannel outputChannel = Channels
						.newChannel(outputStream);
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

	public void fastChannelCopy(final ReadableByteChannel src,
			final WritableByteChannel dest) throws IOException {
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

	public BufferedImage resizeImage(BufferedImage originalImage,
			int IMG_WIDTH, int IMG_HEIGHT, int type) {
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT,
				type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
		g.dispose();

		return resizedImage;
	}
}
