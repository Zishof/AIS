package ais.action.master.recruitment.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import ais.database.model.recruitment.CalonPegawai;

public class RecruitmentNumberGeneratorSupport {

	public static long nomorRegistrasiBerikutnya(Session session, int jumlahDigit, CalonPegawai calonPegawai,
			List<String> nomorPengecualian) {
		return nomorBerikutnya(session, "nomor_induk", jumlahDigit, calonPegawai, nomorPengecualian);
	}

	public static long nomorUjianBerikutnya(Session session, int jumlahDigit, CalonPegawai calonPegawai,
			List<String> nomorPengecualian) {
		return nomorBerikutnya(session, "noujian", jumlahDigit, calonPegawai, nomorPengecualian);
	}

	public static boolean nomorRegistrasiSudahDipakai(Session session, String nomor, CalonPegawai calonPegawai) {
		return nomorSudahDipakai(session, "nomor_induk", nomor, calonPegawai);
	}

	public static boolean nomorUjianSudahDipakai(Session session, String nomor, CalonPegawai calonPegawai) {
		return nomorSudahDipakai(session, "noujian", nomor, calonPegawai);
	}

	public static String leftPadNomor(long nomor, int jumlahDigit) {
		String hasil = "00000000000000000000000000000000000000" + nomor;
		return hasil.substring(hasil.length() - jumlahDigit);
	}

	private static long nomorBerikutnya(Session session, String kolom, int jumlahDigit, CalonPegawai calonPegawai,
			List<String> nomorPengecualian) {
		long nomorTerbesar = 0;
		List<String> nomorTerpakai = ambilNomorTerpakai(session, kolom, calonPegawai);
		for (int i = 0; i < nomorTerpakai.size(); i++) {
			nomorTerbesar = Math.max(nomorTerbesar, ambilNomorUrut(nomorTerpakai.get(i)));
		}
		if (nomorPengecualian != null) {
			for (int i = 0; i < nomorPengecualian.size(); i++) {
				nomorTerbesar = Math.max(nomorTerbesar, ambilNomorUrut(nomorPengecualian.get(i)));
			}
		}
		return nomorTerbesar + 1;
	}

	private static boolean nomorSudahDipakai(Session session, String kolom, String nomor, CalonPegawai calonPegawai) {
		if (nomor == null || nomor.trim().length() == 0) {
			return false;
		}
		String sql = "select count(1) from calon_pegawai where " + kolom + " = :nomor";
		if (calonPegawai != null && calonPegawai.getId() != null) {
			sql += " and id <> :idCalon";
		}
		Query query = session.createSQLQuery(sql);
		query.setParameter("nomor", nomor);
		if (calonPegawai != null && calonPegawai.getId() != null) {
			query.setParameter("idCalon", calonPegawai.getId());
		}
		Number count = (Number) query.uniqueResult();
		return count != null && count.longValue() > 0;
	}

	@SuppressWarnings("unchecked")
	private static List<String> ambilNomorTerpakai(Session session, String kolom, CalonPegawai calonPegawai) {
		String sql = "select " + kolom + " from calon_pegawai where " + kolom + " is not null and trim(" + kolom
				+ ") <> ''";
		if (calonPegawai != null && calonPegawai.getId() != null) {
			sql += " and id <> :idCalon";
		}
		Query query = session.createSQLQuery(sql);
		if (calonPegawai != null && calonPegawai.getId() != null) {
			query.setParameter("idCalon", calonPegawai.getId());
		}
		List hasil = query.list();
		List<String> nomor = new ArrayList<String>();
		for (int i = 0; i < hasil.size(); i++) {
			if (hasil.get(i) != null) {
				nomor.add(hasil.get(i).toString());
			}
		}
		return nomor;
	}

	private static long ambilNomorUrut(String nomor) {
		if (nomor == null) {
			return 0;
		}
		String urut = nomor.replaceAll("[^0-9]", "");
		if (urut.length() == 0) {
			return 0;
		}
		try {
			return Long.parseLong(urut);
		} catch (Exception e) {
			return 0;
		}
	}
}
