package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.doku.DokuRequest;

/**
 * Servlet implementation class CheckISBN
 */
public class DokuVerifyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	// private static PembayaranUtil pembayaranUtil =
	// PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DokuVerifyServlet() {
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

		// Read from request
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();
		System.out.println("==> DokuVerifyServlet data => " + data);

		String[] splt = StringUtils.split(data, "&");
		Map<String, String> param = new HashMap<String, String>();
		for (String s : splt) {
			try {
				String[] v = StringUtils.split(s, "=");
				param.put(v[0], v[1]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DokuVerifyServlet.java:87");

			}
		}

		System.out.println("==> param => " + param + ", " + request.getQueryString());

		int AMOUNT = (int) Double.parseDouble(param.get("AMOUNT").trim());

		String hasil = "Stop";
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Double amount = (Double) session.createCriteria(DokuRequest.class).setProjection(Projections.property("amount"))
					.add(Restrictions.ilike("trxId", param.get("WORDS"), MatchMode.EXACT)).setMaxResults(1).uniqueResult();
			if (amount != null && AMOUNT == amount.intValue()) {
				hasil = "Continue";
			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuVerifyServlet.java:107");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuVerifyServlet.java:108");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuVerifyServlet.java:109");}
			}
		}

		System.out.println("==> hasil => " + hasil);

		response.setHeader("Content-Type", "text/plain");

		PrintWriter writer = response.getWriter();
		writer.write(hasil);
	}

}
