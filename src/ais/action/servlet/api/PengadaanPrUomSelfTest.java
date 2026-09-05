package ais.action.servlet.api;

/** Uji kontrak murni konversi UOM PR/PO/BAST menuju Kulakan. */
public final class PengadaanPrUomSelfTest {
	private static int lulus;

	private static void dekat(double aktual, double harapan, String nama) {
		if (Math.abs(aktual - harapan) > 0.000001) {
			throw new AssertionError(nama + ": " + aktual + " != " + harapan);
		}
		lulus++;
		System.out.println("LULUS  " + nama);
	}

	public static void main(String[] args) {
		double[] dus = PengadaanPosApiHelper.nilaiKulakanDariUom(2.0, 120000.0, 12.0);
		dekat(dus[0], 24.0, "2 Dus isi 12 menjadi 24 satuan stok");
		dekat(dus[1], 10000.0, "harga per Dus dibagi menjadi harga satuan stok");
		dekat(dus[0] * dus[1], 240000.0, "nilai total tidak berubah sesudah konversi");
		boolean ditolak = false;
		try { PengadaanPosApiHelper.nilaiKulakanDariUom(1.0, 100.0, 0.0); }
		catch (IllegalArgumentException benar) { ditolak = true; }
		if (!ditolak) throw new AssertionError("faktor nol wajib ditolak");
		lulus++;
		System.out.println("LULUS  faktor nol ditolak");
		System.out.println("SEMUA " + lulus + " ATURAN UOM PENGADAAN TERJAGA");
	}

	private PengadaanPrUomSelfTest() {
	}
}
