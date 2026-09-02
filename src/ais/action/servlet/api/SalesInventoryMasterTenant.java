package ais.action.servlet.api;

/**
 * <h3>Jalur schema tenant untuk Master Supplier/Customer/Sales (P4, helper kelima).</h3>
 *
 * <p>Jalur legacy di {@link SalesInventoryMasterHelper} tidak diubah; kelas ini menyediakan
 * potongan SQL penggantinya dengan kolom keluaran yang sama dan berurutan sama.</p>
 *
 * <h4>Helper dengan pemetaan tingkat KOLOM terbanyak</h4>
 * <p>Empat helper sebelumnya sebagian besar berganti nama tabel. Di sini yang berpindah adalah
 * <b>letak medannya</b>: legacy menaruh identitas dan profil pada satu-dua tabel, sedangkan
 * model tenant memecahnya menjadi induk + profil + rekening bank + penugasan.</p>
 *
 * <table border="1">
 * <tr><th>Legacy</th><th>Tenant</th></tr>
 * <tr><td>{@code library.penyedia}</td><td>{@code supplier} + {@code supplier_profile}</td></tr>
 * <tr><td>{@code supplier_inventory_profile} (bank)</td><td>{@code supplier_bank_account}</td></tr>
 * <tr><td>{@code anggota_koperasi} + {@code customer_inventory_profile}</td>
 *     <td>{@code customer} + {@code customer_profile}</td></tr>
 * <tr><td>{@code sales_inventory}</td><td>{@code salesperson} + {@code sales_assignment}</td></tr>
 * <tr><td>{@code cp.sales_owner}</td><td>{@code customer.salesperson_id}</td></tr>
 * <tr><td>{@code a.limit_kredit}</td><td>{@code customer_profile.plafon_piutang}</td></tr>
 * <tr><td>{@code termin_hari}</td><td>{@code syarat_bayar_hari}</td></tr>
 * <tr><td>{@code s.nomor_perkiraan}</td><td>{@code salesperson.akun_perkiraan}</td></tr>
 * <tr><td>{@code s.area}</td><td>{@code sales_assignment.wilayah}</td></tr>
 * </table>
 *
 * <h4>Medan yang tidak punya padanan</h4>
 * <p>Tiga medan pemasok hilang di model tenant: {@code akun_utang}, {@code wilayah}, dan
 * {@code keterangan}. Kolomnya tetap dikembalikan agar bentuk JSON tidak berubah, tetapi
 * selalu kosong.</p>
 *
 * <p><b>{@code wilayah} pemasok/pelanggan sengaja TIDAK dipetakan ke {@code kota}.</b> Keduanya
 * memang berdekatan, tetapi wilayah adalah pembagian penjualan sedangkan kota adalah bagian
 * alamat. Memetakannya akan membuat saringan "wilayah = Jawa Barat" mencari kota bernama
 * "Jawa Barat" dan mengembalikan nol baris -- saringan yang tampak bekerja padahal tidak
 * pernah cocok. Lebih baik kosong dan terlihat kosong.</p>
 *
 * <p>Wilayah sales berbeda: model tenant memang menyimpannya, pada
 * {@code sales_assignment.wilayah}, sehingga yang itu dipetakan sungguhan.</p>
 *
 * <h4>Seluruh jalur tulis memakai SQL asli</h4>
 * <p>Tujuh dari sebelas aksi menulis atau membaca lewat entitas Hibernate, dan entitasnya
 * mematok {@code @Table(schema = ...)}. Sama seperti helper ketiga dan keempat, jalur tenant
 * tidak memakai entitas sama sekali.</p>
 */
final class SalesInventoryMasterTenant {

	private SalesInventoryMasterTenant() {
	}

	/** Benar bila aktor ini dilayani schema tenant. */
	static boolean aktif(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.aktif(aktor);
	}

	/** Prefiks schema berikut titiknya. */
	static String skema(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.skema(aktor.tenant);
	}

	// ------------------------------------------------------------------ supplier

	/**
	 * Rekening bank utama pemasok. Legacy menaruhnya sebagai kolom pada profil; model tenant
	 * memakai tabel tersendiri yang boleh berisi lebih dari satu rekening, sehingga yang
	 * bertanda {@code utama} yang diambil.
	 */
	private static String bankUtama(String skema, String kolom) {
		return "(SELECT b." + kolom + " FROM " + skema + "supplier_bank_account b"
				+ " WHERE b.supplier_id = p.id AND COALESCE(b.aktif,true) = true"
				+ " ORDER BY COALESCE(b.utama,false) DESC, b.id ASC LIMIT 1)";
	}

