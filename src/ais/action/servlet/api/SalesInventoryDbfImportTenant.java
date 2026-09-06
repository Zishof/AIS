package ais.action.servlet.api;

import ais.service.tenant.TenantMutasiStok;

/**
 * <h3>Jalur schema tenant untuk impor DBF (P5, helper kesebelas).</h3>
 *
 * <p>Jalur legacy di {@link SalesInventoryDbfImportHelper} tidak diubah; kelas ini menyediakan
 * potongan SQL penggantinya. Seluruh penyimpanan jalur legacy memakai entitas Hibernate yang
 * <b>menyematkan schema-nya pada anotasi</b>, sehingga menjalankannya untuk pemakai tenant
 * membuat master miliknya mendarat di schema bersama sementara schema tenantnya tetap kosong —
 * impor melapor sukses, lalu layarnya tidak menampilkan apa pun. Karena itu jalur tenant menulis
 * lewat SQL asli.</p>
 *
 * <h4>Aturan yang dipertahankan: ISI BILA KOSONG</h4>
 * <p>Impor legacy tidak pernah menimpa nilai yang sudah terisi — ia hanya mengisi yang masih
 * kosong. Aturan itu bukan kebetulan: berkas DBF adalah cerminan data <b>lama</b>, dan
 * menjalankan ulang impor sesudah data dirapikan di layar akan mengembalikan ejaan lama, alamat
 * lama, dan nomor rekening lama. Semua {@code COALESCE(NULLIF(...))} di bawah menegakkan aturan
 * itu <b>di dalam SQL</b>, sehingga ia berlaku walau barisnya diproses berulang.</p>
 *
 * <h4>Yang berubah bentuk pada model tenant</h4>
 * <table border="1">
 * <tr><th>Legacy</th><th>Tenant</th></tr>
 * <tr><td>{@code library.penyedia} + {@code supplier_inventory_profile}</td>
 *     <td>{@code supplier} + {@code supplier_profile}</td></tr>
 * <tr><td>{@code anggota_koperasi} + {@code customer_inventory_profile}</td>
 *     <td>{@code customer} + {@code customer_profile}</td></tr>
 * <tr><td>{@code sales_inventory} (ber-toko)</td>
 *     <td>{@code salesperson} (se-tenant) + {@code sales_assignment} (yang ber-toko)</td></tr>
 * <tr><td>{@code produk.stok} + {@code stok_opname}</td>
 *     <td>{@code mutasi_stok} — stok adalah turunan, bukan kolom</td></tr>
 * </table>
 *
 * <h4>Saldo awal legacy masuk sebagai MUTASI, bukan kolom stok</h4>
 * <p>Jalur legacy menulis dua tempat sekaligus: satu baris {@code stok_opname} <i>dan</i>
 * {@code produk.stok}. Model tenant tidak punya kolom stok sama sekali — saldo mana pun adalah
 * penjumlahan {@code mutasi_stok}. Saldo awal karena itu menjadi <b>satu baris mutasi</b>
 * berjenis opname, dan tidak ada tempat kedua yang bisa berselisih dengannya.</p>
 */
final class SalesInventoryDbfImportTenant {

	private SalesInventoryDbfImportTenant() {
	}

	static boolean aktif(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.aktif(aktor);
	}

	static String skema(EbisnisActorContextResolver.ActorContext aktor) {
		return SalesInventoryTenantSchema.skema(aktor.tenant);
	}

	/**
	 * Jenis impor yang jalur tenantnya sudah ditulis.
	 *
	 * <p>Sejak §24 kedelapan jenisnya termasuk. Daftar ini tetap ada sebagai penjaga: jenis di
	 * luarnya ditolak dengan menyebut namanya, bukan dijatuhkan ke jalur legacy yang akan menulis
	 * ke schema bersama.</p>
	 */
	private static final String[] JENIS_DIDUKUNG = { "supplier", "customer", "sales", "produk",
			"harga_beli", "harga_jual", "pembelian_legacy", "penjualan_legacy",
			// Ditambahkan setelah perbandingan UAT cmnmedika menemukan empat kumpulan data
			// legacy yang tidak punya jalur impor sama sekali, sehingga layar terkaitnya kosong
			// sementara aplikasi lama menampilkannya:
			//   opname         <- dataopn.dbf   -- tanpa ini saldo stok tidak akan pernah cocok,
			//                                      sebab legacy menyesuaikan stok lewat opname
			//                                      fisik yang tidak terekam di BELI/JUAL
			//   piutang_legacy <- Tran_Piut.DBF
			//   hutang_legacy  <- Tran_Hut.DBF
			//   akun_legacy    <- account.dbf   -- ke {S}.akun, sesuai keputusan akuntansi
			//                                      se-tenant
			"opname", "piutang_legacy", "hutang_legacy", "akun_legacy",
			// Turunan, bukan data baru: membangun ulang saldo_stok dari mutasi_stok.
			// Tanpa langkah ini layar Persediaan kosong bagi SETIAP pengguna bertoko,
			// sebab penyaring toko menuntut adanya baris saldo_stok.
			"saldo_stok" };

