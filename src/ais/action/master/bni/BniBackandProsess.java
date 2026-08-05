package ais.action.master.bni;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import com.bni.encrypt.BNIHash;

import ais.action.servlet.Bniresponse;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.bni.BniRequest;
import ais.database.model.bni.BniResponse;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;

public class BniBackandProsess extends TimerTask {

	@SuppressWarnings("deprecation")
	public static JSONObject check(String strURL, BniRequest bniRequest, Session session) {
		return check(strURL, bniRequest, session, false);
	}

	/**
	 * @param paksaProses bila TRUE, pemrosesan pembayaran (Bniresponse.prosesResponse) MELEWATI guard
	 *                    "sudah dibayar/belum" sehingga pembayaran yang tak sengaja terhapus bisa
	 *                    dipulihkan. Dipakai oleh tombol manual "Cek Pembayaran" & "Check Ulang Semua".
	 *                    Proses otomatis (timer) memakai overload tanpa paksa (guard tetap aktif).
	 */
	public static JSONObject check(String strURL, BniRequest bniRequest, Session session, boolean paksaProses) {
		JSONObject bni = null;
		BniResponse bniResponse = (BniResponse) session.createCriteria(BniResponse.class)
				.add(Restrictions.eq("trxId", bniRequest.getTrxId())).setMaxResults(1).addOrder(Order.desc("id"))
				.uniqueResult();
		if (bniResponse != null && !bniResponse.getKeterangan().trim().isEmpty()) {
			try {

				try {
					bni = new JSONObject(bniResponse.getKeterangan());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				if (bni != null) {

					if (bniRequest != null && bniResponse.getKodeStatus().toString().trim().equalsIgnoreCase("000")) {
						bniRequest.setBniResponse(bniResponse);
						bniRequest.setStatus("Payment Sukses");
						bniRequest.setKodeStatus(bniResponse.getKodeStatus());

						bniRequest.setStatus(bniRequest.getStatus());
						bniRequest.setKodeStatus(bniResponse.getKodeStatus());
						session.getTransaction().begin();
						Common.refreshUpdate(session, bniRequest);
						session.getTransaction().commit();
						Bniresponse.prosesResponse(bniResponse, true, paksaProses);
						return bni;
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		System.out.println("strURL = " + strURL);
		PostMethod post = new PostMethod(strURL);
		try {

			Siswa siswa = bniRequest.getSiswa();
			CalonSiswa calonSiswa = bniRequest.getCalonSiswa();

			String[] h = Sekolah.checkCidDanPassword(siswa, calonSiswa);

			String cid = h[0]; // from BNI
			String key = h[1]; // from BNI

			String type = Common.getKonfigurasi("type_inquirybilling_bni", "inquirybilling").getNilai();

			String trx_id = bniRequest.getTrxId();

//			trx_id = "123456789";

			String data = "{" + "\"type\":\"" + type + "\",\"client_id\":\"" + cid + "\",\"trx_id\":\"" + trx_id
					+ "\"}";

//			data = "{" + "\"type\":\"inquirybilling\",\"client_id\":\"" + cid + "\",\"trx_id\":\"\"}";

			System.out.println("data = " + data);

			String parsedData = BNIHash.hashData(data, cid, key);
			String decodeData = BNIHash.parseData(parsedData, cid, key);

			System.out.println("parsedData = " + parsedData);
			System.out.println("decodeData = " + decodeData);

			String postData = "{ \"client_id\":\"" + cid + "\", \"data\":\"" + parsedData + "\"}";

			postData = postData.replaceAll("&", "dan");
			System.out.println("postData = " + postData);

			StringRequestEntity requestEntity = new StringRequestEntity(postData);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Content-type", "application/json");
			HttpClient httpclient = new HttpClient();

			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");

			String hasil = post.getResponseBodyAsString();

			System.out.println(hasil);

			JSONObject bniJson = new JSONObject(hasil);
			System.out.println("bniJson = " + bniJson);

			data = bniJson.isNull("data") ? "" : bniJson.getString("data");

			decodeData = BNIHash.parseData(data, cid, key);
			// Guard: jika data kosong/null (saat "data" BNI tidak ada), JSONTokener NPE
			if (decodeData == null || decodeData.trim().isEmpty()) {
				return bni;
			}

			JSONObject responseData = new JSONObject(decodeData);
			System.out.println("responseData = " + responseData);
			if (responseData != null) {

				String payment_amount = responseData.getString("payment_amount");
				String trx_amount = responseData.getString("trx_amount");

				System.out.println("payment_amount = " + payment_amount + ", trx_amount = " + trx_amount);

				String status = payment_amount.trim().equals(trx_amount.trim()) ? "000" : "001";

				try {
					if (bniResponse == null) {
						bniResponse = new BniResponse();
					}
					bniResponse.setKeterangan(responseData.toString());
					bniResponse.setStatus(
							!status.toString().trim().equalsIgnoreCase("000") ? "Sedang diproses" : "Payment Sukses");
					bniResponse.setKodeStatus(status);
					bniResponse.setMerchant(cid);
					bniResponse.setTrxId(bniRequest.getTrxId());
					bniResponse.setCallback(data);

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, bniResponse);
					session.getTransaction().commit();
					session.flush();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bni/BniBackandProsess.java:164");
					// TODO: handle exception
				}

				try {
					bniRequest.setBniResponse(bniResponse);
					bniRequest.setStatus(bniResponse.getStatus());
					bniRequest.setKodeStatus(status);
					session.getTransaction().begin();
					Common.refreshUpdate(session, bniRequest);
					session.getTransaction().commit();

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bni/BniBackandProsess.java:176");
					// TODO: handle exception
				}

				if (status.trim().equalsIgnoreCase("000")) {
					Bniresponse.prosesResponse(bniResponse, true, paksaProses);
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			post.releaseConnection();
		}

		return bni;
	}

	@Override
	public void run() {
		try {
		boolean aktifkan_pembayaran_via_bni = Common.bolehKonfigurasi("aktifkan_check_ulang_otomatis_pembayaran_via_bni", Konfigurasi.TIDAK_AKTIF);
		if (aktifkan_pembayaran_via_bni) {

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 7);
			Date kemarin = calendar.getTime();

			String sqlRes = "this_.tanggal_dirubah > '" + Common.databaseDateFormat1.get().format(kemarin) + "'";

			Session session = HibernateUtil.currentNativeSession();
			int jumlah = ((Number) session.createCriteria(BniRequest.class).setProjection(Projections.rowCount())
					.add(Restrictions.isNull("bniResponse")).add(Restrictions.sqlRestriction(sqlRes))
					.add(Restrictions.or(
							Restrictions.or(Restrictions.isNull("kodeStatus"), Restrictions.eq("kodeStatus", "0")),
							Restrictions.eq("kodeStatus", "1")))
					.uniqueResult()).intValue();
			// // //System.out.println("BniBackandProsess, jumlah = " +
			// jumlah
			// + ", sqlRes = " + sqlRes);
			if (jumlah > 0) {

				@SuppressWarnings("unchecked")
				List<BniRequest> bniRequests = session.createCriteria(BniRequest.class)
						.add(Restrictions.sqlRestriction(sqlRes)).addOrder(Order.asc("id"))
						.add(Restrictions.isNull("bniResponse"))
						.add(Restrictions.or(
								Restrictions.or(Restrictions.isNull("kodeStatus"), Restrictions.eq("kodeStatus", "0")),
								Restrictions.eq("kodeStatus", "1")))
						.list();
				for (BniRequest bniRequest : bniRequests) {

					Siswa siswa = bniRequest.getSiswa();
					String ipClient = (Common.getKonfigurasi("bni_ip_client", "").getNilai());
					if (!ipClient.trim().isEmpty()) {
						ipClient = ipClient + "/BniForwarder";
					}
					String strURL = !ipClient.trim()
							.isEmpty()
									? ipClient
									: (siswa != null && siswa.getSekolah() != null
											&& !siswa.getSekolah().getBniGatewayUrl().isEmpty()
													? siswa.getSekolah().getBniGatewayUrl()
													: (Common
															.getKonfigurasi("bni_gateway_url",
																	"https://apibeta.bni-ecollection.com/")
															.getNilai()));

					BniBackandProsess.check(strURL, bniRequest, session);
				}

			}
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}
			} finally {
			ais.database.hibernate.HibernateUtil.closeSession();
		}
	}

}
