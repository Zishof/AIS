package ais.common;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.action.master.koperasi.helper.DepositoAroHelper;
import ais.database.hibernate.HibernateUtil;

/**
 * <h2>DepositoAroScheduler — Penjadwal Otomasi ARO Simpanan Berjangka</h2>
 *
 * <p>
 * Kelas ini menjalankan otomasi <b>perpanjangan otomatis (ARO)</b> simpanan berjangka koperasi secara
 * berkala di latar belakang (daemon), sehingga deposito yang jatuh tempo diperpanjang atau ditandai
 * untuk dicairkan tanpa perlu tindakan manual harian. Pekerjaan sesungguhnya dikerjakan
 * {@link DepositoAroHelper}; kelas ini hanya mengatur <b>penjadwalan</b> dan <b>siklus sesi/transaksi</b>.
 * </p>
 *
 * <h3>Cara kerja</h3>
 * <ul>
 * <li>{@link #mulai()} dipanggil sekali saat aplikasi start (dari {@code AppStartupListener}). Ia
 * membuat penjadwal daemon yang menjalankan {@link #jalankanSekali()} pertama kali beberapa menit
 * setelah start (memberi waktu {@code SessionFactory} siap), lalu berulang tiap 24 jam.</li>
 * <li>{@link #hentikan()} dipanggil saat aplikasi berhenti/di-reload agar thread benar-benar mati
 * (mencegah kebocoran classloader pada kontainer).</li>
 * <li>{@link #jalankanSekali()} membuka {@code openSession()} + {@code Transaction} dan menutupnya di
 * blok {@code finally} sesuai kaidah, lalu mengembalikan ringkasan hasil. Metode ini juga dipakai oleh
 * tombol pemicu manual pada layar pemantauan.</li>
 * </ul>
 *
 * <p>
 * Kompatibel Java 1.7; seluruh kesalahan ditangkap agar kegagalan otomasi tidak pernah mengganggu
 * jalannya aplikasi.
 * </p>
 */
public final class DepositoAroScheduler {

	private static volatile ScheduledExecutorService penjadwal;

	private DepositoAroScheduler() {
		// util statis
	}

	/** Mulai penjadwal ARO harian (idempoten; aman dipanggil sekali saat start). */
	public static synchronized void mulai() {
		try {
			if (penjadwal != null) {
				return;
			}
			ScheduledExecutorService s = Executors.newScheduledThreadPool(1, daemonFactory("deposito-aro"));
			s.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					try {
						jalankanSekali();
					} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/DepositoAroScheduler.java:63");
						// jangan biarkan kegagalan menghentikan penjadwal
					}
				}
			}, 3, 24 * 60, TimeUnit.MINUTES);
			penjadwal = s;
			System.out.println("[DepositoAro] Penjadwal ARO deposito aktif (harian).");
		} catch (Throwable t) {
			System.err.println("[DepositoAro] Gagal memulai penjadwal: " + t.getMessage());
		}
	}

	/** Hentikan penjadwal ARO. Dipanggil saat aplikasi berhenti. */
	public static synchronized void hentikan() {
		ScheduledExecutorService s = penjadwal;
		penjadwal = null;
		if (s != null) {
			try {
				s.shutdownNow();
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/DepositoAroScheduler.java:82");
			}
		}
	}

	/**
	 * Jalankan satu siklus ARO sekarang (membuka &amp; menutup sesi/transaksi sendiri).
	 *
	 * @return ringkasan {@code [didaftarkan, diperpanjang, ditandaiJatuhTempo]}
	 */
	public static int[] jalankanSekali() {
		Session session = null;
		Transaction tx = null;
		int[] hasil = new int[] { 0, 0, 0 };
		try {
			session = HibernateUtil.openSession();
			tx = session.beginTransaction();
			hasil = DepositoAroHelper.proses(session, new Date());
			tx.commit();
			tx = null;
			System.out.println("[DepositoAro] daftar=" + hasil[0] + " perpanjang=" + hasil[1] + " jatuhTempo="
					+ hasil[2]);
		} catch (Throwable t) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DepositoAroScheduler.java:108");
				}
			}
			System.err.println("[DepositoAro] Gagal menjalankan siklus ARO: " + t.getMessage());
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/DepositoAroScheduler.java:116");
				}
			}
		}
		return hasil;
	}

	/** Pabrik thread daemon bernama agar mudah dikenali dan tidak menghalangi shutdown. */
	private static ThreadFactory daemonFactory(final String nama) {
		return new ThreadFactory() {
			private final AtomicInteger n = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, nama + "-" + n.getAndIncrement());
				t.setDaemon(true);
				return t;
			}
		};
	}
}
