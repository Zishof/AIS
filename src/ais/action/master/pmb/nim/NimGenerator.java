package ais.action.master.pmb.nim;

import java.util.List;

import ais.database.model.BiodataCalonMahasiswa;

/**
 * Kontrak baku untuk seluruh algoritma penomoran NIM (Nomor Induk Mahasiswa) khusus per institusi
 * pada paket {@code ais.action.master.pmb.nim}. Setiap implementasi (satu kelas per institusi,
 * mis. {@code DefaultNimGenerator}, {@code ItkpNimGenerator}, {@code UkawNimGenerator}, dst.)
 * membangkitkan NIM baru berdasarkan data calon mahasiswa (prodi, tahun masuk, jalur PMB, dsb.)
 * mengikuti format penomoran institusi tersebut, sambil memastikan hasilnya belum terpakai.
 */
public interface NimGenerator {

	/**
	 * Membangkitkan NIM baru untuk calon mahasiswa yang diberikan tanpa daftar pengecualian
	 * tambahan.
	 *
	 * @param calonMahasiswa data calon mahasiswa yang menjadi dasar pembentukan NIM
	 * @return NIM baru sesuai format institusi
	 */
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa);

	/**
	 * Seperti {@link #generateNim(BiodataCalonMahasiswa)}, dengan tambahan daftar NIM yang harus
	 * dihindari (mis. NIM yang sudah dialokasikan pada proses batch yang sama tapi belum tersimpan
	 * ke database) agar tidak terjadi duplikasi dalam satu batch pembangkitan.
	 *
	 * @param calonMahasiswa  data calon mahasiswa yang menjadi dasar pembentukan NIM
	 * @param nimPengecualian daftar NIM yang tidak boleh dihasilkan ulang
	 * @return NIM baru sesuai format institusi, dijamin tidak ada dalam {@code nimPengecualian}
	 */
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa,
			List<String> nimPengecualian);

}
