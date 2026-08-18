package ais.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.zkoss.zk.ui.Executions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Notifikasi;

/**
 * <h1>NotifikasiCache — Cache multi-level (L1/L2/L3) + warm-up untuk pemberitahuan pengguna</h1>
 *
 * <p>
 * Kelas ini adalah lapisan baca berperforma tinggi untuk daftar
 * {@link Notifikasi} yang ditampilkan otomatis di hampir setiap halaman aplikasi:
 * lonceng pemberitahuan pada header ZKoss ({@code MainAction}/{@code MainAction2},
 * dirender dari {@code index.zul}) dan pusat notifikasi pada antarmuka JSP
 * ({@code _api_notifikasi_sistem.jsp}). Tanpa cache, kedua kanal tersebut akan
 * menembak basis data <i>setiap kali timer berdetak</i> dan <i>setiap kali halaman
 * dibuka</i> — beban yang besar dan berulang, sekaligus penyebab "hambatan" yang
 * paling terasa saat aplikasi pertama kali diakses (cold start) karena
 * {@code SessionFactory} dan rencana query belum panas.
 * </p>
 *
 * <h2>Filosofi tiga lapisan (mengikuti praktik terbaik caching aplikasi web)</h2>
 * <p>
 * Strategi mengikuti pola yang sama dengan {@link DashboardCacheUtil}, namun
 * disesuaikan untuk data pemberitahuan yang sering berubah:
 * </p>
 * <ol>
 *   <li><b>L1 — cakupan permintaan (request scope).</b> Hasil per-pengguna disimpan
 *       sebagai atribut {@link org.zkoss.zk.ui.Execution} sehingga bila dalam satu
 *       permintaan yang sama daftar notifikasi diminta lebih dari sekali, ia hanya
 *       dihitung satu kali. Ini lapisan terdekat dan tercepat; otomatis hilang saat
 *       permintaan selesai sehingga tidak pernah basi.</li>
 *   <li><b>L2 — cakupan sesi/tab (session scope).</b> Hasil per-pengguna disimpan di
 *       atribut {@code Desktop} ZK (lihat {@link DashboardCacheUtil#putL2}) dengan TTL
 *       pendek ({@value #TTL_L2_MS} ms). Saat lonceng berdetak berkali-kali dalam tab
 *       yang sama, pemindaian snapshot pun dilewati. Untuk antarmuka non-ZK (JSP)
 *       lapisan ini otomatis dilewati dengan aman (tidak ada {@code Desktop}).</li>
 *   <li><b>L3 — cakupan JVM (application scope).</b> Satu <i>snapshot</i> berisi
 *       maksimal {@value #MAKS_SNAPSHOT} pemberitahuan terbaru disimpan sebagai
 *       variabel statis dan dipakai bersama oleh seluruh pengguna. Pemfilteran
 *       per-pengguna dilakukan di memori (sangat cepat) sehingga sepuluh pengguna yang
 *       loncengnya berdetak bersamaan tetap hanya memerlukan SATU query ke basis data
 *       (saat snapshot perlu dibangun ulang).</li>
 * </ol>
 *
 * <h2>Warm-up saat startup Tomcat</h2>
 * <p>
 * {@link #warmupStartup()} dipanggil dari {@code AppStartupListener} di thread latar
 * (daemon). Ia membangun snapshot L3 lebih awal — setelah memberi jeda singkat agar
 * {@code SessionFactory} selesai dibangun — sehingga ketika pengguna pertama mengakses
 * aplikasi, daftar notifikasi sudah siap di memori dan <b>tidak ada jeda sama sekali</b>.
 * Kegagalan warm-up tidak pernah menggagalkan startup; snapshot akan dibangun
 * <i>on-demand</i> pada akses pertama bila perlu.
 * </p>
 *
 * <h2>Konsistensi &amp; invalidasi</h2>
 * <p>
 * Snapshot L3 dibangun ulang otomatis bila usianya melewati {@value #TTL_L3_MS} ms
 * atau saat ditandai "kotor". Setiap kali sebuah notifikasi baru disimpan (lihat
 * {@code MailSender.simpanNotifikasiHalaman} dan {@code simpanNotifInternal}),
 * {@link #tandaiKotor()} dipanggil sehingga pembacaan berikutnya menyegarkan snapshot.
 * Karena TTL L1/L2 sangat pendek, pemberitahuan baru akan terlihat oleh pengguna dalam
 * hitungan detik. Pendekatan ini memilih ketersediaan dan kecepatan (eventual
 * consistency jangka sangat pendek) dibanding konsistensi seketika yang mahal —
 * sesuai karakter data pemberitahuan.
 * </p>
 *
 * <h2>Keamanan thread &amp; memori</h2>
 * <p>
 * Snapshot disimpan sebagai {@code volatile} dan dibangun di bawah kunci tunggal
 * ({@link #LOCK}) sehingga hanya satu thread yang membangun ulang pada satu waktu
 * (mencegah "cache stampede"). Item snapshot ({@link Item}) bersifat <i>immutable</i>
 * dan hanya menyimpan kolom ringan (id, nama, keterangan, status, waktu, status baca)
 * — bukan entitas Hibernate ber-relasi — sehingga hemat memori dan aman dibaca lintas
 * thread tanpa sesi aktif.
 * </p>
 *
 * <h2>Cara pakai</h2>
 * <pre>
 *   // Lonceng ZK (butuh classData agar tampil di lonceng):
 *   List&lt;String&gt; ket = NotifikasiCache.keteranganLonceng(userId, 20);
 *
 *   // Pusat notifikasi JSP (semua notifikasi pengguna):
 *   List&lt;NotifikasiCache.Item&gt; items = NotifikasiCache.untukUser(userId, 5);
 *
 *   // Saat menyimpan notifikasi baru:
 *   NotifikasiCache.tandaiKotor();
 * </pre>
 *
 * <p>
 * Dengan begitu maintenance ke depan cukup berfokus pada satu kelas ini: ubah TTL,
 * ukuran snapshot, atau aturan filter di sini — seluruh kanal pemberitahuan langsung
 * mengikuti tanpa perlu menyentuh banyak berkas.
 * </p>
 *
 * <h2>Karakteristik performa &amp; penyetelan</h2>
 * <p>
 * Biaya terbesar hanyalah satu query proyeksi ringan setiap {@value #TTL_L3_MS} ms
 * (atau saat ada notifikasi baru), bukan per detak lonceng maupun per pembukaan
 * halaman. Karena pemfilteran per-pengguna dilakukan terhadap struktur memori yang
 * sudah ada, penambahan jumlah pengguna aktif nyaris tidak menambah beban basis data;
 * yang bertambah hanyalah pemindaian memori yang sangat murah. Bila populasi
 * pemberitahuan harian sangat besar, naikkan {@link #MAKS_SNAPSHOT} secukupnya agar
 * jendela "terbaru" tetap mencakup seluruh notifikasi yang masih relevan bagi tiap
 * pengguna; sebaliknya, bila memori menjadi perhatian, turunkan nilainya. Untuk
 * lingkungan dengan tuntutan kesegaran lebih tinggi, perpendek {@link #TTL_L3_MS} dan
 * {@link #TTL_L2_MS}; untuk menghemat query, perpanjang keduanya. Seluruh parameter
 * tersebut sengaja dikumpulkan sebagai konstanta publik di bagian atas kelas agar
 * penyetelan dapat dilakukan terpusat, terdokumentasi, dan aman tanpa menyentuh logika
 * baca/tulis di tempat lain.
 * </p>
 *
 * <h2>Batasan yang diketahui</h2>
 * <p>
 * Pemfilteran pemilik memakai pencocokan substring pada kolom {@code nama} (yang dapat
 * berisi banyak id penerima berformat JSON), sehingga konsisten dengan perilaku lonceng
 * lama. Bila dibutuhkan ketepatan absolut (menghindari id yang saling menjadi
 * substring), pencocokan dapat dipertajam di satu tempat — yakni di metode pembacaan
 * pada kelas ini — tanpa memengaruhi pemanggil mana pun.
 * </p>
 */
