package ais.action.servlet.api;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;

public class DateScrollFilter {

	// Class pembantu untuk memudahkan sorting
	static class ItemData {
		String pertemuan;
		Date dateObj;

		public ItemData(String pertemuan, Date dateObj) {
			this.pertemuan = pertemuan;
			this.dateObj = dateObj;
		}
	}

	/**
	 * Method untuk filter tanggal mendekati hari ini dengan fitur scroll up/down.
	 *
	 * @param sourceArray JSONArray sumber data
	 * @param scrollUp    Jumlah data ke masa depan (bawah index terdekat)
	 * @param scrollDown  Jumlah data ke masa lalu (atas index terdekat)
	 * @return JSONArray hasil filter
	 */
	public static JSONArray getFilteredDateData(JSONArray sourceArray, int scrollUp, int scrollDown) {
		System.out.println("sourceArray -> \n" + sourceArray);
		List<ItemData> tempList = new ArrayList<ItemData>();

		// 1. Convert JSONArray ke List agar bisa di-sort
		for (int i = 0; i < sourceArray.length(); i++) {
			try {
				JSONObject obj = sourceArray.getJSONObject(i);
				String tglStr = obj.getString("tanggal");
				Date tgl = Common.dateFormat85.get().parse(tglStr);
				tempList.add(new ItemData(obj.get("id") + "", tgl));
			} catch (JSONException e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/DateScrollFilter.java:49");
			} catch (ParseException e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/DateScrollFilter.java:51");
			}
		}

		// 2. Sort List berdasarkan Tanggal (Ascending: Lama -> Baru)
		Collections.sort(tempList, new Comparator<ItemData>() {
			@Override
			public int compare(ItemData o1, ItemData o2) {
				return o2.dateObj.compareTo(o1.dateObj);
			}
		});

		// 3. Cari Index yang paling mendekati Hari Ini (Today)
		Date now = new Date();
		int closestIndex = -1;
		long minDiff = Long.MAX_VALUE;

		for (int i = 0; i < tempList.size(); i++) {
			Date currentDate = tempList.get(i).dateObj;
			// Menggunakan Math.abs untuk mencari selisih mutlak (baik masa lalu maupun
			// depan)
			long diff = Math.abs(now.getTime() - currentDate.getTime());

			if (diff < minDiff) {
				minDiff = diff;
				closestIndex = i;
			}
		}

		// Jika data kosong, kembalikan array kosong
		if (closestIndex == -1) {
			return new JSONArray();
		}

		// 4. Tentukan batas ambil data (Scroll Up & Down)
		// Scroll Down (Masa Lalu) -> Index makin kecil
		int startIndex = closestIndex - scrollDown;

		// Scroll Up (Masa Depan) -> Index makin besar
		int endIndex = closestIndex + scrollUp;

		// Validasi agar tidak IndexOutOfBounds
		if (startIndex < 0)
			startIndex = 0;
		if (endIndex >= tempList.size())
			endIndex = tempList.size() - 1;

		// 5. Masukkan data terpilih kembali ke JSONArray baru
		JSONArray resultArray = new JSONArray();

		// Kita loop dari start sampai end (inklusif)
		for (int i = startIndex; i <= endIndex; i++) {
			resultArray.put(tempList.get(i).pertemuan);
		}
		tempList.clear();
		tempList = null;
		return resultArray;
	}

	// --- CONTOH PENGGUNAAN (MAIN) ---
	public static void main(String[] args) throws JSONException {
		// Simulasi Data
		JSONArray array = new JSONArray();

		// Kita buat data dummy (Pastikan tanggalnya relevan saat dijalankan)
		// Anggap hari ini Februari 2026
		String[] dates = { "010120", "010124", "010125", // Masa lalu jauh
				"010226", "050226", // Masa lalu dekat
				"200226", // MASA KINI (Misal hari ini tgl 9 Feb 26, ini yg paling dekat nanti)
				"150326", "010426", // Masa depan dekat
				"010130" // Masa depan jauh
		};

		for (int i = 0; i < dates.length; i++) {
			JSONObject jsonObjectData = new JSONObject();
			jsonObjectData.put("id", i + 1);
			jsonObjectData.put("tanggal", dates[i]);
			array.put(jsonObjectData);
		}

		System.out.println("Total Data Awal: " + array.length());

		// PANGGIL FUNGSI
		// Ambil 2 data ke masa lalu, dan 1 data ke masa depan dari titik terdekat hari
		// ini
		int scrollDown = 2; // Masa lalu
		int scrollUp = 1; // Masa depan

		JSONArray hasil = getFilteredDateData(array, scrollUp, scrollDown);

		System.out.println("\n--- Hasil Filter ---");
		for (int i = 0; i < hasil.length(); i++) {
			System.out.println(hasil.get(i).toString());
		}
	}
}