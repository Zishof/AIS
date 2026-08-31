package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
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
			String prefix = digitKedua + digitKetiga + "01";
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, 3, calonMahasiswa,
					jumlahPengecualian);
			String digitKeempat = NimGeneratorSupport.leftPadNomor(nomorUrut, 3);

			String nim = prefix + digitKeempat;

			boolean nimSudahDipakai = NimGeneratorSupport.nimSudahDipakai(session, nim, calonMahasiswa);
			HibernateUtil.closeSessionQuietly(session);

			if (nimSudahDipakai) {
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
			String prefix = digitPertama + digitKedua + digitKetiga;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, 4, calonMahasiswa,
					jumlahPengecualian);
			String digitKeempat = NimGeneratorSupport.leftPadNomor(nomorUrut, 4);

			System.out.println("digit pertama (kode prodi) = " + digitPertama);
			System.out.println("digit kedua (kode tahun) = " + digitKedua);
			System.out.println("digit ketiga (tahun sememster) = " + digitKetiga);
			System.out.println("digit keempat (urutan) = " + digitKeempat);

			String nim = prefix + digitKeempat;

			boolean nimSudahDipakai = NimGeneratorSupport.nimSudahDipakai(session, nim, calonMahasiswa);
			HibernateUtil.closeSessionQuietly(session);

			if (nimSudahDipakai) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

			return nim;
		}
	}

}
