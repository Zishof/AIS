package ais.action.master.sekolah.psb.nis;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;

public class KssnNisGenerator implements NisGenerator {

	@Override
	public String generateNis(CalonSiswa calonSiswa) {
		return generateNis(calonSiswa, new ArrayList<String>());
	}

	@Override
	public String generateNis(CalonSiswa calonSiswa, List<String> jumlahPengecualian) {

		Integer tahun = calonSiswa.getTahunMasuk();

		Session session = HibernateUtil.currentNativeSession();
		Long jumlah = ((Number) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
				.add(Restrictions.eq("sekolah", calonSiswa.getSekolah())).setProjection(Projections.rowCount())
				.add(Restrictions.eq("tahunMasuk", tahun)).setMaxResults(1).uniqueResult()).longValue();

		jumlah += jumlahPengecualian.size();
		String digitKesembilandst = "000000000000" + (jumlah + 1);
		digitKesembilandst = digitKesembilandst.substring(digitKesembilandst.length() - 3);

		String nomorInduk = calonSiswa.getSekolah().getNss() + digitKesembilandst;

		Integer count = ((Number) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorInduk", nomorInduk))
				.setProjection(Projections.count("nomorInduk")).uniqueResult()).intValue();

		HibernateUtil.closeSession();

		if (!count.equals(0)) {
			jumlahPengecualian.add(nomorInduk);
			return generateNis(calonSiswa, jumlahPengecualian);
		}

		return nomorInduk;
	}

}
