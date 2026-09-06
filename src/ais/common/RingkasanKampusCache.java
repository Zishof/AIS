package ais.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Mahasiswa;

/**
 * <h2>RingkasanKampusCache — cache angka kelembagaan dalam memori</h2>
 *
 * <p>Menampung JUMLAH (angka) mahasiswa per prodi per status per jenis kelamin,
 * dosen mengajar per prodi, dan calon mahasiswa per prodi — dihitung SEKALI lalu
 * disimpan di memori, di-key per (Tahun Akademik | Semester). Dashboard tinggal
 * membaca + menjumlah sesuai parameter (fakultas/jurusan/TA/semester) TANPA query
 * ulang ke database, kecuali ditekan "Refresh".</p>
 *
 * <p><b>Kunci akurasi:</b> jumlah MAHASISWA dihitung memakai LOGIKA YANG PERSIS SAMA
 * dengan {@code LaporanRekapJumlahMahasiswa.generateDataDanImageAlbum} — iterasi mahasiswa
 * lalu {@code Common.getSemester(...)} + {@code Common.currentStatus(mhs, TA, smt)} — sehingga
 * angka dashboard DIJAMIN identik dengan rekap (bukan aproksimasi SQL).</p>
 *
 * <p>Catatan nama: kelas {@code MemoryCacheUtil} yang sudah ada adalah cache entity
 * (MapDB/Hazelcast) yang BERBEDA fungsi; kelas ini khusus angka ringkasan kampus.</p>
 *
 * <p>Thread-safe (ConcurrentHashMap + build di bawah lock). Java 1.6/1.7, ZK 5.5.</p>
 */
public final class RingkasanKampusCache {

	private RingkasanKampusCache() {
	}

	/** Snapshot angka untuk satu (TA, semester). Semua nilai = angka (count). */
	public static final class Snapshot {
		public final String tahunAkademik;
		public final String semester;
		/** prodiKey "fakultasId_jurusanId" -&gt; (statusMahasiswaId -&gt; int[2]{lk, pr}). */
		public final Map<String, Map<Long, int[]>> mahasiswa = new HashMap<String, Map<Long, int[]>>();
		/** jurusanId -&gt; jumlah dosen yang MENGAJAR (punya jadwal) pada TA/semester. */
		public final Map<Long, Integer> dosenPerJurusan = new HashMap<Long, Integer>();
		/**
		 * Jumlah dosen DISTINCT se-UNIVERSITAS yang mengajar pada TA — seorang dosen yang
		 * mengajar di lebih dari satu prodi DIHITUNG SEKALI (bukan dijumlah per prodi).
		 * Dipakai untuk kartu "Dosen Aktif" dan rasio Mhs/Dosen tingkat universitas agar
		 * angkanya realistis (menjumlah {@link #dosenPerJurusan} akan dobel lintas prodi).
		 */
		public volatile int totalDosenUniversitas = 0;
		/** jurusanId -&gt; jumlah calon mahasiswa (pendaftar) pada TA. */
		public final Map<Long, Integer> calonPerJurusan = new HashMap<Long, Integer>();
		/** tahun angkatan -&gt; (statusId -&gt; int[2]{lk,pr}) — untuk tren per angkatan/tahun. */
		public final Map<Integer, Map<Long, int[]>> perAngkatan = new HashMap<Integer, Map<Long, int[]>>();
		/** program (nama) -&gt; (statusId -&gt; int[2]{lk,pr}) — untuk rekap per program. */
		public final Map<String, Map<Long, int[]>> perProgram = new HashMap<String, Map<Long, int[]>>();
		/** sekolahId -&gt; (statusSiswa -&gt; int[2]{lk,pr}) — profil SEKOLAH. */
		public final Map<Long, Map<String, int[]>> siswaPerSekolah = new HashMap<Long, Map<String, int[]>>();
		/** sekolahId -&gt; jumlah guru aktif. */
		public final Map<Long, Integer> guruPerSekolah = new HashMap<Long, Integer>();
		/** jurusanId -&gt; jumlah PENDAFTAR wisuda (semua yang mendaftar). */
		public final Map<Long, Integer> wisudaPendaftarPerJurusan = new HashMap<Long, Integer>();
		/** jurusanId -&gt; jumlah pendaftar wisuda yang sudah di-ACC (persetujuanWisuda = true). */
		public final Map<Long, Integer> wisudaAccPerJurusan = new HashMap<Long, Integer>();
		public volatile long dibangunPada;
		public volatile boolean siap;

		Snapshot(String ta, String sem) {
			this.tahunAkademik = ta;
			this.semester = sem;
		}
	}

