package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.inventory.JenisProduk;

/**
 * <h3>API eBisnis — CRUD Jenis Produk (kategori) + daftar Akun untuk pemilih akun.</h3>
 *
 * <p>Dipakai layar "Jenis Produk" pada aplikasi Flutter/Desktop eBisnis (servlet
 * {@code ApiEBisnis}/{@code PosApi}, aksi bernama: {@code jenis_produk_list},
 * {@code jenis_produk_simpan}, {@code jenis_produk_hapus}, {@code akun_list}). Sengaja dibuat
 * kelas TERPISAH dari {@code KantinHelper} agar tidak tergantung pada method privat di sana.</p>
 *
 * <p>Menyimpan 3 akun akuntansi per jenis produk — <b>Pendapatan Penjualan</b>, <b>PPN Keluaran</b>,
 * <b>HPP</b> ({@link JenisProduk#getAkunPendapatan()} dkk, FK ke {@code akunting.akun}) — konsisten
 * dengan master ZK ({@code JenisProdukAction}) dan JSP ({@code jenis_produk.jsp}). FK dikirim sebagai
 * <b>id</b> (skalar), server yang me-resolve ke entitas {@link Akun}.</p>
 */
public final class JenisProdukApiHelper {

	private JenisProdukApiHelper() {
	}

	private static boolean bolehKelola(Tbmuser tbmuser) {
		try {
			ais.database.model.inventory.Pedagang pedagang = tbmuser == null ? null : tbmuser.getPedagang();
			// Admin global (bukan pedagang toko) atau supervisor toko boleh mengelola master.
			return pedagang == null || Boolean.TRUE.equals(pedagang.getSupervisor());
		} catch (Exception e) {
			return false;
		}
	}

	private static void tutup(Session session) {
		try {
			if (session != null && session.isOpen()) {
				session.close();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) JenisProdukApiHelper.tutup");
		}
	}

