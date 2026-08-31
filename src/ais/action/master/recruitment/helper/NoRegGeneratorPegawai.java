package ais.action.master.recruitment.helper;

import java.util.List;

import ais.database.model.recruitment.CalonPegawai;


/**
 * Kontrak pembangkit Nomor Registrasi (No. Reg) untuk modul rekrutmen pegawai. Format nomor
 * registrasi dapat berbeda per institusi (mis. berbasis tahun, gelombang, urutan pendaftaran),
 * sehingga logika pembentukan nomor diimplementasikan per institusi lewat kelas yang
 * mengimplementasikan antarmuka ini — bandingkan dengan {@link
 * ais.action.master.pmb.noreg} untuk pola serupa pada penomoran registrasi mahasiswa baru.
 */
public interface NoRegGeneratorPegawai {

	/**
	 * Membangkitkan nomor registrasi baru untuk satu calon pegawai.
	 *
	 * @param calonPegawai data calon pegawai yang akan diberi nomor registrasi
	 * @return nomor registrasi yang dibangkitkan, sesuai format khusus institusi
	 */
	public String generateNoReg(CalonPegawai calonPegawai);

	/**
	 * Membangkitkan nomor registrasi baru untuk satu calon pegawai, dengan menghindari nomor
	 * yang sudah dipakai (mis. saat pembangkitan ulang massal).
	 *
	 * @param noRegPengecualian daftar nomor registrasi yang harus dihindari/tidak boleh dipakai ulang
	 * @param calonPegawai      data calon pegawai yang akan diberi nomor registrasi
	 * @return nomor registrasi yang dibangkitkan, tidak termasuk dalam {@code noRegPengecualian}
	 */
	public String generateNoReg(List<String> noRegPengecualian, CalonPegawai calonPegawai);

}
