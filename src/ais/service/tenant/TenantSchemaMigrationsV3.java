package ais.service.tenant;

/**
 * <h3>Migrasi tenant v3 — produk, stok, dan harga (P3).</h3>
 *
 * <p>Sumber legacy: {@code STOK.DBF} &rarr; {@code produk}, {@code BATCHNO.DBF} &rarr;
 * {@code produk_batch}, {@code DATAOPN.DBF} &rarr; {@code stok_opname}, {@code MASTERBL.DBF}
 * &rarr; {@code harga_beli_supplier}, {@code MASTERJL.DBF} &rarr; {@code harga_jual_customer}.</p>
 *
 * <h4>Harga berversi, bukan ditimpa</h4>
 * <p>{@code harga_beli_supplier} dan {@code harga_jual_customer} menyimpan
 * {@code berlaku_dari}/{@code berlaku_sampai}, bukan satu baris yang ditimpa setiap kali harga
 * berubah. Pertanyaan "berapa harga barang ini pada tanggal transaksi X" karena itu dijawab
 * tabel bisnis dan tetap terjawab walau catatan audit dipangkas.</p>
 *
 * <p>Arsip legacy memuat pasangan berulang — sepuluh pada MASTERBL (satu di antaranya rangkap
 * tiga) dan tiga puluh tujuh pada MASTERJL. Karena itu <b>tidak ada unique constraint</b> pada
 * (supplier, produk) maupun (customer, produk); yang dipakai adalah rentang berlaku, dan
 * duplikat legacy masuk antrean pembersihan lewat {@code kandidat_duplikat}. Memasang unique
 * di sana akan menggagalkan impor pada baris yang justru perlu diperiksa manusia.</p>
 *
 * <h4>Saldo dan mutasi terpisah</h4>
 * <p>{@code mutasi_stok} append-only — setiap pergerakan satu baris, koreksi memakai baris
 * pembalik, tidak pernah UPDATE. {@code saldo_stok} adalah ringkasan turunan per
 * (produk, gudang, lokasi, batch) yang boleh dihitung ulang dari mutasi. Menyimpan saldo saja
 * membuat selisih tidak dapat ditelusuri; menyimpan mutasi saja membuat setiap layar stok
 * memindai jutaan baris.</p>
 *
 * <p>Kuantitas {@code numeric(18,4)} — arsip legacy memuat jumlah pecahan. Uang
 * {@code numeric(18,2)}, tidak pernah float (§11.4).</p>
 */
public final class TenantSchemaMigrationsV3 {

	private TenantSchemaMigrationsV3() {
	}

	// PERINGATAN: konstanta ini masuk ke teks DDL kanonik, sehingga MENGUBAHNYA MENGUBAH
	// CHECKSUM v3 dan membuat seluruh tenant yang sudah memasang v3 gagal keras. Bila perlu
	// bentuk lain, buat bundel versi BARU -- jangan sunting yang ini.
	private static final String LEGACY =
			"legacy_source_file varchar(128), legacy_source_record_no integer, "
			+ "legacy_row_hash varchar(64), legacy_import_run_id bigint, "
			+ "legacy_deleted boolean DEFAULT false";

	private static final String JEJAK =
			"dibuat_pada timestamp, tanggal_dirubah timestamp, "
			+ "oleh varchar(255), olehid varchar(255)";

