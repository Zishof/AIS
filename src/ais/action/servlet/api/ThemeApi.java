package ais.action.servlet.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.json.JSONObject;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pendaftar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;

/**
 * API tema untuk aplikasi mobile.
 *
 * Mengembalikan tema efektif instansi mengikuti urutan resolusi yang sama dengan
 * {@link ais.ui.util.MyThemeProvider}: Sekolah -> Pendaftar -> PerguruanTinggi.
 *
 * Response berisi path CSS pilihan web beserta token warna dari blok
 * "AIS THEME VARIABLE EXPORT" tiap CSS, sehingga mobile bisa:
 * 1. Memetakan themeKey ke aset JSON theme lokal (theme_&lt;key&gt;.json), atau
 * 2. Fallback membangun ColorScheme dari primary/accent bila themeKey tidak dikenal.
 *
 * Kompatibilitas: tanpa lambda/stream/diamond agar seragam dengan class API lain (Java 1.7).
 */
public final class ThemeApi {

	/** Token warna default mengikuti :root css_utama.css. */
	private static final String DEFAULT_KEY = "default";
	private static final String DEFAULT_PRIMARY = "#007131";
	private static final String DEFAULT_ACCENT = "#e27d21";
	private static final String DEFAULT_CONTRAST = "#ffffff";

	/**
	 * Tabel token warna per file CSS tema.
	 * Nilai diambil dari blok "AIS THEME VARIABLE EXPORT" masing-masing file di web/css.
	 * Format value: { themeKey, primary, accent, contrast }.
	 */
	private static final Map<String, String[]> TOKENS = buildTokens();

	private ThemeApi() {
	}

	private static Map<String, String[]> buildTokens() {
		Map<String, String[]> map = new HashMap<String, String[]>();
		map.put("ytb.css", new String[] { "biru", "#0066cc", "#0099ff", "#ffffff" });
		map.put("biru_tua.css", new String[] { "biru_tua", "#1a4087", "#096a85", "#ffffff" });
		map.put("biru_merah.css", new String[] { "biru_merah", "#030071", "#d30c25", "#ffffff" });
		map.put("biru_hijau.css", new String[] { "biru_hijau", "#007131", "#5c4c94", "#ffffff" });
		map.put("asm.css", new String[] { "biru_orange", "#e27d21", "#fbab29", "#ffffff" });
		map.put("hijau.css", new String[] { "hijau", "#254d5b", "#096a85", "#ffffff" });
		map.put("hijau_orange.css", new String[] { "hijau_orange", "#b19326", "#254d5b", "#ffffff" });
		map.put("hijau_orange2.css", new String[] { "hijau_orange2", "#fbab29", "#254d5b", "#111827" });
		map.put("hijau_kuning.css", new String[] { "hijau_kuning", "#6ba400", "#b3bd05", "#ffffff" });
		map.put("muda_hitam.css", new String[] { "abu_abu", "#615e6b", "#312755", "#ffffff" });
		map.put("sd.css", new String[] { "merah", "#7c3031", "#b30000", "#ffffff" });
		map.put("merah.css", new String[] { "merah", "#7c3031", "#b30000", "#ffffff" });
		map.put("smp.css", new String[] { "hijau_tua_1", "#0c354b", "#096a85", "#ffffff" });
		map.put("sma.css", new String[] { "hijau_tua_2", "#3e6d76", "#096a85", "#ffffff" });
		map.put("tk.css", new String[] { "kuning_tua", "#6b6224", "#b19326", "#ffffff" });
		map.put("kuning_biru_tua.css", new String[] { "kuning_biru_tua", "#715c16", "#d2aa2a", "#ffffff" });
		map.put("kuning_hijau.css", new String[] { "kuning_hijau", "#5c7d60", "#adf7b6", "#ffffff" });
		map.put("kuning_orange.css", new String[] { "kuning_orange", "#e27d21", "#fbab29", "#ffffff" });
		map.put("my24.css", new String[] { "abu_terang", "#7a7a7a", "#c0c0c0", "#ffffff" });
		return map;
	}

	/**
	 * Action "tema" — publik (tidak butuh login) karena tema juga dipakai halaman login mobile.
	 */
	public static JSONObject tema(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		try {
			String css = resolveCss(request);
			String fileName = extractFileName(css);
			String[] token = fileName == null ? null : TOKENS.get(fileName);

			hasil.put("css", css == null ? "" : css);
			if (token != null) {
				hasil.put("themeKey", token[0]);
				hasil.put("primary", token[1]);
				hasil.put("accent", token[2]);
				hasil.put("contrast", token[3]);
			} else {
				hasil.put("themeKey", DEFAULT_KEY);
				hasil.put("primary", DEFAULT_PRIMARY);
				hasil.put("accent", DEFAULT_ACCENT);
				hasil.put("contrast", DEFAULT_CONTRAST);
			}
			hasil.put("status", "00");
			hasil.put("description", "Ambil tema berhasil");
		} catch (Exception e) {
			return ApiHelperSupport.errorResponse("Gagal mengambil tema");
		}
		return hasil;
	}

	/** Urutan resolusi sama dengan MyThemeProvider: Sekolah -> Pendaftar -> PerguruanTinggi. */
	private static String resolveCss(HttpServletRequest request) {
		String css = resolveSekolahCss(request);
		if (!ApiHelperSupport.hasText(css)) {
			css = resolvePendaftarCss(request);
		}
		if (!ApiHelperSupport.hasText(css)) {
			css = resolvePerguruanTinggiCss(request);
		}
		return ApiHelperSupport.hasText(css) ? css.trim() : "";
	}

	private static String resolveSekolahCss(HttpServletRequest request) {
		try {
			Sekolah sekolah = SekolahUtil.getSekolah(request);
			if (sekolah == null) {
				return null;
			}
			Sekolah fresh = (Sekolah) loadFresh(Sekolah.class, sekolah.getId());
			if (fresh != null) {
				return fresh.getCss();
			}
			return sekolah.getCss();
		} catch (Exception e) {
			return null;
		}
	}

	private static String resolvePendaftarCss(HttpServletRequest request) {
		try {
			Pendaftar pendaftar = PerguruanTinggiUtil.getPendaftar(request);
			if (pendaftar == null) {
				return null;
			}
			Pendaftar fresh = (Pendaftar) loadFresh(Pendaftar.class, pendaftar.getId());
			if (fresh != null) {
				return fresh.getCss();
			}
			return pendaftar.getCss();
		} catch (Exception e) {
			return null;
		}
	}

	private static String resolvePerguruanTinggiCss(HttpServletRequest request) {
		try {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
			if (perguruanTinggi == null) {
				return null;
			}
			PerguruanTinggi fresh = (PerguruanTinggi) loadFresh(PerguruanTinggi.class, perguruanTinggi.getId());
			if (fresh != null) {
				return fresh.getCss();
			}
			return perguruanTinggi.getCss();
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	private static Object loadFresh(Class clazz, Object id) {
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
			try {
				if (session != null && session.isOpen()) {
					session.close();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ThemeApi.java");
			}
		}
	}

	/** "/css/merah.css?x=1" -> "merah.css" */
	private static String extractFileName(String css) {
		if (!ApiHelperSupport.hasText(css)) {
			return null;
		}
		String value = css.trim().toLowerCase(Locale.ENGLISH);
		int queryPos = value.indexOf('?');
		if (queryPos >= 0) {
			value = value.substring(0, queryPos);
		}
		int slashPos = value.lastIndexOf('/');
		if (slashPos >= 0) {
			value = value.substring(slashPos + 1);
		}
		return value.length() == 0 ? null : value;
	}
}
