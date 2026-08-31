package ais.database.model;

import java.util.TreeMap;

import org.zkoss.zul.Label;

/**
 * Value object kontrak bersama untuk entitas yang merepresentasikan pihak dalam relasi
 * pembelajaran mahasiswa-dosen (mis. mahasiswa peserta dan dosen pengampu suatu mata kuliah),
 * bukan entitas Hibernate — hanya antarmuka yang menyeragamkan cara mengambil identitas
 * ({@link #ambilKode()}/{@link #getNama()}) dan materi pertemuan terkait.
 */
public interface VOMahasiswaDosen {
	/** Mengambil kode identitas pihak ini (mis. NIM mahasiswa atau NIDN/kode dosen). */
	public String ambilKode();

	public String getNama();

	/**
	 * Mengambil peta materi per pertemuan untuk keperluan tampilan (mis. rekap kehadiran/materi
	 * kuliah), dengan {@code pertemuans} sebagai daftar pertemuan yang diminta, {@code refresh}
	 * untuk memaksa pengambilan ulang dari sumber data (melewati cache bila ada), dan
	 * {@code label} komponen ZK yang mungkin diperbarui langsung (mis. indikator progres) selama
	 * proses pengambilan.
	 */
	public TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label);
}
