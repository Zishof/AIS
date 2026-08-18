package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.report.Report;
import ais.common.Common;

/**
 * Servlet implementation class AmbilLaporanMahasiswa
 */
public class AmbilLaporanDaftarPegawai extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLaporanDaftarPegawai() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Enumeration<String> enumeration = request.getParameterNames();
		while (enumeration.hasMoreElements()) {
			String param = enumeration.nextElement();
			System.out.print("  " + param + " = " + request.getParameter(param));
		}

		String type = request.getParameter("type") == null ? Report.HTML : request.getParameter("type");
		String namaLaporan = request.getParameter("namaLaporan") == null ? "daftar_pegawai"
				: request.getParameter("namaLaporan");

		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		File file = null;

		if (!type.equalsIgnoreCase("image")) {
			file = Report.generateFileReport(type, parameters, "employ/" + namaLaporan, ais.ui.util.WaktuUtil.getDate(),
					Common.locale);
			response.setContentType("application/" + type);
		} else {
			file = Report.generateFileImageReport(type, parameters, "employ/" + namaLaporan,
					ais.ui.util.WaktuUtil.getDate(), Common.locale);
			response.setContentType("image/jpeg");
		}

		// System.out.println("file = " + file);

		ServletOutputStream out = response.getOutputStream();
		FileInputStream in = new FileInputStream(file);
		int length = (int) file.length();

		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		while ((length = in.read(buffer)) != -1) {
			out.write(buffer, 0, length);
		}

		in.close();
		out.flush();

	}

}
