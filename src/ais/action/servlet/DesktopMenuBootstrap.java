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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Jembatan aman dari aplikasi desktop ke shell ZK.
 *
 * <p>Token hanya diterima melalui POST dan tidak pernah ditempelkan pada URL.
 * Servlet memvalidasi ulang bahwa menu berasal dari relasi job_has_menu milik
 * role aktif, masih aktif, dan merupakan leaf. Setelah sesi HTTP/ZK terbentuk,
 * MainAction membuka menu dengan Common.launchMenu sehingga seluruh Action.java,
 * ZUL, privilege, popup, dan laporan memakai implementasi web yang sama.</p>
 */
public class DesktopMenuBootstrap extends HttpServlet {

	/**
	 * ID versi serialisasi tetap untuk kompatibilitas antar versi kelas servlet ini.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Nama atribut sesi HTTP yang menyimpan ID menu yang baru saja diotorisasi dan
	 * diteruskan ke {@code /main}, sehingga MainAction dapat mengonfirmasi menu mana
	 * yang hendak dibuka oleh alur desktop tanpa membaca ulang parameter permintaan.
	 */
	public static final String ATTR_PENDING_MENU_ID = "desktopPendingMenuId";
	/**
	 * Nama atribut sesi HTTP yang menyimpan ID role yang telah diverifikasi sebagai
	 * milik pengguna, dipasang berdampingan dengan {@link #ATTR_PENDING_MENU_ID}
	 * agar MainAction memakai role aktif yang sama dengan yang divalidasi servlet ini.
	 */
	public static final String ATTR_PENDING_ROLE_ID = "desktopPendingRoleId";

