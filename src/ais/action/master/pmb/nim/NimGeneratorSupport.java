package ais.action.master.pmb.nim;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

import ais.database.model.BiodataCalonMahasiswa;

/**
 * Utilitas bersama untuk keluarga algoritma penomoran NIM (Nomor Induk Mahasiswa) per-institusi
 * ({@code *NimGenerator.java} pada paket ini): menentukan nomor urut berikutnya yang belum
 * terpakai untuk kombinasi prefix/suffix tertentu, memeriksa apakah suatu NIM sudah dipakai, dan
 * memformat nomor urut dengan left-padding nol.
 *
 * <p>
 * Nomor urut ditentukan dengan mengumpulkan seluruh NIM yang cocok pola {@code prefix%suffix} dari
 * tabel {@code mahasiswa} (mahasiswa aktif) DAN {@code biodata_calon_mahasiswa} (calon mahasiswa
 * lain yang NIM-nya sudah di-generate lebih dulu, mengecualikan baris calon mahasiswa yang sedang
 * diproses sendiri), digabung dengan {@code nimPengecualian} (mis. NIM yang sedang dicadangkan di
 * memori pada proses batch), lalu mengambil nomor urut terbesar di antaranya (diekstrak dari bagian
 * tengah NIM setelah prefix dan sebelum suffix) dan menambahkannya satu.
 * </p>
 */
public class NimGeneratorSupport {

	/** Varian ringkas {@link #nomorUrutBerikutnya(Session, String, String, int, BiodataCalonMahasiswa, List)} tanpa suffix (suffix kosong). */
	public static long nomorUrutBerikutnya(Session session, String prefix, int jumlahDigit,
			BiodataCalonMahasiswa calonMahasiswa, List<String> nimPengecualian) {
		return nomorUrutBerikutnya(session, prefix, "", jumlahDigit, calonMahasiswa, nimPengecualian);
	}

	/**
	 * Menghitung nomor urut berikutnya yang belum terpakai untuk kombinasi {@code prefix}/{@code suffix}
	 * (lihat dokumentasi kelas untuk sumber data dan algoritma). Cocok dipakai baik di dalam maupun
	 * di luar transaksi Hibernate karena murni membaca lewat SQL native.
	 *
	 * @param prefix            awalan NIM (mis. kode tahun/prodi), boleh {@code null} (dianggap kosong)
	 * @param suffix            akhiran NIM, boleh {@code null} (dianggap kosong)
	 * @param jumlahDigit       jumlah digit nomor urut yang diekstrak dari bagian tengah NIM
	 * @param calonMahasiswa    baris calon mahasiswa yang sedang diproses (dikecualikan dari pengecekan agar tidak menghitung NIM dirinya sendiri), boleh {@code null}
	 * @param nimPengecualian   daftar NIM tambahan yang dianggap sudah terpakai (mis. dicadangkan di memori pada proses batch), boleh {@code null}
	 * @return nomor urut berikutnya (nomor terbesar yang ditemukan + 1), minimal 1
	 */
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

	/** Memeriksa apakah {@code nim} sudah dipakai oleh {@code mahasiswa} manapun atau {@code biodata_calon_mahasiswa} lain (mengecualikan {@code calonMahasiswa} sendiri bila diberikan). */
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

	/** Memformat {@code nomor} sebagai string berisi {@code jumlahDigit} digit, dipadatkan nol di kiri (mis. {@code leftPadNomor(7, 4)} -> {@code "0007"}). */
	public static String leftPadNomor(long nomor, int jumlahDigit) {
		String hasil = "000000000000" + nomor;
		return hasil.substring(hasil.length() - jumlahDigit);
	}

	/**
	 * Mengekstrak bagian nomor urut ({@code jumlahDigit} digit terakhir sebelum {@code suffix}) dari
	 * {@code nim}, atau {@code null} bila {@code nim} tidak cocok pola (tidak diawali {@code prefix},
	 * tidak diakhiri {@code suffix}, bagian tengah lebih pendek dari {@code jumlahDigit}, atau bagian
	 * yang diekstrak bukan seluruhnya digit).
	 */
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
