package ais.action.servlet.api;

/**
 * <h3>Jalur schema tenant untuk Sales Lapangan / Trip (P4, helper keenam -- SEBAGIAN).</h3>
 *
 * <p><b>Helper ini baru dipindahkan sebagian, dan itu disengaja.</b> Dari sembilan belas aksi,
 * dua yang murni membaca dipindahkan di sini; tujuh belas sisanya <b>ditolak</b> pada jalur
 * tenant sampai ditulis. Alasannya di bawah.</p>
 *
 * <h4>Mengapa Trip berbeda dari lima helper sebelumnya</h4>
 * <table border="1">
 * <tr><th>Helper</th><th>Baris</th><th>Operasi entitas</th></tr>
 * <tr><td>Stok</td><td>330</td><td>0</td></tr>
 * <tr><td>Harga</td><td>700</td><td>11</td></tr>
 * <tr><td>Payable</td><td>933</td><td>8</td></tr>
 * <tr><td>Finance</td><td>791</td><td>5</td></tr>
 * <tr><td>Master</td><td>1519</td><td>26</td></tr>
 * <tr><td><b>Trip</b></td><td><b>1517</b></td><td><b>65</b></td></tr>
 * </table>
 *
 * <p>Enam aksinya menyentuh <b>uang</b>: penjualan tunai, setoran, biaya, retur, rekonsiliasi,
 * dan penutupan trip. Menulis enam puluh lima operasi SQL asli untuk jalur itu sekaligus,
 * tanpa uji kesetaraan per bagian, berarti menaruh kesalahan hitung uang di tempat yang paling
 * mahal untuk ditemukan.</p>
 *
 * <h4>Model tenant di sini DIRANCANG ULANG, bukan dinamai ulang</h4>
 * <p>Berbeda dengan helper sebelumnya yang sebagian besar berganti nama tabel, sisi Trip pada
 * model tenant punya bentuk yang lebih kaya:</p>
 * <ul>
 * <li>{@code sales_trip_nota} -- nota per trip, <b>dengan pisah tunai/kredit</b> yang legacy
 *     tidak punya;</li>
 * <li>{@code sales_trip_hasil} -- hasil barang per produk: terjual, kembali, rusak, selisih;</li>
 * <li>{@code sales_trip_rekonsiliasi} -- rekonsiliasi lengkap: nilai barang bawa/kembali,
 *     penjualan, biaya, setoran, dan selisihnya.</li>
 * </ul>
 * <p>Ini model yang lebih baik, tetapi memetakannya menuntut keputusan per aksi, bukan
 * penggantian nama.</p>
 *
 * <h4>Lingkup: legacy memakai toko, tenant memakai gudang</h4>
 * <p>Seluruh tabel Trip pada model tenant berlingkup {@code gudang_id}; jalur legacy menyaring
 * dengan {@code toko}. Sempat tampak sebagai penghalang, ternyata bukan: {@code gudang} memuat
 * {@code toko_id}, sehingga saringan toko tetap dapat ditegakkan lewat join.</p>
 * <p>Itu <b>wajib</b> ditegakkan, bukan dilewati -- saringan lingkup yang hilang berarti sales
 * satu toko melihat perjalanan toko lain.</p>
 *
 * <h4>Yang belum punya padanan</h4>
 * <p>{@code penerimaan_piutang} model tenant <b>tidak punya kaitan ke trip</b>, sedangkan
 * legacy menautkannya lewat {@code p.sesi}. Akibatnya {@code tripDetail} -- yang menjumlahkan
 * penerimaan piutang selama satu trip -- tidak dapat dipetakan langsung; jalurnya harus lewat
 * {@code sales_trip_nota} &rarr; {@code faktur_penjualan} &rarr; {@code piutang_customer}
 * &rarr; alokasinya. Itu keputusan tersendiri, bukan efek samping.</p>
 * <p>{@code rute} SPJ juga tidak ada; {@code wilayah} bukan padanannya (rute adalah urutan
 * kunjungan, wilayah adalah pembagian penjualan).</p>
 */
final class SalesInventoryTripTenant {

	private SalesInventoryTripTenant() {
	}

	/** Benar bila aktor ini dilayani schema tenant. */
	static boolean aktif(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.aktif(aktor);
	}

	/** Prefiks schema berikut titiknya. */
	static String skema(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.skema(aktor.tenant);
	}

	/**
	 * Benar bila aksi ini sudah punya jalur tenant. Tujuh belas aksi lain menjawab
	 * {@code false} dan ditolak pemanggilnya -- lihat catatan kelas.
	 */
	static boolean dukungAksi(String aksi) {
		return "spjList".equals(aksi) || "tripList".equals(aksi);
	}

