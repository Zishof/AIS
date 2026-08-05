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
import ais.database.model.Perkuliahan;

public class PRODI_YY_SMT_URUT_NimGenerator implements NimGenerator {

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

			String digitPertama = calonMahasiswa.getProdiLulus().getKode();
			String digit12 = tahun.toString().substring(2);
			String digitKedua = calonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL) ? "1" : "2";

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("semesterMulai", calonMahasiswa.getSemesterMulai()))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
					.longValue();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/PRODI_YY_SMT_URUT_NimGenerator.java:48");

			}

			jumlah += jumlahPengecualian.size();
			String digitEmpat = "000000000000" + (jumlah + 1);
			digitEmpat = digitEmpat.substring(digitEmpat.length() - jumlahDigit);

			System.out.println("digit 1 (kode prodi)  = " + digitPertama);
			System.out.println("digit 2 (kode tahun masuk) = " + digit12);
			System.out.println("digit 3 (kode semester) = " + digitKedua);
			System.out.println("digit 4 (urutan) = " + digitEmpat);

			nim = digitPertama + digit12 + digitKedua + digitEmpat;

			Integer count = ((Number) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.eq("nim", nim)).setProjection(Projections.count("nim")).uniqueResult())
					.intValue();

			HibernateUtil.closeSession();

			if (!count.equals(0)) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

		}

		return nim;
	}

}
