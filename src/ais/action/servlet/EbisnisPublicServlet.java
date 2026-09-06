package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import ais.action.servlet.api.PendaftarDashboardHelper;
import ais.action.servlet.api.PendaftarPublicHelper;
import ais.action.servlet.api.PendaftarPublicHelper.HasilProses;
import ais.database.model.Pendaftar;

/**
 * <b>EbisnisPublicServlet</b> -- menangani (1) submit form Daftar/Masuk pada modal popup di
 * landing page publik {@code ebisnis.jsp} lewat AJAX JSON (parameter {@code ajax=1}, dipanggil
 * dari modal via {@code fetch(...)}, pola sama dgn {@code login2.jsp}), dan (2) seluruh aksi
 * dashboard self-service Pendaftar SETELAH login (parameter {@code s}, didelegasikan ke
 * {@link PendaftarDashboardHelper}).
 *
 * <p>Sengaja TIDAK ada logika bisnis di sini -- servlet ini murni: baca parameter, panggil
 * helper, simpan status sesi, tulis JSON/redirect. Seluruh aturan hidup di helper (lihat
 * JavaDoc-nya).</p>
 *
 * <h3>Kenapa sesi menyimpan ENTITAS {@link Pendaftar} penuh (bukan cuma id/nama spt sebelumnya)</h3>
 * <p>Dashboard self-service (Brand/Toko/Mesin POS/Investor/Manajemen) butuh identitas Pendaftar
 * yang login utk setiap query IDOR-safe ({@code PendaftarDashboardHelper} selalu memfilter ulang
 * {@code pendaftar = :id} di server, TIDAK PERNAH mempercayai id dari klien) -- menyimpan entity
 * penuh di sesi menghindari query ulang "ambil Pendaftar dari id sesi" di setiap request.</p>
 *
 * <p>Sesi pendaftar ebisnis.id SENGAJA tetap sesi POLOS (atribut {@code HttpSession} biasa) --
 * BUKAN memakai {@code CommonSecurityLoginHelper.setLogin(...)} yang membungkus jadi
 * {@code Tbmuser}/ZK session penuh (dipakai jalur CalonSiswa/PMB) -- ebisnis.jsp adalah JSP
 * publik sederhana, belum masuk ke aplikasi ZK penuh, jadi sesi ringan ini cukup.</p>
 */
public class EbisnisPublicServlet extends HttpServlet {
	/** ID versi serialisasi servlet ini (kontrak {@link java.io.Serializable} bawaan {@code HttpServlet}). */
	private static final long serialVersionUID = 1L;

	/** Kunci atribut sesi tempat entitas {@link Pendaftar} penuh disimpan setelah login sukses. */
	public static final String SESSION_PENDAFTAR = "pendaftarEbisnisEntity";
	/** Kunci atribut sesi untuk pesan flash (sukses/gagal) yang dibaca sekali oleh {@code ebisnis.jsp}. */
	public static final String SESSION_FLASH = "ebisnisFlash";
	/** Kunci atribut sesi untuk jenis pesan flash, bernilai {@code "sukses"} atau {@code "error"}. */
	public static final String SESSION_FLASH_JENIS = "ebisnisFlashJenis";

