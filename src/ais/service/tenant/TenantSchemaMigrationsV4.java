package ais.service.tenant;

/**
 * <h3>Migrasi tenant v4 — pembelian dan hutang dagang (P3).</h3>
 *
 * <p>Sumber legacy: {@code BELI.DBF} (60.269 baris) &rarr; {@code pembelian} +
 * {@code pembelian_detail}; {@code TRAN_HUT.DBF} (8.863 header, 70 aktif, 8.793 terhapus)
 * &rarr; {@code hutang_supplier} + {@code pembayaran_hutang} + alokasinya.</p>
 *
 * <h4>Nomor faktur tidak unik global</h4>
 * <p>§11.4 menegaskannya, dan arsip legacy membuktikannya: satu nomor dapat muncul untuk
 * pemasok berbeda atau periode berbeda. Karena itu unique-nya berada pada konteks natural
 * <b>(supplier, nomor_faktur, tanggal)</b>, bukan pada nomor saja. Memasang unique global akan
 * menggagalkan impor pada data yang sebenarnya sah.</p>
 *
 * <h4>Baris terhapus legacy bukan penghapusan</h4>
 * <p>TRAN_HUT menyimpan 8.793 baris bertanda hapus terhadap 70 yang aktif. Rasio itu
 * memberitahu bahwa tanda hapus di sana dipakai sebagai penanda <b>lunas/riwayat</b>, bukan
 * pembatalan. Baris demikian tetap diimpor dengan {@code legacy_deleted = true} dan
 * {@code tafsir_legacy} yang menyimpan penafsirannya — dibuang buta akan menghapus seluruh
 * riwayat pelunasan (§25.4).</p>
 *
 * <h4>Dokumen terposting dikoreksi dengan pembalik</h4>
 * <p>Tidak ada hard delete atas dokumen yang sudah diposting (§24 butir 14). Pembatalan
 * menerbitkan dokumen pembalik yang menunjuk asalnya lewat {@code pembalik_dari_id}.</p>
 */
public final class TenantSchemaMigrationsV4 {

	/** Kelas utilitas murni statis — tidak pernah diinstansiasi. */
	private TenantSchemaMigrationsV4() {
	}

	// PERINGATAN: konstanta ini masuk ke teks DDL kanonik. Mengubahnya mengubah checksum v4
	// dan menggagalkan migrasi di seluruh tenant yang sudah memasangnya. Butuh bentuk lain?
	// Buat bundel versi BARU.
	/**
	 * Fragmen kolom provenans impor legacy, disisipkan ke setiap tabel bundel ini yang
	 * datanya berasal dari {@code BELI.DBF}/{@code TRAN_HUT.DBF}: nama berkas dan nomor
	 * baris sumber, hash baris (dedup/verifikasi ulang), id run impor, penanda
	 * {@code legacy_deleted} (lihat javadoc kelas — di TRAN_HUT dipakai sebagai penanda
	 * lunas/riwayat, bukan pembatalan), dan {@code legacy_tafsir} untuk mencatat penafsiran
	 * baris bertanda hapus tersebut.
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
	 * (pembelian, pembayaran hutang). Tidak ada hard delete atas dokumen yang sudah
	 * diposting (§24 butir 14); pembatalan hanya menandai {@code dibatalkan} dan menerbitkan
	 * dokumen pembalik lewat kolom {@code pembalik_dari_id} masing-masing tabel.
	 */
	private static final String POSTING =
			"status varchar(32) DEFAULT 'DRAF', "
			+ "diposting boolean DEFAULT false, diposting_pada timestamp, "
			+ "dibatalkan boolean DEFAULT false, dibatalkan_pada timestamp, alasan_batal text";

