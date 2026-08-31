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

	/** Kelas utilitas murni statis — tidak pernah diinstansiasi. */
	private TenantSchemaService() {
	}

	/**
	 * Validasi keras identifier schema data sebelum dipakai dalam DDL/DML apa pun. Dua
	 * pemeriksaan dijalankan berurutan: (1) kecocokan terhadap {@link #POLA_SCHEMA}
	 * ({@code ^[a-z][a-z0-9_]{2,30}$}, sinkron dengan aturan username §14.2), dan (2)
	 * {@code PendaftaranValidationService#usernameReserved} untuk menolak nama yang
	 * bertabrakan dengan kata kunci/skema yang dicadangkan. Ini adalah satu-satunya jalur
	 * yang boleh melepas identifier schema ke dalam SQL — dipanggil ulang di setiap method
	 * publik kelas ini yang menerima {@code schemaName}, dan juga oleh
	 * {@link TenantSqlExecutor} sebelum mengutip nama schema pada substitusi templat.
	 *
	 * @param schemaName nama schema data (bukan schema audit) yang akan diperiksa
	 * @return {@code schemaName} apa adanya bila sah, untuk kenyamanan pemanggilan berantai
	 * @throws IllegalArgumentException bila {@code schemaName} {@code null}, tidak cocok pola,
	 *         atau termasuk daftar reserved
	 */
	public static String pastikanAman(String schemaName) {
		if (schemaName == null || !POLA_SCHEMA.matcher(schemaName).matches()
				|| PendaftaranValidationService.usernameReserved(schemaName)) {
			throw new IllegalArgumentException("Nama schema tidak sah.");
		}
		return schemaName;
	}

	/**
	 * Buat schema ERP dan pasangan schema audit-nya (akhiran {@code __audit}) lewat
	 * {@code CREATE SCHEMA IF NOT EXISTS} — idempoten, tersedia sejak PostgreSQL 9.3, sehingga
	 * aman dipanggil ulang tanpa memeriksa keberadaannya lebih dulu. {@code schemaName}
	 * divalidasi via {@link #pastikanAman(String)} sebelum disisipkan ke DDL. Tidak membuka
	 * transaksi sendiri — berjalan pada session/transaction milik pemanggil, yang bertanggung
	 * jawab atas commit/rollback-nya.
	 *
	 * @param session    sesi Hibernate aktif dengan transaksi pemanggil
	 * @param schemaName nama schema data (tanpa akhiran {@code __audit})
	 * @throws IllegalArgumentException bila {@code schemaName} tidak lolos {@link #pastikanAman}
	 */
	public static void buatSchema(Session session, String schemaName) {
		String aman = pastikanAman(schemaName);
		session.createSQLQuery("CREATE SCHEMA IF NOT EXISTS \"" + aman + "\"").executeUpdate();
		session.createSQLQuery("CREATE SCHEMA IF NOT EXISTS \"" + aman + "__audit\"").executeUpdate();
	}

	/**
	 * Periksa apakah schema data dan pasangan schema audit-nya sudah ada di
	 * {@code pg_namespace}. Dipakai sebagai gerbang verifikasi ringan sebelum langkah
	 * provisioning berikutnya bergantung padanya (lihat juga {@link #verifikasiLengkap}
	 * untuk pemeriksaan yang lebih menyeluruh, termasuk isi tabel).
	 *
	 * @param session    sesi Hibernate aktif
	 * @param schemaName nama schema data (tanpa akhiran {@code __audit})
	 * @return {@code true} hanya bila KEDUA schema (data dan audit) ditemukan
	 * @throws IllegalArgumentException bila {@code schemaName} tidak lolos {@link #pastikanAman}
	 */
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
	 * (target sesuai definisi: ERP / AUDIT). Ini titik masuk tunggal migrasi schema tenant —
	 * dipanggil dari langkah provisioning maupun dari alat uji ({@code TenantSchemaDdlDump}
	 * mencetak DDL yang sama tanpa benar-benar menjalankannya).
	 *
	 * <p>
	 * Urutan kerja: (1) pastikan tabel riwayat {@code <schema>.tenant_schema_migration} ada
	 * (idempoten, {@code CREATE TABLE IF NOT EXISTS}); (2) iterasi
	 * {@link TenantSchemaMigrations#SEMUA} sesuai urutan definisinya, lewati entri yang tidak
	 * cocok {@code targetFilter}; (3) untuk tiap entri, bandingkan checksum kanonik
	 * ({@link TenantSchemaMigrations.Migrasi#checksum()}) dengan yang tercatat di riwayat —
	 * versi belum tercatat &rarr; seluruh pernyataan {@code m.ddl} dijalankan (penanda
	 * {@code {S}}/{@code {A}}/{@code {SU}} disubstitusi nama schema data/audit/mentah lebih
	 * dulu) lalu baris riwayat baru disisipkan; versi tercatat dengan checksum SAMA &rarr;
	 * dilewati (idempoten, migrasi boleh dipanggil ulang dengan aman); checksum BEDA &rarr;
	 * {@code IllegalStateException} (definisi kanonik tidak boleh berubah diam-diam di bawah
	 * tenant yang sudah memasangnya — lihat peringatan di setiap kelas bundel
	 * {@code TenantSchemaMigrationsV2}..{@code V9}).
	 * </p>
	 *
	 * <p>
	 * Tidak membuka transaksi sendiri — seluruh {@code executeUpdate} berjalan pada
	 * session/transaction milik pemanggil, sehingga kegagalan di tengah katalog dapat
	 * di-rollback oleh pemanggil sebagai satu unit.
	 * </p>
	 *
	 * @param session      sesi Hibernate aktif dengan transaksi pemanggil
	 * @param schemaName   nama schema data (tanpa akhiran {@code __audit})
	 * @param targetFilter {@link TenantSchemaMigrations#TARGET_ERP}/{@code TARGET_AUDIT};
	 *                     {@code null} = semua target
	 * @return ringkasan {@code "applied=N skipped=M"} untuk dicatat pada metadata step
	 *         provisioning
	 * @throws IllegalArgumentException bila {@code schemaName} tidak lolos {@link #pastikanAman}
	 * @throws IllegalStateException    bila checksum migrasi yang sudah tercatat di riwayat
	 *                                   tidak lagi cocok dengan definisi kanonik saat ini
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

	/**
	 * Hitung checksum gabungan seluruh migrasi satu target, murni dari definisi kanonik di
	 * memori (tidak menyentuh basis data) — dicatat di kolom checksum step provisioning agar
	 * dapat dibandingkan kelak tanpa perlu membuka koneksi ke schema tenant. Dibentuk dengan
	 * merangkai {@code versionCode:checksum} tiap entri {@link TenantSchemaMigrations#SEMUA}
	 * yang cocok {@code targetFilter} (urutan definisi, satu baris per entri), lalu di-hash
	 * SHA-256 lewat {@code PasswordHashService#sha256Hex}. Berbeda dari checksum per-migrasi
	 * ({@link TenantSchemaMigrations.Migrasi#checksum()}) yang menjaga satu bundel versi,
	 * method ini menjaga KOMBINASI seluruh bundel bertarget sama.
	 *
	 * @param targetFilter {@link TenantSchemaMigrations#TARGET_ERP}/{@code TARGET_AUDIT};
	 *                     {@code null} = semua target
	 * @return checksum SHA-256 heksadesimal gabungan
	 */
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
	 * Verifikasi menyeluruh pasca-migrasi (langkah {@code VERIFY_SCHEMA} pada provisioning
	 * non-LEGACY): (1) schema data ada di {@code pg_namespace} lewat {@link #schemaAda}; (2)
	 * setiap tabel di {@link TenantSchemaMigrations#TABEL_WAJIB_ERP} ada pada schema data dan
	 * setiap tabel di {@link TenantSchemaMigrations#TABEL_WAJIB_AUDIT} ada pada schema audit
	 * (diperiksa satu-satu lewat {@link #pastikanTabelAda}); (3) jumlah baris riwayat di
	 * {@code <schema>.tenant_schema_migration} tidak kurang dari jumlah entri
	 * {@link TenantSchemaMigrations#SEMUA} yang seharusnya sudah diterapkan. Tidak mengubah
	 * apa pun — murni pembacaan lewat {@code information_schema}/{@code pg_namespace}/tabel
	 * riwayat, sehingga aman dipanggil berkali-kali sebagai gerbang verifikasi.
	 *
	 * @param session    sesi Hibernate aktif
	 * @param schemaName nama schema data (tanpa akhiran {@code __audit})
	 * @throws IllegalArgumentException bila {@code schemaName} tidak lolos {@link #pastikanAman}
	 * @throws IllegalStateException    dengan pesan spesifik yang menyebut bagian mana yang
	 *                                   kurang — schema tidak ditemukan, tabel wajib hilang,
	 *                                   atau riwayat migrasi tidak lengkap
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

	/**
	 * Pastikan satu tabel ada pada satu schema, lewat {@code information_schema.tables}.
	 * Helper privat dipakai berulang oleh {@link #verifikasiLengkap} untuk memeriksa
	 * {@code TABEL_WAJIB_ERP}/{@code TABEL_WAJIB_AUDIT} satu-satu agar pesan galat menyebut
	 * nama tabel yang hilang secara spesifik, bukan sekadar "ada yang kurang".
	 *
	 * @param session sesi Hibernate aktif
	 * @param schema  nama schema (data atau audit) yang SUDAH divalidasi/diketahui aman
	 * @param tabel   nama tabel yang diharapkan ada pada schema tersebut
	 * @throws IllegalStateException bila tabel tidak ditemukan
	 */
	private static void pastikanTabelAda(Session session, String schema, String tabel) {
		Number n = (Number) session.createSQLQuery("SELECT COUNT(*) FROM information_schema.tables "
				+ "WHERE table_schema = :s AND table_name = :t")
				.setParameter("s", schema).setParameter("t", tabel).uniqueResult();
		if (n == null || n.longValue() == 0) {
			throw new IllegalStateException("Tabel wajib tidak ditemukan: " + schema + "." + tabel);
		}
	}
}
