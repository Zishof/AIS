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
				|| "receivableAgingSales".equals(aksi)
				|| "salesOrderDetail".equals(aksi) || "salesOrderStatus".equals(aksi)
				|| "collectionReceipt".equals(aksi);
	}

	// ------------------------------------------------------------------ kosakata status

	/**
	 * <h4>Katalog tenant menyingkat DRAFT menjadi DRAF, jalur {@code si_*} tidak.</h4>
	 *
	 * <p>{@code SalesOrderLapangan.STATUS_DRAFT} bernilai {@code "DRAFT"}, sedangkan bawaan
	 * kolom {@code sales_order.status} pada katalog tenant adalah {@code 'DRAF'} — begitu pula
	 * pada dua belas tabel lain. Itu kosakata katalog, bukan salah ketik satu tempat.</p>
	 *
	 * <p>Akibatnya nyata: penjaga transisi membandingkan {@code "DRAFT".equals(lama)}, sehingga
	 * setiap order yang statusnya berasal dari bawaan kolom akan <b>menolak seluruh transisi
	 * keluar dari draf</b> — tidak dapat dikonfirmasi dan tidak dapat dibatalkan.</p>
	 *
	 * <p>Diterjemahkan di batas, bukan dilawan: basis data tetap berbicara kosakata katalog,
	 * API tetap berbicara kosakata legacy. Nilai lain ({@code PESAN}, {@code SIAP_KIRIM},
	 * {@code TERKIRIM}, {@code BATAL}) sudah sama persis di kedua sisi.</p>
	 */
	static String statusOrder(String alias) {
		return "CASE WHEN " + alias + ".status = 'DRAF' THEN 'DRAFT'"
				+ " ELSE COALESCE(" + alias + ".status,'') END";
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

	/**
	 * SEPULUH kolom: id, nomor, tanggal, status, total, keterangan, custId, custNama, salesId,
	 * salesNama.
	 *
	 * <p>Statusnya dinormalkan lewat {@link #statusOrder(String)}. Sebelumnya kolom ini
	 * dikembalikan apa adanya, sehingga klien tenant menerima {@code DRAF} di tempat klien
	 * legacy menerima {@code DRAFT}.</p>
	 */
	static String selectSalesOrder() {
		return "SELECT o.id, COALESCE(o.nomor_dokumen,''), o.tanggal, " + statusOrder("o") + ", "
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
	// ------------------------------------------------------------------ rincian sales order

	/**
	 * TIGA BELAS kolom: id, nomor, tanggal, status, total, keterangan, alasanBatal, custId,
	 * custNama, salesId, salesNama, piutangDocId, piutangDocNomor.
	 *
	 * <p><b>Piutang tenant tidak menunjuk order secara langsung.</b> Legacy menyimpan
	 * {@code PiutangCustomerDoc.salesOrder}; pada model tenant kaitannya melewati fakturnya
	 * ({@code piutang_customer} &rarr; {@code faktur_penjualan} &rarr; {@code sales_order}).</p>
	 *
	 * <p>Kedua subkueri sengaja memakai {@code ORDER BY d.id LIMIT 1} yang sama supaya id dan
	 * nomornya pasti berasal dari baris yang sama. Jalur legacy memakai {@code setMaxResults(1)}
	 * tanpa urutan — hasilnya sewenang-wenang bila satu order punya lebih dari satu dokumen
	 * piutang. Di sini hasilnya menjadi tentu, dan itu perbedaan yang memperbaiki.</p>
	 */
	static String selectOrderRinci(String skema) {
		String piutang = " FROM " + skema + "piutang_customer d"
				+ " JOIN " + skema + "faktur_penjualan f ON d.faktur_penjualan_id = f.id"
				+ " WHERE f.sales_order_id = o.id ORDER BY d.id LIMIT 1";
		return "SELECT o.id, COALESCE(o.nomor_dokumen,''), o.tanggal, " + statusOrder("o") + ","
				+ " COALESCE(o.total,0), COALESCE(o.keterangan,''), COALESCE(o.alasan_batal,''),"
				+ " c.id, COALESCE(c.nama,''), s.id, COALESCE(s.nama,''),"
				+ " (SELECT d.id" + piutang + "),"
				+ " (SELECT COALESCE(d.nomor_faktur,'')" + piutang + ")"
				+ " FROM " + skema + "sales_order o"
				+ " JOIN " + skema + "customer c ON o.customer_id = c.id"
				+ " LEFT JOIN " + skema + "salesperson s ON o.salesperson_id = s.id"
				+ " WHERE o.id = ?";
	}

	/**
	 * ENAM kolom: id, produkId, namaProduk, hargaSatuan, jumlah, subtotal.
	 *
	 * <p>{@code namaProduk} ditarik lewat join. Jalur legacy menyimpan salinan nama yang membeku
	 * saat order dibuat, sehingga produk yang berganti nama tampil dengan nama lama di sana dan
	 * nama sekarang di sini.</p>
	 */
	static String selectOrderBaris(String skema) {
		return "SELECT i.id, i.produk_id, COALESCE(pr.nama,''), COALESCE(i.harga_satuan,0),"
				+ " COALESCE(i.kuantitas,0), COALESCE(i.total,0)"
				+ " FROM " + skema + "sales_order_detail i"
				+ " JOIN " + skema + "produk pr ON i.produk_id = pr.id"
				+ " WHERE i.sales_order_id = ? ORDER BY i.id ASC";
	}

	// ------------------------------------------------------------------ transisi status order

	/** DUA kolom: status (ternormalkan) dan salesperson_id, untuk penjaga transisi dan lingkup. */
	static String selectOrderUntukStatus(String skema) {
		return "SELECT " + statusOrder("o") + ", o.salesperson_id"
				+ " FROM " + skema + "sales_order o WHERE o.id = ?";
	}

	/**
	 * Menulis status baru berikut alasan pembatalannya.
	 *
	 * <p>{@code alasan_batal} diisi hanya saat status barunya BATAL; pada transisi lain
	 * parameternya bernilai {@code NULL} dan kolomnya dibiarkan seperti semula — sama seperti
	 * jalur legacy yang hanya memanggil {@code setAlasanBatal} pada cabang pembatalan.</p>
	 */
	static String updateStatusOrder(String skema) {
		return "UPDATE " + skema + "sales_order SET status = ?,"
				+ " alasan_batal = COALESCE(?, alasan_batal),"
				+ " oleh = ?, tanggal_dirubah = now() WHERE id = ?";
	}

	/**
	 * <h4>MTO tidak punya apa pun untuk dipicu pada model tenant</h4>
	 *
	 * <p>Jalur legacy menjalankan {@code terapkanMto} pada transisi DRAFT &rarr; PESAN: baris
	 * ber-produk rute {@code MTO_PRODUKSI} menerbitkan draf Work Order, rute {@code MTO_BELI}
	 * menerbitkan pengajuan pembelian gudang.</p>
	 *
	 * <p>Katalog tenant <b>tidak punya kolom {@code produk.rute}</b>, dan tidak punya tabel
	 * work order maupun pengajuan pembelian. Tidak ada produk tenant yang dapat berute MTO,
	 * sehingga pemicunya kosong menurut definisi — bukan dilewati diam-diam.</p>
	 *
	 * <p>Penjaga ini ada supaya keadaan itu berhenti benar dengan berisik bila suatu bundel
	 * kelak menambahkan {@code produk.rute}: konfirmasi order yang seharusnya menerbitkan Work
	 * Order tetapi tidak, adalah data yang berbohong.</p>
	 */
	static boolean mtoMungkin() {
		return false;
	}

	// ------------------------------------------------------------------ kwitansi penerimaan

	/**
	 * TIGA BELAS kolom. Dua belas pertama masuk JSON, berurutan sama dengan jalur legacy: id,
	 * nomor, tanggal, nominal, metode, noBg, namaBank, keterangan, custNama, custKode,
	 * salesNama, dibuatOleh.
	 *
	 * <p>Kolom ke-13, {@code salesperson_id}, <b>tidak ikut JSON</b>. Ia hanya dipakai penjaga
	 * lingkup: sales lapangan hanya boleh membuka kwitansinya sendiri, dan penjaga itu perlu id
	 * — bukan nama, yang bisa sama antar dua orang.</p>
	 */
	static String selectKwitansi(String skema) {
		return "SELECT p.id, COALESCE(p.nomor_dokumen,''), p.tanggal, COALESCE(p.nilai,0),"
				+ " COALESCE(p.cara_bayar,''), COALESCE(p.nomor_bg,''), COALESCE(p.nama_bank,''),"
				+ " COALESCE(p.keterangan,''), COALESCE(c.nama,''), COALESCE(c.kode,''),"
				+ " COALESCE(s.nama,''), COALESCE(p.oleh,''), p.salesperson_id"
				+ " FROM " + skema + "penerimaan_piutang p"
				+ " JOIN " + skema + "customer c ON p.customer_id = c.id"
				+ " LEFT JOIN " + skema + "salesperson s ON p.salesperson_id = s.id"
				+ " WHERE p.id = ?";
	}

	/** EMPAT kolom: fakturNomor, fakturTanggal, totalFaktur, nominal. */
	static String selectAlokasiKwitansi(String skema) {
		return "SELECT COALESCE(d.nomor_faktur,''), d.tanggal, COALESCE(d.nilai,0),"
				+ " COALESCE(a.nilai,0)"
				+ " FROM " + skema + "alokasi_penerimaan_piutang a"
				+ " JOIN " + skema + "piutang_customer d ON a.piutang_customer_id = d.id"
				+ " WHERE a.penerimaan_piutang_id = ? ORDER BY a.id ASC";
	}
}
