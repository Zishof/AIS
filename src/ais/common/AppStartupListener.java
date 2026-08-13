package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;

public class AppStartupListener implements ServletContextListener {

	private Boolean truncareAccessedUsers = false;
	public boolean hasTruncate = false;

	private static final Object LOCK_SKIN = new Object();
	private static volatile boolean prosesSkin = false;
	private static volatile long lastCheckTime = 0L;
	private static volatile boolean fileExistsCache = true;
	private static volatile File markerFile = null;
	private static final long CHECK_INTERVAL = 5000L;

	private static final Object LOCK_MAINTENANCE = new Object();
	private static volatile Thread maintenanceThread = null;
	private static volatile boolean contextStopping = false;

	// True selama proses bootstrap (contextInitialized). Dipakai GeneralValueObject.retreive
	// untuk MELEWATI baca-file atribut (idfinger/googleScholar/dll) saat startup: nilai file
	// tidak dibutuhkan untuk dirty-check Hibernate, sehingga auto-flush tidak meledak menjadi
	// ribuan File.exists (penyebab startup lambat/macet di thread "main").
	private static volatile boolean startupInProgress = false;

	public static boolean isStartupInProgress() {
		return startupInProgress;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		contextStopping = false;
		startupInProgress = true;

		System.out.println("=========================================");
		System.out.println("Aplikasi sedang Bootstrap!");
		System.out.println("Inisialisasi database, load config, dll...");
		System.out.println("=========================================");

		// initConfig() WAJIB sinkron: menyiapkan path & properties fondasi (Common.ROOT/
		// REAL_PATH, dll) yang dibutuhkan semua langkah berikutnya. Cepat (baca file lokal).
		try {
			initConfig();
			BacaTulisUtil.mulai = true;
		} catch (Throwable e) {
			startupInProgress = false;
			System.err.println("Error fatal saat initConfig: " + e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:70");
			throw new RuntimeException("Bootstrap aplikasi gagal (initConfig)", e);
		}

		// Init data BERAT (load reference data, enkripsi, menu, skin/branding) dipindah ke
		// THREAD LATAR agar Tomcat SELESAI start & connector langsung melayani request tanpa
		// menunggu init data. Data yang belum termuat akan dimuat ON-DEMAND oleh tiap fitur.
		// Bisa dikembalikan ke mode sinkron dengan -Dais.init.async=false.
		final ServletContextEvent sceRef = sce;
		boolean async = !"false".equalsIgnoreCase(System.getProperty("ais.init.async", "true"));
		if (async) {
			Thread initThread = new Thread(new Runnable() {
				@Override
				public void run() {
					jalankanInitData(sceRef);
				}
			}, "AIS-Init-Data");
			initThread.setDaemon(true);
			initThread.start();
			System.out.println("Bootstrap aplikasi selesai (init data BERJALAN DI LATAR; siap melayani).");
		} else {
			jalankanInitData(sceRef);
		}
	}

	/**
	 * Init data berat aplikasi. Dijalankan di thread latar (default) atau sinkron
	 * (-Dais.init.async=false). startupInProgress tetap true selama proses ini agar
	 * baca/tulis file via getter/setter ter-map dilewati (lihat GeneralValueObject).
	 */
	private void jalankanInitData(ServletContextEvent sce) {
		boolean startupSukses = false;
		try {
			// PERCEPAT STARTUP: bangun SessionFactory Hibernate (1300+ mapping = bagian terlama
			// inisiasi) DI THREAD LATAR ini, sehingga thread startup Tomcat tidak menunggu. Komponen
			// lain (ZK loader, request pertama) memakai getSessionFactory() yang ter-sinkron → factory
			// dibangun SEKALI di sini, sisanya tinggal memakai ulang.
			try {
				HibernateUtil.getSessionFactory();
			} catch (Throwable e) {
				System.err.println(
						"Warm-up SessionFactory gagal (lanjut, akan dibangun on-demand): " + e.getMessage());
			}
			// Migrasi kecil dan idempoten ini harus dijalankan segera setelah
			// SessionFactory tersedia, sebelum init data/menu yang berat. Dengan
			// demikian promo "Semua Produk" tidak perlu menunggu seluruh maintenance
			// index selesai di thread latar.
			InitIndex.initAturanDiskonProdukNullable();
			InitIndex.initKebijakanReturProduk();

			ConstantValues.initNativeSesion();

			Common.encripSemua();

			// SEMENTARA DINONAKTIFKAN (permintaan user 17-07-2026): cleanupObsoleteMenus()
			// terbukti menghapus menu yang salah. JANGAN diaktifkan kembali sebelum daftar
			// ID-nya diverifikasi ulang terhadap data menu live.
			// MenuHelper.cleanupObsoleteMenus();
			// Pulihkan baris menu yang HILANG dari snapshot resmi Menu_260712042919.xlsx
			// (1.210 baris) — hanya INSERT id yang belum ada, tidak menimpa yang ada.
			MenuHelper.ensureMenusDariSnapshot();
			MenuHelper.ensureAkreditasiMenus();
			MenuHelper.ensureAntarJemputMenus();
			MenuHelper.ensureKantinMenus();
			MenuHelper.ensureSimpanPinjamMenus();
			MenuHelper.ensurePklSiswaMenu();
			MenuHelper.ensureRepositoryMenus();
			MenuHelper.ensureSirsDashboardMenus();
			MenuHelper.healSirsMenuUrls();
			MenuHelper.ensureDokterMenus();
			MenuHelper.ensurePenjaminanMutuAnalisisMenu();
			MenuHelper.ensureKursusMarketplaceMenus();

			// Modul Satuan Pengawasan Internal (SPI) — Bagian A: role & menu Setup SPI.
			// Role harus di-seed dulu SEBELUM menu agar ensurePrivilege langsung berhasil
			// memasang hak akses penuh pada pemanggilan startup pertama kali.
			MenuHelper.ensureSpiRole();
			MenuHelper.ensureSpiMenus();

			// Hapus permanen menu DUPLIKAT "Pengumuman Akademik" id 750287 (duplikat dari 186
			// "Pengumuman Akademis" ber-url sama). Sudah dibuang juga dari MenuSnapshotData.
			MenuHelper.hapusMenuDuplikatBawaan();

			// SEMENTARA (permintaan user 18-07-2026): modul "Sistem Informasi Rumah Sakit" (SIRS)
			// disembunyikan default tiap startup (induk aktif=false) sampai user memerintahkan
			// mengaktifkan kembali. DIPANGGIL TERAKHIR agar tidak ada seeder di atas yang mengubah
			// status induk SIRS setelahnya. Lihat MenuHelper.sembunyikanMenuSirsSementara().
			MenuHelper.sembunyikanMenuSirsSementara();

			// Seed data awal Jenis Lokasi (Gudang/Kasir/Entry Data/Logistik/Keuangan/dll) untuk modul
			// pergudangan e-Kantin — idempoten (hanya menambah yang belum ada). Kegagalan tidak
			// menggagalkan startup (dicoba lagi pada startup berikutnya).
			try {
				ais.action.master.koperasi.helper.LokasiKantinUtil.ensureJenisLokasiDefault();
			} catch (Throwable e) {
				System.err.println("Seed JenisLokasi default gagal (lanjut): " + e.getMessage());
			}

			// Data contoh modul Satuan Pengawasan Internal (SPI) — idempoten (hanya berjalan bila
			// belum ada satu pun Jenis Audit SPI, baik hasil seeding ini maupun data asli).
			// Kegagalan tidak menggagalkan startup (dicoba lagi pada startup berikutnya).
			try {
				ais.action.master.spi.SpiSampleDataHelper.ensureSampleData();
			} catch (Throwable e) {
				System.err.println("Seed data contoh SPI gagal (lanjut): " + e.getMessage());
			}

			initSkin();

			if (sce != null && sce.getServletContext() != null) {
				sce.getServletContext().setAttribute("appStartTime", Long.valueOf(System.currentTimeMillis()));
			}

			startupSukses = true;
			System.out.println("Init data aplikasi selesai.");

			// Hangatkan cache angka ringkasan kampus (mahasiswa/dosen/calon TA berjalan) di latar,
			// agar dashboard profil membaca dari memori (cocok dgn LaporanRekapJumlahMahasiswa).
			try {
				RingkasanKampusCache.warmupStartup();
			} catch (Throwable e) {
				System.err.println("Warm-up RingkasanKampusCache gagal (lanjut): " + e.getMessage());
			}

			// Warm-up cache flag yg diakses <1 minggu (paralel, di latar) + simpan daftar akses
			// ke berkas {catalina.base}/cache/{Common.ROOT} tiap 15 menit (tanpa ALTER tabel).
			try {
				FlagAccessCache.mulaiPenyimpananBerkala();
				FlagAccessCache.warmupStartup();
			} catch (Throwable e) {
				System.err.println("Warm-up FlagAccessCache gagal (lanjut): " + e.getMessage());
			}

			// Jadwalkan penyimpanan berkala riwayat akses ID entity (3 hari) ke
			// {catalina.base}/cache/{Common.ROOT}/last_id_<Class>.dat. Tidak perlu warmup terpisah:
			// EntityAccessCache.ambilIdTerakhir dipanggil LANGSUNG (sinkron, baca berkas) oleh
			// InitDataHelper.doInitData saat preload per-kelas, tidak bergantung pada scheduler ini —
			// baris di sini HANYA menjadwalkan penulisan berkala (tak memengaruhi urutan pembacaan).
			try {
				EntityAccessCache.mulaiPenyimpananBerkala();
			} catch (Throwable e) {
				System.err.println("Warm-up EntityAccessCache gagal (lanjut): " + e.getMessage());
			}

			// Cache PERSISTEN e-Learning: muat angka ringkasan terakhir dari DISK lebih dulu (agar
			// halaman e-Learning tampil INSTAN sesaat setelah restart, sebelum warm DB selesai) +
			// jadwalkan penyimpanan berkala ke disk.
			try {
				ElearningFlagAccessCache.muatStartup();
				ElearningFlagAccessCache.mulaiPenyimpananBerkala();
			} catch (Throwable e) {
				System.err.println("ElearningFlagAccessCache gagal (lanjut): " + e.getMessage());
			}

			// Warm-up agregat per-pertemuan dashboard e-Learning (akses + kehadiran) di latar,
			// untuk MENYEGARKAN angka dari database (data dari disk dipakai untuk tampil cepat dulu).
			try {
				ElearningRingkasanCache.warmupStartup();
			} catch (Throwable e) {
				System.err.println("Warm-up ElearningRingkasanCache gagal (lanjut): " + e.getMessage());
			}

			// Warm-up snapshot pemberitahuan (lonceng header + pusat notifikasi JSP) di latar,
			// agar akses pertama setelah restart tidak menembak DB (tanpa hambatan/cold start).
			try {
				NotifikasiCache.warmupStartup();
			} catch (Throwable e) {
				System.err.println("Warm-up NotifikasiCache gagal (lanjut): " + e.getMessage());
			}

			// Penjadwal ARO (Automatic Roll Over) simpanan berjangka koperasi: perpanjang/otomatis
			// tandai deposito jatuh tempo tiap hari di latar (daemon).
			try {
				DepositoAroScheduler.mulai();
			} catch (Throwable e) {
				System.err.println("Penjadwal ARO deposito gagal dimulai (lanjut): " + e.getMessage());
			}

			// Penjadwal ambang stok gudang (fitur "Purchase" gap analisis PDF klien 2026-07-26):
			// notifikasi + pengajuan pembelian otomatis 2 tingkat saat stok bahan baku menipis.
			try {
				StokThresholdScheduler.mulai();
			} catch (Throwable e) {
				System.err.println("Penjadwal ambang stok gudang gagal dimulai (lanjut): " + e.getMessage());
			}

			// Agregasi otomatis skripsi, pustaka, buku ajar, artikel, serta hasil
			// penelitian/pengabdian yang sudah layak publik ke repository lokal.
			try {
				ais.action.master.repository.RepositorySyncScheduler.mulai();
			} catch (Throwable e) {
				System.err.println("Penjadwal sinkron repository gagal dimulai (lanjut): " + e.getMessage());
			}
		} catch (Throwable e) {
			// Tidak mematikan Tomcat: data yang gagal/menunggu akan dimuat on-demand.
			System.err.println("Error saat init data aplikasi (lanjut, data dimuat on-demand): " + e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:209");
		} finally {
			startupInProgress = false;
			closeHibernateSessionQuietly();
			closeStreamingSessionQuietly();
		}

		if (startupSukses && !contextStopping) {
			startBackgroundMaintenance();
		}

		// SEED penanda dedup ErrorAudit SEKALI dari DB (tabel error_log) supaya error di lokasi yang sama
		// tidak tercatat ulang tiap kali aplikasi restart. Dijalankan di thread singkat (selesai lalu
		// berhenti — bukan pool, tak bocor) agar tidak menghambat proses startup.
		try {
			Thread seedThread = new Thread(new Runnable() {
				public void run() {
					ais.common.ErrorAuditUtil.seedDedupFromDatabase();
				}
			}, "errorlog-dedup-seed");
			seedThread.setDaemon(true);
			seedThread.start();
		} catch (Throwable seedEx) {
			// abaikan: dedup tetap berfungsi tanpa seed (lokasi lama akan tercatat sekali lagi).
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		System.out.println("Aplikasi dimatikan. Membersihkan resource...");
		contextStopping = true;

		// Flush semua tulis flag ASINKRON (BacaTulisUtil) ke DB sebelum pool koneksi
		// ditutup, agar tidak ada flag yang hilang saat shutdown.
		try {
			BacaTulisUtil.flushPendingWrites();
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:230");
		}

		// Hentikan thread init-cache statis (AIS-Common-InitCache) agar tidak
		// menyentuh classloader yang sudah berhenti saat shutdown/redeploy/reload.
		try {
			Common.tandaiAplikasiBerhenti();
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:237");
		}

		// Hentikan timer terjadwal Spring (timerFactory) LEBIH DULU, sebelum resource
		// (MapDB cache, pool koneksi) ditutup di bawah — supaya tidak ada task latar
		// yang menembak resource tertutup ("already closed"/"has been closed") dan agar
		// thread "timerFactory" benar-benar berhenti (cegah warning memory-leak Tomcat).
		hentikanSpringTimer(sce);

		stopBackgroundMaintenance();
		try {
			PerformaSnapshotUtil.stopScheduler();
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:249");
		}
		closeHibernateSessionQuietly();
		closeStreamingSessionQuietly();

		// Hentikan scheduler saver bawaan aplikasi (flag-access-saver / elearning-ringkasan-saver),
		// lalu TUTUP SessionFactory (utama + streaming) supaya pool koneksi c3p0 berhenti, dan
		// terakhir SHUTDOWN CacheManager EhCache singleton (region cache Hibernate). Tanpa ini,
		// thread-thread tersebut tetap hidup setelah webapp stop → Tomcat melaporkan "failed to
		// stop it" (kebocoran classloader). Lihat catatan tiap helper.
		stopSaverSchedulers();
		shutdownSessionFactoriesQuietly();
		shutdownEhcacheCacheManagersQuietly();

		shutdownOptionalMemoryCache();
		unregisterJdbcDrivers();
	}

	/**
	 * Batalkan bean Spring "timerFactory" (sebuah {@link java.util.Timer}) jika masih
	 * tersedia. Aman dipanggil walau konteks Spring sudah ditutup (di-try/catch).
	 */
	private void hentikanSpringTimer(ServletContextEvent sce) {
		try {
			if (sce == null || sce.getServletContext() == null) {
				return;
			}
			org.springframework.web.context.WebApplicationContext wac = org.springframework.web.context.support.WebApplicationContextUtils
					.getWebApplicationContext(sce.getServletContext());
			if (wac == null) {
				return;
			}
			String[] namaBean = { "timerFactory" };
			for (int i = 0; i < namaBean.length; i++) {
				try {
					if (wac.containsBean(namaBean[i])) {
						Object bean = wac.getBean(namaBean[i]);
						if (bean instanceof java.util.Timer) {
							((java.util.Timer) bean).cancel();
							System.out.println("Spring timer '" + namaBean[i] + "' dibatalkan saat shutdown.");
						}
					}
				} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:291");
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:294");
		}
	}

	private void startBackgroundMaintenance() {
		synchronized (LOCK_MAINTENANCE) {
			if (contextStopping) {
				return;
			}
			if (maintenanceThread != null && maintenanceThread.isAlive()) {
				return;
			}

			maintenanceThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if (contextStopping || Thread.currentThread().isInterrupted()) {
							return;
						}

						System.out.println("Memulai proses maintenance startup: index dan query pendukung.");
						// Inisialisasi INDEX DB dijalankan di THREAD TERPISAH (paralel terhadap
						// initEksekusiQuery) dan secara internal juga paralel (pool kecil) -> tidak
						// memblokir maintenance lain + pembuatan index jauh lebih cepat.
						Thread indexThread = new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									InitIndex.initEksekusiQueryIndex();
									System.out.println("Inisialisasi index DB (paralel) selesai.");
								} catch (Throwable e) {
									System.err.println("Inisialisasi index DB gagal (lanjut): " + e.getMessage());
								}
							}
						}, "init-index-startup");
						indexThread.setDaemon(true);
						indexThread.start();

						if (contextStopping || Thread.currentThread().isInterrupted()) {
							return;
						}

						InitDataHelper.initEksekusiQuery();
						System.out.println("Proses maintenance startup selesai.");

						// FASE 4 (warmup e-Learning): panaskan cache koleksi anak pertemuan jendela sekarang di
						// thread latar prioritas rendah, agar akses pertama pasca-restart tidak membayar query
						// reInit* (N+1) yang mahal. Non-blocking + non-kritis (kegagalan di-swallow di dalam).
						if (!contextStopping && !Thread.currentThread().isInterrupted()) {
							try {
								ElearningWarmupHelper.jalankan();
							} catch (Throwable e) {
								System.err.println("Warmup e-Learning gagal dimulai (lanjut): " + e.getMessage());
							}
						}

						// Nyalakan perekam performa berkala (thread daemon). Aman dipanggil
						// di sini karena startup sudah selesai dan koneksi DB siap. Jika gagal,
						// jangan ganggu maintenance lain.
						try {
							PerformaSnapshotUtil.ensureSchedulerStarted();
						} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:356");
						}
					} catch (Throwable e) {
						if (!contextStopping) {
							System.err.println("Gagal menjalankan maintenance startup: " + e.getMessage());
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:361");
						}
					} finally {
						closeHibernateSessionQuietly();
						closeStreamingSessionQuietly();
					}
				}
			}, "ais-startup-maintenance");

			maintenanceThread.setDaemon(true);
			maintenanceThread.start();
		}
	}

	private void stopBackgroundMaintenance() {
		Thread thread = maintenanceThread;
		if (thread != null) {
			try {
				thread.interrupt();
			} catch (Throwable e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:381");
			}
		}
		maintenanceThread = null;
	}

	private static void closeHibernateSessionQuietly() {
		try {
			HibernateUtil.closeSession();
		} catch (Throwable e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:390");
			// Abaikan agar proses destroy/bootstrap tidak gagal hanya karena session sudah tertutup.
		}
	}

	private static void closeStreamingSessionQuietly() {
		try {
			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Throwable e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:398");
			// Abaikan agar proses startup tidak gagal hanya karena session streaming sudah tertutup.
		}
	}

	private static void resetHibernateSessionsBeforeStartupTask() {
		closeHibernateSessionQuietly();
		closeStreamingSessionQuietly();
	}

	private static void runSkinTask(String namaProses, Runnable task) {
		resetHibernateSessionsBeforeStartupTask();
		try {
			if (task != null) {
				task.run();
			}
		} catch (Throwable e) {
			System.err.println("Gagal menjalankan proses skin " + namaProses + ": " + e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:416");
			try {
				ErrorAuditUtil.record(e, "Gagal menjalankan proses skin: " + namaProses);
			} catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:419");
			}
		} finally {
			resetHibernateSessionsBeforeStartupTask();
		}
	}

	/**
	 * Hentikan scheduler penyimpanan berkala bawaan aplikasi agar thread {@code flag-access-saver-*}
	 * dan {@code elearning-ringkasan-saver-*} berhenti saat webapp stop/reload.
	 */
	private static void stopSaverSchedulers() {
		try {
			FlagAccessCache.hentikanPenyimpananBerkala();
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:433");
		}
		try {
			ElearningFlagAccessCache.hentikanPenyimpananBerkala();
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:437");
		}
		try {
			EntityAccessCache.hentikanPenyimpananBerkala();
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:441");
		}
		try {
			DepositoAroScheduler.hentikan();
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:445");
		}
		try {
			ais.action.master.repository.RepositorySyncScheduler.hentikan();
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:repository-sync-stop");
		}
	}

	/**
	 * Tutup SessionFactory Hibernate (utama + streaming). Ini menutup {@code C3P0ConnectionProvider}
	 * masing-masing sehingga pool koneksi c3p0 (thread {@code mchange ... PoolThread} + Timer-nya)
	 * berhenti dan tidak menahan classloader webapp saat shutdown/reload.
	 */
	private static void shutdownSessionFactoriesQuietly() {
		try {
			HibernateUtil.shutdownFactoryQuietly();
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:457");
		}
		try {
			StreamingHibernateUtil.getInstance().closeFactoryQuietly();
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:461");
		}
	}

	/**
	 * Matikan SEMUA {@code net.sf.ehcache.CacheManager} yang masih hidup (via refleksi atas field
	 * statik {@code ALL_CACHE_MANAGERS}). Diperlukan karena cache L2 Hibernate memakai
	 * {@code SingletonEhCacheRegionFactory}: CacheManager-nya singleton lintas-SessionFactory,
	 * sehingga menutup SessionFactory TIDAK mematikannya. Tanpa langkah ini, thread
	 * {@code net.sf.ehcache.CacheManager@...} beserta thread cache region
	 * ({@code StandardQueryCache.data}, {@code UpdateTimestampsCache.data}, dll.) tetap hidup dan
	 * memicu warning kebocoran Tomcat. Refleksi dipakai agar tidak ada ketergantungan compile-time
	 * ke EhCache; semua kegagalan ditelan.
	 */
	private static void shutdownEhcacheCacheManagersQuietly() {
		try {
			Class<?> cacheManager = Class.forName("net.sf.ehcache.CacheManager");
			java.lang.reflect.Field field = cacheManager.getField("ALL_CACHE_MANAGERS");
			Object value = field.get(null);
			if (value instanceof java.util.List) {
				// Salin dulu: shutdown() menghapus manager dari ALL_CACHE_MANAGERS (hindari
				// ConcurrentModificationException saat iterasi).
				java.util.List<?> snapshot = new java.util.ArrayList<Object>((java.util.List<?>) value);
				for (int i = 0; i < snapshot.size(); i++) {
					Object manager = snapshot.get(i);
					if (manager == null) {
						continue;
					}
					try {
						manager.getClass().getMethod("shutdown", new Class[0]).invoke(manager, new Object[0]);
						System.out.println("EhCache CacheManager dimatikan saat shutdown.");
					} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:492");
					}
				}
			}
		} catch (ClassNotFoundException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:496");
			// EhCache tidak ada di classpath: abaikan.
		} catch (Throwable e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:498");
			// Versi EhCache beda / field tidak ada: abaikan agar shutdown tidak gagal.
		}
	}

	private static void shutdownOptionalMemoryCache() {
		try {
			Class<?> clazz = Class.forName("ais.common.MemoryCacheUtil");
			Method shutdown = clazz.getMethod("shutdown", new Class[0]);
			shutdown.invoke(null, new Object[0]);
		} catch (ClassNotFoundException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:508");
			// Abaikan, class cache tidak tersedia di semua versi.
		} catch (NoSuchMethodException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:510");
			// Abaikan, method shutdown hanya ada di versi MemoryCacheUtil yang sudah dioptimalkan.
		} catch (Throwable e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:513");
		}
	}

	private static void unregisterJdbcDrivers() {
		ClassLoader webAppClassLoader = Thread.currentThread().getContextClassLoader();
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			try {
				if (driver != null && driver.getClass() != null
						&& driver.getClass().getClassLoader() == webAppClassLoader) {
					DriverManager.deregisterDriver(driver);
					System.out.println("JDBC driver berhasil di-unregister: " + driver.getClass().getName());
				}
			} catch (SQLException e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:529");
			} catch (Throwable e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:531");
			}
		}
	}

	private void initConfig() {
		File filecache = new File("/opt/cache");
		if (!filecache.exists() && !filecache.mkdirs()) {
			System.err.println("Folder /opt/cache tidak dapat dibuat atau tidak dapat diakses.");
		}

		String context = "";
		String fullPath = "";

		try {
			URL resourceUrl = this.getClass().getClassLoader().getResource("");
			if (resourceUrl != null) {
				fullPath = URLDecoder.decode(resourceUrl.getPath(), "UTF-8");

				if (fullPath.indexOf("/WEB-INF/classes/") >= 0) {
					String[] pathArr = fullPath.split("/WEB-INF/classes/");
					if (pathArr.length > 0) {
						fullPath = pathArr[0];
					}

					String[] ss = fullPath.split("/");
					if (ss.length > 0) {
						context = ss[ss.length - 1];
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Gagal memproses path sistem: " + e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:564");
		}

		Common.ROOT = context == null || context.trim().length() == 0 ? "" : "/" + context;
		Common.REAL_PATH = fullPath == null ? "" : fullPath;
		Common.REAL_PATH_REPORT_TEMP = Common.REAL_PATH + File.separator + "report";

		File file = new File("/opt/.g/.h/xxyxyx.txt");
		File fileConfKhusus = new File("/opt/.g/.h/" + context + ".txt");

		System.out.println("AppStartupListener fileConfKhusus -> " + fileConfKhusus.getAbsolutePath() + " ada: "
				+ fileConfKhusus.exists() + ", root: " + Common.ROOT + ", path: " + Common.REAL_PATH + ", report "
				+ Common.REAL_PATH_REPORT_TEMP);

		if (fileConfKhusus.exists()) {
			file = fileConfKhusus;
		}

		if (file.getParentFile() != null && !file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}

		Properties properties = System.getProperties();
		FileInputStream fis = null;
		try {
			if (file.exists() && file.isFile()) {
				fis = new FileInputStream(file);
				properties.load(fis);
			}
		} catch (Exception e) {
			System.err.println("Gagal memuat file properties: " + e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:595");
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:601");
				}
			}
		}

		String dinamicDb = properties.getProperty("dinamic_local_database");
		if (dinamicDb != null && dinamicDb.trim().equalsIgnoreCase("true")) {
			setupDynamicDatabase(properties);
		}

		if (properties.getProperty("hbm2ddl") == null) {
			properties.setProperty("hbm2ddl", "auto");
		}

		properties.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");

		String truncateVal = properties.getProperty("truncareAccessedUsers");
		truncareAccessedUsers = Boolean.valueOf(truncateVal != null && Boolean.parseBoolean(truncateVal));

		if (truncareAccessedUsers.booleanValue() && !hasTruncate) {
			try {
				// Logika truncate tetap dipertahankan sebagai titik ekstensi lama.
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:624");
			} finally {
				hasTruncate = true;
			}
		}
	}

	private void setupDynamicDatabase(Properties properties) {
		try {
			URL resource = this.getClass().getClassLoader().getResource("");
			if (resource == null) {
				return;
			}

			URL hibernate = this.getClass().getClassLoader().getResource("hibernate.cfg.xml");
			URL streaming = this.getClass().getClassLoader().getResource("hibernate.streaming.cfg.xml");

			String myresource = URLDecoder.decode(resource.getPath(), "UTF-8");
			myresource = StringUtils.replace(myresource, "/WEB-INF/classes/", "");
			String[] s = StringUtils.split(myresource, "/");
			if (s != null && s.length > 0) {
				myresource = s[s.length - 1];
			}

			String host_db = getProperty(properties, "host_database", "localhost");
			String port_db = getProperty(properties, "port_database", "5432");

			if (hibernate != null) {
				File hibernateFile = new File(URLDecoder.decode(hibernate.getPath(), "UTF-8"));
				if (hibernateFile.exists() && hibernateFile.isFile()) {
					String content = FileUtils.readFileToString(hibernateFile, "UTF-8");
					String newContent = StringUtils.replace(content, "${url}",
							"jdbc:postgresql://" + host_db + ":" + port_db + "/" + myresource);
					if (!content.equals(newContent)) {
						FileUtils.writeStringToFile(hibernateFile, newContent, "UTF-8");
					}
				}
			}

			String host_stream = getProperty(properties, "host_streaming_database", "localhost");
			String port_stream = getProperty(properties, "port_streaming_database", "5432");

			if (streaming != null) {
				File streamingFile = new File(URLDecoder.decode(streaming.getPath(), "UTF-8"));
				if (streamingFile.exists() && streamingFile.isFile()) {
					String content = FileUtils.readFileToString(streamingFile, "UTF-8");
					String newContent = StringUtils.replace(content, "${url_streaming}",
							"jdbc:postgresql://" + host_stream + ":" + port_stream + "/streaming_" + myresource);
					if (!content.equals(newContent)) {
						FileUtils.writeStringToFile(streamingFile, newContent, "UTF-8");
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Gagal mengupdate konfigurasi database: " + e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:679");
		}
	}

	private static String getProperty(Properties properties, String key, String defaultValue) {
		String value = properties == null ? null : properties.getProperty(key);
		return value != null && value.trim().length() > 0 ? value.trim() : defaultValue;
	}

	private void initSkin() {
		try {
			if (Common.REAL_PATH == null || Common.REAL_PATH.trim().length() == 0) {
				return;
			}

			long now = System.currentTimeMillis();
			if (now - lastCheckTime > CHECK_INTERVAL || markerFile == null) {
				markerFile = new File(Common.REAL_PATH + File.separator + "udah.udh");
				fileExistsCache = markerFile.exists();
				lastCheckTime = now;
			}

			if (!fileExistsCache && markerFile != null) {
				synchronized (LOCK_SKIN) {
					if (!markerFile.exists()) {
						try {
							System.out.println("buat marker file baru -> " + markerFile.getAbsolutePath());
							if (markerFile.getParentFile() != null) {
								markerFile.getParentFile().mkdirs();
							}
							markerFile.createNewFile();
							fileExistsCache = true;
							skin();
						} catch (IOException e) {
							System.err.println("Gagal membuat marker file: " + e.getMessage());
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:714");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:720");
		}
	}

	private static void skin() {
		if (prosesSkin || contextStopping) {
			return;
		}

		synchronized (LOCK_SKIN) {
			if (prosesSkin || contextStopping) {
				return;
			}
			prosesSkin = true;

			try {
				System.out.println("Memulai proses skinning...");
				if (Common.ROOT == null || Common.ROOT.trim().length() == 0 || Common.REAL_PATH == null
						|| Common.REAL_PATH.trim().length() == 0) {
					return;
				}

				String contentPath = Common.ROOT.replace("/", "");
				CommonMedia.getMediaDirectory(Common.ROOT);

				File themeZip = new File("/opt/" + contentPath + ".zip");
				if (themeZip.exists() && themeZip.isFile()) {
					File targetDir = new File(Common.REAL_PATH + File.separator + "WEB-INF" + File.separator + "j");
					try {
						if (targetDir.exists()) {
							FileUtils.deleteDirectory(targetDir);
						}
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/AppStartupListener.java:752");
					}
					targetDir.mkdirs();

					Common.unzipFolder(Paths.get(themeZip.getAbsolutePath()), Paths.get(targetDir.getAbsolutePath()));

					File[] files = targetDir.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name != null && name.toLowerCase().endsWith(".jsp");
						}
					});

					if (files != null) {
						for (int i = 0; i < files.length; i++) {
							File file = files[i];
							String content = FileUtils.readFileToString(file, "UTF-8");
							String newContent = StringUtils.replace(content, "tampilanSocialLogin()",
									"tampilanSocialLogin(request, response)");
							newContent = StringUtils.replace(newContent, "/pages/ux", "/WEB-INF/o/ux");
							if (!content.equals(newContent)) {
								FileUtils.writeStringToFile(file, newContent, "UTF-8");
							}
						}
					}

					Common.unzipFolder(Paths.get(themeZip.getAbsolutePath()), Paths.get(Common.REAL_PATH));

					// Sinkronisasi branding (logo/background/favicon) banyak melakukan
					// File.exists/baca media; di FS lambat/mount jaringan bisa SANGAT lama.
					// Dulu dijalankan INLINE di thread "main" -> MEMBLOKIR penyelesaian startup
					// (Tomcat tak kunjung siap, "stop" setelah init selesai). Jalankan di thread
					// LATAR (daemon) agar startup selesai & aplikasi bisa diakses; branding
					// tersinkron menyusul (sementara dipakai berkas/logo yang sudah ada).
					Thread brandingThread = new Thread(new Runnable() {
						@Override
						public void run() {
							runSkinTask("sinkron logo", new Runnable() {
								@Override
								public void run() {
									Common.checkLogoUpload();
								}
							});
							runSkinTask("sinkron background", new Runnable() {
								@Override
								public void run() {
									Common.checkBackgroundUpload();
								}
							});
							runSkinTask("sinkron favicon", new Runnable() {
								@Override
								public void run() {
									Common.checkFaviconUpload();
								}
							});
						}
					}, "AIS-Branding-Sync");
					brandingThread.setDaemon(true);
					brandingThread.start();
				}
			} catch (Exception e) {
				System.err.println("Error saat proses skin: " + e.getMessage());
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AppStartupListener.java:813");
			} finally {
				prosesSkin = false;
			}
		}
	}
}
