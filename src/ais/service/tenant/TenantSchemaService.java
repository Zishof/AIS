package ais.service.tenant;

import java.util.regex.Pattern;

import org.hibernate.Session;

import ais.service.registration.PendaftaranValidationService;

/**
 * <h3>Operasi schema PostgreSQL per-tenant (mode HYBRID/TENANT_ONLY; §10.2 dokumen master).</h3>
 *
 * <p>Identifier HANYA berasal dari registry/reservation server -- TIDAK PERNAH dari request
 * (invariant #3). Regex ketat + cek reserved + quoting ganda; DDL idempoten
 * ({@code CREATE SCHEMA IF NOT EXISTS}, tersedia sejak PostgreSQL 9.3). Pada mode LEGACY
 * (default deployment) service ini TIDAK dipanggil -- step schema SKIPPED sah.</p>
 *
 * <p>Migrasi tabel per-schema penuh (data-plane TENANT_ONLY) berada di luar cakupan fase ini
 * dan dicatat jujur pada {@code schemaVersion} registry ("schema-only-v0"): schema dibuat +
 * diverifikasi ada, tabel data menyusul saat mode TENANT_ONLY dikerjakan -- TIDAK ada klaim
 * migrasi yang tidak dijalankan.</p>
 */
public final class TenantSchemaService {

	/** Sinkron dgn aturan username (§14.2); panjang ekstra utk suffix __audit ditangani terpisah. */
	private static final Pattern POLA_SCHEMA = Pattern.compile("^[a-z][a-z0-9_]{2,30}$");

	public static final String SCHEMA_VERSION_AWAL = "schema-only-v0";

	private TenantSchemaService() {
	}

	/** Validasi keras identifier schema; lempar IllegalArgumentException bila tidak sah. */
	public static String pastikanAman(String schemaName) {
		if (schemaName == null || !POLA_SCHEMA.matcher(schemaName).matches()
				|| PendaftaranValidationService.usernameReserved(schemaName)) {
			throw new IllegalArgumentException("Nama schema tidak sah.");
		}
		return schemaName;
	}

	/** Buat schema ERP + audit (idempoten). Berjalan pada session/transaction pemanggil. */
	public static void buatSchema(Session session, String schemaName) {
		String aman = pastikanAman(schemaName);
		session.createSQLQuery("CREATE SCHEMA IF NOT EXISTS \"" + aman + "\"").executeUpdate();
		session.createSQLQuery("CREATE SCHEMA IF NOT EXISTS \"" + aman + "__audit\"").executeUpdate();
	}

	/** true bila schema (dan pasangan __audit-nya) ada di pg_namespace. */
	public static boolean schemaAda(Session session, String schemaName) {
		String aman = pastikanAman(schemaName);
		Number n = (Number) session
				.createSQLQuery("SELECT COUNT(*) FROM pg_namespace WHERE nspname IN (:a, :b)")
				.setParameter("a", aman).setParameter("b", aman + "__audit").uniqueResult();
		return n != null && n.longValue() >= 2;
	}

	// =====================================================================
	// MIGRASI KANONIK PER-TENANT (P7 HYBRID) -- riwayat + checksum, idempoten
	// =====================================================================