	/**
	 * Titik masuk tunggal untuk seluruh permintaan {@code POST}: submit modal daftar/masuk
	 * ({@code aksi=daftar|login|logout}) dan aksi dashboard self-service ({@code s=...}).
	 *
	 * <p>Delegasi penuh ke {@link #prosesPost}; hanya {@link org.json.JSONException} yang
	 * ditangkap di sini sebagai jaring pengaman terakhir (dicatat lewat
	 * {@link ais.common.ErrorAuditUtil} dan dibalas JSON kode {@code "91"}) karena galat lain
	 * sudah ditangani lebih rinci oleh method yang didelegasikan.</p>
	 *
	 * @param request permintaan HTTP; parameter {@code aksi}/{@code s}/{@code ajax} menentukan alur
	 * @param response tanggapan HTTP; berupa JSON (jalur AJAX/dashboard) atau redirect (jalur non-AJAX)
	 * @throws ServletException bila forward/dispatch gagal
	 * @throws IOException bila penulisan tanggapan gagal
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			prosesPost(request, response);
		} catch (org.json.JSONException e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit EbisnisPublicServlet.doPost");
			if (!response.isCommitted()) {
				response.setContentType("application/json; charset=UTF-8");
				response.getWriter().write("{\"status\":\"91\",\"description\":\"Terjadi kesalahan pada sistem. Silakan coba lagi.\"}");
			}
		}
	}

	/**
	 * Menjalankan logika utama {@code POST}: menentukan apakah permintaan adalah aksi
	 * dashboard ({@code s} terisi, selalu dibalas JSON lewat {@link #prosesDashboard}) atau
	 * salah satu dari {@code aksi=daftar|login|logout} pada modal publik.
	 *
	 * <p><b>{@code daftar}</b>: DIHENTIKAN secara sengaja (lihat catatan kelas) -- tidak lagi
	 * membuat {@link Pendaftar}, hanya mengalihkan ke wizard {@code /pendaftaran} sambil tetap
	 * membalas kontrak JSON lama ({@code status:"00"}, {@code redirect:...}) agar JS lama tidak
	 * rusak. <b>{@code login}</b>: memanggil {@link PendaftarPublicHelper#login}; bila sukses,
	 * sesi lama di-invalidate lalu dibuat sesi baru (mitigasi <i>session fixation</i>) sebelum
	 * {@link Pendaftar} dan {@code PendaftarSessionPrincipal} disimpan, dan status tenant
	 * ditandai ACTIVE lewat {@code TenantOnboardingService.tandaiAktifSaatLogin}.
	 * <b>{@code logout}</b>: menghapus atribut sesi terkait Pendaftar.</p>
	 *
	 * <p>Jalur AJAX ({@code ajax=1}) selalu membalas JSON dan {@code return} lebih awal;
	 * jalur non-AJAX (atau aksi tak dikenal) jatuh ke redirect balik ke {@code ebisnis.jsp}.</p>
	 *
	 * @param request permintaan HTTP; parameter {@code aksi}, {@code s}, {@code ajax},
	 *                {@code email}/{@code password} (untuk login)
	 * @param response tanggapan HTTP; JSON atau redirect
	 * @throws ServletException bila forward gagal
	 * @throws IOException bila penulisan tanggapan/redirect gagal
	 * @throws org.json.JSONException bila penyusunan JSON gagal
	 */
	private void prosesPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, org.json.JSONException {
		String aksi = request.getParameter("aksi");
		String subAksi = request.getParameter("s");
		HttpSession session = request.getSession(true);
		boolean ajax = "1".equals(request.getParameter("ajax"));

		if (subAksi != null) {
			// Aksi dashboard (butuh sesi Pendaftar aktif) -- SELALU balas JSON.
			tulisJson(response, prosesDashboard(session, subAksi, request));
			return;
		}

		if ("daftar".equals(aksi)) {
			// DEPRECATED (P2 Pendaftaran Tenant, §4.5 opsi transisi): jalur ini TIDAK lagi
			// menyimpan Pendaftar. Satu-satunya jalur pembuatan akun baru = wizard
			// Common.ROOT + "/pendaftaran" -- form modal lama tidak memuat consent
			// Syarat&Ketentuan/Privasi versioned, pilihan multi-jenis-usaha, maupun username
			// tenant, sehingga "delegasi diam-diam" akan MEMALSUKAN consent yang tidak pernah
			// dicentang pengguna (dilarang §14.5). Respons tetap kompatibel kontrak lama:
			// AJAX menerima {status:"00", redirect:...} -> JS lama otomatis berpindah halaman.
			String tujuanWizard = request.getContextPath() + "/pendaftaran";
			if (ajax) {
				JSONObject j = new JSONObject();
				j.put("status", "00");
				j.put("code", "REGISTRATION_MOVED");
				j.put("description",
						"Formulir pendaftaran telah diperbarui. Anda akan diarahkan ke halaman pendaftaran tenant baru.");
				j.put("redirect", tujuanWizard);
				tulisJson(response, j);
				return;
			}
			response.sendRedirect(tujuanWizard);
			return;
		} else if ("login".equals(aksi)) {
			HasilProses hasil = PendaftarPublicHelper.login(
					request.getParameter("email"),
					request.getParameter("password"));
			if (hasil.sukses) {
				// Mitigasi session fixation (§12.2): sesi pra-login dibuang, ganti sesi baru
				// (Servlet 2.5 tidak punya changeSessionId) -- atribut flash/CSRF pendaftaran
				// lama ikut hangus, itu memang disengaja.
				try {
					session.invalidate();
				} catch (IllegalStateException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) EbisnisPublicServlet.login.invalidate");
				}
				session = request.getSession(true);
				if (hasil.pendaftar != null) {
					session.setAttribute(ais.common.security.PendaftarSessionPrincipal.SESSION_KEY,
							ais.common.security.PendaftarSessionPrincipal.dari(hasil.pendaftar));
					// READY -> ACTIVE saat owner login pertama (invariant workflow #7).
					ais.service.tenant.TenantOnboardingService.tandaiAktifSaatLogin(hasil.pendaftar.getId());
				}
			}
			terapkanHasil(session, hasil);
			if (ajax) {
				tulisJson(response, jsonDariHasil(request, hasil));
				return;
			}
		} else if ("logout".equals(aksi)) {
			session.removeAttribute(SESSION_PENDAFTAR);
			session.removeAttribute(ais.common.security.PendaftarSessionPrincipal.SESSION_KEY);
			if (ajax) {
				JSONObject j = new JSONObject();
				j.put("status", "00");
				tulisJson(response, j);
				return;
			}
		}

