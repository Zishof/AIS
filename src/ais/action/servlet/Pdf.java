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
 * Servlet penyaji berkas PDF (dan gambar apa pun yang MIME type-nya dikenali servlet
 * container) dari direktori laporan bersama ({@link Common#ambilREAL_PATH_REPORT()}), dengan
 * nama berkas yang selalu diterima dalam bentuk terenkripsi DES lewat
 * {@link Common#desEncrypter}, tidak pernah sebagai teks biasa dari klien.
 *
 * <p>Dua mode: (1) parameter {@code calmhs} berisi id {@link BiodataCalonMahasiswa}
 * terenkripsi — servlet men-dekripsi id tersebut, memuat entity-nya, dan bila belum ada
 * berkas cetak biodata, membangkitkannya on-demand lewat
 * {@link CommonReportHelper#onCetakBiodataCalonMahasiswa}; (2) parameter {@code p} berisi
 * NAMA BERKAS (bukan id) yang juga terenkripsi DES — dipakai untuk menyajikan berkas laporan
 * yang sudah pernah dibuat sebelumnya (mis. oleh servlet lain di paket ini) tanpa perlu
 * membangun ulang.</p>
 *
 * <p><b>Keamanan — path/nama berkas TIDAK datang mentah dari klien, tetapi kunci
 * dekripsinya bersifat publik:</b> nilai plaintext {@code filename} pada mode (2) tidak bisa
 * dipilih langsung oleh klien karena harus melalui {@link Common#desEncrypter} terlebih
 * dahulu — namun {@code Common.DES_PASS_PHRASE} adalah konstanta TETAP ({@code "AIS_UIN"})
 * yang tertanam di source dan SAMA untuk seluruh instalasi AIS (lihat javadoc lengkap pada
 * {@link Common#DES_PASS_PHRASE} mengenai riwayat upaya migrasi ke AES-256-GCM yang dibatalkan
 * karena mematahkan alur login) — sehingga siapa pun yang mengetahui konstanta tersebut dapat
 * meng-enkripsi plaintext pilihannya sendiri (termasuk barisan {@code ../}) dan mengirimkannya
 * sebagai parameter {@code p} (atau {@code calmhs}) yang valid secara kriptografis. Risiko
 * root pada kunci DES yang dipakai bersama ini sudah didokumentasikan secara luas di
 * {@link Common#DES_PASS_PHRASE} dan TIDAK ditambal di sini (upaya migrasinya sudah
 * dibatalkan demi kompatibilitas login). Sebagai mitigasi lokal yang murah dan berdiri
 * sendiri, hasil {@code new File(reportPath + "/" + nama)} pada KEDUA mode divalidasi lewat
 * {@link #isDalamReportPath(File, String)} sebelum dialirkan ke respons: path kanonik
 * ({@link File#getCanonicalPath()}, bukan perbandingan string mentah) harus tetap berada di
 * dalam {@code reportPath} — bila gagal (termasuk percobaan traversal {@code ../}), servlet
 * membalas {@code 404} generik tanpa membocorkan alasan spesifiknya ke klien.</p>
 *
 * @see HttpServlet
 */
public class Pdf extends HttpServlet {
	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini. */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor default tanpa argumen, hanya meneruskan ke {@link HttpServlet#HttpServlet()}.
	 * Tidak ada state khusus yang diinisialisasi di sini.
	 */
	public Pdf() {
		super();
	}

	/**
	 * Menangani GET dengan mendelegasikan ke {@link #process}; kegagalan apa pun ditelan dan
	 * hanya ditampilkan ke pengguna bila konteks saat ini adalah administrator, lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param request  request HTTP masuk; parameter {@code calmhs} atau {@code p} (keduanya
	 *                 terenkripsi DES) menentukan berkas yang disajikan, lihat {@link #process}
	 * @param response response HTTP keluar; diisi berkas PDF/gambar oleh {@link #process}
	 * @throws ServletException tidak pernah dilempar keluar karena {@link #process} dibungkus
	 *                          try/catch di sini; dipertahankan hanya karena tanda tangan
	 *                          {@link HttpServlet#doGet}
	 * @throws IOException      idem, ditelan oleh blok catch
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
	 * Menangani POST dengan perilaku identik seperti {@link #doGet}: mendelegasikan ke
	 * {@link #process} dan menelan kegagalan lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param request  request HTTP masuk; parameter sama seperti pada {@link #doGet}
	 * @param response response HTTP keluar; diisi berkas PDF/gambar oleh {@link #process}
	 * @throws ServletException tidak pernah dilempar keluar, lihat catatan pada {@link #doGet}
	 * @throws IOException      idem
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
	 * Menentukan mode penyajian berkas berdasarkan parameter yang hadir, lalu mengalirkan
	 * berkas terkait ke {@code response} lewat {@link #streamFileToResponse}.
	 *
	 * <p>Mode {@code calmhs}: dekripsi id {@link BiodataCalonMahasiswa} lewat
	 * {@link Common#desEncrypter}, muat entity-nya lewat {@link ConstantValues#ambil}, dan
	 * bila ditemukan, bangkitkan (atau ambil bila sudah ada) berkas cetaknya lewat
	 * {@link CommonReportHelper#onCetakBiodataCalonMahasiswa}. Nama berkas hasil generate
	 * digabung dengan {@code reportPath} yang dikembalikan
	 * {@link Common#ambilREAL_PATH_REPORT()} untuk membentuk path final; hanya disajikan bila
	 * {@link File#isFile()} DAN {@link #isDalamReportPath(File, String)} benar, selain itu
	 * dibalas {@code 404}. Exception apa pun pada mode ini ditelan dan hanya dicatat ke
	 * {@link ais.common.ErrorAuditUtil}.</p>
	 *
	 * <p>Mode {@code p} (dipakai bila {@code calmhs} tidak ada): tolak dengan {@code 500} bila
	 * parameter kosong; selain itu dekripsi nama berkas lewat {@link Common#desEncrypter} dan
	 * gabungkan dengan {@code reportPath} yang sama, lalu sajikan bila {@link File#isFile()}
	 * DAN {@link #isDalamReportPath(File, String)} benar, selain itu {@code 404}. Lihat
	 * catatan keamanan pada Javadoc kelas mengenai validasi containment path kanonik ini,
	 * yang menjadi satu-satunya penghalang percobaan traversal {@code ../} setelah dekripsi
	 * DES (kunci mana bersifat publik/dikenal luas).</p>
	 *
	 * @param request request HTTP masuk; parameter {@code calmhs} (id biodata terenkripsi)
	 *                atau {@code p} (nama berkas terenkripsi) menentukan berkas yang disajikan
	 * @param resp    response HTTP keluar; diisi konten berkas oleh {@link #streamFileToResponse}
	 *                atau status {@code 404}/{@code 500} bila gagal
	 * @throws Exception dilempar hanya dari mode {@code p} sebelum dekripsi (mis. bila
	 *                    {@code sc} bermasalah); kegagalan dekripsi/pemuatan pada mode
	 *                    {@code calmhs} maupun kegagalan setelah dekripsi pada mode {@code p}
	 *                    ditelan di dalam masing-masing blok try/catch internal
	 */
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
						// Cek isFile() (bukan cuma exists()) supaya direktori tidak lolos ke streamFileToResponse,
						// dan cek containment kanonik (lihat isDalamReportPath) sebagai defense-in-depth.
						if (file.isFile() && isDalamReportPath(file, reportPath)) {
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
				if (file.isFile() && isDalamReportPath(file, reportPath)) {
					streamFileToResponse(resp, sc, file, filename);
				} else {
					// Jika file tidak ditemukan (atau ternyata sebuah direktori, atau lolos dekripsi DES
					// namun gagal validasi containment path -- lihat catatan keamanan pada Javadoc
					// kelas), kembalikan status 404 generik -- JANGAN bocorkan alasan spesifik ke klien.
					resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Pdf.java:102");
			}
		}
	}

	/**
	 * Memvalidasi bahwa {@code file} secara kanonik tetap berada di dalam {@code reportPath},
	 * menutup celah path traversal (mis. {@code ../}) yang bisa lolos dari validasi dekripsi
	 * DES semata -- lihat catatan keamanan pada Javadoc kelas ini mengenai
	 * {@code Common.DES_PASS_PHRASE} yang bersifat publik/dikenal luas. Perbandingan
	 * dilakukan atas {@link File#getCanonicalPath()} (bukan perbandingan string mentah atas
	 * hasil {@code reportPath + "/" + nama}) sehingga penyusun jalur {@code ".."} maupun
	 * pranala simbolik/junction (relevan di lingkungan Windows produksi ini) ternormalkan
	 * lebih dahulu; pola yang sama dipakai {@code AmbilLampiran.isDalamDirektoriDiizinkan}.
	 *
	 * <p>Sifatnya <i>fail-closed</i>: kegagalan apa pun saat menormalkan jalur (mis.
	 * {@link IOException} dari filesystem) menghasilkan {@code false}, dicatat ke
	 * {@link ais.common.ErrorAuditUtil}.</p>
	 *
	 * @param file       berkas hasil resolusi {@code reportPath + "/" + namaBerkas} yang
	 *                   hendak divalidasi, SEBELUM dialirkan ke respons
	 * @param reportPath direktori laporan yang diizinkan, dari
	 *                   {@link Common#ambilREAL_PATH_REPORT()}
	 * @return {@code true} bila path kanonik {@code file} sama dengan atau berada tepat di
	 *         bawah path kanonik {@code reportPath}
	 */
	private static boolean isDalamReportPath(File file, String reportPath) {
		try {
			String canonicalFile = file.getCanonicalFile().getCanonicalPath();
			String canonicalDir = new File(reportPath).getCanonicalFile().getCanonicalPath();
			return canonicalFile.equals(canonicalDir) || canonicalFile.startsWith(canonicalDir + File.separator);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Pdf.java:isDalamReportPath");
			return false;
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