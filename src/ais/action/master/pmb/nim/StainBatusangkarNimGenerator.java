package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Jurusan;

/**
 * Algoritma NIM khusus STAIN Batusangkar. Format NIM 4 komponen ditambah nomor urut:
 * {@code [2 digit terakhir tahun angkatan][kode jenjang][kode program studi kelulusan, dipad jadi
 * minimal 2 karakter][1 digit jenis kelamin: "1"=Laki-laki, "2"=Perempuan, "0"=tidak diketahui]
 * [4 digit nomor urut]}, mis. {@code 26S1TI104201} (ilustratif). Kode prodi diambil dari
 * {@link Jurusan#getKode()}; bila kosong atau hanya berisi tanda {@code -}/{@code _}, jatuh
 * kembali ke id numerik prodi, lalu disaring hanya karakter alfanumerik dan dipad nol di depan
 * bila panjangnya 1 karakter — bila tetap kosong, method melempar
 * {@link IllegalArgumentException} (kode prodi wajib tersedia).
 *
 * <h2>Pencarian nomor urut</h2>
 * <p>
 * Berbeda dari generator NIM lain di paket ini yang hanya mengandalkan hitung baris (COUNT),
 * kelas ini secara eksplisit mengumpulkan SELURUH NIM yang sudah memakai {@code prefix} yang sama
 * (4 komponen pertama) dari tabel {@code mahasiswa} DAN {@code biodata_calon_mahasiswa} (via SQL
 * native, dikecualikan baris calon mahasiswa itu sendiri bila sudah punya id) ditambah daftar
 * pengecualian dari percobaan rekursif sebelumnya, mengekstrak 4 digit nomor urut dari setiap NIM
 * yang cocok pola, lalu mengambil nomor TERBESAR + 1 sebagai nomor urut berikutnya — pendekatan
 * ini tahan terhadap "lubang" penomoran (nomor yang dihapus/dilewati) dibanding pendekatan
 * COUNT+1 murni.
 * </p>
 * <p>
 * Setiap komponen NIM divalidasi tidak kosong lewat {@link #validasiKomponenNim} sebelum
 * digabungkan; hasil akhir divalidasi tidak kosong lewat {@link #validasiNim}. Bila NIM hasil
 * gabungan ternyata sudah terpakai (dicek ulang lewat {@link #nimSudahDipakai}), nomor tersebut
 * ditambahkan ke daftar pengecualian dan method memanggil dirinya sendiri untuk mencoba nomor
 * berikutnya.
 * </p>
 */
public class StainBatusangkarNimGenerator implements NimGenerator {

	/** Menghasilkan NIM baru tanpa daftar pengecualian awal — lihat {@link #generateNim(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menyusun dan memvalidasi NIM berformat {@code [2 digit tahun][jenjang][prodi][jenis
	 * kelamin][4 digit urut]} (lihat detail format di dokumentasi kelas), menghindari nilai yang
	 * ada di {@code jumlahPengecualian} maupun yang sudah dipakai di tabel {@code mahasiswa}/
	 * {@code biodata_calon_mahasiswa}; mencoba ulang secara rekursif bila terjadi bentrok.
	 *
	 * @param calonMahasiswa     calon mahasiswa target; {@code tahun}, {@code jenjang},
	 *                           {@code prodiLulus}, dan {@code jenisKelamin}-nya menentukan
	 *                           komponen awal NIM
	 * @param jumlahPengecualian NIM-NIM yang sudah dicoba dan diketahui bentrok, dihindari pada
	 *                           percobaan berikutnya (diperbarui di tempat)
	 * @return NIM baru yang belum pernah dipakai
	 * @throws IllegalArgumentException bila salah satu komponen (tahun/jenjang/prodi/jenis
	 *                                   kelamin) tidak dapat ditentukan
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		Integer tahun = calonMahasiswa.getTahun();
		String digitPertama = tahun.toString().substring(2);

		String digitKedua = calonMahasiswa == null || calonMahasiswa.getJenjang() == null ? ""
				: calonMahasiswa.getJenjang().getKode();

		String digitKetiga = ambilKodeProdi(calonMahasiswa == null ? null : calonMahasiswa.getProdiLulus());
		String digitKetiga1 = calonMahasiswa.getJenisKelamin() == null ? "0"
				: calonMahasiswa.getJenisKelamin().equals("Laki-laki") ? "1" : "2";
		validasiKomponenNim(digitPertama, "tahun", calonMahasiswa);
		validasiKomponenNim(digitKedua, "jenjang", calonMahasiswa);
		validasiKomponenNim(digitKetiga, "prodi", calonMahasiswa);
		validasiKomponenNim(digitKetiga1, "jenis kelamin", calonMahasiswa);

		String prefix = digitPertama + digitKedua + digitKetiga + digitKetiga1;
		Long nomorUrut = ambilNomorUrutBerikutnya(prefix, calonMahasiswa, jumlahPengecualian);
		String digitKeempat = "000000000000" + nomorUrut;
		digitKeempat = digitKeempat.substring(digitKeempat.length() - 4);

		System.out.println("digit pertama (kode prodi) = " + digitPertama);
		System.out.println("digit kedua (kode tahun) = " + digitKedua);
		System.out.println("digit ketiga (tahun sememster) = " + digitKetiga);
		System.out.println("digit ketiga (jk) = " + digitKetiga1);
		System.out.println("digit keempat (urutan) = " + digitKeempat);

		String nim = digitPertama + digitKedua + digitKetiga + digitKetiga1 + digitKeempat;
		validasiNim(nim, calonMahasiswa);

		if (nimSudahDipakai(nim, calonMahasiswa)) {
			jumlahPengecualian.add(nim);
			return generateNim(calonMahasiswa, jumlahPengecualian);
		}

		return nim;
	}

	/**
	 * Mengambil nomor urut terbesar di antara seluruh NIM/nomor calon yang sudah berawalan
	 * {@code prefix} (dari tabel {@code mahasiswa} dan {@code biodata_calon_mahasiswa}, ditambah
	 * {@code jumlahPengecualian}), lalu mengembalikan nilai tersebut ditambah satu.
	 */
	private Long ambilNomorUrutBerikutnya(String prefix, BiodataCalonMahasiswa calonMahasiswa,
			List<String> jumlahPengecualian) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Set<String> nimTerpakai = new HashSet<String>();

