package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.JenisKasKecil;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.WaktuUtil;

/**
 * <h3>API JSON "Kas Kecil" untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan menu yang selama ini HANYA ada di layar ZK
 * ({@code ais.action.master.akunting.KasKecilAction}) ke Desktop/Android.</p>
 *
 * <p>Tiga aturan khas modul ini dipertahankan apa adanya dari layar ZK, dan ketiganya
 * bukan sekadar validasi kosmetik:</p>
 * <ol>
 * <li><b>Tiap baris biaya wajib punya AKUN dan jumlahnya tidak boleh nol.</b> Tanpa akun,
 *     pengeluaran kas kecil tidak dapat dijurnal ke buku besar.</li>
 * <li><b>Nilai pengeluaran tidak boleh melebihi SALDO kas kecil</b> pada tanggal laporan --
 *     memakai perhitungan yang sama persis ({@code JenisKasKecilAction.hitungSaldo}), bukan
 *     rumus tiruan.</li>
 * <li><b>Satu jenis kas kecil hanya boleh punya satu dokumen yang belum disetujui.</b>
 *     Dokumen baru ditolak selama masih ada dokumen menggantung, dan kode dokumen yang
 *     menggantung itu disebutkan supaya pengguna tahu harus menyelesaikan yang mana.</li>
 * </ol>
 */
public final class KasKecilApiHelper {

	private static final String KUNCI = "kas_kecil";

