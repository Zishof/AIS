package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.JenisReimbursement;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.ReimbursementPegawai;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.WaktuUtil;

/**
 * <h3>API JSON "Reimbursement Pegawai" untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan menu yang selama ini HANYA ada di layar ZK
 * ({@code ais.action.master.akunting.ReimbursementPegawaiAction}) ke Desktop/Android.</p>
 *
 * <p>Berbeda dengan modul Keuangan lain yang hanya mengenal Pengajuan/Disetujui/Ditolak,
 * reimbursement punya <b>lima</b> status: {@code Diajukan}, {@code Revisi},
 * {@code Ditolak}, {@code Disetujui}, dan {@code Lunas}. Status <b>Revisi</b> itu yang
 * membedakannya — atasan dapat mengembalikan pengajuan untuk diperbaiki pengaju, bukan
 * hanya menerima atau menolak.</p>
 *
 * <p>Tiga aturan khas modul ini dipertahankan apa adanya dari layar ZK:</p>
 * <ol>
 * <li><b>Anggaran wajib atau tidak ditentukan oleh JENISNYA.</b> Bila
 *     {@code jenisReimbursement.menggunakanAnggaran} menyala, anggaran wajib dipilih.
 *     Bila tidak, yang wajib justru <b>akun pada jenisnya</b> — dan bila admin belum
 *     melengkapinya, pengajuan ditolak dengan pesan yang menyebut siapa yang harus
 *     melengkapi.</li>
 * <li><b>Tiap baris rincian wajib punya akun dan jumlah &gt; 0.</b> Akun itu diturunkan
 *     dari Jenis Pengeluaran; bila jenisnya dipilih tetapi akunnya belum dipetakan admin,
 *     pesannya membedakan kedua keadaan itu — "belum dipetakan" berbeda dengan "belum
 *     dipilih".</li>
 * <li><b>Nilai dokumen dihitung dari rincian</b> ({@code nominal = Σ jumlah}), bukan
 *     diketik sendiri, dan minimal satu baris rincian harus valid.</li>
 * </ol>
 *
 * <p>Muaranya sama dengan modul Keuangan lain: dokumen berstatus Disetujui diajukan ke
 * DPC lewat {@link TransferDpcUtil}.</p>
 */
public final class ReimbursementApiHelper {

	private static final String KUNCI = "reimbursement";

