package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.ProsesTransitori;
import ais.database.model.akunting.Transitori;
import ais.ui.util.WaktuUtil;

/**
 * <h3>API JSON "Proses Transitori" untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan {@code ais.action.master.akunting.ProsesTransitoriAction} — jalan KELUAR
 * dari rekening transitori.</p>
 *
 * <p><b>Di mana letaknya pada alur.</b> Pada modul Proses Transfer, tiap baris DPC ditandai
 * <b>Transfer</b> (langsung ke penerima) atau <b>Transitori</b> (mampir dulu di rekening
 * transitori). Baris yang ditandai Transitori melahirkan satu catatan
 * {@link Transitori}. Catatan itulah yang dikumpulkan di sini ke dalam satu
 * {@link ProsesTransitori}; begitu batch-nya disetujui, dananya dianggap keluar dari
 * rekening transitori dan barisnya siap dijurnal
 * ({@code PostingProsesTransitoriAction} menuntut {@code prosesTransitori.disetujuiOleh}
 * tidak null).</p>
 *
 * <p><b>Kenapa modul ini perlu.</b> {@code pengadaan_transitori_*} yang sudah ada
 * <b>hanya</b> melayani transitori milik pembayaran pengadaan — komentarnya menyatakan
 * sendiri bahwa transitori dari modul lain "bukan urusan layar ini". Akibatnya setiap baris
 * Keuangan yang ditandai Transitori tersangkut tanpa jalan keluar di POS.</p>
 *
 * <p><b>Satu penjaga yang tidak ada di layar ZK.</b> Catatan transitori hanya boleh
 * dimasukkan ke batch bila proses transfernya <b>sudah direalisasikan</b> — memindahkan dana
 * keluar dari rekening transitori sebelum dananya masuk ke sana tidak punya arti, dan
 * jurnalnya akan mengkredit akun transitori yang saldonya belum pernah bertambah.
 * Kandidatnya tetap DITAMPILKAN beserta alasannya, tidak disembunyikan, supaya
 * penggunanya tahu apa yang harus diselesaikan lebih dulu.</p>
 *
 * <p><b>Jebakan yang tercatat.</b> {@code Transitori.getTransfer()} mengembalikan
 * {@code true} tanpa syarat (logika aslinya dikomentari di entitasnya). Kriteria
 * {@code transfer = true} pada mesin posting karena itu selalu terpenuhi dan bukan gerbang
 * yang sesungguhnya — gerbangnya adalah persetujuan batch. Jangan menyimpulkan sebaliknya
 * dari nama kolomnya.</p>
 */
public final class ProsesTransitoriApiHelper {

	private static final String KUNCI = "proses_transitori";

	private ProsesTransitoriApiHelper() {
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

	private static String nama(Tbmuser u) {
		return u == null ? "" : (u.getUserNama() == null ? "" : u.getUserNama());
	}

	/** Dua status yang berbeda perlakuannya: Draft masih boleh disunting, Disetujui terkunci. */
	private static String statusDokumen(ProsesTransitori p) {
		return p != null && p.getDisetujuiOleh() != null ? "Disetujui" : "Draft";
	}

	// ============================================================ opsi

	public static void opsi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		JSONArray status = new JSONArray();
		status.put("Draft");
		status.put("Disetujui");
		hasil.put("status", "00");
		hasil.put("daftarStatus", status);
		hasil.put("hak", hakAksesJson(tbmuser));
		hasil.put("catatanAlur",
				"Catatan transitori lahir dari baris DPC yang ditandai Transitori pada Proses "
						+ "Transfer. Batch yang disetujui di sini menandai dananya keluar dari "
						+ "rekening transitori dan membuat barisnya siap dijurnal.");
	}

