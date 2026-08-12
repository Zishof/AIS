package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import ais.common.Common;
import ais.common.security.PasswordHashService;
import ais.common.security.PendaftaranCsrfUtil;
import ais.common.security.PublicRegistrationRateLimiter;
import ais.service.registration.JenisUsahaTenantSeedService;
import ais.service.registration.PendaftaranTenantService;
import ais.service.registration.PendaftaranValidationService;

/**
 * <h3>Servlet publik Pendaftaran Tenant Baru -- {@code Common.ROOT + "/pendaftaran"}.</h3>
 *
 * <p>Controller TIPIS (§4.1): baca request, validasi CSRF envelope + anti-automation
 * (rate limit IP/email/username, honeypot, elapsed-time), panggil
 * {@link PendaftaranTenantService}, tulis JSON / forward JSP. TIDAK ada Criteria/hashing/
 * aturan bisnis di sini. Publik tanpa login (lolos catch-all Spring Security
 * {@code /** = IS_AUTHENTICATED_ANONYMOUSLY}; FilterJSP meneruskan path non-JSP apa adanya --
 * audit P0 §9).</p>
 *
 * <h4>Route</h4>
 * <pre>
 * GET  /pendaftaran                      -> wizard (JSP; katalog+CSRF+formInstance, tanpa record permanen)
 * GET  /pendaftaran?mode=status&kode=..  -> halaman status (data via POST get_status)
 * GET  /pendaftaran?mode=verifikasi&token=.. -> konsumsi tautan verifikasi email
 * GET  /pendaftaran?mode=tenant-baru     -> wizard utk pendaftar login (fase berikut)
 * POST /pendaftaran action=check_username|check_email|submit_registration|verify_email|
 *      resend_verification|get_status|cancel_draft   (JSON; retry_provisioning = admin, ditolak publik)
 * </pre>
 */
