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
import ais.database.model.inventory.PengadaanFaktur;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.KategoriBiayaSales;
import ais.database.model.koperasi.NotaSalesBiaya;
import ais.database.model.koperasi.NotaSalesKas;
import ais.database.model.koperasi.NotaSalesPembelian;
import ais.database.model.koperasi.NotaSalesSession;
import ais.database.model.koperasi.PiutangCustomerDoc;
import ais.database.model.koperasi.SalesInventory;
import ais.database.model.koperasi.SpjSalesBarang;
import ais.database.model.koperasi.SpjSalesNota;
import ais.database.model.koperasi.SuratPerintahSalesJalan;
import ais.database.model.library.Penyedia;

/**
 * <h3>SPJ + Sesi Nota Sales (layar legacy 39-42 + TRIP-001..008) -- varian Inventory &amp; Sales.</h3>
 *
 * <p>SPJ = perintah jalan (assignment barang dibawa + nota/invoice dibawa, state machine ERD
 * &sect;6); Sesi = realisasinya (satu per SPJ): penagihan (reuse {@code si_collection_create}
 * ber-{@code trip_session_id}), biaya berkategori configurable, kulakan dalam sesi (link faktur
 * Kulakan existing), ledger kas append-only, penutupan ber-approval Pemilik/Admin dgn DUA rumus
 * terpisah (hasil bersih &amp; rekonsiliasi kas, ERD &sect;4).</p>
 *
 * <p>D-14: pergerakan stok mobil dicatat penuh di ledger SPJ; integrasi pemotongan stok toko ke
 * formula stok POS menunggu keputusan UAT (kebijakan risiko).</p>
 */
public final class SalesInventoryTripHelper {

