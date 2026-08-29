package ais.action.servlet.api;

import java.math.BigDecimal;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.PengadaanFaktur;
import ais.database.model.koperasi.AlokasiPembayaranHutangSupplier;
import ais.database.model.koperasi.PayableFakturInfo;
import ais.database.model.koperasi.PembayaranHutangSupplier;
import ais.database.model.library.Penyedia;

/**
 * <h3>Hutang Supplier (AP) -- layar legacy 20-29, varian Inventory &amp; Sales.</h3>
 *
 * <p>Register event (Matriks layar 22): DEBIT = faktur kulakan ber-info jenis DP/CREDIT
 * ({@link PayableFakturInfo}); KREDIT = pembayaran teralokasi ({@link PembayaranHutangSupplier}
 * + {@link AlokasiPembayaranHutangSupplier}) + dibayar-awal saat faktur. Outstanding SELALU
 * dihitung, tidak pernah disimpan. Faktur tanpa info = CASH lunas (alur kulakan lama tunai;
 * backfill sadar via {@code si_purchase_terms_save}).</p>
 *
 * <p>Ekspresi outstanding per faktur f:
 * {@code COALESCE(f.total_faktur_manual, COALESCE(f.total_hitung_saat_simpan,0))
 *  - COALESCE(i.dibayar_awal,0) - SUM(alokasi)}.</p>
 */
public final class SalesInventoryPayableHelper {

	private SalesInventoryPayableHelper() {
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

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static void isiOleh(Object entity, Tbmuser tbmuser) {
		try {
			entity.getClass().getMethod("setOleh", String.class).invoke(entity, tbmuser.getUserId());
			entity.getClass().getMethod("setOlehId", String.class).invoke(entity, tbmuser.getUserId());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "SalesInventoryPayableHelper.isiOleh");
		}
	}

	private static final String EXPR_TOTAL = "COALESCE(f.total_faktur_manual, COALESCE(f.total_hitung_saat_simpan,0))";
	private static final String EXPR_ALOKASI = "COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_pembayaran_hutang_supplier a WHERE a.pengadaan_faktur = f.id),0)";
	private static final String EXPR_OUTSTANDING = "(" + EXPR_TOTAL + " - COALESCE(i.dibayar_awal,0) - " + EXPR_ALOKASI + ")";

	// =============================================================================================
	// SCR-20: termin/jenis/DP per faktur kulakan (upsert PayableFakturInfo)
	// =============================================================================================

