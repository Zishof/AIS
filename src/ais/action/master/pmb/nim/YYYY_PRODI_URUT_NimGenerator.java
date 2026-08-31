package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Algoritma penomoran NIM berpola {@code [prefix_pmb]<YYYY tahun angkatan><kode prodi><urut>}:
 * konfigurasi {@code prefix_pmb} (opsional) diikuti tahun angkatan CALON MAHASISWA empat digit penuh
 * + kode prodi lulus + nomor urut (jumlah digit dari konfigurasi
 * {@code jumlah_digit_gen_nim_mahasiswa}, default 4). Nomor urut dan pengecekan duplikasi
 * didelegasikan ke {@link NimGeneratorSupport}; bila NIM hasil sudah terpakai, dicoba ulang secara
 * rekursif dengan NIM tersebut ditambahkan ke daftar pengecualian.
 *
 * @see NimGeneratorSupport
 */
public class YYYY_PRODI_URUT_NimGenerator implements NimGenerator {

	/** Varian ringkas {@link #generateNim(BiodataCalonMahasiswa, List)} tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM untuk {@code calonMahasiswa} sesuai pola kelas ini (lihat javadoc kelas).
	 * Mengembalikan {@code "-"} bila calon mahasiswa belum punya prodi lulus. Rekursif: bila NIM
	 * yang dihasilkan ternyata sudah dipakai, dicoba lagi dengan NIM tersebut ditambahkan ke
	 * {@code jumlahPengecualian}.
	 *
	 * @param jumlahPengecualian daftar NIM yang harus dianggap sudah terpakai (dimutasi dan diteruskan pada percobaan ulang)
	 * @return NIM yang dihasilkan, atau {@code "-"} bila prodi lulus belum diisi
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {
			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = tahun.toString();

			String digitKedua = calonMahasiswa.getProdiLulus().getKode();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YYYY_PRODI_URUT_NimGenerator.java:45");

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