	static String dasarSupplier(String skema, String where) {
		return " FROM " + skema + "supplier p LEFT JOIN " + skema + "supplier_profile sp"
				+ " ON sp.supplier_id = p.id " + where;
	}

	/**
	 * Kolom daftar pemasok, berurutan sama dengan legacy: id, kode, nama, alamat, telp, kontak,
	 * email, keterangan, akunUtang, profilId, terminHari, wilayah, noRekening, atasNama, bank,
	 * alamatBank, aktif, akunUtangId.
	 */
	static String selectSupplier(String skema) {
		// Urutan WAJIB sama dengan jalur legacy: id, kode, nama, alamat, telp, kontak, email,
		// keterangan, profilId, terminHari, wilayah, noRekening, atasNama, bank, alamatBank,
		// aktif, akunUtangId, akunUtangLabel. Kolom tanpa padanan dikosongkan di tempatnya,
		// bukan dihilangkan -- menggeser satu kolom akan menaruh nomor rekening di kolom nama.
		return "SELECT p.id, p.kode, p.nama, COALESCE(sp.alamat1,''), COALESCE(sp.telp,''), "
				+ "COALESCE(sp.kontak,''), COALESCE(sp.email,''), '', "
				+ "sp.id, COALESCE(sp.syarat_bayar_hari,0), '', "
				+ "COALESCE(" + bankUtama(skema, "nomor_rekening") + ",''), "
				+ "COALESCE(" + bankUtama(skema, "atas_nama") + ",''), "
				+ "COALESCE(" + bankUtama(skema, "nama_bank") + ",''), '', "
				+ "COALESCE(p.aktif,true), NULL, ''";
	}

	/** Urutan daftar dengan nama kolom tenant. */
	static String urutSupplier(String sort) {
		if ("nama".equals(sort)) {
			return "p.nama ASC";
		}
		if ("wilayah".equals(sort)) {
			// Wilayah tidak ada pada model tenant; mengurutkan berdasarkan kolom kosong hanya
			// akan mengacak. Jatuh ke urutan nama supaya hasilnya tetap bermakna.
			return "p.nama ASC";
		}
		return "p.kode ASC";
	}

	/** Benar bila pengurutan/penyaringan wilayah punya arti pada model tenant. */
	static boolean dukungWilayahMitra() {
		return false;
	}

	/** Saringan kata kunci pemasok, tanpa wilayah yang tidak ada padanannya. */
	static String kunciSupplier() {
		return " AND (p.kode ILIKE ? OR p.nama ILIKE ? OR COALESCE(sp.alamat1,'') ILIKE ?) ";
	}

	static String aktifSupplier(boolean aktif) {
		return " AND COALESCE(p.aktif, true) = " + (aktif ? "true" : "false") + " ";
	}

	/** Ringkasan hutang pemasok untuk layar rinci. */
	static String sqlRingkasHutang(String skema) {
		return "SELECT COUNT(*), COALESCE(SUM(COALESCE(h.nilai,0)),0),"
				+ " COALESCE(SUM(COALESCE(h.nilai,0) - COALESCE((SELECT SUM(a.nilai) FROM " + skema
				+ "alokasi_pembayaran_hutang a WHERE a.hutang_supplier_id = h.id),0)),0)"
				+ " FROM " + skema + "hutang_supplier h WHERE h.supplier_id = ?";
	}

	// ------------------------------------------------------------------ customer

	static String dasarCustomer(String skema, String where) {
		return " FROM " + skema + "customer a LEFT JOIN " + skema + "customer_profile cp"
				+ " ON cp.customer_id = a.id LEFT JOIN " + skema + "salesperson s"
				+ " ON a.salesperson_id = s.id " + where;
	}

	/**
	 * Kolom daftar pelanggan, berurutan sama dengan legacy: id, kode, nama, alamat, telp, hp,
	 * limitKredit, profilId, terminHari, diskon, wilayah, salesId, salesNama, aktif.
	 */
	static String selectCustomer(String skema) {
		return "SELECT a.id, a.kode, a.nama, COALESCE(cp.alamat, COALESCE(cp.alamat1,'')), "
				+ "COALESCE(cp.telp,''), '', COALESCE(cp.plafon_piutang,0), cp.id, "
				+ "COALESCE(cp.syarat_bayar_hari,0), COALESCE(cp.diskon,0), '', "
				+ "COALESCE(a.salesperson_id,0), COALESCE(s.nama,''), COALESCE(a.aktif,true)";
	}

