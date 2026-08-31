package ais.service.tenant;

/**
 * <h3>Migrasi tenant v7 — akuntansi tenant (P3).</h3>
 *
 * <p>Bagan akun, periode, jurnal, dan jejak posting/pembalikan — semuanya <b>milik tenant</b>,
 * bukan {@code akunting.*} bersama. §12.1 menegaskan bahwa data tenant tidak boleh kanonik di
 * schema global; buku besar adalah contoh paling jelasnya.</p>
 *
 * <h4>Posting dan pembalikan dicatat, bukan disimpulkan</h4>
 * <p>{@code posting_log} dan {@code reversal_log} berdiri terpisah dari kolom
 * {@code diposting} pada dokumen. Kolom itu menjawab "apakah sudah diposting"; kedua tabel ini
 * menjawab "kapan, oleh siapa, dari dokumen mana, ke jurnal mana, dan mengapa dibatalkan" —
 * pertanyaan yang justru muncul ketika angkanya diperdebatkan.</p>
 *
 * <h4>Periode terkunci menolak, bukan memperingatkan</h4>
 * <p>{@code periode_akuntansi.status} = {@code TERTUTUP} adalah gerbang keras bagi posting.
 * Peringatan yang dapat dilewati membuat periode yang sudah dilaporkan berubah setelahnya.</p>
 *
 * <h4>Jurnal seimbang dijaga aplikasi, bukan constraint</h4>
 * <p>PostgreSQL 9.3 tidak punya constraint lintas-baris yang praktis untuk memaksa
 * debit = kredit per jurnal. Penjagaannya di lapisan posting, dan {@code jurnal.total_debit} /
 * {@code total_kredit} disimpan supaya ketidakseimbangan dapat <b>ditemukan</b> lewat kueri
 * sederhana bila suatu saat lolos.</p>
 */
public final class TenantSchemaMigrationsV7 {

	/** Kelas utilitas murni statis — tidak pernah diinstansiasi. */
	private TenantSchemaMigrationsV7() {
	}

	// PERINGATAN: masuk ke teks DDL kanonik -- mengubahnya mengubah checksum v7.
	/** Fragmen kolom jejak audit ringan (pembuat/pengubah + waktunya), dipakai seluruh tabel. */
	private static final String JEJAK =
			"dibuat_pada timestamp, tanggal_dirubah timestamp, "
			+ "oleh varchar(255), olehid varchar(255)";

	/**
	 * Fragmen kolom provenans impor legacy, dipakai tabel akuntansi yang sebagian isinya
	 * diturunkan dari arsip lama ({@code akun}, {@code jurnal}): nama berkas dan nomor baris
	 * sumber, hash baris, id run impor, dan penanda {@code legacy_deleted}.
	 */
	private static final String LEGACY =
			"legacy_source_file varchar(128), legacy_source_record_no integer, "
			+ "legacy_row_hash varchar(64), legacy_import_run_id bigint, "
			+ "legacy_deleted boolean DEFAULT false";

