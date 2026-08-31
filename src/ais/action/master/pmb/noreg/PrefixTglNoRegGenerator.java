package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma penomoran Nomor Registrasi calon mahasiswa berpola
 * {@code <kode jenis seleksi><tanggal daftar><kode prodi pilihan 1><urut>}: kode lain
 * {@link ais.database.model.JenisSeleksi} (bila ada) diikuti tanggal pendaftaran (format
 * {@code Common.dateFormat81}), kode lain prodi pilihan pertama, dan nomor urut (jumlah digit dari
 * konfigurasi {@code jumlah_digit_no_reg_calon_mhs}, default 3). Nomor urut dan pengecekan
 * duplikasi didelegasikan ke {@link NoRegGeneratorSupport}; bila nomor hasil sudah terpakai, dicoba
 * ulang secara rekursif dengan nomor tersebut ditambahkan ke daftar pengecualian.
 *
 * @see NoRegGeneratorSupport
 */
public class PrefixTglNoRegGenerator implements NoRegGenerator {

	/** Varian ringkas {@link #generateNoReg(List, BiodataCalonMahasiswa)} tanpa daftar pengecualian awal. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Menghasilkan nomor registrasi untuk {@code biodataCalonMahasiswa} sesuai pola kelas ini (lihat
	 * javadoc kelas). Rekursif: bila nomor yang dihasilkan ternyata sudah dipakai, dicoba lagi
	 * dengan nomor tersebut ditambahkan ke {@code jumlahPengecualian}.
	 *
	 * @param jumlahPengecualian daftar nomor registrasi yang harus dianggap sudah terpakai (dimutasi dan diteruskan pada percobaan ulang)
	 * @return nomor registrasi yang dihasilkan
	 */
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Integer tahun = biodataCalonMahasiswa.getTahun();
		String digitPertama = biodataCalonMahasiswa.getJenisSeleksi() == null ? ""
				: biodataCalonMahasiswa.getJenisSeleksi().getKodeLain();
		Integer jumlahDigit = 3;
		try {
			jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_no_reg_calon_mhs", "3").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/noreg/PrefixTglNoRegGenerator.java:30");

		}

		String tgl = Common.dateFormat81.get().format(biodataCalonMahasiswa.getTanggalDaftar());

		String digitKetiga = biodataCalonMahasiswa.getProdi1() == null ? ""
				: biodataCalonMahasiswa.getProdi1().getKodeLain();

		Session session = HibernateUtil.currentSession();
		String prefix = digitPertama + tgl + digitKetiga;
		long nomorUrut = NoRegGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahDigit,
				biodataCalonMahasiswa, jumlahPengecualian);
		String digitKedua = NoRegGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

		System.out.println("digit pertama (kode jalur) = " + digitPertama);
		System.out.println("digit kedua (kode tgl) = " + tgl);
		System.out.println("digit kedua (kode prodi 1) = " + digitKetiga);
		System.out.println("digit  (kode urutan) = " + digitKedua);

		String noReg = prefix + digitKedua;

		boolean nomorSudahDipakai = NoRegGeneratorSupport.nomorSudahDipakai(session, noReg, biodataCalonMahasiswa);
		if (nomorSudahDipakai) {
			jumlahPengecualian.add(noReg);
			return generateNoReg(jumlahPengecualian, biodataCalonMahasiswa);
		}

		return noReg;
	}

}
