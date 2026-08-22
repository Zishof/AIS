package ais.ui.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import org.json.JSONObject;

import ais.common.Common;
import ais.common.HeadlessActionContext;
import ais.common.ResponseContext;
// Pastikan Anda meng-import RequestContext dan ResponseContext sesuai package aplikasi Anda
// import com.yourpackage.RequestContext; 
// import com.yourpackage.ResponseContext;

public class MyMessageboxConfig {

	public static final String ERROR = Messagebox.ERROR;
	public static String INFORMATION = Messagebox.INFORMATION;
	public static String EXCLAMATION = Messagebox.EXCLAMATION;
	public static Integer OK = Messagebox.OK;
	public static Integer CANCEL = Messagebox.CANCEL;
	public static String QUESTION = Messagebox.QUESTION;
	// Konstanta tombol tambahan agar dialog konfirmasi (Ya/Tidak dll.) dapat sepenuhnya memakai
	// MyMessageboxConfig (multi-bahasa) tanpa perlu menyentuh org.zkoss.zul.Messagebox secara langsung.
	public static Integer YES = Messagebox.YES;
	public static Integer NO = Messagebox.NO;
	public static Integer ABORT = Messagebox.ABORT;
	public static Integer RETRY = Messagebox.RETRY;
	public static Integer IGNORE = Messagebox.IGNORE;

	/**
	 * Method bantuan untuk mendeteksi apakah aplikasi sedang berada dalam 
	 * context ZKoss Execution atau tidak.
	 */
	private static boolean isZkEnvironment() {
		return Executions.getCurrent() != null;
	}

	/**
	 * Method bantuan untuk menuliskan javascript 'tampilkanToast' langsung 
	 * ke HttpServletResponse jika berada di luar ZKoss.
	 */
	private static void triggerGlobalJavascriptToast(String messageCode, String icon) {
		HttpServletResponse response = ResponseContext.get();
		if (response != null) {
			try {
				// Mapping ZK icon menjadi parameter string 'jenis' untuk fungsi javascript
				String jenis = "info"; // Default
				if (ERROR.equals(icon)) {
					jenis = "error";
				} else if (EXCLAMATION.equals(icon)) {
					jenis = "warning";
				} else if (QUESTION.equals(icon)) {
					jenis = "question";
				}

				// Escape agar aman di dalam string JS single-quote:
				// (1) backslash dulu, (2) kutip, (3) newline→spasi agar tidak buat unterminated literal,
				// (4) </ → <\/ agar tidak mematikan script block lebih awal
				String safeMessage = messageCode != null ? messageCode
						.replace("\\", "\\\\")
						.replace("'", "\\'")
						.replace("\"", "\\\"")
						.replace("\r\n", " ").replace("\r", " ").replace("\n", " ")
						.replace("</", "<\\/") : "";

				// Bentuk script pemanggil fungsi global
				String jsScript = "<script type=\"text/javascript\">"
						+ "tampilkanToast('" + safeMessage + "', '" + jenis + "');"
						+ "</script>";

				// Tulis ke output response
				response.setContentType("text/html;charset=UTF-8");
				response.getWriter().write(jsScript);
				response.getWriter().flush();
				
			} catch (IOException e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyMessageboxConfig.java:73");
			}
		}
	}

	/**
	 * Terjemahkan pesan literal ke bahasa aktif via kamus DB {@link Common#getBahasaConfig(String)}.
	 * <b>Idempotent</b>: aman bila {@code messageCode} sudah diterjemahkan (mis. hasil {@link #format}) —
	 * getBahasaConfig mengembalikan apa adanya bila sudah bertanda sentinel. Aman terhadap exception.
	 */
	private static String terjemah(String messageCode) {
		try {
			return Common.getBahasaConfig(messageCode);
		} catch (Exception e) {
			return messageCode;
		}
	}

