package ais.database.hibernate;

/**
 * Created by IntelliJ IDEA.
 * User: 
 * Date: Jan 3, 2007
 * Time: 3:58:15 PM
 */

import java.net.URL;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.metadata.ClassMetadata;

import ais.common.Common;

/**
 * Pengelola SessionFactory dan session Hibernate khusus untuk ojs hibernate util. Utilitas ini
 * memisahkan konfigurasi persistence subsistem tersebut dari Hibernate utama AIS.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code OjsHibernateUtil
 * hibernateStreamingUtil}, {@code SessionFactory sessionFactory}, {@code ThreadLocal MAP}, {@code ThreadLocal
 * MAPFactory}; pembacaan/pencarian ({@code getInstance()}, {@code getClassMetadata()}, {@code
 * getSessionFactory()}); mutasi data ({@code setSessionFactory()}); operasi domain lain ({@code
 * currentSession()}, {@code closeSession()}, {@code rollbackTransaction()}, {@code closeFactoryQuietly()});
 * konfigurasi constructor: {@code sessionFactory}. Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> operasi dapat membuat, mengikat, atau menutup session dan resource Hibernate.
 * Pemanggil wajib mengikuti pasangan buka/tutup serta batas transaksi yang dijelaskan oleh utilitas ini agar
 * koneksi tidak bocor atau session tidak dipakai lintas thread.</p>
 */
@SuppressWarnings("deprecation")
public class OjsHibernateUtil {

	private static OjsHibernateUtil hibernateStreamingUtil = new OjsHibernateUtil("/hibernate.ojs.cfg.xml");

	public static OjsHibernateUtil getInstance() {
		// String wilayah = Common.getCurrentWilayah();
		// if (wilayah.equals("bekasi")) {
		// return hibernateUtilbekasi;
		// } else if (wilayah.equals("jabar")) {
		// return hibernateStreamingUtil;
		// }
		return hibernateStreamingUtil;
	}

	private OjsHibernateUtil(String config) {
		try {
			// Create the SessionFactory
			Configuration configuration = new AnnotationConfiguration();
			URL url = OjsHibernateUtil.class.getResource(config);
			configuration.configure(url);
			// P0 keamanan: kredensial dari berkas eksternal (bila ada) menimpa nilai cfg.xml.
			DbCredentialOverride.terapkan(configuration, "ojs");
			sessionFactory = configuration.buildSessionFactory();
		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	private SessionFactory sessionFactory;

	// public final ThreadLocal<Session> threadSession = new
	// ThreadLocal<Session>();

	// public final ThreadLocal<Transaction> threadTransaction = new
	// ThreadLocal<Transaction>();

	@SuppressWarnings("rawtypes")
	public ClassMetadata getClassMetadata(Class aClass) {
		return getSessionFactory().getClassMetadata(aClass);
	}

	private static final ThreadLocal<Session> MAP = new ThreadLocal<Session>();
	private static final ThreadLocal<SessionFactory> MAPFactory = new ThreadLocal<SessionFactory>();

	public Session currentSession() throws HibernateException {
		Session s = (Session) MAP.get();
		if (s != null && s.isOpen()) {
			return s;
		}
		SessionFactory sessionFactory = getSessionFactory();
		s = sessionFactory.openSession();
		MAP.set(s);
		MAPFactory.set(sessionFactory);
		return s;
	}

	public void closeSession() throws HibernateException {
		Session s = null;
		try {
			s = (Session) MAP.get();
			MAP.set(null);
            try { MAP.remove(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/OjsHibernateUtil.java:83"); }
			MAPFactory.set(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		HibernateUtil.closeSessionQuietly(s);
	}

	// public void beginTransaction() {
	// Transaction tx = threadTransaction.get();
	// if (tx == null) {
	// tx = currentSession().beginTransaction();
	// threadTransaction.set(tx);
	// }
	// }
	//
	// public void commitTransaction() {
	// Transaction tx = threadTransaction.get();
	// try {
	// if (tx != null && !tx.wasCommitted() && !tx.wasRolledBack()) {
	// tx.commit();
	// }
	// threadTransaction.set(null);
	// } catch (HibernateException ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/hibernate/OjsHibernateUtil.java:106");
	// rollbackTransaction();
	// throw ex;
	// }
	// }

	public void rollbackTransaction() {

		try {
			// Transaction tx = threadTransaction.get();
			// threadTransaction.set(null);
			Session s = (Session) MAP.get();
			if (s != null) {
				Transaction tx = s.getTransaction();
				System.out.println(
						"====================== ROLLING BACK SESSION ============================== wasCommitted = "
								+ tx.wasCommitted() + ", wasRolledBack = " + tx.wasRolledBack());
				try {
					if (tx != null && !tx.wasCommitted() && !tx.wasRolledBack()) {
						try {
							tx.rollback();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/OjsHibernateUtil.java:127");

						}
					}
				} finally {
					closeSession();
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}

		// try {
		// // Transaction tx = threadTransaction.get();
		// // threadTransaction.set(null);
		// Session s = threadSession.get();
		// if (s != null) {
		// Transaction tx = s.getTransaction();
		// System.out
		// .println("====================== ROLLING BACK
		// ============================== wasCommitted = "
		// + tx.wasCommitted()
		// + ", wasRolledBack = "
		// + tx.wasRolledBack());
		// try {
		// if (tx != null && !tx.wasCommitted() && !tx.wasRolledBack()) {
		// tx.rollback();
		// }
		// } finally {
		// closeSession();
		// }
		// }
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/hibernate/OjsHibernateUtil.java:159");
		// Common.tampilErrorJikaAdmin(e); 
		// }

	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Menutup SessionFactory OJS saat webapp berhenti/di-reload (OPTIMASI FASE 4).
	 *
	 * <p>Sebelumnya factory ini TIDAK PERNAH ditutup: pool koneksinya (thread
	 * {@code mchange ... PoolThread} pada c3p0) tetap hidup setelah webapp stop sehingga
	 * menahan classloader Tomcat -- muncul warning "appears to have started a thread ...
	 * but has failed to stop it" dan koneksi ke database tidak dilepas pada tiap redeploy.</p>
	 *
	 * <p>Idempoten dan menelan seluruh kegagalan supaya proses shutdown tidak pernah gagal
	 * karena factory ini (mis. belum pernah dibangun, atau sudah tertutup).</p>
	 */
	public void closeFactoryQuietly() {
		try {
			SessionFactory sf = sessionFactory;
			if (sf != null && !sf.isClosed()) {
				sf.close();
			}
		} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "OjsHibernateUtil.closeFactoryQuietly");
			// Sudah tertutup / versi berbeda: abaikan agar shutdown tetap berjalan.
		}
	}
}
