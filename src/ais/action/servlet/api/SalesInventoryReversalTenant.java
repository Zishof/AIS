package ais.action.servlet.api;

/**
 * <h3>Jalur schema tenant untuk Reversal &amp; Log Cetak (P4, helper kedelapan — SEBAGIAN).</h3>
 *
 * <p>Lima dari tujuh aksi dipindahkan: {@code payablePaymentReverse}, {@code printLogCreate},
 * {@code printLogList}, {@code expenseReverse} (sejak v12), dan {@code collectionReverse}
 * (sejak v13). Dua sisanya — kedua aksi status giro — <b>ditolak</b> karena model tenant tidak
 * menyimpan status giro, bukan karena belum sempat ditulis.</p>
 *
 * <h4>Pembalikan hutang: model tenant justru lebih lengkap</h4>
 * <p>{@code pembayaran_hutang} punya {@code pembalik_dari_id}, {@code idempotency_key},
 * {@code status}, serta trio {@code dibatalkan} / {@code dibatalkan_pada} /
 * {@code alasan_batal}. Semua yang dilakukan jalur legacy — dokumen cermin bernilai negatif,
 * penunjuk ke dokumen asal, kunci idempotensi {@code REV-PHS-<id>}, penandaan dokumen asal
 * sebagai DIBATALKAN beserta alasannya — punya padanan langsung.</p>
 * <p>Ditambah {@code reversal_log}, yang tidak ada padanannya di legacy. Jejaknya bertambah.</p>
 *
 * <h4>Bentuk pembalikan dipertahankan persis: dokumen cermin negatif</h4>
 * <p>Alokasi pembalik bernilai negatif itu <b>bukan hiasan</b>. Rumus sisa hutang yang dipakai
 * helper Payable adalah {@code nilai − Σalokasi}; alokasi negatif mengembalikan sisa hutang ke
 * keadaan sebelum pembayaran tanpa menghapus baris apa pun. Riwayatnya tetap terbaca, dan
 * itulah inti prinsip "event posted tidak pernah dihapus".</p>
 *
 * <h4>Satu tambahan yang dipaksa oleh model</h4>
 * <p>{@code pembayaran_hutang.nomor_dokumen} berstatus {@code NOT NULL}, sedangkan jalur legacy
 * tidak memberi nomor pada dokumen pembalik AP. Nomornya karena itu diturunkan sebagai
 * {@code "REV-" + nomor asal}, mengikuti pola yang sudah dipakai legacy pada sisi piutang.</p>
 *
 * <h4>DUA aksi yang ditolak, dan alasannya</h4>
 *
 * <p><b>{@code collectionReverse} sudah TIDAK ditolak.</b> Dahulu ia menunggu dua hal
 * sekaligus: buku kas trip, dan nota bawaan bernilai tertagih. Bundel v12 menutup yang pertama,
 * v13 yang kedua.</p>
 * <p>v13 bahkan membuat separuh pekerjaannya lenyap. Jalur legacy harus menurunkan
 * {@code SpjSalesNota.nilaiTertagih} secara eksplisit lalu menjepitnya ke nol supaya tidak
 * negatif; pada model tenant angka itu diturunkan dari alokasi, dan alokasi pembalik memang
 * bernilai negatif — jumlahnya turun sendiri, tanpa penjepit dan tanpa pengurang yang bisa
 * terlupa.</p>
 *
 * <p><b>{@code expenseReverse} sudah TIDAK ditolak.</b> Dahulu {@code sales_trip_biaya} adalah
 * satu-satunya tabel dokumen tanpa {@code pembalik_dari_id}, tanpa {@code status}, dan tanpa
 * kolom metode pembayaran — ditambah tidak adanya tabel kas trip untuk mengembalikan uangnya.
 * Bundel <b>v12</b> menambahkan keempatnya, dan aksinya dipindahkan bersama bundel itu.</p>
 *
 * <p><b>{@code payableBgStatus} dan {@code collectionBgStatus}</b> — model tenant menyimpan
 * {@code nomor_bg} dan {@code tanggal_bg}, tetapi <b>tidak menyimpan statusnya</b>. Melacak
 * giro tanpa status berarti tidak dapat membedakan giro yang sudah cair dari yang ditolak, dan
 * itu justru inti kedua aksi ini. Jalur legacy juga menerbitkan reversal otomatis saat giro
 * ditolak; pada sisi piutang, reversal itu sendiri sedang tertutup.</p>
 *
 * <p>Ketiganya menunggu keputusan katalog. {@code collectionReverse} kini tinggal menunggu
 * <b>satu</b> hal saja — tabel nota bawaan bernilai tertagih — sebab sisi kasnya sudah dibuka
 * v12; kedua aksi giro menunggu kolom status giro.</p>
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

	/** Benar bila aksi ini sudah punya jalur tenant. */
	static boolean dukungAksi(String aksi) {
		return "payablePaymentReverse".equals(aksi) || "printLogCreate".equals(aksi)
				|| "printLogList".equals(aksi) || "expenseReverse".equals(aksi)
				|| "collectionReverse".equals(aksi);
	}

	// ------------------------------------------------------------------ pembalikan pembayaran AP

	/** Dokumen pembalik yang sudah ada, dikenali dari kunci idempotensinya. */
	static String cariPembalik(String skema) {
		return "SELECT id FROM " + skema + "pembayaran_hutang WHERE idempotency_key = ? LIMIT 1";
	}

	/**
	 * TUJUH kolom dokumen asal: nomorDokumen, supplierId, nilai, caraBayar, nomorBg, namaBank,
	 * status.
	 */
	static String asalPembayaran(String skema) {
		return "SELECT COALESCE(b.nomor_dokumen,''), b.supplier_id, COALESCE(b.nilai,0),"
				+ " b.cara_bayar, b.nomor_bg, b.nama_bank, COALESCE(b.status,'DRAF')"
				+ " FROM " + skema + "pembayaran_hutang b WHERE b.id = ?";
	}

	/**
	 * Dokumen pembalik: nilainya <b>negatif</b>, berstatus REVERSAL, menunjuk asalnya lewat
	 * {@code pembalik_dari_id}, dan ber-{@code idempotency_key} sehingga pengulangan permintaan
	 * tidak melahirkan pembalik kedua.
	 *
	 * <p>TIGA BELAS kolom; sebelas di antaranya parameter.</p>
	 */
	static String sisipPembalikPembayaran(String skema) {
		return "INSERT INTO " + skema + "pembayaran_hutang (nomor_dokumen, tanggal, supplier_id,"
				+ " cara_bayar, nomor_bg, nama_bank, nilai, keterangan, idempotency_key,"
				+ " pembalik_dari_id, status, dibuat_pada, oleh)"
				+ " VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, 'REVERSAL', now(), ?)";
	}

	/**
	 * Mencerminkan seluruh alokasi dokumen asal secara negatif dalam satu pernyataan.
	 *
	 * <p>Ditulis sebagai {@code INSERT ... SELECT} supaya tidak ada baris yang terlewat bila
	 * alokasinya banyak, dan supaya seluruhnya berada dalam transaksi yang sama dengan dokumen
	 * pembaliknya — pembalik tanpa alokasi akan mengembalikan uangnya tetapi tidak
	 * mengembalikan sisa hutangnya.</p>
	 */
	static String cerminkanAlokasi(String skema) {
		return "INSERT INTO " + skema + "alokasi_pembayaran_hutang (pembayaran_hutang_id,"
				+ " hutang_supplier_id, nilai, dibuat_pada, oleh)"
				+ " SELECT ?, a.hutang_supplier_id, -a.nilai, now(), ?"
				+ " FROM " + skema + "alokasi_pembayaran_hutang a"
				+ " WHERE a.pembayaran_hutang_id = ?";
	}

	/**
	 * Menandai dokumen asal DIBATALKAN berikut alasannya, sebagaimana jalur legacy. Barisnya
	 * tetap ada dan tetap terlihat di riwayat.
	 */
	static String batalkanAsal(String skema) {
		return "UPDATE " + skema + "pembayaran_hutang SET status = 'DIBATALKAN',"
				+ " dibatalkan = true, dibatalkan_pada = now(), alasan_batal = ?,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/**
	 * Catatan pembalikan. Tidak ada padanannya pada jalur legacy — di sana jejaknya hanya kolom
	 * penunjuk pada dokumennya sendiri. Diisi di sini karena tabelnya memang disediakan.
	 */
	static String catatReversal(String skema) {
		return "INSERT INTO " + skema + "reversal_log (dokumen_tipe, dokumen_id, alasan,"
				+ " user_id, waktu) VALUES ('PEMBAYARAN_HUTANG', ?, ?, ?, now())";
	}

	// ------------------------------------------------------------------ log cetak

	/**
	 * Nomor cetakan berikutnya untuk satu dokumen.
	 *
	 * <p>Jalur legacy <b>tidak punya</b> pencacah cetakan; kolom {@code cetakan_ke} milik model
	 * tenant. Diisi di sini karena membiarkannya bernilai bawaan 1 membuat setiap baris mengaku
	 * sebagai cetakan pertama.</p>
	 * <p>Dua permintaan cetak yang benar-benar bersamaan dapat memperoleh angka yang sama.
	 * Itu diterima: registernya bersifat catatan, bukan pengendali, dan tidak ada keputusan
	 * uang yang bergantung padanya.</p>
	 */
	static String cetakanBerikut(String skema) {
		return "SELECT COALESCE(MAX(cetakan_ke),0) + 1 FROM " + skema + "print_log"
				+ " WHERE dokumen_tipe = ? AND COALESCE(nomor_dokumen,'') = ?";
	}

	/**
	 * SEMBILAN kolom; delapan parameter.
	 *
	 * <p>{@code parameter} legacy ({@code LogCetak.parameterJson}) tidak punya kolom sendiri di
	 * sini dan disimpan pada {@code alasan}, satu-satunya kolom teks bebas yang tersedia.
	 * Namanya tidak cocok, tetapi menyimpannya di sana lebih baik daripada membuang isian yang
	 * pada jalur legacy tersimpan.</p>
	 */
	static String sisipLogCetak(String skema) {
		return "INSERT INTO " + skema + "print_log (dokumen_tipe, nomor_dokumen, cetakan_ke,"
				+ " user_id, device_id, alasan, waktu)"
				+ " VALUES (?, ?, ?, ?, ?, ?, now())";
	}

	/**
	 * ENAM kolom, berurutan sama dengan keluaran JSON jalur legacy: id, jenisDokumen,
	 * referensi, userId, perangkat, waktu.
	 */
	static String selectLogCetak(String skema, String where) {
		return "SELECT l.id, COALESCE(l.dokumen_tipe,''), COALESCE(l.nomor_dokumen,''),"
				+ " COALESCE(l.user_id,''), COALESCE(l.device_id,''), l.waktu"
				+ " FROM " + skema + "print_log l" + where
				+ " ORDER BY l.id DESC LIMIT 200";
	}
	// ------------------------------------------------------------------ pembalikan penagihan

	/**
	 * <h4>Pembalikan penagihan: penghalang terakhirnya jatuh bersama v13</h4>
	 *
	 * <p>Aksi ini dahulu ditolak karena dua hal sekaligus — tidak ada buku kas trip, dan tidak
	 * ada nota bawaan bernilai tertagih. Bundel v12 menutup yang pertama, v13 yang kedua.</p>
	 *
	 * <p><b>Dan v13 membuat separuh pekerjaannya lenyap.</b> Jalur legacy harus menurunkan
	 * {@code SpjSalesNota.nilaiTertagih} secara eksplisit, lalu menjepitnya ke nol supaya tidak
	 * negatif. Pada model tenant angka itu diturunkan dari alokasi, dan alokasi pembalik memang
	 * bernilai negatif — jumlahnya turun sendiri, tanpa penjepit, tanpa pengurang yang bisa
	 * terlupa. Yang tersisa hanyalah memutakhirkan <b>statusnya</b>.</p>
	 */
	static String cariPembalikPenerimaan(String skema) {
		return "SELECT id FROM " + skema + "penerimaan_piutang WHERE idempotency_key = ? LIMIT 1";
	}

	/**
	 * SEPULUH kolom dokumen asal berikut konteks tripnya: nomor, customerId, salespersonId,
	 * nilai, caraBayar, nomorBg, namaBank, status, salesTripId, statusTrip, spjId.
	 *
	 * <p>Status trip dan SPJ-nya ikut dibaca di sini supaya penjaga "sesi sudah ditutup" dan
	 * pemutakhiran nota bawaan tidak menuntut kueri tambahan. {@code LEFT JOIN} sebab penerimaan
	 * di kantor memang tidak punya trip.</p>
	 */
	static String asalPenerimaan(String skema) {
		return "SELECT COALESCE(p.nomor_dokumen,''), p.customer_id, p.salesperson_id,"
				+ " COALESCE(p.nilai,0), COALESCE(p.cara_bayar,''), p.nomor_bg, p.nama_bank,"
				+ " COALESCE(p.status,'DRAF'), p.sales_trip_id, COALESCE(t.status,''),"
				+ " t.surat_perintah_sales_id"
				+ " FROM " + skema + "penerimaan_piutang p"
				+ " LEFT JOIN " + skema + "sales_trip t ON p.sales_trip_id = t.id"
				+ " WHERE p.id = ?";
	}

	/** DUA kolom per baris: piutangId dan nilainya. */
	static String alokasiPenerimaan(String skema) {
		return "SELECT a.piutang_customer_id, COALESCE(a.nilai,0)"
				+ " FROM " + skema + "alokasi_penerimaan_piutang a"
				+ " WHERE a.penerimaan_piutang_id = ? ORDER BY a.id ASC";
	}

	/**
	 * Dokumen pembalik: nilainya negatif, berstatus REVERSAL, menunjuk asalnya.
	 *
	 * <p>SEBELAS parameter: nomor, customerId, salespersonId, caraBayar, nomorBg, namaBank,
	 * nilai, keterangan, idempotencyKey, pembalikDariId, salesTripId, oleh.</p>
	 */
	static String sisipPembalikPenerimaan(String skema) {
		return "INSERT INTO " + skema + "penerimaan_piutang (nomor_dokumen, tanggal, customer_id,"
				+ " salesperson_id, cara_bayar, nomor_bg, nama_bank, nilai, keterangan,"
				+ " idempotency_key, pembalik_dari_id, sales_trip_id, status, dibuat_pada, oleh)"
				+ " VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'REVERSAL', now(), ?)";
	}

	/** Mencerminkan seluruh alokasi dokumen asal secara negatif dalam satu pernyataan. */
	static String cerminkanAlokasiPiutang(String skema) {
		return "INSERT INTO " + skema + "alokasi_penerimaan_piutang (penerimaan_piutang_id,"
				+ " piutang_customer_id, nilai, dibuat_pada, oleh)"
				+ " SELECT ?, a.piutang_customer_id, -a.nilai, now(), ?"
				+ " FROM " + skema + "alokasi_penerimaan_piutang a"
				+ " WHERE a.penerimaan_piutang_id = ?";
	}

	/** Menandai penerimaan asal DIBATALKAN berikut alasannya. */
	static String batalkanPenerimaan(String skema) {
		return "UPDATE " + skema + "penerimaan_piutang SET status = 'DIBATALKAN',"
				+ " dibatalkan = true, dibatalkan_pada = now(), alasan_batal = ?,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/** Jejak pembalikan penagihan pada tabel umum {@code reversal_log}. */
	static String catatReversalPenerimaan(String skema) {
		return "INSERT INTO " + skema + "reversal_log (dokumen_tipe, dokumen_id, alasan,"
				+ " user_id, waktu) VALUES ('PENERIMAAN_PIUTANG', ?, ?, ?, now())";
	}
}
