package ais.action.servlet.api;

import java.math.BigDecimal;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Produk;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.HargaBeliSupplier;
import ais.database.model.koperasi.HargaJualCustomer;
import ais.database.model.library.Penyedia;

/**
 * <h3>Master &amp; Analisis Harga -- layar legacy 11-13 &amp; 17-19, varian Inventory &amp; Sales.</h3>
 *
 * <p>Aturan berversi (Matriks layar 18-19): pasangan pihak-produk unik per {@code tanggal_efektif};
 * duplikat tanggal sama DITOLAK; UPDATE hanya boleh mengubah keterangan/status aktif -- perubahan
 * HARGA atau TANGGAL = buat versi baru (histori tidak pernah ditimpa; "Hapus Versi" = nonaktif).
 * Resolusi harga transaksi: versi aktif dgn tanggal efektif terbaru &le; tanggal transaksi;
 * customer spesifik menang atas baris umum (anggota null).</p>
 */
public final class SalesInventoryHargaHelper {

	private SalesInventoryHargaHelper() {
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

	private static void isiOleh(Object entity, Tbmuser tbmuser) {
		try {
			entity.getClass().getMethod("setOleh", String.class).invoke(entity, tbmuser.getUserId());
			entity.getClass().getMethod("setOlehId", String.class).invoke(entity, tbmuser.getUserId());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "SalesInventoryHargaHelper.isiOleh");
		}
	}

