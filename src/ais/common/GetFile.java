package ais.common;

import java.io.File;

/**
 * Kontrak sederhana untuk objek yang mampu menyediakan referensi ke sebuah {@link File} pada
 * disk. Dipakai sebagai abstraksi umum di berbagai bagian AIS yang perlu memperlakukan
 * bermacam-macam sumber data (mis. hasil unggahan, lampiran, dokumen yang dihasilkan sistem)
 * secara seragam tanpa peduli bagaimana objek tersebut memperoleh berkas fisiknya — cukup
 * memanggil {@link #getFile()} untuk mendapatkan berkas siap pakai.
 *
 * <p>
 * Interface ini sengaja minimal (satu method) agar mudah diimplementasikan oleh kelas mana pun
 * yang sudah memiliki referensi {@link File}, baik itu wrapper hasil upload, hasil generate
 * laporan, maupun objek transient lain yang dipakai sekali pakai sebelum dibuang.
 * </p>
 */
public interface GetFile {
	/**
	 * Mengembalikan berkas fisik yang direpresentasikan oleh implementasi ini.
	 *
	 * @return referensi {@link File} milik implementasi; kontrak null-safety bergantung pada
	 *         masing-masing implementasi (tidak dijamin oleh interface ini)
	 */
	public File getFile();
}
