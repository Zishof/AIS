package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Jurusan;

public class YY_JENJANG_PRODI_STATUS_URUT_NimGenerator implements NimGenerator {

	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = tahun.toString().substring(2);
			String digitKedua = calonMahasiswa.getProdiLulus().getJenjang().getKode();
			String digitKetiga = ambilKodeProdi(calonMahasiswa.getProdiLulus());

			String digitKetigaLagi = calonMahasiswa.getMerupakanPindahan() ? "2" : "1";
			validasiKomponenNim(digitPertama, "tahun", calonMahasiswa);
			validasiKomponenNim(digitKedua, "jenjang", calonMahasiswa);
			validasiKomponenNim(digitKetiga, "prodi", calonMahasiswa);
			validasiKomponenNim(digitKetigaLagi, "status", calonMahasiswa);

			Session session = HibernateUtil.openSession();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YY_JENJANG_PRODI_STATUS_URUT_NimGenerator.java:48");

			}
			String prefix = digitPertama + digitKedua + digitKetiga + digitKetigaLagi;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahDigit, calonMahasiswa,
					jumlahPengecualian);
			String digitEmpat = NimGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

			System.out.println("pindahan = " + calonMahasiswa.getMerupakanPindahan() + " nomorUrut " + nomorUrut
					+ " jumlahPengecualian " + jumlahPengecualian);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode jenjang) = " + digitKedua);
			System.out.println("digit kedua (kode prodi) = " + digitKetiga);
			System.out.println("digit kedua (status) = " + digitKetigaLagi);
			System.out.println("digit ketiga (urutan) = " + digitEmpat);

			nim = prefix + digitEmpat;
			validasiNim(nim, calonMahasiswa);

			boolean nimSudahDipakai = NimGeneratorSupport.nimSudahDipakai(session, nim, calonMahasiswa);
			HibernateUtil.closeSessionQuietly(session);

			if (nimSudahDipakai) {
				jumlahPengecualian.add(nim);
				return generateNim(calonMahasiswa, jumlahPengecualian);
			}

		}

		return nim;
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
		if (nim == null || nim.trim().isEmpty() || nim.indexOf('-') >= 0 || nim.indexOf('_') >= 0) {
			throw new IllegalArgumentException("Format NIM tidak valid untuk "
					+ (calonMahasiswa == null ? "" : calonMahasiswa.getNama()) + ": " + nim);
		}
	}

	private void validasiKomponenNim(String nilai, String label, BiodataCalonMahasiswa calonMahasiswa) {
		if (nilai == null || nilai.trim().isEmpty() || nilai.indexOf('-') >= 0 || nilai.indexOf('_') >= 0) {
			throw new IllegalArgumentException("Komponen " + label + " untuk generate NIM belum valid"
					+ (calonMahasiswa == null ? "" : " pada " + calonMahasiswa.getNama()) + ": " + nilai);
		}
	}

}
