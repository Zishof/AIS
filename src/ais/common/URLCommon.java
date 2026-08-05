package ais.common;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.DspaceLog;

public class URLCommon {
	public static Map<String, List<String>> getPostResponseHeader(String urlStr, String postData) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// CURLOPT_POST
		con.setRequestMethod("POST");

		// CURLOPT_FOLLOWLOCATION
		con.setInstanceFollowRedirects(true);

		con.setRequestProperty("Content-length", String.valueOf(postData.length()));

		con.setDoOutput(true);
		con.setDoInput(true);

		DataOutputStream output = new DataOutputStream(con.getOutputStream());
		output.writeBytes(postData);
		output.close();

		int status = con.getResponseCode();
		InputStream responseStream = status >= 400 ? con.getErrorStream() : con.getInputStream();
		if (responseStream == null) {
			responseStream = con.getInputStream();
		}
		DataInputStream input = new DataInputStream(responseStream);
		int c;
		StringBuilder resultBuf = new StringBuilder();
		while ((c = input.read()) != -1) {
			resultBuf.append((char) c);
		}
		input.close();

		if (status >= 400) {
			throw new IOException("POST " + urlStr + " gagal. HTTP " + status + " " + con.getResponseMessage()
					+ ". Response: " + ringkasResponse(resultBuf.toString()));
		}

