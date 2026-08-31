package ais.action.master.sekolah.psb.noreg;

import java.util.List;

import ais.database.model.sekolah.CalonSiswa;

/**
 * Kontrak algoritma pembangkitan nomor registrasi PSB (Penerimaan Siswa Baru) untuk seorang
 * {@link CalonSiswa}. Implementasi berbeda-beda per institusi/sekolah, masing-masing dengan pola
 * format nomor registrasi tersendiri (mis. berbasis tahun ajaran, jenjang, urutan pendaftaran).
 */
public interface NoRegGeneratorPsb {

	/**
	 * Membangkitkan nomor registrasi baru untuk {@code calonSiswa}.
	 *
	 * @param calonSiswa calon siswa yang akan diberi nomor registrasi
	 * @return nomor registrasi baru sesuai format institusi
	 */
	public String generateNoReg(CalonSiswa calonSiswa);

	/**
	 * Seperti {@link #generateNoReg(CalonSiswa)}, dengan tambahan daftar nomor registrasi yang
	 * harus dihindari (mis. karena sudah dipakai/direservasi di proses lain yang belum tersimpan)
	 * sehingga hasilnya dijamin tidak bentrok dengan nomor-nomor tersebut.
	 *
	 * @param noRegPengecualian daftar nomor registrasi yang tidak boleh dihasilkan ulang
	 * @param calonSiswa        calon siswa yang akan diberi nomor registrasi
	 * @return nomor registrasi baru yang tidak termasuk dalam {@code noRegPengecualian}
	 */
	public String generateNoReg(List<String> noRegPengecualian, CalonSiswa calonSiswa);

}
