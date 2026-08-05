package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.master.library.util.LibraryUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.KembaliPengadaanItem;
import ais.database.model.library.PeminjamanPengadaanItem;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

/**
 * Servlet implementation class AmbilLaporanPerpustakaan
 */
public class AmbilLaporanPerpustakaan extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLaporanPerpustakaan() {
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
		String namaLaporan = request.getParameter("laporan");
		String type = request.getParameter("type");
		Long id = Long.parseLong(request.getParameter("id"));
		String locale = request.getParameter("locale");

		String kodeBarcode = "";
		Map<String, Object> parameters = new HashMap<String, Object>();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			if (namaLaporan.equalsIgnoreCase("peminjaman")) {
				final PeminjamanPengadaanItem peminjamanPengadaanItem = (PeminjamanPengadaanItem) session
						.createCriteria(PeminjamanPengadaanItem.class).add(Restrictions.idEq(id)).uniqueResult();
				final File myfilebarcode = new File(getServletContext().getRealPath("/report") + "/barcode_"
						+ peminjamanPengadaanItem.getKode() + ".png");
				Barcode mybarcode = BarcodeFactory.createCode128B(peminjamanPengadaanItem.getKode());
				BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
				kodeBarcode = myfilebarcode.getAbsolutePath();

				parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(
						peminjamanPengadaanItem.getKembaliPengadaanItem(), peminjamanPengadaanItem));

			} else if (namaLaporan.equalsIgnoreCase("perpanjangan")) {
				final PeminjamanPengadaanItem peminjamanPengadaanItem = (PeminjamanPengadaanItem) session
						.createCriteria(PeminjamanPengadaanItem.class).add(Restrictions.idEq(id)).uniqueResult();
				final File myfilebarcode = new File(getServletContext().getRealPath("/report") + "/barcode_"
						+ peminjamanPengadaanItem.getKode() + ".png");
				Barcode mybarcode = BarcodeFactory.createCode128B(peminjamanPengadaanItem.getKode());
				BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
				kodeBarcode = myfilebarcode.getAbsolutePath();

				parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(
						peminjamanPengadaanItem.getKembaliPengadaanItem(), peminjamanPengadaanItem));

			} else if (namaLaporan.equalsIgnoreCase("pengembalian")) {
				final KembaliPengadaanItem kembaliPengadaanItem = (KembaliPengadaanItem) session
						.createCriteria(KembaliPengadaanItem.class).add(Restrictions.idEq(id)).uniqueResult();
				final File myfilebarcode = new File(
						getServletContext().getRealPath("/report") + "/barcode_" + kembaliPengadaanItem.getKode() + ".png");
				Barcode mybarcode = BarcodeFactory.createCode128B(kembaliPengadaanItem.getKode());
				BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
				kodeBarcode = myfilebarcode.getAbsolutePath();

				parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(kembaliPengadaanItem,
						kembaliPengadaanItem.getPeminjamanPengadaanItem()));

			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanPerpustakaan.java:118");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanPerpustakaan.java:119");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLaporanPerpustakaan.java:120");}
			}
		}

		parameters.put("id", id);
		parameters.put("kode_barcode", kodeBarcode);

		File file = null;
		if (type.equals("img")) {
			file = Report.generateFileImageReport(Report.PDF, parameters, "library/" + namaLaporan,
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
		} else {
			file = Report.generateFileReport(Report.PDF, parameters, "library/" + namaLaporan,
					ais.ui.util.WaktuUtil.getDate(),
					locale == null || locale.equals("id") ? Common.locale : Common.localeEn);
		}

		if (type.equals("img")) {
			resp.setContentType("image/jpeg");
		} else {
			resp.setContentType("application/pdf");
		}

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
