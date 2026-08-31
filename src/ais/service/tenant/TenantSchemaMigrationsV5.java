package ais.service.tenant;

/**
 * <h3>Migrasi tenant v5 — penjualan dan piutang dagang (P3).</h3>
 *
 * <p>Sumber legacy: {@code JUAL.DBF} (94.072 baris) &rarr; {@code faktur_penjualan} +
 * {@code faktur_penjualan_detail}; {@code TRAN_PIUT.DBF} (36.010 header, 832 aktif, 35.178
 * terhapus) &rarr; {@code piutang_customer} + {@code penerimaan_piutang} + alokasinya.</p>
 *
 * <p>{@code JUA-rusakL.DBF} <b>tidak diimpor</b>. Pemeriksaan terhadap arsip menunjukkan
 * 88.553 dari 88.592 barisnya identik dengan JUAL, dan JUAL memuat 5.490 baris lebih baru
 * (sampai 4 Agustus 2026 berbanding 30 Juni 2026). Dua belas baris yang hanya ada di sana
 * berasal dari tiga faktur yang tidak berjejak di TRAN_PIUT sama sekali — keadaan sebelum
 * koreksi, bukan transaksi yang hilang.</p>
 *
 * <h4>Header faktur tidak dibentuk dari nomor saja</h4>
 * <p>§13.3 melarangnya, dan alasannya sama dengan pembelian: nomor faktur tidak unik global.
 * Unique-nya memakai konteks natural <b>(customer, nomor_faktur, tanggal)</b>. Pembentukan
 * header saat impor memakai sumber, tanggal, pihak, nomor, dan baris buktinya.</p>
 *
 * <h4>Retur tidak menghapus faktur</h4>
 * <p>TRAN_PIUT menyimpan {@code NORETUR}; nilainya masuk {@code nomor_retur} pada piutang, dan
 * pengurangannya menjadi baris {@code penerimaan_piutang} bertanda {@code jenis = 'RETUR'} —
 * bukan UPDATE atas faktur aslinya. Faktur yang sudah diposting hanya dikoreksi lewat dokumen
 * pembalik (§24 butir 14).</p>
 */
public final class TenantSchemaMigrationsV5 {

	/** Kelas utilitas murni statis — tidak pernah diinstansiasi. */
	private TenantSchemaMigrationsV5() {
	}

	// PERINGATAN: masuk ke teks DDL kanonik -- mengubahnya mengubah checksum v5. Buat bundel
	// versi BARU bila perlu bentuk lain.
	/**
	 * Fragmen kolom provenans impor legacy, disisipkan ke tabel bundel ini yang datanya
	 * berasal dari {@code JUAL.DBF}/{@code TRAN_PIUT.DBF}: nama berkas dan nomor baris
	 * sumber, hash baris, id run impor, penanda {@code legacy_deleted} (di TRAN_PIUT dipakai
	 * untuk retur — lihat javadoc kelas), dan {@code legacy_tafsir} untuk penafsirannya.
	 */
	private static final String LEGACY =
			"legacy_source_file varchar(128), legacy_source_record_no integer, "
			+ "legacy_row_hash varchar(64), legacy_import_run_id bigint, "
			+ "legacy_deleted boolean DEFAULT false, legacy_tafsir varchar(64)";

	/** Fragmen kolom jejak audit ringan (pembuat/pengubah + waktunya), dipakai seluruh tabel. */
	private static final String JEJAK =
			"dibuat_pada timestamp, tanggal_dirubah timestamp, "
			+ "oleh varchar(255), olehid varchar(255)";

	/**
	 * Fragmen kolom status dokumen dan siklus posting/pembatalan, dipakai tabel dokumen
	 * (sales order, faktur penjualan, penerimaan piutang). Faktur yang sudah diposting hanya
	 * dikoreksi lewat dokumen pembalik ({@code pembalik_dari_id}), bukan hard delete
	 * (§24 butir 14).
	 */
	private static final String POSTING =
			"status varchar(32) DEFAULT 'DRAF', "
			+ "diposting boolean DEFAULT false, diposting_pada timestamp, "
			+ "dibatalkan boolean DEFAULT false, dibatalkan_pada timestamp, alasan_batal text";

