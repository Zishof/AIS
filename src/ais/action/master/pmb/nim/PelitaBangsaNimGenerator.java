package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Perkuliahan;

/**
 * Algoritma penomoran NIM khusus institusi Pelita Bangsa, dengan dua pola berbeda tergantung
 * program studi:
 * <ul>
 * <li>Program studi berkode {@code "006"} (kemungkinan program tahun ajaran/matrikulasi khusus):
 * NIM disusun dari {@code 2 digit tahun masuk + 2 digit tahun berikutnya + "01" + 3 digit nomor
 * urut}, mencerminkan penomoran berbasis tahun ajaran (mis. 2024/2025).</li>
 * <li>Program studi lainnya: NIM disusun dari {@code kode prodi + 2 digit tahun masuk +
 * 1 digit penanda semester ganjil ("1") atau genap ("2") + 4 digit nomor urut}.</li>
 * </ul>
 * Pada kedua pola, pembangkitan nomor urut dan pengecekan pemakaian didelegasikan ke
 * {@link NimGeneratorSupport}.
 */
public class PelitaBangsaNimGenerator implements NimGenerator {

	/** Seperti {@link #generateNim(BiodataCalonMahasiswa, List)}, tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan NIM sesuai pola yang berlaku untuk program studi lulus calon mahasiswa — lihat
	 * penjelasan dua pola pada dokumentasi kelas. Nomor urut dihitung via
	 * {@link NimGeneratorSupport#nomorUrutBerikutnya} (memperhitungkan kandidat yang sudah bentrok
	 * di {@code jumlahPengecualian}); bila NIM hasil ternyata sudah dipakai (dicek via
	 * {@link NimGeneratorSupport#nimSudahDipakai}), nomor tersebut ditambahkan ke
	 * {@code jumlahPengecualian} dan method memanggil dirinya sendiri secara rekursif.
	 *
	 * @param jumlahPengecualian NIM kandidat yang sudah terbukti bentrok pada percobaan sebelumnya
	 * @return NIM yang belum dipakai mahasiswa manapun, atau {@code "-"} bila prodi lulus belum ditentukan
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		if (calonMahasiswa.getProdiLulus() == null) {
			return "-";
		} else if (calonMahasiswa.getProdiLulus().getKode().trim().equals("006")) {
			Integer tahun = calonMahasiswa.getTahun();
			Integer tahunberikut = calonMahasiswa.getTahun() + 1;

			String digitKedua = tahun.toString().substring(2);
			String digitKetiga = tahunberikut.toString().substring(2);

			Session session = HibernateUtil.openSession();
			String prefix = digitKedua + digitKetiga + "01";
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, 3, calonMahasiswa,
					jumlahPengecualian);
			String digitKeempat = NimGeneratorSupport.leftPadNomor(nomorUrut, 3);

			String nim = prefix + digitKeempat;

			boolean nimSudahDipakai = NimGeneratorSupport.nimSudahDipakai(session, nim, calonMahasiswa);
			HibernateUtil.closeSessionQuietly(session);

			if (nimSudahDipakai) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

			return nim;

		} else {

			String digitPertama = calonMahasiswa == null || calonMahasiswa.getProdiLulus() == null ? "--"
					: calonMahasiswa.getProdiLulus().getKode().trim();

			Integer tahun = calonMahasiswa.getTahun();
			String digitKedua = tahun.toString().substring(2);

			String digitKetiga = calonMahasiswa == null ? "-"
					: calonMahasiswa.getJenisSemester().equals(Perkuliahan.GANJIL) ? "1" : "2";

			Session session = HibernateUtil.openSession();
			String prefix = digitPertama + digitKedua + digitKetiga;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, 4, calonMahasiswa,
					jumlahPengecualian);
			String digitKeempat = NimGeneratorSupport.leftPadNomor(nomorUrut, 4);

			System.out.println("digit pertama (kode prodi) = " + digitPertama);
			System.out.println("digit kedua (kode tahun) = " + digitKedua);
			System.out.println("digit ketiga (tahun sememster) = " + digitKetiga);
			System.out.println("digit keempat (urutan) = " + digitKeempat);

			String nim = prefix + digitKeempat;

			boolean nimSudahDipakai = NimGeneratorSupport.nimSudahDipakai(session, nim, calonMahasiswa);
			HibernateUtil.closeSessionQuietly(session);

			if (nimSudahDipakai) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

			return nim;
		}
	}

}
