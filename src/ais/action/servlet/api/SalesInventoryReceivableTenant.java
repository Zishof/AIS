package ais.action.servlet.api;

/**
 * <h3>Jalur schema tenant untuk Piutang &amp; Sales Order (P4, helper ketujuh — SEBAGIAN).</h3>
 *
 * <p>Enam aksi yang murni membaca dipindahkan di sini. Enam sisanya — yang menulis lewat
 * entitas Hibernate, termasuk {@code collectionCreate} yang mencatat penerimaan uang —
 * <b>ditolak</b> pada jalur tenant sampai ditulis beserta uji kesetaraannya.</p>
 *
 * <h4>Sisa piutang: dihitung, bukan dibaca dari kolom</h4>
 * <p>{@code piutang_customer} menyediakan {@code terbayar} dan {@code sisa}. Jalur ini
 * <b>sengaja tidak memakainya</b> dan menghitung ulang dari
 * {@code alokasi_penerimaan_piutang}, persis seperti jalur legacy menghitung dari alokasinya.</p>
 * <p>Alasannya sama dengan sisi hutang: kolom ringkasan adalah turunan yang bisa basi, dan
 * piutang yang basi berarti menagih pelanggan yang sudah membayar.</p>
 * <p>{@code dibayar_awal} legacy tidak punya kolom tersendiri di sini — pada model tenant uang
 * muka sudah berupa alokasi penerimaan biasa, sehingga tercakup penjumlahan alokasi.</p>
 *
 * <h4>Piutang tenant tidak menyimpan toko maupun order</h4>
 * <p>{@code piutang_customer} menautkan dirinya ke {@code faktur_penjualan}, dan fakturnya
 * yang menyimpan {@code toko_id} serta {@code sales_order_id}. Saringan lingkup toko dan
 * penyaringan per order karena itu ditempuh lewat faktur.</p>
 * <p>Itu <b>wajib</b> ditegakkan, bukan dilewati: saringan lingkup yang hilang berarti satu
 * toko melihat piutang toko lain.</p>
 *
 * <h4>Medan tanpa padanan</h4>
 * <p>{@code piutang_customer_doc.keterangan} tidak ada pada model tenant; kolomnya
 * dikembalikan kosong agar bentuk JSON tetap.</p>
 */
final class SalesInventoryReceivableTenant {

	private SalesInventoryReceivableTenant() {
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
		return "salesOrderList".equals(aksi) || "receivableList".equals(aksi)
				|| "collectionHistory".equals(aksi) || "receivableReport".equals(aksi)
				|| "receivableAgingCustomer".equals(aksi)
				|| "receivableAgingSales".equals(aksi);
	}

	// ------------------------------------------------------------------ ekspresi bersama

	/** Jumlah alokasi penerimaan atas satu dokumen piutang. */
	static String alokasi(String skema) {
		return "COALESCE((SELECT SUM(a.nilai) FROM " + skema + "alokasi_penerimaan_piutang a"
				+ " WHERE a.piutang_customer_id = d.id),0)";
	}

	/** Sisa piutang. Dihitung dari alokasi, bukan dibaca dari {@code d.sisa}. */
	static String outstanding(String skema) {
		return "(COALESCE(d.nilai,0) - " + alokasi(skema) + ")";
	}

	// ------------------------------------------------------------------ daftar sales order

	/** SEPULUH kolom: id, nomor, tanggal, status, total, keterangan, custId, custNama, salesId, salesNama. */
	static String selectSalesOrder() {
		return "SELECT o.id, COALESCE(o.nomor_dokumen,''), o.tanggal, COALESCE(o.status,''), "
				+ "COALESCE(o.total,0), COALESCE(o.keterangan,''), "
				+ "c.id, COALESCE(c.nama,''), s.id, COALESCE(s.nama,'')";
	}

	static String dasarSalesOrder(String skema, String where) {
		return " FROM " + skema + "sales_order o"
				+ " JOIN " + skema + "customer c ON o.customer_id = c.id"
				+ " LEFT JOIN " + skema + "salesperson s ON o.salesperson_id = s.id" + where;
	}

