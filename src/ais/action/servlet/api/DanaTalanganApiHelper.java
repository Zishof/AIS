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
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.JenisUangMuka;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.UangMuka;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.WaktuUtil;

/**
 * <h3>API JSON "Dana Talangan" untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan menu yang selama ini HANYA ada di layar ZK
 * ({@code ais.action.master.akunting.DanaTalanganAction}) ke Desktop/Android.</p>
 *
 * <p>Dana talangan adalah dana yang menjembatani satu <b>uang muka</b>: uang mukanya sudah
 * disetujui dan transfernya sudah benar-benar direalisasikan, tetapi dananya perlu ditalangi
 * lebih dulu dari sumber lain. Karena itu dua aturan berikut dipertahankan apa adanya dari
 * layar ZK, dan keduanya bukan validasi kosmetik:</p>
 *
 * <ol>
 * <li><b>Uang muka yang dapat dipilih hanya yang transfernya sudah terealisasi.</b> Mengikuti
 *     {@code AmbilDataUangMukaBanbox}: uang muka aktif, berstatus Disetujui, punya penyetuju,
 *     dan pengajuan transfernya sudah direalisasikan — lewat jalur transfer
 *     ({@code prosesTransfer.realisasikanOleh} terisi) atau jalur transitori
 *     ({@code transitoriData.transfer} terisi). Menalangi uang muka yang uangnya belum
 *     cair sama saja mencatat utang yang belum ada.</li>
 * <li><b>Sumber Dana Talangan wajib saat menyetujui, bukan saat menyimpan.</b> Di layar ZK
 *     pemeriksaannya memang hanya berjalan pada jalur persetujuan
 *     ({@code persetujuan &amp;&amp; setujui}) — pengajuan boleh disimpan dulu sebagai draf
 *     tanpa menentukan sumbernya.</li>
 * </ol>
 *
 * <p>Muaranya sama dengan modul Keuangan lain: dokumen yang disetujui diajukan ke DPC lewat
 * {@link TransferDpcUtil}, lalu dijurnal dari dasbor Draft Jurnal.</p>
 */
public final class DanaTalanganApiHelper {

	private static final String KUNCI = "dana_talangan";

