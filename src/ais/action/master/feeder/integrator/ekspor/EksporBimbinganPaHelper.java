package ais.action.master.feeder.integrator.ekspor;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.master.feeder.util.FeederExporterGenerator;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;

/** Pemetaan bersama data KRS ke template aktivitas Bimbingan Akademik Feeder. */
final class EksporBimbinganPaHelper {

	private EksporBimbinganPaHelper() {
	}

	static List<KrsMahasiswa> ambil(Session session, SaringanFeeder saring) {
		Criteria criteria = session.createCriteria(KrsMahasiswa.class)
				.add(Restrictions.gt("semester", Integer.valueOf(0)))
				.add(Restrictions.isNull("semesterPendek"))
				.add(saring.kelas.isEmpty()
						? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kelas", saring.kelas, MatchMode.EXACT))
				.add(saring.tahunAkademik.isEmpty()
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAkademik", saring.tahunAkademik))
				.createAlias("mahasiswa", "mahasiswa")
				.add(saring.jurusan == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa.jurusan", saring.jurusan))
				.createAlias("mahasiswa.jurusan", "jurusan")
				.add(saring.fakultas == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqValue("jurusan.fakultas", saring.fakultas, false))
				.addOrder(Order.asc("mahasiswa.nim"));

		List<KrsMahasiswa> hasil = ConstantValues.simpleList(criteria, KrsMahasiswa.class);
		if (saring.semester.isEmpty()) {
			return hasil;
		}

		List<KrsMahasiswa> sesuaiPeriode = new ArrayList<KrsMahasiswa>();
		for (KrsMahasiswa krs : hasil) {
			if (digitPeriode(krs).equals(digitPeriode(saring.semester))) {
				sesuaiPeriode.add(krs);
			}
		}
		return sesuaiPeriode;
	}

	static String idSemester(KrsMahasiswa krs) {
		String tahunAkademik = krs.getTahunAkademik();
		if (tahunAkademik == null || !tahunAkademik.contains("/")) {
			return "";
		}
		return tahunAkademik.split("/")[0] + digitPeriode(krs);
	}

	static String judul(KrsMahasiswa krs) {
		Mahasiswa mahasiswa = krs.getMahasiswa();
		return "Bimbingan / konsultasi akademik \"" + mahasiswa.getNim() + "\" \""
				+ mahasiswa.getNama() + "\" TA:" + krs.getTahunAkademik() + " SMT:" + krs.getSemester();
	}

	static String kodeProdi(KrsMahasiswa krs) {
		return krs.getMahasiswa() == null || krs.getMahasiswa().getJurusan() == null
				? "" : krs.getMahasiswa().getJurusan().getKodeEpsbed();
	}

	private static String digitPeriode(KrsMahasiswa krs) {
		return FeederExporterGenerator.digitPeriodeFeeder(krs.getMahasiswa(), krs.getSemester(),
				krs.getSemesterPendek());
	}

	private static String digitPeriode(String semester) {
		if (Perkuliahan.SP.equalsIgnoreCase(semester)) {
			return "3";
		}
		return Perkuliahan.GENAP.equalsIgnoreCase(semester) ? "2" : "1";
	}
}
