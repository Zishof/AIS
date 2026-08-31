package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Pembangkit NIM dengan format {@code [prefix_pmb]+YY+KODEPRODI+URUT}, mis. {@code "26TI0007"}
 * (dengan prefiks konfigurasi {@code prefix_pmb} bila diisi). Bagian {@code URUT} dihitung lewat
 * helper bersama {@link NimGeneratorSupport#nomorUrutBerikutnya} berbasis prefiks
 * tahun+kode prodi, dipadatkan sejumlah digit sesuai konfigurasi
 * {@code jumlah_digit_gen_nim_mahasiswa} (default 4). Bila calon mahasiswa belum memiliki prodi
 * lulus, dikembalikan {@code "-"}. Keunikan nomor hasil diperiksa lewat
 * {@link NimGeneratorSupport#nimSudahDipakai}; bila bentrok, dibangkitkan ulang secara rekursif
 * dengan nomor tersebut ditambahkan ke daftar pengecualian.
 */
public class YY_PRODI_URUT_NimGenerator implements NimGenerator {

	/** Membangkitkan NIM baru untuk {@code calonMahasiswa} tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan NIM berformat {@code [prefix_pmb]+YY+KODEPRODI+URUT}, menghindari nomor
	 * pada {@code jumlahPengecualian} maupun yang sudah tersimpan; mengulang secara rekursif
	 * bila terjadi bentrok. Mengembalikan {@code "-"} bila calon mahasiswa belum memiliki prodi
	 * lulus.
	 *
	 * @param calonMahasiswa     data calon mahasiswa yang akan diberi NIM
	 * @param jumlahPengecualian daftar NIM yang harus dihindari, diperbarui di tempat saat
	 *                           terjadi bentrok
	 * @return NIM baru yang belum dipakai, atau {@code "-"} bila prodi lulus belum ditentukan
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = tahun.toString().substring(2);

			String digitKedua = calonMahasiswa.getProdiLulus().getKode();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YY_PRODI_URUT_NimGenerator.java:45");

			}
			String prefix = Common.getKonfigurasi("prefix_pmb", "").getNilai() + digitPertama + digitKedua;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahDigit, calonMahasiswa,
					jumlahPengecualian);
			String digitKetiga = NimGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode prodi) = " + digitKedua);
			System.out.println("digit ketiga (urutan) = " + digitKetiga);

			nim = prefix + digitKetiga;
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
