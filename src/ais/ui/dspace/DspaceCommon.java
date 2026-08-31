package ais.ui.dspace;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.URLCommon;

/**
 * Utilitas integrasi AIS eCampus dengan <b>DSpace</b> — perangkat lunak repositori institusional
 * sumber terbuka yang lazim dipakai kampus untuk menyimpan dan mempublikasikan koleksi digital
 * seperti skripsi, tesis, disertasi, dan jurnal ilmiah. Kelas ini membungkus REST API DSpace
 * (autentikasi berbasis cookie {@code JSESSIONID}, unggah berkas sebagai <i>bitstream</i> pada
 * suatu <i>item</i>, dan pengecekan status layanan) memakai {@link java.net.HttpURLConnection}
 * mentah — tanpa pustaka klien HTTP tingkat tinggi.
 *
 * <h2>Dua jalur login yang hidup berdampingan</h2>
 * <p>
 * Kelas ini memiliki DUA implementasi login yang tumpang tindih secara fungsional namun berbeda
 * kematangan:
 * </p>
 * <ul>
 * <li>{@link #logintest()} — jalur LAMA/uji coba: kredensial DAN alamat REST DITULIS LANGSUNG di
 * kode ({@link #USERNAME}, {@link #PASSWORD}, {@link #REST_URL}), tidak membaca konfigurasi
 * runtime, dan tidak menangani skenario proxy/Cloudflare. Dipertahankan bersama {@link
 * #main(String[])} sebagai peninggalan pengujian manual — lihat peringatan keamanan di bawah.</li>
 * <li>{@link #login()} — jalur PRODUKSI: kredensial dibaca dari konfigurasi runtime
 * ({@code dspace_username}/{@code dspace_password} lewat {@code Common.getKonfigurasi}), dan
 * alamat REST dicoba berurutan (endpoint internal lebih dulu, baru endpoint publik) untuk mengatasi
 * kasus proteksi Cloudflare/WAF yang memblokir permintaan server-ke-server ke endpoint publik. Bila
 * kedua alamat gagal karena pemblokiran WAF, endpoint yang akhirnya berhasil dipakai disimpan
 * kembali ke {@link ais.common.ConstantValues#DSPACE_URL_PRIVATE} agar operasi DSpace berikutnya
 * (mis. {@link #upload}) memakai endpoint yang sama dengan endpoint login yang berhasil — penting
 * karena cookie sesi {@code JSESSIONID} hanya valid untuk host yang menerbitkannya.</li>
 * </ul>
 *
 * <h2>Alur unggah berkas</h2>
 * <p>
 * {@link #upload(String, String, String, String)} mengirim satu berkas sebagai <i>bitstream</i> ke
 * item DSpace tertentu (diidentifikasi lewat UUID) memakai request {@code multipart/form-data}
 * yang disusun manual (boundary dari heksadesimal timestamp, header per-bagian ditulis lewat
 * {@link java.io.PrintWriter}, isi biner disalin langsung ke {@link java.io.OutputStream} lewat
 * {@link java.nio.file.Files#copy}). Nama berkas dan deskripsi disertakan sebagai parameter query
 * pada URL endpoint {@code /items/{uuid}/bitstreams}, bukan sebagai bagian multipart — mengikuti
 * konvensi REST API DSpace versi lama (DSpace 5/6 REST API klasik).
 * </p>
 *
 * <h2>Penanganan galat berorientasi pesan pengguna</h2>
 * <p>
 * {@link #pesanKoneksi(Exception)} menerjemahkan pengecualian teknis (terutama pemblokiran
 * Cloudflare/WAF yang terdeteksi lewat {@link ais.common.URLCommon#isWafForbidden(Exception)})
 * menjadi pesan berbahasa Indonesia yang actionable bagi administrator kampus, mengarahkan mereka
 * untuk mengisi konfigurasi {@code dspace_private_url_internal} — pola ini konsisten dengan
 * penanganan galat di {@link #login()}.
 * </p>
 *
 * @see ais.common.ConstantValues#DSPACE_URL_PRIVATE
 * @see ais.common.URLCommon
 */
