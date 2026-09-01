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

/**
 * Utilitas integrasi pembayaran dengan gateway <b>BSI Maja</b> (Bank Syariah Indonesia, via
 * platform Makaramas/Maja — {@code makaramas.com} / {@code maja.id}) untuk pembuatan Virtual
 * Account, permintaan token autentikasi OAuth2 (grant type {@code password}), pendaftaran
 * tagihan (register), dan pengecekan status pembayaran (inquiry) pada Virtual Account BSI. Kelas
 * ini menjadi jembatan antara model domain AIS ({@link VirtualAccountBank}, {@link BankHost},
 * {@link KanalPembayaran}, {@link Sekolah}) dengan REST API eksternal milik BSI/Maja, dan
 * merupakan salah satu dari beberapa util payment gateway bank di paket {@code ais.common}
 * (bandingkan dengan util Faspay untuk penyedia pembayaran lain).
 *
 * <p>
 * <b>Riwayat keamanan (DIPERBAIKI 2026-09-01):</b> method
 * {@link #sendRequestToken(Sekolah, KanalPembayaran)} sebelumnya memakai
 * {@link Common#getKonfigurasi(String, String)} dengan nilai default literal (kredensial klien
 * BSI/Maja lingkungan pengujian/development, mengingat URL token default mengarah ke realm
 * {@code bpi-dev} pada {@code account.makaramas.com}) yang tertanam langsung di kode sumber untuk
 * {@code CLIENT_ID}/{@code CLIENT_SECRET}/{@code USERNAME}/{@code PASSWORD} bila konfigurasi
 * database belum diisi ataupun objek {@link Sekolah}/{@link KanalPembayaran} tidak
 * menyediakannya. Seluruh default rahasia tersebut sudah DIHAPUS (kini string kosong). Baris
 * {@code System.out.println} yang sebelumnya mencetak isi respons token OAuth mentah (setara
 * access token itu sendiri) juga sudah dihapus. <b>Tindak lanjut yang TETAP diperlukan di luar
 * perubahan kode ini:</b> kredensial yang sebelumnya tertanam sudah lama berada di riwayat SVN
 * dan WAJIB dianggap bocor — perlu ditinjau/dirotasi di sisi BSI/Maja bila masih aktif.
 * </p>
 *
 * <p>
 * <b>Pola resolusi kredensial berjenjang</b> — pada {@link #sendRequestToken(Sekolah, KanalPembayaran)},
 * kredensial diresolusi dengan prioritas: (1) field milik {@link KanalPembayaran} bila diberikan
 * dan tidak kosong (prioritas tertinggi, memungkinkan kanal pembayaran spesifik memakai
 * kredensial merchant BSI sendiri), (2) field milik {@link Sekolah} (mis.
 * {@code getBsiMerchantId()}, {@code getBsiScretId()}, {@code getBsiUsername()},
 * {@code getBsiPassword()}) bila sekolah sudah tersimpan (memiliki id) dan field tidak kosong,
 * lalu (3) fallback ke konfigurasi global {@code maja_CLIENT_ID}/{@code maja_CLIENT_SECRET}/
 * {@code maja_USERNAME}/{@code maja_PASSWORD} dengan nilai default tertanam sebagaimana
 * dijelaskan di atas. Pola berjenjang ini mendukung skenario multi-tenant (beberapa sekolah/kanal
 * pembayaran dengan akun merchant BSI berbeda-beda dalam satu instalasi AIS).
 * </p>
 *
 * <p>
 * <b>Pola retry token kedaluwarsa</b> — {@link #sendRequest(JSONObject, String, Sekolah,
 * KanalPembayaran, boolean)} dan {@link #sendRequestInquery(JSONObject, BankHost, String, boolean)}
 * menerima parameter {@code coba} (boolean) yang menandai apakah pemanggilan saat ini adalah
 * percobaan pertama; bila permintaan HTTP gagal (kemungkinan besar karena token akses sudah
 * kedaluwarsa/tidak valid) DAN {@code coba} bernilai {@code true}, kedua method ini akan meminta
 * token baru lewat {@link #sendRequestToken()} lalu mengulang permintaan sekali lagi dengan
 * {@code coba=false} (mencegah rekursi tak berhingga bila kegagalan berulang bukan karena token).
 * Kelas ini secara sengaja TIDAK menyimpan/melakukan cache token secara otomatis antar-panggilan
 * (field {@code CLIENT_TOKEN}/{@code CLIENT_TOKEN_EXPIRED} yang pernah direncanakan untuk caching
 * token terlihat dikomentari/dinonaktifkan di kode sumber) — setiap pemanggil bertanggung jawab
 * menyimpan dan meneruskan token yang berlaku.
 * </p>
 *
 * <p>
 * Kelas ini memakai dua library HTTP client sekaligus secara tidak konsisten: Apache Commons
 * HttpClient 3.x ({@code org.apache.commons.httpclient}, API yang sudah usang/deprecated, dipakai
 * pada {@link #sendRequest} dan {@link #sendRequestInquery}) dan Apache HttpClient 4.x
 * ({@code org.apache.http}, dipakai pada {@link #sendRequestToken(Sekolah, KanalPembayaran)}) —
 * kemungkinan besar akibat evolusi kode dari waktu ke waktu, bukan desain yang disengaja.
 * </p>
 */
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

	/**
	 * Membangun payload inquiry dari data {@link VirtualAccountBank} yang sudah tersimpan (nomor
	 * invoice diambil dari field JSON {@code data.number} pada {@code virtualAccountBank.getResponse()},
	 * jumlah dari {@code getTotal()}, kode VA dari {@code getKode()}) lalu meneruskannya ke
	 * {@link #sendRequestInquery(JSONObject, BankHost, String, boolean)} untuk mengecek status
	 * pembayaran VA tersebut ke gateway BSI/Maja.
	 *
	 * @param virtualAccountBank Virtual Account yang hendak dicek statusnya
	 * @param CLIENT_TOKEN       token akses OAuth2 yang berlaku, biasanya hasil
	 *                           {@link #sendRequestToken()}
	 * @param bankHostDefault    konfigurasi host bank yang diteruskan ke
	 *                           {@link Maja#doProcess} bila pembayaran ternyata sudah lunas
	 * @return respons JSON mentah dari gateway (hasil inquiry), diteruskan apa adanya dari
	 *         {@link #sendRequestInquery(JSONObject, BankHost, String, boolean)}
	 * @throws Exception diteruskan dari kegagalan parsing JSON respons VA maupun kegagalan
	 *                    komunikasi HTTP
	 */
	public static JSONObject inqiery(VirtualAccountBank virtualAccountBank, String CLIENT_TOKEN,
			BankHost bankHostDefault) throws Exception {

		String number = new JSONObject(virtualAccountBank.getResponse()).getJSONObject("data").getString("number");

		JSONObject jsonObject = new JSONObject();

		jsonObject.put("amount", virtualAccountBank.getTotal().intValue());
		jsonObject.put("invoiceNumber", number);
		jsonObject.put("va", virtualAccountBank.getKode());

		return BSIMajaUtil.sendRequestInquery(jsonObject, bankHostDefault, CLIENT_TOKEN, true);
	}

	/**
	 * Varian ringkas {@link #sendRequestToken(Sekolah, KanalPembayaran)} yang otomatis
	 * mengambil {@link Sekolah} aktif lewat {@link SekolahUtil#getSekolah()} dan tanpa
	 * {@link KanalPembayaran} spesifik.
	 *
	 * @return token akses OAuth2 (access_token) dari BSI/Maja, atau {@code null} bila permintaan
	 *         gagal
	 * @throws Exception diteruskan dari kegagalan yang tidak tertangkap secara internal
	 */
	public static String sendRequestToken() throws Exception {
		Sekolah sekolah = SekolahUtil.getSekolah();
		return sendRequestToken(sekolah, null);
	}

	/**
	 * Meminta token akses OAuth2 (grant type {@code password}) ke endpoint token BSI/Maja
	 * (konfigurasi {@code maja_TOKEN_URL}), memakai kredensial klien yang diresolusi secara
	 * berjenjang: {@link KanalPembayaran} bila diberikan dan lengkap, lalu {@link Sekolah} bila
	 * tersimpan dan lengkap, lalu fallback ke konfigurasi global {@code maja_CLIENT_ID}/
	 * {@code maja_CLIENT_SECRET}/{@code maja_USERNAME}/{@code maja_PASSWORD} — lihat peringatan
	 * keamanan pada Javadoc kelas mengenai nilai default kredensial yang tertanam di kode ini.
	 *
	 * <p>
	 * Permintaan dikirim sebagai form ter-encode URL ({@code application/x-www-form-urlencoded})
	 * lewat Apache HttpClient 4.x. Kegagalan permintaan (jaringan, status non-2xx, parsing JSON)
	 * ditangkap dan dicatat lewat {@link ais.common.ErrorAuditUtil#record(Throwable, String)},
	 * dengan method mengembalikan {@code null} alih-alih melempar pengecualian ke pemanggil.
	 * </p>
	 *
	 * @param sekolah          entitas sekolah yang menyediakan kredensial BSI spesifik institusi
	 *                         (dipakai bila field-nya terisi), boleh {@code null}
	 * @param kanalPembayaran  kanal pembayaran yang menyediakan kredensial BSI spesifik kanal
	 *                         (prioritas tertinggi bila field-nya terisi), boleh {@code null}
	 * @return token akses OAuth2 ({@code access_token}) hasil parsing respons JSON, atau
	 *         {@code null} bila permintaan gagal
	 * @throws Exception saat ini tidak pernah dilempar keluar (kegagalan ditangkap internal),
	 *                    dipertahankan pada signature untuk kompatibilitas pemanggil
	 */
	public static String sendRequestToken(Sekolah sekolah, KanalPembayaran kanalPembayaran) throws Exception {

		String CLIENT_ID = sekolah != null && sekolah.getId() != null && !sekolah.getBsiMerchantId().isEmpty()
				? sekolah.getBsiMerchantId()
				: Common.getKonfigurasi("maja_CLIENT_ID", "").getNilai();
		String CLIENT_SECRET = sekolah != null && sekolah.getId() != null && !sekolah.getBsiScretId().isEmpty()
				? sekolah.getBsiScretId()
				: Common.getKonfigurasi("maja_CLIENT_SECRET", "").getNilai();
		String TOKEN_URL = Common.getKonfigurasi("maja_TOKEN_URL",
				"https://account.makaramas.com/auth/realms/bpi-dev/protocol/openid-connect/token").getNilai();
		String USERNAME = sekolah != null && sekolah.getId() != null && !sekolah.getBsiUsername().isEmpty()
				? sekolah.getBsiUsername()
				: Common.getKonfigurasi("maja_USERNAME", "").getNilai();
		String PASSWORD = sekolah != null && sekolah.getId() != null && !sekolah.getBsiPassword().isEmpty()
				? sekolah.getBsiPassword()
				: Common.getKonfigurasi("maja_PASSWORD", "").getNilai();

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

			JSONObject token = new JSONObject(responseBody);

			CLIENT_TOKEN = token.getString("access_token");
//			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
//			calendar.set(Calendar.SECOND, token.getInt("expires_in"));
//			CLIENT_TOKEN_EXPIRED = calendar.getTime();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BSIMajaUtil.java:154");
		}

		return CLIENT_TOKEN;
	}

	/**
	 * Varian ringkas {@link #sendRequest(JSONObject, String, Sekolah, KanalPembayaran, boolean)}
	 * yang otomatis mengambil {@link Sekolah} aktif lewat {@link SekolahUtil#getSekolah()} dan
	 * tanpa {@link KanalPembayaran} spesifik.
	 *
	 * @param postData     payload JSON pendaftaran Virtual Account/tagihan
	 * @param CLIENT_TOKEN token akses OAuth2 yang berlaku
	 * @param coba         {@code true} bila ini percobaan pertama (mengizinkan retry otomatis
	 *                     dengan token baru saat gagal)
	 * @return respons JSON dari gateway BSI/Maja, atau {@code null} bila gagal
	 * @throws Exception diteruskan dari kegagalan yang tidak tertangkap secara internal
	 */
	public static JSONObject sendRequest(JSONObject postData, String CLIENT_TOKEN, boolean coba) throws Exception {
		Sekolah sekolah = SekolahUtil.getSekolah();
		return sendRequest(postData, CLIENT_TOKEN, sekolah, null, coba);
	}

	/**
	 * Mendaftarkan tagihan/Virtual Account ke gateway BSI/Maja lewat endpoint
	 * {@code POST {BILLING_HOST}/api/v2/register}, dengan {@code BILLING_HOST} diresolusi dari
	 * {@link KanalPembayaran#getBsiGatewayUrl()} bila terisi, lalu {@link Sekolah#getBsiGatewayUrl()}
	 * bila terisi, lalu fallback konfigurasi global {@code maja_BILLING_HOST} (default
	 * {@code "https://billing-bpi-dev.maja.id"}, mengarah ke lingkungan development).
	 *
	 * <p>
	 * Permintaan dikirim sebagai body JSON mentah lewat Apache Commons HttpClient 3.x (API
	 * deprecated) dengan header {@code Authorization: Bearer <CLIENT_TOKEN>}. Bila permintaan
	 * gagal dan {@code coba=true}, method meminta token baru lewat {@link #sendRequestToken()}
	 * dan mengulang permintaan sekali lagi dengan {@code coba=false} — <b>namun perlu dicatat
	 * bahwa hasil dari percobaan ulang ini TIDAK dikembalikan</b> (dipanggil tanpa menyimpan nilai
	 * kembaliannya), sehingga pada skenario token kedaluwarsa, pemanggil pertama akan tetap
	 * menerima {@code bsi} bernilai {@code null} dari percobaan awal yang gagal walaupun
	 * percobaan ulang di baliknya berhasil.
	 * </p>
	 *
	 * @param postData        payload JSON pendaftaran Virtual Account/tagihan
	 * @param CLIENT_TOKEN    token akses OAuth2 yang berlaku
	 * @param sekolah         entitas sekolah untuk resolusi {@code BILLING_HOST} spesifik
	 *                        institusi, boleh {@code null}
	 * @param kanalPembayaran kanal pembayaran untuk resolusi {@code BILLING_HOST} spesifik kanal
	 *                        (prioritas tertinggi), boleh {@code null}
	 * @param coba            {@code true} bila ini percobaan pertama (mengizinkan satu kali retry
	 *                        otomatis dengan token baru saat gagal); {@code false} untuk mencegah
	 *                        rekursi tak berhingga pada percobaan ulang
	 * @return respons JSON dari gateway (hasil pendaftaran), atau {@code null} bila permintaan
	 *         gagal (termasuk pada percobaan awal walau retry di baliknya berhasil — lihat catatan
	 *         di atas)
	 * @throws Exception saat ini tidak pernah dilempar keluar (kegagalan ditangkap internal),
	 *                    dipertahankan pada signature untuk kompatibilitas pemanggil
	 */
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

	/**
	 * Mengecek status pembayaran suatu Virtual Account ke gateway BSI/Maja lewat endpoint
	 * {@code POST {maja_BILLING_HOST}/api/v2/inquiry}, dan bila gateway melaporkan pembayaran
	 * sudah lunas ({@code paid=true}), langsung memproses pelunasan tersebut ke sistem AIS lewat
	 * {@link Maja#doProcess(Integer, String, String, String, BankHost, Object, String, boolean)}.
	 *
	 * <p>
	 * Field yang diambil dari respons inquiry: {@code va} (nomor Virtual Account), {@code totalPayment}
	 * (jumlah yang sudah dibayarkan), {@code lastPaymentDate} (tanggal pembayaran terakhir, dengan
	 * fallback ke field {@code date} bila {@code lastPaymentDate} kosong), dan {@code paid}
	 * (status lunas/belum). Bila objek respons memiliki pembungkus {@code data}, field-field
	 * tersebut dibaca dari dalamnya; bila tidak, dibaca langsung dari objek respons akar.
	 * </p>
	 *
	 * <p>
	 * Sama seperti {@link #sendRequest}, permintaan dikirim lewat Apache Commons HttpClient 3.x
	 * (deprecated) dengan header {@code Authorization: Bearer <CLIENT_TOKEN>}. Bila terjadi
	 * pengecualian dan {@code coba=true}, method meminta token baru lalu mengulang permintaan
	 * sekali (hasil percobaan ulang juga tidak dikembalikan — perilaku sama dengan
	 * {@link #sendRequest}).
	 * </p>
	 *
	 * @param postData        payload JSON permintaan inquiry (amount, invoiceNumber, va)
	 * @param bankHostDefault konfigurasi host bank yang diteruskan ke {@link Maja#doProcess} saat
	 *                        pembayaran terkonfirmasi lunas
	 * @param CLIENT_TOKEN    token akses OAuth2 yang berlaku
	 * @param coba            {@code true} bila ini percobaan pertama (mengizinkan satu kali retry
	 *                        otomatis dengan token baru saat gagal)
	 * @return respons JSON mentah dari gateway (hasil inquiry), atau {@code null} bila permintaan
	 *         gagal
	 * @throws Exception saat ini tidak pernah dilempar keluar (kegagalan ditangkap dan dicatat
	 *                    lewat {@link ais.common.ErrorAuditUtil#record(Throwable, String)}),
	 *                    dipertahankan pada signature untuk kompatibilitas pemanggil
	 */
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
