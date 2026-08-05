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

			OnlineUsers onlineUsers = SecurityFilter.dataOnline.remove(sessionId);
			if (onlineUsers == null) {
				return;
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

					LogLogin login = onlineUsers.getLogin();
					if (login != null) {
						login.setLogout(ais.ui.util.WaktuUtil.getDate());
						Common.refreshUpdate(mySession, login);
					}

					AccessedUsers accessedUsers = onlineUsers.getAccessedUsers();
					logSesi("Remove User Online " + onlineUsers.getNama());
					mySession.delete(onlineUsers);
					// Flush dulu supaya baris online_users ini benar2 hilang dari DB sebelum
					// kita cek sisa referensi (lihat penjelasan di bawah).
					mySession.flush();

					if (accessedUsers != null) {
						// accessed_users.nama adalah PRIMARY KEY (natural key) yg BISA
						// dipakai bersama oleh > 1 baris online_users (mis. user yg sama
						// login dari beberapa tab/device sekaligus -> beberapa sesi HTTP,
						// masing2 dgn baris online_users sendiri, tapi merujuk ke SATU
						// baris accessed_users yg sama via kolom accessed_users(nama)).
						// Versi lama menghapus accessedUsers begitu saja setiap kali salah
						// satu sesi berakhir -> ConstraintViolationException
						// (fk59b25d1c86625d3c) ketika sesi lain milik user yg sama masih
						// aktif dan baris online_users miliknya masih merujuk ke
						// accessed_users tsb. Sekarang: hapus accessedUsers HANYA bila
						// sudah tidak ada lagi baris online_users yg merujuk ke nama yg
						// sama (mis. semua sesi user tsb sudah berakhir).
						Long sisaOnlineUntukNama = (Long) mySession
								.createQuery("select count(*) from OnlineUsers o where o.accessedUsers.nama = :nama")
								.setParameter("nama", accessedUsers.getNama()).uniqueResult();
						if (sisaOnlineUntukNama == null || sisaOnlineUntukNama.longValue() == 0L) {
							mySession.delete(accessedUsers);
						} else {
							logSesi("Lewati hapus AccessedUsers " + accessedUsers.getNama()
									+ ": masih ada " + sisaOnlineUntukNama
									+ " baris online_users lain yg merujuk (sesi lain masih aktif).");
						}
					}

					mySession.flush();
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