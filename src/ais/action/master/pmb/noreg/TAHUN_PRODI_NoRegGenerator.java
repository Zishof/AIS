package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma nomor registrasi PMB pola umum "TAHUN_PRODI": {@code [tahun pendaftaran][kode program
 * studi pilihan pertama][N digit nomor urut]}, mis. {@code 2026TI001}. Jumlah digit nomor urut
 * dapat diatur lewat konfigurasi {@code jumlah_digit_no_reg_calon_mhs} (default 3). Nomor urut
 * dihitung dari jumlah {@link BiodataCalonMahasiswa} aktif pada tahun dan prodi yang sama, ditambah
 * jumlah kandidat yang sudah dicoba tapi bentrok pada pemanggilan rekursif, lalu ditambah 1 dan
 * dipad nol ke kiri. Bila hasil gabungan ternyata sudah dipakai calon mahasiswa aktif lain, nomor
 * tersebut dicatat sebagai pengecualian dan method memanggil dirinya sendiri untuk mencoba nomor
 * berikutnya. Bukan spesifik satu institusi — dipakai sebagai pola default lintas tenant yang tidak
 * memerlukan format nomor registrasi khusus.
 */
public class TAHUN_PRODI_NoRegGenerator implements NoRegGenerator {

	/** Menghasilkan nomor registrasi baru tanpa daftar pengecualian awal — lihat {@link #generateNoReg(List, BiodataCalonMahasiswa)}. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Menghasilkan nomor registrasi berformat {@code [tahun][kode prodi1][N digit urut]} (N dari
	 * konfigurasi {@code jumlah_digit_no_reg_calon_mhs}), menghindari nomor yang ada di
	 * {@code jumlahPengecualian} maupun yang sudah dipakai calon mahasiswa aktif lain di database;
	 * mencoba ulang secara rekursif bila terjadi bentrok.
	 *
	 * @param jumlahPengecualian nomor-nomor yang sudah dicoba dan diketahui bentrok, dihindari pada
	 *                           percobaan berikutnya (diperbarui di tempat)
	 * @param biodataCalonMahasiswa calon mahasiswa target; {@code tahun} dan {@code prodi1}-nya
	 *                           menentukan bagian awal nomor
	 * @return nomor registrasi baru yang belum pernah dipakai
	 */
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Integer tahun = biodataCalonMahasiswa.getTahun();

		String p = biodataCalonMahasiswa.getProdi1() == null ? "_" : biodataCalonMahasiswa.getProdi1().getKode();

		Integer jumlahDigit = 3;
		try {
			jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_no_reg_calon_mhs", "3").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/noreg/TAHUN_PRODI_NoRegGenerator.java:31");

		}

		Session session = HibernateUtil.currentSession();
		String prefix = tahun + "" + p;
		long nomorUrut = NoRegGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahDigit,
				biodataCalonMahasiswa, jumlahPengecualian);
		String nomorurutData = NoRegGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

		System.out.println("digit pertama (kode tahun) = " + tahun);
		System.out.println("digit kedua (kode prodi) = " + p);
		System.out.println("digit keempat (kode nomorurutData) = " + nomorurutData);

		String noReg = prefix + nomorurutData;

		boolean nomorSudahDipakai = NoRegGeneratorSupport.nomorSudahDipakai(session, noReg, biodataCalonMahasiswa);
		if (nomorSudahDipakai) {
			jumlahPengecualian.add(noReg);
			return generateNoReg(jumlahPengecualian, biodataCalonMahasiswa);
		}

		return noReg;
	}

}