	private SalesInventoryTripHelper() {
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
			// entity tanpa kolom oleh (detail) -- abaikan senyap.
		}
	}

	private static boolean aktorSales(EbisnisActorContextResolver.ActorContext ctx) {
		return EbisnisActorContextResolver.ACTOR_SALES.equals(ctx.actorType);
	}

	private static boolean pemilikAtauAdmin(EbisnisActorContextResolver.ActorContext ctx) {
		return ctx.admin || EbisnisActorContextResolver.ACTOR_PEMILIK.equals(ctx.actorType);
	}

	private static boolean bolehSentuhSpj(EbisnisActorContextResolver.ActorContext ctx,
			SuratPerintahSalesJalan spj) {
		if (!aktorSales(ctx)) {
			return true;
		}
		return spj.getSales() != null && ctx.salesId != null
				&& ctx.salesId.equals(spj.getSales().getId());
	}

	private static String fmtNomor(String prefix, Long tokoId, Long id) {
		return prefix + "-" + (tokoId == null ? 0 : tokoId.longValue()) + "-"
				+ String.format("%06d", new Object[] { id });
	}

	/** Tulis satu baris ledger kas sesi (append-only; keluar kas = nominal NEGATIF). */
	private static void catatKas(Session session, NotaSalesSession sesi, String jenis,
			BigDecimal nominal, String referensi, String keterangan) {
		NotaSalesKas kas = new NotaSalesKas();
		kas.setSesi(sesi);
		kas.setJenis(jenis);
		kas.setNominal(nominal);
		kas.setReferensi(referensi);
		kas.setKeterangan(keterangan);
		session.save(kas);
	}

	// =============================================================================================
	// SCR-39: SPJ (si_spj_create/update/list/detail/status/nota_assign/barang_muat)
	// =============================================================================================

	public static void spjSimpan(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil, boolean update) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!ctx.bolehAksi("surat_perintah_sales", update ? "update" : "create")) {
			tolak(hasil, "Akun Anda tidak berhak " + (update ? "mengubah" : "membuat") + " SPJ.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			SuratPerintahSalesJalan spj;
			if (update) {
				Long id = optLong(request, "spj_id");
				spj = id == null ? null : (SuratPerintahSalesJalan) session.get(SuratPerintahSalesJalan.class, id);
				if (spj == null) {
					tolak(hasil, "SPJ tidak ditemukan.");
					return;
				}
				String st = spj.getStatus();
				if (!SuratPerintahSalesJalan.STATUS_DRAFT.equals(st)
						&& !SuratPerintahSalesJalan.STATUS_SUBMITTED.equals(st)) {
					tolak(hasil, "SPJ berstatus " + st + " tidak bisa diubah (hanya DRAFT/SUBMITTED).");
					return;
				}
				if (!bolehSentuhSpj(ctx, spj)) {
					tolak(hasil, "SPJ ini bukan milik sales Anda.");
					return;
				}
			} else {
				String kodeUnik = request.optString("kode_unik", "").trim();
				if (!kodeUnik.isEmpty()) {
					SuratPerintahSalesJalan sudah = (SuratPerintahSalesJalan) session
							.createCriteria(SuratPerintahSalesJalan.class)
							.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
					if (sudah != null) {
						hasil.put("status", "00");
						hasil.put("id", sudah.getId());
						hasil.put("nomor", str(sudah.getNomor()));
						hasil.put("idempotentReplay", true);
						return;
					}
				}
				Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
				if (salesId == null) {
					tolak(hasil, "sales_id wajib diisi (aktor sales: profil sales aktif wajib ada).");
					return;
				}
				SalesInventory sales = (SalesInventory) session.get(SalesInventory.class, salesId);
				if (sales == null) {
					tolak(hasil, "Profil sales tidak ditemukan.");
					return;
				}
				Long tokoId = ctx.tokoId != null ? ctx.tokoId
						: (sales.getToko() == null ? null : sales.getToko().getId());
				if (ctx.admin && optLong(request, "toko_id") != null) {
					tokoId = optLong(request, "toko_id");
				}
				Toko toko = tokoId == null ? null : (Toko) session.get(Toko.class, tokoId);
				if (toko == null) {
					tolak(hasil, "Scope toko tidak dapat ditentukan.");
					return;
				}
				spj = new SuratPerintahSalesJalan();
				spj.setToko(toko);
				spj.setSales(sales);
				spj.setStatus(SuratPerintahSalesJalan.STATUS_DRAFT);
				spj.setKodeUnik(kodeUnik.isEmpty() ? null : kodeUnik);
				spj.setDibuatOleh(tbmuser);
			}
			Date rencana = optTanggal(request, "tanggal_berangkat_rencana");
			if (rencana != null) {
				spj.setTanggalBerangkatRencana(rencana);
			}
			if (!request.isNull("rute")) spj.setRute(request.optString("rute", "").trim());
			if (!request.isNull("kendaraan")) spj.setKendaraan(request.optString("kendaraan", "").trim());
			if (!request.isNull("catatan")) spj.setCatatan(request.optString("catatan", "").trim());
			BigDecimal uangMuka = optBigDecimal(request, "uang_muka_operasional");
			if (uangMuka != null) {
				if (uangMuka.signum() < 0) {
					tolak(hasil, "uang_muka_operasional tidak boleh negatif.");
					return;
				}
				spj.setUangMukaOperasional(uangMuka);
			}
			isiOleh(spj, tbmuser);
			tx = session.beginTransaction();
			if (!update) {
				session.save(spj);
				session.flush();
				spj.setNomor(fmtNomor("SPJ", spj.getToko().getId(), spj.getId()));
			}
			// Barang dibawa (bulk, replace) -- boleh dikirim di create maupun update.
			JSONArray barang = request.optJSONArray("barang");
			if (barang != null) {
				List lama = session.createCriteria(SpjSalesBarang.class)
						.add(Restrictions.eq("spj", spj)).list();
				for (int i = 0; i < lama.size(); i++) {
					session.delete(lama.get(i));
				}
				for (int i = 0; i < barang.length(); i++) {
					JSONObject b = barang.getJSONObject(i);
					Long produkId = optLong(b, "produk_id");
					BigDecimal qty = optBigDecimal(b, "qty_rencana");
					if (produkId == null || qty == null || qty.signum() <= 0) {
						tx.rollback();
						tolak(hasil, "Baris barang ke-" + (i + 1) + " tidak valid (produk_id/qty_rencana>0).");
						return;
					}
					Produk p = (Produk) session.get(Produk.class, produkId);
					if (p == null) {
						tx.rollback();
						tolak(hasil, "Produk " + produkId + " tidak ditemukan.");
						return;
					}
					SpjSalesBarang baris = new SpjSalesBarang();
					baris.setSpj(spj);
					baris.setProduk(p);
					baris.setNamaProduk(str(p.getNama()));
					baris.setQtyRencana(qty);
					double hpp = p.getHargaBeli() == null ? 0 : p.getHargaBeli().doubleValue();
					double hj = p.getHargaJual() == null ? 0 : p.getHargaJual().doubleValue();
					baris.setHppSnapshot(new BigDecimal(String.valueOf(hpp)));
					baris.setHargaJualSnapshot(new BigDecimal(String.valueOf(hj)));
					baris.setStatus(SpjSalesBarang.STATUS_PLANNED);
					session.save(baris);
				}
			}
			session.saveOrUpdate(spj);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", spj.getId());
			hasil.put("nomor", str(spj.getNomor()));
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

	public static void spjList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("surat_perintah_sales")) {
			tolak(hasil, "Menu Surat Perintah Sales tidak aktif untuk akun Anda.");
			return;
		}
		String status = request.optString("status", "").trim().toUpperCase();
		Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
		if (aktorSales(ctx) && salesId == null) {
			tolak(hasil, "Akun sales Anda belum punya profil sales aktif.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			boolean jalurTenant = SalesInventoryTripTenant.aktif(ctx);
			String skemaTenant = jalurTenant ? SalesInventoryTripTenant.skema(ctx) : null;
			StringBuilder where = new StringBuilder(" WHERE 1=1");
			if (!status.isEmpty()) where.append(" AND j.status = ?");
			if (salesId != null) {
				where.append(jalurTenant
						? " AND " + SalesInventoryTripTenant.kolomSalesSpj() + " = ?"
						: " AND j.sales = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) {
				// Tabel Trip tenant berlingkup gudang; toko ditegakkan lewat gudang.toko_id.
				where.append(jalurTenant
						? SalesInventoryTripTenant.syaratToko(skemaTenant, "j.gudang_id")
						: " AND j.toko = ?");
			}
			String sqlSpj;
			if (jalurTenant) {
				sqlSpj = SalesInventoryTripTenant.selectSpj(skemaTenant)
						+ SalesInventoryTripTenant.dasarSpj(skemaTenant, where.toString());
			} else {
				sqlSpj = "SELECT j.id, j.nomor, j.status, j.tanggal_berangkat_rencana, j.rute, j.kendaraan,"
						+ " j.uang_muka_operasional, s.id, s.nama,"
						+ " (SELECT COUNT(*) FROM koperasi.spj_sales_barang b WHERE b.spj = j.id),"
						+ " (SELECT COUNT(*) FROM koperasi.spj_sales_nota n WHERE n.spj = j.id),"
						+ " (SELECT ns.id FROM koperasi.nota_sales_session ns WHERE ns.spj = j.id LIMIT 1)"
						+ " FROM koperasi.surat_perintah_sales_jalan j"
						+ " JOIN koperasi.sales_inventory s ON j.sales = s.id" + where
						+ " ORDER BY j.id DESC LIMIT 100";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sqlSpj);
			int ix = 1;
			if (!status.isEmpty()) ps.setString(ix++, status);
			if (salesId != null) ps.setLong(ix++, salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) ps.setLong(ix++, ctx.tokoId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray rows = new JSONArray();
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("id", rs.getLong(1));
				r.put("nomor", str(rs.getString(2)));
				r.put("status", str(rs.getString(3)));
				r.put("tanggalBerangkat", str(rs.getTimestamp(4)));
				r.put("rute", str(rs.getString(5)));
				r.put("kendaraan", str(rs.getString(6)));
				r.put("uangMuka", rs.getDouble(7));
				r.put("salesId", rs.getLong(8));
				r.put("salesNama", str(rs.getString(9)));
				r.put("jumlahBarang", rs.getLong(10));
				r.put("jumlahNota", rs.getLong(11));
				long sesiId = rs.getLong(12);
				r.put("sessionId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(sesiId));
				rows.put(r);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("rows", rows);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void spjDetail(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!ctx.bolehMenu("surat_perintah_sales") && !ctx.bolehMenu("nota_sales")) {
			tolak(hasil, "Menu SPJ/Nota Sales tidak aktif untuk akun Anda.");
			return;
		}
		Long id = optLong(request, "spj_id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			SuratPerintahSalesJalan spj = id == null ? null
					: (SuratPerintahSalesJalan) session.get(SuratPerintahSalesJalan.class, id);
			if (spj == null) {
				tolak(hasil, "SPJ tidak ditemukan.");
				return;
			}
			if (!bolehSentuhSpj(ctx, spj)) {
				tolak(hasil, "SPJ ini bukan milik sales Anda.");
				return;
			}
			hasil.put("status", "00");
			hasil.put("data", jsonSpj(session, spj));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static JSONObject jsonSpj(Session session, SuratPerintahSalesJalan spj) throws Exception {
		JSONObject j = new JSONObject();
		j.put("id", spj.getId());
		j.put("nomor", str(spj.getNomor()));
		j.put("status", spj.getStatus());
		j.put("tanggalBerangkat", str(spj.getTanggalBerangkatRencana()));
		j.put("tanggalMulaiAktual", spj.getTanggalMulaiAktual() == null ? "" : str(spj.getTanggalMulaiAktual()));
		j.put("tanggalKembaliAktual", spj.getTanggalKembaliAktual() == null ? "" : str(spj.getTanggalKembaliAktual()));
		j.put("rute", str(spj.getRute()));
		j.put("kendaraan", str(spj.getKendaraan()));
		j.put("uangMuka", spj.getUangMukaOperasional().doubleValue());
		j.put("catatan", str(spj.getCatatan()));
		j.put("alasanBatal", str(spj.getAlasanBatal()));
		j.put("salesId", spj.getSales().getId());
		j.put("salesNama", str(spj.getSales().getNama()));
		j.put("disetujuiOleh", spj.getDisetujuiOleh() == null ? "" : str(spj.getDisetujuiOleh().getUserId()));
		List barang = session.createCriteria(SpjSalesBarang.class)
				.add(Restrictions.eq("spj", spj)).addOrder(Order.asc("id")).list();
		JSONArray arrB = new JSONArray();
		for (int i = 0; i < barang.size(); i++) {
			SpjSalesBarang b = (SpjSalesBarang) barang.get(i);
			JSONObject r = new JSONObject();
			r.put("id", b.getId());
			r.put("produkId", b.getProduk().getId());
			r.put("namaProduk", str(b.getNamaProduk()));
			r.put("qtyRencana", b.getQtyRencana().doubleValue());
			r.put("qtyDimuat", b.getQtyDimuat().doubleValue());
			r.put("qtyTerjual", b.getQtyTerjual().doubleValue());
			r.put("qtyKembali", b.getQtyKembali().doubleValue());
			r.put("qtyRusak", b.getQtyRusak().doubleValue());
			r.put("qtyHilang", b.getQtyHilang().doubleValue());
			double masih = b.getQtyDimuat().doubleValue() - b.getQtyTerjual().doubleValue()
					- b.getQtyKembali().doubleValue() - b.getQtyRusak().doubleValue()
					- b.getQtyHilang().doubleValue();
			r.put("masihDibawa", masih);
			r.put("hargaJual", b.getHargaJualSnapshot().doubleValue());
			r.put("status", b.getStatus());
			r.put("alasanSelisih", str(b.getAlasanSelisih()));
			arrB.put(r);
		}
		j.put("barang", arrB);
		List nota = session.createCriteria(SpjSalesNota.class)
				.add(Restrictions.eq("spj", spj)).addOrder(Order.asc("id")).list();
		JSONArray arrN = new JSONArray();
		for (int i = 0; i < nota.size(); i++) {
			SpjSalesNota n = (SpjSalesNota) nota.get(i);
			JSONObject r = new JSONObject();
			r.put("id", n.getId());
			r.put("piutangDocId", n.getPiutangDoc().getId());
			r.put("fakturNomor", str(n.getPiutangDoc().getNomor()));
			r.put("customerNama", str(n.getCustomer().getNama()));
			r.put("nilaiAwal", n.getNilaiAwal().doubleValue());
			r.put("saldoSaatAssign", n.getSaldoSaatAssign().doubleValue());
			r.put("jatuhTempo", n.getJatuhTempo() == null ? "" : str(n.getJatuhTempo()));
			r.put("status", n.getStatus());
			r.put("hasilKunjungan", str(n.getHasilKunjungan()));
			r.put("janjiBayar", n.getJanjiBayar() == null ? "" : str(n.getJanjiBayar()));
			r.put("alasanGagal", str(n.getAlasanGagal()));
			r.put("nilaiTertagih", n.getNilaiTertagih().doubleValue());
			arrN.put(r);
		}
		j.put("nota", arrN);
		NotaSalesSession sesi = (NotaSalesSession) session.createCriteria(NotaSalesSession.class)
				.add(Restrictions.eq("spj", spj)).setMaxResults(1).uniqueResult();
		j.put("sessionId", sesi == null ? JSONObject.NULL : sesi.getId());
		j.put("sessionStatus", sesi == null ? "" : sesi.getStatus());
		return j;
	}

	public static void spjStatus(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!ctx.bolehAksi("surat_perintah_sales", "update")) {
			tolak(hasil, "Akun Anda tidak berhak mengubah status SPJ.");
			return;
		}
		Long id = optLong(request, "spj_id");
		String baru = request.optString("status", "").trim().toUpperCase();
		String alasan = request.optString("alasan", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			SuratPerintahSalesJalan spj = id == null ? null
					: (SuratPerintahSalesJalan) session.get(SuratPerintahSalesJalan.class, id);
			if (spj == null) {
				tolak(hasil, "SPJ tidak ditemukan.");
				return;
			}
			if (!bolehSentuhSpj(ctx, spj)) {
				tolak(hasil, "SPJ ini bukan milik sales Anda.");
				return;
			}
			String lama = spj.getStatus();
			boolean boleh;
			if (SuratPerintahSalesJalan.STATUS_SUBMITTED.equals(baru)) {
				boleh = SuratPerintahSalesJalan.STATUS_DRAFT.equals(lama);
			} else if (SuratPerintahSalesJalan.STATUS_APPROVED.equals(baru)) {
				// Approval = wewenang Pemilik/Admin (ERD: approval eksplisit).
				if (!pemilikAtauAdmin(ctx)) {
					tolak(hasil, "Approval SPJ hanya oleh Pemilik/Admin.");
					return;
				}
				boleh = SuratPerintahSalesJalan.STATUS_SUBMITTED.equals(lama);
				if (boleh) {
					spj.setDisetujuiOleh(tbmuser);
				}
			} else if (SuratPerintahSalesJalan.STATUS_CANCELLED.equals(baru)) {
				boolean sebelumJalan = SuratPerintahSalesJalan.STATUS_DRAFT.equals(lama)
						|| SuratPerintahSalesJalan.STATUS_SUBMITTED.equals(lama)
						|| SuratPerintahSalesJalan.STATUS_APPROVED.equals(lama);
				boleh = sebelumJalan;
				if (boleh && SuratPerintahSalesJalan.STATUS_APPROVED.equals(lama) && alasan.isEmpty()) {
					tolak(hasil, "Membatalkan SPJ APPROVED wajib menyertakan alasan.");
					return;
				}
			} else {
				tolak(hasil, "Status " + baru + " tidak dikelola aksi ini (mulai jalan lewat si_trip_start).");
				return;
			}
			if (!boleh) {
				tolak(hasil, "Transisi " + lama + " -> " + baru + " tidak diizinkan.");
				return;
			}
			tx = session.beginTransaction();
			spj.setStatus(baru);
			if (!alasan.isEmpty()) spj.setAlasanBatal(alasan);
			isiOleh(spj, tbmuser);
			session.saveOrUpdate(spj);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("statusBaru", baru);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Bulk assign nota/invoice piutang ke SPJ -- satu invoice tidak boleh dibawa dua SPJ aktif. */
	public static void spjNotaAssign(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!ctx.bolehAksi("surat_perintah_sales", "update")) {
			tolak(hasil, "Akun Anda tidak berhak mengatur nota dibawa.");
			return;
		}
		Long id = optLong(request, "spj_id");
		JSONArray ids = request.optJSONArray("piutang_doc_ids");
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			SuratPerintahSalesJalan spj = id == null ? null
					: (SuratPerintahSalesJalan) session.get(SuratPerintahSalesJalan.class, id);
			if (spj == null) {
				tolak(hasil, "SPJ tidak ditemukan.");
				return;
			}
			String st = spj.getStatus();
			if (!SuratPerintahSalesJalan.STATUS_DRAFT.equals(st)
					&& !SuratPerintahSalesJalan.STATUS_SUBMITTED.equals(st)
					&& !SuratPerintahSalesJalan.STATUS_APPROVED.equals(st)) {
				tolak(hasil, "Nota hanya bisa diatur sebelum berangkat (status sekarang " + st + ").");
				return;
			}
			if (!bolehSentuhSpj(ctx, spj)) {
				tolak(hasil, "SPJ ini bukan milik sales Anda.");
				return;
			}
			tx = session.beginTransaction();
			// Replace-all assignment (dokumen belum jalan, aman diganti utuh).
			List lama = session.createCriteria(SpjSalesNota.class)
					.add(Restrictions.eq("spj", spj)).list();
			for (int i = 0; i < lama.size(); i++) {
				session.delete(lama.get(i));
			}
			session.flush();
			int dibuat = 0;
			if (ids != null) {
				for (int i = 0; i < ids.length(); i++) {
					long did = ids.optLong(i, -1);
					PiutangCustomerDoc doc = did <= 0 ? null
							: (PiutangCustomerDoc) session.get(PiutangCustomerDoc.class, Long.valueOf(did));
					if (doc == null || !PiutangCustomerDoc.STATUS_AKTIF.equals(doc.getStatus())) {
						tx.rollback();
						tolak(hasil, "Faktur piutang " + did + " tidak ditemukan/tidak aktif.");
						return;
					}
					// Invariant ERD 3.4: satu invoice tidak dibawa dua SPJ aktif sekaligus.
					java.sql.PreparedStatement cek = session.connection().prepareStatement(
							"SELECT COUNT(*) FROM koperasi.spj_sales_nota n"
									+ " JOIN koperasi.surat_perintah_sales_jalan j ON n.spj = j.id"
									+ " WHERE n.piutang_doc = ? AND j.id <> ?"
									+ " AND j.status NOT IN ('CLOSED','CANCELLED')");
					cek.setLong(1, did);
					cek.setLong(2, spj.getId().longValue());
					java.sql.ResultSet rsCek = cek.executeQuery();
					long dobel = rsCek.next() ? rsCek.getLong(1) : 0;
					rsCek.close(); cek.close();
					if (dobel > 0) {
						tx.rollback();
						tolak(hasil, "Faktur " + str(doc.getNomor()) + " sedang dibawa SPJ lain yang belum selesai.");
						return;
					}
					java.sql.PreparedStatement out = session.connection().prepareStatement(
							"SELECT (COALESCE(d.total_faktur,0) - COALESCE(d.dibayar_awal,0) -"
									+ " COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_penerimaan_piutang_customer a"
									+ " WHERE a.piutang_doc = d.id),0)) FROM koperasi.piutang_customer_doc d WHERE d.id = ?");
					out.setLong(1, did);
					java.sql.ResultSet rsOut = out.executeQuery();
					double outstanding = rsOut.next() ? rsOut.getDouble(1) : 0;
					rsOut.close(); out.close();
					SpjSalesNota n = new SpjSalesNota();
					n.setSpj(spj);
					n.setPiutangDoc(doc);
					n.setCustomer(doc.getCustomer());
					n.setNilaiAwal(doc.getTotalFaktur());
					n.setSaldoSaatAssign(new BigDecimal(String.valueOf(outstanding)));
					n.setJatuhTempo(doc.getJatuhTempo());
					n.setStatus(SpjSalesNota.STATUS_ASSIGNED);
					session.save(n);
					dibuat++;
				}
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("jumlahNota", dibuat);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-40: Sesi (si_trip_start/list/detail/nota_result/barang_update/cash_sale/deposit/
	//          return/reconcile/close) + biaya (si_expense_*) + kulakan sesi (si_trip_purchase_link)
	// =============================================================================================

	public static void tripStart(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!ctx.bolehAksi("nota_sales", "create") && !ctx.bolehAksi("surat_perintah_sales", "update")) {
			tolak(hasil, "Akun Anda tidak berhak memulai sesi.");
			return;
		}
		Long id = optLong(request, "spj_id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			SuratPerintahSalesJalan spj = id == null ? null
					: (SuratPerintahSalesJalan) session.get(SuratPerintahSalesJalan.class, id);
			if (spj == null) {
				tolak(hasil, "SPJ tidak ditemukan.");
				return;
			}
			if (!bolehSentuhSpj(ctx, spj)) {
				tolak(hasil, "SPJ ini bukan milik sales Anda.");
				return;
			}
			NotaSalesSession sudah = (NotaSalesSession) session.createCriteria(NotaSalesSession.class)
					.add(Restrictions.eq("spj", spj)).setMaxResults(1).uniqueResult();
			if (sudah != null) {
				hasil.put("status", "00");
				hasil.put("sessionId", sudah.getId());
				hasil.put("idempotentReplay", true);
				return;
			}
			if (!SuratPerintahSalesJalan.STATUS_APPROVED.equals(spj.getStatus())) {
				tolak(hasil, "Hanya SPJ APPROVED yang bisa mulai jalan (status: " + spj.getStatus() + ").");
				return;
			}
			tx = session.beginTransaction();
			NotaSalesSession sesi = new NotaSalesSession();
			sesi.setSpj(spj);
			sesi.setStatus(NotaSalesSession.STATUS_ACTIVE);
			sesi.setWaktuMulai(ais.ui.util.WaktuUtil.getDate());
			sesi.setSaldoKasAwal(spj.getUangMukaOperasional());
			isiOleh(sesi, tbmuser);
			session.save(sesi);
			session.flush();
			sesi.setNomor(fmtNomor("NSS", spj.getToko().getId(), sesi.getId()));
			session.saveOrUpdate(sesi);
			if (spj.getUangMukaOperasional().signum() > 0) {
				catatKas(session, sesi, NotaSalesKas.JENIS_OPENING, spj.getUangMukaOperasional(),
						str(spj.getNomor()), "Uang muka operasional keberangkatan");
			}
			spj.setStatus(SuratPerintahSalesJalan.STATUS_ACTIVE);
			spj.setTanggalMulaiAktual(ais.ui.util.WaktuUtil.getDate());
			session.saveOrUpdate(spj);
			// Barang PLANNED -> LOADED (qtyDimuat default = qtyRencana bila belum diisi).
			List barang = session.createCriteria(SpjSalesBarang.class)
					.add(Restrictions.eq("spj", spj)).list();
			for (int i = 0; i < barang.size(); i++) {
				SpjSalesBarang b = (SpjSalesBarang) barang.get(i);
				if (b.getQtyDimuat().signum() <= 0) {
					b.setQtyDimuat(b.getQtyRencana());
				}
				b.setStatus(SpjSalesBarang.STATUS_LOADED);
				session.saveOrUpdate(b);
			}
			// Nota ASSIGNED -> CARRIED.
			List nota = session.createCriteria(SpjSalesNota.class)
					.add(Restrictions.eq("spj", spj)).list();
			for (int i = 0; i < nota.size(); i++) {
				SpjSalesNota n = (SpjSalesNota) nota.get(i);
				n.setStatus(SpjSalesNota.STATUS_CARRIED);
				session.saveOrUpdate(n);
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("sessionId", sesi.getId());
			hasil.put("nomor", str(sesi.getNomor()));
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void tripList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		if (!ctx.bolehMenu("nota_sales")) {
			tolak(hasil, "Menu Nota Sales tidak aktif untuk akun Anda.");
			return;
		}
		String status = request.optString("status", "").trim().toUpperCase();
		Long salesId = aktorSales(ctx) ? ctx.salesId : optLong(request, "sales_id");
		if (aktorSales(ctx) && salesId == null) {
			tolak(hasil, "Akun sales Anda belum punya profil sales aktif.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1");
			boolean jalurTenant = SalesInventoryTripTenant.aktif(ctx);
			String skemaTenant = jalurTenant ? SalesInventoryTripTenant.skema(ctx) : null;
			if (!status.isEmpty()) where.append(" AND ns.status = ?");
			if (salesId != null) {
				where.append(jalurTenant
						? " AND " + SalesInventoryTripTenant.kolomSalesTrip() + " = ?"
						: " AND j.sales = ?");
			}
			if (ctx.tokoId != null && !ctx.admin) {
				where.append(jalurTenant
						? SalesInventoryTripTenant.syaratToko(skemaTenant, "ns.gudang_id")
						: " AND j.toko = ?");
			}
			String sqlTrip;
			if (jalurTenant) {
				sqlTrip = SalesInventoryTripTenant.selectTrip(skemaTenant)
						+ SalesInventoryTripTenant.dasarTrip(skemaTenant, where.toString());
			} else {
				sqlTrip = "SELECT ns.id, ns.nomor, ns.status, ns.waktu_mulai, ns.waktu_kembali,"
						+ " j.id, j.nomor, s.nama,"
						+ " COALESCE((SELECT SUM(k.nominal) FROM koperasi.nota_sales_kas k WHERE k.sesi = ns.id),0)"
						+ " FROM koperasi.nota_sales_session ns"
						+ " JOIN koperasi.surat_perintah_sales_jalan j ON ns.spj = j.id"
						+ " JOIN koperasi.sales_inventory s ON j.sales = s.id" + where
						+ " ORDER BY ns.id DESC LIMIT 100";
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(sqlTrip);
			int ix = 1;
			if (!status.isEmpty()) ps.setString(ix++, status);
			if (salesId != null) ps.setLong(ix++, salesId.longValue());
			if (ctx.tokoId != null && !ctx.admin) ps.setLong(ix++, ctx.tokoId.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray rows = new JSONArray();
			while (rs.next()) {
				JSONObject r = new JSONObject();
				r.put("id", rs.getLong(1));
				r.put("nomor", str(rs.getString(2)));
				r.put("status", str(rs.getString(3)));
				r.put("waktuMulai", str(rs.getTimestamp(4)));
				r.put("waktuKembali", rs.getTimestamp(5) == null ? "" : str(rs.getTimestamp(5)));
				r.put("spjId", rs.getLong(6));
				r.put("spjNomor", str(rs.getString(7)));
				r.put("salesNama", str(rs.getString(8)));
				r.put("saldoKas", rs.getDouble(9));
				rows.put(r);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("rows", rows);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Detail sesi = SPJ + ledger biaya/pembelian/kas + ringkasan rumus LIVE (SCR-41). */
	public static void tripDetail(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!ctx.bolehMenu("nota_sales")) {
			tolak(hasil, "Menu Nota Sales tidak aktif untuk akun Anda.");
			return;
		}
		Long id = optLong(request, "session_id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			NotaSalesSession sesi = id == null ? null
					: (NotaSalesSession) session.get(NotaSalesSession.class, id);
			if (sesi == null) {
				tolak(hasil, "Sesi tidak ditemukan.");
				return;
			}
			SuratPerintahSalesJalan spj = sesi.getSpj();
			if (!bolehSentuhSpj(ctx, spj)) {
				tolak(hasil, "Sesi ini bukan milik sales Anda.");
				return;
			}
			JSONObject j = new JSONObject();
			j.put("id", sesi.getId());
			j.put("nomor", str(sesi.getNomor()));
			j.put("statusSesi", sesi.getStatus());
			j.put("waktuMulai", sesi.getWaktuMulai() == null ? "" : str(sesi.getWaktuMulai()));
			j.put("waktuKembali", sesi.getWaktuKembali() == null ? "" : str(sesi.getWaktuKembali()));
			j.put("waktuTutup", sesi.getWaktuTutup() == null ? "" : str(sesi.getWaktuTutup()));
			j.put("catatanPenutupan", str(sesi.getCatatanPenutupan()));
			j.put("kasFisikAktual", sesi.getKasFisikAktual() == null ? JSONObject.NULL
					: sesi.getKasFisikAktual().doubleValue());
			j.put("selisihKas", sesi.getSelisihKas() == null ? JSONObject.NULL
					: sesi.getSelisihKas().doubleValue());
			j.put("spj", jsonSpj(session, spj));

			// Biaya sesi
			List biaya = session.createCriteria(NotaSalesBiaya.class)
					.add(Restrictions.eq("sesi", sesi)).addOrder(Order.asc("id")).list();
			JSONArray arrBiaya = new JSONArray();
			double totalBiaya = 0;
			for (int i = 0; i < biaya.size(); i++) {
				NotaSalesBiaya b = (NotaSalesBiaya) biaya.get(i);
				JSONObject r = new JSONObject();
				r.put("id", b.getId());
				r.put("kategori", str(b.getKategori().getNama()));
				r.put("tanggal", str(b.getTanggal()));
				r.put("uraian", str(b.getUraian()));
				r.put("nilai", b.getNilai().doubleValue());
				r.put("metode", b.getMetode());
				r.put("penerima", str(b.getPenerima()));
				r.put("statusDok", b.getStatusDok());
				arrBiaya.put(r);
				totalBiaya += b.getNilai().doubleValue();
			}
			j.put("biaya", arrBiaya);

			// Pembelian sesi
			List beli = session.createCriteria(NotaSalesPembelian.class)
					.add(Restrictions.eq("sesi", sesi)).addOrder(Order.asc("id")).list();
			JSONArray arrBeli = new JSONArray();
			double totalBeliDibayar = 0, totalBeliFaktur = 0, totalBeliSisa = 0;
			for (int i = 0; i < beli.size(); i++) {
				NotaSalesPembelian b = (NotaSalesPembelian) beli.get(i);
				JSONObject r = new JSONObject();
				r.put("id", b.getId());
				r.put("supplierNama", b.getSupplier() == null ? "" : str(b.getSupplier().getNama()));
				r.put("fakturId", b.getPengadaanFaktur() == null ? JSONObject.NULL
						: b.getPengadaanFaktur().getId());
				r.put("totalFaktur", b.getTotalFaktur().doubleValue());
				r.put("dibayarSesi", b.getDibayarSesi().doubleValue());
				r.put("sisaHutang", b.getSisaHutang().doubleValue());
				r.put("tujuanStok", b.getTujuanStok());
				arrBeli.put(r);
				totalBeliDibayar += b.getDibayarSesi().doubleValue();
				totalBeliFaktur += b.getTotalFaktur().doubleValue();
				totalBeliSisa += b.getSisaHutang().doubleValue();
			}
			j.put("pembelian", arrBeli);

			// Ledger kas
			List kas = session.createCriteria(NotaSalesKas.class)
					.add(Restrictions.eq("sesi", sesi)).addOrder(Order.asc("id")).list();
			JSONArray arrKas = new JSONArray();
			double saldoKas = 0, penerimaanTunai = 0, penjualanTunai = 0, biayaTunai = 0,
					pembayaranBeliTunai = 0, setoran = 0, refund = 0;
			for (int i = 0; i < kas.size(); i++) {
				NotaSalesKas k = (NotaSalesKas) kas.get(i);
				JSONObject r = new JSONObject();
				r.put("id", k.getId());
				r.put("jenis", k.getJenis());
				r.put("nominal", k.getNominal().doubleValue());
				r.put("referensi", str(k.getReferensi()));
				r.put("keterangan", str(k.getKeterangan()));
				r.put("waktu", str(k.getWaktu()));
				arrKas.put(r);
				double n = k.getNominal().doubleValue();
				saldoKas += n;
				if (NotaSalesKas.JENIS_COLLECTION_CASH.equals(k.getJenis())) penerimaanTunai += n;
				else if (NotaSalesKas.JENIS_CASH_SALE.equals(k.getJenis())) penjualanTunai += n;
				else if (NotaSalesKas.JENIS_EXPENSE_CASH.equals(k.getJenis())) biayaTunai += -n;
				else if (NotaSalesKas.JENIS_PURCHASE_PAYMENT.equals(k.getJenis())) pembayaranBeliTunai += -n;
				else if (NotaSalesKas.JENIS_OWNER_DEPOSIT.equals(k.getJenis())) setoran += -n;
				else if (NotaSalesKas.JENIS_REFUND.equals(k.getJenis())) refund += n;
			}
			j.put("kas", arrKas);

			// Penerimaan piutang ber-sesi (tunai + non-tunai)
			java.sql.PreparedStatement psP = session.connection().prepareStatement(
					"SELECT COALESCE(SUM(p.nominal),0),"
							+ " COALESCE(SUM(CASE WHEN p.metode = 'TUNAI' THEN p.nominal ELSE 0 END),0)"
							+ " FROM koperasi.penerimaan_piutang_customer p WHERE p.sesi = ?");
			psP.setLong(1, sesi.getId().longValue());
			java.sql.ResultSet rsP = psP.executeQuery();
			double totalTertagih = 0, tertagihTunai = 0;
			if (rsP.next()) {
				totalTertagih = rsP.getDouble(1);
				tertagihTunai = rsP.getDouble(2);
			}
			rsP.close(); psP.close();

			// DUA rumus terpisah (ERD 4.1 & 4.2) -- keduanya SELALU ditampilkan.
			JSONObject rumus = new JSONObject();
			rumus.put("totalPiutangTertagih", totalTertagih);
			rumus.put("tertagihTunai", tertagihTunai);
			rumus.put("tertagihNonTunai", totalTertagih - tertagihTunai);
			rumus.put("totalBiaya", totalBiaya);
			rumus.put("totalNilaiPembelian", totalBeliFaktur);
			rumus.put("pembelianDibayarDp", totalBeliDibayar);
			rumus.put("pembelianSisaHutang", totalBeliSisa);
			rumus.put("hasilBersih", totalTertagih - totalBiaya - totalBeliDibayar);
			rumus.put("uangMukaAwal", sesi.getSaldoKasAwal().doubleValue());
			rumus.put("penjualanTunai", penjualanTunai);
			rumus.put("refundTunai", refund);
			rumus.put("biayaTunai", biayaTunai);
			rumus.put("pembayaranPembelianTunai", pembayaranBeliTunai);
			rumus.put("setoranKePemilik", setoran);
			rumus.put("kasFisikSeharusnya", saldoKas);
			j.put("rumus", rumus);

			hasil.put("status", "00");
			hasil.put("data", j);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Update hasil kunjungan per nota dibawa (SCR-40). */
	public static void tripNotaResult(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!ctx.bolehAksi("nota_sales", "update")) {
			tolak(hasil, "Akun Anda tidak berhak mengubah hasil kunjungan.");
			return;
		}
		Long id = optLong(request, "nota_id");
		String baru = request.optString("status", "").trim().toUpperCase();
		java.util.List<String> sah = java.util.Arrays.asList(SpjSalesNota.STATUS_UNPAID,
				SpjSalesNota.STATUS_PROMISE, SpjSalesNota.STATUS_PARTIAL, SpjSalesNota.STATUS_PAID,
				SpjSalesNota.STATUS_RETURNED, SpjSalesNota.STATUS_DISPUTED, SpjSalesNota.STATUS_LOST);
		if (id == null || !sah.contains(baru)) {
			tolak(hasil, "nota_id dan status hasil kunjungan yang sah wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			SpjSalesNota n = (SpjSalesNota) session.get(SpjSalesNota.class, id);
			if (n == null) {
				tolak(hasil, "Nota dibawa tidak ditemukan.");
				return;
			}
			if (!bolehSentuhSpj(ctx, n.getSpj())) {
				tolak(hasil, "Nota ini bukan milik sales Anda.");
				return;
			}
			if (SpjSalesNota.STATUS_RECONCILED.equals(n.getStatus())) {
				tolak(hasil, "Nota sudah direkonsiliasi -- tidak bisa diubah.");
				return;
			}
			tx = session.beginTransaction();
			n.setStatus(baru);
			n.setHasilKunjungan(request.optString("hasil_kunjungan", "").trim());
			n.setJanjiBayar(optTanggal(request, "janji_bayar"));
			n.setAlasanGagal(request.optString("alasan_gagal", "").trim());
			session.saveOrUpdate(n);
			tx.commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Update kuantitas barang (terjual/kembali/rusak/hilang) -- invariant <= dimuat. */
	public static void tripBarangUpdate(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!ctx.bolehAksi("nota_sales", "update")) {
			tolak(hasil, "Akun Anda tidak berhak mengubah barang dibawa.");
			return;
		}
		JSONArray rows = request.optJSONArray("rows");
		if (rows == null || rows.length() == 0) {
			tolak(hasil, "rows wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			for (int i = 0; i < rows.length(); i++) {
				JSONObject r = rows.getJSONObject(i);
				Long bid = optLong(r, "barang_id");
				SpjSalesBarang b = bid == null ? null
						: (SpjSalesBarang) session.get(SpjSalesBarang.class, bid);
				if (b == null) {
					tx.rollback();
					tolak(hasil, "Baris barang " + bid + " tidak ditemukan.");
					return;
				}
				if (!bolehSentuhSpj(ctx, b.getSpj())) {
					tx.rollback();
					tolak(hasil, "Barang ini bukan milik sales Anda.");
					return;
				}
				if (SpjSalesBarang.STATUS_RECONCILED.equals(b.getStatus())) {
					tx.rollback();
					tolak(hasil, "Baris sudah direkonsiliasi -- tidak bisa diubah.");
					return;
				}
				BigDecimal terjual = optBigDecimal(r, "qty_terjual");
				BigDecimal kembali = optBigDecimal(r, "qty_kembali");
				BigDecimal rusak = optBigDecimal(r, "qty_rusak");
				BigDecimal hilang = optBigDecimal(r, "qty_hilang");
				if (terjual != null) b.setQtyTerjual(terjual);
				if (kembali != null) b.setQtyKembali(kembali);
				if (rusak != null) b.setQtyRusak(rusak);
				if (hilang != null) b.setQtyHilang(hilang);
				double pakai = b.getQtyTerjual().doubleValue() + b.getQtyKembali().doubleValue()
						+ b.getQtyRusak().doubleValue() + b.getQtyHilang().doubleValue();
				if (pakai > b.getQtyDimuat().doubleValue() + 0.001) {
					tx.rollback();
					tolak(hasil, "Barang " + str(b.getNamaProduk()) + ": terjual+kembali+rusak+hilang ("
							+ pakai + ") melebihi qty dimuat (" + b.getQtyDimuat() + ").");
					return;
				}
				b.setAlasanSelisih(r.optString("alasan_selisih", str(b.getAlasanSelisih())));
				session.saveOrUpdate(b);
			}
			tx.commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Penjualan tunai lapangan -> kas CASH_SALE (idempoten kode_unik via referensi unik tidak
	 *  dipaksa -- ledger append-only; klien memakai outbox P7 dgn satu tembakan). */
	public static void tripCashSale(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		catatKasSederhana(ctx, request, hasil, NotaSalesKas.JENIS_CASH_SALE, false);
	}

	/** Setoran kas ke pemilik di tengah/akhir sesi -> kas OWNER_DEPOSIT (negatif). */
	public static void tripDeposit(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		catatKasSederhana(ctx, request, hasil, NotaSalesKas.JENIS_OWNER_DEPOSIT, true);
	}

	private static void catatKasSederhana(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil, String jenis, boolean keluar) throws Exception {
		if (!ctx.bolehAksi("nota_sales", "update") && !ctx.bolehAksi("nota_sales", "create")) {
			tolak(hasil, "Akun Anda tidak berhak mencatat kas sesi.");
			return;
		}
		Long id = optLong(request, "session_id");
		BigDecimal nominal = optBigDecimal(request, "nominal");
		if (id == null || nominal == null || nominal.signum() <= 0) {
			tolak(hasil, "session_id dan nominal (>0) wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			NotaSalesSession sesi = (NotaSalesSession) session.get(NotaSalesSession.class, id);
			if (sesi == null || !NotaSalesSession.STATUS_ACTIVE.equals(sesi.getStatus())
					&& !NotaSalesSession.STATUS_RETURNED.equals(sesi.getStatus())
					&& !NotaSalesSession.STATUS_RECONCILING.equals(sesi.getStatus())) {
				tolak(hasil, "Sesi tidak ditemukan / sudah ditutup.");
				return;
			}
			if (!bolehSentuhSpj(ctx, sesi.getSpj())) {
				tolak(hasil, "Sesi ini bukan milik sales Anda.");
				return;
			}
			tx = session.beginTransaction();
			catatKas(session, sesi, jenis, keluar ? nominal.negate() : nominal,
					request.optString("referensi", "").trim(), request.optString("keterangan", "").trim());
			tx.commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// -- Biaya sesi + kategori configurable ------------------------------------------------------

	public static void expenseCategoryList(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			List rows = session.createCriteria(KategoriBiayaSales.class)
					.addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			for (int i = 0; i < rows.size(); i++) {
				KategoriBiayaSales k = (KategoriBiayaSales) rows.get(i);
				if (!Boolean.TRUE.equals(k.getAktif()) && !request.optBoolean("semua", false)) {
					continue;
				}
				JSONObject r = new JSONObject();
				r.put("id", k.getId());
				r.put("kode", str(k.getKode()));
				r.put("nama", str(k.getNama()));
				r.put("aktif", Boolean.TRUE.equals(k.getAktif()));
				// Akun beban kategori: dipakai mesin posting "Biaya Sesi Sales" (dok 61 butir E).
				try {
					if (k.getAkun() != null) {
						r.put("akunId", k.getAkun().getId());
						r.put("akunKode", str(k.getAkun().getKode()));
						r.put("akunNama", str(k.getAkun().getNama()));
					}
				} catch (Exception abaikan) {
					ais.common.ErrorAuditUtil.record(abaikan,
							"auto-audit SalesInventoryTripHelper.expenseCategoryList akun");
				}
				arr.put(r);
			}
			hasil.put("status", "00");
			hasil.put("rows", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void expenseCategorySave(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!pemilikAtauAdmin(ctx)) {
			tolak(hasil, "Kategori biaya hanya dikelola Pemilik/Admin.");
			return;
		}
		String kode = request.optString("kode", "").trim().toUpperCase();
		String nama = request.optString("nama", "").trim();
		if (kode.isEmpty() || nama.isEmpty()) {
			tolak(hasil, "kode dan nama wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			KategoriBiayaSales k = (KategoriBiayaSales) session.createCriteria(KategoriBiayaSales.class)
					.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
			tx = session.beginTransaction();
			if (k == null) {
				k = new KategoriBiayaSales();
				k.setKode(kode);
			}
			k.setNama(nama);
			if (!request.isNull("aktif")) {
				k.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			}
			if (request.has("akunId")) {
				Long akunId = optLong(request, "akunId");
				k.setAkun(akunId == null ? null
						: (ais.database.model.akunting.Akun) session.get(
								ais.database.model.akunting.Akun.class, akunId));
			}
			session.saveOrUpdate(k);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", k.getId());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void expenseCreate(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!ctx.bolehAksi("biaya_sales", "create") && !ctx.bolehAksi("nota_sales", "update")) {
			tolak(hasil, "Akun Anda tidak berhak mencatat biaya sesi.");
			return;
		}
		Long sesiId = optLong(request, "session_id");
		Long kategoriId = optLong(request, "kategori_id");
		BigDecimal nilai = optBigDecimal(request, "nilai");
		String kodeUnik = request.optString("kode_unik", "").trim();
		if (sesiId == null || kategoriId == null || nilai == null || nilai.signum() <= 0) {
			tolak(hasil, "session_id, kategori_id, dan nilai (>0) wajib diisi.");
			return;
		}
		if (kodeUnik.isEmpty()) {
			tolak(hasil, "kode_unik (kunci idempoten) wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			NotaSalesBiaya sudah = (NotaSalesBiaya) session.createCriteria(NotaSalesBiaya.class)
					.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
			if (sudah != null) {
				hasil.put("status", "00");
				hasil.put("id", sudah.getId());
				hasil.put("idempotentReplay", true);
				return;
			}
			NotaSalesSession sesi = (NotaSalesSession) session.get(NotaSalesSession.class, sesiId);
			if (sesi == null || NotaSalesSession.STATUS_CLOSED.equals(sesi.getStatus())) {
				tolak(hasil, "Sesi tidak ditemukan / sudah ditutup.");
				return;
			}
			if (!bolehSentuhSpj(ctx, sesi.getSpj())) {
				tolak(hasil, "Sesi ini bukan milik sales Anda.");
				return;
			}
			KategoriBiayaSales kategori = (KategoriBiayaSales) session.get(KategoriBiayaSales.class, kategoriId);
			if (kategori == null) {
				tolak(hasil, "Kategori biaya tidak ditemukan.");
				return;
			}
			tx = session.beginTransaction();
			NotaSalesBiaya b = new NotaSalesBiaya();
			b.setSesi(sesi);
			b.setKategori(kategori);
			b.setUraian(request.optString("uraian", "").trim());
			b.setNilai(nilai);
			b.setMetode(request.optString("metode", "TUNAI").trim().toUpperCase());
			b.setPenerima(request.optString("penerima", "").trim());
			b.setNomorBukti(request.optString("nomor_bukti", "").trim());
			b.setKodeUnik(kodeUnik);
			b.setDibuatOleh(tbmuser);
			session.save(b);
			if (NotaSalesBiaya.METODE_TUNAI.equals(b.getMetode())) {
				catatKas(session, sesi, NotaSalesKas.JENIS_EXPENSE_CASH, nilai.negate(),
						"BIAYA-" + b.getId(), str(kategori.getNama()) + ": " + str(b.getUraian()));
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", b.getId());
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

	// -- Kulakan dalam sesi ----------------------------------------------------------------------

	public static void tripPurchaseLink(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!ctx.bolehAksi("pembelian_sales", "create") && !ctx.bolehAksi("nota_sales", "update")) {
			tolak(hasil, "Akun Anda tidak berhak mencatat pembelian sesi.");
			return;
		}
		Long sesiId = optLong(request, "session_id");
		BigDecimal totalFaktur = optBigDecimal(request, "total_faktur");
		BigDecimal dibayar = optBigDecimal(request, "dibayar_sesi");
		String kodeUnik = request.optString("kode_unik", "").trim();
		if (sesiId == null || totalFaktur == null || totalFaktur.signum() <= 0) {
			tolak(hasil, "session_id dan total_faktur (>0) wajib diisi.");
			return;
		}
		if (kodeUnik.isEmpty()) {
			tolak(hasil, "kode_unik (kunci idempoten) wajib diisi.");
			return;
		}
		if (dibayar == null) dibayar = BigDecimal.ZERO;
		if (dibayar.signum() < 0 || dibayar.doubleValue() > totalFaktur.doubleValue() + 0.01) {
			tolak(hasil, "dibayar_sesi harus 0..total_faktur.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			NotaSalesPembelian sudah = (NotaSalesPembelian) session.createCriteria(NotaSalesPembelian.class)
					.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
			if (sudah != null) {
				hasil.put("status", "00");
				hasil.put("id", sudah.getId());
				hasil.put("idempotentReplay", true);
				return;
			}
			NotaSalesSession sesi = (NotaSalesSession) session.get(NotaSalesSession.class, sesiId);
			if (sesi == null || NotaSalesSession.STATUS_CLOSED.equals(sesi.getStatus())) {
				tolak(hasil, "Sesi tidak ditemukan / sudah ditutup.");
				return;
			}
			if (!bolehSentuhSpj(ctx, sesi.getSpj())) {
				tolak(hasil, "Sesi ini bukan milik sales Anda.");
				return;
			}
			tx = session.beginTransaction();
			NotaSalesPembelian b = new NotaSalesPembelian();
			b.setSesi(sesi);
			Long fakturId = optLong(request, "faktur_id");
			if (fakturId != null) {
				b.setPengadaanFaktur((PengadaanFaktur) session.get(PengadaanFaktur.class, fakturId));
			}
			Long supplierId = optLong(request, "supplier_id");
			if (supplierId != null) {
				b.setSupplier((Penyedia) session.get(Penyedia.class, supplierId));
			}
			b.setTotalFaktur(totalFaktur);
			b.setDibayarSesi(dibayar);
			b.setSisaHutang(totalFaktur.subtract(dibayar));
			b.setTujuanStok(request.optString("tujuan_stok", "MOBIL_SALES").trim().toUpperCase());
			b.setKeterangan(request.optString("keterangan", "").trim());
			b.setKodeUnik(kodeUnik);
			session.save(b);
			if (dibayar.signum() > 0) {
				catatKas(session, sesi, NotaSalesKas.JENIS_PURCHASE_PAYMENT, dibayar.negate(),
						"BELI-" + b.getId(), "Pembayaran pembelian dalam sesi");
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", b.getId());
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

	// -- Return / reconcile / close --------------------------------------------------------------

	public static void tripReturn(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		ubahStatusSesi(ctx, tbmuser, request, hasil, NotaSalesSession.STATUS_ACTIVE,
				NotaSalesSession.STATUS_RETURNED, false);
	}

	public static void tripReconcile(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		ubahStatusSesi(ctx, tbmuser, request, hasil, NotaSalesSession.STATUS_RETURNED,
				NotaSalesSession.STATUS_RECONCILING, true);
	}

	private static void ubahStatusSesi(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil, String dari, String ke, boolean cekBarang)
			throws Exception {
		if (!ctx.bolehAksi("nota_sales", "update")) {
			tolak(hasil, "Akun Anda tidak berhak mengubah status sesi.");
			return;
		}
		Long id = optLong(request, "session_id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			NotaSalesSession sesi = id == null ? null
					: (NotaSalesSession) session.get(NotaSalesSession.class, id);
			if (sesi == null) {
				tolak(hasil, "Sesi tidak ditemukan.");
				return;
			}
			if (!bolehSentuhSpj(ctx, sesi.getSpj())) {
				tolak(hasil, "Sesi ini bukan milik sales Anda.");
				return;
			}
			if (!dari.equals(sesi.getStatus())) {
				tolak(hasil, "Transisi " + sesi.getStatus() + " -> " + ke + " tidak diizinkan.");
				return;
			}
			if (cekBarang) {
				// Invariant ERD 3.3: masuk RECONCILING semua barang harus habis teralokasi
				// (dimuat = terjual+kembali+rusak+hilang; masih dibawa = 0).
				List barang = session.createCriteria(SpjSalesBarang.class)
						.add(Restrictions.eq("spj", sesi.getSpj())).list();
				StringBuilder gagal = new StringBuilder();
				for (int i = 0; i < barang.size(); i++) {
					SpjSalesBarang b = (SpjSalesBarang) barang.get(i);
					double masih = b.getQtyDimuat().doubleValue() - b.getQtyTerjual().doubleValue()
							- b.getQtyKembali().doubleValue() - b.getQtyRusak().doubleValue()
							- b.getQtyHilang().doubleValue();
					if (Math.abs(masih) > 0.001) {
						if (gagal.length() > 0) gagal.append(", ");
						gagal.append(str(b.getNamaProduk())).append(" (sisa ").append(masih).append(")");
					}
				}
				if (gagal.length() > 0) {
					tolak(hasil, "Barang belum habis dialokasikan (terjual/kembali/rusak/hilang): " + gagal);
					return;
				}
			}
			tx = session.beginTransaction();
			sesi.setStatus(ke);
			if (NotaSalesSession.STATUS_RETURNED.equals(ke)) {
				sesi.setWaktuKembali(ais.ui.util.WaktuUtil.getDate());
				SuratPerintahSalesJalan spj = sesi.getSpj();
				spj.setStatus(SuratPerintahSalesJalan.STATUS_RETURNED);
				spj.setTanggalKembaliAktual(ais.ui.util.WaktuUtil.getDate());
				session.saveOrUpdate(spj);
			}
			if (NotaSalesSession.STATUS_RECONCILING.equals(ke)) {
				SuratPerintahSalesJalan spj = sesi.getSpj();
				spj.setStatus(SuratPerintahSalesJalan.STATUS_RECONCILING);
				session.saveOrUpdate(spj);
			}
			isiOleh(sesi, tbmuser);
			session.saveOrUpdate(sesi);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("statusBaru", ke);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Tutup sesi (RECONCILING -> CLOSED) -- WAJIB Pemilik/Admin; snapshot dua rumus. */
	public static void tripClose(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (SalesInventoryTripTenant.aktif(ctx)) {
			// BELUM dipindahkan. Menjalankan jalur legacy di sini akan membaca -- dan pada
			// aksi bermuatan uang, MENULIS -- ke schema BERSAMA. Ditolak sampai jalur
			// tenantnya ditulis beserta uji kesetaraannya; lihat SalesInventoryTripTenant.
			tolak(hasil, "Sales Lapangan belum tersedia pada tenant berschema.");
			return;
		}
		if (!pemilikAtauAdmin(ctx)) {
			tolak(hasil, "Penutupan sesi hanya oleh Pemilik/Admin (approval eksplisit).");
			return;
		}
		Long id = optLong(request, "session_id");
		BigDecimal kasAktual = optBigDecimal(request, "kas_fisik_aktual");
		if (id == null || kasAktual == null) {
			tolak(hasil, "session_id dan kas_fisik_aktual wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			NotaSalesSession sesi = (NotaSalesSession) session.get(NotaSalesSession.class, id);
			if (sesi == null) {
				tolak(hasil, "Sesi tidak ditemukan.");
				return;
			}
			if (!NotaSalesSession.STATUS_RECONCILING.equals(sesi.getStatus())) {
				tolak(hasil, "Hanya sesi RECONCILING yang bisa ditutup (status: " + sesi.getStatus() + ").");
				return;
			}
			tx = session.beginTransaction();
			// Snapshot total dari ledger (sumber kebenaran tetap ledger).
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT COALESCE(SUM(k.nominal),0),"
							+ " COALESCE(SUM(CASE WHEN k.jenis='COLLECTION_CASH' THEN k.nominal ELSE 0 END),0),"
							+ " COALESCE(SUM(CASE WHEN k.jenis='OWNER_DEPOSIT' THEN -k.nominal ELSE 0 END),0)"
							+ " FROM koperasi.nota_sales_kas k WHERE k.sesi = ?");
			ps.setLong(1, sesi.getId().longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			double kasSeharusnya = 0, tunai = 0, setoran = 0;
			if (rs.next()) {
				kasSeharusnya = rs.getDouble(1);
				tunai = rs.getDouble(2);
				setoran = rs.getDouble(3);
			}
			rs.close(); ps.close();
			java.sql.PreparedStatement psB = session.connection().prepareStatement(
					"SELECT COALESCE(SUM(b.nilai),0) FROM koperasi.nota_sales_biaya b WHERE b.sesi = ?");
			psB.setLong(1, sesi.getId().longValue());
			java.sql.ResultSet rsB = psB.executeQuery();
			double totalBiaya = rsB.next() ? rsB.getDouble(1) : 0;
			rsB.close(); psB.close();
			java.sql.PreparedStatement psP = session.connection().prepareStatement(
					"SELECT COALESCE(SUM(p.dibayar_sesi),0) FROM koperasi.nota_sales_pembelian p WHERE p.sesi = ?");
			psP.setLong(1, sesi.getId().longValue());
			java.sql.ResultSet rsP = psP.executeQuery();
			double totalBeli = rsP.next() ? rsP.getDouble(1) : 0;
			rsP.close(); psP.close();
			java.sql.PreparedStatement psT = session.connection().prepareStatement(
					"SELECT COALESCE(SUM(p.nominal),0),"
							+ " COALESCE(SUM(CASE WHEN p.metode='TUNAI' THEN p.nominal ELSE 0 END),0)"
							+ " FROM koperasi.penerimaan_piutang_customer p WHERE p.sesi = ?");
			psT.setLong(1, sesi.getId().longValue());
			java.sql.ResultSet rsT = psT.executeQuery();
			double totalTertagih = 0, tertagihTunai = 0;
			if (rsT.next()) {
				totalTertagih = rsT.getDouble(1);
				tertagihTunai = rsT.getDouble(2);
			}
			rsT.close(); psT.close();

			sesi.setTotalPenerimaanTunai(new BigDecimal(String.valueOf(tertagihTunai)));
			sesi.setTotalPenerimaanNonTunai(new BigDecimal(String.valueOf(totalTertagih - tertagihTunai)));
			sesi.setTotalBiaya(new BigDecimal(String.valueOf(totalBiaya)));
			sesi.setTotalPembayaranPembelian(new BigDecimal(String.valueOf(totalBeli)));
			sesi.setTotalSetoran(new BigDecimal(String.valueOf(setoran)));
			sesi.setKasFisikAktual(kasAktual);
			sesi.setSelisihKas(new BigDecimal(String.valueOf(kasAktual.doubleValue() - kasSeharusnya)));
			sesi.setCatatanPenutupan(request.optString("catatan", "").trim());
			sesi.setWaktuTutup(ais.ui.util.WaktuUtil.getDate());
			sesi.setStatus(NotaSalesSession.STATUS_CLOSED);
			sesi.setDisetujuiOleh(tbmuser);
			isiOleh(sesi, tbmuser);
			session.saveOrUpdate(sesi);
			SuratPerintahSalesJalan spj = sesi.getSpj();
			spj.setStatus(SuratPerintahSalesJalan.STATUS_CLOSED);
			session.saveOrUpdate(spj);
			// Barang & nota -> RECONCILED (final).
			List barang = session.createCriteria(SpjSalesBarang.class)
					.add(Restrictions.eq("spj", spj)).list();
			for (int i = 0; i < barang.size(); i++) {
				SpjSalesBarang b = (SpjSalesBarang) barang.get(i);
				b.setStatus(SpjSalesBarang.STATUS_RECONCILED);
				session.saveOrUpdate(b);
			}
			List nota = session.createCriteria(SpjSalesNota.class)
					.add(Restrictions.eq("spj", spj)).list();
			for (int i = 0; i < nota.size(); i++) {
				SpjSalesNota n = (SpjSalesNota) nota.get(i);
				n.setStatus(SpjSalesNota.STATUS_RECONCILED);
				session.saveOrUpdate(n);
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("hasilBersih", totalTertagih - totalBiaya - totalBeli);
			hasil.put("kasFisikSeharusnya", kasSeharusnya);
			hasil.put("selisihKas", kasAktual.doubleValue() - kasSeharusnya);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Seed kategori biaya idempoten (dipanggil ApiEBisnis.init). */
	public static void pastikanSeedKategoriBiaya() {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			String[][] seed = { { "BBM", "Bensin/BBM" }, { "TOL", "Tol" }, { "PARKIR", "Parkir" },
					{ "MAKAN", "Makan/Uang Harian" }, { "BONGKAR_MUAT", "Bongkar Muat" },
					{ "PENGINAPAN", "Penginapan" }, { "SERVIS", "Servis Darurat" },
					{ "ADMIN", "Administrasi" }, { "LAINNYA", "Lain-lain" } };
			tx = session.beginTransaction();
			for (int i = 0; i < seed.length; i++) {
				KategoriBiayaSales ada = (KategoriBiayaSales) session
						.createCriteria(KategoriBiayaSales.class)
						.add(Restrictions.eq("kode", seed[i][0])).setMaxResults(1).uniqueResult();
				if (ada == null) {
					KategoriBiayaSales k = new KategoriBiayaSales();
					k.setKode(seed[i][0]);
					k.setNama(seed[i][1]);
					k.setAktif(Boolean.TRUE);
					session.save(k);
				}
			}
			tx.commit();
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			ais.common.ErrorAuditUtil.record(e, "SalesInventoryTripHelper.pastikanSeedKategoriBiaya");
		} finally {
			if (session != null) {
				HibernateUtil.closeSessionQuietly(session);
			}
		}
	}
}
