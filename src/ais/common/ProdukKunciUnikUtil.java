package ais.common;

/**
 * Kunci unik normalisasi produk (gap-closure "Katalog Barang banyak yang double walau sudah lewat
 * Bersihkan Produk Duplikat" -- toko Al-Bahjah, laporan langsung dari layar hasil impor Excel).
 * Permintaan user EKSPLISIT: {@code hilangkanTandaBacaKecualiUnderscore((kode+_+barcode+_+nama+_+
 * ID_TOKO).toLowerCase().trim())} -- SATU sumber kebenaran dipakai BERSAMA oleh:
 * <ul>
 * <li>{@link ais.database.model.inventory.Produk} (field {@code kunciUnik}, dihitung ULANG
 * otomatis lewat {@code @PrePersist}/{@code @PreUpdate} SETIAP kali baris disimpan -- lihat
 * JavaDoc di sana -- supaya SEMUA jalur penulisan produk, termasuk yang belum tentu lewat
 * {@code KantinHelper} (mis. {@code ProdukAction} ZK lama, auto-create dari Pembelian/Tabungan
 * Siswa), otomatis ikut terjaga TANPA perlu diubah satu-satu.</li>
 * <li>{@code KantinHelper.kunciDuplikat} (jenis {@code "kunci_unik"}, versi SQL persis sama lewat
 * {@code regexp_replace} -- dipakai fitur "Bersihkan Produk Duplikat" utk mencari &amp;
 * membersihkan baris yang SUDAH terlanjur double sebelum kolom ini ada).</li>
 * </ul>
 *
 * <p>SENGAJA cuma menyisakan huruf/angka/underscore (tanda baca/spasi APAPUN dibuang total,
 * BUKAN diganti spasi) -- makin ketat normalisasinya, makin kecil peluang dua produk yang
 * SEBENARNYA sama (mis. beda spasi/tanda hubung/huruf besar-kecil krn sumber Excel berbeda-beda)
 * lolos dianggap "berbeda" oleh kunci ini.</p>
 */
public final class ProdukKunciUnikUtil {

	private ProdukKunciUnikUtil() {
	}

	/** @return {@code s} dengan SELURUH karakter selain huruf/angka/underscore dibuang (bukan diganti spasi). */
	public static String hilangkanTandaBacaKecualiUnderscore(String s) {
		if (s == null) return "";
		return s.replaceAll("[^a-zA-Z0-9_]", "");
	}

	/**
	 * @return kunci unik ternormalisasi utk SATU baris produk -- kombinasi kode+barcode+nama+toko
	 *         supaya produk yang SAMA PERSIS keempatnya (dalam toko yang sama) tak mungkin lolos
	 *         sbg dua baris berbeda, terlepas beda tanda baca/spasi/huruf besar-kecil.
	 */
	public static String hitung(String kode, String barcode, String nama, Long tokoId) {
		String gabung = (kode == null ? "" : kode) + "_" + (barcode == null ? "" : barcode) + "_"
				+ (nama == null ? "" : nama) + "_" + (tokoId == null ? "" : tokoId);
		return hilangkanTandaBacaKecualiUnderscore(gabung).toLowerCase().trim();
	}

	/**
	 * Gap-closure permintaan user eksplisit: tolak baris produk yang KODE, BARCODE, DAN NAMA-nya
	 * SEKALIGUS kosong -- baris semacam itu tak punya identitas apa pun utk dibedakan dari baris
	 * lain (kunci_unik-nya cuma akan berisi id toko doang, gampang bentrok dgn baris sampah lain
	 * yg sama-sama kosong). Kode dan Nama SUDAH wajib diisi di validasi masing-masing pemanggil
	 * (produkSimpan/produkImporExcelKomit/ProdukAction) -- guard ini SENGAJA tetap dipasang di sini
	 * sbg SATU sumber kebenaran terpisah dari validasi per-layar, supaya jalur penulisan produk
	 * mana pun (termasuk yang belum/tidak sempat divalidasi ketat di layarnya sendiri) tetap
	 * tertutup dari kasus baris tanpa identitas ini.
	 * @return {@code true} bila SEKURANG-KURANGNYA satu dari kode/barcode/nama terisi (baris BOLEH
	 *         disimpan); {@code false} bila KETIGANYA kosong/null (baris WAJIB ditolak).
	 */
	public static boolean adaIdentitasProduk(String kode, String barcode, String nama) {
		boolean kodeKosong = kode == null || kode.trim().isEmpty();
		boolean barcodeKosong = barcode == null || barcode.trim().isEmpty();
		boolean namaKosong = nama == null || nama.trim().isEmpty();
		return !(kodeKosong && barcodeKosong && namaKosong);
	}
}
