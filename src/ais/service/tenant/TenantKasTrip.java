package ais.service.tenant;

/**
 * <h3>Kontrak buku kas trip: sembilan jenis baris dan satu aturan tanda.</h3>
 *
 * <p>Tabel {@code {S}.sales_trip_kas} lahir bersama migrasi v12 sebagai padanan
 * {@code koperasi.nota_sales_kas}. Katalog sengaja <b>tidak</b> memasang batasan {@code CHECK}
 * atas kolom {@code jenis}; kontraknya hidup di kelas ini, sebagaimana {@link TenantMutasiStok}
 * memegang kontrak buku besar persediaan.</p>
 *
 * <h4>Satu aturan yang menentukan segalanya: nominal BERTANDA</h4>
 * <p>{@code nominal} disimpan <b>sudah bertanda</b>. Uang yang masuk ke tangan sales bernilai
 * positif; uang yang keluar bernilai negatif. Saldo kas adalah {@code SUM(nominal)} — tanpa
 * {@code CASE}, tanpa penyusunan ulang tanda, tanpa daftar jenis yang harus diingat pembaca.</p>
 * <p>Itu bukan selera. Rumus saldo kas pernah salah kirim sekali pada pemindahan ini justru
 * karena tandanya harus disusun ulang di sisi pembaca. Menyimpannya sekali di sisi penulis
 * membuat kekeliruan itu tidak punya tempat untuk muncul lagi.</p>
 *
 * <h4>Sembilan jenis</h4>
 * <table border="1">
 * <tr><th>jenis</th><th>tanda</th><th>artinya</th></tr>
 * <tr><td>{@code OPENING_ADVANCE}</td><td>+</td>
 *     <td>uang muka operasional yang dibawa saat berangkat</td></tr>
 * <tr><td>{@code COLLECTION_CASH}</td><td>+</td>
 *     <td>penagihan piutang yang diterima tunai di lapangan</td></tr>
 * <tr><td>{@code CASH_SALE}</td><td>+</td><td>penjualan tunai</td></tr>
 * <tr><td>{@code EXPENSE_CASH}</td><td>&minus;</td><td>biaya perjalanan yang dibayar tunai</td></tr>
 * <tr><td>{@code PURCHASE_PAYMENT}</td><td>&minus;</td>
 *     <td>pembayaran ke pemasok dari kas yang dipegang</td></tr>
 * <tr><td>{@code OWNER_DEPOSIT}</td><td>&minus;</td><td>setoran kembali ke pemilik</td></tr>
 * <tr><td>{@code REFUND}</td><td>&minus;</td><td>pengembalian uang ke pelanggan</td></tr>
 * <tr><td>{@code ADJUSTMENT}</td><td>&plusmn;</td><td>koreksi selisih hitung fisik</td></tr>
 * <tr><td>{@code REVERSAL}</td><td>&plusmn;</td>
 *     <td>pembalikan; tandanya berlawanan dengan baris yang dibalik</td></tr>
 * </table>
 *
 * <p>Nilainya sengaja sama persis dengan konstanta {@code NotaSalesKas} pada jalur legacy.
 * Menyamakannya membuat pemindahan data legacy tidak memerlukan tabel terjemahan, dan membuat
 * kedua jalur dapat dibandingkan langsung pada uji kesetaraan.</p>
 *
 * <h4>Uang muka awal DITURUNKAN, tidak disimpan dua kali</h4>
 * <p>Entitas legacy menyimpan {@code NotaSalesSession.saldoKasAwal} <b>selain</b> membukukan
 * baris {@code OPENING_ADVANCE}. Model tenant tidak menirunya: angka itu dibaca dari bukunya
 * sendiri lewat {@link #SQL_UANG_MUKA_AWAL}. Satu sumber, tidak ada yang bisa berselisih.</p>
 */
public final class TenantKasTrip {

	private TenantKasTrip() {
	}

	/** Uang muka operasional saat berangkat. Positif. */
	public static final String OPENING_ADVANCE = "OPENING_ADVANCE";

	/** Penagihan piutang diterima tunai. Positif. */
	public static final String COLLECTION_CASH = "COLLECTION_CASH";

	/** Penjualan tunai. Positif. */
	public static final String CASH_SALE = "CASH_SALE";

	/** Biaya perjalanan dibayar tunai. Negatif. */
	public static final String EXPENSE_CASH = "EXPENSE_CASH";

	/** Pembayaran ke pemasok dari kas yang dipegang. Negatif. */
	public static final String PURCHASE_PAYMENT = "PURCHASE_PAYMENT";

