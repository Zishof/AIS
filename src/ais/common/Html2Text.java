package ais.common;

import java.io.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

/**
 * Kelas bantu untuk mengekstrak teks polos (plain text) dari dokumen HTML, memanfaatkan parser
 * HTML bawaan Swing ({@link javax.swing.text.html.parser.ParserDelegator}) alih-alih pustaka
 * parsing HTML pihak ketiga (bandingkan dengan pemakaian Jsoup di banyak tempat lain pada
 * codebase AIS, mis. {@code ais.delivery.email.sender.MailSender}).
 *
 * <p>
 * Cara pakai: buat instans {@link Html2Text}, panggil {@link #parse(Reader)} dengan sumber HTML,
 * lalu ambil hasil ekstraksi lewat {@link #getText()}. Kelas ini bekerja sebagai
 * {@link HTMLEditorKit.ParserCallback} — {@link #handleText(char[], int)} dipanggil balik oleh
 * parser Swing setiap kali menemukan node teks di dalam dokumen HTML, dan implementasinya di
 * sini sekadar menumpuk teks tersebut ke buffer internal {@link #s} tanpa memproses tag/atribut
 * HTML lain (bukan {@code handleStartTag}/{@code handleEndTag}), sehingga hasil akhirnya adalah
 * gabungan seluruh teks yang tampak pada dokumen, tanpa markup.
 * </p>
 *
 * <p>
 * <b>Catatan:</b> method {@link #main(String[])} pada kelas ini adalah kode demo/uji coba manual
 * peninggalan — path file input ({@code "java-new.html"}) dibuka lewat {@link FileReader} namun
 * hasil baca tersebut TIDAK dipakai; parser justru dipanggil dengan {@link StringReader} kosong
 * ({@code new StringReader("")}), sehingga menjalankan {@code main} ini hanya akan mencetak
 * string kosong walau file HTML berhasil dibuka. Ini kemungkinan bug peninggalan pada kode
 * contoh dan tidak memengaruhi kelas {@link Html2Text} sebagai komponen yang dipakai dari kode
 * lain (bila ada), yang mana perilakunya ditentukan sepenuhnya oleh argumen {@link Reader} yang
 * diberikan pemanggil ke {@link #parse(Reader)}.
 * </p>
 */
public class Html2Text extends HTMLEditorKit.ParserCallback {
	/** Buffer penampung teks yang diekstrak, diisi bertahap oleh {@link #handleText(char[], int)}. */
	StringBuffer s;

	/** Konstruktor kosong; state ({@link #s}) baru diinisialisasi saat {@link #parse(Reader)} dipanggil. */
	public Html2Text() {
	}

	/**
	 * Mem-parsing dokumen HTML dari {@code in} dan mengumpulkan seluruh teksnya ke buffer internal.
	 * Memanggil ulang method ini pada instans yang sama akan mereset buffer sebelumnya (buffer baru
	 * dibuat setiap pemanggilan).
	 *
	 * @param in sumber karakter HTML yang akan diparsing
	 * @throws IOException bila terjadi galat baca dari {@code in} selama parsing
	 */
	public void parse(Reader in) throws IOException {
		s = new StringBuffer();
		ParserDelegator delegator = new ParserDelegator();
		// the third parameter is TRUE to ignore charset directive
		delegator.parse(in, this, Boolean.TRUE);
	}

	/**
	 * Callback dari parser Swing setiap kali sebuah node teks ditemukan di dokumen HTML; teks
	 * tersebut ditambahkan apa adanya ke buffer {@link #s}.
	 *
	 * @param text potongan karakter teks yang ditemukan parser
	 * @param pos  posisi (offset) potongan teks tersebut dalam dokumen sumber, tidak dipakai
	 */
	public void handleText(char[] text, int pos) {
		s.append(text);
	}

	/**
	 * Mengambil hasil akhir ekstraksi teks setelah {@link #parse(Reader)} selesai dijalankan.
	 *
	 * @return seluruh teks yang terkumpul dari dokumen HTML yang diparsing, tanpa markup
	 */
	public String getText() {
		return s.toString();
	}

	/**
	 * Titik masuk demo/uji coba manual (bukan dipanggil dari kode aplikasi lain) untuk mencoba
	 * kelas ini secara command-line. Lihat catatan di Javadoc kelas mengenai bug peninggalan pada
	 * method ini (input file dibaca tetapi tidak dipakai; parser dipanggil dengan string kosong).
	 *
	 * @param args tidak dipakai
	 */
	public static void main(String[] args) {
		try {
			// the HTML to convert
			FileReader in = new FileReader("java-new.html");
			Html2Text parser = new Html2Text();
			parser.parse(new StringReader(""));
			in.close();
			System.out.println(parser.getText());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}
}