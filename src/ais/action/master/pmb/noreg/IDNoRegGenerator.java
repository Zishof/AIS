package ais.action.master.pmb.noreg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import ais.database.model.BiodataCalonMahasiswa;

public class IDNoRegGenerator implements NoRegGenerator {

	@Override
	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		return generateNoReg(new ArrayList<String>(), biodataCalonMahasiswa);
	}

	// generate NIM
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
