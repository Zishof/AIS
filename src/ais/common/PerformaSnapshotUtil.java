package ais.common;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.PerformaLog;

/**
 * Util pusat untuk membaca, menganalisa, dan mencatat performa runtime JVM ke tabel
 * {@code performa_log}.
 *
 * <p>
 * Util ini berperan sebagai "listener performa": membaca beans {@code java.lang.management}
 * (ThreadMXBean, MemoryMXBean, MemoryPoolMXBean, GarbageCollectorMXBean, ClassLoadingMXBean,
 * RuntimeMXBean, OperatingSystemMXBean) lalu merangkumnya. Setara dengan thread dump + jstat
 * yang biasa diambil manual di server, tetapi tersimpan rapi di database.
 * </p>
 *
 * <p>
 * Selain metrik angka, util ini melakukan AGREGASI untuk investigasi:
 * </p>
 * <ul>
 * <li>Top class yang muncul di stack thread (mendeteksi kode aplikasi yang banyak dipakai/macet).</li>
 * <li>Grup thread berdasarkan pola nama (mendeteksi thread yang dibuat tak terkendali).</li>
 * <li>Kontensi lock (mendeteksi banyak thread yang menunggu lock yang sama, mis. pool koneksi).</li>
 * <li>Rincian memory pool (Eden/Survivor/Old/Metaspace/Code Cache) seperti kolom jstat.</li>
 * <li>Thread mencurigakan (RUNNABLE yang sedang menjalankan kode aplikasi).</li>
 * <li>Rekomendasi tindakan berbasis aturan.</li>
 * </ul>
 *
 * <p>
 * {@link #ambilHistogramTeks()} memanggil Diagnostic Command MBean ({@code GC.class_histogram})
 * untuk mendapatkan histogram objek per-class (setara {@code jmap -histo:live}) — alat utama
 * mencari kebocoran memori. Operasi ini BERAT (memicu Full GC) sehingga hanya dijalankan
 * on-demand lewat tombol, bukan pada snapshot berkala.
 * </p>
 *
 * <p>Kompatibel Java 1.6/1.7 (tanpa lambda, stream, try-with-resources, atau diamond operator).</p>
 */
public class PerformaSnapshotUtil {

	private static final Logger log = Logger.getLogger(PerformaSnapshotUtil.class);

	public static final String KEY_AKTIF = "performa_log_aktif";
	public static final String KEY_DATABASE = "performa_log_simpan_database";
	public static final String KEY_INTERVAL_MENIT = "performa_log_interval_menit";

	public static final int DEFAULT_INTERVAL_MENIT = 5;
	private static final int STACK_DEPTH = 24;
	private static final int MAKS_THREAD_DETAIL = 25;
	private static final int MAKS_BARIS_RINGKAS = 12;

	private static final Object LOCK = new Object();
	private static volatile Thread schedulerThread = null;
	private static volatile boolean stopScheduler = false;

	private PerformaSnapshotUtil() {
	}

	// ============================================================ Tipe hasil

	/** Satu pasangan label-nilai untuk tabel agregasi (mis. class -> jumlah). */
	public static class Hitung {
		public String label;
		public long nilai;
		public String ekstra;

		public Hitung(String label, long nilai, String ekstra) {
			this.label = label;
			this.nilai = nilai;
			this.ekstra = ekstra;
		}
	}

	/** Kumpulan hasil analisa performa lengkap untuk satu waktu (tidak disimpan ke DB). */
	public static class Analisa {
		public PerformaLog ringkas = new PerformaLog();
		public List<Hitung> topKelas = new ArrayList<Hitung>();
		public List<Hitung> topKelasAis = new ArrayList<Hitung>();
		public List<Hitung> grupThread = new ArrayList<Hitung>();
		public List<Hitung> grupThreadAktif = new ArrayList<Hitung>();
		public List<Hitung> kontensiLock = new ArrayList<Hitung>();
		public List<Hitung> memoryPool = new ArrayList<Hitung>();
		public List<Hitung> gcDetail = new ArrayList<Hitung>();
		public List<String> threadMencurigakan = new ArrayList<String>();
		public List<String> deadlockDetail = new ArrayList<String>();
		public List<String> rekomendasi = new ArrayList<String>();
		public String infoVm = "";
	}

	// ============================================================ Analisa utama

