package ais.common;

import java.util.Calendar;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class Test2 {

	public static void main(String[] args) throws Exception {

		// PostMethod post = new
		// PostMethod("https://developer.bri.co.id/v1/api/briva");

		for (int i = 0; i < 70; i++) {

			String virtual_account = Common.getGeneratedAngkaDigit(10);
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
			String datetime_expired = Common.databaseDateFormat1.get().format(calendar.getTime());
			
			
			String postData = "{\"amount\":15000,\"keterangan\":\"\",\"nama\":\"MEIYANG " + virtual_account
					+ "\",\"institutionCode\":\"J104408\",\"expiredDate\":\"" + datetime_expired
					+ "\",\"brivaNo\":\"77777\",\"custCode\":\"" + virtual_account + "\"}";

			CloseableHttpClient httpclient = HttpClients.createDefault();
			try {
				HttpPost httppost = new HttpPost("https://developer.bri.co.id/v1/api/briva");

				StringEntity entity = new StringEntity(postData);
				httppost.setEntity(entity);
				httppost.setHeader("Accept", "application/json");
				httppost.setHeader("Content-type", "application/json");
				httppost.setHeader("X-BRI-KEY", "74c6af09da81c1ba474a552c6b03b25b6940875c");
				httppost.setHeader("Authorization", "Bearer 700d15850067afdbccdcff19946a20e8c2dac38e");

				System.out.println("Executing request: " + httppost.getRequestLine());
				CloseableHttpResponse response = httpclient.execute(httppost);
				try {
					System.out.println("----------------------------------------");
					System.out.println(response.getStatusLine().getStatusCode());
					System.out.println(EntityUtils.toString(response.getEntity()));
				} finally {
					response.close();
				}
			} finally {
				httpclient.close();
			}
		}
		// String t = "Pekerjaan->Nama Perusahaan Tempat Bekerja Saat Ini<=>PT.
		// Wave Consulting Indonesia<=><=>1001<=>18<=>1\r\n"
		// + "Pekerjaan->Jabatan<=>Project Manager<=><=>1002<=>19<=>1\r\n"
		// + "Pekerjaan->Alamat Perusahaan<=>Ruko Grand Aries, Jalan Taman
		// Aries, Meruya, Jakarta Barat<=><=>1003<=>20<=>1\r\n"
		// + "Pekerjaan->Telepon Kantor<=><=><=>1004<=>21<=>1\r\n"
		// + "Pekerjaan->Penghasilan Pribadi Per Bulan<=><7.5
		// juta<=><=>1005<=>22<=>1\r\n"
		// + "Pekerjaan->Bidang Perusahaan<=>Konsultan IT<=><=>1006<=>7<=>1\r\n"
		// + "Pekerjaan->Lama Bekerja<=>1 tahun<=><=>1007<=>8<=>1\r\n"
		// + "Pekerjaan->Kategori Perusahaan<=>Perusahaan
		// Konsultan<=><=>1008<=>9<=>1\r\n"
		// + "Pekerjaan->Mulai bekerja dari
		// tanggal<=>05-02-2018<=><=>1009<=>11<=>1\r\n"
		// + "Pekerjaan->Sampai Tanggal<=><=><=>1010<=>10<=>1\r\n"
		// + "Pekerjaan->Website
		// Perusahaan<=>www.wvi.co.id<=><=>1011<=>12<=>1\r\n"
		// + "Pekerjaan->Kota Perusahaan<=>Jakarta Barat<=><=>1012<=>13<=>1\r\n"
		// + "Pekerjaan->Propinsi Perusahaan<=>DKI
		// Jakarta<=><=>1013<=>14<=>1\r\n"
		// + "Pekerjaan->Negara Perusahaan<=>Indonesia<=><=>1014<=>15<=>1\r\n"
		// + "Pekerjaan->No. Telp Perusahaan<=><=><=>1015<=>16<=>1" +
		//
		// "Pekerjaan->Nama Perusahaan Tempat Bekerja Saat Ini<=>PT.oek
		// jaya<=><=>1001<=>18<=>1\r\n"
		// + "Pekerjaan->Jabatan<=>Project Manager<=><=>1002<=>19<=>1\r\n"
		// + "Pekerjaan->Alamat Perusahaan<=>Ruko Grand Aries, Jalan Taman
		// Aries, Meruya, Jakarta Barat<=><=>1003<=>20<=>1\r\n"
		// + "Pekerjaan->Telepon Kantor<=><=><=>1004<=>21<=>1\r\n"
		// + "Pekerjaan->Penghasilan Pribadi Per Bulan<=><7.5
		// juta<=><=>1005<=>22<=>1\r\n"
		// + "Pekerjaan->Bidang Perusahaan<=>Konsultan IT<=><=>1006<=>7<=>1\r\n"
		// + "Pekerjaan->Lama Bekerja<=>1 tahun<=><=>1007<=>8<=>1\r\n"
		// + "Pekerjaan->Kategori Perusahaan<=>Perusahaan
		// Konsultan<=><=>1008<=>9<=>1\r\n"
		// + "Pekerjaan->Mulai bekerja dari
		// tanggal<=>05-02-2018<=><=>1009<=>11<=>1\r\n"
		// + "Pekerjaan->Sampai Tanggal<=><=><=>1010<=>10<=>1\r\n"
		// + "Pekerjaan->Website
		// Perusahaan<=>www.wvi.co.id<=><=>1011<=>12<=>1\r\n"
		// + "Pekerjaan->Kota Perusahaan<=>Jakarta Barat<=><=>1012<=>13<=>1\r\n"
		// + "Pekerjaan->Propinsi Perusahaan<=>DKI
		// Jakarta<=><=>1013<=>14<=>1\r\n"
		// + "Pekerjaan->Negara Perusahaan<=>Indonesia<=><=>1014<=>15<=>1\r\n"
		// + "Pekerjaan->No. Telp Perusahaan<=><=><=>1015<=>16<=>1";
		//
		// if (!t.startsWith("{")) {
		// JSONObject master = new JSONObject();
		// JSONArray jsonArraySub = new JSONArray();
		// Set<String> idParameterTambahan = new HashSet<String>();
		// String lbnSebelumnya = "";
		// String[] splNama = t.split("\n");
		// for (int j = 0; j < splNama.length; j++) {
		//
		// String namaCol = splNama.length > j ? splNama[j] : "";
		//
		// String[] value = namaCol.split("<=>");
		//
		// String lbl = value.length > 0 ? value[0].trim() : "";
		// String url = value.length > 2 ? value[2].trim() : "";
		// String nilai = value.length > 1 ? value[1].trim() : "";
		// Integer nomorUrut = 1;
		// try {
		// nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Test2.java:119");
		//
		// }
		// Long id = -1L;
		// try {
		// id = value.length > 4 ? Long.parseLong(value[4].trim()) : -1L;
		// } catch (Exception e) {
		//
		// }
		//
		// Long idKel = -1L;
		// try {
		// idKel = value.length > 5 ? Long.parseLong(value[5].trim()) : -1L;
		// } catch (Exception e) {
		//
		// }
		//
		// String[] lbs = lbl.split("->");
		//
		// System.out.println(lbs[0] + " -> " + value.length);
		//
		// JSONObject jsonObject = new JSONObject();
		// jsonObject.put("kelompok", lbs[0]);
		// jsonObject.put("nama", lbs.length > 1 ? lbs[1] : "");
		// jsonObject.put("nilai", nilai);
		// jsonObject.put("url", url);
		// jsonObject.put("nomorUrut", nomorUrut);
		// jsonObject.put("id", id);
		// jsonObject.put("id_kelompok", idKel);
		//
		// if (!lbnSebelumnya.trim().equalsIgnoreCase(lbs[0].trim())) {
		//
		// if (jsonArraySub.length() > 0 &&
		// !lbnSebelumnya.trim().equalsIgnoreCase(lbs[0].trim())) {
		// master.put(lbs[0], jsonArraySub);
		// }
		//
		// jsonArraySub = new JSONArray();
		// lbnSebelumnya = lbs[0];
		// }
		// jsonArraySub.put(jsonObject);
		// idParameterTambahan.add(id);
		// }
		// if (jsonArraySub.length() > 0 && !lbnSebelumnya.trim().isEmpty()) {
		// master.put(lbnSebelumnya, jsonArraySub);
		// }
		//
		// String json = master.toString();
		// System.out.println(json);
		// System.out.println(json.startsWith("{"));
		// }

		// String kode = "1101.01";
		// System.out.println(kode.substring(0,kode.length()-3));

		// String bit48Request = "129 12963026191 TEST-H2H-LAGI_LA";
		// String trx_id = bit48Request.substring(10, 26).trim();
		// System.out.println(trx_id);

		// String data =
		// "{\"mti\":\"0200\",\"bit2\":\"000000\",\"bit3\":\"380000\",\"bit4\":\"000000010000\",\"bit7\":\"02030120
		// \",\"bit11\":\"000046\",\"bit12\":\"010152\",\"bit13\":\"0203\",\"bit15\":\"0203\",\"bit18\":\"6014\",\"bit32\":\"0080001\",\"bit37\":\"12345361
		// \",\"bit41\":\"00000129\",\"bit42\":\" \",\"bit48\":\"12912345678
		// 000000010000\",\"bit49\":\"360\",\"bit63\":\"28b99f36adff3b18d271708b48d7dc19b988e2294de12319756b28a990d70513\"}";
		//
		// JSONObject jsonObject = new JSONObject(data);
		//
		// Iterator iterator = jsonObject.keys();
		// while (iterator.hasNext()) {
		// Object key = iterator.next();
		// Object value = jsonObject.get(key.toString());
		// System.out.println("key => " + key + ", value => " + value);
		// }
		//
		// String bit48Request = jsonObject.getString("bit48");
		// String noVa = bit48Request.split(" ")[0];
		//
		// String Norek_alias = Common.maxPanjangSpace("129", 10);
		// String No_VA = Common.maxPanjangSpace(noVa, 16);
		// String Nama = Common.maxPanjangSpace("Budi", 30);
		// String Jenis_trx = Common.maxPanjangSpace("TOPUP", 30);
		// String Keterangan = Common.maxPanjangSpace("Bayar A", 30);
		// String Id_trx = Common.maxPanjangSpace("1234567890", 30);
		// String amount = Common.maxPanjangNol("10000", 12);
		// String admin = Common.maxPanjangNol("0", 12);
		// String hp = Common.maxPanjangSpace("0812222222", 30);
		// String email = Common.maxPanjangSpace("test@yahoo.com", 30);
		//
		// if (jsonObject.getString("bit3").equals("380000")) {
		// System.out.println("Request " + jsonObject);
		//
		// String bit48 = Norek_alias + No_VA + Nama + Jenis_trx + Keterangan +
		// Id_trx +
		// amount + admin + hp + email;
		// jsonObject.put("bit48", bit48);
		// jsonObject.put("bit39", "00");
		//
		// String bit62 = Common.maxPanjangSpace("No.VA", 10) +
		// Common.maxPanjangSpace(No_VA, 30)
		// + Common.maxPanjangSpace("Nama", 10) + Nama +
		// Common.maxPanjangSpace("Jenis",
		// 10) + Jenis_trx
		// + Common.maxPanjangSpace("Nominal", 10)
		// + Common.maxPanjangSpace("Rp. " +
		// Common.numberFormat.get().format(Long.parseLong(amount)), 30)
		// + Common.maxPanjangSpace("Instansi", 10) +
		// Common.maxPanjangSpace("Pelita
		// Bangsa", 30);
		// jsonObject.put("bit62", bit62);
		//
		// } else if (jsonObject.getString("bit3").equals("170000")) {
		// System.out.println("Payment " + jsonObject);
		// String reffNum = Common.maxPanjangSpace(Common.getGeneratedBarCode(),
		// 30);
		// String bit48 = Norek_alias + No_VA + Nama + Jenis_trx + Keterangan +
		// Id_trx +
		// amount + admin + hp + email
		// + reffNum;
		// jsonObject.put("bit48", bit48);
		// jsonObject.put("bit39", "00");
		//
		// String bit62 = Common.maxPanjangSpace("No.VA", 10) +
		// Common.maxPanjangSpace(No_VA, 30)
		// + Common.maxPanjangSpace("Nama", 10) + Nama +
		// Common.maxPanjangSpace("Jenis",
		// 10) + Jenis_trx
		// + Common.maxPanjangSpace("Refnum", 10) +
		// Common.maxPanjangSpace(Id_trx, 30)
		// + Common.maxPanjangSpace("Instansi", 10) +
		// Common.maxPanjangSpace("Pelita
		// Bangsa", 30);
		// jsonObject.put("bit62", bit62);
		// }
		//
		// jsonObject.put("mti", "0210");
		//
		// String response = jsonObject.toString();
		// System.out.println("response " + response);
	}

}

