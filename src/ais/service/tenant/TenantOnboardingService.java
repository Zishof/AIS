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

	/**
	 * Gerbang utama kelas ini -- dipanggil sebelum aksi mutasi dashboard pendaftar
	 * untuk memutuskan apakah pendaftar tersebut boleh melanjutkan. Urutan pemeriksaannya
	 * (berhenti pada pemeriksaan pertama yang gagal, fail-closed):
	 * <ol>
	 * <li><b>RE-FETCH dari DB</b> -- {@code segera} diambil ulang lewat {@code session.get},
	 * BUKAN entity yang mungkin sudah detached di sesi HTTP; status akun harus selalu keadaan
	 * terkini, tidak boleh memakai salinan basi. Akun tidak ditemukan langsung ditolak.</li>
	 * <li><b>Deteksi akun LEGACY</b> -- bila pendaftar TIDAK PERNAH memiliki baris
	 * {@code pendaftaran_tenant} (belum pernah lewat program pendaftaran tenant), gerbang ini
	 * TIDAK berlaku baginya sama sekali -- langsung {@code return null} (boleh), mempertahankan
	 * perilaku lama sebelum program tenant ada (keputusan G-06, fail-open untuk data existing).
	 * Pemeriksaan-pemeriksaan berikutnya HANYA berlaku bagi pendaftar program tenant.</li>
	 * <li><b>Akun aktif</b> -- {@code segar.getAktif()} harus {@code true} (verifikasi email
	 * selesai).</li>
	 * <li><b>Minimal satu tenant siap</b> -- pendaftar harus memiliki (sebagai
	 * {@code ownerPendaftar}) minimal satu {@link TenantRegistry} berstatus READY atau ACTIVE.</li>
	 * <li><b>Gerbang TENANT_ONLY (P8 §3.3)</b> -- lewat {@link
	 * TenantDataPlaneService#alasanBlokirTenantOnly}: bila platform berjalan pada mode
	 * TENANT_ONLY dan tenant pendaftar belum punya schema terprovision, mutasi diblokir dengan
	 * pesan yang mengarahkan ke dukungan.</li>
	 * <li><b>Entitlement modul</b> (hanya bila {@code modulDibutuhkan} diberikan) -- minimal SATU
	 * dari tenant-tenant siap milik pendaftar harus memiliki entitlement modul tersebut berstatus
	 * ACTIVE (bukan PLANNED); bila tidak satu pun, ditolak dengan pesan yang menyebut nama
	 * modulnya.</li>
	 * </ol>
	 * <p>
	 * Bila terjadi galat tak terduga di tengah pemeriksaan (mis. kegagalan Hibernate), method
	 * TIDAK meloloskan pendaftar secara diam-diam -- ia memilih pesan generik aman ("Tidak dapat
	 * memeriksa status tenant saat ini") sebagai perilaku <b>fail-closed</b>, sebab pada titik
	 * kegagalan tidak dapat dipastikan lagi apakah pendaftar ini akun legacy (yang seharusnya
	 * boleh) atau akun program tenant yang belum siap (yang seharusnya ditolak).
	 * </p>
	 *
	 * @param pendaftarId     id Pendaftar yang mutasinya hendak diizinkan/ditolak.
	 * @param modulDibutuhkan kode modul yang disyaratkan aksi ini, atau {@code null}/kosong bila
	 *                        aksi tidak spesifik modul (mis. {@code tenant_list}).
	 * @return {@code null} bila boleh melanjutkan; selain itu pesan penolakan yang aman
	 *         ditampilkan ke pengguna (tanpa detail internal).
	 */
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
			// P8 §3.3: mode platform TENANT_ONLY -- tenant program TANPA schema valid diblokir
			// menjalankan data-plane (mode dibaca SQL langsung, efek seketika tanpa restart).
			String blokirTenantOnly = TenantDataPlaneService.alasanBlokirTenantOnly(session, pendaftarId);
			if (blokirTenantOnly != null) {
				return blokirTenantOnly;
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

	/**
	 * READY → ACTIVE saat owner login pertama (05-workflow #7); best-effort dgn transaksi sendiri.
	 *
	 * <p>
	 * Dipanggil pada jalur login (bukan pada aksi dashboard biasa) untuk seluruh
	 * {@link TenantRegistry} milik {@code pendaftarId} yang masih berstatus READY: menandainya
	 * ACTIVE beserta {@code activatedAt}, ikut menaikkan status {@link PendaftaranTenant} terkait
	 * (yang berstatus READY) menjadi ACTIVE, dan mencatat satu baris {@link
	 * PendaftaranAuditEvent} ({@code EV_TENANT_ACTIVATED}) per tenant yang diaktifkan sebagai
	 * jejak "kapan tenant ini pertama kali benar-benar dipakai pemiliknya".
	 * </p>
	 * <p>
	 * <b>Best-effort</b>: dijalankan dalam transaksinya sendiri (terpisah dari transaksi
	 * login), dan bila terjadi galat, di-rollback lalu DITELAN (dicatat ke audit galat, tidak
	 * dilempar ulang) -- kegagalan menandai ACTIVE TIDAK BOLEH menggagalkan login pengguna itu
	 * sendiri; efeknya hanya tenant tetap READY dan akan dicoba lagi pada login berikutnya.
	 * </p>
	 *
	 * @param pendaftarId id Pendaftar yang baru login, pemilik tenant yang akan diperiksa.
	 */
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

	/**
	 * Daftar tenant milik/di-member-i pendaftar (tenant switcher) + status + trial + entitlement.
	 *
	 * <p>
	 * Sumber datanya adalah baris {@link TenantMembership} aktif milik {@code pendaftarId}
	 * (bukan {@code ownerPendaftar} pada {@link TenantRegistry} secara langsung) -- selaras
	 * dengan prinsip di {@link TenantMembershipResolver} bahwa kewenangan atas tenant ditentukan
	 * oleh baris keanggotaan, bukan sekadar relasi owner. Untuk setiap keanggotaan, method
	 * menyusun satu baris ringkas berisi identitas tenant, status siklus hidupnya, masa trial,
	 * dan status {@code isOwner} aktor pada tenant tersebut, ditambah entitlement modulnya yang
	 * dipecah menjadi dua daftar: {@code modulAktif} (status ACTIVE) dan
	 * {@code modulBelumTersedia} (status PLANNED) -- pemisahan ini disengaja agar UI klien dapat
	 * jujur menyembunyikan/menonaktifkan tombol modul yang belum operasional alih-alih
	 * menampilkannya sebagai aktif secara keliru.
	 * </p>
	 *
	 * @param pendaftarId id Pendaftar yang daftar tenant-nya ingin ditampilkan (mis. untuk
	 *                    pemilih/pengalih tenant pada dashboard).
	 * @param hasil       JSON keluaran yang DIISI DI TEMPAT (bukan dikembalikan): {@code status}
	 *                    ("00") dan {@code data} berisi array baris tenant seperti dijelaskan di
	 *                    atas.
	 * @throws Exception diteruskan dari kegagalan membangun {@link JSONObject}/{@link JSONArray}.
	 */
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
