package ais.action.servlet.api;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;

/**
 * <h3>Laporan apotek (FASE C) -- penjualan, obat terkendali, kedaluwarsa.</h3>
 *
 * <p>Sumber data = jejak yang SUDAH ditulis jalur FASE A/B (tanpa tabel laporan baru):
 * penjualan dari ledger {@code sirs.detail_transaksi_pasien} berkode {@code AJ} (apotik jual);
 * register obat terkendali dari {@code sirs.apotik_narkotika_log}; kedaluwarsa dari
 * {@code sirs.kadaluarsa} dikurangi {@code sirs.apotik_batch_konsumsi} (sisa). Semua read-only,
 * ber-periode {@code dari}/{@code sampai} (format {@code yyyy-MM-dd}, inklusif).</p>
 */
public final class ApotikLaporanHelper {

	private ApotikLaporanHelper() {
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	/** Rentang tanggal -> [dariInklusif, sampaiEksklusif(+1hari)] string yyyy-MM-dd; default 30 hari. */
	private static String[] periode(JSONObject request) {
		String dari = request == null ? "" : request.optString("dari", "").trim();
		String sampai = request == null ? "" : request.optString("sampai", "").trim();
		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
		if (sampai.isEmpty()) {
			sampai = fmt.format(new java.util.Date());
		}
		if (dari.isEmpty()) {
			java.util.Calendar c = java.util.Calendar.getInstance();
			c.add(java.util.Calendar.DAY_OF_MONTH, -30);
			dari = fmt.format(c.getTime());
		}
		return new String[] { dari, sampai };
	}

	// =============================================================================================
	// apotik_laporan_pembayaran -- rekap uang masuk per metode (dasar rekonsiliasi kas apotek)
	// =============================================================================================

	/**
	 * Rekap pembayaran apotek per metode untuk satu periode.
	 *
	 * <p><b>Kenapa aksi ini perlu ada padahal sudah ada {@code sesi_kas_*}.</b> Laporan tutup kas
	 * POS umum ({@code SesiKasUtil}) menghitung uang dari {@code koperasi.pembelian_anggota_koperasi}.
	 * Penjualan apotek TIDAK ditulis ke sana -- jejaknya ada di {@code sirs.detail_transaksi_pasien}
	 * (kode {@code AJ}) dan pembayarannya di {@code sirs.apotik_pembayaran_transaksi}. Memakai ulang
	 * {@code sesi_kas_*} apa adanya karena itu akan melaporkan penjualan tunai apotek sebesar NOL dan
	 * memunculkan selisih kas sebesar seluruh penerimaan hari itu -- angka yang salah, bukan sekadar
	 * kurang lengkap.</p>
	 *
	 * <p>Sifat tunai/non-tunai diambil dari {@code cara_pembayaran_koperasi.ada_kembalian} (fallback
	 * ke nama yang mengandung "tunai", sama dengan aturan yang sudah dipakai laporan shift POS umum),
	 * supaya definisi "uang di laci" tidak dikarang ulang di sini.</p>
	 *
	 * <p><b>Yang sengaja ditampilkan apa adanya:</b> {@code penjualanLedger} (nilai penjualan AJ pada
	 * periode yang sama) dan {@code selisihTanpaMetode} = penjualan dikurangi seluruh pembayaran
	 * tercatat. Selisih ini BUKAN kekurangan kas; ia adalah penjualan yang metodenya tidak pernah
	 * tercatat -- terutama transaksi sebelum pencatatan metode diaktifkan, atau transaksi yang
	 * dikirim tanpa {@code cara_bayar_id}. Menyembunyikannya akan membuat rekonsiliasi tampak rapi
	 * padahal ada uang yang tidak diketahui asal metodenya.</p>
	 */
	public static void laporanPembayaran(JSONObject request, JSONObject hasil) throws Exception {
		String[] p = periode(request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String rentang = " WHERE b.waktu >= CAST(? AS date) "
					+ "AND b.waktu < (CAST(? AS date) + interval '1 day') ";
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT COALESCE(NULLIF(TRIM(b.nama_cara_bayar),''), COALESCE(c.nama,'(tanpa metode)')), "
							+ "COALESCE(c.ada_kembalian, COALESCE(c.nama,'') ilike '%tunai%'), "
							+ "COUNT(DISTINCT b.transaksi), COALESCE(SUM(b.nominal),0) "
							+ "FROM sirs.apotik_pembayaran_transaksi b "
							+ "LEFT JOIN koperasi.cara_pembayaran_koperasi c ON c.id = b.cara_bayar "
							+ rentang + "GROUP BY 1, 2 ORDER BY 1");
			ps.setString(1, p[0]);
			ps.setString(2, p[1]);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray perMetode = new JSONArray();
			double totalTunai = 0, totalNonTunai = 0;
			long jumlahTransaksi = 0;
			while (rs.next()) {
				boolean tunai = rs.getBoolean(2);
				double nominal = rs.getDouble(4);
				JSONObject j = new JSONObject();
				j.put("nama", str(rs.getString(1)));
				j.put("tunai", tunai);
				j.put("jumlahTransaksi", rs.getLong(3));
				j.put("nominal", nominal);
				perMetode.put(j);
				jumlahTransaksi += rs.getLong(3);
				if (tunai) {
					totalTunai += nominal;
				} else {
					totalNonTunai += nominal;
				}
			}
			rs.close();
			ps.close();

			// Nilai penjualan pada periode yang sama, dari ledger yang sama dengan
			// apotik_laporan_penjualan -- pembanding untuk melihat penjualan yang
			// metodenya tidak tercatat.
			java.sql.PreparedStatement psJual = session.connection().prepareStatement(
					"SELECT COALESCE(SUM(d.hasilpenghitungantotal),0) "
							+ "FROM sirs.detail_transaksi_pasien d "
							+ "JOIN sirs.kode_transaksi_medis k ON d.kode_transaksi = k.id "
							+ "WHERE k.kode = 'AJ' AND d.tanggal BETWEEN CAST(? AS date) "
							+ "AND (CAST(? AS date) + interval '1 day')");
			psJual.setString(1, p[0]);
			psJual.setString(2, p[1]);
			java.sql.ResultSet rj = psJual.executeQuery();
			double penjualan = rj.next() ? rj.getDouble(1) : 0;
			rj.close();
			psJual.close();

			hasil.put("status", "00");
			hasil.put("dari", p[0]);
			hasil.put("sampai", p[1]);
			hasil.put("perMetode", perMetode);
			hasil.put("totalTunai", totalTunai);
			hasil.put("totalNonTunai", totalNonTunai);
			hasil.put("totalPembayaran", totalTunai + totalNonTunai);
			hasil.put("jumlahTransaksi", jumlahTransaksi);
			hasil.put("penjualanLedger", penjualan);
			hasil.put("selisihTanpaMetode", penjualan - (totalTunai + totalNonTunai));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_laporan_penjualan -- agregat penjualan (total, per item, per golongan)
	// =============================================================================================

	public static void laporanPenjualan(JSONObject request, JSONObject hasil) throws Exception {
		String[] p = periode(request);
		Long lokasiId = request != null && !request.isNull("lokasi_id")
				? Long.valueOf((request.get("lokasi_id") + "").trim()) : null;
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String filterLokasi = lokasiId == null ? "" : " AND d.lokasi = " + lokasiId + " ";
			String where = "FROM sirs.detail_transaksi_pasien d "
					+ "JOIN sirs.kode_transaksi_medis k ON d.kode_transaksi = k.id "
					+ "LEFT JOIN sirs.item_medis i ON d.item = i.id "
					+ "WHERE k.kode = 'AJ' AND d.tanggal BETWEEN CAST(? AS date) AND (CAST(? AS date) + interval '1 day') "
					+ filterLokasi;

			// KPI total
			java.sql.PreparedStatement psTot = session.connection().prepareStatement(
					"SELECT COALESCE(SUM(d.qty),0), COALESCE(SUM(d.hasilpenghitungantotal),0), COUNT(*) " + where);
			psTot.setString(1, p[0]); psTot.setString(2, p[1]);
			java.sql.ResultSet rt = psTot.executeQuery();
			double totalQty = 0, totalNilai = 0; long baris = 0;
			if (rt.next()) { totalQty = rt.getDouble(1); totalNilai = rt.getDouble(2); baris = rt.getLong(3); }
			rt.close(); psTot.close();

			// Per item
			java.sql.PreparedStatement psItem = session.connection().prepareStatement(
					"SELECT i.id, i.kode, i.nama, SUM(d.qty), SUM(d.hasilpenghitungantotal) " + where
							+ " GROUP BY i.id, i.kode, i.nama ORDER BY SUM(d.hasilpenghitungantotal) DESC LIMIT 200");
			psItem.setString(1, p[0]); psItem.setString(2, p[1]);
			java.sql.ResultSet ri = psItem.executeQuery();
			JSONArray perItem = new JSONArray();
			while (ri.next()) {
				JSONObject j = new JSONObject();
				j.put("itemId", ri.getLong(1));
				j.put("kode", str(ri.getString(2)));
				j.put("nama", str(ri.getString(3)));
				j.put("qty", ri.getDouble(4));
				j.put("nilai", ri.getDouble(5));
				perItem.put(j);
			}
			ri.close(); psItem.close();

			// Per golongan (FROM tersendiri dgn join profil apotik; null -> BEBAS).
			String whereGol = "FROM sirs.detail_transaksi_pasien d "
					+ "JOIN sirs.kode_transaksi_medis k ON d.kode_transaksi = k.id "
					+ "LEFT JOIN sirs.item_medis i ON d.item = i.id "
					+ "LEFT JOIN sirs.apotik_item_profile ap ON ap.item = i.id "
					+ "WHERE k.kode = 'AJ' AND d.tanggal BETWEEN CAST(? AS date) AND (CAST(? AS date) + interval '1 day') "
					+ filterLokasi;
			java.sql.PreparedStatement psGol = session.connection().prepareStatement(
					"SELECT COALESCE(ap.golongan_obat,'BEBAS') gol, SUM(d.qty), SUM(d.hasilpenghitungantotal) "
							+ whereGol + " GROUP BY COALESCE(ap.golongan_obat,'BEBAS') ORDER BY 3 DESC");
			psGol.setString(1, p[0]); psGol.setString(2, p[1]);
			JSONArray perGolongan = new JSONArray();
			try {
				java.sql.ResultSet rg = psGol.executeQuery();
				while (rg.next()) {
					JSONObject j = new JSONObject();
					j.put("golongan", str(rg.getString(1)));
					j.put("qty", rg.getDouble(2));
					j.put("nilai", rg.getDouble(3));
					perGolongan.put(j);
				}
				rg.close();
			} catch (Exception eg) {
				// Golongan bersifat pelengkap -- kegagalannya tidak menggagalkan laporan.
			} finally {
				psGol.close();
			}

			hasil.put("status", "00");
			hasil.put("dari", p[0]);
			hasil.put("sampai", p[1]);
			hasil.put("totalQty", totalQty);
			hasil.put("totalNilai", totalNilai);
			hasil.put("jumlahBaris", baris);
			hasil.put("perItem", perItem);
			hasil.put("perGolongan", perGolongan);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_laporan_terkendali -- register narkotika/psikotropika (wajib apotek)
	// =============================================================================================

	public static void laporanTerkendali(JSONObject request, JSONObject hasil) throws Exception {
		String[] p = periode(request);
		String golongan = request == null ? "" : request.optString("golongan", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String filterGol = golongan.isEmpty() ? "" : " AND l.golongan_obat = ? ";
			String sql = "SELECT l.waktu, i.kode, i.nama, l.golongan_obat, l.qty, l.nama_pembeli, "
					+ "l.alamat_pembeli, l.nama_dokter, l.keterangan, l.oleh "
					+ "FROM sirs.apotik_narkotika_log l LEFT JOIN sirs.item_medis i ON l.item = i.id "
					+ "WHERE l.waktu BETWEEN CAST(? AS date) AND (CAST(? AS date) + interval '1 day') " + filterGol
					+ "ORDER BY l.waktu DESC LIMIT 1000";
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
			ps.setString(1, p[0]); ps.setString(2, p[1]);
			if (!golongan.isEmpty()) ps.setString(3, golongan);
			java.sql.ResultSet rs = ps.executeQuery();
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
			JSONArray arr = new JSONArray();
			double totalQty = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("waktu", rs.getTimestamp(1) == null ? "" : fmt.format(rs.getTimestamp(1)));
				j.put("kode", str(rs.getString(2)));
				j.put("nama", str(rs.getString(3)));
				j.put("golongan", str(rs.getString(4)));
				double q = rs.getDouble(5); totalQty += q;
				j.put("qty", q);
				j.put("namaPembeli", str(rs.getString(6)));
				j.put("alamatPembeli", str(rs.getString(7)));
				j.put("namaDokter", str(rs.getString(8)));
				j.put("keterangan", str(rs.getString(9)));
				j.put("oleh", str(rs.getString(10)));
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("dari", p[0]);
			hasil.put("sampai", p[1]);
			hasil.put("jumlah", arr.length());
			hasil.put("totalQty", totalQty);
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_laporan_kedaluwarsa -- batch sisa>0 mendekati/sudah kedaluwarsa + nilai
	// =============================================================================================

	public static void laporanKedaluwarsa(JSONObject request, JSONObject hasil) throws Exception {
		int hari = request == null ? 90 : request.optInt("hari_ke_depan", 90);
		Long lokasiId = request != null && !request.isNull("lokasi_id")
				? Long.valueOf((request.get("lokasi_id") + "").trim()) : null;
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.util.Calendar batas = java.util.Calendar.getInstance();
			batas.add(java.util.Calendar.DAY_OF_MONTH, hari);
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
			String filterLokasi = lokasiId == null ? "" : " AND kd.lokasi = " + lokasiId + " ";
			// sisa = kd.qty - COALESCE(SUM(konsumsi.qty),0); nilai = sisa * item.default_harga_beli
			String sql = "SELECT kd.id, i.kode, i.nama, kd.tanggal_kadaluarsa, "
					+ "(kd.qty - COALESCE((SELECT SUM(bk.qty) FROM sirs.apotik_batch_konsumsi bk WHERE bk.kadaluarsa = kd.id),0)) sisa, "
					+ "COALESCE(i.default_harga_beli,0) hb "
					+ "FROM sirs.kadaluarsa kd LEFT JOIN sirs.item_medis i ON kd.item = i.id "
					+ "WHERE kd.tanggal_kadaluarsa <= CAST(? AS date) " + filterLokasi
					+ "ORDER BY kd.tanggal_kadaluarsa ASC LIMIT 1000";
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
			ps.setString(1, fmt.format(batas.getTime()));
			java.sql.ResultSet rs = ps.executeQuery();
			java.util.Date awalHari = awalHariIni();
			JSONArray arr = new JSONArray();
			double totalNilai = 0; int jmlKedaluwarsa = 0, jmlSegera = 0;
			while (rs.next()) {
				double sisa = rs.getDouble(5);
				if (sisa <= 0.0001) continue;
				java.sql.Date tgl = rs.getDate(4);
				boolean exp = tgl != null && tgl.before(awalHari);
				double nilai = sisa * rs.getDouble(6);
				totalNilai += nilai;
				if (exp) jmlKedaluwarsa++; else jmlSegera++;
				JSONObject j = new JSONObject();
				j.put("kadaluarsaId", rs.getLong(1));
				j.put("kode", str(rs.getString(2)));
				j.put("nama", str(rs.getString(3)));
				j.put("tanggalKadaluarsa", tgl == null ? "" : fmt.format(tgl));
				j.put("sisa", sisa);
				j.put("nilai", nilai);
				j.put("kedaluwarsa", exp);
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("hariKeDepan", hari);
			hasil.put("jumlahKedaluwarsa", jmlKedaluwarsa);
			hasil.put("jumlahSegera", jmlSegera);
			hasil.put("totalNilai", totalNilai);
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static java.util.Date awalHariIni() {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return c.getTime();
	}
}
