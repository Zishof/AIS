package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Pembangkit NIM dengan format {@code YY+KODEPRODI+KODESTATUSAWAL+URUT}, mis.
 * {@code "26TIP0007"}. Digit status ({@code KODESTATUSAWAL}) diambil dari status awal mahasiswa
 * (mis. status pindahan) dan bernilai {@code "0"} bila tidak ditentukan. Prefiks nomor (tahun +
 * prodi + status), ditambah prefiks konfigurasi {@code prefix_pmb} bila ada, dipakai bersama
 * {@link NimGeneratorSupport#nomorUrutBerikutnya} untuk menghitung nomor urut berikutnya secara
 * seragam dengan generator lain yang memakai helper yang sama; jumlah digit urut dapat diatur
 * lewat konfigurasi {@code jumlah_digit_gen_nim_mahasiswa} (default 4). Bila nomor hasil bentrok
 * (dicek lewat {@link NimGeneratorSupport#nimSudahDipakai}), dibangkitkan ulang secara rekursif.
 */
public class YY_PRODI_STATUS_PINDAHAN_URUT_NimGenerator implements NimGenerator {

	/** Membangkitkan NIM baru untuk {@code calonMahasiswa} tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan NIM berformat {@code YY+KODEPRODI+KODESTATUSAWAL+URUT}, menghindari nomor
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

			String digitKetiga = calonMahasiswa.getStatusAwalMahasiswa() == null ? "0"
					: calonMahasiswa.getStatusAwalMahasiswa().getKode();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YY_PRODI_STATUS_PINDAHAN_URUT_NimGenerator.java:49");

			}

			String prefix = Common.getKonfigurasi("prefix_pmb", "").getNilai() + digitPertama + digitKedua
					+ digitKetiga;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahDigit, calonMahasiswa,
					jumlahPengecualian);
			String digitEmpat = NimGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

			System.out.println("pindahan = " + calonMahasiswa.getMerupakanPindahan() + " nomorUrut " + nomorUrut
					+ " jumlahPengecualian " + jumlahPengecualian);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode prodi) = " + digitKedua);
			System.out.println("digit ketiga (status pindahan) = " + digitKetiga);
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
