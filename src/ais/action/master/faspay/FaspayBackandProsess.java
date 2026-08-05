package ais.action.master.faspay;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.json.XML;

import ais.action.servlet.FasPayResponse;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.faspay.FaspayRequest;
import ais.database.model.faspay.FaspayResponse;

public class FaspayBackandProsess extends TimerTask {

	@SuppressWarnings("deprecation")
	public static JSONObject check(String strURL, FaspayRequest faspayRequest, Session session) {
		JSONObject faspay = null;
		FaspayResponse faspayResponse = (FaspayResponse) session.createCriteria(FaspayResponse.class)
				.add(Restrictions.ilike("keterangan", "\"" + faspayRequest.getBillNo() + "\"", MatchMode.ANYWHERE))
				.add(Restrictions.eq("trxId", faspayRequest.getTrxId())).setMaxResults(1).addOrder(Order.desc("id"))
				.uniqueResult();
		if (faspayResponse != null && !faspayResponse.getKeterangan().trim().isEmpty()) {
			try {

				try {
					faspay = new JSONObject(faspayResponse.getKeterangan());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				if (faspay != null) {
					Object payment_status_code = faspay.get("payment_status_code");
					Object payment_status_desc = faspay.get("payment_status_desc");

					if (payment_status_code != null && payment_status_code.toString().trim().equalsIgnoreCase("2")) {
						faspayResponse.setKeterangan(faspay.toString());
						faspayResponse.setNama(faspayRequest.getTrxId());
						faspayResponse.setStatus(
								payment_status_desc == null ? "Belum diproses" : payment_status_desc.toString());
						faspayResponse
								.setMerchant(faspay.isNull("merchant_id") ? "" : faspay.get("merchant_id").toString());
						faspayResponse.setTrxId(faspayRequest.getTrxId());
						faspayResponse.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
						faspayResponse
								.setKodeStatus(payment_status_code == null ? "0" : payment_status_code.toString());
						session.getTransaction().begin();
						session.saveOrUpdate(faspayResponse);
						session.getTransaction().commit();

						faspayRequest.setStatus(payment_status_desc == null ? null : payment_status_desc.toString());
						faspayRequest
								.setKodeStatus(payment_status_code == null ? null : payment_status_code.toString());
						session.getTransaction().begin();
						session.update(faspayRequest);
						session.getTransaction().commit();
						FasPayResponse.prosesResponse(faspayRequest, faspayResponse, faspayRequest.getBillNo());
						return faspay;
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		PostMethod post = new PostMethod(strURL);
		try {
			String postData = "<faspay><request>Inquiry Status Payment</request><trx_id>" + faspayRequest.getTrxId()
					+ "</trx_id><merchant_id>" + faspayRequest.getMerchant_id() + "</merchant_id><bill_no>"
					+ faspayRequest.getBillNo() + "</bill_no>" + "<signature>" + faspayRequest.getSignature()
					+ "</signature></faspay>";
			StringRequestEntity requestEntity = new StringRequestEntity(postData);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Content-type", "text/xml; charset=ISO-8859-1");
			HttpClient httpclient = new HttpClient();

			int result = httpclient.executeMethod(post);
			System.out.println("FaspayBackandProsess Response status code: " + result);
			System.out.println("FaspayBackandProsess Response body: ");

			String hasil = post.getResponseBodyAsString();

			JSONObject jSONObject = null;
			try {
				jSONObject = XML.toJSONObject(hasil);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/faspay/FaspayBackandProsess.java:99");
			}
			System.out.println("jSONObject => " + jSONObject + ", hasil = " + hasil);
			if (jSONObject != null) {

				try {
					faspay = jSONObject.getJSONObject("faspay");
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				System.out.println("jSONObject = " + faspay);

				if (faspay != null) {

					Object payment_status_code = faspay.get("payment_status_code");
					Object payment_status_desc = faspay.get("payment_status_desc");

					if (faspayResponse == null) {
						faspayResponse = new FaspayResponse();
					}
					faspayResponse.setKeterangan(faspay.toString());
					faspayResponse.setNama(faspayRequest.getTrxId());
					faspayResponse
							.setStatus(payment_status_desc == null ? "Belum diproses" : payment_status_desc.toString());
					faspayResponse
							.setMerchant(faspay.isNull("merchant_id") ? "" : faspay.get("merchant_id").toString());
					faspayResponse.setTrxId(faspayRequest.getTrxId());
					faspayResponse.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
					faspayResponse.setKodeStatus(payment_status_code == null ? "0" : payment_status_code.toString());
					session.getTransaction().begin();
					session.saveOrUpdate(faspayResponse);
					session.getTransaction().commit();

					faspayRequest.setStatus(payment_status_desc == null ? null : payment_status_desc.toString());
					faspayRequest.setKodeStatus(payment_status_code == null ? null : payment_status_code.toString());
					session.getTransaction().begin();
					session.update(faspayRequest);
					session.getTransaction().commit();
					if (payment_status_code != null && payment_status_code.toString().trim().equalsIgnoreCase("2")) {
						FasPayResponse.prosesResponse(faspayRequest, faspayResponse, faspayRequest.getBillNo());
					}
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			post.releaseConnection();
		}

		return faspay;
	}

	@Override
	public void run() {
		try {
		boolean aktifkan_pembayaran_via_faspay = Common.bolehKonfigurasi("aktifkan_pembayaran_via_faspay_auto", Konfigurasi.TIDAK_AKTIF);
		if (aktifkan_pembayaran_via_faspay) {

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 7);
			Date kemarin = calendar.getTime();

			String sqlRes = "this_.tanggal_dirubah > '" + Common.databaseDateFormat1.get().format(kemarin) + "'";

			Session session = HibernateUtil.currentNativeSession();
			int jumlah = ((Number) session.createCriteria(FaspayRequest.class).setProjection(Projections.rowCount())
					.add(Restrictions.isNull("faspayResponse")).add(Restrictions.sqlRestriction(sqlRes))
					.add(Restrictions.or(
							Restrictions.or(Restrictions.isNull("kodeStatus"), Restrictions.eq("kodeStatus", "0")),
							Restrictions.eq("kodeStatus", "1")))
					.uniqueResult()).intValue();
			// // //System.out.println("FaspayBackandProsess, jumlah = " +
			// jumlah
			// + ", sqlRes = " + sqlRes);
			if (jumlah > 0) {
				String strURL = (Common.getKonfigurasi("faspay_check_status_url",
						"http://faspaydev.mediaindonusa.com/pws/100004/183xx00010100000").getNilai());
				@SuppressWarnings("unchecked")
				List<FaspayRequest> faspayRequests = session.createCriteria(FaspayRequest.class)
						.add(Restrictions.sqlRestriction(sqlRes)).addOrder(Order.asc("id"))
						.add(Restrictions.isNull("faspayResponse"))
						.add(Restrictions.or(
								Restrictions.or(Restrictions.isNull("kodeStatus"), Restrictions.eq("kodeStatus", "0")),
								Restrictions.eq("kodeStatus", "1")))
						.list();
				for (FaspayRequest faspayRequest : faspayRequests) {
					FaspayBackandProsess.check(strURL, faspayRequest, session);
				}

			}
			HibernateUtil.closeSession();
		}
			} finally {
			ais.database.hibernate.HibernateUtil.closeSession();
		}
	}

}
