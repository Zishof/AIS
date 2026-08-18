package ais.action.master.bri;

import java.util.Calendar;
import java.util.TimerTask;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.servlet.Briresponse;
import ais.common.BriCommon;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.bri.BriRequest;
import ais.database.model.bri.BriResponse;

public class BriBackandProsess extends TimerTask {

	public static JSONObject checkSatu(BriRequest briRequest, Session session) {
		JSONObject bri = null;
		BriResponse briResponse = (BriResponse) session.createCriteria(BriResponse.class)
				.add(Restrictions.eq("trxId", briRequest.getTrxId())).setMaxResults(1).addOrder(Order.desc("id"))
				.uniqueResult();
		if (briResponse != null && !briResponse.getKeterangan().trim().isEmpty()) {
			try {

				try {
					bri = new JSONObject(briResponse.getKeterangan());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				if (bri != null) {

					if (briRequest != null && briResponse.getKodeStatus().toString().trim().equalsIgnoreCase("00")) {
						briRequest.setBriResponse(briResponse);
						briRequest.setStatus("Payment Sukses");
						briRequest.setKodeStatus(briResponse.getKodeStatus());
						session.getTransaction().begin();
						session.update(briRequest);
						session.getTransaction().commit();

						briRequest.setStatus(briRequest.getStatus());
						briRequest.setKodeStatus(briResponse.getKodeStatus());
						session.getTransaction().begin();
						session.update(briRequest);
						session.getTransaction().commit();
						Briresponse.prosesResponse(briResponse);
						return bri;
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		String merchant_id = Common.getKonfigurasi("bri_institution_code", "J104408").getNilai();
		String brivaNo = Common.getKonfigurasi("bri_briva_no", "77777").getNilai();

		String strURL = Common.getKonfigurasi("bri_gateway_url", "https://developer.bri.co.id/v1/api/briva").getNilai()
				+ "/status/" + merchant_id + "/" + brivaNo + "/" + briRequest.getVa();

		// PostMethod post = new PostMethod(strURL);
		try {

			String hasil = BriCommon.get(strURL);

			JSONObject response = new JSONObject(hasil);
			System.out.println("response = " + response);
			if (response != null) {

				JSONObject responseData = response.getJSONObject("data");
				String status = !responseData.isNull("statusBayar")
						&& responseData.getString("statusBayar").trim().equalsIgnoreCase("Y") ? "00" : "01";
				System.out.println("status -> " + status);
				if (briResponse == null) {
					briResponse = new BriResponse();
				}
				briResponse.setKeterangan(responseData.toString());
				briResponse.setStatus(status.equals("00") ? "Payment Sukses" : "Belum Membayar");
				briResponse.setKodeStatus(status);
				briResponse.setMerchant(merchant_id);
				briResponse.setTrxId(briRequest.getTrxId());
				briResponse.setCallback(response.toString());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, briResponse);
				session.getTransaction().commit();
				session.flush();

				briRequest.setBriResponse(briResponse);
				briRequest.setStatus(briResponse.getStatus());
				briRequest.setKodeStatus(status);
				session.getTransaction().begin();
				Common.refreshUpdate(session, briRequest);
				session.getTransaction().commit();
				if (status.trim().equalsIgnoreCase("00")) {
					Briresponse.prosesResponse(briResponse);
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return bri;
	}

	private void simpan(String merchant_id, JSONObject response, JSONObject responseData, Session session)
			throws Exception {
		// System.out.println("simpan --> responseData = " + responseData);
		if (!responseData.isNull("custCode")) {
			int count = ((Number) session.createCriteria(BriResponse.class)
					.add(Restrictions.eq("trxId", responseData.getString("custCode")))
					.add(Restrictions.eq("kodeStatus", "00")).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();

			// System.out.println("count => " + count + ", responseData = " +
			// responseData);

			if (count == 0) {
				BriRequest briRequest = (BriRequest) session.createCriteria(BriRequest.class)
						.add(Restrictions.eq("va", responseData.getString("custCode"))).setMaxResults(1)
						.addOrder(Order.desc("id")).uniqueResult();
				// System.out.println("briRequest = " + briRequest);
				if (briRequest != null) {
					BriResponse briResponse = (BriResponse) session.createCriteria(BriResponse.class)
							.add(Restrictions.eq("trxId", responseData.getString("custCode"))).setMaxResults(1)
							.addOrder(Order.desc("id")).uniqueResult();
					if (briResponse == null) {
						briResponse = new BriResponse();
					}
					briResponse.setKeterangan(responseData.toString());
					briResponse.setStatus("Payment Sukses");
					briResponse.setKodeStatus("00");
					briResponse.setMerchant(merchant_id);
					briResponse.setTrxId(responseData.getString("custCode"));
					briResponse.setCallback(response.toString());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, briResponse);
					session.getTransaction().commit();
					session.flush();

					briRequest.setBriResponse(briResponse);
					briRequest.setStatus(briResponse.getStatus());
					briRequest.setKodeStatus("00");
					session.getTransaction().begin();
					Common.refreshUpdate(session, briRequest);
					session.getTransaction().commit();
					Briresponse.prosesResponse(briResponse);
				}
			}
		}
	}

	@Override
	public void run() {
		try {
		boolean aktifkan_pembayaran_via_bri = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bri", Konfigurasi.TIDAK_AKTIF);
		if (aktifkan_pembayaran_via_bri) {

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MONTH, 1);
			// Date kemarin = calendar.getTime();

			// String start_time = Common.dateFormat8.get().format(kemarin);
			String end_time = Common.dateFormat8.get().format(ais.ui.util.WaktuUtil.getDate());

			String merchant_id = Common.getKonfigurasi("bri_institution_code", "J104408").getNilai();
			String brivaNo = Common.getKonfigurasi("bri_briva_no", "77777").getNilai();

			String url = Common.getKonfigurasi("bri_gateway_url", "https://developer.bri.co.id/v1/api/briva").getNilai()
					+ "/report/" + merchant_id + "/" + brivaNo + "/" + end_time + "/" + end_time;

			try {

				String hasil = BriCommon.get(url);
				// System.out.println("hasil = " + hasil);
				JSONObject response = null;

				try {
					JSONArray jsonArray = new JSONArray(hasil);

					response = jsonArray.getJSONObject(0);
				} catch (Exception e) {
					response = new JSONObject(hasil);
				}

				// System.out.println("response = " + response);
				if (response != null) {
					Session session = HibernateUtil.currentNativeSession();
					try {

						JSONArray responseDataArray = response.getJSONArray("data");
						for (int i = 0; i < responseDataArray.length(); i++) {
							JSONObject responseData = responseDataArray.getJSONObject(i);
							simpan(merchant_id, response, responseData, session);
						}
					} catch (Exception e) {
						HibernateUtil.rollbackTransaction();
						JSONObject responseData = response.getJSONObject("data");
						simpan(merchant_id, response, responseData, session);
					}
					HibernateUtil.closeSession();
				}

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/bri/BriBackandProsess.java:212");

				// Common.tampilErrorJikaAdmin(e);
			}

		}
			} finally {
			ais.database.hibernate.HibernateUtil.closeSession();
		}
	}

}
