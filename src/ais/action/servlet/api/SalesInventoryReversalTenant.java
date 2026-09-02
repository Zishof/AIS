package ais.action.servlet.api;

/**
 * <h3>Jalur schema tenant untuk Reversal &amp; Log Cetak (P4, helper kedelapan).</h3>
 *
 * <p>Empat dari tujuh aksi dipindahkan. Tiga sisanya ditolak, dan ketiganya karena model
 * tenant memang tidak menyediakan tempatnya — bukan karena belum sempat.</p>
 *
 * <h4>Pembalikan memang dirancang pada model tenant</h4>
 * <p>Berbeda dengan beberapa celah sebelumnya, sisi pembalikan justru <b>lebih matang</b> di
 * model tenant: kolom {@code pembalik_dari_id} tersedia pada enam tabel dokumen
 * ({@code faktur_penjualan}, {@code jurnal}, {@code mutasi_stok}, {@code pembayaran_hutang},
 * {@code pembelian}, {@code penerimaan_piutang}), dan ada tabel {@code reversal_log} yang
 * mencatat dokumen asal, jurnal asal, jurnal pembalik, alasan, serta pelakunya.</p>
 * <p>Jalur legacy hanya punya {@code reversalDari} pada dokumennya sendiri, tanpa catatan
 * terpisah. Jadi di sini jejaknya bertambah, bukan berkurang.</p>
 *
 * <h4>Bentuk pembalikan dipertahankan persis</h4>
 * <p>Legacy membalik dengan membuat dokumen cermin bernilai <b>negatif</b>, berstatus
 * REVERSAL, menunjuk dokumen asal, lalu mencerminkan tiap alokasinya juga secara negatif.
 * Jalur tenant melakukan hal yang sama.</p>
 * <p>Itu penting bagi rumus sisa yang dipakai helper Payable dan Piutang
 * ({@code nilai − Σalokasi}): alokasi negatif mengembalikan sisa hutang/piutang ke keadaan
 * sebelum pembayaran, tanpa menghapus apa pun. Riwayatnya tetap terbaca.</p>
 *
 * <h4>Idempotensi dipertahankan</h4>
 * <p>Legacy menjaga pembalikan ganda lewat {@code kodeUnik = "REV-PHS-<id>"}. Model tenant
 * punya {@code idempotency_key} pada kedua tabel pembayaran/penerimaan, sehingga jaminan yang
 * sama tetap berlaku — pembalikan yang diulang mengembalikan dokumen pembalik yang pertama,
 * bukan membuat yang kedua.</p>
 *
 * <h4>Tiga aksi yang DITOLAK, dan alasannya</h4>
 * <p><b>{@code expenseReverse}</b> — {@code sales_trip_biaya} adalah satu-satunya tabel
 * dokumen yang <b>tidak</b> punya {@code pembalik_dari_id}. Membalik biaya trip dengan
 * menyisipkan baris bernilai negatif tanpa penunjuk asalnya akan menghasilkan dua baris yang
 * tidak dapat dipasangkan kembali — total biayanya benar, tetapi tidak ada yang tahu baris
 * mana membalik baris mana.</p>
 * <p><b>{@code payableBgStatus} dan {@code collectionBgStatus}</b> — model tenant menyimpan
 * {@code nomor_bg} dan {@code tanggal_bg}, tetapi <b>tidak menyimpan statusnya</b>. Melacak
 * giro tanpa status berarti tidak dapat membedakan giro yang sudah cair dari yang ditolak,
 * dan itu justru inti kedua aksi ini.</p>
 * <p>Ketiganya menunggu keputusan katalog, bukan waktu.</p>
 */
final class SalesInventoryReversalTenant {

	private SalesInventoryReversalTenant() {
	}

	/** Benar bila aktor ini dilayani schema tenant. */
	static boolean aktif(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.aktif(aktor);
	}

	/** Prefiks schema berikut titiknya. */
	static String skema(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.skema(aktor.tenant);
	}

	/** Benar bila biaya trip dapat dibalik pada model tenant. */
	static boolean dukungPembalikanBiaya() {
		return false;
	}

	/** Benar bila status giro dapat dilacak pada model tenant. */
	static boolean dukungStatusGiro() {
		return false;
	}

	// ------------------------------------------------------------------ pembalikan pembayaran

	/** Dokumen pembalik yang sudah ada, dikenali dari kunci idempotensinya. */
	static String cariPembalik(String skema, String tabel) {
		return "SELECT id FROM " + skema + tabel + " WHERE idempotency_key = ? LIMIT 1";
	}

	/** Dokumen asal beserta medan yang perlu dicerminkan. */
	static String asalPembayaran(String skema) {
		return "SELECT b.supplier_id, COALESCE(b.nilai,0), COALESCE(b.cara_bayar,''),"
				+ " COALESCE(b.nomor_bg,''), COALESCE(b.nama_bank,''), COALESCE(b.status,'AKTIF')"
				+ " FROM " + skema + "pembayaran_hutang b WHERE b.id = ?";
	}

