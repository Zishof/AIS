package ais.common;

import java.util.Iterator;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

public class CommonFeeder {

	public static SOAPConnectionFactory soapConnectionFactory;
	public static SOAPConnection soapConnection;

	static {
		try {
			soapConnectionFactory = SOAPConnectionFactory.newInstance();
			soapConnection = soapConnectionFactory.createConnection();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	public static String WSDL_URL = "http://localhost:8082/ws/live.php";
	public static String SERVER_URI = "http://localhost/soap/WSPDDIKTI";

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		String token = login("fauzi", "fauzi");
		System.out.println("token = " + token);
	}

	@SuppressWarnings("rawtypes")
	public static String login(String username, String password)
			throws Exception {
		SOAPMessage soapMessage = soapConnection.call(
				CommonFeederHelper.createLoginRequest(username, password),
				WSDL_URL);
		SOAPPart soapPart = soapMessage.getSOAPPart();

		// SOAP Envelope
		SOAPEnvelope envelope = soapPart.getEnvelope();
		SOAPBody soapBody = envelope.getBody();
		Iterator iterator = soapBody.getChildElements();
		while (iterator.hasNext()) {
			SOAPElement soapBodyElem = (SOAPElement) iterator.next();
			String response = soapBodyElem.getTextContent();
			return response;
		}
		return "";
	}

}
