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

	/**
	 * Impor/pembaruan akun dari berkas Excel yang sudah diurai klien menjadi
	 * {@code baris: [{kode, nama, keterangan, posisi, grupAkun, kodeParent}, ...]}.
	 *
	 * <p><b>Aturan yang disengaja, karena ini data master akuntansi:</b></p>
	 * <ul>
	 * <li>Pencocokan memakai {@code kode} (kolom unik). Kode belum ada = DIBUAT,
	 *     kode sudah ada = DIPERBARUI. Tidak pernah menghapus apa pun.</li>
	 * <li>Baris tanpa kode atau tanpa nama DITOLAK dengan alasan, bukan diam-diam dilewati.</li>
	 * <li>Induk dirujuk lewat KODE induk; bila kodenya tidak ditemukan, baris ditolak
	 *     supaya hierarki tidak rusak.</li>
	 * <li>Setiap baris diproses dalam transaksi sendiri: satu baris bermasalah tidak
	 *     membatalkan baris lain yang sudah benar (pola sama dgn posting per transaksi).</li>
	 * <li>Penulisan lewat session Hibernate agar tetap ter-audit Envers seperti layar ZK.</li>
	 * </ul>
	 */
	public static void akunImpor(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		JSONArray baris = request == null ? null : request.optJSONArray("baris");
		if (baris == null || baris.length() == 0) {
			tolak(hasil, "Tidak ada baris untuk diimpor.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		int dibuat = 0, diperbarui = 0, ditolak = 0;
		JSONArray masalah = new JSONArray();
		try {
			for (int i = 0; i < baris.length(); i++) {
				JSONObject b = baris.optJSONObject(i);
				if (b == null) {
					continue;
				}
				String kode = b.optString("kode", "").trim();
				String nama = b.optString("nama", "").trim();
				int nomorBaris = i + 2; // +2: baris 1 = judul kolom di Excel
				if (kode.isEmpty() || nama.isEmpty()) {
					ditolak++;
					masalah.put("Baris " + nomorBaris + ": kode dan nama akun wajib diisi");
					continue;
				}
				try {
					ais.database.model.akunting.Akun akun = (ais.database.model.akunting.Akun) session
							.createCriteria(ais.database.model.akunting.Akun.class)
							.add(org.hibernate.criterion.Restrictions.eq("kode", kode)).uniqueResult();
					boolean baru = akun == null;
					if (baru) {
						akun = new ais.database.model.akunting.Akun();
						akun.setKode(kode);
					}
					akun.setNama(nama);
					if (b.has("keterangan")) {
						akun.setKeterangan(b.optString("keterangan", "").trim());
					}
					String posisi = b.optString("posisi", "").trim().toLowerCase();
					if (posisi.startsWith("d")) {
						akun.setDebetCredit(Integer.valueOf(1));
					} else if (posisi.startsWith("c") || posisi.startsWith("k")) {
						akun.setDebetCredit(Integer.valueOf(2));
					}
					String kodeParent = b.optString("kodeParent", "").trim();
					if (!kodeParent.isEmpty()) {
						if (kodeParent.equals(kode)) {
							throw new IllegalStateException("induk tidak boleh dirinya sendiri");
						}
						ais.database.model.akunting.Akun induk = (ais.database.model.akunting.Akun) session
								.createCriteria(ais.database.model.akunting.Akun.class)
								.add(org.hibernate.criterion.Restrictions.eq("kode", kodeParent)).uniqueResult();
						if (induk == null) {
							throw new IllegalStateException("kode induk \"" + kodeParent + "\" tidak ditemukan");
						}
						akun.setParent(induk);
					}
					String grup = b.optString("grupAkun", "").trim();
					if (!grup.isEmpty()) {
						ais.database.model.akunting.GrupAkun ga = (ais.database.model.akunting.GrupAkun) session
								.createCriteria(ais.database.model.akunting.GrupAkun.class)
								.add(org.hibernate.criterion.Restrictions.eq("nama", grup)).uniqueResult();
						if (ga != null) {
							akun.setGrupAkun(ga);
						}
					}
					if (tbmuser != null) {
						akun.setOleh(tbmuser.getUserNama());
						akun.setOlehId(tbmuser.getUserId());
					}
					session.beginTransaction();
					session.saveOrUpdate(akun);
					session.getTransaction().commit();
					if (baru) {
						dibuat++;
					} else {
						diperbarui++;
					}
				} catch (Exception ex) {
					try {
						if (session.getTransaction() != null && session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
					} catch (Exception eRb) {
						ais.common.ErrorAuditUtil.record(eRb, "auto-audit KodeAkunApiHelper.akunImpor rollback");
					}
					ditolak++;
					if (masalah.length() < 50) {
						masalah.put("Baris " + nomorBaris + " (" + kode + "): " + ex.getMessage());
					}
				}
			}
			hasil.put("status", "00");
			hasil.put("dibuat", dibuat);
			hasil.put("diperbarui", diperbarui);
			hasil.put("ditolak", ditolak);
			hasil.put("masalah", masalah);
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
		if ("kode_akun_impor".equals(action)) {
			akunImpor(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
