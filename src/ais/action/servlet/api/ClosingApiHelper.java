package ais.action.servlet.api;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Closing;
import ais.ui.util.WaktuUtil;

/**
 * <h3>API JSON "Closing" (penutupan periode akuntansi) untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan {@code ais.action.master.akunting.ClosingAction}.</p>
 *
 * <p><b>Kenapa modul ini perlu.</b> Closing-lah yang mengunci buku. Setiap mesin
 * pembatalan posting yang dipakai POS menolak baris yang sudah masuk closing
 * ({@code delete ... where ... closing is null}), dan dasbor Draft Jurnal menampilkan
 * kolom <b>Closing</b> per modul. Artinya pengguna POS sudah melihat angkanya dan sudah
 * terkena akibatnya, tetapi <b>menutup periode hanya bisa dari layar ZK</b>.</p>
 *
 * <p><b>Cara kerjanya, sama persis dengan ZK.</b> Satu {@link Closing} adalah tanggal
 * batas berikut namanya. Seluruh {@code akunting.grup_transaksi} yang tanggal
 * transaksinya pada atau sebelum tanggal itu ditautkan padanya. Penautannya dihitung
 * ulang dari <b>closing terbaru ke terlama</b>, sehingga tiap jurnal berakhir pada
 * closing PALING AWAL yang mencakupnya — bukan yang terakhir dibuat.</p>
 *
 * <p><b>Penjaga yang dibawa dari ZK:</b></p>
 * <ol>
 * <li><b>Tanggal closing wajib unik.</b> Dua closing bertanggal sama membuat penautannya
 *     tidak menentu.</li>
 * <li><b>Tidak boleh ada jurnal yang tidak balance</b> pada atau sebelum tanggal itu.
 *     Menutup periode yang memuat jurnal timpang berarti mengunci kesalahan sehingga
 *     tidak dapat diperbaiki lagi. Kode jurnalnya disebut dalam pesannya.</li>
 * </ol>
 *
 * <p><b>Dua penjaga tambahan yang tidak ada di layar ZK:</b> closing yang sudah
 * <b>dikunci</b> tidak dapat diubah maupun dihapus, dan menghapus closing
 * <b>melepaskan</b> jurnalnya lebih dulu ({@code closing = NULL}) alih-alih
 * meninggalkannya menunjuk baris yang sudah tiada.</p>
 */
public final class ClosingApiHelper {

	private static final String KUNCI = "closing";

	private ClosingApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static boolean bolehAksi(Tbmuser tbmuser, String aksi) {
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		return EbisnisMenuKatalog.bolehAksi(EbisnisMenuKatalog.urai(role.getEbisnisMenu()), KUNCI, aksi);
	}

	private static JSONObject hakAksesJson(Tbmuser tbmuser) throws Exception {
		JSONObject j = new JSONObject();
		j.put("create", bolehAksi(tbmuser, "create"));
		j.put("update", bolehAksi(tbmuser, "update"));
		j.put("delete", bolehAksi(tbmuser, "delete"));
		j.put("approve", bolehAksi(tbmuser, "approve"));
		j.put("reject", bolehAksi(tbmuser, "reject"));
		return j;
	}

