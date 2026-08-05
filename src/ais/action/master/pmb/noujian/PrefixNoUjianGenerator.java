package ais.action.master.pmb.noujian;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

public class PrefixNoUjianGenerator implements NoUjianGenerator {

	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		return generateNoUjian(biodataCalonMahasiswa, new ArrayList<String>());
	}

	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> noRegPengecualian)
			throws Exception {
		Integer tahun = biodataCalonMahasiswa.getTahun();
		String digitPertama = Common.getKonfigurasi("prefix_no_ujian_calon_mhs", "EXAM.").getNilai();
		Integer jumlahDigit = 3;
		try {
			jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_no_ujian_calon_mhs", "3").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/noujian/PrefixNoUjianGenerator.java:30");

		}

		Session session = HibernateUtil.currentSession();
		Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).setProjection(Projections.rowCount())
				.add(Restrictions.eq("tahun", tahun)).add(Restrictions.ilike("noUjian", digitPertama, MatchMode.START))
				.setMaxResults(1).uniqueResult()).longValue();

		jumlah += noRegPengecualian.size();
		String digitKedua = "000000000000000" + (jumlah + 1);
		digitKedua = digitKedua.substring(digitKedua.length() - jumlahDigit);

		System.out.println("digit pertama (kode prefix) = " + digitPertama);
		System.out.println("digit kedua (kode urutan) = " + digitKedua);

		String noReg = digitPertama + digitKedua;

		Integer count = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("noUjian", noReg)).setProjection(Projections.rowCount()).uniqueResult())
						.intValue();
		if (!count.equals(0)) {
			noRegPengecualian.add(noReg);
			return generateNoUjian(biodataCalonMahasiswa, noRegPengecualian);
		}

		return noReg;
	}

}
