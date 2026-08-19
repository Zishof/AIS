package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * <h3>API JSON "Konfigurasi Kode Akun" -- Akun, Daftar Akun, Bank, dan Jenis Transaksi.</h3>
 *
 * <p>Memindahkan menu yang selama ini HANYA ada di layar ZK ({@code pages/master/akunting/akun.zul})
 * ke POS Desktop/Android, dengan ZK sebagai RUJUKAN bentuk data: kolom, hierarki induk-anak,
 * arah debet/kredit, grup akun, dan penanda "sedang dipakai" dibuat sama supaya angka dan
 * struktur yang dilihat pengguna identik di semua kanal.</p>
 *
 * <p>Baca memakai SQL native (ringan, tidak menyeret graf Hibernate); tulis memakai session
 * Hibernate agar tetap ter-audit Envers seperti layar ZK.</p>
 */
public final class KodeAkunApiHelper {

	private KodeAkunApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	/**
	 * Pohon akun + metadata. Param opsional {@code cari} (kode/nama). Setiap baris membawa
	 * {@code parentId} sehingga klien dapat menyusun hierarki seperti layar ZK, dan
	 * {@code jumlahDipakai} sebagai pengaman: akun yang sudah dipakai transaksi tidak
	 * boleh dihapus sembarangan.
	 */
	public static void akunDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT a.id, COALESCE(a.kode,''), COALESCE(a.nama,''), COALESCE(a.keterangan,''),"
							+ " COALESCE(a.debit_credit,0), a.parent, COALESCE(g.nama,''), a.grup_akun,"
							+ " COALESCE(a.jmldipakai,0)"
							+ " FROM akunting.akun a"
							+ " LEFT JOIN akunting.grup_akun g ON g.id = a.grup_akun");
			if (!cari.isEmpty()) {
				sql.append(" WHERE (a.nama ILIKE ? OR a.kode ILIKE ?)");
			}
			sql.append(" ORDER BY a.kode ASC");
			PreparedStatement ps = conn.prepareStatement(sql.toString());
			if (!cari.isEmpty()) {
				String kw = "%" + cari + "%";
				ps.setString(1, kw);
				ps.setString(2, kw);
			}
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("keterangan", rs.getString(4));
				int dc = rs.getInt(5);
				j.put("debetCredit", dc);
				j.put("posisi", dc == 1 ? "Debet" : "Credit");
				long par = rs.getLong(6);
				j.put("parentId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(par));
				j.put("grupAkun", rs.getString(7));
				long gid = rs.getLong(8);
				j.put("grupAkunId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(gid));
				j.put("jumlahDipakai", rs.getLong(9));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Opsi Grup Akun (dipakai form Akun di klien). */
	public static void grupAkunDaftar(JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			PreparedStatement ps = conn.prepareStatement(
					"SELECT id, COALESCE(nama,''), COALESCE(keterangan,'') FROM akunting.grup_akun ORDER BY nama");
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				j.put("keterangan", rs.getString(3));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Daftar Bank + akun kasnya (tab "Bank" pada layar ZK). */
	public static void bankDaftar(JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT b.id, COALESCE(b.nama,''), COALESCE(b.keterangan,''), COALESCE(b.aktif,true),"
							+ " b.akun, COALESCE(a.kode,''), COALESCE(a.nama,'')"
							+ " FROM public.bank b LEFT JOIN akunting.akun a ON a.id = b.akun");
			if (!cari.isEmpty()) {
				sql.append(" WHERE b.nama ILIKE ?");
			}
			sql.append(" ORDER BY b.nama ASC");
			PreparedStatement ps = conn.prepareStatement(sql.toString());
			if (!cari.isEmpty()) {
				ps.setString(1, "%" + cari + "%");
			}
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				j.put("keterangan", rs.getString(3));
				j.put("aktif", rs.getBoolean(4));
				long ak = rs.getLong(5);
				j.put("akunId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(ak));
				j.put("akunKode", rs.getString(6));
				j.put("akunNama", rs.getString(7));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Daftar Jenis Transaksi + akun terkait (tab "Jenis Transaksi" pada layar ZK). */
	public static void jenisTransaksiDaftar(JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT t.id, COALESCE(t.kode,''), COALESCE(t.nama,''), COALESCE(t.keterangan,''),"
							+ " COALESCE(t.aktif,true), t.akun, COALESCE(a.kode,''), COALESCE(a.nama,'')"
							+ " FROM akunting.jenis_transaksi t LEFT JOIN akunting.akun a ON a.id = t.akun");
			if (!cari.isEmpty()) {
				sql.append(" WHERE (t.nama ILIKE ? OR t.kode ILIKE ?)");
			}
			sql.append(" ORDER BY t.kode ASC, t.nama ASC");
			PreparedStatement ps = conn.prepareStatement(sql.toString());
			if (!cari.isEmpty()) {
				String kw = "%" + cari + "%";
				ps.setString(1, kw);
				ps.setString(2, kw);
			}
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("keterangan", rs.getString(4));
				j.put("aktif", rs.getBoolean(5));
				long ak = rs.getLong(6);
				j.put("akunId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(ak));
				j.put("akunKode", rs.getString(7));
				j.put("akunNama", rs.getString(8));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Dipakai dispatcher: seluruh aksi berawalan {@code kode_akun_} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("kode_akun_daftar".equals(action)) {
			akunDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_grup".equals(action)) {
			grupAkunDaftar(hasil);
			return true;
		}
		if ("kode_akun_bank".equals(action)) {
			bankDaftar(request, hasil);
			return true;
		}
		if ("kode_akun_jenis_transaksi".equals(action)) {
			jenisTransaksiDaftar(request, hasil);
			return true;
		}
		return false;
	}
}
