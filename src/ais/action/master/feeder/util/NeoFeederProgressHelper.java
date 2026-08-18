package ais.action.master.feeder.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Timer;

/**
 * {@code NeoFeederProgressHelper} — komponen <b>popup progress melayang</b> yang dipakai bersama
 * (reuse) oleh SELURUH proses sinkronisasi Neo Feeder (kirim data ke Feeder maupun ambil data dari
 * Feeder). Menggantikan tampilan {@code Clients.showBusy(...)} bawaan (overlay teks polos tanpa bar)
 * dengan sebuah kartu melayang modern yang menampilkan <b>judul proses, persentase besar, progress
 * bar bergradien, indikator shimmer/spinner "sedang bekerja", dan baris detail item</b> yang sedang
 * diproses. Kartu bersifat responsif (lebar {@code min(92vw, 420px)}) sehingga enak dipandang baik
 * di desktop maupun perangkat mobile.
 *
 * <h3>Kompatibel drop-in dengan {@code Common.displayLoadBar}</h3>
 * Method {@link #show(String, EventListener)} mengembalikan sebuah {@link Label} "handle" — persis
 * seperti {@code Common.displayLoadBar(EventListener)} — sehingga kode pemanggil yang sudah ada
 * cukup mengganti satu baris pembuatan load bar tanpa menyentuh logika thread latarnya. Thread latar
 * tetap menulis progres via {@code handle.setValue("Memproses X (50%)")} dan menandai selesai dengan
 * {@code handle.setValue("")} — kontrak yang sama dengan {@code displayLoadBar}.
 *
 * <h3>Cara kerja</h3>
 * <ol>
 *   <li>Saat dipanggil (di UI thread, dalam eksekusi event aktif) helper membangun overlay + kartu
 *       melayang serta sebuah {@link Label} tersembunyi ({@code handle}) sebagai papan-tulis progres.</li>
 *   <li>Sebuah {@link Timer} ZK berjalan di UI thread dan mem-<i>poll</i> nilai {@code handle} tiap
 *       ~250&nbsp;ms. Karena hanya UI thread yang menyentuh komponen visual (progress bar, label
 *       persen, label detail), tidak diperlukan server-push dan tidak ada pelanggaran thread ZK —
 *       persis pola yang dipakai {@code LoadBarUtils.displayLoadBar}.</li>
 *   <li><b>PENTING (koreksi):</b> {@code handle.setValue(teks)} TIDAK aman dipanggil langsung dari
 *       thread latar walau {@code handle} tersembunyi/tak dirender — {@code Label.setValue()} tetap
 *       memanggil {@code smartUpdate()} yang butuh eksekusi event ZK aktif pada thread pemanggil,
 *       dan akan melempar {@code IllegalStateException: Components can be accessed only in event
 *       listeners} begitu event asli (yang membuat handle ini) sudah selesai/response terkirim —
 *       persis kondisi thread latar yang baru mulai bekerja. Thread latar WAJIB memakai
 *       {@link #updateProgres(Label, String)} (bukan {@code handle.setValue(...)} langsung), yang
 *       menjadwalkan pembaruan lewat {@code Executions.schedule(desktop, ...)} pada Desktop yang
 *       direkam saat {@code show()} dipanggil.</li>
 * </ol>
 *
 * <h3>Ekstraksi persentase</h3>
 * Teks progres proses Feeder sudah memuat pola persentase, mis. {@code "Memproses FR406 (50%)"} atau
 * {@code "... (33.3%)"}. Helper mengambil angka persen <b>terakhir</b> pada teks via regex
 * {@code (\d+(?:[.,]\d+)?)\s*%}. Bila persentase tidak ditemukan (total tak diketahui), bar tetap
 * hidup lewat animasi shimmer dan hanya menampilkan teks detail — sehingga pengguna tetap melihat
 * indikasi "sedang bekerja".
 *
 * <h3>Semantik selesai / error (identik {@code displayLoadBar})</h3>
 * <ul>
 *   <li>{@code handle.setValue("")} atau teks mengandung "selesai" &rarr; proses dianggap SELESAI:
 *       popup ditutup, lalu {@code onFinish.onEvent(null)} dipanggil.</li>
 *   <li>Teks diawali "Error" &rarr; proses dianggap GAGAL: popup ditutup dan
 *       {@code onFinish.onEvent(new Event(teks))} dipanggil (pemanggil menampilkan pesan/unduh log).</li>
 * </ul>
 *
 * <h3>Manajemen resource</h3>
 * Helper ini murni UI dan <b>tidak membuka session Hibernate</b> apa pun. Saat selesai/gagal, timer,
 * overlay, dan handle di-<i>detach</i> dari halaman sehingga tidak ada komponen menggantung.
 *
 * <h3>Kompatibilitas</h3>
 * Ditulis agar kompatibel dengan <b>Java 1.7</b> (anonymous class, tanpa lambda) dan <b>ZK 5.5</b>
 * (komponen {@link Div}/{@link Label}/{@link Timer}/{@link Html}, styling via CSS inline + satu blok
 * {@code <style>} untuk keyframes animasi).
 *
 * @author Tim AIS
 */