	private static final ConcurrentHashMap<String, Snapshot> CACHE = new ConcurrentHashMap<String, Snapshot>();
	private static final Object BUILD_LOCK = new Object();
	/** Thread/pool milik cache harus dapat dihentikan sebelum classloader webapp dilepas. */
	private static final ConcurrentHashMap<Thread, Boolean> ASYNC_THREADS = new ConcurrentHashMap<Thread, Boolean>();
	private static final ConcurrentHashMap<ExecutorService, Boolean> ACTIVE_POOLS = new ConcurrentHashMap<ExecutorService, Boolean>();
	private static volatile Thread warmupThread = null;
	private static volatile boolean lifecycleDihentikan = false;

	/** Penanda "sedang membangun" per (TA|sem) — single-flight: hanya SATU builder per kunci. */
	private static final ConcurrentHashMap<String, Boolean> BUILDING = new ConcurrentHashMap<String, Boolean>();
	/** Umur maksimum snapshot dianggap segar (ms). Lewat ini → rebuild di latar (stale-while-revalidate). */
	private static final long TTL_MS = 5L * 60L * 1000L;
	/** Batas tunggu pemanggil non-builder saat cold-start (ms) sebelum kembalikan apa adanya. */
	private static final long TUNGGU_MAKS_MS = 45L * 1000L;

	// ── Progres build (untuk progress bar di UI saat "Ringkasan" / "Hitung Ulang") ──
	/** true selama build berjalan. */
	public static volatile boolean progressAktif = false;
	/** deskripsi fase yang sedang diproses. */
	public static volatile String progressFase = "";
	/** total item yang akan diproses (mahasiswa = fase terberat). */
	public static volatile int progressTotal = 0;
	/** jumlah item yang sudah diproses. */
	public static volatile int progressProses = 0;

	/** Persentase progres (0..100). */
	public static int progressPersen() {
		int t = progressTotal;
		int p = progressProses;
		if (t <= 0) {
			return progressAktif ? 3 : 100;
		}
		int pct = (int) (p * 100L / t);
		return pct < 0 ? 0 : (pct > 100 ? 100 : pct);
	}

	private static String key(String ta, String sem) {
		return (ta == null ? "" : ta.trim()) + "|" + (sem == null ? "" : sem.trim());
	}

