package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

public class PrefixTglNoRegGenerator implements NoRegGenerator {

	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	// generate NIM
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Integer tahun = biodataCalonMahasiswa.getTahun();
		String digitPertama = biodataCalonMahasiswa.getJenisSeleksi() == null ? ""
				: biodataCalonMahasiswa.getJenisSeleksi().getKodeLain();
		Integer jumlahDigit = 3;
		try {
			jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_no_reg_calon_mhs", "3").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/noreg/PrefixTglNoRegGenerator.java:30");

		}

		String tgl = Common.dateFormat81.get().format(biodataCalonMahasiswa.getTanggalDaftar());

		String digitKetiga = biodataCalonMahasiswa.getProdi1() == null ? ""
				: biodataCalonMahasiswa.getProdi1().getKodeLain();

		Session session = HibernateUtil.currentSession();
		Long jumlah = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", tahun)).setMaxResults(1)
				.uniqueResult()).longValue();

		jumlah += jumlahPengecualian.size();
		String digitKedua = "000000000000000" + (jumlah + 1);
		digitKedua = digitKedua.substring(digitKedua.length() - jumlahDigit);

		System.out.println("digit pertama (kode jalur) = " + digitPertama);
		System.out.println("digit kedua (kode tgl) = " + tgl);
		System.out.println("digit kedua (kode prodi 1) = " + digitKetiga);
		System.out.println("digit  (kode urutan) = " + digitKedua);

		String noReg = digitPertama + tgl + digitKetiga + digitKedua;

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
