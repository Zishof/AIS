package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;

public class STAINULampungUtara implements NimGenerator {

	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		if (calonMahasiswa.getProdiLulus() == null) {
			return "";
		}

		Integer tahun = calonMahasiswa.getTahun();
		String digitPertama = tahun.toString().substring(2);

		String kodeIain = calonMahasiswa == null || calonMahasiswa.getProdiLulus() == null ? "--"
				: calonMahasiswa.getProdiLulus().getKode().trim();//
		String digitKetiga = calonMahasiswa.getProdiLulus().getFakultas().getPerguruanTinggi().getKodePerguruanTinggi();

		Session session = HibernateUtil.currentNativeSession();
		Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
				.add(Restrictions.eq("tahunangkatan", tahun))
				.add(Restrictions.eq("jenjang", calonMahasiswa.getJenjang()))
				.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
						.longValue();

		jumlah += jumlahPengecualian.size();
		String digitKeempat = "000000000000" + (jumlah + 1);
		digitKeempat = digitKeempat.substring(digitKeempat.length() - 3);

		System.out.println("digit pertama (kode prodi) = " + digitPertama);
		// System.out.println("digit kedua (kode tahun) = " + digitKedua);
		System.out.println("digit kedua (kode prodi) = " + kodeIain);
		System.out.println("digit ketiga (kode institusi) = " + digitKetiga);
		System.out.println("digit keempat (urutan) = " + digitKeempat);

		String nim = digitPertama + /* digitKedua + */ kodeIain + digitKetiga + digitKeempat;

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
