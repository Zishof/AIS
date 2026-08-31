package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import ais.database.model.BiodataCalonMahasiswa;

/**
 * Implementasi {@link NoRegGenerator} generik ("ID") untuk nomor registrasi PMB: 4 digit tahun
 * berjalan diikuti 8 digit acak. Berbeda dari generator lain di paket ini (mis.
 * {@code BorneoLestariNoRegGenerator}) yang menyusun nomor dari kode institusi/urut sekuensial,
 * generator ini TIDAK memeriksa keunikan terhadap data yang sudah ada maupun terhadap parameter
 * pengecualian yang diberikan — bagian acak 8 digit ({@code ThreadLocalRandom}) diasumsikan cukup
 * jarang bertabrakan, sehingga cocok dipakai sebagai fallback generik saat institusi tidak punya
 * skema penomoran registrasi khusus.
 */
public class IDNoRegGenerator implements NoRegGenerator {

	/** Menghasilkan nomor registrasi tanpa daftar pengecualian; mendelegasikan ke varian dengan daftar kosong. */
	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	/**
	 * Menghasilkan nomor registrasi berformat {@code [4 digit tahun berjalan][8 digit acak]}.
	 * Parameter {@code jumlahPengecualian} diterima untuk memenuhi kontrak {@link NoRegGenerator}
	 * namun TIDAK dipakai — tidak ada pengecekan keunikan terhadap daftar tersebut di implementasi ini.
	 *
	 * @param jumlahPengecualian daftar nomor registrasi yang seharusnya dihindari (tidak dipakai)
	 * @param biodataCalonMahasiswa data calon mahasiswa (tidak dipakai untuk membentuk nomor)
	 * @return nomor registrasi 12 digit (4 digit tahun + 8 digit acak)
	 */
	@Override
	public String generateNoReg(List<String> jumlahPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		String digitPertama = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "";

		String digitKedua = "000000000000000" + ThreadLocalRandom.current().nextLong(0, 99999999);
		digitKedua = digitKedua.substring(digitKedua.length() - 8);

		System.out.println("digit pertama (kode prodi) = " + digitPertama);
		System.out.println("digit kedua (kode tahun) = " + digitKedua);

		String noReg = digitPertama + digitKedua;

		return noReg;
	}

}
