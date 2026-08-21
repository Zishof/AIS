package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.UangMuka;

/**
 * <h3>Dasbor &amp; cetak dokumen untuk seluruh menu grup "Keuangan".</h3>
 *
 * <p>Bentuk balasannya SENGAJA sama dengan dasbor Pengadaan ({@code pengadaan_dasbor}):
 * {@code kpi}, {@code tren}, {@code komposisi}, {@code peringkat}, dan {@code daftar}.
 * Dengan begitu klien memakai ULANG widget dasbor yang sudah ada -- satu tampilan yang
 * sama dikenali pengguna di kedua grup menu, dan tidak ada kode grafik yang diduplikasi.</p>
 *
 * <p>Cetak dokumen memakai templat Jasper yang SAMA dengan layar ZK (mis.
 * {@code akunting/uangMuka}), sehingga lembar cetak dari Desktop/Android identik dengan
 * lembar cetak dari ZK -- bukan format baru yang dibuat ulang.</p>
 */
public final class KeuanganApiHelper {

	/** Modul yang didukung -> kunci menu pada {@link EbisnisMenuKatalog}. */
	private static final String[][] MODUL = {
			{ "uang_muka", "uang_muka" },
			{ "pj_uang_muka", "pj_uang_muka" },
	};

	private KeuanganApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static String kunciMenu(String modul) {
		for (int i = 0; i < MODUL.length; i++) {
			if (MODUL[i][0].equals(modul)) {
				return MODUL[i][1];
			}
		}
		return null;
	}

	/** Menu harus tampil untuk peran ini sebelum dasbor/cetaknya boleh dibuka. */
	private static boolean bolehLihat(Tbmuser tbmuser, String kunci) {
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		JSONObject menu = EbisnisMenuKatalog.urai(role.getEbisnisMenu());
		JSONObject m = menu == null ? null : menu.optJSONObject("menu");
		return m == null || m.optBoolean(kunci, false);
	}

	private static JSONObject titik(String label, double nilai) throws Exception {
		JSONObject j = new JSONObject();
		j.put("label", label);
		j.put("nilai", nilai);
		return j;
	}

	private static JSONObject kartu(String label, String nilai) throws Exception {
		JSONObject j = new JSONObject();
		j.put("label", label);
		j.put("nilai", nilai);
		return j;
	}

	private static String rupiah(double v) {
		return "Rp " + Common.numberFormat.get().format(v);
	}

