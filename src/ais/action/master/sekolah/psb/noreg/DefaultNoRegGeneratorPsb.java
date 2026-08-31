package ais.action.master.sekolah.psb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;

/**
 * Algoritma penomoran registrasi (No Reg) default untuk calon siswa pada modul PSB (Penerimaan
 * Siswa Baru): nomor dibentuk dari tahun masuk sebagai prefix, digabung nomor urut berjalan yang
 * di-pad nol sepanjang {@code jumlah_increments_no_registrasi_psb} digit (default 5, dapat
 * dikonfigurasi). Pembangkitan nomor urut dan pengecekan pemakaian didelegasikan ke
 * {@link NoRegGeneratorPsbSupport}, sehingga logika ini dapat dipakai ulang oleh generator lain
 * yang formatnya serupa.
 */
public class DefaultNoRegGeneratorPsb implements NoRegGeneratorPsb {

	/** Seperti {@link #generateNoReg(List, CalonSiswa)}, tanpa daftar pengecualian awal. */
	@Override
	public String generateNoReg(CalonSiswa calonSiswa) {
		return generateNoReg(new ArrayList<String>(), calonSiswa);
	}

	/**
	 * Membangkitkan nomor registrasi PSB: prefix tahun masuk + nomor urut berikutnya (dihitung
	 * lewat {@link NoRegGeneratorPsbSupport#nomorUrutBerikutnya}, memperhitungkan nomor yang sudah
	 * terbukti bentrok di {@code jumlahPengecualian}), di-pad nol sesuai konfigurasi jumlah digit.
	 * Bila nomor hasil ternyata sudah terpakai (dicek via
	 * {@link NoRegGeneratorPsbSupport#nomorSudahDipakai}), nomor tersebut ditambahkan ke
	 * {@code jumlahPengecualian} dan method memanggil dirinya sendiri secara rekursif untuk mencoba
	 * nomor urut berikutnya.
	 *
	 * @param jumlahPengecualian nomor registrasi kandidat yang sudah terbukti bentrok pada percobaan sebelumnya
	 * @return nomor registrasi yang belum dipakai calon siswa manapun
	 */
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, CalonSiswa calonSiswa) {
		Session session = HibernateUtil.openSession();
		Integer jumlahIncrements = 5;
		try {
			jumlahIncrements = Integer
					.parseInt(Common.getKonfigurasi("jumlah_increments_no_registrasi_psb", "5").getNilai());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		String prefix = calonSiswa.getTahunMasuk() + "";
		long nomorUrut = NoRegGeneratorPsbSupport.nomorUrutBerikutnya(session, prefix, jumlahIncrements, calonSiswa,
				jumlahPengecualian);
		String noreg = prefix + NoRegGeneratorPsbSupport.leftPadNomor(nomorUrut, jumlahIncrements);

		boolean nomorSudahDipakai = NoRegGeneratorPsbSupport.nomorSudahDipakai(session, noreg, calonSiswa);
		HibernateUtil.closeSessionQuietly(session);

		if (nomorSudahDipakai) {
			jumlahPengecualian.add(noreg);
			return generateNoReg(jumlahPengecualian, calonSiswa);
		} else {
			return noreg;
		}
	}

}
