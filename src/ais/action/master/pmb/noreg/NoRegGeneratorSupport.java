package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import ais.database.model.BiodataCalonMahasiswa;

public class NoRegGeneratorSupport {

	public static long nomorUrutBerikutnya(Session session, String prefix, int jumlahDigit,
			BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> nomorPengecualian) {
		return nomorUrutBerikutnya(session, prefix, "", jumlahDigit, biodataCalonMahasiswa, nomorPengecualian);
	}

	public static long nomorUrutBerikutnya(Session session, String prefix, String suffix, int jumlahDigit,
			BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> nomorPengecualian) {
		long nomorTerbesar = 0;
		List<String> nomorTerpakai = ambilNomorTerpakai(session, prefix, suffix, biodataCalonMahasiswa);
		for (int i = 0; i < nomorTerpakai.size(); i++) {
			nomorTerbesar = Math.max(nomorTerbesar, ambilNomorUrut(nomorTerpakai.get(i), prefix, suffix));
		}
		if (nomorPengecualian != null) {
			for (int i = 0; i < nomorPengecualian.size(); i++) {
				String nomor = nomorPengecualian.get(i);
				if (nomor != null && nomor.startsWith(prefix) && (suffix == null || suffix.length() == 0
						|| nomor.endsWith(suffix))) {
					nomorTerbesar = Math.max(nomorTerbesar, ambilNomorUrut(nomor, prefix, suffix));
				}
			}
		}
		return nomorTerbesar + 1;
	}

	public static boolean nomorSudahDipakai(Session session, String noRegistrasi,
			BiodataCalonMahasiswa biodataCalonMahasiswa) {
		if (noRegistrasi == null || noRegistrasi.trim().length() == 0) {
			return false;
		}
		String sql = "select count(1) from biodata_calon_mahasiswa where no_registrasi = :noRegistrasi";
		if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null) {
			sql += " and id <> :idCalon";
		}
		Query query = session.createSQLQuery(sql);
		query.setParameter("noRegistrasi", noRegistrasi);
		if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null) {
			query.setParameter("idCalon", biodataCalonMahasiswa.getId());
		}
		Number count = (Number) query.uniqueResult();
		return count != null && count.longValue() > 0;
	}

	public static String leftPadNomor(long nomor, int jumlahDigit) {
		String hasil = "00000000000000000000000000000000000000" + nomor;
		return hasil.substring(hasil.length() - jumlahDigit);
	}

	@SuppressWarnings("unchecked")
	private static List<String> ambilNomorTerpakai(Session session, String prefix, String suffix,
			BiodataCalonMahasiswa biodataCalonMahasiswa) {
		String pola = prefix + "%";
		if (suffix != null && suffix.length() > 0) {
			pola = prefix + "%" + suffix;
		}
		String sql = "select no_registrasi from biodata_calon_mahasiswa "
				+ "where no_registrasi is not null and trim(no_registrasi) like :pola";
		if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null) {
			sql += " and id <> :idCalon";
		}
		Query query = session.createSQLQuery(sql);
		query.setParameter("pola", pola);
		if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null) {
			query.setParameter("idCalon", biodataCalonMahasiswa.getId());
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

	private static long ambilNomorUrut(String nomor, String prefix, String suffix) {
		if (nomor == null || !nomor.startsWith(prefix)) {
			return 0;
		}
		int awal = prefix.length();
		int akhir = suffix == null || suffix.length() == 0 ? nomor.length() : nomor.length() - suffix.length();
		if (akhir <= awal) {
			return 0;
		}
		String urut = nomor.substring(awal, akhir).replaceAll("[^0-9]", "");
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
