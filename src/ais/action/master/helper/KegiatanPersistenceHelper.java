package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VOMahasiswa;
import ais.ui.util.WaktuUtil;

/**
 * Helper persistence Kegiatan.
 *
 * Fokus class ini:
 * - Mengambil dan menyinkronkan daftar DetailKegiatan/CicilanPembayaran.
 * - Membangun ulang field denormalisasi Kegiatan: bulans, tagihans, tagihan,
 *   dibayar, persentase, cicilans, dan detailKegiatans.
 * - Menjaga agar update tidak membebani UI melalui antrean async yang lebih
 *   kecil dan aman.
 *
 * Catatan session:
 * - Session yang dibuat dengan openSession() selalu ditutup di finally.
 * - currentSession() tidak ditutup oleh helper ini.
 */
public class KegiatanPersistenceHelper {

	/**
	 * HQL bulk-update yang menulis KETUJUH kolom denormalisasi {@link Kegiatan} sekaligus dalam
	 * satu pernyataan: {@code bulans}, {@code tagihans}, {@code tagihan}, {@code dibayar},
	 * {@code persentase}, {@code cicilans}, dan {@code detailKegiatans}.
	 *
	 * <p>Sengaja memakai bulk-update HQL, bukan {@code session.update(entity)}, agar hanya ketujuh
	 * kolom ini yang tersentuh — kolom lain pada baris {@code Kegiatan} yang sedang diubah proses
	 * interaktif lain tidak ikut tertimpa oleh salinan entity milik worker async. Konsekuensinya
	 * bulk-update MELEWATI cache level-2 dan interceptor Hibernate, sehingga pemanggil yang masih
	 * memegang instance {@link Kegiatan} lama harus disinkronkan manual — itulah sebabnya
	 * {@link #eksekusiUpdateDenganRetryTerkunci} menyalin balik nilai hasil hitung ke object
	 * {@code kegiatan} milik pemanggil.</p>
	 */
	private static final String HQL_UPDATE_KEGIATAN = "UPDATE Kegiatan SET bulans = :nilaiBaru, "
			+ "tagihans = :nilaiTagihanBaru, tagihan = :tagihanBaru, dibayar = :dibayarBaru, "
			+ "persentase = :persentaseBaru, cicilans = :cicilansBaru, "
			+ "detailKegiatans = :detailKegiatansBaru WHERE id = :idKegiatan";

	/**
	 * Batas jumlah id per klausa {@code IN (...)} saat memuat cicilan/detail kegiatan
	 * ({@link #loadCicilanByIds}, {@link #loadDetailKegiatanByIds}). Daftar id yang lebih panjang
	 * dipecah menjadi beberapa query berurutan; nilainya dijaga di bawah batas parameter bind
	 * PostgreSQL sekaligus menghindari rencana eksekusi yang buruk untuk daftar sangat panjang.
	 */
	private static final int MAX_IN_CLAUSE_SIZE = 800;
	/**
	 * Jeda debounce antrean async: perubahan dijadwalkan {@value} detik ke depan
	 * ({@link #simpanPerubahanAsync}). Selama jeda ini, perubahan berikutnya pada kegiatan yang
	 * sama hanya MEMPERBARUI isi tugas yang sudah antre alih-alih menjadwalkan tugas baru,
	 * sehingga rentetan penyuntingan di UI menghasilkan satu penulisan basis data, bukan puluhan.
	 */
	private static final int ASYNC_DELAY_SECONDS = 15;
	/**
	 * Jumlah maksimum percobaan penulisan pada {@link #eksekusiUpdateDenganRetryTerkunci} sebelum
	 * kegagalan dilaporkan lewat {@code Common.tampilErrorJikaAdmin}. Antar percobaan diberi
	 * backoff eksponensial ringan berjitter untuk memberi waktu lock basis data terlepas.
	 */
	private static final int MAX_RETRY = 5;

	/**
	 * Pool thread terjadwal yang menjalankan seluruh penulisan denormalisasi tertunda. Ukurannya
	 * dibatasi {@link #hitungThreadPoolAman(int)} (maksimum 8) agar tidak menghabiskan koneksi
	 * kolam c3p0 saat sinkronisasi massal. Seluruh thread-nya DAEMON, sehingga JVM/container tetap
	 * bisa berhenti walau masih ada tugas terjadwal.
	 *
	 * <p><b>Konsekuensi daemon:</b> tugas yang masih menunggu jeda {@link #ASYNC_DELAY_SECONDS}
	 * saat aplikasi dimatikan akan HILANG tanpa tertulis ke basis data. Kolom denormalisasi bersifat
	 * turunan dan dapat dibangun ulang lewat "Hitung Ulang", jadi kehilangan ini tidak merusak data
	 * sumber — tetapi angka dasbor bisa basi sampai perhitungan ulang berikutnya. Pemanggil yang
	 * butuh kepastian tulis memakai {@code immediateUpdate=true} pada
	 * {@link #simpanPerubahanAsync}.</p>
	 */
	private static final ScheduledExecutorService asyncExecutor = Executors.newScheduledThreadPool(
			hitungThreadPoolAman(Runtime.getRuntime().availableProcessors()), new ThreadFactory() {
				/**
				 * Membuat thread worker bernama {@code AsyncUpdateKegiatan-Thread-<id>} dan menandainya sebagai
				 * DAEMON. Penamaan eksplisit memudahkan mengenali worker ini pada thread dump saat menelusuri
				 * kontensi lock basis data.
				 *
				 * @param runnable tugas penulisan denormalisasi yang akan dijalankan.
				 * @return thread daemon siap pakai untuk {@link #asyncExecutor}.
				 */
				@Override
				public Thread newThread(Runnable runnable) {
					Thread thread = new Thread(runnable);
					thread.setDaemon(true);
					thread.setName("AsyncUpdateKegiatan-Thread-" + thread.getId());
					return thread;
				}
			});

	/**
	 * Peta id {@link Kegiatan} ke tugas penulisan yang sedang antre — inti mekanisme debounce.
	 * Selama sebuah id masih terdaftar di sini dengan {@code future} yang belum selesai, perubahan
	 * baru hanya menimpa isi {@link PendingKegiatanData} alih-alih menjadwalkan tugas kedua.
	 *
	 * <p>Meski bertipe {@link ConcurrentHashMap}, SELURUH akses baca-ubah-tulis di kelas ini
	 * dibungkus {@code synchronized (pendingTasks)} karena operasinya majemuk (cek {@code future},
	 * batalkan, lalu ganti entri) dan tidak dapat diatomkan oleh {@code ConcurrentHashMap} sendiri.
	 * Satu-satunya akses tanpa blok {@code synchronized} adalah {@code remove(id)} di dalam badan
	 * tugas terjadwal, yang memang aman karena bersifat atomik tunggal.</p>
	 */
	private static final ConcurrentHashMap<Long, PendingKegiatanData> pendingTasks = new ConcurrentHashMap<Long, PendingKegiatanData>();
	/**
	 * Array 1024 monitor untuk penguncian ber-stripe per {@link Kegiatan} (lihat
	 * {@link #buatKegiatanLocks()} dan {@link #getKegiatanLock(Long)}). Menjamin dua penulisan
	 * denormalisasi untuk kegiatan yang SAMA berjalan serial di dalam satu JVM; kegiatan berbeda
	 * yang kebetulan sama modulo 1024 ikut terserialisasi (tabrakan palsu yang ditoleransi).
	 *
	 * <p>Penguncian lintas node JVM ditangani terpisah oleh {@code pg_advisory_xact_lock} di dalam
	 * transaksi {@link #eksekusiUpdateDenganRetryTerkunci}.</p>
	 */
	private static final Object[] kegiatanLocks = buatKegiatanLocks();

	/**
	 * Tipe implementasi bersarang {@link PendingKegiatanData} milik {@link KegiatanPersistenceHelper}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * KegiatanPersistenceHelper}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
	 * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code ScheduledFuture future}, {@code
	 * String cicilans}, {@code String detailKegiatans}, {@code Kegiatan kegiatan}. Aturan bisnis bersama tetap
	 * berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see KegiatanPersistenceHelper
	 */
	private static class PendingKegiatanData {
		private ScheduledFuture<?> future;
		private String cicilans;
		private String detailKegiatans;
		private Kegiatan kegiatan;

		private PendingKegiatanData(String cicilans, String detailKegiatans, Kegiatan kegiatan) {
			this.cicilans = cicilans;
			this.detailKegiatans = detailKegiatans;
			this.kegiatan = kegiatan;
		}
	}

	/**
	 * Tipe implementasi bersarang {@link RekapPembayaran} milik {@link KegiatanPersistenceHelper}. Kelas ini
	 * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * KegiatanPersistenceHelper}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
	 * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String bulans}, {@code Double
	 * dibayar}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 *
	 * @see KegiatanPersistenceHelper
	 */
	private static class RekapPembayaran {
		private String bulans = "{}";
		private Double dibayar = Double.valueOf(0.0);
	}

	/**
	 * Menghitung ukuran thread pool yang aman: minimal 1, maksimal 8, dan tidak pernah melebihi
	 * jumlah prosesor yang tersedia maupun {@code totalData}. Batas atas 8 dipilih agar pekerjaan
	 * paralel tidak menghabiskan kolam koneksi basis data.
	 *
	 * <p>Bersifat publik karena juga dipakai pemanggil lain (mis. proses tagihan massal) yang
	 * perlu menghitung ukuran pool dengan aturan yang sama.</p>
	 *
	 * @param totalData banyaknya data yang akan diproses; nilai {@code <= 0} berarti "tidak
	 *                  diketahui" sehingga tidak ikut membatasi.
	 * @return ukuran pool antara 1 dan 8.
	 */
	public static int hitungThreadPoolAman(int totalData) {
		int processor = Runtime.getRuntime().availableProcessors();
		int batasProcessor = processor <= 0 ? 2 : processor;
		int jumlah = Math.min(8, Math.max(1, batasProcessor));
		if (totalData > 0) {
			jumlah = Math.min(jumlah, totalData);
		}
		return Math.max(1, jumlah);
	}

