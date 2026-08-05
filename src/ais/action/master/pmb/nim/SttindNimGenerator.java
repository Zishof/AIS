package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;

public class SttindNimGenerator implements NimGenerator {

	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		Integer tahun = calonMahasiswa.getTahun();
		String digitPertama = tahun.toString().substring(2);

		String digitKedua = calonMahasiswa == null || calonMahasiswa.getProdiLulus() == null ? "-"
				: calonMahasiswa.getProdiLulus().getKode();

		digitKedua = "100" + digitKedua;

		Session session = HibernateUtil.currentNativeSession();
		Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
				.add(Restrictions.eq("tahunangkatan", tahun))
				.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
						.longValue();

		jumlah += jumlahPengecualian.size();
		String digitKetiga = "000000000000" + (jumlah + 1);
		digitKetiga = digitKetiga.substring(digitKetiga.length() - 3);

		System.out.println("digit pertama (kode tahun) = " + digitPertama);
		System.out.println("digit kedua (kode prodi) = " + digitKedua);
		System.out.println("digit ketiga (urutan) = " + digitKetiga);

		String nim = digitPertama + digitKedua + digitKetiga;

		Integer count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
				.setProjection(Projections.count("nim")).uniqueResult()).intValue();

		HibernateUtil.closeSession();

		if (!count.equals(0)) {
			jumlahPengecualian.add(nim);
			return generateNim(calonMahasiswa, jumlahPengecualian);
		}

		return nim;
	}

}
