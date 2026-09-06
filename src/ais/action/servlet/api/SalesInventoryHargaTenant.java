package ais.action.servlet.api;

import ais.service.tenant.TenantMutasiStok;

/**
 * <h3>Jalur schema tenant untuk Harga Supplier/Customer &amp; Analisa Harga (P4, helper kedua).</h3>
 *
 * <p>Sama seperti helper pertama, jalur legacy di {@link SalesInventoryHargaHelper} tidak diubah;
 * kelas ini hanya menyediakan potongan SQL penggantinya, dengan kolom keluaran yang sama dan
 * berurutan sama sehingga perakitan JSON dipakai bersama.</p>
 *
 * <h4>Pemetaan kolom</h4>
 * <table border="1">
 * <tr><th>Legacy</th><th>Tenant</th></tr>
 * <tr><td>{@code h.supplier}</td><td>{@code h.supplier_id}</td></tr>
 * <tr><td>{@code h.anggota_koperasi}</td><td>{@code h.customer_id}</td></tr>
 * <tr><td>{@code h.produk}</td><td>{@code h.produk_id}</td></tr>
 * <tr><td>{@code h.tanggal_efektif}</td><td>{@code h.berlaku_dari}</td></tr>
 * <tr><td>{@code library.penyedia}</td><td>{@code <schema>.supplier}</td></tr>
 * <tr><td>{@code koperasi.anggota_koperasi}</td><td>{@code <schema>.customer}</td></tr>
 * <tr><td>{@code koperasi.satuan_produk}</td><td>{@code <schema>.satuan}</td></tr>
 * </table>
 *
 * <p>Perhatikan baris kelima: supplier legacy tidak berada di schema {@code koperasi} melainkan
 * {@code library}. Jalur tenant menariknya masuk ke schema tenant, sehingga daftar pemasok satu
 * tenant tidak lagi bercampur dengan tenant lain.</p>
 *
 * <h4>{@code keterangan} tidak ada di model tenant</h4>
 * <p>Kolom keluaran tetap dipertahankan supaya bentuk JSON tidak berubah, tetapi selalu berisi
 * string kosong. Menambahkannya ke DDL adalah perubahan katalog migrasi -- keputusan tersendiri,
 * bukan efek samping pemindahan kueri. Sampai itu diputuskan, mengosongkannya lebih jujur
 * daripada mengisinya dengan tebakan.</p>
 *
 * <h4>Satu tanggal versus rentang berlaku -- perbedaan yang DISENGAJA</h4>
 * <p>Legacy menyimpan satu {@code tanggal_efektif} dan memilih "versi terbaru yang tanggalnya
 * sudah lewat". Model tenant menyimpan <b>rentang</b> ({@code berlaku_dari}..{@code berlaku_sampai}),
 * sehingga harga yang sudah <b>kedaluwarsa</b> tidak lagi ikut terpilih.</p>
 * <p>Selama {@code berlaku_sampai} kosong keduanya sama persis. Begitu diisi, jalur tenant
 * benar dan jalur legacy tetap menyodorkan harga mati -- perbedaan yang memang dikehendaki,
 * bukan cacat pemetaan.</p>
 *
 * <h4>{@code p.stok} tidak ada di model tenant</h4>
 * <p>Legacy menyimpan stok sebagai kolom pada {@code produk}. Model tenant menurunkannya dari
 * {@code mutasi_stok}, dan jalur ini memakai rumus yang <b>sama persis</b> dengan
 * {@link SalesInventoryStokTenant} -- {@code SUM(arah * kuantitas)}. Memakai rumus berbeda
 * akan membuat layar Analisa Harga dan layar Persediaan menyebut angka stok yang berbeda untuk
 * produk yang sama.</p>
 */
final class SalesInventoryHargaTenant {

	private SalesInventoryHargaTenant() {
	}

	/** Benar bila aktor ini dilayani schema tenant. */
	static boolean aktif(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.aktif(aktor);
	}

	/** Prefiks schema berikut titiknya. */
	static String skema(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.skema(aktor.tenant);
	}

	// ------------------------------------------------------------------ daftar harga supplier

	/** {@code FROM ... JOIN ...} untuk daftar harga beli supplier. */
	static String dasarSupplier(String skema, String where) {
		return " FROM " + skema + "harga_beli_supplier h JOIN " + skema + "supplier s"
				+ " ON h.supplier_id = s.id JOIN " + skema + "produk p ON h.produk_id = p.id " + where;
	}

