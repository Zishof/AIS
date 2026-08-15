package ais.common.ebisnis;

import java.security.SecureRandom;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** Token CSRF session-bound untuk form dan AJAX eBisnis berbasis cookie. */
public final class EBisnisCsrf {
	public static final String SESSION_KEY = "ebisnisCsrfToken";
	public static final String PARAMETER = "_csrf";
	public static final String HEADER = "X-CSRF-Token";
	private static final SecureRandom RANDOM = new SecureRandom();

	private EBisnisCsrf() { }

	public static String ensure(HttpSession session) {
		Object existing = session.getAttribute(SESSION_KEY);
		if (existing instanceof String && ((String) existing).length() >= 32) return (String) existing;
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		StringBuilder value = new StringBuilder(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) value.append(String.format("%02x", Integer.valueOf(bytes[i] & 0xff)));
		String token = value.toString();
		session.setAttribute(SESSION_KEY, token);
		return token;
	}

	public static boolean valid(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) return false;
		Object expectedObject = session.getAttribute(SESSION_KEY);
		if (!(expectedObject instanceof String)) return false;
		String actual = request.getHeader(HEADER);
		if (actual == null || actual.length() == 0) actual = request.getParameter(PARAMETER);
		return constantTimeEquals((String) expectedObject, actual);
	}

	private static boolean constantTimeEquals(String expected, String actual) {
		if (expected == null || actual == null) return false;
		int difference = expected.length() ^ actual.length();
		int maximum = Math.max(expected.length(), actual.length());
		for (int i = 0; i < maximum; i++) {
			char left = i < expected.length() ? expected.charAt(i) : 0;
			char right = i < actual.length() ? actual.charAt(i) : 0;
			difference |= left ^ right;
		}
		return difference == 0;
	}
}
