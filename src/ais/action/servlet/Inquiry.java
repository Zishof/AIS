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
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.database.model.LogHostToHost;

/**
 * Komponen batas HTTP/servlet untuk inquiry. Tipe ini menerima input dari luar aplikasi,
 * meneruskannya ke layanan domain, lalu membentuk respons tanpa menduplikasi aturan bisnis.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * HttpServlet}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PembayaranAction action};
 * pembacaan/pencarian ({@code doGet()}); operasi domain lain ({@code doPost()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see HttpServlet
 */
public class Inquiry extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private PembayaranAction action = new PembayaranAction();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Inquiry() {
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
			jsonObject.put("BillDetail", new JSONObject());
			jsonObject.put("Info1", "Inquiry");
			jsonObject.put("Info2", "");
			jsonObject.put("Info3", "");
			jsonObject.put("Info4", "");
			jsonObject.put("Info5", "");
			jsonObject.put("StatusBill", "2");
			jsonObject.put("Currency", "IDR");

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
				String nama = ("=============================== PAY INQUIRY --> inquery dengan = " + req);

				String nim = req.getString("BrivaNum");
				LogHostToHost logHostToHost = new LogHostToHost();
				logHostToHost.setInfo0(nim);

				int substrBriOnline = 0;
				try {
					substrBriOnline = Integer.parseInt(Common.getKonfigurasi("substrBriOnline", "0").getNilai());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Inquiry.java:76");
					// TODO: handle exception
				}

				String n = substrBriOnline == 0 ? nim : nim.substring(substrBriOnline);

				Response resp = action.inquery(n, nama, logHostToHost, request);
				JSONObject BillDetail = new JSONObject();
				BillDetail.put("BillAmount", resp.getTotal_amount());
				BillDetail.put("BillName", resp.getNama());
				BillDetail.put("BrivaNum", nim);

				jsonObject.put("BillDetail", BillDetail);
				jsonObject.put("Info1", resp.getAmount());
				jsonObject.put("Info2", resp.getProdi());
				jsonObject.put("Info3", resp.getFakultas());
				jsonObject.put("Info4", "Semester " + resp.getSemester());
				jsonObject.put("Info5", resp.getInfo1());

				if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.SUCCESS)) {
					jsonObject.put("StatusBill", "0");
				} else if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.BILLS_HAVE_BEEN_PAID)) {
					jsonObject.put("StatusBill", "1");
				} else if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.NIM_NOT_FOUND)
						|| resp.getResponse_code().equalsIgnoreCase(ConstantUtil.BILLS_NOT_FOUND)) {
					jsonObject.put("StatusBill", "2");
				} else if (resp.getResponse_code().equalsIgnoreCase(ConstantUtil.BILLS_HAS_EXPIRED)) {
					jsonObject.put("StatusBill", "3");
				} else {
					jsonObject.put("StatusBill", "9");
				}

			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Inquiry.java:110");
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
