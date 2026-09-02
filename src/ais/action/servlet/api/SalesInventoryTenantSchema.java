package ais.action.servlet.api;

import ais.service.tenant.TenantAccessException;
import ais.service.tenant.TenantContext;
import ais.service.tenant.TenantSchemaService;

/**
 * <h3>Satu-satunya tempat helper Sales/Inventory menentukan dan mengutip nama schema tenant.</h3>
 *
 * <p>Tiap helper yang dipindahkan ke schema tenant membutuhkan dua hal yang sama: apakah aktor
 * ini dilayani schema tenant, dan bagaimana nama schemanya dikutip dengan aman. Menyalin
 * keduanya ke setiap kelas {@code *Tenant} berarti sebelas salinan aturan pengutipan -- dan
 * dua tempat yang mengutip nama schema dengan caranya masing-masing adalah cara termudah
 * melahirkan lubang injeksi.</p>
 *
 * <p>Validasinya diserahkan {@link TenantSchemaService#pastikanAman}, validator yang sama
 * dengan jalur {@code {S}} milik {@code TenantSqlExecutor}. Kelas ini tidak menyusun aturan
 * nama sendiri.</p>
 */
final class SalesInventoryTenantSchema {

	private SalesInventoryTenantSchema() {
	}

	/**
	 * Benar bila aktor ini dilayani schema tenant, bukan schema bersama {@code koperasi}.
	 *
	 * <p>Aktor tanpa tenant -- seluruh pengguna yang ada hari ini -- selalu {@code false},
	 * sehingga jalur legacy berjalan tanpa perubahan (&sect;12.5).</p>
	 */
	static boolean aktif(EbisnisActorContextResolver.ActorContext aktor) {
		return aktor != null && aktor.tenant != null
				&& aktor.tenant.getSchemaName() != null
				&& aktor.tenant.getSchemaName().trim().length() > 0;
	}

	/**
	 * Prefiks schema <b>berikut titiknya</b>, siap disambung langsung ke nama tabel.
	 *
	 * @throws TenantAccessException bila tenant tidak punya schema atau namanya tidak sah.
	 */
	/**
	 * Prefiks schema <b>audit</b> berikut titiknya.
	 *
	 * <p>Divalidasi {@link ais.service.tenant.TenantSchemaLocator#pastikanAmanAudit} — validator
	 * yang <b>berbeda</b> dari {@code pastikanAman} biasa, sebab nama audit adalah nama turunan
	 * yang lebih panjang ({@code <schema>__audit}) dan validator biasa menolaknya. Memakai
	 * validator yang salah di sini akan menolak tenant yang sah; memakai perangkaian tanpa
	 * validator sama sekali akan membuka lubang injeksi. Keduanya dihindari dengan memanggil
	 * validator yang sama dengan {@link ais.service.tenant.TenantAuditWriter}.</p>
	 *
	 * @throws TenantAccessException bila tenant berjalan tanpa schema, atau namanya tidak sah.
	 */
	static String skemaAudit(TenantContext tenant) {
		if (tenant == null || !tenant.pakaiSchemaTenant()) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Audit per-tenant tidak berlaku pada mode ini.");
		}
		String a = tenant.getAuditSchemaName();
		if (a == null || a.trim().length() == 0) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Schema audit tenant tidak tersedia.");
		}
		try {
			return "\"" + ais.service.tenant.TenantSchemaLocator.pastikanAmanAudit(a.trim())
					+ "\".";
		} catch (IllegalArgumentException e) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Konfigurasi schema audit tidak sah.", e);
		}
	}

	static String skema(TenantContext tenant) {
		String s = tenant == null ? null : tenant.getSchemaName();
		if (s == null || s.trim().length() == 0) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Tenant ini tidak memiliki schema.");
		}
		try {
			return "\"" + TenantSchemaService.pastikanAman(s.trim()) + "\".";
		} catch (IllegalArgumentException e) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_INVALID,
					"Konfigurasi schema tenant tidak sah.", e);
		}
	}
}
