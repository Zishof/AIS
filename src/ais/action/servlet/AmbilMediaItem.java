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
 * Servlet yang menyajikan gambar item ({@link FotoGambarItem}) berdasarkan ID
 * item yang dikirim TANPA enkripsi lewat parameter request {@code id}, dengan
 * dukungan opsional untuk versi thumbnail hasil resize ({@code height}/{@code width}).
 * <p>
 * Blob gambar disimpan di kolom {@code foto} pada tabel {@link FotoGambarItem}
 * (baris TERBARU milik {@code item} yang diminta, diurutkan menurun berdasar
 * {@code id}); pada permintaan pertama untuk suatu ID, blob disalin sekali ke
 * berkas cache lokal di direktori {@code <webapp>/../media/}, lalu permintaan
 * berikutnya untuk ID yang sama langsung membaca berkas cache. Bila parameter
 * {@code height} dan {@code width} dikirim, servlet membuat (atau membaca dari
 * cache) versi thumbnail berukuran tersebut lewat
 * {@link #resizeImage(BufferedImage, int, int, int)}.
 * </p>
 * <p>
 * Pembacaan blob (Large Object PostgreSQL) WAJIB dilakukan di dalam transaksi
 * yang sudah aktif SEBELUM query pengambilannya dijalankan -- lih. komentar
 * "KE-FIX" pada {@link #loadFile(HttpServletRequest, HttpServletResponse, Session)};
 * tanpa itu PostgreSQL melempar "Large Objects may not be used in auto-commit mode".
 * </p>
 * <p>
 * <b>Catatan keamanan:</b> servlet ini TIDAK memiliki gerbang otentikasi/
 * otorisasi apa pun, dan parameter {@code id} adalah ID item numerik polos
 * yang lazimnya berurutan -- siapa pun yang bisa menebak/mengiterasi ID dapat
 * mengunduh gambar item mana pun tanpa login. Pola "anonim + id sekuensial"
 * yang sama seperti servlet {@code Ambil*} lain di paket ini (mis.
 * {@code AmbilMediaProduk}, {@code AmbilImageItemPerHalaman}).
 * </p>
 */
public class AmbilMediaItem extends HttpServlet {
	/** ID versi serialisasi tetap untuk kontrak {@link java.io.Serializable} milik {@link HttpServlet}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Membuat instance servlet. Tidak ada inisialisasi khusus di luar konstruktor
	 * bawaan {@link HttpServlet#HttpServlet()}.
	 */
	public AmbilMediaItem() {
		super();
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP; parameter {@code id} (wajib), {@code height}/{@code width} (opsional) menentukan gambar yang diminta
	 * @param response respons HTTP; isi gambar (atau ikon default) ditulis ke sini
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
	 * @param request permintaan HTTP; parameter {@code id} (wajib), {@code height}/{@code width} (opsional) menentukan gambar yang diminta
	 * @param response respons HTTP; isi gambar (atau ikon default) ditulis ke sini
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Menentukan berkas gambar yang akan disajikan lewat
	 * {@link #loadFile(HttpServletRequest, HttpServletResponse, Session)}, lalu
	 * menyalin isinya ke response dengan {@code Content-Type} yang ditebak dari
	 * ekstensi nama berkas (fallback ke {@code image/jpg} bila tidak dikenali).
	 * <p>
	 * Sesi {@link StreamingHibernateUtil} dibuka di sini dan selalu ditutup
	 * (clear/disconnect/close) di blok {@code finally} SEBELUM path berkas hasil
	 * {@link #loadFile} dipakai untuk menulis isi berkas ke response.
	 * </p>
	 *
	 * @param request permintaan HTTP; parameter {@code id} (wajib), {@code height}/{@code width} (opsional) menentukan gambar yang diminta
	 * @param resp respons HTTP tujuan penulisan isi berkas
	 */
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

	/**
	 * Mencari baris {@link FotoGambarItem} TERBARU milik {@code item} (parameter
	 * {@code id}), menyalin kolom blob {@code foto}-nya ke berkas cache lokal
	 * (bila belum ada), dan (opsional) menghasilkan versi thumbnail.
	 * <p>
	 * Langkah kerja:
	 * <ol>
	 *   <li>Memastikan direktori cache {@code <webapp>/../media/} ada.</li>
	 *   <li>Mengambil kolom {@code nama} baris {@link FotoGambarItem} terbaru
	 *       (id terbesar) milik {@code item} yang diminta.</li>
	 *   <li>Bila ditemukan dan berkas cache-nya belum ada: membuka transaksi
	 *       Hibernate BARU lebih dulu (wajib untuk Large Object PostgreSQL),
	 *       mengambil kolom blob {@code foto} di dalam transaksi tersebut, lalu
	 *       menyalinnya ke berkas cache lewat {@link #writeBlobToFile(Blob, File)}
	 *       dan meng-commit transaksi. Kegagalan di tengah jalan memicu rollback.</li>
	 *   <li>Bila parameter {@code height} dan {@code width} keduanya dikirim:
	 *       jika versi thumbnail dengan ukuran itu sudah ada di cache, langsung
	 *       dikembalikan; jika belum, gambar asli dibaca, di-resize lewat
	 *       {@link #resizeImage(BufferedImage, int, int, int)}, disimpan sebagai
	 *       {@code jpg} baru ke cache, lalu dikembalikan.</li>
	 *   <li>Sebelum dikembalikan, berkas divalidasi lewat {@link Common#isImage(File)};
	 *       bila bukan gambar valid, method jatuh ke {@code /img/book.jpg}.</li>
	 * </ol>
	 * </p>
	 *
	 * @param request permintaan HTTP; parameter {@code id} (wajib, ID item), {@code height}/{@code width} (opsional, ukuran thumbnail)
	 * @param resp respons HTTP; header {@code Content-Disposition} diisi dengan nama asli berkas di sini
	 * @param streamingSession sesi Hibernate (dibuka pemanggil) dipakai untuk seluruh query/transaksi pada method ini
	 * @return berkas gambar (asli atau thumbnail) yang harus disajikan, atau {@code /img/book.jpg} sebagai fallback
	 * @throws Exception bila {@code id} tidak valid atau query/penyalinan blob gagal; diteruskan ke pemanggil ({@link #process}) yang menanganinya lewat {@link Common#tampilErrorJikaAdmin(Exception)}
	 */
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

	/**
	 * Menyalin isi {@code blob} ke {@code file} sekali saja: bila {@code file}
	 * sudah ada di disk, method langsung kembali tanpa melakukan apa pun (blob
	 * tidak dibaca ulang). Bila belum ada, method membuat berkas baru lalu
	 * menyalin seluruh isi {@link Blob#getBinaryStream()} lewat
	 * {@link #fastChannelCopy(ReadableByteChannel, WritableByteChannel)}.
	 *
	 * @param blob sumber data biner dari kolom {@code foto}; boleh {@code null} hanya bila {@code file} sudah ada
	 * @param file berkas cache tujuan penulisan
	 */
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

	/**
	 * Menyalin seluruh isi {@code src} ke {@code dest} memakai buffer langsung
	 * (direct {@link ByteBuffer}) berukuran 16 KiB, dengan pola baca-flip-tulis-
	 * compact standar NIO sampai {@code src} habis, lalu mengosongkan sisa buffer
	 * yang belum tertulis.
	 *
	 * @param src kanal sumber data biner yang akan disalin
	 * @param dest kanal tujuan penulisan data biner
	 * @throws IOException bila operasi baca/tulis pada salah satu kanal gagal
	 */
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

	/**
	 * Mengubah ukuran {@code originalImage} menjadi kanvas berukuran
	 * {@code IMG_WIDTH}&times;{@code IMG_HEIGHT} dengan tipe {@link BufferedImage}
	 * sesuai parameter {@code type}, memakai penggambaran ulang sederhana
	 * ({@link Graphics2D#drawImage}) tanpa interpolasi kualitas khusus. Dipakai
	 * oleh {@link #loadFile(HttpServletRequest, HttpServletResponse, Session)}
	 * untuk menghasilkan versi thumbnail saat parameter {@code height}/{@code width}
	 * dikirim.
	 *
	 * @param originalImage gambar sumber yang akan digambar ulang
	 * @param IMG_WIDTH lebar kanvas hasil, dalam piksel
	 * @param IMG_HEIGHT tinggi kanvas hasil, dalam piksel
	 * @param type salah satu konstanta tipe {@link BufferedImage} (mis. {@link BufferedImage#TYPE_INT_ARGB})
	 * @return gambar hasil resize berukuran {@code IMG_WIDTH}&times;{@code IMG_HEIGHT}
	 */
	public BufferedImage resizeImage(BufferedImage originalImage, int IMG_WIDTH, int IMG_HEIGHT, int type) {
		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
		g.dispose();

		return resizedImage;
	}
}