	private static java.util.Date parseTanggal(String s) {
		try {
			return s == null || !s.matches("\\d{4}-\\d{2}-\\d{2}") ? null
					: new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s);
		} catch (Exception e) {
			return null;
		}
	}

	// =============================================================================================
	// HARGA BELI SUPPLIER (layar 18)
	// =============================================================================================

	public static void supplierPriceList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		Long supplierId = optLong(request, "supplier_id");
		Long produkId = optLong(request, "produk_id");
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = Math.min(100, Math.max(1, request == null ? 20 : request.optInt("page_size", 20)));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			boolean jalurTenant = SalesInventoryHargaTenant.aktif(ctx);
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (supplierId != null) {
				where.append(jalurTenant ? " AND h.supplier_id = ? " : " AND h.supplier = ? ");
				params.add(supplierId);
			}
			if (produkId != null) {
				where.append(jalurTenant ? " AND h.produk_id = ? " : " AND h.produk = ? ");
				params.add(produkId);
			}
			String dasar;
			String pilih;
			String urut;
			if (jalurTenant) {
				String sk = SalesInventoryHargaTenant.skema(ctx);
				dasar = SalesInventoryHargaTenant.dasarSupplier(sk, where.toString());
				pilih = SalesInventoryHargaTenant.selectSupplier();
				urut = SalesInventoryHargaTenant.urutSupplier();
			} else {
				dasar = " FROM koperasi.harga_beli_supplier h JOIN library.penyedia s ON h.supplier = s.id "
						+ "JOIN koperasi.produk p ON h.produk = p.id " + where;
				pilih = "SELECT h.id, h.supplier, s.kode, s.nama, h.produk, p.kode, p.nama, h.harga, "
						+ "h.tanggal_efektif, COALESCE(h.keterangan,''), COALESCE(h.aktif,true), COALESCE(h.oleh,'')";
				urut = " ORDER BY p.kode ASC, h.tanggal_efektif DESC, h.id DESC LIMIT ? OFFSET ?";
			}
			java.sql.PreparedStatement psTotal = session.connection().prepareStatement("SELECT COUNT(*) " + dasar);
			for (int i = 0; i < params.size(); i++) psTotal.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsTotal = psTotal.executeQuery();
			long total = rsTotal.next() ? rsTotal.getLong(1) : 0;
			rsTotal.close(); psTotal.close();

			java.sql.PreparedStatement ps = session.connection().prepareStatement(pilih + dasar + urut);
			int idx = 1;
			for (int i = 0; i < params.size(); i++) ps.setObject(idx++, params.get(i));
			ps.setInt(idx++, size);
			ps.setInt(idx++, (page - 1) * size);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("supplierId", rs.getLong(2));
				j.put("supplierKode", str(rs.getString(3)));
				j.put("supplierNama", str(rs.getString(4)));
				j.put("produkId", rs.getLong(5));
				j.put("produkKode", str(rs.getString(6)));
				j.put("produkNama", str(rs.getString(7)));
				j.put("harga", rs.getDouble(8));
				j.put("tanggalEfektif", str(rs.getDate(9)));
				j.put("keterangan", str(rs.getString(10)));
				j.put("aktif", rs.getBoolean(11));
				j.put("oleh", str(rs.getString(12)));
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", size);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void supplierPriceSave(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		Long id = optLong(request, "id");
		boolean baru = id == null;
		if (!ctx.bolehAksi("harga", baru ? "create" : "update")) {
			tolak(hasil, "Akun Anda tidak berhak mengelola Master Harga.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			if (SalesInventoryHargaTenant.aktif(ctx)) {
				// Entitas Hibernate mematok @Table(schema = "koperasi"), sehingga
				// session.saveOrUpdate() SELALU menulis ke schema bersama berapa pun tenant
				// yang aktif. Jalur tenant karena itu menulis lewat SQL asli. Aturannya sama:
				// tanggal efektif ganda ditolak, versi tersimpan tidak boleh diubah harganya.
				simpanHargaTenant(ctx, tbmuser, request, hasil, session, true, id);
				return;
			}
			HargaBeliSupplier h;
			if (baru) {
				Long supplierId = optLong(request, "supplier_id");
				Long produkId = optLong(request, "produk_id");
				java.util.Date tanggal = parseTanggal(request.optString("tanggal_efektif", "").trim());
				BigDecimal harga = request.isNull("harga") ? null
						: new BigDecimal((request.get("harga") + "").trim().replace(',', '.'));
				if (supplierId == null || produkId == null || tanggal == null || harga == null) {
					tolak(hasil, "supplier_id, produk_id, harga, dan tanggal_efektif (yyyy-MM-dd) wajib diisi.");
					return;
				}
				Penyedia s = (Penyedia) session.get(Penyedia.class, supplierId);
				Produk p = (Produk) session.get(Produk.class, produkId);
				if (s == null || p == null) {
					tolak(hasil, "Supplier/produk tidak ditemukan.");
					return;
				}
				java.sql.PreparedStatement cek = session.connection().prepareStatement(
						"SELECT COUNT(*) FROM koperasi.harga_beli_supplier WHERE supplier = ? AND produk = ? AND tanggal_efektif = ?");
				cek.setLong(1, supplierId.longValue());
				cek.setLong(2, produkId.longValue());
				cek.setDate(3, new java.sql.Date(tanggal.getTime()));
				java.sql.ResultSet rsCek = cek.executeQuery();
				boolean dobel = rsCek.next() && rsCek.getLong(1) > 0;
				rsCek.close(); cek.close();
				if (dobel) {
					tolak(hasil, "Sudah ada versi harga supplier-produk ini pada tanggal efektif yang sama (overlap ditolak).");
					return;
				}
				h = new HargaBeliSupplier();
				h.setSupplier(s);
				h.setProduk(p);
				h.setHarga(harga);
				h.setTanggalEfektif(tanggal);
			} else {
				h = (HargaBeliSupplier) session.get(HargaBeliSupplier.class, id);
				if (h == null) {
					tolak(hasil, "Versi harga tidak ditemukan.");
					return;
				}
				// Histori tidak ditimpa: harga/tanggal/pihak/produk TERKUNCI pada versi tersimpan;
				// perubahan harga = buat VERSI BARU (create). Update hanya keterangan/status aktif.
				if (!request.isNull("harga") || !request.isNull("tanggal_efektif")
						|| !request.isNull("supplier_id") || !request.isNull("produk_id")) {
					tolak(hasil, "Versi tersimpan tidak boleh diubah harganya -- buat versi baru dengan tanggal efektif baru.");
					return;
				}
			}
			if (!request.isNull("keterangan")) h.setKeterangan(request.optString("keterangan", "").trim());
			if (!request.isNull("aktif")) h.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			isiOleh(h, tbmuser);
			tx = session.beginTransaction();
			session.saveOrUpdate(h);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", h.getId());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// HARGA JUAL CUSTOMER (layar 19; baris anggota NULL = daftar harga umum, layar 13)
	// =============================================================================================

	public static void customerPriceList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		Long anggotaId = optLong(request, "anggota_id");
		boolean hanyaUmum = request != null && request.optBoolean("hanya_umum", false);
		Long produkId = optLong(request, "produk_id");
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = Math.min(100, Math.max(1, request == null ? 20 : request.optInt("page_size", 20)));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			boolean jalurTenant = SalesInventoryHargaTenant.aktif(ctx);
			String kolomAnggota = jalurTenant ? "h.customer_id" : "h.anggota_koperasi";
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (hanyaUmum && jalurTenant && !SalesInventoryHargaTenant.dukungBarisHargaUmum()) {
				// Mengembalikan daftar kosong akan terbaca sebagai "memang belum ada datanya",
				// padahal model tenant memang TIDAK BISA menyimpannya (customer_id NOT NULL).
				tolak(hasil, "Harga umum belum dapat disimpan sebagai baris tersendiri pada tenant "
						+ "berschema; harga jual standar produk yang berlaku. Lihat catatan gap "
						+ "customer_id pada SalesInventoryHargaTenant.");
				return;
			}
			if (hanyaUmum) {
				where.append(" AND ").append(kolomAnggota).append(" IS NULL ");
			} else if (anggotaId != null) {
				where.append(" AND ").append(kolomAnggota).append(" = ? ");
				params.add(anggotaId);
			}
			if (produkId != null) {
				where.append(jalurTenant ? " AND h.produk_id = ? " : " AND h.produk = ? ");
				params.add(produkId);
			}
			String dasar;
			String pilih;
			String urut;
			if (jalurTenant) {
				String sk = SalesInventoryHargaTenant.skema(ctx);
				dasar = SalesInventoryHargaTenant.dasarCustomer(sk, where.toString());
				pilih = SalesInventoryHargaTenant.selectCustomer();
				urut = SalesInventoryHargaTenant.urutCustomer();
			} else {
				dasar = " FROM koperasi.harga_jual_customer h "
						+ "LEFT JOIN koperasi.anggota_koperasi a ON h.anggota_koperasi = a.id "
						+ "JOIN koperasi.produk p ON h.produk = p.id " + where;
				pilih = "SELECT h.id, h.anggota_koperasi, COALESCE(a.kode,''), COALESCE(a.nama,'(Umum)'), "
						+ "h.produk, p.kode, p.nama, h.harga, h.tanggal_efektif, COALESCE(h.keterangan,''), "
						+ "COALESCE(h.aktif,true), COALESCE(h.oleh,'')";
				urut = " ORDER BY p.kode ASC, h.tanggal_efektif DESC, h.id DESC LIMIT ? OFFSET ?";
			}
			java.sql.PreparedStatement psTotal = session.connection().prepareStatement("SELECT COUNT(*) " + dasar);
			for (int i = 0; i < params.size(); i++) psTotal.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsTotal = psTotal.executeQuery();
			long total = rsTotal.next() ? rsTotal.getLong(1) : 0;
			rsTotal.close(); psTotal.close();

			java.sql.PreparedStatement ps = session.connection().prepareStatement(pilih + dasar + urut);
			int idx = 1;
			for (int i = 0; i < params.size(); i++) ps.setObject(idx++, params.get(i));
			ps.setInt(idx++, size);
			ps.setInt(idx++, (page - 1) * size);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				long aId = rs.getLong(2);
				j.put("anggotaId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(aId));
				j.put("customerKode", str(rs.getString(3)));
				j.put("customerNama", str(rs.getString(4)));
				j.put("produkId", rs.getLong(5));
				j.put("produkKode", str(rs.getString(6)));
				j.put("produkNama", str(rs.getString(7)));
				j.put("harga", rs.getDouble(8));
				j.put("tanggalEfektif", str(rs.getDate(9)));
				j.put("keterangan", str(rs.getString(10)));
				j.put("aktif", rs.getBoolean(11));
				j.put("oleh", str(rs.getString(12)));
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", size);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void customerPriceSave(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		Long id = optLong(request, "id");
		boolean baru = id == null;
		if (!ctx.bolehAksi("harga", baru ? "create" : "update")) {
			tolak(hasil, "Akun Anda tidak berhak mengelola Master Harga.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			if (SalesInventoryHargaTenant.aktif(ctx)) {
				// Lihat catatan pada supplierPriceSave: entitas terkunci ke schema koperasi.
				simpanHargaTenant(ctx, tbmuser, request, hasil, session, false, id);
				return;
			}
			HargaJualCustomer h;
			if (baru) {
				Long anggotaId = optLong(request, "anggota_id"); // null = harga umum
				Long produkId = optLong(request, "produk_id");
				java.util.Date tanggal = parseTanggal(request.optString("tanggal_efektif", "").trim());
				BigDecimal harga = request.isNull("harga") ? null
						: new BigDecimal((request.get("harga") + "").trim().replace(',', '.'));
				if (produkId == null || tanggal == null || harga == null) {
					tolak(hasil, "produk_id, harga, dan tanggal_efektif (yyyy-MM-dd) wajib diisi.");
					return;
				}
				Produk p = (Produk) session.get(Produk.class, produkId);
				if (p == null) {
					tolak(hasil, "Produk tidak ditemukan.");
					return;
				}
				AnggotaKoperasi a = null;
				if (anggotaId != null) {
					a = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, anggotaId);
					if (a == null) {
						tolak(hasil, "Customer tidak ditemukan.");
						return;
					}
				}
				java.sql.PreparedStatement cek = session.connection().prepareStatement(
						"SELECT COUNT(*) FROM koperasi.harga_jual_customer WHERE produk = ? AND tanggal_efektif = ? AND "
								+ (anggotaId == null ? "anggota_koperasi IS NULL" : "anggota_koperasi = ?"));
				cek.setLong(1, produkId.longValue());
				cek.setDate(2, new java.sql.Date(tanggal.getTime()));
				if (anggotaId != null) cek.setLong(3, anggotaId.longValue());
				java.sql.ResultSet rsCek = cek.executeQuery();
				boolean dobel = rsCek.next() && rsCek.getLong(1) > 0;
				rsCek.close(); cek.close();
				if (dobel) {
					tolak(hasil, "Sudah ada versi harga customer-produk ini pada tanggal efektif yang sama (overlap ditolak).");
					return;
				}
				h = new HargaJualCustomer();
				h.setAnggotaKoperasi(a);
				h.setProduk(p);
				h.setHarga(harga);
				h.setTanggalEfektif(tanggal);
			} else {
				h = (HargaJualCustomer) session.get(HargaJualCustomer.class, id);
				if (h == null) {
					tolak(hasil, "Versi harga tidak ditemukan.");
					return;
				}
				if (!request.isNull("harga") || !request.isNull("tanggal_efektif")
						|| !request.isNull("anggota_id") || !request.isNull("produk_id")) {
					tolak(hasil, "Versi tersimpan tidak boleh diubah harganya -- buat versi baru dengan tanggal efektif baru.");
					return;
				}
			}
			if (!request.isNull("keterangan")) h.setKeterangan(request.optString("keterangan", "").trim());
			if (!request.isNull("aktif")) h.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			isiOleh(h, tbmuser);
			tx = session.beginTransaction();
			session.saveOrUpdate(h);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", h.getId());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// ANALISIS HARGA (layar 11) -- beli vs jual vs margin per produk
	// =============================================================================================

	public static void priceAnalysis(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		String keyword = request == null || request.isNull("keyword") ? null : request.optString("keyword", "").trim();
		String filter = request == null ? "" : request.optString("filter", "").trim(); // stok_ada|stok_nol|margin_negatif
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = Math.min(100, Math.max(1, request == null ? 20 : request.optInt("page_size", 20)));
		Long tokoId = ctx.admin
				? ais.common.Common.angkaAtauNull(request, "toko_id")
				: ctx.tokoId;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE COALESCE(p.aktif,true) = true ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (keyword != null && !keyword.isEmpty()) {
				where.append(" AND (p.kode ILIKE ? OR p.nama ILIKE ?) ");
				String k = "%" + keyword + "%";
				params.add(k); params.add(k);
			}
			boolean jalurTenant = SalesInventoryHargaTenant.aktif(ctx);
			String skemaTenant = jalurTenant ? SalesInventoryHargaTenant.skema(ctx) : null;
			if (tokoId != null) {
				if (jalurTenant) {
					// Sama seperti layar Persediaan: lingkup toko = lingkup GUDANG.
					where.append(SalesInventoryHargaTenant.syaratTokoProduk(skemaTenant, tokoId));
				} else {
					where.append(" AND p.toko = ? ");
					params.add(tokoId);
				}
			}
			if (jalurTenant) {
				// Stok pada model tenant adalah turunan buku besar, bukan kolom.
				if ("stok_ada".equals(filter)) {
					// Stoknya ikut dibatasi gudang toko: menyaring "ada stok" atas angka
					// se-tenant akan meloloskan produk yang justru habis di toko ini.
					where.append(" AND ")
							.append(SalesInventoryHargaTenant.stokTurunan(skemaTenant, tokoId))
							.append(" > 0 ");
				} else if ("stok_nol".equals(filter)) {
					where.append(SalesInventoryHargaTenant.syaratStokNol(skemaTenant, tokoId));
				} else if ("margin_negatif".equals(filter)) {
					where.append(SalesInventoryHargaTenant.syaratMarginNegatif());
				}
			} else {
				if ("stok_ada".equals(filter)) where.append(" AND COALESCE(p.stok,0) > 0 ");
				else if ("stok_nol".equals(filter)) where.append(" AND COALESCE(p.stok,0) <= 0 ");
				else if ("margin_negatif".equals(filter))
					where.append(" AND COALESCE(p.hargajual,0) < COALESCE(p.hargabeli,0) ");
			}

			String dasar;
			String pilih;
			if (jalurTenant) {
				dasar = SalesInventoryHargaTenant.dasarAnalisa(skemaTenant, where.toString());
				pilih = SalesInventoryHargaTenant.selectAnalisa(skemaTenant);
			} else {
				// hargaUmumEfektif = versi aktif terbaru harga jual UMUM (anggota null) per produk.
				String hargaUmum = "(SELECT h.harga FROM koperasi.harga_jual_customer h WHERE h.produk = p.id "
						+ "AND h.anggota_koperasi IS NULL AND COALESCE(h.aktif,true) = true "
						+ "AND h.tanggal_efektif <= CURRENT_DATE ORDER BY h.tanggal_efektif DESC, h.id DESC LIMIT 1)";
				String hargaBeliSupplierTerbaru = "(SELECT h.harga FROM koperasi.harga_beli_supplier h WHERE h.produk = p.id "
						+ "AND COALESCE(h.aktif,true) = true AND h.tanggal_efektif <= CURRENT_DATE "
						+ "ORDER BY h.tanggal_efektif DESC, h.id DESC LIMIT 1)";
				dasar = " FROM koperasi.produk p LEFT JOIN koperasi.satuan_produk sp ON p.satuan = sp.id " + where;
				pilih = "SELECT p.id, p.kode, p.nama, COALESCE(NULLIF(TRIM(sp.nama),''),'(Belum diatur)'), COALESCE(p.stok,0), "
						+ "COALESCE(p.hargabeli,0), COALESCE(p.hargajual,0), " + hargaUmum + ", "
						+ hargaBeliSupplierTerbaru;
			}
			java.sql.PreparedStatement psTotal = session.connection().prepareStatement("SELECT COUNT(*) " + dasar);
			for (int i = 0; i < params.size(); i++) psTotal.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsTotal = psTotal.executeQuery();
			long total = rsTotal.next() ? rsTotal.getLong(1) : 0;
			rsTotal.close(); psTotal.close();

			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					pilih + dasar + " ORDER BY p.kode ASC LIMIT ? OFFSET ?");
			int idx = 1;
			for (int i = 0; i < params.size(); i++) ps.setObject(idx++, params.get(i));
			ps.setInt(idx++, size);
			ps.setInt(idx++, (page - 1) * size);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("produkId", rs.getLong(1));
				j.put("kode", str(rs.getString(2)));
				j.put("nama", str(rs.getString(3)));
				j.put("satuan", str(rs.getString(4)));
				j.put("stok", rs.getDouble(5));
				double beli = rs.getDouble(6);
				double jual = rs.getDouble(7);
				j.put("hargaBeli", beli);
				j.put("hargaJual", jual);
				double umum = rs.getDouble(8);
				j.put("hargaJualUmumEfektif", rs.wasNull() ? JSONObject.NULL : Double.valueOf(umum));
				double beliSupplier = rs.getDouble(9);
				j.put("hargaBeliSupplierEfektif", rs.wasNull() ? JSONObject.NULL : Double.valueOf(beliSupplier));
				j.put("marginPersen", beli <= 0 ? JSONObject.NULL
						: Double.valueOf((jual - beli) * 100.0 / beli));
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", size);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Simpan versi harga pada schema tenant, lewat SQL asli.
	 *
	 * <p>Entitas Hibernate mematok schemanya di anotasi ({@code @Table(schema = "koperasi")});
	 * 1.551 entitas di deployment ini melakukannya, dan pemetaan itu statis per SessionFactory.
	 * Akibatnya {@code session.saveOrUpdate()} selalu menulis ke schema bersama, berapa pun
	 * tenant yang sedang aktif -- yaitu kebocoran yang justru hendak dicegah. Karena itu jalur
	 * tenant tidak memakai entitas sama sekali.</p>
	 *
	 * <p>Aturan bisnisnya tidak dilonggarkan: tanggal efektif ganda tetap ditolak, dan versi
	 * yang sudah tersimpan tetap tidak boleh diubah harga/tanggal/pihak/produknya -- perubahan
	 * harga berarti versi baru. Yang berbeda hanya {@code keterangan}, yang memang tidak ada
	 * pada model tenant sehingga tidak dapat disimpan.</p>
	 *
	 * @param supplierMode {@code true} untuk harga beli supplier, {@code false} untuk harga
	 *        jual customer.
	 */
	private static void simpanHargaTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject request, JSONObject hasil, Session session,
			boolean supplierMode, Long id) throws Exception {
		String sk = SalesInventoryHargaTenant.skema(ctx);
		String oleh = tbmuser == null || tbmuser.getUserId() == null ? "" : tbmuser.getUserId();
		Transaction tx = null;
		try {
			if (id == null) {
				Long pihakId = optLong(request, supplierMode ? "supplier_id" : "anggota_id");
				Long produkId = optLong(request, "produk_id");
				java.util.Date tanggal = parseTanggal(request.optString("tanggal_efektif", "").trim());
				BigDecimal harga = request.isNull("harga") ? null
						: new BigDecimal((request.get("harga") + "").trim().replace(',', '.'));
				if (produkId == null || tanggal == null || harga == null
						|| (supplierMode && pihakId == null)) {
					tolak(hasil, supplierMode
							? "supplier_id, produk_id, harga, dan tanggal_efektif (yyyy-MM-dd) wajib diisi."
							: "produk_id, harga, dan tanggal_efektif (yyyy-MM-dd) wajib diisi.");
					return;
				}
				if (!supplierMode && pihakId == null
						&& !SalesInventoryHargaTenant.dukungBarisHargaUmum()) {
					// customer_id NOT NULL: baris harga umum mustahil disimpan di sini.
					// Menyisipkannya akan gagal di lapisan basis data dengan pesan yang tidak
					// berarti apa-apa bagi pengguna; ditolak lebih awal dengan sebab yang jelas.
					tolak(hasil, "Harga umum (tanpa anggota) belum dapat disimpan pada tenant "
							+ "berschema -- kolom customer_id belum boleh kosong. Isi anggota_id, "
							+ "atau atur harga jual standar pada master produk.");
					return;
				}
				// Pihak dan produk diperiksa DI DALAM schema tenant. Memakai session.get()
				// akan memeriksanya di schema bersama -- id yang sah di sana belum tentu
				// ada di sini, dan sebaliknya.
				if (!adaBaris(session, "SELECT COUNT(*) FROM " + sk + "produk WHERE id = ?", produkId)) {
					tolak(hasil, "Produk tidak ditemukan pada tenant ini.");
					return;
				}
				if (pihakId != null) {
					String tabelPihak = supplierMode ? "supplier" : "customer";
					if (!adaBaris(session, "SELECT COUNT(*) FROM " + sk + tabelPihak + " WHERE id = ?",
							pihakId)) {
						tolak(hasil, (supplierMode ? "Supplier" : "Customer")
								+ " tidak ditemukan pada tenant ini.");
						return;
					}
				}
				java.sql.PreparedStatement cek = session.connection().prepareStatement(supplierMode
						? SalesInventoryHargaTenant.cekDobelSupplier(sk)
						: SalesInventoryHargaTenant.cekDobelCustomer(sk, pihakId == null));
				int c = 1;
				if (supplierMode) {
					cek.setLong(c++, pihakId.longValue());
					cek.setLong(c++, produkId.longValue());
					cek.setDate(c++, new java.sql.Date(tanggal.getTime()));
				} else {
					cek.setLong(c++, produkId.longValue());
					cek.setDate(c++, new java.sql.Date(tanggal.getTime()));
					if (pihakId != null) {
						cek.setLong(c++, pihakId.longValue());
					}
				}
				java.sql.ResultSet rsCek = cek.executeQuery();
				boolean dobel = rsCek.next() && rsCek.getLong(1) > 0;
				rsCek.close();
				cek.close();
				if (dobel) {
					tolak(hasil, supplierMode
							? "Sudah ada versi harga supplier-produk ini pada tanggal efektif yang sama (overlap ditolak)."
							: "Sudah ada versi harga untuk produk/anggota ini pada tanggal efektif yang sama (overlap ditolak).");
					return;
				}
				tx = session.beginTransaction();
				java.sql.PreparedStatement ins = session.connection().prepareStatement(
						supplierMode ? SalesInventoryHargaTenant.sisipSupplier(sk)
								: SalesInventoryHargaTenant.sisipCustomer(sk),
						java.sql.Statement.RETURN_GENERATED_KEYS);
				if (pihakId == null) {
					ins.setNull(1, java.sql.Types.BIGINT);
				} else {
					ins.setLong(1, pihakId.longValue());
				}
				ins.setLong(2, produkId.longValue());
				ins.setBigDecimal(3, harga);
				ins.setDate(4, new java.sql.Date(tanggal.getTime()));
				ins.setString(5, oleh);
				ins.executeUpdate();
				Long baruId = null;
				java.sql.ResultSet gk = ins.getGeneratedKeys();
				if (gk.next()) {
					baruId = Long.valueOf(gk.getLong(1));
				}
				gk.close();
				ins.close();
				tx.commit();
				hasil.put("status", "00");
				if (baruId != null) {
					hasil.put("id", baruId);
				}
				return;
			}

			// -- Pembaruan: histori TIDAK ditimpa.
			if (!adaBaris(session, supplierMode ? SalesInventoryHargaTenant.adaSupplier(sk)
					: SalesInventoryHargaTenant.adaCustomer(sk), id)) {
				tolak(hasil, "Versi harga tidak ditemukan.");
				return;
			}
			if (!request.isNull("harga") || !request.isNull("tanggal_efektif")
					|| !request.isNull("supplier_id") || !request.isNull("anggota_id")
					|| !request.isNull("produk_id")) {
				tolak(hasil, "Versi tersimpan tidak boleh diubah harganya -- buat versi baru dengan tanggal efektif baru.");
				return;
			}
			if (request.isNull("aktif")) {
				// keterangan tidak ada pada model tenant, sehingga tanpa "aktif" tidak ada
				// satu pun medan yang dapat diperbarui. Katakan terus terang.
				tolak(hasil, "Tidak ada yang dapat diperbarui: pada tenant berschema hanya status aktif yang dapat diubah.");
				return;
			}
			tx = session.beginTransaction();
			java.sql.PreparedStatement upd = session.connection().prepareStatement(supplierMode
					? SalesInventoryHargaTenant.ubahAktifSupplier(sk)
					: SalesInventoryHargaTenant.ubahAktifCustomer(sk));
			upd.setBoolean(1, request.optBoolean("aktif", true));
			upd.setString(2, oleh);
			upd.setLong(3, id.longValue());
			upd.executeUpdate();
			upd.close();
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", id);
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
				// rollback gagal tidak boleh menutupi galat aslinya
			}
			throw e;
		}
	}

	/** Benar bila kueri COUNT berparameter satu id menghasilkan lebih dari nol. */
	private static boolean adaBaris(Session session, String sql, Long id) throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
		try {
			ps.setLong(1, id.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			boolean ada = rs.next() && rs.getLong(1) > 0;
			rs.close();
			return ada;
		} finally {
			ps.close();
		}
	}
}
