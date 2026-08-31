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
 * Komponen batas HTTP/servlet untuk filter jsp. Tipe ini menerima input dari luar aplikasi,
 * meneruskannya ke layanan domain, lalu membentuk respons tanpa menduplikasi aturan bisnis.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini mendeklarasikan kontrak {@link Filter}. Implementasi konkret
 * bertanggung jawab atas transaksi, resource, error handling, dan efek samping; pemanggil sebaiknya bergantung
 * pada kontrak ini agar tidak menggandakan integrasi.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code init()}); validasi/perhitungan
 * ({@code checkSingleDeviceBlock()}, {@code checkSingleDeviceBlock()}); operasi domain lain ({@code destroy()},
 * {@code doFilter()}, {@code isUserMatch()}, {@code handleLogout()}, {@code handleRouting()}, {@code
 * handleSubdomainRedirects()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class FilterJSP implements Filter {

	@Override
	public void init(FilterConfig confg) throws ServletException {
		String initParam = confg.getInitParameter("init-param");
		System.out.println("FilterJSP Initialized. Param: " + initParam);
	}

	@Override
	public void destroy() {
		// Cleanup resources if needed
	}

	/**
	 * Logic utama filter
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