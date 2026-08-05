package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;
import ais.database.model.Konfigurasi;

/**
 * Servlet AI Generator berbasis OpenAI-compatible API.
 *
 * Default provider/model:
 *   GEMINI / gemini-1.5-flash
 *
 * Default endpoint menggunakan Google Gemini OpenAI-compatible:
 *   https://generativelanguage.googleapis.com/v1beta/openai/chat/completions
 *
 * Jika ingin memakai Ollama lokal/proxy, ubah konfigurasi:
 *   AI_PROVIDER_AKTIF = OLLAMA_LOCAL atau OLLAMA_PROXY
 *
 * Konfigurasi opsional di tabel konfigurasi:
 * - AI_OPENAI_URL       : full URL endpoint chat completions,
 *                         contoh http://192.168.88.128:11434/v1/chat/completions
 * - AI_OPENAI_BASE_URL  : base URL, contoh http://192.168.88.128:11434 atau http://38.47.178.42:9002
 * - AI_OPENAI_MODEL     : model default, contoh qwen2.5:7b
 * - AI_OPENAI_KEY       : optional, kosong untuk Ollama
 * - AI_TIMEOUT_MS       : timeout koneksi/read dalam milidetik
 * - AI_NUM_PREDICT      : batas token output Ollama/OpenAI-compatible, default 700
 *
 * Debug:
 * - boolean debug default true sesuai permintaan.
 * - Jika debug == true, servlet akan menampilkan log detail setiap proses ke stdout/log server.
 */
@SuppressWarnings("serial")
public class AiGenerateServlet extends HttpServlet {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai";
	private static final String DEFAULT_MODEL = "gemini-1.5-flash";
	private static final String DEFAULT_TIMEOUT_MS = "240000";
	private static final String DEFAULT_NUM_PREDICT = "700";

	// Default true sesuai permintaan user.
	private static boolean debug = true;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		refreshDebugConfig();
		long start = System.currentTimeMillis();
		debugLog("doGet() mulai. remote=" + request.getRemoteAddr() + ", uri=" + request.getRequestURI());

		response.setContentType("application/json");

		String model = getActiveAiModel();
		String endpoint = getOpenAiEndpoint();
		int timeoutMs = getTimeoutMs();

