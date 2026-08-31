package ais.ui.util;

import java.text.DecimalFormat;

import org.json.JSONObject;

/**
 * Berkas uji coba/scratch manual (bukan bagian dari alur aplikasi, tidak dipanggil kelas lain)
 * untuk mengecek perilaku format angka: bagaimana {@code MyJSONObject} (varian
 * {@link org.json.JSONObject} milik AIS) merender angka besar/desimal dalam {@code toString()},
 * dibandingkan dengan {@link java.text.DecimalFormat} memakai pola tanpa notasi ilmiah
 * ({@code "###########################"}, maksimum 6 digit desimal). Sebagian besar isi method
 * {@code main} berupa kode yang dikomentari — sisa eksperimen lama (parsing tanggal minggu,
 * decode URL, konstanta scope Google Drive/Calendar) yang ditinggalkan sebagai catatan, bukan
 * kode aktif.
 */
public class Test {

	/**
	 * Menjalankan eksperimen format angka: membandingkan hasil {@code DecimalFormat} dan
	 * {@code MyJSONObject.toString()} untuk tiga nilai (angka besar, {@code 0.4}, {@code 3.74})
	 * dan mencetak hasilnya ke konsol. Tidak ada nilai kembali yang dikonsumsi program lain.
	 *
	 * @param argv argumen baris perintah, tidak dipakai
	 * @throws Exception diteruskan apa adanya dari operasi JSON/format di dalamnya
	 */
	public static void main(String[] argv) throws Exception {

		JSONObject name = new JSONObject();
		name.put("test", 22000000000000.0);
		
		name.put("test1", 3.74);
		name.put("test1", 0.4);
		JSONObject name1 = new JSONObject();
		
		
		
		name1.put("tets lagi", name);

		MyJSONObject MyJSONObject = new MyJSONObject(name1);

		String pattern = "###########################";
		DecimalFormat decimalFormat = new DecimalFormat(pattern);
		decimalFormat.setMaximumFractionDigits(6);
		decimalFormat.setMinimumIntegerDigits(1);

		String sss = decimalFormat.format(22000000000000.0);
		System.out.println("sss -> " + sss + " " + MyJSONObject.toString());

		sss = decimalFormat.format(0.4);
		System.out.println("sss -> " + sss + " " + MyJSONObject.toString());
		
		
		 sss = decimalFormat.format(3.74);
		System.out.println("sss -> " + sss + " " + MyJSONObject.toString());
		
//		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
//		calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) - 1);
//		DateFormat dateFormat = new SimpleDateFormat("YYYY-ww", new Locale("in", "ID"));
//		System.out.println(dateFormat.format(calendar.getTime()));
//		System.out.println("10.00".replaceAll("\\.", ":"));
//		System.out.println(DriveScopes.DRIVE_FILE);
//		System.out.println(CalendarScopes.CALENDAR);
//
//		String d = URLDecoder.decode("%3D%3D", "UTF-8");
//		System.out.println("d -> " + d);
	}
}
