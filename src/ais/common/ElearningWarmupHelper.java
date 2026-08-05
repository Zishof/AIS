package ais.common;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pertemuan;

/**
 * FASE 4 &mdash; <b>pemanasan (warmup) cache e-Learning</b>.
 *
 * <p>
 * Penyebab utama loading e-Learning "lambat pada akses pertama" adalah <b>cache DINGIN</b>: koleksi anak
 * per-pertemuan (materi/audio/video/tugas/tugas-kelompok/ujian) di-<i>reInit</i> lewat query
 * {@code WHERE pertemuan = id} <b>satu per pertemuan</b> (N+1) saat pertama diakses setelah restart Tomcat.
 * Flag hasil reInit ({@code GeneralValueObject.udah(key)}) beserta lokasi-JSON-nya bersifat <b>GLOBAL</b>
 * (berbasis file), sehingga sekali dipanaskan, <b>semua pengguna &amp; semua sesi</b> ikut menikmati.
 *
 * <p>
 * Helper ini menjalankan {@code ambil*Total()} yang <b>sama persis</b> dengan jalur normal &mdash; tanpa
 * logika/keying baru &mdash; untuk seluruh pertemuan di <b>jendela saat ini (&plusmn;{@value #HARI_JENDELA}
 * hari)</b>, di <b>thread latar prioritas rendah</b> saat startup. Efek: saat pengguna membuka e-Learning,
 * {@code udah(key)} sudah {@code true} &rarr; query {@code reInit*} yang mahal <b>dilewati</b> &rarr; fan-out
 * panel kanan tinggal baca memori. Karena hanya memakai ulang kode yang sudah teruji, <b>tidak ada risiko
 * regresi</b> pada keying/urutan/freshness. Non-kritis: kegagalan apa pun di-swallow diam-diam.
 */
public final class ElearningWarmupHelper {

	private static final AtomicBoolean SUDAH = new AtomicBoolean(false);

	/** Lebar jendela pemanasan (hari) di kiri &amp; kanan hari ini; samakan/lebihkan dari jendela timeline. */
	private static final int HARI_JENDELA = 14;

	/** Pool kecil agar tidak membebani DB saat startup (thread latar prioritas rendah). */
	private static final int MAKS_THREAD = 4;

	private ElearningWarmupHelper() {
	}

	/**
	 * Memanaskan cache jendela sekarang di thread latar. Aman dipanggil berkali-kali (hanya jalan sekali per
	 * JVM) dan tidak memblokir pemanggil.
	 */
	public static void jalankan() {
		if (!SUDAH.compareAndSet(false, true)) {
			return; // cukup sekali per JVM
		}
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					warmupInternal();
				} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/ElearningWarmupHelper.java:60");
					// warmup non-kritis: jangan pernah mengganggu proses lain.
				}
			}
		}, "elearning-warmup");
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	@SuppressWarnings("unchecked")
	private static void warmupInternal() {
		List<?> ids = null;
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			// Sargable lewat idx_dash_el_pertemuan_date_tanggal (index atas DATE(tanggal)).
			ids = session.createSQLQuery(
					"SELECT id FROM pertemuan WHERE date(tanggal) BETWEEN (CURRENT_DATE - INTERVAL '" + HARI_JENDELA
							+ " day') AND (CURRENT_DATE + INTERVAL '" + HARI_JENDELA + " day')")
					.list();
		} catch (Exception e) {
			return; // gagal mengambil daftar pertemuan → batal (non-kritis)
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		if (ids == null || ids.isEmpty()) {
			return;
		}

		ExecutorService ex = Executors.newFixedThreadPool(MAKS_THREAD);
		try {
			final CountDownLatch latch = new CountDownLatch(ids.size());
			for (Object idObj : ids) {
				if (!(idObj instanceof Number)) {
					latch.countDown();
					continue;
				}
				final Long id = ((Number) idObj).longValue();
				ex.execute(new Runnable() {
					@Override
					public void run() {
						try {
							Pertemuan p = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, id.toString(),
									false);
							if (p == null) {
								return;
							}
							// Panaskan 6 koleksi anak → set flag udah() + lokasi-JSON GLOBAL. Masing-masing
							// dibungkus try sendiri agar satu kegagalan tak menggagalkan yang lain.
							try {
								p.ambilPertemuanFileContentTotal();
							} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningWarmupHelper.java:112");
							}
							try {
								p.ambilAudioPertemuanTotal();
							} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningWarmupHelper.java:116");
							}
							try {
								p.ambilVideoPertemuanTotal();
							} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningWarmupHelper.java:120");
							}
							try {
								p.ambilTugasPertemuanTotal();
							} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningWarmupHelper.java:124");
							}
							try {
								p.ambilTugasKelompokTotal();
							} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningWarmupHelper.java:128");
							}
							try {
								p.ambilPertemuanPunyaUjianTotal(null);
							} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningWarmupHelper.java:132");
							}
						} catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/ElearningWarmupHelper.java:134");
							// abaikan: warmup non-kritis
						} finally {
							latch.countDown();
						}
					}
				});
			}
			latch.await(10, TimeUnit.MINUTES);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ElearningWarmupHelper.java:143");
			// abaikan
		} finally {
			ex.shutdown();
		}
	}
}
