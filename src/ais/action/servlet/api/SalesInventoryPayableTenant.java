package ais.action.servlet.api;

/**
 * <h3>Jalur schema tenant untuk Hutang Supplier (P4, helper ketiga).</h3>
 *
 * <p>Jalur legacy di {@link SalesInventoryPayableHelper} tidak diubah; kelas ini menyediakan
 * potongan SQL penggantinya dengan kolom keluaran yang sama dan berurutan sama, sehingga
 * perakitan JSON dipakai bersama.</p>
 *
 * <h4>Legacy menyimpan faktur, tenant menyimpan HUTANG</h4>
 * <p>Perbedaan bentuknya lebih dalam daripada penggantian nama. Legacy menaruh seluruh faktur
 * kulakan di {@code pengadaan_faktur}, lalu menempelkan {@code payable_faktur_info} 1:1 untuk
 * medan yang khusus hutang (termin, jatuh tempo, DP), dan menyaring mana yang berhutang lewat
 * {@code i.jenis_pembayaran IN ('DP','CREDIT')}.</p>
 * <p>Model tenant memisahkannya: {@code pembelian} adalah dokumen pembeliannya, sedangkan
 * {@code hutang_supplier} <b>hanya berisi yang benar-benar berhutang</b>. Tabel sisi
 * {@code payable_faktur_info} lenyap -- medannya sudah menyatu.</p>
 * <p>Akibatnya saringan jenis pembayaran <b>tidak diperlukan</b> di jalur tenant: setiap baris
 * di {@code hutang_supplier} sudah pasti hutang. Menyalin saringan itu ke sini justru salah,
 * sebab kolomnya tidak ada dan konsepnya sudah terwujud sebagai keberadaan barisnya.</p>
 *
 * <h4>Pemetaan</h4>
 * <table border="1">
 * <tr><th>Legacy</th><th>Tenant</th></tr>
 * <tr><td>{@code pengadaan_faktur f} + {@code payable_faktur_info i}</td><td>{@code hutang_supplier h}</td></tr>
 * <tr><td>{@code alokasi_pembayaran_hutang_supplier} ({@code a.nominal}, {@code a.pengadaan_faktur})</td>
 *     <td>{@code alokasi_pembayaran_hutang} ({@code a.nilai}, {@code a.hutang_supplier_id})</td></tr>
 * <tr><td>{@code pembayaran_hutang_supplier}</td><td>{@code pembayaran_hutang}</td></tr>
 * <tr><td>{@code library.penyedia}</td><td>{@code <schema>.supplier}</td></tr>
 * <tr><td>{@code f.total_faktur_manual} / {@code f.total_hitung_saat_simpan}</td><td>{@code h.nilai}</td></tr>
 * <tr><td>{@code i.dibayar_awal}</td><td>alokasi pembayaran biasa</td></tr>
 * <tr><td>{@code f.tanggal_faktur}</td><td>{@code h.tanggal}</td></tr>
 * <tr><td>{@code i.jatuh_tempo}</td><td>{@code h.jatuh_tempo}</td></tr>
 * </table>
 *
 * <h4>Sisa hutang dihitung dari alokasi, bukan dibaca dari kolom</h4>
 * <p>{@code hutang_supplier} menyediakan {@code terbayar} dan {@code sisa}. Jalur ini
 * <b>sengaja tidak memakainya</b> dan menghitung ulang dari {@code alokasi_pembayaran_hutang},
 * persis seperti legacy menghitung dari alokasinya.</p>
 * <p>Alasannya sama dengan {@code saldo_stok} pada model stok: kolom ringkasan adalah turunan
 * yang bisa basi, dan angka hutang yang basi berarti membayar dua kali atau menagih yang sudah
 * lunas. Menghitung dari alokasi membuat hasilnya selalu sesuai dengan pembayaran yang benar-benar
 * tercatat.</p>
 */
final class SalesInventoryPayableTenant {

	private SalesInventoryPayableTenant() {
	}

	/** Benar bila aktor ini dilayani schema tenant. */
	static boolean aktif(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.aktif(aktor);
	}

	/** Prefiks schema berikut titiknya. */
	static String skema(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.skema(aktor.tenant);
	}

	/** Nilai hutang menurut dokumennya. */
	static String total() {
		return "COALESCE(h.nilai,0)";
	}

	/** Jumlah seluruh alokasi pembayaran atas satu hutang. */
	static String alokasi(String skema) {
		return "COALESCE((SELECT SUM(a.nilai) FROM " + skema + "alokasi_pembayaran_hutang a"
				+ " WHERE a.hutang_supplier_id = h.id),0)";
	}

	/** Sisa yang belum dibayar. Dihitung, bukan dibaca dari {@code h.sisa}. */
	static String outstanding(String skema) {
		return "(" + total() + " - " + alokasi(skema) + ")";
	}

	// ------------------------------------------------------------------ daftar hutang

	/**
	 * Awal klausa {@code WHERE}. Legacy menyaring {@code jenis_pembayaran IN ('DP','CREDIT')};
	 * di sini tidak perlu, sebab tabelnya memang hanya memuat hutang.
	 */
	static String whereAwal() {
		return " WHERE 1=1 ";
	}

	/**
	 * Alamat pemasok berada di {@code supplier_profile}, bukan di {@code supplier}. {@code LEFT
	 * JOIN} karena profil boleh belum diisi -- pemasok tanpa profil tetap harus muncul di daftar
	 * hutang, bukan hilang begitu saja.
	 */
	static String dasarDaftar(String skema, String where) {
		return " FROM " + skema + "hutang_supplier h JOIN " + skema + "supplier s"
				+ " ON h.supplier_id = s.id LEFT JOIN " + skema + "supplier_profile sp"
				+ " ON sp.supplier_id = s.id " + where;
	}

