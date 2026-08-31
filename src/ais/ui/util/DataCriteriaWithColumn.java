package ais.ui.util;

/**
 * Varian {@link DataCriteria} yang mengembalikan sepasang informasi sekaligus alih-alih satu
 * objek kriteria tunggal: array {@link Object}[] yang lazimnya berisi {@code {kriteria, daftar
 * kolom/proyeksi}} atau kombinasi sejenis yang dibutuhkan pemanggil untuk membangun grid dengan
 * kolom dinamis. Dipakai pada layar-layar yang kolom tampilannya tidak tetap (bergantung pada
 * konfigurasi/pilihan pengguna) sehingga kriteria pencarian dan definisi kolom perlu dibangun
 * bersamaan dalam satu pemanggilan.
 */
public interface DataCriteriaWithColumn {

	/**
	 * Membangun kriteria pencarian beserta informasi kolom terkait.
	 *
	 * @param order bila {@code true}, kriteria yang dikembalikan menyertakan pengurutan
	 * @return array objek; elemen pertama lazimnya kriteria pencarian, elemen berikutnya
	 *         informasi kolom/proyeksi tambahan sesuai kebutuhan implementasi
	 */
	public Object[] initCriteria(boolean order);

}
