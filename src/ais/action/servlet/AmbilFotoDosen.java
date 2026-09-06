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
 * Servlet yang menyajikan foto dosen berdasarkan ID dosen yang dikirim lewat
 * parameter request {@code id}.
 * <p>
 * Alur kerja: {@link #process(HttpServletRequest, HttpServletResponse)} membaca
 * parameter {@code id}, mencari baris {@link FotoDosen} yang berelasi dengan
 * dosen tersebut (query {@code Restrictions.eq("dosen", idDosen)}, tanpa filter
 * status aktif/nonaktif dosen), lalu menuliskan isi berkas foto (lewat
 * {@link FotoDosen#ambilFile()}) ke response dengan {@code Content-Type:
 * image/png}. Bila baris foto tidak ditemukan, ID tidak valid, atau berkas
 * fisiknya gagal dibaca, servlet jatuh ke gambar ikon default
 * {@code /img/administrator-icon.png} sebagai fallback.
 * </p>
 * <p>
 * <b>Catatan keamanan:</b> servlet ini dapat diakses lewat {@code doGet}/
 * {@code doPost} TANPA gerbang otentikasi atau otorisasi apa pun. Karena
 * {@code id} adalah ID baris numerik yang lazimnya berurutan, siapa pun yang
 * bisa menebak/mengiterasi nilainya dapat mengunduh foto dosen mana pun tanpa
 * login -- pola arsitektur "anonim + id sekuensial" yang sama seperti pada
 * beberapa servlet {@code Ambil*} lain di paket ini (mis. {@code AmbilFotoMahasiswa}).
 * </p>
 */
public class AmbilFotoDosen extends HttpServlet {
	/** ID versi serialisasi tetap untuk kontrak {@link java.io.Serializable} milik {@link HttpServlet}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Membuat instance servlet. Tidak ada inisialisasi khusus di luar konstruktor
	 * bawaan {@link HttpServlet#HttpServlet()}.
	 */
	public AmbilFotoDosen() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}. Exception apa pun
	 * yang dilempar oleh {@code process} ditangkap di sini dan diteruskan ke
	 * {@link Common#tampilErrorJikaAdmin(Exception)} sehingga rincian error hanya
	 * ditampilkan bila pengguna yang sedang login adalah administrator.
	 *
	 * @param request permintaan HTTP; parameter {@code id} berisi ID dosen yang fotonya diminta
	 * @param response respons HTTP; isi foto (atau ikon default) ditulis ke output stream-nya
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doGet}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doGet}, tidak pernah dilempar keluar method ini
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
	 * Menangani permintaan HTTP POST dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}, dengan perilaku
	 * dan penanganan error yang identik dengan {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP; parameter {@code id} berisi ID dosen yang fotonya diminta
	 * @param response respons HTTP; isi foto (atau ikon default) ditulis ke output stream-nya
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Mencari dan menuliskan foto dosen ke response.
	 * <p>
	 * Langkah kerja:
	 * <ol>
	 *   <li>Membaca parameter {@code id}; bila kosong, membalas status 500 dan berhenti.</li>
	 *   <li>Mem-parsing {@code id} menjadi {@code Long}; bila gagal parsing, {@code idDosen}
	 *       tetap bernilai default {@code -1L} sehingga query di bawah otomatis tidak
	 *       menemukan baris manapun (fallback ke gambar default).</li>
	 *   <li>Membuka sesi {@link StreamingHibernateUtil} terpisah untuk mencari satu
	 *       baris {@link FotoDosen} yang berelasi dengan {@code idDosen} (paling banyak
	 *       satu baris, tanpa urutan eksplisit).</li>
	 *   <li>Bila baris ditemukan, membuka berkas fisiknya lewat {@link FotoDosen#ambilFile()}
	 *       dan menyalin isinya ke response sebagai {@code image/png}.</li>
	 *   <li>Bila baris tidak ditemukan ATAU pembacaan berkas fisik gagal (exception),
	 *       menyajikan ikon fallback {@code /img/administrator-icon.png} sebagai lampiran unduhan.</li>
	 * </ol>
	 * Sesi Hibernate yang dibuka selalu ditutup (clear/disconnect/close) di blok
	 * {@code finally} sebelum method berlanjut ke penulisan response.
	 * </p>
	 *
	 * @param request permintaan HTTP; parameter {@code id} wajib berisi ID dosen
	 * @param resp respons HTTP tujuan penulisan isi foto/ikon default
	 * @throws Exception diteruskan ke pemanggil ({@link #doGet}/{@link #doPost}) yang menanganinya lewat {@link Common#tampilErrorJikaAdmin(Exception)}
	 */
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