	/** Setoran kembali ke pemilik. Negatif. */
	public static final String OWNER_DEPOSIT = "OWNER_DEPOSIT";

	/** Pengembalian uang ke pelanggan. Negatif. */
	public static final String REFUND = "REFUND";

	/** Koreksi selisih hitung fisik. Boleh positif maupun negatif. */
	public static final String ADJUSTMENT = "ADJUSTMENT";

	/** Pembalikan; tandanya berlawanan dengan baris yang dibalik. */
	public static final String REVERSAL = "REVERSAL";

	/** Seluruh jenis yang sah, untuk pemeriksaan sisi aplikasi. */
	public static final String[] SEMUA = { OPENING_ADVANCE, COLLECTION_CASH, CASH_SALE,
			EXPENSE_CASH, PURCHASE_PAYMENT, OWNER_DEPOSIT, REFUND, ADJUSTMENT, REVERSAL };

	/**
	 * Benar bila {@code jenis} termasuk kontrak ini.
	 *
	 * <p>Dipakai sebagai penjaga sisi aplikasi, menggantikan batasan {@code CHECK} yang sengaja
	 * tidak dipasang katalog — lihat javadoc {@link TenantSchemaMigrationsV12}.</p>
	 *
	 * @param jenis nilai kolom {@code jenis} yang hendak ditulis
	 * @return benar bila sah
	 */
	public static boolean sah(String jenis) {
		if (jenis == null) {
			return false;
		}
		for (int i = 0; i < SEMUA.length; i++) {
			if (SEMUA[i].equals(jenis)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Benar bila jenis ini SELALU mengurangi kas, sehingga penulisnya wajib menyimpan nominal
	 * negatif.
	 *
	 * <p>{@code ADJUSTMENT} dan {@code REVERSAL} tidak termasuk: keduanya memang boleh dua arah,
	 * dan tandanya ditentukan peristiwa yang dicatatnya.</p>
	 *
	 * @param jenis jenis baris kas
	 * @return benar bila jenisnya selalu keluar
	 */
	public static boolean selaluKeluar(String jenis) {
		return EXPENSE_CASH.equals(jenis) || PURCHASE_PAYMENT.equals(jenis)
				|| OWNER_DEPOSIT.equals(jenis) || REFUND.equals(jenis);
	}

	/**
	 * Benar bila jenis ini SELALU menambah kas.
	 *
	 * @param jenis jenis baris kas
	 * @return benar bila jenisnya selalu masuk
	 */
	public static boolean selaluMasuk(String jenis) {
		return OPENING_ADVANCE.equals(jenis) || COLLECTION_CASH.equals(jenis)
				|| CASH_SALE.equals(jenis);
	}

	/**
	 * Saldo kas berjalan satu trip: penjumlahan bertanda seluruh bukunya.
	 *
	 * <p>Bentuknya sengaja sesederhana ini. Setiap {@code CASE} yang ditambahkan di sini adalah
	 * kesempatan baru untuk melupakan satu jenis.</p>
	 *
	 * @param skema prefiks schema tenant berikut titiknya
	 * @param aliasTripId ekspresi id trip pada kueri pemanggil
	 * @return subkueri skalar saldo kas
	 */
	public static String sqlSaldoKas(String skema, String aliasTripId) {
		return "COALESCE((SELECT SUM(k.nominal) FROM " + skema + "sales_trip_kas k"
				+ " WHERE k.sales_trip_id = " + aliasTripId + "),0)";
	}

	/**
	 * Uang muka operasional yang benar-benar dibukukan untuk satu trip.
	 *
	 * <p>Padanan {@code NotaSalesSession.saldoKasAwal} legacy, tetapi diturunkan dari bukunya
	 * alih-alih disimpan terpisah.</p>
	 */
	public static final String SQL_UANG_MUKA_AWAL =
			"COALESCE((SELECT SUM(k.nominal) FROM %sales_trip_kas k"
					+ " WHERE k.sales_trip_id = %alias AND k.jenis = '" + OPENING_ADVANCE + "'),0)";

	/**
	 * Bentuk {@link #SQL_UANG_MUKA_AWAL} untuk schema dan alias tertentu.
	 *
	 * @param skema prefiks schema tenant berikut titiknya
	 * @param aliasTripId ekspresi id trip pada kueri pemanggil
	 * @return subkueri skalar uang muka awal
	 */
	public static String sqlUangMukaAwal(String skema, String aliasTripId) {
		return SQL_UANG_MUKA_AWAL.replace("%sales_trip_kas", skema + "sales_trip_kas")
				.replace("%alias", aliasTripId);
	}
}
