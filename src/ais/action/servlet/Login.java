package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.SecurityFilter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sisdes.Penduduk;

/**
 * Servlet halaman login utama aplikasi — melayani dua hal sekaligus: penyajian halaman login yang
 * sesuai konfigurasi, dan endpoint otentikasi AJAX {@code action=ajax_login}.
 *
 * <h3>Dua peran</h3>
 * <ol>
 *   <li><b>Endpoint otentikasi AJAX</b> — dipilih ketika parameter {@code action} bernilai
 *       {@code ajax_login}. Mengembalikan JSON berisi {@code status}, {@code message}, dan
 *       {@code redirect}. Otentikasi sesungguhnya <b>tidak</b> dilakukan di kelas ini melainkan
 *       didelegasikan ke {@code SecurityFilter.doAutoLogin}; kelas ini berperan sebagai pembungkus
 *       yang menangkap hasilnya dan menerjemahkannya menjadi JSON yang rapi.</li>
 *   <li><b>Penyaji halaman</b> — memilih berkas JSP login menurut konfigurasi
 *       {@code default_login_versi_baru}, {@code default_login3_versi_baru},
 *       {@code default_login5_versi_baru}, lalu menurun ke rantai cadangan berbasis keberadaan
 *       berkas ({@code ecampus.jsp}, {@code login_ecampus.jsp}, {@code eschool.jsp},
 *       {@code login.jsp}, {@code index.jsp}). Parameter {@code p} juga dapat mengarahkan ke dua
 *       halaman modul kantin.</li>
 * </ol>
 *
 * <h3>Validasi kredensial</h3>
 * <p>Kelas ini hanya memeriksa bahwa nama pengguna dan kata sandi tidak kosong. Pencocokan kata
 * sandi, penentuan hak akses, dan pembentukan sesi seluruhnya milik
 * {@code SecurityFilter.doAutoLogin}. Yang dilakukan kelas ini sebelum itu adalah pemeriksaan
 * <b>status akun</b> lewat {@link #cekStatusAwalAjaxLogin(String, HttpServletRequest)}.</p>
 *
 * <h3>Catatan pola query — hubungan dengan perbaikan MatchMode.EXACT</h3>
 * <p>Seluruh pencarian akun di kelas ini memakai {@link org.hibernate.criterion.Restrictions#eq}
 * dengan pembandingan persis, <b>bukan</b> {@code Restrictions.ilike(..., MatchMode.EXACT)}.
 * Karena {@code ilike} menerjemahkan nilainya menjadi pola {@code LIKE}, karakter joker
 * {@code %} dan {@code _} pada nama pengguna akan ikut berlaku sebagai joker pada pola tersebut;
 * kelas ini tidak terkena persoalan itu karena tidak memakai {@code ilike} sama sekali. Perlu
 * dicatat bahwa {@code PembayaranUtil.getCalonMahasiswaByNoPendaftaran} masih memakai pola
 * {@code ilike} tersebut, tetapi jalur itu tidak dipanggil dari sini.</p>
 *
 * <h3>Pembatasan laju percobaan — keadaan yang berlaku sekarang</h3>
 * <p>Tidak ada pembatasan laju, penundaan bertahap, penguncian akun, maupun CAPTCHA pada endpoint
 * ini, dan tidak ada pula di {@code SecurityFilter}. {@link #isAjaxLoginReentry(HttpServletRequest)}
 * <b>bukan</b> pembatas laju: ia hanya mencegah pemanggilan berulang di dalam <i>satu</i>
 * permintaan yang sama (masuk kembali secara rekursif), dan tidak menghitung apa pun lintas
 * permintaan. Percobaan kata sandi karenanya dapat diulang tanpa batas dari luar. Keadaan ini
 * didokumentasikan apa adanya dan sudah tercatat sebagai kandidat perbaikan tersendiri.</p>
 *
 * <p>Perlu diketahui pula bahwa {@link #cekStatusAwalAjaxLogin(String, HttpServletRequest)}
 * berjalan <b>sebelum</b> kata sandi diperiksa dan menghasilkan pesan yang berbeda untuk akun
 * yang ada tetapi tidak aktif dibandingkan akun yang tidak ditemukan, sehingga membedakan kedua
 * keadaan itu dimungkinkan tanpa mengetahui kata sandi.</p>
 *
 * @see SecurityFilter
 * @see Main#checkAndSetUserSession(HttpServletRequest, boolean)
 */
