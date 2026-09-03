package ais.action.servlet.api;

/**
 * Test harness tanpa JUnit dan tanpa basis data untuk kunci advisory lock sinkron
 * BAST ({@link PengadaanPosApiHelper#kunciSinkronBast(Long)}).
 *
 * <p>Advisory lock inilah yang mencegah <b>stok Kulakan tergandakan</b> saat dua
 * permintaan sinkron BAST yang sama berjalan bersamaan (dok. 79). Yang dijaga di
 * sini bukan "kuncinya bekerja" — itu dibuktikan harness ber-database — melainkan
 * dua keputusan yang mudah rusak diam-diam saat seseorang menyunting helper kelak:</p>
 *
 * <ol>
 *   <li><b>Satu sumber kebenaran untuk string kunci.</b> Sisi lock dan sisi unlock
 *       memanggil metode yang sama, jadi keduanya tak mungkin menyimpang; kalau
 *       menyimpang, unlock meleset dan kunci hanya lepas saat koneksi ditutup.</li>
 *   <li><b>Prefiks namespace {@code bast-sinkron:} dipertahankan dan unik.</b> Seluruh
 *       pemakai {@code hashtext(...)} di basis kode berbagi SATU ruang kunci advisory
 *       global PostgreSQL. Bila prefiks BAST dihapus atau disamakan dengan fitur lain
 *       ({@code online-bmt:}, {@code init:}, {@code PMB_NO_UJIAN_SAVE_}), sinkron BAST
 *       akan saling memblokir dengan fitur tak terkait — bahkan berpotensi deadlock.</li>
 * </ol>
 *
 * <p>Jalankan: {@code java ais.action.servlet.api.PengadaanBastLockSelfTest}.
 * Keluar dengan kode 1 dan menyebut invarian yang dilanggar bila ada yang rusak.</p>
 */
public final class PengadaanBastLockSelfTest {

	private PengadaanBastLockSelfTest() { }

	/** Prefiks namespace fitur lain yang berbagi ruang kunci advisory yang sama. */
	private static final String[] NAMESPACE_LAIN = {
			"online-bmt:",           // OnlineBmt
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
		String k500 = PengadaanPosApiHelper.kunciSinkronBast(500L);

		// 1. Deterministik: id sama -> kunci sama (lock & unlock tak mungkin menyimpang).
		cek(k500.equals(PengadaanPosApiHelper.kunciSinkronBast(500L)),
				"kunci deterministik untuk id yang sama (lock == unlock)");

		// 2. Membawa identitas BAST dan membedakan BAST yang berbeda.
		cek(k500.indexOf("500") >= 0, "kunci memuat id BAST");
		cek(!k500.equals(PengadaanPosApiHelper.kunciSinkronBast(501L)),
				"BAST berbeda -> kunci berbeda (tidak saling menserialkan)");

		// 3. Prefiks namespace BAST ada dan unik terhadap seluruh pemakai lain.
		cek(k500.startsWith("bast-sinkron:"), "kunci memakai prefiks namespace bast-sinkron:");
		for (String lain : NAMESPACE_LAIN) {
			cek(!k500.startsWith(lain),
					"kunci BAST tidak memakai namespace fitur lain (" + lain + ")");
			cek(!lain.startsWith("bast-sinkron:"),
					"namespace fitur lain (" + lain + ") tidak menabrak bast-sinkron:");
		}

		// 4. Perilaku null dipertahankan persis seperti "bast-sinkron:" + id (tanpa NPE).
		cek("bast-sinkron:null".equals(PengadaanPosApiHelper.kunciSinkronBast(null)),
				"id null menghasilkan kunci stabil tanpa melempar NPE");

		System.out.println(gagal == 0
				? "SEMUA INVARIAN KUNCI SINKRON BAST TERJAGA"
				: ("ADA " + gagal + " INVARIAN YANG DILANGGAR"));
		if (gagal > 0) {
			System.exit(1);
		}
	}
}
