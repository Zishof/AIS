package ais.ui.util;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Messagebox;

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

	public static void show(String messageCode) throws InterruptedException {
		String pesan = terjemah(messageCode);
		if (HeadlessActionContext.isActive()) {
			HeadlessActionContext.record(pesan);
			return;
		}
		if (isZkEnvironment()) {
			Messagebox.show(pesan, Common.getBahasaConfig("Informasi"), Messagebox.OK,
					Messagebox.INFORMATION);
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
			return Messagebox.show(pesan, Common.getBahasaConfig(titleCode), buttons, icon);
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
			return Messagebox.show(pesan, Common.getBahasaConfig(titleCode), buttons, icon, eventListener);
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
			return Messagebox.show(pesan, Common.getBahasaConfig(titleCode), buttons, icon);
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
			return Messagebox.show(pesan, Common.getBahasaConfig(titleCode), buttons, icon, eventListener);
		} else {
			triggerGlobalJavascriptToast(pesan, icon);
			return OK;
		}
	}

}