	/**
	 * Kolom daftar harga supplier, berurutan sama dengan jalur legacy:
	 * id, pihak_id, pihak_kode, pihak_nama, produk_id, produk_kode, produk_nama, harga,
	 * tanggal_efektif, keterangan, aktif, oleh.
	 */
	static String selectSupplier() {
		return "SELECT h.id, h.supplier_id, COALESCE(s.kode,''), COALESCE(s.nama,''), "
				+ "h.produk_id, p.kode, p.nama, h.harga, h.berlaku_dari, '', "
				+ "COALESCE(h.aktif,true), COALESCE(h.oleh,'')";
	}

	/** Urutan daftar: sama dengan legacy, dengan kolom tanggal yang setara. */
	static String urutSupplier() {
		return " ORDER BY p.kode ASC, h.berlaku_dari DESC, h.id DESC LIMIT ? OFFSET ?";
	}

	// ------------------------------------------------------------------ daftar harga customer

	static String dasarCustomer(String skema, String where) {
		return " FROM " + skema + "harga_jual_customer h LEFT JOIN " + skema + "customer a"
				+ " ON h.customer_id = a.id JOIN " + skema + "produk p ON h.produk_id = p.id " + where;
	}

	static String selectCustomer() {
		return "SELECT h.id, h.customer_id, COALESCE(a.kode,''), COALESCE(a.nama,'(Umum)'), "
				+ "h.produk_id, p.kode, p.nama, h.harga, h.berlaku_dari, '', "
				+ "COALESCE(h.aktif,true), COALESCE(h.oleh,'')";
	}

	static String urutCustomer() {
		return " ORDER BY p.kode ASC, h.berlaku_dari DESC, h.id DESC LIMIT ? OFFSET ?";
	}

	// ------------------------------------------------------------------ analisa harga

	/**
	 * Stok turunan dari buku besar -- rumus identik {@link SalesInventoryStokTenant}, supaya
	 * layar Analisa Harga dan layar Persediaan tidak pernah menyebut angka berbeda.
	 */
	static String stokTurunan(String skema) {
		return stokTurunan(skema, null);
	}

	/**
	 * Stok turunan, dibatasi gudang milik satu toko bila {@code tokoId} diberikan.
	 *
	 * <p>Lihat {@link #syaratTokoProduk(String, Long)} untuk alasan lingkup toko ditegakkan
	 * lewat gudang, dan mengapa {@code tokoId} disambung sebagai literal.</p>
	 */
	static String stokTurunan(String skema, Long tokoId) {
		String gudang = tokoId == null ? ""
				: " AND m.gudang_id IN (SELECT g.id FROM " + skema + "gudang g"
						+ " WHERE g.toko_id = " + tokoId.longValue() + ")";
		return "COALESCE((SELECT SUM(m.arah * m.kuantitas) FROM " + skema + "mutasi_stok m"
				+ " WHERE m.produk_id = p.id" + gudang + "),0)";
	}

	/**
	 * <h4>&sect;16 — lingkup toko pada model tenant adalah lingkup GUDANG</h4>
	 *
	 * <p>Jalur legacy menyaring {@code produk.toko}: di sana produk <b>milik</b> satu toko.
	 * Model tenant tidak begitu — produk berlaku se-tenant, dan yang menjadi milik satu toko
	 * adalah <b>gudangnya</b> ({@code gudang.toko_id}). Karena itu lingkup toko di sini
	 * ditegakkan lewat gudang, bukan lewat produk.</p>
	 *
	 * <p>Dua hal ditegakkan bersama, dan keduanya perlu:</p>
	 * <ul>
	 * <li><b>Daftar barisnya</b> dibatasi produk yang punya baris {@code saldo_stok} pada gudang
	 * toko itu — "produk yang ditangani toko ini". Dipakai {@code saldo_stok} dan bukan
	 * {@code mutasi_stok} justru supaya produk yang <b>bersaldo nol</b> tetap muncul; memakai
	 * mutasi akan menyembunyikan produk yang habis, padahal justru itu yang ingin dilihat.</li>
	 * <li><b>Angkanya</b> dihitung hanya dari mutasi pada gudang toko itu. Membatasi daftarnya
	 * saja tanpa membatasi angkanya akan menampilkan produk toko ini dengan stok
	 * se-tenant — angka yang lebih besar dari kenyataan di raknya.</li>
	 * </ul>
	 *
	 * <h4>Satu perbedaan hasil, dan itu disengaja</h4>
	 * <p>Produk yang <i>ditugaskan</i> ke suatu toko tetapi belum pernah distok di sana tidak
	 * muncul, sedangkan jalur legacy menampilkannya — sebab di sana penugasannya atribut produk,
	 * bukan akibat adanya stok. Model tenant tidak punya penugasan semacam itu; satu-satunya
	 * pernyataan bahwa toko menangani suatu produk adalah adanya stok produk itu di gudangnya.</p>
	 *
	 * <p>{@code tokoId} disambung sebagai literal, bukan parameter. Ia {@code Long} yang sudah
	 * tervalidasi pemanggil, dan ekspresi ini muncul di dalam {@code SELECT} — memakai
	 * {@code ?} di sana akan menyisipkan parameter <b>sebelum</b> parameter {@code where},
	 * mengacaukan urutan pengikatan yang sudah ada.</p>
	 */
	static String syaratTokoProduk(String skema, Long tokoId) {
		if (tokoId == null) {
			return "";
		}
		return " AND EXISTS (SELECT 1 FROM " + skema + "saldo_stok ss"
				+ " JOIN " + skema + "gudang g ON ss.gudang_id = g.id"
				+ " WHERE ss.produk_id = p.id AND g.toko_id = " + tokoId.longValue() + ") ";
	}