		debugLog("doGet() status servlet. model=" + model + ", endpoint=" + endpoint + ", timeoutMs=" + timeoutMs + ", debug=" + debug);

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"success\":true,");
		sb.append("\"message\":\"AiGenerateServlet aktif\",");
		sb.append("\"provider\":\"openai-compatible\",");
		sb.append("\"model\":\"").append(escapeJson(model)).append("\",");
		sb.append("\"endpoint\":\"").append(escapeJson(endpoint)).append("\",");
		sb.append("\"debug\":").append(debug ? "true" : "false").append(",");
		sb.append("\"timeoutMs\":").append(timeoutMs);
		sb.append("}");

		response.getWriter().write(sb.toString());
		debugLog("doGet() selesai dalam " + (System.currentTimeMillis() - start) + " ms.");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		refreshDebugConfig();
		long start = System.currentTimeMillis();
		debugLog("============================================================");
		debugLog("doPost() mulai. waktu=" + new Date() + ", remote=" + request.getRemoteAddr() + ", uri=" + request.getRequestURI());

		request.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");

		String body = "";
		String prompt = "";
		String model = "";
		String endpoint = "";
		HttpURLConnection conn = null;
		String rawAiResponse = "";
		int statusCode = 0;

		try {
			debugLog("Step 1 - Membaca request body dari browser/text_area.jsp...");
			body = readRequestBody(request);
			debugLog("Step 1 selesai. bodyLength=" + lengthOf(body) + ", bodyPreview=" + shortText(body, 500));

			debugLog("Step 2 - Mengambil instruksi/prompt dari JSON/form parameter...");
			prompt = firstNotEmpty(
					extractJsonValue(body, "instruksi"),
					extractJsonValue(body, "prompt"),
					extractJsonValue(body, "text"),
					request.getParameter("instruksi"),
					request.getParameter("prompt"),
					request.getParameter("text"));

			if (isBlank(prompt)) {
				prompt = "Buatkan teks akademik yang rapi, formal, mudah dipahami, dan siap digunakan.";
				debugLog("Step 2 - Prompt kosong, memakai prompt default.");
			}
			debugLog("Step 2 selesai. promptLength=" + lengthOf(prompt) + ", promptPreview=" + shortText(prompt, 700));

			debugLog("Step 3 - Menentukan model AI...");
			String requestedModel = firstNotEmpty(extractJsonValue(body, "model"), request.getParameter("model"));
			model = isBlank(requestedModel) ? getActiveAiModel() : requestedModel.trim();
			if (isBlank(model)) {
				model = DEFAULT_MODEL;
			}
			debugLog("Step 3 selesai. requestedModel=" + requestedModel + ", finalModel=" + model);

			debugLog("Step 4 - Membaca parameter temperature dan konfigurasi endpoint...");
			String temperatureValue = firstNotEmpty(extractJsonValue(body, "temperature"), request.getParameter("temperature"));
			double temperature = parseDouble(temperatureValue, parseDouble(getConfigValue("AI_TEMPERATURE_DEFAULT", "0.4"), 0.4D));
			endpoint = getOpenAiEndpoint();
			String apiKey = getActiveAiKey();
			int timeoutMs = getTimeoutMs();
			int numPredict = getNumPredict();
			// Override maxTokens per-request (mis. Buat Soal via AI perlu output panjang agar
			// JSON tidak terpotong). Backward-compatible: bila tidak dikirim, pakai config.
			String maxTokensValue = firstNotEmpty(
					extractJsonValue(body, "maxTokens"),
					extractJsonValue(body, "max_tokens"),
					request.getParameter("maxTokens"),
					request.getParameter("max_tokens"),
					null, null);
			if (!isBlank(maxTokensValue)) {
				int minta = (int) parseDouble(maxTokensValue, numPredict);
				if (minta >= 50 && minta <= 8192) {
					numPredict = minta;
					debugLog("Step 4 - maxTokens override diterima=" + numPredict);
				}
			}
			debugLog("Step 4 selesai. temperature=" + temperature + ", endpoint=" + endpoint + ", timeoutMs=" + timeoutMs
					+ ", numPredict=" + numPredict + ", apiKeyAda=" + (!isBlank(apiKey)));

			debugLog("Step 5 - Membuat payload OpenAI-compatible...");
			String payload = buildOpenAiPayload(model, prompt, temperature, numPredict);
			debugLog("Step 5 selesai. payloadLength=" + lengthOf(payload) + ", payloadPreview=" + shortText(payload, 1000));

			debugLog("Step 6 - Membuka koneksi HTTP ke endpoint AI...");
			URL url = new URL(endpoint);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(timeoutMs);
			conn.setReadTimeout(timeoutMs);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setRequestProperty("Accept", "application/json");

			if (!isBlank(apiKey)) {
				conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
			}
			debugLog("Step 6 selesai. Koneksi disiapkan.");

			debugLog("Step 7 - Mengirim payload ke endpoint AI...");
			OutputStream os = null;
			try {
				os = conn.getOutputStream();
				byte[] input = payload.getBytes(UTF_8);
				os.write(input, 0, input.length);
				os.flush();
				debugLog("Step 7 selesai. byteTerkirim=" + input.length);
			} finally {
				closeQuietly(os);
			}

			debugLog("Step 8 - Membaca HTTP status dari endpoint AI...");
			statusCode = conn.getResponseCode();
			debugLog("Step 8 selesai. statusCode=" + statusCode + ", responseMessage=" + safe(conn.getResponseMessage()));

			debugLog("Step 9 - Membaca response body dari endpoint AI...");
			InputStream inputStream = statusCode >= 200 && statusCode < 300 ? conn.getInputStream() : conn.getErrorStream();
			rawAiResponse = readStream(inputStream);
			debugLog("Step 9 selesai. rawResponseLength=" + lengthOf(rawAiResponse) + ", rawResponsePreview=" + shortText(rawAiResponse, 1500));

			if (statusCode < 200 || statusCode >= 300) {
				debugLog("Step 10 - Endpoint AI mengembalikan status error. statusCode=" + statusCode);
				writeError(response, "HTTP " + statusCode + " dari endpoint AI.", rawAiResponse, endpoint, model);
				return;
			}

			debugLog("Step 10 - Parsing response AI...");
			String resultText = parseOpenAiContent(rawAiResponse);
			debugLog("Step 10 selesai. resultLength=" + lengthOf(resultText) + ", resultPreview=" + shortText(resultText, 1200));

			if (isBlank(resultText)) {
				debugLog("Step 11 - Response AI kosong atau format tidak dikenali.");
				writeError(response, "Response AI kosong atau format OpenAI-compatible tidak dikenali.", rawAiResponse, endpoint, model);
				return;
			}

			debugLog("Step 11 - Menulis response sukses ke browser...");
			StringBuilder out = new StringBuilder();
			out.append("{");
			out.append("\"success\":true,");
			out.append("\"provider\":\"openai-compatible\",");
			out.append("\"model\":\"").append(escapeJson(model)).append("\",");
			out.append("\"text\":\"").append(escapeJson(resultText)).append("\",");
			out.append("\"response\":\"").append(escapeJson(resultText)).append("\"");
			out.append("}");

			response.getWriter().write(out.toString());
			debugLog("Step 11 selesai. Response sukses ditulis. totalMs=" + (System.currentTimeMillis() - start));

		} catch (Exception e) {
			debugException("ERROR doPost() gagal. endpoint=" + endpoint + ", model=" + model + ", statusCode=" + statusCode
					+ ", rawPreview=" + shortText(rawAiResponse, 1000), e);
			writeError(response, "Gagal menghubungi AI lokal/OpenAI-compatible endpoint: " + e.getMessage(), rawAiResponse, endpoint, model);
		} finally {
			if (conn != null) {
				try {
					conn.disconnect();
					debugLog("Step akhir - Koneksi HTTP diputus.");
				} catch (Exception e) {
					debugException("Gagal disconnect koneksi HTTP.", e);
				}
			}
			debugLog("doPost() selesai. totalMs=" + (System.currentTimeMillis() - start));
			debugLog("============================================================");
		}
	}

	/**
	 * Panggilan AI server-side (non-streaming) untuk dipakai komponen non-servlet, mis. service
	 * JSP OBE "Generate via AI". Mengembalikan teks hasil AI, atau melempar Exception bila gagal.
	 * Memakai provider/model/endpoint yang sama dengan doPost.
	 */
	public static String generateText(String prompt, int maxTokens) throws Exception {
		AiGenerateServlet inst = new AiGenerateServlet();
		return inst.callAiInternal(prompt, maxTokens);
	}

	private String callAiInternal(String prompt, int maxTokens) throws Exception {
		refreshDebugConfig();
		if (prompt == null || prompt.trim().length() == 0) {
			throw new Exception("Prompt kosong.");
		}
		String model = getActiveAiModel();
		if (isBlank(model)) {
			model = DEFAULT_MODEL;
		}
		double temperature = parseDouble(getConfigValue("AI_TEMPERATURE_DEFAULT", "0.4"), 0.4D);
		String endpoint = getOpenAiEndpoint();
		String apiKey = getActiveAiKey();
		int timeoutMs = getTimeoutMs();
		int numPredict = maxTokens;
		if (numPredict < 50) {
			numPredict = 50;
		}
		if (numPredict > 8192) {
			numPredict = 8192;
		}
		String payload = buildOpenAiPayload(model, prompt, temperature, numPredict);
		HttpURLConnection conn = null;
		try {
			URL url = new URL(endpoint);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(timeoutMs);
			conn.setReadTimeout(timeoutMs);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setRequestProperty("Accept", "application/json");
			if (!isBlank(apiKey)) {
				conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
			}
			OutputStream os = null;
			try {
				os = conn.getOutputStream();
				byte[] in = payload.getBytes(UTF_8);
				os.write(in, 0, in.length);
				os.flush();
			} finally {
				closeQuietly(os);
			}
			int statusCode = conn.getResponseCode();
			InputStream is = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();
			String raw = readStream(is);
			if (statusCode < 200 || statusCode >= 300) {
				throw new Exception("HTTP " + statusCode + " dari endpoint AI: " + shortText(raw, 300));
			}
			String text = parseOpenAiContent(raw);
			if (isBlank(text)) {
				throw new Exception("Response AI kosong atau format OpenAI-compatible tidak dikenali.");
			}
			return text;
		} finally {
			if (conn != null) {
				try { conn.disconnect(); } catch (Exception e) {}
			}
		}
	}

	private String buildOpenAiPayload(String model, String prompt, double temperature, int numPredict) {
		String systemPrompt = getConfigValue("AI_SYSTEM_PROMPT",
				"Anda adalah asisten akademik dan penulisan profesional untuk sekolah dan perguruan tinggi di Indonesia. "
						+ "Gunakan Bahasa Indonesia formal, jelas, rapi, dan siap ditempel ke editor. "
						+ "Untuk kebutuhan akademik, gunakan istilah yang sesuai seperti RPS, CPL, CPMK, Sub-CPMK, CP, TP, ATP, asesmen, rubrik, materi, dan evaluasi jika relevan. "
						+ "Jangan gunakan HTML kecuali diminta secara eksplisit.");

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"model\":\"").append(escapeJson(model)).append("\",");
		sb.append("\"stream\":false,");
		sb.append("\"temperature\":").append(formatDouble(temperature)).append(",");
		sb.append("\"max_tokens\":").append(numPredict).append(",");
		sb.append("\"messages\":[");
		sb.append("{\"role\":\"system\",\"content\":\"").append(escapeJson(systemPrompt)).append("\"},");
		sb.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(prompt)).append("\"}");
		sb.append("]");
		sb.append("}");
		return sb.toString();
	}

	private String parseOpenAiContent(String json) {
		debugLog("parseOpenAiContent() mulai. jsonLength=" + lengthOf(json));
		if (json == null) {
			return "";
		}

		// Format OpenAI/Ollama OpenAI-compatible:
		// {"choices":[{"message":{"role":"assistant","content":"..."}}]}
		String content = extractJsonValue(json, "content");
		if (!isBlank(content)) {
			debugLog("parseOpenAiContent() memakai field content. length=" + lengthOf(content));
			return content;
		}

		// Fallback untuk response Ollama /api/generate jika endpoint diarahkan ke sana.
		String response = extractJsonValue(json, "response");
		if (!isBlank(response)) {
			debugLog("parseOpenAiContent() memakai field response. length=" + lengthOf(response));
			return response;
		}

		String text = extractJsonValue(json, "text");
		if (!isBlank(text)) {
			debugLog("parseOpenAiContent() memakai field text. length=" + lengthOf(text));
			return text;
		}

		debugLog("parseOpenAiContent() tidak menemukan field content/response/text.");
		return "";
	}

	private String getOpenAiEndpoint() {
		debugLog("getOpenAiEndpoint() mulai.");
		String fullUrl = getConfigValue("AI_OPENAI_URL", "");
		if (!isBlank(fullUrl)) {
			debugLog("getOpenAiEndpoint() memakai AI_OPENAI_URL=" + fullUrl.trim());
			return fullUrl.trim();
		}

		String provider = getActiveAiProvider();
		String baseUrl = getActiveAiBaseUrl(provider);
		String endpoint = buildChatCompletionsEndpoint(baseUrl);
		debugLog("getOpenAiEndpoint() provider=" + provider + ", baseUrl=" + baseUrl + ", endpoint=" + endpoint);
		return endpoint;
	}

	private String getActiveAiProvider() {
		String provider = getConfigValue("AI_PROVIDER_AKTIF", "GEMINI");
		if (isBlank(provider)) {
			provider = "GEMINI";
		}
		return provider.trim().toUpperCase();
	}

	private String getActiveAiBaseUrl(String provider) {
		if ("OLLAMA_PROXY".equals(provider)) {
			return getConfigValue("AI_OLLAMA_PROXY_BASE_URL", "http://38.47.178.42:9002");
		}
		if ("GEMINI".equals(provider)) {
			return getConfigValue("AI_GEMINI_BASE_URL", "https://generativelanguage.googleapis.com/v1beta/openai");
		}
		if ("GROQ".equals(provider)) {
			return getConfigValue("AI_GROQ_BASE_URL", "https://api.groq.com/openai/v1");
		}
		if ("CLOUDFLARE".equals(provider)) {
			return getConfigValue("AI_CLOUDFLARE_BASE_URL", "https://api.cloudflare.com/client/v4/accounts/ACCOUNT_ID/ai/v1");
		}
		if ("OPENAI".equals(provider)) {
			return getConfigValue("AI_OPENAI_CLOUD_BASE_URL", "https://api.openai.com/v1");
		}
		if ("DEEPSEEK".equals(provider)) {
			return getConfigValue("AI_DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1");
		}

		String baseUrl = getConfigValue("AI_OLLAMA_LOCAL_BASE_URL", "");
		if (!isBlank(baseUrl)) {
			return baseUrl;
		}

		return getConfigValue("AI_OPENAI_BASE_URL", DEFAULT_BASE_URL);
	}

	private String getActiveAiModel() {
		String provider = getActiveAiProvider();
		if ("OLLAMA_PROXY".equals(provider) || "OLLAMA_LOCAL".equals(provider)) {
			return getConfigValue("AI_OLLAMA_AKADEMIK_MODEL", getConfigValue("AI_OPENAI_MODEL", DEFAULT_MODEL));
		}
		if ("GEMINI".equals(provider)) {
			return getConfigValue("AI_GEMINI_MODEL", "gemini-1.5-flash");
		}
		if ("GROQ".equals(provider)) {
			return getConfigValue("AI_GROQ_MODEL", "llama-3.1-8b-instant");
		}
		if ("CLOUDFLARE".equals(provider)) {
			return getConfigValue("AI_CLOUDFLARE_MODEL", "@cf/meta/llama-3.1-8b-instruct");
		}
		if ("OPENAI".equals(provider)) {
			return getConfigValue("AI_OPENAI_CLOUD_MODEL", "gpt-4o-mini");
		}
		if ("DEEPSEEK".equals(provider)) {
			return getConfigValue("AI_DEEPSEEK_MODEL", "deepseek-chat");
		}
		return getConfigValue("AI_OPENAI_MODEL", DEFAULT_MODEL);
	}

	private String getActiveAiKey() {
		String provider = getActiveAiProvider();
		if ("GEMINI".equals(provider)) {
			return getConfigValue("AI_GEMINI_KEY", "AIzaSyAFSzVMA8o9DWZpHCsTQT8Mf4M5SN77e2E");
		}
		if ("GROQ".equals(provider)) {
			return getConfigValue("AI_GROQ_KEY", "");
		}
		if ("CLOUDFLARE".equals(provider)) {
			return getConfigValue("AI_CLOUDFLARE_KEY", "");
		}
		if ("OPENAI".equals(provider)) {
			return getConfigValue("AI_OPENAI_CLOUD_KEY", "");
		}
		if ("DEEPSEEK".equals(provider)) {
			return getConfigValue("AI_DEEPSEEK_KEY", "");
		}
		return getConfigValue("AI_OPENAI_KEY", "");
	}

	private String buildChatCompletionsEndpoint(String baseUrl) {
		if (isBlank(baseUrl)) {
			baseUrl = DEFAULT_BASE_URL;
		}

		baseUrl = baseUrl.trim();
		while (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}

		if (baseUrl.endsWith("/chat/completions") || baseUrl.endsWith("/v1/chat/completions")) {
			return baseUrl;
		}

		if (baseUrl.endsWith("/v1") || baseUrl.endsWith("/openai") || baseUrl.endsWith("/ai/v1")) {
			return baseUrl + "/chat/completions";
		}

		return baseUrl + "/v1/chat/completions";
	}

	private void refreshDebugConfig() {
		try {
			Konfigurasi konfigurasi = Common.getKonfigurasi("AI_DEBUG", Konfigurasi.AKTIF);
			if (konfigurasi != null && konfigurasi.getNilai() != null) {
				debug = konfigurasi.getNilai().trim().equals(Konfigurasi.AKTIF);
			}
		} catch (Exception e) {
			// Default tetap true agar mudah tracing saat konfigurasi belum tersedia.
			debug = true;
		}
	}

	private int getTimeoutMs() {
		int timeout = (int) parseDouble(getConfigValue("AI_TIMEOUT_MS", DEFAULT_TIMEOUT_MS), 240000D);
		debugLog("getTimeoutMs()=" + timeout);
		return timeout;
	}

	private int getNumPredict() {
		int numPredict = (int) parseDouble(getConfigValue("AI_NUM_PREDICT", DEFAULT_NUM_PREDICT), 700D);
		if (numPredict < 50) {
			numPredict = 50;
		}
		debugLog("getNumPredict()=" + numPredict);
		return numPredict;
	}

	private String getConfigValue(String key, String defaultValue) {
		try {
			debugLog("getConfigValue() membaca konfigurasi key=" + key + ", default=" + defaultValue);
			if (Common.getKonfigurasi(key, defaultValue) != null && Common.getKonfigurasi(key, defaultValue).getNilai() != null) {
				String nilai = Common.getKonfigurasi(key, defaultValue).getNilai();
				if (!isBlank(nilai)) {
					debugLog("getConfigValue() key=" + key + " ditemukan, value=" + maskIfSecret(key, nilai));
					return nilai;
				}
			}
		} catch (Exception e) {
			debugException("getConfigValue() gagal membaca key=" + key + ", gunakan default.", e);
		}
		debugLog("getConfigValue() key=" + key + " memakai default=" + defaultValue);
		return defaultValue;
	}

	private String readRequestBody(HttpServletRequest request) throws IOException {
		debugLog("readRequestBody() mulai.");
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = request.getReader();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
					debugException("readRequestBody() gagal close reader.", e);
				}
			}
		}
		debugLog("readRequestBody() selesai. length=" + sb.length());
		return sb.toString();
	}

	private String readStream(InputStream inputStream) throws IOException {
		debugLog("readStream() mulai. inputStreamNull=" + (inputStream == null));
		if (inputStream == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					debugException("readStream() gagal close reader.", e);
				}
			}
		}
		debugLog("readStream() selesai. length=" + sb.length());
		return sb.toString();
	}

	private void writeError(HttpServletResponse response, String message, String rawResponse, String endpoint, String model) throws IOException {
		debugLog("writeError() message=" + message + ", endpoint=" + endpoint + ", model=" + model
				+ ", rawLength=" + lengthOf(rawResponse) + ", rawPreview=" + shortText(rawResponse, 1000));

		StringBuilder out = new StringBuilder();
		out.append("{");
		out.append("\"success\":false,");
		out.append("\"provider\":\"openai-compatible\",");
		out.append("\"model\":\"").append(escapeJson(model)).append("\",");
		out.append("\"endpoint\":\"").append(escapeJson(endpoint)).append("\",");
		out.append("\"error\":\"").append(escapeJson(message)).append("\",");
		out.append("\"raw\":\"").append(escapeJson(rawResponse == null ? "" : rawResponse)).append("\"");
		out.append("}");
		response.getWriter().write(out.toString());
	}

	private String firstNotEmpty(String a, String b, String c, String d, String e, String f) {
		if (!isBlank(a)) return a;
		if (!isBlank(b)) return b;
		if (!isBlank(c)) return c;
		if (!isBlank(d)) return d;
		if (!isBlank(e)) return e;
		if (!isBlank(f)) return f;
		return "";
	}

	private String firstNotEmpty(String a, String b) {
		if (!isBlank(a)) return a;
		if (!isBlank(b)) return b;
		return "";
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private double parseDouble(String value, double defaultValue) {
		if (isBlank(value)) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(value.trim());
		} catch (Exception e) {
			debugException("parseDouble() gagal parse value=" + value + ", default=" + defaultValue, e);
			return defaultValue;
		}
	}

	private String formatDouble(double value) {
		String s = String.valueOf(value);
		if (s.indexOf('E') >= 0 || s.indexOf('e') >= 0) {
			return "0.4";
		}
		return s;
	}

	private String extractJsonValue(String json, String key) {
		if (json == null || key == null) {
			return "";
		}

		String pattern = "\"" + key + "\"";
		int keyIndex = json.indexOf(pattern);
		if (keyIndex < 0) {
			return "";
		}

		int colonIndex = json.indexOf(':', keyIndex + pattern.length());
		if (colonIndex < 0) {
			return "";
		}

		int i = colonIndex + 1;
		while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
			i++;
		}

		if (i >= json.length()) {
			return "";
		}

		char first = json.charAt(i);
		if (first == '"') {
			return parseJsonString(json, i);
		}

		int start = i;
		while (i < json.length()) {
			char ch = json.charAt(i);
			if (ch == ',' || ch == '}' || ch == ']') {
				break;
			}
			i++;
		}

		return json.substring(start, i).trim();
	}

	private String parseJsonString(String json, int quoteIndex) {
		StringBuilder sb = new StringBuilder();
		boolean escaping = false;

		for (int i = quoteIndex + 1; i < json.length(); i++) {
			char ch = json.charAt(i);

			if (escaping) {
				if (ch == '"' || ch == '\\' || ch == '/') {
					sb.append(ch);
				} else if (ch == 'b') {
					sb.append('\b');
				} else if (ch == 'f') {
					sb.append('\f');
				} else if (ch == 'n') {
					sb.append('\n');
				} else if (ch == 'r') {
					sb.append('\r');
				} else if (ch == 't') {
					sb.append('\t');
				} else if (ch == 'u' && i + 4 < json.length()) {
					String hex = json.substring(i + 1, i + 5);
					try {
						sb.append((char) Integer.parseInt(hex, 16));
						i += 4;
					} catch (Exception e) {
						sb.append("\\u").append(hex);
						i += 4;
					}
				} else {
					sb.append(ch);
				}
				escaping = false;
			} else if (ch == '\\') {
				escaping = true;
			} else if (ch == '"') {
				return sb.toString();
			} else {
				sb.append(ch);
			}
		}

		return sb.toString();
	}

	private String escapeJson(String value) {
		if (value == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch == '"') {
				sb.append("\\\"");
			} else if (ch == '\\') {
				sb.append("\\\\");
			} else if (ch == '\b') {
				sb.append("\\b");
			} else if (ch == '\f') {
				sb.append("\\f");
			} else if (ch == '\n') {
				sb.append("\\n");
			} else if (ch == '\r') {
				sb.append("\\r");
			} else if (ch == '\t') {
				sb.append("\\t");
			} else if (ch < 32) {
				String hex = Integer.toHexString(ch);
				sb.append("\\u");
				for (int j = hex.length(); j < 4; j++) {
					sb.append('0');
				}
				sb.append(hex);
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	private int lengthOf(String value) {
		return value == null ? 0 : value.length();
	}

	private String shortText(String value, int max) {
		if (value == null) {
			return "";
		}
		String cleaned = value.replace('\r', ' ').replace('\n', ' ');
		if (cleaned.length() <= max) {
			return cleaned;
		}
		return cleaned.substring(0, max) + "...";
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}

	private String maskIfSecret(String key, String value) {
		if (key != null && key.toLowerCase().indexOf("key") >= 0) {
			return isBlank(value) ? "" : "***MASKED***";
		}
		return value;
	}

	private void closeQuietly(OutputStream os) {
		if (os != null) {
			try {
				os.close();
			} catch (Exception e) {
				debugException("closeQuietly(OutputStream) gagal.", e);
			}
		}
	}

	private void debugLog(String message) {
		if (debug) {
			System.out.println("[AiGenerateServlet] " + message);
		}
	}

	private void debugException(String message, Exception e) {
		if (debug) {
			System.out.println("[AiGenerateServlet] " + message);
			if (e != null) {
				e.printStackTrace(System.out);
			}
		}
	}
}
