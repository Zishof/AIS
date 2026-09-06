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
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoInformasiPerpustakaan;

/**
 * Servlet yang menyajikan lampiran foto/gambar informasi perpustakaan
 * ({@link FotoInformasiPerpustakaan}) berdasarkan ID baris yang dikirim
 * TANPA enkripsi lewat parameter request {@code id}.
 * <p>
 * Blob foto disimpan di kolom {@code foto} pada tabel
 * {@link FotoInformasiPerpustakaan}; pada permintaan pertama untuk suatu ID,
 * blob tersebut disalin sekali ke berkas cache lokal di direktori
 * {@code <webapp>/../media/} (nama berkas memuat ID dan nama asli, lih.
 * {@link #loadFile(HttpServletRequest, HttpServletResponse, Session)}), lalu
 * permintaan berikutnya untuk ID yang sama langsung membaca berkas cache
 * tersebut tanpa mengulang query blob. Bila baris tidak ditemukan atau blob-nya
 * kosong, servlet jatuh ke gambar default {@code /img/book.jpg}.
 * </p>
 * <p>
 * Header {@code Content-Type} response diisi APA ADANYA dari kolom
 * {@code keterangan} milik baris data (dipakai sebagai nilai MIME type),
 * bukan dideteksi dari isi berkas -- nilai kolom ini sepenuhnya dikendalikan
 * oleh data yang tersimpan di database.
 * </p>
 * <p>
 * <b>Catatan keamanan:</b> servlet ini TIDAK memiliki gerbang otentikasi/
 * otorisasi apa pun, dan parameter {@code id} adalah ID baris numerik polos
 * (bukan terenkripsi seperti pada {@code AmbilFile}/{@code AmbilMedia}) yang
 * lazimnya berurutan -- siapa pun yang bisa menebak/mengiterasi ID dapat
 * mengunduh lampiran informasi perpustakaan mana pun tanpa login. Pola
 * "anonim + id sekuensial" yang sama seperti servlet {@code Ambil*} lain di
 * paket ini; lih. juga kembarannya {@code AmbilLampiranInformasiRab} yang
 * strukturnya identik untuk entitas {@link ais.database.model.file.FotoInformasiRab}.
 * </p>
 */