	/** Label bulan {@code yyyy-MM} mundur sebanyak {@code bulan} periode, termasuk bulan ini. */
	private static String[] kerangkaBulan(int bulan) {
		String[] label = new String[bulan];
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -(bulan - 1));
		for (int i = 0; i < bulan; i++) {
			label[i] = new java.text.SimpleDateFormat("yyyy-MM").format(c.getTime());
			c.add(Calendar.MONTH, 1);
		}
		return label;
	}

	private static java.sql.Timestamp awalPeriode(int bulan) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -(bulan - 1));
		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return new java.sql.Timestamp(c.getTimeInMillis());
	}

	// ==================================================================== dasbor

	public static void dasbor(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String modul = request == null ? "" : request.optString("modul", "").trim().toLowerCase();
		String kunci = kunciMenu(modul);
		if (kunci == null) {
			tolak(hasil, "Modul dasbor tidak dikenali. Pilih salah satu: uang_muka, pj_uang_muka.");
			return;
		}
		if (!bolehLihat(tbmuser, kunci)) {
			tolak(hasil, "Menu ini tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		int bulan = Math.min(36, Math.max(3, request.optInt("bulan", 12)));
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			hasil.put("status", "00");
			hasil.put("modul", modul);
			hasil.put("bulan", bulan);
			if ("uang_muka".equals(modul)) {
				dasborUangMuka(session, bulan, hasil);
			} else {
				dasborPj(session, bulan, hasil);
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Dasbor Uang Muka: berapa yang menunggu persetujuan, berapa nilai yang sudah cair
	 * tetapi BELUM dipertanggungjawabkan (angka yang paling sering ditanyakan bendahara),
	 * tren pengajuan per bulan, komposisi status, dan satuan kerja pemakai terbesar.
	 */
	private static void dasborUangMuka(Session session, int bulan, JSONObject hasil) throws Exception {
		Connection conn = session.connection();
		java.sql.Timestamp sejak = awalPeriode(bulan);

		PreparedStatement ps = conn.prepareStatement(
				"SELECT COALESCE(status,'') AS s, count(*) AS jml, COALESCE(SUM(nilai),0) AS nilai,"
						+ " COALESCE(SUM(CASE WHEN pertangungjawaban IS NULL THEN nilai ELSE 0 END),0) AS belum_lpj"
						+ " FROM public.uang_muka WHERE tanggal_pembuatan >= ? GROUP BY COALESCE(status,'')");
		ps.setTimestamp(1, sejak);
		ResultSet rs = ps.executeQuery();
		JSONArray komposisi = new JSONArray();
		long total = 0;
		double nilaiTotal = 0;
		double nilaiMenunggu = 0;
		double nilaiBelumLpj = 0;
		while (rs.next()) {
			String s = rs.getString(1);
			long jml = rs.getLong(2);
			double nilai = rs.getDouble(3);
			total += jml;
			nilaiTotal += nilai;
			if (UangMuka.PENGAJUAN.equals(s)) {
				nilaiMenunggu += nilai;
			}
			if (UangMuka.DISETUJU.equals(s)) {
				nilaiBelumLpj += rs.getDouble(4);
			}
			komposisi.put(titik(s.isEmpty() ? "(tanpa status)" : s, jml));
		}
		rs.close();
		ps.close();

		JSONArray kpi = new JSONArray();
		kpi.put(kartu("Jumlah Pengajuan", String.valueOf(total)));
		kpi.put(kartu("Nilai Pengajuan", rupiah(nilaiTotal)));
		kpi.put(kartu("Menunggu Persetujuan", rupiah(nilaiMenunggu)));
		kpi.put(kartu("Cair, Belum di-LPJ", rupiah(nilaiBelumLpj)));

		hasil.put("kpi", kpi);
		hasil.put("komposisi", komposisi);
		hasil.put("komposisiJudul", "Komposisi Status Pengajuan");
		hasil.put("tren", trenBulanan(conn,
				"SELECT to_char(tanggal_pembuatan,'YYYY-MM') AS bl, COALESCE(SUM(nilai),0), count(*)"
						+ " FROM public.uang_muka WHERE tanggal_pembuatan >= ? GROUP BY 1", sejak, bulan));
		hasil.put("trenJudul", "Tren Nilai Pengajuan per Bulan");
		hasil.put("peringkat", peringkat(conn,
				"SELECT COALESCE(sk.nama,'(tanpa satuan kerja)'), COALESCE(SUM(m.nilai),0)"
						+ " FROM public.uang_muka m LEFT JOIN rab.satuan_kerja sk ON sk.id = m.satuan_kerja"
						+ " WHERE m.tanggal_pembuatan >= ? GROUP BY 1 ORDER BY 2 DESC LIMIT 8", sejak));
		hasil.put("peringkatJudul", "Satuan Kerja dengan Nilai Terbesar");

		// Perlu perhatian: sudah disetujui (dana cair) tetapi belum ada LPJ-nya.
		ps = conn.prepareStatement(
				"SELECT COALESCE(kode,''), COALESCE(nama,''), COALESCE(nilai,0),"
						+ " GREATEST(0, DATE_PART('day', now() - COALESCE(tanggal_persetujuan, tanggal_pembuatan)))"
						+ " FROM public.uang_muka"
						+ " WHERE COALESCE(status,'') = ? AND pertangungjawaban IS NULL"
						+ " ORDER BY COALESCE(tanggal_persetujuan, tanggal_pembuatan) ASC LIMIT 15");
		ps.setString(1, UangMuka.DISETUJU);
		rs = ps.executeQuery();
		JSONArray daftar = new JSONArray();
		while (rs.next()) {
			JSONObject j = new JSONObject();
			j.put("kode", rs.getString(1));
			j.put("keterangan", rs.getString(2) + " — " + rupiah(rs.getDouble(3)));
			j.put("umurHari", (int) rs.getDouble(4));
			daftar.put(j);
		}
		rs.close();
		ps.close();
		hasil.put("daftar", daftar);
		hasil.put("daftarJudul", "Sudah Cair, Belum Dipertanggungjawabkan");
		hasil.put("catatanKosong", "Belum ada pengajuan uang muka pada periode ini.");
	}

	/**
	 * Dasbor LPJ: nilai yang sudah dipertanggungjawabkan, pajak terkumpul, dan sisa dana
	 * yang belum distor kembali -- tiga angka yang menentukan apakah siklus uang muka
	 * benar-benar tertutup.
	 */
	private static void dasborPj(Session session, int bulan, JSONObject hasil) throws Exception {
		Connection conn = session.connection();
		java.sql.Timestamp sejak = awalPeriode(bulan);

		PreparedStatement ps = conn.prepareStatement(
				"SELECT COALESCE(status,''), count(*), COALESCE(SUM(nilai),0), COALESCE(SUM(pajak),0),"
						+ " COALESCE(SUM(CASE WHEN COALESCE(telahdikembalikan,false) = false"
						+ "                   THEN COALESCE(dikembalikan,0) ELSE 0 END),0)"
						+ " FROM akunting.pertangungjawaban WHERE tanggal_pembuatan >= ?"
						+ " GROUP BY COALESCE(status,'')");
		ps.setTimestamp(1, sejak);
		ResultSet rs = ps.executeQuery();
		JSONArray komposisi = new JSONArray();
		long total = 0;
		double nilaiTotal = 0, pajakTotal = 0, belumStor = 0;
		while (rs.next()) {
			String s = rs.getString(1);
			long jml = rs.getLong(2);
			total += jml;
			nilaiTotal += rs.getDouble(3);
			pajakTotal += rs.getDouble(4);
			belumStor += rs.getDouble(5);
			komposisi.put(titik(s.isEmpty() ? "(tanpa status)" : s, jml));
		}
		rs.close();
		ps.close();

		JSONArray kpi = new JSONArray();
		kpi.put(kartu("Jumlah LPJ", String.valueOf(total)));
		kpi.put(kartu("Nilai Dipertanggungjawabkan", rupiah(nilaiTotal)));
		kpi.put(kartu("Pajak Terkumpul", rupiah(pajakTotal)));
		kpi.put(kartu("Sisa Dana Belum Distor", rupiah(belumStor)));

		hasil.put("kpi", kpi);
		hasil.put("komposisi", komposisi);
		hasil.put("komposisiJudul", "Komposisi Status LPJ");
		hasil.put("tren", trenBulanan(conn,
				"SELECT to_char(tanggal_pembuatan,'YYYY-MM'), COALESCE(SUM(nilai),0), count(*)"
						+ " FROM akunting.pertangungjawaban WHERE tanggal_pembuatan >= ? GROUP BY 1",
				sejak, bulan));
		hasil.put("trenJudul", "Tren Nilai LPJ per Bulan");
		hasil.put("peringkat", peringkat(conn,
				"SELECT COALESCE(sk.nama,'(tanpa satuan kerja)'), COALESCE(SUM(p.nilai),0)"
						+ " FROM akunting.pertangungjawaban p"
						+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = p.satuan_kerja"
						+ " WHERE p.tanggal_pembuatan >= ? GROUP BY 1 ORDER BY 2 DESC LIMIT 8", sejak));
		hasil.put("peringkatJudul", "Satuan Kerja dengan LPJ Terbesar");

		ps = conn.prepareStatement(
				"SELECT COALESCE(kode,''), COALESCE(nama,''), COALESCE(dikembalikan,0),"
						+ " GREATEST(0, DATE_PART('day', now() - tanggal_pembuatan))"
						+ " FROM akunting.pertangungjawaban"
						+ " WHERE COALESCE(dikembalikan,0) > 0 AND COALESCE(telahdikembalikan,false) = false"
						+ " ORDER BY tanggal_pembuatan ASC LIMIT 15");
		rs = ps.executeQuery();
		JSONArray daftar = new JSONArray();
		while (rs.next()) {
			JSONObject j = new JSONObject();
			j.put("kode", rs.getString(1));
			j.put("keterangan", rs.getString(2) + " — sisa " + rupiah(rs.getDouble(3)));
			j.put("umurHari", (int) rs.getDouble(4));
			daftar.put(j);
		}
		rs.close();
		ps.close();
		hasil.put("daftar", daftar);
		hasil.put("daftarJudul", "Sisa Dana Belum Distor");
		hasil.put("catatanKosong", "Belum ada pertanggungjawaban pada periode ini.");
	}

	/** Tren per bulan dalam kerangka penuh supaya bulan tanpa data tetap tampil sebagai 0. */
	private static JSONArray trenBulanan(Connection conn, String sql, java.sql.Timestamp sejak, int bulan)
			throws Exception {
		java.util.Map<String, double[]> peta = new java.util.LinkedHashMap<String, double[]>();
		String[] label = kerangkaBulan(bulan);
		for (int i = 0; i < label.length; i++) {
			peta.put(label[i], new double[] { 0, 0 });
		}
		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setTimestamp(1, sejak);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			double[] v = peta.get(rs.getString(1));
			if (v != null) {
				v[0] = rs.getDouble(2);
				v[1] = rs.getDouble(3);
			}
		}
		rs.close();
		ps.close();
		JSONArray arr = new JSONArray();
		for (java.util.Map.Entry<String, double[]> e : peta.entrySet()) {
			JSONObject j = new JSONObject();
			j.put("label", e.getKey());
			j.put("nilai", e.getValue()[0]);
			j.put("jumlah", e.getValue()[1]);
			arr.put(j);
		}
		return arr;
	}

	private static JSONArray peringkat(Connection conn, String sql, java.sql.Timestamp sejak) throws Exception {
		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setTimestamp(1, sejak);
		ResultSet rs = ps.executeQuery();
		JSONArray arr = new JSONArray();
		while (rs.next()) {
			arr.put(titik(rs.getString(1), rs.getDouble(2)));
		}
		rs.close();
		ps.close();
		return arr;
	}

	// ==================================================================== cetak

	/**
	 * Cetak satu dokumen menjadi PDF, memakai templat Jasper yang SAMA dengan layar ZK.
	 *
	 * <p>Berkasnya dikirim sebagai base64 supaya klien Desktop/Android dapat langsung
	 * memperlihatkan pratinjau -- pola yang sama dengan {@code pengadaan_cetak}.</p>
	 */
	public static void cetak(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String modul = request == null ? "" : request.optString("modul", "").trim().toLowerCase();
		String kunci = kunciMenu(modul);
		if (kunci == null) {
			tolak(hasil, "Modul cetak tidak dikenali. Pilih salah satu: uang_muka, pj_uang_muka.");
			return;
		}
		if (!bolehLihat(tbmuser, kunci)) {
			tolak(hasil, "Menu ini tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		long id = request.optLong("id", 0);
		if (id <= 0) {
			tolak(hasil, "Dokumen yang dicetak belum dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.io.File berkas = null;
			String kode = "";
			if ("uang_muka".equals(modul)) {
				UangMuka d = (UangMuka) session.get(UangMuka.class, Long.valueOf(id));
				if (d == null) {
					tolak(hasil, "Dokumen tidak ditemukan.");
					return;
				}
				kode = d.getKode() == null ? "" : d.getKode();
				berkas = ais.action.report.format1.akunting.LaporanUangMuka.cetakPdf(d);
			} else {
				Pertangungjawaban d = (Pertangungjawaban) session.get(Pertangungjawaban.class, Long.valueOf(id));
				if (d == null) {
					tolak(hasil, "Dokumen tidak ditemukan.");
					return;
				}
				kode = d.getKode() == null ? "" : d.getKode();
				berkas = ais.action.report.format1.akunting.LaporanPertangungjawaban.cetakPdf(d);
			}
			if (berkas == null || !berkas.exists()) {
				tolak(hasil, "Dokumen gagal dicetak: templat laporannya belum tersedia di server ini.");
				return;
			}
			byte[] isi = new byte[(int) berkas.length()];
			java.io.FileInputStream in = new java.io.FileInputStream(berkas);
			try {
				int dibaca = 0;
				while (dibaca < isi.length) {
					int n = in.read(isi, dibaca, isi.length - dibaca);
					if (n < 0) {
						break;
					}
					dibaca += n;
				}
			} finally {
				in.close();
			}
			hasil.put("status", "00");
			hasil.put("kode", kode);
			hasil.put("namaBerkas", (kode.isEmpty() ? modul : kode) + ".pdf");
			hasil.put("fileBase64", java.util.Base64.getEncoder().encodeToString(isi));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit KeuanganApiHelper.cetak " + modul);
			tolak(hasil, "Dokumen gagal dicetak: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ==================================================================== dispatcher

	/** Dipakai dispatcher: aksi {@code keuangan_dasbor} dan {@code keuangan_cetak}. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("keuangan_dasbor".equals(action)) {
			dasbor(tbmuser, request, hasil);
			return true;
		}
		if ("keuangan_cetak".equals(action)) {
			cetak(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
