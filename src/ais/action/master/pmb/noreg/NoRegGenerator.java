package ais.action.master.pmb.noreg;

import java.util.List;

import ais.database.model.BiodataCalonMahasiswa;

/**
 * Kontrak pembangkit nomor registrasi (No Reg) calon mahasiswa pada modul PMB (Penerimaan
 * Mahasiswa Baru). Setiap institusi dapat mengimplementasikan format/algoritma penomoran
 * registrasinya sendiri (bandingkan dengan pola serupa pada
 * {@link ais.action.master.sekolah.psb.noreg.DefaultNoRegGeneratorPsb} untuk modul PSB sekolah)
 * melalui antarmuka yang seragam ini.
 */
public interface NoRegGenerator {

	/** Membangkitkan nomor registrasi baru untuk {@code biodataCalonMahasiswa}. */
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa);

	/** Seperti {@link #generateNoReg(BiodataCalonMahasiswa)}, dengan tambahan daftar nomor registrasi yang harus dihindari (mis. sudah dipakai di batch yang sama namun belum tersimpan ke database). */
	public String generateNoReg(List<String> noRegPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa);

}