	/**
	 * Kolom daftar hutang, berurutan sama dengan jalur legacy: fakturId, supplierId,
	 * supplierKode, supplierNama, nomorFaktur, tanggalFaktur, jatuhTempo, jenisPembayaran,
	 * terminHari, total, dibayarAwal, alokasi, outstanding.
	 *
	 * <p>{@code jenisPembayaran} dan {@code terminHari} tidak ada pada model tenant. Kolomnya
	 * dipertahankan supaya bentuk JSON tidak berubah: jenis diisi {@code 'CREDIT'} -- itulah
	 * arti keberadaan baris hutang -- dan termin diturunkan dari selisih tanggal, yang memang
	 * definisinya.</p>
	 */
	static String selectDaftar(String skema) {
		return "SELECT h.id, COALESCE(h.nomor_faktur,''), h.tanggal, h.supplier_id, "
				+ "COALESCE(s.kode,''), COALESCE(s.nama,''), COALESCE(sp.alamat1,''), "
				+ "'CREDIT', "
				+ "CASE WHEN h.jatuh_tempo IS NULL OR h.tanggal IS NULL THEN NULL "
				+ "ELSE (h.jatuh_tempo - h.tanggal) END, h.jatuh_tempo, "
				+ total() + ", 0, " + alokasi(skema) + ", " + outstanding(skema);
	}

	/** Urutan sama dengan legacy: kode pemasok, lalu tanggal, lalu id. */
	static String urutDaftar() {
		return " ORDER BY s.kode ASC, h.tanggal ASC, h.id ASC LIMIT ? OFFSET ?";
	}

	/** Sisa satu hutang, untuk memeriksa kelebihan bayar sebelum alokasi baru. */
	static String outstandingSatu(String skema) {
		return "SELECT " + outstanding(skema) + " FROM " + skema + "hutang_supplier h WHERE h.id = ?";
	}

	// ------------------------------------------------------------------ riwayat pembayaran

	/**
	 * Rincian alokasi satu dokumen pembayaran. Legacy menautkan alokasi ke faktur lewat
	 * {@code a.pengadaan_faktur}; tenant menautkannya ke hutangnya lewat
	 * {@code a.hutang_supplier_id}.
	 */
	static String sqlRincianAlokasi(String skema) {
		return "SELECT a.hutang_supplier_id, COALESCE(h.nomor_faktur,'#' || h.id), a.nilai, "
				+ total() + ", h.jatuh_tempo"
				+ " FROM " + skema + "alokasi_pembayaran_hutang a"
				+ " JOIN " + skema + "hutang_supplier h ON a.hutang_supplier_id = h.id"
				+ " WHERE a.pembayaran_hutang_id = ? ORDER BY h.tanggal ASC";
	}

	// ------------------------------------------------------------------ umur hutang

	/**
	 * Umur hutang. Saringan jenis pembayaran legacy dijatuhkan dengan sengaja -- lihat catatan
	 * kelas: setiap baris {@code hutang_supplier} sudah pasti hutang.
	 */
	static String sqlAging(String skema, String bucket) {
		return "SELECT h.supplier_id, COALESCE(s.kode,''), COALESCE(s.nama,''), "
				+ "COALESCE(h.nomor_faktur,'#' || h.id), h.tanggal, h.jatuh_tempo, "
				+ outstanding(skema) + " AS outstanding, " + bucket + " AS bucket"
				+ " FROM " + skema + "hutang_supplier h"
				+ " JOIN " + skema + "supplier s ON h.supplier_id = s.id"
				+ " WHERE " + outstanding(skema) + " > 0.009"
				+ " ORDER BY s.kode ASC, h.jatuh_tempo ASC NULLS FIRST LIMIT 2000";
	}

	/** Ekspresi ember umur dengan nama kolom tenant. */
	static String bucketAging() {
		return "CASE WHEN h.jatuh_tempo IS NULL THEN 'TANPA_TEMPO'"
				+ " WHEN CURRENT_DATE <= h.jatuh_tempo THEN 'BELUM_JATUH'"
				+ " WHEN CURRENT_DATE - h.jatuh_tempo <= 30 THEN '1_30'"
				+ " WHEN CURRENT_DATE - h.jatuh_tempo <= 60 THEN '31_60'"
				+ " WHEN CURRENT_DATE - h.jatuh_tempo <= 90 THEN '61_90'"
				+ " ELSE 'DI_ATAS_90' END";
	}

	// ------------------------------------------------------------------ laporan pembelian

	/**
	 * Laporan pembelian. Diskon berada di dokumen {@code pembelian}, bukan di hutangnya,
	 * sehingga ditarik lewat {@code h.pembelian_id}. {@code LEFT JOIN} karena hutang hasil
	 * impor legacy bisa saja belum tertaut ke dokumen pembelian mana pun.
	 */
	static String dasarLaporan(String skema, String where) {
		return " FROM " + skema + "hutang_supplier h"
				+ " LEFT JOIN " + skema + "supplier s ON h.supplier_id = s.id"
				+ " LEFT JOIN " + skema + "pembelian b ON h.pembelian_id = b.id " + where;
	}

	static String selectLaporan(String skema) {
		return "SELECT h.id, COALESCE(h.nomor_faktur,'#' || h.id), h.tanggal, "
				+ "COALESCE(s.kode,''), COALESCE(s.nama,''), 'CREDIT', "
				+ total() + ", 0, " + alokasi(skema) + ", COALESCE(b.diskon,0)";
	}

	static String urutLaporan() {
		return " ORDER BY h.tanggal ASC, h.id ASC LIMIT 3000";
	}
}
