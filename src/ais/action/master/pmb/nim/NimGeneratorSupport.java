package ais.action.master.pmb.nim;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

import ais.database.model.BiodataCalonMahasiswa;

public class NimGeneratorSupport {

	public static long nomorUrutBerikutnya(Session session, String prefix, int jumlahDigit,
			BiodataCalonMahasiswa calonMahasiswa, List<String> nimPengecualian) {
		return nomorUrutBerikutnya(session, prefix, "", jumlahDigit, calonMahasiswa, nimPengecualian);
	}

	public static long nomorUrutBerikutnya(Session session, String prefix, String suffix, int jumlahDigit,
			BiodataCalonMahasiswa calonMahasiswa, List<String> nimPengecualian) {
		Set<String> nimTerpakai = new HashSet<String>();
		String likePattern = (prefix == null ? "" : prefix) + "%" + (suffix == null ? "" : suffix);

		SQLQuery qMahasiswa = session.createSQLQuery(
				"select nim from mahasiswa where nim is not null and trim(nim) like :pola");
		qMahasiswa.setString("pola", likePattern);
		nimTerpakai.addAll(qMahasiswa.list());

		String sqlCalon = "select nim from biodata_calon_mahasiswa where nim is not null and trim(nim) like :pola";
		if (calonMahasiswa != null && calonMahasiswa.getId() != null) {
			sqlCalon += " and id <> :idCalon";
		}
		SQLQuery qCalon = session.createSQLQuery(sqlCalon);
		qCalon.setString("pola", likePattern);
		if (calonMahasiswa != null && calonMahasiswa.getId() != null) {
			qCalon.setLong("idCalon", calonMahasiswa.getId());
		}
		nimTerpakai.addAll(qCalon.list());

		if (nimPengecualian != null) {
			nimTerpakai.addAll(nimPengecualian);
		}

		long nomorTerbesar = 0L;
		for (String nim : nimTerpakai) {
			Long nomor = ambilNomorUrut(prefix, suffix, jumlahDigit, nim);
			if (nomor != null && nomor.longValue() > nomorTerbesar) {
				nomorTerbesar = nomor.longValue();
			}
		}
		return nomorTerbesar + 1L;
	}

	public static boolean nimSudahDipakai(Session session, String nim, BiodataCalonMahasiswa calonMahasiswa) {
		Number countMahasiswa = (Number) session.createSQLQuery("select count(1) from mahasiswa where nim = :nim")
				.setString("nim", nim).uniqueResult();

		String sqlCalon = "select count(1) from biodata_calon_mahasiswa where nim = :nim";
		if (calonMahasiswa != null && calonMahasiswa.getId() != null) {
			sqlCalon += " and id <> :idCalon";
		}
		SQLQuery qCalon = session.createSQLQuery(sqlCalon);
		qCalon.setString("nim", nim);
		if (calonMahasiswa != null && calonMahasiswa.getId() != null) {
			qCalon.setLong("idCalon", calonMahasiswa.getId());
		}
		Number countCalon = (Number) qCalon.uniqueResult();

		return (countMahasiswa != null && countMahasiswa.intValue() > 0)
				|| (countCalon != null && countCalon.intValue() > 0);
	}

	public static String leftPadNomor(long nomor, int jumlahDigit) {
		String hasil = "000000000000" + nomor;
		return hasil.substring(hasil.length() - jumlahDigit);
	}

	private static Long ambilNomorUrut(String prefix, String suffix, int jumlahDigit, String nim) {
		if (nim == null) {
			return null;
		}
		String nilai = nim.trim();
		prefix = prefix == null ? "" : prefix;
		suffix = suffix == null ? "" : suffix;
		if (!nilai.startsWith(prefix) || !nilai.endsWith(suffix)) {
			return null;
		}
		int start = prefix.length();
		int end = suffix.length() == 0 ? nilai.length() : nilai.length() - suffix.length();
		if (end <= start || end - start < jumlahDigit) {
			return null;
		}
		String nomor = nilai.substring(end - jumlahDigit, end);
		for (int i = 0; i < nomor.length(); i++) {
			if (!Character.isDigit(nomor.charAt(i))) {
				return null;
			}
		}
		return Long.valueOf(Long.parseLong(nomor));
	}

}
