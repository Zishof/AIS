package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.epsbed.KapasitasMahasiswaBaru;

public class AakBorneoLestariNimGenerator implements NimGenerator {

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
			KapasitasMahasiswaBaru kapasitasMahasiswaBaru = (KapasitasMahasiswaBaru) session
					.createCriteria(KapasitasMahasiswaBaru.class)
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus()))
					.add(Restrictions.eq("tahunAkademik", calonMahasiswa.getTahunAkademik()))
					.add(Restrictions.eq("ganjilGenap", calonMahasiswa.getSemesterMulai())).addOrder(Order.desc("id"))
					.setMaxResults(1).uniqueResult();

			String tahunAngkatan = "0";
			if (kapasitasMahasiswaBaru != null) {
				tahunAngkatan = kapasitasMahasiswaBaru.getAngkatanKe().toString();
			} else {
				kapasitasMahasiswaBaru = (KapasitasMahasiswaBaru) session.createCriteria(KapasitasMahasiswaBaru.class)
						.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus()))
						.addOrder(Order.desc("tahunAkademik")).setMaxResults(1).uniqueResult();
				if (kapasitasMahasiswaBaru != null) {
					Integer tahun = Integer.parseInt(kapasitasMahasiswaBaru.getTahunAkademik().split("/")[0]);
					Integer currtahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
					int selisih = currtahun - tahun;
					int angkatanKe = kapasitasMahasiswaBaru.getAngkatanKe() + selisih;
					tahunAngkatan = angkatanKe + "";
				}
			}

			Integer tahun = calonMahasiswa.getTahun();
			String digitKedua = tahun.toString().substring(2);

			String digitPertama = calonMahasiswa.getProdiLulus().getKode() + tahunAngkatan;

			String digitKetiga = "2";
			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setProjection(Projections.rowCount())
					.add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
							.longValue();
			jumlah += jumlahPengecualian.size();
			String digitKeempat = "000000000000" + (jumlah + 1);
			digitKeempat = digitKeempat.substring(digitKeempat.length() - 3);

			System.out.println("digit pertama (kode angkatan) = " + digitPertama);
			System.out.println("digit kedua (kode tahun masuk) = " + digitKedua);
			System.out.println("digit ketiga (identitas S1) = " + digitKetiga);
			System.out.println("digit keempat (urutan) = " + digitKeempat);

			nim = digitPertama + digitKedua + digitKetiga + digitKeempat;

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
