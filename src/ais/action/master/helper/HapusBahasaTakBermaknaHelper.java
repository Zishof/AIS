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

	/** Ukuran batch HQL bulk-delete pada Fase B, agar satu transaksi tidak mengunci terlalu banyak baris sekaligus. */
	private static final int BATCH = 200;

	/** Jumlah maksimum baris riwayat "terkini" yang disimpan untuk ditampilkan pada progres (ring buffer). */
	private static final int MAKS_TERKINI = 18;

	/** Daftar sumber {@link LabelBahasa} yang akan dinilai; tidak pernah {@code null} (dinormalkan di konstruktor). */
	private final List<LabelBahasa> daftar;

	/** Jumlah thread paralel Fase A, dihitung dari ukuran {@link #daftar} dan dibatasi 1..50. */
	private final int paralel;

	/** Salinan ukuran {@link #daftar} pada saat konstruksi, dipakai untuk perhitungan {@link #persen()}. */
	private final int totalSumber;

	/** Status ringkas per-slot thread Fase A (baris terakhir yang sedang diperiksa), diindeks oleh nomor slot. */
	private final String[] statusThread;

	/** Penghitung atomik jumlah baris yang sudah dinilai pada Fase A, dipakai bersama oleh seluruh thread. */
	private final AtomicInteger diperiksa = new AtomicInteger(0);

	/** Penghitung atomik jumlah baris yang sudah benar-benar dihapus pada Fase B. */
	private final AtomicInteger dihapus = new AtomicInteger(0);

	/** Total id yang akan dihapus, diisi sekali setelah Fase A selesai (snapshot ukuran {@link #hapusIds}). */
	private final AtomicInteger totalHapus = new AtomicInteger(0);

	/** Menjadi {@code true} setelah seluruh proses (Fase A dan Fase B) selesai, termasuk bila gagal di tengah jalan. */
	private final AtomicBoolean selesai = new AtomicBoolean(false);

	/** {@code true} selama Fase A (penilaian) berjalan; berubah {@code false} begitu Fase B (penghapusan) dimulai. */
	private final AtomicBoolean fasePersiapan = new AtomicBoolean(true);

	/** Kumpulan id {@link LabelBahasa} yang dinilai JUNK pada Fase A dan akan dihapus pada Fase B. */
	private final ConcurrentLinkedQueue<Long> hapusIds = new ConcurrentLinkedQueue<Long>();

	/** Ring buffer baris log ringkas terbaru (mis. "HAPUS: ...") untuk ditampilkan sebagai progres berjalan. */
	private final ConcurrentLinkedQueue<String> terkini = new ConcurrentLinkedQueue<String>();

	/**
	 * Menyiapkan proses penghapusan untuk {@code daftar} baris {@link LabelBahasa}. Tidak memulai
	 * proses apa pun — pemanggil harus memanggil {@link #mulai()} secara terpisah. Jumlah thread
	 * paralel dihitung otomatis: kira-kira satu thread per 200 baris, dibatasi minimal 1 dan
	 * maksimal 50 agar tidak membebani server meskipun {@code daftar} sangat besar.
	 *
	 * @param daftar baris {@link LabelBahasa} yang akan dinilai/dihapus; {@code null} diperlakukan
	 *        sebagai daftar kosong (proses langsung dianggap selesai saat {@link #mulai()} dipanggil)
	 */
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

	/** @return jumlah thread paralel yang dipakai pada Fase A (1..50), ditentukan sekali di konstruktor. */
	public int getParalel() {
		return paralel;
	}

	/** @return jumlah total baris {@link LabelBahasa} sumber yang diproses (ukuran daftar awal). */
	public int getTotalSumber() {
		return totalSumber;
	}

	/** @return jumlah baris yang sudah dinilai (diperiksa) pada Fase A sejauh ini, nilai berjalan (live). */
	public int getDiperiksa() {
		return diperiksa.get();
	}

	/**
	 * @return perkiraan/jumlah baris yang akan/sudah ditandai untuk dihapus. Selama Fase A masih
	 *         berjalan (sebelum {@link #totalHapus} diisi), nilai ini adalah ukuran {@link #hapusIds}
	 *         yang terus bertambah; setelah Fase A selesai, nilai ini adalah snapshot final tersebut.
	 */
	public int getAkanDihapus() {
		return totalHapus.get() > 0 ? totalHapus.get() : hapusIds.size();
	}

	/** @return jumlah baris yang sudah benar-benar terhapus dari database pada Fase B, nilai berjalan (live). */
	public int getDihapus() {
		return dihapus.get();
	}

	/** @return {@code true} bila seluruh proses (Fase A dan Fase B) telah selesai, termasuk bila gagal di tengah jalan. */
	public boolean isSelesai() {
		return selesai.get();
	}

	/** @return {@code true} selama Fase A (penilaian paralel) masih berjalan; {@code false} begitu Fase B (hapus) dimulai. */
	public boolean isFasePersiapan() {
		return fasePersiapan.get();
	}

	/**
	 * Mengembalikan salinan pertahanan (defensive copy) dari status ringkas tiap slot thread Fase A,
	 * sehingga pemanggil (mis. tampilan progres) tidak dapat mengubah array internal.
	 *
	 * @return array baru berisi status ringkas per slot thread, seukuran {@link #getParalel()}
	 */
	public String[] getStatusThread() {
		String[] c = new String[statusThread.length];
		System.arraycopy(statusThread, 0, c, 0, statusThread.length);
		return c;
	}

	/**
	 * Menggabungkan riwayat baris log ringkas terbaru ({@link #terkini}) menjadi satu teks
	 * multi-baris, dengan urutan baris terbaru di atas (dibalik dari urutan penyimpanan FIFO).
	 *
	 * @return teks gabungan siap tampil, satu baris log per baris teks, diakhiri newline; string
	 *         kosong bila belum ada baris log
	 */
	public String getTerkiniGabung() {
		StringBuilder sb = new StringBuilder();
		String[] arr = terkini.toArray(new String[0]);
		for (int i = arr.length - 1; i >= 0; i--) {
			sb.append(arr[i]).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Menghitung persentase progres gabungan untuk ditampilkan ke pengguna: Fase A (penilaian)
	 * direpresentasikan sebagai 0-85%, dan Fase B (penghapusan) sebagai 85-100%, sehingga progress
	 * bar tetap bergerak maju secara linear meskipun proses sesungguhnya terdiri dari dua fase
	 * dengan durasi yang sangat berbeda.
	 *
	 * @return nilai persen 0-100
	 */
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

	/**
	 * Memulai proses secara asinkron pada thread daemon terpisah ("hapus-tak-bermakna"), sehingga
	 * pemanggil (mis. handler ZK) tidak diblokir menunggu Fase A dan Fase B selesai. Progres dapat
	 * dipantau lewat getter seperti {@link #getDiperiksa()}, {@link #getDihapus()},
	 * {@link #persen()}, dan {@link #isSelesai()}. Bila {@link #daftar} kosong, proses langsung
	 * ditandai selesai tanpa membuat thread.
	 */
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

	/**
	 * Koordinator yang dijalankan pada thread daemon terpisah: menjalankan Fase A
	 * ({@link #nilaiParalel()}), mengunci jumlah total yang akan dihapus, menandai
	 * transisi ke Fase B (mengubah {@link #fasePersiapan}), lalu menjalankan Fase B
	 * ({@link #hapusSemua()}). Kegagalan tak terduga (termasuk {@link Error}) dicatat
	 * ke {@code ErrorAuditUtil} agar tidak hilang diam-diam; {@link #selesai} tetap
	 * ditandai {@code true} pada blok {@code finally} apa pun yang terjadi, sehingga
	 * pemanggil yang memantau {@link #isSelesai()} tidak menunggu selamanya.
	 */
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

	/**
	 * Fase A: membagi {@link #daftar} menjadi {@link #paralel} bagian berukuran hampir sama,
	 * menilai tiap bagian secara paralel lewat {@link #nilaiBagian(int, int, int)} pada
	 * thread pool tetap, lalu menunggu seluruh bagian selesai sebelum kembali. Kegagalan pada
	 * satu bagian (lemparan dari {@link Future#get()}) dicatat ke audit dan tidak menghentikan
	 * penantian bagian lain; {@code executor} selalu di-{@code shutdown()} pada blok
	 * {@code finally}.
	 */
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

	/**
	 * Menilai satu rentang {@code [dari, sampai)} dari {@link #daftar} pada satu thread worker:
	 * untuk tiap baris, mengekstrak teks yang relevan lewat {@link #teksNilai(LabelBahasa)},
	 * memperbarui status ringkas slot thread ({@link #statusThread}), dan menandai baris untuk
	 * dihapus (menambah {@link #hapusIds} serta mencatat log ringkas) bila
	 * {@link #junk(LabelBahasa, String)} menyatakan JUNK. Baris tanpa id atau dengan teks kosong
	 * dilewati tanpa dihapus (aman — tidak bisa dinilai berarti tidak dihapus).
	 *
	 * @param dari indeks awal (inklusif) rentang pada {@link #daftar}
	 * @param sampai indeks akhir (eksklusif) rentang pada {@link #daftar}
	 * @param slot indeks slot pada {@link #statusThread} untuk worker ini; di luar jangkauan array
	 *        diabaikan tanpa efek (jaga-jaga pembagian slot yang tidak pas)
	 */
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

	/**
	 * Mengekstrak teks yang akan dinilai dari satu baris {@link LabelBahasa}: memprioritaskan
	 * kolom terjemahan {@code indonesia} bila terisi; jika tidak, jatuh ke {@code nama} (kunci
	 * label) dengan garis bawah diganti spasi agar lebih mirip teks alami saat dinilai heuristik
	 * atau Ollama.
	 *
	 * @param lb baris label bahasa sumber, tidak boleh {@code null}
	 * @return teks yang dinilai, sudah di-{@code trim()}; string kosong bila kedua kolom kosong
	 */
	private static String teksNilai(LabelBahasa lb) {
		if (lb.getIndonesia() != null && lb.getIndonesia().trim().length() > 0) {
			return lb.getIndonesia().trim();
		}
		if (lb.getNama() != null) {
			return lb.getNama().replace('_', ' ').trim();
		}
		return "";
	}

	/**
	 * Menentukan apakah {@code teks} dianggap JUNK (tidak bermakna) dan boleh dihapus. Bersifat
	 * fail-safe/aman-secara-default: JUNK hanya bila heuristik cepat ({@link #junkHeuristik(String)})
	 * memastikan JUNK, ATAU {@link ais.common.AiTerjemah#bermakna(String)} (dibantu Ollama) menjawab
	 * secara TEGAS {@link Boolean#FALSE}. Bila Ollama sedang mati/sibuk dan mengembalikan
	 * {@code null} (tidak tahu), atau menjawab {@link Boolean#TRUE} (bermakna), baris DIPERTAHANKAN
	 * — tidak pernah dihapus karena keraguan.
	 *
	 * @param lb baris label bahasa yang dinilai (parameter disediakan untuk pemanggil, tidak dipakai
	 *        langsung di method ini selain untuk konteks pemanggilan)
	 * @param teks teks yang sudah diekstrak lewat {@link #teksNilai(LabelBahasa)}
	 * @return {@code true} bila baris ini boleh ditandai untuk dihapus
	 */
	private boolean junk(LabelBahasa lb, String teks) {
		if (junkHeuristik(teks)) {
			return true;
		}
		Boolean b = ais.common.AiTerjemah.bermakna(teks);
		return Boolean.FALSE.equals(b);
	}

	/**
	 * Heuristik cepat (tanpa memanggil Ollama) untuk mendeteksi teks yang pasti tidak bermakna:
	 * (1) menyerupai alamat email; (2) sama sekali tidak mengandung huruf (murni angka/tanggal/
	 * simbol/kode, mis. {@code "123"}, {@code "10.00"}, {@code "2024-01-01"}); atau (3) seluruh
	 * karakternya adalah huruf yang sama berulang sepanjang minimal 3 karakter (mis.
	 * {@code "aaa"}, {@code "xxxx"}). Heuristik ini sengaja konservatif — hanya pola yang PASTI
	 * junk yang dikembalikan {@code true}; kasus ragu diserahkan ke {@link #junk(LabelBahasa, String)}
	 * untuk ditanyakan ke Ollama.
	 *
	 * @param t teks yang akan diperiksa; {@code null} dianggap bukan junk (aman)
	 * @return {@code true} bila teks cocok salah satu pola pasti-junk di atas
	 */
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

	/**
	 * Fase B: menghapus seluruh baris yang ditandai pada {@link #hapusIds} lewat HQL bulk-delete,
	 * dibagi per batch {@link #BATCH} id agar satu transaksi tidak terlalu besar. Setiap batch
	 * memakai transaksi sendiri; kegagalan pada satu batch di-{@code rollback} dan dicatat ke audit
	 * tanpa menghentikan batch berikutnya (batch yang sudah berhasil tetap tersimpan). Sesi
	 * Hibernate dibuka baru (bukan sesi thread-lokal) karena method ini berjalan di luar thread
	 * request ZK biasa, dan selalu ditutup pada blok {@code finally}.
	 */
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

	/**
	 * Menambahkan satu baris ke ring buffer {@link #terkini}, membuang baris tertua bila ukuran
	 * melebihi {@link #MAKS_TERKINI}, sehingga memori dipakai tetap terbatas meskipun proses
	 * memeriksa jutaan baris.
	 *
	 * @param baris baris log ringkas yang akan dicatat, mis. hasil dari {@link #ringkas(String, int)}
	 */
	private void catatTerkini(String baris) {
		terkini.offer(baris);
		while (terkini.size() > MAKS_TERKINI) {
			terkini.poll();
		}
	}

	/**
	 * Meringkas teks untuk tampilan log: menormalkan spasi berurutan menjadi satu spasi, lalu
	 * memotong hingga {@code maks} karakter dengan tanda elipsis ({@code …}) bila lebih panjang.
	 *
	 * @param s teks sumber; {@code null} diperlakukan sebagai string kosong
	 * @param maks panjang maksimum hasil (termasuk tanda elipsis bila dipotong)
	 * @return teks yang sudah diringkas, tidak pernah {@code null}
	 */
	private static String ringkas(String s, int maks) {
		if (s == null) {
			return "";
		}
		s = s.trim().replaceAll("\\s+", " ");
		return s.length() > maks ? s.substring(0, maks - 1) + "…" : s;
	}
}
