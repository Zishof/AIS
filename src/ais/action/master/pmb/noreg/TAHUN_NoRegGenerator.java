package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Pembangkit Nomor Registrasi (No. Reg) calon mahasiswa dengan format {@code TAHUN+URUT}, mis.
 * {@code "20260007"}. Bagian {@code URUT} dihitung dari jumlah calon mahasiswa aktif yang sudah
 * terdaftar pada tahun dan program studi (prodi1) yang sama, dipadatkan sejumlah digit sesuai
 * konfigurasi {@code jumlah_digit_no_reg_calon_mhs} (default 4). Bila nomor hasil bentrok dengan
 * yang sudah tersimpan, dibangkitkan ulang secara rekursif dengan nomor tersebut ditambahkan ke
 * daftar pengecualian.
 */
public class TAHUN_NoRegGenerator implements NoRegGenerator {

	/** Membangkitkan nomor registrasi baru untuk {@code biodataCalonMahasiswa} tanpa daftar pengecualian awal. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Membangkitkan nomor registrasi berformat {@code TAHUN+URUT}, menghindari nomor pada
	 * {@code jumlahPengecualian} maupun yang sudah tersimpan; mengulang secara rekursif bila
	 * terjadi bentrok.
	 *
	 * @param jumlahPengecualian    daftar nomor registrasi yang harus dihindari, diperbarui di
	 *                              tempat saat terjadi bentrok
	 * @param biodataCalonMahasiswa data calon mahasiswa yang akan diberi nomor registrasi
	 * @return nomor registrasi baru yang belum dipakai
	 */
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Integer tahun = biodataCalonMahasiswa.getTahun();

		Integer jumlahDigit = 4;
		try {
			jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_no_reg_calon_mhs", "4").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/noreg/TAHUN_NoRegGenerator.java:29");

		}

		Session session = HibernateUtil.currentSession();
		String prefix = tahun + "";
		long nomorUrut = NoRegGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahDigit,
				biodataCalonMahasiswa, jumlahPengecualian);
		String nomorurutData = NoRegGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

		System.out.println("digit pertama (kode tahun) = " + tahun);
		System.out.println("digit kedua (kode nomorurutData) = " + nomorurutData);

		String noReg = prefix + nomorurutData;

		boolean nomorSudahDipakai = NoRegGeneratorSupport.nomorSudahDipakai(session, noReg, biodataCalonMahasiswa);
		if (nomorSudahDipakai) {
			jumlahPengecualian.add(noReg);
			return generateNoReg(jumlahPengecualian, biodataCalonMahasiswa);
		}

		return noReg;
	}

}
