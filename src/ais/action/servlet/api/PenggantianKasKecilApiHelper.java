package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.PenggantianKasKecil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.WaktuUtil;

/**
 * <h3>API JSON "Penggantian Kas Kecil (Reimbursement)" untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan menu yang selama ini HANYA ada di layar ZK
 * ({@code ais.action.master.akunting.PenggantianKasKecilAction}) ke Desktop/Android.</p>
 *
 * <p><b>Yang membedakan modul ini dari lima modul Keuangan lainnya:</b> rinciannya BUKAN
 * milik dokumen ini, melainkan milik dokumen KAS KECIL yang sedang diganti. Layar ZK
 * memang menyunting rincian kas kecil itu dari sini -- karena itu penyimpanan di sini
 * ikut memperbarui {@code formula}, {@code nilai}, dan {@code saldo} pada dokumen kas
 * kecilnya, persis seperti ZK. Aturan rinciannya pun sama: tiap baris biaya wajib punya
 * akun dan jumlahnya tidak boleh nol.</p>
 *
 * <p>Setelah tersimpan, dokumen kas kecil ditautkan balik ke penggantian ini sehingga
 * daftar kas kecil tahu dokumennya sudah diganti.</p>
 */
public final class PenggantianKasKecilApiHelper {

	private static final String KUNCI = "penggantian_kas_kecil";

	private PenggantianKasKecilApiHelper() {
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
			if (session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PenggantianKasKecilApiHelper.batalkanDiam");
		}
	}

	private static Date tanggal(JSONObject request, String kunci) {
		String v = request == null ? "" : request.optString(kunci, "").trim();
		if (v.isEmpty()) {
			return null;
		}
		try {
			return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(v.length() > 10 ? v.substring(0, 10) : v);
		} catch (Exception e) {
			return null;
		}
	}

	private static String teks(java.sql.Timestamp t) {
		return t == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(t);
	}

	// ==================================================================== daftar

	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		String statusFilter = request == null ? "" : request.optString("statusFilter", "").trim();
		Date dari = tanggal(request, "dari");
		Date sampai = tanggal(request, "sampai");
		long satkerId = request == null ? 0 : request.optLong("satuanKerjaId", 0);
		int batas = request == null ? 200 : request.optInt("limit", 200);
		if (batas <= 0 || batas > 1000) {
			batas = 200;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT p.id, COALESCE(p.kode,''), COALESCE(p.nama,''), COALESCE(p.keterangan,''),"
							+ " COALESCE(p.nilai,0), COALESCE(p.saldo,0), COALESCE(p.status,''),"
							+ " p.tanggal_pembuatan, p.tanggal_persetujuan,"
							+ " p.kas_kecil, COALESCE(kk.kode,''), COALESCE(kk.nama,''),"
							+ " COALESCE(kk.nilai,0), COALESCE(kk.formula,''),"
							+ " p.satuan_kerja, COALESCE(sk.nama,''),"
							+ " COALESCE(p.dibuat_oleh,''), COALESCE(p.disetujui_oleh,''),"
							+ " p.posting_history, p.daftar_pengajuan_transfer,"
							+ " COALESCE(j.nama,'')"
							+ " FROM akunting.penggantian_kas_kecil p"
							+ " LEFT JOIN akunting.kas_kecil kk ON kk.id = p.kas_kecil"
							+ " LEFT JOIN public.jenis_kas_kecil j ON j.id = kk.jenis_kas_kecil"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = p.satuan_kerja"
							+ " WHERE 1 = 1");
			if (!cari.isEmpty()) {
				sql.append(" AND (p.kode ILIKE ? OR p.nama ILIKE ? OR COALESCE(kk.kode,'') ILIKE ?)");
			}
			if (!statusFilter.isEmpty()) {
				sql.append(" AND COALESCE(p.status,'') = ?");
			}
			if (dari != null) {
				sql.append(" AND p.tanggal_pembuatan >= ?");
			}
			if (sampai != null) {
				sql.append(" AND p.tanggal_pembuatan < (?::date + 1)");
			}
			if (satkerId > 0) {
				sql.append(" AND p.satuan_kerja = ?");
			}
			sql.append(" ORDER BY p.id DESC LIMIT ").append(batas);

