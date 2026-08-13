package ais.action.servlet.api;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.json.JSONObject;

import ais.common.ErrorAuditUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ErrorLog;

/** Menerima catatan kegagalan POS Flutter tanpa pernah menerima password/token/body bisnis. */
public final class ClientErrorLogApi {
	private static final int MAX_PESAN = 2000;
	private static final int MAX_DETAIL = 12000;
	private static final int MAKS_PER_MENIT = 60;
	private static final ConcurrentHashMap<String, Counter> COUNTERS = new ConcurrentHashMap<String, Counter>();

	private ClientErrorLogApi() {
	}

	private static final class Counter {
		long mulai;
		int jumlah;
	}

	public static JSONObject catat(JSONObject payload, HttpServletRequest request) {
		JSONObject hasil = new JSONObject();
		String ip = request == null ? "unknown" : aman(request.getRemoteAddr(), 100);
		try {
			if (!boleh(ip)) {
				hasil.put("status", "success");
				return hasil;
			}
			String sumber = aman(payload.optString("sumber", "unknown"), 250);
			String pesan = aman(payload.optString("pesan", ""), MAX_PESAN);
			String detail = aman(payload.optString("detail", ""), MAX_DETAIL);
			String referensi = aman(payload.optString("referensi", ""), 100);
			String userAgent = request == null ? "" : aman(request.getHeader("User-Agent"), 500);

			ErrorLog log = new ErrorLog();
			log.setKeterangan("Sumber: POS Flutter (Desktop/Android)\n"
					+ "Referensi: " + referensi + "\n"
					+ "Waktu server: " + new Date() + "\n"
					+ "IP: " + ip + "\n"
					+ "Perangkat: " + userAgent + "\n"
					+ "Aktivitas: " + sumber + "\n"
					+ "Pesan pengguna: " + pesan + "\n"
					+ "Informasi teknis:\n" + detail);

			Session session = HibernateUtil.getSessionFactory().openSession();
			try {
				session.beginTransaction();
				session.save(log);
				session.getTransaction().commit();
			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
			hasil.put("status", "success");
		} catch (Exception e) {
			ErrorAuditUtil.record(e, "auto-audit ClientErrorLogApi.catat", request, false);
			try {
				hasil.put("status", "error");
			} catch (Exception ignored) {
			}
		}
		return hasil;
	}

	private static boolean boleh(String ip) {
		long now = System.currentTimeMillis();
		Counter c = COUNTERS.get(ip);
		if (c == null) {
			Counter baru = new Counter();
			baru.mulai = now;
			Counter lama = COUNTERS.putIfAbsent(ip, baru);
			c = lama == null ? baru : lama;
		}
		synchronized (c) {
			if (now - c.mulai >= 60000L) {
				c.mulai = now;
				c.jumlah = 0;
			}
			return ++c.jumlah <= MAKS_PER_MENIT;
		}
	}

	private static String aman(String value, int max) {
		if (value == null) return "";
		String s = value.replaceAll("(?i)bearer\\s+[a-z0-9._~+/-]+", "Bearer [DISEMBUNYIKAN]")
				.replaceAll("(?i)(password|kata sandi)(\\s*[=:]\\s*)[^\\s,;]+", "$1$2[DISEMBUNYIKAN]");
		return s.length() > max ? s.substring(0, max) : s;
	}
}
