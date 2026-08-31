package ais.service.tenant;

/**
 * <h3>Migrasi tenant v2 — master organisasi, akses, supplier, dan customer (P3).</h3>
 *
 * <p>Bundel pertama Inventory &amp; Sales di atas baseline {@code v1-core-pos}
 * (brand/toko/pedagang). Ditambahkan, tidak mengubah v1 — checksum v1 harus tetap.</p>
 *
 * <h4>Audit: satu tabel generik, bukan enam puluh cermin kolom</h4>
 * <p>v1 memakai <b>cermin per-tabel</b> gaya Envers ({@code {A}.brand}, {@code {A}.toko},
 * {@code {A}.pedagang}) — satu tabel audit untuk satu tabel data, kolomnya disalin satu per
 * satu. Pola itu <b>tidak diteruskan</b> untuk enam puluhan tabel Inventory &amp; Sales.</p>
 *
 * <p>Alasannya dua. Pertama, setiap penambahan kolom bisnis menuntut ALTER kembar di sisi
 * audit; yang lupa tidak menimbulkan galat, hanya kolom yang diam-diam tidak pernah terekam.
 * Kedua, daftar medan wajib dokumen master §11.6 sendiri memuat {@code before/after} — bentuk
 * yang menunjuk catatan baris generik, bukan cermin kolom.</p>
 *
 * <p>Karena itu v2 menambah:</p>
 * <ul>
 * <li>{@code {A}.revinfo} diperluas dengan konteks aktor/permintaan §11.6 (satu baris per
 *     revisi: siapa, peran apa, dari perangkat mana, permintaan yang mana, alasannya apa);</li>
 * <li>{@code {A}.audit_baris} generik: {@code entity}, {@code entity_id}, {@code revtype},
 *     {@code sebelum}, {@code sesudah}.</li>
 * </ul>
 *
 * <p>Cermin v1 dibiarkan berdiri apa adanya — {@link TenantDataPlaneService} masih menulis ke
 * sana, dan menghapusnya akan melanggar append-only.</p>
 *
 * <p><b>Riwayat harga tidak bergantung audit.</b> {@code harga_beli_supplier} dan
 * {@code harga_jual_customer} berversi di schema ERP (v3), sehingga pertanyaan "berapa harga
 * barang ini pada tanggal X" dijawab tabel bisnis, bukan tabel audit. Itu yang membuat
 * kehilangan cermin kolom tidak menyakitkan.</p>
 *
 * <h4>Catatan PostgreSQL 9.3</h4>
 * <p>{@code CREATE INDEX IF NOT EXISTS} baru ada di 9.5, jadi tidak dipakai. Idempotensi
 * datang dari riwayat versi migrasi, dan DDL PostgreSQL transaksional — migrasi yang gagal
 * di tengah membatalkan seluruhnya termasuk baris riwayatnya. {@code jsonb} juga tidak
 * dipakai (9.4+); muatan {@code sebelum}/{@code sesudah} disimpan sebagai {@code text}.</p>
 */
public final class TenantSchemaMigrationsV2 {

	private TenantSchemaMigrationsV2() {
	}

	/** Kolom jejak impor legacy — dipakai berulang, disatukan supaya tidak berbeda-beda. */
	private static final String LEGACY =
			"legacy_source_file varchar(128), legacy_source_record_no integer, "
			+ "legacy_row_hash varchar(64), legacy_import_run_id bigint, "
			+ "legacy_deleted boolean DEFAULT false";

	/** Kolom jejak perubahan — sama dengan konvensi v1. */
	private static final String JEJAK =
			"dibuat_pada timestamp, tanggal_dirubah timestamp, "
			+ "oleh varchar(255), olehid varchar(255)";

