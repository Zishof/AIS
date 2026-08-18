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

@SuppressWarnings("deprecation")
public class RadiusHibernateUtil {

	private static RadiusHibernateUtil hibernateStreamingUtil = new RadiusHibernateUtil(
			"/hibernate.radius.cfg.xml");

	public  static RadiusHibernateUtil getInstance() {
		// String wilayah = Common.getCurrentWilayah();
		// if (wilayah.equals("bekasi")) {
		// return hibernateUtilbekasi;
		// } else if (wilayah.equals("jabar")) {
		// return hibernateStreamingUtil;
		// }
		return hibernateStreamingUtil;
	}

	private RadiusHibernateUtil(String config) {
		try {
			// Create the SessionFactory
			Configuration configuration = new AnnotationConfiguration();
			URL url = RadiusHibernateUtil.class.getResource(config);
			configuration.configure(url);
			// P0 keamanan: kredensial dari berkas eksternal (bila ada) menimpa nilai cfg.xml.
			DbCredentialOverride.terapkan(configuration, "radius");
			sessionFactory = configuration.buildSessionFactory();
		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	private SessionFactory sessionFactory;

	public final ThreadLocal<Session> threadSession = new ThreadLocal<Session>();

	// public final ThreadLocal<Transaction> threadTransaction = new
	// ThreadLocal<Transaction>();

	@SuppressWarnings("rawtypes")
	public  ClassMetadata getClassMetadata(Class aClass) {
		return getSessionFactory().getClassMetadata(aClass);
	}

	public  Session currentSession() throws HibernateException {
		Session s = threadSession.get();
		// Open a new Session, if this Thread has none yet
		if (s == null) {
			s = getSessionFactory().openSession();
			threadSession.set(s);
		}
		return s;
	}

	public  void closeSession() throws HibernateException {
		Session s = threadSession.get();
		threadSession.set(null);
        try { threadSession.remove(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/hibernate/RadiusHibernateUtil.java:76"); }
		HibernateUtil.closeSessionQuietly(s);
	}

	// public  void beginTransaction() {
	// Transaction tx = threadTransaction.get();
	// if (tx == null) {
	// tx = currentSession().beginTransaction();
	// threadTransaction.set(tx);
	// }
	// }
	//
	// public  void commitTransaction() {
	// Transaction tx = threadTransaction.get();
	// try {
	// if (tx != null && !tx.wasCommitted() && !tx.wasRolledBack()) {
	// tx.commit();
	// }
	// threadTransaction.set(null);
	// } catch (HibernateException ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/hibernate/RadiusHibernateUtil.java:95");
	// rollbackTransaction();
	// throw ex;
	// }
	// }

	public  void rollbackTransaction() {
		try {
			// Transaction tx = threadTransaction.get();
			// threadTransaction.set(null);
			Session s = threadSession.get();
			if (s != null) {
				Transaction tx = s.getTransaction();
				System.out
						.println("====================== ROLLING BACK ============================== wasCommitted = "
								+ tx.wasCommitted()
								+ ", wasRolledBack = "
								+ tx.wasRolledBack());
				try {
					if (tx != null && !tx.wasCommitted() && !tx.wasRolledBack()) {
						tx.rollback();
					}
				} finally {
					closeSession();
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}

	}

	public  SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public  void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}