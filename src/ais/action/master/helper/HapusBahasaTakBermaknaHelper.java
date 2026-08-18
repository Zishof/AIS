package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.LabelBahasa;

/**
 * <h2>Hapus label bahasa TIDAK BERMAKNA (berbantuan Ollama) — paralel + progres</h2>
 *
 * <p><b>Fase A (paralel, maks 50 thread):</b> menilai tiap baris — heuristik cepat (email, huruf berulang)
 * ATAU {@link ais.common.AiTerjemah#bermakna(String)} (Ollama). Baris yang TEGAS dinilai JUNK dikumpulkan.
 * Bila Ollama mati/sibuk (nilai {@code null}), baris DIPERTAHANKAN (aman — tidak dihapus).</p>
 *
 * <p><b>Fase B (satu koneksi, ter-batch):</b> menghapus baris junk lewat HQL bulk-delete per 200 id.</p>
 *
 * <p><b>Aman:</b> hanya menghapus bila heuristik junk ATAU Ollama menjawab JUNK secara tegas; teks kosong &amp;
 * kasus ragu tidak dihapus.</p>
 */
public class HapusBahasaTakBermaknaHelper {

	private static final int BATCH = 200;
	private static final int MAKS_TERKINI = 18;

	private final List<LabelBahasa> daftar;
	private final int paralel;
	private final int totalSumber;
	private final String[] statusThread;

	private final AtomicInteger diperiksa = new AtomicInteger(0);
	private final AtomicInteger dihapus = new AtomicInteger(0);
	private final AtomicInteger totalHapus = new AtomicInteger(0);
	private final AtomicBoolean selesai = new AtomicBoolean(false);
	private final AtomicBoolean fasePersiapan = new AtomicBoolean(true);
	private final ConcurrentLinkedQueue<Long> hapusIds = new ConcurrentLinkedQueue<Long>();
	private final ConcurrentLinkedQueue<String> terkini = new ConcurrentLinkedQueue<String>();

	public HapusBahasaTakBermaknaHelper(List<LabelBahasa> daftar) {
		this.daftar = daftar == null ? new ArrayList<LabelBahasa>() : daftar;
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
	}

	public int getParalel() {
		return paralel;
	}

	public int getTotalSumber() {
		return totalSumber;
	}

	public int getDiperiksa() {
		return diperiksa.get();
	}

	public int getAkanDihapus() {
		return totalHapus.get() > 0 ? totalHapus.get() : hapusIds.size();
	}

	public int getDihapus() {
		return dihapus.get();
	}

	public boolean isSelesai() {
		return selesai.get();
	}

	public boolean isFasePersiapan() {
		return fasePersiapan.get();
	}

	public String[] getStatusThread() {
		String[] c = new String[statusThread.length];
		System.arraycopy(statusThread, 0, c, 0, statusThread.length);
		return c;
	}

	public String getTerkiniGabung() {
		StringBuilder sb = new StringBuilder();
		String[] arr = terkini.toArray(new String[0]);
		for (int i = arr.length - 1; i >= 0; i--) {
			sb.append(arr[i]).append("\n");
		}
		return sb.toString();
	}

