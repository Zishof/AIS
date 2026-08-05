package ais.ui.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.util.ThemeProvider;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pendaftar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;

/**
 * Theme provider untuk memilih CSS sesuai tenant/sekolah/pendaftar/PT.
 *
 * Urutan CSS yang dimuat:
 * 1. CSS bawaan ZK/Breeze dan theme-uri dari zk.xml.
 * 2. /css/css_utama.css sebagai style pusat aplikasi.
 * 3. CSS theme pilihan tenant/sekolah/PT sebagai token warna.
 *
 * Catatan kompatibilitas:
 * - Tetap memakai signature ThemeProvider ZKoss 5.x yang raw Collection/List.
 * - Tidak memakai lambda, stream API, try-with-resources, diamond operator,
 *   atau fitur Java 8+.
 * - Cache busting dibuat per request agar perubahan CSS langsung dimuat ulang browser.
 */
public class MyThemeProvider implements ThemeProvider {

	public static final String FONT_FAMILY = "Verdana, Arial, sans-serif";
	public static final String COLOR_TEXT = "#0f172a";
	public static final String COLOR_TEXT_MUTED = "#475569";

	private static final String CACHE_BUSTER_PARAM = "aisCssReload";
	private static final String OLD_CACHE_BUSTER_PARAM = "timemilis";
	private static final String CACHE_BUSTER_REQUEST_ATTR = MyThemeProvider.class.getName() + ".cacheToken";
	private static final String CUSTOM_CSS_REQUEST_ATTR = MyThemeProvider.class.getName() + ".customCss";

	private static final String AIS_MAIN_CSS = "/css/css_utama.css";

	/**
	 * Cache-busting aktif sementara agar perubahan CSS cepat terlihat saat deploy.
	 * Jika deployment sudah stabil, boleh diubah ke false.
	 */
	private static final boolean FORCE_CSS_RELOAD = true;

	@SuppressWarnings({ "rawtypes" })
	@Override
	public Collection getThemeURIs(Execution exec, List uris) {
		List result = new ArrayList();

		copyDefaultThemeUris(result, uris);
		addIfNotExists(result, appendReloadToken(exec, AIS_MAIN_CSS));

		String customCss = resolveCustomCss(exec);
		if (hasText(customCss)) {
			addIfNotExists(result, appendReloadToken(exec, customCss.trim()));
		}

		return result;
	}

	@Override
	public int getWCSCacheControl(Execution exec, String uri) {
		/*
		 * 0 = jangan cache WCS terlalu lama. Ini membantu ketika CSS theme baru diupload
		 * tetapi browser/proxy masih memegang CSS lama.
		 */
		return FORCE_CSS_RELOAD ? 0 : -1;
	}

	@Override
	public String beforeWCS(Execution exec, String uri) {
		return appendReloadToken(exec, uri);
	}

	@Override
	public String beforeWidgetCSS(Execution exec, String uri) {
		return appendReloadToken(exec, uri);
	}

	private String resolveCustomCss(Execution exec) {
		HttpServletRequest request = getRequest(exec);
		if (request == null) {
			return null;
		}

		Object cachedCss = null;
		try {
			cachedCss = request.getAttribute(CUSTOM_CSS_REQUEST_ATTR);
		} catch (Exception e) {
			cachedCss = null;
		}
		if (cachedCss != null) {
			String cached = cachedCss.toString();
			return hasText(cached) ? cached.trim() : null;
		}

		String css = resolveSekolahCss(request);
		if (!hasText(css)) {
			css = resolvePendaftarCss(request);
		}
		if (!hasText(css)) {
			css = resolvePerguruanTinggiCss(request);
		}

		css = hasText(css) ? css.trim() : "";
		try {
			request.setAttribute(CUSTOM_CSS_REQUEST_ATTR, css);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyThemeProvider.java:117");
		}
		return hasText(css) ? css : null;
	}

	private String resolveSekolahCss(HttpServletRequest request) {
		try {
			Sekolah sekolah = SekolahUtil.getSekolah(request);
			if (sekolah == null) {
				return null;
			}
			Object id = sekolah.getId();
			if (id == null) {
				return hasText(sekolah.getCss()) ? sekolah.getCss().trim() : null;
			}
			Sekolah fresh = (Sekolah) loadFresh(Sekolah.class, id);
			if (fresh != null) {
				return hasText(fresh.getCss()) ? fresh.getCss().trim() : null;
			}
			return hasText(sekolah.getCss()) ? sekolah.getCss().trim() : null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyThemeProvider.java:137");
		}
		return null;
	}

	private String resolvePendaftarCss(HttpServletRequest request) {
		try {
			Pendaftar pendaftar = PerguruanTinggiUtil.getPendaftar(request);
			if (pendaftar == null) {
				return null;
			}
			Object id = pendaftar.getId();
			if (id == null) {
				return hasText(pendaftar.getCss()) ? pendaftar.getCss().trim() : null;
			}
			Pendaftar fresh = (Pendaftar) loadFresh(Pendaftar.class, id);
			if (fresh != null) {
				return hasText(fresh.getCss()) ? fresh.getCss().trim() : null;
			}
			return hasText(pendaftar.getCss()) ? pendaftar.getCss().trim() : null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyThemeProvider.java:157");
		}
		return null;
	}

