package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.RequestContext;
import ais.common.ResponseContext;
import ais.common.SecurityFilter;
import ais.database.model.OnlineUsers;
import ais.database.model.Tbmuser;

/**
 * Filter global aplikasi yang dipetakan ke {@code /*} — pengarah URL ramah ke berkas JSP/ZUL di
 * bawah {@code /WEB-INF/}, pemasang header CORS, dan tempat penutupan terpusat session Hibernate
 * per permintaan.
 *
 * <h3>Kedudukan dalam rantai filter</h3>
 * <p>Menurut {@code web.xml}, filter ini dideklarasikan <b>sesudah</b>
 * {@code springSecurityFilterChain}, sehingga otorisasi Spring Security sudah berjalan lebih dulu.
 * Filter ini karena itu bukan gerbang otentikasi dan tidak boleh diperlakukan sebagai gerbang
 * otentikasi; perannya adalah pengarahan (routing) dan pembersihan sumber daya.</p>
 *
 * <h3>Empat tanggung jawab</h3>
 * <ol>
 *   <li><b>Konteks per-thread</b> — memasang {@link RequestContext} dan {@link ResponseContext}
 *       di awal, lalu melepasnya di {@code finally}, sehingga kode di lapisan bawah dapat meraih
 *       permintaan yang sedang berjalan tanpa meneruskannya sebagai argumen.</li>
 *   <li><b>Header CORS</b> — lihat {@link #addCorsHeader(HttpServletResponse)}; berlaku untuk
 *       SELURUH aplikasi karena pemetaan {@code /*}.</li>
 *   <li><b>Pengarahan URL</b> — {@link #handleRouting} menerjemahkan URL ramah menjadi
 *       {@code forward} ke berkas di bawah {@code /WEB-INF/} yang tidak dapat diakses langsung,
 *       atau menjadi {@code sendRedirect} ke rute kanonik.</li>
 *   <li><b>Penutupan terpusat session Hibernate</b> — lihat catatan di
 *       {@link #doFilter(ServletRequest, ServletResponse, FilterChain)}.</li>
 * </ol>
 *
 * <h3>Yang BUKAN tanggung jawab kelas ini</h3>
 * <p>Perlu ditegaskan karena mudah keliru: penyaring parameter {@code hanya_tampil_jsp} —
 * yaitu daftar putih nama berkas layanan JSP yang boleh dirender — <b>tidak berada di kelas
 * ini</b>. Penyaring itu dimiliki masing-masing servlet halaman yang membaca parameter tersebut,
 * yakni {@code Ppdb}, {@code Pmb}, {@code Karir}, {@code Tamu}, {@code Welpus}, {@code Welsis},
 * dan {@code Anjungan}. {@link FilterJSP} tidak pernah membaca parameter {@code hanya_tampil_jsp},
 * {@code p}, maupun {@code s}.</p>
 *
 * <h3>Penyaring jalur yang perlu dipahami</h3>
 * <p>{@link #isIgnoredPath(String)} memutuskan apakah sebuah permintaan diproses pengarah atau
 * diteruskan begitu saja ke rantai berikutnya. Keputusan itu memakai pencocokan <b>substring</b>
 * yang sangat longgar pada beberapa entrinya — antara lain {@code "al"}, {@code "pdf"}, dan
 * {@code "lampiran"} — sehingga banyak URL yang tidak berkaitan ikut melewati pengarah. Lihat
 * uraian pada method tersebut.</p>
 *
 * @see SecurityFilter
 * @see RequestContext
 * @see ResponseContext
 */
public class FilterJSP implements Filter {

	@Override
	/**
	 * Dipanggil sekali oleh container saat filter dimuat.
	 *
	 * <p>Hanya membaca parameter inisialisasi {@code init-param} dari {@code web.xml} lalu
	 * mencetaknya sebagai penanda bahwa filter benar-benar terpasang. Tidak ada state yang
	 * dibentuk, sehingga filter ini tanpa-state dan aman dipakai banyak thread sekaligus.</p>
	 *
	 * @param confg konfigurasi filter dari container
	 * @throws ServletException bila inisialisasi gagal
	 */
	public void init(FilterConfig confg) throws ServletException {
		String initParam = confg.getInitParameter("init-param");
		System.out.println("FilterJSP Initialized. Param: " + initParam);
	}