	/**
	 * Menolak semua permintaan GET.
	 *
	 * <p>Token otentikasi tidak pernah boleh muncul pada query string URL karena akan
	 * tersimpan di riwayat browser, log server, dan header Referer. Endpoint ini hanya
	 * menerima POST (lihat {@link #doPost}); permintaan GET selalu dijawab
	 * {@link HttpServletResponse#SC_METHOD_NOT_ALLOWED}.</p>
	 *
	 * @param request permintaan HTTP masuk (tidak dipakai selain untuk kontrak servlet)
	 * @param response respons HTTP; diisi status 405 dan pesan penjelasan
	 * @throws ServletException tidak pernah dilempar oleh implementasi ini
	 * @throws IOException bila penulisan pesan kesalahan ke respons gagal
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
				"Gunakan aplikasi desktop untuk membuka menu ini.");
	}

	/**
	 * Memvalidasi token, role, dan menu aplikasi desktop, lalu membangun sesi ZK dan
	 * meneruskan permintaan ke {@code /main} agar shell web menampilkan menu yang sama
	 * seperti yang dipakai pengguna berbasis browser.
	 *
	 * <p>Urutan validasi:</p>
	 * <ol>
	 * <li>Header cache/referrer diset lebih dulu agar token dan detail sesi tidak ikut
	 * ter-cache oleh proxy atau tersimpan di riwayat browser.</li>
	 * <li>Parameter {@code token}, {@code menuId}, dan {@code roleId} dibaca dan diperiksa
	 * kelengkapannya; kekurangan salah satu dari {@code token}/{@code menuId} dijawab
	 * {@code 400 Bad Request}.</li>
	 * <li>Token diverifikasi lewat {@link ApiUtil#currentUser(String)}. Token menjadi
	 * satu-satunya sumber identitas -- sengaja TIDAK mengandalkan cookie WebView yang
	 * mungkin masih menyimpan user lama bila aplikasi desktop baru saja berganti akun.
	 * Token tidak valid/kedaluwarsa dijawab {@code 401 Unauthorized}.</li>
	 * <li>Role yang diminta diverifikasi kepemilikannya lewat
	 * {@link #findOwnedRole(Tbmuser, String)}; role yang bukan milik pengguna dijawab
	 * {@code 403 Forbidden}.</li>
	 * <li>Role dimuat ulang dari basis data (bukan dari objek session/token) agar daftar
	 * menunya terbaru, lalu menu yang diminta diverifikasi lewat
	 * {@link #findAuthorizedLeaf(List, Long)} -- lihat javadoc method tersebut untuk
	 * penjelasan lengkap kenapa validasi ini menutup celah spoofing menu lewat parameter
	 * URL. Menu yang tidak ditemukan/tidak aktif/bukan leaf menu dijawab
	 * {@code 403 Forbidden}.</li>
	 * <li>Setelah lolos semua penjaga, sesi HTTP baru dibuat dan atribut ZK
	 * ({@code mytbmuser}, {@code usersTemp}, {@code user}, {@code currentMenus},
	 * {@code currentMenu}, dst.) dipasang, serta map role aktif thread-safe
	 * {@link Tbmuser#getUserRoleYgDipakai} disamakan dengan role yang baru diverifikasi
	 * supaya {@code hakAkses()} di hilir konsisten dengan hasil validasi servlet ini.</li>
	 * <li>Permintaan di-forward secara internal ke {@code /main}. Forward internal ini
	 * melewati pemeriksaan principal Spring Security yang biasanya menjaga endpoint
	 * tersebut, tetapi tetap aman karena sesi yang dipakai sudah diautentikasi ulang
	 * dengan token di atas pada langkah-langkah sebelumnya; request berikutnya (ZK
	 * AU/ZUL) membawa JSESSIONID yang sama.</li>
	 * </ol>
	 *
	 * <p>Semua kegagalan tak terduga ditangkap, dilaporkan lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}, dan dijawab {@code 500} bila respons
	 * belum ter-commit. Sesi Hibernate yang dibuka untuk memuat ulang role selalu ditutup
	 * di blok {@code finally}, termasuk thread-local session yang mungkin sudah ditutup
	 * oleh helper otentikasi.</p>
	 *
	 * @param request permintaan HTTP POST berisi parameter {@code token}, {@code menuId},
	 *        dan {@code roleId} opsional
	 * @param response respons HTTP; pada sukses berupa hasil forward ke {@code /main},
	 *        pada gagal berupa kode status dan pesan kesalahan
	 * @throws ServletException diteruskan dari
	 *         {@link javax.servlet.RequestDispatcher#forward}
	 * @throws IOException bila penulisan respons atau forward gagal karena I/O
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Referrer-Policy", "no-referrer");

		Session db = null;
		try {
			String token = trim(request.getParameter("token"));
			Long menuId = parseLong(request.getParameter("menuId"));
			String roleId = trim(request.getParameter("roleId"));
			if (token.length() == 0 || menuId == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Token atau menu tidak lengkap.");
				return;
			}

			// Token harus menjadi sumber identitas; jangan menerima user lama dari
			// cookie WebView bila aplikasi desktop baru saja berganti akun.
			Tbmuser user = ApiUtil.currentUser(token);
			if (user == null || user.getUserId() == null) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sesi aplikasi sudah berakhir.");
				return;
			}

			Tbmrole selectedRole = findOwnedRole(user, roleId);
			if (selectedRole == null || selectedRole.getRoleId() == null) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Role tidak tersedia untuk pengguna ini.");
				return;
			}

			db = HibernateUtil.getSessionFactory().openSession();
			Tbmrole roleDb = (Tbmrole) db.get(Tbmrole.class, selectedRole.getRoleId());
			if (roleDb == null) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Role tidak ditemukan.");
				return;
			}

			List<Menu> menus = new ArrayList<Menu>();
			if (roleDb.getMenus() != null) {
				menus.addAll(roleDb.getMenus());
			}
			Collections.sort(menus);
			Menu requestedMenu = findAuthorizedLeaf(menus, menuId);
			if (requestedMenu == null) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN,
						"Menu tidak tersedia atau bukan leaf menu untuk role aktif.");
				return;
			}

			// Samakan role aktif yang dipakai hakAkses() dengan role pada respons API.
			Tbmuser.getUserRoleYgDipakai.put(user.getUserId(), roleDb);

			HttpSession httpSession = request.getSession(true);
			httpSession.setAttribute("mytbmuser", user);
			httpSession.setAttribute("usersTemp", user);
			httpSession.setAttribute("user", user);
			httpSession.setAttribute("udah_tanya", Boolean.TRUE);
			httpSession.setAttribute("currentMenus", menus);
			httpSession.setAttribute("currentMenu", requestedMenu);
			httpSession.setAttribute(ATTR_PENDING_MENU_ID, requestedMenu.getId());
			httpSession.setAttribute(ATTR_PENDING_ROLE_ID, roleDb.getRoleId());

			// Forward internal melewati pemeriksaan principal Spring pada /main, tetapi
			// tetap memakai sesi yang baru diautentikasi dengan token di atas. Request
			// berikutnya (ZK AU/ZUL) membawa JSESSIONID yang sama.
			request.setAttribute("desktop", "true");
			request.setAttribute("versilama", "true");
			request.getRequestDispatcher("/main").forward(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			if (!response.isCommitted()) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Menu web gagal disiapkan. Silakan coba kembali.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(db);
			try {
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// Thread-local session boleh sudah ditutup oleh helper autentikasi.
			}
		}
	}

	/**
	 * Menentukan role aktif yang SAH dipakai untuk permintaan ini, tanpa pernah
	 * memercayai begitu saja parameter {@code roleId} yang dikirim klien.
	 *
	 * <p>Bila {@code requestedRoleId} diisi, method ini mencari role tersebut di dalam
	 * daftar role MILIK pengguna ({@link Tbmuser#ambilRoles()}) -- bukan mencarinya di
	 * tabel role global. ID yang tidak ada dalam daftar role pengguna menghasilkan
	 * {@code null}, sehingga pemanggil (lihat {@link #doPost}) menjawabnya dengan
	 * {@code 403 Forbidden}. Dengan pola ini klien tidak dapat menyamar sebagai role
	 * milik pengguna lain hanya dengan mengganti nilai parameter {@code roleId}.</p>
	 *
	 * <p>Bila {@code requestedRoleId} kosong, role aktif diambil dari
	 * {@link Tbmuser#hakAkses()} (role yang sedang dipakai pengguna di sesi web bila
	 * ada), dengan fallback ke {@link Tbmuser#getUserRole()} bila {@code hakAkses()}
	 * melempar pengecualian.</p>
	 *
	 * @param user pengguna yang sudah diverifikasi lewat token; tidak boleh {@code null}
	 * @param requestedRoleId ID role yang diminta klien, boleh string kosong tetapi
	 *        tidak boleh {@code null} (lihat {@link #trim(String)} pada pemanggil)
	 * @return role milik pengguna yang cocok, atau {@code null} bila tidak ditemukan atau
	 *         tidak dimiliki pengguna
	 */
	private static Tbmrole findOwnedRole(Tbmuser user, String requestedRoleId) {
		List<Tbmrole> roles;
		try {
			roles = user.ambilRoles();
		} catch (Exception e) {
			roles = new ArrayList<Tbmrole>();
		}
		if (requestedRoleId.length() > 0) {
			for (Tbmrole role : roles) {
				if (role != null && role.getRoleId() != null
						&& requestedRoleId.equalsIgnoreCase(role.getRoleId())) {
					return role;
				}
			}
			return null;
		}
		try {
			return user.hakAkses();
		} catch (Exception e) {
			return user.getUserRole();
		}
	}

