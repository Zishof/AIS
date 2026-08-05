package ais.action.master.sekolah.util;

import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;

import ais.action.master.sekolah.SekolahAction;
import ais.action.master.sekolah.YayasanAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.RequestContext;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

public class SekolahUtil {

	public static HashSet<SatuanKerja> ambilSatuanKerjas() {

		Tbmuser tbmuser = Common.getCurrentUser();
		String satuanKerjaPengguna = tbmuser == null || tbmuser.hakAkses() == null ? ""
				: tbmuser.hakAkses().getSatuanKerjas();

		if (!satuanKerjaPengguna.trim().isEmpty()) {
			Criterion criterion = Restrictions.sqlRestriction(satuanKerjaPengguna.isEmpty() ? "true" : "false");
			for (String s : satuanKerjaPengguna.split(",")) {
				if (!s.trim().isEmpty()) {
					criterion = Restrictions.or(criterion, Restrictions.ilike("kode", s.trim(), MatchMode.EXACT));
				}
			}
			Session session = HibernateUtil.currentNativeSession();
			List<SatuanKerja> satuanKerjas = ConstantValues.simpleList(
					session.createCriteria(SatuanKerja.class).add(criterion).add(Restrictions.eq("defaultItem", true)),
					SatuanKerja.class);
			return new HashSet<SatuanKerja>(satuanKerjas);
		} else {

			Yayasan yayasan = getYayasan();
			if (yayasan != null && yayasan.getId() != null) {
				Session session = HibernateUtil.currentNativeSession();
				List<SatuanKerja> satuanKerjas = ConstantValues
						.simpleList(session.createCriteria(SatuanKerja.class).add(Restrictions.eq("defaultItem", true))
								.add(yayasan == null || yayasan.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("yayasan", yayasan)),
								SatuanKerja.class);
				return new HashSet<SatuanKerja>(satuanKerjas);
			} else {
				return new HashSet<SatuanKerja>();
			}
		}
	}