	@Override
	/**
	 * Dipanggil sekali saat filter dilepas container.
	 *
	 * <p>Tidak ada sumber daya tingkat filter yang perlu dilepas: seluruh pembersihan bersifat
	 * per-permintaan dan sudah dilakukan di blok {@code finally} milik
	 * {@link #doFilter(ServletRequest, ServletResponse, FilterChain)}.</p>
	 */
	public void destroy() {
		// Cleanup resources if needed
	}

	/**
	 * Titik masuk utama filter: memasang konteks per-thread dan header CORS, mengarahkan
	 * permintaan, lalu membersihkan seluruh state per-thread di blok {@code finally}.
	 *
	 * <h4>Urutan kerja</h4>
	 * <ol>
	 *   <li>Memasang {@link RequestContext} dan {@link ResponseContext}, lalu memanggil
	 *       {@link #addCorsHeader(HttpServletResponse)};</li>
	 *   <li>permintaan yang jalurnya memuat {@code /resources/} langsung diteruskan ke rantai
	 *       berikutnya sebagai jalan pintas untuk aset statis;</li>
	 *   <li>jalur permintaan dipotong dari context path — dengan penjagaan agar pemotongan tidak
	 *       melampaui panjang teks — lalu dihuruf-kecilkan sekali saja dan dipakai ulang demi
	 *       menghemat alokasi;</li>
	 *   <li>bila {@link #isIgnoredPath(String)} bernilai {@code true}, permintaan diserahkan ke
	 *       {@link #handleRouting}; bila tidak, permintaan diteruskan apa adanya ke rantai
	 *       berikutnya.</li>
	 * </ol>
	 *
	 * <h4>Dua exception yang sengaja diredam</h4>
	 * <p>{@link IllegalStateException} dan {@code DesktopUnavailableException} adalah gejala balapan
	 * yang <b>normal</b>, bukan bug: permintaan AU/ZK yang masih berjalan berbenturan dengan
	 * pembatalan sesi (logout atau timeout di tab lain) atau tiba setelah desktop ZK-nya
	 * dihancurkan karena tab ditutup. Keduanya dicatat sebagai info biasa agar tidak memunculkan
	 * HTTP 500 dan tidak mengotori log error.</p>
	 *
	 * <h4>Penutupan terpusat session Hibernate — WAJIB dipahami sebelum menambah kode</h4>
	 * <p>Blok {@code finally} adalah <b>satu-satunya</b> tempat session Hibernate native
	 * per-thread ditutup untuk permintaan non-ZK (JSP di {@code /baru/modul/**} dan sejenisnya).
	 * {@code HibernateUtil.rollbackTransaction()} membatalkan transaksi yang belum di-commit lalu
	 * menutup session. Banyak JSP layanan memakai {@code currentNativeSession()} tanpa menutupnya
	 * sendiri — dan memang <b>tidak boleh</b> menutup sendiri, karena {@code closeSession()} atau
	 * {@code clear()} di tengah permintaan dapat membuang tulisan yang belum ter-flush sehingga
	 * penyimpanan gagal diam-diam.</p>
	 * <p>Session milik ZK tidak tersentuh di sini karena dikelola
	 * {@code OpenSessionInViewListener}. Proses non-JSP — thread latar, timer, dan API — tidak
	 * melewati filter ini sama sekali, sehingga wajib menutup session-nya sendiri di
	 * {@code finally} (lihat panduan di {@code HibernateUtil}).</p>
	 *
	 * <p>Dua pembersihan per-thread lain juga dilakukan: keputusan audit yang belum terpakai
	 * ({@code AuditTrailHelper.clearUpdateDecisions()}) agar tidak terbawa ke permintaan
	 * berikutnya pada worker thread yang sama dan salah meredam audit perubahan nyata, serta hasil
	 * audit error terakhir ({@code ErrorAuditUtil.clearLastResult()}) yang dapat berisi konten
	 * besar.</p>
	 *
	 * @param request  permintaan yang sedang dilayani
	 * @param response respons yang sedang dibentuk
	 * @param chain    rantai filter berikutnya
	 * @throws IOException      bila operasi masukan/keluaran gagal
	 * @throws ServletException bila rantai berikutnya melaporkan kegagalan
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		try {
			// 1. Setup Context
			RequestContext.set(req);
			ResponseContext.set(res);
			addCorsHeader(res);

			String uri = req.getRequestURI();

			// 2. Skip filter untuk resources statis murni dengan cepat
			if (uri.contains("/resources/")) {
				chain.doFilter(req, res);
				return;
			}

			String contextPath = req.getContextPath();
			// Cegah error substring jika URI entah bagaimana lebih pendek dari context
			// (Edge Case)
			String requestedPath = uri.length() >= contextPath.length() ? uri.substring(contextPath.length()) : uri;
			String lowerPath = requestedPath.toLowerCase();

			// 3. Cek apakah path perlu difilter
			if (isIgnoredPath(lowerPath)) {
				// Routing Logic - Teruskan lowerPath agar tidak perlu alokasi memori
				// toLowerCase() lagi
				handleRouting(req, res, chain, requestedPath, lowerPath);
			} else {
				// Untuk file gambar/css/js langsung lolos
				chain.doFilter(req, res);
			} 

		} catch (IllegalStateException ise) {
			// Race condition normal: request AU/ZK sedang berjalan bersamaan dengan
			// invalidate session (logout/timeout di tab lain, atau di thread lain).
			// Saat response mau ditulis (mis. Spring Security SaveToSessionResponseWrapper
			// atau ZK HttpAuWriter flush/close), session sudah invalid ->
			// "Session has already been invalidated". Ini BUKAN bug aplikasi dan TIDAK
			// boleh dicatat sebagai error mencolok -- cukup info/debug agar tidak
			// membuat 500 yang menakut-nakuti / mengotori log error.
			System.out.println("FilterJSP: diabaikan IllegalStateException (race logout/invalidate session): "
					+ ise.getMessage());
		} catch (org.zkoss.zk.ui.DesktopUnavailableException due) {
			// Race condition normal yang SEJENIS dengan IllegalStateException di atas: request
			// AU/ZK async (mis. polling server push) sampai persis setelah desktop-nya sudah
			// dihancurkan (tab ditutup pengguna / sesi timeout). Ini BUKAN bug aplikasi -- jangan
			// dicatat sebagai error mencolok, cukup info/debug spt penanganan IllegalStateException.
			System.out.println("FilterJSP: diabaikan DesktopUnavailableException (tab ditutup/desktop sudah tidak aktif): "
					+ due.getMessage());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/FilterJSP.java:82");

		} finally {

			// PENUTUPAN TERPUSAT (untuk AI & kode baru): ini adalah SATU-SATUNYA tempat native
			// ThreadLocal Hibernate session (HibernateUtil.currentNativeSession / currentSession
			// fallback) ditutup untuk request non-ZK (JSP /baru/modul/**, dsb). Dijalankan di akhir
			// SETIAP request. Banyak JSP service memakai currentNativeSession() TANPA menutup sendiri —
			// dan MEMANG TIDAK BOLEH menutup di JSP: closeSession()/clear() di tengah request bisa
			// membuang tulisan yang belum ter-flush (simpan gagal). rollbackTransaction() membatalkan
			// transaksi yang belum di-commit (jika ada) lalu closeSession() (clear+disconnect+close).
			// Session ZK (currentSession milik ZK) tidak tersentuh (dikelola OpenSessionInViewListener).
			// Proses NON-JSP (thread latar/timer/API) TIDAK lewat filter ini → wajib tutup sendiri di
			// finally (lihat COOKBOOK di HibernateUtil).
			try {
				ais.database.hibernate.HibernateUtil.rollbackTransaction();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/FilterJSP.java:99");
			}

			// Bersihkan keputusan audit per-thread. Keputusan yang tidak ter-consume
			// (mis. update yang dibatalkan karena tanpa perubahan bisnis) tidak boleh
			// terbawa ke request berikutnya pada worker thread yang sama, karena bisa
			// salah men-suppress audit untuk perubahan yang nyata.
			try {
				ais.database.hibernate.AuditTrailHelper.clearUpdateDecisions();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/FilterJSP.java:109");
			}

			// Lepas hasil audit error terakhir milik thread ini (bisa berisi content besar).
			try {
				ais.common.ErrorAuditUtil.clearLastResult();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Cleanup Context
			RequestContext.remove();
			ResponseContext.remove();
		}
	}

	// ========================================================================
	// LOGIC BLOCKING DEVICE
	// ========================================================================

	public static boolean checkSingleDeviceBlock(HttpServletRequest req, HttpServletResponse res, boolean redirect)
			throws Exception {
		HttpSession session = req.getSession(false);
		if (session == null)
			return false;

		Tbmuser tbmuser = (Tbmuser) session.getAttribute("mytbmuser");
		if (tbmuser == null)
			return false;

		return checkSingleDeviceBlock(tbmuser, req, res, redirect);
	}

	public static boolean checkSingleDeviceBlock(Tbmuser tbmuser, HttpServletRequest req, HttpServletResponse res,
			boolean redirect) throws Exception {

		if (!ConstantValues.satuperangkat_mahasiswa && !ConstantValues.satuperangkatipygbeda
				&& !ConstantValues.satuperangkat) {
			return false;
		}

		String currentSessionId = req.getSession().getId();
		String currentIp = req.getRemoteAddr();

		// Loop entry map
		for (Map.Entry<String, OnlineUsers> entry : SecurityFilter.dataOnline.entrySet()) {
			OnlineUsers online = entry.getValue();

			if (!isUserMatch(tbmuser, online))
				continue;

			if (ConstantValues.satuperangkat_mahasiswa || ConstantValues.satuperangkat) {
				if (online.getAktif() && online.getAccessedUsers() != null) {
					String oldSessionId = online.getAccessedUsers().getNama();
					if (!oldSessionId.equalsIgnoreCase(currentSessionId)) {
						return handleLogout(req, res, currentSessionId, tbmuser, "Akun telah login di perangkat lain.",
								redirect);
					}
				} else if (!online.getAktif() && online.getAccessedUsers() != null) {
					if (online.getAccessedUsers().getNama().equalsIgnoreCase(currentSessionId)) {
						return handleLogout(req, res, null, tbmuser, "Sesi akun telah dinonaktifkan oleh admin.",
								redirect);
					}
				}
			}

			if (ConstantValues.satuperangkatipygbeda && online.getAktif() && online.getLogin() != null) {
				String loginIp = online.getLogin().getIp();
				if (StringUtils.isNotEmpty(loginIp) && !loginIp.equalsIgnoreCase(currentIp)) {
					return handleLogout(req, res, currentSessionId, tbmuser, "Akun terdeteksi login dengan IP berbeda.",
							redirect);
				}
			}
		}
		return false;
	}

	private static boolean isUserMatch(Tbmuser currentUser, OnlineUsers online) {
		if (online.getMahasiswa() != null && currentUser.getMahasiswa() != null) {
			return online.getMahasiswa().getId().equals(currentUser.getMahasiswa().getId());
		}
		if (online.getSiswa() != null && currentUser.getSiswa() != null) {
			return online.getSiswa().getId().equals(currentUser.getSiswa().getId());
		}
		if (online.getTbmuser() != null && currentUser.getUserId() != null) {
			return online.getTbmuser().getUserId().equals(currentUser.getUserId());
		}
		return false;
	}

	private static boolean handleLogout(HttpServletRequest req, HttpServletResponse res, String sessionIdToRemove,
			Tbmuser user, String message, boolean redirect) throws IOException, ServletException {

		if (sessionIdToRemove != null) {
			SecurityFilter.dataOnline.remove(sessionIdToRemove);
		}

		if (redirect && !isCommitted(res)) {
			String encodedName = URLEncoder.encode(user == null ? "" : user.getUserNama(), "UTF-8");
			String fullMsg = URLEncoder.encode(message + " Harap menghubungi admin.", "UTF-8");
			try {
				try { if (!res.isCommitted()) res.resetBuffer(); } catch (IllegalStateException e) { return true; }
				req.getRequestDispatcher("/WEB-INF/u/logout.jsp?login_error=Akun+" + encodedName + "+" + fullMsg)
						.forward(req, res);
			} catch (IllegalStateException e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return true;
	}

	// ========================================================================
	// ROUTING LOGIC
	// ========================================================================

	@SuppressWarnings("deprecation")
	private void handleRouting(HttpServletRequest req, HttpServletResponse res, FilterChain chain, String path,
			String lowerPath) throws IOException, ServletException {

		String serverName = req.getServerName();

		// 1. Handle Subdomain Redirects
		if (handleSubdomainRedirects(req, res, serverName, lowerPath)) {
			return;
		}

		Common.ROOT = req.getContextPath();
		Common.REAL_PATH = req.getRealPath("/");
		Common.REAL_PATH_REPORT_TEMP = req.getRealPath("/report");

		int port = req.getServerPort();
		Common.CURRENT_URL_SIMPLE = (req.isSecure() ? "https://" : "http://") + serverName
				+ (port == 80 || port == 443 ? "" : ":" + port);

		

		

		// 3. Path Forwarding
		if (lowerPath.endsWith("/main2") || lowerPath.contains("/main2/")) {
			forward(req, res, "/WEB-INF/uiux/ux_pages/index.jsp");
		} else if (lowerPath.contains("/new/")) {
			// Efisiensi memori: hindari .split()
			int idx = path.indexOf("new");
			String subPath = (idx != -1) ? path.substring(idx + 3) : "";
			String target = (lowerPath.endsWith("/new") || lowerPath.endsWith("/new/"))
					? "/WEB-INF/uiux/dist/pages/index.html"
					: "/WEB-INF/uiux" + subPath;
			forward(req, res, target);
		} else if (lowerPath.endsWith("/pm")) {
			HttpSession session = req.getSession(true);
			Long randLong = (Long) session.getAttribute("data_session_pmb");
			if (randLong == null || !Api.nexts.contains(randLong)) {
				forward(req, res, "/WEB-INF/z/x/y/pmb.jsp");
			} else {
				forward(req, res, "/WEB-INF/z/x/y/pm.zul");
			}
		} else if (isUtilityJsp(lowerPath)) {
			// Ambil nama file dengan cepat
			int lastSlash = path.lastIndexOf('/');
			String fileName = (lastSlash != -1) ? path.substring(lastSlash + 1) : path;
			forward(req, res, "/WEB-INF/u/" + fileName);
		} else if (lowerPath.endsWith("accept.jsp"))
			forward(req, res, "/accept");
		else if (lowerPath.endsWith("code.jsp"))
			forward(req, res, "/code");
		else if (lowerPath.endsWith("broken.jsp"))
			forward(req, res, "/broken");
		else if (lowerPath.endsWith("error.jsp"))
			forward(req, res, "/error");
		else if (lowerPath.endsWith("redirect"))
			forward(req, res, "/redirect");
		else if (lowerPath.endsWith(".zul")) {
			handleZulFiles(req, res, path, lowerPath);
		} else if (lowerPath.endsWith("login.jsp") || lowerPath.endsWith("ecampus.jsp")
				|| lowerPath.endsWith("eschool.jsp")) {
			redirectToLogin(req, res);
		} else if (lowerPath.contains("/whatsapp/") || lowerPath.contains("/ux")) {
			handleDynamicPath(req, res, path);
		} else if (lowerPath.endsWith(".jsp") || lowerPath.endsWith(".jspx")) {
			safeSendRedirect(req, res, req.getContextPath() + "/");
		} else {
			chain.doFilter(req, res);
		}
	}

	private boolean handleSubdomainRedirects(HttpServletRequest req, HttpServletResponse res, String serverName,
			String path) throws IOException {
		String query = req.getQueryString();

		// Jika terdapat parameter / query string sekecil apa pun, aturan tidak berlaku
		if (query != null && !query.isEmpty()) {
			return false;
		}

//		if(serverName.toLowerCase().contains("doupload") || query.toLowerCase().contains("doupload")) {
//			return false;
//		}

		String context = req.getContextPath();

		// Aturan redirect dieksekusi hanya jika TIDAK ADA query string
//		if ((serverName.startsWith("pmb") || serverName.startsWith("spmb") || serverName.startsWith("um."))
//				&& !(path.endsWith("pmb") || path.endsWith("pmb2"))) {
//			res.sendRedirect(context + "/pmb");
//			return true;
//		}

//		if ((serverName.startsWith("anjungan")) && !(path.endsWith("anjungan"))) {
//			res.sendRedirect(context + "/anjungan");
//			return true;
//		}

		if ((serverName.startsWith("ppdb") || serverName.startsWith("psb")) && !path.endsWith("ppdb")) {
			safeSendRedirect(req, res, context + "/ppdb");
			return true;
		}
		if ((serverName.startsWith("alumni") || serverName.startsWith("tracer")) && !path.endsWith("alumni")) {
			safeSendRedirect(req, res, context + "/alumni");
			return true;
		}

		return false;
	}

	private void handleZulFiles(HttpServletRequest req, HttpServletResponse res, String originalPath, String lowerPath)
			throws IOException, ServletException {

		String ctx = req.getContextPath();

		if (lowerPath.endsWith("welpus.zul"))
			safeSendRedirect(req, res, ctx + "/welpus");
		else if (lowerPath.endsWith("dekstop.zul"))
			safeSendRedirect(req, res, ctx + "/dekstop");
		else if (lowerPath.endsWith("welsis.zul"))
			safeSendRedirect(req, res, ctx + "/welsis");
		else if (lowerPath.endsWith("vendor.zul"))
			safeSendRedirect(req, res, ctx + "/vendor");
		else if (lowerPath.endsWith("psb.zul") || lowerPath.endsWith("psb"))
			safeSendRedirect(req, res, ctx + "/ppdb");
		else if (lowerPath.endsWith("karir.zul"))
			safeSendRedirect(req, res, ctx + "/karir");
		else if (lowerPath.endsWith("pmb.zul"))
			safeSendRedirect(req, res, ctx + "/pmb");
		else if (lowerPath.endsWith("alumni.zul"))
			safeSendRedirect(req, res, ctx + "/alumni");
		else if (lowerPath.endsWith("main.zul"))
			safeSendRedirect(req, res, ctx + "/main");
		else if (lowerPath.endsWith("login.zul"))
			redirectToLogin(req, res);
		else {
			String cleanPath = originalPath;
			// Efisiensi memori: Ganti .replace(ROOT, "") dengan .substring() yang jauh
			// lebih ringan
			if (Common.ROOT != null && !Common.ROOT.isEmpty() && cleanPath.startsWith(Common.ROOT)) {
				cleanPath = cleanPath.substring(Common.ROOT.length());
			}

			// Cegah double slashes tanpa menggunakan Regex replaceAll
			if (cleanPath.startsWith("//")) {
				cleanPath = cleanPath.substring(1);
			}

			forward(req, res, "/WEB-INF/z/x/y" + (!cleanPath.startsWith("/") ? "/" : "") + cleanPath);
		}
	}

	private void handleDynamicPath(HttpServletRequest req, HttpServletResponse res, String path)
			throws ServletException, IOException {
		String target = "";
		try {
			// Efisiensi memori: Gunakan indexOf + substring alih-alih regex split()
			int waIdx = path.indexOf("/whatsapp/");
			if (waIdx != -1) {
				target = "/WEB-INF/o/whatsapp/" + path.substring(waIdx + 10);
			} else {
				int uxIdx = path.indexOf("/ux/");
				if (uxIdx != -1) {
					target = "/WEB-INF/o/ux/" + path.substring(uxIdx + 4);
				} else if (path.endsWith("/ux")) {
					target = "/WEB-INF/o/ux/index.jsp";
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/FilterJSP.java:386");
		}

		if (!target.isEmpty()) {
			forward(req, res, target);
		}
	}

	private boolean isUtilityJsp(String path) {
		return path.contains("capture.jsp") || path.contains("capture_keterangan.jsp")
				|| path.contains("capture_video.jsp") || path.contains("doupload.jsp")
				|| path.contains("capture_screen.jsp") || path.contains("capture_lokasi.jsp")
				|| path.contains("capture_audio.jsp") || path.contains("jml_pendaftar.jsp") || path.contains("mail.jsp")
				|| path.contains("read_qr_code") || path.contains("read_rfid");
	}

	// ========================================================================
	// HELPER METHODS
	// ========================================================================

	private void forward(HttpServletRequest req, HttpServletResponse res, String path)
			throws ServletException, IOException {
		if (isCommitted(res)) {
			return;
		}
		try {
			if (!res.isCommitted()) {
				try { res.resetBuffer(); } catch (IllegalStateException e) { return; }
			}
			req.getRequestDispatcher(path).forward(req, res);
		} catch (IllegalStateException e) {
			// Response sudah terlanjur terkirim oleh filter/servlet lain. Jangan forward ulang.
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void redirectToLogin(HttpServletRequest req, HttpServletResponse res) throws IOException {
		String q = req.getQueryString();
		safeSendRedirect(req, res, req.getContextPath() + "/login" + (q != null && !q.isEmpty() ? "?" + q : ""));
	}

	private static boolean isCommitted(HttpServletResponse res) {
		return res == null || res.isCommitted();
	}

	private static void safeSendRedirect(HttpServletRequest req, HttpServletResponse res, String target) throws IOException {
		if (isCommitted(res)) {
			return;
		}
		res.sendRedirect(target);
	}

	private boolean isIgnoredPath(String p) {
		// Logika tetap, menggunakan 'p' yang sudah lowerCase dari doFilter
		return !(p.endsWith(".wcs") || p.endsWith(".css") || p.endsWith(".dsp") || p.contains("jsessionid")
				|| p.endsWith("daftaranggota") || p.endsWith("halamananggota") || p.endsWith(".js") || p.contains("al")
				|| p.contains("pdf") || p.contains("lampiran") || p.endsWith(".wpd") || p.endsWith(".png")
				|| p.endsWith(".mp4") || p.endsWith(".mp3") || p.endsWith(".mov") || p.endsWith(".jpg")
				|| p.endsWith(".jpeg") || p.endsWith(".webp") || p.endsWith(".gif") || p.endsWith("pmb") || p.endsWith("anjungan")
				|| p.endsWith("psb") || p.endsWith("alumni") || p.endsWith("hadir") || p.endsWith(".svg")
				|| p.endsWith("zi"));
	}

	@SuppressWarnings("unused")
	private boolean isHtmlRequest(String path) {
		return !path.endsWith(".json") && !path.endsWith(".xml");
	}

	@SuppressWarnings("unused")
	private void sendMaintenancePage(HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter writer = response.getWriter();
		writer.println("<html>");
		writer.println(
				"<h2 style='text-align:center;'>Sistem dalam proses inisiasi data, harap tunggu beberapa saat lagi</h2>");
		writer.println("<script type=\"text/javascript\">setTimeout(function(){location.reload();}, 3000);</script>");
		writer.println("</html>");
	}

	private void addCorsHeader(HttpServletResponse response) {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
		response.addHeader("Access-Control-Allow-Headers",
				"X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept");
		response.addHeader("Access-Control-Max-Age", "1728000");
	}

}