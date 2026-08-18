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

public class IsmNimGenerator implements NimGenerator {

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

			String digitKedua = calonMahasiswa.getMerupakanPindahan() ? "2" : "1";

			String digitKetiga = calonMahasiswa.getProdiLulus().getJenjang().getNama().equalsIgnoreCase("S1") ? "2"
					: calonMahasiswa.getProdiLulus().getJenjang().getNama().equalsIgnoreCase("S2") ? "1" : "-";

			// String digitKetiga =
			// calonMahasiswa.getProdiLulus().getFakultas().getKode();

			String digitKeempat = calonMahasiswa.getProdiLulus().getKode();

			String digitKelima = calonMahasiswa.getProgram().equalsIgnoreCase("Reguler")
					|| calonMahasiswa.getProgram().equalsIgnoreCase("Pascasarjana")
							? "1"
							: calonMahasiswa.getProgram().equalsIgnoreCase("Ekstensi") ? "2"
									: calonMahasiswa.getProgram().equalsIgnoreCase("Karyawan") ? "3"
											: calonMahasiswa.getProgram().equalsIgnoreCase("Internasional") ? "4" : "5";

			String digitKeEnam = calonMahasiswa.getJenisSemester().equals(Perkuliahan.GANJIL) ? "1" : "2";

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("program", calonMahasiswa.getProgram()))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();

			jumlah += jumlahPengecualian.size();
			String digitTujuh = "000000000000" + (jumlah + 1);
			digitTujuh = digitTujuh.substring(digitTujuh.length() - 3);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (pindahan atau bukan) = " + digitKedua);
			System.out.println("digit ketiga (kode jenjang) = " + digitKetiga);
			System.out.println("digit keempat (kode prodi) = " + digitKeempat);
			System.out.println("digit kelima (kode program) = " + digitKelima);
			System.out.println("digit keenam (ganjil genap) = " + digitKeEnam);
			System.out.println("digit keenam (nomor urut) = " + digitTujuh);

			nim = "4" + digitPertama + digitKedua + digitKetiga + digitKeempat + digitKelima + digitKeEnam + digitTujuh;

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
