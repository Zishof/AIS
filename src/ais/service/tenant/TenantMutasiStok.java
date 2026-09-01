package ais.service.tenant;

/**
 * <h3>Kontrak isi <code>{S}.mutasi_stok</code>: nilai <code>jenis</code> dan <code>arah</code>.</h3>
 *
 * <p>Tabelnya sudah berdiri sejak migrasi v3, tetapi nilai yang boleh diisikan ke
 * {@code jenis} dan {@code arah} belum pernah ditetapkan di mana pun -- tidak di DDL, tidak
 * di dokumen. Kelas ini menetapkannya, supaya penulis pertama tidak diam-diam menentukan
 * kontrak untuk semua penulis sesudahnya.</p>
 *
 * <h4>Arah, bukan tanda pada kuantitas</h4>
 * <p>{@code kuantitas} SELALU positif; yang menentukan naik-turun adalah {@code arah}
 * ({@value #MASUK} atau {@value #KELUAR}). Pengaruh satu baris terhadap stok =
 * {@code arah * kuantitas}.</p>
 * <p>Menyimpan tanda di kuantitas akan membuat {@code SUM(kuantitas)} bermakna ganda: pada
 * satu kueri ia total pergerakan, pada kueri lain saldo bersih. Dengan arah terpisah,
 * "berapa yang masuk bulan ini" dan "berapa saldonya" adalah dua kueri yang jelas berbeda.</p>
 *
 * <h4>Opname adalah jenis tersendiri, bukan masuk/keluar biasa</h4>
 * <p>Layar persediaan legacy menampilkan Awal / Masuk / Keluar / <b>Penyesuaian</b> sebagai
 * empat kolom terpisah. Karena itu {@link #OPNAME} harus dapat dipisahkan dari pergerakan
 * dagang: selisih opname positif berarti {@code arah = MASUK}, negatif berarti
 * {@code arah = KELUAR}, dan keduanya tetap ber-{@code jenis} {@code OPNAME}.</p>
 *
 * <h4>Padanan peristiwa legacy</h4>
 * <table border="1">
 * <tr><th>Legacy</th><th>jenis</th><th>arah</th></tr>
 * <tr><td>{@code pengadaan_produk}</td><td>{@value #PENGADAAN}</td><td>masuk</td></tr>
 * <tr><td>{@code pembelian} <b>(= penjualan)</b></td><td>{@value #PENJUALAN}</td><td>keluar</td></tr>
 * <tr><td>{@code pemakaian_bahan_baku}</td><td>{@value #PEMAKAIAN_BAHAN}</td><td>keluar</td></tr>
 * <tr><td>{@code retur_penjualan} (kembali ke stok)</td><td>{@value #RETUR_PENJUALAN}</td><td>masuk</td></tr>
 * <tr><td>{@code retur_pembelian}</td><td>{@value #RETUR_PEMBELIAN}</td><td>keluar</td></tr>
 * <tr><td>{@code mutasi_stok_toko} (produk_tujuan)</td><td>{@value #MUTASI_MASUK}</td><td>masuk</td></tr>
 * <tr><td>{@code mutasi_stok_toko} (produk_asal)</td><td>{@value #MUTASI_KELUAR}</td><td>keluar</td></tr>
 * <tr><td>{@code stok_opname.selisih}</td><td>{@value #OPNAME}</td><td>ikut tanda selisih</td></tr>
 * <tr><td>{@code mutasi_stok_produksi}</td><td>{@value #PRODUKSI}</td><td>ikut qty_masuk/qty_keluar</td></tr>
 * </table>
 *
 * <p><b>Perhatikan baris kedua.</b> Tabel legacy bernama {@code pembelian} menyimpan
 * <b>penjualan</b>, bukan pembelian -- terbukti dari label 'Penjualan' pada kartu stok
 * legacy. Memetakannya ke {@code PEMBELIAN} akan membalik arah seluruh omzet.</p>
 *
 * <p>{@code retur_penjualan} yang <b>tidak</b> kembali ke stok sengaja tidak punya padanan:
 * ia peristiwa uang, bukan peristiwa stok.</p>
 */
public final class TenantMutasiStok {

	/** Menambah stok. Disimpan di kolom {@code arah}. */
	public static final short MASUK = 1;
	/** Mengurangi stok. */
	public static final short KELUAR = -1;

	public static final String PENGADAAN = "PENGADAAN";
	public static final String PENJUALAN = "PENJUALAN";
	public static final String PEMAKAIAN_BAHAN = "PEMAKAIAN_BAHAN";
	public static final String RETUR_PENJUALAN = "RETUR_PENJUALAN";
	public static final String RETUR_PEMBELIAN = "RETUR_PEMBELIAN";
	public static final String MUTASI_MASUK = "MUTASI_MASUK";
	public static final String MUTASI_KELUAR = "MUTASI_KELUAR";
	public static final String OPNAME = "OPNAME";
	public static final String PRODUKSI = "PRODUKSI";

	/** Seluruh jenis yang sah, untuk validasi dan uji kelengkapan. */
	public static final String[] JENIS = { PENGADAAN, PENJUALAN, PEMAKAIAN_BAHAN,
			RETUR_PENJUALAN, RETUR_PEMBELIAN, MUTASI_MASUK, MUTASI_KELUAR, OPNAME, PRODUKSI };

	private TenantMutasiStok() {
	}

	/** Benar bila {@code jenis} dikenal kontrak ini. */
	public static boolean jenisSah(String jenis) {
		if (jenis == null) {
			return false;
		}
		for (int i = 0; i < JENIS.length; i++) {
			if (JENIS[i].equals(jenis)) {
				return true;
			}
		}
		return false;
	}

	/** Arah dari selisih opname: nol dianggap masuk, sebab kuantitasnya nol juga. */
	public static short arahDariSelisih(java.math.BigDecimal selisih) {
		return selisih != null && selisih.signum() < 0 ? KELUAR : MASUK;
	}
}
