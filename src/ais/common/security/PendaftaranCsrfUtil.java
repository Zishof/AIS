package ais.common.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <h3>Token CSRF session-bound utk route publik {@code /pendaftaran}.</h3>
 *
 * <p>Pola sama {@code ais.common.newui.NewUiCsrfUtil} (session-key sendiri supaya tidak
 * saling mengganggu), tetapi token dibangkitkan {@code SecureRandom} (bukan UUID) dan
 * dibandingkan constant-time. Wajib pada SEMUA POST mutasi (submit/resend/cancel/verify);
 * {@code check_username}/{@code check_email} juga memakainya (token sudah tersedia sejak
 * GET form) plus rate-limit khusus.</p>
 */
public final class PendaftaranCsrfUtil {

	public static final String SESSION_KEY = "pendaftaran_csrf";
	public static final String PARAM = "csrf";
	public static final String HEADER = "X-Pendaftaran-CSRF";

	private PendaftaranCsrfUtil() {
	}

	/** Ambil token session; buat bila belum ada (dipanggil saat GET form). */
	public static String getToken(HttpSession session) {
		if (session == null) {
			return "";
		}
		Object existing = session.getAttribute(SESSION_KEY);
		if (existing instanceof String && ((String) existing).length() > 0) {
			return (String) existing;
		}
		String token = PasswordHashService.tokenAcakHex(32);
		session.setAttribute(SESSION_KEY, token);
		return token;
	}

	/** true bila token request cocok dgn token session (constant-time). */
	public static boolean isValid(HttpServletRequest request) {
		if (request == null) {
			return false;
		}
		HttpSession session = request.getSession(false);
		if (session == null) {
			return false;
		}
		Object expectedObj = session.getAttribute(SESSION_KEY);
		if (!(expectedObj instanceof String) || ((String) expectedObj).isEmpty()) {
			return false;
		}
		String provided = request.getParameter(PARAM);
		if (provided == null || provided.isEmpty()) {
			provided = request.getHeader(HEADER);
		}
		if (provided == null || provided.isEmpty()) {
			return false;
		}
		return java.security.MessageDigest.isEqual(((String) expectedObj).getBytes(), provided.getBytes());
	}

	/** Rotasi token (dipanggil setelah login/verifikasi -- bagian mitigasi session fixation). */
	public static String rotate(HttpSession session) {
		if (session == null) {
			return "";
		}
		String token = PasswordHashService.tokenAcakHex(32);
		session.setAttribute(SESSION_KEY, token);
		return token;
	}
}
