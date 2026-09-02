package ais.action.servlet.api;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.ApotikSesiKas;
import ais.database.model.Tbmuser;

/**
 * <h3>Sesi kas (shift) kasir apotek — IR-06.</h3>
 *
 * <p><b>Kenapa apotek punya sesi kas sendiri.</b> {@code SesiKasUtil} POS umum
 * menghitung uang dari {@code koperasi.pembelian_anggota_koperasi}; penjualan
 * apotek tidak pernah ditulis ke sana. Memakainya apa adanya akan melaporkan
 * penjualan tunai apotek Rp 0 dan memunculkan selisih kas sebesar seluruh
 * penerimaan hari itu. Aksi di sini menghitung dari
 * {@code sirs.apotik_pembayaran_transaksi} — sumber yang memang menyimpan
 * pembayaran apotek.</p>
 *
 * <p><b>Aturan yang ditegakkan server, bukan layar:</b></p>
 * <ul>
 * <li>satu kasir hanya boleh punya SATU sesi berstatus BUKA;</li>
 * <li>angka penerimaan dihitung server dari data pembayaran — klien hanya
 * boleh mengirim modal awal dan hasil hitungan fisik laci. Kalau angka sistem
 * pun boleh dikirim klien, rekonsiliasi berhenti menjadi pemeriksaan;</li>
 * <li>sesi yang sudah TUTUP tidak dapat ditutup ulang.</li>
 * </ul>
 *
 * <p><b>Batas yang jujur:</b> "tunai" mengikuti flag
 * {@code cara_pembayaran_koperasi.ada_kembalian} (fallback nama mengandung
 * "tunai"), aturan yang sama dengan laporan shift POS umum, supaya definisi
 * "uang di laci" tidak dikarang ulang. Transaksi apotek yang dikirim TANPA
 * metode pembayaran tidak dapat digolongkan tunai/non-tunai; jumlahnya
 * dilaporkan terpisah sebagai {@code penjualanTanpaMetode} agar tidak
 * diam-diam dianggap nol.</p>
 */
public final class ApotikSesiKasHelper {

