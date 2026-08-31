package ais.action.servlet.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import ais.action.servlet.Wa;
import ais.common.Common;
import ais.database.model.Konfigurasi;

/**
 * Kelas utilitas untuk menyusun perintah {@code curl} pengiriman pesan WhatsApp lewat dua
 * penyedia pihak ketiga: Ultramsg ({@link #ultramsgFormat}) dan Watzap ({@link #watzapFormat}).
 * Kedua method hanya <b>menyusun</b> array argumen perintah {@code curl} (dijalankan lewat
 * {@link ProcessBuilder} oleh pemanggil, mis. {@link ais.action.servlet.Wa}) — argumen disusun
 * sebagai elemen array terpisah (bukan satu string shell) agar aman dari celah shell-injection.
 *
 * <p>
 * <b>Peringatan keamanan (dilaporkan, TIDAK diperbaiki sesuai instruksi tugas):</b> beberapa
 * token/kunci API pihak ketiga tertanam langsung di kode sebagai nilai default fallback pada
 * pemanggilan {@code Common.getKonfigurasi(key, default)} — nilai default ini dipakai bila
 * konfigurasi database belum diisi, sehingga secara efektif menjadi kredensial cadangan yang
 * ikut ter-commit ke source control. Ditemukan di:
 * </p>
 * <ul>
 * <li>{@code instance_id} Ultramsg — default {@code "instance101739"} (kunci konfigurasi
 * {@code token_instance_id_baru}, lihat {@link #ultramsgFormat})</li>
 * <li>{@code token} Ultramsg — default {@code "dd9gcfbnp928paj0"} (kunci konfigurasi
 * {@code token_ultramsg_baru}, lihat {@link #ultramsgFormat})</li>
 * <li>{@code api_key} Watzap — default {@code "YBIYGXHPIVEVHT3G"} (kunci konfigurasi
 * {@code watzap_api_key}, lihat {@link #watzapFormat})</li>
 * <li>{@code number_key} Watzap — default {@code "u3w09ScxqJsNIrpG"} (kunci konfigurasi
 * {@code watzap_number_key}, lihat {@link #watzapFormat})</li>
 * <li>daftar {@code number_key} rotasi Watzap — default
 * {@code "u3w09ScxqJsNIrpG;ESaI8uxCG6hHdJro;1zUEMU5zLp2UlJis;5ur22YeVFmUkCpCX"} (kunci
 * konfigurasi {@code watzap_number_key_random}, lihat {@link #watzapFormat})</li>
 * </ul>
 * <p>
 * Bila token-token ini masih aktif/valid di sisi penyedia, keberadaannya dalam riwayat kode
 * (termasuk riwayat SVN) merupakan kebocoran kredensial yang perlu ditinjau dan dirotasi oleh
 * pemilik integrasi; tidak diubah di sini sesuai batasan tugas dokumentasi.
 * </p>
 */
public class WaApi {

	/**
	 * Menyusun perintah {@code curl} untuk mengirim pesan (atau dokumen, bila {@code namaFile}
	 * dan {@code url} diisi) lewat API Ultramsg. Kredensial instance/token diambil dari
	 * konfigurasi {@code token_instance_id_baru}/{@code token_ultramsg_baru} — lihat peringatan
	 * keamanan pada javadoc kelas terkait nilai default fallback yang tertanam di kode.
	 *
	 * @param from     nomor tujuan WhatsApp
	 * @param send     isi pesan (atau caption, bila mengirim dokumen)
	 * @param namaFile nama file lampiran, boleh {@code null} bila tanpa lampiran
	 * @param url      URL dokumen yang akan dilampirkan, boleh {@code null} bila tanpa lampiran
	 * @return array argumen perintah {@code curl} siap dijalankan lewat {@link ProcessBuilder}
	 */
	public static String[] ultramsgFormat(String from, String send, String namaFile, String url) throws Exception {
		String instance_id = Common.getKonfigurasi("token_instance_id_baru", "instance101739").getNilai().trim();

		String token = Common.getKonfigurasi("token_ultramsg_baru", "dd9gcfbnp928paj0").getNilai().trim();
		String linkPost = "https://api.ultramsg.com/" + instance_id + "/messages/chat";
		String[] command = { "curl", "--request", "POST", linkPost, "--header",
				"content-type: application/x-www-form-urlencoded", "--data-urlencode", "token=" + token,
				"--data-urlencode", "to=" + from, "--data-urlencode", "body=" + send };

		if (namaFile != null && url != null) {
			linkPost = "https://api.ultramsg.com/" + instance_id + "/messages/document";
			command = new String[] { "curl", "--request", "POST", linkPost, "--header",
					"content-type: application/x-www-form-urlencoded", "--data-urlencode", "token=" + token,
					"--data-urlencode", "to=" + from, "--data-urlencode", "filename=" + namaFile, "--data-urlencode",
					"document=" + url, "--data-urlencode", "caption=" + send };
		}

		return command;
	}

