package ais.action.master.sekolah.psb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

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
		Session session = HibernateUtil.currentNativeSession();
		Number number = (Number) session.createCriteria(CalonSiswa.class)
				.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).setProjection(Projections.max("id"))
				.uniqueResult();

		if (number == null) {
			number = 0;
		} else {
			number = number.longValue() + (jumlahPengecualian.size() + 1);
		}
		String kodeRegistratsi = "00000000000000000000000000000000000000" + number.longValue();

		Integer jumlahIncrements = 5;
		try {
			jumlahIncrements = Integer
					.parseInt(Common.getKonfigurasi("jumlah_increments_no_registrasi_psb", "5").getNilai());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		String noreg = (calonSiswa.getTahunMasuk() + "")
				+ kodeRegistratsi.substring(kodeRegistratsi.length() - jumlahIncrements, kodeRegistratsi.length());

		Integer count = ((Number) session.createCriteria(CalonSiswa.class).add(Restrictions.eq("nomorInduk", noreg))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		if (count > 0) {
			jumlahPengecualian.add(noreg);
			return generateNoReg(jumlahPengecualian, calonSiswa);
		} else {
			return noreg;
		}
	}

}
