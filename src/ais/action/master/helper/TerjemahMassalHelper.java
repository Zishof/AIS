package ais.action.master.helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;

import ais.common.KamusBahasaInternal;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.LabelBahasa;

/**
 * <h2>Mesin Terjemah Massal — PARALEL menerjemah, AMAN menulis (bebas deadlock)</h2>
 *
 * <p><b>Fase A (paralel, maks 50 thread):</b> menerjemahkan kolom English/Arab dari teks Indonesia via
 * {@link KamusBahasaInternal}. Murni CPU (kamus di memori) — TIDAK menyentuh DB, sehingga tak ada
 * kontensi lock. Hasil dikumpulkan ke antrian thread-safe.</p>
 *
 * <p><b>Fase B (satu koneksi, ter-batch):</b> menulis hasil ke tabel {@code label_bahasa} lewat JDBC
 * batch ({@code PreparedStatement.addBatch/executeBatch}) pada SATU koneksi, commit per 500 baris.
 * Karena penulisan TIDAK konkuren, tidak mungkin terjadi deadlock antar-transaksi — sekaligus jauh
 * lebih cepat daripada UPDATE per-baris paralel. Dilindungi RETRY bila batch gagal (mis. deadlock dari
 * pemicu/trigger DB).</p>
 *
 * <p>Sebelumnya versi 50-writer-paralel memicu {@code deadlock detected} + {@code current transaction is
 * aborted} pada PostgreSQL. Pola ini mencegahnya.</p>
 *
 * <p>Mode: {@code timpa=false} → hanya baris yang belum diterjemah; {@code timpa=true} → menimpa semua.</p>
 */
public class TerjemahMassalHelper {

	private static final String SQL_UPDATE = "update public.label_bahasa set english = ?, arab = ?, mandarin = ? where id = ?";
	private static final int BATCH = 500;
	private static final int MAKS_RETRY = 3;

	private final List<LabelBahasa> daftar;
	private final boolean timpa;
	private final int paralel;
	// Terjemah massal DEFAULT = kamus internal (cepat, hemat CPU). Set konfigurasi ollama_untuk_massal=true
	// untuk memaksa Ollama pada mass-translate (jauh lebih lambat di CPU, tapi kualitas lebih tinggi).
	private final boolean pakaiOllamaMassal;
	// aiOnly = HANYA Ollama (tanpa fallback kamus internal); baris yg gagal AI DIPERTAHANKAN (tak ditimpa).
	private final boolean aiOnly;

	private final AtomicInteger ditulis = new AtomicInteger(0);
	private final AtomicInteger diperbarui = new AtomicInteger(0);
	private final AtomicInteger totalTulis = new AtomicInteger(0);
	private final AtomicBoolean selesai = new AtomicBoolean(false);
	private final AtomicBoolean fasePersiapan = new AtomicBoolean(true);
	private final ConcurrentLinkedQueue<Object[]> antrian = new ConcurrentLinkedQueue<Object[]>();

	// Progres FASE A (terjemah) + status streaming per-thread.
	private final AtomicInteger diterjemah = new AtomicInteger(0);
	private final int totalSumber;
	private final String[] statusThread; // teks yg sedang diproses tiap thread (indeks = slot)
	private final ConcurrentLinkedQueue<String> terkini = new ConcurrentLinkedQueue<String>(); // aliran hasil terbaru
	private static final int MAKS_TERKINI = 18;

	public TerjemahMassalHelper(List<LabelBahasa> daftar, boolean timpa) {
		this(daftar, timpa, false);
	}

