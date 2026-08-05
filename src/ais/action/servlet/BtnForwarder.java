package ais.action.servlet;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Servlet implementation class BniForwarder
 */
public class BtnForwarder extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public BtnForwarder() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String postData = buffer.toString();

//		String postData = "{\"layanan\":\"STMIK Palangkaraya\",\"flag\":\"F\",\"angkatan\":\"2021\",\"jenisbayar\":\"siswa Baru - Heregistrasi (Daftar Ulang)\",\"kodelayanan\":\"113066\",\"description\":\"Jl. Batu Suli No.35\",\"va\":\"946160016691629533\",\"noid\":\"202100149\",\"ref\":\"9D8DF6D5DC\",\"tagihan\":\"5705000\",\"expired\":\"\",\"nama\":\"ANNISA ANGGRAINI\",\"reserve\":\"943\",\"kodejenisbyr\":\"103\"}";
		System.out.println("==> BtnForwarder request => " + postData);

		String strURL = request.getParameter("strURL");
		System.out.println("strURL => " + strURL);

//		String strURL = "https://vabtn.btn.co.id:9022/v1/stimikpr/createVA";
		String hasil = "";
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {

			HttpPost httpPost = new HttpPost(strURL);

			String prefix = request.getParameter("prefix");// "BSTIMPR";
			String postfix = request.getParameter("postfix"); // "OLVWnHmtrOVnCKzKvnLN0JdIp8uFOtWu";
			String signature = request.getParameter("signature");// "5f8aad4fcca3fb5cefab13e2f57b0591da3d6accca96b266b5c45d14a5460c89";

			StringEntity entity = new StringEntity(postData);
			httpPost.setEntity(entity);
			// httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");
			httpPost.setHeader("id", prefix);
			httpPost.setHeader("key", postfix);

			System.out.println("id => " + prefix);
			System.out.println("key => " + postfix);
			System.out.println("signature => " + signature);

			httpPost.setHeader("signature", signature);

			CloseableHttpResponse r = httpclient.execute(httpPost);

			hasil = EntityUtils.toString(r.getEntity());

		} finally {
			httpclient.close();
		}

		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();
		writer.write(hasil);
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
