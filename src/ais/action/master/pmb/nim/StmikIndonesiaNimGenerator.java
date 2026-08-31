package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;

public class StmikIndonesiaNimGenerator implements NimGenerator {

	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			if (calonMahasiswa.getMerupakanPindahan()) {
				Integer tahun = calonMahasiswa.getTahun();

				String digitPertama = calonMahasiswa.getProdiLulus().getKode();

				String digitKedua = tahun.toString().substring(2);

				Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
						.add(Restrictions.eq("tahunangkatan", tahun))
						.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus()))
						.createAlias("statusAwalMahasiswa", "statusAwalMahasiswa")
						.add(Restrictions.eq("statusAwalMahasiswa.pindahan", true)).setMaxResults(1).uniqueResult())
								.longValue();

				String digitKetiga = "9";

				jumlah += jumlahPengecualian.size();
				String digitEmpat = "000000000000" + (jumlah + 1);
				digitEmpat = digitEmpat.substring(digitEmpat.length() - 2);

				System.out.println("digit pertama (kode prodi) = " + digitPertama);
				System.out.println("digit kedua (kode tahun masuk) = " + digitKedua);
				System.out.println("digit ketiga (kode pindahan) = " + digitKetiga);
				System.out.println("digit kempat (urutan) = " + digitEmpat);

				nim = digitPertama + digitKedua + digitKetiga + digitEmpat;

				Integer count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
						.setProjection(Projections.count("nim")).uniqueResult()).intValue();

				HibernateUtil.closeSessionQuietly(session);

				if (!count.equals(0)) {
					jumlahPengecualian.add(nim);
					return generateNim(calonMahasiswa, jumlahPengecualian);
				}
			} else {

				Integer tahun = calonMahasiswa.getTahun();

				String digitPertama = calonMahasiswa.getProdiLulus().getKode();

				String digitKedua = tahun.toString().substring(2);

				Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
						.add(Restrictions.eq("tahunangkatan", tahun))
						.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1)
						.uniqueResult()).longValue();

				jumlah += jumlahPengecualian.size();
				String digitKetiga = "000000000000" + (jumlah + 1);
				digitKetiga = digitKetiga.substring(digitKetiga.length() - 3);

				System.out.println("digit pertama (kode prodi) = " + digitPertama);
				System.out.println("digit kedua (kode tahun masuk) = " + digitKedua);
				System.out.println("digit ketiga (urutan) = " + digitKetiga);

				nim = digitPertama + digitKedua + digitKetiga;

				Integer count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
						.setProjection(Projections.count("nim")).uniqueResult()).intValue();

				HibernateUtil.closeSessionQuietly(session);

				if (!count.equals(0)) {
					jumlahPengecualian.add(nim);
					return generateNim(calonMahasiswa, jumlahPengecualian);
				}
			}
		}

		return nim;
	}

}