	public TerjemahMassalHelper(List<LabelBahasa> daftar, boolean timpa, boolean aiOnly) {
		this.aiOnly = aiOnly;
		this.daftar = daftar == null ? new ArrayList<LabelBahasa>() : daftar;
		this.timpa = timpa;
		int n = this.daftar.size();
		int p = (n + 199) / 200;
		if (p < 1) {
			p = 1;
		}
		if (p > 50) {
			p = 50;
		}
		this.paralel = p;
		this.totalSumber = n;
		this.statusThread = new String[p];
		for (int i = 0; i < p; i++) {
			this.statusThread[i] = "";
		}
		boolean ol = false;
		try {
			String v = ais.common.Common.getKonfigurasi("ollama_untuk_massal", "false").getNilai();
			ol = v != null && ("true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim())
					|| "on".equalsIgnoreCase(v.trim()) || "ya".equalsIgnoreCase(v.trim()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TerjemahMassalHelper.java:97");
		}
		this.pakaiOllamaMassal = ol;
	}

	/**
	 * Mesin terjemah utk mass-translate: aiOnly → HANYA Ollama (bisa null bila gagal); pakaiOllamaMassal →
	 * Ollama+fallback internal; selain itu kamus internal.
	 */
	private String terjemahMesin(String id, String lang) {
		if (aiOnly) {
			return ais.common.AiTerjemah.ollamaSaja(id, lang);
		}
		if (pakaiOllamaMassal) {
			return ais.common.AiTerjemah.terjemah(id, lang);
		}
		return KamusBahasaInternal.terjemah(id, lang);
	}

	public int getDiterjemah() {
		return diterjemah.get();
	}

	public int getTotalSumber() {
		return totalSumber;
	}

	/** Salinan status "sedang diproses" tiap thread (slot 1..paralel). */
	public String[] getStatusThread() {
		String[] c = new String[statusThread.length];
		System.arraycopy(statusThread, 0, c, 0, statusThread.length);
		return c;
	}

	/** Aliran hasil terjemahan TERBARU (dari yang paling baru), untuk ditampilkan streaming. */
	public String getTerkiniGabung() {
		StringBuilder sb = new StringBuilder();
		String[] arr = terkini.toArray(new String[0]);
		for (int i = arr.length - 1; i >= 0; i--) {
			sb.append(arr[i]).append("\n");
		}
		return sb.toString();
	}

	private void catatTerkini(String baris) {
		terkini.offer(baris);
		while (terkini.size() > MAKS_TERKINI) {
			terkini.poll();
		}
	}

	private static String ringkas(String s, int maks) {
		if (s == null) {
			return "";
		}
		s = s.trim().replaceAll("\\s+", " ");
		return s.length() > maks ? s.substring(0, maks - 1) + "…" : s;
	}

	public int getTotal() {
		int t = totalTulis.get();
		return t > 0 ? t : daftar.size();
	}

	public int getDiproses() {
		return ditulis.get();
	}

	public int getDiperbarui() {
		return diperbarui.get();
	}

	public boolean isSelesai() {
		return selesai.get();
	}

	public boolean isFasePersiapan() {
		return fasePersiapan.get();
	}

	public int getParalel() {
		return paralel;
	}

	public int persen() {
		// Fase A (terjemah) menempati 0..80%, Fase B (tulis) 80..100% — bar bergerak sejak awal.
		if (fasePersiapan.get()) {
			if (totalSumber <= 0) {
				return 0;
			}
			int p = (int) (diterjemah.get() * 80L / totalSumber);
			return p > 80 ? 80 : p;
		}
		int t = getTotal();
		if (t <= 0) {
			return 100;
		}
		int p = 80 + (int) (ditulis.get() * 20L / t);
		return p > 100 ? 100 : p;
	}

	/** Mulai proses di thread koordinator (daemon) — TIDAK memblokir pemanggil. */
	public void mulai() {
		if (daftar.isEmpty()) {
			selesai.set(true);
			return;
		}
		Thread koordinator = new Thread(new Runnable() {
			@Override
			public void run() {
				jalankan();
			}
		}, "terjemah-massal");
		koordinator.setDaemon(true);
		koordinator.start();
	}

	private void jalankan() {
		try {
			terjemahParalel();
			totalTulis.set(antrian.size());
			fasePersiapan.set(false);
			tulisBatchAman();
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/TerjemahMassalHelper.java:220");
			// abaikan: tetap tandai selesai agar UI menutup progres
		} finally {
			selesai.set(true);
		}
	}

	/** Fase A: terjemah paralel (maks 50 thread), TANPA DB. Baris disjoint per chunk. */
	private void terjemahParalel() {
		ExecutorService executor = Executors.newFixedThreadPool(paralel);
		try {
			int n = daftar.size();
			int chunk = (n + paralel - 1) / paralel;
			if (chunk < 1) {
				chunk = 1;
			}
			List<Future<?>> futures = new ArrayList<Future<?>>();
			for (int start = 0; start < n; start += chunk) {
				final int dari = start;
				final int sampai = Math.min(n, start + chunk);
				final int slot = start / chunk;
				futures.add(executor.submit(new Runnable() {
					@Override
					public void run() {
						terjemahBagian(dari, sampai, slot);
					}
				}));
			}
			for (int i = 0; i < futures.size(); i++) {
				try {
					futures.get(i).get();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TerjemahMassalHelper.java:251");
				}
			}
		} finally {
			try {
				executor.shutdown();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TerjemahMassalHelper.java:257");
			}
		}
	}

	private void terjemahBagian(int dari, int sampai, int slot) {
		for (int i = dari; i < sampai; i++) {
			LabelBahasa lb = daftar.get(i);
			diterjemah.incrementAndGet();
			if (lb == null || lb.getId() == null || lb.getIndonesia() == null
					|| lb.getIndonesia().trim().length() == 0) {
				continue;
			}
			String id = lb.getIndonesia().trim();
			// tandai apa yg sedang dikerjakan thread ini (utk tampilan per-thread di popup)
			if (slot >= 0 && slot < statusThread.length) {
				statusThread[slot] = ringkas(id, 42);
			}
			String enLama = lb.getEnglish();
			String arLama = lb.getArab();
			String zhLama = lb.getMandarin();
			boolean enPerlu = timpa || enLama == null || enLama.trim().length() == 0
					|| enLama.trim().equalsIgnoreCase(id);
			boolean arPerlu = timpa || arLama == null || arLama.trim().length() == 0
					|| arLama.trim().equalsIgnoreCase(id);
			boolean zhPerlu = timpa || zhLama == null || zhLama.trim().length() == 0
					|| zhLama.trim().equalsIgnoreCase(id);
			if (!enPerlu && !arPerlu && !zhPerlu) {
				continue;
			}
			// Mass-translate. Pada aiOnly, hasil bisa null (Ollama gagal) → PERTAHANKAN nilai lama (jangan timpa).
			String enBaru = enLama;
			if (enPerlu) {
				String v = terjemahMesin(id, "english");
				if (v != null && v.trim().length() > 0) {
					enBaru = v;
				}
			}
			String arBaru = arLama;
			if (arPerlu) {
				String v = terjemahMesin(id, "arab");
				if (v != null && v.trim().length() > 0) {
					arBaru = v;
				}
			}
			String zhBaru = zhLama;
			if (zhPerlu) {
				String v = terjemahMesin(id, "mandarin");
				if (v != null && v.trim().length() > 0) {
					zhBaru = v;
				}
			}
			antrian.offer(new Object[] { lb.getId(), enBaru, arBaru, zhBaru, lb.getNama() });
			catatTerkini(ringkas(id, 28) + "  →  " + ringkas(enBaru, 24));
		}
		if (slot >= 0 && slot < statusThread.length) {
			statusThread[slot] = "(selesai)";
		}
	}

	/** Fase B: tulis semua hasil via SATU koneksi (JDBC batch, commit per 500) + retry aman. */
	private void tulisBatchAman() {
		final List<Object[]> list = new ArrayList<Object[]>(antrian);
		if (list.isEmpty()) {
			return;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			session.doWork(new Work() {
				@Override
				public void execute(Connection conn) throws SQLException {
					boolean autoLama = conn.getAutoCommit();
					try {
						conn.setAutoCommit(false);
						tulisSemua(conn, list);
					} finally {
						try {
							conn.setAutoCommit(autoLama);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TerjemahMassalHelper.java:336");
						}
					}
				}
			});
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TerjemahMassalHelper.java:341");
			// abaikan: sudah ada retry di dalam
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TerjemahMassalHelper.java:347");
				}
			}
		}
	}

