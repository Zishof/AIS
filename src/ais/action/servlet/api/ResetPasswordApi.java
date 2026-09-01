package ais.action.servlet.api;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.DesEncrypter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.delivery.email.sender.MailSender;

/**
 * Endpoint mobile untuk halaman <b>Reset Password User</b> — padanan
 * {@code reset_password.zul} + {@code ais.action.master.helper.ResetPasswordHelper}.
 *
 * <p>
 * Dua action:
 * <ul>
 * <li>{@code cariPenggunaResetPassword} — cari pengguna berdasarkan User ID dan
 * tampilkan nama serta emailnya, TANPA mengubah apa pun.</li>
 * <li>{@code resetPasswordPengguna} — reset password lalu kirim email
 * pemberitahuan.</li>
 * </ul>
 *
 * <p>
 * <b>Kontrol akses.</b> Fitur ini mengubah kredensial pengguna LAIN, jadi
 * penjagaannya dibuat setara — bukan lebih longgar — dengan sisi web:
 * <ol>
 * <li>Token harus sah ({@code 97} bila tidak).</li>
 * <li>Role harus tepat {@link Tbmrole#ADMINISTRATOR} ({@code am}), meniru
 * {@code Common.getApakahAdmin()} yang dipakai {@code ResetPasswordHelper}.</li>
 * <li>Role harus benar-benar memiliki menu <b>{@value #MENU_RESET_PASSWORD}</b>
 * ("Reset Password User") di {@code job_has_menu}.</li>
 * </ol>
 * Syarat ke-3 sengaja ditambahkan meski web tidak memeriksanya pada setiap
 * event: pada web, {@code doAfterCompose} keluar lebih awal untuk non-admin
 * tetapi handler {@code forward} tetap terpasang. Memeriksa kepemilikan menu di
 * sini menutup celah itu alih-alih menyalinnya ke mobile.
 *
 * <p>
 * Perilaku fungsional sengaja dipertahankan persis seperti web, termasuk
 * <b>password baru = User ID</b>. Mengubah aturan itu di mobile akan membuat
 * dua kanal berperilaku berbeda untuk fitur yang sama.
 *
 * <p>
 * Kompatibel Java 1.7 — tanpa lambda maupun try-with-resources.
 */
public class ResetPasswordApi {

	/** `menu.id` untuk "Reset Password User". */
	public static final long MENU_RESET_PASSWORD = 1000000272L;
	/** `menu.id` untuk reset akun Dosen/Mahasiswa. */
	public static final long MENU_RESET_DOSEN_MAHASISWA = 11860L;
	/** `menu.id` untuk reset akun Guru/Siswa. */
	public static final long MENU_RESET_GURU_SISWA = 11861L;

	private ResetPasswordApi() {
	}

	// ── Action: cari ────────────────────────────────────────────────────────

	public static JSONObject cari(HttpServletRequest req, JSONObject request) {
		Session session = null;
		try {
			Tbmuser pemanggil = ApiUtil.currentUser(request, req);
			session = HibernateUtil.getSessionFactory().openSession();

			long menuId = menuId(request);
			JSONObject tolak = tolakBilaTidakBerhak(session, pemanggil, menuId);
			if (tolak != null) {
				return tolak;
			}

			String id = ApiHelperSupport.optString(request, "userid");
			if (!ApiHelperSupport.hasText(id)) {
				return ApiHelperSupport.status("91", "Masukkan User ID");
			}
			id = id.trim();

			Object[] ketemu = cariPengguna(session, id, menuId);
			if (ketemu == null) {
				return ApiHelperSupport.status("91", "User ID tidak valid");
			}

			JSONObject hasil = new JSONObject();
			hasil.put("status", ApiHelperSupport.STATUS_OK);
			hasil.put("description", "Pengguna ditemukan");
			isiDataPengguna(hasil, id, ketemu);
			return hasil;
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			return ApiHelperSupport.errorResponse(err);
		} finally {
			tutupSessionDiam(session);
			tutupSessionNativeDiam();
		}
	}

	// ── Action: reset ───────────────────────────────────────────────────────

