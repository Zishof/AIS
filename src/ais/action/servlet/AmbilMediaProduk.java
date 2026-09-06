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
import ais.database.model.file.FotoGambarProduk;

/**
 * Servlet yang menyajikan gambar produk ({@link FotoGambarProduk}) dengan DUA
 * mode akses: berdasarkan ID baris foto itu sendiri ({@code fotoId}, dipakai
 * galeri Ubah Produk &amp; carousel Kasir untuk menampilkan foto tertentu apa
 * adanya), atau berdasarkan ID produk ({@code id}, mengambil foto TERBARU
 * milik produk tersebut, dipakai thumbnail katalog). Kedua parameter dikirim
 * TANPA enkripsi; dukungan opsional tersedia untuk versi thumbnail hasil
 * resize ({@code height}/{@code width}, hanya berlaku pada mode {@code id}).
 * <p>
 * Blob gambar disimpan di kolom {@code foto} pada tabel
 * {@link FotoGambarProduk}; pada permintaan pertama untuk suatu kombinasi
 * parameter, blob disalin sekali ke berkas cache lokal di direktori
 * {@code <webapp>/../media/}, lalu permintaan berikutnya untuk kombinasi yang
 * sama langsung membaca berkas cache.
 * </p>
 * <p>
 * <b>Catatan keamanan:</b> servlet ini TIDAK memiliki gerbang otentikasi/
 * otorisasi apa pun, dan parameter {@code id}/{@code fotoId} adalah ID
 * numerik polos yang lazimnya berurutan -- siapa pun yang bisa menebak/
 * mengiterasi ID dapat mengunduh gambar produk mana pun tanpa login. Pola
 * "anonim + id sekuensial" yang sama seperti servlet {@code Ambil*} lain di
 * paket ini (mis. {@code AmbilMediaItem}, {@code AmbilImageItemPerHalaman}).
 * </p>
 */
