package ais.service.tenant;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pendaftar;
import ais.database.model.tenant.PendaftaranAuditEvent;
import ais.database.model.tenant.PendaftaranTenant;
import ais.database.model.tenant.ProvisioningJob;
import ais.database.model.tenant.ProvisioningStep;
import ais.database.model.tenant.SchemaNameReservation;
import ais.database.model.tenant.TenantDomain;
import ais.database.model.tenant.TenantMembership;
import ais.database.model.tenant.TenantRegistry;

/**
 * <h3>Eksekusi job provisioning tenant -- step machine idempoten (§10 dokumen master).</h3>
 *
 * <p>Setiap step berjalan pada transaction sendiri dan dicatat di {@code provisioning_step}
 * (unique job+step): step SUCCESS/SKIPPED dilewati saat retry -- schema/owner/seed tidak pernah
 * dibuat ganda (invariant #11). Kegagalan menandai step+job FAILED, permohonan
 * {@code PROVISIONING_FAILED} dgn pesan publik aman, lalu auto-retry terbatas (backoff;
 * konfigurasi {@code pendaftaran_provisioning_max_attempt}, default 3).</p>
 *
 * <p>Mode {@code pendaftaran_tenant_mode}: LEGACY (default) = step schema SKIPPED sah -- tenant
 * mendapat registry+entitlement+membership+trial, data operasional tetap scope per-Pendaftar
 * existing. HYBRID/TENANT_ONLY = schema {@code <slug>}/{@code <slug>__audit} dibuat+diverifikasi
 * ({@code schemaVersion=schema-only-v0}; tabel data-plane menyusul, dicatat jujur).</p>
 */
public final class TenantProvisioningService {

	private TenantProvisioningService() {
	}

	/** Sinyal internal: step tidak berlaku pada mode/kondisi ini -- tandai SKIPPED (bukan gagal). */
	private static class LewatiStep extends Exception {
		private static final long serialVersionUID = 1L;
		LewatiStep(String alasan) {
			super(alasan);
		}
	}

	private interface LogikaStep {
		/** @return metadata singkat utk kolom metadata_json (boleh null). */
		String jalankan(Session session, ProvisioningJob job) throws Exception;
	}

	static String modeTenant() {
		try {
			String v = Common.getKonfigurasi("pendaftaran_tenant_mode", TenantRegistry.MODE_LEGACY).getNilai();
			if (TenantRegistry.MODE_HYBRID.equalsIgnoreCase(v)) {
				return TenantRegistry.MODE_HYBRID;
			}
			if (TenantRegistry.MODE_TENANT_ONLY.equalsIgnoreCase(v)) {
				return TenantRegistry.MODE_TENANT_ONLY;
			}
			return TenantRegistry.MODE_LEGACY;
		} catch (Exception e) {
			return TenantRegistry.MODE_LEGACY;
		}
	}

	private static int maksimalAttempt() {
		try {
			return Integer.parseInt(
					Common.getKonfigurasi("pendaftaran_provisioning_max_attempt", "3").getNilai().trim());
		} catch (Exception e) {
			return 3;
		}
	}

	/** Urutan step kanonik §7.9. */
	private static final String[] URUTAN_STEP = { ProvisioningStep.STEP_VALIDATE_REGISTRATION,
			ProvisioningStep.STEP_RESERVE_USERNAME, ProvisioningStep.STEP_CREATE_TENANT_REGISTRY,
			ProvisioningStep.STEP_CREATE_SCHEMA_ERP, ProvisioningStep.STEP_CREATE_SCHEMA_AUDIT,
			ProvisioningStep.STEP_RUN_MIGRATIONS, ProvisioningStep.STEP_INSTALL_AUDIT,
			ProvisioningStep.STEP_SEED_CONFIGURATION, ProvisioningStep.STEP_SEED_MODULES,
			ProvisioningStep.STEP_SEED_ROLES, ProvisioningStep.STEP_CREATE_OWNER_USER,
			ProvisioningStep.STEP_CREATE_MEMBERSHIP, ProvisioningStep.STEP_CREATE_SUBSCRIPTION_TRIAL,
			ProvisioningStep.STEP_VERIFY_SCHEMA, ProvisioningStep.STEP_VERIFY_LOGIN,
			ProvisioningStep.STEP_MARK_READY };

