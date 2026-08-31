package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.AturanHargaProduk;

/**
 * Mesin harga grosir (Fase A dok. 48/49) — SATU salinan untuk semua kanal.
 *
 * <p>{@link #terapkanKeItems} dipanggil dari DUA tempat dan hanya dua tempat:
 * pratinjau keranjang ({@code KantinHelper.diskonEvaluasi}) dan checkout
 * ({@code KantinHelper.bayar}, SEBELUM {@code terapkanEvaluasiDiskonServer}).
 * Urutan itu keputusan pemilik sistem 29-08-2026: harga grosir menentukan
 * HARGA SATUAN lebih dulu, AturanDiskon memotong SESUDAHNYA. "HARGA_AWAL" pada
 * {@code AturanDiskon.dasarPerhitungan} dengan demikian berarti harga SESUDAH
 * grosir.</p>
 *
 * <p>Aturan pemilihan: hanya aturan aktif dalam jendela waktunya; ambang
 * TERBESAR yang &le; qty menang; aturan ber-toko mengalahkan aturan global
 * pada ambang yang sama-sama terpenuhi. Tanpa aturan cocok → harga baris tidak
 * disentuh (harga katalog dari klien berlaku, dan validasi harga lama tetap
 * bekerja seperti sebelumnya).</p>
 */
public final class HargaGrosirApiHelper {

	private HargaGrosirApiHelper() { }

	// =====================================================================
	// MESIN
	// =====================================================================

	/**
	 * Harga satuan efektif untuk satu produk pada qty tertentu, atau
	 * {@code null} bila tidak ada aturan yang cocok.
	 */
	public static Double hargaSatuan(Connection conn, long tokoId, long produkId, double qtyDasar,
			Date waktu) throws Exception {
		if (qtyDasar <= 0) return null;
		java.sql.Timestamp ts = new java.sql.Timestamp((waktu == null ? new Date() : waktu).getTime());
		Object[] a = aturanCocok(conn, tokoId, produkId, qtyDasar, waktu);
		return a == null ? null : (Double) a[0];
	}

	/**
	 * Aturan grosir yang cocok utk (produk, qty): {@code {hargaEfektif, minQtyDasar,
	 * kelipatanWajib}} atau {@code null}. Harga efektif = {@code harga_paket/min_qty_dasar}
	 * bila Metode 2 terisi (dok. 48 §6 no.1), selain itu kolom {@code harga} biasa.
	 */
	private static Object[] aturanCocok(Connection conn, long tokoId, long produkId,
			double qtyDasar, Date waktu) throws Exception {
		if (qtyDasar <= 0) return null;
		java.sql.Timestamp ts = new java.sql.Timestamp((waktu == null ? new Date() : waktu).getTime());
		PreparedStatement ps = conn.prepareStatement(
				"SELECT COALESCE(harga_paket / NULLIF(min_qty_dasar, 0), harga),"
						+ " min_qty_dasar, COALESCE(kelipatan_wajib, false)"
						+ " FROM koperasi.aturan_harga_produk"
						+ " WHERE produk = ? AND COALESCE(aktif, true) = true"
						+ " AND (toko IS NULL OR toko = ?)"
						+ " AND min_qty_dasar <= ?"
						+ " AND (berlaku_mulai IS NULL OR berlaku_mulai <= ?)"
						+ " AND (berlaku_sampai IS NULL OR berlaku_sampai >= ?)"
						// Ambang terbesar menang; pada ambang sama, aturan ber-toko
						// (toko IS NOT NULL) menang atas aturan global.
						+ " ORDER BY min_qty_dasar DESC, (toko IS NULL) ASC LIMIT 1");
		try {
			ps.setLong(1, produkId); ps.setLong(2, tokoId); ps.setDouble(3, qtyDasar);
			ps.setTimestamp(4, ts); ps.setTimestamp(5, ts);
			ResultSet rs = ps.executeQuery();
			try {
				if (!rs.next()) return null;
				return new Object[] { Double.valueOf(rs.getDouble(1)),
						Double.valueOf(rs.getDouble(2)), Boolean.valueOf(rs.getBoolean(3)) };
			} finally { rs.close(); }
		} finally { ps.close(); }
	}