	/**
	 * Katalog DDL kanonik migrasi v4: {@code CREATE TABLE}/{@code CREATE INDEX} berurutan
	 * untuk {@code pembelian}, {@code pembelian_detail}, {@code hutang_supplier},
	 * {@code pembayaran_hutang}, dan {@code alokasi_pembayaran_hutang} — lihat javadoc kelas
	 * untuk sumber legacy dan aturan unique natural per tabel. Dikonsumsi oleh
	 * {@link TenantSchemaMigrations#SEMUA} lewat entri bertarget {@code TARGET_ERP}; penanda
	 * {@code {S}} (schema data), {@code {A}} (schema audit), dan {@code {SU}} (nama schema
	 * mentah, dipakai pada nama indeks/constraint) disubstitusi saat migrasi diterapkan oleh
	 * {@code TenantSchemaService#terapkanMigrasi}. Isi array ini adalah bagian dari checksum
	 * kanonik v4 — lihat peringatan di atas sebelum mengubah elemen mana pun.
	 */
	public static final String[] ERP = {
			"CREATE TABLE {S}.pembelian ("
					+ "id bigserial PRIMARY KEY, "
					+ "nomor_dokumen varchar(64) NOT NULL, "
					+ "nomor_faktur varchar(64), "
					+ "tanggal date NOT NULL, "
					+ "jatuh_tempo date, "
					+ "supplier_id bigint NOT NULL REFERENCES {S}.supplier(id), "
					+ "gudang_id bigint REFERENCES {S}.gudang(id), "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "subtotal numeric(18,2) DEFAULT 0, "
					+ "diskon numeric(18,2) DEFAULT 0, "
					+ "pajak numeric(18,2) DEFAULT 0, "
					+ "total numeric(18,2) DEFAULT 0, "
					+ "keterangan text, "
					+ "pembalik_dari_id bigint REFERENCES {S}.pembelian(id), "
					+ "idempotency_key varchar(128), correlation_id varchar(64), "
					+ POSTING + ", " + JEJAK + ", " + LEGACY + ", "
					+ "CONSTRAINT uq_{SU}_pembelian_natural UNIQUE (supplier_id, nomor_faktur, tanggal))",
			"CREATE INDEX idx_{SU}_pembelian_nomor ON {S}.pembelian (nomor_dokumen)",
			"CREATE INDEX idx_{SU}_pembelian_tanggal ON {S}.pembelian (tanggal)",
			"CREATE INDEX idx_{SU}_pembelian_supplier ON {S}.pembelian (supplier_id)",
			"CREATE INDEX idx_{SU}_pembelian_status ON {S}.pembelian (status)",
			"CREATE INDEX idx_{SU}_pembelian_gudang ON {S}.pembelian (gudang_id)",
			"CREATE INDEX idx_{SU}_pembelian_idem ON {S}.pembelian (idempotency_key)",
			"CREATE INDEX idx_{SU}_pembelian_correlation ON {S}.pembelian (correlation_id)",
			"CREATE INDEX idx_{SU}_pembelian_legacy ON {S}.pembelian "
					+ "(legacy_source_file, legacy_source_record_no)",
			"CREATE INDEX idx_{SU}_pembelian_rowhash ON {S}.pembelian (legacy_row_hash)",

			"CREATE TABLE {S}.pembelian_detail ("
					+ "id bigserial PRIMARY KEY, "
					+ "pembelian_id bigint NOT NULL REFERENCES {S}.pembelian(id), "
					+ "baris_ke integer, "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "produk_batch_id bigint REFERENCES {S}.produk_batch(id), "
					+ "batch_no varchar(64), expiry_date date, "
					+ "kuantitas numeric(18,4) NOT NULL, "
					+ "kuantitas_bonus numeric(18,4) DEFAULT 0, "
					+ "harga_satuan numeric(18,2) NOT NULL, "
					+ "diskon numeric(18,2) DEFAULT 0, "
					+ "total numeric(18,2) DEFAULT 0, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_pembelian_detail_induk ON {S}.pembelian_detail (pembelian_id)",
			"CREATE INDEX idx_{SU}_pembelian_detail_produk ON {S}.pembelian_detail (produk_id)",
			"CREATE INDEX idx_{SU}_pembelian_detail_batch ON {S}.pembelian_detail (batch_no)",
			"CREATE INDEX idx_{SU}_pembelian_detail_expiry ON {S}.pembelian_detail (expiry_date)",

			"CREATE TABLE {S}.hutang_supplier ("
					+ "id bigserial PRIMARY KEY, "
					+ "supplier_id bigint NOT NULL REFERENCES {S}.supplier(id), "
					+ "pembelian_id bigint REFERENCES {S}.pembelian(id), "
					+ "nomor_faktur varchar(64), "
					+ "tanggal date NOT NULL, "
					+ "jatuh_tempo date, "
					+ "nilai numeric(18,2) NOT NULL, "
					+ "terbayar numeric(18,2) DEFAULT 0, "
					+ "sisa numeric(18,2) DEFAULT 0, "
					+ "status varchar(32) DEFAULT 'TERBUKA', "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_hutang_supplier_sup ON {S}.hutang_supplier (supplier_id)",
			"CREATE INDEX idx_{SU}_hutang_supplier_tanggal ON {S}.hutang_supplier (tanggal)",
			"CREATE INDEX idx_{SU}_hutang_supplier_status ON {S}.hutang_supplier (status)",
			"CREATE INDEX idx_{SU}_hutang_supplier_nomor ON {S}.hutang_supplier (nomor_faktur)",
			"CREATE INDEX idx_{SU}_hutang_supplier_legacy ON {S}.hutang_supplier "
					+ "(legacy_source_file, legacy_source_record_no)",

			"CREATE TABLE {S}.pembayaran_hutang ("
					+ "id bigserial PRIMARY KEY, "
					+ "nomor_dokumen varchar(64) NOT NULL, "
					+ "tanggal date NOT NULL, "
					+ "supplier_id bigint NOT NULL REFERENCES {S}.supplier(id), "
					+ "cara_bayar varchar(32), "
					+ "nomor_bg varchar(64), nama_bank varchar(255), tanggal_bg date, "
					+ "nilai numeric(18,2) NOT NULL, "
					+ "keterangan text, "
					+ "pembalik_dari_id bigint REFERENCES {S}.pembayaran_hutang(id), "
					+ "idempotency_key varchar(128), correlation_id varchar(64), "
					+ POSTING + ", " + JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_bayar_hutang_nomor ON {S}.pembayaran_hutang (nomor_dokumen)",
			"CREATE INDEX idx_{SU}_bayar_hutang_tanggal ON {S}.pembayaran_hutang (tanggal)",
			"CREATE INDEX idx_{SU}_bayar_hutang_supplier ON {S}.pembayaran_hutang (supplier_id)",
			"CREATE INDEX idx_{SU}_bayar_hutang_status ON {S}.pembayaran_hutang (status)",
			"CREATE INDEX idx_{SU}_bayar_hutang_idem ON {S}.pembayaran_hutang (idempotency_key)",

			"CREATE TABLE {S}.alokasi_pembayaran_hutang ("
					+ "id bigserial PRIMARY KEY, "
					+ "pembayaran_hutang_id bigint NOT NULL REFERENCES {S}.pembayaran_hutang(id), "
					+ "hutang_supplier_id bigint NOT NULL REFERENCES {S}.hutang_supplier(id), "
					+ "nilai numeric(18,2) NOT NULL, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_alokasi_hutang_bayar ON {S}.alokasi_pembayaran_hutang "
					+ "(pembayaran_hutang_id)",
			"CREATE INDEX idx_{SU}_alokasi_hutang_hutang ON {S}.alokasi_pembayaran_hutang "
					+ "(hutang_supplier_id)",
	};
}
