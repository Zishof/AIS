package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;

public class YY_SEKARANG_SMT_PRODI_PROGRAM_URUT_NimGenerator implements NimGenerator {

	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {

			Program prog = Common.programs.get(calonMahasiswa.getProgram());

			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			Integer tahunSekarang = Calendar.getInstance().get(Calendar.YEAR);

			String digitPertama = tahunSekarang.toString().substring(2);
			String digit12 = calonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL) ? "1" : "2";
			String digitKedua = calonMahasiswa.getProdiLulus().getKode();
			String digitKetiga = prog == null || prog.getNum() == null ? "_" : prog.getNum().toString();

			Long jumlah = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("tahunangkatan", tahun))
					.add(Restrictions.eq("program", calonMahasiswa.getProgram()))
					.add(Restrictions.eq("semesterMulai", calonMahasiswa.getSemesterMulai()))
					.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1).uniqueResult())
					.longValue();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YY_SEKARANG_SMT_PRODI_PROGRAM_URUT_NimGenerator.java:56");

			}

			jumlah += jumlahPengecualian.size();
			String digitEmpat = "000000000000" + (jumlah + 1);
			digitEmpat = digitEmpat.substring(digitEmpat.length() - jumlahDigit);

			System.out.println("digit 1 (kode tahun masuk) = " + digitPertama);
			System.out.println("digit 2 (kode semester) = " + digit12);
			System.out.println("digit 3 (kode prodi) = " + digitKedua);
			System.out.println("digit 4 (kode program) = " + digitKetiga);
			System.out.println("digit 5 (urutan) = " + digitEmpat);

			nim = digitPertama + digit12 + digitKedua + digitKetiga + digitEmpat;

			Integer count = ((Number) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.eq("nim", nim)).setProjection(Projections.count("nim")).uniqueResult())
					.intValue();

			HibernateUtil.closeSessionQuietly(session);

			if (!count.equals(0)) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

		}

		return nim;
	}

}