	/**
	 * DDL kanonik v2 untuk schema ERP tenant, dieksekusi berurutan oleh
	 * {@link TenantSchemaService#terapkanMigrasi} sebagai bundel {@code v2-inventory-master-erp}.
	 * Empat kelompok tabel, dalam urutan yang menghormati dependensi FK (organisasi sebelum
	 * akses, akses sebelum sales, mitra di akhir karena merujuk {@code salesperson}):
	 * <ol>
	 * <li><b>Organisasi</b> -- {@code gudang} dan {@code lokasi_stok}, unit fisik penyimpanan
	 * di bawah {@code toko} (v1).</li>
	 * <li><b>Akses dalam tenant</b> -- {@code role_tenant} (lihat {@link TenantRoleSeeder}),
	 * {@code pengguna_tenant}, dan tabel pivot {@code user_role_tenant}.</li>
	 * <li><b>Sales</b> -- {@code salesperson} dan {@code sales_assignment} (wilayah/toko/gudang
	 * yang menjadi tanggung jawabnya).</li>
	 * <li><b>Supplier</b> dan <b>Customer</b> -- masing-masing tabel induk plus
	 * {@code _profile} dan {@code _bank_account} satu-ke-satu/satu-ke-banyak.</li>
	 * </ol>
	 * Setiap tabel legacy-import memakai kolom {@link #LEGACY} dan setiap tabel memakai kolom
	 * jejak {@link #JEJAK}; lihat javadoc kelas untuk aturan checksum (array ini TIDAK BOLEH
	 * diubah setelah dirilis -- tambahkan bundel versi baru untuk perubahan lanjutan).
	 */
	public static final String[] ERP = {
			// ---------- Organisasi ----------
			"CREATE TABLE {S}.gudang ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "alamat text, "
					+ "tipe varchar(32), "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_gudang_kode UNIQUE (kode))",
			"CREATE INDEX idx_{SU}_gudang_toko ON {S}.gudang (toko_id)",

			"CREATE TABLE {S}.lokasi_stok ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "gudang_id bigint NOT NULL REFERENCES {S}.gudang(id), "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_lokasi_stok_kode UNIQUE (gudang_id, kode))",
			"CREATE INDEX idx_{SU}_lokasi_stok_gudang ON {S}.lokasi_stok (gudang_id)",