	/**
	 * Menyusun perintah {@code curl} untuk mengirim pesan lewat API Watzap. Kunci nomor
	 * pengirim ({@code number_key}) dipilih secara round-robin dari daftar
	 * {@code watzap_number_key_random} (indeks berjalan di {@link
	 * ais.action.servlet.Wa#indexPengiriman}) bila {@code from} belum punya kunci tetap di
	 * {@link ais.action.servlet.Wa#nomorKey}, agar beban pengiriman tersebar ke beberapa nomor
	 * pengirim. Payload JSON ditulis ke berkas sementara {@code /opt/tanya/send_<nomor>.txt} dan
	 * dikirim lewat {@code --data @berkas} (menghindari batas panjang argumen shell untuk pesan
	 * panjang); bila penulisan berkas gagal (mis. direktori tidak dapat dibuat), method jatuh
	 * kembali mengirim payload langsung sebagai argumen {@code curl} (tetap aman dari
	 * shell-injection karena tetap berupa elemen array terpisah). Bila {@code namaFile} dan
	 * {@code url} diisi serta konfigurasi {@code kirim_file_via_watzap} aktif, method ini juga
	 * langsung memicu pengiriman dokumen terpisah lewat endpoint {@code send_file_url} (efek
	 * samping tambahan, di luar perintah {@code curl} yang dikembalikan). Lihat peringatan
	 * keamanan pada javadoc kelas terkait kredensial default fallback yang tertanam di kode.
	 *
	 * @param from     nomor tujuan WhatsApp/Watzap, tidak boleh kosong
	 * @param send     isi pesan; {@code null} diperlakukan sebagai string kosong
	 * @param namaFile nama file lampiran, boleh {@code null} bila tanpa lampiran
	 * @param url      URL dokumen yang akan dilampirkan, boleh {@code null} bila tanpa lampiran
	 * @return array argumen perintah {@code curl} untuk pengiriman pesan teks, siap dijalankan
	 *         lewat {@link ProcessBuilder}
	 * @throws IllegalArgumentException bila {@code from} kosong/{@code null}
	 */
	public static String[] watzapFormat(String from, String send, String namaFile, String url) throws Exception {
		if (from == null || from.trim().length() == 0) {
			throw new IllegalArgumentException("Nomor tujuan WhatsApp/Watzap kosong");
		}
		if (send == null) {
			send = "";
		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("api_key", Common.getKonfigurasi("watzap_api_key", "YBIYGXHPIVEVHT3G").getNilai().trim());

		String number_key = Common.getKonfigurasi("watzap_number_key", "u3w09ScxqJsNIrpG").getNilai().trim();

		try {

			if (Wa.nomorKey.containsKey(from.trim())) {
				number_key = Wa.nomorKey.get(from.trim());
			} else {
				String[] ss = Common
						.getKonfigurasi("watzap_number_key_random",
								"u3w09ScxqJsNIrpG;ESaI8uxCG6hHdJro;1zUEMU5zLp2UlJis;5ur22YeVFmUkCpCX")
						.getNilai().trim().split(";");

				number_key = ss[Wa.indexPengiriman % ss.length];
				Wa.indexPengiriman++;
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/WaApi.java:57");
			// TODO: handle exception
		}

		jsonObject.put("number_key", number_key);
		jsonObject.put("phone_no", from.trim());

		jsonObject.put("message", send);

		jsonObject.put("wait_until_send", "1");

		String linkPost = "https://api.watzap.id/v1/send_message";

		String data = jsonObject.toString();
		data = StringUtils.replace(data, "\\\\n", "\n");
		data = data.replaceAll("\n", "\\\\n");
		System.out.println("data -> " + data);

		File fileOut = new File("/opt/tanya/send_" + from + ".txt");
		String[] command;
		try {
			if (!fileOut.getParentFile().exists()) {
				fileOut.getParentFile().mkdirs();
			}

			FileUtils.writeStringToFile(fileOut, data);

			command = new String[] { "curl", "--request", "POST", linkPost, "--header",
					"Content-Type: application/json", "--data", "@" + fileOut.getAbsolutePath() };
		} catch (Exception e) {
			// Gagal buat/tulis file perantara (mis. direktori /opt/tanya tak ada & tak bisa
			// dibuat karena izin, dsb). Jangan biarkan IOException ini menggagalkan seluruh
			// pengiriman WA -> fallback kirim payload langsung sbg argumen curl (tanpa file),
			// via ProcessBuilder array jadi tetap aman dari shell-quoting.
			System.err.println("[WaApi] Tidak bisa menulis file payload sementara " + fileOut.getAbsolutePath()
					+ ", kirim langsung lewat argumen curl: " + e.getMessage());
			command = new String[] { "curl", "--request", "POST", linkPost, "--header",
					"Content-Type: application/json", "--data", data };
		}

		try {
			if (namaFile != null && url != null
					&& Common.bolehKonfigurasi("kirim_file_via_watzap", Konfigurasi.TIDAK_AKTIF)) {
				String linkPostFile = "https://api.watzap.id/v1/send_file_url";
				jsonObject.put("url", url);
				jsonObject.remove("message");
				String[] commandFile = new String[] { "curl", "--request", "POST", linkPostFile, "--header",
						"Content-Type: application/json", "--data", jsonObject.toString() };
				ProcessBuilder process = new ProcessBuilder(commandFile);
				Process p;
				p = process.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append(System.getProperty("line.separator"));
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/WaApi.java:104");
			// TODO: handle exception
		}

		return command;
	}
}