public class NeoFeederProgressHelper {

	/** Interval poll timer (milidetik). */
	private static final int POLL_MS = 250;

	/** Nama attribute komponen tempat menyimpan Desktop pemilik popup (lihat {@link #updateProgres}). */
	private static final String NF_DESKTOP_ATTR = "neoFeederProgressHelper.desktop";

	/** Pola pencari persentase pada teks progres (angka + optional desimal + '%'). */
	private static final Pattern PCT = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*%");

	/** Kelas utilitas statis — tidak untuk diinstansiasi. */
	private NeoFeederProgressHelper() {
	}

	/**
	 * Menampilkan popup progress melayang pada root halaman aktif.
	 *
	 * @param judul    judul proses (mis. {@code "Mengirim Data ke Neo Feeder"}); boleh {@code null}
	 * @param onFinish listener yang dipanggil saat proses selesai/gagal (boleh {@code null}) —
	 *                 {@code Event} bernilai {@code null} saat sukses, atau {@code new Event(teksError)} saat error
	 * @return {@link Label} handle; thread latar menulis progres ke sini ({@code setValue}) dan
	 *         menandai selesai dengan {@code setValue("")}
	 */
	public static Label show(String judul, EventListener onFinish) {
		Component root = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
		return show(root, judul, onFinish);
	}

