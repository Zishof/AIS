package ais.action.master.finpay;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.servlet.FinPayResponse;
import ais.common.Common;
import ais.common.FinpayCommon;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.finpay.FinpayRequest;
import ais.database.model.finpay.FinpayResponse;

public class FinpayBackandProsess extends TimerTask {

	public static JSONObject check(FinpayRequest finpayRequest, Session session) {
		JSONObject finpay = null;
		FinpayResponse finpayResponse = (FinpayResponse) session.createCriteria(FinpayResponse.class)
				.add(Restrictions.eq("paymentCode", finpayRequest.getPaymentCode())).setMaxResults(1)
				.addOrder(Order.desc("id")).uniqueResult();
		if (finpayResponse != null && finpayResponse.getKeterangan() != null
				&& !finpayResponse.getKeterangan().trim().isEmpty()) {
			try {

				try {
					finpay = new JSONObject(finpayResponse.getKeterangan());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}

				if (finpay != null) {

					if (finpayRequest != null
							&& finpayResponse.getResultCode().toString().trim().equalsIgnoreCase("00")) {
						finpayRequest.setFinpayResponse(finpayResponse);
						finpayRequest.setStatus("Payment Sukses");
						finpayRequest.setResultCode(finpayResponse.getResultCode());
						session.getTransaction().begin();
						session.update(finpayRequest);
						session.getTransaction().commit();

						finpayRequest.setStatus(finpayRequest.getStatus());
						finpayRequest.setResultCode(finpayResponse.getResultCode());
						session.getTransaction().begin();
						session.update(finpayRequest);
						session.getTransaction().commit();
						FinPayResponse.prosesResponse(finpayResponse);
						return finpay;
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}
		}

		Mahasiswa mahasiswa = finpayRequest.getMahasiswa();
		BiodataCalonMahasiswa biodataCalonMahasiswa = finpayRequest.getBiodataCalonMahasiswa();

		String add_info1 = "";
		String add_info2 = "";
		String add_info3 = "";

		String amount = finpayRequest.getAmount().intValue() + "";
		if (mahasiswa != null) {
			add_info1 = mahasiswa.getNama() + "-" + mahasiswa.getNim();
			add_info2 = mahasiswa.getJurusan().getNama();
			add_info3 = mahasiswa.getJurusan().getFakultas().getNama();
		} else if (biodataCalonMahasiswa != null) {
			add_info1 = biodataCalonMahasiswa.getNama() + "-" + biodataCalonMahasiswa.getNoRegistrasi();
			add_info2 = biodataCalonMahasiswa.getNoUjian() == null ? "" : biodataCalonMahasiswa.getNoUjian();
			add_info3 = "";
		}
		String add_info4 = "";
		String add_info5 = "";

		String cust_email = "";
		String cust_id = "";
		String cust_msisdn = "";
		String cust_name = "";

		if (mahasiswa != null) {
			try {
				cust_email = mahasiswa.getEmail().trim().isEmpty() ? mahasiswa.getNim() + "@info.com"
						: mahasiswa.getEmail().split(",")[0];
			} catch (Exception e) {
				cust_email = mahasiswa.getEmail().trim().isEmpty() ? mahasiswa.getNim() + "@info.com"
						: mahasiswa.getEmail().trim();
			}
			cust_id = mahasiswa.getNim();
			cust_name = mahasiswa.getNama();
			cust_msisdn = mahasiswa.getTelp() == null || mahasiswa.getTelp().trim().isEmpty() ? "081300000"
					: mahasiswa.getTelp().trim();
		} else if (biodataCalonMahasiswa != null) {
			try {
				cust_email = biodataCalonMahasiswa.getEmail().trim().isEmpty()
						? biodataCalonMahasiswa.getNim() + "@info.com" : biodataCalonMahasiswa.getEmail().split(",")[0];
			} catch (Exception e) {
				cust_email = biodataCalonMahasiswa.getEmail().trim().isEmpty()
						? biodataCalonMahasiswa.getNim() + "@info.com" : biodataCalonMahasiswa.getEmail().trim();
			}
			cust_id = biodataCalonMahasiswa.getNoRegistrasi();
			cust_name = biodataCalonMahasiswa.getNama();
			cust_msisdn = biodataCalonMahasiswa.getHp() == null || biodataCalonMahasiswa.getHp().trim().isEmpty()
					? "081300000" : biodataCalonMahasiswa.getHp().trim();
		}

		TreeMap<String, String> data = FinpayCommon.generateFinpayPostdata(finpayRequest.getInvoice(), amount,
				add_info1, add_info2, add_info3, add_info4, add_info5, cust_email, cust_id, cust_msisdn, cust_name,
				finpayRequest.getPaymentCode());

		try {
			URL url = new URL(Common.getKonfigurasi("new_finpay_gateway_url",
					"https://sandbox.finpay.co.id/servicescode/api/apiFinpay.php").getNilai());
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("POST");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);

			String postData = data.get("sendData");

			con.setRequestProperty("Content-length", String.valueOf(postData.length()));

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

			String res = resultBuf.toString();

			System.out.println("==> res param => " + res);

			JSONObject responseData = new JSONObject(res);

			if (responseData != null) {

				if (finpayResponse == null) {
					finpayResponse = new FinpayResponse();
				}
				finpayResponse.setKeterangan(responseData.toString());
				finpayResponse.setResultDesc(responseData.getString("status_desc"));
				finpayResponse.setResultCode(responseData.getString("status_code"));
				finpayResponse.setMerchant(finpayRequest.getMerchant());
				finpayResponse.setPaymentCode(finpayRequest.getPaymentCode());
				finpayResponse.setKeterangan(res);

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, finpayResponse);
				session.getTransaction().commit();
				session.flush();

				finpayRequest.setFinpayResponse(finpayResponse);
				finpayRequest.setStatus(finpayResponse.getResultDesc());
				finpayRequest.setResultCode(finpayResponse.getResultCode());
				session.getTransaction().begin();
				Common.refreshUpdate(session, finpayRequest);
				session.getTransaction().commit();
				if (finpayResponse.getResultCode().trim().equalsIgnoreCase("00")) {
					FinPayResponse.prosesResponse(finpayResponse);
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}

		return finpay;
	}

	@Override
	public void run() {
		try {
		boolean aktifkan_pembayaran_via_finpay = Common.bolehKonfigurasi("aktifkan_pembayaran_via_finpay", Konfigurasi.TIDAK_AKTIF);
		if (aktifkan_pembayaran_via_finpay) {

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 7);
			Date kemarin = calendar.getTime();

			String sqlRes = "this_.tanggal_dirubah > '" + Common.databaseDateFormat1.get().format(kemarin) + "'";

			Session session = HibernateUtil.currentNativeSession();
			int jumlah = ((Number) session.createCriteria(FinpayRequest.class).setProjection(Projections.rowCount())
					.add(Restrictions.isNull("finpayResponse")).add(Restrictions.sqlRestriction(sqlRes))
					.add(Restrictions.or(
							Restrictions.or(Restrictions.isNull("kodeStatus"), Restrictions.eq("kodeStatus", "0")),
							Restrictions.eq("kodeStatus", "1")))
					.uniqueResult()).intValue();
			// // //System.out.println("FinpayBackandProsess, jumlah = " +
			// jumlah
			// + ", sqlRes = " + sqlRes);
			if (jumlah > 0) {
				@SuppressWarnings("unchecked")
				List<FinpayRequest> finpayRequests = session.createCriteria(FinpayRequest.class)
						.add(Restrictions.sqlRestriction(sqlRes)).addOrder(Order.asc("id"))
						.add(Restrictions.isNull("finpayResponse"))
						.add(Restrictions.or(
								Restrictions.or(Restrictions.isNull("kodeStatus"), Restrictions.eq("kodeStatus", "0")),
								Restrictions.eq("kodeStatus", "1")))
						.list();
				for (FinpayRequest finpayRequest : finpayRequests) {
					FinpayBackandProsess.check(finpayRequest, session);
				}

			}
			HibernateUtil.closeSession();
		}
			} finally {
			ais.database.hibernate.HibernateUtil.closeSession();
		}
	}

}
