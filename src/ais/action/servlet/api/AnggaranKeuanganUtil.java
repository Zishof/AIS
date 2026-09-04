package ais.action.servlet.api;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.Workspace;
import ais.ui.util.WaktuUtil;

/**
 * <h3>Penggunaan anggaran untuk seluruh modul Keuangan.</h3>
 *
 * <p>Di AIS, pemotongan anggaran TIDAK ditulis oleh layarnya. Setiap kali dokumen
 * disimpan lewat Hibernate, {@code AuditListener} memanggil
 * {@code PenggunaanAnggaran.simpan(...)} yang -- beberapa detik kemudian, di thread
 * terpisah -- membuat/menyegarkan baris {@code rab.penggunaan_anggaran} secara
 * idempotent berdasarkan {@code ref}. Jadi tugas API di sini bukan menulis baris itu,
 * melainkan <b>menyediakan data yang dibutuhkannya</b>.</p>
 *
 * <p>Yang dibutuhkan berbeda per dokumen:</p>
 * <ul>
 * <li><b>Uang Muka</b> memakai kolom {@code workspace} pada dokumennya sendiri
 *     (sudah ditangani {@code UangMukaApiHelper}). Uang muka yang diambil dari PR
 *     sengaja TIDAK memotong anggaran lagi -- PR-nya sudah memotong.</li>
 * <li><b>Pertanggungjawaban</b> mewarisi anggaran dari uang muka induknya.</li>
 * <li><b>Kas Kecil / Kas Besar</b> memotong anggaran <b>per baris rincian</b>: tiap
 *     baris {@code formula} harus membawa field {@code workspace}. Baris tanpa
 *     {@code workspace} dilewati begitu saja oleh {@code prosesKasKecil}/{@code prosesKasBesar}
 *     -- itulah sebabnya rincian yang hanya berisi akun tidak pernah memotong anggaran.</li>
 * </ul>
 *
 * <p>Layar ZK menutup celah itu dengan menebak workspace dari akun biaya yang dipilih.
 * {@link #lengkapiRincian} memindahkan tebakan yang sama ke sisi server, sehingga klien
 * Desktop/Android cukup memilih akun (atau anggaran, kalau memang ingin spesifik) dan
 * pemotongan anggarannya tetap terjadi persis seperti di ZK.</p>
 */
public final class AnggaranKeuanganUtil {

	private AnggaranKeuanganUtil() {
	}

	// ============================================================ pencarian anggaran