	/**
	 * DDL kanonik v3 untuk schema ERP tenant, dieksekusi oleh
	 * {@link TenantSchemaService#terapkanMigrasi} sebagai bundel {@code v3-inventory-stock-erp}.
	 * Empat kelompok tabel, urut sesuai dependensi FK:
	 * <ol>
	 * <li><b>Master produk</b> -- {@code satuan}, {@code kategori_produk} (self-referencing
	 * lewat {@code induk_id}), {@code produk}, dan {@code produk_batch} (untuk produk yang
	 * memakai nomor batch/tanggal kedaluwarsa).</li>
	 * <li><b>Stok</b> -- {@code mutasi_stok} (append-only, setiap pergerakan satu baris, koreksi
	 * lewat baris pembalik via {@code pembalik_dari_id}, tidak pernah UPDATE) dan
	 * {@code saldo_stok} (ringkasan turunan per produk/gudang/lokasi/batch, boleh dihitung ulang
	 * dari {@code mutasi_stok}).</li>
	 * <li><b>Opname</b> -- {@code stok_opname} (dokumen induk) dan {@code stok_opname_detail}
	 * (selisih sistem vs fisik per produk).</li>
	 * <li><b>Harga</b> -- {@code harga_beli_supplier} dan {@code harga_jual_customer}
	 * (berversi lewat {@code berlaku_dari}/{@code berlaku_sampai}, tanpa unique constraint
	 * karena duplikat legacy ditangani lewat {@code kandidat_duplikat}), serta
	 * {@code price_list}/{@code price_list_detail}.</li>
	 * </ol>
	 * Tabel legacy-import memakai kolom {@link #LEGACY}, semua tabel memakai kolom jejak
	 * {@link #JEJAK}. Array ini bagian dari DDL kanonik ber-checksum -- lihat peringatan pada
	 * {@link #LEGACY}/{@link #JEJAK} dan javadoc kelas: TIDAK BOLEH diubah setelah dirilis.
	 */
	public static final String[] ERP = {
			// ---------- Master produk ----------
			"CREATE TABLE {S}.satuan ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_satuan_kode UNIQUE (kode))",

			"CREATE TABLE {S}.kategori_produk ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "induk_id bigint REFERENCES {S}.kategori_produk(id), "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_kategori_produk_kode UNIQUE (kode))",

			"CREATE TABLE {S}.produk ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "kategori_produk_id bigint REFERENCES {S}.kategori_produk(id), "
					+ "satuan_id bigint REFERENCES {S}.satuan(id), "
					+ "barcode varchar(64), "
					+ "harga_beli_terakhir numeric(18,2), "
					+ "harga_jual_standar numeric(18,2), "
					+ "stok_minimum numeric(18,4), "
					+ "pakai_batch boolean DEFAULT false, "
					+ "pakai_expiry boolean DEFAULT false, "
					+ "status varchar(32) DEFAULT 'AKTIF', "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", " + LEGACY + ", "
					+ "CONSTRAINT uq_{SU}_produk_kode UNIQUE (kode))",
			"CREATE INDEX idx_{SU}_produk_nama ON {S}.produk (nama)",
			"CREATE INDEX idx_{SU}_produk_status ON {S}.produk (status)",
			"CREATE INDEX idx_{SU}_produk_kategori ON {S}.produk (kategori_produk_id)",
			"CREATE INDEX idx_{SU}_produk_barcode ON {S}.produk (barcode)",
			"CREATE INDEX idx_{SU}_produk_legacy ON {S}.produk "
					+ "(legacy_source_file, legacy_source_record_no)",
			"CREATE INDEX idx_{SU}_produk_rowhash ON {S}.produk (legacy_row_hash)",

			"CREATE TABLE {S}.produk_batch ("
					+ "id bigserial PRIMARY KEY, "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "batch_no varchar(64) NOT NULL, "
					+ "expiry_date date, "
					+ "harga_beli numeric(18,2), "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_produk_batch_produk ON {S}.produk_batch (produk_id)",
			"CREATE INDEX idx_{SU}_produk_batch_no ON {S}.produk_batch (batch_no)",
			"CREATE INDEX idx_{SU}_produk_batch_expiry ON {S}.produk_batch (expiry_date)",
			"CREATE INDEX idx_{SU}_produk_batch_legacy ON {S}.produk_batch "
					+ "(legacy_source_file, legacy_source_record_no)",

			// ---------- Stok ----------
			"CREATE TABLE {S}.mutasi_stok ("
					+ "id bigserial PRIMARY KEY, "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "gudang_id bigint REFERENCES {S}.gudang(id), "
					+ "lokasi_stok_id bigint REFERENCES {S}.lokasi_stok(id), "
					+ "produk_batch_id bigint REFERENCES {S}.produk_batch(id), "
					+ "tanggal date NOT NULL, "
					+ "jenis varchar(32) NOT NULL, "
					+ "arah smallint NOT NULL, "
					+ "kuantitas numeric(18,4) NOT NULL, "
					+ "harga_satuan numeric(18,2), "
					+ "nilai numeric(18,2), "
					+ "dokumen_tipe varchar(64), "
					+ "dokumen_id bigint, "
					+ "nomor_dokumen varchar(64), "
					+ "pembalik_dari_id bigint REFERENCES {S}.mutasi_stok(id), "
					+ "keterangan text, "
					+ "idempotency_key varchar(128), "
					+ "correlation_id varchar(64), "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_mutasi_stok_produk ON {S}.mutasi_stok (produk_id)",
			"CREATE INDEX idx_{SU}_mutasi_stok_tanggal ON {S}.mutasi_stok (tanggal)",
			"CREATE INDEX idx_{SU}_mutasi_stok_gudang ON {S}.mutasi_stok (gudang_id)",
			"CREATE INDEX idx_{SU}_mutasi_stok_lokasi ON {S}.mutasi_stok (lokasi_stok_id)",
			"CREATE INDEX idx_{SU}_mutasi_stok_batch ON {S}.mutasi_stok (produk_batch_id)",
			"CREATE INDEX idx_{SU}_mutasi_stok_dokumen ON {S}.mutasi_stok (dokumen_tipe, dokumen_id)",
			"CREATE INDEX idx_{SU}_mutasi_stok_nomor ON {S}.mutasi_stok (nomor_dokumen)",
			"CREATE INDEX idx_{SU}_mutasi_stok_idem ON {S}.mutasi_stok (idempotency_key)",
			"CREATE INDEX idx_{SU}_mutasi_stok_correlation ON {S}.mutasi_stok (correlation_id)",

			"CREATE TABLE {S}.saldo_stok ("
					+ "id bigserial PRIMARY KEY, "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "gudang_id bigint REFERENCES {S}.gudang(id), "
					+ "lokasi_stok_id bigint REFERENCES {S}.lokasi_stok(id), "
					+ "produk_batch_id bigint REFERENCES {S}.produk_batch(id), "
					+ "kuantitas numeric(18,4) NOT NULL DEFAULT 0, "
					+ "nilai numeric(18,2) NOT NULL DEFAULT 0, "
					+ "dihitung_pada timestamp, "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_saldo_stok_produk ON {S}.saldo_stok (produk_id)",
			"CREATE INDEX idx_{SU}_saldo_stok_gudang ON {S}.saldo_stok (gudang_id)",
			"CREATE INDEX idx_{SU}_saldo_stok_lokasi ON {S}.saldo_stok (lokasi_stok_id)",
			"CREATE INDEX idx_{SU}_saldo_stok_batch ON {S}.saldo_stok (produk_batch_id)",

			// ---------- Opname ----------
			"CREATE TABLE {S}.stok_opname ("
					+ "id bigserial PRIMARY KEY, "
					+ "nomor_dokumen varchar(64) NOT NULL, "
					+ "tanggal date NOT NULL, "
					+ "gudang_id bigint REFERENCES {S}.gudang(id), "
					+ "status varchar(32) DEFAULT 'DRAF', "
					+ "keterangan text, "
					+ "diposting boolean DEFAULT false, "
					+ "diposting_pada timestamp, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_stok_opname_nomor ON {S}.stok_opname (nomor_dokumen)",
			"CREATE INDEX idx_{SU}_stok_opname_tanggal ON {S}.stok_opname (tanggal)",
			"CREATE INDEX idx_{SU}_stok_opname_status ON {S}.stok_opname (status)",
			"CREATE INDEX idx_{SU}_stok_opname_gudang ON {S}.stok_opname (gudang_id)",

			"CREATE TABLE {S}.stok_opname_detail ("
					+ "id bigserial PRIMARY KEY, "
					+ "stok_opname_id bigint NOT NULL REFERENCES {S}.stok_opname(id), "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "produk_batch_id bigint REFERENCES {S}.produk_batch(id), "
					+ "lokasi_stok_id bigint REFERENCES {S}.lokasi_stok(id), "
					+ "kuantitas_sistem numeric(18,4), "
					+ "kuantitas_fisik numeric(18,4), "
					+ "selisih numeric(18,4), "
					+ "harga_satuan numeric(18,2), "
					+ "keterangan text, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_stok_opname_detail_induk ON {S}.stok_opname_detail (stok_opname_id)",
			"CREATE INDEX idx_{SU}_stok_opname_detail_produk ON {S}.stok_opname_detail (produk_id)",

			// ---------- Harga ----------
			"CREATE TABLE {S}.harga_beli_supplier ("
					+ "id bigserial PRIMARY KEY, "
					+ "supplier_id bigint NOT NULL REFERENCES {S}.supplier(id), "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "harga numeric(18,2) NOT NULL, "
					+ "diskon numeric(9,4), "
					+ "berlaku_dari date, berlaku_sampai date, "
					+ "kandidat_duplikat boolean DEFAULT false, "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_harga_beli_supplier ON {S}.harga_beli_supplier (supplier_id)",
			"CREATE INDEX idx_{SU}_harga_beli_produk ON {S}.harga_beli_supplier (produk_id)",
			"CREATE INDEX idx_{SU}_harga_beli_berlaku ON {S}.harga_beli_supplier (berlaku_dari)",
			"CREATE INDEX idx_{SU}_harga_beli_legacy ON {S}.harga_beli_supplier "
					+ "(legacy_source_file, legacy_source_record_no)",

			"CREATE TABLE {S}.harga_jual_customer ("
					+ "id bigserial PRIMARY KEY, "
					+ "customer_id bigint NOT NULL REFERENCES {S}.customer(id), "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "harga numeric(18,2) NOT NULL, "
					+ "diskon numeric(9,4), "
					+ "berlaku_dari date, berlaku_sampai date, "
					+ "kandidat_duplikat boolean DEFAULT false, "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_harga_jual_customer ON {S}.harga_jual_customer (customer_id)",
			"CREATE INDEX idx_{SU}_harga_jual_produk ON {S}.harga_jual_customer (produk_id)",
			"CREATE INDEX idx_{SU}_harga_jual_berlaku ON {S}.harga_jual_customer (berlaku_dari)",
			"CREATE INDEX idx_{SU}_harga_jual_legacy ON {S}.harga_jual_customer "
					+ "(legacy_source_file, legacy_source_record_no)",

			"CREATE TABLE {S}.price_list ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "berlaku_dari date, berlaku_sampai date, "
					+ "status varchar(32) DEFAULT 'DRAF', "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_price_list_kode UNIQUE (kode))",
			"CREATE INDEX idx_{SU}_price_list_status ON {S}.price_list (status)",

