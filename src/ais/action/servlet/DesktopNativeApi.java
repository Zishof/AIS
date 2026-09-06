package ais.action.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;

import ais.action.servlet.api.ApiUtil;
import ais.common.Common;
import ais.common.newui.NewUiUnggahRequest;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Gerbang API native untuk halaman desktop Flutter.
 *
 * <p>Servlet ini tidak merender HTML, ZUL, WebView, maupun iframe. Setelah token,
 * role, relasi {@code job_has_menu}, status aktif dan status leaf diverifikasi,
 * request diteruskan secara internal ke service JSON New UI. Dengan demikian
 * renderer Flutter dapat memakai metadata/list/mutation Generic CRUD yang telah
 * mengikat lifecycle Action existing tanpa membawa presentasi ZK ke klien.</p>
 *
 * <p><b>Unggahan berkas.</b> Permintaan yang membawa berkas datang sebagai
 * {@code multipart/form-data} dan diurai lebih dahulu oleh
 * {@link NewUiUnggahRequest}, karena deskriptor Servlet 2.5 aplikasi ini tidak
 * menyediakan {@code getPart()} maupun pembacaan field multipart oleh
 * {@code getParameter()}. Setelah diurai, permintaan berperilaku seperti form
 * biasa sehingga otentikasi, penjaga, dan controller di hilir tidak berubah.</p>
 *
 * <p>Route yang belum mempunyai service native tetap fail-closed oleh resolver
 * New UI dengan respons {@code NOT_MAPPED}/{@code SERVICE_NOT_FOUND}. Hal ini
 * penting: menebak CRUD dari bentuk ZUL saja dapat melewatkan validasi, detail
 * transaksi, stok, persetujuan, cetak, dan efek bisnis Action.</p>
 */
public class DesktopNativeApi extends HttpServlet {

