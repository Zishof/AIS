package ais.service.registration;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.tenant.JenisUsahaTenant;
import ais.database.model.tenant.PendaftaranAuditEvent;
import ais.database.model.tenant.PendaftaranTenant;
import ais.database.model.tenant.PendaftaranTenantJenisUsaha;
import ais.database.model.tenant.ProvisioningJob;
import ais.database.model.tenant.ProvisioningStep;
import ais.database.model.tenant.SchemaNameReservation;
import ais.database.model.tenant.TenantRegistry;

/**
 * <h3>Backoffice admin permohonan tenant (§15 dokumen master).</h3>
 *
 * <p>SELURUH method menerima {@link Tbmuser} admin yang SUDAH lolos gerbang privilege di
 * servlet ({@code root} / role {@code Tbmrole.ADMINISTRATOR}) -- di sini identitas admin
 * hanya dipakai utk audit (siapa menyetujui/menolak/dst). Aturan §15: reason WAJIB utk
 * approve/reject/release; tidak ada hard delete; retry idempoten (lanjut dari step aman);
 * release reservation DITOLAK bila sudah CONSUMED/tenant sudah ada; kredensial tidak pernah
 * bisa dilihat ulang (memang tidak disimpan).</p>
 */
public final class PendaftaranTenantAdminService {

	private PendaftaranTenantAdminService() {
	}

