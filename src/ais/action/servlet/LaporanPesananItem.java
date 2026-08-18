package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.PesananAnggota;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

/**
 * Servlet implementation class LaporanPesananItem
 */
public class LaporanPesananItem extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LaporanPesananItem() {
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {
		String myid = request.getParameter("id");
		System.out.println("myid = " + myid);
		String[] strids = myid.split(",");
		List<Long> ids = new ArrayList<Long>();
		for (String strid : strids) {
			Long id = Long.parseLong(strid.trim());
			ids.add(id);
		}
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<PesananAnggota> pesananAnggotas = session.createCriteria(PesananAnggota.class)
					.add(Restrictions.in("id", ids)).list();
			for (PesananAnggota pesananAnggota : pesananAnggotas) {

				if (pesananAnggota != null) {

					Map<String, Object> map = new java.util.HashMap<String, Object>();
					String code = pesananAnggota.getAnggota().toString()
							+ (pesananAnggota.getPerpustakaan() == null ? ""
									: "\n" + pesananAnggota.getPerpustakaan().getNama())
							+ "\n" + pesananAnggota.getItem().getIsbn() + " - " + pesananAnggota.getItem().getNama()
							+ "\nStatus : " + pesananAnggota.getStatus() + " - "
							+ Common.dateFormat5.get().format(pesananAnggota.getTanggal());
					map.put("code", code);

					@SuppressWarnings("deprecation")
					final File myfile = new File(
							request.getRealPath("/report") + "/barcode_" + pesananAnggota.getKode() + ".png");

					if (!myfile.exists()) {
						Barcode mybarcode = BarcodeFactory.createCode128(pesananAnggota.getKode());
						BarcodeImageHandler.savePNG(mybarcode, myfile);
					}

					map.put("barcode", myfile.getAbsolutePath());
					map.put("c_code", pesananAnggota.getKode());
					maps.add(map);

				}
			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/LaporanPesananItem.java:113");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/LaporanPesananItem.java:114");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/LaporanPesananItem.java:115");}
			}
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("maps", maps);

		File file = Report.generateFileReport(Report.PDF, parameters, "library/pesanan",
				ais.ui.util.WaktuUtil.getDate(), Common.locale);

		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=\"pesanan_anggota.pdf\"");

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

}
