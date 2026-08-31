package ais.action.master.recruitment.helper;

import java.util.List;

import ais.database.model.recruitment.CalonPegawai;

/**
 * Kontrak algoritma pembangkit NIS (Nomor Induk Sistem/Pegawai) untuk seorang
 * {@link CalonPegawai} pada modul rekrutmen. Format nomor bergantung pada implementasi konkret
 * per institusi; kode pemanggil hanya bergantung pada kontrak ini.
 */
public interface NisGenerator {

	/**
	 * @param calonPegawai calon pegawai yang akan diberi NIS
	 * @return NIS baru yang belum pernah dipakai
	 */
	public String generateNis(CalonPegawai calonPegawai);

	/**
	 * Seperti {@link #generateNis(CalonPegawai)}, dengan daftar NIS yang harus dihindari
	 * (mis. nomor yang sudah dipesan dalam batch yang sama namun belum tersimpan).
	 *
	 * @param calonPegawai   calon pegawai yang akan diberi NIS
	 * @param nimPengecualian NIS yang tidak boleh dihasilkan ulang
	 * @return NIS baru yang belum pernah dipakai dan tidak ada dalam daftar pengecualian
	 */
	public String generateNis(CalonPegawai calonPegawai, List<String> nimPengecualian);

}
