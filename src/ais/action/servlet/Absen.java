package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ais.action.master.resources.ELearningResource;
import ais.action.master.resources.PosResource;
import ais.action.master.resources.model.CommonID;
import ais.common.Common;
import ais.ui.util.WaktuUtil;

public class Absen extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public Absen() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject jsonObject = new JSONObject();
		try {
			addCorsHeaders(response);

			if (!isValidPassword(request)) {
				jsonObject.put("status", "91");
				jsonObject.put("info", "Password salah");
				writeJson(response, jsonObject);
				return;
			}

			String id = safeTrim(request.getParameter("id"));
			String data = safeTrim(request.getParameter("data"));
			String lat = safeTrim(request.getParameter("lat"));
			String lng = safeTrim(request.getParameter("lng"));
			String idFinger = safeTrim(request.getParameter("id_finger"));
			String waktu = safeTrim(request.getParameter("waktu"));
			String state = safeTrim(request.getParameter("state"));

			if (idFinger.length() > 0 || waktu.length() > 0) {
				if (idFinger.length() == 0 || waktu.length() == 0) {
					jsonObject.put("status", "92");
					jsonObject.put("info", "Parameter id_finger dan waktu wajib diisi bersamaan");
					writeJson(response, jsonObject);
					return;
				}

				CommonID commonID = PosResource.absen(idFinger, waktu, state);
				if (commonID == null) {
					jsonObject.put("status", "90");
					jsonObject.put("info", "Absensi gagal diproses");
				} else {
					String info = commonID.getInfo4();
					jsonObject.put("status", info != null && info.toLowerCase().startsWith("gagal") ? "90" : "00");
					jsonObject.put("id", commonID.getInfo1());
					jsonObject.put("waktu", commonID.getInfo2());
					jsonObject.put("nama", commonID.getInfo3());
					jsonObject.put("info", commonID.getInfo4());
					jsonObject.put("mulai", commonID.getInfo5());
					jsonObject.put("selesai", commonID.getInfo6());
				}
			} else {
				CommonID hasil = ELearningResource.doAbsen(id, data, lat, lng,
						"Absensi online sukses pada " + Common.dateFormat5.get().format(WaktuUtil.getDate()));
				if (hasil == null) {
					jsonObject.put("status", "90");
					jsonObject.put("info", "Absensi online gagal diproses");
				} else {
					jsonObject.put("status", "00");
					jsonObject.put("id", hasil.getId());
					jsonObject.put("prodi", hasil.getInfo3());
					jsonObject.put("nama", hasil.getInfo1());
					jsonObject.put("foto", hasil.getInfo16());
					jsonObject.put("nim", hasil.getInfo2());
					jsonObject.put("status_absen", hasil.getInfo10());
					jsonObject.put("status", hasil.getInfo10() == null || hasil.getInfo10().trim().length() == 0 ? "00"
							: hasil.getInfo10());
				}
			}
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Absen.java:90");
			}
			try {
				jsonObject.put("status", "90");
				jsonObject.put("info", "Terjadi kesalahan internal: " + (e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Absen.java:95");
			}
		}
		writeJson(response, jsonObject);
	}

	private boolean isValidPassword(HttpServletRequest request) {
		try {
			String headerPassword = request == null ? null : request.getHeader("p");
			if (headerPassword == null || headerPassword.trim().length() == 0) {
				return false;
			}
			String password = Common.getKonfigurasi("password_absen",
					"4GUb3KPArA78B9AOmKj3pLivo49IEPfQDFHbeCLFpsAG6fgWQZ").getNilai();
			return password != null && password.trim().equalsIgnoreCase(headerPassword.trim());
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Absen.java:113");
			}
			return false;
		}
	}

	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private void addCorsHeaders(HttpServletResponse response) {
		if (response == null) {
			return;
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, p");
	}

	private void writeJson(HttpServletResponse response, JSONObject jsonObject) throws IOException {
		String body = jsonObject == null ? "{}" : jsonObject.toString();
		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		addCorsHeaders(response);
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
			writer.write(body);
			writer.flush();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Absen.java:146");
				}
			}
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		addCorsHeaders(response);
	}
}