	/**
	 * Menutup {@link Session} hasil {@code openSession()} dengan urutan aman:
	 * {@code clear()} → {@code disconnect()} → {@code close()}, masing-masing dibungkus penangkap
	 * kesalahan sendiri sehingga satu langkah yang gagal tidak menghalangi langkah berikutnya.
	 * {@code clear()} dipanggil lebih dulu supaya entity yang masih menempel dilepas dan tidak
	 * ikut ter-flush secara tidak sengaja saat penutupan.
	 *
	 * <p><b>Jangan</b> memakai method ini untuk {@code HibernateUtil.currentSession()} — session
	 * milik request itu dikelola di luar kelas ini dan menutupnya akan merusak pemanggil. Aman
	 * dipanggil dengan {@code null} maupun session yang sudah tertutup.</p>
	 *
	 * @param session session yang akan ditutup; boleh {@code null}.
	 */
	public static void closeOpenedSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:115");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:119");
				}
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:125");
		}
	}

	/**
	 * Menutup session hasil {@code openSession()} lewat {@link #closeOpenedSession(Session)} DAN
	 * sekaligus melepas session milik thread saat ini ({@code HibernateUtil.closeSession()}).
	 *
	 * <p>Dipakai pada jalur yang berjalan di luar siklus request web (mis. worker batch) di mana
	 * session per-thread juga harus dibersihkan agar tidak bocor ke tugas berikutnya yang memakai
	 * thread yang sama. Pada jalur request web biasa gunakan {@link #closeOpenedSession(Session)}
	 * saja.</p>
	 *
	 * @param session session hasil {@code openSession()}; boleh {@code null}.
	 */
	public static void closeNativeSession(Session session) {
		closeOpenedSession(session);
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:133");
		}
	}

	/**
	 * Melakukan {@code rollback()} bila transaksi masih aktif, dan menelan kegagalan rollback itu
	 * sendiri (dicatat ke {@code ErrorAuditUtil}). Dipakai di blok {@code catch} agar kesalahan
	 * asli yang memicu rollback tidak tertutupi oleh kesalahan sekunder saat rollback.
	 *
	 * @param tx transaksi yang akan di-rollback; boleh {@code null} atau sudah tidak aktif.
	 */
	private static void rollbackQuietly(Transaction tx) {
		if (tx != null) {
			try {
				if (tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:143");
			}
		}
	}

	/**
	 * Mengembalikan {@code true} bila {@code value} {@code null} atau hanya berisi spasi.
	 *
	 * @param value teks yang diperiksa.
	 * @return {@code true} bila kosong atau {@code null}.
	 */
	private static boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	/**
	 * Mengubah {@code null} menjadi {@code 0.0} agar aritmetika di kelas ini tidak perlu
	 * memeriksa null berulang kali.
	 *
	 * @param value nilai yang mungkin {@code null}.
	 * @return {@code value}, atau {@code 0.0} bila {@code null}.
	 */
	private static Double safeDouble(Double value) {
		return value == null ? Double.valueOf(0.0) : value;
	}

	/**
	 * Memotong (bukan membulatkan) sebuah {@link Double} menjadi {@code long}, dengan {@code null}
	 * dipetakan ke {@code 0}. Dipakai saat menuliskan nominal ke JSON {@code bulans}.
	 *
	 * <p>Perhatikan perbedaannya dengan {@link #nominalTagihan(Double)} yang MEMBULATKAN
	 * ({@code Math.round}). Rekap pembayaran memotong, rekap tagihan membulatkan.</p>
	 *
	 * @param value nilai yang mungkin {@code null}.
	 * @return bagian bulat dari {@code value}, atau {@code 0}.
	 */
	private static long safeLong(Double value) {
		return value == null ? 0L : value.longValue();
	}

	/**
	 * Mengambil id sebuah entity secara generik. Menggunakan jalur cepat bertipe untuk
	 * {@link CicilanPembayaran} dan {@link DetailKegiatan}, lalu jatuh ke refleksi
	 * ({@code getId()}) untuk tipe lain. Kegagalan refleksi dicatat dan menghasilkan {@code null}.
	 *
	 * <p>Dibutuhkan karena {@link #bangunStringAktif(java.util.List)} bekerja pada {@code List}
	 * mentah yang isinya bisa cicilan maupun detail kegiatan.</p>
	 *
	 * @param object entity yang akan diambil id-nya; boleh {@code null}.
	 * @return id sebagai {@link Long}, atau {@code null} bila tidak tersedia.
	 */
	private static Long getId(Object object) {
		if (object == null) {
			return null;
		}
		if (object instanceof CicilanPembayaran) {
			return ((CicilanPembayaran) object).getId();
		}
		if (object instanceof DetailKegiatan) {
			return ((DetailKegiatan) object).getId();
		}
		try {
			Object value = object.getClass().getMethod("getId", new Class[0]).invoke(object, new Object[0]);
			if (value instanceof Number) {
				return Long.valueOf(((Number) value).longValue());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:175");
		}
		return null;
	}

	// ========================================================================
	// 1. PENGAMBILAN DETAIL KEGIATAN DAN CICILAN
	// ========================================================================

	/**
	 * Mengambil seluruh {@link DetailKegiatan} milik satu {@link Kegiatan} <b>dan sekaligus
	 * menyinkronkan kolom denormalisasi</b> {@code Kegiatan.detailKegiatans}.
	 *
	 * <p><b>Method ini MENULIS ke basis data meskipun namanya terdengar seperti pembacaan.</b>
	 * Setelah daftar terkumpul, {@link #updateDetailKegiatan(java.util.List, Kegiatan, boolean)}
	 * dipanggil dan menjadwalkan penulisan lewat antrean async. Untuk kebutuhan laporan atau
	 * render berulang yang harus bebas efek samping, gunakan
	 * {@link #ambilDetailKegiatanReadOnly(Kegiatan, boolean)}.</p>
	 *
	 * @param kegiatan kegiatan pemilik detail; {@code null}/tanpa id menghasilkan daftar kosong.
	 * @param refresh  bila {@code true}, paksa query ke basis data dan abaikan id aktif yang
	 *                 sudah ter-cache pada object {@code kegiatan}.
	 * @return daftar detail kegiatan; tidak pernah {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public static List<DetailKegiatan> ambilDetailKegiatanSaja(Kegiatan kegiatan, boolean refresh) {
		return ambilDetailKegiatanInternal(kegiatan, refresh, true);
	}

	/**
	 * Varian baca-saja untuk laporan. Mengambil data terkini tanpa mengubah kolom
	 * denormalisasi Kegiatan dan tanpa menjadwalkan worker persistence.
	 */
	public static List<DetailKegiatan> ambilDetailKegiatanReadOnly(Kegiatan kegiatan, boolean refresh) {
		return ambilDetailKegiatanInternal(kegiatan, refresh, false);
	}

	/**
	 * Implementasi bersama {@link #ambilDetailKegiatanSaja(Kegiatan, boolean)} dan
	 * {@link #ambilDetailKegiatanReadOnly(Kegiatan, boolean)}.
	 *
	 * <p><b>Sumber id</b> ditentukan {@code refresh}: bila {@code false}, id diambil dari cache
	 * {@code kegiatan.ambilDetailKegiatansAktifIds()}; query basis data baru dijalankan bila cache
	 * kosong DAN kegiatan belum pernah menjalankan operasi ini ({@code udah(...)}). Bila
	 * {@code true}, query selalu dijalankan.
	 *
	 * <p><b>Retry koneksi.</b> Query dibungkus perulangan dua percobaan: kegagalan yang dikenali
	 * {@link #isConnectionFailure(Throwable)} pada percobaan pertama memicu satu kali pengulangan
	 * dengan session baru (koneksi c3p0 basi adalah penyebab lazim); kegagalan jenis lain langsung
	 * dilempar. Perhatikan bahwa blok {@code finally} di dalam perulangan sudah menutup session
	 * tiap percobaan, dan {@code closeOpenedSession} di {@code finally} terluar bersifat
	 * idempoten.</p>
	 *
	 * @param kegiatan          kegiatan pemilik detail.
	 * @param refresh           paksa baca dari basis data.
	 * @param sinkronkanKegiatan bila {@code true}, tulis balik kolom denormalisasi lewat antrean
	 *                           async; bila {@code false}, murni baca.
	 * @return daftar detail kegiatan; tidak pernah {@code null}.
	 */
	@SuppressWarnings("unchecked")
	private static List<DetailKegiatan> ambilDetailKegiatanInternal(Kegiatan kegiatan, boolean refresh,
			boolean sinkronkanKegiatan) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return new ArrayList<DetailKegiatan>();
		}

		TreeSet<Long> keyData = new TreeSet<Long>();

		if (!refresh) {
			List<Long> aktifIds = kegiatan.ambilDetailKegiatansAktifIds();
			if (aktifIds != null && !aktifIds.isEmpty()) {
				keyData.addAll(aktifIds);
			}
		}

		Session session = null;
		try {
			if (refresh || (keyData.isEmpty() && !kegiatan.udah("ambilDetailKegiatanSaja"))) {
				Exception last = null;
				for (int attempt = 0; attempt < 2; attempt++) {
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						Criteria criteria = session.createCriteria(DetailKegiatan.class);
						criteria.setProjection(Projections.property("id"));
						criteria.add(Restrictions.eq("kegiatan", kegiatan));
						criteria.addOrder(Order.asc("id"));
						criteria.setTimeout(600);
						List<Long> dbKeys = criteria.list();
						if (dbKeys != null && !dbKeys.isEmpty()) keyData.addAll(dbKeys);
						last = null;
						break;
					} catch (Exception queryError) {
						last = queryError;
						if (!isConnectionFailure(queryError) || attempt > 0) throw queryError;
					} finally {
						closeOpenedSession(session);
						session = null;
					}
				}
				if (last != null) throw last;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}

		List<DetailKegiatan> hasilDetailKegiatan = new ArrayList<DetailKegiatan>();
		if (!keyData.isEmpty()) {
			hasilDetailKegiatan = GeneralValueObject.ambilDataBanyak(DetailKegiatan.class, new ArrayList<Long>(keyData),
					refresh);
		}

		if (sinkronkanKegiatan) {
			updateDetailKegiatan(hasilDetailKegiatan, kegiatan, refresh);
		}
		return hasilDetailKegiatan;
	}

	/**
	 * Menelusuri seluruh rantai {@code getCause()} untuk memutuskan apakah sebuah kegagalan
	 * berasal dari koneksi basis data yang putus/basi — dikenali dari nama kelas
	 * ({@code JDBCConnectionException}, {@code EOFException}, {@code SocketException}) atau dari
	 * pesan ({@code "connection has been closed"}, {@code "i/o error"}).
	 *
	 * <p>Pencocokan berbasis nama dan teks pesan memang rapuh terhadap perubahan versi driver,
	 * tetapi dipakai karena kegagalan yang relevan datang terbungkus beberapa lapis pembungkus
	 * Hibernate/c3p0 yang tidak punya tipe bersama. Salah menilai hanya berakibat satu percobaan
	 * ulang yang tidak perlu, atau hilangnya satu percobaan ulang — bukan kesalahan data.</p>
	 *
	 * @param error kegagalan yang diperiksa; boleh {@code null}.
	 * @return {@code true} bila kegagalan berasal dari lapisan koneksi.
	 */
	private static boolean isConnectionFailure(Throwable error) {
		Throwable current = error;
		while (current != null) {
			String name = current.getClass().getName();
			String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
			if (name.indexOf("JDBCConnectionException") >= 0 || name.indexOf("EOFException") >= 0
					|| name.indexOf("SocketException") >= 0 || message.indexOf("connection has been closed") >= 0
					|| message.indexOf("i/o error") >= 0) return true;
			current = current.getCause();
		}
		return false;
	}

	// ========================================================================
	// HITUNG TAGIHAN "SEGAR & KONSISTEN" (READ-ONLY)
	// ========================================================================

	/**
	 * Hitung ulang total tagihan Kegiatan langsung dari template biaya AKTIF/terkini
	 * (bukan dari field denormalisasi Kegiatan.tagihan/tagihans yang bisa basi/tertukar
	 * generasi), dengan sumber nilai yang KONSISTEN dengan tampilan "Daftar Rincian
	 * Tagihan" (DetailKegiatan ter-baru per item, dikurangi diskon tersimpan terbesar).
	 * Murni READ-ONLY -- tidak menulis apa pun ke database, aman dipanggil berulang
	 * (mis. tiap render baris grid) tanpa efek samping/nilai bolak-balik.
	 *
	 * Dipakai bersama oleh InformasiPembayaranMahasiswaAction (dashboard/rincian
	 * mahasiswa) dan DetailSettingBiayaAction (grid mahasiswa yang cocok dengan satu
	 * SettingBiaya) agar keduanya SELALU menampilkan angka yang sama persis untuk
	 * Kegiatan yang sama -- sebelumnya logika ini terduplikasi lokal di
	 * InformasiPembayaranMahasiswaAction saja.
	 *
	 * @return total tagihan, atau {@code null} bila gerbang konfigurasi
	 *         {@code dashboard_tagihan_segar_konsisten} nonaktif atau data kegiatan
	 *         belum lengkap (pemanggil sebaiknya fallback ke {@link Kegiatan#hitungTagihan()}).
	 */
	@SuppressWarnings("rawtypes")
	public static Double hitungTagihanSegarKonsisten(Kegiatan k) {
		try {
			if (k == null || k.getJenisKegiatan() == null || k.getSemster() == null
					|| (k.getMahasiswa() == null && k.getCalonMahasiswa() == null)) {
				return null;
			}
			if (!Konfigurasi.AKTIF.equals(
					Common.getKonfigurasi("dashboard_tagihan_segar_konsisten", Konfigurasi.AKTIF).getNilai())) {
				return null;
			}
			Mahasiswa m = k.getMahasiswa();
			Integer smt = k.getSemster();
			Collection detailBiayas;
			if (k.getCalonMahasiswa() != null) {
				// Kegiatan milik CALON mahasiswa (pendaftaran/daftar ulang): WAJIB memakai
				// template jalur-CALON, persis seperti layar pembayarannya
				// (DaftarUlangMahasiswaBaruAction). Template jalur-MAHASISWA untuk kegiatan
				// jenis ini bisa BASI: cohort search-nya kosong sehingga cache lama tidak
				// pernah tergantikan dan menunjuk generasi DetailBiaya lama dengan nominal
				// lama -> header kelebihan (log kasus: item 99 via jalur-mahasiswa 18.95jt
				// [detailBiaya 31194] vs jalur-calon 15.95jt [detailBiaya 31006]).
				BiodataCalonMahasiswa cm = k.getCalonMahasiswa();
				Jurusan jurusanCalon = cm.getProdiLulus();
				if (jurusanCalon == null || jurusanCalon.getId() == null) {
					jurusanCalon = cm.getProdi1() == null ? cm.getProdi2() : cm.getProdi1();
				}
				detailBiayas = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(cm, k.getJenisKegiatan(),
						jurusanCalon, smt, false);
			} else {
				detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(m, smt, k.getJenisKegiatan(), false);
			}
			boolean adaBulanan = false;
			if (detailBiayas != null) {
				for (Object o : detailBiayas) {
					if (o instanceof PengaturanPembayaranBulanan) {
						adaBulanan = true;
						break;
					}
				}
			}
			if (adaBulanan && k.getCalonMahasiswa() == null) {
				detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(m, smt, k.getJenisKegiatan(), "-1", false);
			}
			Collection detailKegiatans = (k.getId() == null) ? null : k.ambilDetailKegiatan();

			// === Resolusi diskon DETERMINISTIK & READ-ONLY (anti BOLAK-BALIK 1.8jt/5.165jt) ===
			// "Hitung Ulang"/recompute MENUMPUK DetailKegiatan DUPLIKAT per item: sebagian sudah
			// meng-cache diskon BENAR (kadang kini NON-AKTIF), sebagian fresh diskon=0. Memilih SATU
			// DK (ambilSatuDetailKegiatan) hasilnya TIDAK stabil -> tagihan dasbor bolak-balik. Solusi:
			// pindai SEMUA DetailKegiatan kegiatan (aktif & non-aktif, via ambilDetailKegiatanSaja
			// refresh=true yang query by kegiatan TANPA filter aktif), simpan diskon TERBESAR per item
			// + status bukanTagihan dari DK TERBARU (id tertinggi). Diskon benar selalu ketemu -> hasil
			// KONSISTEN tiap render, tanpa menulis apa pun.
			//
			// FIX 2026-08-06 (kasus MARDI MARSON: item 500rb ganda ke-nol-kan, "Biaya Semester"
			// nilaiBisaDiubah sudah diedit langsung jadi 500rb NET tapi header tampil 0): diskon
			// TERBESAR di atas dulu SELALU dipasangkan ke `biaya` DK TERBARU walau keduanya berasal
			// dari GENERASI DK BERBEDA -> kalau baris terbaru sudah net (biaya diedit langsung jadi
			// lebih kecil, diskon di baris itu sendiri 0), diskon historis dari baris LAMA tetap
			// tersubtraksi lagi (double-count) -> hasil 0/negatif. Sekarang diskon historis HANYA
			// dipakai ulang bila baris tempat diskon terbesar itu ditemukan py `biaya` (gross) yang
			// SAMA dengan `biaya` baris terbaru -- artinya base belum berubah, cuma metadata diskon
			// hilang saat regenerasi (kasus asli yang dulu dilindungi fix ini). Bila base baris
			// terbaru SUDAH beda (diedit langsung / generasi baru beda nominal), pakai diskon MILIK
			// baris terbaru itu sendiri (bisa 0 -- base-nya memang sudah net), TIDAK subtraksi ulang.
			Map<String, double[]> diskonItemMap = new HashMap<String, double[]>();
			if (k.getId() != null) {
				try {
					List<DetailKegiatan> semuaDk = ambilDetailKegiatanSaja(k, true);
					if (semuaDk != null) {
						for (DetailKegiatan dkc : semuaDk) {
							if (dkc == null || dkc.getItemBiaya() == null || dkc.getItemBiaya().getId() == null) {
								continue;
							}
							// Samakan identitas dengan ambilSatuDetailKegiatan() yang dipakai panel rincian:
							// item + bayarKe + kegiatan. Memetakan hanya per item membuat baris historis
							// bayarKe lain dapat menimpa DPP/komponen aktif yang sedang ditampilkan.
							String detailKey = DetailKegiatan.kodeUnik(null, dkc.getItemBiaya(),
									dkc.getDetailBiaya() == null ? null : dkc.getDetailBiaya().getBayarKe(), k, null);
							if (detailKey == null) {
								continue;
							}
							// [maxDiskon, bukanTagihanTerbaru?1:0, idTerbaru, biayaDkTerbaru, adaBiayaDk?1:0,
							//  biayaPadaBarisMaxDiskon, diskonMilikBarisTerbaruSendiri]
							double[] info = diskonItemMap.get(detailKey);
							if (info == null) {
								info = new double[] { 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0 };
								diskonItemMap.put(detailKey, info);
							}
							double diskonBarisIni = dkc.getDiskon() == null ? 0.0 : dkc.getDiskon().doubleValue();
							double biayaBarisIni = dkc.getBiaya() == null ? 0.0 : dkc.getBiaya().doubleValue();
							if (diskonBarisIni > info[0]) {
								info[0] = diskonBarisIni;
								info[5] = biayaBarisIni;
							}
							double idDk = dkc.getId() == null ? -1.0 : dkc.getId().doubleValue();
							// Kondisi/nominal terkini harus berasal dari baris AKTIF. Baris nonaktif tetap
							// dipindai di atas hanya untuk fallback histori diskon, bukan untuk meniadakan
							// tagihan aktif (kasus DPP hilang dari total panel kiri).
							if (Boolean.TRUE.equals(dkc.getAktif()) && idDk >= info[2]) {
								info[2] = idDk;
								info[1] = (dkc.getBukanTagihan() != null && dkc.getBukanTagihan()) ? 1.0 : 0.0;
								info[3] = biayaBarisIni;
								info[4] = 1.0;
								info[6] = diskonBarisIni;
							}
						}
					}
				} catch (Exception eDk) {
					ais.common.ErrorAuditUtil.record(eDk,
							"auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:hitungTagihanSegarKonsisten");
				}
			}

			double total = 0.0;
			// ANTI "NOMINAL KELEBIHAN": template biaya bisa berisi item DOBEL (mahasiswa cocok
			// ke >1 baris Setting Biaya dgn item sama). Rincian tagihan di layar ini sudah
			// men-dedup per item, tapi penjumlahan header dulunya TIDAK -> header lebih besar
			// dari jumlah rincian (mis. selisih persis 1x nilai item yang dobel). Dedup di sini
			// per kunci itemBiaya+bayarKe (non-bulanan) dan per-PPB (bulanan), READ-ONLY.
			java.util.Set<String> kunciSudahDihitung = new java.util.HashSet<String>();
			if (detailBiayas != null) {
				for (Object o : detailBiayas) {
					if (o instanceof DetailBiaya) {
						DetailBiaya db = (DetailBiaya) o;
						String kunciDb = "item:"
								+ (db.getItemBiaya() == null || db.getItemBiaya().getId() == null ? "?"
										: db.getItemBiaya().getId().toString())
								+ "|bayarKe:" + (db.getBayarKe() == null ? "1" : db.getBayarKe().toString());
						if (!kunciSudahDihitung.add(kunciDb)) {
							continue;
						}
						// Nilai awal mengikuti mesin utama supaya item khusus tetap kompatibel. Perlu
						// diingat bahwa overload ini SUDAH mengurangi diskon melalui
						// Kegiatan.ambilSatuDetailKegiatan(...). Karena itu nilai ini hanya fallback;
						// bila metadata DetailKegiatan tersedia, total di bawah dibangun ulang dari
						// nominal DASAR dan diskon tepat satu kali.
						Double j = Kegiatan.ambilJumlahTagihan(k, db);
						// PENTING (idempoten): JANGAN memanggil overload ber-DetailKegiatan di sini karena
						// itu memicu hitungDiskon yang MENULIS ke DB (setDiskon/refreshUpdate) + menutup
						// session. Recompute display ini harus READ-ONLY; bila tidak, tiap klik "Hitung
						// Ulang" mengubah state diskon -> hasil bolak-balik BENAR(1.8jt)/SALAH(5.165jt).
						// Solusi: untuk item harga-tetap, jangan mengurangi `j` lagi karena `j` sudah
						// neto. Bangun ulang dari biaya DK aktif (atau nominal konfigurasi) lalu kurangi
						// diskon efektif satu kali. Bug lama melakukan `j - diskon`, sehingga contoh
						// 2.000.000 - 1.000.000 = 1.000.000 dikurangi lagi menjadi 0.
						if (j != null && db.getItemBiaya() != null && !db.getItemBiaya().getNilaiBisaDiubah()
								&& db.getItemBiaya().getId() != null) {
							String detailKey = DetailKegiatan.kodeUnik(null, db.getItemBiaya(), db.getBayarKe(),
									k, null);
							double[] info = diskonItemMap.get(detailKey);
							if (info != null) {
								if (info[1] == 1.0) {
									j = Double.valueOf(0.0);
								} else {
									double brutoReferensi = ambilBrutoReferensi(db, j.doubleValue());
									double dasar = info.length >= 5 && info[4] == 1.0
											? info[3] : brutoReferensi;
									double diskonDipakai = diskonEfektif(info, brutoReferensi);
									j = Double.valueOf(Math.max(0.0, dasar - diskonDipakai));
								}
							}
						}
						// KONSISTEN DGN RINCIAN (fix header 28.25jt vs rincian 25.25jt): untuk item
						// nilaiBisaDiubah, ambilJumlahTagihan memilih SATU DetailKegiatan secara TIDAK
						// STABIL sehingga bisa mengambil baris DUPLIKAT LAMA yang nilainya basi (log
						// kasus: item 99 terhitung 18.95jt padahal DK terbaru 15.95jt). Tampilan
						// "Daftar Rincian Tagihan" memakai DK ter-BARU (id terbesar) -- header wajib
						// memakai sumber yang sama: neto = biaya DK terbaru - diskon (bukanTagihan=0).
						// Item khusus (parameterTambahan/skor, hitung tunggakan) tetap ke hasil lama.
						try {
							if (db.getItemBiaya() != null && db.getItemBiaya().getId() != null
									&& db.getItemBiaya().getNilaiBisaDiubah()
									&& db.getItemBiaya().getParameterTambahan() == null
									&& (db.getItemBiaya().getPenghitungan() == null || !db.getItemBiaya()
											.getPenghitungan().equals(ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU))) {
								String detailKey = DetailKegiatan.kodeUnik(null, db.getItemBiaya(), db.getBayarKe(),
										k, null);
								double[] info = diskonItemMap.get(detailKey);
								if (info != null && info.length >= 5 && info[4] == 1.0) {
									double brutoReferensi = ambilBrutoReferensi(db, j == null ? 0.0 : j.doubleValue());
									double neto = info[1] == 1.0 ? 0.0
											: info[3] - diskonEfektif(info, brutoReferensi);
									if (neto < 0.0) {
										neto = 0.0;
									}
									j = Double.valueOf(neto);
								}
							}
						} catch (Exception eKoreksi) {
							ais.common.ErrorAuditUtil.record(eKoreksi,
									"auto-audit hitungTagihanSegarKonsisten koreksi DK-terbaru");
						}
						if (j != null) {
							total += j;
						}
					} else if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan ppb = (PengaturanPembayaranBulanan) o;
						if (ppb.getId() != null && !kunciSudahDihitung.add("ppb:" + ppb.getId())) {
							continue;
						}
						Double j = Kegiatan.ambilJumlahTagihan(k, detailKegiatans, m, smt, ppb);
						if (j != null) {
							total += j;
						}
					}
				}
			}
			return Double.valueOf(total);
		} catch (Exception e) {
			if (e instanceof org.hibernate.HibernateException) {
				throw (org.hibernate.HibernateException) e;
			}
			return null;
		}
	}

	/**
	 * Tentukan diskon yang BOLEH dipakai utk mengurangi {@code biaya} DK TERBARU (dipakai
	 * {@link #hitungTagihanSegarKonsisten(Kegiatan)}), menghindari DOUBLE-COUNT saat base sudah
	 * net.
	 *
	 * <p>{@code info} = {@code [maxDiskon, bukanTagihanTerbaru, idTerbaru, biayaDkTerbaru,
	 * adaBiayaDk, biayaPadaBarisMaxDiskon, diskonMilikBarisTerbaruSendiri]} (lihat pemindaian di
	 * {@link #hitungTagihanSegarKonsisten(Kegiatan)}).</p>
	 *
	 * <p><b>Aturan.</b> (1) Bila baris TERBARU sendiri sudah punya diskon tercatat (&gt;0),
	 * pakai itu -- paling akurat, sudah dipasangkan dgn biaya di baris yg sama, kecuali nominal
	 * terbaru + diskon sama dengan bruto konfigurasi. Pola terakhir berarti nominal terbaru sudah
	 * NETO dan diskon hanya metadata penjelas; menguranginya lagi akan menghasilkan double-count.
	 * (2) Bila baris
	 * terbaru diskonnya 0/kosong TAPI biaya-nya SAMA dgn biaya baris tempat diskon terbesar
	 * historis ditemukan, berarti base belum berubah sejak generasi yang py diskon benar --
	 * metadata diskonnya saja yang hilang saat regenerasi -> pakai diskon historis itu (baru
	 * "reapply"). (3) Selain itu (base baris terbaru SUDAH beda dari base baris diskon historis
	 * -- mis. nilai diedit langsung jadi net) -> jangan subtraksi ulang, kembalikan 0.</p>
	 */
	private static double diskonEfektif(double[] info, double brutoReferensi) {
		if (info == null || info.length < 7) {
			return 0.0;
		}
		if (info[6] > 0.0) {
			// Sebagian alur diskon lama menyimpan biaya DK sebagai nominal sesudah diskon,
			// sementara getDiskon() tetap mengembalikan potongannya. Contoh: konfigurasi
			// 2 jt, biaya DK 1 jt, diskon 1 jt. Dalam bentuk ini biaya DK tidak boleh
			// dikurangi lagi. Toleransi 1 rupiah menghindari salah klasifikasi akibat hasil
			// diskon persen bertipe floating point.
			if (brutoReferensi > info[3] + 0.5
					&& hampirSama(info[3] + info[6], brutoReferensi)) {
				return 0.0;
			}
			return info[6];
		}
		if (info[0] > 0.0 && hampirSama(info[3], info[5])) {
			return info[0];
		}
		return 0.0;
	}

	/**
	 * Ambil nominal bruto konfigurasi tanpa menambahkan atau mengurangi diskon. Nilai ini
	 * hanya dipakai sebagai pembanding untuk membedakan dua bentuk data historis
	 * DetailKegiatan: {@code biaya=bruto,diskon=potongan} dan
	 * {@code biaya=neto,diskon=potongan}. Fallback adalah hasil mesin lama agar item khusus
	 * yang tidak mempunyai nominal konfigurasi tetap dapat dirender.
	 */
	private static double ambilBrutoReferensi(DetailBiaya detailBiaya, double fallback) {
		if (detailBiaya == null) {
			return fallback;
		}
		try {
			Double nilai = detailBiaya.getTunggakanLalu();
			if (nilai == null || Math.abs(nilai.doubleValue()) < 0.01) {
				nilai = detailBiaya.getNilaiBiayaBaru() == null
						? detailBiaya.getNilaiBiaya() : detailBiaya.getNilaiBiayaBaru();
			}
			return nilai == null ? fallback : nilai.doubleValue();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit ambilBrutoReferensi KegiatanPersistenceHelper");
			return fallback;
		}
	}

	/**
	 * Membandingkan dua nilai rupiah dengan toleransi 1 rupiah. Dipakai
	 * {@link #diskonEfektif(double[], double)} untuk membedakan bentuk data historis
	 * {@code biaya=bruto} dan {@code biaya=neto} tanpa salah klasifikasi akibat pembulatan
	 * floating point pada diskon berbasis persen.
	 *
	 * @param kiri  nilai pertama.
	 * @param kanan nilai kedua.
	 * @return {@code true} bila selisihnya tidak lebih dari 1.
	 */
	private static boolean hampirSama(double kiri, double kanan) {
		return Math.abs(kiri - kanan) <= 1.0;
	}

	/**
	 * Varian per-MAHASISWA dari {@link #ambilDetailKegiatanSaja(Kegiatan, boolean)}: mengambil
	 * detail kegiatan untuk SELURUH kegiatan milik satu mahasiswa/calon mahasiswa sekaligus, lalu
	 * menyinkronkan kolom denormalisasi tiap kegiatan lewat
	 * {@link #sinkronkanDetailPerKegiatan(java.util.List, java.util.Map, boolean)}.
	 *
	 * <p>Percabangan {@code student instanceof Mahasiswa} memilih kolom {@code mahasiswa} atau
	 * {@code calonMahasiswa} pada kriteria {@link Kegiatan} — pola dua-sisi yang sama dengan
	 * {@code KegiatanHelper}. Saat {@code refresh=false}, id dikumpulkan dari {@code jsonLokasi...}
	 * dan dari cache tiap kegiatan tanpa menyentuh basis data.</p>
	 *
	 * <p><b>Menulis ke basis data</b> lewat antrean async, termasuk MENGOSONGKAN
	 * {@code detailKegiatans} bagi kegiatan pada {@code kegiatansCache} yang ternyata tidak punya
	 * detail sama sekali (hanya bila {@code refresh=true}).</p>
	 *
	 * @param student                  {@link Mahasiswa} atau {@link BiodataCalonMahasiswa}
	 *                                 pemilik kegiatan.
	 * @param jsonLokasiDetailKegiatan snapshot JSON id detail kegiatan; dipakai hanya bila
	 *                                 {@code refresh=false}.
	 * @param kegiatansCache           kegiatan yang sudah dimuat pemanggil, agar object yang sama
	 *                                 yang disinkronkan (bukan salinan baru).
	 * @param refresh                  paksa baca dari basis data.
	 * @return daftar detail kegiatan seluruh kegiatan mahasiswa tersebut.
	 */
	@SuppressWarnings("unchecked")
	public static List<DetailKegiatan> ambilDetailKegiatanSaja(VOMahasiswa student, String jsonLokasiDetailKegiatan,
			Collection<Kegiatan> kegiatansCache, boolean refresh) {

		TreeSet<Long> keysData = new TreeSet<Long>();
		Map<Long, Kegiatan> mapKegiatanUtama = mapKegiatan(kegiatansCache);

		if (!refresh) {
			keysData.addAll(ekstrakIdDariJson(jsonLokasiDetailKegiatan));

			for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
				List<Long> aktifIds = kegiatan.ambilDetailKegiatansAktifIds();
				if (aktifIds != null && !aktifIds.isEmpty()) {
					keysData.addAll(aktifIds);
				}
			}
		}

		Session session = null;
		try {
			if (refresh) {
				session = HibernateUtil.getSessionFactory().openSession();
				Criteria criteria = session.createCriteria(DetailKegiatan.class);
				criteria.setProjection(Projections.property("id"));

				Criteria kegCrit = criteria.createCriteria("kegiatan");
				if (student instanceof Mahasiswa) {
					kegCrit.add(Restrictions.eq("mahasiswa", student));
				} else {
					kegCrit.add(Restrictions.eq("calonMahasiswa", student));
				}

				kegCrit.addOrder(Order.asc("semster"));
				kegCrit.addOrder(Order.asc("jenisKegiatan"));
				kegCrit.addOrder(Order.asc("id"));

				criteria.setTimeout(600);
				List<Long> dbKeys = criteria.list();
				if (dbKeys != null && !dbKeys.isEmpty()) {
					keysData.addAll(dbKeys);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}

		List<DetailKegiatan> detailKegiatans = new ArrayList<DetailKegiatan>();
		if (!keysData.isEmpty()) {
			detailKegiatans = GeneralValueObject.ambilDataBanyak(DetailKegiatan.class, new ArrayList<Long>(keysData),
					refresh);
		}

		sinkronkanDetailPerKegiatan(detailKegiatans, mapKegiatanUtama, refresh);
		return detailKegiatans;
	}

	/**
	 * Memuat seluruh {@link CicilanPembayaran} satu kegiatan dan menyimpan snapshot pembayaran
	 * secara TERISOLASI (transaksi sendiri). Pintasan untuk
	 * {@code ambilCicilan(kegiatan, refresh, true)}.
	 *
	 * <p>Gunakan overload tiga-parameter dengan {@code simpanTerisolasi=false} bila pemanggil
	 * sendiri sedang memegang transaksi yang akan menyimpan kegiatan yang sama — jika tidak,
	 * transaksi kedua di sini akan menunggu lock milik transaksi pemanggil sendiri.</p>
	 *
	 * @param kegiatan kegiatan pemilik cicilan.
	 * @param refresh  paksa baca dari basis data.
	 * @return daftar cicilan; tidak pernah {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public static List<CicilanPembayaran> ambilCicilan(Kegiatan kegiatan, boolean refresh) {
		return ambilCicilan(kegiatan, refresh, true);
	}

	/**
	 * Memuat cicilan dan memperbarui snapshot pembayaran pada object {@link Kegiatan}.
	 *
	 * <p>{@code simpanTerisolasi=false} dipakai ketika pemanggil masih memiliki transaksi
	 * yang akan menyimpan object kegiatan yang sama. Pada kondisi itu membuka transaksi
	 * kedua untuk meng-update baris yang sama akan menunggu lock milik transaksi pemanggil
	 * sendiri. Snapshot tetap dihitung lengkap pada object, kemudian ikut tersimpan dalam
	 * satu flush milik pemanggil.</p>
	 */
	@SuppressWarnings("unchecked")
	public static List<CicilanPembayaran> ambilCicilan(Kegiatan kegiatan, boolean refresh,
			boolean simpanTerisolasi) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return new ArrayList<CicilanPembayaran>();
		}

		TreeSet<Long> keyData = new TreeSet<Long>();

		if (!refresh) {
			List<Long> aktifIds = kegiatan.ambilCicilansAktifIds();
			if (aktifIds != null && !aktifIds.isEmpty()) {
				keyData.addAll(aktifIds);
			}
		}

		Session session = null;
		try {
			if (refresh || (keyData.isEmpty() && !kegiatan.udah("ambilCicilan"))) {
				session = HibernateUtil.getSessionFactory().openSession();
				Criteria criteria = session.createCriteria(CicilanPembayaran.class);
				criteria.setProjection(Projections.property("id"));
				criteria.add(Restrictions.eq("kegiatan", kegiatan));
				criteria.addOrder(Order.asc("id"));
				criteria.setTimeout(600);

				List<Long> dbKeys = criteria.list();
				if (dbKeys != null && !dbKeys.isEmpty()) {
					keyData.addAll(dbKeys);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}

		List<CicilanPembayaran> hasilCicilan = new ArrayList<CicilanPembayaran>();
		if (!keyData.isEmpty()) {
			hasilCicilan = GeneralValueObject.ambilDataBanyak(CicilanPembayaran.class, new ArrayList<Long>(keyData),
					refresh);
		}

		if (refresh) {
			kegiatan.setTanggal_dirubah(WaktuUtil.getDate());
		}
		if (simpanTerisolasi) {
			updatePembayaran(hasilCicilan, kegiatan, refresh);
		} else {
			terapkanRekapPembayaranLokal(hasilCicilan, kegiatan);
		}

		return hasilCicilan;
	}

	/**
	 * Varian per-MAHASISWA dari {@link #ambilCicilan(Kegiatan, boolean)}: memuat cicilan seluruh
	 * kegiatan milik satu mahasiswa/calon mahasiswa, opsional disaring per {@link JenisKegiatan},
	 * lalu menyinkronkan snapshot pembayaran tiap kegiatan lewat
	 * {@link #sinkronkanCicilanPerKegiatan(java.util.List, java.util.Map, boolean)}.
	 *
	 * <p><b>Pintasan penting:</b> mahasiswa dengan flag {@code getTidakAdaTagihan()} bernilai
	 * {@code TRUE} langsung mendapat daftar KOSONG tanpa query apa pun — dan karena kembali lebih
	 * awal, snapshot pembayaran kegiatannya juga TIDAK disinkronkan. Kolom denormalisasi kegiatan
	 * mahasiswa seperti ini karena itu mempertahankan nilai terakhirnya. Flag ini hanya diperiksa
	 * untuk {@link Mahasiswa}, tidak untuk calon mahasiswa.</p>
	 *
	 * @param student           {@link Mahasiswa} atau {@link BiodataCalonMahasiswa}.
	 * @param jsonLokasiCicilan snapshot JSON id cicilan; dipakai hanya bila {@code refresh=false}.
	 * @param kegiatansCache    kegiatan yang sudah dimuat pemanggil.
	 * @param jenisKegiatanData bila tidak {@code null}, batasi hanya pada jenis kegiatan ini.
	 * @param refresh           paksa baca dari basis data.
	 * @return daftar cicilan; tidak pernah {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public static List<CicilanPembayaran> ambilCicilan(Object student, String jsonLokasiCicilan,
			Collection<Kegiatan> kegiatansCache, JenisKegiatan jenisKegiatanData, boolean refresh) {

		if (student instanceof Mahasiswa && Boolean.TRUE.equals(((Mahasiswa) student).getTidakAdaTagihan())) {
			return new ArrayList<CicilanPembayaran>();
		}

		TreeSet<Long> keyData = new TreeSet<Long>();
		Map<Long, Kegiatan> mapKegiatanUtama = mapKegiatan(kegiatansCache);

		if (!refresh) {
			keyData.addAll(ekstrakIdDariJson(jsonLokasiCicilan));

			for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
				List<Long> aktifIds = kegiatan.ambilCicilansAktifIds();
				if (aktifIds != null && !aktifIds.isEmpty()) {
					keyData.addAll(aktifIds);
				}
			}
		}

		Session session = null;
		try {
			if (refresh) {
				session = HibernateUtil.getSessionFactory().openSession();
				Criteria criteria = session.createCriteria(CicilanPembayaran.class);
				criteria.setProjection(Projections.property("id"));

				Criteria kegCrit = criteria.createCriteria("kegiatan");
				if (student instanceof Mahasiswa) {
					kegCrit.add(Restrictions.eq("mahasiswa", student));
				} else {
					kegCrit.add(Restrictions.eq("calonMahasiswa", student));
				}

				if (jenisKegiatanData != null && jenisKegiatanData.getId() != null) {
					kegCrit.add(Restrictions.eq("jenisKegiatan", jenisKegiatanData));
				}

				kegCrit.addOrder(Order.asc("semster"));
				kegCrit.addOrder(Order.asc("jenisKegiatan"));
				kegCrit.addOrder(Order.asc("id"));

				criteria.setTimeout(600);
				List<Long> dbKeys = criteria.list();
				if (dbKeys != null && !dbKeys.isEmpty()) {
					keyData.addAll(dbKeys);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}

		List<CicilanPembayaran> hasilCicilan = new ArrayList<CicilanPembayaran>();
		if (!keyData.isEmpty()) {
			hasilCicilan = GeneralValueObject.ambilDataBanyak(CicilanPembayaran.class, new ArrayList<Long>(keyData),
					refresh);
		}

		sinkronkanCicilanPerKegiatan(hasilCicilan, mapKegiatanUtama, refresh);
		return hasilCicilan;
	}

	/**
	 * Menyaring koleksi {@link DetailKegiatan} yang SUDAH dimuat pemanggil agar hanya menyisakan
	 * milik {@code kegiatan}, lalu menyinkronkan kolom denormalisasi kegiatan tersebut. Tidak
	 * melakukan query apa pun — dipakai ketika pemanggil sudah memuat detail untuk banyak kegiatan
	 * sekaligus dan tinggal membagikannya per kegiatan.
	 *
	 * <p><b>Tetap menulis</b> ke basis data lewat
	 * {@link #updateDetailKegiatan(java.util.List, Kegiatan, boolean)}.</p>
	 *
	 * @param temp     koleksi sumber; bila {@code null} dikembalikan daftar kosong.
	 * @param kegiatan kegiatan penyaring; {@code null}/tanpa id membuat {@code temp} dikembalikan
	 *                 apa adanya tanpa sinkronisasi.
	 * @param refresh  diteruskan ke sinkronisasi denormalisasi.
	 * @return sub-daftar detail milik {@code kegiatan}.
	 */
	public static Collection<DetailKegiatan> ambilDetailKegiatan(Collection<DetailKegiatan> temp, Kegiatan kegiatan,
			boolean refresh) {
		if (kegiatan == null || kegiatan.getId() == null || temp == null) {
			return temp != null ? temp : new ArrayList<DetailKegiatan>();
		}

		List<DetailKegiatan> list = new ArrayList<DetailKegiatan>();
		for (DetailKegiatan detailKegiatan : temp) {
			if (detailKegiatan != null && detailKegiatan.getKegiatan() != null
					&& kegiatan.getId().equals(detailKegiatan.getKegiatan().getId())) {
				list.add(detailKegiatan);
			}
		}

		updateDetailKegiatan(list, kegiatan, refresh);
		return list;
	}

	/**
	 * Mengindeks koleksi {@link Kegiatan} menjadi peta id → kegiatan, melewati entri {@code null}
	 * atau yang belum punya id. Tujuannya agar sinkronisasi selalu menulis ke INSTANCE yang sama
	 * dengan yang dipegang pemanggil, bukan ke salinan hasil query baru — sehingga nilai
	 * denormalisasi yang dihitung ikut terlihat oleh layar pemanggil.
	 *
	 * @param kegiatansCache koleksi sumber; boleh {@code null}.
	 * @return peta id → kegiatan; tidak pernah {@code null}.
	 */
	private static Map<Long, Kegiatan> mapKegiatan(Collection<Kegiatan> kegiatansCache) {
		Map<Long, Kegiatan> result = new HashMap<Long, Kegiatan>();
		if (kegiatansCache == null) {
			return result;
		}
		for (Kegiatan kegiatan : kegiatansCache) {
			if (kegiatan != null && kegiatan.getId() != null) {
				result.put(kegiatan.getId(), kegiatan);
			}
		}
		return result;
	}

	/**
	 * Membaca id dari snapshot JSON berbentuk {@code {"<id>":"<nilai>"}} — KUNCI-nya yang diambil
	 * sebagai id, bukan nilainya; nilai hanya dipakai sebagai penanda "terisi" (entri bernilai
	 * kosong dilewati).
	 *
	 * <p>Kunci yang tidak dapat diurai menjadi angka dilewati diam-diam (dicatat ke
	 * {@code ErrorAuditUtil}), dan JSON yang rusak seluruhnya menghasilkan daftar kosong alih-alih
	 * kegagalan — sengaja permisif karena kolom ini hanya cache turunan yang selalu dapat dibangun
	 * ulang dari basis data.</p>
	 *
	 * @param jsonData teks JSON snapshot; boleh {@code null}/kosong.
	 * @return daftar id; tidak pernah {@code null}.
	 */
	private static List<Long> ekstrakIdDariJson(String jsonData) {
		List<Long> result = new ArrayList<Long>();
		if (isEmpty(jsonData)) {
			return result;
		}
		try {
			JSONObject jsonObject = new JSONObject(jsonData);
			Iterator<?> keys = jsonObject.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				String value = jsonObject.optString(key, "");
				if (!isEmpty(value)) {
					try {
						result.add(Long.valueOf(key.trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:449");
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:453");
		}
		return result;
	}

	/**
	 * Mengelompokkan daftar {@link DetailKegiatan} gabungan menurut kegiatan pemiliknya lalu
	 * memperbarui kolom {@code detailKegiatans} tiap kegiatan.
	 *
	 * <p><b>Pembersihan kegiatan tanpa detail.</b> Bila {@code refresh=true}, kegiatan yang ada di
	 * {@code mapKegiatanUtama} tetapi TIDAK muncul di daftar detail akan dikosongkan
	 * ({@code setDetailKegiatans("")}) dan ditulis segera. Mekanismenya: tiap kegiatan yang sudah
	 * tertangani DIHAPUS dari {@code mapKegiatanUtama} di dalam perulangan, sehingga sisa isi peta
	 * tepat berisi kegiatan yang perlu dikosongkan. Karena peta pemanggil ikut termutasi, method
	 * ini tidak boleh dipanggil dua kali dengan peta yang sama.</p>
	 *
	 * <p>Bila daftar detail seluruhnya kosong, seluruh isi peta langsung dikosongkan (tetap hanya
	 * bila {@code refresh=true}).</p>
	 *
	 * @param detailKegiatans  detail gabungan lintas kegiatan.
	 * @param mapKegiatanUtama peta id → kegiatan milik pemanggil; DIMUTASI oleh method ini.
	 * @param refresh          bila {@code false}, pembersihan kegiatan tanpa detail dilewati.
	 */
	private static void sinkronkanDetailPerKegiatan(List<DetailKegiatan> detailKegiatans,
			Map<Long, Kegiatan> mapKegiatanUtama, boolean refresh) {
		if (detailKegiatans == null || detailKegiatans.isEmpty()) {
			if (refresh) {
				for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
					kegiatan.setDetailKegiatans("");
					simpanPerubahanAsync(kegiatan, true, true);
				}
			}
			return;
		}

		Map<Long, List<DetailKegiatan>> grouped = new HashMap<Long, List<DetailKegiatan>>();
		Map<Long, Kegiatan> mapKegiatanTemp = new HashMap<Long, Kegiatan>();

		for (DetailKegiatan detailKegiatan : detailKegiatans) {
			if (detailKegiatan != null && detailKegiatan.getKegiatan() != null && detailKegiatan.getId() != null) {
				Long kId = detailKegiatan.getKegiatan().getId();
				if (!grouped.containsKey(kId)) {
					grouped.put(kId, new ArrayList<DetailKegiatan>());
					mapKegiatanTemp.put(kId, detailKegiatan.getKegiatan());
				}
				grouped.get(kId).add(detailKegiatan);
			}
		}

		for (Map.Entry<Long, List<DetailKegiatan>> entry : grouped.entrySet()) {
			Long kId = entry.getKey();
			Kegiatan target = mapKegiatanUtama.containsKey(kId) ? mapKegiatanUtama.get(kId) : mapKegiatanTemp.get(kId);
			updateDetailKegiatan(entry.getValue(), target, refresh);
			if (refresh) {
				mapKegiatanUtama.remove(kId);
			}
		}

		if (refresh) {
			for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
				kegiatan.setDetailKegiatans("");
				simpanPerubahanAsync(kegiatan, true, true);
			}
		}
	}

	/**
	 * Padanan {@link #sinkronkanDetailPerKegiatan(java.util.List, java.util.Map, boolean)} untuk
	 * {@link CicilanPembayaran}: mengelompokkan cicilan per kegiatan, menyetel
	 * {@code tanggal_dirubah} bila {@code refresh}, lalu memperbarui snapshot pembayaran.
	 *
	 * <p>Perbedaan dari versi detail: kegiatan yang tidak punya cicilan TIDAK dikosongkan begitu
	 * saja melainkan diproses ulang dengan daftar KOSONG lewat
	 * {@link #updatePembayaran(java.util.List, Kegiatan, boolean)} — sehingga {@code bulans} dan
	 * {@code dibayar} ikut dihitung ulang menjadi nol, bukan sekadar dikosongkan teksnya.
	 * {@code mapKegiatanUtama} juga DIMUTASI di sini.</p>
	 *
	 * @param cicilanPembayarans cicilan gabungan lintas kegiatan.
	 * @param mapKegiatanUtama   peta id → kegiatan milik pemanggil; DIMUTASI oleh method ini.
	 * @param refresh            bila {@code false}, kegiatan tanpa cicilan tidak diproses ulang.
	 */
	private static void sinkronkanCicilanPerKegiatan(List<CicilanPembayaran> cicilanPembayarans,
			Map<Long, Kegiatan> mapKegiatanUtama, boolean refresh) {
		if (cicilanPembayarans == null || cicilanPembayarans.isEmpty()) {
			if (refresh) {
				for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
					updatePembayaran(new ArrayList<CicilanPembayaran>(), kegiatan, true);
				}
			}
			return;
		}

		Map<Long, List<CicilanPembayaran>> grouped = new HashMap<Long, List<CicilanPembayaran>>();
		Map<Long, Kegiatan> mapKegiatanTemp = new HashMap<Long, Kegiatan>();

		for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
			if (cicilanPembayaran != null && cicilanPembayaran.getKegiatan() != null) {
				Long kId = cicilanPembayaran.getKegiatan().getId();
				if (!grouped.containsKey(kId)) {
					grouped.put(kId, new ArrayList<CicilanPembayaran>());
					mapKegiatanTemp.put(kId, cicilanPembayaran.getKegiatan());
				}
				grouped.get(kId).add(cicilanPembayaran);
			}
		}

		for (Map.Entry<Long, List<CicilanPembayaran>> entry : grouped.entrySet()) {
			Long kId = entry.getKey();
			Kegiatan target = mapKegiatanUtama.containsKey(kId) ? mapKegiatanUtama.get(kId) : mapKegiatanTemp.get(kId);
			if (refresh && target != null) {
				target.setTanggal_dirubah(WaktuUtil.getDate());
			}
			updatePembayaran(entry.getValue(), target, refresh);
			if (refresh) {
				mapKegiatanUtama.remove(kId);
			}
		}

		if (refresh) {
			for (Kegiatan kegiatan : mapKegiatanUtama.values()) {
				updatePembayaran(new ArrayList<CicilanPembayaran>(), kegiatan, true);
			}
		}
	}

	// ========================================================================
	// 2. UPDATE STATE KEGIATAN
	// ========================================================================

	/**
	 * Menyetel kolom {@code cicilans} kegiatan dari daftar cicilan aktif lalu menjadwalkan
	 * penulisan denormalisasi.
	 *
	 * <p>Nilai {@code refresh} diteruskan sebagai DUA argumen sekaligus ke
	 * {@link #simpanPerubahanAsync(Kegiatan, boolean, boolean)}, yaitu {@code refreshOrDelete} dan
	 * {@code immediateUpdate}. Artinya {@code refresh=true} memaksa penulisan SEGERA (melewati
	 * debounce 15 detik), sedangkan {@code refresh=false} menempuh antrean tertunda.</p>
	 *
	 * <p>Kegagalan tidak dilempar ke pemanggil, hanya ditampilkan bagi admin lewat
	 * {@code Common.tampilErrorJikaAdmin} — konsisten dengan sifat kolom ini sebagai data turunan.</p>
	 *
	 * @param listCp   cicilan aktif kegiatan.
	 * @param kegiatan kegiatan sasaran; {@code null}/tanpa id diabaikan diam-diam.
	 * @param refresh  {@code true} untuk menulis segera, {@code false} untuk menunda.
	 */
	public static void updatePembayaran(List<CicilanPembayaran> listCp, Kegiatan kegiatan, boolean refresh) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return;
		}
		try {
			kegiatan.setCicilans(bangunStringAktif(listCp));
			simpanPerubahanAsync(kegiatan, refresh, refresh);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menyetel SATU entri nominal tagihan pada JSON {@code Kegiatan.tagihans} untuk kombinasi
	 * {@link DetailBiaya}/{@link PengaturanPembayaranBulanan} tertentu, lalu — hanya bila hasilnya
	 * BERBEDA dari nilai lama — menjadwalkan penulisan tertunda.
	 *
	 * <p>Nilai disimpan sebagai {@code String} dari {@code nilai.intValue()}, jadi pecahan rupiah
	 * DIPOTONG. Hasil akhir dilewatkan {@link #murnikan(JenisKegiatan, String)} agar kunci yang
	 * tidak sesuai sifat jenis kegiatan (hanya angsuran / hanya bukan angsuran) ikut tersaring.
	 *
	 * <p>Berbeda dari overload berbasis daftar cicilan, method ini SELALU memakai jalur tertunda
	 * ({@code immediateUpdate=false}) karena dipanggil per field saat pengguna menyunting nominal
	 * di layar — penulisan segera akan menghasilkan satu transaksi per ketikan.</p>
	 *
	 * @param detailBiaya       komponen biaya; dipakai bila {@code pengaturanBulanan} tidak
	 *                          menyediakan kunci.
	 * @param pengaturanBulanan pengaturan pembayaran bulanan; bila ada, kuncinya diberi sufiks
	 *                          {@code _<realBulan>}.
	 * @param kegiatan          kegiatan sasaran; {@code null} diabaikan.
	 * @param nilai             nominal tagihan baru; {@code null} diabaikan.
	 */
	public static void updatePembayaran(DetailBiaya detailBiaya, PengaturanPembayaranBulanan pengaturanBulanan,
			Kegiatan kegiatan, Double nilai) {
		if (kegiatan == null || nilai == null) {
			return;
		}
		try {
			String key = buatKeyTagihan(detailBiaya, pengaturanBulanan);
			if (key == null) {
				return;
			}

			String oldTagihans = kegiatan.getTagihans() == null ? "{}" : kegiatan.getTagihans();
			JSONObject jsonObject = new JSONObject(oldTagihans);
			jsonObject.put(key, String.valueOf(nilai.intValue()));

			String newTagihans = murnikan(kegiatan.getJenisKegiatan(), jsonObject.toString());
			if (!oldTagihans.equals(newTagihans)) {
				kegiatan.setTagihans(newTagihans);
				simpanPerubahanAsync(kegiatan, false, false);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menyetel kolom {@code detailKegiatans} kegiatan dari daftar detail aktif lalu menjadwalkan
	 * penulisan denormalisasi. Sepenuhnya sejajar dengan
	 * {@link #updatePembayaran(java.util.List, Kegiatan, boolean)}, termasuk pemakaian
	 * {@code refresh} sebagai penentu segera-vs-tertunda dan penelanan kegagalan.
	 *
	 * @param listDk   detail kegiatan aktif.
	 * @param kegiatan kegiatan sasaran; {@code null}/tanpa id diabaikan diam-diam.
	 * @param refresh  {@code true} untuk menulis segera, {@code false} untuk menunda.
	 */
	public static void updateDetailKegiatan(List<DetailKegiatan> listDk, Kegiatan kegiatan, boolean refresh) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return;
		}
		try {
			kegiatan.setDetailKegiatans(bangunStringAktif(listDk));
			simpanPerubahanAsync(kegiatan, refresh, refresh);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menandai satu {@link CicilanPembayaran} sebagai TIDAK aktif pada kolom denormalisasi
	 * kegiatan, lalu menulis segera.
	 *
	 * <p><b>Perhatikan bentuk penulisannya:</b> kolom {@code cicilans} ditimpa TOTAL dengan
	 * {@code ",<id>:false,"} — bukan ditambahi entri baru. Ini disengaja dan bekerja karena
	 * {@link #ekstrakIdAktif(String)} hanya mengambil id ber-flag {@code true}: hasilnya daftar
	 * aktif menjadi KOSONG, dan {@link #eksekusiUpdateDenganRetryTerkunci} kemudian membangun
	 * ulang seluruh rekap dari daftar kosong itu. Jadi efeknya "lupakan seluruh cicilan yang
	 * ter-cache lalu hitung ulang", bukan "buang satu id ini saja". Pemuatan berikutnya dengan
	 * {@code refresh=true} akan mengisi ulang daftar dari basis data.</p>
	 *
	 * <p>Method ini TIDAK menghapus baris {@link CicilanPembayaran} dari basis data; penghapusan
	 * entity sesungguhnya adalah tanggung jawab pemanggil.</p>
	 *
	 * @param cicilanPembayaran cicilan yang dihapus; {@code null}/tanpa id diabaikan.
	 * @param kegiatan          kegiatan pemilik; {@code null} diabaikan.
	 */
	public static void hapusCicilan(CicilanPembayaran cicilanPembayaran, Kegiatan kegiatan) {
		if (kegiatan == null || cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
			return;
		}
		kegiatan.setCicilans("," + cicilanPembayaran.getId() + ":false,");
		simpanPerubahanAsync(kegiatan, true, true);
	}

	/**
	 * Padanan {@link #hapusCicilan(CicilanPembayaran, Kegiatan)} untuk {@link DetailKegiatan}:
	 * menimpa kolom {@code detailKegiatans} dengan {@code ",<id>:false,"} sehingga daftar aktif
	 * menjadi kosong dan rekap dibangun ulang saat penulisan segera. Tidak menghapus baris entity
	 * dari basis data.
	 *
	 * @param detailKegiatan detail yang dihapus; {@code null}/tanpa id diabaikan.
	 * @param kegiatan       kegiatan pemilik; {@code null} diabaikan.
	 */
	public static void hapusDetailKegiatan(DetailKegiatan detailKegiatan, Kegiatan kegiatan) {
		if (kegiatan == null || detailKegiatan == null || detailKegiatan.getId() == null) {
			return;
		}
		kegiatan.setDetailKegiatans("," + detailKegiatan.getId() + ":false,");
		simpanPerubahanAsync(kegiatan, true, true);
	}

	/**
	 * Menyusun representasi tekstual daftar aktif dengan format
	 * {@code ",<id1>:true,<id2>:true,"} — diawali dan diakhiri koma agar pencarian substring
	 * {@code ",<id>:"} selalu tepat batasnya. Entri yang id-nya tidak dapat ditentukan
	 * ({@link #getId(Object)} mengembalikan {@code null}) dilewati.
	 *
	 * <p>Menerima {@code List} mentah karena dipakai untuk cicilan maupun detail kegiatan;
	 * pembacaan baliknya oleh {@link #ekstrakIdAktif(String)}.</p>
	 *
	 * @param list daftar entity aktif; boleh {@code null} (menghasilkan {@code ","}).
	 * @return teks daftar aktif; tidak pernah {@code null}.
	 */
	@SuppressWarnings("rawtypes")
	private static String bangunStringAktif(List list) {
		StringBuilder builder = new StringBuilder(",");
		if (list != null) {
			for (Object object : list) {
				Long id = getId(object);
				if (id != null) {
					builder.append(id).append(":true,");
				}
			}
		}
		return builder.toString();
	}

	/**
	 * Menyusun kunci entri JSON {@code tagihans} dari sisi KONFIGURASI biaya. Bila
	 * {@code pengaturanBulanan} tersedia, kunci diambil dari item biaya miliknya dan diberi sufiks
	 * {@code _<realBulan>} (menandai tagihan bulanan); bila tidak, kunci adalah id item biaya
	 * {@code detailBiaya} tanpa sufiks.
	 *
	 * <p>Kehadiran atau ketiadaan garis bawah pada kunci inilah yang dipakai
	 * {@link #murnikan(JenisKegiatan, String)} untuk memisahkan tagihan angsuran dari non-angsuran,
	 * jadi format ini bersifat kontrak, bukan sekadar penamaan.</p>
	 *
	 * @param detailBiaya       komponen biaya; dipakai bila {@code pengaturanBulanan} tidak dapat
	 *                          menyediakan kunci.
	 * @param pengaturanBulanan pengaturan pembayaran bulanan; diutamakan bila lengkap.
	 * @return kunci JSON, atau {@code null} bila item biaya tidak dapat ditentukan.
	 */
	private static String buatKeyTagihan(DetailBiaya detailBiaya, PengaturanPembayaranBulanan pengaturanBulanan) {
		if (pengaturanBulanan != null && pengaturanBulanan.getDetailBiaya() != null
				&& pengaturanBulanan.getDetailBiaya().getItemBiaya() != null
				&& pengaturanBulanan.getDetailBiaya().getItemBiaya().getId() != null) {
			String key = pengaturanBulanan.getDetailBiaya().getItemBiaya().getId().toString();
			if (pengaturanBulanan.getRealBulan() != null) {
				key += "_" + pengaturanBulanan.getRealBulan();
			}
			return key;
		}
		if (detailBiaya != null && detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getId() != null) {
			return detailBiaya.getItemBiaya().getId().toString();
		}
		return null;
	}

	/**
	 * Menyusun kunci entri JSON {@code tagihans} dari sisi DATA TAGIHAN yang sudah terbentuk.
	 * Harus menghasilkan kunci yang IDENTIK dengan
	 * {@link #buatKeyTagihan(DetailBiaya, PengaturanPembayaranBulanan)} untuk pasangan item dan
	 * bulan yang sama — bila tidak, nominal yang ditulis dari layar dan nominal hasil rekap ulang
	 * akan mendarat di kunci berbeda dan tagihan tampak berlipat.
	 *
	 * @param detail detail kegiatan sumber; {@code null}/tanpa item biaya menghasilkan {@code null}.
	 * @return kunci JSON, atau {@code null} bila item biaya tidak dapat ditentukan.
	 */
	private static String buatKeyTagihan(DetailKegiatan detail) {
		if (detail == null || detail.getItemBiaya() == null || detail.getItemBiaya().getId() == null) {
			return null;
		}
		String key = detail.getItemBiaya().getId().toString();
		if (detail.getPengaturanPembayaranBulanan() != null
				&& detail.getPengaturanPembayaranBulanan().getRealBulan() != null) {
			key += "_" + detail.getPengaturanPembayaranBulanan().getRealBulan();
		}
		return key;
	}

	/**
	 * Membulatkan nominal tagihan ke rupiah terdekat ({@code Math.round}), dengan {@code null}
	 * dipetakan ke {@code 0}. Bandingkan dengan {@link #safeLong(Double)} pada jalur rekap
	 * pembayaran yang MEMOTONG, bukan membulatkan.
	 *
	 * @param nilai nominal yang mungkin {@code null}.
	 * @return nominal bulat.
	 */
	private static long nominalTagihan(Double nilai) {
		if (nilai == null) {
			return 0L;
		}
		return Math.round(nilai.doubleValue());
	}

	/**
	 * Membaca diskon terbesar tersimpan untuk satu kunci tagihan, dengan {@code 0.0} sebagai nilai
	 * aman ketika peta atau kunci tidak tersedia.
	 *
	 * @param diskonTerbesarPerKey peta hasil {@link #kumpulkanDiskonTerbesarPerKey(Kegiatan)}.
	 * @param key                  kunci tagihan.
	 * @return diskon terbesar, atau {@code 0.0}.
	 */
	private static double ambilDiskonTerbesar(Map<String, Double> diskonTerbesarPerKey, String key) {
		if (diskonTerbesarPerKey == null || key == null) {
			return 0.0;
		}
		Double diskon = diskonTerbesarPerKey.get(key);
		return diskon == null ? 0.0 : diskon.doubleValue();
	}

	/**
	 * Memindai SELURUH {@link DetailKegiatan} milik kegiatan — <b>aktif maupun tidak aktif</b> —
	 * lalu mencatat diskon TERBESAR per kunci tagihan.
	 *
	 * <p><b>Mengapa termasuk baris non-aktif.</b> Operasi "Hitung Ulang" menonaktifkan detail lama
	 * dan membuat detail baru; pada generasi baru itu metadata diskon kerap hilang (bernilai 0)
	 * walau potongan yang sah sudah pernah diterapkan. Dengan menyimpan diskon terbesar lintas
	 * generasi, nilai diskon yang benar tetap dapat ditemukan kembali dan tagihan tidak melonjak
	 * balik ke nominal bruto.
	 *
	 * <p>Konsekuensinya: diskon yang memang sudah DICABUT tetap terbawa selama masih ada baris
	 * historis yang mencatatnya. Penjaga terhadap efek samping itu ada pada
	 * {@link #diskonEfektif(double[], double)} dan
	 * {@link #normalisasiNilaiDiskon(String, DetailKegiatan, Double, java.util.Map)}, yang hanya
	 * memakai ulang diskon historis bila nominal dasarnya belum berubah.</p>
	 *
	 * <p>Membuka session sendiri dan selalu menutupnya; kegagalan menghasilkan peta kosong
	 * (perilaku aman: tanpa diskon historis, nominal apa adanya yang dipakai).</p>
	 *
	 * @param kegiatan kegiatan sumber; {@code null}/tanpa id menghasilkan peta kosong.
	 * @return peta kunci tagihan → diskon terbesar; tidak pernah {@code null}.
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Double> kumpulkanDiskonTerbesarPerKey(Kegiatan kegiatan) {
		Map<String, Double> hasil = new HashMap<String, Double>();
		if (kegiatan == null || kegiatan.getId() == null) {
			return hasil;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<DetailKegiatan> semuaDetail = session.createCriteria(DetailKegiatan.class)
					.add(Restrictions.eq("kegiatan", kegiatan)).list();
			if (semuaDetail == null) {
				return hasil;
			}

			for (DetailKegiatan detail : semuaDetail) {
				String key = buatKeyTagihan(detail);
				if (key == null) {
					continue;
				}
				double diskon = detail.getDiskon() == null ? 0.0 : detail.getDiskon().doubleValue();
				Double tersimpan = hasil.get(key);
				if (tersimpan == null || diskon > tersimpan.doubleValue()) {
					hasil.put(key, Double.valueOf(diskon));
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}
		return hasil;
	}

	/**
	 * Menurunkan nominal tagihan sebesar SELISIH antara diskon terbesar historis dan diskon yang
	 * tercatat pada baris detail ini sendiri — bukan sebesar diskon terbesar seluruhnya.
	 *
	 * <p>Pengurangan selisih inilah yang mencegah diskon terhitung dua kali: bila baris ini sudah
	 * mencatat diskonnya sendiri, {@code nilai} yang masuk umumnya sudah neto, sehingga hanya
	 * kekurangannya terhadap diskon historis yang perlu dipotong. Hasil di-klem agar tidak pernah
	 * negatif, dan {@code nilai} {@code null} dianggap {@code 0.0}.</p>
	 *
	 * @param key                  kunci tagihan baris ini.
	 * @param detail               detail kegiatan sumber; boleh {@code null}.
	 * @param nilai                nominal sebelum normalisasi.
	 * @param diskonTerbesarPerKey peta diskon historis.
	 * @return nominal setelah normalisasi; tidak pernah negatif dan tidak pernah {@code null}.
	 */
	private static Double normalisasiNilaiDiskon(String key, DetailKegiatan detail, Double nilai,
			Map<String, Double> diskonTerbesarPerKey) {
		if (nilai == null) {
			return Double.valueOf(0.0);
		}
		double hasil = nilai.doubleValue();
		double diskonTerbesar = ambilDiskonTerbesar(diskonTerbesarPerKey, key);
		if (diskonTerbesar > 0.0) {
			double diskonDetail = detail == null || detail.getDiskon() == null ? 0.0 : detail.getDiskon().doubleValue();
			if (diskonTerbesar > diskonDetail) {
				hasil -= (diskonTerbesar - diskonDetail);
				if (hasil < 0.0) {
					hasil = 0.0;
				}
			}
		}
		return Double.valueOf(hasil);
	}

	/**
	 * Menuliskan nominal ke JSON tagihan dengan aturan penggabungan yang berbeda tergantung ada
	 * tidaknya diskon pada kunci tersebut.
	 *
	 * <ul>
	 * <li>Kunci BELUM ada: nominal ditulis apa adanya.</li>
	 * <li>Kunci sudah ada dan kunci itu PUNYA diskon historis: dipakai nilai TERKECIL antara yang
	 * lama dan yang baru ({@code Math.min}). Ini menjaga hasil tetap stabil dan tidak pernah
	 * melonjak balik ke bruto ketika generasi detail yang sedang dipindai kebetulan kehilangan
	 * metadata diskonnya.</li>
	 * <li>Kunci sudah ada dan TANPA diskon: nilai DIJUMLAHKAN. Ini menampung kasus sah beberapa
	 * baris detail berkontribusi pada satu item biaya yang sama.</li>
	 * </ul>
	 *
	 * <p>Perbedaan perlakuan ini berarti dua baris detail sah yang seharusnya dijumlahkan akan
	 * di-{@code min}-kan bila salah satunya kebetulan punya diskon tercatat. Kompromi ini
	 * disengaja: melonjaknya tagihan ke nominal bruto dinilai lebih merugikan daripada tagihan
	 * yang terlalu kecil dan mudah terlihat saat rekonsiliasi.</p>
	 *
	 * @param jsonTagihan         objek JSON tujuan; DIMUTASI.
	 * @param key                 kunci tagihan.
	 * @param nilai               nominal baru.
	 * @param diskonTerbesarPerKey peta diskon historis penentu aturan penggabungan.
	 * @throws JSONException bila penulisan ke objek JSON gagal.
	 */
	private static void putTagihanStabil(JSONObject jsonTagihan, String key, Double nilai,
			Map<String, Double> diskonTerbesarPerKey) throws JSONException {
		long nilaiBaru = nominalTagihan(nilai);
		if (!jsonTagihan.has(key)) {
			jsonTagihan.put(key, String.valueOf(nilaiBaru));
			return;
		}

		long nilaiLama = 0L;
		try {
			nilaiLama = Long.parseLong(jsonTagihan.optString(key, "0"));
		} catch (Exception e) {
			nilaiLama = 0L;
		}

		if (ambilDiskonTerbesar(diskonTerbesarPerKey, key) > 0.0 && nilaiLama >= 0L && nilaiBaru >= 0L) {
			jsonTagihan.put(key, String.valueOf(Math.min(nilaiLama, nilaiBaru)));
		} else {
			jsonTagihan.put(key, String.valueOf(nilaiLama + nilaiBaru));
		}
	}

	// ========================================================================
	// 3. PERSISTENCE ASYNC
	// ========================================================================

	/**
	 * Gerbang tunggal seluruh penulisan kolom denormalisasi — setiap perubahan di kelas ini
	 * bermuara ke sini.
	 *
	 * <p><b>Mode segera</b> ({@code immediateUpdate=true}): tugas yang sedang antre untuk kegiatan
	 * ini dibatalkan lebih dulu, lalu penulisan dijalankan SINKRON pada thread pemanggil. Dipakai
	 * saat pemanggil butuh nilai yang sudah pasti tersimpan (mis. sesudah refresh atau penghapusan).
	 *
	 * <p><b>Mode tertunda</b> ({@code immediateUpdate=false}): bila sudah ada tugas antre yang
	 * belum selesai, isinya DIPERBARUI di tempat dan tidak ada tugas baru dijadwalkan — inilah
	 * debounce yang membuat rentetan penyuntingan menghasilkan satu penulisan. Bila belum ada,
	 * satu tugas dijadwalkan {@link #ASYNC_DELAY_SECONDS} detik ke depan.
	 *
	 * <p><b>Parameter {@code refreshOrDelete} saat ini tidak dipakai</b> di badan method. Seluruh
	 * pemanggil meneruskan nilai yang sama dengan {@code immediateUpdate}, sehingga perilakunya
	 * sepenuhnya ditentukan parameter ketiga. Parameter ini dipertahankan demi kestabilan tanda
	 * tangan bagi pemanggil yang ada.</p>
	 *
	 * <p><b>Catatan konkurensi:</b> pembatalan {@code future.cancel(false)} tidak menghentikan
	 * tugas yang badannya SUDAH mulai berjalan. Bila itu terjadi bersamaan dengan mode segera,
	 * dua penulisan untuk kegiatan yang sama bisa berjalan beriringan — keduanya tetap aman karena
	 * {@link #eksekusiUpdateDenganRetry} menyerialkan lewat stripe lock dan advisory lock, dan
	 * karena isi tulisan dihitung ulang dari basis data di dalam kunci.</p>
	 *
	 * @param kegiatan        kegiatan yang kolom denormalisasinya akan ditulis; {@code null}/tanpa
	 *                        id diabaikan.
	 * @param refreshOrDelete saat ini tidak berpengaruh (lihat catatan di atas).
	 * @param immediateUpdate {@code true} untuk menulis sinkron sekarang, {@code false} untuk
	 *                        menempuh antrean debounce.
	 */
	private static void simpanPerubahanAsync(final Kegiatan kegiatan, boolean refreshOrDelete, boolean immediateUpdate) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return;
		}

		final Long id = kegiatan.getId();

		if (immediateUpdate) {
			synchronized (pendingTasks) {
				PendingKegiatanData pending = pendingTasks.remove(id);
				if (pending != null && pending.future != null) {
					pending.future.cancel(false);
				}
			}
			eksekusiUpdateDenganRetry(kegiatan, kegiatan.getCicilans(), kegiatan.getDetailKegiatans());
			return;
		}

		synchronized (pendingTasks) {
			PendingKegiatanData pending = pendingTasks.get(id);
			if (pending != null && pending.future != null && !pending.future.isDone()) {
				pending.cicilans = kegiatan.getCicilans();
				pending.detailKegiatans = kegiatan.getDetailKegiatans();
				pending.kegiatan = kegiatan;
				return;
			}

			final PendingKegiatanData data = new PendingKegiatanData(kegiatan.getCicilans(),
					kegiatan.getDetailKegiatans(), kegiatan);
			data.future = asyncExecutor.schedule(new Runnable() {
				/**
				 * Badan tugas terjadwal: mengambil DAN membuang entri terbaru milik kegiatan ini dari
				 * {@link #pendingTasks}, lalu menulis isinya. Karena yang dipakai adalah entri hasil
				 * {@code remove} — bukan salinan yang ditangkap saat penjadwalan — perubahan yang masuk selama
				 * jeda debounce ikut tertulis.
				 *
				 * <p>Bila entri sudah tidak ada (mode segera keburu membatalkan dan menghapusnya), tugas ini
				 * tidak melakukan apa-apa.</p>
				 */
				@Override
				public void run() {
					PendingKegiatanData latest = pendingTasks.remove(id);
					if (latest != null) {
						eksekusiUpdateDenganRetry(latest.kegiatan, latest.cicilans, latest.detailKegiatans);
					}
				}
			}, ASYNC_DELAY_SECONDS, TimeUnit.SECONDS);

			pendingTasks.put(id, data);
		}
	}

	/**
	 * Membungkus {@link #eksekusiUpdateDenganRetryTerkunci(Kegiatan, String, String)} dalam stripe
	 * lock per kegiatan ({@link #getKegiatanLock(Long)}), sehingga dua penulisan untuk kegiatan
	 * yang sama tidak pernah berjalan bersamaan di dalam satu JVM.
	 *
	 * @param kegiatan             kegiatan sasaran; {@code null}/tanpa id diabaikan.
	 * @param cicilansBaru         nilai kolom {@code cicilans} yang akan ditulis.
	 * @param detailKegiatansBaru  nilai kolom {@code detailKegiatans} yang akan ditulis.
	 */
	private static void eksekusiUpdateDenganRetry(Kegiatan kegiatan, String cicilansBaru, String detailKegiatansBaru) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return;
		}

		Long idKegiatan = kegiatan.getId();
		Object lock = getKegiatanLock(idKegiatan);
		synchronized (lock) {
			eksekusiUpdateDenganRetryTerkunci(kegiatan, cicilansBaru, detailKegiatansBaru);
		}
	}

	/**
	 * Membuat array 1024 objek monitor untuk penguncian ber-stripe. Jumlah stripe dipilih cukup
	 * besar agar sinkronisasi massal ribuan kegiatan jarang mengalami tabrakan palsu (dua kegiatan
	 * berbeda yang id-nya sama modulo jumlah stripe ikut terserialisasi), sementara biaya memorinya
	 * tetap dapat diabaikan.
	 *
	 * @return array monitor yang seluruhnya sudah terinisialisasi.
	 */
	private static Object[] buatKegiatanLocks() {
		// Bulk sinkronisasi pembayaran dapat memproses ribuan kegiatan bersamaan. Dengan 64
		// stripe, kegiatan yang berbeda tetapi memiliki id modulo sama ikut terserialisasi dan
		// membentuk lock convoy panjang. 1024 stripe tetap sangat kecil di memori, tetapi jauh
		// menurunkan tabrakan palsu; kegiatan dengan id yang sama tetap aman/serial.
		Object[] locks = new Object[1024];
		for (int i = 0; i < locks.length; i++) {
			locks[i] = new Object();
		}
		return locks;
	}

	/**
	 * Memetakan id kegiatan ke salah satu monitor pada {@link #kegiatanLocks} lewat modulo, dengan
	 * id negatif dinegasikan lebih dulu agar indeks tidak pernah negatif. Id {@code null} dipetakan
	 * ke stripe 0.
	 *
	 * @param idKegiatan id kegiatan; boleh {@code null}.
	 * @return monitor untuk kegiatan tersebut; tidak pernah {@code null}.
	 */
	private static Object getKegiatanLock(Long idKegiatan) {
		long value = idKegiatan == null ? 0L : idKegiatan.longValue();
		if (value < 0L) {
			value = 0L - value;
		}
		return kegiatanLocks[(int) (value % kegiatanLocks.length)];
	}

	/**
	 * Inti penulisan denormalisasi. Dipanggil hanya dari dalam stripe lock, dan mengulang hingga
	 * {@link #MAX_RETRY} kali dengan backoff berjitter bila terjadi kegagalan.
	 *
	 * <p><b>Alur satu percobaan:</b></p>
	 * <ol>
	 * <li>Buka session baru dengan {@code FlushMode.MANUAL} supaya tidak ada flush otomatis yang
	 * menulis kolom di luar kendali method ini.</li>
	 * <li>Muat baris {@link Kegiatan} dari basis data; bila sudah tidak ada, berhenti tanpa
	 * kesalahan.</li>
	 * <li>Muat cicilan dan detail kegiatan yang aktif menurut teks daftar yang diberikan.</li>
	 * <li>Bangun ulang {@code bulans} ({@link #bangunRekapPembayaran(java.util.List)}) dan
	 * {@code tagihans} ({@code bangunRekapTagihan} dengan {@code live=false} dan
	 * {@code validasiItemAsing=false}), lalu hitung {@code tagihan}, {@code dibayar}, dan
	 * {@code persentase}.</li>
	 * <li>Salin hasilnya ke object {@code kegiatan} MILIK PEMANGGIL — perlu karena penulisan
	 * memakai bulk-update HQL yang tidak menyegarkan instance mana pun.</li>
	 * <li>Bila {@link #databaseSudahSama(Session, Long, Kegiatan, String, String)} menyatakan
	 * tidak ada perubahan, selesai tanpa membuka transaksi sama sekali (idempoten dan hemat lock).</li>
	 * <li>Buka transaksi, longgarkan {@code statement_timeout} ke 300 detik dan
	 * {@code lock_timeout} ke 120 detik untuk transaksi ini saja, ambil
	 * {@code pg_advisory_xact_lock} agar penulisan juga terserialisasi LINTAS node JVM, jalankan
	 * {@link #HQL_UPDATE_KEGIATAN}, lalu commit.</li>
	 * </ol>
	 *
	 * <p><b>Mengapa timeout dilonggarkan lewat SQL, bukan {@code Query.setTimeout}:</b>
	 * {@code setTimeout} memakai {@code Statement.cancel()} yang oleh PostgreSQL dilaporkan sebagai
	 * pembatalan atas permintaan pengguna sehingga sulit dibedakan dari kegagalan sungguhan.
	 * Advisory lock dibungkus CTE dan dikembalikan sebagai skalar INTEGER karena tipe {@code void}
	 * PostgreSQL tidak dikenali auto-discovery Hibernate 3.</p>
	 *
	 * <p>Setelah {@link #MAX_RETRY} kegagalan, kesalahan hanya dilaporkan ke admin dan kolom
	 * denormalisasi dibiarkan basi — konsisten dengan sifatnya sebagai data turunan yang selalu
	 * dapat dibangun ulang.</p>
	 *
	 * @param kegiatan            kegiatan sasaran (instance milik pemanggil, akan disinkronkan).
	 * @param cicilansBaru        nilai kolom {@code cicilans} yang akan ditulis.
	 * @param detailKegiatansBaru nilai kolom {@code detailKegiatans} yang akan ditulis.
	 */
	private static void eksekusiUpdateDenganRetryTerkunci(Kegiatan kegiatan, String cicilansBaru,
			String detailKegiatansBaru) {
		Long idKegiatan = kegiatan.getId();
		int attempt = 0;
		boolean success = false;

		while (attempt < MAX_RETRY && !success) {
			attempt++;
			Session session = null;
			Transaction tx = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				session.setFlushMode(FlushMode.MANUAL);

				Kegiatan kegiatanDb = (Kegiatan) session.get(Kegiatan.class, idKegiatan);
				if (kegiatanDb == null) {
					return;
				}

				List<Long> aktifCicilan = ekstrakIdAktif(cicilansBaru);
				List<Long> aktifDetail = ekstrakIdAktif(detailKegiatansBaru);

				List<CicilanPembayaran> listCicilan = loadCicilanByIds(session, aktifCicilan);
				List<DetailKegiatan> listDetail = loadDetailKegiatanByIds(session, aktifDetail);

				RekapPembayaran rekapPembayaran = bangunRekapPembayaran(listCicilan);
					// Worker persistence hanya merangkum snapshot DetailKegiatan yang sudah ada.
					// Jangan dari sini memanggil getDetailBiayaMahasiswa untuk validasi item asing:
					// helper tersebut dapat menyinkronkan KRS/Mahasiswa lagi dan menimbulkan rantai
					// rekursif (FK KRS, NIM ganda, serta koneksi tertutup) di thread async.
					String tagihansTerbaru = bangunRekapTagihan(kegiatanDb, listDetail, false, false);

				kegiatanDb.setBulans(rekapPembayaran.bulans);
				kegiatanDb.setTagihans(tagihansTerbaru);
				kegiatanDb.setCicilans(cicilansBaru);
				kegiatanDb.setDetailKegiatans(detailKegiatansBaru);

				Double tagihan = kegiatanDb.hitungTagihan();
				Double dibayar = kegiatanDb.hitungDibayar();
				Double persentase = (tagihan != null && tagihan.doubleValue() > 0.0 && dibayar != null)
						? Double.valueOf((dibayar.doubleValue() * 100.0) / tagihan.doubleValue())
						: Double.valueOf(0.0);

				kegiatanDb.setTagihan(tagihan);
				kegiatanDb.setDibayar(dibayar);
				kegiatanDb.setPersentase(persentase);

				kegiatan.setBulans(kegiatanDb.getBulans());
				kegiatan.setTagihans(kegiatanDb.getTagihans());
				kegiatan.setTagihan(kegiatanDb.getTagihan());
				kegiatan.setDibayar(kegiatanDb.getDibayar());
				kegiatan.setPersentase(kegiatanDb.getPersentase());
				kegiatan.setCicilans(cicilansBaru);
				kegiatan.setDetailKegiatans(detailKegiatansBaru);

				if (databaseSudahSama(session, idKegiatan, kegiatanDb, cicilansBaru, detailKegiatansBaru)) {
					success = true;
					break;
				}

					tx = session.beginTransaction();
					// Query ini sering menunggu lock saat sinkronisasi pembayaran massal.
				// Query.setTimeout(45) memakai Statement.cancel(), yang oleh PostgreSQL
				// dilaporkan sebagai "canceling statement due to user request". Gunakan
				// timeout transaksi server yang lebih longgar; retry/backoff di method ini
				// tetap menjadi pengaman bila kontensi benar-benar berkepanjangan.
					session.createSQLQuery("SET LOCAL statement_timeout = '300s'").executeUpdate();
					// Banyak instalasi menetapkan lock_timeout global sangat pendek. Override hanya
					// untuk transaksi worker ini, lalu serialkan per kegiatan juga lintas node JVM.
					// Advisory lock dilepas otomatis saat commit/rollback.
					session.createSQLQuery("SET LOCAL lock_timeout = '120s'").executeUpdate();
					// pg_advisory_xact_lock mengembalikan pseudo-type PostgreSQL void
					// (JDBC Types.OTHER/1111). Hibernate 3 gagal melakukan auto-discovery
					// terhadap tipe tersebut. Bungkus pemanggilan lock dalam CTE dan
					// kembalikan scalar INTEGER yang tipenya ditentukan secara eksplisit.
					session.createSQLQuery("WITH lock_guard AS (SELECT pg_advisory_xact_lock(:lockKey)) "
							+ "SELECT 1 AS lock_acquired FROM lock_guard")
							.addScalar("lock_acquired", org.hibernate.Hibernate.INTEGER)
							.setParameter("lockKey", Long.valueOf(4200000000000L + idKegiatan.longValue()))
							.uniqueResult();
				Query query = session.createQuery(HQL_UPDATE_KEGIATAN);
				query.setParameter("nilaiBaru", kegiatanDb.getBulans());
				query.setParameter("nilaiTagihanBaru", kegiatanDb.getTagihans());
				query.setParameter("tagihanBaru", kegiatanDb.getTagihan());
				query.setParameter("dibayarBaru", kegiatanDb.getDibayar());
				query.setParameter("persentaseBaru", kegiatanDb.getPersentase());
				query.setParameter("cicilansBaru", cicilansBaru);
				query.setParameter("detailKegiatansBaru", detailKegiatansBaru);
				query.setParameter("idKegiatan", idKegiatan);
				query.executeUpdate();

				tx.commit();
				success = true;
			} catch (Exception e) {
				rollbackQuietly(tx);
				if (attempt >= MAX_RETRY) {
					Common.tampilErrorJikaAdmin(e);
				} else {
					try {
						// Backoff diperpanjang: beri waktu lebih agar lock (kegiatan/detail_biaya)
						// terlepas sebelum percobaan berikut (kontensi saat singkronkanDenganPembayaran).
						Thread.sleep(Math.min(750L * attempt, 4000L) + (long) (Math.random() * 500L));
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:903");
					}
				}
			} finally {
				closeOpenedSession(session);
			}
		}
	}

	/**
	 * Membandingkan ketujuh kolom denormalisasi yang tersimpan di basis data dengan nilai yang
	 * hendak ditulis, memakai {@link #isSama(Object, Object)} agar angka dibandingkan dengan
	 * toleransi. Bila seluruhnya sama, pemanggil melewati transaksi penulisan sepenuhnya.
	 *
	 * <p>Inilah yang membuat penulisan berulang bersifat idempoten dan tidak menimbulkan kontensi
	 * lock yang tidak perlu saat sinkronisasi massal. Kegagalan query dipetakan ke {@code false}
	 * (anggap berbeda) sehingga kesalahan pemeriksaan tidak sampai membatalkan penulisan yang sah.</p>
	 *
	 * @param session            session aktif milik pemanggil.
	 * @param idKegiatan         id kegiatan yang diperiksa.
	 * @param kegiatan           instance berisi nilai hasil hitung yang akan ditulis.
	 * @param cicilansBaru       nilai kolom {@code cicilans} yang akan ditulis.
	 * @param detailKegiatansBaru nilai kolom {@code detailKegiatans} yang akan ditulis.
	 * @return {@code true} bila basis data sudah identik dengan nilai yang hendak ditulis.
	 */
	private static boolean databaseSudahSama(Session session, Long idKegiatan, Kegiatan kegiatan, String cicilansBaru,
			String detailKegiatansBaru) {
		try {
			String hqlCek = "SELECT bulans, tagihans, tagihan, dibayar, persentase, cicilans, detailKegiatans "
					+ "FROM Kegiatan WHERE id = :id";
			Object[] current = (Object[]) session.createQuery(hqlCek).setParameter("id", idKegiatan).uniqueResult();
			if (current == null) {
				return false;
			}
			return isSama(current[0], kegiatan.getBulans()) && isSama(current[1], kegiatan.getTagihans())
					&& isSama(current[2], kegiatan.getTagihan()) && isSama(current[3], kegiatan.getDibayar())
					&& isSama(current[4], kegiatan.getPersentase()) && isSama(current[5], cicilansBaru)
					&& isSama(current[6], detailKegiatansBaru);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Memuat {@link CicilanPembayaran} berdasarkan daftar id, dipecah menjadi beberapa query
	 * berukuran maksimum {@link #MAX_IN_CLAUSE_SIZE} agar klausa {@code IN (...)} tidak melampaui
	 * batas parameter bind maupun menghasilkan rencana eksekusi yang buruk.
	 *
	 * <p>Urutan hasil mengikuti urutan pengembalian tiap potongan, bukan urutan {@code ids}.</p>
	 *
	 * @param session session aktif; {@code null} menghasilkan daftar kosong.
	 * @param ids     daftar id; {@code null}/kosong menghasilkan daftar kosong.
	 * @return daftar cicilan; tidak pernah {@code null}.
	 */
	@SuppressWarnings("unchecked")
	private static List<CicilanPembayaran> loadCicilanByIds(Session session, List<Long> ids) {
		List<CicilanPembayaran> result = new ArrayList<CicilanPembayaran>();
		if (session == null || ids == null || ids.isEmpty()) {
			return result;
		}
		for (int start = 0; start < ids.size(); start += MAX_IN_CLAUSE_SIZE) {
			int end = Math.min(start + MAX_IN_CLAUSE_SIZE, ids.size());
			List<Long> chunk = ids.subList(start, end);
			result.addAll(session.createCriteria(CicilanPembayaran.class).add(Restrictions.in("id", chunk)).list());
		}
		return result;
	}

	/**
	 * Padanan {@link #loadCicilanByIds(Session, java.util.List)} untuk {@link DetailKegiatan},
	 * dengan pemecahan potongan yang sama.
	 *
	 * @param session session aktif; {@code null} menghasilkan daftar kosong.
	 * @param ids     daftar id; {@code null}/kosong menghasilkan daftar kosong.
	 * @return daftar detail kegiatan; tidak pernah {@code null}.
	 */
	@SuppressWarnings("unchecked")
	private static List<DetailKegiatan> loadDetailKegiatanByIds(Session session, List<Long> ids) {
		List<DetailKegiatan> result = new ArrayList<DetailKegiatan>();
		if (session == null || ids == null || ids.isEmpty()) {
			return result;
		}
		for (int start = 0; start < ids.size(); start += MAX_IN_CLAUSE_SIZE) {
			int end = Math.min(start + MAX_IN_CLAUSE_SIZE, ids.size());
			List<Long> chunk = ids.subList(start, end);
			result.addAll(session.createCriteria(DetailKegiatan.class).add(Restrictions.in("id", chunk)).list());
		}
		return result;
	}

	/**
	 * Membangun rekap pembayaran: JSON {@code bulans} berisi rincian tiap cicilan, dan total
	 * {@code dibayar}.
	 *
	 * <p><b>Kunci JSON</b> berbentuk
	 * {@code <idItemBiaya>_<realBulan>_<tanggal>-<idCicilan>}, dengan {@code realBulan} bernilai
	 * {@code "0"} bila cicilan tidak terkait pengaturan bulanan. Karena id cicilan ikut masuk ke
	 * kunci, setiap pembayaran menempati entri sendiri dan tidak pernah saling menimpa.
	 *
	 * <p>Cicilan yang tidak punya item biaya, tanggal, atau id DILEWATI — sehingga baris yang belum
	 * lengkap tidak ikut menambah total. Item ber-penghitungan
	 * {@code DIKALI_NILAI_MINUS} dipaksa bernilai negatif ({@code -Math.abs}) agar berlaku sebagai
	 * pengurang, dan nominal ditulis lewat {@link #safeLong(Double)} yang MEMOTONG pecahan —
	 * sementara {@code totalDibayar} dijumlahkan dari nilai penuh sebelum pemotongan, sehingga total
	 * dan penjumlahan rincian dapat berselisih beberapa rupiah pada data berpecahan.</p>
	 *
	 * @param listCicilan cicilan aktif; boleh {@code null}.
	 * @return rekap berisi {@code bulans} dan {@code dibayar}; tidak pernah {@code null}.
	 * @throws JSONException bila penyusunan objek JSON gagal.
	 */
	private static RekapPembayaran bangunRekapPembayaran(List<CicilanPembayaran> listCicilan) throws JSONException { 
		RekapPembayaran rekap = new RekapPembayaran();
		JSONObject jsonBulans = new JSONObject();
		double totalDibayar = 0.0;

		if (listCicilan != null) {
			for (CicilanPembayaran cicilan : listCicilan) {
				if (cicilan == null || cicilan.getItemBiaya() == null || cicilan.getTanggal() == null
						|| cicilan.getId() == null) {
					continue;
				}

				String realBulan = "0";
				if (cicilan.getPengaturanPembayaranBulanan() != null
						&& cicilan.getPengaturanPembayaranBulanan().getRealBulan() != null) {
					realBulan = String.valueOf(cicilan.getPengaturanPembayaranBulanan().getRealBulan());
				}

				String key = cicilan.getItemBiaya().getId() + "_" + realBulan + "_"
						+ Common.dateFormat84.get().format(cicilan.getTanggal()) + "-" + cicilan.getId();

				Double nilai = safeDouble(cicilan.getNilai());
				if (ItemBiaya.DIKALI_NILAI_MINUS.equals(cicilan.getItemBiaya().getPenghitungan())) {
					nilai = Double.valueOf(-Math.abs(nilai.doubleValue()));
				}

				jsonBulans.put(key, String.valueOf(safeLong(nilai)));
				totalDibayar += nilai.doubleValue();
			}
		}

		rekap.bulans = jsonBulans.toString();
		rekap.dibayar = Double.valueOf(totalDibayar);
		return rekap;
	}
 
	// ============================================================
	// SELF-HEALING TAGIHAN: cegah item ASING (mis. item prodi lain yang nyangkut, atau item
	// yang sudah tidak berlaku) ikut menggelembungkan tagihan. Sebuah item DIKECUALIKAN dari
	// perhitungan HANYA bila: (1) tidak ada di daftar biaya yang berlaku untuk mahasiswa ini
	// (getDetailBiayaMahasiswa) DAN (2) belum ada pembayaran sama sekali. Item yang sudah
	// dibayar TIDAK PERNAH disentuh. Dapat dimatikan via konfigurasi 'tagihan_buang_item_asing'.
	// ============================================================
	/**
	 * Membaca gerbang konfigurasi {@code tagihan_buang_item_asing} yang mengaktifkan penyaringan
	 * item biaya "asing" (tidak berlaku bagi mahasiswa ini dan belum pernah dibayar) saat rekap
	 * tagihan dibangun. Default-nya AKTIF.
	 *
	 * <p>Kegagalan pembacaan konfigurasi dipetakan ke {@code false}, yakni fail-safe ke arah TIDAK
	 * menyaring: bila status gerbang tidak dapat dipastikan, seluruh item tetap dihitung sehingga
	 * tidak ada tagihan yang hilang secara diam-diam.</p>
	 *
	 * <p><b>Perhatian:</b> {@code Common.getKonfigurasi(nama, default)} pada basis kode ini akan
	 * MENULIS nilai default ke basis data bila kunci belum ada. Jadi pemanggilan pertama method ini
	 * dapat membuat baris konfigurasi baru sebagai efek samping.</p>
	 *
	 * @return {@code true} bila penyaringan item asing aktif.
	 */
	private static boolean buangItemAsingAktif() {
		try {
			return ais.database.model.Konfigurasi.AKTIF.equals(Common
				.getKonfigurasi("tagihan_buang_item_asing", ais.database.model.Konfigurasi.AKTIF).getNilai());
		} catch (Exception e) {
			return false;
		}
	}

	/** Id ItemBiaya yang BERLAKU utk mahasiswa kegiatan ini (incl. bulanan). null bila tak bisa ditentukan/kosong. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static java.util.Set<Long> kumpulkanItemBiayaBerlaku(Kegiatan kegiatan) {
		try {
			if (kegiatan == null || kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getId() == null
				|| kegiatan.getJenisKegiatan() == null || kegiatan.getSemster() == null) {
				return null;
			}
			java.util.Collection biayas = ais.action.master.helper.PembayaranUtilHelper.getDetailBiayaMahasiswa(
				kegiatan.getMahasiswa(), kegiatan.getSemster(), kegiatan.getJenisKegiatan(), false);
			boolean adaBulanan = false;
			if (biayas != null) {
				for (Object o : biayas) {
					if (o instanceof PengaturanPembayaranBulanan) {
						adaBulanan = true;
						break;
					}
				}
			}
			if (adaBulanan) {
				biayas = ais.action.master.helper.PembayaranUtilHelper.getDetailBiayaMahasiswa(
					kegiatan.getMahasiswa(), kegiatan.getSemster(), kegiatan.getJenisKegiatan(), "-1", false);
			}
			java.util.Set<Long> out = new java.util.HashSet<Long>();
			if (biayas != null) {
				for (Object o : biayas) {
					ItemBiaya ib = null;
					if (o instanceof DetailBiaya) {
						ib = ((DetailBiaya) o).getItemBiaya();
					} else if (o instanceof PengaturanPembayaranBulanan) {
						DetailBiaya db = ((PengaturanPembayaranBulanan) o).getDetailBiaya();
						ib = db == null ? null : db.getItemBiaya();
					}
					if (ib != null && ib.getId() != null) {
						out.add(ib.getId());
					}
				}
			}
			return out.isEmpty() ? null : out;
		} catch (Exception e) {
			return null;
		}
	}

	/** Id ItemBiaya yang SUDAH ada pembayaran (cicilan nilai>0) pada kegiatan ini. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static java.util.Set<Long> kumpulkanItemBiayaAdaPembayaran(Kegiatan kegiatan) {
		java.util.Set<Long> out = new java.util.HashSet<Long>();
		if (kegiatan == null || kegiatan.getId() == null) {
			return out;
		}
		Session s = null;
		try {
			s = HibernateUtil.getSessionFactory().openSession();
			java.util.List rows = s.createQuery(
				"select distinct c.itemBiaya.id from CicilanPembayaran c where c.kegiatan.id = :kid "
					+ "and c.nilai is not null and c.nilai > 0 and c.itemBiaya is not null")
				.setParameter("kid", kegiatan.getId()).list();
			if (rows != null) {
				for (Object o : rows) {
					if (o instanceof Number) {
						out.add(((Number) o).longValue());
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:1075");
		} finally {
			try {
				if (s != null && s.isOpen()) {
					s.close();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:1081");
			}
		}
		return out;
	}

	/** true bila DetailKegiatan ASING: item tak ada di biaya berlaku DAN belum ada pembayaran. */
	public static boolean detailKegiatanAsingTakDihitung(DetailKegiatan detail, java.util.Set<Long> itemValid,
			java.util.Set<Long> itemAdaBayar) {
		try {
			if (detail == null || detail.getItemBiaya() == null || detail.getItemBiaya().getId() == null) {
				return false;
			}
			if (itemValid == null) {
				return false;
			}
			Long ib = detail.getItemBiaya().getId();
			if (itemValid.contains(ib)) {
				return false;
			}
			if (itemAdaBayar != null && itemAdaBayar.contains(ib)) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** Daftar DetailKegiatan ASING pada kegiatan (utk pembersihan manual / tombol Bersihkan). */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static java.util.List<DetailKegiatan> cariDetailKegiatanAsing(Kegiatan kegiatan) {
		java.util.List<DetailKegiatan> out = new java.util.ArrayList<DetailKegiatan>();
		try {
			if (kegiatan == null || kegiatan.getId() == null) {
				return out;
			}
			java.util.Set<Long> valid = kumpulkanItemBiayaBerlaku(kegiatan);
			if (valid == null) {
				return out;
			}
			java.util.Set<Long> bayar = kumpulkanItemBiayaAdaPembayaran(kegiatan);
			java.util.Collection dks = kegiatan.ambilDetailKegiatan();
			if (dks != null) {
				for (Object o : dks) {
					if (o instanceof DetailKegiatan && detailKegiatanAsingTakDihitung((DetailKegiatan) o, valid, bayar)) {
						out.add((DetailKegiatan) o);
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:1131");
		}
		return out;
	}

	/**
	 * Hapus DetailKegiatan ASING (belum dibayar & tidak sesuai Setting Biaya yang berlaku) pada SATU
	 * kegiatan. Dipakai bersama oleh tombol "Bersihkan Item Tak Sesuai" per-mahasiswa
	 * (InformasiPembayaranMahasiswaAction) MAUPUN proses massal (KegiatanProsesHeper.prosesUlangTagihan,
	 * opsi "Bersihkan Item Tak Sesuai (Massal)") -- satu logika pembersihan yang sama, tidak diduplikasi.
	 * Session/transaksi dibuka & ditutup sendiri di sini (aman dipanggil dari thread worker paralel).
	 * @return jumlah baris DetailKegiatan yang benar-benar terhapus (0 bila tak ada/gagal).
	 */
	public static int bersihkanItemAsing(Kegiatan kegiatan) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return 0;
		}
		java.util.List<DetailKegiatan> asing = cariDetailKegiatanAsing(kegiatan);
		if (asing.isEmpty()) {
			return 0;
		}
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			int jumlah = 0;
			for (DetailKegiatan dk : asing) {
				if (dk == null || dk.getId() == null) {
					continue;
				}
				DetailKegiatan dkdb = (DetailKegiatan) session.get(DetailKegiatan.class, dk.getId());
				if (dkdb != null) {
					session.delete(dkdb);
					jumlah++;
				}
			}
			tx.commit();
			return jumlah;
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:bersihkanItemAsing:rollback"); }
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(bersihkan-massal-gagal) src/ais/action/master/helper/KegiatanPersistenceHelper.java:bersihkanItemAsing kegiatanId="
							+ kegiatan.getId());
			return 0;
		} finally {
			try {
				if (session != null) {
					session.close();
				}
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:bersihkanItemAsing:close"); }
		}
	}

	private static String bangunRekapTagihan(Kegiatan kegiatan, List<DetailKegiatan> listDetail) throws JSONException {
		return bangunRekapTagihan(kegiatan, listDetail, false);
	}

	/**
	 * @param live bila {@code true}, item biaya ber-rumus KRS (UTS/UAS/SKS/Matakuliah)
	 *             yang nilainya tidak diinput manual dihitung ulang mengikuti KRS terkini
	 *             (sama dengan yang tampil di grid layar). Dipakai saat mencetak bukti agar
	 *             tagihan tidak basi. Jalur async tetap memakai {@code false} (perilaku lama).
	 */
	private static String bangunRekapTagihan(Kegiatan kegiatan, List<DetailKegiatan> listDetail, boolean live)
			throws JSONException {
		return bangunRekapTagihan(kegiatan, listDetail, live, true);
	}

	/** Terapkan rekap yang sama dengan worker persistence tanpa membuka transaksi kedua. */
	private static void terapkanRekapPembayaranLokal(List<CicilanPembayaran> listCp, Kegiatan kegiatan) {
		if (kegiatan == null) {
			return;
		}
		try {
			kegiatan.setCicilans(bangunStringAktif(listCp));
			RekapPembayaran rekap = bangunRekapPembayaran(listCp);
			kegiatan.setBulans(rekap.bulans);
			kegiatan.setDibayar(rekap.dibayar);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Persentase pemenuhan pembayaran berdasarkan tagihan bersih terkini. Nilai ini
	 * dipakai untuk gerbang akademik seperti KRS agar diskon/beasiswa yang sah ikut
	 * diperhitungkan tanpa mengharuskan operator menjalankan "Hitung Ulang".
	 *
	 * <p>Bila kegiatan pembayaran sudah ada dan tagihan bersihnya nol (misalnya
	 * beasiswa menutup seluruh biaya), kewajiban dianggap terpenuhi. Bila perhitungan
	 * segar tidak tersedia, gunakan rekap aktual pada Kegiatan sebagai fallback.</p>
	 */
	public static Double hitungPersentasePemenuhanTagihan(Kegiatan kegiatan) {
		if (kegiatan == null) {
			return Double.valueOf(0.0);
		}
		try {
			Double tagihanBersih = hitungTagihanSegarKonsisten(kegiatan);
			if (tagihanBersih != null) {
				double tagihan = tagihanBersih.doubleValue();
				double dibayar = kegiatan.hitungDibayarAktualTanpaBatas().doubleValue();
				if (tagihan <= 0.01) {
					return Double.valueOf(100.0);
				}
				if (dibayar + 0.01 >= tagihan) {
					return Double.valueOf(100.0);
				}
				return Double.valueOf((dibayar * 100.0) / tagihan);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit hitungPersentasePemenuhanTagihan kegiatan=" + kegiatan.getId());
		}
		return kegiatan.hitungPersentaseLunasAktual();
	}

	private static String bangunRekapTagihan(Kegiatan kegiatan, List<DetailKegiatan> listDetail, boolean live,
			boolean validasiItemAsing) throws JSONException {
		JSONObject jsonTagihan = new JSONObject();
		Map<String, Double> diskonTerbesarPerKey = kumpulkanDiskonTerbesarPerKey(kegiatan);
		java.util.Set<Long> itemValidBerlaku = null;
		java.util.Set<Long> itemSudahAdaBayar = null;
		if (validasiItemAsing && buangItemAsingAktif()) {
			itemValidBerlaku = kumpulkanItemBiayaBerlaku(kegiatan);
			if (itemValidBerlaku != null) {
				itemSudahAdaBayar = kumpulkanItemBiayaAdaPembayaran(kegiatan);
			}
		}
		boolean kegiIsAngsuran = kegiatan != null && kegiatan.getJenisKegiatan() != null
				&& Boolean.TRUE.equals(kegiatan.getJenisKegiatan().getHanyaBerupaAngsuran());
		if (listDetail != null) {
			for (DetailKegiatan detail : listDetail) {
				if (detail == null || detail.getItemBiaya() == null) {
					continue;
				}

				if (detailKegiatanAsingTakDihitung(detail, itemValidBerlaku, itemSudahAdaBayar)) {
					try {
						System.out.println("[bangunRekapTagihan] Item ASING dilewati (tak berlaku & belum dibayar) kegiatan="
							+ (kegiatan == null ? "?" : String.valueOf(kegiatan.getId())) + " item="
							+ detail.getItemBiaya().getId() + " " + detail.getItemBiaya().getNama());
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:1171");
					}
					continue;
				}

				// Mode angsuran: lewati DK tanpa PPB agar tidak menambah key non-"_" ke tagihans
				if (kegiIsAngsuran && detail.getPengaturanPembayaranBulanan() == null) {
					continue;
				}

				String key = buatKeyTagihan(detail);
				if (key == null) {
					continue;
				}

				Double nilai = hitungJumlahTagihan(kegiatan, detail, live);
				nilai = normalisasiNilaiDiskon(key, detail, nilai, diskonTerbesarPerKey);
				if (ItemBiaya.DIKALI_NILAI_MINUS.equals(detail.getItemBiaya().getPenghitungan())) {
					nilai = Double.valueOf(-Math.abs(nilai.doubleValue()));
				}

				putTagihanStabil(jsonTagihan, key, nilai, diskonTerbesarPerKey);
			}
		}
		String hasilTagihan = murnikan(kegiatan == null ? null : kegiatan.getJenisKegiatan(), jsonTagihan.toString());
		hasilTagihan = lindungiNilaiDiskonTersimpan(kegiatan, hasilTagihan, diskonTerbesarPerKey);

		// LINDUNGI dari hasil KOSONG yang TRANSIEN. Akar masalah "tagihan dasbor BOLAK-BALIK
		// BENAR/SALAH": saat "Hitung Ulang"/recompute, engine menonaktifkan DetailKegiatan lama
		// (DetailKegiatan.getKodeUnik() mengembalikan null saat aktif=false) lalu membuat/meng-
		// aktifkan DK baru. Pada jendela transisi itu, daftar DK AKTIF (sumber listDetail) bisa
		// SEMENTARA KOSONG -> map tagihan terbangun {} -> menimpa nilai tersimpan yang BENAR
		// (mis. {"199":"1800000",...}) -> dasbor menampilkan BRUTO. Klik berikutnya benar lagi.
		// Bila rebuild menghasilkan KOSONG padahal nilai 'tagihans' TERSIMPAN tidak kosong,
		// JANGAN timpa dengan kosong -> pertahankan nilai tersimpan yang valid (idempoten/stabil).
		if (hasilTagihan == null || hasilTagihan.trim().isEmpty() || hasilTagihan.trim().equals("{}")) {
			String tersimpan = kegiatan == null ? null : kegiatan.getTagihans();
			if (tersimpan != null && !tersimpan.trim().isEmpty() && !tersimpan.trim().equals("{}")) {
				return tersimpan;
			}
		}
		return hasilTagihan;
	}

	private static String lindungiNilaiDiskonTersimpan(Kegiatan kegiatan, String hasilTagihan,
			Map<String, Double> diskonTerbesarPerKey) {
		if (kegiatan == null || diskonTerbesarPerKey == null || diskonTerbesarPerKey.isEmpty()
				|| hasilTagihan == null || hasilTagihan.trim().length() == 0 || "{}".equals(hasilTagihan.trim())) {
			return hasilTagihan;
		}

		String tersimpan = kegiatan.getTagihans();
		if (tersimpan == null || tersimpan.trim().length() == 0 || "{}".equals(tersimpan.trim())) {
			return hasilTagihan;
		}

		try {
			JSONObject jsonBaru = new JSONObject(hasilTagihan);
			JSONObject jsonLama = new JSONObject(tersimpan);
			Iterator<?> iterator = jsonLama.keys();
			boolean berubah = false;
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				if (ambilDiskonTerbesar(diskonTerbesarPerKey, key) <= 0.0 || !jsonBaru.has(key)) {
					continue;
				}
				long nilaiLama = Long.parseLong(jsonLama.optString(key, "0"));
				long nilaiBaru = Long.parseLong(jsonBaru.optString(key, "0"));
				if (nilaiLama >= 0L && nilaiBaru > nilaiLama) {
					jsonBaru.put(key, String.valueOf(nilaiLama));
					berubah = true;
				}
			}
			return berubah ? jsonBaru.toString() : hasilTagihan;
		} catch (Exception e) {
			return hasilTagihan;
		}
	}

	private static Double hitungJumlahTagihan(Kegiatan kegiatan, DetailKegiatan detail) {
		return hitungJumlahTagihan(kegiatan, detail, false);
	}

	private static Double hitungJumlahTagihan(Kegiatan kegiatan, DetailKegiatan detail, boolean live) {
		if (detail == null) {
			return Double.valueOf(0.0);
		}
		try {
			if (detail.getPengaturanPembayaranBulanan() == null) {
				DetailBiaya detailBiaya = detail.getDetailBiaya();
				if (live && bolehHitungUlangLive(detailBiaya)) {
					// Sama dengan tampilan grid layar: segarkan nilaiBiayaBaru lalu pakai
					// resolusi tanpa detailKegiatan (memakai hitungan rumus, bukan biaya beku).
					segarkanNilaiBiayaBaru(kegiatan, detailBiaya);
					return Kegiatan.ambilJumlahTagihan((DetailKegiatan) null, kegiatan, detailBiaya, false);
				}
				return Kegiatan.ambilJumlahTagihan(detail, kegiatan, detailBiaya, false);
			}
			return Kegiatan.ambilJumlahTagihan(detail, detail.getDetailBiaya(), kegiatan,
					kegiatan == null ? null : kegiatan.getMahasiswa(),
					kegiatan == null ? null : kegiatan.getSemster(), detail.getPengaturanPembayaranBulanan());
		} catch (Exception e) {
			return Double.valueOf(0.0);
		}
	}

	/**
	 * Item biaya yang boleh dihitung ulang LIVE mengikuti KRS terkini: item ber-rumus
	 * (UTS/UAS/SKS/Matakuliah) yang nilainya TIDAK diinput manual ({@code nilaiBisaDiubah=false}).
	 * Item tanpa penghitungan (flat), tunggakan semester lalu, dan item yang nilainya
	 * bisa diubah manual TIDAK ikut dihitung ulang agar nilai manual/diskon tidak hilang.
	 */
	private static boolean bolehHitungUlangLive(DetailBiaya detailBiaya) {
		try {
			if (detailBiaya == null || detailBiaya.getItemBiaya() == null) {
				return false;
			}
			ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
			if (Boolean.TRUE.equals(itemBiaya.getNilaiBisaDiubah())) {
				return false;
			}
			if (PembayaranNominalModifikasiHelper.isTanpaPenghitungan(itemBiaya)) {
				return false;
			}
			String penghitungan = itemBiaya.getPenghitungan();
			if (ItemBiaya.HITUNG_TUNGGAKAN_SMT_LALU.equals(penghitungan)) {
				return false;
			}
			if (itemBiaya.getParameterTambahan() != null) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/** Hitung ulang nilaiBiayaBaru detailBiaya mengikuti KRS terkini (idempoten, aman dipanggil ulang). */
	private static void segarkanNilaiBiayaBaru(Kegiatan kegiatan, DetailBiaya detailBiaya) {
		try {
			if (kegiatan == null || detailBiaya == null) {
				return;
			}
			Mahasiswa mahasiswa = kegiatan.getMahasiswa();
			Integer semester = kegiatan.getSemster();
			if (mahasiswa == null || mahasiswa.getId() == null || semester == null) {
				return;
			}
			detailBiaya.updateKeterangan(mahasiswa, semester);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Hitung ulang tagihan kegiatan secara LIVE (mengikuti KRS terkini, sama dengan grid
	 * layar) lalu simpan ke kolom denormalisasi yang dibaca laporan bukti pembayaran:
	 * {@code tagihans, tagihan, dibayar, persentase, amount, amountTerhutang}. Dipakai
	 * sebelum mencetak agar JUMLAH TAGIHAN pada PDF sama dengan layar.
	 *
	 * <p>Hanya item ber-rumus KRS non-manual yang ikut dihitung ulang (lihat
	 * {@link #bolehHitungUlangLive}); item flat/manual/tunggakan tidak berubah. Aman
	 * dipanggil dari thread web (session terkelola). Mengembalikan {@code true} bila berhasil.</p>
	 */
	public static boolean segarkanTagihanLive(Long kegiatanId) {
		if (kegiatanId == null) {
			return false;
		}
		Object lock = getKegiatanLock(kegiatanId);
		synchronized (lock) {
			Session session = null;
			Transaction tx = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				session.setFlushMode(FlushMode.MANUAL);

				Kegiatan kegiatan = (Kegiatan) session.get(Kegiatan.class, kegiatanId);
				if (kegiatan == null) {
					return false;
				}

				List<Long> aktifDetail = ekstrakIdAktif(kegiatan.getDetailKegiatans());
				List<DetailKegiatan> listDetail = loadDetailKegiatanByIds(session, aktifDetail);

				String tagihansTerbaru = bangunRekapTagihan(kegiatan, listDetail, true);
				kegiatan.setTagihans(tagihansTerbaru);

				Double tagihan = kegiatan.hitungTagihan();
				if (tagihan == null) {
					tagihan = Double.valueOf(0.0);
				}
				Double dibayar = kegiatan.getDibayar();
				if (dibayar == null) {
					dibayar = Double.valueOf(0.0);
				}
				Double sisa = Double.valueOf(tagihan.doubleValue() - dibayar.doubleValue());
				if (sisa.doubleValue() < 0.0) {
					sisa = Double.valueOf(0.0);
				}
				Double persentase = (tagihan.doubleValue() > 0.0)
						? Double.valueOf((dibayar.doubleValue() * 100.0) / tagihan.doubleValue())
						: Double.valueOf(0.0);

				tx = session.beginTransaction();
				Query query = session.createQuery("UPDATE Kegiatan SET tagihans = :tagihans, tagihan = :tagihan, "
						+ "persentase = :persentase, amount = :amount, amountTerhutang = :amountTerhutang "
						+ "WHERE id = :idKegiatan");
				query.setParameter("tagihans", tagihansTerbaru);
				query.setParameter("tagihan", tagihan);
				query.setParameter("persentase", persentase);
				query.setParameter("amount", dibayar);
				query.setParameter("amountTerhutang", sisa);
				query.setParameter("idKegiatan", kegiatanId);
				query.executeUpdate();
				tx.commit();
				return true;
			} catch (Exception e) {
				rollbackQuietly(tx);
				Common.tampilErrorJikaAdmin(e);
				return false;
			} finally {
				closeOpenedSession(session);
			}
		}
	}

	// ========================================================================
	// 4. REUSE UNTUK KegiatanProsesHeper.singkronkanDataCicilan
	// ========================================================================

	@SuppressWarnings("unchecked")
	public static Map<Long, List<Long>> ambilPetaCicilanPerKegiatan() {
		Map<Long, List<Long>> result = new HashMap<Long, List<Long>>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Query query = session.createSQLQuery(
					"SELECT id, kegiatan FROM cicilan_pembayaran WHERE kegiatan IS NOT NULL ORDER BY kegiatan DESC");
			query.setFetchSize(1000);

			List<Object[]> rows = query.list();
			if (rows == null) {
				return result;
			}

			for (Object[] row : rows) {
				if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
					continue;
				}
				Long cicilanId = Long.valueOf(((Number) row[0]).longValue());
				Long kegiatanId = Long.valueOf(((Number) row[1]).longValue());
				List<Long> ids = result.get(kegiatanId);
				if (ids == null) {
					ids = new ArrayList<Long>();
					result.put(kegiatanId, ids);
				}
				ids.add(cicilanId);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeOpenedSession(session);
		}
		return result;
	}

	public static boolean sinkronkanCicilanKegiatanLangsung(Long kegiatanId, List<Long> cicilanIds) {
		if (kegiatanId == null) {
			return false;
		}

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			session.setFlushMode(FlushMode.MANUAL);

			Kegiatan kegiatan = (Kegiatan) session.get(Kegiatan.class, kegiatanId);
			if (kegiatan == null) {
				return false;
			}

			String cicilansBaru = bangunStringAktifDariIds(cicilanIds);
			List<CicilanPembayaran> listCicilan = loadCicilanByIds(session,
					cicilanIds == null ? new ArrayList<Long>() : cicilanIds);
			RekapPembayaran rekap = bangunRekapPembayaran(listCicilan);

			kegiatan.setCicilans(cicilansBaru);
			kegiatan.setBulans(rekap.bulans);
			Double dibayar = rekap.dibayar;
			try {
				dibayar = kegiatan.hitungDibayar();
			} catch (Exception e) {
				dibayar = rekap.dibayar;
			}
			kegiatan.setDibayar(dibayar);

			tx = session.beginTransaction();
			session.createSQLQuery("UPDATE kegiatan SET bulans = :bulans, cicilans = :cicilans, dibayar = :dibayar "
					+ "WHERE id = :id")
					.setParameter("bulans", kegiatan.getBulans() == null ? "{}" : kegiatan.getBulans())
					.setParameter("cicilans", cicilansBaru)
					.setParameter("dibayar", kegiatan.getDibayar() == null ? Double.valueOf(0.0) : kegiatan.getDibayar())
					.setParameter("id", kegiatanId).executeUpdate();
			tx.commit();
			return true;
		} catch (Exception e) {
			rollbackQuietly(tx);
			Common.tampilErrorJikaAdmin(e);
			return false;
		} finally {
			closeOpenedSession(session);
		}
	}

	private static String bangunStringAktifDariIds(List<Long> ids) {
		StringBuilder builder = new StringBuilder(",");
		if (ids != null) {
			for (Long id : ids) {
				if (id != null) {
					builder.append(id).append(":true,");
				}
			}
		}
		return builder.toString();
	}

	// ========================================================================
	// 5. UTILITY PUBLIK
	// ========================================================================

	public static List<Long> ekstrakIdAktif(String data) {
		List<Long> list = new ArrayList<Long>();
		if (!isEmpty(data)) {
			String[] partsData = data.split(",");
			for (int i = 0; i < partsData.length; i++) {
				String part = partsData[i];
				if (isEmpty(part)) {
					continue;
				}
				String[] parts = part.split(":");
				try {
					Long id = Long.valueOf(parts[0].trim());
					boolean aktif = parts.length > 1 ? Boolean.parseBoolean(parts[1].trim()) : true;
					if (aktif) {
						list.add(id);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KegiatanPersistenceHelper.java:1518");
				}
			}
		}
		return list;
	}

	public static String murnikan(JenisKegiatan jenisKegiatan, String tagihans) {
		if (jenisKegiatan == null || isEmpty(tagihans)) {
			return tagihans;
		}

		boolean hanyaAngsuran = Boolean.TRUE.equals(jenisKegiatan.getHanyaBerupaAngsuran());
		boolean hanyaBukanAngsuran = Boolean.TRUE.equals(jenisKegiatan.getHanyaBerupaBukanAngsuran());

		if (!hanyaAngsuran && !hanyaBukanAngsuran) {
			return tagihans;
		}

		try {
			JSONObject oldJson = new JSONObject(tagihans);
			JSONObject newJson = new JSONObject();
			Iterator<?> iterator = oldJson.keys();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				if (((hanyaAngsuran && key.contains("_")) || (hanyaBukanAngsuran && !key.contains("_")))
						&& !oldJson.isNull(key)) {
					newJson.put(key, oldJson.get(key));
				}
			}
			return newJson.toString();
		} catch (Exception e) {
			return tagihans;
		}
	}

	private static boolean isSama(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1 instanceof Number && o2 instanceof Number) {
			double d1 = ((Number) o1).doubleValue();
			double d2 = ((Number) o2).doubleValue();
			return Math.abs(d1 - d2) < 0.0001;
		}
		return o1.equals(o2);
	}
}