	/**
	 * Membaca dan menganalisa kondisi JVM saat ini secara menyeluruh. Tidak menyimpan ke DB.
	 * Semua pembacaan dibungkus try/catch agar satu metrik gagal tidak membatalkan keseluruhan.
	 */
	public static Analisa analisaLengkap() {
		Analisa analisa = new Analisa();
		PerformaLog performaLog = analisa.ringkas;
		performaLog.setTanggal_dirubah(new Date());

		StringBuilder detail = new StringBuilder(16384);
		detail.append("=== SNAPSHOT PERFORMA JVM ECAMPUS ===\n");
		detail.append("Waktu        : ").append(new Date()).append("\n");

		// ---------------------------------------------------------------- Thread
		int blocked = 0;
		int waiting = 0;
		int timedWaiting = 0;
		int runnable = 0;
		int threadBaru = 0;
		int terminated = 0;
		ThreadInfo[] infos = null;
		ThreadMXBean threadBean = null;

		Map<String, Integer> kelasCount = new HashMap<String, Integer>();
		Map<String, Integer> kelasAisCount = new HashMap<String, Integer>();
		Map<String, Integer> grupCount = new HashMap<String, Integer>();
		Map<String, Integer> grupAktifCount = new HashMap<String, Integer>();
		Map<String, int[]> lockWaiter = new HashMap<String, int[]>();
		Map<String, String> lockOwner = new HashMap<String, String>();

		try {
			threadBean = ManagementFactory.getThreadMXBean();
			performaLog.setJumlahThread(Integer.valueOf(threadBean.getThreadCount()));
			performaLog.setJumlahThreadPuncak(Integer.valueOf(threadBean.getPeakThreadCount()));
			performaLog.setJumlahDaemon(Integer.valueOf(threadBean.getDaemonThreadCount()));
			performaLog.setTotalThreadDimulai(Long.valueOf(threadBean.getTotalStartedThreadCount()));

			long[] ids = threadBean.getAllThreadIds();
			infos = threadBean.getThreadInfo(ids, STACK_DEPTH);
			if (infos != null) {
				for (int i = 0; i < infos.length; i++) {
					ThreadInfo info = infos[i];
					if (info == null) {
						continue;
					}
					Thread.State state = info.getThreadState();
					if (state == Thread.State.BLOCKED) {
						blocked++;
					} else if (state == Thread.State.WAITING) {
						waiting++;
					} else if (state == Thread.State.TIMED_WAITING) {
						timedWaiting++;
					} else if (state == Thread.State.RUNNABLE) {
						runnable++;
					} else if (state == Thread.State.NEW) {
						threadBaru++;
					} else if (state == Thread.State.TERMINATED) {
						terminated++;
					}

					boolean idleWorker = adalahWorkerIdle(info);
					String grup = grupNamaThread(info.getThreadName());
					tambah(grupCount, grup, 1);
					if (!idleWorker) {
						tambah(grupAktifCount, grup, 1);
					}
					hitungKelasStack(info, kelasCount, kelasAisCount);
					hitungKontensiLock(info, state, idleWorker, lockWaiter, lockOwner);
				}
			}
		} catch (Throwable t) {
			tulisGagal(detail, "Thread", t);
		}
		performaLog.setJumlahRunnable(Integer.valueOf(runnable));
		performaLog.setJumlahWaiting(Integer.valueOf(waiting));
		performaLog.setJumlahTimedWaiting(Integer.valueOf(timedWaiting));
		performaLog.setJumlahBlocked(Integer.valueOf(blocked));
		performaLog.setJumlahNew(Integer.valueOf(threadBaru));
		performaLog.setJumlahTerminated(Integer.valueOf(terminated));

		analisa.topKelas = urutkan(kelasCount, 12);
		analisa.topKelasAis = urutkan(kelasAisCount, 12);
		analisa.grupThread = urutkan(grupCount, 12);
		analisa.grupThreadAktif = urutkan(grupAktifCount, 12);
		analisa.kontensiLock = urutkanLock(lockWaiter, lockOwner, 12);
		analisa.threadMencurigakan = bangunThreadMencurigakan(infos);

		// Sinyal kontensi lock NYATA (di luar worker pool yang idle): jumlah thread terbanyak yang
		// menumpuk pada SATU lock/objek yang sama. Dipakai PerformaLog.getStatusKesehatan() untuk
		// menentukan Perhatian/Kritis, menggantikan rasio WAITING/total lama yang salah menghitung
		// worker idle (alarm palsu). Pola pool koneksi habis (awaitAvailable) tetap terjaring.
		long kontensiTertinggi = analisa.kontensiLock.isEmpty() ? 0L : analisa.kontensiLock.get(0).nilai;
		performaLog.setMaxThreadSatuLock(Integer.valueOf((int) kontensiTertinggi));

		if (!analisa.topKelasAis.isEmpty()) {
			performaLog.setTopClass(potong(analisa.topKelasAis.get(0).label, 250));
		} else if (!analisa.topKelas.isEmpty()) {
			performaLog.setTopClass(potong(analisa.topKelas.get(0).label, 250));
		}

		// ---------------------------------------------------------------- Deadlock
		int deadlock = 0;
		long[] deadlockIds = null;
		try {
			if (threadBean != null) {
				long[] dl = threadBean.findDeadlockedThreads();
				if (dl == null) {
					dl = threadBean.findMonitorDeadlockedThreads();
				}
				if (dl != null) {
					deadlock = dl.length;
					deadlockIds = dl;
				}
			}
		} catch (Throwable t) {
			tulisGagal(detail, "Deadlock", t);
		}
		performaLog.setJumlahDeadlock(Integer.valueOf(deadlock));
		analisa.deadlockDetail = bangunDeadlockDetail(threadBean, deadlockIds);

		// ---------------------------------------------------------------- Memori
		try {
			MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
			MemoryUsage heap = memoryBean.getHeapMemoryUsage();
			MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
			if (heap != null) {
				performaLog.setHeapDipakai(Long.valueOf(heap.getUsed()));
				performaLog.setHeapMaksimum(Long.valueOf(heap.getMax()));
			}
			if (nonHeap != null) {
				performaLog.setNonHeapDipakai(Long.valueOf(nonHeap.getUsed()));
			}
		} catch (Throwable t) {
			tulisGagal(detail, "Memori", t);
		}

		// ---------------------------------------------------------------- Memory pool (jstat-like)
		try {
			List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
			if (pools != null) {
				for (int i = 0; i < pools.size(); i++) {
					MemoryPoolMXBean pool = pools.get(i);
					if (pool == null) {
						continue;
					}
					MemoryUsage usage = pool.getUsage();
					if (usage == null) {
						continue;
					}
					long used = usage.getUsed();
					long max = usage.getMax();
					String nama = pool.getName() == null ? "?" : pool.getName();
					int persen = max <= 0L ? -1 : (int) ((used * 100L) / max);
					String ekstra = formatBytes(Long.valueOf(used)) + " / " + formatBytes(Long.valueOf(max))
							+ (persen < 0 ? "" : (" (" + persen + "%)"));
					analisa.memoryPool.add(new Hitung(nama, persen < 0 ? 0 : persen, ekstra));

					String namaLower = nama.toLowerCase();
					if (namaLower.indexOf("old gen") >= 0 || namaLower.indexOf("tenured") >= 0) {
						performaLog.setOldGenDipakai(Long.valueOf(used));
						performaLog.setOldGenMaksimum(Long.valueOf(max));
					} else if (namaLower.indexOf("metaspace") >= 0) {
						performaLog.setMetaspaceDipakai(Long.valueOf(used));
						performaLog.setMetaspaceMaksimum(Long.valueOf(max));
					}
				}
			}
		} catch (Throwable t) {
			tulisGagal(detail, "Memory Pool", t);
		}

		// ---------------------------------------------------------------- Sistem & kelas
		try {
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
			performaLog.setBebanSistem(Double.valueOf(osBean.getSystemLoadAverage()));
			performaLog.setJumlahProsesor(Integer.valueOf(osBean.getAvailableProcessors()));
		} catch (Throwable t) {
			tulisGagal(detail, "Sistem Operasi", t);
		}
		Double cpu = ambilProcessCpuLoad();
		if (cpu != null) {
			performaLog.setProsesCpuPersen(cpu);
		}
		try {
			ClassLoadingMXBean classBean = ManagementFactory.getClassLoadingMXBean();
			performaLog.setJumlahKelas(Integer.valueOf(classBean.getLoadedClassCount()));
		} catch (Throwable t) {
			tulisGagal(detail, "Class Loading", t);
		}

		// ---------------------------------------------------------------- Garbage collector
		long gcCount = 0L;
		long gcTime = 0L;
		try {
			List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
			if (gcs != null) {
				for (int i = 0; i < gcs.size(); i++) {
					GarbageCollectorMXBean gc = gcs.get(i);
					if (gc == null) {
						continue;
					}
					long count = gc.getCollectionCount();
					long time = gc.getCollectionTime();
					if (count > 0L) {
						gcCount += count;
					}
					if (time > 0L) {
						gcTime += time;
					}
					analisa.gcDetail.add(new Hitung(gc.getName(), count < 0L ? 0L : count,
							(time < 0L ? 0L : time) + " ms"));
				}
			}
		} catch (Throwable t) {
			tulisGagal(detail, "Garbage Collector", t);
		}
		performaLog.setGcJumlah(Long.valueOf(gcCount));
		performaLog.setGcWaktuMillis(Long.valueOf(gcTime));

		// ---------------------------------------------------------------- VM info & uptime
		try {
			RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
			performaLog.setUptimeMillis(Long.valueOf(runtimeBean.getUptime()));
			analisa.infoVm = bangunInfoVm(runtimeBean);
		} catch (Throwable t) {
			tulisGagal(detail, "Runtime", t);
		}

		// ---------------------------------------------------------------- Rekomendasi
		analisa.rekomendasi = bangunRekomendasi(performaLog, analisa);

		// ---------------------------------------------------------------- Laporan teks rinci
		detail.append("Status       : ").append(performaLog.getStatusKesehatan()).append("\n");
		detail.append("Thread total : ").append(nz(performaLog.getJumlahThread()))
				.append(" (puncak ").append(nz(performaLog.getJumlahThreadPuncak()))
				.append(", daemon ").append(nz(performaLog.getJumlahDaemon()))
				.append(", total dimulai ").append(nzLong(performaLog.getTotalThreadDimulai())).append(")\n");
		detail.append("Status thread: RUNNABLE=").append(runnable)
				.append(", WAITING=").append(waiting)
				.append(", TIMED_WAITING=").append(timedWaiting)
				.append(", BLOCKED=").append(blocked)
				.append(", NEW=").append(threadBaru)
				.append(", TERMINATED=").append(terminated).append("\n");
		detail.append("Deadlock     : ").append(deadlock).append("\n");
		detail.append("Heap         : ").append(formatBytes(performaLog.getHeapDipakai()))
				.append(" / ").append(formatBytes(performaLog.getHeapMaksimum()))
				.append(" (").append(performaLog.getPersenHeap()).append("%)\n");
		detail.append("Old Gen      : ").append(formatBytes(performaLog.getOldGenDipakai()))
				.append(" / ").append(formatBytes(performaLog.getOldGenMaksimum()))
				.append(" (").append(performaLog.getPersenOldGen()).append("%)\n");
		detail.append("Metaspace    : ").append(formatBytes(performaLog.getMetaspaceDipakai()))
				.append(" / ").append(formatBytes(performaLog.getMetaspaceMaksimum()))
				.append(" (").append(performaLog.getPersenMetaspace()).append("%)\n");
		detail.append("Non-Heap     : ").append(formatBytes(performaLog.getNonHeapDipakai())).append("\n");
		detail.append("Beban sistem : ").append(performaLog.getBebanSistem())
				.append(", CPU proses ").append(cpu == null ? "-" : (Math.round(cpu.doubleValue() * 10.0) / 10.0) + "%")
				.append(" (prosesor ").append(nz(performaLog.getJumlahProsesor())).append(")\n");
		detail.append("Kelas dimuat : ").append(nz(performaLog.getJumlahKelas())).append("\n");
		detail.append("GC total     : ").append(gcCount).append("x, ").append(gcTime).append(" ms\n");
		appendListGc(detail, analisa.gcDetail);
		detail.append("Uptime       : ").append(formatDurasi(performaLog.getUptimeMillis())).append("\n");
		if (analisa.infoVm.length() > 0) {
			detail.append(analisa.infoVm);
		}

		appendBagian(detail, "REKOMENDASI", analisa.rekomendasi);
		appendBagianHitung(detail, "TOP CLASS DI STACK (semua)", analisa.topKelas, "thread");
		appendBagianHitung(detail, "TOP CLASS DI STACK (aplikasi ais.*)", analisa.topKelasAis, "thread");
		appendBagianHitung(detail, "GRUP THREAD", analisa.grupThread, "thread");
		appendBagianHitung(detail, "KONTENSI LOCK (thread menunggu lock yang sama)", analisa.kontensiLock, "penunggu");
		appendBagian(detail, "THREAD MENCURIGAKAN (RUNNABLE menjalankan kode aplikasi)", analisa.threadMencurigakan);
		appendBagian(detail, "DETAIL DEADLOCK", analisa.deadlockDetail);
		appendThreadMenungguDetail(detail, infos);

		performaLog.setKeterangan(detail.toString());
		return analisa;
	}