	public static JSONObject reset(HttpServletRequest req, JSONObject request) {
		Session session = null;
		try {
			Tbmuser pemanggil = ApiUtil.currentUser(request, req);
			session = HibernateUtil.getSessionFactory().openSession();

			long menuId = menuId(request);
			JSONObject tolak = tolakBilaTidakBerhak(session, pemanggil, menuId);
			if (tolak != null) {
				return tolak;
			}

			String id = ApiHelperSupport.optString(request, "userid");
			if (!ApiHelperSupport.hasText(id)) {
				return ApiHelperSupport.status("91", "Masukkan User ID");
			}
			id = id.trim();

			Object[] ketemu = cariPengguna(session, id, menuId);
			if (ketemu == null) {
				return ApiHelperSupport.status("91",
						"Data pengguna (User ID " + id + ") tidak ditemukan pada sistem, "
								+ "sehingga password baru tidak dapat disimpan.");
			}

			// Perilaku web dipertahankan: password baru = User ID.
			String passwordBaru = id;
			simpanPasswordBaru(session, ketemu, passwordBaru);

			String email = (String) ketemu[2];
			boolean emailValid = ApiHelperSupport.hasText(email) && Common.isValidEmailAddress(email.trim());
			boolean terkirim = false;
			if (emailValid) {
				terkirim = kirimEmailReset(email.trim(), id, passwordBaru);
			}

			String pesan = "Password untuk User ID " + id + " telah direset menjadi: " + passwordBaru + ".";
			pesan += terkirim ? " Email pemberitahuan telah dikirim ke " + email.trim() + "."
					: " Email TIDAK dikirim (alamat email pengguna kosong/tidak valid).";

			JSONObject hasil = new JSONObject();
			hasil.put("status", ApiHelperSupport.STATUS_OK);
			hasil.put("description", pesan);
			hasil.put("emailTerkirim", Boolean.valueOf(terkirim));
			isiDataPengguna(hasil, id, ketemu);
			return hasil;
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			return ApiHelperSupport.errorResponse(err);
		} finally {
			tutupSessionDiam(session);
			tutupSessionNativeDiam();
		}
	}

	// ── Kontrol akses ───────────────────────────────────────────────────────

	/**
	 * Mengembalikan respons penolakan bila pemanggil tidak berhak, atau
	 * {@code null} bila boleh lanjut.
	 */
	private static JSONObject tolakBilaTidakBerhak(Session session, Tbmuser pemanggil, long menuId) {
		if (pemanggil == null || pemanggil.getUserId() == null) {
			return ApiHelperSupport.status("97", "Token tidak sesuai");
		}
		if (!menuDikenal(menuId)) {
			return ApiHelperSupport.status("93", "Jenis menu reset password tidak dikenal");
		}

		Tbmrole role = null;
		try {
			role = pemanggil.hakAkses();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/ResetPasswordApi.java:hakAkses");
		}
		if (role == null || role.getRoleId() == null) {
			return ApiHelperSupport.status("93", "Anda tidak berhak mengakses fitur ini");
		}
		// Reset umum mempertahankan syarat lama: hanya Administrator. Dua menu
		// khusus Dosen/Mahasiswa dan Guru/Siswa mengikuti layar ZK-nya: role yang
		// memperoleh menu tersebut boleh menjalankannya, tetapi cakupan targetnya
		// dikunci oleh cariPengguna(..., menuId).
		if (menuId == MENU_RESET_PASSWORD
				&& !Tbmrole.ADMINISTRATOR.equalsIgnoreCase(role.getRoleId())) {
			return ApiHelperSupport.status("93", "Reset Password User hanya tersedia untuk Administrator");
		}
		if (!punyaMenu(session, role, menuId)) {
			return ApiHelperSupport.status("93", "Menu reset password tidak tersedia untuk role Anda");
		}
		return null;
	}

	private static long menuId(JSONObject request) {
		String value = ApiHelperSupport.optString(request, "menuId").trim();
		if (!ApiHelperSupport.hasText(value)) {
			// Klien lama hanya mengenal menu Reset Password User.
			return MENU_RESET_PASSWORD;
		}
		try {
			return Long.parseLong(value);
		} catch (Exception e) {
			return -1L;
		}
	}

	private static boolean menuDikenal(long menuId) {
		return menuId == MENU_RESET_PASSWORD || menuId == MENU_RESET_DOSEN_MAHASISWA
				|| menuId == MENU_RESET_GURU_SISWA;
	}

