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
		return "COALESCE((SELECT SUM(m.arah * m.kuantitas) FROM " + skema + "mutasi_stok m"
				+ " WHERE m.produk_id = p.id),0)";
	}

	/** Harga jual UMUM yang berlaku hari ini (tanpa customer tertentu). */
	static String hargaUmum(String skema) {
		return "(SELECT h.harga FROM " + skema + "harga_jual_customer h WHERE h.produk_id = p.id"
				+ " AND h.customer_id IS NULL AND COALESCE(h.aktif,true) = true"
				+ " AND h.berlaku_dari <= CURRENT_DATE"
				+ " AND (h.berlaku_sampai IS NULL OR h.berlaku_sampai >= CURRENT_DATE)"
				+ " ORDER BY h.berlaku_dari DESC, h.id DESC LIMIT 1)";
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
	static String selectAnalisa(String skema) {
		return "SELECT p.id, p.kode, p.nama, COALESCE(NULLIF(TRIM(sp.nama),''),'(Belum diatur)'), "
				+ stokTurunan(skema) + ", COALESCE(p.harga_beli_terakhir,0), "
				+ "COALESCE(p.harga_jual_standar,0), " + hargaUmum(skema) + ", "
				+ hargaBeliTerbaru(skema);
	}

	/** Saringan {@code stok_nol} pada model tenant: stok turunan, bukan kolom. */
	static String syaratStokNol(String skema) {
		return " AND " + stokTurunan(skema) + " <= 0 ";
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
