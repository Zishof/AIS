package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailLogLogin;
import ais.database.model.GeneralValueObject;
import ais.database.model.LogLogin;

/**
 * Servlet implementation class CheckISBN
 */
public class DisplayMenu extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DisplayMenu() {
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

		ais.database.model.Menu menu = null;
		if (request.getParameter("menu") != null) {
			menu = (ais.database.model.Menu) GeneralValueObject.ambilData(ais.database.model.Menu.class,
					request.getParameter("menu").trim(), true);
		}
		String p = request.getParameter("p");
		if (menu != null || (p != null && (p.equalsIgnoreCase("prestasi") || p.equalsIgnoreCase("pustaka")
				|| p.equalsIgnoreCase("pengajuananda") || p.equalsIgnoreCase("akademik")
				|| p.equalsIgnoreCase("suratmenyurat") || p.equalsIgnoreCase("pengadaan")
				|| p.equalsIgnoreCase("pembayaran") || p.equalsIgnoreCase("keuangan") || p.equalsIgnoreCase("akuntansi")
				|| p.equalsIgnoreCase("kepegawaian") || p.equalsIgnoreCase("kinerja")
				|| p.equalsIgnoreCase("presensi")))) {

			if (menu != null) {
				Session session = null;
				try {
					LogLogin login = (LogLogin) request.getSession().getAttribute("login");
					request.getSession().setAttribute("currentMenu", menu);

					DetailLogLogin detailLogLogin = new DetailLogLogin();
					detailLogLogin.setKeterangan(menu.getLabel());
					detailLogLogin.setWaktu(ais.ui.util.WaktuUtil.getDate());
					detailLogLogin.setLogLogin(login);

					session = HibernateUtil.getSessionFactory().openSession();
					try {
						session.getTransaction().begin();
						session.save(detailLogLogin);
						session.getTransaction().commit();
						// session.disconnect();

						request.getSession().setAttribute("detailLogLogin", detailLogLogin);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DisplayMenu.java:94");
//						e.printStackTrace();
					}

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DisplayMenu.java:98");

				} finally {
					if (session != null) {
						try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DisplayMenu.java:102");}
						try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DisplayMenu.java:103");}
						try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DisplayMenu.java:104");}
					}
				}
			}
			try {

				if (p != null && p.equalsIgnoreCase("presensi")) {
					String dispacher = "/WEB-INF/z/x/y/presensi.zul";
					request.getRequestDispatcher(dispacher).forward(request, response);
				} else {
					if (menu != null) {
						Class.forName(menu.getUrl().trim()).newInstance();
					}
					String dispacher = "/WEB-INF/z/x/y/common/display.zul?menu=" + (menu == null || menu.getId() == null ? -11L : menu.getId())
							+ "&p=" + request.getParameter("p");
					request.getRequestDispatcher(dispacher).forward(request, response);
				}
			} catch (Exception e) {
				String dispacher = "/WEB-INF/z/x/y" + menu.getUrl();
				request.getRequestDispatcher(dispacher).forward(request, response);
			}
		} else {

		}

	}

}
