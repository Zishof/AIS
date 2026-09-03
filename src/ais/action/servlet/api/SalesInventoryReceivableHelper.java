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
import ais.database.model.koperasi.SpjSalesNota;
import ais.database.model.koperasi.NotaSalesSession;
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
		if (SalesInventoryReceivableTenant.aktif(ctx)) {
			salesOrderSimpanTenant(ctx, tbmuser, request, hasil, update, customerId, items);
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
				so.setStatus(SalesOrderLapangan.STATUS_DRAFT);
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
				// Fase B: baris ber-satuan-jual -- jumlah dasar DITURUNKAN server dari
				// qty_input x faktor (satu penegak: KantinHelper.faktorUomInputKeDasar);
				// jumlah kiriman klien hanya pratinjau.
				Long satuanJualId = optLong(it, "satuan_jual_id");
				java.math.BigDecimal qtyInputSo = optBigDecimal(it, "qty_input");
				java.math.BigDecimal faktorSo = null;
				if (satuanJualId != null) {
					if (qtyInputSo == null || qtyInputSo.signum() <= 0) {
						tx.rollback();
						tolak(hasil, "Item ke-" + (i + 1) + ": qty_input wajib > 0 untuk satuan jual.");
						return;
					}
					ais.database.model.inventory.SatuanProduk satuanJual =
							(ais.database.model.inventory.SatuanProduk) session.get(
									ais.database.model.inventory.SatuanProduk.class, satuanJualId);
					if (satuanJual == null) {
						tx.rollback();
						tolak(hasil, "Item ke-" + (i + 1) + ": satuan jual tidak ditemukan.");
						return;
					}
					double f;
					try {
						f = KantinHelper.faktorUomInputKeDasar(p, satuanJual);
					} catch (IllegalArgumentException salah) {
						tx.rollback();
						tolak(hasil, salah.getMessage());
						return;
					}
					faktorSo = new java.math.BigDecimal(String.valueOf(f));
					jumlah = qtyInputSo.multiply(faktorSo);
				}
				SalesOrderLapanganItem baris = new SalesOrderLapanganItem();
				baris.setSalesOrder(so);
				baris.setProduk(p);
				baris.setNamaProduk(str(p.getNama()));
				baris.setHargaSatuan(harga);
				baris.setJumlah(jumlah);
				if (satuanJualId != null) {
					baris.setSatuanJual(satuanJualId);
					baris.setQtyInput(qtyInputSo);
					baris.setFaktorKeDasar(faktorSo);
				}
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
			boolean jalurTenant = SalesInventoryReceivableTenant.aktif(ctx);
			StringBuilder where = new StringBuilder(" WHERE 1=1");
			if (!status.isEmpty()) {
				// Kosakata status berbeda: klien mengirim DRAFT, kolom tenant berisi DRAF.
				// Membandingkan mentah membuat saringan "hanya draf" mengembalikan kosong.
				where.append(jalurTenant
						? SalesInventoryReceivableTenant.syaratStatusOrder()
						: " AND o.status = ?");
			}
			if (customerId != null) {
				where.append(jalurTenant ? " AND o.customer_id = ?" : " AND o.customer = ?");
			}
			if (salesId != null) {
				where.append(jalurTenant ? " AND o.salesperson_id = ?" : " AND o.sales = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) {
				// sales_order tenant menyimpan toko_id langsung; tidak perlu lewat faktur.
				where.append(jalurTenant ? " AND o.toko_id = ?" : " AND o.toko = ?");
			}
			if (!q.isEmpty()) {
				where.append(jalurTenant
						? " AND (LOWER(o.nomor_dokumen) LIKE ? OR LOWER(c.nama) LIKE ?)"
						: " AND (LOWER(o.nomor) LIKE ? OR LOWER(c.nama) LIKE ?)");
			}
			String base;
			String pilihSo;
			if (jalurTenant) {
				String sk = SalesInventoryReceivableTenant.skema(ctx);
				base = SalesInventoryReceivableTenant.dasarSalesOrder(sk, where.toString());
				pilihSo = SalesInventoryReceivableTenant.selectSalesOrder();
			} else {
				base = " FROM koperasi.sales_order_lapangan o"
						+ " JOIN koperasi.anggota_koperasi c ON o.customer = c.id"
						+ " LEFT JOIN koperasi.sales_inventory s ON o.sales = s.id" + where;
				pilihSo = "SELECT o.id, o.nomor, o.tanggal, o.status, o.total, o.keterangan,"
						+ " c.id, c.nama, s.id, s.nama";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					pilihSo + base
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
		if (SalesInventoryReceivableTenant.aktif(ctx)) {
			salesOrderDetailTenant(ctx, orderId, hasil);
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
		if (SalesInventoryReceivableTenant.aktif(ctx)) {
			salesOrderStatusTenant(ctx, tbmuser, orderId, baru, alasan, hasil);
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
			// Fase E dok. 48 P5: konfirmasi SO (DRAFT->PESAN) memicu MTO utk baris ber-rute MTO_*.
			// Cakupan = SalesOrderLapangan SAJA (§6 no. 3: Pesanan POS = keranjang tertahan).
			if (SalesOrderLapangan.STATUS_PESAN.equals(baru)) {
				terapkanMto(session, so, hasil);
			}
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

	/**
	 * Fase E dok. 48 P5 (MTO): baris SO ber-produk rute MTO_PRODUKSI -> draf Work Order lewat
	 * mesin bersama {@code ProduksiApiHelper.buatWoDrafOtomatis} (kunci {@code MTO:SO:so:produk},
	 * idempoten); rute MTO_BELI -> {@code PengajuanPembelianGudang} ber-{@code so_id} lewat
	 * gudangPemasok toko (idempoten per SO+produk selama BARU/DIPROSES). Toko tanpa
	 * gudangPemasok: kebutuhan MTO_BELI tetap dilaporkan di respons -- tidak diam-diam hilang.
	 * Rute lain/kosong tidak disentuh. Berjalan DI DALAM transaksi konfirmasi -- SO yang
	 * mengaku terkonfirmasi tetapi pemicunya gagal adalah kebohongan data (pola Fase 0).
	 */
	@SuppressWarnings("unchecked")
	private static void terapkanMto(Session session, SalesOrderLapangan so, JSONObject hasil)
			throws Exception {
		List<SalesOrderLapanganItem> items = session
				.createQuery("from SalesOrderLapanganItem where salesOrder=:so").setEntity("so", so).list();
		JSONArray ringkasanMto = new JSONArray();
		for (SalesOrderLapanganItem item : items) {
			Produk produk = item.getProduk();
			if (produk == null || produk.getRute() == null) {
				continue;
			}
			String rute = produk.getRute().trim().toUpperCase();
			BigDecimal qty = item.getJumlah() == null ? BigDecimal.ZERO : item.getJumlah();
			if (qty.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			JSONObject r = new JSONObject();
			r.put("produkId", produk.getId()); r.put("nama", produk.getNama()); r.put("qty", qty);
			if (Produk.RUTE_MTO_PRODUKSI.equals(rute)) {
				Long woBaru = ais.action.servlet.api.ProduksiApiHelper.buatWoDrafOtomatis(session,
						so.getToko().getId().longValue(), produk.getId(), qty,
						"MTO:SO:" + so.getId() + ":" + produk.getId(),
						"Draf otomatis MTO dari SO " + so.getNomor() + " (" + produk.getNama() + ").");
				r.put("tindakan", woBaru == null ? "WO sudah ada" : "WO draf dibuat");
				if (woBaru != null) r.put("woId", woBaru);
			} else if (Produk.RUTE_MTO_BELI.equals(rute)) {
				Toko toko = so.getToko();
				if (toko == null || toko.getGudangPemasok() == null) {
					r.put("tindakan", "TANPA pengajuan: toko belum punya Gudang Pemasok");
				} else {
					Object ada = session.createQuery(
							"select id from PengajuanPembelianGudang where soId=:so and produk.id=:p"
									+ " and status in ('BARU','DIPROSES')")
							.setLong("so", so.getId().longValue()).setLong("p", produk.getId().longValue())
							.setMaxResults(1).uniqueResult();
					if (ada == null) {
						ais.database.model.inventory.PengajuanPembelianGudang pengajuan =
								new ais.database.model.inventory.PengajuanPembelianGudang();
						pengajuan.setProduk(produk);
						pengajuan.setGudangAsal(toko.getGudangPemasok());
						pengajuan.setGudangTujuan(toko.getGudangPemasok().getGudangInduk());
						pengajuan.setQtyDiminta(Double.valueOf(qty.doubleValue()));
						pengajuan.setStatus(ais.database.model.inventory.PengajuanPembelianGudang.STATUS_BARU);
						pengajuan.setOtomatis(Boolean.TRUE);
						pengajuan.setSoId(so.getId());
						pengajuan.setWaktuDibuat(ais.ui.util.WaktuUtil.getDate());
						pengajuan.setKeterangan("MTO dari SO " + so.getNomor() + ": " + qty + " "
								+ produk.getNama() + " dipesan customer.");
						session.save(pengajuan);
						r.put("tindakan", "pengajuan pembelian dibuat");
					} else {
						r.put("tindakan", "pengajuan sudah ada");
					}
				}
			} else {
				continue;
			}
			ringkasanMto.put(r);
		}
		if (ringkasanMto.length() > 0) {
			hasil.put("mto", ringkasanMto);
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
		if (SalesInventoryReceivableTenant.aktif(ctx)) {
			salesOrderInvoiceTenant(ctx, tbmuser, orderId, request, hasil);
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
			boolean jalurTenant = SalesInventoryReceivableTenant.aktif(ctx);
			String skTenant = jalurTenant ? SalesInventoryReceivableTenant.skema(ctx) : null;
			String exprSisa = jalurTenant
					? SalesInventoryReceivableTenant.outstanding(skTenant) : EXPR_OUTSTANDING;
			StringBuilder where = new StringBuilder(" WHERE d.status = 'AKTIF'");
			if (!tampilkanLunas) where.append(" AND ").append(exprSisa).append(" > 0.009");
			if (customerId != null) {
				where.append(" AND ").append(jalurTenant
						? SalesInventoryReceivableTenant.kolomCustomerPiutang() : "d.customer")
						.append(" = ?");
			}
			if (orderId != null) {
				// Piutang tenant tidak menyimpan order; fakturnya yang menautkannya.
				where.append(" AND ").append(jalurTenant
						? SalesInventoryReceivableTenant.kolomOrderPiutang() : "d.sales_order")
						.append(" = ?");
			}
			if (salesId != null) {
				where.append(" AND ").append(jalurTenant
						? SalesInventoryReceivableTenant.kolomSalesPiutang() : "d.sales")
						.append(" = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) {
				// Lingkup toko ditegakkan lewat faktur; piutang tenant tidak punya toko.
				where.append(" AND ").append(jalurTenant
						? SalesInventoryReceivableTenant.kolomTokoPiutang() : "d.toko")
						.append(" = ?");
			}
			if (!q.isEmpty()) {
				where.append(" AND (LOWER(").append(jalurTenant
						? SalesInventoryReceivableTenant.kolomNomorPiutang() : "d.nomor")
						.append(") LIKE ? OR LOWER(c.nama) LIKE ?)");
			}
			String base;
			String pilihAr;
			if (jalurTenant) {
				base = SalesInventoryReceivableTenant.dasarPiutang(skTenant, where.toString());
				pilihAr = SalesInventoryReceivableTenant.selectPiutang(skTenant);
			} else {
				base = " FROM koperasi.piutang_customer_doc d"
						+ " JOIN koperasi.anggota_koperasi c ON d.customer = c.id"
						+ " LEFT JOIN koperasi.sales_inventory s ON d.sales = s.id"
						+ " LEFT JOIN koperasi.sales_order_lapangan o ON d.sales_order = o.id" + where;
				pilihAr = "SELECT d.id, d.nomor, d.tanggal, d.jatuh_tempo, d.total_faktur, d.dibayar_awal, "
						+ EXPR_ALOKASI + " AS teralokasi, " + EXPR_OUTSTANDING + " AS outstanding,"
						+ " c.id, c.nama, s.id, s.nama, o.id, o.nomor, d.keterangan";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					pilihAr + base
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
		if (SalesInventoryReceivableTenant.aktif(ctx)) {
			collectionCreateTenant(ctx, tbmuser, request, hasil, customerId, nominal, metode,
					kodeUnik, alokasi);
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
			boolean jalurTenant = SalesInventoryReceivableTenant.aktif(ctx);
			StringBuilder where = new StringBuilder(" WHERE 1=1");
			if (customerId != null) {
				where.append(" AND ").append(jalurTenant
						? SalesInventoryReceivableTenant.kolomCustomerPenagihan() : "p.customer").append(" = ?");
			}
			if (salesId != null) {
				where.append(" AND ").append(jalurTenant
						? SalesInventoryReceivableTenant.kolomSalesPenagihan() : "p.sales").append(" = ?");
			}
			if (dari != null) where.append(" AND p.tanggal >= ?");
			if (sampai != null) where.append(" AND p.tanggal < (CAST(? AS date) + 1)");
			String sqlRiwayat;
			if (jalurTenant) {
				String sk = SalesInventoryReceivableTenant.skema(ctx);
				sqlRiwayat = SalesInventoryReceivableTenant.selectPenagihan(sk)
						+ SalesInventoryReceivableTenant.dasarPenagihan(sk, where.toString())
						+ " ORDER BY p.tanggal DESC, p.id DESC LIMIT 500";
			} else {
				sqlRiwayat = "SELECT p.id, p.nomor, p.tanggal, p.nominal, p.metode, p.no_bg, p.nama_bank,"
							+ " p.keterangan, c.id, c.nama, s.nama,"
							+ " (SELECT COALESCE(string_agg(d.nomor, ', '), '') FROM"
							+ "   koperasi.alokasi_penerimaan_piutang_customer a"
							+ "   JOIN koperasi.piutang_customer_doc d ON a.piutang_doc = d.id"
							+ "   WHERE a.penerimaan = p.id) AS faktur,"
							+ " COALESCE(p.status_dok,'AKTIF'), p.status_bg"
							+ " FROM koperasi.penerimaan_piutang_customer p"
							+ " JOIN koperasi.anggota_koperasi c ON p.customer = c.id"
							+ " LEFT JOIN koperasi.sales_inventory s ON p.sales = s.id" + where
							+ " ORDER BY p.tanggal DESC, p.id DESC LIMIT 500";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sqlRiwayat);
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
		if (SalesInventoryReceivableTenant.aktif(ctx)) {
			collectionReceiptTenant(ctx, id, hasil);
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
			boolean jalurTenant = SalesInventoryReceivableTenant.aktif(ctx);
			// Baris order tenant tidak menyimpan salinan nama produk; saringannya ke master.
			if (!q.isEmpty()) {
				where.append(jalurTenant ? " AND LOWER(pr.nama) LIKE ?"
						: " AND LOWER(i.nama_produk) LIKE ?");
			}
			if (salesId != null) {
				where.append(jalurTenant ? " AND o.salesperson_id = ?" : " AND o.sales = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) {
				where.append(jalurTenant ? " AND o.toko_id = ?" : " AND o.toko = ?");
			}
			String sqlLap;
			if (jalurTenant) {
				String urutTenant = urut.replace("i.nama_produk", "pr.nama")
						.replace("SUM(i.jumlah)", "SUM(COALESCE(i.kuantitas,0))")
						.replace("SUM(i.subtotal)", "SUM(COALESCE(i.total,0))");
				sqlLap = SalesInventoryReceivableTenant.sqlLaporan(SalesInventoryReceivableTenant.skema(ctx), where.toString(), urutTenant);
			} else {
				sqlLap = "SELECT i.produk, i.nama_produk, SUM(i.jumlah), SUM(i.subtotal)"
						+ " FROM koperasi.sales_order_lapangan_item i"
						+ " JOIN koperasi.sales_order_lapangan o ON i.sales_order = o.id" + where
						+ " GROUP BY i.produk, i.nama_produk ORDER BY " + urut + " LIMIT 500";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sqlLap);
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
			boolean jalurTenant = SalesInventoryReceivableTenant.aktif(ctx);
			String asOf = asOfRaw.matches("\\d{4}-\\d{2}-\\d{2}") ? asOfRaw
					: new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
			StringBuilder where = new StringBuilder(" WHERE d.status = 'AKTIF' AND "
					+ (jalurTenant
							? SalesInventoryReceivableTenant.outstanding(
									SalesInventoryReceivableTenant.skema(ctx))
							: EXPR_OUTSTANDING)
					+ " > 0.009");
			if (salesId != null) {
				where.append(" AND ").append(jalurTenant
						? SalesInventoryReceivableTenant.kolomSalesPiutang() : "d.sales").append(" = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) {
				where.append(" AND ").append(jalurTenant
						? SalesInventoryReceivableTenant.kolomTokoPiutang() : "d.toko").append(" = ?");
			}
			String sqlAgingC;
			if (jalurTenant) {
				String sk = SalesInventoryReceivableTenant.skema(ctx);
				sqlAgingC = SalesInventoryReceivableTenant.sqlAgingCustomer(sk, SalesInventoryReceivableTenant.bucketAging(asOf), where.toString());
			} else {
				String bucket = exprBucket(asOfSql);
				sqlAgingC = "SELECT c.id, c.nama, " + bucket + " AS bucket, SUM(" + EXPR_OUTSTANDING + ")"
						+ " FROM koperasi.piutang_customer_doc d"
						+ " JOIN koperasi.anggota_koperasi c ON d.customer = c.id" + where
						+ " GROUP BY c.id, c.nama, bucket ORDER BY c.nama";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sqlAgingC);
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
			boolean jalurTenantAs = SalesInventoryReceivableTenant.aktif(ctx);
			StringBuilder where = new StringBuilder(" WHERE d.status = 'AKTIF'");
			if (aktorSales(ctx)) {
				if (ctx.salesId == null) {
					tolak(hasil, "Akun sales Anda belum punya profil sales aktif.");
					return;
				}
				where.append(" AND ").append(jalurTenantAs
						? SalesInventoryReceivableTenant.kolomSalesPiutang() : "d.sales")
						.append(" = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) {
				// Lingkup toko lewat faktur: piutang tenant tidak menyimpan toko.
				where.append(" AND ").append(jalurTenantAs
						? SalesInventoryReceivableTenant.kolomTokoPiutang() : "d.toko")
						.append(" = ?");
			}
			String sqlAgingS;
			if (jalurTenantAs) {
				sqlAgingS = SalesInventoryReceivableTenant.sqlAgingSales(
						SalesInventoryReceivableTenant.skema(ctx), where.toString());
			} else {
				sqlAgingS = "SELECT COALESCE(s.id,0), COALESCE(s.nama,'(tanpa sales)'), COUNT(d.id),"
						+ " SUM(COALESCE(d.total_faktur,0)),"
						+ " SUM(COALESCE(d.dibayar_awal,0) + " + EXPR_ALOKASI + "),"
						+ " SUM(" + EXPR_OUTSTANDING + "),"
						+ " SUM(CASE WHEN " + EXPR_OUTSTANDING + " <= 0.009 THEN 1 ELSE 0 END)"
						+ " FROM koperasi.piutang_customer_doc d"
						+ " LEFT JOIN koperasi.sales_inventory s ON d.sales = s.id" + where
						+ " GROUP BY s.id, s.nama ORDER BY 6 DESC";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sqlAgingS);
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
	// =============================================================================================
	// Jalur schema tenant -- rincian order, transisi status, kwitansi
	// =============================================================================================

	/**
	 * Rincian sales order pada schema tenant.
	 *
	 * <p>Penjaga lingkup sales ditegakkan di sini, sama seperti jalur legacy: sales lapangan
	 * hanya boleh membuka ordernya sendiri. Melewatkannya berarti satu sales dapat membaca
	 * order sales lain hanya dengan menebak id.</p>
	 */
	private static void salesOrderDetailTenant(EbisnisActorContextResolver.ActorContext ctx,
			Long orderId, JSONObject hasil) throws Exception {
		String skema = SalesInventoryReceivableTenant.skema(ctx);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.selectOrderRinci(skema));
			ps.setLong(1, orderId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				tolak(hasil, "Sales order tidak ditemukan.");
				return;
			}
			Long salesId = rs.getObject(10) == null ? null : Long.valueOf(rs.getLong(10));
			if (aktorSales(ctx) && (salesId == null || ctx.salesId == null
					|| !ctx.salesId.equals(salesId))) {
				rs.close();
				ps.close();
				tolak(hasil, "Order ini bukan milik sales Anda.");
				return;
			}
			JSONObject j = new JSONObject();
			j.put("id", rs.getLong(1));
			j.put("nomor", str(rs.getString(2)));
			j.put("tanggal", str(rs.getDate(3)));
			j.put("status", str(rs.getString(4)));
			j.put("total", rs.getDouble(5));
			j.put("keterangan", str(rs.getString(6)));
			j.put("alasanBatal", str(rs.getString(7)));
			j.put("customerId", rs.getLong(8));
			j.put("customerNama", str(rs.getString(9)));
			j.put("salesId", salesId == null ? JSONObject.NULL : salesId);
			j.put("salesNama", str(rs.getString(11)));
			Object docId = rs.getObject(12);
			j.put("piutangDocId", docId == null ? JSONObject.NULL : Long.valueOf(rs.getLong(12)));
			j.put("piutangDocNomor", docId == null ? "" : str(rs.getString(13)));
			rs.close();
			ps.close();

			java.sql.PreparedStatement psI = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.selectOrderBaris(skema));
			psI.setLong(1, orderId.longValue());
			java.sql.ResultSet rsI = psI.executeQuery();
			JSONArray arr = new JSONArray();
			while (rsI.next()) {
				JSONObject r = new JSONObject();
				r.put("id", rsI.getLong(1));
				r.put("produkId", rsI.getLong(2));
				r.put("namaProduk", str(rsI.getString(3)));
				r.put("hargaSatuan", rsI.getDouble(4));
				r.put("jumlah", rsI.getDouble(5));
				r.put("subtotal", rsI.getDouble(6));
				arr.put(r);
			}
			rsI.close();
			psI.close();
			j.put("items", arr);
			hasil.put("status", "00");
			hasil.put("data", j);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Transisi status sales order pada schema tenant.
	 *
	 * <p>Matriks transisinya disalin apa adanya dari jalur legacy — bukan disederhanakan.
	 * Pembatalan tetap hanya boleh sebelum barang keluar, dan tetap wajib beralasan.</p>
	 *
	 * <p>Pemicu MTO pada DRAFT &rarr; PESAN tidak dijalankan karena model tenant tidak punya
	 * rute produk maupun tabel work order; lihat {@code mtoMungkin()}. Penjaganya tetap
	 * dipasang supaya keadaan itu berhenti berisik bila kelak berubah.</p>
	 */
	private static void salesOrderStatusTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, Long orderId, String baru, String alasan, JSONObject hasil)
			throws Exception {
		String skema = SalesInventoryReceivableTenant.skema(ctx);
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.selectOrderUntukStatus(skema));
			ps.setLong(1, orderId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				tolak(hasil, "Sales order tidak ditemukan.");
				return;
			}
			String lama = str(rs.getString(1));
			Long salesId = rs.getObject(2) == null ? null : Long.valueOf(rs.getLong(2));
			rs.close();
			ps.close();
			if (aktorSales(ctx) && (salesId == null || ctx.salesId == null
					|| !ctx.salesId.equals(salesId))) {
				tolak(hasil, "Order ini bukan milik sales Anda.");
				return;
			}
			boolean boleh;
			if (SalesOrderLapangan.STATUS_BATAL.equals(baru)) {
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
			if (SalesOrderLapangan.STATUS_PESAN.equals(baru)
					&& SalesInventoryReceivableTenant.mtoMungkin()) {
				tolak(hasil, "Konfirmasi order menuntut pemicu MTO yang belum ada pada schema"
						+ " tenant; order tidak dikonfirmasi supaya datanya tidak berbohong.");
				return;
			}
			tx = session.beginTransaction();
			java.sql.PreparedStatement psU = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.updateStatusOrder(skema));
			psU.setString(1, baru);
			if (SalesOrderLapangan.STATUS_BATAL.equals(baru)) {
				psU.setString(2, alasan);
			} else {
				psU.setNull(2, java.sql.Types.VARCHAR);
			}
			psU.setString(3, tbmuser.getUserId());
			psU.setLong(4, orderId.longValue());
			int kena = psU.executeUpdate();
			psU.close();
			if (kena != 1) {
				tx.rollback();
				tolak(hasil, "Status sales order gagal disimpan.");
				return;
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", orderId);
			hasil.put("statusBaru", baru);
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Kwitansi penerimaan piutang pada schema tenant: 12 medan + rincian alokasinya. */
	private static void collectionReceiptTenant(EbisnisActorContextResolver.ActorContext ctx,
			Long id, JSONObject hasil) throws Exception {
		String skema = SalesInventoryReceivableTenant.skema(ctx);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.selectKwitansi(skema));
			ps.setLong(1, id.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				tolak(hasil, "Penerimaan tidak ditemukan.");
				return;
			}
			Long salesId = rs.getObject(13) == null ? null : Long.valueOf(rs.getLong(13));
			if (aktorSales(ctx) && (salesId == null || ctx.salesId == null
					|| !ctx.salesId.equals(salesId))) {
				rs.close();
				ps.close();
				tolak(hasil, "Kwitansi ini bukan milik sales Anda.");
				return;
			}
			JSONObject j = new JSONObject();
			j.put("id", rs.getLong(1));
			j.put("nomor", str(rs.getString(2)));
			j.put("tanggal", str(rs.getDate(3)));
			j.put("nominal", rs.getDouble(4));
			j.put("metode", str(rs.getString(5)));
			j.put("noBg", str(rs.getString(6)));
			j.put("namaBank", str(rs.getString(7)));
			j.put("keterangan", str(rs.getString(8)));
			j.put("customerNama", str(rs.getString(9)));
			j.put("customerKode", str(rs.getString(10)));
			j.put("salesNama", str(rs.getString(11)));
			j.put("dibuatOleh", str(rs.getString(12)));
			rs.close();
			ps.close();

			java.sql.PreparedStatement psA = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.selectAlokasiKwitansi(skema));
			psA.setLong(1, id.longValue());
			java.sql.ResultSet rsA = psA.executeQuery();
			JSONArray arr = new JSONArray();
			while (rsA.next()) {
				JSONObject r = new JSONObject();
				r.put("fakturNomor", str(rsA.getString(1)));
				r.put("fakturTanggal", str(rsA.getDate(2)));
				r.put("totalFaktur", rsA.getDouble(3));
				r.put("nominal", rsA.getDouble(4));
				arr.put(r);
			}
			rsA.close();
			psA.close();
			j.put("alokasi", arr);
			hasil.put("status", "00");
			hasil.put("data", j);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
	// =============================================================================================
	// Jalur schema tenant -- simpan order dan terbitkan faktur
	// =============================================================================================

	/** Keberadaan satu baris induk pada schema tenant. */
	private static boolean adaTenant(Session session, String skema, String tabel, Long id)
			throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				SalesInventoryReceivableTenant.adaBaris(skema, tabel));
		ps.setLong(1, id.longValue());
		java.sql.ResultSet rs = ps.executeQuery();
		boolean ada = rs.next();
		rs.close();
		ps.close();
		return ada;
	}

	/**
	 * Satu satuan sebagai {@code [kategori, pecahan]}, dengan pecahannya berasal dari
	 * {@link SalesInventoryReceivableTenant#pecahanSatuan}. {@code null} bila satuannya tidak ada
	 * atau rasionya tidak sah.
	 */
	private static Object[] pecahanSatuan(Session session, String skema, Long satuanId)
			throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				SalesInventoryReceivableTenant.satuanKonversi(skema));
		try {
			ps.setLong(1, satuanId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				return null;
			}
			String kategori = rs.getString(1);
			BigDecimal rasio = rs.getBigDecimal(2);
			String arah = rs.getString(3);
			rs.close();
			BigDecimal[] pecahan = SalesInventoryReceivableTenant.pecahanSatuan(rasio, arah);
			if (pecahan == null) {
				return null;
			}
			return new Object[] { kategori, pecahan };
		} finally {
			ps.close();
		}
	}

	/**
	 * Menurunkan kuantitas dasar dari {@code qtyInput} pada satuan jual.
	 *
	 * <p>Mengembalikan {@code [kuantitasDasar, faktorCuplikan]}, atau {@code null} bila ditolak
	 * (pesannya sudah ditaruh pada {@code hasil}).</p>
	 *
	 * <p>Pembagiannya dilakukan <b>sekali</b>, atas pembilang yang sudah dikalikan {@code qtyInput}
	 * — bukan atas faktor yang dibulatkan lebih dulu. Pada kasus yang lazim penyebutnya 1 dan
	 * hasilnya bulat betulan; kalau tidak, pembulatannya terjadi satu kali saja, pada angka
	 * terbesar yang tersedia.</p>
	 */
	private static BigDecimal[] konversiSatuanTenant(Session session, String skema, Long produkId,
			Long satuanJualId, BigDecimal qtyInput, int nomorBaris, JSONObject hasil)
			throws Exception {
		Long dasarId = null;
		java.sql.PreparedStatement psD = session.connection().prepareStatement(
				SalesInventoryReceivableTenant.satuanDasarProduk(skema));
		try {
			psD.setLong(1, produkId.longValue());
			java.sql.ResultSet rs = psD.executeQuery();
			if (rs.next()) {
				long v = rs.getLong(1);
				if (!rs.wasNull()) {
					dasarId = Long.valueOf(v);
				}
			}
			rs.close();
		} finally {
			psD.close();
		}
		if (dasarId == null) {
			// Produk tanpa satuan dasar: jalur legacy mengembalikan faktor 1 demi katalog lama.
			// Di sini itu berarti menerima qty_input apa adanya sebagai kuantitas dasar, dan
			// diam-diam menganggap DUS sama dengan PCS. Ditolak: yang kurang datanya, bukan
			// permintaannya.
			tolak(hasil, "Item ke-" + nomorBaris + ": produk " + produkId + " belum punya satuan"
					+ " dasar, sehingga satuan jual tidak dapat dikonversi. Lengkapi satuan produk"
					+ " lebih dulu, atau kirim jumlah dalam satuan dasar.");
			return null;
		}
		Object[] jual = pecahanSatuan(session, skema, satuanJualId);
		if (jual == null) {
			tolak(hasil, "Item ke-" + nomorBaris + ": satuan jual tidak ditemukan atau rasionya"
					+ " tidak sah (harus lebih dari 0).");
			return null;
		}
		Object[] dasar = pecahanSatuan(session, skema, dasarId);
		if (dasar == null) {
			tolak(hasil, "Item ke-" + nomorBaris + ": satuan dasar produk " + produkId
					+ " rasionya tidak sah (harus lebih dari 0).");
			return null;
		}
		if (!((String) jual[0]).equals((String) dasar[0])) {
			tolak(hasil, "Item ke-" + nomorBaris + ": satuan jual berkategori " + jual[0]
					+ " tidak dapat dikonversi ke satuan dasar berkategori " + dasar[0]
					+ ". Perbaiki kategori satuan pada master satuan, lalu coba kembali.");
			return null;
		}
		BigDecimal[] hasilKonversi = SalesInventoryReceivableTenant.kuantitasDasar(
				(BigDecimal[]) jual[1], (BigDecimal[]) dasar[1], qtyInput);
		if (hasilKonversi == null) {
			tolak(hasil, "Item ke-" + nomorBaris + ": konversi satuan tidak sah.");
			return null;
		}
		if (hasilKonversi[0].signum() <= 0) {
			tolak(hasil, "Item ke-" + nomorBaris + ": konversi satuan menghasilkan kuantitas nol."
					+ " Periksa rasio satuan jual terhadap satuan dasarnya.");
			return null;
		}
		return hasilKonversi;
	}

	/**
	 * Menyisipkan seluruh baris order dan mengembalikan totalnya; {@code null} bila ada baris
	 * yang ditolak (pesannya sudah ditaruh pada {@code hasil}).
	 */
	private static BigDecimal sisipBarisOrderTenant(Session session, String skema, long orderId,
			JSONArray items, String oleh, JSONObject hasil) throws Exception {
		BigDecimal total = BigDecimal.ZERO;
		for (int i = 0; i < items.length(); i++) {
			JSONObject it = items.getJSONObject(i);
			Long produkId = optLong(it, "produk_id");
			BigDecimal jumlah = optBigDecimal(it, "jumlah");
			BigDecimal harga = optBigDecimal(it, "harga");
			if (produkId == null || jumlah == null || jumlah.signum() <= 0 || harga == null
					|| harga.signum() < 0) {
				tolak(hasil, "Item ke-" + (i + 1) + " tidak valid (produk_id/jumlah>0/harga>=0).");
				return null;
			}
			if (!adaTenant(session, skema, "produk", produkId)) {
				tolak(hasil, "Produk " + produkId + " tidak ditemukan.");
				return null;
			}
			// Satuan jual: jumlah dasar DITURUNKAN server dari qty_input × faktor, sama seperti
			// jalur legacy; jumlah kiriman klien hanya pratinjau. Sejak bundel v19 satuan tenant
			// membawa metadata konversinya, sehingga penurunannya bisa dilakukan di sini.
			Long satuanJualId = optLong(it, "satuan_jual_id");
			BigDecimal qtyInput = null;
			BigDecimal faktorCuplikan = null;
			if (satuanJualId != null) {
				qtyInput = optBigDecimal(it, "qty_input");
				if (qtyInput == null || qtyInput.signum() <= 0) {
					tolak(hasil, "Item ke-" + (i + 1) + ": qty_input wajib > 0 untuk satuan jual.");
					return null;
				}
				BigDecimal[] hitung = konversiSatuanTenant(session, skema, produkId, satuanJualId,
						qtyInput, i + 1, hasil);
				if (hitung == null) {
					return null; // pesannya sudah ditaruh pada hasil
				}
				jumlah = hitung[0];
				faktorCuplikan = hitung[1];
			}
			BigDecimal sub = harga.multiply(jumlah);
			java.sql.PreparedStatement ins = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.sisipOrderBaris(skema));
			ins.setLong(1, orderId);
			ins.setInt(2, i + 1);
			ins.setLong(3, produkId.longValue());
			ins.setBigDecimal(4, jumlah);
			ins.setBigDecimal(5, harga);
			ins.setBigDecimal(6, sub);
			if (satuanJualId == null) {
				ins.setNull(7, java.sql.Types.BIGINT);
			} else {
				ins.setLong(7, satuanJualId.longValue());
			}
			ins.setBigDecimal(8, qtyInput);
			ins.setBigDecimal(9, faktorCuplikan);
			ins.setString(10, oleh);
			ins.executeUpdate();
			ins.close();
			total = total.add(sub);
		}
		return total;
	}

	/**
	 * Simpan/ubah sales order pada schema tenant.
	 *
	 * <p>Alur dan seluruh penjaganya disalin dari jalur legacy: idempotensi lewat kode unik,
	 * order hanya boleh diubah selama DRAFT/PESAN, sales lapangan hanya boleh menyentuh ordernya
	 * sendiri, dan nomor dokumen dibentuk setelah id-nya diketahui.</p>
	 *
	 * <p>Idempotensinya kini benar-benar mengikat: indeks unik parsial pada
	 * {@code sales_order.idempotency_key} dari migrasi v11 menutup dua permintaan bersamaan yang
	 * lolos pemeriksaan di muka. Pelanggaran batasannya diperlakukan sebagai pengulangan yang
	 * sah, sama seperti jalur legacy memperlakukan {@code ConstraintViolationException}.</p>
	 */
	private static void salesOrderSimpanTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject request, JSONObject hasil, boolean update, Long customerId,
			JSONArray items) throws Exception {
		String skema = SalesInventoryReceivableTenant.skema(ctx);
		String oleh = tbmuser.getUserId();
		Date tanggal = optTanggal(request, "tanggal");
		String keterangan = request.optString("keterangan", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			if (update) {
				Long orderId = optLong(request, "order_id");
				if (orderId == null) {
					tolak(hasil, "order_id wajib diisi.");
					return;
				}
				java.sql.PreparedStatement ps = session.connection().prepareStatement(
						SalesInventoryReceivableTenant.orderUntukUbah(skema));
				ps.setLong(1, orderId.longValue());
				java.sql.ResultSet rs = ps.executeQuery();
				if (!rs.next()) {
					rs.close();
					ps.close();
					tolak(hasil, "Sales order tidak ditemukan.");
					return;
				}
				String st = str(rs.getString(1));
				Long salesId = rs.getObject(2) == null ? null : Long.valueOf(rs.getLong(2));
				String nomor = str(rs.getString(4));
				rs.close();
				ps.close();
				if (!SalesOrderLapangan.STATUS_DRAFT.equals(st)
						&& !SalesOrderLapangan.STATUS_PESAN.equals(st)) {
					tolak(hasil, "Order berstatus " + st + " tidak bisa diubah (hanya DRAFT/PESAN).");
					return;
				}
				if (aktorSales(ctx) && (salesId == null || ctx.salesId == null
						|| !ctx.salesId.equals(salesId))) {
					tolak(hasil, "Order ini bukan milik sales Anda.");
					return;
				}
				tx = session.beginTransaction();
				java.sql.PreparedStatement psH = session.connection().prepareStatement(
						SalesInventoryReceivableTenant.hapusOrderBaris(skema));
				psH.setLong(1, orderId.longValue());
				psH.executeUpdate();
				psH.close();
				BigDecimal total = sisipBarisOrderTenant(session, skema, orderId.longValue(),
						items, oleh, hasil);
				if (total == null) {
					tx.rollback();
					return;
				}
				java.sql.PreparedStatement psU = session.connection().prepareStatement(
						SalesInventoryReceivableTenant.perbaruiOrder(skema));
				if (tanggal == null) {
					psU.setNull(1, java.sql.Types.DATE);
				} else {
					psU.setDate(1, new java.sql.Date(tanggal.getTime()));
				}
				psU.setString(2, keterangan);
				psU.setBigDecimal(3, total);
				psU.setString(4, oleh);
				psU.setLong(5, orderId.longValue());
				psU.executeUpdate();
				psU.close();
				tx.commit();
				hasil.put("status", "00");
				hasil.put("id", orderId);
				hasil.put("nomor", nomor);
				hasil.put("total", total.doubleValue());
				return;
			}

			// ---------- pembuatan ----------
			String kodeUnik = request.optString("kode_unik", "").trim();
			if (!kodeUnik.isEmpty()) {
				java.sql.PreparedStatement psC = session.connection().prepareStatement(
						SalesInventoryReceivableTenant.cariOrderByKunci(skema));
				psC.setString(1, kodeUnik);
				java.sql.ResultSet rsC = psC.executeQuery();
				boolean ada = rsC.next();
				long idAda = ada ? rsC.getLong(1) : 0;
				String nomorAda = ada ? str(rsC.getString(2)) : "";
				rsC.close();
				psC.close();
				if (ada) {
					hasil.put("status", "00");
					hasil.put("id", idAda);
					hasil.put("nomor", nomorAda);
					hasil.put("idempotentReplay", true);
					return;
				}
			}
			if (!adaTenant(session, skema, "customer", customerId)) {
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
			if (!adaTenant(session, skema, "toko", tokoId)) {
				tolak(hasil, "Toko tidak ditemukan.");
				return;
			}
			Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
			if (aktorSales(ctx) && salesId == null) {
				tolak(hasil, "Akun sales Anda belum punya profil sales aktif.");
				return;
			}
			if (salesId != null && !adaTenant(session, skema, "salesperson", salesId)) {
				tolak(hasil, "Profil sales tidak ditemukan.");
				return;
			}
			// tanggal tenant NOT NULL; legacy membolehkannya kosong.
			Date tanggalBaru = tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;

			tx = session.beginTransaction();
			java.sql.PreparedStatement ins = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.sisipOrder(skema),
					java.sql.Statement.RETURN_GENERATED_KEYS);
			ins.setDate(1, new java.sql.Date(tanggalBaru.getTime()));
			ins.setLong(2, customerId.longValue());
			if (salesId == null) {
				ins.setNull(3, java.sql.Types.BIGINT);
			} else {
				ins.setLong(3, salesId.longValue());
			}
			ins.setLong(4, tokoId.longValue());
			ins.setString(5, keterangan);
			if (kodeUnik.isEmpty()) {
				ins.setNull(6, java.sql.Types.VARCHAR);
			} else {
				ins.setString(6, kodeUnik);
			}
			ins.setString(7, oleh);
			ins.executeUpdate();
			long orderId = 0;
			java.sql.ResultSet gk = ins.getGeneratedKeys();
			if (gk.next()) {
				orderId = gk.getLong(1);
			}
			gk.close();
			ins.close();
			if (orderId <= 0) {
				tx.rollback();
				tolak(hasil, "Sales order gagal disimpan.");
				return;
			}
			BigDecimal total = sisipBarisOrderTenant(session, skema, orderId, items, oleh, hasil);
			if (total == null) {
				tx.rollback();
				return;
			}
			String nomor = fmtNomor("SO", tokoId, Long.valueOf(orderId));
			java.sql.PreparedStatement psF = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.finalisasiOrder(skema));
			psF.setString(1, nomor);
			psF.setBigDecimal(2, total);
			psF.setLong(3, orderId);
			psF.executeUpdate();
			psF.close();
			// Aksi ini hanya MENYISIP: pengulangan permintaan yang sama sudah keluar lebih awal
			// lewat penjaga idempotensinya, jadi tidak ada keadaan "sebelum" untuk dicuplik.
			SalesInventoryAudit.catatBaru(session, ctx, "sales_order_simpan", skema, "sales_order",
					Long.valueOf(orderId));
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", orderId);
			hasil.put("nomor", nomor);
			hasil.put("total", total.doubleValue());
		} catch (java.sql.SQLException dup) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			if (dup.getSQLState() != null && dup.getSQLState().startsWith("23")) {
				hasil.put("status", "00");
				hasil.put("idempotentReplay", true);
			} else {
				throw dup;
			}
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Menerbitkan faktur piutang dari sales order pada schema tenant.
	 *
	 * <p>Satu dokumen legacy menjadi dua dokumen tenant: {@code faktur_penjualan} (dokumen
	 * penjualannya) dan {@code piutang_customer} (tagihannya). Keduanya lahir dalam SATU
	 * transaksi bersama pemutakhiran status ordernya — faktur tanpa piutang berarti barang
	 * terjual yang tak pernah ditagih.</p>
	 *
	 * <p>Nomornya diturunkan dari id fakturnya, sebab nomor itu memang nomor faktur. Yang
	 * dikembalikan sebagai {@code piutangDocId} tetap id piutangnya, sesuai yang dibaca kembali
	 * oleh {@code salesOrderDetail}.</p>
	 */
	private static void salesOrderInvoiceTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, Long orderId, JSONObject request, JSONObject hasil) throws Exception {
		String skema = SalesInventoryReceivableTenant.skema(ctx);
		String oleh = tbmuser.getUserId();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.orderUntukFaktur(skema));
			ps.setLong(1, orderId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				tolak(hasil, "Sales order tidak ditemukan.");
				return;
			}
			String status = str(rs.getString(1));
			Long salesId = rs.getObject(2) == null ? null : Long.valueOf(rs.getLong(2));
			Long tokoId = rs.getObject(3) == null ? null : Long.valueOf(rs.getLong(3));
			long customerId = rs.getLong(4);
			BigDecimal total = rs.getBigDecimal(5);
			String nomorOrder = str(rs.getString(6));
			rs.close();
			ps.close();
			if (aktorSales(ctx) && (salesId == null || ctx.salesId == null
					|| !ctx.salesId.equals(salesId))) {
				tolak(hasil, "Order ini bukan milik sales Anda.");
				return;
			}
			java.sql.PreparedStatement psA = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.cariPiutangOrder(skema));
			psA.setLong(1, orderId.longValue());
			java.sql.ResultSet rsA = psA.executeQuery();
			boolean sudah = rsA.next();
			long idSudah = sudah ? rsA.getLong(1) : 0;
			String nomorSudah = sudah ? str(rsA.getString(2)) : "";
			rsA.close();
			psA.close();
			if (sudah) {
				hasil.put("status", "00");
				hasil.put("piutangDocId", idSudah);
				hasil.put("nomor", nomorSudah);
				hasil.put("idempotentReplay", true);
				return;
			}
			if (!SalesOrderLapangan.STATUS_TERKIRIM.equals(status)) {
				tolak(hasil, "Hanya order TERKIRIM yang bisa difakturkan (status sekarang: "
						+ status + ").");
				return;
			}
			if (total == null || total.signum() <= 0) {
				tolak(hasil, "Total order 0 -- tidak ada yang difakturkan.");
				return;
			}
			// Uang muka pada model tenant bukan kolom melainkan DOKUMEN penerimaan berikut
			// alokasinya; ia diterbitkan di bawah, dalam transaksi yang sama dengan fakturnya.
			BigDecimal dibayarAwal = optBigDecimal(request, "dibayar_awal");
			if (dibayarAwal != null && dibayarAwal.signum() < 0) {
				tolak(hasil, "dibayar_awal tidak boleh negatif.");
				return;
			}
			if (dibayarAwal != null
					&& dibayarAwal.doubleValue() > total.doubleValue() + 0.01) {
				tolak(hasil, "dibayar_awal melebihi total order.");
				return;
			}
			String metodeDp = request.optString("dp_metode", "").trim().toUpperCase();
			if (metodeDp.isEmpty()) {
				metodeDp = PenerimaanPiutangCustomer.METODE_TUNAI;
			}
			if (dibayarAwal != null && dibayarAwal.signum() > 0
					&& !SalesInventoryReceivableTenant.metodeUangMukaSah(metodeDp)) {
				tolak(hasil, "dp_metode tidak dikenali: "
						+ SalesInventoryReceivableTenant.daftarMetodeUangMuka() + ".");
				return;
			}
			int termin = 0;
			java.sql.PreparedStatement psT = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.terminCustomer(skema));
			psT.setLong(1, customerId);
			java.sql.ResultSet rsT = psT.executeQuery();
			if (rsT.next()) {
				termin = rsT.getInt(1);
			}
			rsT.close();
			psT.close();
			if (!request.isNull("termin_hari")) {
				termin = Math.max(0, request.optInt("termin_hari", termin));
			}
			Date tanggal = ais.ui.util.WaktuUtil.getDate();
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.setTime(tanggal);
			cal.add(java.util.Calendar.DAY_OF_MONTH, termin);
			Date jatuhTempo = cal.getTime();
			String idem = "SO-INV-" + orderId;

			tx = session.beginTransaction();
			java.sql.PreparedStatement insF = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.sisipFaktur(skema),
					java.sql.Statement.RETURN_GENERATED_KEYS);
			insF.setString(1, idem);
			insF.setString(2, idem);
			insF.setDate(3, new java.sql.Date(tanggal.getTime()));
			insF.setDate(4, new java.sql.Date(jatuhTempo.getTime()));
			insF.setLong(5, customerId);
			if (salesId == null) {
				insF.setNull(6, java.sql.Types.BIGINT);
			} else {
				insF.setLong(6, salesId.longValue());
			}
			insF.setLong(7, orderId.longValue());
			if (tokoId == null) {
				insF.setNull(8, java.sql.Types.BIGINT);
			} else {
				insF.setLong(8, tokoId.longValue());
			}
			insF.setBigDecimal(9, total);
			insF.setBigDecimal(10, total);
			insF.setString(11, "Faktur dari sales order " + nomorOrder);
			insF.setString(12, idem);
			insF.setString(13, oleh);
			insF.executeUpdate();
			long fakturId = 0;
			java.sql.ResultSet gkF = insF.getGeneratedKeys();
			if (gkF.next()) {
				fakturId = gkF.getLong(1);
			}
			gkF.close();
			insF.close();
			if (fakturId <= 0) {
				tx.rollback();
				tolak(hasil, "Faktur gagal disimpan.");
				return;
			}
			String nomorInv = fmtNomor("INV", tokoId, Long.valueOf(fakturId));
			java.sql.PreparedStatement psFn = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.finalisasiFaktur(skema));
			psFn.setString(1, nomorInv);
			psFn.setString(2, nomorInv);
			psFn.setLong(3, fakturId);
			psFn.executeUpdate();
			psFn.close();

			java.sql.PreparedStatement insP = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.sisipPiutang(skema),
					java.sql.Statement.RETURN_GENERATED_KEYS);
			insP.setLong(1, customerId);
			if (salesId == null) {
				insP.setNull(2, java.sql.Types.BIGINT);
			} else {
				insP.setLong(2, salesId.longValue());
			}
			insP.setLong(3, fakturId);
			insP.setString(4, nomorInv);
			insP.setDate(5, new java.sql.Date(tanggal.getTime()));
			insP.setDate(6, new java.sql.Date(jatuhTempo.getTime()));
			insP.setBigDecimal(7, total);
			insP.setBigDecimal(8, total);
			insP.setString(9, oleh);
			insP.executeUpdate();
			long piutangId = 0;
			java.sql.ResultSet gkP = insP.getGeneratedKeys();
			if (gkP.next()) {
				piutangId = gkP.getLong(1);
			}
			gkP.close();
			insP.close();
			if (piutangId <= 0) {
				tx.rollback();
				tolak(hasil, "Dokumen piutang gagal disimpan.");
				return;
			}

			// Dicuplik SEBELUM statusnya berpindah; sesudahnya keadaan lama tidak dapat
			// direkonstruksi dari mana pun.
			String sebelumOrderFaktur = SalesInventoryAudit.cuplikan(session, skema, "sales_order",
					orderId);
			java.sql.PreparedStatement psS = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.tandaiSiapTagih(skema));
			psS.setString(1, oleh);
			psS.setLong(2, orderId.longValue());
			psS.executeUpdate();
			psS.close();

			// Dua peristiwa, dua baris audit, SATU revisi masing-masing: piutangnya lahir, dan
			// ordernya berpindah status. Menggabungkannya menjadi satu catatan akan membuat
			// penelusuran per-entitas kehilangan salah satunya.
			SalesInventoryAudit.catatBaru(session, ctx, "sales_order_faktur", skema,
					"piutang_customer", Long.valueOf(piutangId));
			SalesInventoryAudit.catat(session, ctx, "sales_order_faktur", "sales_order", orderId,
					ais.service.tenant.TenantAuditWriter.REVTYPE_MOD, sebelumOrderFaktur,
					SalesInventoryAudit.cuplikan(session, skema, "sales_order", orderId));

			// Uang muka: dokumen penerimaan berikut alokasinya, di dalam transaksi yang sama.
			// Bukan kolom pengurang -- lihat catatan pada uangMukaFaktur().
			long uangMukaId = 0;
			String nomorUangMuka = null;
			if (dibayarAwal != null && dibayarAwal.signum() > 0) {
				String idemDp = "SO-DP-" + orderId;
				java.sql.PreparedStatement insD = session.connection().prepareStatement(
						SalesInventoryReceivableTenant.sisipPenerimaan(skema),
						java.sql.Statement.RETURN_GENERATED_KEYS);
				// Nomor sementara memakai kunci idempotensinya: sudah unik per order.
				insD.setString(1, idemDp);
				insD.setLong(2, customerId);
				if (salesId == null) {
					insD.setNull(3, java.sql.Types.BIGINT);
				} else {
					insD.setLong(3, salesId.longValue());
				}
				insD.setString(4, metodeDp);
				insD.setString(5, "");
				insD.setString(6, "");
				insD.setNull(7, java.sql.Types.DATE);
				insD.setBigDecimal(8, dibayarAwal);
				insD.setString(9, "Uang muka faktur " + nomorInv);
				insD.setString(10, idemDp);
				insD.setNull(11, java.sql.Types.BIGINT);
				insD.setString(12, oleh);
				insD.executeUpdate();
				java.sql.ResultSet gkD = insD.getGeneratedKeys();
				if (gkD.next()) {
					uangMukaId = gkD.getLong(1);
				}
				gkD.close();
				insD.close();
				if (uangMukaId <= 0) {
					tx.rollback();
					tolak(hasil, "Dokumen uang muka gagal disimpan.");
					return;
				}
				nomorUangMuka = fmtNomor("KWT", tokoId, Long.valueOf(uangMukaId));
				java.sql.PreparedStatement psDn = session.connection().prepareStatement(
						SalesInventoryReceivableTenant.finalisasiNomorPenerimaan(skema));
				psDn.setString(1, nomorUangMuka);
				psDn.setLong(2, uangMukaId);
				psDn.executeUpdate();
				psDn.close();

				java.sql.PreparedStatement psDa = session.connection().prepareStatement(
						SalesInventoryReceivableTenant.sisipAlokasiPenerimaan(skema));
				psDa.setLong(1, uangMukaId);
				psDa.setLong(2, piutangId);
				psDa.setBigDecimal(3, dibayarAwal);
				psDa.setString(4, oleh);
				psDa.executeUpdate();
				psDa.close();
				SalesInventoryAudit.catatBaru(session, ctx, "sales_order_faktur_uang_muka", skema,
						"penerimaan_piutang", Long.valueOf(uangMukaId));
			}

			tx.commit();
			hasil.put("status", "00");
			hasil.put("piutangDocId", piutangId);
			hasil.put("nomor", nomorInv);
			hasil.put("jatuhTempo", str(jatuhTempo));
			if (uangMukaId > 0) {
				// Uang mukanya adalah dokumen, dan nomornya perlu sampai ke pemanggil supaya
				// kwitansinya bisa dicetak. Pada jalur legacy ia hanya angka pada kolom, tanpa
				// dokumen yang bisa ditunjuk.
				hasil.put("uangMukaId", uangMukaId);
				hasil.put("uangMukaNomor", nomorUangMuka);
				hasil.put("uangMukaMetode", metodeDp);
			}
		} catch (java.sql.SQLException dup) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			if (dup.getSQLState() != null && dup.getSQLState().startsWith("23")) {
				hasil.put("status", "00");
				hasil.put("idempotentReplay", true);
			} else {
				throw dup;
			}
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
	/**
	 * Mencatat penerimaan piutang pada schema tenant.
	 *
	 * <p>Aksi ini menunggu dua bundel sekaligus — v12 untuk buku kasnya dan v13 untuk nota
	 * bawaannya — dan urutan langkahnya disalin utuh dari jalur legacy: kunci tiap faktur,
	 * periksa sisanya, terbitkan penerimaan, alokasikan, bukukan kas bila tunai, mutakhirkan
	 * status nota bawaan, lalu tandai order LUNAS bila fakturnya habis.</p>
	 *
	 * <p><b>Satu langkah legacy sengaja tidak ada di sini.</b> Legacy menaikkan
	 * {@code SpjSalesNota.nilaiTertagih}; model tenant menurunkan angka itu dari alokasinya
	 * sendiri, sehingga menuliskannya akan menciptakan sumber kedua yang bisa berselisih.</p>
	 */
	private static void collectionCreateTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject request, JSONObject hasil, Long customerId,
			BigDecimal nominal, String metode, String kodeUnik, JSONArray alokasi)
			throws Exception {
		String skema = SalesInventoryReceivableTenant.skema(ctx);
		String oleh = tbmuser == null || tbmuser.getUserId() == null ? "" : tbmuser.getUserId();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			java.sql.PreparedStatement psCek = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.cariPenerimaanByKunci(skema));
			psCek.setString(1, kodeUnik);
			java.sql.ResultSet rsCek = psCek.executeQuery();
			boolean sudah = rsCek.next();
			long idSudah = sudah ? rsCek.getLong(1) : 0;
			String nomorSudah = sudah ? str(rsCek.getString(2)) : "";
			rsCek.close();
			psCek.close();
			if (sudah) {
				hasil.put("status", "00");
				hasil.put("id", idSudah);
				hasil.put("nomor", nomorSudah);
				hasil.put("idempotentReplay", true);
				return;
			}
			if (!adaTenant(session, skema, "customer", customerId)) {
				tolak(hasil, "Customer tidak ditemukan.");
				return;
			}
			double jumlahAlokasi = 0;
			for (int i = 0; i < alokasi.length(); i++) {
				jumlahAlokasi += alokasi.getJSONObject(i).optDouble("nominal", 0);
			}
			if (Math.abs(jumlahAlokasi - nominal.doubleValue()) > 0.01) {
				tolak(hasil, "Total alokasi (" + jumlahAlokasi
						+ ") harus sama dengan nominal penerimaan (" + nominal + ").");
				return;
			}
			Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
			Long tripId = optLong(request, "trip_session_id");

			tx = session.beginTransaction();
			Long spjId = null;
			if (tripId != null) {
				java.sql.PreparedStatement psT = session.connection().prepareStatement(
						SalesInventoryReceivableTenant.tripUntukPenerimaan(skema));
				psT.setLong(1, tripId.longValue());
				java.sql.ResultSet rsT = psT.executeQuery();
				boolean adaTrip = rsT.next();
				String statusTrip = adaTrip ? str(rsT.getString(1)) : "";
				if (adaTrip && rsT.getObject(2) != null) {
					spjId = Long.valueOf(rsT.getLong(2));
				}
				rsT.close();
				psT.close();
				if (!adaTrip || NotaSalesSession.STATUS_CLOSED.equals(statusTrip)) {
					tx.rollback();
					tolak(hasil, "Sesi Nota Sales tidak ditemukan / sudah ditutup.");
					return;
				}
			}

			// Kunci tiap faktur lalu periksa sisanya -- keduanya di dalam transaksi.
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
						SalesInventoryReceivableTenant.kunciPiutang(skema));
				lock.setLong(1, did);
				lock.setLong(2, customerId.longValue());
				java.sql.ResultSet rsLock = lock.executeQuery();
				boolean ada = rsLock.next();
				rsLock.close();
				lock.close();
				if (!ada) {
					tx.rollback();
					tolak(hasil, "Faktur piutang " + did
							+ " tidak ditemukan / bukan milik customer ini.");
					return;
				}
				double sisa = sisaPiutangTenant(session, skema, did);
				if (n > sisa + 0.01) {
					tx.rollback();
					tolak(hasil, "Alokasi " + n + " melebihi outstanding faktur " + did + " ("
							+ sisa + ").");
					return;
				}
			}

			java.sql.PreparedStatement ins = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.sisipPenerimaan(skema),
					java.sql.Statement.RETURN_GENERATED_KEYS);
			// Nomor sementara memakai kunci idempotensinya: sudah unik per permintaan.
			ins.setString(1, kodeUnik);
			ins.setLong(2, customerId.longValue());
			if (salesId == null) {
				ins.setNull(3, java.sql.Types.BIGINT);
			} else {
				ins.setLong(3, salesId.longValue());
			}
			ins.setString(4, metode);
			ins.setString(5, request.optString("no_bg", "").trim());
			ins.setString(6, request.optString("nama_bank", "").trim());
			java.util.Date tglBg = optTanggal(request, "tanggal_bg");
			if (tglBg == null) {
				ins.setNull(7, java.sql.Types.DATE);
			} else {
				ins.setDate(7, new java.sql.Date(tglBg.getTime()));
			}
			ins.setBigDecimal(8, nominal);
			ins.setString(9, request.optString("keterangan", "").trim());
			ins.setString(10, kodeUnik);
			if (tripId == null) {
				ins.setNull(11, java.sql.Types.BIGINT);
			} else {
				ins.setLong(11, tripId.longValue());
			}
			ins.setString(12, oleh);
			ins.executeUpdate();
			long terimaId = 0;
			java.sql.ResultSet gk = ins.getGeneratedKeys();
			if (gk.next()) {
				terimaId = gk.getLong(1);
			}
			gk.close();
			ins.close();
			if (terimaId <= 0) {
				tx.rollback();
				tolak(hasil, "Penerimaan gagal disimpan.");
				return;
			}
			String nomor = fmtNomor("KWT", ctx.tokoId, Long.valueOf(terimaId));
			java.sql.PreparedStatement psN = session.connection().prepareStatement(
					SalesInventoryReceivableTenant.finalisasiNomorPenerimaan(skema));
			psN.setString(1, nomor);
			psN.setLong(2, terimaId);
			psN.executeUpdate();
			psN.close();

			for (int i = 0; i < alokasi.length(); i++) {
				JSONObject a = alokasi.getJSONObject(i);
				java.sql.PreparedStatement psA = session.connection().prepareStatement(
						SalesInventoryReceivableTenant.sisipAlokasiPenerimaan(skema));
				psA.setLong(1, terimaId);
				psA.setLong(2, a.optLong("piutang_id"));
				psA.setBigDecimal(3, new BigDecimal(String.valueOf(a.optDouble("nominal"))));
				psA.setString(4, oleh);
				psA.executeUpdate();
				psA.close();
			}

			if (tripId != null) {
				if (PenerimaanPiutangCustomer.METODE_TUNAI.equals(metode)) {
					// Kas yang dipegang sales naik. Bertanda POSITIF.
					java.sql.PreparedStatement kas = session.connection().prepareStatement(
							SalesInventoryTripTenant.sisipKas(skema));
					try {
						kas.setLong(1, tripId.longValue());
						kas.setString(2, ais.service.tenant.TenantKasTrip.COLLECTION_CASH);
						kas.setBigDecimal(3, nominal);
						kas.setString(4, nomor);
						kas.setString(5, "Penagihan tunai");
						kas.setString(6, "KAS-KWT-" + terimaId);
						kas.setString(7, oleh);
						kas.executeUpdate();
					} finally {
						kas.close();
					}
				}
				if (spjId != null) {
					// HANYA statusnya. Nilai tertagihnya diturunkan dari alokasi.
					for (int i = 0; i < alokasi.length(); i++) {
						long did = alokasi.getJSONObject(i).optLong("piutang_id");
						double sisa = sisaPiutangTenant(session, skema, did);
						java.sql.PreparedStatement psS = session.connection().prepareStatement(
								SalesInventoryReceivableTenant.ubahStatusNotaBawaan(skema));
						try {
							psS.setString(1, sisa <= 0.009 ? SpjSalesNota.STATUS_PAID
									: SpjSalesNota.STATUS_PARTIAL);
							psS.setLong(2, spjId.longValue());
							psS.setLong(3, did);
							psS.executeUpdate();
						} finally {
							psS.close();
						}
					}
				}
			}

			// Order asal menjadi LUNAS bila fakturnya kini habis.
			for (int i = 0; i < alokasi.length(); i++) {
				long did = alokasi.getJSONObject(i).optLong("piutang_id");
				if (sisaPiutangTenant(session, skema, did) > 0.009) {
					continue;
				}
				Long orderId = orderDariPiutangTenant(session, skema, did);
				if (orderId == null) {
					continue;
				}
				java.sql.PreparedStatement psO = session.connection().prepareStatement(
						SalesInventoryReceivableTenant.ubahStatusOrderLunas(skema));
				try {
					psO.setString(1, oleh);
					psO.setLong(2, orderId.longValue());
					psO.executeUpdate();
				} finally {
					psO.close();
				}
			}

			SalesInventoryAudit.catatBaru(session, ctx, "penagihan_simpan", skema,
					"penerimaan_piutang", Long.valueOf(terimaId));
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", terimaId);
			hasil.put("nomor", nomor);
		} catch (java.sql.SQLException dup) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			if (dup.getSQLState() != null && dup.getSQLState().startsWith("23")) {
				hasil.put("status", "00");
				hasil.put("idempotentReplay", true);
			} else {
				throw dup;
			}
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception ignore) {
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Sisa satu dokumen piutang pada schema tenant; dihitung dari alokasinya. */
	private static double sisaPiutangTenant(Session session, String skema, long piutangId)
			throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				SalesInventoryReceivableTenant.sisaSatuPiutang(skema));
		try {
			ps.setLong(1, piutangId);
			java.sql.ResultSet rs = ps.executeQuery();
			double sisa = rs.next() ? rs.getDouble(1) : -1;
			rs.close();
			return sisa;
		} finally {
			ps.close();
		}
	}

	/** Sales order asal satu dokumen piutang, lewat fakturnya; {@code null} bila tidak ada. */
	private static Long orderDariPiutangTenant(Session session, String skema, long piutangId)
			throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				SalesInventoryReceivableTenant.orderDariPiutang(skema));
		try {
			ps.setLong(1, piutangId);
			java.sql.ResultSet rs = ps.executeQuery();
			Long id = null;
			if (rs.next() && rs.getObject(1) != null) {
				id = Long.valueOf(rs.getLong(1));
			}
			rs.close();
			return id;
		} finally {
			ps.close();
		}
	}
}
