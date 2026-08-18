package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ais.action.ws.PembayaranAction;
import ais.action.ws.model.Response;
import ais.common.Common;
import ais.database.model.LogHostToHost;

public class Payment extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private PembayaranAction action = new PembayaranAction();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Payment() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub

		JSONObject jsonObject = new JSONObject();

		try {
//			jsonObject.put("BillDetail", new JSONObject());
			jsonObject.put("Info1", "Payment");
			jsonObject.put("Info2", "");
			jsonObject.put("Info3", "");
			jsonObject.put("Info4", "");
			jsonObject.put("Info5", "");
//			jsonObject.put("StatusBill", "2");
//			jsonObject.put("Currency", "IDR");

			StringBuilder buffer = new StringBuilder();
			BufferedReader reader = request.getReader();
			String line;
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}
			String data = buffer.toString();
			JSONObject req = new JSONObject(data);

			System.out.println(req);

			String PassApp = req.getString("PassApp");
			if (PassApp == null || !PassApp.equals(Common.getKonfigurasi("BrivaPassApp", "1234567890").getNilai())) {
				jsonObject.put("StatusBill", "11");
			} else {

				String nim = req.getString("BrivaNum");
				String reffNumber = req.getString("TransaksiID");
				String tanggalBayar = req.getString("TransmisiDateTime");
				String jamBayar = req.getString("TransmisiDateTime");
				String userID = req.getString("TerminalID");
				String nominalTagihan = req.getString("PaymentAmount");
				String namaCabang = req.getString("BankID");
				
				
				

				String nama = ("=============================== PAY PAYMENT --> pay dengan NIM = " + nim
						+ ", reffNumber = " + reffNumber + ", tanggalBayar = " + tanggalBayar + ", userID = " + userID
						+ ", namaCabang = " + namaCabang + ", nominalTagihan = " + nominalTagihan);
				LogHostToHost logHostToHost = new LogHostToHost();
				logHostToHost.setInfo0(nim);
				logHostToHost.setInfo1(reffNumber);
				logHostToHost.setInfo2(tanggalBayar);
				logHostToHost.setInfo3(jamBayar);
				logHostToHost.setInfo4(userID);
				logHostToHost.setInfo5(namaCabang);
				logHostToHost.setInfo6(nominalTagihan);

				int substrBriOnline = 0;
				try {
					substrBriOnline = Integer.parseInt(Common.getKonfigurasi("substrBriOnline", "0").getNilai());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Payment.java:93");
					// TODO: handle exception
				}

				String n = substrBriOnline == 0 ? nim : nim.substring(substrBriOnline);

				Response resp = action.pay(n, reffNumber, tanggalBayar, jamBayar, userID, namaCabang, nominalTagihan,
						nama, logHostToHost, request);
				JSONObject StatusPayment = new JSONObject();
				StatusPayment.put("ErrorDesc", resp.getResponse_code().equals("03") ? "Bill Already paid"
						: resp.getResponse_code().equals("00") ? "Success" : "Fail");
				StatusPayment.put("ErrorCode", resp.getResponse_code());
				StatusPayment.put("isError", resp.getResponse_code().equals("00") ? "0" : "1");

//				if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.SUCCESS)) {
//					jsonObject.put("StatusBill", "0");
//				} else if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.BILLS_HAVE_BEEN_PAID)) {
//					jsonObject.put("StatusBill", "1");
//				} else if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.NIM_NOT_FOUND)
//						|| resp.getResponse_code().equalsIgnoreCase(ConstantUtil.BILLS_NOT_FOUND)) {
//					jsonObject.put("StatusBill", "2");
//				} else if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.BILLS_HAS_EXPIRED)) {
//					jsonObject.put("StatusBill", "3");
//				} else {
//					jsonObject.put("StatusBill", "9");
//				}
				jsonObject.put("StatusPayment", StatusPayment);
				jsonObject.put("Info1", resp.getNama());
				jsonObject.put("Info2", resp.getProdi());
				jsonObject.put("Info3", resp.getFakultas());
				jsonObject.put("Info4", "Semester " + resp.getSemester());
				jsonObject.put("Info5", resp.getInfo1());
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Payment.java:127");
		}
		String body = jsonObject.toString();
		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		PrintWriter writer = response.getWriter();

		writer.write(body);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
