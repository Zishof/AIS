package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

public class PrefixNoRegGenerator implements NoRegGenerator {

	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	// generate NIM
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
		Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", tahun))
				.add(Restrictions.ilike("noRegistrasi", digitPertama, MatchMode.START)).setMaxResults(1).uniqueResult())
				.longValue();

		jumlah += jumlahPengecualian.size();
		String digitKedua = "000000000000000" + (jumlah + 1);
		digitKedua = digitKedua.substring(digitKedua.length() - jumlahDigit);

		System.out.println("digit pertama (kode prefix) = " + digitPertama);
		System.out.println("digit kedua (kode urutan) = " + digitKedua);

		String noReg = digitPertama + digitKedua;

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
