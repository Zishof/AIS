package ais.action.master.pmb.noujian;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import ais.database.model.BiodataCalonMahasiswa;

/**
 * Logika bersama "nomor urut berikutnya" untuk keluarga {@code *NoUjianGenerator} di paket
 * {@code ais.action.master.pmb.noujian} — sama persis polanya dengan
 * {@link ais.action.master.pmb.noreg.NoRegGeneratorSupport} (lihat Javadoc di sana untuk
 * penjelasan lengkap algoritma "cari nomor terbesar berpola prefix/suffix lalu +1"), hanya
 * beroperasi pada kolom {@code no_ujian} alih-alih {@code no_registrasi} di tabel
 * {@code biodata_calon_mahasiswa}. Dipakai bersama oleh belasan implementasi
 * {@code *NoUjianGenerator} per institusi agar tidak menduplikasi query dan parsing nomor.
 */
public class NoUjianGeneratorSupport {

	/** Sama seperti {@link #nomorUrutBerikutnya(Session, String, String, int, BiodataCalonMahasiswa, List)} dengan {@code suffix} kosong. */
	public static long nomorUrutBerikutnya(Session session, String prefix, int jumlahDigit,
			BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> nomorPengecualian) {
		return nomorUrutBerikutnya(session, prefix, "", jumlahDigit, biodataCalonMahasiswa, nomorPengecualian);
	}

	/**
	 * Implementasi kanonik — algoritma identik dengan
	 * {@link ais.action.master.pmb.noreg.NoRegGeneratorSupport#nomorUrutBerikutnya(Session, String, String, int, BiodataCalonMahasiswa, List)}
	 * tapi mencari pada kolom {@code no_ujian}.
	 *
	 * @param session               sesi Hibernate aktif
	 * @param prefix                awalan tetap nomor ujian institusi/jalur ini
	 * @param suffix                akhiran tetap, boleh kosong/{@code null}
	 * @param jumlahDigit           dipertahankan untuk kompatibilitas tanda tangan, padding
	 *                              dilakukan terpisah lewat {@link #leftPadNomor}
	 * @param biodataCalonMahasiswa data calon mahasiswa yang sedang diproses, boleh {@code null}
	 * @param nomorPengecualian     nomor lain yang sedang dipakai di memori tapi belum tersimpan,
	 *                              boleh {@code null}
	 * @return nomor urut berikutnya yang belum terpakai (mulai dari 1)
	 */
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

	/**
	 * Pengecekan ulang eksistensi tepat-sama sebelum menyimpan — jaring pengaman terhadap race
	 * condition, mengingat {@link #nomorUrutBerikutnya} tidak melakukan locking.
	 *
	 * @param session               sesi Hibernate aktif
	 * @param noUjian               nomor ujian lengkap yang akan diperiksa
	 * @param biodataCalonMahasiswa data yang sedang diedit (dikecualikan bila sudah punya id),
	 *                              boleh {@code null}
	 * @return {@code true} bila nomor sudah dipakai baris lain
	 */
	public static boolean nomorSudahDipakai(Session session, String noUjian,
			BiodataCalonMahasiswa biodataCalonMahasiswa) {
		if (noUjian == null || noUjian.trim().length() == 0) {
			return false;
		}
		String sql = "select count(1) from biodata_calon_mahasiswa where no_ujian = :noUjian";
		if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null) {
			sql += " and id <> :idCalon";
		}
		Query query = session.createSQLQuery(sql);
		query.setParameter("noUjian", noUjian);
		if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getId() != null) {
			query.setParameter("idCalon", biodataCalonMahasiswa.getId());
		}
		Number count = (Number) query.uniqueResult();
		return count != null && count.longValue() > 0;
	}

	/** Meratakan {@code nomor} dengan awalan {@code "0"} hingga {@code jumlahDigit} karakter. */
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
		String sql = "select no_ujian from biodata_calon_mahasiswa "
				+ "where no_ujian is not null and trim(no_ujian) like :pola";
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
