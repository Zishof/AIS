package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.common.Common;

/**
 * Servlet implementation class AmbilFotoDosen
 */
public class AmbilFileServer extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilFileServer() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
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
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	private void process(HttpServletRequest request, HttpServletResponse resp)
			throws Exception {

		String filename = request.getParameter("file");
		ServletContext sc = getServletContext();
		if (filename == null || filename.trim().equals("")) {
			sc.log("file harus diisi !");
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		try {

			String mimeType = sc==null?null:sc.getMimeType(filename);
			// Set content type
			resp.setContentType(mimeType);
			String headerKey = "Content-Disposition";
			String headerValue = String.format("attachment; filename=\"%s\"", filename);
			resp.setHeader(headerKey, headerValue);

			File file = new File(filename);
			resp.setContentLength((int) file.length());
			// Open the file and output streams
			FileInputStream in = new FileInputStream(file);
			OutputStream out = resp.getOutputStream();
			byte[] buf = new byte[1024];
			int count = 0;
			while ((count = in.read(buf)) >= 0) {
				out.write(buf, 0, count);
			}
			in.close();
			out.close();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

}
