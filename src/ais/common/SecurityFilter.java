package ais.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.filter.GenericFilterBean;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.AccessedUsers;
import ais.database.model.LogLogin;
import ais.database.model.Mahasiswa;
import ais.database.model.OnlineUsers;
import ais.database.model.Tbmuser;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sisdes.Penduduk;
import nl.captcha.Captcha;

public class SecurityFilter extends GenericFilterBean {

	public static Map<String, Object[]> dataLogin = new HashMap<String, Object[]>();
	// ConcurrentHashMap (bukan HashMap): dataOnline dibaca/diubah dari BANYAK
	// thread bersamaan — request handler (FilterJSP/LogoutListener/SessionCounter
	// menulis saat login/logout) dan Timer thread UserOnlineCounter.check() yang
	// mengiterasi values() secara berkala. HashMap biasa memicu
	// ConcurrentModificationException saat iterasi bersamaan modifikasi.
	public static Map<String, OnlineUsers> dataOnline = new java.util.concurrent.ConcurrentHashMap<String, OnlineUsers>();

	public SecurityFilter() {
		super();
	}

	@Override
	public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2)
			throws IOException, ServletException {

		// Parameter URL bahasa: ?lang=ar (atau id/en/zh; juga menerima ?bahasa=). Dijalankan LEBIH DULU pada
		// tiap request sehingga bahasa langsung aktif untuk render halaman — TIDAK bergantung persist DB.
		terapkanBahasaDariUrl(arg0, arg1);

		if (ConstantValues.aktifkanLoginHanyaViaMediaSocial) {
			arg2.doFilter(arg0, arg1);
			return;
		}

		try {
			String rawUsername = arg0.getParameter("j_username");
			String rawPassword = arg0.getParameter("j_password");

			if (rawUsername != null && rawPassword != null && !rawUsername.trim().isEmpty()
					&& !rawPassword.trim().isEmpty()) {
				String username = rawUsername.trim();
				String password = rawPassword.trim();
				dataLogin.remove(username);

				HttpServletRequest req = (HttpServletRequest) arg0;
				HttpServletResponse res = (HttpServletResponse) arg1;

				// Validasi Google reCAPTCHA
				if (ConstantValues.aktifkanRecapcha) {
					String gRecaptchaResponse = arg0.getParameter("g-recaptcha-response");
					boolean verify = gRecaptchaResponse != null && VerifyRecaptcha.verify(gRecaptchaResponse);
					if (!verify) {
						req.getRequestDispatcher(ConstantValues.recapchaHome
								+ "?login_error=Untuk+mencegah+terjadinya+autentikasi+yang+tidak+legal,+captcha+harus+Anda+pilih+!")
								.forward(req, res);
						return;
					}
				}

				// Validasi Captcha Lokal
				if (ConstantValues.aktifkanCaptchaLokal) {
					try {
						Captcha captcha = (Captcha) req.getSession().getAttribute(Captcha.NAME);
						req.setCharacterEncoding("UTF-8");
						String answer = req.getParameter("answer");

						if (captcha == null || !captcha.isCorrect(answer)) {
							req.getRequestDispatcher(ConstantValues.recapchaHome
									+ "?login_error=Untuk+mencegah+terjadinya+autentikasi+yang+tidak+legal,+captcha+harus+Anda+isi+dengan+benar+!")
									.forward(req, res);
							return;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SecurityFilter.java:98");
						// Abaikan atau log error jika diperlukan
					}
				}

				SecurityFilter.doingFilter(username, password, false, null, arg0, arg1);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SecurityFilter.java:105");
			// e.printStackTrace();
		}

		arg2.doFilter(arg0, arg1);
	}

	public static void saveOnlineData(LogLogin login, HttpServletRequest arg0) {
		// OnlineUsers mereferensikan LogLogin. Bila login masih TRANSIENT (belum punya id), baik query
		// Restrictions.eq("login", login) maupun saveOrUpdate(onlineUsers) akan melempar
		// TransientObjectException ("object references an unsaved transient instance ... LogLogin").
		// Lewati pencatatan online untuk request ini; akan tercatat pada request berikutnya saat login
		// sudah persisten. Tidak mematikan fungsi: data online hanya bersifat presence sementara.
		if (login == null || login.getId() == null) {
			return;
		}
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.openSession();
			AccessedUsers accessedUsers = Common.setUserAccess(session, arg0);
			// Bila accessedUsers null (gagal di-persist karena koneksi/pool), lewati
			// OnlineUsers agar tidak terjadi FK violation "Key (accessed_users) not present".
			if (accessedUsers == null || accessedUsers.getNama() == null) {
				return;
			}

			OnlineUsers onlineUsers = (OnlineUsers) session.createCriteria(OnlineUsers.class)
					.add(Restrictions.eq("login", login)).setMaxResults(1).uniqueResult();

			if (onlineUsers == null) {
				onlineUsers = new OnlineUsers();
			}

			Tbmuser users = login.getTbmuser();
			Mahasiswa mahasiswa = login.getMahasiswa();
			Siswa siswa = login.getSiswa();
			Penduduk penduduk = login.getPenduduk();

			// Setup data dasar
			onlineUsers.setAccessedUsers(accessedUsers);
			onlineUsers.setHost(Common.getRequestHost(arg0));
			onlineUsers.setLogin(login);

			// Kondisi untuk hierarki relasi entitas
			if (users != null) {
				onlineUsers.setDosen(users.getDosen());
				onlineUsers.setFakultas(users.ambilFakultas());
				onlineUsers.setJurusan(users.ambilJurusan());
				onlineUsers.setSekolah(users.ambilSekolah());
				onlineUsers.setYayasan(users.ambilYayasan());
				onlineUsers.setTbmuser(users);
				onlineUsers.setNama(users.getUserId());
			} else if (siswa != null) {
				onlineUsers.setSekolah(siswa.getSekolah());
				onlineUsers.setYayasan(siswa.getSekolah() != null ? siswa.getSekolah().getYayasan() : null);
				onlineUsers.setSiswa(siswa);
				onlineUsers.setNama(siswa.getNomorIndukNasional());
			}

			onlineUsers.setMahasiswa(mahasiswa);
			if (mahasiswa != null && onlineUsers.getNama() == null) {
				onlineUsers.setNama(mahasiswa.getNim());
			}

			onlineUsers.setPenduduk(penduduk);
			if (penduduk != null) {
				onlineUsers.setNama(penduduk.getKode());
			}

			tx = session.beginTransaction();
			session.saveOrUpdate(onlineUsers);
			tx.commit();

			dataOnline.put(accessedUsers.getNama(), onlineUsers);

		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/SecurityFilter.java:185");
					// Abaikan exception saat rollback
				}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static boolean doingFilter(String username, String password, boolean mobile, String linkProfile,
			ServletRequest arg0, ServletResponse arg1) {
		return FilterLoginAis.doingFilter(username, password, mobile, linkProfile, arg0, arg1);
	}

	public static boolean doAutoLogin(String username, String password, boolean mobile, String linkProfile) {
		if (ExecutionsCtrl.getCurrent() != null) {
			HttpServletRequest request = null;
			HttpServletResponse response = null;

			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
				response = (HttpServletResponse) ExecutionsCtrl.getCurrent().getNativeResponse();
			}

			if (request == null) {
				request = RequestContext.get();
				response = ResponseContext.get();
			}

			System.out.println("[SOCIAL-LOGIN] doAutoLogin(" + username + "): request=" + (request == null ? "NULL" : "ada")
					+ ", response=" + (response == null ? "NULL" : "ada") + " (via ExecutionsCtrl.getCurrent()!=null)");
			return doAutoLogin(username, password, mobile, linkProfile, request, response);
		} else {
			HttpServletRequest request = RequestContext.get();
			HttpServletResponse response = ResponseContext.get();
			System.out.println("[SOCIAL-LOGIN] doAutoLogin(" + username + "): request=" + (request == null ? "NULL" : "ada")
					+ ", response=" + (response == null ? "NULL" : "ada") + " (via RequestContext/ResponseContext, "
					+ "ExecutionsCtrl.getCurrent()==null)");
			return doAutoLogin(username, password, mobile, linkProfile, request, response);
		}
	}

	public static boolean doAutoLogin(String username, String password, boolean mobile, String linkProfile,
			HttpServletRequest request, HttpServletResponse response) {
		boolean hasil = false;
		try {
			hasil = SecurityFilter.doingFilter(username, password, mobile, linkProfile, request, response);
			System.out.println("[SOCIAL-LOGIN] doAutoLogin(" + username + "): hasil doingFilter=" + hasil);

			if (hasil) {
				UserDetails user = UserDetailsServiceImpl.getUserDetails(username);
				if (user == null) {
					System.out.println("[SOCIAL-LOGIN] doAutoLogin(" + username
							+ "): doingFilter SUKSES tapi UserDetailsServiceImpl.getUserDetails() balik NULL "
							+ "-- SecurityContext TIDAK akan diisi, akan redirect ke home dari catch di bawah.");
					throw new UsernameNotFoundException("User does not exist");
				}

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user,
						user.getPassword(), user.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
				System.out.println("[SOCIAL-LOGIN] doAutoLogin(" + username
						+ "): SecurityContextHolder authentication BERHASIL diisi.");
			} else {
				System.out.println("[SOCIAL-LOGIN] doAutoLogin(" + username
						+ "): doingFilter mengembalikan FALSE -- SecurityContext TIDAK diisi, login GAGAL di jalur ini.");
			}
		} catch (Exception e) {
			System.out.println("[SOCIAL-LOGIN] doAutoLogin(" + username + "): EXCEPTION -- " + e
					+ " -- SecurityContext dikosongkan & redirect paksa ke home dari sini (bukan dari pemanggil).");
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/SecurityFilter.java:241");
			SecurityContextHolder.getContext().setAuthentication(null);
			ExecutionsCtrl.sendRedirect(Common.getRequestHostWithProtocol());
		}
		return hasil;
	}

	public static Tbmuser getCurrentFromSpringUser() {
		return getCurrentFromSpringUser(getActiveRequest());
	}

	public static Tbmuser getCurrentFromSpringUser(HttpServletRequest request) {
		try {
			if (SecurityContextHolder.getContext().getAuthentication() != null) {
				Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
				if (principal instanceof UserDetails) {
					return getCurrentFromUsername(((UserDetails) principal).getUsername(), request, true);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SecurityFilter.java:260");
			// Abaikan atau log error
		}
		return null;
	}

	public static Tbmuser getCurrentFromUsername(String userName) {
		return getCurrentFromUsername(userName, getActiveRequest(), true);
	}

	public static Tbmuser getCurrentFromUsername(String userName, HttpServletRequest request,
			boolean logoutJikaTidakAda) {
		if (userName == null || userName.trim().isEmpty()) {
			return null;
		}

		try {
			if (request != null) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					Tbmuser tbmuserDiSession = (Tbmuser) session.getAttribute("mytbmuser");
					if (tbmuserDiSession != null) {
						return tbmuserDiSession;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/SecurityFilter.java:287");
		}

		Object[] obj = SecurityFilter.dataLogin.get(userName);
		if (obj == null || obj.length < 2) {
			if (logoutJikaTidakAda) {
				Common.goLogoff();
			}
			return null;
		}

		Object entityObj = obj[0];
		LogLogin login = (LogLogin) obj[1];

		SecurityFilter.saveOnlineData(login, request);

		Tbmuser users = null;
		try {
			PesertaKursus pesertaKursus = findPesertaKursusByEntity(entityObj);

			if (entityObj instanceof Penduduk) {
				Penduduk penduduk = (Penduduk) entityObj;
				users = new Tbmuser();
				users.setPenduduk(penduduk);
				users.setUserNama(penduduk.getKode());
				users.setUserId(penduduk.getKode());
				users.setUserRole(ConstantValues.tbmrolePenduduk);
				users.setBahasa(Tbmuser.INDONESIA);

			} else if (entityObj instanceof Siswa) {
				Siswa siswa = (Siswa) entityObj;
				users = new Tbmuser();
				users.setSiswa(siswa);
				users.setUserNama(siswa.getNomorInduk());
				users.setUserId(siswa.getNomorInduk());
				users.setUserRole(ConstantValues.tbmroleSiswa);
				users.setYayasan(siswa.getSekolah() != null ? siswa.getSekolah().getYayasan() : null);
				users.setSekolah(siswa.getSekolah());
				users.setBahasa(siswa.getBahasa());

			} else if (entityObj instanceof Mahasiswa) {
				Mahasiswa mahasiswa = (Mahasiswa) entityObj;
				users = new Tbmuser();
				users.setMahasiswa(mahasiswa);
				users.setUserNama(mahasiswa.getNim());
				users.setUserId(mahasiswa.getNim());
				users.setUserRole(ConstantValues.tbmroleMahasiswa);
				users.setFakultas(mahasiswa.getJurusan() != null ? mahasiswa.getJurusan().getFakultas() : null);
				users.setJurusan(mahasiswa.getJurusan());
				users.setBahasa(mahasiswa.getBahasa());

			} else if (entityObj instanceof Tbmuser) {
				Tbmuser tempUser = (Tbmuser) entityObj;
				users = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), tempUser.getUserId());
				if (users == null) {
					users = tempUser;
				}
			}

			if (users != null) {
				users.setPesertaKursus(pesertaKursus);
				saveUserToSessions(users, login, request);
			}

			return users;

		} finally {
			// Blok ini dibiarkan sebagai pelindung jika ada operasi I/O stream / Connection
			// tambahan yang perlu dirilis di masa mendatang (bukan HTTP session).
		}
	}

	// --- HELPER METHODS --- //

	private static HttpServletRequest getActiveRequest() {
		try {
			if (ExecutionsCtrl.getCurrent() != null) {
				return (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}
			return RequestContext.get();
		} catch (Exception e) {
			return null;
		}
	}

	private static PesertaKursus findPesertaKursusByEntity(Object entity) {
		if (entity == null)
			return null;

		try {
			for (Object o : ConstantValues.ambilBerdasarClass(PesertaKursus.class).values()) {
				PesertaKursus p = (PesertaKursus) o;
				if (p == null)
					continue;

				if (entity instanceof Siswa && p.getSiswa() != null) {
					if (isIdEqual(p.getSiswa().getId(), ((Siswa) entity).getId()))
						return p;
				} else if (entity instanceof Mahasiswa && p.getMahasiswa() != null) {
					if (isIdEqual(p.getMahasiswa().getId(), ((Mahasiswa) entity).getId()))
						return p;
				} else if (entity instanceof Tbmuser && p.getTbmuser() != null) {
					if (isIdEqual(p.getTbmuser().getUserId(), ((Tbmuser) entity).getUserId()))
						return p;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SecurityFilter.java:393");
			// Abaikan
		}
		return null;
	}

	private static boolean isIdEqual(Object id1, Object id2) {
		return id1 != null && id2 != null && id1.equals(id2);
	}

	private static void saveUserToSessions(Tbmuser users, LogLogin login, HttpServletRequest request) {
		try {
			String bahasaDb = users.getBahasa() != null ? users.getBahasa() : Tbmuser.INDONESIA;

			if (Sessions.getCurrent() != null) {
				Sessions.getCurrent().setAttribute("usersTemp", users);
				// JANGAN timpa pilihan bahasa yang sudah aktif di sesi (mis. baru dipilih pengguna) — hanya
				// set dari DB bila sesi belum punya current_lang. Mencegah bendera/menu ter-reset ke Indonesia
				// saat cache user dibangun ulang di tengah sesi.
				if (kosong(Sessions.getCurrent().getAttribute("current_lang"))) {
					Sessions.getCurrent().setAttribute("current_lang", bahasaDb);
				}
				Sessions.getCurrent().setAttribute("login", login);
			}

			if (request != null) {
				HttpSession sess = request.getSession();
				sess.setAttribute("usersTemp", users);
				if (kosong(sess.getAttribute("current_lang"))) {
					sess.setAttribute("current_lang", bahasaDb);
				}
				sess.setAttribute("login", login);
				sess.setAttribute("mytbmuser", users);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/SecurityFilter.java:428");
		}
	}

	private static boolean kosong(Object o) {
		return o == null || o.toString().trim().isEmpty();
	}

	/**
	 * Terapkan bahasa dari parameter URL ({@code ?lang=} atau {@code ?bahasa=}, nilai id/en/ar/zh atau nama
	 * lengkap). Menyetel {@code current_lang} pada HttpSession (dan ZK Session bila tersedia) sehingga halaman
	 * langsung dirender dalam bahasa tsb. Nilai menetap di sesi untuk request berikutnya walau parameter
	 * dilepas. Bila pengguna login, pilihan juga di-persist permanen (best-effort). Tanpa parameter → no-op.
	 */
	private static void terapkanBahasaDariUrl(ServletRequest req, ServletResponse res) {
		try {
			String lang = req.getParameter("lang");
			if (kosong(lang)) {
				lang = req.getParameter("bahasa");
			}
			if (kosong(lang)) {
				return;
			}
			final String currentLang = petakanBahasa(lang.trim());
			if (currentLang == null) {
				return;
			}

			// 1) HttpSession — dasar yang dibaca ZK Session & JSP.
			if (req instanceof HttpServletRequest) {
				try {
					HttpSession sess = ((HttpServletRequest) req).getSession(true);
					sess.setAttribute("current_lang", currentLang);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SecurityFilter.java:461");
				}
			}
			// 2) ZK Session (bila thread ini sudah punya konteks ZK).
			try {
				if (Sessions.getCurrent() != null) {
					Sessions.getCurrent().setAttribute("current_lang", currentLang);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SecurityFilter.java:469");
			}
			// 3) Persist permanen bila login (agar menetap walau parameter dilepas & lintas sesi).
			try {
				Tbmuser u = Common.getCurrentUser();
				if (u != null && u.getId() != null) {
					Common.initBahasaParameter(currentLang);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SecurityFilter.java:477");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SecurityFilter.java:479");
		}
	}

	/** Petakan kode/nama bahasa → konstanta Tbmuser; null bila tak dikenal. */
	private static String petakanBahasa(String kode) {
		if (kode == null) {
			return null;
		}
		String k = kode.trim();
		if (k.equalsIgnoreCase("id") || k.equalsIgnoreCase(Tbmuser.INDONESIA) || k.equalsIgnoreCase("indonesia")) {
			return Tbmuser.INDONESIA;
		}
		if (k.equalsIgnoreCase("en") || k.equalsIgnoreCase(Tbmuser.ENGLISH) || k.equalsIgnoreCase("english")) {
			return Tbmuser.ENGLISH;
		}
		if (k.equalsIgnoreCase("ar") || k.equalsIgnoreCase(Tbmuser.ARAB) || k.equalsIgnoreCase("arab")
				|| k.equalsIgnoreCase("arabic")) {
			return Tbmuser.ARAB;
		}
		if (k.equalsIgnoreCase("zh") || k.equalsIgnoreCase(Tbmuser.MANDARIN) || k.equalsIgnoreCase("mandarin")
				|| k.equalsIgnoreCase("china") || k.equalsIgnoreCase("chinese")) {
			return Tbmuser.MANDARIN;
		}
		return null;
	}
}