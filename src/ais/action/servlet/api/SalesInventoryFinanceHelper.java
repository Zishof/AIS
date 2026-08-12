package ais.action.servlet.api;

import java.math.BigDecimal;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;

/**
 * <h3>Finance varian Inventory &amp; Sales -- layar legacy 43-48.</h3>
 *
 * <p>REUSE modul akunting existing (mapping layar 44 "jangan duplikasi COA"): COA =
 * {@code akunting.akun} ({@link Akun}), jurnal = {@code akunting.transaksi} -- helper ini
 * hanya membungkusnya jadi aksi API varian (baca + tulis master terbatas), TIDAK membuat
 * tabel akuntansi kedua. Laporan keuangan LENGKAP (Neraca/Arus Kas/Buku Besar) tetap lewat
 * katalog {@code laporan_keuangan_katalog} existing (menu Laporan Keuangan).</p>
 *
 * <p>Laba kotor (layar 46) dihitung dari SNAPSHOT HPP per baris sales order
 * ({@code sales_order_lapangan_item.hpp_snapshot}, dibekukan saat order dibuat) -- tidak
 * merekonstruksi HPP historis. Laba/Rugi varian (layar 47-48) = pendapatan faktur AR +
 * penjualan tunai sesi &minus; HPP &minus; biaya sesi, per periode/toko/sales -- ringkasan
 * operasional Sales Lapangan, BUKAN pengganti laporan keuangan penuh existing.</p>
 */
public final class SalesInventoryFinanceHelper {

