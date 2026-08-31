package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Perkuliahan;

/**
 * Algoritma penomoran NIM khusus institusi "IBS": menyusun NIM 7 digit dari kode tahun masuk
 * (4 digit penuh), kode strata (1=S1, 2=S2, 3 untuk jenjang lain), kode program studi, kode
 * program (1=Reguler/Pascasarjana, 2=Ekstensi, 3=Karyawan, 4=Internasional, 5=lainnya), penanda
 * semester ganjil/genap (1/2), dan nomor urut 3 digit dari {@link NimGeneratorSupport}. Bila
 * program studi kelulusan calon mahasiswa tidak diketahui, mengembalikan {@code "-"}.
 */
public class IbsNimGenerator implements NimGenerator {

	/** Menghasilkan NIM tanpa daftar pengecualian awal; lihat {@link #generateNim(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menyusun NIM dari kode tahun, strata, program studi, program, dan semester, lalu menambahkan
	 * nomor urut berikutnya (dicek terhadap {@code jumlahPengecualian} dan NIM yang sudah dipakai
	 * di database). Bila NIM hasil sudah terpakai, method memanggil dirinya sendiri secara
	 * rekursif dengan NIM tersebut ditambahkan ke {@code jumlahPengecualian} agar nomor urut
	 * berikutnya dicoba.
	 *
	 * @param calonMahasiswa    data calon mahasiswa (tahun masuk, prodi lulus, program, semester)
	 * @param jumlahPengecualian daftar NIM yang harus dilewati (diisi bertambah saat rekursi)
	 * @return NIM 7 digit yang belum terpakai, atau {@code "-"} bila prodi lulus tidak diketahui
	 */
	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = tahun.toString();

			String digitKedua = calonMahasiswa.getProdiLulus().getJenjang().getNama().equalsIgnoreCase("S1") ? "1"
					: calonMahasiswa.getProdiLulus().getJenjang().getNama().equalsIgnoreCase("S2") ? "2" : "3";

			// String digitKetiga = calonMahasiswa.getProdiLulus().getFakultas().getKode();

			String digitKeempat = calonMahasiswa.getProdiLulus().getKode();

			String digitKelima = calonMahasiswa.getProgram().equalsIgnoreCase("Reguler")
					|| calonMahasiswa.getProgram().equalsIgnoreCase("Pascasarjana")
							? "1"
							: calonMahasiswa.getProgram().equalsIgnoreCase("Ekstensi") ? "2"
									: calonMahasiswa.getProgram().equalsIgnoreCase("Karyawan") ? "3"
											: calonMahasiswa.getProgram().equalsIgnoreCase("Internasional") ? "4" : "5";

			String digitKeEnam = calonMahasiswa.getJenisSemester().equals(Perkuliahan.GANJIL) ? "1" : "2";

			String prefix = digitPertama + digitKedua + digitKeempat + digitKelima + digitKeEnam;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, 3, calonMahasiswa,
					jumlahPengecualian);
			String digitTujuh = NimGeneratorSupport.leftPadNomor(nomorUrut, 3);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode strata) = " + digitKedua);
			// System.out.println("digit ketiga (kode fakultas) = " + digitKetiga);
			System.out.println("digit keempat (kode prodi) = " + digitKeempat);
			System.out.println("digit kelima (kode program) = " + digitKelima);
			System.out.println("digit keenam (ganjil genap) = " + digitKeEnam);
			System.out.println("digit keenam (nomor urut) = " + digitTujuh);

			nim = digitPertama + digitKedua + digitKeempat + digitKelima + digitKeEnam + digitTujuh;

			boolean nimSudahDipakai = NimGeneratorSupport.nimSudahDipakai(session, nim, calonMahasiswa);
			HibernateUtil.closeSessionQuietly(session);

			if (nimSudahDipakai) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

		}

		return nim;
	}

}
