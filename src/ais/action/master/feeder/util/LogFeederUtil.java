package ais.action.master.feeder.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

/**
 * Utilitas uji coba/debug (dijalankan lewat {@code main}, bukan dipakai sebagai komponen aplikasi)
 * untuk mem-parsing respons SOAP XML dari web service PDDIKTI Feeder ({@code WSPDDIKTI}) dan
 * mengekstrak isi elemen {@code error_desc} di dalamnya. Berguna sebagai contoh cepat cara membaca
 * pesan galat dari balasan operasi seperti {@code InsertRecord} tanpa perlu memanggil layanan
 * Feeder sungguhan — payload SOAP contoh ditulis langsung sebagai literal string di dalam method.
 */
public class LogFeederUtil {

	/**
	 * Mem-parsing contoh payload SOAP {@code InsertRecordResponse} (literal, ditulis di dalam
	 * method) memakai parser XML Jsoup, mengambil teks elemen {@code error_desc} yang tidak kosong
	 * (bila ada beberapa, hanya yang terakhir yang tersimpan), dan mencetaknya ke stdout.
	 */
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