	private String resolvePerguruanTinggiCss(HttpServletRequest request) {
		try {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
			if (perguruanTinggi == null) {
				return null;
			}
			Object id = perguruanTinggi.getId();
			if (id == null) {
				return hasText(perguruanTinggi.getCss()) ? perguruanTinggi.getCss().trim() : null;
			}
			PerguruanTinggi fresh = (PerguruanTinggi) loadFresh(PerguruanTinggi.class, id);
			if (fresh != null) {
				return hasText(fresh.getCss()) ? fresh.getCss().trim() : null;
			}
			return hasText(perguruanTinggi.getCss()) ? perguruanTinggi.getCss().trim() : null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyThemeProvider.java:177");
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes" })
	private Object loadFresh(Class clazz, Object id) {
		if (!(id instanceof Serializable)) {
			return null;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			return session.get(clazz, (Serializable) id);
		} catch (Exception e) {
			return null;
		} finally {
			closeOpenedSessionQuietly(session);
		}
	}

	private void closeOpenedSessionQuietly(Session session) {
		try {
			if (session != null && session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyThemeProvider.java:203");
		}
	}

	@SuppressWarnings({ "rawtypes" })
	private void copyDefaultThemeUris(List target, List source) {
		if (source == null || source.isEmpty()) {
			return;
		}

		for (Object uriObject : source) {
			if (uriObject == null) {
				continue;
			}
			String uri = uriObject.toString();
			if (shouldRemoveBundledCustomTheme(uri)) {
				continue;
			}
			addIfNotExists(target, appendReloadToken(null, uri));
		}
	}

	@SuppressWarnings({ "rawtypes" })
	private Collection appendReloadTokenToUris(Execution exec, List uris, boolean filterBundledCustomTheme) {
		if (!FORCE_CSS_RELOAD || uris == null || uris.isEmpty()) {
			return uris;
		}

		List result = new ArrayList();
		for (Object uriObject : uris) {
			if (uriObject == null) {
				continue;
			}
			String uri = uriObject.toString();
			if (filterBundledCustomTheme && shouldRemoveBundledCustomTheme(uri)) {
				continue;
			}
			addIfNotExists(result, appendReloadToken(exec, uri));
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addIfNotExists(List list, Object value) {
		if (value == null) {
			return;
		}
		String valueText = value.toString();
		String normalizedValue = normalizeUriForCompare(valueText);
		for (Object existing : list) {
			if (existing == null) {
				continue;
			}
			String existingText = existing.toString();
			String normalizedExisting = normalizeUriForCompare(existingText);
			if (valueText.equals(existingText) || normalizedValue.equals(normalizedExisting)) {
				return;
			}
		}
		list.add(value);
	}

	private String normalizeUriForCompare(String uri) {
		if (!hasText(uri)) {
			return "";
		}
		String value = uri.trim();
		int hashPos = value.indexOf('#');
		if (hashPos >= 0) {
			value = value.substring(0, hashPos);
		}
		int queryPos = value.indexOf('?');
		if (queryPos >= 0) {
			value = value.substring(0, queryPos);
		}

		String lower = value.toLowerCase();
		int cssPos = lower.indexOf("/css/");
		if (cssPos >= 0) {
			return lower.substring(cssPos);
		}
		return lower;
	}

	private boolean shouldRemoveBundledCustomTheme(String uri) {
		if (!hasText(uri)) {
			return false;
		}
		String lower = uri.toLowerCase();
		return lower.indexOf("css/my") >= 0 || lower.indexOf("css/ytb") >= 0;
	}

	private String appendReloadToken(Execution exec, String uri) {
		if (!FORCE_CSS_RELOAD || !hasText(uri)) {
			return uri;
		}

		String cleanUri = uri.trim();
		if (cleanUri.indexOf(CACHE_BUSTER_PARAM + "=") >= 0
				|| cleanUri.indexOf(OLD_CACHE_BUSTER_PARAM + "=") >= 0) {
			return cleanUri;
		}
		if (!isCssLikeUri(cleanUri)) {
			return cleanUri;
		}

		String anchor = "";
		int hashPos = cleanUri.indexOf('#');
		if (hashPos >= 0) {
			anchor = cleanUri.substring(hashPos);
			cleanUri = cleanUri.substring(0, hashPos);
		}

		String separator = cleanUri.indexOf('?') >= 0 ? "&" : "?";
		return cleanUri + separator + CACHE_BUSTER_PARAM + "=" + getReloadToken(exec) + anchor;
	}

	private boolean isCssLikeUri(String uri) {
		if (!hasText(uri)) {
			return false;
		}
		String lower = uri.toLowerCase();
		return lower.indexOf(".css") >= 0 || lower.indexOf("/css/") >= 0 || lower.indexOf("/wcs") >= 0;
	}

	private String getReloadToken(Execution exec) {
		HttpServletRequest request = getRequest(exec);
		if (request != null) {
			try {
				Object token = request.getAttribute(CACHE_BUSTER_REQUEST_ATTR);
				if (token != null) {
					return token.toString();
				}
				String newToken = String.valueOf(System.currentTimeMillis());
				request.setAttribute(CACHE_BUSTER_REQUEST_ATTR, newToken);
				return newToken;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyThemeProvider.java:339");
			}
		}
		return String.valueOf(System.currentTimeMillis());
	}

	private HttpServletRequest getRequest(Execution exec) {
		if (exec == null) {
			return null;
		}
		try {
			Object nativeRequest = exec.getNativeRequest();
			if (nativeRequest instanceof HttpServletRequest) {
				return (HttpServletRequest) nativeRequest;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyThemeProvider.java:354");
		}
		return null;
	}

	private boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}

	public static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		String text = value;
		text = text.replace("&", "&amp;");
		text = text.replace("<", "&lt;");
		text = text.replace(">", "&gt;");
		text = text.replace("\"", "&quot;");
		text = text.replace("'", "&#39;");
		return text;
	}

	public static String normalizeStyle(String style) {
		if (style == null) {
			return "";
		}
		return style.replace('\r', ' ').replace('\n', ' ').trim();
	}
}
