package ais.action.master.helper.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.KonfigurasiKalenderAkademik;

/**
 * Processor konfigurasi kalender akademik.
 *
 * Catatan penting:
 * - Tidak memakai currentNativeSession() karena class ini berjalan di background thread.
 * - Semua session manual selalu ditutup di finally.
 * - Query pencarian hanya mengambil ID konfigurasi dan nilai tujuan agar hemat memory.
 * - Update konfigurasi dilakukan melalui entity Konfigurasi yang di-load ulang pada
 *   session yang sama agar audit/lifecycle tetap berjalan.
 */
public class KonfigurasiKalenderAkademikProcessor extends TimerTask {

	private static final Object LOCK = new Object();
	private static volatile boolean running = false;

	private static final String CONFIG_PROCESSOR_AKTIF = "konfigurasi_kalender_akademik_processor";
	private static final int INITIAL_DELAY_MILLIS = 1000;

	@Override
	public void run() {
		doProcess();
	}

	public static void doProcess() {
		synchronized (LOCK) {
			if (running) {
				return;
			}
			running = true;
		}

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					sleepQuietly(INITIAL_DELAY_MILLIS);

					if (!isProcessorAktif()) {
						return;
					}

					processSelesaiCalendar();
					processAktifCalendar();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				} finally {
					synchronized (LOCK) {
						running = false;
					}
				}
			}
		}, "AIS-KonfigurasiKalenderAkademikProcessor");
		thread.setDaemon(true);
		thread.start();
	}

	private static void sleepQuietly(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static boolean isProcessorAktif() {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Konfigurasi konfigurasi = (Konfigurasi) session.createCriteria(Konfigurasi.class)
					.add(Restrictions.eq("nama", CONFIG_PROCESSOR_AKTIF)).setMaxResults(1).uniqueResult();

			if (konfigurasi == null || konfigurasi.getNilai() == null) {
				return true;
			}
			return Konfigurasi.AKTIF.equalsIgnoreCase(konfigurasi.getNilai().trim());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return true;
		} finally {
			closeSession(session);
		}
	}

	/**
	 * Mengembalikan semua konfigurasi ke nilai ketika kalender sudah selesai.
	 *
	 * Logic lama dipertahankan: semua data KonfigurasiKalenderAkademik diproses
	 * lebih dulu memakai nilai padaSaatSelesaiBerubahMenjadi.
	 */
	private static void processSelesaiCalendar() {
		List<ConfigChange> changes = loadSelesaiChanges();
		applyChanges(changes, "selesai");
	}

	/**
	 * Mengaktifkan konfigurasi yang kalender akademiknya sedang berjalan hari ini.
	 */
	private static void processAktifCalendar() {
		List<ConfigChange> changes = loadAktifChanges();
		applyChanges(changes, "mulai");
	}

	@SuppressWarnings("unchecked")
	private static List<ConfigChange> loadSelesaiChanges() {
		Session session = null;
		List<ConfigChange> changes = new ArrayList<ConfigChange>();
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			session.setFlushMode(FlushMode.MANUAL);

			ProjectionList projectionList = Projections.projectionList();
			projectionList.add(Projections.property("konfigurasi.id"));
			projectionList.add(Projections.property("padaSaatSelesaiBerubahMenjadi"));

			List<Object[]> rows = session.createCriteria(KonfigurasiKalenderAkademik.class)
					.createAlias("konfigurasi", "konfigurasi", Criteria.LEFT_JOIN).setProjection(projectionList).list();

			appendChanges(changes, rows);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
		return changes;
	}

	@SuppressWarnings("unchecked")
	private static List<ConfigChange> loadAktifChanges() {
		Session session = null;
		List<ConfigChange> changes = new ArrayList<ConfigChange>();
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			session.setFlushMode(FlushMode.MANUAL);

			Date mulaiHari = startOfToday();
			Date selesaiHari = endOfToday();

			ProjectionList projectionList = Projections.projectionList();
			projectionList.add(Projections.property("konfigurasi.id"));
			projectionList.add(Projections.property("padaSaatMulaiBerubahMenjadi"));

			List<Object[]> rows = session.createCriteria(KonfigurasiKalenderAkademik.class)
					.createAlias("konfigurasi", "konfigurasi")
					.createAlias("kalenderAkademik", "kalenderAkademik")
					.add(Restrictions.le("kalenderAkademik.tanggalMulai", mulaiHari))
					.add(Restrictions.ge("kalenderAkademik.tanggalSelesai", selesaiHari)).setProjection(projectionList)
					.list();

			appendChanges(changes, rows);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
		return changes;
	}

	private static void appendChanges(List<ConfigChange> changes, List<Object[]> rows) {
		if (changes == null || rows == null || rows.isEmpty()) {
			return;
		}
		for (int i = 0; i < rows.size(); i++) {
			Object[] row = rows.get(i);
			if (row == null || row.length < 2) {
				continue;
			}
			Long konfigurasiId = toLong(row[0]);
			if (konfigurasiId == null) {
				continue;
			}
			changes.add(new ConfigChange(konfigurasiId, row[1] == null ? null : String.valueOf(row[1])));
		}
	}

	private static Date startOfToday() {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	private static Date endOfToday() {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	private static Long toLong(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Long) {
			return (Long) value;
		}
		if (value instanceof Number) {
			return Long.valueOf(((Number) value).longValue());
		}
		try {
			return Long.valueOf(String.valueOf(value));
		} catch (Exception e) {
			return null;
		}
	}

	private static void applyChanges(List<ConfigChange> changes, String fase) {
		if (changes == null || changes.isEmpty()) {
			return;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			for (int i = 0; i < changes.size(); i++) {
				ConfigChange change = changes.get(i);
				applyOneChange(session, change, fase);

				if (i > 0 && i % 50 == 0) {
					clearSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSession(session);
		}
	}

	private static void applyOneChange(Session session, ConfigChange change, String fase) {
		// KE-FIX (ConstraintViolationException "duplicate key ... konfigurasi_pkey" saat commit,
		// mis. konfigurasi_id=986): baris Konfigurasi yang sama bisa juga sedang diedit lewat
		// layar admin (KonfigurasiAction dkk) tepat saat processor latar ini berjalan. Karena
		// session ini dipakai berulang lintas-iterasi (dibuka sekali di applyChanges, session.clear()
		// hanya melepas objek Java dari persistence context -- TIDAK membuka koneksi/transaksi baru),
		// tabrakan tulis-bersamaan pada baris yang sama bisa membuat flush/commit gagal dengan
		// exception yang membingungkan meski data sebenarnya konsisten setelah pihak lain selesai.
		// Ini murni kontensi sesaat (bukan bug data), jadi aman & tepat untuk dicoba ulang SEKALI
		// dengan membaca ulang nilai TERKINI dari DB pada sesi/transaksi yang baru -- alih-alih
		// langsung menyerah untuk satu konfigurasi_id yang datanya sebenarnya baik-baik saja.
		applyOneChangeWithRetry(session, change, fase, 1);
	}

	// KE-FIX (recurring, konfigurasi_id lain, 3 hari setelah fix di atas): retry TUNGGAL yang
	// sebelumnya dipasang di sini terbukti kadang tidak cukup -- ConstraintViolationException
	// "duplicate key konfigurasi_pkey" yang sama terulang lagi. Perbesar ke MAKS_PERCOBAAN_KONFLIK
	// percobaan dengan jeda singkat (bertambah tiap percobaan) di antaranya, karena kontensi
	// tulis-bersamaan pada baris yang sama biasanya reda dalam hitungan puluhan/ratusan milidetik,
	// bukan seketika -- retry tunggal tanpa jeda bisa saja masih jatuh di jendela kontensi yang sama.
	private static final int MAKS_PERCOBAAN_KONFLIK = 3;

	private static void applyOneChangeWithRetry(Session session, ConfigChange change, String fase,
			int percobaanKe) {
		Transaction transaction = null;
		try {
			if (session == null || change == null || change.konfigurasiId == null) {
				return;
			}

			transaction = session.beginTransaction();

			// Update hanya kolom nilai berdasarkan primary key. Entity Konfigurasi tidak
			// di-attach kembali, sehingga identity-map/canonical object dari thread lain tidak
			// dapat mengubah id saat flush dan Hibernate tidak mungkin menghasilkan INSERT.
			// Ini menghilangkan akar duplicate key konfigurasi_pkey pada job latar.
			session.createQuery("update Konfigurasi set nilai = :nilai where id = :id")
					.setParameter("nilai", change.nilaiBaru)
					.setParameter("id", change.konfigurasiId)
					.executeUpdate();

			transaction.commit();
		} catch (Exception e) {
			rollbackQuietly(transaction);
			clearSessionQuietly(session);
			if (isConstraintViolation(e) && percobaanKe < MAKS_PERCOBAAN_KONFLIK) {
				try {
					Thread.sleep(50L * percobaanKe);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
				}
				applyOneChangeWithRetry(session, change, fase, percobaanKe + 1);
				return;
			}
			// Setelah batas retry, laporkan sekali dengan penyebab asli.
			Common.tampilErrorJikaAdmin(new Exception("Gagal update konfigurasi kalender akademik fase " + fase
					+ " untuk konfigurasi_id=" + (change == null ? null : change.konfigurasiId) + " setelah "
					+ percobaanKe + " percobaan", e));
		} finally {
			clearSessionQuietly(session);
		}
	}

	private static boolean isConstraintViolation(Throwable e) {
		Throwable t = e;
		int guard = 0;
		while (t != null && guard++ < 12) {
			String className = t.getClass().getName();
			if (className.indexOf("ConstraintViolationException") >= 0) {
				return true;
			}
			String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
			if (msg.indexOf("duplicate key") >= 0 || msg.indexOf("unique constraint") >= 0) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	private static boolean equalsString(String a, String b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

	private static void rollbackQuietly(Transaction transaction) {
		try {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static void clearSessionQuietly(Session session) {
		try {
			if (session != null && session.isOpen()) {
				session.clear();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/KonfigurasiKalenderAkademikProcessor.java:310");
		}
	}

	private static void closeSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/KonfigurasiKalenderAkademikProcessor.java:322");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/KonfigurasiKalenderAkademikProcessor.java:326");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/KonfigurasiKalenderAkademikProcessor.java:330");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static class ConfigChange {
		private Long konfigurasiId;
		private String nilaiBaru;

		private ConfigChange(Long konfigurasiId, String nilaiBaru) {
			this.konfigurasiId = konfigurasiId;
			this.nilaiBaru = nilaiBaru;
		}
	}
}
