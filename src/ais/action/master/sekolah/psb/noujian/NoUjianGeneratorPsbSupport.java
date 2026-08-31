package ais.action.master.sekolah.psb.noujian;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import ais.database.model.sekolah.CalonSiswa;

public class NoUjianGeneratorPsbSupport {

	public static long nomorUrutBerikutnya(Session session, String prefix, int jumlahDigit, CalonSiswa calonSiswa,
			List<String> nomorPengecualian) {
		long nomorTerbesar = 0;
		List<String> nomorTerpakai = ambilNomorTerpakai(session, prefix, calonSiswa);
		for (int i = 0; i < nomorTerpakai.size(); i++) {
			nomorTerbesar = Math.max(nomorTerbesar, ambilNomorUrut(nomorTerpakai.get(i), prefix));
		}
		if (nomorPengecualian != null) {
			for (int i = 0; i < nomorPengecualian.size(); i++) {
				String nomor = nomorPengecualian.get(i);
				if (nomor != null && nomor.startsWith(prefix)) {
					nomorTerbesar = Math.max(nomorTerbesar, ambilNomorUrut(nomor, prefix));
				}
			}
		}
		return nomorTerbesar + 1;
	}

	public static boolean nomorSudahDipakai(Session session, String noUjian, CalonSiswa calonSiswa) {
		if (noUjian == null || noUjian.trim().length() == 0) {
			return false;
		}
		String sql = "select count(1) from sekolah.calon_siswa where noujian = :noUjian";
		if (calonSiswa != null && calonSiswa.getId() != null) {
			sql += " and id <> :idCalon";
		}
		Query query = session.createSQLQuery(sql);
		query.setParameter("noUjian", noUjian);
		if (calonSiswa != null && calonSiswa.getId() != null) {
			query.setParameter("idCalon", calonSiswa.getId());
		}
		Number count = (Number) query.uniqueResult();
		return count != null && count.longValue() > 0;
	}

	public static String leftPadNomor(long nomor, int jumlahDigit) {
		String hasil = "00000000000000000000000000000000000000" + nomor;
		return hasil.substring(hasil.length() - jumlahDigit);
	}

	@SuppressWarnings("unchecked")
	private static List<String> ambilNomorTerpakai(Session session, String prefix, CalonSiswa calonSiswa) {
		String sql = "select noujian from sekolah.calon_siswa "
				+ "where noujian is not null and trim(noujian) like :pola";
		if (calonSiswa != null && calonSiswa.getId() != null) {
			sql += " and id <> :idCalon";
		}
		Query query = session.createSQLQuery(sql);
		query.setParameter("pola", prefix + "%");
		if (calonSiswa != null && calonSiswa.getId() != null) {
			query.setParameter("idCalon", calonSiswa.getId());
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

	private static long ambilNomorUrut(String nomor, String prefix) {
		if (nomor == null || !nomor.startsWith(prefix)) {
			return 0;
		}
		String urut = nomor.substring(prefix.length()).replaceAll("[^0-9]", "");
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
