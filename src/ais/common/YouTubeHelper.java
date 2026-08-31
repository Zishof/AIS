package ais.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitas statis ringan untuk mengekstrak ID video YouTube dari berbagai bentuk URL YouTube
 * yang umum dijumpai (URL halaman tonton biasa, URL pendek {@code youtu.be}, URL embed, maupun
 * varian ter-encode URL-nya), serta untuk membangun markup {@code <iframe>} embed siap pakai dari
 * URL tersebut. Kelas ini TIDAK memanggil YouTube Data API \u2014 tidak ada API key, kredensial, atau
 * panggilan jaringan apa pun yang terlibat; seluruh proses murni berbasis pencocokan pola teks
 * (regex) terhadap string URL yang diberikan pemanggil, sehingga aman dipakai tanpa konfigurasi
 * tambahan maupun kekhawatiran kredensial tertanam.
 *
 * <p>
 * Kelas ini biasa dipakai pada fitur-fitur AIS yang mengizinkan pengguna menempelkan tautan
 * YouTube (mis. materi perkuliahan, pengumuman, atau konten pengayaan) dan menampilkannya secara
 * otomatis sebagai video ter-embed di halaman, tanpa mengharuskan pengguna memasukkan kode embed
 * mentah secara manual.
 * </p>
 *
 * <p>
 * <b>Mekanisme ekstraksi</b> \u2014 field {@link #pattern} adalah satu ekspresi reguler tunggal yang
 * memakai <i>lookbehind</i> ({@code (?<=...)}) untuk mendaftar seluruh penanda (marker) yang
 * mendahului bagian ID video pada berbagai bentuk URL YouTube yang dikenal (mis. {@code watch?v=},
 * {@code youtu.be/}, {@code /embed/}, {@code /v/}, beserta varian ter-encode URL seperti
 * {@code watch?v%3D} dan {@code %2Fvideos%2F}), diikuti oleh kelompok karakter yang BUKAN salah
 * satu dari {@code #}, {@code &}, {@code ?}, atau baris baru \u2014 yaitu bagian ID video itu sendiri,
 * yang berhenti begitu bertemu parameter query tambahan atau fragment URL. Pendekatan lookbehind
 * tunggal ini memungkinkan satu pola regex menangani banyak variasi bentuk URL sekaligus tanpa
 * perlu percabangan kode terpisah per format.
 * </p>
 */
public class YouTubeHelper {

	/**
	 * Pola regex tunggal (memakai lookbehind) yang mencocokkan ID video pada berbagai bentuk URL
	 * YouTube yang dikenal (halaman tonton, {@code youtu.be}, embed, serta varian ter-encode
	 * URL-nya). Dipakai oleh {@link #extractVideoIdFromUrl(String)} dan {@link #main(String[])}.
	 */
	public static String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";

	/**
	 * Mengekstrak ID video YouTube (11 karakter khas YouTube, meski tidak divalidasi panjangnya
	 * secara eksplisit di sini) dari sebuah URL YouTube, dengan mencocokkan {@code url} terhadap
	 * pola {@link #pattern}.
	 *
	 * @param url URL YouTube dalam bentuk apa pun yang dikenali {@link #pattern} (halaman tonton,
	 *            {@code youtu.be}, embed, dsb.)
	 * @return ID video yang berhasil diekstrak, atau string kosong ({@code ""}) bila tidak ada
	 *         kecocokan pola pada {@code url} yang diberikan
	 */
	public static String extractVideoIdFromUrl(String url) {

		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(url); // url is youtube url
														// for which you want to
														// extract the id.

		String id = "";
		if (matcher.find()) {
			id = matcher.group();
		}
		return id;
	}

	// https://youtu.be/xzQb7G_-UYE

	/**
	 * Titik masuk baris perintah sederhana untuk menguji/mendemonstrasikan
	 * {@link #extractVideoIdFromUrl(String)} secara manual terhadap satu URL contoh yang
	 * ditulis tetap (hardcoded) di dalam method ini, lalu mencetak ID yang berhasil diekstrak ke
	 * konsol. Tidak dipakai oleh alur aplikasi AIS yang sebenarnya — murni alat bantu
	 * pengembangan/verifikasi pola regex.
	 *
	 * @param argv argumen baris perintah; tidak dipakai
	 */
	public static void main(String[] argv) {
		String url = "https://youtu.be/xzQb7G_-UYE";

		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(url); // url is youtube url
														// for which you want to
														// extract the id.

		String id = "";
		if (matcher.find()) {
			id = matcher.group();
		}

		System.out.println("ID = " + id);
	}

	/**
	 * Membangun markup HTML {@code <iframe>} siap tempel untuk menampilkan video YouTube
	 * ter-embed dari sebuah URL YouTube, dengan ID video diekstrak otomatis lewat
	 * {@link #extractVideoIdFromUrl(String)}. Iframe yang dihasilkan memakai lebar 100% (relatif
	 * terhadap kontainer pemanggil), tinggi minimum 315px, tanpa border, dan mengizinkan fitur
	 * accelerometer, autoplay, encrypted-media, gyroscope, picture-in-picture, serta mode layar
	 * penuh.
	 *
	 * @param url URL YouTube dalam bentuk apa pun yang dikenali {@link #extractVideoIdFromUrl(String)}
	 * @return string markup HTML {@code <iframe>} yang menyematkan video YouTube tersebut; bila
	 *         ID video tidak berhasil diekstrak dari {@code url}, iframe tetap dihasilkan namun
	 *         mengarah ke URL embed tanpa ID video yang valid
	 */
	public static String convertoToEmbed(String url) {

		String id = extractVideoIdFromUrl(url);

		String v = "<iframe style=\"width:100%;min-height: 315px;\" "
				+ " src=\"https://www.youtube.com/embed/" + id
				+ "\" frameborder=\"0\" allow=\"accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>";

		return v;
	}
}