	/**
	 * Memverifikasi bahwa {@code menuId} yang diminta klien benar-benar merupakan menu
	 * yang DITAUTKAN ke role ini (relasi {@code job_has_menu}), masih aktif, memiliki URL
	 * yang bisa dibuka, dan merupakan leaf menu -- lalu mengembalikan entitas {@link Menu}
	 * tersebut, atau {@code null} bila salah satu syarat gagal.
	 *
	 * <p><b>Ini adalah pola referensi untuk menutup celah spoofing menu via parameter URL</b>
	 * (dicatat sebagai temuan {@code task_9f520b16} pada {@code DisplayMenu.java}, yang saat
	 * dokumentasi ini ditulis MASIH menerima {@code ?menu=<id>} tanpa memverifikasi
	 * kepemilikan menu terhadap role aktif). Method yang identik juga sudah diterapkan pada
	 * {@link DesktopNativeApi} (lihat method privat dengan nama sama di kelas tersebut),
	 * sehingga pola ini sudah dua kali diverifikasi bekerja pada dua servlet berbeda.</p>
	 *
	 * <h3>Algoritma, langkah demi langkah</h3>
	 * <ol>
	 * <li><b>Pencarian target di dalam koleksi yang SUDAH di-scope oleh role.</b> Parameter
	 * {@code menus} yang diterima method ini BUKAN daftar seluruh menu aplikasi, melainkan
	 * hasil {@code roleDb.getMenus()} -- koleksi yang diisi Hibernate persis sesuai baris
	 * tabel {@code job_has_menu} milik role yang sudah diverifikasi kepemilikannya oleh
	 * {@link #findOwnedRole(Tbmuser, String)} dan dimuat ulang segar dari basis data oleh
	 * pemanggil (bukan dari objek session/token yang bisa basi). Method ini melakukan
	 * iterasi linear pada koleksi tersebut mencari menu yang aktif ({@link #isActive(Menu)})
	 * DAN ID-nya sama dengan {@code menuId} permintaan. Bila tidak ketemu, {@code target}
	 * tetap {@code null} dan method mengembalikan {@code null} pada langkah berikutnya.</li>
	 * <li><b>Penolakan menu tanpa URL.</b> Bila {@code target} ditemukan tetapi
	 * {@link Menu#getUrl()}-nya kosong ({@link #hasText(String)} gagal), method tetap
	 * mengembalikan {@code null}. Menu semacam ini biasanya adalah node judul/kelompok pada
	 * pohon menu ZK yang tidak terikat ke Action apa pun; membukanya secara langsung tidak
	 * bermakna dan berisiko jatuh ke halaman default yang tidak dimaksud.</li>
	 * <li><b>Penolakan menu non-leaf (menu induk/kelompok).</b> Method melakukan iterasi
	 * kedua atas koleksi menu yang sama, mencari apakah ada {@code candidate} aktif yang
	 * {@link Menu#getRoot()}-nya sama dengan {@link Menu#getChild()} milik {@code target}.
	 * Skema {@code root}/{@code child} pada entitas {@link Menu} merepresentasikan struktur
	 * pohon menu (mirip pola nested-set/path segment): setiap menu anak menyimpan penanda
	 * {@code root} yang menunjuk ke penanda {@code child} milik induknya. Jika ditemukan
	 * kandidat seperti itu, artinya {@code target} punya turunan pada daftar menu role ini
	 * sehingga {@code target} adalah cabang/kelompok, bukan leaf, dan method mengembalikan
	 * {@code null}. Hanya leaf menu -- yang di ZK adalah satu-satunya jenis node yang bisa
	 * diklik langsung oleh pengguna -- yang boleh dibuka lewat jalur ini.</li>
	 * <li><b>Bila semua pemeriksaan lolos</b>, {@code target} dikembalikan sebagai menu yang
	 * sah untuk dibuka.</li>
	 * </ol>
	 *
	 * <h3>Mengapa pola ini benar (dan mengapa alternatifnya salah)</h3>
	 * <p>Pendekatan yang RENTAN -- dan diduga inilah yang masih dipakai
	 * {@code DisplayMenu.java} pada {@code task_9f520b16} -- adalah memuat entitas
	 * {@link Menu} langsung dari primary key memakai parameter mentah, misalnya
	 * {@code db.get(Menu.class, menuId)} atau query berdasarkan ID tanpa klausa yang
	 * membatasi pada role aktif. ID pada tabel {@code menu} kemungkinan besar berurutan
	 * dan dapat ditebak/dienumerasi. Dengan pola tersebut, pengguna yang login dengan role
	 * berprivilese rendah dapat mengganti nilai parameter {@code menu}/{@code menuId} pada
	 * URL menjadi ID milik menu yang seharusnya hanya ditautkan ke role administratif, dan
	 * servlet akan tetap memuat serta membuka menu tersebut karena validasinya berhenti di
	 * "apakah baris ini ada di tabel menu", bukan "apakah baris ini ditautkan ke role yang
	 * sedang dipakai pengguna ini". Ini adalah pola klasik broken access control/IDOR:
	 * parameter yang dikendalikan klien dipakai sebagai kunci pencarian langsung ke data
	 * yang seharusnya di-gate oleh kepemilikan, bukan sekadar eksistensi baris.</p>
	 * <p>{@code findAuthorizedLeaf} menutup celah itu dengan mengubah {@code menuId} dari
	 * "kunci pencarian bebas" menjadi sekadar SELEKTOR di dalam himpunan yang sudah
	 * diotorisasi terlebih dahulu ({@code roleDb.getMenus()}). ID yang tidak ada dalam
	 * himpunan itu tidak akan pernah ditemukan, berapa pun nilainya, karena pencarian tidak
	 * pernah menyentuh tabel {@code menu} secara independen -- ia hanya menyaring koleksi
	 * yang sudah dibatasi oleh relasi {@code job_has_menu} milik role yang telah diverifikasi
	 * kepemilikannya oleh {@link #findOwnedRole(Tbmuser, String)}. Tambahan pemeriksaan
	 * status aktif dan larangan menu non-leaf memastikan bahkan menu yang memang tertaut ke
	 * role ini pun tidak bisa disalahgunakan bila sedang dinonaktifkan admin atau bila menu
	 * itu sebetulnya cuma node kelompok yang di ZK tidak pernah bisa diklik langsung.</p>
	 *
	 * <h3>Cara menerapkan pola ini ke {@code DisplayMenu.java} untuk menutup task_9f520b16</h3>
	 * <p>Perbaikan yang disarankan (TIDAK dieksekusi oleh dokumentasi ini, hanya dicatat
	 * sebagai referensi -- lihat catatan proyek {@code task_9f520b16}):</p>
	 * <ol>
	 * <li>Tentukan role aktif pengguna yang sedang login dengan cara yang SAMA seperti di
	 * sini: {@link Tbmuser#hakAkses()} atau {@link Tbmuser#getUserRoleYgDipakai}, BUKAN
	 * dari parameter request.</li>
	 * <li>Muat ulang role tersebut dari basis data ({@code db.get(Tbmrole.class, roleId)})
	 * agar {@code getMenus()} yang dipakai untuk validasi selalu segar, bukan koleksi
	 * detached lama yang mungkin tertinggal di objek session.</li>
	 * <li>Panggil logika yang sama persis dengan {@code findAuthorizedLeaf(menus, menuId)}
	 * terhadap koleksi menu role tersebut dan parameter {@code menu}/{@code menuId} yang
	 * dikirim klien. Karena method ini sudah diduplikasi identik di dua servlet
	 * ({@link DesktopMenuBootstrap} dan {@link DesktopNativeApi}), sebaiknya diekstrak
	 * menjadi satu utilitas bersama (mis. method statis di kelas util RBAC/menu) supaya
	 * {@code DisplayMenu.java} memanggil implementasi yang SAMA dan sudah teruji, alih-alih
	 * menulis salinan ketiga yang berisiko menyimpang seperti pola salin-tempel yang
	 * berulang kali ditemukan pada codebase ini.</li>
	 * <li>Bila hasilnya {@code null}, jawab {@code 403 Forbidden} PERSIS seperti di
	 * {@link #doPost} -- jangan lanjutkan merender/membuka menu, dan jangan fallback diam-diam
	 * ke menu lain.</li>
	 * <li>Jangan pernah memakai parameter {@code menu}/{@code menuId} sebagai kunci pencarian
	 * independen ke tabel {@code menu}; ia hanya boleh dipakai sebagai selektor di dalam
	 * koleksi menu role yang sudah dimuat dan diverifikasi kepemilikannya.</li>
	 * </ol>
	 *
	 * @param menus daftar menu milik role aktif yang sudah diverifikasi kepemilikannya
	 *        (hasil {@code roleDb.getMenus()} pada role yang dimuat segar dari basis data)
	 *        -- BUKAN daftar seluruh menu aplikasi
	 * @param menuId ID menu yang diminta klien; nilai ini hanya dipakai sebagai selektor di
	 *        dalam {@code menus}, tidak pernah untuk pencarian langsung ke tabel menu
	 * @return entitas {@link Menu} yang aktif, memiliki URL, merupakan leaf, dan tertaut ke
	 *         role pemilik {@code menus}; atau {@code null} bila salah satu syarat gagal
	 */
	private static Menu findAuthorizedLeaf(List<Menu> menus, Long menuId) {
		Menu target = null;
		for (Menu menu : menus) {
			if (isActive(menu) && menuId.equals(menu.getId())) {
				target = menu;
				break;
			}
		}
		if (target == null || !hasText(target.getUrl())) {
			return null;
		}
		for (Menu candidate : menus) {
			if (isActive(candidate) && candidate.getRoot() != null && target.getChild() != null
					&& candidate.getRoot().equals(target.getChild())) {
				return null;
			}
		}
		return target;
	}

