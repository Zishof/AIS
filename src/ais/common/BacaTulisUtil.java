package ais.common;

import java.io.File;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;

/**
 * Utilitas baca/tulis "flag" dan data kecil berbasis kunci, dengan tiga lapis penyimpanan: cache
 * RAM (instan), basis data (tabel {@code ais_flags_data_baru}), dan berkas fisik (mode lama).
 * Dipakai untuk penanda ringan per-pengguna—mis. info pembaruan tampilan, pengumuman yang harus
 * langsung tampil, status proses—yang perlu cepat ditulis dan dibaca tanpa membuat entity/tabel
 * khusus.
 *
 * <h3>Untuk apa class ini</h3>
 * Banyak fitur hanya butuh menyimpan sepotong string per kunci (mis. "sudah lihat pengumuman X").
 * Membuat entity Hibernate untuk tiap kebutuhan itu berlebihan. BacaTulisUtil menyediakan API
 * sederhana ({@link #tulis(File, String)}/{@link #baca(File)} dan
 * {@link #tulisFlag(String, String)}/{@link #bacaFlag(String)}) yang cepat, aman dari deadlock, dan
 * koheren antar-node kluster.
 *
 * <h3>Tiga lapis penyimpanan &amp; mode</h3>
 * <ul>
 *   <li><b>Cache RAM.</b> Saat {@link #mulai} {@code true}, dipakai {@code MemoryDbUtil} yang
 *   cluster-aware (sinkron via broadcast {@link MemoryCacheUtil}); saat {@code false}, dipakai
 *   {@link #flagLocalCache} (ConcurrentHashMap lokal proses) agar tidak ada baca-basi antar-node
 *   sebelum kluster aktif.</li>
 *   <li><b>Database.</b> Saat {@link #flagDataMenggunakandatabase} {@code true} (dan selalu untuk
 *   API flag langsung), data disimpan ke {@code ais_flags_data_baru} via native SQL.</li>
 *   <li><b>Berkas fisik.</b> Mode lama (legacy) ketika DB tidak dipakai—data ditulis/dibaca sebagai
 *   berkas memakai {@code commons-io}.</li>
 * </ul>
 *
 * <h3>Kunci (key) yang aman</h3>
 * Kunci global berbentuk {@code <parentFolder>_<namaFile>} yang di-{@link #sanitize(String)}
 * (membuang tanda baca selain {@code _} dan {@code -}) agar konsisten dipakai sebagai PK kolom
 * {@code flag_key}. Ini menyatukan representasi "berkas" dan "baris DB" di bawah satu kunci.
 *
 * <h3>Tulis cepat &amp; bebas deadlock</h3>
 * Penulisan DB dapat ASINKRON + COALESCED (konfigurasi {@code bacatulis_async_write}, default
 * aktif): satu worker daemon tunggal menyerialkan tulisan (tidak ada deadlock antar-writer) dan
 * tulisan beruntun ke kunci sama digabung jadi sekali (last-write-wins via {@link #pendingWrites}).
 * Pemanggil tidak menunggu DB—nilai sudah terlihat dari cache seketika. Saat shutdown,
 * {@link #flushPendingWrites()} memastikan tak ada flag tertinggal.
 *
 * <h3>Ketahanan DDL (PostgreSQL)</h3>
 * {@link #ensureTableAndIndexExists(Session)} membuat tabel di SESSION+TRANSAKSI TERPISAH yang
 * di-commit sendiri (bukan numpang transaksi pemanggil), karena DDL bersifat transaksional di
 * PostgreSQL—numpang transaksi yang lalu di-rollback akan ikut membatalkan CREATE TABLE sementara
 * flag "sudah init" terlanjur true (bug "relation does not exist" permanen). Verifikasi memakai
 * {@code CAST(to_regclass(...) AS text)} (BUKAN {@code ::text}, yang dikira named-parameter oleh
 * Hibernate). Upsert memakai {@code INSERT ... ON CONFLICT} atomik.
 *
 * <h3>Keamanan thread &amp; pemeliharaan</h3>
 * Cache memakai ConcurrentHashMap; inisialisasi tabel/executor pakai double-checked locking; setiap
 * session DB ditutup rapi di {@code finally} via {@link #cleanupSession(Session)}. Kompatibel Java
 * 1.6/1.7 (anonymous class, bukan lambda). Lihat juga sejarah perbaikan DDL/cast pada catatan
 * memori proyek.
 */
public class BacaTulisUtil {

	public static boolean mulai = false;
	public static boolean flagDataMenggunakandatabase = false;

	private static final Pattern KEY_PATTERN = Pattern.compile("[\\p{Punct}&&[^_-]]+");
	private static volatile boolean isTableInitialized = false;

	// ===================================================================================
	// PERCEPATAN BACA: cache lokal proses (dipakai saat cluster-cache MemoryDb belum aktif,
	// yaitu mulai=false). Saat mulai=true, dipakai MemoryDb (sudah cluster-aware via broadcast)
	// agar tidak ada baca basi antar-node.
	// ===================================================================================
	private static final java.util.concurrent.ConcurrentHashMap<String, String> flagLocalCache = new java.util.concurrent.ConcurrentHashMap<String, String>();

	// ===================================================================================
	// PERCEPATAN TULIS (anti-deadlock): antrian tulis ASINKRON + COALESCED.
	// - 1 worker thread → penulisan ke DB diserialisasi → TIDAK ADA deadlock antar-writer.
	// - coalescing per-key (last-write-wins) → tulisan beruntun ke key sama cukup 1x ke DB.
	// - caller TIDAK menunggu DB ("secepat kilat"); nilai sudah terlihat dari cache seketika.
	// Diaktifkan via konfigurasi "bacatulis_async_write" (default AKTIF). Bila non-aktif,
	// penulisan tetap sinkron memakai upsert atomik.
	// ===================================================================================
	private static final Object WRITE_LOCK = new Object();
	private static volatile java.util.concurrent.ExecutorService writeExecutor = null;
	// key -> [parentKey, value]
	private static final java.util.concurrent.ConcurrentHashMap<String, String[]> pendingWrites = new java.util.concurrent.ConcurrentHashMap<String, String[]>();
	// Maksimum jumlah flag yang digabung dalam SATU transaksi tulis (batching).
	// Menggabungkan banyak UPSERT dalam 1 commit menekan overhead commit & buka-tutup
	// session saat ada lonjakan tulis, sehingga worker tidak tertinggal. TIDAK mengubah
	// nilai/menghapus data; hanya cara penyetorannya yang dibatch.
	private static final int WRITE_BATCH_MAX = 500;

	// Tracker melacak berdasarkan NAMA FOLDER untuk memastikan seluruh isi folder ada di RAM
	private static final java.util.Set<String> folderSudahDiLoad = java.util.Collections
			.synchronizedSet(new java.util.HashSet<String>());

	// ===================================================================================
	// PENGHASIL KUNCI (KEY GENERATOR)
	// ===================================================================================

	/**
	 * Membersihkan string dari karakter tanda baca agar layak menjadi bagian kunci.
	 *
	 * <p><b>Tujuan.</b> Kunci dipakai sebagai PK kolom {@code flag_key}/{@code flag_key_parent_folder}
	 * dan sebagai nama logis cache; karakter tanda baca dapat memecah konsistensi atau menyulitkan
	 * pencocokan awalan. Method ini menormalkan input agar kunci selalu terdiri dari karakter aman.</p>
	 *
	 * <p><b>Cara kerja.</b> Bila {@code raw} null mengembalikan {@code ""}. Selain itu menerapkan
	 * {@link #KEY_PATTERN} (regex {@code [\p{Punct}&&[^_-]]+}) untuk menghapus SEMUA tanda baca KECUALI
	 * garis bawah ({@code _}) dan tanda hubung ({@code -})—keduanya dipertahankan karena {@code _}
	 * adalah pemisah parent/file dan {@code -} sering muncul di kunci yang sah.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code raw} = string mentah (boleh null). Mengembalikan string
	 * bersih (mungkin kosong), tidak pernah null.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Bila menambah karakter yang harus dipertahankan, ubah {@link #KEY_PATTERN}
	 * dengan hati-hati—mengubahnya dapat menggeser kunci yang sudah tersimpan di DB sehingga data lama
	 * "hilang" dari pandangan. Method privat, fondasi semua pembentuk kunci.</p>
	 *
	 * @param raw string mentah (boleh null)
	 * @return string yang sudah dibersihkan dari tanda baca selain {@code _} dan {@code -}
	 */
	private static String sanitize(String raw) {
		return raw == null ? "" : KEY_PATTERN.matcher(raw).replaceAll("");
	}

