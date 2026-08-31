package ais.action.maintenance;

import java.io.Serializable;
import java.util.Date;

import javax.servlet.http.HttpSession;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Timer;

import ais.common.Common;
import ais.ui.util.WaktuUtil;

/**
 * Helper terfokus untuk main progress. Tipe ini membungkus satu variasi kecil dari alur yang lebih
 * umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String SESSION_MAIN_PROGRESS}, {@code
 * String ATTR_PROGRESS_KEY}; pembacaan/pencarian ({@code getProgressTitle()}); mutasi data ({@code
 * updateProgressPanel()}); operasi domain lain ({@code createProgressPanel()}, {@code masihTerpasang()}, {@code
 * finishProgressPanel()}, {@code errorProgressPanel()}, {@code startProgressAnimation()}, {@code
 * startProgressAnimation()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public final class MainProgressHelper {

	public static final String SESSION_MAIN_PROGRESS = "ais.main.progress.state";
	private static final String ATTR_PROGRESS_KEY = "mainProgressKey";

	private MainProgressHelper() {
	}

	public static Div createProgressPanel(String title, String description, int percent) {
		Div wrapper = new Div();
		wrapper.setWidth("100%");
		wrapper.setStyle("background:#ffffff;border:1px solid #bfdbfe;border-radius:14px;padding:11px 13px;"
				+ "margin-bottom:10px;box-sizing:border-box;box-shadow:0 6px 14px rgba(37,99,235,0.10);");
		wrapper.setAttribute("progressPercent", Integer.valueOf(normalizePercent(percent)));
		String progressKey = "main-progress-" + System.currentTimeMillis() + "-" + System.identityHashCode(wrapper);
		wrapper.setAttribute(ATTR_PROGRESS_KEY, progressKey);

		Label titleLabel = new Label(title == null ? "Memuat data" : title);
		titleLabel.setStyle("display:block;font-size:12px;font-weight:bold;color:#1d4ed8;margin-bottom:3px;");
		titleLabel.setParent(wrapper);
		wrapper.setAttribute("progressTitle", titleLabel);

		Label descriptionLabel = new Label(description == null ? "Mohon tunggu sebentar" : description);
		descriptionLabel.setStyle("display:block;font-size:11px;color:#64748b;margin-bottom:8px;white-space:normal;");
		descriptionLabel.setParent(wrapper);
		wrapper.setAttribute("progressDescription", descriptionLabel);

		Div barOuter = new Div();
		barOuter.setWidth("100%");
		barOuter.setStyle("height:10px;background:#e2e8f0;border-radius:999px;overflow:hidden;");
		barOuter.setParent(wrapper);

		Div barInner = new Div();
		barInner.setHeight("10px");
		barInner.setWidth(normalizePercent(percent) + "%");
		barInner.setStyle("height:10px;width:" + normalizePercent(percent)
				+ "%;background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));border-radius:999px;transition:width .25s ease;");
		barInner.setParent(barOuter);
		wrapper.setAttribute("progressBar", barInner);

		Label percentLabel = new Label(normalizePercent(percent) + "%");
		percentLabel
				.setStyle("display:block;text-align:right;font-size:11px;font-weight:bold;color:#1e40af;margin-top:5px;");
		percentLabel.setParent(wrapper);
		wrapper.setAttribute("progressLabel", percentLabel);

		storeProgressState(new ProgressState(progressKey, title, description, percent, false));
		return wrapper;
	}

	/**
	 * Panel progress sering di-update dari thread background; bila user sudah
	 * pindah halaman/desktop berakhir, komponen menjadi detached dan
	 * smartUpdate (setValue/setStyle/setWidth) melempar NPE di
	 * getAttachedUiEngine. Semua method update di bawah memeriksa ini dulu.
	 */
	private static boolean masihTerpasang(Component component) {
		try {
			return component != null && component.getDesktop() != null && component.getPage() != null;
		} catch (Exception e) {
			return false;
		}
	}

	public static void updateProgressPanel(Div progress, String description, int percent) {
		if (progress == null) {
			return;
		}
		percent = normalizePercent(percent);
		if (!masihTerpasang(progress)) {
			/* Komponen sudah detached: cukup simpan state untuk dilanjutkan
			 * halaman lain, jangan sentuh UI. */
			storeProgressState(new ProgressState((String) progress.getAttribute(ATTR_PROGRESS_KEY),
					getProgressTitle(progress), description, percent, false));
			return;
		}
		progress.setAttribute("progressPercent", Integer.valueOf(percent));
		Object descObj = progress.getAttribute("progressDescription");
		if (descObj instanceof Label && description != null) {
			((Label) descObj).setValue(description);
		}
		Object barObj = progress.getAttribute("progressBar");
		if (barObj instanceof Div) {
			Div bar = (Div) barObj;
			bar.setWidth(percent + "%");
			bar.setStyle("height:10px;width:" + percent
					+ "%;background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));border-radius:999px;transition:width .25s ease;");
		}
		Object labelObj = progress.getAttribute("progressLabel");
		if (labelObj instanceof Label) {
			((Label) labelObj).setValue(percent + "%");
		}
		storeProgressState(new ProgressState((String) progress.getAttribute(ATTR_PROGRESS_KEY),
				getProgressTitle(progress), description, percent, false));
	}

	public static void finishProgressPanel(Div progress, String description, boolean hideWhenDone) {
		if (progress == null) {
			return;
		}
		updateProgressPanel(progress, description == null ? "Selesai" : description, 100);
		storeProgressState(new ProgressState((String) progress.getAttribute(ATTR_PROGRESS_KEY),
				getProgressTitle(progress), description == null ? "Selesai" : description, 100, true));
		if (hideWhenDone && masihTerpasang(progress)) {
			progress.setVisible(false);
		}
	}

	public static void errorProgressPanel(Div progress, String description) {
		if (progress == null) {
			return;
		}
		if (!masihTerpasang(progress)) {
			storeProgressState(new ProgressState((String) progress.getAttribute(ATTR_PROGRESS_KEY),
					getProgressTitle(progress), description == null ? "Data belum dapat dimuat" : description, 100,
					true));
			return;
		}
		progress.setStyle("background:#fff7f7;border:1px solid #fecaca;border-radius:14px;padding:11px 13px;"
				+ "margin-bottom:10px;box-sizing:border-box;box-shadow:0 6px 14px rgba(220,38,38,0.10);");
		Object titleObj = progress.getAttribute("progressTitle");
		if (titleObj instanceof Label) {
			((Label) titleObj).setStyle("display:block;font-size:12px;font-weight:bold;color:#b91c1c;margin-bottom:3px;");
		}
		updateProgressPanel(progress, description == null ? "Data belum dapat dimuat" : description, 100);
		storeProgressState(new ProgressState((String) progress.getAttribute(ATTR_PROGRESS_KEY),
				getProgressTitle(progress), description == null ? "Data belum dapat dimuat" : description, 100, true));
	}

	public static void startProgressAnimation(final Div progress, final String finishDescription,
			final boolean hideWhenDone) {
		startProgressAnimation(progress, null, finishDescription, hideWhenDone);
	}

	public static void startProgressAnimation(final Div progress, final Component revealWhenDone,
			final String finishDescription, final boolean hideWhenDone) {
		if (progress == null) {
			revealComponent(revealWhenDone);
			return;
		}
		final Timer timer = new Timer(220);
		timer.setRepeats(true);
		try {
			timer.setParent(progress);
		} catch (Exception e) {
			finishProgressPanel(progress, finishDescription == null ? "Selesai" : finishDescription, hideWhenDone);
			revealComponent(revealWhenDone);
			showError(e);
			return;
		}
		timer.addEventListener("onTimer", new EventListener() {
			public void onEvent(Event event) throws Exception {
				int percent = 0;
				Object value = progress.getAttribute("progressPercent");
				if (value instanceof Integer) {
					percent = ((Integer) value).intValue();
				}
				percent += percent < 70 ? 22 : 15;
				if (percent >= 100) {
					finishProgressPanel(progress, finishDescription == null ? "Selesai" : finishDescription,
							hideWhenDone);
					revealComponent(revealWhenDone);
					timer.stop();
					timer.detach();
				} else {
					updateProgressPanel(progress, "Sedang memproses data awal", percent);
				}
			}
		});
		timer.start();
	}

	private static void revealComponent(Component component) {
		if (component == null) {
			return;
		}
		try {
			component.setVisible(true);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainProgressHelper.java:197");
		}
		try {
			component.invalidate();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainProgressHelper.java:201");
		}
	}

	public static int normalizePercent(int percent) {
		if (percent < 0) {
			return 0;
		}
		if (percent > 100) {
			return 100;
		}
		return percent;
	}

	private static String getProgressTitle(Div progress) {
		if (progress == null) {
			return "Memuat data";
		}
		Object titleObj = progress.getAttribute("progressTitle");
		if (titleObj instanceof Label) {
			return ((Label) titleObj).getValue();
		}
		return "Memuat data";
	}

	private static void storeProgressState(ProgressState progressState) {
		if (progressState == null) {
			return;
		}
		try {
			org.zkoss.zk.ui.Session zkSession = Sessions.getCurrent();
			if (zkSession != null) {
				zkSession.setAttribute(SESSION_MAIN_PROGRESS, progressState);
				Object nativeSession = zkSession.getNativeSession();
				if (nativeSession instanceof HttpSession) {
					((HttpSession) nativeSession).setAttribute(SESSION_MAIN_PROGRESS, progressState);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainProgressHelper.java:239");
		}
	}

	private static void showError(Exception e) {
		try {
			Common.tampilErrorJikaAdmin(e);
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainProgressHelper.java:246");
		}
	}

	public static class ProgressState implements Serializable {
		private static final long serialVersionUID = 1L;
		public String key;
		public String title;
		public String description;
		public int percent;
		public boolean done;
		public Date updatedAt;

		public ProgressState(String key, String title, String description, int percent, boolean done) {
			this.key = key;
			this.title = title == null ? "Memuat data" : title;
			this.description = description == null ? "Mohon tunggu sebentar" : description;
			this.percent = normalizePercent(percent);
			this.done = done;
			this.updatedAt = WaktuUtil.getDate();
		}
	}
}
