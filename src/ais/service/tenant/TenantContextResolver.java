package ais.service.tenant;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;

import ais.database.model.tenant.TenantModuleEntitlement;
import ais.database.model.tenant.TenantRegistry;

/**
 * <h3>Pembentuk {@link TenantContext} untuk satu request (P1 &sect;9.1).</h3>
 *
 * <p>Menyatukan tiga penentuan yang selama ini tercecer: tenant mana, aktor berhak atau tidak,
 * dan schema mana yang dituju. Semua jalur masuk memakai kelas ini supaya ketiganya tidak
 * pernah dijawab berbeda-beda di tempat berbeda.</p>
 *
 * <h4>Tenant wajib dinyatakan eksplisit (&sect;9.3)</h4>
 * <p>Tidak ada penafsiran "aktor tanpa pedagang berarti boleh seluruh tenant". Platform admin
 * pun harus menyebut tenant yang dituju; {@code tenantId} kosong ditolak dengan
 * {@link TenantAccessException#KODE_TENANT_BELUM_DIPILIH}. Penafsiran diam-diam semacam itu
 * adalah cara paling mudah membocorkan data antar tenant.</p>
 */
public final class TenantContextResolver {

	private TenantContextResolver() {
	}

	/**
	 * Bentuk konteks tenant. Memakai Session milik pemanggil -- tidak membuka Session sendiri,
	 * supaya seluruh request tetap satu transaksi.
	 *
	 * @param tenantId    tenant yang dituju; wajib.
	 * @param tbmuserId   userid Tbmuser yang login; boleh {@code null} bila aktor berupa Pendaftar.
	 * @param pendaftarId id Pendaftar yang login; boleh {@code null} bila aktor berupa Tbmuser.
	 */
	public static TenantContext resolve(Session session, Long tenantId, String tbmuserId,
			Long pendaftarId) {
		if (tenantId == null) {
			throw new TenantAccessException(TenantAccessException.KODE_TENANT_BELUM_DIPILIH,
					"Tenant belum dipilih.");
		}
		TenantRegistry tenant = (TenantRegistry) session.get(TenantRegistry.class, tenantId);
		return bentuk(session, tenant, tbmuserId, pendaftarId);
	}

	/** Sama dengan {@link #resolve}, tetapi tenant ditunjuk lewat {@code code}. */
	public static TenantContext resolveByCode(Session session, String tenantCode, String tbmuserId,
			Long pendaftarId) {
		if (tenantCode == null || tenantCode.trim().length() == 0) {
			throw new TenantAccessException(TenantAccessException.KODE_TENANT_BELUM_DIPILIH,
					"Tenant belum dipilih.");
		}
		Query q = session.createQuery("FROM TenantRegistry t WHERE t.code = :c");
		q.setParameter("c", tenantCode.trim());
		q.setMaxResults(1);
		return bentuk(session, (TenantRegistry) q.uniqueResult(), tbmuserId, pendaftarId);
	}

	private static TenantContext bentuk(Session session, TenantRegistry tenant, String tbmuserId,
			Long pendaftarId) {
		if (tenant == null) {
			throw new TenantAccessException(TenantAccessException.KODE_TENANT_TIDAK_DIKENAL,
					"Tenant tidak dikenal.");
		}
		pastikanTenantDapatDipakai(tenant);

		TenantMembershipResolver.Hasil anggota =
				TenantMembershipResolver.resolve(session, tenant, tbmuserId, pendaftarId);

		String schema = TenantSchemaLocator.schemaData(tenant);
		String schemaAudit = TenantSchemaLocator.schemaAudit(tenant);

		TenantContext.Builder b = TenantContext.builder();
		b.tenantId(tenant.getId());
		b.tenantCode(tenant.getCode());
		b.tenantName(tenant.getNama());
		b.tenantStatus(tenant.getStatus());
		b.tenantMode(tenant.getTenantMode());
		b.membershipId(anggota.getMembershipId());
		b.membershipRole(anggota.getRole());
		b.ownerPendaftarId(tenant.getOwnerPendaftar() == null ? null
				: tenant.getOwnerPendaftar().getId());
		b.activePendaftarId(anggota.getPendaftarId());
		b.activeTbmuserId(tbmuserId == null || tbmuserId.trim().length() == 0 ? null : tbmuserId.trim());
		b.schemaName(schema);
		b.auditSchemaName(schemaAudit);
		b.schemaVersion(tenant.getSchemaVersion());
		b.timezone(tenant.getTimezone());
		b.locale(tenant.getDefaultLocale());
		b.moduleEntitlements(muatModul(session, tenant.getId()));
		return b.build();
	}

	/**
	 * Tenant harus ACTIVE atau READY. PROVISIONING berarti schema-nya belum tentu ada;
	 * SUSPENDED berarti aksesnya memang sedang dihentikan.
	 */
	private static void pastikanTenantDapatDipakai(TenantRegistry tenant) {
		String status = tenant.getStatus();
		if (TenantRegistry.STATUS_ACTIVE.equals(status) || TenantRegistry.STATUS_READY.equals(status)) {
			return;
		}
		throw new TenantAccessException(TenantAccessException.KODE_TENANT_TIDAK_AKTIF,
				"Tenant sedang tidak dapat dipakai.");
	}

	/** Modul aktif tenant, dinormalkan ke HURUF BESAR supaya pembandingnya tidak peka huruf. */
	private static Set<String> muatModul(Session session, Long tenantId) {
		Set<String> hasil = new HashSet<String>();
		Query q = session.createQuery("SELECT e.moduleCode, e.effectiveFrom, e.effectiveUntil"
				+ " FROM TenantModuleEntitlement e WHERE e.tenant.id = :tid AND e.status = :st");
		q.setParameter("tid", tenantId);
		q.setParameter("st", TenantModuleEntitlement.STATUS_ACTIVE);
		List<?> baris = q.list();
		Date sekarang = new Date();
		for (int i = 0; i < baris.size(); i++) {
			Object[] r = (Object[]) baris.get(i);
			String kode = (String) r[0];
			Date dari = (Date) r[1];
			Date sampai = (Date) r[2];
			if (kode == null || kode.trim().length() == 0) {
				continue;
			}
			if (dari != null && sekarang.before(dari)) {
				continue;
			}
			if (sampai != null && sekarang.after(sampai)) {
				continue;
			}
			hasil.add(kode.trim().toUpperCase());
		}
		return hasil;
	}
}