	public int persen() {
		if (fasePersiapan.get()) {
			if (totalSumber <= 0) {
				return 0;
			}
			int p = (int) (diperiksa.get() * 85L / totalSumber);
			return p > 85 ? 85 : p;
		}
		int t = totalHapus.get();
		if (t <= 0) {
			return 100;
		}
		int p = 85 + (int) (dihapus.get() * 15L / t);
		return p > 100 ? 100 : p;
	}

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
		}, "hapus-tak-bermakna");
		koordinator.setDaemon(true);
		koordinator.start();
	}

	private void jalankan() {
		try {
			nilaiParalel();
			totalHapus.set(hapusIds.size());
			fasePersiapan.set(false);
			hapusSemua();
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/HapusBahasaTakBermaknaHelper.java:145");
		} finally {
			selesai.set(true);
		}
	}

	private void nilaiParalel() {
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
						nilaiBagian(dari, sampai, slot);
					}
				}));
			}
			for (int i = 0; i < futures.size(); i++) {
				try {
					futures.get(i).get();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HapusBahasaTakBermaknaHelper.java:174");
				}
			}
		} finally {
			try {
				executor.shutdown();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HapusBahasaTakBermaknaHelper.java:180");
			}
		}
	}

	private void nilaiBagian(int dari, int sampai, int slot) {
		for (int i = dari; i < sampai; i++) {
			LabelBahasa lb = daftar.get(i);
			diperiksa.incrementAndGet();
			if (lb == null || lb.getId() == null) {
				continue;
			}
			String teks = teksNilai(lb);
			if (slot >= 0 && slot < statusThread.length) {
				statusThread[slot] = ringkas(teks, 42);
			}
			if (teks.length() == 0) {
				continue; // tak bisa dinilai → JANGAN hapus (aman)
			}
			if (junk(lb, teks)) {
				hapusIds.offer(lb.getId());
				catatTerkini("HAPUS: " + ringkas(teks, 48));
			}
		}
		if (slot >= 0 && slot < statusThread.length) {
			statusThread[slot] = "(selesai)";
		}
	}

	private static String teksNilai(LabelBahasa lb) {
		if (lb.getIndonesia() != null && lb.getIndonesia().trim().length() > 0) {
			return lb.getIndonesia().trim();
		}
		if (lb.getNama() != null) {
			return lb.getNama().replace('_', ' ').trim();
		}
		return "";
	}

	/** JUNK bila heuristik pasti-junk, ATAU Ollama menjawab JUNK secara TEGAS (null/true → simpan). */
	private boolean junk(LabelBahasa lb, String teks) {
		if (junkHeuristik(teks)) {
			return true;
		}
		Boolean b = ais.common.AiTerjemah.bermakna(teks);
		return Boolean.FALSE.equals(b);
	}

	private static boolean junkHeuristik(String t) {
		if (t == null) {
			return false;
		}
		// email
		if (t.indexOf('@') > 0 && t.indexOf('.', t.indexOf('@')) > 0) {
			return true;
		}
		// TIDAK ada huruf sama sekali → angka/tanggal/simbol/kode-angka murni (mis. "123", "10.00", "2024-01-01")
		boolean adaHuruf = false;
		for (int i = 0; i < t.length(); i++) {
			if (Character.isLetter(t.charAt(i))) {
				adaHuruf = true;
				break;
			}
		}
		if (!adaHuruf) {
			return true;
		}
		// semua karakter (huruf) sama & panjang >= 3, mis. "aaa", "xxxx"
		if (t.length() >= 3) {
			char c0 = Character.toLowerCase(t.charAt(0));
			boolean same = Character.isLetter(c0);
			for (int i = 1; same && i < t.length(); i++) {
				if (Character.toLowerCase(t.charAt(i)) != c0) {
					same = false;
				}
			}
			if (same) {
				return true;
			}
		}
		return false;
	}

	private void hapusSemua() {
		List<Long> ids = new ArrayList<Long>(hapusIds);
		if (ids.isEmpty()) {
			return;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			int n = ids.size();
			int mulai = 0;
			while (mulai < n) {
				int akhir = Math.min(n, mulai + BATCH);
				List<Long> batch = ids.subList(mulai, akhir);
				org.hibernate.Transaction tx = null;
				try {
					tx = session.beginTransaction();
					session.createQuery("delete from ais.database.model.LabelBahasa where id in (:ids)")
							.setParameterList("ids", batch).executeUpdate();
					tx.commit();
					dihapus.addAndGet(batch.size());
				} catch (Exception e) {
					if (tx != null) {
						try {
							tx.rollback();
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HapusBahasaTakBermaknaHelper.java:287");
						}
					}
				}
				mulai = akhir;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HapusBahasaTakBermaknaHelper.java:293");
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HapusBahasaTakBermaknaHelper.java:298");
				}
			}
		}
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
}