	private static void batalkanDiam(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			// rollback gagal: kegagalan aslinya yang dilaporkan ke pemanggil
		}
	}

	private static Date tanggal(JSONObject request, String kunci) {
		String s = request == null ? "" : request.optString(kunci, "").trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s.substring(0, 10));
		} catch (Exception e) {
			return null;
		}
	}

	private static String teksTanggal(Date t) {
		return t == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(t);
	}

	// ============================================================ daftar

	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder sql = new StringBuilder(
					"SELECT c.id, COALESCE(c.nama,''), COALESCE(c.keterangan,''), c.tanggal,"
							+ " du.usernama,"
							+ " (SELECT count(*) FROM akunting.grup_transaksi g WHERE g.closing = c.id),"
							+ " (SELECT COALESCE(sum(g.total_debet),0) FROM akunting.grup_transaksi g"
							+ "  WHERE g.closing = c.id)"
							+ " FROM akunting.closing c"
							+ " LEFT JOIN tbmuser du ON du.userid = c.dikunci"
							+ " WHERE 1 = 1");
			if (!cari.isEmpty()) {
				sql.append(" AND (COALESCE(c.nama,'') ILIKE ? OR COALESCE(c.keterangan,'') ILIKE ?)");
			}
			sql.append(" ORDER BY c.tanggal DESC LIMIT 300");
			PreparedStatement ps = session.connection().prepareStatement(sql.toString());
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
				j.put("nama", rs.getString(2));
				j.put("keterangan", rs.getString(3));
				j.put("tanggal", teksTanggal(rs.getTimestamp(4)));
				String kunci = rs.getString(5);
				j.put("dikunciOleh", kunci == null ? "" : kunci);
				j.put("terkunci", kunci != null);
				j.put("jumlahJurnal", rs.getLong(6));
				j.put("nilaiDebet", rs.getDouble(7));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void opsi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		hasil.put("status", "00");
		hasil.put("hak", hakAksesJson(tbmuser));
		hasil.put("catatanAlur",
				"Closing menautkan seluruh jurnal bertanggal transaksi pada atau sebelum tanggal "
						+ "batasnya. Jurnal yang sudah masuk closing tidak dapat dibatalkan postingnya.");
	}

	// ============================================================ periksa kesiapan

	/**
	 * Kesiapan satu tanggal untuk ditutup, TANPA menyimpan apa pun: berapa jurnal yang
	 * akan tertaut, dan adakah jurnal yang tidak balance.
	 *
	 * <p>Dipisah sebagai aksi tersendiri supaya layar dapat memperingatkan sebelum
	 * penggunanya menekan Simpan — penolakan yang baru muncul setelah menekan tombol
	 * membuat orang mengira dirinya salah tekan, bukan salah tanggal.</p>
	 */
	public static void periksa(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Date tgl = tanggal(request, "tanggal");
		if (tgl == null) {
			tolak(hasil, "Tanggal closing belum diisi.");
			return;
		}
		long kecuali = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			hasil.put("status", "00");
			hasil.put("tanggal", teksTanggal(tgl));
			hasil.put("jumlahJurnal", hitungJurnal(session, tgl));
			String timpang = jurnalTidakBalance(session, tgl);
			hasil.put("adaTidakBalance", timpang != null);
			if (timpang != null) {
				hasil.put("jurnalTidakBalance", timpang);
				hasil.put("peringatan", "Jurnal \"" + timpang + "\" tidak balance (debet tidak sama "
						+ "dengan kredit). Perbaiki dulu — menutup periode yang memuatnya berarti "
						+ "mengunci kesalahan itu sehingga tidak dapat diperbaiki lagi.");
			}
			String bentrok = tanggalTerpakai(session, tgl, kecuali);
			hasil.put("tanggalTerpakai", bentrok != null);
			if (bentrok != null) {
				hasil.put("closingLain", bentrok);
				hasil.put("peringatanTanggal", "Tanggal ini sudah dipakai closing \"" + bentrok + "\".");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static long hitungJurnal(Session session, Date tgl) throws Exception {
		PreparedStatement ps = session.connection().prepareStatement(
				"SELECT count(*) FROM akunting.grup_transaksi WHERE date(tanggal_transaksi) <= ?");
		ps.setDate(1, new java.sql.Date(tgl.getTime()));
		ResultSet rs = ps.executeQuery();
		rs.next();
		long n = rs.getLong(1);
		rs.close();
		ps.close();
		return n;
	}

	/** Kode jurnal pertama yang tidak balance pada atau sebelum tanggal itu, atau null. */
	private static String jurnalTidakBalance(Session session, Date tgl) throws Exception {
		PreparedStatement ps = session.connection().prepareStatement(
				"SELECT COALESCE(kode,'(tanpa kode)') FROM akunting.grup_transaksi"
						+ " WHERE date(tanggal_transaksi) <= ?"
						+ "   AND COALESCE(total_debet,0) <> COALESCE(total_kredit,0)"
						+ " ORDER BY tanggal_transaksi LIMIT 1");
		ps.setDate(1, new java.sql.Date(tgl.getTime()));
		ResultSet rs = ps.executeQuery();
		String kode = rs.next() ? rs.getString(1) : null;
		rs.close();
		ps.close();
		return kode;
	}

	/** Nama closing lain yang sudah memakai tanggal itu, atau null. */
	private static String tanggalTerpakai(Session session, Date tgl, long kecuali) throws Exception {
		PreparedStatement ps = session.connection().prepareStatement(
				"SELECT COALESCE(nama,'(tanpa nama)') FROM akunting.closing"
						+ " WHERE date(tanggal) = ? AND id <> ? LIMIT 1");
		ps.setDate(1, new java.sql.Date(tgl.getTime()));
		ps.setLong(2, kecuali);
		ResultSet rs = ps.executeQuery();
		String nama = rs.next() ? rs.getString(1) : null;
		rs.close();
		ps.close();
		return nama;
	}

	// ============================================================ simpan

	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id == 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak membuat closing."
					: "Anda tidak memiliki hak mengubah closing.");
			return;
		}
		// Urutan validasinya disamakan dengan ClosingAction.onSave.
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama Closing wajib diisi.");
			return;
		}
		Date tgl = tanggal(request, "tanggal");
		if (tgl == null) {
			tolak(hasil, "Tanggal Closing wajib diisi.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Closing c = baru ? new Closing() : (Closing) session.get(Closing.class, Long.valueOf(id));
			if (c == null) {
				tolak(hasil, "Closing tidak ditemukan.");
				return;
			}
			if (!baru && c.getDikunci() != null) {
				tolak(hasil, "Closing \"" + c.getNama() + "\" sudah dikunci sehingga tidak boleh diubah.");
				return;
			}
			String bentrok = tanggalTerpakai(session, tgl, baru ? 0 : id);
			if (bentrok != null) {
				tolak(hasil, "Tanggal itu sudah dipakai closing \"" + bentrok + "\". Dua closing "
						+ "bertanggal sama membuat penautan jurnalnya tidak menentu.");
				return;
			}
			String timpang = jurnalTidakBalance(session, tgl);
			if (timpang != null) {
				tolak(hasil, "Jurnal \"" + timpang + "\" tidak balance (debet tidak sama dengan "
						+ "kredit). Perbaiki dulu — menutup periode yang memuatnya berarti mengunci "
						+ "kesalahan itu sehingga tidak dapat diperbaiki lagi.");
				return;
			}

			session.beginTransaction();
			c.setNama(nama);
			c.setKeterangan(request.optString("keterangan", "").trim());
			c.setTanggal(tgl);
			if (tbmuser != null) {
				c.setOleh(tbmuser.getUserNama());
				c.setOlehId(tbmuser.getUserId());
			}
			session.saveOrUpdate(c);
			session.flush();
			int tertaut = tautkanUlang(session);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", c.getId());
			hasil.put("message", (baru ? "Closing " + nama + " dibuat" : "Closing " + nama + " diperbarui")
					+ "; " + tertaut + " jurnal ditautkan ulang.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Closing belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Menautkan ulang seluruh jurnal ke closing-nya, persis seperti
	 * {@code ClosingAction.reload()}.
	 *
	 * <p>Ditelusuri dari closing TERBARU ke TERLAMA. Karena tiap langkah menimpa
	 * penautan sebelumnya untuk rentangnya sendiri, jurnal berakhir pada closing PALING
	 * AWAL yang mencakupnya — itulah periode yang sebenarnya menutupnya. Membalik
	 * urutannya akan menaruh semua jurnal lama pada closing terbaru.</p>
	 */
	private static int tautkanUlang(Session session) throws Exception {
		// Lepas dulu SELURUH penautan lama, sama seperti ClosingAction.reload(): tanpa
		// ini, memundurkan tanggal sebuah closing tidak pernah melepas jurnal yang
		// telanjur tertaut, karena UPDATE di bawah tidak pernah menulis NULL.
		PreparedStatement lepas = session.connection().prepareStatement(
				"UPDATE akunting.grup_transaksi SET closing = NULL WHERE closing IS NOT NULL");
		lepas.executeUpdate();
		lepas.close();

		PreparedStatement ps = session.connection().prepareStatement(
				"SELECT id, tanggal FROM akunting.closing ORDER BY tanggal DESC");
		ResultSet rs = ps.executeQuery();
		JSONArray urut = new JSONArray();
		while (rs.next()) {
			JSONObject j = new JSONObject();
			j.put("id", rs.getLong(1));
			j.put("tanggal", rs.getTimestamp(2) == null ? "" : teksTanggal(rs.getTimestamp(2)));
			urut.put(j);
		}
		rs.close();
		ps.close();

		int total = 0;
		for (int i = 0; i < urut.length(); i++) {
			JSONObject j = urut.getJSONObject(i);
			if (j.optString("tanggal").isEmpty()) {
				continue;
			}
			PreparedStatement up = session.connection().prepareStatement(
					"UPDATE akunting.grup_transaksi SET closing = ?"
							+ " WHERE date(tanggal_transaksi) <= ?");
			up.setLong(1, j.optLong("id"));
			up.setDate(2, java.sql.Date.valueOf(j.optString("tanggal")));
			total += up.executeUpdate();
			up.close();
		}
		return total;
	}

	// ============================================================ hapus

	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus closing.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Closing c = id == 0 ? null : (Closing) session.get(Closing.class, Long.valueOf(id));
			if (c == null) {
				tolak(hasil, "Closing tidak ditemukan.");
				return;
			}
			if (c.getDikunci() != null) {
				tolak(hasil, "Closing \"" + c.getNama() + "\" sudah dikunci sehingga tidak boleh dihapus. "
						+ "Buka kuncinya lebih dulu bila memang perlu dibatalkan.");
				return;
			}
			session.beginTransaction();
			// Jurnalnya DILEPASKAN lebih dulu, bukan dibiarkan menunjuk baris yang sudah
			// tiada. Membukanya kembali memang konsekuensi yang dimaksud: periode itu
			// tidak lagi tertutup.
			PreparedStatement up = session.connection().prepareStatement(
					"UPDATE akunting.grup_transaksi SET closing = NULL WHERE closing = ?");
			up.setLong(1, id);
			int dilepas = up.executeUpdate();
			up.close();
			String nama = c.getNama();
			session.delete(c);
			session.flush();
			// Sisa closing lain menautkan ulang jurnal yang masih dalam rentangnya.
			int tertaut = tautkanUlang(session);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Closing " + nama + " dihapus; " + dilepas + " jurnal dilepas, "
					+ tertaut + " ditautkan ulang ke closing lain yang masih berlaku.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Closing belum dapat dihapus: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ kunci / buka

	/**
	 * Mengunci / membuka kunci satu closing.
	 *
	 * <p><b>Sengaja memakai SQL langsung, bukan entitasnya.</b>
	 * {@code Closing.getDikunci()} adalah getter YANG MENULIS BALIK fieldnya
	 * ({@code dikunci = check(dikunci)}). Saat Hibernate memanggilnya di tengah flush,
	 * penulisan itu mengubah konteks persistensi yang sedang ditelusuri, dan
	 * {@code session.update()} atas closing meledak dengan
	 * {@code ConcurrentModificationException} — terbukti di harness, bukan dugaan.
	 * Kolomnya sendiri hanya FK ke {@code tbmuser}, jadi satu UPDATE menyatakan
	 * maksudnya dengan tepat tanpa menyentuh getter itu sama sekali.</p>
	 */
	public static void kunci(Tbmuser tbmuser, JSONObject request, JSONObject hasil, boolean pasang)
			throws Exception {
		if (!bolehAksi(tbmuser, pasang ? "approve" : "reject")) {
			tolak(hasil, pasang ? "Anda tidak memiliki hak mengunci closing."
				: "Anda tidak memiliki hak membuka kunci closing.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Keadaannya pun dibaca lewat SQL, dengan alasan yang sama.
			PreparedStatement ps = session.connection().prepareStatement(
					"SELECT COALESCE(nama,''), dikunci FROM akunting.closing WHERE id = ?");
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			boolean ada = rs.next();
			String nama = ada ? rs.getString(1) : "";
			String pemegang = ada ? rs.getString(2) : null;
			rs.close();
			ps.close();
			if (!ada) {
				tolak(hasil, "Closing tidak ditemukan.");
				return;
			}
			if (pasang && pemegang != null) {
				tolak(hasil, "Closing ini sudah dikunci.");
				return;
			}
			if (!pasang && pemegang == null) {
				tolak(hasil, "Closing ini belum dikunci.");
				return;
			}
			if (pasang && (tbmuser == null || tbmuser.getUserId() == null)) {
				tolak(hasil, "Sesi pengguna tidak dikenali, silakan masuk ulang.");
				return;
			}
			session.beginTransaction();
			PreparedStatement up = session.connection().prepareStatement(
					"UPDATE akunting.closing SET dikunci = ? WHERE id = ?");
			if (pasang) {
				up.setString(1, tbmuser.getUserId());
			} else {
				up.setNull(1, java.sql.Types.VARCHAR);
			}
			up.setLong(2, id);
			up.executeUpdate();
			up.close();
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", pasang
				? ("Closing " + nama + " dikunci; isinya tidak dapat diubah maupun dihapus lagi.")
				: ("Kunci closing " + nama + " dibuka."));
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Kunci closing belum dapat diubah: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ isi satu closing

	/** Jurnal yang tertaut pada satu closing; dipakai panel rincian. */
	public static void jurnal(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		if (id == 0) {
			tolak(hasil, "Closing belum dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PreparedStatement ps = session.connection().prepareStatement(
					"SELECT g.id, COALESCE(g.kode,''), COALESCE(g.keterangan,''), g.tanggal_transaksi,"
							+ " COALESCE(g.total_debet,0), COALESCE(g.total_kredit,0)"
							+ " FROM akunting.grup_transaksi g WHERE g.closing = ?"
							+ " ORDER BY g.tanggal_transaksi DESC, g.id DESC LIMIT 500");
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			double debet = 0;
			double kredit = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("keterangan", rs.getString(3));
				j.put("tanggal", teksTanggal(rs.getTimestamp(4)));
				double d = rs.getDouble(5);
				double k = rs.getDouble(6);
				j.put("debet", d);
				j.put("kredit", k);
				// Ditandai di sini supaya jurnal timpang yang TERLANJUR tertutup tetap
				// terlihat, bukan tersembunyi di balik total yang kelihatan rapi.
				j.put("seimbang", Math.abs(d - k) < 0.005);
				debet += d;
				kredit += k;
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("totalDebet", debet);
			hasil.put("totalKredit", kredit);
			hasil.put("seimbang", Math.abs(debet - kredit) < 0.005);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ dispatcher

	/** Dipakai dispatcher: seluruh aksi berawalan {@code closing_}. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("closing_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("closing_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("closing_periksa".equals(action)) {
			periksa(tbmuser, request, hasil);
			return true;
		}
		if ("closing_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("closing_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		if ("closing_kunci".equals(action)) {
			kunci(tbmuser, request, hasil, true);
			return true;
		}
		if ("closing_buka".equals(action)) {
			kunci(tbmuser, request, hasil, false);
			return true;
		}
		if ("closing_jurnal".equals(action)) {
			jurnal(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
