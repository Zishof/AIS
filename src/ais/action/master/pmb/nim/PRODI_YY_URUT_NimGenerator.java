package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma NIM pola umum "PRODI_YY_URUT": {@code [prefix konfigurasi][kode program studi
 * kelulusan][2 digit terakhir tahun angkatan][N digit nomor urut]}, mis. {@code TI260007}. Prefix
 * diambil dari konfigurasi {@code prefix_pmb} (default kosong) dan jumlah digit nomor urut dari
 * {@code jumlah_digit_gen_nim_mahasiswa} (default 4). Nomor urut dihitung dari jumlah
 * {@link Mahasiswa} aktif pada tahun angkatan dan jurusan (prodi kelulusan) yang sama, ditambah
 * jumlah kandidat yang sudah dicoba tapi bentrok pada pemanggilan rekursif, lalu ditambah 1 dan
 * dipad nol ke kiri. Bila {@code calonMahasiswa.getProdiLulus()} belum diisi, NIM dikembalikan
 * sebagai {@code "-"}. Bukan spesifik satu institusi — dipakai sebagai pola default yang dapat
 * dikonfigurasi lintas tenant.
 */
public class PRODI_YY_URUT_NimGenerator implements NimGenerator {

	/** Menghasilkan NIM baru tanpa daftar pengecualian awal — lihat {@link #generateNim(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM berformat {@code [prefix]+kode prodi lulus+2 digit tahun+N digit urut},
	 * menghindari nilai yang ada di {@code jumlahPengecualian} maupun yang sudah dipakai mahasiswa
	 * lain di database; mencoba ulang secara rekursif bila terjadi bentrok.
	 *
	 * @param calonMahasiswa     calon mahasiswa target; {@code prodiLulus} dan {@code tahun}-nya
	 *                           menentukan bagian awal NIM
	 * @param jumlahPengecualian NIM-NIM yang sudah dicoba dan diketahui bentrok, dihindari pada
	 *                           percobaan berikutnya (diperbarui di tempat)
	 * @return NIM baru yang belum pernah dipakai, atau {@code "-"} bila {@code prodiLulus} belum diisi
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = calonMahasiswa.getProdiLulus().getKode();

			String digitKedua = tahun.toString().substring(2);

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/PRODI_YY_URUT_NimGenerator.java:45");

			}
			String prefix = Common.getKonfigurasi("prefix_pmb", "").getNilai() + digitPertama + digitKedua;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahDigit, calonMahasiswa,
					jumlahPengecualian);
			String digitKetiga = NimGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode prodi) = " + digitKedua);
			System.out.println("digit ketiga (urutan) = " + digitKetiga);

			nim = prefix + digitKetiga;
			boolean nimSudahDipakai = NimGeneratorSupport.nimSudahDipakai(session, nim, calonMahasiswa);

			HibernateUtil.closeSessionQuietly(session);

			if (nimSudahDipakai) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

		}

		return nim;
	}

}
