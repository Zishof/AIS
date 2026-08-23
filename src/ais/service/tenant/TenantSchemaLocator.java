package ais.service.tenant;

import org.hibernate.Session;

import ais.database.model.tenant.TenantRegistry;

/**
 * <h3>Penentu schema milik satu tenant (P1).</h3>
 *
 * <p>Satu-satunya tempat yang boleh menerjemahkan {@link TenantRegistry} menjadi nama schema.
 * Menyebarkan {@code getSchemaName()} ke banyak tempat berarti setiap tempat itu harus ingat
 * memvalidasinya; cepat atau lambat ada yang lupa, dan nama schema masuk ke SQL apa adanya.</p>
 *
 * <p>Nama audit mengikuti kesepakatan {@code <schema>__audit} yang sudah dipakai
 * {@link TenantSchemaService#buatSchema}. Bila kolom {@code auditSchemaName} pada registry
 * kosong, nama itu diturunkan -- bukan dianggap galat -- supaya baris registry lama tetap
 * terpakai.</p>
 */
public final class TenantSchemaLocator {

	private TenantSchemaLocator() {
	}

	/**
	 * Nama schema data untuk tenant ini, atau {@code null} bila mode LEGACY (memang memakai
	 * tabel shared lama).
	 *
	 * @throws TenantAccessException bila mode menuntut schema tetapi namanya kosong/tidak sah.
	 */
	public static String schemaData(TenantRegistry tenant) {
		if (tenant == null) {
			throw new TenantAccessException(TenantAccessException.TENANT_ACCESS_DENIED,
					"Tenant tidak dikenal.");
		}
		if (!butuhSchema(tenant)) {
			return null;
		}
		String nama = tenant.getSchemaName();
		if (nama == null || nama.trim().length() == 0) {
			throw new TenantAccessException(TenantAccessException.TENANT_NOT_READY,
					"Tenant belum selesai diprovisioning.");
		}
		try {
			return TenantSchemaService.pastikanAman(nama.trim());
		} catch (IllegalArgumentException e) {
			// Pesan sengaja tidak memuat nama schema-nya.
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Konfigurasi schema tenant tidak sah.", e);
		}
	}

	/** Nama schema audit, atau {@code null} bila mode LEGACY. */
	public static String schemaAudit(TenantRegistry tenant) {
		String data = schemaData(tenant);
		if (data == null) {
			return null;
		}
		String tersimpan = tenant.getAuditSchemaName();
		if (tersimpan != null && tersimpan.trim().length() > 0) {
			try {
				return TenantSchemaService.pastikanAman(tersimpan.trim());
			} catch (IllegalArgumentException abaikan) {
				// Baris registry lama bisa menyimpan nilai yang tidak lolos pola; turunkan saja.
			}
		}
		return data + "__audit";
	}

	/**
	 * Benar bila mode tenant menuntut schema tersendiri. LEGACY tidak; HYBRID dan TENANT_ONLY ya.
	 * Mode kosong diperlakukan sebagai LEGACY, mengikuti {@code TenantRegistry.getTenantMode()}.
	 */
	public static boolean butuhSchema(TenantRegistry tenant) {
		if (tenant == null) {
			return false;
		}
		String mode = tenant.getTenantMode();
		return TenantRegistry.MODE_HYBRID.equals(mode) || TenantRegistry.MODE_TENANT_ONLY.equals(mode);
	}

	/**
	 * Pastikan schema-nya benar-benar ada di basis data. <b>Satu kueri</b> ke {@code pg_namespace},
	 * jadi panggil saat memulai request tenant, bukan per kueri.
	 *
	 * @throws TenantAccessException bila mode menuntut schema tetapi schema-nya belum terbentuk.
	 */
	public static void pastikanSiap(Session session, TenantRegistry tenant) {
		String schema = schemaData(tenant);
		if (schema == null) {
			return;
		}
		if (!TenantSchemaService.schemaAda(session, schema)) {
			throw new TenantAccessException(TenantAccessException.TENANT_NOT_READY,
					"Tenant belum selesai diprovisioning.");
		}
	}
}