	// ------------------------------------------------------------------ daftar piutang

	/**
	 * LIMA BELAS kolom: id, nomor, tanggal, jatuhTempo, total, dibayarAwal, teralokasi,
	 * outstanding, custId, custNama, salesId, salesNama, orderId, orderNomor, keterangan.
	 *
	 * <p>{@code dibayarAwal} selalu nol dan {@code keterangan} selalu kosong — keduanya tidak
	 * punya padanan; posisinya dipertahankan supaya kolom sesudahnya tidak bergeser.</p>
	 */
	static String selectPiutang(String skema) {
		return "SELECT d.id, COALESCE(d.nomor_faktur,''), d.tanggal, d.jatuh_tempo, "
				+ "COALESCE(d.nilai,0), 0, " + alokasi(skema) + " AS teralokasi, "
				+ outstanding(skema) + " AS outstanding, "
				+ "c.id, COALESCE(c.nama,''), s.id, COALESCE(s.nama,''), "
				+ "o.id, COALESCE(o.nomor_dokumen,''), ''";
	}

	/**
	 * Piutang tenant menautkan diri ke faktur; fakturnya yang menyimpan toko dan order.
	 * Keduanya di-{@code LEFT JOIN} sebab piutang hasil impor legacy boleh belum tertaut.
	 */
	static String dasarPiutang(String skema, String where) {
		return " FROM " + skema + "piutang_customer d"
				+ " JOIN " + skema + "customer c ON d.customer_id = c.id"
				+ " LEFT JOIN " + skema + "salesperson s ON d.salesperson_id = s.id"
				+ " LEFT JOIN " + skema + "faktur_penjualan f ON d.faktur_penjualan_id = f.id"
				+ " LEFT JOIN " + skema + "sales_order o ON f.sales_order_id = o.id" + where;
	}

	/** Nama kolom untuk saringan, dengan alias jalur tenant. */
	static String kolomCustomerPiutang() {
		return "d.customer_id";
	}

	static String kolomSalesPiutang() {
		return "d.salesperson_id";
	}

	static String kolomOrderPiutang() {
		return "f.sales_order_id";
	}

	static String kolomTokoPiutang() {
		return "f.toko_id";
	}

	static String kolomNomorPiutang() {
		return "d.nomor_faktur";
	}

	// ------------------------------------------------------------------ riwayat penagihan

	/**
	 * EMPAT BELAS kolom: id, nomor, tanggal, nominal, metode, noBg, namaBank, keterangan,
	 * custId, custNama, salesNama, daftarFaktur, statusDok, statusBg.
	 *
	 * <p>{@code status_bg} tidak ada pada model tenant: legacy melacak status giro terpisah
	 * dari status dokumennya. Dikembalikan {@code NULL}, bukan disamakan dengan status
	 * dokumen -- menyamakannya membuat giro yang belum cair tampak sudah beres.</p>
	 */
	static String selectPenagihan(String skema) {
		return "SELECT p.id, COALESCE(p.nomor_dokumen,''), p.tanggal, COALESCE(p.nilai,0), "
				+ "COALESCE(p.cara_bayar,''), COALESCE(p.nomor_bg,''), COALESCE(p.nama_bank,''), "
				+ "COALESCE(p.keterangan,''), c.id, COALESCE(c.nama,''), COALESCE(s.nama,''), "
				+ "(SELECT COALESCE(string_agg(COALESCE(d2.nomor_faktur,'#' || d2.id), ', '),'')"
				+ " FROM " + skema + "alokasi_penerimaan_piutang a"
				+ " JOIN " + skema + "piutang_customer d2 ON a.piutang_customer_id = d2.id"
				+ " WHERE a.penerimaan_piutang_id = p.id) AS faktur, "
				+ "COALESCE(p.status,'AKTIF'), NULL";
	}

	/** Nama kolom customer dan sales pada penerimaan, untuk saringan. */
	static String kolomCustomerPenagihan() {
		return "p.customer_id";
	}

