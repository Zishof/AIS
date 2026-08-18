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

public class NiitNimGenerator implements NimGenerator {

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

			String digitKedua = tahun.toString().substring(2);

			// String digitKetiga =
			// calonMahasiswa.getProdiLulus().getFakultas().getKode();

			String digitKeempat = "0";
			if (calonMahasiswa.getMerupakanPindahan()) {
				if (calonMahasiswa.getPindahanDariKampus().equalsIgnoreCase("ccit")) {
					if (calonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL)) {
						digitKeempat = "5";
					} else {
						digitKeempat = "6";
					}
				} else {
					if (calonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL)) {
						digitKeempat = "3";
					} else {
						digitKeempat = "4";
					}
				}
			} else {
				if (calonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL)) {
					digitKeempat = "1";
				} else {
					digitKeempat = "2";
				}
			}

			String digitKelima = calonMahasiswa.getProgram().equalsIgnoreCase("Reguler") ? "0" : "1";

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();

			jumlah += jumlahPengecualian.size();
			String digitTujuh = "000000000000" + (jumlah + 1);
			digitTujuh = digitTujuh.substring(digitTujuh.length() - 3);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode strata) = " + digitKedua);
			// System.out.println("digit ketiga (kode fakultas) = " +
			// digitKetiga);
			System.out.println("digit keempat (kode prodi) = " + digitKeempat);
			System.out.println("digit kelima (kode program) = " + digitKelima);
			System.out.println("digit keenam (nomor urut) = " + digitTujuh);

			nim = digitPertama + digitKedua + digitKeempat + digitKelima + digitTujuh;

			Integer count = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
					.setProjection(Projections.count("nim")).uniqueResult()).intValue();

			HibernateUtil.closeSession();

			if (!count.equals(0)) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

		}

		return nim;
	}

}