	/** Ambil snapshot (build lazy bila belum ada). Tidak query DB bila sudah ada. */
	public static Snapshot get(String ta, String sem) {
		String k = key(ta, sem);
		Snapshot s = CACHE.get(k);
		if (s != null && s.siap) {
			// stale-while-revalidate: kembalikan CEPAT; bila kedaluwarsa, rebuild di latar.
			// Pembaca TIDAK pernah memblokir di build lock untuk snapshot yang sudah pernah jadi —
			// inilah yang dulu menumpuk puluhan thread BLOCKED di belakang warmup yang berjam-jam.
			if ((System.currentTimeMillis() - s.dibangunPada) > TTL_MS) {
				ensureBuiltAsync(ta, sem);
			}
			return s;
		}
		if (sedangBerhenti()) {
			return s != null ? s : new Snapshot(ta, sem);
		}
		// Belum ada snapshot siap (cold start). Hanya SATU thread yang membangun; pemanggil lain
		// menunggu TERBATAS lalu memakai apa adanya — jangan biarkan banyak thread macet berjam-jam.
		if (BUILDING.putIfAbsent(k, Boolean.TRUE) == null) {
			try {
				return build(ta, sem);
			} finally {
				BUILDING.remove(k);
			}
		}
		long deadline = System.currentTimeMillis() + TUNGGU_MAKS_MS;
		while (System.currentTimeMillis() < deadline) {
			Snapshot r = CACHE.get(k);
			if (r != null && r.siap) {
				return r;
			}
			try {
				Thread.sleep(200L);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		Snapshot r = CACHE.get(k);
		return r != null ? r : new Snapshot(ta, sem);
	}

	/** true bila snapshot untuk (TA|sem) sudah jadi — dipakai UI memantau tanpa memblokir. */
	public static boolean isReady(String ta, String sem) {
		Snapshot s = CACHE.get(key(ta, sem));
		return s != null && s.siap;
	}

	/**
	 * Pastikan ada SATU build latar berjalan untuk (TA|sem) — dikoalesai (idempoten): no-op bila
	 * snapshot masih segar atau sudah ada builder lain. Pengganti aman dari "spawn thread per render".
	 */
	public static void ensureBuiltAsync(final String ta, final String sem) {
		if (sedangBerhenti()) {
			return;
		}
		final String k = key(ta, sem);
		Snapshot s = CACHE.get(k);
		if (s != null && s.siap && (System.currentTimeMillis() - s.dibangunPada) <= TTL_MS) {
			return;
		}
		if (BUILDING.putIfAbsent(k, Boolean.TRUE) != null) {
			return;
		}
		if (sedangBerhenti()) {
			BUILDING.remove(k);
			return;
		}
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (!sedangBerhenti() && !Thread.currentThread().isInterrupted()) {
						build(ta, sem);
					}
				} catch (Throwable ig) {
					if (!sedangBerhenti()) {
						ais.common.ErrorAuditUtil.record(ig, "RingkasanKampusCache.ensureBuiltAsync");
					}
				} finally {
					BUILDING.remove(k);
					ASYNC_THREADS.remove(Thread.currentThread());
				}
			}
		}, "ringkasan-ensure");
		t.setDaemon(true);
		ASYNC_THREADS.put(t, Boolean.TRUE);
		if (sedangBerhenti()) {
			ASYNC_THREADS.remove(t);
			BUILDING.remove(k);
			return;
		}
		t.start();
	}

	/** Bangun ulang snapshot (tombol Refresh) — query ulang ke DB. */
	public static Snapshot refresh(String ta, String sem) {
		return build(ta, sem);
	}

	/** Hapus seluruh cache (mis. setelah Singkronkan status mahasiswa). */
	public static void invalidasiSemua() {
		CACHE.clear();
	}

	/** Hapus snapshot untuk satu (TA|semester) — get()/renderKartu berikutnya akan build ulang (progress). */
	public static void invalidasi(String ta, String sem) {
		CACHE.remove(key(ta, sem));
	}

	private static Snapshot build(String ta, String sem) {
		if (sedangBerhenti()) {
			Snapshot existing = CACHE.get(key(ta, sem));
			return existing != null ? existing : new Snapshot(ta, sem);
		}
		synchronized (BUILD_LOCK) {
			if (sedangBerhenti()) {
				Snapshot existing = CACHE.get(key(ta, sem));
				return existing != null ? existing : new Snapshot(ta, sem);
			}
			Snapshot existing = CACHE.get(key(ta, sem));
			if (existing != null && existing.siap && (System.currentTimeMillis() - existing.dibangunPada) < 2000) {
				return existing;
			}
			Snapshot s = new Snapshot(ta, sem);
			progressAktif = true;
			progressFase = "Menyiapkan data…";
			progressTotal = 0;
			progressProses = 0;
			// PARALEL: tiap sub-build (mahasiswa/dosen/calon/siswa/guru) pakai sesi sendiri, jalan
			// SERENTAK -> total lebih cepat (yang lambat = mahasiswa, jalan bareng yang lain).
			ExecutorService pool = Executors.newFixedThreadPool(6, daemonFactory("ringkasan-build"));
			ACTIVE_POOLS.put(pool, Boolean.TRUE);
			try {
				List<Future<?>> fs = new ArrayList<Future<?>>();
				fs.add(pool.submit(subBuild(s, "mahasiswa")));
				fs.add(pool.submit(subBuild(s, "dosen")));
				fs.add(pool.submit(subBuild(s, "calon")));
				fs.add(pool.submit(subBuild(s, "siswa")));
				fs.add(pool.submit(subBuild(s, "guru")));
				fs.add(pool.submit(subBuild(s, "wisuda")));
				for (Future<?> f : fs) {
					try {
						f.get();
					} catch (InterruptedException berhenti) {
						Thread.currentThread().interrupt();
						break;
					} catch (Exception ig) {
						if (!sedangBerhenti()) {
							ais.common.ErrorAuditUtil.record(ig, "RingkasanKampusCache.build");
						}
					}
				}
			} catch (Throwable t) {
				if (!sedangBerhenti()) {
					try {
						Common.tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
					} catch (Throwable ig) {
						ais.common.ErrorAuditUtil.record(ig, "RingkasanKampusCache.build.tampilError");
					}
				}
			} finally {
				ACTIVE_POOLS.remove(pool);
				if (sedangBerhenti() || Thread.currentThread().isInterrupted()) {
					pool.shutdownNow();
				} else {
					pool.shutdown();
				}
				progressFase = "Selesai";
				progressAktif = false;
			}
			if (sedangBerhenti() || Thread.currentThread().isInterrupted()) {
				return existing != null ? existing : s;
			}
			s.dibangunPada = System.currentTimeMillis();
			s.siap = true;
			CACHE.put(key(ta, sem), s);
			return s;
		}
	}

	/**
	 * Jumlah mahasiswa per prodi/status/kelamin — PERSIS logika LaporanRekapJumlahMahasiswa:
	 * iterasi mhs (aktif null/true) lalu getSemester + currentStatus(mhs, TA, smt).
	 */
	@SuppressWarnings("unchecked")
	private static void buildMahasiswa(Session session, Snapshot s) {
		List<Long> ids = session.createCriteria(Mahasiswa.class).setProjection(Projections.property("id"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		// Fase terberat -> dipakai sebagai dasar progress bar.
		progressTotal = ids == null ? 0 : ids.size();
		progressProses = 0;
		progressFase = "Menghitung status mahasiswa (" + progressTotal + ")…";
		// OPTIMASI: muat Mahasiswa per BATCH (1 query / 200 baris) — bukan session.get per-id
		// (ribuan query). Akurasi sama (entity & currentStatus identik); memori dijaga clear() per batch.
		final int batchSize = 200;
		for (int i = 0; i < ids.size(); i += batchSize) {
			if (sedangBerhenti() || Thread.currentThread().isInterrupted()) {
				return;
			}
			List<Long> chunk = new ArrayList<Long>(ids.subList(i, Math.min(i + batchSize, ids.size())));
			List<Mahasiswa> batch;
			Session batchSession = null;
			try {
				// Satu session panjang pernah mengalami ConcurrentModificationException
				// pada persistenceContext.clear() karena currentStatus memuat relasi tambahan.
				// Isolasi setiap batch supaya L1 cache kecil dan hanya dimiliki worker ini.
				batchSession = HibernateUtil.openSession();
				batch = batchSession.createCriteria(Mahasiswa.class).add(Restrictions.in("id", chunk)).list();
			} catch (Throwable t) {
				progressProses += chunk.size();
				closeBatchSession(batchSession);
				continue;
			}
			for (Mahasiswa m : batch) {
				try {
					if (m == null || m.getJurusan() == null || m.getJurusan().getFakultas() == null) {
						continue;
					}
					String kelamin = m.getKelamin();
					if (kelamin == null) {
						continue;
					}
					boolean lk = kelamin.equalsIgnoreCase("Laki-laki");
					boolean pr = kelamin.equalsIgnoreCase("Perempuan");
					if (!lk && !pr) {
						continue;
					}
					Integer smt = Common.getSemester(m.getTahunangkatan(), s.tahunAkademik, s.semester,
							m.getPindahKeKampusIniMasukSemester(), m.getSemesterMulai());
					HistoryStatusMahasiswa hs = Common.currentStatus(m, s.tahunAkademik, smt);
					if (hs == null || hs.getStatusMahasiswa() == null || hs.getStatusMahasiswa().getId() == null) {
						continue;
					}
					Long statusId = hs.getStatusMahasiswa().getId();
					String prodiKey = m.getJurusan().getFakultas().getId() + "_" + m.getJurusan().getId();
					incCount(s.mahasiswa, prodiKey, statusId, lk);
					if (m.getTahunangkatan() != null) {
						incCount(s.perAngkatan, m.getTahunangkatan(), statusId, lk);
					}
					String program = m.getProgram();
					if (program != null && program.trim().length() > 0) {
						incCount(s.perProgram, program.trim(), statusId, lk);
					}
				} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/RingkasanKampusCache.java:306");
					// lewati satu mahasiswa bila error (data rusak); jangan gagalkan seluruh build.
				}
			}
			// Lepas batch dari L1 agar memori tetap rendah (puluhan ribu mahasiswa).
			closeBatchSession(batchSession);
			progressProses += chunk.size();
		}
	}

	private static void closeBatchSession(Session session) {
		HibernateUtil.closeSessionQuietly(session);
	}

	/** Dosen yang MENGAJAR (punya jadwal perkuliahan) per jurusan pada TA. */
	@SuppressWarnings("unchecked")
	private static void buildDosen(Session session, Snapshot s) {
		try {
			List<Object[]> rows = session
					.createSQLQuery("SELECT dz.jurusan AS jid, COUNT(DISTINCT dz.dosen) AS jml FROM ("
							+ " SELECT p.jurusan AS jurusan, unnest(ARRAY[p.dosen1, p.dosen2, p.dosen3]) AS dosen"
							+ " FROM perkuliahan p WHERE p.tahun_ajaran = :ta) dz"
							+ " WHERE dz.dosen IS NOT NULL AND dz.jurusan IS NOT NULL GROUP BY dz.jurusan")
					.setParameter("ta", s.tahunAkademik).list();
			for (Object[] r : rows) {
				if (r == null || r[0] == null) {
					continue;
				}
				s.dosenPerJurusan.put(((Number) r[0]).longValue(), r[1] == null ? 0 : ((Number) r[1]).intValue());
			}
			// TOTAL dosen DISTINCT se-universitas: TANPA GROUP BY jurusan, sehingga dosen
			// yang mengajar di banyak prodi hanya dihitung SEKALI. Inilah angka yang benar
			// untuk kartu "Dosen Aktif" + rasio universitas (penjumlahan per-jurusan dobel).
			Object totUniv = session
					.createSQLQuery("SELECT COUNT(DISTINCT dz.dosen) FROM ("
							+ " SELECT unnest(ARRAY[p.dosen1, p.dosen2, p.dosen3]) AS dosen"
							+ " FROM perkuliahan p WHERE p.tahun_ajaran = :ta) dz"
							+ " WHERE dz.dosen IS NOT NULL")
					.setParameter("ta", s.tahunAkademik).uniqueResult();
			s.totalDosenUniversitas = totUniv == null ? 0 : ((Number) totUniv).intValue();
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/RingkasanKampusCache.java:345");
			// gagal-diam: skema perkuliahan bisa beda; dosen tetap 0.
		}
	}

	/** Calon mahasiswa (pendaftar) per jurusan pada TA. */
	@SuppressWarnings("unchecked")
	private static void buildCalon(Session session, Snapshot s) {
		try {
			// Sama persis dgn subquery calon dashboard lama: per prodi_lulus, filter aktif + TA.
			List<Object[]> rows = session
					.createSQLQuery("SELECT b.prodi_lulus AS jid, COUNT(*) AS jml FROM biodata_calon_mahasiswa b"
							+ " WHERE (b.aktif = true OR b.aktif IS NULL) AND b.tahunakademik = :ta"
							+ " AND b.prodi_lulus IS NOT NULL GROUP BY b.prodi_lulus")
					.setParameter("ta", s.tahunAkademik).list();
			for (Object[] r : rows) {
				if (r == null || r[0] == null) {
					continue;
				}
				s.calonPerJurusan.put(((Number) r[0]).longValue(), r[1] == null ? 0 : ((Number) r[1]).intValue());
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/RingkasanKampusCache.java:366");
			// gagal-diam.
		}
	}

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

	/** Sub-build dengan SESI SENDIRI (aman dijalankan paralel). Gagal-diam per jenis. */
	private static Runnable subBuild(final Snapshot s, final String jenis) {
		return new Runnable() {
			@Override
			public void run() {
				if (sedangBerhenti() || Thread.currentThread().isInterrupted()) {
					return;
				}
				Session se = null;
				try {
					se = HibernateUtil.openSession();
					if ("mahasiswa".equals(jenis)) {
						buildMahasiswa(se, s);
					} else if ("dosen".equals(jenis)) {
						buildDosen(se, s);
					} else if ("calon".equals(jenis)) {
						buildCalon(se, s);
					} else if ("siswa".equals(jenis)) {
						buildSiswa(se, s);
					} else if ("guru".equals(jenis)) {
						buildGuru(se, s);
					} else if ("wisuda".equals(jenis)) {
						buildWisuda(se, s);
					}
				} catch (Throwable t) {
					if (!sedangBerhenti()) {
						ais.common.ErrorAuditUtil.record(t, "RingkasanKampusCache.subBuild." + jenis);
					}
					// gagal-diam: satu jenis gagal tak menjatuhkan yang lain.
				} finally {
					HibernateUtil.closeSessionQuietly(se);
				}
			}
		};
	}

	/** Jumlah SISWA per sekolah/status/kelamin (profil sekolah). statusSiswa = field (bukan history). */
	@SuppressWarnings("unchecked")
	private static void buildSiswa(Session session, Snapshot s) {
		try {
			List<Object[]> rows = session.createQuery(
					"SELECT s.sekolah.id, s.statusSiswa, s.jenisKelamin, COUNT(*) FROM ais.database.model.sekolah.Siswa s"
							+ " WHERE (s.aktif IS NULL OR s.aktif = true) AND s.sekolah IS NOT NULL"
							+ " GROUP BY s.sekolah.id, s.statusSiswa, s.jenisKelamin").list();
			for (Object[] r : rows) {
				if (r == null || r[0] == null) {
					continue;
				}
				Long sekId = ((Number) r[0]).longValue();
				String status = r[1] == null ? "" : r[1].toString();
				String kelamin = r[2] == null ? "" : r[2].toString();
				int cnt = r[3] == null ? 0 : ((Number) r[3]).intValue();
				boolean lk = kelamin.length() > 0 && (kelamin.charAt(0) == 'L' || kelamin.charAt(0) == 'l');
				Map<String, int[]> byStatus = s.siswaPerSekolah.get(sekId);
				if (byStatus == null) {
					byStatus = new HashMap<String, int[]>();
					s.siswaPerSekolah.put(sekId, byStatus);
				}
				int[] g = byStatus.get(status);
				if (g == null) {
					g = new int[2];
					byStatus.put(status, g);
				}
				if (lk) {
					g[0] += cnt;
				} else {
					g[1] += cnt;
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/RingkasanKampusCache.java:446");
			// gagal-diam.
		}
	}

	/** Jumlah GURU aktif per sekolah. */
	@SuppressWarnings("unchecked")
	private static void buildGuru(Session session, Snapshot s) {
		try {
			List<Object[]> rows = session.createQuery(
					"SELECT g.sekolah.id, COUNT(*) FROM ais.database.model.sekolah.Guru g"
							+ " WHERE (g.aktif IS NULL OR g.aktif = true) AND g.sekolah IS NOT NULL GROUP BY g.sekolah.id")
					.list();
			for (Object[] r : rows) {
				if (r == null || r[0] == null) {
					continue;
				}
				s.guruPerSekolah.put(((Number) r[0]).longValue(), r[1] == null ? 0 : ((Number) r[1]).intValue());
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/RingkasanKampusCache.java:465");
			// gagal-diam.
		}
	}

	/**
	 * Jumlah PENDAFTAR WISUDA per jurusan (dari {@code PendaftaranWisuda}): total yang mendaftar
	 * dan subset yang sudah di-ACC ({@code persetujuanWisuda = true}). COUNT(*) agregat (angka).
	 */
	@SuppressWarnings("unchecked")
	private static void buildWisuda(Session session, Snapshot s) {
		try {
			List<Object[]> rows = session.createQuery(
					"SELECT p.mahasiswa.jurusan.id, COUNT(*) FROM ais.database.model.PendaftaranWisuda p"
							+ " WHERE p.mahasiswa IS NOT NULL AND p.mahasiswa.jurusan IS NOT NULL"
							+ " GROUP BY p.mahasiswa.jurusan.id").list();
			for (Object[] r : rows) {
				if (r == null || r[0] == null) {
					continue;
				}
				s.wisudaPendaftarPerJurusan.put(((Number) r[0]).longValue(),
						r[1] == null ? 0 : ((Number) r[1]).intValue());
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/RingkasanKampusCache.java:488");
			// gagal-diam.
		}
		try {
			List<Object[]> rows = session.createQuery(
					"SELECT p.mahasiswa.jurusan.id, COUNT(*) FROM ais.database.model.PendaftaranWisuda p"
							+ " WHERE p.persetujuanWisuda = true AND p.mahasiswa IS NOT NULL AND p.mahasiswa.jurusan IS NOT NULL"
							+ " GROUP BY p.mahasiswa.jurusan.id").list();
			for (Object[] r : rows) {
				if (r == null || r[0] == null) {
					continue;
				}
				s.wisudaAccPerJurusan.put(((Number) r[0]).longValue(), r[1] == null ? 0 : ((Number) r[1]).intValue());
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/RingkasanKampusCache.java:502");
			// gagal-diam.
		}
	}

	// ════════════════════════════════════════════════════════════════════════
	// AGREGASI (display tinggal panggil — tanpa query DB)
	// ════════════════════════════════════════════════════════════════════════

	private static boolean cocokProdi(String prodiKey, Long fakultasId, Long jurusanId) {
		int us = prodiKey.indexOf('_');
		if (us <= 0) {
			return false;
		}
		try {
			long fId = Long.parseLong(prodiKey.substring(0, us));
			long jId = Long.parseLong(prodiKey.substring(us + 1));
			if (fakultasId != null && fId != fakultasId.longValue()) {
				return false;
			}
			if (jurusanId != null && jId != jurusanId.longValue()) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** Jumlah mahasiswa untuk status tertentu, di-filter fakultas/jurusan (null = semua). */
	public static int countMahasiswa(String ta, String sem, Long statusId, Long fakultasId, Long jurusanId) {
		if (statusId == null) {
			return 0;
		}
		Snapshot s = get(ta, sem);
		int total = 0;
		for (Map.Entry<String, Map<Long, int[]>> e : s.mahasiswa.entrySet()) {
			if (!cocokProdi(e.getKey(), fakultasId, jurusanId)) {
				continue;
			}
			int[] g = e.getValue().get(statusId);
			if (g != null) {
				total += g[0] + g[1];
			}
		}
		return total;
	}

	/** Jumlah mahasiswa satu prodi (jurusanId) untuk status tertentu. */
	public static int countMahasiswaProdi(String ta, String sem, Long statusId, Long jurusanId) {
		return countMahasiswa(ta, sem, statusId, null, jurusanId);
	}

	/** Daftar jurusanId yang punya data mahasiswa, terfilter fakultas. */
	public static List<Long> jurusanIds(String ta, String sem, Long fakultasId) {
		Snapshot s = get(ta, sem);
		List<Long> out = new ArrayList<Long>();
		for (String k : s.mahasiswa.keySet()) {
			if (cocokProdi(k, fakultasId, null)) {
				try {
					out.add(Long.parseLong(k.substring(k.indexOf('_') + 1)));
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/RingkasanKampusCache.java:563");
				}
			}
		}
		return out;
	}

	/** Jumlah dosen mengajar (null jurusan = semua jurusan di cache). */
	public static int countDosen(String ta, String sem, Long jurusanId) {
		Snapshot s = get(ta, sem);
		if (jurusanId != null) {
			Integer v = s.dosenPerJurusan.get(jurusanId);
			return v == null ? 0 : v.intValue();
		}
		// jurusanId == null = total UNIVERSITAS → pakai hitungan DISTINCT (anti dobel
		// lintas prodi). Hanya jatuh ke penjumlahan per-jurusan bila snapshot lama belum
		// memuat angka distinct (mis. cache dibangun sebelum perbaikan ini).
		if (s.totalDosenUniversitas > 0) {
			return s.totalDosenUniversitas;
		}
		int total = 0;
		for (Integer v : s.dosenPerJurusan.values()) {
			total += (v == null ? 0 : v.intValue());
		}
		return total;
	}

	/**
	 * Jumlah dosen DISTINCT se-UNIVERSITAS yang mengajar (punya jadwal perkuliahan) pada TA.
	 *
	 * <p>Seorang dosen yang mengajar di lebih dari satu prodi dihitung <b>SEKALI</b>; dosen
	 * yang tidak memiliki jadwal/SKS pada TA berjalan <b>tidak</b> dihitung. Inilah angka yang
	 * benar untuk kartu "Dosen Aktif" dan rasio Mhs/Dosen tingkat universitas — berbeda dari
	 * {@link #countDosen(String, String, Long)} per-prodi yang bila dijumlah akan dobel.</p>
	 *
	 * @param ta  tahun akademik terpilih
	 * @param sem semester terpilih (saat ini hitungan dosen mengikuti TA)
	 * @return jumlah dosen distinct se-universitas
	 */
	public static int countDosenUniversitas(String ta, String sem) {
		return countDosen(ta, sem, null);
	}

	/** Jumlah calon mahasiswa, terfilter jurusan (null = semua). */
	public static int countCalon(String ta, String sem, Long jurusanId) {
		Snapshot s = get(ta, sem);
		if (jurusanId != null) {
			Integer v = s.calonPerJurusan.get(jurusanId);
			return v == null ? 0 : v.intValue();
		}
		int total = 0;
		for (Integer v : s.calonPerJurusan.values()) {
			total += (v == null ? 0 : v.intValue());
		}
		return total;
	}

	/** Increment count generik (dipakai build: per prodi/angkatan/program). */
	private static <K> void incCount(Map<K, Map<Long, int[]>> map, K keyVal, Long statusId, boolean lk) {
		Map<Long, int[]> byStatus = map.get(keyVal);
		if (byStatus == null) {
			byStatus = new HashMap<Long, int[]>();
			map.put(keyVal, byStatus);
		}
		int[] g = byStatus.get(statusId);
		if (g == null) {
			g = new int[2];
			byStatus.put(statusId, g);
		}
		if (lk) {
			g[0]++;
		} else {
			g[1]++;
		}
	}

	private static int sumStatus(Map<Long, int[]> byStatus, Long statusId) {
		if (byStatus == null || statusId == null) {
			return 0;
		}
		int[] g = byStatus.get(statusId);
		return g == null ? 0 : g[0] + g[1];
	}

	/** HITUNG ULANG TERPUSAT — query ulang DB + bangun ulang snapshot. Dipakai semua rekap. */
	public static Snapshot hitungUlang(String ta, String sem) {
		return refresh(ta, sem);
	}

	/** Jumlah mahasiswa satu angkatan untuk status tertentu (untuk tren per angkatan). */
	public static int countAngkatan(String ta, String sem, Long statusId, Integer angkatan) {
		return sumStatus(get(ta, sem).perAngkatan.get(angkatan), statusId);
	}

	/** Daftar angkatan (urut menaik) yang punya data. */
	public static List<Integer> angkatanList(String ta, String sem) {
		List<Integer> out = new ArrayList<Integer>(get(ta, sem).perAngkatan.keySet());
		java.util.Collections.sort(out);
		return out;
	}

	/** Jumlah mahasiswa satu program untuk status tertentu. */
	public static int countProgram(String ta, String sem, Long statusId, String program) {
		return sumStatus(get(ta, sem).perProgram.get(program), statusId);
	}

	/** Daftar program (urut) yang punya data. */
	public static List<String> programList(String ta, String sem) {
		List<String> out = new ArrayList<String>(get(ta, sem).perProgram.keySet());
		java.util.Collections.sort(out);
		return out;
	}

	/** Jumlah siswa (profil sekolah); statusSiswa null = semua status, sekolahId null = semua sekolah. */
	public static int countSiswa(String ta, String sem, String statusSiswa, Long sekolahId) {
		Snapshot s = get(ta, sem);
		int total = 0;
		for (Map.Entry<Long, Map<String, int[]>> e : s.siswaPerSekolah.entrySet()) {
			if (sekolahId != null && !sekolahId.equals(e.getKey())) {
				continue;
			}
			if (statusSiswa == null) {
				for (int[] g : e.getValue().values()) {
					total += g[0] + g[1];
				}
			} else {
				int[] g = e.getValue().get(statusSiswa);
				if (g != null) {
					total += g[0] + g[1];
				}
			}
		}
		return total;
	}

	/** Jumlah guru aktif (sekolahId null = semua sekolah). */
	public static int countGuru(String ta, String sem, Long sekolahId) {
		Snapshot s = get(ta, sem);
		if (sekolahId != null) {
			Integer v = s.guruPerSekolah.get(sekolahId);
			return v == null ? 0 : v.intValue();
		}
		int total = 0;
		for (Integer v : s.guruPerSekolah.values()) {
			total += (v == null ? 0 : v.intValue());
		}
		return total;
	}

	/** Jumlah PENDAFTAR wisuda (semua yang mendaftar); jurusanId null = semua jurusan. */
	public static int countWisudaPendaftar(String ta, String sem, Long jurusanId) {
		Snapshot s = get(ta, sem);
		if (jurusanId != null) {
			Integer v = s.wisudaPendaftarPerJurusan.get(jurusanId);
			return v == null ? 0 : v.intValue();
		}
		int total = 0;
		for (Integer v : s.wisudaPendaftarPerJurusan.values()) {
			total += (v == null ? 0 : v.intValue());
		}
		return total;
	}

	/** Jumlah pendaftar wisuda yang sudah di-ACC (persetujuanWisuda=true); jurusanId null = semua. */
	public static int countWisudaAcc(String ta, String sem, Long jurusanId) {
		Snapshot s = get(ta, sem);
		if (jurusanId != null) {
			Integer v = s.wisudaAccPerJurusan.get(jurusanId);
			return v == null ? 0 : v.intValue();
		}
		int total = 0;
		for (Integer v : s.wisudaAccPerJurusan.values()) {
			total += (v == null ? 0 : v.intValue());
		}
		return total;
	}

	/** Warm-up cache TA/semester berjalan saat aplikasi start (di latar, tak memblokir). */
	public static synchronized void warmupStartup() {
		if (AppStartupListener.isContextStopping()) {
			return;
		}
		lifecycleDihentikan = false;
		Thread lama = warmupThread;
		if (lama != null && lama.isAlive()) {
			lama.interrupt();
		}
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// Beri jeda agar SessionFactory & ConstantValues siap lebih dulu.
					Thread.sleep(90000);
					if (sedangBerhenti() || Thread.currentThread().isInterrupted()) {
						return;
					}
					String ta = Common.getCurrentTahunAkademik();
					String sem = Common.getSemesterString();
					// Lewat builder terkoalesai (set penanda BUILDING) supaya pemanggil get() saat
					// cold-start tidak ikut memicu build kedua di belakang lock.
					ensureBuiltAsync(ta, sem);
				} catch (InterruptedException berhenti) {
					Thread.currentThread().interrupt();
				} catch (Throwable ig) {
					if (!sedangBerhenti()) {
						ais.common.ErrorAuditUtil.record(ig, "RingkasanKampusCache.warmupStartup");
					}
				} finally {
					if (warmupThread == Thread.currentThread()) {
						warmupThread = null;
					}
				}
			}
		}, "RingkasanKampusCache-Warmup");
		t.setDaemon(true);
		warmupThread = t;
		t.start();
	}

	/**
	 * Hentikan warm-up dan seluruh builder cache sebelum Hibernate serta classloader ditutup.
	 * Idempoten dan tidak membuka sesi/database baru sehingga aman dipanggil paling awal saat shutdown.
	 */
	public static synchronized void hentikanUntukShutdown() {
		lifecycleDihentikan = true;
		Thread warmup = warmupThread;
		warmupThread = null;
		if (warmup != null && warmup != Thread.currentThread()) {
			warmup.interrupt();
		}
		for (ExecutorService pool : ACTIVE_POOLS.keySet()) {
			if (pool != null) {
				pool.shutdownNow();
			}
		}
		for (Thread thread : ASYNC_THREADS.keySet()) {
			if (thread != null && thread != Thread.currentThread()) {
				thread.interrupt();
			}
		}
		long batas = System.currentTimeMillis() + 5000L;
		tungguThread(warmup, batas);
		for (Thread thread : ASYNC_THREADS.keySet()) {
			tungguThread(thread, batas);
		}
		ACTIVE_POOLS.clear();
		ASYNC_THREADS.clear();
		BUILDING.clear();
		progressAktif = false;
		progressFase = "Dihentikan";
	}

	private static void tungguThread(Thread thread, long batas) {
		if (thread == null || thread == Thread.currentThread()) {
			return;
		}
		long sisa = batas - System.currentTimeMillis();
		if (sisa <= 0L) {
			return;
		}
		try {
			thread.join(sisa);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static boolean sedangBerhenti() {
		return lifecycleDihentikan || AppStartupListener.isContextStopping();
	}
}
