package ais.action.ws;

import ais.action.ws.model.Response;
import ais.database.model.LogHostToHost;

/**
 * Controller/action ZK untuk pay bri. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code action}; operasi lokal: {@code hello()},
 * {@code reversal()}, {@code inquery()}, {@code pay}(). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 */
public class PayBRIAction {
	private PembayaranAction action = new PembayaranAction();

	/**
	 * Sample method
	 */
	public String hello(String name) {
		return "Hello " + name;
	}

	public Response reversal(String nim) {
		String nama = ("=============================== BRI REVERSAL --> reversal dengan NIM = " + nim);
		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo0(nim);
		String nominalTagihan = "0";
		return action.reversal(nim, nama, logHostToHost, nominalTagihan);
	}

	public Response inquery(String nim) {
		String nama = ("=============================== BRI INQUIRY --> inquery dengan NIM = " + nim);
		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo0(nim);
		return action.inquery(nim, nama, logHostToHost);
	}

	public Response pay(String nim, String reffNumber, String tanggalBayar,
			String jamBayar, String userID, String namaCabang,
			String nominalTagihan) {
		String nama = ("=============================== BRI PAYMENT --> pay dengan NIM = "
				+ nim
				+ ", reffNumber = "
				+ reffNumber
				+ ", tanggalBayar = "
				+ tanggalBayar
				+ ", userID = "
				+ userID
				+ ", namaCabang = "
				+ namaCabang + ", nominalTagihan = " + nominalTagihan);
		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo0(nim);
		logHostToHost.setInfo1(reffNumber);
		logHostToHost.setInfo2(tanggalBayar);
		logHostToHost.setInfo3(jamBayar);
		logHostToHost.setInfo4(userID);
		logHostToHost.setInfo5(namaCabang);
		logHostToHost.setInfo6(nominalTagihan);
		return action.pay(nim, reffNumber, tanggalBayar, jamBayar, userID,
				namaCabang, nominalTagihan, nama, logHostToHost);
	}
}
