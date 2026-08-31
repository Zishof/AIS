package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Implementasi {@link NimGenerator} baku (fallback) yang dipakai institusi tanpa algoritma
 * penomoran khusus. Format NIM (contoh {@code 106091002858}): digit 1 = kode jenjang; digit 2 =
 * kode program (dari {@code programNIM} bila diisi, jika tidak dari {@code program}); digit 3–4 =
 * 2 digit terakhir tahun angkatan; digit 5–6 = kode fakultas dari program studi kelulusan; digit
 * 7–8 = kode program studi kelulusan; digit 9 dst. = 5 digit nomor urut auto-increment yang mulai
 * dari 1 lagi setiap tahun angkatan berganti. Penghitungan nomor urut berikutnya dan pengecekan
 * pemakaian NIM didelegasikan ke helper bersama {@link NimGeneratorSupport}; tabrakan ditangani
 * rekursif via daftar pengecualian.
 */
public class DefaultNimGenerator implements NimGenerator {

	/** Menghasilkan NIM tanpa daftar pengecualian; mendelegasikan ke varian dengan daftar kosong. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM baku 12-digit (lihat format di javadoc kelas) untuk calon mahasiswa,
	 * menggunakan {@link NimGeneratorSupport} untuk nomor urut dan verifikasi keunikan.
	 *
	 * @param calonMahasiswa      data calon mahasiswa, sumber jenjang, program, tahun, dan prodi kelulusan
	 * @param jumlahPengecualian  daftar NIM yang harus dihindari, dimodifikasi di tempat saat rekursi
	 * @return NIM baku 12-digit yang belum terpakai
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa,
			List<String> jumlahPengecualian) {
		String digitPertama = calonMahasiswa == null ? "" : calonMahasiswa
				.getJenjang() == null ? "" : calonMahasiswa.getJenjang()
				.getKode();

		Integer digitKedua = 0;
		if (calonMahasiswa.getProgramNIM() == null) {
			digitKedua = Common.programs.get(calonMahasiswa.getProgram())
					.getNum();
		} else {
			digitKedua = Common.programs.get(calonMahasiswa.getProgramNIM())
					.getNum();
		}

		Integer tahun = calonMahasiswa.getTahun();

		String digitKetigaKeempat = tahun.toString().substring(2);

		String kodeFakultas = calonMahasiswa == null
				|| calonMahasiswa.getProdiLulus() == null
				|| calonMahasiswa.getProdiLulus().getFakultas() == null ? "-1"
				: calonMahasiswa.getProdiLulus().getFakultas().getKode(); // kode
		// fakultas

		String digitKelimaKeenam = "00000" + kodeFakultas;
		digitKelimaKeenam = digitKelimaKeenam.substring(digitKelimaKeenam
				.length() - 2);

		String kodeProdi = calonMahasiswa.getProdiLulus().getKode(); // kode
		// prodi

		String digitKetujuhKedelapan = "00000" + kodeProdi;
		digitKetujuhKedelapan = digitKetujuhKedelapan
				.substring(digitKetujuhKedelapan.length() - 2);

		Session session = HibernateUtil.openSession();
		String prefix = digitPertama + digitKedua + digitKetigaKeempat
				+ digitKelimaKeenam.toString() + digitKetujuhKedelapan;
		long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, 5,
				calonMahasiswa, jumlahPengecualian);
		String digitKesembilandst = NimGeneratorSupport.leftPadNomor(nomorUrut, 5);

		System.out.println("digit pertama (kode jenjang) = " + digitPertama);
		System.out.println("digit kedua (kode program) = " + digitKedua);
		System.out.println("digit ketiga keempat (tahun angkatan) = "
				+ digitKetigaKeempat);
		System.out.println("digit kelima keenam (kode fakultas) = "
				+ digitKelimaKeenam);
		System.out.println("digit ketujuh kedelapan (kode prodi) = "
				+ digitKetujuhKedelapan);
		System.out.println("digit kesembilan dst (auto increment) = "
				+ digitKesembilandst);

		String nim = prefix + digitKesembilandst;
		boolean nimSudahDipakai = NimGeneratorSupport.nimSudahDipakai(session, nim, calonMahasiswa);

		HibernateUtil.closeSessionQuietly(session);

		if (nimSudahDipakai) {
			jumlahPengecualian.add(nim);
			return generateNim(calonMahasiswa, jumlahPengecualian);
		}

		return nim;
	}

}
