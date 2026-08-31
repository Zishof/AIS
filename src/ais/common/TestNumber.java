package ais.common;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.lang.StringUtils;

/**
 * Kelas percobaan (scratch class) yang dipakai sebagai tempat menguji coba potongan logika format
 * angka secara mandiri, di luar alur aplikasi AIS yang sesungguhnya. Kelas ini TIDAK dipanggil dari
 * bagian lain aplikasi kecuali lewat method {@link #main(String[])}-nya sendiri; tidak ada referensi
 * masuk dari modul akademik, keuangan, ataupun modul lain manapun di codebase. Fungsinya murni
 * sebagai "kertas coretan" bagi pengembang untuk memverifikasi perilaku pembulatan angka
 * ({@link #withMathRound(double, int)}) dan perilaku {@link DecimalFormat} berbasis locale
 * ({@link #df}) sebelum pola yang sama dipakai di kode produksi lain (mis. tampilan nilai rupiah
 * atau nilai akademik pada layar ZK).
 *
 * <p>
 * Ada dua bagian yang tidak berkaitan langsung dalam kelas ini:
 * </p>
 * <ol>
 * <li><b>Pembulatan angka</b> — {@link #withMathRound(double, int)} adalah utilitas pembulatan
 * generik berbasis {@link Math#round(double)} dengan jumlah desimal (<i>places</i>) yang dapat
 * diatur, memakai teknik kalikan-bulatkan-bagi dengan faktor skala {@code 10^places}. Method ini
 * sebetulnya dipanggil (dalam bentuk komentar, dinonaktifkan) di {@link #main(String[])} untuk
 * membandingkan hasil pembulatan manual dengan hasil format {@link DecimalFormat} bawaan locale.</li>
 * <li><b>Parsing string berformat khusus</b> — bagian kedua {@link #main(String[])} menguji coba
 * pemisahan string berisi nama kriteria penilaian dan daftar pilihan nilai huruf yang dipisahkan
 * tanda panah ({@code "->"}), pola yang lazim dipakai pada konfigurasi rubrik penilaian di modul
 * akademik (mis. {@code "1. Do'a Sebelum Belajar->A;A-;B+;B;C+;C;D"}).</li>
 * </ol>
 *
 * <p>
 * Karena sifatnya sebagai kelas percobaan, tidak ada kontrak API yang perlu dijaga stabil di sini;
 * perubahan pada {@link #main(String[])} tidak berdampak pada modul lain manapun.
 * </p>
 */
public class TestNumber {

	/**
	 * Membulatkan {@code value} ke sejumlah {@code places} angka di belakang koma memakai teknik
	 * kalikan dengan faktor skala {@code 10^places}, bulatkan ke integer terdekat lewat
	 * {@link Math#round(double)}, lalu bagi kembali dengan faktor skala yang sama.
	 *
	 * @param value  nilai yang akan dibulatkan
	 * @param places jumlah digit desimal yang dipertahankan setelah pembulatan
	 * @return nilai {@code value} yang sudah dibulatkan ke {@code places} desimal
	 */
	public static double withMathRound(double value, int places) {
		double scale = Math.pow(10, places);
		return Math.round(value * scale) / scale;
	}

	/**
	 * {@link DecimalFormat} ber-<i>thread-local</i> yang diinisialisasi dari locale aplikasi
	 * ({@link Common#locale}) lewat {@link NumberFormat#getNumberInstance(java.util.Locale)}.
	 * Dipakai di {@link #main(String[])} untuk menguji bagaimana angka desimal diformat/diparse
	 * sesuai locale (mis. pemisah ribuan berupa titik dan pemisah desimal berupa koma pada locale
	 * Indonesia) sebelum nilai tersebut dibersihkan kembali menjadi format numerik polos.
	 */
	public static final ThreadLocal<DecimalFormat> df = new ThreadLocal<DecimalFormat>() {
		@Override
		protected DecimalFormat initialValue() {
			return (DecimalFormat) NumberFormat.getNumberInstance(Common.locale);
		}
	};

	/**
	 * Titik masuk uji coba manual. Menjalankan dua percobaan independen: (1) memformat nilai
	 * {@code 0.5} lewat {@link #df} lalu membersihkan pemisah ribuan/desimal ala locale Indonesia
	 * menjadi format numerik polos (titik sebagai desimal), dan (2) memecah contoh string konfigurasi
	 * rubrik penilaian berbasis tanda panah ({@code "->"}) menjadi bagian nama kriteria dan bagian
	 * daftar pilihan nilai huruf. Seluruh hasil percobaan dicetak ke {@link System#out}; method ini
	 * tidak mengembalikan nilai maupun mengubah state aplikasi.
	 *
	 * @param args argumen baris perintah, tidak dipakai
	 */
	public static void main(String[] args) {

		double number = 0.5;
//		number = withMathRound(number, 2);
		String string = df.get().format(number);
		string = org.apache.commons.lang3.StringUtils.replace(string, ".", "");
		string = org.apache.commons.lang3.StringUtils.replace(string, ",", ".");
		System.out.println("string -> " + string);

		String rowData = "1. Do'a Sebelum Belajar->A;A-;B+;B;C+;C;D";
		String[] colAtauRow = rowData.split("->");

		String[] colAtauRowOld = rowData.split("->");

		String colsOld = colAtauRowOld.length > 1 ? colAtauRowOld[1] : "";
		System.out.println("colsOld => " + colsOld);

		String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";

		System.out.println("cols => " + cols);
	}

}
