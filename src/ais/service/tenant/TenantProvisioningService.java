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

	/**
	 * Mode provisioning tenant yang berlaku saat ini, dibaca dari konfigurasi
	 * {@code pendaftaran_tenant_mode}. Nilai yang tidak dikenali (termasuk kosong atau galat
	 * membaca konfigurasi) selalu jatuh ke {@link TenantRegistry#MODE_LEGACY} -- default paling
	 * aman, sebab step schema per-tenant memang di-{@code SKIP} pada mode ini.
	 *
	 * @return salah satu dari {@link TenantRegistry#MODE_LEGACY}, {@link TenantRegistry#MODE_HYBRID},
	 *         atau {@link TenantRegistry#MODE_TENANT_ONLY}
	 */
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

	/**
	 * Batas maksimal percobaan (attempt) sebuah job sebelum {@link #tandaiGagal} menandainya
	 * FAILED final alih-alih dijadwalkan retry lagi. Dibaca dari konfigurasi
	 * {@code pendaftaran_provisioning_max_attempt} (default {@code 3}); nilai yang tidak dapat
	 * diparse jatuh ke default yang sama.
	 */
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

	/**
	 * Jalankan satu step provisioning dalam transaksi tersendiri, dengan pencatatan
	 * {@link ProvisioningStep} yang membuat seluruh proses idempoten terhadap retry.
	 *
	 * <p>
	 * Urutan kerja: (1) muat {@link ProvisioningJob}; bila sudah tidak ada atau berstatus
	 * CANCELLED, batalkan dan kembalikan {@code false} tanpa menandai gagal (job memang sengaja
	 * dihentikan, bukan error); (2) cari baris {@link ProvisioningStep} untuk kombinasi
	 * job+stepCode -- bila statusnya sudah SUCCESS atau SKIPPED dari attempt sebelumnya, step ini
	 * dilewati dan {@code true} langsung dikembalikan (invariant #11: schema/owner/seed tidak
	 * pernah dibuat ganda); (3) bila belum ada atau belum selesai, step ditandai RUNNING,
	 * attempt-nya dinaikkan, dan {@code job.currentStage} diperbarui; (4) logika sesungguhnya
	 * step dijalankan lewat {@link #logikaUntuk(String)} -- bila melempar {@link LewatiStep},
	 * step ditandai SKIPPED (bukan gagal); bila berhasil, ditandai SUCCESS beserta metadata
	 * singkatnya; (5) transaksi di-commit.
	 * </p>
	 *
	 * <p>
	 * Bila logika step melempar exception lain (bukan {@link LewatiStep}), transaksi ini
	 * dibatalkan, galatnya diaudit, dan {@link #tandaiGagal} dipanggil pada transaksi terpisah
	 * untuk menandai step/job/permohonan gagal serta menjadwalkan retry bila attempt masih
	 * tersisa.
	 * </p>
	 *
	 * @param jobId    id {@link ProvisioningJob} yang sedang diproses
	 * @param stepCode salah satu kode di {@link #URUTAN_STEP}
	 * @return {@code true} bila step ini beres (baru dijalankan, sudah SUCCESS/SKIPPED
	 *         sebelumnya, atau job sudah tidak berlaku lagi), {@code false} bila step ini gagal
	 *         dan job sudah ditandai lewat {@link #tandaiGagal}
	 */
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

	/**
	 * Bangun {@link LogikaStep} yang menjalankan logika satu step provisioning tertentu, sesuai
	 * {@code stepCode}. Ini adalah satu-satunya tempat yang tahu APA yang dilakukan tiap step
	 * §7.9 -- {@link #langkah} hanya tahu BAGAIMANA membungkusnya (transaksi, status, retry).
	 *
	 * <p>
	 * Setiap cabang {@code if} di badan {@link LogikaStep#jalankan} menangani satu
	 * {@code stepCode} dari {@link #URUTAN_STEP}, seluruhnya idempoten terhadap pemanggilan ulang
	 * (baik karena dicoba ulang oleh {@link #langkah} maupun karena job yang sama diklaim ulang
	 * setelah kegagalan sebagian):
	 * </p>
	 * <ul>
	 * <li>{@code VALIDATE_REGISTRATION} -- pastikan status permohonan masih berada di salah satu
	 * status antrean/provisioning/gagal yang sah, lalu set ke PROVISIONING.</li>
	 * <li>{@code RESERVE_USERNAME} -- pastikan {@link SchemaNameReservation} milik permohonan ini
	 * ada dan berstatus RESERVED/CONSUMED.</li>
	 * <li>{@code CREATE_TENANT_REGISTRY} -- buat baris {@link TenantRegistry} (bila belum ada),
	 * kode tenant {@code TEN-<tahun>-<id>}, serta subdomain bawaan {@code <slug>.<base>} pada
	 * {@link TenantDomain}.</li>
	 * <li>{@code CREATE_SCHEMA_ERP}/{@code CREATE_SCHEMA_AUDIT} -- pada mode LEGACY, dilewati
	 * ({@link LewatiStep}); pada HYBRID/TENANT_ONLY, memastikan schema data+audit tenant ada
	 * lewat {@link TenantSchemaService#buatSchema} dan mencatat nama schema pada registry.</li>
	 * <li>{@code RUN_MIGRATIONS}/{@code INSTALL_AUDIT} -- menerapkan migrasi kanonik
	 * ber-riwayat+checksum ({@link TenantSchemaService#terapkanMigrasi}) ke target ERP/AUDIT;
	 * dilewati pada LEGACY.</li>
	 * <li>{@code SEED_CONFIGURATION} -- set locale default dan timezone tenant (diambil dari
	 * profil pendaftar bila ada, jatuh ke {@code Asia/Jakarta}).</li>
	 * <li>{@code SEED_MODULES} -- terapkan entitlement modul bawaan sesuai jenis usaha lewat
	 * {@link TenantEntitlementService#terapkanDariJenisUsaha}.</li>
	 * <li>{@code SEED_ROLES} -- pada LEGACY, dilewati dengan catatan bahwa peran owner cukup
	 * lewat {@code tenant_membership.role_code}; pada HYBRID/TENANT_ONLY, menyemai delapan peran
	 * bawaan ke {@code role_tenant} lewat {@link TenantRoleSeeder#seedSchema}.</li>
	 * <li>{@code CREATE_OWNER_USER} -- selalu dilewati: akun login owner memakai
	 * {@link Pendaftar} yang sudah ada (PBKDF2), bukan {@code Tbmuser} baru (§10.4).</li>
	 * <li>{@code CREATE_MEMBERSHIP} -- buat {@link TenantMembership} owner (idempoten, dicek
	 * lebih dulu lewat count).</li>
	 * <li>{@code CREATE_SUBSCRIPTION_TRIAL} -- tidak membuat baris apa pun di sini; hanya
	 * mencatat metadata snapshot plan/trial, sebab tanggal trial sesungguhnya baru dihitung saat
	 * {@code MARK_READY}.</li>
	 * <li>{@code VERIFY_SCHEMA} -- verifikasi menyeluruh schema+tabel wajib+riwayat migrasi lewat
	 * {@link TenantSchemaService#verifikasiLengkap}; dilewati pada LEGACY.</li>
	 * <li>{@code VERIFY_LOGIN} -- pastikan akun pemilik punya password hash.</li>
	 * <li>{@code MARK_READY} -- step terakhir: set tenant dan permohonan READY, hitung
	 * {@code trialStartAt}/{@code trialEndAt} dari READY (bukan dari saat form dibuka --
	 * invariant #7), tandai reservasi username CONSUMED, aktifkan akun {@link Pendaftar} bila ini
	 * tenant pertamanya, dan catat {@link PendaftaranAuditEvent#EV_TENANT_READY}.</li>
	 * </ul>
	 *
	 * <p>
	 * {@code stepCode} yang tidak cocok satu pun cabang di atas melempar
	 * {@link IllegalStateException} -- fail-closed, bukan diam-diam berhasil.
	 * </p>
	 *
	 * @param stepCode kode step, salah satu dari {@link #URUTAN_STEP}
	 * @return {@link LogikaStep} yang, saat dijalankan, mengeksekusi step tersebut dan
	 *         mengembalikan metadata singkat untuk kolom {@code metadata_json}
	 */
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

	/**
	 * Tandai job SUCCESS setelah seluruh step di {@link #URUTAN_STEP} berhasil (atau SKIPPED).
	 * Dipanggil {@link #jalankanJob} sekali di akhir; idempoten -- job yang sudah SUCCESS tidak
	 * ditulis ulang. Kegagalan menandai (mis. job sudah terhapus) diaudit dan ditelan: seluruh
	 * step sudah berhasil dijalankan, jadi kegagalan di sini murni bookkeeping, bukan kegagalan
	 * provisioning yang sesungguhnya.
	 */
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

	/**
	 * {@link SchemaNameReservation} milik {@code permohonan} yang berstatus RESERVED atau
	 * CONSUMED, atau {@code null} bila tidak ada.
	 */
	private static SchemaNameReservation reservasiMilik(Session session, PendaftaranTenant permohonan) {
		return (SchemaNameReservation) session.createCriteria(SchemaNameReservation.class)
				.add(Restrictions.eq("pendaftaranTenant.id", permohonan.getId()))
				.add(Restrictions.in("status", new String[] { SchemaNameReservation.STATUS_RESERVED,
						SchemaNameReservation.STATUS_CONSUMED }))
				.setMaxResults(1).uniqueResult();
	}

	/**
	 * {@link TenantRegistry} milik {@code job}, dicari berturut-turut dari: field
	 * {@code job.tenant} (sudah tertaut), lalu {@code permohonan.tenantRegistry}, dan terakhir
	 * kueri berdasarkan slug ({@code normalizedUsername}). Tiga jalur ini diperlukan karena step
	 * {@code CREATE_TENANT_REGISTRY} boleh baru saja membuat registry pada attempt sebelumnya
	 * tanpa sempat menautkannya balik ke seluruh entitas terkait.
	 *
	 * @return registry tenant, atau {@code null} bila belum pernah dibuat sama sekali
	 */
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

	/**
	 * Timezone tenant baru, diambil dari {@code PendaftarTenantProfile} milik pendaftar bila ada;
	 * jatuh ke {@code "Asia/Jakarta"} bila profil tidak ada atau gagal dibaca.
	 */
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

	/** Kode tampil tenant: {@code TEN-<tahun berjalan>-<id, padded 6 digit>}. */
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

	/**
	 * Catat satu baris {@link PendaftaranAuditEvent} beraktor SYSTEM untuk jejak provisioning
	 * (mis. tenant siap, provisioning gagal). Kegagalan mencatat audit diaudit sendiri lewat
	 * {@link ais.common.ErrorAuditUtil} dan tidak dilempar balik -- kegagalan audit trail tidak
	 * boleh menggagalkan step provisioning yang sebenarnya sudah berhasil/gagal.
	 *
	 * @param eventCode  kode event, mis. {@link PendaftaranAuditEvent#EV_TENANT_READY}
	 * @param permohonan permohonan tenant terkait
	 * @param tenantId   id tenant terkait, boleh {@code null} bila belum ada registry
	 */
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