	/** Jalankan seluruh step satu job (dipanggil worker SETELAH job diklaim). */
	public static void jalankanJob(Long jobId) {
		for (int i = 0; i < URUTAN_STEP.length; i++) {
			if (!langkah(jobId, URUTAN_STEP[i])) {
				return; // gagal -- job sudah ditandai; retry dilanjutkan worker berikutnya
			}
		}
		selesaikanJob(jobId);
	}

	// =====================================================================
	// EKSEKUSI PER-STEP
	// =====================================================================

	private static boolean langkah(Long jobId, String stepCode) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			ProvisioningJob job = (ProvisioningJob) session.get(ProvisioningJob.class, jobId);
			if (job == null || ProvisioningJob.STATUS_CANCELLED.equals(job.getStatus())) {
				session.getTransaction().rollback();
				return false;
			}
			ProvisioningStep step = (ProvisioningStep) session.createCriteria(ProvisioningStep.class)
					.add(Restrictions.eq("job.id", jobId))
					.add(Restrictions.eq("stepCode", stepCode)).setMaxResults(1).uniqueResult();
			if (step != null && (ProvisioningStep.STATUS_SUCCESS.equals(step.getStatus())
					|| ProvisioningStep.STATUS_SKIPPED.equals(step.getStatus()))) {
				session.getTransaction().commit();
				return true; // idempoten: sudah beres pada attempt sebelumnya
			}
			if (step == null) {
				step = new ProvisioningStep();
				step.setJob(job);
				step.setStepCode(stepCode);
				step.setOleh("provisioning");
				step.setOlehId("provisioning");
			}
			Date sekarang = new Date();
			step.setStatus(ProvisioningStep.STATUS_RUNNING);
			step.setAttempt(Integer.valueOf(step.getAttempt().intValue() + 1));
			step.setStartedAt(sekarang);
			session.saveOrUpdate(step);
			job.setCurrentStage(stepCode);
			session.saveOrUpdate(job);

