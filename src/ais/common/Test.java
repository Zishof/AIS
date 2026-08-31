package ais.common;

import org.apache.commons.lang3.StringUtils;

/**
 * Kelas <b>uji coba/scratch manual</b> murni untuk kebutuhan pengembang — bukan bagian dari alur
 * aplikasi AIS yang berjalan normal. Tidak ada indikasi kelas ini dipanggil dari bagian lain
 * aplikasi; satu-satunya method, {@link #main(String[])}, adalah tempat pengembang menjalankan
 * potongan kode percobaan sekali pakai (menguji fungsi {@link StringUtils#replace}, format angka,
 * parsing tanggal, membangun pesan ISO-8583, memanggil REST/SOAP eksternal, dsb.) sebelum
 * dipindahkan ke lokasi produksinya masing-masing.
 *
 * <p>
 * Isi kelas ini <b>hampir seluruhnya berupa kode yang telah dinonaktifkan (commented-out)</b> —
 * bertumpuk-tumpuk potongan uji coba dari waktu yang berbeda-beda, dibiarkan sebagai catatan/
 * referensi historis daripada dihapus. Hanya dua baris paling atas di {@link #main(String[])}
 * yang benar-benar aktif dieksekusi (menguji {@link StringUtils#replace} untuk mengonversi urutan
 * escape {@code "\\n"} literal menjadi karakter newline sungguhan dan sebaliknya); seluruh sisanya
 * — termasuk seluruh definisi kelas kedua di bagian paling bawah berkas ini (di luar deklarasi
 * kelas {@code Test} yang resmi, murni komentar blok C-style berisi kelas {@code Test} versi lama
 * dengan method {@code listAlertContent}/{@code getString}/{@code getJsonObject}/{@code getJsonArray})
 * — adalah kode nonaktif.
 * </p>
 *
 * <h2>Peringatan keamanan — nilai sensitif tertanam di kode yang dinonaktifkan</h2>
 * <p>
 * Meskipun dinonaktifkan (tidak pernah dieksekusi selama tetap dalam bentuk komentar), beberapa
 * blok komentar di {@link #main(String[])} dan di kelas lama pada komentar blok akhir berkas ini
 * tetap memuat nilai sensitif yang sudah tercatat permanen di riwayat kode sumber:
 * </p>
 * <ul>
 * <li>Header {@code X-BRI-KEY: "b6642aad94d9861f21671cfcccfa672fc880a89d"} dan
 * {@code Authorization: "Bearer ee9d8ad39fe81ffe276bc52833108b2513eb8854"} pada blok komentar
 * percobaan panggilan API BRI VA ({@code https://developer.bri.co.id/v1/api/briva}) — keduanya
 * berbentuk token/API key nyata, bukan placeholder.</li>
 * <li>Sejumlah alamat email pribadi tertanam sebagai data uji coba pada blok komentar (mis.
 * {@code achtar.tuanda@gmail.com}, {@code asrofiridho@gmail.com}, dan variasi
 * {@code fauzioke2003@email.com}) beserta host/IP server percobaan lama
 * ({@code 54.251.44.17}/{@code 54.251.62.201} port {@code 7892}, domain
 * {@code zishofdemo.cloudapp.net}) pada komentar blok kelas lama di akhir berkas.</li>
 * </ul>
 * <p>
 * Sesuai cakupan pekerjaan dokumentasi ini, komentar-komentar tersebut TIDAK dihapus atau diubah
 * — lihat ringkasan hasil dokumentasi untuk detail lokasi baris lengkap. Karena nilai-nilai ini
 * berada di dalam kode yang tidak pernah dieksekusi, risikonya terbatas pada pengungkapan lewat
 * pembacaan kode sumber/riwayat versi, bukan eksekusi aktif — namun API key/token BRI tersebut
 * tetap sebaiknya ditinjau dan dirotasi bila masih berlaku.
 * </p>
 */
public class Test {