	/**
	 * Bangun pesan MULTI-BAHASA berparameter: terjemahkan TEMPLATE lalu substitusi placeholder
	 * {@code {V1}},{@code {V2}},... Delegasi ke {@link Common#pesan(String, Object...)}.
	 * <p>Contoh: {@code MyMessageboxConfig.format("Nilai \"{V1}\" tidak valid.", nilai)}.
	 */
	public static String format(String template, Object... args) {
		return Common.pesan(template, args);
	}

	private static boolean hanyaTombolOk(Integer buttons) {
		return buttons != null && buttons.intValue() == Messagebox.OK;
	}

	private static boolean bisaPakaiModern(Integer buttons, EventListener eventListener) {
		return hanyaTombolOk(buttons) || eventListener != null;
	}

	private static String warnaUtama(String icon) {
		if (ERROR.equals(icon)) {
			return "#dc2626";
		}
		if (EXCLAMATION.equals(icon)) {
			return "#b45309";
		}
		if (QUESTION.equals(icon)) {
			return "#2563eb";
		}
		return "#0f766e";
	}

	private static String simbol(String icon) {
		if (ERROR.equals(icon)) {
			return "!";
		}
		if (EXCLAMATION.equals(icon)) {
			return "!";
		}
		if (QUESTION.equals(icon)) {
			return "?";
		}
		return "i";
	}