	/**
	 * <h4>Harga jual UMUM -- ASUMSI yang perlu ditegaskan pemilik keputusan</h4>
	 *
	 * <p>Legacy menyatakan harga umum sebagai baris {@code harga_jual_customer} dengan
	 * {@code anggota_koperasi IS NULL}. Model tenant <b>tidak dapat menyatakannya begitu</b>:
	 * {@code customer_id} di sana {@code NOT NULL} (lihat {@code TenantSchemaMigrationsV3}),
	 * sehingga baris harga tanpa customer mustahil ada.</p>
	 *
	 * <p>Satu-satunya representasi harga umum yang tersedia pada model tenant adalah
	 * {@code produk.harga_jual_standar}, dan itulah yang dipakai di sini.</p>
	 *
	 * <p><b>Bila yang dimaksud desain sebenarnya adalah {@code customer_id} boleh kosong,
	 * maka ini gap katalog, bukan pilihan pemetaan</b> -- perbaikannya sebuah migrasi v10
	 * yang melonggarkan kolom itu, bukan mengubah DDL yang sudah dirilis (katalognya
	 * append-only ber-checksum). Keputusan itu bukan efek samping pemindahan kueri, jadi
	 * ditandai di sini alih-alih diputuskan diam-diam.</p>
	 */
	static String hargaUmum(String skema) {
		return "COALESCE(p.harga_jual_standar, NULL)";
	}

	/**
	 * Benar bila model tenant sanggup menyimpan harga khusus-umum sebagai baris tersendiri.
	 * Selama {@code customer_id} masih {@code NOT NULL}, jawabannya tidak -- dan saringan
	 * "hanya umum" harus ditolak terang-terangan, bukan mengembalikan daftar kosong yang
	 * tampak seperti "memang belum ada datanya".
	 */
	static boolean dukungBarisHargaUmum() {
		return false;
	}

	/** Harga beli supplier terbaru yang masih berlaku hari ini. */
	static String hargaBeliTerbaru(String skema) {
		return "(SELECT h.harga FROM " + skema + "harga_beli_supplier h WHERE h.produk_id = p.id"
				+ " AND COALESCE(h.aktif,true) = true AND h.berlaku_dari <= CURRENT_DATE"
				+ " AND (h.berlaku_sampai IS NULL OR h.berlaku_sampai >= CURRENT_DATE)"
				+ " ORDER BY h.berlaku_dari DESC, h.id DESC LIMIT 1)";
	}

	static String dasarAnalisa(String skema, String where) {
		return " FROM " + skema + "produk p LEFT JOIN " + skema + "satuan sp ON p.satuan_id = sp.id "
				+ where;
	}

	/**
	 * Kolom analisa harga, berurutan sama dengan legacy: id, kode, nama, satuan, stok,
	 * harga_beli, harga_jual, harga_umum, harga_beli_supplier_terbaru.
	 */
	/**
	 * <p>Kolom ke-10 adalah harga jual TUNAI (legacy {@code HARGAJUAL2}, migrasi v20). Ia
	 * sengaja TIDAK dibungkus {@code COALESCE(...,0)} seperti tetangganya: NULL di sini berarti
	 * "produk ini tidak punya harga tunai terpisah", dan itu berbeda dari "harga tunainya nol".
	 * Menyamakan keduanya membuat margin tunai 459 produk menjadi -100%.</p>
	 */
	static String selectAnalisa(String skema) {
		return "SELECT p.id, p.kode, p.nama, COALESCE(NULLIF(TRIM(sp.nama),''),'(Belum diatur)'), "
				+ stokTurunan(skema) + ", COALESCE(p.harga_beli_terakhir,0), "
				+ "COALESCE(p.harga_jual_standar,0), " + hargaUmum(skema) + ", "
				+ hargaBeliTerbaru(skema) + ", p.harga_jual_tunai";
	}

