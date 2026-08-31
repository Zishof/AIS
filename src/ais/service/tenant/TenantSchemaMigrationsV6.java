package ais.service.tenant;

/**
 * <h3>Migrasi tenant v6 — sales keliling, SPJ, dan nota (P3).</h3>
 *
 * <p>Alur lapangan: <b>surat perintah</b> menugaskan sales membawa barang keluar;
 * <b>trip</b> mencatat pelaksanaannya; <b>nota</b> mencatat penjualan di tempat; <b>hasil</b>
 * mencatat barang yang kembali; <b>biaya</b> dan <b>setoran</b> menutup uangnya; dan
 * <b>rekonsiliasi</b> mempertemukan barang keluar dengan barang kembali plus uang masuk.</p>
 *
 * <h4>Rekonsiliasi berdiri sendiri</h4>
 * <p>{@code sales_trip_rekonsiliasi} tidak dihitung ulang setiap layar dibuka melainkan
 * disimpan, karena ia adalah <b>kesepakatan</b> antara sales dan kantor pada satu titik waktu.
 * Menghitungnya ulang dari data terkini akan mengubah angka yang sudah ditandatangani ketika
 * ada koreksi menyusul — dan justru selisih itulah yang perlu terlihat.</p>
 *
 * <h4>Barang keluar bukan penjualan</h4>
 * <p>{@code sales_trip_barang} mencatat barang yang dibawa, bukan yang terjual. Mutasi
 * stoknya berjenis pemindahan ke lokasi sales, bukan pengurangan penjualan; penjualan baru
 * terjadi saat nota terbit. Menyamakan keduanya membuat stok berkurang dua kali.</p>
 */
public final class TenantSchemaMigrationsV6 {

	/** Kelas utilitas murni statis — tidak pernah diinstansiasi. */
	private TenantSchemaMigrationsV6() {
	}

	// PERINGATAN: masuk ke teks DDL kanonik -- mengubahnya mengubah checksum v6.
	/** Fragmen kolom jejak audit ringan (pembuat/pengubah + waktunya), dipakai seluruh tabel. */
	private static final String JEJAK =
			"dibuat_pada timestamp, tanggal_dirubah timestamp, "
			+ "oleh varchar(255), olehid varchar(255)";

	/**
	 * Fragmen kolom status dokumen dan siklus posting/pembatalan, dipakai tabel dokumen alur
	 * sales keliling ({@code surat_perintah_sales}, {@code sales_trip}, {@code sales_trip_nota},
	 * {@code sales_trip_setoran}, {@code sales_trip_rekonsiliasi}).
	 */
	private static final String POSTING =
			"status varchar(32) DEFAULT 'DRAF', "
			+ "diposting boolean DEFAULT false, diposting_pada timestamp, "
			+ "dibatalkan boolean DEFAULT false, dibatalkan_pada timestamp, alasan_batal text";

	/**
	 * Katalog DDL kanonik migrasi v6: {@code CREATE TABLE}/{@code CREATE INDEX} berurutan
	 * untuk seluruh rangkaian sales keliling — {@code surat_perintah_sales} (+ detail),
	 * {@code sales_trip}, {@code sales_trip_barang} (barang dibawa, bukan penjualan — lihat
	 * javadoc kelas), {@code sales_trip_nota}, {@code sales_trip_hasil},
	 * {@code sales_trip_biaya}, {@code sales_trip_setoran}, dan
	 * {@code sales_trip_rekonsiliasi} (disimpan, tidak dihitung ulang — lihat javadoc kelas).
	 * Dikonsumsi oleh {@link TenantSchemaMigrations#SEMUA} lewat entri bertarget
	 * {@code TARGET_ERP}; penanda {@code {S}}/{@code {A}}/{@code {SU}} disubstitusi saat
	 * migrasi diterapkan oleh {@code TenantSchemaService#terapkanMigrasi}. Isi array ini
	 * bagian dari checksum kanonik v6 — lihat peringatan di atas sebelum mengubah elemen
	 * mana pun.
	 */
	public static final String[] ERP = {
			"CREATE TABLE {S}.surat_perintah_sales ("
					+ "id bigserial PRIMARY KEY, "
					+ "nomor_dokumen varchar(64) NOT NULL, "
					+ "tanggal date NOT NULL, "
					+ "salesperson_id bigint NOT NULL REFERENCES {S}.salesperson(id), "
					+ "gudang_id bigint REFERENCES {S}.gudang(id), "
					+ "wilayah varchar(255), "
					+ "berlaku_dari date, berlaku_sampai date, "
					+ "keterangan text, "
					+ POSTING + ", " + JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_sps_nomor UNIQUE (nomor_dokumen, tanggal))",
			"CREATE INDEX idx_{SU}_sps_tanggal ON {S}.surat_perintah_sales (tanggal)",
			"CREATE INDEX idx_{SU}_sps_sales ON {S}.surat_perintah_sales (salesperson_id)",
			"CREATE INDEX idx_{SU}_sps_status ON {S}.surat_perintah_sales (status)",

