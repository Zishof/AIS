package ais.action.servlet.api;

import ais.service.tenant.TenantContext;

/**
 * <h3>Jalur schema tenant untuk Laporan Opname — layar legacy 09-10.</h3>
 *
 * <p>Sebagaimana {@link SalesInventoryStokTenant}, kelas ini hanya menyediakan bagian
 * {@code SELECT}; pembungkus, paginasi, dan perakitan JSON di {@link SalesInventoryOpnameHelper}
 * dipakai bersama kedua jalur. Kolom keluarannya sama dan berurutan sama dengan jalur legacy,
 * sehingga tidak ada satu baris perakitan hasil pun yang digandakan.</p>
 *
 * <h4>Perbedaan bentuk yang membuat kelas ini perlu</h4>
 * <p>{@code koperasi.stok_opname} adalah baris DATAR per produk: satu baris = satu produk yang
 * dihitung, dengan {@code waktuopname} sendiri-sendiri dan tanpa kepala dokumen. Model tenant
 * memisahkan kepala ({@code stok_opname}: nomor dokumen, tanggal, gudang, status) dari rincinya
 * ({@code stok_opname_detail}). Karena itu jalur legacy harus <b>mengelompokkan</b> baris datar
 * menjadi sesi-per-tanggal supaya punya bentuk yang sama, sedangkan jalur tenant tinggal membaca
 * kepalanya. Ini bukan penggantian prefiks schema; keduanya kueri yang berbeda.</p>
 *
 * <h4>Lingkup toko = lingkup gudang (&sect;16)</h4>
 * <p>Legacy menyaring {@code stok_opname.toko}. Model tenant tidak punya kolom itu — yang menjadi
 * milik satu toko adalah <b>gudangnya</b>, jadi lingkupnya ditegakkan lewat
 * {@code gudang.toko_id}. Alasan lengkapnya di
 * {@link SalesInventoryStokTenant#syaratTokoProduk(String, Long)}.</p>
 *
 * <h4>Mengapa total dihitung dari rincinya, bukan disimpan di kepala</h4>
 * <p>Kepala opname tidak menyimpan jumlah produk maupun total selisih, dan memang tidak boleh:
 * itu turunan dari rincinya ("derive, don't store"). Menyimpannya berarti dua sumber kebenaran
 * yang bisa berselisih diam-diam sesudah satu baris rinci disunting.</p>
 */
final class SalesInventoryOpnameTenant {

	private SalesInventoryOpnameTenant() {
	}

	/** Benar bila aktor ini dilayani schema tenant. */
	static boolean aktif(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.aktif(aktor);
	}

	/** Prefiks schema berikut titiknya. */
	static String skema(TenantContext tenant) {
		return SalesInventoryTenantSchema.skema(tenant);
	}

	/**
	 * Pembatas gudang untuk kepala opname; kosong bila tanpa lingkup toko.
	 *
	 * <p>{@code tokoId} disambung sebagai literal, bukan {@code ?}: ekspresi ini muncul di dalam
	 * {@code SELECT}, dan menyisipkan parameter di sana akan mendahului parameter {@code where}
	 * sehingga urutan pengikatannya kacau — jebakan yang sama sudah dicatat di
	 * {@link SalesInventoryStokTenant}.</p>
	 */
	static String syaratTokoOpname(String skema, Long tokoId) {
		if (tokoId == null) {
			return "";
		}
		return " AND EXISTS (SELECT 1 FROM " + skema + "gudang g"
				+ " WHERE g.id = o.gudang_id AND g.toko_id = " + tokoId.longValue() + ") ";
	}

	/**
	 * {@code SELECT} pengganti untuk {@code si_stock_count_list}: satu baris per DOKUMEN opname.
	 *
	 * <p>Kolom, berurutan: id, nomor, tanggal, gudang, status, keterangan, oleh, jumlah_produk,
	 * total_lebih, total_kurang, selisih_bersih, nilai_selisih.</p>
	 *
	 * <p>{@code total_lebih} dan {@code total_kurang} dipisah, bukan hanya selisih bersihnya:
	 * opname yang kelebihan 50 pada satu produk dan kekurangan 50 pada produk lain berselisih
	 * bersih NOL, padahal itu justru sesi yang paling perlu diperiksa. Menampilkan bersihnya saja
	 * menyembunyikan tepat kasus yang dicari laporan ini.</p>
	 *
	 * <p>{@code nilai_selisih} memakai {@code harga_satuan} rinci bila ada, dan jatuh ke harga
	 * beli produk bila tidak — potret harga saat opname lebih benar daripada harga hari ini, tetapi
	 * baris impor legacy tidak membawanya.</p>
	 */
	static String selectDaftar(String skema, String where) {
		String rinci = " FROM " + skema + "stok_opname_detail d"
				+ " WHERE d.stok_opname_id = o.id AND COALESCE(d.legacy_deleted, false) = false";
		return "SELECT o.id AS id,"
				+ " COALESCE(o.nomor_dokumen,'') AS nomor,"
				+ " o.tanggal AS tanggal,"
				+ " COALESCE(NULLIF(TRIM(g.nama),''),'(Tanpa gudang)') AS gudang,"
				+ " COALESCE(o.status,'') AS status,"
				+ " COALESCE(o.keterangan,'') AS keterangan,"
				+ " COALESCE(o.oleh,'') AS oleh,"
				+ " COALESCE((SELECT COUNT(*)" + rinci + "),0) AS jumlah_produk,"
				+ " COALESCE((SELECT SUM(CASE WHEN d.selisih > 0 THEN d.selisih ELSE 0 END)"
				+ rinci + "),0) AS total_lebih,"
				+ " COALESCE((SELECT SUM(CASE WHEN d.selisih < 0 THEN -d.selisih ELSE 0 END)"
				+ rinci + "),0) AS total_kurang,"
				+ " COALESCE((SELECT SUM(d.selisih)" + rinci + "),0) AS selisih_bersih,"
				+ " COALESCE((SELECT SUM(d.selisih * COALESCE(NULLIF(d.harga_satuan,0),"
				+ " (SELECT COALESCE(p.harga_beli_terakhir,0) FROM " + skema + "produk p WHERE p.id = d.produk_id)))"
				+ rinci + "),0) AS nilai_selisih"
				+ " FROM " + skema + "stok_opname o"
				+ " LEFT JOIN " + skema + "gudang g ON g.id = o.gudang_id"
				+ where;
	}

