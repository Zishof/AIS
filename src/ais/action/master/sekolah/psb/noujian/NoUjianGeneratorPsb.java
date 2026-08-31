package ais.action.master.sekolah.psb.noujian;

import java.util.List;

import ais.database.model.sekolah.CalonSiswa;

/**
 * Kontrak algoritma pembangkit nomor ujian PSB (Penerimaan Siswa Baru) untuk seorang
 * {@link CalonSiswa}. Setiap institusi/sekolah dapat memiliki format nomor ujian berbeda
 * (mis. berbasis tahun, jenjang, urutan pendaftaran); implementasi konkret memutuskan format
 * tersebut, sementara kode pemanggil cukup bergantung pada kontrak ini.
 */
public interface NoUjianGeneratorPsb {

	/**
	 * @param calonSiswa calon siswa yang akan diberi nomor ujian
	 * @return nomor ujian baru yang belum pernah dipakai
	 */
	public String generateNoUjian(CalonSiswa calonSiswa)
			throws Exception;

	/**
	 * Seperti {@link #generateNoUjian(CalonSiswa)}, dengan daftar nomor yang harus dihindari
	 * (mis. nomor yang sudah dipesan dalam batch yang sama namun belum tersimpan ke basis data).
	 *
	 * @param calonSiswa        calon siswa yang akan diberi nomor ujian
	 * @param noRegPengecualian nomor ujian yang tidak boleh dihasilkan ulang
	 * @return nomor ujian baru yang belum pernah dipakai dan tidak ada dalam daftar pengecualian
	 */
	public String generateNoUjian(CalonSiswa calonSiswa,
			List<String> noRegPengecualian) throws Exception;

}
