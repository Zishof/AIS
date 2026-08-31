package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Program;

/**
 * Algoritma penomoran NIM dengan pola {@code YY-PROGRAM-PRODI-URUT}: NIM disusun dari
 * {@code 2 digit tahun masuk + kode numerik program (jenjang/kelas kuliah) + kode program studi +
 * nomor urut} yang panjangnya dapat dikonfigurasi lewat {@code jumlah_digit_gen_nim_mahasiswa}
 * (default 4 digit). Pembangkitan nomor urut dan pengecekan pemakaian didelegasikan ke
 * {@link NimGeneratorSupport}, sehingga logika ini dapat dipakai ulang oleh generator lain yang
 * formatnya serupa.
 */
public class YY_PROGRAM_PRODI_URUT_NimGenerator implements NimGenerator {

	/** Seperti {@link #generateNim(BiodataCalonMahasiswa, List)}, tanpa daftar pengecualian awal. */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Membangkitkan NIM: menyusun prefix dari 2 digit tahun masuk, kode numerik
	 * {@link Program program kuliah} calon mahasiswa (dicari dari {@link Common#programs}, diisi
	 * {@code "_"} bila tidak ditemukan), dan kode program studi lulus, lalu menyisipkan nomor urut
	 * berikutnya (dihitung via {@link NimGeneratorSupport#nomorUrutBerikutnya}, memperhitungkan
	 * kandidat yang sudah bentrok di {@code jumlahPengecualian}). Bila NIM hasil ternyata sudah
	 * dipakai (dicek via {@link NimGeneratorSupport#nimSudahDipakai}), nomor tersebut ditambahkan
	 * ke {@code jumlahPengecualian} dan method memanggil dirinya sendiri secara rekursif.
	 * Mengembalikan {@code "-"} bila calon mahasiswa belum punya program studi lulus.
	 *
	 * @param jumlahPengecualian NIM kandidat yang sudah terbukti bentrok pada percobaan sebelumnya
	 * @return NIM yang belum dipakai mahasiswa manapun, atau {@code "-"} bila prodi lulus belum ditentukan
	 */
	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {

		String nim = "-";

		if (calonMahasiswa.getProdiLulus() != null) {

			Program prog = Common.programs.get(calonMahasiswa.getProgram());

			Session session = HibernateUtil.openSession();

			Integer tahun = calonMahasiswa.getTahun();

			String digitPertama = tahun.toString().substring(2);
			String digitKedua = prog == null || prog.getNum() == null ? "_" : prog.getNum().toString();
			String digitKetiga = calonMahasiswa.getProdiLulus().getKode();
			
			Integer jumlahDigit = 4;
			try {
				jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_gen_nim_mahasiswa", "4").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/YY_PROGRAM_PRODI_URUT_NimGenerator.java:50");

			}
			String prefix = digitPertama + digitKedua + digitKetiga;
			long nomorUrut = NimGeneratorSupport.nomorUrutBerikutnya(session, prefix, jumlahDigit, calonMahasiswa,
					jumlahPengecualian);
			String digitEmpat = NimGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

			System.out.println("digit pertama (kode tahun masuk) = " + digitPertama);
			System.out.println("digit kedua (kode prodi) = " + digitKedua);
			System.out.println("digit kedua (kode program) = " + digitKetiga);
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