	/** Daftar permohonan + filter (kode/nama/email/username/status), paginasi sederhana. */
	public static JSONObject daftar(JSONObject filter) throws Exception {
		JSONObject hasil = new JSONObject();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			int halaman = Math.max(1, filter.optInt("halaman", 1));
			int perHalaman = 20;
			Criteria c = session.createCriteria(PendaftaranTenant.class, "pt")
					.createAlias("pendaftar", "p");
			String q = filter.optString("q", "").trim();
			if (!q.isEmpty()) {
				c.add(Restrictions.or(
						Restrictions.or(Restrictions.ilike("pt.registrationCode", q, MatchMode.ANYWHERE),
								Restrictions.ilike("pt.normalizedUsername", q, MatchMode.ANYWHERE)),
						Restrictions.or(Restrictions.ilike("p.nama", q, MatchMode.ANYWHERE),
								Restrictions.ilike("p.email", q, MatchMode.ANYWHERE))));
			}
			String status = filter.optString("statusFilter", "").trim();
			if (!status.isEmpty()) {
				c.add(Restrictions.eq("pt.status", status));
			}
			c.addOrder(Order.desc("pt.id"));
			c.setFirstResult((halaman - 1) * perHalaman);
			c.setMaxResults(perHalaman);

			JSONArray arr = new JSONArray();
			List<?> daftar = c.list();
			for (Object o : daftar) {
				PendaftaranTenant pt = (PendaftaranTenant) o;
				JSONObject row = new JSONObject();
				row.put("kode", pt.getRegistrationCode());
				row.put("namaUsaha", pt.getPendaftar().getNama());
				row.put("email", pt.getPendaftar().getEmail());
				row.put("username", pt.getNormalizedUsername());
				row.put("statusPermohonan", pt.getStatus());
				row.put("tahap", pt.getCurrentStage() == null ? "" : pt.getCurrentStage());
				row.put("submittedAt", pt.getSubmittedAt() == null ? "" : String.valueOf(pt.getSubmittedAt()));
				row.put("failureMessage", pt.getFailureMessageSafe() == null ? "" : pt.getFailureMessageSafe());
				StringBuilder jenis = new StringBuilder();
				List<?> pilihan = session.createCriteria(PendaftaranTenantJenisUsaha.class)
						.add(Restrictions.eq("pendaftaranTenant.id", pt.getId())).list();
				for (Object pil : pilihan) {
					JenisUsahaTenant j = ((PendaftaranTenantJenisUsaha) pil).getJenisUsahaTenant();
					if (j != null) {
						if (jenis.length() > 0) {
							jenis.append(", ");
						}
						jenis.append(j.getCode());
					}
				}
				row.put("jenisUsaha", jenis.toString());
				ProvisioningJob job = (ProvisioningJob) session.createCriteria(ProvisioningJob.class)
						.add(Restrictions.eq("pendaftaranTenant.id", pt.getId()))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
				row.put("jobStatus", job == null ? "" : job.getStatus());
				row.put("jobStage", job == null || job.getCurrentStage() == null ? "" : job.getCurrentStage());
				TenantRegistry t = pt.getTenantRegistry();
				row.put("tenantStatus", t == null ? "" : t.getStatus());
				arr.put(row);
			}
			hasil.put("status", "00");
			hasil.put("halaman", halaman);
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/** Detail step provisioning satu permohonan (monitor §15 tab Provisioning). */
	public static JSONObject detailProvisioning(String kode) throws Exception {
		JSONObject hasil = new JSONObject();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PendaftaranTenant pt = cariByKode(session, kode);
			if (pt == null) {
				return tolak(hasil, "REGISTRATION_NOT_FOUND", "Permohonan tidak ditemukan.");
			}
			ProvisioningJob job = (ProvisioningJob) session.createCriteria(ProvisioningJob.class)
					.add(Restrictions.eq("pendaftaranTenant.id", pt.getId()))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			hasil.put("status", "00");
			hasil.put("kode", pt.getRegistrationCode());
			hasil.put("statusPermohonan", pt.getStatus());
			JSONArray steps = new JSONArray();
			if (job != null) {
				hasil.put("jobStatus", job.getStatus());
				hasil.put("attempt", job.getAttempt());
				hasil.put("lockedBy", job.getLockedBy() == null ? "" : job.getLockedBy());
				hasil.put("errorMessage", job.getErrorMessageSafe() == null ? "" : job.getErrorMessageSafe());
				List<?> daftar = session.createCriteria(ProvisioningStep.class)
						.add(Restrictions.eq("job.id", job.getId())).addOrder(Order.asc("id")).list();
				for (Object o : daftar) {
					ProvisioningStep s = (ProvisioningStep) o;
					JSONObject row = new JSONObject();
					row.put("step", s.getStepCode());
					row.put("statusStep", s.getStatus());
					row.put("attempt", s.getAttempt());
					row.put("error", s.getErrorMessageSafe() == null ? "" : s.getErrorMessageSafe());
					steps.put(row);
				}
			}
			hasil.put("steps", steps);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/** Setujui permohonan REVIEW_PENDING → antre provisioning. Reason wajib. */
	public static JSONObject approve(Tbmuser admin, String kode, String reason) throws Exception {
		return prosesKeputusan(admin, kode, reason, true);
	}

	/** Tolak permohonan (REVIEW_PENDING / EMAIL_VERIFICATION_PENDING). Reason wajib; tanpa hard delete. */
	public static JSONObject reject(Tbmuser admin, String kode, String reason) throws Exception {
		return prosesKeputusan(admin, kode, reason, false);
	}

	private static JSONObject prosesKeputusan(Tbmuser admin, String kode, String reason, boolean setuju)
			throws Exception {
		JSONObject hasil = new JSONObject();
		if (reason == null || reason.trim().isEmpty()) {
			return tolak(hasil, "REASON_REQUIRED", "Alasan wajib diisi.");
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			PendaftaranTenant pt = cariByKode(session, kode);
			if (pt == null) {
				session.getTransaction().rollback();
				return tolak(hasil, "REGISTRATION_NOT_FOUND", "Permohonan tidak ditemukan.");
			}
			Date sekarang = new Date();
			if (setuju) {
				if (!PendaftaranTenant.STATUS_REVIEW_PENDING.equals(pt.getStatus())) {
					session.getTransaction().rollback();
					return tolak(hasil, "STATE_INVALID", "Hanya permohonan REVIEW_PENDING yang dapat disetujui.");
				}
				pt.setStatus(PendaftaranTenant.STATUS_PROVISIONING_QUEUED);
				pt.setCurrentStage("PROVISIONING");
				pt.setApprovedAt(sekarang);
				buatJobBilaBelumAda(session, pt, sekarang);
			} else {
				if (!PendaftaranTenant.STATUS_REVIEW_PENDING.equals(pt.getStatus())
						&& !PendaftaranTenant.STATUS_EMAIL_VERIFICATION_PENDING.equals(pt.getStatus())) {
					session.getTransaction().rollback();
					return tolak(hasil, "STATE_INVALID", "Permohonan pada tahap ini tidak dapat ditolak.");
				}
				pt.setStatus(PendaftaranTenant.STATUS_REJECTED);
				pt.setCurrentStage("REJECTED");
				pt.setRejectedAt(sekarang);
				pt.setFailureCode("REJECTED_BY_ADMIN");
				// Pesan publik aman; reason internal admin tersimpan di audit (bukan di sini).
				pt.setFailureMessageSafe("Pendaftaran tidak dapat diproses. Silakan hubungi dukungan.");
				lepasReservasi(session, pt, sekarang);
			}
			session.saveOrUpdate(pt);
			audit(session, setuju ? "REGISTRATION_APPROVED" : PendaftaranAuditEvent.EV_REGISTRATION_REJECTED,
					pt, admin, reason);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("code", setuju ? "REGISTRATION_APPROVED" : "REGISTRATION_REJECTED");
			hasil.put("description", setuju ? "Permohonan disetujui dan diantrekan provisioning."
					: "Permohonan ditolak.");
			return hasil;
		} catch (Exception e) {
			rollbackDiam(session, "prosesKeputusan");
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Retry provisioning (PROVISIONING_FAILED → QUEUED); step SUCCESS/SKIPPED tetap dilewati. */
	public static JSONObject retry(Tbmuser admin, String kode, String reason) throws Exception {
		JSONObject hasil = new JSONObject();
		if (reason == null || reason.trim().isEmpty()) {
			return tolak(hasil, "REASON_REQUIRED", "Alasan wajib diisi.");
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			PendaftaranTenant pt = cariByKode(session, kode);
			if (pt == null) {
				session.getTransaction().rollback();
				return tolak(hasil, "REGISTRATION_NOT_FOUND", "Permohonan tidak ditemukan.");
			}
			if (!PendaftaranTenant.STATUS_PROVISIONING_FAILED.equals(pt.getStatus())) {
				session.getTransaction().rollback();
				return tolak(hasil, "STATE_INVALID", "Hanya permohonan PROVISIONING_FAILED yang dapat di-retry.");
			}
			ProvisioningJob job = (ProvisioningJob) session.createCriteria(ProvisioningJob.class)
					.add(Restrictions.eq("pendaftaranTenant.id", pt.getId()))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			Date sekarang = new Date();
			if (job == null) {
				buatJobBilaBelumAda(session, pt, sekarang);
			} else {
				job.setStatus(ProvisioningJob.STATUS_QUEUED);
				job.setRetryAt(sekarang);
				job.setAttempt(Integer.valueOf(0)); // reset jatah auto-retry atas keputusan admin
				job.setLockedBy(null);
				session.saveOrUpdate(job);
			}
			pt.setStatus(PendaftaranTenant.STATUS_PROVISIONING_QUEUED);
			pt.setCurrentStage("PROVISIONING");
			pt.setFailureCode(null);
			pt.setFailureMessageSafe(null);
			session.saveOrUpdate(pt);
			audit(session, PendaftaranAuditEvent.EV_PROVISIONING_RETRIED, pt, admin, reason);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("code", "PROVISIONING_RETRIED");
			hasil.put("description", "Provisioning diantrekan ulang (melanjutkan dari step yang belum sukses).");
			return hasil;
		} catch (Exception e) {
			rollbackDiam(session, "retry");
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Lepas reservasi username -- DITOLAK bila sudah CONSUMED atau tenant/schema sudah ada (§15). */
	public static JSONObject releaseReservation(Tbmuser admin, String kode, String reason) throws Exception {
		JSONObject hasil = new JSONObject();
		if (reason == null || reason.trim().isEmpty()) {
			return tolak(hasil, "REASON_REQUIRED", "Alasan wajib diisi.");
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			PendaftaranTenant pt = cariByKode(session, kode);
			if (pt == null) {
				session.getTransaction().rollback();
				return tolak(hasil, "REGISTRATION_NOT_FOUND", "Permohonan tidak ditemukan.");
			}
			if (pt.getTenantRegistry() != null) {
				session.getTransaction().rollback();
				return tolak(hasil, "RELEASE_NOT_ALLOWED",
						"Tenant sudah terbentuk -- reservasi tidak dapat dilepas.");
			}
			SchemaNameReservation r = (SchemaNameReservation) session
					.createCriteria(SchemaNameReservation.class)
					.add(Restrictions.eq("pendaftaranTenant.id", pt.getId()))
					.add(Restrictions.eq("status", SchemaNameReservation.STATUS_RESERVED))
					.setMaxResults(1).uniqueResult();
			if (r == null) {
				session.getTransaction().rollback();
				return tolak(hasil, "RESERVATION_NOT_FOUND", "Tidak ada reservasi aktif utk permohonan ini.");
			}
			r.setStatus(SchemaNameReservation.STATUS_RELEASED);
			r.setReleasedAt(new Date());
			session.saveOrUpdate(r);
			audit(session, "RESERVATION_RELEASED", pt, admin, reason);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("code", "RESERVATION_RELEASED");
			hasil.put("description", "Reservasi username dilepas.");
			return hasil;
		} catch (Exception e) {
			rollbackDiam(session, "releaseReservation");
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Verifikasi email MANUAL oleh admin (mis. pengiriman email platform sedang nonaktif). */
	public static JSONObject verifikasiManual(Tbmuser admin, String kode, String reason) throws Exception {
		JSONObject hasil = new JSONObject();
		if (reason == null || reason.trim().isEmpty()) {
			return tolak(hasil, "REASON_REQUIRED", "Alasan wajib diisi.");
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Long permohonanId = null;
		try {
			session.beginTransaction();
			PendaftaranTenant pt = cariByKode(session, kode);
			if (pt == null) {
				session.getTransaction().rollback();
				return tolak(hasil, "REGISTRATION_NOT_FOUND", "Permohonan tidak ditemukan.");
			}
			if (!PendaftaranTenant.STATUS_EMAIL_VERIFICATION_PENDING.equals(pt.getStatus())) {
				session.getTransaction().rollback();
				return tolak(hasil, "STATE_INVALID", "Permohonan tidak sedang menunggu verifikasi email.");
			}
			permohonanId = pt.getId();
			audit(session, "MANUAL_VERIFICATION", pt, admin, reason);
			session.getTransaction().commit();
		} catch (Exception e) {
			rollbackDiam(session, "verifikasiManual");
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		// Jalur transisi status SAMA dgn verifikasi token (satu sumber aturan):
		// tandai challenge PENDING terbaru sbg CONSUMED lewat service publik TIDAK bisa
		// (butuh token mentah) -- maka transisi dilakukan setara: langsung VERIFIED via
		// verifikasiEmailTanpaToken.
		return PendaftaranTenantService.verifikasiTanpaToken(permohonanId);
	}

	// =====================================================================
	// UTIL
	// =====================================================================

	private static void buatJobBilaBelumAda(Session session, PendaftaranTenant pt, Date sekarang) {
		ProvisioningJob ada = (ProvisioningJob) session.createCriteria(ProvisioningJob.class)
				.add(Restrictions.eq("pendaftaranTenant.id", pt.getId())).setMaxResults(1).uniqueResult();
		if (ada != null) {
			if (!ProvisioningJob.STATUS_SUCCESS.equals(ada.getStatus())) {
				ada.setStatus(ProvisioningJob.STATUS_QUEUED);
				ada.setRetryAt(sekarang);
				ada.setLockedBy(null);
				session.saveOrUpdate(ada);
			}
			return;
		}
		ProvisioningJob job = new ProvisioningJob();
		job.setPendaftaranTenant(pt);
		job.setStatus(ProvisioningJob.STATUS_QUEUED);
		job.setAttempt(Integer.valueOf(0));
		job.setRetryAt(sekarang);
		job.setCreatedAt(sekarang);
		job.setOleh("admin");
		job.setOlehId("admin");
		session.save(job);
	}

	private static void lepasReservasi(Session session, PendaftaranTenant pt, Date sekarang) {
		SchemaNameReservation r = (SchemaNameReservation) session.createCriteria(SchemaNameReservation.class)
				.add(Restrictions.eq("pendaftaranTenant.id", pt.getId()))
				.add(Restrictions.eq("status", SchemaNameReservation.STATUS_RESERVED))
				.setMaxResults(1).uniqueResult();
		if (r != null) {
			r.setStatus(SchemaNameReservation.STATUS_RELEASED);
			r.setReleasedAt(sekarang);
			session.saveOrUpdate(r);
		}
	}

	/** id pendaftar pemilik permohonan (utk aksi lintas-service spt rekonsiliasi P9); null bila tak ada. */
	public static Long pendaftarIdDariKode(String kode) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PendaftaranTenant pt = cariByKode(session, kode);
			return pt == null || pt.getPendaftar() == null ? null : pt.getPendaftar().getId();
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static PendaftaranTenant cariByKode(Session session, String kode) {
		if (kode == null || kode.trim().isEmpty() || kode.length() > 40) {
			return null;
		}
		return (PendaftaranTenant) session.createCriteria(PendaftaranTenant.class)
				.add(Restrictions.eq("registrationCode", kode.trim())).setMaxResults(1).uniqueResult();
	}

	private static void audit(Session session, String eventCode, PendaftaranTenant pt, Tbmuser admin,
			String reason) {
		try {
			PendaftaranAuditEvent ev = new PendaftaranAuditEvent();
			ev.setEventCode(eventCode);
			ev.setActorType(PendaftaranAuditEvent.ACTOR_ADMIN);
			ev.setPendaftarId(pt.getPendaftar() == null ? null : pt.getPendaftar().getId());
			ev.setRegistrationId(pt.getId());
			ev.setReason(reason != null && reason.length() > 500 ? reason.substring(0, 500) : reason);
			ev.setResult("OK");
			ev.setWaktu(new Date());
			if (admin != null) {
				ev.setOleh(admin.getUserNama());
				ev.setOlehId(admin.getUserId());
			}
			session.save(ev);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftaranTenantAdminService.audit:" + eventCode);
		}
	}

	private static JSONObject tolak(JSONObject hasil, String code, String description) throws Exception {
		hasil.put("status", "91");
		hasil.put("code", code);
		hasil.put("description", description);
		return hasil;
	}

	private static void rollbackDiam(Session session, String lokasi) {
		try {
			if (session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftaranTenantAdminService." + lokasi + ".rollback");
		}
	}
}
