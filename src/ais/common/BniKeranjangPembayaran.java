package ais.common;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.hibernate.Session;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import com.bni.encrypt.BNIHash;

import ais.action.master.bni.BniRequestAction;
import ais.action.report.Report;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.bni.BniRequest;
import ais.ui.util.MyMessageboxConfig;

public class BniKeranjangPembayaran {

	@SuppressWarnings({})
	public static boolean onSaveBni(final Double amn, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final Set<KegiatanTemporary> selectedKegiatanTemporary,
			final Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		onPilihBni(amn, mahasiswa, biodataCalonMahasiswa, selectedKegiatanTemporary, event);

		return true;
	}

	@SuppressWarnings("unchecked")
	public static boolean onPilihBni(final Double amn, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			final Set<KegiatanTemporary> selectedKegiatanTemporary, Event event) throws Exception {

		String merchant_id = Common.getKonfigurasi("bni_merchant_id", "000").getNilai().trim();
		String Password = Common.getKonfigurasi("bni_password", "685dedd9f045787873794ead6276f8bf").getNilai().trim();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

		String bill_no = Common.getGeneratedBarCode();

		Date tanggalExpired = null;
		String tanggal_terakhir_pembayaran = Common.getKonfigurasi("tanggal_terakhir_pembayaran", "").getNilai();
		if (tanggal_terakhir_pembayaran != null && !tanggal_terakhir_pembayaran.trim().isEmpty()) {
			try {
				tanggalExpired = Common.dateFormat1.get().parse(tanggal_terakhir_pembayaran);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniKeranjangPembayaran.java:62");

			}
		}

		String datetime_expired = tanggalExpired != null ? Common.databaseDateFormat1.get().format(tanggalExpired)
				: Common.databaseDateFormat1.get().format(calendar.getTime());

		String virtual_account = "";

		if (Common.bolehKonfigurasi("angka_va_bni_menggunakan_nim")) {
			virtual_account = Common.getKonfigurasi("angka_prefix_va_bni", "8").getNilai() + merchant_id
					+ (mahasiswa != null ? mahasiswa.getNim()
							: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNoRegistrasi() : ""));
		} else {
			int generatedAngkaDigit = 8;
			try {
				generatedAngkaDigit = Integer
						.parseInt(Common.getKonfigurasi("generated_angka_digit_bni", "8").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniKeranjangPembayaran.java:81");

			}
			virtual_account = Common.getKonfigurasi("angka_prefix_va_bni", "8").getNilai() + merchant_id
					+ Common.getGeneratedAngkaDigit(generatedAngkaDigit);
		}

		Double biayaAdministrasi = 0.0;
		try {
			biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniKeranjangPembayaran.java:91");

		}

		int generatedAngkaDigit = 16;
		try {
			generatedAngkaDigit = Integer
					.parseInt(Common.getKonfigurasi("virtual_account_angka_digit_bni", "16").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniKeranjangPembayaran.java:99");

		}
		virtual_account = (virtual_account + "00000000000000000").substring(0, generatedAngkaDigit);

		// BNIHash hash = new BNIHash();
		String data = "{\"customer_email\":\""
				+ (mahasiswa != null ? mahasiswa.getEmail().split(",")[0].trim()
						: (biodataCalonMahasiswa == null ? "test@email.com"
								: biodataCalonMahasiswa.getEmail().split(",")[0].trim()))
				+ "\",\"trx_id\":\"" + bill_no + "\",\"datetime_expired\":\"" + datetime_expired + "\",\"client_id\":\""
				+ merchant_id + "\",\"customer_phone\":\""
				+ (mahasiswa != null ? mahasiswa.getTelp()
						: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getHp() : ""))
				+ "\",\"customer_name\":\""
				+ (mahasiswa != null ? mahasiswa.getNama()
						: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") : ""))
				+ "\",\"type\":\"createbilling\",\"virtual_account\":\"" + virtual_account + "\",\"trx_amount\":\""
				+ (biayaAdministrasi.intValue() + amn.intValue()) + "\",\"billing_type\":\"c\"}";

		String cid = merchant_id; // from BNI
		String key = Password; // from BNI

		String parsedData = BNIHash.hashData(data, cid, key);
		String decodeData = BNIHash.parseData(parsedData, cid, key);

		System.out.println("parsedData = " + parsedData);
		System.out.println("decodeData = " + decodeData);

		String postData = "{ \"client_id\":\"" + merchant_id + "\", \"data\":\"" + parsedData + "\"}";
		final BniRequest bniRequest = BniKeranjangPembayaran.sendRequest(postData, mahasiswa, biodataCalonMahasiswa,
				selectedKegiatanTemporary, amn, merchant_id, data, bill_no, key, true);
		if (bniRequest != null && bniRequest.getVa() != null && !bniRequest.getVa().trim().isEmpty()) {

			MyMessageboxConfig.show("Kode pembayaran Anda adalah " + bniRequest.getVa() + " dengan tagihan "
					+ Common.numberFormat.get().format(amn)
					+ (biayaAdministrasi > 0.1
							? "\nBiaya administrasi " + Common.numberFormat.get().format(biayaAdministrasi)
									+ "\nJadi total tagihan " + Common.numberFormat.get().format(amn + biayaAdministrasi)
									+ "\nTerbilang " + IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi))
							: "")
					+ "\n\nAnda dapat membayar tagihan ini dengan memasukkan kode \"" + bniRequest.getVa()
					+ "\" di semua channel BNI.", "Pemberitahuan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings({ "rawtypes" })
								@Override
								public void onEvent(Event arg0) throws Exception {

									Double biayaAdministrasi = 0.0;
									try {
										biayaAdministrasi = Double.parseDouble(
												Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniKeranjangPembayaran.java:157");

									}

									String info = "Kode Pembayaran\t\t: " + bniRequest.getVa() + "\n";
									info += "Kode invoice\t\t\t: " + bniRequest.getBillNo() + "\n";
									info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
									if (biayaAdministrasi > 0.1) {
										info += "Biaya admin \t\t\t: " + Common.numberFormat.get().format(biayaAdministrasi)
												+ "\n";
										info += "Total tagihan \t\t: "
												+ Common.numberFormat.get().format(amn + biayaAdministrasi) + "\n";
									}
									info += "Terbilang \t\t\t: "
											+ IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi)) + "\n";
									if (bniRequest.getMahasiswa() != null) {
										info += "NIM \t\t\t\t: " + bniRequest.getMahasiswa().getNim() + "\n";
										info += "Nama \t\t\t\t: " + bniRequest.getMahasiswa().getNama() + "\n";
									} else if (bniRequest.getBiodataCalonMahasiswa() != null) {
										info += "No. Reg \t\t\t: "
												+ bniRequest.getBiodataCalonMahasiswa().getNoRegistrasi() + "\n";
										if (bniRequest.getBiodataCalonMahasiswa().getNoUjian() != null) {
											info += "No. Ujian \t\t\t: "
													+ bniRequest.getBiodataCalonMahasiswa().getNoUjian() + "\n";
										}
										info += "Nama \t\t\t\t: " + bniRequest.getBiodataCalonMahasiswa().getNama()
												+ "\n";
									}

									Map parameters = ais.common.HashMapGenerator.getRand();
									parameters.put("tanggal", bniRequest.getTanggal_dirubah());
									parameters.put("bniRequest", bniRequest.getId());
									parameters.put("info", info);
									Report.generatePDFReport(Report.PDF, parameters, "Bukti_Bni_Mahasiswa",
											ais.ui.util.WaktuUtil.getDate());

									try {
										File file = Report.generateFileReport(Report.PDF, parameters,
												"Bukti_Bni_Mahasiswa", ais.ui.util.WaktuUtil.getDate(),
												Common.locale);
										CommonEmail.infoBayarViaBni(bniRequest, file);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniKeranjangPembayaran.java:198");

									}

								}
							}, "Menyiapkan pembayaran via bni..");

						}
					});

		} else {
			MyMessageboxConfig.show(BniCommon.pesanGagalDenganInfoTeknis(), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);

		}

		return true;
	}

	@SuppressWarnings("deprecation")
	public static BniRequest sendRequest(String postData, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, final Set<KegiatanTemporary> selectedKegiatanTemporary,
			Double amount, String merchant_id, String signature, String bill_no, String key,
			Boolean hapusCicilanSebelumnya) throws Exception {

		KegiatanTemporary kegiatanTemporary = selectedKegiatanTemporary.iterator().next();

		postData = postData.replaceAll("&", "dan");

		// curl_init and url
		String ipClient = (Common.getKonfigurasi("bni_ip_client", "").getNilai());
		if (!ipClient.trim().isEmpty()) {
			ipClient = ipClient + "/BniForwarder";
		}
		String strURL = !ipClient.trim().isEmpty() ? ipClient
				: (Common.getKonfigurasi("bni_gateway_url", "https://apibeta.bni-ecollection.com/").getNilai());

		BniRequest bniRequest = new BniRequest();
		System.out.println("postData = " + postData);

		PostMethod post = new PostMethod(strURL);
		try {
			StringRequestEntity requestEntity = new StringRequestEntity(postData);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Content-type", "application/json");
			HttpClient httpclient = new HttpClient();

			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");

			String hasil = post.getResponseBodyAsString();

			System.out.println(hasil);

			JSONObject bni = new JSONObject(hasil);
			System.out.println("jSONObject = " + bni);

			String status = bni.isNull("status") ? "" : bni.getString("status");

			if (!status.trim().equals("000")) {
				return null;
			}

			String data = bni.isNull("data") ? "" : bni.getString("data");

			// String decodeData = "";
			String decodeData = BNIHash.parseData(data, merchant_id, key);

			JSONObject responseData = new JSONObject(decodeData);
			System.out.println("responseData = " + responseData);

			bniRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
			bniRequest.setNama(responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));

			bniRequest.setTrxId(responseData.isNull("trx_id") ? "" : responseData.getString("trx_id"));
			bniRequest.setVa(responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
			bniRequest.setBillNo(bill_no);
			bniRequest.setMerchant_id(merchant_id);
			bniRequest.setData(responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
			bniRequest.setMerchant(merchant_id);
			bniRequest.setResponse_code(status);
			bniRequest.setResponse_desc(BniRequestAction.statses.get(status));
			bniRequest.setMahasiswa(mahasiswa);
			bniRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			// bniRequest.setJenisKegiatan(jenisKegiatan);
			// bniRequest.setJadwalPembayaran(jadwalPembayaran);
			bniRequest.setSemester(kegiatanTemporary.getSemster());
			bniRequest.setTahunAkademik(kegiatanTemporary.getTahunAkademik());
			// bniRequest.setKeterangan(keterangan);
			// bniRequest.setPengurangan(pengurangan);
			bniRequest.setNilaiBiayaHarusDiBayars(amount);
			bniRequest.setAmount(amount);
			bniRequest.setResponse(bni.toString());
			bniRequest.setRequest(postData);
			bniRequest.setKegiatanTemporarys(selectedKegiatanTemporary);

			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.save(bniRequest);
				session.getTransaction().commit();
			} catch (Exception se) {
				try {
					if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) src/ais/common/BniKeranjangPembayaran.java:306");
				}
				throw se;
			} finally {
				Common.closeNativeSessionQuietly(session);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			post.releaseConnection();
		}

		return bniRequest;
	}

}
