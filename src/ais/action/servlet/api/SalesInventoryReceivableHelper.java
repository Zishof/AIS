package ais.action.servlet.api;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.AlokasiPenerimaanPiutangCustomer;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CustomerInventoryProfile;
import ais.database.model.koperasi.PenerimaanPiutangCustomer;
import ais.database.model.koperasi.PiutangCustomerDoc;
import ais.database.model.koperasi.SalesInventory;
import ais.database.model.koperasi.SalesOrderLapangan;
import ais.database.model.koperasi.SalesOrderLapanganItem;

/**
 * <h3>Penjualan Sales + Piutang Customer (AR) -- layar legacy 30-38, varian Inventory &amp; Sales.</h3>
 *
 * <p>Cermin sisi jual dari {@link SalesInventoryPayableHelper} (AP, P3):</p>
 * <ul>
 *   <li>Order &ne; invoice (mapping layar 30): {@link SalesOrderLapangan} berstatus
 *       DRAFT&rarr;PESAN&rarr;SIAP_KIRIM&rarr;TERKIRIM&rarr;SIAP_TAGIH; piutang
 *       ({@link PiutangCustomerDoc}) baru lahir saat {@code si_sales_order_invoice}.</li>
 *   <li>Register event: outstanding faktur SELALU dihitung
 *       ({@code total_faktur - dibayar_awal - SUM(alokasi)}), tidak pernah disimpan;
 *       "lunas" murni turunan (filter visual layar 33), tidak ada delete.</li>
 *   <li>Collection ({@code si_collection_create}) idempoten {@code kode_unik} + FOR UPDATE per
 *       faktur + &Sigma;alokasi = nominal -- pola byte-per-byte sama dgn pembayaran AP.</li>
 *   <li>Scope aktor: SALES keliling hanya melihat/mengisi data ber-sales dirinya
 *       ({@code ctx.salesId}); Pemilik/Admin bebas dalam scope tokonya.</li>
 * </ul>
 *
 * <p>KEPUTUSAN D-13: transisi TERKIRIM TIDAK menggerakkan stok pada P4 -- movement fisik
 * dicatat lewat SPJ "barang dibawa" (P5) supaya tidak dobel-hitung.</p>
 */
public final class SalesInventoryReceivableHelper {