			"CREATE TABLE {S}.surat_perintah_sales_detail ("
					+ "id bigserial PRIMARY KEY, "
					+ "surat_perintah_sales_id bigint NOT NULL REFERENCES {S}.surat_perintah_sales(id), "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "kuantitas numeric(18,4) NOT NULL, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_sps_detail_induk ON {S}.surat_perintah_sales_detail "
					+ "(surat_perintah_sales_id)",
			"CREATE INDEX idx_{SU}_sps_detail_produk ON {S}.surat_perintah_sales_detail (produk_id)",

			"CREATE TABLE {S}.sales_trip ("
					+ "id bigserial PRIMARY KEY, "
					+ "nomor_dokumen varchar(64) NOT NULL, "
					+ "surat_perintah_sales_id bigint REFERENCES {S}.surat_perintah_sales(id), "
					+ "salesperson_id bigint NOT NULL REFERENCES {S}.salesperson(id), "
					+ "gudang_id bigint REFERENCES {S}.gudang(id), "
					+ "tanggal_berangkat date NOT NULL, tanggal_kembali date, "
					+ "kendaraan varchar(64), "
					+ "keterangan text, "
					+ "idempotency_key varchar(128), correlation_id varchar(64), "
					+ POSTING + ", " + JEJAK + ")",
			"CREATE INDEX idx_{SU}_trip_nomor ON {S}.sales_trip (nomor_dokumen)",
			"CREATE INDEX idx_{SU}_trip_tanggal ON {S}.sales_trip (tanggal_berangkat)",
			"CREATE INDEX idx_{SU}_trip_sales ON {S}.sales_trip (salesperson_id)",
			"CREATE INDEX idx_{SU}_trip_status ON {S}.sales_trip (status)",
			"CREATE INDEX idx_{SU}_trip_idem ON {S}.sales_trip (idempotency_key)",

			"CREATE TABLE {S}.sales_trip_barang ("
					+ "id bigserial PRIMARY KEY, "
					+ "sales_trip_id bigint NOT NULL REFERENCES {S}.sales_trip(id), "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "produk_batch_id bigint REFERENCES {S}.produk_batch(id), "
					+ "kuantitas_bawa numeric(18,4) NOT NULL DEFAULT 0, "
					+ "harga_satuan numeric(18,2), "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_trip_barang_induk ON {S}.sales_trip_barang (sales_trip_id)",
			"CREATE INDEX idx_{SU}_trip_barang_produk ON {S}.sales_trip_barang (produk_id)",

			"CREATE TABLE {S}.sales_trip_nota ("
					+ "id bigserial PRIMARY KEY, "
					+ "sales_trip_id bigint NOT NULL REFERENCES {S}.sales_trip(id), "
					+ "nomor_nota varchar(64) NOT NULL, "
					+ "tanggal date NOT NULL, "
					+ "customer_id bigint REFERENCES {S}.customer(id), "
					+ "faktur_penjualan_id bigint REFERENCES {S}.faktur_penjualan(id), "
					+ "total numeric(18,2) DEFAULT 0, "
					+ "tunai numeric(18,2) DEFAULT 0, kredit numeric(18,2) DEFAULT 0, "
					+ "idempotency_key varchar(128), "
					+ POSTING + ", " + JEJAK + ")",
			"CREATE INDEX idx_{SU}_trip_nota_induk ON {S}.sales_trip_nota (sales_trip_id)",
			"CREATE INDEX idx_{SU}_trip_nota_nomor ON {S}.sales_trip_nota (nomor_nota)",
			"CREATE INDEX idx_{SU}_trip_nota_tanggal ON {S}.sales_trip_nota (tanggal)",
			"CREATE INDEX idx_{SU}_trip_nota_customer ON {S}.sales_trip_nota (customer_id)",
			"CREATE INDEX idx_{SU}_trip_nota_idem ON {S}.sales_trip_nota (idempotency_key)",

