package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma penomoran NIM generik dengan pola {@code YY-JENJANG-PRODI-URUT} (nama kelas mengikuti
 * pola formatnya): {@code <2 digit tahun><kode jenjang><kode prodi><N digit urutan>}. Jumlah digit
 * urutan dapat dikonfigurasi lewat {@code jumlah_digit_gen_nim_mahasiswa} (default 4). Cocok
 * dipakai institusi yang ingin NIM-nya secara eksplisit menyatakan jenjang (D3/S1/S2/dst) dan
 * program studi tanpa komponen angkatan/kapasitas seperti pada generator lain.
 */
public class YY_JENJANG_PRODI_URUT_NimGenerator implements NimGenerator {

	/** Seperti {@link #generateNim(BiodataCalonMahasiswa, List)} tanpa daftar NIM yang harus dihindari. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM baru untuk {@code calonMahasiswa} sesuai pola {@code YY-JENJANG-PRODI-URUT}
	 * (lihat javadoc kelas), menghindari nomor pada {@code jumlahPengecualian}. Rekursif pada
	 * bentrokan nomor.
	 *
	 * @param calonMahasiswa      calon mahasiswa yang akan diberi NIM; harus memiliki program studi
	 *                            lulus agar NIM dapat dihitung (selain itu mengembalikan {@code "-"})
	 * @param jumlahPengecualian  daftar nomor yang harus dihindari, dimutasi langsung saat bentrokan
	 * @return NIM unik sesuai pola tahun-jenjang-prodi-urutan, atau {@code "-"} bila program studi lulus belum diisi
	 */
	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = tahun.toString().substring(2);
			String digitKedua = calonMahasiswa.getProdiLulus().getJenjang().getKode();
			String digitKetiga = calonMahasiswa.getProdiLulus().getKode();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YY_JENJANG_PRODI_URUT_NimGenerator.java:45");

			}
			String prefix = digitPertama + digitKedua + digitKetiga;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahDigit, calonMahasiswa,
					jumlahPengecualian);
			String digitEmpat = NimGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode jenjang) = " + digitKedua);
			System.out.println("digit kedua (kode prodi) = " + digitKetiga);
			System.out.println("digit ketiga (urutan) = " + digitEmpat);

			nim = prefix + digitEmpat;
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
