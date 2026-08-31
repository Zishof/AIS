package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Perkuliahan;

/**
 * Algoritma NIM pola umum "PRODI_YY_SMT_URUT": {@code [kode program studi kelulusan][2 digit
 * terakhir tahun angkatan][1 digit kode semester mulai: "1"=Ganjil, "2"=Genap][N digit nomor
 * urut]}, mis. {@code TI26100 07}. Jumlah digit nomor urut dapat diatur lewat konfigurasi
 * {@code jumlah_digit_gen_nim_mahasiswa} (default 4). Nomor urut berikutnya dan pengecekan
 * bentrok didelegasikan ke {@link NimGeneratorSupport} berdasarkan prefix (prodi+tahun+semester)
 * yang sudah terbentuk. Bila {@code calonMahasiswa.getProdiLulus()} belum diisi, NIM dikembalikan
 * sebagai {@code "-"}. Bukan spesifik satu institusi — dipakai sebagai pola default yang
 * membedakan penomoran per semester masuk (ganjil/genap) selain per tahun.
 */
public class PRODI_YY_SMT_URUT_NimGenerator implements NimGenerator {

	/** Menghasilkan NIM baru tanpa daftar pengecualian awal — lihat {@link #generateNim(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM berformat {@code kode prodi lulus+2 digit tahun+1 digit semester+N digit
	 * urut}, menghindari nilai yang ada di {@code jumlahPengecualian} maupun yang sudah dipakai
	 * mahasiswa lain di database (lewat {@link NimGeneratorSupport}); mencoba ulang secara
	 * rekursif bila terjadi bentrok.
	 *
	 * @param calonMahasiswa     calon mahasiswa target; {@code prodiLulus}, {@code tahun}, dan
	 *                           {@code semesterMulai}-nya menentukan bagian awal NIM
	 * @param jumlahPengecualian NIM-NIM yang sudah dicoba dan diketahui bentrok, dihindari pada
	 *                           percobaan berikutnya (diperbarui di tempat)
	 * @return NIM baru yang belum pernah dipakai, atau {@code "-"} bila {@code prodiLulus} belum diisi
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {

			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = calonMahasiswa.getProdiLulus().getKode();
			String digit12 = tahun.toString().substring(2);
			String digitKedua = calonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL) ? "1" : "2";

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/PRODI_YY_SMT_URUT_NimGenerator.java:48");

			}
			String prefix = digitPertama + digit12 + digitKedua;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahDigit, calonMahasiswa,
					jumlahPengecualian);
			String digitEmpat = NimGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

			System.out.println("digit 1 (kode prodi)  = " + digitPertama);
			System.out.println("digit 2 (kode tahun masuk) = " + digit12);
			System.out.println("digit 3 (kode semester) = " + digitKedua);
			System.out.println("digit 4 (urutan) = " + digitEmpat);

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