public final class NotifikasiCache {

	private NotifikasiCache() {
	}

	/** Maksimal jumlah pemberitahuan terbaru yang disimpan di snapshot L3. */
	public static final int MAKS_SNAPSHOT = 1500;

	/** Umur snapshot L3 sebelum dibangun ulang (ms). */
	public static final long TTL_L3_MS = 45 * 1000L;

	/** TTL hasil per-pengguna di L2 (ms) — pendek agar pemberitahuan baru cepat tampil. */
	public static final int TTL_L2_MS = 20 * 1000;

	/** Prefix kunci L1 (atribut Execution). */
	private static final String PFX_L1 = "_notifL1_";

	private static final Object LOCK = new Object();

	private static volatile List<Item> snapshot = null;
	private static volatile long dibangunPada = 0L;
	private static volatile boolean kotor = true;

	/**
	 * Kunci status baca PER-PENERIMA: berisi "{notifikasiId}_{userId-lowercase}" untuk
	 * setiap baris pada tabel {@code notifikasi_dibaca} yang notifikasinya masih berada
	 * di dalam jendela snapshot. Thread-safe agar penandaan saat klik (menambah kunci)
	 * aman berbarengan dengan pembacaan.
	 */
	private static volatile Set<String> readKeys = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

	/**
	 * Id notifikasi lama yang kolom {@code buka}-nya sudah true (perilaku per-record
	 * warisan). Dianggap "sudah dibaca untuk semua penerima" demi kompatibilitas mundur,
	 * agar notifikasi lama tidak tiba-tiba muncul sebagai belum dibaca.
	 */
	private static volatile Set<Long> legacyBukaIds = new HashSet<Long>();

