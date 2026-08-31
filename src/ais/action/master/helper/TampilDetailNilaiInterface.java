package ais.action.master.helper;

import ais.database.model.Perkuliahan;


/**
 * Kontrak callback untuk komponen tampilan detail nilai yang perlu dimuat ulang setelah nilai satu
 * {@link Perkuliahan} berubah (mis. setelah penyimpanan komponen penilaian). Diimplementasikan oleh
 * layar/helper yang menampilkan rincian nilai perkuliahan agar dapat dipanggil ulang secara seragam
 * oleh kode yang memicu perubahan nilai.
 */
public interface TampilDetailNilaiInterface {

	/**
	 * Memuat ulang tampilan detail nilai untuk perkuliahan yang diberikan.
	 *
	 * @param perkuliahan perkuliahan yang nilainya berubah dan perlu ditampilkan ulang
	 */
	public void realoadNilai(Perkuliahan perkuliahan);

}
