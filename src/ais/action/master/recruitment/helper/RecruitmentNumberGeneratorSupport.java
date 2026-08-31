package ais.action.master.recruitment.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import ais.database.model.recruitment.CalonPegawai;

/**
 * Logika bersama "nomor urut berikutnya" untuk keluarga {@code *NoRegGeneratorPegawai}/
 * {@code *NoUjianGeneratorPegawai} di paket {@code ais.action.master.recruitment.helper} — padanan
 * modul rekrutmen pegawai dari {@link ais.action.master.pmb.noreg.NoRegGeneratorSupport}/
 * {@link ais.action.master.pmb.noujian.NoUjianGeneratorSupport} yang dipakai modul PMB mahasiswa.
 * Algoritma sama ("cari nomor terbesar lalu +1", lihat Javadoc {@code NoRegGeneratorSupport} untuk
 * detail), TAPI kelas ini menangani DUA kolom sekaligus dalam satu kelas ({@code nomor_induk} dan
 * {@code noujian} pada tabel {@code calon_pegawai}) via parameter {@code kolom} pada method privat
 * bersama, alih-alih dua kelas terpisah — dan TIDAK mendukung prefix/suffix (nomor rekrutmen
 * pegawai di sini murni numerik tanpa awalan/akhiran institusi).
 */
public class RecruitmentNumberGeneratorSupport {

	/** Nomor urut registrasi (kolom {@code nomor_induk}) berikutnya — lihat Javadoc kelas untuk algoritma. */
	public static long nomorRegistrasiBerikutnya(Session session, int jumlahDigit, CalonPegawai calonPegawai,
			List<String> nomorPengecualian) {
		return nomorBerikutnya(session, "nomor_induk", jumlahDigit, calonPegawai, nomorPengecualian);
	}

	/** Nomor urut ujian (kolom {@code noujian}) berikutnya — lihat Javadoc kelas untuk algoritma. */
	public static long nomorUjianBerikutnya(Session session, int jumlahDigit, CalonPegawai calonPegawai,
			List<String> nomorPengecualian) {
		return nomorBerikutnya(session, "noujian", jumlahDigit, calonPegawai, nomorPengecualian);
	}

	/** Pengecekan ulang eksistensi tepat-sama pada kolom {@code nomor_induk} sebelum menyimpan. */
	public static boolean nomorRegistrasiSudahDipakai(Session session, String nomor, CalonPegawai calonPegawai) {
		return nomorSudahDipakai(session, "nomor_induk", nomor, calonPegawai);
	}

	/** Pengecekan ulang eksistensi tepat-sama pada kolom {@code noujian} sebelum menyimpan. */
	public static boolean nomorUjianSudahDipakai(Session session, String nomor, CalonPegawai calonPegawai) {
		return nomorSudahDipakai(session, "noujian", nomor, calonPegawai);
	}

	/** Meratakan {@code nomor} dengan awalan {@code "0"} hingga {@code jumlahDigit} karakter. */
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