public class PendaftaranTenantServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/** Peta formInstanceId → epoch dibuat (elapsed-time check §13.3), per session. */
	public static final String SESSION_FORM_INSTANCE = "pendaftaranFormInstance";
	/** Daftar registrationCode yang dibuat sesi browser ini (bukti kepemilikan cancel_draft). */
	public static final String SESSION_MILIK = "pendaftaranMilikSesi";
	private static final String SESSION_FORM_AUDITED = "pendaftaranFormAudited";

	@Override
	public void init() throws ServletException {
		super.init();
		// Seed katalog idempoten saat startup (load-on-startup web.xml) -- §6.1.
		JenisUsahaTenantSeedService.pastikanSeed();
		// Worker provisioning latar (daemon; lihat JavaDoc TenantProvisioningWorker).
		ais.service.tenant.TenantProvisioningWorker.mulai();
	}

	@Override
	public void destroy() {
		ais.service.tenant.TenantProvisioningWorker.hentikan();
		super.destroy();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			Common.ROOT = request.getContextPath();
			String mode = param(request, "mode", 40);
			HttpSession session = request.getSession(true);

			if ("verifikasi".equals(mode)) {
				JSONObject hasil;
				if (!PublicRegistrationRateLimiter.izinkan("verif-ip|" + clientIp(request), 20, 3600000L)) {
					hasil = tolak("RATE_LIMITED", "Terlalu banyak percobaan. Coba lagi nanti.");
				} else {
					hasil = PendaftaranTenantService.verifikasiEmail(param(request, "token", 200));
				}
				request.setAttribute("hasilVerifikasi", hasil.toString());
				request.setAttribute("modeHalaman", "verifikasi");
				forwardWizard(request, response, session);
				return;
			}

			if ("admin".equals(mode)) {
				// Backoffice ringkas (§15): HANYA admin platform (root/role Administrator).
				if (!adminBerwenang(Common.getCurrentUser(request))) {
					response.sendRedirect(request.getContextPath() + "/");
					return;
				}
				request.setAttribute("csrfToken", PendaftaranCsrfUtil.getToken(session));
				request.getRequestDispatcher("/WEB-INF/baru/public/pendaftaran_tenant_admin.jsp")
						.forward(request, response);
				return;
			}

			if ("status".equals(mode)) {
				request.setAttribute("modeHalaman", "status");
				request.setAttribute("kodeStatus",
						org.apache.commons.lang.StringEscapeUtils.escapeHtml(param(request, "kode", 40)));
				forwardWizard(request, response, session);
				return;
			}

			// mode default & mode=tenant-baru -> wizard.
			if ("tenant-baru".equals(mode)
					&& ais.common.security.PendaftarSessionPrincipal.dariSesi(session) == null) {
				// Flow tenant tambahan HANYA utk pendaftar yang sudah login (§3.1) --
				// belum login: kembali ke landing utk masuk dulu.
				response.sendRedirect(request.getContextPath() + "/");
				return;
			}
			request.setAttribute("modeHalaman", "tenant-baru".equals(mode) ? "tenant-baru" : "wizard");
			forwardWizard(request, response, session);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftaranTenantServlet.doGet", request);
			if (!response.isCommitted()) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}

	private void forwardWizard(HttpServletRequest request, HttpServletResponse response, HttpSession session)
			throws Exception {
		// Katalog + token CSRF + form instance -- GET tidak membuat record permanen (§4.3).
		request.setAttribute("katalogJson", PendaftaranTenantService.katalog().toString());
		request.setAttribute("csrfToken", PendaftaranCsrfUtil.getToken(session));
		String formInstanceId = PasswordHashService.tokenAcakHex(16);
		petaFormInstance(session).put(formInstanceId, Long.valueOf(System.currentTimeMillis()));
		request.setAttribute("formInstanceId", formInstanceId);
		request.setAttribute("idempotencyKey", PasswordHashService.tokenAcakHex(16));
		if (session.getAttribute(SESSION_FORM_AUDITED) == null) {
			session.setAttribute(SESSION_FORM_AUDITED, Boolean.TRUE);
		}
		request.getRequestDispatcher("/WEB-INF/baru/public/pendaftaran_tenant.jsp").forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject hasil;
		try {
			Common.ROOT = request.getContextPath();
			hasil = prosesPost(request);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftaranTenantServlet.doPost", request);
			hasil = new JSONObject();
			try {
				hasil.put("status", "91");
				hasil.put("code", "INTERNAL_ERROR");
				hasil.put("description", "Terjadi kesalahan pada sistem. Silakan coba lagi.");
			} catch (org.json.JSONException je) { ais.common.ErrorAuditUtil.record(je, "auto-audit(empty-catch) PendaftaranTenantServlet.doPost.fallback");
			}
		}
		tulisJson(response, hasil);
	}

	private JSONObject prosesPost(HttpServletRequest request) throws Exception {
		String action = param(request, "action", 40);
		HttpSession session = request.getSession(true);
		String ip = clientIp(request);

		// ---- CSRF envelope utk SEMUA action POST (token dibagikan saat GET form) ----
		if (!PendaftaranCsrfUtil.isValid(request)) {
			return tolak("CSRF_INVALID", "Sesi formulir tidak valid. Muat ulang halaman pendaftaran.");
		}

		if ("check_username".equals(action)) {
			if (!PublicRegistrationRateLimiter.izinkan("cek-user-ip|" + ip, 60, 3600000L)) {
				return tolak("RATE_LIMITED", "Terlalu banyak percobaan. Coba lagi nanti.");
			}
			return PendaftaranTenantService.cekUsername(param(request, "username", 64));
		}
		if ("check_email".equals(action)) {
			if (!PublicRegistrationRateLimiter.izinkan("cek-email-ip|" + ip, 30, 3600000L)) {
				return tolak("RATE_LIMITED", "Terlalu banyak percobaan. Coba lagi nanti.");
			}
			return PendaftaranTenantService.cekEmail(param(request, "email", 255));
		}
		if ("get_status".equals(action)) {
			if (!PublicRegistrationRateLimiter.izinkan("status-ip|" + ip, 60, 3600000L)) {
				return tolak("RATE_LIMITED", "Terlalu banyak percobaan. Coba lagi nanti.");
			}
			return PendaftaranTenantService.status(param(request, "kode", 40));
		}
		if ("verify_email".equals(action)) {
			if (!PublicRegistrationRateLimiter.izinkan("verif-ip|" + ip, 20, 3600000L)) {
				return tolak("RATE_LIMITED", "Terlalu banyak percobaan. Coba lagi nanti.");
			}
			return PendaftaranTenantService.verifikasiEmail(param(request, "token", 200));
		}
		if ("resend_verification".equals(action)) {
			String kode = param(request, "kode", 40);
			if (!PublicRegistrationRateLimiter.izinkan("resend-ip|" + ip, 5, 3600000L)
					|| !PublicRegistrationRateLimiter.izinkan("resend-kode|" + kode, 3, 3600000L)) {
				return tolak("RATE_LIMITED", "Pengiriman ulang dibatasi. Coba lagi dalam beberapa saat.");
			}
			return PendaftaranTenantService.resendVerifikasi(kode, baseUrl(request));
		}
		if ("cancel_draft".equals(action)) {
			String kode = param(request, "kode", 40);
			if (!milikSesi(session, kode)) {
				return tolak("NOT_OWNER", "Pembatalan hanya dapat dilakukan dari sesi yang mendaftar.");
			}
			return PendaftaranTenantService.cancel(kode);
		}
		if ("retry_provisioning".equals(action)) {
			// Retry adalah wewenang admin backoffice -- endpoint publik menolak (pakai admin_retry).
			return tolak("ADMIN_ONLY", "Aksi ini memerlukan akses admin.");
		}
		if (action.startsWith("admin_")) {
			return prosesAdmin(request, action);
		}
		if ("submit_registration".equals(action)) {
			return prosesSubmit(request, session, ip);
		}
		return tolak("UNKNOWN_ACTION", "Aksi tidak dikenal.");
	}

	private JSONObject prosesSubmit(HttpServletRequest request, HttpSession session, String ip) throws Exception {
		// ---- Anti-automation (§13.3): honeypot + elapsed-time + rate limit ----
		if (!param(request, "website_hp", 100).isEmpty()) {
			return tolakDiam("REQUEST_REJECTED");
		}
		String formInstanceId = param(request, "formInstanceId", 64);
		Long dibuat = petaFormInstance(session).get(formInstanceId);
		long minimalMs = 1000L * minimalDetikIsiForm();
		if (dibuat == null || System.currentTimeMillis() - dibuat.longValue() < minimalMs) {
			return tolakDiam("REQUEST_REJECTED");
		}
		String emailNorm = PendaftaranValidationService.normalisasiEmail(param(request, "emailLogin", 255));
		String userNorm = PendaftaranValidationService.normalisasiUsername(param(request, "desiredUsername", 64));
		if (!PublicRegistrationRateLimiter.izinkan("submit-ip|" + ip, 10, 3600000L)
				|| !PublicRegistrationRateLimiter.izinkan("submit-email|" + emailNorm, 5, 3600000L)
				|| !PublicRegistrationRateLimiter.izinkan("submit-user|" + userNorm, 5, 3600000L)) {
			return tolak("RATE_LIMITED", "Terlalu banyak percobaan pendaftaran. Coba lagi nanti.");
		}

		// ---- Payload utk service (servlet TIDAK memvalidasi bisnis -- itu tugas service) ----
		JSONObject p = new JSONObject();
		String[] fields = { "namaUsaha", "legalName", "tradeName", "bentukUsaha", "nib", "npwp", "telpUsaha",
				"emailUsaha", "website", "deskripsi", "negara", "provinsi", "kotaKabupaten", "kecamatan",
				"kelurahan", "kodePos", "alamat", "timezone", "picNama", "picJabatan", "picEmail", "picTelp",
				"jenisUsahaIds", "jenisUsahaLainnya", "desiredUsername", "customDomain", "planCode",
				"emailLogin", "password", "konfirmasiPassword", "setujuTerms", "setujuPrivacy",
				"setujuMarketing", "termsVersion", "privacyVersion", "idempotencyKey", "requestId", "locale" };
		for (int i = 0; i < fields.length; i++) {
			String nilai = request.getParameter(fields[i]);
			if (nilai != null) {
				p.put(fields[i], nilai);
			}
		}
		if (!p.has("requestId") || p.optString("requestId", "").trim().isEmpty()) {
			p.put("requestId", PasswordHashService.tokenAcakHex(16));
		}
		p.put("sourceIp", ip);
		p.put("userAgent", request.getHeader("User-Agent") == null ? "" : request.getHeader("User-Agent"));

		// Pendaftar yang SUDAH login (principal ringan): permohonan menjadi tenant tambahan
		// milik akun itu -- tidak membuat Pendaftar kedua (§3.1). Anonim = null.
		ais.common.security.PendaftarSessionPrincipal principal =
				ais.common.security.PendaftarSessionPrincipal.dariSesi(session);
		JSONObject hasil = PendaftaranTenantService.submit(p,
				principal == null ? null : principal.pendaftarId, baseUrl(request));

		if ("00".equals(hasil.optString("status")) && hasil.has("registrationCode")) {
			tandaiMilikSesi(session, hasil.optString("registrationCode"));
			petaFormInstance(session).remove(formInstanceId);
			if (hasil.has("redirect")) {
				hasil.put("redirect", request.getContextPath() + hasil.optString("redirect"));
			}
		}
		return hasil;
	}

	// =====================================================================
	// ADMIN BACKOFFICE (§15) -- gerbang privilege per-request, bukan sesi publik
	// =====================================================================

	private JSONObject prosesAdmin(HttpServletRequest request, String action) throws Exception {
		ais.database.model.Tbmuser admin = Common.getCurrentUser(request);
		if (!adminBerwenang(admin)) {
			return tolak("ADMIN_ONLY", "Aksi ini memerlukan akses admin platform.");
		}
		String kode = param(request, "kode", 40);
		String reason = param(request, "reason", 500);
		if ("admin_list".equals(action)) {
			JSONObject filter = new JSONObject();
			filter.put("q", param(request, "q", 100));
			filter.put("statusFilter", param(request, "statusFilter", 40));
			filter.put("halaman", param(request, "halaman", 6));
			return ais.service.registration.PendaftaranTenantAdminService.daftar(filter);
		}
		if ("admin_detail_provisioning".equals(action)) {
			return ais.service.registration.PendaftaranTenantAdminService.detailProvisioning(kode);
		}
		if ("admin_approve".equals(action)) {
			return ais.service.registration.PendaftaranTenantAdminService.approve(admin, kode, reason);
		}
		if ("admin_reject".equals(action)) {
			return ais.service.registration.PendaftaranTenantAdminService.reject(admin, kode, reason);
		}
		if ("admin_retry".equals(action)) {
			return ais.service.registration.PendaftaranTenantAdminService.retry(admin, kode, reason);
		}
		if ("admin_release_reservation".equals(action)) {
			return ais.service.registration.PendaftaranTenantAdminService
					.releaseReservation(admin, kode, reason);
		}
		if ("admin_verify_manual".equals(action)) {
			return ais.service.registration.PendaftaranTenantAdminService
					.verifikasiManual(admin, kode, reason);
		}
		if ("admin_reconcile".equals(action) || "admin_reconcile_repair".equals(action)) {
			Long pendaftarId = ais.service.registration.PendaftaranTenantAdminService
					.pendaftarIdDariKode(kode);
			if (pendaftarId == null) {
				return tolak("REGISTRATION_NOT_FOUND", "Permohonan tidak ditemukan.");
			}
			return "admin_reconcile_repair".equals(action)
					? ais.service.tenant.TenantDataReconciliationService.repair(pendaftarId)
					: ais.service.tenant.TenantDataReconciliationService.reconcile(pendaftarId);
		}
		return tolak("UNKNOWN_ACTION", "Aksi admin tidak dikenal.");
	}

	/** Root ATAU role Administrator ("am") -- konsisten gerbang layar master platform. */
	private static boolean adminBerwenang(ais.database.model.Tbmuser user) {
		if (user == null) {
			return false;
		}
		try {
			if (Boolean.TRUE.equals(user.getRoot())) {
				return true;
			}
			ais.database.model.Tbmrole role = user.hakAkses();
			return role != null
					&& ais.database.model.Tbmrole.ADMINISTRATOR.equalsIgnoreCase(role.getRoleId());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftaranTenantServlet.adminBerwenang");
			return false; // ragu = tolak (fail-closed)
		}
	}

	// =====================================================================
	// UTIL
	// =====================================================================

	@SuppressWarnings("unchecked")
	private java.util.Map<String, Long> petaFormInstance(HttpSession session) {
		Object peta = session.getAttribute(SESSION_FORM_INSTANCE);
		if (!(peta instanceof java.util.Map)) {
			peta = new java.util.concurrent.ConcurrentHashMap<String, Long>();
			session.setAttribute(SESSION_FORM_INSTANCE, peta);
		}
		return (java.util.Map<String, Long>) peta;
	}

	@SuppressWarnings("unchecked")
	private void tandaiMilikSesi(HttpSession session, String kode) {
		Object daftar = session.getAttribute(SESSION_MILIK);
		if (!(daftar instanceof java.util.Set)) {
			daftar = new java.util.concurrent.CopyOnWriteArraySet<String>();
			session.setAttribute(SESSION_MILIK, daftar);
		}
		((java.util.Set<String>) daftar).add(kode);
	}

	private boolean milikSesi(HttpSession session, String kode) {
		Object daftar = session.getAttribute(SESSION_MILIK);
		return daftar instanceof java.util.Set && ((java.util.Set<?>) daftar).contains(kode);
	}

	private static int minimalDetikIsiForm() {
		try {
			return Integer.parseInt(
					Common.getKonfigurasi("pendaftaran_min_detik_isi_form", "5").getNilai().trim());
		} catch (Exception e) {
			return 5;
		}
	}

	/** Basis URL absolut utk tautan email: konfigurasi menang; fallback dari request. */
	private static String baseUrl(HttpServletRequest request) {
		try {
			String konfig = Common.getKonfigurasi("pendaftaran_base_url", "").getNilai();
			if (konfig != null && !konfig.trim().isEmpty()) {
				String b = konfig.trim();
				return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PendaftaranTenantServlet.baseUrl");
		}
		StringBuilder sb = new StringBuilder();
		sb.append(request.getScheme()).append("://").append(request.getServerName());
		int port = request.getServerPort();
		if (!(port == 80 && "http".equals(request.getScheme()))
				&& !(port == 443 && "https".equals(request.getScheme()))) {
			sb.append(":").append(port);
		}
		sb.append(request.getContextPath());
		return sb.toString();
	}

	/** IP klien: X-Forwarded-For token pertama (di belakang proxy) else remoteAddr. */
	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.trim().isEmpty()) {
			int koma = xff.indexOf(',');
			return (koma > 0 ? xff.substring(0, koma) : xff).trim();
		}
		return request.getRemoteAddr();
	}

	private static String param(HttpServletRequest request, String nama, int maksimal) {
		String v = request.getParameter(nama);
		if (v == null) {
			return "";
		}
		v = v.trim();
		return v.length() > maksimal ? v.substring(0, maksimal) : v;
	}

	private static JSONObject tolak(String code, String description) throws org.json.JSONException {
		JSONObject j = new JSONObject();
		j.put("status", "91");
		j.put("code", code);
		j.put("description", description);
		j.put("retryable", "RATE_LIMITED".equals(code));
		return j;
	}

	/** Penolakan anti-bot: generik, tanpa membocorkan mekanisme (honeypot/elapsed-time). */
	private static JSONObject tolakDiam(String code) throws org.json.JSONException {
		JSONObject j = new JSONObject();
		j.put("status", "91");
		j.put("code", code);
		j.put("description", "Permintaan tidak dapat diproses. Muat ulang halaman lalu coba lagi.");
		j.put("retryable", true);
		return j;
	}

	private static void tulisJson(HttpServletResponse response, JSONObject json) throws IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setHeader("Cache-Control", "no-store");
		PrintWriter out = response.getWriter();
		out.write(json.toString());
		out.flush();
	}
}
