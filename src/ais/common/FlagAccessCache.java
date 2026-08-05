package ais.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h2>FlagAccessCache — warm-up cache flag yang sering diakses</h2>
 *
 * <p>Melacak kunci {@code ais_flags_data_baru} yang DIAKSES (via {@link BacaTulisUtil#baca}) dan
 * menyimpan daftar kunci + waktu-akses terakhir ke BERKAS (bukan meng-ALTER tabel). Saat aplikasi
 * start, kunci yang diakses dalam &lt; 1 minggu di-PRELOAD (paralel) ke cache, sehingga flag yang
 * memang sering dipakai langsung hangat tanpa hit DB satu-per-satu.</p>
 *
 * <p><b>Berkas:</b> {@code {catalina.base}/cache/{Common.ROOT}/flag_access_keys.dat} — satu baris
 * per kunci: {@code <epochMillis>\t<globalKey>}. Ditulis tiap 15 menit di latar (tulis ke berkas
 * sementara lalu rename = atomik), dan hanya kunci &le; 1 minggu yang disimpan (auto-prune).</p>
 *
 * <p>Best-practice: tracking ringan (ConcurrentHashMap), penyimpanan periodik di latar (daemon),
 * tulis atomik (temp+rename), preload paralel agar tak mengganggu waktu start. Java 1.6/1.7.</p>
 */
public final class FlagAccessCache {

	private FlagAccessCache() {
	}

	private static final long SEMINGGU_MS = 7L * 24 * 60 * 60 * 1000;
	private static final String NAMA_BERKAS = "flag_access_keys.dat";

	/** globalKey -&gt; waktu akses terakhir (epoch ms). */
	private static final ConcurrentHashMap<String, Long> akses = new ConcurrentHashMap<String, Long>();

	/** Scheduler penyimpanan berkala (disimpan agar bisa di-shutdown saat webapp berhenti). */
	private static volatile ScheduledExecutorService penyimpan;

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

	/** Catat satu akses kunci (dipanggil dari BacaTulisUtil.baca). Sangat ringan. */
	public static void catat(String globalKey) {
		if (globalKey != null && globalKey.length() > 0) {
			akses.put(globalKey, Long.valueOf(System.currentTimeMillis()));
		}
	}

	/** Lokasi berkas cache: {catalina.base}/cache/{Common.ROOT}/flag_access_keys.dat */
	private static File berkasCache() {
		String base = System.getProperty("catalina.base");
		if (base == null || base.trim().isEmpty()) {
			base = System.getProperty("catalina.home");
		}
		if (base == null || base.trim().isEmpty()) {
			base = System.getProperty("java.io.tmpdir");
		}
		String root = (Common.ROOT == null ? "default" : Common.ROOT).replaceAll("[\\\\/:*?\"<>|]", "_");
		if (root.trim().isEmpty()) {
			root = "default";
		}
		File dir = new File(new File(base, "cache"), root);
		try {
			dir.mkdirs();
		} catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:87");
		}
		return new File(dir, NAMA_BERKAS);
	}

	/** Simpan kunci yang diakses &le; 1 minggu ke berkas (tulis temp lalu rename). Auto-prune memori. */
	public static synchronized void simpanKeFile() {
		try {
			long batas = System.currentTimeMillis() - SEMINGGU_MS;
			File f = berkasCache();
			File tmp = new File(f.getAbsolutePath() + ".tmp");
			BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8"));
			try {
				for (Map.Entry<String, Long> e : akses.entrySet()) {
					Long ts = e.getValue();
					if (ts != null && ts.longValue() >= batas && e.getKey() != null) {
						w.write(ts.longValue() + "\t" + e.getKey());
						w.newLine();
					}
				}
			} finally {
				w.close();
			}
			// rename atomik (best-effort).
			if (f.exists()) {
				f.delete();
			}
			if (!tmp.renameTo(f)) {
				// fallback bila rename gagal antar-filesystem.
				tmp.delete();
			}
			// Prune memori: buang yang lebih lama dari 1 minggu agar tak menumpuk.
			for (Map.Entry<String, Long> e : akses.entrySet()) {
				Long ts = e.getValue();
				if (ts == null || ts.longValue() < batas) {
					akses.remove(e.getKey(), ts);
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:125");
			// gagal-diam: cache berkas hanya pendukung.
		}
	}

	/** Baca berkas, kembalikan kunci yang diakses &le; 1 minggu (sekaligus restore ke memori). */
	public static List<String> muatKunciSeminggu() {
		List<String> out = new ArrayList<String>();
		try {
			File f = berkasCache();
			if (!f.exists() || f.length() <= 0) {
				return out;
			}
			long batas = System.currentTimeMillis() - SEMINGGU_MS;
			BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			try {
				String line;
				while ((line = r.readLine()) != null) {
					int tab = line.indexOf('\t');
					if (tab <= 0) {
						continue;
					}
					try {
						long ts = Long.parseLong(line.substring(0, tab).trim());
						String key = line.substring(tab + 1);
						if (ts >= batas && key.length() > 0) {
							out.add(key);
							akses.put(key, Long.valueOf(ts));
						}
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:154");
					}
				}
			} finally {
				r.close();
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:160");
		}
		return out;
	}

	/** Jadwalkan penyimpanan berkala tiap 15 menit di latar (daemon). */
	public static synchronized void mulaiPenyimpananBerkala() {
		try {
			if (penyimpan != null) {
				return;
			}
			ScheduledExecutorService sch = Executors.newScheduledThreadPool(1, daemonFactory("flag-access-saver"));
			sch.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					try {
						simpanKeFile();
					} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:177");
					}
				}
			}, 15, 15, TimeUnit.MINUTES);
			penyimpan = sch;
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:182");
		}
	}

	/**
	 * Hentikan scheduler penyimpanan berkala. Dipanggil dari
	 * {@code AppStartupListener.contextDestroyed} saat webapp berhenti/di-reload agar thread
	 * {@code flag-access-saver-*} benar-benar berhenti (cegah warning "failed to stop it" /
	 * kebocoran classloader Tomcat). Sebelum berhenti, sekali lagi simpan ke berkas agar tidak
	 * ada akses yang hilang.
	 */
	public static synchronized void hentikanPenyimpananBerkala() {
		ScheduledExecutorService sch = penyimpan;
		penyimpan = null;
		if (sch != null) {
			try {
				simpanKeFile();
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:199");
			}
			try {
				sch.shutdownNow();
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:203");
			}
		}
	}

	/**
	 * Warm-up saat start: baca berkas -&gt; preload kunci &lt; 1 minggu ke cache flag SECARA PARALEL
	 * (pool 4 thread, batch 500), di latar (daemon) agar tak menambah waktu start.
	 */
	public static void warmupStartup() {
		Thread starter = new Thread(new Runnable() {
			@Override
			public void run() {
				ExecutorService pool = null;
				try {
					// beri jeda agar SessionFactory siap lebih dulu.
					Thread.sleep(45000);
					List<String> keys = muatKunciSeminggu();
					if (keys == null || keys.isEmpty()) {
						return;
					}
					pool = Executors.newFixedThreadPool(4, daemonFactory("flag-warmup"));
					List<Future<?>> futures = new ArrayList<Future<?>>();
					List<String> batch = new ArrayList<String>();
					for (String k : keys) {
						batch.add(k);
						if (batch.size() >= 500) {
							futures.add(pool.submit(tugasMuat(batch)));
							batch = new ArrayList<String>();
						}
					}
					if (!batch.isEmpty()) {
						futures.add(pool.submit(tugasMuat(batch)));
					}
					for (Future<?> fu : futures) {
						try {
							fu.get();
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:240");
						}
					}
					System.out.println("FlagAccessCache: warm-up " + keys.size() + " kunci flag selesai.");
				} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:244");
				} finally {
					if (pool != null) {
						try {
							pool.shutdown();
						} catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:249");
						}
					}
				}
			}
		}, "flag-warmup-startup");
		starter.setDaemon(true);
		starter.start();
	}

	private static Runnable tugasMuat(final List<String> batch) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					BacaTulisUtil.muatKeCacheBanyak(batch);
				} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/FlagAccessCache.java:265");
				}
			}
		};
	}
}
