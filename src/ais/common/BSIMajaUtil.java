package ais.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.servlet.Maja;
import ais.database.model.BankHost;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.KanalPembayaran;
import ais.database.model.sekolah.Sekolah;

public class BSIMajaUtil {

//	public static Date CLIENT_TOKEN_EXPIRED = null;
//	public static String CLIENT_TOKEN = null;

//	public static void main(String[] args) throws Exception {
//
//		JSONArray items = new JSONArray();
//		JSONObject jsonObjectitems = new JSONObject();
//		jsonObjectitems.put("description", "UI Works Common Area 2hours");
//		jsonObjectitems.put("unitPrice", 25000);
//		jsonObjectitems.put("qty", 1);
//		jsonObjectitems.put("amount", 25000);
//		items.put(jsonObjectitems);
//
//		JSONObject jsonObject = new JSONObject();
//
//		jsonObject.put("date", Common.databaseDateFormat.get().format(new Date()));
//		jsonObject.put("amount", 25000);
//		jsonObject.put("name", "Alfiyah");
//		jsonObject.put("email", "alfiyah@sebuahdomain.com");
//		jsonObject.put("address", "Depok");
//		jsonObject.put("va", "880812345671");
//		jsonObject.put("attribute1", "Fasilkom");
//		jsonObject.put("attribute2", "Manajemen Sistem Informasi");
//		jsonObject.put("items", items);
//		jsonObject.put("attributes", new JSONArray());
//
//		BSIMajaUtil.sendRequest(jsonObject, true);
//	}

	public static JSONObject inqiery(VirtualAccountBank virtualAccountBank, String CLIENT_TOKEN,
			BankHost bankHostDefault) throws Exception {

		String number = new JSONObject(virtualAccountBank.getResponse()).getJSONObject("data").getString("number");

		JSONObject jsonObject = new JSONObject();

		jsonObject.put("amount", virtualAccountBank.getTotal().intValue());
		jsonObject.put("invoiceNumber", number);
		jsonObject.put("va", virtualAccountBank.getKode());

		return BSIMajaUtil.sendRequestInquery(jsonObject, bankHostDefault, CLIENT_TOKEN, true);
	}

	public static String sendRequestToken() throws Exception {
		Sekolah sekolah = SekolahUtil.getSekolah();
		return sendRequestToken(sekolah, null);
	}

	public static String sendRequestToken(Sekolah sekolah, KanalPembayaran kanalPembayaran) throws Exception {

		String CLIENT_ID = sekolah != null && sekolah.getId() != null && !sekolah.getBsiMerchantId().isEmpty()
				? sekolah.getBsiMerchantId()
				: Common.getKonfigurasi("maja_CLIENT_ID", "BPI7512").getNilai();
		String CLIENT_SECRET = sekolah != null && sekolah.getId() != null && !sekolah.getBsiScretId().isEmpty()
				? sekolah.getBsiScretId()
				: Common.getKonfigurasi("maja_CLIENT_SECRET", "JRs0EtuebD0XpC0JVXQOc6kUPZ7o24rG").getNilai();
		String TOKEN_URL = Common.getKonfigurasi("maja_TOKEN_URL",
				"https://account.makaramas.com/auth/realms/bpi-dev/protocol/openid-connect/token").getNilai();
		String USERNAME = sekolah != null && sekolah.getId() != null && !sekolah.getBsiUsername().isEmpty()
				? sekolah.getBsiUsername()
				: Common.getKonfigurasi("maja_USERNAME", "7512").getNilai();
		String PASSWORD = sekolah != null && sekolah.getId() != null && !sekolah.getBsiPassword().isEmpty()
				? sekolah.getBsiPassword()
				: Common.getKonfigurasi("maja_PASSWORD", "7512").getNilai();

		if (kanalPembayaran != null && !kanalPembayaran.getBsiMerchantId().isEmpty()) {
			CLIENT_ID = kanalPembayaran.getBsiMerchantId();
		}
		if (kanalPembayaran != null && !kanalPembayaran.getBsiScretId().isEmpty()) {
			CLIENT_SECRET = kanalPembayaran.getBsiScretId();
		}
		if (kanalPembayaran != null && !kanalPembayaran.getBsiUsername().isEmpty()) {
			USERNAME = kanalPembayaran.getBsiUsername();
		}
		if (kanalPembayaran != null && !kanalPembayaran.getBsiPassword().isEmpty()) {
			PASSWORD = kanalPembayaran.getBsiPassword();
		}

		List<BasicNameValuePair> form = new ArrayList<BasicNameValuePair>();
		form.add(new BasicNameValuePair("client_id", CLIENT_ID));
		form.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
		form.add(new BasicNameValuePair("grant_type", "password"));
		form.add(new BasicNameValuePair("username", USERNAME));
		form.add(new BasicNameValuePair("password", PASSWORD));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);

