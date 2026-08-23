package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.ErrorAuditUtil;

import java.util.UUID;

/**
 * Servlet implementation class CheckISBN
 */
public class Error extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Error() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
		if (throwable == null) {
			throwable = (Throwable) request.getAttribute("ais.error.throwable");
		}
		String traceId = (String) request.getAttribute("ais.error.trace");
		if (traceId == null || traceId.trim().length() == 0) {
			traceId = UUID.randomUUID().toString();
			request.setAttribute("ais.error.trace", traceId);
		}
		if (request.getAttribute("ais.error.content") == null) {
			if (throwable == null) {
				Object status = request.getAttribute("javax.servlet.error.status_code");
				Object message = request.getAttribute("javax.servlet.error.message");
				int statusCode = status instanceof Number ? ((Number) status).intValue() : 500;
				if (statusCode >= 500 || statusCode == 429) {
					/* Bila container hanya meneruskan status tanpa exception, akar masalah sudah
					 * dicatat oleh request asal. Jangan membuat ServletException sintetis di /error
					 * karena hasilnya audit kedua yang menutupi stack trace pertama. */
					response.setStatus(statusCode);
					request.setAttribute("ais.error.content", statusCode == 429
							? "Layanan sedang sibuk karena terlalu banyak permintaan bersamaan. Silakan tunggu beberapa saat lalu coba kembali."
							: "Permintaan belum dapat diproses. Silakan coba kembali; bila berulang, sampaikan kode waktu kejadian kepada Administrator.");
					request.setAttribute("ais.error.log_id", null);
				} else {
					throwable = new ServletException("HTTP " + status + ": " + message);
				}
			}
			if (request.getAttribute("ais.error.content") == null) {
				ErrorAuditUtil.ErrorAuditResult audit = ErrorAuditUtil.recordVisibleFailure(throwable,
						"Container error page", request, traceId);
				request.setAttribute("ais.error.content", audit == null ? null : audit.getContent());
				request.setAttribute("ais.error.log_id", audit == null ? null : audit.getErrorLogId());
				request.setAttribute("ais.error.throwable", throwable);
			}
		}
		request.setAttribute("ais.error.show_detail", Boolean.valueOf(ErrorAuditUtil.isUiDetailActive()));
		request.getRequestDispatcher("/WEB-INF/u/error.jsp").forward(request, response);

	}

}
