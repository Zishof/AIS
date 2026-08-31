package ais.common; // Sesuaikan dengan struktur package Anda

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.DesktopUnavailableException;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Label;

/**
 * Pengelola eksekusi tugas asinkron bersama AIS. Kelas ini memusatkan executor, antrean, penamaan
 * thread, timeout, dan shutdown agar modul tidak membuat thread pool tanpa lifecycle yang seragam.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code ExecutorService EXECUTOR}, {@code
 * String PUSH_REFCOUNT_ATTR}; pembacaan/pencarian ({@code getBahasaConfig()}, {@code tampilErrorJikaAdmin()});
 * operasi domain lain ({@code shutdown()}, {@code increfServerPush()}, {@code decrefServerPush()}, {@code
 * statistikPool()}, {@code tambahPush()}, {@code lepasPush()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> operasi menjadwalkan pekerjaan pada thread lain dan mengelola lifecycle executor
 * global. Tugas tidak boleh membawa session Hibernate/ZK request lintas thread; shutdown harus dipanggil saat
 * aplikasi berhenti agar thread tidak bocor.</p>
 */
public class AsyncTaskManager {

	// Thread Pool global untuk tugas latar (banyak di antaranya membuka koneksi DB).
	// Kapasitas DIBATASI plafon aman (DbThreadPool, default 16, config max_thread_db_paralel)
	// jauh di bawah c3p0 max_size. SEBELUMNYA 250 -> bisa meminta 250 koneksi sekaligus dan
	// MENGHABISKAN pool c3p0 sehingga seluruh thread AJP/HTTP menunggu koneksi (Tomcat hang).
	// Tugas berlebih ANTRE dengan rapi (bukan gagal).
	// Thread DAEMON + bernama (ais-async-task-N): pool ini tidak pernah di-shutdown pada
	// versi lama sehingga thread non-daemon default menahan classloader webapp lama saat
	// redeploy (warning "appears to have started a thread ... failed to stop it" Tomcat).
	// Daemon juga memastikan JVM bisa exit walau ada task menggantung. Shutdown deterministik
	// tetap dipanggil dari AppStartupListener.contextDestroyed (lihat shutdown() di bawah).
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
			ais.common.DbThreadPool.safe(250), new java.util.concurrent.ThreadFactory() {
				private final java.util.concurrent.atomic.AtomicInteger urutan = new java.util.concurrent.atomic.AtomicInteger(1);

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "ais-async-task-" + urutan.getAndIncrement());
					t.setDaemon(true);
					return t;
				}
			});

	/**
	 * Menghentikan pool async global saat webapp stop/redeploy. Dipanggil dari
	 * {@code AppStartupListener.contextDestroyed}. Memberi waktu singkat bagi task yang
	 * sedang berjalan untuk selesai, lalu memaksa interrupt — task di kelas ini sudah
	 * defensif terhadap desktop mati (DesktopUnavailableException/isAlive check).
	 */
	public static void shutdown() {
		try {
			EXECUTOR.shutdown();
			if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
				EXECUTOR.shutdownNow();
			}
		} catch (InterruptedException e) {
			EXECUTOR.shutdownNow();
			Thread.currentThread().interrupt();
		} catch (Throwable e) { ais.common.ErrorAuditUtil.record(e, "AsyncTaskManager.shutdown");
		}
	}

	// Interfaces
	/**
	 * Kontrak callback/strategi bersarang milik {@link AsyncTaskManager}. Tipe ini memisahkan satu variasi
	 * perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AsyncTaskManager} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code doInBackground}(). Aturan bisnis
	 * bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> pekerjaan berjalan di thread lain. Buka/tutup resource miliknya sendiri, jangan
	 * memakai komponen ZK atau session Hibernate request tanpa aktivasi yang eksplisit, dan laporkan kegagalan
	 * melalui mekanisme kelas induk.</p>
	 *
	 * @see AsyncTaskManager
	 */
	public interface BackgroundTask {
		Object doInBackground() throws Exception;
	}

	/**
	 * Kontrak callback/strategi bersarang milik {@link AsyncTaskManager}. Tipe ini memisahkan satu variasi
	 * perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AsyncTaskManager} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code updateUI}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> pekerjaan berjalan di thread lain. Buka/tutup resource miliknya sendiri, jangan
	 * memakai komponen ZK atau session Hibernate request tanpa aktivasi yang eksplisit, dan laporkan kegagalan
	 * melalui mekanisme kelas induk.</p>
	 *
	 * @see AsyncTaskManager
	 */
	public interface UITask {
		void updateUI(Object backgroundResult) throws Exception;
	}

	private static String getBahasaConfig(String text) {
		// Sesuaikan pemanggilan ke utilitas aplikasi Anda
		// return ais.common.Common.getBahasaConfig(text);
		return text; 
	}

	private static void tampilErrorJikaAdmin(Exception e) {
		// Sesuaikan pemanggilan ke utilitas aplikasi Anda
		// ais.common.Common.tampilErrorJikaAdmin(e);
		e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AsyncTaskManager.java:42");
	}

	// ==============================================================================
	// REFERENCE COUNTING SERVER PUSH (FIX ledakan thread http-nio Tomcat)
	// ==============================================================================
	// AKAR MASALAH: seluruh method di kelas ini memanggil desktop.enableServerPush(true)
	// tapi TIDAK PERNAH memanggil enableServerPush(false) setelah tugas selesai. Server
	// push ZKoss (PollingServerPush) membuat browser terus-menerus mengirim request
	// polling yang masing-masing MENAHAN satu thread Tomcat (http-nio-...-exec-N) sampai
	// timeout/ada update, lalu langsung polling lagi -- selama server push aktif. Karena
	// tidak pernah dimatikan, setiap desktop yang PERNAH memakai fitur async apa pun
	// (laporan, proses latar, dsb -- dipakai di puluhan modul) polling TERUS SEUMUR HIDUP
	// desktop tsb, walau tugas aslinya sudah selesai dalam hitungan detik. Pada sistem
	// dengan ribuan sesi aktif bersamaan, ini menjelaskan ledakan thread http-nio sampai
	// ~15.000 dalam hitungan menit (lihat analisa snapshot performa 20-07-2026).
	//
	// FIX: reference-count per Desktop. enableServerPush(true) hanya sekali saat counter
	// naik dari 0, dan enableServerPush(false) hanya saat counter kembali ke 0 (semua
	// tugas async pada desktop itu, lewat kelas ini, sudah selesai) -- aman dipakai
	// bersamaan oleh banyak tugas async pada desktop yang sama tanpa saling mematikan
	// push milik tugas lain yang masih berjalan.
	private static final String PUSH_REFCOUNT_ATTR = "aisAsyncTaskManager.pushRefCount";

	private static void increfServerPush(Desktop desktop) {
		if (desktop == null) {
			return;
		}
		try {
			synchronized (desktop) {
				java.util.concurrent.atomic.AtomicInteger ref = (java.util.concurrent.atomic.AtomicInteger) desktop
						.getAttribute(PUSH_REFCOUNT_ATTR);
				if (ref == null) {
					ref = new java.util.concurrent.atomic.AtomicInteger(0);
					desktop.setAttribute(PUSH_REFCOUNT_ATTR, ref);
				}
				if (ref.incrementAndGet() == 1 && !desktop.isServerPushEnabled()) {
					desktop.enableServerPush(true);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AsyncTaskManager.java:increfServerPush"); }
	}

	private static void decrefServerPush(Desktop desktop) {
		if (desktop == null) {
			return;
		}
		try {
			synchronized (desktop) {
				java.util.concurrent.atomic.AtomicInteger ref = (java.util.concurrent.atomic.AtomicInteger) desktop
						.getAttribute(PUSH_REFCOUNT_ATTR);
				if (ref == null) {
					return;
				}
				if (ref.decrementAndGet() <= 0 && desktop.isAlive() && desktop.isServerPushEnabled()) {
					desktop.enableServerPush(false);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AsyncTaskManager.java:decrefServerPush"); }
	}

	/**
	 * Menyalakan server push untuk sebuah desktop dengan reference counting (OPTIMASI FASE 5).
	 *
	 * <p>Dipakai oleh alur latar yang TIDAK bisa memakai {@link #jalankanDenganPush} karena
	 * titik mulai dan titik selesainya berada di method berbeda (mis. proses backup yang
	 * menyalakan push saat tombol ditekan lalu menampilkan hasilnya lewat method notifikasi
	 * terpisah). WAJIB dipasangkan dengan {@link #lepasPush(Desktop)} pada titik akhir alur,
	 * jika tidak push akan bocor dan browser terus polling selamanya.</p>
	 */
	/**
	 * Ringkasan kondisi pool async untuk snapshot performa (OPTIMASI FASE 10).
	 * Dipakai membuktikan batas pool dan pelepasan tugas latar benar-benar bekerja.
	 * Menelan seluruh kegagalan agar tidak pernah menggagalkan pembuatan snapshot.
	 */
	public static String statistikPool() {
		try {
			if (EXECUTOR instanceof java.util.concurrent.ThreadPoolExecutor) {
				java.util.concurrent.ThreadPoolExecutor tpe = (java.util.concurrent.ThreadPoolExecutor) EXECUTOR;
				return "aktif=" + tpe.getActiveCount()
						+ ", pool=" + tpe.getPoolSize() + "/" + tpe.getMaximumPoolSize()
						+ ", antre=" + tpe.getQueue().size()
						+ ", selesai=" + tpe.getCompletedTaskCount()
						+ (EXECUTOR.isShutdown() ? ", SUDAH-SHUTDOWN" : "");
			}
			return "tidak tersedia (bukan ThreadPoolExecutor)";
		} catch (Throwable t) {
			return "gagal dibaca: " + t.getClass().getSimpleName();
		}
	}
	public static void tambahPush(Desktop desktop) {
		increfServerPush(desktop);
	}

	/**
	 * Melepas server push yang sebelumnya dinyalakan {@link #tambahPush(Desktop)}. Push baru
	 * benar-benar dimatikan ketika seluruh tugas pada desktop tersebut sudah selesai
	 * (reference count kembali nol), sehingga aman dipanggil walau ada tugas async lain
	 * yang masih berjalan pada desktop yang sama.
	 */
	public static void lepasPush(Desktop desktop) {
		decrefServerPush(desktop);
	}

	/**
	 * Menjalankan tugas latar SAMBIL mengelola server push secara otomatis (OPTIMASI FASE 5).
	 *
	 * <p><b>Masalah yang diselesaikan.</b> Puluhan layar (laporan, dashboard, proses massal)
	 * memakai pola:</p>
	 * <pre>
	 *   if (!desktop.isServerPushEnabled()) desktop.enableServerPush(true);
	 *   new Thread(runnable).start();
	 * </pre>
	 * <p>Push DINYALAKAN tetapi TIDAK PERNAH dimatikan. Server push ZK 5.5
	 * (PollingServerPush) membuat browser terus-menerus mengirim request polling yang
	 * masing-masing MENAHAN satu thread Tomcat sampai timeout, lalu polling lagi -- selamanya
	 * selama tab masih terbuka, walau tugasnya sudah selesai dalam hitungan detik. Inilah
	 * penyebab ledakan thread http-nio yang terdokumentasi pada kelas ini. Selain itu
	 * {@code new Thread(...)} membuat thread MENTAH per operasi: tak berbatas, tak bernama,
	 * dan tidak berhenti saat webapp di-redeploy.</p>
	 *
	 * <p><b>Cara pakai.</b> Ganti kedua baris di atas dengan satu panggilan:</p>
	 * <pre>
	 *   AsyncTaskManager.jalankanDenganPush(desktop, new Runnable() { public void run() { ... } });
	 * </pre>
	 *
	 * <p>Push dinyalakan lewat reference counting (aman bila beberapa tugas berjalan pada
	 * desktop yang sama) dan DILEPAS di {@code finally} begitu tugas selesai/gagal. Tugas
	 * dijalankan pada pool daemon berbatas milik kelas ini yang sudah ditutup rapi saat
	 * webapp berhenti -- bukan thread mentah.</p>
	 *
	 * @param desktop desktop ZK pemilik tugas; boleh null (tugas tetap dijalankan tanpa push)
	 * @param tugas   pekerjaan latar; diabaikan bila null
	 */
	public static void jalankanDenganPush(final Desktop desktop, final Runnable tugas) {
		if (tugas == null) {
			return;
		}
		increfServerPush(desktop);
		EXECUTOR.submit(new Runnable() {
			@Override
			public void run() {
				try {
					tugas.run();
				} catch (Throwable t) {
					try {
						tampilErrorJikaAdmin(t instanceof Exception ? (Exception) t : new Exception(t));
					} catch (Throwable abaikan) {
					}
				} finally {
					decrefServerPush(desktop);
				}
			}
		});
	}

	// ==============================================================================
	// METHOD EXECUTE ASYNC (BACKGROUND PROCESS ENGINE - ZK 5.5 COMPATIBLE)
	// ==============================================================================

	/**
	 * Mengeksekusi proses berat ke belakang layar dan memperbarui UI secara aman.
	 * Sangat cocok untuk mencegah UI Freeze / Deadlock pada ZKoss 5.5 dengan PollingServerPush.
	 * 
	 * @param desktop        Desktop dari ZK Execution saat ini
	 * @param labelProgress  Label UI untuk menampilkan status loading (boleh null)
	 * @param loadingMessage Pesan teks yang muncul saat loading (boleh null)
	 * @param bgTask         Tugas berat yang tidak menyentuh komponen UI
	 * @param uiTask         Tugas ringan untuk mencetak hasil ke layar UI
	 */
	public static void executeAsync(final Desktop desktop, final Label labelProgress,
			final String loadingMessage, final BackgroundTask bgTask, final UITask uiTask) {

		// 1. Validasi & Aktifkan Server Push ZK 5.5 (reference-counted, lihat increfServerPush)
		increfServerPush(desktop);

		// Menjalankan tugas menggunakan Thread Pool yang aman (Anti OutOfMemory)
		EXECUTOR.submit(new Runnable() {
			@Override
			public void run() {
				try {
					Object resultData = null;

					// 2. Update Informasi Loading Awal (Aman dengan Desktop Activate)
					if (labelProgress != null && loadingMessage != null && !loadingMessage.trim().isEmpty()) {
						if (desktop == null || !desktop.isAlive()) return; // Batal jika tab ditutup

						try {
							Executions.activate(desktop);
							try {
								labelProgress.setValue(loadingMessage);
							} finally {
								Executions.deactivate(desktop);
							}
						} catch (DesktopUnavailableException due) {
							return; // Tab keburu ditutup saat Polling, hentikan seluruh proses hemat CPU
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AsyncTaskManager.java:87");
						}
					}

					// 3. Eksekusi Kueri Database (Berjalan MURNI di Latar Belakang)
					try {
						if (bgTask != null) {
							resultData = bgTask.doInBackground();
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AsyncTaskManager.java:97");
					}

					// Cek kelangsungan hidup desktop sebelum merender hasil akhir
					if (desktop == null || !desktop.isAlive()) {
						return;
					}

					// 4. Bawa Data Hasil DB ke Layar / UI ZKoss
					try {
						Executions.activate(desktop);
						try {
							if (uiTask != null) {
								uiTask.updateUI(resultData);
							}

							// Matikan indikator loading jika proses selesai dan Trigger UI Event
							if (labelProgress != null) {
								labelProgress.setValue("");
								org.zkoss.zk.ui.event.Events.postEvent("onEvent", labelProgress, null);
							}
						} finally {
							// 5. Wajib melepaskan kunci (lock) Desktop agar UI pengguna kembali interaktif
							Executions.deactivate(desktop);
						}
					} catch (DesktopUnavailableException due) {
						// Diabaikan. Berarti tab ditutup persis setelah task selesai.
					} catch (Exception e) {
						try {
							tampilErrorJikaAdmin(e);
						} catch (Exception ex) {
							ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/AsyncTaskManager.java:128");
						}
					}
				} finally {
					// 6. Lepaskan server push (reference-counted) -- FIX ledakan thread http-nio.
					decrefServerPush(desktop);
				}
			}
		});
	}

	// =========================================================================================
	// 1. CREATE DEFAULT TIMER (NORMAL ASYNC)
	// =========================================================================================
	public static void createDefaultTimer(String info, final BackgroundTask bgTask, final UITask uiTask) {
		try {
			Clients.clearBusy();
			Clients.showBusy(info == null ? getBahasaConfig("Harap tunggu, sedang menyiapkan data...") : info);

			final Desktop desktop = Executions.getCurrent().getDesktop();
			increfServerPush(desktop);

			EXECUTOR.submit(new Runnable() {
				@Override
				public void run() {
					try {
						Object resultData = null;

						try {
							if (bgTask != null) {
								resultData = bgTask.doInBackground();
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AsyncTaskManager.java:158");
						}

						if (desktop == null || !desktop.isAlive()) {
							return;
						}

						try {
							Executions.activate(desktop);
							try {
								if (uiTask != null) {
									uiTask.updateUI(resultData);
								}
							} finally {
								Clients.clearBusy();
								Executions.deactivate(desktop);
							}
						} catch (DesktopUnavailableException due) { ais.common.ErrorAuditUtil.record(due, "auto-audit(empty-catch) src/ais/common/AsyncTaskManager.java:175");
						} catch (Exception e) {
							tampilErrorJikaAdmin(e);
						}
					} finally {
						decrefServerPush(desktop);
					}
				}
			});

		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}
	}

	// =========================================================================================
	// 2. CREATE DEFAULT TIMER TIMEOUT (DENGAN WATCHER 25 DETIK & LATCH)
	// =========================================================================================
	public static void createDefaultTimerTimeout(String info, final BackgroundTask bgTask, final UITask uiTask) {
		try {
			Clients.clearBusy();
			Clients.showBusy(info == null ? getBahasaConfig("Harap tunggu, sedang memproses data...") : info);

			final Desktop desktop = Executions.getCurrent().getDesktop();
			// incref DUA KALI: watcher dan pekerja berjalan independen (urutan selesai tidak
			// pasti), push baru boleh dimatikan setelah KEDUANYA selesai.
			increfServerPush(desktop);
			increfServerPush(desktop);

			final CountDownLatch latch = new CountDownLatch(1);

			// THREAD WATCHER
			EXECUTOR.submit(new Runnable() {
				@Override
				public void run() {
					try {
						boolean finishedEarly = latch.await(25, TimeUnit.SECONDS);

						if (!finishedEarly && desktop != null && desktop.isAlive()) {
							Executions.activate(desktop);
							try {
								Clients.clearBusy();
								ais.ui.util.MyMessageboxConfig.show(getBahasaConfig(
										"Proses membutuhkan waktu yang lama dan sedang berjalan di latar belakang. Anda dapat melanjutkan aktivitas lain sementara sistem menyelesaikannya."),
										getBahasaConfig("Informasi"), ais.ui.util.MyMessageboxConfig.OK,
										ais.ui.util.MyMessageboxConfig.INFORMATION);
							} finally {
								Executions.deactivate(desktop);
							}
						}
					} catch (DesktopUnavailableException due) { ais.common.ErrorAuditUtil.record(due, "auto-audit(empty-catch) src/ais/common/AsyncTaskManager.java:221");
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AsyncTaskManager.java:225");
					} finally {
						decrefServerPush(desktop);
					}
				}
			});

			// THREAD PEKERJA
			EXECUTOR.submit(new Runnable() {
				@Override
				public void run() {
					try {
						Object resultData = null;

						try {
							if (bgTask != null) {
								resultData = bgTask.doInBackground();
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AsyncTaskManager.java:241");
						} finally {
							latch.countDown();
						}

						if (desktop == null || !desktop.isAlive()) {
							return;
						}

						try {
							Executions.activate(desktop);
							try {
								if (uiTask != null) {
									uiTask.updateUI(resultData);
								}
							} finally {
								Clients.clearBusy();
								Executions.deactivate(desktop);
							}
						} catch (DesktopUnavailableException due) { ais.common.ErrorAuditUtil.record(due, "auto-audit(empty-catch) src/ais/common/AsyncTaskManager.java:260");
						} catch (Exception e) {
							tampilErrorJikaAdmin(e);
						}
					} finally {
						decrefServerPush(desktop);
					}
				}
			});

		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}
	}

	// =========================================================================================
	// 3. CREATE DEFAULT TIMER NO BUSY (TANPA LOADING BAR)
	// =========================================================================================
	public static void createDefaultTimerNoBusy(final BackgroundTask bgTask, final UITask uiTask) {
		try {
			final Desktop desktop = Executions.getCurrent().getDesktop();
			increfServerPush(desktop);

			EXECUTOR.submit(new Runnable() {
				@Override
				public void run() {
					try {
						Object resultData = null;

						try {
							if (bgTask != null) {
								resultData = bgTask.doInBackground();
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AsyncTaskManager.java:292");
						}

						if (desktop == null || !desktop.isAlive()) {
							return;
						}

						try {
							Executions.activate(desktop);
							try {
								if (uiTask != null) {
									uiTask.updateUI(resultData);
								}
							} finally {
								Executions.deactivate(desktop);
							}
						} catch (DesktopUnavailableException due) { ais.common.ErrorAuditUtil.record(due, "auto-audit(empty-catch) src/ais/common/AsyncTaskManager.java:308");
						} catch (Exception e) {
							tampilErrorJikaAdmin(e);
						}
					} finally {
						decrefServerPush(desktop);
					}
				}
			});
		} catch (Exception e) {
			tampilErrorJikaAdmin(e);
		}
	}

	// =========================================================================================
	// 4. LEGACY SUPPORT: EventListener Murni (Dipertahankan agar kode lama tidak error)
	// =========================================================================================
	public static void createDefaultTimer(final EventListener eventListener, String info, final Boolean repeat, final Integer interfal) {
		try {
			Clients.clearBusy();
			Clients.showBusy(info == null ? getBahasaConfig("Harap tunggu, sedang menyiapkan data...") : info);

			final Desktop desktop = Executions.getCurrent().getDesktop();
			increfServerPush(desktop);

			EXECUTOR.submit(new Runnable() {
				@Override
				public void run() {
					try {
						try {
							if (interfal != null && interfal > 0) {
								Thread.sleep(interfal);
							}

							if (desktop == null || !desktop.isAlive()) return;

							Executions.activate(desktop);
							try {
								eventListener.onEvent(null);
							} finally {
								Clients.clearBusy();
								Executions.deactivate(desktop);
							}
						} catch (DesktopUnavailableException due) { ais.common.ErrorAuditUtil.record(due, "auto-audit(empty-catch) src/ais/common/AsyncTaskManager.java:349");
						} catch (Exception e) {
							tampilErrorJikaAdmin(e);
						}
					} finally {
						decrefServerPush(desktop);
					}
				}
			});

		} catch (Exception e) {
			try {
				eventListener.onEvent(null);
			} catch (Exception ee) {
				tampilErrorJikaAdmin(e);
			}
		}
	}

	public static void createDefaultTimerNoBusy(final EventListener eventListener, String info, final Boolean repeat, final Integer interfal) {
		try {
			final Desktop desktop = Executions.getCurrent().getDesktop();
			// incref SATU KALI di luar loop: ini timer BERULANG (repeat=true), push baru
			// dilepas saat loop benar-benar berhenti (bukan tiap iterasi), lihat finally di bawah.
			increfServerPush(desktop);

			EXECUTOR.submit(new Runnable() {
				@Override
				public void run() {
					try {
						while (desktop != null && desktop.isAlive()) {
							try {
								if (interfal != null && interfal > 0) {
									Thread.sleep(interfal);
								}

								if (!desktop.isAlive()) break;

								Executions.activate(desktop);
								try {
									eventListener.onEvent(null);
								} finally {
									Executions.deactivate(desktop);
								}

							} catch (DesktopUnavailableException due) {
								break;
							} catch (Exception e) {
								tampilErrorJikaAdmin(e);
							}

							if (repeat == null || !repeat) {
								break;
							}
						}
					} finally {
						decrefServerPush(desktop);
					}
				}
			});

		} catch (Exception e) {
			try {
				eventListener.onEvent(null);
			} catch (Exception ee) {
				tampilErrorJikaAdmin(e);
			}
		}
	}

	public static void createDefaultTimerTimeout(final EventListener eventListener, final String info, Boolean repeat, final Integer interfal) {
		try {
			Clients.clearBusy();
			Clients.showBusy(info == null ? getBahasaConfig("Harap tunggu, sedang memproses data...") : info);

			final Desktop desktop = Executions.getCurrent().getDesktop();
			// incref DUA KALI: watcher dan pekerja berjalan independen, lihat catatan di
			// createDefaultTimerTimeout(String, BackgroundTask, UITask) di atas.
			increfServerPush(desktop);
			increfServerPush(desktop);

			final CountDownLatch latch = new CountDownLatch(1);

			EXECUTOR.submit(new Runnable() {
				@Override
				public void run() {
					try {
						boolean finishedEarly = latch.await(25, TimeUnit.SECONDS);

						if (!finishedEarly && desktop != null && desktop.isAlive()) {
							Executions.activate(desktop);
							try {
								Clients.clearBusy();
								ais.ui.util.MyMessageboxConfig.show(getBahasaConfig(
										"Proses membutuhkan waktu yang lama dan sedang berjalan di latar belakang."),
										getBahasaConfig("Informasi"), ais.ui.util.MyMessageboxConfig.OK,
										ais.ui.util.MyMessageboxConfig.INFORMATION);
							} finally {
								Executions.deactivate(desktop);
							}
						}
					} catch (DesktopUnavailableException due) { ais.common.ErrorAuditUtil.record(due, "auto-audit(empty-catch) src/ais/common/AsyncTaskManager.java:442");
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AsyncTaskManager.java:446");
					} finally {
						decrefServerPush(desktop);
					}
				}
			});

			EXECUTOR.submit(new Runnable() {
				@Override
				public void run() {
					try {
						try {
							if (interfal != null && interfal > 0) {
								Thread.sleep(interfal);
							}

							latch.countDown();

							if (desktop == null || !desktop.isAlive()) return;

							Executions.activate(desktop);
							try {
								eventListener.onEvent(null);
							} finally {
								Clients.clearBusy();
								Executions.deactivate(desktop);
							}
						} catch (DesktopUnavailableException due) { ais.common.ErrorAuditUtil.record(due, "auto-audit(empty-catch) src/ais/common/AsyncTaskManager.java:470");
						} catch (Exception e) {
							tampilErrorJikaAdmin(e);
						}
					} finally {
						decrefServerPush(desktop);
					}
				}
			});

		} catch (Exception e) {
			try {
				eventListener.onEvent(null);
			} catch (Exception ee) {
				tampilErrorJikaAdmin(e);
			}
		}
	}
}
