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
 * Servlet generik untuk menyajikan berkas lampiran ({@link FileFoto}/{@link FileFotoLain})
 * berdasarkan ID yang dikirim TERENKRIPSI lewat parameter request {@code id}
 * (didekripsi dengan {@link Common#desEncrypter}), dan kelas entitas opsional
 * lewat parameter {@code clazz}.
 * <p>
 * Bila {@code clazz} tidak dikirim atau tidak valid, servlet memakai
 * {@link LampiranLain} sebagai kelas default. {@code clazz} divalidasi lewat
 * {@link #sanitasiNamaClassFile(String)} (harus berada di paket
 * {@code ais.database.model.file} dan merupakan subkelas {@link FileFoto}) sebelum
 * dipakai pada {@link Class#forName(String)}, sehingga tidak sembarang kelas Java
 * bisa diinstansiasi lewat parameter ini.
 * </p>
 * <p>
 * Pengambilan baris dilakukan lewat {@link FileFotoLain#ambil(boolean, Object, String, Class, boolean)}
 * dengan {@code usingId = true}, yang berarti baris dicari berdasarkan ID barisnya
 * sendiri TANPA memfilter kolom {@code jenis} milik {@link FileFotoLain}. Bila baris
 * yang ditemukan tersimpan di GDrive, response di-redirect ke URL GDrive-nya; bila
 * tersimpan sebagai berkas lokal, isinya disalin langsung ke response.
 * </p>
 * <p>
 * <b>Catatan keamanan:</b> (1) servlet ini tidak memiliki gerbang otentikasi/otorisasi
 * apa pun -- siapa pun yang memiliki nilai {@code id} terenkripsi yang valid (mis.
 * disalin dari HTML halaman lain) dapat mengunduh lampiran tersebut. (2) Karena
 * {@code usingId = true}, filter {@code jenis} pada {@link FileFotoLain} diabaikan
 * sepenuhnya -- pola yang sama dengan kerentanan yang telah dikonfirmasi pada
 * servlet lampiran lain di paket ini (baris bisa terambil lewat ID-nya meski
 * jenis/namespace lampiran yang diminta berbeda dari jenis sesungguhnya baris
 * tersebut). Komentar Javadoc lama pada kelas ini keliru menyebut "AmbilMedia"
 * (bukan {@code AmbilFile}) -- artefak salin-tempel yang tidak memengaruhi
 * perilaku, hanya diperbaiki di sini sebagai dokumentasi.
 * </p>
 */
public class AmbilFile extends HttpServlet {
	/** ID versi serialisasi tetap untuk kontrak {@link java.io.Serializable} milik {@link HttpServlet}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Membuat instance servlet. Tidak ada inisialisasi khusus di luar konstruktor
	 * bawaan {@link HttpServlet#HttpServlet()}.
	 */
	public AmbilFile() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP; parameter {@code clazz} dan {@code id} (terenkripsi) menentukan lampiran yang diminta
	 * @param response respons HTTP; isi lampiran (atau redirect GDrive) ditulis ke sini
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
	 * @param request permintaan HTTP; parameter {@code clazz} dan {@code id} (terenkripsi) menentukan lampiran yang diminta
	 * @param response respons HTTP; isi lampiran (atau redirect GDrive) ditulis ke sini
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Titik masuk pemrosesan permintaan: memanggil
	 * {@link #loadFile(HttpServletRequest, HttpServletResponse)} untuk menentukan
	 * berkas yang akan disajikan, lalu menyalin isinya ke response.
	 * <p>
	 * Bila {@link #loadFile} mengembalikan {@code null} (berarti response sudah
	 * di-redirect, mis. lampiran tersimpan di GDrive), method berhenti tanpa
	 * menulis apa pun lagi ke response. Bila berkas fisik yang dikembalikan
	 * ternyata sudah tidak ada di disk atau kosong, membalas
	 * {@link HttpServletResponse#SC_NOT_FOUND 404} yang ramah alih-alih
	 * melempar {@link java.io.FileNotFoundException} mentah. Exception lain
	 * yang terjadi saat menyalin isi berkas ditelan dan dicatat lewat
	 * {@link ais.common.ErrorAuditUtil#record} tanpa mengubah status response.
	 * </p>
	 *
	 * @param request permintaan HTTP; diteruskan apa adanya ke {@link #loadFile(HttpServletRequest, HttpServletResponse)}
	 * @param resp respons HTTP tujuan penulisan isi berkas
	 */
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

	/**
	 * Menentukan berkas fisik yang akan disajikan berdasarkan parameter request
	 * {@code clazz} dan {@code id} (id terenkripsi).
	 * <p>
	 * Langkah kerja:
	 * <ol>
	 *   <li>Memvalidasi &amp; menyanitasi {@code clazz} lewat
	 *       {@link #sanitasiNamaClassFile(String)}; bila kosong/tidak valid, dipakai
	 *       {@link LampiranLain} sebagai default.</li>
	 *   <li>Menyiapkan ikon fallback {@code /img/} + {@link FileFotoLain#iconNggakAda(Class)}
	 *       milik kelas tersebut sebagai berkas default sebelum pencarian data.</li>
	 *   <li>Mendekripsi parameter {@code id} lewat {@link Common#desEncrypter} lalu
	 *       mencari baris data lewat {@link FileFotoLain#ambil(boolean, Object, String, Class, boolean)}
	 *       dengan {@code usingId = true} (lih. catatan keamanan di Javadoc kelas:
	 *       filter {@code jenis} diabaikan pada mode ini).</li>
	 *   <li>Bila baris ditemukan dan tersimpan di GDrive ({@link FileFoto#getGdrive()}
	 *       tidak null), me-redirect response ke URL GDrive-nya dan mengembalikan
	 *       {@code null} (menandakan tidak ada lagi yang perlu ditulis oleh
	 *       {@link #process(HttpServletRequest, HttpServletResponse)}).</li>
	 *   <li>Bila baris ditemukan dan tersimpan lokal, mengembalikan berkas fisiknya
	 *       lewat {@link FileFoto#ambilFile()}.</li>
	 * </ol>
	 * Bila ID gagal didekripsi/di-parse, baris tidak ditemukan, atau
	 * {@link FileFoto#ambilFile()} melempar exception, method jatuh ke berkas
	 * ikon fallback yang disiapkan di langkah kedua.
	 * </p>
	 *
	 * @param request permintaan HTTP; parameter {@code clazz} (opsional) dan {@code id} (wajib, terenkripsi)
	 * @param resp respons HTTP; dipakai untuk redirect langsung ke GDrive bila lampiran tersimpan di sana
	 * @return berkas yang harus disajikan ke klien, atau {@code null} bila response sudah di-redirect
	 * @throws Exception diteruskan ke pemanggil ({@link #process}) yang menelannya lewat pencatatan error
	 */
	@SuppressWarnings("rawtypes")
	private File loadFile(HttpServletRequest request, HttpServletResponse resp) throws Exception {

		ServletContext sc = getServletContext();

		String clazzData = sanitasiNamaClassFile(request.getParameter("clazz"));
		Class clazz = LampiranLain.class;
		if (clazzData != null && clazzData.length() > 0) {
			try {
				clazz = Class.forName(clazzData);
				if (!FileFoto.class.isAssignableFrom(clazz)) {
					clazz = LampiranLain.class;
				}
			} catch (Exception e) {
				clazz = LampiranLain.class;
				ais.common.ErrorAuditUtil.record(e, "AmbilFile: parameter clazz tidak valid: " + clazzData);
			}
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

	/**
	 * Memvalidasi nama kelas entitas lampiran yang boleh dipakai lewat parameter
	 * {@code clazz}, untuk mencegah {@link Class#forName(String)} memuat kelas
	 * Java sembarangan dari input pengguna.
	 * <p>
	 * Nilai dianggap valid hanya bila: (1) tidak null, (2) setelah dipotong pada
	 * spasi pertama dan di-{@code trim()}, hanya berisi karakter alfanumerik,
	 * {@code _}, {@code .}, atau {@code $}, dan (3) diawali dengan prefiks paket
	 * {@code ais.database.model.file.}. Nilai yang gagal validasi dikembalikan
	 * sebagai string kosong (pemanggil akan jatuh ke {@link LampiranLain} sebagai
	 * default).
	 * </p>
	 *
	 * @param value nilai mentah parameter {@code clazz} dari request, boleh {@code null}
	 * @return nama kelas yang sudah divalidasi (masih berupa {@code String}), atau string kosong bila tidak valid
	 */
	private String sanitasiNamaClassFile(String value) {
		if (value == null) {
			return "";
		}
		String text = value.trim();
		int spasi = text.indexOf(' ');
		if (spasi > 0) {
			text = text.substring(0, spasi);
		}
		if (!text.matches("[A-Za-z0-9_.$]+")) {
			return "";
		}
		if (!text.startsWith("ais.database.model.file.")) {
			return "";
		}
		return text;
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

}