public class Login extends HttpServlet {
	/** Versi serial standar {@link java.io.Serializable} untuk kontrak servlet. */
	private static final long serialVersionUID = 1L;

	/**
	 * Kunci atribut permintaan yang menandai bahwa proses login AJAX sedang berjalan pada
	 * permintaan ini. Dipakai {@link #isAjaxLoginReentry(HttpServletRequest)} untuk mengenali
	 * pemanggilan yang masuk kembali secara rekursif — misalnya ketika alur otentikasi meneruskan
	 * permintaan ke servlet ini lagi — agar tidak terjadi pengulangan tanpa akhir.
	 */
	private static final String ATTR_AJAX_LOGIN_RUNNING = "ais.login.ajax.running";

	/**
	 * Kunci atribut permintaan yang mencacah berapa kali pemanggilan masuk kembali terjadi dalam
	 * satu permintaan. Bersifat diagnostik; nilainya tidak memengaruhi keputusan apa pun.
	 */
	private static final String ATTR_AJAX_LOGIN_REENTRY_COUNT = "ais.login.ajax.reentry.count";

	/**
	 * Kunci atribut permintaan tempat menyimpan pesan galat terakhir, agar pemanggilan yang masuk
	 * kembali dapat mengambil alasan penolakan yang sudah ditentukan lebih dulu alih-alih
	 * menghasilkan pesan baru yang kurang tepat.
	 */
	private static final String ATTR_AJAX_LOGIN_LAST_MESSAGE = "ais.login.ajax.last.message";

	/**
	 * Konstruktor default yang dibutuhkan container servlet; tidak melakukan inisialisasi apa pun
	 * selain memanggil konstruktor {@link HttpServlet}.
	 */
	public Login() {
		super();
	}

