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
}