	/**
	 * Katalog DDL kanonik migrasi v5: {@code CREATE TABLE}/{@code CREATE INDEX} berurutan
	 * untuk {@code sales_order}, {@code sales_order_detail}, {@code faktur_penjualan},
	 * {@code faktur_penjualan_detail}, {@code piutang_customer}, {@code penerimaan_piutang},
	 * dan {@code alokasi_penerimaan_piutang} — lihat javadoc kelas untuk sumber legacy dan
	 * aturan unique natural (customer, nomor_faktur, tanggal). Dikonsumsi oleh
	 * {@link TenantSchemaMigrations#SEMUA} lewat entri bertarget {@code TARGET_ERP}; penanda
	 * {@code {S}}/{@code {A}}/{@code {SU}} disubstitusi saat migrasi diterapkan oleh
	 * {@code TenantSchemaService#terapkanMigrasi}. Isi array ini bagian dari checksum
	 * kanonik v5 — lihat peringatan di atas sebelum mengubah elemen mana pun.
	 */
	public static final String[] ERP = {
			"CREATE TABLE {S}.sales_order ("
					+ "id bigserial PRIMARY KEY, "
					+ "nomor_dokumen varchar(64) NOT NULL, "
					+ "tanggal date NOT NULL, "
					+ "customer_id bigint NOT NULL REFERENCES {S}.customer(id), "
					+ "salesperson_id bigint REFERENCES {S}.salesperson(id), "
					+ "gudang_id bigint REFERENCES {S}.gudang(id), "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "subtotal numeric(18,2) DEFAULT 0, diskon numeric(18,2) DEFAULT 0, "
					+ "pajak numeric(18,2) DEFAULT 0, total numeric(18,2) DEFAULT 0, "
					+ "keterangan text, "
					+ "idempotency_key varchar(128), correlation_id varchar(64), "
					+ POSTING + ", " + JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_sales_order_nomor ON {S}.sales_order (nomor_dokumen)",
			"CREATE INDEX idx_{SU}_sales_order_tanggal ON {S}.sales_order (tanggal)",
			"CREATE INDEX idx_{SU}_sales_order_customer ON {S}.sales_order (customer_id)",
			"CREATE INDEX idx_{SU}_sales_order_sales ON {S}.sales_order (salesperson_id)",
			"CREATE INDEX idx_{SU}_sales_order_status ON {S}.sales_order (status)",
			"CREATE INDEX idx_{SU}_sales_order_idem ON {S}.sales_order (idempotency_key)",

			"CREATE TABLE {S}.sales_order_detail ("
					+ "id bigserial PRIMARY KEY, "
					+ "sales_order_id bigint NOT NULL REFERENCES {S}.sales_order(id), "
					+ "baris_ke integer, "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "kuantitas numeric(18,4) NOT NULL, "
					+ "harga_satuan numeric(18,2) NOT NULL, "
					+ "diskon numeric(18,2) DEFAULT 0, total numeric(18,2) DEFAULT 0, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_sales_order_detail_induk ON {S}.sales_order_detail (sales_order_id)",
			"CREATE INDEX idx_{SU}_sales_order_detail_produk ON {S}.sales_order_detail (produk_id)",

			"CREATE TABLE {S}.faktur_penjualan ("
					+ "id bigserial PRIMARY KEY, "
					+ "nomor_dokumen varchar(64) NOT NULL, "
					+ "nomor_faktur varchar(64), "
					+ "tanggal date NOT NULL, "
					+ "jatuh_tempo date, "
					+ "customer_id bigint NOT NULL REFERENCES {S}.customer(id), "
					+ "salesperson_id bigint REFERENCES {S}.salesperson(id), "
					+ "sales_order_id bigint REFERENCES {S}.sales_order(id), "
					+ "gudang_id bigint REFERENCES {S}.gudang(id), "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "subtotal numeric(18,2) DEFAULT 0, diskon numeric(18,2) DEFAULT 0, "
					+ "pajak numeric(18,2) DEFAULT 0, total numeric(18,2) DEFAULT 0, "
					+ "hpp numeric(18,2) DEFAULT 0, "
					+ "keterangan text, "
					+ "pembalik_dari_id bigint REFERENCES {S}.faktur_penjualan(id), "
					+ "idempotency_key varchar(128), correlation_id varchar(64), "
					+ POSTING + ", " + JEJAK + ", " + LEGACY + ", "
					+ "CONSTRAINT uq_{SU}_faktur_jual_natural UNIQUE (customer_id, nomor_faktur, tanggal))",
			"CREATE INDEX idx_{SU}_faktur_jual_nomor ON {S}.faktur_penjualan (nomor_dokumen)",
			"CREATE INDEX idx_{SU}_faktur_jual_tanggal ON {S}.faktur_penjualan (tanggal)",
			"CREATE INDEX idx_{SU}_faktur_jual_customer ON {S}.faktur_penjualan (customer_id)",
			"CREATE INDEX idx_{SU}_faktur_jual_sales ON {S}.faktur_penjualan (salesperson_id)",
			"CREATE INDEX idx_{SU}_faktur_jual_status ON {S}.faktur_penjualan (status)",
			"CREATE INDEX idx_{SU}_faktur_jual_idem ON {S}.faktur_penjualan (idempotency_key)",
			"CREATE INDEX idx_{SU}_faktur_jual_correlation ON {S}.faktur_penjualan (correlation_id)",
			"CREATE INDEX idx_{SU}_faktur_jual_legacy ON {S}.faktur_penjualan "
					+ "(legacy_source_file, legacy_source_record_no)",
			"CREATE INDEX idx_{SU}_faktur_jual_rowhash ON {S}.faktur_penjualan (legacy_row_hash)",

			"CREATE TABLE {S}.faktur_penjualan_detail ("
					+ "id bigserial PRIMARY KEY, "
					+ "faktur_penjualan_id bigint NOT NULL REFERENCES {S}.faktur_penjualan(id), "
					+ "baris_ke integer, "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "produk_batch_id bigint REFERENCES {S}.produk_batch(id), "
					+ "batch_no varchar(64), expiry_date date, "
					+ "kuantitas numeric(18,4) NOT NULL, "
					+ "harga_satuan numeric(18,2) NOT NULL, "
					+ "harga_beli numeric(18,2), "
					+ "diskon numeric(18,2) DEFAULT 0, total numeric(18,2) DEFAULT 0, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_faktur_jual_detail_induk ON {S}.faktur_penjualan_detail "
					+ "(faktur_penjualan_id)",
			"CREATE INDEX idx_{SU}_faktur_jual_detail_produk ON {S}.faktur_penjualan_detail (produk_id)",
			"CREATE INDEX idx_{SU}_faktur_jual_detail_batch ON {S}.faktur_penjualan_detail (batch_no)",
			"CREATE INDEX idx_{SU}_faktur_jual_detail_expiry ON {S}.faktur_penjualan_detail (expiry_date)",

			"CREATE TABLE {S}.piutang_customer ("
					+ "id bigserial PRIMARY KEY, "
					+ "customer_id bigint NOT NULL REFERENCES {S}.customer(id), "
					+ "salesperson_id bigint REFERENCES {S}.salesperson(id), "
					+ "faktur_penjualan_id bigint REFERENCES {S}.faktur_penjualan(id), "
					+ "nomor_faktur varchar(64), "
					+ "nomor_retur varchar(64), "
					+ "tanggal date NOT NULL, jatuh_tempo date, "
					+ "nilai numeric(18,2) NOT NULL, "
					+ "terbayar numeric(18,2) DEFAULT 0, sisa numeric(18,2) DEFAULT 0, "
					+ "status varchar(32) DEFAULT 'TERBUKA', "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_piutang_customer_cus ON {S}.piutang_customer (customer_id)",
			"CREATE INDEX idx_{SU}_piutang_customer_sales ON {S}.piutang_customer (salesperson_id)",
			"CREATE INDEX idx_{SU}_piutang_customer_tanggal ON {S}.piutang_customer (tanggal)",
			"CREATE INDEX idx_{SU}_piutang_customer_status ON {S}.piutang_customer (status)",
			"CREATE INDEX idx_{SU}_piutang_customer_nomor ON {S}.piutang_customer (nomor_faktur)",
			"CREATE INDEX idx_{SU}_piutang_customer_legacy ON {S}.piutang_customer "
					+ "(legacy_source_file, legacy_source_record_no)",

			"CREATE TABLE {S}.penerimaan_piutang ("
					+ "id bigserial PRIMARY KEY, "
					+ "nomor_dokumen varchar(64) NOT NULL, "
					+ "tanggal date NOT NULL, "
					+ "customer_id bigint NOT NULL REFERENCES {S}.customer(id), "
					+ "salesperson_id bigint REFERENCES {S}.salesperson(id), "
					+ "jenis varchar(32) DEFAULT 'PEMBAYARAN', "
					+ "cara_bayar varchar(32), "
					+ "nomor_bg varchar(64), nama_bank varchar(255), tanggal_bg date, "
					+ "nilai numeric(18,2) NOT NULL, "
					+ "keterangan text, "
					+ "pembalik_dari_id bigint REFERENCES {S}.penerimaan_piutang(id), "
					+ "idempotency_key varchar(128), correlation_id varchar(64), "
					+ POSTING + ", " + JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_terima_piutang_nomor ON {S}.penerimaan_piutang (nomor_dokumen)",
			"CREATE INDEX idx_{SU}_terima_piutang_tanggal ON {S}.penerimaan_piutang (tanggal)",
			"CREATE INDEX idx_{SU}_terima_piutang_customer ON {S}.penerimaan_piutang (customer_id)",
			"CREATE INDEX idx_{SU}_terima_piutang_sales ON {S}.penerimaan_piutang (salesperson_id)",
			"CREATE INDEX idx_{SU}_terima_piutang_status ON {S}.penerimaan_piutang (status)",
			"CREATE INDEX idx_{SU}_terima_piutang_idem ON {S}.penerimaan_piutang (idempotency_key)",

			"CREATE TABLE {S}.alokasi_penerimaan_piutang ("
					+ "id bigserial PRIMARY KEY, "
					+ "penerimaan_piutang_id bigint NOT NULL REFERENCES {S}.penerimaan_piutang(id), "
					+ "piutang_customer_id bigint NOT NULL REFERENCES {S}.piutang_customer(id), "
					+ "nilai numeric(18,2) NOT NULL, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_alokasi_piutang_terima ON {S}.alokasi_penerimaan_piutang "
					+ "(penerimaan_piutang_id)",
			"CREATE INDEX idx_{SU}_alokasi_piutang_piutang ON {S}.alokasi_penerimaan_piutang "
					+ "(piutang_customer_id)",
	};
}
