package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;

public class PelitaBangsaNimGenerator implements NimGenerator {

	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		if (calonMahasiswa.getProdiLulus() == null) {
			return "-";
		} else if (calonMahasiswa.getProdiLulus().getKode().trim().equals("006")) {
			Integer tahun = calonMahasiswa.getTahun();
			Integer tahunberikut = calonMahasiswa.getTahun() + 1;

			String digitKedua = tahun.toString().substring(2);
			String digitKetiga = tahunberikut.toString().substring(2);

			Session session = HibernateUtil.openSession();
			String maxNim = ((String) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.max("nim"))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus()))
					.add(Restrictions.eq("tahunangkatan", tahun)).setMaxResults(1).uniqueResult());

			Integer n = Integer.parseInt(maxNim == null ? "0" : maxNim.trim().substring(maxNim.length() - 3));

			n += jumlahPengecualian.size();
			String digitKeempat = "000000000000" + (n + 1);
			digitKeempat = digitKeempat.substring(digitKeempat.length() - 3);

			String nim = digitKedua + digitKetiga + "01" + digitKeempat;

			Integer count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
					.setProjection(Projections.count("nim")).uniqueResult()).intValue();
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSessionQuietly(session);

			if (!count.equals(0)) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

			return nim;

		} else {

			String digitPertama = calonMahasiswa == null || calonMahasiswa.getProdiLulus() == null ? "--"
					: calonMahasiswa.getProdiLulus().getKode().trim();

			Integer tahun = calonMahasiswa.getTahun();
			String digitKedua = tahun.toString().substring(2);

			String digitKetiga = calonMahasiswa == null ? "-"
					: calonMahasiswa.getJenisSemester().equals(Perkuliahan.GANJIL) ? "1" : "2";

			Session session = HibernateUtil.openSession();
			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.setProjection(Projections.rowCount()).add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jenjang", calonMahasiswa.getJenjang()))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();

			jumlah += jumlahPengecualian.size();
			String digitKeempat = "000000000000" + (jumlah + 1);
			digitKeempat = digitKeempat.substring(digitKeempat.length() - 4);

			System.out.println("digit pertama (kode prodi) = " + digitPertama);
			System.out.println("digit kedua (kode tahun) = " + digitKedua);
			System.out.println("digit ketiga (tahun sememster) = " + digitKetiga);
			System.out.println("digit keempat (urutan) = " + digitKeempat);

			String nim = digitPertama + digitKedua + digitKetiga + digitKeempat;

			Integer count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
					.setProjection(Projections.count("nim")).uniqueResult()).intValue();
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSessionQuietly(session);

			if (!count.equals(0)) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

			return nim;
		}
	}

}
