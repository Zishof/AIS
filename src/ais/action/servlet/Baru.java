package ais.action.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Servlet routing pasca-login untuk shell "baru" (UI generasi baru berbasis JSP,
 * bukan ZK), dipetakan pada URL {@code /baru}.
 *
 * <p>Alur: {@link #initCommonUrl(HttpServletRequest)} menyiapkan variabel statis
 * global {@link Common} yang dipakai kode lain, lalu
 * {@link Main#checkAndSetUserSession(HttpServletRequest, boolean)} memastikan sesi
 * pengguna valid (mengambil {@code tbmuser} dari sesi, atau melakukan redirect ke
 * halaman login bila belum login) sebelum
 * {@link #resolveDispatcher(HttpServletRequest, Tbmuser)} menentukan JSP tujuan
 * berdasarkan empat prioritas yang dievaluasi berurutan:</p>
 * <ol>
 * <li>Parameter {@code p=apotik&s=service} -- ke halaman service apotik.</li>
 * <li>Anggota koperasi/member biasa (bukan pedagang, bukan admin lain, dengan
 * konfigurasi terkait aktif) -- ke halaman landing member kantin.</li>
 * <li>Parameter {@code p=kantin} -- ke index shell baru.</li>
 * <li>Permintaan eksplisit {@code hak_akses=true}, atau user memiliki lebih dari
 * satu role dan belum pernah ditanya pada sesi ini -- ke halaman pilih hak
 * akses.</li>
 * </ol>
 * <p>Bila tidak ada prioritas yang cocok, fallback ke index shell baru
 * ({@link #PAGE_MAIN}).</p>
 */
public class Baru extends HttpServlet {

	/**
	 * ID versi serialisasi tetap untuk kompatibilitas antar versi kelas servlet ini.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Halaman fallback default bila tidak ada prioritas routing lain yang cocok.
	 * Nilainya sama dengan {@link #PAGE_BARU_INDEX}; keduanya dipertahankan sebagai
	 * konstanta terpisah karena mewakili keputusan routing yang berbeda secara makna
	 * meski kebetulan menunjuk ke JSP yang sama.
	 */
	private static final String PAGE_MAIN = "/WEB-INF/baru/index.jsp";
	/**
	 * Index shell baru, dituju ketika permintaan membawa parameter {@code p=kantin}
	 * (lihat {@link #isParameterKantin(HttpServletRequest)}).
	 */
	private static final String PAGE_BARU_INDEX = "/WEB-INF/baru/index.jsp";
	/**
	 * Halaman pilih hak akses/role, dituju bila diminta eksplisit lewat parameter
	 * {@code hak_akses=true} atau bila user memiliki lebih dari satu role dan belum
	 * pernah ditanya pada sesi berjalan.
	 */
	private static final String PAGE_HAK_AKSES = "/WEB-INF/baru/modul/common/hak_akses.jsp";
	/**
	 * Halaman landing member kantin/koperasi, dituju bila
	 * {@link #harusKeHalamanMember(Tbmuser)} bernilai true.
	 */
	private static final String PAGE_MEMBER = "/WEB-INF/baru/modul/kantin/index.jsp";
	/**
	 * Halaman service apotik, dituju bila parameter request adalah
	 * {@code p=apotik&s=service}. Prioritas tertinggi pada
	 * {@link #resolveDispatcher(HttpServletRequest, Tbmuser)}, dievaluasi sebelum
	 * status login/role diperiksa.
	 */
	private static final String PAGE_APOTIK_SERVICE = "/WEB-INF/baru/modul/apotik/service.jsp";

	/**
	 * Konstruktor default tanpa argumen, dipanggil kontainer servlet saat instansiasi.
	 */
	public Baru() {
		super();
	}

	/**
	 * Menangani permintaan GET dengan mendelegasikan ke
	 * {@link #handleRequest(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP yang akan diisi hasil forward
	 * @throws ServletException diteruskan dari {@link #handleRequest}
	 * @throws IOException diteruskan dari {@link #handleRequest}
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		handleRequest(request, response);
	}

	/**
	 * Menangani permintaan POST dengan perilaku yang sama persis dengan
	 * {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP yang akan diisi hasil forward
	 * @throws ServletException diteruskan dari {@link #handleRequest}
	 * @throws IOException diteruskan dari {@link #handleRequest}
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		handleRequest(request, response);
	}

	/**
	 * Membungkus {@link #process(HttpServletRequest, HttpServletResponse)} dengan
	 * penanganan kesalahan terpusat.
	 *
	 * <p>Kesalahan selalu dilaporkan lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)}. Bila respons belum ter-commit,
	 * kesalahan juga dibungkus ulang sebagai {@link ServletException} agar kontainer
	 * servlet menampilkan halaman error standar; bila respons sudah ter-commit (mis.
	 * sebagian output sudah terkirim), pelemparan ulang dilewati untuk menghindari
	 * "IllegalStateException: getOutputStream() has already been called" atau respons
	 * yang campur aduk.</p>
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP yang akan diisi hasil forward
	 * @throws ServletException dibungkus dari kesalahan apa pun pada
	 *         {@link #process}, hanya bila respons belum ter-commit
	 * @throws IOException tidak dilempar langsung oleh method ini; disertakan pada
	 *         signature untuk mengikuti kontrak servlet
	 */
	private void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);

			if (!response.isCommitted()) {
				throw new ServletException(e);
			}
		}
	}

	/**
	 * Menyiapkan konteks URL, memastikan sesi pengguna, lalu mem-forward ke JSP hasil
	 * {@link #resolveDispatcher(HttpServletRequest, Tbmuser)}.
	 *
	 * <p>Gerbang otentikasi ada di
	 * {@link Main#checkAndSetUserSession(HttpServletRequest, boolean)}: bila belum
	 * login, method tersebut yang menangani redirect ke halaman login dan meng-commit
	 * respons -- method ini hanya memeriksa {@link HttpServletResponse#isCommitted()}
	 * sesudahnya dan berhenti tanpa melanjutkan forward bila demikian. Komentar pada
	 * kode aslinya mencatat bahwa versi lama sempat memakai variabel {@code tbmuser}
	 * sebelum dideklarasikan; urutan pemeriksaan pada method ini sengaja menempatkan
	 * pengecekan sesi sebelum {@code tbmuser} dipakai di percabangan manapun.</p>
	 *
	 * @param request permintaan HTTP masuk
	 * @param response respons HTTP; bisa sudah ter-commit sekembalinya dari
	 *         pemeriksaan sesi, dalam hal ini forward dibatalkan
	 * @throws Exception diteruskan dari pemeriksaan sesi, resolusi dispatcher, atau
	 *         forward JSP
	 */
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		initCommonUrl(request);

		/*
		 * User wajib dicek dulu sebelum dipakai.
		 * Pada kode lama, tbmuser dipakai sebelum dideklarasikan.
		 */
		Tbmuser tbmuser = Main.checkAndSetUserSession(request, true);

		if (response.isCommitted()) {
			return;
		}

		String dispatcher = resolveDispatcher(request, tbmuser);

		if (!response.isCommitted()) {
			request.getRequestDispatcher(dispatcher).forward(request, response);
		}
	}

	/**
	 * Menginisialisasi variabel statis global {@link Common} (REAL_PATH, ROOT,
	 * CURRENT_URL, dst.) dari request saat ini.
	 *
	 * <p>Penulisan {@code Common.CURRENT_URL}/{@code Common.CURRENT_URL_SIMPLE} dari
	 * protokol, nama host, dan port permintaan digerbangi oleh
	 * {@link Common#sanitizedRequestHostForCurrentUrl(HttpServletRequest)}: bila host
	 * pada permintaan tidak lolos validasi (format tidak valid, atau tidak ada pada
	 * allowlist konfigurasi bila diaktifkan), kedua variabel tersebut TIDAK ditimpa,
	 * sehingga nilai lama tetap dipakai alih-alih ikut ditentukan header {@code Host}
	 * yang bisa dipalsukan klien.</p>
	 *
	 * @param request permintaan HTTP yang menjadi sumber protokol, host, dan port
	 */
	private void initCommonUrl(HttpServletRequest request) {
		Common.REAL_PATH = getServletContext().getRealPath("/");
		Common.REAL_PATH_REPORT_TEMP = getServletContext().getRealPath("/report");
		Common.ROOT = request.getContextPath();

		String protocol = Common.isSecure(request) ? "https://" : "http://";
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();

		String port = "";
		if (serverPort != 80 && serverPort != 443) {
			port = ":" + serverPort;
		}

		if (Common.sanitizedRequestHostForCurrentUrl(request) != null) {
			Common.CURRENT_URL_SIMPLE = protocol + serverName + port;
			Common.CURRENT_URL = Common.CURRENT_URL_SIMPLE + request.getContextPath();
		}
	}

	/**
	 * Menentukan path JSP tujuan forward berdasarkan empat prioritas berurutan, lihat
	 * Javadoc kelas ({@link Baru}) untuk ringkasannya secara utuh.
	 *
	 * <p>Prioritas dievaluasi dari yang paling spesifik (parameter service apotik)
	 * hingga yang paling umum (fallback {@link #PAGE_MAIN}); prioritas pertama yang
	 * cocok langsung dikembalikan tanpa memeriksa prioritas berikutnya.</p>
	 *
	 * @param request permintaan HTTP, sumber parameter {@code p}, {@code s}, dan
	 *         {@code hak_akses}
	 * @param tbmuser pengguna yang sudah divalidasi sesinya oleh
	 *         {@link Main#checkAndSetUserSession(HttpServletRequest, boolean)}, boleh
	 *         {@code null} bila sesi tidak mensyaratkan login untuk path ini
	 * @return path JSP internal tujuan forward, tidak pernah {@code null}
	 * @throws Exception diteruskan dari {@link #harusKeHalamanMember(Tbmuser)} atau
	 *         {@link #harusTanyaHakAkses(HttpServletRequest, Tbmuser)}
	 */
	private String resolveDispatcher(HttpServletRequest request, Tbmuser tbmuser) throws Exception {
		if ("apotik".equalsIgnoreCase(request.getParameter("p"))
				&& "service".equalsIgnoreCase(request.getParameter("s"))) {
			return PAGE_APOTIK_SERVICE;
		}
		/*
		 * Prioritas 1:
		 * Jika user adalah anggota koperasi/member biasa,
		 * bukan pedagang, bukan admin lain, dan konfigurasi aktif,
		 * langsung arahkan ke halaman landing member.
		 */
		if (tbmuser != null && harusKeHalamanMember(tbmuser)) {
			return PAGE_MEMBER;
		}

		/*
		 * Prioritas 2:
		 * Jika bukan kondisi harusKeHalamanMember(),
		 * lalu terdapat parameter p=kantin,
		 * maka selalu arahkan ke /WEB-INF/baru/index.jsp.
		 *
		 * Contoh:
		 * /baru?p=kantin&s=ringkasan
		 */
		if (isParameterKantin(request)) {
			return PAGE_BARU_INDEX;
		}

		/*
		 * Prioritas 3:
		 * Jika request memang meminta halaman pilih hak akses.
		 */
		if (isRequestHakAkses(request)) {
			return PAGE_HAK_AKSES;
		}

		/*
		 * Prioritas 4:
		 * Jika user punya lebih dari 1 role dan belum pernah ditanya,
		 * arahkan ke halaman pilih hak akses.
		 */
		if (tbmuser != null && harusTanyaHakAkses(request, tbmuser)) {
			return PAGE_HAK_AKSES;
		}

		return PAGE_MAIN;
	}

	/**
	 * Memeriksa apakah parameter {@code p} bernilai {@code kantin}.
	 *
	 * @param request permintaan HTTP yang diperiksa
	 * @return {@code true} bila parameter {@code p} sama (case-insensitive) dengan
	 *         {@code "kantin"}
	 */
	private boolean isParameterKantin(HttpServletRequest request) {
		return "kantin".equalsIgnoreCase(request.getParameter("p"));
	}

	/**
	 * Memeriksa apakah permintaan secara eksplisit meminta halaman pilih hak akses.
	 *
	 * @param request permintaan HTTP yang diperiksa
	 * @return {@code true} bila parameter {@code hak_akses} sama (case-insensitive)
	 *         dengan {@code "true"}
	 */
	private boolean isRequestHakAkses(HttpServletRequest request) {
		return "true".equalsIgnoreCase(request.getParameter("hak_akses"));
	}

	/**
	 * Menentukan apakah pengguna perlu diarahkan memilih hak akses/role terlebih
	 * dahulu karena memiliki lebih dari satu role dan belum pernah ditanya pada sesi
	 * berjalan.
	 *
	 * <p>Atribut sesi {@code udah_tanya} dipakai sebagai penanda "sudah pernah
	 * ditanya" pada sesi ini, sehingga pengguna tidak diminta memilih role berulang
	 * kali selama sesi yang sama berlangsung.</p>
	 *
	 * @param request permintaan HTTP, sumber sesi HTTP yang diperiksa
	 * @param tbmuser pengguna yang diperiksa jumlah role-nya, boleh {@code null}
	 * @return {@code true} bila {@code tbmuser} tidak null, sesi belum pernah
	 *         ditandai {@code udah_tanya}, dan {@link Tbmuser#ambilRoles()}
	 *         mengembalikan lebih dari satu role
	 * @throws Exception diteruskan dari {@link Tbmuser#ambilRoles()}
	 */
	private boolean harusTanyaHakAkses(HttpServletRequest request, Tbmuser tbmuser) throws Exception {
		if (tbmuser == null) {
			return false;
		}

		if (request.getSession().getAttribute("udah_tanya") != null) {
			return false;
		}

		List<Tbmrole> tbmroles = tbmuser.ambilRoles();

		return tbmroles != null && tbmroles.size() > 1;
	}

	/**
	 * Menentukan apakah pengguna harus langsung diarahkan ke halaman landing member
	 * kantin/koperasi, alih-alih ke shell utama.
	 *
	 * <p>Seluruh syarat berikut harus terpenuhi: {@code tbmuser} tidak null, memiliki
	 * role aktif ({@link Tbmuser#hakAkses()} tidak null) yang BUKAN role
	 * {@link Tbmrole#KANTIN}, terdaftar sebagai anggota koperasi
	 * ({@link Tbmuser#getAnggotaKoperasi()} tidak null), BUKAN pedagang
	 * ({@link Tbmuser#getPedagang()} null), BUKAN admin lain
	 * ({@link Common#getApakahAdminLain(Tbmuser)} false), dan konfigurasi
	 * {@code jika_login_sebagai_member_kecuali_admin_maka_langsung_ke_halaman_member}
	 * bernilai aktif. Kombinasi syarat ini memastikan hanya member murni (bukan
	 * pengurus/pedagang/admin) yang dialihkan otomatis, dan hanya bila admin memang
	 * mengaktifkan perilaku tersebut lewat konfigurasi.</p>
	 *
	 * @param tbmuser pengguna yang diperiksa, boleh {@code null}
	 * @return {@code true} hanya bila seluruh syarat member murni di atas terpenuhi
	 * @throws Exception diteruskan dari {@link Tbmuser#hakAkses()}
	 */
	private boolean harusKeHalamanMember(Tbmuser tbmuser) throws Exception {
		if (tbmuser == null) {
			return false;
		}

		if (tbmuser.hakAkses() == null) {
			return false;
		}

		if (Tbmrole.KANTIN.equals(tbmuser.hakAkses().getRoleId())) {
			return false;
		}

		if (tbmuser.getAnggotaKoperasi() == null) {
			return false;
		}

		if (tbmuser.getPedagang() != null) {
			return false;
		}

		if (Common.getApakahAdminLain(tbmuser)) {
			return false;
		}

		Konfigurasi konfigurasi = Common.getKonfigurasi(
				"jika_login_sebagai_member_kecuali_admin_maka_langsung_ke_halaman_member",
				Konfigurasi.TIDAK_AKTIF);

		return konfigurasi != null && Konfigurasi.AKTIF.equals(konfigurasi.getNilai());
	}
}