			String sqlMahasiswa = "select nim from mahasiswa where nim is not null and trim(nim) like :prefix";
			SQLQuery qMahasiswa = session.createSQLQuery(sqlMahasiswa);
			qMahasiswa.setString("prefix", prefix + "%");
			nimTerpakai.addAll(qMahasiswa.list());

			String sqlCalon = "select nim from biodata_calon_mahasiswa where nim is not null and trim(nim) like :prefix";
			if (calonMahasiswa != null && calonMahasiswa.getId() != null) {
				sqlCalon += " and id <> :idCalon";
			}
			SQLQuery qCalon = session.createSQLQuery(sqlCalon);
			qCalon.setString("prefix", prefix + "%");
			if (calonMahasiswa != null && calonMahasiswa.getId() != null) {
				qCalon.setLong("idCalon", calonMahasiswa.getId());
			}
			nimTerpakai.addAll(qCalon.list());

			if (jumlahPengecualian != null) {
				nimTerpakai.addAll(jumlahPengecualian);
			}

			long nomorTerbesar = 0L;
			for (String nim : nimTerpakai) {
				Long nomor = ambilNomorUrutDariNim(prefix, nim);
				if (nomor != null && nomor.longValue() > nomorTerbesar) {
					nomorTerbesar = nomor.longValue();
				}
			}
			return Long.valueOf(nomorTerbesar + 1L);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private Long ambilNomorUrutDariNim(String prefix, String nim) {
		if (prefix == null || nim == null) {
			return null;
		}
		nim = nim.trim();
		if (!nim.startsWith(prefix) || nim.length() < prefix.length() + 4) {
			return null;
		}
		String nomor = nim.substring(nim.length() - 4);
		for (int i = 0; i < nomor.length(); i++) {
			if (!Character.isDigit(nomor.charAt(i))) {
				return null;
			}
		}
		return Long.valueOf(Long.parseLong(nomor));
	}

	private boolean nimSudahDipakai(String nim, BiodataCalonMahasiswa calonMahasiswa) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Number countMahasiswa = (Number) session.createSQLQuery(
					"select count(1) from mahasiswa where nim = :nim")
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
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private String ambilKodeProdi(Jurusan prodi) {
		String kode = prodi == null ? "" : prodi.getKode();
		if (kode != null) {
			kode = kode.trim();
		}
		if (kode == null || kode.length() == 0 || kode.replace("-", "").replace("_", "").trim().length() == 0) {
			kode = prodi == null || prodi.getId() == null ? "" : prodi.getId().toString();
		}
		kode = kode == null ? "" : kode.replaceAll("[^A-Za-z0-9]", "");
		if (kode.length() == 1) {
			kode = "0" + kode;
		}
		if (kode.length() == 0) {
			throw new IllegalArgumentException("Kode prodi untuk generate NIM belum tersedia.");
		}
		return kode;
	}

	private void validasiNim(String nim, BiodataCalonMahasiswa calonMahasiswa) {
		if (nim == null || nim.trim().isEmpty()) {
			throw new IllegalArgumentException("Format NIM tidak valid untuk "
					+ (calonMahasiswa == null ? "" : calonMahasiswa.getNama()) + ": " + nim);
		}
	}

	private void validasiKomponenNim(String nilai, String label, BiodataCalonMahasiswa calonMahasiswa) {
		if (nilai == null || nilai.trim().isEmpty()) {
			throw new IllegalArgumentException("Komponen " + label + " untuk generate NIM belum valid"
					+ (calonMahasiswa == null ? "" : " pada " + calonMahasiswa.getNama()) + ": " + nilai);
		}
	}

}
