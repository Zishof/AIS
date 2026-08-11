package ais.service.registration;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.security.PasswordHashService;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pendaftar;
import ais.database.model.tenant.JenisUsahaTenant;
import ais.database.model.tenant.JenisUsahaTenantModule;
import ais.database.model.tenant.PendaftarTenantProfile;
import ais.database.model.tenant.PendaftaranAuditEvent;
import ais.database.model.tenant.PendaftaranConsent;
import ais.database.model.tenant.PendaftaranEmailVerification;
import ais.database.model.tenant.PendaftaranTenant;
import ais.database.model.tenant.PendaftaranTenantJenisUsaha;
import ais.database.model.tenant.ProvisioningJob;
import ais.database.model.tenant.SchemaNameReservation;

/**
 * <h3>Service inti pendaftaran tenant -- SATU sumber aturan utk route {@code /pendaftaran}
 * DAN compatibility bridge {@code aksi=daftar} lama (§4.5 dokumen master).</h3>
 *
 * <p>Seluruh mutasi berjalan dalam SATU transaction Hibernate per §9.1: find/create Pendaftar →
 * profile → permohonan → pilihan jenis usaha (join = sumber kebenaran; {@code jenisBisnis} hanya
 * snapshot) → consent → reservasi username (INSERT unique = serialisasi race) → tantangan
 * verifikasi → audit event → commit. Gagal apa pun = rollback semua. Response JSON memakai kode
 * stabil ({@code code}) utk test/UI, bukan hanya pesan bebas.</p>
 */
public final class PendaftaranTenantService {

	private PendaftaranTenantService() {
	}

	// =====================================================================
	// KATALOG (GET form)
	// =====================================================================

