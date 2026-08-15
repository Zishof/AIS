package ais.action.servlet.api;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.common.ConstantValues;
import ais.common.SecurityFilter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.PosDeviceToken;

/**
 * Otentikasi berbasis TOKEN untuk halaman POS lokal ({@code PosApi}, dimuat sebagai berkas
 * {@code file://} di aplikasi desktop Electron -- lihat JavaDoc {@code ais.action.servlet.PosApi}).
 *
 * <p><b>Kenapa token, bukan cookie session seperti jalur web biasa:</b> halaman yang dimuat dari
 * {@code file://} tidak memiliki cookie apa pun untuk domain server manapun -- {@code HttpSession}
 * (dipakai {@code Common.getCurrentUser(request)} di seluruh jalur web/JSP/ZK lain) karena itu tidak
 * bisa dipakai di sini. Kelas ini SENGAJA TERISOLASI dari jalur otentikasi cookie yang sudah ada
 * (tidak menyentuh {@code CommonCurrentSessionHelper}/{@code FilterJSP} sama sekali) -- supaya
 * perubahan yang dibawa fitur ini tidak berdampak ke jalur otentikasi web/ZK yang sudah berjalan
 * bertahun-tahun, konsisten dengan prinsip "blast radius sekecil mungkin" untuk perubahan yang
 * menyentuh keamanan.</p>
 *
 * <p><b>Validasi kredensial memakai jalur SAMA PERSIS dengan login web biasa</b>
 * ({@link SecurityFilter#doAutoLogin}, endpoint yang sama dipakai {@code login2.jsp} dan layar login
 * lokal aplikasi Electron) -- token TIDAK PERNAH diterbitkan lewat jalur validasi terpisah/lebih
 * longgar. Bedanya HANYA apa yang terjadi SETELAH validasi berhasil: jalur web menyimpan status login
 * ke {@code HttpSession} (cookie {@code JSESSIONID}), jalur ini menerbitkan token acak 256-bit yang
 * disimpan klien sendiri.</p>
 */
public final class PosDeviceAuthApi {

	private PosDeviceAuthApi() {
	}

	/** Entropi token: 32 byte (256 bit) acak kriptografis -- setara kekuatan token API modern (mis. GitHub PAT). */
	private static final int PANJANG_TOKEN_BYTE = 32;

	/** Masa berlaku token sejak diterbitkan -- dipilih longgar (kios kasir, bukan sesi web biasa) tapi tetap terbatas. */
	private static final long MASA_BERLAKU_HARI = 30;

	private static final SecureRandom RNG = new SecureRandom();