	public static void purchaseTermsSave(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehAksi("hutang", "create") && !ctx.bolehAksi("hutang", "update")
				&& !ctx.bolehAksi("kulakan", "update")) {
			tolak(hasil, "Akun Anda tidak berhak mengatur termin/jenis pembayaran faktur.");
			return;
		}
		Long fakturId = optLong(request, "faktur_id");
		if (fakturId == null) {
			tolak(hasil, "faktur_id wajib diisi.");
			return;
		}
		String jenis = request.optString("jenis_pembayaran", "").trim().toUpperCase();
		if (!PayableFakturInfo.JENIS_CASH.equals(jenis) && !PayableFakturInfo.JENIS_DP.equals(jenis)
				&& !PayableFakturInfo.JENIS_CREDIT.equals(jenis)) {
			tolak(hasil, "jenis_pembayaran harus CASH, DP, atau CREDIT.");
			return;
		}
		int terminHari = Math.max(0, request.optInt("termin_hari", 0));
		BigDecimal dibayarAwal = optBigDecimal(request, "dibayar_awal");
		if (PayableFakturInfo.JENIS_DP.equals(jenis) && (dibayarAwal == null || dibayarAwal.signum() <= 0)) {
			tolak(hasil, "Jenis DP wajib menyertakan dibayar_awal > 0.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			PengadaanFaktur f = (PengadaanFaktur) session.get(PengadaanFaktur.class, fakturId);
			if (f == null) {
				tolak(hasil, "Faktur kulakan tidak ditemukan.");
				return;
			}
			double total = f.getTotalFakturFinal() == null ? 0 : f.getTotalFakturFinal().doubleValue();
			if (dibayarAwal != null && dibayarAwal.doubleValue() > total + 0.01) {
				tolak(hasil, "dibayar_awal melebihi total faktur (" + total + ").");
				return;
			}
			PayableFakturInfo info = (PayableFakturInfo) session.createCriteria(PayableFakturInfo.class)
					.add(Restrictions.eq("pengadaanFaktur", f)).setMaxResults(1).uniqueResult();
			if (info == null) {
				info = new PayableFakturInfo();
				info.setPengadaanFaktur(f);
			}
			info.setJenisPembayaran(jenis);
			info.setTerminHari(Integer.valueOf(terminHari));
			if (PayableFakturInfo.JENIS_CASH.equals(jenis)) {
				// CASH = lunas saat faktur: dibayar awal otomatis = total (tidak menimbulkan hutang).
				info.setDibayarAwal(new BigDecimal(String.valueOf(total)));
			} else if (dibayarAwal != null) {
				info.setDibayarAwal(dibayarAwal);
			}
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.setTime(f.getTanggalFaktur());
			cal.add(java.util.Calendar.DAY_OF_MONTH, terminHari);
			info.setJatuhTempo(cal.getTime());
			if (!request.isNull("keterangan")) info.setKeterangan(request.optString("keterangan", "").trim());
			isiOleh(info, tbmuser);
			tx = session.beginTransaction();
			session.saveOrUpdate(info);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("infoId", info.getId());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-21/22/23: register hutang per supplier + faktur (filter lunas hanya visual)
	// =============================================================================================

	public static void payableList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		String keyword = request == null || request.isNull("keyword") ? null : request.optString("keyword", "").trim();
		boolean tampilkanLunas = request != null && request.optBoolean("tampilkan_lunas", false);
		Long supplierId = optLong(request, "supplier_id");
		Long fakturId = optLong(request, "faktur_id"); // deep-link SCR-21 (si_payable_from_purchase)
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = Math.min(100, Math.max(1, request == null ? 20 : request.optInt("page_size", 20)));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(
					" WHERE i.jenis_pembayaran IN ('DP','CREDIT') ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (supplierId != null) {
				where.append(" AND f.supplier = ? ");
				params.add(supplierId);
			}
			if (fakturId != null) {
				where.append(" AND f.id = ? ");
				params.add(fakturId);
			}
			if (keyword != null && !keyword.isEmpty()) {
				where.append(" AND (s.kode ILIKE ? OR s.nama ILIKE ? OR COALESCE(f.nomor_faktur,'') ILIKE ?) ");
				String k = "%" + keyword + "%";
				params.add(k); params.add(k); params.add(k);
			}
			if (!tampilkanLunas) {
				where.append(" AND ").append(EXPR_OUTSTANDING).append(" > 0.009 ");
			}
			String dasar = " FROM koperasi.pengadaan_faktur f "
					+ "JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id "
					+ "JOIN library.penyedia s ON f.supplier = s.id " + where;

			java.sql.PreparedStatement psTotal = session.connection().prepareStatement(
					"SELECT COUNT(*), COALESCE(SUM(" + EXPR_OUTSTANDING + "),0) " + dasar);
			for (int i = 0; i < params.size(); i++) psTotal.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsTotal = psTotal.executeQuery();
			long total = 0;
			double totalOutstanding = 0;
			if (rsTotal.next()) {
				total = rsTotal.getLong(1);
				totalOutstanding = rsTotal.getDouble(2);
			}
			rsTotal.close(); psTotal.close();

			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT f.id, COALESCE(f.nomor_faktur,''), f.tanggal_faktur, f.supplier, s.kode, s.nama, "
							+ "COALESCE(s.alamat,''), i.jenis_pembayaran, i.termin_hari, i.jatuh_tempo, "
							+ EXPR_TOTAL + ", COALESCE(i.dibayar_awal,0), " + EXPR_ALOKASI + ", " + EXPR_OUTSTANDING
							+ dasar + " ORDER BY s.kode ASC, f.tanggal_faktur ASC, f.id ASC LIMIT ? OFFSET ?");
			int idx = 1;
			for (int i = 0; i < params.size(); i++) ps.setObject(idx++, params.get(i));
			ps.setInt(idx++, size);
			ps.setInt(idx++, (page - 1) * size);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("fakturId", rs.getLong(1));
				j.put("nomorFaktur", str(rs.getString(2)));
				j.put("tanggalFaktur", str(rs.getTimestamp(3)));
				j.put("supplierId", rs.getLong(4));
				j.put("supplierKode", str(rs.getString(5)));
				j.put("supplierNama", str(rs.getString(6)));
				j.put("supplierAlamat", str(rs.getString(7)));
				j.put("jenisPembayaran", str(rs.getString(8)));
				j.put("terminHari", rs.getInt(9));
				j.put("jatuhTempo", str(rs.getDate(10)));
				j.put("totalFaktur", rs.getDouble(11));
				j.put("dibayarAwal", rs.getDouble(12));
				j.put("dibayarAlokasi", rs.getDouble(13));
				double outstanding = rs.getDouble(14);
				j.put("outstanding", outstanding);
				j.put("lunas", outstanding <= 0.009);
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", page);
			hasil.put("pageSize", size);
			hasil.put("totalOutstanding", totalOutstanding);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-24: pembayaran hutang (atomic + idempoten + alokasi multi-faktur)
	// =============================================================================================

	public static void payablePaymentCreate(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehAksi("hutang", "create")) {
			tolak(hasil, "Akun Anda tidak berhak mencatat pembayaran hutang.");
			return;
		}
		Long supplierId = optLong(request, "supplier_id");
		BigDecimal nominal = optBigDecimal(request, "nominal");
		String metode = request.optString("metode", "").trim().toUpperCase();
		String kodeUnik = request.optString("kode_unik", "").trim();
		JSONArray alokasi = request.optJSONArray("alokasi");
		if (supplierId == null || nominal == null || nominal.signum() <= 0) {
			tolak(hasil, "supplier_id dan nominal (>0) wajib diisi.");
			return;
		}
		if (kodeUnik.isEmpty()) {
			tolak(hasil, "kode_unik (kunci idempoten) wajib diisi.");
			return;
		}
		if (alokasi == null || alokasi.length() == 0) {
			tolak(hasil, "Alokasi ke faktur wajib diisi (minimal satu).");
			return;
		}
		if (metode.isEmpty()) metode = PembayaranHutangSupplier.METODE_TUNAI;
		if (PembayaranHutangSupplier.METODE_GIRO.equals(metode)
				&& request.optString("no_bg", "").trim().isEmpty()) {
			tolak(hasil, "Metode GIRO wajib menyertakan no_bg.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			// Idempoten: kode_unik sudah ada -> kembalikan pembayaran pertama, JANGAN menggandakan.
			PembayaranHutangSupplier sudahAda = (PembayaranHutangSupplier) session
					.createCriteria(PembayaranHutangSupplier.class)
					.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
			if (sudahAda != null) {
				hasil.put("status", "00");
				hasil.put("id", sudahAda.getId());
				hasil.put("idempotentReplay", true);
				return;
			}
			Penyedia supplier = (Penyedia) session.get(Penyedia.class, supplierId);
			if (supplier == null) {
				tolak(hasil, "Supplier tidak ditemukan.");
				return;
			}

			// Validasi alokasi: jumlah = nominal, tiap faktur milik supplier & cukup outstanding.
			double jumlahAlokasi = 0;
			for (int i = 0; i < alokasi.length(); i++) {
				jumlahAlokasi += alokasi.getJSONObject(i).optDouble("nominal", 0);
			}
			if (Math.abs(jumlahAlokasi - nominal.doubleValue()) > 0.01) {
				tolak(hasil, "Total alokasi (" + jumlahAlokasi + ") harus sama dengan nominal pembayaran ("
						+ nominal + ").");
				return;
			}

			tx = session.beginTransaction();
			// Kunci baris faktur yang terlibat (FOR UPDATE) supaya dua pembayaran bersamaan tidak
			// sama-sama lolos validasi outstanding.
			for (int i = 0; i < alokasi.length(); i++) {
				JSONObject a = alokasi.getJSONObject(i);
				long fid = a.optLong("faktur_id", -1);
				double n = a.optDouble("nominal", 0);
				if (fid <= 0 || n <= 0) {
					tx.rollback();
					tolak(hasil, "Baris alokasi tidak valid (faktur_id/nominal).");
					return;
				}
				java.sql.PreparedStatement lock = session.connection().prepareStatement(
						"SELECT f.id FROM koperasi.pengadaan_faktur f WHERE f.id = ? AND f.supplier = ? FOR UPDATE");
				lock.setLong(1, fid);
				lock.setLong(2, supplierId.longValue());
				java.sql.ResultSet rsLock = lock.executeQuery();
				boolean ada = rsLock.next();
				rsLock.close(); lock.close();
				if (!ada) {
					tx.rollback();
					tolak(hasil, "Faktur " + fid + " tidak ditemukan / bukan milik supplier ini.");
					return;
				}
				java.sql.PreparedStatement cek = session.connection().prepareStatement(
						"SELECT " + EXPR_OUTSTANDING + " FROM koperasi.pengadaan_faktur f "
								+ "JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id WHERE f.id = ?");
				cek.setLong(1, fid);
				java.sql.ResultSet rsCek = cek.executeQuery();
				double outstanding = rsCek.next() ? rsCek.getDouble(1) : -1;
				rsCek.close(); cek.close();
				if (outstanding < 0) {
					tx.rollback();
					tolak(hasil, "Faktur " + fid + " belum punya info hutang (set termin/jenis dulu).");
					return;
				}
				if (n > outstanding + 0.01) {
					tx.rollback();
					tolak(hasil, "Alokasi " + n + " melebihi outstanding faktur " + fid + " (" + outstanding + ").");
					return;
				}
			}

			PembayaranHutangSupplier bayar = new PembayaranHutangSupplier();
			bayar.setSupplier(supplier);
			bayar.setNominal(nominal);
			bayar.setMetode(metode);
			bayar.setNoBg(request.optString("no_bg", "").trim());
			bayar.setNamaBank(request.optString("nama_bank", "").trim());
			java.util.Date tglBg = null;
			try {
				String s = request.optString("tanggal_bg", "").trim();
				if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
					tglBg = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s);
				}
			} catch (Exception ignore) { }
			bayar.setTanggalBg(tglBg);
			bayar.setKeterangan(request.optString("keterangan", "").trim());
			bayar.setKodeUnik(kodeUnik);
			bayar.setDibuatOleh(tbmuser);
			isiOleh(bayar, tbmuser);
			session.save(bayar);
			// Tautkan ke Daftar Pengajuan Transfer supaya pembayaran ini terlihat di menu
			// Pembayaran Transfer keuangan (sejajar pembayaran pengadaan aset). Kegagalan
			// penautan tidak boleh menggagalkan pembayarannya sendiri -- tombol Sinkronkan
			// pada layar transfer akan menyusulkan yang terlewat.
			try {
				ais.database.model.akunting.DaftarPengajuanTransfer.simpanPembayaranHutangSupplier(bayar);
			} catch (Exception exDpc) {
				ais.common.ErrorAuditUtil.record(exDpc,
						"auto-audit SalesInventoryPayableHelper.payablePaymentCreate simpanDpc");
			}
			for (int i = 0; i < alokasi.length(); i++) {
				JSONObject a = alokasi.getJSONObject(i);
				AlokasiPembayaranHutangSupplier al = new AlokasiPembayaranHutangSupplier();
				al.setPembayaran(bayar);
				al.setPengadaanFaktur((PengadaanFaktur) session.get(PengadaanFaktur.class,
						Long.valueOf(a.optLong("faktur_id"))));
				al.setNominal(new BigDecimal(String.valueOf(a.optDouble("nominal"))));
				session.save(al);
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", bayar.getId());
		} catch (org.hibernate.exception.ConstraintViolationException dup) {
			// Balapan kode_unik (dua retry paralel) -- perlakukan sbg replay idempoten.
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
	// SCR-25/26: riwayat + data voucher
	// =============================================================================================

	public static void payablePaymentHistory(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		String dari = request == null ? "" : request.optString("dari", "").trim();
		String sampai = request == null ? "" : request.optString("sampai", "").trim();
		String metode = request == null ? "" : request.optString("metode", "").trim().toUpperCase();
		String keyword = request == null ? "" : request.optString("keyword", "").trim();
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = Math.min(100, Math.max(1, request == null ? 20 : request.optInt("page_size", 20)));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (dari.matches("\\d{4}-\\d{2}-\\d{2}") && sampai.matches("\\d{4}-\\d{2}-\\d{2}")) {
				where.append(" AND b.tanggal BETWEEN CAST(? AS date) AND (CAST(? AS date) + interval '1 day') ");
				params.add(dari); params.add(sampai);
			}
			if (!metode.isEmpty() && !"SEMUA".equals(metode)) {
				where.append(" AND b.metode = ? ");
				params.add(metode);
			}
			if (!keyword.isEmpty()) {
				where.append(" AND (s.kode ILIKE ? OR s.nama ILIKE ?) ");
				params.add("%" + keyword + "%");
				params.add("%" + keyword + "%");
			}
			String dasar = " FROM koperasi.pembayaran_hutang_supplier b JOIN library.penyedia s ON b.supplier = s.id " + where;
			java.sql.PreparedStatement psTotal = session.connection().prepareStatement("SELECT COUNT(*) " + dasar);
			for (int i = 0; i < params.size(); i++) psTotal.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsTotal = psTotal.executeQuery();
			long total = rsTotal.next() ? rsTotal.getLong(1) : 0;
			rsTotal.close(); psTotal.close();

			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT b.id, b.tanggal, b.supplier, s.kode, s.nama, b.nominal, b.metode, "
							+ "COALESCE(b.no_bg,''), COALESCE(b.nama_bank,''), b.tanggal_bg, "
							+ "COALESCE(b.keterangan,''), COALESCE(b.oleh,''), b.kode_unik, "
							+ "(SELECT COALESCE(string_agg(COALESCE(f2.nomor_faktur,'#' || f2.id) || ' (' || a2.nominal || ')', ', '),'') "
							+ " FROM koperasi.alokasi_pembayaran_hutang_supplier a2 "
							+ " JOIN koperasi.pengadaan_faktur f2 ON a2.pengadaan_faktur = f2.id WHERE a2.pembayaran = b.id), "
							+ "COALESCE(b.status_dok,'AKTIF'), b.status_bg "
							+ dasar + " ORDER BY b.tanggal DESC, b.id DESC LIMIT ? OFFSET ?");
			int idx = 1;
			for (int i = 0; i < params.size(); i++) ps.setObject(idx++, params.get(i));
			ps.setInt(idx++, size);
			ps.setInt(idx++, (page - 1) * size);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("tanggal", str(rs.getTimestamp(2)));
				j.put("supplierId", rs.getLong(3));
				j.put("supplierKode", str(rs.getString(4)));
				j.put("supplierNama", str(rs.getString(5)));
				j.put("nominal", rs.getDouble(6));
				j.put("metode", str(rs.getString(7)));
				j.put("noBg", str(rs.getString(8)));
				j.put("namaBank", str(rs.getString(9)));
				j.put("tanggalBg", str(rs.getDate(10)));
				j.put("keterangan", str(rs.getString(11)));
				j.put("oleh", str(rs.getString(12)));
				j.put("kodeUnik", str(rs.getString(13)));
				j.put("alokasiRingkas", str(rs.getString(14)));
				j.put("statusDok", str(rs.getString(15)));
				j.put("statusBg", rs.getString(16) == null ? "" : str(rs.getString(16)));
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

	public static void payablePaymentReceipt(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		Long id = optLong(request, "id");
		if (id == null) {
			tolak(hasil, "id pembayaran wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT b.id, b.tanggal, s.kode, s.nama, COALESCE(s.alamat,''), b.nominal, b.metode, "
							+ "COALESCE(b.no_bg,''), COALESCE(b.nama_bank,''), b.tanggal_bg, COALESCE(b.keterangan,''), "
							+ "COALESCE(b.oleh,''), b.kode_unik "
							+ "FROM koperasi.pembayaran_hutang_supplier b JOIN library.penyedia s ON b.supplier = s.id "
							+ "WHERE b.id = ?");
			ps.setLong(1, id.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close(); ps.close();
				tolak(hasil, "Pembayaran tidak ditemukan.");
				return;
			}
			JSONObject j = new JSONObject();
			j.put("id", rs.getLong(1));
			j.put("tanggal", str(rs.getTimestamp(2)));
			j.put("supplierKode", str(rs.getString(3)));
			j.put("supplierNama", str(rs.getString(4)));
			j.put("supplierAlamat", str(rs.getString(5)));
			j.put("nominal", rs.getDouble(6));
			j.put("metode", str(rs.getString(7)));
			j.put("noBg", str(rs.getString(8)));
			j.put("namaBank", str(rs.getString(9)));
			j.put("tanggalBg", str(rs.getDate(10)));
			j.put("keterangan", str(rs.getString(11)));
			j.put("oleh", str(rs.getString(12)));
			j.put("kodeUnik", str(rs.getString(13)));
			rs.close(); ps.close();

			java.sql.PreparedStatement psA = session.connection().prepareStatement(
					"SELECT COALESCE(f.nomor_faktur,'#' || f.id), f.tanggal_faktur, a.nominal, "
							+ EXPR_TOTAL.replace("f.", "f.") + ", i.jatuh_tempo "
							+ "FROM koperasi.alokasi_pembayaran_hutang_supplier a "
							+ "JOIN koperasi.pengadaan_faktur f ON a.pengadaan_faktur = f.id "
							+ "LEFT JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id "
							+ "WHERE a.pembayaran = ? ORDER BY f.tanggal_faktur ASC");
			psA.setLong(1, id.longValue());
			java.sql.ResultSet rsA = psA.executeQuery();
			JSONArray arrA = new JSONArray();
			while (rsA.next()) {
				JSONObject a = new JSONObject();
				a.put("nomorFaktur", str(rsA.getString(1)));
				a.put("tanggalFaktur", str(rsA.getTimestamp(2)));
				a.put("nominalAlokasi", rsA.getDouble(3));
				a.put("totalFaktur", rsA.getDouble(4));
				a.put("jatuhTempo", str(rsA.getDate(5)));
				arrA.put(a);
			}
			rsA.close(); psA.close();
			j.put("alokasi", arrA);
			hasil.put("status", "00");
			hasil.put("data", j);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-27: aging hutang (bucket saling eksklusif, jumlah = outstanding)
	// =============================================================================================

	public static void payableAging(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		String asOf = request == null ? "" : request.optString("as_of", "").trim();
		if (!asOf.matches("\\d{4}-\\d{2}-\\d{2}")) {
			asOf = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String umur = "(DATE '" + asOf + "' - i.jatuh_tempo)";
			String bucket = "CASE WHEN i.jatuh_tempo IS NULL OR i.jatuh_tempo >= DATE '" + asOf + "' THEN 'BELUM' "
					+ "WHEN " + umur + " <= 30 THEN 'B1_30' "
					+ "WHEN " + umur + " <= 60 THEN 'B31_60' "
					+ "WHEN " + umur + " <= 90 THEN 'B61_90' ELSE 'B90' END";
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT f.supplier, s.kode, s.nama, COALESCE(f.nomor_faktur,'#' || f.id), f.tanggal_faktur, "
							+ "i.jatuh_tempo, " + EXPR_OUTSTANDING + " AS outstanding, " + bucket + " AS bucket "
							+ "FROM koperasi.pengadaan_faktur f "
							+ "JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id "
							+ "JOIN library.penyedia s ON f.supplier = s.id "
							+ "WHERE i.jenis_pembayaran IN ('DP','CREDIT') AND " + EXPR_OUTSTANDING + " > 0.009 "
							+ "ORDER BY s.kode ASC, i.jatuh_tempo ASC NULLS FIRST LIMIT 2000");
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			double tBelum = 0, t130 = 0, t3160 = 0, t6190 = 0, t90 = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("supplierId", rs.getLong(1));
				j.put("supplierKode", str(rs.getString(2)));
				j.put("supplierNama", str(rs.getString(3)));
				j.put("nomorFaktur", str(rs.getString(4)));
				j.put("tanggalFaktur", str(rs.getTimestamp(5)));
				j.put("jatuhTempo", str(rs.getDate(6)));
				double outstanding = rs.getDouble(7);
				String b = str(rs.getString(8));
				j.put("outstanding", outstanding);
				j.put("bucket", b);
				if ("BELUM".equals(b)) tBelum += outstanding;
				else if ("B1_30".equals(b)) t130 += outstanding;
				else if ("B31_60".equals(b)) t3160 += outstanding;
				else if ("B61_90".equals(b)) t6190 += outstanding;
				else t90 += outstanding;
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("asOf", asOf);
			JSONObject ringkas = new JSONObject();
			ringkas.put("belumJatuhTempo", tBelum);
			ringkas.put("b1_30", t130);
			ringkas.put("b31_60", t3160);
			ringkas.put("b61_90", t6190);
			ringkas.put("b90", t90);
			ringkas.put("total", tBelum + t130 + t3160 + t6190 + t90);
			hasil.put("ringkasan", ringkas);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SCR-29: laporan pembelian per periode (+ breakdown cash/DP/kredit)
	// =============================================================================================

	public static void purchaseReport(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		String dari = request == null ? "" : request.optString("dari", "").trim();
		String sampai = request == null ? "" : request.optString("sampai", "").trim();
		if (!dari.matches("\\d{4}-\\d{2}-\\d{2}") || !sampai.matches("\\d{4}-\\d{2}-\\d{2}")) {
			tolak(hasil, "dari dan sampai (yyyy-MM-dd) wajib diisi.");
			return;
		}
		Long supplierId = optLong(request, "supplier_id");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(
					" WHERE f.tanggal_faktur BETWEEN CAST(? AS date) AND (CAST(? AS date) + interval '1 day') ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			params.add(dari); params.add(sampai);
			if (supplierId != null) {
				where.append(" AND f.supplier = ? ");
				params.add(supplierId);
			}
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					"SELECT f.id, COALESCE(f.nomor_faktur,'#' || f.id), f.tanggal_faktur, "
							+ "COALESCE(s.kode,''), COALESCE(s.nama,'(tanpa supplier)'), " + EXPR_TOTAL + ", "
							+ "COALESCE(i.jenis_pembayaran,'CASH'), COALESCE(i.dibayar_awal, " + EXPR_TOTAL + "), "
							+ EXPR_ALOKASI + ", COALESCE(f.diskon,0) "
							+ "FROM koperasi.pengadaan_faktur f "
							+ "LEFT JOIN library.penyedia s ON f.supplier = s.id "
							+ "LEFT JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id "
							+ where + " ORDER BY f.tanggal_faktur ASC, f.id ASC LIMIT 3000");
			for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			double totalSemua = 0, totalCash = 0, totalDp = 0, totalCredit = 0, totalSisa = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("fakturId", rs.getLong(1));
				j.put("nomorFaktur", str(rs.getString(2)));
				j.put("tanggalFaktur", str(rs.getTimestamp(3)));
				j.put("supplierKode", str(rs.getString(4)));
				j.put("supplierNama", str(rs.getString(5)));
				double total = rs.getDouble(6);
				String jenis = str(rs.getString(7));
				double dibayarAwal = rs.getDouble(8);
				double alokasi = rs.getDouble(9);
				double sisa = Math.max(0, total - dibayarAwal - alokasi);
				j.put("totalFaktur", total);
				j.put("jenisPembayaran", jenis);
				j.put("dibayar", dibayarAwal + alokasi);
				j.put("sisaHutang", sisa);
				j.put("diskon", rs.getDouble(10));
				totalSemua += total;
				totalSisa += sisa;
				if ("DP".equals(jenis)) totalDp += total;
				else if ("CREDIT".equals(jenis)) totalCredit += total;
				else totalCash += total;
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("dari", dari);
			hasil.put("sampai", sampai);
			JSONObject ringkas = new JSONObject();
			ringkas.put("total", totalSemua);
			ringkas.put("cash", totalCash);
			ringkas.put("dp", totalDp);
			ringkas.put("credit", totalCredit);
			ringkas.put("sisaHutang", totalSisa);
			hasil.put("ringkasan", ringkas);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
