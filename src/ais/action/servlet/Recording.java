package ais.action.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;

/**
 * Komponen batas HTTP/servlet untuk recording. Tipe ini menerima input dari luar aplikasi,
 * meneruskannya ke layanan domain, lalu membentuk respons tanpa menduplikasi aturan bisnis.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * HttpServlet}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code doGet()}); operasi domain lain
 * ({@code doPost()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see HttpServlet
 */
public class Recording extends HttpServlet {
	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini. */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor default tanpa argumen, hanya meneruskan ke {@link HttpServlet#HttpServlet()}.
	 * Tidak ada state khusus yang diinisialisasi di sini.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public Recording() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menerima upload berkas rekaman video multipart ({@code Content-Type: multipart/*}) dan
	 * menyimpannya ke direktori laporan bersama ({@link Common#ambilREAL_PATH_REPORT()}) dengan
	 * nama berkas yang diambil MENTAH dari klien, lalu membalas JSON {@code {"status":"OK"}}.
	 *
	 * <p>Alur: (1) ambil nilai awal {@code video-filename} dari parameter query (bila ada);
	 * (2) parse body multipart dengan {@link ServletFileUpload}; field form biasa (termasuk
	 * field bernama {@code video-filename}, yang menimpa nilai dari langkah 1) dibaca sebagai
	 * teks, sedangkan bagian non-form-field pertama dianggap konten video; (3) bila ada konten
	 * video, tulis ke {@code Common.ambilREAL_PATH_REPORT() + "/" + fileNameVideo} dengan
	 * {@link StandardCopyOption#REPLACE_EXISTING} (menimpa berkas lama tanpa peringatan bila
	 * nama sama); (4) balas {@code Content-Type: application/json}, header
	 * {@code Access-Control-Allow-Origin: *}, dan badan {@code {"status":"OK"}} — status ini
	 * SELALU "OK" walau upload sebenarnya gagal, karena exception pada blok upload hanya
	 * dicatat ke {@link ais.common.ErrorAuditUtil} dan tidak mengubah respons.</p>
	 *
	 * <p><b>Keamanan — path traversal dan tulis berkas sembarang tanpa gerbang otentikasi:</b>
	 * {@code fileNameVideo} dipakai APA ADANYA untuk membentuk path tujuan tanpa sanitasi
	 * (tidak ada penolakan {@code ../}, tidak ada pembatasan ekstensi/karakter, tidak ada
	 * pengecekan bahwa hasil resolusi path tetap berada di dalam direktori laporan). Endpoint
	 * ini dipetakan di {@code web.xml} sebagai {@code /Recording} tanpa
	 * {@code security-constraint} khusus, sehingga tunduk pada aturan tangkapan-semua
	 * {@code IS_AUTHENTICATED_ANONYMOUSLY} — dapat dipanggil siapa pun tanpa login. Akibatnya
	 * pemanggil anonim berpotensi menulis berkas dengan isi dan nama bebas (termasuk memakai
	 * {@code ../} untuk keluar dari direktori laporan) ke lokasi mana pun yang terjangkau oleh
	 * hak tulis proses server, dan {@code Access-Control-Allow-Origin: *} membuka pemanggilan
	 * lintas-origin dari browser mana pun. Header {@code Access-Control-Allow-Origin} yang
	 * longgar dikombinasikan dengan tulis berkas sembarang ini adalah kerentanan serius yang
	 * berdiri sendiri, terlepas dari fungsi rekaman video yang dimaksud.</p>
	 *
	 * @param request  request HTTP masuk; body multipart-nya berisi metadata form (termasuk
	 *                 {@code video-filename}) dan konten berkas video yang diunggah
	 * @param response response HTTP keluar; selalu diisi JSON {@code {"status":"OK"}} dengan
	 *                 header CORS terbuka, terlepas dari keberhasilan penulisan berkas
	 * @throws ServletException tidak pernah dilempar keluar dari sini karena kegagalan upload
	 *                          ditelan oleh blok catch; dipertahankan hanya karena tanda
	 *                          tangan {@link HttpServlet#doGet}
	 * @throws IOException      bila penulisan respons akhir gagal
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub

		String fileNameVideo = request.getParameter("video-filename");
		InputStream fileContent = null;
		try {
			List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
			System.out.println("items size" + items.size());
			for (FileItem item : items) {
				if (item.isFormField()) {
					// Process regular form field (input type="text|radio|checkbox|etc", select,
					// etc).
					String fieldName = item.getFieldName();
					String fieldValue = item.getString();
					System.out.println("fieldName -> " + fieldName + " fieldValue " + fieldValue);

					if (fieldName.equalsIgnoreCase("video-filename")) {
						fileNameVideo = fieldValue;
					}

				} else {
					fileContent = item.getInputStream();
				}
			}

			if (fileContent != null) {
				File outputfile = new File(Common.ambilREAL_PATH_REPORT() + "/" + fileNameVideo);
				System.out.println("outputfile video -> " + outputfile.getAbsolutePath());
				java.nio.file.Files.copy(fileContent, outputfile.toPath(), StandardCopyOption.REPLACE_EXISTING);

				IOUtils.closeQuietly(fileContent);
			}

			System.out.println("Uploading done..");
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Recording.java:76");
		}

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("status", "OK");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Recording.java:84");
		}
		String body = jsonObject.toString();
		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		PrintWriter writer = response.getWriter();

		writer.write(body);
	}

	/**
	 * Menangani POST dengan perilaku identik seperti GET: langsung mendelegasikan ke
	 * {@link #doGet(HttpServletRequest, HttpServletResponse)} tanpa logika tambahan, karena
	 * upload multipart pada praktiknya selalu dikirim sebagai POST namun ditangani sama saja
	 * bila diterima lewat GET.
	 *
	 * @param request  request HTTP masuk; diteruskan apa adanya ke {@link #doGet}
	 * @param response response HTTP keluar; diteruskan apa adanya ke {@link #doGet}
	 * @throws ServletException bila {@link #doGet} melempar {@code ServletException}
	 * @throws IOException      bila {@link #doGet} melempar {@code IOException}
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