public class AmbilMediaProduk extends HttpServlet {
	/** ID versi serialisasi tetap untuk kontrak {@link java.io.Serializable} milik {@link HttpServlet}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Membuat instance servlet. Tidak ada inisialisasi khusus di luar konstruktor
	 * bawaan {@link HttpServlet#HttpServlet()}.
	 */
	public AmbilMediaProduk() {
		super();
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP; parameter {@code fotoId} ATAU {@code id}, {@code height}/{@code width} (opsional) menentukan gambar yang diminta
	 * @param response respons HTTP; isi gambar (atau ikon peringatan default) ditulis ke sini
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
	 * @param request permintaan HTTP; parameter {@code fotoId} ATAU {@code id}, {@code height}/{@code width} (opsional) menentukan gambar yang diminta
	 * @param response respons HTTP; isi gambar (atau ikon peringatan default) ditulis ke sini
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
	 * @param request permintaan HTTP; parameter {@code fotoId} ATAU {@code id} (salah satu wajib), {@code height}/{@code width} (opsional, hanya berlaku untuk mode {@code id}) menentukan gambar yang diminta
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
				try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMediaProduk.java:78");}
				try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMediaProduk.java:79");}
				try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMediaProduk.java:80");}
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
	 * Menentukan berkas gambar produk yang akan disajikan, dengan dua mode akses
	 * yang saling eksklusif.
	 * <p>
	 * Mode 1 -- {@code fotoId} (ID baris {@link FotoGambarProduk} itu sendiri,
	 * BUKAN id produk): mengambil baris itu langsung lewat {@link Session#get}
	 * apa adanya (tanpa memandang produk mana yang memilikinya), lalu menyalin
	 * blob {@code foto}-nya ke cache dan mengembalikannya. Dipakai galeri "Ubah
	 * Produk" dan carousel Kasir untuk merender daftar foto (satu per satu) yang
	 * ID-nya sudah didapat lebih dulu dari {@code produk_foto_list}. Bila baris
	 * tidak ada atau blob-nya kosong, mengembalikan ikon peringatan
	 * {@code /img/Package-Warning-icon.png}.
	 * </p>
	 * <p>
	 * Mode 2 -- {@code id} (id produk, dipakai bila {@code fotoId} tidak dikirim):
	 * mengambil foto TERBARU (id terbesar) milik produk tersebut. Bila cache-nya
	 * belum ada, blob disalin lewat {@link #writeBlobToFile(Blob, File)}. Bila
	 * parameter {@code height} dan {@code width} keduanya dikirim, method
	 * menghasilkan (atau membaca dari cache) versi thumbnail lewat
	 * {@link #resizeImage(BufferedImage, int, int, int)}. Hasil akhir selalu
	 * divalidasi lewat {@link Common#isImage(File)}; bila bukan gambar valid,
	 * jatuh ke ikon peringatan.
	 * </p>
	 * <p>
	 * Penambahan parameter {@code fotoId} (ditandai komentar "Gap-closure" pada
	 * kode) TIDAK mengubah perilaku parameter {@code id} yang sudah ada
	 * sebelumnya -- keduanya independen, salah satu boleh dipakai sesuai
	 * kebutuhan pemanggil.
	 * </p>
	 *
	 * @param request permintaan HTTP; parameter {@code fotoId} ATAU {@code id} (salah satu wajib), {@code height}/{@code width} (opsional, mode {@code id} saja)
	 * @param resp respons HTTP; header {@code Content-Disposition} diisi dengan nama berkas di sini
	 * @param streamingSession sesi Hibernate (dibuka pemanggil) dipakai untuk seluruh query pada method ini
	 * @return berkas gambar (asli atau thumbnail) yang harus disajikan, atau ikon peringatan {@code /img/Package-Warning-icon.png} sebagai fallback
	 * @throws Exception bila parameter tidak valid atau query/penyalinan blob gagal; diteruskan ke pemanggil ({@link #process}) yang menanganinya lewat {@link Common#tampilErrorJikaAdmin(Exception)}
	 */
	private File loadFile(HttpServletRequest request, HttpServletResponse resp, Session streamingSession)
			throws Exception {

		ServletContext sc = getServletContext();
		String path = new File(sc.getRealPath("/")).getParentFile().getAbsolutePath() + "/media/";
		File mediaDic = new File(path);
		if (!mediaDic.exists()) {
			mediaDic.mkdirs();
		}

		File file = new File(sc.getRealPath("/img/Package-Warning-icon.png"));

		// Gap-closure "foto produk banyak" -- param BARU `fotoId` (id baris FotoGambarProduk itu
		// SENDIRI, BUKAN id produk) untuk mengambil SATU foto tertentu apa adanya, dipakai galeri
		// Ubah Produk & carousel Kasir (produk_foto_list mengembalikan daftar id, masing-masing
		// dirender lewat URL ini). TIDAK mengubah perilaku param `id` yang sudah ada di bawah
		// (tetap "foto TERBARU milik produk ini", dipakai thumbnail katalog) -- dua param berbeda,
		// boleh dipakai salah satu.
		String strFotoId = request.getParameter("fotoId");
		if (strFotoId != null && !strFotoId.trim().isEmpty()) {
			FotoGambarProduk baris = (FotoGambarProduk) streamingSession.get(FotoGambarProduk.class,
					Long.parseLong(strFotoId.trim()));
			if (baris == null || baris.getFoto() == null) {
				return new File(Common.REAL_PATH + "/img/Package-Warning-icon.png");
			}
			String namaFoto = baris.getNama() == null ? "foto.jpg" : baris.getNama();
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + namaFoto + "\"");
			File fotoFile = new File(mediaDic + "/foto_" + strFotoId.trim() + "_"
					+ namaFoto.replaceAll(" ", "_"));
			if (!fotoFile.exists()) {
				writeBlobToFile(baris.getFoto(), fotoFile);
			}
			if (!Common.isImage(fotoFile)) {
				return new File(Common.REAL_PATH + "/img/Package-Warning-icon.png");
			}
			return fotoFile;
		}

		String strid = request.getParameter("id");
		String height = request.getParameter("height");
		String width = request.getParameter("width");
		String myName = (String) streamingSession.createCriteria(FotoGambarProduk.class)
				.add(Restrictions.eq("produk", Long.parseLong(strid.trim()))).addOrder(Order.desc("id"))
				.setProjection(Projections.property("nama")).setMaxResults(1).uniqueResult();

		if (strid != null && myName != null) {
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + myName + "\"");

			File myfile = new File(mediaDic + "/" + strid + "_" + FotoGambarProduk.class.getName() + "_"
					+ myName.toString().replaceAll(" ", "_"));

			System.out.println("myfile = " + myfile + ", " + myfile.exists());

			if (!myfile.exists()) {
				Blob blob = (Blob) streamingSession.createCriteria(FotoGambarProduk.class)
						.add(Restrictions.eq("produk", Long.parseLong(strid.trim()))).addOrder(Order.desc("id"))
						.setProjection(Projections.property("foto")).setMaxResults(1).uniqueResult();
				if (blob == null) {
					if (!Common.isImage(file)) {
						return new File(Common.REAL_PATH + "/img/Package-Warning-icon.png");
					}
					return file;
				}

				writeBlobToFile(blob, myfile);
			}
			file = myfile;
		}

		if (height != null && width != null) {
			File filekecil = new File(
					mediaDic + "/" + height + "px_" + width + "px_" + strid + "_" + FotoGambarProduk.class.getName()
							+ "_" + (myName == null ? "__" : myName.toString().replaceAll(" ", "_")));

			System.out.println("filekecil = " + filekecil + ", " + filekecil.exists());

			if (filekecil.exists()) {
				if (!Common.isImage(filekecil)) {
					return new File(Common.REAL_PATH + "/img/Package-Warning-icon.png");
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
			return new File(Common.REAL_PATH + "/img/Package-Warning-icon.png");
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
	 * (mode {@code id}) untuk menghasilkan versi thumbnail saat parameter
	 * {@code height}/{@code width} dikirim.
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