		String CLIENT_TOKEN = null;
//		JSONObject token = null;
//		PostMethod post = new PostMethod(TOKEN_URL);
		try {

			HttpPost httpPost = new HttpPost(TOKEN_URL);
			httpPost.setEntity(entity);
			System.out.println("Executing request " + httpPost.getRequestLine());

			CloseableHttpClient httpclient = HttpClients.createDefault();
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				@Override
				public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity responseEntity = response.getEntity();
						return responseEntity != null ? EntityUtils.toString(responseEntity) : null;
					} else {
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}
			};

			String responseBody = httpclient.execute(httpPost, responseHandler);
			System.out.println("----------------------------------------");
			System.out.println(responseBody);

			JSONObject token = new JSONObject(responseBody);
			System.out.println("token = " + token);

			CLIENT_TOKEN = token.getString("access_token");
//			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
//			calendar.set(Calendar.SECOND, token.getInt("expires_in"));
//			CLIENT_TOKEN_EXPIRED = calendar.getTime();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BSIMajaUtil.java:154");
		}

		return CLIENT_TOKEN;
	}

	public static JSONObject sendRequest(JSONObject postData, String CLIENT_TOKEN, boolean coba) throws Exception {
		Sekolah sekolah = SekolahUtil.getSekolah();
		return sendRequest(postData, CLIENT_TOKEN, sekolah, null, coba);
	}

	@SuppressWarnings("deprecation")
	public static JSONObject sendRequest(JSONObject postData, String CLIENT_TOKEN, Sekolah sekolah,
			KanalPembayaran kanalPembayaran, boolean coba) throws Exception {

		JSONObject bsi = null;
		String BILLING_HOST = sekolah != null && sekolah.getId() != null && !sekolah.getBsiGatewayUrl().isEmpty()
				? sekolah.getBsiGatewayUrl()
				: Common.getKonfigurasi("maja_BILLING_HOST", "https://billing-bpi-dev.maja.id").getNilai();

		if (kanalPembayaran != null && !kanalPembayaran.getBsiGatewayUrl().isEmpty()) {
			BILLING_HOST = kanalPembayaran.getBsiGatewayUrl();
		}

		PostMethod post = new PostMethod(BILLING_HOST + "/api/v2/register");
		try {
			String postD = postData.toString();
			System.out.println(postD);
			StringRequestEntity requestEntity = new StringRequestEntity(postD);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Authorization", "Bearer " + CLIENT_TOKEN);
			post.setRequestHeader("Content-type", "application/json");

			HttpClient httpclient = new HttpClient();

			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");

			String hasil = post.getResponseBodyAsString();

			bsi = new JSONObject(hasil);
			System.out.println(bsi);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BSIMajaUtil.java:198");
			try {
				if (coba) {
					CLIENT_TOKEN = sendRequestToken();
					sendRequest(postData, CLIENT_TOKEN, sekolah, kanalPembayaran, false);
				}
			} catch (Exception ew) {
				ew.printStackTrace(); ais.common.ErrorAuditUtil.record(ew, "auto-audit src/ais/common/BSIMajaUtil.java:205");
			}

		}

		return bsi;
	}

	@SuppressWarnings("deprecation")
	public static JSONObject sendRequestInquery(JSONObject postData, BankHost bankHostDefault, String CLIENT_TOKEN,
			boolean coba) throws Exception {

		JSONObject bsi = null;
		String BILLING_HOST = Common.getKonfigurasi("maja_BILLING_HOST", "https://billing-bpi-dev.maja.id").getNilai();
		PostMethod post = new PostMethod(BILLING_HOST + "/api/v2/inquiry");
		try {
			String postD = postData.toString();
			StringRequestEntity requestEntity = new StringRequestEntity(postD);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Authorization", "Bearer " + CLIENT_TOKEN);
			post.setRequestHeader("Content-type", "application/json");

			HttpClient httpclient = new HttpClient();

			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");

			String hasil = post.getResponseBodyAsString();

			bsi = new JSONObject(hasil);
			System.out.println(bsi);
			JSONObject data = bsi.isNull("data") ? bsi : bsi.getJSONObject("data");

			String va = data.getString("va");
			Integer totalPayment = data.getInt("totalPayment");
			String lastPaymentDate = data.isNull("lastPaymentDate") ? data.getString("date")
					: data.getString("lastPaymentDate");

			Boolean paid = data.getBoolean("paid");

			System.out.println("va = " + va);
			System.out.println("totalPayment = " + totalPayment);
			System.out.println("lastPaymentDate = " + lastPaymentDate);
			System.out.println("paid = " + paid);

			String bank = "Maja";

			if (paid) {

				JSONObject res = Maja.doProcess(totalPayment, lastPaymentDate, va, bank, bankHostDefault, null, hasil,
						true);
				System.out.println("res = " + res);
			}

		} catch (Exception e) {

			if (coba) {
				CLIENT_TOKEN = sendRequestToken();
				sendRequestInquery(postData, bankHostDefault, CLIENT_TOKEN, false);
			}

			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BSIMajaUtil.java:267");
		}

		return bsi;
	}
}