	private SalesInventoryFinanceHelper() {
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static Long optLong(JSONObject r, String kunci) {
		if (r == null || r.isNull(kunci)) {
			return null;
		}
		try {
			return Long.valueOf((r.get(kunci) + "").trim());
		} catch (Exception e) {
			return null;
		}
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static boolean pemilikAtauAdmin(EbisnisActorContextResolver.ActorContext ctx) {
		return ctx.admin || EbisnisActorContextResolver.ACTOR_PEMILIK.equals(ctx.actorType);
	}

	/** Literal tanggal aman (regex ketat) -- dipakai rentang periode di beberapa subquery. */
	private static String tglLiteral(JSONObject request, String kunci, String fallback) {
		String s = request.optString(kunci, "").trim();
		return s.matches("\\d{4}-\\d{2}-\\d{2}") ? ("DATE '" + s + "'") : fallback;
	}

	// =============================================================================================
	// SCR-44: Master Akun (COA existing akunting.akun -- baca + tulis terbatas)
	// =============================================================================================

	public static void coaList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("kas_jurnal")) {
			tolak(hasil, "Menu Kas & Jurnal tidak aktif untuk akun Anda.");
			return;
		}
		String q = request.optString("q", "").trim().toLowerCase();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1");
			if (!q.isEmpty()) where.append(" AND (LOWER(a.kode) LIKE ? OR LOWER(a.nama) LIKE ?)");
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT a.id, a.kode, a.nama, a.keterangan, a.debet_credit, p.kode, p.nama"
							+ " FROM akunting.akun a LEFT JOIN akunting.akun p ON a.parent = p.id"
							+ where + " ORDER BY a.kode LIMIT 500");
			int ix = 1;
			if (!q.isEmpty()) { ps.setString(ix++, "%" + q + "%"); ps.setString(ix++, "%" + q + "%"); }
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray rows = new JSONArray();
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("id", rs.getLong(1));
				r.put("kode", str(rs.getString(2)));
				r.put("nama", str(rs.getString(3)));
				r.put("keterangan", str(rs.getString(4)));
				r.put("debetCredit", rs.getInt(5));
				r.put("parentKode", str(rs.getString(6)));
				r.put("parentNama", str(rs.getString(7)));
				rows.put(r);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("rows", rows);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Create/update perkiraan -- Pemilik/Admin; kode unik; TANPA delete (akun berhistori). */
	public static void coaSave(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!pemilikAtauAdmin(ctx)) {
			tolak(hasil, "Master Akun hanya dikelola Pemilik/Admin.");
			return;
		}
		String kode = request.optString("kode", "").trim();
		String nama = request.optString("nama", "").trim();
		if (kode.isEmpty() || nama.isEmpty()) {
			tolak(hasil, "kode dan nama akun wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			Long id = optLong(request, "akun_id");
			Akun a;
			if (id != null) {
				a = (Akun) session.get(Akun.class, id);
				if (a == null) {
					tolak(hasil, "Akun tidak ditemukan.");
					return;
				}
			} else {
				Akun dobel = (Akun) session.createCriteria(Akun.class)
						.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
				if (dobel != null) {
					tolak(hasil, "Kode akun " + kode + " sudah dipakai (" + str(dobel.getNama()) + ").");
					return;
				}
				a = new Akun();
			}
			tx = session.beginTransaction();
			a.setKode(kode);
			a.setNama(nama);
			if (!request.isNull("keterangan")) {
				a.setKeterangan(request.optString("keterangan", "").trim());
			}
			if (!request.isNull("debet_credit")) {
				a.setDebetCredit(Integer.valueOf(request.optInt("debet_credit", 0)));
			}
			Long parentId = optLong(request, "parent_id");
			if (parentId != null) {
				a.setParent((Akun) session.get(Akun.class, parentId));
			}
			try {
				a.getClass().getMethod("setOleh", String.class).invoke(a, tbmuser.getUserId());
			} catch (Exception ignore) { }
			session.saveOrUpdate(a);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", a.getId());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-43: Kas / Jurnal (baca akunting.transaksi existing)
	// =============================================================================================

	public static void cashJournalList(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("kas_jurnal")) {
			tolak(hasil, "Menu Kas & Jurnal tidak aktif untuk akun Anda.");
			return;
		}
		String dari = tglLiteral(request, "dari", "(CURRENT_DATE - 30)");
		String sampai = tglLiteral(request, "sampai", "CURRENT_DATE");
		Long akunId = optLong(request, "akun_id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(
					" WHERE t.tanggal_transaksi >= " + dari
							+ " AND t.tanggal_transaksi < (" + sampai + " + 1)");
			if (akunId != null) where.append(" AND t.akun = ?");
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT t.id, t.kode, t.jenis_jurnal, t.tanggal_transaksi, t.keterangan,"
							+ " a.kode, a.nama, COALESCE(t.debet,0), COALESCE(t.kredit,0), t.tanggal_posting"
							+ " FROM akunting.transaksi t LEFT JOIN akunting.akun a ON t.akun = a.id"
							+ where + " ORDER BY t.tanggal_transaksi DESC, t.id DESC LIMIT 500");
			int ix = 1;
			if (akunId != null) ps.setLong(ix++, akunId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray rows = new JSONArray();
			double totalDebet = 0, totalKredit = 0;
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("id", rs.getLong(1));
				r.put("kode", str(rs.getString(2)));
				r.put("jenisJurnal", str(rs.getString(3)));
				r.put("tanggal", str(rs.getTimestamp(4)));
				r.put("keterangan", str(rs.getString(5)));
				r.put("akunKode", str(rs.getString(6)));
				r.put("akunNama", str(rs.getString(7)));
				r.put("debet", rs.getDouble(8));
				r.put("kredit", rs.getDouble(9));
				r.put("posted", rs.getTimestamp(10) != null);
				rows.put(r);
				totalDebet += rs.getDouble(8);
				totalKredit += rs.getDouble(9);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("rows", rows);
			hasil.put("totalDebet", totalDebet);
			hasil.put("totalKredit", totalKredit);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-45: parameter Laba/Rugi (pilihan periode/toko/sales/basis)
	// =============================================================================================

	public static void profitLossParams(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("laba_rugi")) {
			tolak(hasil, "Menu Laba Rugi tidak aktif untuk akun Anda.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray salesArr = new JSONArray();
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT s.id, s.kode, s.nama FROM koperasi.sales_inventory s"
							+ " WHERE COALESCE(s.aktif,true) = true ORDER BY s.kode LIMIT 200");
			java.sql.ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("id", rs.getLong(1));
				r.put("kode", str(rs.getString(2)));
				r.put("nama", str(rs.getString(3)));
				salesArr.put(r);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("sales", salesArr);
			JSONArray basis = new JSONArray();
			basis.put("FAKTUR"); // pendapatan diakui saat faktur AR terbit (default)
			hasil.put("basis", basis);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-46: Laba Kotor (HPP snapshot per baris order INVOICED/LUNAS)
	// =============================================================================================

	public static void grossProfitReport(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("laba_rugi")) {
			tolak(hasil, "Menu Laba Rugi tidak aktif untuk akun Anda.");
			return;
		}
		String dari = tglLiteral(request, "dari", "(CURRENT_DATE - 30)");
		String sampai = tglLiteral(request, "sampai", "CURRENT_DATE");
		String grup = request.optString("group_by", "produk").trim().toLowerCase();
		String kolomGrup;
		String joinGrup = "";
		if ("customer".equals(grup)) {
			kolomGrup = "c.id, c.nama";
			joinGrup = " JOIN koperasi.anggota_koperasi c ON o.customer = c.id";
		} else if ("sales".equals(grup)) {
			kolomGrup = "COALESCE(s.id,0), COALESCE(s.nama,'(tanpa sales)')";
			joinGrup = " LEFT JOIN koperasi.sales_inventory s ON o.sales = s.id";
		} else {
			grup = "produk";
			kolomGrup = "i.produk, i.nama_produk";
		}
		Long salesId = optLong(request, "sales_id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(
					" WHERE o.status IN ('SIAP_TAGIH','LUNAS')"
							+ " AND o.tanggal >= " + dari + " AND o.tanggal < (" + sampai + " + 1)");
			if (salesId != null) where.append(" AND o.sales = ?");
			if (ctx.tokoId != null && !ctx.admin) where.append(" AND o.toko = ?");
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT " + kolomGrup + ", SUM(i.jumlah), SUM(i.subtotal),"
							+ " SUM(i.hpp_snapshot * i.jumlah)"
							+ " FROM koperasi.sales_order_lapangan_item i"
							+ " JOIN koperasi.sales_order_lapangan o ON i.sales_order = o.id"
							+ joinGrup + where
							+ " GROUP BY " + kolomGrup + " ORDER BY 4 DESC LIMIT 300");
			int ix = 1;
			if (salesId != null) ps.setLong(ix++, salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) ps.setLong(ix++, ctx.tokoId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray rows = new JSONArray();
			double totalJual = 0, totalHpp = 0;
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("grupId", rs.getLong(1));
				r.put("grupNama", str(rs.getString(2)));
				r.put("qty", rs.getDouble(3));
				double jual = rs.getDouble(4);
				double hpp = rs.getDouble(5);
				r.put("penjualan", jual);
				r.put("hpp", hpp);
				r.put("labaKotor", jual - hpp);
				r.put("marginPersen", jual <= 0 ? 0 : (jual - hpp) / jual * 100);
				rows.put(r);
				totalJual += jual;
				totalHpp += hpp;
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("groupBy", grup);
			hasil.put("rows", rows);
			JSONObject ringkas = new JSONObject();
			ringkas.put("penjualan", totalJual);
			ringkas.put("hpp", totalHpp);
			ringkas.put("labaKotor", totalJual - totalHpp);
			ringkas.put("marginPersen", totalJual <= 0 ? 0 : (totalJual - totalHpp) / totalJual * 100);
			hasil.put("ringkasan", ringkas);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-47/48: Laporan Laba/Rugi varian (pendapatan - HPP - beban sesi) -- cetak di klien
	// =============================================================================================

	public static void profitLossReport(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("laba_rugi")) {
			tolak(hasil, "Menu Laba Rugi tidak aktif untuk akun Anda.");
			return;
		}
		String dari = tglLiteral(request, "dari", "(CURRENT_DATE - 30)");
		String sampai = tglLiteral(request, "sampai", "CURRENT_DATE");
		Long salesId = optLong(request, "sales_id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Pendapatan: faktur AR terbit pada periode (basis FAKTUR).
			StringBuilder wDoc = new StringBuilder(" WHERE d.status = 'AKTIF'"
					+ " AND d.tanggal >= " + dari + " AND d.tanggal < (" + sampai + " + 1)");
			if (salesId != null) wDoc.append(" AND d.sales = ?");
			if (ctx.tokoId != null && !ctx.admin) wDoc.append(" AND d.toko = ?");
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT COALESCE(SUM(d.total_faktur),0), COUNT(*)"
							+ " FROM koperasi.piutang_customer_doc d" + wDoc);
			int ix = 1;
			if (salesId != null) ps.setLong(ix++, salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) ps.setLong(ix++, ctx.tokoId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			double pendapatanFaktur = 0;
			long jumlahFaktur = 0;
			if (rs.next()) {
				pendapatanFaktur = rs.getDouble(1);
				jumlahFaktur = rs.getLong(2);
			}
			rs.close(); ps.close();

			// Penjualan tunai lapangan (ledger kas sesi CASH_SALE) pada periode.
			java.sql.PreparedStatement psK = session.connection().prepareStatement(
					"SELECT COALESCE(SUM(k.nominal),0) FROM koperasi.nota_sales_kas k"
							+ " WHERE k.jenis = 'CASH_SALE' AND k.waktu >= " + dari
							+ " AND k.waktu < (" + sampai + " + 1)");
			java.sql.ResultSet rsK = psK.executeQuery();
			double penjualanTunai = rsK.next() ? rsK.getDouble(1) : 0;
			rsK.close(); psK.close();

			// HPP: snapshot baris order yang difakturkan pada periode.
			StringBuilder wHpp = new StringBuilder(" WHERE o.status IN ('SIAP_TAGIH','LUNAS')"
					+ " AND o.tanggal >= " + dari + " AND o.tanggal < (" + sampai + " + 1)");
			if (salesId != null) wHpp.append(" AND o.sales = ?");
			if (ctx.tokoId != null && !ctx.admin) wHpp.append(" AND o.toko = ?");
			java.sql.PreparedStatement psH = session.connection().prepareStatement(
					"SELECT COALESCE(SUM(i.hpp_snapshot * i.jumlah),0)"
							+ " FROM koperasi.sales_order_lapangan_item i"
							+ " JOIN koperasi.sales_order_lapangan o ON i.sales_order = o.id" + wHpp);
			ix = 1;
			if (salesId != null) psH.setLong(ix++, salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) psH.setLong(ix++, ctx.tokoId.longValue());
			java.sql.ResultSet rsH = psH.executeQuery();
			double hpp = rsH.next() ? rsH.getDouble(1) : 0;
			rsH.close(); psH.close();

			// Beban: biaya sesi sales per kategori pada periode.
			java.sql.PreparedStatement psB = session.connection().prepareStatement(
					"SELECT kb.nama, COALESCE(SUM(b.nilai),0)"
							+ " FROM koperasi.nota_sales_biaya b"
							+ " JOIN koperasi.kategori_biaya_sales kb ON b.kategori = kb.id"
							+ " WHERE b.tanggal >= " + dari + " AND b.tanggal < (" + sampai + " + 1)"
							+ " GROUP BY kb.nama ORDER BY 2 DESC");
			java.sql.ResultSet rsB = psB.executeQuery();
			JSONArray beban = new JSONArray();
			double totalBeban = 0;
			while (rsB.next()) {
				JSONObject r = new JSONObject();
				r.put("kategori", str(rsB.getString(1)));
				r.put("nilai", rsB.getDouble(2));
				beban.put(r);
				totalBeban += rsB.getDouble(2);
			}
			rsB.close(); psB.close();

			double pendapatan = pendapatanFaktur + penjualanTunai;
			double labaKotor = pendapatan - hpp;
			JSONObject j = new JSONObject();
			j.put("pendapatanFaktur", pendapatanFaktur);
			j.put("jumlahFaktur", jumlahFaktur);
			j.put("penjualanTunai", penjualanTunai);
			j.put("totalPendapatan", pendapatan);
			j.put("hpp", hpp);
			j.put("labaKotor", labaKotor);
			j.put("beban", beban);
			j.put("totalBeban", totalBeban);
			j.put("labaBersih", labaKotor - totalBeban);
			j.put("catatan", "Ringkasan operasional varian Inventory & Sales (basis FAKTUR)."
					+ " Laporan keuangan penuh (Neraca/Arus Kas/Buku Besar) tetap lewat menu"
					+ " Laporan Keuangan existing.");
			hasil.put("status", "00");
			hasil.put("data", j);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
