package ais.action.master.helper.util;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.sys.ExecutionsCtrl;

import ais.action.master.PerguruanTinggiAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.RequestContext;
import ais.database.model.Konfigurasi;
import ais.database.model.Pendaftar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;

public class PerguruanTinggiUtil {

	public static PerguruanTinggi getPerguruanTinggi() {

		try {

			if (PerguruanTinggiAction.perguruanTinggiByDomain.isEmpty()) {
				PerguruanTinggiAction.reInitByDomain();
			}
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}
			if (request == null) {
				request = RequestContext.get();
			}
			return getPerguruanTinggi(request);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:38");

		}
		return PerguruanTinggiAction.perguruanTinggiDefault;
	}

	public static PerguruanTinggi getPerguruanTinggi(HttpServletRequest request) {

		Tbmuser tbmuser = Common.getCurrentUser(request);
		try {
			PerguruanTinggi ptUser = tbmuser == null ? null : tbmuser.getPerguruanTinggi();
			if (ptUser != null && ptUser.getId() != null) {
				return ptUser;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:52");
			// User mahasiswa/PMB tertentu bisa tidak memiliki relasi PT langsung.
		}

		PerguruanTinggi perguruanTinggi = (PerguruanTinggi) (request == null ? null
				: request.getSession().getAttribute("perguruanTinggi_data"));
		if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			return perguruanTinggi;
		}
		perguruanTinggi = getPerguruanTinggiData(request);
		if (request != null && perguruanTinggi != null && perguruanTinggi.getId() != null) {
			request.getSession().setAttribute("perguruanTinggi_data", perguruanTinggi);
		}

//		System.out.println("perguruanTinggi -> " + perguruanTinggi);

		return perguruanTinggi == null ? PerguruanTinggiAction.perguruanTinggiDefault : perguruanTinggi;
	}

	private static PerguruanTinggi getPerguruanTinggiData(HttpServletRequest request) {

		try {
			if (request == null) {
				return null;
			}

			Tbmuser tbmuser = Common.getCurrentUser(request);
			Tbmrole hakAkses = tbmuser == null ? null : tbmuser.hakAkses();
			if (tbmuser != null && hakAkses != null && hakAkses.getJurusan() != null
					&& hakAkses.getJurusan().getFakultas() != null
					&& hakAkses.getJurusan().getFakultas().getPerguruanTinggi() != null) {
				return hakAkses.getJurusan().getFakultas().getPerguruanTinggi();
			} else if (tbmuser != null && hakAkses != null && hakAkses.getFakultas() != null
					&& hakAkses.getFakultas().getPerguruanTinggi() != null) {
				return hakAkses.getFakultas().getPerguruanTinggi();
			}

			String sn = request.getServerName().toLowerCase().trim();
//			System.out.println("server name -> " + sn);

			for (String s : PerguruanTinggiAction.perguruanTinggiByDomain.keySet()) {
//				System.out.println("server s -> " + s);
				if (s != null && !s.trim().isEmpty() && sn.startsWith(s.trim().toLowerCase())) {
					return PerguruanTinggiAction.perguruanTinggiByDomain.get(s);
				}

			}

			Sekolah sekolah = SekolahUtil.getSekolah(request);
			if (sekolah != null && sekolah.getPerguruanTinggi() != null
					&& sekolah.getPerguruanTinggi().getId() != null) {
				return sekolah.getPerguruanTinggi();
			}

			if (PerguruanTinggiAction.perguruanTinggiByDomain.isEmpty()) {
				PerguruanTinggiAction.reInitByDomain();
			}

			for (String s : PerguruanTinggiAction.perguruanTinggiByDomain.keySet()) {

				if (s != null && !s.trim().isEmpty() && sn.contains(s.trim().toLowerCase())) {
					return PerguruanTinggiAction.perguruanTinggiByDomain.get(s);
				}
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:117");

		}
		return PerguruanTinggiAction.perguruanTinggiDefault;
	}

	public static Pendaftar getPendaftar() {

		try {

			if (PerguruanTinggiAction.pendaftarByDomain.isEmpty()) {
				PerguruanTinggiAction.reInitByDomain();
			}
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}
			if (request == null) {
				request = RequestContext.get();
			}
			return getPendaftar(request);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:138");

		}
		return null;
	}

	public static Pendaftar getPendaftar(HttpServletRequest request) {
		Pendaftar pendaftar = (Pendaftar) (request == null ? null
				: request.getSession().getAttribute("pendaftar_data"));
		if (pendaftar != null && pendaftar.getId() != null) {
			return pendaftar;
		}
		pendaftar = getPendaftarData(request);
		if (request != null && pendaftar != null && pendaftar.getId() != null) {
			request.getSession().setAttribute("pendaftar_data", pendaftar);
		}
		return pendaftar;
	}

	private static Pendaftar getPendaftarData(HttpServletRequest request) {

		try {
			if (request == null) {
				return null;
			}
			String sn = request.getServerName().toLowerCase().trim();
//			System.out.println("server name -> " + sn);

			for (String s : PerguruanTinggiAction.pendaftarByDomain.keySet()) {
//				System.out.println("server s -> " + s);
				if (s != null && !s.trim().isEmpty() && sn.startsWith(s.trim().toLowerCase())) {
					return PerguruanTinggiAction.pendaftarByDomain.get(s);
				}

			}

			Sekolah sekolah = SekolahUtil.getSekolah(request);
			if (sekolah != null && sekolah.getPendaftar() != null && sekolah.getPendaftar().getId() != null) {
				return sekolah.getPendaftar();
			}

			if (PerguruanTinggiAction.pendaftarByDomain.isEmpty()) {
				PerguruanTinggiAction.reInitByDomain();
			}

			for (String s : PerguruanTinggiAction.pendaftarByDomain.keySet()) {

				if (s != null && !s.trim().isEmpty() && sn.contains(s.trim().toLowerCase())) {
					return PerguruanTinggiAction.pendaftarByDomain.get(s);
				}
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:190");

		}
		return null;
	}

	public static String getMedia(String jenis) {

		try {

			PerguruanTinggi perguruanTinggi = getPerguruanTinggi();
			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				return LampiranLain.ambil(perguruanTinggi.getId(), jenis).ambilFile().getAbsolutePath();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/PerguruanTinggiUtil.java:204");

		}
		return new File(Common.ambilREAL_PATH_REPORT() + "/logo.png").getAbsolutePath();
	}

	public static String getPerguruanTinggiMedia(String jenis) {
		try {
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}
			if (request == null) {
				request = RequestContext.get();
			}
			return getPerguruanTinggiMedia(request, jenis);
		} catch (Exception e) {
			if (jenis.contains("background")) {
				return Common.getRequestHostWithProtocol() + "/img/main.jpg";
			} else if (jenis.contains("banner")) {
				return Common.getRequestHostWithProtocol() + "/img/header.jpg";
			} else {
				return Common.getRequestHostWithProtocol() + "/img/logo.png";
			}
		}
	}

	public static String getPerguruanTinggiMedia(HttpServletRequest request, String jenis) {
		String media = (String) (request == null ? null : request.getSession().getAttribute("media_data_" + jenis));
		if (media != null && !media.trim().isEmpty()) {
			return media;
		}
		media = getPerguruanTinggiMediaData(request, jenis);
		if (request != null && media != null && !media.trim().isEmpty()) {
			request.getSession().setAttribute("media_data_" + jenis, media);
		}
		return media;
	}

	public static String getPerguruanTinggiMedia(HttpServletRequest request, String jenis,
			PerguruanTinggi perguruanTinggi) {
		String media = (String) (request == null ? null : request.getSession().getAttribute("media_data_" + jenis));
		if (media != null && !media.trim().isEmpty()) {
			return media;
		}
		media = getPerguruanTinggiMediaData(request, jenis, perguruanTinggi);
		if (request != null && media != null && !media.trim().isEmpty()) {
			request.getSession().setAttribute("media_data_" + jenis, media);
		}
		return media;
	}

	private static String getPerguruanTinggiMediaData(HttpServletRequest request, String jenis) {
		return getPerguruanTinggiMediaData(request, jenis, null);
	}

	private static String getPerguruanTinggiMediaData(HttpServletRequest request, String jenis,
			PerguruanTinggi perguruanTinggi) {

		String CURRENT_URL = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
				+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
				+ request.getContextPath();

		if (Common.bolehKonfigurasi("dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF)) {
			CURRENT_URL = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai();
		}

		try {

			Tbmuser tbmuser = Common.getCurrentUser(request);
			if (tbmuser != null && tbmuser.getPerguruanTinggi() != null
					&& tbmuser.getPerguruanTinggi().getId() != null) {
				perguruanTinggi = tbmuser.getPerguruanTinggi();
			}

			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				CURRENT_URL = CURRENT_URL + "/img/" + jenis + perguruanTinggi.getId()
						+ (jenis.contains("background") || jenis.contains("banner") ? ".jpg" : ".png");
			} else {

				if (PerguruanTinggiAction.perguruanTinggiByDomain.isEmpty()) {
					PerguruanTinggiAction.reInitByDomain();
				}

				boolean a = false;

				String sn = request.getServerName().toLowerCase().trim();
//			System.out.println("server name -> " + sn);

				for (String s : PerguruanTinggiAction.pendaftarByDomain.keySet()) {

					if (sn.startsWith(s.trim().toLowerCase())) {
						CURRENT_URL = CURRENT_URL + "/img/" + jenis + "Pendaftar_"
								+ PerguruanTinggiAction.pendaftarByDomain.get(s).getId()
								+ (jenis.contains("background") || jenis.contains("banner") ? ".jpg" : ".png");
						a = true;
						break;
					}
				}
				if (!a) {
					for (String s : PerguruanTinggiAction.perguruanTinggiByDomain.keySet()) {

						if (sn.startsWith(s.trim().toLowerCase())) {
							CURRENT_URL = CURRENT_URL + "/img/" + jenis
									+ PerguruanTinggiAction.perguruanTinggiByDomain.get(s).getId()
									+ (jenis.contains("background") || jenis.contains("banner") ? ".jpg" : ".png");
							a = true;
							break;
						}
					}
				}

				if (!a) {
					Sekolah sekolah = SekolahUtil.getSekolah(request);
					if (sekolah != null && sekolah.getPerguruanTinggi() != null
							&& sekolah.getPerguruanTinggi().getId() != null) {

						CURRENT_URL = CURRENT_URL + "/img/" + jenis + sekolah.getPerguruanTinggi().getId()
								+ (jenis.contains("background") || jenis.contains("banner") ? ".jpg" : ".png");
						a = true;
					}
				}
				if (!a) {
					for (String s : PerguruanTinggiAction.perguruanTinggiByDomain.keySet()) {

						if (sn.contains(s.trim().toLowerCase())) {
							CURRENT_URL = CURRENT_URL + "/img/" + jenis
									+ PerguruanTinggiAction.perguruanTinggiByDomain.get(s).getId()
									+ (jenis.contains("background") || jenis.contains("banner") ? ".jpg" : ".png");
							a = true;
							break;
						}
					}
				}
				if (!a) {
					if (jenis.contains("background")) {
						CURRENT_URL = CURRENT_URL + "/img/main.jpg";
					} else if (jenis.contains("banner")) {
						CURRENT_URL = CURRENT_URL + "/img/header.jpg";
					} else {
						CURRENT_URL = CURRENT_URL + "/img/logo.png";
					}
				}
			}
			if (!CURRENT_URL.contains("https")) {
				if (Common.bolehKonfigurasi("wajib_https", Konfigurasi.TIDAK_AKTIF)) {
					CURRENT_URL = CURRENT_URL.replaceAll("http", "https");
				}
			}
			return CURRENT_URL;
		} catch (Exception e) {
			if (jenis.contains("background")) {
				return CURRENT_URL + "/img/main.jpg";
			} else if (jenis.contains("banner")) {
				return CURRENT_URL + "/img/header.jpg";
			} else {
				return CURRENT_URL + "/img/logo.png";
			}
		}
	}
}
