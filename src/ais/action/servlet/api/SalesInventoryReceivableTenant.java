package ais.action.servlet.api;

/**
 * <h3>Jalur schema tenant untuk Piutang &amp; Sales Order (P4, helper ketujuh — TUNTAS).</h3>
 *
 * <p><b>Kedua belas aksinya kini berjalan pada schema tenant.</b> Enam yang murni membaca
 * dipindahkan lebih dulu; enam penulis menyusul bertahap, dan yang terakhir —
 * {@code collectionCreate} — menunggu dua bundel katalog sekaligus: v12 untuk buku kas trip dan
 * v13 untuk nota bawaan. Tidak ada lagi aksi Piutang yang gagal-tertutup.</p>
 *
 * <h4>Sisa piutang: dihitung, bukan dibaca dari kolom</h4>
 * <p>{@code piutang_customer} menyediakan {@code terbayar} dan {@code sisa}. Jalur ini
 * <b>sengaja tidak memakainya</b> dan menghitung ulang dari
 * {@code alokasi_penerimaan_piutang}, persis seperti jalur legacy menghitung dari alokasinya.</p>
 * <p>Alasannya sama dengan sisi hutang: kolom ringkasan adalah turunan yang bisa basi, dan
 * piutang yang basi berarti menagih pelanggan yang sudah membayar.</p>
 * <p>{@code dibayar_awal} legacy tidak punya kolom tersendiri di sini — pada model tenant uang
 * muka sudah berupa alokasi penerimaan biasa, sehingga tercakup penjumlahan alokasi. Sejak
 * §18 pemfakturan pun menerbitkannya sebagai dokumen penerimaan sungguhan, bukan
 * menolaknya — lihat {@link #catatanUangMuka()}.</p>
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
				|| "collectionReceipt".equals(aksi) || "salesOrderSimpan".equals(aksi)
				|| "salesOrderInvoice".equals(aksi) || "collectionCreate".equals(aksi);
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
	 * <p>{@code status_bg} ada sejak bundel v17. Sebelumnya dikembalikan {@code NULL}, dan
	 * sengaja tidak disamakan dengan status dokumen — menyamakannya membuat giro yang belum cair
	 * tampak sudah beres.</p>
	 */
	static String selectPenagihan(String skema) {
		return "SELECT p.id, COALESCE(p.nomor_dokumen,''), p.tanggal, COALESCE(p.nilai,0), "
				+ "COALESCE(p.cara_bayar,''), COALESCE(p.nomor_bg,''), COALESCE(p.nama_bank,''), "
				+ "COALESCE(p.keterangan,''), c.id, COALESCE(c.nama,''), COALESCE(s.nama,''), "
				+ "(SELECT COALESCE(string_agg(COALESCE(d2.nomor_faktur,'#' || d2.id), ', '),'')"
				+ " FROM " + skema + "alokasi_penerimaan_piutang a"
				+ " JOIN " + skema + "piutang_customer d2 ON a.piutang_customer_id = d2.id"
				+ " WHERE a.penerimaan_piutang_id = p.id) AS faktur, "
				+ "COALESCE(p.status,'AKTIF'), COALESCE(p.status_bg,'')";
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
	// ------------------------------------------------------------------ penyaring status

	/**
	 * Ekspresi status untuk dipakai pada klausa {@code WHERE}.
	 *
	 * <p>Penyaring {@code salesOrderList} membandingkan status kiriman klien terhadap kolomnya
	 * secara mentah. Klien mengirim {@code DRAFT} (kosakata legacy) sedangkan kolom tenant
	 * berisi {@code DRAF}, sehingga saringan "hanya draf" mengembalikan <b>kosong</b> — bukan
	 * salah tampil, melainkan order yang hilang dari daftar.</p>
	 *
	 * <p>Sama seperti pada pembacaan, yang dinormalkan adalah sisi basis datanya.</p>
	 */
	static String syaratStatusOrder() {
		return " AND " + statusOrder("o") + " = ?";
	}

	// ------------------------------------------------------------------ simpan sales order

	/**
	 * <h4>Tiga hal jalur legacy yang TIDAK punya tempat pada model tenant</h4>
	 *
	 * <p><b>1. Satuan jual (konversi UoM).</b> Legacy menurunkan jumlah dasar dari
	 * {@code qty_input × faktor} lewat {@code KantinHelper.faktorUomInputKeDasar}, dan menegaskan
	 * jumlah kiriman klien hanya pratinjau. Katalog tenant tidak punya tabel {@code satuan_produk}
	 * maupun kolom penampung {@code satuan_jual}/{@code qty_input}/{@code faktor_ke_dasar} pada
	 * baris order. Permintaan yang menyertakan {@code satuan_jual_id} karena itu <b>ditolak</b>:
	 * menerimanya berarti membiarkan angka pratinjau klien menjadi angka resmi, dan itu justru
	 * yang dijaga jalur legacy.</p>
	 *
	 * <p><b>2. Salinan nama produk.</b> Baris order tenant tidak menyimpannya; namanya ditarik
	 * lewat join saat dibaca. Produk yang berganti nama tampil dengan nama sekarang.</p>
	 *
	 * <p><b>3. Snapshot HPP per baris.</b> Legacy membekukan harga beli pada tiap baris order
	 * untuk perhitungan margin. Model tenant menaruh biaya pada {@code faktur_penjualan.hpp} —
	 * di tingkat faktur, bukan baris, dan pada saat pemfakturan, bukan pemesanan. Itu perbedaan
	 * rancangan, dan akibatnya margin per baris order tidak dapat dihitung mundur. Dicatat
	 * sebagai celah C-12; tidak ada kueri tenant yang membacanya saat ini.</p>
	 */
	static String cariOrderByKunci(String skema) {
		return "SELECT id, COALESCE(nomor_dokumen,'') FROM " + skema + "sales_order"
				+ " WHERE idempotency_key = ? LIMIT 1";
	}

	/** Keberadaan satu baris induk; dipakai menggantikan {@code session.get(...)} jalur legacy. */
	static String adaBaris(String skema, String tabel) {
		return "SELECT 1 FROM " + skema + tabel + " WHERE id = ?";
	}

	/**
	 * SEBELAS kolom; tujuh parameter: tanggal, customerId, salespersonId, tokoId, keterangan,
	 * idempotencyKey, oleh.
	 *
	 * <p>{@code nomor_dokumen} disisipkan kosong lalu ditimpa setelah id-nya diketahui, persis
	 * seperti jalur legacy yang menyimpan dulu baru memanggil {@code setNomor}. Aman karena
	 * indeks nomor pada {@code sales_order} tidak unik.</p>
	 *
	 * <p>{@code tanggal} tenant berstatus {@code NOT NULL} sedangkan legacy membolehkannya
	 * kosong; bila permintaan tidak menyertakannya, dipakai tanggal server hari ini.</p>
	 */
	static String sisipOrder(String skema) {
		return "INSERT INTO " + skema + "sales_order (nomor_dokumen, tanggal, customer_id,"
				+ " salesperson_id, toko_id, total, keterangan, idempotency_key, status,"
				+ " dibuat_pada, oleh)"
				+ " VALUES ('', ?, ?, ?, ?, 0, ?, ?, 'DRAF', now(), ?)";
	}

	/**
	 * SEPULUH parameter: orderId, barisKe, produkId, kuantitas, hargaSatuan, total,
	 * satuanJualId, qtyInput, faktorKeDasar, oleh.
	 *
	 * <p>Tiga yang terakhir sebelum {@code oleh} adalah <b>cuplikan</b> satuan jual dan boleh
	 * {@code NULL} (baris yang dikirim dalam satuan dasar). {@code kuantitas} tetap yang
	 * berwenang: ia sudah dalam satuan dasar, dihitung sekali, dan tidak pernah dihitung ulang
	 * dari {@code faktor_ke_dasar} — lihat {@link #catatanSatuanJual()}.</p>
	 */
	static String sisipOrderBaris(String skema) {
		return "INSERT INTO " + skema + "sales_order_detail (sales_order_id, baris_ke, produk_id,"
				+ " kuantitas, harga_satuan, total, satuan_jual_id, qty_input, faktor_ke_dasar,"
				+ " dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?)";
	}

	// ------------------------------------------------------------------ satuan jual (v19)

	/** Kategori UOM bawaan untuk satuan yang belum dikategorikan, sama dengan jalur legacy. */
	static final String KATEGORI_UOM_BAWAAN = "UNIT";

	/** Arah konversi "satuan ini LEBIH KECIL dari acuan", sama dengan sandi legacy. */
	static final String KONVERSI_LEBIH_KECIL = "SMALLER";

	/**
	 * TIGA kolom: kategori (ternormalkan), rasio, tipe_konversi — untuk satu satuan.
	 *
	 * <p>Kategori kosong menjadi {@code UNIT}, persis seperti jalur legacy memperlakukan katalog
	 * lama yang belum punya kolomnya. Rasio kosong menjadi 1: satuan yang belum pernah
	 * dikonversi berperilaku sebagai dirinya sendiri, bukan sebagai galat.</p>
	 */
	static String satuanKonversi(String skema) {
		return "SELECT UPPER(COALESCE(NULLIF(TRIM(s.kategori),''),'" + KATEGORI_UOM_BAWAAN + "')),"
				+ " COALESCE(s.rasio,1), UPPER(COALESCE(s.tipe_konversi,''))"
				+ " FROM " + skema + "satuan s WHERE s.id = ?";
	}

	/** SATU kolom: satuan dasar produk, atau {@code NULL} bila produk belum bersatuan. */
	static String satuanDasarProduk(String skema) {
		return "SELECT p.satuan_id FROM " + skema + "produk p WHERE p.id = ?";
	}

	/**
	 * <h4>Mengapa rasio dan arahnya disimpan terpisah, bukan sebagai satu faktor</h4>
	 *
	 * <p>Faktor pecahan tidak selalu dapat disimpan tepat. 1/12 pada {@code numeric(18,6)}
	 * menjadi {@code 0.083333}, dan {@code 12 × 0.083333 = 0.999996}: dua belas PCS berubah
	 * menjadi 0,999996 DUS. Selisih itu tidak pernah cukup besar untuk terlihat, dan tidak pernah
	 * hilang.</p>
	 * <p>Karena itu tiap satuan disimpan sebagai <b>rasio bulat berikut arahnya</b>, lalu
	 * konversinya dihitung sebagai satu pecahan:</p>
	 * <pre>
	 * faktor(jual —&gt; dasar) = (pembilangJual × penyebutDasar)
	 *                        / (penyebutJual × pembilangDasar)
	 * kuantitas               = qtyInput × pembilang / penyebut
	 * </pre>
	 * <p>Pembagiannya dilakukan <b>sekali</b>, atas pembilang yang sudah dikalikan — bukan atas
	 * faktor yang sudah dibulatkan lebih dulu. Pada kasus yang lazim (dasar PCS, jual DUS, rasio
	 * 12) penyebutnya 1 dan hasilnya bulat betulan.</p>
	 * <p>{@code faktor_ke_dasar} pada barisnya adalah <b>catatan</b>, bukan masukan hitungan
	 * ulang. Kuantitas dasarnya sudah dihitung sekali ke {@code kuantitas}, sehingga pembulatan
	 * pada kolom cuplikan tidak pernah bisa merusak angka yang mengikat.</p>
	 *
	 * <h4>Konversi antar-kategori DITOLAK</h4>
	 * <p>Kilogram tidak boleh menjadi liter. Penolakan itu satu-satunya hal yang mencegah rasio
	 * asal-asalan menghasilkan kuantitas yang tampak wajar, dan jalur legacy pun menolaknya.</p>
	 */
	static String catatanSatuanJual() {
		return "rasio + arah, bukan satu faktor desimal";
	}

	/**
	 * Satu satuan sebagai pecahan menuju acuan kategorinya: {@code [pembilang, penyebut]}.
	 *
	 * <p>{@code SMALLER} berarti satuan ini lebih kecil dari acuannya, sehingga pecahannya
	 * {@code 1/rasio}; selain itu {@code rasio/1}. Disimpan sebagai pecahan, bukan desimal, supaya
	 * rasio kebalikan tetap tepat.</p>
	 */
	static java.math.BigDecimal[] pecahanSatuan(java.math.BigDecimal rasio, String tipeKonversi) {
		if (rasio == null || rasio.signum() <= 0) {
			return null;
		}
		boolean lebihKecil = KONVERSI_LEBIH_KECIL.equals(tipeKonversi);
		return new java.math.BigDecimal[] {
				lebihKecil ? java.math.BigDecimal.ONE : rasio,
				lebihKecil ? rasio : java.math.BigDecimal.ONE };
	}

	/**
	 * Kuantitas dasar dari {@code qtyInput} pada satuan jual, berikut faktor cuplikannya:
	 * {@code [kuantitas, faktorKeDasar]}. {@code null} bila konversinya tidak sah.
	 *
	 * <pre>
	 * faktor(jual —&gt; dasar) = (pembilangJual × penyebutDasar)
	 *                        / (penyebutJual × pembilangDasar)
	 * </pre>
	 *
	 * <p>Pembagiannya dilakukan <b>sekali</b>, atas pembilang yang sudah dikalikan
	 * {@code qtyInput} — bukan atas faktor yang dibulatkan lebih dulu. Itulah yang menjaga
	 * {@code 12 PCS} tetap menjadi tepat {@code 12}, bukan {@code 11.999952}.</p>
	 * <p>Skala 4 mengikuti {@code sales_order_detail.kuantitas numeric(18,4)}; skala 6 pada
	 * faktornya mengikuti kolom cuplikannya, yang memang hanya catatan.</p>
	 */
	static java.math.BigDecimal[] kuantitasDasar(java.math.BigDecimal[] jual,
			java.math.BigDecimal[] dasar, java.math.BigDecimal qtyInput) {
		if (jual == null || dasar == null || qtyInput == null) {
			return null;
		}
		java.math.BigDecimal pembilang = jual[0].multiply(dasar[1]);
		java.math.BigDecimal penyebut = jual[1].multiply(dasar[0]);
		if (penyebut.signum() <= 0) {
			return null;
		}
		java.math.BigDecimal kuantitas = qtyInput.multiply(pembilang)
				.divide(penyebut, 4, java.math.BigDecimal.ROUND_HALF_UP);
		java.math.BigDecimal faktor = pembilang.divide(penyebut, 6,
				java.math.BigDecimal.ROUND_HALF_UP);
		return new java.math.BigDecimal[] { kuantitas, faktor };
	}

	/** Menimpa nomor dan total setelah seluruh barisnya tersisip. */
	static String finalisasiOrder(String skema) {
		return "UPDATE " + skema + "sales_order SET nomor_dokumen = ?, total = ?,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/** EMPAT kolom: status (ternormalkan), salespersonId, tokoId, nomor. */
	static String orderUntukUbah(String skema) {
		return "SELECT " + statusOrder("o") + ", o.salesperson_id, o.toko_id,"
				+ " COALESCE(o.nomor_dokumen,'') FROM " + skema + "sales_order o WHERE o.id = ?";
	}

	static String hapusOrderBaris(String skema) {
		return "DELETE FROM " + skema + "sales_order_detail WHERE sales_order_id = ?";
	}

	/** LIMA parameter: tanggal (boleh NULL), keterangan, total, oleh, id. */
	static String perbaruiOrder(String skema) {
		return "UPDATE " + skema + "sales_order SET tanggal = COALESCE(?, tanggal),"
				+ " keterangan = ?, total = ?, oleh = ?, tanggal_dirubah = now() WHERE id = ?";
	}

	// ------------------------------------------------------------------ terbitkan faktur

	/**
	 * <h4>Satu dokumen legacy menjadi DUA dokumen tenant</h4>
	 *
	 * <p>Legacy menerbitkan satu {@code PiutangCustomerDoc} yang sekaligus berperan sebagai
	 * faktur dan sebagai piutang. Model tenant memisahkannya: {@code faktur_penjualan} adalah
	 * dokumen penjualannya (memegang {@code toko_id}, {@code sales_order_id}, nomor, total),
	 * dan {@code piutang_customer} adalah tagihannya (memegang jatuh tempo dan nilai).</p>
	 *
	 * <p>Keduanya WAJIB lahir dalam satu transaksi. Faktur tanpa piutang berarti barang terjual
	 * yang tidak pernah ditagih; piutang tanpa faktur memutus seluruh penelusuran ke ordernya —
	 * termasuk saringan lingkup toko, yang pada model tenant memang ditempuh lewat faktur.</p>
	 *
	 * <p><b>{@code dibayar_awal} kini DILAYANI (§18).</b> Legacy menyimpannya sebagai kolom
	 * pada dokumen piutang. Pada model tenant uang muka bukan kolom melainkan <b>dokumen
	 * penerimaan berikut alokasinya</b>, dan sejak celah C-11 ditutup bundel v12 tidak ada lagi
	 * alasan menundanya. Dokumennya lahir dalam transaksi yang sama dengan fakturnya — lihat
	 * {@link #catatanUangMuka()}.</p>
	 * <p>{@code sales_trip_id}-nya {@code NULL}, dan itu benar: {@code sales_order} model tenant
	 * tidak punya kaitan trip, dan jalur legacy pun tidak mencatat pergerakan kas apa pun untuk
	 * {@code dibayar_awal}. Uang muka yang diterima sales <b>di lapangan</b> punya jalurnya
	 * sendiri lewat penagihan, yang memang mengaitkannya ke trip dan menulis buku kasnya.</p>
	 */
	static String orderUntukFaktur(String skema) {
		return "SELECT " + statusOrder("o") + ", o.salesperson_id, o.toko_id, o.customer_id,"
				+ " COALESCE(o.total,0), COALESCE(o.nomor_dokumen,'')"
				+ " FROM " + skema + "sales_order o WHERE o.id = ?";
	}

	/** DUA kolom: piutangId dan nomornya; dipakai penjaga idempotensi pemfakturan. */
	static String cariPiutangOrder(String skema) {
		return "SELECT d.id, COALESCE(d.nomor_faktur,'')"
				+ " FROM " + skema + "piutang_customer d"
				+ " JOIN " + skema + "faktur_penjualan f ON d.faktur_penjualan_id = f.id"
				+ " WHERE f.sales_order_id = ? ORDER BY d.id LIMIT 1";
	}

	/**
	 * Termin pembayaran customer.
	 *
	 * <p>Legacy menyaring profil yang {@code aktif}; profil tenant tidak punya kolom itu dan
	 * dibatasi {@code UNIQUE (customer_id)} — satu customer tepat satu profil, sehingga tidak
	 * ada yang perlu disaring.</p>
	 */
	static String terminCustomer(String skema) {
		return "SELECT COALESCE(syarat_bayar_hari,0) FROM " + skema + "customer_profile"
				+ " WHERE customer_id = ?";
	}

	/**
	 * TIGA BELAS parameter: nomorSementara (dua kali), tanggal, jatuhTempo, customerId,
	 * salespersonId, salesOrderId, tokoId, subtotal, total, keterangan, idempotencyKey, oleh.
	 *
	 * <p>Nomornya disisipkan dengan nilai <b>sementara yang sudah unik per order</b>, bukan
	 * kosong: {@code faktur_penjualan} dibatasi {@code UNIQUE (customer_id, nomor_faktur,
	 * tanggal)}, sehingga dua faktur berkode kosong untuk customer yang sama pada hari yang sama
	 * akan bertabrakan.</p>
	 */
	static String sisipFaktur(String skema) {
		return "INSERT INTO " + skema + "faktur_penjualan (nomor_dokumen, nomor_faktur, tanggal,"
				+ " jatuh_tempo, customer_id, salesperson_id, sales_order_id, toko_id, subtotal,"
				+ " total, keterangan, idempotency_key, status, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'AKTIF', now(), ?)";
	}

	static String finalisasiFaktur(String skema) {
		return "UPDATE " + skema + "faktur_penjualan SET nomor_dokumen = ?, nomor_faktur = ?,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/**
	 * SEMBILAN parameter: customerId, salespersonId, fakturId, nomor, tanggal, jatuhTempo,
	 * nilai, sisa, oleh.
	 *
	 * <p>{@code terbayar} dan {@code sisa} diisi konsisten sejak awal walaupun seluruh pembacaan
	 * tenant menghitung ulang sisa dari alokasinya. Membiarkannya nol pada dokumen yang bernilai
	 * membuat kolom ringkasan berbohong sejak baris pertama.</p>
	 */
	static String sisipPiutang(String skema) {
		return "INSERT INTO " + skema + "piutang_customer (customer_id, salesperson_id,"
				+ " faktur_penjualan_id, nomor_faktur, tanggal, jatuh_tempo, nilai, terbayar,"
				+ " sisa, status, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, 'TERBUKA', now(), ?)";
	}

	static String tandaiSiapTagih(String skema) {
		return "UPDATE " + skema + "sales_order SET status = 'SIAP_TAGIH', oleh = ?,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}
	// ------------------------------------------------------------------ catat penagihan (v12+v13)

	/**
	 * <h4>Penagihan piutang: aksi yang paling lama tertahan, dan sebabnya ada dua</h4>
	 *
	 * <p>Aksi ini menunggu <b>dua</b> bundel sekaligus. Sisi kasnya menunggu v12 —
	 * penagihan tunai di lapangan menaikkan uang yang dipegang sales, dan tanpa buku kas tidak
	 * ada tempat mencatatnya. Sisi notanya menunggu v13 — penagihan memutakhirkan status nota
	 * bawaan yang ditugaskan pada SPJ. Memindahkan salah satu saja berarti mencatat uang masuk
	 * yang tidak terlihat di kas, atau nota yang tidak pernah berubah status.</p>
	 *
	 * <h4>Satu langkah legacy yang HILANG di sini, dan itu benar</h4>
	 * <p>Jalur legacy menaikkan {@code SpjSalesNota.nilaiTertagih} pada tiap alokasi. Model
	 * tenant tidak menyimpan angka itu — ia diturunkan dari alokasinya sendiri (bundel v13),
	 * sehingga menuliskannya justru akan menciptakan sumber kedua. Yang tersisa untuk
	 * dimutakhirkan hanyalah <b>statusnya</b>, dan itu memang bukan turunan: PAID/PARTIAL
	 * ditentukan sisa tagihan, tetapi PROMISE_TO_PAY atau DISPUTED datang dari kunjungan.</p>
	 *
	 * <h4>Kosakata status piutang</h4>
	 * <p>Legacy mengunci faktur dengan syarat {@code status = 'AKTIF'}. Katalog tenant memakai
	 * {@code 'TERBUKA'} sebagai bawaan. Alih-alih memilih salah satu, penjaganya dibalik menjadi
	 * <b>bukan</b> dokumen yang dibatalkan — itu yang sebenarnya dimaksud, dan tahan terhadap
	 * kosakata mana pun yang dipakai baris hasil impor.</p>
	 */
	static String cariPenerimaanByKunci(String skema) {
		return "SELECT id, COALESCE(nomor_dokumen,'') FROM " + skema + "penerimaan_piutang"
				+ " WHERE idempotency_key = ? LIMIT 1";
	}

	/**
	 * Mengunci satu dokumen piutang milik customer tersebut. DUA parameter: piutangId,
	 * customerId.
	 *
	 * <p>{@code FOR UPDATE} disalin dari jalur legacy dan bukan hiasan: tanpa kunci baris, dua
	 * penagihan bersamaan atas faktur yang sama sama-sama membaca sisa yang masih penuh, lalu
	 * sama-sama lolos — dan faktur tertagih melebihi nilainya.</p>
	 */
	static String kunciPiutang(String skema) {
		return "SELECT d.id FROM " + skema + "piutang_customer d"
				+ " WHERE d.id = ? AND d.customer_id = ?"
				+ " AND COALESCE(d.status,'TERBUKA') NOT IN ('BATAL','DIBATALKAN')"
				+ " FOR UPDATE";
	}

	/** Sisa satu dokumen piutang; dihitung dari alokasi, bukan dibaca kolom ringkasan. */
	static String sisaSatuPiutang(String skema) {
		return "SELECT COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai)"
				+ " FROM " + skema + "alokasi_penerimaan_piutang a"
				+ " WHERE a.piutang_customer_id = d.id),0)"
				+ " FROM " + skema + "piutang_customer d WHERE d.id = ?";
	}

	/**
	 * SEMBILAN parameter: nomorSementara, customerId, salespersonId, caraBayar, nomorBg,
	 * namaBank, tanggalBg, nilai, keterangan — lalu idempotencyKey, salesTripId, oleh.
	 *
	 * <p>Nomor sementara memakai kunci idempotensinya, yang sudah unik per permintaan; nomor
	 * final memuat id yang baru lahir sesudah INSERT, sama pola dengan jalur legacy.</p>
	 */
	static String sisipPenerimaan(String skema) {
		return "INSERT INTO " + skema + "penerimaan_piutang (nomor_dokumen, tanggal, customer_id,"
				+ " salesperson_id, cara_bayar, nomor_bg, nama_bank, tanggal_bg, nilai,"
				+ " keterangan, idempotency_key, sales_trip_id, status, dibuat_pada, oleh)"
				+ " VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'AKTIF', now(), ?)";
	}

	static String finalisasiNomorPenerimaan(String skema) {
		return "UPDATE " + skema + "penerimaan_piutang SET nomor_dokumen = ?,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/** EMPAT parameter: penerimaanId, piutangId, nilai, oleh. */
	static String sisipAlokasiPenerimaan(String skema) {
		return "INSERT INTO " + skema + "alokasi_penerimaan_piutang (penerimaan_piutang_id,"
				+ " piutang_customer_id, nilai, dibuat_pada, oleh) VALUES (?, ?, ?, now(), ?)";
	}

	// ------------------------------------------------------------------ uang muka faktur

	/**
	 * Cara bayar yang diterima untuk uang muka saat faktur diterbitkan.
	 *
	 * <p>{@code DISCOUNT} dan {@code RETUR} sengaja TIDAK termasuk, walau keduanya sah pada
	 * penagihan biasa. Keduanya bukan uang yang masuk melainkan pengurang nilai tagihan, dan
	 * memberikannya sebagai "uang muka" pada saat faktur lahir berarti menerbitkan faktur yang
	 * sejak detik pertama sudah dipotong tanpa dokumen retur maupun persetujuan diskon yang
	 * menyertainya. Potongan semacam itu punya jalurnya sendiri lewat penagihan.</p>
	 */
	private static final String[] METODE_UANG_MUKA = {
			ais.database.model.koperasi.PenerimaanPiutangCustomer.METODE_TUNAI,
			ais.database.model.koperasi.PenerimaanPiutangCustomer.METODE_TRANSFER,
			ais.database.model.koperasi.PenerimaanPiutangCustomer.METODE_GIRO };

	/** Benar bila {@code metode} boleh dipakai sebagai cara bayar uang muka. */
	static boolean metodeUangMukaSah(String metode) {
		for (int i = 0; i < METODE_UANG_MUKA.length; i++) {
			if (METODE_UANG_MUKA[i].equals(metode)) {
				return true;
			}
		}
		return false;
	}

	/** Daftar cara bayar uang muka untuk pesan penolakan. */
	static String daftarMetodeUangMuka() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < METODE_UANG_MUKA.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(METODE_UANG_MUKA[i]);
		}
		return sb.toString();
	}

	/**
	 * <h4>Uang muka faktur adalah DOKUMEN, bukan kolom pengurang</h4>
	 *
	 * <p>Jalur legacy menyimpan {@code dibayar_awal} sebagai kolom pada dokumen piutang, dan
	 * menghitung sisanya {@code total_faktur − dibayar_awal − Σalokasi}. Ada
	 * <b>dua</b> pengurang di sana, dan hanya satu di antaranya yang berasal dari dokumen: uang
	 * mukanya mengurangi tagihan tanpa ada kwitansi yang bisa ditunjuk, tanpa cara bayar, dan
	 * tanpa pasangan di sisi kas.</p>
	 * <p>Model tenant menghitung sisa {@code nilai − Σalokasi} — satu sumber. Karena
	 * itu uang muka di sini diterbitkan sebagai {@code penerimaan_piutang} sungguhan berikut
	 * barisnya di {@code alokasi_penerimaan_piutang}, di dalam <b>transaksi yang sama</b> dengan
	 * fakturnya. Fakturnya tetap bernilai penuh, dan yang berkurang adalah sisanya —
	 * sebagaimana mestinya.</p>
	 *
	 * <h4>Yang berubah pada keluarannya</h4>
	 * <p>Pada daftar piutang, {@code dibayarAwal} jalur tenant tetap nol dan uang mukanya muncul
	 * pada {@code teralokasi}. {@code outstanding} sama persis dengan legacy, sebab legacy
	 * menjumlahkan keduanya. Klien yang menampilkan "uang muka" sebagai kolom tersendiri akan
	 * melihatnya nol; yang menampilkan sisa tagihan melihat angka yang sama.</p>
	 *
	 * <h4>Idempotensi</h4>
	 * <p>Kunci uang mukanya {@code SO-DP-<orderId>}, sejajar dengan {@code SO-INV-<orderId>}
	 * milik fakturnya. Karena keduanya lahir dalam satu transaksi, pengulangan permintaan yang
	 * sama tertolak pada indeks unik faktur lebih dulu dan tidak pernah sampai menerbitkan
	 * kwitansi kedua.</p>
	 */
	static String catatanUangMuka() {
		return "uang muka = penerimaan_piutang + alokasi, bukan kolom";
	}

	/** DUA kolom: status trip dan SPJ-nya, untuk penjaga sesi dan penelusuran nota bawaan. */
	static String tripUntukPenerimaan(String skema) {
		return "SELECT COALESCE(t.status,''), t.surat_perintah_sales_id"
				+ " FROM " + skema + "sales_trip t WHERE t.id = ?";
	}

	/**
	 * Memutakhirkan status satu nota bawaan. TIGA parameter: status, spjId, piutangId.
	 *
	 * <p>Hanya statusnya. Nilai tertagihnya diturunkan, sehingga tidak ada yang perlu ditambah
	 * maupun dikurangi di sini — lihat bundel v13.</p>
	 */
	static String ubahStatusNotaBawaan(String skema) {
		return "UPDATE " + skema + "surat_perintah_sales_nota SET status = ?,"
				+ " tanggal_dirubah = now()"
				+ " WHERE surat_perintah_sales_id = ? AND piutang_customer_id = ?";
	}

	/**
	 * Sales order asal satu dokumen piutang, lewat fakturnya.
	 *
	 * <p>Legacy menyimpan {@code PiutangCustomerDoc.salesOrder} langsung; model tenant
	 * menempuhnya lewat {@code faktur_penjualan}. Dikembalikan {@code NULL} bila piutangnya
	 * memang tidak berasal dari order.</p>
	 */
	static String orderDariPiutang(String skema) {
		return "SELECT f.sales_order_id FROM " + skema + "piutang_customer d"
				+ " JOIN " + skema + "faktur_penjualan f ON d.faktur_penjualan_id = f.id"
				+ " WHERE d.id = ?";
	}

	/** DUA parameter: oleh, orderId. */
	static String ubahStatusOrderLunas(String skema) {
		return "UPDATE " + skema + "sales_order SET status = 'LUNAS', oleh = ?,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/** DUA parameter: oleh, orderId. Dipakai pembalikan: LUNAS mundur ke SIAP_TAGIH. */
	static String ubahStatusOrderSiapTagih(String skema) {
		return "UPDATE " + skema + "sales_order SET status = 'SIAP_TAGIH', oleh = ?,"
				+ " tanggal_dirubah = now() WHERE id = ? AND status = 'LUNAS'";
	}
}