	/**
	 * Menampilkan popup progress melayang pada komponen induk tertentu.
	 *
	 * @param parent   komponen induk tempat overlay dipasang; bila {@code null} dipakai root halaman
	 * @param judul    judul proses; boleh {@code null}
	 * @param onFinish listener selesai/gagal; boleh {@code null}
	 * @return {@link Label} handle progres
	 */
	public static Label show(Component parent, final String judul, final EventListener onFinish) {
		final Component root = (parent != null) ? parent
				: ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();

		// Papan-tulis progres tersembunyi yang ditulis thread latar & dibaca timer.
		final Label handle = new Label(ais.common.Common.getBahasaConfig("Menyiapkan data ..."));
		handle.setMultiline(true);
		handle.setVisible(false);
		handle.setParent(root);

		// FIX IllegalStateException "Components can be accessed only in event listeners":
		// simpan Desktop pemilik halaman ini SEKARANG (masih dalam eksekusi event yang valid)
		// sbg attribute komponen, supaya updateProgres() bisa dipanggil belakangan dari thread
		// latar TANPA butuh konteks Execution aktif pada thread itu sendiri.
		final Desktop desktopSaatIni;
		Desktop desktopSementara = null;
		try {
			if (Executions.getCurrent() != null) {
				desktopSementara = Executions.getCurrent().getDesktop();
				handle.setAttribute(NF_DESKTOP_ATTR, desktopSementara);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederProgressHelper.java:show.desktop"); }
		desktopSaatIni = desktopSementara;

		// FIX popup "macet selamanya" (loading bar tampil tapi tidak pernah bergerak/menutup):
		// TANPA server push, pembaruan lewat Executions.schedule() (updateProgres) hanya
		// benar-benar terkirim ke browser saat SIKLUS POLLING Timer client berikutnya tiba di
		// server. Bila siklus itu terhenti sekali saja (mis. request timeout/jaringan), sinyal
		// SELESAI/ERROR tidak pernah sampai ke client walau proses di thread latar sudah kelar —
		// popup terlihat macet padahal backend sudah selesai. Server push mendorong pembaruan
		// LANGSUNG ke browser (comet), tidak bergantung siklus polling. Pola yang sama sudah
		// dipakai aman di modul lain (Chatter, ChatUsers, RekapHasilTugasPerTugasDanUjianObe).
		//
		// PENTING: server push adalah SATU sakelar per Desktop (dipakai bersama fitur lain, mis.
		// Chat). Hanya MATIKAN lagi nanti bila KAMI yang menyalakannya di sini (akuYangMengaktifkan)
		// — supaya tidak mematikan server push milik fitur lain yang kebetulan sedang aktif.
		final boolean akuYangMengaktifkanServerPush;
		boolean sdhAktifSebelumnya = true;
		try {
			if (desktopSaatIni != null) {
				sdhAktifSebelumnya = desktopSaatIni.isServerPushEnabled();
				if (!sdhAktifSebelumnya) {
					desktopSaatIni.enableServerPush(true);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederProgressHelper.java:show.serverPush"); }
		akuYangMengaktifkanServerPush = desktopSaatIni != null && !sdhAktifSebelumnya;

		// Overlay layar penuh (backdrop) yang memusatkan kartu.
		final Div overlay = new Div();
		overlay.setParent(root);
		overlay.setStyle("position:fixed;top:0;left:0;right:0;bottom:0;z-index:100000;display:flex;"
				+ "align-items:center;justify-content:center;background:rgba(15,23,42,.45);padding:16px;");

		// Keyframes animasi (spinner + shimmer) — cukup disuntik sekali per popup.
		Html style = new Html();
		style.setContent("<style>@keyframes nf-spin{to{transform:rotate(360deg)}}"
				+ "@keyframes nf-shim{0%{transform:translateX(-130%)}100%{transform:translateX(380%)}}</style>");
		style.setParent(overlay);

		// Kartu melayang.
		Div card = new Div();
		card.setParent(overlay);
		card.setStyle("width:420px;max-width:92vw;box-sizing:border-box;background:#ffffff;border-radius:16px;"
				+ "box-shadow:0 20px 55px rgba(2,6,23,.35);padding:22px 22px 20px;"
				+ "font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;");

		// Header: spinner + judul.
		Div head = new Div();
		head.setParent(card);
		head.setStyle("display:flex;align-items:center;margin-bottom:14px;");
		Div spin = new Div();
		spin.setParent(head);
		spin.setStyle("width:18px;height:18px;flex:0 0 auto;margin-right:10px;border-radius:50%;"
				+ "border:3px solid #dbeafe;border-top-color:#2563eb;animation:nf-spin .8s linear infinite;");
		Label title = new Label((judul == null || judul.trim().isEmpty()) ? "Sinkronisasi Neo Feeder" : judul);
		title.setParent(head);
		title.setStyle("font-size:15px;font-weight:700;color:#0f172a;");

		// Persentase besar.
		final Label percent = new Label("");
		percent.setParent(card);
		percent.setStyle("display:block;font-size:30px;font-weight:800;color:#1d4ed8;line-height:1;margin-bottom:12px;");

		// Track + fill + shimmer.
		Div track = new Div();
		track.setParent(card);
		track.setStyle("position:relative;height:14px;background:#eef2f7;border-radius:999px;overflow:hidden;");
		final Div fill = new Div();
		fill.setParent(track);
		final String fillBase = "height:100%;border-radius:999px;transition:width .35s ease;"
				+ "background:linear-gradient(90deg,#2563eb,#4f46e5);";
		fill.setStyle(fillBase + "width:0%;");
		final Div shim = new Div();
		shim.setParent(track);
		shim.setStyle("position:absolute;top:0;left:0;height:100%;width:35%;"
				+ "background:linear-gradient(90deg,transparent,rgba(255,255,255,.6),transparent);"
				+ "animation:nf-shim 1.4s infinite;");

		// Baris detail item.
		final Label detail = new Label(ais.common.Common.getBahasaConfig("Menyiapkan data ..."));
		detail.setParent(card);
		detail.setMultiline(true);
		detail.setStyle("display:block;margin-top:12px;font-size:12.5px;color:#475569;line-height:1.45;"
				+ "max-height:60px;overflow:hidden;word-break:break-word;");

		// Timer UI-thread yang mem-poll handle dan memperbarui bar. Dideklarasikan LEBIH DULU
		// (sebelum tombol Tutup) agar bisa dirujuk oleh handler tombol Tutup di bawah.
		final Timer timer = new Timer(POLL_MS);

		// Tombol Tutup — JARING PENGAMAN bila popup "macet" (mis. sinyal selesai gagal terkirim ke
		// browser walau proses di server sudah kelar — lihat catatan server push di atas). Menutup
		// TAMPILAN saja secara paksa; TIDAK membatalkan proses di thread latar (yang mungkin masih
		// berjalan/sudah selesai di server).
		final org.zkoss.zul.Toolbarbutton tutup = new org.zkoss.zul.Toolbarbutton(
				ais.common.Common.getBahasaConfig("Tutup"));
		tutup.setStyle("display:block;margin-top:14px;font-size:11.5px;color:#94a3b8;text-align:right;"
				+ "cursor:pointer;text-decoration:underline;");
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				int jawab = ais.ui.util.MyMessageboxConfig.show(
						"Tutup jendela progres ini? Proses di server BISA JADI masih berjalan/sudah "
								+ "selesai di latar — menutup jendela ini hanya menyembunyikan tampilan, "
								+ "tidak membatalkan proses.",
						"Tutup Progres", ais.ui.util.MyMessageboxConfig.OK | ais.ui.util.MyMessageboxConfig.CANCEL,
						ais.ui.util.MyMessageboxConfig.QUESTION);
				if (jawab != ais.ui.util.MyMessageboxConfig.OK.intValue()) {
					return;
				}
				tutupPopup(overlay, handle, timer, desktopSaatIni, akuYangMengaktifkanServerPush);
			}
		});
		tutup.setParent(card);

		timer.setParent(root);
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String status = handle.getValue();
				if (status == null) {
					status = "";
				}
				String s = status.trim();

				boolean error = s.length() >= 5 && s.substring(0, 5).equalsIgnoreCase("Error");
				boolean done = s.length() == 0 || s.toLowerCase().contains("selesai");

				if (!done && !error) {
					double pct = parsePercent(s);
					if (pct >= 0) {
						if (pct > 100) {
							pct = 100;
						}
						long lebar = Math.round(pct);
						fill.setStyle(fillBase + "width:" + lebar + "%;");
						percent.setValue(formatPct(pct));
					} else {
						percent.setValue("");
					}
					detail.setValue(s);
					return;
				}

				// Selesai / gagal — bersihkan komponen lalu picu listener pemanggil.
				tutupPopup(overlay, handle, timer, desktopSaatIni, akuYangMengaktifkanServerPush);
				if (onFinish != null) {
					onFinish.onEvent(error ? new Event(status) : null);
				}
			}
		});
		timer.start();

