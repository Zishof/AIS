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
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.PertangungjawabanKasBesar;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.WaktuUtil;

/**
 * <h3>API JSON "Pertanggungjawaban Kas Besar" untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan menu yang selama ini HANYA ada di layar ZK
 * ({@code ais.action.master.akunting.PertangungjawabanKasBesarAction}) ke Desktop/Android.</p>
 *
 * <p>Perhitungan nilainya IDENTIK dengan pertanggungjawaban uang muka -- rumusnya memang
 * satu dan sama pada layar ZK -- sehingga di sini pun dipakai ULANG
 * {@link PertangungjawabanApiHelper#hitung(Session, JSONArray, boolean)}, bukan disalin.
 * Dengan begitu perubahan rumus di kemudian hari tidak mungkin menyimpang di salah satu
 * modul saja.</p>
 *
 * <p>Beda yang disengaja terhadap modul LPJ uang muka, mengikuti layar ZK-nya
 * masing-masing: batas "tidak boleh melebihi" di sini dibandingkan dalam PECAHAN
 * ({@code nilai kas besar < nilai LPJ}), bukan dibulatkan ke satuan seperti pada LPJ
 * uang muka.</p>
 */
public final class PertangungjawabanKasBesarApiHelper {

	private static final String KUNCI = "pj_kas_besar";