	/**
	 * Titik masuk uji coba manual. Bagian yang benar-benar aktif hanya menguji konversi bolak-balik
	 * antara urutan escape {@code "\\n"} literal (dua karakter: backslash + huruf n) dan karakter
	 * newline sungguhan lewat {@link StringUtils#replace(String, String, String)} serta
	 * {@link String#replaceAll(String, String)}, dengan hasil tiap tahap dicetak ke konsol. Seluruh
	 * kode setelahnya adalah potongan-potongan uji coba historis yang dinonaktifkan (commented-out)
	 * — lihat javadoc kelas untuk ringkasan isinya dan peringatan keamanan terkait nilai sensitif
	 * yang tertanam di dalamnya.
	 *
	 * @param args argumen baris perintah; tidak dipakai sama sekali oleh method ini
	 * @throws Exception dideklarasikan untuk mengakomodasi potongan kode uji coba yang mungkin
	 *                    diaktifkan kembali oleh pengembang; kode yang aktif saat ini tidak
	 *                    melempar exception apa pun dalam kondisi normal
	 */
	public static void main(String[] args) throws Exception {
		
		String data = "siap\\n oke bosss";
		
		data = StringUtils.replace(data, "\\\\n", "\n");
		System.out.println("data 1 -> " + data);
		data = data.replaceAll("\n", "\\\\n");
		System.out.println("data 2 -> " + data);

//		for (int i = 0; i < 100; i++) {
//			String ports = "1010,2020,3030,4040,5050,6060,7070,8080,9090,1000,9000,8081,7071,5051";
//			java.util.Random random = new java.util.Random();
//			int max = 13;
//			int min = 0;
//			int rand = random.nextInt(max - min + 1) + min;
//			String port = ports.split(",")[rand];
//			System.out.println("port => " + port + ", rand => " + rand);
//		}

//		Double ips = 3.47;

//		System.out.println("port =>  " + Common.numberFormatEn.get().format(ips));

//		String nama = "DATA31;3<>___DATA36;3<>___DATA32;3<>___DATA43;3<>___DATA53;3<>___DATA49;3<>___DATA58;3<>___DATA33;3<>___DATA34;3<>___DATA38;3<>___DATA37;3<>___DATA40;3<>___DATA41;3<>___DATA42;3<>___DATA45;3<>___DATA44;3<>___DATA47;3<>___DATA51;2<>___DATA52;2<>___DATA54;3<>___DATA55;3<>___DATA56;3<>___DATA59;3<>___DATA57;3<>___DATA35;3<>___DATA39;3<>___DATA46;3<>___DATA48;3<>___DATA50;3<>___DATA60;3<>";
//		System.out.println("nama -> " + nama);
//
//		List<Long> hasil = new ArrayList<Long>();
//		for (String d : StringUtils.split(nama, "<>___DATA")) {
//			if (StringUtils.contains(d, ";")) {
//				Long sd = Long.parseLong(StringUtils.split(d, ";")[0].trim());
//				hasil.add(sd); 
//			}
//		}
//		
		
		
//		String p = "241001051626;241001051708;241001132254;241001211926;700101051626;700101211926";
//		
//		Date waktu = null;
//		String[] d = p.split(";");
//		for (String s : d) {
//			try {
//				if (!s.startsWith("700")) {
//					Date dss = Common.dateFormat84.get().parse(s);
//
//					Double nilai = Double.parseDouble(Common.timeFormat2.get().format(dss));
//
//					if (nilai >= masuk && nilai <= pulang) {
//
//						if (waktu == null || waktu.before(dss)) {
//
//							waktu = dss;
//
//						}
//
//					}
//				}
//			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/Test.java:64");
//
//			}
//		}
//		
//		System.out.println("hasil -> " + hasil);
//		System.out.println("nama -> "+nama);

		// PostMethod post = new
		// PostMethod("https://developer.bri.co.id/v1/api/briva");

		// for (int i = 0; i < 70; i++) {
		//
		// String virtual_account = Common.getGeneratedAngkaDigit(10);
		// Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		// calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		// String datetime_expired =
		// Common.databaseDateFormat1.get().format(calendar.getTime());
		//
		//
		// String postData =
		// "{\"amount\":15000,\"keterangan\":\"\",\"nama\":\"MEIYANG " +
		// virtual_account
		// + "\",\"institutionCode\":\"J104408\",\"expiredDate\":\"" +
		// datetime_expired
		// + "\",\"brivaNo\":\"77777\",\"custCode\":\"" + virtual_account +
		// "\"}";
		//
		// CloseableHttpClient httpclient = HttpClients.createDefault();
		// try {
		// HttpPost httppost = new
		// HttpPost("https://developer.bri.co.id/v1/api/briva");
		//
		// StringEntity entity = new StringEntity(postData);
		// httppost.setEntity(entity);
		// httppost.setHeader("Accept", "application/json");
		// httppost.setHeader("Content-type", "application/json");
		// httppost.setHeader("X-BRI-KEY",
		// "b6642aad94d9861f21671cfcccfa672fc880a89d");
		// httppost.setHeader("Authorization", "Bearer
		// ee9d8ad39fe81ffe276bc52833108b2513eb8854");
		//
		// System.out.println("Executing request: " +
		// httppost.getRequestLine());
		// CloseableHttpResponse response = httpclient.execute(httppost);
		// try {
		// System.out.println("----------------------------------------");
		// System.out.println(response.getStatusLine().getStatusCode());
		// System.out.println(EntityUtils.toString(response.getEntity()));
		// } finally {
		// response.close();
		// }
		// } finally {
		// httpclient.close();
		// }
		// }
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
		// } catch (Exception e) {
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
// } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/Test.java:354");
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
