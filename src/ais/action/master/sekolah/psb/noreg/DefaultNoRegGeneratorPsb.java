package ais.action.master.sekolah.psb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;

public class DefaultNoRegGeneratorPsb implements NoRegGeneratorPsb {

	@Override
	public String generateNoReg(CalonSiswa calonSiswa) {
		return generateNoReg(new ArrayList<String>(), calonSiswa);
	}

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
