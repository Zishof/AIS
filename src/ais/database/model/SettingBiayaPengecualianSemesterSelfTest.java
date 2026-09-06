package ais.database.model;

/**
 * Self-test tanpa database untuk format pengecualian NIM pada {@link SettingBiaya}.
 * Jalankan setelah kompilasi dengan:
 * {@code java ais.database.model.SettingBiayaPengecualianSemesterSelfTest}.
 */
public final class SettingBiayaPengecualianSemesterSelfTest {

	/** Penghitung jumlah asersi yang gagal selama satu jalannya {@link #main(String[])}. */
	private static int gagal;

	/** Kelas utilitas: konstruktor privat, tidak dimaksudkan untuk diinstansiasi. */
	private SettingBiayaPengecualianSemesterSelfTest() { }

	/**
	 * Mencetak hasil satu asersi ({@code LULUS}/{@code GAGAL}) ke stdout dan menambah
	 * {@link #gagal} bila {@code kondisi} bernilai false.
	 *
	 * @param kondisi hasil kondisi yang diharapkan true.
	 * @param pesan   deskripsi invarian yang diuji, dicetak bersama status.
	 */
	private static void cek(boolean kondisi, String pesan) {
		System.out.println((kondisi ? "LULUS  " : "GAGAL  ") + pesan);
		if (!kondisi) gagal++;
	}

	/**
	 * Menguji bahwa {@link SettingBiaya#validasiFormatPengecualianMahasiswa(String)}
	 * MENOLAK {@code nilai} dengan melempar {@link IllegalArgumentException}; dicatat
	 * LULUS bila exception tersebut terlempar, GAGAL bila tidak (format yang seharusnya
	 * invalid malah diterima).
	 *
	 * @param nilai nilai format pengecualian yang diharapkan ditolak validasi.
	 * @param pesan deskripsi invarian yang diuji.
	 */
	private static void cekTidakValid(String nilai, String pesan) {
		try {
			SettingBiaya.validasiFormatPengecualianMahasiswa(nilai);
			cek(false, pesan);
		} catch (IllegalArgumentException e) {
			cek(true, pesan);
		}
	}

	/**
	 * Menjalankan seluruh skenario uji format pengecualian NIM pada {@link SettingBiaya}:
	 * format lama (daftar NIM dipisah koma/titik koma, berlaku semua semester), format
	 * baru berentang ({@code NIM:semesterMulai:semesterSampai}, batas inklusif),
	 * campuran kedua format, normalisasi spasi di sekitar pemisah, serta penolakan
	 * format tidak valid (bagian kurang, semester bukan angka, semester nol, rentang
	 * terbalik) oleh {@link SettingBiaya#validasiFormatPengecualianMahasiswa(String)}.
	 * Mencetak ringkasan ke stdout dan keluar dengan status non-nol ({@link System#exit(int)})
	 * bila ada invarian yang gagal — cocok dipakai sebagai gerbang CI sederhana.
	 *
	 * @param args tidak dipakai.
	 */
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
