package ais.action.master.feeder.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

public class LogFeederUtil {

	public static void main(String[] argv) {
		String data = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><SOAP-ENV:Body><ns1:InsertRecordResponse xmlns:ns1=\"http://202.180.27.6/soap/WSPDDIKTI\"><output><error_code xsi:type=\"xsd:string\">0</error_code><error_desc xsi:type=\"xsd:string\"/><result><error_code xsi:type=\"xsd:int\">103</error_code><error_desc xsi:type=\"xsd:string\">Error database. Periksa kembali parameter yang dikirim (nama kolom, tipe data, filter, order atau parameter lainnya).</error_desc></result></output></ns1:InsertRecordResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>";
		Document doc = Jsoup.parse(data, "", Parser.xmlParser());
		String error = "";
		for (Element e : doc.select("error_desc")) {
			String err = e.text();
			if (!err.trim().isEmpty()) {
				error = err;
			}
		}

		System.out.println(error);
	}

}