	/**
	 * ID versi serialisasi tetap untuk kompatibilitas antar versi kelas servlet ini.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Menolak semua permintaan GET.
	 *
	 * <p>Token otentikasi dan parameter menu/role tidak pernah boleh muncul pada
	 * query string URL karena akan tersimpan di riwayat browser, log server, dan
	 * header Referer. Endpoint ini hanya menerima POST (lihat {@link #doPost});
	 * GET selalu dijawab {@link HttpServletResponse#SC_METHOD_NOT_ALLOWED}.</p>
	 *
	 * @param request permintaan HTTP masuk (tidak dipakai selain untuk kontrak servlet)
	 * @param response respons HTTP; diisi status 405 dan pesan penjelasan
	 * @throws IOException bila penulisan pesan kesalahan ke respons gagal
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Gunakan POST untuk API desktop native.");
	}

	/**
	 * Memvalidasi token, role, dan menu aplikasi desktop, lalu meneruskan permintaan
	 * secara internal ke service JSON New UI ({@code /new?service=1&menuId=...}) agar
	 * renderer Flutter mendapat metadata/list/mutation Generic CRUD yang sama dengan
	 * yang dipakai UI ZK, tanpa membawa presentasi ZK ke klien.
	 *
	 * <p>Urutan pemrosesan:</p>
	 * <ol>
	 * <li>Header cache/referrer/{@code X-Content-Type-Options} diset lebih dulu agar
	 * token dan detail sesi tidak ikut ter-cache proxy, tersimpan di riwayat, atau
	 * di-sniff sebagai tipe konten lain.</li>
	 * <li>Bila permintaan {@code multipart/form-data}
	 * ({@link NewUiUnggahRequest#multipart(HttpServletRequest)}), badannya diurai lebih
	 * dulu oleh {@link NewUiUnggahRequest#urai(HttpServletRequest)} karena deskriptor
	 * Servlet 2.5 aplikasi ini tidak menyediakan {@code getPart()} maupun pembacaan
	 * field multipart oleh {@code getParameter()}. {@code request} yang dipakai sisa
	 * method ini DIGANTI dengan hasil bungkusan tersebut, sehingga langkah-langkah
	 * berikutnya tetap memanggil {@code getParameter()} seperti biasa. Kegagalan parse
	 * (berkas bukan .xlsx, terlalu besar, dsb.) dijawab {@code 400 Bad Request} dengan
	 * kode {@code UPLOAD_INVALID} dan method berhenti di sini.</li>
	 * <li>Parameter {@code token}, {@code menuId}, dan {@code roleId} dibaca dan
	 * diperiksa kelengkapannya; kekurangan salah satu dari {@code token}/{@code menuId}
	 * dijawab {@code 400 Bad Request} dengan kode {@code REQUEST_INVALID}.</li>
	 * <li>Token diverifikasi lewat {@link ApiUtil#currentUser(String)}; token tidak
	 * valid/kedaluwarsa dijawab {@code 401 Unauthorized} dengan kode
	 * {@code TOKEN_EXPIRED}.</li>
	 * <li>Role yang diminta diverifikasi kepemilikannya lewat
	 * {@link #findOwnedRole(Tbmuser, String)}; role yang bukan milik pengguna dijawab
	 * {@code 403 Forbidden} dengan kode {@code ROLE_FORBIDDEN}.</li>
	 * <li>Role dimuat ulang dari basis data agar daftar menunya terbaru, lalu menu yang
	 * diminta diverifikasi lewat {@link #findAuthorizedLeaf(List, Long)} -- pola yang
	 * identik dengan {@link DesktopMenuBootstrap#doPost}, lihat javadoc
	 * {@code findAuthorizedLeaf} di kelas tersebut untuk penjelasan lengkap kenapa
	 * validasi ini menutup celah spoofing menu lewat parameter URL. Menu yang tidak
	 * ditemukan/tidak aktif/bukan leaf dijawab {@code 403 Forbidden} dengan kode
	 * {@code MENU_FORBIDDEN}.</li>
	 * <li>Setelah lolos semua penjaga, sesi HTTP baru dibuat dan atribut ZK/New UI
	 * dipasang ({@code mytbmuser}, {@code currentMenus}/{@code current_menus}, dst.),
	 * serta map role aktif thread-safe {@link Tbmuser#getUserRoleYgDipakai} disamakan
	 * dengan role yang baru diverifikasi supaya {@code HakAksesApi} dan shell New UI
	 * konsisten dengan hasil validasi servlet ini.</li>
	 * <li>Permintaan di-forward secara internal ke
	 * {@code /new?service=1&menuId=<menuId>}. Query internal ini hanya menambahkan
	 * {@code service}/{@code menuId}; parameter {@code action}, paging, filter, dan
	 * field form dari POST semula tetap diteruskan container sebagaimana adanya.</li>
	 * </ol>
	 *
	 * <p>Semua kegagalan tak terduga ditangkap, dilaporkan lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}, dan dijawab {@code 500} dengan
	 * kode {@code INTERNAL_ERROR} bila respons belum ter-commit. Sesi Hibernate yang
	 * dibuka untuk memuat ulang role selalu ditutup di blok {@code finally}, termasuk
	 * thread-local session yang mungkin sudah ditutup oleh helper otentikasi. Berbeda
	 * dari {@link DesktopMenuBootstrap} yang mem-forward ke shell HTML, seluruh respons
	 * kegagalan servlet ini berupa JSON ({@link #writeError}) karena pemanggilnya
	 * adalah klien native/Flutter, bukan WebView yang merender halaman.</p>
	 *
	 * @param request permintaan HTTP POST berisi parameter {@code token}, {@code menuId},
	 *        dan {@code roleId} opsional, atau badan {@code multipart/form-data} yang
	 *        membawa field-field tersebut beserta berkas
	 * @param response respons HTTP; pada sukses berupa hasil forward JSON ke
	 *        {@code /new}, pada gagal berupa JSON {@code {success:false,code,message}}
	 * @throws ServletException diteruskan dari {@link javax.servlet.RequestDispatcher#forward}
	 * @throws IOException diteruskan dari forward, dari penulisan JSON kesalahan, atau
	 *         dari pengurai multipart
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Referrer-Policy", "no-referrer");
		response.setHeader("X-Content-Type-Options", "nosniff");

		// Permintaan yang membawa berkas datang sebagai multipart. Badannya diurai
		// di sini lalu dibungkus supaya seluruh lapisan di hilir -- otentikasi di
		// bawah, penjaga, index.jsp, controller -- tetap memanggil getParameter()
		// seperti biasa. Cabang ini hanya dimasuki bila tipe isinya memang
		// multipart, sehingga perilaku permintaan yang sudah ada tidak berubah.
		if (NewUiUnggahRequest.multipart(request)) {
			try {
				request = NewUiUnggahRequest.urai(request);
			} catch (IllegalArgumentException e) {
				writeError(response, HttpServletResponse.SC_BAD_REQUEST, "UPLOAD_INVALID",
						e.getMessage() == null ? "Berkas unggahan ditolak." : e.getMessage());
				return;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				writeError(response, HttpServletResponse.SC_BAD_REQUEST, "UPLOAD_INVALID",
						"Berkas unggahan tidak dapat dibaca. Pastikan ukurannya di bawah "
								+ (NewUiUnggahRequest.BATAS_UKURAN / (1024 * 1024)) + " MB.");
				return;
			}
		}

		Session db = null;
		try {
			String token = trim(request.getParameter("token"));
			Long menuId = parseLong(request.getParameter("menuId"));
			String roleId = trim(request.getParameter("roleId"));
			if (token.length() == 0 || menuId == null) {
				writeError(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_INVALID",
						"Token atau menu tidak lengkap.");
				return;
			}

			Tbmuser user = ApiUtil.currentUser(token);
			if (user == null || user.getUserId() == null) {
				writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_EXPIRED",
						"Sesi aplikasi sudah berakhir.");
				return;
			}

			Tbmrole selectedRole = findOwnedRole(user, roleId);
			if (selectedRole == null || selectedRole.getRoleId() == null) {
				writeError(response, HttpServletResponse.SC_FORBIDDEN, "ROLE_FORBIDDEN",
						"Role tidak tersedia untuk pengguna ini.");
				return;
			}

			db = HibernateUtil.getSessionFactory().openSession();
			Tbmrole roleDb = (Tbmrole) db.get(Tbmrole.class, selectedRole.getRoleId());
			if (roleDb == null) {
				writeError(response, HttpServletResponse.SC_FORBIDDEN, "ROLE_NOT_FOUND",
						"Role tidak ditemukan.");
				return;
			}

			List<Menu> menus = new ArrayList<Menu>();
			if (roleDb.getMenus() != null) menus.addAll(roleDb.getMenus());
			Collections.sort(menus);
			Menu requestedMenu = findAuthorizedLeaf(menus, menuId);
			if (requestedMenu == null) {
				writeError(response, HttpServletResponse.SC_FORBIDDEN, "MENU_FORBIDDEN",
						"Menu tidak tersedia atau bukan leaf menu untuk role aktif.");
				return;
			}

			// Samakan role aktif dengan HakAksesApi dan shell New UI.
			Tbmuser.getUserRoleYgDipakai.put(user.getUserId(), roleDb);
			HttpSession httpSession = request.getSession(true);
			httpSession.setAttribute("mytbmuser", user);
			httpSession.setAttribute("usersTemp", user);
			httpSession.setAttribute("user", user);
			httpSession.setAttribute("udah_tanya", Boolean.TRUE);
			httpSession.setAttribute("currentMenus", menus);
			httpSession.setAttribute("currentMenu", requestedMenu);
			httpSession.setAttribute("current_menus", menus);
			httpSession.setAttribute("current_menu", requestedMenu);

			// Query internal menambahkan service/menuId; parameter action, paging,
			// filter dan field form dari POST semula tetap diteruskan oleh container.
			request.getRequestDispatcher("/new?service=1&menuId=" + menuId).forward(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			if (!response.isCommitted()) {
				writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
						"Layanan native gagal memproses menu.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(db);
			try { HibernateUtil.closeSession(); } catch (Exception ignored) { }
		}
	}

	/**
	 * Menentukan role aktif yang SAH dipakai untuk permintaan ini, tanpa pernah
	 * memercayai begitu saja parameter {@code roleId} yang dikirim klien.
	 *
	 * <p>Implementasi identik dengan
	 * {@link DesktopMenuBootstrap#doPost}: bila {@code requestedRoleId} diisi,
	 * method ini mencari role tersebut di dalam daftar role MILIK pengguna
	 * ({@link Tbmuser#ambilRoles()}), bukan di tabel role global, sehingga ID yang
	 * bukan milik pengguna selalu menghasilkan {@code null}. Bila kosong, role aktif
	 * diambil dari {@link Tbmuser#hakAkses()} dengan fallback ke
	 * {@link Tbmuser#getUserRole()}.</p>
	 *
	 * @param user pengguna yang sudah diverifikasi lewat token; tidak boleh {@code null}
	 * @param requestedRoleId ID role yang diminta klien, boleh string kosong tetapi
	 *        tidak boleh {@code null} (lihat {@link #trim(String)} pada pemanggil)
	 * @return role milik pengguna yang cocok, atau {@code null} bila tidak ditemukan atau
	 *         tidak dimiliki pengguna
	 */
	private static Tbmrole findOwnedRole(Tbmuser user, String requestedRoleId) {
		List<Tbmrole> roles;
		try { roles = user.ambilRoles(); }
		catch (Exception ignored) { roles = new ArrayList<Tbmrole>(); }
		if (requestedRoleId.length() > 0) {
			for (Tbmrole role : roles) {
				if (role != null && role.getRoleId() != null
						&& requestedRoleId.equalsIgnoreCase(role.getRoleId())) return role;
			}
			return null;
		}
		try { return user.hakAkses(); }
		catch (Exception ignored) { return user.getUserRole(); }
	}

