package ais.database.model;

/**
 * Value object kontrak bersama untuk entitas yang merepresentasikan peserta suatu sesi
 * pembelajaran (mis. mahasiswa/siswa yang mengikuti satu {@link VOPembelajaran}), bukan
 * entitas Hibernate — hanya antarmuka yang menyeragamkan cara mengakses objek pembelajaran
 * yang diikuti peserta tersebut.
 */
public interface VOPesertaPembelajaran {

	/** Mengambil objek pembelajaran (mata kuliah/sesi) yang diikuti oleh peserta ini. */
	public VOPembelajaran ambilVOPembelajaran();
}