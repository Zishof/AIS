package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

public class DefaultNoRegGenerator implements NoRegGenerator {

	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	// generate NIM
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Session session = HibernateUtil.currentSession();
		Number number = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.max("id")).uniqueResult();

		if (number == null) {
			number = 0;
		} else {
			number = number.longValue() + (jumlahPengecualian.size() + 1);
		}
		String kodeRegistratsi = "00000000000000000000000000000000000000" + number.longValue();

		Integer jumlahIncrements = 8;
		try {
			jumlahIncrements = Integer
					.parseInt(Common.getKonfigurasi("jumlah_increments_no_registrasi_pmb", "8").getNilai());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}

		String noreg = (biodataCalonMahasiswa.getTahun() + "")
				+ kodeRegistratsi.substring(kodeRegistratsi.length() - jumlahIncrements, kodeRegistratsi.length());

		Integer count = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("noRegistrasi", noreg)).setProjection(Projections.rowCount()).uniqueResult())
						.intValue();

		if (!count.equals(0)) {
			jumlahPengecualian.add(noreg);
			return generateNoReg(jumlahPengecualian, biodataCalonMahasiswa);
		} else {
			return noreg;
		}
	}

}
