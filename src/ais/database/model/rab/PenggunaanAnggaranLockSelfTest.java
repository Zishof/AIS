package ais.database.model.rab;

/**
 * Test harness tanpa JUnit dan tanpa basis data untuk kunci advisory lock ref
 * RAB ({@link PenggunaanAnggaran#kunciRef(String)}).
 *
 * <p>Advisory lock inilah yang mencegah <b>dua proses menulis ref RAB yang sama
 * secara bersamaan</b> (lihat {@code PenggunaanAnggaran.lockRef}). Yang dijaga di
 * sini bukan "kuncinya bekerja" — itu urusan harness ber-database — melainkan satu
 * keputusan yang mudah rusak diam-diam saat seseorang menyunting kelak:</p>
 *
 * <ol>
 *   <li><b>Prefiks namespace {@code rab-ref:} ada, deterministik, dan unik.</b>
 *       Seluruh pemakai {@code hashtext(...)} di basis kode berbagi SATU ruang
 *       kunci advisory global PostgreSQL. Tanpa prefiks, isi {@code ref} yang
 *       bebas ditentukan modul lain bisa memetakan ke hash yang sama dengan kunci
 *       fitur lain ({@code online-bmt:}, {@code bast-sinkron:}, {@code init:},
 *       {@code PMB_NO_UJIAN_SAVE_}) sehingga saling memblokir walau tak
 *       berhubungan — bahkan berpotensi deadlock.</li>
 * </ol>
 *
 * <p>Jalankan: {@code java ais.database.model.rab.PenggunaanAnggaranLockSelfTest}.
 * Keluar dengan kode 1 dan menyebut invarian yang dilanggar bila ada yang rusak.</p>
 */
public final class PenggunaanAnggaranLockSelfTest {

	private PenggunaanAnggaranLockSelfTest() { }

	/** Prefiks namespace fitur lain yang berbagi ruang kunci advisory yang sama. */
	private static final String[] NAMESPACE_LAIN = {
			"online-bmt:",           // OnlineBmt
			"bast-sinkron:",         // PengadaanPosApiHelper
			"init:",                 // InitIndex
			"PMB_NO_UJIAN_SAVE_" };  // CommonPMB

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
		String kA = PenggunaanAnggaran.kunciRef("RAB-2026-000123");

		// 1. Deterministik: ref sama -> kunci sama.
		cek(kA.equals(PenggunaanAnggaran.kunciRef("RAB-2026-000123")),
				"kunci deterministik untuk ref yang sama");

		// 2. Membawa identitas ref dan membedakan ref yang berbeda.
		cek(kA.indexOf("RAB-2026-000123") >= 0, "kunci memuat ref");
		cek(!kA.equals(PenggunaanAnggaran.kunciRef("RAB-2026-000124")),
				"ref berbeda -> kunci berbeda (tidak saling menserialkan)");

		// 3. Prefiks namespace RAB ada dan unik terhadap seluruh pemakai lain.
		cek(kA.startsWith("rab-ref:"), "kunci memakai prefiks namespace rab-ref:");
		for (String lain : NAMESPACE_LAIN) {
			cek(!kA.startsWith(lain),
					"kunci RAB tidak memakai namespace fitur lain (" + lain + ")");
			cek(!lain.startsWith("rab-ref:"),
					"namespace fitur lain (" + lain + ") tidak menabrak rab-ref:");
		}

		// 4. Anti-tabrakan: ref mentah yang KEBETULAN sama persis dengan kunci utuh
		//    fitur lain tetap tak menabrak setelah diberi prefiks. Inilah inti
		//    perbaikan dok. 107 — sebelumnya ref dikunci mentah (tanpa prefiks).
		for (String lain : NAMESPACE_LAIN) {
			String refBerbahaya = lain + "999";        // mis. "online-bmt:999"
			cek(!PenggunaanAnggaran.kunciRef(refBerbahaya).equals(refBerbahaya),
					"ref '" + refBerbahaya + "' tidak lagi identik dengan kunci fitur lain");
		}

		// 5. Perilaku null stabil tanpa NPE (lockRef sudah menyaring ref kosong,
		//    tetapi kunciRef tetap tidak boleh melempar).
		cek("rab-ref:null".equals(PenggunaanAnggaran.kunciRef(null)),
				"ref null menghasilkan kunci stabil tanpa melempar NPE");

		System.out.println(gagal == 0
				? "SEMUA INVARIAN KUNCI REF RAB TERJAGA"
				: ("ADA " + gagal + " INVARIAN YANG DILANGGAR"));
		if (gagal > 0) {
			System.exit(1);
		}
	}
}
