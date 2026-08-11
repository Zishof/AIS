package ais.service.tenant;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pendaftar;
import ais.database.model.tenant.PendaftaranAuditEvent;
import ais.database.model.tenant.PendaftaranTenant;
import ais.database.model.tenant.TenantMembership;
import ais.database.model.tenant.TenantModuleEntitlement;
import ais.database.model.tenant.TenantRegistry;

/**
 * <h3>Gerbang READY/entitlement dashboard pendaftar + data onboarding (§11 dokumen master).</h3>
 *
 * <p>Aturan gating {@link #alasanTidakBolehMutasi}: (1) status akun di-RE-FETCH dari DB --
 * bukan entity sesi detached; (2) Pendaftar LEGACY (tidak pernah lewat program pendaftaran
 * tenant -- tanpa baris {@code pendaftaran_tenant}) TETAP boleh seperti sekarang (fail-open
 * data existing, keputusan G-06); (3) Pendaftar program tenant WAJIB punya minimal satu tenant
 * READY/ACTIVE; (4) bila modul disyaratkan, minimal satu tenant READY/ACTIVE ber-entitlement
 * modul itu ACTIVE. Pesan penolakan aman + jujur (tanpa detail internal).</p>
 */
public final class TenantOnboardingService {

	private TenantOnboardingService() {
	}

