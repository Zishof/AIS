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
 */
public final class TenantSchemaMigrations {

	private TenantSchemaMigrations() {
	}

	/** Target eksekusi migrasi. */
	public static final String TARGET_ERP = "ERP";
	public static final String TARGET_AUDIT = "AUDIT";

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
	};

	/** Versi schema efektif setelah seluruh migrasi terpasang (dicatat ke registry.schemaVersion). */
	public static final String VERSI_TERKINI = "v1-core-pos";

	/** Tabel yang WAJIB ada pasca-migrasi (dipakai VERIFY_SCHEMA). */
	public static final String[] TABEL_WAJIB_ERP = { "tenant_schema_migration", "brand", "toko", "pedagang" };
	public static final String[] TABEL_WAJIB_AUDIT = { "revinfo", "brand", "toko", "pedagang" };
}
