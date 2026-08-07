package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Servlet implementation class CheckISBN
 */
public class Psb extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Psb() {
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

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String piilhan = getPiilhanTampilanDomain(request);
		if (PerguruanTinggi.TAMPILAN_BARU.equals(piilhan)) {
			request.setAttribute("new_context", "psb");
			request.getRequestDispatcher("/WEB-INF/new/index.jsp").forward(request, response);
		} else {
			request.getRequestDispatcher("/WEB-INF/z/x/y/psb.zul").forward(request, response);
		}
	}

	private String getPiilhanTampilanDomain(HttpServletRequest request) {
		try {
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
			if (pt != null && !PerguruanTinggi.TAMPILAN_DEFAULT.equals(pt.getPiilhanTampilan())) {
				return pt.getPiilhanTampilan();
			}
			boolean[] ptAtauSekolah = Common.chekPtAtauSekolah();
			boolean sekolahMode = ptAtauSekolah != null && ptAtauSekolah.length > 1 && ptAtauSekolah[1];
			if (sekolahMode) {
				Sekolah sekolah = SekolahUtil.getSekolah(request);
				if (sekolah != null && !Sekolah.TAMPILAN_DEFAULT.equals(sekolah.getPiilhanTampilan())) {
					return sekolah.getPiilhanTampilan();
				}
				Yayasan yayasan = SekolahUtil.getYayasan(request);
				if (yayasan != null && !Yayasan.TAMPILAN_DEFAULT.equals(yayasan.getPiilhanTampilan())) {
					return yayasan.getPiilhanTampilan();
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return PerguruanTinggi.TAMPILAN_DEFAULT;
	}

}