	/**
	 * Mengambil komponen kunci dari NAMA FOLDER INDUK sebuah berkas.
	 *
	 * <p><b>Tujuan.</b> Bagian "parent" dari kunci global menandai pengelompokan (folder) sehingga
	 * data sejenis bisa dicari/dihapus per-kelompok lewat kolom {@code flag_key_parent_folder}.</p>
	 *
	 * <p><b>Cara kerja.</b> Bila {@code file} null mengembalikan {@code "root"}. Selain itu mengambil
	 * {@code getParentFile().getName()} lalu di-{@link #sanitize(String)}; bila tak ada induk, memakai
	 * {@code "root"}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code file} = berkas acuan (boleh null). Mengembalikan kunci
	 * parent yang sudah bersih.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Nilai {@code "root"} adalah penampung default; jangan dijadikan nama
	 * folder nyata agar tidak bercampur. Method privat.</p>
	 *
	 * @param file berkas acuan (boleh null)
	 * @return kunci dari nama folder induk, atau "root"
	 */
	private static String getParentKey(File file) {
		if (file == null) return "root";
		File parent = file.getParentFile();
		return sanitize(parent != null ? parent.getName() : "root");
	}

	/**
	 * Mengambil komponen kunci dari NAMA BERKAS.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Bagian "file" dari kunci global mengidentifikasi entri spesifik
	 * dalam kelompoknya. Mengembalikan {@code ""} bila {@code file} null; selain itu
	 * {@link #sanitize(String) sanitize}({@code file.getName()}).</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code file} = berkas acuan (boleh null). Mengembalikan kunci
	 * nama berkas yang sudah bersih (mungkin kosong).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Method privat; dipakai bersama {@link #getParentKey(File)} untuk membentuk
	 * kunci global.</p>
	 *
	 * @param file berkas acuan (boleh null)
	 * @return kunci dari nama berkas
	 */
	private static String getFileKey(File file) {
		return file == null ? "" : sanitize(file.getName());
	}

	/**
	 * Menggabungkan kunci parent dan kunci file menjadi satu kunci global unik.
	 *
	 * <p><b>Tujuan.</b> Membentuk identitas tunggal ({@code <parent>_<file>}) yang dipakai sebagai PK
	 * {@code flag_key} di DB dan key di cache, sehingga "berkas" dan "baris DB" konsisten dirujuk.</p>
	 *
	 * <p><b>Cara kerja.</b> Menggabung dengan pemisah {@code "_"}. Karena {@link #sanitize(String)}
	 * mempertahankan {@code _}, struktur {@code parent_file} tetap dapat dipakai untuk pencocokan
	 * awalan ({@code startsWith}) saat mengambil/menghapus per-folder.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code parentKey}, {@code fileKey} = dua komponen kunci.
	 * Mengembalikan kunci global gabungan.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Jangan mengubah pemisah {@code "_"}; banyak logika pencocokan awalan
	 * bergantung padanya. Method privat.</p>
	 *
	 * @param parentKey komponen parent
	 * @param fileKey   komponen file
	 * @return kunci global {@code parent_file}
	 */
	private static String getGlobalKey(String parentKey, String fileKey) {
		return parentKey + "_" + fileKey;
	}

	/**
	 * Membentuk kunci global dari sebuah {@link File} secara langsung (gabungan parent+file).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Pintasan yang menggabungkan {@link #getParentKey(File)} dan
	 * {@link #getFileKey(File)} lewat {@link #getGlobalKey(String, String)}. Dipakai pada jalur hapus
	 * massal ({@link #doHapus(File, String)}) di mana kunci perlu dihitung dari berkas yang sedang
	 * di-iterasi.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code file} = berkas acuan. Mengembalikan kunci global-nya.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Disebut "legacy" karena erat dengan representasi berkas; tetap valid
	 * untuk membentuk kunci DB karena skema kunci sama. Method privat.</p>
	 *
	 * @param file berkas acuan
	 * @return kunci global dari berkas tersebut
	 */
	private static String generateLegacyKey(File file) {
		return getGlobalKey(getParentKey(file), getFileKey(file));
	}

	// ===================================================================================
	// METODE UTAMA (TULIS, BACA, HAPUS)
	// ===================================================================================

	/**
	 * Menangani kegagalan saat mengakses cache MapDB (get/put pada HTreeMap "data_temp" milik
	 * {@link MemoryDbUtil#getDataKey()}).
	 *
	 * <p><b>Kenapa penting.</b> MapDB dapat melempar {@link java.io.IOError} yang membungkus
	 * {@link java.nio.channels.ClosedChannelException} bila FileChannel store-nya tertutup di tengah
	 * jalan (mmap remap gagal, berkas terusik, dll). {@code IOError} adalah turunan {@link Error} —
	 * BUKAN {@link Exception} — sehingga LOLOS dari {@code catch (Exception)} di seluruh pemanggil
	 * ({@code tulis}/{@code baca} → {@code GeneralValueObject} → {@code FilterLoginAis}) dan
	 * MERUNTUHKAN request kritis (login, security filter, profil, biodata). Di sini kegagalan MapDB
	 * dideteksi lalu store DIBANGUN ULANG via {@link MemoryCacheUtil#rebuildAfterFailure(String)} agar
	 * akses berikutnya pulih. Cache hanya mirror (sumber kebenaran RDBMS) sehingga kegagalan tunggal
	 * cukup diabaikan setelah penyembuhan — pemanggil tetap jalan (baca mengembalikan "").</p>
	 *
	 * @param t    error/exception yang tertangkap
	 * @param asal nama jalur pemanggil (untuk log)
	 */
	private static void tanganiKegagalanCache(Throwable t, String asal) {
		boolean mapdbRusak = false;
		Throwable c = t;
		int guard = 0;
		while (c != null && guard++ < 12) {
			String cn = c.getClass().getName();
			String msg = c.getMessage();
			if (c instanceof java.io.IOError || c instanceof java.nio.channels.ClosedChannelException
					|| cn.startsWith("org.mapdb.")
					|| (msg != null && msg.toLowerCase().indexOf("closed") >= 0)) {
				mapdbRusak = true;
				break;
			}
			c = c.getCause();
		}
		if (mapdbRusak) {
			try {
				MemoryCacheUtil.rebuildAfterFailure(asal);
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/BacaTulisUtil.java:261");
				// diabaikan.
			}
		}
	}

	/**
	 * Menulis/memperbarui data untuk sebuah berkas-kunci, dengan koherensi cache dan persistensi.
	 *
	 * <p><b>Tujuan.</b> API tulis utama berbasis {@link File}. Menjamin nilai langsung terlihat dari
	 * cache, ter-replikasi ke node lain (bila kluster aktif), dan tersimpan ke DB/berkas—tanpa menulis
	 * ulang bila nilainya tidak berubah (hemat I/O).</p>
	 *
	 * <h4>Cara kerja</h4>
	 * <ul>
	 *   <li>Membentuk {@code parentKey}/{@code fileKey}/{@code globalKey}.</li>
	 *   <li>Mode kluster ({@link #mulai} true): bila nilai berbeda dari yang ada di
	 *   {@code MemoryDbUtil}, (1) perbarui cache L1 lokal, (2) broadcast "PUT" + data ke node lain
	 *   untuk replikasi, (3) simpan ke DB/berkas via
	 *   {@link #simpanDataFisikAtauDatabase(File, String, String, String)}.</li>
	 *   <li>Mode pra-kluster ({@code mulai} false): bila nilai berbeda dari {@link #flagLocalCache},
	 *   perbarui cache lokal lalu simpan.</li>
	 * </ul>
	 *
	 * <p><b>Parameter.</b> {@code file} = berkas-kunci target; {@code data} = nilai yang ditulis.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Dibungkus try/catch ({@code printStackTrace});
	 * kegagalan tulis tidak menjatuhkan pemanggil. Perbandingan "nilai berubah" memakai
	 * {@code trim().equalsIgnoreCase} sehingga perbedaan spasi/case dianggap sama—pertimbangkan ini
	 * bila butuh perbedaan yang sensitif huruf.</p>
	 *
	 * @param file berkas-kunci target
	 * @param data nilai yang ditulis
	 */
	public static void tulis(File file, String data) {
		try {
			String parentKey = getParentKey(file);
			String fileKey = getFileKey(file);
			String globalKey = getGlobalKey(parentKey, fileKey);

			if (mulai) {
				String dataSudahAda = MemoryDbUtil.getDataKey().get(globalKey);

				if (dataSudahAda == null || !dataSudahAda.trim().equalsIgnoreCase(data)) {
					// 1. Update L1 Cache Lokal di Tomcat Pengirim
					MemoryDbUtil.getDataKey().put(globalKey, data);

					// 2. Beri tahu Tomcat Lain ("PUT") sekaligus kirim Data terbarunya (Replikasi)
					MemoryCacheUtil.broadcastUpdate("data_temp", "PUT", globalKey, data);

					// 3. Simpan ke database atau file fisik
					simpanDataFisikAtauDatabase(file, parentKey, globalKey, data);
				}
			} else {
				// Mode pra-cluster: cache lokal agar baca berikutnya instan, lalu simpan.
				String dataSudahAda = flagLocalCache.get(globalKey);
				if (dataSudahAda == null || !dataSudahAda.trim().equalsIgnoreCase(data)) {
					flagLocalCache.put(globalKey, data);
					simpanDataFisikAtauDatabase(file, parentKey, globalKey, data);
				}
			}
		} catch (Throwable e) {
			// Tangkap Throwable (bukan hanya Exception): MapDB bisa melempar java.io.IOError
			// (ClosedChannelException) yang turunan Error → LOLOS catch(Exception) & meruntuhkan
			// pemanggil. Sembuhkan store lalu abaikan (tulis flag bersifat best-effort).
			tanganiKegagalanCache(e, "tulis");
		}
	}

