package ais.common;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;

/**
 * Migrasi skema retail yang kecil, berurutan, dan tercatat. Kelas ini menjadi
 * jembatan untuk aplikasi legacy yang belum memakai Maven/Flyway: setiap
 * perubahan mempunyai versi immutable, checksum, advisory lock PostgreSQL, dan
 * riwayat eksekusi. Setelah build AIS dimodernisasi, isi migrasi ini dapat
 * dipindah 1:1 ke Flyway tanpa kehilangan urutan atau audit.
 */
public final class RetailDatabaseMigrations {

	private static final long ADVISORY_LOCK = 7348219061401L;

	private RetailDatabaseMigrations() {
	}

	private static final class Migration {
		final String version;
		final String description;
		final String sql;

		Migration(String version, String description, String sql) {
			this.version = version;
			this.description = description;
			this.sql = sql;
		}
	}

	/**
	 * Pecah skrip migrasi menjadi pernyataan-pernyataan terpisah, TAPI hormati blok
	 * dollar-quoted PostgreSQL ({@code $tag$ ... $tag$}).
	 *
	 * <p><b>Kenapa perlu.</b> Sebelumnya skrip dipecah dengan {@code sql.split(";")} begitu saja.
	 * Itu cukup untuk DDL sederhana, tetapi MERUSAK blok {@code DO $$ ... $$} yang isinya
	 * mengandung titik-koma: potongannya menjadi fragmen SQL tak valid. Padahal blok {@code DO}
	 * justru yang dibutuhkan untuk DDL KONDISIONAL — mis. hanya menambah kolom bila tabelnya ada,
	 * supaya instalasi yang tidak memasang modul tertentu (SIRS, akunting) tidak gagal migrasi.</p>
	 *
	 * <p>Untuk skrip tanpa dollar-quoting, hasilnya identik dengan perilaku lama.</p>
	 */
	private static List<String> pisahPernyataan(String skrip) {
		List<String> hasil = new ArrayList<String>();
		if (skrip == null) {
			return hasil;
		}
		StringBuilder buffer = new StringBuilder();
		String tagAktif = null; // tag dollar-quote yang sedang dibuka, mis. "$mig$"
		int i = 0;
		while (i < skrip.length()) {
			char c = skrip.charAt(i);
			if (tagAktif == null && c == '$') {
				int tutup = skrip.indexOf('$', i + 1);
				// Tag valid hanya bila isinya kosong/identifier sederhana (tanpa spasi).
				if (tutup > i && skrip.substring(i + 1, tutup).indexOf(' ') < 0) {
					tagAktif = skrip.substring(i, tutup + 1);
					buffer.append(tagAktif);
					i = tutup + 1;
					continue;
				}
			} else if (tagAktif != null && c == '$' && skrip.startsWith(tagAktif, i)) {
				buffer.append(tagAktif);
				i += tagAktif.length();
				tagAktif = null;
				continue;
			}
			if (c == ';' && tagAktif == null) {
				hasil.add(buffer.toString());
				buffer.setLength(0);
			} else {
				buffer.append(c);
			}
			i++;
		}
		hasil.add(buffer.toString());
		return hasil;
	}

