package ais.action.master.pmb.noujian;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;

/**
 * Implementasi algoritma penomoran nomor ujian calon mahasiswa (PMB) berbasis <b>prefix
 * tetap + urutan bertambah per tahun</b>. Prefix diambil dari konfigurasi
 * {@code prefix_no_ujian_calon_mhs} (default {@code "EXAM."}) dan jumlah digit urutan dari
 * konfigurasi {@code jumlah_digit_no_ujian_calon_mhs} (default 3). Nomor urut dihitung dari
 * jumlah {@link BiodataCalonMahasiswa} aktif pada tahun yang sama yang nomor ujiannya sudah diawali
 * prefix tersebut, sehingga penomoran berjalan berkelanjutan per tahun per prefix, bukan global.
 */
public class PrefixNoUjianGenerator implements NoUjianGenerator {

	/** Seperti {@link #generateNoUjian(BiodataCalonMahasiswa, List)} tanpa daftar nomor yang harus dihindari. */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		return generateNoUjian(biodataCalonMahasiswa, new ArrayList<String>());
	}

	/**
	 * Menghasilkan nomor ujian baru ({@code prefix + urutan berpadding nol}) untuk
	 * {@code biodataCalonMahasiswa}, unik dalam lingkup tahun dan prefix konfigurasi saat ini.
	 * Rekursif: bila nomor yang dihasilkan sudah terpakai, nomor tersebut ditambahkan ke
	 * {@code noRegPengecualian} dan method memanggil dirinya sendiri untuk mencoba nomor
	 * berikutnya — tidak ada batas percobaan eksplisit.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa yang akan diberi nomor ujian; menentukan tahun
	 *                               sebagai lingkup keunikan
	 * @param noRegPengecualian     daftar nomor yang harus dihindari, dimutasi langsung oleh
	 *                               method ini saat terjadi bentrokan
	 * @return nomor ujian unik dalam lingkup tahun + prefix konfigurasi
	 * @throws Exception diteruskan dari kegagalan akses konfigurasi/database
	 */
	@Override
	public String generateNoUjian(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> noRegPengecualian)
			throws Exception {
		Integer tahun = biodataCalonMahasiswa.getTahun();
		String digitPertama = Common.getKonfigurasi("prefix_no_ujian_calon_mhs", "EXAM.").getNilai();
		Integer jumlahDigit = 3;
		try {
			jumlahDigit = Integer.parseInt(Common.getKonfigurasi("jumlah_digit_no_ujian_calon_mhs", "3").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/noujian/PrefixNoUjianGenerator.java:30");

		}

		Session session = HibernateUtil.currentSession();
		long nomorUrut = NoUjianGeneratorSupport.nomorUrutBerikutnya(session, digitPertama, jumlahDigit,
				biodataCalonMahasiswa, noRegPengecualian);
		String digitKedua = NoUjianGeneratorSupport.leftPadNomor(nomorUrut, jumlahDigit);

		System.out.println("digit pertama (kode prefix) = " + digitPertama);
		System.out.println("digit kedua (kode urutan) = " + digitKedua);

		String noReg = digitPertama + digitKedua;

		boolean nomorSudahDipakai = NoUjianGeneratorSupport.nomorSudahDipakai(session, noReg,
				biodataCalonMahasiswa);
		if (nomorSudahDipakai) {
			noRegPengecualian.add(noReg);
			return generateNoUjian(biodataCalonMahasiswa, noRegPengecualian);
		}

		return noReg;
	}

}