public class AmbilLampiranInformasiPerpustakaan extends HttpServlet {
	/** ID versi serialisasi tetap untuk kontrak {@link java.io.Serializable} milik {@link HttpServlet}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Membuat instance servlet. Tidak ada inisialisasi khusus di luar konstruktor
	 * bawaan {@link HttpServlet#HttpServlet()}.
	 */
	public AmbilLampiranInformasiPerpustakaan() {
		super();
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP; parameter {@code id} berisi ID baris {@link FotoInformasiPerpustakaan} yang diminta
	 * @param response respons HTTP; isi lampiran (atau gambar default) ditulis ke sini
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doGet}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doGet}, tidak pernah dilempar keluar method ini
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Menangani permintaan HTTP POST dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}, dengan perilaku
	 * yang identik dengan {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP; parameter {@code id} berisi ID baris {@link FotoInformasiPerpustakaan} yang diminta
	 * @param response respons HTTP; isi lampiran (atau gambar default) ditulis ke sini
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Menentukan berkas yang akan disajikan lewat
	 * {@link #loadFile(HttpServletRequest, HttpServletResponse, Session)}, lalu
	 * menyalin isinya ke response.
	 * <p>
	 * Sesi {@link StreamingHibernateUtil} dibuka di sini dan selalu ditutup
	 * (clear/disconnect/close) di blok {@code finally} SEBELUM path berkas hasil
	 * {@link #loadFile} dipakai untuk membaca &amp; menulis isi berkas ke response
	 * -- pemisahan ini aman karena {@code loadFile} sudah menuntaskan seluruh
	 * akses database (termasuk penyalinan blob ke berkas cache) sebelum
	 * mengembalikan path-nya.
	 * </p>
	 *
	 * @param request permintaan HTTP; parameter {@code id} menentukan lampiran yang diminta
	 * @param resp respons HTTP tujuan penulisan isi berkas
	 */
	private void process(HttpServletRequest request, HttpServletResponse resp) {

		Session streamingSession = null;
		String filename = "";
		try {
			streamingSession = StreamingHibernateUtil.getInstance().openSession();
			filename = loadFile(request, resp, streamingSession)
					.getAbsolutePath();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (streamingSession != null) {
				try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranInformasiPerpustakaan.java:75");}
				try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranInformasiPerpustakaan.java:76");}
				try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiranInformasiPerpustakaan.java:77");}
			}
		}

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

	/**
	 * Mencari baris {@link FotoInformasiPerpustakaan} berdasarkan {@code id},
	 * menyalin kolom blob {@code foto}-nya ke berkas cache lokal (bila belum ada),
	 * dan mengisi header response.
	 * <p>
	 * Langkah kerja:
	 * <ol>
	 *   <li>Memastikan direktori cache {@code <webapp>/../media/} ada (membuatnya
	 *       bila perlu).</li>
	 *   <li>Mengambil kolom {@code nama} baris dengan {@code id} yang diminta lewat
	 *       proyeksi Hibernate (tanpa memuat entitas penuh).</li>
	 *   <li>Bila ditemukan, menentukan nama berkas cache ({@code <id>_<nama kelas>_<nama>}),
	 *       dan bila berkas cache itu belum ada, mengambil kolom blob {@code foto}
	 *       lewat proyeksi terpisah lalu menyalinnya ke berkas cache lewat
	 *       {@link #writeBlobToFile(Blob, File)}. Header {@code Content-Disposition}
	 *       diisi dengan nama asli berkas.</li>
	 *   <li>Mengisi {@code Content-Type} response langsung dari kolom
	 *       {@code keterangan} baris tersebut (dipakai sebagai nilai MIME type
	 *       apa adanya).</li>
	 * </ol>
	 * Bila {@code id} tidak valid (gagal di-{@code parseLong}) atau baris tidak
	 * ditemukan, method mengembalikan berkas default {@code /img/book.jpg}.
	 * </p>
	 *
	 * @param request permintaan HTTP; parameter {@code id} wajib berisi ID baris {@link FotoInformasiPerpustakaan}
	 * @param resp respons HTTP; header {@code Content-Disposition}/{@code Content-Type} diisi di sini
	 * @param streamingSession sesi Hibernate (dibuka pemanggil) dipakai untuk seluruh query pada method ini
	 * @return berkas yang harus disajikan ke klien (berkas cache, atau {@code /img/book.jpg} sebagai default)
	 * @throws Exception bila {@code id} tidak valid atau query/penyalinan blob gagal; diteruskan ke pemanggil ({@link #process}) yang menanganinya lewat {@link Common#tampilErrorJikaAdmin(Exception)}
	 */
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
				.createCriteria(FotoInformasiPerpustakaan.class)
				.add(Restrictions.idEq(Long.parseLong(strid.trim())))
				.setProjection(Projections.property("nama")).setMaxResults(1)
				.uniqueResult();

		System.out.println("myName = " + myName);

		if (strid != null && myName != null) {

			File myfile = new File(mediaDic + "/" + strid + "_"
					+ FotoInformasiPerpustakaan.class.getName() + "_"
					+ myName.toString().replaceAll(" ", "_"));
			if (!myfile.exists()) {
				Blob blob = (Blob) streamingSession
						.createCriteria(FotoInformasiPerpustakaan.class)
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
				.createCriteria(FotoInformasiPerpustakaan.class)
				.add(Restrictions.idEq(Long.parseLong(strid.trim())))
				.setProjection(Projections.property("keterangan"))
				.setMaxResults(1).uniqueResult();
		System.out.println("keterangan = " + keterangan);
		if (keterangan != null) {
			resp.setContentType(keterangan);
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

	/**
	 * Mengubah ukuran {@code originalImage} menjadi kanvas berukuran
	 * {@code IMG_WIDTH}&times;{@code IMG_HEIGHT} dengan tipe {@link BufferedImage}
	 * sesuai parameter {@code type}, memakai penggambaran ulang sederhana
	 * ({@link Graphics2D#drawImage}) tanpa interpolasi kualitas khusus.
	 * <p>
	 * Method ini tidak dipanggil di mana pun pada alur kerja servlet ini (foto
	 * informasi perpustakaan selalu disajikan pada ukuran aslinya); ia hanya
	 * merupakan utilitas yang tersedia untuk potensi pemakaian di masa depan.
	 * </p>
	 *
	 * @param originalImage gambar sumber yang akan digambar ulang
	 * @param IMG_WIDTH lebar kanvas hasil, dalam piksel
	 * @param IMG_HEIGHT tinggi kanvas hasil, dalam piksel
	 * @param type salah satu konstanta tipe {@link BufferedImage} (mis. {@link BufferedImage#TYPE_INT_ARGB})
	 * @return gambar hasil resize berukuran {@code IMG_WIDTH}&times;{@code IMG_HEIGHT}
	 */
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
