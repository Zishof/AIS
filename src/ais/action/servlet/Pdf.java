package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Servlet implementation class Pdf
 */
public class Pdf extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public Pdf() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {
		String calmhs = request.getParameter("calmhs");
		ServletContext sc = getServletContext();

		// --- MODIFIKASI SHARED DIRECTORY ---
		// Ambil path report secara dinamis
		String reportPath = Common.ambilREAL_PATH_REPORT();
		// -----------------------------------

		if (calmhs != null) {
			try {
				Long idCalmhs = Long.parseLong(Common.desEncrypter.get().decrypt(calmhs));
				BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
						.ambil(BiodataCalonMahasiswa.class.getName(), idCalmhs);
						
				if (biodataCalonMahasiswa != null) {
					File fileBio = CommonReportHelper.onCetakBiodataCalonMahasiswa(biodataCalonMahasiswa, false);
					
					if (fileBio != null) {
						// Gunakan variabel reportPath yang dinamis
						File file = new File(reportPath + "/" + fileBio.getName());
						// Cek isFile() (bukan cuma exists()) supaya direktori tidak lolos ke streamFileToResponse
						if (file.isFile()) {
							streamFileToResponse(resp, sc, file, fileBio.getName());
						} else {
							resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Pdf.java:71");
				// Common.tampilErrorJikaAdmin(e);
			}

		} else {
			String filenameParam = request.getParameter("p");
			
			if (filenameParam == null || filenameParam.trim().isEmpty()) {
				if (sc != null) {
					sc.log("Parameter file (p) harus diisi!");
				}
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}

			try {
				String filename = Common.desEncrypter.get().decrypt(filenameParam);
				
				// Gunakan variabel reportPath yang dinamis
				File file = new File(reportPath + "/" + filename);
				
				System.out.println("Tampil file di -> " + file.getAbsolutePath() + ", Ada -> " + file.exists());

				// PENTING: pakai isFile(), bukan cuma exists() -- bila parameter path/nama-file kosong
				// atau tidak lengkap, path yang terbentuk bisa berhenti di sebuah DIREKTORI (mis. folder
				// "report" itu sendiri). exists() tetap true untuk direktori, sehingga FileInputStream
				// di streamFileToResponse akan melempar FileNotFoundException("... (Is a directory)").
				if (file.isFile()) {
					streamFileToResponse(resp, sc, file, filename);
				} else {
					// Jika file tidak ditemukan (atau ternyata sebuah direktori), kembalikan status 404
					// agar UI merespon dengan benar, bukan exception mentah.
					resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Pdf.java:102");
			}
		}
	}

	/**
	 * Private helper method untuk memproses I/O Stream secara terpusat.
	 * Menghilangkan duplikasi kode dan mengunci keamanan resource memory (Memory Leak Safe).
	 */
	private void streamFileToResponse(HttpServletResponse resp, ServletContext sc, File file, String displayFilename) throws IOException {
		FileInputStream in = null;
		OutputStream out = null;

		// Jaring pengaman terakhir: jangan pernah buka FileInputStream ke sebuah direktori --
		// java.io.FileInputStream constructor akan melempar FileNotFoundException mentah
		// ("... (Is a directory)") yang tidak informatif buat user. Validasi eksplisit dulu.
		if (!file.isFile()) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			resp.setContentType("text/plain");
			resp.getWriter().write("Berkas laporan tidak ditemukan");
			return;
		}

		try {
			// Menentukan tipe MIME, berikan fallback aman ke 'application/pdf' jika server tidak mengenali
			String mimeType = sc == null ? null : sc.getMimeType(displayFilename);
			if (mimeType == null) {
				mimeType = "application/pdf";
			}

			resp.setContentType(mimeType);
			resp.setHeader("Content-Disposition", "inline; filename=\"" + displayFilename + "\"");
			// Menggunakan header String untuk mencegah int overflow pada file berukuran besar
			resp.setHeader("Content-Length", String.valueOf(file.length()));

			in = new FileInputStream(file);
			out = resp.getOutputStream();

			// OPTIMASI: Buffer 8KB sangat efisien menyeimbangkan RAM dan kecepatan baca-tulis harddisk
			byte[] buf = new byte[8192];
			int count;
			
			while ((count = in.read(buf)) >= 0) {
				out.write(buf, 0, count);
			}
			
			// Pastikan sisa data di dalam buffer didorong secara utuh ke koneksi client
			out.flush();

		} finally {
			// WAJIB: Selalu tutup jalur I/O di dalam finally untuk membebaskan memory & file descriptor OS
			if (in != null) {
				try { in.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Pdf.java:144");}
			}
			if (out != null) {
				try { out.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Pdf.java:147");}
			}
		}
	}

}