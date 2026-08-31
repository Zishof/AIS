package ais.common;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.AccessedUsers;
import ais.database.model.LogLogin;
import ais.database.model.OnlineUsers;
import ais.database.model.Tbmuser;

/**
 * Listener lifecycle aplikasi/web untuk session counter. Tipe ini bereaksi terhadap startup,
 * session, atau logout dan mengelola state global terkait tanpa mengambil alih aturan bisnis
 * request.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini mendeklarasikan kontrak {@link HttpSessionListener}. Implementasi
 * konkret bertanggung jawab atas transaksi, resource, error handling, dan efek samping; pemanggil sebaiknya
 * bergantung pada kontrak ini agar tidak menggandakan integrasi.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code boolean LOG_VERBOSE}, {@code int
 * SESSION_POOL_SIZE}, {@code java.util.concurrent.ExecutorService SESSION_POOL}; inisialisasi/lifecycle ({@code
 * initSessionTimeout()}); operasi domain lain ({@code logSesi()}, {@code sessionCreated()}, {@code
 * sessionDestroyed()}, {@code hentikanPool()}, {@code bersihkanRegistryPerUser()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> callback berjalan pada lifecycle container dan dapat mengubah session, response,
 * penghitung global, atau resource aplikasi. Implementasi harus thread-safe dan tidak menyimpan data pengguna
 * request pada field singleton.</p>
 */
public class SessionCounter implements HttpSessionListener {

	// Log verbose banner sesi (SESSION CREATED/DESTROYED/TUTUP). DEFAULT MATI: dulu tiap create/destroy
	// mencetak banner ke System.out; saat sesi kedaluwarsa massal, ratusan SessionDeleter men-serial di
	// monitor java.io.PrintStream (snapshot 07-08/07: 120-187 thread BLOCKED di println SessionCounter:104).
	// Nyalakan dgn -Dsession.log.verbose=true bila perlu debug.
	private static final boolean LOG_VERBOSE = "true"
			.equalsIgnoreCase(System.getProperty("session.log.verbose", "false").trim());

	private static void logSesi(String msg) {
		if (LOG_VERBOSE) {
			System.out.println(msg);
		}
	}