public class DspaceCommon {

	/**
	 * Alamat dasar REST API DSpace yang dipakai oleh jalur uji coba {@link #logintest()}/
	 * {@link #upload}/{@link #main(String[])} — <b>bukan</b> yang dipakai jalur produksi
	 * {@link #login()}/{@link #status(String)}, yang membaca alamat dari
	 * {@link ais.common.ConstantValues#DSPACE_URL_PRIVATE} (konfigurasi runtime).
	 */
	public static String REST_URL = "http://demo.ecampus.id/rest";
	/**
	 * Username DSpace tertanam langsung di kode sumber, dipakai HANYA oleh jalur uji coba
	 * {@link #logintest()}. Lihat peringatan keamanan pada javadoc kelas — nilai ini adalah
	 * kredensial nyata (alamat email), bukan placeholder.
	 */
	private static String USERNAME = "fauzioke2003@gmail.com";
	/**
	 * Password DSpace tertanam langsung di kode sumber, dipakai HANYA oleh jalur uji coba
	 * {@link #logintest()}. Lihat peringatan keamanan pada javadoc kelas.
	 */
	private static String PASSWORD = "jangannakal";

	/**
	 * Jalur login UJI COBA/PENINGGALAN ke DSpace: memakai {@link #REST_URL} dan kredensial
	 * tertanam ({@link #USERNAME}/{@link #PASSWORD}) alih-alih konfigurasi runtime. Mem-POST
	 * {@code email}+{@code password} ke {@code /login}, lalu mem-parsing header respons
	 * {@code Set-Cookie} secara naif (memisah pada karakter {@code "="} pertama, mengambil bagian
	 * sebelum {@code ";"}) untuk mendapatkan nilai cookie sesi. Untuk jalur produksi yang membaca
	 * kredensial dari konfigurasi dan menangani multi-endpoint, gunakan {@link #login()}.
	 *
	 * @return nilai cookie sesi DSpace hasil login
	 * @throws Exception bila server tidak mengembalikan cookie {@code Set-Cookie} sama sekali
	 *                    (kredensial salah atau endpoint tidak dapat diakses), atau kegagalan
	 *                    jaringan lain yang diteruskan dari {@link URLCommon#getPostResponseHeader}
	 */
	public static String logintest() throws Exception {

		String postData = "email=" + USERNAME + "&password=" + PASSWORD;

		String urlStr = REST_URL + "/login";

		String cookie = "";
		Map<String, List<String>> map = URLCommon.getPostResponseHeader(urlStr, postData);
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue() + "";
			if (key != null && key.equalsIgnoreCase("Set-Cookie")) {
				System.out.println("value = " + value);
				String[] bagianCookie = StringUtils.split(value, "=");
				if (bagianCookie != null && bagianCookie.length > 1 && bagianCookie[1] != null) {
					cookie = bagianCookie[1].split(";")[0];
				}
			}
		}

