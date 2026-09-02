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
			String sqlCoa;
			if (SalesInventoryFinanceTenant.aktif(ctx)) {
				String sk = SalesInventoryFinanceTenant.skema(ctx);
				sqlCoa = SalesInventoryFinanceTenant.selectCoa()
						+ SalesInventoryFinanceTenant.dasarCoa(sk, where.toString());
			} else {
				sqlCoa = "SELECT a.id, a.kode, a.nama, a.keterangan, a.debit_credit, p.kode, p.nama"
						+ " FROM akunting.akun a LEFT JOIN akunting.akun p ON a.parent = p.id"
						+ where + " ORDER BY a.kode LIMIT 500";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sqlCoa);
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
			if (SalesInventoryFinanceTenant.aktif(ctx)) {
				// Entitas Akun mematok @Table(schema = "akunting"): session.saveOrUpdate() akan
				// menulis ke bagan akun BERSAMA berapa pun tenant yang aktif. Jalur tenant
				// karena itu memakai SQL asli.
				simpanAkunTenant(ctx, tbmuser, request, hasil, session, kode, nama);
				return;
			}
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
			boolean jalurTenant = SalesInventoryFinanceTenant.aktif(ctx);
			String kolTgl = jalurTenant
					? SalesInventoryFinanceTenant.kolomTanggalJurnal() : "t.tanggal_transaksi";
			String kolAkun = jalurTenant
					? SalesInventoryFinanceTenant.kolomAkunJurnal() : "t.akun";
			StringBuilder where = new StringBuilder(
					" WHERE " + kolTgl + " >= " + dari
							+ " AND " + kolTgl + " < (" + sampai + " + 1)");
			if (akunId != null) where.append(" AND ").append(kolAkun).append(" = ?");
			String sqlJurnal;
			if (jalurTenant) {
				String sk = SalesInventoryFinanceTenant.skema(ctx);
				sqlJurnal = SalesInventoryFinanceTenant.selectJurnal()
						+ SalesInventoryFinanceTenant.dasarJurnal(sk, where.toString());
			} else {
				sqlJurnal = "SELECT t.id, t.kode, t.jenis_jurnal, t.tanggal_transaksi, t.keterangan,"
						+ " a.kode, a.nama, COALESCE(t.debet,0), COALESCE(t.kredit,0), t.tanggal_posting"
						+ " FROM akunting.transaksi t LEFT JOIN akunting.akun a ON t.akun = a.id"
						+ where + " ORDER BY t.tanggal_transaksi DESC, t.id DESC LIMIT 500";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sqlJurnal);
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
					SalesInventoryFinanceTenant.aktif(ctx)
							? SalesInventoryFinanceTenant.sqlSalesperson(
									SalesInventoryFinanceTenant.skema(ctx))
							: "SELECT s.id, s.kode, s.nama FROM koperasi.sales_inventory s"
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
		// "HPP Tambah (%)" legacy (layar 45/46): markup atas HPP snapshot -- eksplisit dari
		// request, 0 bila tak diisi, di-echo balik supaya tercantum di cetakan.
		double hppTambah = request.optDouble("hpp_tambah_persen", 0);
		if (hppTambah < 0 || hppTambah > 100) {
			tolak(hasil, "hpp_tambah_persen harus 0..100.");
			return;
		}
		double faktorHpp = 1 + hppTambah / 100.0;
		boolean jalurTenant = SalesInventoryFinanceTenant.aktif(ctx);
		String skemaTenant = jalurTenant ? SalesInventoryFinanceTenant.skema(ctx) : null;
		String kolomGrup;
		String joinGrup = "";
		if ("customer".equals(grup)) {
			kolomGrup = jalurTenant ? SalesInventoryFinanceTenant.kolomGrup(grup) : "c.id, c.nama";
			joinGrup = jalurTenant ? SalesInventoryFinanceTenant.joinCustomer(skemaTenant)
					: " JOIN koperasi.anggota_koperasi c ON o.customer = c.id";
		} else if ("sales".equals(grup)) {
			kolomGrup = jalurTenant ? SalesInventoryFinanceTenant.kolomGrup(grup)
					: "COALESCE(s.id,0), COALESCE(s.nama,'(tanpa sales)')";
			joinGrup = jalurTenant ? SalesInventoryFinanceTenant.joinSalesperson(skemaTenant)
					: " LEFT JOIN koperasi.sales_inventory s ON o.sales = s.id";
		} else {
			grup = "produk";
			// Baris faktur tenant tidak menyimpan salinan nama produk; namanya ditarik lewat join.
			kolomGrup = jalurTenant ? SalesInventoryFinanceTenant.kolomGrup(grup)
					: "i.produk, i.nama_produk";
			joinGrup = jalurTenant ? SalesInventoryFinanceTenant.joinProduk(skemaTenant) : "";
		}
		Long salesId = optLong(request, "sales_id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(jalurTenant
					? SalesInventoryFinanceTenant.whereFakturSah()
							+ " AND o.tanggal >= " + dari + " AND o.tanggal < (" + sampai + " + 1)"
					: " WHERE o.status IN ('SIAP_TAGIH','LUNAS')"
							+ " AND o.tanggal >= " + dari + " AND o.tanggal < (" + sampai + " + 1)");
			if (salesId != null) {
				where.append(jalurTenant
						? " AND " + SalesInventoryFinanceTenant.kolomSales() + " = ?"
						: " AND o.sales = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) {
				where.append(jalurTenant
						? " AND " + SalesInventoryFinanceTenant.kolomToko() + " = ?"
						: " AND o.toko = ?");
			}
			String sqlLaba;
			if (jalurTenant) {
				sqlLaba = "SELECT " + kolomGrup + ", " + SalesInventoryFinanceTenant.kolomUkuran()
						+ SalesInventoryFinanceTenant.dasarLabaKotor(skemaTenant, joinGrup,
								where.toString())
						+ " GROUP BY " + kolomGrup + " ORDER BY 4 DESC LIMIT 300";
			} else {
				sqlLaba = "SELECT " + kolomGrup + ", SUM(i.jumlah), SUM(i.subtotal),"
						+ " SUM(i.hpp_snapshot * i.jumlah)"
						+ " FROM koperasi.sales_order_lapangan_item i"
						+ " JOIN koperasi.sales_order_lapangan o ON i.sales_order = o.id"
						+ joinGrup + where
						+ " GROUP BY " + kolomGrup + " ORDER BY 4 DESC LIMIT 300";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sqlLaba);
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
				double hpp = rs.getDouble(5) * faktorHpp;
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
			hasil.put("hppTambahPersen", hppTambah);
			hasil.put("rows", rows);
			JSONObject ringkas = new JSONObject();
			ringkas.put("penjualan", totalJual);
			ringkas.put("hpp", totalHpp);
			ringkas.put("labaKotor", totalJual - totalHpp);
			ringkas.put("marginPersen", totalJual <= 0 ? 0 : (totalJual - totalHpp) / totalJual * 100);
			ringkas.put("hppTambahPersen", hppTambah);
			hasil.put("ringkasan", ringkas);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-47: rincian Laba/Rugi PER BARIS FAKTUR (grid legacy: Sales, Tanggal, No.Faktur,
	// Nama Barang, HPP, Hrg.Jual, Jumlah, Rugi/Laba, Customer) + filter Jual Rugi / Lunas
	// =============================================================================================

	public static void profitLossDetail(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("laba_rugi")) {
			tolak(hasil, "Menu Laba Rugi tidak aktif untuk akun Anda.");
			return;
		}
		String dari = tglLiteral(request, "dari", "(CURRENT_DATE - 30)");
		String sampai = tglLiteral(request, "sampai", "CURRENT_DATE");
		Long salesId = optLong(request, "sales_id");
		boolean hanyaRugi = "jual_rugi".equalsIgnoreCase(request.optString("filter", ""));
		// lunas | belum | (kosong = semua) -- status pelunasan faktur asal baris.
		String statusLunas = request.optString("status_lunas", "").trim().toLowerCase();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			boolean jalurTenant = SalesInventoryFinanceTenant.aktif(ctx);
			String skemaTenant = jalurTenant ? SalesInventoryFinanceTenant.skema(ctx) : null;
			String exprOut = jalurTenant
					? SalesInventoryFinanceTenant.sisaPiutang(skemaTenant)
					: "(COALESCE(d.total_faktur,0) - COALESCE(d.dibayar_awal,0)"
							+ " - COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_penerimaan_piutang_customer a"
							+ " WHERE a.piutang_doc = d.id),0))";
			String exprLaba = jalurTenant
					? SalesInventoryFinanceTenant.labaBaris()
					: "(i.subtotal - i.hpp_snapshot * i.jumlah)";
			StringBuilder where = new StringBuilder(jalurTenant
					? SalesInventoryFinanceTenant.whereFakturSah()
							+ " AND o.tanggal >= " + dari + " AND o.tanggal < (" + sampai + " + 1)"
					: " WHERE o.status IN ('SIAP_TAGIH','LUNAS')"
							+ " AND o.tanggal >= " + dari + " AND o.tanggal < (" + sampai + " + 1)");
			if (salesId != null) {
				where.append(jalurTenant
						? " AND " + SalesInventoryFinanceTenant.kolomSales() + " = ?"
						: " AND o.sales = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) {
				where.append(jalurTenant
						? " AND " + SalesInventoryFinanceTenant.kolomToko() + " = ?"
						: " AND o.toko = ?");
			}
			if (hanyaRugi) where.append(" AND " + exprLaba + " < 0");
			if ("lunas".equals(statusLunas)) where.append(" AND " + exprOut + " <= 0.009");
			else if ("belum".equals(statusLunas)) where.append(" AND " + exprOut + " > 0.009");
			String sqlRincian;
			if (jalurTenant) {
				sqlRincian = SalesInventoryFinanceTenant.selectRincian(skemaTenant)
						+ SalesInventoryFinanceTenant.dasarRincian(skemaTenant, where.toString());
			} else {
				sqlRincian = "SELECT COALESCE(s.nama,'(tanpa sales)'), o.tanggal, COALESCE(d.nomor, o.nomor),"
						+ " i.nama_produk, i.jumlah, i.hpp_snapshot, i.harga_satuan, i.subtotal,"
						+ " " + exprLaba + ", c.nama, " + exprOut
						+ " FROM koperasi.sales_order_lapangan_item i"
						+ " JOIN koperasi.sales_order_lapangan o ON i.sales_order = o.id"
						+ " JOIN koperasi.anggota_koperasi c ON o.customer = c.id"
						+ " LEFT JOIN koperasi.sales_inventory s ON o.sales = s.id"
						+ " LEFT JOIN koperasi.piutang_customer_doc d ON d.sales_order = o.id" + where
						+ " ORDER BY o.tanggal, o.id, i.id LIMIT 500";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sqlRincian);
			int ix = 1;
			if (salesId != null) ps.setLong(ix++, salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) ps.setLong(ix++, ctx.tokoId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray rows = new JSONArray();
			double totalJual = 0, totalHpp = 0;
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("salesNama", str(rs.getString(1)));
				r.put("tanggal", str(rs.getTimestamp(2)));
				r.put("fakturNomor", str(rs.getString(3)));
				r.put("namaProduk", str(rs.getString(4)));
				r.put("qty", rs.getDouble(5));
				r.put("hppSatuan", rs.getDouble(6));
				r.put("hargaJual", rs.getDouble(7));
				r.put("jumlah", rs.getDouble(8));
				r.put("labaRugi", rs.getDouble(9));
				r.put("customerNama", str(rs.getString(10)));
				r.put("lunas", rs.getDouble(11) <= 0.009);
				rows.put(r);
				totalJual += rs.getDouble(8);
				totalHpp += rs.getDouble(6) * rs.getDouble(5);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("rows", rows);
			JSONObject ringkas = new JSONObject();
			ringkas.put("penjualan", totalJual);
			ringkas.put("hpp", totalHpp);
			ringkas.put("labaRugi", totalJual - totalHpp);
			hasil.put("ringkasan", ringkas);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// Riwayat Audit per record (aksi "Riwayat Audit" di seluruh layar master -- baca Envers)
	// =============================================================================================

	/** Peta jenis entity -> kelas + kunci menu penjaga. Whitelist eksplisit, bukan reflection
	 *  bebas -- mencegah pembacaan entity di luar varian. */
	private static Object[] petaAudit(String jenis) {
		if ("supplier".equals(jenis))
			return new Object[] { ais.database.model.koperasi.SupplierInventoryProfile.class, "master_supplier" };
		if ("customer".equals(jenis))
			return new Object[] { ais.database.model.koperasi.CustomerInventoryProfile.class, "master_customer" };
		if ("sales".equals(jenis))
			return new Object[] { ais.database.model.koperasi.SalesInventory.class, "master_sales" };
		if ("piutang".equals(jenis))
			return new Object[] { ais.database.model.koperasi.PiutangCustomerDoc.class, "piutang" };
		if ("penerimaan".equals(jenis))
			return new Object[] { ais.database.model.koperasi.PenerimaanPiutangCustomer.class, "piutang" };
		if ("order".equals(jenis))
			return new Object[] { ais.database.model.koperasi.SalesOrderLapangan.class, "penjualan_sales" };
		if ("spj".equals(jenis))
			return new Object[] { ais.database.model.koperasi.SuratPerintahSalesJalan.class, "surat_perintah_sales" };
		return null;
	}

	public static void auditHistory(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil) throws Exception {
		String jenis = request.optString("entity", "").trim().toLowerCase();
		Long id = optLong(request, "id");
		Object[] peta = petaAudit(jenis);
		if (peta == null || id == null) {
			tolak(hasil, "entity (supplier/customer/sales/piutang/penerimaan/order/spj) dan id wajib diisi.");
			return;
		}
		if (!ctx.bolehMenu((String) peta[1])) {
			tolak(hasil, "Menu terkait tidak aktif untuk akun Anda.");
			return;
		}
		if (SalesInventoryFinanceTenant.aktif(ctx)) {
			// Envers menaruh seluruh barisnya pada satu schema yang ditetapkan statis per
			// SessionFactory (default_schema=new_audit): membiarkannya berjalan akan menyajikan
			// riwayat perubahan SELURUH instalasi kepada satu tenant. Jalur tenant karena itu
			// membaca jejaknya sendiri, yang ditulis TenantAuditWriter ke schema audit tenant.
			auditHistoryTenant(ctx, hasil, jenis, id);
			return;
		}
		Class<?> kelas = (Class<?>) peta[0];
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.envers.AuditReader reader =
					org.hibernate.envers.AuditReaderFactory.get(session);
			@SuppressWarnings("unchecked")
			java.util.List<Number> revs = reader.getRevisions((Class<Object>) kelas, id);
			JSONArray rows = new JSONArray();
			// 25 revisi terakhir -- cukup utk telaah perubahan tanpa membebani respon.
			int mulai = Math.max(0, revs.size() - 25);
			for (int i = revs.size() - 1; i >= mulai; i--) {
				Number rev = revs.get(i);
				Object snap = reader.find(kelas, id, rev);
				JSONObject r = new JSONObject();
				r.put("revisi", rev.longValue());
				r.put("waktu", str(reader.getRevisionDate(rev)));
				JSONObject nilai = new JSONObject();
				if (snap != null) {
					// Hanya getter skalar (String/Number/Boolean/Date) -- relasi/koleksi dilewati
					// supaya snapshot ringkas dan bebas lazy-loading.
					for (java.lang.reflect.Method m : kelas.getMethods()) {
						if (!m.getName().startsWith("get") || m.getParameterTypes().length != 0
								|| "getClass".equals(m.getName())) {
							continue;
						}
						Class<?> tipe = m.getReturnType();
						if (tipe != String.class && !Number.class.isAssignableFrom(tipe)
								&& tipe != Boolean.class && tipe != java.util.Date.class
								&& !tipe.isPrimitive()) {
							continue;
						}
						try {
							Object v = m.invoke(snap);
							if (v != null) {
								nilai.put(m.getName().substring(3), str(v));
							}
						} catch (Exception lewati) {
							// properti audit yang gagal dibaca dilewati senyap.
						}
					}
				}
				r.put("nilai", nilai);
				rows.put(r);
			}
			hasil.put("status", "00");
			hasil.put("rows", rows);
			hasil.put("totalRevisi", revs.size());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Riwayat audit satu baris data pada schema tenant.
	 *
	 * <h4>Mengapa bukan Envers</h4>
	 * <p>{@code org.hibernate.envers.default_schema} bersifat <b>statis per SessionFactory</b>,
	 * sehingga baris audit seluruh tenant berkumpul di satu schema. Membacanya untuk satu tenant
	 * berarti menyajikan riwayat perubahan seluruh instalasi kepadanya — kebocoran, bukan sekadar
	 * hasil yang salah. Jalur tenant karena itu membaca {@code audit_baris} miliknya sendiri.</p>
	 *
	 * <h4>Entitas di luar cakupan DITOLAK, bukan dijawab kosong</h4>
	 * <p>Jejak audit hanya ada untuk entitas yang penulisnya sudah terpasang. Menjawab entitas
	 * lain dengan daftar kosong berarti mengatakan "tidak pernah berubah" tentang record yang
	 * jelas pernah berubah — persis kekeliruan yang penolakan lama justru hindari. Penolakannya
	 * menyebut entitas mana yang sudah terliput.</p>
	 *
	 * <h4>Riwayat dimulai saat pencatatannya dipasang</h4>
	 * <p>Tidak ada pengisian surut: perubahan yang terjadi sebelum penulisnya terpasang memang
	 * tidak terekam, dan tidak dapat direkayasa belakangan. Karena itu jawaban yang kosong
	 * disertai {@code peringatan} yang mengatakan begitu, alih-alih membiarkannya terbaca sebagai
	 * "record ini tidak pernah disentuh".</p>
	 */
	private static void auditHistoryTenant(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject hasil, String jenis, Long id) throws Exception {
		String tabel = SalesInventoryFinanceTenant.tabelTeraudit(jenis);
		if (tabel == null) {
			tolak(hasil, "Riwayat audit untuk \"" + jenis + "\" belum dicatat pada tenant"
					+ " berschema. Yang sudah terliput: "
					+ SalesInventoryFinanceTenant.daftarEntitasTeraudit() + ".");
			return;
		}
		String audit = SalesInventoryFinanceTenant.skemaAudit(ctx);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			long total = 0;
			java.sql.PreparedStatement psC = session.connection().prepareStatement(
					SalesInventoryFinanceTenant.cacahRiwayatAudit(audit));
			try {
				psC.setString(1, tabel);
				psC.setString(2, String.valueOf(id));
				java.sql.ResultSet rsC = psC.executeQuery();
				if (rsC.next()) {
					total = rsC.getLong(1);
				}
				rsC.close();
			} finally {
				psC.close();
			}
			JSONArray rows = new JSONArray();
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					SalesInventoryFinanceTenant.selectRiwayatAudit(audit));
			try {
				ps.setString(1, tabel);
				ps.setString(2, String.valueOf(id));
				java.sql.ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					JSONObject r = new JSONObject();
					r.put("revisi", rs.getLong(1));
					r.put("waktu", str(rs.getTimestamp(2)));
					r.put("revtype", rs.getInt(3));
					// "nilai" mengikuti bentuk jalur legacy: keadaan SESUDAH perubahan. Pada
					// penghapusan ia memang kosong, sama seperti Envers mengembalikan null.
					r.put("nilai", muatanAudit(rs.getString(5)));
					r.put("sebelum", muatanAudit(rs.getString(4)));
					r.put("oleh", str(rs.getString(6)));
					r.put("peran", str(rs.getString(7)));
					r.put("aksi", str(rs.getString(8)));
					r.put("alasan", str(rs.getString(9)));
					r.put("requestId", str(rs.getString(10)));
					rows.put(r);
				}
				rs.close();
			} finally {
				ps.close();
			}
			hasil.put("status", "00");
			hasil.put("rows", rows);
			hasil.put("totalRevisi", total);
			if (total == 0) {
				hasil.put("peringatan", "Belum ada jejak audit untuk data ini pada tenant"
						+ " berschema. Pencatatannya dimulai sejak penulis audit dipasang;"
						+ " perubahan sebelum itu tidak terekam dan tidak dapat direkayasa"
						+ " belakangan.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Muatan {@code sebelum}/{@code sesudah} sebagai objek JSON. Muatannya memang disimpan
	 * sebagai teks JSON oleh penulisnya; bila suatu baris ternyata bukan JSON yang sah —
	 * misalnya ditulis alat lain — isinya dikembalikan apa adanya pada medan {@code teks},
	 * bukan dibuang diam-diam.
	 */
	private static JSONObject muatanAudit(String teks) throws Exception {
		JSONObject kosong = new JSONObject();
		if (teks == null || teks.trim().length() == 0) {
			return kosong;
		}
		try {
			return new JSONObject(teks);
		} catch (Exception bukanJson) {
			kosong.put("teks", teks);
			return kosong;
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
			boolean jalurTenant = SalesInventoryFinanceTenant.aktif(ctx);
			String skemaTenant = jalurTenant ? SalesInventoryFinanceTenant.skema(ctx) : null;
			// Pendapatan: faktur AR terbit pada periode (basis FAKTUR).
			StringBuilder wDoc = new StringBuilder(" WHERE d.status = 'AKTIF'"
					+ " AND d.tanggal >= " + dari + " AND d.tanggal < (" + sampai + " + 1)");
			if (salesId != null) {
				wDoc.append(jalurTenant
						? " AND " + SalesInventoryFinanceTenant.kolomSalesPiutang() + " = ?"
						: " AND d.sales = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) {
				wDoc.append(jalurTenant
						? " AND " + SalesInventoryFinanceTenant.kolomTokoPiutang() + " = ?"
						: " AND d.toko = ?");
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(jalurTenant
					? SalesInventoryFinanceTenant.sqlOmzetKredit(skemaTenant, wDoc.toString())
					: "SELECT COALESCE(SUM(d.total_faktur),0), COUNT(*)"
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
			// Saringan lingkup untuk omzet tunai memakai kolom faktur; legacy tidak
			// menyaringnya sama sekali pada ledger kas, dan perilaku itu dipertahankan.
			java.sql.PreparedStatement psK = session.connection().prepareStatement(jalurTenant
					? SalesInventoryFinanceTenant.sqlOmzetTunai(skemaTenant, dari, sampai, "")
					: "SELECT COALESCE(SUM(k.nominal),0) FROM koperasi.nota_sales_kas k"
							+ " WHERE k.jenis = 'CASH_SALE' AND k.waktu >= " + dari
							+ " AND k.waktu < (" + sampai + " + 1)");
			java.sql.ResultSet rsK = psK.executeQuery();
			double penjualanTunai = rsK.next() ? rsK.getDouble(1) : 0;
			rsK.close(); psK.close();

			// HPP: snapshot baris order yang difakturkan pada periode.
			StringBuilder wHpp = new StringBuilder(jalurTenant
					? SalesInventoryFinanceTenant.whereFakturSah()
							+ " AND o.tanggal >= " + dari + " AND o.tanggal < (" + sampai + " + 1)"
					: " WHERE o.status IN ('SIAP_TAGIH','LUNAS')"
							+ " AND o.tanggal >= " + dari + " AND o.tanggal < (" + sampai + " + 1)");
			if (salesId != null) {
				wHpp.append(jalurTenant
						? " AND " + SalesInventoryFinanceTenant.kolomSales() + " = ?"
						: " AND o.sales = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) {
				wHpp.append(jalurTenant
						? " AND " + SalesInventoryFinanceTenant.kolomToko() + " = ?"
						: " AND o.toko = ?");
			}
			java.sql.PreparedStatement psH = session.connection().prepareStatement(jalurTenant
					? SalesInventoryFinanceTenant.sqlHpp(skemaTenant, wHpp.toString())
					: "SELECT COALESCE(SUM(i.hpp_snapshot * i.jumlah),0)"
							+ " FROM koperasi.sales_order_lapangan_item i"
							+ " JOIN koperasi.sales_order_lapangan o ON i.sales_order = o.id" + wHpp);
			ix = 1;
			if (salesId != null) psH.setLong(ix++, salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) psH.setLong(ix++, ctx.tokoId.longValue());
			java.sql.ResultSet rsH = psH.executeQuery();
			double hpp = rsH.next() ? rsH.getDouble(1) : 0;
			rsH.close(); psH.close();

			// Beban: biaya sesi sales per kategori pada periode.
			java.sql.PreparedStatement psB = session.connection().prepareStatement(jalurTenant
					? SalesInventoryFinanceTenant.sqlBeban(skemaTenant, dari, sampai)
					: "SELECT kb.nama, COALESCE(SUM(b.nilai),0)"
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

	/**
	 * Simpan akun pada schema tenant, lewat SQL asli.
	 *
	 * <h4>Pembuatan akun bukan pelengkap; tanpanya bagan akun tenant tidak dapat dihuni</h4>
	 * <p>Tidak ada penyemai bagan akun di katalog migrasi, dan aksi ini satu-satunya penulis
	 * {@code akun} pada schema tenant. Selama pembuatan ditolak, tabelnya tetap kosong
	 * selamanya — dan {@code jurnal_detail.akun_id} bersifat {@code NOT NULL REFERENCES akun(id)},
	 * sehingga satu baris jurnal pun tidak akan pernah bisa ditulis. Yang tertutup bukan satu
	 * layar master, melainkan seluruh pembukuan tenant.</p>
	 *
	 * <h4>Kelas akun: diminta, atau diwarisi dari induknya</h4>
	 * <p>{@code tipe} bersifat {@code NOT NULL} sedangkan permintaan legacy tidak membawanya.
	 * Ada dua jalan keluar yang keduanya bukan tebakan. Yang pertama: permintaan menyebut
	 * {@code tipe} sendiri. Yang kedua: akun anak <b>mewarisi</b> kelas induknya — sub-akun dari
	 * sebuah akun aset adalah aset, dan itu definisi, bukan dugaan. Bila permintaan tidak
	 * menyebut kelas maupun induk, barulah ditolak, dan penolakannya menyebutkan kedua jalannya.</p>
	 * <p>Yang tetap TIDAK dilakukan: menyimpulkan kelas dari awalan kode akun. Konvensi
	 * "1=aset, 2=kewajiban" memang lazim, tetapi ia konvensi satu bagan akun, bukan aturan; salah
	 * menebaknya menaruh akun pada sisi neraca yang keliru dan baru terlihat saat laporan
	 * disusun.</p>
	 *
	 * <h4>{@code keterangan} tetap tidak punya tempat</h4>
	 * <p>Model tenant tidak punya kolomnya. Permintaan yang mengirimnya tidak ditolak — itu akan
	 * mematahkan klien yang selalu mengirim medan itu — melainkan dijawab dengan
	 * {@code peringatan}, supaya pemanggil tahu keterangannya tidak tersimpan alih-alih
	 * mengiranya tersimpan.</p>
	 */
	private static void simpanAkunTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject request, JSONObject hasil, Session session, String kode,
			String nama) throws Exception {
		String sk = SalesInventoryFinanceTenant.skema(ctx);
		String oleh = tbmuser == null || tbmuser.getUserId() == null ? "" : tbmuser.getUserId();
		Long id = optLong(request, "akun_id");
		Long indukId = optLong(request, "parent_id");

		// -------------------------------------------------------------- induk, bila disebut
		String tipeInduk = null;
		if (indukId != null) {
			if (id != null && indukId.equals(id)) {
				tolak(hasil, "Induk akun tidak boleh dirinya sendiri.");
				return;
			}
			tipeInduk = satuTeks(session, SalesInventoryFinanceTenant.tipeAkun(sk), indukId);
			if (tipeInduk == null) {
				tolak(hasil, "Akun induk tidak ditemukan pada tenant ini.");
				return;
			}
		}

		// -------------------------------------------------------------- saldo normal, bila disebut
		// Nol berarti "tidak disebut", bukan sandi yang salah. Klien yang selalu mengirim
		// debet_credit dengan bawaan 0 tetap dapat membuat akun -- saldo normalnya lalu diambil
		// dari kelas akunnya. Menyimpan nol justru merusak: laporan keuangan MENGALIKAN saldo
		// dengan sandi ini, sehingga akun bersandi nol selalu tampil nol.
		String saldoNormal = null;
		int sandi = request.isNull("debet_credit") ? 0 : request.optInt("debet_credit", 0);
		if (sandi != 0) {
			saldoNormal = SalesInventoryFinanceTenant.saldoNormalDariSandi(sandi);
			if (saldoNormal == null) {
				tolak(hasil, "debet_credit tidak dikenali: pakai 1 untuk debet, -1 atau 2 untuk"
						+ " kredit, atau kosongkan supaya diambil dari kelas akunnya.");
				return;
			}
		}

		if (id == null) {
			buatAkunTenant(request, hasil, session, sk, kode, nama, indukId, tipeInduk,
					saldoNormal, oleh);
		} else {
			ubahAkunTenant(request, hasil, session, sk, kode, nama, id, indukId, saldoNormal,
					oleh);
		}
	}

	/** Satu kolom teks dari satu baris ber-id, atau {@code null} bila barisnya tidak ada. */
	private static String satuTeks(Session session, String sql, Long id) throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
		try {
			ps.setLong(1, id.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			String nilai = rs.next() ? rs.getString(1) : null;
			rs.close();
			return nilai;
		} finally {
			ps.close();
		}
	}

	/**
	 * Nama akun lain yang sudah memakai kode ini, atau {@code null} bila kodenya bebas.
	 * {@code kecuali} = id yang tidak dihitung (0 saat menyisipkan).
	 */
	private static String pemakaiKode(Session session, String skema, String kode, long kecuali)
			throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				SalesInventoryFinanceTenant.kodeAkunDipakai(skema));
		try {
			ps.setString(1, kode);
			ps.setLong(2, kecuali);
			java.sql.ResultSet rs = ps.executeQuery();
			String nama = rs.next() ? rs.getString(1) : null;
			rs.close();
			return nama;
		} finally {
			ps.close();
		}
	}

	/** Akun baru pada schema tenant. */
	private static void buatAkunTenant(JSONObject request, JSONObject hasil, Session session,
			String sk, String kode, String nama, Long indukId, String tipeInduk,
			String saldoNormal, String oleh) throws Exception {
		String tipe = request.optString("tipe", "").trim().toUpperCase();
		if (tipe.isEmpty()) {
			tipe = tipeInduk; // sub-akun mewarisi kelas induknya
		}
		if (tipe == null || tipe.isEmpty()) {
			tolak(hasil, "Kelas akun (tipe) wajib diisi saat membuat akun baru pada tenant"
					+ " berschema: sebutkan tipe (" + SalesInventoryFinanceTenant.daftarTipe()
					+ "), atau sebutkan parent_id supaya kelasnya diwarisi dari akun induk.");
			return;
		}
		if (!SalesInventoryFinanceTenant.tipeSah(tipe)) {
			tolak(hasil, "Kelas akun \"" + tipe + "\" tidak dikenali. Yang diterima: "
					+ SalesInventoryFinanceTenant.daftarTipe() + ".");
			return;
		}
		String dipakai = pemakaiKode(session, sk, kode, 0L);
		if (dipakai != null) {
			tolak(hasil, "Kode akun " + kode + " sudah dipakai (" + str(dipakai) + ").");
			return;
		}
		if (saldoNormal == null) {
			saldoNormal = SalesInventoryFinanceTenant.saldoNormalBawaan(tipe);
		}
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			long baru;
			java.sql.PreparedStatement ins = session.connection().prepareStatement(
					SalesInventoryFinanceTenant.sisipAkun(sk));
			try {
				ins.setString(1, kode);
				ins.setString(2, nama);
				ins.setString(3, tipe);
				if (indukId == null) {
					ins.setNull(4, java.sql.Types.BIGINT);
				} else {
					ins.setLong(4, indukId.longValue());
				}
				ins.setString(5, saldoNormal);
				ins.setString(6, oleh);
				java.sql.ResultSet rs = ins.executeQuery();
				baru = rs.next() ? rs.getLong(1) : 0L;
				rs.close();
			} finally {
				ins.close();
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", Long.valueOf(baru));
			hasil.put("tipe", tipe);
			peringatanKeterangan(request, hasil);
		} catch (Exception e) {
			batalkan(tx);
			throw e;
		}
	}

	/** Pembaruan akun pada schema tenant. */
	private static void ubahAkunTenant(JSONObject request, JSONObject hasil, Session session,
			String sk, String kode, String nama, Long id, Long indukId, String saldoNormal,
			String oleh) throws Exception {
		java.sql.PreparedStatement cek = session.connection().prepareStatement(
				SalesInventoryFinanceTenant.adaAkun(sk));
		boolean ada;
		try {
			cek.setLong(1, id.longValue());
			java.sql.ResultSet rs = cek.executeQuery();
			ada = rs.next() && rs.getLong(1) > 0;
			rs.close();
		} finally {
			cek.close();
		}
		if (!ada) {
			tolak(hasil, "Akun tidak ditemukan pada tenant ini.");
			return;
		}
		String dipakai = pemakaiKode(session, sk, kode, id.longValue());
		if (dipakai != null) {
			tolak(hasil, "Kode akun " + kode + " sudah dipakai (" + str(dipakai) + ").");
			return;
		}
		if (indukId != null && berlingkar(session, sk, id, indukId)) {
			tolak(hasil, "Akun induk yang dipilih berada di bawah akun ini; pemasangannya akan"
					+ " membentuk lingkaran pada bagan akun.");
			return;
		}
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			java.sql.PreparedStatement upd = session.connection().prepareStatement(
					SalesInventoryFinanceTenant.ubahAkun(sk));
			try {
				upd.setString(1, kode);
				upd.setString(2, nama);
				if (indukId == null) {
					upd.setNull(3, java.sql.Types.BIGINT);
				} else {
					upd.setLong(3, indukId.longValue());
				}
				upd.setString(4, saldoNormal);
				upd.setString(5, oleh);
				upd.setLong(6, id.longValue());
				upd.executeUpdate();
			} finally {
				upd.close();
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", id);
			peringatanKeterangan(request, hasil);
		} catch (Exception e) {
			batalkan(tx);
			throw e;
		}
	}

	/** Benar bila memasang {@code indukId} sebagai induk {@code id} akan membentuk lingkaran. */
	private static boolean berlingkar(Session session, String skema, Long id, Long indukId)
			throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				SalesInventoryFinanceTenant.indukBerlingkar(skema));
		try {
			ps.setLong(1, id.longValue());
			ps.setLong(2, indukId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			boolean ya = rs.next() && rs.getLong(1) > 0;
			rs.close();
			return ya;
		} finally {
			ps.close();
		}
	}

	/**
	 * Beri tahu pemanggil bila ia mengirim {@code keterangan}: model tenant tidak punya kolomnya,
	 * dan diam akan membuatnya mengira keterangannya tersimpan.
	 */
	private static void peringatanKeterangan(JSONObject request, JSONObject hasil)
			throws Exception {
		if (!request.isNull("keterangan")
				&& !request.optString("keterangan", "").trim().isEmpty()) {
			hasil.put("peringatan", "Keterangan akun tidak disimpan pada tenant berschema:"
					+ " model tenant tidak punya kolomnya.");
		}
	}

	/** Rollback yang tidak boleh menutupi galat aslinya. */
	private static void batalkan(Transaction tx) {
		try {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
		} catch (Exception ignore) {
			// rollback gagal tidak boleh menutupi galat aslinya
		}
	}
}
