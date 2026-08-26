package ais.action.servlet.api;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.servlet.api.biometric.BiometricCrypto;
import ais.action.servlet.api.biometric.BiometricMatchResult;
import ais.action.servlet.api.biometric.BiometricMatcherRegistry;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.biometric.BiometricCredential;
import ais.database.model.biometric.BiometricEvent;

/** API vendor-neutral untuk enrollment, verifikasi, dan absensi biometrik. */
public final class BiometricApi {
	private static final String FINGERPRINT = "FINGERPRINT";
	private static final String FACE = "FACE";

	private BiometricApi() { }

	public static JSONObject capability(HttpServletRequest req, JSONObject request) throws Exception {
		Tbmuser actor = ApiUtil.currentUser(request, req);
		if (actor == null || actor.getUserId() == null) return ApiHelperSupport.status("91", "Sesi login tidak valid");
		JSONObject out = ApiHelperSupport.status("00", "Kemampuan biometrik berhasil dimuat");
		out.put("boleh_melihat", bolehBiometrik(actor));
		out.put("boleh_enroll_sendiri", bolehBiometrik(actor));
		out.put("boleh_enroll_pengguna_lain", Common.getApakahAdminLain(actor));
		out.put("boleh_absen", bolehBiometrik(actor));
		out.put("server_encryption_ready", BiometricCrypto.configured());
		out.put("fingerprint_matcher_ready", BiometricMatcherRegistry.available(FINGERPRINT, "ISO_19794_2"));
		out.put("face_matcher_ready", BiometricMatcherRegistry.available(FACE, BiometricMatcherRegistry.FACE_FORMAT));
		out.put("face_format", BiometricMatcherRegistry.FACE_FORMAT);
		out.put("privacy", "Template terenkripsi; foto mentah sidik jari/wajah tidak disimpan");
		return out;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject list(HttpServletRequest req, JSONObject request) {
		Tbmuser actor = ApiUtil.currentUser(request, req);
		if (!bolehBiometrik(actor)) return ApiHelperSupport.status("93", "Tidak memiliki hak akses data absensi");
		String subject = targetUser(actor, request);
		if (subject == null) return ApiHelperSupport.status("93", "Tidak boleh melihat biometrik pengguna lain");
		Session session = HibernateUtil.openSession();
		try {
			List<BiometricCredential> rows = session.createCriteria(BiometricCredential.class)
					.add(Restrictions.eq("subjectUserId", subject)).addOrder(Order.desc("updatedAt")).list();
			JSONArray data = new JSONArray();
			for (BiometricCredential c : rows) {
				JSONObject row = new JSONObject();
				row.put("id", c.getId()); row.put("modality", c.getModality());
				row.put("position", nullToEmpty(c.getPositionCode())); row.put("format", c.getTemplateFormat());
				row.put("provider", nullToEmpty(c.getProvider())); row.put("quality", c.getQualityScore());
				row.put("active", c.getActive()); row.put("updated_at", c.getUpdatedAt() == null ? 0L : c.getUpdatedAt().getTime());
				row.put("template_revision", c.getTemplateHash() == null ? "" : c.getTemplateHash().substring(0, 12));
				data.put(row);
			}
			JSONObject out = ApiHelperSupport.status("00", "Daftar biometrik berhasil dimuat"); out.put("data", data); return out;
		} catch (Exception e) { Common.tampilErrorJikaAdmin(e); return ApiHelperSupport.errorResponse("Gagal memuat daftar biometrik"); }
		finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static JSONObject enroll(HttpServletRequest req, JSONObject request) {
		Tbmuser actor = ApiUtil.currentUser(request, req);
		if (!bolehBiometrik(actor)) return ApiHelperSupport.status("93", "Tidak memiliki hak akses data absensi");
		String subject = targetUser(actor, request);
		if (subject == null) return ApiHelperSupport.status("93", "Hanya admin yang boleh mendaftarkan biometrik pengguna lain");
		if (!request.optBoolean("consent", false)) return ApiHelperSupport.status("92", "Persetujuan pemilik/wali wajib dicatat");
		if (!BiometricCrypto.configured()) return ApiHelperSupport.status("95", "Kunci enkripsi biometrik server belum dikonfigurasi");
		String modality = modality(request.optString("modality"));
		String format = clean(request.optString("template_format"), 80);
		if (modality == null || format == null) return ApiHelperSupport.status("92", "Modality dan format template wajib diisi");
		Session session = HibernateUtil.openSession(); Transaction tx = null;
		try {
			byte[] clear = BiometricCrypto.decodeTemplate(request.optString("template_base64"));
			String hash = BiometricCrypto.sha256(clear);
			BiometricCredential existing = (BiometricCredential) session.createCriteria(BiometricCredential.class)
					.add(Restrictions.eq("subjectUserId", subject)).add(Restrictions.eq("modality", modality))
					.add(Restrictions.eq("templateHash", hash)).setMaxResults(1).uniqueResult();
			if (existing != null && Boolean.TRUE.equals(existing.getActive())) {
				JSONObject out = ApiHelperSupport.status("00", "Template biometrik sudah terdaftar"); out.put("id", existing.getId()); out.put("duplicate", true); return out;
			}
			tx = session.beginTransaction();
			BiometricCredential c = existing == null ? new BiometricCredential() : existing;
			c.setSubjectUserId(subject); c.setModality(modality); c.setPositionCode(clean(request.optString("position"), 40));
			c.setTemplateFormat(format); c.setTemplateHash(hash); c.setKeyVersion(BiometricCrypto.keyVersion());
			c.setTemplateCiphertext(BiometricCrypto.encrypt(clear, aad(subject, modality, format)));
			c.setProvider(clean(request.optString("provider"), 120)); c.setQualityScore(integer(request, "quality"));
			c.setActive(Boolean.TRUE); c.setConsentAt(new Date()); c.setConsentBy(actor.getUserId()); c.setUpdatedAt(new Date());
			if (existing == null) session.save(c); else session.update(c);
			saveEvent(session, actor.getUserId(), subject, c.getId(), modality, "ENROLL", mutation(request), false, null,
					doubleValue(request, "liveness_score"), "ENROLLED", request, null);
			tx.commit();
			JSONObject out = ApiHelperSupport.status("00", "Biometrik tersimpan terenkripsi"); out.put("id", c.getId());
			out.put("matcher_ready", BiometricMatcherRegistry.available(modality, format));
			if (!BiometricMatcherRegistry.available(modality, format)) out.put("warning", "SDK matcher vendor belum aktif; enrollment tersimpan tetapi verifikasi belum dapat digunakan");
			return out;
		} catch (IllegalArgumentException e) { rollback(tx); return ApiHelperSupport.status("92", e.getMessage()); }
		catch (Exception e) { rollback(tx); Common.tampilErrorJikaAdmin(e); return ApiHelperSupport.errorResponse("Gagal menyimpan biometrik"); }
		finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static JSONObject revoke(HttpServletRequest req, JSONObject request) {
		Tbmuser actor = ApiUtil.currentUser(request, req);
		if (!bolehBiometrik(actor)) return ApiHelperSupport.status("93", "Tidak memiliki hak akses data absensi");
		Long id = longValue(request, "credential_id"); if (id == null) return ApiHelperSupport.status("92", "credential_id wajib diisi");
		Session session = HibernateUtil.openSession(); Transaction tx = null;
		try {
			BiometricCredential c = (BiometricCredential) session.get(BiometricCredential.class, id);
			if (c == null) return ApiHelperSupport.status("94", "Biometrik tidak ditemukan");
			if (!actor.getUserId().equals(c.getSubjectUserId()) && !Common.getApakahAdminLain(actor)) return ApiHelperSupport.status("93", "Tidak boleh menonaktifkan biometrik pengguna lain");
			// Replay aman: respons jaringan bisa hilang sesudah transaksi pertama
			// berhasil. Credential yang sudah nonaktif dianggap selesai, bukan error.
			if (!Boolean.TRUE.equals(c.getActive())) return ApiHelperSupport.status("00", "Biometrik sudah dinonaktifkan");
			tx = session.beginTransaction(); c.setActive(Boolean.FALSE); c.setUpdatedAt(new Date()); session.update(c);
			saveEvent(session, actor.getUserId(), c.getSubjectUserId(), c.getId(), c.getModality(), "REVOKE", mutation(request), false, null, null, "REVOKED", request, null);
			tx.commit(); return ApiHelperSupport.status("00", "Biometrik dinonaktifkan");
		} catch (Exception e) { rollback(tx); Common.tampilErrorJikaAdmin(e); return ApiHelperSupport.errorResponse("Gagal menonaktifkan biometrik"); }
		finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static JSONObject verify(HttpServletRequest req, JSONObject request) {
		Tbmuser actor = ApiUtil.currentUser(request, req);
		if (!bolehBiometrik(actor)) return ApiHelperSupport.status("93", "Tidak memiliki hak akses data absensi");
		Verification v = verifyInternal(actor, request, "VERIFY", null);
		return v.response;
	}

	public static JSONObject attendance(HttpServletRequest req, JSONObject request) throws Exception {
		Tbmuser actor = ApiUtil.currentUser(request, req);
		if (!bolehBiometrik(actor)) return ApiHelperSupport.status("93", "Tidak memiliki hak akses data absensi");
		Long capturedAt = longValue(request, "captured_at_epoch");
		long now = System.currentTimeMillis();
		if (capturedAt == null || capturedAt.longValue() > now + 300000L
				|| now - capturedAt.longValue() > maximumOfflineAgeMillis())
			return ApiHelperSupport.status("97", "Waktu pengambilan biometrik tidak valid atau sudah kedaluwarsa");
		String mutation = mutation(request);
		JSONObject replay = replay(actor.getUserId(), "ATTENDANCE", mutation);
		if (replay != null) { replay.put("idempoten_replay", true); return replay; }
		JSONObject verificationRequest = new JSONObject(request.toString());
		verificationRequest.put("clientMutationId", mutation + "-verify-" + UUID.randomUUID().toString());
		Verification v = verifyInternal(actor, verificationRequest, "ATTENDANCE_VERIFY", null);
		if (!v.matched) return v.response;
		JSONObject attendance = AbsensiApiAction.absen(req, request);
		attendance.put("biometric_verified", true); attendance.put("biometric_modality", v.modality);
		updateEventResponse(v.eventId, attendance, mutation);
		return attendance;
	}

	@SuppressWarnings("unchecked")
	/** Dipakai PosApi setelah izin kasir dan relasi member diperiksa di sana. */
	public static JSONObject verifyLinkedSubject(Tbmuser actor, String subjectUserId, JSONObject request, String purpose) {
		if (actor == null || actor.getUserId() == null || subjectUserId == null || subjectUserId.trim().length() == 0)
			return ApiHelperSupport.status("92", "Aktor dan pengguna member wajib tersedia");
		return verifyInternal(actor, request, purpose, subjectUserId.trim()).response;
	}

	@SuppressWarnings("unchecked")
	private static Verification verifyInternal(Tbmuser actor, JSONObject request, String purpose, String forcedSubject) {
		String subject = forcedSubject == null ? targetUser(actor, request) : forcedSubject;
		if (subject == null) return Verification.error(ApiHelperSupport.status("93", "Tidak boleh memverifikasi pengguna lain"));
		String modality = modality(request.optString("modality")); String format = clean(request.optString("template_format"), 80);
		if (modality == null || format == null) return Verification.error(ApiHelperSupport.status("92", "Modality dan format template wajib diisi"));
		Double live = doubleValue(request, "liveness_score");
		if (FACE.equals(modality) && (live == null || live.doubleValue() < livenessThreshold()))
			return Verification.error(ApiHelperSupport.status("96", "Liveness wajah tidak memenuhi batas minimum"));
		if (!BiometricMatcherRegistry.available(modality, format))
			return Verification.error(ApiHelperSupport.status("95", "SDK matcher biometrik belum dikonfigurasi untuk format ini"));
		Session session = HibernateUtil.openSession(); Transaction tx = null;
		try {
			byte[] probe = BiometricCrypto.decodeTemplate(request.optString("probe_base64"));
			List<BiometricCredential> rows = session.createCriteria(BiometricCredential.class)
					.add(Restrictions.eq("subjectUserId", subject)).add(Restrictions.eq("modality", modality))
					.add(Restrictions.eq("templateFormat", format)).add(Restrictions.eq("active", Boolean.TRUE)).list();
			BiometricCredential best = null; BiometricMatchResult bestResult = null;
			for (BiometricCredential c : rows) {
				byte[] reference = BiometricCrypto.decrypt(c.getTemplateCiphertext(), aad(subject, modality, format));
				BiometricMatchResult result = BiometricMatcherRegistry.match(modality, format, probe, reference);
				if (bestResult == null || result.getScore() > bestResult.getScore()) { best = c; bestResult = result; }
			}
			boolean matched = bestResult != null && bestResult.isMatched();
			tx = session.beginTransaction();
			BiometricEvent event = saveEvent(session, actor.getUserId(), subject, best == null ? null : best.getId(), modality,
					purpose, mutation(request), matched, bestResult == null ? null : Double.valueOf(bestResult.getScore()), live,
					matched ? "MATCHED" : "NO_MATCH", request, null);
			tx.commit();
			JSONObject out = ApiHelperSupport.status(matched ? "00" : "96", matched ? "Identitas biometrik terverifikasi" : "Biometrik tidak cocok");
			out.put("matched", matched); out.put("score", bestResult == null ? JSONObject.NULL : Double.valueOf(bestResult.getScore()));
			out.put("provider", BiometricMatcherRegistry.providerName(modality, format)); out.put("event_id", event.getId());
			return new Verification(out, matched, modality, event.getId());
		} catch (Exception e) { rollback(tx); Common.tampilErrorJikaAdmin(e); return Verification.error(ApiHelperSupport.errorResponse("Verifikasi biometrik gagal")); }
		finally { HibernateUtil.closeSessionQuietly(session); }
	}

	private static boolean bolehBiometrik(Tbmuser user) {
		if (user == null || user.getUserId() == null) return false;
		if (Common.getApakahAdminLain(user)) return true;
		try { for (Tbmrole role : user.ambilRoles()) if (role != null && Boolean.TRUE.equals(role.getPresensiKehadiran())) return true; }
		catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "BiometricApi.bolehBiometrik"); }
		return false;
	}

	private static String targetUser(Tbmuser actor, JSONObject request) {
		if (actor == null || actor.getUserId() == null) return null;
		String target = clean(request.optString("target_user_id"), 255);
		if (target == null) target = actor.getUserId();
		return actor.getUserId().equals(target) || Common.getApakahAdminLain(actor) ? target : null;
	}

	private static BiometricEvent saveEvent(Session session, String actor, String subject, Long credentialId,
			String modality, String purpose, String mutation, boolean matched, Double score, Double live,
			String code, JSONObject request, JSONObject response) {
		BiometricEvent e = new BiometricEvent(); e.setActorUserId(actor); e.setSubjectUserId(subject); e.setCredentialId(credentialId);
		e.setModality(modality); e.setPurpose(purpose); e.setClientMutationId(mutation); e.setDeviceId(clean(request.optString("device_id"), 150));
		e.setMatched(Boolean.valueOf(matched)); e.setMatchScore(score); e.setLivenessScore(live); e.setResultCode(code);
		e.setReferenceType(clean(request.optString("reference_type"), 40)); e.setReferenceId(clean(request.optString("reference_id"), 150));
		Long captured = longValue(request, "captured_at_epoch"); e.setCapturedAt(captured == null ? new Date() : new Date(captured.longValue()));
		e.setResponseJson(response == null ? null : response.toString()); session.save(e); return e;
	}

	private static JSONObject replay(String actor, String purpose, String mutation) {
		Session session = HibernateUtil.openSession();
		try {
			BiometricEvent e = (BiometricEvent) session.createCriteria(BiometricEvent.class)
					.add(Restrictions.eq("actorUserId", actor)).add(Restrictions.eq("purpose", purpose))
					.add(Restrictions.eq("clientMutationId", mutation)).setMaxResults(1).uniqueResult();
			if (e == null) return null;
			return e.getResponseJson() == null ? ApiHelperSupport.status("00", "Permintaan biometrik sudah diproses") : new JSONObject(e.getResponseJson());
		} catch (Exception e) { return null; } finally { HibernateUtil.closeSessionQuietly(session); }
	}

	private static void updateEventResponse(Long id, JSONObject response, String mutation) {
		if (id == null) return; Session session = HibernateUtil.openSession(); Transaction tx = null;
		try { tx = session.beginTransaction(); BiometricEvent e = (BiometricEvent) session.get(BiometricEvent.class, id);
			if (e != null) { e.setPurpose("ATTENDANCE"); e.setClientMutationId(mutation); e.setResponseJson(response.toString()); e.setReferenceType("ATTENDANCE"); session.update(e); } tx.commit();
		} catch (Exception e) { rollback(tx); ais.common.ErrorAuditUtil.record(e, "BiometricApi.updateEventResponse"); }
		finally { HibernateUtil.closeSessionQuietly(session); }
	}

	private static String modality(String value) { String v = value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH); return FINGERPRINT.equals(v) || FACE.equals(v) ? v : null; }
	private static String clean(String value, int max) { if (value == null) return null; String v = value.trim(); return v.length() == 0 ? null : (v.length() > max ? v.substring(0, max) : v); }
	private static String nullToEmpty(String value) { return value == null ? "" : value; }
	private static String mutation(JSONObject request) { String v = clean(request.optString("clientMutationId"), 150); return v == null ? UUID.randomUUID().toString() : v; }
	private static String aad(String subject, String modality, String format) { return subject + "|" + modality + "|" + format; }
	private static Integer integer(JSONObject r, String key) { try { return r.isNull(key) ? null : Integer.valueOf(r.getInt(key)); } catch (Exception e) { return null; } }
	private static Long longValue(JSONObject r, String key) { try { return r.isNull(key) ? null : Long.valueOf(Long.parseLong(String.valueOf(r.get(key)))); } catch (Exception e) { return null; } }
	private static Double doubleValue(JSONObject r, String key) { try { return r.isNull(key) ? null : Double.valueOf(Double.parseDouble(String.valueOf(r.get(key)))); } catch (Exception e) { return null; } }
	private static double livenessThreshold() { try { String v = BiometricCrypto.setting("AIS_BIOMETRIC_LIVENESS_THRESHOLD"); return v == null ? 0.70d : Double.parseDouble(v); } catch (Exception e) { return 0.70d; } }
	private static long maximumOfflineAgeMillis() { try { String v = BiometricCrypto.setting("AIS_BIOMETRIC_MAX_OFFLINE_MINUTES"); return 60000L * (v == null ? 1440L : Long.parseLong(v)); } catch (Exception e) { return 86400000L; } }
	private static void rollback(Transaction tx) { if (tx != null) try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "BiometricApi.rollback"); } }

	private static final class Verification {
		private final JSONObject response; private final boolean matched; private final String modality; private final Long eventId;
		private Verification(JSONObject response, boolean matched, String modality, Long eventId) { this.response = response; this.matched = matched; this.modality = modality; this.eventId = eventId; }
		private static Verification error(JSONObject response) { return new Verification(response, false, "", null); }
	}
}