	// ════════════════════════════════════════════════════════════════════
	// MODEL ITEM RINGAN (IMMUTABLE)
	// ════════════════════════════════════════════════════════════════════

	/**
	 * Representasi ringan satu pemberitahuan untuk keperluan tampilan. Hanya memuat
	 * kolom yang dibutuhkan konsumen (lonceng &amp; pusat notifikasi), bukan entitas
	 * {@link Notifikasi} penuh, agar hemat memori dan aman dibaca tanpa sesi Hibernate.
	 */
	public static final class Item {
		private final Long id;
		private final String nama;
		private final String keterangan;
		private final String statusNotif;
		private final Date waktu;
		/**
		 * Nilai kolom {@code buka} per-RECORD warisan (lama). Sejak status baca menjadi
		 * PER-PENERIMA (tabel {@code notifikasi_dibaca}), nilai ini TIDAK lagi dipakai untuk
		 * menentukan status baca per pengguna — gunakan
		 * {@link NotifikasiCache#sudahDibacaOleh(Long, String)}. Dipertahankan hanya sebagai
		 * penanda kompatibilitas mundur: notifikasi lama yang {@code buka=true} tetap
		 * dianggap sudah dibaca untuk semua penerima.
		 */
		private final boolean buka;

		public Item(Long id, String nama, String keterangan, String statusNotif, Date waktu, boolean buka) {
			this.id = id;
			this.nama = nama == null ? "" : nama;
			this.keterangan = keterangan == null ? "" : keterangan;
			this.statusNotif = statusNotif;
			this.waktu = waktu;
			this.buka = buka;
		}

		public Long getId() {
			return id;
		}

		public String getNama() {
			return nama;
		}

		public String getKeterangan() {
			return keterangan;
		}

		public String getStatusNotif() {
			return statusNotif;
		}

		public Date getWaktu() {
			return waktu;
		}

		public boolean isBuka() {
			return buka;
		}
	}

	// ════════════════════════════════════════════════════════════════════
	// API PUBLIK
	// ════════════════════════════════════════════════════════════════════