			// ---------- Akses dalam tenant ----------
			"CREATE TABLE {S}.role_tenant ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "keterangan text, "
					+ "bawaan boolean DEFAULT false, "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_role_tenant_kode UNIQUE (kode))",

			"CREATE TABLE {S}.pengguna_tenant ("
					+ "id bigserial PRIMARY KEY, "
					+ "userid varchar(100) NOT NULL, "
					+ "nama varchar(255), "
					+ "email varchar(255), "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "gudang_id bigint REFERENCES {S}.gudang(id), "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_pengguna_tenant_userid UNIQUE (userid))",
			"CREATE INDEX idx_{SU}_pengguna_tenant_toko ON {S}.pengguna_tenant (toko_id)",

			"CREATE TABLE {S}.user_role_tenant ("
					+ "id bigserial PRIMARY KEY, "
					+ "pengguna_tenant_id bigint NOT NULL REFERENCES {S}.pengguna_tenant(id), "
					+ "role_tenant_id bigint NOT NULL REFERENCES {S}.role_tenant(id), "
					+ "berlaku_dari date, berlaku_sampai date, "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_user_role_tenant UNIQUE (pengguna_tenant_id, role_tenant_id))",

			// ---------- Sales ----------
			"CREATE TABLE {S}.salesperson ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "pengguna_tenant_id bigint REFERENCES {S}.pengguna_tenant(id), "
					+ "akun_perkiraan varchar(64), "
					+ "telp varchar(50), "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", " + LEGACY + ", "
					+ "CONSTRAINT uq_{SU}_salesperson_kode UNIQUE (kode))",
			"CREATE INDEX idx_{SU}_salesperson_aktif ON {S}.salesperson (aktif)",
			"CREATE INDEX idx_{SU}_salesperson_legacy ON {S}.salesperson "
					+ "(legacy_source_file, legacy_source_record_no)",

			"CREATE TABLE {S}.sales_assignment ("
					+ "id bigserial PRIMARY KEY, "
					+ "salesperson_id bigint NOT NULL REFERENCES {S}.salesperson(id), "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "gudang_id bigint REFERENCES {S}.gudang(id), "
					+ "wilayah varchar(255), "
					+ "berlaku_dari date, berlaku_sampai date, "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_sales_assignment_sales ON {S}.sales_assignment (salesperson_id)",

			// ---------- Supplier ----------
			"CREATE TABLE {S}.supplier ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "status varchar(32) DEFAULT 'AKTIF', "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", " + LEGACY + ", "
					+ "CONSTRAINT uq_{SU}_supplier_kode UNIQUE (kode))",
			"CREATE INDEX idx_{SU}_supplier_nama ON {S}.supplier (nama)",
			"CREATE INDEX idx_{SU}_supplier_status ON {S}.supplier (status)",
			"CREATE INDEX idx_{SU}_supplier_legacy ON {S}.supplier "
					+ "(legacy_source_file, legacy_source_record_no)",
			"CREATE INDEX idx_{SU}_supplier_rowhash ON {S}.supplier (legacy_row_hash)",

			"CREATE TABLE {S}.supplier_profile ("
					+ "id bigserial PRIMARY KEY, "
					+ "supplier_id bigint NOT NULL REFERENCES {S}.supplier(id), "
					+ "alamat1 text, alamat2 text, kota varchar(100), kode_pos varchar(20), "
					+ "telp varchar(50), fax varchar(50), email varchar(255), "
					+ "kontak varchar(255), npwp varchar(50), "
					+ "syarat_bayar_hari integer, "
					+ "diskon numeric(9,4), "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_supplier_profile UNIQUE (supplier_id))",

			"CREATE TABLE {S}.supplier_bank_account ("
					+ "id bigserial PRIMARY KEY, "
					+ "supplier_id bigint NOT NULL REFERENCES {S}.supplier(id), "
					+ "nama_bank varchar(255), "
					+ "nomor_rekening varchar(64), "
					+ "atas_nama varchar(255), "
					+ "utama boolean DEFAULT false, "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_supplier_bank_supplier ON {S}.supplier_bank_account (supplier_id)",

			// ---------- Customer ----------
			"CREATE TABLE {S}.customer ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "salesperson_id bigint REFERENCES {S}.salesperson(id), "
					+ "status varchar(32) DEFAULT 'AKTIF', "
					+ "aktif boolean DEFAULT true, "
					+ "kandidat_duplikat boolean DEFAULT false, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_customer_kode ON {S}.customer (kode)",
			"CREATE INDEX idx_{SU}_customer_nama ON {S}.customer (nama)",
			"CREATE INDEX idx_{SU}_customer_status ON {S}.customer (status)",
			"CREATE INDEX idx_{SU}_customer_sales ON {S}.customer (salesperson_id)",
			"CREATE INDEX idx_{SU}_customer_legacy ON {S}.customer "
					+ "(legacy_source_file, legacy_source_record_no)",
			"CREATE INDEX idx_{SU}_customer_rowhash ON {S}.customer (legacy_row_hash)",

			"CREATE TABLE {S}.customer_profile ("
					+ "id bigserial PRIMARY KEY, "
					+ "customer_id bigint NOT NULL REFERENCES {S}.customer(id), "
					+ "atas_nama varchar(255), "
					+ "alamat1 text, alamat2 text, alamat text, "
					+ "kota varchar(100), kode_pos varchar(20), "
					+ "telp varchar(50), email varchar(255), "
					+ "syarat_bayar_hari integer, "
					+ "diskon numeric(9,4), "
					+ "plafon_piutang numeric(18,2), "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_customer_profile UNIQUE (customer_id))",

			"CREATE TABLE {S}.customer_bank_account ("
					+ "id bigserial PRIMARY KEY, "
					+ "customer_id bigint NOT NULL REFERENCES {S}.customer(id), "
					+ "nama_bank varchar(255), "
					+ "nomor_rekening varchar(64), "
					+ "atas_nama varchar(255), "
					+ "utama boolean DEFAULT false, "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_customer_bank_customer ON {S}.customer_bank_account (customer_id)",
	};

	/**
	 * DDL kanonik v2 untuk schema audit tenant, dieksekusi oleh
	 * {@link TenantSchemaService#terapkanMigrasi} sebagai bundel {@code v2-inventory-master-audit}.
	 * Dua bagian: (1) serangkaian {@code ALTER TABLE {A}.revinfo ADD COLUMN} yang memperluas
	 * tabel {@code revinfo} v1 dengan konteks aktor/permintaan §11.6 (siapa, peran apa, dari
	 * perangkat mana, permintaan yang mana, alasannya apa) -- sengaja ALTER, bukan CREATE ulang,
	 * sebab definisi kolom v1 tidak boleh disentuh; (2) tabel baru {@code {A}.audit_baris},
	 * catatan baris generik ({@code entity}/{@code entity_id}/{@code sebelum}/{@code sesudah})
	 * yang menggantikan pola cermin-per-tabel v1 untuk seluruh tabel v2 ke atas -- lihat javadoc
	 * kelas untuk alasannya.
	 */
	public static final String[] AUDIT = {
			// revinfo v1 hanya (rev, revtstmp). Diperluas dengan konteks §11.6 -- ALTER, bukan
			// CREATE ulang, sebab definisi v1 tidak boleh disentuh.
			"ALTER TABLE {A}.revinfo ADD COLUMN tenant_id bigint",
			"ALTER TABLE {A}.revinfo ADD COLUMN tenant_code varchar(64)",
			"ALTER TABLE {A}.revinfo ADD COLUMN membership_id bigint",
			"ALTER TABLE {A}.revinfo ADD COLUMN user_id varchar(255)",
			"ALTER TABLE {A}.revinfo ADD COLUMN role varchar(64)",
			"ALTER TABLE {A}.revinfo ADD COLUMN actor_type varchar(32)",
			"ALTER TABLE {A}.revinfo ADD COLUMN device_id varchar(128)",
			"ALTER TABLE {A}.revinfo ADD COLUMN request_id varchar(64)",
			"ALTER TABLE {A}.revinfo ADD COLUMN correlation_id varchar(64)",
			"ALTER TABLE {A}.revinfo ADD COLUMN idempotency_key varchar(128)",
			"ALTER TABLE {A}.revinfo ADD COLUMN action varchar(64)",
			"ALTER TABLE {A}.revinfo ADD COLUMN reason text",
			"ALTER TABLE {A}.revinfo ADD COLUMN waktu timestamp",
			"CREATE INDEX idx_{SU}_revinfo_user ON {A}.revinfo (user_id)",
			"CREATE INDEX idx_{SU}_revinfo_waktu ON {A}.revinfo (waktu)",
			"CREATE INDEX idx_{SU}_revinfo_correlation ON {A}.revinfo (correlation_id)",
			"CREATE INDEX idx_{SU}_revinfo_idem ON {A}.revinfo (idempotency_key)",

			// Catatan baris generik: menggantikan cermin kolom untuk seluruh tabel v2 ke atas.
			// sebelum/sesudah bertipe text (bukan jsonb -- PostgreSQL 9.3).
			"CREATE TABLE {A}.audit_baris ("
					+ "id bigserial PRIMARY KEY, "
					+ "rev bigint NOT NULL REFERENCES {A}.revinfo(rev), "
					+ "revtype smallint NOT NULL, "
					+ "entity varchar(128) NOT NULL, "
					+ "entity_id varchar(64) NOT NULL, "
					+ "sebelum text, "
					+ "sesudah text, "
					+ "waktu timestamp)",
			"CREATE INDEX idx_{SU}_audit_baris_entity ON {A}.audit_baris (entity, entity_id)",
			"CREATE INDEX idx_{SU}_audit_baris_rev ON {A}.audit_baris (rev)",
			"CREATE INDEX idx_{SU}_audit_baris_waktu ON {A}.audit_baris (waktu)",
	};
}
