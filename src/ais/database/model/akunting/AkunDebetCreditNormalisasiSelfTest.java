package ais.database.model.akunting;

/**
 * Test harness tanpa JUnit dan tanpa basis data untuk normalisasi sandi
 * {@code debit_credit} di {@link Akun#setDebetCredit(Integer)}.
 *
 * <p>Melindungi perbaikan integritas data 3 Sep 2026: sandi legacy {@code 2} (ditulis jalur
 * impor Accurate dan API {@code akunSimpan} untuk akun kredit) pernah lolos apa adanya ke
 * kolom {@code debit_credit}, membuat {@code LaporanKeuanganCoaHelper} mengalikan saldo akun
 * itu dengan {@code +2} alih-alih {@code -1} pada laporan keuangan cetak. Setter sekarang
 * menormalkan {@code 2} menjadi {@link Akun#CREDIT} sebelum menyimpannya ke field -- baris ini
 * memastikan regresi itu tidak diam-diam kembali.</p>
 *
 * <p>Jalankan: {@code java ais.database.model.akunting.AkunDebetCreditNormalisasiSelfTest}.
 * Keluar dengan kode 1 dan menyebut invarian yang dilanggar bila ada yang rusak.</p>
 */
public final class AkunDebetCreditNormalisasiSelfTest {

	private AkunDebetCreditNormalisasiSelfTest() { }

	private static int gagal = 0;

	private static void cek(boolean nilai, String pesan) {
		if (nilai) {
			System.out.println("LULUS  " + pesan);
		} else {
			gagal++;
			System.out.println("GAGAL  " + pesan);
		}
	}

	public static void main(String[] args) {
		cek(Integer.valueOf(1).equals(Akun.DEBET), "konstanta Akun.DEBET tetap 1");
		cek(Integer.valueOf(-1).equals(Akun.CREDIT), "konstanta Akun.CREDIT tetap -1");

		Akun akun = new Akun();

		// Inti perbaikan: sandi legacy 2 (Accurate/akunSimpan) HARUS ternormalkan jadi -1,
		// bukan lolos apa adanya -- ini yang dulu menggandakan & membalik tanda saldo di
		// LaporanKeuanganCoaHelper (nilai = saldo * akun.getDebetCredit()).
		akun.setDebetCredit(Integer.valueOf(2));
		cek(Akun.CREDIT.equals(akun.getDebetCredit()),
				"setDebetCredit(2) ternormalkan menjadi Akun.CREDIT (-1), bukan lolos sebagai 2");

		akun.setDebetCredit(Akun.DEBET);
		cek(Akun.DEBET.equals(akun.getDebetCredit()), "setDebetCredit(DEBET) tetap 1, tidak ikut ternormalkan");

		akun.setDebetCredit(Akun.CREDIT);
		cek(Akun.CREDIT.equals(akun.getDebetCredit()), "setDebetCredit(-1) tetap -1 (sandi kanonik)");

		akun.setDebetCredit(null);
		cek(Akun.CREDIT.equals(akun.getDebetCredit()),
				"setDebetCredit(null) tetap dipaksa getter menjadi CREDIT (-1) -- perilaku lama tidak berubah");

		System.out.println(gagal == 0
				? "SEMUA INVARIAN NORMALISASI DEBIT_CREDIT TERJAGA"
				: ("ADA " + gagal + " INVARIAN YANG DILANGGAR"));
		if (gagal > 0) {
			System.exit(1);
		}
	}
}
