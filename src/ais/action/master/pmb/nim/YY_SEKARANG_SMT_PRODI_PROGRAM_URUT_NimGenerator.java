package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Program;

/**
 * Algoritma penomoran NIM (Nomor Induk Mahasiswa) berpola
 * {@code <YY tahun sekarang><1=Ganjil/2=Genap><kode prodi><kode program><urut>}: dua digit terakhir
 * TAHUN BERJALAN (bukan tahun angkatan calon mahasiswa) + kode semester mulai + kode prodi lulus +
 * kode {@link Program} + nomor urut (jumlah digit dari konfigurasi
 * {@code jumlah_digit_gen_nim_mahasiswa}, default 4). Nomor urut dan pengecekan duplikasi
 * didelegasikan ke {@link NimGeneratorSupport}; bila NIM hasil sudah terpakai, dicoba ulang secara
 * rekursif dengan NIM tersebut ditambahkan ke daftar pengecualian.
 *
 * @see NimGeneratorSupport
 */
public class YY_SEKARANG_SMT_PRODI_PROGRAM_URUT_NimGenerator implements NimGenerator {

	/** Varian ringkas {@link #generateNim(BiodataCalonMahasiswa, List)} tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan NIM untuk {@code calonMahasiswa} sesuai pola kelas ini (lihat javadoc kelas).
	 * Mengembalikan {@code "-"} bila calon mahasiswa belum punya prodi lulus. Rekursif: bila NIM
	 * yang dihasilkan ternyata sudah dipakai, dicoba lagi dengan NIM tersebut ditambahkan ke
	 * {@code jumlahPengecualian} agar nomor urut berikutnya melompatinya.
	 *
	 * @param jumlahPengecualian daftar NIM yang harus dianggap sudah terpakai (dimutasi dan diteruskan pada percobaan ulang)
	 * @return NIM yang dihasilkan, atau {@code "-"} bila prodi lulus belum diisi
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {

			Program prog = Common.programs.get(calonMahasiswa.getProgram());

			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			Integer tahunSekarang = Calendar.getInstance().get(Calendar.YEAR);

			String digitPertama = tahunSekarang.toString().substring(2);
			String digit12 = calonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL) ? "1" : "2";
			String digitKedua = calonMahasiswa.getProdiLulus().getKode();
			String digitKetiga = prog == null || prog.getNum() == null ? "_" : prog.getNum().toString();

			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YY_SEKARANG_SMT_PRODI_PROGRAM_URUT_NimGenerator.java:56");

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
