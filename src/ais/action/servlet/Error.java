package ais.action.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.ErrorAuditUtil;

import java.util.UUID;

/**
 * Servlet halaman error generik aplikasi, dipetakan sebagai {@code <error-page>} kontainer
 * (lihat {@code web.xml}) untuk kode status HTTP dan/atau exception tak tertangani yang lolos
 * dari layer lain. Tidak menampilkan stack trace mentah ke pengguna: setiap kegagalan dicatat
 * ke audit lewat {@link ErrorAuditUtil#recordVisibleFailure} dengan sebuah trace ID unik (dibuat
 * dari {@link UUID} bila belum ada), lalu pesan yang ramah-pengguna (atau detail teknis bila
 * {@link ErrorAuditUtil#isUiDetailActive()} aktif) diteruskan ke {@code /WEB-INF/u/error.jsp}
 * untuk dirender.
 */
public class Error extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet; tidak melakukan inisialisasi
	 * khusus.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public Error() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan seluruhnya ke {@link #process}.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}.
	 *
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Membangun konten halaman error dan meneruskannya ke {@code /WEB-INF/u/error.jsp}.
	 * <p>Urutan kerja: (1) mengambil {@code Throwable} asal dari atribut standar kontainer
	 * {@code javax.servlet.error.exception}, atau atribut khusus AIS
	 * {@code ais.error.throwable} bila yang pertama kosong; (2) memastikan tersedia
	 * {@code ais.error.trace} (trace ID) untuk korelasi log, membuatnya dari {@link UUID}
	 * bila belum ada; (3) bila belum ada {@code ais.error.content} yang disiapkan oleh
	 * pemanggil sebelumnya, dan tidak ada exception yang tercatat, memeriksa
	 * {@code javax.servlet.error.status_code}: untuk status {@code >= 500} atau {@code 429}
	 * (rate limit) menampilkan pesan generik ramah-pengguna TANPA membuat audit kedua (agar
	 * tidak menutupi stack trace asal yang sudah dicatat request semula), sedangkan status
	 * lain dibungkus jadi {@link ServletException} sintetis; (4) bila konten masih kosong,
	 * mencatat kegagalan lewat {@link ErrorAuditUtil#recordVisibleFailure} dan mengisi
	 * {@code ais.error.content}/{@code ais.error.log_id}/{@code ais.error.throwable} dari
	 * hasilnya; (5) menyetel {@code ais.error.show_detail} sesuai
	 * {@link ErrorAuditUtil#isUiDetailActive()} lalu forward ke {@code /WEB-INF/u/error.jsp}.
	 */
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
