package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

public class TAHUN_PRODI_NoRegGenerator implements NoRegGenerator {

	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	// generate NIM
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
		Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", tahun))
				.add(Restrictions.eq("prodi1", biodataCalonMahasiswa.getProdi1())).setMaxResults(1).uniqueResult())
				.longValue();

		jumlah += jumlahPengecualian.size();
		String nomorurutData = "000000000000000" + (jumlah + 1);
		nomorurutData = nomorurutData.substring(nomorurutData.length() - jumlahDigit);

		System.out.println("digit pertama (kode tahun) = " + tahun);
		System.out.println("digit kedua (kode prodi) = " + p);
		System.out.println("digit keempat (kode nomorurutData) = " + nomorurutData);

		String noReg = tahun + "" + p + "" + nomorurutData;

		Integer count = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("noRegistrasi", noReg)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (!count.equals(0)) {
			jumlahPengecualian.add(noReg);
			return generateNoReg(jumlahPengecualian, biodataCalonMahasiswa);
		}

		return noReg;
	}

}
