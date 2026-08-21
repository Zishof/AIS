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
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.JenisUangMuka;
import ais.database.model.surat.NomorSurat;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.UangMuka;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.ui.util.WaktuUtil;

/**
 * <h3>API JSON "Uang Muka (Cash Advance)" untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan menu yang selama ini HANYA ada di layar ZK
 * ({@code ais.action.master.akunting.UangMukaAction}) ke Desktop/Android, dengan layar ZK
 * sebagai RUJUKAN: urutan validasi, aturan wajib-isi, cara kode dokumen dibuat, pemeriksaan
 * sisa saldo anggaran, dan arti tiap status dibuat SAMA supaya dokumen yang dibuat lewat
 * kanal mana pun berperilaku identik.</p>
 *
 * <p>Aturan yang dipertahankan apa adanya dari layar ZK:</p>
 * <ul>
 * <li>Satuan Kerja wajib bila dokumen <i>tanpa anggaran</i> atau <i>diambil dari PR</i>.</li>
 * <li>Akun wajib bila <i>tanpa anggaran</i>; Anggaran (workspace) wajib bila BUKAN tanpa
 *     anggaran dan BUKAN dari PR.</li>
 * <li>Judul, Tanggal Mulai, Tanggal Sampai, Tanggal Laporan, dan Nilai wajib diisi.</li>
 * <li>Bila konfigurasi {@code saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran}
 *     menyala, nilai pengajuan tidak boleh melebihi sisa saldo anggaran
 *     ({@link ais.action.master.akunting.JenisUangMukaAction#hitungSaldo}) -- perhitungan
 *     yang sama persis dipakai, bukan rumus tiruan.</li>
 * <li>Kode dokumen memakai Nomor Surat Alur Keuangan yang sama, termasuk aturan reset
 *     urutan per tahun/bulan/kelompok, dan dipastikan unik.</li>
 * <li>Status "Disetujui" mengisi penyetuju dan tanggal persetujuan; status lain
 *     mengosongkannya kembali.</li>
 * </ul>
 *
 * <p>Yang DITAMBAHKAN dibanding layar ZK, karena kanal ini dipakai perangkat lapangan:
 * gerbang hak akses granular per aksi ({@code uang_muka} pada grid CRUD
 * {@code TbmroleAction}) dan pengaman hapus -- dokumen yang sudah disetujui, sudah punya
 * pertanggungjawaban, sudah dijurnal, atau sudah masuk pengajuan transfer TIDAK boleh
 * dihapus, karena dokumen turunannya akan kehilangan induk.</p>
 *
 * <p>Baca memakai SQL native (ringan, tidak menyeret graf Hibernate); tulis memakai session
 * Hibernate agar tetap ter-audit Envers seperti layar ZK.</p>
 */
public final class UangMukaApiHelper {

	/** Kunci menu pada {@link EbisnisMenuKatalog} -- dipakai gerbang aksi granular. */
	private static final String KUNCI = "uang_muka";