		Map<String, List<String>> map = con.getHeaderFields();
		return map;
	}

	private static String ringkasResponse(String response) {
		String isi = response == null ? "" : response.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
		while (isi.indexOf("  ") >= 0) {
			isi = isi.replace("  ", " ");
		}
		if (isi.length() > 500) {
			return isi.substring(0, 500) + "...";
		}
		return isi;
	}

	public static JSONObject post(String cookie, String acton, String postData) throws Exception {
		JSONObject jsonObject = null;

		DspaceLog dspaceLog = new DspaceLog();
		dspaceLog.setKeterangan(postData);

		dspaceLog.setAction(DspaceLog.SIMPAN);

		try {
			String u = ConstantValues.DSPACE_URL_PRIVATE + "/" + acton;
			
			
			postData = postData.replaceAll("\\P{Print}", "");
			
			System.out.println("post acton = " + u + ", postData = " + postData);

			URL url = new URL(u);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("POST");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty("Cookie", "JSESSIONID=" + cookie);
			con.setRequestProperty("Content-length", String.valueOf(postData.length()));
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			con.setDoInput(true);

			DataOutputStream output = new DataOutputStream(con.getOutputStream());
			output.writeBytes(postData);
			output.close();

			String data = "{uuid:\"\"}";
			try {
				// read the response
				DataInputStream input = new DataInputStream(con.getInputStream());
				int c;
				StringBuilder resultBuf = new StringBuilder();
				while ((c = input.read()) != -1) {
					resultBuf.append((char) c);
				}
				input.close();
				data = resultBuf.toString();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/URLCommon.java:106");

			}

			Map<String, List<String>> map = con.getHeaderFields();
			for (Map.Entry<String, List<String>> entry : map.entrySet()) {
				System.out.println("Key : " + entry.getKey() + ", Value : " + entry.getValue());
			}

			jsonObject = new JSONObject(data);
			System.out.println("jsonObject = " + jsonObject);

			String handle = !jsonObject.isNull("retrieveLink") ? jsonObject.getString("retrieveLink")
					: !jsonObject.isNull("handle") ? jsonObject.getString("handle") : "";
			dspaceLog.setHandle(handle);

			int code = con.getResponseCode(); // 200 = HTTP_OK
			System.out.println("post Response    (Code):" + code);

			dspaceLog.setStatus(code);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/URLCommon.java:127");
			dspaceLog.setError(e.getMessage());
		}

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(dspaceLog);
		session.getTransaction().commit();
		HibernateUtil.closeSession();

		return jsonObject;
	}

	public static JSONObject get(String action) throws Exception {

		String urlStr = ConstantValues.DSPACE_URL_PRIVATE + "/" + action;

		URL url = new URL(urlStr);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// CURLOPT_POST
		con.setRequestMethod("GET");

		// CURLOPT_FOLLOWLOCATION
		con.setInstanceFollowRedirects(true);
		con.setRequestProperty("Accept", "application/json");

		con.setDoOutput(true);
		con.setDoInput(true);

		String data = "{uuid:\"\"}";
		try {
			// read the response
			DataInputStream input = new DataInputStream(con.getInputStream());
			int c;
			StringBuilder resultBuf = new StringBuilder();
			while ((c = input.read()) != -1) {
				resultBuf.append((char) c);
			}
			input.close();
			data = resultBuf.toString();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/URLCommon.java:168");

		}

		JSONObject jsonObject = new JSONObject(data);
		System.out.println("jsonObject = " + jsonObject);
		return jsonObject;

	}

	public static boolean cek(String action) throws Exception {

		String urlStr = ConstantValues.DSPACE_URL_PRIVATE + "/" + action;

		System.out.println("cek = " + urlStr);

		URL url = new URL(urlStr);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// CURLOPT_POST
		con.setRequestMethod("GET");

		// CURLOPT_FOLLOWLOCATION
		con.setInstanceFollowRedirects(true);
		con.setRequestProperty("Accept", "application/json");

		con.setDoOutput(true);
		con.setDoInput(true);

		int code = con.getResponseCode(); // 200 = HTTP_OK
		System.out.println("Response    (Code):" + code);
		return code == 200;

	}

	public static JSONObject upload(String cookie, String uuid, File binaryFile, String description) throws Exception {
		DspaceLog dspaceLog = new DspaceLog();
		dspaceLog.setKeterangan(description + ". File : " + binaryFile.getName());

		dspaceLog.setAction(DspaceLog.UPLOAD);

		JSONObject jsonObject = null;
		try {

			String charset = "UTF-8";
			String u = ConstantValues.DSPACE_URL_PRIVATE + "/items/" + uuid + "/bitstreams?name="
					+ URLEncoder.encode(binaryFile.getName(), charset) + "&description="
					+ URLEncoder.encode(description, charset);

			String data = "{uuid:\"\"}";
			try {

				System.out.println("curl file " + binaryFile.getAbsolutePath() + " to " + u);

				String[] command = { "curl", "-k", "-4", "-v", "-H", "Content-Type: multipart/form-data", "--cookie",
						"JSESSIONID=" + cookie + "", "-H", "accept: application/json", "-X", "POST", u, "-T",
						binaryFile.getAbsolutePath() };

				ProcessBuilder process = new ProcessBuilder(command);
				Process p;
				p = process.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append(System.getProperty("line.separator"));
				}
				data = builder.toString();

				System.out.println("data = " + data);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/URLCommon.java:240");
			}

			jsonObject = new JSONObject(data);
			System.out.println("jsonObject = " + jsonObject);
			String handle = !jsonObject.isNull("retrieveLink") ? jsonObject.getString("retrieveLink")
					: !jsonObject.isNull("handle") ? jsonObject.getString("handle") : "";
			dspaceLog.setHandle(handle);

			dspaceLog.setStatus(200);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/URLCommon.java:251");
			dspaceLog.setError(e.getMessage());
		}

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(dspaceLog);
		session.getTransaction().commit();
		HibernateUtil.closeSession();

		return jsonObject;
	}

	public static JSONObject uploadOld(String cookie, String uuid, File binaryFile, String description)
			throws Exception {

		DspaceLog dspaceLog = new DspaceLog();
		dspaceLog.setKeterangan(description + ". File : " + binaryFile.getName());

		dspaceLog.setAction(DspaceLog.UPLOAD);

		JSONObject jsonObject = null;
		try {

			String charset = "UTF-8";
			String param = "data";
			String boundary = Long.toHexString(System.currentTimeMillis()); // Just
																			// generate
																			// some
																			// unique
																			// random
																			// value.
			String CRLF = "\r\n"; // Line separator required by
									// multipart/form-data.

			URL url = new URL(ConstantValues.DSPACE_URL_PRIVATE + "/items/" + uuid + "/bitstreams?name="
					+ URLEncoder.encode(binaryFile.getName(), charset) + "&description="
					+ URLEncoder.encode(description, charset));
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
			writer.append(
					"Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + binaryFile.getName() + "\"")
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

			// Request is lazily fired whenever you need to obtain information
			// about
			// response.
			int responseCode = ((HttpURLConnection) con).getResponseCode();
			System.out.println(responseCode); // Should be 200

			String data = "{uuid:\"\"}";
			try {
				// read the response
				DataInputStream input = new DataInputStream(con.getInputStream());
				int c;
				StringBuilder resultBuf = new StringBuilder();
				while ((c = input.read()) != -1) {
					resultBuf.append((char) c);
				}
				input.close();
				data = resultBuf.toString();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/URLCommon.java:343");

			}

			jsonObject = new JSONObject(data);
			System.out.println("jsonObject = " + jsonObject);
			String handle = !jsonObject.isNull("retrieveLink") ? jsonObject.getString("retrieveLink")
					: !jsonObject.isNull("handle") ? jsonObject.getString("handle") : "";
			dspaceLog.setHandle(handle);

			int code = con.getResponseCode(); // 200 = HTTP_OK
			System.out.println("Upload Response    (Code):" + code);

			dspaceLog.setStatus(code);
		} catch (Exception e) {
			dspaceLog.setError(e.getMessage());
		}

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(dspaceLog);
		session.getTransaction().commit();
		HibernateUtil.closeSession();

		return jsonObject;
	}

	public static int put(String cookie, String acton, String postData) throws Exception {
		int code = 0;

		DspaceLog dspaceLog = new DspaceLog();
		dspaceLog.setKeterangan(postData);

		dspaceLog.setAction(DspaceLog.UBAH);

		try {
			String urlStr = ConstantValues.DSPACE_URL_PRIVATE + "/" + acton;
			URL url = new URL(urlStr);

			System.out.println("put urlStr = " + urlStr + ", postData = " + postData);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("PUT");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty("Cookie", "JSESSIONID=" + cookie);
			con.setRequestProperty("Content-length", String.valueOf(postData.length()));
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			con.setDoInput(true);

			DataOutputStream output = new DataOutputStream(con.getOutputStream());
			output.writeBytes(postData);
			output.close();

			Map<String, List<String>> map = con.getHeaderFields();
			for (Map.Entry<String, List<String>> entry : map.entrySet()) {
				System.out.println("Key : " + entry.getKey() + ", Value : " + entry.getValue());
			}

			code = con.getResponseCode(); // 200 = HTTP_OK
			System.out.println("Put Response    (Code):" + code);

			dspaceLog.setStatus(code);
		} catch (Exception e) {
			dspaceLog.setError(e.getMessage());
		}

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(dspaceLog);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		return code;
	}

	public static int delete(String cookie, String acton, String description) throws Exception {

		DspaceLog dspaceLog = new DspaceLog();
		dspaceLog.setKeterangan(description);

		dspaceLog.setAction(DspaceLog.HAPUS);

		int code = 0;

		try {

			String urlStr = ConstantValues.DSPACE_URL_PRIVATE + "/" + acton;
			URL url = new URL(urlStr);

			System.out.println("delete urlStr = " + urlStr);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("DELETE");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty("Cookie", "JSESSIONID=" + cookie);
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setDoOutput(true);
			con.setDoInput(true);

			code = con.getResponseCode(); // 200 = HTTP_OK
			System.out.println("Delete Response    (Code):" + code);

			dspaceLog.setStatus(code);

		} catch (Exception e) {
			dspaceLog.setError(e.getMessage());
		}

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(dspaceLog);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		return code;
	}
}