			"CREATE TABLE {S}.price_list_detail ("
					+ "id bigserial PRIMARY KEY, "
					+ "price_list_id bigint NOT NULL REFERENCES {S}.price_list(id), "
					+ "produk_id bigint NOT NULL REFERENCES {S}.produk(id), "
					+ "harga numeric(18,2) NOT NULL, "
					+ "diskon numeric(9,4), "
					+ "minimum_kuantitas numeric(18,4), "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_price_list_detail_induk ON {S}.price_list_detail (price_list_id)",
			"CREATE INDEX idx_{SU}_price_list_detail_produk ON {S}.price_list_detail (produk_id)",
	};

	// TIDAK ADA bundel AUDIT untuk v3, dan itu disengaja.
	//
	// audit_baris generik dari v2 sudah menampung seluruh tabel di sini, jadi tidak ada DDL
	// audit yang perlu dijalankan. Mendaftarkan bundel KOSONG bernama "v3-inventory-stock-audit"
	// justru berbahaya: checksum-nya akan tercatat pada setiap tenant yang memasangnya, sehingga
	// menambahkan DDL ke slot itu kelak mengubah checksum dan membuat migrasi GAGAL KERAS di
	// seluruh tenant. Bila suatu saat v3 butuh DDL audit, tambahkan versi BARU di akhir katalog.
}
