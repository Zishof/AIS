package ais.action.master.recruitment.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.recruitment.CalonPegawai;

public class DefaultNisGenerator implements NisGenerator {

	private static final int MAX_ATTEMPT = 10000;

	@Override
	public String generateNis(CalonPegawai calonPegawai) {
		return generateNis(calonPegawai, new ArrayList<String>());
	}

	@Override
	public String generateNis(CalonPegawai calonPegawai, List<String> jumlahPengecualian) {
		List<String> pengecualian = jumlahPengecualian == null ? new ArrayList<String>() : jumlahPengecualian;
		Session session = HibernateUtil.currentNativeSession();
		try {
			Number jumlahData = (Number) session.createCriteria(Pegawai.class).setProjection(Projections.rowCount())
					.setMaxResults(1).uniqueResult();
			long jumlah = jumlahData == null ? 0L : jumlahData.longValue();
			for (int attempt = 0; attempt < MAX_ATTEMPT; attempt++) {
				String nomorInduk = formatNomor(jumlah + pengecualian.size() + attempt + 1, 5);
				if (pengecualian.contains(nomorInduk)) {
					continue;
				}
				Number count = (Number) session.createCriteria(Pegawai.class).add(Restrictions.eq("code", nomorInduk))
						.setProjection(Projections.count("code")).uniqueResult();
				if (count == null || count.intValue() == 0) {
					return nomorInduk;
				}
				pengecualian.add(nomorInduk);
			}
			return formatNomor(jumlah + pengecualian.size() + 1, 5);
		} finally {
			HibernateUtil.closeSession();
		}
	}

	private String formatNomor(long nomor, int digit) {
		String hasil = "00000000000000000000" + nomor;
		return hasil.substring(hasil.length() - digit);
	}
}
