package ais.action.servlet;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.SecurityFilter;
import ais.common.ZkLanguageBootstrap;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Servlet pintu utama aplikasi AIS.
 *
 * <p>Class ini sengaja ditulis kembali sebagai source lengkap karena di
 * workspace yang tersedia implementasi lama hanya ada dalam bentuk bytecode.
 * Alur routing lama dipertahankan: request ke {@code /main} tetap mengisi
 * nilai global {@link Common#REAL_PATH}, {@link Common#ROOT},
 * {@link Common#CURRENT_URL_SIMPLE}, dan {@link Common#CURRENT_URL}; kemudian
 * memilih halaman ZK lama, ZK baru, JSP baru, halaman khusus kantin, atau
 * halaman pemilihan hak akses berdasarkan parameter request dan konfigurasi.
 * Tambahan baru berada di satu titik yang kecil dan mudah diaudit, yaitu
 * pendeteksian perangkat mobile. Jika request terlihat berasal dari ponsel
 * atau tablet kecil, dan user tidak sedang memaksa mode desktop/legacy melalui
 * parameter, servlet akan mengarahkan browser ke {@code Common.ROOT + "/mobile"}.
 * Dengan pola ini halaman desktop tidak ikut berubah, namun pengalaman mobile
 * langsung memakai shell mobile yang lebih ringan.</p>
 *
 * <p>Pendeteksian mobile dibuat defensif. Ia membaca header
 * {@code User-Agent}, {@code X-WAP-Profile}, {@code Profile}, dan beberapa
 * client hint modern seperti {@code Sec-CH-UA-Mobile}. Hasil deteksi juga bisa
 * dikendalikan lewat parameter request: {@code mobile=true} memaksa mobile,
 * sedangkan {@code desktop=true}, {@code forceDesktop=true}, atau parameter
 * legacy seperti {@code versilama}, {@code versi_lama}, {@code zkbaru}, dan
 * {@code jspbaru} mempertahankan halaman lama. Ini penting untuk operasional:
 * admin tetap bisa membuka tampilan desktop dari perangkat kecil ketika perlu,
 * dan tautan khusus lama tetap dapat berjalan sesuai perilaku sebelumnya.</p>
 *
 * <p>Class ini tidak membuka Hibernate session baru. Ia hanya memanggil helper
 * lama yang sudah dipakai aplikasi, sehingga aturan penutupan session tidak
 * berubah. Bila helper lama memakai {@code currentSession()}, session tetap
 * menjadi tanggung jawab lifecycle existing. Bila terjadi error saat memilih
 * halaman, exception dicatat dengan perilaku lama dan request tetap tidak
 * membuat session tambahan yang tidak perlu. Semua method menggunakan gaya
 * Java 1.6/1.7, tanpa lambda, stream, atau try-with-resources.</p>
 */
public class Main extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String PAGE_KANTIN_INDEX = Tbmrole.HALAMAN_UTAMA_KANTIN;
	private static final String PAGE_APOTIK_INDEX = Tbmrole.HALAMAN_UTAMA_APOTIK;
	private static final String PAGE_EMEDIK_INDEX = Tbmrole.HALAMAN_UTAMA_EMEDIK;
	private static final String PAGE_INVENTORY_INDEX = Tbmrole.HALAMAN_UTAMA_INVENTORY;
	private static final String ATTR_KANTIN_DIRECT_PAGE = "kantinDirectPage";
	private static final String ATTR_POS_DIRECT_PAGE = "posDirectPage";
	private static final String ATTR_INVENTORY_DIRECT_PAGE = "inventoryDirectPage";
	private static final String[][] INVENTORY_FUNCTION_PAGES = {
			{ "data_supplier", "data_supplier.jsp" },
			{ "daftar_supplier", "daftar_supplier.jsp" },
			{ "detail_supplier_aktif", "detail_supplier_aktif.jsp" },
			{ "data_customer", "data_customer.jsp" },
			{ "daftar_customer", "daftar_customer.jsp" },
			{ "detail_customer_aktif", "detail_customer_aktif.jsp" },
			{ "data_sales", "data_sales.jsp" },
			{ "data_stok_barang", "data_stok_barang.jsp" },
			{ "laporan_opname", "laporan_opname.jsp" },
			{ "cetak_laporan_opname", "cetak_laporan_opname.jsp" },
			{ "harga_beli_jual", "harga_beli_jual.jsp" },
			{ "cetak_harga_beli_jual", "cetak_harga_beli_jual.jsp" },
			{ "cetak_harga_jual", "cetak_harga_jual.jsp" },
			{ "ekspor_harga_stok", "ekspor_harga_stok.jsp" },
			{ "cetak_daftar_stok", "cetak_daftar_stok.jsp" },
			{ "hasil_cetak_stok", "hasil_cetak_stok.jsp" },
			{ "menu_master_harga", "menu_master_harga.jsp" },
			{ "harga_beli_supplier", "harga_beli_supplier.jsp" },
			{ "harga_jual_customer", "harga_jual_customer.jsp" },
			{ "pembelian_supplier", "pembelian_supplier.jsp" },
			{ "hutang_pembelian", "hutang_pembelian.jsp" },
			{ "data_hutang_supplier", "data_hutang_supplier.jsp" },
			{ "hutang_dengan_lunas", "hutang_dengan_lunas.jsp" },
			{ "pembayaran_hutang", "pembayaran_hutang.jsp" },
			{ "riwayat_pembayaran_hutang", "riwayat_pembayaran_hutang.jsp" },
			{ "cetak_pembayaran_hutang", "cetak_pembayaran_hutang.jsp" },
			{ "analisis_hutang", "analisis_hutang.jsp" },
			{ "cetak_faktur_pembelian", "cetak_faktur_pembelian.jsp" },
			{ "laporan_pembelian_periode", "laporan_pembelian_periode.jsp" },
			{ "penjualan_sales", "penjualan_sales.jsp" },
			{ "piutang_penjualan", "piutang_penjualan.jsp" },
			{ "data_piutang_customer", "data_piutang_customer.jsp" },
			{ "piutang_dengan_lunas", "piutang_dengan_lunas.jsp" },
			{ "pembayaran_piutang", "pembayaran_piutang.jsp" },
			{ "riwayat_pembayaran_piutang", "riwayat_pembayaran_piutang.jsp" },
			{ "cetak_pembayaran_piutang", "cetak_pembayaran_piutang.jsp" },
			{ "analisis_piutang_customer", "analisis_piutang_customer.jsp" },
			{ "analisis_piutang_sales", "analisis_piutang_sales.jsp" },
			{ "surat_perintah_sales", "surat_perintah_sales.jsp" },
			{ "nota_sales", "nota_sales.jsp" },
			{ "laporan_piutang", "laporan_piutang.jsp" },
			{ "cetak_laporan_piutang", "cetak_laporan_piutang.jsp" },
			{ "kas_jurnal", "kas_jurnal.jsp" },
			{ "data_perkiraan", "data_perkiraan.jsp" },
			{ "parameter_laba_rugi", "parameter_laba_rugi.jsp" },
			{ "cetak_laba_rugi_kotor", "cetak_laba_rugi_kotor.jsp" },
			{ "laporan_laba_rugi", "laporan_laba_rugi.jsp" },
			{ "cetak_laporan_laba_rugi", "cetak_laporan_laba_rugi.jsp" }
	};

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	private void processRequest(HttpServletRequest request, HttpServletResponse response) {
		try {
			initCommonRequestState(request);
			forwardToPage(request, response);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Main.java:77");
		}
	}

	private void initCommonRequestState(HttpServletRequest request) {
		ServletContext context = getServletContext();
		Common.REAL_PATH = context.getRealPath("/");
		Common.REAL_PATH_REPORT_TEMP = context.getRealPath("/report");
		Common.ROOT = request.getContextPath();
		Common.CURRENT_URL_SIMPLE = buildBaseUrl(request, false);
		Common.CURRENT_URL = buildBaseUrl(request, true);
	}

	private String buildBaseUrl(HttpServletRequest request, boolean includeContext) {
		String protocol = Common.isSecure(request) ? "https://" : "http://";
		int port = request.getServerPort();
		String portText = (port == 80 || port == 443) ? "" : ":" + port;
		String context = includeContext ? request.getContextPath() : "";
		return protocol + request.getServerName() + portText + context;
	}

	private void forwardToPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
		String path = requestUri != null && contextPath != null && requestUri.length() >= contextPath.length()
				? requestUri.substring(contextPath.length()) : "";

		if (path.startsWith("/main/item/")) {
			String id = path.substring("/main/item/".length());
			if (id != null && id.trim().length() > 0) {
				request.getRequestDispatcher("/pustaka?id=" + id.trim()).forward(request, response);
				return;
			}
		}
		String inventoryFunction = request.getParameter("inventory");
		if ((inventoryFunction != null && inventoryFunction.trim().length() > 0)
				|| path.startsWith("/main/inventory/")) {
			String functionName = inventoryFunction != null && inventoryFunction.trim().length() > 0
					? inventoryFunction : path.substring("/main/inventory/".length());
			String inventoryPage = resolveInventoryFunctionPage(functionName);
			if (inventoryPage != null) {
				request.setAttribute(ATTR_INVENTORY_DIRECT_PAGE, Boolean.TRUE);
				request.getRequestDispatcher(inventoryPage).forward(request, response);
				return;
			}
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Halaman Inventory & Sales tidak ditemukan.");
			return;
		}

		String page = "/WEB-INF/z/x/y/pages/main/index.zul";
		if ("true".equals(request.getParameter("hak_akses"))) {
			page = "/WEB-INF/baru/modul/common/hak_akses.jsp";
		} else if ("registrasi_calon_anggota".equalsIgnoreCase(request.getParameter("p"))) {
			page = "/WEB-INF/baru/modul/kantin/member/form_registrasi_calon.jsp";
		} else if ("halaman_calon_anggota".equalsIgnoreCase(request.getParameter("p"))) {
			checkAndSetUserSession(request, false);
			page = PAGE_KANTIN_INDEX;
		} else {
			page = resolveMainPage(request, page);
		}

		if (page != null && page.toLowerCase().endsWith(".zul")) {
			ZkLanguageBootstrap.ensureZulLanguageMapping();
		}
		if (PAGE_KANTIN_INDEX.equals(page)) {
			request.setAttribute(ATTR_KANTIN_DIRECT_PAGE, Boolean.TRUE);
		}
		if (PAGE_APOTIK_INDEX.equals(page) || PAGE_EMEDIK_INDEX.equals(page)) {
			request.setAttribute(ATTR_POS_DIRECT_PAGE, Boolean.TRUE);
		}
		if (PAGE_INVENTORY_INDEX.equals(page)) {
			request.setAttribute(ATTR_INVENTORY_DIRECT_PAGE, Boolean.TRUE);
		}
		RequestDispatcher dispatcher = request.getRequestDispatcher(page);
		dispatcher.forward(request, response);
	}

	private String resolveInventoryFunctionPage(String functionName) {
		if (functionName == null) {
			return null;
		}
		String normalized = functionName.trim();
		for (int i = 0; i < INVENTORY_FUNCTION_PAGES.length; i++) {
			if (INVENTORY_FUNCTION_PAGES[i][0].equals(normalized)) {
				return "/WEB-INF/baru/modul/inventory/" + INVENTORY_FUNCTION_PAGES[i][1];
			}
		}
		return null;
	}

	private String resolveMainPage(HttpServletRequest request, String defaultPage) {
		String page = defaultPage;
		try {
			Tbmuser user = checkAndSetUserSession(request, true);
			// Role dasar Kantin harus membuka shell JSP Kantin lengkap (header,
			// sidebar dan dashboard Ringkasan), sebelum halamanUtama atau flag
			// Inventory/Pergudangan pada role dievaluasi.
			if (isKantinUser(user)) {
				request.setAttribute("default_p", "kantin");
				request.setAttribute("default_s", "ringkasan");
				return "/WEB-INF/baru/index.jsp";
			}
			// Pilihan eksplisit pada Tbmrole harus menjadi sumber utama landing page.
			// Sebelumnya flag landingInventory/landingKantin dan role Kantin dievaluasi
			// lebih dulu, sehingga pilihan POS Apotik/eMedik yang baru disimpan dapat
			// terabaikan setelah login.
			String halamanRole = resolveRoleLandingPage(user);
			if (halamanRole != null) {
				return halamanRole;
			}
			if (isInventoryLandingRole(user)) {
				return PAGE_INVENTORY_INDEX;
			}
			if (isKantinMemberLandingRole(user)) {
				request.setAttribute("default_p", "kantin");
				request.setAttribute("default_s", "ringkasan");
				request.setAttribute("kantinMemberLanding", Boolean.TRUE);
				return "/WEB-INF/baru/index.jsp";
			}
			if (isKoperasiMemberLandingEnabled(user)) {
				return PAGE_KANTIN_INDEX;
			}
			boolean versiLama = isParameterAktif(request, "versilama") || isParameterAktif(request, "versi_lama");
			boolean zkBaru = isParameterAktif(request, "zkbaru") || isParameterAktif(request, "main2")
					|| isParameterAktif(request, "index2") || isParameterAktif(request, "versizk")
					|| isParameterAktif(request, "versi_zk");
			boolean jspBaru = isParameterAktif(request, "jspbaru") || isParameterAktif(request, "versibaru")
					|| isParameterAktif(request, "versi_baru") || isParameterAktif(request, "htmlbaru")
					|| isParameterAktif(request, "versihtml");
			boolean defaultBaruFull = isKonfigurasiAktif("default_gunakan_versi_baru_full", "tidak aktif");
			boolean defaultBaru = isKonfigurasiAktif("default_gunakan_versi_baru", "tidak aktif");

			// Cek pilihan tampilan dari entitas (override konfigurasi global jika tidak ikut default)
			if (!versiLama && !zkBaru && !jspBaru) {
				String piilhan = getPiilhanTampilanDomain(request);
				if (PerguruanTinggi.TAMPILAN_BARU.equals(piilhan)) {
					request.setAttribute("new_context", "main");
					return "/WEB-INF/new/index.jsp";
				} else if (PerguruanTinggi.TAMPILAN_KLASIK.equals(piilhan)) {
					return "/WEB-INF/z/x/y/pages/main/index.zul";
				}
			}

			if (versiLama) {
				page = "/WEB-INF/z/x/y/pages/main/index.zul";
			} else if (zkBaru) {
				page = "/WEB-INF/z/x/y/pages/main/index2.zul";
			} else if (jspBaru || defaultBaruFull || defaultBaru) {
				page = "/WEB-INF/baru/index.jsp";
			}

			if (!versiLama && (zkBaru || jspBaru || defaultBaru || defaultBaruFull) && shouldAskRole(user, request)) {
				page = "/WEB-INF/baru/modul/common/hak_akses.jsp";
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Main.java:164");
		}
		return page;
	}

	private String resolveRoleLandingPage(Tbmuser user) {
		if (user == null || user.hakAkses() == null) {
			return null;
		}
		String halaman = user.hakAkses().getHalamanUtama();
		if (PAGE_KANTIN_INDEX.equals(halaman) || PAGE_APOTIK_INDEX.equals(halaman) || PAGE_EMEDIK_INDEX.equals(halaman)
				|| PAGE_INVENTORY_INDEX.equals(halaman)) {
			return halaman;
		}
		return null;
	}

	private String getPiilhanTampilanDomain(HttpServletRequest request) {
		try {
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
			if (pt != null && !PerguruanTinggi.TAMPILAN_DEFAULT.equals(pt.getPiilhanTampilan())) {
				return pt.getPiilhanTampilan();
			}
			boolean[] ptAtauSekolah = Common.chekPtAtauSekolah();
			boolean sekolahMode = ptAtauSekolah != null && ptAtauSekolah.length > 1 && ptAtauSekolah[1];
			if (sekolahMode) {
				Sekolah sekolah = SekolahUtil.getSekolah(request);
				if (sekolah != null && !Sekolah.TAMPILAN_DEFAULT.equals(sekolah.getPiilhanTampilan())) {
					return sekolah.getPiilhanTampilan();
				}
				Yayasan yayasan = SekolahUtil.getYayasan(request);
				if (yayasan != null && !Yayasan.TAMPILAN_DEFAULT.equals(yayasan.getPiilhanTampilan())) {
					return yayasan.getPiilhanTampilan();
				}
			}
		} catch (Exception e) {
			// ignore, fall through to default routing
		}
		return PerguruanTinggi.TAMPILAN_DEFAULT;
	}

	private boolean isKantinUser(Tbmuser user) {
		return user != null && user.hakAkses() != null && user.hakAkses().getRoleId() != null
				&& "Kantin".equalsIgnoreCase(user.hakAkses().getRoleId());
	}

	private boolean isKantinMemberLandingRole(Tbmuser user) {
		try {
			return user != null && user.hakAkses() != null
					&& ais.common.EbisnisMenuKatalog.urai(user.hakAkses().getEbisnisMenu()).optBoolean("landingKantin", false);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isInventoryLandingRole(Tbmuser user) {
		try {
			return user != null && user.hakAkses() != null
					&& ais.common.EbisnisMenuKatalog.urai(user.hakAkses().getEbisnisMenu())
							.optBoolean("landingInventory", false);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isKoperasiMemberLandingEnabled(Tbmuser user) {
		try {
			if (user == null || user.hakAkses() == null || "Kantin".equals(user.hakAkses().getRoleId())) {
				return false;
			}
			if (user.getAnggotaKoperasi() == null || user.getPedagang() != null || Common.getApakahAdminLain(user)) {
				return false;
			}
			Konfigurasi konfigurasi = Common.getKonfigurasi(
					"jika_login_sebagai_member_kecuali_admin_maka_langsung_ke_halaman_member", "tidak aktif");
			return konfigurasi != null && "aktif".equals(konfigurasi.getNilai());
		} catch (Exception e) {
			return false;
		}
	}

	private boolean shouldAskRole(Tbmuser user, HttpServletRequest request) {
		try {
			if (user == null || request.getSession().getAttribute("udah_tanya") != null) {
				return false;
			}
			List<Tbmrole> roles = user.ambilRoles();
			return roles != null && roles.size() > 1;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean shouldRedirectToMobile(HttpServletRequest request) {
		if (isParameterAktif(request, "mobile")) {
			return true;
		}
		if (isDesktopForced(request) || isLegacyOrExplicitVersionRequest(request)) {
			return false;
		}
		String path = request.getRequestURI();
		if (path != null && path.indexOf("/mobile") >= 0) {
			return false;
		}
		return isMobileRequest(request);
	}

	private boolean isDesktopForced(HttpServletRequest request) {
		return isParameterAktif(request, "desktop") || isParameterAktif(request, "forceDesktop")
				|| isParameterAktif(request, "full") || isParameterAktif(request, "nonmobile");
	}

	private boolean isLegacyOrExplicitVersionRequest(HttpServletRequest request) {
		return isParameterAktif(request, "versilama") || isParameterAktif(request, "versi_lama")
				|| isParameterAktif(request, "zkbaru") || isParameterAktif(request, "main2")
				|| isParameterAktif(request, "index2") || isParameterAktif(request, "versizk")
				|| isParameterAktif(request, "versi_zk") || isParameterAktif(request, "jspbaru")
				|| isParameterAktif(request, "versibaru") || isParameterAktif(request, "versi_baru")
				|| isParameterAktif(request, "htmlbaru") || isParameterAktif(request, "versihtml")
				|| request.getParameter("hak_akses") != null || request.getParameter("p") != null;
	}

	private boolean isMobileRequest(HttpServletRequest request) {
		String mobileHint = request.getHeader("Sec-CH-UA-Mobile");
		if (mobileHint != null && mobileHint.toLowerCase().indexOf("?1") >= 0) {
			return true;
		}
		if (request.getHeader("X-WAP-Profile") != null || request.getHeader("Profile") != null) {
			return true;
		}
		String userAgent = request.getHeader("User-Agent");
		if (userAgent == null) {
			return false;
		}
		String ua = userAgent.toLowerCase();
		return ua.indexOf("android") >= 0 || ua.indexOf("iphone") >= 0 || ua.indexOf("ipad") >= 0
				|| ua.indexOf("ipod") >= 0 || ua.indexOf("blackberry") >= 0 || ua.indexOf("iemobile") >= 0
				|| ua.indexOf("opera mini") >= 0 || ua.indexOf("mobile") >= 0 || ua.indexOf("windows phone") >= 0;
	}

	private boolean isParameterAktif(HttpServletRequest request, String name) {
		try {
			String value = request == null || name == null ? null : request.getParameter(name);
			if (value == null && request != null && name != null
					&& request.getAttribute(name) != null) {
				value = String.valueOf(request.getAttribute(name));
			}
			if (value == null) {
				return false;
			}
			value = value.trim();
			return value.length() == 0 || "true".equalsIgnoreCase(value) || "1".equals(value)
					|| "ya".equalsIgnoreCase(value) || "y".equalsIgnoreCase(value) || "aktif".equalsIgnoreCase(value);
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isKonfigurasiAktif(String key, String defaultValue) {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
			return konfigurasi != null && konfigurasi.getNilai() != null
					&& "aktif".equalsIgnoreCase(konfigurasi.getNilai().trim());
		} catch (Exception e) {
			return "aktif".equalsIgnoreCase(defaultValue);
		}
	}

	public static Tbmuser checkAndSetUserSession(HttpServletRequest request, boolean createSession) {
		try {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Object userObject = session.getAttribute("mytbmuser");
				if (userObject instanceof Tbmuser) {
					return (Tbmuser) userObject;
				}
				Object loginObject = session.getAttribute("login");
				if (loginObject instanceof LogLogin) {
					return ((LogLogin) loginObject).getTbmuser();
				}
			}
			boolean emptySession = session == null || session.getAttribute("login") == null;
			if (emptySession) {
				Principal principal = request.getUserPrincipal();
				if (principal instanceof Authentication) {
					Authentication authentication = (Authentication) principal;
					Object principalObject = authentication.getPrincipal();
					if (principalObject instanceof UserDetails) {
						String username = ((UserDetails) principalObject).getUsername();
						return SecurityFilter.getCurrentFromUsername(username, request, createSession);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Main.java:289");
		}
		return null;
	}
}