	// ============================================================ daftar

	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		String statusFilter = request == null ? "" : request.optString("statusFilter", "").trim();
		Date dari = tanggal(request, "dari");
		Date sampai = tanggal(request, "sampai");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder sql = new StringBuilder(
					"SELECT p.id, COALESCE(p.nama,''), COALESCE(p.keterangan,''), COALESCE(p.nilai,0),"
							+ " p.tanggal_pembuatan, du.usernama, p.tanggal_persetujuan,"
							+ " (SELECT count(*) FROM akunting.transitori t WHERE t.proses_transitori = p.id),"
							+ " (SELECT count(*) FROM akunting.transitori t"
							+ "  WHERE t.proses_transitori = p.id AND t.posting_history IS NOT NULL)"
							+ " FROM akunting.proses_transitori p"
							+ " LEFT JOIN tbmuser du ON du.userid = p.disetujui_oleh"
							+ " WHERE COALESCE(p.aktif,true)");
			if (!cari.isEmpty()) {
				sql.append(" AND (COALESCE(p.nama,'') ILIKE ? OR COALESCE(p.keterangan,'') ILIKE ?)");
			}
			if (dari != null) {
				sql.append(" AND date(p.tanggal_pembuatan) >= ?");
			}
			if (sampai != null) {
				sql.append(" AND date(p.tanggal_pembuatan) <= ?");
			}
			if ("Draft".equals(statusFilter)) {
				sql.append(" AND p.disetujui_oleh IS NULL");
			} else if ("Disetujui".equals(statusFilter)) {
				sql.append(" AND p.disetujui_oleh IS NOT NULL");
			}
			sql.append(" ORDER BY p.id DESC LIMIT 300");

			PreparedStatement ps = session.connection().prepareStatement(sql.toString());
			int k = 1;
			if (!cari.isEmpty()) {
				String kw = "%" + cari + "%";
				ps.setString(k++, kw);
				ps.setString(k++, kw);
			}
			if (dari != null) {
				ps.setDate(k++, new java.sql.Date(dari.getTime()));
			}
			if (sampai != null) {
				ps.setDate(k++, new java.sql.Date(sampai.getTime()));
			}
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			double total = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				j.put("keterangan", rs.getString(3));
				double nilai = rs.getDouble(4);
				j.put("nilai", nilai);
				total += nilai;
				j.put("tanggalPembuatan", teksTanggal(rs.getTimestamp(5)));
				String setuju = rs.getString(6);
				j.put("disetujuiOleh", setuju == null ? "" : setuju);
				j.put("tanggalPersetujuan", teksTanggal(rs.getTimestamp(7)));
				j.put("jumlahItem", rs.getLong(8));
				long sudahJurnal = rs.getLong(9);
				j.put("sudahDijurnal", sudahJurnal);
				j.put("statusDokumen", setuju != null ? "Disetujui" : "Draft");
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("totalNilai", total);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ detail