			String metadata;
			try {
				metadata = logikaUntuk(stepCode).jalankan(session, job);
				step.setStatus(ProvisioningStep.STATUS_SUCCESS);
			} catch (LewatiStep lewati) {
				metadata = lewati.getMessage();
				step.setStatus(ProvisioningStep.STATUS_SKIPPED);
			}
			step.setFinishedAt(new Date());
			if (metadata != null) {
				step.setMetadataJson(new org.json.JSONObject().put("info", metadata).toString());
			}
			session.saveOrUpdate(step);
			session.getTransaction().commit();
			return true;
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) TenantProvisioningService.langkah.rollback");
			}
			ais.common.ErrorAuditUtil.record(e, "auto-audit TenantProvisioningService.langkah:" + stepCode);
			tandaiGagal(jobId, stepCode, e);
			return false;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static LogikaStep logikaUntuk(final String stepCode) {
		return new LogikaStep() {
			public String jalankan(Session session, ProvisioningJob job) throws Exception {
				PendaftaranTenant permohonan = job.getPendaftaranTenant();
				String mode = modeTenant();
				boolean legacy = TenantRegistry.MODE_LEGACY.equals(mode);

				if (ProvisioningStep.STEP_VALIDATE_REGISTRATION.equals(stepCode)) {
					if (!PendaftaranTenant.STATUS_PROVISIONING_QUEUED.equals(permohonan.getStatus())
							&& !PendaftaranTenant.STATUS_PROVISIONING.equals(permohonan.getStatus())
							&& !PendaftaranTenant.STATUS_PROVISIONING_FAILED.equals(permohonan.getStatus())) {
						throw new IllegalStateException("Status permohonan bukan antrean provisioning: "
								+ permohonan.getStatus());
					}
					permohonan.setStatus(PendaftaranTenant.STATUS_PROVISIONING);
					session.saveOrUpdate(permohonan);
					return "validasi lulus; mode=" + mode;
				}

				if (ProvisioningStep.STEP_RESERVE_USERNAME.equals(stepCode)) {
					SchemaNameReservation r = reservasiMilik(session, permohonan);
					if (r == null) {
						throw new IllegalStateException("Reservasi username tidak ditemukan/tidak aktif.");
					}
					return "reservasi #" + r.getId() + " status " + r.getStatus();
				}

				if (ProvisioningStep.STEP_CREATE_TENANT_REGISTRY.equals(stepCode)) {
					TenantRegistry tenant = tenantDariJob(session, job);
					if (tenant != null) {
						return "registry sudah ada #" + tenant.getId();
					}
					tenant = new TenantRegistry();
					tenant.setNama(permohonan.getPendaftar().getNama());
					tenant.setSlug(permohonan.getNormalizedUsername());
					tenant.setOwnerPendaftar(permohonan.getPendaftar());
					tenant.setStatus(TenantRegistry.STATUS_PROVISIONING);
					tenant.setTenantMode(mode);
					tenant.setCreatedAt(new Date());
					tenant.setOleh("provisioning");
					tenant.setOlehId("provisioning");
					session.save(tenant);
					session.flush();
					tenant.setCode(kodeTenant(tenant.getId()));
					session.saveOrUpdate(tenant);
					job.setTenant(tenant);
					session.saveOrUpdate(job);
					permohonan.setTenantRegistry(tenant);
					session.saveOrUpdate(permohonan);
					// Subdomain bawaan <slug>.<base> (unique normalized_domain).
					TenantDomain domain = new TenantDomain();
					domain.setTenant(tenant);
					String sub = tenant.getSlug() + "." + ais.service.registration.PendaftaranTenantService
							.subdomainBase();
					domain.setDomain(sub);
					domain.setNormalizedDomain(sub.toLowerCase());
					domain.setType(TenantDomain.TYPE_SUBDOMAIN);
					domain.setStatus(TenantDomain.STATUS_ACTIVE);
					domain.setPrimaryDomain(Boolean.TRUE);
					domain.setVerifiedAt(new Date());
					domain.setCreatedAt(new Date());
					domain.setOleh("provisioning");
					domain.setOlehId("provisioning");
					session.save(domain);
					return "registry #" + tenant.getId() + " slug " + tenant.getSlug();
				}

				TenantRegistry tenant = tenantDariJob(session, job);
				if (tenant == null) {
					throw new IllegalStateException("Registry tenant belum terbentuk.");
				}

				if (ProvisioningStep.STEP_CREATE_SCHEMA_ERP.equals(stepCode)
						|| ProvisioningStep.STEP_CREATE_SCHEMA_AUDIT.equals(stepCode)) {
					if (legacy) {
						throw new LewatiStep("mode LEGACY: schema per-tenant tidak dibuat");
					}
					TenantSchemaService.buatSchema(session, tenant.getSlug());
					if (ProvisioningStep.STEP_CREATE_SCHEMA_AUDIT.equals(stepCode)) {
						tenant.setSchemaName(tenant.getSlug());
						tenant.setAuditSchemaName(tenant.getSlug() + "__audit");
						tenant.setSchemaVersion(TenantSchemaService.SCHEMA_VERSION_AWAL);
						session.saveOrUpdate(tenant);
					}
					return "schema " + tenant.getSlug() + " dipastikan ada";
				}

				if (ProvisioningStep.STEP_RUN_MIGRATIONS.equals(stepCode)) {
					if (legacy) {
						throw new LewatiStep("mode LEGACY: tidak ada migrasi schema per-tenant");
					}
					// P7 HYBRID: migrasi kanonik ber-riwayat+checksum (BUKAN hbm2ddl) -- §10.2.
					String hasilMigrasi = TenantSchemaService.terapkanMigrasi(session,
							tenant.getSlug(), TenantSchemaMigrations.TARGET_ERP);
					tenant.setSchemaVersion(TenantSchemaMigrations.VERSI_TERKINI);
					session.saveOrUpdate(tenant);
					return "ERP " + hasilMigrasi + " checksum=" + TenantSchemaService
							.checksumTarget(TenantSchemaMigrations.TARGET_ERP).substring(0, 12);
				}

				if (ProvisioningStep.STEP_INSTALL_AUDIT.equals(stepCode)) {
					if (legacy) {
						throw new LewatiStep("mode LEGACY: tidak ada audit schema per-tenant");
					}
					String hasilAudit = TenantSchemaService.terapkanMigrasi(session,
							tenant.getSlug(), TenantSchemaMigrations.TARGET_AUDIT);
					return "AUDIT " + hasilAudit + " checksum=" + TenantSchemaService
							.checksumTarget(TenantSchemaMigrations.TARGET_AUDIT).substring(0, 12);
				}

				if (ProvisioningStep.STEP_SEED_CONFIGURATION.equals(stepCode)) {
					tenant.setDefaultLocale("id_ID");
					tenant.setTimezone(zonaWaktu(session, permohonan));
					session.saveOrUpdate(tenant);
					return "locale/timezone di-set";
				}

				if (ProvisioningStep.STEP_SEED_MODULES.equals(stepCode)) {
					int dibuat = TenantEntitlementService.terapkanDariJenisUsaha(session, tenant, permohonan);
					return dibuat + " entitlement BUSINESS_TYPE dibuat";
				}

				if (ProvisioningStep.STEP_SEED_ROLES.equals(stepCode)) {
					// Mode LEGACY: role tenant = membership role_code (OWNER); Tbmrole global
					// TIDAK disentuh (role tenant tidak boleh mengubah platform super admin §10.4).
					if (legacy) {
						return "mode LEGACY: role owner via tenant_membership.role_code "
								+ "(tanpa Tbmrole baru)";
					}
					// HYBRID/TENANT_ONLY: tanpa langkah ini tabel role_tenant berdiri kosong,
					// sehingga tidak ada satu peran pun yang dapat diberikan kepada pengguna --
					// matriks TenantRbac lengkap di kode tetapi tidak terpakai. Idempoten, jadi
					// aman pada provisioning ulang maupun pemulihan.
					int disemai = TenantRoleSeeder.seedSchema(session, tenant.getSlug(),
							"provisioning");
					return disemai + " peran bawaan disemai ke role_tenant (dari "
							+ TenantRoleSeeder.kodeBawaan().length + " peran baku)";
				}

				if (ProvisioningStep.STEP_CREATE_OWNER_USER.equals(stepCode)) {
					// Akun login owner = Pendaftar (PBKDF2) -- TIDAK membuat Tbmuser baru di sini:
					// menyalin hash ke jalur DES dilarang (§10.4); adapter Tbmuser bila diperlukan
					// dibuat sadar lewat backoffice admin, bukan otomatis.
					throw new LewatiStep("owner login memakai akun Pendaftar existing (tanpa Tbmuser baru)");
				}

				if (ProvisioningStep.STEP_CREATE_MEMBERSHIP.equals(stepCode)) {
					Number ada = (Number) session.createCriteria(TenantMembership.class)
							.add(Restrictions.eq("tenant.id", tenant.getId()))
							.add(Restrictions.eq("pendaftar.id", permohonan.getPendaftar().getId()))
							.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
					if (ada != null && ada.longValue() > 0) {
						return "membership owner sudah ada";
					}
					TenantMembership m = new TenantMembership();
					m.setTenant(tenant);
					m.setPendaftar(permohonan.getPendaftar());
					m.setStatus(TenantMembership.STATUS_ACTIVE);
					m.setIsOwner(Boolean.TRUE);
					m.setRoleCode(TenantMembership.ROLE_OWNER);
					m.setValidFrom(new Date());
					m.setCreatedAt(new Date());
					m.setOleh("provisioning");
					m.setOlehId("provisioning");
					session.save(m);
					return "membership owner dibuat";
				}

				if (ProvisioningStep.STEP_CREATE_SUBSCRIPTION_TRIAL.equals(stepCode)) {
					return "snapshot plan=" + permohonan.getSelectedPlanVersion() + " trialHari="
							+ permohonan.getTrialDaysSnapshot() + " (tanggal dihitung saat MARK_READY)";
				}

				if (ProvisioningStep.STEP_VERIFY_SCHEMA.equals(stepCode)) {
					if (legacy) {
						throw new LewatiStep("mode LEGACY: tanpa schema per-tenant");
					}
					// P7: verifikasi menyeluruh -- schema + seluruh tabel wajib + riwayat migrasi.
					TenantSchemaService.verifikasiLengkap(session, tenant.getSlug());
					return "schema+tabel+riwayat migrasi terverifikasi (" +
							TenantSchemaMigrations.VERSI_TERKINI + ")";
				}

				if (ProvisioningStep.STEP_VERIFY_LOGIN.equals(stepCode)) {
					Pendaftar pemilik = permohonan.getPendaftar();
					if (pemilik.getPasswordHash() == null || pemilik.getPasswordHash().trim().isEmpty()) {
						throw new IllegalStateException("Akun owner tidak memiliki kredensial login.");
					}
					return "kredensial owner tersedia (hash PBKDF2)";
				}

				if (ProvisioningStep.STEP_MARK_READY.equals(stepCode)) {
					Date ready = new Date();
					int hari = permohonan.getTrialDaysSnapshot() == null ? 30
							: permohonan.getTrialDaysSnapshot().intValue();
					// Invariant #7: trial mulai dari READY, bukan saat form dibuka.
					tenant.setStatus(TenantRegistry.STATUS_READY);
					tenant.setTrialStartAt(ready);
					tenant.setTrialEndAt(new Date(ready.getTime() + hari * 24L * 3600L * 1000L));
					session.saveOrUpdate(tenant);
					permohonan.setStatus(PendaftaranTenant.STATUS_READY);
					permohonan.setReadyAt(ready);
					permohonan.setCurrentStage("READY");
					session.saveOrUpdate(permohonan);
					SchemaNameReservation r = reservasiMilik(session, permohonan);
					if (r != null && SchemaNameReservation.STATUS_RESERVED.equals(r.getStatus())) {
						r.setStatus(SchemaNameReservation.STATUS_CONSUMED);
						r.setConsumedAt(ready);
						session.saveOrUpdate(r);
					}
					// Aktifkan akun Pendaftar saat tenant PERTAMA-nya READY (05-workflow #2).
					Pendaftar pemilik = permohonan.getPendaftar();
					if (!Boolean.TRUE.equals(pemilik.getAktif())) {
						pemilik.setAktif(Boolean.TRUE);
						session.saveOrUpdate(pemilik);
					}
					auditEvent(session, PendaftaranAuditEvent.EV_TENANT_READY, permohonan, tenant.getId());
					return "READY; trial s.d. " + tenant.getTrialEndAt();
				}

				throw new IllegalStateException("Step tidak dikenal: " + stepCode);
			}
		};
	}

	private static void selesaikanJob(Long jobId) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			ProvisioningJob job = (ProvisioningJob) session.get(ProvisioningJob.class, jobId);
			if (job != null && !ProvisioningJob.STATUS_SUCCESS.equals(job.getStatus())) {
				job.setStatus(ProvisioningJob.STATUS_SUCCESS);
				job.setFinishedAt(new Date());
				job.setLockedBy(null);
				session.saveOrUpdate(job);
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TenantProvisioningService.selesaikanJob");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Tandai kegagalan: step FAILED + job retry-backoff/FAILED final + permohonan PROVISIONING_FAILED. */
	private static void tandaiGagal(Long jobId, String stepCode, Exception e) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			ProvisioningJob job = (ProvisioningJob) session.get(ProvisioningJob.class, jobId);
			if (job == null) {
				session.getTransaction().rollback();
				return;
			}
			Date sekarang = new Date();
			ProvisioningStep step = (ProvisioningStep) session.createCriteria(ProvisioningStep.class)
					.add(Restrictions.eq("job.id", jobId))
					.add(Restrictions.eq("stepCode", stepCode)).setMaxResults(1).uniqueResult();
			if (step != null) {
				step.setStatus(ProvisioningStep.STATUS_FAILED);
				step.setFinishedAt(sekarang);
				step.setErrorCode("STEP_FAILED");
				step.setErrorMessageSafe(pesanAman(e));
				session.saveOrUpdate(step);
			}
			boolean masihBolehRetry = job.getAttempt().intValue() < maksimalAttempt();
			job.setErrorCode("STEP_FAILED:" + stepCode);
			job.setErrorMessageSafe(pesanAman(e));
			job.setLockedBy(null);
			if (masihBolehRetry) {
				job.setStatus(ProvisioningJob.STATUS_QUEUED);
				job.setRetryAt(new Date(sekarang.getTime() + job.getAttempt().intValue() * 5L * 60L * 1000L));
			} else {
				job.setStatus(ProvisioningJob.STATUS_FAILED);
				job.setFinishedAt(sekarang);
			}
			session.saveOrUpdate(job);

			PendaftaranTenant permohonan = job.getPendaftaranTenant();
			permohonan.setStatus(PendaftaranTenant.STATUS_PROVISIONING_FAILED);
			permohonan.setFailureCode("PROVISIONING_FAILED");
			permohonan.setFailureMessageSafe(masihBolehRetry
					? "Penyiapan tenant tertunda dan akan dicoba ulang otomatis."
					: "Penyiapan tenant gagal. Tim kami akan menindaklanjuti; Anda juga dapat menghubungi dukungan.");
			session.saveOrUpdate(permohonan);
			auditEvent(session, PendaftaranAuditEvent.EV_PROVISIONING_FAILED, permohonan,
					job.getTenant() == null ? null : job.getTenant().getId());
			session.getTransaction().commit();
		} catch (Exception e2) {
			ais.common.ErrorAuditUtil.record(e2, "auto-audit TenantProvisioningService.tandaiGagal");
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) TenantProvisioningService.tandaiGagal.rollback");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =====================================================================
	// UTIL
	// =====================================================================

	private static SchemaNameReservation reservasiMilik(Session session, PendaftaranTenant permohonan) {
		return (SchemaNameReservation) session.createCriteria(SchemaNameReservation.class)
				.add(Restrictions.eq("pendaftaranTenant.id", permohonan.getId()))
				.add(Restrictions.in("status", new String[] { SchemaNameReservation.STATUS_RESERVED,
						SchemaNameReservation.STATUS_CONSUMED }))
				.setMaxResults(1).uniqueResult();
	}

	private static TenantRegistry tenantDariJob(Session session, ProvisioningJob job) {
		if (job.getTenant() != null) {
			return job.getTenant();
		}
		PendaftaranTenant permohonan = job.getPendaftaranTenant();
		if (permohonan.getTenantRegistry() != null) {
			return permohonan.getTenantRegistry();
		}
		return (TenantRegistry) session.createCriteria(TenantRegistry.class)
				.add(Restrictions.eq("slug", permohonan.getNormalizedUsername()))
				.setMaxResults(1).uniqueResult();
	}

	private static String zonaWaktu(Session session, PendaftaranTenant permohonan) {
		try {
			ais.database.model.tenant.PendaftarTenantProfile profile =
					(ais.database.model.tenant.PendaftarTenantProfile) session
							.createCriteria(ais.database.model.tenant.PendaftarTenantProfile.class)
							.add(Restrictions.eq("pendaftar.id", permohonan.getPendaftar().getId()))
							.setMaxResults(1).uniqueResult();
			return profile == null ? "Asia/Jakarta" : profile.getTimezone();
		} catch (Exception e) {
			return "Asia/Jakarta";
		}
	}

	private static String kodeTenant(Long id) {
		java.util.Calendar cal = java.util.Calendar.getInstance();
		String idPadded = String.valueOf(id.longValue());
		while (idPadded.length() < 6) {
			idPadded = "0" + idPadded;
		}
		return "TEN-" + cal.get(java.util.Calendar.YEAR) + "-" + idPadded;
	}

	/** Pesan aman publik: TANPA nama schema/SQL/stack -- detail teknis ada di error_log. */
	private static String pesanAman(Exception e) {
		String kelas = e.getClass().getSimpleName();
		return "Langkah penyiapan gagal (" + kelas + "). Detail teknis tercatat internal.";
	}

	private static void auditEvent(Session session, String eventCode, PendaftaranTenant permohonan, Long tenantId) {
		try {
			PendaftaranAuditEvent ev = new PendaftaranAuditEvent();
			ev.setEventCode(eventCode);
			ev.setActorType(PendaftaranAuditEvent.ACTOR_SYSTEM);
			ev.setPendaftarId(permohonan.getPendaftar() == null ? null : permohonan.getPendaftar().getId());
			ev.setRegistrationId(permohonan.getId());
			ev.setTenantId(tenantId);
			ev.setResult("OK");
			ev.setWaktu(new Date());
			ev.setOleh("provisioning");
			ev.setOlehId("provisioning");
			session.save(ev);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TenantProvisioningService.auditEvent:" + eventCode);
		}
	}
}
