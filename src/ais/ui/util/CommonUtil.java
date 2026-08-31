package ais.ui.util;

import java.util.Calendar;


/**
 * Berkas uji coba/scratch manual (bukan bagian dari alur aplikasi, tidak dipanggil kelas lain)
 * yang hanya berisi satu titik masuk {@link #main(String[])} untuk mencetak nilai konstanta
 * {@link Calendar#SATURDAY} ke konsol — dipakai sebagai pengecekan cepat nilai konstanta
 * {@code Calendar} saat pengembangan, bukan utilitas yang dipakai ulang oleh kode lain.
 */
public class CommonUtil {

	/**
	 * Mencetak nilai konstanta {@link Calendar#SATURDAY} ke konsol.
	 *
	 * @param argv argumen baris perintah, tidak dipakai
	 */
	public static void main(String[]argv){
		System.out.println(Calendar.SATURDAY);
	}

}
