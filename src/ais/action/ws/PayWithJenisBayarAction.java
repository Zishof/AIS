package ais.action.ws;

import ais.action.ws.model.Response;
import ais.database.model.LogHostToHost;

public class PayWithJenisBayarAction {
	private PembayaranWithJenisBayarAction action = new PembayaranWithJenisBayarAction();

	/**
	 * Sample method
	 */
	public String hello(String name) {
		return "Hello " + name;
	}

	public Response reversal(String nim, String kode) {
		String nama = ("=============================== PAY REVERSAL --> reversal dengan NIM = "
				+ nim + ", kode = " + kode);
		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo0(nim);
		String nominalTagihan = "0";
		return action.reversal(nim, nama, logHostToHost, nominalTagihan, kode);
	}

	public Response inquery(String nim, String kode) {
		String nama = ("=============================== PAY INQUIRY --> inquery dengan NIM = "
				+ nim + ", kode = " + kode);
		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo0(nim);
		return action.inquery(nim, nama, logHostToHost, kode);
	}

	public Response pay(String nim, String reffNumber, String tanggalBayar,
			String jamBayar, String userID, String namaCabang,
			String nominalTagihan, String kode) {
		String nama = ("=============================== PAY PAYMENT --> pay dengan NIM = "
				+ nim
				+ ", reffNumber = "
				+ reffNumber
				+ ", tanggalBayar = "
				+ tanggalBayar
				+ ", userID = "
				+ userID
				+ ", namaCabang = "
				+ namaCabang
				+ ", nominalTagihan = "
				+ nominalTagihan
				+ ", kode = " + kode);
		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo0(nim);
		logHostToHost.setInfo1(reffNumber);
		logHostToHost.setInfo2(tanggalBayar);
		logHostToHost.setInfo3(jamBayar);
		logHostToHost.setInfo4(userID);
		logHostToHost.setInfo5(namaCabang);
		logHostToHost.setInfo6(nominalTagihan);
		logHostToHost.setInfo18(kode);
		return action.pay(nim, reffNumber, tanggalBayar, jamBayar, userID,
				namaCabang, nominalTagihan, nama, logHostToHost, kode);
	}
}