	static String kolomSalesPenagihan() {
		return "p.salesperson_id";
	}

	static String dasarPenagihan(String skema, String where) {
		return " FROM " + skema + "penerimaan_piutang p"
				+ " JOIN " + skema + "customer c ON p.customer_id = c.id"
				+ " LEFT JOIN " + skema + "salesperson s ON p.salesperson_id = s.id" + where;
	}

	static String kolomMetodePenagihan() {
		return "p.cara_bayar";
	}

	static String kolomNomorPenagihan() {
		return "p.nomor_dokumen";
	}

	// ------------------------------------------------------------------ laporan penjualan

	/**
	 * EMPAT kolom: produkId, produkNama, jumlah, subtotal.
	 *
	 * <p>Baris order tenant tidak menyimpan salinan nama produk; namanya ditarik lewat join.
	 * Bila produk berganti nama, laporan legacy menampilkan nama lama (salinan yang membeku)
	 * sedangkan laporan tenant menampilkan nama sekarang.</p>
	 */
	static String sqlLaporan(String skema, String where, String urut) {
		return "SELECT i.produk_id, COALESCE(pr.nama,''), SUM(COALESCE(i.kuantitas,0)),"
				+ " SUM(COALESCE(i.total,0))"
				+ " FROM " + skema + "sales_order_detail i"
				+ " JOIN " + skema + "sales_order o ON i.sales_order_id = o.id"
				+ " JOIN " + skema + "produk pr ON i.produk_id = pr.id" + where
				+ " GROUP BY i.produk_id, pr.nama ORDER BY " + urut + " LIMIT 500";
	}

	// ------------------------------------------------------------------ umur piutang

	/** EMPAT kolom: custId, custNama, bucket, jumlahOutstanding. */
	static String sqlAgingCustomer(String skema, String bucket, String where) {
		return "SELECT c.id, COALESCE(c.nama,''), " + bucket + " AS bucket, SUM("
				+ outstanding(skema) + ")"
				+ " FROM " + skema + "piutang_customer d"
				+ " JOIN " + skema + "customer c ON d.customer_id = c.id"
				+ " LEFT JOIN " + skema + "faktur_penjualan f ON d.faktur_penjualan_id = f.id"
				+ where + " GROUP BY c.id, c.nama, bucket ORDER BY c.nama";
	}

	/**
	 * TUJUH kolom: salesId, salesNama, jumlahDokumen, totalFaktur, totalTerbayar,
	 * totalOutstanding, jumlahLunas.
	 */
	static String sqlAgingSales(String skema, String where) {
		return "SELECT COALESCE(s.id,0), COALESCE(s.nama,'(tanpa sales)'), COUNT(d.id),"
				+ " SUM(COALESCE(d.nilai,0)), SUM(" + alokasi(skema) + "),"
				+ " SUM(" + outstanding(skema) + "),"
				+ " SUM(CASE WHEN " + outstanding(skema) + " <= 0.009 THEN 1 ELSE 0 END)"
				+ " FROM " + skema + "piutang_customer d"
				+ " LEFT JOIN " + skema + "salesperson s ON d.salesperson_id = s.id"
				+ " LEFT JOIN " + skema + "faktur_penjualan f ON d.faktur_penjualan_id = f.id"
				+ where + " GROUP BY s.id, s.nama ORDER BY 6 DESC";
	}

	/** Ember umur dengan nama kolom tenant. */
	static String bucketAging(String asOf) {
		String umur = "(DATE '" + asOf + "' - d.jatuh_tempo)";
		return "CASE WHEN d.jatuh_tempo IS NULL OR d.jatuh_tempo >= DATE '" + asOf + "' THEN 'BELUM' "
				+ "WHEN " + umur + " <= 30 THEN 'B1_30' "
				+ "WHEN " + umur + " <= 60 THEN 'B31_60' "
				+ "WHEN " + umur + " <= 90 THEN 'B61_90' ELSE 'B90' END";
	}
}