	/**
	 * §6 no.2 dok. 48: bila aturan grosir yang cocok menyandang {@code kelipatan_wajib}, TOTAL
	 * qty produk itu di keranjang harus kelipatan {@code min_qty_dasar}. Mengembalikan pesan
	 * pelanggaran pertama yang terbaca kasir, atau {@code null} bila semua sah. Dipanggil
	 * {@code bayar} SETELAH harga grosir diterapkan (urutan dok. 51).
	 */
	public static String cekKelipatanWajib(Connection conn, long tokoId, JSONArray items,
			Date waktu) throws Exception {
		if (items == null || items.length() == 0) return null;
		java.util.Map<Long, Double> totalQty = new java.util.LinkedHashMap<Long, Double>();
		for (int i = 0; i < items.length(); i++) {
			JSONObject it = items.optJSONObject(i); if (it == null) continue;
			kumpulkanQty(totalQty, it);
			JSONArray ekstra = it.optJSONArray("ekstra");
			if (ekstra != null) for (int k = 0; k < ekstra.length(); k++) {
				JSONObject e = ekstra.optJSONObject(k); if (e != null) kumpulkanQty(totalQty, e);
			}
		}
		Date kini = waktu == null ? new Date() : waktu;
		for (java.util.Iterator<java.util.Map.Entry<Long, Double>> it = totalQty.entrySet().iterator(); it.hasNext();) {
			java.util.Map.Entry<Long, Double> en = it.next();
			Object[] a = aturanCocok(conn, tokoId, en.getKey().longValue(), en.getValue().doubleValue(), kini);
			if (a == null || !Boolean.TRUE.equals(a[2])) continue;
			double minQty = ((Double) a[1]).doubleValue();
			if (minQty <= 0) continue;
			double sisa = en.getValue().doubleValue() % minQty;
			if (sisa > 1e-6 && (minQty - sisa) > 1e-6) {
				String nama = String.valueOf(en.getKey());
				PreparedStatement pn = conn.prepareStatement("SELECT nama FROM koperasi.produk WHERE id=?");
				try {
					pn.setLong(1, en.getKey().longValue());
					ResultSet rn = pn.executeQuery();
					try { if (rn.next() && rn.getString(1) != null) nama = rn.getString(1); }
					finally { rn.close(); }
				} finally { pn.close(); }
				long bawah = (long) Math.floor(en.getValue().doubleValue() / minQty) * (long) minQty;
				long atas = bawah + (long) minQty;
				return "Pembelian grosir " + nama + " wajib kelipatan " + ((long) minQty)
						+ " (saat ini " + en.getValue() + "). Bulatkan ke " + bawah + " atau " + atas + ".";
			}
		}
		return null;
	}