	public static Yayasan getYayasan() {
		try {
			HttpServletRequest request = null;

			// 1. Ambil request dari ZK ExecutionsCtrl dengan aman
			// Perbaikan: Cek null pada getCurrent() dan gunakan instanceof untuk mencegah
			// ClassCastException
			try {
				if (ExecutionsCtrl.getCurrent() != null) {
					Object nativeRequest = ExecutionsCtrl.getCurrent().getNativeRequest();
					if (nativeRequest instanceof HttpServletRequest) {
						request = (HttpServletRequest) nativeRequest;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:78");
				// Abaikan, lanjut ke fallback RequestContext
			}

			// 2. Fallback via RequestContext
			if (request == null) {
				try {
					request = RequestContext.get();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:86");
					// Abaikan
				}
			}
			return getYayasan(request);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:91");

		}
		return new Yayasan();
	}

	public static Yayasan getYayasan(HttpServletRequest request) {

		Yayasan yayasan = (Yayasan) (request == null ? null : request.getSession().getAttribute("yayasan_data"));
		if (yayasan != null && yayasan.getId() != null) {
			return yayasan;
		}
		yayasan = getYayasanData(request);
		if (request != null && yayasan != null && yayasan.getId() != null) {
			request.getSession().setAttribute("yayasan_data", yayasan);
		}
		return yayasan;

	}

	private static Yayasan getYayasanData(HttpServletRequest request) {

		try {

			Sekolah sekolah = getSekolah(request);
			if (sekolah != null && sekolah.getId() != null && sekolah.getYayasan() != null
					&& sekolah.getYayasan().getId() != null) {
				return sekolah.getYayasan();
			}

			if (YayasanAction.yayasanByDomain.isEmpty()) {
				YayasanAction.reInitByDomain();
			}

			// FIX NPE rutin: request bisa null di thread background (mis.
			// elearning-ringkasan-warmup) yg tak punya konteks HTTP -- resolusi
			// Yayasan berbasis domain memang tak mungkin di konteks itu, jadi
			// lewati saja & lanjut ke fallback (return Yayasan kosong) tanpa
			// melempar exception rutin.
			if (request != null) {
				String _sn = request.getServerName();
				if (_sn != null) {
					String _snLc = _sn.toLowerCase();
					for (String s : YayasanAction.yayasanByDomain.keySet()) {
						if (s == null || s.trim().isEmpty()) continue;
						String _sLc = s.trim().toLowerCase();
						if (_snLc.startsWith(_sLc)) {
							return YayasanAction.yayasanByDomain.get(s);
						}
						if (_snLc.contains(_sLc)) {
							return YayasanAction.yayasanByDomain.get(s);
						}
					}
				}
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:136");

		}
		return new Yayasan();
	}

	public static Sekolah getSekolah() {
		try {

			HttpServletRequest request = null;

			// 1. Ambil request dari ZK ExecutionsCtrl dengan aman
			// Perbaikan: Cek null pada getCurrent() dan gunakan instanceof untuk mencegah
			// ClassCastException
			try {
				if (ExecutionsCtrl.getCurrent() != null) {
					Object nativeRequest = ExecutionsCtrl.getCurrent().getNativeRequest();
					if (nativeRequest instanceof HttpServletRequest) {
						request = (HttpServletRequest) nativeRequest;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:157");
				// Abaikan, lanjut ke fallback RequestContext
			}

			// 2. Fallback via RequestContext
			if (request == null) {
				try {
					request = RequestContext.get();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:165");
					// Abaikan
				}
			}

			return getSekolah(request);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:171");

		}
		return new Sekolah();
	}

	public static Sekolah getSekolah(HttpServletRequest request) {
		Sekolah sekolah = (Sekolah) (request == null ? null : request.getSession().getAttribute("sekolah_data"));
		if (sekolah != null && sekolah.getId() != null) {
			return sekolah;
		}
		sekolah = getSekolahData(request);
		if (request != null && sekolah != null && sekolah.getId() != null) {
			request.getSession().setAttribute("sekolah_data", sekolah);
		}

		return sekolah;
	}

	private static Sekolah getSekolahData(HttpServletRequest request) {

		try {

			Tbmuser tbmuser = Common.getCurrentUser(request);
			Tbmrole hakAkses = tbmuser == null ? null : tbmuser.hakAkses();
			if (tbmuser != null && hakAkses != null && hakAkses.getSekolah() != null) {
				return hakAkses.getSekolah();
			}

			if (SekolahAction.sekolahByDomain.isEmpty()) {
				SekolahAction.reInitByDomain();
			}

			// FIX NPE rutin: request bisa null di thread background (mis. laporan
			// payroll async) tanpa konteks HTTP -- lewati resolusi berbasis domain,
			// lanjut ke fallback (return Sekolah kosong) tanpa exception rutin.
			if (request != null) {
				String _sn = request.getServerName();
				if (_sn != null) {
					String _snLc = _sn.toLowerCase();
					for (String s : SekolahAction.sekolahByDomain.keySet()) {
						if (s == null || s.trim().isEmpty()) continue;
						String _sLc = s.trim().toLowerCase();
						if (_snLc.startsWith(_sLc)) {
							return SekolahAction.sekolahByDomain.get(s);
						}
						if (_snLc.contains(_sLc)) {
							return SekolahAction.sekolahByDomain.get(s);
						}
					}
				}
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:215");

		}
		return new Sekolah();
	}

	public static String getSekolahMedia(String jenis) {
		try {
			HttpServletRequest request = null;

			// 1. Ambil request dari ZK ExecutionsCtrl dengan aman
			// Perbaikan: Cek null pada getCurrent() dan gunakan instanceof untuk mencegah
			// ClassCastException
			try {
				if (ExecutionsCtrl.getCurrent() != null) {
					Object nativeRequest = ExecutionsCtrl.getCurrent().getNativeRequest();
					if (nativeRequest instanceof HttpServletRequest) {
						request = (HttpServletRequest) nativeRequest;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:235");
				// Abaikan, lanjut ke fallback RequestContext
			}

			// 2. Fallback via RequestContext
			if (request == null) {
				try {
					request = RequestContext.get();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:243");
					// Abaikan
				}
			}
			return getSekolahMedia(request, jenis);
		} catch (Exception e) {
			if (jenis.contains("background")) {
				return Common.getRequestHostWithProtocol() + "/img/main.jpg";
			} else {
				return Common.getRequestHostWithProtocol() + "/img/logo.png";
			}
		}
	}

	public static String getSekolahMedia(HttpServletRequest request, String jenis) {
		return getSekolahMedia(request, jenis, null);
	}

	public static String getSekolahMedia(HttpServletRequest request, String jenis, Sekolah sekolah) {
		String CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
				+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
				+ request.getContextPath();
		if (Common.bolehKonfigurasi("dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF)) {
			CURRENT_URL = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai();
		}

		try {

			if (sekolah != null && sekolah.getId() != null) {
				CURRENT_URL = CURRENT_URL + "/img/" + jenis + sekolah.getId()
						+ (jenis.contains("background") ? ".jpg" : ".png");
			} else {

				if (SekolahAction.sekolahByDomain.isEmpty()) {
					SekolahAction.reInitByDomain();
				}

				String sn = request.getServerName().toLowerCase().trim();

				boolean a = false;
				for (String s : SekolahAction.sekolahByDomain.keySet()) {

					if (sn.startsWith(s.trim().toLowerCase()) && !s.trim().isEmpty()) {
						CURRENT_URL = CURRENT_URL + "/img/" + jenis + SekolahAction.sekolahByDomain.get(s).getId()
								+ (jenis.contains("background") ? ".jpg" : ".png");
						a = true;
					}

				}

				if (!a) {
					for (String s : SekolahAction.sekolahByDomain.keySet()) {

						if (sn.contains(s.trim().toLowerCase()) && !s.trim().isEmpty()) {
							CURRENT_URL = CURRENT_URL + "/img/" + jenis + SekolahAction.sekolahByDomain.get(s).getId()
									+ (jenis.contains("background") ? ".jpg" : ".png");
							a = true;
						}
					}
				}

				if (!a) {
					if (jenis.contains("background")) {
						CURRENT_URL = CURRENT_URL + "/img/main.jpg";
					} else {
						CURRENT_URL = CURRENT_URL + "/img/logo.png";
					}
				}
			}
			return CURRENT_URL;
		} catch (Exception e) {
			if (jenis.contains("background")) {
				return CURRENT_URL + "/img/main.jpg";
			} else {
				return CURRENT_URL + "/img/logo.png";
			}
		}
	}

	public static String getYayasanMedia(String jenis) {
		try {
			HttpServletRequest request = null;

			// 1. Ambil request dari ZK ExecutionsCtrl dengan aman
			// Perbaikan: Cek null pada getCurrent() dan gunakan instanceof untuk mencegah
			// ClassCastException
			try {
				if (ExecutionsCtrl.getCurrent() != null) {
					Object nativeRequest = ExecutionsCtrl.getCurrent().getNativeRequest();
					if (nativeRequest instanceof HttpServletRequest) {
						request = (HttpServletRequest) nativeRequest;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:336");
				// Abaikan, lanjut ke fallback RequestContext
			}

			// 2. Fallback via RequestContext
			if (request == null) {
				try {
					request = RequestContext.get();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/SekolahUtil.java:344");
					// Abaikan
				}
			}
			return getYayasanMedia(request, jenis);
		} catch (Exception e) {
			if (jenis.contains("background")) {
				return Common.getRequestHostWithProtocol() + "/img/main.jpg";
			} else {
				return Common.getRequestHostWithProtocol() + "/img/logo.png";
			}
		}
	}

	public static String getYayasanMedia(HttpServletRequest request, String jenis) {
		return getYayasanMedia(request, jenis, null);
	}

	public static String getYayasanMedia(HttpServletRequest request, String jenis, Yayasan yayasan) {

		try {
			String CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
					+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
							: ":" + request.getServerPort())
					+ request.getContextPath();

			if (yayasan != null && yayasan.getId() != null) {
				CURRENT_URL = CURRENT_URL + "/img/" + jenis + yayasan.getId()
						+ (jenis.contains("background") ? ".jpg" : ".png");
			} else {

				if (YayasanAction.yayasanByDomain.isEmpty()) {
					YayasanAction.reInitByDomain();
				}

				String sn = request.getServerName().toLowerCase().trim();

				boolean a = false;
				for (String s : YayasanAction.yayasanByDomain.keySet()) {

					if (sn.startsWith(s.trim().toLowerCase())) {
						CURRENT_URL = CURRENT_URL + "/img/" + jenis + YayasanAction.yayasanByDomain.get(s).getId()
								+ (jenis.contains("background") ? ".jpg" : ".png");
						a = true;
					}

				}
				if (!a) {
					for (String s : YayasanAction.yayasanByDomain.keySet()) {

						if (sn.contains(s.trim().toLowerCase())) {
							CURRENT_URL = CURRENT_URL + "/img/" + jenis + YayasanAction.yayasanByDomain.get(s).getId()
									+ (jenis.contains("background") ? ".jpg" : ".png");
							a = true;
						}
					}
				}
				if (!a) {
					if (jenis.contains("background")) {
						CURRENT_URL = CURRENT_URL + "/img/main.jpg";
					} else {
						CURRENT_URL = CURRENT_URL + "/img/logo.png";
					}
				}
			}
			return CURRENT_URL;
		} catch (Exception e) {
			if (jenis.contains("background")) {
				return Common.getRequestHostWithProtocol() + "/img/main.jpg";
			} else {
				return Common.getRequestHostWithProtocol() + "/img/logo.png";
			}
		}
	}
}