	/**
	 * Saringan lingkup toko lewat gudang. Tabel Trip tenant berlingkup gudang, sedangkan aktor
	 * berlingkup toko; {@code gudang.toko_id} yang menjembatani.
	 */
	static String syaratToko(String skema, String aliasGudangId) {
		return " AND EXISTS (SELECT 1 FROM " + skema + "gudang g WHERE g.id = " + aliasGudangId
				+ " AND g.toko_id = ?) ";
	}

	// ------------------------------------------------------------------ daftar SPJ

	/**
	 * Daftar Surat Perintah Sales. Kolom berurutan sama dengan legacy: id, nomor, status,
	 * tanggalBerangkat, rute, kendaraan, jumlahBarang, jumlahNota, sesiId.
	 *
	 * <p>{@code rute} tidak ada pada model tenant dan dikosongkan; {@code wilayah} bukan
	 * padanannya. {@code kendaraan} juga tidak ada pada SPJ tenant -- ia melekat pada
	 * {@code sales_trip}, sehingga ditarik dari trip yang lahir dari SPJ ini.</p>
	 */
	static String selectSpj(String skema) {
		// DUA BELAS kolom, berurutan sama dengan legacy: id, nomor, status, tanggalBerangkat,
		// rute, kendaraan, uangMuka, salesId, salesNama, jumlahBarang, jumlahNota, sesiId.
		// Menggeser satu kolom akan menaruh nama sales di kolom kendaraan.
		return "SELECT j.id, COALESCE(j.nomor_dokumen,''), COALESCE(j.status,''), j.tanggal, '', "
				+ "COALESCE((SELECT t.kendaraan FROM " + skema + "sales_trip t"
				+ " WHERE t.surat_perintah_sales_id = j.id ORDER BY t.id DESC LIMIT 1),''), "
				+ "NULL, s.id, COALESCE(s.nama,''), "
				+ "(SELECT COUNT(*) FROM " + skema + "surat_perintah_sales_detail d"
				+ " WHERE d.surat_perintah_sales_id = j.id), "
				+ "(SELECT COUNT(*) FROM " + skema + "sales_trip_nota n"
				+ " JOIN " + skema + "sales_trip t2 ON n.sales_trip_id = t2.id"
				+ " WHERE t2.surat_perintah_sales_id = j.id), "
				+ "(SELECT t3.id FROM " + skema + "sales_trip t3"
				+ " WHERE t3.surat_perintah_sales_id = j.id ORDER BY t3.id DESC LIMIT 1)";
	}

	static String dasarSpj(String skema, String where) {
		return " FROM " + skema + "surat_perintah_sales j"
				+ " JOIN " + skema + "salesperson s ON j.salesperson_id = s.id" + where
				+ " ORDER BY j.id DESC LIMIT 100";
	}

	/** Nama kolom sales pada SPJ, untuk saringan. */
	static String kolomSalesSpj() {
		return "j.salesperson_id";
	}

	// ------------------------------------------------------------------ daftar trip

	/**
	 * Daftar trip. Kolom berurutan sama dengan legacy: id, nomor, status, waktuMulai,
	 * waktuKembali, spjId, totalKas.
	 */
	static String selectTrip(String skema) {
		// SEMBILAN kolom, berurutan sama dengan legacy: id, nomor, status, waktuMulai,
		// waktuKembali, spjId, spjNomor, salesNama, saldoKas.
		return "SELECT ns.id, COALESCE(ns.nomor_dokumen,''), COALESCE(ns.status,''), "
				+ "ns.tanggal_berangkat, ns.tanggal_kembali, "
				+ "COALESCE(ns.surat_perintah_sales_id,0), COALESCE(j.nomor_dokumen,''), "
				+ "COALESCE(s.nama,''), "
				+ "COALESCE((SELECT SUM(k.nilai) FROM " + skema + "sales_trip_setoran k"
				+ " WHERE k.sales_trip_id = ns.id),0)";
	}

	static String dasarTrip(String skema, String where) {
		return " FROM " + skema + "sales_trip ns"
				+ " LEFT JOIN " + skema + "surat_perintah_sales j"
				+ " ON ns.surat_perintah_sales_id = j.id"
				+ " JOIN " + skema + "salesperson s ON ns.salesperson_id = s.id" + where
				+ " ORDER BY ns.id DESC LIMIT 100";
	}

	/** Nama kolom sales pada trip, untuk saringan. */
	static String kolomSalesTrip() {
		return "ns.salesperson_id";
	}
}
