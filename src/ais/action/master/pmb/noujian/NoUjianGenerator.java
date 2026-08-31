package ais.action.master.pmb.noujian;

import java.util.List;

import ais.database.model.BiodataCalonMahasiswa;

/**
 * Kontrak pembangkit nomor ujian (No Ujian) calon mahasiswa pada modul PMB (Penerimaan Mahasiswa
 * Baru). Setiap institusi punya format/algoritma penomoran ujiannya sendiri (lihat implementasi
 * konkret di paket ini, mis. {@link DefaultNoUjianGenerator} dan {@link BukittinggiNoUjianGenerator})
 * yang mengimplementasikan antarmuka seragam ini.
 */
public interface NoUjianGenerator {

	/** Membangkitkan nomor ujian baru untuk {@code biodataCalonMahasiswa}. */
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa)
			throws Exception;

	/** Seperti {@link #generateNoUjian(BiodataCalonMahasiswa)}, dengan tambahan daftar nomor yang harus dihindari (mis. sudah dipakai di batch yang sama namun belum tersimpan ke database). */
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa,
			List<String> noRegPengecualian) throws Exception;

}
