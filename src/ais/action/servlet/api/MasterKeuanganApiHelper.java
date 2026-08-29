package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.CaraPembayaranTransfer;
import ais.database.model.akunting.JenisKasBesar;
import ais.database.model.akunting.JenisKasKecil;
import ais.database.model.akunting.JenisPengeluaran;
import ais.database.model.akunting.JenisReimbursement;
import ais.database.model.akunting.JenisUangMuka;
import ais.database.model.rab.SatuanKerja;

/**
 * <h3>API JSON "Master Keuangan" untuk POS Desktop/Android.</h3>
 *
 * <p>Enam data master yang menjadi tulang punggung seluruh grup Keuangan, dan selama ini
 * HANYA dapat dipelihara dari layar ZK:</p>
 *
 * <table border="1">
 * <tr><th>Tipe</th><th>Dipakai oleh</th><th>Akun yang dipetakan</th></tr>
 * <tr><td>{@code jenis_uang_muka}</td><td>Uang Muka, LPJ, Dana Talangan</td>
 *     <td>akun penerima, akun kelebihan, akun sponsor</td></tr>
 * <tr><td>{@code jenis_kas_kecil}</td><td>Kas Kecil, Penggantian Kas Kecil</td>
 *     <td>akun kas kecil, akun penutup</td></tr>
 * <tr><td>{@code jenis_kas_besar}</td><td>Kas Besar, LPJ Kas Besar</td>
 *     <td>akun kas besar, akun penerima</td></tr>
 * <tr><td>{@code jenis_reimbursement}</td><td>Reimbursement Pegawai</td>
 *     <td>akun biaya (bila tanpa anggaran)</td></tr>
 * <tr><td>{@code jenis_pengeluaran}</td><td>rincian Reimbursement</td>
 *     <td>akun biaya per baris</td></tr>
 * <tr><td>{@code cara_pembayaran_transfer}</td><td>Proses Transfer, posting jurnal</td>
 *     <td>akun kas/bank, akun transitori</td></tr>
 * </table>
 *
 * <p><b>Kenapa ini penting.</b> Pemetaan akun di sinilah yang menentukan jurnal setiap
 * dokumen Keuangan. Tanpa akun yang lengkap, dokumen tetap bisa diajukan dan disetujui
 * tetapi <b>dilewati begitu saja</b> oleh mesin posting — gejala yang sulit dilacak karena
 * tidak ada pesan galat di dokumennya. Karena itu daftar di sini ikut menandai jenis yang
 * akunnya belum lengkap ({@code akunLengkap}), supaya admin melihat masalahnya sebelum
 * pengguna menemukannya.</p>
 *
 * <p><b>Penghapusan dijaga.</b> Jenis yang sudah dipakai dokumen tidak dapat dihapus —
 * bukan sekadar karena FK menolak, tetapi karena menghapusnya akan memutus riwayat
 * dokumen lama dari akunnya. Jumlah pemakaiannya disebutkan dalam pesan.</p>
 */
public final class MasterKeuanganApiHelper {

	private static final String KUNCI = "master_keuangan";