	/**
	 * Menimpa {@code harga} tiap baris {@code {id, harga, jumlah}} — termasuk
	 * baris {@code ekstra} bersarang (bentuk sama) — dengan harga grosir bila
	 * ada aturan yang cocok. Baris tanpa aturan TIDAK disentuh.
	 *
	 * <p>Qty penentu ambang adalah TOTAL qty produk itu di seluruh keranjang
	 * (baris yang sama dipecah dua tetap dihitung gabungan — 2×25 kg memenuhi
	 * ambang 50 kg, sama seperti pembeli menaruhnya sebagai satu baris).</p>
	 *
	 * @param petaKeluar bila bukan null, diisi {@code produkId → harga} untuk
	 *                   tiap produk yang berubah — dipakai pratinjau keranjang
	 *                   menampilkan harga efektif tanpa menebak ulang.
	 */
	public static void terapkanKeItems(Connection conn, long tokoId, JSONArray items,
			JSONObject petaKeluar) throws Exception {
		if (items == null || items.length() == 0) return;
		// Lintasan 1: total qty per produk (baris atas + ekstra bersarang).
		java.util.Map<Long, Double> totalQty = new java.util.LinkedHashMap<Long, Double>();
		for (int i = 0; i < items.length(); i++) {
			JSONObject it = items.optJSONObject(i); if (it == null) continue;
			kumpulkanQty(totalQty, it);
			JSONArray ekstra = it.optJSONArray("ekstra");
			if (ekstra != null) for (int k = 0; k < ekstra.length(); k++) {
				JSONObject e = ekstra.optJSONObject(k); if (e != null) kumpulkanQty(totalQty, e);
			}
		}
		if (totalQty.isEmpty()) return;
		// Lintasan 2: harga efektif per produk, lalu timpa barisnya.
		Date kini = new Date();
		java.util.Map<Long, Double> efektif = new java.util.LinkedHashMap<Long, Double>();
		for (java.util.Iterator<java.util.Map.Entry<Long, Double>> it = totalQty.entrySet().iterator(); it.hasNext();) {
			java.util.Map.Entry<Long, Double> en = it.next();
			Double h = hargaSatuan(conn, tokoId, en.getKey().longValue(), en.getValue().doubleValue(), kini);
			if (h != null) efektif.put(en.getKey(), h);
		}
		if (efektif.isEmpty()) return;
		for (int i = 0; i < items.length(); i++) {
			JSONObject it = items.optJSONObject(i); if (it == null) continue;
			timpaHarga(efektif, it);
			JSONArray ekstra = it.optJSONArray("ekstra");
			if (ekstra != null) for (int k = 0; k < ekstra.length(); k++) {
				JSONObject e = ekstra.optJSONObject(k); if (e != null) timpaHarga(efektif, e);
			}
		}
		if (petaKeluar != null) {
			for (java.util.Iterator<java.util.Map.Entry<Long, Double>> it = efektif.entrySet().iterator(); it.hasNext();) {
				java.util.Map.Entry<Long, Double> en = it.next();
				petaKeluar.put(String.valueOf(en.getKey()), en.getValue());
			}
		}
	}

	private static void kumpulkanQty(java.util.Map<Long, Double> peta, JSONObject baris) {
		Long id = idProduk(baris); if (id == null) return;
		double jumlah = baris.isNull("jumlah") ? 1.0 : baris.optDouble("jumlah", 1.0);
		if (jumlah <= 0) return;
		Double lama = peta.get(id);
		peta.put(id, Double.valueOf((lama == null ? 0.0 : lama.doubleValue()) + jumlah));
	}

	private static void timpaHarga(java.util.Map<Long, Double> efektif, JSONObject baris) {
		Long id = idProduk(baris); if (id == null) return;
		Double h = efektif.get(id);
		if (h != null) try { baris.put("harga", h.doubleValue()); } catch (Exception ignored) { }
	}

	private static Long idProduk(JSONObject baris) {
		if (baris.isNull("id")) return null;
		try { return Long.valueOf((baris.get("id") + "").trim()); }
		catch (Exception e) { return null; }
	}

	// =====================================================================
	// CRUD — gerbang mengikuti pola aturan diskon (KantinHelper.diskonSimpan):
	// aturan harga adalah aturan komersial dengan pemilik yang sama.
	// =====================================================================

