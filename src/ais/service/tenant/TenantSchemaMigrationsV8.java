package ais.service.tenant;

/**
 * <h3>Migrasi tenant v8 — idempotensi, cetak, dan impor legacy (P3).</h3>
 *
 * <p>Tabel operasional yang menopang P5: impor DBF bertahap, dapat diulang, dan dapat
 * dipertanggungjawabkan.</p>
 *
 * <h4>Staging berlapis: run &rarr; file &rarr; row</h4>
 * <p>Impor tidak menulis langsung ke tabel bisnis. Setiap baris sumber mendarat di
 * {@code legacy_import_row} lebih dulu, lengkap dengan nomor recordnya, tanda hapus aslinya,
 * dan hash barisnya. Itu yang membuat {@code --resume} mungkin, dan yang membuat pertanyaan
 * "baris ini berasal dari mana" terjawab setahun kemudian.</p>
 *
 * <h4>{@code legacy_key_map} adalah jembatan yang tidak boleh hilang</h4>
 * <p>Kode legacy ({@code KODECUST}, {@code KODESALES}, {@code NOFAKTUR}) dipetakan ke id baru
 * di sini. Tanpa peta ini, impor ulang akan menggandakan data alih-alih mengenalinya — dan
 * rekonsiliasi tidak dapat menunjuk balik ke sumbernya. Unique-nya
 * (entity, legacy_key) menjadikan impor idempoten pada tingkat baris.</p>
 *
 * <h4>Kandidat duplikat masuk antrean, bukan digabung</h4>
 * <p>{@code legacy_import_exception} menampung baris yang perlu keputusan manusia — misalnya
 * kode customer {@code 00375} yang <b>benar-benar ganda</b> pada arsip (334 baris, 333 kode
 * unik), serta pasangan nama berulang seperti DEDI TK dan ZAHRA TK yang berkode berbeda.
 * §24 butir 12 melarang penggabungan otomatis.</p>
 *
 * <h4>Kata sandi legacy tidak diimpor</h4>
 * <p>{@code USERS.DBF} memuat kolom {@code PSW}. Tidak ada kolom mana pun di sini yang
 * menampungnya, dan itu disengaja (§24 butir 13, §25.4). Kredensial disetel ulang, bukan
 * dipindahkan.</p>
 */
public final class TenantSchemaMigrationsV8 {

	/** Kelas utilitas murni statis — tidak pernah diinstansiasi. */
	private TenantSchemaMigrationsV8() {
	}

	// PERINGATAN: masuk ke teks DDL kanonik -- mengubahnya mengubah checksum v8.
	/** Fragmen kolom jejak audit ringan (pembuat/pengubah + waktunya), dipakai tabel run impor. */
	private static final String JEJAK =
			"dibuat_pada timestamp, tanggal_dirubah timestamp, "
			+ "oleh varchar(255), olehid varchar(255)";

	/**
	 * Katalog DDL kanonik migrasi v8: {@code CREATE TABLE}/{@code CREATE INDEX} berurutan
	 * untuk tabel operasional yang menopang P5 — {@code idempotency_record},
	 * {@code print_log}, dan staging impor berlapis run &rarr; file &rarr; row
	 * ({@code legacy_import_run}, {@code legacy_import_file}, {@code legacy_import_row}),
	 * jembatan kode lama ke id baru ({@code legacy_key_map}), antrean keputusan manusia
	 * ({@code legacy_import_exception}), dan hasil pencocokan angka pasca-impor
	 * ({@code legacy_reconciliation}). Lihat javadoc kelas untuk penjelasan tiap tabel dan
	 * catatan bahwa kolom kata sandi legacy ({@code USERS.DBF.PSW}) sengaja tidak diimpor.
	 * Dikonsumsi oleh {@link TenantSchemaMigrations#SEMUA} lewat entri bertarget
	 * {@code TARGET_ERP}; penanda {@code {S}}/{@code {A}}/{@code {SU}} disubstitusi saat
	 * migrasi diterapkan oleh {@code TenantSchemaService#terapkanMigrasi}. Isi array ini
	 * bagian dari checksum kanonik v8 — lihat peringatan di atas sebelum mengubah elemen
	 * mana pun.
	 */
	public static final String[] ERP = {
			"CREATE TABLE {S}.idempotency_record ("
					+ "id bigserial PRIMARY KEY, "
					+ "idempotency_key varchar(128) NOT NULL, "
					+ "aksi varchar(128) NOT NULL, "
					+ "user_id varchar(255), device_id varchar(128), "
					+ "request_id varchar(64), correlation_id varchar(64), "
					+ "hasil_ringkas text, "
					+ "waktu timestamp NOT NULL, "
					+ "CONSTRAINT uq_{SU}_idempotency UNIQUE (idempotency_key, aksi))",
			"CREATE INDEX idx_{SU}_idempotency_waktu ON {S}.idempotency_record (waktu)",
			"CREATE INDEX idx_{SU}_idempotency_correlation ON {S}.idempotency_record (correlation_id)",

			"CREATE TABLE {S}.print_log ("
					+ "id bigserial PRIMARY KEY, "
					+ "dokumen_tipe varchar(64) NOT NULL, "
					+ "dokumen_id bigint, "
					+ "nomor_dokumen varchar(64), "
					+ "cetakan_ke integer DEFAULT 1, "
					+ "user_id varchar(255), role varchar(64), device_id varchar(128), "
					+ "alasan text, "
					+ "waktu timestamp NOT NULL)",
			"CREATE INDEX idx_{SU}_print_log_dokumen ON {S}.print_log (dokumen_tipe, dokumen_id)",
			"CREATE INDEX idx_{SU}_print_log_nomor ON {S}.print_log (nomor_dokumen)",
			"CREATE INDEX idx_{SU}_print_log_waktu ON {S}.print_log (waktu)",

			"CREATE TABLE {S}.legacy_import_run ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode_run varchar(64) NOT NULL, "
					+ "sumber_keterangan text, "
					+ "archive_sha256 varchar(64), "
					+ "dry_run boolean DEFAULT false, "
					+ "status varchar(32) DEFAULT 'BERJALAN', "
					+ "dimulai_pada timestamp, selesai_pada timestamp, "
					+ "baris_dibaca bigint DEFAULT 0, baris_ditulis bigint DEFAULT 0, "
					+ "baris_dilewati bigint DEFAULT 0, baris_exception bigint DEFAULT 0, "
					+ "pesan text, "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_import_run_kode UNIQUE (kode_run))",
			"CREATE INDEX idx_{SU}_import_run_status ON {S}.legacy_import_run (status)",