	/** Saringan dasar daftar: dokumen yang belum dihapus, dalam rentang tanggal tervalidasi. */
	static String whereDasar(String dari, String sampai) {
		return " WHERE COALESCE(o.legacy_deleted, false) = false"
				+ " AND o.tanggal BETWEEN DATE '" + dari + "' AND DATE '" + sampai + "' ";
	}

	/**
	 * {@code SELECT} untuk mode <b>per produk</b> — bentuk yang dipakai layar legacy 09.
	 *
	 * <h4>Mengapa mode ini ada</h4>
	 * <p>Layar legacy "LAPORAN STOK OPNAME" bukan daftar sesi: ia daftar <b>datar</b> satu baris
	 * per produk per tanggal, berkolom TGL.OPNAME, #KODE, NAMA BARANG, SAT., STOK KOMP., STOK
	 * FISIK, SELISIH, HRG.POKOK, TOTAL HARGA, dengan satu angka total di kanan bawah. Daftar sesi
	 * menjawab pertanyaan lain ("opname apa saja yang pernah dilakukan"), dan berguna — tetapi ia
	 * bukan padanan layar 09. Keduanya karena itu disediakan, dan mode per produk yang menjadi
	 * bawaan.</p>
	 *
	 * <p>Kolom, berurutan: id_sesi, tanggal, kode, nama, satuan, sistem, fisik, selisih, harga,
	 * nilai. {@code id_sesi} ikut supaya baris tetap dapat ditelusuri ke dokumennya.</p>
	 */
	static String selectBarisProduk(String skema, String where) {
		String harga = "COALESCE(NULLIF(d.harga_satuan,0), COALESCE(p.harga_beli_terakhir,0))";
		return "SELECT o.id AS id_sesi,"
				+ " o.tanggal AS tanggal,"
				+ " COALESCE(p.kode,'') AS kode,"
				+ " COALESCE(p.nama,'') AS nama,"
				+ " COALESCE(NULLIF(TRIM(s.nama),''),'(Belum diatur)') AS satuan,"
				+ " COALESCE(d.kuantitas_sistem,0) AS sistem,"
				+ " COALESCE(d.kuantitas_fisik,0) AS fisik,"
				+ " COALESCE(d.selisih,0) AS selisih,"
				+ " " + harga + " AS harga,"
				+ " COALESCE(d.selisih,0) * " + harga + " AS nilai"
				+ " FROM " + skema + "stok_opname_detail d"
				+ " JOIN " + skema + "stok_opname o ON o.id = d.stok_opname_id"
				+ " LEFT JOIN " + skema + "produk p ON p.id = d.produk_id"
				+ " LEFT JOIN " + skema + "satuan s ON s.id = p.satuan_id"
				+ where
				+ " AND COALESCE(d.legacy_deleted, false) = false";
	}

	/**
	 * {@code SELECT} pengganti untuk {@code si_stock_count_detail}: rincian satu dokumen.
	 *
	 * <p>Kolom, berurutan: produk_id, kode, nama, satuan, sistem, fisik, selisih, harga, nilai,
	 * keterangan.</p>
	 */
	static String selectRinci(String skema) {
		String harga = "COALESCE(NULLIF(d.harga_satuan,0), COALESCE(p.harga_beli_terakhir,0))";
		return "SELECT d.produk_id AS produk_id,"
				+ " COALESCE(p.kode,'') AS kode,"
				+ " COALESCE(p.nama,'') AS nama,"
				+ " COALESCE(NULLIF(TRIM(s.nama),''),'(Belum diatur)') AS satuan,"
				+ " COALESCE(d.kuantitas_sistem,0) AS sistem,"
				+ " COALESCE(d.kuantitas_fisik,0) AS fisik,"
				+ " COALESCE(d.selisih,0) AS selisih,"
				+ " " + harga + " AS harga,"
				+ " COALESCE(d.selisih,0) * " + harga + " AS nilai,"
				+ " COALESCE(d.keterangan,'') AS keterangan"
				+ " FROM " + skema + "stok_opname_detail d"
				+ " LEFT JOIN " + skema + "produk p ON p.id = d.produk_id"
				+ " LEFT JOIN " + skema + "satuan s ON s.id = p.satuan_id"
				+ " WHERE d.stok_opname_id = ? AND COALESCE(d.legacy_deleted, false) = false"
				+ " ORDER BY COALESCE(p.kode,'')";
	}

	/** Kepala satu dokumen, untuk menyertai rincinya. */
	static String selectKepala(String skema) {
		return "SELECT o.id, COALESCE(o.nomor_dokumen,''), o.tanggal,"
				+ " COALESCE(NULLIF(TRIM(g.nama),''),'(Tanpa gudang)'), COALESCE(o.status,''),"
				+ " COALESCE(o.keterangan,''), COALESCE(o.oleh,'')"
				+ " FROM " + skema + "stok_opname o"
				+ " LEFT JOIN " + skema + "gudang g ON g.id = o.gudang_id"
				+ " WHERE o.id = ?";
	}
}