	/** Saringan {@code stok_nol} pada model tenant: stok turunan, bukan kolom. */
	static String syaratStokNol(String skema) {
		return syaratStokNol(skema, null);
	}

	/** Saringan {@code stok_nol}, dibatasi gudang milik satu toko bila diberikan. */
	static String syaratStokNol(String skema, Long tokoId) {
		return " AND " + stokTurunan(skema, tokoId) + " <= 0 ";
	}

	/** Saringan {@code margin_negatif} dengan nama kolom tenant. */
	static String syaratMarginNegatif() {
		return " AND COALESCE(p.harga_jual_standar,0) < COALESCE(p.harga_beli_terakhir,0) ";
	}

	// ------------------------------------------------------------------ simpan (SQL asli)

	/**
	 * <h4>Mengapa penyimpanan memakai SQL asli, bukan entitas</h4>
	 * <p>Entitas Hibernate mematok schemanya di anotasi -- {@code @Table(schema = "koperasi")} --
	 * dan 1.551 entitas di deployment ini melakukannya. Pemetaan itu statis per SessionFactory,
	 * sehingga {@code session.save(new HargaBeliSupplier())} <b>selalu</b> menulis ke
	 * {@code koperasi}, berapa pun tenant yang sedang aktif. Menyimpan lewat entitas pada jalur
	 * tenant berarti menulis data tenant ke tabel bersama.</p>
	 * <p>Karena itu jalur tenant menulis lewat SQL asli. Aturan bisnisnya tidak berubah: versi
	 * tersimpan tetap tidak boleh diubah harganya, dan tanggal efektif ganda tetap ditolak.</p>
	 */
	static String cekDobelSupplier(String skema) {
		return "SELECT COUNT(*) FROM " + skema + "harga_beli_supplier"
				+ " WHERE supplier_id = ? AND produk_id = ? AND berlaku_dari = ?";
	}

	static String cekDobelCustomer(String skema, boolean anggotaNull) {
		return "SELECT COUNT(*) FROM " + skema + "harga_jual_customer"
				+ " WHERE produk_id = ? AND berlaku_dari = ? AND "
				+ (anggotaNull ? "customer_id IS NULL" : "customer_id = ?");
	}

	static String sisipSupplier(String skema) {
		return "INSERT INTO " + skema + "harga_beli_supplier"
				+ " (supplier_id, produk_id, harga, berlaku_dari, aktif, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, true, now(), ?)";
	}

	static String sisipCustomer(String skema) {
		return "INSERT INTO " + skema + "harga_jual_customer"
				+ " (customer_id, produk_id, harga, berlaku_dari, aktif, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, true, now(), ?)";
	}

	/**
	 * Pembaruan hanya menyentuh status aktif -- sama seperti jalur legacy, yang mengunci
	 * harga/tanggal/pihak/produk pada versi tersimpan. {@code keterangan} tidak ada di model
	 * tenant sehingga tidak ikut diperbarui.
	 */
	static String ubahAktifSupplier(String skema) {
		return "UPDATE " + skema + "harga_beli_supplier SET aktif = ?, tanggal_dirubah = now(),"
				+ " oleh = ? WHERE id = ?";
	}

	static String ubahAktifCustomer(String skema) {
		return "UPDATE " + skema + "harga_jual_customer SET aktif = ?, tanggal_dirubah = now(),"
				+ " oleh = ? WHERE id = ?";
	}

	/** Keberadaan satu versi harga, untuk memastikan id yang diubah memang milik tenant ini. */
	static String adaSupplier(String skema) {
		return "SELECT COUNT(*) FROM " + skema + "harga_beli_supplier WHERE id = ?";
	}

	static String adaCustomer(String skema) {
		return "SELECT COUNT(*) FROM " + skema + "harga_jual_customer WHERE id = ?";
	}

	/** Nama jenis mutasi yang dipakai {@link #stokTurunan}; dirujuk uji kesetaraan. */
	static String jenisOpname() {
		return TenantMutasiStok.OPNAME;
	}
}