	private ApotikSesiKasHelper() {
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static ApotikSesiKas sesiAktif(Session session, String userId) {
		List<?> l = session.createCriteria(ApotikSesiKas.class)
				.add(Restrictions.eq("userId", userId))
				.add(Restrictions.eq("status", ApotikSesiKas.STATUS_BUKA))
				.addOrder(Order.desc("id")).setMaxResults(1).list();
		return l.isEmpty() ? null : (ApotikSesiKas) l.get(0);
	}

	/** Penerimaan sejak [mulai] s.d. sekarang: [tunai, nonTunai, penjualanLedger]. */
	private static double[] penerimaan(Session session, Date mulai, Date sampai) throws Exception {
		double[] hasil = new double[] { 0, 0, 0 };
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				"SELECT COALESCE(c.ada_kembalian, COALESCE(c.nama,'') ilike '%tunai%') tunai, "
						+ "COALESCE(SUM(b.nominal),0) "
						+ "FROM sirs.apotik_pembayaran_transaksi b "
						+ "LEFT JOIN koperasi.cara_pembayaran_koperasi c ON c.id = b.cara_bayar "
						+ "WHERE b.waktu >= ? AND b.waktu <= ? GROUP BY 1");
		try {
			ps.setTimestamp(1, new java.sql.Timestamp(mulai.getTime()));
			ps.setTimestamp(2, new java.sql.Timestamp(sampai.getTime()));
			java.sql.ResultSet rs = ps.executeQuery();
			try {
				while (rs.next()) {
					if (rs.getBoolean(1)) {
						hasil[0] += rs.getDouble(2);
					} else {
						hasil[1] += rs.getDouble(2);
					}
				}
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}

		// Nilai penjualan pada periode yang sama, untuk memperlihatkan bagian
		// yang metodenya tidak pernah tercatat.
		java.sql.PreparedStatement psJual = session.connection().prepareStatement(
				"SELECT COALESCE(SUM(d.hasilpenghitungantotal),0) "
						+ "FROM sirs.detail_transaksi_pasien d "
						+ "JOIN sirs.kode_transaksi_medis k ON d.kode_transaksi = k.id "
						+ "WHERE k.kode = 'AJ' AND d.tanggal >= ? AND d.tanggal <= ?");
		try {
			psJual.setTimestamp(1, new java.sql.Timestamp(mulai.getTime()));
			psJual.setTimestamp(2, new java.sql.Timestamp(sampai.getTime()));
			java.sql.ResultSet rj = psJual.executeQuery();
			try {
				if (rj.next()) hasil[2] = rj.getDouble(1);
			} finally {
				rj.close();
			}
		} finally {
			psJual.close();
		}
		return hasil;
	}

	private static void isiJson(JSONObject j, ApotikSesiKas s) throws Exception {
		j.put("id", s.getId());
		j.put("userId", str(s.getUserId()));
		j.put("namaKasir", str(s.getNamaKasir()));
		j.put("status", str(s.getStatus()));
		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
		j.put("waktuBuka", s.getWaktuBuka() == null ? "" : fmt.format(s.getWaktuBuka()));
		j.put("waktuTutup", s.getWaktuTutup() == null ? "" : fmt.format(s.getWaktuTutup()));
		j.put("modalAwal", s.getModalAwal().doubleValue());
		if (s.getUangFisik() != null) j.put("uangFisik", s.getUangFisik().doubleValue());
		if (s.getTotalTunaiSistem() != null) j.put("totalTunaiSistem", s.getTotalTunaiSistem().doubleValue());
		if (s.getTotalNonTunaiSistem() != null) j.put("totalNonTunaiSistem", s.getTotalNonTunaiSistem().doubleValue());
		if (s.getSelisih() != null) j.put("selisih", s.getSelisih().doubleValue());
		j.put("keterangan", str(s.getKeterangan()));
	}

	// =============================================================================================
	// apotik_sesi_kas_status
	// =============================================================================================

	public static void status(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ApotikSesiKas s = sesiAktif(session, tbmuser.getUserId());
			hasil.put("status", "00");
			hasil.put("ada", s != null);
			if (s == null) return;
			JSONObject j = new JSONObject();
			isiJson(j, s);
			// Angka berjalan (bukan angka tutup): apa yang sudah masuk sejak
			// sesi dibuka sampai detik ini.
			double[] p = penerimaan(session, s.getWaktuBuka(), new Date());
			j.put("tunaiBerjalan", p[0]);
			j.put("nonTunaiBerjalan", p[1]);
			j.put("penjualanBerjalan", p[2]);
			j.put("penjualanTanpaMetode", p[2] - (p[0] + p[1]));
			j.put("kasSeharusnya", s.getModalAwal().doubleValue() + p[0]);
			hasil.put("sesi", j);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_sesi_kas_buka
	// =============================================================================================

	public static void buka(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		double modal = request == null ? 0 : request.optDouble("modal_awal", 0);
		if (modal < 0) {
			tolak(hasil, "Modal awal tidak boleh negatif.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			if (sesiAktif(session, tbmuser.getUserId()) != null) {
				tolak(hasil, "Masih ada sesi kas yang terbuka atas nama Anda. "
						+ "Tutup sesi itu lebih dulu sebelum membuka yang baru.");
				return;
			}
			tx = session.beginTransaction();
			ApotikSesiKas s = new ApotikSesiKas();
			s.setUserId(tbmuser.getUserId());
			s.setNamaKasir(str(tbmuser.getUserNama()));
			s.setStatus(ApotikSesiKas.STATUS_BUKA);
			s.setWaktuBuka(new Date());
			s.setModalAwal(Double.valueOf(modal));
			s.setKeterangan(request == null ? null : request.optString("keterangan", "").trim());
			s.setOleh(tbmuser.getUserId());
			s.setOlehId(tbmuser.getUserId());
			session.save(s);
			tx.commit();

			JSONObject j = new JSONObject();
			isiJson(j, s);
			hasil.put("status", "00");
			hasil.put("sesi", j);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			ais.common.ErrorAuditUtil.record(e, "ApotikSesiKasHelper.buka");
			tolak(hasil, "Gagal membuka sesi kas: " + e.getMessage());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_sesi_kas_tutup
	// =============================================================================================

	public static void tutup(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (request == null || request.isNull("uang_fisik")) {
			tolak(hasil, "Hasil hitungan fisik laci wajib diisi sebelum sesi ditutup.");
			return;
		}
		double uangFisik = request.optDouble("uang_fisik", -1);
		if (uangFisik < 0) {
			tolak(hasil, "Uang fisik tidak boleh negatif.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			ApotikSesiKas s = sesiAktif(session, tbmuser.getUserId());
			if (s == null) {
				tolak(hasil, "Tidak ada sesi kas terbuka atas nama Anda.");
				return;
			}
			Date sampai = new Date();
			// Angka sistem dihitung DI SINI, tidak pernah diambil dari klien.
			double[] p = penerimaan(session, s.getWaktuBuka(), sampai);
			double kasSeharusnya = s.getModalAwal().doubleValue() + p[0];
			double selisih = uangFisik - kasSeharusnya;

			tx = session.beginTransaction();
			s.setStatus(ApotikSesiKas.STATUS_TUTUP);
			s.setWaktuTutup(sampai);
			s.setUangFisik(Double.valueOf(uangFisik));
			s.setTotalTunaiSistem(Double.valueOf(p[0]));
			s.setTotalNonTunaiSistem(Double.valueOf(p[1]));
			s.setSelisih(Double.valueOf(selisih));
			String tambahan = request.optString("keterangan", "").trim();
			if (tambahan.length() > 0) {
				String lama = str(s.getKeterangan());
				s.setKeterangan((lama.length() == 0 ? "" : lama + " | ") + tambahan);
			}
			session.update(s);
			tx.commit();

			JSONObject j = new JSONObject();
			isiJson(j, s);
			j.put("kasSeharusnya", kasSeharusnya);
			j.put("penjualanBerjalan", p[2]);
			j.put("penjualanTanpaMetode", p[2] - (p[0] + p[1]));
			hasil.put("status", "00");
			hasil.put("sesi", j);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			ais.common.ErrorAuditUtil.record(e, "ApotikSesiKasHelper.tutup");
			tolak(hasil, "Gagal menutup sesi kas: " + e.getMessage());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_sesi_kas_list -- riwayat sesi (untuk penyelia)
	// =============================================================================================

	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		int size = request == null ? 20 : request.optInt("page_size", 20);
		if (size < 1) size = 20;
		if (size > 200) size = 200;
		boolean semuaKasir = request != null && request.optBoolean("semua_kasir", false);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(ApotikSesiKas.class)
					.addOrder(Order.desc("id")).setMaxResults(size);
			if (!semuaKasir) {
				c.add(Restrictions.eq("userId", tbmuser.getUserId()));
			}
			@SuppressWarnings("unchecked")
			List<ApotikSesiKas> daftar = c.list();
			JSONArray arr = new JSONArray();
			for (ApotikSesiKas s : daftar) {
				JSONObject j = new JSONObject();
				isiJson(j, s);
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
