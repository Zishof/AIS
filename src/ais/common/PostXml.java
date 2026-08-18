package ais.common;

import java.net.*;
import java.io.*;

public class PostXml {

	public static void main(String[] args) {

		try {
			String xmldata = "<s11:Envelope xmlns:s11='http://schemas.xmlsoap.org/soap/envelope/'>"
					+ "<s11:Body>"
					+ "  <ns1:GetToken xmlns:ns1='http://localhost/soap/WSPDDIKTI'>"
					+ "    <username>fauzi</username>"
					+ "    <password>fauzi</password>"
					+ "  </ns1:GetToken>"
					+ "</s11:Body>" + "</s11:Envelope>";

			// Create socket
			String hostname = "localhost";
			int port = 8082;
			InetAddress addr = InetAddress.getByName(hostname);
			@SuppressWarnings("resource")
			Socket sock = new Socket(addr, port);

			// Send header
			String path = "/ws/live.php";
			BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(
					sock.getOutputStream(), "UTF-8"));
			// You can use "UTF8" for compatibility with the Microsoft virtual
			// machine.
			wr.write("POST " + path + " HTTP/1.1\r\n");
			wr.write("Host: " + hostname + ":" + port + "\r\n");
			wr.write("SOAPAction: http://" + hostname + path + "/GetToken\r\n");
			wr.write("Content-Length: " + xmldata.length() + "\r\n");
			wr.write("Content-Type: text/xml; charset=\"utf-8\"\r\n");
			wr.write("\r\n");

			// Send data
			wr.write(xmldata);
			wr.flush();

			// Response
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					sock.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null)
				System.out.println(line);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}
}