	private static List<Migration> daftar() {
		List<Migration> result = new ArrayList<Migration>();
		result.add(new Migration("20260814.001", "retail request idempotency",
				"CREATE TABLE IF NOT EXISTS public.retail_request_idempotency ("
						+ "id BIGSERIAL PRIMARY KEY, action VARCHAR(80) NOT NULL, idempotency_key VARCHAR(160) NOT NULL, "
						+ "request_hash VARCHAR(64), status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING', "
						+ "result_reference VARCHAR(160), response_json TEXT, created_at TIMESTAMP NOT NULL DEFAULT NOW(), "
						+ "updated_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE(action,idempotency_key));"
						+ "CREATE INDEX IF NOT EXISTS idx_retail_idempotency_status_updated "
						+ "ON public.retail_request_idempotency(status,updated_at);"));
		result.add(new Migration("20260814.002", "inventory movement ledger",
				"CREATE TABLE IF NOT EXISTS koperasi.inventory_movement ("
						+ "id BIGSERIAL PRIMARY KEY, reference_type VARCHAR(40) NOT NULL, reference_id VARCHAR(120) NOT NULL, "
						+ "idempotency_key VARCHAR(160), produk BIGINT NOT NULL, produk_batch BIGINT, toko_asal BIGINT, toko_tujuan BIGINT, "
						+ "movement_type VARCHAR(40) NOT NULL, quantity NUMERIC(20,6) NOT NULL, unit_cost NUMERIC(20,4) NOT NULL DEFAULT 0, "
						+ "movement_value NUMERIC(22,4) NOT NULL DEFAULT 0, stock_before NUMERIC(20,6), stock_after NUMERIC(20,6), "
						+ "occurred_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(), created_by VARCHAR(255), "
						+ "device_id VARCHAR(255), reason TEXT, reversal_of BIGINT, status VARCHAR(20) NOT NULL DEFAULT 'POSTED');"
						+ "CREATE UNIQUE INDEX IF NOT EXISTS uq_inventory_movement_reference "
						+ "ON koperasi.inventory_movement(reference_type,reference_id,produk,movement_type,COALESCE(produk_batch,0));"
						+ "CREATE INDEX IF NOT EXISTS idx_inventory_movement_produk_waktu "
						+ "ON koperasi.inventory_movement(produk,occurred_at DESC,id DESC);"
						+ "CREATE INDEX IF NOT EXISTS idx_inventory_movement_toko_waktu "
						+ "ON koperasi.inventory_movement(COALESCE(toko_tujuan,toko_asal),occurred_at DESC,id DESC);"));
		result.add(new Migration("20260814.003", "database performance samples",
				"CREATE TABLE IF NOT EXISTS public.database_performance_sample ("
						+ "id BIGSERIAL PRIMARY KEY, captured_at TIMESTAMP NOT NULL DEFAULT NOW(), source VARCHAR(40) NOT NULL, "
						+ "query_fingerprint VARCHAR(64), duration_ms BIGINT, calls BIGINT, rows_count BIGINT, detail TEXT);"
						+ "CREATE INDEX IF NOT EXISTS idx_db_performance_captured "
						+ "ON public.database_performance_sample(captured_at DESC,id DESC);"));
		/*
		 * BASELINE SKEMA (Fase 4). Menuliskan SECARA EKSPLISIT seluruh perubahan yang selama ini
		 * dijalankan diam-diam oleh kelas ais.common.*SchemaFix saat startup, supaya skema menjadi
		 * versioned, tercatat di ais_schema_history, dan dapat ditinjau DBA. Salinan yang dapat
		 * dijalankan manual lewat psql: docs/performance/migrations/20260819.001-*.sql
		 *
		 * Bagian 1 (ADD COLUMN) pada dasarnya sudah dikerjakan otomatis oleh hbm2ddl.auto=update
		 * (tabel utama di public) dan AuditSchemaSyncUtil (tabel audit di new_audit); di sini
		 * hanya DIBAKUKAN agar tidak lagi bergantung pada kelas *SchemaFix per-rilis.
		 * Bagian 2 (pelebaran varchar -> text) TIDAK BISA didelegasikan ke hibernate.cfg.xml:
		 * hbm2ddl=update TIDAK PERNAH mengubah tipe kolom yang SUDAH ADA.
		 *
		 * Keduanya idempoten dan MELEWATI tabel/skema yang tidak ada, sehingga instalasi tanpa
		 * modul tertentu (mis. SIRS atau akunting) tidak gagal saat migrasi berjalan.
		 */
		result.add(new Migration("20260819.001", "baseline kolom schema-fix + pelebaran kolom text",
				"DO $mig1$ DECLARE r RECORD; BEGIN FOR r IN SELECT * FROM (VALUES "
						+ "('new_audit','statusabsensi__audit','durasi_baku_hari','integer'),"
						+ "('new_audit','libur_nasional__audit','libur_panjang','boolean'),"
						+ "('public','produk_kursus','instruktur','bigint'),"
						+ "('new_audit','produk_kursus__audit','instruktur','bigint'),"
						+ "('public','produk_kursus','status','varchar(50)'),"
						+ "('new_audit','produk_kursus__audit','status','varchar(50)'),"
						+ "('public','produk_kursus','gratis','boolean'),"
						+ "('new_audit','produk_kursus__audit','gratis','boolean'),"
						+ "('public','peserta_punya_produk_kursus','hargadibayar','double precision'),"
						+ "('new_audit','peserta_punya_produk_kursus__audit','hargadibayar','double precision'),"
						+ "('public','peserta_punya_produk_kursus','kupon_kursus','bigint'),"
						+ "('new_audit','peserta_punya_produk_kursus__audit','kupon_kursus','bigint'),"
						+ "('sirs','pasien','nik','varchar(20)'),"
						+ "('new_audit','pasien__audit','nik','varchar(20)'),"
						+ "('sirs','pasien','no_kartu_bpjs','varchar(25)'),"
						+ "('new_audit','pasien__audit','no_kartu_bpjs','varchar(25)'),"
						+ "('sirs','pasien','ihs_number','varchar(30)'),"
						+ "('new_audit','pasien__audit','ihs_number','varchar(30)'),"
						+ "('sirs','asuransi','jenis_payer','varchar(30)'),"
						+ "('new_audit','asuransi__audit','jenis_payer','varchar(30)'),"
						+ "('sirs','asuransi','kode_payer','varchar(30)'),"
						+ "('new_audit','asuransi__audit','kode_payer','varchar(30)'),"
						+ "('sirs','asuransi','nomor_pks','varchar(50)'),"
						+ "('new_audit','asuransi__audit','nomor_pks','varchar(50)'),"
						+ "('sirs','asuransi','aktif','boolean'),"
						+ "('new_audit','asuransi__audit','aktif','boolean'),"
						+ "('public','tbmuser','dokter','bigint'),"
						+ "('new_audit','tbmuser__audit','dokter','bigint'),"
						+ "('public','diskon_mahasiswa','jenis_diskon_mahasiswa','bigint'),"
						+ "('new_audit','diskon_mahasiswa__audit','jenis_diskon_mahasiswa','bigint'),"
						+ "('public','gelombang_pendaftaran','jenis_diskon_mahasiswa','bigint'),"
						+ "('new_audit','gelombang_pendaftaran__audit','jenis_diskon_mahasiswa','bigint'),"
						+ "('public','jenis_seleksi','jenis_diskon_mahasiswa','bigint'),"
						+ "('new_audit','jenis_seleksi__audit','jenis_diskon_mahasiswa','bigint'),"
						+ "('public','kelompok_mahasiswa','jenis_diskon_mahasiswa','bigint'),"
						+ "('new_audit','kelompok_mahasiswa__audit','jenis_diskon_mahasiswa','bigint'),"
						+ "('public','jenis_diskon_mahasiswa','tanggal_mulai_berlaku','date'),"
						+ "('public','jenis_diskon_mahasiswa','tanggal_sampai_berlaku','date'),"
						+ "('public','jenis_diskon_mahasiswa','berlaku_untuk_semua_mahasiswa','boolean'),"
						+ "('public','jenis_diskon_mahasiswa','fakultas','bigint'),"
						+ "('public','jenis_diskon_mahasiswa','jurusan','bigint'),"
						+ "('public','jenis_diskon_mahasiswa','program','varchar(50)'),"
						+ "('public','jenis_diskon_mahasiswa','status_awal_mahasiswa','bigint'),"
						+ "('new_audit','jenis_diskon_mahasiswa__audit','tanggal_mulai_berlaku','date'),"
						+ "('new_audit','jenis_diskon_mahasiswa__audit','tanggal_sampai_berlaku','date'),"
						+ "('new_audit','jenis_diskon_mahasiswa__audit','berlaku_untuk_semua_mahasiswa','boolean'),"
						+ "('new_audit','jenis_diskon_mahasiswa__audit','fakultas','bigint'),"
						+ "('new_audit','jenis_diskon_mahasiswa__audit','jurusan','bigint'),"
						+ "('new_audit','jenis_diskon_mahasiswa__audit','program','varchar(50)'),"
						+ "('new_audit','jenis_diskon_mahasiswa__audit','status_awal_mahasiswa','bigint'),"
						+ "('public','kelompok_calon_mahasiswa','jenis_seleksi_target','bigint'),"
						+ "('new_audit','kelompok_calon_mahasiswa__audit','jenis_seleksi_target','bigint'),"
						+ "('new_audit','tugas_pertemuan__audit','sub_cpmk_per_peserta','text'),"
						+ "('public','detailperkuliahan','total_nilai_kunci','double precision'),"
						+ "('new_audit','detailperkuliahan__audit','total_nilai_kunci','double precision'),"
						+ "('public','detailperkuliahan','nilai_huruf_kunci','varchar(2)'),"
						+ "('new_audit','detailperkuliahan__audit','nilai_huruf_kunci','varchar(2)'),"
						+ "('public','detailperkuliahan','nilai_ip_kunci','double precision'),"
						+ "('new_audit','detailperkuliahan__audit','nilai_ip_kunci','double precision'),"
						+ "('public','detailperkuliahan','lulus_kunci','boolean'),"
						+ "('new_audit','detailperkuliahan__audit','lulus_kunci','boolean'),"
						+ "('public','detailperkuliahan','total_nilai_sementara_kunci','double precision'),"
						+ "('new_audit','detailperkuliahan__audit','total_nilai_sementara_kunci','double precision'),"
						+ "('public','detailperkuliahan','nilai_huruf_sementara_kunci','varchar(2)'),"
						+ "('new_audit','detailperkuliahan__audit','nilai_huruf_sementara_kunci','varchar(2)'),"
						+ "('public','detailperkuliahan','nilai_ip_sementara_kunci','double precision'),"
						+ "('new_audit','detailperkuliahan__audit','nilai_ip_sementara_kunci','double precision')"
						+ ") AS t(sch,tbl,col,typ) LOOP "
						+ "IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=r.sch AND table_name=r.tbl) "
						+ "AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=r.sch AND table_name=r.tbl AND column_name=r.col) "
						+ "THEN EXECUTE format('ALTER TABLE %I.%I ADD COLUMN %I %s', r.sch, r.tbl, r.col, r.typ); "
						+ "END IF; END LOOP; END $mig1$;"
						+ "DO $mig2$ DECLARE r RECORD; BEGIN FOR r IN SELECT * FROM (VALUES "
						+ "('public','virtual_account_bank','keterangan'),('public','virtual_account_bank','cicilan'),"
						+ "('public','virtual_account_bank','detailbiaya'),('public','virtual_account_bank','bulanan'),"
						+ "('public','virtual_account_bank','request'),('public','virtual_account_bank','response'),"
						+ "('public','virtual_account_bank','link'),('public','virtual_account_bank','notif'),"
						+ "('public','virtual_account_bank','barcode'),"
						+ "('public','log_host_to_host','oleh'),('public','log_host_to_host','olehid'),"
						+ "('public','log_host_to_host','ip'),('public','log_host_to_host','nama'),"
						+ "('public','log_host_to_host','keterangan'),('public','log_host_to_host','response_description'),"
						+ "('public','log_host_to_host','request'),('public','log_host_to_host','response'),"
						+ "('public','log_host_to_host','item'),('public','log_host_to_host','stack_trace'),"
						+ "('public','log_host_to_host','info0'),('public','log_host_to_host','info1'),"
						+ "('public','log_host_to_host','info2'),('public','log_host_to_host','info3'),"
						+ "('public','log_host_to_host','info4'),('public','log_host_to_host','info5'),"
						+ "('public','log_host_to_host','info6'),('public','log_host_to_host','info7'),"
						+ "('public','log_host_to_host','info8'),('public','log_host_to_host','info9'),"
						+ "('public','log_host_to_host','info10'),('public','log_host_to_host','info11'),"
						+ "('public','log_host_to_host','info12'),('public','log_host_to_host','info13'),"
						+ "('public','log_host_to_host','info14'),('public','log_host_to_host','info15'),"
						+ "('public','log_host_to_host','info16'),('public','log_host_to_host','info17'),"
						+ "('public','log_host_to_host','info18'),"
						+ "('akunting','transaksi','keterangan'),('new_audit','transaksi__audit','keterangan'),"
						+ "('public','konfigurasi','keterangan'),('public','konfigurasi','nilai'),"
						+ "('public','konfigurasi','nilaidikunci'),('new_audit','konfigurasi__audit','keterangan'),"
						+ "('new_audit','konfigurasi__audit','nilai'),('new_audit','konfigurasi__audit','nilaidikunci')"
						+ ") AS t(sch,tbl,col) LOOP "
						+ "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=r.sch AND table_name=r.tbl AND column_name=r.col AND data_type <> 'text') "
						+ "THEN EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN %I TYPE text', r.sch, r.tbl, r.col); "
						+ "END IF; END LOOP; END $mig2$;"));
		return result;
	}