	private DanaTalanganApiHelper() {
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

	private static String teksTanggal(java.util.Date t) {
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
					"SELECT d.id, COALESCE(d.kode,''), COALESCE(d.nama,''), COALESCE(d.keterangan,''),"
							+ " COALESCE(d.nilai,0), COALESCE(d.status,''),"
							+ " d.tanggal_pembuatan, d.tanggal_persetujuan,"
							+ " d.satuan_kerja, COALESCE(sk.nama,''),"
							+ " d.jenis_uang_muka, COALESCE(ju.nama,''),"
							+ " d.uang_muka, COALESCE(um.kode,''), COALESCE(um.nama,''),"
							+ " COALESCE(um.nilai,0), COALESCE(w.nama,''),"
							+ " COALESCE(d.dibuat_oleh,''), COALESCE(d.disetujui_oleh,''),"
							+ " d.posting_history, d.daftar_pengajuan_transfer, COALESCE(d.tahun,0)"
							+ " FROM public.dana_talangan d"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = d.satuan_kerja"
							+ " LEFT JOIN public.jenis_uang_muka ju ON ju.id = d.jenis_uang_muka"
							+ " LEFT JOIN public.uang_muka um ON um.id = d.uang_muka"
							+ " LEFT JOIN rab.workspace w ON w.id = um.workspace"
							+ " WHERE 1 = 1");
			if (!cari.isEmpty()) {
				sql.append(" AND (d.kode ILIKE ? OR d.nama ILIKE ? OR d.keterangan ILIKE ?)");
			}
			if (!statusFilter.isEmpty()) {
				sql.append(" AND COALESCE(d.status,'') = ?");
			}
			if (dari != null) {
				sql.append(" AND date(d.tanggal_pembuatan) >= ?");
			}
			if (sampai != null) {
				sql.append(" AND date(d.tanggal_pembuatan) <= ?");
			}
			if (satkerId != 0) {
				sql.append(" AND d.satuan_kerja = ?");
			}
			sql.append(" ORDER BY d.id DESC LIMIT ").append(batas);

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
				ps.setDate(i++, new java.sql.Date(dari.getTime()));
			}
			if (sampai != null) {
				ps.setDate(i++, new java.sql.Date(sampai.getTime()));
			}
			if (satkerId != 0) {
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
				j.put("statusDokumen", rs.getString(6));
				j.put("tanggalPembuatan", teksTanggal(rs.getTimestamp(7)));
				j.put("tanggalPersetujuan", teksTanggal(rs.getTimestamp(8)));
				long v = rs.getLong(9);
				j.put("satuanKerjaId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("satuanKerjaNama", rs.getString(10));
				v = rs.getLong(11);
				j.put("jenisUangMukaId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("jenisUangMukaNama", rs.getString(12));
				v = rs.getLong(13);
				j.put("uangMukaId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("uangMukaKode", rs.getString(14));
				j.put("uangMukaNama", rs.getString(15));
				j.put("uangMukaNilai", rs.getDouble(16));
				j.put("anggaran", rs.getString(17));
				j.put("dibuatOleh", rs.getString(18));
				j.put("disetujuiOleh", rs.getString(19));
				rs.getLong(20);
				j.put("sudahDijurnal", !rs.wasNull());
				rs.getLong(21);
				j.put("punyaTransfer", !rs.wasNull());
				j.put("tahun", rs.getInt(22));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			TransferDpcUtil.lampirkanStatus(session, KUNCI, arr);
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
			JSONArray jenis = new JSONArray();
			PreparedStatement ps = conn.prepareStatement(
					"SELECT id, COALESCE(nama,'') FROM public.jenis_uang_muka"
							+ " WHERE COALESCE(aktif,true) = true ORDER BY nama");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				jenis.put(j);
			}
			rs.close();
			ps.close();

			JSONArray satker = new JSONArray();
			ps = conn.prepareStatement("SELECT id, COALESCE(nama,'') FROM rab.satuan_kerja"
					+ " ORDER BY nama LIMIT 500");
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
			status.put(DanaTalangan.PENGAJUAN);
			status.put(DanaTalangan.DISETUJU);
			status.put(DanaTalangan.DITOLAK);

			hasil.put("status", "00");
			hasil.put("jenisUangMuka", jenis);
			hasil.put("satuanKerja", satker);
			hasil.put("daftarStatus", status);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================== pemilih uang muka

	/**
	 * Uang muka yang boleh ditalangi. Penyaringnya sama dengan banbox ZK
	 * ({@code AmbilDataUangMukaBanbox}): aktif, disetujui, dan pengajuan transfernya sudah
	 * DIREALISASIKAN — lewat jalur transfer maupun transitori.
	 */
	public static void cariUangMuka(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		long idDokumen = request == null ? 0 : request.optLong("id", 0);

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT um.id, COALESCE(um.kode,''), COALESCE(um.nama,''), COALESCE(um.nilai,0),"
							+ " COALESCE(w.kode,''), COALESCE(w.nama,''), COALESCE(sk.nama,''),"
							+ " COALESCE(ju.nama,''), um.mulai, um.sampai"
							+ " FROM public.uang_muka um"
							+ " JOIN akunting.daftar_pengajuan_transfer dpt ON dpt.uang_muka = um.id"
							+ " LEFT JOIN akunting.proses_transfer pt ON pt.id = dpt.proses_transfer"
							+ " LEFT JOIN akunting.transitori tr ON tr.id = dpt.transitori_data"
							+ " LEFT JOIN rab.workspace w ON w.id = um.workspace"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = um.satuan_kerja"
							+ " LEFT JOIN public.jenis_uang_muka ju ON ju.id = um.jenis_uang_muka"
							+ " WHERE COALESCE(um.aktif,true) = true"
							+ "   AND um.disetujui_oleh IS NOT NULL"
							+ "   AND COALESCE(um.status,'') = ?"
							// Realisasi lewat salah satu dari dua jalur, sama seperti banbox ZK.
							+ "   AND ( (COALESCE(dpt.transfer,false) = true"
							+ "          AND pt.realisasikan_oleh IS NOT NULL)"
							+ "      OR (COALESCE(dpt.transitori,false) = true"
							+ "          AND tr.transfer IS NOT NULL) )");
			if (!cari.isEmpty()) {
				sql.append(" AND (um.kode ILIKE ? OR um.nama ILIKE ? OR COALESCE(um.keterangan,'') ILIKE ?)");
			}
			sql.append(" ORDER BY um.id DESC LIMIT 200");

			PreparedStatement ps = conn.prepareStatement(sql.toString());
			int i = 1;
			ps.setString(i++, UangMuka.DISETUJU);
			if (!cari.isEmpty()) {
				String kw = "%" + cari + "%";
				ps.setString(i++, kw);
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
				j.put("anggaranKode", rs.getString(5));
				j.put("anggaran", rs.getString(6));
				j.put("satuanKerja", rs.getString(7));
				j.put("jenisUangMuka", rs.getString(8));
				j.put("mulai", teksTanggal(rs.getDate(9)));
				j.put("sampai", teksTanggal(rs.getDate(10)));
				arr.put(j);
			}
			rs.close();
			ps.close();

			// Dokumen yang sedang disunting tetap dapat melihat uang muka pilihannya,
			// meski keadaannya berubah sejak dokumen itu dibuat.
			if (idDokumen != 0) {
				ps = conn.prepareStatement(
						"SELECT um.id, COALESCE(um.kode,''), COALESCE(um.nama,''), COALESCE(um.nilai,0),"
								+ " COALESCE(w.kode,''), COALESCE(w.nama,''), COALESCE(sk.nama,''),"
								+ " COALESCE(ju.nama,''), um.mulai, um.sampai"
								+ " FROM public.dana_talangan d"
								+ " JOIN public.uang_muka um ON um.id = d.uang_muka"
								+ " LEFT JOIN rab.workspace w ON w.id = um.workspace"
								+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = um.satuan_kerja"
								+ " LEFT JOIN public.jenis_uang_muka ju ON ju.id = um.jenis_uang_muka"
								+ " WHERE d.id = ?");
				ps.setLong(1, idDokumen);
				rs = ps.executeQuery();
				while (rs.next()) {
					long id = rs.getLong(1);
					boolean sudahAda = false;
					for (int k = 0; k < arr.length(); k++) {
						if (arr.getJSONObject(k).optLong("id") == id) {
							sudahAda = true;
						}
					}
					if (!sudahAda) {
						JSONObject j = new JSONObject();
						j.put("id", id);
						j.put("kode", rs.getString(2));
						j.put("nama", rs.getString(3));
						j.put("nilai", rs.getDouble(4));
						j.put("anggaranKode", rs.getString(5));
						j.put("anggaran", rs.getString(6));
						j.put("satuanKerja", rs.getString(7));
						j.put("jenisUangMuka", rs.getString(8));
						j.put("mulai", teksTanggal(rs.getDate(9)));
						j.put("sampai", teksTanggal(rs.getDate(10)));
						j.put("pilihanTersimpan", true);
						arr.put(j);
					}
				}
				rs.close();
				ps.close();
			}

			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== simpan

	/**
	 * Simpan (tambah/ubah) satu pengajuan dana talangan. Urutan validasinya mengikuti
	 * {@code DanaTalanganAction.onSave} supaya pesan yang dilihat pengguna sama di semua kanal.
	 */
	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id == 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah pengajuan dana talangan."
					: "Anda tidak memiliki hak mengubah pengajuan dana talangan.");
			return;
		}

		long uangMukaId = request.optLong("uangMukaId", 0);
		String nama = request.optString("nama", "").trim();
		double nilai = request.optDouble("nilai", 0);
		long jenisId = request.optLong("jenisUangMukaId", 0);
		long satkerId = request.optLong("satuanKerjaId", 0);
		String statusDokumen = request.optString("statusDokumen", DanaTalangan.PENGAJUAN).trim();

		// --- urutan validasi disamakan dengan layar ZK
		if (uangMukaId == 0) {
			tolak(hasil, "Uang Muka belum dipilih.");
			return;
		}
		if (nama.isEmpty()) {
			tolak(hasil, "Judul Pengajuan belum diisi.");
			return;
		}
		if (nilai <= 0) {
			tolak(hasil, "Nilai belum diisi.");
			return;
		}
		// Sumber dana talangan hanya wajib pada jalur PERSETUJUAN -- draf boleh disimpan
		// tanpa menentukan sumbernya, sama seperti layar ZK.
		if (DanaTalangan.DISETUJU.equals(statusDokumen) && jenisId == 0) {
			tolak(hasil, "Sumber Dana Talangan belum dipilih.");
			return;
		}
		if (DanaTalangan.DISETUJU.equals(statusDokumen) && !bolehAksi(tbmuser, "approve")) {
			tolak(hasil, "Anda tidak memiliki hak menyetujui pengajuan dana talangan.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			UangMuka um = (UangMuka) session.get(UangMuka.class, Long.valueOf(uangMukaId));
			if (um == null) {
				tolak(hasil, "Uang Muka tidak ditemukan.");
				return;
			}
			DanaTalangan dt = baru ? new DanaTalangan()
					: (DanaTalangan) session.get(DanaTalangan.class, Long.valueOf(id));
			if (dt == null) {
				tolak(hasil, "Pengajuan tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			if (!baru && dt.getPostingHistory() != null) {
				tolak(hasil, "Pengajuan ini sudah dijurnal sehingga tidak boleh diubah lagi.");
				return;
			}

			dt.setUangMuka(um);
			dt.setNama(nama);
			dt.setNilai(Double.valueOf(nilai));
			dt.setKeterangan(request.optString("keterangan", "").trim());
			dt.setJenisUangMuka(jenisId == 0 ? null
					: (JenisUangMuka) session.get(JenisUangMuka.class, Long.valueOf(jenisId)));
			// Satuan kerja mengikuti uang mukanya bila tidak ditentukan sendiri.
			dt.setSatuanKerja(satkerId != 0
					? (SatuanKerja) session.get(SatuanKerja.class, Long.valueOf(satkerId))
					: um.getSatuanKerja());

			if (dt.getDibuatOleh() == null) {
				dt.setDibuatOleh(tbmuser);
				dt.setTanggalPembuatan(WaktuUtil.getDate());
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(dt.getTanggalPembuatan() == null ? WaktuUtil.getDate() : dt.getTanggalPembuatan());
			dt.setTahun(Integer.valueOf(cal.get(Calendar.YEAR)));
			dt.setBulan(Integer.valueOf(cal.get(Calendar.MONTH) + 1));

			if (DanaTalangan.DISETUJU.equals(statusDokumen)) {
				dt.setDisetujuiOleh(tbmuser);
				Date tglSetuju = tanggal(request, "tanggalPersetujuan");
				dt.setTanggalPersetujuan(tglSetuju == null ? WaktuUtil.getDate() : tglSetuju);
			} else {
				dt.setDisetujuiOleh(null);
				dt.setTanggalPersetujuan(null);
			}
			dt.setStatus(statusDokumen);
			if (dt.getAktif() == null) {
				dt.setAktif(Boolean.TRUE);
			}
			if (tbmuser != null) {
				dt.setOleh(tbmuser.getUserNama());
				dt.setOlehId(tbmuser.getUserId());
			}

			session.beginTransaction();
			if (baru) {
				dt.setKode(buatKode(session));
			}
			session.saveOrUpdate(dt);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", dt.getId());
			hasil.put("kode", dt.getKode());
			hasil.put("message", baru ? "Pengajuan dana talangan " + dt.getKode() + " dibuat."
					: "Pengajuan dana talangan diperbarui.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pengajuan belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Penomoran dokumen memakai alur yang sama dengan layar ZK
	 * ({@code NomorSuratAlurKeuangan.DANA_TALANGAN_DATA}), termasuk aturan resetnya.
	 */
	private static String buatKode(Session session) {
		try {
			if (NomorSuratAlurKeuangan.DANA_TALANGAN_DATA == null
					|| NomorSuratAlurKeuangan.DANA_TALANGAN_DATA.getNomorSurat() == null) {
				return Common.getGeneratedBarCode();
			}
			NomorSurat ns = NomorSuratAlurKeuangan.DANA_TALANGAN_DATA.getNomorSurat();
			Long index = Boolean.TRUE.equals(ns.getGunakanIndexUrut()) ? NomorSurat.ambilLaluTambahIndexNomorSurat(ns)
					: indexBerikutnya(session, ns);
			String noAgenda = ns.format(index, WaktuUtil.getDate());
			return ais.action.master.KodeUnikUtil.pastikanUnik(DanaTalangan.class, noAgenda);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit DanaTalanganApiHelper.buatKode");
			return Common.getGeneratedBarCode();
		}
	}

	/** Nomor urut berikutnya, mengikuti lingkup reset yang dipilih pada nomor suratnya. */
	private static Long indexBerikutnya(Session session, NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return Long.valueOf(0);
		}
		int tahun = WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Criteria c = session.createCriteria(DanaTalangan.class)
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
		c.add(nomorSurat.getResetTiap() != null
				? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
				: Restrictions.sqlRestriction("true"));
		Number n = (Number) c.setProjection(Projections.rowCount()).uniqueResult();
		return Long.valueOf((n == null ? 0L : n.longValue()) + 1L);
	}

	// ==================================================================== persetujuan

	public static void ubahStatus(Tbmuser tbmuser, JSONObject request, JSONObject hasil, boolean setujui)
			throws Exception {
		if (!bolehAksi(tbmuser, setujui ? "approve" : "reject")) {
			tolak(hasil, setujui ? "Anda tidak memiliki hak menyetujui pengajuan dana talangan."
					: "Anda tidak memiliki hak menolak pengajuan dana talangan.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			DanaTalangan dt = id == 0 ? null : (DanaTalangan) session.get(DanaTalangan.class, Long.valueOf(id));
			if (dt == null) {
				tolak(hasil, "Pengajuan tidak ditemukan.");
				return;
			}
			if (dt.getPostingHistory() != null) {
				tolak(hasil, "Pengajuan ini sudah dijurnal sehingga statusnya tidak boleh diubah.");
				return;
			}
			// Sumber dana talangan menentukan akun kredit jurnalnya, jadi wajib ada
			// sebelum disetujui -- sama seperti layar ZK.
			if (setujui && dt.getJenisUangMuka() == null) {
				tolak(hasil, "Sumber Dana Talangan belum dipilih, belum dapat disetujui.");
				return;
			}

			session.beginTransaction();
			if (setujui) {
				dt.setStatus(DanaTalangan.DISETUJU);
				dt.setDisetujuiOleh(tbmuser);
				Date tgl = tanggal(request, "tanggalPersetujuan");
				dt.setTanggalPersetujuan(tgl == null ? WaktuUtil.getDate() : tgl);
			} else {
				dt.setStatus(DanaTalangan.DITOLAK);
				dt.setDisetujuiOleh(null);
				dt.setTanggalPersetujuan(null);
			}
			if (tbmuser != null) {
				dt.setOleh(tbmuser.getUserNama());
				dt.setOlehId(tbmuser.getUserId());
			}
			session.update(dt);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Pengajuan " + dt.getKode() + (setujui ? " disetujui." : " ditolak."));
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Status belum dapat diubah: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== hapus

	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus pengajuan dana talangan.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			DanaTalangan dt = id == 0 ? null : (DanaTalangan) session.get(DanaTalangan.class, Long.valueOf(id));
			if (dt == null) {
				tolak(hasil, "Pengajuan tidak ditemukan.");
				return;
			}
			if (dt.getPostingHistory() != null) {
				tolak(hasil, "Pengajuan ini sudah dijurnal sehingga tidak boleh dihapus.");
				return;
			}
			if (DanaTalangan.DISETUJU.equals(dt.getStatus())) {
				tolak(hasil, "Pengajuan yang sudah disetujui tidak boleh dihapus. Tolak dulu bila memang keliru.");
				return;
			}
			if (dt.getDaftarPengajuanTransfer() != null) {
				tolak(hasil, "Pengajuan ini sudah masuk daftar pengajuan transfer sehingga tidak boleh dihapus.");
				return;
			}

			session.beginTransaction();
			session.delete(dt);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Pengajuan dana talangan dihapus.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pengajuan belum dapat dihapus: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== dispatcher

	/** Dipakai dispatcher: seluruh aksi berawalan {@code dana_talangan_} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("dana_talangan_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("dana_talangan_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("dana_talangan_cari_uang_muka".equals(action)) {
			cariUangMuka(tbmuser, request, hasil);
			return true;
		}
		if ("dana_talangan_ajukan_transfer".equals(action)) {
			TransferDpcUtil.ajukan(KUNCI, tbmuser, request, hasil);
			return true;
		}
		if ("dana_talangan_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("dana_talangan_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		if ("dana_talangan_setujui".equals(action)) {
			ubahStatus(tbmuser, request, hasil, true);
			return true;
		}
		if ("dana_talangan_tolak".equals(action)) {
			ubahStatus(tbmuser, request, hasil, false);
			return true;
		}
		return false;
	}
}