	private UangMukaApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	/**
	 * Gerbang aksi granular per menu (padanan grid CRUD {@code TbmroleAction}). Server yang
	 * menjadi gerbang sebenarnya: klien boleh menyembunyikan tombol, tapi permintaan yang tetap
	 * dikirim akan ditolak di sini.
	 */
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
			ais.common.ErrorAuditUtil.record(e, "auto-audit UangMukaApiHelper.batalkanDiam");
		}
	}

	/** Tanggal dari string {@code yyyy-MM-dd}; null bila kosong/tidak terbaca. */
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

	private static String teksTanggal(java.sql.Timestamp t) {
		return t == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(t);
	}

	private static String teksTanggal(java.sql.Date d) {
		return d == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd").format(d);
	}

	// ==================================================================== daftar

	/**
	 * Daftar pengajuan uang muka. Seluruh penyaring bersifat opsional dan dipakai bersamaan
	 * (AND), meniru kepala halaman layar ZK: {@code cari} (kode/judul), {@code status},
	 * {@code dari}/{@code sampai} (rentang Tanggal Mulai), {@code satuanKerjaId}, {@code tahun},
	 * dan {@code belumLpj} (hanya yang belum dipertanggungjawabkan).
	 */
	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		String statusFilter = request == null ? "" : request.optString("statusFilter", "").trim();
		Date dari = tanggal(request, "dari");
		Date sampai = tanggal(request, "sampai");
		long satkerId = request == null ? 0 : request.optLong("satuanKerjaId", 0);
		int tahun = request == null ? 0 : request.optInt("tahun", 0);
		boolean belumLpj = request != null && request.optBoolean("belumLpj", false);
		int batas = request == null ? 200 : request.optInt("limit", 200);
		if (batas <= 0 || batas > 1000) {
			batas = 200;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT m.id, COALESCE(m.kode,''), COALESCE(m.nama,''), COALESCE(m.keterangan,''),"
							+ " COALESCE(m.nilai,0), COALESCE(m.saldo,0), COALESCE(m.status,''),"
							+ " m.mulai, m.sampai, m.selesai, m.tanggal_pembuatan, m.tanggal_persetujuan,"
							+ " COALESCE(m.tanpaanggaran,false), COALESCE(m.ambildaripr,false),"
							+ " m.satuan_kerja, COALESCE(sk.nama,''),"
							+ " m.jenis_uang_muka, COALESCE(ju.nama,''),"
							+ " m.workspace, COALESCE(w.nama,''),"
							+ " m.akun, COALESCE(a.kode,''), COALESCE(a.nama,''),"
							+ " COALESCE(m.dibuat_oleh,''), COALESCE(m.disetujui_oleh,''),"
							+ " m.pertangungjawaban, m.posting_history, m.daftar_pengajuan_transfer,"
							+ " COALESCE(m.tahun,0)"
							+ " FROM public.uang_muka m"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = m.satuan_kerja"
							+ " LEFT JOIN public.jenis_uang_muka ju ON ju.id = m.jenis_uang_muka"
							+ " LEFT JOIN rab.workspace w ON w.id = m.workspace"
							+ " LEFT JOIN akunting.akun a ON a.id = m.akun"
							+ " WHERE 1 = 1");
			if (!cari.isEmpty()) {
				sql.append(" AND (m.kode ILIKE ? OR m.nama ILIKE ? OR m.keterangan ILIKE ?)");
			}
			if (!statusFilter.isEmpty()) {
				sql.append(" AND COALESCE(m.status,'') = ?");
			}
			if (dari != null) {
				sql.append(" AND m.mulai >= ?");
			}
			if (sampai != null) {
				sql.append(" AND m.mulai <= ?");
			}
			if (satkerId > 0) {
				sql.append(" AND m.satuan_kerja = ?");
			}
			if (tahun > 0) {
				sql.append(" AND COALESCE(m.tahun,0) = ?");
			}
			if (belumLpj) {
				sql.append(" AND m.pertangungjawaban IS NULL");
			}
			sql.append(" ORDER BY m.id DESC LIMIT ").append(batas);

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
			if (satkerId > 0) {
				ps.setLong(i++, satkerId);
			}
			if (tahun > 0) {
				ps.setInt(i++, tahun);
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
				j.put("mulai", teksTanggal(rs.getDate(8)));
				j.put("sampai", teksTanggal(rs.getDate(9)));
				j.put("selesai", teksTanggal(rs.getDate(10)));
				j.put("tanggalPembuatan", teksTanggal(rs.getTimestamp(11)));
				j.put("tanggalPersetujuan", teksTanggal(rs.getTimestamp(12)));
				j.put("tanpaAnggaran", rs.getBoolean(13));
				j.put("ambilDariPr", rs.getBoolean(14));
				long v = rs.getLong(15);
				j.put("satuanKerjaId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("satuanKerjaNama", rs.getString(16));
				v = rs.getLong(17);
				j.put("jenisUangMukaId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("jenisUangMukaNama", rs.getString(18));
				v = rs.getLong(19);
				j.put("workspaceId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("workspaceNama", rs.getString(20));
				v = rs.getLong(21);
				j.put("akunId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("akunKode", rs.getString(22));
				j.put("akunNama", rs.getString(23));
				j.put("dibuatOleh", rs.getString(24));
				j.put("disetujuiOleh", rs.getString(25));
				rs.getLong(26);
				j.put("punyaLpj", !rs.wasNull());
				rs.getLong(27);
				j.put("sudahDijurnal", !rs.wasNull());
				rs.getLong(28);
				j.put("punyaTransfer", !rs.wasNull());
				j.put("tahun", rs.getInt(29));
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

	// ==================================================================== opsi formulir

	/**
	 * Isi dropdown formulir: Jenis Uang Muka, Satuan Kerja, daftar status, dan apakah
	 * konfigurasi "saldo harus cukup" menyala (menentukan apakah klien perlu menampilkan
	 * sisa saldo sebelum menyimpan).
	 */
	public static void opsi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			JSONArray jenis = new JSONArray();
			PreparedStatement ps = conn.prepareStatement(
					"SELECT id, COALESCE(kode,''), COALESCE(nama,''), COALESCE(defaultdata,false)"
							+ " FROM public.jenis_uang_muka WHERE COALESCE(aktif,true) = true ORDER BY nama");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("bawaan", rs.getBoolean(4));
				jenis.put(j);
			}
			rs.close();
			ps.close();

			JSONArray satker = new JSONArray();
			ps = conn.prepareStatement(
					"SELECT id, COALESCE(nama,'') FROM rab.satuan_kerja ORDER BY nama LIMIT 500");
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
			status.put(UangMuka.PENGAJUAN);
			status.put(UangMuka.DISETUJU);
			status.put(UangMuka.DITOLAK);

			hasil.put("status", "00");
			hasil.put("jenisUangMuka", jenis);
			hasil.put("satuanKerja", satker);
			hasil.put("daftarStatus", status);
			hasil.put("saldoHarusCukup",
					Common.bolehKonfigurasi("saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran"));
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Sisa saldo anggaran untuk satu workspace pada satu tanggal -- memakai perhitungan yang
	 * SAMA dengan layar ZK ({@code JenisUangMukaAction.hitungSaldo}), termasuk pengecualian
	 * dokumen yang sedang disunting supaya nilainya sendiri tidak dihitung dua kali.
	 */
	public static void saldoAnggaran(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long workspaceId = request == null ? 0 : request.optLong("workspaceId", 0);
		long idKecuali = request == null ? 0 : request.optLong("id", 0);
		Date pada = tanggal(request, "tanggal");
		if (workspaceId <= 0) {
			hasil.put("status", "00");
			hasil.put("saldo", 0);
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Workspace w = (Workspace) session.get(Workspace.class, Long.valueOf(workspaceId));
			if (w == null) {
				tolak(hasil, "Anggaran tidak ditemukan.");
				return;
			}
			Double saldo = ais.action.master.akunting.JenisUangMukaAction.hitungSaldo(
					idKecuali > 0 ? Long.valueOf(idKecuali) : null, null, null, null, w,
					pada == null ? WaktuUtil.getDate() : pada);
			hasil.put("status", "00");
			hasil.put("saldo", saldo == null ? 0 : saldo.doubleValue());
			hasil.put("anggaran", w.getNama());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== simpan

	/**
	 * Simpan (tambah/ubah) satu pengajuan uang muka. Urutan validasinya mengikuti
	 * {@code UangMukaAction.onSave} supaya pesan yang dilihat pengguna sama di semua kanal.
	 */
	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id <= 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah pengajuan uang muka."
					: "Anda tidak memiliki hak mengubah pengajuan uang muka.");
			return;
		}

		boolean tanpaAnggaran = request.optBoolean("tanpaAnggaran", false);
		boolean ambilDariPr = request.optBoolean("ambilDariPr", false);
		long satkerId = request.optLong("satuanKerjaId", 0);
		String nama = request.optString("nama", "").trim();
		long akunId = request.optLong("akunId", 0);
		long workspaceId = request.optLong("workspaceId", 0);
		Date mulai = tanggal(request, "mulai");
		Date sampai = tanggal(request, "sampai");
		Date selesai = tanggal(request, "selesai");
		double nilai = request.optDouble("nilai", 0);
		String statusDokumen = request.optString("statusDokumen", UangMuka.PENGAJUAN).trim();

		// --- urutan validasi disamakan dengan layar ZK
		if (satkerId <= 0 && (tanpaAnggaran || ambilDariPr)) {
			tolak(hasil, "Satuan Kerja belum dipilih.");
			return;
		}
		if (nama.isEmpty()) {
			tolak(hasil, "Judul Pengajuan belum diisi.");
			return;
		}
		if (akunId <= 0 && tanpaAnggaran) {
			tolak(hasil, "Akun belum dipilih.");
			return;
		}
		if (workspaceId <= 0 && !tanpaAnggaran && !ambilDariPr) {
			tolak(hasil, "Anggaran belum dipilih.");
			return;
		}
		if (mulai == null) {
			tolak(hasil, "Tanggal Mulai belum diisi.");
			return;
		}
		if (sampai == null) {
			tolak(hasil, "Tanggal Sampai belum diisi.");
			return;
		}
		if (selesai == null) {
			tolak(hasil, "Tanggal Laporan belum diisi.");
			return;
		}
		if (nilai <= 0) {
			tolak(hasil, "Nilai belum diisi.");
			return;
		}
		if (UangMuka.DISETUJU.equals(statusDokumen) && !bolehAksi(tbmuser, "approve")) {
			tolak(hasil, "Anda tidak memiliki hak menyetujui pengajuan uang muka.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			UangMuka um = baru ? new UangMuka() : (UangMuka) session.get(UangMuka.class, Long.valueOf(id));
			if (um == null) {
				tolak(hasil, "Pengajuan tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			if (!baru && um.getPostingHistory() != null) {
				tolak(hasil, "Pengajuan ini sudah dijurnal sehingga tidak boleh diubah lagi.");
				return;
			}

			Workspace workspace = null;
			if (!tanpaAnggaran && workspaceId > 0) {
				workspace = (Workspace) session.get(Workspace.class, Long.valueOf(workspaceId));
				if (workspace == null) {
					tolak(hasil, "Anggaran tidak ditemukan.");
					return;
				}
			}

			// Pemeriksaan sisa saldo: HANYA bila konfigurasinya menyala, dan hanya untuk
			// dokumen yang membebani anggaran (bukan tanpa anggaran / bukan dari PR).
			if (!tanpaAnggaran && !ambilDariPr
					&& Common.bolehKonfigurasi("saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran")) {
				Double sisa = ais.action.master.akunting.JenisUangMukaAction.hitungSaldo(
						baru ? null : Long.valueOf(id), null, null, null, workspace, mulai);
				double sisaD = sisa == null ? 0 : sisa.doubleValue();
				if (sisaD < nilai) {
					tolak(hasil, "Nilai yang diajukan tidak boleh melebihi sisa saldo. Sisa saldo: "
							+ Common.numberFormat.get().format(sisaD) + ", nilai pengajuan: "
							+ Common.numberFormat.get().format(nilai));
					return;
				}
				um.setSaldo(Double.valueOf(sisaD));
			}

			um.setTanpaAnggaran(Boolean.valueOf(tanpaAnggaran));
			um.setAmbilDariPr(Boolean.valueOf(ambilDariPr));
			um.setWorkspace(tanpaAnggaran ? null : workspace);
			um.setNama(nama);
			um.setKeterangan(request.optString("keterangan", "").trim());
			um.setNilai(Double.valueOf(nilai));
			um.setMulai(mulai);
			um.setSampai(sampai);
			um.setSelesai(selesai);
			um.setAkun(akunId > 0 ? (Akun) session.get(Akun.class, Long.valueOf(akunId)) : null);
			um.setSatuanKerja(satkerId > 0
					? (SatuanKerja) session.get(SatuanKerja.class, Long.valueOf(satkerId)) : null);
			long jenisId = request.optLong("jenisUangMukaId", 0);
			um.setJenisUangMuka(jenisId > 0
					? (JenisUangMuka) session.get(JenisUangMuka.class, Long.valueOf(jenisId)) : null);

			Calendar cal = Calendar.getInstance();
			cal.setTime(mulai);
			um.setTahun(Integer.valueOf(cal.get(Calendar.YEAR)));
			um.setBulan(Integer.valueOf(cal.get(Calendar.MONTH) + 1));

			if (um.getDibuatOleh() == null) {
				um.setDibuatOleh(tbmuser);
				um.setTanggalPembuatan(WaktuUtil.getDate());
			}
			// Status "Disetujui" mencatat penyetuju; status lain mengosongkannya kembali --
			// perilaku yang sama dengan layar ZK.
			if (UangMuka.DISETUJU.equals(statusDokumen)) {
				um.setDisetujuiOleh(tbmuser);
				Date tglSetuju = tanggal(request, "tanggalPersetujuan");
				um.setTanggalPersetujuan(tglSetuju == null ? WaktuUtil.getDate() : tglSetuju);
			} else {
				um.setDisetujuiOleh(null);
				um.setTanggalPersetujuan(null);
			}
			um.setStatus(statusDokumen);
			if (um.getAktif() == null) {
				um.setAktif(Boolean.TRUE);
			}
			if (tbmuser != null) {
				um.setOleh(tbmuser.getUserNama());
				um.setOlehId(tbmuser.getUserId());
			}

			session.beginTransaction();
			if (baru) {
				um.setKode(buatKode(session));
			}
			session.saveOrUpdate(um);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", um.getId());
			hasil.put("kode", um.getKode());
			hasil.put("message", baru ? "Pengajuan uang muka " + um.getKode() + " dibuat."
					: "Pengajuan uang muka diperbarui.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pengajuan belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== persetujuan

	/** Setujui / tolak satu pengajuan tanpa membuka formulirnya (padanan tombol di layar ZK). */
	public static void ubahStatus(Tbmuser tbmuser, JSONObject request, JSONObject hasil, boolean setujui)
			throws Exception {
		if (!bolehAksi(tbmuser, setujui ? "approve" : "reject")) {
			tolak(hasil, setujui ? "Anda tidak memiliki hak menyetujui pengajuan uang muka."
					: "Anda tidak memiliki hak menolak pengajuan uang muka.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			UangMuka um = id <= 0 ? null : (UangMuka) session.get(UangMuka.class, Long.valueOf(id));
			if (um == null) {
				tolak(hasil, "Pengajuan tidak ditemukan.");
				return;
			}
			if (um.getPostingHistory() != null) {
				tolak(hasil, "Pengajuan ini sudah dijurnal sehingga statusnya tidak boleh diubah.");
				return;
			}
			if (setujui && um.getJenisUangMuka() == null) {
				// Sama dgn layar ZK: persetujuan menentukan akun penerima, jadi jenisnya wajib.
				tolak(hasil, "Akun Penerima (Jenis Uang Muka) belum dipilih, belum dapat disetujui.");
				return;
			}
			session.beginTransaction();
			if (setujui) {
				um.setStatus(UangMuka.DISETUJU);
				um.setDisetujuiOleh(tbmuser);
				Date tgl = tanggal(request, "tanggalPersetujuan");
				um.setTanggalPersetujuan(tgl == null ? WaktuUtil.getDate() : tgl);
			} else {
				um.setStatus(UangMuka.DITOLAK);
				um.setDisetujuiOleh(null);
				um.setTanggalPersetujuan(null);
			}
			if (tbmuser != null) {
				um.setOleh(tbmuser.getUserNama());
				um.setOlehId(tbmuser.getUserId());
			}
			session.update(um);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Pengajuan " + um.getKode() + (setujui ? " disetujui." : " ditolak."));
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Status belum dapat diubah: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== hapus

	/**
	 * Hapus satu pengajuan. Pengaman sengaja LEBIH KETAT daripada layar ZK karena kanal ini
	 * dipakai perangkat lapangan: dokumen yang sudah disetujui, sudah punya pertanggungjawaban,
	 * sudah dijurnal, atau sudah masuk pengajuan transfer akan membuat dokumen turunannya
	 * kehilangan induk bila dihapus.
	 */
	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus pengajuan uang muka.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			UangMuka um = id <= 0 ? null : (UangMuka) session.get(UangMuka.class, Long.valueOf(id));
			if (um == null) {
				tolak(hasil, "Pengajuan tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			if (um.getPertangungjawaban() != null) {
				tolak(hasil, "Pengajuan ini sudah punya pertanggungjawaban sehingga tidak boleh dihapus.");
				return;
			}
			if (um.getPostingHistory() != null) {
				tolak(hasil, "Pengajuan ini sudah dijurnal sehingga tidak boleh dihapus.");
				return;
			}
			if (um.getDaftarPengajuanTransfer() != null) {
				tolak(hasil, "Pengajuan ini sudah masuk daftar pengajuan transfer sehingga tidak boleh dihapus.");
				return;
			}
			if (UangMuka.DISETUJU.equals(um.getStatus())) {
				tolak(hasil, "Pengajuan yang sudah disetujui tidak boleh dihapus. Tolak dulu bila memang keliru.");
				return;
			}
			String kode = um.getKode();
			session.beginTransaction();
			session.delete(um);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Pengajuan " + kode + " dihapus.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pengajuan tidak dapat dihapus karena masih berelasi dengan data lain.");
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== kode dokumen

	/**
	 * Kode dokumen memakai Nomor Surat Alur Keuangan yang SAMA dengan layar ZK, termasuk
	 * aturan reset urutan (per tahun/bulan/kelompok/tanggal reset) dan pemastian keunikan.
	 * Bila alur nomor surat belum dikonfigurasi, jatuh ke barcode acak seperti ZK.
	 */
	private static String buatKode(Session session) {
		try {
			if (NomorSuratAlurKeuangan.UANG_MUKA_DATA == null
					|| NomorSuratAlurKeuangan.UANG_MUKA_DATA.getNomorSurat() == null) {
				return Common.getGeneratedBarCode();
			}
			NomorSurat ns = NomorSuratAlurKeuangan.UANG_MUKA_DATA.getNomorSurat();
			Long index = Boolean.TRUE.equals(ns.getGunakanIndexUrut()) ? ns.getNomorIndex()
					: indexBerikutnya(session, ns);
			NomorSurat.tambahIndexNomorSurat(ns);
			String noAgenda = ns.format(index, WaktuUtil.getDate());
			return ais.action.master.KodeUnikUtil.pastikanUnik(UangMuka.class, noAgenda);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit UangMukaApiHelper.buatKode");
			return Common.getGeneratedBarCode();
		}
	}

	/** Salinan aturan lingkup penomoran {@code UangMukaAction.getindex}. */
	private static Long indexBerikutnya(Session session, NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return Long.valueOf(0);
		}
		int tahun = WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Criteria c = session.createCriteria(UangMuka.class)
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

	/**
	 * Cari anggaran (workspace) untuk dipilih di formulir. Hanya baris DAUN yang boleh dipilih
	 * -- sama dengan layar ZK, karena hanya daun yang memegang nilai anggaran; baris induk
	 * cuma pengelompok. Hasilnya dibatasi agar aman dipakai perangkat lapangan.
	 */
	public static void cariAnggaran(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		// Satu sumber pencarian anggaran untuk seluruh grup Keuangan, supaya daftar yang
		// dilihat pengguna di Uang Muka dan di rincian kas benar-benar sama.
		AnggaranKeuanganUtil.cari(request, hasil);
	}

	// ==================================================================== dispatcher

	/** Dipakai dispatcher: seluruh aksi berawalan {@code uang_muka_} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("uang_muka_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("uang_muka_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("uang_muka_cari_anggaran".equals(action)) {
			cariAnggaran(tbmuser, request, hasil);
			return true;
		}
		if ("uang_muka_saldo".equals(action)) {
			saldoAnggaran(tbmuser, request, hasil);
			return true;
		}
		if ("uang_muka_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("uang_muka_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		if ("uang_muka_setujui".equals(action)) {
			ubahStatus(tbmuser, request, hasil, true);
			return true;
		}
		if ("uang_muka_tolak".equals(action)) {
			ubahStatus(tbmuser, request, hasil, false);
			return true;
		}
		return false;
	}
}