//
// public class Test {
//
// public static void listAlertContent(String message) {
// try {
//
// Socket socket = new Socket("54.251.44.17", 7892);
// // Socket socket = new Socket("54.251.62.201", 7892);
// // Socket socket = new Socket("localhost", 7892);
//
// OutputStream out = socket.getOutputStream();
// PrintStream pout = new PrintStream(out);
// pout.println(message);
// out.flush();
//
// InputStream in = socket.getInputStream();
// String result = "";
// int c;
// while ((c = in.read()) != -1) {
// result += (char) c;
// }
//
// in.close();
// pout.close();
//
// System.out.println("Received: " + result);
// // corem2m.core.util.Common.showLog("Received: " + result);
//
// } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/Test2.java:289");
// ex.printStackTrace();
// }
// }
//
// public static void main(String[] argv) throws JSONException {
//
// // for (int i = 0; i < 100; i++) {
// //
// // new Thread(new Runnable() {
// //
// // @Override
// // public void run() {
// // String message = "366562336526565355;getAccount;achtar.tuanda@gmail.com";
// //
// // listAlertContent(message);
// //
// // message =
// "366562336526565355;updateAccount;achtar.tuanda@gmail.com;123;Asrofi
// Ridho;081382028582;fauzioke2003@email.com";
// // listAlertContent(message);
// //
// // message = "366562336526565355;getAccount;achtar.tuanda@gmail.com";
// //
// // listAlertContent(message);
// // }
// // }).start();
// //
// // }
// //
// String message = "366562336526565355;login;achtar.tuanda@gmail.com;123";
//
// listAlertContent(message);
//
//
// message = "366562336526565355;getGuardianPassword;achtar.tuanda@gmail.com";
//
// listAlertContent(message);
//
// message = "366562336526565355;getGuardianPassword;asrofiridho@gmail.com";
//
// listAlertContent(message);
//
//
// // JSONObject daftarPengumuan =
// //
// getJsonObject("http://zishofdemo.cloudapp.net/ecampus/resources/pengumumanAkademis/search/1001111360/1001111360");
// //
// // System.out.println("daftarPengumuan = " + daftarPengumuan);
// // JSONArray arr = daftarPengumuan.getJSONArray("pengumumanAkademis");
// //
// // for (int i = 0; i < arr.length(); i++) {
// // JSONObject obj = arr.getJSONObject(i);
// // System.out.println("obj = " + obj);
// //
// // JSONObject jurusan = obj.isNull("jurusan") ? null : obj
// // .getJSONObject("jurusan");
// // if (jurusan != null) {
// // System.out
// // .println("Nama Jurusan = " + jurusan.getString("nama")
// // + ", jurusan = " + jurusan);
// // }
// // }
// //
// // JSONObject login =
// //
// getJsonObject("http://zishofdemo.cloudapp.net/ecampus/resources/mahasiswa/login/1001111360/1001111360");
// // String nama = login.getString("nama");
// // String nim = login.getString("nim");
// //
// // System.out.println("nama = " + nama + ", nim = " + nim);
//
// // JSONObject cari_buku_stok =
// //
// getJsonObject("http://zishofdemo.cloudapp.net/ecampus/resources/perpustakaan/cari_buku_stok/_/_/_/_/desc/_/_/_/0/100/");
// // System.out.println("cari_buku_stok = " + cari_buku_stok);
// // JSONArray stokItem = cari_buku_stok.getJSONArray("stokItem");
// // for (int i = 0; i < stokItem.length(); i++) {
// // JSONObject obj = stokItem.getJSONObject(i);
// // System.out.println("obj = " + obj.get("nama"));
// // }
//
// // JSONObject items =
// //
// getJsonObject("http://zishofdemo.cloudapp.net/ecampus/resources/perpustakaan/items/_/_/_/_/_/_/nama/_/0/20/");
// // System.out.println("items = " + items);
// // JSONArray stokItem = items.getJSONArray("item");
// // for (int i = 0; i < stokItem.length(); i++) {
// // JSONObject obj = stokItem.getJSONObject(i);
// // System.out.println("obj = " + obj);
// // }
//
// }
//
// public static String getString(String urlString) {
// try {
// URL url = new URL(urlString);
// URLConnection connection = url.openConnection();
// connection.setDoOutput(true);
// connection.setRequestProperty("Content-Type", "application/json");
// connection.setConnectTimeout(5000);
// connection.setReadTimeout(5000);
//
// BufferedReader in = new BufferedReader(new InputStreamReader(
// connection.getInputStream(), "UTF-8"));
// String json = "";
// String str = "";
// while ((json = in.readLine()) != null) {
// byte[] bytes = json.getBytes("UTF-8");
// str += new String(bytes, "UTF-8");
// }
// in.close();
// in.close();
// return str;
// } catch (Exception e) {
// System.out.println("\nError while calling REST Service");
// System.out.println(e);
// }
// return "";
// }
//
// public static JSONObject getJsonObject(String url) {
// try {
// JSONObject jsono = new JSONObject(getString(url));
//
// return jsono;
// } catch (Exception ee) {
// // Common.tampilErrorJikaAdmin(ee); 
// }
// return null;
// }
//
// public static JSONArray getJsonArray(String url) {
// try {
// JSONArray jsono = new JSONArray(getString(url));
//
// return jsono;
// } catch (Exception ee) {
// Common.tampilErrorJikaAdmin(ee); 
// }
// return null;
// }
//
// }
