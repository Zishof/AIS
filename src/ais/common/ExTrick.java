package ais.common;

/**
 * Kelas percobaan (scratch class) yang mendemonstrasikan teknik introspeksi pemanggil (<i>caller
 * introspection</i>) di Java memakai {@link Throwable#getStackTrace()} — yaitu cara sebuah method
 * mengetahui siapa (kelas dan method apa) yang memanggilnya tanpa parameter eksplisit yang
 * diteruskan pemanggil. Namanya "ExTrick" (Exception Trick) menegaskan bahwa teknik ini bekerja
 * dengan memanfaatkan efek samping dari melempar dan langsung menangkap sebuah {@link Exception}:
 * begitu {@link Exception} dibuat, JVM otomatis mengisi jejak tumpukan (<i>stack trace</i>) pada
 * titik pembuatannya, dan jejak tersebut dapat dibaca ulang untuk menelusuri rantai pemanggilan
 * tanpa benar-benar membiarkan exception tersebut merambat keluar.
 *
 * <p>
 * Kelas ini TIDAK dipanggil dari bagian lain aplikasi AIS mana pun (tidak ada referensi masuk dari
 * modul akademik, keuangan, atau modul lain manapun di codebase) — satu-satunya jalur eksekusi
 * adalah lewat {@link #main(String[])}-nya sendiri. Fungsinya murni sebagai catatan/percobaan
 * teknik pemrograman, kemungkinan besar ditinggalkan dari fase eksplorasi awal proyek atau sebagai
 * referensi pola bagi pengembang yang ingin menambahkan logging/audit berbasis pemanggil di tempat
 * lain (bandingkan dengan pemakaian {@code Thread.currentThread().getStackTrace()} pada beberapa
 * utilitas audit lain di paket {@code ais.common}).
 * </p>
 *
 * <p>
 * <b>Catatan performa</b> — mengisi stack trace (baik lewat pelemparan exception sungguhan maupun
 * lewat pembuatan objek {@link Throwable} tanpa dilempar) relatif mahal dibandingkan pemanggilan
 * method biasa karena JVM harus menelusuri seluruh bingkai tumpukan aktif. Karena itu pola ini
 * sebaiknya tidak dipakai pada jalur kode yang sering dieksekusi (hot path); ia cocok untuk
 * kebutuhan diagnostik/audit yang jarang terjadi.
 * </p>
 */
public class ExTrick {
	/**
	 * Titik masuk uji coba manual. Membuat satu instance {@link ExTrick} lalu memanggil
	 * {@link #callMeAnyTime()} untuk mendemonstrasikan bagaimana method tersebut menemukan identitas
	 * pemanggilnya sendiri (dalam hal ini, method {@code main} ini) lewat jejak tumpukan exception.
	 *
	 * @param args argumen baris perintah, tidak dipakai
	 */
	public static void main(String args[]) {
		new ExTrick().callMeAnyTime();
	}

	/**
	 * Mendemonstrasikan teknik introspeksi pemanggil: melempar sebuah {@link Exception} lokal lalu
	 * segera menangkapnya sendiri, kemudian membaca elemen kedua ({@code getStackTrace()[1]}, indeks
	 * 0 adalah bingkai method ini sendiri) dari jejak tumpukan exception tersebut untuk menemukan
	 * nama kelas dan nama method dari pemanggil langsung. Hasilnya dicetak ke {@link System#out}.
	 * Method ini tidak melempar exception keluar (exception yang dibuat murni dipakai sebagai
	 * wadah jejak tumpukan, bukan sebagai sinyal kegagalan) dan tidak mengembalikan nilai.
	 */
	void callMeAnyTime() {
		try {
			throw new Exception("Who called me?");
		} catch (Exception e) {
			System.out.println("I was called by "
					+ e.getStackTrace()[1].getClassName() + "."
					+ e.getStackTrace()[1].getMethodName() + "()!");
		}
	}
}