	public static void migrate() throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Connection connection = session.connection();
		boolean oldAutoCommit = connection.getAutoCommit();
		try {
			connection.setAutoCommit(false);
			Statement bootstrap = connection.createStatement();
			bootstrap.execute("CREATE TABLE IF NOT EXISTS public.ais_schema_history ("
					+ "version VARCHAR(40) PRIMARY KEY, description VARCHAR(255) NOT NULL, checksum VARCHAR(64) NOT NULL, "
					+ "installed_at TIMESTAMP NOT NULL DEFAULT NOW(), execution_ms BIGINT NOT NULL, success BOOLEAN NOT NULL)");
			bootstrap.execute("SELECT pg_advisory_xact_lock(" + ADVISORY_LOCK + ")");
			bootstrap.close();

			for (Migration migration : daftar()) {
				String checksum = sha256(migration.sql);
				PreparedStatement check = connection.prepareStatement(
						"SELECT checksum,success FROM public.ais_schema_history WHERE version=?");
				check.setString(1, migration.version);
				ResultSet rs = check.executeQuery();
				if (rs.next()) {
					String existingChecksum = rs.getString(1);
					boolean success = rs.getBoolean(2);
					rs.close();
					check.close();
					if (!success || !checksum.equals(existingChecksum)) {
						throw new IllegalStateException("Migrasi " + migration.version
								+ " pernah dijalankan tetapi checksum/status tidak sesuai. Jangan mengubah migrasi lama; buat versi baru.");
					}
					continue;
				}
				rs.close();
				check.close();

				long mulai = System.currentTimeMillis();
				// Satu migration dapat berisi beberapa DDL. Eksekusi per pernyataan
				// supaya tetap kompatibel dengan konfigurasi JDBC yang menolak multi-query.
				for (String sql : pisahPernyataan(migration.sql)) {
					if (sql == null || sql.trim().isEmpty()) continue;
					Statement statement = connection.createStatement();
					try {
						statement.execute(sql.trim());
					} finally {
						statement.close();
					}
				}
				PreparedStatement insert = connection.prepareStatement(
						"INSERT INTO public.ais_schema_history(version,description,checksum,execution_ms,success) VALUES (?,?,?,?,true)");
				insert.setString(1, migration.version);
				insert.setString(2, migration.description);
				insert.setString(3, checksum);
				insert.setLong(4, System.currentTimeMillis() - mulai);
				insert.executeUpdate();
				insert.close();
			}
			connection.commit();
		} catch (Exception e) {
			try {
				connection.rollback();
			} catch (Exception rollbackError) {
				ErrorAuditUtil.record(rollbackError, "RetailDatabaseMigrations.rollback");
			}
			throw e;
		} finally {
			try {
				connection.setAutoCommit(oldAutoCommit);
			} catch (Exception ignored) {
				ErrorAuditUtil.record(ignored, "RetailDatabaseMigrations.restoreAutoCommit");
			}
			try {
				session.close();
			} catch (Exception ignored) {
				ErrorAuditUtil.record(ignored, "RetailDatabaseMigrations.close");
			}
		}
	}

	/**
	 * Deteksi (bukan perbaiki) skema database yang TIDAK bisa diakses user DB tenant saat ini.
	 * Akar masalah produksi /albahjah: user DB tenant tidak punya GRANT pada schema "rab" (POS
	 * login gagal simpan LogLogin) maupun "library" (RepositorySyncScheduler gagal baca modul
	 * Perpustakaan). Memberi GRANT butuh privilege owner/superuser yang TIDAK dimiliki koneksi
	 * aplikasi ini, jadi kelas ini hanya MENDETEKSI lewat has_schema_privilege() lalu mencatat
	 * peringatan berisi perintah GRANT yang harus dijalankan manual oleh DBA. Dipanggil sekali
	 * saat startup (lihat AppStartupListener), gagal-aman: exception apa pun di sini TIDAK BOLEH
	 * menggagalkan startup aplikasi.
	 */
	public static void checkSchemaPrivileges() {
		String[] schemas = { "rab", "library" };
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Connection connection = session.connection();
			List<String> kurangAkses = new ArrayList<String>();
			for (String schema : schemas) {
				try {
					PreparedStatement ps = connection
							.prepareStatement("SELECT has_schema_privilege(current_user, ?, 'USAGE')");
					try {
						ps.setString(1, schema);
						ResultSet rs = ps.executeQuery();
						try {
							if (rs.next() && !rs.getBoolean(1)) {
								kurangAkses.add(schema);
							}
						} finally {
							rs.close();
						}
					} finally {
						ps.close();
					}
				} catch (Exception exSchema) {
					// Query cek privilege sendiri gagal (mis. schema tidak ada sama sekali) --
					// jangan sampai menggagalkan pengecekan schema lain.
					ErrorAuditUtil.record(exSchema,
							"RetailDatabaseMigrations.checkSchemaPrivileges:cek-schema-" + schema);
				}
			}
			if (!kurangAkses.isEmpty()) {
				String daftarSchema = joinComma(kurangAkses);
				String pesan = "[PERINGATAN DBA] User database aplikasi TIDAK punya hak akses (USAGE) pada schema: "
						+ daftarSchema + ". Ini akan membuat operasi yang menyentuh schema tsb gagal dengan"
						+ " \"permission denied for schema " + kurangAkses.get(0) + "\" (aplikasi sudah menangani ini"
						+ " secara best-effort agar login/scheduler tidak berhenti, tetapi data terkait schema"
						+ " tsb TIDAK akan tersinkron/tercatat). Jalankan sebagai superuser/owner database:\n"
						+ "GRANT USAGE ON SCHEMA " + daftarSchema + " TO " + currentDbUser(connection) + ";\n"
						+ "GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA " + daftarSchema + " TO "
						+ currentDbUser(connection) + ";\n"
						+ "ALTER DEFAULT PRIVILEGES IN SCHEMA " + daftarSchema + " GRANT SELECT,INSERT,UPDATE,DELETE ON TABLES TO "
						+ currentDbUser(connection) + ";";
				System.err.println(pesan);
				ErrorAuditUtil.record(null, pesan);
			}
		} catch (Throwable t) {
			ErrorAuditUtil.record(t, "RetailDatabaseMigrations.checkSchemaPrivileges");
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Throwable ignored) {
				}
			}
		}
	}

	private static String currentDbUser(Connection connection) {
		try {
			return connection.getMetaData().getUserName();
		} catch (Throwable ignored) {
			return "<user>";
		}
	}

	private static String joinComma(List<String> values) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) sb.append(", ");
			sb.append(values.get(i));
		}
		return sb.toString();
	}

	private static String sha256(String value) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(Charset.forName("UTF-8")));
		StringBuilder result = new StringBuilder();
		for (byte b : digest) {
			result.append(String.format("%02x", Integer.valueOf(b & 0xff)));
		}
		return result.toString();
	}
}