	static String kunciCustomer() {
		return " AND (a.kode ILIKE ? OR a.nama ILIKE ? OR COALESCE(cp.alamat,'') ILIKE ?) ";
	}

	static String aktifCustomer(boolean aktif) {
		return " AND COALESCE(a.aktif, true) = " + (aktif ? "true" : "false") + " ";
	}

	/** Ringkasan piutang pelanggan untuk layar rinci. */
	static String sqlRingkasPiutang(String skema) {
		return "SELECT COUNT(*), COALESCE(SUM(COALESCE(d.nilai,0)),0),"
				+ " COALESCE(SUM(COALESCE(d.nilai,0) - COALESCE((SELECT SUM(a.nilai) FROM " + skema
				+ "alokasi_penerimaan_piutang a WHERE a.piutang_customer_id = d.id),0)),0)"
				+ " FROM " + skema + "piutang_customer d WHERE d.customer_id = ?";
	}

	// ------------------------------------------------------------------ sales

	/**
	 * Penugasan sales yang sedang berlaku. Legacy menaruh toko dan area langsung pada
	 * {@code sales_inventory}; model tenant memisahkannya ke {@code sales_assignment} yang
	 * berjangka waktu, sehingga yang diambil adalah penugasan aktif terbaru.
	 */
	static String dasarSales(String skema, String where) {
		return " FROM " + skema + "salesperson s"
				+ " LEFT JOIN LATERAL (SELECT sa.toko_id, sa.wilayah FROM " + skema
				+ "sales_assignment sa WHERE sa.salesperson_id = s.id"
				+ " AND COALESCE(sa.aktif,true) = true"
				+ " ORDER BY sa.berlaku_dari DESC NULLS LAST, sa.id DESC LIMIT 1) sa ON true"
				+ " LEFT JOIN " + skema + "toko t ON sa.toko_id = t.id " + where;
	}

	/**
	 * Kolom daftar sales, <b>13 kolom</b> berurutan sama dengan legacy: id, kode, nama,
	 * nomorPerkiraan, area, telepon, targetBulanan, limitPenagihan, aktif, tokoId, tokoNama,
	 * userId, jumlahCustomer.
	 *
	 * <p>{@code target_bulanan} dan {@code limit_penagihan} tidak ada pada model tenant;
	 * dikembalikan {@code NULL} di tempatnya, bukan nol. Nol berarti "targetnya nol" dan akan
	 * membuat laporan pencapaian melaporkan 100% terhadap target kosong; {@code NULL} berarti
	 * "belum diatur" dan terbaca apa adanya oleh klien.</p>
	 *
	 * <p>{@code userId} legacy adalah userid Tbmuser berupa teks; model tenant menautkannya lewat
	 * {@code pengguna_tenant_id}, sehingga useridnya ditarik dari tabel itu.</p>
	 */
	static String selectSales(String skema) {
		return "SELECT s.id, s.kode, s.nama, COALESCE(s.akun_perkiraan,''), "
				+ "COALESCE(sa.wilayah,''), COALESCE(s.telp,''), NULL, NULL, "
				+ "COALESCE(s.aktif,true), COALESCE(sa.toko_id,0), COALESCE(t.nama,''), "
				+ "COALESCE((SELECT pt.userid FROM " + skema + "pengguna_tenant pt"
				+ " WHERE pt.id = s.pengguna_tenant_id),''), "
				+ "(SELECT COUNT(*) FROM " + skema + "customer c WHERE c.salesperson_id = s.id"
				+ " AND COALESCE(c.aktif,true) = true)";
	}

	static String kunciSales() {
		return " AND (s.kode ILIKE ? OR s.nama ILIKE ? OR COALESCE(sa.wilayah,'') ILIKE ?) ";
	}

	static String aktifSales(boolean aktif) {
		return " AND COALESCE(s.aktif, true) = " + (aktif ? "true" : "false") + " ";
	}

	/** Saringan toko memakai penugasan, bukan kolom pada sales. */
	static String kolomTokoSales() {
		return "sa.toko_id";
	}

	// ------------------------------------------------------------------ tulis (SQL asli)

	static String adaBaris(String skema, String tabel) {
		return "SELECT COUNT(*) FROM " + skema + tabel + " WHERE id = ?";
	}

	static String nonaktifkan(String skema, String tabel) {
		return "UPDATE " + skema + tabel + " SET aktif = ?, tanggal_dirubah = now(), oleh = ?"
				+ " WHERE id = ?";
	}

	static String kodeDipakai(String skema, String tabel) {
		return "SELECT COUNT(*) FROM " + skema + tabel + " WHERE kode = ? AND id <> COALESCE(?, -1)";
	}

