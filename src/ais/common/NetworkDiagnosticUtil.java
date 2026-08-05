package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utilitas kecil untuk diagnosa jaringan sisi admin — sejauh ini hanya dipakai untuk mengetahui
 * IP PUBLIK/OUTBOUND yang dilihat pihak luar (mis. gateway bank) saat server AIS memanggil mereka.
 *
 * <p><b>Kenapa perlu:</b> beberapa bank (mis. Bankaltimtara) menolak panggilan "Cek Ulang" dengan
 * pesan semacam "IP Partner is not registered" — penolakan ini terjadi di sisi bank karena IP
 * publik server AIS belum/tidak lagi terdaftar di whitelist partner mereka. Ini BUKAN validasi
 * yang bisa di-bypass dari kode AIS (bank menolak sebelum data kita sempat diproses); satu-satunya
 * solusi adalah mendaftarkan ulang IP publik server ke pihak bank. Method di bawah membantu admin
 * menemukan IP tersebut tanpa harus SSH ke server / bertanya ke tim infra.</p>
 */
public class NetworkDiagnosticUtil {

	/** Beberapa layanan "what is my ip" independen — dicoba berurutan agar tidak gagal total bila satu down/diblokir. */
	private static final String[] LAYANAN_CEK_IP = { "https://api.ipify.org", "https://checkip.amazonaws.com",
			"https://ifconfig.me/ip" };

	private static final int TIMEOUT_MS = 6000;

	private NetworkDiagnosticUtil() {
	}

	/**
	 * Ambil IP publik/outbound server saat ini dengan bertanya ke layanan echo-IP eksternal.
	 *
	 * @return IP publik server (mis. "103.xx.xx.xx")
	 * @throws Exception bila SEMUA layanan gagal dihubungi — pesan berisi rincian kegagalan tiap layanan
	 */
	public static String ambilIpPublikOutbound() throws Exception {
		StringBuilder kegagalan = new StringBuilder();
		for (String layanan : LAYANAN_CEK_IP) {
			try {
				String ip = panggilLayananIp(layanan);
				if (ip != null && !ip.trim().isEmpty()) {
					return ip.trim();
				}
			} catch (Exception e) {
				kegagalan.append(layanan).append(": ").append(e.getMessage()).append("; ");
			}
		}
		throw new Exception("Tidak dapat menghubungi layanan pengecek IP manapun (dicoba: "
				+ LAYANAN_CEK_IP.length + " layanan). Kemungkinan server tidak punya akses internet keluar, "
				+ "atau diblokir firewall/proxy. Detail per layanan: " + kegagalan);
	}

	private static String panggilLayananIp(String urlStr) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setConnectTimeout(TIMEOUT_MS);
		con.setReadTimeout(TIMEOUT_MS);
		con.setInstanceFollowRedirects(true);

		int kode = con.getResponseCode();
		if (kode != 200) {
			throw new Exception("HTTP " + kode);
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		try {
			StringBuilder sb = new StringBuilder();
			String baris;
			while ((baris = reader.readLine()) != null) {
				sb.append(baris);
			}
			return sb.toString();
		} finally {
			reader.close();
		}
	}
}
