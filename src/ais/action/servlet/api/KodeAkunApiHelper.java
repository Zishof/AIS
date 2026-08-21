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
 *
 * <h4>Hak akses: impor massal tunduk pada grid CRUD yang sama</h4>
 *
 * <p>Simpan dan hapus SATU baris bergerbang sejak awal, tetapi ketiga aksi IMPOR-nya sempat
 * menerima payload apa adanya. Akibatnya peran yang di {@code TbmroleAction} hanya diberi hak
 * MELIHAT Kode Akun tetap dapat membuat atau mengubah ratusan akun sekaligus lewat unggah
 * Excel. Gerbang di layar tidak menolong: aksinya dapat dipanggil langsung tanpa melewati
 * tombol mana pun, dan itulah sebabnya SERVER yang harus memutuskan.</p>
 *
 * <p>Pemeriksaannya dua lapis, dan keduanya perlu:</p>
 * <ul>
 * <li><b>Di muka</b> -- peran yang sama sekali tidak berhak (tidak {@code create} maupun
 *     {@code update}) ditolak sekali, bukan baris demi baris, supaya pesannya jelas dan
 *     berkas besar tidak diproses sia-sia.</li>
 * <li><b>Per baris</b> -- satu berkas impor lazim memuat CAMPURAN baris baru dan baris
 *     perubahan, sehingga hak yang diperlukan berbeda-beda di dalam satu unggahan. Peran
 *     yang boleh menambah tapi tidak boleh mengubah tetap dapat mengimpor akun baru, dan
 *     baris yang menimpa akun lama ditolak sendiri-sendiri.</li>
 * </ul>
 *
 * <p>Baris yang ditolak masuk ke ringkasan {@code masalah} seperti penolakan validasi lainnya
 * -- pengguna melihat persis baris mana yang tidak dikerjakan, bukan sekadar "gagal".
 * Kunci yang dipakai mengikuti menu masing-masing tab: {@code kode_akun}, {@code bank_akun},
 * dan {@code jenis_transaksi}.</p>
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
			hasil.put("hak", hakAksesJson(tbmuser, "kode_akun"));
			// Kode anak bawaan = kode induk + nol sebanyak ini (padanan akun_lenght pada layar ZK).
			hasil.put("panjangKodeAnak", panjangKodeAnak());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Opsi Grup Akun (dipakai form Akun di klien). */
	public static void grupAkunDaftar(Tbmuser tbmuser, JSONObject hasil) throws Exception {
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
			hasil.put("hak", hakAksesJson(tbmuser, "grup_akun"));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Daftar Bank + akun kasnya (tab "Bank" pada layar ZK). */
	public static void bankDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT b.id, COALESCE(b.nama,''), COALESCE(b.keterangan,''), COALESCE(b.aktif,true),"
							+ " b.akun, COALESCE(a.kode,''), COALESCE(a.nama,''), COALESCE(b.kode,'')"
							+ " FROM public.bank b LEFT JOIN akunting.akun a ON a.id = b.akun");
			if (!cari.isEmpty()) {
				sql.append(" WHERE (b.nama ILIKE ? OR COALESCE(b.kode,'') ILIKE ?)");
			}
			sql.append(" ORDER BY b.nama ASC");
			PreparedStatement ps = conn.prepareStatement(sql.toString());
			if (!cari.isEmpty()) {
				ps.setString(1, "%" + cari + "%");
				ps.setString(2, "%" + cari + "%");
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
				j.put("kode", rs.getString(8));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("hak", hakAksesJson(tbmuser, "bank_akun"));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Daftar Jenis Transaksi + akun terkait (tab "Jenis Transaksi" pada layar ZK). */
	public static void jenisTransaksiDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
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
			hasil.put("hak", hakAksesJson(tbmuser, "jenis_transaksi"));
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
	 * <li>Hak akses diperiksa di muka DAN per baris ({@code kode_akun}) -- lihat penjelasan
	 *     pada Javadoc kelas: satu berkas bisa memuat campuran baris baru dan perubahan.</li>
	 * </ul>
	 */
	public static void akunImpor(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		JSONArray baris = request == null ? null : request.optJSONArray("baris");
		if (baris == null || baris.length() == 0) {
			tolak(hasil, "Tidak ada baris untuk diimpor.");
			return;
		}
		if (!bolehAksi(tbmuser, "kode_akun", "create") && !bolehAksi(tbmuser, "kode_akun", "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengimpor akun.");
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
					if (!bolehAksi(tbmuser, "kode_akun", baru ? "create" : "update")) {
						ditolak++;
						masalah.put("Baris " + nomorBaris + ": hak akses tidak mengizinkan "
								+ (baru ? "menambah" : "mengubah") + " akun");
						continue;
					}
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

	/** Cari akun berdasar KODE (dipakai impor Bank & Jenis Transaksi). */
	private static ais.database.model.akunting.Akun akunByKode(Session session, String kodeAkun) {
		if (kodeAkun == null || kodeAkun.trim().isEmpty()) {
			return null;
		}
		return (ais.database.model.akunting.Akun) session
				.createCriteria(ais.database.model.akunting.Akun.class)
				.add(org.hibernate.criterion.Restrictions.eq("kode", kodeAkun.trim())).uniqueResult();
	}

	/** Rollback yang tidak pernah melempar (dipakai jalur impor per baris). */
	private static void batalkanDiam(Session session) {
		try {
			if (session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit KodeAkunApiHelper.batalkanDiam");
		}
	}

	/**
	 * Impor Bank dari Excel: {@code baris: [{nama, keterangan, kodeAkun, aktif}]}.
	 * Dicocokkan lewat NAMA (entitas Bank tidak punya kolom kode). Aturan pengaman
	 * sama dgn impor akun: buat/perbarui saja (tidak pernah menghapus), baris tanpa
	 * nama ditolak, kode akun tak ditemukan ditolak, tiap baris transaksi sendiri.
	 * Hak akses memakai kunci {@code bank_akun}, diperiksa di muka dan per baris.
	 */
	public static void bankImpor(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		JSONArray baris = request == null ? null : request.optJSONArray("baris");
		if (baris == null || baris.length() == 0) {
			tolak(hasil, "Tidak ada baris untuk diimpor.");
			return;
		}
		if (!bolehAksi(tbmuser, "bank_akun", "create") && !bolehAksi(tbmuser, "bank_akun", "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengimpor bank.");
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
				int nomorBaris = i + 2;
				String nama = b.optString("nama", "").trim();
				String kodeBank = b.optString("kode", "").trim();
				if (nama.isEmpty()) {
					ditolak++;
					masalah.put("Baris " + nomorBaris + ": nama bank wajib diisi");
					continue;
				}
				try {
					// Cocokkan lewat KODE bila diisi (konsisten dgn Akun & Jenis Transaksi);
					// baris lama yang belum punya kode tetap dikenali lewat NAMA.
					ais.database.model.Bank bank = null;
					if (!kodeBank.isEmpty()) {
						bank = (ais.database.model.Bank) session
								.createCriteria(ais.database.model.Bank.class)
								.add(org.hibernate.criterion.Restrictions.eq("kode", kodeBank))
								.setMaxResults(1).uniqueResult();
					}
					if (bank == null) {
						bank = (ais.database.model.Bank) session
								.createCriteria(ais.database.model.Bank.class)
								.add(org.hibernate.criterion.Restrictions.eq("nama", nama))
								.setMaxResults(1).uniqueResult();
					}
					boolean baru = bank == null;
					if (!bolehAksi(tbmuser, "bank_akun", baru ? "create" : "update")) {
						ditolak++;
						masalah.put("Baris " + nomorBaris + ": hak akses tidak mengizinkan "
								+ (baru ? "menambah" : "mengubah") + " bank");
						continue;
					}
					if (baru) {
						bank = new ais.database.model.Bank();
					}
					bank.setNama(nama);
					if (!kodeBank.isEmpty()) {
						bank.setKode(kodeBank);
					}
					if (b.has("keterangan")) {
						bank.setKeterangan(b.optString("keterangan", "").trim());
					}
					String kodeAkun = b.optString("kodeAkun", "").trim();
					if (!kodeAkun.isEmpty()) {
						ais.database.model.akunting.Akun ak = akunByKode(session, kodeAkun);
						if (ak == null) {
							throw new IllegalStateException("kode akun " + kodeAkun + " tidak ditemukan");
						}
						bank.setAkun(ak);
					}
					String aktif = b.optString("aktif", "").trim().toLowerCase();
					if (!aktif.isEmpty()) {
						bank.setAktif(Boolean.valueOf(aktif.startsWith("y") || aktif.startsWith("t")
								|| aktif.equals("1")));
					}
					if (tbmuser != null) {
						bank.setOleh(tbmuser.getUserNama());
						bank.setOlehId(tbmuser.getUserId());
					}
					session.beginTransaction();
					session.saveOrUpdate(bank);
					session.getTransaction().commit();
					if (baru) {
						dibuat++;
					} else {
						diperbarui++;
					}
				} catch (Exception ex) {
					batalkanDiam(session);
					ditolak++;
					if (masalah.length() < 50) {
						masalah.put("Baris " + nomorBaris + " (" + nama + "): " + ex.getMessage());
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

	/**
	 * Impor Jenis Transaksi dari Excel:
	 * {@code baris: [{kode, nama, keterangan, kodeAkun, aktif}]}. Dicocokkan lewat KODE.
	 * Hak akses memakai kunci {@code jenis_transaksi}, diperiksa di muka dan per baris.
	 */
	public static void jenisTransaksiImpor(Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		JSONArray baris = request == null ? null : request.optJSONArray("baris");
		if (baris == null || baris.length() == 0) {
			tolak(hasil, "Tidak ada baris untuk diimpor.");
			return;
		}
		if (!bolehAksi(tbmuser, "jenis_transaksi", "create") && !bolehAksi(tbmuser, "jenis_transaksi", "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengimpor jenis transaksi.");
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
				int nomorBaris = i + 2;
				String kode = b.optString("kode", "").trim();
				String nama = b.optString("nama", "").trim();
				if (kode.isEmpty() || nama.isEmpty()) {
					ditolak++;
					masalah.put("Baris " + nomorBaris + ": kode dan nama jenis transaksi wajib diisi");
					continue;
				}
				try {
					ais.database.model.akunting.JenisTransaksi jt =
							(ais.database.model.akunting.JenisTransaksi) session
									.createCriteria(ais.database.model.akunting.JenisTransaksi.class)
									.add(org.hibernate.criterion.Restrictions.eq("kode", kode)).uniqueResult();
					boolean baru = jt == null;
					if (!bolehAksi(tbmuser, "jenis_transaksi", baru ? "create" : "update")) {
						ditolak++;
						masalah.put("Baris " + nomorBaris + ": hak akses tidak mengizinkan "
								+ (baru ? "menambah" : "mengubah") + " jenis transaksi");
						continue;
					}
					if (baru) {
						jt = new ais.database.model.akunting.JenisTransaksi();
						jt.setKode(kode);
					}
					jt.setNama(nama);
					if (b.has("keterangan")) {
						jt.setKeterangan(b.optString("keterangan", "").trim());
					}
					String kodeAkun = b.optString("kodeAkun", "").trim();
					if (!kodeAkun.isEmpty()) {
						ais.database.model.akunting.Akun ak = akunByKode(session, kodeAkun);
						if (ak == null) {
							throw new IllegalStateException("kode akun " + kodeAkun + " tidak ditemukan");
						}
						jt.setAkun(ak);
					}
					String aktif = b.optString("aktif", "").trim().toLowerCase();
					if (!aktif.isEmpty()) {
						jt.setAktif(Boolean.valueOf(aktif.startsWith("y") || aktif.startsWith("t")
								|| aktif.equals("1")));
					}
					if (tbmuser != null) {
						jt.setOleh(tbmuser.getUserNama());
						jt.setOlehId(tbmuser.getUserId());
					}
					session.beginTransaction();
					session.saveOrUpdate(jt);
					session.getTransaction().commit();
					if (baru) {
						dibuat++;
					} else {
						diperbarui++;
					}
				} catch (Exception ex) {
					batalkanDiam(session);
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

	// ==================================================================== CRUD

	/** Nama kelas panjang dipakai berulang; alias supaya badan method tetap terbaca. */
	private static ais.database.model.akunting.Akun akunById(Session session, long id) {
		return id <= 0 ? null
				: (ais.database.model.akunting.Akun) session
						.get(ais.database.model.akunting.Akun.class, Long.valueOf(id));
	}

	/**
	 * Gerbang aksi granular per menu -- padanan grid CRUD pada {@code TbmroleAction}.
	 *
	 * <p>Server yang menjadi gerbang sebenarnya: klien boleh menyembunyikan tombol, tapi
	 * permintaan yang tetap dikirim akan ditolak di sini. Admin global boleh; pengguna tanpa
	 * role dianggap boleh supaya akun lama tidak mendadak kehilangan akses.</p>
	 */
	private static boolean bolehAksi(Tbmuser tbmuser, String kunciMenu, String aksi) {
		if (ais.common.Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		ais.database.model.Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		// Satu aturan untuk seluruh kelompok Akuntansi: kotak CRUD yang sudah diatur admin
		// menang, sedangkan yang BELUM PERNAH diatur mengikuti visibilitas menunya.
		return ais.common.EbisnisMenuKatalog.bolehAksiAkuntansi(role.getEbisnisMenu(),
				role.getRoleId(), kunciMenu, aksi);
	}

	/** Hak tombol yang ikut pada tiap balasan daftar supaya klien tidak menebak-nebak. */
	private static JSONObject hakAksesJson(Tbmuser tbmuser, String kunciMenu) throws Exception {
		JSONObject j = new JSONObject();
		j.put("create", bolehAksi(tbmuser, kunciMenu, "create"));
		j.put("update", bolehAksi(tbmuser, kunciMenu, "update"));
		j.put("delete", bolehAksi(tbmuser, kunciMenu, "delete"));
		return j;
	}

	/** Panjang digit tambahan kode akun anak (system property {@code akun_lenght}, bawaan 2). */
	static int panjangKodeAnak() {
		try {
			Object p = System.getProperties().get("akun_lenght");
			if (p != null) {
				int n = Integer.parseInt(p.toString().trim());
				if (n > 0 && n < 10) {
					return n;
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit KodeAkunApiHelper.panjangKodeAnak");
		}
		return 2;
	}

	/** true bila {@code calonInduk} ternyata keturunan dari akun ber-id {@code id} (cegah lingkaran). */
	private static boolean keturunanDari(ais.database.model.akunting.Akun calonInduk, long id) {
		ais.database.model.akunting.Akun p = calonInduk;
		int pagar = 0;
		while (p != null && pagar++ < 50) {
			if (p.getId() != null && p.getId().longValue() == id) {
				return true;
			}
			p = p.getParent();
		}
		return false;
	}

	private static void batalkan(Session session) {
		batalkanDiam(session);
	}

	/**
	 * Simpan (tambah/ubah) satu Akun. Urutan validasinya SAMA dengan {@code AkunAction.onSave}:
	 * kode wajib, nama wajib, debet/kredit wajib, grup akun wajib, lalu kode harus unik.
	 *
	 * <p>Tombol "Copy" dan "Tambah Anak" di klien memakai aksi yang sama; bedanya hanya nilai
	 * awal formulir (kode induk + nol sebanyak {@link #panjangKodeAnak()} untuk anak).</p>
	 */
	public static void akunSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id <= 0;
		if (!bolehAksi(tbmuser, "kode_akun", baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah akun."
					: "Anda tidak memiliki hak mengubah akun.");
			return;
		}
		String kode = request.optString("kode", "").trim();
		String nama = request.optString("nama", "").trim();
		if (kode.isEmpty()) {
			tolak(hasil, "Kode Akun belum diisi.");
			return;
		}
		if (nama.isEmpty()) {
			tolak(hasil, "Nama Akun belum diisi.");
			return;
		}
		int dc = request.optInt("debetCredit", 0);
		if (dc != 1 && dc != 2) {
			tolak(hasil, "Debet / Credit belum dipilih.");
			return;
		}
		long grupId = request.optLong("grupAkunId", 0);
		if (grupId <= 0) {
			tolak(hasil, "Grup Akun belum dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria cek = session.createCriteria(ais.database.model.akunting.Akun.class)
					.add(org.hibernate.criterion.Restrictions.eq("kode", kode));
			if (!baru) {
				cek.add(org.hibernate.criterion.Restrictions.ne("id", Long.valueOf(id)));
			}
			if (cek.setMaxResults(1).uniqueResult() != null) {
				tolak(hasil, "Kode Akun \"" + kode + "\" sudah dipakai akun lain.");
				return;
			}
			ais.database.model.akunting.Akun akun = baru ? new ais.database.model.akunting.Akun()
					: akunById(session, id);
			if (akun == null) {
				tolak(hasil, "Akun yang diubah tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			ais.database.model.akunting.GrupAkun grup = (ais.database.model.akunting.GrupAkun) session
					.get(ais.database.model.akunting.GrupAkun.class, Long.valueOf(grupId));
			if (grup == null) {
				tolak(hasil, "Grup Akun tidak ditemukan.");
				return;
			}
			ais.database.model.akunting.Akun induk = null;
			long parentId = request.optLong("parentId", 0);
			if (parentId > 0) {
				if (!baru && parentId == id) {
					tolak(hasil, "Induk tidak boleh akun itu sendiri.");
					return;
				}
				induk = akunById(session, parentId);
				if (induk == null) {
					tolak(hasil, "Akun induk tidak ditemukan.");
					return;
				}
				if (!baru && keturunanDari(induk, id)) {
					tolak(hasil, "Induk tidak boleh akun turunannya sendiri (hierarki akan melingkar).");
					return;
				}
			}
			akun.setKode(kode);
			akun.setNama(nama);
			akun.setKeterangan(request.optString("keterangan", "").trim());
			akun.setDebetCredit(Integer.valueOf(dc));
			akun.setGrupAkun(grup);
			akun.setParent(induk);
			String aktifitas = request.optString("aktifitas", "").trim();
			akun.setAktifitas(aktifitas.isEmpty() ? null : aktifitas);
			long bankId = request.optLong("bankId", 0);
			akun.setBank(bankId > 0
					? (ais.database.model.Bank) session.get(ais.database.model.Bank.class, Long.valueOf(bankId))
					: null);
			akun.setAtasNama(request.optString("atasNama", "").trim());
			akun.setNoRek(request.optString("noRek", "").trim());
			if (tbmuser != null) {
				akun.setOleh(tbmuser.getUserNama());
				akun.setOlehId(tbmuser.getUserId());
			}
			session.beginTransaction();
			session.saveOrUpdate(akun);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", akun.getId());
			hasil.put("message", baru ? "Akun berhasil ditambahkan." : "Akun berhasil diperbarui.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Akun belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Hapus satu Akun. Pengaman disamakan dengan layar ZK (tombol hapus di sana hanya muncul
	 * untuk node tanpa anak), ditambah pengaman yang tidak ada di ZK: akun yang SUDAH DIPAKAI
	 * jurnal tidak boleh hilang karena buku besarnya akan kehilangan induk.
	 */
	public static void akunHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "kode_akun", "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus akun.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		if (id <= 0) {
			tolak(hasil, "Akun yang dihapus belum dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.akunting.Akun akun = akunById(session, id);
			if (akun == null) {
				tolak(hasil, "Akun tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			Number anak = (Number) session.createCriteria(ais.database.model.akunting.Akun.class)
					.add(org.hibernate.criterion.Restrictions.eq("parent", akun))
					.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			if (anak != null && anak.longValue() > 0) {
				tolak(hasil, "Akun ini masih punya " + anak.longValue()
						+ " akun turunan. Hapus atau pindahkan turunannya lebih dulu.");
				return;
			}
			Number dipakai = (Number) session
					.createSQLQuery("SELECT count(*) FROM akunting.transaksi WHERE akun = :id")
					.setLong("id", id).uniqueResult();
			if (dipakai != null && dipakai.longValue() > 0) {
				tolak(hasil, "Akun ini sudah dipakai " + dipakai.longValue()
						+ " baris jurnal sehingga tidak boleh dihapus. Nonaktifkan pemakaiannya saja.");
				return;
			}
			session.beginTransaction();
			session.delete(akun);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Akun \"" + akun.getKode() + " - " + akun.getNama() + "\" dihapus.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Akun tidak dapat dihapus karena masih berelasi dengan data lain.");
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Simpan (tambah/ubah) satu Bank beserta akun kas/banknya. */
	public static void bankSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id <= 0;
		if (!bolehAksi(tbmuser, "bank_akun", baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah bank." : "Anda tidak memiliki hak mengubah bank.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama Bank belum diisi.");
			return;
		}
		String kode = request.optString("kode", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			if (!kode.isEmpty()) {
				org.hibernate.Criteria cek = session.createCriteria(ais.database.model.Bank.class)
						.add(org.hibernate.criterion.Restrictions.eq("kode", kode));
				if (!baru) {
					cek.add(org.hibernate.criterion.Restrictions.ne("id", Long.valueOf(id)));
				}
				if (cek.setMaxResults(1).uniqueResult() != null) {
					tolak(hasil, "Kode Bank \"" + kode + "\" sudah dipakai bank lain.");
					return;
				}
			}
			ais.database.model.Bank bank = baru ? new ais.database.model.Bank()
					: (ais.database.model.Bank) session.get(ais.database.model.Bank.class, Long.valueOf(id));
			if (bank == null) {
				tolak(hasil, "Bank yang diubah tidak ditemukan.");
				return;
			}
			bank.setKode(kode.isEmpty() ? null : kode);
			bank.setNama(nama);
			bank.setKeterangan(request.optString("keterangan", "").trim());
			bank.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			long akunId = request.optLong("akunId", 0);
			bank.setAkun(akunId > 0 ? akunById(session, akunId) : null);
			if (tbmuser != null) {
				bank.setOleh(tbmuser.getUserNama());
				bank.setOlehId(tbmuser.getUserId());
			}
			session.beginTransaction();
			session.saveOrUpdate(bank);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", bank.getId());
			hasil.put("message", baru ? "Bank berhasil ditambahkan." : "Bank berhasil diperbarui.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Bank belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Hapus satu Bank. */
	public static void bankHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "bank_akun", "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus bank.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.Bank bank = id <= 0 ? null
					: (ais.database.model.Bank) session.get(ais.database.model.Bank.class, Long.valueOf(id));
			if (bank == null) {
				tolak(hasil, "Bank tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(bank);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Bank \"" + bank.getNama() + "\" dihapus.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Bank tidak dapat dihapus karena masih dipakai data lain (mis. akun atau rekening).");
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Simpan (tambah/ubah) satu Jenis Transaksi. */
	public static void jenisTransaksiSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id <= 0;
		if (!bolehAksi(tbmuser, "jenis_transaksi", baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah jenis transaksi."
					: "Anda tidak memiliki hak mengubah jenis transaksi.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama Jenis Transaksi belum diisi.");
			return;
		}
		String kode = request.optString("kode", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			if (!kode.isEmpty()) {
				org.hibernate.Criteria cek = session
						.createCriteria(ais.database.model.akunting.JenisTransaksi.class)
						.add(org.hibernate.criterion.Restrictions.eq("kode", kode));
				if (!baru) {
					cek.add(org.hibernate.criterion.Restrictions.ne("id", Long.valueOf(id)));
				}
				if (cek.setMaxResults(1).uniqueResult() != null) {
					tolak(hasil, "Kode \"" + kode + "\" sudah dipakai jenis transaksi lain.");
					return;
				}
			}
			ais.database.model.akunting.JenisTransaksi jt = baru
					? new ais.database.model.akunting.JenisTransaksi()
					: (ais.database.model.akunting.JenisTransaksi) session
							.get(ais.database.model.akunting.JenisTransaksi.class, Long.valueOf(id));
			if (jt == null) {
				tolak(hasil, "Jenis transaksi yang diubah tidak ditemukan.");
				return;
			}
			jt.setKode(kode.isEmpty() ? null : kode);
			jt.setNama(nama);
			jt.setKeterangan(request.optString("keterangan", "").trim());
			jt.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			long akunId = request.optLong("akunId", 0);
			jt.setAkun(akunId > 0 ? akunById(session, akunId) : null);
			if (tbmuser != null) {
				jt.setOleh(tbmuser.getUserNama());
				jt.setOlehId(tbmuser.getUserId());
			}
			session.beginTransaction();
			session.saveOrUpdate(jt);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", jt.getId());
			hasil.put("message", baru ? "Jenis transaksi berhasil ditambahkan."
					: "Jenis transaksi berhasil diperbarui.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Jenis transaksi belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Hapus satu Jenis Transaksi. */
	public static void jenisTransaksiHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if (!bolehAksi(tbmuser, "jenis_transaksi", "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus jenis transaksi.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.akunting.JenisTransaksi jt = id <= 0 ? null
					: (ais.database.model.akunting.JenisTransaksi) session
							.get(ais.database.model.akunting.JenisTransaksi.class, Long.valueOf(id));
			if (jt == null) {
				tolak(hasil, "Jenis transaksi tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(jt);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Jenis transaksi \"" + jt.getNama() + "\" dihapus.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Jenis transaksi tidak dapat dihapus karena masih dipakai transaksi lain.");
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Simpan (tambah/ubah) satu Grup Akun. */
	public static void grupAkunSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id <= 0;
		if (!bolehAksi(tbmuser, "grup_akun", baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah grup akun."
					: "Anda tidak memiliki hak mengubah grup akun.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama Grup Akun belum diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria cek = session.createCriteria(ais.database.model.akunting.GrupAkun.class)
					.add(org.hibernate.criterion.Restrictions.eq("nama", nama));
			if (!baru) {
				cek.add(org.hibernate.criterion.Restrictions.ne("id", Long.valueOf(id)));
			}
			if (cek.setMaxResults(1).uniqueResult() != null) {
				tolak(hasil, "Grup Akun \"" + nama + "\" sudah ada.");
				return;
			}
			ais.database.model.akunting.GrupAkun ga = baru ? new ais.database.model.akunting.GrupAkun()
					: (ais.database.model.akunting.GrupAkun) session
							.get(ais.database.model.akunting.GrupAkun.class, Long.valueOf(id));
			if (ga == null) {
				tolak(hasil, "Grup akun yang diubah tidak ditemukan.");
				return;
			}
			ga.setNama(nama);
			ga.setKeterangan(request.optString("keterangan", "").trim());
			if (tbmuser != null) {
				ga.setOleh(tbmuser.getUserNama());
				ga.setOlehId(tbmuser.getUserId());
			}
			session.beginTransaction();
			session.saveOrUpdate(ga);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", ga.getId());
			hasil.put("message", baru ? "Grup akun berhasil ditambahkan." : "Grup akun berhasil diperbarui.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Grup akun belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Hapus satu Grup Akun -- ditolak bila masih ada akun yang memakainya. */
	public static void grupAkunHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "grup_akun", "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus grup akun.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.akunting.GrupAkun ga = id <= 0 ? null
					: (ais.database.model.akunting.GrupAkun) session
							.get(ais.database.model.akunting.GrupAkun.class, Long.valueOf(id));
			if (ga == null) {
				tolak(hasil, "Grup akun tidak ditemukan.");
				return;
			}
			Number dipakai = (Number) session.createCriteria(ais.database.model.akunting.Akun.class)
					.add(org.hibernate.criterion.Restrictions.eq("grupAkun", ga))
					.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
			if (dipakai != null && dipakai.longValue() > 0) {
				tolak(hasil, "Grup akun ini masih dipakai " + dipakai.longValue()
						+ " akun. Pindahkan akun-akun itu ke grup lain lebih dulu.");
				return;
			}
			session.beginTransaction();
			session.delete(ga);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Grup akun \"" + ga.getNama() + "\" dihapus.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Grup akun tidak dapat dihapus karena masih berelasi dengan data lain.");
			hasil.put("teknis", e.toString());
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
			grupAkunDaftar(tbmuser, hasil);
			return true;
		}
		if ("kode_akun_bank".equals(action)) {
			bankDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_jenis_transaksi".equals(action)) {
			jenisTransaksiDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_impor".equals(action)) {
			akunImpor(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_bank_impor".equals(action)) {
			bankImpor(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_jenis_transaksi_impor".equals(action)) {
			jenisTransaksiImpor(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_simpan".equals(action)) {
			akunSimpan(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_hapus".equals(action)) {
			akunHapus(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_bank_simpan".equals(action)) {
			bankSimpan(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_bank_hapus".equals(action)) {
			bankHapus(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_jenis_transaksi_simpan".equals(action)) {
			jenisTransaksiSimpan(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_jenis_transaksi_hapus".equals(action)) {
			jenisTransaksiHapus(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_grup_simpan".equals(action)) {
			grupAkunSimpan(tbmuser, request, hasil);
			return true;
		}
		if ("kode_akun_grup_hapus".equals(action)) {
			grupAkunHapus(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
