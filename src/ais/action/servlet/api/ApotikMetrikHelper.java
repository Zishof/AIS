package ais.action.servlet.api;

import org.hibernate.Session;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;

/**
 * <h3>Metrik operasional apotek (IR-10) -- angka PASTI untuk dasbor.</h3>
 *
 * <p><b>Masalah yang diselesaikan.</b> Dasbor sebelumnya menghitung sendiri
 * angka prioritasnya dari daftar ber-halaman ({@code apotik_resep_list},
 * {@code apotik_batch_monitor}, {@code apotik_item_cari}, masing-masing
 * {@code page_size=100}). Akibatnya angkanya bisa salah dalam dua cara:
 * terpotong pada 100, dan -- khusus resep -- {@code apotik_resep_list}
 * memotong halaman DULU baru menyaring yang belum ditebus, sehingga "resep
 * menunggu" sebenarnya berarti "yang belum ditebus di antara 100 resep
 * terbaru". Kartu prioritas yang menyesatkan lebih berbahaya daripada tidak
 * ada kartu sama sekali.</p>
 *
 * <p>Seluruh angka di sini dihitung dengan COUNT di basis data, tanpa tabel
 * baru, dan read-only.</p>
 *
 * <p><b>Yang sengaja TIDAK disediakan: SLA waktu tunggu resep.</b>
 * {@code sirs.resep} tidak punya kolom waktu masuk; satu-satunya stempel waktu
 * adalah {@code tanggal_dirubah} yang berubah setiap kali baris disunting,
 * sehingga "menunggu 40 menit" yang dihitung darinya akan sering salah.
 * Karena itu respons memuat {@code slaResepTersedia=false} dan klien tidak
 * merakit kartu SLA. Menambah kolom {@code waktu_masuk} pada
 * {@code sirs.resep} adalah sisa pekerjaan IR-10.</p>
 */
public final class ApotikMetrikHelper {

	private ApotikMetrikHelper() {
	}

	private static long hitung(Session session, String sql) throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
		try {
			java.sql.ResultSet rs = ps.executeQuery();
			try {
				return rs.next() ? rs.getLong(1) : 0L;
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
	}

	private static double jumlah(Session session, String sql) throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
		try {
			java.sql.ResultSet rs = ps.executeQuery();
			try {
				return rs.next() ? rs.getDouble(1) : 0d;
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
	}

	public static void metrikOperasional(JSONObject request, JSONObject hasil) throws Exception {
		int hariSegera = request == null ? 90 : request.optInt("hari_ke_depan", 90);
		if (hariSegera < 1) hariSegera = 90;
		if (hariSegera > 365) hariSegera = 365;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Resep menunggu = belum pernah ditebus. Definisi SAMA dengan
			// apotik_resep_list (TransaksiMedis.resep sebagai bukti penebusan),
			// hanya saja dihitung atas SELURUH baris, bukan satu halaman.
			long resepMenunggu = hitung(session,
					"SELECT COUNT(*) FROM sirs.resep r "
							+ "WHERE NOT EXISTS (SELECT 1 FROM sirs.transaksi_medis tm "
							+ "WHERE tm.resep = r.id)");
			long resepTotal = hitung(session, "SELECT COUNT(*) FROM sirs.resep");

			// Batch: sisa = qty batch dikurangi konsumsi, definisi sama dengan
			// apotik_laporan_kedaluwarsa.
			String sisa = "(kd.qty - COALESCE((SELECT SUM(bk.qty) FROM sirs.apotik_batch_konsumsi bk "
					+ "WHERE bk.kadaluarsa = kd.id),0)) > 0";
			long batchKedaluwarsa = hitung(session,
					"SELECT COUNT(*) FROM sirs.kadaluarsa kd "
							+ "WHERE kd.tanggal_kadaluarsa < CURRENT_DATE AND " + sisa);
			long batchSegera = hitung(session,
					"SELECT COUNT(*) FROM sirs.kadaluarsa kd "
							+ "WHERE kd.tanggal_kadaluarsa >= CURRENT_DATE "
							+ "AND kd.tanggal_kadaluarsa <= (CURRENT_DATE + INTERVAL '" + hariSegera + " day') "
							+ "AND " + sisa);
			long batchDitahan = hitung(session,
					"SELECT COUNT(*) FROM sirs.kadaluarsa kd "
							+ "WHERE kd.status_lot IS NOT NULL AND kd.status_lot <> '' "
							+ "AND kd.status_lot <> 'ELIGIBLE' AND " + sisa);

			// "Habis" dibatasi pada obat yang PERNAH bergerak di ledger lalu
			// saldonya <= 0. Tanpa batasan itu, seluruh katalog yang belum
			// pernah distok ikut terhitung dan angkanya jadi tidak berarti.
			long itemHabis = hitung(session,
					"SELECT COUNT(*) FROM (SELECT a.item, SUM((a.qty + a.qty_bonus) * b.jenis) saldo "
							+ "FROM sirs.detail_transaksi_pasien a "
							+ "INNER JOIN sirs.kode_transaksi_medis b ON a.kode_transaksi = b.id "
							+ "GROUP BY a.item) x WHERE x.saldo <= 0");

			long transaksiHariIni = hitung(session,
					"SELECT COUNT(DISTINCT td.transaksi) FROM sirs.detail_transaksi_pasien d "
							+ "INNER JOIN sirs.transaksi_medis_detail td ON d.transaksi_detail = td.id "
							+ "INNER JOIN sirs.kode_transaksi_medis k ON d.kode_transaksi = k.id "
							+ "WHERE k.kode = 'AJ' AND d.tanggal >= CURRENT_DATE "
							+ "AND d.tanggal < (CURRENT_DATE + INTERVAL '1 day')");
			double nilaiHariIni = jumlah(session,
					"SELECT COALESCE(SUM(d.hasilpenghitungantotal),0) FROM sirs.detail_transaksi_pasien d "
							+ "INNER JOIN sirs.kode_transaksi_medis k ON d.kode_transaksi = k.id "
							+ "WHERE k.kode = 'AJ' AND d.tanggal >= CURRENT_DATE "
							+ "AND d.tanggal < (CURRENT_DATE + INTERVAL '1 day')");

			hasil.put("status", "00");
			hasil.put("resepMenunggu", resepMenunggu);
			hasil.put("resepTotal", resepTotal);
			hasil.put("batchKedaluwarsa", batchKedaluwarsa);
			hasil.put("batchSegera", batchSegera);
			hasil.put("batchDitahan", batchDitahan);
			hasil.put("hariSegera", hariSegera);
			hasil.put("itemHabis", itemHabis);
			hasil.put("transaksiHariIni", transaksiHariIni);
			hasil.put("nilaiHariIni", nilaiHariIni);
			// Lihat JavaDoc kelas: waktu tunggu resep belum dapat dihitung jujur.
			hasil.put("slaResepTersedia", false);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
