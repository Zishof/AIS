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
		return "spjList".equals(aksi) || "tripList".equals(aksi)
				|| "spjSimpan".equals(aksi) || "spjDetail".equals(aksi)
				|| "spjStatus".equals(aksi) || "tripStart".equals(aksi)
				|| "tripBarangUpdate".equals(aksi) || "tripDeposit".equals(aksi)
				|| "tripReturn".equals(aksi) || "tripReconcile".equals(aksi)
				|| "tripCashSale".equals(aksi) || "spjNotaAssign".equals(aksi)
				|| "tripNotaResult".equals(aksi) || "tripClose".equals(aksi)
				|| "expenseCategoryList".equals(aksi) || "expenseCategorySave".equals(aksi)
				|| "expenseCreate".equals(aksi) || "tripPurchaseLink".equals(aksi)
				|| "tripDetail".equals(aksi);
	}

	/**
	 * Benar bila penjualan tunai dapat dibukukan pada model tenant.
	 *
	 * <p>Tidak bisa, dan alasannya berbeda dari setoran. Legacy mencatat keduanya sebagai baris
	 * buku kas sesi: {@code CASH_SALE} positif dan {@code OWNER_DEPOSIT} negatif. Model tenant
	 * tidak punya buku kas (celah C-11), tetapi <b>punya</b> rumah untuk setoran —
	 * {@code sales_trip_setoran} — sehingga setoran tetap dapat dicatat setara.</p>
	 *
	 * <p>Penjualan tunai tidak punya rumah yang setara. Satu-satunya kandidat,
	 * {@code sales_trip_nota}, adalah <b>nota penjualan</b> berkop nomor dan tanggal, sedangkan
	 * aksi ini hanya menerima nominal. Menyisipkan nota sintetis akan menambah jumlah nota yang
	 * dilaporkan layar SPJ dan rekonsiliasi — angka yang tidak bertambah pada jalur legacy,
	 * sebab di sana yang bertambah hanya baris buku kas.</p>
	 */
	static boolean dukungPenjualanTunai() {
		return true;
	}

	// ------------------------------------------------------------------ status trip & setoran

	/** TIGA kolom: status, salespersonId, suratPerintahSalesId. */
	static String tripUntukStatus(String skema) {
		return "SELECT COALESCE(t.status,''), t.salesperson_id, t.surat_perintah_sales_id"
				+ " FROM " + skema + "sales_trip t WHERE t.id = ?";
	}

	/**
	 * EMPAT parameter: status, tanggalKembali (boleh NULL), oleh, id.
	 *
	 * <p>{@code tanggal_kembali} hanya diisi pada transisi ke RETURNED; pada transisi lain
	 * parameternya NULL dan kolomnya dibiarkan seperti semula.</p>
	 *
	 * <p>Jalur legacy juga menyetel {@code spj.tanggalKembaliAktual}. Kolom itu tidak ada pada
	 * {@code surat_perintah_sales} tenant — tanggal kembali sesungguhnya melekat pada tripnya,
	 * dan SPJ hanya punya satu trip, sehingga angkanya tetap dapat ditelusuri lewat trip itu.</p>
	 */
	static String ubahStatusTrip(String skema) {
		return "UPDATE " + skema + "sales_trip SET status = ?,"
				+ " tanggal_kembali = COALESCE(?, tanggal_kembali), oleh = ?,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/**
	 * Barang yang belum habis teralokasi, sebagai penjaga masuk RECONCILING.
	 *
	 * <p>Invarian legacy: {@code dimuat = terjual + kembali + rusak + hilang}. Model tenant
	 * memisahkan rencana ({@code sales_trip_barang.kuantitas_bawa}) dari hasil
	 * ({@code sales_trip_hasil}), dan memetakan {@code qty_hilang} legacy ke {@code selisih} —
	 * pemetaan yang sama sudah dipakai {@code tripBarangUpdate}.</p>
	 *
	 * <p>{@code LEFT JOIN} disengaja: produk yang dibawa tetapi belum punya baris hasil sama
	 * sekali harus <b>ikut tertangkap</b> sebagai belum teralokasi, bukan hilang dari
	 * pemeriksaan.</p>
	 *
	 * <p>DUA kolom: namaProduk dan sisanya.</p>
	 */
	static String barangBelumHabis(String skema) {
		String sisa = "(COALESCE(b.kuantitas_bawa,0) - COALESCE(h.kuantitas_terjual,0)"
				+ " - COALESCE(h.kuantitas_kembali,0) - COALESCE(h.kuantitas_rusak,0)"
				+ " - COALESCE(h.selisih,0))";
		return "SELECT COALESCE(pr.nama,''), " + sisa
				+ " FROM " + skema + "sales_trip_barang b"
				+ " JOIN " + skema + "produk pr ON b.produk_id = pr.id"
				+ " LEFT JOIN " + skema + "sales_trip_hasil h"
				+ " ON h.sales_trip_id = b.sales_trip_id AND h.produk_id = b.produk_id"
				+ " WHERE b.sales_trip_id = ? AND ABS(" + sisa + ") > 0.001"
				+ " ORDER BY pr.nama";
	}

	/**
	 * Setoran kas ke pemilik. ENAM parameter: tripId, tanggal, caraBayar, nomorBukti, nilai,
	 * oleh.
	 *
	 * <p>Legacy mencatatnya sebagai baris buku kas {@code OWNER_DEPOSIT} bernilai negatif; di
	 * sini ia dokumen tersendiri. Arahnya tetap sama terhadap saldo kas: rumus
	 * {@link #saldoKas(String)} mengurangkan setoran, persis seperti penjumlahan bertanda pada
	 * jalur legacy.</p>
	 *
	 * <p><b>{@code keterangan} tidak punya kolom di sini</b> — {@code sales_trip_setoran} hanya
	 * menyediakan {@code nomor_bukti}. Isian itu tidak disimpan, dan hal itu disampaikan balik
	 * pada respons alih-alih dibuang diam-diam; lihat celah C-13.</p>
	 */
	static String sisipSetoran(String skema) {
		return "INSERT INTO " + skema + "sales_trip_setoran (sales_trip_id, tanggal, cara_bayar,"
				+ " nomor_bukti, nilai, status, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, 'AKTIF', now(), ?)";
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
				+ "COALESCE(s.nama,''), " + saldoKas(skema);
	}

	/**
	 * <h4>Saldo kas sesi: kas yang MASIH DIPEGANG sales, bukan total setoran</h4>
	 *
	 * <p>Legacy menyimpan penjualan tunai dan setoran pada satu buku kas
	 * ({@code nota_sales_kas}): tunai masuk bertanda positif, setoran ke pemilik
	 * <b>dinegatifkan</b>. Jumlahnya karena itu adalah kas bersih yang masih ada di tangan
	 * sales.</p>
	 *
	 * <p>Model tenant memisahkannya: bagian tunai tiap nota ada di
	 * {@code sales_trip_nota.tunai}, setoran ada di {@code sales_trip_setoran.nilai} dan
	 * seluruhnya positif karena tabelnya memang khusus setoran.</p>
	 *
	 * <p><b>Menjumlahkan setoran saja bukan padanan saldo kas</b> -- itu angka yang berbeda
	 * arah maknanya. Untuk trip bertunai 1.000.000 dan setoran 800.000, saldo kas adalah
	 * 200.000 (yang masih dipegang), bukan 800.000 (yang sudah disetor). Rumus di bawah
	 * mengurangkan keduanya, sebagaimana penjumlahan bertanda pada jalur legacy.</p>
	 */
	/**
	 * <h4>Saldo kas dibaca dari BUKUNYA, sejak migrasi v12</h4>
	 *
	 * <p>Rumus sebelumnya, {@code Σ nota.tunai − Σ setoran}, adalah tambalan atas ketiadaan buku
	 * kas — dan tambalan yang <b>salah</b>. Ia mengabaikan uang muka operasional, penagihan tunai
	 * piutang lama, dan biaya tunai. Untuk trip berpanjar 500.000 dengan penjualan tunai 300.000,
	 * penagihan tunai 200.000, biaya 100.000, dan setoran 400.000, ia menghasilkan
	 * <b>−100.000</b> di tempat jalur legacy menghasilkan <b>500.000</b>: bukan selisih kecil,
	 * melainkan beda tanda.</p>
	 *
	 * <p>Sekarang angkanya adalah penjumlahan bertanda {@code sales_trip_kas}, sama persis dengan
	 * jalur legacy menjumlahkan {@code nota_sales_kas}. Tidak ada {@code CASE}, tidak ada daftar
	 * jenis yang harus diingat pembaca — lihat {@link ais.service.tenant.TenantKasTrip}.</p>
	 */
	static String saldoKas(String skema) {
		return ais.service.tenant.TenantKasTrip.sqlSaldoKas(skema, "ns.id");
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

	// ------------------------------------------------------------------ kelompok SPJ

	/**
	 * <h4>Tiga celah model yang membentuk jalur SPJ</h4>
	 *
	 * <p><b>1. Lingkup: permintaan membawa toko, model tenant menuntut gudang.</b>
	 * {@code surat_perintah_sales.gudang_id} boleh kosong, tetapi mengosongkannya membuat SPJ
	 * <b>tidak terlihat</b> oleh saringan lingkup toko — yang justru menegakkan lewat
	 * {@code gudang.toko_id}. Satu toko boleh punya beberapa gudang, sehingga memilihkannya
	 * secara sepihak berarti menebak. Jalur tenant karena itu <b>menuntut {@code gudang_id}
	 * eksplisit</b> pada permintaan.</p>
	 *
	 * <p><b>2. Idempotensi tidak punya tempat.</b> {@code surat_perintah_sales} tidak memiliki
	 * {@code idempotency_key} maupun {@code correlation_id}. Jalur legacy memakai
	 * {@code kode_unik} untuk mencegah SPJ ganda saat permintaan diulang. Bila permintaan
	 * membawa {@code kode_unik}, jalur tenant <b>menolaknya</b> alih-alih mengabaikannya:
	 * menerima kunci idempotensi lalu tidak menghormatinya lebih buruk daripada berterus terang,
	 * sebab pemanggil akan mengira pengulangan aman.</p>
	 *
	 * <p><b>3. Penyetuju tidak tersimpan.</b> Tidak ada kolom {@code disetujui_oleh} pada SPJ
	 * tenant. Perpindahan status tetap ditegakkan; identitas penyetujunya yang tidak tercatat.
	 * Kebutuhan yang sama dengan alasan nonaktif pada helper Master — keduanya menunggu
	 * {@code TenantAuditWriter}.</p>
	 */
	static boolean dukungIdempotensiSpj() {
		return false;
	}

	/** Benar bila model tenant menyimpan identitas penyetuju SPJ. */
	static boolean dukungPenyetujuSpj() {
		return false;
	}

	/**
	 * Benar bila model tenant mengenal penugasan piutang ke SPJ.
	 *
	 * <p>Jalur legacy menautkan dokumen piutang ke SPJ lewat {@code spj_sales_nota} — rencana
	 * penagihan sebelum berangkat. Model tenant tidak punya konsep itu: {@code sales_trip_nota}
	 * menautkan <b>trip ke faktur penjualan</b>, yakni nota yang <b>dihasilkan</b> selama
	 * perjalanan, bukan piutang yang <b>direncanakan</b> untuk ditagih.</p>
	 *
	 * <p>Keduanya berbeda arah waktu dan berbeda maksud. Memetakan salah satu ke yang lain akan
	 * membuat daftar rencana penagihan menampilkan hasil penjualan.</p>
	 */
	static boolean dukungNotaSpj() {
		return false;
	}

	/** Keberadaan satu SPJ di schema tenant. */
	static String adaSpj(String skema) {
		return "SELECT COUNT(*) FROM " + skema + "surat_perintah_sales WHERE id = ?";
	}

	/** Status dan pemilik SPJ, untuk memeriksa wewenang sebelum mengubah. */
	static String statusSpj(String skema) {
		return "SELECT COALESCE(j.status,''), j.salesperson_id, j.gudang_id"
				+ " FROM " + skema + "surat_perintah_sales j WHERE j.id = ?";
	}

	/**
	 * Rinci SPJ. Kolom berurutan: id, nomor, status, tanggalBerangkat, rute, kendaraan,
	 * uangMuka, catatan, salesId, salesNama, tokoId, sesiId.
	 *
	 * <p>{@code rute} dan {@code uangMuka} tidak ada pada model tenant; {@code kendaraan}
	 * melekat pada trip, bukan SPJ. {@code tokoId} diturunkan dari gudangnya.</p>
	 */
	static String sqlDetailSpj(String skema) {
		return "SELECT j.id, COALESCE(j.nomor_dokumen,''), COALESCE(j.status,''), j.tanggal, '', "
				+ "COALESCE((SELECT t.kendaraan FROM " + skema + "sales_trip t"
				+ " WHERE t.surat_perintah_sales_id = j.id ORDER BY t.id DESC LIMIT 1),''), "
				+ "COALESCE(j.uang_muka_operasional,0), COALESCE(j.keterangan,''),"
				+ " j.salesperson_id, COALESCE(s.nama,''), "
				+ "COALESCE((SELECT g.toko_id FROM " + skema + "gudang g WHERE g.id = j.gudang_id),0), "
				+ "(SELECT t2.id FROM " + skema + "sales_trip t2"
				+ " WHERE t2.surat_perintah_sales_id = j.id ORDER BY t2.id DESC LIMIT 1)"
				+ " FROM " + skema + "surat_perintah_sales j"
				+ " LEFT JOIN " + skema + "salesperson s ON j.salesperson_id = s.id"
				+ " WHERE j.id = ?";
	}

	/** Baris barang SPJ: produkId, kode, nama, qtyRencana. */
	static String sqlBarangSpj(String skema) {
		return "SELECT d.produk_id, COALESCE(p.kode,''), COALESCE(p.nama,''), "
				+ "COALESCE(d.kuantitas,0)"
				+ " FROM " + skema + "surat_perintah_sales_detail d"
				+ " JOIN " + skema + "produk p ON d.produk_id = p.id"
				+ " WHERE d.surat_perintah_sales_id = ? ORDER BY d.id ASC";
	}

	// ---------- tulis ----------

	/**
	 * TUJUH parameter: nomor, tanggal, salespersonId, gudangId, keterangan,
	 * uangMukaOperasional, oleh.
	 *
	 * <p>{@code uang_muka_operasional} ada sejak migrasi v12. Ia sumber baris pembuka buku kas:
	 * tanpa disimpan di sini, trip yang dimulai tidak punya angka apa pun untuk dibukukan sebagai
	 * {@code OPENING_ADVANCE}, dan saldo kasnya berangkat dari nol padahal sales membawa uang.</p>
	 */
	static String sisipSpj(String skema) {
		return "INSERT INTO " + skema + "surat_perintah_sales"
				+ " (nomor_dokumen, tanggal, salesperson_id, gudang_id, keterangan,"
				+ " uang_muka_operasional, status, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', now(), ?)";
	}

	/** TUJUH parameter: tanggal, salespersonId, gudangId, keterangan, uangMuka, oleh, id. */
	static String ubahSpj(String skema) {
		return "UPDATE " + skema + "surat_perintah_sales SET tanggal = ?, salesperson_id = ?,"
				+ " gudang_id = ?, keterangan = ?, uang_muka_operasional = ?,"
				+ " tanggal_dirubah = now(), oleh = ? WHERE id = ?";
	}

	/** Nomor dokumen final, disusun sesudah id terbentuk -- sama pola dengan jalur legacy. */
	static String ubahNomorSpj(String skema) {
		return "UPDATE " + skema + "surat_perintah_sales SET nomor_dokumen = ? WHERE id = ?";
	}

	static String hapusDetailSpj(String skema) {
		return "DELETE FROM " + skema + "surat_perintah_sales_detail"
				+ " WHERE surat_perintah_sales_id = ?";
	}

	static String sisipDetailSpj(String skema) {
		return "INSERT INTO " + skema + "surat_perintah_sales_detail"
				+ " (surat_perintah_sales_id, produk_id, kuantitas, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, now(), ?)";
	}

	static String adaProduk(String skema) {
		return "SELECT COUNT(*) FROM " + skema + "produk WHERE id = ?";
	}

	/** Gudang milik toko tertentu, untuk memvalidasi gudang_id yang dikirim. */
	static String gudangMilikToko(String skema) {
		return "SELECT COUNT(*) FROM " + skema + "gudang WHERE id = ? AND toko_id = ?";
	}

	static String ubahStatusSpj(String skema) {
		return "UPDATE " + skema + "surat_perintah_sales SET status = ?, tanggal_dirubah = now(),"
				+ " oleh = ? WHERE id = ?";
	}

	// ------------------------------------------------------------------ kelompok trip non-uang

	/**
	 * <h4>Uang muka operasional: bukan celah, melainkan perbedaan rancangan</h4>
	 *
	 * <p>Jalur legacy memulai trip dengan {@code saldoKasAwal} yang disalin dari
	 * {@code spj.uangMukaOperasional} — kas mengambang yang dibawa sales dan diperhitungkan saat
	 * rekonsiliasi.</p>
	 *
	 * <p>Model tenant tidak punya keduanya, dan rekonsiliasinya memang <b>tidak memakainya</b>:
	 * {@code sales_trip_rekonsiliasi} menimbang nilai barang bawa, barang kembali, penjualan,
	 * biaya, dan setoran — tanpa kas mengambang. Jadi ini bukan medan yang hilang, melainkan
	 * cara rekonsiliasi yang berbeda.</p>
	 *
	 * <p>Karena itu {@code tripStart} pada jalur tenant memulai trip tanpa saldo kas awal, dan
	 * itu benar untuk model ini.</p>
	 */
	static boolean dukungUangMukaTrip() {
		return false;
	}

	/**
	 * Benar bila model tenant mengenal nota kunjungan per SPJ.
	 *
	 * <p>{@code tripNotaResult} memperbarui hasil kunjungan pada {@code spj_sales_nota} —
	 * konsep yang, sebagaimana dicatat pada {@link #dukungNotaSpj()}, tidak ada di model tenant.
	 * Aksinya tetap ditolak.</p>
	 */
	static boolean dukungHasilKunjungan() {
		return false;
	}

	/**
	 * Benar bila model tenant mencatat pembelian yang dilakukan selama trip.
	 *
	 * <p>Tidak ada tabel padanan {@code nota_sales_pembelian}. Karena {@code tripDetail}
	 * menjumlahkannya bersama penerimaan piutang — yang juga tidak punya kaitan ke trip —
	 * aksinya tetap ditolak sampai kedua jalurnya diputuskan.</p>
	 */
	/**
	 * Benar bila pembelian dalam perjalanan dapat dicatat. Sejak migrasi v16: bisa.
	 *
	 * <p>Sebelumnya model tenant tidak punya tabelnya sama sekali, sehingga pembayaran pemasok
	 * dari kas trip tidak punya dokumen pendamping dan rekap penutupan terpaksa menyatakan
	 * angkanya nol menurut definisi.</p>
	 */
	static boolean dukungPembelianTrip() {
		return true;
	}

	/** Status dan kepemilikan SPJ beserta gudangnya, untuk memulai trip. */
	/** LIMA kolom: status, salespersonId, gudangId, tanggal, uangMukaOperasional. */
	static String spjUntukMulai(String skema) {
		return "SELECT COALESCE(j.status,''), j.salesperson_id, j.gudang_id, j.tanggal,"
				+ " COALESCE(j.uang_muka_operasional,0)"
				+ " FROM " + skema + "surat_perintah_sales j WHERE j.id = ?";
	}

	/** Trip yang sudah lahir dari SPJ ini; satu SPJ hanya boleh satu trip. */
	static String tripDariSpj(String skema) {
		return "SELECT COUNT(*) FROM " + skema + "sales_trip WHERE surat_perintah_sales_id = ?";
	}

	static String sisipTrip(String skema) {
		return "INSERT INTO " + skema + "sales_trip (nomor_dokumen, surat_perintah_sales_id,"
				+ " salesperson_id, gudang_id, tanggal_berangkat, status, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, 'ACTIVE', now(), ?)";
	}

	static String ubahNomorTrip(String skema) {
		return "UPDATE " + skema + "sales_trip SET nomor_dokumen = ? WHERE id = ?";
	}

	/**
	 * Menyalin barang rencana SPJ menjadi barang yang dibawa trip.
	 *
	 * <p>Legacy menyimpan rencana dan hasil pada <b>satu</b> baris {@code spj_sales_barang};
	 * model tenant memisahkannya menjadi {@code sales_trip_barang} (yang dibawa) dan
	 * {@code sales_trip_hasil} (yang terjadi). Pemisahan itu membuat rencana tidak tertimpa
	 * hasil — riwayat berapa yang dibawa tetap terbaca sesudah trip ditutup.</p>
	 */
	static String salinBarangTrip(String skema) {
		return "INSERT INTO " + skema + "sales_trip_barang (sales_trip_id, produk_id,"
				+ " kuantitas_bawa, dibuat_pada, oleh)"
				+ " SELECT ?, d.produk_id, d.kuantitas, now(), ?"
				+ " FROM " + skema + "surat_perintah_sales_detail d"
				+ " WHERE d.surat_perintah_sales_id = ?";
	}

	static String ubahStatusSpjJadiAktif(String skema) {
		return "UPDATE " + skema + "surat_perintah_sales SET status = 'ACTIVE',"
				+ " tanggal_dirubah = now(), oleh = ? WHERE id = ?";
	}

	// ---------- hasil barang ----------

	/** Barang trip beserta status tripnya, untuk memeriksa wewenang sebelum memperbarui. */
	static String barangTrip(String skema) {
		return "SELECT b.sales_trip_id, b.produk_id, COALESCE(t.status,'')"
				+ " FROM " + skema + "sales_trip_barang b"
				+ " JOIN " + skema + "sales_trip t ON b.sales_trip_id = t.id"
				+ " WHERE b.id = ?";
	}

	static String adaHasil(String skema) {
		return "SELECT id FROM " + skema + "sales_trip_hasil"
				+ " WHERE sales_trip_id = ? AND produk_id = ? LIMIT 1";
	}

	/**
	 * Hasil barang per produk. {@code qty_hilang} legacy dipetakan ke {@code selisih}: keduanya
	 * menyatakan kuantitas yang tidak kembali dan tidak terjual.
	 */
	static String sisipHasil(String skema) {
		return "INSERT INTO " + skema + "sales_trip_hasil (sales_trip_id, produk_id,"
				+ " kuantitas_terjual, kuantitas_kembali, kuantitas_rusak, selisih,"
				+ " dibuat_pada, oleh) VALUES (?, ?, ?, ?, ?, ?, now(), ?)";
	}

	static String ubahHasil(String skema) {
		return "UPDATE " + skema + "sales_trip_hasil SET kuantitas_terjual = ?,"
				+ " kuantitas_kembali = ?, kuantitas_rusak = ?, selisih = ?,"
				+ " tanggal_dirubah = now(), oleh = ? WHERE id = ?";
	}
	// ------------------------------------------------------------------ buku kas trip (v12)

	/**
	 * Menulis satu baris buku kas. TUJUH parameter: tripId, jenis, nominal, referensi,
	 * keterangan, idempotencyKey, oleh.
	 *
	 * <p><b>{@code nominal} harus sudah bertanda saat sampai di sini.</b> Pemanggil yang
	 * mencatat uang keluar wajib mengirim nilai negatif; tidak ada kolom arah yang akan
	 * memperbaikinya belakangan. Kontraknya pada
	 * {@link ais.service.tenant.TenantKasTrip}.</p>
	 */
	static String sisipKas(String skema) {
		return "INSERT INTO " + skema + "sales_trip_kas (sales_trip_id, jenis, nominal,"
				+ " referensi, keterangan, idempotency_key, waktu, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, ?, now(), now(), ?)";
	}

	/** Uang muka yang benar-benar sudah dibukukan; padanan saldoKasAwal legacy. */
	static String uangMukaAwal(String skema) {
		return ais.service.tenant.TenantKasTrip.sqlUangMukaAwal(skema, "ns.id");
	}

	// ------------------------------------------------------------------ pembalikan biaya (v12)

	/**
	 * Benar bila biaya trip dapat dibalik. Sejak v12: bisa.
	 *
	 * <p>Sebelumnya {@code sales_trip_biaya} adalah satu-satunya tabel dokumen tanpa
	 * {@code pembalik_dari_id}, tanpa {@code status}, dan tanpa cara membedakan biaya tunai.
	 * Ketiganya ditambahkan bundel v12.</p>
	 */
	static boolean dukungPembalikanBiaya() {
		return true;
	}

	/** Biaya pembalik yang sudah ada, dikenali dari kunci idempotensinya. */
	static String cariBiayaPembalik(String skema) {
		return "SELECT id FROM " + skema + "sales_trip_biaya WHERE idempotency_key = ? LIMIT 1";
	}

	/**
	 * ENAM kolom biaya asal: tripId, kategori, keterangan, nilai, caraBayar, status —
	 * ditambah status tripnya sebagai kolom KETUJUH.
	 *
	 * <p>Status trip ikut dibaca di sini supaya penjaga "sesi sudah ditutup" dapat ditegakkan
	 * tanpa kueri kedua. Jalur legacy menolak pembalikan biaya pada sesi CLOSED, sebab snapshot
	 * penutupan tidak boleh berubah diam-diam.</p>
	 */
	static String biayaUntukBalik(String skema) {
		return "SELECT b.sales_trip_id, COALESCE(b.kategori,''), COALESCE(b.keterangan,''),"
				+ " COALESCE(b.nilai,0), COALESCE(b.cara_bayar,''), COALESCE(b.status,'AKTIF'),"
				+ " COALESCE(t.status,''), b.kategori_biaya_id"
				+ " FROM " + skema + "sales_trip_biaya b"
				+ " JOIN " + skema + "sales_trip t ON b.sales_trip_id = t.id"
				+ " WHERE b.id = ?";
	}

	/**
	 * Biaya pembalik: nilainya negatif, berstatus REVERSAL, menunjuk asalnya.
	 *
	 * <p>DELAPAN parameter: tripId, kategori, keterangan, nilai, caraBayar, idempotencyKey,
	 * pembalikDariId, oleh.</p>
	 */
	static String sisipBiayaPembalik(String skema) {
		return "INSERT INTO " + skema + "sales_trip_biaya (sales_trip_id, kategori,"
				+ " kategori_biaya_id, keterangan, nilai, tanggal, cara_bayar, idempotency_key,"
				+ " pembalik_dari_id, status, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, CURRENT_DATE, ?, ?, ?, 'REVERSAL', now(), ?)";
	}

	/** Menandai biaya asal DIBATALKAN; barisnya tetap ada dan tetap terbaca di riwayat. */
	static String batalkanBiaya(String skema) {
		return "UPDATE " + skema + "sales_trip_biaya SET status = 'DIBATALKAN' WHERE id = ?";
	}

	/**
	 * Jejak pembalikan. Tabel {@code reversal_log} bersifat umum, sehingga biaya trip memakai
	 * tabel yang sama dengan pembalikan pembayaran hutang.
	 *
	 * <p>Ini juga menampung {@code alasanReversal} legacy, yang tidak punya kolom sendiri pada
	 * {@code sales_trip_biaya}.</p>
	 */
	static String catatReversalBiaya(String skema) {
		return "INSERT INTO " + skema + "reversal_log (dokumen_tipe, dokumen_id, alasan,"
				+ " user_id, waktu) VALUES ('SALES_TRIP_BIAYA', ?, ?, ?, now())";
	}
	// ------------------------------------------------------------------ nota bawaan (v13)

	/**
	 * <h4>Nota bawaan: piutang lama yang dititipkan untuk ditagih</h4>
	 *
	 * <p>Tabel {@code surat_perintah_sales_nota} lahir bersama migrasi v13. Ia menggantung pada
	 * SPJ, bukan pada trip, sebab penugasannya terjadi sebelum berangkat.</p>
	 *
	 * <p>Empat medan legacy tidak punya kolom di sini dan ditarik lewat join atau diturunkan:
	 * {@code nilaiAwal}, {@code jatuhTempo}, {@code customer} — salinan dari piutangnya — serta
	 * {@code nilaiTertagih}, yang dihitung dari alokasi penerimaan. Lihat
	 * {@link ais.service.tenant.TenantSchemaMigrationsV13}.</p>
	 */
	static String spjUntukNota(String skema) {
		return "SELECT COALESCE(j.status,''), j.salesperson_id"
				+ " FROM " + skema + "surat_perintah_sales j WHERE j.id = ?";
	}

	/**
	 * Sisa tagihan satu dokumen piutang PADA SAAT INI — dipakai sebagai potret
	 * {@code saldo_saat_assign}.
	 *
	 * <p>Dihitung dari alokasi, bukan dibaca dari kolom {@code sisa}: kolom ringkasan itu bisa
	 * basi, dan potret yang salah akan terus salah selamanya sebab tidak pernah dihitung ulang.
	 * Justru karena angkanya dibekukan, angka yang dibekukan harus benar.</p>
	 */
	static String sisaPiutangSekarang(String skema) {
		return "SELECT COALESCE(p.nilai,0) - COALESCE((SELECT SUM(a.nilai)"
				+ " FROM " + skema + "alokasi_penerimaan_piutang a"
				+ " WHERE a.piutang_customer_id = p.id),0)"
				+ " FROM " + skema + "piutang_customer p WHERE p.id = ?";
	}

	/** Penggantian menyeluruh: dokumen belum jalan, jadi aman diganti utuh. */
	static String hapusNotaSpj(String skema) {
		return "DELETE FROM " + skema + "surat_perintah_sales_nota"
				+ " WHERE surat_perintah_sales_id = ?";
	}

	/** EMPAT parameter: spjId, piutangId, saldoSaatAssign, oleh. */
	static String sisipNotaSpj(String skema) {
		return "INSERT INTO " + skema + "surat_perintah_sales_nota (surat_perintah_sales_id,"
				+ " piutang_customer_id, saldo_saat_assign, status, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, 'ASSIGNED', now(), ?)";
	}

	/** DUA kolom: status nota dan salesperson pemilik SPJ-nya, untuk penjaga lingkup. */
	static String notaUntukHasil(String skema) {
		return "SELECT COALESCE(n.status,''), j.salesperson_id"
				+ " FROM " + skema + "surat_perintah_sales_nota n"
				+ " JOIN " + skema + "surat_perintah_sales j"
				+ " ON n.surat_perintah_sales_id = j.id WHERE n.id = ?";
	}

	/** LIMA parameter: status, hasilKunjungan, janjiBayar (boleh NULL), alasanGagal, oleh, id. */
	static String ubahHasilNota(String skema) {
		return "UPDATE " + skema + "surat_perintah_sales_nota SET status = ?,"
				+ " hasil_kunjungan = ?, janji_bayar = ?, alasan_gagal = ?, oleh = ?,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/**
	 * Nilai yang tertagih atas satu nota bawaan selama perjalanannya. DITURUNKAN, tidak
	 * disimpan.
	 *
	 * <p>Penerimaan yang menunjuk trip milik SPJ nota ini, teralokasi ke dokumen piutangnya.
	 * Alokasi pembalik bernilai negatif, sehingga pembatalan penagihan menurunkan angka ini
	 * dengan sendirinya — tidak ada pengurang yang bisa terlupa.</p>
	 */
	static String nilaiTertagihNota(String skema, String aliasNota) {
		return "COALESCE((SELECT SUM(a.nilai)"
				+ " FROM " + skema + "alokasi_penerimaan_piutang a"
				+ " JOIN " + skema + "penerimaan_piutang r ON a.penerimaan_piutang_id = r.id"
				+ " JOIN " + skema + "sales_trip t ON r.sales_trip_id = t.id"
				+ " WHERE a.piutang_customer_id = " + aliasNota + ".piutang_customer_id"
				+ " AND t.surat_perintah_sales_id = " + aliasNota
				+ ".surat_perintah_sales_id),0)";
	}
	// ------------------------------------------------------------------ penutupan trip (v14)

	/**
	 * <h4>Snapshot penutupan: dari buku, ke satu baris rekonsiliasi</h4>
	 *
	 * <p>Jalur legacy menyimpan sepuluh medan ringkasan pada sesinya sendiri. Model tenant
	 * menaruhnya pada {@code sales_trip_rekonsiliasi} — satu baris per trip, dijaga
	 * {@code UNIQUE (sales_trip_id)} — dan tidak semuanya perlu kolom: pemilahan penerimaan
	 * tunai/non-tunai diturunkan, dan pembayaran pembelian bernilai nol menurut definisi sebab
	 * model tenant belum punya pembelian dalam trip.</p>
	 */
	static String ringkasanKasTutup(String skema) {
		return "SELECT COALESCE(SUM(k.nominal),0),"
				+ " COALESCE(SUM(CASE WHEN k.jenis = '"
				+ ais.service.tenant.TenantKasTrip.OWNER_DEPOSIT
				+ "' THEN -k.nominal ELSE 0 END),0)"
				+ " FROM " + skema + "sales_trip_kas k WHERE k.sales_trip_id = ?";
	}

	/**
	 * Total biaya trip. Baris pembalik bernilai negatif, sehingga penjumlahan menghasilkan
	 * biaya BERSIH — sama seperti jalur legacy menjumlahkan seluruh barisnya.
	 */
	static String totalBiayaTrip(String skema) {
		return "SELECT COALESCE(SUM(b.nilai),0) FROM " + skema + "sales_trip_biaya b"
				+ " WHERE b.sales_trip_id = ?";
	}

	/** DUA kolom: total tertagih dan bagian tunainya. */
	static String ringkasanTagihTrip(String skema) {
		return "SELECT COALESCE(SUM(p.nilai),0),"
				+ " COALESCE(SUM(CASE WHEN p.cara_bayar = 'TUNAI' THEN p.nilai ELSE 0 END),0)"
				+ " FROM " + skema + "penerimaan_piutang p WHERE p.sales_trip_id = ?";
	}

	/**
	 * DUA kolom: nilai barang dibawa dan nilai barang kembali.
	 *
	 * <p>Keduanya dinilai memakai {@code harga_satuan} pada baris barang yang dibawa — harga
	 * yang berlaku saat pemuatan, bukan harga sekarang. Menilai barang kembali dengan harga
	 * sekarang akan membuat rekonsiliasi berubah angka setiap kali daftar harga diperbarui.</p>
	 */
	static String nilaiBarangTrip(String skema) {
		return "SELECT COALESCE(SUM(COALESCE(b.kuantitas_bawa,0)"
				+ " * COALESCE(b.harga_satuan,0)),0),"
				+ " COALESCE(SUM(COALESCE(h.kuantitas_kembali,0)"
				+ " * COALESCE(b.harga_satuan,0)),0)"
				+ " FROM " + skema + "sales_trip_barang b"
				+ " LEFT JOIN " + skema + "sales_trip_hasil h"
				+ " ON h.sales_trip_id = b.sales_trip_id AND h.produk_id = b.produk_id"
				+ " WHERE b.sales_trip_id = ?";
	}

	static String nilaiPenjualanTrip(String skema) {
		return "SELECT COALESCE(SUM(n.total),0) FROM " + skema + "sales_trip_nota n"
				+ " WHERE n.sales_trip_id = ?";
	}

	/**
	 * Memperbarui rekonsiliasi penutupan yang sudah ada. SEPULUH parameter: nilaiBarangBawa,
	 * nilaiBarangKembali, nilaiPenjualan, nilaiBiaya, nilaiSetoran, selisih, kasFisikAktual,
	 * keterangan, disetujuiOleh, tripId.
	 *
	 * <p>Bersama {@link #sisipRekonsiliasi(String)} membentuk upsert <b>UPDATE-lalu-INSERT</b>,
	 * bukan {@code ON CONFLICT}. Lapisan tenant konsisten bergaya PostgreSQL 9.3 —
	 * {@code TenantDataPlaneService} menyatakannya, dan penjaga struktural pada katalog migrasi
	 * menegakkannya. Versi pertama metode ini memakai {@code ON CONFLICT} dan luput dari penjaga
	 * itu semata karena penjaganya hanya memeriksa bundel migrasi, bukan SQL runtime.</p>
	 */
	static String perbaruiRekonsiliasi(String skema) {
		return "UPDATE " + skema + "sales_trip_rekonsiliasi SET nilai_barang_bawa = ?,"
				+ " nilai_barang_kembali = ?, nilai_penjualan = ?, nilai_biaya = ?,"
				+ " nilai_setoran = ?, selisih = ?, kas_fisik_aktual = ?, keterangan = ?,"
				+ " disetujui_oleh = ?, disetujui_pada = now(), status = 'CLOSED',"
				+ " tanggal_dirubah = now() WHERE sales_trip_id = ?";
	}

	/**
	 * Menyisipkan rekonsiliasi penutupan. SEBELAS parameter: tripId, nilaiBarangBawa,
	 * nilaiBarangKembali, nilaiPenjualan, nilaiBiaya, nilaiSetoran, selisih, kasFisikAktual,
	 * keterangan, disetujuiOleh, oleh.
	 *
	 * <p>Dijalankan hanya bila {@link #perbaruiRekonsiliasi(String)} tidak mengenai satu baris
	 * pun. Batasan {@code UNIQUE (sales_trip_id)} tetap menjadi penjaga terakhir bila dua
	 * penutupan berpacu.</p>
	 */
	static String sisipRekonsiliasi(String skema) {
		return "INSERT INTO " + skema + "sales_trip_rekonsiliasi (sales_trip_id, tanggal,"
				+ " nilai_barang_bawa, nilai_barang_kembali, nilai_penjualan, nilai_biaya,"
				+ " nilai_setoran, selisih, kas_fisik_aktual, keterangan, disetujui_oleh,"
				+ " disetujui_pada, status, dibuat_pada, oleh)"
				+ " VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), 'CLOSED',"
				+ " now(), ?)";
	}

	/** DUA parameter: oleh, tripId. */
	static String tutupTrip(String skema) {
		return "UPDATE " + skema + "sales_trip SET status = 'CLOSED', oleh = ?,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/**
	 * Menandai seluruh nota bawaan SPJ ini RECONCILED.
	 *
	 * <p>Barang yang dibawa TIDAK ikut ditandai: jalur legacy menyetel status per baris barang,
	 * sedangkan pada model tenant kefinalan itu sudah dinyatakan status tripnya yang menjadi
	 * CLOSED. Nota berbeda — statusnya menyimpan hasil kunjungan per nota, jadi RECONCILED di
	 * sana adalah keadaan nota itu sendiri.</p>
	 */
	static String rekonsiliasiNotaSpj(String skema) {
		return "UPDATE " + skema + "surat_perintah_sales_nota SET status = 'RECONCILED',"
				+ " tanggal_dirubah = now() WHERE surat_perintah_sales_id = ?";
	}
	// ------------------------------------------------------------------ kategori biaya (v15)

	/**
	 * <h4>Kategori biaya: master sejak v15, teks bebas sebelumnya</h4>
	 *
	 * <p>{@code sales_trip_biaya.kategori} lama tetap dibaca sebagai cadangan lewat
	 * {@code COALESCE(k.kode, b.kategori)}: baris baru menjawab lewat penunjuknya, baris hasil
	 * impor legacy menjawab lewat teksnya. Keduanya tidak pernah terisi bersamaan pada baris
	 * yang sama.</p>
	 *
	 * <p>TUJUH kolom, berurutan sama dengan keluaran JSON legacy: id, kode, nama, aktif,
	 * akunId, akunKode, akunNama.</p>
	 */
	static String daftarKategoriBiaya(String skema, boolean semua) {
		return "SELECT k.id, COALESCE(k.kode,''), COALESCE(k.nama,''),"
				+ " COALESCE(k.aktif,true), k.akun_id, COALESCE(a.kode,''), COALESCE(a.nama,'')"
				+ " FROM " + skema + "kategori_biaya_sales k"
				+ " LEFT JOIN " + skema + "akun a ON k.akun_id = a.id"
				+ (semua ? "" : " WHERE COALESCE(k.aktif,true) = true")
				+ " ORDER BY k.id ASC";
	}

	static String cariKategoriByKode(String skema) {
		return "SELECT id FROM " + skema + "kategori_biaya_sales WHERE kode = ? LIMIT 1";
	}

	/** LIMA parameter: kode, nama, aktif, akunId, oleh. */
	static String sisipKategoriBiaya(String skema) {
		return "INSERT INTO " + skema + "kategori_biaya_sales (kode, nama, aktif, akun_id,"
				+ " dibuat_pada, oleh) VALUES (?, ?, ?, ?, now(), ?)";
	}

	/**
	 * TIGA parameter: nama, aktif (boleh NULL = biarkan), id.
	 *
	 * <p>{@code aktif} memakai {@code COALESCE} supaya permintaan yang tidak menyebutkannya
	 * tidak diam-diam menyalakan kembali kategori yang sengaja dimatikan — jalur legacy juga
	 * hanya menyetelnya bila kuncinya ada.</p>
	 */
	static String perbaruiKategoriBiaya(String skema) {
		return "UPDATE " + skema + "kategori_biaya_sales SET nama = ?,"
				+ " aktif = COALESCE(?, aktif), tanggal_dirubah = now() WHERE id = ?";
	}

	/**
	 * DUA parameter: akunId (boleh NULL), id.
	 *
	 * <p>Pernyataan terpisah, dijalankan HANYA bila permintaan menyebut {@code akunId}.
	 * Menggabungkannya ke pernyataan di atas akan membuat permintaan yang tidak menyebut akun
	 * mengosongkan akun beban yang sudah disetel — dan akun beban yang hilang membuat mesin
	 * posting tidak tahu ke mana biaya dibukukan.</p>
	 */
	static String ubahAkunKategori(String skema) {
		return "UPDATE " + skema + "kategori_biaya_sales SET akun_id = ?,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/** DUA kolom: kode dan nama kategori; dipakai memvalidasi dan menyusun keterangan kas. */
	static String kategoriBiayaById(String skema) {
		return "SELECT COALESCE(kode,''), COALESCE(nama,'') FROM " + skema
				+ "kategori_biaya_sales WHERE id = ?";
	}

	// ------------------------------------------------------------------ catat biaya (v15)

	static String cariBiayaByKunci(String skema) {
		return "SELECT id FROM " + skema + "sales_trip_biaya WHERE idempotency_key = ? LIMIT 1";
	}

	/**
	 * SEMBILAN parameter: tripId, kategoriBiayaId, keterangan, nilai, caraBayar, penerima,
	 * nomorBukti, idempotencyKey, oleh.
	 *
	 * <p>Kolom teks {@code kategori} sengaja <b>tidak</b> diisi untuk baris baru: penunjuknya
	 * yang berwenang, dan menyalin kodenya ke sana hanya melahirkan salinan yang membeku saat
	 * kategori berganti nama.</p>
	 */
	static String sisipBiaya(String skema) {
		return "INSERT INTO " + skema + "sales_trip_biaya (sales_trip_id, kategori_biaya_id,"
				+ " keterangan, nilai, tanggal, cara_bayar, penerima, nomor_bukti,"
				+ " idempotency_key, status, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, CURRENT_DATE, ?, ?, ?, ?, 'AKTIF', now(), ?)";
	}
	// ------------------------------------------------------------------ pembelian dalam trip (v16)

	static String cariPembelianTripByKunci(String skema) {
		return "SELECT id FROM " + skema + "sales_trip_pembelian"
				+ " WHERE idempotency_key = ? LIMIT 1";
	}

	/**
	 * DELAPAN parameter: tripId, pembelianId, supplierId, totalFaktur, dibayarTrip, tujuanStok,
	 * keterangan, idempotencyKey, oleh.
	 *
	 * <p>{@code sisa_hutang} legacy tidak punya kolom di sini: nilainya persis
	 * {@code total_faktur − dibayar_trip}, aritmetika dua kolom sebaris. Diturunkan saat dibaca
	 * lewat {@link #sisaHutangPembelianTrip()}.</p>
	 */
	static String sisipPembelianTrip(String skema) {
		return "INSERT INTO " + skema + "sales_trip_pembelian (sales_trip_id, pembelian_id,"
				+ " supplier_id, total_faktur, dibayar_trip, tujuan_stok, keterangan,"
				+ " idempotency_key, dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), ?)";
	}

	/** Sisa hutang pembelian trip: diturunkan, tidak disimpan. */
	static String sisaHutangPembelianTrip() {
		return "(COALESCE(b.total_faktur,0) - COALESCE(b.dibayar_trip,0))";
	}

	/**
	 * Daftar pembelian satu trip. ENAM kolom, berurutan sama dengan keluaran JSON legacy:
	 * id, supplierNama, fakturId, totalFaktur, dibayarSesi, sisaHutang, tujuanStok.
	 *
	 * <p>Tujuh sebenarnya — {@code tujuanStok} kolom ketujuh; legacy menyusunnya dalam urutan
	 * yang sama.</p>
	 */
	static String daftarPembelianTrip(String skema) {
		return "SELECT b.id, COALESCE(s.nama,''), b.pembelian_id, COALESCE(b.total_faktur,0),"
				+ " COALESCE(b.dibayar_trip,0), " + sisaHutangPembelianTrip() + ","
				+ " COALESCE(b.tujuan_stok,'')"
				+ " FROM " + skema + "sales_trip_pembelian b"
				+ " LEFT JOIN " + skema + "supplier s ON b.supplier_id = s.id"
				+ " WHERE b.sales_trip_id = ? ORDER BY b.id ASC";
	}

	/** Keberadaan satu baris induk pada schema tenant; dipakai memvalidasi kaitan opsional. */
	static String adaBarisTenant(String skema, String tabel) {
		return "SELECT 1 FROM " + skema + tabel + " WHERE id = ?";
	}
	// ------------------------------------------------------------------ rincian trip (v16)

	/**
	 * <h4>Rincian sesi: satu layar yang menghimpun seluruh bundel</h4>
	 *
	 * <p>Aksi ini membaca hasil enam bundel sekaligus — buku kas (v12), nota bawaan (v13), kas
	 * fisik penutupan (v14), kategori biaya (v15), dan pembelian trip (v16) — di samping tabel
	 * yang sudah ada sejak awal. Ia aksi terakhir helper Trip justru karena itu.</p>
	 *
	 * <p>SEPULUH kolom: id, nomor, statusSesi, waktuMulai, waktuKembali, waktuTutup,
	 * catatanPenutupan, kasFisikAktual, selisihKas, suratPerintahSalesId.</p>
	 *
	 * <p>Empat medan penutupan datang dari {@code sales_trip_rekonsiliasi}; {@code LEFT JOIN}
	 * sebab sesi yang belum ditutup memang belum punya barisnya, dan legacy pun mengembalikan
	 * {@code null} untuk keduanya.</p>
	 */
	static String tripRinci(String skema) {
		return "SELECT t.id, COALESCE(t.nomor_dokumen,''), COALESCE(t.status,''),"
				+ " t.tanggal_berangkat, t.tanggal_kembali, r.disetujui_pada,"
				+ " COALESCE(r.keterangan,''), r.kas_fisik_aktual, r.selisih,"
				+ " t.surat_perintah_sales_id"
				+ " FROM " + skema + "sales_trip t"
				+ " LEFT JOIN " + skema + "sales_trip_rekonsiliasi r ON r.sales_trip_id = t.id"
				+ " WHERE t.id = ?";
	}

	/**
	 * Blok SPJ pada rincian sesi, berbentuk sama dengan {@code jsonSpj} legacy.
	 *
	 * <p>EMPAT BELAS kolom: id, nomor, status, tanggalBerangkat, tanggalMulaiAktual,
	 * tanggalKembaliAktual, rute, kendaraan, uangMuka, catatan, alasanBatal, salesId, salesNama,
	 * disetujuiOleh.</p>
	 *
	 * <p>Dua medan dikembalikan kosong dan itu dicatat, bukan disamarkan. {@code rute} tidak ada
	 * pada model tenant — {@code wilayah} bukan padanannya, sebab rute adalah urutan kunjungan
	 * sedangkan wilayah adalah cakupan. {@code disetujuiOleh} juga tidak ada; SPJ tenant mencatat
	 * statusnya berubah menjadi APPROVED tetapi tidak menyimpan siapa yang menyetujui.</p>
	 *
	 * <p>{@code kendaraan} dan kedua tanggal aktual melekat pada <b>tripnya</b>, bukan SPJ —
	 * itulah sebabnya kueri ini menerima id trip, bukan id SPJ.</p>
	 */
	static String spjRinciUntukTrip(String skema) {
		return "SELECT j.id, COALESCE(j.nomor_dokumen,''), COALESCE(j.status,''), j.tanggal,"
				+ " t.tanggal_berangkat, t.tanggal_kembali, '', COALESCE(t.kendaraan,''),"
				+ " COALESCE(j.uang_muka_operasional,0), COALESCE(j.keterangan,''),"
				+ " COALESCE(j.alasan_batal,''), j.salesperson_id, COALESCE(s.nama,''), ''"
				+ " FROM " + skema + "sales_trip t"
				+ " JOIN " + skema + "surat_perintah_sales j"
				+ " ON t.surat_perintah_sales_id = j.id"
				+ " LEFT JOIN " + skema + "salesperson s ON j.salesperson_id = s.id"
				+ " WHERE t.id = ?";
	}

	/**
	 * Barang pada blok SPJ. DUA BELAS kolom: id, produkId, namaProduk, qtyRencana, qtyDimuat,
	 * qtyTerjual, qtyKembali, qtyRusak, qtyHilang, masihDibawa, hargaJual, status.
	 *
	 * <p>Legacy menyimpan rencana dan hasil pada satu baris; model tenant memecahnya menjadi
	 * tiga tabel. Rencananya di {@code surat_perintah_sales_detail}, yang dibawa di
	 * {@code sales_trip_barang}, hasilnya di {@code sales_trip_hasil} — dan kueri ini menyatukan
	 * ketiganya kembali menjadi bentuk yang dikenali klien.</p>
	 *
	 * <p>Basisnya rencana SPJ dengan {@code LEFT JOIN} ke dua sisanya: barang yang direncanakan
	 * tetapi tidak jadi dimuat harus tetap muncul dengan qtyDimuat nol, bukan menghilang.</p>
	 *
	 * <p>{@code status} per baris barang dikembalikan kosong: model tenant tidak menyimpannya,
	 * sebab kefinalan barang sudah dinyatakan status tripnya — lihat catatan v14.</p>
	 */
	static String barangSpjRinci(String skema) {
		String masih = "(COALESCE(b.kuantitas_bawa,0) - COALESCE(h.kuantitas_terjual,0)"
				+ " - COALESCE(h.kuantitas_kembali,0) - COALESCE(h.kuantitas_rusak,0)"
				+ " - COALESCE(h.selisih,0))";
		return "SELECT COALESCE(b.id, d.id), d.produk_id, COALESCE(pr.nama,''),"
				+ " COALESCE(d.kuantitas,0), COALESCE(b.kuantitas_bawa,0),"
				+ " COALESCE(h.kuantitas_terjual,0), COALESCE(h.kuantitas_kembali,0),"
				+ " COALESCE(h.kuantitas_rusak,0), COALESCE(h.selisih,0), " + masih + ","
				+ " COALESCE(b.harga_satuan,0), ''"
				+ " FROM " + skema + "surat_perintah_sales_detail d"
				+ " JOIN " + skema + "produk pr ON d.produk_id = pr.id"
				+ " LEFT JOIN " + skema + "sales_trip_barang b"
				+ " ON b.sales_trip_id = ? AND b.produk_id = d.produk_id"
				+ " LEFT JOIN " + skema + "sales_trip_hasil h"
				+ " ON h.sales_trip_id = ? AND h.produk_id = d.produk_id"
				+ " WHERE d.surat_perintah_sales_id = ? ORDER BY d.id ASC";
	}

	/**
	 * Biaya sesi. DELAPAN kolom: id, kategori, tanggal, uraian, nilai, metode, penerima,
	 * statusDok.
	 *
	 * <p>{@code kategori} memakai {@code COALESCE(k.nama, b.kategori)}: baris baru menjawab
	 * lewat masternya (v15), baris hasil impor legacy lewat teksnya.</p>
	 */
	static String daftarBiayaTrip(String skema) {
		return "SELECT b.id, COALESCE(k.nama, b.kategori, ''), b.tanggal,"
				+ " COALESCE(b.keterangan,''), COALESCE(b.nilai,0), COALESCE(b.cara_bayar,''),"
				+ " COALESCE(b.penerima,''), COALESCE(b.status,'AKTIF')"
				+ " FROM " + skema + "sales_trip_biaya b"
				+ " LEFT JOIN " + skema + "kategori_biaya_sales k ON b.kategori_biaya_id = k.id"
				+ " WHERE b.sales_trip_id = ? ORDER BY b.id ASC";
	}

	/** Buku kas sesi. ENAM kolom: id, jenis, nominal, referensi, keterangan, waktu. */
	static String daftarKasTrip(String skema) {
		return "SELECT k.id, COALESCE(k.jenis,''), COALESCE(k.nominal,0),"
				+ " COALESCE(k.referensi,''), COALESCE(k.keterangan,''), k.waktu"
				+ " FROM " + skema + "sales_trip_kas k"
				+ " WHERE k.sales_trip_id = ? ORDER BY k.id ASC";
	}

	/**
	 * Nota piutang yang dibawa satu SPJ. DELAPAN kolom: id, piutangId, nomorFaktur, jatuhTempo,
	 * saldoSaatAssign, nilaiTertagih (turunan), status, customerNama.
	 *
	 * <p>Dipakai blok {@code nota} pada rincian SPJ, yang sebelum bundel v13 selalu dikembalikan
	 * kosong sebab tabelnya memang belum ada.</p>
	 */
	static String daftarNotaSpj(String skema) {
		return "SELECT n.id, n.piutang_customer_id, COALESCE(d.nomor_faktur,''), d.jatuh_tempo,"
				+ " COALESCE(n.saldo_saat_assign,0), " + nilaiTertagihNota(skema, "n") + ","
				+ " COALESCE(n.status,''), COALESCE(c.nama,'')"
				+ " FROM " + skema + "surat_perintah_sales_nota n"
				+ " JOIN " + skema + "piutang_customer d ON n.piutang_customer_id = d.id"
				+ " LEFT JOIN " + skema + "customer c ON d.customer_id = c.id"
				+ " WHERE n.surat_perintah_sales_id = ? ORDER BY n.id ASC";
	}
}
