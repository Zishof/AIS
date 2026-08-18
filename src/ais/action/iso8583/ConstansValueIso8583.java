package ais.action.iso8583;

import java.util.Random;

public class ConstansValueIso8583 {
	public static final String SUCCESS = "0000";
	// public static final String KARTU_TIDAK_VALID = "01";
	// public static final String KARTU_BELUM_TERDAFTAR = "03";
	// public static final String EDC_BELUM_TERDAFTAR = "04";
	// public static final String TARIF_TIDAK_DITEMUKAN = "05";
	// public static final String GAGAL_MEMPROSES_KARTU = "06";
	// public static final String DONOT_HONOUR = "05";
	// public static final String INVALID_TRANSACTION = "12";
	// public static final String INVALID_AMOUNT = "13";
	// public static final String NO_SUCH_ISSUER = "15";
	// public static final String FORMAT_ERROR = "30";
	// public static final String REQUEST_NOT_SUPPORTED = "40";
	// public static final String INSUFFICIENT_FUND = "51";
	// public static final String SECURITY_VIOLATION = "02";
	// public static final String RESPONSE_TOO_LATE = "68";
	// public static final String LINK_TO_HOST_DOWN = "89";
	// public static final String SWICTH_IS_INOPERATIVE = "91";
	// public static final String UNNABLE_TO_ROUTE_TRANSACTION = "92";
	// public static final String DUPLICATE_TRANSACTION = "94";
	// public static final String SYSTEM_ERROR = "96";
	//

	public static final String NetManReq = "2800";
	public static final String NetManRes = "2810";

	public static final String InqReq = "2100";
	public static final String InqRes = "2110";

	public static void main(String[] argv) {
		String[] ports = new String[] { "5050", "6060", "7070", "8080" };
		Random rn = new Random();
		int max = 3;
		int min = 0;
		for (int i = 0; i < 10; i++) {
			int rand = rn.nextInt(max - min + 1) + min;
			System.out.println("rand "+rand);
			System.out.println("Port "+ports[rand]);
		}
	}
}