	static boolean jenisDidukung(String jenis) {
		for (int i = 0; i < JENIS_DIDUKUNG.length; i++) {
			if (JENIS_DIDUKUNG[i].equals(jenis)) {
				return true;
			}
		}
		return false;
	}

	static String daftarJenisDidukung() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < JENIS_DIDUKUNG.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(JENIS_DIDUKUNG[i]);
		}
		return sb.toString();
	}

	// ------------------------------------------------------------------ pencarian

	/** SATU kolom: id baris berkode itu, atau kosong. {@code tabel} selalu literal dari kode. */
	static String cariKode(String skema, String tabel) {
		return "SELECT id FROM " + skema + tabel + " WHERE LOWER(kode) = LOWER(?) LIMIT 1";
	}

	/** SATU kolom: id satuan bernama itu (tanpa membedakan huruf besar/kecil), atau kosong. */
	static String cariSatuan(String skema) {
		return "SELECT id FROM " + skema + "satuan WHERE LOWER(nama) = LOWER(?) LIMIT 1";
	}

	/** Satuan baru; kodenya diambil dari namanya sebab DBF tidak membawa kode satuan. */
	static String sisipSatuan(String skema) {
		return "INSERT INTO " + skema + "satuan (kode, nama, aktif, dibuat_pada, oleh)"
				+ " VALUES (?, ?, true, now(), ?) RETURNING id";
	}

	// ------------------------------------------------------------------ supplier & customer

	/** TIGA parameter: kode, nama, oleh. */
	static String sisipMitra(String skema, String tabel) {
		return "INSERT INTO " + skema + tabel + " (kode, nama, aktif, dibuat_pada, oleh)"
				+ " VALUES (?, ?, true, now(), ?) RETURNING id";
	}

	/**
	 * Isi nama HANYA bila masih kosong. DUA parameter: nama, id.
	 *
	 * <p>Mengembalikan jumlah baris tersentuh, sehingga pemanggil dapat membedakan "diperbarui"
	 * dari "dilewati" tanpa membaca ulang barisnya.</p>
	 */
	static String isiNamaMitra(String skema, String tabel) {
		return "UPDATE " + skema + tabel + " SET nama = ?, tanggal_dirubah = now()"
				+ " WHERE id = ? AND (nama IS NULL OR TRIM(nama) = '')";
	}

	/**
	 * Profil supplier: sisip bila belum ada, lalu isi yang masih kosong. Pola sisip-bersyarat
	 * dipakai alih-alih {@code ON CONFLICT} karena lapisan tenant tetap aman-9.3.
	 *
	 * <p>ENAM parameter: supplierId, alamat1, kota, telp, syaratBayarHari, oleh (+ supplierId
	 * sekali lagi untuk penjaganya).</p>
	 *
	 * <p><b>Empat medan legacy tidak punya rumah di sini</b> — {@code wilayah},
	 * {@code rekening}, {@code atas_nama}, dan {@code bank}: {@code supplier_profile} model tenant
	 * memuat {@code kontak}, {@code npwp}, dan {@code fax}, bukan keempatnya. Medan itu
	 * <b>dilewati</b>, dan pemanggil melaporkannya sebagai peringatan alih-alih membuangnya
	 * diam-diam — impor yang menelan kolom tanpa berkata apa-apa adalah impor yang
	 * datanya hilang tanpa jejak.</p>
	 */
	static String sisipProfilSupplier(String skema) {
		return "INSERT INTO " + skema + "supplier_profile (supplier_id, alamat1, kota, telp,"
				+ " syarat_bayar_hari, dibuat_pada, oleh)"
				+ " SELECT ?, ?, ?, ?, ?, now(), ? WHERE NOT EXISTS (SELECT 1 FROM " + skema
				+ "supplier_profile p WHERE p.supplier_id = ?)";
	}

	/** LIMA parameter: alamat1, kota, telp, syaratBayarHari, supplierId. */
	static String isiProfilSupplier(String skema) {
		return "UPDATE " + skema + "supplier_profile SET"
				+ " alamat1 = COALESCE(NULLIF(TRIM(alamat1),''), ?),"
				+ " kota = COALESCE(NULLIF(TRIM(kota),''), ?),"
				+ " telp = COALESCE(NULLIF(TRIM(telp),''), ?),"
				+ " syarat_bayar_hari = CASE WHEN COALESCE(syarat_bayar_hari,0) = 0"
				+ " THEN ? ELSE syarat_bayar_hari END,"
				+ " tanggal_dirubah = now() WHERE supplier_id = ?";
	}

	/**
	 * DELAPAN parameter: customerId, alamat1, kota, telp, atasNama, syaratBayarHari, diskon,
	 * oleh (+ customerId sekali lagi untuk penjaganya).
	 *
	 * <p>Termin legacy bernama {@code syarat_bayar_hari} di sini — satu-satunya perbedaan
	 * namanya, bukan artinya. {@code wilayah}, {@code rekening}, dan {@code bank} tidak punya
	 * rumah pada {@code customer_profile} tenant dan dilaporkan sebagai peringatan.</p>
	 */
	static String sisipProfilCustomer(String skema) {
		return "INSERT INTO " + skema + "customer_profile (customer_id, alamat1, kota, telp,"
				+ " atas_nama, syarat_bayar_hari, diskon, dibuat_pada, oleh)"
				+ " SELECT ?, ?, ?, ?, ?, ?, ?, now(), ? WHERE NOT EXISTS (SELECT 1 FROM " + skema
				+ "customer_profile p WHERE p.customer_id = ?)";
	}

	/** TUJUH parameter: alamat1, kota, telp, atasNama, syaratBayarHari, diskon, customerId. */
	static String isiProfilCustomer(String skema) {
		return "UPDATE " + skema + "customer_profile SET"
				+ " alamat1 = COALESCE(NULLIF(TRIM(alamat1),''), ?),"
				+ " kota = COALESCE(NULLIF(TRIM(kota),''), ?),"
				+ " telp = COALESCE(NULLIF(TRIM(telp),''), ?),"
				+ " atas_nama = COALESCE(NULLIF(TRIM(atas_nama),''), ?),"
				+ " syarat_bayar_hari = CASE WHEN COALESCE(syarat_bayar_hari,0) = 0"
				+ " THEN ? ELSE syarat_bayar_hari END,"
				+ " diskon = CASE WHEN COALESCE(diskon,0) = 0 THEN ? ELSE diskon END,"
				+ " tanggal_dirubah = now() WHERE customer_id = ?";
	}

	// ------------------------------------------------------------------ salesperson

	/** EMPAT parameter: kode, nama, akunPerkiraan, oleh. */
	static String sisipSales(String skema) {
		return "INSERT INTO " + skema + "salesperson (kode, nama, akun_perkiraan, aktif,"
				+ " dibuat_pada, oleh) VALUES (?, ?, ?, true, now(), ?) RETURNING id";
	}

	/** TIGA parameter: nama, akunPerkiraan, id. */
	static String isiSales(String skema) {
		return "UPDATE " + skema + "salesperson SET"
				+ " nama = COALESCE(NULLIF(TRIM(nama),''), ?),"
				+ " akun_perkiraan = COALESCE(NULLIF(TRIM(akun_perkiraan),''), ?),"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/**
	 * Penugasan sales ke toko. Pada model tenant salesperson berlaku SE-TENANT dan yang
	 * mengikatnya ke toko adalah {@code sales_assignment} — itulah sebabnya impor tidak menaruh
	 * {@code toko_id} pada salespersonnya. TIGA parameter: salespersonId, tokoId, oleh
	 * (+ dua untuk penjaga keberadaannya).
	 */
	static String sisipPenugasan(String skema) {
		return "INSERT INTO " + skema + "sales_assignment (salesperson_id, toko_id, aktif,"
				+ " dibuat_pada, oleh) SELECT ?, ?, true, now(), ? WHERE NOT EXISTS ("
				+ "SELECT 1 FROM " + skema + "sales_assignment a WHERE a.salesperson_id = ?"
				+ " AND a.toko_id = ?)";
	}

	// ------------------------------------------------------------------ produk

	/**
	 * TUJUH parameter: kode, nama, satuanId, hargaJualStandar, hargaBeliTerakhir, stokMinimum,
	 * oleh.
	 *
	 * <p><b>Tanpa toko.</b> {@code produk} model tenant berlaku SE-TENANT — yang menjadi milik
	 * satu toko adalah gudangnya (lihat §16). Pencarian produk karena itu cukup lewat kodenya,
	 * dan dua toko tidak lagi dapat memiliki dua produk berkode sama yang sebenarnya barang yang
	 * sama.</p>
	 */
	static String sisipProduk(String skema) {
		return "INSERT INTO " + skema + "produk (kode, nama, satuan_id, harga_jual_standar,"
				+ " harga_jual_tunai, harga_beli_terakhir, stok_minimum, status, aktif,"
				+ " dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, 'AKTIF', true, now(), ?) RETURNING id";
	}

	/**
	 * TUJUH parameter: nama, satuanId, hargaJual, hargaJualTunai, hargaBeli, stokMinimum, id.
	 *
	 * <p>{@code harga_jual_tunai} memakai penjaga {@code IS NULL}, bukan {@code = 0} seperti
	 * tetangganya. Bedanya berarti: nol adalah harga tunai yang SAH (produk yang tidak dijual
	 * tunai dicatat legacy sebagai 0), sedangkan NULL berarti "belum pernah diisi". Memakai
	 * {@code = 0} akan menimpa nol yang disengaja setiap kali impor diulang.</p>
	 */
	static String isiProduk(String skema) {
		return "UPDATE " + skema + "produk SET"
				+ " nama = COALESCE(NULLIF(TRIM(nama),''), ?),"
				+ " satuan_id = COALESCE(satuan_id, ?),"
				+ " harga_jual_standar = CASE WHEN COALESCE(harga_jual_standar,0) = 0"
				+ " THEN ? ELSE harga_jual_standar END,"
				+ " harga_jual_tunai = COALESCE(harga_jual_tunai, ?),"
				+ " harga_beli_terakhir = CASE WHEN COALESCE(harga_beli_terakhir,0) = 0"
				+ " THEN ? ELSE harga_beli_terakhir END,"
				+ " stok_minimum = CASE WHEN COALESCE(stok_minimum,0) = 0"
				+ " THEN ? ELSE stok_minimum END,"
				+ " tanggal_dirubah = now() WHERE id = ?";
	}

	/**
	 * Saldo awal legacy sebagai satu baris {@code mutasi_stok} berjenis opname.
	 *
	 * <p>EMPAT parameter: produkId, gudangId, kuantitas, oleh. Arahnya selalu MASUK dan
	 * kuantitasnya positif — saldo pembuka yang negatif tidak punya arti fisik, dan pemanggil
	 * sudah menolaknya lebih dulu.</p>
	 *
	 * <p>Penjaganya {@code WHERE NOT EXISTS}: menjalankan ulang berkas DBF yang sama tidak boleh
	 * melipatgandakan saldo pembukanya. Inilah satu-satunya baris impor yang menambah
	 * <b>kuantitas</b>, sehingga pengulangan di sini berakibat langsung pada angka stok.</p>
	 */
	static String sisipSaldoAwal(String skema) {
		return "INSERT INTO " + skema + "mutasi_stok (produk_id, gudang_id, tanggal, jenis, arah,"
				+ " kuantitas, nomor_dokumen, dibuat_pada, oleh)"
				+ " SELECT ?, ?, CURRENT_DATE, '" + TenantMutasiStok.OPNAME + "', "
				+ TenantMutasiStok.MASUK + ", ?, 'MIGRASI-DBF', now(), ?"
				+ " WHERE NOT EXISTS (SELECT 1 FROM " + skema + "mutasi_stok m"
				+ " WHERE m.produk_id = ? AND m.nomor_dokumen = 'MIGRASI-DBF')";
	}

	// ------------------------------------------------------------------ harga

	/**
	 * Harga beli per supplier. Idempoten pada (supplier, produk, berlaku_dari): berkas yang sama
	 * boleh diimpor ulang tanpa melahirkan versi harga kedua bertanggal sama.
	 * LIMA parameter: supplierId, produkId, tanggal, harga, oleh (+ tiga untuk penjaganya).
	 */
	static String sisipHargaBeli(String skema) {
		return "INSERT INTO " + skema + "harga_beli_supplier (supplier_id, produk_id,"
				+ " berlaku_dari, harga, aktif, dibuat_pada, oleh)"
				+ " SELECT ?, ?, ?, ?, true, now(), ?"
				+ " WHERE NOT EXISTS (SELECT 1 FROM " + skema + "harga_beli_supplier h"
				+ " WHERE h.supplier_id = ? AND h.produk_id = ? AND h.berlaku_dari = ?)";
	}

	/**
	 * Harga jual. {@code customer_id} boleh {@code NULL} — itulah harga umum, dan penjaganya
	 * memakai {@code IS NOT DISTINCT FROM} supaya baris ber-customer kosong pun tetap dikenali
	 * sebagai duplikat (perbandingan {@code =} biasa selalu tidak-diketahui terhadap NULL, dan
	 * penjaganya akan lolos setiap kali).
	 * LIMA parameter: customerId, produkId, tanggal, harga, oleh (+ tiga untuk penjaganya).
	 */
	static String sisipHargaJual(String skema) {
		return "INSERT INTO " + skema + "harga_jual_customer (customer_id, produk_id,"
				+ " berlaku_dari, harga, aktif, dibuat_pada, oleh)"
				+ " SELECT ?, ?, ?, ?, true, now(), ?"
				+ " WHERE NOT EXISTS (SELECT 1 FROM " + skema + "harga_jual_customer h"
				+ " WHERE h.customer_id IS NOT DISTINCT FROM ? AND h.produk_id = ?"
				+ " AND h.berlaku_dari = ?)";
	}

	/** SATU kolom: gudang pertama milik toko itu, untuk menempatkan saldo awalnya. */
	static String gudangToko(String skema) {
		return "SELECT id FROM " + skema + "gudang WHERE toko_id = ?"
				+ " AND COALESCE(aktif,true) = true ORDER BY id LIMIT 1";
	}

	// ------------------------------------------------------------------ riwayat transaksi DBF

	/**
	 * <h4>Riwayat BELI.DBF/JUAL.DBF menjadi MUTASI, bukan dokumen karangan</h4>
	 *
	 * <p>Godaan yang wajar: menerbitkan {@code pembelian} ber-kepala/detail dan
	 * {@code faktur_penjualan} supaya riwayat lama tampak seperti dokumen tenant biasa.
	 * <b>Itu akan mengarang data.</b> Baris DBF-nya tidak membawa supplier maupun customer sebagai
	 * relasi — hanya <i>teks kode</i> — dan tidak membawa termin, jatuh tempo, pajak, maupun
	 * status dokumen. Dokumen yang dibentuk dari situ akan punya kepala yang isinya tebakan, lalu
	 * ikut masuk ke umur piutang dan hutang seolah-olah tagihan sungguhan.</p>
	 *
	 * <p>Yang benar-benar dibawa berkas itu hanya satu hal: <b>barang berpindah, sekian banyak,
	 * pada tanggal sekian, seharga sekian</b>. Pada model tenant itu persis satu baris
	 * {@code mutasi_stok}. Jalur legacy pun tidak membuat dokumen — {@code PengadaanProduk} dan
	 * {@code Pembelian} sama-sama baris datar, bukan kepala/detail — sehingga pemetaan ini
	 * setara bentuknya, bukan penyederhanaan.</p>
	 *
	 * <p>Kode supplier/customer/sales dan nomor batch tetap disimpan pada {@code keterangan}:
	 * teksnya tidak dapat menjadi relasi, tetapi membuangnya berarti kehilangan satu-satunya
	 * petunjuk asal-usul baris itu.</p>
	 *
	 * <p>DELAPAN parameter: produkId, gudangId, tanggal, kuantitas, hargaSatuan, nilai,
	 * nomorDokumen, keterangan, idempotencyKey, oleh — dengan {@code jenis} dan {@code arah}
	 * ditentukan pemanggil lewat {@link #MUTASI_MASUK_PENGADAAN} atau
	 * {@link #MUTASI_KELUAR_PENJUALAN}.</p>
	 *
	 * <p>Idempotensinya bersandar pada indeks unik {@code idempotency_key} milik
	 * {@code mutasi_stok} (bundel v11), bukan pada {@code WHERE NOT EXISTS}: dua permintaan
	 * serentak yang membawa berkas sama tetap berakhir pada satu baris, bukan dua.</p>
	 */
	static String sisipMutasiRiwayat(String skema, boolean masuk) {
		return "INSERT INTO " + skema + "mutasi_stok (produk_id, gudang_id, tanggal, jenis, arah,"
				+ " kuantitas, harga_satuan, nilai, nomor_dokumen, keterangan, idempotency_key,"
				+ " dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, '"
				+ (masuk ? TenantMutasiStok.PENGADAAN : TenantMutasiStok.PENJUALAN) + "', "
				+ (masuk ? TenantMutasiStok.MASUK : TenantMutasiStok.KELUAR)
				+ ", ?, ?, ?, ?, ?, ?, now(), ?)";
	}

	/** Benar bila kunci idempotensi itu sudah dipakai — baris DBF yang sama pernah diimpor. */
	static String adaMutasiRiwayat(String skema) {
		return "SELECT 1 FROM " + skema + "mutasi_stok WHERE idempotency_key = ? LIMIT 1";
	}

	// ================================================================= DOKUMEN RIWAYAT

	/**
	 * Kepala dokumen pembelian/penjualan dari baris riwayat legacy.
	 *
	 * <h4>Mengapa ini bukan mengarang struktur</h4>
	 * <p>{@code BELI.DBF} dan {@code JUAL.DBF} tidak memuat rekaman header — tetapi memuat
	 * <b>informasinya</b>: nomor faktur, kode mitra, dan tanggal ada pada SETIAP baris. Yang
	 * dilakukan di sini normalisasi, bukan penciptaan: baris yang bersepakat pada ketiga medan itu
	 * memang satu faktur yang sama di aplikasi lama, dan layarnya menampilkannya begitu.</p>
	 *
	 * <p>Tanpa ini, layar yang mendaftar faktur pembelian/penjualan kosong sementara aplikasi lama
	 * menampilkan puluhan ribu baris — kartu stoknya cocok, daftar fakturnya tidak.</p>
	 *
	 * <p>Idempotensinya bersandar pada {@code nomor_dokumen}, yang diisi nomor faktur legacy apa
	 * adanya. Dua faktur berbeda bernomor sama pada mitra berbeda akan menyatu — dan itu memang
	 * yang terjadi di aplikasi lama, sebab nomor faktur di sanalah kunci yang dilihat pengguna.</p>
	 */
	static String sisipDokumenKepala(String skema, boolean pembelian) {
		String t = pembelian ? "pembelian" : "faktur_penjualan";
		String kolomMitra = pembelian ? "supplier_id" : "customer_id";
		return "INSERT INTO " + skema + t
				+ " (nomor_dokumen, nomor_faktur, tanggal, " + kolomMitra + ", gudang_id, toko_id,"
				+ "  subtotal, total, status, diposting, keterangan, dibuat_pada, oleh,"
				+ "  legacy_source_file, legacy_tafsir)"
				+ " SELECT ?, ?, ?, ?, ?, ?, 0, 0, 'SELESAI', true,"
				+ "        'Migrasi " + (pembelian ? "BELI.DBF" : "JUAL.DBF") + "', now(), ?, ?, ?"
				+ " WHERE NOT EXISTS (SELECT 1 FROM " + skema + t + " d WHERE d.nomor_dokumen = ?)";
	}

	static String cariDokumenKepala(String skema, boolean pembelian) {
		return "SELECT id FROM " + skema + (pembelian ? "pembelian" : "faktur_penjualan")
				+ " WHERE nomor_dokumen = ? LIMIT 1";
	}

	/**
	 * Rincian dokumen. {@code batch_no} dan {@code expiry_date} menempati kolomnya sendiri di sini
	 * — pada {@code mutasi_stok} keduanya hanya dapat dititipkan sebagai teks pada
	 * {@code keterangan}, sebab tabel itu tidak punya tempatnya.
	 *
	 * <p>Penjaganya (dokumen, baris_ke): nomor baris DBF sudah menjadi pembeda pada kunci mutasi,
	 * dan memakai pembeda yang sama di sini membuat kedua sisi konsisten — satu baris DBF
	 * menghasilkan tepat satu mutasi dan tepat satu rincian dokumen.</p>
	 */
	static String sisipDokumenRinci(String skema, boolean pembelian) {
		String t = pembelian ? "pembelian_detail" : "faktur_penjualan_detail";
		String induk = pembelian ? "pembelian_id" : "faktur_penjualan_id";
		return "INSERT INTO " + skema + t
				+ " (" + induk + ", baris_ke, produk_id, batch_no, expiry_date, kuantitas,"
				+ "  harga_satuan, total, dibuat_pada, oleh,"
				+ "  legacy_source_file, legacy_source_record_no)"
				+ " SELECT ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, ?, ?"
				+ " WHERE NOT EXISTS (SELECT 1 FROM " + skema + t + " x"
				+ " WHERE x." + induk + " = ? AND x.baris_ke = ?)";
	}

	/**
	 * Menambahkan nilai satu baris ke total kepalanya.
	 *
	 * <p>Total DIAKUMULASI, bukan dihitung ulang: baris satu faktur tersebar di beberapa bongkah
	 * permintaan, jadi saat baris pertama masuk totalnya belum dapat diketahui. Pemanggil hanya
	 * memanggilnya bila rincian benar-benar tersisip — sehingga kiriman ulang tidak menggelembungkan
	 * totalnya.</p>
	 */
	static String tambahTotalDokumen(String skema, boolean pembelian) {
		String t = pembelian ? "pembelian" : "faktur_penjualan";
		return "UPDATE " + skema + t + " SET subtotal = COALESCE(subtotal,0) + ?,"
				+ " total = COALESCE(total,0) + ? WHERE id = ?";
	}

	// ================================================================= OPNAME (dataopn.dbf)

	/**
	 * Kepala opname per tanggal. {@code dataopn.dbf} tidak punya nomor dokumen, jadi nomornya
	 * dibentuk dari tanggalnya — sekaligus menjadi penjaga idempotensinya.
	 */
	/**
	 * Membangun ulang {@code saldo_stok} dari {@code mutasi_stok}, per (produk, gudang).
	 *
	 * <p>Dijalankan sebagai langkah penutup impor. Isinya murni turunan, jadi aman diulang:
	 * baris lama dibuang lebih dulu, bukan ditambahkan.</p>
	 *
	 * <p>Produk yang saldonya NOL tetap dibuatkan barisnya. Itu disengaja: baris
	 * {@code saldo_stok} adalah pernyataan "gudang ini menangani produk ini", dan produk yang
	 * kebetulan habis tidak boleh lenyap dari layar persediaan — justru itu yang ingin dilihat.</p>
	 */
	static String[] bangunSaldoStok(String skema) {
		return new String[] {
			"DELETE FROM " + skema + "saldo_stok",
			"INSERT INTO " + skema + "saldo_stok"
					+ " (produk_id, gudang_id, kuantitas, nilai, dihitung_pada, dibuat_pada, oleh)"
					+ " SELECT m.produk_id, m.gudang_id,"
					+ " COALESCE(SUM(m.arah * m.kuantitas),0) AS kuantitas,"
					+ " COALESCE(SUM(m.arah * m.kuantitas),0)"
					+ " * COALESCE((SELECT p.harga_beli_terakhir FROM " + skema + "produk p"
					+ " WHERE p.id = m.produk_id),0) AS nilai,"
					+ " LOCALTIMESTAMP, LOCALTIMESTAMP, 'impor-legacy'"
					+ " FROM " + skema + "mutasi_stok m"
					+ " WHERE m.gudang_id IS NOT NULL"
					+ " GROUP BY m.produk_id, m.gudang_id"
		};
	}

	/** Gudang tunggal milik satu toko, atau tidak ada bila jumlahnya bukan satu. */
	static String gudangTunggalToko(String skema) {
		return "SELECT g.id FROM " + skema + "gudang g WHERE g.toko_id = ?"
				+ " AND COALESCE(g.aktif, true) = true";
	}

	/**
	 * Baris saldo NOL untuk produk yang belum pernah punya mutasi.
	 *
	 * <p>Tanpa ini, produk yang masuk lewat master tetapi tidak pernah dibeli maupun dijual
	 * tidak muncul di layar Persediaan bagi pengguna bertoko — pada data UAT cmnmedika 70 dari
	 * 626 produk. Aplikasi lama menampilkan seluruh 626.</p>
	 *
	 * <p>{@code gudangId} disambung sebagai literal karena sudah tervalidasi pemanggil sebagai
	 * hasil kueri, bukan masukan pengguna.</p>
	 */
	static String saldoNolProdukTanpaMutasi(String skema, long gudangId) {
		return "INSERT INTO " + skema + "saldo_stok"
				+ " (produk_id, gudang_id, kuantitas, nilai, dihitung_pada, dibuat_pada, oleh)"
				+ " SELECT p.id, " + gudangId + ", 0, 0, LOCALTIMESTAMP, LOCALTIMESTAMP, 'impor-legacy'"
				+ " FROM " + skema + "produk p"
				+ " WHERE NOT EXISTS (SELECT 1 FROM " + skema + "saldo_stok ss"
				+ " WHERE ss.produk_id = p.id)";
	}

	static String sisipOpnameKepala(String skema) {
		return "INSERT INTO " + skema + "stok_opname"
				+ " (nomor_dokumen, tanggal, gudang_id, status, keterangan, diposting,"
				+ "  dibuat_pada, oleh, legacy_source_file)"
				+ " SELECT ?, ?, ?, 'SELESAI', 'Migrasi dataopn.dbf', true, now(), ?, 'dataopn.dbf'"
				+ " WHERE NOT EXISTS (SELECT 1 FROM " + skema + "stok_opname o"
				+ " WHERE o.nomor_dokumen = ?)";
	}

	static String cariOpnameKepala(String skema) {
		return "SELECT id FROM " + skema + "stok_opname WHERE nomor_dokumen = ? LIMIT 1";
	}

	/**
	 * Mutasi penyesuaian opname — ber-{@code jenis} {@link TenantMutasiStok#OPNAME}, bukan
	 * menumpang PENGADAAN/PENJUALAN.
	 *
	 * <p>Penyesuaian opname memang menggerakkan saldo seperti pembelian atau penjualan, tetapi ia
	 * BUKAN keduanya. Menumpangkannya membuat laporan pembelian dan penjualan ikut membengkak, dan
	 * kartu stok menyebut koreksi fisik sebagai "PENGADAAN" — angka saldonya benar, tetapi
	 * ceritanya salah. Terukur saat pertama kali ditulis begitu: total PENGADAAN membengkak
	 * 211.554 unit dan PENJUALAN 194.728 unit di atas berkas sumbernya.</p>
	 *
	 * <p>{@code arah} ditentukan pemanggil dari tanda selisihnya: fisik lebih banyak = masuk.</p>
	 */
	static String sisipMutasiOpname(String skema, boolean masuk) {
		return "INSERT INTO " + skema + "mutasi_stok (produk_id, gudang_id, tanggal, jenis, arah,"
				+ " kuantitas, harga_satuan, nilai, nomor_dokumen, keterangan, idempotency_key,"
				+ " dibuat_pada, oleh)"
				+ " VALUES (?, ?, ?, '" + TenantMutasiStok.OPNAME + "', "
				+ (masuk ? TenantMutasiStok.MASUK : TenantMutasiStok.KELUAR)
				+ ", ?, ?, ?, ?, ?, ?, now(), ?)";
	}

	/**
	 * Rincian opname. {@code selisih} disimpan sebagai kolom tersendiri walau dapat diturunkan
	 * dari fisik-sistem: nilainya adalah potret saat opname, dan menghitungnya ulang di kemudian
	 * hari akan memakai angka sistem yang sudah berubah.
	 */
	static String sisipOpnameRinci(String skema) {
		return "INSERT INTO " + skema + "stok_opname_detail"
				+ " (stok_opname_id, produk_id, kuantitas_sistem, kuantitas_fisik, selisih,"
				+ "  harga_satuan, dibuat_pada, oleh, legacy_source_file, legacy_source_record_no)"
				+ " SELECT ?, ?, ?, ?, ?, ?, now(), ?, 'dataopn.dbf', ?"
				+ " WHERE NOT EXISTS (SELECT 1 FROM " + skema + "stok_opname_detail d"
				+ " WHERE d.stok_opname_id = ? AND d.produk_id = ?)";
	}

	// ================================================================= PIUTANG / HUTANG

	/**
	 * Piutang legacy. {@code terbayar}/{@code sisa} DISIMPAN, bukan diturunkan, karena sumbernya
	 * hanya menyatakan lunas atau belum lewat ada-tidaknya {@code TGLBAYAR} — tidak ada rincian
	 * pembayaran yang bisa dijumlahkan.
	 */
	static String sisipPiutangLegacy(String skema) {
		return "INSERT INTO " + skema + "piutang_customer"
				+ " (customer_id, salesperson_id, faktur_penjualan_id, nomor_faktur, nomor_retur,"
				+ "  tanggal, jatuh_tempo,"
				+ "  nilai, terbayar, sisa, status, dibuat_pada, oleh,"
				+ "  legacy_source_file, legacy_source_record_no, legacy_tafsir)"
				+ " SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, 'Tran_Piut.DBF', ?, ?"
				+ " WHERE NOT EXISTS (SELECT 1 FROM " + skema + "piutang_customer p"
				+ " WHERE p.customer_id = ? AND p.nomor_faktur = ? AND p.tanggal = ?)";
	}

	/**
	 * Mencari faktur penjualan yang bernomor dan bercustomer sama, untuk ditautkan pada piutang.
	 *
	 * <p><b>Mengapa penting.</b> {@code kolomTokoPiutang()} menyaring lewat
	 * {@code faktur_penjualan.toko_id}. Piutang yang tidak tertaut faktur karena itu punya
	 * {@code toko_id} bernilai NULL, dan saringan toko membuang seluruh barisnya — layar piutang
	 * tampak kosong padahal datanya ada. Terukur pada UAT cmnmedika: 108 baris di basis data,
	 * 0 baris di {@code si_receivable_list}.</p>
	 *
	 * <p>Penautan hanya mungkin sesudah dokumen penjualan terbentuk, jadi {@code piutang_legacy}
	 * WAJIB diimpor setelah {@code penjualan_legacy}.</p>
	 */
	static String cariFakturUntukPiutang(String skema) {
		return "SELECT id FROM " + skema + "faktur_penjualan"
				+ " WHERE nomor_dokumen = ? AND customer_id = ? LIMIT 1";
	}

	static String sisipHutangLegacy(String skema) {
		return "INSERT INTO " + skema + "hutang_supplier"
				+ " (supplier_id, nomor_faktur, tanggal, jatuh_tempo, nilai, terbayar, sisa,"
				+ "  status, dibuat_pada, oleh,"
				+ "  legacy_source_file, legacy_source_record_no, legacy_tafsir)"
				+ " SELECT ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, 'Tran_Hut.DBF', ?, ?"
				+ " WHERE NOT EXISTS (SELECT 1 FROM " + skema + "hutang_supplier h"
				+ " WHERE h.supplier_id = ? AND h.nomor_faktur = ? AND h.tanggal = ?)";
	}

	// ================================================================= AKUN (account.dbf)

	/**
	 * Bagan akun legacy. {@code account.dbf} memuat kode ganda (mis. 102 dua kali), jadi
	 * penjaganya bersandar pada kode saja — yang pertama masuk, sisanya dilewati. Itu sesuai
	 * batasan unik {@code uq_..._akun_kode} pada katalognya.
	 */
	static String sisipAkunLegacy(String skema) {
		return "INSERT INTO " + skema + "akun"
				+ " (kode, nama, tipe, saldo_normal, posting_diizinkan, aktif, dibuat_pada, oleh,"
				+ "  legacy_source_file, legacy_source_record_no)"
				+ " SELECT ?, ?, ?, ?, true, true, now(), ?, 'account.dbf', ?"
				+ " WHERE NOT EXISTS (SELECT 1 FROM " + skema + "akun a WHERE a.kode = ?)";
	}
}