			PreparedStatement ps = conn.prepareStatement(sql.toString());
			int i = 1;
			if (!cari.isEmpty()) {
				String kw = "%" + cari + "%";
				ps.setString(i++, kw);
				ps.setString(i++, kw);
				ps.setString(i++, kw);
			}
			if (!statusFilter.isEmpty()) {
				ps.setString(i++, statusFilter);
			}
			if (dari != null) {
				ps.setTimestamp(i++, new java.sql.Timestamp(dari.getTime()));
			}
			if (sampai != null) {
				ps.setDate(i++, new java.sql.Date(sampai.getTime()));
			}
			if (satkerId > 0) {
				ps.setLong(i++, satkerId);
			}

			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			double totalNilai = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("keterangan", rs.getString(4));
				double nilai = rs.getDouble(5);
				j.put("nilai", nilai);
				totalNilai += nilai;
				j.put("saldo", rs.getDouble(6));
				j.put("statusDokumen", rs.getString(7));
				j.put("tanggalPembuatan", teks(rs.getTimestamp(8)));
				j.put("tanggalPersetujuan", teks(rs.getTimestamp(9)));
				long v = rs.getLong(10);
				j.put("kasKecilId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("kasKecilKode", rs.getString(11));
				j.put("kasKecilNama", rs.getString(12));
				j.put("kasKecilNilai", rs.getDouble(13));
				String formula = rs.getString(14);
				j.put("rincian", formula == null || formula.trim().isEmpty() ? new JSONArray()
						: new JSONArray(formula));
				v = rs.getLong(15);
				j.put("satuanKerjaId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("satuanKerjaNama", rs.getString(16));
				j.put("dibuatOleh", rs.getString(17));
				j.put("disetujuiOleh", rs.getString(18));
				rs.getLong(19);
				j.put("sudahDijurnal", !rs.wasNull());
				rs.getLong(20);
				j.put("punyaTransfer", !rs.wasNull());
				j.put("jenisKasKecilNama", rs.getString(21));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("totalNilai", totalNilai);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== opsi

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

			JSONArray status = new JSONArray();
			status.put(PenggantianKasKecil.PENGAJUAN);
			status.put(PenggantianKasKecil.DISETUJU);
			status.put(PenggantianKasKecil.DITOLAK);

			hasil.put("status", "00");
			hasil.put("satuanKerja", satker);
			hasil.put("daftarStatus", status);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Cari dokumen Kas Kecil yang boleh diganti: sudah DISETUJUI dan belum punya
	 * penggantian. Saat menyunting, dokumen miliknya sendiri tetap ikut supaya tidak
	 * hilang dari pilihan.
	 */
	public static void cariKasKecil(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		long pgId = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT k.id, COALESCE(k.kode,''), COALESCE(k.nama,''), COALESCE(k.nilai,0),"
							+ " COALESCE(k.saldo,0), COALESCE(j.nama,''), COALESCE(sk.nama,''),"
							+ " COALESCE(k.formula,'')"
							+ " FROM akunting.kas_kecil k"
							+ " LEFT JOIN public.jenis_kas_kecil j ON j.id = k.jenis_kas_kecil"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = k.satuan_kerja"
							+ " WHERE COALESCE(k.status,'') = ?"
							+ "   AND (k.penggantian_kas_kecil IS NULL"
							+ (pgId > 0 ? " OR k.penggantian_kas_kecil = ?" : "") + ")");
			if (!cari.isEmpty()) {
				sql.append(" AND (k.kode ILIKE ? OR k.nama ILIKE ?)");
			}
			sql.append(" ORDER BY k.id DESC LIMIT 100");
			PreparedStatement ps = conn.prepareStatement(sql.toString());
			int i = 1;
			ps.setString(i++, KasKecil.DISETUJU);
			if (pgId > 0) {
				ps.setLong(i++, pgId);
			}
			if (!cari.isEmpty()) {
				String kw = "%" + cari + "%";
				ps.setString(i++, kw);
				ps.setString(i++, kw);
			}
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("nilai", rs.getDouble(4));
				j.put("saldo", rs.getDouble(5));
				j.put("jenis", rs.getString(6));
				j.put("satuanKerja", rs.getString(7));
				String formula = rs.getString(8);
				j.put("rincian", formula == null || formula.trim().isEmpty() ? new JSONArray()
						: new JSONArray(formula));
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

	// ==================================================================== simpan

	/**
	 * Simpan penggantian kas kecil. Selain dokumennya sendiri, penyimpanan ini IKUT
	 * memperbarui rincian, nilai, dan saldo dokumen kas kecil yang diganti -- perilaku
	 * yang sama dengan layar ZK, karena rinciannya memang milik dokumen kas kecil itu.
	 */
	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id <= 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah penggantian kas kecil."
					: "Anda tidak memiliki hak mengubah penggantian kas kecil.");
			return;
		}
		long satkerId = request.optLong("satuanKerjaId", 0);
		long kasKecilId = request.optLong("kasKecilId", 0);
		String nama = request.optString("nama", "").trim();
		String statusDokumen = request.optString("statusDokumen", PenggantianKasKecil.PENGAJUAN).trim();
		JSONArray rincian = request.optJSONArray("rincian");

