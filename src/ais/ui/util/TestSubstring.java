package ais.ui.util;

/**
 * Berkas uji coba/scratch manual (bukan bagian dari alur aplikasi, tidak dipanggil kelas lain)
 * untuk mengecek perilaku {@link String#substring(int)} dan {@link String#substring(int, int)}
 * terhadap string angka tanggal ({@code "202308"}, format {@code yyyyMM}). Dijalankan langsung
 * lewat {@link #main(String[])} dari IDE/command line untuk melihat hasil potongan string di
 * konsol; tidak memiliki efek samping maupun nilai kembali yang dikonsumsi program lain.
 */
public class TestSubstring {

	/**
	 * Mencetak ke konsol string asal {@code "202308"} beserta dua variasi potongannya: dari
	 * indeks 4 hingga akhir, dan dari indeks 0 sampai (tidak termasuk) indeks 4.
	 *
	 * @param args argumen baris perintah, tidak dipakai
	 */
	public static void main(String args[]) {
		String s = "202308";
		System.out.println("Original String: " + s);
		System.out.println("Substring starting from index 4: " + s.substring(4));// Tendulkar
		System.out.println("Substring starting from index 0 to 6: " + s.substring(0, 4)); // Sachin
	}
}
