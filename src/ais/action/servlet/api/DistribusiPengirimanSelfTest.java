package ais.action.servlet.api;

/**
 * Test harness tanpa JUnit dan tanpa basis data untuk mesin-status distribusi/logistik.
 *
 * <p>Yang dijaga bukan "kodenya jalan", melainkan invarian yang menjaga INTEGRITAS STOK:
 * posting stok hanya terjadi saat {@code COMPLETED} (maju) dan {@code REVERSED} (balik), dan
 * mesin status tidak boleh mengizinkan jalan pintas yang membuat stok terposting dua kali atau
 * terlewat validasinya. Ketiga sifat di bawah masing-masing menjaga hal itu:</p>
 *
 * <ol>
 *   <li>hanya dua jenis dokumen yang menyentuh stok — penerimaan transfer outlet dan reverse
 *       logistics; jenis lain tidak boleh memicu mutasi;</li>
 *   <li>{@code COMPLETED} hanya dapat dicapai lewat {@code APPROVED}/{@code IN_PROGRESS}, tidak
 *       langsung dari {@code DRAFT}/{@code SUBMITTED} (yang melewati persetujuan &amp; validasi);</li>
 *   <li>status yang sudah final tidak dapat berpindah lagi: {@code COMPLETED} hanya boleh ke
 *       {@code REVERSED}, dan {@code REVERSED}/{@code CANCELLED} adalah terminal — sehingga posting
 *       maju tidak dapat terjadi dua kali dan pembalikan tidak dapat berulang.</li>
 * </ol>
 *
 * <p>Jalankan: {@code java ais.action.servlet.api.DistribusiPengirimanSelfTest}.</p>
 */
public final class DistribusiPengirimanSelfTest {

	private DistribusiPengirimanSelfTest() { }

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
		// 1. Hanya dua jenis dokumen yang menyentuh stok.
		cek(DistribusiPengirimanApiHelper.memengaruhiStok("penerimaan_transfer_outlet"),
				"penerimaan_transfer_outlet menyentuh stok");
		cek(DistribusiPengirimanApiHelper.memengaruhiStok("reverse_logistics"),
				"reverse_logistics menyentuh stok");
		String[] takSentuh = { "delivery_order", "freight_order", "shipment_tracking",
				"proof_of_delivery", "klaim_distribusi", "", "penerimaan" };
		for (String j : takSentuh) {
			cek(!DistribusiPengirimanApiHelper.memengaruhiStok(j),
					"jenis '" + j + "' TIDAK menyentuh stok");
		}
		cek(!DistribusiPengirimanApiHelper.memengaruhiStok(null),
				"jenis null TIDAK menyentuh stok");

		// 2. COMPLETED tidak dapat dicapai tanpa melewati persetujuan.
		cek(!DistribusiPengirimanApiHelper.transisiBoleh("DRAFT", "COMPLETED"),
				"DRAFT tidak boleh langsung COMPLETED");
		cek(!DistribusiPengirimanApiHelper.transisiBoleh("SUBMITTED", "COMPLETED"),
				"SUBMITTED tidak boleh langsung COMPLETED");
		cek(DistribusiPengirimanApiHelper.transisiBoleh("APPROVED", "COMPLETED"),
				"APPROVED boleh COMPLETED");
		cek(DistribusiPengirimanApiHelper.transisiBoleh("IN_PROGRESS", "COMPLETED"),
				"IN_PROGRESS boleh COMPLETED");

		// 3. Status final tidak berpindah lagi (cegah posting/reverse ganda).
		cek(!DistribusiPengirimanApiHelper.transisiBoleh("COMPLETED", "COMPLETED"),
				"COMPLETED tidak boleh COMPLETED lagi (cegah posting maju ganda)");
		cek(DistribusiPengirimanApiHelper.transisiBoleh("COMPLETED", "REVERSED"),
				"COMPLETED boleh REVERSED");
		cek(!DistribusiPengirimanApiHelper.transisiBoleh("REVERSED", "COMPLETED"),
				"REVERSED terminal: tidak boleh kembali COMPLETED");
		cek(!DistribusiPengirimanApiHelper.transisiBoleh("REVERSED", "REVERSED"),
				"REVERSED tidak boleh REVERSED lagi (cegah pembalikan ganda)");
		cek(!DistribusiPengirimanApiHelper.transisiBoleh("CANCELLED", "APPROVED"),
				"CANCELLED terminal");

		// Alur maju yang sah dan penjaga umum.
		cek(DistribusiPengirimanApiHelper.transisiBoleh("DRAFT", "SUBMITTED"), "DRAFT -> SUBMITTED");
		cek(DistribusiPengirimanApiHelper.transisiBoleh("SUBMITTED", "APPROVED"), "SUBMITTED -> APPROVED");
		cek(DistribusiPengirimanApiHelper.transisiBoleh("REJECTED", "DRAFT"), "REJECTED -> DRAFT");
		cek(!DistribusiPengirimanApiHelper.transisiBoleh("DRAFT", "DRAFT"), "status sama ditolak");
		cek(!DistribusiPengirimanApiHelper.transisiBoleh(null, "DRAFT"), "null asal ditolak");
		cek(!DistribusiPengirimanApiHelper.transisiBoleh("DRAFT", null), "null tujuan ditolak");

		System.out.println(gagal == 0
				? "SEMUA INVARIAN MESIN-STATUS DISTRIBUSI TERJAGA"
				: ("ADA " + gagal + " INVARIAN YANG DILANGGAR"));
		if (gagal > 0) {
			System.exit(1);
		}
	}
}
