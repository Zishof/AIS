package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;

public class YY_JENJANG_PRODI_STATUS_URUT_NimGenerator implements NimGenerator {

	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.currentNativeSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = tahun.toString().substring(2);
			String digitKedua = calonMahasiswa.getProdiLulus().getJenjang().getKode();
			String digitKetiga = calonMahasiswa.getProdiLulus().getKode();

			String digitKetigaLagi = calonMahasiswa.getMerupakanPindahan() ? "2" : "1";

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("merupakanPindahan", calonMahasiswa.getMerupakanPindahan()))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
					.longValue();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YY_JENJANG_PRODI_STATUS_URUT_NimGenerator.java:48");

			}

			jumlah += jumlahPengecualian.size();
			String digitEmpat = "000000000000" + (jumlah + 1);
			digitEmpat = digitEmpat.substring(digitEmpat.length() - jumlahDigit);

			System.out.println("pindahan = " + calonMahasiswa.getMerupakanPindahan() + " jumlah " + jumlah
					+ " jumlahPengecualian " + jumlahPengecualian);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode jenjang) = " + digitKedua);
			System.out.println("digit kedua (kode prodi) = " + digitKetiga);
			System.out.println("digit kedua (status) = " + digitKetigaLagi);
			System.out.println("digit ketiga (urutan) = " + digitEmpat);

			nim = digitPertama + digitKedua + digitKetiga + digitKetigaLagi + digitEmpat;

			Integer count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
					.setProjection(Projections.count("nim")).uniqueResult()).intValue();

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();

			if (!count.equals(0)) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

		}

		return nim;
	}

}