	/**
	 * Terapkan seluruh migrasi kanonik {@link TenantSchemaMigrations#SEMUA} pada schema tenant
	 * (target sesuai definisi: ERP / AUDIT). Riwayat di {@code <schema>.tenant_schema_migration}:
	 * versi tercatat + checksum SAMA → dilewati; checksum BEDA → IllegalStateException (definisi
	 * kanonik tidak boleh berubah diam-diam). Berjalan pada session/transaction pemanggil.
	 *
	 * @param targetFilter {@link TenantSchemaMigrations#TARGET_ERP}/{@code TARGET_AUDIT};
	 *                     null = semua target.
	 * @return ringkasan "applied=N skipped=M" utk metadata step.
	 */
	public static String terapkanMigrasi(Session session, String schemaName, String targetFilter) {
		String erp = pastikanAman(schemaName);
		String audit = erp + "__audit";
		session.createSQLQuery("CREATE TABLE IF NOT EXISTS \"" + erp + "\".tenant_schema_migration ("
				+ "id bigserial PRIMARY KEY, version_code varchar(64) NOT NULL UNIQUE, "
				+ "checksum varchar(64) NOT NULL, target varchar(10) NOT NULL, "
				+ "applied_at timestamp NOT NULL DEFAULT now())").executeUpdate();

		int applied = 0;
		int skipped = 0;
		for (int i = 0; i < TenantSchemaMigrations.SEMUA.length; i++) {
			TenantSchemaMigrations.Migrasi m = TenantSchemaMigrations.SEMUA[i];
			if (targetFilter != null && !targetFilter.equals(m.target)) {
				continue;
			}
			String checksumKanonik = m.checksum();
			Object tercatat = session.createSQLQuery("SELECT checksum FROM \"" + erp
					+ "\".tenant_schema_migration WHERE version_code = :v")
					.setParameter("v", m.versionCode).uniqueResult();
			if (tercatat != null) {
				if (!checksumKanonik.equals(String.valueOf(tercatat))) {
					throw new IllegalStateException("Checksum migrasi " + m.versionCode
							+ " tidak cocok dengan riwayat schema -- definisi kanonik berubah.");
				}
				skipped++;
				continue;
			}
			for (int d = 0; d < m.ddl.length; d++) {
				String sql = m.ddl[d].replace("{S}", "\"" + erp + "\"")
						.replace("{A}", "\"" + audit + "\"")
						.replace("{SU}", erp);
				session.createSQLQuery(sql).executeUpdate();
			}
			session.createSQLQuery("INSERT INTO \"" + erp + "\".tenant_schema_migration "
					+ "(version_code, checksum, target) VALUES (:v, :c, :t)")
					.setParameter("v", m.versionCode)
					.setParameter("c", checksumKanonik)
					.setParameter("t", m.target).executeUpdate();
			applied++;
		}
		return "applied=" + applied + " skipped=" + skipped;
	}

	/** Checksum gabungan seluruh migrasi satu target -- dicatat di kolom checksum step. */
	public static String checksumTarget(String targetFilter) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < TenantSchemaMigrations.SEMUA.length; i++) {
			TenantSchemaMigrations.Migrasi m = TenantSchemaMigrations.SEMUA[i];
			if (targetFilter == null || targetFilter.equals(m.target)) {
				sb.append(m.versionCode).append(':').append(m.checksum()).append('\n');
			}
		}
		return ais.common.security.PasswordHashService.sha256Hex(sb.toString());
	}

	/**
	 * Verifikasi menyeluruh pasca-migrasi (VERIFY_SCHEMA non-LEGACY): schema ada + seluruh
	 * tabel wajib ERP/AUDIT ada + riwayat migrasi lengkap. Lempar IllegalStateException
	 * dgn pesan spesifik bila ada yang kurang.
	 */
	public static void verifikasiLengkap(Session session, String schemaName) {
		String erp = pastikanAman(schemaName);
		String audit = erp + "__audit";
		if (!schemaAda(session, erp)) {
			throw new IllegalStateException("Schema tenant tidak ditemukan di pg_namespace.");
		}
		for (int i = 0; i < TenantSchemaMigrations.TABEL_WAJIB_ERP.length; i++) {
			pastikanTabelAda(session, erp, TenantSchemaMigrations.TABEL_WAJIB_ERP[i]);
		}
		for (int i = 0; i < TenantSchemaMigrations.TABEL_WAJIB_AUDIT.length; i++) {
			pastikanTabelAda(session, audit, TenantSchemaMigrations.TABEL_WAJIB_AUDIT[i]);
		}
		Number riwayat = (Number) session.createSQLQuery(
				"SELECT COUNT(*) FROM \"" + erp + "\".tenant_schema_migration").uniqueResult();
		int diharapkan = TenantSchemaMigrations.SEMUA.length;
		if (riwayat == null || riwayat.intValue() < diharapkan) {
			throw new IllegalStateException("Riwayat migrasi tidak lengkap: " + riwayat + "/" + diharapkan);
		}
	}

	private static void pastikanTabelAda(Session session, String schema, String tabel) {
		Number n = (Number) session.createSQLQuery("SELECT COUNT(*) FROM information_schema.tables "
				+ "WHERE table_schema = :s AND table_name = :t")
				.setParameter("s", schema).setParameter("t", tabel).uniqueResult();
		if (n == null || n.longValue() == 0) {
			throw new IllegalStateException("Tabel wajib tidak ditemukan: " + schema + "." + tabel);
		}
	}
}
