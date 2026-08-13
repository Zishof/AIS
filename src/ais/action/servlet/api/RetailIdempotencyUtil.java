package ais.action.servlet.api;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.Session;
import org.json.JSONObject;

/** Exactly-once guard untuk write API retail yang memakai transaksi DB tunggal. */
public final class RetailIdempotencyUtil {

	private RetailIdempotencyUtil() {
	}

	/**
	 * @return respons sukses lama bila request sudah selesai, atau null bila caller
	 *         memperoleh hak memproses request baru.
	 */
	public static JSONObject mulai(Session session, String action, String key, String requestHash) throws Exception {
		if (key == null || key.trim().isEmpty()) return null;
		String cleanKey = key.trim();
		PreparedStatement insert = session.connection().prepareStatement(
				"INSERT INTO public.retail_request_idempotency(action,idempotency_key,request_hash,status) "
						+ "VALUES (?,?,?,'PROCESSING') ON CONFLICT DO NOTHING");
		insert.setString(1, action);
		insert.setString(2, cleanKey);
		insert.setString(3, requestHash == null || requestHash.trim().isEmpty() ? null : requestHash.trim());
		int inserted = insert.executeUpdate();
		insert.close();
		if (inserted == 1) return null;

		PreparedStatement select = session.connection().prepareStatement(
				"SELECT status,response_json,request_hash FROM public.retail_request_idempotency "
						+ "WHERE action=? AND idempotency_key=? FOR UPDATE");
		select.setString(1, action);
		select.setString(2, cleanKey);
		ResultSet rs = select.executeQuery();
		if (!rs.next()) {
			rs.close();
			select.close();
			throw new IllegalStateException("Status permintaan idempoten tidak dapat dibaca.");
		}
		String status = rs.getString(1);
		String response = rs.getString(2);
		String existingHash = rs.getString(3);
		rs.close();
		select.close();
		if (existingHash != null && requestHash != null && !existingHash.equals(requestHash)) {
			throw new IllegalArgumentException("Kunci permintaan sudah pernah dipakai untuk data yang berbeda.");
		}
		if ("COMPLETED".equals(status) && response != null && !response.trim().isEmpty()) {
			return new JSONObject(response);
		}
		throw new IllegalStateException(
				"Permintaan yang sama masih diproses atau hasil sebelumnya belum lengkap. Tunggu beberapa saat lalu muat ulang riwayat; jangan membuat transaksi baru.");
	}

	public static void selesai(Session session, String action, String key, String resultReference,
			JSONObject response) throws Exception {
		if (key == null || key.trim().isEmpty()) return;
		PreparedStatement update = session.connection().prepareStatement(
				"UPDATE public.retail_request_idempotency SET status='COMPLETED',result_reference=?,response_json=?,updated_at=NOW() "
						+ "WHERE action=? AND idempotency_key=?");
		update.setString(1, resultReference);
		update.setString(2, response == null ? null : response.toString());
		update.setString(3, action);
		update.setString(4, key.trim());
		update.executeUpdate();
		update.close();
	}

	public static void salin(JSONObject dari, JSONObject ke) throws Exception {
		java.util.Iterator<?> keys = dari.keys();
		while (keys.hasNext()) {
			String key = String.valueOf(keys.next());
			ke.put(key, dari.get(key));
		}
	}
}

