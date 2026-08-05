package ais.action.master.recruitment.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.recruitment.CalonPegawai;

public class DefaultNoRegGeneratorPegawai implements NoRegGeneratorPegawai {

	private static final int MAX_ATTEMPT = 10000;

	@Override
	public String generateNoReg(CalonPegawai calonPegawai) {
		return generateNoReg(new ArrayList<String>(), calonPegawai);
	}

	@Override
	public String generateNoReg(List<String> jumlahPengecualian, CalonPegawai calonPegawai) {
		List<String> pengecualian = jumlahPengecualian == null ? new ArrayList<String>() : jumlahPengecualian;
		Session session = HibernateUtil.currentNativeSession();
		try {
			Number maxId = (Number) session.createCriteria(CalonPegawai.class).setProjection(Projections.max("id"))
					.uniqueResult();
			long dasar = maxId == null ? 0L : maxId.longValue();
			int digit = ambilJumlahDigit();
			for (int attempt = 0; attempt < MAX_ATTEMPT; attempt++) {
				String noReg = formatNomor(dasar + pengecualian.size() + attempt + 1, digit);
				if (pengecualian.contains(noReg)) {
					continue;
				}
				Number count = (Number) session.createCriteria(CalonPegawai.class)
						.add(Restrictions.eq("nomorInduk", noReg)).setProjection(Projections.rowCount()).uniqueResult();
				if (count == null || count.intValue() == 0) {
					return noReg;
				}
				pengecualian.add(noReg);
			}
			return formatNomor(dasar + pengecualian.size() + 1, digit);
		} finally {
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	private int ambilJumlahDigit() {
		try {
			return Integer.parseInt(Common.getKonfigurasi("jumlah_increments_no_registrasi_pegawai", "5").getNilai());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 5;
		}
	}

	private String formatNomor(long nomor, int digit) {
		String hasil = "00000000000000000000000000000000000000" + nomor;
		return hasil.substring(hasil.length() - digit);
	}
}
