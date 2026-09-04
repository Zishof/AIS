package ais.action.servlet.api;

/** Uji kontrak tanpa database untuk batas nominal pelunasan piutang. */
public final class KantinMutasiPiutangSelfTest {
	private static int lulus;

	private static void check(boolean kondisi, String nama) {
		if (!kondisi) throw new AssertionError(nama);
		lulus++;
		System.out.println("LULUS  " + nama);
	}

	public static void main(String[] args) {
		check(KantinHelper.nominalPelunasanPiutangValid(25000.0, 25000.0, 0.0),
				"pelunasan penuh sebesar saldo diterima");
		check(KantinHelper.nominalPelunasanPiutangValid(1500.0, 25000.0, 0.0),
				"cicilan sebagian diterima");
		check(!KantinHelper.nominalPelunasanPiutangValid(25001.0, 25000.0, 0.0),
				"pelunasan melebihi saldo ditolak");
		check(!KantinHelper.nominalPelunasanPiutangValid(25000.01, 25000.0, 0.0),
				"kelebihan nominal pecahan tetap ditolak");
		check(!KantinHelper.nominalPelunasanPiutangValid(0.0, 25000.0, 0.0),
				"nominal nol ditolak");
		check(!KantinHelper.nominalPelunasanPiutangValid(Double.NaN, 25000.0, 0.0),
				"nominal bukan angka ditolak");
		check(KantinHelper.nominalPelunasanPiutangValid(30000.0, 5000.0, 25000.0),
				"edit pembayaran memperhitungkan nominal lama");
		System.out.println("SEMUA " + lulus + " ATURAN MUTASI PIUTANG TERJAGA");
	}

	private KantinMutasiPiutangSelfTest() {
	}
}