	/**
	 * Menerima permintaan HTTP GET dan meneruskannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>Endpoint {@code ajax_login} juga dapat dicapai lewat GET, yang berarti kredensial dapat
	 * dikirim sebagai parameter query. Exception apa pun ditelan
	 * {@link Common#tampilErrorJikaAdmin(Exception)} sehingga kegagalan tidak memunculkan halaman
	 * error container ke pengguna yang belum login.</p>
	 *
	 * @param request  permintaan dari peramban
	 * @param response respons yang akan diisi halaman login atau JSON hasil otentikasi
	 * @throws ServletException bila container melaporkan kegagalan servlet
	 * @throws IOException      bila penulisan respons gagal
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
	 * Menerima permintaan HTTP POST — metode yang seharusnya dipakai formulir login — dan
	 * meneruskannya ke {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>Perilakunya identik dengan {@link #doGet(HttpServletRequest, HttpServletResponse)};
	 * kelas ini tidak membedakan metode HTTP.</p>
	 *
	 * @param request  permintaan dari peramban
	 * @param response respons yang akan diisi halaman login atau JSON hasil otentikasi
	 * @throws ServletException bila container melaporkan kegagalan servlet
	 * @throws IOException      bila penulisan respons gagal
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
	 * Menentukan apakah permintaan adalah upaya otentikasi AJAX atau permintaan halaman, lalu
	 * menjalankan salah satunya.
	 *
	 * <h4>Efek samping global di awal</h4>
	 * <p>Sebelum apa pun, method menulis lima field statis bersama pada {@link Common}:
	 * {@code REAL_PATH}, {@code REAL_PATH_REPORT_TEMP}, {@code ROOT},
	 * {@code CURRENT_URL_SIMPLE}, dan {@code CURRENT_URL}. Sama seperti pada
	 * {@link FilterJSP}, nilai itu adalah state proses dan bukan per-thread.</p>
	 *
	 * <h4>Cabang otentikasi AJAX</h4>
	 * <p>Dipilih ketika {@code action=ajax_login}. Urutannya: menolak nama pengguna atau kata
	 * sandi kosong; menolak pemanggilan yang masuk kembali
	 * ({@link #isAjaxLoginReentry(HttpServletRequest)}); memeriksa status akun lewat
	 * {@link #cekStatusAwalAjaxLogin(String, HttpServletRequest)}; membersihkan atribut galat
	 * sisa; lalu memanggil {@code SecurityFilter.doAutoLogin} dengan respons yang dibungkus
	 * {@link AjaxLoginResponseWrapper} agar pengalihan dan keluaran HTML yang dihasilkan alur
	 * otentikasi lama tertangkap alih-alih terkirim ke klien.</p>
	 * <p>Setelah otentikasi berhasil, sesi pengguna masih wajib terbentuk: dicoba
	 * {@code Main.checkAndSetUserSession}, lalu {@code SecurityFilter.getCurrentFromUsername}.
	 * Bila keduanya gagal, hasil diperlakukan sebagai kegagalan agar klien tidak dialihkan ke
	 * dasbor dengan sesi yang belum ada.</p>
	 *
	 * <h4>Cabang halaman</h4>
	 * <p>Parameter {@code p} bernilai {@code registrasi_calon_anggota} atau
	 * {@code halaman_calon_anggota} diteruskan ke halaman modul kantin. Selain itu berkas login
	 * dipilih menurut tiga konfigurasi versi baru secara berurutan, lalu menurun ke rantai
	 * cadangan yang memeriksa keberadaan berkas di cakram satu per satu. Seluruh nama berkas
	 * tujuan adalah <b>konstanta di dalam kode</b>; tidak ada bagian jalur yang berasal dari
	 * masukan pengguna.</p>
	 *
	 * @param request  permintaan yang sedang dilayani
	 * @param response respons yang akan diisi
	 * @throws Exception bila penerusan halaman atau proses otentikasi gagal
	 */
	@SuppressWarnings({ "deprecation" })
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Common.REAL_PATH = getServletContext().getRealPath("/");
		Common.REAL_PATH_REPORT_TEMP = getServletContext().getRealPath("/report");
		Common.ROOT = request.getContextPath();
		Common.CURRENT_URL_SIMPLE = (request.isSecure() ? "https://" : "http://") + request.getServerName()
				+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
						: ":" + request.getServerPort());
		Common.CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
				+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
				+ request.getContextPath();

        // =========================================================================================
		// AWAL: ENDPOINT AJAX LOGIN
		// =========================================================================================
		if ("ajax_login".equals(request.getParameter("action"))) {
			//response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
			response.setHeader("Pragma", "no-cache");

			JSONObject jsonResponse = new JSONObject();
			String username = request.getParameter("username");
			String password = request.getParameter("password");
			username = username == null ? "" : username.trim();
			password = password == null ? "" : password;

			if (username.length() == 0 || password.length() == 0) {
				jsonResponse.put("status", "error");
				jsonResponse.put("message",
						bahasaAman("Nama pengguna dan kata sandi tidak boleh kosong."));
				tulisJsonLogin(response, jsonResponse);
				return;
			}

			if (isAjaxLoginReentry(request)) {
				String pesan = ambilPesanErrorLoginAjax(request, null,
						"Nama pengguna atau kata sandi tidak valid. Silakan periksa kembali.");
				if (pesan == null || pesan.trim().length() == 0) {
					pesan = "Nama pengguna atau kata sandi tidak valid. Silakan periksa kembali.";
				}
				jsonResponse.put("status", "error");
				jsonResponse.put("message", bahasaAman(pesan));
				tulisJsonLogin(response, jsonResponse);
				return;
			}

			try {
				AjaxLoginPrecheck precheck = cekStatusAwalAjaxLogin(username, request);
				if (precheck != null && !precheck.valid) {
					jsonResponse.put("status", "error");
					jsonResponse.put("message", bahasaAman(precheck.message));
					tulisJsonLogin(response, jsonResponse);
					return;
				}

				String linkProfile = request.getParameter("linkProfile") == null ? "" : request.getParameter("linkProfile");
				AjaxLoginResponseWrapper ajaxResponse = new AjaxLoginResponseWrapper(response);
				bersihkanAtributErrorLogin(request);
				request.setAttribute("AJAX_LOGIN", "true");
				request.setAttribute("ajax_login", Boolean.TRUE);
				request.setAttribute(ATTR_AJAX_LOGIN_RUNNING, Boolean.TRUE);

				boolean sukses = false;
				try {
					sukses = SecurityFilter.doAutoLogin(username, password, false, linkProfile, request, ajaxResponse);
				} finally {
					try {
						request.removeAttribute(ATTR_AJAX_LOGIN_RUNNING);
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:133");
					}
				}

				if (sukses) {
					Tbmuser userSession = Main.checkAndSetUserSession(request, false);
					if (userSession == null) {
						userSession = SecurityFilter.getCurrentFromUsername(username, request, true);
					}
					if (userSession == null) {
						sukses = false;
						jsonResponse.put("status", "error");
						jsonResponse.put("message", bahasaAman(
								"Otentikasi berhasil, tetapi sesi pengguna gagal dibuat. Silakan coba lagi atau hubungi administrator."));
					}
				}

				if (sukses) {
					jsonResponse.put("status", "success");
					jsonResponse.put("message",
							bahasaAman("Otentikasi berhasil. Mengalihkan ke dasbor..."));
					jsonResponse.put("redirect", Common.ROOT + "/main");
				} else if (!jsonResponse.has("status")) {
					String pesan = ambilPesanDariJsonLogin(ajaxResponse == null ? null : ajaxResponse.getCapturedContent());
					if (pesan == null || pesan.trim().length() == 0) {
						pesan = ambilPesanErrorLoginAjax(request, ajaxResponse,
								"Nama pengguna atau kata sandi tidak valid. Silakan periksa kembali.");
					}
					request.setAttribute(ATTR_AJAX_LOGIN_LAST_MESSAGE, pesan);
					jsonResponse.put("status", "error");
					jsonResponse.put("message", bahasaAman(pesan));
				}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Login.java:167");
				jsonResponse.put("status", "error");
				jsonResponse.put("message", bahasaAman(
						"Terjadi kesalahan pada sistem. Silakan hubungi administrator."));
			}

			tulisJsonLogin(response, jsonResponse);
			return;
		}
        // =========================================================================================
		// AKHIR: ENDPOINT AJAX LOGIN
		// =========================================================================================

		if (request.getParameter("p") != null
				&& request.getParameter("p").equalsIgnoreCase("registrasi_calon_anggota")) {
			String dispatcherPath = "/WEB-INF/baru/modul/kantin/member/form_registrasi_calon.jsp";
			safeForward(request, response, dispatcherPath);
			return;
		} else if (request.getParameter("p") != null
				&& request.getParameter("p").equalsIgnoreCase("halaman_calon_anggota")) {

			String dispatcherPath = "/WEB-INF/baru/modul/kantin/index.jsp";
			safeForward(request, response, dispatcherPath);
			return;
		} else {

			Konfigurasi config = Common.getKonfigurasi("default_login_versi_baru", Konfigurasi.TIDAK_AKTIF);
			if (isAktif(config)) {
				safeForward(request, response, "/WEB-INF/baru/login2.jsp");
				return;
			}

			Konfigurasi config3 = Common.getKonfigurasi("default_login3_versi_baru", Konfigurasi.TIDAK_AKTIF);
			if (isAktif(config3)) {
				safeForward(request, response, "/WEB-INF/baru/login3.jsp");
				return;
			}

			Konfigurasi config5 = Common.getKonfigurasi("default_login5_versi_baru", Konfigurasi.TIDAK_AKTIF);
			if (isAktif(config5)) {
				safeForward(request, response, "/WEB-INF/baru/login5.jsp");
				return;
			}

			String fileDiMedia = request.getRealPath("/WEB-INF/j/ecampus.jsp");

			boolean exist = fileExists(fileDiMedia);

			if (exist) {
				safeForward(request, response, "/WEB-INF/j/ecampus.jsp");
			} else {
				fileDiMedia = request.getRealPath("/WEB-INF/j/login_ecampus.jsp");
				exist = fileExists(fileDiMedia);
				if (exist) {
					safeForward(request, response, "/WEB-INF/j/login_ecampus.jsp");
				} else {
					fileDiMedia = request.getRealPath("/WEB-INF/j/eschool.jsp");
					exist = fileExists(fileDiMedia);
					if (exist) {
						safeForward(request, response, "/WEB-INF/j/eschool.jsp");
					} else {
						fileDiMedia = request.getRealPath("/WEB-INF/j/login.jsp");
						exist = fileExists(fileDiMedia);
						if (exist) {
							safeForward(request, response, "/WEB-INF/j/login.jsp");
						} else {
							safeForward(request, response, "/WEB-INF/j/index.jsp");
						}
					}
				}
			}
		}
	}

	/**
	 * Meneruskan permintaan ke berkas JSP tujuan, hanya bila respons masih dapat diubah.
	 *
	 * @param request  permintaan yang sedang dilayani
	 * @param response respons tujuan; boleh {@code null}
	 * @param path     jalur berkas tujuan, selalu konstanta di dalam kode
	 * @return {@code true} bila penerusan benar-benar dilakukan
	 * @throws ServletException bila penerusan gagal
	 * @throws IOException      bila operasi masukan/keluaran gagal
	 */
	private static boolean safeForward(HttpServletRequest request, HttpServletResponse response, String path)
			throws ServletException, IOException {
		if (response == null || response.isCommitted()) {
			return false;
		}
		request.getRequestDispatcher(path).forward(request, response);
		return true;
	}

	/**
	 * Mengenali pemanggilan endpoint login AJAX yang masuk kembali di dalam permintaan yang sama.
	 *
	 * <p>Keadaan itu terjadi ketika alur otentikasi lama meneruskan permintaan kembali ke servlet
	 * ini; tanpa penjagaan, prosesnya akan berulang tanpa akhir. Penanda yang dipakai adalah
	 * atribut {@link #ATTR_AJAX_LOGIN_RUNNING}, dan setiap kejadian dicacah ke
	 * {@link #ATTR_AJAX_LOGIN_REENTRY_COUNT} untuk keperluan diagnostik.</p>
	 *
	 * <p><b>Bukan pembatas laju.</b> Seluruh penanda disimpan sebagai atribut <i>permintaan</i>
	 * yang umurnya habis begitu permintaan selesai, sehingga method ini tidak mengetahui apa pun
	 * tentang percobaan login sebelumnya dan tidak dapat memperlambat maupun menghentikan
	 * percobaan berulang dari luar.</p>
	 *
	 * @param request permintaan yang sedang dilayani; boleh {@code null}
	 * @return {@code true} bila pemanggilan ini adalah masuk kembali
	 */
	private static boolean isAjaxLoginReentry(HttpServletRequest request) {
		if (request == null) {
			return false;
		}
		try {
			if (Boolean.TRUE.equals(request.getAttribute(ATTR_AJAX_LOGIN_RUNNING))) {
				Object countObj = request.getAttribute(ATTR_AJAX_LOGIN_REENTRY_COUNT);
				int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
				request.setAttribute(ATTR_AJAX_LOGIN_REENTRY_COUNT, Integer.valueOf(count + 1));
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:261");
		}
		return false;
	}

	private static String ambilPesanDariJsonLogin(String content) {
		if (content == null) {
			return null;
		}
		String text = content.trim();
		if (text.length() == 0 || !text.startsWith("{")) {
			return null;
		}
		try {
			JSONObject json = new JSONObject(text);
			if (json.has("message")) {
				String message = String.valueOf(json.get("message"));
				return bersihkanPesanLogin(message);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:280");
		}
		return null;
	}

	private static void tulisJsonLogin(HttpServletResponse response, JSONObject jsonResponse) throws IOException {
		if (!response.isCommitted()) {
			try {
				response.resetBuffer();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:289");
				// Abaikan. Beberapa container bisa menolak resetBuffer jika response sudah tersentuh.
			}
			//response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=UTF-8");
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
			response.setHeader("Pragma", "no-cache");
		}
		PrintWriter writer = response.getWriter();
		writer.print(jsonResponse.toString());
		writer.flush();
	}

	@SuppressWarnings({ })
	private static AjaxLoginPrecheck cekStatusAwalAjaxLogin(String username, HttpServletRequest request) {
		Session dbSession = null;
		try {
			PerguruanTinggi perguruanTinggi = null;
			try {
				perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:309");
				// Jika konteks perguruan tinggi tidak tersedia, validasi status tetap dilanjutkan.
			}

			// Sesi DEDIKASI dan dibuka SETELAH getPerguruanTinggi. Sebelumnya memakai
			// currentNativeSession() (sesi native BERSAMA) yang ditutup oleh getPerguruanTinggi,
			// sehingga createCriteria di bawah gagal "Session is closed!". Sesi ini ditutup di finally.
			dbSession = HibernateUtil.getSessionFactory().openSession();

			Mahasiswa mahasiswa = null;
			Criteria criteriaMahasiswa = dbSession.createCriteria(Mahasiswa.class);
			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				criteriaMahasiswa.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
						.add(Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi));
			}
			mahasiswa = (Mahasiswa) ConstantValues.simpleObject(criteriaMahasiswa.add(Restrictions.or(
					Restrictions.and(Restrictions.eq("userOrtu", username.trim()),
							Restrictions.and(Restrictions.isNotNull("userOrtu"),
									Restrictions.sqlRestriction("trim(this_.user_ortu) != ''"))),
					Restrictions.eq("nim", username.trim()))).setMaxResults(1), Mahasiswa.class);
			if (mahasiswa != null) {
				if (Boolean.FALSE.equals(mahasiswa.getAktif())) {
					return AjaxLoginPrecheck.error(
							"Akun Anda tidak aktif : Harap menghubungi bagian akademik untuk informasi lebih lanjut.");
				}
				return AjaxLoginPrecheck.ok();
			}

			Siswa siswa = (Siswa) ConstantValues.simpleObject(
					dbSession.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
							.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
							.add(Restrictions.or(Restrictions.eq("nomorInduk", username.trim()),
									Restrictions.or(Restrictions.and(Restrictions.eq("userOrtu", username.trim()),
											Restrictions.and(Restrictions.isNotNull("userOrtu"),
													Restrictions.sqlRestriction("trim(this_.user_ortu) != ''"))),
											Restrictions.eq("nomorIndukNasional", username.trim()))))
							.setMaxResults(1),
					Siswa.class);
			if (siswa != null) {
				if (Boolean.FALSE.equals(siswa.getAktif())) {
					return AjaxLoginPrecheck.error(
							"Akun Anda tidak aktif : Harap menghubungi bagian akademik untuk informasi lebih lanjut.");
				}
				return AjaxLoginPrecheck.ok();
			}

			Penduduk penduduk = (Penduduk) ConstantValues.simpleObject(
					dbSession.createCriteria(Penduduk.class).add(Restrictions.isNotNull("nama"))
							.add(Restrictions.ne("nama", "")).add(Restrictions.eq("kode", username.trim()))
							.setMaxResults(1),
					Penduduk.class);
			if (penduduk != null) {
				if (Boolean.FALSE.equals(penduduk.getAktif())) {
					return AjaxLoginPrecheck
							.error("Akun Anda tidak aktif : Harap menghubungi kantor desa untuk informasi lebih lanjut.");
				}
				return AjaxLoginPrecheck.ok();
			}

			Criteria criteriaUser = dbSession.createCriteria(Tbmuser.class);
			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				criteriaUser.createAlias("fakultas", "fakultas", Criteria.LEFT_JOIN)
						.add(Restrictions.or(Restrictions.isNull("fakultas.perguruanTinggi"),
								Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi)));
			}
			Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(
					criteriaUser.add(Restrictions.eq("userId", username)).setMaxResults(1), Tbmuser.class);
			if (tbmuser != null) {
				if (Boolean.FALSE.equals(tbmuser.getAktif())) {
					return AjaxLoginPrecheck.error(
							"Akun Anda tidak aktif : Harap menghubungi bagian akademik untuk informasi lebih lanjut.");
				}
				try {
					if (tbmuser.hakAkses() != null && Boolean.FALSE.equals(tbmuser.hakAkses().getAktif())) {
						return AjaxLoginPrecheck.error(
								"Hak akses akun Anda tidak aktif. Harap menghubungi administrator.");
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:386");
					// Validasi hak akses utama tetap dilakukan oleh SecurityFilter.doAutoLogin.
				}
				return AjaxLoginPrecheck.ok();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Login.java:392");
		} finally {
			try {
				if (dbSession != null && dbSession.isOpen()) {
					try {
						dbSession.clear();
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:398");
					}
					try {
						dbSession.disconnect();
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:402");
					}
					dbSession.close();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Login.java:407");
			}
		}
		return AjaxLoginPrecheck.ok();
	}

	private static void bersihkanAtributErrorLogin(HttpServletRequest request) {
		String[] keys = new String[] { "login_error", "loginError", "LOGIN_ERROR", "error", "errorMessage",
				"message", "SPRING_SECURITY_LAST_EXCEPTION", "AUTHENTICATION_EXCEPTION",
				"org.springframework.security.web.WebAttributes.AUTHENTICATION_EXCEPTION" };
		for (int i = 0; i < keys.length; i++) {
			try {
				request.removeAttribute(keys[i]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:420");
			}
		}
		try {
			HttpSession session = request.getSession(false);
			if (session != null) {
				for (int i = 0; i < keys.length; i++) {
					try {
						session.removeAttribute(keys[i]);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:429");
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:433");
		}
	}

	private static String ambilPesanErrorLoginAjax(HttpServletRequest request, AjaxLoginResponseWrapper ajaxResponse,
			String defaultMessage) {
		String[] keys = new String[] { "login_error", "loginError", "LOGIN_ERROR", "error", "errorMessage",
				"message", "javax.servlet.error.message", "SPRING_SECURITY_LAST_EXCEPTION", "AUTHENTICATION_EXCEPTION",
				"org.springframework.security.web.WebAttributes.AUTHENTICATION_EXCEPTION" };

		for (int i = 0; i < keys.length; i++) {
			String pesan = stringDariObjek(request.getAttribute(keys[i]));
			pesan = bersihkanPesanLogin(pesan);
			if (pesan != null && pesan.length() > 0) {
				return pesan;
			}
		}

		try {
			HttpSession session = request.getSession(false);
			if (session != null) {
				for (int i = 0; i < keys.length; i++) {
					String pesan = stringDariObjek(session.getAttribute(keys[i]));
					pesan = bersihkanPesanLogin(pesan);
					if (pesan != null && pesan.length() > 0) {
						return pesan;
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:462");
		}

		String redirect = ajaxResponse == null ? null : ajaxResponse.getRedirectLocation();
		String pesanRedirect = ambilParameterDariUrl(redirect, "login_error");
		if (pesanRedirect == null) {
			pesanRedirect = ambilParameterDariUrl(redirect, "error");
		}
		if (pesanRedirect == null) {
			pesanRedirect = ambilParameterDariUrl(redirect, "message");
		}
		pesanRedirect = bersihkanPesanLogin(pesanRedirect);
		if (pesanRedirect != null && pesanRedirect.length() > 0) {
			return pesanRedirect;
		}

		String content = ajaxResponse == null ? null : ajaxResponse.getCapturedContent();
		String pesanContent = ambilPesanPentingDariHtml(content);
		if (pesanContent != null && pesanContent.length() > 0) {
			return pesanContent;
		}

		if (ajaxResponse != null && ajaxResponse.getErrorMessage() != null
				&& ajaxResponse.getErrorMessage().trim().length() > 0) {
			return ajaxResponse.getErrorMessage().trim();
		}
		return defaultMessage;
	}

	private static String stringDariObjek(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof Throwable) {
			return ((Throwable) obj).getMessage();
		}
		return String.valueOf(obj);
	}

	private static String ambilParameterDariUrl(String url, String parameter) {
		if (url == null || parameter == null || url.indexOf('?') < 0) {
			return null;
		}
		try {
			String query = url.substring(url.indexOf('?') + 1);
			String[] pairs = query.split("&");
			for (int i = 0; i < pairs.length; i++) {
				String pair = pairs[i];
				int eq = pair.indexOf('=');
				String key = eq >= 0 ? pair.substring(0, eq) : pair;
				if (parameter.equals(key)) {
					String value = eq >= 0 ? pair.substring(eq + 1) : "";
					return URLDecoder.decode(value, "UTF-8");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:517");
		}
		return null;
	}

	private static String ambilPesanPentingDariHtml(String html) {
		String text = bersihkanPesanLogin(html);
		if (text == null || text.length() == 0) {
			return null;
		}
		String lower = text.toLowerCase();
		String[] keywords = new String[] { "akun anda tidak aktif", "akun anda tidak bisa login",
				"pengguna tidak ditemukan", "tidak diizinkan", "satuan kerja", "diblokir", "kuota",
				"kata sandi", "password", "gagal login" };
		for (int i = 0; i < keywords.length; i++) {
			int idx = lower.indexOf(keywords[i]);
			if (idx >= 0) {
				int end = text.indexOf('.', idx);
				if (end < 0 || end - idx > 260) {
					end = Math.min(text.length(), idx + 260);
				}
				return text.substring(idx, end).trim();
			}
		}
		return null;
	}

	private static String bersihkanPesanLogin(String pesan) {
		if (pesan == null) {
			return null;
		}
		String text = pesan.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
				.replaceAll("(?is)<style[^>]*>.*?</style>", " ").replaceAll("(?is)<[^>]+>", " ")
				.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
				.replace("&#39;", "'").replace("&quot;", "\"");
		text = text.replaceAll("\\s+", " ").trim();
		if (text.length() > 320) {
			text = text.substring(0, 320).trim();
		}
		return text;
	}

	private static boolean fileExists(String filePath) {
		if (filePath == null || filePath.trim().length() == 0) {
			return false;
		}
		try {
			return new java.io.File(filePath).exists();
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean isAktif(Konfigurasi config) {
		return config != null && config.getNilai() != null && Konfigurasi.AKTIF.equalsIgnoreCase(config.getNilai().trim());
	}

	private static String bahasaAman(String pesanDefault) {
		if (pesanDefault == null) {
			return "";
		}
		try {
			String hasil = Common.getBahasaConfig(pesanDefault);
			if (hasil != null && hasil.trim().length() > 0) {
				return hasil;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Login.java:584");
		}
		return pesanDefault;
	}

	/**
	 * Tipe implementasi bersarang {@link AjaxLoginPrecheck} milik {@link Login}. Kelas ini memberi nama pada state
	 * atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link Login}. Dependensi
	 * yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini merupakan
	 * detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean valid}, {@code String
	 * message}; operasi lokal: {@code ok()}, {@code error}(). Aturan bisnis bersama tetap berada pada kelas induk
	 * atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see Login
	 */
	private static class AjaxLoginPrecheck {
		boolean valid;
		String message;

		static AjaxLoginPrecheck ok() {
			AjaxLoginPrecheck r = new AjaxLoginPrecheck();
			r.valid = true;
			return r;
		}

		static AjaxLoginPrecheck error(String message) {
			AjaxLoginPrecheck r = new AjaxLoginPrecheck();
			r.valid = false;
			r.message = message;
			return r;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link AjaxLoginResponseWrapper} milik {@link Login}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link Login}. Dependensi
	 * yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini merupakan
	 * detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code StringWriter capture}, {@code
	 * PrintWriter writer}, {@code String redirectLocation}, {@code int errorCode}, {@code String errorMessage};
	 * operasi lokal: {@code getWriter()}, {@code sendRedirect()}, {@code sendError()}, {@code sendError()}, {@code
	 * getRedirectLocation()}, {@code getErrorMessage()}, {@code getErrorCode()}, {@code getCapturedContent}().
	 * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see Login
	 */
	private static class AjaxLoginResponseWrapper extends HttpServletResponseWrapper {
		private final StringWriter capture = new StringWriter();
		private final PrintWriter writer = new PrintWriter(capture);
		private String redirectLocation;
		private int errorCode;
		private String errorMessage;

		AjaxLoginResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		public PrintWriter getWriter() throws IOException {
			return writer;
		}

		public void sendRedirect(String location) throws IOException {
			this.redirectLocation = location;
		}

		public void sendError(int sc) throws IOException {
			this.errorCode = sc;
		}

		public void sendError(int sc, String msg) throws IOException {
			this.errorCode = sc;
			this.errorMessage = msg;
		}

		String getRedirectLocation() {
			return redirectLocation;
		}

		String getErrorMessage() {
			return errorMessage;
		}

		@SuppressWarnings("unused")
		int getErrorCode() {
			return errorCode;
		}

		String getCapturedContent() {
			try {
				writer.flush();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Login.java:651");
			}
			return capture.toString();
		}
	}

}
