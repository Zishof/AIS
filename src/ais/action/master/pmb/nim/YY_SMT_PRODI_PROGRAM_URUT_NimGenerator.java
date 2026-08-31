package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;

/**
 * Algoritma pembangkit NIM dengan lima komponen sesuai nama kelas: 2 digit tahun angkatan, 1
 * digit kode semester (1=Ganjil, 2=Genap), kode prodi lulus, kode {@link Program} (atau
 * {@code "_"} bila program tidak dikenal/tidak memiliki nomor), dan digit urutan mahasiswa aktif
 * pada kombinasi (tahun, program, semester mulai, prodi) yang sama (panjang dari konfigurasi
 * {@code jumlah_digit_gen_nim_mahasiswa}, default 4). Mengembalikan {@code "-"} bila calon
 * mahasiswa belum memiliki prodi lulus.
 */
public class YY_SMT_PRODI_PROGRAM_URUT_NimGenerator implements NimGenerator {

	/** @return NIM baru untuk {@code calonMahasiswa}, lihat {@link #generateNim(BiodataCalonMahasiswa, List)}. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM format tahun+semester+prodi+program+urutan; mengembalikan {@code "-"} bila
	 * {@code calonMahasiswa} belum memiliki prodi lulus. Bila hasil sudah dipakai, NIM tersebut
	 * ditambahkan ke {@code jumlahPengecualian} dan method memanggil dirinya sendiri secara
	 * rekursif untuk mencoba urutan berikutnya.
	 *
	 * @param calonMahasiswa      data calon mahasiswa yang akan diberi NIM
	 * @param jumlahPengecualian  NIM yang harus dihindari (diperbarui di tempat sebagai akumulator rekursi)
	 * @return NIM baru yang belum pernah dipakai, atau {@code "-"} bila prodi lulus belum diisi
	 */
	// generate NIM
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {

			Program prog = Common.programs.get(calonMahasiswa.getProgram());

			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = tahun.toString().substring(2);
			String digit12 = calonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL) ? "1" : "2";
			String digitKedua = calonMahasiswa.getProdiLulus().getKode();
			String digitKetiga = prog == null || prog.getNum() == null ? "_" : prog.getNum().toString();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YY_SMT_PRODI_PROGRAM_URUT_NimGenerator.java:53");

			}
			String prefix = digitPertama + digit12 + digitKedua + digitKetiga;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahDigit, calonMahasiswa,
					jumlahPengecualian);
			String digitEmpat = NimGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

			System.out.println("digit 1 (kode tahun masuk) = " + digitPertama);
			System.out.println("digit 2 (kode semester) = " + digit12);
			System.out.println("digit 3 (kode prodi) = " + digitKedua);
			System.out.println("digit 4 (kode program) = " + digitKetiga);
			System.out.println("digit 5 (urutan) = " + digitEmpat);

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
