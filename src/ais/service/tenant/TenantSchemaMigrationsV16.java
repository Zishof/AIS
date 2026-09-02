package ais.service.tenant;

/**
 * <h3>Migrasi tenant v16 — pembelian yang dilakukan sales dalam perjalanan.</h3>
 *
 * <p>Satu tabel: {@code sales_trip_pembelian}. Ia penghalang katalog <b>terakhir</b> pada helper
 * Trip, dan menutupnya membuat kedua aksi yang tersisa — {@code tripPurchaseLink} dan
 * {@code tripDetail} — dapat dipindahkan.</p>
 *
 * <h4>Apa yang dicatatnya</h4>
 * <p>Sales yang berkeliling kadang membeli barang di jalan: menebus faktur pemasok, membayar
 * sebagiannya dari kas yang dipegang, dan memutuskan barangnya masuk ke mobil atau langsung ke
 * gudang. Jalur legacy mencatatnya sebagai {@code NotaSalesPembelian} yang menggantung pada
 * sesinya.</p>
 * <p>Tanpa tabel ini, dua hal ikut hilang: pembayaran pemasok dari kas trip tidak punya dokumen
 * pendamping, dan rekap penutupan tidak dapat menyatakan berapa yang dibayarkan ke pemasok —
 * angka yang selama ini terpaksa dinyatakan <b>nol menurut definisi</b> pada
 * {@code tripClose}.</p>
 *
 * <h4>{@code sisaHutang} TIDAK dibuatkan kolom</h4>
 * <p>Entitas legacy menyimpannya sebagai kolom ketiga di samping {@code totalFaktur} dan
 * {@code dibayarSesi}. Nilainya persis {@code totalFaktur − dibayarSesi} — aritmetika dua kolom
 * yang berada pada baris yang sama.</p>
 * <p>Kolom semacam itu tidak menambah apa pun kecuali kesempatan untuk berselisih: satu
 * pembaruan yang lupa menyentuhnya sudah cukup membuat sisa hutang berbohong sementara kedua
 * angka penyusunnya benar. Diturunkan saat dibaca.</p>
 *
 * <h4>Dua kaitan yang boleh kosong</h4>
 * <p>{@code pembelian_id} dan {@code supplier_id} keduanya {@code NULL}-able, mengikuti jalur
 * legacy yang menyetel keduanya hanya bila permintaan menyebutkannya. Sales di lapangan tidak
 * selalu tahu nomor faktur pengadaan saat mencatat, dan memaksanya akan membuat pencatatan
 * tertunda sampai kembali ke kantor — persis yang hendak dihindari fitur ini.</p>
 *
 * <h4>Tujuan stok disimpan apa adanya</h4>
 * <p>{@code tujuan_stok} bernilai {@code MOBIL_SALES} atau {@code GUDANG}, sama persis dengan
 * konstanta legacy. Tidak dipasang batasan {@code CHECK}: baris hasil impor boleh memuat nilai
 * lain, dan menolaknya saat impor akan menghentikan pemindahan data karena satu kolom
 * keterangan.</p>
 *
 * <h4>Apa yang TIDAK dilakukan bundel ini</h4>
 * <p>Tabel ini <b>tidak</b> menyentuh persediaan. Legacy pun tidak: {@code tujuan_stok} hanyalah
 * keterangan niat, dan barangnya baru benar-benar masuk lewat jalur penerimaan tersendiri.
 * Menjadikannya pemicu mutasi stok di sini akan membukukan barang dua kali.</p>
 */
public final class TenantSchemaMigrationsV16 {

	private TenantSchemaMigrationsV16() {
	}

	public static final String[] ERP = {

			"CREATE TABLE {S}.sales_trip_pembelian ("
					+ "id bigserial PRIMARY KEY, "
					+ "sales_trip_id bigint NOT NULL REFERENCES {S}.sales_trip(id), "
					+ "pembelian_id bigint REFERENCES {S}.pembelian(id), "
					+ "supplier_id bigint REFERENCES {S}.supplier(id), "
					+ "total_faktur numeric(18,2) NOT NULL, "
					+ "dibayar_trip numeric(18,2) NOT NULL DEFAULT 0, "
					// sisa_hutang TIDAK disimpan: total_faktur - dibayar_trip, dihitung saat baca.
					+ "tujuan_stok varchar(32) DEFAULT 'MOBIL_SALES', "
					+ "keterangan text, "
					+ "idempotency_key varchar(128), "
					+ "correlation_id varchar(64), "
					+ "dibuat_pada timestamp, "
					+ "tanggal_dirubah timestamp, "
					+ "oleh varchar(255), "
					+ "olehid varchar(255), "
					+ "legacy_source_file varchar(128), "
					+ "legacy_source_record_no integer, "
					+ "legacy_row_hash varchar(64), "
					+ "legacy_import_run_id bigint, "
					+ "legacy_deleted boolean DEFAULT false, "
					+ "legacy_tafsir varchar(64))",

			"CREATE INDEX idx_{SU}_trip_pembelian_trip ON {S}.sales_trip_pembelian"
					+ " (sales_trip_id)",

			"CREATE INDEX idx_{SU}_trip_pembelian_supplier ON {S}.sales_trip_pembelian"
					+ " (supplier_id)",

			// Sebentuk dengan indeks idempotensi yang dipasang v10-v13.
			"CREATE UNIQUE INDEX uq_{SU}_trip_pembelian_idem ON {S}.sales_trip_pembelian"
					+ " (idempotency_key) WHERE idempotency_key IS NOT NULL" };
}