	private MasterKeuanganApiHelper() {
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

	// ============================================================ definisi tipe

	/**
	 * Enam tipe yang dikenal. Nama tabel dan kolom TIDAK PERNAH datang dari luar apa
	 * adanya -- hanya tipe pada daftar ini yang diterima, dan setiap SQL di bawah memakai
	 * nama yang tertulis di kode.
	 */
	private static boolean tipeSah(String tipe) {
		return "jenis_uang_muka".equals(tipe) || "jenis_kas_kecil".equals(tipe)
				|| "jenis_kas_besar".equals(tipe) || "jenis_reimbursement".equals(tipe)
				|| "jenis_pengeluaran".equals(tipe) || "cara_pembayaran_transfer".equals(tipe);
	}

	private static String label(String tipe) {
		if ("jenis_uang_muka".equals(tipe)) {
			return "Jenis Uang Muka";
		}
		if ("jenis_kas_kecil".equals(tipe)) {
			return "Jenis Kas Kecil";
		}
		if ("jenis_kas_besar".equals(tipe)) {
			return "Jenis Kas Besar";
		}
		if ("jenis_reimbursement".equals(tipe)) {
			return "Jenis Reimbursement";
		}
		if ("jenis_pengeluaran".equals(tipe)) {
			return "Jenis Pengeluaran";
		}
		return "Cara Pembayaran Transfer";
	}

	/**
	 * Kolom akun yang berarti untuk tiap tipe, beserta labelnya di layar.
	 *
	 * <p>Kuncinya SENGAJA posisional ({@code akunId}, {@code akunKeduaId},
	 * {@code akunKetigaId}) dan sama persis dengan kunci pada daftar maupun simpan,
	 * sehingga layar cukup menggambar medan yang disebutkan di sini tanpa memetakan
	 * nama semantik per tipe. Yang berbeda antar tipe hanya LABEL-nya.</p>
	 */
	private static JSONArray kolomAkun(String tipe) throws Exception {
		JSONArray a = new JSONArray();
		if ("jenis_uang_muka".equals(tipe)) {
			a.put(medan("akunId", "Akun Penerima", true));
			a.put(medan("akunKeduaId", "Akun Kelebihan", true));
			a.put(medan("akunKetigaId", "Akun Sponsor", false));
		} else if ("jenis_kas_kecil".equals(tipe)) {
			a.put(medan("akunId", "Akun Kas Kecil", true));
			a.put(medan("akunKeduaId", "Akun Penutup Kas Kecil", false));
		} else if ("jenis_kas_besar".equals(tipe)) {
			a.put(medan("akunId", "Akun Kas Besar", true));
			a.put(medan("akunKeduaId", "Akun Penerima", true));
		} else if ("jenis_reimbursement".equals(tipe)) {
			a.put(medan("akunId", "Akun Biaya", false));
		} else if ("jenis_pengeluaran".equals(tipe)) {
			a.put(medan("akunId", "Akun Biaya", true));
		} else {
			a.put(medan("akunId", "Akun Kas/Bank", true));
			a.put(medan("akunKeduaId", "Akun Transitori", false));
		}
		return a;
	}

	private static JSONObject medan(String kunci, String label, boolean wajib) throws Exception {
		JSONObject j = new JSONObject();
		j.put("kunci", kunci);
		j.put("label", label);
		// "Wajib" di sini berarti: tanpa akun ini, dokumen yang memakainya TIDAK akan
		// terjurnal. Penyimpanan tetap diizinkan supaya admin dapat melengkapi bertahap,
		// tetapi daftarnya menandainya sebagai belum lengkap.
		j.put("wajibUntukJurnal", wajib);
		return j;
	}

	// ============================================================ daftar

	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String tipe = request == null ? "" : request.optString("tipe", "").trim();
		String cari = request == null ? "" : request.optString("cari", "").trim();
		if (!tipeSah(tipe)) {
			tolak(hasil, "Tipe master keuangan tidak dikenali.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			String sql;
			if ("jenis_uang_muka".equals(tipe)) {
				sql = "SELECT m.id, COALESCE(m.kode,''), COALESCE(m.nama,''), COALESCE(m.keterangan,''),"
						+ " COALESCE(m.aktif,true), m.akun, m.akun_kelebihan, m.akun_sponsor, CAST(NULL AS bigint),"
						+ " (SELECT count(*) FROM public.uang_muka x WHERE x.jenis_uang_muka = m.id)"
						+ " , m.satuan_kerja FROM public.jenis_uang_muka m";
			} else if ("jenis_kas_kecil".equals(tipe)) {
				sql = "SELECT m.id, COALESCE(m.kode,''), COALESCE(m.nama,''), COALESCE(m.keterangan,''),"
						+ " COALESCE(m.aktif,true), m.akun, m.akun_penutup_kas_kecil, CAST(NULL AS bigint), CAST(NULL AS bigint),"
						+ " (SELECT count(*) FROM akunting.kas_kecil x WHERE x.jenis_kas_kecil = m.id)"
						+ " , m.satuan_kerja FROM public.jenis_kas_kecil m";
			} else if ("jenis_kas_besar".equals(tipe)) {
				sql = "SELECT m.id, COALESCE(m.kode,''), COALESCE(m.nama,''), COALESCE(m.keterangan,''),"
						+ " COALESCE(m.aktif,true), m.akun, m.akun_penerima, CAST(NULL AS bigint), CAST(NULL AS bigint),"
						+ " (SELECT count(*) FROM akunting.kas_besar x WHERE x.jenis_kas_besar = m.id)"
						+ " , m.satuan_kerja FROM public.jenis_kas_besar m";
			} else if ("jenis_reimbursement".equals(tipe)) {
				sql = "SELECT m.id, '' AS kode, COALESCE(m.nama,''), COALESCE(m.keterangan,''),"
						+ " COALESCE(m.aktif,true), m.akun, CAST(NULL AS bigint), CAST(NULL AS bigint),"
						+ " CAST(CASE WHEN COALESCE(m.menggunakan_anggaran,false) THEN 1 ELSE 0 END AS bigint),"
						+ " (SELECT count(*) FROM akunting.reimbursement_pegawai x"
						+ "  WHERE x.jenis_reimbursement = m.id)"
						+ " , m.satuan_kerja FROM akunting.jenis_reimbursement m";
			} else if ("jenis_pengeluaran".equals(tipe)) {
				sql = "SELECT m.id, '' AS kode, COALESCE(m.nama,''), COALESCE(m.keterangan,''),"
						+ " COALESCE(m.aktif,true), m.akun, CAST(NULL AS bigint), CAST(NULL AS bigint), CAST(NULL AS bigint),"
						+ " CAST(0 AS bigint), CAST(NULL AS bigint) FROM akunting.jenis_pengeluaran m";
			} else {
				sql = "SELECT m.id, COALESCE(m.kode,''), COALESCE(m.nama,''), COALESCE(m.keterangan,''),"
						+ " COALESCE(m.aktif,true), m.akun, m.akun_transitori, CAST(NULL AS bigint), CAST(NULL AS bigint),"
						+ " (SELECT count(*) FROM akunting.proses_transfer x"
						+ "  WHERE x.cara_pembayaran_transfer = m.id)"
						+ " , m.satuan_kerja FROM akunting.cara_pembayaran_transfer m";
			}
			StringBuilder b = new StringBuilder(sql).append(" WHERE 1 = 1");
			if (!cari.isEmpty()) {
				b.append(" AND (COALESCE(m.nama,'') ILIKE ? OR COALESCE(m.keterangan,'') ILIKE ?)");
			}
			b.append(" ORDER BY COALESCE(m.nama,'') LIMIT 500");

			PreparedStatement ps = conn.prepareStatement(b.toString());
			if (!cari.isEmpty()) {
				String kw = "%" + cari + "%";
				ps.setString(1, kw);
				ps.setString(2, kw);
			}
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			int belumLengkap = 0;
			JSONArray medan = kolomAkun(tipe);
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("keterangan", rs.getString(4));
				j.put("aktif", rs.getBoolean(5));
				j.put("akunId", nilaiId(rs, 6));
				j.put("akunKeduaId", nilaiId(rs, 7));
				j.put("akunKetigaId", nilaiId(rs, 8));
				long flag = rs.getLong(9);
				j.put("menggunakanAnggaran", !rs.wasNull() && flag == 1);
				j.put("dipakai", rs.getLong(10));
				j.put("satuanKerjaId", nilaiId(rs, 11));

				// Akun "wajib untuk jurnal" yang masih kosong ditandai di sini, bukan
				// dibiarkan ketahuan saat dokumen gagal terjurnal.
				boolean lengkap = true;
				for (int k = 0; k < medan.length(); k++) {
					if (!medan.getJSONObject(k).optBoolean("wajibUntukJurnal")) {
						continue;
					}
					Object v = k == 0 ? j.get("akunId") : (k == 1 ? j.get("akunKeduaId") : j.get("akunKetigaId"));
					if (v == JSONObject.NULL) {
						lengkap = false;
					}
				}
				j.put("akunLengkap", lengkap);
				if (!lengkap) {
					belumLengkap++;
				}
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("tipe", tipe);
			hasil.put("label", label(tipe));
			hasil.put("medanAkun", medan);
			hasil.put("data", arr);
			hasil.put("belumLengkap", belumLengkap);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static Object nilaiId(ResultSet rs, int kolom) throws Exception {
		long v = rs.getLong(kolom);
		return rs.wasNull() ? JSONObject.NULL : Long.valueOf(v);
	}

	// ============================================================ opsi

	public static void opsi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			JSONArray satker = new JSONArray();
			PreparedStatement ps = conn.prepareStatement(
					"SELECT id, COALESCE(nama,'') FROM rab.satuan_kerja ORDER BY nama LIMIT 500");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				satker.put(j);
			}
			rs.close();
			ps.close();

			JSONArray tipe = new JSONArray();
			String[] semua = { "jenis_uang_muka", "jenis_kas_kecil", "jenis_kas_besar",
					"jenis_reimbursement", "jenis_pengeluaran", "cara_pembayaran_transfer" };
			for (int i = 0; i < semua.length; i++) {
				JSONObject j = new JSONObject();
				j.put("tipe", semua[i]);
				j.put("label", label(semua[i]));
				j.put("medanAkun", kolomAkun(semua[i]));
				j.put("punyaKode", !"jenis_reimbursement".equals(semua[i])
						&& !"jenis_pengeluaran".equals(semua[i]));
				j.put("punyaAnggaran", "jenis_reimbursement".equals(semua[i]));
				// jenis_pengeluaran satu-satunya yang tidak bertautan satuan kerja.
				j.put("punyaSatuanKerja", !"jenis_pengeluaran".equals(semua[i]));
				tipe.put(j);
			}

			hasil.put("status", "00");
			hasil.put("tipe", tipe);
			hasil.put("satuanKerja", satker);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ simpan

	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String tipe = request == null ? "" : request.optString("tipe", "").trim();
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id == 0;
		if (!tipeSah(tipe)) {
			tolak(hasil, "Tipe master keuangan tidak dikenali.");
			return;
		}
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah master keuangan."
					: "Anda tidak memiliki hak mengubah master keuangan.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama " + label(tipe) + " wajib diisi.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Akun akun = ambilAkun(session, request, "akunId");
			Akun akun2 = ambilAkun(session, request, "akunKeduaId");
			Akun akun3 = ambilAkun(session, request, "akunKetigaId");
			SatuanKerja satker = null;
			long satkerId = request.optLong("satuanKerjaId", 0);
			if (satkerId != 0) {
				satker = (SatuanKerja) session.get(SatuanKerja.class, Long.valueOf(satkerId));
			}
			String kode = request.optString("kode", "").trim();
			String keterangan = request.optString("keterangan", "").trim();
			boolean aktif = request.optBoolean("aktif", true);

			session.beginTransaction();
			Long idBaru;
			if ("jenis_uang_muka".equals(tipe)) {
				JenisUangMuka m = baru ? new JenisUangMuka()
						: (JenisUangMuka) session.get(JenisUangMuka.class, Long.valueOf(id));
				if (m == null) {
					throw new IllegalStateException(label(tipe) + " tidak ditemukan.");
				}
				m.setNama(nama);
				m.setKode(kode);
				m.setKeterangan(keterangan);
				m.setAktif(Boolean.valueOf(aktif));
				m.setAkun(akun);
				m.setAkunKelebihan(akun2);
				m.setAkunSponsor(akun3);
				m.setSatuanKerja(satker);
				session.saveOrUpdate(m);
				idBaru = m.getId();
			} else if ("jenis_kas_kecil".equals(tipe)) {
				JenisKasKecil m = baru ? new JenisKasKecil()
						: (JenisKasKecil) session.get(JenisKasKecil.class, Long.valueOf(id));
				if (m == null) {
					throw new IllegalStateException(label(tipe) + " tidak ditemukan.");
				}
				m.setNama(nama);
				m.setKode(kode);
				m.setKeterangan(keterangan);
				m.setAktif(Boolean.valueOf(aktif));
				m.setAkun(akun);
				m.setAkunPenutupKasKecil(akun2);
				m.setSatuanKerja(satker);
				session.saveOrUpdate(m);
				idBaru = m.getId();
			} else if ("jenis_kas_besar".equals(tipe)) {
				JenisKasBesar m = baru ? new JenisKasBesar()
						: (JenisKasBesar) session.get(JenisKasBesar.class, Long.valueOf(id));
				if (m == null) {
					throw new IllegalStateException(label(tipe) + " tidak ditemukan.");
				}
				m.setNama(nama);
				m.setKode(kode);
				m.setKeterangan(keterangan);
				m.setAktif(Boolean.valueOf(aktif));
				m.setAkun(akun);
				m.setAkunPenerima(akun2);
				m.setSatuanKerja(satker);
				session.saveOrUpdate(m);
				idBaru = m.getId();
			} else if ("jenis_reimbursement".equals(tipe)) {
				JenisReimbursement m = baru ? new JenisReimbursement()
						: (JenisReimbursement) session.get(JenisReimbursement.class, Long.valueOf(id));
				if (m == null) {
					throw new IllegalStateException(label(tipe) + " tidak ditemukan.");
				}
				m.setNama(nama);
				m.setKeterangan(keterangan);
				m.setAktif(Boolean.valueOf(aktif));
				m.setAkun(akun);
				m.setMenggunakanAnggaran(Boolean.valueOf(request.optBoolean("menggunakanAnggaran", false)));
				m.setSatuanKerja(satker);
				session.saveOrUpdate(m);
				idBaru = m.getId();
			} else if ("jenis_pengeluaran".equals(tipe)) {
				JenisPengeluaran m = baru ? new JenisPengeluaran()
						: (JenisPengeluaran) session.get(JenisPengeluaran.class, Long.valueOf(id));
				if (m == null) {
					throw new IllegalStateException(label(tipe) + " tidak ditemukan.");
				}
				m.setNama(nama);
				m.setKeterangan(keterangan);
				m.setAktif(Boolean.valueOf(aktif));
				m.setAkun(akun);
				session.saveOrUpdate(m);
				idBaru = m.getId();
			} else {
				CaraPembayaranTransfer m = baru ? new CaraPembayaranTransfer()
						: (CaraPembayaranTransfer) session.get(CaraPembayaranTransfer.class, Long.valueOf(id));
				if (m == null) {
					throw new IllegalStateException(label(tipe) + " tidak ditemukan.");
				}
				m.setNama(nama);
				m.setKode(kode);
				m.setKeterangan(keterangan);
				m.setAktif(Boolean.valueOf(aktif));
				m.setAkun(akun);
				m.setAkunTransitori(akun2);
				m.setSatuanKerja(satker);
				session.saveOrUpdate(m);
				idBaru = m.getId();
			}
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", idBaru);
			hasil.put("message", label(tipe) + (baru ? " ditambahkan." : " diperbarui."));
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, label(tipe) + " belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static Akun ambilAkun(Session session, JSONObject request, String kunci) {
		long v = request == null ? 0 : request.optLong(kunci, 0);
		return v == 0 ? null : (Akun) session.get(Akun.class, Long.valueOf(v));
	}

	// ============================================================ hapus

	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String tipe = request == null ? "" : request.optString("tipe", "").trim();
		long id = request == null ? 0 : request.optLong("id", 0);
		if (!tipeSah(tipe)) {
			tolak(hasil, "Tipe master keuangan tidak dikenali.");
			return;
		}
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus master keuangan.");
			return;
		}
		if (id == 0) {
			tolak(hasil, "Data yang akan dihapus belum dipilih.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Jenis yang sudah dipakai dokumen tidak boleh dihapus: menghapusnya memutus
			// riwayat dokumen lama dari akunnya, bukan sekadar melanggar FK.
			long dipakai = hitungPemakaian(session, tipe, id);
			if (dipakai > 0) {
				tolak(hasil, label(tipe) + " ini sudah dipakai " + dipakai
						+ " dokumen sehingga tidak boleh dihapus. Nonaktifkan saja bila tidak dipakai lagi.");
				return;
			}

			Object m = muat(session, tipe, id);
			if (m == null) {
				tolak(hasil, label(tipe) + " tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			session.delete(m);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", label(tipe) + " dihapus.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, label(tipe) + " belum dapat dihapus: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static Object muat(Session session, String tipe, long id) {
		if ("jenis_uang_muka".equals(tipe)) {
			return session.get(JenisUangMuka.class, Long.valueOf(id));
		}
		if ("jenis_kas_kecil".equals(tipe)) {
			return session.get(JenisKasKecil.class, Long.valueOf(id));
		}
		if ("jenis_kas_besar".equals(tipe)) {
			return session.get(JenisKasBesar.class, Long.valueOf(id));
		}
		if ("jenis_reimbursement".equals(tipe)) {
			return session.get(JenisReimbursement.class, Long.valueOf(id));
		}
		if ("jenis_pengeluaran".equals(tipe)) {
			return session.get(JenisPengeluaran.class, Long.valueOf(id));
		}
		return session.get(CaraPembayaranTransfer.class, Long.valueOf(id));
	}

	/** Berapa dokumen yang memakai satu master. Nama tabel & kolomnya tertulis di kode. */
	private static long hitungPemakaian(Session session, String tipe, long id) throws Exception {
		String sql;
		if ("jenis_uang_muka".equals(tipe)) {
			sql = "SELECT count(*) FROM public.uang_muka WHERE jenis_uang_muka = ?";
		} else if ("jenis_kas_kecil".equals(tipe)) {
			sql = "SELECT count(*) FROM akunting.kas_kecil WHERE jenis_kas_kecil = ?";
		} else if ("jenis_kas_besar".equals(tipe)) {
			sql = "SELECT count(*) FROM akunting.kas_besar WHERE jenis_kas_besar = ?";
		} else if ("jenis_reimbursement".equals(tipe)) {
			sql = "SELECT count(*) FROM akunting.reimbursement_pegawai WHERE jenis_reimbursement = ?";
		} else if ("jenis_pengeluaran".equals(tipe)) {
			// Jenis pengeluaran dipakai di dalam kolom formula (JSON) rincian reimbursement,
			// bukan lewat relasi -- jadi dicari sebagai teks pada dokumen yang ada.
			sql = "SELECT count(*) FROM akunting.reimbursement_pegawai"
					+ " WHERE COALESCE(formula,'') LIKE '%\"jenisPengeluaran\":' || ? || '%'";
		} else {
			sql = "SELECT count(*) FROM akunting.proses_transfer WHERE cara_pembayaran_transfer = ?";
		}
		PreparedStatement ps = session.connection().prepareStatement(sql);
		if ("jenis_pengeluaran".equals(tipe)) {
			ps.setString(1, String.valueOf(id));
		} else {
			ps.setLong(1, id);
		}
		ResultSet rs = ps.executeQuery();
		rs.next();
		long n = rs.getLong(1);
		rs.close();
		ps.close();
		return n;
	}

	// ============================================================ dispatcher

	/** Dipakai dispatcher: seluruh aksi berawalan {@code master_keuangan_}. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("master_keuangan_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("master_keuangan_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("master_keuangan_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("master_keuangan_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