	private void tulisSemua(Connection conn, List<Object[]> list) {
		int n = list.size();
		int mulai = 0;
		while (mulai < n) {
			int akhir = Math.min(n, mulai + BATCH);
			boolean ok = tulisSatuBatch(conn, list, mulai, akhir);
			int jml = akhir - mulai;
			ditulis.addAndGet(jml);
			if (ok) {
				diperbarui.addAndGet(jml);
			}
			mulai = akhir;
		}
	}

	/** Tulis satu batch [mulai, akhir) dengan retry bila gagal (mis. deadlock trigger). */
	private boolean tulisSatuBatch(Connection conn, List<Object[]> list, int mulai, int akhir) {
		for (int percobaan = 1; percobaan <= MAKS_RETRY; percobaan++) {
			PreparedStatement ps = null;
			try {
				ps = conn.prepareStatement(SQL_UPDATE);
				for (int i = mulai; i < akhir; i++) {
					Object[] r = list.get(i);
					ps.setString(1, (String) r[1]);
					ps.setString(2, (String) r[2]);
					ps.setString(3, (String) r[3]);
					ps.setLong(4, ((Long) r[0]).longValue());
					ps.addBatch();
				}
				ps.executeBatch();
				conn.commit();
				return true;
			} catch (Exception e) {
				try {
					conn.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/TerjemahMassalHelper.java:388");
				}
				if (percobaan >= MAKS_RETRY) {
					return false;
				}
				try {
					Thread.sleep(50L * percobaan);
				} catch (InterruptedException ie) { ais.common.ErrorAuditUtil.record(ie, "auto-audit(empty-catch) src/ais/action/master/helper/TerjemahMassalHelper.java:395");
				}
			} finally {
				if (ps != null) {
					try {
						ps.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TerjemahMassalHelper.java:401");
					}
				}
			}
		}
		return false;
	}

	/** Terapkan hasil ke cache memori bahasa. WAJIB dipanggil dari thread event (single-thread). */
	public void terapkanKeMemori() {
		Object[] r;
		while ((r = antrian.poll()) != null) {
			try {
				String nama = (String) r[4];
				if (nama != null) {
					MemoryDbUtil.getBahasaEnglishs().put(nama, (String) r[1]);
					MemoryDbUtil.getBahasaArabs().put(nama, (String) r[2]);
					MemoryDbUtil.getBahasaMandarins().put(nama, (String) r[3]);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TerjemahMassalHelper.java:420");
			}
		}
	}
}
