package ais.action.iso8583;

import java.util.Random;

/**
 * Kumpulan konstanta protokol ISO 8583 (kode respons dan MTI/Message Type Indicator) yang dipakai
 * paket {@code ais.action.iso8583} untuk komunikasi transaksi finansial antarmesin (mis. EDC/switch
 * pembayaran). Tidak berisi kredensial atau kunci rahasia — hanya kode respons standar (mis.
 * {@link #SUCCESS}) dan nilai MTI untuk pesan manajemen jaringan ({@link #NetManReq}/
 * {@link #NetManRes}) serta pesan inquiry ({@link #InqReq}/{@link #InqRes}). Sebagian besar kode
 * respons standar lain (kartu tidak valid, dana tidak cukup, dsb.) dinonaktifkan sebagai komentar
 * dan tidak dipakai aktif oleh kode saat ini.
 *
 * <p>
 * <b>Catatan:</b> method {@link #main(String[])} pada kelas ini adalah kode uji coba pemilihan port
 * acak yang tidak berkaitan dengan tujuan kelas (konstanta protokol) dan tampak sebagai sisa
 * eksperimen/debug, bukan bagian dari alur produksi.
 * </p>
 */
public class ConstansValueIso8583 {
	/** Kode respons ISO 8583 untuk transaksi sukses. */
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

	/** MTI permintaan manajemen jaringan (network management request). */
	public static final String NetManReq = "2800";
	/** MTI balasan manajemen jaringan (network management response). */
	public static final String NetManRes = "2810";

	/** MTI permintaan inquiry (pengecekan status transaksi). */
	public static final String InqReq = "2100";
	/** MTI balasan inquiry. */
	public static final String InqRes = "2110";

	/**
	 * Kode uji coba/demo yang mencetak 10 pilihan port acak dari daftar tetap; tidak berkaitan
	 * dengan konstanta protokol ISO 8583 di kelas ini dan tampak sebagai sisa eksperimen.
	 *
	 * @param argv argumen baris perintah, tidak dipakai
	 */
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
