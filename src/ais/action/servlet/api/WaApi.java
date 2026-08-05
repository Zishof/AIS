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

public class WaApi {

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

	public static String[] watzapFormat(String from, String send, String namaFile, String url) throws Exception {
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
			ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/WaApi.java:80 - gagal tulis "
					+ fileOut.getAbsolutePath() + ", fallback kirim data langsung");
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
