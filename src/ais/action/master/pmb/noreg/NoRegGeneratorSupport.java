package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import ais.database.model.BiodataCalonMahasiswa;

/**
 * Logika bersama "nomor urut berikutnya" untuk keluarga {@code *NoRegGenerator} di paket
 * {@code ais.action.master.pmb.noreg} — belasan implementasi {@code NoRegGenerator} per institusi
 * (mis. {@code StainBatusangkarNoRegGenerator}, {@code BukittinggiNoRegGenerator}) sebelumnya
 * masing-masing menduplikasi query cari-nomor-terbesar dan parsing nomor urutnya sendiri; kelas ini
 * mengekstrak logika tersebut menjadi satu implementasi yang dipakai bersama. Kelas paralel dengan
 * {@code ais.action.master.pmb.nim.NimGeneratorSupport} (untuk NIM) dan
 * {@code ais.action.master.recruitment.helper.RecruitmentNumberGeneratorSupport} (untuk nomor
 * registrasi/ujian pegawai) — ketiganya menerapkan pola algoritma yang sama pada tabel berbeda.
 *
 * <h2>Algoritma "nomor urut berikutnya"</h2>
 * <p>
 * Diberikan {@code prefix} (dan opsional {@code suffix}) yang menjadi pola nomor registrasi suatu
 * institusi/jalur pendaftaran, method ini: (1) mengambil SEMUA nilai {@code no_registrasi} yang
 * sudah tersimpan di {@code biodata_calon_mahasiswa} yang cocok pola {@code prefix%suffix} (via
 * {@code LIKE}, dikecualikan baris milik {@code biodataCalonMahasiswa} sendiri bila sedang
 * mengedit data yang sudah ada — mencegah nomor kalah bersaing dengan nomornya sendiri saat
 * disimpan ulang); (2) mem-parsing bagian numerik di antara prefix dan suffix dari setiap nomor
 * (karakter non-digit dibuang dulu via regex, sehingga nomor dengan pemisah seperti {@code "-"}
 * tetap terbaca); (3) turut mempertimbangkan {@code nomorPengecualian} — daftar nomor yang SEDANG
 * dipakai di memori tapi BELUM tersimpan ke database (mis. batch registrasi yang sedang diproses
 * dalam satu transaksi), agar dua pendaftar dalam batch yang sama tidak diberi nomor urut yang
 * sama; (4) mengembalikan nomor urut TERBESAR yang ditemukan (dari database maupun pengecualian)
 * ditambah 1. Tidak ada penguncian/locking eksplisit di sini — pemanggil bertanggung jawab
 * mencegah race condition bila dua registrasi diproses bersamaan (lihat catatan di
 * {@link #nomorSudahDipakai} yang biasa dipakai sebagai pengecekan ulang sebelum simpan).
 * </p>
 */
public class NoRegGeneratorSupport {

	/** Sama seperti {@link #nomorUrutBerikutnya(Session, String, String, int, BiodataCalonMahasiswa, List)} dengan {@code suffix} kosong. */
	public static long nomorUrutBerikutnya(Session session, String prefix, int jumlahDigit,
			BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> nomorPengecualian) {
		return nomorUrutBerikutnya(session, prefix, "", jumlahDigit, biodataCalonMahasiswa, nomorPengecualian);
	}

	/**
	 * Implementasi kanonik — lihat penjelasan algoritma lengkap pada Javadoc kelas
	 * {@link NoRegGeneratorSupport}.
	 *
	 * @param session                 sesi Hibernate aktif untuk query pencarian nomor terpakai
	 * @param prefix                  awalan tetap nomor registrasi institusi/jalur ini
	 * @param suffix                  akhiran tetap (boleh kosong/{@code null} bila tidak ada)
	 * @param jumlahDigit             tidak dipakai langsung di sini (padding dilakukan terpisah
	 *                                lewat {@link #leftPadNomor}) — dipertahankan di tanda tangan
	 *                                method untuk kompatibilitas pemanggil lama
	 * @param biodataCalonMahasiswa   data calon mahasiswa yang sedang diproses (nomornya sendiri
	 *                                dikecualikan dari pencarian bila sudah punya id), boleh
	 *                                {@code null} untuk pendaftar baru
	 * @param nomorPengecualian       nomor-nomor lain yang sedang dipakai di memori tapi belum
	 *                                tersimpan (batch dalam transaksi yang sama), boleh
	 *                                {@code null}
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
	 * Pengecekan ulang eksistensi tepat-sama (bukan pola prefix) sebelum menyimpan — dipakai
	 * pemanggil sebagai jaring pengaman terakhir terhadap race condition yang tidak ditangkap
	 * oleh {@link #nomorUrutBerikutnya}, mengingat method itu tidak melakukan locking.
	 *
	 * @param session               sesi Hibernate aktif
	 * @param noRegistrasi          nomor registrasi lengkap yang akan diperiksa
	 * @param biodataCalonMahasiswa data yang sedang diedit (dikecualikan dari pencarian bila
	 *                              sudah punya id), boleh {@code null}
	 * @return {@code true} bila nomor sudah dipakai baris lain
	 */
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

	/** Meratakan {@code nomor} dengan awalan {@code "0"} hingga {@code jumlahDigit} karakter (mis. {@code leftPadNomor(7, 4) == "0007"}). */
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