	/**
	 * Memvalidasi username/password lewat {@link SecurityFilter#doAutoLogin} (jalur SAMA dengan login
	 * web biasa), lalu menerbitkan token acak baru bila berhasil. Token MENTAH hanya pernah ada di
	 * sini dan di respons yang dikembalikan -- yang disimpan ke database hanya hash SHA-256-nya
	 * (lihat {@link PosDeviceToken}).
	 *
	 * @param username
	 * @param password
	 * @param labelPerangkat label bebas opsional (boleh {@code null}).
	 * @param request  diperlukan oleh {@link SecurityFilter#doAutoLogin} -- sesi/cookie yang
	 *        dihasilkannya TIDAK dipakai jalur token ini (dibiarkan apa adanya, tidak berbahaya).
	 * @param response idem.
	 * @return {@code {status:"success", token, expiresAt}} atau {@code {status:"error", message}}.
	 */
	public static JSONObject terbitkanToken(String username, String password, String labelPerangkat,
			HttpServletRequest request, HttpServletResponse response) {
		JSONObject hasil = new JSONObject();
		try {
			String u = username == null ? "" : username.trim();
			String p = password == null ? "" : password;
			if (u.isEmpty() || p.isEmpty()) {
				hasil.put("status", "error");
				hasil.put("message", "Nama pengguna dan kata sandi tidak boleh kosong.");
				return hasil;
			}

			boolean sukses = SecurityFilter.doAutoLogin(u, p, false, "", request, response);
			if (!sukses) {
				hasil.put("status", "error");
				hasil.put("message", "Nama pengguna atau kata sandi tidak valid.");
				return hasil;
			}

			Tbmuser user = SecurityFilter.getCurrentFromUsername(u, request, true);
			if (user == null || user.getUserId() == null) {
				hasil.put("status", "error");
				hasil.put("message", "Otentikasi berhasil, tetapi data pengguna tidak ditemukan.");
				return hasil;
			}

			byte[] rawBytes = new byte[PANJANG_TOKEN_BYTE];
			RNG.nextBytes(rawBytes);
			String tokenMentah = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);
			Date kedaluwarsa = new Date(System.currentTimeMillis() + MASA_BERLAKU_HARI * 24L * 60 * 60 * 1000);

			Session dbSession = HibernateUtil.getSessionFactory().openSession();
			try {
				PosDeviceToken entitas = new PosDeviceToken();
				entitas.setTokenHash(sha256Hex(tokenMentah));
				entitas.setUserId(user.getUserId());
				entitas.setLabelPerangkat(labelPerangkat);
				entitas.setDibuatPada(new Date());
				entitas.setKedaluwarsaPada(kedaluwarsa);
				dbSession.beginTransaction();
				dbSession.save(entitas);
				dbSession.getTransaction().commit();
			} finally {
				HibernateUtil.closeSessionQuietly(dbSession);
			}

			hasil.put("status", "success");
			hasil.put("token", tokenMentah);
			hasil.put("expiresAt", kedaluwarsa.getTime());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit PosDeviceAuthApi.terbitkanToken");
			try {
				hasil.put("status", "error");
				hasil.put("message", "Terjadi kesalahan pada sistem. Silakan hubungi administrator.");
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) PosDeviceAuthApi.terbitkanToken"); }
		}
		return hasil;
	}

	/**
	 * Meng-hash token dari header {@code Authorization: Bearer <token>}, mencari barisnya, validasi
	 * belum kedaluwarsa, lalu memuat {@link Tbmuser} pemiliknya -- dipanggil di AWAL setiap aksi
	 * {@code PosApi} SELAIN {@code login}.
	 *
	 * @param request
	 * @return {@link Tbmuser} bila token valid; {@code null} bila header tidak ada/format salah/token
	 *         tak dikenal/sudah kedaluwarsa (SEMUA kasus ini diperlakukan setara "tidak terautentikasi"
	 *         oleh pemanggil -- pesan error generik, tidak membocorkan alasan spesifik).
	 */
	public static Tbmuser resolveDariRequest(HttpServletRequest request) {
		try {
			String header = request.getHeader("Authorization");
			if (header == null || !header.startsWith("Bearer ")) return null;
			String tokenMentah = header.substring(7).trim();
			if (tokenMentah.isEmpty()) return null;

			String hash = sha256Hex(tokenMentah);

			Session dbSession = HibernateUtil.getSessionFactory().openSession();
			try {
				Criteria c = dbSession.createCriteria(PosDeviceToken.class).add(Restrictions.eq("tokenHash", hash));
				PosDeviceToken entitas = (PosDeviceToken) c.setMaxResults(1).uniqueResult();
				if (entitas == null) return null;
				if (entitas.getKedaluwarsaPada() == null || entitas.getKedaluwarsaPada().before(new Date())) return null;

				Date waktuDipakai = new Date();
				dbSession.beginTransaction();
				dbSession.createSQLQuery("update koperasi.pos_device_token "
						+ "set terakhir_dipakai_pada = :waktu where id = :id")
						.setTimestamp("waktu", waktuDipakai)
						.setLong("id", entitas.getId().longValue()).executeUpdate();
				dbSession.getTransaction().commit();

				Criteria cUser = dbSession.createCriteria(Tbmuser.class).add(Restrictions.eq("userId", entitas.getUserId()));
				return (Tbmuser) ConstantValues.simpleObject(cUser.setMaxResults(1), Tbmuser.class);
			} finally {
				HibernateUtil.closeSessionQuietly(dbSession);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit PosDeviceAuthApi.resolveDariRequest");
			return null;
		}
	}

	/**
	 * Mencabut (menghapus) satu token -- dipakai alur "Keluar Akun (Logout)" di aplikasi desktop.
	 * Token yang sudah dicabut langsung tak berlaku lagi pada permintaan berikutnya. Aman dipanggil
	 * dengan token yang sudah tak ada/tak valid (diam-diam tak melakukan apa-apa).
	 */
	public static void cabutToken(String tokenMentah) {
		if (tokenMentah == null || tokenMentah.trim().isEmpty()) return;
		try {
			String hash = sha256Hex(tokenMentah.trim());
			Session dbSession = HibernateUtil.getSessionFactory().openSession();
			try {
				Criteria c = dbSession.createCriteria(PosDeviceToken.class).add(Restrictions.eq("tokenHash", hash));
				PosDeviceToken entitas = (PosDeviceToken) c.setMaxResults(1).uniqueResult();
				if (entitas != null) {
					dbSession.beginTransaction();
					dbSession.delete(entitas);
					dbSession.getTransaction().commit();
				}
			} finally {
				HibernateUtil.closeSessionQuietly(dbSession);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit PosDeviceAuthApi.cabutToken");
		}
	}

	private static String sha256Hex(String raw) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hashBytes = digest.digest(raw.getBytes("UTF-8"));
		StringBuilder sb = new StringBuilder(hashBytes.length * 2);
		for (int i = 0; i < hashBytes.length; i++) {
			String hex = Integer.toHexString(0xff & hashBytes[i]);
			if (hex.length() == 1) sb.append('0');
			sb.append(hex);
		}
		return sb.toString();
	}
}