	/**
	 * Membaca snapshot performa JVM saat ini sebagai {@link PerformaLog} yang BELUM disimpan.
	 */
	public static PerformaLog captureNow() {
		return analisaLengkap().ringkas;
	}

	/**
	 * Membaca snapshot lalu menyimpannya ke database bila konfigurasi aktif.
	 */
	public static Long recordNow(String oleh, String olehId) {
		if (!isAktif() || !isDatabaseAktif()) {
			return null;
		}
		return simpan(oleh, olehId, "Gagal merekam snapshot performa");
	}

	/**
	 * Sama seperti {@link #recordNow(String, String)} tetapi MENGABAIKAN gerbang konfigurasi
	 * {@code performa_log_aktif}. Dipakai oleh tombol manual "Rekam Sekarang".
	 */
	public static Long recordPaksa(String oleh, String olehId) {
		return simpan(oleh, olehId, "Gagal merekam snapshot performa (paksa)");
	}

	private static Long simpan(String oleh, String olehId, String pesanGagal) {
		try {
			PerformaLog performaLog = captureNow();
			performaLog.setOleh(oleh == null || oleh.trim().length() == 0 ? "Sistem" : oleh);
			performaLog.setOlehId(olehId == null || olehId.trim().length() == 0 ? "sistem" : olehId);
			return saveToDatabase(performaLog);
		} catch (Throwable t) {
			try {
				log.error(pesanGagal, t);
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:431");
			}
			return null;
		}
	}

