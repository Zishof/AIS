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
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.WaktuUtil;

/**
 * <h3>API JSON "Pertanggungjawaban Uang Muka (LPJ)" untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan menu yang selama ini HANYA ada di layar ZK
 * ({@code ais.action.master.akunting.PertangungjawabanAction}) ke Desktop/Android, dengan
 * layar ZK sebagai RUJUKAN.</p>
 *
 * <p><b>Inti modul ini adalah perhitungan nilai LPJ dari rincian.</b> Rincian disimpan apa
 * adanya sebagai JSON pada kolom {@code formula} -- bentuk yang sama dengan layar ZK -- dan
 * nilainya DIHITUNG ULANG DI SERVER dengan rumus yang sama persis, bukan menerima angka
 * kiriman klien:</p>
 * <pre>
 *   pajak baris = persen JenisPajakBarang / 100 x jumlah
 *   total baris = (jumlah + ppn% x jumlah) - (pph_mengurangi_lpj ? pajak baris : 0)
 *   nilai LPJ   = jumlah seluruh total baris
 *   pajak LPJ   = jumlah seluruh pajak baris
 * </pre>
 *
 * <p>Aturan yang dipertahankan apa adanya dari layar ZK:</p>
 * <ul>
 * <li>Uang Muka wajib dipilih; Judul wajib diisi.</li>
 * <li>Tanggal Stor wajib bila ada dana yang dikembalikan.</li>
 * <li>Nilai yang dipertanggungjawabkan TIDAK boleh melebihi nilai uang mukanya.</li>
 * <li>Status "Disetujui" mengisi penyetuju dan tanggal persetujuan; status lain
 *     mengosongkannya kembali.</li>
 * <li>Setelah tersimpan, uang muka yang bersangkutan ditautkan balik ke LPJ ini
 *     sehingga daftar uang muka tahu dokumennya sudah dipertanggungjawabkan.</li>
 * <li>Kode dokumen memakai Nomor Surat Alur Keuangan yang sama beserta aturan resetnya.</li>
 * </ul>
 */
public final class PertangungjawabanApiHelper {

	/** Kunci menu pada {@link EbisnisMenuKatalog} -- dipakai gerbang aksi granular. */
	private static final String KUNCI = "pj_uang_muka";