	/**
	 * Daftar {@code keterangan} (JSON {subject, body, classData, bukaZk, bukaJsp, ...})
	 * untuk lonceng ZK milik {@code userId}. Hanya menyertakan notifikasi ber-classData
	 * (syarat tampil di lonceng), urut terbaru, maksimal {@code max} item.
	 */
	@SuppressWarnings("unchecked")
	public static List<String> keteranganLonceng(String userId, int max) {
		if (userId == null || userId.trim().isEmpty()) {
			return new ArrayList<String>();
		}
		String key = "lonceng_" + userId + "_" + max;
		Object l1 = getL1(key);
		if (l1 instanceof List) {
			return (List<String>) l1;
		}
		Object l2 = DashboardCacheUtil.getL2(key);
		if (l2 instanceof List) {
			putL1(key, l2);
			return (List<String>) l2;
		}
		List<String> hasil = new ArrayList<String>();
		String uid = userId.toLowerCase();
		for (Item it : ambilSnapshot()) {
			if (hasil.size() >= max) {
				break;
			}
			if (it.nama.toLowerCase().contains(uid) && it.keterangan.contains("classData")) {
				hasil.add(it.keterangan);
			}
		}
		DashboardCacheUtil.putL2(key, hasil, TTL_L2_MS);
		putL1(key, hasil);
		return hasil;
	}

	/**
	 * Daftar {@link Item} untuk lonceng ZK milik {@code userId} — sama dengan
	 * {@link #keteranganLonceng(String, int)} namun mengembalikan Item lengkap sehingga
	 * pemanggil mengetahui {@code id} (untuk menandai sudah dibaca saat diklik) dan
	 * status {@code buka} (untuk membedakan warna serta menghitung badge hanya dari
	 * notifikasi yang BELUM dibaca).
	 */
	@SuppressWarnings("unchecked")
	public static List<Item> itemLonceng(String userId, int max) {
		if (userId == null || userId.trim().isEmpty()) {
			return new ArrayList<Item>();
		}
		String key = "loncengItem_" + userId + "_" + max;
		Object l1 = getL1(key);
		if (l1 instanceof List) {
			return (List<Item>) l1;
		}
		Object l2 = DashboardCacheUtil.getL2(key);
		if (l2 instanceof List) {
			putL1(key, l2);
			return (List<Item>) l2;
		}
		List<Item> hasil = new ArrayList<Item>();
		String uid = userId.toLowerCase();
		for (Item it : ambilSnapshot()) {
			if (hasil.size() >= max) {
				break;
			}
			if (it.nama.toLowerCase().contains(uid) && it.keterangan.contains("classData")) {
				hasil.add(it);
			}
		}
		DashboardCacheUtil.putL2(key, hasil, TTL_L2_MS);
		putL1(key, hasil);
		return hasil;
	}

	/**
	 * Apakah notifikasi {@code notifId} sudah dibaca OLEH {@code userId} (per-penerima)?
	 *
	 * <p>
	 * True bila ada baris {@code notifikasi_dibaca} untuk pasangan tersebut (tercermin
	 * pada {@link #readKeys}), ATAU — demi kompatibilitas mundur — bila notifikasi lama
	 * kolom {@code buka}-nya sudah true ({@link #legacyBukaIds}). Operasi O(1) di memori.
	 * </p>
	 *
	 * @param notifId id notifikasi
	 * @param userId  user id penerima
	 * @return true bila sudah dibaca oleh pengguna tersebut
	 */
	public static boolean sudahDibacaOleh(Long notifId, String userId) {
		if (notifId == null || userId == null || userId.trim().isEmpty()) {
			return false;
		}
		if (legacyBukaIds.contains(notifId)) {
			return true;
		}
		return readKeys.contains(notifId.longValue() + "_" + userId.trim().toLowerCase());
	}

	private static String escSql(String s) {
		return s == null ? "" : s.replace("'", "''");
	}