	private static boolean punyaMenu(Session session, Tbmrole role, long idMenu) {
		try {
			Object dariDb = session.get(Tbmrole.class, role.getRoleId());
			Tbmrole roleDb = (dariDb == null) ? role : (Tbmrole) dariDb;
			if (roleDb.getMenus() == null) {
				return false;
			}
			for (Object o : roleDb.getMenus()) {
				if (!(o instanceof Menu)) {
					continue;
				}
				Menu menu = (Menu) o;
				if (menu.getId() != null && menu.getId().longValue() == idMenu) {
					return true;
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/ResetPasswordApi.java:punyaMenu");
		}
		return false;
	}

	// ── Pencarian pengguna ──────────────────────────────────────────────────

	/**
	 * Urutan pencarian meniru {@code ResetPasswordHelper.onCari()} persis:
	 * {@code Tbmuser.userId} → {@code Mahasiswa.nim} → {@code Siswa.nomorInduk}
	 * atau {@code nomorIndukNasional}.
	 *
	 * @return {@code {entity, jenis, email, nama}} atau {@code null} bila tidak
	 *         ditemukan.
	 */
	private static Object[] cariPengguna(Session session, String id, long menuId) {
		org.hibernate.Criteria userCriteria = session.createCriteria(Tbmuser.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("userId", id)).setMaxResults(1);
		if (menuId == MENU_RESET_DOSEN_MAHASISWA) {
			userCriteria.add(Restrictions.isNotNull("dosen"));
		} else if (menuId == MENU_RESET_GURU_SISWA) {
			userCriteria.add(Restrictions.isNotNull("guru"));
		}
		Tbmuser tbmuser = (Tbmuser) ConstantValues.simpleObject(userCriteria, Tbmuser.class);
		if (tbmuser != null && tbmuser.getUserId() != null) {
			return new Object[] { tbmuser, "user", amanEmail(tbmuser.getEmail()), amanTeks(tbmuser.getUserNama()) };
		}

		if (menuId != MENU_RESET_GURU_SISWA) {
			Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("nim", id)).setMaxResults(1), Mahasiswa.class);
			if (mahasiswa != null) {
				return new Object[] { mahasiswa, "mahasiswa", amanEmail(mahasiswa.getEmail()),
						amanTeks(mahasiswa.getNama()) };
			}
		}

		if (menuId != MENU_RESET_DOSEN_MAHASISWA) {
			Siswa siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
					.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
					.add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.or(Restrictions.eq("nomorInduk", id),
							Restrictions.eq("nomorIndukNasional", id)))
					.setMaxResults(1), Siswa.class);
			if (siswa != null) {
				// Siswa tidak punya kolom email, sama seperti catatan di helper web.
				return new Object[] { siswa, "siswa", "", amanTeks(siswa.getNama()) };
			}
		}

		return null;
	}

	private static void isiDataPengguna(JSONObject hasil, String id, Object[] ketemu) {
		String email = (String) ketemu[2];
		boolean emailValid = ApiHelperSupport.hasText(email) && Common.isValidEmailAddress(email.trim());
		ApiHelperSupport.put(hasil, "userId", id);
		ApiHelperSupport.put(hasil, "nama", ketemu[3]);
		ApiHelperSupport.put(hasil, "jenis", ketemu[1]);
		ApiHelperSupport.put(hasil, "email", emailValid ? email.trim() : "");
		ApiHelperSupport.put(hasil, "adaEmail", Boolean.valueOf(emailValid));
	}

	// ── Simpan & email ──────────────────────────────────────────────────────

	private static void simpanPasswordBaru(Session session, Object[] ketemu, String passwordBaru) throws Exception {
		DesEncrypter desEncrypter = Common.desEncrypter.get();
		String terenkripsi = desEncrypter.encrypt(passwordBaru);
		Object entity = ketemu[0];

		session.getTransaction().begin();
		try {
			if (entity instanceof Tbmuser) {
				((Tbmuser) entity).setUserPassword(terenkripsi);
				session.update(entity);
			} else if (entity instanceof Mahasiswa) {
				((Mahasiswa) entity).setPass(terenkripsi);
				session.update(entity);
			} else if (entity instanceof Siswa) {
				((Siswa) entity).setPass(terenkripsi);
				session.update(entity);
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ig) {
				ais.common.ErrorAuditUtil.record(ig,
						"auto-audit(empty-catch) src/ais/action/servlet/api/ResetPasswordApi.java:rollback");
			}
			throw e;
		}
	}

	private static boolean kirimEmailReset(String email, String id, String passwordBaru) {
		try {
			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
			String subject = "Reset Password Akun Anda";
			String body = "Halo,<br><br>Password akun Anda telah direset oleh administrator.<br><br>"
					+ "User ID: <b>" + id + "</b><br>"
					+ "Password baru: <b>" + passwordBaru + "</b><br><br>"
					+ "Demi keamanan, silakan masuk lalu segera ganti password Anda melalui menu Ubah Password.<br><br>"
					+ "Terima kasih.";
			MailSender.sendMail(new JSONArray(), subject, body, sender, email, null);
			return true;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
	}

	// ── Util ────────────────────────────────────────────────────────────────

	private static String amanTeks(String v) {
		return v == null ? "" : v.trim();
	}

	private static String amanEmail(String v) {
		return v == null ? "" : v.trim();
	}

	private static void tutupSessionDiam(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				try {
					session.clear();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/action/servlet/api/ResetPasswordApi.java:clear");
				}
				try {
					session.disconnect();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/action/servlet/api/ResetPasswordApi.java:disconnect");
				}
				try {
					session.close();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/action/servlet/api/ResetPasswordApi.java:close");
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/ResetPasswordApi.java:tutupSession");
		}
	}

	private static void tutupSessionNativeDiam() {
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/action/servlet/api/ResetPasswordApi.java:closeSession");
		}
	}
}