	/** Katalog jenis usaha aktif + module bundle + paket + versi terms -- payload GET wizard. */
	public static JSONObject katalog() throws Exception {
		JSONObject hasil = new JSONObject();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray arr = new JSONArray();
			List<?> daftar = session.createCriteria(JenisUsahaTenant.class)
					.add(Restrictions.eq("aktif", Boolean.TRUE))
					.addOrder(Order.asc("displayOrder")).list();
			for (Object o : daftar) {
				JenisUsahaTenant j = (JenisUsahaTenant) o;
				JSONObject row = new JSONObject();
				row.put("id", j.getId());
				row.put("code", j.getCode());
				row.put("nama", j.getNama());
				row.put("deskripsi", j.getDeskripsi() == null ? "" : j.getDeskripsi());
				row.put("icon", j.getIcon() == null ? "" : j.getIcon());
				row.put("requiresManualReview", Boolean.TRUE.equals(j.getRequiresManualReview()));
				JSONArray modul = new JSONArray();
				List<?> moduls = session.createCriteria(JenisUsahaTenantModule.class)
						.add(Restrictions.eq("jenisUsahaTenant.id", j.getId()))
						.addOrder(Order.asc("displayOrder")).list();
				for (Object m : moduls) {
					modul.put(((JenisUsahaTenantModule) m).getModuleCode());
				}
				row.put("modules", modul);
				arr.put(row);
			}
			hasil.put("jenisUsaha", arr);
			hasil.put("paket", paketAktif());
			hasil.put("termsVersion", PendaftaranValidationService.versiTermsAktif());
			hasil.put("privacyVersion", PendaftaranValidationService.versiPrivacyAktif());
			hasil.put("trialHari", trialHari());
			hasil.put("subdomainBase", subdomainBase());
			hasil.put("passwordMin", PendaftaranValidationService.minimalPanjangPassword());
			hasil.put("status", "00");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/** Paket dari Konfigurasi `pendaftaran_paket_json` (database-driven, TIDAK di-hard-code di JSP/Java). */
	static JSONArray paketAktif() {
		String defaultJson = "[" +
				"{\"code\":\"POS\",\"nama\":\"Paket POS\",\"hargaBulanan\":250000,\"modul\":\"POS\"}," +
				"{\"code\":\"POS_KEUANGAN_GUDANG\",\"nama\":\"POS + Keuangan/Akuntansi + Gudang\",\"hargaBulanan\":400000,\"modul\":\"POS,KAS_JURNAL,GUDANG\"}," +
				"{\"code\":\"POS_KEUANGAN_GUDANG_SDM\",\"nama\":\"POS + Keuangan + Gudang + SDM\",\"hargaBulanan\":600000,\"modul\":\"POS,KAS_JURNAL,GUDANG,SDM\"}," +
				"{\"code\":\"LENGKAP\",\"nama\":\"Seluruh Modul\",\"hargaBulanan\":750000,\"modul\":\"SEMUA\"}]";
		try {
			return new JSONArray(Common.getKonfigurasi("pendaftaran_paket_json", defaultJson).getNilai());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftaranTenantService.paketAktif");
			try {
				return new JSONArray(defaultJson);
			} catch (Exception e2) {
				return new JSONArray();
			}
		}
	}

	static int trialHari() {
		try {
			return Integer.parseInt(Common.getKonfigurasi("pendaftaran_trial_hari", "30").getNilai().trim());
		} catch (Exception e) {
			return 30;
		}
	}

	public static String subdomainBase() {
		try {
			return Common.getKonfigurasi("pendaftaran_subdomain_base", "ebisnis.id").getNilai();
		} catch (Exception e) {
			return "ebisnis.id";
		}
	}

	// =====================================================================
	// CHECK USERNAME / EMAIL
	// =====================================================================

	public static JSONObject cekUsername(String desired) throws Exception {
		JSONObject hasil = new JSONObject();
		String normalized = PendaftaranValidationService.normalisasiUsername(desired);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String alasan = UsernameReservationService.alasanTidakTersedia(session, normalized);
			hasil.put("status", "00");
			hasil.put("username", normalized);
			hasil.put("tersedia", alasan == null);
			hasil.put("code", alasan == null ? "USERNAME_AVAILABLE" : alasan);
			hasil.put("preview", normalized + "." + subdomainBase());
			hasil.put("description", alasan == null ? "Username tersedia."
					: "USERNAME_INVALID".equals(alasan)
							? "Gunakan 3-31 karakter: huruf kecil di awal, lalu huruf/angka/garis bawah."
							: "Username tidak tersedia. Gunakan username lain.");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/** Jawaban SENGAJA tidak membocorkan tenant apa yang dimiliki email tsb (§13.5). */
	public static JSONObject cekEmail(String email) throws Exception {
		JSONObject hasil = new JSONObject();
		String normalized = PendaftaranValidationService.normalisasiEmail(email);
		if (!PendaftaranValidationService.emailValid(normalized)) {
			hasil.put("status", "00");
			hasil.put("code", "EMAIL_INVALID");
			hasil.put("terdaftar", false);
			hasil.put("description", "Format email tidak valid.");
			return hasil;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			boolean ada = adaAkunSelfService(session, normalized);
			hasil.put("status", "00");
			hasil.put("code", ada ? "EMAIL_ALREADY_REGISTERED" : "EMAIL_AVAILABLE");
			hasil.put("terdaftar", ada);
			hasil.put("description", ada
					? "Email telah memiliki akun. Silakan masuk untuk menambah tenant baru."
					: "Email dapat digunakan.");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	private static boolean adaAkunSelfService(Session session, String emailNormalized) {
		Object profile = session.createCriteria(PendaftarTenantProfile.class)
				.add(Restrictions.eq("normalizedEmail", emailNormalized)).setMaxResults(1).uniqueResult();
		if (profile != null) {
			return true;
		}
		// Akun self-service jalur LAMA (ebisnis.jsp) belum tentu punya profile.
		Object legacy = session.createCriteria(Pendaftar.class)
				.add(Restrictions.eq("email", emailNormalized))
				.add(Restrictions.isNotNull("passwordHash")).setMaxResults(1).uniqueResult();
		return legacy != null;
	}

	// =====================================================================
	// SUBMIT (§9.1 -- satu transaction)
	// =====================================================================

	/**
	 * @param p                payload ternormalisasi dari servlet (parameter form).
	 * @param pendaftarLoginId bila != null = flow "Buat Tenant Baru" pendaftar yang SUDAH login
	 *                         (tidak membuat Pendaftar kedua).
	 * @param baseUrl          basis URL absolut utk tautan verifikasi email.
	 */
	public static JSONObject submit(JSONObject p, Long pendaftarLoginId, String baseUrl) throws Exception {
		JSONObject hasil = new JSONObject();
		JSONObject fieldErrors = new JSONObject();

		// ---- Normalisasi + validasi tanpa-DB dulu (§14) ----
		String namaUsaha = PendaftaranValidationService.rapikan(p.optString("namaUsaha", ""), 255);
		String emailLogin = PendaftaranValidationService.normalisasiEmail(p.optString("emailLogin", ""));
		String password = p.optString("password", "");
		String konfirmasi = p.optString("konfirmasiPassword", "");
		String desiredUsername = PendaftaranValidationService.rapikan(p.optString("desiredUsername", ""), 64);
		String normalizedUsername = PendaftaranValidationService.normalisasiUsername(desiredUsername);
		String idempotencyKey = PendaftaranValidationService.rapikan(p.optString("idempotencyKey", ""), 100);
		Set<Long> idJenis = PendaftaranValidationService.dedupIdJenisUsaha(p.optString("jenisUsahaIds", ""));

		if (namaUsaha.isEmpty()) {
			fieldErrors.put("namaUsaha", "Nama usaha/instansi wajib diisi.");
		}
		if (!PendaftaranValidationService.emailValid(emailLogin)) {
			fieldErrors.put("emailLogin", "Alamat email yang valid wajib diisi.");
		}
		if (pendaftarLoginId == null) {
			String salahPassword = PendaftaranValidationService.cekPassword(password, konfirmasi);
			if (salahPassword != null) {
				fieldErrors.put("password", salahPassword);
			}
		}
		if (!PendaftaranValidationService.usernameValid(normalizedUsername)) {
			fieldErrors.put("desiredUsername",
					"Gunakan 3-31 karakter: huruf kecil di awal, lalu huruf/angka/garis bawah.");
		} else if (PendaftaranValidationService.usernameReserved(normalizedUsername)) {
			fieldErrors.put("desiredUsername", "Username ini tidak dapat digunakan. Gunakan username lain.");
		}
		if (idJenis.isEmpty()) {
			fieldErrors.put("jenisUsahaIds", "Pilih minimal satu jenis usaha/instansi.");
		}
		if (idempotencyKey.isEmpty()) {
			fieldErrors.put("idempotencyKey", "Form tidak lengkap. Muat ulang halaman pendaftaran.");
		}
		String termsVersion = p.optString("termsVersion", "");
		String privacyVersion = p.optString("privacyVersion", "");
		boolean setujuTerms = "1".equals(p.optString("setujuTerms", ""));
		boolean setujuPrivacy = "1".equals(p.optString("setujuPrivacy", ""));
		boolean setujuMarketing = "1".equals(p.optString("setujuMarketing", ""));
		if (!setujuTerms || !PendaftaranValidationService.versiTermsAktif().equals(termsVersion)) {
			fieldErrors.put("setujuTerms", "Setujui Syarat & Ketentuan versi terbaru (muat ulang bila baru berubah).");
		}
		if (!setujuPrivacy || !PendaftaranValidationService.versiPrivacyAktif().equals(privacyVersion)) {
			fieldErrors.put("setujuPrivacy", "Setujui Kebijakan Privasi versi terbaru.");
		}

		if (fieldErrors.length() > 0) {
			hasil.put("status", "91");
			hasil.put("code", "VALIDATION_FAILED");
			hasil.put("description", "Periksa kembali isian formulir.");
			hasil.put("fieldErrors", fieldErrors);
			hasil.put("retryable", true);
			return hasil;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		Long permohonanId = null;
		String tokenVerifikasiMentah = null;
		String registrationCode = null;
		try {
			session.beginTransaction();

			// ---- Idempotency: submit ulang dgn key sama = balikan permohonan yang sudah ada ----
			PendaftaranTenant existing = (PendaftaranTenant) session.createCriteria(PendaftaranTenant.class)
					.add(Restrictions.eq("idempotencyKey", idempotencyKey)).setMaxResults(1).uniqueResult();
			if (existing != null) {
				session.getTransaction().rollback();
				hasil.put("status", "00");
				hasil.put("code", "REGISTRATION_ALREADY_CREATED");
				hasil.put("description", "Pendaftaran sudah tercatat sebelumnya.");
				hasil.put("registrationCode", existing.getRegistrationCode());
				hasil.put("nextStep", nextStepDari(existing.getStatus()));
				hasil.put("redirect", "/pendaftaran?mode=status&kode=" + existing.getRegistrationCode());
				return hasil;
			}

			// ---- Cek ketersediaan username (final tetap constraint reservation) ----
			String alasanUsername = UsernameReservationService.alasanTidakTersedia(session, normalizedUsername);
			if (alasanUsername != null) {
				session.getTransaction().rollback();
				return errorField(hasil, "USERNAME_NOT_AVAILABLE", "Username tidak tersedia.",
						"desiredUsername", "Gunakan username lain.");
			}

			// ---- Muat + validasi jenis usaha (aktif; LAINNYA wajib keterangan) ----
			List<JenisUsahaTenant> jenisTerpilih = new ArrayList<JenisUsahaTenant>();
			for (Iterator<Long> it = idJenis.iterator(); it.hasNext();) {
				Long idJu = it.next();
				JenisUsahaTenant j = (JenisUsahaTenant) session.get(JenisUsahaTenant.class, idJu);
				if (j == null || !Boolean.TRUE.equals(j.getAktif())) {
					session.getTransaction().rollback();
					return errorField(hasil, "BUSINESS_TYPE_INVALID", "Jenis usaha tidak dikenal/nonaktif.",
							"jenisUsahaIds", "Muat ulang halaman lalu pilih kembali.");
				}
				jenisTerpilih.add(j);
			}
			String keteranganLainnya = PendaftaranValidationService.rapikan(p.optString("jenisUsahaLainnya", ""), 500);
			for (int i = 0; i < jenisTerpilih.size(); i++) {
				if ("LAINNYA".equals(jenisTerpilih.get(i).getCode()) && keteranganLainnya.isEmpty()) {
					session.getTransaction().rollback();
					return errorField(hasil, "BUSINESS_TYPE_OTHER_REQUIRED",
							"Jelaskan jenis usaha Anda pada kolom keterangan.",
							"jenisUsahaLainnya", "Wajib diisi untuk pilihan \"Jasa Lainnya\".");
				}
			}

			// ---- Find/create Pendaftar (tanpa duplikasi akun -- §3.1) ----
			Pendaftar pendaftar;
			boolean pendaftarBaru = false;
			if (pendaftarLoginId != null) {
				pendaftar = (Pendaftar) session.get(Pendaftar.class, pendaftarLoginId);
				if (pendaftar == null) {
					session.getTransaction().rollback();
					return error(hasil, "SESSION_INVALID", "Sesi Anda tidak valid. Silakan masuk kembali.", false);
				}
				// Tenant tambahan: tantangan verifikasi SELALU ke email akun yang login,
				// bukan isian form (mencegah pengalihan verifikasi ke email pihak lain).
				emailLogin = PendaftaranValidationService.normalisasiEmail(pendaftar.getEmail());
			} else {
				if (adaAkunSelfService(session, emailLogin)) {
					session.getTransaction().rollback();
					return errorField(hasil, "EMAIL_ALREADY_REGISTERED",
							"Email telah memiliki akun. Silakan masuk untuk menambah tenant baru.",
							"emailLogin", "Masuk (login) lalu gunakan menu Buat Tenant Baru.");
				}
				pendaftarBaru = true;
				pendaftar = buatPendaftar(p, namaUsaha, emailLogin, password, normalizedUsername, jenisTerpilih,
						keteranganLainnya);
				session.save(pendaftar);
				session.flush();

				PendaftarTenantProfile profile = buatProfile(p, pendaftar, emailLogin);
				session.save(profile);
			}

			// ---- Permohonan ----
			PendaftaranTenant permohonan = new PendaftaranTenant();
			permohonan.setPendaftar(pendaftar);
			permohonan.setDesiredUsername(desiredUsername);
			permohonan.setNormalizedUsername(normalizedUsername);
			permohonan.setRequestedDomain(PendaftaranValidationService.rapikan(p.optString("customDomain", ""), 255));
			permohonan.setStatus(PendaftaranTenant.STATUS_EMAIL_VERIFICATION_PENDING);
			permohonan.setCurrentStage("VERIFY_EMAIL");
			permohonan.setSelectedPlanVersion(
					PendaftaranValidationService.rapikan(p.optString("planCode", "TRIAL"), 64));
			permohonan.setTrialDaysSnapshot(Integer.valueOf(trialHari()));
			Date sekarang = new Date();
			permohonan.setSubmittedAt(sekarang);
			permohonan.setCreatedAt(sekarang);
			permohonan.setIdempotencyKey(idempotencyKey);
			permohonan.setRequestId(PendaftaranValidationService.rapikan(p.optString("requestId", ""), 64));
			permohonan.setSourceIpHash(hashIp(p.optString("sourceIp", "")));
			permohonan.setUserAgent(PendaftaranValidationService.rapikan(p.optString("userAgent", ""), 500));
			permohonan.setRegistrationSource(pendaftarLoginId != null ? "DASHBOARD" : "PUBLIC_FORM");
			permohonan.setOleh("pendaftaran");
			permohonan.setOlehId("pendaftaran");
			session.save(permohonan);
			session.flush();
			registrationCode = buatRegistrationCode(permohonan.getId(), sekarang);
			permohonan.setRegistrationCode(registrationCode);
			session.saveOrUpdate(permohonan);
			permohonanId = permohonan.getId();

			// ---- Pilihan jenis usaha (join = sumber kebenaran) ----
			for (int i = 0; i < jenisTerpilih.size(); i++) {
				JenisUsahaTenant j = jenisTerpilih.get(i);
				PendaftaranTenantJenisUsaha pilihan = new PendaftaranTenantJenisUsaha();
				pilihan.setPendaftaranTenant(permohonan);
				pilihan.setJenisUsahaTenant(j);
				pilihan.setPrimaryChoice(Boolean.valueOf(i == 0));
				if ("LAINNYA".equals(j.getCode())) {
					pilihan.setOtherDescription(keteranganLainnya);
				}
				pilihan.setCreatedAt(sekarang);
				pilihan.setOleh("pendaftaran");
				pilihan.setOlehId("pendaftaran");
				session.save(pilihan);
			}

			// ---- Consent (versi = versi terpublikasi, sudah divalidasi di atas) ----
			simpanConsent(session, permohonan, PendaftaranConsent.TYPE_TERMS, termsVersion, true, p, sekarang);
			simpanConsent(session, permohonan, PendaftaranConsent.TYPE_PRIVACY, privacyVersion, true, p, sekarang);
			if (setujuMarketing) {
				simpanConsent(session, permohonan, PendaftaranConsent.TYPE_MARKETING, "1", true, p, sekarang);
			}

			// ---- Reservasi username (INSERT unique = titik serialisasi race) ----
			UsernameReservationService.reservasi(session, normalizedUsername, permohonan, reservasiJam());

			// ---- Tantangan verifikasi email ----
			tokenVerifikasiMentah = EmailVerificationService.buatChallenge(session, permohonan, emailLogin);

			// ---- Audit events ----
			if (pendaftarBaru) {
				audit(session, PendaftaranAuditEvent.EV_PENDAFTAR_CREATED, permohonan, pendaftar.getId(), null, p);
			}
			audit(session, PendaftaranAuditEvent.EV_REGISTRATION_SUBMITTED, permohonan, pendaftar.getId(), null, p);
			audit(session, PendaftaranAuditEvent.EV_BUSINESS_TYPES_SELECTED, permohonan, pendaftar.getId(),
					kodeJenisCsv(jenisTerpilih), p);
			audit(session, PendaftaranAuditEvent.EV_CONSENT_ACCEPTED, permohonan, pendaftar.getId(),
					"terms=" + termsVersion + ";privacy=" + privacyVersion, p);

			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) PendaftaranTenantService.submit.rollback");
			}
			return terjemahkanException(hasil, e);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}

		// ---- Kirim email verifikasi DI LUAR transaction (best-effort) ----
		EmailVerificationService.kirimEmail(permohonanId, emailLogin, namaUsaha, registrationCode,
				tokenVerifikasiMentah, baseUrl);

		hasil.put("status", "00");
		hasil.put("code", "REGISTRATION_CREATED");
		hasil.put("description", "Pendaftaran berhasil disimpan. Periksa email Anda untuk verifikasi.");
		hasil.put("registrationCode", registrationCode);
		hasil.put("nextStep", "VERIFY_EMAIL");
		hasil.put("redirect", "/pendaftaran?mode=status&kode=" + registrationCode);
		return hasil;
	}

	private static Pendaftar buatPendaftar(JSONObject p, String namaUsaha, String emailLogin, String password,
			String normalizedUsername, List<JenisUsahaTenant> jenisTerpilih, String keteranganLainnya) {
		Pendaftar pendaftar = new Pendaftar();
		pendaftar.setNama(namaUsaha);
		pendaftar.setKode(normalizedUsername);
		pendaftar.setDomain(normalizedUsername);
		pendaftar.setEmail(emailLogin);
		pendaftar.setTelp(PendaftaranValidationService
				.normalisasiTelepon(p.optString("telpUsaha", "")));
		pendaftar.setAlamat(PendaftaranValidationService.rapikan(p.optString("alamat", ""), 500));
		pendaftar.setKontakperson(PendaftaranValidationService.rapikan(p.optString("picNama", ""), 255));
		pendaftar.setTelpkontakperson(
				PendaftaranValidationService.normalisasiTelepon(p.optString("picTelp", "")));
		pendaftar.setEmailkontakperson(
				PendaftaranValidationService.normalisasiEmail(p.optString("picEmail", "")));
		String negara = PendaftaranValidationService.rapikan(p.optString("negara", ""), 100);
		pendaftar.setNegara(negara.isEmpty() ? "Indonesia" : negara);
		pendaftar.setProvinsi(PendaftaranValidationService.rapikan(p.optString("provinsi", ""), 100));
		pendaftar.setKotaKabupaten(PendaftaranValidationService.rapikan(p.optString("kotaKabupaten", ""), 100));
		pendaftar.setKecamatan(PendaftaranValidationService.rapikan(p.optString("kecamatan", ""), 100));
		// Snapshot kompatibilitas SAJA -- sumber kebenaran = pendaftaran_tenant_jenis_usaha.
		pendaftar.setJenisBisnis(kodeJenisCsv(jenisTerpilih));
		String[] hashSalt = PasswordHashService.hash(password);
		pendaftar.setPasswordHash(hashSalt[0]);
		pendaftar.setPasswordSalt(hashSalt[1]);
		pendaftar.setDibuatPada(new Date());
		// WAJIB eksplisit: getter default null->true utk keduanya (audit §1). Aktif menyusul READY.
		pendaftar.setAktif(Boolean.FALSE);
		pendaftar.setMerupakanSekolah(Boolean.valueOf(
				!jenisTerpilih.isEmpty() && "SEKOLAH".equals(jenisTerpilih.get(0).getCode())));
		pendaftar.setKeterangan(keteranganLainnya.isEmpty() ? null : "Jenis usaha lainnya: " + keteranganLainnya);
		pendaftar.setOleh("pendaftaran");
		pendaftar.setOlehId("pendaftaran");
		return pendaftar;
	}

	private static PendaftarTenantProfile buatProfile(JSONObject p, Pendaftar pendaftar, String emailLogin) {
		PendaftarTenantProfile profile = new PendaftarTenantProfile();
		profile.setPendaftar(pendaftar);
		profile.setNormalizedEmail(emailLogin);
		profile.setOwnerDisplayName(PendaftaranValidationService.rapikan(p.optString("picNama", ""), 255));
		profile.setLegalName(PendaftaranValidationService.rapikan(p.optString("legalName", ""), 255));
		profile.setTradeName(PendaftaranValidationService.rapikan(p.optString("tradeName", ""), 255));
		profile.setLegalForm(PendaftaranValidationService.rapikan(p.optString("bentukUsaha", ""), 100));
		profile.setNib(PendaftaranValidationService.rapikan(p.optString("nib", ""), 50));
		profile.setNpwp(PendaftaranValidationService.rapikan(p.optString("npwp", ""), 50));
		profile.setWebsite(PendaftaranValidationService.rapikan(p.optString("website", ""), 255));
		profile.setPostalCode(PendaftaranValidationService.rapikan(p.optString("kodePos", ""), 20));
		profile.setTimezone(PendaftaranValidationService.rapikan(p.optString("timezone", "Asia/Jakarta"), 64));
		profile.setRegistrationSource("PUBLIC_FORM");
		profile.setAccountStatus(PendaftarTenantProfile.STATUS_PENDING_VERIFICATION);
		profile.setPasswordAlgorithm(PasswordHashService.ALGORITHM);
		profile.setPasswordVersion(Integer.valueOf(PasswordHashService.VERSION));
		profile.setPasswordIterations(Integer.valueOf(PasswordHashService.ITERASI));
		profile.setMustChangePassword(Boolean.FALSE);
		profile.setCreatedAt(new Date());
		profile.setOleh("pendaftaran");
		profile.setOlehId("pendaftaran");
		return profile;
	}

	private static void simpanConsent(Session session, PendaftaranTenant permohonan, String tipe, String versi,
			boolean accepted, JSONObject p, Date sekarang) {
		PendaftaranConsent c = new PendaftaranConsent();
		c.setPendaftaranTenant(permohonan);
		c.setConsentType(tipe);
		c.setDocumentVersion(versi);
		c.setAccepted(Boolean.valueOf(accepted));
		c.setAcceptedAt(sekarang);
		c.setSourceIp(PendaftaranValidationService.rapikan(p.optString("sourceIp", ""), 64));
		c.setUserAgent(PendaftaranValidationService.rapikan(p.optString("userAgent", ""), 500));
		c.setLocale(PendaftaranValidationService.rapikan(p.optString("locale", "id"), 20));
		c.setChannel("WEB");
		c.setOleh("pendaftaran");
		c.setOlehId("pendaftaran");
		session.save(c);
	}

	// =====================================================================
	// VERIFIKASI EMAIL / RESEND / STATUS / CANCEL
	// =====================================================================

	public static JSONObject verifikasiEmail(String token) throws Exception {
		JSONObject hasil = new JSONObject();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			PendaftaranEmailVerification v = EmailVerificationService.cariByToken(session, token);
			if (v == null) {
				session.getTransaction().commit();
				return error(hasil, "VERIFICATION_TOKEN_INVALID",
						"Tautan verifikasi tidak valid atau sudah kedaluwarsa. Kirim ulang verifikasi dari halaman status.",
						false);
			}
			PendaftaranTenant permohonan = v.getPendaftaranTenant();
			Date sekarang = new Date();
			v.setStatus(PendaftaranEmailVerification.STATUS_CONSUMED);
			v.setConsumedAt(sekarang);
			session.saveOrUpdate(v);

			if (PendaftaranTenant.STATUS_EMAIL_VERIFICATION_PENDING.equals(permohonan.getStatus())
					|| PendaftaranTenant.STATUS_SUBMITTED.equals(permohonan.getStatus())) {
				permohonan.setVerifiedAt(sekarang);
				boolean perluReview = perluManualReview(session, permohonan);
				if (perluReview) {
					permohonan.setStatus(PendaftaranTenant.STATUS_REVIEW_PENDING);
					permohonan.setCurrentStage("MANUAL_REVIEW");
				} else {
					permohonan.setStatus(PendaftaranTenant.STATUS_PROVISIONING_QUEUED);
					permohonan.setCurrentStage("PROVISIONING");
					buatJobProvisioning(session, permohonan, sekarang);
				}
				session.saveOrUpdate(permohonan);

				PendaftarTenantProfile profile = (PendaftarTenantProfile) session
						.createCriteria(PendaftarTenantProfile.class)
						.add(Restrictions.eq("pendaftar.id", permohonan.getPendaftar().getId()))
						.setMaxResults(1).uniqueResult();
				if (profile != null) {
					profile.setEmailVerifiedAt(sekarang);
					profile.setAccountStatus(PendaftarTenantProfile.STATUS_ACTIVE);
					session.saveOrUpdate(profile);
				}
				audit(session, PendaftaranAuditEvent.EV_EMAIL_VERIFIED, permohonan,
						permohonan.getPendaftar().getId(), null, new JSONObject());
				if (!perluReview) {
					audit(session, PendaftaranAuditEvent.EV_TENANT_PROVISIONING_QUEUED, permohonan,
							permohonan.getPendaftar().getId(), null, new JSONObject());
				}
			}
			String kode = permohonan.getRegistrationCode();
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("code", "EMAIL_VERIFIED");
			hasil.put("description", "Email berhasil diverifikasi.");
			hasil.put("registrationCode", kode);
			hasil.put("redirect", "/pendaftaran?mode=status&kode=" + kode);
			return hasil;
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) PendaftaranTenantService.verifikasiEmail.rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Verifikasi TANPA token -- HANYA utk verifikasi manual admin (backoffice; token mentah tidak
	 * pernah bisa direkonstruksi dari hash). Transisi status memakai jalur yang SAMA dgn
	 * verifikasi token; challenge PENDING ditandai SUPERSEDED supaya tautan lama mati.
	 */
	public static JSONObject verifikasiTanpaToken(Long permohonanId) throws Exception {
		JSONObject hasil = new JSONObject();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			PendaftaranTenant permohonan = (PendaftaranTenant) session.get(PendaftaranTenant.class, permohonanId);
			if (permohonan == null) {
				session.getTransaction().rollback();
				return error(hasil, "REGISTRATION_NOT_FOUND", "Permohonan tidak ditemukan.", false);
			}
			if (!PendaftaranTenant.STATUS_EMAIL_VERIFICATION_PENDING.equals(permohonan.getStatus())
					&& !PendaftaranTenant.STATUS_SUBMITTED.equals(permohonan.getStatus())) {
				session.getTransaction().rollback();
				return error(hasil, "STATE_INVALID", "Permohonan tidak sedang menunggu verifikasi email.", false);
			}
			List<?> pending = session.createCriteria(PendaftaranEmailVerification.class)
					.add(Restrictions.eq("pendaftaranTenant.id", permohonan.getId()))
					.add(Restrictions.eq("status", PendaftaranEmailVerification.STATUS_PENDING)).list();
			for (Object o : pending) {
				((PendaftaranEmailVerification) o).setStatus(PendaftaranEmailVerification.STATUS_SUPERSEDED);
				session.saveOrUpdate((PendaftaranEmailVerification) o);
			}
			Date sekarang = new Date();
			permohonan.setVerifiedAt(sekarang);
			boolean perluReview = perluManualReview(session, permohonan);
			if (perluReview) {
				permohonan.setStatus(PendaftaranTenant.STATUS_REVIEW_PENDING);
				permohonan.setCurrentStage("MANUAL_REVIEW");
			} else {
				permohonan.setStatus(PendaftaranTenant.STATUS_PROVISIONING_QUEUED);
				permohonan.setCurrentStage("PROVISIONING");
				buatJobProvisioning(session, permohonan, sekarang);
			}
			session.saveOrUpdate(permohonan);
			PendaftarTenantProfile profile = (PendaftarTenantProfile) session
					.createCriteria(PendaftarTenantProfile.class)
					.add(Restrictions.eq("pendaftar.id", permohonan.getPendaftar().getId()))
					.setMaxResults(1).uniqueResult();
			if (profile != null) {
				profile.setEmailVerifiedAt(sekarang);
				profile.setAccountStatus(PendaftarTenantProfile.STATUS_ACTIVE);
				session.saveOrUpdate(profile);
			}
			audit(session, PendaftaranAuditEvent.EV_EMAIL_VERIFIED, permohonan,
					permohonan.getPendaftar().getId(), "manual-admin", new JSONObject());
			String kode = permohonan.getRegistrationCode();
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("code", "EMAIL_VERIFIED");
			hasil.put("description", "Verifikasi manual berhasil; permohonan lanjut ke tahap berikutnya.");
			hasil.put("registrationCode", kode);
			return hasil;
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) PendaftaranTenantService.verifikasiTanpaToken.rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static boolean perluManualReview(Session session, PendaftaranTenant permohonan) {
		if (Common.bolehKonfigurasi("pendaftaran_wajib_review_manual",
				ais.database.model.Konfigurasi.TIDAK_AKTIF)) {
			return true;
		}
		List<?> pilihan = session.createCriteria(PendaftaranTenantJenisUsaha.class)
				.add(Restrictions.eq("pendaftaranTenant.id", permohonan.getId())).list();
		for (Object o : pilihan) {
			JenisUsahaTenant j = ((PendaftaranTenantJenisUsaha) o).getJenisUsahaTenant();
			if (j != null && Boolean.TRUE.equals(j.getRequiresManualReview())) {
				return true;
			}
		}
		return false;
	}

	private static void buatJobProvisioning(Session session, PendaftaranTenant permohonan, Date sekarang) {
		ProvisioningJob adaJob = (ProvisioningJob) session.createCriteria(ProvisioningJob.class)
				.add(Restrictions.eq("pendaftaranTenant.id", permohonan.getId())).setMaxResults(1).uniqueResult();
		if (adaJob != null) {
			return; // idempoten -- verifikasi ulang tidak membuat job kedua
		}
		ProvisioningJob job = new ProvisioningJob();
		job.setPendaftaranTenant(permohonan);
		job.setStatus(ProvisioningJob.STATUS_QUEUED);
		job.setAttempt(Integer.valueOf(0));
		job.setRetryAt(sekarang);
		job.setCreatedAt(sekarang);
		job.setOleh("pendaftaran");
		job.setOlehId("pendaftaran");
		session.save(job);
	}

	public static JSONObject resendVerifikasi(String registrationCode, String baseUrl) throws Exception {
		JSONObject hasil = new JSONObject();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Long permohonanId = null;
		String email = null;
		String namaUsaha = null;
		String token = null;
		try {
			session.beginTransaction();
			PendaftaranTenant permohonan = cariByKode(session, registrationCode);
			if (permohonan == null) {
				session.getTransaction().rollback();
				return error(hasil, "REGISTRATION_NOT_FOUND", "Nomor pendaftaran tidak ditemukan.", false);
			}
			if (!PendaftaranTenant.STATUS_EMAIL_VERIFICATION_PENDING.equals(permohonan.getStatus())) {
				session.getTransaction().rollback();
				return error(hasil, "VERIFICATION_NOT_PENDING",
						"Permohonan ini tidak sedang menunggu verifikasi email.", false);
			}
			email = permohonan.getPendaftar().getEmail();
			namaUsaha = permohonan.getPendaftar().getNama();
			permohonanId = permohonan.getId();
			token = EmailVerificationService.buatChallenge(session, permohonan,
					PendaftaranValidationService.normalisasiEmail(email));
			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) PendaftaranTenantService.resendVerifikasi.rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		EmailVerificationService.kirimEmail(permohonanId, email, namaUsaha, registrationCode, token, baseUrl);
		hasil.put("status", "00");
		hasil.put("code", "VERIFICATION_RESENT");
		hasil.put("description", "Email verifikasi dikirim ulang. Periksa kotak masuk/spam Anda.");
		return hasil;
	}

	/** Status publik ter-mask (§5.2): tanpa stack trace/schema/detail internal. */
	public static JSONObject status(String registrationCode) throws Exception {
		JSONObject hasil = new JSONObject();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PendaftaranTenant permohonan = cariByKode(session, registrationCode);
			if (permohonan == null) {
				return error(hasil, "REGISTRATION_NOT_FOUND", "Nomor pendaftaran tidak ditemukan.", false);
			}
			hasil.put("status", "00");
			hasil.put("registrationCode", permohonan.getRegistrationCode());
			hasil.put("namaUsaha", permohonan.getPendaftar().getNama());
			hasil.put("username", permohonan.getNormalizedUsername());
			hasil.put("previewDomain", permohonan.getNormalizedUsername() + "." + subdomainBase());
			hasil.put("statusPermohonan", permohonan.getStatus());
			hasil.put("tahap", permohonan.getCurrentStage() == null ? "" : permohonan.getCurrentStage());
			hasil.put("emailMasked", maskEmail(permohonan.getPendaftar().getEmail()));
			hasil.put("nextStep", nextStepDari(permohonan.getStatus()));
			if (permohonan.getFailureMessageSafe() != null) {
				hasil.put("failureMessage", permohonan.getFailureMessageSafe());
			}
			JSONArray jenis = new JSONArray();
			List<?> pilihan = session.createCriteria(PendaftaranTenantJenisUsaha.class)
					.add(Restrictions.eq("pendaftaranTenant.id", permohonan.getId())).list();
			for (Object o : pilihan) {
				JenisUsahaTenant j = ((PendaftaranTenantJenisUsaha) o).getJenisUsahaTenant();
				if (j != null) {
					jenis.put(j.getNama());
				}
			}
			hasil.put("jenisUsaha", jenis);

			ProvisioningJob job = (ProvisioningJob) session.createCriteria(ProvisioningJob.class)
					.add(Restrictions.eq("pendaftaranTenant.id", permohonan.getId()))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			if (job != null) {
				hasil.put("provisioningStatus", job.getStatus());
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/** Batalkan permohonan yang belum berjalan jauh; kepemilikan dibuktikan servlet (sesi form). */
	public static JSONObject cancel(String registrationCode) throws Exception {
		JSONObject hasil = new JSONObject();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			PendaftaranTenant permohonan = cariByKode(session, registrationCode);
			if (permohonan == null) {
				session.getTransaction().rollback();
				return error(hasil, "REGISTRATION_NOT_FOUND", "Nomor pendaftaran tidak ditemukan.", false);
			}
			String status = permohonan.getStatus();
			if (!PendaftaranTenant.STATUS_DRAFT.equals(status)
					&& !PendaftaranTenant.STATUS_SUBMITTED.equals(status)
					&& !PendaftaranTenant.STATUS_EMAIL_VERIFICATION_PENDING.equals(status)) {
				session.getTransaction().rollback();
				return error(hasil, "CANCEL_NOT_ALLOWED",
						"Permohonan pada tahap ini tidak dapat dibatalkan mandiri. Hubungi dukungan.", false);
			}
			permohonan.setStatus(PendaftaranTenant.STATUS_CANCELLED);
			permohonan.setCurrentStage("CANCELLED");
			session.saveOrUpdate(permohonan);
			SchemaNameReservation r = (SchemaNameReservation) session
					.createCriteria(SchemaNameReservation.class)
					.add(Restrictions.eq("pendaftaranTenant.id", permohonan.getId()))
					.add(Restrictions.eq("status", SchemaNameReservation.STATUS_RESERVED))
					.setMaxResults(1).uniqueResult();
			if (r != null) {
				r.setStatus(SchemaNameReservation.STATUS_RELEASED);
				r.setReleasedAt(new Date());
				session.saveOrUpdate(r);
			}
			audit(session, PendaftaranAuditEvent.EV_REGISTRATION_CANCELLED, permohonan,
					permohonan.getPendaftar().getId(), null, new JSONObject());
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("code", "REGISTRATION_CANCELLED");
			hasil.put("description", "Pendaftaran dibatalkan.");
			return hasil;
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) PendaftaranTenantService.cancel.rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =====================================================================
	// UTIL INTERNAL
	// =====================================================================

	private static PendaftaranTenant cariByKode(Session session, String registrationCode) {
		if (registrationCode == null || registrationCode.trim().isEmpty() || registrationCode.length() > 40) {
			return null;
		}
		return (PendaftaranTenant) session.createCriteria(PendaftaranTenant.class)
				.add(Restrictions.eq("registrationCode", registrationCode.trim()))
				.setMaxResults(1).uniqueResult();
	}

	private static String buatRegistrationCode(Long id, Date sekarang) {
		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.setTime(sekarang);
		String idPadded = String.valueOf(id.longValue());
		while (idPadded.length() < 6) {
			idPadded = "0" + idPadded;
		}
		return "REG-" + cal.get(java.util.Calendar.YEAR) + "-" + idPadded;
	}

	private static String kodeJenisCsv(List<JenisUsahaTenant> jenis) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < jenis.size(); i++) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(jenis.get(i).getCode());
		}
		// Kolom snapshot jenis_bisnis (teks) -- daftar kode terkontrol, primary di depan.
		return sb.length() > 255 ? sb.substring(0, 255) : sb.toString();
	}

	static String hashIp(String ip) {
		return ip == null || ip.trim().isEmpty() ? null : PasswordHashService.sha256Hex(ip.trim());
	}

	static String maskEmail(String email) {
		String e = email == null ? "" : email.trim();
		int at = e.indexOf('@');
		if (at <= 1) {
			return "***";
		}
		String depan = e.substring(0, at);
		String belakang = e.substring(at);
		if (depan.length() <= 3) {
			return depan.charAt(0) + "**" + belakang;
		}
		return depan.substring(0, 2) + "***" + depan.substring(depan.length() - 1) + belakang;
	}

	private static String nextStepDari(String status) {
		if (PendaftaranTenant.STATUS_EMAIL_VERIFICATION_PENDING.equals(status)) {
			return "VERIFY_EMAIL";
		}
		if (PendaftaranTenant.STATUS_VERIFIED.equals(status)
				|| PendaftaranTenant.STATUS_PROVISIONING_QUEUED.equals(status)
				|| PendaftaranTenant.STATUS_PROVISIONING.equals(status)) {
			return "WAIT_PROVISIONING";
		}
		if (PendaftaranTenant.STATUS_REVIEW_PENDING.equals(status)) {
			return "WAIT_REVIEW";
		}
		if (PendaftaranTenant.STATUS_READY.equals(status) || PendaftaranTenant.STATUS_ACTIVE.equals(status)) {
			return "LOGIN";
		}
		return "CONTACT_SUPPORT";
	}

	private static int reservasiJam() {
		try {
			return Integer.parseInt(Common.getKonfigurasi("pendaftaran_reservasi_jam", "72").getNilai().trim());
		} catch (Exception e) {
			return 72;
		}
	}

	private static void audit(Session session, String eventCode, PendaftaranTenant permohonan, Long pendaftarId,
			String detail, JSONObject p) {
		try {
			PendaftaranAuditEvent ev = new PendaftaranAuditEvent();
			ev.setEventCode(eventCode);
			ev.setActorType(PendaftaranAuditEvent.ACTOR_PUBLIC);
			ev.setPendaftarId(pendaftarId);
			ev.setRegistrationId(permohonan == null ? null : permohonan.getId());
			ev.setRequestId(p == null ? null : p.optString("requestId", null));
			ev.setSourceIpHash(p == null ? null : hashIp(p.optString("sourceIp", "")));
			ev.setUserAgent(p == null ? null
					: PendaftaranValidationService.rapikan(p.optString("userAgent", ""), 500));
			if (detail != null) {
				ev.setDetailJson(new JSONObject().put("detail", detail).toString());
			}
			ev.setResult("OK");
			ev.setWaktu(new Date());
			ev.setOleh("pendaftaran");
			ev.setOlehId("pendaftaran");
			session.save(ev);
		} catch (Exception e) {
			// Audit tidak boleh menggagalkan alur; exception dicatat ke error_log saja.
			ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftaranTenantService.audit:" + eventCode);
		}
	}

	private static JSONObject error(JSONObject hasil, String code, String description, boolean retryable)
			throws Exception {
		hasil.put("status", "91");
		hasil.put("code", code);
		hasil.put("description", description);
		hasil.put("retryable", retryable);
		return hasil;
	}

	private static JSONObject errorField(JSONObject hasil, String code, String description, String field,
			String pesanField) throws Exception {
		error(hasil, code, description, false);
		hasil.put("fieldErrors", new JSONObject().put(field, pesanField));
		return hasil;
	}

	/** Terjemahkan unique-violation race jadi kode stabil; selain itu pesan generik aman. */
	private static JSONObject terjemahkanException(JSONObject hasil, Exception e) throws Exception {
		String pesan = String.valueOf(e) + " " + (e.getCause() == null ? "" : String.valueOf(e.getCause()));
		String pesanLower = pesan.toLowerCase();
		if (pesanLower.contains("schema_name_reservation") || pesanLower.contains("normalized_name")
				|| pesanLower.contains("domain")) {
			return errorField(hasil, "USERNAME_NOT_AVAILABLE", "Username tidak tersedia.",
					"desiredUsername", "Gunakan username lain.");
		}
		if (pesanLower.contains("normalized_email") || pesanLower.contains("pendaftar_tenant_profile")) {
			return errorField(hasil, "EMAIL_ALREADY_REGISTERED",
					"Email telah memiliki akun. Silakan masuk untuk menambah tenant baru.",
					"emailLogin", "Masuk (login) lalu gunakan menu Buat Tenant Baru.");
		}
		if (pesanLower.contains("idempotency")) {
			return error(hasil, "DUPLICATE_REQUEST", "Permintaan sedang/sudah diproses. Cek halaman status.", false);
		}
		ais.common.ErrorAuditUtil.record(e, "auto-audit PendaftaranTenantService.submit");
		return error(hasil, "INTERNAL_ERROR", "Terjadi kesalahan pada sistem. Silakan coba lagi.", true);
	}
}