	static String asalPenerimaan(String skema) {
		return "SELECT p.customer_id, p.salesperson_id, COALESCE(p.nilai,0),"
				+ " COALESCE(p.cara_bayar,''), COALESCE(p.nomor_bg,''), COALESCE(p.nama_bank,''),"
				+ " COALESCE(p.status,'AKTIF')"
				+ " FROM " + skema + "penerimaan_piutang p WHERE p.id = ?";
	}

	/**
	 * Dokumen pembalik: nilainya <b>negatif</b>, berstatus REVERSAL, dan menunjuk asalnya lewat
	 * {@code pembalik_dari_id}.
	 */
	static String sisipPembalikPembayaran(String skema) {
		return "INSERT INTO " + skema + "pembayaran_hutang (nomor_dokumen, tanggal, supplier_id,"
				+ " cara_bayar, nomor_bg, nama_bank, nilai, keterangan, idempotency_key,"
				+ " pembalik_dari_id, status, dibuat_pada, oleh)"
				+ " VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, 'REVERSAL', now(), ?)";
	}

	static String sisipPembalikPenerimaan(String skema) {
		return "INSERT INTO " + skema + "penerimaan_piutang (nomor_dokumen, tanggal, customer_id,"
				+ " salesperson_id, cara_bayar, nomor_bg, nama_bank, nilai, keterangan,"
				+ " idempotency_key, pembalik_dari_id, status, dibuat_pada, oleh)"
				+ " VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'REVERSAL', now(), ?)";
	}

	/**
	 * Mencerminkan seluruh alokasi dokumen asal secara negatif.
	 *
	 * <p>Dikerjakan satu pernyataan {@code INSERT ... SELECT} supaya tidak ada baris yang
	 * terlewat bila jumlah alokasinya banyak, dan supaya keseluruhannya berada dalam satu
	 * transaksi yang sama dengan dokumen pembaliknya.</p>
	 */
	static String cerminkanAlokasiHutang(String skema) {
		return "INSERT INTO " + skema + "alokasi_pembayaran_hutang (pembayaran_hutang_id,"
				+ " hutang_supplier_id, nilai, dibuat_pada, oleh)"
				+ " SELECT ?, a.hutang_supplier_id, -a.nilai, now(), ?"
				+ " FROM " + skema + "alokasi_pembayaran_hutang a"
				+ " WHERE a.pembayaran_hutang_id = ? ORDER BY a.id ASC";
	}

	static String cerminkanAlokasiPiutang(String skema) {
		return "INSERT INTO " + skema + "alokasi_penerimaan_piutang (penerimaan_piutang_id,"
				+ " piutang_customer_id, nilai, dibuat_pada, oleh)"
				+ " SELECT ?, a.piutang_customer_id, -a.nilai, now(), ?"
				+ " FROM " + skema + "alokasi_penerimaan_piutang a"
				+ " WHERE a.penerimaan_piutang_id = ? ORDER BY a.id ASC";
	}

	/**
	 * Catatan pembalikan. Tidak ada padanannya pada jalur legacy — di sana jejaknya hanya
	 * kolom penunjuk pada dokumennya. Diisi di sini karena tabelnya memang disediakan.
	 */
	static String catatReversal(String skema) {
		return "INSERT INTO " + skema + "reversal_log (dokumen_tipe, dokumen_id, alasan,"
				+ " user_id, role, waktu) VALUES (?, ?, ?, ?, ?, now())";
	}

	// ------------------------------------------------------------------ log cetak

	/**
	 * Nomor cetakan berikutnya untuk satu dokumen. Legacy menghitungnya dari jumlah baris log
	 * yang sudah ada; bentuknya dipertahankan.
	 */
	static String cetakanBerikut(String skema) {
		return "SELECT COALESCE(MAX(cetakan_ke),0) + 1 FROM " + skema + "print_log"
				+ " WHERE dokumen_tipe = ? AND dokumen_id = ?";
	}

	static String sisipLogCetak(String skema) {
		return "INSERT INTO " + skema + "print_log (dokumen_tipe, dokumen_id, nomor_dokumen,"
				+ " cetakan_ke, user_id, role, device_id, alasan, waktu)"
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())";
	}

	/**
	 * DELAPAN kolom, berurutan sama dengan jalur legacy: id, dokumenTipe, dokumenId,
	 * nomorDokumen, cetakanKe, userId, alasan, waktu.
	 */
	static String selectLogCetak(String skema, String where) {
		return "SELECT l.id, COALESCE(l.dokumen_tipe,''), l.dokumen_id,"
				+ " COALESCE(l.nomor_dokumen,''), COALESCE(l.cetakan_ke,0),"
				+ " COALESCE(l.user_id,''), COALESCE(l.alasan,''), l.waktu"
				+ " FROM " + skema + "print_log l" + where
				+ " ORDER BY l.waktu DESC, l.id DESC LIMIT 200";
	}
}
