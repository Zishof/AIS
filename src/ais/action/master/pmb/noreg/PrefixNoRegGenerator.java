package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma nomor registrasi PMB pola umum "Prefix": {@code [prefix konfigurasi][N digit nomor
 * urut]}, mis. dengan prefix default {@code "REG."} menghasilkan {@code REG.001}. Prefix diambil
 * dari konfigurasi {@code prefix_no_reg_calon_mhs} (default {@code "REG."}) dan jumlah digit nomor
 * urut dari {@code jumlah_digit_no_reg_calon_mhs} (default 3), sehingga dapat disesuaikan per
 * tenant tanpa mengubah kode. Tahun pendaftaran diambil dari bagian awal
 * {@code biodataCalonMahasiswa.getTahunAkademik()} (format {@code "YYYY/YYYY"}, dipisah {@code /});
 * bila gagal diparsing, jatuh kembali ke {@code getTahun()}. Nomor urut dihitung dari jumlah
 * {@link BiodataCalonMahasiswa} aktif pada tahun yang sama yang nomor registrasinya sudah
 * berawalan prefix tersebut, ditambah jumlah kandidat yang sudah dicoba tapi bentrok pada
 * pemanggilan rekursif, lalu ditambah 1 dan dipad nol ke kiri. Bila hasil gabungan ternyata sudah
 * dipakai calon mahasiswa aktif lain, nomor tersebut dicatat sebagai pengecualian dan method
 * memanggil dirinya sendiri untuk mencoba nomor berikutnya. Bukan spesifik satu institusi — dipakai
 * sebagai pola default berbasis prefix yang dapat dikonfigurasi.
 */
public class PrefixNoRegGenerator implements NoRegGenerator {

	/** Menghasilkan nomor registrasi baru tanpa daftar pengecualian awal — lihat {@link #generateNoReg(List, BiodataCalonMahasiswa)}. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Menghasilkan nomor registrasi berformat {@code [prefix konfigurasi][N digit urut]},
	 * menghindari nomor yang ada di {@code jumlahPengecualian} maupun yang sudah dipakai calon
	 * mahasiswa aktif lain di database; mencoba ulang secara rekursif bila terjadi bentrok.
	 *
	 * @param jumlahPengecualian nomor-nomor yang sudah dicoba dan diketahui bentrok, dihindari pada
	 *                           percobaan berikutnya (diperbarui di tempat)
	 * @param biodataCalonMahasiswa calon mahasiswa target; tahun akademiknya menentukan cakupan
	 *                           penghitungan nomor urut
	 * @return nomor registrasi baru yang belum pernah dipakai
	 */
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Integer tahun = biodataCalonMahasiswa.getTahun();

		try {
			tahun = Integer.parseInt(StringUtils.split(biodataCalonMahasiswa.getTahunAkademik(), "/")[0].trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/noreg/PrefixNoRegGenerator.java:30");
			// Common.tampilErrorJikaAdmin(e);
		}

		String digitPertama = Common.getKonfigurasi("prefix_no_reg_calon_mhs", "REG.").getNilai();
		Integer jumlahDigit = 3;
		try {
			jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_no_reg_calon_mhs", "3").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/noreg/PrefixNoRegGenerator.java:38");

		}

		Session session = HibernateUtil.currentSession();
		long nomorUrut = NoRegGeneratorSupport.nomorUrutBerikutnya(session, digitPertama, jumlahDigit,
				biodataCalonMahasiswa, jumlahPengecualian);
		String digitKedua = NoRegGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

		System.out.println("digit pertama (kode prefix) = " + digitPertama);
		System.out.println("digit kedua (kode urutan) = " + digitKedua);

		String noReg = digitPertama + digitKedua;

		boolean nomorSudahDipakai = NoRegGeneratorSupport.nomorSudahDipakai(session, noReg, biodataCalonMahasiswa);
		if (nomorSudahDipakai) {
			jumlahPengecualian.add(noReg);
			return generateNoReg(jumlahPengecualian, biodataCalonMahasiswa);
		}

		return noReg;
	}

}
