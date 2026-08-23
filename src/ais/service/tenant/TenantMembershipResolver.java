package ais.service.tenant;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import ais.database.model.tenant.TenantMembership;
import ais.database.model.tenant.TenantRegistry;

/**
 * <h3>Penentu keanggotaan aktor pada satu tenant (P1 &sect;9.2).</h3>
 *
 * <p>Kewenangan atas tenant ditentukan oleh baris {@link TenantMembership} yang <b>aktif dan
 * masih berlaku</b> -- bukan oleh {@code TenantRegistry.ownerPendaftar} saja, dan bukan oleh
 * role global aktor. Role global menjawab "boleh apa di aplikasi ini", bukan "berhak atas
 * tenant yang mana".</p>
 *
 * <h4>Kompatibilitas owner lama, tanpa admin implicit</h4>
 * <p>Tenant yang terbit sebelum tabel keanggotaan terisi hanya punya {@code ownerPendaftar}.
 * Pemiliknya tetap dilayani, tetapi hasilnya <b>ditandai {@link Hasil#isTurunan() turunan}</b>
 * dengan {@code membershipId} kosong -- tidak ada baris keanggotaan yang dikarang diam-diam.
 * Hanya pemilik terdaftar yang mendapat kelonggaran ini; aktor lain tetap ditolak.</p>
 */
public final class TenantMembershipResolver {

	/** Role yang diberikan pada jalur kompatibilitas owner. */
	public static final String ROLE_OWNER = "OWNER";
	/** Penanda asal keanggotaan: baris tabel. */
	public static final String SUMBER_KEANGGOTAAN = "MEMBERSHIP";
	/** Penanda asal keanggotaan: diturunkan dari ownerPendaftar registry. */
	public static final String SUMBER_OWNER_REGISTRY = "OWNER_REGISTRY";

	private TenantMembershipResolver() {
	}

	/**
	 * Cari keanggotaan aktif aktor pada tenant.
	 *
	 * @param tbmuserId  userid Tbmuser yang sedang login; boleh {@code null}.
	 * @param pendaftarId id Pendaftar yang sedang login; boleh {@code null}.
	 * @throws TenantAccessException bila aktor tidak dinyatakan, bukan anggota, atau
	 *         keanggotaannya kedaluwarsa.
	 */
	public static Hasil resolve(Session session, TenantRegistry tenant, String tbmuserId,
			Long pendaftarId) {
		if (tenant == null || tenant.getId() == null) {
			throw new TenantAccessException(TenantAccessException.TENANT_ACCESS_DENIED,
					"Tenant tidak dikenal.");
		}
		boolean adaTbmuser = tbmuserId != null && tbmuserId.trim().length() > 0;
		boolean adaPendaftar = pendaftarId != null;
		if (!adaTbmuser && !adaPendaftar) {
			throw new TenantAccessException(TenantAccessException.TENANT_ACCESS_DENIED,
					"Aktor tidak dinyatakan.");
		}

		Date sekarang = new Date();
		List<?> baris = cariKeanggotaan(session, tenant.getId(), adaTbmuser ? tbmuserId.trim() : null,
				pendaftarId);

		// Kedaluwarsa dibedakan dari bukan-anggota supaya pesannya dapat menuntun pengguna.
		boolean adaTetapiKedaluwarsa = false;
		for (int i = 0; i < baris.size(); i++) {
			TenantMembership m = (TenantMembership) baris.get(i);
			if (!TenantMembership.STATUS_ACTIVE.equals(m.getStatus())) {
				continue;
			}
			if (berlaku(m, sekarang)) {
				return new Hasil(m, namaRole(m), pendaftarIdDari(m, pendaftarId), false,
						SUMBER_KEANGGOTAAN);
			}
			adaTetapiKedaluwarsa = true;
		}
		if (adaTetapiKedaluwarsa) {
			throw new TenantAccessException(TenantAccessException.TENANT_ACCESS_DENIED,
					"Keanggotaan Anda pada tenant ini sudah tidak berlaku.");
		}

		if (adaPendaftar && tenant.getOwnerPendaftar() != null
				&& pendaftarId.equals(tenant.getOwnerPendaftar().getId())) {
			return new Hasil(null, ROLE_OWNER, pendaftarId, true, SUMBER_OWNER_REGISTRY);
		}

		throw new TenantAccessException(TenantAccessException.TENANT_ACCESS_DENIED,
				"Anda tidak terdaftar pada tenant ini.");
	}

	private static List<?> cariKeanggotaan(Session session, Long tenantId, String tbmuserId,
			Long pendaftarId) {
		StringBuilder hql = new StringBuilder("FROM TenantMembership m WHERE m.tenant.id = :tid AND (");
		if (tbmuserId != null) {
			hql.append("m.tbmuser.userId = :uid");
		}
		if (tbmuserId != null && pendaftarId != null) {
			hql.append(" OR ");
		}
		if (pendaftarId != null) {
			hql.append("m.pendaftar.id = :pid");
		}
		hql.append(") ORDER BY m.id");
		Query q = session.createQuery(hql.toString());
		q.setParameter("tid", tenantId);
		if (tbmuserId != null) {
			q.setParameter("uid", tbmuserId);
		}
		if (pendaftarId != null) {
			q.setParameter("pid", pendaftarId);
		}
		return q.list();
	}

	private static boolean berlaku(TenantMembership m, Date sekarang) {
		Date dari = m.getValidFrom();
		Date sampai = m.getValidUntil();
		if (dari != null && sekarang.before(dari)) {
			return false;
		}
		// validUntil dianggap batas akhir yang masih termasuk hari itu; pembandingnya sudah
		// berupa timestamp, jadi cukup "belum lewat".
		if (sampai != null && sekarang.after(sampai)) {
			return false;
		}
		return true;
	}

	private static String namaRole(TenantMembership m) {
		if (m.getIsOwner() != null && m.getIsOwner().booleanValue()) {
			return ROLE_OWNER;
		}
		String r = m.getRoleCode();
		return r == null || r.trim().length() == 0 ? "" : r.trim();
	}

	private static Long pendaftarIdDari(TenantMembership m, Long cadangan) {
		if (m.getPendaftar() != null && m.getPendaftar().getId() != null) {
			return m.getPendaftar().getId();
		}
		return cadangan;
	}

	/** Hasil penentuan keanggotaan. Immutable. */
	public static final class Hasil {
		private final TenantMembership membership;
		private final String role;
		private final Long pendaftarId;
		private final boolean turunan;
		private final String sumber;

		Hasil(TenantMembership membership, String role, Long pendaftarId, boolean turunan,
				String sumber) {
			this.membership = membership;
			this.role = role;
			this.pendaftarId = pendaftarId;
			this.turunan = turunan;
			this.sumber = sumber;
		}

		public TenantMembership getMembership() { return membership; }
		public String getRole() { return role; }
		public Long getPendaftarId() { return pendaftarId; }
		public String getSumber() { return sumber; }

		/** Benar bila kewenangan berasal dari ownerPendaftar registry, bukan baris keanggotaan. */
		public boolean isTurunan() { return turunan; }

		public Long getMembershipId() {
			return membership == null ? null : membership.getId();
		}
	}
}