	private static Long saveToDatabase(PerformaLog performaLog) {
		// Kolom max_thread_satu_lock dibuat otomatis oleh Hibernate saat startup (hbm2ddl.auto=update);
		// tidak perlu ALTER TABLE manual. PerformaLog TIDAK @Audited, jadi caveat drift tabel audit
		// new_audit (lihat hibernate.cfg.xml) tidak berlaku untuk tabel ini.
		Session session = null;
		Transaction transaction = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			transaction = session.beginTransaction();
			session.save(performaLog);
			transaction.commit();
			return performaLog.getId();
		} catch (Throwable t) {
			try {
				if (transaction != null && transaction.isActive()) {
					transaction.rollback();
				}
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:454");
			}
			try {
				log.error("Gagal menyimpan snapshot performa ke tabel performa_log", t);
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:458");
			}
			return null;
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:465");
				}
				try {
					session.disconnect();
				} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:469");
				}
				try {
					session.close();
				} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:473");
				}
			}
		}
	}

	// ============================================================ Histogram objek (berat)

	/**
	 * Mengambil histogram objek per-class memakai Diagnostic Command MBean
	 * ({@code GC.class_histogram}), setara {@code jmap -histo:live}. <b>BERAT</b>: memicu Full GC.
	 * Mengembalikan teks histogram mentah, atau pesan kesalahan singkat bila tidak didukung.
	 */
	public static String ambilHistogramTeks() {
		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			ObjectName name = new ObjectName("com.sun.management:type=DiagnosticCommand");
			Object hasil = server.invoke(name, "gcClassHistogram",
					new Object[] { new String[0] }, new String[] { "[Ljava.lang.String;" });
			return hasil == null ? "" : hasil.toString();
		} catch (Throwable t) {
			try {
				log.error("Gagal mengambil histogram objek", t);
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:496");
			}
			return "[Histogram objek tidak tersedia pada JVM ini: "
					+ (t == null ? "" : t.getClass().getSimpleName()) + "]";
		}
	}

	/**
	 * Mengurai teks histogram (hasil {@link #ambilHistogramTeks()}) menjadi daftar class teratas
	 * berdasarkan ukuran byte. {@code ekstra} berisi jumlah instance.
	 */
	public static List<Hitung> parseHistogram(String teks, int limit) {
		List<Hitung> hasil = new ArrayList<Hitung>();
		if (teks == null || teks.trim().length() == 0) {
			return hasil;
		}
		String[] lines = teks.replace('\r', '\n').split("\\n");
		for (int i = 0; i < lines.length && hasil.size() < limit; i++) {
			String line = lines[i] == null ? "" : lines[i].trim();
			if (line.length() == 0 || line.startsWith("num") || line.startsWith("---")
					|| line.startsWith("Total")) {
				continue;
			}
			if (line.indexOf(':') < 0) {
				continue;
			}
			String[] token = line.split("\\s+");
			if (token.length < 4 || !token[0].endsWith(":")) {
				continue;
			}
			try {
				long instances = Long.parseLong(token[1]);
				long bytes = Long.parseLong(token[2]);
				StringBuilder kelas = new StringBuilder();
				for (int k = 3; k < token.length; k++) {
					if (kelas.length() > 0) {
						kelas.append(' ');
					}
					kelas.append(token[k]);
				}
				hasil.add(new Hitung(rapikanNamaKelas(kelas.toString()), bytes, instances + " objek"));
			} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:537");
			}
		}
		return hasil;
	}

	// ============================================================ Scheduler

	public static void ensureSchedulerStarted() {
		if (schedulerThread != null && schedulerThread.isAlive()) {
			return;
		}
		synchronized (LOCK) {
			if (schedulerThread != null && schedulerThread.isAlive()) {
				return;
			}
			stopScheduler = false;
			Thread thread = new Thread(new Runnable() {
				public void run() {
					jalankanLoopPerekam();
				}
			}, "AIS-Performa-Recorder");
			thread.setDaemon(true);
			thread.setPriority(Thread.MIN_PRIORITY);
			schedulerThread = thread;
			thread.start();
			try {
				log.info("Scheduler perekam performa dimulai.");
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:565");
			}
		}
	}

	public static void stopScheduler() {
		stopScheduler = true;
		Thread thread = schedulerThread;
		if (thread != null) {
			try {
				thread.interrupt();
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:576");
			}
		}
		schedulerThread = null;
	}

	private static void jalankanLoopPerekam() {
		while (!stopScheduler && !Thread.currentThread().isInterrupted()) {
			long tidurMillis = ambilIntervalMenit() * 60L * 1000L;
			try {
				Thread.sleep(tidurMillis);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			if (stopScheduler) {
				break;
			}
			try {
				recordNow("Sistem", "sistem");
			} catch (Throwable t) {
				try {
					log.error("Loop perekam performa gagal pada satu siklus", t);
				} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:599");
				}
			}
		}
	}

	private static int ambilIntervalMenit() {
		try {
			String value = getKonfigurasi(KEY_INTERVAL_MENIT, String.valueOf(DEFAULT_INTERVAL_MENIT));
			int menit = Integer.parseInt(value == null ? String.valueOf(DEFAULT_INTERVAL_MENIT) : value.trim());
			if (menit < 1) {
				menit = 1;
			}
			if (menit > 1440) {
				menit = 1440;
			}
			return menit;
		} catch (Throwable t) {
			return DEFAULT_INTERVAL_MENIT;
		}
	}

	// ============================================================ Agregasi internal

	private static void hitungKelasStack(ThreadInfo info, Map<String, Integer> kelasCount,
			Map<String, Integer> kelasAisCount) {
		StackTraceElement[] stack = info.getStackTrace();
		if (stack == null || stack.length == 0) {
			return;
		}
		// Hitung KEHADIRAN class dalam satu thread (distinct), bukan tiap frame, supaya
		// loop rekursif tidak menggelembungkan angka.
		Set<String> kelasDiThread = new HashSet<String>();
		Set<String> kelasAisDiThread = new HashSet<String>();
		for (int i = 0; i < stack.length; i++) {
			String kelas = stack[i] == null ? null : stack[i].getClassName();
			if (kelas == null || kelas.length() == 0) {
				continue;
			}
			if ("java.lang.Thread".equals(kelas) || "java.lang.Object".equals(kelas)) {
				continue;
			}
			kelasDiThread.add(kelas);
			if (kelas.startsWith("ais.")) {
				kelasAisDiThread.add(kelas);
			}
		}
		for (java.util.Iterator<String> it = kelasDiThread.iterator(); it.hasNext();) {
			tambah(kelasCount, it.next(), 1);
		}
		for (java.util.Iterator<String> it = kelasAisDiThread.iterator(); it.hasNext();) {
			tambah(kelasAisCount, it.next(), 1);
		}
	}

	/**
	 * True bila thread adalah worker pool yang sedang IDLE menunggu tugas (parkir di antrean),
	 * bukan sedang menahan/berebut lock aplikasi. Contoh: worker Tomcat AJP/HTTP, ThreadPoolExecutor,
	 * Timer, dan pool Cocoon yang menunggu di {@code getTask()/poll()/take()/mainLoop()}. Thread
	 * seperti ini WAJAR berjumlah banyak dan menunggu "lock" (ConditionObject antrean) yang SAMA,
	 * sehingga TIDAK boleh dihitung sebagai kontensi lock — kalau dihitung, snapshot yang idle pun
	 * tampak "Kritis" (alarm palsu "100+ thread menunggu lock yang sama"). Kontensi nyata seperti
	 * pool koneksi c3p0 habis ({@code BasicResourcePool.awaitAvailable}) TIDAK cocok pola ini,
	 * jadi tetap terhitung.
	 */
	private static boolean adalahWorkerIdle(ThreadInfo info) {
		if (info == null) {
			return false;
		}
		Thread.State state = info.getThreadState();
		if (state != Thread.State.WAITING && state != Thread.State.TIMED_WAITING) {
			return false;
		}
		StackTraceElement[] stack = info.getStackTrace();
		if (stack == null) {
			return false;
		}
		int batas = stack.length < 8 ? stack.length : 8;
		for (int i = 0; i < batas; i++) {
			StackTraceElement f = stack[i];
			if (f == null) {
				continue;
			}
			String cls = f.getClassName();
			String mtd = f.getMethodName();
			if (cls == null || mtd == null) {
				continue;
			}
			if ("getTask".equals(mtd) && cls.indexOf("ThreadPoolExecutor") >= 0) {
				return true;
			}
			if (("take".equals(mtd) || "poll".equals(mtd))
					&& (cls.indexOf("LinkedBlockingQueue") >= 0 || cls.indexOf("SynchronousQueue") >= 0
							|| cls.indexOf("DelayedWorkQueue") >= 0 || cls.indexOf("TaskQueue") >= 0
							|| cls.indexOf("ChannelWrapper") >= 0 || cls.indexOf("LinkedQueue") >= 0)) {
				return true;
			}
			if ("run".equals(mtd) && cls.indexOf("ThreadPoolAsynchronousRunner") >= 0) {
				// Housekeeping c3p0 yang IDLE (PoolThread menunggu tugas async di runner-nya) — bukan
				// kontensi. Pool KONEKSI habis polanya beda (BasicResourcePool.awaitAvailable) & tetap terhitung.
				return true;
			}
			if ("mainLoop".equals(mtd) && cls.indexOf("TimerThread") >= 0) {
				return true;
			}
		}
		return false;
	}

	private static void hitungKontensiLock(ThreadInfo info, Thread.State state, boolean idleWorker,
			Map<String, int[]> lockWaiter, Map<String, String> lockOwner) {
		if (idleWorker) {
			// Worker pool yang idle (menunggu tugas di antrean) bukan kontensi lock nyata.
			return;
		}
		if (state != Thread.State.BLOCKED && state != Thread.State.WAITING
				&& state != Thread.State.TIMED_WAITING) {
			return;
		}
		String lockName = info.getLockName();
		if (lockName == null || lockName.length() == 0) {
			return;
		}
		int[] counter = lockWaiter.get(lockName);
		if (counter == null) {
			counter = new int[] { 0 };
			lockWaiter.put(lockName, counter);
		}
		counter[0]++;
		if (info.getLockOwnerName() != null && !lockOwner.containsKey(lockName)) {
			lockOwner.put(lockName, info.getLockOwnerName() + " #" + info.getLockOwnerId());
		}
	}

	private static List<String> bangunThreadMencurigakan(ThreadInfo[] infos) {
		List<String> hasil = new ArrayList<String>();
		if (infos == null) {
			return hasil;
		}
		for (int i = 0; i < infos.length && hasil.size() < MAKS_THREAD_DETAIL; i++) {
			ThreadInfo info = infos[i];
			if (info == null || info.getThreadState() != Thread.State.RUNNABLE) {
				continue;
			}
			StackTraceElement[] stack = info.getStackTrace();
			if (stack == null) {
				continue;
			}
			StackTraceElement frameAis = null;
			for (int j = 0; j < stack.length; j++) {
				if (stack[j] != null && stack[j].getClassName() != null
						&& stack[j].getClassName().startsWith("ais.")) {
					frameAis = stack[j];
					break;
				}
			}
			if (frameAis == null) {
				continue;
			}
			hasil.add("\"" + info.getThreadName() + "\" #" + info.getThreadId() + " -> " + frameAis);
		}
		return hasil;
	}

	private static List<String> bangunDeadlockDetail(ThreadMXBean threadBean, long[] deadlockIds) {
		List<String> hasil = new ArrayList<String>();
		if (threadBean == null || deadlockIds == null || deadlockIds.length == 0) {
			return hasil;
		}
		try {
			ThreadInfo[] infos = threadBean.getThreadInfo(deadlockIds, STACK_DEPTH);
			if (infos != null) {
				for (int i = 0; i < infos.length; i++) {
					ThreadInfo info = infos[i];
					if (info == null) {
						continue;
					}
					StringBuilder baris = new StringBuilder();
					baris.append("\"").append(info.getThreadName()).append("\" #").append(info.getThreadId());
					if (info.getLockName() != null) {
						baris.append(" menunggu ").append(info.getLockName());
					}
					if (info.getLockOwnerName() != null) {
						baris.append(" dipegang oleh \"").append(info.getLockOwnerName())
								.append("\" #").append(info.getLockOwnerId());
					}
					hasil.add(baris.toString());
				}
			}
		} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:788");
		}
		return hasil;
	}

	private static List<String> bangunRekomendasi(PerformaLog p, Analisa analisa) {
		List<String> rekomendasi = new ArrayList<String>();
		if (nz(p.getJumlahDeadlock()) > 0) {
			rekomendasi.add("DEADLOCK terdeteksi (" + nz(p.getJumlahDeadlock())
					+ " thread). Lihat detail deadlock; perbaiki urutan pengambilan lock.");
		}
		if (!analisa.kontensiLock.isEmpty()) {
			Hitung top = analisa.kontensiLock.get(0);
			if (top.nilai >= 5) {
				String saran = top.label != null && (top.label.toLowerCase().indexOf("resourcepool") >= 0
						|| top.label.toLowerCase().indexOf("c3p0") >= 0)
								? " Pola ini khas POOL KONEKSI DB HABIS: periksa Session/koneksi yang tidak ditutup (openSession tanpa close) dan ukuran maxPoolSize."
								: "";
				rekomendasi.add(top.nilai + " thread menunggu lock yang sama (" + top.label + ")." + saran);
			}
		}
		if (!analisa.grupThreadAktif.isEmpty()) {
			Hitung top = analisa.grupThreadAktif.get(0);
			// Pakai jumlah thread AKTIF (bukan yang idle menunggu tugas di antrean) supaya pool
			// worker yang wajar-idle tidak salah divonis "membengkak / dibuat tak terkendali".
			if (top.nilai >= 50) {
				rekomendasi.add("Grup thread '" + top.label + "' punya " + top.nilai
						+ " thread AKTIF (di luar yang idle di antrean). Indikasi beban tinggi/kebocoran thread; pastikan ada pembatasan jumlah dan penghentian thread.");
			}
		}
		int heap = p.getPersenHeap();
		if (heap >= 0 && heap >= 85) {
			rekomendasi.add("Heap tinggi (" + heap + "%). Jalankan 'Ambil Histogram Objek' untuk mencari class pemakan memori terbesar.");
		}
		int oldGen = p.getPersenOldGen();
		if (oldGen >= 0 && oldGen >= 85) {
			rekomendasi.add("Old Gen tinggi (" + oldGen + "%). Objek bertahan lama; indikasi kebocoran memori (cache/proxy tak dilepas).");
		}
		int meta = p.getPersenMetaspace();
		if (meta >= 0 && meta >= 85) {
			rekomendasi.add("Metaspace tinggi (" + meta + "%). Waspadai kebocoran classloader/proxy (mis. Javassist/CGLIB) atau redeploy berulang.");
		}
		// 1-2 thread BLOCKED itu transien & normal (menunggu sesaat masuk blok synchronized) -> jangan
		// jadikan rekomendasi masalah. Hanya peringatkan bila cukup banyak (lock convoy nyata), selaras
		// dengan ambang status di PerformaLog.getStatusKesehatan().
		if (nz(p.getJumlahBlocked()) >= 3) {
			rekomendasi.add("Ada " + nz(p.getJumlahBlocked())
					+ " thread BLOCKED (rebutan monitor). Periksa blok synchronized yang lama.");
		}
		if (!analisa.topKelasAis.isEmpty()) {
			Hitung top = analisa.topKelasAis.get(0);
			if (top.nilai >= 10) {
				rekomendasi.add("Class aplikasi '" + top.label + "' muncul di " + top.nilai
						+ " thread. Telusuri apakah kode ini lambat/macet atau dipanggil terlalu sering.");
			}
		}
		if (rekomendasi.isEmpty()) {
			rekomendasi.add("Tidak ada indikasi masalah performa pada snapshot ini.");
		}
		return rekomendasi;
	}

	private static String bangunInfoVm(RuntimeMXBean runtimeBean) {
		StringBuilder builder = new StringBuilder(512);
		try {
			builder.append("VM           : ").append(runtimeBean.getVmName()).append(" ")
					.append(runtimeBean.getVmVersion()).append("\n");
		} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:855");
		}
		try {
			List<String> args = runtimeBean.getInputArguments();
			if (args != null) {
				StringBuilder penting = new StringBuilder();
				for (int i = 0; i < args.size(); i++) {
					String a = args.get(i);
					if (a == null) {
						continue;
					}
					if (a.indexOf("-Xmx") >= 0 || a.indexOf("-Xms") >= 0 || a.toLowerCase().indexOf("gc") >= 0
							|| a.indexOf("Metaspace") >= 0 || a.indexOf("HeapSize") >= 0) {
						if (penting.length() > 0) {
							penting.append(" ");
						}
						penting.append(a);
					}
				}
				if (penting.length() > 0) {
					builder.append("Argumen VM   : ").append(penting).append("\n");
				}
			}
		} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:878");
		}
		return builder.toString();
	}

	private static Double ambilProcessCpuLoad() {
		try {
			OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
			java.lang.reflect.Method m = os.getClass().getMethod("getProcessCpuLoad", new Class[0]);
			m.setAccessible(true);
			Object hasil = m.invoke(os, new Object[0]);
			if (hasil instanceof Number) {
				double v = ((Number) hasil).doubleValue();
				if (v >= 0) {
					return Double.valueOf(v * 100.0);
				}
			}
		} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:895");
		}
		return null;
	}

	// ============================================================ Util format

	private static void tambah(Map<String, Integer> map, String key, int value) {
		if (key == null || key.length() == 0) {
			return;
		}
		Integer lama = map.get(key);
		map.put(key, Integer.valueOf((lama == null ? 0 : lama.intValue()) + value));
	}

	private static List<Hitung> urutkan(Map<String, Integer> map, int limit) {
		List<Hitung> list = new ArrayList<Hitung>();
		for (java.util.Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Integer> e = it.next();
			list.add(new Hitung(e.getKey(), e.getValue().longValue(), null));
		}
		Collections.sort(list, new Comparator<Hitung>() {
			public int compare(Hitung a, Hitung b) {
				return a.nilai < b.nilai ? 1 : (a.nilai > b.nilai ? -1 : 0);
			}
		});
		return list.size() > limit ? new ArrayList<Hitung>(list.subList(0, limit)) : list;
	}

	private static List<Hitung> urutkanLock(Map<String, int[]> lockWaiter, Map<String, String> lockOwner, int limit) {
		List<Hitung> list = new ArrayList<Hitung>();
		for (java.util.Iterator<Map.Entry<String, int[]>> it = lockWaiter.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, int[]> e = it.next();
			String owner = lockOwner.get(e.getKey());
			list.add(new Hitung(e.getKey(), e.getValue()[0], owner == null ? "tanpa pemilik" : ("pemilik: " + owner)));
		}
		Collections.sort(list, new Comparator<Hitung>() {
			public int compare(Hitung a, Hitung b) {
				return a.nilai < b.nilai ? 1 : (a.nilai > b.nilai ? -1 : 0);
			}
		});
		return list.size() > limit ? new ArrayList<Hitung>(list.subList(0, limit)) : list;
	}

	/** Membuat kunci grup dari nama thread dengan mengganti angka jadi '#'. */
	private static String grupNamaThread(String nama) {
		if (nama == null || nama.length() == 0) {
			return "(tanpa nama)";
		}
		String key = nama.replaceAll("[0-9]+", "#");
		return key.length() > 60 ? key.substring(0, 60) : key;
	}

	private static String rapikanNamaKelas(String nama) {
		if (nama == null) {
			return "";
		}
		return nama.length() > 120 ? nama.substring(0, 120) + "..." : nama;
	}

	private static String potong(String nilai, int maks) {
		if (nilai == null) {
			return null;
		}
		return nilai.length() > maks ? nilai.substring(0, maks) : nilai;
	}

	private static void appendBagian(StringBuilder detail, String judul, List<String> baris) {
		if (baris == null || baris.isEmpty()) {
			return;
		}
		detail.append("\n=== ").append(judul).append(" ===\n");
		int batas = baris.size() > MAKS_THREAD_DETAIL ? MAKS_THREAD_DETAIL : baris.size();
		for (int i = 0; i < batas; i++) {
			detail.append("- ").append(baris.get(i)).append("\n");
		}
	}

	private static void appendBagianHitung(StringBuilder detail, String judul, List<Hitung> list, String satuan) {
		if (list == null || list.isEmpty()) {
			return;
		}
		detail.append("\n=== ").append(judul).append(" ===\n");
		int batas = list.size() > MAKS_BARIS_RINGKAS ? MAKS_BARIS_RINGKAS : list.size();
		for (int i = 0; i < batas; i++) {
			Hitung h = list.get(i);
			detail.append("- ").append(h.label).append(" : ").append(h.nilai).append(" ").append(satuan);
			if (h.ekstra != null && h.ekstra.length() > 0) {
				detail.append(" (").append(h.ekstra).append(")");
			}
			detail.append("\n");
		}
	}

	private static void appendListGc(StringBuilder detail, List<Hitung> gcDetail) {
		if (gcDetail == null) {
			return;
		}
		for (int i = 0; i < gcDetail.size(); i++) {
			Hitung h = gcDetail.get(i);
			detail.append("  - ").append(h.label).append(": ").append(h.nilai).append("x, ").append(h.ekstra).append("\n");
		}
	}

	private static void appendThreadMenungguDetail(StringBuilder detail, ThreadInfo[] infos) {
		if (infos == null) {
			return;
		}
		StringBuilder isi = new StringBuilder(4096);
		int jumlah = 0;
		for (int i = 0; i < infos.length && jumlah < MAKS_THREAD_DETAIL; i++) {
			ThreadInfo info = infos[i];
			if (info == null) {
				continue;
			}
			Thread.State state = info.getThreadState();
			if (state == Thread.State.BLOCKED || state == Thread.State.WAITING
					|| state == Thread.State.TIMED_WAITING) {
				appendThreadInfo(isi, info);
				jumlah++;
			}
		}
		if (isi.length() > 0) {
			detail.append("\n=== CUPLIKAN THREAD MENUNGGU/TERBLOKIR (maks ")
					.append(MAKS_THREAD_DETAIL).append(") ===\n");
			detail.append(isi);
		}
	}

	private static void appendThreadInfo(StringBuilder detail, ThreadInfo info) {
		if (info == null) {
			return;
		}
		detail.append("\n\"").append(info.getThreadName()).append("\" #").append(info.getThreadId())
				.append(" ").append(info.getThreadState());
		if (info.getLockName() != null) {
			detail.append(" on ").append(info.getLockName());
		}
		if (info.getLockOwnerName() != null) {
			detail.append(" owned by \"").append(info.getLockOwnerName())
					.append("\" #").append(info.getLockOwnerId());
		}
		detail.append("\n");
		StackTraceElement[] stack = info.getStackTrace();
		if (stack != null) {
			for (int i = 0; i < stack.length; i++) {
				detail.append("\tat ").append(stack[i]).append("\n");
			}
		}
	}

	private static void tulisGagal(StringBuilder detail, String bagian, Throwable t) {
		try {
			detail.append("[gagal baca ").append(bagian).append(": ")
					.append(t == null ? "" : t.getClass().getSimpleName()).append("]\n");
		} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:1050");
		}
	}

	public static String formatBytes(Long bytes) {
		if (bytes == null) {
			return "-";
		}
		long value = bytes.longValue();
		if (value < 0L) {
			return "-";
		}
		double kb = value / 1024.0;
		if (kb < 1024.0) {
			return Math.round(kb) + " KB";
		}
		double mb = kb / 1024.0;
		if (mb < 1024.0) {
			return Math.round(mb) + " MB";
		}
		double gb = mb / 1024.0;
		return (Math.round(gb * 100.0) / 100.0) + " GB";
	}

	public static String formatDurasi(Long millis) {
		if (millis == null || millis.longValue() < 0L) {
			return "-";
		}
		long detik = millis.longValue() / 1000L;
		long hari = detik / 86400L;
		long jam = (detik % 86400L) / 3600L;
		long menit = (detik % 3600L) / 60L;
		StringBuilder builder = new StringBuilder();
		if (hari > 0L) {
			builder.append(hari).append(" hari ");
		}
		builder.append(jam).append(" jam ").append(menit).append(" menit");
		return builder.toString();
	}

	private static int nz(Integer value) {
		return value == null ? 0 : value.intValue();
	}

	private static long nzLong(Long value) {
		return value == null ? 0L : value.longValue();
	}

	public static boolean isAktif() {
		return isKonfigurasiActive(KEY_AKTIF, Konfigurasi.AKTIF);
	}

	public static boolean isDatabaseAktif() {
		return isKonfigurasiActive(KEY_DATABASE, Konfigurasi.AKTIF);
	}

	private static boolean isKonfigurasiActive(String key, String defaultValue) {
		String value = getKonfigurasi(key, defaultValue);
		return value == null || value.trim().length() == 0 || Konfigurasi.AKTIF.equalsIgnoreCase(value.trim())
				|| "true".equalsIgnoreCase(value.trim()) || "ya".equalsIgnoreCase(value.trim());
	}

	private static String getKonfigurasi(String key, String defaultValue) {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi(key, defaultValue);
			if (konfigurasi != null && konfigurasi.getNilai() != null) {
				return konfigurasi.getNilai();
			}
		} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/PerformaSnapshotUtil.java:1118");
		}
		return defaultValue;
	}
}