	private SalesInventoryReceivableHelper() {
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

	private static BigDecimal optBigDecimal(JSONObject r, String kunci) {
		if (r == null || r.isNull(kunci)) {
			return null;
		}
		try {
			String v = (r.get(kunci) + "").trim().replace(',', '.');
			return v.isEmpty() ? null : new BigDecimal(v);
		} catch (Exception e) {
			return null;
		}
	}

	private static Date optTanggal(JSONObject r, String kunci) {
		try {
			String s = r.optString(kunci, "").trim();
			if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
				return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s);
			}
		} catch (Exception ignore) {
		}
		return null;
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
			ais.common.ErrorAuditUtil.record(e, "SalesInventoryReceivableHelper.isiOleh");
		}
	}

	/** Sales keliling = scope data milik sendiri (mapping: "Terbatas pada data/sesi sendiri"). */
	private static boolean aktorSales(EbisnisActorContextResolver.ActorContext ctx) {
		return EbisnisActorContextResolver.ACTOR_SALES.equals(ctx.actorType);
	}

	private static String fmtNomor(String prefix, Long tokoId, Long id) {
		return prefix + "-" + (tokoId == null ? 0 : tokoId.longValue()) + "-"
				+ String.format("%06d", new Object[] { id });
	}

	private static final String EXPR_ALOKASI =
			"COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_penerimaan_piutang_customer a WHERE a.piutang_doc = d.id),0)";
	private static final String EXPR_OUTSTANDING =
			"(COALESCE(d.total_faktur,0) - COALESCE(d.dibayar_awal,0) - " + EXPR_ALOKASI + ")";

	// =============================================================================================
	// SCR-30: Sales Order lifecycle (si_sales_order_create/update/list/detail/status/invoice)
	// =============================================================================================

	public static void salesOrderSimpan(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil, boolean update) throws Exception {
		if (!ctx.bolehAksi("penjualan_sales", update ? "update" : "create")) {
			tolak(hasil, "Akun Anda tidak berhak " + (update ? "mengubah" : "membuat") + " sales order.");
			return;
		}
		Long customerId = optLong(request, "customer_id");
		JSONArray items = request.optJSONArray("items");
		if (!update && customerId == null) {
			tolak(hasil, "customer_id wajib diisi.");
			return;
		}
		if (items == null || items.length() == 0) {
			tolak(hasil, "Order minimal berisi satu item.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			SalesOrderLapangan so;
			if (update) {
				Long orderId = optLong(request, "order_id");
				if (orderId == null) {
					tolak(hasil, "order_id wajib diisi.");
					return;
				}
				so = (SalesOrderLapangan) session.get(SalesOrderLapangan.class, orderId);
				if (so == null) {
					tolak(hasil, "Sales order tidak ditemukan.");
					return;
				}
				String st = so.getStatus();
				if (!SalesOrderLapangan.STATUS_DRAFT.equals(st) && !SalesOrderLapangan.STATUS_PESAN.equals(st)) {
					tolak(hasil, "Order berstatus " + st + " tidak bisa diubah (hanya DRAFT/PESAN).");
					return;
				}
				if (aktorSales(ctx) && (so.getSales() == null || ctx.salesId == null
						|| !ctx.salesId.equals(so.getSales().getId()))) {
					tolak(hasil, "Order ini bukan milik sales Anda.");
					return;
				}
			} else {
				// Idempoten create: kode_unik sudah ada -> kembalikan order pertama.
				String kodeUnik = request.optString("kode_unik", "").trim();
				if (!kodeUnik.isEmpty()) {
					SalesOrderLapangan sudahAda = (SalesOrderLapangan) session
							.createCriteria(SalesOrderLapangan.class)
							.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
					if (sudahAda != null) {
						hasil.put("status", "00");
						hasil.put("id", sudahAda.getId());
						hasil.put("nomor", str(sudahAda.getNomor()));
						hasil.put("idempotentReplay", true);
						return;
					}
				}
				AnggotaKoperasi cust = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, customerId);
				if (cust == null) {
					tolak(hasil, "Customer tidak ditemukan.");
					return;
				}
				Long tokoId = ctx.tokoId;
				if (ctx.admin && optLong(request, "toko_id") != null) {
					tokoId = optLong(request, "toko_id");
				}
				if (tokoId == null) {
					tolak(hasil, "Scope toko tidak dapat ditentukan untuk akun ini.");
					return;
				}
				Toko toko = (Toko) session.get(Toko.class, tokoId);
				if (toko == null) {
					tolak(hasil, "Toko tidak ditemukan.");
					return;
				}
				so = new SalesOrderLapangan();
				so.setToko(toko);
				so.setCustomer(cust);
				/* Keputusan bisnis 2026-08-15: order baru langsung disetujui/PESAN.
				 * Draft tetap tersedia hanya jika klien memintanya secara eksplisit. */
				so.setStatus(request.optBoolean("simpan_draft", false)
						? SalesOrderLapangan.STATUS_DRAFT : SalesOrderLapangan.STATUS_PESAN);
				so.setKodeUnik(kodeUnik.isEmpty() ? null : kodeUnik);
				so.setDibuatOleh(tbmuser);
				// Sales: aktor sales = dirinya (paksa); Pemilik/Admin boleh memilih.
				Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
				if (aktorSales(ctx) && salesId == null) {
					tolak(hasil, "Akun sales Anda belum punya profil sales aktif.");
					return;
				}
				if (salesId != null) {
					SalesInventory s = (SalesInventory) session.get(SalesInventory.class, salesId);
					if (s == null) {
						tolak(hasil, "Profil sales tidak ditemukan.");
						return;
					}
					so.setSales(s);
				}
			}
			Date tanggal = optTanggal(request, "tanggal");
			if (tanggal != null) {
				so.setTanggal(tanggal);
			}
			so.setKeterangan(request.optString("keterangan", "").trim());
			isiOleh(so, tbmuser);

			tx = session.beginTransaction();
			if (!update) {
				session.save(so);
				session.flush();
				so.setNomor(fmtNomor("SO", so.getToko().getId(), so.getId()));
			} else {
				// Ganti item: hapus lewat entity (bukan raw SQL) supaya Envers tetap mencatat.
				List lama = session.createCriteria(SalesOrderLapanganItem.class)
						.add(Restrictions.eq("salesOrder", so)).list();
				for (int i = 0; i < lama.size(); i++) {
					session.delete(lama.get(i));
				}
			}
			BigDecimal total = BigDecimal.ZERO;
			for (int i = 0; i < items.length(); i++) {
				JSONObject it = items.getJSONObject(i);
				Long produkId = optLong(it, "produk_id");
				BigDecimal jumlah = optBigDecimal(it, "jumlah");
				BigDecimal harga = optBigDecimal(it, "harga");
				if (produkId == null || jumlah == null || jumlah.signum() <= 0 || harga == null
						|| harga.signum() < 0) {
					tx.rollback();
					tolak(hasil, "Item ke-" + (i + 1) + " tidak valid (produk_id/jumlah>0/harga>=0).");
					return;
				}
				Produk p = (Produk) session.get(Produk.class, produkId);
				if (p == null) {
					tx.rollback();
					tolak(hasil, "Produk " + produkId + " tidak ditemukan.");
					return;
				}
				SalesOrderLapanganItem baris = new SalesOrderLapanganItem();
				baris.setSalesOrder(so);
				baris.setProduk(p);
				baris.setNamaProduk(str(p.getNama()));
				baris.setHargaSatuan(harga);
				baris.setJumlah(jumlah);
				BigDecimal sub = harga.multiply(jumlah);
				baris.setSubtotal(sub);
				double hpp = p.getHargaBeli() == null ? 0 : p.getHargaBeli().doubleValue();
				baris.setHppSnapshot(new BigDecimal(String.valueOf(hpp)));
				session.save(baris);
				total = total.add(sub);
			}
			so.setTotal(total);
			session.saveOrUpdate(so);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", so.getId());
			hasil.put("nomor", str(so.getNomor()));
			hasil.put("total", total.doubleValue());
			hasil.put("orderStatus", so.getStatus());
		} catch (org.hibernate.exception.ConstraintViolationException dup) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			hasil.put("status", "00");
			hasil.put("idempotentReplay", true);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void salesOrderList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("penjualan_sales")) {
			tolak(hasil, "Menu Penjualan Sales tidak aktif untuk akun Anda.");
			return;
		}
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(1, request.optInt("page_size", 20)));
		String status = request.optString("status", "").trim().toUpperCase();
		Long customerId = optLong(request, "customer_id");
		Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
		String q = request.optString("q", "").trim().toLowerCase();
		if (aktorSales(ctx) && salesId == null) {
			tolak(hasil, "Akun sales Anda belum punya profil sales aktif.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1");
			if (!status.isEmpty()) where.append(" AND o.status = ?");
			if (customerId != null) where.append(" AND o.customer = ?");
			if (salesId != null) where.append(" AND o.sales = ?");
			if (ctx.tokoId != null && !ctx.admin) where.append(" AND o.toko = ?");
			if (!q.isEmpty()) where.append(" AND (LOWER(o.nomor) LIKE ? OR LOWER(c.nama) LIKE ?)");
			String base = " FROM koperasi.sales_order_lapangan o"
					+ " JOIN koperasi.anggota_koperasi c ON o.customer = c.id"
					+ " LEFT JOIN koperasi.sales_inventory s ON o.sales = s.id" + where;
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT o.id, o.nomor, o.tanggal, o.status, o.total, o.keterangan,"
							+ " c.id, c.nama, s.id, s.nama" + base
							+ " ORDER BY o.tanggal DESC, o.id DESC LIMIT " + pageSize
							+ " OFFSET " + ((page - 1) * pageSize));
			int ix = 1;
			if (!status.isEmpty()) ps.setString(ix++, status);
			if (customerId != null) ps.setLong(ix++, customerId.longValue());
			if (salesId != null) ps.setLong(ix++, salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) ps.setLong(ix++, ctx.tokoId.longValue());
			if (!q.isEmpty()) { ps.setString(ix++, "%" + q + "%"); ps.setString(ix++, "%" + q + "%"); }
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray rows = new JSONArray();
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("id", rs.getLong(1));
				r.put("nomor", str(rs.getString(2)));
				r.put("tanggal", str(rs.getTimestamp(3)));
				r.put("status", str(rs.getString(4)));
				r.put("total", rs.getDouble(5));
				r.put("keterangan", str(rs.getString(6)));
				r.put("customerId", rs.getLong(7));
				r.put("customerNama", str(rs.getString(8)));
				long sid = rs.getLong(9);
				r.put("salesId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(sid));
				r.put("salesNama", str(rs.getString(10)));
				rows.put(r);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("rows", rows);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void salesOrderDetail(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("penjualan_sales")) {
			tolak(hasil, "Menu Penjualan Sales tidak aktif untuk akun Anda.");
			return;
		}
		Long orderId = optLong(request, "order_id");
		if (orderId == null) {
			tolak(hasil, "order_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			SalesOrderLapangan so = (SalesOrderLapangan) session.get(SalesOrderLapangan.class, orderId);
			if (so == null) {
				tolak(hasil, "Sales order tidak ditemukan.");
				return;
			}
			if (aktorSales(ctx) && (so.getSales() == null || ctx.salesId == null
					|| !ctx.salesId.equals(so.getSales().getId()))) {
				tolak(hasil, "Order ini bukan milik sales Anda.");
				return;
			}
			JSONObject j = new JSONObject();
			j.put("id", so.getId());
			j.put("nomor", str(so.getNomor()));
			j.put("tanggal", str(so.getTanggal()));
			j.put("status", so.getStatus());
			j.put("total", so.getTotal().doubleValue());
			j.put("keterangan", str(so.getKeterangan()));
			j.put("alasanBatal", str(so.getAlasanBatal()));
			j.put("customerId", so.getCustomer().getId());
			j.put("customerNama", str(so.getCustomer().getNama()));
			j.put("salesId", so.getSales() == null ? JSONObject.NULL : so.getSales().getId());
			j.put("salesNama", so.getSales() == null ? "" : str(so.getSales().getNama()));
			List items = session.createCriteria(SalesOrderLapanganItem.class)
					.add(Restrictions.eq("salesOrder", so)).addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			for (int i = 0; i < items.size(); i++) {
				SalesOrderLapanganItem it = (SalesOrderLapanganItem) items.get(i);
				JSONObject r = new JSONObject();
				r.put("id", it.getId());
				r.put("produkId", it.getProduk().getId());
				r.put("namaProduk", str(it.getNamaProduk()));
				r.put("hargaSatuan", it.getHargaSatuan().doubleValue());
				r.put("jumlah", it.getJumlah().doubleValue());
				r.put("subtotal", it.getSubtotal().doubleValue());
				arr.put(r);
			}
			j.put("items", arr);
			// Deep-link SCR-31: faktur piutang hasil posting order ini (bila sudah ada).
			PiutangCustomerDoc doc = (PiutangCustomerDoc) session.createCriteria(PiutangCustomerDoc.class)
					.add(Restrictions.eq("salesOrder", so)).setMaxResults(1).uniqueResult();
			j.put("piutangDocId", doc == null ? JSONObject.NULL : doc.getId());
			j.put("piutangDocNomor", doc == null ? "" : str(doc.getNomor()));
			hasil.put("status", "00");
			hasil.put("data", j);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Transisi status manual (selain SIAP_TAGIH -- itu lewat {@link #salesOrderInvoice}). */
	public static void salesOrderStatus(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehAksi("penjualan_sales", "update")) {
			tolak(hasil, "Akun Anda tidak berhak mengubah status sales order.");
			return;
		}
		Long orderId = optLong(request, "order_id");
		String baru = request.optString("status", "").trim().toUpperCase();
		String alasan = request.optString("alasan", "").trim();
		if (orderId == null || baru.isEmpty()) {
			tolak(hasil, "order_id dan status wajib diisi.");
			return;
		}
		if (SalesOrderLapangan.STATUS_SIAP_TAGIH.equals(baru)) {
			tolak(hasil, "SIAP_TAGIH lewat aksi si_sales_order_invoice (menerbitkan faktur piutang).");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			SalesOrderLapangan so = (SalesOrderLapangan) session.get(SalesOrderLapangan.class, orderId);
			if (so == null) {
				tolak(hasil, "Sales order tidak ditemukan.");
				return;
			}
			if (aktorSales(ctx) && (so.getSales() == null || ctx.salesId == null
					|| !ctx.salesId.equals(so.getSales().getId()))) {
				tolak(hasil, "Order ini bukan milik sales Anda.");
				return;
			}
			String lama = so.getStatus();
			boolean boleh;
			if (SalesOrderLapangan.STATUS_BATAL.equals(baru)) {
				// Soft-cancel hanya sebelum barang keluar; sesudah TERKIRIM koreksi = retur (P5).
				boleh = SalesOrderLapangan.STATUS_DRAFT.equals(lama)
						|| SalesOrderLapangan.STATUS_PESAN.equals(lama)
						|| SalesOrderLapangan.STATUS_SIAP_KIRIM.equals(lama);
				if (boleh && alasan.isEmpty()) {
					tolak(hasil, "Pembatalan wajib menyertakan alasan.");
					return;
				}
			} else if (SalesOrderLapangan.STATUS_PESAN.equals(baru)) {
				boleh = SalesOrderLapangan.STATUS_DRAFT.equals(lama);
			} else if (SalesOrderLapangan.STATUS_SIAP_KIRIM.equals(baru)) {
				boleh = SalesOrderLapangan.STATUS_PESAN.equals(lama);
			} else if (SalesOrderLapangan.STATUS_TERKIRIM.equals(baru)) {
				boleh = SalesOrderLapangan.STATUS_SIAP_KIRIM.equals(lama);
			} else {
				boleh = false;
			}
			if (!boleh) {
				tolak(hasil, "Transisi " + lama + " -> " + baru + " tidak diizinkan.");
				return;
			}
			tx = session.beginTransaction();
			so.setStatus(baru);
			if (SalesOrderLapangan.STATUS_BATAL.equals(baru)) {
				so.setAlasanBatal(alasan);
			}
			isiOleh(so, tbmuser);
			session.saveOrUpdate(so);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", so.getId());
			hasil.put("statusBaru", baru);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** TERKIRIM -> SIAP_TAGIH + terbitkan {@link PiutangCustomerDoc} (idempoten per order). */
	public static void salesOrderInvoice(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehAksi("penjualan_sales", "update") && !ctx.bolehAksi("piutang", "create")) {
			tolak(hasil, "Akun Anda tidak berhak menerbitkan faktur piutang.");
			return;
		}
		Long orderId = optLong(request, "order_id");
		if (orderId == null) {
			tolak(hasil, "order_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			SalesOrderLapangan so = (SalesOrderLapangan) session.get(SalesOrderLapangan.class, orderId);
			if (so == null) {
				tolak(hasil, "Sales order tidak ditemukan.");
				return;
			}
			if (aktorSales(ctx) && (so.getSales() == null || ctx.salesId == null
					|| !ctx.salesId.equals(so.getSales().getId()))) {
				tolak(hasil, "Order ini bukan milik sales Anda.");
				return;
			}
			// Idempoten: order sudah pernah difakturkan -> replay.
			PiutangCustomerDoc sudah = (PiutangCustomerDoc) session.createCriteria(PiutangCustomerDoc.class)
					.add(Restrictions.eq("salesOrder", so)).setMaxResults(1).uniqueResult();
			if (sudah != null) {
				hasil.put("status", "00");
				hasil.put("piutangDocId", sudah.getId());
				hasil.put("nomor", str(sudah.getNomor()));
				hasil.put("idempotentReplay", true);
				return;
			}
			if (!SalesOrderLapangan.STATUS_TERKIRIM.equals(so.getStatus())) {
				tolak(hasil, "Hanya order TERKIRIM yang bisa difakturkan (status sekarang: "
						+ so.getStatus() + ").");
				return;
			}
			if (so.getTotal().signum() <= 0) {
				tolak(hasil, "Total order 0 -- tidak ada yang difakturkan.");
				return;
			}
			BigDecimal dibayarAwal = optBigDecimal(request, "dibayar_awal");
			if (dibayarAwal != null && dibayarAwal.doubleValue() > so.getTotal().doubleValue() + 0.01) {
				tolak(hasil, "dibayar_awal melebihi total order.");
				return;
			}
			// Termin dari profil customer (0 bila tanpa profil) -- boleh dioverride request.
			int termin = 0;
			CustomerInventoryProfile prof = (CustomerInventoryProfile) session
					.createCriteria(CustomerInventoryProfile.class)
					.add(Restrictions.eq("anggotaKoperasi", so.getCustomer()))
					.add(Restrictions.eq("aktif", Boolean.TRUE)).setMaxResults(1).uniqueResult();
			if (prof != null) {
				termin = prof.getTerminHari().intValue();
			}
			if (!request.isNull("termin_hari")) {
				termin = Math.max(0, request.optInt("termin_hari", termin));
			}
			tx = session.beginTransaction();
			PiutangCustomerDoc doc = new PiutangCustomerDoc();
			doc.setToko(so.getToko());
			doc.setCustomer(so.getCustomer());
			doc.setSales(so.getSales());
			doc.setSalesOrder(so);
			doc.setTanggal(ais.ui.util.WaktuUtil.getDate());
			doc.setTerminHari(Integer.valueOf(termin));
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.setTime(doc.getTanggal());
			cal.add(java.util.Calendar.DAY_OF_MONTH, termin);
			doc.setJatuhTempo(cal.getTime());
			doc.setTotalFaktur(so.getTotal());
			doc.setDibayarAwal(dibayarAwal);
			doc.setStatus(PiutangCustomerDoc.STATUS_AKTIF);
			doc.setKeterangan("Faktur dari sales order " + str(so.getNomor()));
			doc.setKodeUnik("SO-INV-" + so.getId());
			isiOleh(doc, tbmuser);
			session.save(doc);
			session.flush();
			doc.setNomor(fmtNomor("INV", so.getToko().getId(), doc.getId()));
			so.setStatus(SalesOrderLapangan.STATUS_SIAP_TAGIH);
			isiOleh(so, tbmuser);
			session.saveOrUpdate(so);
			session.saveOrUpdate(doc);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("piutangDocId", doc.getId());
			hasil.put("nomor", str(doc.getNomor()));
			hasil.put("jatuhTempo", str(doc.getJatuhTempo()));
		} catch (org.hibernate.exception.ConstraintViolationException dup) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			hasil.put("status", "00");
			hasil.put("idempotentReplay", true);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-31/32/33: ledger piutang (si_receivable_list / si_receivable_from_sale)
	// =============================================================================================

	public static void receivableList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("piutang")) {
			tolak(hasil, "Menu Piutang tidak aktif untuk akun Anda.");
			return;
		}
		boolean tampilkanLunas = request.optBoolean("tampilkan_lunas", false);
		Long customerId = optLong(request, "customer_id");
		Long orderId = optLong(request, "order_id");
		Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
		String q = request.optString("q", "").trim().toLowerCase();
		int page = Math.max(1, request.optInt("page", 1));
		int pageSize = Math.min(200, Math.max(1, request.optInt("page_size", 50)));
		if (aktorSales(ctx) && salesId == null) {
			tolak(hasil, "Akun sales Anda belum punya profil sales aktif.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE d.status = 'AKTIF'");
			if (!tampilkanLunas) where.append(" AND ").append(EXPR_OUTSTANDING).append(" > 0.009");
			if (customerId != null) where.append(" AND d.customer = ?");
			if (orderId != null) where.append(" AND d.sales_order = ?");
			if (salesId != null) where.append(" AND d.sales = ?");
			if (ctx.tokoId != null && !ctx.admin) where.append(" AND d.toko = ?");
			if (!q.isEmpty()) where.append(" AND (LOWER(d.nomor) LIKE ? OR LOWER(c.nama) LIKE ?)");
			String base = " FROM koperasi.piutang_customer_doc d"
					+ " JOIN koperasi.anggota_koperasi c ON d.customer = c.id"
					+ " LEFT JOIN koperasi.sales_inventory s ON d.sales = s.id"
					+ " LEFT JOIN koperasi.sales_order_lapangan o ON d.sales_order = o.id" + where;
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT d.id, d.nomor, d.tanggal, d.jatuh_tempo, d.total_faktur, d.dibayar_awal, "
							+ EXPR_ALOKASI + " AS teralokasi, " + EXPR_OUTSTANDING + " AS outstanding,"
							+ " c.id, c.nama, s.id, s.nama, o.id, o.nomor, d.keterangan" + base
							+ " ORDER BY d.jatuh_tempo NULLS LAST, d.id LIMIT " + pageSize
							+ " OFFSET " + ((page - 1) * pageSize));
			int ix = 1;
			if (customerId != null) ps.setLong(ix++, customerId.longValue());
			if (orderId != null) ps.setLong(ix++, orderId.longValue());
			if (salesId != null) ps.setLong(ix++, salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) ps.setLong(ix++, ctx.tokoId.longValue());
			if (!q.isEmpty()) { ps.setString(ix++, "%" + q + "%"); ps.setString(ix++, "%" + q + "%"); }
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray rows = new JSONArray();
			double totalOutstanding = 0;
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("id", rs.getLong(1));
				r.put("nomor", str(rs.getString(2)));
				r.put("tanggal", str(rs.getTimestamp(3)));
				r.put("jatuhTempo", str(rs.getDate(4)));
				r.put("totalFaktur", rs.getDouble(5));
				r.put("dibayarAwal", rs.getDouble(6));
				r.put("teralokasi", rs.getDouble(7));
				double out = rs.getDouble(8);
				r.put("outstanding", out);
				r.put("lunas", out <= 0.009);
				r.put("customerId", rs.getLong(9));
				r.put("customerNama", str(rs.getString(10)));
				long sid = rs.getLong(11);
				r.put("salesId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(sid));
				r.put("salesNama", str(rs.getString(12)));
				long oid = rs.getLong(13);
				r.put("orderId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(oid));
				r.put("orderNomor", str(rs.getString(14)));
				r.put("keterangan", str(rs.getString(15)));
				rows.put(r);
				totalOutstanding += out > 0 ? out : 0;
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("rows", rows);
			hasil.put("totalOutstanding", totalOutstanding);
			hasil.put("page", page);
			hasil.put("pageSize", pageSize);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-34: penerimaan piutang (si_collection_create) -- cermin payablePaymentCreate P3
	// =============================================================================================

	public static void collectionCreate(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehAksi("piutang", "create")) {
			tolak(hasil, "Akun Anda tidak berhak mencatat penerimaan piutang.");
			return;
		}
		Long customerId = optLong(request, "customer_id");
		BigDecimal nominal = optBigDecimal(request, "nominal");
		String metode = request.optString("metode", "").trim().toUpperCase();
		String kodeUnik = request.optString("kode_unik", "").trim();
		JSONArray alokasi = request.optJSONArray("alokasi");
		if (customerId == null || nominal == null || nominal.signum() <= 0) {
			tolak(hasil, "customer_id dan nominal (>0) wajib diisi.");
			return;
		}
		if (kodeUnik.isEmpty()) {
			tolak(hasil, "kode_unik (kunci idempoten) wajib diisi.");
			return;
		}
		if (alokasi == null || alokasi.length() == 0) {
			tolak(hasil, "Alokasi ke faktur piutang wajib diisi (minimal satu).");
			return;
		}
		if (metode.isEmpty()) metode = PenerimaanPiutangCustomer.METODE_TUNAI;
		if (PenerimaanPiutangCustomer.METODE_GIRO.equals(metode)
				&& request.optString("no_bg", "").trim().isEmpty()) {
			tolak(hasil, "Metode GIRO wajib menyertakan no_bg.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			// Idempoten: kode_unik sudah ada -> kembalikan penerimaan pertama, JANGAN menggandakan.
			PenerimaanPiutangCustomer sudahAda = (PenerimaanPiutangCustomer) session
					.createCriteria(PenerimaanPiutangCustomer.class)
					.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
			if (sudahAda != null) {
				hasil.put("status", "00");
				hasil.put("id", sudahAda.getId());
				hasil.put("nomor", str(sudahAda.getNomor()));
				hasil.put("idempotentReplay", true);
				return;
			}
			AnggotaKoperasi customer = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, customerId);
			if (customer == null) {
				tolak(hasil, "Customer tidak ditemukan.");
				return;
			}
			double jumlahAlokasi = 0;
			for (int i = 0; i < alokasi.length(); i++) {
				jumlahAlokasi += alokasi.getJSONObject(i).optDouble("nominal", 0);
			}
			if (Math.abs(jumlahAlokasi - nominal.doubleValue()) > 0.01) {
				tolak(hasil, "Total alokasi (" + jumlahAlokasi + ") harus sama dengan nominal penerimaan ("
						+ nominal + ").");
				return;
			}

			tx = session.beginTransaction();
			// FOR UPDATE per faktur -- dua penerimaan bersamaan tidak boleh sama-sama lolos.
			for (int i = 0; i < alokasi.length(); i++) {
				JSONObject a = alokasi.getJSONObject(i);
				long did = a.optLong("piutang_id", -1);
				double n = a.optDouble("nominal", 0);
				if (did <= 0 || n <= 0) {
					tx.rollback();
					tolak(hasil, "Baris alokasi tidak valid (piutang_id/nominal).");
					return;
				}
				java.sql.PreparedStatement lock = session.connection().prepareStatement(
						"SELECT d.id FROM koperasi.piutang_customer_doc d"
								+ " WHERE d.id = ? AND d.customer = ? AND d.status = 'AKTIF' FOR UPDATE");
				lock.setLong(1, did);
				lock.setLong(2, customerId.longValue());
				java.sql.ResultSet rsLock = lock.executeQuery();
				boolean ada = rsLock.next();
				rsLock.close(); lock.close();
				if (!ada) {
					tx.rollback();
					tolak(hasil, "Faktur piutang " + did + " tidak ditemukan / bukan milik customer ini.");
					return;
				}
				java.sql.PreparedStatement cek = session.connection().prepareStatement(
						"SELECT " + EXPR_OUTSTANDING + " FROM koperasi.piutang_customer_doc d WHERE d.id = ?");
				cek.setLong(1, did);
				java.sql.ResultSet rsCek = cek.executeQuery();
				double outstanding = rsCek.next() ? rsCek.getDouble(1) : -1;
				rsCek.close(); cek.close();
				if (n > outstanding + 0.01) {
					tx.rollback();
					tolak(hasil, "Alokasi " + n + " melebihi outstanding faktur " + did + " ("
							+ outstanding + ").");
					return;
				}
			}

			PenerimaanPiutangCustomer terima = new PenerimaanPiutangCustomer();
			terima.setCustomer(customer);
			terima.setNominal(nominal);
			terima.setMetode(metode);
			terima.setNoBg(request.optString("no_bg", "").trim());
			terima.setNamaBank(request.optString("nama_bank", "").trim());
			terima.setTanggalBg(optTanggal(request, "tanggal_bg"));
			terima.setKeterangan(request.optString("keterangan", "").trim());
			terima.setKodeUnik(kodeUnik);
			terima.setDibuatOleh(tbmuser);
			Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
			if (salesId != null) {
				terima.setSales((SalesInventory) session.get(SalesInventory.class, salesId));
			}
			// Penerimaan dalam sesi Nota Sales (P5): tandai sesi + kas COLLECTION_CASH bila tunai
			// + akumulasi nilai tertagih nota dibawa.
			ais.database.model.koperasi.NotaSalesSession sesiTrip = null;
			Long tripSessionId = optLong(request, "trip_session_id");
			if (tripSessionId != null) {
				sesiTrip = (ais.database.model.koperasi.NotaSalesSession) session
						.get(ais.database.model.koperasi.NotaSalesSession.class, tripSessionId);
				if (sesiTrip == null
						|| ais.database.model.koperasi.NotaSalesSession.STATUS_CLOSED.equals(sesiTrip.getStatus())) {
					tx.rollback();
					tolak(hasil, "Sesi Nota Sales tidak ditemukan / sudah ditutup.");
					return;
				}
				terima.setSesi(sesiTrip);
			}
			isiOleh(terima, tbmuser);
			session.save(terima);
			session.flush();
			terima.setNomor(fmtNomor("KWT", ctx.tokoId, terima.getId()));
			session.saveOrUpdate(terima);
			for (int i = 0; i < alokasi.length(); i++) {
				JSONObject a = alokasi.getJSONObject(i);
				AlokasiPenerimaanPiutangCustomer al = new AlokasiPenerimaanPiutangCustomer();
				al.setPenerimaan(terima);
				al.setPiutangDoc((PiutangCustomerDoc) session.get(PiutangCustomerDoc.class,
						Long.valueOf(a.optLong("piutang_id"))));
				al.setNominal(new BigDecimal(String.valueOf(a.optDouble("nominal"))));
				session.save(al);
			}
			if (sesiTrip != null) {
				if (PenerimaanPiutangCustomer.METODE_TUNAI.equals(metode)) {
					ais.database.model.koperasi.NotaSalesKas kas =
							new ais.database.model.koperasi.NotaSalesKas();
					kas.setSesi(sesiTrip);
					kas.setJenis(ais.database.model.koperasi.NotaSalesKas.JENIS_COLLECTION_CASH);
					kas.setNominal(nominal);
					kas.setReferensi("KWT-" + terima.getId());
					kas.setKeterangan("Penagihan tunai " + str(customer.getNama()));
					session.save(kas);
				}
				// Akumulasi nilaiTertagih + status nota dibawa milik SPJ sesi ini.
				for (int i = 0; i < alokasi.length(); i++) {
					long did = alokasi.getJSONObject(i).optLong("piutang_id");
					double n = alokasi.getJSONObject(i).optDouble("nominal", 0);
					ais.database.model.koperasi.SpjSalesNota notaBawa =
							(ais.database.model.koperasi.SpjSalesNota) session
									.createCriteria(ais.database.model.koperasi.SpjSalesNota.class)
									.add(Restrictions.eq("spj", sesiTrip.getSpj()))
									.add(Restrictions.eq("piutangDoc",
											session.get(PiutangCustomerDoc.class, Long.valueOf(did))))
									.setMaxResults(1).uniqueResult();
					if (notaBawa != null) {
						notaBawa.setNilaiTertagih(notaBawa.getNilaiTertagih()
								.add(new BigDecimal(String.valueOf(n))));
						java.sql.PreparedStatement cekSisa = session.connection().prepareStatement(
								"SELECT " + EXPR_OUTSTANDING
										+ " FROM koperasi.piutang_customer_doc d WHERE d.id = ?");
						cekSisa.setLong(1, did);
						java.sql.ResultSet rsSisa = cekSisa.executeQuery();
						double sisa = rsSisa.next() ? rsSisa.getDouble(1) : 1;
						rsSisa.close(); cekSisa.close();
						notaBawa.setStatus(sisa <= 0.009
								? ais.database.model.koperasi.SpjSalesNota.STATUS_PAID
								: ais.database.model.koperasi.SpjSalesNota.STATUS_PARTIAL);
						session.saveOrUpdate(notaBawa);
					}
				}
			}
			// Derivasi status order LUNAS bila faktur asalnya kini nol outstanding.
			for (int i = 0; i < alokasi.length(); i++) {
				long did = alokasi.getJSONObject(i).optLong("piutang_id");
				PiutangCustomerDoc doc = (PiutangCustomerDoc) session.get(PiutangCustomerDoc.class,
						Long.valueOf(did));
				if (doc == null || doc.getSalesOrder() == null) {
					continue;
				}
				java.sql.PreparedStatement cek = session.connection().prepareStatement(
						"SELECT " + EXPR_OUTSTANDING + " FROM koperasi.piutang_customer_doc d WHERE d.id = ?");
				cek.setLong(1, did);
				java.sql.ResultSet rsCek = cek.executeQuery();
				double sisa = rsCek.next() ? rsCek.getDouble(1) : 1;
				rsCek.close(); cek.close();
				if (sisa <= 0.009) {
					SalesOrderLapangan so = doc.getSalesOrder();
					so.setStatus(SalesOrderLapangan.STATUS_LUNAS);
					session.saveOrUpdate(so);
				}
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", terima.getId());
			hasil.put("nomor", str(terima.getNomor()));
		} catch (org.hibernate.exception.ConstraintViolationException dup) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			hasil.put("status", "00");
			hasil.put("idempotentReplay", true);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-35/36: riwayat + kwitansi (si_collection_history / si_collection_receipt)
	// =============================================================================================

	public static void collectionHistory(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("piutang")) {
			tolak(hasil, "Menu Piutang tidak aktif untuk akun Anda.");
			return;
		}
		Long customerId = optLong(request, "customer_id");
		Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
		Date dari = optTanggal(request, "dari");
		Date sampai = optTanggal(request, "sampai");
		if (aktorSales(ctx) && salesId == null) {
			tolak(hasil, "Akun sales Anda belum punya profil sales aktif.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1");
			if (customerId != null) where.append(" AND p.customer = ?");
			if (salesId != null) where.append(" AND p.sales = ?");
			if (dari != null) where.append(" AND p.tanggal >= ?");
			if (sampai != null) where.append(" AND p.tanggal < (?::date + 1)");
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT p.id, p.nomor, p.tanggal, p.nominal, p.metode, p.no_bg, p.nama_bank,"
							+ " p.keterangan, c.id, c.nama, s.nama,"
							+ " (SELECT COALESCE(string_agg(d.nomor, ', '), '') FROM"
							+ "   koperasi.alokasi_penerimaan_piutang_customer a"
							+ "   JOIN koperasi.piutang_customer_doc d ON a.piutang_doc = d.id"
							+ "   WHERE a.penerimaan = p.id) AS faktur,"
							+ " COALESCE(p.status_dok,'AKTIF'), p.status_bg"
							+ " FROM koperasi.penerimaan_piutang_customer p"
							+ " JOIN koperasi.anggota_koperasi c ON p.customer = c.id"
							+ " LEFT JOIN koperasi.sales_inventory s ON p.sales = s.id" + where
							+ " ORDER BY p.tanggal DESC, p.id DESC LIMIT 500");
			int ix = 1;
			if (customerId != null) ps.setLong(ix++, customerId.longValue());
			if (salesId != null) ps.setLong(ix++, salesId.longValue());
			if (dari != null) ps.setTimestamp(ix++, new java.sql.Timestamp(dari.getTime()));
			if (sampai != null) ps.setDate(ix++, new java.sql.Date(sampai.getTime()));
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray rows = new JSONArray();
			double total = 0;
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("id", rs.getLong(1));
				r.put("nomor", str(rs.getString(2)));
				r.put("tanggal", str(rs.getTimestamp(3)));
				r.put("nominal", rs.getDouble(4));
				r.put("metode", str(rs.getString(5)));
				r.put("noBg", str(rs.getString(6)));
				r.put("namaBank", str(rs.getString(7)));
				r.put("keterangan", str(rs.getString(8)));
				r.put("customerId", rs.getLong(9));
				r.put("customerNama", str(rs.getString(10)));
				r.put("salesNama", str(rs.getString(11)));
				r.put("faktur", str(rs.getString(12)));
				r.put("statusDok", str(rs.getString(13)));
				r.put("statusBg", rs.getString(14) == null ? "" : str(rs.getString(14)));
				rows.put(r);
				total += rs.getDouble(4);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("rows", rows);
			hasil.put("totalNominal", total);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void collectionReceipt(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("piutang")) {
			tolak(hasil, "Menu Piutang tidak aktif untuk akun Anda.");
			return;
		}
		Long id = optLong(request, "penerimaan_id");
		if (id == null) {
			tolak(hasil, "penerimaan_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenerimaanPiutangCustomer p = (PenerimaanPiutangCustomer) session
					.get(PenerimaanPiutangCustomer.class, id);
			if (p == null) {
				tolak(hasil, "Penerimaan tidak ditemukan.");
				return;
			}
			if (aktorSales(ctx) && (p.getSales() == null || ctx.salesId == null
					|| !ctx.salesId.equals(p.getSales().getId()))) {
				tolak(hasil, "Kwitansi ini bukan milik sales Anda.");
				return;
			}
			JSONObject j = new JSONObject();
			j.put("id", p.getId());
			j.put("nomor", str(p.getNomor()));
			j.put("tanggal", str(p.getTanggal()));
			j.put("nominal", p.getNominal().doubleValue());
			j.put("metode", p.getMetode());
			j.put("noBg", str(p.getNoBg()));
			j.put("namaBank", str(p.getNamaBank()));
			j.put("keterangan", str(p.getKeterangan()));
			j.put("customerNama", str(p.getCustomer().getNama()));
			j.put("customerKode", str(p.getCustomer().getKode()));
			j.put("salesNama", p.getSales() == null ? "" : str(p.getSales().getNama()));
			j.put("dibuatOleh", p.getDibuatOleh() == null ? "" : str(p.getDibuatOleh().getUserId()));
			List aloks = session.createCriteria(AlokasiPenerimaanPiutangCustomer.class)
					.add(Restrictions.eq("penerimaan", p)).addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			for (int i = 0; i < aloks.size(); i++) {
				AlokasiPenerimaanPiutangCustomer a = (AlokasiPenerimaanPiutangCustomer) aloks.get(i);
				JSONObject r = new JSONObject();
				r.put("fakturNomor", str(a.getPiutangDoc().getNomor()));
				r.put("fakturTanggal", str(a.getPiutangDoc().getTanggal()));
				r.put("totalFaktur", a.getPiutangDoc().getTotalFaktur().doubleValue());
				r.put("nominal", a.getNominal().doubleValue());
				arr.put(r);
			}
			j.put("alokasi", arr);
			hasil.put("status", "00");
			hasil.put("data", j);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-41: Laporan Piutang jenis "Rekap Penjualan Barang" (fungsi legacy layar 41)
	// =============================================================================================

	/** Rekap penjualan PER BARANG (legacy layar 41: #Brg, Nama, Satuan, QTY, Jumlah Rp) dari
	 *  baris order terfakturkan. Urut: nama | qty (fast moving). Tab Outstanding & Register
	 *  Event memakai {@code si_receivable_list} / {@code si_collection_history} existing. */
	public static void receivableReport(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("piutang")) {
			tolak(hasil, "Menu Piutang tidak aktif untuk akun Anda.");
			return;
		}
		String dariRaw = request.optString("dari", "").trim();
		String sampaiRaw = request.optString("sampai", "").trim();
		String dari = dariRaw.matches("\\d{4}-\\d{2}-\\d{2}") ? ("DATE '" + dariRaw + "'")
				: "(CURRENT_DATE - 30)";
		String sampai = sampaiRaw.matches("\\d{4}-\\d{2}-\\d{2}") ? ("DATE '" + sampaiRaw + "'")
				: "CURRENT_DATE";
		String urut = "qty".equalsIgnoreCase(request.optString("urut", "nama"))
				? "3 DESC" : "i.nama_produk ASC";
		String q = request.optString("q", "").trim().toLowerCase();
		Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE o.status IN ('SIAP_TAGIH','LUNAS')"
					+ " AND o.tanggal >= " + dari + " AND o.tanggal < (" + sampai + " + 1)");
			if (!q.isEmpty()) where.append(" AND LOWER(i.nama_produk) LIKE ?");
			if (salesId != null) where.append(" AND o.sales = ?");
			if (ctx.tokoId != null && !ctx.admin) where.append(" AND o.toko = ?");
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT i.produk, i.nama_produk, SUM(i.jumlah), SUM(i.subtotal)"
							+ " FROM koperasi.sales_order_lapangan_item i"
							+ " JOIN koperasi.sales_order_lapangan o ON i.sales_order = o.id" + where
							+ " GROUP BY i.produk, i.nama_produk ORDER BY " + urut + " LIMIT 500");
			int ix = 1;
			if (!q.isEmpty()) ps.setString(ix++, "%" + q + "%");
			if (salesId != null) ps.setLong(ix++, salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) ps.setLong(ix++, ctx.tokoId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray rows = new JSONArray();
			double totalQty = 0, totalRp = 0;
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("produkId", rs.getLong(1));
				r.put("namaProduk", str(rs.getString(2)));
				r.put("qty", rs.getDouble(3));
				r.put("jumlahRp", rs.getDouble(4));
				rows.put(r);
				totalQty += rs.getDouble(3);
				totalRp += rs.getDouble(4);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("rows", rows);
			hasil.put("totalQty", totalQty);
			hasil.put("totalRp", totalRp);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-37/38: aging per customer / per sales
	// =============================================================================================

	private static String exprBucket(String asOf) {
		return "CASE WHEN d.jatuh_tempo IS NULL OR d.jatuh_tempo >= " + asOf + " THEN 'BELUM'"
				+ " WHEN " + asOf + " - d.jatuh_tempo <= 30 THEN 'H1_30'"
				+ " WHEN " + asOf + " - d.jatuh_tempo <= 60 THEN 'H31_60'"
				+ " WHEN " + asOf + " - d.jatuh_tempo <= 90 THEN 'H61_90'"
				+ " ELSE 'LEBIH_90' END";
	}

	public static void receivableAgingCustomer(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("piutang")) {
			tolak(hasil, "Menu Piutang tidak aktif untuk akun Anda.");
			return;
		}
		// as_of sebagai LITERAL tervalidasi regex ketat (bukan placeholder): ekspresi bucket
		// memakainya 4x -- placeholder ganda rawan salah hitung binding. Aman injeksi karena
		// hanya lolos bila persis yyyy-MM-dd.
		String asOfRaw = request.optString("as_of", "").trim();
		String asOfSql = asOfRaw.matches("\\d{4}-\\d{2}-\\d{2}") ? ("DATE '" + asOfRaw + "'")
				: "CURRENT_DATE";
		Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE d.status = 'AKTIF' AND "
					+ EXPR_OUTSTANDING + " > 0.009");
			if (salesId != null) where.append(" AND d.sales = ?");
			if (ctx.tokoId != null && !ctx.admin) where.append(" AND d.toko = ?");
			String bucket = exprBucket(asOfSql);
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT c.id, c.nama, " + bucket + " AS bucket, SUM(" + EXPR_OUTSTANDING + ")"
							+ " FROM koperasi.piutang_customer_doc d"
							+ " JOIN koperasi.anggota_koperasi c ON d.customer = c.id" + where
							+ " GROUP BY c.id, c.nama, bucket ORDER BY c.nama");
			int ix = 1;
			if (salesId != null) ps.setLong(ix++, salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) ps.setLong(ix++, ctx.tokoId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			java.util.LinkedHashMap perCustomer = new java.util.LinkedHashMap();
			double[] totalBucket = new double[5];
			String[] kunci = { "BELUM", "H1_30", "H31_60", "H61_90", "LEBIH_90" };
			while (rs.next()) {
				Long cid = Long.valueOf(rs.getLong(1));
				JSONObject r = (JSONObject) perCustomer.get(cid);
				if (r == null) {
					r = new JSONObject();
					r.put("customerId", cid);
					r.put("customerNama", str(rs.getString(2)));
					for (int b = 0; b < kunci.length; b++) {
						r.put(kunci[b], 0);
					}
					r.put("total", 0);
					perCustomer.put(cid, r);
				}
				String b = str(rs.getString(3));
				double v = rs.getDouble(4);
				r.put(b, r.optDouble(b, 0) + v);
				r.put("total", r.optDouble("total", 0) + v);
				for (int k = 0; k < kunci.length; k++) {
					if (kunci[k].equals(b)) totalBucket[k] += v;
				}
			}
			rs.close(); ps.close();
			JSONArray rows = new JSONArray();
			java.util.Iterator it = perCustomer.values().iterator();
			while (it.hasNext()) {
				rows.put(it.next());
			}
			JSONObject ringkas = new JSONObject();
			double grand = 0;
			for (int k = 0; k < kunci.length; k++) {
				ringkas.put(kunci[k], totalBucket[k]);
				grand += totalBucket[k];
			}
			ringkas.put("total", grand);
			hasil.put("status", "00");
			hasil.put("rows", rows);
			hasil.put("ringkasan", ringkas);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void receivableAgingSales(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("piutang")) {
			tolak(hasil, "Menu Piutang tidak aktif untuk akun Anda.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE d.status = 'AKTIF'");
			if (aktorSales(ctx)) {
				if (ctx.salesId == null) {
					tolak(hasil, "Akun sales Anda belum punya profil sales aktif.");
					return;
				}
				where.append(" AND d.sales = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) where.append(" AND d.toko = ?");
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT COALESCE(s.id,0), COALESCE(s.nama,'(tanpa sales)'), COUNT(d.id),"
							+ " SUM(COALESCE(d.total_faktur,0)),"
							+ " SUM(COALESCE(d.dibayar_awal,0) + " + EXPR_ALOKASI + "),"
							+ " SUM(" + EXPR_OUTSTANDING + "),"
							+ " SUM(CASE WHEN " + EXPR_OUTSTANDING + " <= 0.009 THEN 1 ELSE 0 END)"
							+ " FROM koperasi.piutang_customer_doc d"
							+ " LEFT JOIN koperasi.sales_inventory s ON d.sales = s.id" + where
							+ " GROUP BY s.id, s.nama ORDER BY 6 DESC");
			int ix = 1;
			if (aktorSales(ctx)) ps.setLong(ix++, ctx.salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) ps.setLong(ix++, ctx.tokoId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray rows = new JSONArray();
			double totalAssigned = 0, totalTertagih = 0, totalOutstanding = 0;
			while (rs.next()) {
				JSONObject r = new JSONObject();
				long sid = rs.getLong(1);
				r.put("salesId", sid == 0 ? JSONObject.NULL : Long.valueOf(sid));
				r.put("salesNama", str(rs.getString(2)));
				r.put("jumlahFaktur", rs.getLong(3));
				r.put("totalAssigned", rs.getDouble(4));
				r.put("totalTertagih", rs.getDouble(5));
				r.put("totalOutstanding", rs.getDouble(6));
				r.put("jumlahLunas", rs.getLong(7));
				rows.put(r);
				totalAssigned += rs.getDouble(4);
				totalTertagih += rs.getDouble(5);
				totalOutstanding += rs.getDouble(6);
			}
			rs.close(); ps.close();
			JSONObject ringkas = new JSONObject();
			ringkas.put("totalAssigned", totalAssigned);
			ringkas.put("totalTertagih", totalTertagih);
			ringkas.put("totalOutstanding", totalOutstanding);
			hasil.put("status", "00");
			hasil.put("rows", rows);
			hasil.put("ringkasan", ringkas);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
