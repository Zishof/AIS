package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.KebijakanRetur;

/** API CRUD master Kebijakan Retur untuk POS Desktop dan Android. */
public final class KebijakanReturApiHelper {
	private KebijakanReturApiHelper() { }

	private static boolean bolehKelola(Tbmuser user) {
		try {
			ais.database.model.inventory.Pedagang p = user == null ? null : user.getPedagang();
			return p == null || Boolean.TRUE.equals(p.getSupervisor());
		} catch (Exception e) { return false; }
	}

	private static void tutup(Session s) {
		try { if (s != null && s.isOpen()) s.close(); }
		catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "KebijakanReturApiHelper.tutup"); }
	}

	public static void list(JSONObject request, JSONObject hasil) throws Exception {
		boolean semua = request.optBoolean("termasuk_nonaktif", false);
		Session s = HibernateUtil.getSessionFactory().openSession();
		try {
			PreparedStatement ps = s.connection().prepareStatement(
					"SELECT id,nama,COALESCE(keterangan,''),COALESCE(aktif,true) "
					+ "FROM koperasi.kebijakan_retur WHERE (? OR COALESCE(aktif,true)) "
					+ "ORDER BY CASE WHEN lower(btrim(nama))=lower(?) THEN 0 ELSE 1 END,nama");
			ps.setBoolean(1, semua);
			ps.setString(2, KebijakanRetur.TANPA_KEBIJAKAN);
			ResultSet rs = ps.executeQuery();
			JSONArray data = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1)); j.put("nama", rs.getString(2));
				j.put("keterangan", rs.getString(3)); j.put("aktif", rs.getBoolean(4));
				j.put("bawaan", KebijakanRetur.TANPA_KEBIJAKAN.equalsIgnoreCase(rs.getString(2).trim()));
				data.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00"); hasil.put("data", data);
		} finally { tutup(s); }
	}

	public static void simpan(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelola(user)) {
			hasil.put("status", "91"); hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola Kebijakan Retur."); return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) { hasil.put("status", "91"); hasil.put("description", "Nama Kebijakan Retur wajib diisi."); return; }
		Long id = request.has("id") && !request.isNull("id") ? Long.valueOf(request.get("id").toString()) : null;
		Session s = HibernateUtil.getSessionFactory().openSession();
		try {
			KebijakanRetur k = id == null ? new KebijakanRetur() : (KebijakanRetur) s.get(KebijakanRetur.class, id);
			if (k == null) { hasil.put("status", "91"); hasil.put("description", "Kebijakan Retur tidak ditemukan."); return; }
			if (KebijakanRetur.TANPA_KEBIJAKAN.equalsIgnoreCase(k.getNama())) {
				nama = KebijakanRetur.TANPA_KEBIJAKAN;
			}
			k.setNama(nama); k.setKeterangan(request.optString("keterangan", ""));
			k.setAktif(KebijakanRetur.TANPA_KEBIJAKAN.equalsIgnoreCase(nama) || !request.has("aktif") || request.optBoolean("aktif", true));
			s.beginTransaction(); s.saveOrUpdate(k); s.getTransaction().commit();
			hasil.put("status", "00"); hasil.put("id", k.getId());
		} finally { tutup(s); }
	}

	public static void hapus(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelola(user)) { hasil.put("status", "91"); hasil.put("description", "Anda tidak memiliki hak menghapus Kebijakan Retur."); return; }
		Long id = request.has("id") && !request.isNull("id") ? Long.valueOf(request.get("id").toString()) : null;
		if (id == null) { hasil.put("status", "91"); hasil.put("description", "Kebijakan Retur belum dipilih."); return; }
		Session s = HibernateUtil.getSessionFactory().openSession();
		try {
			KebijakanRetur k = (KebijakanRetur) s.get(KebijakanRetur.class, id);
			if (k == null) { hasil.put("status", "91"); hasil.put("description", "Kebijakan Retur tidak ditemukan."); return; }
			if (KebijakanRetur.TANPA_KEBIJAKAN.equalsIgnoreCase(k.getNama())) { hasil.put("status", "91"); hasil.put("description", "Kebijakan baku Tanpa Kebijakan Retur tidak dapat dihapus."); return; }
			Connection c = s.connection();
			PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM koperasi.produk WHERE kebijakan_retur=?"); ps.setLong(1, id);
			ResultSet rs = ps.executeQuery(); long dipakai = rs.next() ? rs.getLong(1) : 0; rs.close(); ps.close();
			if (dipakai > 0) { hasil.put("status", "91"); hasil.put("description", "Kebijakan masih dipakai " + dipakai + " produk. Ubah produk tersebut atau nonaktifkan kebijakannya."); return; }
			s.beginTransaction(); s.delete(k); s.getTransaction().commit(); hasil.put("status", "00");
		} finally { tutup(s); }
	}

	/** Menghasilkan entitas kebijakan yang diminta, atau kebijakan baku bila kosong/tidak valid. */
	public static KebijakanRetur resolveAtauBawaan(Session s, JSONObject request) {
		if (request.has("kebijakan_retur_id") && !request.isNull("kebijakan_retur_id")) {
			try {
				KebijakanRetur k = (KebijakanRetur) s.get(KebijakanRetur.class, Long.valueOf(request.get("kebijakan_retur_id").toString()));
				if (k != null && Boolean.TRUE.equals(k.getAktif())) return k;
			} catch (Exception abaikan) { }
		}
		return (KebijakanRetur) s.createCriteria(KebijakanRetur.class)
				.add(org.hibernate.criterion.Restrictions.ilike("nama", KebijakanRetur.TANPA_KEBIJAKAN))
				.setMaxResults(1).uniqueResult();
	}
}

