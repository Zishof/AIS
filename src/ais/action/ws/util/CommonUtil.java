/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ais.action.ws.util;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import org.apache.axis.MessageContext;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.ws.model.Response;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Perkuliahan;

/**
 * 
 * @author Fauzi
 */
public class CommonUtil {

//	private static Logger logger = Logger.getLogger(CommonUtil.class);

	public static Boolean isNowSemensterGanjil() {
		return Common.isNowSemensterGanjil();
	}

	public static void setRequestAndresponse(LogHostToHost logHostToHost) {
		if (logHostToHost == null) {
			return;
		}

		// CATATAN: MessageContext.getCurrentContext() adalah ThreadLocal milik Axis, hanya
		// terisi saat request datang lewat dispatcher SOAP Axis. Dipanggil dari jalur non-SOAP
		// (mis. servlet Va -> PembayaranAction -> DisplayUtil, bukan lewat Axis) ia akan
		// bernilai null -> NullPointerException bila langsung dirantai .getResponseMessage()/
		// .getRequestMessage(). Guard eksplisit di sini (bukan cuma andalkan catch di bawah)
		// agar kondisi normal-tanpa-konteks-Axis ini tidak tercatat sebagai error tiap kali.
		MessageContext context = null;
		try {
			context = MessageContext.getCurrentContext();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/CommonUtil.java:38");
		}

		if (context == null) {
			return;
		}

		try {
			String response = context.getResponseMessage() == null ? null
					: context.getResponseMessage().getContentDescription();
			logHostToHost.setResponse(response);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/CommonUtil.java:38");
//			logger.error("Error", e);
//			Common.tampilErrorJikaAdmin(e);
		}

		try {
			String request = context.getRequestMessage() == null ? null
					: context.getRequestMessage().getContentDescription();
			logHostToHost.setRequest(request);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/CommonUtil.java:46");
//			logger.error("Error", e);
			// Common.tampilErrorJikaAdmin(e);
		}
	}

	public static Response convertToResponse(List<String[]> data) {
		return convertToResponse(data.toArray(new String[][] {}));
	}