	/**
	 * Katalog DDL kanonik migrasi v7: {@code CREATE TABLE}/{@code CREATE INDEX} berurutan
	 * untuk bagan akun ({@code akun}), periode ({@code periode_akuntansi}, ditutup keras —
	 * lihat javadoc kelas), buku besar ({@code jurnal} + {@code jurnal_detail}, keseimbangan
	 * debit/kredit dijaga aplikasi karena PostgreSQL 9.3 tidak punya constraint lintas-baris
	 * yang praktis), dan jejak posting/pembalikan ({@code posting_log}, {@code reversal_log},
	 * dicatat terpisah dari kolom {@code diposting} — lihat javadoc kelas). Seluruhnya milik
	 * tenant, bukan {@code akunting.*} bersama (§12.1). Dikonsumsi oleh
	 * {@link TenantSchemaMigrations#SEMUA} lewat entri bertarget {@code TARGET_ERP}; penanda
	 * {@code {S}}/{@code {A}}/{@code {SU}} disubstitusi saat migrasi diterapkan oleh
	 * {@code TenantSchemaService#terapkanMigrasi}. Isi array ini bagian dari checksum
	 * kanonik v7 — lihat peringatan di atas sebelum mengubah elemen mana pun.
	 */
	public static final String[] ERP = {
			"CREATE TABLE {S}.akun ("
					+ "id bigserial PRIMARY KEY, "
					+ "kode varchar(64) NOT NULL, "
					+ "nama varchar(255) NOT NULL, "
					+ "tipe varchar(32) NOT NULL, "
					+ "induk_id bigint REFERENCES {S}.akun(id), "
					+ "saldo_normal varchar(8), "
					+ "level integer, "
					+ "posting_diizinkan boolean DEFAULT true, "
					+ "aktif boolean DEFAULT true, "
					+ JEJAK + ", " + LEGACY + ", "
					+ "CONSTRAINT uq_{SU}_akun_kode UNIQUE (kode))",
			"CREATE INDEX idx_{SU}_akun_tipe ON {S}.akun (tipe)",
			"CREATE INDEX idx_{SU}_akun_induk ON {S}.akun (induk_id)",
			"CREATE INDEX idx_{SU}_akun_legacy ON {S}.akun "
					+ "(legacy_source_file, legacy_source_record_no)",

			"CREATE TABLE {S}.periode_akuntansi ("
					+ "id bigserial PRIMARY KEY, "
					+ "tahun integer NOT NULL, "
					+ "bulan integer NOT NULL, "
					+ "tanggal_awal date NOT NULL, tanggal_akhir date NOT NULL, "
					+ "status varchar(32) DEFAULT 'TERBUKA', "
					+ "ditutup_oleh varchar(255), ditutup_pada timestamp, "
					+ "dibuka_ulang_oleh varchar(255), dibuka_ulang_pada timestamp, "
					+ "alasan_buka_ulang text, "
					+ JEJAK + ", "
					+ "CONSTRAINT uq_{SU}_periode UNIQUE (tahun, bulan))",
			"CREATE INDEX idx_{SU}_periode_status ON {S}.periode_akuntansi (status)",
			"CREATE INDEX idx_{SU}_periode_tanggal ON {S}.periode_akuntansi (tanggal_awal)",

			"CREATE TABLE {S}.jurnal ("
					+ "id bigserial PRIMARY KEY, "
					+ "nomor_dokumen varchar(64) NOT NULL, "
					+ "tanggal date NOT NULL, "
					+ "periode_akuntansi_id bigint REFERENCES {S}.periode_akuntansi(id), "
					+ "sumber_tipe varchar(64), sumber_id bigint, "
					+ "keterangan text, "
					+ "total_debit numeric(18,2) DEFAULT 0, "
					+ "total_kredit numeric(18,2) DEFAULT 0, "
					+ "pembalik_dari_id bigint REFERENCES {S}.jurnal(id), "
					+ "idempotency_key varchar(128), correlation_id varchar(64), "
					+ "status varchar(32) DEFAULT 'DRAF', "
					+ "diposting boolean DEFAULT false, diposting_pada timestamp, "
					+ "dibatalkan boolean DEFAULT false, dibatalkan_pada timestamp, alasan_batal text, "
					+ JEJAK + ", " + LEGACY + ")",
			"CREATE INDEX idx_{SU}_jurnal_nomor ON {S}.jurnal (nomor_dokumen)",
			"CREATE INDEX idx_{SU}_jurnal_tanggal ON {S}.jurnal (tanggal)",
			"CREATE INDEX idx_{SU}_jurnal_status ON {S}.jurnal (status)",
			"CREATE INDEX idx_{SU}_jurnal_sumber ON {S}.jurnal (sumber_tipe, sumber_id)",
			"CREATE INDEX idx_{SU}_jurnal_periode ON {S}.jurnal (periode_akuntansi_id)",
			"CREATE INDEX idx_{SU}_jurnal_idem ON {S}.jurnal (idempotency_key)",
			"CREATE INDEX idx_{SU}_jurnal_correlation ON {S}.jurnal (correlation_id)",

			"CREATE TABLE {S}.jurnal_detail ("
					+ "id bigserial PRIMARY KEY, "
					+ "jurnal_id bigint NOT NULL REFERENCES {S}.jurnal(id), "
					+ "baris_ke integer, "
					+ "akun_id bigint NOT NULL REFERENCES {S}.akun(id), "
					+ "debit numeric(18,2) DEFAULT 0, "
					+ "kredit numeric(18,2) DEFAULT 0, "
					+ "keterangan text, "
					+ "toko_id bigint REFERENCES {S}.toko(id), "
					+ "gudang_id bigint REFERENCES {S}.gudang(id), "
					+ "salesperson_id bigint REFERENCES {S}.salesperson(id), "
					+ JEJAK + ")",
			"CREATE INDEX idx_{SU}_jurnal_detail_induk ON {S}.jurnal_detail (jurnal_id)",
			"CREATE INDEX idx_{SU}_jurnal_detail_akun ON {S}.jurnal_detail (akun_id)",

			"CREATE TABLE {S}.posting_log ("
					+ "id bigserial PRIMARY KEY, "
					+ "dokumen_tipe varchar(64) NOT NULL, "
					+ "dokumen_id bigint NOT NULL, "
					+ "jurnal_id bigint REFERENCES {S}.jurnal(id), "
					+ "hasil varchar(32) NOT NULL, "
					+ "pesan text, "
					+ "user_id varchar(255), role varchar(64), "
					+ "request_id varchar(64), correlation_id varchar(64), "
					+ "idempotency_key varchar(128), "
					+ "waktu timestamp NOT NULL)",
			"CREATE INDEX idx_{SU}_posting_log_dokumen ON {S}.posting_log (dokumen_tipe, dokumen_id)",
			"CREATE INDEX idx_{SU}_posting_log_waktu ON {S}.posting_log (waktu)",
			"CREATE INDEX idx_{SU}_posting_log_correlation ON {S}.posting_log (correlation_id)",
			"CREATE INDEX idx_{SU}_posting_log_idem ON {S}.posting_log (idempotency_key)",

			"CREATE TABLE {S}.reversal_log ("
					+ "id bigserial PRIMARY KEY, "
					+ "dokumen_tipe varchar(64) NOT NULL, "
					+ "dokumen_id bigint NOT NULL, "
					+ "jurnal_asal_id bigint REFERENCES {S}.jurnal(id), "
					+ "jurnal_pembalik_id bigint REFERENCES {S}.jurnal(id), "
					+ "alasan text NOT NULL, "
					+ "user_id varchar(255), role varchar(64), "
					+ "request_id varchar(64), correlation_id varchar(64), "
					+ "waktu timestamp NOT NULL)",
			"CREATE INDEX idx_{SU}_reversal_log_dokumen ON {S}.reversal_log (dokumen_tipe, dokumen_id)",
			"CREATE INDEX idx_{SU}_reversal_log_waktu ON {S}.reversal_log (waktu)",
	};
}
