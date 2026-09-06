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
 * Servlet AI Generator berbasis OpenAI-compatible API &mdash; dipetakan ke <code>/Ai</code>.
 *
 * <p><b>Tujuan.</b> Satu-satunya gerbang pemanggilan model bahasa (LLM) di AIS. Ia menerima sebuah
 * <i>prompt</i>, meneruskannya ke penyedia AI yang sedang aktif memakai format permintaan
 * <i>chat completions</i> ala OpenAI, lalu mengembalikan teks hasilnya sebagai JSON. Dipakai dari
 * dua arah:</p>
 * <ul>
 *   <li><b>Dari peramban</b>, lewat HTTP POST ke <code>/Ai</code> &mdash; misalnya tombol
 *   "Generate via AI" pada {@code WEB-INF/baru/modul/common/text_area.jsp},
 *   {@code elearning/obe/_deskripsi_dan_pustaka.jsp}, dan "Buat Soal via AI" pada
 *   {@code elearning/ujian/daftar_soal.jsp}. Servlet ini sengaja menjadi perantara agar peramban
 *   tidak terbentur CORS saat memanggil endpoint AI secara langsung.</li>
 *   <li><b>Dari sisi server</b>, lewat {@link #generateText(String, int)} &mdash; dipakai berkas
 *   JSP OBE seperti {@code _generate_cpl_ai.jsp}, {@code _generate_cpmk_ai.jsp},
 *   {@code _generate_subcpmk_ai.jsp}, {@code _generate_rincian_ai.jsp}, dan
 *   {@code _generate_catatan_obe_ai.jsp}, serta {@code ObeAiJspHelper}.</li>
 * </ul>
 *
 * <h3>Pemilihan penyedia (provider), model, endpoint, dan kunci</h3>
 *
 * <p>Seluruhnya ditentukan tabel {@code konfigurasi} lewat {@link #getConfigValue(String, String)}.
 * Kuncinya {@code AI_PROVIDER_AKTIF}, dengan <b>nilai bawaan {@code GEMINI}</b>. Nilai yang dikenali
 * beserta pasangan base-URL/model/kuncinya:</p>
 * <table border="1" summary="Peta provider ke konfigurasi">
 *   <tr><th>AI_PROVIDER_AKTIF</th><th>base URL</th><th>model</th><th>kunci API</th></tr>
 *   <tr><td>GEMINI (bawaan)</td><td>AI_GEMINI_BASE_URL</td><td>AI_GEMINI_MODEL</td><td>AI_GEMINI_KEY</td></tr>
 *   <tr><td>GROQ</td><td>AI_GROQ_BASE_URL</td><td>AI_GROQ_MODEL</td><td>AI_GROQ_KEY</td></tr>
 *   <tr><td>CLOUDFLARE</td><td>AI_CLOUDFLARE_BASE_URL</td><td>AI_CLOUDFLARE_MODEL</td><td>AI_CLOUDFLARE_KEY</td></tr>
 *   <tr><td>OPENAI</td><td>AI_OPENAI_CLOUD_BASE_URL</td><td>AI_OPENAI_CLOUD_MODEL</td><td>AI_OPENAI_CLOUD_KEY</td></tr>
 *   <tr><td>DEEPSEEK</td><td>AI_DEEPSEEK_BASE_URL</td><td>AI_DEEPSEEK_MODEL</td><td>AI_DEEPSEEK_KEY</td></tr>
 *   <tr><td>OLLAMA_PROXY</td><td>AI_OLLAMA_PROXY_BASE_URL</td><td>AI_OLLAMA_AKADEMIK_MODEL</td><td>AI_OPENAI_KEY</td></tr>
 *   <tr><td>OLLAMA_LOCAL / lainnya</td><td>AI_OLLAMA_LOCAL_BASE_URL lalu AI_OPENAI_BASE_URL</td><td>AI_OLLAMA_AKADEMIK_MODEL / AI_OPENAI_MODEL</td><td>AI_OPENAI_KEY</td></tr>
 * </table>
 * <p>Kunci {@code AI_OPENAI_URL} berpangkat lebih tinggi dari semuanya: bila terisi, nilainya
 * dipakai apa adanya sebagai URL endpoint penuh dan seluruh pemetaan di atas dilewati (lihat
 * {@link #getOpenAiEndpoint()}). Konfigurasi opsional lain: {@code AI_TIMEOUT_MS} (bawaan
 * 240000&nbsp;ms), {@code AI_NUM_PREDICT} (batas token keluaran, bawaan 700),
 * {@code AI_TEMPERATURE_DEFAULT} (bawaan 0.4), {@code AI_SYSTEM_PROMPT}, dan {@code AI_DEBUG}.</p>
 *
 * <p>Rincian konfigurasi opsional beserta contoh nilainya:</p>
 * <ul>
 *   <li>{@code AI_OPENAI_URL} &mdash; full URL endpoint chat completions, contoh
 *   <code>http://192.168.88.128:11434/v1/chat/completions</code>;</li>
 *   <li>{@code AI_OPENAI_BASE_URL} &mdash; base URL, contoh
 *   <code>http://192.168.88.128:11434</code> atau <code>http://38.47.178.42:9002</code>;</li>
 *   <li>{@code AI_OPENAI_MODEL} &mdash; model default, contoh <code>qwen2.5:7b</code>;</li>
 *   <li>{@code AI_OPENAI_KEY} &mdash; opsional, kosong untuk Ollama;</li>
 *   <li>{@code AI_TIMEOUT_MS} &mdash; timeout koneksi/read dalam milidetik;</li>
 *   <li>{@code AI_NUM_PREDICT} &mdash; batas token output Ollama/OpenAI-compatible, default 700.</li>
 * </ul>
 *
 * <p>Untuk memakai Ollama lokal/proxy, setel {@code AI_PROVIDER_AKTIF} menjadi
 * {@code OLLAMA_LOCAL} atau {@code OLLAMA_PROXY}.</p>
 *
 * <h3>Cara kerja {@code getKonfigurasi()} &mdash; PENTING sebelum mengubah nilai bawaan mana pun</h3>
 *
 * <p>{@link #getConfigValue(String, String)} memanggil
 * {@code Common.getKonfigurasi(nama, defaultValue)} &rarr; {@code KonfigurasiManager.getKonfigurasi(...)}.
 * Manager itu <b>bukan pembaca murni</b>: bila baris konfigurasi dengan nama tersebut belum ada di
 * basis data, ia <b>MEMBUAT dan MENYIMPAN</b> baris baru berisi {@code defaultValue} lalu
 * meng-<i>commit</i>-nya. Konsekuensinya sangat penting untuk berkas ini:</p>
 * <ol>
 *   <li>Nilai bawaan yang ditulis di kode Java hanya berlaku <b>satu kali</b>, yaitu pada
 *   pemanggilan pertama di sebuah instalasi. Sesudah itu nilai yang menang adalah isi baris di
 *   basis data.</li>
 *   <li>Karena itu, <b>mengubah literal bawaan di kode tidak memperbaiki instalasi yang sudah
 *   berjalan</b> &mdash; barisnya sudah tersemai dengan nilai lama. Perbaikan harus disertai
 *   pembaruan baris {@code konfigurasi} di basis data.</li>
 *   <li>{@link #getConfigValue(String, String)} hanya menerima nilai dari basis data bila tidak
 *   kosong; bila kosong ia jatuh kembali ke {@code defaultValue}.</li>
 * </ol>
 *
 * <h3>STATUS TEMUAN <code>task_d1f5ce07</code> &mdash; SUDAH DITAMBAL DI KODE (r85840), sisa risiko di DATA</h3>
 *
 * <p>Diverifikasi ulang langsung dari kode dan riwayat SVN. {@link #getActiveAiKey()} dahulu
 * memuat nilai bawaan berupa <b>kunci API Google Gemini asli</b> pada cabang {@code GEMINI}.
 * Revisi <b>r85840</b> menggantinya menjadi string kosong, sehingga cabang tersebut kini berbunyi
 * <code>getConfigValue("AI_GEMINI_KEY", "")</code> &mdash; sama seperti seluruh cabang provider
 * lainnya. <b>Kode sumber saat ini sudah bersih.</b></p>
 *
 * <p><b>Namun perbaikan kode saja BELUM menuntaskan insidennya</b>, karena mekanisme auto-seed di
 * atas: pada setiap instalasi yang pernah menjalankan versi sebelum r85840 dengan
 * {@code AI_PROVIDER_AKTIF=GEMINI} (nilai bawaan!), pemanggilan pertama
 * {@code getConfigValue("AI_GEMINI_KEY", "<kunci asli>")} sudah <b>menuliskan kunci asli itu ke
 * baris {@code konfigurasi} bernama {@code AI_GEMINI_KEY}</b>. Baris tersebut tetap ada dan tetap
 * dipakai setelah kode diperbaiki, sebab nilai basis data yang tidak kosong selalu menang atas
 * bawaan. Karena itu masih diperlukan, di luar berkas ini:</p>
 * <ul>
 *   <li>pencabutan/rotasi kunci Gemini yang bocor pada konsol Google Cloud &mdash; kunci itu juga
 *   masih tercatat di riwayat SVN (revisi sebelum r85840) sehingga harus dianggap bocor permanen;</li>
 *   <li>pembersihan atau penggantian baris {@code konfigurasi} bernama {@code AI_GEMINI_KEY} pada
 *   setiap basis data instalasi;</li>
 *   <li>pemeriksaan tagihan/kuota Google atas pemakaian tak sah.</li>
 * </ul>
 *
 * <h3>Catatan keamanan lain pada berkas ini</h3>
 * <ul>
 *   <li><b>Tidak ada pemeriksaan autentikasi maupun hak akses.</b> {@link #doGet} dan
 *   {@link #doPost} langsung bekerja tanpa memeriksa {@code Common.getCurrentUser()} maupun
 *   {@code CommonPrivilages.checkPrevilages(...)}, dan pemetaan <code>/Ai</code> tercakup aturan
 *   tangkap-semua <code>&lt;intercept-url pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY"/&gt;</code>.
 *   Siapa pun di internet dapat memakai endpoint ini sebagai proksi LLM gratis atas biaya dan kuota
 *   pemilik instalasi.</li>
 *   <li><b>{@link #debug} bernilai {@code true} secara bawaan</b> dan mencetak seluruh isi
 *   permintaan serta tanggapan AI ke stdout &mdash; termasuk teks yang mungkin memuat data
 *   akademik atau pribadi.</li>
 *   <li><b>{@link #writeError} memantulkan {@code rawResponse} dari penyedia</b> kembali ke
 *   pemanggil, sehingga pesan galat internal penyedia (nama proyek, batas kuota, detail konfigurasi)
 *   dapat terbaca pihak luar.</li>
 * </ul>
 *
 * <h3>Debug</h3>
 * <ul>
 *   <li>boolean debug default true sesuai permintaan.</li>
 *   <li>Jika debug == true, servlet akan menampilkan log detail setiap proses ke stdout/log server.</li>
 *   <li>Dapat dimatikan lewat konfigurasi {@code AI_DEBUG} (lihat {@link #refreshDebugConfig()});
 *   toggle-nya juga tersedia di layar Konfigurasi ({@code KonfigurasiNewAction}, label "Tampilkan
 *   debug log detail di AiGenerateServlet").</li>
 * </ul>
 *
 * <p><b>Perilaku kode TIDAK diubah pada revisi dokumentasi ini.</b></p>
 *
 * @see #generateText(String, int)
 * @see ais.action.master.helper.ObeAiJspHelper
 */
@SuppressWarnings("serial")
public class AiGenerateServlet extends HttpServlet {

	/**
	 * Charset UTF-8 yang dipakai untuk seluruh encoding/decoding byte pada servlet ini.
	 *
	 * <p>Dibuat sekali sebagai konstanta agar {@link #doPost} (saat menulis payload),
	 * {@link #callAiInternal(String, int)}, dan {@link #readStream(InputStream)} memakai charset yang
	 * <b>sama persis</b>. Ini penting karena payload dikirim dengan header
	 * {@code Content-Type: application/json; charset=UTF-8}; memakai charset bawaan platform akan
	 * merusak huruf beraksen dan tanda baca khas Bahasa Indonesia pada JVM yang default-nya bukan
	 * UTF-8 (lazim pada Windows).</p>
	 */
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	/**
	 * Base URL cadangan terakhir bila tidak ada satu pun konfigurasi base URL yang terisi.
	 *
	 * <p>Nilainya menunjuk lapisan OpenAI-compatible milik Google Gemini
	 * (<code>https://generativelanguage.googleapis.com/v1beta/openai</code>). Dipakai di dua tempat:
	 * sebagai bawaan {@code AI_OPENAI_BASE_URL} pada
	 * {@link #getActiveAiBaseUrl(String)} dan sebagai penjaga di
	 * {@link #buildChatCompletionsEndpoint(String)} ketika argumennya kosong.</p>
	 */
	private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai";

	/**
	 * Nama model cadangan bila konfigurasi model tidak terisi.
	 *
	 * <p>Bernilai {@code "gemini-1.5-flash"}, selaras dengan {@link #DEFAULT_BASE_URL}. Dipakai
	 * sebagai bawaan pada {@link #getActiveAiModel()} dan sebagai penjaga terakhir di
	 * {@link #doPost} serta {@link #callAiInternal(String, int)}.</p>
	 */
	private static final String DEFAULT_MODEL = "gemini-1.5-flash";

	/**
	 * Batas waktu koneksi dan baca bawaan dalam milidetik, sebagai {@link String}.
	 *
	 * <p>Bernilai {@code "240000"} (empat menit) &mdash; sengaja panjang karena model besar,
	 * khususnya Ollama yang berjalan di CPU, bisa butuh menit-menit untuk keluaran panjang.
	 * Bertipe {@code String} karena langsung diumpankan sebagai {@code defaultValue} ke
	 * {@link #getConfigValue(String, String)} yang bekerja pada teks; konversinya dilakukan
	 * {@link #getTimeoutMs()}.</p>
	 */
	private static final String DEFAULT_TIMEOUT_MS = "240000";

	/**
	 * Batas jumlah token keluaran bawaan, sebagai {@link String}.
	 *
	 * <p>Bernilai {@code "700"}. Namanya mengikuti istilah Ollama ({@code num_predict}), tetapi pada
	 * payload yang dikirim ia dipetakan ke field {@code max_tokens} milik format OpenAI (lihat
	 * {@link #buildOpenAiPayload(String, String, double, int)}). Sama seperti
	 * {@link #DEFAULT_TIMEOUT_MS}, bertipe {@code String} agar cocok dengan tanda tangan
	 * {@link #getConfigValue(String, String)}; konversinya di {@link #getNumPredict()}.</p>
	 */
	private static final String DEFAULT_NUM_PREDICT = "700";

	/**
	 * Sakelar pencatatan log detail; bernilai awal {@code true} sesuai permintaan pengguna.
	 *
	 * <p>Dibaca {@link #debugLog(String)} dan {@link #debugException(String, Exception)}, serta
	 * diperbarui dari konfigurasi {@code AI_DEBUG} oleh {@link #refreshDebugConfig()} pada setiap
	 * {@code doGet}/{@code doPost}/{@link #callAiInternal(String, int)}.</p>
	 *
	 * <p><b>Peringatan &mdash; state statis yang dapat berubah (mutable static).</b> Field ini
	 * {@code static} tetapi bukan {@code volatile}, sedangkan {@code refreshDebugConfig()} menulisinya
	 * dari thread request mana pun. Akibatnya: (a) nilainya dibagi ke seluruh request, bukan
	 * per-request; (b) perubahan oleh satu thread bisa tidak langsung terlihat thread lain (masalah
	 * <i>visibility</i> model memori Java). Dampaknya terbatas pada banyak/sedikitnya baris log, jadi
	 * tidak merusak fungsi &mdash; namun jangan menjadikan pola ini contoh untuk field yang
	 * memengaruhi keputusan bisnis.</p>
	 *
	 * <p><b>Peringatan privasi.</b> Selama bernilai {@code true}, isi prompt dan seluruh tanggapan AI
	 * dicetak ke stdout/log server. Bila prompt memuat data mahasiswa atau materi internal, data itu
	 * ikut mengendap di berkas log.</p>
	 */
	// Default true sesuai permintaan user.
	private static boolean debug = true;

	/**
	 * Menangani HTTP GET &mdash; endpoint pemeriksaan kesehatan (<i>health check</i>) servlet.
	 *
	 * <p><b>Tujuan.</b> Tidak memanggil AI sama sekali. Ia hanya melaporkan konfigurasi efektif yang
	 * sedang dipakai, sehingga administrator dapat memastikan mapping <code>/Ai</code> hidup dan
	 * provider/model/endpoint sudah sesuai sebelum menelusuri kegagalan lebih jauh.</p>
	 *
	 * <p><b>Cara kerja.</b> Menyegarkan sakelar {@link #debug} lewat {@link #refreshDebugConfig()},
	 * membaca {@link #getActiveAiModel()}, {@link #getOpenAiEndpoint()}, dan {@link #getTimeoutMs()},
	 * lalu menyusun JSON secara manual dengan {@link StringBuilder} dan menuliskannya. Bentuk
	 * keluarannya: {@code success}, {@code message}, {@code provider} (selalu literal
	 * {@code "openai-compatible"}, bukan nama provider yang aktif), {@code model}, {@code endpoint},
	 * {@code debug}, dan {@code timeoutMs}.</p>
	 *
	 * <p><b>Catatan keamanan.</b> Endpoint ini terbuka anonim (lihat Javadoc kelas) dan
	 * <b>membocorkan alamat endpoint AI internal</b> &mdash; termasuk alamat IP dan porta Ollama
	 * lokal/proksi bila provider-nya OLLAMA. Kunci API tidak ikut ditampilkan. Nilai {@code provider}
	 * yang selalu tetap juga menyesatkan: untuk mengetahui provider sesungguhnya, periksa
	 * {@code endpoint}.</p>
	 *
	 * @param request permintaan HTTP; isinya tidak dibaca selain untuk pencatatan log
	 * @param response tanggapan HTTP; diisi JSON status servlet dengan tipe {@code application/json}
	 * @throws ServletException bila kontainer melaporkan kegagalan servlet
	 * @throws IOException bila penulisan respons gagal
	 */
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

	/**
	 * Menangani HTTP POST &mdash; jalur utama pemanggilan AI dari peramban.
	 *
	 * <p><b>Tujuan.</b> Menerima prompt dari halaman JSP, meneruskannya ke endpoint AI aktif dalam
	 * format <i>chat completions</i> ala OpenAI, lalu mengembalikan teks hasilnya sebagai JSON.
	 * Servlet ini menjadi perantara agar peramban tidak terbentur CORS.</p>
	 *
	 * <h3>Alur sebelas langkah</h3>
	 * <ol>
	 *   <li><b>Membaca body.</b> {@link #readRequestBody(HttpServletRequest)} menyerap seluruh isi
	 *   request menjadi satu {@link String}. Tidak ada batas ukuran &mdash; body sebesar apa pun
	 *   masuk ke memori.</li>
	 *   <li><b>Menentukan prompt.</b> Dicari secara berurutan dari field JSON {@code instruksi},
	 *   {@code prompt}, {@code text}, lalu dari parameter form dengan tiga nama yang sama
	 *   ({@link #firstNotEmpty(String, String, String, String, String, String)}). Bila semuanya
	 *   kosong, dipakai prompt bawaan "Buatkan teks akademik yang rapi&hellip;", sehingga permintaan
	 *   kosong tetap menghasilkan panggilan berbayar ke penyedia alih-alih ditolak.</li>
	 *   <li><b>Menentukan model.</b> Model boleh <b>ditentukan pemanggil</b> lewat field/parameter
	 *   {@code model}; bila kosong barulah dipakai {@link #getActiveAiModel()}, lalu
	 *   {@link #DEFAULT_MODEL}. Perhatikan: nilai dari pemanggil dipakai apa adanya tanpa daftar
	 *   putih, sehingga klien dapat meminta model yang lebih mahal daripada yang dikonfigurasi
	 *   administrator.</li>
	 *   <li><b>Membaca temperature dan konfigurasi endpoint.</b> {@code temperature} dari
	 *   field/parameter, dengan bawaan dari {@code AI_TEMPERATURE_DEFAULT} (0.4). Lalu diambil
	 *   {@link #getOpenAiEndpoint()}, {@link #getActiveAiKey()}, {@link #getTimeoutMs()}, dan
	 *   {@link #getNumPredict()}. Pemanggil boleh menaikkan batas token lewat {@code maxTokens} /
	 *   {@code max_tokens}; nilainya <b>hanya diterima dalam rentang 50&ndash;8192</b>, di luar itu
	 *   diabaikan diam-diam dan konfigurasi tetap dipakai. Fitur ini ditambahkan agar "Buat Soal via
	 *   AI" tidak menghasilkan JSON terpotong, dan bersifat <i>backward-compatible</i>.</li>
	 *   <li><b>Menyusun payload</b> lewat {@link #buildOpenAiPayload(String, String, double, int)}.</li>
	 *   <li><b>Membuka koneksi</b> {@link HttpURLConnection} ke endpoint, metode POST, dengan
	 *   {@code Content-Type}/{@code Accept} JSON dan timeout koneksi maupun baca. Header
	 *   {@code Authorization: Bearer &lt;kunci&gt;} hanya dipasang bila kunci tidak kosong &mdash;
	 *   itulah sebabnya Ollama tanpa autentikasi tetap dapat dilayani kode yang sama.</li>
	 *   <li><b>Mengirim payload</b> sebagai byte UTF-8; {@code OutputStream} ditutup pada
	 *   {@code finally} lewat {@link #closeQuietly(OutputStream)}.</li>
	 *   <li><b>Membaca kode status</b> HTTP dari penyedia.</li>
	 *   <li><b>Membaca body tanggapan</b>, memilih {@code getInputStream()} untuk status 2xx dan
	 *   {@code getErrorStream()} untuk selainnya.</li>
	 *   <li><b>Menangani status non-2xx</b> dengan {@link #writeError} lalu berhenti; bila 2xx,
	 *   isi teks diekstrak {@link #parseOpenAiContent(String)}.</li>
	 *   <li><b>Menulis tanggapan sukses</b>: JSON berisi {@code success}, {@code provider},
	 *   {@code model}, serta teks hasil yang <b>digandakan</b> pada field {@code text} dan
	 *   {@code response} &mdash; kesengajaan agar berbagai halaman JSP lama maupun baru sama-sama
	 *   menemukan field yang mereka harapkan. Hasil kosong diperlakukan sebagai galat.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan galat dan pembersihan.</b> Seluruh {@link Exception} ditangkap dan diubah
	 * menjadi tanggapan {@link #writeError} sehingga klien selalu menerima JSON, tidak pernah halaman
	 * galat kontainer. Blok {@code finally} selalu memanggil {@code conn.disconnect()} dan mencatat
	 * total waktu.</p>
	 *
	 * <p><b>Catatan keamanan.</b> Tidak ada pemeriksaan autentikasi maupun hak akses di sini, dan
	 * <code>/Ai</code> terbuka anonim. Digabung dengan prompt bawaan pada Langkah 2 dan pemilihan
	 * model bebas pada Langkah 3, pihak luar dapat memakai instalasi ini sebagai proksi LLM atas
	 * biaya dan kuota pemilik. Lihat pula peringatan pada {@link #writeError} dan {@link #debug}.</p>
	 *
	 * @param request permintaan HTTP; body JSON atau parameter form berisi {@code instruksi}/
	 *        {@code prompt}/{@code text}, serta opsional {@code model}, {@code temperature},
	 *        {@code maxTokens}/{@code max_tokens}
	 * @param response tanggapan HTTP; selalu JSON, baik sukses maupun galat
	 * @throws ServletException bila kontainer melaporkan kegagalan servlet
	 * @throws IOException bila penulisan respons gagal
	 */
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
	 *
	 * <p><b>Cara kerja.</b> Membuat instance {@code AiGenerateServlet} secara manual &mdash; bukan
	 * instance yang dikelola kontainer &mdash; lalu memanggil {@link #callAiInternal(String, int)}
	 * padanya. Pola ini dipakai semata karena helper-helper privat di kelas ini bersifat
	 * <i>instance method</i>; servlet ini memang tanpa state (kecuali {@link #debug} yang statis),
	 * sehingga instansiasi manual aman dan tidak menyentuh daur hidup servlet
	 * ({@code init()}/{@code destroy()} tidak dipanggil, dan memang tidak diperlukan).</p>
	 *
	 * <p><b>Berbeda dari {@link #doPost}</b> dalam dua hal: model tidak dapat ditentukan pemanggil
	 * (selalu {@link #getActiveAiModel()}), dan kegagalan <b>dilempar sebagai {@link Exception}</b>
	 * alih-alih diubah menjadi JSON galat &mdash; sehingga JSP pemanggil wajib membungkusnya dengan
	 * {@code try/catch} sendiri.</p>
	 *
	 * <p><b>Pemakai.</b> {@code _generate_cpl_ai.jsp} (1500 token), {@code _generate_cpmk_ai.jsp}
	 * (1500), {@code _generate_subcpmk_ai.jsp} (900), {@code _generate_deskripsi_pustaka_ai.jsp}
	 * (1800), {@code _generate_catatan_obe_ai.jsp} (2000), {@code _generate_cqi_ai.jsp} (2500), dan
	 * {@code _generate_rincian_ai.jsp} (8000 &mdash; nilai ini akan dipangkas menjadi 8192 hanya bila
	 * melebihi batas, jadi 8000 lolos apa adanya).</p>
	 *
	 * <p><b>Peringatan waktu tanggap.</b> Method ini <i>blocking</i> dengan timeout bawaan empat
	 * menit ({@link #DEFAULT_TIMEOUT_MS}). Karena dipanggil langsung dari dalam JSP, thread request
	 * pengguna tertahan selama itu. Untuk keluaran panjang, pertimbangkan memanggilnya lewat jalur
	 * asinkron.</p>
	 *
	 * @param prompt teks instruksi untuk model; tidak boleh {@code null} maupun hanya spasi
	 * @param maxTokens batas token keluaran yang diinginkan; otomatis dijepit ke rentang
	 *        50&ndash;8192 oleh {@link #callAiInternal(String, int)}
	 * @return teks hasil AI yang sudah diekstrak dari amplop JSON penyedia; tidak pernah kosong
	 * @throws Exception bila prompt kosong, endpoint tidak dapat dihubungi, penyedia membalas status
	 *         non-2xx, atau tanggapan tidak memuat teks yang dikenali
	 * @see #callAiInternal(String, int)
	 */
	public static String generateText(String prompt, int maxTokens) throws Exception {
		AiGenerateServlet inst = new AiGenerateServlet();
		return inst.callAiInternal(prompt, maxTokens);
	}

	/**
	 * Pelaksana sebenarnya dari {@link #generateText(String, int)} &mdash; versi ringkas
	 * {@link #doPost} tanpa lapisan HTTP servlet.
	 *
	 * <p><b>Cara kerja.</b> (1) Menyegarkan {@link #debug}; (2) menolak prompt kosong dengan
	 * {@link Exception}; (3) menentukan model dari {@link #getActiveAiModel()} dengan cadangan
	 * {@link #DEFAULT_MODEL}; (4) membaca {@code AI_TEMPERATURE_DEFAULT}, endpoint, kunci, dan
	 * timeout; (5) <b>menjepit</b> {@code maxTokens} ke rentang 50&ndash;8192 &mdash; berbeda dari
	 * {@link #doPost} yang justru <i>mengabaikan</i> nilai di luar rentang dan tetap memakai
	 * konfigurasi; (6) menyusun payload, mengirimnya, membaca status dan body; (7) melempar
	 * {@link Exception} bila status non-2xx (dengan cuplikan 300 karakter pertama body) atau bila
	 * hasil ekstraksi kosong; (8) memutus koneksi pada {@code finally}.</p>
	 *
	 * <p><b>Catatan.</b> Berbeda dari sisa berkas ini, blok {@code catch} pada {@code disconnect()}
	 * di {@code finally} sengaja dibiarkan kosong tanpa {@code debugException} &mdash; kegagalan
	 * memutus koneksi tidak boleh menutupi hasil atau exception yang sebenarnya.</p>
	 *
	 * @param prompt teks instruksi untuk model
	 * @param maxTokens batas token keluaran; dijepit ke 50&ndash;8192
	 * @return teks hasil AI, dijamin tidak kosong
	 * @throws Exception bila prompt kosong, koneksi gagal, status non-2xx, atau tanggapan tak dikenali
	 */
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

	/**
	 * Menyusun badan permintaan JSON berformat <i>chat completions</i> ala OpenAI.
	 *
	 * <p><b>Cara kerja.</b> Mengambil <i>system prompt</i> dari konfigurasi {@code AI_SYSTEM_PROMPT}
	 * &mdash; dengan bawaan panjang yang mengarahkan model berbahasa Indonesia formal dan memakai
	 * istilah akademik (RPS, CPL, CPMK, Sub-CPMK, CP, TP, ATP, asesmen, rubrik) serta melarang HTML
	 * kecuali diminta &mdash; lalu merangkai objek JSON berisi {@code model}, {@code stream:false},
	 * {@code temperature}, {@code max_tokens}, dan larik {@code messages} dengan dua entri: peran
	 * {@code system} berisi system prompt dan peran {@code user} berisi prompt pemanggil.</p>
	 *
	 * <p><b>Mengapa JSON dirangkai manual.</b> Berkas ini sengaja tidak memakai pustaka JSON agar
	 * bebas dari ketergantungan tambahan. Keamanannya bertumpu sepenuhnya pada
	 * {@link #escapeJson(String)}, yang dipanggil untuk <b>setiap</b> nilai teks. Bila kelak
	 * menambah field baru, jangan lupa membungkus nilainya dengan {@code escapeJson} &mdash; prompt
	 * berasal dari pemanggil dan tanpa itu payload bisa dibelokkan (mis. menyisipkan pesan
	 * {@code system} tambahan).</p>
	 *
	 * <p><b>Catatan format.</b> {@code stream:false} bersifat wajib bagi kode ini, sebab
	 * {@link #parseOpenAiContent(String)} mengharapkan satu dokumen JSON utuh, bukan aliran
	 * <i>server-sent events</i>. Nilai {@code max_tokens} adalah nama field OpenAI; Ollama pada mode
	 * OpenAI-compatible memetakannya sendiri ke {@code num_predict}.</p>
	 *
	 * @param model nama model yang diminta
	 * @param prompt instruksi pengguna
	 * @param temperature tingkat keacakan keluaran; diformat {@link #formatDouble(double)}
	 * @param numPredict batas token keluaran, dikirim sebagai {@code max_tokens}
	 * @return string JSON siap kirim; tidak pernah {@code null}
	 */
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

	/**
	 * Mengekstrak teks hasil dari amplop JSON tanggapan penyedia AI.
	 *
	 * <p><b>Cara kerja.</b> Mencoba tiga nama field berurutan lewat
	 * {@link #extractJsonValue(String, String)}, dan mengembalikan yang pertama tidak kosong:</p>
	 * <ol>
	 *   <li>{@code content} &mdash; format OpenAI dan Ollama mode OpenAI-compatible:
	 *   <code>{"choices":[{"message":{"role":"assistant","content":"&hellip;"}}]}</code>;</li>
	 *   <li>{@code response} &mdash; cadangan untuk API asli Ollama ({@code /api/generate}) bila
	 *   {@code AI_OPENAI_URL} diarahkan ke sana;</li>
	 *   <li>{@code text} &mdash; cadangan umum untuk penyedia lain.</li>
	 * </ol>
	 * <p>Bila ketiganya kosong, mengembalikan string kosong; pemanggil ({@link #doPost} dan
	 * {@link #callAiInternal(String, int)}) memperlakukannya sebagai galat.</p>
	 *
	 * <p><b>Keterbatasan yang perlu diketahui.</b> Ekstraksinya berbasis pemindaian teks, bukan
	 * penguraian JSON sungguhan. Yang diambil adalah <b>kemunculan pertama</b> nama field di seluruh
	 * dokumen &mdash; termasuk bila nama itu muncul di dalam sebuah nilai string. Untuk format OpenAI
	 * hal ini kebetulan aman karena {@code "content"} pertama memang milik pesan asisten, tetapi
	 * penyedia yang menyisipkan objek lain lebih dahulu (mis. blok penalaran atau kutipan) dapat
	 * membuat method ini memungut potongan yang salah. Selain itu, larik {@code choices} berisi lebih
	 * dari satu kandidat hanya terbaca kandidat pertamanya. Bila kelak dibutuhkan ketelitian penuh,
	 * ganti dengan pustaka JSON sungguhan ({@code org.json} sudah tersedia di classpath).</p>
	 *
	 * @param json badan tanggapan mentah dari penyedia; boleh {@code null}
	 * @return teks hasil, atau string kosong bila tidak ada field yang dikenali
	 */
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

	/**
	 * Menentukan URL endpoint <i>chat completions</i> yang akan dihubungi.
	 *
	 * <p><b>Cara kerja &mdash; dua jalur.</b></p>
	 * <ol>
	 *   <li><b>Jalur pintas.</b> Bila konfigurasi {@code AI_OPENAI_URL} terisi, nilainya (setelah
	 *   {@code trim()}) dipakai <b>apa adanya</b> sebagai URL penuh. Ini mengalahkan
	 *   {@code AI_PROVIDER_AKTIF} beserta seluruh pemetaan base-URL &mdash; berguna untuk endpoint
	 *   tak lazim, tetapi juga berarti sebuah instalasi bisa tampak "memakai GEMINI" pada
	 *   {@link #getActiveAiProvider()} sementara lalu lintasnya sebenarnya mengalir ke tempat lain.
	 *   Saat mendiagnosis, <b>periksa {@code AI_OPENAI_URL} lebih dulu.</b></li>
	 *   <li><b>Jalur normal.</b> Ambil provider aktif, petakan ke base URL lewat
	 *   {@link #getActiveAiBaseUrl(String)}, lalu lengkapi menjadi endpoint utuh dengan
	 *   {@link #buildChatCompletionsEndpoint(String)}.</li>
	 * </ol>
	 *
	 * <p><b>Catatan keamanan.</b> Nilai ini berasal dari tabel {@code konfigurasi} (dikelola
	 * administrator), <b>bukan</b> dari permintaan pengguna &mdash; sehingga tidak membuka SSRF yang
	 * dikendalikan penyerang lewat HTTP. Meski begitu, kemampuan menulis baris konfigurasi setara
	 * dengan kemampuan mengarahkan seluruh lalu lintas AI (beserta header
	 * {@code Authorization: Bearer}, yaitu kunci API) ke host pilihan sendiri. Karena itu hak ubah
	 * konfigurasi harus diperlakukan sebagai hak istimewa tinggi.</p>
	 *
	 * @return URL endpoint lengkap yang siap dipakai {@link URL}; tidak pernah {@code null}
	 */
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

	/**
	 * Membaca nama penyedia AI yang sedang aktif dari konfigurasi {@code AI_PROVIDER_AKTIF}.
	 *
	 * <p><b>Cara kerja.</b> Mengambil nilai dengan bawaan {@code "GEMINI"}, memaksa {@code "GEMINI"}
	 * lagi bila hasilnya kosong (penjaga ganda terhadap baris konfigurasi yang ada tapi bernilai
	 * kosong), lalu mengembalikannya dalam bentuk {@code trim().toUpperCase()}. Normalisasi huruf
	 * besar inilah yang membuat seluruh perbandingan di
	 * {@link #getActiveAiBaseUrl(String)}, {@link #getActiveAiModel()}, dan {@link #getActiveAiKey()}
	 * cukup memakai {@code equals} biasa.</p>
	 *
	 * <p><b>Penting.</b> Karena bawaannya {@code GEMINI}, instalasi yang <b>belum pernah</b> menyetel
	 * konfigurasi apa pun tetap mengarah ke Google Gemini &mdash; bukan ke Ollama lokal. Inilah
	 * sebabnya nilai bawaan kunci pada {@link #getActiveAiKey()} berdampak begitu luas; lihat
	 * uraian {@code task_d1f5ce07} pada Javadoc kelas.</p>
	 *
	 * <p><b>Nilai tak dikenal</b> tidak ditolak: ia jatuh ke cabang terakhir masing-masing method
	 * pemetaan, yaitu perlakuan "Ollama lokal / OpenAI generik". Jadi salah ketik pada
	 * {@code AI_PROVIDER_AKTIF} tidak memunculkan galat, melainkan diam-diam mengganti penyedia.</p>
	 *
	 * @return nama provider dalam huruf besar, mis. {@code "GEMINI"}; tidak pernah kosong
	 */
	private String getActiveAiProvider() {
		String provider = getConfigValue("AI_PROVIDER_AKTIF", "GEMINI");
		if (isBlank(provider)) {
			provider = "GEMINI";
		}
		return provider.trim().toUpperCase();
	}

	/**
	 * Memetakan nama provider ke base URL-nya, dibaca dari konfigurasi masing-masing.
	 *
	 * <p><b>Cara kerja.</b> Rangkaian {@code if} atas nilai {@code provider} (sudah huruf besar dari
	 * {@link #getActiveAiProvider()}); tiap cabang membaca kunci konfigurasi khusus provider itu
	 * dengan alamat resmi sebagai bawaan:</p>
	 * <ul>
	 *   <li>{@code OLLAMA_PROXY} &rarr; {@code AI_OLLAMA_PROXY_BASE_URL}, bawaan
	 *   <code>http://38.47.178.42:9002</code> (proksi Ollama milik pengembang &mdash; alamat pihak
	 *   ketiga yang tertanam di kode; jangan dipakai untuk data sensitif tanpa persetujuan);</li>
	 *   <li>{@code GEMINI} &rarr; {@code AI_GEMINI_BASE_URL}, bawaan lapisan OpenAI-compatible
	 *   Google;</li>
	 *   <li>{@code GROQ} &rarr; {@code AI_GROQ_BASE_URL}, bawaan <code>https://api.groq.com/openai/v1</code>;</li>
	 *   <li>{@code CLOUDFLARE} &rarr; {@code AI_CLOUDFLARE_BASE_URL}, bawaan memuat penanda
	 *   <code>ACCOUNT_ID</code> yang <b>harus</b> diganti id akun sungguhan &mdash; bila tidak,
	 *   panggilan pasti gagal;</li>
	 *   <li>{@code OPENAI} &rarr; {@code AI_OPENAI_CLOUD_BASE_URL};</li>
	 *   <li>{@code DEEPSEEK} &rarr; {@code AI_DEEPSEEK_BASE_URL}.</li>
	 * </ul>
	 * <p>Bila tidak ada yang cocok (termasuk {@code OLLAMA_LOCAL} dan salah ketik apa pun), dicoba
	 * {@code AI_OLLAMA_LOCAL_BASE_URL}; bila itu pun kosong, dipakai {@code AI_OPENAI_BASE_URL}
	 * dengan bawaan {@link #DEFAULT_BASE_URL}.</p>
	 *
	 * <p><b>Perhatian.</b> Cabang terakhir mengembalikan nilai {@code AI_OLLAMA_LOCAL_BASE_URL} tanpa
	 * {@code trim()}; perapiannya baru terjadi di {@link #buildChatCompletionsEndpoint(String)}.</p>
	 *
	 * @param provider nama provider huruf besar dari {@link #getActiveAiProvider()}
	 * @return base URL penyedia; masih berupa base, belum endpoint chat completions
	 */
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

	/**
	 * Memetakan provider aktif ke nama model yang dipakai, dibaca dari konfigurasi masing-masing.
	 *
	 * <p><b>Cara kerja.</b> Memanggil {@link #getActiveAiProvider()} lalu memilih kunci konfigurasi:</p>
	 * <ul>
	 *   <li>{@code OLLAMA_PROXY} atau {@code OLLAMA_LOCAL} &rarr; {@code AI_OLLAMA_AKADEMIK_MODEL},
	 *   dengan <b>bawaan bertingkat</b>: bila kunci itu belum ada, bawaannya adalah hasil pembacaan
	 *   {@code AI_OPENAI_MODEL} (yang sendirinya berbawaan {@link #DEFAULT_MODEL}). Perhatikan bahwa
	 *   {@code getConfigValue} bagian dalam <b>selalu dievaluasi</b> karena Java menghitung argumen
	 *   lebih dulu &mdash; artinya baris {@code AI_OPENAI_MODEL} ikut tersemai ke basis data meski
	 *   nilainya tidak jadi dipakai;</li>
	 *   <li>{@code GEMINI} &rarr; {@code AI_GEMINI_MODEL}, bawaan {@code gemini-1.5-flash};</li>
	 *   <li>{@code GROQ} &rarr; {@code AI_GROQ_MODEL}, bawaan {@code llama-3.1-8b-instant};</li>
	 *   <li>{@code CLOUDFLARE} &rarr; {@code AI_CLOUDFLARE_MODEL}, bawaan
	 *   {@code @cf/meta/llama-3.1-8b-instruct};</li>
	 *   <li>{@code OPENAI} &rarr; {@code AI_OPENAI_CLOUD_MODEL}, bawaan {@code gpt-4o-mini};</li>
	 *   <li>{@code DEEPSEEK} &rarr; {@code AI_DEEPSEEK_MODEL}, bawaan {@code deepseek-chat};</li>
	 *   <li>selain itu &rarr; {@code AI_OPENAI_MODEL} dengan bawaan {@link #DEFAULT_MODEL}.</li>
	 * </ul>
	 *
	 * <p><b>Pemeliharaan.</b> Nama-nama model bawaan di atas menua seiring waktu (penyedia menghentikan
	 * versi lama). Bila panggilan tiba-tiba gagal dengan galat "model not found" pada instalasi yang
	 * sebelumnya normal, kemungkinan besar penyebabnya adalah baris konfigurasi model yang tersemai
	 * lama, bukan kode ini.</p>
	 *
	 * @return nama model untuk provider aktif; bisa kosong bila baris konfigurasinya ada tetapi
	 *         bernilai kosong &mdash; pemanggil sudah menyiapkan cadangan {@link #DEFAULT_MODEL}
	 */
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

	/**
	 * Memetakan provider aktif ke kunci API-nya, dibaca dari konfigurasi masing-masing.
	 *
	 * <p><b>Cara kerja.</b> Sama polanya dengan {@link #getActiveAiBaseUrl(String)} dan
	 * {@link #getActiveAiModel()}: {@code GEMINI} &rarr; {@code AI_GEMINI_KEY}, {@code GROQ} &rarr;
	 * {@code AI_GROQ_KEY}, {@code CLOUDFLARE} &rarr; {@code AI_CLOUDFLARE_KEY}, {@code OPENAI} &rarr;
	 * {@code AI_OPENAI_CLOUD_KEY}, {@code DEEPSEEK} &rarr; {@code AI_DEEPSEEK_KEY}, selain itu
	 * (termasuk seluruh varian Ollama) &rarr; {@code AI_OPENAI_KEY}. <b>Seluruh cabang kini berbawaan
	 * string kosong.</b></p>
	 *
	 * <p>Nilai baliknya dipakai {@link #doPost} dan {@link #callAiInternal(String, int)} untuk
	 * memasang header {@code Authorization: Bearer &lt;kunci&gt;} &mdash; dan hanya bila tidak kosong.
	 * Bawaan kosong karena itu berarti "jangan kirim header autentikasi", yang justru merupakan
	 * perilaku yang benar untuk Ollama lokal tanpa autentikasi, sekaligus membuat provider berbayar
	 * <b>gagal terang-terangan dengan HTTP 401</b> alih-alih diam-diam memakai kunci milik orang
	 * lain.</p>
	 *
	 * <h3>Riwayat keamanan &mdash; <code>task_d1f5ce07</code></h3>
	 * <p><b>Sebelum revisi r85840</b>, cabang {@code GEMINI} berbunyi
	 * <code>getConfigValue("AI_GEMINI_KEY", "&lt;kunci Gemini asli&gt;")</code>: sebuah kunci API
	 * Google yang sah dan berlaku, tertanam di kode sumber. Karena
	 * {@link #getActiveAiProvider()} berbawaan {@code GEMINI}, kunci itu aktif pada <b>setiap</b>
	 * instalasi yang belum menyetel apa pun. <b>Kode ini sudah ditambal</b> &mdash; per revisi
	 * dokumentasi ini cabang tersebut memakai bawaan kosong dan sudah setara dengan cabang lainnya.</p>
	 * <p><b>Yang belum selesai</b> ada di lapisan data, bukan kode. Karena
	 * {@link #getConfigValue(String, String)} menyemai bawaan ke basis data (lihat Javadoc kelas),
	 * instalasi yang pernah menjalankan versi lama sudah menyimpan kunci asli itu pada baris
	 * {@code konfigurasi} bernama {@code AI_GEMINI_KEY} &mdash; dan nilai basis data yang tidak
	 * kosong <b>mengalahkan</b> bawaan baru. Jadi menambal kode saja tidak menghentikan pemakaian
	 * kunci yang bocor. Diperlukan: rotasi/pencabutan kunci di Google Cloud (kunci juga tersimpan
	 * permanen di riwayat SVN sebelum r85840), pembersihan baris {@code AI_GEMINI_KEY} di setiap
	 * basis data, dan pemeriksaan tagihan atas pemakaian tak sah.</p>
	 *
	 * @return kunci API untuk provider aktif; string kosong bila tidak dikonfigurasi, yang berarti
	 *         permintaan dikirim tanpa header {@code Authorization}
	 */
	private String getActiveAiKey() {
		String provider = getActiveAiProvider();
		if ("GEMINI".equals(provider)) {
			return getConfigValue("AI_GEMINI_KEY", "");
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

	/**
	 * Melengkapi sebuah base URL menjadi URL endpoint <i>chat completions</i> yang utuh.
	 *
	 * <p><b>Cara kerja.</b> (1) Bila argumen kosong, dipakai {@link #DEFAULT_BASE_URL}; (2) di-
	 * {@code trim()} dan seluruh garis miring di ujung dibuang lewat perulangan {@code while};
	 * (3) bila sudah berakhiran {@code /chat/completions} atau {@code /v1/chat/completions},
	 * dikembalikan apa adanya &mdash; sifat <i>idempoten</i> ini membuat administrator boleh mengisi
	 * konfigurasi dengan URL penuh maupun base saja; (4) bila berakhiran {@code /v1},
	 * {@code /openai}, atau {@code /ai/v1}, cukup ditambahi {@code /chat/completions}; (5) selain
	 * itu ditambahi {@code /v1/chat/completions}.</p>
	 *
	 * <p><b>Contoh.</b> <code>http://192.168.88.128:11434</code> &rarr;
	 * <code>http://192.168.88.128:11434/v1/chat/completions</code>;
	 * <code>https://generativelanguage.googleapis.com/v1beta/openai</code> &rarr; berakhiran
	 * {@code /openai} sehingga menjadi <code>&hellip;/v1beta/openai/chat/completions</code>;
	 * <code>https://api.groq.com/openai/v1/</code> &rarr; garis miring dibuang, berakhiran
	 * {@code /v1}, menjadi <code>https://api.groq.com/openai/v1/chat/completions</code>.</p>
	 *
	 * <p><b>Keterbatasan.</b> Aturannya murni berbasis akhiran teks. Base URL dengan bentuk tak lazim
	 * (mis. penyedia yang memakai {@code /v2} atau jalur bertingkat lain) akan memperoleh akhiran
	 * {@code /v1/chat/completions} yang keliru. Untuk kasus seperti itu, isi
	 * {@code AI_OPENAI_URL} dengan URL penuh agar seluruh logika ini dilewati.</p>
	 *
	 * @param baseUrl base URL penyedia; boleh {@code null}, kosong, atau berakhiran garis miring
	 * @return URL endpoint chat completions yang utuh
	 */
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

	/**
	 * Menyegarkan sakelar {@link #debug} dari konfigurasi {@code AI_DEBUG}.
	 *
	 * <p><b>Cara kerja.</b> Membaca {@code Common.getKonfigurasi("AI_DEBUG", Konfigurasi.AKTIF)}.
	 * Perhatikan bahwa yang dipakai adalah {@code Common.getKonfigurasi} langsung, <b>bukan</b>
	 * {@link #getConfigValue(String, String)} &mdash; sebab nilai yang dibutuhkan di sini adalah
	 * penanda aktif/tidak-aktif, bukan teks bebas. Bila baris ditemukan dan nilainya tidak
	 * {@code null}, {@link #debug} disetel {@code true} hanya bila nilainya persis
	 * {@link Konfigurasi#AKTIF} (yaitu string {@code "aktif"}) setelah {@code trim()}. Bila baris
	 * belum ada, mekanisme auto-seed {@code KonfigurasiManager} akan membuatnya dengan nilai
	 * {@code "aktif"} &mdash; itulah sebabnya log detail menyala secara bawaan pada instalasi baru.</p>
	 *
	 * <p><b>Penanganan galat.</b> Bila pembacaan konfigurasi melempar (mis. basis data belum siap
	 * saat startup), {@link #debug} sengaja dipaksa {@code true} agar penelusuran masalah tetap
	 * mungkin justru pada saat sistem sedang bermasalah. Konsekuensinya: gangguan pada basis data
	 * dapat diam-diam <b>menghidupkan kembali</b> log detail yang sebelumnya dimatikan administrator.</p>
	 *
	 * <p><b>Dipanggil di awal</b> {@link #doGet}, {@link #doPost}, dan
	 * {@link #callAiInternal(String, int)}. Karena {@link #debug} bersifat statis, panggilan ini
	 * mengubah perilaku log untuk seluruh request yang sedang berjalan, bukan hanya request ini.</p>
	 */
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

	/**
	 * Membaca batas waktu koneksi/baca dari konfigurasi {@code AI_TIMEOUT_MS}.
	 *
	 * <p><b>Cara kerja.</b> Mengambil nilai teks dengan bawaan {@link #DEFAULT_TIMEOUT_MS}
	 * ({@code "240000"}), menguraikannya sebagai {@code double} lewat
	 * {@link #parseDouble(String, double)} dengan cadangan {@code 240000D}, lalu meng-<i>cast</i>-nya
	 * ke {@code int}. Nilai ini dipasang sebagai {@code setConnectTimeout} <b>dan</b>
	 * {@code setReadTimeout}.</p>
	 *
	 * <p><b>Perhatian.</b> Tidak ada penjaga batas bawah maupun atas &mdash; berbeda dari
	 * {@link #getNumPredict()} yang punya lantai 50. Konfigurasi bernilai {@code 0} berarti
	 * <b>menunggu selamanya</b> menurut kontrak {@link java.net.URLConnection}, sehingga thread
	 * request bisa tertahan tanpa batas bila penyedia tidak menjawab. Nilai negatif akan melempar
	 * {@link IllegalArgumentException} saat dipasang, yang lalu ditangkap {@link #doPost} dan diubah
	 * menjadi JSON galat. Isi konfigurasi ini dengan angka positif yang wajar.</p>
	 *
	 * @return batas waktu dalam milidetik
	 */
	private int getTimeoutMs() {
		int timeout = (int) parseDouble(getConfigValue("AI_TIMEOUT_MS", DEFAULT_TIMEOUT_MS), 240000D);
		debugLog("getTimeoutMs()=" + timeout);
		return timeout;
	}

	/**
	 * Membaca batas token keluaran dari konfigurasi {@code AI_NUM_PREDICT}.
	 *
	 * <p><b>Cara kerja.</b> Mengambil nilai teks dengan bawaan {@link #DEFAULT_NUM_PREDICT}
	 * ({@code "700"}), menguraikannya lewat {@link #parseDouble(String, double)} dengan cadangan
	 * {@code 700D}, meng-<i>cast</i> ke {@code int}, lalu <b>menaikkannya ke 50 bila lebih kecil</b>
	 * &mdash; penjaga agar konfigurasi yang keliru tidak menghasilkan jawaban terpotong sampai tak
	 * berguna.</p>
	 *
	 * <p><b>Catatan.</b> Tidak ada batas atas di sini; pembatasan 8192 hanya diterapkan pada
	 * <i>override</i> per-permintaan di {@link #doPost} dan pada penjepitan di
	 * {@link #callAiInternal(String, int)}. Nilai konfigurasi yang sangat besar akan diteruskan apa
	 * adanya, dan penyedialah yang menolaknya bila melampaui batas modelnya.</p>
	 *
	 * @return batas token keluaran, minimal 50
	 */
	private int getNumPredict() {
		int numPredict = (int) parseDouble(getConfigValue("AI_NUM_PREDICT", DEFAULT_NUM_PREDICT), 700D);
		if (numPredict < 50) {
			numPredict = 50;
		}
		debugLog("getNumPredict()=" + numPredict);
		return numPredict;
	}

	/**
	 * Pembaca konfigurasi tunggal untuk seluruh berkas ini &mdash; membaca satu kunci dari tabel
	 * {@code konfigurasi} dengan nilai bawaan.
	 *
	 * <p><b>Cara kerja.</b> Memanggil {@code Common.getKonfigurasi(key, defaultValue)}, dan bila
	 * hasilnya tidak {@code null} serta {@code getNilai()} tidak {@code null} <i>maupun</i> kosong,
	 * nilainya dikembalikan. Selain itu dikembalikan {@code defaultValue}. Setiap kegagalan
	 * ditangkap, dicatat lewat {@link #debugException(String, Exception)}, dan juga berujung pada
	 * {@code defaultValue} &mdash; artinya gangguan basis data <b>tidak</b> menggagalkan panggilan
	 * AI, melainkan mengembalikannya ke perilaku bawaan.</p>
	 *
	 * <p><b>PENTING &mdash; ini bukan pembacaan murni.</b> {@code Common.getKonfigurasi} bermuara ke
	 * {@code KonfigurasiManager.getKonfigurasi}, yang <b>MEMBUAT dan MENYIMPAN</b> baris konfigurasi
	 * baru berisi {@code defaultValue} bila kunci tersebut belum ada. Konsekuensinya: (a) nilai
	 * bawaan di kode hanya berpengaruh sekali seumur instalasi; (b) <b>mengubah literal bawaan tidak
	 * memperbaiki instalasi yang sudah berjalan</b> &mdash; barisnya sudah tersemai; (c) memanggil
	 * method ini dengan kunci baru punya efek samping tulis ke basis data. Inilah kunci memahami
	 * mengapa perbaikan kunci API pada {@link #getActiveAiKey()} perlu ditindaklanjuti di lapisan
	 * data.</p>
	 *
	 * <p><b>Ketidakefisienan yang disengaja dibiarkan.</b> Pemeriksaan {@code null} dan pengambilan
	 * nilai memanggil {@code Common.getKonfigurasi(...)} sampai <b>tiga kali</b> untuk kunci yang
	 * sama dalam satu invokasi. Hasilnya benar karena manager memakai cache, tetapi bila kelak
	 * disentuh, satukan menjadi satu pemanggilan ke variabel lokal.</p>
	 *
	 * <p><b>Log.</b> Nilai yang dibaca ikut dicatat, namun disaring lebih dulu oleh
	 * {@link #maskIfSecret(String, String)} sehingga kunci API tidak pernah tercetak utuh.</p>
	 *
	 * @param key nama kunci konfigurasi, mis. {@code "AI_GEMINI_MODEL"}
	 * @param defaultValue nilai yang dipakai &mdash; <b>dan disemai ke basis data</b> &mdash; bila
	 *        kunci belum ada atau nilainya kosong
	 * @return nilai konfigurasi, atau {@code defaultValue}
	 */
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

	/**
	 * Membaca seluruh badan permintaan HTTP menjadi satu {@link String}.
	 *
	 * <p><b>Cara kerja.</b> Mengambil {@code request.getReader()} &mdash; yang menghormati
	 * {@code setCharacterEncoding("UTF-8")} yang sudah dipanggil di awal {@link #doPost} &mdash; lalu
	 * membaca baris demi baris dan menggabungkannya ke {@link StringBuilder}. Reader ditutup pada
	 * {@code finally}; kegagalan penutupan hanya dicatat.</p>
	 *
	 * <p><b>Perhatian &mdash; pemisah baris hilang.</b> Karena memakai {@code readLine()} dan
	 * menyambung tanpa menyisipkan {@code "\n"}, seluruh baris menyatu tanpa pemisah. Untuk JSON
	 * satu baris (bentuk yang dikirim seluruh JSP pemanggil) ini tidak berpengaruh, dan untuk JSON
	 * berformat rapi pun masih aman sebab spasi antar-token tidak wajib. <b>Tetapi</b> bila badan
	 * permintaan berupa teks biasa berbaris banyak, batas barisnya lenyap tanpa peringatan. Bila
	 * kelak endpoint ini menerima teks polos, tambahkan {@code sb.append('\n')} di dalam
	 * perulangan.</p>
	 *
	 * <p><b>Tidak ada batas ukuran.</b> Badan permintaan sebesar apa pun diserap ke memori; tidak ada
	 * penjaga panjang maksimum. Digabung dengan ketiadaan autentikasi pada {@code /Ai}, ini membuka
	 * peluang penghabisan memori dari luar.</p>
	 *
	 * @param request permintaan HTTP yang badannya akan dibaca
	 * @return isi badan permintaan sebagai satu string; kosong bila badan kosong, tidak pernah
	 *         {@code null}
	 * @throws IOException bila pembacaan aliran gagal
	 */
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

	/**
	 * Membaca seluruh isi sebuah {@link InputStream} menjadi {@link String} ber-encoding UTF-8.
	 *
	 * <p><b>Cara kerja.</b> Mengembalikan string kosong bila argumennya {@code null} &mdash; penting,
	 * karena {@link HttpURLConnection#getErrorStream()} memang mengembalikan {@code null} ketika
	 * penyedia membalas status galat tanpa badan. Selebihnya membungkus aliran dengan
	 * {@link InputStreamReader} ber-{@link #UTF_8} lalu {@link BufferedReader}, membaca baris demi
	 * baris, dan menutup reader pada {@code finally}.</p>
	 *
	 * <p><b>Perhatian.</b> Sama seperti {@link #readRequestBody(HttpServletRequest)}, penyambungan
	 * dilakukan tanpa {@code "\n"} sehingga pemisah baris pada tanggapan penyedia hilang. Untuk JSON
	 * hal ini tidak berpengaruh pada penguraian, <b>namun</b> teks jawaban AI di dalam field
	 * {@code content} tidak terdampak karena baris barunya sudah berupa escape {@code \n} di dalam
	 * string JSON, bukan baris baru sungguhan &mdash; sehingga paragraf pada hasil AI tetap utuh.</p>
	 *
	 * <p><b>Tidak ada batas ukuran.</b> Tanggapan sebesar apa pun diserap ke memori.</p>
	 *
	 * @param inputStream aliran yang akan dibaca; boleh {@code null}
	 * @return isi aliran sebagai string; kosong bila {@code null} atau memang kosong
	 * @throws IOException bila pembacaan aliran gagal
	 */
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

	/**
	 * Menulis tanggapan JSON bertanda gagal ke klien.
	 *
	 * <p><b>Cara kerja.</b> Mencatat rincian ke log lalu merangkai objek JSON berisi
	 * {@code success:false}, {@code provider} (literal tetap {@code "openai-compatible"}),
	 * {@code model}, {@code endpoint}, {@code error} (pesan yang dapat dibaca manusia), dan
	 * {@code raw} (badan tanggapan mentah dari penyedia, atau string kosong bila {@code null}).
	 * Seluruh nilai teks dilewatkan {@link #escapeJson(String)}.</p>
	 *
	 * <p><b>Kode status HTTP tetap 200.</b> Method ini tidak memanggil {@code setStatus}, sehingga
	 * kegagalan tetap dikirim sebagai HTTP 200 dengan {@code success:false}. Klien JSP karenanya
	 * <b>wajib</b> memeriksa field {@code success}, bukan kode status. Bila kelak diubah menjadi 4xx/5xx,
	 * seluruh JSP pemanggil harus disesuaikan bersamaan.</p>
	 *
	 * <p><b>Peringatan kebocoran informasi.</b> Field {@code raw} memantulkan badan galat penyedia
	 * <b>apa adanya</b> ke pemanggil, dan {@code endpoint} membocorkan alamat endpoint AI internal
	 * (termasuk IP/porta Ollama lokal). Karena {@code /Ai} terbuka anonim (lihat Javadoc kelas),
	 * pihak luar dapat memancing galat untuk memetakan konfigurasi internal, kuota, dan identitas
	 * proyek penyedia. Kunci API sendiri tidak ikut tercetak, tetapi pesan galat penyedia adakalanya
	 * memuat potongan pengenal kunci atau nama proyek. Bila hendak diperketat, sembunyikan {@code raw}
	 * dan {@code endpoint} untuk pemanggil non-admin.</p>
	 *
	 * @param response tanggapan HTTP yang akan ditulisi
	 * @param message pesan galat yang dapat dibaca manusia
	 * @param rawResponse badan tanggapan mentah dari penyedia; boleh {@code null}
	 * @param endpoint URL endpoint yang sedang dituju, untuk diagnosis
	 * @param model nama model yang sedang dipakai, untuk diagnosis
	 * @throws IOException bila penulisan respons gagal
	 */
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

	/**
	 * Mengembalikan argumen pertama dari enam yang tidak kosong.
	 *
	 * <p><b>Tujuan.</b> Menyatakan urutan prioritas sumber nilai secara ringkas. Dipakai
	 * {@link #doPost} untuk mencari prompt (JSON {@code instruksi}, {@code prompt}, {@code text},
	 * lalu parameter form dengan tiga nama yang sama) dan untuk mencari {@code maxTokens}
	 * (JSON {@code maxTokens}, {@code max_tokens}, lalu dua parameter form senama, dengan dua slot
	 * terakhir diisi {@code null} sebagai pengisi).</p>
	 *
	 * <p><b>Cara kerja.</b> Enam pemeriksaan {@link #isBlank(String)} berurutan; yang pertama gagal
	 * blank langsung dikembalikan. Bila semuanya kosong, dikembalikan string kosong &mdash;
	 * <b>bukan</b> {@code null}, sehingga pemanggil tidak perlu memeriksa {@code null}. Nilai
	 * baliknya tidak di-{@code trim()}; perapian dilakukan pemanggil bila perlu.</p>
	 *
	 * <p><b>Catatan.</b> Seluruh argumen dievaluasi sebelum method dipanggil (Java tidak punya
	 * evaluasi malas untuk argumen), jadi jangan mengisinya dengan ekspresi yang mahal atau
	 * ber-efek-samping dengan harapan "hanya dipakai bila diperlukan".</p>
	 *
	 * @param a kandidat prioritas pertama
	 * @param b kandidat prioritas kedua
	 * @param c kandidat prioritas ketiga
	 * @param d kandidat prioritas keempat
	 * @param e kandidat prioritas kelima
	 * @param f kandidat prioritas keenam
	 * @return kandidat pertama yang tidak {@code null} dan tidak hanya spasi; string kosong bila
	 *         tidak ada
	 */
	private String firstNotEmpty(String a, String b, String c, String d, String e, String f) {
		if (!isBlank(a)) return a;
		if (!isBlank(b)) return b;
		if (!isBlank(c)) return c;
		if (!isBlank(d)) return d;
		if (!isBlank(e)) return e;
		if (!isBlank(f)) return f;
		return "";
	}

	/**
	 * Mengembalikan argumen pertama dari dua yang tidak kosong.
	 *
	 * <p><b>Tujuan.</b> Varian dua argumen dari
	 * {@link #firstNotEmpty(String, String, String, String, String, String)}, dipakai
	 * {@link #doPost} untuk memilih nilai {@code model} dan {@code temperature} antara field JSON
	 * dan parameter form.</p>
	 *
	 * <p><b>Cara kerja.</b> Dua pemeriksaan {@link #isBlank(String)}; bila keduanya kosong,
	 * dikembalikan string kosong, bukan {@code null}.</p>
	 *
	 * @param a kandidat prioritas pertama
	 * @param b kandidat prioritas kedua
	 * @return kandidat pertama yang tidak {@code null} dan tidak hanya spasi; string kosong bila
	 *         tidak ada
	 */
	private String firstNotEmpty(String a, String b) {
		if (!isBlank(a)) return a;
		if (!isBlank(b)) return b;
		return "";
	}

	/**
	 * Memeriksa apakah sebuah string dianggap "kosong".
	 *
	 * <p><b>Cara kerja.</b> Mengembalikan {@code true} bila argumennya {@code null} <b>atau</b>
	 * panjangnya nol setelah {@code trim()} &mdash; yaitu string yang hanya berisi spasi juga
	 * dianggap kosong. Predikat inilah dasar seluruh keputusan "pakai nilai ini atau jatuh ke bawaan"
	 * di berkas ini.</p>
	 *
	 * <p><b>Catatan.</b> {@code trim()} pada Java 7 hanya membuang karakter dengan kode &le; U+0020,
	 * sehingga spasi non-ASCII seperti U+00A0 (<i>non-breaking space</i>) <b>tidak</b> dianggap
	 * kosong. Nilai konfigurasi hasil salin-tempel dari dokumen bisa mengandung karakter semacam itu
	 * dan akan lolos sebagai "terisi".</p>
	 *
	 * @param value string yang diperiksa; boleh {@code null}
	 * @return {@code true} bila {@code null} atau kosong/hanya spasi
	 */
	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	/**
	 * Menguraikan teks menjadi {@code double} dengan nilai cadangan bila gagal.
	 *
	 * <p><b>Cara kerja.</b> Bila argumennya kosong menurut {@link #isBlank(String)}, langsung
	 * mengembalikan {@code defaultValue}. Selain itu mencoba {@code Double.parseDouble(value.trim())};
	 * setiap kegagalan dicatat lewat {@link #debugException(String, Exception)} dan juga berujung pada
	 * {@code defaultValue}. Dengan kata lain method ini <b>tidak pernah melempar</b>.</p>
	 *
	 * <p><b>Dipakai untuk tiga hal</b> yang sebenarnya bertipe berbeda: {@code temperature} (memang
	 * pecahan), serta {@code AI_TIMEOUT_MS}, {@code AI_NUM_PREDICT}, dan {@code maxTokens} yang
	 * bilangan bulat lalu di-<i>cast</i> ke {@code int} oleh pemanggil. Pemakaian {@code double}
	 * sebagai tipe perantara membuat nilai konfigurasi seperti {@code "700.9"} diterima dan dipotong
	 * menjadi 700 &mdash; longgar, tetapi tidak menimbulkan galat. Perlu diingat pula bahwa nilai di
	 * atas {@link Integer#MAX_VALUE} akan mengalami <i>overflow</i> saat di-cast; isi konfigurasi
	 * dengan angka yang wajar.</p>
	 *
	 * @param value teks yang diuraikan; boleh {@code null} atau kosong
	 * @param defaultValue nilai yang dikembalikan bila kosong atau tidak dapat diuraikan
	 * @return hasil penguraian, atau {@code defaultValue}
	 */
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

	/**
	 * Memformat sebuah {@code double} menjadi teks yang aman disisipkan ke payload JSON.
	 *
	 * <p><b>Tujuan.</b> Mencegah notasi ilmiah masuk ke JSON. {@code String.valueOf(1.0E-4)}
	 * menghasilkan {@code "1.0E-4"} &mdash; bentuk yang sebenarnya sah menurut spesifikasi JSON,
	 * namun tidak selalu diterima mulus oleh setiap penyedia OpenAI-compatible. Method ini memilih
	 * bermain aman.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengubah nilai menjadi teks lewat {@code String.valueOf}; bila hasilnya
	 * memuat huruf {@code E} atau {@code e}, dikembalikan literal {@code "0.4"} &mdash; yaitu nilai
	 * temperature bawaan. Selain itu teks dikembalikan apa adanya.</p>
	 *
	 * <p><b>Perhatikan konsekuensinya.</b> Ini bukan pemformatan, melainkan <b>penggantian senyap</b>:
	 * temperature yang sangat kecil (mis. {@code 0.00001}, yang tercetak sebagai {@code 1.0E-5})
	 * diam-diam berubah menjadi {@code 0.4} &mdash; hampir kebalikan dari maksud pengguna, tanpa
	 * peringatan apa pun. Rentang temperature yang lazim (0,0&ndash;2,0) tidak pernah tercetak dalam
	 * notasi ilmiah kecuali sangat mendekati nol, sehingga dalam praktik jarang terpicu. Bila kelak
	 * diperbaiki, pakai {@code BigDecimal.toPlainString()} atau {@code String.format(Locale.US, ...)}
	 * &mdash; catat pemakaian {@code Locale.US}, sebab locale Indonesia memakai koma sebagai pemisah
	 * desimal dan akan merusak JSON.</p>
	 *
	 * @param value nilai yang diformat
	 * @return representasi desimal biasa, atau {@code "0.4"} bila hasilnya bernotasi ilmiah
	 */
	private String formatDouble(double value) {
		String s = String.valueOf(value);
		if (s.indexOf('E') >= 0 || s.indexOf('e') >= 0) {
			return "0.4";
		}
		return s;
	}

	/**
	 * Mengambil nilai sebuah field dari teks JSON dengan pemindaian sederhana.
	 *
	 * <p><b>Tujuan.</b> Menghindari ketergantungan pustaka JSON untuk dua kebutuhan sempit: membaca
	 * beberapa field dari badan permintaan ({@link #doPost}) dan mengambil teks hasil dari tanggapan
	 * penyedia ({@link #parseOpenAiContent(String)}).</p>
	 *
	 * <p><b>Cara kerja.</b> (1) Mencari kemunculan <b>pertama</b> pola <code>"&lt;key&gt;"</code>
	 * (nama field lengkap dengan tanda kutip); (2) mencari tanda titik dua sesudahnya; (3) melompati
	 * spasi; (4) bila karakter pertama nilai adalah tanda kutip, penguraian string diserahkan ke
	 * {@link #parseJsonString(String, int)} yang menangani karakter escape; (5) selain itu nilai
	 * dibaca apa adanya sampai bertemu {@code ,}, <code>}</code>, atau <code>]</code>, lalu
	 * di-{@code trim()}. Mengembalikan string kosong bila argumen {@code null} atau field tidak
	 * ditemukan.</p>
	 *
	 * <p><b>Keterbatasan yang WAJIB diketahui sebelum menambah pemakaian.</b></p>
	 * <ul>
	 *   <li><b>Tidak sadar struktur.</b> Yang dicari adalah teks <code>"key"</code> di mana pun,
	 *   termasuk di dalam nilai string milik field lain atau di kedalaman objek bersarang mana pun.
	 *   Badan permintaan yang prompt-nya kebetulan memuat teks <code>"model"</code> berikut titik dua
	 *   dapat membuat field {@code model} terbaca keliru &mdash; asalkan kemunculannya lebih awal.</li>
	 *   <li><b>Tidak dapat membaca objek atau larik.</b> Nilai berupa <code>{...}</code> atau
	 *   <code>[...]</code> akan terpotong pada tanda kurung/koma pertama.</li>
	 *   <li><b>Hanya kemunculan pertama.</b> Field dengan nama sama pada beberapa kandidat jawaban
	 *   hanya terbaca yang pertama.</li>
	 * </ul>
	 * <p>Untuk kebutuhan yang lebih ketat, pakai {@code org.json} yang sudah tersedia di classpath
	 * (dipakai antara lain oleh {@code MServet}).</p>
	 *
	 * @param json teks JSON yang dipindai; boleh {@code null}
	 * @param key nama field yang dicari, tanpa tanda kutip; boleh {@code null}
	 * @return nilai field sebagai teks, atau string kosong bila tidak ditemukan
	 */
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

	/**
	 * Menguraikan sebuah string JSON berkutip mulai dari posisi tanda kutip pembuka, sekaligus
	 * menerjemahkan karakter escape-nya.
	 *
	 * <p><b>Cara kerja.</b> Menelusuri karakter demi karakter mulai satu posisi sesudah
	 * {@code quoteIndex}, sambil menjaga bendera {@code escaping}. Saat bertemu garis miring terbalik,
	 * bendera dinyalakan; karakter berikutnya diterjemahkan: <code>\"</code>, <code>\\</code>, dan
	 * <code>\/</code> menjadi karakter aslinya; {@code \b}, {@code \f}, {@code \n}, {@code \r},
	 * {@code \t} menjadi karakter kendali yang sesuai; {@code \\uXXXX} diuraikan sebagai heksadesimal
	 * empat digit menjadi satu {@code char}. Tanda kutip yang tidak sedang di-escape mengakhiri
	 * penguraian dan hasilnya dikembalikan.</p>
	 *
	 * <p><b>Perilaku pada masukan tak lazim.</b> (a) Escape yang tidak dikenal (mis. {@code \q})
	 * menghasilkan karakter itu sendiri tanpa garis miring &mdash; lebih longgar daripada spesifikasi
	 * JSON, yang seharusnya menolak; (b) urutan {@code \\u} yang heksadesimalnya tidak valid
	 * dikembalikan sebagai teks literal <code>\\uXXXX</code>, dan penunjuk tetap dimajukan empat
	 * posisi sehingga penguraian berlanjut rapi; (c) bila tanda kutip penutup tidak pernah ditemukan
	 * (JSON terpotong), seluruh sisa teks dikembalikan alih-alih melempar &mdash; pilihan yang
	 * disengaja agar jawaban AI yang terpotong tetap sebagian dapat dipakai; (d) syarat
	 * {@code i + 4 < json.length()} pada penanganan {@code \\u} bersifat sedikit terlalu ketat: bila
	 * empat digit heksadesimal berada tepat di ujung teks tanpa karakter sesudahnya, ia tidak
	 * diuraikan melainkan dianggap escape tak dikenal.</p>
	 *
	 * <p><b>Catatan Unicode.</b> Hasil {@code Integer.parseInt(hex, 16)} di-<i>cast</i> ke satu
	 * {@code char}, sehingga pasangan pengganti (<i>surrogate pair</i>) untuk karakter di luar BMP
	 * &mdash; emoji, misalnya &mdash; terbentuk secara benar hanya karena penyedia mengirimkannya
	 * sebagai dua urutan {@code \\uXXXX} berturut-turut, dan keduanya tersalin apa adanya ke
	 * {@link StringBuilder}.</p>
	 *
	 * @param json teks JSON yang memuat string tersebut
	 * @param quoteIndex indeks tanda kutip pembuka
	 * @return isi string yang sudah diterjemahkan escape-nya, tanpa tanda kutip
	 */
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

	/**
	 * Meng-escape sebuah teks agar aman menjadi isi string JSON.
	 *
	 * <p><b>Peran keamanan.</b> Inilah satu-satunya pelindung integritas seluruh JSON yang dirangkai
	 * manual di berkas ini &mdash; baik payload keluar ({@link #buildOpenAiPayload}) maupun tanggapan
	 * ke klien ({@link #doGet}, {@link #doPost}, {@link #writeError}). Karena prompt berasal dari
	 * pemanggil, tanpa method ini sebuah prompt yang memuat tanda kutip sudah cukup untuk merusak
	 * payload, dan yang disusun dengan sengaja dapat menyisipkan pesan {@code system} tambahan ke
	 * dalam larik {@code messages}. <b>Setiap nilai teks yang disisipkan ke JSON di berkas ini WAJIB
	 * melewati method ini.</b></p>
	 *
	 * <p><b>Cara kerja.</b> Mengembalikan string kosong bila argumennya {@code null}. Selebihnya
	 * menyalin karakter demi karakter: <code>"</code> menjadi <code>\"</code>, garis miring terbalik
	 * digandakan, {@code \b} {@code \f} {@code \n} {@code \r} {@code \t} diganti escape pendeknya,
	 * dan setiap karakter kendali lain di bawah kode 32 diubah menjadi <code>\\u00XX</code> dengan
	 * pengisian nol sampai empat digit. Karakter selain itu disalin apa adanya.</p>
	 *
	 * <p><b>Catatan.</b> Karakter non-ASCII (huruf beraksen, aksara Arab, emoji) sengaja
	 * <b>tidak</b> di-escape &mdash; sah menurut spesifikasi JSON selama dokumennya dikirim sebagai
	 * UTF-8, dan itulah yang dilakukan {@link #doPost} lewat header {@code charset=UTF-8} serta
	 * konstanta {@link #UTF_8}. Perlu diketahui pula bahwa {@code U+2028}/{@code U+2029} tidak
	 * di-escape; keduanya sah di JSON tetapi dapat menyulitkan bila hasilnya ditanam langsung ke
	 * dalam blok JavaScript pada halaman.</p>
	 *
	 * @param value teks yang di-escape; boleh {@code null}
	 * @return teks yang aman dipakai sebagai isi string JSON; tidak pernah {@code null}
	 */
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

	/**
	 * Menghitung panjang sebuah string dengan aman terhadap {@code null}.
	 *
	 * <p><b>Tujuan.</b> Dipakai khusus di dalam rangkaian pesan {@link #debugLog(String)} agar
	 * pencatatan panjang tidak pernah melempar {@link NullPointerException} &mdash; penting karena
	 * log tidak boleh menjadi penyebab kegagalan.</p>
	 *
	 * @param value string yang diukur; boleh {@code null}
	 * @return panjang string, atau {@code 0} bila {@code null}
	 */
	private int lengthOf(String value) {
		return value == null ? 0 : value.length();
	}

	/**
	 * Meringkas string menjadi cuplikan satu baris untuk keperluan log.
	 *
	 * <p><b>Cara kerja.</b> Mengembalikan string kosong bila argumennya {@code null}. Selebihnya
	 * mengganti setiap {@code \r} dan {@code \n} dengan spasi &mdash; agar satu entri log tetap satu
	 * baris dan mudah ditelusuri {@code grep} &mdash; lalu memotongnya pada {@code max} karakter dan
	 * menambahkan {@code "..."} bila melebihi.</p>
	 *
	 * <p><b>Catatan.</b> Ini semata pemangkas tampilan, <b>bukan</b> penyaring data sensitif: isi
	 * cuplikan tetap apa adanya. Pemangkasannya juga berbasis {@code char}, sehingga secara teori
	 * dapat memotong tepat di tengah pasangan pengganti Unicode dan menghasilkan satu karakter
	 * rusak di ujung cuplikan &mdash; tidak berdampak apa pun selain tampilan log.</p>
	 *
	 * @param value teks yang diringkas; boleh {@code null}
	 * @param max jumlah karakter maksimum sebelum dipotong
	 * @return cuplikan satu baris, dengan akhiran {@code "..."} bila dipotong
	 */
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

	/**
	 * Mengubah {@code null} menjadi string kosong.
	 *
	 * <p><b>Tujuan.</b> Dipakai {@link #doPost} saat mencatat
	 * {@code conn.getResponseMessage()}, yang memang boleh mengembalikan {@code null}. Menjaga agar
	 * penyambungan string pada log tidak menghasilkan teks {@code "null"} yang membingungkan.</p>
	 *
	 * @param value teks yang diperiksa; boleh {@code null}
	 * @return teks aslinya, atau string kosong bila {@code null}
	 */
	private String safe(String value) {
		return value == null ? "" : value;
	}

	/**
	 * Menyamarkan nilai konfigurasi yang tergolong rahasia sebelum dicatat ke log.
	 *
	 * <p><b>Cara kerja.</b> Bila nama kunci (huruf kecil) memuat substring {@code "key"}, nilainya
	 * diganti {@code "***MASKED***"} &mdash; atau string kosong bila memang kosong, sehingga log tetap
	 * membedakan "ada tapi disamarkan" dari "belum diisi". Kunci lain dikembalikan apa adanya.</p>
	 *
	 * <p><b>Cakupan saat ini memadai</b> untuk seluruh kunci rahasia di berkas ini, sebab semuanya
	 * berakhiran {@code _KEY}: {@code AI_GEMINI_KEY}, {@code AI_GROQ_KEY},
	 * {@code AI_CLOUDFLARE_KEY}, {@code AI_OPENAI_CLOUD_KEY}, {@code AI_DEEPSEEK_KEY}, dan
	 * {@code AI_OPENAI_KEY}.</p>
	 *
	 * <p><b>Keterbatasan yang perlu diingat saat menambah konfigurasi baru.</b> Penyaringnya hanya
	 * mengenali kata {@code "key"}. Kunci rahasia yang dinamai dengan kata lain &mdash;
	 * {@code TOKEN}, {@code SECRET}, {@code PASSWORD}, {@code CREDENTIAL} &mdash; akan
	 * <b>tercetak utuh</b> ke log. Bila menambahkan konfigurasi rahasia, gunakan nama berakhiran
	 * {@code _KEY} atau perluas daftar kata di method ini. Perlu dicatat pula bahwa penyamaran ini
	 * hanya berlaku untuk log {@link #getConfigValue(String, String)}; nilai yang sama tetap dipasang
	 * utuh pada header {@code Authorization} (yang memang tidak dicatat) dan, bila endpoint dialihkan
	 * ke host pihak ketiga lewat {@code AI_OPENAI_URL}, tetap terkirim ke sana.</p>
	 *
	 * @param key nama kunci konfigurasi; boleh {@code null}
	 * @param value nilai yang hendak dicatat
	 * @return {@code "***MASKED***"} bila kunci tergolong rahasia dan nilainya terisi; string kosong
	 *         bila rahasia tetapi kosong; selain itu nilai aslinya
	 */
	private String maskIfSecret(String key, String value) {
		if (key != null && key.toLowerCase().indexOf("key") >= 0) {
			return isBlank(value) ? "" : "***MASKED***";
		}
		return value;
	}

	/**
	 * Menutup sebuah {@link OutputStream} tanpa memunculkan galat baru.
	 *
	 * <p><b>Tujuan.</b> Dipanggil dari blok {@code finally} di {@link #doPost} dan
	 * {@link #callAiInternal(String, int)} setelah payload dikirim. Kegagalan menutup aliran tidak
	 * boleh menutupi exception yang sedang merambat naik &mdash; itulah sebabnya galatnya hanya
	 * dicatat lewat {@link #debugException(String, Exception)} dan tidak dilempar ulang.</p>
	 *
	 * <p><b>Cara kerja.</b> Argumen {@code null} diabaikan (kondisi normal bila
	 * {@code conn.getOutputStream()} sendiri yang gagal), selebihnya {@code close()} dibungkus
	 * {@code try/catch}.</p>
	 *
	 * @param os aliran yang ditutup; boleh {@code null}
	 */
	private void closeQuietly(OutputStream os) {
		if (os != null) {
			try {
				os.close();
			} catch (Exception e) {
				debugException("closeQuietly(OutputStream) gagal.", e);
			}
		}
	}

	/**
	 * Mencetak satu baris log detail bila {@link #debug} sedang aktif.
	 *
	 * <p><b>Cara kerja.</b> Bila {@link #debug} bernilai {@code true}, mencetak pesan ke
	 * {@code System.out} dengan awalan tetap {@code "[AiGenerateServlet] "} agar mudah disaring dari
	 * log kontainer. Bila tidak, tidak melakukan apa pun.</p>
	 *
	 * <p><b>Catatan kinerja.</b> Penyaringan terjadi <b>di dalam</b> method, sedangkan penyambungan
	 * string pada argumen sudah dikerjakan pemanggil sebelum method dipanggil. Jadi biaya merangkai
	 * pesan &mdash; termasuk {@link #shortText(String, int)} atas payload dan tanggapan berukuran
	 * ribuan karakter &mdash; tetap dibayar meski log sedang dimatikan.</p>
	 *
	 * <p><b>Peringatan privasi.</b> Pesan-pesan yang dikirim ke sini memuat prompt dan tanggapan AI
	 * secara utuh (hanya dipangkas panjangnya). Bila prompt memuat data mahasiswa atau materi
	 * internal, data itu ikut tersimpan di berkas log server. Matikan lewat konfigurasi
	 * {@code AI_DEBUG} pada lingkungan produksi.</p>
	 *
	 * @param message pesan yang dicatat
	 */
	private void debugLog(String message) {
		if (debug) {
			System.out.println("[AiGenerateServlet] " + message);
		}
	}

	/**
	 * Mencetak pesan beserta <i>stack trace</i> sebuah exception bila {@link #debug} sedang aktif.
	 *
	 * <p><b>Cara kerja.</b> Bila {@link #debug} bernilai {@code true}, mencetak pesan dengan awalan
	 * {@code "[AiGenerateServlet] "} lalu &mdash; bila exception-nya tidak {@code null} &mdash;
	 * mencetak jejak tumpukannya ke {@code System.out}. Pemakaian {@code System.out}, bukan
	 * {@code System.err}, disengaja agar seluruh keluaran berkas ini berurutan pada satu aliran log.</p>
	 *
	 * <p><b>Perhatian.</b> Ketika {@link #debug} dimatikan, exception yang dilaporkan ke sini
	 * <b>hilang sepenuhnya</b> &mdash; berbeda dari sisa basis kode AIS yang memakai
	 * {@code ais.common.ErrorAuditUtil.record(...)} sehingga galat tetap terekam terlepas dari
	 * sakelar log. Akibatnya, mematikan {@code AI_DEBUG} di produksi juga mematikan seluruh jejak
	 * kegagalan pembacaan konfigurasi, penguraian angka, dan penutupan aliran di berkas ini. Bila
	 * kelak disentuh, pertimbangkan menambahkan {@code ErrorAuditUtil.record} di luar penjaga
	 * {@code debug}.</p>
	 *
	 * @param message pesan konteks yang menjelaskan di mana kegagalan terjadi
	 * @param e exception yang dicatat; boleh {@code null}
	 */
	private void debugException(String message, Exception e) {
		if (debug) {
			System.out.println("[AiGenerateServlet] " + message);
			if (e != null) {
				e.printStackTrace(System.out);
			}
		}
	}
}
