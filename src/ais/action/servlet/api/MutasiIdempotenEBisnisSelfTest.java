package ais.action.servlet.api;

/**
 * Test harness tanpa JUnit dan tanpa basis data untuk kontrak idempotensi kiriman ulang
 * ({@link MutasiIdempotenEBisnisUtil#aksiMasterAntrean(String)}).
 *
 * <p>Aksi yang MENGUBAH data master/stok dan dikirim lewat antrean MasterOffline harus
 * terdaftar, supaya kiriman ulang (lost-ack / retry offline) di-replay -- tidak menciptakan
 * data atau stok ganda. Ini menjaga khususnya {@code kulakan_faktur_simpan} (dok. 81) agar tidak
 * terlepas dari daftar tanpa sengaja: melepasnya menghidupkan kembali bug faktur &amp; stok
 * ganda. Beberapa aksi lain yang mewakili disertakan sebagai jangkar.</p>
 *
 * <p>Sengaja MENDOKUMENTASIKAN bahwa {@code bayar} TIDAK ada di daftar ini: penjualan POS punya
 * idempotensi transaksinya sendiri (kode transaksi unik), bukan lewat lapisan ini.</p>
 *
 * <p>Jalankan: {@code java ais.action.servlet.api.MutasiIdempotenEBisnisSelfTest}.</p>
 */
public final class MutasiIdempotenEBisnisSelfTest {

	private MutasiIdempotenEBisnisSelfTest() { }

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
		// Aksi yang WAJIB idempoten (mengubah master/stok lewat antrean).
		String[] wajib = {
				"kulakan_faktur_simpan", // dok. 81 -- cegah faktur & stok ganda
				"produk_simpan",
				"produk_batch_simpan",
				"anggota_simpan",
				"penyedia_simpan",
				"grup_produk_simpan",
				"kebijakan_retur_simpan",
		};
		for (String a : wajib) {
			cek(MutasiIdempotenEBisnisUtil.aksiMasterAntrean(a), "terdaftar idempoten: " + a);
		}

		// Pencocokan case-insensitive (klien lama bisa mengirim huruf berbeda).
		cek(MutasiIdempotenEBisnisUtil.aksiMasterAntrean("KULAKAN_FAKTUR_SIMPAN"),
				"pencocokan case-insensitive");

		// Dokumentasi kontrak: bayar TIDAK lewat lapisan ini (punya idempotensi sendiri).
		cek(!MutasiIdempotenEBisnisUtil.aksiMasterAntrean("bayar"),
				"bayar TIDAK di daftar (idempotensi transaksi POS terpisah)");

		// Masukan tak sah aman.
		cek(!MutasiIdempotenEBisnisUtil.aksiMasterAntrean(null), "null aman -> false");
		cek(!MutasiIdempotenEBisnisUtil.aksiMasterAntrean(""), "kosong aman -> false");
		cek(!MutasiIdempotenEBisnisUtil.aksiMasterAntrean("aksi_ngawur_xyz"),
				"aksi tak dikenal -> false");

		System.out.println(gagal == 0
				? "SEMUA KONTRAK IDEMPOTENSI TERJAGA"
				: ("ADA " + gagal + " KONTRAK YANG DILANGGAR"));
		if (gagal > 0) {
			System.exit(1);
		}
	}
}