	static String sisipMitra(String skema, String tabel) {
		return "INSERT INTO " + skema + tabel + " (kode, nama, aktif, dibuat_pada, oleh)"
				+ " VALUES (?, ?, true, now(), ?)";
	}

	/**
	 * Cuplikan satu baris master untuk jejak audit.
	 *
	 * <p>Kolomnya <b>disebut satu per satu</b>, bukan {@code SELECT *}. Dua sebabnya. Pertama,
	 * jejak audit tidak boleh memuat rahasia (§11.6), dan {@code SELECT *} akan menyeret kolom
	 * apa pun yang ditambahkan bundel berikutnya — termasuk yang tidak boleh ikut. Kedua,
	 * muatan audit yang isinya berubah-ubah mengikuti skema membuat riwayat lama dan baru tidak
	 * lagi dapat dibandingkan.</p>
	 *
	 * <p>Kolom jejak ({@code oleh}, {@code tanggal_dirubah}) sengaja tidak ikut: "siapa dan
	 * kapan" sudah ada pada {@code revinfo}, dan menyalinnya ke muatan hanya membuat setiap
	 * perubahan tampak berbeda pada kolom yang bukan isi datanya.</p>
	 *
	 * <p>{@code tabel} selalu literal dari kode pemanggil, sama seperti {@link #nonaktifkan} dan
	 * {@link #adaBaris} — tidak pernah berasal dari permintaan.</p>
	 */
	static String cuplikanAudit(String skema, String tabel) {
		String kolom;
		if ("salesperson".equals(tabel)) {
			kolom = "kode, nama, aktif, akun_perkiraan, telp";
		} else if ("supplier".equals(tabel)) {
			kolom = "kode, nama, aktif, status";
		} else {
			kolom = "kode, nama, aktif";
		}
		return "SELECT " + kolom + " FROM " + skema + tabel + " WHERE id = ?";
	}

	static String ubahMitra(String skema, String tabel) {
		return "UPDATE " + skema + tabel + " SET kode = ?, nama = ?, tanggal_dirubah = now(),"
				+ " oleh = ? WHERE id = ?";
	}

	/**
	 * Sales punya medan tambahan pada induknya sendiri ({@code akun_perkiraan}, {@code telp}),
	 * sehingga penyimpanannya tidak memakai {@link #sisipMitra}.
	 */
	static String sisipSales(String skema) {
		return "INSERT INTO " + skema + "salesperson (kode, nama, akun_perkiraan, telp, aktif,"
				+ " dibuat_pada, oleh) VALUES (?, ?, ?, ?, true, now(), ?)";
	}

	static String ubahSales(String skema) {
		return "UPDATE " + skema + "salesperson SET kode = ?, nama = ?, akun_perkiraan = ?,"
				+ " telp = ?, tanggal_dirubah = now(), oleh = ? WHERE id = ?";
	}

	/** Profil pemasok/pelanggan disimpan terpisah dari induknya (upsert manual). */
	static String adaProfil(String skema, String tabel, String kolomInduk) {
		return "SELECT id FROM " + skema + tabel + " WHERE " + kolomInduk + " = ? LIMIT 1";
	}

	static String sisipProfilSupplier(String skema) {
		return "INSERT INTO " + skema + "supplier_profile (supplier_id, alamat1, telp, kontak,"
				+ " email, syarat_bayar_hari, dibuat_pada, oleh) VALUES (?, ?, ?, ?, ?, ?, now(), ?)";
	}

	static String ubahProfilSupplier(String skema) {
		return "UPDATE " + skema + "supplier_profile SET alamat1 = ?, telp = ?, kontak = ?,"
				+ " email = ?, syarat_bayar_hari = ?, tanggal_dirubah = now(), oleh = ?"
				+ " WHERE supplier_id = ?";
	}

	static String sisipProfilCustomer(String skema) {
		return "INSERT INTO " + skema + "customer_profile (customer_id, alamat, telp,"
				+ " syarat_bayar_hari, diskon, plafon_piutang, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, ?, now(), ?)";
	}

	static String ubahProfilCustomer(String skema) {
		return "UPDATE " + skema + "customer_profile SET alamat = ?, telp = ?,"
				+ " syarat_bayar_hari = ?, diskon = ?, plafon_piutang = ?, tanggal_dirubah = now(),"
				+ " oleh = ? WHERE customer_id = ?";
	}

	/** Pemilik sales pada pelanggan berada di induknya, bukan di profil. */
	static String ubahSalesOwner(String skema) {
		return "UPDATE " + skema + "customer SET salesperson_id = ?, tanggal_dirubah = now(),"
				+ " oleh = ? WHERE id = ?";
	}