	/**
	 * Membaca data untuk sebuah berkas-kunci, dengan cache dan auto-migrasi berkas→DB.
	 *
	 * <p><b>Tujuan.</b> API baca utama berbasis {@link File}. Mengutamakan cache RAM (instan), lalu
	 * jatuh ke DB/berkas bila perlu, sekaligus menghangatkan cache untuk pembacaan berikutnya.</p>
	 *
	 * <h4>Cara kerja</h4>
	 * <ul>
	 *   <li>Ambil dari cache: {@code MemoryDbUtil} (mode kluster) atau {@link #flagLocalCache}
	 *   (pra-kluster).</li>
	 *   <li>Bila cache kosong, baca dari DB/berkas via
	 *   {@link #ambilDataFisikAtauDatabase(File, String, String)} (yang juga melakukan auto-migrasi:
	 *   data lama di berkas akan disalin ke DB saat pertama dibaca dalam mode DB).</li>
	 *   <li>Bila ketemu, simpan ke cache agar baca berikutnya cepat.</li>
	 * </ul>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code file} = berkas-kunci. Mengembalikan nilai data, atau
	 * {@code ""} (string kosong) bila tidak ada—TIDAK pernah null, agar pemanggil aman.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Exception sengaja diabaikan (mengikuti logika
	 * asli) dan tetap mengembalikan {@code ""}. Konvensi "tidak pernah null" harus dipertahankan;
	 * banyak pemanggil mengandalkannya.</p>
	 *
	 * @param file berkas-kunci yang dibaca
	 * @return nilai data, atau {@code ""} bila tidak ada
	 */
	public static String baca(File file) {
		String data = null;
		try {
			String parentKey = getParentKey(file);
			String fileKey = getFileKey(file);
			String globalKey = getGlobalKey(parentKey, fileKey);

			// Catat akses (untuk warm-up cache flag yang sering dipakai saat start berikutnya).
			FlagAccessCache.catat(globalKey);

			if (mulai) {
				data = MemoryDbUtil.getDataKey().get(globalKey);
			} else {
				data = flagLocalCache.get(globalKey);
			}

			if (data == null) {
				// Coba ambil dari Database atau Fisik (dengan fitur Auto-Migration)
				data = ambilDataFisikAtauDatabase(file, parentKey, globalKey);

				if (data != null) {
					// Simpan ke cache agar baca berikutnya cepat
					if (mulai) {
						MemoryDbUtil.getDataKey().put(globalKey, data);
					} else {
						flagLocalCache.put(globalKey, data);
					}
				}
			}
		} catch (Throwable e) {
			// Tangkap Throwable: MapDB IOError (ClosedChannelException) = Error, bukan Exception →
			// tanpa ini login/security-filter/profil/biodata runtuh. Sembuhkan store & kembalikan ""
			// (aman: sumber kebenaran tetap di RDBMS).
			tanganiKegagalanCache(e, "baca");
		}

		return data == null ? "" : data;
	}

	/**
	 * Muat BANYAK kunci flag sekaligus dari DB ke cache (warm-up saat start). Aman dipanggil
	 * paralel (tiap pemanggil membawa batch kunci sendiri + session sendiri). Gagal-diam.
	 */
	public static void muatKeCacheBanyak(java.util.Collection<String> globalKeys) {
		if (globalKeys == null || globalKeys.isEmpty()) {
			return;
		}
		java.util.List<String> batch = new java.util.ArrayList<String>();
		for (String k : globalKeys) {
			if (k != null && !k.isEmpty()) {
				batch.add(k);
			}
			if (batch.size() >= 500) {
				muatBatchKeCache(batch);
				batch = new java.util.ArrayList<String>();
			}
		}
		if (!batch.isEmpty()) {
			muatBatchKeCache(batch);
		}
	}