	/** Daftar jenis produk (kategori) + akun terpetakan, dengan paging & pencarian. */
	public static void jenisProdukList(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request.optString("keyword", "").trim();
		boolean termasukNonaktif = request.optBoolean("termasuk_nonaktif", false);
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));
		int offset = (page - 1) * pageSize;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			if (!termasukNonaktif) {
				where.append(" AND COALESCE(jp.aktif,true) = true ");
			}
			if (!keyword.isEmpty()) {
				where.append(" AND (jp.nama ILIKE ? OR jp.keterangan ILIKE ?) ");
			}

			Connection conn = session.connection();
			PreparedStatement psCount = conn
					.prepareStatement("SELECT COUNT(*) FROM koperasi.jenis_produk jp" + where);
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				psCount.setString(1, kw);
				psCount.setString(2, kw);
			}
			ResultSet rsCount = psCount.executeQuery();
			long total = rsCount.next() ? rsCount.getLong(1) : 0;
			rsCount.close();
			psCount.close();

			PreparedStatement ps = conn.prepareStatement(
					"SELECT jp.id, jp.nama, COALESCE(jp.keterangan,''), COALESCE(jp.maksimalharian,0), "
							+ "COALESCE(jp.defaultproduk,false), COALESCE(jp.aktif,true), "
							+ "jp.akun_pendapatan, ap.kode, ap.nama, "
							+ "jp.akun_ppn_keluaran, app.kode, app.nama, "
							+ "jp.akun_hpp, ah.kode, ah.nama, "
							+ "jp.akun_selisih_persediaan, asl.kode, asl.nama, "
							+ "jp.akun_retur_penjualan, art.kode, art.nama "
							+ "FROM koperasi.jenis_produk jp "
							+ "LEFT JOIN akunting.akun ap  ON ap.id  = jp.akun_pendapatan "
							+ "LEFT JOIN akunting.akun app ON app.id = jp.akun_ppn_keluaran "
							+ "LEFT JOIN akunting.akun ah  ON ah.id  = jp.akun_hpp "
							+ "LEFT JOIN akunting.akun asl ON asl.id = jp.akun_selisih_persediaan "
							+ "LEFT JOIN akunting.akun art ON art.id = jp.akun_retur_penjualan "
							+ where + " ORDER BY jp.nama ASC LIMIT ? OFFSET ?");
			int idx = 1;
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				ps.setString(idx++, kw);
				ps.setString(idx++, kw);
			}
			ps.setInt(idx++, pageSize);
			ps.setInt(idx++, offset);
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				j.put("keterangan", rs.getString(3));
				j.put("maksimalHarian", rs.getDouble(4));
				j.put("defaultProduk", rs.getBoolean(5));
				j.put("aktif", rs.getBoolean(6));
				long apId = rs.getLong(7);
				j.put("akunPendapatanId", rs.wasNull() ? JSONObject.NULL : apId);
				j.put("akunPendapatanNama", labelAkun(rs.getString(8), rs.getString(9)));
				long appId = rs.getLong(10);
				j.put("akunPpnKeluaranId", rs.wasNull() ? JSONObject.NULL : appId);
				j.put("akunPpnKeluaranNama", labelAkun(rs.getString(11), rs.getString(12)));
				long ahId = rs.getLong(13);
				j.put("akunHppId", rs.wasNull() ? JSONObject.NULL : ahId);
				j.put("akunHppNama", labelAkun(rs.getString(14), rs.getString(15)));
				long aslId = rs.getLong(16);
				j.put("akunSelisihPersediaanId", rs.wasNull() ? JSONObject.NULL : aslId);
				j.put("akunSelisihPersediaanNama", labelAkun(rs.getString(17), rs.getString(18)));
				long artId = rs.getLong(19);
				j.put("akunReturPenjualanId", rs.wasNull() ? JSONObject.NULL : artId);
				j.put("akunReturPenjualanNama", labelAkun(rs.getString(20), rs.getString(21)));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			tutup(session);
		}
	}

	/** Simpan (create/update) satu {@link JenisProduk} beserta 3 akun (id). */
	public static void jenisProdukSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelola(tbmuser)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola Jenis Produk.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			hasil.put("status", "91");
			hasil.put("description", "Nama jenis produk wajib diisi.");
			return;
		}
		Long id = ais.common.Common.angkaAtauNull(request, "id");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JenisProduk jp;
			if (id == null) {
				jp = new JenisProduk();
			} else {
				jp = (JenisProduk) session.get(JenisProduk.class, id);
				if (jp == null) {
					hasil.put("status", "91");
					hasil.put("description", "Jenis produk tidak ditemukan.");
					return;
				}
			}
			jp.setNama(nama);
			jp.setKeterangan(request.optString("keterangan", ""));
			if (request.has("maksimalHarian") && !request.isNull("maksimalHarian")) {
				jp.setMaksimalHarian(Double.valueOf(request.optDouble("maksimalHarian", 0)));
			}
			jp.setDefaultProduk(Boolean.valueOf(request.optBoolean("defaultProduk", false)));
			jp.setAktif(!request.has("aktif") || request.optBoolean("aktif", true));
			jp.setAkunPendapatan(akunDariId(session, request, "akunPendapatanId"));
			jp.setAkunPpnKeluaran(akunDariId(session, request, "akunPpnKeluaranId"));
			jp.setAkunHpp(akunDariId(session, request, "akunHppId"));
			// Akun selisih persediaan (susut/temuan) -- lawan jurnal stok opname.
			jp.setAkunSelisihPersediaan(akunDariId(session, request, "akunSelisihPersediaanId"));
			// Akun retur penjualan (kontra-pendapatan); kosong = pakai akun pendapatan jenis ini.
			jp.setAkunReturPenjualan(akunDariId(session, request, "akunReturPenjualanId"));

			session.beginTransaction();
			session.saveOrUpdate(jp);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", jp.getId());
		} finally {
			tutup(session);
		}
	}

	/** Hapus satu {@link JenisProduk}; ditolak bila masih dipakai produk. */
	public static void jenisProdukHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelola(tbmuser)) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat menghapus Jenis Produk.");
			return;
		}
		if (request.isNull("id")) {
			hasil.put("status", "91");
			hasil.put("description", "Id jenis produk tidak ada.");
			return;
		}
		Long id = Long.valueOf((request.get("id") + "").trim());
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			PreparedStatement psCek = conn
					.prepareStatement("SELECT COUNT(*) FROM koperasi.produk WHERE jenis_produk = ?");
			psCek.setLong(1, id.longValue());
			ResultSet rsCek = psCek.executeQuery();
			long dipakai = rsCek.next() ? rsCek.getLong(1) : 0;
			rsCek.close();
			psCek.close();
			if (dipakai > 0) {
				hasil.put("status", "91");
				hasil.put("description", "Jenis produk masih dipakai " + dipakai
						+ " produk. Nonaktifkan saja (matikan status Aktif) daripada menghapus.");
				return;
			}
			JenisProduk jp = (JenisProduk) session.get(JenisProduk.class, id);
			if (jp == null) {
				hasil.put("status", "91");
				hasil.put("description", "Jenis produk tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(jp);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} finally {
			tutup(session);
		}
	}

	/** Daftar Akun akuntansi (id, kode, nama) untuk pemilih akun — pencarian opsional. */
	public static void akunList(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request.optString("keyword", "").trim();
		int limit = Math.min(2000, Math.max(1, request.optInt("limit", 1000)));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder("SELECT id, COALESCE(kode,''), COALESCE(nama,'') FROM akunting.akun");
			if (!keyword.isEmpty()) {
				sql.append(" WHERE (nama ILIKE ? OR kode ILIKE ?)");
			}
			sql.append(" ORDER BY kode ASC LIMIT ?");
			PreparedStatement ps = conn.prepareStatement(sql.toString());
			int idx = 1;
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				ps.setString(idx++, kw);
				ps.setString(idx++, kw);
			}
			ps.setInt(idx++, limit);
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("label", labelAkun(rs.getString(2), rs.getString(3)));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			tutup(session);
		}
	}

	private static Akun akunDariId(Session session, JSONObject request, String key) {
		try {
			if (!request.has(key) || request.isNull(key)) {
				return null;
			}
			String v = (request.get(key) + "").trim();
			if (v.isEmpty() || v.equalsIgnoreCase("null")) {
				return null;
			}
			return (Akun) session.get(Akun.class, Long.valueOf(Long.parseLong(v)));
		} catch (Exception e) {
			return null;
		}
	}

	private static String labelAkun(String kode, String nama) {
		String k = kode == null ? "" : kode.trim();
		String n = nama == null ? "" : nama.trim();
		if (k.isEmpty() && n.isEmpty()) {
			return "";
		}
		return (k.isEmpty() ? "" : k + " - ") + n;
	}
}
