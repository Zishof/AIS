package ais.action.master.recruitment.helper;

import java.util.List;

import ais.database.model.recruitment.CalonPegawai;

/**
 * Kontrak algoritma penomoran Nomor Ujian untuk {@link CalonPegawai} (modul rekrutmen pegawai),
 * padanan {@code NoUjianGenerator} pada modul PMB (penerimaan mahasiswa baru) — memungkinkan setiap
 * institusi mengimplementasikan pola nomor ujian rekrutmen pegawainya sendiri.
 */
public interface NoUjianGeneratorPegawai {

	/** Varian ringkas {@link #generateNoUjian(CalonPegawai, List)} tanpa daftar pengecualian awal. */
	public String generateNoUjian(CalonPegawai calonPegawai) throws Exception;

	/**
	 * Menghasilkan nomor ujian untuk {@code calonPegawai}.
	 *
	 * @param noRegPengecualian nomor-nomor yang harus dianggap sudah terpakai (biasanya dipakai implementasi untuk percobaan ulang rekursif saat nomor bentrok)
	 * @return nomor ujian yang dihasilkan
	 */
	public String generateNoUjian(CalonPegawai calonPegawai, List<String> noRegPengecualian) throws Exception;

}
