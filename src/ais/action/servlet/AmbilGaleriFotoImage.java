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
import ais.database.model.file.GaleriFotoImage;

/**
 * Servlet yang menyajikan gambar galeri foto ({@link GaleriFotoImage})
 * berdasarkan ID galeri ({@code id}) dan/atau ID baris gambar spesifik
 * ({@code idData}) yang dikirim TANPA enkripsi lewat parameter request, dengan
 * dukungan opsional untuk versi thumbnail hasil resize ({@code height}/{@code width}).
 * <p>
 * Bila {@code idData} dikirim, baris {@link GaleriFotoImage} dengan ID tersebut
 * (ID barisnya sendiri) dipakai langsung untuk mengambil blob; bila tidak,
 * dipakai baris TERBARU (id terbesar) yang berelasi dengan galeri {@code id}
 * yang diminta. Blob disalin sekali ke berkas cache lokal di direktori
 * {@code <webapp>/../media/}, lalu permintaan berikutnya untuk kombinasi
 * {@code id}/{@code idData} yang sama langsung membaca berkas cache.
 * </p>
 * <p>
 * Seluruh pembacaan (termasuk blob Large Object PostgreSQL) dibungkus dalam
 * SATU transaksi Hibernate yang dibuka di
 * {@link #process(HttpServletRequest, HttpServletResponse)} sebelum memanggil
 * {@link #loadFile(HttpServletRequest, HttpServletResponse, Session)}, dan
 * di-commit setelahnya -- lih. komentar pada {@code process()} perihal
 * "Large Objects may not be used in auto-commit mode".
 * </p>
 * <p>
 * <b>Catatan keamanan:</b> servlet ini TIDAK memiliki gerbang otentikasi/
 * otorisasi apa pun, dan parameter {@code id}/{@code idData} adalah ID
 * numerik polos yang lazimnya berurutan -- siapa pun yang bisa menebak/
 * mengiterasi ID dapat mengunduh gambar galeri foto mana pun tanpa login.
 * Pola "anonim + id sekuensial" yang sama seperti servlet {@code Ambil*} lain
 * di paket ini.
 * </p>
 */
