package ais.action.master.helper.virtualaccount;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.helper.util.SmartlinkChannelWindow;
import ais.common.BSIMajaUtil;
import ais.common.Common;
import ais.common.URLBuilder;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.Konfigurasi;
import ais.database.model.VirtualAccountBank;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.database.model.sekolah.KanalPembayaran;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class DownloadTagihanAnggotaKoperasiBankOnline {

	public static final String getBasicAuthenticationHeader(String username, String password) {
		String valueToEncode = username + ":" + password;
		return "Basic " + new String(org.apache.commons.codec.binary.Base64.encodeBase64(valueToEncode.getBytes()));
	}

	@SuppressWarnings({ "rawtypes" })
	public static VirtualAccountBank downloadData(AnggotaKoperasi anggotaKoperasi,
			Collection<TransaksiKoperasiDetail> tag, Map param, Double biayaAdmin, BankHost bankHost,
			CaraPembayaranKoperasi caraPembayaranKoperasi) throws Exception {
		List<String> warnings = null;
		return downloadData(anggotaKoperasi, tag, param, biayaAdmin, bankHost, caraPembayaranKoperasi, warnings);
	}

	@SuppressWarnings({ "rawtypes" })
	public static VirtualAccountBank downloadData(AnggotaKoperasi anggotaKoperasi,
			Collection<TransaksiKoperasiDetail> tag, Map param, Double biayaAdmin, BankHost bankHost,
			CaraPembayaranKoperasi caraPembayaranKoperasi, List<String> warnings) throws Exception {

		try {

			boolean qris = (Boolean) (param.get("qris") == null ? false : param.get("qris"));
			boolean flip = (Boolean) (param.get("flip") == null ? false : param.get("flip"));
			boolean esmartlink = (Boolean) (param.get("esmartlink") == null ? false : param.get("esmartlink"));
			String esmartlinkBayarVia = (String) (param.get("esmartlinkBayarVia") == null ? null
					: param.get("esmartlinkBayarVia"));
			boolean maja = (Boolean) (param.get("maja") == null ? false : param.get("maja"));

			boolean update = (Boolean) (param.get("update") == null ? false : param.get("update"));

			List<TransaksiKoperasiDetail> tagihans = new ArrayList<TransaksiKoperasiDetail>(tag);
			Collections.sort(tagihans);

			String cicilan = "";
			Double total = 0.0;

			JSONArray items = new JSONArray();

			if (biayaAdmin.intValue() > 0) {
				JSONObject jsonObjectitems = new JSONObject();
				jsonObjectitems.put("description", "Biaya Admin");
				jsonObjectitems.put("unitPrice", biayaAdmin.intValue());
				jsonObjectitems.put("qty", 1);
				jsonObjectitems.put("amount", biayaAdmin.intValue());
				items.put(jsonObjectitems);

			}

			String keterangan = "";
			String keteranganSimple = "";
			String keteranganSimpleBanget = "";
			for (TransaksiKoperasiDetail tagihan : tagihans) {

				String desc = tagihan.getId() + "-" + tagihan.getTransaksiKoperasi().getProdukKoperasi().getNama()
						+ " (ke " + tagihan.getKe() + ")" + ", ";

				keterangan += desc;

				String descSimple = tagihan.getTransaksiKoperasi().getProdukKoperasi().getNama() + " (ke "
						+ tagihan.getKe() + ")" + ", ";

				keteranganSimple += descSimple;

				String descSimpleBanget = tagihan.getId() + "";

				keteranganSimpleBanget += keteranganSimpleBanget.isEmpty() ? descSimpleBanget : ";" + descSimpleBanget;

				Double nilai = (tagihan.getPokok() + tagihan.getMargin());
				total += nilai;
				cicilan = MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan, ("Bulanan-" + tagihan.getId().toString() + "-" + nilai));

				JSONObject jsonObjectitems = new JSONObject();
				jsonObjectitems.put("description", desc);
				jsonObjectitems.put("unitPrice", nilai.intValue());
				jsonObjectitems.put("qty", 1);
				jsonObjectitems.put("amount", nilai.intValue());
				items.put(jsonObjectitems);
			}
			KanalPembayaran kanalPembayaran = caraPembayaranKoperasi.getKanalPembayaran();

			Session session = MahasiswaVirtualAccountHelper.openSession();
			try {

			boolean tagihan_expired_akhir_hari = Common
					.getKonfigurasi("tagihan_expired_akhir_hari", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
					.equals(Konfigurasi.AKTIF);
			Date expired_date = null;
			if (tagihan_expired_akhir_hari) {
				try {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.HOUR_OF_DAY, 23);
					calendar.set(Calendar.MINUTE, 59);
					calendar.set(Calendar.SECOND, 59);
					expired_date = calendar.getTime();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:135");
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
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:147");
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
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:160");
						}
					}
				}
			}
			VirtualAccountBank virtualAccountBankOnline = (VirtualAccountBank) session
					.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("terjadiKendala", false))
					.add(bankHost == null ? Restrictions.isNull("bankHost") : Restrictions.eq("bankHost", bankHost))
					.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
					.add(Restrictions.eq("keterangan", keterangan + (qris ? "qris:true" : "")))
					.add(Restrictions.eq("anggotaKoperasi", anggotaKoperasi))
					.add(Restrictions.isNull("deposit"))
					.add(Restrictions.isNull("kegiatan"))
					.add(Restrictions.isNull("pembayaran"))
					.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
			if (virtualAccountBankOnline == null || update) {

				if (virtualAccountBankOnline == null) {
					virtualAccountBankOnline = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());
				} else if (update && virtualAccountBankOnline != null) {
					virtualAccountBankOnline.setVa(null);
					virtualAccountBankOnline.setLink(null);
					virtualAccountBankOnline.setResponse(null);
				}

				virtualAccountBankOnline.setKanalPembayaran(kanalPembayaran);

				if (flip) {

					if (expired_date == null) {
						try {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
							expired_date = calendar.getTime();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:192");
						}
					}

					int mytotal = total.intValue() + biayaAdmin.intValue();

					String strURL = Common.getKonfigurasi("flip_gateway_url_v2", "https://bigflip.id/api/v2/pwf/bill")
							.getNilai();

//					String redirect_url = Common.getRequestHostWithProtocol() + "/Flip";

					String sender_address = "";
					if (anggotaKoperasi != null && !anggotaKoperasi.getAlamat().isEmpty()) {
						sender_address = anggotaKoperasi.getAlamat();
					}
					String sender_name = "";
					if (anggotaKoperasi != null) {
						sender_name = anggotaKoperasi.getNama();
					}
//					String sender_email = sekolah.getEmail();
//					if (calonAnggotaKoperasi != null && !calonAnggotaKoperasi.getAlamatEmail().isEmpty()) {
//					sender_email = calonAnggotaKoperasi.getAlamatEmail();
//					} else if (anggotaKoperasi != null && !anggotaKoperasi.getAlamatEmail().isEmpty()) {
//						sender_email = anggotaKoperasi.getAlamatEmail();
//					}

					String sender_email = "";
					if (anggotaKoperasi != null) {
						sender_email = anggotaKoperasi.generateEmail();
					}

					String sender_phone_number = "";
					if (anggotaKoperasi != null && !anggotaKoperasi.getHp().isEmpty()
							&& anggotaKoperasi.getHp().length() > 8 && anggotaKoperasi.getHp().length() < 15) {
						sender_phone_number = anggotaKoperasi.getHp();
					}

					Map<String, Object> params = new HashMap<String, Object>();
					params.put("title", keteranganSimple.trim());
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

//					con.setRequestProperty("Content-length", String.valueOf(postData.length()));
					con.setRequestProperty("Authorization", getBasicAuthenticationHeader(
							kanalPembayaran.getApiKeyFlip(), kanalPembayaran.getTokenFlip()));

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

				}

				else if (esmartlink) {

					if (virtualAccountBankOnline.getResponse() == null
							|| virtualAccountBankOnline.getResponse().trim().isEmpty()) {
						String usernameEsmartlink = kanalPembayaran.getUsernameEsmartlink();
						String passwordEsmartlink = kanalPembayaran.getPasswordEsmartlink();

						if (kanalPembayaran != null) {
							usernameEsmartlink = kanalPembayaran.getUsernameEsmartlink();
							passwordEsmartlink = kanalPembayaran.getPasswordEsmartlink();
						}

						if (!kanalPembayaran.getVariableBiayaAdminEsmartlink().isEmpty()
								&& esmartlinkBayarVia == null) {
							MyWindow window = new MyWindow("Pilih Channel Pembayaran", "none", true);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							SmartlinkChannelWindow.init(window, anggotaKoperasi, tag, param, biayaAdmin, bankHost,
									caraPembayaranKoperasi, keterangan, total, true, kanalPembayaran);
							window.setHeight("90%");
							window.setWidth("600px");
							window.setVisible(true);
							window.onModal();

							return null;
						} else {

							if (expired_date == null) {
								try {
									Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
									calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
									expired_date = calendar.getTime();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:330");
								}
							}

							String hasil = "";
							try {
								if (expired_date == null) {
									try {
										Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
										calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
										expired_date = calendar.getTime();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:342");
									}
								}

								String sender_name = "";
								if (anggotaKoperasi != null) {
									sender_name = anggotaKoperasi.getNama();
								}
								String sender_email = "";
								if (anggotaKoperasi != null && !anggotaKoperasi.getEmail().isEmpty()) {
									sender_email = anggotaKoperasi.getEmail();
								}

								String sender_phone_number = "";
								if (anggotaKoperasi != null && !anggotaKoperasi.getHp().isEmpty()
										&& anggotaKoperasi.getHp().length() > 8
										&& anggotaKoperasi.getHp().length() < 15) {
									sender_phone_number = anggotaKoperasi.getHp();
								}

								String va = Common.getGeneratedBarCode(30);
								int mytotal = total.intValue() + biayaAdmin.intValue();
								JSONObject postData = new JSONObject();
								postData.put("order_id", va);
								postData.put("amount", mytotal);
								postData.put("description", keteranganSimpleBanget);
								JSONObject customer = new JSONObject();

								customer.put("name", sender_name.replaceAll("[^\\sa-zA-Z0-9]", ""));
								customer.put("email", sender_email);
								customer.put("phone", sender_phone_number);

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
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:394");
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
									link = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol())
											.getNilai();
								}

								postData.put("type", "payment-page");
								postData.put("payment_mode", "CLOSE");
								postData.put("expired_time", Common.iso8601.get().format(expired_date));
								postData.put("callback_url", link + "/Esmartlink");
								postData.put("success_redirect_url", link + "/PembayaranSukses");
								postData.put("failed_redirect_url", link + "/PembayaranGagal");

								String strURL = Common.getKonfigurasi("gateway_url_va_e_smartlink",
										"https://payment-service-sbx.pakar-digital.com/api/payment/create-order")
										.getNilai();

								hasil = VirtualAccountBank.curlSmartlink(strURL, usernameEsmartlink, passwordEsmartlink,
										postData);

								JSONObject jSONObject = new JSONObject(hasil);

								if (!(jSONObject.get("code") + "").equals("0")) {
									return null;
								}

								JSONObject data = jSONObject.getJSONObject("data");

								virtualAccountBankOnline.setRequest(postData.toString());
								virtualAccountBankOnline.setResponse(jSONObject.toString());
								virtualAccountBankOnline.setLink(data.get("payment_url") + "");
								virtualAccountBankOnline.setKode(va);
								virtualAccountBankOnline.setBank("Esmartlink");

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:453");
								return null;
							}
						}
					}
				}

				else if (maja || Common.bolehKonfigurasi("aktifkan_va_maja", Konfigurasi.TIDAK_AKTIF)) {
					try {

						JSONObject jsonObject = new JSONObject();
						String va = Common.getGeneratedAngkaDigit(10);
						int mytotal = total.intValue() + biayaAdmin.intValue();
						jsonObject.put("date", Common.databaseDateFormat.get().format(new Date()));
						if (Common.bolehKonfigurasi("maja_pakai_tanpa_amount")) {
							jsonObject.put("amount", mytotal);
						}

						jsonObject.put("name", anggotaKoperasi.getNama());
						jsonObject.put("email", anggotaKoperasi.getEmail());
						jsonObject.put("address", anggotaKoperasi.getAlamat());
						jsonObject.put("va", va);
						jsonObject.put("attribute1", anggotaKoperasi.getKoperasi().getNama());
						jsonObject.put("attribute2", anggotaKoperasi.getJenisAnggotaKoperasi() == null ? ""
								: anggotaKoperasi.getJenisAnggotaKoperasi().getNama());
						jsonObject.put("attribute3", anggotaKoperasi.getTipeAnggotaKoperasi() == null ? ""
								: anggotaKoperasi.getTipeAnggotaKoperasi().getNama());
						jsonObject.put("attribute4", anggotaKoperasi.getKode());
						jsonObject.put("attribute5", anggotaKoperasi.getKeterangan());

						jsonObject.put("items", items);
						jsonObject.put("attributes", new JSONArray());
						String CLIENT_TOKEN = null;
						try {
							CLIENT_TOKEN = BSIMajaUtil.sendRequestToken(null, kanalPembayaran);
						} catch (Exception e1) {
							e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:489");
						}

						try {
							JSONObject jsonObject2 = BSIMajaUtil.sendRequest(jsonObject, CLIENT_TOKEN, null,
									kanalPembayaran, true);
							virtualAccountBankOnline.setRequest(jsonObject.toString());
							virtualAccountBankOnline.setResponse(jsonObject2.toString());
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:498");
						}

						virtualAccountBankOnline.setKode(va);
						virtualAccountBankOnline.setBank("Maja");
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:504");
						return null;
					}
				}

				else {

					int jml_digit_prefix_va_bank_online = 10;
					try {
						jml_digit_prefix_va_bank_online = Integer.parseInt(Common
								.getKonfigurasi("jml_digit_prefix_va_bank_online", jml_digit_prefix_va_bank_online + "")
								.getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:516");
						// TODO: handle exception
					}

					String va = Common.getKonfigurasi("prefix_va_bank_online", "").getNilai()
							+ Common.getGeneratedAngkaDigit(jml_digit_prefix_va_bank_online);

					virtualAccountBankOnline.setKode(va);
					virtualAccountBankOnline.setBank("Bank Online 2");
				}
				virtualAccountBankOnline.setKadaluarsa(expired_date);
				virtualAccountBankOnline.setOtomatis(false);

				virtualAccountBankOnline.setCicilan(cicilan);
				virtualAccountBankOnline.setKeterangan(keterangan + (qris ? "qris:true" : ""));
				virtualAccountBankOnline.setTotal(total);
				virtualAccountBankOnline.setBulanan("");
				virtualAccountBankOnline.setBiayaAdmin(biayaAdmin);

				virtualAccountBankOnline.setAnggotaKoperasi(anggotaKoperasi);
				virtualAccountBankOnline.setBankHost(bankHost);
				virtualAccountBankOnline.setCaraPembayaranKoperasi(caraPembayaranKoperasi);

				MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
				session.saveOrUpdate(virtualAccountBankOnline);
				MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);

				for (TransaksiKoperasiDetail tagihan : tagihans) {
					tagihan.setVa(virtualAccountBankOnline.getKode());
					tagihan.setExpired(expired_date);
					tagihan.setLink(virtualAccountBankOnline.getLink());
					MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
					session.update(tagihan);
					MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);
				}
			}
			return virtualAccountBankOnline;
		} finally {
			// Tutup session yang DIBUKA di FINALLY -> tak bocor pada jalur exception.
			MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
			MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();
		}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes" })
	public static VirtualAccountBank downloadData(AnggotaKoperasi anggotaKoperasi, Double topup, Double biayaAdmin,
			Map param, BankHost bankHost, CaraPembayaranKoperasi caraPembayaranKoperasi) throws Exception {
		List<String> warnings = null;
		return downloadData(anggotaKoperasi, topup, biayaAdmin, param, bankHost, caraPembayaranKoperasi, warnings);
	}

	@SuppressWarnings({ "rawtypes" })
	public static VirtualAccountBank downloadData(AnggotaKoperasi anggotaKoperasi, Double topup, Double biayaAdmin,
			Map param, BankHost bankHost, CaraPembayaranKoperasi caraPembayaranKoperasi, List<String> warnings)
			throws Exception {

		try {

			boolean qris = (Boolean) (param.get("qris") == null ? false : param.get("qris"));
			boolean flip = (Boolean) (param.get("flip") == null ? false : param.get("flip"));
			boolean esmartlink = (Boolean) (param.get("esmartlink") == null ? false : param.get("esmartlink"));
			String esmartlinkBayarVia = (String) (param.get("esmartlinkBayarVia") == null ? null
					: param.get("esmartlinkBayarVia"));
			boolean maja = (Boolean) (param.get("maja") == null ? false : param.get("maja"));

			boolean update = (Boolean) (param.get("update") == null ? false : param.get("update"));

			String cicilan = "";
			Double total = topup;

			JSONArray items = new JSONArray();

			if (topup.intValue() > 0) {
				JSONObject jsonObjectitems = new JSONObject();
				jsonObjectitems.put("description", "Biaya Topup");
				jsonObjectitems.put("unitPrice", topup.intValue());
				jsonObjectitems.put("qty", 1);
				jsonObjectitems.put("amount", topup.intValue());
				items.put(jsonObjectitems);

			}

			if (biayaAdmin.intValue() > 0) {
				JSONObject jsonObjectitems = new JSONObject();
				jsonObjectitems.put("description", "Biaya Admin");
				jsonObjectitems.put("unitPrice", biayaAdmin.intValue());
				jsonObjectitems.put("qty", 1);
				jsonObjectitems.put("amount", biayaAdmin.intValue());
				items.put(jsonObjectitems);

			}

			String keterangan = "Topup senilai " + Common.numberFormat.get().format(topup);
			String keteranganSimple = keterangan;
			String keteranganSimpleBanget = keterangan;

			KanalPembayaran kanalPembayaran = caraPembayaranKoperasi.getKanalPembayaran();

			Session session = MahasiswaVirtualAccountHelper.openSession();
			try {

			boolean tagihan_expired_akhir_hari = Common
					.getKonfigurasi("tagihan_expired_akhir_hari", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
					.equals(Konfigurasi.AKTIF);
			Date expired_date = null;
			if (tagihan_expired_akhir_hari) {
				try {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.HOUR_OF_DAY, 23);
					calendar.set(Calendar.MINUTE, 59);
					calendar.set(Calendar.SECOND, 59);
					expired_date = calendar.getTime();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:633");
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
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:645");
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
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:658");
						}
					}
				}
			}
			VirtualAccountBank virtualAccountBankOnline = (VirtualAccountBank) session
					.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("terjadiKendala", false))
					.add(bankHost == null ? Restrictions.isNull("bankHost") : Restrictions.eq("bankHost", bankHost))
					.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
					.add(Restrictions.eq("keterangan", keterangan + (qris ? "qris:true" : "")))
					.add(Restrictions.eq("anggotaKoperasi", anggotaKoperasi))
					.add(Restrictions.isNull("deposit"))
					.add(Restrictions.isNull("kegiatan"))
					.add(Restrictions.isNull("pembayaran"))
					.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
			if (virtualAccountBankOnline == null || update) {

				if (virtualAccountBankOnline == null) {
					virtualAccountBankOnline = new VirtualAccountBank(PerguruanTinggiUtil.getPerguruanTinggi().getId());
				} else if (update && virtualAccountBankOnline != null) {
					virtualAccountBankOnline.setVa(null);
					virtualAccountBankOnline.setLink(null);
					virtualAccountBankOnline.setResponse(null);
				}

				virtualAccountBankOnline.setKanalPembayaran(kanalPembayaran);

				if (flip) {

					if (expired_date == null) {
						try {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
							expired_date = calendar.getTime();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:690");
						}
					}

					int mytotal = total.intValue() + biayaAdmin.intValue();

					String strURL = Common.getKonfigurasi("flip_gateway_url_v2", "https://bigflip.id/api/v2/pwf/bill")
							.getNilai();

//					String redirect_url = Common.getRequestHostWithProtocol() + "/Flip";

					String sender_address = "";
					if (anggotaKoperasi != null && !anggotaKoperasi.getAlamat().isEmpty()) {
						sender_address = anggotaKoperasi.getAlamat();
					}
					String sender_name = "";
					if (anggotaKoperasi != null) {
						sender_name = anggotaKoperasi.getNama();
					}
//					String sender_email = sekolah.getEmail();
//					if (calonAnggotaKoperasi != null && !calonAnggotaKoperasi.getAlamatEmail().isEmpty()) {
//					sender_email = calonAnggotaKoperasi.getAlamatEmail();
//					} else if (anggotaKoperasi != null && !anggotaKoperasi.getAlamatEmail().isEmpty()) {
//						sender_email = anggotaKoperasi.getAlamatEmail();
//					}

					String sender_email = "";
					if (anggotaKoperasi != null) {
						sender_email = anggotaKoperasi.generateEmail();
					}

					String sender_phone_number = "";
					if (anggotaKoperasi != null && !anggotaKoperasi.getHp().isEmpty()
							&& anggotaKoperasi.getHp().length() > 8 && anggotaKoperasi.getHp().length() < 15) {
						sender_phone_number = anggotaKoperasi.getHp();
					}

					Map<String, Object> params = new HashMap<String, Object>();
					params.put("title", keteranganSimple.trim());
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

//					con.setRequestProperty("Content-length", String.valueOf(postData.length()));
					con.setRequestProperty("Authorization", getBasicAuthenticationHeader(
							kanalPembayaran.getApiKeyFlip(), kanalPembayaran.getTokenFlip()));

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

				}

				else if (esmartlink) {

					if (virtualAccountBankOnline.getResponse() == null
							|| virtualAccountBankOnline.getResponse().trim().isEmpty()) {
						String usernameEsmartlink = kanalPembayaran.getUsernameEsmartlink();
						String passwordEsmartlink = kanalPembayaran.getPasswordEsmartlink();

						if (kanalPembayaran != null) {
							usernameEsmartlink = kanalPembayaran.getUsernameEsmartlink();
							passwordEsmartlink = kanalPembayaran.getPasswordEsmartlink();
						}

						if (!kanalPembayaran.getVariableBiayaAdminEsmartlink().isEmpty()
								&& esmartlinkBayarVia == null) {
							MyWindow window = new MyWindow("Pilih Channel Pembayaran", "none", true);
							window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

							SmartlinkChannelWindow.init(window, anggotaKoperasi, param, 0.0, bankHost,
									caraPembayaranKoperasi, keterangan, total, true, kanalPembayaran);

							window.setHeight("90%");
							window.setWidth("600px");
							window.setVisible(true);
							window.onModal();

							return null;
						} else {

							if (expired_date == null) {
								try {
									Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
									calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
									expired_date = calendar.getTime();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:830");
								}
							}

							String hasil = "";
							try {
								if (expired_date == null) {
									try {
										Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
										calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
										expired_date = calendar.getTime();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:842");
									}
								}

								String sender_name = "";
								if (anggotaKoperasi != null) {
									sender_name = anggotaKoperasi.getNama();
								}
								String sender_email = "";
								if (anggotaKoperasi != null && !anggotaKoperasi.getEmail().isEmpty()) {
									sender_email = anggotaKoperasi.getEmail();
								}

								String sender_phone_number = "";
								if (anggotaKoperasi != null && !anggotaKoperasi.getHp().isEmpty()
										&& anggotaKoperasi.getHp().length() > 8
										&& anggotaKoperasi.getHp().length() < 15) {
									sender_phone_number = anggotaKoperasi.getHp();
								}

								String va = Common.getGeneratedBarCode(30);
								int mytotal = total.intValue() + biayaAdmin.intValue();
								JSONObject postData = new JSONObject();
								postData.put("order_id", va);
								postData.put("amount", mytotal);
								postData.put("description", keteranganSimpleBanget);
								JSONObject customer = new JSONObject();

								customer.put("name", sender_name.replaceAll("[^\\sa-zA-Z0-9]", ""));
								customer.put("email", sender_email);
								customer.put("phone", sender_phone_number);

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
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:894");
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
									link = Common.getKonfigurasi("CURRENT_URL", Common.getRequestHostWithProtocol())
											.getNilai();
								}

								postData.put("type", "payment-page");
								postData.put("payment_mode", "CLOSE");
								postData.put("expired_time", Common.iso8601.get().format(expired_date));
								postData.put("callback_url", link + "/Esmartlink");
								postData.put("success_redirect_url", link + "/PembayaranSukses");
								postData.put("failed_redirect_url", link + "/PembayaranGagal");

								String strURL = Common.getKonfigurasi("gateway_url_va_e_smartlink",
										"https://payment-service-sbx.pakar-digital.com/api/payment/create-order")
										.getNilai();

								hasil = VirtualAccountBank.curlSmartlink(strURL, usernameEsmartlink, passwordEsmartlink,
										postData);

								JSONObject jSONObject = new JSONObject(hasil);

								if (!(jSONObject.get("code") + "").equals("0")) {
									return null;
								}

								JSONObject data = jSONObject.getJSONObject("data");

								virtualAccountBankOnline.setRequest(postData.toString());
								virtualAccountBankOnline.setResponse(jSONObject.toString());
								virtualAccountBankOnline.setLink(data.get("payment_url") + "");
								virtualAccountBankOnline.setKode(va);
								virtualAccountBankOnline.setBank("Esmartlink");

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:953");
								return null;
							}
						}
					}
				}

				else if (maja || Common.bolehKonfigurasi("aktifkan_va_maja", Konfigurasi.TIDAK_AKTIF)) {
					try {

						JSONObject jsonObject = new JSONObject();
						String va = Common.getGeneratedAngkaDigit(10);
						int mytotal = total.intValue() + biayaAdmin.intValue();
						jsonObject.put("date", Common.databaseDateFormat.get().format(new Date()));
						if (Common.bolehKonfigurasi("maja_pakai_tanpa_amount")) {
							jsonObject.put("amount", mytotal);
						}

						jsonObject.put("name", anggotaKoperasi.getNama());
						jsonObject.put("email", anggotaKoperasi.getEmail());
						jsonObject.put("address", anggotaKoperasi.getAlamat());
						jsonObject.put("va", va);
						jsonObject.put("attribute1", anggotaKoperasi.getKoperasi().getNama());
						jsonObject.put("attribute2", anggotaKoperasi.getJenisAnggotaKoperasi() == null ? ""
								: anggotaKoperasi.getJenisAnggotaKoperasi().getNama());
						jsonObject.put("attribute3", anggotaKoperasi.getTipeAnggotaKoperasi() == null ? ""
								: anggotaKoperasi.getTipeAnggotaKoperasi().getNama());
						jsonObject.put("attribute4", anggotaKoperasi.getKode());
						jsonObject.put("attribute5", anggotaKoperasi.getKeterangan());

						jsonObject.put("items", items);
						jsonObject.put("attributes", new JSONArray());
						String CLIENT_TOKEN = null;
						try {
							CLIENT_TOKEN = BSIMajaUtil.sendRequestToken(null, kanalPembayaran);
						} catch (Exception e1) {
							e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:989");
						}

						try {
							JSONObject jsonObject2 = BSIMajaUtil.sendRequest(jsonObject, CLIENT_TOKEN, null,
									kanalPembayaran, true);
							virtualAccountBankOnline.setRequest(jsonObject.toString());
							virtualAccountBankOnline.setResponse(jsonObject2.toString());
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:998");
						}

						virtualAccountBankOnline.setKode(va);
						virtualAccountBankOnline.setBank("Maja");
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:1004");
						return null;
					}
				}

				else {

					int jml_digit_prefix_va_bank_online = 10;
					try {
						jml_digit_prefix_va_bank_online = Integer.parseInt(Common
								.getKonfigurasi("jml_digit_prefix_va_bank_online", jml_digit_prefix_va_bank_online + "")
								.getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanAnggotaKoperasiBankOnline.java:1016");
						// TODO: handle exception
					}

					String va = Common.getKonfigurasi("prefix_va_bank_online", "").getNilai()
							+ Common.getGeneratedAngkaDigit(jml_digit_prefix_va_bank_online);

					virtualAccountBankOnline.setKode(va);
					virtualAccountBankOnline.setBank("Bank Online 2");
				}
				virtualAccountBankOnline.setKadaluarsa(expired_date);
				virtualAccountBankOnline.setOtomatis(false);

				virtualAccountBankOnline.setCicilan(cicilan);
				virtualAccountBankOnline.setKeterangan(keterangan + (qris ? "qris:true" : ""));
				virtualAccountBankOnline.setTotal(total);
				virtualAccountBankOnline.setBulanan("");
				virtualAccountBankOnline.setBiayaAdmin(biayaAdmin);

				virtualAccountBankOnline.setAnggotaKoperasi(anggotaKoperasi);
				virtualAccountBankOnline.setBankHost(bankHost);
				virtualAccountBankOnline.setCaraPembayaranKoperasi(caraPembayaranKoperasi);

				MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
				session.saveOrUpdate(virtualAccountBankOnline);
				MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);

			}
			return virtualAccountBankOnline;
		} finally {
			// Tutup session yang DIBUKA di FINALLY -> tak bocor pada jalur exception.
			MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
			MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();
		}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return null;
	}

	public static void main(String[] argv) {
		String customer_code = "2010630100001";
		System.out.println(Common.maxPanjangAkhir(customer_code, 12));
	}
}