	/**
	 * Memverifikasi bahwa {@code menuId} yang diminta klien benar-benar merupakan
	 * menu yang DITAUTKAN ke role ini (relasi {@code job_has_menu}), masih aktif,
	 * memiliki URL yang bisa dibuka, dan merupakan leaf menu.
	 *
	 * <p>Implementasi ini SAMA PERSIS dengan
	 * {@link DesktopMenuBootstrap#findAuthorizedLeaf(List, Long)} -- lihat javadoc
	 * method tersebut untuk penjelasan lengkap algoritmenya langkah demi langkah,
	 * alasan mengapa pola ini menutup celah spoofing menu via parameter URL
	 * ({@code task_9f520b16} pada {@code DisplayMenu.java}), dan bagaimana pola yang
	 * sama dapat diterapkan di sana. Singkatnya: {@code menuId} hanya dipakai sebagai
	 * SELEKTOR di dalam koleksi {@code menus} yang sudah di-scope pada role aktif
	 * (hasil {@code roleDb.getMenus()}), bukan sebagai kunci pencarian bebas ke tabel
	 * {@code menu}, sehingga ID milik menu role lain tidak akan pernah cocok berapa
	 * pun nilainya.</p>
	 *
	 * <p>Duplikasi verbatim method ini di dua servlet (di sini dan di
	 * {@link DesktopMenuBootstrap}) adalah kandidat kuat untuk diekstrak menjadi
	 * satu utilitas RBAC/menu bersama, terutama karena pola yang sama juga perlu
	 * diterapkan pada {@code DisplayMenu.java} untuk menutup {@code task_9f520b16}
	 * -- lihat catatan pada javadoc {@link DesktopMenuBootstrap} untuk detailnya.</p>
	 *
	 * @param menus daftar menu milik role aktif yang sudah diverifikasi kepemilikannya
	 *        (hasil {@code roleDb.getMenus()} pada role yang dimuat segar dari basis
	 *        data) -- BUKAN daftar seluruh menu aplikasi
	 * @param menuId ID menu yang diminta klien; nilai ini hanya dipakai sebagai
	 *        selektor di dalam {@code menus}, tidak pernah untuk pencarian langsung
	 *        ke tabel menu
	 * @return entitas {@link Menu} yang aktif, memiliki URL, merupakan leaf, dan
	 *         tertaut ke role pemilik {@code menus}; atau {@code null} bila salah
	 *         satu syarat gagal
	 */
	private static Menu findAuthorizedLeaf(List<Menu> menus, Long menuId) {
		Menu target = null;
		for (Menu menu : menus) {
			if (isActive(menu) && menuId.equals(menu.getId())) { target = menu; break; }
		}
		if (target == null || !hasText(target.getUrl())) return null;
		for (Menu candidate : menus) {
			if (isActive(candidate) && target.getChild() != null
					&& target.getChild().equals(candidate.getRoot())) return null;
		}
		return target;
	}