		// --- urutan validasi disamakan dengan layar ZK
		if (satkerId <= 0) {
			tolak(hasil, "Satuan Kerja belum dipilih.");
			return;
		}
		if (kasKecilId <= 0) {
			tolak(hasil, "Kas Kecil belum dipilih.");
			return;
		}
		if (nama.isEmpty()) {
			tolak(hasil, "Judul Pengajuan belum diisi.");
			return;
		}
		if (PenggantianKasKecil.DISETUJU.equals(statusDokumen) && !bolehAksi(tbmuser, "approve")) {
			tolak(hasil, "Anda tidak memiliki hak menyetujui penggantian kas kecil.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			KasKecil kk = (KasKecil) session.get(KasKecil.class, Long.valueOf(kasKecilId));
			if (kk == null) {
				tolak(hasil, "Kas Kecil tidak ditemukan.");
				return;
			}
			// Rincian yang dikirim adalah rincian KAS KECIL-nya; aturannya sama.
			String masalah = rincian == null ? null : KasKecilApiHelper.masalahRincian(session, rincian);
			if (masalah != null) {
				tolak(hasil, masalah);
				return;
			}

			PenggantianKasKecil pg = baru ? new PenggantianKasKecil()
					: (PenggantianKasKecil) session.get(PenggantianKasKecil.class, Long.valueOf(id));
			if (pg == null) {
				tolak(hasil, "Dokumen tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			if (!baru && pg.getPostingHistory() != null) {
				tolak(hasil, "Dokumen ini sudah dijurnal sehingga tidak boleh diubah lagi.");
				return;
			}

			double nilai = kk.getNilai() == null ? 0 : kk.getNilai().doubleValue();
			double saldo = kk.getSaldo() == null ? 0 : kk.getSaldo().doubleValue();
			if (rincian != null) {
				nilai = KasKecilApiHelper.hitungRincian(rincian);
				saldo = kk.getSaldo() == null ? 0 : kk.getSaldo().doubleValue();
			}

			pg.setKasKecil(kk);
			pg.setSatuanKerja((SatuanKerja) session.get(SatuanKerja.class, Long.valueOf(satkerId)));
			pg.setNama(nama);
			pg.setKeterangan(request.optString("keterangan", "").trim());
			pg.setNilai(Double.valueOf(nilai));
			pg.setSaldo(Double.valueOf(saldo));
			if (pg.getDibuatOleh() == null) {
				pg.setDibuatOleh(tbmuser);
				pg.setTanggalPembuatan(WaktuUtil.getDate());
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(pg.getTanggalPembuatan() == null ? WaktuUtil.getDate() : pg.getTanggalPembuatan());
			pg.setTahun(Integer.valueOf(cal.get(Calendar.YEAR)));
			pg.setBulan(Integer.valueOf(cal.get(Calendar.MONTH) + 1));
			if (PenggantianKasKecil.DISETUJU.equals(statusDokumen)) {
				pg.setDisetujuiOleh(tbmuser);
				Date tglSetuju = tanggal(request, "tanggalPersetujuan");
				pg.setTanggalPersetujuan(tglSetuju == null ? WaktuUtil.getDate() : tglSetuju);
			} else {
				pg.setDisetujuiOleh(null);
				pg.setTanggalPersetujuan(null);
			}
			pg.setStatus(statusDokumen);
			if (pg.getAktif() == null) {
				pg.setAktif(Boolean.TRUE);
			}
			if (tbmuser != null) {
				pg.setOleh(tbmuser.getUserNama());
				pg.setOlehId(tbmuser.getUserId());
			}

			session.beginTransaction();
			if (baru) {
				pg.setKode(buatKode(session));
			}
			session.saveOrUpdate(pg);
			// Rincian & nilai dokumen kas kecil ikut diperbarui, lalu ditautkan balik.
			if (rincian != null) {
				kk.setFormula(rincian.toString());
				kk.setNilai(Double.valueOf(nilai));
			}
			kk.setPenggantianKasKecil(pg);
			session.update(kk);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", pg.getId());
			hasil.put("kode", pg.getKode());
			hasil.put("nilai", nilai);
			hasil.put("message", baru ? "Penggantian kas kecil " + pg.getKode() + " dibuat."
					: "Penggantian kas kecil diperbarui.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Dokumen belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== status & hapus

	public static void ubahStatus(Tbmuser tbmuser, JSONObject request, JSONObject hasil, boolean setujui)
			throws Exception {
		if (!bolehAksi(tbmuser, setujui ? "approve" : "reject")) {
			tolak(hasil, setujui ? "Anda tidak memiliki hak menyetujui penggantian kas kecil."
					: "Anda tidak memiliki hak menolak penggantian kas kecil.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenggantianKasKecil pg = id <= 0 ? null
					: (PenggantianKasKecil) session.get(PenggantianKasKecil.class, Long.valueOf(id));
			if (pg == null) {
				tolak(hasil, "Dokumen tidak ditemukan.");
				return;
			}
			if (pg.getPostingHistory() != null) {
				tolak(hasil, "Dokumen ini sudah dijurnal sehingga statusnya tidak boleh diubah.");
				return;
			}
			session.beginTransaction();
			if (setujui) {
				pg.setStatus(PenggantianKasKecil.DISETUJU);
				pg.setDisetujuiOleh(tbmuser);
				Date tgl = tanggal(request, "tanggalPersetujuan");
				pg.setTanggalPersetujuan(tgl == null ? WaktuUtil.getDate() : tgl);
			} else {
				pg.setStatus(PenggantianKasKecil.DITOLAK);
				pg.setDisetujuiOleh(null);
				pg.setTanggalPersetujuan(null);
			}
			if (tbmuser != null) {
				pg.setOleh(tbmuser.getUserNama());
				pg.setOlehId(tbmuser.getUserId());
			}
			session.update(pg);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Dokumen " + pg.getKode() + (setujui ? " disetujui." : " ditolak."));
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Status belum dapat diubah: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus penggantian kas kecil.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenggantianKasKecil pg = id <= 0 ? null
					: (PenggantianKasKecil) session.get(PenggantianKasKecil.class, Long.valueOf(id));
			if (pg == null) {
				tolak(hasil, "Dokumen tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			if (pg.getPostingHistory() != null) {
				tolak(hasil, "Dokumen ini sudah dijurnal sehingga tidak boleh dihapus.");
				return;
			}
			if (pg.getDaftarPengajuanTransfer() != null) {
				tolak(hasil, "Dokumen ini sudah masuk daftar pengajuan transfer sehingga tidak boleh dihapus.");
				return;
			}
			if (PenggantianKasKecil.DISETUJU.equals(pg.getStatus())) {
				tolak(hasil, "Dokumen yang sudah disetujui tidak boleh dihapus. Tolak dulu bila memang keliru.");
				return;
			}
			String kode = pg.getKode();
			session.beginTransaction();
			// Lepas tautan dari kas kecilnya supaya dokumen itu bisa diganti ulang.
			Criteria c = session.createCriteria(KasKecil.class)
					.add(Restrictions.eq("penggantianKasKecil", pg));
			for (Object o : c.list()) {
				KasKecil kk = (KasKecil) o;
				kk.setPenggantianKasKecil(null);
				session.update(kk);
			}
			session.delete(pg);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Dokumen " + kode + " dihapus.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Dokumen tidak dapat dihapus karena masih berelasi dengan data lain.");
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== kode dokumen

	private static String buatKode(Session session) {
		try {
			if (NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA == null
					|| NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA.getNomorSurat() == null) {
				return Common.getGeneratedBarCode();
			}
			NomorSurat ns = NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA.getNomorSurat();
			Long index = Boolean.TRUE.equals(ns.getGunakanIndexUrut()) ? ns.getNomorIndex()
					: indexBerikutnya(session, ns);
			NomorSurat.tambahIndexNomorSurat(ns);
			String noAgenda = ns.format(index, WaktuUtil.getDate());
			return ais.action.master.KodeUnikUtil.pastikanUnik(PenggantianKasKecil.class, noAgenda);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PenggantianKasKecilApiHelper.buatKode");
			return Common.getGeneratedBarCode();
		}
	}

	private static Long indexBerikutnya(Session session, NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return Long.valueOf(0);
		}
		int tahun = WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Criteria c = session.createCriteria(PenggantianKasKecil.class)
				.createAlias("nomorSuratAlurKeuangan", "nomorSuratAlurKeuangan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurKeuangan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN);
		c.add(Boolean.TRUE.equals(nomorSurat.getUrutBerdasarkanNomor())
				? Restrictions.eq("nomorSuratAlurKeuangan.nomorSurat", nomorSurat)
				: (Boolean.TRUE.equals(nomorSurat.getUrutBerdasarkanKelompok())
						&& nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat",
										nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")));
		c.add(Boolean.TRUE.equals(nomorSurat.getResetUrutanTiapTahun())
				? Restrictions.eq("tahun", Integer.valueOf(tahun))
				: Restrictions.sqlRestriction("true"));
		c.add(Boolean.TRUE.equals(nomorSurat.getResetUrutanTiapBulan())
				? Restrictions.and(Restrictions.eq("tahun", Integer.valueOf(tahun)),
						Restrictions.eq("bulan", Integer.valueOf(bulan)))
				: Restrictions.sqlRestriction("true"));
		c.add(nomorSurat.getResetTiap() != null && !nomorSurat.getResetTiap().after(sekarang)
				? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
				: Restrictions.sqlRestriction("true"));
		Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
		return Long.valueOf((n == null ? 0L : n.longValue()) + 1L);
	}

	// ==================================================================== dispatcher

	/** Dipakai dispatcher: seluruh aksi berawalan {@code penggantian_kas_kecil_}. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("penggantian_kas_kecil_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("penggantian_kas_kecil_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("penggantian_kas_kecil_cari_kas_kecil".equals(action)) {
			cariKasKecil(tbmuser, request, hasil);
			return true;
		}
		if ("penggantian_kas_kecil_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("penggantian_kas_kecil_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		if ("penggantian_kas_kecil_setujui".equals(action)) {
			ubahStatus(tbmuser, request, hasil, true);
			return true;
		}
		if ("penggantian_kas_kecil_tolak".equals(action)) {
			ubahStatus(tbmuser, request, hasil, false);
			return true;
		}
		return false;
	}
}
