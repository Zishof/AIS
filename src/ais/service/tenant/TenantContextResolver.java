package ais.service.tenant;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;

import ais.database.model.tenant.TenantMembership;
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
 * {@link TenantAccessException#TENANT_SELECTION_REQUIRED}. Penafsiran diam-diam semacam itu
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
			throw new TenantAccessException(TenantAccessException.TENANT_SELECTION_REQUIRED,
					"Tenant belum dipilih.");
		}
		TenantRegistry tenant = (TenantRegistry) session.get(TenantRegistry.class, tenantId);
		return bentuk(session, tenant, tbmuserId, pendaftarId);
	}

	/** Sama dengan {@link #resolve}, tetapi tenant ditunjuk lewat {@code code}. */
	public static TenantContext resolveByCode(Session session, String tenantCode, String tbmuserId,
			Long pendaftarId) {
		if (tenantCode == null || tenantCode.trim().length() == 0) {
			throw new TenantAccessException(TenantAccessException.TENANT_SELECTION_REQUIRED,
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
			throw new TenantAccessException(TenantAccessException.TENANT_ACCESS_DENIED,
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
		// Dibedakan sesuai kontrak 7.1: SUSPENDED (butir 6) berarti akses sengaja dihentikan,
		// status lain (butir 7) berarti belum siap. Klien memerlukan pembedaan itu -- yang satu
		// menyuruh menghubungi admin, yang satu menyuruh menunggu.
		if (TenantRegistry.STATUS_SUSPENDED.equals(status)) {
			throw new TenantAccessException(TenantAccessException.TENANT_SUSPENDED,
					"Tenant sedang dihentikan sementara.");
		}
		throw new TenantAccessException(TenantAccessException.TENANT_NOT_READY,
				"Tenant belum siap dipakai.");
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
	/**
	 * Bentuk konteks tanpa tenantId dinyatakan, mengikuti kontrak &sect;7.1 butir 3-4:
	 * keanggotaan aktif <b>tepat satu</b> boleh dipilih otomatis; lebih dari satu wajib
	 * dipilih pengguna.
	 *
	 * <p>Perhatikan bedanya dengan "ambil yang pertama": bila aktor punya dua tenant,
	 * memilih salah satunya diam-diam berarti ia bekerja pada perusahaan yang salah tanpa
	 * pernah tahu. Karena itu lebih dari satu selalu ditolak, bukan diurutkan.</p>
	 */
	public static TenantContext resolveOtomatis(Session session, String tbmuserId, Long pendaftarId) {
		List<TenantRegistry> daftar = daftarTenantAktif(session, tbmuserId, pendaftarId);
		if (daftar.isEmpty()) {
			throw new TenantAccessException(TenantAccessException.TENANT_ACCESS_DENIED,
					"Anda belum terdaftar pada tenant mana pun.");
		}
		if (daftar.size() > 1) {
			throw new TenantAccessException(TenantAccessException.TENANT_SELECTION_REQUIRED,
					"Pilih tenant terlebih dahulu.");
		}
		return bentuk(session, daftar.get(0), tbmuserId, pendaftarId);
	}

	/**
	 * Tenant yang keanggotaannya aktif dan masih berlaku bagi aktor, ditambah tenant yang
	 * dimilikinya lewat {@code ownerPendaftar} (jalur kompatibilitas yang sama dengan
	 * {@link TenantMembershipResolver}). Dipakai {@code tenant_list} dan
	 * {@link #resolveOtomatis}.
	 *
	 * <p>Tenant SUSPENDED dan yang belum READY sengaja <b>tetap ditampilkan</b> -- pengguna
	 * perlu melihat bahwa perusahaannya ada tetapi sedang tidak dapat dipakai, alih-alih
	 * menghadapi daftar kosong yang tampak seperti kehilangan data.</p>
	 */
	public static List<TenantRegistry> daftarTenantAktif(Session session, String tbmuserId,
			Long pendaftarId) {
		boolean adaTbmuser = tbmuserId != null && tbmuserId.trim().length() > 0;
		boolean adaPendaftar = pendaftarId != null;
		if (!adaTbmuser && !adaPendaftar) {
			throw new TenantAccessException(TenantAccessException.TENANT_ACCESS_DENIED,
					"Aktor tidak dinyatakan.");
		}
		Date sekarang = new Date();
		LinkedHashMap<Long, TenantRegistry> unik = new LinkedHashMap<Long, TenantRegistry>();

		StringBuilder hql = new StringBuilder("SELECT m FROM TenantMembership m WHERE m.status = :st AND (");
		if (adaTbmuser) {
			hql.append("m.tbmuser.userId = :uid");
		}
		if (adaTbmuser && adaPendaftar) {
			hql.append(" OR ");
		}
		if (adaPendaftar) {
			hql.append("m.pendaftar.id = :pid");
		}
		hql.append(") ORDER BY m.id");
		Query q = session.createQuery(hql.toString());
		q.setParameter("st", TenantMembership.STATUS_ACTIVE);
		if (adaTbmuser) {
			q.setParameter("uid", tbmuserId.trim());
		}
		if (adaPendaftar) {
			q.setParameter("pid", pendaftarId);
		}
		List<?> baris = q.list();
		for (int i = 0; i < baris.size(); i++) {
			TenantMembership m = (TenantMembership) baris.get(i);
			if (m.getValidFrom() != null && sekarang.before(m.getValidFrom())) {
				continue;
			}
			if (m.getValidUntil() != null && sekarang.after(m.getValidUntil())) {
				continue;
			}
			TenantRegistry t = m.getTenant();
			if (t != null && t.getId() != null) {
				unik.put(t.getId(), t);
			}
		}

		if (adaPendaftar) {
			Query qo = session.createQuery("FROM TenantRegistry t WHERE t.ownerPendaftar.id = :pid ORDER BY t.id");
			qo.setParameter("pid", pendaftarId);
			List<?> milik = qo.list();
			for (int i = 0; i < milik.size(); i++) {
				TenantRegistry t = (TenantRegistry) milik.get(i);
				if (t.getId() != null) {
					unik.put(t.getId(), t);
				}
			}
		}
		return new ArrayList<TenantRegistry>(unik.values());
	}

	/**
	 * Gerbang modul, kontrak &sect;7.1 butir 8. Dipanggil terpisah supaya aksi yang memang
	 * lintas modul (mis. {@code tenant_list}) tidak ikut tergerbang.
	 */
	public static void pastikanModulAktif(TenantContext ctx, String moduleCode) {
		if (ctx == null || !ctx.punyaModul(moduleCode)) {
			throw new TenantAccessException(TenantAccessException.TENANT_MODULE_DISABLED,
					"Modul tersebut tidak aktif pada tenant ini.");
		}
	}

	/**
	 * Gerbang versi schema, kontrak &sect;7.1 butir 9. Menolak bila schema tenant belum
	 * dimigrasikan ke versi yang dituntut aplikasi -- lebih baik menolak daripada menjalankan
	 * kueri pada tabel/kolom yang belum ada.
	 */
	public static void pastikanSchemaMutakhir(TenantContext ctx, String versiDituntut) {
		if (versiDituntut == null || versiDituntut.trim().length() == 0) {
			return;
		}
		String punya = ctx == null ? null : ctx.getSchemaVersion();
		if (punya == null || !versiDituntut.trim().equals(punya.trim())) {
			throw new TenantAccessException(TenantAccessException.TENANT_SCHEMA_MIGRATION_REQUIRED,
					"Tenant ini menunggu pembaruan basis data.");
		}
	}

	/**
	 * Selaraskan tenantId dari header dan body, kontrak &sect;7.1 butir 1-2.
	 *
	 * @return tenantId yang disepakati, atau {@code null} bila keduanya kosong (pemanggil
	 *         lalu memakai {@link #resolveOtomatis}).
	 */
	public static Long selaraskanTenantId(Long dariHeader, Long dariBody) {
		if (dariHeader == null) {
			return dariBody;
		}
		if (dariBody == null || dariHeader.equals(dariBody)) {
			return dariHeader;
		}
		throw new TenantAccessException(TenantAccessException.TENANT_CONTEXT_MISMATCH,
				"Tenant pada header dan isi permintaan tidak sama.");
	}
}
