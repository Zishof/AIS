package ais.service.tenant;

import ais.common.security.PasswordHashService;

/**
 * <h3>Katalog migrasi KANONIK schema per-tenant (mode HYBRID/TENANT_ONLY; §10.2).</h3>
 *
 * <p>Provisioning production TIDAK memakai {@code hbm2ddl=update} utk schema tenant -- setiap
 * versi migrasi didefinisikan eksplisit di sini (DDL kanonik ber-checksum SHA-256), dieksekusi
 * {@link TenantSchemaService#terapkanMigrasi} dgn riwayat per-schema pada tabel
 * {@code <schema>.tenant_schema_migration}: versi yang sudah tercatat dgn checksum SAMA dilewati
 * (idempoten); checksum BEDA = korupsi definisi → gagal keras (tidak menimpa diam-diam).</p>
 *
 * <p>Placeholder {@code {S}} = schema ERP tenant; {@code {A}} = schema audit
 * ({@code <slug>__audit}). Identifier disubstitusi SETELAH lolos whitelist
 * {@link TenantSchemaService#pastikanAman} -- tidak pernah dari request (invariant #3).</p>
 *
 * <p><b>v1-core-pos</b>: baseline data-plane inti eBisnis per-tenant (brand, toko, pedagang/mesin
 * POS) -- cermin struktur entity existing ({@code public.brand}, {@code koperasi.toko},
 * {@code koperasi.pedagang}) dgn FK tenant-lokal. Audit: {@code revinfo} + tabel mirror
 * (pola Envers rev/revtype) di schema audit. Aturan menambah versi: TAMBAH entri baru di akhir
 * array (JANGAN mengubah DDL versi lama yang sudah dirilis -- checksum akan menolak).</p>
 *
 * <p><b>v2 ke atas</b> tinggal di kelas tersendiri ({@code TenantSchemaMigrationsV2} dst.)
 * supaya berkas ini tetap terbaca; DDL-nya tetap kanonik dan tetap ber-checksum sama.</p>
 */
public final class TenantSchemaMigrations {

	private TenantSchemaMigrations() {
	}

	/** Target eksekusi migrasi. */
	public static final String TARGET_ERP = "ERP";
	public static final String TARGET_AUDIT = "AUDIT";

	/**
	 * Tipe implementasi bersarang {@link Migrasi} milik {@link TenantSchemaMigrations}. Kelas ini memberi nama
	 * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * TenantSchemaMigrations}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan
	 * diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String versionCode}, {@code String
	 * target}, {@code String ddl}; operasi lokal: {@code checksum}(). Aturan bisnis bersama tetap berada pada
	 * kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see TenantSchemaMigrations
	 */
	public static final class Migrasi {
		public final String versionCode;
		public final String target;
		public final String[] ddl;
		Migrasi(String versionCode, String target, String[] ddl) {
			this.versionCode = versionCode;
			this.target = target;
			this.ddl = ddl;
		}