	/** @return null = boleh; selain itu pesan penolakan aman utk pengguna. */
	public static String alasanTidakBolehMutasi(Long pendaftarId, String modulDibutuhkan) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Pendaftar segar = (Pendaftar) session.get(Pendaftar.class, pendaftarId);
			if (segar == null) {
				return "Akun tidak ditemukan. Silakan masuk kembali.";
			}
			Number jumlahPermohonan = (Number) session.createCriteria(PendaftaranTenant.class)
					.add(Restrictions.eq("pendaftar.id", pendaftarId))
					.setProjection(Projections.rowCount()).uniqueResult();
			if (jumlahPermohonan == null || jumlahPermohonan.longValue() == 0) {
				// Akun legacy pra-program tenant: perilaku existing dipertahankan.
				return null;
			}
			if (!Boolean.TRUE.equals(segar.getAktif())) {
				return "Akun Anda belum aktif. Selesaikan verifikasi email dan tunggu tenant siap.";
			}
			List<?> tenantSiap = session.createCriteria(TenantRegistry.class)
					.add(Restrictions.eq("ownerPendaftar.id", pendaftarId))
					.add(Restrictions.in("status",
							new String[] { TenantRegistry.STATUS_READY, TenantRegistry.STATUS_ACTIVE }))
					.list();
			if (tenantSiap.isEmpty()) {
				return "Tenant Anda belum siap (READY). Pantau progres pada halaman status pendaftaran.";
			}
			if (modulDibutuhkan == null || modulDibutuhkan.trim().isEmpty()) {
				return null;
			}
			for (Object o : tenantSiap) {
				Number adaModul = (Number) session.createCriteria(TenantModuleEntitlement.class)
						.add(Restrictions.eq("tenant.id", ((TenantRegistry) o).getId()))
						.add(Restrictions.eq("moduleCode", modulDibutuhkan))
						.add(Restrictions.eq("status", TenantModuleEntitlement.STATUS_ACTIVE))
						.setProjection(Projections.rowCount()).uniqueResult();
				if (adaModul != null && adaModul.longValue() > 0) {
					return null;
				}
			}
			return "Modul " + modulDibutuhkan + " tidak termasuk entitlement tenant Anda saat ini.";
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TenantOnboardingService.alasanTidakBolehMutasi");
			// Fail-closed utk keraguan pada akun program tenant TIDAK bisa dibedakan di sini
			// saat error -- pilih pesan generik aman (bukan meloloskan diam-diam).
			return "Tidak dapat memeriksa status tenant saat ini. Coba lagi.";
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** READY → ACTIVE saat owner login pertama (05-workflow #7); best-effort dgn transaksi sendiri. */
	public static void tandaiAktifSaatLogin(Long pendaftarId) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			Date sekarang = new Date();
			List<?> daftar = session.createCriteria(TenantRegistry.class)
					.add(Restrictions.eq("ownerPendaftar.id", pendaftarId))
					.add(Restrictions.eq("status", TenantRegistry.STATUS_READY)).list();
			for (Object o : daftar) {
				TenantRegistry tenant = (TenantRegistry) o;
				tenant.setStatus(TenantRegistry.STATUS_ACTIVE);
				tenant.setActivatedAt(sekarang);
				session.saveOrUpdate(tenant);
				List<?> permohonans = session.createCriteria(PendaftaranTenant.class)
						.add(Restrictions.eq("tenantRegistry.id", tenant.getId()))
						.add(Restrictions.eq("status", PendaftaranTenant.STATUS_READY)).list();
				for (Object p : permohonans) {
					((PendaftaranTenant) p).setStatus(PendaftaranTenant.STATUS_ACTIVE);
					session.saveOrUpdate((PendaftaranTenant) p);
				}
				PendaftaranAuditEvent ev = new PendaftaranAuditEvent();
				ev.setEventCode(PendaftaranAuditEvent.EV_TENANT_ACTIVATED);
				ev.setActorType(PendaftaranAuditEvent.ACTOR_PENDAFTAR);
				ev.setPendaftarId(pendaftarId);
				ev.setTenantId(tenant.getId());
				ev.setResult("OK");
				ev.setWaktu(sekarang);
				ev.setOleh("login");
				ev.setOlehId("login");
				session.save(ev);
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TenantOnboardingService.tandaiAktifSaatLogin");
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) TenantOnboardingService.tandaiAktif.rollback");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Daftar tenant milik/di-member-i pendaftar (tenant switcher) + status + trial + entitlement. */
	public static void tenantList(Long pendaftarId, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray arr = new JSONArray();
			List<?> memberships = session.createCriteria(TenantMembership.class)
					.add(Restrictions.eq("pendaftar.id", pendaftarId))
					.add(Restrictions.eq("status", TenantMembership.STATUS_ACTIVE))
					.addOrder(Order.asc("id")).list();
			for (Object o : memberships) {
				TenantMembership m = (TenantMembership) o;
				TenantRegistry t = m.getTenant();
				if (t == null) {
					continue;
				}
				JSONObject row = new JSONObject();
				row.put("tenantId", t.getId());
				row.put("code", t.getCode());
				row.put("nama", t.getNama());
				row.put("slug", t.getSlug());
				row.put("status", t.getStatus());
				row.put("isOwner", Boolean.TRUE.equals(m.getIsOwner()));
				row.put("trialStart", t.getTrialStartAt() == null ? null : t.getTrialStartAt().toString());
				row.put("trialEnd", t.getTrialEndAt() == null ? null : t.getTrialEndAt().toString());
				JSONArray modulAktif = new JSONArray();
				JSONArray modulRencana = new JSONArray();
				List<?> ents = session.createCriteria(TenantModuleEntitlement.class)
						.add(Restrictions.eq("tenant.id", t.getId())).addOrder(Order.asc("moduleCode")).list();
				for (Object e : ents) {
					TenantModuleEntitlement ent = (TenantModuleEntitlement) e;
					if (TenantModuleEntitlement.STATUS_ACTIVE.equals(ent.getStatus())) {
						modulAktif.put(ent.getModuleCode());
					} else if (TenantModuleEntitlement.STATUS_PLANNED.equals(ent.getStatus())) {
						modulRencana.put(ent.getModuleCode());
					}
				}
				row.put("modulAktif", modulAktif);
				// JUJUR ke UI: modul PLANNED = belum tersedia, jangan render tombol aktif.
				row.put("modulBelumTersedia", modulRencana);
				arr.put(row);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