	private KasKecilApiHelper() {
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
			ais.common.ErrorAuditUtil.record(e, "auto-audit KasKecilApiHelper.batalkanDiam");
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

	// ==================================================================== rincian

	/** Alasan rincian ditolak, atau null bila rincian sudah benar. */
	static String masalahRincian(Session session, JSONArray rincian) {
		for (int i = 0; rincian != null && i < rincian.length(); i++) {
			JSONObject b = rincian.optJSONObject(i);
			if (b == null || b.isNull("key")) {
				continue;
			}
			long akunId = b.optLong("akun", 0);
			if (akunId <= 0 || session.get(Akun.class, Long.valueOf(akunId)) == null) {
				return "Ada akun biaya pengeluaran yang belum dipilih. Lengkapi dulu rinciannya.";
			}
			if ((long) b.optDouble("jumlah", 0) == 0L) {
				return "Ada nilai biaya pengeluaran yang masih nol. Lengkapi dulu rinciannya.";
			}
		}
		return null;
	}

	/** Jumlah rincian: hanya baris ber-{@code key} (baris biaya) yang dihitung. */
	static double hitungRincian(JSONArray rincian) {
		double total = 0;
		for (int i = 0; rincian != null && i < rincian.length(); i++) {
			JSONObject b = rincian.optJSONObject(i);
			if (b == null || b.isNull("key")) {
				continue;
			}
			total += b.optDouble("jumlah", 0);
		}
		return total;
	}

	// ==================================================================== daftar

	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		String statusFilter = request == null ? "" : request.optString("statusFilter", "").trim();
		Date dari = tanggal(request, "dari");
		Date sampai = tanggal(request, "sampai");
		long satkerId = request == null ? 0 : request.optLong("satuanKerjaId", 0);
		long jenisId = request == null ? 0 : request.optLong("jenisKasKecilId", 0);
		boolean belumDiganti = request != null && request.optBoolean("belumDiganti", false);
		int batas = request == null ? 200 : request.optInt("limit", 200);
		if (batas <= 0 || batas > 1000) {
			batas = 200;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT k.id, COALESCE(k.kode,''), COALESCE(k.nama,''), COALESCE(k.keterangan,''),"
							+ " COALESCE(k.nilai,0), COALESCE(k.saldo,0), COALESCE(k.sisa,0),"
							+ " COALESCE(k.status,''), k.tanggal_pengajuan, k.tanggal_pembuatan,"
							+ " k.tanggal_persetujuan,"
							+ " k.satuan_kerja, COALESCE(sk.nama,''),"
							+ " k.jenis_kas_kecil, COALESCE(j.nama,''),"
							+ " COALESCE(k.dibuat_oleh,''), COALESCE(k.disetujui_oleh,''),"
							+ " k.posting_history, k.penggantian_kas_kecil, k.kas_besar,"
							+ " COALESCE(k.merupakanpenutupankaskecil,false), COALESCE(k.formula,'')"
							+ " FROM akunting.kas_kecil k"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = k.satuan_kerja"
							+ " LEFT JOIN public.jenis_kas_kecil j ON j.id = k.jenis_kas_kecil"
							+ " WHERE 1 = 1");
			if (!cari.isEmpty()) {
				sql.append(" AND (k.kode ILIKE ? OR k.nama ILIKE ? OR COALESCE(k.keterangan,'') ILIKE ?)");
			}
			if (!statusFilter.isEmpty()) {
				sql.append(" AND COALESCE(k.status,'') = ?");
			}
			if (dari != null) {
				sql.append(" AND k.tanggal_pengajuan >= ?");
			}
			if (sampai != null) {
				sql.append(" AND k.tanggal_pengajuan <= ?");
			}
			if (satkerId > 0) {
				sql.append(" AND k.satuan_kerja = ?");
			}
			if (jenisId > 0) {
				sql.append(" AND k.jenis_kas_kecil = ?");
			}
			if (belumDiganti) {
				sql.append(" AND k.penggantian_kas_kecil IS NULL");
			}
			sql.append(" ORDER BY k.id DESC LIMIT ").append(batas);

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
				ps.setTimestamp(i++, new java.sql.Timestamp(sampai.getTime() + 86399000L));
			}
			if (satkerId > 0) {
				ps.setLong(i++, satkerId);
			}
			if (jenisId > 0) {
				ps.setLong(i++, jenisId);
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
				j.put("sisa", rs.getDouble(7));
				j.put("statusDokumen", rs.getString(8));
				j.put("tanggal", teks(rs.getTimestamp(9)));
				j.put("tanggalPembuatan", teks(rs.getTimestamp(10)));
				j.put("tanggalPersetujuan", teks(rs.getTimestamp(11)));
				long v = rs.getLong(12);
				j.put("satuanKerjaId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("satuanKerjaNama", rs.getString(13));
				v = rs.getLong(14);
				j.put("jenisKasKecilId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("jenisKasKecilNama", rs.getString(15));
				j.put("dibuatOleh", rs.getString(16));
				j.put("disetujuiOleh", rs.getString(17));
				rs.getLong(18);
				j.put("sudahDijurnal", !rs.wasNull());
				rs.getLong(19);
				j.put("sudahDiganti", !rs.wasNull());
				rs.getLong(20);
				j.put("masukKasBesar", !rs.wasNull());
				j.put("penutupanKasKecil", rs.getBoolean(21));
				String formula = rs.getString(22);
				j.put("rincian", formula == null || formula.trim().isEmpty() ? new JSONArray()
						: new JSONArray(formula));
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

	// ==================================================================== opsi & saldo

	public static void opsi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			JSONArray jenis = new JSONArray();
			PreparedStatement ps = conn.prepareStatement(
					"SELECT id, COALESCE(kode,''), COALESCE(nama,''), COALESCE(saldoawal,0)"
							+ " FROM public.jenis_kas_kecil WHERE COALESCE(aktif,true) = true"
							+ " ORDER BY nama LIMIT 300");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("saldoAwal", rs.getDouble(4));
				jenis.put(j);
			}
			rs.close();
			ps.close();

			JSONArray satker = new JSONArray();
			ps = conn.prepareStatement("SELECT id, COALESCE(nama,'') FROM rab.satuan_kerja ORDER BY nama LIMIT 500");
			rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				satker.put(j);
			}
			rs.close();
			ps.close();

			JSONArray status = new JSONArray();
			status.put(KasKecil.PENGAJUAN);
			status.put(KasKecil.DISETUJU);
			status.put(KasKecil.DITOLAK);

			hasil.put("status", "00");
			hasil.put("jenisKasKecil", jenis);
			hasil.put("satuanKerja", satker);
			hasil.put("daftarStatus", status);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Saldo kas kecil pada satu tanggal -- memakai perhitungan yang SAMA dengan layar ZK,
	 * termasuk pengecualian dokumen yang sedang disunting. Dipakai klien untuk memperlihatkan
	 * batas sebelum pengguna menekan Simpan.
	 */
	public static void saldo(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long jenisId = request == null ? 0 : request.optLong("jenisKasKecilId", 0);
		long idKecuali = request == null ? 0 : request.optLong("id", 0);
		Date pada = tanggal(request, "tanggal");
		if (jenisId <= 0) {
			hasil.put("status", "00");
			hasil.put("saldo", 0);
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JenisKasKecil jenis = (JenisKasKecil) session.get(JenisKasKecil.class, Long.valueOf(jenisId));
			if (jenis == null) {
				tolak(hasil, "Jenis Kas Kecil tidak ditemukan.");
				return;
			}
			Double saldo = ais.action.master.akunting.JenisKasKecilAction.hitungSaldo(
					idKecuali > 0 ? Long.valueOf(idKecuali) : null, jenis,
					pada == null ? WaktuUtil.getDate() : pada);
			hasil.put("status", "00");
			hasil.put("saldo", saldo == null ? 0 : saldo.doubleValue());
			hasil.put("jenis", jenis.getNama());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Pratinjau jumlah rincian + alasan penolakannya, tanpa menyimpan. */
	public static void hitungPratinjau(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray rincian = request == null ? null : request.optJSONArray("rincian");
			String masalah = masalahRincian(session, rincian);
			hasil.put("status", "00");
			hasil.put("nilai", hitungRincian(rincian));
			hasil.put("masalah", masalah == null ? "" : masalah);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== simpan

	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id <= 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah pengeluaran kas kecil."
					: "Anda tidak memiliki hak mengubah pengeluaran kas kecil.");
			return;
		}
		long satkerId = request.optLong("satuanKerjaId", 0);
		String nama = request.optString("nama", "").trim();
		long jenisId = request.optLong("jenisKasKecilId", 0);
		Date tgl = tanggal(request, "tanggal");
		String statusDokumen = request.optString("statusDokumen", KasKecil.PENGAJUAN).trim();
		JSONArray rincian = request.optJSONArray("rincian");

		if (satkerId <= 0) {
			tolak(hasil, "Satuan Kerja belum dipilih.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// --- urutan validasi disamakan dengan layar ZK
			String masalah = masalahRincian(session, rincian);
			if (masalah != null) {
				tolak(hasil, masalah);
				return;
			}
			double nilai = hitungRincian(rincian);
			if (nama.isEmpty()) {
				tolak(hasil, "Judul Pengeluaran Kas Kecil belum diisi.");
				return;
			}
			if (jenisId <= 0) {
				tolak(hasil, "Jenis Kas Kecil belum dipilih.");
				return;
			}
			if (tgl == null) {
				tolak(hasil, "Tanggal Laporan belum diisi.");
				return;
			}
			JenisKasKecil jenis = (JenisKasKecil) session.get(JenisKasKecil.class, Long.valueOf(jenisId));
			if (jenis == null) {
				tolak(hasil, "Jenis Kas Kecil tidak ditemukan.");
				return;
			}
			if (KasKecil.DISETUJU.equals(statusDokumen) && !bolehAksi(tbmuser, "approve")) {
				tolak(hasil, "Anda tidak memiliki hak menyetujui pengeluaran kas kecil.");
				return;
			}

			Double saldo = ais.action.master.akunting.JenisKasKecilAction.hitungSaldo(
					baru ? null : Long.valueOf(id), jenis, tgl);
			double saldoD = saldo == null ? 0 : saldo.doubleValue();
			if (saldoD < nilai) {
				tolak(hasil, "Nilai pengeluaran kas kecil (" + Common.numberFormat.get().format(nilai)
						+ ") melebihi saldo yang tersedia (" + Common.numberFormat.get().format(saldoD) + ").");
				return;
			}

			// Satu jenis kas kecil hanya boleh punya satu dokumen yang belum disetujui.
			if (baru) {
				@SuppressWarnings("unchecked")
				List<String> menggantung = session.createCriteria(KasKecil.class)
						.add(Restrictions.ne("status", KasKecil.DITOLAK))
						.add(Restrictions.eq("jenisKasKecil", jenis))
						.add(Restrictions.isNull("disetujuiOleh"))
						.setProjection(Projections.property("kode")).list();
				if (menggantung != null && !menggantung.isEmpty()) {
					StringBuilder daftarKode = new StringBuilder();
					for (String kode : menggantung) {
						if (daftarKode.length() > 0) {
							daftarKode.append("; ");
						}
						daftarKode.append(kode);
					}
					tolak(hasil, "Untuk \"" + jenis.getNama() + "\" masih ada dokumen kas kecil yang belum"
							+ " disetujui: " + daftarKode + ". Selesaikan dulu dokumen itu.");
					return;
				}
			}

			KasKecil kk = baru ? new KasKecil() : (KasKecil) session.get(KasKecil.class, Long.valueOf(id));
			if (kk == null) {
				tolak(hasil, "Dokumen tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			if (!baru && kk.getPostingHistory() != null) {
				tolak(hasil, "Dokumen ini sudah dijurnal sehingga tidak boleh diubah lagi.");
				return;
			}

			kk.setSatuanKerja((SatuanKerja) session.get(SatuanKerja.class, Long.valueOf(satkerId)));
			kk.setJenisKasKecil(jenis);
			kk.setNama(nama);
			kk.setNilai(Double.valueOf(nilai));
			kk.setSaldo(Double.valueOf(saldoD));
			kk.setSisa(Double.valueOf(saldoD - nilai));
			kk.setKeterangan(request.optString("keterangan", "").trim());
			kk.setTanggal(tgl);
			kk.setFormula(rincian == null ? "[]" : rincian.toString());
			kk.setTampilkanAnggaran(Boolean.valueOf(request.optBoolean("tampilkanAnggaran", false)));
			if (kk.getDibuatOleh() == null) {
				kk.setDibuatOleh(tbmuser);
				kk.setTanggalPembuatan(WaktuUtil.getDate());
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(tgl);
			kk.setTahun(Integer.valueOf(cal.get(Calendar.YEAR)));
			kk.setBulan(Integer.valueOf(cal.get(Calendar.MONTH) + 1));
			if (KasKecil.DISETUJU.equals(statusDokumen)) {
				kk.setDisetujuiOleh(tbmuser);
				Date tglSetuju = tanggal(request, "tanggalPersetujuan");
				kk.setTanggalPersetujuan(tglSetuju == null ? WaktuUtil.getDate() : tglSetuju);
			} else {
				kk.setDisetujuiOleh(null);
				kk.setTanggalPersetujuan(null);
			}
			kk.setStatus(statusDokumen);
			if (kk.getAktif() == null) {
				kk.setAktif(Boolean.TRUE);
			}
			if (tbmuser != null) {
				kk.setOleh(tbmuser.getUserNama());
				kk.setOlehId(tbmuser.getUserId());
			}

			session.beginTransaction();
			if (baru) {
				kk.setKode(buatKode(session));
			}
			session.saveOrUpdate(kk);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", kk.getId());
			hasil.put("kode", kk.getKode());
			hasil.put("nilai", nilai);
			hasil.put("saldo", saldoD);
			hasil.put("sisa", saldoD - nilai);
			hasil.put("message", baru ? "Pengeluaran kas kecil " + kk.getKode() + " dibuat."
					: "Pengeluaran kas kecil diperbarui.");
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
			tolak(hasil, setujui ? "Anda tidak memiliki hak menyetujui pengeluaran kas kecil."
					: "Anda tidak memiliki hak menolak pengeluaran kas kecil.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			KasKecil kk = id <= 0 ? null : (KasKecil) session.get(KasKecil.class, Long.valueOf(id));
			if (kk == null) {
				tolak(hasil, "Dokumen tidak ditemukan.");
				return;
			}
			if (kk.getPostingHistory() != null) {
				tolak(hasil, "Dokumen ini sudah dijurnal sehingga statusnya tidak boleh diubah.");
				return;
			}
			session.beginTransaction();
			if (setujui) {
				kk.setStatus(KasKecil.DISETUJU);
				kk.setDisetujuiOleh(tbmuser);
				Date tgl = tanggal(request, "tanggalPersetujuan");
				kk.setTanggalPersetujuan(tgl == null ? WaktuUtil.getDate() : tgl);
			} else {
				kk.setStatus(KasKecil.DITOLAK);
				kk.setDisetujuiOleh(null);
				kk.setTanggalPersetujuan(null);
			}
			if (tbmuser != null) {
				kk.setOleh(tbmuser.getUserNama());
				kk.setOlehId(tbmuser.getUserId());
			}
			session.update(kk);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Dokumen " + kk.getKode() + (setujui ? " disetujui." : " ditolak."));
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
			tolak(hasil, "Anda tidak memiliki hak menghapus pengeluaran kas kecil.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			KasKecil kk = id <= 0 ? null : (KasKecil) session.get(KasKecil.class, Long.valueOf(id));
			if (kk == null) {
				tolak(hasil, "Dokumen tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			if (kk.getPostingHistory() != null) {
				tolak(hasil, "Dokumen ini sudah dijurnal sehingga tidak boleh dihapus.");
				return;
			}
			if (kk.getPenggantianKasKecil() != null) {
				tolak(hasil, "Dokumen ini sudah masuk penggantian kas kecil sehingga tidak boleh dihapus.");
				return;
			}
			if (kk.getKasBesar() != null) {
				tolak(hasil, "Dokumen ini sudah ditarik ke kas besar sehingga tidak boleh dihapus.");
				return;
			}
			if (KasKecil.DISETUJU.equals(kk.getStatus())) {
				tolak(hasil, "Dokumen yang sudah disetujui tidak boleh dihapus. Tolak dulu bila memang keliru.");
				return;
			}
			String kode = kk.getKode();
			session.beginTransaction();
			session.delete(kk);
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
			if (NomorSuratAlurKeuangan.KAS_KECIL_DATA == null
					|| NomorSuratAlurKeuangan.KAS_KECIL_DATA.getNomorSurat() == null) {
				return Common.getGeneratedBarCode();
			}
			NomorSurat ns = NomorSuratAlurKeuangan.KAS_KECIL_DATA.getNomorSurat();
			Long index = Boolean.TRUE.equals(ns.getGunakanIndexUrut()) ? ns.getNomorIndex()
					: indexBerikutnya(session, ns);
			NomorSurat.tambahIndexNomorSurat(ns);
			String noAgenda = ns.format(index, WaktuUtil.getDate());
			return ais.action.master.KodeUnikUtil.pastikanUnik(KasKecil.class, noAgenda);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit KasKecilApiHelper.buatKode");
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
		Criteria c = session.createCriteria(KasKecil.class)
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

	/** Dipakai dispatcher: seluruh aksi berawalan {@code kas_kecil_} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("kas_kecil_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("kas_kecil_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("kas_kecil_saldo".equals(action)) {
			saldo(tbmuser, request, hasil);
			return true;
		}
		if ("kas_kecil_hitung".equals(action)) {
			hitungPratinjau(tbmuser, request, hasil);
			return true;
		}
		if ("kas_kecil_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("kas_kecil_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		if ("kas_kecil_setujui".equals(action)) {
			ubahStatus(tbmuser, request, hasil, true);
			return true;
		}
		if ("kas_kecil_tolak".equals(action)) {
			ubahStatus(tbmuser, request, hasil, false);
			return true;
		}
		return false;
	}
}
