package ais.action.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;
import ais.common.ebisnis.EBisnisCsrf;
import ais.common.ebisnis.EBisnisRouteRegistry;
import ais.common.ebisnis.EBisnisRouteRegistry.Route;

/**
 * Front controller namespace {@code /ebisnis/*}.
 *
 * <p>Class ini hanya melakukan routing, header keamanan, dan gerbang sesi AIS.
 * Logika bisnis tetap berada di servlet/helper/service existing agar URL lama
 * dan URL baru memakai aturan transaksi yang sama.</p>
 */
public class EBisnisFrontController extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String requestId = request.getHeader("X-Request-ID");
		if (requestId == null || !requestId.matches("[A-Za-z0-9._-]{8,100}")) {
			requestId = UUID.randomUUID().toString();
		}
		response.setHeader("X-Request-ID", requestId);
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("X-Frame-Options", "SAMEORIGIN");
		response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
		request.setAttribute("ebisnisRequestId", requestId);

		try {
			dispatch(request, response);
		} catch (IllegalArgumentException e) {
			if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "EBisnisFrontController", request);
			if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private void dispatch(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String path = normalize(request.getPathInfo());
		Route route = EBisnisRouteRegistry.resolve(path);
		if (route == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Halaman eBisnis tidak ditemukan.");
			return;
		}

		if (route.isAisLoginRequired() && !hasAisUser(request)) {
			if (expectsJson(request)) {
				response.setContentType("application/json; charset=UTF-8");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.getWriter().write("{\"status\":\"91\",\"code\":\"AUTH_REQUIRED\",\"description\":\"Silakan masuk ke AIS.\"}");
			} else {
				String next = request.getContextPath() + "/ebisnis" + path;
				response.sendRedirect(request.getContextPath() + "/login?redirect=" +
						java.net.URLEncoder.encode(next, "UTF-8"));
			}
			return;
		}

		if ("POST".equalsIgnoreCase(request.getMethod())
				&& ("/auth/login".equals(path) || "/masuk".equals(path)
						|| "/auth/logout".equals(path) || "/keluar".equals(path)
						|| "/auth/session".equals(path))
				&& !EBisnisCsrf.valid(request)) {
			response.setContentType("application/json; charset=UTF-8");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.getWriter().write("{\"status\":\"91\",\"code\":\"CSRF_INVALID\",\"description\":\"Token keamanan tidak valid. Muat ulang halaman.\"}");
			return;
		}

		if (!EBisnisRouteRegistry.KIND_API.equals(route.getKind())
				&& !EBisnisRouteRegistry.KIND_ASSET.equals(route.getKind())) {
			request.setAttribute("ebisnisCsrfToken", EBisnisCsrf.ensure(request.getSession(true)));
		}

		if (route.isAisLoginRequired()) {
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
			response.setHeader("Pragma", "no-cache");
		}

		if (EBisnisRouteRegistry.KIND_REDIRECT.equals(route.getKind())) {
			response.sendRedirect(request.getContextPath() + "/ebisnis" + route.getTarget());
			return;
		}

		HttpServletRequest wrapped = withParametersAndAttributes(request, route.getParameters());
		RequestDispatcher dispatcher = wrapped.getRequestDispatcher(route.getTarget());
		if (dispatcher == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		dispatcher.forward(wrapped, response);
	}

	static String normalize(String pathInfo) {
		String path = pathInfo == null || pathInfo.length() == 0 ? "/" : pathInfo;
		if (!path.startsWith("/") || path.indexOf('\0') >= 0 || path.indexOf('\\') >= 0
				|| path.indexOf("//") >= 0 || path.indexOf("..") >= 0) {
			throw new IllegalArgumentException("Path eBisnis tidak valid.");
		}
		String lower = path.toLowerCase(java.util.Locale.ENGLISH);
		if (lower.indexOf("%2f") >= 0 || lower.indexOf("%5c") >= 0 || lower.indexOf("%2e") >= 0) {
			throw new IllegalArgumentException("Path eBisnis tidak valid.");
		}
		while (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
		return path.toLowerCase(java.util.Locale.ENGLISH);
	}

	private boolean hasAisUser(HttpServletRequest request) {
		try {
			return Common.getCurrentUser(request) != null;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean expectsJson(HttpServletRequest request) {
		String accept = request.getHeader("Accept");
		String contentType = request.getContentType();
		return (accept != null && accept.indexOf("application/json") >= 0)
				|| (contentType != null && contentType.indexOf("application/json") >= 0);
	}

	private HttpServletRequest withParametersAndAttributes(HttpServletRequest request,
			Map<String, String> additions) {
		if (additions == null || additions.isEmpty()) return request;
		Map<String, String> parameters = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String> entry : additions.entrySet()) {
			if (entry.getKey().startsWith("__requestAttribute:")) {
				request.setAttribute(entry.getKey().substring("__requestAttribute:".length()),
						Boolean.valueOf(entry.getValue()));
			} else {
				parameters.put(entry.getKey(), entry.getValue());
			}
		}
		return parameters.isEmpty() ? request : new ParameterRequestWrapper(request, parameters);
	}

	private static final class ParameterRequestWrapper extends HttpServletRequestWrapper {
		private final Map<String, String[]> parameters;

		@SuppressWarnings("unchecked")
		private ParameterRequestWrapper(HttpServletRequest request, Map<String, String> additions) {
			super(request);
			Map<String, String[]> copy = new LinkedHashMap<String, String[]>();
			copy.putAll(request.getParameterMap());
			for (Map.Entry<String, String> entry : additions.entrySet()) {
				copy.put(entry.getKey(), new String[] { entry.getValue() });
			}
			parameters = Collections.unmodifiableMap(copy);
		}

		@Override public String getParameter(String name) {
			String[] values = parameters.get(name);
			return values == null || values.length == 0 ? null : values[0];
		}
		@Override public Map<String, String[]> getParameterMap() { return parameters; }
		@Override public Enumeration<String> getParameterNames() {
			return new Vector<String>(parameters.keySet()).elements();
		}
		@Override public String[] getParameterValues(String name) { return parameters.get(name); }
	}
}
