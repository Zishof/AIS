package ais.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;

import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.library.Item;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * <h2>EntityAccessCache — riwayat ID entity yang benar-benar DIAKSES 3 hari terakhir</h2>
 *
 * <p>Melacak pasangan (kelas, id) yang diakses via {@link DataUtil#ambilData(Class, String, boolean)}
 * dan menyimpan daftar id + waktu-akses terakhir ke BERKAS TERPISAH per kelas (bukan meng-ALTER
 * tabel). Dipakai oleh {@link InitDataHelper#doInitData(Class)} sebagai pengganti heuristik tanggal
 * ("mahasiswa 3 tahun terakhir", dsb.) saat boot: bila riwayat akses nyata tersedia, HANYA id yang
 * benar-benar pernah dipakai 3 hari terakhir yang dimuat ke memori — bukan tebakan berbasis
 * tahun/angkatan. Bila belum ada riwayat (boot pertama setelah deploy fitur ini, atau berkas
 * hilang/kosong), pemanggil WAJIB fallback ke heuristik lama agar perilaku tetap aman.</p>
 *
 * <p><b>Berkas:</b> {@code {catalina.base}/cache/{Common.ROOT}/last_id_<SimpleClassName>.dat} — satu
 * berkas per kelas, satu baris per id: {@code <epochMillis>\t<id>}. Ditulis tiap 15 menit di latar
 * (tulis ke berkas sementara lalu rename = atomik), hanya id &le; 3 hari yang disimpan (auto-prune).
 * Pola berkas &amp; path identik dengan {@link FlagAccessCache} agar konsisten satu konvensi.</p>
 *
 * <p><b>Kelas yang dilacak</b> sengaja DIBATASI ({@link #KELAS_DILACAK}) hanya pada entity besar yang
 * preload-nya selama ini memakai heuristik tanggal atau di-skip sepenuhnya (Tbmuser, Dosen,
 * Mahasiswa, BiodataMahasiswa, BiodataCalonMahasiswa, Siswa, CalonSiswa, Perkuliahan, Item). Entity
 * master kecil (&lt;100 baris) tidak dilacak — sudah full-load murah, tak perlu jejak akses.</p>
 *
 * <p><b>Keamanan &amp; batas.</b> Daftar id per kelas dibatasi {@link #MAKS_ID_PER_KELAS} (ambil yang
 * paling BARU diakses) agar klausa {@code WHERE id IN (...)} dan volume preload tetap terbatas
 * walau riwayat akses membengkak. Semua operasi dibungkus try/catch luas ({@code Throwable}) —
 * kegagalan berkas tidak boleh menggagalkan boot atau bisnis. Java 1.6/1.7 (tanpa lambda/Stream).</p>
 */
public final class EntityAccessCache {

	private EntityAccessCache() {
	}

	private static final long TIGA_HARI_MS = 3L * 24 * 60 * 60 * 1000;

	/** Batas jumlah id per kelas yang dipakai untuk preload (ambil yang paling baru diakses). */
	private static final int MAKS_ID_PER_KELAS = 5000;

	private static final String PREFIX_BERKAS = "last_id_";
	private static final String SUFFIX_BERKAS = ".dat";

	/** Kelas (nama lengkap) yang dilacak riwayat aksesnya — selaras dengan cabang khusus InitDataHelper. */
	private static final Set<String> KELAS_DILACAK;
	static {
		HashSet<String> s = new HashSet<String>();
		s.add(Tbmuser.class.getName());
		s.add(Dosen.class.getName());
		s.add(Mahasiswa.class.getName());
		s.add(BiodataMahasiswa.class.getName());
		s.add(BiodataCalonMahasiswa.class.getName());
		s.add(Siswa.class.getName());
		s.add(CalonSiswa.class.getName());
		s.add(Perkuliahan.class.getName());
		s.add(Item.class.getName());
		KELAS_DILACAK = s;
	}

	/** namaKelasLengkap -> (id -> waktu akses terakhir epoch ms). Satu peta per kelas dilacak. */
	private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> AKSES = new ConcurrentHashMap<String, ConcurrentHashMap<String, Long>>();

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

	/**
	 * Penanda "thread ini sedang menjalankan preload/batch massal" (mis. {@code InitDataHelper.doInitData}).
	 * {@link #catat(String, String)} SKIP pencatatan selama flag ini true — mencegah preload menandai
	 * ulang id yang baru saja dimuatnya sendiri sebagai "baru diakses" (self-reinforcing: karena
	 * aplikasi restart otomatis tiap hari, riwayat 3-hari tidak akan pernah menyusut bila load preload
	 * ikut dihitung sebagai akses nyata — akhirnya SEMUA id yang pernah dimuat tampak "baru diakses"
	 * selamanya). ThreadLocal (bukan flag global) karena preload berjalan di thread/executor tersendiri
	 * secara paralel dengan thread request nyata yang TETAP harus tercatat normal.
	 */
	private static final ThreadLocal<Boolean> SEDANG_PRELOAD = new ThreadLocal<Boolean>();

	/** Tandai/lepas thread saat ini sebagai "sedang preload/batch massal". Selalu panggil di finally. */
	public static void tandaiPreload(boolean sedang) {
		if (sedang) {
			SEDANG_PRELOAD.set(Boolean.TRUE);
		} else {
			SEDANG_PRELOAD.remove();
		}
	}

	/** @return true bila thread saat ini sedang dalam preload/batch massal (lihat {@link #tandaiPreload}). */
	public static boolean sedangPreload() {
		Boolean v = SEDANG_PRELOAD.get();
		return v != null && v.booleanValue();
	}

	/**
	 * Catat satu akses (kelas, id). Dipanggil dari titik LOAD HIBERNATE SEBENARNYA
	 * ({@code AuditTimestampInterceptor.onLoad}) — bukan dari cache-hit generik (lihat komentar di
	 * {@code DataUtil.ambilData} kenapa titik itu TIDAK aman dipakai). Sangat ringan: no-op instan bila
	 * kelas di luar {@link #KELAS_DILACAK}, atau bila thread saat ini sedang preload/batch massal
	 * ({@link #sedangPreload()}).
	 *
	 * @param clazzFullName nama lengkap kelas (SUDAH dibersihkan dari sufiks proxy/javassist oleh
	 *                      pemanggil, mis. {@code StringUtils.split(c.getName(),"_")[0]})
	 * @param id            id entity (string numerik)
	 */
	public static void catat(String clazzFullName, String id) {
		if (clazzFullName == null || id == null || id.length() == 0) {
			return;
		}
		if (!KELAS_DILACAK.contains(clazzFullName)) {
			return;
		}
		if (sedangPreload()) {
			return;
		}
		ConcurrentHashMap<String, Long> peta = AKSES.get(clazzFullName);
		if (peta == null) {
			ConcurrentHashMap<String, Long> baru = new ConcurrentHashMap<String, Long>();
			ConcurrentHashMap<String, Long> ada = AKSES.putIfAbsent(clazzFullName, baru);
			peta = (ada != null) ? ada : baru;
		}
		peta.put(id, Long.valueOf(System.currentTimeMillis()));
	}

	/** Lokasi berkas cache utk satu kelas: {catalina.base}/cache/{Common.ROOT}/last_id_<Simple>.dat */
	private static File berkasCache(String simpleClassName) {
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
		} catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/EntityAccessCache.java:181");
		}
		String nama = simpleClassName.replaceAll("[\\\\/:*?\"<>|]", "_");
		return new File(dir, PREFIX_BERKAS + nama + SUFFIX_BERKAS);
	}

	private static String simpleName(String fullName) {
		int dot = fullName.lastIndexOf('.');
		return dot >= 0 ? fullName.substring(dot + 1) : fullName;
	}

	/** Simpan seluruh kelas dilacak yang aksesnya &le; 3 hari ke berkas masing-masing (temp+rename). */
	public static synchronized void simpanKeFile() {
		long batas = System.currentTimeMillis() - TIGA_HARI_MS;
		for (Map.Entry<String, ConcurrentHashMap<String, Long>> entry : AKSES.entrySet()) {
			String clazzFullName = entry.getKey();
			ConcurrentHashMap<String, Long> peta = entry.getValue();
			try {
				File f = berkasCache(simpleName(clazzFullName));
				File tmp = new File(f.getAbsolutePath() + ".tmp");
				BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8"));
				try {
					for (Map.Entry<String, Long> e : peta.entrySet()) {
						Long ts = e.getValue();
						if (ts != null && ts.longValue() >= batas && e.getKey() != null) {
							w.write(ts.longValue() + "\t" + e.getKey());
							w.newLine();
						}
					}
				} finally {
					w.close();
				}
				if (f.exists()) {
					f.delete();
				}
				if (!tmp.renameTo(f)) {
					tmp.delete();
				}
				// Prune memori: buang id yang lebih lama dari 3 hari agar peta tak menumpuk.
				for (Map.Entry<String, Long> e : peta.entrySet()) {
					Long ts = e.getValue();
					if (ts == null || ts.longValue() < batas) {
						peta.remove(e.getKey(), ts);
					}
				}
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/EntityAccessCache.java:226");
				// gagal-diam per kelas: berkas hanya pendukung, jangan ganggu kelas lain/boot.
			}
		}
	}

	/**
	 * Baca berkas riwayat akses satu kelas, kembalikan id (&le; 3 hari) sebagai {@code List<Long>},
	 * dibatasi {@link #MAKS_ID_PER_KELAS} (ambil yang paling BARU diakses bila melebihi batas).
	 * Sekaligus me-restore ke memori agar kontinuitas TTL terjaga lintas restart. Mengembalikan list
	 * KOSONG bila kelas tidak dilacak, berkas tidak ada, atau seluruh isi sudah kedaluwarsa — pemanggil
	 * WAJIB memperlakukan list kosong sebagai "belum ada riwayat" dan fallback ke heuristik lama.
	 *
	 * @param clazz kelas entity
	 * @return daftar id (mungkin kosong, tidak pernah null)
	 */
	@SuppressWarnings("rawtypes")
	public static List<Long> ambilIdTerakhir(Class clazz) {
		List<Long> hasil = new ArrayList<Long>();
		if (clazz == null) {
			return hasil;
		}
		String clazzFullName = StringUtils.split(clazz.getName(), "_")[0];
		if (!KELAS_DILACAK.contains(clazzFullName)) {
			return hasil;
		}
		try {
			File f = berkasCache(simpleName(clazzFullName));
			if (!f.exists() || f.length() <= 0) {
				return hasil;
			}
			long batas = System.currentTimeMillis() - TIGA_HARI_MS;
			ConcurrentHashMap<String, Long> peta = AKSES.get(clazzFullName);
			if (peta == null) {
				ConcurrentHashMap<String, Long> baru = new ConcurrentHashMap<String, Long>();
				ConcurrentHashMap<String, Long> ada = AKSES.putIfAbsent(clazzFullName, baru);
				peta = (ada != null) ? ada : baru;
			}
			final Map<String, Long> tsById = new ConcurrentHashMap<String, Long>();
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
						String idStr = line.substring(tab + 1).trim();
						if (ts >= batas && idStr.length() > 0 && Common.isNumber(idStr)) {
							tsById.put(idStr, Long.valueOf(ts));
							peta.put(idStr, Long.valueOf(ts));
						}
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/EntityAccessCache.java:280");
					}
				}
			} finally {
				r.close();
			}
			List<String> idKeys = new ArrayList<String>(tsById.keySet());
			if (idKeys.size() > MAKS_ID_PER_KELAS) {
				// Ambil yang paling BARU diakses saja (potong ekor terlama) agar preload/IN(...) terbatas.
				Collections.sort(idKeys, new Comparator<String>() {
					@Override
					public int compare(String a, String b) {
						return tsById.get(b).compareTo(tsById.get(a));
					}
				});
				idKeys = idKeys.subList(0, MAKS_ID_PER_KELAS);
			}
			for (String idStr : idKeys) {
				try {
					hasil.add(Long.valueOf(idStr));
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/EntityAccessCache.java:300");
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/EntityAccessCache.java:303");
		}
		return hasil;
	}

	/** Jadwalkan penyimpanan berkala tiap 15 menit di latar (daemon, satu thread utk semua kelas). */
	public static synchronized void mulaiPenyimpananBerkala() {
		try {
			if (penyimpan != null) {
				return;
			}
			ScheduledExecutorService sch = Executors.newScheduledThreadPool(1, daemonFactory("entity-access-saver"));
			sch.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					try {
						simpanKeFile();
					} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/EntityAccessCache.java:320");
					}
				}
			}, 15, 15, TimeUnit.MINUTES);
			penyimpan = sch;
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/EntityAccessCache.java:325");
		}
	}

	/**
	 * Hentikan scheduler penyimpanan berkala. Dipanggil dari
	 * {@code AppStartupListener.contextDestroyed} saat webapp berhenti/di-reload agar thread
	 * {@code entity-access-saver-*} benar-benar berhenti. Sebelum berhenti, sekali lagi simpan ke
	 * berkas agar tidak ada akses yang hilang.
	 */
	public static synchronized void hentikanPenyimpananBerkala() {
		ScheduledExecutorService sch = penyimpan;
		penyimpan = null;
		if (sch != null) {
			try {
				simpanKeFile();
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/EntityAccessCache.java:341");
			}
			try {
				sch.shutdownNow();
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/EntityAccessCache.java:345");
			}
		}
	}
}
