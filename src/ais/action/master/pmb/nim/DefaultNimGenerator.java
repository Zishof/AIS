package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

public class DefaultNimGenerator implements NimGenerator {

	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	// generate NIM
	/*
	 * contoh NIM: 106091002858 digit pertama = jenjang (dalam contoh di atas:
	 * 1) digit kedua sampai ketiga = tahun angkatan (dalam contoh di atas: 06)
	 * digit keempat sampai ke kelima = fakultas (dalam contoh di atas: 09)
	 * digit keenam sampai ke ketujuh = prodi (dalam contoh di atas: 10) digit
	 * kedelapan sampai seterusnya = nomor urut mahasiswa tiap prodi (dalam
	 * contoh di atas: 02858). Dalam tiap tahun angkatan nomor urut mahasiswa
	 * akan diulang lagi dari nomor 1
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