	public static Response convertToResponse(String[][] data) {

		Response response = new Response();
		for (String[] strings : data) {
			try {
				String key = strings[0];
				String value = strings[1];
				if (key.equalsIgnoreCase("response_code")) {
					response.setResponse_code(value);
				}
				if (key.equalsIgnoreCase("response_description")) {
					response.setResponse_description(value);
				}
				if (key.equalsIgnoreCase("nim") || key.equalsIgnoreCase("no_registrasi")) {
					response.setNim(value);
				}
				if (key.equalsIgnoreCase("kurs")) {
					response.setKurs(value);
				}
				if (key.equalsIgnoreCase("nama")) {
					response.setNama(value);
				}
				if (key.equalsIgnoreCase("program")) {
					response.setProgram(value);
				}
				if (key.equalsIgnoreCase("fakultas")) {
					response.setFakultas(value);
				}
				if (key.equalsIgnoreCase("prodi")) {
					response.setProdi(value);
				}
				if (key.equalsIgnoreCase("angkatan")) {
					response.setAngkatan(value);
				}
				if (key.equalsIgnoreCase("semester")) {
					response.setSemester(value);
				}
				if (key.equalsIgnoreCase("semester_ke")) {
					response.setSemester_ke(value);
				}
				if (key.equalsIgnoreCase("amount")) {
					response.setAmount(value);
				}
				if (key.equalsIgnoreCase("total_amount")) {
					response.setTotal_amount(value);
				}

				if (key.equalsIgnoreCase("kode_status_pembayaran")) {
					response.setKode_status_pembayaran(value);
				}
				if (key.equalsIgnoreCase("keterangan_status_pembayaran")) {
					response.setKeterangan_status_pembayaran(value);
				}
				if (key.equalsIgnoreCase("reference_number")) {
					response.setReference_number(value);
				}
				if (key.equalsIgnoreCase("jumlah_yang_telah_dibayar")) {
					response.setInfo1(value);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/CommonUtil.java:115");
				// TODO: handle exception
			}

		}

		return checkIfCicilan(response);

	}

	public static Response checkIfCicilan(Response response) {
		try {

			if (response != null && ConstantUtil.SUCCESS.equals(response.getResponse_code())) {

				boolean perbulan = Common.bolehKonfigurasi("aktifkan_biaya_host_to_host_per_bulan", Konfigurasi.TIDAK_AKTIF);

				if (!perbulan) {
					String totalText = response.getTotal_amount();
					String dibayarText = response.getInfo1();
					if (totalText != null && dibayarText != null && !totalText.trim().isEmpty()
							&& !dibayarText.trim().isEmpty()) {
						Double totalTagihan = Double.parseDouble(totalText.trim());
						Double jumlahDibayar = Double.parseDouble(dibayarText.trim());
						Double sisaTagihan = hitungSisaTagihan(totalTagihan, jumlahDibayar);

						response.setTotal_amount(sisaTagihan.toString());
						response.setAmount(sesuaikanRincianDenganSisa(response.getAmount(), sisaTagihan));
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return response;
	}

	/**
	 * Menghitung sisa yang benar-benar masih boleh dibayar oleh host bank.
	 * Nilai negatif akibat pembulatan/kelebihan bayar selalu dinormalkan menjadi nol.
	 */
	public static Double hitungSisaTagihan(Double totalTagihan, Double jumlahDibayar) {
		double total = totalTagihan == null ? 0.0 : totalTagihan.doubleValue();
		double dibayar = jumlahDibayar == null ? 0.0 : jumlahDibayar.doubleValue();
		double sisa = total - dibayar;
		return Double.valueOf(sisa > 0.0 ? sisa : 0.0);
	}

	/**
	 * Menyamakan jumlah rincian dengan total sisa. Pembayaran lama dialokasikan
	 * berurutan terhadap rincian tagihan sehingga nama item dan nominal yang masih
	 * harus dibayar tetap konsisten dengan total inquiry.
	 */
	public static String sesuaikanRincianDenganSisa(String amount, Double sisaTagihan) {
		if (amount == null || amount.trim().isEmpty() || sisaTagihan == null) {
			return amount;
		}
		String[] baris = amount.split("\\|", -1);
		double totalRincian = 0.0;
		for (int i = 0; i < baris.length; i++) {
			if (baris[i] == null || baris[i].trim().isEmpty()) {
				continue;
			}
			String[] bagian = baris[i].split("\\\\", -1);
			if (bagian.length >= 3) {
				try {
					totalRincian += Double.parseDouble(bagian[bagian.length - 1].trim());
				} catch (Exception e) {
					// Rincian tidak valid diperlakukan nol, sama seperti parser lama.
				}
			}
		}
		double pembayaranTeralokasi = Math.max(0.0,
				totalRincian - Math.max(0.0, sisaTagihan.doubleValue()));
		StringBuilder hasil = new StringBuilder("|");
		for (int i = 0; i < baris.length; i++) {
			if (baris[i] == null || baris[i].trim().isEmpty()) {
				continue;
			}
			String[] bagian = baris[i].split("\\\\", -1);
			if (bagian.length < 3) {
				continue;
			}
			double nominal = 0.0;
			try {
				nominal = Double.parseDouble(bagian[bagian.length - 1].trim());
			} catch (Exception e) {
				nominal = 0.0;
			}
			double pembayaranItem = Math.min(Math.max(0.0, nominal), pembayaranTeralokasi);
			double nominalSisa = Math.max(0.0, nominal - pembayaranItem);
			pembayaranTeralokasi -= pembayaranItem;
			for (int j = 0; j < bagian.length - 1; j++) {
				if (j > 0) {
					hasil.append('\\');
				}
				hasil.append(bagian[j] == null ? "" : bagian[j]);
			}
			hasil.append('\\').append(Double.valueOf(nominalSisa).longValue()).append('|');
		}
		return hasil.toString();
	}

	public static Integer getSemester(Integer angkatan, Boolean isGanjil, Integer mulaiSemester,
			String masukDiSemester) {
		return Common.getSemester(angkatan, isGanjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP, mulaiSemester,
				masukDiSemester);
	}

	public static String convertToString(List<String[]> data) {
		String ss = "";
		for (String[] s : data) {
			ss += s[0] + "=" + s[1] + "<br>";
		}
		return ss;
	}

	public static String generateTahunAkademik() {
		Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		if (isNowSemensterGanjil()) {
			return (tahun) + "/" + (tahun + 1);
		} else {
			return (tahun - 1) + "/" + (tahun);
		}
	}

	public static void simpanTemporary(String key, List<String> filePaths) {
		String fileLocation = ConstantValues.lokasiFileTemproraryTemp + key + ".json";
		File file = new File(fileLocation);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		JSONArray jsonArray = new JSONArray();
		String isiTemporary = ais.common.BacaTulisUtil.baca(file);
		// Belum ada data temporary tersimpan (penyimpanan pertama kali utk key ini):
		// kondisi normal, bukan error. Jangan coba parse string kosong/bukan-array
		// sebagai JSONArray (akan throw JSONException "must start with '['") -
		// langsung mulai dari JSONArray kosong, sama seperti pola di ambilTemporary().
		if (isiTemporary != null && !isiTemporary.trim().isEmpty() && isiTemporary.trim().startsWith("[")) {
			try {
				jsonArray = new JSONArray(isiTemporary);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/CommonUtil.java:198");
			}
		}

		for (String filePath : filePaths) {
			jsonArray.put(filePath);
		}
		try {
			ais.common.BacaTulisUtil.tulis(file, jsonArray.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/CommonUtil.java:206");
		}
		// System.out.println("Simpan temporary " + fileLocation + ", " +
		// filePaths);
		jsonArray = null;
	}

	public static JSONArray ambilTemporary(String key) {
		String fileLocation = ConstantValues.lokasiFileTemproraryTemp + key + ".json";
		File file = new File(fileLocation);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		JSONArray a = new JSONArray();

		JSONArray jsonArray = new JSONArray();
		try {
			String isiTemporary = ais.common.BacaTulisUtil.baca(file);
			// Belum ada data temporary tersimpan (sinkron pertama kali / data
			// sudah dibersihkan): kondisi normal, bukan error. Jangan coba parse
			// string kosong/blank sebagai JSONArray (akan throw ParseException
			// "must start with '['") - langsung kembalikan JSONArray kosong.
			if (isiTemporary != null && !isiTemporary.trim().isEmpty()) {
				a = new JSONArray(isiTemporary);
				for (int i = 0; i < a.length(); i++) {
					String da = a.getString(i);
					if (Common.isNumber(da)) {
						jsonArray.put(da);
					} else {
						File fileData = new File(da);
						if (fileData.exists()) {
							jsonArray.put(new JSONObject(ais.common.BacaTulisUtil.baca(fileData)));
						} else {
							jsonArray.put(da);
						}
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/CommonUtil.java:237");
			// e.printStackTrace();
		}
		// System.out.println("Ambil temporary " + fileLocation + ", " + a);
		return jsonArray;
	}

	public static boolean ada(String key) {
		String fileLocation = ConstantValues.lokasiFileTemproraryTemp + key + ".json";

		try {
			File file = new File(fileLocation);
			if (file.exists()) {
				return !ais.common.BacaTulisUtil.baca(file).trim().isEmpty();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/CommonUtil.java:252");
			// TODO: handle exception
		}

		return false;

	}

	public static void reset(String key) {
		try {
			String fileLocation = ConstantValues.lokasiFileTemproraryTemp + key + ".json";
			File file = new File(fileLocation);
			if (file != null && file.getParentFile().exists()) {
				ais.common.BacaTulisUtil.hapus(file);
				file.delete();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/util/CommonUtil.java:268");
			// TODO: handle exception
		}

	}
}
