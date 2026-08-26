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

public class DspaceCommon {

	public static String REST_URL = "http://demo.ecampus.id/rest";
	private static String USERNAME = "fauzioke2003@gmail.com";
	private static String PASSWORD = "jangannakal";

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

	private static String rapikanAlamat(String alamat) {
		String hasil = alamat == null ? "" : alamat.trim();
		while (hasil.endsWith("/")) {
			hasil = hasil.substring(0, hasil.length() - 1);
		}
		return hasil;
	}

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