	/**
	 * Tandai satu notifikasi sebagai SUDAH DIBACA oleh {@code userId} (PER-PENERIMA):
	 * menyisipkan baris {@code notifikasi_dibaca} bila belum ada, lalu menambahkan
	 * kuncinya ke {@link #readKeys} sehingga badge/daftar langsung konsisten tanpa
	 * menunggu TTL cache. Status pembaca lain tidak terpengaruh.
	 *
	 * @param id     id notifikasi
	 * @param userId user id penerima yang membaca
	 */
	public static void tandaiSudahDibaca(Long id, String userId) {
		if (id == null || userId == null || userId.trim().isEmpty()) {
			return;
		}
		String uid = userId.trim();
		String uidEsc = escSql(uid);
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			session.getTransaction().begin();
			// Sisip hanya bila belum ada -> idempoten walau tanpa indeks unik.
			session.createSQLQuery(
					"insert into public.notifikasi_dibaca (notifikasi_id, user_id, waktu) "
							+ "select " + id.longValue() + ", '" + uidEsc + "', now() where not exists "
							+ "(select 1 from public.notifikasi_dibaca where notifikasi_id = " + id.longValue()
							+ " and user_id = '" + uidEsc + "')")
					.executeUpdate();
			session.getTransaction().commit();
		} catch (Throwable e) {
			try {
				if (session != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Throwable ig) {
			}
			System.err.println("NotifikasiCache: gagal menandai notifikasi dibaca: " + e.getMessage());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		try {
			readKeys.add(id.longValue() + "_" + uid.toLowerCase());
		} catch (Throwable ignored) {
		}
	}

	/**
	 * Apakah notifikasi {@code id} memang ditujukan kepada {@code userId}? Dipakai
	 * sebagai penjaga sebelum menandai "sudah dibaca" dari antarmuka JSP, agar seorang
	 * pengguna tidak dapat mengubah status notifikasi milik orang lain.
	 *
	 * @param userId user id pengguna yang sedang masuk
	 * @param id     id notifikasi
	 * @return true bila notifikasi tersebut memuat {@code userId} pada daftar penerima
	 */
	public static boolean milikPengguna(String userId, Long id) {
		if (userId == null || userId.trim().isEmpty() || id == null) {
			return false;
		}
		String uid = userId.toLowerCase();
		for (Item it : ambilSnapshot()) {
			if (id.equals(it.getId())) {
				return it.nama.toLowerCase().contains(uid);
			}
		}
		return false;
	}

	/**
	 * Tandai SEMUA notifikasi milik {@code userId} (dalam jendela snapshot terbaru)
	 * sebagai sudah dibaca — dipakai tombol "Tandai Semua Telah Dibaca".
	 *
	 * <p>
	 * Id dikumpulkan dari snapshot lalu diperbarui memakai klausa {@code id in (...)}
	 * berisi angka murni, sehingga aman dari penyisipan SQL sekaligus tepat sasaran
	 * (tidak bergantung pada pencocokan kolom {@code nama} yang berformat JSON).
	 * </p>
	 *
	 * @param userId user id pengguna yang sedang masuk
	 */
	public static void tandaiSemuaDibaca(String userId) {
		if (userId == null || userId.trim().isEmpty()) {
			return;
		}
		String uid = userId.trim();
		String uidLow = uid.toLowerCase();
		String uidEsc = escSql(uid);
		// Kumpulkan id milik pengguna yang BELUM dibaca oleh pengguna ini (per-penerima).
		List<Long> ids = new ArrayList<Long>();
		for (Item it : ambilSnapshot()) {
			if (it.getId() != null && it.nama.toLowerCase().contains(uidLow) && !sudahDibacaOleh(it.getId(), uid)) {
				ids.add(it.getId());
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (Long id : ids) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(id.longValue());
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			session.getTransaction().begin();
			// Sisipkan satu baris per notifikasi untuk pengguna ini; lewati yang sudah ada.
			session.createSQLQuery(
					"insert into public.notifikasi_dibaca (notifikasi_id, user_id, waktu) "
							+ "select n.id, '" + uidEsc + "', now() from (select unnest(array[" + sb.toString()
							+ "]) as id) n where not exists (select 1 from public.notifikasi_dibaca d "
							+ "where d.notifikasi_id = n.id and d.user_id = '" + uidEsc + "')")
					.executeUpdate();
			session.getTransaction().commit();
		} catch (Throwable e) {
			try {
				if (session != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Throwable ig) {
			}
			System.err.println("NotifikasiCache: gagal menandai semua notifikasi dibaca: " + e.getMessage());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		try {
			for (Long id : ids) {
				readKeys.add(id.longValue() + "_" + uidLow);
			}
		} catch (Throwable ignored) {
		}
	}

	/**
	 * Daftar {@link Item} milik {@code userId} untuk pusat notifikasi JSP. Mencakup
	 * semua notifikasi pengguna (tidak mensyaratkan classData), urut terbaru, maksimal
	 * {@code max} item.
	 */
	@SuppressWarnings("unchecked")
	public static List<Item> untukUser(String userId, int max) {
		if (userId == null || userId.trim().isEmpty()) {
			return new ArrayList<Item>();
		}
		String key = "user_" + userId + "_" + max;
		Object l1 = getL1(key);
		if (l1 instanceof List) {
			return (List<Item>) l1;
		}
		Object l2 = DashboardCacheUtil.getL2(key);
		if (l2 instanceof List) {
			putL1(key, l2);
			return (List<Item>) l2;
		}
		List<Item> hasil = new ArrayList<Item>();
		String uid = userId.toLowerCase();
		for (Item it : ambilSnapshot()) {
			if (hasil.size() >= max) {
				break;
			}
			if (it.nama.toLowerCase().contains(uid)) {
				hasil.add(it);
			}
		}
		DashboardCacheUtil.putL2(key, hasil, TTL_L2_MS);
		putL1(key, hasil);
		return hasil;
	}

	/**
	 * Snapshot mentah (seluruh pemberitahuan terbaru lintas pengguna). Dipakai untuk
	 * agregasi dashboard (komposisi tipe, tren harian, rasio dibaca) tanpa query ulang.
	 */
	public static List<Item> snapshot() {
		return ambilSnapshot();
	}

	/** Tandai snapshot L3 perlu dibangun ulang — panggil saat ada notifikasi baru. */
	public static void tandaiKotor() {
		kotor = true;
	}

	// ════════════════════════════════════════════════════════════════════
	// WARM-UP
	// ════════════════════════════════════════════════════════════════════

	/**
	 * Hangatkan snapshot di thread latar (daemon) saat startup Tomcat. Memberi jeda
	 * singkat agar {@code SessionFactory} siap, lalu membangun snapshot. Tidak pernah
	 * menggagalkan startup.
	 */
	public static void warmupStartup() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(45000);
					warmupSekarang();
				} catch (Throwable ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/common/NotifikasiCache.java:309");
					// abaikan — snapshot akan dibangun on-demand pada akses pertama
				}
			}
		}, "notifikasi-cache-warmup");
		t.setDaemon(true);
		t.start();
	}

	/** Bangun ulang snapshot secara sinkron (dipakai warm-up + dapat dipanggil manual). */
	public static void warmupSekarang() {
		bangunUlangSnapshot();
	}

	// ════════════════════════════════════════════════════════════════════
	// INTERNAL
	// ════════════════════════════════════════════════════════════════════

	private static List<Item> ambilSnapshot() {
		List<Item> s = snapshot;
		boolean perluBangun = kotor || s == null || (System.currentTimeMillis() - dibangunPada) > TTL_L3_MS;
		if (perluBangun) {
			s = bangunUlangSnapshot();
		}
		return s == null ? new ArrayList<Item>() : s;
	}

	@SuppressWarnings("unchecked")
	private static List<Item> bangunUlangSnapshot() {
		synchronized (LOCK) {
			// Cek ganda: thread lain mungkin sudah membangun saat kita menunggu kunci.
			if (!kotor && snapshot != null && (System.currentTimeMillis() - dibangunPada) <= TTL_L3_MS) {
				return snapshot;
			}
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				List<Object[]> rows = session.createCriteria(Notifikasi.class)
						.setProjection(Projections.projectionList().add(Projections.property("id"))
								.add(Projections.property("nama")).add(Projections.property("keterangan"))
								.add(Projections.property("statusNotif")).add(Projections.property("waktu"))
								.add(Projections.property("buka")))
						.addOrder(Order.desc("id")).setMaxResults(MAKS_SNAPSHOT).list();

				List<Item> baru = new ArrayList<Item>(rows == null ? 0 : rows.size());
				Set<Long> legacyBaru = new HashSet<Long>();
				StringBuilder idIn = new StringBuilder();
				if (rows != null) {
					for (Object[] r : rows) {
						Long id = r[0] == null ? null : ((Number) r[0]).longValue();
						String nama = r[1] == null ? "" : r[1].toString();
						String ket = r[2] == null ? "" : r[2].toString();
						String status = r[3] == null ? null : r[3].toString();
						Date waktu = r[4] instanceof Date ? (Date) r[4] : null;
						boolean buka = r[5] instanceof Boolean ? ((Boolean) r[5]).booleanValue() : false;
						baru.add(new Item(id, nama, ket, status, waktu, buka));
						if (id != null) {
							if (buka) {
								legacyBaru.add(id);
							}
							if (idIn.length() > 0) {
								idIn.append(",");
							}
							idIn.append(id.longValue());
						}
					}
				}

				// Muat status baca PER-PENERIMA (tabel notifikasi_dibaca) untuk notifikasi
				// yang berada di jendela snapshot. Set diganti utuh (bukan dimutasi) agar
				// pembacaan bersamaan tetap konsisten.
				Set<String> readBaru = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
				if (idIn.length() > 0) {
					try {
						List<Object[]> rk = session
								.createSQLQuery("select notifikasi_id, user_id from public.notifikasi_dibaca "
										+ "where notifikasi_id in (" + idIn.toString() + ")")
								.list();
						if (rk != null) {
							for (Object[] row : rk) {
								if (row[0] == null || row[1] == null) {
									continue;
								}
								long nid = ((Number) row[0]).longValue();
								readBaru.add(nid + "_" + row[1].toString().toLowerCase());
							}
						}
					} catch (Throwable eRead) {
						// Tabel mungkin belum terbentuk pada startup pertama (dibuat hbm2ddl).
						System.err.println("NotifikasiCache: notifikasi_dibaca belum siap: " + eRead.getMessage());
					}
				}

				snapshot = baru;
				legacyBukaIds = legacyBaru;
				readKeys = readBaru;
				dibangunPada = System.currentTimeMillis();
				kotor = false;
				return baru;
			} catch (Throwable e) {
				// Bila gagal, jangan biarkan null permanen — kembalikan snapshot lama bila ada.
				System.err.println("NotifikasiCache: gagal membangun snapshot: " + e.getMessage());
				return snapshot == null ? new ArrayList<Item>() : snapshot;
			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
		}
	}

	// ── L1 (atribut Execution / request) ───────────────────────────────
	private static Object getL1(String key) {
		try {
			if (Executions.getCurrent() != null) {
				return Executions.getCurrent().getAttribute(PFX_L1 + key);
			}
		} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/NotifikasiCache.java:385");
		}
		return null;
	}

	private static void putL1(String key, Object data) {
		try {
			if (Executions.getCurrent() != null) {
				Executions.getCurrent().setAttribute(PFX_L1 + key, data);
			}
		} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/NotifikasiCache.java:395");
		}
	}

	/**
	 * Utilitas: dari daftar keterangan, ambil subject unik (untuk monitoring/uji).
	 * Tidak dipakai jalur render utama; disediakan agar logika dedup dapat diuji.
	 */
	public static Set<String> subjectUnik(List<String> keterangans) {
		Set<String> hasil = new HashSet<String>();
		if (keterangans != null) {
			for (String k : keterangans) {
				try {
					hasil.add(new org.json.JSONObject(k).optString("subject", ""));
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/NotifikasiCache.java:409");
				}
			}
		}
		return hasil;
	}
}