		/** Checksum SHA-256 atas seluruh teks DDL kanonik (sebelum substitusi identifier). */
		public String checksum() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ddl.length; i++) {
				sb.append(ddl[i]).append('\n');
			}
			return PasswordHashService.sha256Hex(sb.toString());
		}
	}

	private static final String[] DDL_V1_ERP = {
			"CREATE TABLE IF NOT EXISTS {S}.brand (" +
					"id bigserial PRIMARY KEY, " +
					"nama varchar(255) NOT NULL, " +
					"aktif boolean DEFAULT true, " +
					"dibuat_pada timestamp, " +
					"tanggal_dirubah timestamp, " +
					"oleh varchar(255), olehid varchar(255))",
			"CREATE TABLE IF NOT EXISTS {S}.toko (" +
					"id bigserial PRIMARY KEY, " +
					"nama varchar(255) NOT NULL, " +
					"kode varchar(100), " +
					"brand_id bigint REFERENCES {S}.brand(id), " +
					"alamat text, kota varchar(100), kode_pos varchar(20), telp varchar(50), " +
					"email varchar(255), npwp varchar(50), jam_operasional varchar(255), " +
					"aktif boolean DEFAULT true, " +
					"dibuat_pada timestamp, tanggal_dirubah timestamp, " +
					"oleh varchar(255), olehid varchar(255))",
			"CREATE TABLE IF NOT EXISTS {S}.pedagang (" +
					"id bigserial PRIMARY KEY, " +
					"userid varchar(100) NOT NULL, " +
					"pass varchar(100) NOT NULL, " +
					"nama varchar(255), " +
					"toko_id bigint REFERENCES {S}.toko(id), " +
					"supervisor boolean DEFAULT false, " +
					"aktif boolean DEFAULT true, " +
					"tanggal_dirubah timestamp, " +
					"oleh varchar(255), olehid varchar(255), " +
					"CONSTRAINT uq_{SU}_pedagang_userid UNIQUE (userid))",
	};

	private static final String[] DDL_V1_AUDIT = {
			"CREATE TABLE IF NOT EXISTS {A}.revinfo (" +
					"rev bigserial PRIMARY KEY, " +
					"revtstmp bigint)",
			"CREATE TABLE IF NOT EXISTS {A}.brand (" +
					"id bigint NOT NULL, rev bigint NOT NULL REFERENCES {A}.revinfo(rev), " +
					"revtype smallint, " +
					"nama varchar(255), aktif boolean, dibuat_pada timestamp, " +
					"oleh varchar(255), olehid varchar(255), " +
					"PRIMARY KEY (id, rev))",
			"CREATE TABLE IF NOT EXISTS {A}.toko (" +
					"id bigint NOT NULL, rev bigint NOT NULL REFERENCES {A}.revinfo(rev), " +
					"revtype smallint, " +
					"nama varchar(255), kode varchar(100), brand_id bigint, alamat text, " +
					"kota varchar(100), kode_pos varchar(20), telp varchar(50), email varchar(255), " +
					"npwp varchar(50), jam_operasional varchar(255), aktif boolean, " +
					"dibuat_pada timestamp, oleh varchar(255), olehid varchar(255), " +
					"PRIMARY KEY (id, rev))",
			"CREATE TABLE IF NOT EXISTS {A}.pedagang (" +
					"id bigint NOT NULL, rev bigint NOT NULL REFERENCES {A}.revinfo(rev), " +
					"revtype smallint, " +
					"userid varchar(100), nama varchar(255), toko_id bigint, supervisor boolean, " +
					"aktif boolean, oleh varchar(255), olehid varchar(255), " +
					"PRIMARY KEY (id, rev))",
	};

	/** Seluruh migrasi terurut (versi baru SELALU ditambahkan di akhir). */
	public static final Migrasi[] SEMUA = {
			new Migrasi("v1-core-pos-erp", TARGET_ERP, DDL_V1_ERP),
			new Migrasi("v1-core-pos-audit", TARGET_AUDIT, DDL_V1_AUDIT),
			new Migrasi("v2-inventory-master-erp", TARGET_ERP, TenantSchemaMigrationsV2.ERP),
			new Migrasi("v2-inventory-master-audit", TARGET_AUDIT, TenantSchemaMigrationsV2.AUDIT),
			new Migrasi("v3-inventory-stock-erp", TARGET_ERP, TenantSchemaMigrationsV3.ERP),
			new Migrasi("v4-inventory-purchase-ap-erp", TARGET_ERP, TenantSchemaMigrationsV4.ERP),
			new Migrasi("v5-inventory-sales-ar-erp", TARGET_ERP, TenantSchemaMigrationsV5.ERP),
			new Migrasi("v6-inventory-trip-erp", TARGET_ERP, TenantSchemaMigrationsV6.ERP),
			new Migrasi("v7-inventory-accounting-erp", TARGET_ERP, TenantSchemaMigrationsV7.ERP),
			new Migrasi("v8-inventory-import-erp", TARGET_ERP, TenantSchemaMigrationsV8.ERP),
			new Migrasi("v9-pos-ebisnis-erp", TARGET_ERP, TenantSchemaMigrationsV9.ERP),
			new Migrasi("v10-celah-p4-erp", TARGET_ERP, TenantSchemaMigrationsV10.ERP),
			new Migrasi("v11-idempotensi-erp", TARGET_ERP, TenantSchemaMigrationsV11.ERP),
	};

	/** Versi schema efektif setelah seluruh migrasi terpasang (dicatat ke registry.schemaVersion). */
	public static final String VERSI_TERKINI = "v11-idempotensi";

	/** Tabel yang WAJIB ada pasca-migrasi (dipakai VERIFY_SCHEMA). */
	public static final String[] TABEL_WAJIB_ERP = {
			// v1
			"tenant_schema_migration", "brand", "toko", "pedagang",
			// v2 -- organisasi & akses
			"gudang", "lokasi_stok", "role_tenant", "pengguna_tenant", "user_role_tenant",
			"salesperson", "sales_assignment",
			// v2 -- supplier & customer
			"supplier", "supplier_profile", "supplier_bank_account",
			"customer", "customer_profile", "customer_bank_account",
			// v3 -- produk, stok, harga
			"satuan", "kategori_produk", "produk", "produk_batch",
			"mutasi_stok", "saldo_stok", "stok_opname", "stok_opname_detail",
			"harga_beli_supplier", "harga_jual_customer", "price_list", "price_list_detail",
			// v4 -- pembelian & hutang
			"pembelian", "pembelian_detail", "hutang_supplier",
			"pembayaran_hutang", "alokasi_pembayaran_hutang",
			// v5 -- penjualan & piutang
			"sales_order", "sales_order_detail", "faktur_penjualan", "faktur_penjualan_detail",
			"piutang_customer", "penerimaan_piutang", "alokasi_penerimaan_piutang",
			// v6 -- sales keliling
			"surat_perintah_sales", "surat_perintah_sales_detail", "sales_trip",
			"sales_trip_barang", "sales_trip_nota", "sales_trip_hasil", "sales_trip_biaya",
			"sales_trip_setoran", "sales_trip_rekonsiliasi",
			// v7 -- akuntansi tenant
			"akun", "periode_akuntansi", "jurnal", "jurnal_detail", "posting_log", "reversal_log",
			// v8 -- idempotensi, cetak, impor legacy
			"idempotency_record", "print_log", "legacy_import_run", "legacy_import_file",
			"legacy_import_row", "legacy_key_map", "legacy_import_exception", "legacy_reconciliation",
			// v9 -- tabel POS eBisnis yang tidak ada di arsip FoxPro
			"cara_pembayaran", "jenis_customer", "customer_anggota_profile", "sesi_kas_kasir",
			"draft_penjualan", "draft_penjualan_detail", "retur_penjualan",
			"pemakaian_bahan_baku", "survey_kepuasan", "transaksi_backup_ack", "foto_produk",
	};
	/** Tabel audit yang WAJIB ada pasca-migrasi (dipakai VERIFY_SCHEMA). */
	public static final String[] TABEL_WAJIB_AUDIT = {
			// v1 -- cermin kolom, dipertahankan (TenantDataPlaneService masih menulis ke sana)
			"revinfo", "brand", "toko", "pedagang",
			// v2 -- catatan baris generik
			"audit_baris",
	};
}
