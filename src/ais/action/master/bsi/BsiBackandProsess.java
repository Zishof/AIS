package ais.action.master.bsi;

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

import ais.action.servlet.Bsiresponse;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.bsi.BsiRequest;
import ais.database.model.bsi.BsiResponse;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;

public class BsiBackandProsess extends TimerTask {

	@SuppressWarnings("deprecation")
	public static JSONObject check(String strURL, BsiRequest bsiRequest, Session session) {
		JSONObject bsi = null;
		BsiResponse bsiResponse = (BsiResponse) session.createCriteria(BsiResponse.class)
				.add(Restrictions.eq("trxId", bsiRequest.getTrxId())).setMaxResults(1).addOrder(Order.desc("id"))
				.uniqueResult();
		if (bsiResponse != null && !bsiResponse.getKeterangan().trim().isEmpty()) {
			try {

				try {
					bsi = new JSONObject(bsiResponse.getKeterangan());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				if (bsi != null) {

					if (bsiRequest != null && bsiResponse.getKodeStatus().toString().trim().equalsIgnoreCase("000")) {
						bsiRequest.setBsiResponse(bsiResponse);
						bsiRequest.setStatus("Payment Sukses");
						bsiRequest.setKodeStatus(bsiResponse.getKodeStatus());
						session.getTransaction().begin();
						session.update(bsiRequest);
						session.getTransaction().commit();

						bsiRequest.setStatus(bsiRequest.getStatus());
						bsiRequest.setKodeStatus(bsiResponse.getKodeStatus());
						session.getTransaction().begin();
						session.update(bsiRequest);
						session.getTransaction().commit();
						Bsiresponse.prosesResponse(bsiResponse, true);
						return bsi;
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		System.out.println("strURL = " + strURL);
		PostMethod post = new PostMethod(strURL);
		try {

			Siswa siswa = bsiRequest.getSiswa();
			CalonSiswa calonSiswa = bsiRequest.getCalonSiswa();

			String[] h = Sekolah.checkCidDanPasswordBsi(siswa, calonSiswa);

			String cid = h[0]; // from BNI
			String key = h[1]; // from BNI

			String data = "{" + "\"type\":\"inquirybilling\",\"client_id\":\"" + cid + "\",\"trx_id\":\""
					+ bsiRequest.getTrxId() + "\"}";

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

			JSONObject bsiJson = new JSONObject(hasil);
			System.out.println("bsiJson = " + bsiJson);

			data = bsiJson.isNull("data") ? "" : bsiJson.getString("data");

			decodeData = BNIHash.parseData(data, cid, key);

			JSONObject responseData = new JSONObject(decodeData);
			System.out.println("responseData = " + responseData);
			if (responseData != null) {

				String payment_amount = responseData.getString("payment_amount");
				String trx_amount = responseData.getString("trx_amount");

				System.out.println("payment_amount = " + payment_amount + ", trx_amount = " + trx_amount);

				String status = payment_amount.trim().equals(trx_amount.trim()) ? "000" : "001";

				if (bsiResponse == null) {
					bsiResponse = new BsiResponse();
				}
				bsiResponse.setKeterangan(responseData.toString());
				bsiResponse.setStatus(
						!status.toString().trim().equalsIgnoreCase("000") ? "Sedang diproses" : "Payment Sukses");
				bsiResponse.setKodeStatus(status);
				bsiResponse.setMerchant(cid);
				bsiResponse.setTrxId(bsiRequest.getTrxId());
				bsiResponse.setCallback(data);

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, bsiResponse);
				session.getTransaction().commit();
				session.flush();

				bsiRequest.setBsiResponse(bsiResponse);
				bsiRequest.setStatus(bsiResponse.getStatus());
				bsiRequest.setKodeStatus(status);
				session.getTransaction().begin();
				Common.refreshUpdate(session, bsiRequest);
				session.getTransaction().commit();
				if (status.trim().equalsIgnoreCase("000")) {
					Bsiresponse.prosesResponse(bsiResponse, true);
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			post.releaseConnection();
		}

		return bsi;
	}

	@Override
	public void run() {
		try {
		boolean aktifkan_pembayaran_via_bsi = Common.bolehKonfigurasi("aktifkan_check_ulang_otomatis_pembayaran_via_bsi", Konfigurasi.TIDAK_AKTIF);
		if (aktifkan_pembayaran_via_bsi) {

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 7);
			Date kemarin = calendar.getTime();

			String sqlRes = "this_.tanggal_dirubah > '" + Common.databaseDateFormat1.get().format(kemarin) + "'";

			Session session = HibernateUtil.currentNativeSession();
			int jumlah = ((Number) session.createCriteria(BsiRequest.class).setProjection(Projections.rowCount())
					.add(Restrictions.isNull("bsiResponse")).add(Restrictions.sqlRestriction(sqlRes))
					.add(Restrictions.or(
							Restrictions.or(Restrictions.isNull("kodeStatus"), Restrictions.eq("kodeStatus", "0")),
							Restrictions.eq("kodeStatus", "1")))
					.uniqueResult()).intValue();
			// // //System.out.println("BsiBackandProsess, jumlah = " +
			// jumlah
			// + ", sqlRes = " + sqlRes);
			if (jumlah > 0) {

				@SuppressWarnings("unchecked")
				List<BsiRequest> bsiRequests = session.createCriteria(BsiRequest.class)
						.add(Restrictions.sqlRestriction(sqlRes)).addOrder(Order.asc("id"))
						.add(Restrictions.isNull("bsiResponse"))
						.add(Restrictions.or(
								Restrictions.or(Restrictions.isNull("kodeStatus"), Restrictions.eq("kodeStatus", "0")),
								Restrictions.eq("kodeStatus", "1")))
						.list();
				for (BsiRequest bsiRequest : bsiRequests) {

					Siswa siswa = bsiRequest.getSiswa();
					String ipClient = (Common.getKonfigurasi("bsi_ip_client", "").getNilai());
					if (!ipClient.trim().isEmpty()) {
						ipClient = ipClient + "/BsiForwarder";
					}
					String strURL = !ipClient.trim()
							.isEmpty()
									? ipClient
									: (siswa != null && siswa.getSekolah() != null
											&& !siswa.getSekolah().getBsiGatewayUrl().isEmpty()
													? siswa.getSekolah().getBsiGatewayUrl()
													: (Common
															.getKonfigurasi("bsi_gateway_url",
																	"https://apibeta.bsi-ecollection.com/")
															.getNilai()));

					BsiBackandProsess.check(strURL, bsiRequest, session);
				}

			}
			HibernateUtil.closeSession();
		}
			} finally {
			ais.database.hibernate.HibernateUtil.closeSession();
		}
	}

}
