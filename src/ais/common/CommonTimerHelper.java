package ais.common;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Timer;

import ais.ui.util.MyMessageboxConfig;

/**
 * Helper timer ZK untuk menghindari pengulangan pola createDefaultTimer di
 * Common.java.
 */
public final class CommonTimerHelper {

	private CommonTimerHelper() {
	}

	// FIX ledakan thread http-nio Tomcat: createDefaultTimerTimeout() di bawah mengaktifkan
	// desktop.enableServerPush(true) untuk thread latar yang meng-activate desktop di luar
	// konteks event ZK, tapi SEBELUMNYA tidak pernah mematikannya lagi -- desktop polling
	// terus seumur hidupnya. Pakai attribute key YANG SAMA dengan ais.common.AsyncTaskManager
	// supaya reference count-nya konsisten (tidak saling mematikan push milik satu sama lain)
	// jika kedua helper dipakai bersamaan pada desktop yang sama.
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonTimerHelper.java:increfServerPush"); }
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonTimerHelper.java:decrefServerPush"); }
	}

	public static void createDefaultTimerNoBusy(EventListener eventListener) {
		createDefaultTimerNoBusy(eventListener, "", false, 50);
	}

	public static void createDefaultTimer(EventListener eventListener) {
		createDefaultTimer(eventListener, null);
	}

	public static void createDefaultTimer(EventListener eventListener, String info) {
		createDefaultTimer(eventListener, info, false, 50);
	}

	public static void createDefaultTimerTimeout(EventListener eventListener) {
		createDefaultTimerTimeout(eventListener, null);
	}

	public static void createDefaultTimerTimeout(EventListener eventListener, String info) {
		createDefaultTimerTimeout(eventListener, info, false, 50);
	}

	public static void createDefaultTimer(final EventListener eventListener, String info, final Boolean repeat,
			final Integer interfal) {
		try {
			if (!hasZkExecution()) {
				runFallback(eventListener, null);
				return;
			}
			safeClearBusy();

			final Timer timer = new Timer(interfal != null ? interfal : 100);
			attachTimer(timer);
			timer.setRepeats(repeat != null ? repeat : false);

			safeShowBusy(info == null ? Common.getBahasaConfig("Harap tunggu, sedang menyiapkan data...") : info);

			timer.addEventListener("onTimer", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						if (repeat == null || !repeat.booleanValue()) {
							timer.stop();
						}
						eventListener.onEvent(event);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					} finally {
						safeClearBusy();
						if (repeat == null || !repeat.booleanValue()) {
							timer.detach();
						}
					}
				}
			});
			timer.start();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			runFallback(eventListener, e);
		}
	}

	public static void createDefaultTimerTimeout(final EventListener eventListener, final String info, Boolean repeat,
			Integer interfal) {
		Desktop desktopUntukCatch = null;
		try {
			if (!hasZkExecution()) {
				runFallback(eventListener, null);
				return;
			}
			safeClearBusy();

			final Desktop desktop = Executions.getCurrent().getDesktop();
			desktopUntukCatch = desktop;
			increfServerPush(desktop);

			final Timer timerStart = new Timer(interfal != null ? interfal : 100);
			attachTimer(timerStart);
			timerStart.setRepeats(false);

			safeShowBusy(info == null ? Common.getBahasaConfig("Harap tunggu, sedang memproses data...") : info);

			// Jaga agar decrefServerPush() untuk desktop ini dijalankan TEPAT SEKALI, baik
			// bila thread latar berhasil dimulai (dilepas di finally thread itu) MAUPUN bila
			// ada exception sebelum sempat dimulai (dilepas di catch listener onTimer di bawah)
			// -- mencegah kebocoran push kalau setup timerWatcher/thread gagal.
			final java.util.concurrent.atomic.AtomicBoolean pushDilepas = new java.util.concurrent.atomic.AtomicBoolean(
					false);

			timerStart.addEventListener("onTimer", new EventListener() {
				@Override
				public void onEvent(final Event event) throws Exception {
					try {
					timerStart.stop();
					timerStart.detach();

					final java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean(
							false);

					final Timer timerWatcher = new Timer(25000);
					attachTimer(timerWatcher);
					timerWatcher.setRepeats(false);
					timerWatcher.addEventListener("onTimer", new EventListener() {
						@Override
						public void onEvent(Event e) throws Exception {
							if (!finished.get()) {
								safeClearBusy();
								MyMessageboxConfig.show(Common.getBahasaConfig(
										"Proses membutuhkan waktu yang lama dan sedang berjalan di latar belakang (background). Anda dapat melanjutkan aktivitas lain sementara sistem menyelesaikannya."),
										Common.getBahasaConfig("Informasi"), MyMessageboxConfig.OK,
										MyMessageboxConfig.INFORMATION);
							}
							timerWatcher.stop();
							timerWatcher.detach();
						}
					});
					timerWatcher.start();

					new Thread(new Runnable() {
						@Override
						public void run() {
							boolean activated = false;
							try {
								// Guard: kalau user sudah menutup halaman/logout/timeout tepat
								// sebelum thread latar ini sempat jalan, desktop sudah destroyed --
								// skip activate & listener sepenuhnya, tapi tetap lepas resource di
								// finally supaya tidak bocor.
								if (!desktop.isAlive()) {
									return;
								}
								try {
									Executions.activate(desktop);
									activated = true;
								} catch (org.zkoss.zk.ui.DesktopUnavailableException due) {
									// Defense-in-depth: race condition tepat di antara isAlive() dan
									// activate() -- kondisi normal saat user menutup halaman, bukan
									// error aplikasi, jadi catat sebagai info dan skip listener saja.
									ais.common.ErrorAuditUtil.record(due, "info-audit src/ais/common/CommonTimerHelper.java:createDefaultTimerTimeout - desktop destroyed sebelum activate (user sudah menutup halaman)");
									return;
								}
								eventListener.onEvent(event);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							} finally {
								finished.set(true);
								safeClearBusy();
								if (activated) {
									try {
										Executions.deactivate(desktop);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonTimerHelper.java:145");
									}
								}
								if (pushDilepas.compareAndSet(false, true)) {
									decrefServerPush(desktop);
								}
							}
						}
					}).start();
					} catch (Exception exSetup) {
						// Gagal sebelum thread latar sempat dimulai (mis. attachTimer/timerWatcher
						// gagal) -- thread latar tidak akan pernah jalan untuk melepas push, jadi
						// lepas di sini sebagai fallback.
						if (pushDilepas.compareAndSet(false, true)) {
							decrefServerPush(desktop);
						}
						Common.tampilErrorJikaAdmin(exSetup);
					}
				}
			});
			timerStart.start();
		} catch (Exception e) {
			decrefServerPush(desktopUntukCatch);
			runFallback(eventListener, e);
		}
	}

	public static void createDefaultTimerNoBusy(final EventListener eventListener, String info, final Boolean repeat,
			final Integer interfal) {
		try {
			if (!hasZkExecution()) {
				runFallback(eventListener, null);
				return;
			}
			final Timer timer = new Timer(interfal != null ? interfal : 100);
			attachTimer(timer);
			timer.setRepeats(repeat != null ? repeat : false);

			timer.addEventListener("onTimer", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						if (repeat == null || !repeat.booleanValue()) {
							timer.stop();
						}
						eventListener.onEvent(event);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					} finally {
						if (repeat == null || !repeat.booleanValue()) {
							timer.detach();
						}
					}
				}
			});
			timer.start();
		} catch (Exception e) {
			runFallback(eventListener, e);
		}
	}


	private static boolean hasZkExecution() {
		try {
			return Executions.getCurrent() != null && Executions.getCurrent().getDesktop() != null;
		} catch (Throwable t) {
			return false;
		}
	}

	private static void safeClearBusy() {
		try {
			if (hasZkExecution()) {
				Clients.clearBusy();
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/CommonTimerHelper.java:206");
		}
	}

	private static void safeShowBusy(String info) {
		try {
			if (hasZkExecution()) {
				Clients.showBusy(info);
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/CommonTimerHelper.java:215");
		}
	}


	private static void attachTimer(Timer timer) {
		if (timer == null) {
			return;
		}
		try {
			Page page = null;
			try {
				if (ExecutionsCtrl.getCurrentCtrl() != null) {
					page = ExecutionsCtrl.getCurrentCtrl().getCurrentPage();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonTimerHelper.java:230");
			}

			if (page != null) {
				/*
				 * Jangan menempelkan Timer ke firstRoot secara langsung.
				 * Pada beberapa halaman firstRoot adalah Borderlayout, sedangkan
				 * Borderlayout hanya menerima North/South/East/West/Center sehingga
				 * Timer akan memunculkan error "Unsupported child for Borderlayout".
				 * Timer dijadikan root component pada Page agar tetap berada di desktop
				 * yang sama tanpa mengganggu struktur layout.
				 */
				try {
					timer.setPage(page);
					return;
				} catch (Exception ePage) { ais.common.ErrorAuditUtil.record(ePage, "auto-audit(empty-catch) src/ais/common/CommonTimerHelper.java:245");
					/*
					 * Page/desktop bisa sudah tidak valid (sedang dibongkar) sehingga
					 * setPage memicu NullPointerException di addMoved. Jangan gagal
					 * total; lanjut ke fallback setParent(root) di bawah.
					 */
				}
			}

			Component root = null;
			if (ExecutionsCtrl.getCurrentCtrl() != null
					&& ExecutionsCtrl.getCurrentCtrl().getCurrentPage() != null) {
				root = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
			}
			if (root != null) {
				timer.setParent(root);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void runFallback(EventListener eventListener, Exception source) {
		try {
			if (eventListener != null) {
				eventListener.onEvent(null);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}
}
