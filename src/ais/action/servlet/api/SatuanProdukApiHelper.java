package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.SatuanProduk;

/** CRUD master satuan produk POS (Pcs, Kg, Liter, Dus, dan sebagainya). */
public final class SatuanProdukApiHelper {

	private SatuanProdukApiHelper() {
	}

	private static boolean bolehKelola(Tbmuser tbmuser) {
		try {
			Pedagang pedagang = tbmuser == null ? null : tbmuser.getPedagang();
			return pedagang == null || Boolean.TRUE.equals(pedagang.getSupervisor());
		} catch (Exception e) {
			return false;
		}
	}

	private static void tutup(ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SatuanProdukApiHelper.tutupResultSet");
		}
	}

	private static void tutup(PreparedStatement ps) {
		try {
			if (ps != null) {
				ps.close();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SatuanProdukApiHelper.tutupStatement");
		}
	}

	/** openSession milik helper ini selalu dibersihkan dan ditutup penuh. */
	private static void tutup(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				session.clear();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SatuanProdukApiHelper.clear");
		}
		try {
			if (session.isConnected()) {
				session.disconnect();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SatuanProdukApiHelper.disconnect");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SatuanProdukApiHelper.close");
		}
	}

	private static void rollback(Transaction transaksi) {
		try {
			if (transaksi != null && transaksi.isActive()) {
				transaksi.rollback();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SatuanProdukApiHelper.rollback");
		}
	}

	public static void uomList(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request.optString("keyword", "").trim();
		boolean termasukNonaktif = request.optBoolean("termasuk_nonaktif", false);
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 25)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		PreparedStatement psCount = null;
		PreparedStatement ps = null;
		ResultSet rsCount = null;
		ResultSet rs = null;
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			if (!termasukNonaktif) {
				where.append(" AND COALESCE(sp.aktif,true) = true ");
			}
			if (!keyword.isEmpty()) {
				where.append(" AND sp.nama ILIKE ? ");
			}
			Connection conn = session.connection();
			psCount = conn.prepareStatement("SELECT COUNT(*) FROM koperasi.satuan_produk sp" + where);
			if (!keyword.isEmpty()) {
				psCount.setString(1, "%" + keyword + "%");
			}
			rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0L;

			ps = conn.prepareStatement("SELECT sp.id, sp.nama, COALESCE(sp.aktif,true) "
					+ "FROM koperasi.satuan_produk sp" + where + " ORDER BY sp.nama ASC LIMIT ? OFFSET ?");
			int index = 1;
			if (!keyword.isEmpty()) {
				ps.setString(index++, "%" + keyword + "%");
			}
			ps.setInt(index++, pageSize);
			ps.setInt(index++, offset);
			rs = ps.executeQuery();
			JSONArray data = new JSONArray();
			while (rs.next()) {
				JSONObject row = new JSONObject();
				row.put("id", rs.getLong(1));
				row.put("nama", rs.getString(2));
				row.put("aktif", rs.getBoolean(3));
				data.put(row);
			}
			hasil.put("status", "00");
			hasil.put("data", data);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			tutup(rs);
			tutup(rsCount);
			tutup(ps);
			tutup(psCount);
			tutup(session);
		}
	}

	public static void uomSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelola(tbmuser)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin atau supervisor toko yang dapat mengelola satuan produk.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama satuan wajib diisi.");
			return;
		}
		Long id = ais.common.Common.angkaAtauNull(request, "id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction transaksi = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = session.connection().prepareStatement("SELECT COUNT(*) FROM koperasi.satuan_produk "
					+ "WHERE lower(trim(nama)) = lower(trim(?)) AND (? IS NULL OR id <> ?)");
			ps.setString(1, nama);
			if (id == null) {
				ps.setNull(2, java.sql.Types.BIGINT);
				ps.setNull(3, java.sql.Types.BIGINT);
			} else {
				ps.setLong(2, id.longValue());
				ps.setLong(3, id.longValue());
			}
			rs = ps.executeQuery();
			if (rs.next() && rs.getLong(1) > 0L) {
				hasil.put("status", "91");
				hasil.put("description", "Nama satuan sudah digunakan.");
				return;
			}
			SatuanProduk satuan = id == null ? new SatuanProduk()
					: (SatuanProduk) session.get(SatuanProduk.class, id);
			if (satuan == null) {
				hasil.put("status", "91");
				hasil.put("description", "Satuan produk tidak ditemukan.");
				return;
			}
			satuan.setNama(nama);
			satuan.setAktif(Boolean.valueOf(!request.has("aktif") || request.optBoolean("aktif", true)));
			transaksi = session.beginTransaction();
			session.saveOrUpdate(satuan);
			transaksi.commit();
			hasil.put("status", "00");
			hasil.put("id", satuan.getId());
		} catch (Exception e) {
			rollback(transaksi);
			throw e;
		} finally {
			tutup(rs);
			tutup(ps);
			tutup(session);
		}
	}

	public static void uomHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelola(tbmuser)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin atau supervisor toko yang dapat menghapus satuan produk.");
			return;
		}
		Long id = ais.common.Common.angkaAtauNull(request, "id");
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "Id satuan wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction transaksi = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = session.connection().prepareStatement("SELECT COUNT(*) FROM koperasi.produk WHERE satuan = ?");
			ps.setLong(1, id.longValue());
			rs = ps.executeQuery();
			long dipakai = rs.next() ? rs.getLong(1) : 0L;
			if (dipakai > 0L) {
				hasil.put("status", "91");
				hasil.put("description", "Satuan masih dipakai " + dipakai
						+ " produk. Nonaktifkan satuan daripada menghapusnya.");
				return;
			}
			SatuanProduk satuan = (SatuanProduk) session.get(SatuanProduk.class, id);
			if (satuan == null) {
				hasil.put("status", "91");
				hasil.put("description", "Satuan produk tidak ditemukan.");
				return;
			}
			transaksi = session.beginTransaction();
			session.delete(satuan);
			transaksi.commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			rollback(transaksi);
			throw e;
		} finally {
			tutup(rs);
			tutup(ps);
			tutup(session);
		}
	}
}
