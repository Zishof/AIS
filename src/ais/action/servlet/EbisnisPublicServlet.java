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
	private static final long serialVersionUID = 1L;

	public static final String SESSION_PENDAFTAR = "pendaftarEbisnisEntity";
	public static final String SESSION_FLASH = "ebisnisFlash";
	public static final String SESSION_FLASH_JENIS = "ebisnisFlashJenis";

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

	private void terapkanHasil(HttpSession session, HasilProses hasil) {
		if (hasil.sukses && hasil.pendaftar != null) {
			session.setAttribute(SESSION_PENDAFTAR, hasil.pendaftar);
		}
		session.setAttribute(SESSION_FLASH, hasil.pesan);
		session.setAttribute(SESSION_FLASH_JENIS, hasil.sukses ? "sukses" : "error");
	}

	private JSONObject jsonDariHasil(HttpServletRequest request, HasilProses hasil) throws org.json.JSONException {
		JSONObject j = new JSONObject();
		j.put("status", hasil.sukses ? "00" : "91");
		j.put("description", hasil.pesan);
		if (hasil.sukses) {
			j.put("redirect", request.getContextPath() + "/EbisnisPublic");
		}
		return j;
	}

	private void tulisJson(HttpServletResponse response, JSONObject json) throws IOException {
		response.setContentType("application/json; charset=UTF-8");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		out.flush();
	}
}