		response.sendRedirect(request.getContextPath() + "/ebisnis.jsp");
	}

	/**
	 * Melayani {@code GET}: menampilkan dashboard self-service bila sesi Pendaftar aktif,
	 * atau mengalihkan ke landing page publik {@code ebisnis.jsp} bila belum login.
	 *
	 * @param request permintaan HTTP
	 * @param response tanggapan HTTP; forward ke dashboard atau redirect ke landing page
	 * @throws ServletException bila forward gagal
	 * @throws IOException bila redirect gagal
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession(true);
		if (session.getAttribute(SESSION_PENDAFTAR) != null) {
			request.getRequestDispatcher("/WEB-INF/baru/dashboard_ebisnis.jsp").forward(request, response);
			return;
		}
		response.sendRedirect(request.getContextPath() + "/ebisnis.jsp");
	}

	/**
	 * Menjalankan satu aksi dashboard self-service Pendaftar ({@code s=...}), selalu
	 * membalas {@link JSONObject} (tidak pernah melempar ke pemanggil untuk galat bisnis).
	 *
	 * <p>Mensyaratkan sesi Pendaftar aktif ({@link #SESSION_PENDAFTAR}); tanpa itu, dibalas
	 * {@code status:"91"} "Sesi Anda telah berakhir". Seluruh parameter permintaan disalin ke
	 * satu {@code payload} JSON yang diteruskan ke {@link PendaftarDashboardHelper}, yang
	 * SELALU memfilter ulang berdasarkan {@code pendaftar.getId()} milik sesi di server
	 * (IDOR-safe -- lihat catatan kelas), tidak pernah memercayai ID dari klien.</p>
	 *
	 * <p><b>Gerbang program pendaftaran tenant (§11.1):</b> aksi yang namanya berakhiran
	 * {@code _tambah}, {@code _ubah}, atau {@code _nonaktif} dianggap aksi mutasi dan ditolak
	 * ({@code code:"TENANT_NOT_READY"}) selama status tenant belum READY/ACTIVE, dicek ulang
	 * dari basis data lewat {@code TenantOnboardingService.alasanTidakBolehMutasi} -- akun
	 * legacy dari sebelum program ini tetap lolos (dikenal sebagai G-06).</p>
	 *
	 * <p>Aksi tak dikenal dibalas {@code status:"91"} "Aksi tidak dikenal"; galat lain (dari
	 * helper) dicatat lewat {@link ais.common.ErrorAuditUtil} dan dibalas {@code status:"91"}
	 * generik agar detail internal tidak bocor ke klien.</p>
	 *
	 * @param session sesi HTTP aktif; harus memuat atribut {@link #SESSION_PENDAFTAR}
	 * @param subAksi nama aksi dashboard, mis. {@code "brand_list"}, {@code "toko_tambah"}
	 * @param request permintaan HTTP; seluruh parameternya disalin ke payload aksi
	 * @return objek JSON hasil aksi, selalu memuat {@code status}
	 * @throws IOException tidak pernah dilempar dalam praktiknya (disediakan oleh kontrak helper)
	 * @throws org.json.JSONException bila penyusunan JSON gagal
	 */
	private JSONObject prosesDashboard(HttpSession session, String subAksi, HttpServletRequest request)
			throws IOException, org.json.JSONException {
		JSONObject hasil = new JSONObject();
		Pendaftar pendaftar = (Pendaftar) session.getAttribute(SESSION_PENDAFTAR);
		if (pendaftar == null) {
			hasil.put("status", "91");
			hasil.put("description", "Sesi Anda telah berakhir. Silakan masuk kembali.");
			return hasil;
		}
		try {
			JSONObject payload = new JSONObject();
			java.util.Enumeration<String> namaParam = request.getParameterNames();
			while (namaParam.hasMoreElements()) {
				String nm = namaParam.nextElement();
				payload.put(nm, request.getParameter(nm));
			}

			// ---- Gerbang program pendaftaran tenant (§11.1): SEMUA aksi mutasi data
			// operasional ditolak sebelum tenant READY/ACTIVE. Status di-re-fetch dari DB
			// (bukan entity sesi detached); akun legacy pra-program tetap lolos (G-06).
			boolean aksiMutasi = subAksi.endsWith("_tambah") || subAksi.endsWith("_ubah")
					|| subAksi.endsWith("_nonaktif");
			if (aksiMutasi) {
				String modulPerlu = subAksi.startsWith("toko_") || subAksi.startsWith("mesin_pos_")
						? "POS" : null;
				String alasan = ais.service.tenant.TenantOnboardingService
						.alasanTidakBolehMutasi(pendaftar.getId(), modulPerlu);
				if (alasan != null) {
					hasil.put("status", "91");
					hasil.put("code", "TENANT_NOT_READY");
					hasil.put("description", alasan);
					return hasil;
				}
			}

			if ("tenant_list".equals(subAksi)) {
				ais.service.tenant.TenantOnboardingService.tenantList(pendaftar.getId(), hasil);
			} else if ("ringkasan".equals(subAksi)) {
				PendaftarDashboardHelper.ringkasan(pendaftar, hasil);
			} else if ("brand_list".equals(subAksi)) {
				PendaftarDashboardHelper.brandList(pendaftar, hasil);
			} else if ("brand_tambah".equals(subAksi)) {
				PendaftarDashboardHelper.brandTambah(pendaftar, payload, hasil);
			} else if ("toko_list".equals(subAksi)) {
				PendaftarDashboardHelper.tokoList(pendaftar, hasil);
			} else if ("toko_tambah".equals(subAksi)) {
				PendaftarDashboardHelper.tokoTambah(pendaftar, payload, hasil);
			} else if ("mesin_pos_list".equals(subAksi)) {
				PendaftarDashboardHelper.mesinPosList(pendaftar, payload, hasil);
			} else if ("mesin_pos_tambah".equals(subAksi)) {
				PendaftarDashboardHelper.mesinPosTambah(pendaftar, payload, hasil);
			} else if ("investor_list".equals(subAksi)) {
				PendaftarDashboardHelper.investorList(pendaftar, hasil);
			} else if ("investor_tambah".equals(subAksi)) {
				PendaftarDashboardHelper.investorTambah(pendaftar, payload, hasil);
			} else if ("manajemen_list".equals(subAksi)) {
				PendaftarDashboardHelper.manajemenList(pendaftar, hasil);
			} else if ("manajemen_tambah".equals(subAksi)) {
				PendaftarDashboardHelper.manajemenTambah(pendaftar, payload, hasil);
			} else if ("brand_ubah".equals(subAksi)) {
				PendaftarDashboardHelper.brandUbah(pendaftar, payload, hasil);
			} else if ("toko_ubah".equals(subAksi)) {
				PendaftarDashboardHelper.tokoUbah(pendaftar, payload, hasil);
			} else if ("mesin_pos_nonaktif".equals(subAksi)) {
				PendaftarDashboardHelper.mesinPosNonaktif(pendaftar, payload, hasil);
			} else if ("investor_nonaktif".equals(subAksi)) {
				PendaftarDashboardHelper.investorNonaktif(pendaftar, payload, hasil);
			} else if ("manajemen_nonaktif".equals(subAksi)) {
				PendaftarDashboardHelper.manajemenNonaktif(pendaftar, payload, hasil);
			} else {
				hasil.put("status", "91");
				hasil.put("description", "Aksi tidak dikenal.");
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit EbisnisPublicServlet.prosesDashboard:" + subAksi);
			hasil.put("status", "91");
			hasil.put("description", "Terjadi kesalahan pada sistem. Silakan coba lagi.");
		}
		return hasil;
	}

	/**
	 * Menerapkan hasil {@link PendaftarPublicHelper#login} ke sesi: menyimpan entitas
	 * {@link Pendaftar} bila sukses, serta selalu mengisi pesan dan jenis flash
	 * ({@link #SESSION_FLASH}/{@link #SESSION_FLASH_JENIS}) untuk ditampilkan di
	 * {@code ebisnis.jsp} pada jalur non-AJAX.
	 *
	 * @param session sesi HTTP aktif (sesi baru pasca-invalidate untuk login sukses)
	 * @param hasil hasil pemrosesan dari helper, memuat status sukses, pesan, dan Pendaftar
	 */
	private void terapkanHasil(HttpSession session, HasilProses hasil) {
		if (hasil.sukses && hasil.pendaftar != null) {
			session.setAttribute(SESSION_PENDAFTAR, hasil.pendaftar);
		}
		session.setAttribute(SESSION_FLASH, hasil.pesan);
		session.setAttribute(SESSION_FLASH_JENIS, hasil.sukses ? "sukses" : "error");
	}

	/**
	 * Menerjemahkan {@link HasilProses} login menjadi JSON kontrak lama untuk klien AJAX:
	 * {@code status:"00"} dan {@code redirect} ke dashboard bila sukses, atau
	 * {@code status:"91"} dengan {@code description} berisi pesan galat bila gagal.
	 *
	 * @param request permintaan HTTP, dipakai untuk membentuk {@code contextPath} pada redirect
	 * @param hasil hasil pemrosesan login dari {@link PendaftarPublicHelper}
	 * @return objek JSON siap ditulis ke tanggapan
	 * @throws org.json.JSONException bila penyusunan JSON gagal
	 */
	private JSONObject jsonDariHasil(HttpServletRequest request, HasilProses hasil) throws org.json.JSONException {
		JSONObject j = new JSONObject();
		j.put("status", hasil.sukses ? "00" : "91");
		j.put("description", hasil.pesan);
		if (hasil.sukses) {
			j.put("redirect", request.getContextPath() + "/EbisnisPublic");
		}
		return j;
	}

	/**
	 * Menulis {@code json} sebagai isi tanggapan bertipe {@code application/json}, tanpa
	 * membungkus dalam struktur tambahan apa pun.
	 *
	 * @param response tanggapan HTTP yang akan diisi
	 * @param json objek JSON yang akan ditulis apa adanya lewat {@code toString()}
	 * @throws IOException bila penulisan gagal
	 */
	private void tulisJson(HttpServletResponse response, JSONObject json) throws IOException {
		response.setContentType("application/json; charset=UTF-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		out.flush();
	}
}
