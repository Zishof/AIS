package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;

/**
 * Servlet implementation class AmbilLaporanMahasiswa
 */
public class AmbilLaporanVa extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLaporanVa() {
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

	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {

		Enumeration<String> enumeration = request.getParameterNames();
		while (enumeration.hasMoreElements()) {
			String param = enumeration.nextElement();
			System.out.print("  " + param + " = " + request.getParameter(param));
		}

		File file = laporan(request);

		if (file == null) {
			ServletContext sc = getServletContext();
			sc.log("Laporan Mahasiswa tidak ditemukan");
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		resp.setContentType("application/pdf");

		ServletOutputStream out = resp.getOutputStream();
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

	private File laporan(HttpServletRequest request) throws Exception {

		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		Enumeration<String> ennu = request.getParameterNames();
		while (ennu.hasMoreElements()) {
			String name = ennu.nextElement();
			parameters.put(name, request.getParameter(name));
		}

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
		Sekolah sekolah = SekolahUtil.getSekolah(request);

		if (sekolah != null && sekolah.getId() != null) {

			LampiranLain lampiranLain = LampiranLain.ambil(sekolah.getId(), LampiranLain.KOP_SEKOLAH);
			if (lampiranLain != null) {
				parameters.put("kop_surat", lampiranLain.ambilFile().getAbsolutePath());
				parameters.put("kop_surat_local", lampiranLain.ambilFile().getAbsolutePath());
			} else {
				if (perguruanTinggi != null) {
					lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_PT);
					if (lampiranLain != null) {
						parameters.put("kop_surat", lampiranLain.ambilFile().getAbsolutePath());
						parameters.put("kop_surat_local", lampiranLain.ambilFile().getAbsolutePath());
					} else {
						parameters.put("kop_surat", "");
						parameters.put("kop_surat_local", "");
					}

					lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT);
					if (lampiranLain != null) {
						parameters.put("kop_bawah_surat", lampiranLain.ambilFile().getAbsolutePath());
						parameters.put("kop_bawah_surat_local", lampiranLain.ambilFile().getAbsolutePath());
					} else {
						parameters.put("kop_bawah_surat", "");
						parameters.put("kop_bawah_surat_local", "");
					}
				}
			}

			lampiranLain = LampiranLain.ambil(sekolah.getId(), LampiranLain.KOP_BAWAH_SEKOLAH);
			if (lampiranLain != null) {
				parameters.put("kop_bawah_surat", lampiranLain.ambilFile().getAbsolutePath());
				parameters.put("kop_bawah_surat_local", lampiranLain.ambilFile().getAbsolutePath());
			}

		} else {
			if (perguruanTinggi != null) {
				LampiranLain lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_PT);
				if (lampiranLain != null) {
					parameters.put("kop_surat", lampiranLain.ambilFile().getAbsolutePath());
					parameters.put("kop_surat_local", lampiranLain.ambilFile().getAbsolutePath());
				} else {
					parameters.put("kop_surat", "");
					parameters.put("kop_surat_local", "");
				}

				lampiranLain = LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT);
				if (lampiranLain != null) {
					parameters.put("kop_bawah_surat", lampiranLain.ambilFile().getAbsolutePath());
					parameters.put("kop_bawah_surat_local", lampiranLain.ambilFile().getAbsolutePath());
				} else {
					parameters.put("kop_bawah_surat", "");
					parameters.put("kop_bawah_surat_local", "");
				}
			}

		}

		System.out.println("parameters => " + parameters);
		File file = Report.generateFileReport(Report.PDF, parameters, "va", ais.ui.util.WaktuUtil.getDate(),
				Common.locale);
		return file;
	}

}