	public static void detail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		if (id == 0) {
			tolak(hasil, "Proses transitori belum dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ProsesTransitori p = (ProsesTransitori) session.get(ProsesTransitori.class, Long.valueOf(id));
			if (p == null) {
				tolak(hasil, "Proses transitori tidak ditemukan.");
				return;
			}
			JSONObject h = new JSONObject();
			h.put("id", p.getId());
			h.put("nama", p.getNama() == null ? "" : p.getNama());
			h.put("keterangan", p.getKeterangan() == null ? "" : p.getKeterangan());
			h.put("nilai", p.getNilai() == null ? 0 : p.getNilai().doubleValue());
			h.put("tanggalPembuatan", teksTanggal(p.getTanggalPembuatan()));
			h.put("disetujuiOleh", nama(p.getDisetujuiOleh()));
			h.put("tanggalPersetujuan", teksTanggal(p.getTanggalPersetujuan()));
			h.put("statusDokumen", statusDokumen(p));

			hasil.put("status", "00");
			hasil.put("header", h);
			hasil.put("item", baris(session, " t.proses_transitori = ?", Long.valueOf(id)));
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Baris transitori beserta konteks yang menentukan boleh-tidaknya diproses:
	 * dokumen DPC-nya, proses transfernya, dan apakah transfernya sudah cair.
	 */
	private static JSONArray baris(Session session, String syarat, Long param) throws Exception {
		PreparedStatement ps = session.connection().prepareStatement(
				"SELECT t.id, COALESCE(t.kode,''), COALESCE(t.nama,''),"
						+ " COALESCE(d.nominal,0), COALESCE(pt.kode,''), COALESCE(pt.nama,''),"
						+ " pt.realisasikan_oleh, pt.disetujui_oleh, s.nama,"
						+ " t.posting_history, COALESCE(c.nama,''), ka.kode, ka.nama"
						+ " FROM akunting.transitori t"
						+ " LEFT JOIN akunting.daftar_pengajuan_transfer d ON d.id = t.daftar_pengajuan_transfer_id"
						+ " LEFT JOIN akunting.proses_transfer pt ON pt.id = d.proses_transfer"
						+ " LEFT JOIN akunting.cara_pembayaran_transfer c ON c.id = pt.cara_pembayaran_transfer"
						+ " LEFT JOIN akunting.akun ka ON ka.id = c.akun_transitori"
						+ " LEFT JOIN rab.satuan_kerja s ON s.id = d.satuan_kerja"
						+ " WHERE COALESCE(t.aktif,true) AND" + syarat
						+ " ORDER BY t.id DESC LIMIT 500");
		// param null berarti syaratnya memang tidak berparameter (daftar kandidat).
		if (param != null) {
			ps.setLong(1, param.longValue());
		}
		ResultSet rs = ps.executeQuery();
		JSONArray arr = new JSONArray();
		while (rs.next()) {
			JSONObject j = new JSONObject();
			j.put("id", rs.getLong(1));
			j.put("kode", rs.getString(2));
			j.put("nama", rs.getString(3));
			j.put("nominal", rs.getDouble(4));
			j.put("prosesTransferKode", rs.getString(5));
			j.put("prosesTransferNama", rs.getString(6));
			boolean cair = rs.getString(7) != null;
			boolean disetujui = rs.getString(8) != null;
			j.put("transferCair", cair);
			j.put("satuanKerja", rs.getString(9) == null ? "" : rs.getString(9));
			j.put("sudahDijurnal", rs.getObject(10) != null);
			j.put("caraPembayaran", rs.getString(11));
			String kodeAkun = rs.getString(12);
			j.put("akunTransitori", kodeAkun == null ? ""
					: (kodeAkun + " " + (rs.getString(13) == null ? "" : rs.getString(13))).trim());
			// Alasan ditulis di sini, bukan disembunyikan: pengguna perlu tahu apa yang
			// harus diselesaikan lebih dulu sebelum barisnya dapat diproses.
			j.put("siap", cair);
			if (!cair) {
				j.put("alasan", disetujui
						? "Proses transfernya sudah disetujui tetapi belum direalisasikan; "
								+ "dananya belum masuk rekening transitori."
						: "Proses transfernya belum disetujui, apalagi direalisasikan.");
			}
			if (kodeAkun == null) {
				j.put("peringatan", "Cara pembayarannya belum memetakan Akun Transitori; "
						+ "barisnya tidak akan terjurnal.");
			}
			arr.put(j);
		}
		rs.close();
		ps.close();
		return arr;
	}

	// ============================================================ kandidat

	/** Catatan transitori yang belum masuk batch mana pun. */
	public static void kandidat(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray arr = baris(session, " t.proses_transitori IS NULL", null);
			String cari = request == null ? "" : request.optString("cari", "").trim().toLowerCase();
			boolean hanyaSiap = request != null && request.optBoolean("hanyaSiap", false);
			JSONArray saring = new JSONArray();
			double total = 0;
			int belumSiap = 0;
			for (int i = 0; i < arr.length(); i++) {
				JSONObject j = arr.getJSONObject(i);
				if (!j.optBoolean("siap")) {
					belumSiap++;
					if (hanyaSiap) {
						continue;
					}
				}
				if (!cari.isEmpty()) {
					String gabung = (j.optString("kode") + " " + j.optString("nama") + " "
							+ j.optString("prosesTransferKode")).toLowerCase();
					if (gabung.indexOf(cari) < 0) {
						continue;
					}
				}
				total += j.optDouble("nominal", 0);
				saring.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", saring);
			hasil.put("totalNilai", total);
			hasil.put("belumSiap", belumSiap);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ simpan

	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id == 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak membuat proses transitori."
					: "Anda tidak memiliki hak mengubah proses transitori.");
			return;
		}
		String judul = request.optString("nama", "").trim();
		if (judul.isEmpty()) {
			tolak(hasil, "Judul Proses Transitori wajib diisi.");
			return;
		}
		JSONArray transitoriIds = request.optJSONArray("transitoriIds");
		if (transitoriIds == null || transitoriIds.length() == 0) {
			tolak(hasil, "Pilih minimal satu catatan transitori yang akan diproses.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ProsesTransitori p = baru ? new ProsesTransitori()
					: (ProsesTransitori) session.get(ProsesTransitori.class, Long.valueOf(id));
			if (p == null) {
				tolak(hasil, "Proses transitori tidak ditemukan.");
				return;
			}
			if (!baru && p.getDisetujuiOleh() != null) {
				tolak(hasil, "Proses transitori ini sudah disetujui; batalkan persetujuannya "
						+ "lebih dulu bila isinya perlu diubah.");
				return;
			}

			Date tglBuat = tanggal(request, "tanggalPembuatan");
			if (tglBuat == null) {
				tglBuat = p.getTanggalPembuatan() == null ? WaktuUtil.getDate() : p.getTanggalPembuatan();
			}

			session.beginTransaction();
			p.setNama(judul);
			p.setKeterangan(request.optString("keterangan", "").trim());
			p.setTanggalPembuatan(tglBuat);
			p.setAktif(Boolean.TRUE);
			if (tbmuser != null) {
				p.setOleh(tbmuser.getUserNama());
				p.setOlehId(tbmuser.getUserId());
			}
			session.saveOrUpdate(p);
			session.flush();

			java.util.Set<Long> pilih = new java.util.LinkedHashSet<Long>();
			for (int i = 0; i < transitoriIds.length(); i++) {
				long v = transitoriIds.optLong(i, 0);
				if (v != 0) {
					pilih.add(Long.valueOf(v));
				}
			}

			// Yang tadinya menempel tetapi tidak lagi dipilih DILEPASKAN, supaya tidak
			// nyangkut selamanya di batch ini.
			@SuppressWarnings("unchecked")
			java.util.List<Transitori> terpasang = session.createCriteria(Transitori.class)
					.add(Restrictions.eq("prosesTransitori", p)).list();
			for (int i = 0; i < terpasang.size(); i++) {
				Transitori tr = terpasang.get(i);
				if (!pilih.contains(tr.getId())) {
					tr.setProsesTransitori(null);
					session.update(tr);
				}
			}

			double total = 0;
			StringBuilder belumSiap = new StringBuilder();
			for (java.util.Iterator<Long> it = pilih.iterator(); it.hasNext();) {
				Long v = it.next();
				Transitori tr = (Transitori) session.get(Transitori.class, v);
				if (tr == null) {
					continue;
				}
				if (tr.getProsesTransitori() != null
						&& !tr.getProsesTransitori().getId().equals(p.getId())) {
					throw new IllegalStateException("Catatan " + tr.getNama()
							+ " sudah masuk proses transitori lain.");
				}
				// Penjaga yang tidak ada di layar ZK: dana belum masuk rekening transitori
				// bila transfernya belum direalisasikan, sehingga mengeluarkannya tidak
				// punya arti dan jurnalnya akan mengkredit akun yang belum pernah bertambah.
				if (!transferSudahCair(session, tr)) {
					if (belumSiap.length() > 0) {
						belumSiap.append(", ");
					}
					belumSiap.append(tr.getKode() == null || tr.getKode().trim().isEmpty()
							? String.valueOf(tr.getId()) : tr.getKode());
					continue;
				}
				tr.setProsesTransitori(p);
				if (tbmuser != null) {
					tr.setOleh(tbmuser.getUserNama());
					tr.setOlehId(tbmuser.getUserId());
				}
				session.update(tr);
				total += nominal(tr);
			}
			if (belumSiap.length() > 0) {
				throw new IllegalStateException("Proses transfernya belum direalisasikan untuk "
						+ belumSiap + ". Dananya belum masuk rekening transitori, jadi belum "
						+ "ada yang dapat dikeluarkan.");
			}
			p.setNilai(Double.valueOf(total));
			session.update(p);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", p.getId());
			hasil.put("nilai", total);
			hasil.put("message", baru ? "Proses transitori dibuat." : "Proses transitori diperbarui.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Proses transitori belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static double nominal(Transitori tr) {
		if (tr == null || tr.getDaftarPengajuanTransfer() == null
				|| tr.getDaftarPengajuanTransfer().getNominal() == null) {
			return 0;
		}
		return tr.getDaftarPengajuanTransfer().getNominal().doubleValue();
	}

	/** Apakah proses transfer di balik catatan ini sudah benar-benar cair. */
	private static boolean transferSudahCair(Session session, Transitori tr) {
		if (tr == null || tr.getDaftarPengajuanTransfer() == null) {
			return false;
		}
		return tr.getDaftarPengajuanTransfer().getProsesTransfer() != null
				&& tr.getDaftarPengajuanTransfer().getProsesTransfer().getRealisasikanOleh() != null;
	}

	// ============================================================ hapus

	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus proses transitori.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ProsesTransitori p = id == 0 ? null
					: (ProsesTransitori) session.get(ProsesTransitori.class, Long.valueOf(id));
			if (p == null) {
				tolak(hasil, "Proses transitori tidak ditemukan.");
				return;
			}
			if (p.getDisetujuiOleh() != null) {
				tolak(hasil, "Proses transitori ini sudah disetujui; batalkan persetujuannya lebih dulu.");
				return;
			}
			session.beginTransaction();
			// Catatan transitorinya DILEPASKAN, bukan ikut terhapus: dananya masih ada di
			// rekening transitori dan tetap perlu jalan keluar.
			@SuppressWarnings("unchecked")
			java.util.List<Transitori> nempel = session.createCriteria(Transitori.class)
					.add(Restrictions.eq("prosesTransitori", p)).list();
			for (int i = 0; i < nempel.size(); i++) {
				Transitori tr = nempel.get(i);
				tr.setProsesTransitori(null);
				session.update(tr);
			}
			session.flush();
			session.delete(p);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Proses transitori dihapus; " + nempel.size()
					+ " catatan dikembalikan ke daftar belum diproses.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Proses transitori belum dapat dihapus: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ persetujuan

	public static void setujui(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "approve")) {
			tolak(hasil, "Anda tidak memiliki hak menyetujui proses transitori.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ProsesTransitori p = id == 0 ? null
					: (ProsesTransitori) session.get(ProsesTransitori.class, Long.valueOf(id));
			if (p == null) {
				tolak(hasil, "Proses transitori tidak ditemukan.");
				return;
			}
			if (p.getDisetujuiOleh() != null) {
				tolak(hasil, "Proses transitori ini sudah disetujui.");
				return;
			}
			PreparedStatement ps = session.connection().prepareStatement(
					"SELECT count(*) FROM akunting.transitori WHERE proses_transitori = ?");
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			rs.next();
			long jumlah = rs.getLong(1);
			rs.close();
			ps.close();
			if (jumlah == 0) {
				tolak(hasil, "Proses transitori ini belum berisi satu pun catatan.");
				return;
			}
			session.beginTransaction();
			p.setDisetujuiOleh(tbmuser);
			p.setTanggalPersetujuan(WaktuUtil.getDate());
			session.update(p);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Proses transitori disetujui; " + jumlah
					+ " catatan kini siap diposting dari Draft Jurnal.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Persetujuan belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void batalSetuju(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "reject")) {
			tolak(hasil, "Anda tidak memiliki hak membatalkan persetujuan proses transitori.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ProsesTransitori p = id == 0 ? null
					: (ProsesTransitori) session.get(ProsesTransitori.class, Long.valueOf(id));
			if (p == null) {
				tolak(hasil, "Proses transitori tidak ditemukan.");
				return;
			}
			if (p.getDisetujuiOleh() == null) {
				tolak(hasil, "Proses transitori ini belum disetujui.");
				return;
			}
			// Yang sudah DIJURNAL tidak boleh dibatalkan dari sini: jurnalnya harus
			// dibatalkan lebih dulu lewat Draft Jurnal, supaya buku besar dan dokumennya
			// tidak berbeda pendapat.
			PreparedStatement ps = session.connection().prepareStatement(
					"SELECT count(*) FROM akunting.transitori"
							+ " WHERE proses_transitori = ? AND posting_history IS NOT NULL");
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			rs.next();
			long sudahJurnal = rs.getLong(1);
			rs.close();
			ps.close();
			if (sudahJurnal > 0) {
				tolak(hasil, sudahJurnal + " catatan pada proses ini sudah dijurnal. "
						+ "Batalkan postingnya lebih dulu dari Draft Jurnal.");
				return;
			}
			session.beginTransaction();
			p.setDisetujuiOleh(null);
			p.setTanggalPersetujuan(null);
			session.update(p);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Persetujuan proses transitori dibatalkan.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pembatalan persetujuan belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ lepas satu baris

	public static void lepasItem(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengubah isi proses transitori.");
			return;
		}
		long trId = request == null ? 0 : request.optLong("transitoriId", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Transitori tr = trId == 0 ? null : (Transitori) session.get(Transitori.class, Long.valueOf(trId));
			if (tr == null || tr.getProsesTransitori() == null) {
				tolak(hasil, "Catatan transitori tidak ditemukan pada proses mana pun.");
				return;
			}
			ProsesTransitori p = tr.getProsesTransitori();
			if (p.getDisetujuiOleh() != null) {
				tolak(hasil, "Proses transitori ini sudah disetujui; catatannya tidak dapat dilepas.");
				return;
			}
			session.beginTransaction();
			tr.setProsesTransitori(null);
			session.update(tr);
			session.flush();
			double sisa = 0;
			@SuppressWarnings("unchecked")
			java.util.List<Transitori> nempel = session.createCriteria(Transitori.class)
					.add(Restrictions.eq("prosesTransitori", p)).list();
			for (int i = 0; i < nempel.size(); i++) {
				sisa += nominal(nempel.get(i));
			}
			p.setNilai(Double.valueOf(sisa));
			session.update(p);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("nilai", sisa);
			hasil.put("message", "Catatan dikembalikan ke daftar belum diproses.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Catatan belum dapat dilepas: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ dasbor

	public static void dasbor(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			JSONArray kpi = new JSONArray();
			kpi.put(angka(conn, "Batch Draft",
					"SELECT count(*) FROM akunting.proses_transitori"
							+ " WHERE COALESCE(aktif,true) AND disetujui_oleh IS NULL"));
			kpi.put(angka(conn, "Batch Disetujui",
					"SELECT count(*) FROM akunting.proses_transitori"
							+ " WHERE COALESCE(aktif,true) AND disetujui_oleh IS NOT NULL"));
			kpi.put(angka(conn, "Mengendap di Transitori",
					"SELECT count(*) FROM akunting.transitori"
							+ " WHERE COALESCE(aktif,true) AND proses_transitori IS NULL"));
			kpi.put(angka(conn, "Belum Dijurnal",
					"SELECT count(*) FROM akunting.transitori t"
							+ " JOIN akunting.proses_transitori p ON p.id = t.proses_transitori"
							+ " WHERE p.disetujui_oleh IS NOT NULL AND t.posting_history IS NULL"));

			// Yang mengendap dipecah menurut SIAP atau BELUM: itulah yang membedakan
			// "menunggu dikerjakan" dari "menunggu transfernya cair dulu".
			JSONArray komposisi = new JSONArray();
			komposisi.put(titik(conn, "Siap diproses",
					"SELECT count(*) FROM akunting.transitori t"
							+ " JOIN akunting.daftar_pengajuan_transfer d ON d.id = t.daftar_pengajuan_transfer_id"
							+ " JOIN akunting.proses_transfer pt ON pt.id = d.proses_transfer"
							+ " WHERE COALESCE(t.aktif,true) AND t.proses_transitori IS NULL"
							+ " AND pt.realisasikan_oleh IS NOT NULL"));
			komposisi.put(titik(conn, "Menunggu transfer cair",
					"SELECT count(*) FROM akunting.transitori t"
							+ " LEFT JOIN akunting.daftar_pengajuan_transfer d ON d.id = t.daftar_pengajuan_transfer_id"
							+ " LEFT JOIN akunting.proses_transfer pt ON pt.id = d.proses_transfer"
							+ " WHERE COALESCE(t.aktif,true) AND t.proses_transitori IS NULL"
							+ " AND (pt.id IS NULL OR pt.realisasikan_oleh IS NULL)"));

			JSONArray daftar = new JSONArray();
			PreparedStatement ps = conn.prepareStatement(
					"SELECT COALESCE(t.kode,''), COALESCE(t.nama,''),"
							+ " COALESCE(date_part('day', now() - pt.tanggal_realisasikan),0)"
							+ " FROM akunting.transitori t"
							+ " JOIN akunting.daftar_pengajuan_transfer d ON d.id = t.daftar_pengajuan_transfer_id"
							+ " JOIN akunting.proses_transfer pt ON pt.id = d.proses_transfer"
							+ " WHERE COALESCE(t.aktif,true) AND t.proses_transitori IS NULL"
							+ " AND pt.realisasikan_oleh IS NOT NULL"
							+ " ORDER BY pt.tanggal_realisasikan LIMIT 20");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("kode", rs.getString(1));
				j.put("keterangan", rs.getString(2));
				j.put("umurHari", rs.getLong(3));
				daftar.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("kpi", kpi);
			hasil.put("komposisi", komposisi);
			hasil.put("komposisiJudul", "Catatan Mengendap di Rekening Transitori");
			hasil.put("daftar", daftar);
			hasil.put("daftarJudul", "Siap Diproses, Paling Lama Mengendap");
			hasil.put("catatanKosong", "Tidak ada dana yang mengendap di rekening transitori.");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static JSONObject angka(Connection conn, String label, String sql) throws Exception {
		PreparedStatement ps = conn.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		rs.next();
		JSONObject j = new JSONObject();
		j.put("label", label);
		j.put("nilai", rs.getLong(1));
		rs.close();
		ps.close();
		return j;
	}

	private static JSONObject titik(Connection conn, String label, String sql) throws Exception {
		return angka(conn, label, sql);
	}

	// ============================================================ dispatcher

	/** Dipakai dispatcher: seluruh aksi berawalan {@code proses_transitori_}. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("proses_transitori_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transitori_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transitori_detail".equals(action)) {
			detail(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transitori_kandidat".equals(action)) {
			kandidat(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transitori_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transitori_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transitori_setujui".equals(action)) {
			setujui(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transitori_batal_setuju".equals(action)) {
			batalSetuju(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transitori_lepas".equals(action)) {
			lepasItem(tbmuser, request, hasil);
			return true;
		}
		if ("proses_transitori_dasbor".equals(action)) {
			dasbor(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