public class AmbilGaleriFotoImage extends HttpServlet {
	/** ID versi serialisasi tetap untuk kontrak {@link java.io.Serializable} milik {@link HttpServlet}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Membuat instance servlet. Tidak ada inisialisasi khusus di luar konstruktor
	 * bawaan {@link HttpServlet#HttpServlet()}.
	 */
	public AmbilGaleriFotoImage() {
		super();
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP; parameter {@code id}/{@code idData}, {@code height}/{@code width} (opsional) menentukan gambar yang diminta
	 * @param response respons HTTP; isi gambar (atau {@code /img/book.jpg} default) ditulis ke sini
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doGet}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doGet}, tidak pernah dilempar keluar method ini
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Menangani permintaan HTTP POST dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}, dengan perilaku
	 * yang identik dengan {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP; parameter {@code id}/{@code idData}, {@code height}/{@code width} (opsional) menentukan gambar yang diminta
	 * @param response respons HTTP; isi gambar (atau {@code /img/book.jpg} default) ditulis ke sini
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Membuka transaksi Hibernate, menentukan berkas gambar yang akan disajikan
	 * lewat {@link #loadFile(HttpServletRequest, HttpServletResponse, Session)},
	 * meng-commit transaksi, lalu menyalin isi berkas ke response dengan
	 * {@code Content-Type} yang ditebak dari ekstensi nama berkas (fallback ke
	 * {@code image/jpg} bila tidak dikenali).
	 * <p>
	 * Transaksi WAJIB sudah aktif sebelum {@code loadFile} membaca kolom blob
	 * (Large Object PostgreSQL); bila terjadi exception di mana pun sepanjang
	 * proses, transaksi di-rollback di blok {@code finally} alih-alih
	 * di-commit. Sesi {@link StreamingHibernateUtil} yang dibuka di sini
	 * selalu ditutup (clear/disconnect/close) di blok {@code finally} yang sama.
	 * </p>
	 *
	 * @param request permintaan HTTP; parameter {@code id}/{@code idData} (salah satu wajib), {@code height}/{@code width} (opsional) menentukan gambar yang diminta
	 * @param resp respons HTTP tujuan penulisan isi berkas
	 */
	private void process(HttpServletRequest request, HttpServletResponse resp) {

		Session streamingSession = null;
		org.hibernate.Transaction streamingTx = null;
		ServletContext sc = getServletContext();

		String filename = "";
		try {
			streamingSession = StreamingHibernateUtil.getInstance().openSession();
			// PostgreSQL Large Object (Blob.getBinaryStream) TIDAK boleh dibaca dalam mode
			// auto-commit ("Large Objects may not be used in auto-commit mode" / "invalid
			// large-object descriptor"). WAJIB dalam transaksi: buka transaksi selama membaca
			// blob (loadFile → writeBlobToFile streaming), commit setelah selesai.
			streamingTx = streamingSession.beginTransaction();
			filename = loadFile(request, resp, streamingSession).getAbsolutePath();
			streamingTx.commit();
			streamingTx = null;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (streamingTx != null) {
				try { streamingTx.rollback(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilGaleriFotoImage.java:86");}
			}
			if (streamingSession != null) {
				try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilGaleriFotoImage.java:89");}
				try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilGaleriFotoImage.java:90");}
				try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilGaleriFotoImage.java:91");}
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

		String idData = request.getParameter("idData");
		String strid = request.getParameter("id");
		String height = request.getParameter("height");
		String width = request.getParameter("width");
		String myName = (String) (!Common.isNumber(strid) ? "Foto"
				: streamingSession.createCriteria(GaleriFotoImage.class)
						.add(Restrictions.eq("galeriFoto", Long.parseLong(strid.trim()))).addOrder(Order.desc("id"))
						.setProjection(Projections.property("nama")).setMaxResults(1).uniqueResult());

		if (strid != null && myName != null) {
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + myName + "\"");

			File myfile = new File(mediaDic + "/" + strid + "___" + idData + "_" + GaleriFotoImage.class.getName() + "_"
					+ myName.toString().replaceAll(" ", "_"));

			System.out.println("myfile = " + myfile + ", " + myfile.exists());

			if (!myfile.exists()) {

				Blob blob = null;

				if (idData != null) {

					blob = (Blob) streamingSession.createCriteria(GaleriFotoImage.class)
							.add(Restrictions.idEq(Long.parseLong(idData.trim())))
							.setProjection(Projections.property("foto")).setMaxResults(1).uniqueResult();

				} else {

					blob = (Blob) streamingSession.createCriteria(GaleriFotoImage.class)
							.add(Restrictions.eq("galeriFoto", Long.parseLong(strid.trim()))).addOrder(Order.desc("id"))
							.setProjection(Projections.property("foto")).setMaxResults(1).uniqueResult();

				}

				if (blob == null) {
					return file;
				}

				writeBlobToFile(blob, myfile);
			}
			file = myfile;
		}

		if (height != null && !height.trim().equalsIgnoreCase("null") && width != null
				&& !width.trim().equalsIgnoreCase("null")) {
			File filekecil = new File(mediaDic + "/" + height + "px_" + width + "px_" + strid + "___" + idData + "_"
					+ GaleriFotoImage.class.getName() + "_"
					+ (myName == null ? "__" : myName.toString().replaceAll(" ", "_")));

			System.out.println("filekecil = " + filekecil + ", " + filekecil.exists());

			if (filekecil.exists()) {
				return filekecil;
			}

			try {
				BufferedImage originalImage = ais.common.CommonFileMediaHelper.bacaGambarAman(file);
				if (originalImage != null) {
					int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

					BufferedImage resizeImagePng = resizeImage(originalImage, Integer.parseInt(width),
							Integer.parseInt(height), type);
					ImageIO.write(resizeImagePng, "jpg", filekecil);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}
			file = filekecil;
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

				System.out.println("writeBlobToFile -> file = " + file.getAbsolutePath());

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
