package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

public class PoltekkesMedanNoRegGenerator implements NoRegGenerator {

	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	// generate NIM
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {

		if (biodataCalonMahasiswa.getGelombangPendaftaran() == null) {
			return "-";
		}

		String digitPertama = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "";

		Session session = HibernateUtil.currentSession();
		Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount())
				.add(Restrictions.ilike("noRegistrasi", digitPertama, MatchMode.START)).setMaxResults(1).uniqueResult())
						.longValue();

		String digitKedua = biodataCalonMahasiswa == null || biodataCalonMahasiswa.getPaket() == null ? "1"
				: biodataCalonMahasiswa.getPaket().getJumlahProdiYgBolehDiambil().toString();

		jumlah += jumlahPengecualian.size();
		String digitKetiga = "000000000000000" + (jumlah + 1);
		digitKetiga = digitKetiga.substring(digitKetiga.length() - 5);

		System.out.println("digit pertama (kode tahun) = " + digitPertama);
		System.out.println("digit kedua (kode jumlah pilihan prodi) = " + digitKedua);
		System.out.println("digit ketiga (kode increment) = " + digitKetiga);

		String noReg = biodataCalonMahasiswa.getGelombangPendaftaran().getKode() + digitPertama + digitKedua
				+ digitKetiga;

		Integer count = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("noRegistrasi", noReg)).setProjection(Projections.rowCount()).uniqueResult())
						.intValue();
		if (!count.equals(0)) {
			jumlahPengecualian.add(noReg);
			return generateNoReg(jumlahPengecualian, biodataCalonMahasiswa);
		}

		return noReg;
	}

}