	private static boolean bolehKelola(Tbmuser tbmuser, String aksi) throws Exception {
		ais.database.model.inventory.Pedagang p = tbmuser == null ? null : tbmuser.getPedagang();
		boolean adminGlobal = p == null;
		boolean supervisor = p != null && Boolean.TRUE.equals(p.getSupervisor());
		return KantinHelper.bolehAksiCrud(tbmuser, p, adminGlobal, supervisor, "diskon", aksi);
	}

	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(AturanHargaProduk.class)
					.addOrder(Order.asc("produk")).addOrder(Order.desc("minQtyDasar"));
			Long produkId = ais.common.Common.angkaAtauNull(request, "produk_id");
			if (produkId != null) c.add(Restrictions.eq("produk", produkId));
			if (!request.optBoolean("termasuk_nonaktif", false)) c.add(Restrictions.eq("aktif", Boolean.TRUE));
			List list = c.setMaxResults(500).list();
			JSONArray data = new JSONArray();
			for (int i = 0; i < list.size(); i++) {
				AturanHargaProduk a = (AturanHargaProduk) list.get(i);
				JSONObject j = new JSONObject();
				j.put("id", a.getId()); j.put("produkId", a.getProduk());
				j.put("tokoId", a.getToko() == null ? JSONObject.NULL : a.getToko());
				j.put("minQtyDasar", a.getMinQtyDasar()); j.put("harga", a.getHarga());
				j.put("hargaPaket", a.getHargaPaket() == null ? JSONObject.NULL : a.getHargaPaket());
				j.put("kelipatanWajib", Boolean.TRUE.equals(a.getKelipatanWajib()));
				j.put("berlakuMulai", a.getBerlakuMulai() == null ? JSONObject.NULL : Long.valueOf(a.getBerlakuMulai().getTime()));
				j.put("berlakuSampai", a.getBerlakuSampai() == null ? JSONObject.NULL : Long.valueOf(a.getBerlakuSampai().getTime()));
				j.put("aktif", Boolean.TRUE.equals(a.getAktif()));
				j.put("keterangan", a.getKeterangan() == null ? "" : a.getKeterangan());
				data.put(j);
			}
			hasil.put("status", "00"); hasil.put("data", data);
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = ais.common.Common.angkaAtauNull(request, "id");
		if (!bolehKelola(tbmuser, id == null ? "create" : "update")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola aturan harga grosir.");
			return;
		}
		Long produkId = ais.common.Common.angkaAtauNull(request, "produk_id");
		double minQty = request.optDouble("min_qty_dasar", 0);
		double harga = request.optDouble("harga", 0);
		double hargaPaket = request.optDouble("harga_paket", 0);
		if (produkId == null || minQty <= 0 || (harga <= 0 && hargaPaket <= 0)) {
			hasil.put("status", "91");
			hasil.put("description",
					"produk_id, min_qty_dasar (>0), dan harga (>0) ATAU harga_paket (>0) wajib diisi.");
			return;
		}
		// Metode 2: harga satuan turunan disimpan juga supaya kolom NOT NULL lama tetap terisi
		// dan laporan lama yang membaca `harga` tidak melihat nol.
		if (harga <= 0) harga = hargaPaket / minQty;
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AturanHargaProduk a = id == null ? new AturanHargaProduk()
					: (AturanHargaProduk) session.get(AturanHargaProduk.class, id);
			if (a == null) { hasil.put("status", "91"); hasil.put("description", "Aturan harga tidak ditemukan."); return; }
			a.setProduk(produkId);
			a.setToko(ais.common.Common.angkaAtauNull(request, "toko_id"));
			a.setMinQtyDasar(Double.valueOf(minQty)); a.setHarga(Double.valueOf(harga));
			a.setHargaPaket(hargaPaket > 0 ? Double.valueOf(hargaPaket) : null);
			a.setKelipatanWajib(Boolean.valueOf(request.optBoolean("kelipatan_wajib", false)));
			a.setBerlakuMulai(request.isNull("berlaku_mulai") ? null : new Date(request.getLong("berlaku_mulai")));
			a.setBerlakuSampai(request.isNull("berlaku_sampai") ? null : new Date(request.getLong("berlaku_sampai")));
			a.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			a.setKeterangan(request.optString("keterangan", "").trim());
			a.setOleh(tbmuser == null ? "SYSTEM" : tbmuser.getUserId());
			if (a.getWaktu() == null) a.setWaktu(new Date());
			session.beginTransaction(); session.saveOrUpdate(a); session.getTransaction().commit();
			hasil.put("status", "00"); hasil.put("id", a.getId());
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehKelola(tbmuser, "delete")) {
			hasil.put("status", "91");
			hasil.put("description", "Hanya admin/manager atau supervisor toko yang dapat mengelola aturan harga grosir.");
			return;
		}
		Long id = ais.common.Common.angkaAtauNull(request, "id");
		if (id == null) { hasil.put("status", "91"); hasil.put("description", "Parameter id wajib diisi."); return; }
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			AturanHargaProduk a = (AturanHargaProduk) session.get(AturanHargaProduk.class, id);
			if (a == null) { hasil.put("status", "91"); hasil.put("description", "Aturan harga tidak ditemukan."); return; }
			// Nonaktif, bukan hapus baris: harga yang pernah berlaku adalah bagian
			// jejak komersial — transaksi lama dihitung dengannya.
			a.setAktif(Boolean.FALSE);
			session.beginTransaction(); session.saveOrUpdate(a); session.getTransaction().commit();
			hasil.put("status", "00");
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}
}
