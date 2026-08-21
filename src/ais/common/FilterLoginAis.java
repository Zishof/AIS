package ais.common;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.servlet.MServet;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlacklistIp;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.Mahasiswa;
import ais.database.model.OnlineUsers;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.library.AnggotaYangDiblokir;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sisdes.Penduduk;

public class FilterLoginAis { // Sesuaikan nama class dengan implementasi Anda

	public static PerguruanTinggi perguruanTinggi = null;
	private static Sekolah sekolah = null;

	@SuppressWarnings({ })
	public static boolean doingFilter(String username, String password, boolean mobile, String linkProfile,
			ServletRequest arg0, ServletResponse arg1) {

		HttpServletRequest req = (HttpServletRequest) arg0;
		HttpServletResponse res = (HttpServletResponse) arg1;

		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(req);
		sekolah = SekolahUtil.getSekolah(req);

		Mahasiswa mahasiswa = null;
		Penduduk penduduk = null;
		Tbmuser users = null;
		Siswa siswa = null;
		LogLogin login = null;
		String sBaru = null;

		// 1. PENGAMBILAN DATA DARI CACHE
		if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			penduduk = (Penduduk) getFromCacheSafe(perguruanTinggi, username + "_id_penduduk", Penduduk.class);
			if (penduduk == null) {
				mahasiswa = (Mahasiswa) getFromCacheSafe(perguruanTinggi, username + "_id_mahasiswa", Mahasiswa.class);
			}
			if (mahasiswa == null && penduduk == null) {
				siswa = (Siswa) getFromCacheSafe(perguruanTinggi, username + "_id_siswa", Siswa.class);
			}
			if (mahasiswa == null && siswa == null && penduduk == null) {
				users = getUserFromCacheSafe(username);
			}
		}

		String encryptedPassword = Common.desEncrypter.get().encrypt(password);

		// 2. PENGAMBILAN DATA DARI DATABASE JIKA CACHE KOSONG
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();

