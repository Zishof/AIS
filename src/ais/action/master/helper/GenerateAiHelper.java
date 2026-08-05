package ais.action.master.helper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Label;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>Helper Generate AI STREAMING (dipakai bersama beberapa Action)</h2>
 *
 * <p>Memanggil server AI (OpenAI-compatible) yang DIPAKSA ke {@code ai_rpsobe_base_url} +
 * {@code ai_rpsobe_model} (default Ollama 38.47.182.162 + qwen2.5:1.5b), menampilkan popup progres yang
 * menampilkan teks saat AI mengetik, lalu memanggil {@link HasilAi#selesai(String)} di thread event ZK.</p>
 *
 * <p>Parser tahan-banting (lewati baris non-JSON/kosong/done), dan otomatis coba-ulang saat error SEMENTARA
 * (server sibuk/antre/model dimuat).</p>
 */
public class GenerateAiHelper {

	/** Callback saat streaming AI selesai (dipanggil di thread event ZK). */
	public interface HasilAi {
		void selesai(String hasil) throws Exception;
	}

	private GenerateAiHelper() {
	}

	/**
	 * Cek cepat apakah server AI (Ollama) BISA dihubungi. Dipakai untuk memutuskan pakai AI atau
	 * fallback (mis. kamus internal). Timeout pendek ({@code ollama_health_timeout_ms}, default 1500ms).
	 */
	public static boolean siap() {
		HttpURLConnection c = null;
		try {
			String baseUrl = Common.getKonfigurasi("ai_rpsobe_base_url", "http://38.47.182.162:11434/v1").getNilai()
					.trim();
			String host = baseUrl;
			int vi = host.indexOf("/v1");
			if (vi > 0) {
				host = host.substring(0, vi);
			}
			while (host.endsWith("/")) {
				host = host.substring(0, host.length() - 1);
			}
			int timeout = 1500;
			try {
				timeout = Integer.parseInt(Common.getKonfigurasi("ollama_health_timeout_ms", "1500").getNilai().trim());
			} catch (Exception e) {
			}
			URL url = new URL(host + "/api/version");
			c = (HttpURLConnection) url.openConnection();
			c.setRequestMethod("GET");
			c.setConnectTimeout(timeout);
			c.setReadTimeout(timeout);
			int code = c.getResponseCode();
			return code >= 200 && code < 500;
		} catch (Exception e) {
			return false;
		} finally {
			if (c != null) {
				try {
					c.disconnect();
				} catch (Exception ig) {
				}
			}
		}
	}

	public static String panggilAi(String prompt) {
		return panggilAi(prompt, 1, null, 900);
	}

	/** Varian STREAMING: token disalurkan ke {@code sink} saat tiba. */
	public static String panggilAi(String prompt, StringBuffer sink) {
		return panggilAi(prompt, 1, sink, 900);
	}

	/** Varian STREAMING dengan batas token keluaran tersendiri (mis. terjemahan HTML panjang). */
	public static String panggilAi(String prompt, StringBuffer sink, int maxTokens) {
		return panggilAi(prompt, 1, sink, maxTokens);
	}

	private static String panggilAi(String prompt, int attempt, StringBuffer sink, int maxTokens) {
		HttpURLConnection conn = null;
		try {
			String baseUrl = Common.getKonfigurasi("ai_rpsobe_base_url", "http://38.47.182.162:11434/v1").getNilai()
					.trim();
			String model = Common.getKonfigurasi("ai_rpsobe_model", "qwen2.5:1.5b-instruct-q4_K_M").getNilai().trim();
			int timeoutMs = 120000;
			try {
				timeoutMs = Integer.parseInt(Common.getKonfigurasi("ollama_timeout_baca_ms", "120000").getNilai().trim());
			} catch (Exception e) {
			}
			while (baseUrl.endsWith("/")) {
				baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
			}
			String endpoint = baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";

			JSONObject payload = new JSONObject();
			payload.put("model", model);
			payload.put("stream", sink != null);
			payload.put("temperature", 0.4);
			payload.put("max_tokens", maxTokens);
			JSONArray messages = new JSONArray();
			JSONObject sys = new JSONObject();
			sys.put("role", "system");
			sys.put("content", "Anda asisten akademik untuk perguruan tinggi di Indonesia. "
					+ "Jawab dalam Bahasa Indonesia formal-akademis, ringkas, dan patuhi format yang diminta.");
			messages.put(sys);
			JSONObject usr = new JSONObject();
			usr.put("role", "user");
			usr.put("content", prompt);
			messages.put(usr);
			payload.put("messages", messages);

			URL url = new URL(endpoint);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(timeoutMs);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setRequestProperty("Accept", sink != null ? "text/event-stream" : "application/json");

			byte[] body = payload.toString().getBytes("UTF-8");
			OutputStream os = conn.getOutputStream();
			try {
				os.write(body);
				os.flush();
			} finally {
				try {
					os.close();
				} catch (Exception ig) {
				}
			}

			int status = conn.getResponseCode();
			InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			if (sink != null) {
				StringBuilder full = new StringBuilder();
				String errText = null;
				String line;
				while ((line = br.readLine()) != null) {
					String t = line.trim();
					if (t.length() == 0) {
						continue;
					}
					if (t.startsWith("data:")) {
						t = t.substring(5).trim();
					}
					if ("[DONE]".equals(t)) {
						break;
					}
					if (!t.startsWith("{")) {
						errText = t;
						continue;
					}
					try {
						JSONObject o = new JSONObject(t);
						if (o.has("error") && !o.isNull("error")) {
							errText = String.valueOf(o.opt("error"));
							continue;
						}
						JSONArray ch = o.optJSONArray("choices");
						if (ch != null && ch.length() > 0) {
							JSONObject c0 = ch.getJSONObject(0);
							String piece = "";
							JSONObject delta = c0.optJSONObject("delta");
							if (delta != null) {
								piece = delta.optString("content", "");
							}
							if (piece.length() == 0 && c0.has("message")) {
								piece = c0.getJSONObject("message").optString("content", "");
							}
							if (piece.length() > 0) {
								full.append(piece);
								sink.append(piece);
							}
						}
					} catch (Exception exLine) {
					}
				}
				try {
					br.close();
				} catch (Exception ig) {
				}
				String res = full.toString();
				if (res.trim().length() == 0 && errText != null) {
					ais.common.ErrorAuditUtil.record(new Exception("AI stream error: " + errText),
							"GenerateAiHelper.stream");
					String low = errText.toLowerCase();
					boolean sementara = low.contains("busy") || low.contains("pending") || low.contains("try again")
							|| low.contains("loading") || low.contains("timeout") || low.contains("unavailable");
					if (sementara && attempt < 3) {
						try {
							Thread.sleep(1500L * attempt);
						} catch (Exception ie) {
						}
						return panggilAi(prompt, attempt + 1, sink, maxTokens);
					}
				}
				return res;
			}

			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			try {
				br.close();
			} catch (Exception ig) {
			}
			String raw = sb.toString().trim();
			if (raw.length() == 0) {
				return "";
			}
			if (!raw.startsWith("{") && !raw.startsWith("[")) {
				String low = raw.toLowerCase();
				boolean sementara = low.contains("busy") || low.contains("pending") || low.contains("try again")
						|| low.contains("loading") || low.contains("timeout") || low.contains("unavailable");
				if (sementara && attempt < 3) {
					try {
						Thread.sleep(1500L * attempt);
					} catch (Exception ie) {
					}
					return panggilAi(prompt, attempt + 1, sink, maxTokens);
				}
				return "";
			}
			JSONObject resp = new JSONObject(raw);
			if (resp.has("choices")) {
				JSONArray ch = resp.getJSONArray("choices");
				if (ch.length() > 0) {
					JSONObject c0 = ch.getJSONObject(0);
					if (c0.has("message")) {
						JSONObject m = c0.getJSONObject("message");
						if (m.has("content")) {
							return m.getString("content");
						}
					}
					if (c0.has("content")) {
						return c0.getString("content");
					}
				}
			}
			if (resp.has("response")) {
				return resp.getString("response");
			}
			return "";
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "GenerateAiHelper.panggilAi");
			return "";
		} finally {
			if (conn != null) {
				try {
					conn.disconnect();
				} catch (Exception ig) {
				}
			}
		}
	}

	/**
	 * Menjalankan panggilan AI STREAMING dengan popup progres (teks muncul saat AI mengetik), lalu memanggil
	 * {@code cb.selesai(hasil)} di thread event ZK. Jika gagal, tampilkan Messagebox dan callback tidak dipanggil.
	 */
	public static void jalankanAiStreaming(final String judul, final String prompt, final HasilAi cb)
			throws Exception {
		jalankanAiStreaming(judul, prompt, cb, 900);
	}

	public static void jalankanAiStreaming(final String judul, final String prompt, final HasilAi cb,
			final int maxTokens) throws Exception {
		final MyWindow loadingWin = new MyWindow(judul, "none", false);
		loadingWin.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		loadingWin.setWidth("560px");
		loadingWin.setClosable(false);

		Vbox loadingVbox = new Vbox();
		loadingVbox.setAlign("center");
		loadingVbox.setHflex("1");
		loadingVbox.setStyle("padding:20px;text-align:center;");
		loadingVbox.setParent(loadingWin);

		Label loadingLbl = new Label(Common.getBahasaConfig("AI sedang menyusun jawaban..."));
		loadingLbl.setStyle("font-size:13px;color:#1e40af;font-weight:bold;");
		loadingLbl.setParent(loadingVbox);

		Label loadingLbl2 = new Label(Common.getBahasaConfig("Teks di bawah muncul langsung saat AI mengetik."));
		loadingLbl2.setStyle("font-size:11px;color:#64748b;margin-top:6px;");
		loadingLbl2.setParent(loadingVbox);

		final org.zkoss.zul.Textbox streamBox = new org.zkoss.zul.Textbox();
		streamBox.setMultiline(true);
		streamBox.setReadonly(true);
		streamBox.setRows(9);
		streamBox.setHflex("1");
		streamBox.setStyle("width:100%;margin-top:12px;font-family:monospace;font-size:11px;"
				+ "color:#334155;background:#f8fafc;");
		streamBox.setParent(loadingVbox);

		loadingWin.onModal();

		final String[] aiResult = new String[]{ null };
		final String[] aiErrorMsg = new String[]{ "" };
		final StringBuffer aiStream = new StringBuffer();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String r = panggilAi(prompt, aiStream, maxTokens);
					aiResult[0] = (r == null) ? "" : r;
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "GenerateAiHelper.jalankanAiStreaming.thread");
					aiResult[0] = "";
					aiErrorMsg[0] = (e.getMessage() != null) ? e.getMessage() : "Unknown error";
				}
			}
		}).start();

		final org.zkoss.zul.Timer pollingTimer = new org.zkoss.zul.Timer(1200);
		pollingTimer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		pollingTimer.setRepeats(true);
		pollingTimer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event evtTimer) throws Exception {
				try {
					String cur = aiStream.toString();
					if (cur.length() > 0 && !cur.equals(streamBox.getValue())) {
						streamBox.setValue(cur);
						org.zkoss.zk.ui.util.Clients.scrollIntoView(streamBox);
					}
				} catch (Exception ig) {
				}
				if (aiResult[0] == null) {
					return;
				}
				pollingTimer.stop();
				pollingTimer.detach();
				loadingWin.detach();
				final String resp = aiResult[0];
				if (resp == null || resp.trim().length() == 0) {
					MyMessageboxConfig.show(
							Common.getBahasaConfig("AI tidak dapat diakses saat ini. Coba lagi beberapa saat.")
									+ (aiErrorMsg[0].isEmpty() ? "" : "\n" + aiErrorMsg[0]),
							Common.getBahasaConfig("Informasi"), MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				try {
					cb.selesai(resp);
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex, "GenerateAiHelper.jalankanAiStreaming.cb");
				}
			}
		});
		pollingTimer.start();
	}
}
