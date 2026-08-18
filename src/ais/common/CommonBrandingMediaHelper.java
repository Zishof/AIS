package ais.common;

import java.io.File;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.LampiranLain;

/**
 * Helper sinkronisasi logo, banner, dan background dari LampiranLain ke folder web/img.
 * Dibuat terpusat agar proses startup dan upload ulang tidak saling memakai session lama.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CommonBrandingMediaHelper {

	private CommonBrandingMediaHelper() {
	}

	public static void checkLogoUpload() {
		Session streamingSession = null;
		try {
			resetSessions();
			ensureImgDirectory();

			copySingle(LampiranLain.LOGO_DEPAN, LampiranLain.LOGO_DEPAN_STR, new String[] {
					Common.ambilREAL_PATH_REPORT() + "/logo.png",
					Common.ambilREAL_PATH_REPORT() + "/logo.jpg",
					Common.REAL_PATH + "/img/logo.png",
					Common.REAL_PATH + "/img/logo.jpg" });

			copySingleOrFallback(LampiranLain.LOGO_DEPAN_PMB, LampiranLain.LOGO_DEPAN_PMB_STR,
					Common.REAL_PATH + "/img/logo_pmb.png", Common.REAL_PATH + "/img/logo.png", false);

			copySingle(LampiranLain.BANNER_DEPAN, LampiranLain.BANNER_DEPAN_STR, new String[] {
					Common.REAL_PATH + "/img/banner.jpg",
					Common.REAL_PATH + "/img/banner_pmb.jpg",
					Common.REAL_PATH + "/img/banner_psb.jpg" });

			copySingle(LampiranLain.BANNER_DEPAN_PMB, LampiranLain.BANNER_DEPAN_PMB_STR,
					new String[] { Common.REAL_PATH + "/img/banner_pmb.jpg" });
			copySingle(LampiranLain.BANNER_DEPAN_PSB, LampiranLain.BANNER_DEPAN_PSB_STR,
					new String[] { Common.REAL_PATH + "/img/banner_psb.jpg" });

			copySingleOrFallback(LampiranLain.LOGO_DEPAN_ALUMNI, LampiranLain.LOGO_DEPAN_ALUMNI_STR,
					Common.REAL_PATH + "/img/logo_alumni.png", Common.REAL_PATH + "/img/logo.png", false);
			copySingleOrFallback(LampiranLain.LOGO_DEPAN_PESANTREN, LampiranLain.LOGO_DEPAN_PESANTREN_STR,
					Common.REAL_PATH + "/img/logo_sekolah.png", Common.REAL_PATH + "/img/logo.png", false);
			copySingleOrFallback(LampiranLain.BACKGROUND_DEPAN_PESANTREN, LampiranLain.BACKGROUND_DEPAN_PESANTREN_STR,
					Common.REAL_PATH + "/img/main_sekolah.jpg", Common.REAL_PATH + "/img/main.jpg", false);

			copySingle(LampiranLain.BANNER_DEPAN_ALUMNI, LampiranLain.BANNER_DEPAN_ALUMNI_STR,
					new String[] { Common.REAL_PATH + "/img/banner_alumni.jpg" });
			copySingleOrFallback(LampiranLain.LOGO_DEPAN_DASHBOARD, LampiranLain.LOGO_DEPAN_DASHBOARD_STR,
					Common.REAL_PATH + "/img/logo_dashboard.png", Common.REAL_PATH + "/img/logo.png", false);
			copySingle(LampiranLain.BANNER_DEPAN_DASHBOARD, LampiranLain.BANNER_DEPAN_DASHBOARD_STR,
					new String[] { Common.REAL_PATH + "/img/banner_dashboard.jpg" });

			streamingSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
			copyByJenis(streamingSession, LampiranLain.LOGO_SEKOLAH, "logo_sekolah_", ".png", null);
			copyByJenis(streamingSession, LampiranLain.BACKGROUND_SEKOLAH, "background_sekolah_", ".jpg", null);
			copyByJenis(streamingSession, LampiranLain.BACKGROUND_LOGIN_SEKOLAH, "background_login_sekolah_", ".jpg", null);
			copyByJenis(streamingSession, LampiranLain.LOGO_PT, "logo_perguruanTinggi_", ".png", Common.getPath() + "/img/logo.png");
			copyByJenis(streamingSession, LampiranLain.BACKGROUND_PT, "background_perguruanTinggi_", ".jpg", Common.getPath() + "/img/bg.jpg");
			copyByJenis(streamingSession, LampiranLain.BACKGROUND_LOGIN_PT, "background_login_perguruanTinggi_", ".jpg", Common.getPath() + "/img/main.jpg");
			copyByJenis(streamingSession, LampiranLain.BANNER_UTAMA_PT, "banner_perguruanTinggi_", ".jpg", Common.getPath() + "/img/header.jpg");
			copyByJenis(streamingSession, LampiranLain.BANNER_MOBILE_PT, "banner_mobile_perguruanTinggi_", ".jpg", Common.getPath() + "/img/header.jpg");
			copyByJenis(streamingSession, LampiranLain.LOGO_YAYASAN, "logo_yayasan_", ".png", null);
			copyByJenis(streamingSession, LampiranLain.BACKGROUND_YAYASAN, "background_yayasan_", ".jpg", null);
			copyByJenis(streamingSession, LampiranLain.LOGO_PT + "_Pendaftar", "logo_perguruanTinggi_Pendaftar_", ".png", Common.getPath() + "/img/logo.png");
			copyByJenis(streamingSession, LampiranLain.BACKGROUND_PT + "_Pendaftar", "background_perguruanTinggi_Pendaftar_", ".jpg", Common.getPath() + "/img/bg.jpg");
			copyByJenis(streamingSession, LampiranLain.BACKGROUND_LOGIN_PT + "_Pendaftar", "background_login_perguruanTinggi_Pendaftar_", ".jpg", Common.getPath() + "/img/main.jpg");
			copyByJenis(streamingSession, LampiranLain.BANNER_UTAMA_PT + "_Pendaftar", "banner_perguruanTinggi_Pendaftar_", ".jpg", Common.getPath() + "/img/header.jpg");
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(streamingSession);
			resetSessions();
		}
	}

	private static void copySingle(Long ref, String jenis, String[] destinations) {
		try {
			LampiranLain lampiran = LampiranLain.ambil(ref, jenis, true);
			File source = resolveSource(lampiran, null);
			if (source == null) {
				return;
			}
			copyToDestinations(source, destinations);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void copySingleOrFallback(Long ref, String jenis, String destination, String fallback, boolean alwaysOverwriteFallback) {
		try {
			LampiranLain lampiran = LampiranLain.ambil(ref, jenis, true);
			File source = resolveSource(lampiran, fallback);
			File dest = new File(destination);
			if (source == null) {
				return;
			}
			if (lampiran != null || alwaysOverwriteFallback || !dest.exists()) {
				copyToDestinations(source, new String[] { destination });
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void copyByJenis(Session session, String jenis, String prefix, String suffix, String fallback) {
		try {
			if (session == null || !session.isOpen()) {
				return;
			}
			List list = session.createCriteria(LampiranLain.class).addOrder(Order.desc("id"))
					.add(Restrictions.eq("jenis", jenis)).list();
			if (list == null) {
				return;
			}
			for (int i = 0; i < list.size(); i++) {
				try {
					LampiranLain lampiran = (LampiranLain) list.get(i);
					if (lampiran == null || lampiran.getRef() == null) {
						continue;
					}
					File source = resolveSource(lampiran, fallback);
					if (source != null) {
						copyToDestinations(source, new String[] { Common.REAL_PATH + "/img/" + prefix + lampiran.getRef() + suffix });
					}
				} catch (Exception eItem) {
					// ISOLASI PER-ITEM: 1 berkas branding bermasalah (mis. berkas salah-baris atau
					// fisik hilang, lihat FileFoto.ambilFile) jangan menggagalkan sinkronisasi
					// item lain dalam jenis yang sama, apalagi seluruh checkLogoUpload() saat startup.
					Common.tampilErrorJikaAdmin(eItem);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static File resolveSource(LampiranLain lampiran, String fallback) {
		try {
			if (lampiran != null) {
				File file = lampiran.ambilFile();
				if (isReadableFile(file)) {
					return file;
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		if (fallback != null && fallback.trim().length() > 0) {
			File fallbackFile = new File(fallback);
			if (isReadableFile(fallbackFile)) {
				return fallbackFile;
			}
		}
		return null;
	}

	private static boolean isReadableFile(File file) {
		return file != null && file.exists() && file.isFile() && file.length() >= 0L;
	}

	private static void copyToDestinations(File source, String[] destinations) {
		if (source == null || destinations == null) {
			return;
		}
		for (int i = 0; i < destinations.length; i++) {
			try {
				String destination = destinations[i];
				if (destination == null || destination.trim().length() == 0) {
					continue;
				}
				Common.copy(source, new File(destination));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	private static void ensureImgDirectory() {
		try {
			if (Common.REAL_PATH != null && Common.REAL_PATH.trim().length() > 0) {
				File img = new File(Common.REAL_PATH + "/img");
				if (!img.exists()) {
					img.mkdirs();
				}
			}
			String report = Common.ambilREAL_PATH_REPORT();
			if (report != null && report.trim().length() > 0) {
				File reportDir = new File(report);
				if (!reportDir.exists()) {
					reportDir.mkdirs();
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void resetSessions() {
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonBrandingMediaHelper.java:204");
		}
		try {
			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonBrandingMediaHelper.java:208");
		}
	}

	private static void closeSession(Session session) {
		if (session != null) {
			try {
				session.clear();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonBrandingMediaHelper.java:216");
			}
			try {
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonBrandingMediaHelper.java:220");
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonBrandingMediaHelper.java:224");
			}
		}
	}
}
