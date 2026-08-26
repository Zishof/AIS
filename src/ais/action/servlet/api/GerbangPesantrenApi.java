package ais.action.servlet.api;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.biometric.IzinGerbangPesantren;

/** Workflow izin keluar/masuk pondok dengan verifikasi biometrik fail-closed. */
public final class GerbangPesantrenApi {
	private GerbangPesantrenApi() { }

	public static JSONObject capability(HttpServletRequest req, JSONObject request) throws Exception {
		Tbmuser actor = ApiUtil.currentUser(request, req);
		if (actor == null || actor.getUserId() == null) return ApiHelperSupport.status("91", "Sesi login tidak valid");
		JSONObject out = ApiHelperSupport.status("00", "Kemampuan gerbang berhasil dimuat");
		out.put("boleh_mengajukan", true); out.put("boleh_mengelola", bolehPetugas(actor));
		out.put("mode_verifikasi", "ONLINE_FAIL_CLOSED");
		out.put("catatan_offline", "Snapshot izin boleh dibaca offline, tetapi keputusan gerbang menunggu matcher server sampai SDK cache bertanda tangan tersedia");
		return out;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject list(HttpServletRequest req, JSONObject request) {
		Tbmuser actor = ApiUtil.currentUser(request, req);
		if (actor == null || actor.getUserId() == null) return ApiHelperSupport.status("91", "Sesi login tidak valid");
		String status = clean(request.optString("status"), 30);
		String query = clean(request.optString("query"), 120);
		int limit = integer(request, "limit", 100, 1, 200);
		Session session = HibernateUtil.openSession();
		try {
			org.hibernate.Criteria criteria = session.createCriteria(IzinGerbangPesantren.class)
					.add(Restrictions.eq("aktif", Boolean.TRUE));
			if (!bolehPetugas(actor)) criteria.add(Restrictions.eq("subjectUserId", actor.getUserId()));
			if (status != null && !"SEMUA".equalsIgnoreCase(status))
				criteria.add(Restrictions.eq("status", status.toUpperCase(Locale.ENGLISH)));
			if (query != null) {
				Disjunction search = Restrictions.disjunction();
				search.add(Restrictions.ilike("subjectUserId", query, MatchMode.ANYWHERE));
				search.add(Restrictions.ilike("tujuan", query, MatchMode.ANYWHERE));
				search.add(Restrictions.ilike("alasan", query, MatchMode.ANYWHERE));
				criteria.add(search);
			}
			criteria.addOrder(Order.desc("diubahPada")).setMaxResults(limit);
			List<IzinGerbangPesantren> rows = criteria.list();
			JSONArray data = new JSONArray();
			for (IzinGerbangPesantren row : rows) data.put(toJson(session, row));
			JSONObject out = ApiHelperSupport.status("00", "Daftar izin gerbang berhasil dimuat");
			out.put("data", data); out.put("total", data.length()); out.put("dari_server", true); return out;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); return ApiHelperSupport.errorResponse("Gagal memuat izin gerbang");
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static JSONObject create(HttpServletRequest req, JSONObject request) {
		Tbmuser actor = ApiUtil.currentUser(request, req);
		if (actor == null || actor.getUserId() == null) return ApiHelperSupport.status("91", "Sesi login tidak valid");
		String subjectId = clean(request.optString("target_user_id"), 255);
		if (subjectId == null || !bolehPetugas(actor)) subjectId = actor.getUserId();
		String reason = clean(request.optString("alasan"), 500);
		String destination = clean(request.optString("tujuan"), 500);
		String companion = clean(request.optString("pendamping"), 255);
		Long exitEpoch = longValue(request, "rencana_keluar_epoch");
		Long returnEpoch = longValue(request, "rencana_kembali_epoch");
		if (reason == null || destination == null || exitEpoch == null || returnEpoch == null)
			return ApiHelperSupport.status("92", "Alasan, tujuan, rencana keluar, dan rencana kembali wajib diisi");
		if (returnEpoch.longValue() <= exitEpoch.longValue())
			return ApiHelperSupport.status("92", "Rencana kembali harus setelah waktu keluar");
		String mutation = mutation(request);
		Session session = HibernateUtil.openSession(); Transaction tx = null;
		try {
			IzinGerbangPesantren replay = (IzinGerbangPesantren) session.createCriteria(IzinGerbangPesantren.class)
					.add(Restrictions.eq("requesterUserId", actor.getUserId()))
					.add(Restrictions.eq("clientMutationId", mutation)).setMaxResults(1).uniqueResult();
			if (replay != null) { JSONObject out = ApiHelperSupport.status("00", "Pengajuan sudah tersimpan"); out.put("data", toJson(session, replay)); out.put("idempoten_replay", true); return out; }
			if (session.get(Tbmuser.class, subjectId) == null) return ApiHelperSupport.status("94", "Pengguna tidak ditemukan");
			tx = session.beginTransaction();
			IzinGerbangPesantren row = new IzinGerbangPesantren();
			row.setSubjectUserId(subjectId); row.setRequesterUserId(actor.getUserId()); row.setClientMutationId(mutation);
			row.setAlasan(reason); row.setTujuan(destination); row.setPendamping(companion);
			row.setRencanaKeluar(new Date(exitEpoch.longValue())); row.setRencanaKembali(new Date(returnEpoch.longValue()));
			row.setStatus("DIAJUKAN"); session.save(row); tx.commit();
			JSONObject out = ApiHelperSupport.status("00", "Izin berhasil diajukan"); out.put("data", toJson(session, row)); return out;
		} catch (Exception e) {
			rollback(tx); Common.tampilErrorJikaAdmin(e); return ApiHelperSupport.errorResponse("Gagal mengajukan izin gerbang");
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static JSONObject process(HttpServletRequest req, JSONObject request) {
		Tbmuser actor = ApiUtil.currentUser(request, req);
		if (!bolehPetugas(actor)) return ApiHelperSupport.status("93", "Tidak memiliki izin memproses izin gerbang");
		Long id = longValue(request, "id"); String decision = clean(request.optString("keputusan"), 20);
		if (id == null || decision == null) return ApiHelperSupport.status("92", "ID dan keputusan wajib diisi");
		decision = decision.toUpperCase(Locale.ENGLISH);
		if (!"SETUJUI".equals(decision) && !"TOLAK".equals(decision)) return ApiHelperSupport.status("92", "Keputusan tidak dikenal");
		Session session = HibernateUtil.openSession(); Transaction tx = null;
		try {
			tx = session.beginTransaction();
			IzinGerbangPesantren row = (IzinGerbangPesantren) session.get(IzinGerbangPesantren.class, id, LockMode.UPGRADE);
			if (row == null) { rollback(tx); return ApiHelperSupport.status("94", "Izin tidak ditemukan"); }
			if (!"DIAJUKAN".equals(row.getStatus())) { rollback(tx); return ApiHelperSupport.status("91", "Izin sudah diproses sebelumnya"); }
			row.setStatus("SETUJUI".equals(decision) ? "DISETUJUI" : "DITOLAK");
			row.setDiprosesOleh(actor.getUserId()); row.setDiprosesPada(new Date());
			row.setCatatanPetugas(clean(request.optString("catatan"), 1000)); row.setDiubahPada(new Date());
			session.update(row); tx.commit();
			JSONObject out = ApiHelperSupport.status("00", "Izin berhasil diproses"); out.put("data", toJson(session, row)); return out;
		} catch (Exception e) { rollback(tx); Common.tampilErrorJikaAdmin(e); return ApiHelperSupport.errorResponse("Gagal memproses izin"); }
		finally { HibernateUtil.closeSessionQuietly(session); }
	}

	/** Verifikasi biometrik dan ubah status keluar/kembali dalam satu keputusan server. */
	public static JSONObject verifyGate(HttpServletRequest req, JSONObject request) {
		Tbmuser actor = ApiUtil.currentUser(request, req);
		if (!bolehPetugas(actor)) return ApiHelperSupport.status("93", "Tidak memiliki izin petugas gerbang");
		Long id = longValue(request, "id"); String direction = clean(request.optString("arah"), 20);
		if (id == null || direction == null) return ApiHelperSupport.status("92", "Izin dan arah gerbang wajib dipilih");
		direction = direction.toUpperCase(Locale.ENGLISH);
		if (!"KELUAR".equals(direction) && !"KEMBALI".equals(direction)) return ApiHelperSupport.status("92", "Arah gerbang tidak dikenal");
		Session session = HibernateUtil.openSession(); Transaction tx = null;
		try {
			tx = session.beginTransaction();
			IzinGerbangPesantren row = (IzinGerbangPesantren) session.get(IzinGerbangPesantren.class, id, LockMode.UPGRADE);
			if (row == null) { rollback(tx); return ApiHelperSupport.status("94", "Izin tidak ditemukan"); }
			if ("KELUAR".equals(direction) && "KELUAR".equals(row.getStatus())) { rollback(tx); return success(session, row, "Santri sudah tercatat keluar", true); }
			if ("KEMBALI".equals(direction) && "KEMBALI".equals(row.getStatus())) { rollback(tx); return success(session, row, "Santri sudah tercatat kembali", true); }
			if ("KELUAR".equals(direction) && !"DISETUJUI".equals(row.getStatus())) { rollback(tx); return ApiHelperSupport.status("91", "Izin belum disetujui atau sudah tidak berlaku"); }
			if ("KEMBALI".equals(direction) && !"KELUAR".equals(row.getStatus())) { rollback(tx); return ApiHelperSupport.status("91", "Santri belum tercatat keluar"); }
			if ("KELUAR".equals(direction) && row.getRencanaKembali().before(new Date())) { rollback(tx); return ApiHelperSupport.status("91", "Izin sudah kedaluwarsa"); }

			JSONObject verificationRequest = new JSONObject(request.toString());
			verificationRequest.put("reference_type", "GATE_PERMISSION"); verificationRequest.put("reference_id", String.valueOf(id));
			if (clean(verificationRequest.optString("clientMutationId"), 150) == null)
				verificationRequest.put("clientMutationId", "gate-" + UUID.randomUUID().toString());
			JSONObject verification = BiometricApi.verifyLinkedSubject(actor, row.getSubjectUserId(), verificationRequest,
					"KELUAR".equals(direction) ? "GATE_EXIT" : "GATE_ENTRY");
			if (!"00".equals(verification.optString("status")) || !verification.optBoolean("matched", false)) { rollback(tx); return verification; }
			Long eventId = longValue(verification, "event_id");
			if ("KELUAR".equals(direction)) { row.setStatus("KELUAR"); row.setKeluarPada(new Date()); row.setEventKeluarId(eventId); }
			else { row.setStatus("KEMBALI"); row.setKembaliPada(new Date()); row.setEventKembaliId(eventId); }
			row.setDiubahPada(new Date()); session.update(row); tx.commit();
			return success(session, row, "KELUAR".equals(direction) ? "Keluar pondok tercatat" : "Kembali ke pondok tercatat", false);
		} catch (Exception e) { rollback(tx); Common.tampilErrorJikaAdmin(e); return ApiHelperSupport.errorResponse("Verifikasi gerbang gagal"); }
		finally { HibernateUtil.closeSessionQuietly(session); }
	}

	private static JSONObject success(Session session, IzinGerbangPesantren row, String message, boolean replay) throws Exception {
		JSONObject out = ApiHelperSupport.status("00", message); out.put("data", toJson(session, row));
		if (replay) out.put("idempoten_replay", true); return out;
	}

	private static JSONObject toJson(Session session, IzinGerbangPesantren row) throws Exception {
		JSONObject out = new JSONObject(); out.put("id", row.getId()); out.put("user_id", row.getSubjectUserId());
		Tbmuser subject = (Tbmuser) session.get(Tbmuser.class, row.getSubjectUserId());
		out.put("nama", subject == null ? row.getSubjectUserId() : subject.getUserNama());
		out.put("alasan", row.getAlasan()); out.put("tujuan", row.getTujuan()); out.put("pendamping", row.getPendamping());
		out.put("status", row.getStatus()); out.put("rencana_keluar_epoch", epoch(row.getRencanaKeluar()));
		out.put("rencana_kembali_epoch", epoch(row.getRencanaKembali())); out.put("keluar_epoch", epoch(row.getKeluarPada()));
		out.put("kembali_epoch", epoch(row.getKembaliPada())); out.put("catatan", row.getCatatanPetugas());
		out.put("updated_at_epoch", epoch(row.getDiubahPada())); return out;
	}

	private static boolean bolehPetugas(Tbmuser actor) {
		if (actor == null || actor.getUserId() == null) return false;
		if (Common.getApakahAdminLain(actor)) return true;
		try { for (Tbmrole role : actor.ambilRoles()) if (role != null
				&& Boolean.TRUE.equals(role.getAksesGerbangPesantren())) return true; }
		catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "GerbangPesantrenApi.bolehPetugas"); }
		return false;
	}

	private static String mutation(JSONObject request) {
		String value = clean(request.optString("clientMutationId"), 150);
		return value == null ? UUID.randomUUID().toString() : value;
	}
	private static String clean(String value, int max) { if (value == null) return null; value = value.trim(); return value.length() == 0 ? null : value.substring(0, Math.min(max, value.length())); }
	private static Long longValue(JSONObject request, String key) { try { return request == null || request.isNull(key) ? null : Long.valueOf(String.valueOf(request.get(key))); } catch (Exception e) { return null; } }
	private static int integer(JSONObject request, String key, int fallback, int min, int max) { Long value = longValue(request, key); return value == null ? fallback : Math.max(min, Math.min(max, value.intValue())); }
	private static long epoch(Date value) { return value == null ? 0L : value.getTime(); }
	private static void rollback(Transaction tx) { if (tx != null) try { tx.rollback(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "GerbangPesantrenApi.rollback"); } }
}