	private PertangungjawabanApiHelper() {
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
			ais.common.ErrorAuditUtil.record(e, "auto-audit PertangungjawabanApiHelper.batalkanDiam");
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

	// ==================================================================== hitung rincian

	/** Hasil perhitungan rincian LPJ: nilai bersih dan total pajaknya. */
	public static final class Hitungan {
		public double nilai;
		public double pajak;
	}

	/**
	 * Hitung nilai & pajak LPJ dari rincian. Rumusnya SAMA PERSIS dengan
	 * {@code PertangungjawabanAction.onSave}, termasuk pengaruh konfigurasi
	 * {@code pph_mengurangi_lpj} yang menentukan apakah PPh memotong nilai LPJ.
	 */
	public static Hitungan hitung(Session session, JSONArray rincian, boolean pphMengurangi) {
		Hitungan h = new Hitungan();
		for (int i = 0; rincian != null && i < rincian.length(); i++) {
			JSONObject b = rincian.optJSONObject(i);
			if (b == null) {
				continue;
			}
			double jumlah = b.optDouble("jumlah", 0);
			double ppn = b.optDouble("ppn", 0);
			double persenPajak = 0;
			long idPajak = b.optLong("pajak", 0);
			if (idPajak > 0) {
				JenisPajakBarang jp = (JenisPajakBarang) session.get(JenisPajakBarang.class, Long.valueOf(idPajak));
				if (jp != null && jp.getPersen() != null) {
					persenPajak = jp.getPersen().doubleValue();
				}
			}
			double pajakBaris = (persenPajak / 100.0) * jumlah;
			double totalBaris = (jumlah + ((ppn / 100.0) * jumlah)) - (pphMengurangi ? pajakBaris : 0.0);
			h.nilai += totalBaris;
			h.pajak += pajakBaris;
		}
		return h;
	}

	// ==================================================================== daftar

	/** Daftar LPJ beserta uang muka induknya. Penyaring opsional dan digabung (AND). */
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
							+ " p.uang_muka, COALESCE(um.kode,''), COALESCE(um.nama,''), COALESCE(um.nilai,0),"
							+ " p.satuan_kerja, COALESCE(sk.nama,''),"
							+ " COALESCE(p.dibuat_oleh,''), COALESCE(p.disetujui_oleh,''),"
							+ " p.posting_history, p.posting_history_pajak, p.posting_history_pengembalian,"
							+ " COALESCE(p.formula,'')"
							+ " FROM akunting.pertangungjawaban p"
							+ " LEFT JOIN public.uang_muka um ON um.id = p.uang_muka"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = p.satuan_kerja"
							+ " WHERE 1 = 1");
			if (!cari.isEmpty()) {
				sql.append(" AND (p.kode ILIKE ? OR p.nama ILIKE ? OR COALESCE(um.kode,'') ILIKE ?)");
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
				j.put("uangMukaId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(v));
				j.put("uangMukaKode", rs.getString(16));
				j.put("uangMukaNama", rs.getString(17));
				j.put("uangMukaNilai", rs.getDouble(18));
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
			TransferDpcUtil.lampirkanStatus(session, "pj_uang_muka", arr);
			hasil.put("data", arr);
			hasil.put("totalNilai", totalNilai);
			hasil.put("totalDikembalikan", totalDikembalikan);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== opsi

	/** Isi dropdown formulir: jenis pajak (PPh) beserta persennya, status, dan konfigurasi. */
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
			status.put(Pertangungjawaban.PENGAJUAN);
			status.put(Pertangungjawaban.DISETUJU);
			status.put(Pertangungjawaban.DITOLAK);

			hasil.put("status", "00");
			hasil.put("jenisPajak", pajak);
			hasil.put("satuanKerja", satker);
			hasil.put("daftarStatus", status);
			// Menentukan apakah PPh memotong nilai LPJ -- klien menampilkan rumusnya agar
			// pengguna paham dari mana angka totalnya berasal.
			hasil.put("pphMengurangiLpj", Common.bolehKonfigurasi("pph_mengurangi_lpj"));
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Cari seluruh uang muka untuk pemilih LPJ. Data yang belum memenuhi syarat tetap
	 * dikirim agar pengguna mengetahui statusnya, tetapi ditandai tidak dapat dipilih.
	 */
	public static void cariUangMuka(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		long lpjId = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT m.id, COALESCE(m.kode,''), COALESCE(m.nama,''), COALESCE(m.nilai,0),"
							+ " COALESCE(sk.nama,''), m.mulai, m.sampai, COALESCE(m.aktif,true),"
							+ " COALESCE(m.status,''), m.disetujui_oleh, m.pertangungjawaban,"
							+ " EXISTS (SELECT 1 FROM akunting.daftar_pengajuan_transfer d1 WHERE d1.uang_muka=m.id),"
							+ " EXISTS (SELECT 1 FROM akunting.daftar_pengajuan_transfer d2"
							+ " LEFT JOIN akunting.proses_transfer pt ON pt.id=d2.proses_transfer"
							+ " LEFT JOIN akunting.transitori tr ON tr.id=d2.transitori_data"
							+ " WHERE d2.uang_muka=m.id AND ((COALESCE(d2.transfer,false)=true"
							+ " AND pt.realisasikan_oleh IS NOT NULL) OR (COALESCE(d2.transitori,false)=true"
							+ " AND tr.transfer IS NOT NULL)))"
							+ " FROM public.uang_muka m"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = m.satuan_kerja"
							+ " WHERE 1=1");
			if (!cari.isEmpty()) {
				sql.append(" AND (m.kode ILIKE ? OR m.nama ILIKE ?)");
			}
			sql.append(" ORDER BY m.id DESC LIMIT 200");
			PreparedStatement ps = conn.prepareStatement(sql.toString());
			int i = 1;
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
				j.put("mulai", rs.getDate(6) == null ? "" : rs.getDate(6).toString());
				j.put("sampai", rs.getDate(7) == null ? "" : rs.getDate(7).toString());
				boolean aktif = rs.getBoolean(8);
				String status = rs.getString(9);
				boolean disetujui = UangMuka.DISETUJU.equals(status) && rs.getObject(10) != null;
				long lpjTerpakai = rs.getLong(11);
				boolean punyaLpj = !rs.wasNull();
				boolean pilihanTersimpan = lpjId > 0 && punyaLpj && lpjTerpakai == lpjId;
				boolean sudahMasukDpc = rs.getBoolean(12);
				boolean dpcDirealisasikan = rs.getBoolean(13);
				boolean dapatDipilih = pilihanTersimpan
						|| (aktif && disetujui && !punyaLpj && sudahMasukDpc && dpcDirealisasikan);
				String alasan = "";
				String statusPemilihan = pilihanTersimpan ? "Pilihan saat ini" : "Tersedia";
				if (!dapatDipilih) {
					if (!aktif) {
						alasan = "Uang muka tidak aktif.";
					} else if (!disetujui) {
						alasan = "Belum disetujui.";
					} else if (!sudahMasukDpc) {
						alasan = "Belum masuk Proses Transfer (DPC).";
					} else if (!dpcDirealisasikan) {
						alasan = "Proses Transfer (DPC) belum direalisasikan.";
					} else if (punyaLpj) {
						alasan = "Sudah dipakai pada pertanggungjawaban lain.";
					}
					statusPemilihan = "Tidak dapat dipilih";
				}
				j.put("dapatDipilih", dapatDipilih);
				j.put("pilihanTersimpan", pilihanTersimpan);
				j.put("statusPemilihan", statusPemilihan);
				j.put("alasanTidakDapatDipilih", alasan);
				j.put("statusUangMuka", status);
				j.put("sudahMasukDpc", sudahMasukDpc);
				j.put("dpcDirealisasikan", dpcDirealisasikan);
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

	/** Pratinjau perhitungan rincian tanpa menyimpan -- dipakai klien saat mengetik. */
	public static void hitungPratinjau(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			boolean pph = Common.bolehKonfigurasi("pph_mengurangi_lpj");
			Hitungan h = hitung(session, request == null ? null : request.optJSONArray("rincian"), pph);
			hasil.put("status", "00");
			hasil.put("nilai", h.nilai);
			hasil.put("pajak", h.pajak);
			hasil.put("pphMengurangiLpj", pph);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== simpan

	/** Simpan (tambah/ubah) satu LPJ. Urutan validasinya mengikuti layar ZK. */
	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id <= 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah pertanggungjawaban."
					: "Anda tidak memiliki hak mengubah pertanggungjawaban.");
			return;
		}
		long uangMukaId = request.optLong("uangMukaId", 0);
		String nama = request.optString("nama", "").trim();
		double dikembalikan = request.optDouble("dikembalikan", 0);
		Date tanggalStor = tanggal(request, "tanggalStor");
		String statusDokumen = request.optString("statusDokumen", Pertangungjawaban.PENGAJUAN).trim();
		JSONArray rincian = request.optJSONArray("rincian");

		if (uangMukaId <= 0) {
			tolak(hasil, "Uang Muka belum dipilih.");
			return;
		}
		if (nama.isEmpty()) {
			tolak(hasil, "Judul Pengajuan belum diisi.");
			return;
		}
		// Sama dgn layar ZK: ambang 0.1 supaya pembulatan kecil tidak memaksa tanggal stor.
		if (tanggalStor == null && dikembalikan > 0.1) {
			tolak(hasil, "Tanggal Stor belum diisi.");
			return;
		}
		if (Pertangungjawaban.DISETUJU.equals(statusDokumen) && !bolehAksi(tbmuser, "approve")) {
			tolak(hasil, "Anda tidak memiliki hak menyetujui pertanggungjawaban.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			UangMuka um = (UangMuka) session.get(UangMuka.class, Long.valueOf(uangMukaId));
			if (um == null) {
				tolak(hasil, "Uang Muka tidak ditemukan.");
				return;
			}
			Pertangungjawaban pj = baru ? new Pertangungjawaban()
					: (Pertangungjawaban) session.get(Pertangungjawaban.class, Long.valueOf(id));
			if (pj == null) {
				tolak(hasil, "Pertanggungjawaban tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			if (!baru && pj.getPostingHistory() != null) {
				tolak(hasil, "Pertanggungjawaban ini sudah dijurnal sehingga tidak boleh diubah lagi.");
				return;
			}
			boolean pilihanTersimpan = !baru && pj.getUangMuka() != null
					&& pj.getUangMuka().getId().longValue() == uangMukaId;
			if (!pilihanTersimpan) {
				String alasan = alasanUangMukaTidakDapatDipilih(session.connection(), uangMukaId);
				if (!alasan.isEmpty()) {
					tolak(hasil, alasan);
					return;
				}
			}

			boolean pph = Common.bolehKonfigurasi("pph_mengurangi_lpj");
			Hitungan h = hitung(session, rincian, pph);
			double nilaiUangMuka = um.getNilai() == null ? 0 : um.getNilai().doubleValue();
			// Perbandingan memakai satuan bulat seperti layar ZK (longValue), supaya selisih
			// pembulatan sen tidak menolak dokumen yang sebenarnya pas.
			if ((long) nilaiUangMuka < (long) h.nilai) {
				tolak(hasil, "Nilai yang dipertanggungjawabkan (" + Common.numberFormat.get().format(h.nilai)
						+ ") melebihi nilai uang muka (" + Common.numberFormat.get().format(nilaiUangMuka) + ").");
				return;
			}

			pj.setUangMuka(um);
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
				pj.setSatuanKerja(um.getSatuanKerja());
			}
			if (pj.getDibuatOleh() == null) {
				pj.setDibuatOleh(tbmuser);
				pj.setTanggalPembuatan(WaktuUtil.getDate());
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(pj.getTanggalPembuatan() == null ? WaktuUtil.getDate() : pj.getTanggalPembuatan());
			pj.setTahun(Integer.valueOf(cal.get(Calendar.YEAR)));
			pj.setBulan(Integer.valueOf(cal.get(Calendar.MONTH) + 1));
			if (Pertangungjawaban.DISETUJU.equals(statusDokumen)) {
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
			// Tautan balik: uang muka menandai dirinya sudah dipertanggungjawabkan -- langkah
			// yang di layar ZK dilakukan tepat setelah simpan.
			um.setPertangungjawaban(pj);
			session.update(um);
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

	/** Validasi otoritatif agar aturan pemilih tidak dapat dilewati oleh klien lama. */
	private static String alasanUangMukaTidakDapatDipilih(Connection conn, long uangMukaId) throws Exception {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT COALESCE(m.aktif,true), COALESCE(m.status,''),"
					+ " m.disetujui_oleh, m.pertangungjawaban,"
					+ " EXISTS (SELECT 1 FROM akunting.daftar_pengajuan_transfer d1 WHERE d1.uang_muka=m.id),"
					+ " EXISTS (SELECT 1 FROM akunting.daftar_pengajuan_transfer d2"
					+ " LEFT JOIN akunting.proses_transfer pt ON pt.id=d2.proses_transfer"
					+ " LEFT JOIN akunting.transitori tr ON tr.id=d2.transitori_data"
					+ " WHERE d2.uang_muka=m.id AND ((COALESCE(d2.transfer,false)=true"
					+ " AND pt.realisasikan_oleh IS NOT NULL) OR (COALESCE(d2.transitori,false)=true"
					+ " AND tr.transfer IS NOT NULL))) FROM public.uang_muka m WHERE m.id=?");
			ps.setLong(1, uangMukaId);
			rs = ps.executeQuery();
			if (!rs.next()) return "Uang Muka tidak ditemukan.";
			if (!rs.getBoolean(1)) return "Uang Muka tidak aktif.";
			if (!UangMuka.DISETUJU.equals(rs.getString(2)) || rs.getObject(3) == null)
				return "Uang Muka belum disetujui sehingga belum dapat dipertanggungjawabkan.";
			if (rs.getObject(4) != null)
				return "Uang Muka sudah dipakai pada pertanggungjawaban lain.";
			if (!rs.getBoolean(5))
				return "Uang Muka belum masuk Proses Transfer (DPC).";
			if (!rs.getBoolean(6))
				return "Proses Transfer (DPC) Uang Muka belum direalisasikan.";
			return "";
		} finally {
			if (rs != null) try { rs.close(); } catch (Exception ignored) { }
			if (ps != null) try { ps.close(); } catch (Exception ignored) { }
		}
	}

	// ==================================================================== status & hapus

	public static void ubahStatus(Tbmuser tbmuser, JSONObject request, JSONObject hasil, boolean setujui)
			throws Exception {
		if (!bolehAksi(tbmuser, setujui ? "approve" : "reject")) {
			tolak(hasil, setujui ? "Anda tidak memiliki hak menyetujui pertanggungjawaban."
					: "Anda tidak memiliki hak menolak pertanggungjawaban.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Pertangungjawaban pj = id <= 0 ? null
					: (Pertangungjawaban) session.get(Pertangungjawaban.class, Long.valueOf(id));
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
				pj.setStatus(Pertangungjawaban.DISETUJU);
				pj.setDisetujuiOleh(tbmuser);
				Date tgl = tanggal(request, "tanggalPersetujuan");
				pj.setTanggalPersetujuan(tgl == null ? WaktuUtil.getDate() : tgl);
			} else {
				pj.setStatus(Pertangungjawaban.DITOLAK);
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

	/**
	 * Hapus satu LPJ. Dokumen yang sudah dijurnal (nilai, pajak, atau pengembaliannya) tidak
	 * boleh dihapus. Tautan pada uang muka induknya ikut dilepas supaya uang muka itu bisa
	 * dipertanggungjawabkan ulang.
	 */
	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus pertanggungjawaban.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Pertangungjawaban pj = id <= 0 ? null
					: (Pertangungjawaban) session.get(Pertangungjawaban.class, Long.valueOf(id));
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
			if (Pertangungjawaban.DISETUJU.equals(pj.getStatus())) {
				tolak(hasil, "Pertanggungjawaban yang sudah disetujui tidak boleh dihapus. Tolak dulu bila memang keliru.");
				return;
			}
			String kode = pj.getKode();
			session.beginTransaction();
			// Lepas tautan dari uang muka induknya lebih dulu, kalau tidak uang muka itu akan
			// menunjuk baris yang sudah tidak ada.
			Criteria c = session.createCriteria(UangMuka.class).add(Restrictions.eq("pertangungjawaban", pj));
			for (Object o : c.list()) {
				UangMuka um = (UangMuka) o;
				um.setPertangungjawaban(null);
				session.update(um);
			}
			// Anggaran yang sempat terpotong dokumen ini dikembalikan lebih dulu; FK-nya
			// tidak ber-ON DELETE CASCADE sehingga penghapusan akan ditolak bila dilewati.
			AnggaranKeuanganUtil.lepaskan(session, "pertangungjawaban", pj.getId());
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
			if (ais.database.model.akunting.NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA == null
					|| ais.database.model.akunting.NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA
							.getNomorSurat() == null) {
				return Common.getGeneratedBarCode();
			}
			NomorSurat ns = ais.database.model.akunting.NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA
					.getNomorSurat();
			Long index = Boolean.TRUE.equals(ns.getGunakanIndexUrut()) ? ns.getNomorIndex()
					: indexBerikutnya(session, ns);
			NomorSurat.tambahIndexNomorSurat(ns);
			String noAgenda = ns.format(index, WaktuUtil.getDate());
			return ais.action.master.KodeUnikUtil.pastikanUnik(Pertangungjawaban.class, noAgenda);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PertangungjawabanApiHelper.buatKode");
			return Common.getGeneratedBarCode();
		}
	}

	/** Salinan aturan lingkup penomoran pada layar ZK. */
	private static Long indexBerikutnya(Session session, NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return Long.valueOf(0);
		}
		int tahun = WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Criteria c = session.createCriteria(Pertangungjawaban.class)
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

	/** Dipakai dispatcher: seluruh aksi berawalan {@code pj_uang_muka_} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("pj_uang_muka_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("pj_uang_muka_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("pj_uang_muka_cari_uang_muka".equals(action)) {
			cariUangMuka(tbmuser, request, hasil);
			return true;
		}
		if ("pj_uang_muka_hitung".equals(action)) {
			hitungPratinjau(tbmuser, request, hasil);
			return true;
		}
		if ("pj_uang_muka_ajukan_transfer".equals(action)) {
			TransferDpcUtil.ajukan("pj_uang_muka", tbmuser, request, hasil);
			return true;
		}
		if ("pj_uang_muka_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("pj_uang_muka_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		if ("pj_uang_muka_setujui".equals(action)) {
			ubahStatus(tbmuser, request, hasil, true);
			return true;
		}
		if ("pj_uang_muka_tolak".equals(action)) {
			ubahStatus(tbmuser, request, hasil, false);
			return true;
		}
		return false;
	}
}