	/**
	 * Daftar anggaran (Workspace) yang boleh dipakai: hanya yang aktif dan berupa daun,
	 * karena hanya simpul daun yang memegang pagu.
	 */
	public static void cari(JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		int tahun = request == null ? 0 : request.optInt("tahun", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			hasil.put("status", "00");
			hasil.put("data", cari(session, cari, tahun));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Bentuk yang bisa dipakai ulang bila sesi Hibernate-nya sudah terbuka. */
	public static JSONArray cari(Session session, String cari, int tahun) throws Exception {
		Connection conn = session.connection();
		StringBuilder sql = new StringBuilder(
				"SELECT w.id, COALESCE(w.kode,''), COALESCE(w.nama,''), COALESCE(w.harga_total,0),"
						+ " COALESCE(w.realisasi_total,0), COALESCE(w.tahun_workspace,0),"
						+ " COALESCE(sk.nama,''), COALESCE(a.id,0), COALESCE(a.kode,''), COALESCE(a.nama,'')"
						+ " FROM rab.workspace w"
						+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = w.satuan_kerja"
						+ " LEFT JOIN akunting.akun a ON a.id = w.akun"
						+ " WHERE COALESCE(w.aktif,true) = true"
						+ " AND NOT EXISTS (SELECT 1 FROM rab.workspace anak"
						+ " WHERE anak.parent_id = w.id AND anak.id <> w.id"
						+ " AND COALESCE(anak.aktif,true) = true)");
		if (cari != null && !cari.isEmpty()) {
			sql.append(" AND (w.nama ILIKE ? OR COALESCE(w.kode,'') ILIKE ?)");
		}
		if (tahun > 0) {
			sql.append(" AND COALESCE(w.tahun_workspace,0) = ?");
		}
		sql.append(" ORDER BY w.nama LIMIT 500");
		PreparedStatement ps = conn.prepareStatement(sql.toString());
		int i = 1;
		if (cari != null && !cari.isEmpty()) {
			String kw = "%" + cari + "%";
			ps.setString(i++, kw);
			ps.setString(i++, kw);
		}
		if (tahun > 0) {
			ps.setInt(i++, tahun);
		}
		ResultSet rs = ps.executeQuery();
		JSONArray arr = new JSONArray();
		while (rs.next()) {
			JSONObject j = new JSONObject();
			j.put("id", rs.getLong(1));
			// Bentuk teksnya ikut dikirim: id anggaran adalah bilangan 19 digit yang tidak
			// muat utuh pada tipe angka JavaScript, sedangkan formula rincian memang
			// menyimpannya sebagai teks.
			j.put("idTeks", String.valueOf(rs.getLong(1)));
			j.put("kode", rs.getString(2));
			j.put("nama", rs.getString(3));
			j.put("pagu", rs.getDouble(4));
			j.put("realisasi", rs.getDouble(5));
			j.put("tahun", rs.getInt(6));
			j.put("satuanKerja", rs.getString(7));
			j.put("akunId", rs.getLong(8));
			j.put("akunKode", rs.getString(9));
			j.put("akunNama", rs.getString(10));
			arr.put(j);
		}
		rs.close();
		ps.close();
		return arr;
	}

	// ============================================================ sisa saldo

	/**
	 * Sisa anggaran satu workspace pada satu tanggal, memakai perhitungan yang sama
	 * dengan layar ZK ({@code JenisUangMukaAction.hitungSaldo}).
	 */
	public static void saldo(JSONObject request, JSONObject hasil) throws Exception {
		// Perhatikan: id anggaran boleh NEGATIF (lihat catatan pada lengkapiRincian),
		// jadi "belum dipilih" berarti 0, bukan "kurang dari nol".
		long workspaceId = request == null ? 0 : request.optLong("workspaceId", 0);
		if (workspaceId == 0) {
			hasil.put("status", "00");
			hasil.put("saldo", 0);
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Workspace w = (Workspace) session.get(Workspace.class, Long.valueOf(workspaceId));
			if (w == null) {
				hasil.put("status", "91");
				hasil.put("description", "Anggaran tidak ditemukan.");
				return;
			}
			Date pada = tanggal(request);
			Double saldo = ais.action.master.akunting.JenisUangMukaAction.hitungSaldo(null, null, null, null, w,
					pada == null ? WaktuUtil.getDate() : pada);
			hasil.put("status", "00");
			hasil.put("saldo", saldo == null ? 0 : saldo.doubleValue());
			hasil.put("anggaran", w.getNama() == null ? "" : w.getNama());
			hasil.put("pagu", w.getHargaTotal() == null ? 0 : w.getHargaTotal().doubleValue());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static Date tanggal(JSONObject request) {
		String s = request == null ? "" : request.optString("tanggal", "").trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s.substring(0, 10));
		} catch (Exception e) {
			return null;
		}
	}

	// ============================================================ rincian kas

	/**
	 * Melengkapi tiap baris rincian dengan {@code workspace} supaya pemotongan anggaran
	 * benar-benar terjadi. Urutannya sama dengan layar ZK:
	 * <ol>
	 * <li>Kalau baris sudah membawa {@code workspace}, itu yang dipakai -- dan akun
	 *     biayanya diselaraskan dengan akun milik anggaran tersebut.</li>
	 * <li>Kalau tidak, anggaran dicari dari akun biaya baris itu untuk tahun dokumen:
	 *     pertama lewat relasi akun, lalu (kalau belum ketemu atau tidak aktif) lewat
	 *     kesamaan kode.</li>
	 * <li>Kalau tetap tidak ketemu, {@code workspace} dibiarkan kosong. Barisnya tetap
	 *     tersimpan dan tetap dijurnal, hanya saja tidak memotong anggaran -- perilaku
	 *     yang sama dengan ZK, bukan kegagalan simpan.</li>
	 * </ol>
	 *
	 * @param carryOver true untuk Kas Besar, yang juga menerima anggaran luncuran
	 *                  ({@code carryOver}) selain anggaran aktif.
	 * @return jumlah baris yang berhasil memperoleh anggaran.
	 */
	public static int lengkapiRincian(Session session, JSONArray rincian, Date tanggalDokumen, boolean carryOver)
			throws Exception {
		if (session == null || rincian == null || rincian.length() == 0) {
			return 0;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(tanggalDokumen == null ? WaktuUtil.getDate() : tanggalDokumen);
		Integer tahun = Integer.valueOf(cal.get(Calendar.YEAR));

		int terisi = 0;
		for (int i = 0; i < rincian.length(); i++) {
			JSONObject b = rincian.optJSONObject(i);
			if (b == null || b.isNull("key")) {
				continue;
			}
			Workspace workspace = null;
			if (!b.isNull("workspace")) {
				try {
					// Id anggaran pada basis data AIS bisa NEGATIF (rab.workspace memakai
					// ruang id tersendiri, bukan serial). Penjaga "> 0" akan menolak
					// seluruh anggaran yang sah -- yang menandakan "kosong" hanyalah 0.
					long id = new BigDecimal(b.get("workspace") + "").longValue();
					if (id != 0) {
						workspace = (Workspace) session.get(Workspace.class, Long.valueOf(id));
					}
				} catch (Exception e) {
					workspace = null;
				}
			}

			Akun akunBiaya = null;
			long akunId = b.optLong("akun", 0);
			if (akunId != 0) {
				akunBiaya = (Akun) session.get(Akun.class, Long.valueOf(akunId));
			}

			if (workspace != null) {
				// Anggaran yang dipilih pengguna menentukan akun biayanya, sama seperti
				// banbox ZK yang mengisi akun begitu anggaran dipilih.
				if (workspace.getAkun() != null) {
					akunBiaya = workspace.getAkun();
					b.put("akun", akunBiaya.getId());
				}
			} else if (akunBiaya != null) {
				workspace = tebakDariAkun(session, akunBiaya, tahun, carryOver);
			}

			if (workspace == null) {
				b.remove("workspace");
				b.remove("anggaranNama");
			} else {
				b.put("workspace", String.valueOf(workspace.getId()));
				b.put("anggaranNama", workspace.getNama() == null ? "" : workspace.getNama());
				terisi++;
			}
		}
		return terisi;
	}

	/** Tebakan anggaran dari akun biaya -- lewat relasi akun dulu, baru lewat kode. */
	private static Workspace tebakDariAkun(Session session, Akun akunBiaya, Integer tahun, boolean carryOver) {
		Workspace workspace = (Workspace) ConstantValues.simpleObject(
				kriteria(session, tahun, carryOver).add(Restrictions.eq("akun", akunBiaya)).addOrder(Order.desc("id"))
						.setMaxResults(1),
				Workspace.class);
		if (workspace == null || workspace.getAktif() == null || !workspace.getAktif().booleanValue()) {
			Workspace lewatKode = (Workspace) ConstantValues.simpleObject(
					kriteria(session, tahun, carryOver).add(Restrictions.eq("kode", akunBiaya.getKode()))
							.addOrder(Order.desc("id")).setMaxResults(1),
					Workspace.class);
			if (lewatKode != null) {
				workspace = lewatKode;
			}
		}
		return workspace;
	}

	private static org.hibernate.Criteria kriteria(Session session, Integer tahun, boolean carryOver) {
		org.hibernate.Criteria c = session.createCriteria(Workspace.class);
		if (carryOver) {
			c.add(Restrictions.or(Restrictions.eq("carryOver", Boolean.TRUE),
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE))));
		} else {
			c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
		}
		c.add(Restrictions.eq("tahunWorkspace", tahun));
		return c;
	}

	// ============================================================ pelepasan

	/**
	 * Mengembalikan anggaran yang dipotong satu dokumen, dengan membuang baris
	 * {@code rab.penggunaan_anggaran} miliknya.
	 *
	 * <p>Wajib dipanggil sebelum dokumennya dihapus: FK dari tabel itu ke dokumen tidak
	 * memakai {@code ON DELETE CASCADE}, jadi tanpa ini basis data menolak penghapusan.
	 * Dipanggil di dalam transaksi milik pemanggil supaya keduanya batal bersama bila
	 * penghapusan dokumennya gagal.</p>
	 *
	 * @param kolom {@code uang_muka}, {@code kas_kecil}, {@code kas_besar},
	 *              {@code pertangungjawaban}, atau {@code grup_transaksi}.
	 */
	public static void lepaskan(Session session, String kolom, Long id) throws Exception {
		if (session == null || id == null || !kolomSah(kolom)) {
			return;
		}
		PreparedStatement ps = session.connection()
				.prepareStatement("DELETE FROM rab.penggunaan_anggaran WHERE " + kolom + " = ?");
		ps.setLong(1, id.longValue());
		ps.executeUpdate();
		ps.close();
	}

	// ============================================================ ringkasan pemakaian

	/**
	 * Baris {@code rab.penggunaan_anggaran} milik satu dokumen -- dipakai layar untuk
	 * menunjukkan anggaran mana saja yang terpotong oleh dokumen ini.
	 *
	 * @param kolom nama kolom relasi: {@code uang_muka}, {@code kas_kecil}, {@code kas_besar},
	 *              {@code pertangungjawaban}, atau {@code grup_transaksi}.
	 */
	public static JSONArray pemakaianDokumen(Session session, String kolom, long id) throws Exception {
		JSONArray arr = new JSONArray();
		if (session == null || id <= 0 || !kolomSah(kolom)) {
			return arr;
		}
		PreparedStatement ps = session.connection().prepareStatement(
				"SELECT COALESCE(w.kode,''), COALESCE(w.nama,''), COALESCE(pa.nilai,0),"
						+ " COALESCE(w.harga_total,0)"
						+ " FROM rab.penggunaan_anggaran pa"
						+ " LEFT JOIN rab.workspace w ON w.id = pa.workspace"
						+ " WHERE pa." + kolom + " = ? ORDER BY w.nama");
		ps.setLong(1, id);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			JSONObject j = new JSONObject();
			j.put("kode", rs.getString(1));
			j.put("nama", rs.getString(2));
			j.put("nilai", rs.getDouble(3));
			j.put("pagu", rs.getDouble(4));
			arr.put(j);
		}
		rs.close();
		ps.close();
		return arr;
	}

	/** Nama kolom tidak boleh datang dari luar apa adanya -- hanya relasi ini yang sah. */
	private static boolean kolomSah(String kolom) {
		return "uang_muka".equals(kolom) || "kas_kecil".equals(kolom) || "kas_besar".equals(kolom)
				|| "pertangungjawaban".equals(kolom) || "grup_transaksi".equals(kolom);
	}
}
