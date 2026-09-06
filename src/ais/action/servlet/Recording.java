package ais.action.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

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
	 * Ekstensi berkas video yang diizinkan untuk disimpan lewat {@link #doGet}. Perbandingan
	 * dilakukan tanpa memandang huruf besar/kecil pada bagian setelah titik terakhir nama berkas.
	 */
	private static final Set<String> EKSTENSI_VIDEO_DIIZINKAN = new HashSet<String>(
			Arrays.asList("webm", "mp4", "mov", "avi", "mkv", "3gp", "m4v"));

	/**
	 * Pola nama berkas yang dianggap aman: hanya huruf, angka, titik, garis bawah, dan tanda hubung.
	 * Karakter pemisah direktori ({@code /} dan {@code \}), titik dua (dipakai Windows untuk drive
	 * letter maupun alternate data stream NTFS), byte nol, dan spasi SENGAJA tidak termasuk di
	 * pola ini sehingga otomatis ditolak oleh {@link #isNamaBerkasVideoAman(String)}.
	 */
	private static final Pattern POLA_NAMA_BERKAS_AMAN = Pattern.compile("^[A-Za-z0-9._-]+$");

	/**
	 * Memvalidasi bahwa {@code namaBerkas} aman dipakai sebagai nama berkas tunggal (bukan path)
	 * di dalam direktori laporan bersama: tidak kosong, hanya berisi karakter dalam
	 * {@link #POLA_NAMA_BERKAS_AMAN}, tidak memuat {@code ".."}, dan berekstensi salah satu dari
	 * {@link #EKSTENSI_VIDEO_DIIZINKAN}.
	 *
	 * <p>Validasi ini adalah lapisan pertama pertahanan terhadap path traversal; lapisan kedua ada
	 * di {@link #doGet} yang menormalkan hasil gabungan direktori+nama berkas lalu memastikan hasil
	 * akhirnya tetap berada di bawah direktori laporan sebelum benar-benar menulis berkas.</p>
	 *
	 * @param namaBerkas nama berkas video mentah dari klien ({@code video-filename})
	 * @return {@code true} bila nama berkas lolos semua pemeriksaan di atas
	 */
	private static boolean isNamaBerkasVideoAman(String namaBerkas) {
		if (namaBerkas == null) {
			return false;
		}
		String nama = namaBerkas.trim();
		if (nama.isEmpty() || nama.contains("..") || !POLA_NAMA_BERKAS_AMAN.matcher(nama).matches()) {
			return false;
		}
		int titikTerakhir = nama.lastIndexOf('.');
		if (titikTerakhir <= 0 || titikTerakhir == nama.length() - 1) {
			return false;
		}
		String ekstensi = nama.substring(titikTerakhir + 1).toLowerCase(Locale.ROOT);
		return EKSTENSI_VIDEO_DIIZINKAN.contains(ekstensi);
	}

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
	 * <p><b>Keamanan — path traversal dan tulis berkas sembarang (DITAMBAL):</b> {@code fileNameVideo}
	 * berasal MENTAH dari klien dan endpoint ini tetap dapat dipanggil tanpa login (dipetakan di
	 * {@code web.xml} sebagai {@code /Recording} tanpa {@code security-constraint} khusus, sehingga
	 * tunduk pada aturan tangkapan-semua {@code IS_AUTHENTICATED_ANONYMOUSLY} — gerbang login
	 * SENGAJA tidak ditambahkan di sini karena halaman perekaman klien ({@code capture_video.jsp}
	 * dan variannya di bawah {@code WEB-INF/u/}) dipakai untuk alur absensi kiosk sebelum pengguna
	 * login; lihat juga {@code FilterJSP#isUtilityJsp}). Karena itu penulisan berkas sekarang
	 * dijaga dua lapis sebelum {@link java.nio.file.Files#copy}: (1) {@link
	 * #isNamaBerkasVideoAman(String)} menolak nama berkas yang mengandung karakter di luar
	 * {@code [A-Za-z0-9._-]} (termasuk {@code /}, {@code \}, dan {@code :}), mengandung
	 * {@code ".."}, atau berekstensi di luar {@link #EKSTENSI_VIDEO_DIIZINKAN}; (2) path hasil
	 * gabungan direktori laporan dan nama berkas dinormalkan dengan {@link
	 * java.nio.file.Path#normalize()} dan WAJIB tetap berada di bawah direktori laporan
	 * ({@link java.nio.file.Path#startsWith(Path)}) sebelum benar-benar ditulis — bila salah satu
	 * gerbang gagal, penulisan berkas dilewati dan percobaannya dicatat lewat {@link
	 * ais.common.ErrorAuditUtil#record}, namun respons akhir tetap {@code {"status":"OK"}} seperti
	 * semula agar tidak membocorkan detail penolakan ke pemanggil. {@code Access-Control-Allow-Origin: *}
	 * dipertahankan karena dipakai juga oleh klien non-browser/origin lain yang memanggil endpoint
	 * ini; risikonya sudah berkurang signifikan setelah penulisan berkas sembarang ditutup.</p>
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
				if (isNamaBerkasVideoAman(fileNameVideo)) {
					Path direktoriLaporan = Paths.get(Common.ambilREAL_PATH_REPORT()).toAbsolutePath().normalize();
					Path berkasTujuan = direktoriLaporan.resolve(fileNameVideo).normalize();
					if (berkasTujuan.startsWith(direktoriLaporan)) {
						System.out.println("outputfile video -> " + berkasTujuan.toString());
						java.nio.file.Files.copy(fileContent, berkasTujuan, StandardCopyOption.REPLACE_EXISTING);
					} else {
						ais.common.ErrorAuditUtil.record(
								new SecurityException("Recording: path hasil normalisasi keluar dari direktori laporan: " + fileNameVideo),
								"Recording.doGet: percobaan path traversal ditolak");
					}
				} else {
					ais.common.ErrorAuditUtil.record(
							new SecurityException("Recording: nama berkas video ditolak (karakter/ekstensi tidak diizinkan): " + fileNameVideo),
							"Recording.doGet: nama berkas video tidak valid");
				}

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