	@SuppressWarnings("unchecked")
	private static void muatBatchKeCache(java.util.List<String> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			ensureTableAndIndexExists(session);
			Query q = session.createSQLQuery(
					"SELECT flag_key, flag_value FROM ais_flags_data_baru WHERE flag_key IN (:keys)");
			q.setParameterList("keys", keys);
			java.util.List<Object[]> rows = q.list();
			for (Object[] r : rows) {
				if (r == null || r[0] == null) {
					continue;
				}
				String gk = r[0].toString();
				String val = r[1] == null ? "" : r[1].toString();
				if (mulai) {
					MemoryDbUtil.getDataKey().put(gk, val);
				} else {
					flagLocalCache.put(gk, val);
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/BacaTulisUtil.java:443");
			// gagal-diam: warm-up tidak boleh menjatuhkan start.
		} finally {
			cleanupSession(session);
		}
	}

	/**
	 * Menghapus data sebuah berkas-kunci dari SEMUA lapis: tulis-tertunda, cache, DB, dan berkas fisik.
	 *
	 * <p><b>Tujuan.</b> Menghapus tuntas sebuah entri sehingga tidak muncul kembali dari salah satu
	 * lapis penyimpanan—termasuk membatalkan tulisan asinkron yang mungkin masih mengantre.</p>
	 *
	 * <h4>Cara kerja (urutan disengaja)</h4>
	 * <ol>
	 *   <li>{@link #pendingWrites}.remove + {@link #flagLocalCache}.remove — batalkan tulis tertunda
	 *   dan bersihkan cache lokal, agar nilai lama tidak "hidup lagi" setelah dihapus.</li>
	 *   <li>Mode kluster: hapus dari {@code MemoryDbUtil} dan broadcast "REMOVE" ke node lain.</li>
	 *   <li>Bila pakai DB: {@link #deleteFromDatabase(String)}.</li>
	 *   <li>Bila berkas fisik ada: {@code file.delete()}.</li>
	 * </ol>
	 *
	 * <p><b>Parameter.</b> {@code file} = berkas-kunci yang dihapus.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Dibungkus try/catch ({@code printStackTrace}).
	 * Urutan "batalkan tulis tertunda lebih dulu" penting di mode async—tanpa itu, worker bisa menulis
	 * ulang nilai yang baru saja dihapus. Pertahankan urutan tersebut.</p>
	 *
	 * @param file berkas-kunci yang dihapus
	 */
	public static void hapus(File file) {
		try {
			String parentKey = getParentKey(file);
			String fileKey = getFileKey(file);
			String globalKey = getGlobalKey(parentKey, fileKey);

			// Batalkan tulis tertunda + bersihkan cache lokal agar tidak muncul kembali.
			pendingWrites.remove(globalKey);
			flagLocalCache.remove(globalKey);

			if (mulai) {
				MemoryDbUtil.getDataKey().remove(globalKey);
				MemoryCacheUtil.broadcastUpdate("data_temp", "REMOVE", globalKey, null);
			}

			if (flagDataMenggunakandatabase) {
				deleteFromDatabase(globalKey);
			}

			if (file != null && file.exists()) {
				file.delete();
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BacaTulisUtil.java:497");
		}
	}

	/**
	 * Menghapus SEKUMPULAN berkas-kunci dalam satu folder yang namanya berawalan {@code string}.
	 *
	 * <p><b>Tujuan.</b> Penghapusan massal berbasis awalan—mis. membuang semua flag bertema sama dalam
	 * satu folder sekaligus, tanpa menyebut tiap kunci satu per satu.</p>
	 *
	 * <h4>Cara kerja</h4>
	 * <ul>
	 *   <li>Menentukan folder induk dari {@code file} dan kunci parent-nya.</li>
	 *   <li>Untuk tiap berkas dalam folder yang namanya {@code startsWith(string)} (case-insensitive):
	 *   hapus berkas fisik, lalu bersihkan tulis-tertunda &amp; cache, dan (mode kluster) hapus dari
	 *   MemoryDb + broadcast "REMOVE".</li>
	 *   <li>Bila pakai DB, panggil {@link #deletePrefixFromDatabase(String, String)} untuk menghapus
	 *   baris berawalan di DB.</li>
	 * </ul>
	 *
	 * <p><b>Parameter.</b> {@code file} = berkas acuan (penentu folder induk); {@code string} = awalan
	 * nama berkas/kunci yang akan dihapus.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> No-op bila {@code file} null; dibungkus try/catch.
	 * Pencocokan awalan dilakukan case-insensitive baik di sisi berkas maupun—lewat helper DB—di sisi
	 * database. Hati-hati memilih awalan agar tidak menghapus lebih banyak dari yang dimaksud.</p>
	 *
	 * @param file   berkas acuan penentu folder induk
	 * @param string awalan nama berkas/kunci yang dihapus
	 */
	public static void doHapus(File file, String string) {
		try {
			if (file == null) return;

			File parentFolder = file.getParentFile();
			String parentKey = sanitize(parentFolder != null ? parentFolder.getName() : "root");
			String filePrefix = sanitize(string);

			if (parentFolder != null && parentFolder.exists()) {
				File[] files = parentFolder.listFiles();
				if (files != null) {
					for (File filehapus : files) {
						if (filehapus.getName().toLowerCase().startsWith(string.toLowerCase())) {
							filehapus.delete(); 

							String globalKey = generateLegacyKey(filehapus);
							pendingWrites.remove(globalKey);
							flagLocalCache.remove(globalKey);
							if (mulai) {
								MemoryDbUtil.getDataKey().remove(globalKey);
								MemoryCacheUtil.broadcastUpdate("data_temp", "REMOVE", globalKey, null);
							}
						}
					}
				}
			}

			if (flagDataMenggunakandatabase) {
				deletePrefixFromDatabase(parentKey, filePrefix);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BacaTulisUtil.java:559");
		}
	}

	/**
	 * Mengambil SEMUA nilai dalam satu folder yang kuncinya berawalan {@code prefixNamaFile}.
	 *
	 * <p><b>Tujuan.</b> Membaca sekumpulan flag/data sekaligus (mis. semua catatan bertema sama dalam
	 * satu folder), efisien dari DB maupun dari cache, untuk ditampilkan/diproses bersama.</p>
	 *
	 * <h4>Cara kerja</h4>
	 * <ul>
	 *   <li><b>Mode DB.</b> Bila folder sudah pernah dimuat penuh ke cache
	 *   ({@link #folderSudahDiLoad}) dan kluster aktif, hasil diambil dari {@code MemoryDbUtil} dengan
	 *   menyaring key berawalan {@code <parent>_<prefix>}. Bila belum, satu query
	 *   {@code WHERE flag_key_parent_folder = :parent} (EXACT match parent, tanpa LIKE) menarik SELURUH
	 *   isi folder; hasil disaring awalan di memori Java dan SELURUH konten dimasukkan ke cache untuk
	 *   akses berikutnya, lalu folder ditandai sudah-dimuat.</li>
	 *   <li><b>Mode berkas (legacy).</b> Menelusuri berkas dalam folder yang namanya berawalan prefix
	 *   dan membaca tiap nilainya via {@link #baca(File)}.</li>
	 * </ul>
	 *
	 * <p><b>Mengapa "tanpa LIKE".</b> Query exact-match pada kolom ber-index ({@code idx_flags_parent})
	 * jauh lebih murah daripada {@code LIKE 'prefix%'}; penyaringan awalan yang lebih halus dilakukan
	 * di memori. Memuat seluruh folder sekali juga menghindari banyak round-trip.</p>
	 *
	 * <p><b>Higiene koneksi &amp; error.</b> Session DB ditutup di {@code finally} via
	 * {@link #cleanupSession(Session)}. Seluruh proses dibungkus try/catch berlapis; kegagalan di-log
	 * dan mengembalikan apa yang sudah terkumpul.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code parentFolder} = folder sumber; {@code prefixNamaFile} =
	 * awalan nama berkas/kunci. Mengembalikan {@link java.util.List} berisi nilai non-kosong yang cocok.</p>
	 *
	 * @param parentFolder   folder sumber
	 * @param prefixNamaFile awalan nama berkas/kunci
	 * @return daftar nilai yang cocok (mungkin kosong)
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<String> ambilSemuaValueDenganPrefixFile(File parentFolder, String prefixNamaFile) {
		java.util.List<String> results = new java.util.ArrayList<String>();
		try {
			if (flagDataMenggunakandatabase) {
				String parentKey = sanitize(parentFolder != null ? parentFolder.getName() : "root");
				String filePrefix = sanitize(prefixNamaFile);
				String globalPrefixKey = getGlobalKey(parentKey, filePrefix);

				// 1. Jika FOLDER ini sudah pernah ditarik penuh dari DB ke Cache
				if (mulai && folderSudahDiLoad.contains(parentKey)) {
					for (java.util.Map.Entry<String, String> entry : MemoryDbUtil.getDataKey().entrySet()) {
						if (entry.getKey() != null && entry.getKey().startsWith(globalPrefixKey)) {
							String val = entry.getValue();
							if (val != null && !val.trim().isEmpty()) {
								results.add(val);
							}
						}
					}
					return results; 
				}

				// 2. Jika belum, Tarik SELURUH KONTEN FOLDER dari Database (Exact Match, Tanpa LIKE)
				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					ensureTableAndIndexExists(session);

					Query query = session.createSQLQuery(
							"SELECT flag_key, flag_value FROM ais_flags_data_baru " +
							"WHERE flag_key_parent_folder = :parent");
					query.setParameter("parent", parentKey);

					java.util.List<Object[]> list = query.list();
					if (list != null && !list.isEmpty()) {
						for (Object[] row : list) {
							String dbGlobalKey = (String) row[0];
							String dbValue = (String) row[1];

							if (dbValue != null && !dbValue.trim().isEmpty()) {
								// Filter hasil sesuai awalan (di level memori Java)
								if (dbGlobalKey.toLowerCase().startsWith(globalPrefixKey.toLowerCase())) {
									results.add(dbValue);
								}

								// Masukkan seluruh konten ke Cache untuk mengamankan akses berikutnya
								if (mulai) {
									MemoryDbUtil.getDataKey().put(dbGlobalKey, dbValue);
								}
							}
						}
					}

					if (mulai) {
						folderSudahDiLoad.add(parentKey);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BacaTulisUtil.java:653");
				} finally {
					cleanupSession(session);
				}

			} else {
				// Legacy Mode (File Fisik)
				if (parentFolder != null && parentFolder.exists() && parentFolder.isDirectory()) {
					File[] files = parentFolder.listFiles();
					if (files != null) {
						for (File f : files) {
							if (f.getName().toLowerCase().startsWith(prefixNamaFile.toLowerCase())) {
								String val = baca(f);
								if (val != null && !val.trim().isEmpty()) {
									results.add(val);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BacaTulisUtil.java:675");
		}
		return results;
	}

	// ===================================================================================
	// FLAG LANGSUNG KE TABEL ais_flags_data_baru (TANPA FILE / OBJECT MODEL)
	// Dipakai untuk penanda per-pengguna: info pembaruan tampilan, pengumuman
	// yang langsung tampil, dan flag ringan lain. Selalu menulis ke database
	// (native SQL) apa pun nilai flagDataMenggunakandatabase, dengan cache L1
	// MemoryDb bila cluster cache aktif.
	// ===================================================================================

	private static final String PARENT_FLAG_LANGSUNG = "flaglangsung";

	/**
	 * Membentuk kunci global untuk "flag langsung" (parent tetap {@link #PARENT_FLAG_LANGSUNG}).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> API flag langsung tidak berbasis berkas, jadi semua flag
	 * dikelompokkan di bawah satu parent virtual {@code "flaglangsung"}. Method ini menggabungkannya
	 * dengan {@code kunci} yang sudah di-{@link #sanitize(String)} menjadi {@code flaglangsung_<kunci>}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code kunci} = nama logis flag. Mengembalikan kunci global-nya.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Konsistensi parent virtual penting agar {@code bacaFlag}/{@code tulisFlag}/
	 * {@code hapusFlag} merujuk baris yang sama. Method privat.</p>
	 *
	 * @param kunci nama logis flag
	 * @return kunci global {@code flaglangsung_<kunci>}
	 */
	private static String globalKeyFlag(String kunci) {
		return getGlobalKey(PARENT_FLAG_LANGSUNG, sanitize(kunci));
	}

	/**
	 * Membaca nilai sebuah flag langsung dari tabel {@code ais_flags_data_baru} (dengan cache).
	 *
	 * <p><b>Tujuan.</b> API baca ringan untuk penanda per-pengguna (mis. "sudah lihat pengumuman",
	 * "perlu refresh tampilan") tanpa berkas/entity. Cepat karena mengutamakan cache RAM.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengembalikan {@code ""} bila {@code kunci} kosong. Membentuk
	 * {@link #globalKeyFlag(String)}, membaca dari cache ({@code MemoryDbUtil} bila kluster aktif,
	 * selain itu {@link #flagLocalCache}); bila kosong, baca DB via {@link #readFromDatabase(String)}
	 * lalu hangatkan cache.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code kunci} = nama flag. Mengembalikan nilainya, atau
	 * {@code ""} bila belum ada—tidak pernah null.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Exception dibungkus dan mengembalikan {@code ""}.
	 * Pertahankan konvensi non-null.</p>
	 *
	 * @param kunci nama flag
	 * @return nilai flag, atau {@code ""} bila belum ada
	 */
	public static String bacaFlag(String kunci) {
		if (kunci == null || kunci.trim().isEmpty()) {
			return "";
		}
		try {
			String globalKey = globalKeyFlag(kunci);
			String data = null;
			// Baca dari cache dulu (RAM, instan). mulai=true → MemoryDb (cluster-aware via
			// broadcast); mulai=false → cache lokal proses.
			if (mulai) {
				data = MemoryDbUtil.getDataKey().get(globalKey);
			} else {
				data = flagLocalCache.get(globalKey);
			}
			if (data == null) {
				data = readFromDatabase(globalKey);
				if (data != null) {
					if (mulai) {
						MemoryDbUtil.getDataKey().put(globalKey, data);
					} else {
						flagLocalCache.put(globalKey, data);
					}
				}
			}
			return data == null ? "" : data;
		} catch (Throwable e) {
			// MapDB IOError (Error, bukan Exception) tak boleh meruntuhkan pembacaan flag.
			tanganiKegagalanCache(e, "bacaFlag");
			return "";
		}
	}

	/**
	 * Menulis/menimpa nilai sebuah flag langsung ke tabel {@code ais_flags_data_baru} ("secepat kilat").
	 *
	 * <p><b>Tujuan.</b> API tulis ringan untuk penanda per-pengguna. Dirancang agar pemanggil tidak
	 * menunggu DB: nilai langsung terlihat dari cache, penulisan DB dilakukan asinkron (default).</p>
	 *
	 * <p><b>Cara kerja.</b> No-op bila {@code kunci} kosong. Memperbarui cache LEBIH DULU
	 * ({@code MemoryDbUtil} + broadcast "PUT" pada mode kluster, atau {@link #flagLocalCache}),
	 * kemudian {@link #saveToDatabase(String, String, String)} (dispatcher async/sinkron). Nilai null
	 * dinormalkan menjadi {@code ""}.</p>
	 *
	 * <p><b>Parameter.</b> {@code kunci} = nama flag; {@code nilai} = nilai yang ditulis (null → "").</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Dibungkus try/catch. Urutan "cache dulu, DB
	 * kemudian" adalah inti kecepatannya—jangan dibalik. Daya tahan saat shutdown dijamin
	 * {@link #flushPendingWrites()}.</p>
	 *
	 * @param kunci nama flag
	 * @param nilai nilai yang ditulis (null dianggap "")
	 */
	public static void tulisFlag(String kunci, String nilai) {
		if (kunci == null || kunci.trim().isEmpty()) {
			return;
		}
		try {
			String globalKey = globalKeyFlag(kunci);
			String val = nilai == null ? "" : nilai;
			// Update cache LEBIH DULU agar baca berikutnya seketika melihat nilai baru,
			// lalu simpan ke DB (asinkron/upsert) → caller tidak menunggu DB.
			if (mulai) {
				MemoryDbUtil.getDataKey().put(globalKey, val);
				MemoryCacheUtil.broadcastUpdate("data_temp", "PUT", globalKey, val);
			} else {
				flagLocalCache.put(globalKey, val);
			}
			saveToDatabase(PARENT_FLAG_LANGSUNG, globalKey, val);
		} catch (Throwable e) {
			// MapDB IOError (Error, bukan Exception) tak boleh meruntuhkan penulisan flag.
			tanganiKegagalanCache(e, "tulisFlag");
		}
	}

	/**
	 * Menghapus sebuah flag langsung dari cache, antrian tulis, dan tabel {@code ais_flags_data_baru}.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> No-op bila {@code kunci} kosong. Membentuk
	 * {@link #globalKeyFlag(String)}, MEMBATALKAN tulis tertunda untuk kunci itu
	 * ({@link #pendingWrites}.remove) dan membersihkan {@link #flagLocalCache}, lalu (mode kluster)
	 * menghapus dari MemoryDb + broadcast "REMOVE", dan akhirnya {@link #deleteFromDatabase(String)}.</p>
	 *
	 * <p><b>Parameter.</b> {@code kunci} = nama flag yang dihapus.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Dibungkus try/catch. Membatalkan tulis tertunda
	 * lebih dulu mencegah worker async menulis ulang nilai yang baru dihapus—pertahankan urutan ini.</p>
	 *
	 * @param kunci nama flag yang dihapus
	 */
	public static void hapusFlag(String kunci) {
		if (kunci == null || kunci.trim().isEmpty()) {
			return;
		}
		try {
			String globalKey = globalKeyFlag(kunci);
			// Batalkan tulis tertunda utk key ini agar tidak menulis ulang setelah dihapus.
			pendingWrites.remove(globalKey);
			flagLocalCache.remove(globalKey);
			if (mulai) {
				MemoryDbUtil.getDataKey().remove(globalKey);
				MemoryCacheUtil.broadcastUpdate("data_temp", "REMOVE", globalKey, null);
			}
			deleteFromDatabase(globalKey);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BacaTulisUtil.java:833");
		}
	}

	// ===================================================================================
	// HELPER NATIVE DATABASE & AUTO-MIGRATION (100% NATIVE SQL)
	// ===================================================================================

	/**
	 * Menyimpan data ke DATABASE atau ke BERKAS fisik sesuai mode penyimpanan aktif.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Memusatkan keputusan "tulis ke mana": bila
	 * {@link #flagDataMenggunakandatabase} true → {@link #saveToDatabase(String, String, String)};
	 * selain itu → tulis berkas via {@code FileUtils.writeStringToFile} (bila {@code file} tidak null).
	 * Dipakai {@link #tulis(File, String)}.</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code file}/{@code parentKey}/{@code globalKey}/
	 * {@code data} sesuai konteks tulis. Melempar {@code Exception} agar pemanggil dapat menangani
	 * kegagalan I/O. Method privat; satu-satunya tempat percabangan DB-vs-berkas untuk tulis.</p>
	 *
	 * @param file      berkas target (mode berkas)
	 * @param parentKey kunci parent (mode DB)
	 * @param globalKey kunci global (mode DB)
	 * @param data      nilai yang disimpan
	 * @throws Exception bila penulisan berkas/DB gagal
	 */
	private static void simpanDataFisikAtauDatabase(File file, String parentKey, String globalKey, String data) throws Exception {
		if (flagDataMenggunakandatabase) {
			saveToDatabase(parentKey, globalKey, data);
		} else {
			if (file != null) {
				FileUtils.writeStringToFile(file, data);
			}
		}
	}

	/**
	 * Membaca data dari DATABASE atau BERKAS fisik sesuai mode, dengan AUTO-MIGRASI berkas→DB.
	 *
	 * <p><b>Tujuan.</b> Memusatkan keputusan "baca dari mana" dan—penting—memindahkan data lama yang
	 * masih tersimpan sebagai berkas ke DB secara otomatis saat pertama dibaca dalam mode DB, sehingga
	 * migrasi penyimpanan berlangsung mulus tanpa skrip terpisah.</p>
	 *
	 * <p><b>Cara kerja.</b> Mode DB: baca via {@link #readFromDatabase(String)}; bila kosong TAPI
	 * berkas fisik masih ada, baca berkas lalu {@link #saveToDatabase(String, String, String)} (itulah
	 * auto-migrasinya). Mode berkas: baca langsung dari berkas bila ada.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code file}/{@code parentKey}/{@code globalKey} sesuai konteks
	 * baca. Mengembalikan nilai data, atau {@code null} bila tidak ditemukan di mana pun.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Melempar {@code Exception} (I/O). Method privat;
	 * auto-migrasi hanya berjalan bila berkas lama masih ada—setelah termigrasi, pembacaan berikut
	 * langsung dari DB.</p>
	 *
	 * @param file      berkas sumber (mode berkas / sumber migrasi)
	 * @param parentKey kunci parent (untuk simpan saat migrasi)
	 * @param globalKey kunci global yang dibaca
	 * @return nilai data, atau {@code null} bila tidak ada
	 * @throws Exception bila pembacaan berkas/DB gagal
	 */
	private static String ambilDataFisikAtauDatabase(File file, String parentKey, String globalKey) throws Exception {
		String data = null;

		if (flagDataMenggunakandatabase) {
			data = readFromDatabase(globalKey);

			// Auto-Migration
			if (data == null && file != null && file.exists()) {
				data = FileUtils.readFileToString(file);
				if (data != null && !data.isEmpty()) {
					saveToDatabase(parentKey, globalKey, data);
				}
			}
		} else {
			if (file != null && file.exists()) {
				data = FileUtils.readFileToString(file);
			}
		}

		return data;
	}

	/**
	 * Memastikan tabel {@code ais_flags_data_baru} dan indeksnya ADA, secara durable (idempoten,
	 * self-healing).
	 *
	 * <p><b>Tujuan.</b> Membuat skema sekali tanpa migrasi manual, sekaligus menghindari bug "relation
	 * does not exist" PERMANEN yang pernah terjadi. Tabel ber-PK tunggal {@code flag_key}, kolom
	 * {@code flag_key_parent_folder} (NOT NULL, ber-index), dan {@code flag_value TEXT}.</p>
	 *
	 * <h4>Cara kerja &amp; alasan desain (PENTING)</h4>
	 * <ul>
	 *   <li>Double-checked locking pada {@link #isTableInitialized} agar pembuatan hanya sekali.</li>
	 *   <li><b>DDL di SESSION &amp; TRANSAKSI TERPISAH yang di-commit sendiri</b>, BUKAN menumpang
	 *   transaksi pemanggil. Sebab di PostgreSQL DDL bersifat transaksional: bila CREATE TABLE
	 *   menumpang transaksi pemanggil lalu UPSERT-nya gagal &amp; rollback, CREATE TABLE ikut batal—
	 *   padahal flag terlanjur true → tabel tak pernah dibuat lagi.</li>
	 *   <li>Index dibuat di transaksi sendiri; kegagalannya tidak menggagalkan tabel (index hanya
	 *   optimasi).</li>
	 *   <li>Flag {@code isTableInitialized} di-set true HANYA bila
	 *   {@code SELECT CAST(to_regclass('ais_flags_data_baru') AS text)} membuktikan tabel benar-benar
	 *   ada (self-healing). Memakai {@code CAST(... AS text)} BUKAN {@code ::text}, karena {@code ::}
	 *   membuat Hibernate menyangka ada named-parameter {@code ":text"}; dan {@code to_regclass} harus
	 *   di-cast karena tipe regclass (JDBC 1111/OTHER) tak bisa dipetakan Hibernate.</li>
	 * </ul>
	 *
	 * <p><b>Parameter.</b> {@code sessionPemanggilDiabaikan} — parameter ini SENGAJA DIABAIKAN; method
	 * membuka session sendiri. Dipertahankan demi tanda tangan pemanggil lama.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Rollback + log bila gagal; session ditutup di
	 * {@code finally} via {@link #cleanupSession(Session)}. Jangan mengubah verifikasi {@code CAST}
	 * menjadi {@code ::text}, dan jangan menumpangkan DDL ke transaksi tulis—keduanya adalah inti
	 * perbaikan yang sudah teruji.</p>
	 *
	 * @param sessionPemanggilDiabaikan diabaikan (method membuka session sendiri)
	 */
	private static void ensureTableAndIndexExists(Session sessionPemanggilDiabaikan) {
		if (isTableInitialized) {
			return;
		}
		synchronized (BacaTulisUtil.class) {
			if (isTableInitialized) {
				return;
			}
			/* PENTING: DDL dibuat di SESSION & TRANSAKSI TERPISAH yang DI-COMMIT sendiri,
			 * JANGAN numpang transaksi pemanggil (saveToDatabaseSync/delete). Sebab di
			 * PostgreSQL DDL bersifat TRANSAKSIONAL: bila kita CREATE TABLE pada transaksi
			 * pemanggil lalu UPSERT-nya gagal & rollback, CREATE TABLE ikut dibatalkan —
			 * padahal flag isTableInitialized terlanjur true → tabel tak pernah ada lagi →
			 * "relation does not exist" permanen. Flag hanya di-set true bila to_regclass
			 * memastikan tabel benar-benar ada (self-healing). */
			Session s = null;
			Transaction tx = null;
			try {
				s = HibernateUtil.getSessionFactory().openSession();
				tx = s.beginTransaction();
				s.createSQLQuery("CREATE TABLE IF NOT EXISTS ais_flags_data_baru (" +
						"flag_key VARCHAR(150) NOT NULL, " +
						"flag_key_parent_folder VARCHAR(150) NOT NULL, " +
						"flag_value TEXT, " +
						"PRIMARY KEY (flag_key))"
				).executeUpdate();
				tx.commit();

				// Index opsional di transaksi sendiri — kegagalan index tak menggagalkan tabel.
				try {
					Transaction txIdx = s.beginTransaction();
					s.createSQLQuery(
							"CREATE INDEX IF NOT EXISTS idx_flags_parent ON ais_flags_data_baru(flag_key_parent_folder)")
							.executeUpdate();
					txIdx.commit();
				} catch (Exception eIdx) { ais.common.ErrorAuditUtil.record(eIdx, "auto-audit(empty-catch) src/ais/common/BacaTulisUtil.java:984");
					// abaikan — index hanya optimasi
				}

				// Set flag HANYA bila tabel terbukti ada. WAJIB CAST ke text — to_regclass
				// mengembalikan tipe regclass (JDBC type 1111/OTHER) yang TIDAK bisa dipetakan
				// Hibernate. Pakai CAST(... AS text), BUKAN '::text' karena '::' membuat
				// Hibernate menyangka ada named-parameter ":text".
				Object reg = s.createSQLQuery("SELECT CAST(to_regclass('ais_flags_data_baru') AS text)").uniqueResult();
				if (reg != null) {
					isTableInitialized = true;
				}
			} catch (Exception e) {
				if (tx != null) {
					try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/BacaTulisUtil.java:998");}
				}
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BacaTulisUtil.java:1000");
			} finally {
				cleanupSession(s);
			}
		}
	}

	/**
	 * Mengecek apakah penulisan asinkron diaktifkan lewat konfigurasi {@code bacatulis_async_write}.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Membaca konfigurasi (default {@code AKTIF}) dan
	 * mengembalikan {@code true} bila nilainya sama dengan {@code Konfigurasi.AKTIF}. Dipakai
	 * {@link #saveToDatabase(String, String, String)} untuk memilih jalur async vs sinkron.</p>
	 *
	 * <p><b>Return &amp; penanganan error.</b> Mengembalikan {@code true}/{@code false}; bila membaca
	 * konfigurasi gagal (mis. saat startup awal), mengembalikan {@code false} (fallback sinkron yang
	 * lebih aman/sederhana).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Method privat. Mengubah default ke nonaktif akan membuat semua tulis
	 * menjadi sinkron (lebih lambat tapi tanpa worker latar).</p>
	 *
	 * @return {@code true} bila tulis asinkron aktif
	 */
	private static boolean asyncWriteAktif() {
		try {
			return Common.getKonfigurasi("bacatulis_async_write", Konfigurasi.AKTIF).getNilai().trim()
					.equalsIgnoreCase(Konfigurasi.AKTIF);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Memastikan {@link #writeExecutor} (worker tulis tunggal) sudah dibuat, secara lazy &amp;
	 * thread-safe.
	 *
	 * <p><b>Tujuan.</b> Menyediakan SATU thread daemon ("bacatulis-async-writer") yang menyerialkan
	 * semua penulisan DB. Serialisasi inilah yang membuat tulisan BEBAS DEADLOCK antar-writer dan
	 * hemat koneksi pool.</p>
	 *
	 * <p><b>Cara kerja.</b> Double-checked locking pada {@link #WRITE_LOCK}; bila belum ada, membuat
	 * {@code Executors.newSingleThreadExecutor} dengan {@link java.util.concurrent.ThreadFactory}
	 * anonim yang menandai thread sebagai daemon (agar tidak menahan JVM shutdown).</p>
	 *
	 * <p><b>Pemeliharaan.</b> Method privat; jangan menambah ukuran pool—sifat single-thread adalah
	 * jaminan anti-deadlock-nya. Thread daemon disengaja; flush akhir ditangani
	 * {@link #flushPendingWrites()}.</p>
	 */
	private static void ensureWriteExecutor() {
		if (writeExecutor == null) {
			synchronized (WRITE_LOCK) {
				if (writeExecutor == null) {
					writeExecutor = java.util.concurrent.Executors
							.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory() {
								@Override
								public Thread newThread(Runnable r) {
									Thread t = new Thread(r, "bacatulis-async-writer");
									t.setDaemon(true);
									return t;
								}
							});
				}
			}
		}
	}

	/**
	 * Mengantrikan satu penulisan DB secara ASINKRON dengan COALESCING per-kunci (last-write-wins).
	 *
	 * <p><b>Tujuan.</b> Membuat tulis "secepat kilat" bagi pemanggil: nilai dicatat di
	 * {@link #pendingWrites} dan tugas dikirim ke worker tunggal, sehingga pemanggil tidak menunggu
	 * DB. Coalescing memastikan beberapa tulisan beruntun ke kunci yang sama cukup ditulis sekali ke
	 * DB (nilai terakhir yang menang).</p>
	 *
	 * <p><b>Cara kerja.</b> (1) {@code pendingWrites.put(globalKey, [parentKey, data])}—menimpa nilai
	 * tertunda sebelumnya untuk kunci itu (inilah coalescing). (2) Pastikan executor ada
	 * ({@link #ensureWriteExecutor()}). (3) Submit {@link Runnable} yang MENGAMBIL-dan-menghapus nilai
	 * terakhir dari {@code pendingWrites} lalu {@link #saveToDatabaseSync(String, String, String)}.
	 * Karena task membaca nilai terbaru saat dijalankan, task yang menumpuk untuk satu kunci otomatis
	 * tergabung.</p>
	 *
	 * <p><b>Penanganan error.</b> Bila executor menolak (mis. sudah di-shutdown), tulisan dikerjakan
	 * SINKRON sebagai cadangan agar data tidak hilang.</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code parentKey}/{@code globalKey}/{@code data} =
	 * muatan tulis. Method privat. Sifat "ambil nilai terbaru saat run" penting—jangan menangkap
	 * {@code data} mati di dalam task; ambil ulang dari {@code pendingWrites}.</p>
	 *
	 * @param parentKey kunci parent
	 * @param globalKey kunci global (PK)
	 * @param data      nilai yang ditulis
	 */
	private static void enqueueWrite(final String parentKey, final String globalKey, final String data) {
		pendingWrites.put(globalKey, new String[] { parentKey, data });
		ensureWriteExecutor();
		try {
			// Tugas men-DRAIN seluruh antrian dalam batch (bukan 1 baris/tugas). Karena worker
			// tunggal, tugas pertama menyapu semua yang pending; tugas berikutnya yang menumpuk
			// menemukan antrian kosong → cepat keluar. Ini coalescing alami antar-tugas.
			writeExecutor.submit(new Runnable() {
				@Override
				public void run() {
					drainAndFlushBatch();
				}
			});
		} catch (Exception e) {
			// Executor tak menerima (mis. sudah di-shutdown) → flush sinkron sebagai cadangan.
			drainAndFlushBatch();
		}
	}

	/**
	 * Mengosongkan {@link #pendingWrites} ke DB dalam beberapa BATCH (tiap batch maksimal
	 * {@link #WRITE_BATCH_MAX} entri dalam SATU transaksi). Dipanggil oleh worker async dan oleh
	 * {@link #flushPendingWrites()} saat shutdown.
	 *
	 * <p>Karena dipanggil hanya dari worker tunggal (atau saat shutdown), tidak ada dua drain
	 * berjalan paralel. Entri yang masuk selagi satu batch sedang ditulis akan ikut terambil pada
	 * iterasi berikutnya—tidak ada yang hilang. Coalescing tetap berlaku: nilai yang diambil adalah
	 * yang terakhir tercatat di {@code pendingWrites} saat di-drain (last-write-wins).</p>
	 */
	private static void drainAndFlushBatch() {
		while (!pendingWrites.isEmpty()) {
			// [globalKey, parentKey, value]
			java.util.List<String[]> batch = new java.util.ArrayList<String[]>();
			java.util.Iterator<String> it = pendingWrites.keySet().iterator();
			while (it.hasNext() && batch.size() < WRITE_BATCH_MAX) {
				String key = it.next();
				String[] v = pendingWrites.remove(key);
				if (v != null) {
					batch.add(new String[] { key, v[0], v[1] });
				}
			}
			if (batch.isEmpty()) {
				break;
			}
			saveBatchToDatabaseSync(batch);
		}
	}

	/**
	 * Menyimpan SEKUMPULAN entri ke DB dalam SATU transaksi memakai UPSERT atomik per baris, dengan
	 * retry. Bila batch gagal hingga batas retry, jatuh ke penulisan PER-ITEM
	 * ({@link #saveToDatabaseSync(String, String, String)}) agar tidak ada data hilang—mis. ketika
	 * hanya satu baris bermasalah, baris lain tetap tersimpan.
	 *
	 * @param batch daftar {@code [globalKey, parentKey, value]}
	 */
	private static void saveBatchToDatabaseSync(java.util.List<String[]> batch) {
		if (batch == null || batch.isEmpty()) {
			return;
		}
		// Jalur cepat: bila cuma 1 entri, pakai penyetor tunggal yang sudah teruji.
		if (batch.size() == 1) {
			String[] row = batch.get(0);
			saveToDatabaseSync(row[1], row[0], row[2]);
			return;
		}

		int maxRetries = 3;
		int currentAttempt = 0;
		boolean isSuccess = false;

		while (currentAttempt < maxRetries && !isSuccess) {
			Session session = null;
			Transaction tx = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				tx = session.beginTransaction();
				ensureTableAndIndexExists(session);

				for (int i = 0; i < batch.size(); i++) {
					String[] row = batch.get(i);
					Query up = session.createSQLQuery(
							"INSERT INTO ais_flags_data_baru (flag_key, flag_key_parent_folder, flag_value) "
									+ "VALUES (:key, :parent, :val) "
									+ "ON CONFLICT (flag_key) DO UPDATE SET flag_value = EXCLUDED.flag_value, "
									+ "flag_key_parent_folder = EXCLUDED.flag_key_parent_folder");
					up.setParameter("key", row[0]);
					up.setParameter("parent", row[1]);
					up.setParameter("val", row[2]);
					up.executeUpdate();
				}

				tx.commit();
				isSuccess = true;

			} catch (Exception e) {
				if (tx != null) {
					try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/BacaTulisUtil.java:1189");}
				}
				currentAttempt++;
				if (currentAttempt < maxRetries) {
					try {
						Thread.sleep(50 * currentAttempt);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				} else {
					// Batas retry habis → tulis PER-ITEM agar baris yang sehat tetap tersimpan
					// (durabilitas dipertahankan; tidak ada data yang hilang/terhapus).
					for (int i = 0; i < batch.size(); i++) {
						String[] row = batch.get(i);
						saveToDatabaseSync(row[1], row[0], row[2]);
					}
				}
			} finally {
				cleanupSession(session);
			}
		}
	}

	/**
	 * Menuliskan SEMUA sisa antrian tulis secara SINKRON—dipanggil saat shutdown aplikasi.
	 *
	 * <p><b>Tujuan.</b> Mencegah kehilangan data: karena tulis normal asinkron, saat aplikasi
	 * dimatikan bisa ada entri di {@link #pendingWrites} yang belum sempat ditulis worker. Method ini
	 * memflush semuanya ke DB secara langsung.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengiterasi kunci {@code pendingWrites}, mengambil-dan-menghapus tiap
	 * nilai, lalu {@link #saveToDatabaseSync(String, String, String)} untuk masing-masing.</p>
	 *
	 * <p><b>Kapan dipanggil &amp; pemeliharaan.</b> Idealnya dari
	 * {@code AppStartupListener.contextDestroyed}. Bersifat publik agar dapat dipicu listener
	 * shutdown. Aman dipanggil meski antrian kosong (tidak melakukan apa-apa). Pastikan listener
	 * shutdown benar-benar memanggilnya agar jaminan "tidak ada flag hilang" berlaku.</p>
	 */
	public static void flushPendingWrites() {
		// Pakai drain berbatch yang sama agar sisa antrian saat shutdown disetor cepat
		// (beberapa UPSERT per transaksi), dengan fallback per-item bila batch gagal.
		drainAndFlushBatch();
	}

	/**
	 * Dispatcher penyimpanan DB: memilih jalur ASINKRON (default) atau SINKRON sesuai konfigurasi.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> No-op bila {@code globalKey} kosong. Bila
	 * {@link #asyncWriteAktif()} true, mengantre via {@link #enqueueWrite(String, String, String)};
	 * selain itu menulis langsung via {@link #saveToDatabaseSync(String, String, String)}. Ini
	 * memisahkan keputusan "bagaimana menulis" dari pemanggil ({@link #tulisFlag(String, String)},
	 * {@link #simpanDataFisikAtauDatabase(File, String, String, String)}).</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code parentKey}/{@code globalKey}/{@code data} =
	 * muatan tulis. Method privat. Satu-satunya titik percabangan async/sinkron.</p>
	 *
	 * @param parentKey kunci parent
	 * @param globalKey kunci global (PK)
	 * @param data      nilai yang disimpan
	 */
	private static void saveToDatabase(String parentKey, String globalKey, String data) {
		if (globalKey == null || globalKey.isEmpty()) return;
		if (asyncWriteAktif()) {
			enqueueWrite(parentKey, globalKey, data);
		} else {
			saveToDatabaseSync(parentKey, globalKey, data);
		}
	}

	/**
	 * Menyimpan satu entri ke DB secara SINKRON memakai UPSERT ATOMIK, dengan retry singkat.
	 *
	 * <p><b>Tujuan.</b> Penulis DB sebenarnya—dipakai baik oleh worker async maupun jalur sinkron.
	 * Menggantikan pola lama "UPDATE-lalu-INSERT" (dua round-trip, rawan race/duplicate-key/DEADLOCK)
	 * dengan satu pernyataan atomik.</p>
	 *
	 * <p><b>Cara kerja.</b> No-op bila {@code globalKey} kosong. Hingga 3 percobaan: buka session +
	 * transaksi, pastikan tabel ada ({@link #ensureTableAndIndexExists(Session)}), jalankan
	 * {@code INSERT ... VALUES (:key,:parent,:val) ON CONFLICT (flag_key) DO UPDATE SET flag_value =
	 * EXCLUDED.flag_value, flag_key_parent_folder = EXCLUDED.flag_key_parent_folder} (PostgreSQL 9.5+),
	 * lalu commit. Bila gagal, rollback dan—jika masih ada sisa percobaan—tidur singkat
	 * ({@code 50ms * percobaan}, backoff) sebelum mencoba lagi; bila habis, log error.</p>
	 *
	 * <p><b>Higiene koneksi &amp; konkurensi.</b> Session ditutup di {@code finally} via
	 * {@link #cleanupSession(Session)}. Interupsi saat tidur menyetel ulang flag interrupt thread.
	 * Karena pada mode async hanya worker tunggal yang memanggil ini, kontensi minimal; retry tetap
	 * disediakan untuk transien (mis. lock sesaat).</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code parentKey}/{@code globalKey}/{@code data}.
	 * UPSERT bergantung pada PK {@code flag_key} dan PostgreSQL ≥9.5—jangan kembali ke pola
	 * UPDATE-lalu-INSERT. Method privat.</p>
	 *
	 * @param parentKey kunci parent
	 * @param globalKey kunci global (PK)
	 * @param data      nilai yang disimpan
	 */
	private static void saveToDatabaseSync(String parentKey, String globalKey, String data) {
		if (globalKey == null || globalKey.isEmpty()) return;

		int maxRetries = 3;
		int currentAttempt = 0;
		boolean isSuccess = false;

		while (currentAttempt < maxRetries && !isSuccess) {
			Session session = null;
			Transaction tx = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				tx = session.beginTransaction();
				ensureTableAndIndexExists(session);

				// UPSERT ATOMIK (PostgreSQL 9.5+): satu statement menggantikan pola
				// UPDATE-lalu-INSERT yang rawan race/duplicate-key/DEADLOCK & dua round-trip.
				Query up = session.createSQLQuery(
						"INSERT INTO ais_flags_data_baru (flag_key, flag_key_parent_folder, flag_value) "
								+ "VALUES (:key, :parent, :val) "
								+ "ON CONFLICT (flag_key) DO UPDATE SET flag_value = EXCLUDED.flag_value, "
								+ "flag_key_parent_folder = EXCLUDED.flag_key_parent_folder");
				up.setParameter("key", globalKey);
				up.setParameter("parent", parentKey);
				up.setParameter("val", data);
				up.executeUpdate();

				tx.commit();
				isSuccess = true;

			} catch (Exception e) {
				if (tx != null) {
					try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/BacaTulisUtil.java:1317");}
				}

				currentAttempt++;
				if (currentAttempt < maxRetries) {
					try {
						Thread.sleep(50 * currentAttempt);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				} else {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BacaTulisUtil.java:1328");
				}
			} finally {
				cleanupSession(session);
			}
		}
	}

	/**
	 * Membaca nilai sebuah entri dari DB berdasarkan {@code flag_key}.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> No-op (null) bila {@code globalKey} kosong. Membuka session,
	 * memastikan tabel ada, lalu {@code SELECT flag_value FROM ais_flags_data_baru WHERE flag_key =
	 * :key} dengan {@code uniqueResult()}. Hasil dikonversi ke string. Dipakai sebagai sumber
	 * kebenaran saat cache kosong oleh {@link #baca(File)} dan {@link #bacaFlag(String)}.</p>
	 *
	 * <p><b>Higiene koneksi &amp; error.</b> Session ditutup di {@code finally} via
	 * {@link #cleanupSession(Session)}. Exception di-log dan mengembalikan {@code null}.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code globalKey} = PK entri. Mengembalikan nilai atau
	 * {@code null} bila tidak ada/gagal.</p>
	 *
	 * @param globalKey kunci global (PK) yang dibaca
	 * @return nilai entri, atau {@code null}
	 */
	private static String readFromDatabase(String globalKey) {
		if (globalKey == null || globalKey.isEmpty()) return null;

		Session session = null;
		String result = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			ensureTableAndIndexExists(session);

			Query query = session.createSQLQuery(
					"SELECT flag_value FROM ais_flags_data_baru WHERE flag_key = :key");
			query.setParameter("key", globalKey);
			
			Object res = query.uniqueResult();
			if (res != null) {
				result = res.toString();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BacaTulisUtil.java:1371");
		} finally {
			cleanupSession(session);
		}
		return result;
	}

	/**
	 * Menghapus satu entri dari DB berdasarkan {@code flag_key}.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> No-op bila {@code globalKey} kosong. Membuka session +
	 * transaksi, memastikan tabel ada, menjalankan {@code DELETE ... WHERE flag_key = :key}, lalu
	 * commit. Dipakai oleh {@link #hapus(File)} dan {@link #hapusFlag(String)}.</p>
	 *
	 * <p><b>Higiene koneksi &amp; error.</b> Rollback bila gagal; session ditutup di {@code finally}
	 * via {@link #cleanupSession(Session)}. Exception di-log.</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code globalKey} = PK entri yang dihapus. Method privat.</p>
	 *
	 * @param globalKey kunci global (PK) yang dihapus
	 */
	private static void deleteFromDatabase(String globalKey) {
		if (globalKey == null || globalKey.isEmpty()) return;

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			ensureTableAndIndexExists(session);

			Query query = session.createSQLQuery("DELETE FROM ais_flags_data_baru WHERE flag_key = :key");
			query.setParameter("key", globalKey);
			query.executeUpdate();

			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/BacaTulisUtil.java:1409");}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BacaTulisUtil.java:1411");
		} finally {
			cleanupSession(session);
		}
	}

	/**
	 * Menghapus dari DB semua entri dalam satu parent-folder yang kuncinya berawalan tertentu.
	 *
	 * <p><b>Tujuan.</b> Mendukung penghapusan massal berbasis awalan ({@link #doHapus(File, String)})
	 * pada sisi database.</p>
	 *
	 * <p><b>Cara kerja.</b> No-op bila parameter kosong. Membuka session + transaksi, memastikan tabel
	 * ada, lalu mengambil SEMUA {@code flag_key} pada parent itu (EXACT match
	 * {@code flag_key_parent_folder = :parent}, memanfaatkan index). Penyaringan awalan
	 * {@code <parent>_<prefix>} dilakukan di memori Java (case-insensitive); kunci yang cocok dihapus
	 * satu per satu memakai query DELETE yang sama (parameter diset ulang per iterasi). Akhirnya
	 * commit.</p>
	 *
	 * <p><b>Mengapa saring di memori.</b> Konsisten dengan {@link #ambilSemuaValueDenganPrefixFile}:
	 * exact-match parent pada kolom ber-index lebih murah, dan penyaringan awalan halus murah di RAM.</p>
	 *
	 * <p><b>Higiene koneksi &amp; error.</b> Rollback bila gagal; session ditutup di {@code finally}.
	 * Exception di-log.</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code parentKey} = folder; {@code filePrefix} = awalan
	 * nama berkas/kunci. Method privat. Untuk volume sangat besar dalam satu folder, pertimbangkan
	 * DELETE berbasis pola di DB—namun pola saat ini menjaga kesederhanaan &amp; konsistensi.</p>
	 *
	 * @param parentKey  kunci parent (folder)
	 * @param filePrefix awalan nama berkas/kunci yang dihapus
	 */
	@SuppressWarnings("unchecked")
	private static void deletePrefixFromDatabase(String parentKey, String filePrefix) {
		if (parentKey == null || filePrefix == null || parentKey.isEmpty() || filePrefix.isEmpty()) return;

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			ensureTableAndIndexExists(session);

			String globalPrefixKey = getGlobalKey(parentKey, filePrefix);

			// Ambil semua Single Key di Parent (Exact Match ke Parent Folder)
			Query fetchQuery = session.createSQLQuery(
					"SELECT flag_key FROM ais_flags_data_baru WHERE flag_key_parent_folder = :parent");
			fetchQuery.setParameter("parent", parentKey);
			
			java.util.List<String> allKeysInParent = fetchQuery.list();
			
			if (allKeysInParent != null && !allKeysInParent.isEmpty()) {
				Query deleteQuery = session.createSQLQuery("DELETE FROM ais_flags_data_baru WHERE flag_key = :key");
				
				for (String key : allKeysInParent) {
					// Saring awalan di memori Java
					if (key.toLowerCase().startsWith(globalPrefixKey.toLowerCase())) {
						deleteQuery.setParameter("key", key);
						deleteQuery.executeUpdate();
					}
				}
			}

			tx.commit();
			
		} catch (Exception e) {
			if (tx != null) {
				try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/BacaTulisUtil.java:1479");}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BacaTulisUtil.java:1481");
		} finally {
			cleanupSession(session);
		}
	}

	// ===================================================================================
	// PEMBERSIH SESSION MEMORI KETAT (CLEANUP)
	// ===================================================================================
	/**
	 * Menutup sebuah session DB secara aman dan ketat (clear → disconnect → close).
	 *
	 * <p><b>Tujuan.</b> Memastikan setiap session yang dibuka method-method native di class ini
	 * dikembalikan ke pool tanpa kebocoran. Karena class ini memakai {@code openSession()} (bukan
	 * session ZK), penutupan adalah tanggung jawab sendiri.</p>
	 *
	 * <p><b>Cara kerja.</b> Bila {@code session} tidak null: {@code clear()} (buang objek persisten),
	 * {@code disconnect()}, lalu {@code close()}. Setiap langkah dibungkus try/catch independen
	 * sehingga kegagalan satu langkah tidak menghentikan yang lain—prinsip "apa pun terjadi, lepaskan
	 * koneksi".</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code session} = session yang ditutup (boleh null).
	 * Method privat, dipanggil di {@code finally} semua helper DB. Untuk higiene transaksi yang lebih
	 * lengkap (rollback sebelum close) lihat {@code HibernateUtil.closeSessionQuietly}; di sini cukup
	 * karena transaksi sudah di-commit/rollback eksplisit oleh pemanggil.</p>
	 *
	 * @param session session yang ditutup (boleh null)
	 */
	private static void cleanupSession(Session session) {
		if (session != null) {
			try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BacaTulisUtil.java:1511");}
			try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BacaTulisUtil.java:1512");}
			try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BacaTulisUtil.java:1513");}
		}
	}
}