	private static String stackTrace(Throwable throwable) {
		if (throwable == null) {
			return "";
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}

	private static int indexPenandaDetail(String pesan) {
		if (pesan == null) {
			return -1;
		}
		String lower = pesan.toLowerCase();
		String[] penanda = new String[] { "langkah yang dapat dilakukan:", "langkah perbaikan:",
				"detail error", "info teknis", "informasi teknis", "java exception", "stack trace" };
		int hasil = -1;
		for (int i = 0; i < penanda.length; i++) {
			int idx = lower.indexOf(penanda[i]);
			if (idx >= 0 && (hasil < 0 || idx < hasil)) {
				hasil = idx;
			}
		}
		return hasil;
	}

	private static String ringkasPesanAwal(String pesan) {
		if (pesan == null) {
			return "";
		}
		String value = pesan.trim();
		int idx = indexPenandaDetail(value);
		if (idx > 0) {
			value = value.substring(0, idx).trim();
		}
		int newline = value.indexOf('\n');
		if (newline > 0) {
			value = value.substring(0, newline).trim();
		}
		if (value.length() > 260) {
			int titik = value.lastIndexOf('.', 260);
			value = value.substring(0, titik > 80 ? titik + 1 : 260).trim();
		}
		return value.length() == 0 ? pesan.trim() : value;
	}

	private static String ambilDetailDariPesan(String pesan) {
		if (pesan == null) {
			return "";
		}
		String value = pesan.trim();
		int idx = indexPenandaDetail(value);
		if (idx > 0 && idx < value.length()) {
			return value.substring(idx).trim();
		}
		if (value.indexOf('\n') >= 0 || value.length() > 260) {
			return value;
		}
		return "";
	}

	private static String susunDetail(String pesan, String pesanTampilan, String title, String icon, Throwable throwable,
			String detailTambahan) {
		StringBuilder detail = new StringBuilder();
		detail.append("Waktu : ")
				.append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date())).append("\n");
		detail.append("Judul : ").append(title == null ? "" : title).append("\n");
		detail.append("Jenis : ").append(icon == null ? "" : icon).append("\n\n");
		detail.append("Pesan Singkat yang Ditampilkan:\n").append(pesanTampilan == null ? "" : pesanTampilan)
				.append("\n\n");
		detail.append("Pesan Lengkap:\n").append(pesan == null ? "" : pesan).append("\n\n");
		String detailDariPesan = ambilDetailDariPesan(pesan);
		if (detailTambahan != null && detailTambahan.trim().length() > 0) {
			detail.append("Detail/Saran:\n").append(detailTambahan.trim()).append("\n\n");
		} else if (detailDariPesan.length() > 0) {
			detail.append("Detail/Saran dari pesan:\n").append(detailDariPesan).append("\n\n");
		} else {
			detail.append("Saran umum:\n");
			detail.append("1. Periksa kembali data/filter/input pada form yang sedang diproses.\n");
			detail.append("2. Ulangi proses setelah data yang wajib diisi sudah lengkap.\n");
			detail.append("3. Jika masih gagal, salin detail ini lalu kirim ke admin/teknis.\n\n");
		}
		String trace = stackTrace(throwable);
		if (trace.length() > 0) {
			detail.append("Java Exception:\n").append(trace);
		} else {
			detail.append("Java Exception:\nTidak ada exception Java yang dikirim ke komponen alert ini.");
		}
		return detail.toString();
	}

	private static int tampilModern(final String pesan, final String title, final Integer buttons, final String icon,
			final EventListener eventListener, Throwable throwable, String detailTambahan) throws InterruptedException {
		if (!isZkEnvironment() || !bisaPakaiModern(buttons, eventListener)) {
			return Messagebox.show(pesan, title, buttons, icon, eventListener);
		}

		try {
			String pesanSingkat = ringkasPesanAwal(pesan);
			final String detail = susunDetail(pesan, pesanSingkat, title, icon, throwable, detailTambahan);
			final Window win = new Window();
			win.setTitle("");
			win.setBorder("none");
			win.setClosable(true);
			win.setSizable(false);
			win.setWidth("520px");
			win.setStyle("border-radius:14px;overflow:hidden;box-shadow:0 24px 70px rgba(15,23,42,.28);"
					+ "background:#ffffff;border:1px solid #e5e7eb;");
			win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			String warna = warnaUtama(icon);
			Vbox body = new Vbox();
			body.setWidth("100%");
			body.setSpacing("0");
			body.setStyle("background:#fff;font-family:Arial,sans-serif;");
			body.setParent(win);

			Hbox header = new Hbox();
			header.setWidth("100%");
			header.setAlign("center");
			header.setSpacing("12px");
			header.setStyle("box-sizing:border-box;padding:18px 20px;background:linear-gradient(135deg,"
					+ warna + ",#1d4ed8);color:#fff;");
			header.setParent(body);

			Label iconLabel = new Label(simbol(icon));
			iconLabel.setStyle("display:inline-flex;align-items:center;justify-content:center;width:34px;height:34px;"
					+ "border-radius:50%;background:rgba(255,255,255,.22);font-size:20px;font-weight:800;");
			iconLabel.setParent(header);

			Vbox headerText = new Vbox();
			headerText.setSpacing("2px");
			headerText.setHflex("1");
			headerText.setStyle("min-width:0;");
			headerText.setParent(header);

			Label titleLabel = new Label(title == null || title.trim().length() == 0 ? "Informasi" : title);
			titleLabel.setStyle("font-size:16px;font-weight:800;color:#fff;");
			titleLabel.setParent(headerText);

			Label subTitle = new Label("Klik Detail untuk melihat informasi teknis dan saran perbaikan.");
			subTitle.setStyle("font-size:11px;color:rgba(255,255,255,.86);");
			subTitle.setParent(headerText);

			Button closeHeader = new Button("X");
			closeHeader.setTooltiptext("Tutup");
			closeHeader.setStyle("border:1px solid rgba(255,255,255,.55);background:rgba(15,23,42,.22);"
					+ "color:#fff;border-radius:8px;width:38px;height:38px;padding:0;"
					+ "font-size:16px;font-weight:900;cursor:pointer;");
			closeHeader.setParent(header);
			closeHeader.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					tutupDialog(win, eventListener, Messagebox.CANCEL);
				}
			});
			win.addEventListener("onCancel", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					tutupDialog(win, eventListener, Messagebox.CANCEL);
				}
			});

			Vbox content = new Vbox();
			content.setWidth("100%");
			content.setSpacing("12px");
			content.setStyle("box-sizing:border-box;padding:18px 20px 14px;");
			content.setParent(body);

			Label msg = new Label(pesanSingkat == null ? "" : pesanSingkat);
			msg.setMultiline(true);
			msg.setStyle("font-size:13px;line-height:1.55;color:#1f2937;");
			msg.setParent(content);

			final Vbox detailBox = new Vbox();
			detailBox.setWidth("100%");
			detailBox.setVisible(false);
			detailBox.setSpacing("8px");
			detailBox.setStyle("box-sizing:border-box;padding:10px;border:1px solid #dbeafe;border-radius:8px;"
					+ "background:#f8fafc;");
			detailBox.setParent(content);

			final Textbox detailText = new Textbox(detail);
			detailText.setReadonly(true);
			detailText.setMultiline(true);
			detailText.setRows(10);
			detailText.setWidth("100%");
			detailText.setStyle("box-sizing:border-box;font-family:Consolas,monospace;font-size:11px;"
					+ "line-height:1.45;border:1px solid #cbd5e1;border-radius:6px;background:#fff;");
			detailText.setParent(detailBox);

			Hbox detailButtons = new Hbox();
			detailButtons.setSpacing("8px");
			detailButtons.setStyle("justify-content:flex-end;width:100%;");
			detailButtons.setParent(detailBox);

			final Button copy = new Button("Copy Detail");
			copy.setStyle("border:1px solid #94a3b8;background:#fff;color:#0f172a;border-radius:7px;"
					+ "padding:5px 10px;font-weight:700;");
			copy.setParent(detailButtons);
			copy.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					String quoted = JSONObject.quote(detailText.getValue() == null ? "" : detailText.getValue());
					Clients.evalJavaScript("(function(t){"
							+ "if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(t);}"
							+ "else{var a=document.createElement('textarea');a.value=t;document.body.appendChild(a);"
							+ "a.select();try{document.execCommand('copy');}catch(e){}document.body.removeChild(a);}"
							+ "})( " + quoted + " );");
					copy.setLabel("Tersalin");
				}
			});

			Hbox footer = new Hbox();
			footer.setWidth("100%");
			footer.setSpacing("8px");
			footer.setPack("end");
			footer.setStyle("box-sizing:border-box;padding:12px 20px 18px;background:#f9fafb;"
					+ "border-top:1px solid #e5e7eb;");
			footer.setParent(body);

			Button detailBtn = new Button("Detail");
			detailBtn.setStyle("border:1px solid #cbd5e1;background:#fff;color:#334155;border-radius:8px;"
					+ "padding:6px 12px;font-weight:800;");
			detailBtn.setParent(footer);
			detailBtn.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					detailBox.setVisible(!detailBox.isVisible());
				}
			});

			tambahTombolAksi(footer, win, eventListener, buttons, warna);

			win.doModal();
			return OK;
		} catch (Exception e) {
			return Messagebox.show(pesan, title, buttons, icon, eventListener);
		}
	}

	private static int tampilModern(final String pesan, final String title, final Integer buttons, final String icon,
			final EventListener eventListener) throws InterruptedException {
		return tampilModern(pesan, title, buttons, icon, eventListener, null, null);
	}

	private static void tambahTombolAksi(Hbox footer, final Window win, final EventListener eventListener,
			Integer buttons, String warna) {
		int mask = buttons == null ? Messagebox.OK : buttons.intValue();
		boolean ada = false;
		if ((mask & Messagebox.YES) != 0) {
			tambahTombol(footer, win, eventListener, "Ya", Messagebox.YES, warna, true);
			ada = true;
		}
		if ((mask & Messagebox.NO) != 0) {
			tambahTombol(footer, win, eventListener, "Tidak", Messagebox.NO, warna, false);
			ada = true;
		}
		if ((mask & Messagebox.OK) != 0) {
			tambahTombol(footer, win, eventListener, mask == Messagebox.OK ? "Tutup" : "OK",
					Messagebox.OK, warna, true);
			ada = true;
		}
		if ((mask & Messagebox.CANCEL) != 0) {
			tambahTombol(footer, win, eventListener, "Batal", Messagebox.CANCEL, warna, false);
			ada = true;
		}
		if ((mask & Messagebox.RETRY) != 0) {
			tambahTombol(footer, win, eventListener, "Ulangi", Messagebox.RETRY, warna, true);
			ada = true;
		}
		if ((mask & Messagebox.ABORT) != 0) {
			tambahTombol(footer, win, eventListener, "Batalkan", Messagebox.ABORT, warna, false);
			ada = true;
		}
		if ((mask & Messagebox.IGNORE) != 0) {
			tambahTombol(footer, win, eventListener, "Abaikan", Messagebox.IGNORE, warna, false);
			ada = true;
		}
		if (!ada) {
			tambahTombol(footer, win, eventListener, "Tutup", Messagebox.OK, warna, true);
		}
	}

	private static void tutupDialog(Window win, EventListener eventListener, int kode) throws Exception {
		if (win != null && win.getParent() != null) {
			win.detach();
		}
		if (eventListener != null) {
			eventListener.onEvent(new Event(namaEventTombol(kode), win, Integer.valueOf(kode)));
		}
	}

	private static String namaEventTombol(int kode) {
		if (kode == Messagebox.YES) {
			return "onYes";
		}
		if (kode == Messagebox.NO) {
			return "onNo";
		}
		if (kode == Messagebox.CANCEL) {
			return "onCancel";
		}
		if (kode == Messagebox.RETRY) {
			return "onRetry";
		}
		if (kode == Messagebox.ABORT) {
			return "onAbort";
		}
		if (kode == Messagebox.IGNORE) {
			return "onIgnore";
		}
		return "onOK";
	}

	private static void tambahTombol(Hbox footer, final Window win, final EventListener eventListener, String label,
			final int kode, String warna, boolean utama) {
		Button button = new Button(label);
		button.setStyle(utama
				? "border:0;background:" + warna + ";color:#fff;border-radius:8px;padding:6px 18px;font-weight:800;"
				: "border:1px solid #cbd5e1;background:#fff;color:#334155;border-radius:8px;"
						+ "padding:6px 12px;font-weight:800;");
		button.setParent(footer);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				tutupDialog(win, eventListener, kode);
			}
		});
	}

	public static int showDetail(String messageCode, String titleCode, Integer buttons, String icon,
			Throwable throwable, String detailTambahan) throws InterruptedException {
		String pesan = terjemah(messageCode);
		if (HeadlessActionContext.isActive()) {
			HeadlessActionContext.record(ringkasPesanAwal(pesan) + "\n"
					+ susunDetail(pesan, ringkasPesanAwal(pesan), titleCode, icon, throwable, detailTambahan));
			return OK;
		}
		if (isZkEnvironment()) {
			return tampilModern(pesan, Common.getBahasaConfig(titleCode), buttons, icon, null, throwable,
					detailTambahan);
		}
		triggerGlobalJavascriptToast(pesan, icon);
		return OK;
	}

	public static int showDetail(String messageCode, Throwable throwable) throws InterruptedException {
		return showDetail(messageCode, "Peringatan", OK, ERROR, throwable, null);
	}

	public static int showDetail(String messageCode, Throwable throwable, String detailTambahan)
			throws InterruptedException {
		return showDetail(messageCode, "Peringatan", OK, ERROR, throwable, detailTambahan);
	}

	public static int showDetail(String messageCode, String titleCode, String icon, String detailTambahan)
			throws InterruptedException {
		return showDetail(messageCode, titleCode, OK, icon, null, detailTambahan);
	}

	public static void show(String messageCode) throws InterruptedException {
		String pesan = terjemah(messageCode);
		if (HeadlessActionContext.isActive()) {
			HeadlessActionContext.record(pesan);
			return;
		}
		if (isZkEnvironment()) {
			tampilModern(pesan, Common.getBahasaConfig("Informasi"), Messagebox.OK, Messagebox.INFORMATION, null);
		} else {
			triggerGlobalJavascriptToast(pesan, INFORMATION);
		}
	}

	public static int show(String messageCode, String titleCode, Integer buttons, String icon)
			throws InterruptedException {
		String pesan = terjemah(messageCode);
		if (HeadlessActionContext.isActive()) {
			HeadlessActionContext.record(pesan);
			return OK;
		}
		if (isZkEnvironment()) {
			// Mengembalikan tombol yang diklik (mode sinkron ZK) agar pemanggil dapat memeriksa
			// hasil, mis. if (MyMessageboxConfig.show(...) == MyMessageboxConfig.YES) { ... }.
			return tampilModern(pesan, Common.getBahasaConfig(titleCode), buttons, icon, null);
		} else {
			triggerGlobalJavascriptToast(pesan, icon);
			return OK;
		}
	}

	public static int show(String messageCode, String titleCode, int buttons, String icon, EventListener eventListener)
			throws InterruptedException {
		String pesan = terjemah(messageCode);
		if (HeadlessActionContext.isActive()) {
			HeadlessActionContext.record(pesan);
			return OK;
		}
		if (isZkEnvironment()) {
			return tampilModern(pesan, Common.getBahasaConfig(titleCode), buttons, icon, eventListener);
		} else {
			triggerGlobalJavascriptToast(pesan, icon);
			// Karena pemanggilan JavaScript di luar ZK berjalan secara asynchronous di browser,
			// kita tidak bisa mem-pause thread Java untuk menunggu input user (seperti klik OK/Cancel).
			// Oleh karena itu, kita kembalikan nilai default OK.
			return OK;
		}
	}

	/**
	 * <h3>Tampilkan pesan MULTI-BAHASA berparameter (judul + tombol + ikon)</h3>
	 * Terjemahkan {@code template} (via kamus DB) lalu substitusi {@code {V1}},{@code {V2}},... dengan
	 * {@code args}, sehingga nilai dinamis tidak membanjiri kamus terjemahan. Judul juga diterjemahkan.
	 * <p>Contoh:
	 * {@code MyMessageboxConfig.showFormat("Perkuliahan \"{V1}\" berhasil disimpan.", "Informasi", OK, INFORMATION, nama)}.
	 *
	 * @param template teks template berisi {@code {V1}},{@code {V2}},...
	 * @param titleCode judul (akan diterjemahkan)
	 * @param buttons tombol Messagebox (mis. {@link #OK})
	 * @param icon ikon Messagebox (mis. {@link #INFORMATION})
	 * @param args nilai pengganti berurutan untuk {@code {V1}},{@code {V2}},...
	 */
	public static int showFormat(String template, String titleCode, Integer buttons, String icon, Object... args)
			throws InterruptedException {
		String pesan = format(template, args);
		if (HeadlessActionContext.isActive()) {
			HeadlessActionContext.record(pesan);
			return OK;
		}
		if (isZkEnvironment()) {
			return tampilModern(pesan, Common.getBahasaConfig(titleCode), buttons, icon, null);
		} else {
			triggerGlobalJavascriptToast(pesan, icon);
			return OK;
		}
	}

	/**
	 * Varian {@link #showFormat} dengan {@link EventListener} (menangani klik tombol). NAMA dibedakan
	 * ({@code showFormatCb}) agar tidak ambigu dengan {@code showFormat(...Object...)} saat argumen listener
	 * ikut diteruskan.
	 */
	public static int showFormatCb(String template, String titleCode, int buttons, String icon,
			EventListener eventListener, Object... args) throws InterruptedException {
		String pesan = format(template, args);
		if (HeadlessActionContext.isActive()) {
			HeadlessActionContext.record(pesan);
			return OK;
		}
		if (isZkEnvironment()) {
			return tampilModern(pesan, Common.getBahasaConfig(titleCode), buttons, icon, eventListener);
		} else {
			triggerGlobalJavascriptToast(pesan, icon);
			return OK;
		}
	}

}
