package ais.common;

import java.io.File;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Mahasiswa;
import ais.database.model.faspay.FaspayRequest;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyWindow;

public class FaspayKeranjangPembayaran {

	@SuppressWarnings({ "deprecation" })
	public static boolean onSaveFaspay(final Double amn, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final Set<KegiatanTemporary> selectedKegiatanTemporary,
			final Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		String strURL = (Common.getKonfigurasi("faspay_payment_channel_url",
				"http://faspaydev.mediaindonusa.com/pws/100001/182xx00010100000").getNilai());

		String merchant_id = Common.getKonfigurasi("faspay_merchant_id", "31503").getNilai().trim();
		String merchant = Common.getKonfigurasi("faspay_merchant_name", "eCampus").getNilai().trim();
		String UserID = Common.getKonfigurasi("faspay_user_id", "bot31503").getNilai().trim();
		String Password = Common.getKonfigurasi("faspay_password", "W4TYRmO0").getNilai().trim();

		String signature = AeSimpleSHA1.SHA1(MD5.crypt(UserID + Password));

		String postData = "<?xml version=\"1.0\"?>\n<faspay>\n"
				+ "<request>Request List of Payment Gateway</request>\n<merchant_id>" + merchant_id
				+ "</merchant_id>\n<merchant>" + merchant + "</merchant>\n<signature>" + signature
				+ "</signature>\n</faspay>";

		System.out.println("postData = " + postData);

		PostMethod post = new PostMethod(strURL);
		try {
			StringRequestEntity requestEntity = new StringRequestEntity(postData);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Content-type", "text/xml; charset=ISO-8859-1");
			HttpClient httpclient = new HttpClient();

			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");
			String hasil = post.getResponseBodyAsString();
			System.out.println(hasil);

			JSONObject jSONObject = XML.toJSONObject(hasil);
			JSONObject faspay = jSONObject.getJSONObject("faspay");
			TreeMap<String, String> channel = new TreeMap<String, String>();
			try {
				JSONArray jsonArray = faspay.getJSONArray("payment_channel");
				System.out.println("jsonArray = " + jsonArray);
				for (int i = 0; i < jsonArray.length(); i++) {
					try {
						JSONObject json = jsonArray.getJSONObject(i);
						System.out.println("json = " + json);
						channel.put(json.get("pg_code").toString(), json.get("pg_name").toString());
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e); 
					}
				}
			} catch (Exception e) {
				try {
					JSONObject json = faspay.getJSONObject("payment_channel");
					System.out.println("json = " + json);
					channel.put(json.get("pg_code").toString(), json.get("pg_name").toString());
				} catch (Exception ee) {
					Common.tampilErrorJikaAdmin(ee); 
				}
			}

			if (channel.size() == 1) {
				String kode = channel.keySet().iterator().next();
				String nama = channel.get(kode);
				onPilihFaspay(amn, mahasiswa, biodataCalonMahasiswa, selectedKegiatanTemporary, kode, nama, event);
			} else if (!channel.isEmpty()) {
				final MyWindow window = new MyWindow("Pilihlah salah satu kanal pembayaran", "none", false);
				window.setHeight("300px");
				window.setWidth("500px");

				Radiogroup radiogroup = new Radiogroup();
				radiogroup.setParent(window);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(radiogroup);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Rows rows = new Rows();
				rows.setParent(grid);
				for (final String kode : channel.keySet()) {
					final String nama = channel.get(kode);
					Row row = new Row();row.setValign("top");
					row.setParent(rows);
					MyRadioConfig radio = new MyRadioConfig(kode + " - " + nama);
					radio.setParent(row);
					radio.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onPilihFaspay(amn, mahasiswa, biodataCalonMahasiswa, selectedKegiatanTemporary,
											kode, nama, event);
								}
							});
							window.detach();

						}
					});
				}

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.onModal();

			} else {
				MyMessageboxConfig.show("Kanal pembayaran tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}

		} catch (Exception e) {
			MyMessageboxConfig.show("Kanal pembayaran tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			Common.tampilErrorJikaAdmin(e); 
		} finally {
			post.releaseConnection();
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public static boolean onPilihFaspay(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, final Set<KegiatanTemporary> selectedKegiatanTemporary,
			String payment_channel, String payment_channel_name, Event event) throws Exception {

		String merchant_id = Common.getKonfigurasi("faspay_merchant_id", "31503").getNilai().trim();
		String merchant = Common.getKonfigurasi("faspay_merchant_name", "eCampus").getNilai().trim();
		String UserID = Common.getKonfigurasi("faspay_user_id", "bot31503").getNilai().trim();
		String Password = Common.getKonfigurasi("faspay_password", "W4TYRmO0").getNilai().trim();

		StringBuilder items = new StringBuilder();
		Session session = null;

		try {
			session = HibernateUtil.currentNativeSession();

			for (KegiatanTemporary kegiatanTemporary : selectedKegiatanTemporary) {
				List<CicilanPembayaran> cicilanPembayarans = session.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("kegiatanTemporary", kegiatanTemporary)).list();
				for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
					if (cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
						String item = "<item>\n<product>"
								+ cicilanPembayaran.getPengaturanPembayaranBulanan().getDetailBiaya().getItemBiaya()
										.getNama()
								+ " bulan " + cicilanPembayaran.getPengaturanPembayaranBulanan().getNamaBulan()
								+ "</product>\n<qty>1</qty>\n<amount>" + cicilanPembayaran.getNilai().intValue()
								+ "00</amount>\n"
								+ "<payment_plan>01</payment_plan>\n<merchant_id></merchant_id>\n<tenor>00</tenor>\n"
								+ "</item>\n";
						items.append(item);
					} else {
						String item = "<item>\n<product>" + cicilanPembayaran.getItemBiaya().getNama()
								+ "</product>\n<qty>1</qty>\n<amount>" + cicilanPembayaran.getNilai().intValue()
								+ "00</amount>\n"
								+ "<payment_plan>01</payment_plan>\n<merchant_id></merchant_id>\n<tenor>00</tenor>\n"
								+ "</item>\n";
						items.append(item);
					}

				}
			}

			Double biayaAdministrasi = 0.0;
			try {
				biayaAdministrasi = Double
						.parseDouble(Common.getKonfigurasi("faspay_biaya_administrasi", "0.0").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/FaspayKeranjangPembayaran.java:222");

			}

			if (biayaAdministrasi > 0.1) {
				String item = "<item>\n<product>Biaya Administrasi</product>\n<qty>1</qty>\n<amount>"
						+ biayaAdministrasi.intValue() + "00</amount>\n"
						+ "<payment_plan>01</payment_plan>\n<merchant_id></merchant_id>\n<tenor>00</tenor>\n"
						+ "</item>\n";
				items.append(item);
			}

			String bill_no = Common.getGeneratedBarCode();

			String signature = AeSimpleSHA1.SHA1(MD5.crypt(UserID + Password + bill_no));

			System.out.println("signature = " + signature);

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) + 12);

			String cust_name = "";
			String cust_no = "";
			String msisdn = "";
			String email = "";
			String address = "";
			String city = "";
			String region = "";
			String poscode = "";
			if (mahasiswa != null) {
				cust_name = mahasiswa.getNama();
				cust_no = mahasiswa.getNim();
				email = mahasiswa.getEmail().split(",")[0];
				address = mahasiswa.getAlamat();
				BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();
				if (biodataMahasiswa != null) {
					city = biodataMahasiswa.getKota() == null ? "" : biodataMahasiswa.getKota().getNama();
					region = biodataMahasiswa.getPropinsi() == null ? "" : biodataMahasiswa.getPropinsi().getNama();
					poscode = biodataMahasiswa.getKodepos();
					msisdn = biodataMahasiswa.getHp();
					if (!Common.isNumber(msisdn.replaceAll("\\+", "").trim())) {
						msisdn = "0810000000";
					}
				}
			} else if (biodataCalonMahasiswa != null) {
				cust_name = biodataCalonMahasiswa.getNama();
				cust_no = biodataCalonMahasiswa.getNoRegistrasi();
				msisdn = biodataCalonMahasiswa.getTeleponRumah();
				if (!Common.isNumber(msisdn.replaceAll("\\+", "").trim())) {
					msisdn = "0810000000";
				}
				email = biodataCalonMahasiswa.getEmail().split(",")[0];
				address = biodataCalonMahasiswa.getAlamat();
				city = biodataCalonMahasiswa.getKotaCalon() == null ? ""
						: biodataCalonMahasiswa.getKotaCalon().getNama();
				region = biodataCalonMahasiswa.getPropinsiCalon() == null ? ""
						: biodataCalonMahasiswa.getPropinsiCalon().getNama();
				poscode = biodataCalonMahasiswa.getKodePos();
			}

			StringBuilder billDesc = new StringBuilder();
			for (KegiatanTemporary kegiatanTemporary : selectedKegiatanTemporary) {
				if (billDesc.length() > 0) {
					billDesc.append(",");
				}
				billDesc.append(kegiatanTemporary.getJenisKegiatan().getNamaKegiatan());
			}

			String postData = "<faspay>\n<request>Post Data Transaksi</request>\n<merchant_id>" + merchant_id
					+ "</merchant_id>\n<merchant>" + merchant + "</merchant>\n<bill_no>" + bill_no + "</bill_no>\n"
					+ "<bill_reff>" + cust_no + "</bill_reff>\n<bill_date>" + Common.databaseDateFormat1.get().format(ais.ui.util.WaktuUtil.getDate())
					+ "</bill_date>\n<bill_expired>" + Common.databaseDateFormat1.get().format(calendar.getTime()) + "</bill_expired>\n"
					+ "<bill_desc>" + billDesc.toString() + "</bill_desc>\n<bill_currency>IDR</bill_currency>\n" + "<bill_gross>"
					+ (biayaAdministrasi.intValue() + amn.intValue()) + "00</bill_gross>\n<bill_tax>0</bill_tax>\n"
					+ "<bill_miscfee>0</bill_miscfee>\n<bill_total>" + (biayaAdministrasi.intValue() + amn.intValue())
					+ "00</bill_total>\n<cust_no>" + cust_no + "</cust_no>\n<cust_name>" + cust_name
					+ "</cust_name>\n<payment_channel>" + payment_channel
					+ "</payment_channel>\n<pay_type>1</pay_type>\n<bank_userid>" + cust_no + "</bank_userid>\n<msisdn>"
					+ msisdn + "</msisdn>\n<email>" + email + "</email>\n<terminal>10</terminal>\n<billing_address>"
					+ address + "</billing_address>\n" + "<billing_address_city>" + city
					+ "</billing_address_city>\n<billing_address_region>" + region
					+ "</billing_address_region>\n<billing_address_state>Indonesia</billing_address_state>\n"
					+ "<billing_address_poscode>" + poscode + "</billing_address_poscode>\n"
					+ "<billing_address_country_code>ID</billing_address_country_code>\n<receiver_name_for_shipping>"
					+ cust_name + "</receiver_name_for_shipping>\n<shipping_address>" + address
					+ "</shipping_address>\n<shipping_address_city>" + city + "</shipping_address_city>\n"
					+ "<shipping_address_region>" + region + "</shipping_address_region>\n"
					+ "<shipping_address_state>Indonesia</shipping_address_state>\n<shipping_address_poscode>" + poscode
					+ "</shipping_address_poscode>\n" + items.toString()
					+ "\n<reserve1></reserve1>\n<reserve2></reserve2>\n<signature>" + signature
					+ "</signature>\n</faspay>";

			final FaspayRequest faspayRequest = FaspayKeranjangPembayaran.sendRequest(postData, mahasiswa,
					biodataCalonMahasiswa, selectedKegiatanTemporary, amn, merchant_id, signature, bill_no,
					payment_channel_name, true);
			if (faspayRequest != null && faspayRequest.getUrl() != null && !faspayRequest.getUrl().trim().isEmpty()) {

				String code = faspayRequest.getTrxId();

				File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
						+ faspayRequest.getId() + ".png");

				BarcodeCommon.generateCRCode(code, myfilebarcode1);

				String myUrl = "/common/faspay/no_va.zul?va=" + URLEncoder.encode(faspayRequest.getTrxId(), "UTF-8")
						+ "&nominal="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(faspayRequest.getAmount()), "UTF-8")
						+ "&biayaAdministrasi="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8")
						+ "&biayaTotal="
						+ URLEncoder.encode(
								"Rp. " + Common.numberFormat.get().format(faspayRequest.getAmount() + biayaAdministrasi),
								"UTF-8")
						+ "&qr="
						+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/report/" + myfilebarcode1.getName(),
								"UTF-8")
						+ "&terbilang="
						+ URLEncoder.encode(
								IndonesianNumberToWords.convert((long) (faspayRequest.getAmount() + biayaAdministrasi)),
								"UTF-8")
						+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

				Common.displayWindow(myUrl, true, "65%");

			} else {
				MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		} finally {
			Common.closeNativeSessionQuietly(session);
		}

		return true;
	}

	@SuppressWarnings("deprecation")
	public static FaspayRequest sendRequest(String postData, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, final Set<KegiatanTemporary> selectedKegiatanTemporary,
			Double amount, String merchant_id, String signature, String bill_no, String payment_channel_name,
			Boolean hapusCicilanSebelumnya) throws Exception {
		// Bersihkan detail kegagalan lama agar alert tidak menampilkan penyebab transaksi sebelumnya.
		InfoTeknisPembayaran.bersihkan();
		postData = postData.replaceAll("&", "dan");

		// curl_init and url
		String strURL = (Common
				.getKonfigurasi("faspay_gateway_url", "http://faspaydev.mediaindonusa.com/pws/300002/183xx00010100000")
				.getNilai());
		String redirectURL = (Common
				.getKonfigurasi("faspay_redirect_url", "http://faspaydev.mediaindonusa.com/pws/100003/0830000010100000")
				.getNilai());

		FaspayRequest faspayRequest = new FaspayRequest();
		System.out.println("postData = " + postData);

		KegiatanTemporary kegiatanTemporary = selectedKegiatanTemporary.iterator().next();

		PostMethod post = new PostMethod(strURL);
		try {
			StringRequestEntity requestEntity = new StringRequestEntity(postData);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Content-type", "text/xml; charset=ISO-8859-1");
			HttpClient httpclient = new HttpClient();

			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");

			String hasil = post.getResponseBodyAsString();

			System.out.println(hasil);

			JSONObject jSONObject = XML.toJSONObject(hasil);
			JSONObject faspay = jSONObject.getJSONObject("faspay");
			System.out.println("jSONObject = " + faspay);

			String response_code = faspay.optString("response_code", "");
			if (!response_code.trim().isEmpty() && !response_code.trim().equals("00")) {
				// Faspay menolak permintaan — catat kode + pesan server apa adanya agar
				// pengguna/admin tahu penyebab pastinya (bukan sekadar "Transaksi Gagal").
				String response_desc = faspay.optString("response_desc", "");
				InfoTeknisPembayaran.catat("Server Faspay menolak permintaan, kode status=" + response_code
						+ (response_desc.trim().isEmpty() ? "" : ", pesan=" + response_desc.trim())
						+ ". Respons server: " + InfoTeknisPembayaran.potong(hasil, 300) + " (URL: " + strURL + ")");
			}

			String trx_id = faspay.isNull("trx_id") ? "" : ais.common.CommonJSONUtil.ambilLong(faspay,"trx_id") + "";

			String url = redirectURL + "/" + signature + "?trx_id=" + trx_id + "&merchant_id=" + merchant_id
					+ "&bill_no=" + bill_no;

			System.out.println("url = " + url);

			faspayRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
			faspayRequest.setNama(faspay.getString("response"));
			faspayRequest.setUrl(url);
			faspayRequest.setTrxId(trx_id);
			faspayRequest.setBillNo(bill_no);
			faspayRequest.setMerchant_id(merchant_id);
			faspayRequest.setSignature(signature);
			faspayRequest.setMerchant(faspay.getString("merchant"));
			faspayRequest.setResponse_code(faspay.getString("response_code"));
			faspayRequest.setResponse_desc(faspay.getString("response_desc"));
			faspayRequest.setMahasiswa(mahasiswa);
			faspayRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			// faspayRequest.setJenisKegiatan(jenisKegiatan);
			// faspayRequest.setJadwalPembayaran(jadwalPembayaran);
			faspayRequest.setSemester(kegiatanTemporary.getSemster());
			faspayRequest.setTahunAkademik(kegiatanTemporary.getTahunAkademik());
			// faspayRequest.setKeterangan(keterangan);
			// faspayRequest.setPengurangan(pengurangan);
			faspayRequest.setNilaiBiayaHarusDiBayars(amount);
			faspayRequest.setAmount(amount);
			faspayRequest.setResponse(faspay.toString());
			faspayRequest.setRequest(postData);
			faspayRequest.setPayment_channel_name(payment_channel_name);
			faspayRequest.setKegiatanTemporarys(selectedKegiatanTemporary);

			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.save(faspayRequest);
				session.getTransaction().commit();
			} catch (Exception se) {
				try {
					if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) src/ais/common/FaspayKeranjangPembayaran.java:443");
				}
				throw se;
			} finally {
				Common.closeNativeSessionQuietly(session);
			}

			// for (FaspayRequestDetail faspayRequestDetail :
			// faspayRequestDetails) {
			// faspayRequestDetail.setFaspayRequest(faspayRequest);
			// session.getTransaction().begin();
			// session.save(faspayRequestDetail);
			// session.getTransaction().commit();
			// }
			//
			// for (FaspayRequestDetailBiaya faspayRequestDetailBiaya :
			// faspayRequestDetailBiayas) {
			// faspayRequestDetailBiaya.setFaspayRequest(faspayRequest);
			// session.getTransaction().begin();
			// session.save(faspayRequestDetailBiaya);
			// session.getTransaction().commit();
			// }

		} catch (java.net.ConnectException ce) {
			// Gateway Faspay tak bisa dihubungi (unreachable/refused). Alur tetap seperti semula:
			// faspayRequest kosong (url null) dikembalikan sehingga pemanggil menampilkan alert.
			InfoTeknisPembayaran.catat("Tidak dapat terhubung ke gateway Faspay (" + strURL + "): "
					+ InfoTeknisPembayaran.potong(ce.getMessage(), 200) + ". Periksa koneksi/whitelist IP server.");
			Common.tampilErrorJikaAdmin(ce);
		} catch (java.net.SocketTimeoutException te) {
			// Gateway Faspay tidak merespons dalam batas waktu baca (timeout).
			InfoTeknisPembayaran.catat("Gateway Faspay (" + strURL + ") tidak merespons dalam batas waktu (timeout): "
					+ InfoTeknisPembayaran.potong(te.getMessage(), 200) + ". Coba beberapa saat lagi.");
			Common.tampilErrorJikaAdmin(te);
		} catch (org.apache.commons.httpclient.ConnectTimeoutException cte) {
			// ConnectTimeoutException = subclass InterruptedIOException, BUKAN ConnectException,
			// jadi perlu catch tersendiri: koneksi tidak tersambung dalam batas waktu.
			InfoTeknisPembayaran.catat("Koneksi ke gateway Faspay (" + strURL + ") tidak tersambung dalam batas waktu: "
					+ InfoTeknisPembayaran.potong(cte.getMessage(), 200) + ". Periksa jaringan/firewall server.");
			Common.tampilErrorJikaAdmin(cte);
		} catch (Exception e) {
			// Kegagalan request/proses lain (mis. respons tak bisa diparse / gagal simpan) —
			// catat penyebabnya; alur tetap seperti semula.
			InfoTeknisPembayaran.catat("Gagal memproses request/respons Faspay (" + strURL + "): "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			Common.tampilErrorJikaAdmin(e);
		} finally {
			post.releaseConnection();
		}

		return faspayRequest;
	}

}
