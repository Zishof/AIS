package ais.service.registration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.tenant.SchemaNameReservation;

/**
 * <h3>Penyapu latar reservasi username/schema tenant yang kedaluwarsa -- pola scheduler
 * {@code DepositoAroScheduler}/{@code TenantProvisioningWorker}.</h3>
 *
 * <p>
 * Menutup celah yang didokumentasikan pada Javadoc {@link SchemaNameReservation} dan
 * {@link UsernameReservationService}: {@code expiresAt} dihitung saat reservasi dibuat tetapi,
 * sebelum perbaikan ini, tidak pernah dibaca -- permohonan yang ditinggalkan sebelum verifikasi
 * email menahan usernamenya selamanya. Penyapu ini HANYA menyentuh baris yang memenuhi
 * {@link UsernameReservationService#cariKandidatKedaluwarsa}: RESERVED, {@code expiresAt} sudah
 * lewat, DAN permohonan pemiliknya masih persis {@code STATUS_EMAIL_VERIFICATION_PENDING}/
 * {@code STATUS_SUBMITTED}. Permohonan yang sudah melangkah lebih jauh (VERIFIED/REVIEW_PENDING/
 * PROVISIONING_QUEUED/PROVISIONING/READY/...) TIDAK PERNAH disentuh di sini tanpa memandang
 * seberapa lama ia berjalan -- job yang sedang diprovisikan karenanya aman dari penyapu ini.
 * </p>
 *
 * <p>
 * Lifecycle: {@code mulai()} dipanggil {@code PendaftaranTenantServlet.init()} (load-on-startup),
 * {@code hentikan()} dari {@code destroy()} -- sama seperti {@code TenantProvisioningWorker}. Satu
 * baris kandidat = satu transaksi pendek ({@link #sapuSatu}) yang memverifikasi ulang status &amp;
 * expiresAt pada saat klaim (baris bisa saja berubah antara query kandidat dan klaim, mis.
 * pendaftar mengklik tautan verifikasi tepat di antara keduanya) sebelum memanggil
 * {@link UsernameReservationService#kedaluwarsakan} -- satu baris kalah race/berubah tidak
 * menggagalkan baris lain.
 * </p>
 */
public final class ReservationExpiryScheduler {

	private static volatile ScheduledExecutorService penjadwal;

	/** Maksimal baris disapu per tick (jaga tick tetap pendek). */
	private static final int MAKSIMAL_PER_TICK = 200;

	private ReservationExpiryScheduler() {
	}

	/**
	 * Mulai penjadwal (idempoten; aman dipanggil berulang). Delay awal 5 menit (beri waktu
	 * {@code SessionFactory} siap), lalu berulang tiap 6 jam -- reservasi berumur jam-jaman
	 * (default {@code pendaftaran_reservasi_jam}=72), jadi granularitas ini cukup rapat tanpa
	 * membebani DB dengan polling terlalu sering.
	 */
	public static synchronized void mulai() {
		if (penjadwal != null) {
			return;
		}
		ScheduledExecutorService s = Executors.newScheduledThreadPool(1, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "reservation-expiry-sweep");
				t.setDaemon(true);
				return t;
			}
		});
		s.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					jalankanSekali();
				} catch (Throwable t) {
					ais.common.ErrorAuditUtil.record(t instanceof Exception ? (Exception) t : new RuntimeException(t),
							"auto-audit ReservationExpiryScheduler.tick");
				}
			}
		}, 5, 6 * 60, TimeUnit.MINUTES);
		penjadwal = s;
	}

	/** Hentikan penjadwal (dipanggil dari {@code destroy()} servlet). Aman walau belum pernah dinyalakan. */
	public static synchronized void hentikan() {
		ScheduledExecutorService s = penjadwal;
		penjadwal = null;
		if (s != null) {
			s.shutdownNow();
		}
	}

	/** Jalankan satu siklus sapu sekarang (dipakai juga oleh tombol pemicu manual bila ada). @return jumlah baris yang disapu. */
	public static int jalankanSekali() {
		List<Long> idKandidat = kandidatIds();
		int disapu = 0;
		for (int i = 0; i < idKandidat.size(); i++) {
			if (sapuSatu(idKandidat.get(i).longValue())) {
				disapu++;
			}
		}
		return disapu;
	}

	/** Ambil id kandidat dalam sesi read-only pendek (daftar id saja, diproses satu-per-satu di {@link #sapuSatu}). */
	private static List<Long> kandidatIds() {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			List<SchemaNameReservation> baris =
					UsernameReservationService.cariKandidatKedaluwarsa(session, new Date(), MAKSIMAL_PER_TICK);
			List<Long> ids = new ArrayList<Long>(baris.size());
			for (int i = 0; i < baris.size(); i++) {
				ids.add(baris.get(i).getId());
			}
			return ids;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Sapu satu baris DI DALAM transaksi pendek sendiri; memverifikasi ulang status &amp;
	 * expiresAt persis sebelum menandainya (baris kandidat bisa saja sudah berubah sejak
	 * {@link #kandidatIds()} dipanggil).
	 *
	 * @return true bila baris ini berhasil disapu (dan di-commit)
	 */
	private static boolean sapuSatu(long reservationId) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			SchemaNameReservation r = (SchemaNameReservation) session.get(SchemaNameReservation.class,
					Long.valueOf(reservationId));
			Date sekarang = new Date();
			if (r == null || !SchemaNameReservation.STATUS_RESERVED.equals(r.getStatus())
					|| r.getExpiresAt() == null || !r.getExpiresAt().before(sekarang)) {
				tx.rollback();
				return false;
			}
			UsernameReservationService.kedaluwarsakan(session, r, sekarang);
			tx.commit();
			return true;
		} catch (Throwable t) {
			if (tx != null) {
				try {
					if (tx.isActive()) {
						tx.rollback();
					}
				} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored,
						"auto-audit(empty-catch) ReservationExpiryScheduler.sapuSatu.rollback");
				}
			}
			ais.common.ErrorAuditUtil.record(t instanceof Exception ? (Exception) t : new RuntimeException(t),
					"auto-audit ReservationExpiryScheduler.sapuSatu:" + reservationId);
			return false;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
