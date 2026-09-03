package ais.database.model;

/**
 * Self-test tanpa database untuk format pengecualian NIM pada {@link SettingBiaya}.
 * Jalankan setelah kompilasi dengan:
 * {@code java ais.database.model.SettingBiayaPengecualianSemesterSelfTest}.
 */
public final class SettingBiayaPengecualianSemesterSelfTest {

	private static int gagal;

	private SettingBiayaPengecualianSemesterSelfTest() { }

	private static void cek(boolean kondisi, String pesan) {
		System.out.println((kondisi ? "LULUS  " : "GAGAL  ") + pesan);
		if (!kondisi) gagal++;
	}

	private static void cekTidakValid(String nilai, String pesan) {
		try {
			SettingBiaya.validasiFormatPengecualianMahasiswa(nilai);
			cek(false, pesan);
		} catch (IllegalArgumentException e) {
			cek(true, pesan);
		}
	}

	public static void main(String[] args) {
		SettingBiaya setting = new SettingBiaya();
		setting.setPengecualianMahasiswa("20240001,20240002; 20240003");
		cek(setting.isMahasiswaDikecualikan("20240001", Integer.valueOf(1)),
				"format NIM lama berlaku pada semua semester");
		cek(setting.isMahasiswaDikecualikan("20240003", Integer.valueOf(12)),
				"pemisah titik koma/spasi format lama tetap didukung");

		setting.setPengecualianMahasiswa("20241001:3:5;20241002:8:10");
		cek(setting.isMahasiswaDikecualikan("20241001", Integer.valueOf(3)),
				"batas semester mulai termasuk rentang");
		cek(setting.isMahasiswaDikecualikan("20241001", Integer.valueOf(5)),
				"batas semester sampai termasuk rentang");
		cek(!setting.isMahasiswaDikecualikan("20241001", Integer.valueOf(2)),
				"semester sebelum rentang tidak dikecualikan");
		cek(!setting.isMahasiswaDikecualikan("20241001", Integer.valueOf(6)),
				"semester setelah rentang tidak dikecualikan");
		cek(!setting.isMahasiswaDikecualikan("20241001"),
				"overload tanpa semester tidak menerapkan entri berentang secara berlebihan");

		setting.setPengecualianMahasiswa("20242001,20242002 : 2 : 4");
		cek(setting.isMahasiswaDikecualikan("20242001", Integer.valueOf(9)),
				"format lama dan rentang dapat dicampur");
		cek(setting.isMahasiswaDikecualikan("20242002", Integer.valueOf(4)),
				"spasi di sekitar titik dua dinormalisasi");

		SettingBiaya.validasiFormatPengecualianMahasiswa("");
		SettingBiaya.validasiFormatPengecualianMahasiswa("20240001,20240002;20241001:3:5");
		cekTidakValid("20241001:3", "format rentang kurang satu bagian ditolak");
		cekTidakValid("20241001:a:5", "semester bukan angka ditolak");
		cekTidakValid("20241001:0:5", "semester mulai nol ditolak");
		cekTidakValid("20241001:6:5", "rentang terbalik ditolak");

		System.out.println(gagal == 0 ? "SEMUA INVARIAN PENGECUALIAN NIM TERJAGA"
				: "ADA " + gagal + " INVARIAN YANG DILANGGAR");
		if (gagal > 0) System.exit(1);
	}
}