	private PertangungjawabanKasBesarApiHelper() {
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
			ais.common.ErrorAuditUtil.record(e, "auto-audit PertangungjawabanKasBesarApiHelper.batalkanDiam");
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
		boolean belumDikembalikan = request != null && request.optBoolean("belumDikembalikan", false);
		int batas = request == null ? 200 : request.optInt("limit", 200);
		if (batas <= 0 || batas > 1000) {
			batas = 200;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT p.id, COALESCE(p.kode,''), COALESCE(p.nama,''), COALESCE(p.keterangan,''),"
							+ " COALESCE(p.nilai,0), COALESCE(p.pajak,0), COALESCE(p.dikembalikan,0),"
							+ " COALESCE(p.darisponsor,0), COALESCE(p.namasponsor,''),"
							+ " COALESCE(p.status,''), p.tanggal_pembuatan, p.tanggal_persetujuan,"
							+ " p.tanggalstor, COALESCE(p.telahdikembalikan,false),"
							+ " p.kas_besar, COALESCE(kb.kode,''), COALESCE(kb.nama,''), COALESCE(kb.nilai,0),"
							+ " p.satuan_kerja, COALESCE(sk.nama,''),"
							+ " COALESCE(p.dibuat_oleh,''), COALESCE(p.disetujui_oleh,''),"
							+ " p.posting_history, p.posting_history_pajak, p.posting_history_pengembalian,"
							+ " COALESCE(p.formula,'')"
							+ " FROM akunting.pertangungjawaban_kas_besar p"
							+ " LEFT JOIN akunting.kas_besar kb ON kb.id = p.kas_besar"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = p.satuan_kerja"
							+ " WHERE 1 = 1");
			if (!cari.isEmpty()) {
				sql.append(" AND (p.kode ILIKE ? OR p.nama ILIKE ? OR COALESCE(kb.kode,'') ILIKE ?)");
			}
			if (!statusFilter.isEmpty()) {
				sql.append(" AND COALESCE(p.status,'') = ?");
			}
			if (dari != null) {
				sql.append(" AND p.tanggal_pembuatan >= ?");
			}
			if (sampai != null) {
				sql.append(" AND p.tanggal_pembuatan < (CAST(? AS date) + 1)");
			}
			if (satkerId > 0) {
				sql.append(" AND p.satuan_kerja = ?");
			}
			if (belumDikembalikan) {
				sql.append(" AND COALESCE(p.dikembalikan,0) > 0 AND COALESCE(p.telahdikembalikan,false) = false");
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
			double totalDikembalikan = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("keterangan", rs.getString(4));
				double nilai = rs.getDouble(5);
				j.put("nilai", nilai);
				totalNilai += nilai;
				j.put("pajak", rs.getDouble(6));
				double kembali = rs.getDouble(7);
				j.put("dikembalikan", kembali);
				totalDikembalikan += kembali;
				j.put("dariSponsor", rs.getDouble(8));
				j.put("namaSponsor", rs.getString(9));
				j.put("statusDokumen", rs.getString(10));
				j.put("tanggalPembuatan", teks(rs.getTimestamp(11)));
				j.put("tanggalPersetujuan", teks(rs.getTimestamp(12)));
				j.put("tanggalStor", teks(rs.getTimestamp(13)));
				j.put("telahDikembalikan", rs.getBoolean(14));
				long v = rs.getLong(15);
				j.put("kasBesarId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("kasBesarKode", rs.getString(16));
				j.put("kasBesarNama", rs.getString(17));
				j.put("kasBesarNilai", rs.getDouble(18));
				v = rs.getLong(19);
				j.put("satuanKerjaId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("satuanKerjaNama", rs.getString(20));
				j.put("dibuatOleh", rs.getString(21));
				j.put("disetujuiOleh", rs.getString(22));
				rs.getLong(23);
				boolean jurnal = !rs.wasNull();
				rs.getLong(24);
				boolean jurnalPajak = !rs.wasNull();
				rs.getLong(25);
				boolean jurnalKembali = !rs.wasNull();
				j.put("sudahDijurnal", jurnal);
				j.put("sudahDijurnalPajak", jurnalPajak);
				j.put("sudahDijurnalPengembalian", jurnalKembali);
				String formula = rs.getString(26);
				j.put("rincian", formula == null || formula.trim().isEmpty() ? new JSONArray()
						: new JSONArray(formula));
				arr.put(j);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			// Status DPC ikut dikirim supaya layar tahu dokumen mana yang sudah
			// masuk kolam transfer bagian keuangan dan mana yang belum.
			TransferDpcUtil.lampirkanStatus(session, "pj_kas_besar", arr);
			hasil.put("data", arr);
			hasil.put("totalNilai", totalNilai);
			hasil.put("totalDikembalikan", totalDikembalikan);
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
			JSONArray pajak = new JSONArray();
			PreparedStatement ps = conn.prepareStatement(
					"SELECT id, COALESCE(kode,''), COALESCE(nama,''), COALESCE(persen,0)"
							+ " FROM asset.jenis_pajak_barang ORDER BY nama LIMIT 300");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("persen", rs.getDouble(4));
				pajak.put(j);
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
			status.put(PertangungjawabanKasBesar.PENGAJUAN);
			status.put(PertangungjawabanKasBesar.DISETUJU);
			status.put(PertangungjawabanKasBesar.DITOLAK);

			hasil.put("status", "00");
			hasil.put("jenisPajak", pajak);
			hasil.put("satuanKerja", satker);
			hasil.put("daftarStatus", status);
			hasil.put("pphMengurangiLpj", Common.bolehKonfigurasi("pph_mengurangi_lpj"));
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Cari dokumen Kas Besar yang boleh dipertanggungjawabkan: sudah DISETUJUI dan belum
	 * punya pertanggungjawaban. Saat menyunting, dokumen miliknya sendiri tetap ikut.
	 */
	public static void cariKasBesar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		long pjId = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					// formula kas besarnya ikut dikirim supaya rincian LPJ dapat langsung terisi
					// begitu kas besarnya dipilih -- LPJ melaporkan pemakaian atas rencana yang
					// sudah tertulis di kas besar itu, jadi mengetik ulang seluruh barisnya hanya
					// membuang waktu dan membuka peluang salah ketik.
					"SELECT k.id, COALESCE(k.kode,''), COALESCE(k.nama,''), COALESCE(k.nilai,0),"
							+ " COALESCE(sk.nama,''), k.tanggal_pengajuan, COALESCE(k.formula,'')"
							+ " FROM akunting.kas_besar k"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = k.satuan_kerja"
							+ " WHERE COALESCE(k.status,'') = ?"
							+ "   AND (k.pertangungjawaban_kas_besar IS NULL"
							+ (pjId > 0 ? " OR k.pertangungjawaban_kas_besar = ?" : "") + ")");
			if (!cari.isEmpty()) {
				sql.append(" AND (k.kode ILIKE ? OR k.nama ILIKE ?)");
			}
			sql.append(" ORDER BY k.id DESC LIMIT 100");
			PreparedStatement ps = conn.prepareStatement(sql.toString());
			int i = 1;
			ps.setString(i++, KasBesar.DISETUJU);
			if (pjId > 0) {
				ps.setLong(i++, pjId);
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
				j.put("satuanKerja", rs.getString(5));
				j.put("tanggal", teks(rs.getTimestamp(6)));
				String formulaKb = rs.getString(7);
				j.put("rincian", formulaKb == null || formulaKb.trim().isEmpty() ? new JSONArray()
						: new JSONArray(formulaKb));
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

	/** Pratinjau perhitungan rincian tanpa menyimpan -- rumus yang sama dgn LPJ uang muka. */
	public static void hitungPratinjau(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			boolean pph = Common.bolehKonfigurasi("pph_mengurangi_lpj");
			PertangungjawabanApiHelper.Hitungan h = PertangungjawabanApiHelper.hitung(session,
					request == null ? null : request.optJSONArray("rincian"), pph);
			hasil.put("status", "00");
			hasil.put("nilai", h.nilai);
			hasil.put("pajak", h.pajak);
			hasil.put("pphMengurangiLpj", pph);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== simpan

	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id <= 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah pertanggungjawaban kas besar."
					: "Anda tidak memiliki hak mengubah pertanggungjawaban kas besar.");
			return;
		}
		long kasBesarId = request.optLong("kasBesarId", 0);
		String nama = request.optString("nama", "").trim();
		Date tanggalStor = tanggal(request, "tanggalStor");
		String statusDokumen = request.optString("statusDokumen", PertangungjawabanKasBesar.PENGAJUAN).trim();
		JSONArray rincian = request.optJSONArray("rincian");

		if (kasBesarId <= 0) {
			tolak(hasil, "Kas Besar yang akan dipertanggungjawabkan belum dipilih.");
			return;
		}
		if (nama.isEmpty()) {
			tolak(hasil, "Judul Pengajuan belum diisi.");
			return;
		}
		if (PertangungjawabanKasBesar.DISETUJU.equals(statusDokumen) && !bolehAksi(tbmuser, "approve")) {
			tolak(hasil, "Anda tidak memiliki hak menyetujui pertanggungjawaban kas besar.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			KasBesar kb = (KasBesar) session.get(KasBesar.class, Long.valueOf(kasBesarId));
			if (kb == null) {
				tolak(hasil, "Kas Besar tidak ditemukan.");
				return;
			}
			PertangungjawabanKasBesar pj = baru ? new PertangungjawabanKasBesar()
					: (PertangungjawabanKasBesar) session.get(PertangungjawabanKasBesar.class, Long.valueOf(id));
			if (pj == null) {
				tolak(hasil, "Pertanggungjawaban tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			if (!baru && pj.getPostingHistory() != null) {
				tolak(hasil, "Pertanggungjawaban ini sudah dijurnal sehingga tidak boleh diubah lagi.");
				return;
			}

			boolean pph = Common.bolehKonfigurasi("pph_mengurangi_lpj");
			PertangungjawabanApiHelper.Hitungan h = PertangungjawabanApiHelper.hitung(session, rincian, pph);
			double nilaiKasBesar = kb.getNilai() == null ? 0 : kb.getNilai().doubleValue();
			// Layar ZK modul ini membandingkan dalam PECAHAN (bukan dibulatkan seperti LPJ
			// uang muka) -- perbedaan itu dipertahankan apa adanya.
			if (nilaiKasBesar < h.nilai) {
				tolak(hasil, "Nilai yang dipertanggungjawabkan (" + Common.numberFormat.get().format(h.nilai)
						+ ") melebihi nilai kas besar (" + Common.numberFormat.get().format(nilaiKasBesar) + ").");
				return;
			}
			double dikembalikan = nilaiKasBesar - h.nilai;
			if (Math.abs(dikembalikan) < 0.005D) {
				dikembalikan = 0D;
			}
			if (tanggalStor == null && dikembalikan > 0.1D) {
				tolak(hasil, "Tanggal Stor wajib diisi karena terdapat dana yang dikembalikan sebesar "
						+ Common.numberFormat.get().format(dikembalikan) + ".");
				return;
			}

			pj.setKasBesar(kb);
			pj.setNama(nama);
			pj.setKeterangan(request.optString("keterangan", "").trim());
			pj.setNilai(Double.valueOf(h.nilai));
			pj.setPajak(Double.valueOf(h.pajak));
			pj.setDikembalikan(Double.valueOf(dikembalikan));
			pj.setFormula(rincian == null ? "[]" : rincian.toString());
			pj.setNamaSponsor(request.optString("namaSponsor", "").trim());
			pj.setDariSponsor(Double.valueOf(request.optDouble("dariSponsor", 0)));
			pj.setTanggalStor(tanggalStor);
			long satkerId = request.optLong("satuanKerjaId", 0);
			if (satkerId > 0) {
				pj.setSatuanKerja((SatuanKerja) session.get(SatuanKerja.class, Long.valueOf(satkerId)));
			} else if (pj.getSatuanKerja() == null) {
				pj.setSatuanKerja(kb.getSatuanKerja());
			}
			if (pj.getDibuatOleh() == null) {
				pj.setDibuatOleh(tbmuser);
				pj.setTanggalPembuatan(WaktuUtil.getDate());
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(pj.getTanggalPembuatan() == null ? WaktuUtil.getDate() : pj.getTanggalPembuatan());
			pj.setTahun(Integer.valueOf(cal.get(Calendar.YEAR)));
			pj.setBulan(Integer.valueOf(cal.get(Calendar.MONTH) + 1));
			if (PertangungjawabanKasBesar.DISETUJU.equals(statusDokumen)) {
				pj.setDisetujuiOleh(tbmuser);
				Date tglSetuju = tanggal(request, "tanggalPersetujuan");
				pj.setTanggalPersetujuan(tglSetuju == null ? WaktuUtil.getDate() : tglSetuju);
			} else {
				pj.setDisetujuiOleh(null);
				pj.setTanggalPersetujuan(null);
			}
			pj.setStatus(statusDokumen);
			if (pj.getAktif() == null) {
				pj.setAktif(Boolean.TRUE);
			}
			if (tbmuser != null) {
				pj.setOleh(tbmuser.getUserNama());
				pj.setOlehId(tbmuser.getUserId());
			}

			session.beginTransaction();
			if (baru) {
				pj.setKode(buatKode(session));
			}
			session.saveOrUpdate(pj);
			// Tautan balik: kas besar menandai dirinya sudah dipertanggungjawabkan.
			kb.setPertangungjawabanKasBesar(pj);
			session.update(kb);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", pj.getId());
			hasil.put("kode", pj.getKode());
			hasil.put("nilai", h.nilai);
			hasil.put("pajak", h.pajak);
			hasil.put("message", baru ? "Pertanggungjawaban " + pj.getKode() + " dibuat."
					: "Pertanggungjawaban diperbarui.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pertanggungjawaban belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== status & hapus

	public static void ubahStatus(Tbmuser tbmuser, JSONObject request, JSONObject hasil, boolean setujui)
			throws Exception {
		if (!bolehAksi(tbmuser, setujui ? "approve" : "reject")) {
			tolak(hasil, setujui ? "Anda tidak memiliki hak menyetujui pertanggungjawaban kas besar."
					: "Anda tidak memiliki hak menolak pertanggungjawaban kas besar.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PertangungjawabanKasBesar pj = id <= 0 ? null
					: (PertangungjawabanKasBesar) session.get(PertangungjawabanKasBesar.class, Long.valueOf(id));
			if (pj == null) {
				tolak(hasil, "Pertanggungjawaban tidak ditemukan.");
				return;
			}
			if (pj.getPostingHistory() != null) {
				tolak(hasil, "Pertanggungjawaban ini sudah dijurnal sehingga statusnya tidak boleh diubah.");
				return;
			}
			session.beginTransaction();
			if (setujui) {
				pj.setStatus(PertangungjawabanKasBesar.DISETUJU);
				pj.setDisetujuiOleh(tbmuser);
				Date tgl = tanggal(request, "tanggalPersetujuan");
				pj.setTanggalPersetujuan(tgl == null ? WaktuUtil.getDate() : tgl);
			} else {
				pj.setStatus(PertangungjawabanKasBesar.DITOLAK);
				pj.setDisetujuiOleh(null);
				pj.setTanggalPersetujuan(null);
			}
			if (tbmuser != null) {
				pj.setOleh(tbmuser.getUserNama());
				pj.setOlehId(tbmuser.getUserId());
			}
			session.update(pj);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Pertanggungjawaban " + pj.getKode() + (setujui ? " disetujui." : " ditolak."));
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
			tolak(hasil, "Anda tidak memiliki hak menghapus pertanggungjawaban kas besar.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PertangungjawabanKasBesar pj = id <= 0 ? null
					: (PertangungjawabanKasBesar) session.get(PertangungjawabanKasBesar.class, Long.valueOf(id));
			if (pj == null) {
				tolak(hasil, "Pertanggungjawaban tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			if (pj.getPostingHistory() != null || pj.getPostingHistoryPajak() != null
					|| pj.getPostingHistoryPengembalian() != null) {
				tolak(hasil, "Pertanggungjawaban ini sudah dijurnal sehingga tidak boleh dihapus.");
				return;
			}
			if (pj.getDaftarPengajuanTransfer() != null) {
				tolak(hasil, "Pertanggungjawaban ini sudah masuk daftar pengajuan transfer sehingga tidak boleh dihapus.");
				return;
			}
			if (PertangungjawabanKasBesar.DISETUJU.equals(pj.getStatus())) {
				tolak(hasil, "Pertanggungjawaban yang sudah disetujui tidak boleh dihapus. Tolak dulu bila memang keliru.");
				return;
			}
			String kode = pj.getKode();
			session.beginTransaction();
			// Lepas tautan dari kas besar induknya supaya dokumen itu bisa
			// dipertanggungjawabkan ulang.
			Criteria c = session.createCriteria(KasBesar.class)
					.add(Restrictions.eq("pertangungjawabanKasBesar", pj));
			for (Object o : c.list()) {
				KasBesar kb = (KasBesar) o;
				kb.setPertangungjawabanKasBesar(null);
				session.update(kb);
			}
			session.delete(pj);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Pertanggungjawaban " + kode + " dihapus.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pertanggungjawaban tidak dapat dihapus karena masih berelasi dengan data lain.");
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== kode dokumen

	private static String buatKode(Session session) {
		try {
			if (NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR_DATA == null
					|| NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR_DATA.getNomorSurat() == null) {
				return Common.getGeneratedBarCode();
			}
			NomorSurat ns = NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_KAS_BESAR_DATA.getNomorSurat();
			Long index = Boolean.TRUE.equals(ns.getGunakanIndexUrut()) ? ns.getNomorIndex()
					: indexBerikutnya(session, ns);
			NomorSurat.tambahIndexNomorSurat(ns);
			String noAgenda = ns.format(index, WaktuUtil.getDate());
			return ais.action.master.KodeUnikUtil.pastikanUnik(PertangungjawabanKasBesar.class, noAgenda);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PertangungjawabanKasBesarApiHelper.buatKode");
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
		Criteria c = session.createCriteria(PertangungjawabanKasBesar.class)
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

	/** Dipakai dispatcher: seluruh aksi berawalan {@code pj_kas_besar_} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("pj_kas_besar_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("pj_kas_besar_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("pj_kas_besar_cari_kas_besar".equals(action)) {
			cariKasBesar(tbmuser, request, hasil);
			return true;
		}
		if ("pj_kas_besar_hitung".equals(action)) {
			hitungPratinjau(tbmuser, request, hasil);
			return true;
		}
		if ("pj_kas_besar_ajukan_transfer".equals(action)) {
			TransferDpcUtil.ajukan("pj_kas_besar", tbmuser, request, hasil);
			return true;
		}
		if ("pj_kas_besar_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("pj_kas_besar_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		if ("pj_kas_besar_setujui".equals(action)) {
			ubahStatus(tbmuser, request, hasil, true);
			return true;
		}
		if ("pj_kas_besar_tolak".equals(action)) {
			ubahStatus(tbmuser, request, hasil, false);
			return true;
		}
		return false;
	}
}