	/**
	 * Memeriksa apakah menu memiliki ID dan berstatus aktif.
	 *
	 * <p>Nilai {@code null} pada {@link Menu#getAktif()} dianggap aktif (fail-open by
	 * design untuk data lama yang belum mengisi kolom status), sedangkan menu tanpa ID
	 * selalu dianggap tidak valid.</p>
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
	private static boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}

	/**
	 * Melakukan trim yang aman terhadap nilai {@code null}.
	 *
	 * @param value string yang akan di-trim, boleh {@code null}
	 * @return hasil {@link String#trim()}, atau string kosong bila {@code value} null
	 */
	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}

	/**
	 * Mengurai string menjadi {@link Long}, mengembalikan {@code null} bila gagal.
	 *
	 * <p>Dipakai untuk parameter {@code menuId} yang berasal dari input klien; kegagalan
	 * parse (format tidak valid, kosong, atau bukan angka) sengaja tidak melempar
	 * pengecualian ke pemanggil, cukup dianggap sebagai permintaan tidak lengkap.</p>
	 *
	 * @param value string yang diurai, boleh {@code null}
	 * @return nilai {@link Long} hasil parse, atau {@code null} bila {@code value}
	 *         null atau tidak dapat diurai
	 */
	private static Long parseLong(String value) {
		try {
			return Long.valueOf(trim(value));
		} catch (Exception e) {
			return null;
		}
	}
}