			"CREATE TABLE {S}.legacy_import_file ("
					+ "id bigserial PRIMARY KEY, "
					+ "legacy_import_run_id bigint NOT NULL REFERENCES {S}.legacy_import_run(id), "
					+ "nama_file varchar(128) NOT NULL, "
					+ "sha256 varchar(64), "
					+ "jumlah_header bigint, jumlah_aktif bigint, jumlah_terhapus bigint, "
					+ "status varchar(32) DEFAULT 'MENUNGGU', "
					+ "diproses_pada timestamp, "
					+ "pesan text)",
			"CREATE INDEX idx_{SU}_import_file_run ON {S}.legacy_import_file (legacy_import_run_id)",
			"CREATE INDEX idx_{SU}_import_file_nama ON {S}.legacy_import_file (nama_file)",

			"CREATE TABLE {S}.legacy_import_row ("
					+ "id bigserial PRIMARY KEY, "
					+ "legacy_import_file_id bigint NOT NULL REFERENCES {S}.legacy_import_file(id), "
					+ "source_record_no integer NOT NULL, "
					+ "source_deleted boolean DEFAULT false, "
					+ "row_hash varchar(64), "
					+ "muatan text, "
					+ "status varchar(32) DEFAULT 'STAGED', "
					+ "target_entity varchar(128), target_id bigint, "
					+ "pesan text)",
			"CREATE INDEX idx_{SU}_import_row_file ON {S}.legacy_import_row (legacy_import_file_id)",
			"CREATE INDEX idx_{SU}_import_row_hash ON {S}.legacy_import_row (row_hash)",
			"CREATE INDEX idx_{SU}_import_row_status ON {S}.legacy_import_row (status)",
			"CREATE INDEX idx_{SU}_import_row_target ON {S}.legacy_import_row (target_entity, target_id)",

			"CREATE TABLE {S}.legacy_key_map ("
					+ "id bigserial PRIMARY KEY, "
					+ "entity varchar(128) NOT NULL, "
					+ "legacy_key varchar(128) NOT NULL, "
					+ "legacy_source_file varchar(128), "
					+ "target_id bigint NOT NULL, "
					+ "legacy_import_run_id bigint REFERENCES {S}.legacy_import_run(id), "
					+ "dibuat_pada timestamp, "
					+ "CONSTRAINT uq_{SU}_legacy_key_map UNIQUE (entity, legacy_key))",
			"CREATE INDEX idx_{SU}_legacy_key_map_target ON {S}.legacy_key_map (entity, target_id)",

			"CREATE TABLE {S}.legacy_import_exception ("
					+ "id bigserial PRIMARY KEY, "
					+ "legacy_import_run_id bigint NOT NULL REFERENCES {S}.legacy_import_run(id), "
					+ "legacy_import_row_id bigint REFERENCES {S}.legacy_import_row(id), "
					+ "kategori varchar(64) NOT NULL, "
					+ "entity varchar(128), legacy_key varchar(128), "
					+ "keterangan text, "
					+ "status varchar(32) DEFAULT 'TERBUKA', "
					+ "diselesaikan_oleh varchar(255), diselesaikan_pada timestamp, "
					+ "keputusan text, "
					+ "dibuat_pada timestamp)",
			"CREATE INDEX idx_{SU}_import_exc_run ON {S}.legacy_import_exception (legacy_import_run_id)",
			"CREATE INDEX idx_{SU}_import_exc_kategori ON {S}.legacy_import_exception (kategori)",
			"CREATE INDEX idx_{SU}_import_exc_status ON {S}.legacy_import_exception (status)",

			"CREATE TABLE {S}.legacy_reconciliation ("
					+ "id bigserial PRIMARY KEY, "
					+ "legacy_import_run_id bigint NOT NULL REFERENCES {S}.legacy_import_run(id), "
					+ "metrik varchar(128) NOT NULL, "
					+ "nilai_sumber numeric(20,4), "
					+ "nilai_staging numeric(20,4), "
					+ "nilai_target numeric(20,4), "
					+ "selisih numeric(20,4), "
					+ "lulus boolean, "
					+ "keterangan text, "
					+ "dihitung_pada timestamp)",
			"CREATE INDEX idx_{SU}_rekon_run ON {S}.legacy_reconciliation (legacy_import_run_id)",
			"CREATE INDEX idx_{SU}_rekon_metrik ON {S}.legacy_reconciliation (metrik)",
			"CREATE INDEX idx_{SU}_rekon_lulus ON {S}.legacy_reconciliation (lulus)",
	};
}