	// ------------------------------------------------------------------ layar rinci

	/**
	 * Rinci pemasok dalam satu baris. Kolom berurutan: id, kode, nama, alamat, kodePos, telp,
	 * fax, kontak, email, keterangan, profilId, terminHari, wilayah, noRekening, atasNama, bank,
	 * alamatBank, aktif, saldoHutang.
	 *
	 * <p>{@code kodePos} dan {@code fax} ada pada {@code supplier_profile}; {@code keterangan},
	 * {@code wilayah}, dan {@code alamatBank} tidak punya padanan dan dikosongkan.</p>
	 *
	 * <p>Saldo hutang memakai rumus yang <b>sama persis</b> dengan
	 * {@code SalesInventoryPayableTenant.outstanding} -- dihitung dari alokasi, bukan dibaca dari
	 * kolom ringkasan. Dua layar yang menghitung hutang dengan cara berbeda adalah cara pasti
	 * melahirkan dua angka.</p>
	 */
	static String sqlDetailSupplier(String skema) {
		return "SELECT p.id, p.kode, p.nama, COALESCE(sp.alamat1,''), COALESCE(sp.kode_pos,''), "
				+ "COALESCE(sp.telp,''), COALESCE(sp.fax,''), COALESCE(sp.kontak,''), "
				+ "COALESCE(sp.email,''), '', sp.id, COALESCE(sp.syarat_bayar_hari,0), '', "
				+ "COALESCE(" + bankUtama(skema, "nomor_rekening") + ",''), "
				+ "COALESCE(" + bankUtama(skema, "atas_nama") + ",''), "
				+ "COALESCE(" + bankUtama(skema, "nama_bank") + ",''), '', "
				+ "COALESCE(p.aktif,true), "
				+ "COALESCE((SELECT SUM(COALESCE(h.nilai,0) - COALESCE((SELECT SUM(a.nilai) FROM "
				+ skema + "alokasi_pembayaran_hutang a WHERE a.hutang_supplier_id = h.id),0))"
				+ " FROM " + skema + "hutang_supplier h WHERE h.supplier_id = p.id),0)"
				+ " FROM " + skema + "supplier p LEFT JOIN " + skema + "supplier_profile sp"
				+ " ON sp.supplier_id = p.id WHERE p.id = ?";
	}

	/**
	 * Rinci pelanggan dalam satu baris. Kolom berurutan: id, kode, nama, alamat, telp, hp, email,
	 * limitKredit, profilId, terminHari, diskon, wilayah, noRekening, atasNama, bank,
	 * salesOwnerId, salesOwnerNama, aktif, saldoPiutang.
	 *
	 * <p>{@code hp} dan {@code email} pelanggan berada di {@code customer_anggota_profile},
	 * terpisah dari {@code customer_profile} yang menyimpan alamat dan syarat bayar. Rekening
	 * bank pelanggan tidak punya padanan sama sekali dan dikosongkan.</p>
	 */
	static String sqlDetailCustomer(String skema) {
		String anggota = "(SELECT ap.%s FROM " + skema + "customer_anggota_profile ap"
				+ " WHERE ap.customer_id = a.id ORDER BY ap.id ASC LIMIT 1)";
		return "SELECT a.id, a.kode, a.nama, COALESCE(cp.alamat, COALESCE(cp.alamat1,'')), "
				+ "COALESCE(cp.telp,''), COALESCE(" + String.format(anggota, "hp") + ",''), "
				+ "COALESCE(cp.email, COALESCE(" + String.format(anggota, "email") + ",'')), "
				+ "COALESCE(cp.plafon_piutang,0), cp.id, COALESCE(cp.syarat_bayar_hari,0), "
				+ "COALESCE(cp.diskon,0), '', '', '', '', "
				+ "COALESCE(a.salesperson_id,0), COALESCE(s.nama,''), COALESCE(a.aktif,true), "
				+ "COALESCE((SELECT SUM(COALESCE(d.nilai,0) - COALESCE((SELECT SUM(x.nilai) FROM "
				+ skema + "alokasi_penerimaan_piutang x WHERE x.piutang_customer_id = d.id),0))"
				+ " FROM " + skema + "piutang_customer d WHERE d.customer_id = a.id),0)"
				+ " FROM " + skema + "customer a LEFT JOIN " + skema + "customer_profile cp"
				+ " ON cp.customer_id = a.id LEFT JOIN " + skema + "salesperson s"
				+ " ON a.salesperson_id = s.id WHERE a.id = ?";
	}
}
