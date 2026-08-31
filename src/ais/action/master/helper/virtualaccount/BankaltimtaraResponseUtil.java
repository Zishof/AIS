package ais.action.master.helper.virtualaccount;

import org.json.JSONObject;

/**
 * Utilitas statis (tanpa state, tanpa kredensial/URL/API key tertanam — murni fungsi pengolah
 * teks/JSON) untuk mem-parsing dan menerjemahkan response API virtual account Bank Bankaltimtara
 * menjadi pesan galat berbahasa Indonesia yang informatif. Dipakai oleh kelas integrasi
 * pembayaran virtual account Bankaltimtara lain di paket {@code
 * ais.action.master.helper.virtualaccount} untuk menyeragamkan penanganan response yang gagal
 * atau tidak terduga (response kosong, bukan JSON, JSON tak valid, field wajib hilang, atau
 * kode status non-sukses).
 *
 * <p>
 * Konvensi kode sukses yang dipakai {@link #pastikanSukses} adalah field {@code "code"} bernilai
 * {@code "00"} — spesifik untuk API Bankaltimtara ini, bukan konvensi umum. Pesan galat yang
 * disusun {@link #ambilPesan} mencoba beberapa nama field umum yang mungkin dipakai API bank
 * (code/message/msg/error/description/desc/responseMessage) dan menggabungkan yang ditemukan;
 * bila tidak ada satupun yang cocok, seluruh isi JSON mentah dipakai sebagai fallback. Semua
 * pesan dan potongan response mentah yang disisipkan ke pesan galat dipangkas/dirapikan lewat
 * {@link #ringkas(String)} (baris baru diratakan jadi spasi, spasi ganda dirapatkan, dipotong
 * maksimal 1000 karakter) agar log/pesan error tetap ringkas dan aman ditampilkan.
 * </p>
 */
public class BankaltimtaraResponseUtil {

	private BankaltimtaraResponseUtil() {
	}

	/**
	 * Mem-parsing {@code hasil} (body response mentah) menjadi {@link JSONObject}.
	 *
	 * @param hasil   body response mentah dari API Bankaltimtara
	 * @param konteks label proses saat ini (mis. "membuat virtual account"), disisipkan ke pesan galat agar mudah ditelusuri
	 * @return objek JSON hasil parsing
	 * @throws Exception bila response kosong, bukan berformat JSON (tidak diawali {@code '{'}), atau gagal diparsing sebagai JSON
	 */
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

	/**
	 * Mengambil satu field String wajib dari {@code jsonObject}.
	 *
	 * @return nilai field {@code key}
	 * @throws Exception bila {@code jsonObject} null atau field {@code key} tidak ada/bernilai null
	 */
	public static String ambilStringWajib(JSONObject jsonObject, String key, String konteks) throws Exception {
		if (jsonObject == null || jsonObject.isNull(key)) {
			throw new Exception("Response Bank Bankaltimtara pada proses " + konteks
					+ " tidak memuat data '" + key + "'. Response: " + ringkas(jsonObject == null ? "" : jsonObject.toString()));
		}
		return jsonObject.getString(key);
	}

	/**
	 * Memastikan response Bankaltimtara menandakan sukses (field {@code "code"} bernilai
	 * {@code "00"}); bila tidak, melempar exception berisi pesan galat yang disusun {@link
	 * #ambilPesan}.
	 *
	 * @throws Exception bila {@code jsonObject} null atau kode responsnya bukan {@code "00"}
	 */
	public static void pastikanSukses(JSONObject jsonObject, String konteks) throws Exception {
		if (jsonObject == null) {
			throw new Exception("Bank Bankaltimtara tidak mengirim response pada proses " + konteks + ".");
		}
		if (!jsonObject.isNull("code") && "00".equals(jsonObject.get("code") + "")) {
			return;
		}
		throw new Exception("Bank Bankaltimtara menolak proses " + konteks + ": " + ambilPesan(jsonObject));
	}

	/** Menyusun pesan galat ringkas dari field-field umum penanda error/kode pada response (lihat javadoc kelas), atau JSON mentah bila tidak ada satupun field yang cocok. */
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

	/** Menambahkan {@code "key: nilai"} ke {@code builder} (dipisah {@code "; "} dari entri sebelumnya) bila field {@code key} ada pada {@code jsonObject}; diam-diam diabaikan bila gagal dibaca. */
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

	/**
	 * Merapikan teks untuk disisipkan ke pesan galat/log: baris baru dan tab diratakan jadi
	 * spasi, spasi berurutan dirapatkan jadi satu, dan hasil dipotong maksimal 1000 karakter
	 * (diberi akhiran {@code "..."} bila terpotong).
	 *
	 * @param nilai teks mentah; {@code null} diperlakukan sebagai string kosong
	 * @return teks yang sudah dirapikan
	 */
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