			if (mahasiswa == null && siswa == null && users == null && penduduk == null) {
				Criteria criteria = session.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
					criteria.createAlias("jurusan", "jurusan").createAlias("jurusan.fakultas", "fakultas")
							.add(Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi));
				}
				mahasiswa = (Mahasiswa) ConstantValues
						.simpleObject(
								criteria.add(
										Restrictions.or(
												Restrictions.and(Restrictions.eq("userOrtu", username.trim()),
														Restrictions.and(Restrictions.isNotNull("userOrtu"),
																Restrictions.sqlRestriction(
																		"trim(this_.user_ortu) != ''"))),
												Restrictions.eq("nim", username.trim())))
										.setMaxResults(1),
								Mahasiswa.class);
			}

			if (mahasiswa == null && siswa == null && users == null && penduduk == null) {
				siswa = (Siswa) ConstantValues.simpleObject(
						session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
								.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.or(Restrictions.eq("nomorInduk", username.trim()),
										Restrictions.or(
												Restrictions.and(Restrictions.eq("userOrtu", username.trim()),
														Restrictions.and(Restrictions.isNotNull("userOrtu"),
																Restrictions.sqlRestriction(
																		"trim(this_.user_ortu) != ''"))),
												Restrictions.eq("nomorIndukNasional", username.trim()))))
								.setMaxResults(1),
						Siswa.class);
			}

			if (mahasiswa == null && siswa == null && users == null && penduduk == null) {
				penduduk = (Penduduk) ConstantValues
						.simpleObject(
								session.createCriteria(Penduduk.class).add(Restrictions.isNotNull("nama"))
										.add(Restrictions.ne("nama", ""))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("kode", username.trim())).setMaxResults(1),
								Penduduk.class);
			}

			if (mahasiswa == null && siswa == null && penduduk == null && users == null) {
				Criteria criteria = session.createCriteria(Tbmuser.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
					criteria.createAlias("fakultas", "fakultas", Criteria.LEFT_JOIN)
							.add(Restrictions.or(Restrictions.isNull("fakultas.perguruanTinggi"),
									Restrictions.eq("fakultas.perguruanTinggi", perguruanTinggi)));
				}
				users = (Tbmuser) ConstantValues.simpleObject(
						criteria.add(Restrictions.eq("userId", username)).setMaxResults(1), Tbmuser.class);
				if (users != null && users.hakAkses() != null && !users.hakAkses().getAktif()) {
					users = null;
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/FilterLoginAis.java:146");
		} finally {
			closeSessionSafely(session);
		}

		// 3. VALIDASI HAK AKSES DAN STATUS PENGGUNA
		String header = MServet.getHeadersInfo(req).toString();
		login = new LogLogin(req.getRequestedSessionId(), header);
		login.setIp(getClientIp(req, header));
		login.setKeterangan("");
		login.setLogin(ais.ui.util.WaktuUtil.getDate());
		login.setNama(username);
		login.setLinkProfile(linkProfile);
		login.setHostname(req.getServerName());

		if (mahasiswa == null && siswa == null && penduduk == null) {
			// Flow untuk Dosen / Admin
			System.out.println("[SOCIAL-LOGIN] doingFilter(" + username + "): jalur Dosen/Admin -- users="
					+ (users == null ? "NULL (tidak ketemu di cache/DB)" : "ditemukan, userId=" + users.getUserId()));
			if (users == null || users.getUserId() == null) {
				System.out.println("[SOCIAL-LOGIN] doingFilter(" + username + "): DITOLAK -- \"Pengguna tidak ditemukan\"");
				return forwardError(req, res, "Pengguna tidak ditemukan");
			}
			if (!users.getAktif()) {
				System.out.println("[SOCIAL-LOGIN] doingFilter(" + username + "): DITOLAK -- akun tidak aktif (aktif=false)");
				return forwardError(req, res,
						"Akun Anda tidak aktif : Harap menghubungi bagian akademik untuk informasi lebih lanjut.");
			}

			if (users.getDosen() != null) {
				if (!checkBlokirDanKuotaDosen(users, req, res)) {
					System.out.println("[SOCIAL-LOGIN] doingFilter(" + username
							+ "): DITOLAK -- checkBlokirDanKuotaDosen() balik false");
					return false;
				}
			}

			login.setDosen(users.getDosen());
			login.setPegawai(users.getPegawai());
			login.setFakultas(users.getDosen() != null ? users.getDosen().getFakultas() : users.ambilFakultas());
			login.setJurusan(users.getDosen() != null ? users.getDosen().getJurusan() : users.ambilJurusan());
			login.setTbmuser(users);
			boolean passwordCocok = users.getUserPassword() != null && !users.getUserPassword().trim().equals("")
					&& users.getUserPassword().equals(encryptedPassword);
			login.setSuccess_status(passwordCocok);
			System.out.println("[SOCIAL-LOGIN] doingFilter(" + username + "): cek password -- tersimpan(panjang)="
					+ (users.getUserPassword() == null ? "NULL" : users.getUserPassword().length()) + ", dihitung ulang(panjang)="
					+ (encryptedPassword == null ? "NULL" : encryptedPassword.length()) + ", cocok=" + passwordCocok);

			login.setDescription(!login.getSuccess_status()
					? "Pengguna ini mencoba mengakses sistem menggunakan password " + password + ", namun gagal login"
					: "Pengguna berhasil login");

			sBaru = (sBaru == null || sBaru.trim().isEmpty()) ? users.write().getAbsolutePath() : sBaru;
			SecurityFilter.dataLogin.put(username, new Object[] { users, login });

			if (perguruanTinggi != null && perguruanTinggi.getId() != null)
				perguruanTinggi.put(sBaru, username + "_admin");

		} else if (siswa != null) {
			// Flow untuk Siswa
			if (!siswa.getAktif()) {
				return forwardError(req, res,
						"Akun Anda tidak aktif : Harap menghubungi bagian akademik untuk informasi lebih lanjut.");
			}

			if ((sekolah == null || sekolah.getId() == null) || (siswa.getSekolah() != null && sekolah != null
					&& !sekolah.getId().equals(siswa.getSekolah().getId()))) {
				if (siswa.getSekolah() != null)
					sekolah = siswa.getSekolah();
				if (sekolah == null || !sekolah.getSiswaDiizinkanDiPortalYayasan()) {
					return forwardError(req, res,
							"Akun siswa tidak diizinkan login di portal bukan sekolah. Harap menghubungi bagian akademik untuk informasi lebih lanjut.");
				}
			}

			if (!checkBlokirDanKuotaSiswa(siswa, req, res))
				return false;

			login.setYayasan(siswa.getSekolah() == null ? null : siswa.getSekolah().getYayasan());
			login.setSekolah(siswa.getSekolah());
			login.setSiswa(siswa);

			boolean pwdMatch = siswa.getPass() != null && !siswa.getPass().trim().isEmpty()
					&& siswa.getPass().equals(encryptedPassword);
			if (!pwdMatch)
				pwdMatch = siswa.getPassOrtu() != null && !siswa.getPassOrtu().trim().isEmpty()
						&& siswa.getPassOrtu().equals(encryptedPassword);

			login.setSuccess_status(pwdMatch);
			login.setDescription(!login.getSuccess_status()
					? "Siswa ini mencoba mengakses sistem menggunakan password " + password + ", namun gagal login"
					: "Siswa berhasil login");

			sBaru = (sBaru == null || sBaru.trim().isEmpty()) ? siswa.getId().toString() : sBaru;
			SecurityFilter.dataLogin.put(username, new Object[] { siswa, login });

			if (perguruanTinggi != null && perguruanTinggi.getId() != null)
				perguruanTinggi.put(sBaru, username + "_id_siswa");

		} else if (penduduk != null) {
			// Flow untuk Penduduk
			if (!penduduk.getAktif()) {
				return forwardError(req, res,
						"Akun Anda tidak aktif : Harap menghubungi kantor desa untuk informasi lebih lanjut.");
			}

			login.setPenduduk(penduduk);
			login.setSuccess_status(penduduk.getPass() != null && !penduduk.getPass().trim().isEmpty()
					&& penduduk.getPass().equals(encryptedPassword));
			login.setDescription(!login.getSuccess_status()
					? "Penduduk ini mencoba mengakses sistem menggunakan password " + password + ", namun gagal login"
					: "Penduduk berhasil login");

			sBaru = (sBaru == null || sBaru.trim().isEmpty()) ? penduduk.getId().toString() : sBaru;
			SecurityFilter.dataLogin.put(username, new Object[] { penduduk, login });

			if (perguruanTinggi != null && perguruanTinggi.getId() != null)
				perguruanTinggi.put(sBaru, username + "_id_penduduk");

		} else {
			// Flow untuk Mahasiswa
			if (!mahasiswa.getAktif()) {
				return forwardError(req, res,
						"Akun Anda tidak aktif : Harap menghubungi bagian akademik untuk informasi lebih lanjut.");
			}

			if ((perguruanTinggi == null || perguruanTinggi.getId() == null)
					&& (mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null
							&& mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null
							&& perguruanTinggi != null && !perguruanTinggi.getId()
									.equals(mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getId()))) {

				return forwardError(req, res,
						"Akun mahasiswa tidak diizinkan login di portal bukan perguruan tinggi. Harap menghubungi bagian akademik untuk informasi lebih lanjut.");
			}

			if (!checkBlokirDanKuotaMahasiswa(mahasiswa, req, res))
				return false;

			login.setFakultas(mahasiswa.getJurusan() == null ? null : mahasiswa.getJurusan().getFakultas());
			login.setJurusan(mahasiswa.getJurusan());
			login.setMahasiswa(mahasiswa);
			login.setNama(mahasiswa.getNim());

			boolean pwdMatch = mahasiswa.getPass() != null && !mahasiswa.getPass().trim().isEmpty()
					&& mahasiswa.getPass().equals(encryptedPassword);
			if (!pwdMatch)
				pwdMatch = mahasiswa.getPassOrtu() != null && !mahasiswa.getPassOrtu().trim().isEmpty()
						&& mahasiswa.getPassOrtu().equals(encryptedPassword);

			login.setSuccess_status(pwdMatch);
			login.setDescription(!login.getSuccess_status()
					? "Mahasiswa ini mencoba mengakses sistem menggunakan password " + password + ", namun gagal login"
					: "Mahasiswa berhasil login");

			sBaru = (sBaru == null || sBaru.trim().isEmpty()) ? mahasiswa.getId().toString() : sBaru;
			SecurityFilter.dataLogin.put(username, new Object[] { mahasiswa, login });

			if (perguruanTinggi != null && perguruanTinggi.getId() != null)
				perguruanTinggi.put(sBaru, username + "_id_mahasiswa");
		}

		// 4. VALIDASI SATUAN KERJA
		if (!isSatuanKerjaValid(mahasiswa, siswa, users, perguruanTinggi, sekolah)) {
			System.out.println("[SOCIAL-LOGIN] doingFilter(" + username + "): DITOLAK -- isSatuanKerjaValid() balik false"
					+ " (\"tidak bisa login di unit/satuan kerja berbeda\")");
			return forwardError(req, res,
					"Akun Anda tidak bisa login di unit / satuan kerja yang berbeda, untuk informasi lebih lanjut, hubungi Admin");
		}

		// 5. PENYIMPANAN LOG DAN CHECK BLACKLIST IP
		if (!saveLoginAndCheckBlacklist(login, req, res)) {
			System.out.println("[SOCIAL-LOGIN] doingFilter(" + username + "): DITOLAK -- saveLoginAndCheckBlacklist() balik false"
					+ " (kemungkinan IP masuk blacklist)");
			return false;
		}

		// 6. MANAJEMEN COOKIE REMEMBER ME
		try {
			boolean rememberMe = req.getParameter("rememberMe") != null
					&& "true".equals(req.getParameter("rememberMe"));
			if (rememberMe && login.getSuccess_status()) {
				String selector = RandomStringUtils.randomAlphanumeric(12);
				String rawValidator = RandomStringUtils.randomAlphanumeric(64);
				String hashedValidator = DigestUtils.sha256Hex(rawValidator);

				Cookie cookieUsername = new Cookie("userinfo", sanitizeCookieValue(java.net.URLEncoder.encode(Common.desEncrypter.get().encrypt(
						username + ";" + password + ";" + selector + ";" + rawValidator + ";" + hashedValidator), "UTF-8")));
				cookieUsername.setMaxAge(15552000);
				res.addCookie(cookieUsername);

				cookieUsername = new Cookie("userid", sanitizeCookieValue(java.net.URLEncoder.encode(username == null ? "" : username, "UTF-8")));
				cookieUsername.setMaxAge(15552000);

				res.addCookie(cookieUsername);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/FilterLoginAis.java:329");
		}

		System.out.println("[SOCIAL-LOGIN] doingFilter(" + username + "): SELESAI, return login.getSuccess_status()="
				+ login.getSuccess_status());
		return login.getSuccess_status();
	}

	/**
	 * Gerbang pengaman terakhir sebelum sebuah String dipakai sebagai value
	 * {@link Cookie}. Nilai yang dikirim ke sini SEHARUSNYA sudah aman (sudah
	 * melalui {@code URLEncoder.encode}), tapi Tomcat pernah melempar
	 * {@code IllegalArgumentException: An invalid character [44] was present in
	 * the Cookie value} (koma, char 44) di jalur remember-me ini -- buang
	 * SEMUA karakter di luar cookie-octet RFC 6265 (huruf/angka & simbol aman)
	 * sebagai jaring pengaman kedua, agar cookie remember-me tak pernah gagal
	 * diset walau ada karakter tak terduga lolos dari lapisan encode di atas.
	 */
	private static String sanitizeCookieValue(String value) {
		// Aturannya kini tinggal SATU salinan di Common.nilaiCookieAman supaya tidak ada
		// jalur cookie lain yang memakai daftar karakter berbeda (lihat KE-FIX di sana).
		return Common.nilaiCookieAman(value);
	}

	// =========================================================================
	// KUMPULAN HELPER METHOD AGAR CODE LEBIH RAPI & BEBAS MEMORY LEAK
	// =========================================================================

	private static void closeSessionSafely(Session session) {
		try {
			if (session != null && session.isOpen()) {
				session.disconnect();
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/FilterLoginAis.java:345");
			// Abaikan error saat menutup agar eksekusi berlanjut
		} finally {
			HibernateUtil.closeSession();
		}
	}

	private static boolean forwardError(ServletRequest req, ServletResponse res, String errorMessage) {
		try {
			req.getRequestDispatcher(
					ConstantValues.recapchaHome + "?login_error=" + URLEncoder.encode(errorMessage, "UTF-8"))
					.forward(req, res);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	private static Object getFromCacheSafe(PerguruanTinggi pt, String key, Class<?> clazz) {
		try {
			String s = pt.retreive(key);
			if (s != null && !s.trim().isEmpty()) {
				return ConstantValues.ambil(clazz.getName(), Long.parseLong(s));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/FilterLoginAis.java:369");
			// Abaikan dan kembalikan null
		}
		return null;
	}

	private static Tbmuser getUserFromCacheSafe(String username) {
		try {
			Tbmuser users = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), username);
			if (users != null && users.hakAkses() != null && !users.hakAkses().getAktif())
				return null;
			return users;
		} catch (Exception e) {
			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				Tbmuser users = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), username);
				if (users != null) {
					Tbmrole tbmrole = users.hakAkses();
					session.refresh(tbmrole);
					if (tbmrole != null && !tbmrole.getAktif())
						return null;
				}
				return users;
			} catch (Exception ex) {
				return null;
			} finally {
				closeSessionSafely(session);
			}
		}
	}

	private static String getClientIp(HttpServletRequest request, String headerString) {
		try {
			if (headerString != null && headerString.trim().startsWith("{")) {
				JSONObject json = new JSONObject(headerString);
				String[] ipHeaders = { "Cf-Connecting-Ip", "CF-Connecting-IP", "X-Forwarded-For", "X-Real-IP" };
				for (String h : ipHeaders) {
					if (json.has(h))
						return json.getString(h);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/FilterLoginAis.java:411");
			// Gunakan remote addr fallback
		}
		return request.getRemoteAddr();
	}

	@SuppressWarnings("unchecked")
	private static boolean saveLoginAndCheckBlacklist(LogLogin login, HttpServletRequest req, HttpServletResponse res) {
		// 1) PENCATATAN LOG BERSIFAT BEST-EFFORT.
		//    Kegagalan menyimpan log login (mis. sequence DB "log_login_id_seq" hilang)
		//    TIDAK boleh menggagalkan proses login pengguna. Bungkus tersendiri agar
		//    error pencatatan tidak merembet menghentikan autentikasi.
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.currentNativeSession();
			tx = session.beginTransaction();
			session.save(login);
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/FilterLoginAis.java:434");
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/FilterLoginAis.java:437");
			// Lanjutkan — jangan blokir login hanya karena gagal mencatat log.
		} finally {
			closeSessionSafely(session);
		}

		// 2) CEK BLACKLIST IP — ini GERBANG KEAMANAN, harus tetap jalan meski pencatatan
		//    di atas gagal. Tidak bergantung pada log yang tersimpan (memakai login.getIp()).
		try {
			Map<Long, BlacklistIp> blacklistIps = ConstantValues.ambilBerdasarClass(BlacklistIp.class);
			if (blacklistIps != null) {
				for (BlacklistIp blacklistIp : blacklistIps.values()) {
					if (blacklistIp.getAktif() && blacklistIp.chek(login.getIp())) {
						login.setSuccess_status(false);
						login.setLogout(ais.ui.util.WaktuUtil.getDate());
						login.setDescription("Pengguna ini sedang di blacklist nomor IP nya");

						// Update status blacklist juga best-effort (login bisa saja belum
						// tersimpan bila langkah 1 gagal) — pemblokiran tetap dilakukan.
						Session s2 = null;
						Transaction tx2 = null;
						try {
							s2 = HibernateUtil.currentNativeSession();
							tx2 = s2.beginTransaction();
							s2.update(login);
							tx2.commit();
						} catch (Exception ex) {
							if (tx2 != null) {
								try {
									tx2.rollback();
								} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/FilterLoginAis.java:467");
								}
							}
						} finally {
							closeSessionSafely(s2);
						}

						return forwardError(req, res,
								"Akun Anda tidak bisa digunakan, untuk informasi lebih lanjut, hubungi Admin");
					}
				}
			}
		} catch (Exception e) {
			// Error tak terduga saat cek blacklist tidak boleh menggagalkan login.
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/FilterLoginAis.java:481");
		}
		return true;
	}

	private static boolean isSatuanKerjaValid(Mahasiswa mhs, Siswa siswa, Tbmuser users, PerguruanTinggi pt,
			Sekolah sekolah) {
		if (pt == null || pt.getSatuanKerja() == null || pt.getSatuanKerja().getId() == null)
			return true;

		if (!Konfigurasi.AKTIF
				.equals(Common.getKonfigurasi("harus_menggunakan_satker_yg_sama", Konfigurasi.TIDAK_AKTIF).getNilai()))
			return true;

		Long targetId = pt.getSatuanKerja().getId();
		boolean tidakSesuai = false;

		if (mhs != null && mhs.getJurusan() != null && mhs.getJurusan().getFakultas() != null
				&& mhs.getJurusan().getFakultas().getPerguruanTinggi() != null
				&& mhs.getJurusan().getFakultas().getPerguruanTinggi().getSatuanKerja() != null) {
			tidakSesuai = !mhs.getJurusan().getFakultas().getPerguruanTinggi().getSatuanKerja().getId()
					.equals(targetId);
		} else if (siswa != null && siswa.getSekolah() != null && siswa.getSekolah().getSatuanKerja() != null) {
			tidakSesuai = !siswa.getSekolah().getSatuanKerja().getId().equals(targetId);
		} else if (users != null && users.ambilSatuanKerja() != null) {
			tidakSesuai = !users.ambilSatuanKerja().getId().equals(targetId);
		}

		if (users != null && users.getGuru() != null && sekolah != null && sekolah.getId() != null
				&& users.getGuru().ambilSekolahs().contains(sekolah.getId())) {
			tidakSesuai = false;
		}

		return !tidakSesuai;
	}

	private static boolean checkBlokirDanKuotaDosen(Tbmuser users, HttpServletRequest req, HttpServletResponse res) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();

			// Persist pegawai bila belum ada
			if (users.getDosen().getPegawaiId() == null) {
				Pegawai pegawai = Pegawai.createDataPegawaiDariDosen(session, users.getDosen());
				users.setPegawai(pegawai);
			}

			Number diblokirCount = (Number) session.createCriteria(AnggotaYangDiblokir.class)
					.add(Restrictions.eq("tidakBisaLogin", true)).createAlias("anggota", "anggota")
					.add(Restrictions.eq("anggota.dosen", users.getDosen())).setProjection(Projections.rowCount())
					.uniqueResult();

			if (diblokirCount != null && diblokirCount.intValue() > 0) {
				String info = (String) session.createCriteria(AnggotaYangDiblokir.class)
						.add(Restrictions.eq("tidakBisaLogin", true)).createAlias("anggota", "anggota")
						.add(Restrictions.eq("anggota.dosen", users.getDosen()))
						.setProjection(Projections.property("informasiKeMahasiswaTidakBisaLogin")).uniqueResult();
				return forwardError(req, res, "Akun Anda diblokir oleh perpustakaan karena alasan :<br> " + info);
			}

			if (ConstantValues.aktifkanApakahJumlahLoginDosenDibatasi) {
				Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
				cal.add(Calendar.HOUR_OF_DAY, -3);
				Number onlineCount = (Number) session
						.createCriteria(OnlineUsers.class).add(Restrictions.isNotNull("dosen"))
						.createAlias("accessedUsers", "accessedUsers").add(Restrictions.between("accessedUsers.waktu",
								cal.getTime(), ais.ui.util.WaktuUtil.getDate()))
						.setProjection(Projections.rowCount()).uniqueResult();

				if (onlineCount != null && onlineCount.intValue() >= ConstantValues.nilaiJumlahLoginDosenDibatasi) {
					return forwardError(req, res,
							"Maaf, kapasitas akses maksimal " + ConstantValues.nilaiJumlahLoginDosenDibatasi
									+ " dosen. Saat ini " + onlineCount.intValue() + " dosen aktif. Harap tunggu.");
				}
			}
			return true;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return true;
		} finally {
			closeSessionSafely(session);
		}
	}

	private static boolean checkBlokirDanKuotaSiswa(Siswa siswa, HttpServletRequest req, HttpServletResponse res) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Number diblokirCount = (Number) session.createCriteria(AnggotaYangDiblokir.class)
					.add(Restrictions.eq("tidakBisaLogin", true)).createAlias("anggota", "anggota")
					.add(Restrictions.eq("anggota.siswa", siswa)).setProjection(Projections.rowCount()).uniqueResult();

			if (diblokirCount != null && diblokirCount.intValue() > 0) {
				String info = (String) session.createCriteria(AnggotaYangDiblokir.class)
						.add(Restrictions.eq("tidakBisaLogin", true)).createAlias("anggota", "anggota")
						.add(Restrictions.eq("anggota.siswa", siswa))
						.setProjection(Projections.property("informasiKeSiswaTidakBisaLogin")).uniqueResult();
				return forwardError(req, res, "Akun Anda diblokir oleh perpustakaan karena alasan :<br> " + info);
			}

			if (ConstantValues.aktifkanApakahJumlahLoginDibatasi) {
				Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
				cal.add(Calendar.HOUR_OF_DAY, -3);
				Number onlineCount = (Number) session
						.createCriteria(OnlineUsers.class).add(Restrictions.isNotNull("siswa"))
						.createAlias("accessedUsers", "accessedUsers").add(Restrictions.between("accessedUsers.waktu",
								cal.getTime(), ais.ui.util.WaktuUtil.getDate()))
						.setProjection(Projections.rowCount()).uniqueResult();

				if (onlineCount != null && onlineCount.intValue() >= ConstantValues.nilaiJumlahLoginDibatasi) {
					return forwardError(req, res,
							"Maaf, kapasitas akses maksimal " + ConstantValues.nilaiJumlahLoginDibatasi
									+ " siswa. Saat ini " + onlineCount.intValue() + " siswa aktif. Harap tunggu.");
				}
			}
			return true;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return true;
		} finally {
			closeSessionSafely(session);
		}
	}

	private static boolean checkBlokirDanKuotaMahasiswa(Mahasiswa mahasiswa, HttpServletRequest req,
			HttpServletResponse res) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Number diblokirCount = (Number) session.createCriteria(AnggotaYangDiblokir.class)
					.add(Restrictions.eq("tidakBisaLogin", true)).createAlias("anggota", "anggota")
					.add(Restrictions.eq("anggota.mahasiswa", mahasiswa)).setProjection(Projections.rowCount())
					.uniqueResult();

			if (diblokirCount != null && diblokirCount.intValue() > 0) {
				String info = (String) session.createCriteria(AnggotaYangDiblokir.class)
						.add(Restrictions.eq("tidakBisaLogin", true)).createAlias("anggota", "anggota")
						.add(Restrictions.eq("anggota.mahasiswa", mahasiswa))
						.setProjection(Projections.property("informasiKeMahasiswaTidakBisaLogin")).uniqueResult();
				return forwardError(req, res, "Akun Anda diblokir oleh perpustakaan karena alasan :<br> " + info);
			}

			if (ConstantValues.aktifkanApakahJumlahLoginDibatasi) {
				Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
				cal.add(Calendar.HOUR_OF_DAY, -3);
				Number onlineCount = (Number) session
						.createCriteria(OnlineUsers.class).add(Restrictions.isNotNull("mahasiswa"))
						.createAlias("accessedUsers", "accessedUsers").add(Restrictions.between("accessedUsers.waktu",
								cal.getTime(), ais.ui.util.WaktuUtil.getDate()))
						.setProjection(Projections.rowCount()).uniqueResult();

				if (onlineCount != null && onlineCount.intValue() >= ConstantValues.nilaiJumlahLoginDibatasi) {
					return forwardError(req, res,
							"Maaf, kapasitas akses maksimal " + ConstantValues.nilaiJumlahLoginDibatasi
									+ " mahasiswa. Saat ini " + onlineCount.intValue()
									+ " mahasiswa aktif. Harap tunggu.");
				} else {
					String tahunAngkatanStr = Common.getKonfigurasi("mahasiswa_tidak_bisa_login", "",
							mahasiswa.currentSemester(), mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(),
							mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa()).getNilai();

					if (tahunAngkatanStr != null && !tahunAngkatanStr.trim().isEmpty()) {
						for (String ta : tahunAngkatanStr.split(";")) {
							if (!ta.trim().isEmpty() && Common.isNumber(ta.trim())) {
								if (Integer.valueOf(ta.trim()).equals(mahasiswa.getTahunangkatan())) {
									return forwardError(req, res,
											"Maaf, untuk saat ini Anda tidak diizinkan login. Harap menghubungi bagian admin atau akademik.");
								}
							}
						}
					}
				}
			}
			return true;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return true;
		} finally {
			closeSessionSafely(session);
		}
	}
}
