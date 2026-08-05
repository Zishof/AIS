package ais.action.master.helper.virtualaccount;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;

import ais.action.master.helper.util.MahasiswaSmartlinkChannelWindow;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.helper.util.SmartlinkChannelWindow;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BJBUtil;
import ais.common.BRIDataUtil;
import ais.common.BSIMajaUtil;
import ais.common.Common;
import ais.common.OttoUtil;
import ais.common.URLBuilder;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class DownloadNoRegistrasiCalonMahasiswaBankOnline extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;
	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	static JenisKegiatan jenisKegiatan = pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);

	public DownloadNoRegistrasiCalonMahasiswaBankOnline() {
		super();

	}

	public DownloadNoRegistrasiCalonMahasiswaBankOnline(String title, String border, boolean closable) {
		super(title, border, closable);

	}

	@SuppressWarnings("rawtypes")
	public static VirtualAccountBank downloadData(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JadwalPembayaran myjadwalPembayaran, Collection detailBiayas, Map param, Double biayaAdmin,
			BankHost bankHost) throws Exception {
		String waktuSampai = null;
		return downloadData(biodataCalonMahasiswa, myjadwalPembayaran, detailBiayas, param, biayaAdmin, bankHost,
				waktuSampai);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static VirtualAccountBank downloadData(BiodataCalonMahasiswa biodataCalonMahasiswa,
			JadwalPembayaran myjadwalPembayaran, Collection detailBiayas, Map param, Double biayaAdmin,
			BankHost bankHost, String waktuSampai) throws Exception {

		Boolean qris = (Boolean) (param.get("qris") == null ? false : param.get("qris"));
		Boolean finpay = (Boolean) (param.get("finpay") == null ? false : param.get("finpay"));
		Boolean otto = (Boolean) (param.get("otto") == null ? false : param.get("otto"));
		Boolean briva = (Boolean) (param.get("briva") == null ? false : param.get("briva"));
		Boolean flip = (Boolean) (param.get("flip") == null ? false : param.get("flip"));
		Boolean maja = (Boolean) (param.get("maja") == null ? false : param.get("maja"));
		Boolean smartlink = (Boolean) (param.get("smartlink") == null ? false : param.get("smartlink"));
		List<String> warnings = (param.get("warnings") == null ? null : (List<String>) param.get("warnings"));
		boolean update = (Boolean) (param.get("update") == null ? false : param.get("update"));
		String detailbiaya = "";
		String esmartlinkBayarVia = (String) (param.get("esmartlinkBayarVia") == null ? null
				: param.get("esmartlinkBayarVia"));

		Session session = MahasiswaVirtualAccountHelper.openSession();
		try {

		String ket = (param.get("ket") == null ? "" : param.get("ket") + "");
		String pemb = (param.get("pemb") == null ? "" : param.get("pemb") + "");
		String cicilan = (param.get("cicilan") == null ? "" : param.get("cicilan") + "");
		Double total = (param.get("total") == null ? 0.0 : (Double) param.get("total"));
		JSONArray items = (param.get("items") == null ? new JSONArray() : (JSONArray) param.get("items"));

		JSONArray itemsBersih = new JSONArray();
		for (int i = 0; i < items.length(); i++) {
			try {
				if (!"Biaya Admin".equals(items.getJSONObject(i).optString("description"))) {
					itemsBersih.put(items.getJSONObject(i));
				}
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:111");
			}
		}
		items = itemsBersih;
		if (biayaAdmin.intValue() > 0) {
			JSONObject jsonObjectitems = new JSONObject();
			jsonObjectitems.put("description", "Biaya Admin");
			jsonObjectitems.put("unitPrice", biayaAdmin.intValue());
			jsonObjectitems.put("qty", 1);
			jsonObjectitems.put("amount", biayaAdmin.intValue());
			items.put(jsonObjectitems);
		}
		String keteranganSimpleBanget = "";

		if (param.get("items") == null) {
			for (Object o : detailBiayas) {
				if (o instanceof DetailBiaya) {
					DetailBiaya biaya = (DetailBiaya) o;
					ItemBiaya itemBiaya = biaya.getItemBiaya();
					detailbiaya += (detailbiaya.isEmpty() ? biaya.getId() : "," + biaya.getId());

					Double nilai = (biaya.getNilaiBiayaBaru() == null ? biaya.getNilaiBiaya()
							: biaya.getNilaiBiayaBaru());
					if (nilai > 0.01) {

						String descSimpleBanget = itemBiaya.getId() + "";

						keteranganSimpleBanget += keteranganSimpleBanget.isEmpty() ? descSimpleBanget
								: ";" + descSimpleBanget;

						String desc = itemBiaya.getKode().trim() + "," + itemBiaya.getNama().trim() + ","
								+ (nilai).longValue();

						pemb += desc + ";";

						cicilan = MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan, ("Item-" + itemBiaya.getId().toString() + "-" + nilai + "-" + biaya.getBayarKe() + "-"
										+ biaya.getId()));

						ket += ket.isEmpty() ? itemBiaya.getNama() : "," + itemBiaya.getNama();

						total += nilai;

						JSONObject jsonObjectitems = new JSONObject();
						jsonObjectitems.put("description", desc);
						jsonObjectitems.put("unitPrice", nilai.intValue());
						jsonObjectitems.put("qty", 1);
						jsonObjectitems.put("amount", nilai.intValue());
						items.put(jsonObjectitems);
					}
				}
			}
		}

		boolean tagihan_expired_akhir_hari = Common
				.getKonfigurasi("tagihan_expired_akhir_hari", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
				.equals(Konfigurasi.AKTIF);
		Date expired_date = myjadwalPembayaran.getEndDate();
		if (tagihan_expired_akhir_hari) {
			try {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.set(Calendar.HOUR_OF_DAY, 23);
				calendar.set(Calendar.MINUTE, 59);
				calendar.set(Calendar.SECOND, 59);
				expired_date = calendar.getTime();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:176");
			}
		} else {
			String tagihan_expired_jam = Common.getKonfigurasi("tagihan_expired_jam", "").getNilai();
			if (!tagihan_expired_jam.isEmpty()) {
				if (!tagihan_expired_jam.isEmpty() && !tagihan_expired_jam.equalsIgnoreCase("0")) {
					try {
						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.set(Calendar.HOUR_OF_DAY,
								calendar.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(tagihan_expired_jam));
						expired_date = calendar.getTime();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:188");
					}
				}
			} else {
				String tagihan_expired_day = Common.getKonfigurasi("tagihan_expired_day", "0").getNilai();

				if (!tagihan_expired_day.isEmpty() && !tagihan_expired_day.equalsIgnoreCase("0")) {
					try {
						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.set(Calendar.DATE,
								calendar.get(Calendar.DATE) + Integer.parseInt(tagihan_expired_day));
						expired_date = calendar.getTime();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:201");
					}
				}
			}
		}

		if (waktuSampai != null) {
			try {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_15_MENIT)) {
					calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 15);
				} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_30_MENIT)) {
					calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 30);
				} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_1_JAM)) {
					calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 1);
				} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_3_JAM)) {
					calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 3);
				} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_6_JAM)) {
					calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + 6);
				} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_24_JAM)) {
					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
				} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_3_HARI)) {
					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 3);
				} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_1_MINGGU)) {
					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 7);
				} else if (waktuSampai.equals(SmartlinkChannelWindow.WAKTU_1_BULAN)) {
					calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
				}
				expired_date = calendar.getTime();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:231");
			}
		}

		VirtualAccountBank virtualAccountBankOnline = (VirtualAccountBank) session
				.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("terjadiKendala", false))
				.add(bankHost == null ? Restrictions.isNull("bankHost") : Restrictions.eq("bankHost", bankHost))
				.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
				.add(Restrictions.eq("keterangan", pemb + (qris ? "qris:true" : "")))
				.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
				.add(Restrictions.eq("jenisKegiatan", myjadwalPembayaran.getJenisKegiatan()))
				.add(Restrictions.isNull("kegiatan")).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

		if (virtualAccountBankOnline != null && virtualAccountBankOnline.getChannel() != null
				&& esmartlinkBayarVia != null
				&& esmartlinkBayarVia.equalsIgnoreCase(virtualAccountBankOnline.getChannel())) {
			update = false;
		}

		if (virtualAccountBankOnline == null || update) {

			if (virtualAccountBankOnline == null) {
				virtualAccountBankOnline = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());
			} else if (update && virtualAccountBankOnline != null) {
				virtualAccountBankOnline.setVa(null);
				virtualAccountBankOnline.setLink(null);
				virtualAccountBankOnline.setResponse(null);
			}

			virtualAccountBankOnline.setKanalPembayaran(
					myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
							: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());

			if (qris) {

				String hasil = "";
				try {
					String va = Common.getGeneratedAngkaDigit(10);
					int mytotal = total.intValue() + biayaAdmin.intValue();

					JSONObject postData = new JSONObject();

					postData.put("merchantId",
							Common.getKonfigurasi("qris_jaring_merchantId", "3200124010015").getNilai());
					postData.put("terminalId", Common.getKonfigurasi("qris_jaring_terminalId", "10010005").getNilai());
					postData.put("trxId", va);
					postData.put("amount", mytotal + "");
					postData.put("expire", Common.getKonfigurasi("qris_jaring_expire", "7200").getNilai());
					postData.put("posId", biodataCalonMahasiswa.getNoRegistrasi());
					postData.put("timestamp", Common.databaseDateFormat1.get().format(WaktuUtil.getDate()));

					String strURL = Common
							.getKonfigurasi("qris_jaring_gateway_url", "http://api.jsa2.host/agg/api/v1/qris/generate")
							.getNilai();

					String screet_key = Common.getKonfigurasi("qris_jaring_screet_key", "YnNuOmJzbg==").getNilai();

					String sign = postData.getString("merchantId") + postData.getString("terminalId")
							+ postData.getString("posId") + postData.getString("trxId") + postData.getString("amount")
							+ postData.getString("expire") + postData.getString("timestamp") + screet_key;
					String token = DigestUtils.sha256Hex(sign);

					postData.put("signature", token);

					System.out.println(postData);

					String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H",
							"Authorization: Basic " + screet_key, "-X", "POST", strURL, "--data", postData.toString() };

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
					hasil = builder.toString();
					System.out.println(hasil);

					JSONObject jSONObject = new JSONObject(hasil);

					if (!jSONObject.getString("ack").equals("00")) {
						return null;
					}

					String data = jSONObject.getString("data");
					byte[] decodedBytes = org.apache.commons.codec.binary.Base64.decodeBase64(data);
					String decodedString = new String(decodedBytes);

					JSONObject jSONObjectDecode = new JSONObject(decodedString);

					System.out.println(jSONObjectDecode);

					virtualAccountBankOnline.setRequest(postData.toString());
					virtualAccountBankOnline.setResponse(jSONObjectDecode.toString());

					virtualAccountBankOnline.setKode(jSONObjectDecode.getString("rawQRIS"));
					virtualAccountBankOnline.setBank("QRIS");
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:333");
					return null;
				}

			}

			else if (finpay) {

				String hasil = "";
				try {
					int mytotal = total.intValue() + biayaAdmin.intValue();

					if (expired_date == null) {
						try {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
							expired_date = calendar.getTime();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:351");
						}
					}

					String strURL = Common.getKonfigurasi("finpay_gateway_url_data",
							"https://devo.finnet.co.id/pg/payment/card/initiate").getNilai();

					String ApiKeyFinpay = Common.getKonfigurasi("finpay_apikeyfinpay_data", "").getNilai();

					String TokenFinpay = Common.getKonfigurasi("finpay_tokenFinpay_data", "").getNilai();

					String sender_name = biodataCalonMahasiswa.getNama();
					String sender_email = biodataCalonMahasiswa.getEmail().split(",")[0].split(";")[0];

					String sender_phone_number = biodataCalonMahasiswa.getHp();

					if (sender_email == null || sender_email.isEmpty()) {
						PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
						sender_email = perguruanTinggi.getEmail();
					}

					if (sender_phone_number == null || sender_phone_number.isEmpty()) {
						PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
						sender_phone_number = perguruanTinggi.getTelepon();
					}

					sender_phone_number = sender_phone_number.startsWith("08")
							? "+62" + sender_phone_number.substring(1)
							: sender_phone_number;
					sender_phone_number = sender_phone_number.startsWith("0") ? "+62" + sender_phone_number.substring(1)
							: sender_phone_number;
					sender_phone_number = !sender_phone_number.startsWith("+") ? "+62" + sender_phone_number
							: sender_phone_number;

					JSONObject postData = new JSONObject();

					JSONObject customer = new JSONObject();
					customer.put("email", sender_email);
					customer.put("firstName", sender_name);
					customer.put("lastName",
							biodataCalonMahasiswa.getPaket() == null ? "" : biodataCalonMahasiswa.getPaket().getNama());
					customer.put("mobilePhone", sender_phone_number);

					postData.put("customer", customer);

					String va = WaktuUtil.getDate().getTime() + "";

					JSONObject order = new JSONObject();
					order.put("id", va);
					order.put("amount", mytotal + "");
					order.put("description", keteranganSimpleBanget.trim());

					postData.put("order", order);

					JSONObject url = new JSONObject();
					url.put("callbackUrl", Common.CURRENT_URL + "/Finpay");

					postData.put("url", url);

					String screet_key = DownloadTagihanSiswaBankOnline.getBasicAuthenticationHeader(ApiKeyFinpay,
							TokenFinpay);

//					System.out.println("screet_key -> " + screet_key);

					String[] command = { "curl", "-k", "-H", "Content-Type: application/json", "-H",
							"Authorization: " + screet_key, "-X", "POST", strURL, "--data", postData.toString() };

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
					hasil = builder.toString();
					System.out.println(hasil);

					JSONObject jSONObject = new JSONObject(hasil);

					System.out.println(jSONObject);

					try {
						virtualAccountBankOnline
								.setKadaluarsa(Common.databaseDateFormat1.get().parse(jSONObject.get("expiryLink") + ""));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:438");
						// TODO: handle exception
					}

					virtualAccountBankOnline.setRequest(postData.toString());
					virtualAccountBankOnline.setResponse(jSONObject.toString());

					if (!jSONObject.isNull("redirecturl")) {
						virtualAccountBankOnline.setLink(jSONObject.get("redirecturl") + "");
					} else {
						return null;
					}

					virtualAccountBankOnline.setKode(va);
					virtualAccountBankOnline.setBank("Finpay");

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:455");
					return null;
				}

			}

			else if (flip) {
				try {
					int mytotal = total.intValue() + biayaAdmin.intValue();

					if (expired_date == null) {
						try {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
							expired_date = calendar.getTime();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:471");
						}
					}

					String strURL = Common.getKonfigurasi("flip_gateway_url_v2", "https://bigflip.id/api/v2/pwf/bill")
							.getNilai();

//					String redirect_url = Common.getRequestHostWithProtocol() + "/Flip";

					String sender_address = biodataCalonMahasiswa.getAlamat();
					String sender_name = biodataCalonMahasiswa.getNama();

					String sender_email = biodataCalonMahasiswa.getEmail().split(",")[0];

					String sender_phone_number = biodataCalonMahasiswa.getHp();

					if (sender_address == null || sender_address.isEmpty()) {
						sender_address = biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi()
								.getAlamat1();
					}
					if (sender_email == null || sender_email.isEmpty()) {
						sender_email = biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi().getEmail();
					}
					if (sender_phone_number == null || sender_phone_number.isEmpty()) {
						sender_phone_number = biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi()
								.getTelepon();
					}

					sender_phone_number = sender_phone_number.startsWith("08")
							? "+62" + sender_phone_number.substring(1)
							: sender_phone_number;
					sender_phone_number = sender_phone_number.startsWith("0") ? "+62" + sender_phone_number.substring(1)
							: sender_phone_number;
					sender_phone_number = !sender_phone_number.startsWith("+") ? "+62" + sender_phone_number
							: sender_phone_number;

					Map<String, Object> params = new HashMap<String, Object>();
					params.put("title", ket.trim());
					params.put("amount", mytotal);
					params.put("type", "SINGLE");
					params.put("step", 2);
					params.put("expired_date", Common.databaseDateFormat2.get().format(expired_date));
//					params.put("redirect_url", redirect_url);
					params.put("is_address_required", (sender_address.trim().isEmpty() ? 0 : 1));
					params.put("is_phone_number_required", (sender_phone_number.trim().isEmpty() ? 0 : 1));
					params.put("sender_name", sender_name);
					params.put("sender_email", sender_email);
					params.put("sender_phone_number", sender_phone_number);
					params.put("sender_address", sender_address);
					params.put("charge_fee", 1);
					String postData = URLBuilder.httpBuildQuery(params, "UTF-8");

					System.out.println(postData);

					URL url = new URL(strURL);
					HttpURLConnection con = (HttpURLConnection) url.openConnection();

					// CURLOPT_POST
					con.setRequestMethod("POST");

					// CURLOPT_FOLLOWLOCATION
					con.setInstanceFollowRedirects(true);

					String apiKeyFlip = Common.getKonfigurasi("flip_gateway_api_key_flip", "").getNilai();
					String tokenFlip = Common.getKonfigurasi("flip_gateway_api_token_flip", "").getNilai();

//					con.setRequestProperty("Content-length", String.valueOf(postData.length()));
					con.setRequestProperty("Authorization",
							DownloadTagihanSiswaBankOnline.getBasicAuthenticationHeader(apiKeyFlip, tokenFlip));

					con.setDoOutput(true);
					con.setDoInput(true);

					DataOutputStream output = new DataOutputStream(con.getOutputStream());
					output.writeBytes(postData);
					output.close();

					// "Post data send ... waiting for reply");
					int code = con.getResponseCode(); // 200 = HTTP_OK
					System.out.println("Response    (Code):" + code);
					System.out.println("Response (Message):" + con.getResponseMessage());

					// read the response
					DataInputStream input = new DataInputStream(con.getInputStream());
					int c;
					StringBuilder resultBuf = new StringBuilder();
					while ((c = input.read()) != -1) {
						resultBuf.append((char) c);
					}
					input.close();

					JSONObject jSONObject = new JSONObject(resultBuf.toString());

					System.out.println(jSONObject);

					virtualAccountBankOnline.setRequest(postData);
					virtualAccountBankOnline.setResponse(jSONObject.toString());

					if (!jSONObject.isNull("link_url")) {
						virtualAccountBankOnline.setLink(jSONObject.get("link_url") + "");
					}

					virtualAccountBankOnline.setKode(jSONObject.get("link_id") + "");
					virtualAccountBankOnline.setBank("Flip");

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:577");
					return null;
				}
			}

			else if (otto) {
				try {
					int mytotal = total.intValue() + biayaAdmin.intValue();
					JSONObject data = OttoUtil.post(biodataCalonMahasiswa, mytotal, virtualAccountBankOnline);

					virtualAccountBankOnline.setLink(data.getJSONObject("responseData").get("endpointUrl") + "");
					virtualAccountBankOnline.setKode(data.getJSONObject("responseData").get("orderId") + "");
					virtualAccountBankOnline.setBank("Otto");
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:591");
					return null;
				}
			}

			else if (briva) {
				try {
					int mytotal = total.intValue() + biayaAdmin.intValue();
					JSONObject data = BRIDataUtil.post(biodataCalonMahasiswa, mytotal, ket, virtualAccountBankOnline);

					expired_date = virtualAccountBankOnline.getKadaluarsaWaktu();

					virtualAccountBankOnline
							.setKode((data.getJSONObject("virtualAccountData").get("virtualAccountNo") + "").trim());
					virtualAccountBankOnline.setBank("BRI");
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:607");
					return null;
				}
			}

			else if (Common.bolehKonfigurasi("aktifkan_va_maja", Konfigurasi.TIDAK_AKTIF) || maja) {
				try {

					Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus();
					if (jurusan == null) {
						jurusan = biodataCalonMahasiswa.getProdi1();
					}

					JSONObject jsonObject = new JSONObject();
					String va = Common.getGeneratedAngkaDigit(10);
					int mytotal = total.intValue() + biayaAdmin.intValue();
					jsonObject.put("date", Common.databaseDateFormat.get().format(new Date()));
					jsonObject.put("amount", mytotal);
					jsonObject.put("name", biodataCalonMahasiswa.getNama());
					jsonObject.put("email", biodataCalonMahasiswa.getEmail());
					jsonObject.put("address", biodataCalonMahasiswa.getAlamat());
					jsonObject.put("va", va);
					jsonObject.put("attribute1", jurusan == null ? "" : jurusan.getFakultas().getNama());
					jsonObject.put("attribute2", jurusan == null ? "" : jurusan.getNama());
					jsonObject.put("attribute3", biodataCalonMahasiswa.getNoRegistrasi());
					jsonObject.put("attribute4", biodataCalonMahasiswa.getNoUjian());
					jsonObject.put("attribute5", biodataCalonMahasiswa.getProgram());
					jsonObject.put("items", items);
					jsonObject.put("attributes", new JSONArray());
					String CLIENT_TOKEN = null;
					try {
						CLIENT_TOKEN = BSIMajaUtil.sendRequestToken();
					} catch (Exception e1) {
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:640");
					}
					try {

						JSONObject jsonObject2 = BSIMajaUtil.sendRequest(jsonObject, CLIENT_TOKEN, true);
						virtualAccountBankOnline.setRequest(jsonObject.toString());
						virtualAccountBankOnline.setResponse(jsonObject2.toString());
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:648");
					}

					virtualAccountBankOnline.setKode(va);
					virtualAccountBankOnline.setBank("Maja");
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:654");
					return null;
				}
			}

			else if (Common.bolehKonfigurasi("aktifkan_va_jaring", Konfigurasi.TIDAK_AKTIF)) {
				String hasil = "";
				try {
					String va = Common.getGeneratedAngkaDigit(10);
					int mytotal = total.intValue() + biayaAdmin.intValue();
					JSONObject postData = new JSONObject();
					postData.put("custName", biodataCalonMahasiswa.getNama());
					postData.put("custID", biodataCalonMahasiswa.getNoRegistrasi());
					postData.put("trxID", va);
					postData.put("productID", Common.getKonfigurasi("va_jaring_produk_id", "207").getNilai());
					postData.put("paymentType", Common.getKonfigurasi("va_jaring_payment_type", "04").getNilai());
					postData.put("productName", myjadwalPembayaran.getJenisKegiatan().getNamaKegiatan());
					postData.put("amount", mytotal + "");
					postData.put("expire", Common.getKonfigurasi("va_jaring_expire", "1440").getNilai());
					postData.put("urlCallback", Common.getRequestHostWithProtocol() + "/Jaring");
					postData.put("timestamp", Common.databaseDateFormat1.get().format(WaktuUtil.getDate()));

					String strURL = Common.getKonfigurasi("va_jaring_gateway_url",
							"http://sandbox.jaring.host/api/v3/billpay/inquiry").getNilai();

					String screet_key = Common.getKonfigurasi("va_jaring_screet_key", "amFyaW5nOmphcmluZw==")
							.getNilai();

					String sign = postData.getString("custName") + postData.getString("custID")
							+ postData.getString("trxID") + postData.getString("productID")
							+ postData.getString("productName") + postData.getString("paymentType")
							+ postData.getString("timestamp") + postData.getString("amount")
							+ postData.getString("expire") + postData.getString("urlCallback") + screet_key;
					String token = DigestUtils.sha256Hex(sign);

					postData.put("signature", token);

					System.out.println(postData);

					String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H",
							"Authorization: Basic " + screet_key, "-X", "POST", strURL, "--data", postData.toString() };

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
					hasil = builder.toString();
					System.out.println(hasil);

					JSONObject jSONObject = new JSONObject(hasil);

					if (!jSONObject.getString("ack").equals("00")) {
						return null;
					}

					virtualAccountBankOnline.setRequest(postData.toString());
					virtualAccountBankOnline.setResponse(jSONObject.toString());

					virtualAccountBankOnline.setKode(jSONObject.getString("payCode"));
					virtualAccountBankOnline.setBank("Jaring");
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:721");
					return null;
				}

			}

			else if (Common.bolehKonfigurasi("aktifkan_va_e_smartlink", Konfigurasi.TIDAK_AKTIF) || smartlink) {

				String variableSmartlink = Common.getKonfigurasi("channel_biaya_e_smartlink",
						"VA_BNI:2500:BNI;VA_BRI:2500:BRI;VA_BCA:3500:BCA;VA_BNC:3500:BNC(Bank Neo Commerce);VA_CIMB:2500:CIMB Niaga;VA_MANDIRI:3500:Bank Mandiri;VA_PERMATA:2500:Bank Permata;VA_BSI:3000:BSI;VA_DANAMON:3000:Danamon;OTC_ALFAMART:3000:Alfamart;OTC_INDOMARET:3000:Indomart")
						.getNilai();

				if (virtualAccountBankOnline != null && virtualAccountBankOnline.getKanalPembayaran() != null) {
					variableSmartlink = virtualAccountBankOnline.getKanalPembayaran().getVariableBiayaAdminEsmartlink();
				}

				System.out.println("esmartlinkBayarVia -> " + esmartlinkBayarVia);
				if (!variableSmartlink.isEmpty() && esmartlinkBayarVia == null) {

					if (param.get("smartlink_direct") != null && ((Boolean) param.get("smartlink_direct"))) {

						String va = Common.getGeneratedBarCode();
						virtualAccountBankOnline.setLink(param.get("payment_url") + "");
						virtualAccountBankOnline.setKode(va);
						virtualAccountBankOnline.setBank("Esmartlink");

					} else {
						Double tabungan = null;
						Double topup = null;
						MyWindow window = new MyWindow("Pilih Channel Pembayaran", "none", true);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						MahasiswaSmartlinkChannelWindow.init(window, null, biodataCalonMahasiswa, 0, myjadwalPembayaran,
								detailBiayas, param, biayaAdmin, tabungan,topup, bankHost, ket, pemb, cicilan, total, items, true);
						window.setHeight("90%");
						window.setWidth("600px");
						window.setVisible(true);
						window.onModal();

						return null;
					}
				} else {
					String hasil = "";
					try {
						if (expired_date == null) {
							try {
								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
								expired_date = calendar.getTime();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:770");
							}
						}

						String va = Common.getGeneratedBarCode(30);
						int mytotal = total.intValue() + biayaAdmin.intValue();
						JSONObject postData = new JSONObject();
						postData.put("order_id", va);
						postData.put("amount", mytotal);
						postData.put("description", myjadwalPembayaran.getJenisKegiatan().getNamaKegiatan());
						JSONObject customer = new JSONObject();

						customer.put("name", biodataCalonMahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", ""));
						customer.put("email", biodataCalonMahasiswa.getEmail().split(",")[0].split(";")[0]);
						customer.put("phone", biodataCalonMahasiswa.getHp());

						postData.put("customer", customer);

						JSONArray itemsSmartlink = new JSONArray();
						for (int i = 0; i < items.length(); i++) {
							try {
								JSONObject d = items.getJSONObject(i);
								int amount = d.getInt("amount");
								if (amount > 0) {
									JSONObject jsonObject = new JSONObject();
									String description = d.getString("description");
									if (description.length() > 255) {
										description = description.substring(0, 255);
									}

									jsonObject.put("name", description);
									jsonObject.put("amount", amount);
									jsonObject.put("qty", 1);
									itemsSmartlink.put(jsonObject);
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:806");
							}
						}

						postData.put("item", itemsSmartlink);

						if (esmartlinkBayarVia == null) {
							String cannel_va_e_smartlink = Common
									.getKonfigurasi("cannel_va_e_smartlink", "VA_CIMB,VA_BRI").getNilai();
							String[] ch = cannel_va_e_smartlink.split(",");
							JSONArray channel = new JSONArray();
							for (int i = 0; i < ch.length; i++) {
								if (!ch[i].trim().isEmpty()) {
									channel.put(ch[i].trim());
								}
							}
							postData.put("channel", channel);
						} else {
							JSONArray channel = new JSONArray();
							channel.put(esmartlinkBayarVia);
							postData.put("channel", channel);
						}

						String link = Common.getRequestHostWithProtocol();
						if (Common.bolehKonfigurasi("dapatkan_code_via_url_custom", Konfigurasi.TIDAK_AKTIF)) {
							link = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol()).getNilai();
						}

						postData.put("type", "payment-page");
						postData.put("payment_mode", "CLOSE");
						postData.put("expired_time", Common.iso8601.get().format(expired_date));
						postData.put("callback_url", link + "/Esmartlink");
						postData.put("success_redirect_url", link + "/PembayaranSukses");
						postData.put("failed_redirect_url", link + "/PembayaranGagal");

						String strURL = Common
								.getKonfigurasi("gateway_url_va_e_smartlink",
										"https://payment-service-sbx.pakar-digital.com/api/payment/create-order")
								.getNilai();

						String username_va_e_smartlink = Common
								.getKonfigurasi("username_va_e_smartlink", "api-smartlink-sbx@budi-mulia.com")
								.getNilai().trim();
						String password_va_e_smartlink = Common
								.getKonfigurasi("password_va_e_smartlink", "sQ3f2PMbGWvNxvi").getNilai().trim();
						if (virtualAccountBankOnline != null && virtualAccountBankOnline.getKanalPembayaran() != null) {
							username_va_e_smartlink = virtualAccountBankOnline.getKanalPembayaran()
									.getUsernameEsmartlink();
							password_va_e_smartlink = virtualAccountBankOnline.getKanalPembayaran()
									.getPasswordEsmartlink();
						}

						hasil = VirtualAccountBank.curlSmartlink(strURL, username_va_e_smartlink,
								password_va_e_smartlink, postData);

						JSONObject jSONObject = new JSONObject(hasil);

						if (!(jSONObject.get("code") + "").equals("0")) {
							try {
								if (warnings != null) {
									warnings.add(jSONObject.getString("message"));
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:868");
								// TODO: handle exception
							}
							return null;
						}

						JSONObject data = jSONObject.getJSONObject("data");

						virtualAccountBankOnline.setRequest(postData.toString());
						virtualAccountBankOnline.setResponse(jSONObject.toString());
						virtualAccountBankOnline.setLink(data.get("payment_url") + "");
						virtualAccountBankOnline.setKode(va);
						virtualAccountBankOnline.setBank("Esmartlink");
					} catch (Exception e) {
						if (warnings != null) {
							warnings.add(e.getMessage());
						}
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:885");
						return null;
					}
				}
			}

			else if (Common.bolehKonfigurasi("aktifkan_va_bjb_langsung", Konfigurasi.TIDAK_AKTIF)) {

				try {
					String va = Common.getGeneratedAngkaDigit(12);
					int mytotal = total.intValue() + biayaAdmin.intValue();
					String hp = biodataCalonMahasiswa.getHp();

					String product_code = Common.maxPanjangAkhir(myjadwalPembayaran.getJenisKegiatan().getKode(), 2);

					JSONObject postData = new JSONObject();
					postData.put("customer_email", biodataCalonMahasiswa.getEmail());
					postData.put("billing_type", "f");
					postData.put("customer_code", va);
					postData.put("customer_phone", hp);
					postData.put("description", Common.maxPanjangAkhir(pemb, 1000));
					postData.put("client_refnum", va);
					postData.put("amount", mytotal + "");
					postData.put("customer_name", biodataCalonMahasiswa.getNama() == null ? ""
							: biodataCalonMahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", ""));
					postData.put("product_code", product_code);
					postData.put("cin", Common.getKonfigurasi("bjb_langsung_cin", "530").getNilai());

					postData.put("expired_date", Common.databaseDateFormat1.get().format(expired_date));
					postData.put("client_type", "1");
					postData.put("va_type", "m");
					postData.put("currency", "360");

					JSONObject jSONObject = BJBUtil.billingBJB(postData.toString(), true);

					virtualAccountBankOnline.setRequest(postData.toString());
					virtualAccountBankOnline.setResponse(jSONObject.toString());

					virtualAccountBankOnline.setKode(jSONObject.getString("va_number"));
					virtualAccountBankOnline.setBank("BJB");
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:926");
					return null;
				}

			} else {

				String prefix = myjadwalPembayaran != null && myjadwalPembayaran.getJenisKegiatan() != null
						&& myjadwalPembayaran.getJenisKegiatan().getPrefixKodePembayaran() != null
								? myjadwalPembayaran.getJenisKegiatan().getPrefixKodePembayaran()
								: Common.getKonfigurasi("prefix_va_bank_online", "").getNilai();

				String va = prefix + Common.getGeneratedAngkaDigit(10);
				virtualAccountBankOnline.setKode(va);
				virtualAccountBankOnline.setBank("Bank Online");
			}
			virtualAccountBankOnline.setOtomatis(false);
			virtualAccountBankOnline.setKadaluarsa(expired_date);
			virtualAccountBankOnline.setCicilan(cicilan);
			virtualAccountBankOnline.setJenisKegiatan(myjadwalPembayaran.getJenisKegiatan());
			virtualAccountBankOnline.setKeterangan(pemb + (qris ? "qris:true" : ""));
			virtualAccountBankOnline.setTotal(total);
			virtualAccountBankOnline.setBulanan("");
			virtualAccountBankOnline.setBiayaAdmin(biayaAdmin);
			virtualAccountBankOnline.setDetailbiaya(detailbiaya);

			virtualAccountBankOnline.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			virtualAccountBankOnline.setJadwalPembayaran(myjadwalPembayaran);
			virtualAccountBankOnline.setSemester(0);
			virtualAccountBankOnline.setTahunAkademik(myjadwalPembayaran.getTahunAkademik());

			MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
			if (virtualAccountBankOnline.getId() == null) {
				session.save(virtualAccountBankOnline);
			} else {
				session.update(virtualAccountBankOnline);
			}
			MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);

		}

		// session.disconnect();
		MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
			MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();

		return virtualAccountBankOnline;
	} finally {
			// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
			// finally menjamin penutupan walau terjadi exception (idempoten via isOpen()).
			if (session != null && session.isOpen()) {
				try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:975");}
				try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadNoRegistrasiCalonMahasiswaBankOnline.java:976");}
			}
		}
	}
}