	private ReimbursementApiHelper() {
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

	// ==================================================================== rincian

	/** Alasan rincian ditolak, atau null bila sudah benar. Urutan pesannya mengikuti ZK. */
	static String masalahRincian(JSONArray rincian) {
		int valid = 0;
		for (int i = 0; rincian != null && i < rincian.length(); i++) {
			JSONObject b = rincian.optJSONObject(i);
			if (b == null || b.length() == 0) {
				continue;
			}
			if (b.optLong("akun", 0) == 0) {
				// Dua keadaan yang berbeda dan harus dibedakan: jenisnya sudah dipilih
				// tetapi akunnya belum dipetakan admin, versus jenisnya memang belum dipilih.
				if (b.optLong("jenisPengeluaran", 0) != 0) {
					return "Akun untuk Jenis Pengeluaran pada rincian item belum dipetakan oleh administrator."
							+ " Mohon admin melengkapi akun pada tab \"Jenis Pengeluaran\" terlebih dahulu.";
				}
				return "Setiap baris rincian wajib memilih Jenis Pengeluaran.";
			}
			if (b.optDouble("jumlah", 0) <= 0) {
				return "Jumlah pada rincian item harus lebih dari 0 (isi qty dan harga).";
			}
			valid++;
		}
		if (valid == 0) {
			return "Rincian barang/biaya minimal satu baris.";
		}
		return null;
	}

	/** Nilai dokumen dihitung dari rinciannya, bukan diketik sendiri. */
	static double hitungRincian(JSONArray rincian) {
		double total = 0;
		for (int i = 0; rincian != null && i < rincian.length(); i++) {
			JSONObject b = rincian.optJSONObject(i);
			if (b == null || b.optLong("akun", 0) == 0) {
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
		long pegawaiId = request == null ? 0 : request.optLong("pegawaiId", 0);
		int batas = request == null ? 200 : request.optInt("limit", 200);
		if (batas <= 0 || batas > 1000) {
			batas = 200;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT r.id, COALESCE(r.kode,''), COALESCE(r.nama,''), COALESCE(r.keterangan,''),"
							+ " COALESCE(r.nominal,0), COALESCE(r.status,''),"
							+ " r.tanggal_pengeluaran, r.tanggal_pengajuan, r.tanggal_persetujuan,"
							+ " r.pegawai, COALESCE(pg.nama,''),"
							+ " r.jenis_reimbursement, COALESCE(jr.nama,''),"
							+ " r.workspace, COALESCE(w.nama,''), COALESCE(r.tanpaanggaran,false),"
							+ " r.satuan_kerja, COALESCE(sk.nama,''),"
							+ " COALESCE(r.kategori,''), COALESCE(r.catatan_pengaju,''),"
							+ " COALESCE(r.catatan_atasan,''), COALESCE(r.pajak_persen,0),"
							+ " r.posting_pengeluaran, r.daftar_pengajuan_transfer, COALESCE(r.formula,'[]')"
							+ " FROM akunting.reimbursement_pegawai r"
							+ " LEFT JOIN public.pegawai pg ON pg.id = r.pegawai"
							+ " LEFT JOIN akunting.jenis_reimbursement jr ON jr.id = r.jenis_reimbursement"
							+ " LEFT JOIN rab.workspace w ON w.id = r.workspace"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = r.satuan_kerja"
							+ " WHERE 1 = 1");
			if (!cari.isEmpty()) {
				sql.append(" AND (r.kode ILIKE ? OR r.nama ILIKE ? OR COALESCE(r.keterangan,'') ILIKE ?)");
			}
			if (!statusFilter.isEmpty()) {
				sql.append(" AND COALESCE(r.status,'') = ?");
			}
			if (dari != null) {
				sql.append(" AND date(r.tanggal_pengeluaran) >= ?");
			}
			if (sampai != null) {
				sql.append(" AND date(r.tanggal_pengeluaran) <= ?");
			}
			if (pegawaiId != 0) {
				sql.append(" AND r.pegawai = ?");
			}
			sql.append(" ORDER BY r.id DESC LIMIT ").append(batas);

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
			if (pegawaiId != 0) {
				ps.setLong(i++, pegawaiId);
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
				j.put("tanggalPengeluaran", teksTanggal(rs.getDate(7)));
				j.put("tanggalPengajuan", teksTanggal(rs.getTimestamp(8)));
				j.put("tanggalPersetujuan", teksTanggal(rs.getTimestamp(9)));
				long v = rs.getLong(10);
				j.put("pegawaiId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("pegawaiNama", rs.getString(11));
				v = rs.getLong(12);
				j.put("jenisReimbursementId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("jenisReimbursementNama", rs.getString(13));
				v = rs.getLong(14);
				j.put("workspaceId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("workspaceNama", rs.getString(15));
				j.put("tanpaAnggaran", rs.getBoolean(16));
				v = rs.getLong(17);
				j.put("satuanKerjaId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("satuanKerjaNama", rs.getString(18));
				j.put("kategori", rs.getString(19));
				j.put("catatanPengaju", rs.getString(20));
				j.put("catatanAtasan", rs.getString(21));
				j.put("pajakPersen", rs.getDouble(22));
				rs.getLong(23);
				j.put("sudahDijurnal", !rs.wasNull());
				rs.getLong(24);
				j.put("punyaTransfer", !rs.wasNull());
				try {
					j.put("rincian", new JSONArray(rs.getString(25)));
				} catch (Exception e) {
					j.put("rincian", new JSONArray());
				}
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
					"SELECT jr.id, COALESCE(jr.nama,''), COALESCE(jr.menggunakan_anggaran,false),"
							+ " jr.akun, COALESCE(a.kode,''), COALESCE(a.nama,'')"
							+ " FROM akunting.jenis_reimbursement jr"
							+ " LEFT JOIN akunting.akun a ON a.id = jr.akun"
							+ " WHERE COALESCE(jr.aktif,true) = true ORDER BY jr.nama");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				// Menentukan wajib-tidaknya anggaran pada formulir; dikirim supaya layar
				// tidak perlu menebak dan server tetap memeriksanya ulang.
				j.put("menggunakanAnggaran", rs.getBoolean(3));
				long v = rs.getLong(4);
				j.put("akunId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("akunKode", rs.getString(5));
				j.put("akunNama", rs.getString(6));
				jenis.put(j);
			}
			rs.close();
			ps.close();

			JSONArray pengeluaran = new JSONArray();
			ps = conn.prepareStatement(
					"SELECT jp.id, COALESCE(jp.nama,''), jp.akun, COALESCE(a.kode,''), COALESCE(a.nama,'')"
							+ " FROM akunting.jenis_pengeluaran jp"
							+ " LEFT JOIN akunting.akun a ON a.id = jp.akun"
							+ " WHERE COALESCE(jp.aktif,true) = true ORDER BY jp.nama");
			rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				long v = rs.getLong(3);
				j.put("akunId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("akunKode", rs.getString(4));
				j.put("akunNama", rs.getString(5));
				pengeluaran.put(j);
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
			status.put(ReimbursementPegawai.DIAJUKAN);
			status.put(ReimbursementPegawai.REVISI);
			status.put(ReimbursementPegawai.DISETUJUI);
			status.put(ReimbursementPegawai.DITOLAK);
			status.put(ReimbursementPegawai.LUNAS);

			hasil.put("status", "00");
			hasil.put("jenisReimbursement", jenis);
			hasil.put("jenisPengeluaran", pengeluaran);
			hasil.put("satuanKerja", satker);
			hasil.put("daftarStatus", status);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================== pemilih pegawai

	public static void cariPegawai(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT p.id, COALESCE(p.nama,''), COALESCE(sk.nama,'')"
							+ " FROM public.pegawai p"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = p.satuan_kerja"
							+ " WHERE COALESCE(p.aktif,true) = true");
			if (!cari.isEmpty()) {
				sql.append(" AND p.nama ILIKE ?");
			}
			sql.append(" ORDER BY p.nama LIMIT 200");
			PreparedStatement ps = conn.prepareStatement(sql.toString());
			if (!cari.isEmpty()) {
				ps.setString(1, "%" + cari + "%");
			}
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				j.put("satuanKerja", rs.getString(3));
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
	 * Simpan (tambah/ubah) satu pengajuan reimbursement. Urutan validasinya mengikuti
	 * {@code ReimbursementPegawaiAction.onSave} supaya pesan yang dilihat pengguna sama
	 * di semua kanal.
	 */
	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id == 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah pengajuan reimbursement."
					: "Anda tidak memiliki hak mengubah pengajuan reimbursement.");
			return;
		}

		long jenisId = request.optLong("jenisReimbursementId", 0);
		String nama = request.optString("nama", "").trim();
		long workspaceId = request.optLong("workspaceId", 0);
		long pegawaiId = request.optLong("pegawaiId", 0);
		Date tglKeluar = tanggal(request, "tanggalPengeluaran");
		JSONArray rincian = request.optJSONArray("rincian");

		if (jenisId == 0) {
			tolak(hasil, "Jenis Reimbursement wajib dipilih.");
			return;
		}
		if (nama.isEmpty()) {
			tolak(hasil, "Judul pengajuan wajib diisi.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JenisReimbursement jenis = (JenisReimbursement) session.get(JenisReimbursement.class,
					Long.valueOf(jenisId));
			if (jenis == null) {
				tolak(hasil, "Jenis Reimbursement tidak ditemukan.");
				return;
			}
			// Jenisnya yang menentukan apakah pengajuan membebani anggaran atau tidak.
			boolean pakaiAnggaran = Boolean.TRUE.equals(jenis.getMenggunakanAnggaran());

			Workspace workspace = null;
			if (pakaiAnggaran) {
				if (workspaceId != 0) {
					workspace = (Workspace) session.get(Workspace.class, Long.valueOf(workspaceId));
				}
				if (workspace == null) {
					tolak(hasil, "Anggaran wajib dipilih dari daftar untuk Jenis Reimbursement \""
							+ jenis.getNama() + "\".");
					return;
				}
			} else if (jenis.getAkun() == null || jenis.getAkun().getId() == null) {
				tolak(hasil, "Akun pada Jenis Reimbursement \"" + jenis.getNama()
						+ "\" belum ditentukan. Mohon administrator melengkapi akun pada tab"
						+ " Jenis Reimbursement terlebih dahulu.");
				return;
			}

			if (pegawaiId == 0) {
				tolak(hasil, "Pegawai penerima wajib dipilih dari daftar.");
				return;
			}
			ais.database.model.Pegawai pegawai = (ais.database.model.Pegawai) session
					.get(ais.database.model.Pegawai.class, Long.valueOf(pegawaiId));
			if (pegawai == null) {
				tolak(hasil, "Pegawai penerima tidak ditemukan.");
				return;
			}
			if (tglKeluar == null) {
				tolak(hasil, "Tanggal pengeluaran wajib diisi.");
				return;
			}
			String masalah = masalahRincian(rincian);
			if (masalah != null) {
				tolak(hasil, masalah);
				return;
			}

			ReimbursementPegawai r = baru ? new ReimbursementPegawai()
					: (ReimbursementPegawai) session.get(ReimbursementPegawai.class, Long.valueOf(id));
			if (r == null) {
				tolak(hasil, "Pengajuan tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			if (!baru && r.getPostingPengeluaran() != null) {
				tolak(hasil, "Pengajuan ini sudah dijurnal sehingga tidak boleh diubah lagi.");
				return;
			}
			// Pengajuan yang sudah disetujui atau lunas dikunci, sama seperti layar ZK yang
			// menyembunyikan tombol Ubah/Hapus pada kedua status itu.
			if (!baru && (ReimbursementPegawai.DISETUJUI.equals(r.getStatus())
					|| ReimbursementPegawai.LUNAS.equals(r.getStatus()))) {
				tolak(hasil, "Pengajuan berstatus \"" + r.getStatus() + "\" tidak boleh diubah lagi.");
				return;
			}

			double nominal = hitungRincian(rincian);

			r.setJenisReimbursement(jenis);
			r.setNama(nama);
			r.setKeterangan(request.optString("keterangan", "").trim());
			r.setDeskripsi(request.optString("deskripsi", "").trim());
			r.setKategori(request.optString("kategori", "").trim());
			r.setCatatanPengaju(request.optString("catatanPengaju", "").trim());
			r.setPegawai(pegawai);
			r.setTanggalPengeluaran(tglKeluar);
			r.setFormula(rincian == null ? "[]" : rincian.toString());
			r.setNominal(Double.valueOf(nominal));
			r.setPajakPersen(Double.valueOf(request.optDouble("pajakPersen", 0)));
			r.setDibayarPegawai(Boolean.valueOf(request.optBoolean("dibayarPegawai", true)));
			r.setTanpaAnggaran(Boolean.valueOf(!pakaiAnggaran));
			r.setWorkspace(pakaiAnggaran ? workspace : null);
			// Tanpa anggaran, akun biayanya diambil dari jenisnya.
			r.setAkun(pakaiAnggaran ? null : jenis.getAkun());
			long satkerId = request.optLong("satuanKerjaId", 0);
			r.setSatuanKerja(satkerId != 0
					? (SatuanKerja) session.get(SatuanKerja.class, Long.valueOf(satkerId))
					: (workspace == null ? r.getSatuanKerja() : workspace.getSatuanKerja()));

			if (r.getDibuatOleh() == null) {
				r.setDibuatOleh(tbmuser);
				r.setTanggalPengajuan(WaktuUtil.getDate());
			}
			// Menyimpan selalu mengembalikan dokumen ke antrean persetujuan: pengajuan yang
			// tadinya "Revisi" kembali menjadi "Diajukan" begitu pengaju memperbaikinya.
			r.setStatus(ReimbursementPegawai.DIAJUKAN);
			r.setDisetujuiOleh(null);
			r.setTanggalPersetujuan(null);
			if (r.getAktif() == null) {
				r.setAktif(Boolean.TRUE);
			}

			session.beginTransaction();
			if (baru) {
				r.setKode(buatKode(session));
			}
			session.saveOrUpdate(r);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", r.getId());
			hasil.put("kode", r.getKode());
			hasil.put("nilai", nominal);
			hasil.put("message", baru ? "Pengajuan reimbursement " + r.getKode() + " dibuat."
					: "Pengajuan reimbursement diperbarui dan kembali masuk antrean persetujuan.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pengajuan belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== kode dokumen

	/**
	 * Kode dokumen memakai catalog Nomor Surat yang SAMA dengan layar ZK
	 * ({@code ReimbursementPegawaiAction.generateCode}): coba
	 * {@code NomorSuratAlurPengadaan.REIMBURSEMENT_PEGAWAI_DATA} lebih dulu, lalu fallback ke
	 * {@code NomorSuratAlurKeuangan.REIMBURSEMENT_DATA}, dan terakhir format bawaan
	 * RMB-yyyyMM-urut -- supaya dokumen yang dibuat lewat REST (Desktop/Android) tidak
	 * bentrok/dobel dengan yang dibuat lewat ZK.
	 */
	private static String buatKode(Session session) {
		long count = 0;
		try {
			Number n = (Number) session.createCriteria(ReimbursementPegawai.class)
					.setProjection(Projections.rowCount()).uniqueResult();
			count = n == null ? 0 : n.longValue();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementApiHelper.buatKode");
		}

		try {
			NomorSurat ns = null;
			if (NomorSuratAlurPengadaan.REIMBURSEMENT_PEGAWAI_DATA != null) {
				ns = NomorSuratAlurPengadaan.REIMBURSEMENT_PEGAWAI_DATA.getNomorSurat();
			}
			if (ns == null && NomorSuratAlurKeuangan.REIMBURSEMENT_DATA != null) {
				ns = NomorSuratAlurKeuangan.REIMBURSEMENT_DATA.getNomorSurat();
			}
			if (ns != null) {
				Long index = Boolean.TRUE.equals(ns.getGunakanIndexUrut())
						? NomorSurat.ambilLaluTambahIndexNomorSurat(ns)
						: Long.valueOf(count + 1);
				String noAgenda = ns.format(index, WaktuUtil.getDate());
				return ais.action.master.KodeUnikUtil.pastikanUnik(ReimbursementPegawai.class, noAgenda);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementApiHelper.buatKode-kustom");
		}

		String prefix = "RMB-" + new java.text.SimpleDateFormat("yyyyMM").format(WaktuUtil.getDate()) + "-";
		return ais.action.master.KodeUnikUtil.pastikanUnik(ReimbursementPegawai.class, prefix + (count + 1));
	}

	// ==================================================================== keputusan

	/**
	 * Tiga keputusan atasan: {@code setujui}, {@code tolak}, dan {@code revisi}.
	 *
	 * <p>"Revisi" itu yang membedakan modul ini dari modul Keuangan lain — pengajuan
	 * dikembalikan kepada pengaju untuk diperbaiki, bukan ditutup. Catatan atasan wajib
	 * pada penolakan maupun permintaan revisi, karena tanpa alasan pengaju tidak tahu apa
	 * yang harus diperbaiki.</p>
	 */
	public static void keputusan(Tbmuser tbmuser, JSONObject request, JSONObject hasil, String jenisKeputusan)
			throws Exception {
		boolean setujui = "setujui".equals(jenisKeputusan);
		if (!bolehAksi(tbmuser, setujui ? "approve" : "reject")) {
			tolak(hasil, setujui ? "Anda tidak memiliki hak menyetujui pengajuan reimbursement."
					: "Anda tidak memiliki hak menolak atau meminta revisi pengajuan reimbursement.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		String catatan = request == null ? "" : request.optString("catatanAtasan", "").trim();
		if (!setujui && catatan.isEmpty()) {
			tolak(hasil, "Catatan atasan wajib diisi agar pengaju tahu apa yang harus diperbaiki.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ReimbursementPegawai r = id == 0 ? null
					: (ReimbursementPegawai) session.get(ReimbursementPegawai.class, Long.valueOf(id));
			if (r == null) {
				tolak(hasil, "Pengajuan tidak ditemukan.");
				return;
			}
			if (r.getPostingPengeluaran() != null) {
				tolak(hasil, "Pengajuan ini sudah dijurnal sehingga statusnya tidak boleh diubah.");
				return;
			}
			if (ReimbursementPegawai.LUNAS.equals(r.getStatus())) {
				tolak(hasil, "Pengajuan yang sudah lunas tidak boleh diubah statusnya.");
				return;
			}

			session.beginTransaction();
			if (setujui) {
				r.setStatus(ReimbursementPegawai.DISETUJUI);
				r.setDisetujuiOleh(tbmuser);
				Date tgl = tanggal(request, "tanggalPersetujuan");
				r.setTanggalPersetujuan(tgl == null ? WaktuUtil.getDate() : tgl);
			} else {
				r.setStatus("revisi".equals(jenisKeputusan) ? ReimbursementPegawai.REVISI
						: ReimbursementPegawai.DITOLAK);
				r.setDisetujuiOleh(null);
				r.setTanggalPersetujuan(null);
			}
			if (!catatan.isEmpty()) {
				r.setCatatanAtasan(catatan);
			}
			r.setDiputuskanOleh(tbmuser);
			r.setTanggalKeputusan(WaktuUtil.getDate());
			session.update(r);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("statusDokumen", r.getStatus());
			hasil.put("message", "Pengajuan " + r.getKode() + " kini berstatus " + r.getStatus() + ".");
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
			tolak(hasil, "Anda tidak memiliki hak menghapus pengajuan reimbursement.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ReimbursementPegawai r = id == 0 ? null
					: (ReimbursementPegawai) session.get(ReimbursementPegawai.class, Long.valueOf(id));
			if (r == null) {
				tolak(hasil, "Pengajuan tidak ditemukan.");
				return;
			}
			if (r.getPostingPengeluaran() != null) {
				tolak(hasil, "Pengajuan ini sudah dijurnal sehingga tidak boleh dihapus.");
				return;
			}
			// Sama dengan layar ZK: tombol Hapus hanya muncul selama status BUKAN
			// Disetujui atau Lunas.
			if (ReimbursementPegawai.DISETUJUI.equals(r.getStatus())
					|| ReimbursementPegawai.LUNAS.equals(r.getStatus())) {
				tolak(hasil, "Pengajuan berstatus \"" + r.getStatus() + "\" tidak boleh dihapus.");
				return;
			}
			if (r.getDaftarPengajuanTransfer() != null) {
				tolak(hasil, "Pengajuan ini sudah masuk daftar pengajuan transfer sehingga tidak boleh dihapus.");
				return;
			}

			session.beginTransaction();
			session.delete(r);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Pengajuan reimbursement dihapus.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pengajuan belum dapat dihapus: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== dispatcher

	/** Dipakai dispatcher: seluruh aksi berawalan {@code reimbursement_} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("reimbursement_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("reimbursement_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("reimbursement_cari_pegawai".equals(action)) {
			cariPegawai(tbmuser, request, hasil);
			return true;
		}
		if ("reimbursement_cari_anggaran".equals(action)) {
			AnggaranKeuanganUtil.cari(request, hasil);
			return true;
		}
		if ("reimbursement_saldo_anggaran".equals(action)) {
			AnggaranKeuanganUtil.saldo(request, hasil);
			return true;
		}
		if ("reimbursement_ajukan_transfer".equals(action)) {
			TransferDpcUtil.ajukan(KUNCI, tbmuser, request, hasil);
			return true;
		}
		if ("reimbursement_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("reimbursement_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		if ("reimbursement_setujui".equals(action)) {
			keputusan(tbmuser, request, hasil, "setujui");
			return true;
		}
		if ("reimbursement_tolak".equals(action)) {
			keputusan(tbmuser, request, hasil, "tolak");
			return true;
		}
		if ("reimbursement_revisi".equals(action)) {
			keputusan(tbmuser, request, hasil, "revisi");
			return true;
		}
		return false;
	}
}
