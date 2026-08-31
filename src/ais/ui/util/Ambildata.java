package ais.ui.util;

/**
 * Kontrak generik untuk kelas helper/pengambil-data (mis. {@code AmbilDataDosenBanbox},
 * {@code PenjaminanMutuAnalisisHelper}, {@code HasilUjianSiswaHelper}, {@code
 * HasilUjianMahasiswaHelper}) yang bertugas mengambil satu objek data dari sumbernya
 * (biasanya query database via Hibernate) untuk dipakai oleh layar/aksi ZK pemanggil.
 * Implementasi menentukan sendiri sumber dan kriteria pengambilan; interface ini hanya
 * menstandardisasi titik panggil {@link #ambil()} sehingga kode pemanggil dapat memperlakukan
 * berbagai strategi pengambilan data secara seragam.
 */
public interface Ambildata {

	/**
	 * Mengambil dan mengembalikan objek data sesuai implementasi kelas pemanggil.
	 *
	 * @return objek data hasil pengambilan; tipe dan makna bergantung pada implementasi
	 */
	public Object ambil();
}