			"CREATE TABLE {S}.sales_trip_hasil ("
					+ "id bigserial PRIMARY KEY, "
					+ "sales_trip_id bigint NOT NULL REFERENCES {S}.sales_trip(id), "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "produk_batch_id bigint REFERENCES {S}.produk_batch(id), "
					+ "kuantitas_terjual numeric(18,4) DEFAULT 0, "
					+ "kuantitas_kembali numeric(18,4) DEFAULT 0, "
					+ "kuantitas_rusak numeric(18,4) DEFAULT 0, "
					+ "selisih numeric(18,4) DEFAULT 0, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_trip_hasil_induk ON {S}.sales_trip_hasil (sales_trip_id)",
			"CREATE INDEX idx_{SU}_trip_hasil_produk ON {S}.sales_trip_hasil (produk_id)",

			"CREATE TABLE {S}.sales_trip_biaya ("
					+ "id bigserial PRIMARY KEY, "
					+ "sales_trip_id bigint NOT NULL REFERENCES {S}.sales_trip(id), "
					+ "kategori varchar(64), "
					+ "keterangan text, "
					+ "nilai numeric(18,2) NOT NULL, "
					+ "tanggal date, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_trip_biaya_induk ON {S}.sales_trip_biaya (sales_trip_id)",

			"CREATE TABLE {S}.sales_trip_setoran ("
					+ "id bigserial PRIMARY KEY, "
					+ "sales_trip_id bigint NOT NULL REFERENCES {S}.sales_trip(id), "
					+ "tanggal date NOT NULL, "
					+ "cara_bayar varchar(32), "
					+ "nomor_bukti varchar(64), "
					+ "nilai numeric(18,2) NOT NULL, "
					+ "idempotency_key varchar(128), "
					+ POSTING + ", " + JEJAK + ")",
			"CREATE INDEX idx_{SU}_trip_setoran_induk ON {S}.sales_trip_setoran (sales_trip_id)",
			"CREATE INDEX idx_{SU}_trip_setoran_tanggal ON {S}.sales_trip_setoran (tanggal)",
			"CREATE INDEX idx_{SU}_trip_setoran_idem ON {S}.sales_trip_setoran (idempotency_key)",

			"CREATE TABLE {S}.sales_trip_rekonsiliasi ("
					+ "id bigserial PRIMARY KEY, "
					+ "sales_trip_id bigint NOT NULL REFERENCES {S}.sales_trip(id), "
					+ "tanggal date NOT NULL, "
					+ "nilai_barang_bawa numeric(18,2) DEFAULT 0, "
					+ "nilai_barang_kembali numeric(18,2) DEFAULT 0, "
					+ "nilai_penjualan numeric(18,2) DEFAULT 0, "
					+ "nilai_biaya numeric(18,2) DEFAULT 0, "
					+ "nilai_setoran numeric(18,2) DEFAULT 0, "
					+ "selisih numeric(18,2) DEFAULT 0, "
					+ "keterangan text, "
					+ "disetujui_oleh varchar(255), disetujui_pada timestamp, "
					+ POSTING + ", " + JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_trip_rekonsiliasi UNIQUE (sales_trip_id))",
			"CREATE INDEX idx_{SU}_trip_rekon_tanggal ON {S}.sales_trip_rekonsiliasi (tanggal)",
			"CREATE INDEX idx_{SU}_trip_rekon_status ON {S}.sales_trip_rekonsiliasi (status)",
	};
}
