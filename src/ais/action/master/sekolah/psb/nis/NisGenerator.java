package ais.action.master.sekolah.psb.nis;

import java.util.List;

import ais.database.model.sekolah.CalonSiswa;

/**
 * Kontrak pembangkit Nomor Induk Siswa (NIS) untuk modul Penerimaan Siswa Baru (PSB) di jenjang
 * sekolah. Setiap institusi/sekolah dapat memiliki format penomoran NIS yang berbeda (mis.
 * berbasis tahun ajaran, jenjang, urutan pendaftaran), sehingga logika pembentukan nomor
 * diimplementasikan per institusi lewat kelas yang mengimplementasikan antarmuka ini.
 */
public interface NisGenerator {

	/**
	 * Membangkitkan NIS baru untuk satu calon siswa.
	 *
	 * @param calonSiswa data calon siswa yang akan diberi NIS
	 * @return NIS yang dibangkitkan, sesuai format khusus institusi
	 */
	public String generateNis(CalonSiswa calonSiswa);

	/**
	 * Membangkitkan NIS baru untuk satu calon siswa, dengan menghindari nomor yang sudah
	 * dipakai (mis. saat pembangkitan ulang massal).
	 *
	 * @param calonSiswa      data calon siswa yang akan diberi NIS
	 * @param nimPengecualian daftar NIS yang harus dihindari/tidak boleh dipakai ulang
	 * @return NIS yang dibangkitkan, tidak termasuk dalam {@code nimPengecualian}
	 */
	public String generateNis(CalonSiswa calonSiswa, List<String> nimPengecualian);

}