	// Pool TETAP daemon utk hapus sesi (pengganti `new Thread(...).start()` per session-destroy). DULU
	// tiap logout/expiry melahirkan raw-thread (snapshot: grup Thread-# s/d 193, puncak 1670 thread,
	// total dimulai ~530rb); saat expiry massal ratusan lahir & BLOCKED di println. Sekarang terbatas +
	// antrean + backpressure (CallerRunsPolicy). Ukuran via -Dsession.pool.size (default 3).
	private static final int SESSION_POOL_SIZE;
	static {
		int n = 3;
		try {
			n = Integer.parseInt(System.getProperty("session.pool.size", "3").trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SessionCounter.java:39");
		}
		SESSION_POOL_SIZE = n < 1 ? 1 : n;
	}

	private static final java.util.concurrent.ExecutorService SESSION_POOL = new java.util.concurrent.ThreadPoolExecutor(
			SESSION_POOL_SIZE, SESSION_POOL_SIZE, 60L, java.util.concurrent.TimeUnit.SECONDS,
			new java.util.concurrent.ArrayBlockingQueue<Runnable>(2000), new java.util.concurrent.ThreadFactory() {
				private final java.util.concurrent.atomic.AtomicInteger seq = new java.util.concurrent.atomic.AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "session-deleter-" + seq.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			}, new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

	public SessionCounter() {

	}

	public void sessionCreated(HttpSessionEvent event) {

	}

	public static void initSessionTimeout(HttpSession session, Tbmuser tbmuser, boolean mobile) {
		Integer loginTimeout;

		if (mobile) {
			loginTimeout = 30;
			session.setMaxInactiveInterval(loginTimeout * 60);
		} else {

			if (tbmuser == null || tbmuser.getUserId() == null) {
				loginTimeout = 30;
				session.setMaxInactiveInterval(loginTimeout * 60); // in
				// seconds
			} else if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				loginTimeout = 10;
				try {
					loginTimeout = Integer
							.parseInt(Common.getKonfigurasi("session_timeout_mahasiswa", "10").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SessionCounter.java:82");

				}
				session.setMaxInactiveInterval(loginTimeout * 60); // in
																	// seconds
			} else if (tbmuser != null
					&& (tbmuser.getBiodataCalonMahasiswa() != null || tbmuser.getCalonSiswa() != null)) {
				loginTimeout = 20;
				try {
					loginTimeout = Integer
							.parseInt(Common.getKonfigurasi("session_timeout_calon", "20").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SessionCounter.java:93");

				}
				session.setMaxInactiveInterval(loginTimeout * 60); // in
																	// seconds
			} else if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				loginTimeout = 60;
				try {
					loginTimeout = Integer
							.parseInt(Common.getKonfigurasi("session_timeout_dosen", "60").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SessionCounter.java:104");

				}
				session.setMaxInactiveInterval(loginTimeout * 60); // in
																	// seconds
			} else {
				loginTimeout = 120;
				try {
					loginTimeout = Integer
							.parseInt(Common.getKonfigurasi("session_timeout_admin", "120").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SessionCounter.java:114");

				}
				session.setMaxInactiveInterval(loginTimeout * 60); // in
																	// seconds
			}
		}

		logSesi(
				"=============================================================== SESSION CREATED, SESSION ID = "
						+ session.getId() + " tbmuser = " + tbmuser + ", timout " + loginTimeout
						+ " ====================================================================== ");
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		HttpSession session = event.getSession();
		SESSION_POOL.execute(new SessionDeleter(session.getId()));
	}

	/**
	 * Hentikan worker penghapus sesi saat webapp dihentikan/redeploy.
	 *
	 * <p>Pool ini statis sehingga harus ditutup eksplisit agar thread
	 * {@code session-deleter-*} tidak menahan classloader webapp lama. Task yang sudah
	 * mengantre diberi waktu singkat untuk membersihkan registry in-memory; akses DB
	 * akan dilewati karena listener lebih dahulu menandai aplikasi sedang berhenti.</p>
	 */
	public static void hentikanPool() {
		SESSION_POOL.shutdown();
		try {
			if (!SESSION_POOL.awaitTermination(10L, java.util.concurrent.TimeUnit.SECONDS)) {
				SESSION_POOL.shutdownNow();
				SESSION_POOL.awaitTermination(5L, java.util.concurrent.TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			SESSION_POOL.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Pembersihan terpusat registry statis ber-key user saat sebuah sesi dihancurkan
	 * (optimasi RAM Fase 1 — sebelumnya entri-entri ini tidak pernah dihapus dan tumbuh
	 * monoton sepanjang umur JVM).
	 *
	 * <p>{@code MainHelper.logins} ber-key ID {@code LogLogin} yang unik per event login,
	 * jadi aman dihapus tanpa syarat. {@code dataLogin} dan map UI per-user hanya dihapus
	 * bila TIDAK ADA sesi aktif lain milik username yang sama (dukungan multi-perangkat):
	 * {@code dataLogin} masih dipakai sebagai fallback identitas oleh
	 * {@code SecurityFilter.getUser} selama sesi lain hidup.</p>
	 */
	private static void bersihkanRegistryPerUser(OnlineUsers onlineUsers) {
		LogLogin login = onlineUsers.getLogin();
		if (login != null && login.getId() != null) {
			ais.action.master.helper.MainHelper.logins.remove(login.getId());
		}

		String username = login == null ? null : login.getNama();
		if (username == null) {
			return;
		}
		for (OnlineUsers lain : SecurityFilter.dataOnline.values()) {
			LogLogin loginLain = lain.getLogin();
			if (loginLain != null && username.equals(loginLain.getNama())) {
				// Masih ada sesi aktif lain milik user ini — jangan sentuh registry per-user.
				return;
			}
		}

		SecurityFilter.dataLogin.remove(username);

		String userId = onlineUsers.getTbmuser() == null ? null : onlineUsers.getTbmuser().getUserId();
		if (userId != null) {
			ais.action.maintenance.MainAction.desktopWidths.remove(userId);
			ais.action.maintenance.MainAction.desktopHeights.remove(userId);
			ais.action.maintenance.MainAction2.desktopWidths.remove(userId);
			ais.action.maintenance.MainAction2.desktopHeights.remove(userId);
			ais.ui.util.ChatThread chat = ais.action.maintenance.MainAction.mapChat.remove(userId);
			if (chat != null) {
				try {
					chat.onExit();
				} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "SessionCounter.hentikanChatThread");
				}
			}
			ais.ui.util.ChatThread chat2 = ais.action.maintenance.MainAction2.mapChat.remove(userId);
			if (chat2 != null) {
				try {
					chat2.onExit();
				} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "SessionCounter.hentikanChatThread2");
				}
			}
		}
	}

	private class SessionDeleter implements Runnable {

		private String sessionId;

		public SessionDeleter(String sessionId) {
			this.sessionId = sessionId;
		}

		@Override
		public void run() {
			try {

			logSesi(
					"=============================================================== SESSION DESTROYED, SESSION ID = "
							+ sessionId
							+ " ====================================================================== ");

			// PEMBERSIHAN TERPUSAT registry in-memory ber-key session (optimasi RAM Fase 1).
			// Common.mapSession dipakai juga oleh sesi ANONIM (form PMB/PPDB) yang tidak
			// terdaftar di dataOnline — jadi harus dibersihkan SEBELUM cek onlineUsers null.
			try {
				Common.hapusSessionById(sessionId);
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "SessionCounter.bersihkanMapSession");
			}

			OnlineUsers onlineUsers = SecurityFilter.dataOnline.remove(sessionId);
			if (onlineUsers == null) {
				return;
			}

			try {
				bersihkanRegistryPerUser(onlineUsers);
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "SessionCounter.bersihkanRegistryPerUser");
			}

			logSesi(
					"=============================================================== SESSION DESTROYED, SESSION ID = "
							+ sessionId + " " + onlineUsers.getNama()
							+ " ====================================================================== ");

			/*
			 * Thread background tanpa konteks ZK: gunakan openSession() khusus dan
			 * SELALU tutup di finally dengan clear/disconnect/close (sesuai aturan sesi).
			 *
			 * Versi lama memakai currentNativeSession() lalu pada catch memanggil
			 * HibernateUtil.rollbackTransaction() yang IKUT menutup sesi ThreadLocal,
			 * kemudian blok finally menutup lagi (disconnect/close/closeSession) ->
			 * close ganda. Sesi yang sudah tertutup ini lalu dipakai lagi pada iterasi
			 * while(true) berikutnya sehingga muncul "Session is closed!". Selain itu
			 * thread yang hidup selamanya (sleep 60 detik lalu loop) menahan koneksi
			 * fisik TLS dan ikut memicu "Cannot reuse iv for GCM encryption" ketika
			 * koneksi tersebut bertabrakan dengan thread request.
			 *
			 * Sekarang: satu transaksi untuk seluruh pekerjaan, retry terbatas (bukan
			 * tak berujung), setiap percobaan memakai sesi sendiri yang ditutup rapi.
			 */
			// Saat aplikasi/webapp berhenti, pool koneksi sudah/akan ditutup. Lewati
			// penghapusan sesi DB agar tidak memunculkan "has been closed" dan menahan
			// thread saat shutdown (status logout akan tersinkron saat start ulang).
			if (Common.aplikasiSedangBerhenti) {
				return;
			}

			int maksimalPercobaan = 3;
			for (int percobaan = 1; percobaan <= maksimalPercobaan; percobaan++) {

				Session mySession = HibernateUtil.openSession();
				org.hibernate.Transaction tx = null;
				boolean sukses = false;
				try {
					tx = mySession.beginTransaction();

					/*
					 * Objek di SecurityFilter.dataOnline adalah entity DETACHED dari sesi
					 * request yang membuatnya. sessionDestroyed juga dapat terpanggil dua
					 * kali/race dengan pembersihan startup. delete/update entity detached
					 * mengharuskan tepat satu baris berubah; bila baris sudah hilang Hibernate
					 * melempar StaleStateException (expected 1, actual 0). Gunakan bulk SQL
					 * idempoten: 0 baris berarti pekerjaan sudah dilakukan thread lain.
					 */
					LogLogin login = onlineUsers.getLogin();
					if (login != null && login.getId() != null) {
						mySession.createSQLQuery("UPDATE public.log_login SET logout=:logout WHERE id=:id")
								.setParameter("logout", ais.ui.util.WaktuUtil.getDate())
								.setParameter("id", login.getId()).executeUpdate();
					}

					AccessedUsers accessedUsers = onlineUsers.getAccessedUsers();
					logSesi("Remove User Online " + onlineUsers.getNama());
					if (onlineUsers.getId() != null) {
						mySession.createSQLQuery("DELETE FROM public.online_users WHERE id=:id")
								.setParameter("id", onlineUsers.getId()).executeUpdate();
					}
					if (accessedUsers != null && accessedUsers.getNama() != null) {
						mySession.createSQLQuery("DELETE FROM public.accessed_users au WHERE au.nama=:nama "
								+ "AND NOT EXISTS (SELECT 1 FROM public.online_users ou WHERE ou.accessed_users=au.nama)")
								.setParameter("nama", accessedUsers.getNama()).executeUpdate();
					}
					tx.commit();
					sukses = true;
				} catch (Exception e) {
					if (tx != null) {
						try {
							if (tx.isActive() && !tx.wasCommitted() && !tx.wasRolledBack()) {
								tx.rollback();
							}
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/SessionCounter.java:215");
							// Abaikan agar penutupan sesi tetap berjalan.
						}
					}
					// Saat shutdown, pool koneksi c3p0 sudah ditutup -> "has been closed".
					// Itu noise shutdown: jangan cetak stack trace & jangan retry.
					String pesanErr = e.getMessage();
					boolean tertutupShutdown = Common.aplikasiSedangBerhenti || (pesanErr != null
							&& (pesanErr.contains("has been closed") || pesanErr.contains("already closed")
									|| pesanErr.contains("Cannot open connection")));
					if (tertutupShutdown) {
						System.out.println("SessionCounter: lewati hapus sesi (aplikasi berhenti / pool koneksi sudah ditutup).");
						break;
					}
					// Jaga-jaga (race condition): meski sudah dicek dulu, sesi lain milik
					// user yg sama bisa saja baru dibuat tepat di antara pengecekan dan
					// commit, sehingga hapus AccessedUsers tetap kena FK constraint
					// (fk59b25d1c86625d3c, tabel online_users). Ini bukan error yg akan
					// hilang dgn retry (baris online_users lain itu memang masih valid
					// selama sesi lain masih aktif) -> lewati saja tanpa mengulang.
					boolean fkAccessedUsersMasihDipakai = e instanceof org.hibernate.exception.ConstraintViolationException
							&& pesanErr != null && pesanErr.toLowerCase().contains("accessed_users");
					if (fkAccessedUsersMasihDipakai) {
						System.out.println(
								"SessionCounter: lewati hapus AccessedUsers (masih direferensikan oleh online_users sesi lain yg aktif), session ID = "
										+ sessionId);
						break;
					}
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/SessionCounter.java:229");
				} finally {
					try {
						if (mySession != null && mySession.isOpen()) {
							mySession.clear();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SessionCounter.java:235");
					}
					try {
						if (mySession != null && mySession.isOpen()) {
							mySession.disconnect();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SessionCounter.java:241");
					}
					try {
						if (mySession != null && mySession.isOpen()) {
							mySession.close();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/SessionCounter.java:247");
					}
					logSesi(
							"=============================================================== TUTUP SESSION, SESSION ID = "
									+ sessionId
									+ " ====================================================================== ");
				}

				if (sukses) {
					break;
				}

				try {
					Thread.sleep(5000);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/SessionCounter.java:262");
				}
			}

			// Bersihkan ThreadLocal "berat" (DesEncrypter = class webapp) milik thread ini
			// agar tidak menyandera classloader webapp saat redeploy (warning Tomcat).
			try {
				Common.bersihkanThreadLocalThreadIni();
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/SessionCounter.java:270");
			}
					} finally {
				ais.database.hibernate.HibernateUtil.closeSession();
			}
		}

	}

}
