package ais.action.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;

public class Recording extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Recording() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub

		String fileNameVideo = request.getParameter("video-filename");
		InputStream fileContent = null;
		try {
			List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
			System.out.println("items size" + items.size());
			for (FileItem item : items) {
				if (item.isFormField()) {
					// Process regular form field (input type="text|radio|checkbox|etc", select,
					// etc).
					String fieldName = item.getFieldName();
					String fieldValue = item.getString();
					System.out.println("fieldName -> " + fieldName + " fieldValue " + fieldValue);

					if (fieldName.equalsIgnoreCase("video-filename")) {
						fileNameVideo = fieldValue;
					}

				} else {
					fileContent = item.getInputStream();
				}
			}

			if (fileContent != null) {
				File outputfile = new File(Common.ambilREAL_PATH_REPORT() + "/" + fileNameVideo);
				System.out.println("outputfile video -> " + outputfile.getAbsolutePath());
				java.nio.file.Files.copy(fileContent, outputfile.toPath(), StandardCopyOption.REPLACE_EXISTING);

				IOUtils.closeQuietly(fileContent);
			}

			System.out.println("Uploading done..");
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Recording.java:76");
		}

		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("status", "OK");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Recording.java:84");
		}
		String body = jsonObject.toString();
		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		response.addHeader("Access-Control-Allow-Origin", "*");
		PrintWriter writer = response.getWriter();

		writer.write(body);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