		if (cookie == null || cookie.trim().length() == 0) {
			throw new Exception("Login DSpace gagal: server tidak mengirim cookie sesi. Periksa username/password dan akses DSpace.");
		}
		System.out.println("cookie = " + cookie);
		return cookie;

	}

	/**
	 * Mengunggah satu berkas lokal sebagai <i>bitstream</i> baru pada item DSpace {@code uuid}, lewat
	 * {@code POST /items/{uuid}/bitstreams} berformat {@code multipart/form-data} yang disusun manual
	 * (boundary unik dari heksadesimal waktu saat ini, header/isi bagian ditulis langsung ke stream
	 * koneksi). Nama berkas dan deskripsi dikirim sebagai parameter query URL (ter-encode UTF-8),
	 * sedangkan isi biner berkas disalin langsung dari disk ke {@link java.io.OutputStream} koneksi
	 * lewat {@link java.nio.file.Files#copy(java.nio.file.Path, java.io.OutputStream)} tanpa
	 * dibuffer penuh di memori.
	 *
	 * <p>
	 * Kode status HTTP dan isi respons dicetak ke {@code System.out} untuk keperluan diagnosis;
	 * method tidak mengembalikan nilai maupun memvalidasi kode status — pemanggil yang perlu
	 * memastikan sukses harus memeriksa log atau memodifikasi method ini.
	 * </p>
	 *
	 * @param cookie      cookie sesi {@code JSESSIONID} hasil {@link #login()}/{@link #logintest()}
	 * @param uuid        UUID item DSpace tujuan unggahan
	 * @param filepath    path berkas lokal yang akan diunggah
	 * @param description deskripsi bitstream, disisipkan sebagai parameter query {@code description}
	 * @throws Exception diteruskan dari kegagalan koneksi HTTP, penulisan multipart, atau pembacaan
	 *                    berkas lokal
	 */
	public static void upload(String cookie, String uuid, String filepath, String description) throws Exception {
		File binaryFile = new File(filepath);

		String charset = "UTF-8";
		String param = "data";
		String boundary = Long.toHexString(System.currentTimeMillis()); // Just
																		// generate
																		// some
																		// unique
																		// random
																		// value.
		String CRLF = "\r\n"; // Line separator required by multipart/form-data.

		URL url = new URL(
				REST_URL + "/items/" + uuid + "/bitstreams?name=" + URLEncoder.encode(binaryFile.getName(), charset)
						+ "&description=" + URLEncoder.encode(description, charset));
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// CURLOPT_POST
		con.setRequestMethod("POST");

		// CURLOPT_FOLLOWLOCATION
		con.setInstanceFollowRedirects(true);
		con.setRequestProperty("Cookie", "JSESSIONID=" + cookie);
		con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		con.setRequestProperty("Accept", "application/json");
		con.setDoOutput(true);
		con.setDoInput(true);

		OutputStream output = con.getOutputStream();
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
		// Send normal param.
		writer.append("--" + boundary).append(CRLF);
		writer.append("Content-Disposition: form-data; name=\"param\"").append(CRLF);
		writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		writer.append(CRLF).append(param).append(CRLF).flush();

		// Send binary file.
		writer.append("--" + boundary).append(CRLF);
		writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + binaryFile.getName() + "\"")
				.append(CRLF);
		writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
		writer.append("Content-Transfer-Encoding: binary").append(CRLF);
		writer.append(CRLF).flush();
		Files.copy(binaryFile.toPath(), output);
		output.flush(); // Important before continuing with writer!
		writer.append(CRLF).flush(); // CRLF is important! It indicates end
										// of boundary.

		// End of multipart/form-data.
		writer.append("--" + boundary + "--").append(CRLF).flush();

		// Request is lazily fired whenever you need to obtain information about
		// response.
		int responseCode = ((HttpURLConnection) con).getResponseCode();
		System.out.println(responseCode); // Should be 200

		DataInputStream input = new DataInputStream(con.getInputStream());
		int c;
		StringBuilder resultBuf = new StringBuilder();
		while ((c = input.read()) != -1) {
			resultBuf.append((char) c);
		}
		input.close();

		System.out.println("resultBuf = " + resultBuf.toString());
	}

	/**
	 * Titik masuk uji coba manual (dijalankan langsung dari IDE/command line, bukan dipanggil oleh
	 * aplikasi) untuk memverifikasi jalur {@link #logintest()} + {@link #upload} terhadap server
	 * DSpace di {@link #REST_URL}. Path berkas dan UUID item pada isi method ini bersifat contoh
	 * spesifik-mesin developer, bukan konfigurasi umum.
	 *
	 * @param argv tidak dipakai
	 * @throws Exception diteruskan dari {@link #logintest()}/{@link #upload}
	 */
	public static void main(String[] argv) throws Exception {
		String cookie = logintest();
		upload(cookie, "e9adbe28-d7b9-42df-a913-7f5c69b42851", "C:\\opt\\background.png", "test deskrips file yaaa");
		// status(cookie);
		// saveCommunities(cookie);
		// System.out.println(jsonObject);
		// collections();

		// URLCommon.cek("communities/f0279636-4804-4be2-afb6-32c2981548b3");

		// String postData = "{\"name\":\"FAUZI TEST OK1\"," +
		// "\"copyrightText\":\"FAUZI TEST OK\","
		// + "\"introductoryText\":\"FAUZI TEST OK\"," +
		// "\"shortDescription\":\"FAUZI TEST OK\","
		// + "\"shortDescription\":\"FAUZI TEST OK\"," +
		// "\"sidebarText\":\"FAUZI TEST OK\"}";
		//
		// URLCommon.put(cookie, "communities",
		// "a9e8c451-5ba0-4887-8b9e-e157c6430388", postData);
	}

	/**
	 * Mengecek status layanan DSpace produksi lewat {@code GET
	 * {ConstantValues.DSPACE_URL_PRIVATE}/status}, terautentikasi memakai cookie sesi yang diberikan.
	 * Berbeda dari {@link #logintest()}/{@link #upload}, method ini memakai alamat REST dari
	 * konfigurasi runtime ({@link ais.common.ConstantValues#DSPACE_URL_PRIVATE}), sejalan dengan
	 * jalur produksi {@link #login()}.
	 *
	 * @param cookie cookie sesi {@code JSESSIONID} hasil {@link #login()}
	 * @return respons JSON mentah dari endpoint {@code /status} (mis. status login, tipe pengguna)
	 * @throws Exception diteruskan dari kegagalan koneksi HTTP atau parsing JSON respons
	 */
	public static JSONObject status(String cookie) throws Exception {

		String urlStr = ConstantValues.DSPACE_URL_PRIVATE + "/status";

		URL url = new URL(urlStr);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// CURLOPT_POST
		con.setRequestMethod("GET");

		// CURLOPT_FOLLOWLOCATION
		con.setInstanceFollowRedirects(true);
		con.setRequestProperty("Cookie", "JSESSIONID=" + cookie);
		con.setRequestProperty("Accept", "application/json");

		con.setDoOutput(true);
		con.setDoInput(true);

		// read the response
		DataInputStream input = new DataInputStream(con.getInputStream());
		int c;
		StringBuilder resultBuf = new StringBuilder();
		while ((c = input.read()) != -1) {
			resultBuf.append((char) c);
		}
		input.close();

		return new JSONObject(resultBuf.toString());

	}

	/**
	 * Jalur login PRODUKSI ke DSpace: kredensial dibaca dari konfigurasi runtime
	 * ({@code dspace_username}/{@code dspace_password}, dengan nilai default yang identik dengan
	 * kredensial tertanam di {@link #USERNAME}/{@link #PASSWORD} — lihat peringatan keamanan pada
	 * javadoc kelas), lalu login dicoba berurutan ke daftar alamat kandidat: alamat internal
	 * ({@code dspace_private_url_internal}, bila diisi) lebih dulu, baru alamat utama
	 * ({@link ais.common.ConstantValues#DSPACE_URL_PRIVATE}). Urutan ini sengaja mengutamakan
	 * endpoint internal karena endpoint publik dapat diblokir oleh proteksi Cloudflare/WAF terhadap
	 * permintaan otomatis server-ke-server.
	 *
	 * <p>
	 * Bila percobaan ke suatu alamat gagal karena pemblokiran WAF (dideteksi lewat
	 * {@link ais.common.URLCommon#isWafForbidden(Exception)}), method lanjut mencoba alamat
	 * berikutnya dalam daftar; kegagalan karena sebab lain (mis. kredensial salah) langsung
	 * dilempar tanpa mencoba alamat lain. Begitu satu alamat berhasil login,
	 * {@link ais.common.ConstantValues#DSPACE_URL_PRIVATE} DIPERBARUI ke alamat tersebut (efek
	 * samping global) agar pemanggilan DSpace berikutnya dalam sesi yang sama konsisten memakai
	 * endpoint yang sama dengan endpoint login yang berhasil.
	 * </p>
	 *
	 * @return nilai cookie sesi {@code JSESSIONID} hasil login
	 * @throws IOException bila SEMUA alamat kandidat gagal karena pemblokiran WAF (pesan mengarahkan
	 *                      administrator mengisi {@code dspace_private_url_internal}), bila tidak ada
	 *                      satu pun alamat DSpace terkonfigurasi, atau bila kegagalan login pada suatu
	 *                      alamat disebabkan hal lain di luar WAF (langsung dilempar dari
	 *                      {@link #loginKeAlamat})
	 */
	public static String login() throws Exception {
		String username = Common.getKonfigurasi("dspace_username", "fauzioke2003@gmail.com").getNilai();
		String password = Common.getKonfigurasi("dspace_password", "jangannakal").getNilai();
		String postData = "email=" + URLEncoder.encode(username == null ? "" : username, "UTF-8")
				+ "&password=" + URLEncoder.encode(password == null ? "" : password, "UTF-8");

		String alamatUtama = rapikanAlamat(ConstantValues.DSPACE_URL_PRIVATE);
		String alamatInternal = rapikanAlamat(Common
				.getKonfigurasi("dspace_private_url_internal", "").getNilai());
		List<String> alamatLogin = new ArrayList<String>();
		if (alamatInternal.length() > 0) {
			alamatLogin.add(alamatInternal);
		}
		if (alamatUtama.length() > 0 && !alamatLogin.contains(alamatUtama)) {
			alamatLogin.add(alamatUtama);
		}

		IOException wafTerakhir = null;
		for (String alamat : alamatLogin) {
			try {
				String cookie = loginKeAlamat(alamat, postData);
				// Semua operasi DSpace berikutnya harus memakai endpoint yang sama dengan
				// endpoint login yang berhasil agar cookie sesi tetap berlaku.
				ConstantValues.DSPACE_URL_PRIVATE = alamat;
				return cookie;
			} catch (IOException gagalLogin) {
				if (URLCommon.isWafForbidden(gagalLogin)) {
					wafTerakhir = gagalLogin;
					continue;
				}
				throw gagalLogin;
			}
		}
		if (wafTerakhir != null) {
			throw new IOException("Login DSpace ditolak Cloudflare/WAF. Isi konfigurasi "
					+ "dspace_private_url_internal dengan alamat REST DSpace yang dapat diakses "
					+ "langsung dari server eCampus, tanpa proxy publik Cloudflare.");
		}
		throw new IOException("Alamat DSpace belum dikonfigurasi. Isi dspace_private_url atau "
				+ "dspace_private_url_internal.");
	}

	/**
	 * Melakukan satu percobaan login POST ke {@code alamat + "/login"} dan mengekstrak nilai cookie
	 * {@code JSESSIONID} dari seluruh header {@code Set-Cookie} pada respons (dapat berupa beberapa
	 * nilai cookie berbeda dalam satu respons; hanya pasangan bernama persis {@code JSESSIONID},
	 * dibandingkan tanpa memandang huruf besar/kecil, yang diambil). Dipanggil oleh {@link #login()}
	 * untuk setiap alamat kandidat.
	 *
	 * @param alamat   alamat dasar REST DSpace (tanpa {@code /login}), sudah dirapikan lewat
	 *                 {@link #rapikanAlamat(String)}
	 * @param postData isi body POST berformat {@code application/x-www-form-urlencoded}
	 *                 ({@code email=...&password=...})
	 * @return nilai cookie sesi {@code JSESSIONID}
	 * @throws IOException diteruskan langsung bila kegagalan berasal dari
	 *                      {@link URLCommon#getPostResponseHeader} (termasuk kasus WAF, dideteksi
	 *                      belakangan oleh pemanggil), dibungkus ulang dengan pesan beralamat bila
	 *                      berasal dari pengecualian lain, atau dilempar bila respons tidak memuat
	 *                      cookie {@code JSESSIONID} sama sekali
	 */
	private static String loginKeAlamat(String alamat, String postData) throws IOException {
		String urlStr = alamat + "/login";
		String cookie = "";
		Map<String, List<String>> map;
		try {
			map = URLCommon.getPostResponseHeader(urlStr, postData);
		} catch (IOException koneksi) {
			throw koneksi;
		} catch (Exception koneksi) {
			throw new IOException("Login DSpace gagal pada " + urlStr + ".", koneksi);
		}
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			String key = entry.getKey();
			if (key != null && key.equalsIgnoreCase("Set-Cookie")) {
				List<String> values = entry.getValue();
				if (values == null) {
					continue;
				}
				for (String value : values) {
					if (value == null) {
						continue;
					}
					String[] bagian = value.split(";", 2);
					String pasangan = bagian.length == 0 ? "" : bagian[0];
					int samaDengan = pasangan.indexOf('=');
					if (samaDengan > 0 && "JSESSIONID".equalsIgnoreCase(pasangan.substring(0, samaDengan).trim())) {
						cookie = pasangan.substring(samaDengan + 1).trim();
					}
				}
			}
		}
		if (cookie == null || cookie.trim().length() == 0) {
			throw new IOException(
					"Login DSpace tidak mengembalikan cookie sesi. Periksa dspace_private_url dan kredensial DSpace.");
		}
		return cookie;
	}

	/**
	 * Merapikan alamat dasar URL dengan membuang spasi di ujung dan seluruh garis miring ({@code /})
	 * berlebih di akhir string, sehingga penggabungan dengan path (mis. {@code "/login"}) tidak
	 * menghasilkan garis miring ganda.
	 *
	 * @param alamat alamat mentah, boleh {@code null}
	 * @return alamat yang sudah dirapikan, atau string kosong bila {@code alamat} {@code null}
	 */
	private static String rapikanAlamat(String alamat) {
		String hasil = alamat == null ? "" : alamat.trim();
		while (hasil.endsWith("/")) {
			hasil = hasil.substring(0, hasil.length() - 1);
		}
		return hasil;
	}

	/**
	 * Menerjemahkan pengecualian teknis hasil operasi DSpace menjadi pesan berbahasa Indonesia yang
	 * siap ditampilkan ke pengguna/administrator. Membedakan dua kasus: pemblokiran Cloudflare/WAF
	 * (dideteksi lewat {@link ais.common.URLCommon#isWafForbidden(Exception)} atau lewat penanda
	 * teks {@code "Cloudflare/WAF"} pada pesan pengecualian, yang cocok dengan pesan yang dilempar
	 * {@link #login()}) versus kegagalan koneksi umum lainnya.
	 *
	 * @param exception pengecualian yang ditangkap dari operasi DSpace (login/upload/status)
	 * @return pesan siap tampil yang mengarahkan administrator ke langkah perbaikan yang relevan
	 */
	public static String pesanKoneksi(Exception exception) {
		if (URLCommon.isWafForbidden(exception)
				|| (exception != null && exception.getMessage() != null
						&& exception.getMessage().indexOf("Cloudflare/WAF") >= 0)) {
			return "Koneksi ke repository DSpace ditolak oleh pengaman Cloudflare. "
					+ "Administrator perlu mengisi konfigurasi dspace_private_url_internal "
					+ "dengan alamat REST internal DSpace, lalu ulangi proses.";
		}
		return "Koneksi ke repository DSpace gagal. Periksa alamat REST internal, "
				+ "username, password, dan ketersediaan layanan DSpace.";
	}

}