	/**
	 * Memeriksa apakah menu memiliki ID dan berstatus aktif.
	 *
	 * <p>Nilai {@code null} pada {@link Menu#getAktif()} dianggap aktif (fail-open
	 * by design untuk data lama yang belum mengisi kolom status), sedangkan menu
	 * tanpa ID selalu dianggap tidak valid.</p>
	 *
	 * @param menu menu yang diperiksa, boleh {@code null}
	 * @return {@code true} bila menu tidak null, memiliki ID, dan aktif (atau status
	 *         aktifnya belum diisi)
	 */
	private static boolean isActive(Menu menu) {
		return menu != null && menu.getId() != null
				&& (menu.getAktif() == null || menu.getAktif().booleanValue());
	}
	/**
	 * Memeriksa apakah string tidak {@code null} dan memiliki isi selain spasi.
	 *
	 * @param value string yang diperiksa, boleh {@code null}
	 * @return {@code true} bila {@code value} tidak null dan panjang hasil
	 *         {@link String#trim()}-nya lebih dari nol
	 */
	private static boolean hasText(String value) { return value != null && value.trim().length() > 0; }
	/**
	 * Melakukan trim yang aman terhadap nilai {@code null}.
	 *
	 * @param value string yang akan di-trim, boleh {@code null}
	 * @return hasil {@link String#trim()}, atau string kosong bila {@code value} null
	 */
	private static String trim(String value) { return value == null ? "" : value.trim(); }
	/**
	 * Mengurai string menjadi {@link Long}, mengembalikan {@code null} bila gagal.
	 *
	 * <p>Dipakai untuk parameter {@code menuId} yang berasal dari input klien;
	 * kegagalan parse (format tidak valid, kosong, atau bukan angka) sengaja tidak
	 * melempar pengecualian ke pemanggil, cukup dianggap sebagai permintaan tidak
	 * lengkap.</p>
	 *
	 * @param value string yang diurai, boleh {@code null}
	 * @return nilai {@link Long} hasil parse, atau {@code null} bila {@code value}
	 *         null atau tidak dapat diurai
	 */
	private static Long parseLong(String value) {
		try { return Long.valueOf(trim(value)); } catch (Exception ignored) { return null; }
	}
	/**
	 * Menuliskan respons kegagalan berformat JSON seragam
	 * ({@code {"success":false,"code":...,"message":...}}) untuk seluruh jalur
	 * gagal pada servlet ini.
	 *
	 * <p>Kode ({@code code}) dimaksudkan untuk ditangani secara programatik oleh
	 * klien Flutter (mis. membedakan {@code TOKEN_EXPIRED} yang perlu login ulang
	 * dari {@code MENU_FORBIDDEN} yang perlu kembali ke daftar menu), sedangkan
	 * {@code message} adalah teks yang bisa ditampilkan langsung ke pengguna.
	 * Nilai {@code code} dan {@code message} di-escape lewat {@link #json(String)}
	 * agar karakter kutip/backslash/baris baru pada pesan tidak merusak struktur
	 * JSON.</p>
	 *
	 * @param response respons HTTP yang menerima status dan badan JSON
	 * @param status kode status HTTP yang dipasang (mis. 400, 401, 403, 500)
	 * @param code kode kesalahan pendek yang stabil untuk ditangani klien
	 * @param message pesan yang aman ditampilkan ke pengguna
	 * @throws IOException bila penulisan badan JSON ke respons gagal
	 */
	private static void writeError(HttpServletResponse response, int status, String code, String message)
			throws IOException {
		response.setStatus(status);
		response.setContentType("application/json; charset=UTF-8");
		response.getWriter().write("{\"success\":false,\"code\":\"" + json(code)
				+ "\",\"message\":\"" + json(message) + "\"}");
	}
	/**
	 * Meng-escape string agar aman disisipkan sebagai nilai string literal JSON.
	 *
	 * <p>Meng-escape backslash, tanda kutip ganda, carriage return, dan line feed.
	 * Ini BUKAN encoder JSON lengkap (tidak menangani karakter kontrol lain atau
	 * unicode di luar rentang umum), tetapi cukup untuk pesan kesalahan berbahasa
	 * Indonesia biasa yang ditulis {@link #writeError}.</p>
	 *
	 * @param value string yang akan di-escape, boleh {@code null}
	 * @return string yang aman disisipkan di antara tanda kutip JSON, atau string
	 *         kosong bila {@code value} null
	 */
	private static String json(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
				.replace("\r", "\\r").replace("\n", "\\n");
	}
}
