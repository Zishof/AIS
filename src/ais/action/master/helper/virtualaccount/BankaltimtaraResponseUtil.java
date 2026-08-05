package ais.action.master.helper.virtualaccount;

import org.json.JSONObject;

public class BankaltimtaraResponseUtil {

	private BankaltimtaraResponseUtil() {
	}

	public static JSONObject parseJson(String hasil, String konteks) throws Exception {
		String isi = hasil == null ? "" : hasil.trim();
		if (isi.isEmpty()) {
			throw new Exception("Bank Bankaltimtara tidak mengirim response pada proses " + konteks + ".");
		}
		if (!isi.startsWith("{")) {
			throw new Exception("Bank Bankaltimtara mengirim response non-JSON pada proses " + konteks + ": "
					+ ringkas(isi));
		}
		try {
			return new JSONObject(isi);
		} catch (Exception e) {
			throw new Exception("Response Bank Bankaltimtara belum dapat dibaca pada proses " + konteks + ": "
					+ ringkas(isi), e);
		}
	}

	public static String ambilStringWajib(JSONObject jsonObject, String key, String konteks) throws Exception {
		if (jsonObject == null || jsonObject.isNull(key)) {
			throw new Exception("Response Bank Bankaltimtara pada proses " + konteks
					+ " tidak memuat data '" + key + "'. Response: " + ringkas(jsonObject == null ? "" : jsonObject.toString()));
		}
		return jsonObject.getString(key);
	}

	public static void pastikanSukses(JSONObject jsonObject, String konteks) throws Exception {
		if (jsonObject == null) {
			throw new Exception("Bank Bankaltimtara tidak mengirim response pada proses " + konteks + ".");
		}
		if (!jsonObject.isNull("code") && "00".equals(jsonObject.get("code") + "")) {
			return;
		}
		throw new Exception("Bank Bankaltimtara menolak proses " + konteks + ": " + ambilPesan(jsonObject));
	}

	private static String ambilPesan(JSONObject jsonObject) {
		if (jsonObject == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		tambahInfo(builder, jsonObject, "code");
		tambahInfo(builder, jsonObject, "message");
		tambahInfo(builder, jsonObject, "msg");
		tambahInfo(builder, jsonObject, "error");
		tambahInfo(builder, jsonObject, "description");
		tambahInfo(builder, jsonObject, "desc");
		tambahInfo(builder, jsonObject, "responseMessage");
		if (builder.length() == 0) {
			builder.append(jsonObject.toString());
		}
		return ringkas(builder.toString());
	}

	private static void tambahInfo(StringBuilder builder, JSONObject jsonObject, String key) {
		try {
			if (!jsonObject.isNull(key)) {
				if (builder.length() > 0) {
					builder.append("; ");
				}
				builder.append(key).append(": ").append(jsonObject.get(key));
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/BankaltimtaraResponseUtil.java:tambahInfo");
		}
	}

	public static String ringkas(String nilai) {
		String isi = nilai == null ? "" : nilai.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
		while (isi.indexOf("  ") >= 0) {
			isi = isi.replace("  ", " ");
		}
		if (isi.length() > 1000) {
			return isi.substring(0, 1000) + "...";
		}
		return isi;
	}
}