		return handle;
	}

	/**
	 * Membersihkan popup progres (detach overlay/handle/timer) &amp; menonaktifkan kembali server
	 * push yang diaktifkan {@link #show(Component, String, EventListener)} — dipakai baik oleh alur
	 * SELESAI/ERROR normal (timer) maupun tombol Tutup manual (jaring pengaman). Aman dipanggil
	 * berkali-kali (tiap langkah dibungkus try/catch sendiri, komponen yang sudah detached diabaikan).
	 */
	private static void tutupPopup(Div overlay, Label handle, Timer timer, Desktop desktop,
			boolean matikanServerPush) {
		try {
			overlay.detach();
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederProgressHelper.java:tutupPopup.overlay");
		}
		try {
			handle.detach();
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederProgressHelper.java:tutupPopup.handle");
		}
		try {
			if (timer != null) {
				timer.detach();
			}
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederProgressHelper.java:tutupPopup.timer");
		}
		try {
			if (matikanServerPush && desktop != null && desktop.isServerPushEnabled()) {
				desktop.enableServerPush(false);
			}
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederProgressHelper.java:tutupPopup.serverPush");
		}
	}

	/**
	 * Perbarui teks progres {@code handle} dengan AMAN dari thread mana pun (termasuk thread
	 * latar tanpa konteks eksekusi ZK). WAJIB dipakai oleh thread latar SEBAGAI GANTI
	 * {@code handle.setValue(teks)} langsung — lihat catatan "PENTING (koreksi)" di Javadoc kelas.
	 *
	 * <p>Bekerja dengan menjadwalkan pembaruan lewat {@code Executions.schedule(desktop, ...)} pada
	 * {@link Desktop} yang direkam saat {@link #show(Component, String, EventListener)} dipanggil.
	 * Bila desktop sudah tidak hidup (mis. user menutup halaman sebelum proses latar selesai),
	 * pembaruan diabaikan dengan aman (tidak melempar exception, tidak menghentikan proses latar
	 * yang sedang berjalan).</p>
	 *
	 * @param handle handle hasil {@link #show(Component, String, EventListener)}; boleh {@code null}
	 *               (no-op)
	 * @param value  teks progres baru, mis. {@code "Mengirim data ... (50%)"} atau {@code ""} utk
	 *               menandai selesai
	 */
	public static void updateProgres(final Label handle, final String value) {
		if (handle == null) {
			return;
		}
		try {
			Object desktopAttr = handle.getAttribute(NF_DESKTOP_ATTR);
			final Desktop desktop = (desktopAttr instanceof Desktop) ? (Desktop) desktopAttr : null;
			if (desktop == null || !desktop.isAlive()) {
				return;
			}
			Executions.schedule(desktop, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					handle.setValue(value);
				}
			}, new Event("onUpdateProgresNeoFeeder"));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederProgressHelper.java:updateProgres");
		}
	}

	/**
	 * Perbarui {@link Label} dengan AMAN dari thread mana pun, TANPA memerlukan {@code label} berasal
	 * dari {@link #show}. Dipakai oleh pemanggil lama seperti {@code FeederExporter}/{@code
	 * FeederImporter} yang menerima field {@code Label}/{@code Progressmeter} langsung lewat
	 * constructor (bisa berupa komponen asli yang sudah terpasang di halaman, ATAU objek "bayangan"
	 * yang sengaja tak pernah dipasang ke halaman mana pun — lihat pola {@code
	 * Common.displayLoadBar}). Deteksi otomatis lewat {@link Component#getDesktop()}: bila komponen
	 * belum terpasang ke Desktop manapun ({@code null}), penulisan langsung aman (tidak memicu
	 * {@code smartUpdate}); bila sudah terpasang, dijadwalkan lewat {@code Executions.schedule} persis
	 * seperti {@link #updateProgres(Label, String)}.
	 *
	 * @param label komponen; boleh {@code null} (no-op)
	 * @param value nilai teks baru
	 */
	public static void setLabelValueSafe(final Label label, final String value) {
		safeApply(label, new Runnable() {
			@Override
			public void run() {
				label.setValue(value);
			}
		});
	}

	/**
	 * Sama seperti {@link #setLabelValueSafe(Label, String)} tetapi untuk {@link Progressmeter}.
	 *
	 * @param meter komponen; boleh {@code null} (no-op)
	 * @param value nilai persentase baru (0..100)
	 */
	public static void setProgressmeterValueSafe(final Progressmeter meter, final int value) {
		safeApply(meter, new Runnable() {
			@Override
			public void run() {
				meter.setValue(value);
			}
		});
	}

	/**
	 * Inti mekanisme aman-lintas-thread untuk {@link #setLabelValueSafe} dan
	 * {@link #setProgressmeterValueSafe}: komponen detached (belum {@code setParent()}-kan ke halaman
	 * manapun, {@code getDesktop() == null}) tidak memicu pemeriksaan konteks event ZK sama sekali
	 * sehingga boleh ditulis langsung; komponen yang sudah terpasang ke halaman WAJIB lewat
	 * {@code Executions.schedule(desktop, ...)} agar tidak melempar
	 * {@code IllegalStateException: Components can be accessed only in event listeners} saat dipanggil
	 * dari {@code Thread} latar.
	 *
	 * @param comp    komponen target; boleh {@code null} (no-op)
	 * @param applier aksi yang benar-benar mengubah nilai komponen (dijalankan langsung atau dijadwalkan)
	 */
	private static void safeApply(final Component comp, final Runnable applier) {
		if (comp == null || applier == null) {
			return;
		}
		try {
			Desktop desktop = comp.getDesktop();
			if (desktop == null) {
				applier.run();
				return;
			}
			if (!desktop.isAlive()) {
				return;
			}
			Executions.schedule(desktop, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					applier.run();
				}
			}, new Event("onSafeUpdateNeoFeeder"));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederProgressHelper.java:safeApply");
		}
	}

	/**
	 * Mengambil nilai persentase <b>terakhir</b> yang muncul pada teks progres.
	 *
	 * @param s teks progres
	 * @return persentase 0..100, atau {@code -1} bila tidak ditemukan
	 */
	private static double parsePercent(String s) {
		double pct = -1;
		try {
			Matcher m = PCT.matcher(s);
			while (m.find()) {
				pct = Double.parseDouble(m.group(1).replace(',', '.'));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederProgressHelper.java:239");
		}
		return pct;
	}

	/**
	 * Memformat persentase: tanpa desimal bila (mendekati) bulat, selain itu satu angka desimal.
	 *
	 * @param pct persentase
	 * @return teks persentase, mis. {@code "50%"} atau {@code "33.3%"}
	 */
	private static String formatPct(double pct) {
		if (Math.abs(pct - Math.round(pct)) < 0.05) {
			return Math.round(pct) + "%";
		}
		return (Math.round(pct * 10.0) / 10.0) + "%";
	}
}
