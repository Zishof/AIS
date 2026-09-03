package ais.action.servlet.api;

import ais.database.model.akunting.Akun;

/**
 * Test harness tanpa JUnit dan tanpa basis data untuk
 * {@link KodeAkunApiHelper#posisiDariTipeAccurate(String)}.
 *
 * <p>Melindungi perbaikan integritas data 3 Sep 2026: method ini dulu mengembalikan sandi
 * asing {@code 2} untuk seluruh tipe Accurate sisi kredit, bukan {@link Akun#CREDIT}
 * ({@code -1}). Nilai {@code 2} yang ditulis {@code akunImpor} ke kolom {@code debit_credit}
 * membuat {@code LaporanKeuanganCoaHelper} mengalikan saldo akun kredit hasil impor dengan
 * {@code +2} alih-alih {@code -1} pada laporan keuangan cetak -- besaran dua kali lipat dan
 * tanda terbalik. Baris ini memastikan regresi itu tidak diam-diam kembali.</p>
 *
 * <p>Jalankan: {@code java ais.action.servlet.api.KodeAkunPosisiAccurateSelfTest}.
 * Keluar dengan kode 1 dan menyebut invarian yang dilanggar bila ada yang rusak.</p>
 */
public final class KodeAkunPosisiAccurateSelfTest {

	private KodeAkunPosisiAccurateSelfTest() { }

	private static final String[] TIPE_DEBET = { "BANK", "AREC", "OCAS", "INTR", "FASS", "EXPS", "COGS", "OEXP" };
	private static final String[] TIPE_KREDIT = { "DEPR", "APAY", "OCLY", "LTLY", "EQTY", "REVE", "OINC" };

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
		for (int i = 0; i < TIPE_DEBET.length; i++) {
			String tipe = TIPE_DEBET[i];
			cek(Akun.DEBET.equals(KodeAkunApiHelper.posisiDariTipeAccurate(tipe)),
					"tipe Accurate " + tipe + " -> Akun.DEBET (1)");
		}

		for (int i = 0; i < TIPE_KREDIT.length; i++) {
			String tipe = TIPE_KREDIT[i];
			Integer hasil = KodeAkunApiHelper.posisiDariTipeAccurate(tipe);
			// Inti perbaikan: HARUS Akun.CREDIT (-1), bukan sandi asing 2 dari Accurate.
			cek(Akun.CREDIT.equals(hasil),
					"tipe Accurate " + tipe + " -> Akun.CREDIT (-1), bukan sandi asing " + hasil);
			cek(hasil == null || hasil.intValue() != 2,
					"tipe Accurate " + tipe + " tidak lagi mengembalikan sandi asing 2");
		}

		cek(KodeAkunApiHelper.posisiDariTipeAccurate("TIPE_TAK_DIKENAL") == null,
				"tipe tak dikenal -> null (pemanggil membiarkan posisi lama apa adanya)");
		cek(KodeAkunApiHelper.posisiDariTipeAccurate(null) == null, "tipe null -> null, tanpa NPE");

		System.out.println(gagal == 0
				? "SEMUA INVARIAN POSISI DARI TIPE ACCURATE TERJAGA"
				: ("ADA " + gagal + " INVARIAN YANG DILANGGAR"));
		if (gagal > 0) {
			System.exit(1);
		}
	}
}
