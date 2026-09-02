package ais.action.servlet.api;

import java.math.BigDecimal;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CustomerInventoryProfile;
import ais.database.model.koperasi.SalesInventory;
import ais.database.model.koperasi.SupplierInventoryProfile;
import ais.database.model.library.Penyedia;

/**
 * <h3>Master varian "eBisnis Inventory &amp; Sales" -- Supplier / Customer / Sales (P2, layar 01-07).</h3>
 *
 * <p>Konvensi sama dgn {@code KantinHelper}: status {@code "00"} sukses / {@code "91"} gagal +
 * {@code description}; list ber-paginasi {@code page}/{@code page_size} (maks 100) dgn pencarian
 * server-side. Otorisasi DUA lapis: gate menu di {@code PosApi.bolehAksesActionKantin} + cek aktor/
 * aksi granular di sini ({@code ctx.bolehAksi(kunciMenu, aksi)}) -- Flutter hanya kosmetik.</p>
 *
 * <p>Prinsip master legacy (Panduan v2 / Matriks 48 layar): kode dipertahankan sebagai TEKS (nol di
 * depan tidak hilang, panjang legacy 3/5/2 karakter TIDAK dipaksakan ke data baru), duplikasi kode
 * ditolak, master berhistori DINONAKTIFKAN (bukan delete), perubahan rekening/bank tercatat audit
 * (Envers otomatis + kolom oleh/waktu).</p>
 */
public final class SalesInventoryMasterHelper {

	private SalesInventoryMasterHelper() {
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String opt(JSONObject r, String kunci) {
		return r == null || r.isNull(kunci) ? null : r.optString(kunci, "").trim();
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

	private static int[] halaman(JSONObject request) {
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 20 : request.optInt("page_size", 20);
		if (size < 1) size = 20;
		if (size > 100) size = 100;
		return new int[] { page, size, (page - 1) * size };
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static void isiOleh(Object entity, Tbmuser tbmuser) {
		try {
			java.lang.reflect.Method setOleh = entity.getClass().getMethod("setOleh", String.class);
			java.lang.reflect.Method setOlehId = entity.getClass().getMethod("setOlehId", String.class);
			String nama = null;
			try {
				Object n = tbmuser.getClass().getMethod("getNama").invoke(tbmuser);
				nama = n == null ? null : n.toString();
			} catch (Exception e) {
				// Tbmuser tanpa getNama -- pakai userId saja.
			}
			setOleh.invoke(entity, nama == null || nama.trim().isEmpty() ? tbmuser.getUserId() : nama);
			setOlehId.invoke(entity, tbmuser.getUserId());
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "SalesInventoryMasterHelper.isiOleh");
		}
	}

	// =============================================================================================
	// SUPPLIER (layar 01-03) -- Penyedia existing + SupplierInventoryProfile
	// =============================================================================================

	public static void supplierList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		int[] h = halaman(request);
		String keyword = opt(request, "keyword");
		String filterAktif = opt(request, "aktif"); // null/semua | aktif | nonaktif
		String sort = opt(request, "sort"); // kode | nama | wilayah

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (keyword != null && !keyword.isEmpty()) {
				where.append(" AND (p.kode ILIKE ? OR p.nama ILIKE ? OR COALESCE(p.alamat,'') ILIKE ? OR COALESCE(sp.wilayah,'') ILIKE ?) ");
				String k = "%" + keyword + "%";
				params.add(k); params.add(k); params.add(k); params.add(k);
			}
			if ("aktif".equals(filterAktif)) {
				where.append(" AND COALESCE(sp.aktif, true) = true ");
			} else if ("nonaktif".equals(filterAktif)) {
				where.append(" AND COALESCE(sp.aktif, true) = false ");
			}
			String orderBy = "p.kode ASC";
			if ("nama".equals(sort)) orderBy = "p.nama ASC";
			else if ("wilayah".equals(sort)) orderBy = "COALESCE(sp.wilayah,'') ASC, p.nama ASC";

			boolean jalurTenant = SalesInventoryMasterTenant.aktif(ctx);
			String dasar;
			String pilih;
			if (jalurTenant) {
				String sk = SalesInventoryMasterTenant.skema(ctx);
				dasar = SalesInventoryMasterTenant.dasarSupplier(sk, where.toString());
				pilih = SalesInventoryMasterTenant.selectSupplier(sk);
				orderBy = SalesInventoryMasterTenant.urutSupplier(sort);
			} else {
				dasar = " FROM library.penyedia p LEFT JOIN koperasi.supplier_inventory_profile sp ON sp.penyedia = p.id " + where;
				pilih = "SELECT p.id, p.kode, p.nama, p.alamat, p.telp, p.kontak, p.email, p.keterangan, "
						+ "sp.id, sp.termin_hari, sp.wilayah, sp.no_rekening, sp.atas_nama, sp.bank, sp.alamat_bank, "
						+ "COALESCE(sp.aktif, true), p.akun_utang, "
						+ "(SELECT ak.kode || ' ' || ak.nama FROM akunting.akun ak WHERE ak.id = p.akun_utang) ";
			}
			java.sql.PreparedStatement psTotal = session.connection()
					.prepareStatement("SELECT COUNT(*) " + dasar);
			for (int i = 0; i < params.size(); i++) psTotal.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsTotal = psTotal.executeQuery();
			long total = rsTotal.next() ? rsTotal.getLong(1) : 0;
			rsTotal.close(); psTotal.close();

			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					pilih + dasar + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?");
			int idx = 1;
			for (int i = 0; i < params.size(); i++) ps.setObject(idx++, params.get(i));
			ps.setInt(idx++, h[1]);
			ps.setInt(idx++, h[2]);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", str(rs.getString(2)));
				j.put("nama", str(rs.getString(3)));
				j.put("alamat", str(rs.getString(4)));
				j.put("telp", str(rs.getString(5)));
				j.put("kontak", str(rs.getString(6)));
				j.put("email", str(rs.getString(7)));
				j.put("keterangan", str(rs.getString(8)));
				long profilId = rs.getLong(9);
				j.put("profilId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(profilId));
				int termin = rs.getInt(10);
				j.put("terminHari", rs.wasNull() ? 0 : termin);
				j.put("wilayah", str(rs.getString(11)));
				j.put("noRekening", str(rs.getString(12)));
				j.put("atasNama", str(rs.getString(13)));
				j.put("bank", str(rs.getString(14)));
				j.put("alamatBank", str(rs.getString(15)));
				j.put("aktif", rs.getBoolean(16));
				// Akun utang dagang -- menentukan akun kredit saat kulakan kredit dijurnal.
				long akunUtang = rs.getLong(17);
				j.put("akunUtangId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(akunUtang));
				j.put("akunUtangLabel", str(rs.getString(18)));
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", h[0]);
			hasil.put("pageSize", h[1]);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void supplierDetail(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		Long id = optLong(request, "id");
		if (id == null) {
			tolak(hasil, "ID supplier wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			if (SalesInventoryMasterTenant.aktif(ctx)) {
				detailSupplierTenant(ctx, hasil, session, id);
				return;
			}
			Penyedia p = (Penyedia) session.get(Penyedia.class, id);
			if (p == null) {
				tolak(hasil, "Supplier tidak ditemukan.");
				return;
			}
			SupplierInventoryProfile sp = profilSupplier(session, p);
			JSONObject j = new JSONObject();
			j.put("id", p.getId());
			j.put("kode", str(p.getKode()));
			j.put("nama", str(p.getNama()));
			j.put("alamat", str(p.getAlamat()));
			j.put("kodePos", str(p.getKodePos()));
			j.put("telp", str(p.getTelp()));
			j.put("fax", str(p.getFax()));
			j.put("kontak", str(p.getKontak()));
			j.put("email", str(p.getEmail()));
			j.put("keterangan", str(p.getKeterangan()));
			j.put("profilId", sp == null ? JSONObject.NULL : sp.getId());
			j.put("terminHari", sp == null ? 0 : sp.getTerminHari().intValue());
			j.put("wilayah", sp == null ? "" : str(sp.getWilayah()));
			j.put("noRekening", sp == null ? "" : str(sp.getNoRekening()));
			j.put("atasNama", sp == null ? "" : str(sp.getAtasNama()));
			j.put("bank", sp == null ? "" : str(sp.getBank()));
			j.put("alamatBank", sp == null ? "" : str(sp.getAlamatBank()));
			j.put("aktif", sp == null || Boolean.TRUE.equals(sp.getAktif()));
			j.put("version", sp == null ? JSONObject.NULL : sp.getVersion());
			j.put("auditOleh", sp == null ? "" : str(sp.getOleh()));
			j.put("auditWaktu", sp == null || sp.getTanggal_dirubah() == null ? "" : sp.getTanggal_dirubah().toString());
			// Saldo hutang supplier -- NYATA dari ledger AP (P3): SUM outstanding seluruh faktur
			// DP/CREDIT supplier ini (formula sama si_payable_list). Baca-saja & direkonsiliasi.
			java.sql.PreparedStatement psHutang = session.connection().prepareStatement(
					"SELECT COALESCE(SUM(COALESCE(f.total_faktur_manual, COALESCE(f.total_hitung_saat_simpan,0)) "
							+ "- COALESCE(i.dibayar_awal,0) "
							+ "- COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_pembayaran_hutang_supplier a WHERE a.pengadaan_faktur = f.id),0)),0) "
							+ "FROM koperasi.pengadaan_faktur f "
							+ "JOIN koperasi.payable_faktur_info i ON i.pengadaan_faktur = f.id "
							+ "WHERE f.supplier = ? AND i.jenis_pembayaran IN ('DP','CREDIT')");
			psHutang.setLong(1, p.getId().longValue());
			java.sql.ResultSet rsHutang = psHutang.executeQuery();
			double saldoHutang = rsHutang.next() ? rsHutang.getDouble(1) : 0;
			rsHutang.close(); psHutang.close();
			j.put("saldoHutang", saldoHutang);
			hasil.put("status", "00");
			hasil.put("data", j);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static SupplierInventoryProfile profilSupplier(Session session, Penyedia p) {
		return (SupplierInventoryProfile) session.createCriteria(SupplierInventoryProfile.class)
				.add(Restrictions.eq("penyedia", p)).setMaxResults(1).uniqueResult();
	}

	public static void supplierSimpan(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		Long id = optLong(request, "id");
		boolean baru = id == null;
		if (!ctx.bolehAksi("master_supplier", baru ? "create" : "update")) {
			tolak(hasil, "Akun Anda tidak berhak " + (baru ? "menambah" : "mengubah") + " Master Supplier.");
			return;
		}
		String kode = opt(request, "kode");
		String nama = opt(request, "nama");
		if (baru && (kode == null || kode.isEmpty())) {
			tolak(hasil, "Kode supplier wajib diisi (teks, nol di depan dipertahankan).");
			return;
		}
		if (baru && (nama == null || nama.isEmpty())) {
			tolak(hasil, "Nama supplier wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			if (SalesInventoryMasterTenant.aktif(ctx)) {
				// Entitas Penyedia/SupplierInventoryProfile mematok schema-nya; jalur tenant
				// menulis lewat SQL asli agar tidak mendarat di schema bersama.
				simpanMitraTenant(ctx, tbmuser, request, hasil, session, true, id, kode, nama);
				return;
			}
			Penyedia p;
			if (baru) {
				Penyedia dobel = (Penyedia) session.createCriteria(Penyedia.class)
						.add(Restrictions.eq("kode", kode).ignoreCase()).setMaxResults(1).uniqueResult();
				if (dobel != null) {
					tolak(hasil, "Kode supplier \"" + kode + "\" sudah dipakai \"" + str(dobel.getNama())
							+ "\". Duplikasi ditolak -- gunakan record yang ada atau kode lain.");
					return;
				}
				p = new Penyedia();
				p.setKode(kode);
			} else {
				p = (Penyedia) session.get(Penyedia.class, id);
				if (p == null) {
					tolak(hasil, "Supplier tidak ditemukan.");
					return;
				}
				if (kode != null && !kode.isEmpty() && !kode.equals(p.getKode())) {
					// Kode kunci legacy: perubahan pada record berhistori berisiko memutus rekonsiliasi
					// arsip DBF -- ditolak; cleansing kode lewat proses khusus, bukan edit biasa.
					tolak(hasil, "Kode supplier tidak boleh diubah (kunci rekonsiliasi legacy).");
					return;
				}
			}
			if (nama != null && !nama.isEmpty()) p.setNama(nama);
			if (opt(request, "alamat") != null) p.setAlamat(opt(request, "alamat"));
			if (opt(request, "kode_pos") != null) p.setKodePos(opt(request, "kode_pos"));
			if (opt(request, "telp") != null) p.setTelp(opt(request, "telp"));
			if (opt(request, "fax") != null) p.setFax(opt(request, "fax"));
			if (opt(request, "kontak") != null) p.setKontak(opt(request, "kontak"));
			if (opt(request, "email") != null) p.setEmail(opt(request, "email"));
			if (opt(request, "keterangan") != null) p.setKeterangan(opt(request, "keterangan"));
			// Akun utang dagang: dipakai jurnal kulakan kredit & pembayaran hutang. Kirim null/0
			// untuk mengosongkan (nanti jatuh ke konfigurasi akun_utang_supplier_toko).
			if (request.has("akunUtangId")) {
				if (request.isNull("akunUtangId") || request.optLong("akunUtangId", 0) <= 0) {
					p.setAkunUtang(null);
				} else {
					p.setAkunUtang((ais.database.model.akunting.Akun) session.get(
							ais.database.model.akunting.Akun.class,
							Long.valueOf(request.optLong("akunUtangId"))));
				}
			}
			isiOleh(p, tbmuser);

			tx = session.beginTransaction();
			session.saveOrUpdate(p);
			SupplierInventoryProfile sp = profilSupplier(session, p);
			if (sp == null) {
				sp = new SupplierInventoryProfile();
				sp.setPenyedia(p);
			}
			if (!request.isNull("termin_hari")) sp.setTerminHari(Integer.valueOf(request.optInt("termin_hari", 0)));
			if (opt(request, "wilayah") != null) sp.setWilayah(opt(request, "wilayah"));
			if (opt(request, "no_rekening") != null || opt(request, "atas_nama") != null
					|| opt(request, "bank") != null || opt(request, "alamat_bank") != null) {
				// Perubahan rekening/bank = data sensitif (Matriks layar 01: perlu otorisasi) --
				// granular update Master Supplier sudah jadi gerbangnya; jejak tersimpan via Envers.
				if (opt(request, "no_rekening") != null) sp.setNoRekening(opt(request, "no_rekening"));
				if (opt(request, "atas_nama") != null) sp.setAtasNama(opt(request, "atas_nama"));
				if (opt(request, "bank") != null) sp.setBank(opt(request, "bank"));
				if (opt(request, "alamat_bank") != null) sp.setAlamatBank(opt(request, "alamat_bank"));
			}
			if (!request.isNull("aktif")) sp.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			isiOleh(sp, tbmuser);
			session.saveOrUpdate(sp);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", p.getId());
			hasil.put("profilId", sp.getId());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void supplierDeactivate(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehAksi("master_supplier", "deactivate") && !ctx.bolehAksi("master_supplier", "delete")) {
			tolak(hasil, "Akun Anda tidak berhak menonaktifkan Master Supplier.");
			return;
		}
		Long id = optLong(request, "id");
		String alasan = opt(request, "alasan");
		boolean aktifBaru = request != null && request.optBoolean("aktif", false);
		if (id == null) {
			tolak(hasil, "ID supplier wajib diisi.");
			return;
		}
		if (!aktifBaru && (alasan == null || alasan.isEmpty())) {
			tolak(hasil, "Alasan nonaktif wajib diisi (audit).");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			if (SalesInventoryMasterTenant.aktif(ctx)) {
				nonaktifkanTenant(ctx, tbmuser, hasil, session, "supplier", id, aktifBaru);
				return;
			}
			Penyedia p = (Penyedia) session.get(Penyedia.class, id);
			if (p == null) {
				tolak(hasil, "Supplier tidak ditemukan.");
				return;
			}
			tx = session.beginTransaction();
			SupplierInventoryProfile sp = profilSupplier(session, p);
			if (sp == null) {
				sp = new SupplierInventoryProfile();
				sp.setPenyedia(p);
			}
			sp.setAktif(Boolean.valueOf(aktifBaru));
			isiOleh(sp, tbmuser);
			session.saveOrUpdate(sp);
			tx.commit();
			System.out.println("[SI-SUPPLIER] " + (aktifBaru ? "AKTIFKAN" : "NONAKTIFKAN") + " penyedia="
					+ id + " oleh=" + tbmuser.getUserId() + " alasan=" + str(alasan));
			hasil.put("status", "00");
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// CUSTOMER (layar 04-06) -- AnggotaKoperasi existing + CustomerInventoryProfile
	// =============================================================================================

	public static void customerList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		int[] h = halaman(request);
		String keyword = opt(request, "keyword");
		String filterAktif = opt(request, "aktif");
		String sort = opt(request, "sort");
		Long salesOwnerId = optLong(request, "sales_owner_id");
		boolean hanyaProfil = request != null && request.optBoolean("hanya_profil", false);

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (keyword != null && !keyword.isEmpty()) {
				where.append(" AND (a.kode ILIKE ? OR a.nama ILIKE ? OR COALESCE(a.alamat,'') ILIKE ? "
						+ "OR COALESCE(a.telp,'') ILIKE ? OR COALESCE(a.hp,'') ILIKE ? OR COALESCE(cp.wilayah,'') ILIKE ?) ");
				String k = "%" + keyword + "%";
				for (int i = 0; i < 6; i++) params.add(k);
			}
			if ("aktif".equals(filterAktif)) {
				where.append(" AND COALESCE(cp.aktif, true) = true ");
			} else if ("nonaktif".equals(filterAktif)) {
				where.append(" AND COALESCE(cp.aktif, true) = false ");
			}
			if (salesOwnerId != null) {
				where.append(" AND cp.sales_owner = ? ");
				params.add(salesOwnerId);
			}
			if (hanyaProfil) {
				// Mode "customer distributor saja" (punya profil) -- default menampilkan SEMUA
				// anggota (member retail pun sah jadi customer, tinggal dilengkapi profilnya).
				where.append(" AND cp.id IS NOT NULL ");
			}
			String orderBy = "a.kode ASC";
			if ("nama".equals(sort)) orderBy = "a.nama ASC";
			else if ("wilayah".equals(sort)) orderBy = "COALESCE(cp.wilayah,'') ASC, a.nama ASC";

			boolean jalurTenant = SalesInventoryMasterTenant.aktif(ctx);
			String dasar;
			String pilihCust;
			String urutCust = " ORDER BY a.kode ASC LIMIT ? OFFSET ?";
			if (jalurTenant) {
				String sk = SalesInventoryMasterTenant.skema(ctx);
				dasar = SalesInventoryMasterTenant.dasarCustomer(sk, where.toString());
				pilihCust = SalesInventoryMasterTenant.selectCustomer(sk);
				urutCust = " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
			} else {
				dasar = " FROM koperasi.anggota_koperasi a "
						+ "LEFT JOIN koperasi.customer_inventory_profile cp ON cp.anggota_koperasi = a.id "
						+ "LEFT JOIN koperasi.sales_inventory s ON cp.sales_owner = s.id " + where;
				pilihCust = "SELECT a.id, a.kode, a.nama, a.alamat, a.telp, a.hp, a.limit_kredit, "
						+ "cp.id, cp.termin_hari, cp.diskon_default_persen, cp.wilayah, cp.sales_owner, s.nama, "
						+ "COALESCE(cp.aktif, true) ";
				urutCust = " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
			}
			java.sql.PreparedStatement psTotal = session.connection()
					.prepareStatement("SELECT COUNT(*) " + dasar);
			for (int i = 0; i < params.size(); i++) psTotal.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsTotal = psTotal.executeQuery();
			long total = rsTotal.next() ? rsTotal.getLong(1) : 0;
			rsTotal.close(); psTotal.close();

			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					pilihCust + dasar + urutCust);
			int idx = 1;
			for (int i = 0; i < params.size(); i++) ps.setObject(idx++, params.get(i));
			ps.setInt(idx++, h[1]);
			ps.setInt(idx++, h[2]);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("anggotaId", rs.getLong(1));
				j.put("kode", str(rs.getString(2)));
				j.put("nama", str(rs.getString(3)));
				j.put("alamat", str(rs.getString(4)));
				j.put("telp", str(rs.getString(5)));
				j.put("hp", str(rs.getString(6)));
				j.put("limitKredit", rs.getDouble(7));
				long profilId = rs.getLong(8);
				j.put("profilId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(profilId));
				int termin = rs.getInt(9);
				j.put("terminHari", rs.wasNull() ? 0 : termin);
				java.math.BigDecimal diskon = rs.getBigDecimal(10);
				j.put("diskonDefaultPersen", diskon == null ? 0 : diskon.doubleValue());
				j.put("wilayah", str(rs.getString(11)));
				long ownerId = rs.getLong(12);
				j.put("salesOwnerId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(ownerId));
				j.put("salesOwnerNama", str(rs.getString(13)));
				j.put("aktif", rs.getBoolean(14));
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", h[0]);
			hasil.put("pageSize", h[1]);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Saldo piutang customer (baca-saja) = DUA sub-ledger dijumlah (keputusan D-12, P4):
	 *  (1) ledger POS existing -- formula SAMA dgn KantinHelper.mutasiHutangList (belanja
	 *  ber-cara-bayar masuk_sebagai_hutang, 5 slot split-payment, minus pembayaran_hutang);
	 *  (2) outstanding faktur AR varian IS (piutang_customer_doc AKTIF: total - dibayar_awal -
	 *  alokasi penerimaan). Keduanya ledger terpisah tanpa duplikasi pencatatan. */
	private static double saldoPiutang(Session session, long anggotaId) throws Exception {
		String n1 = "GREATEST(0, COALESCE(h.total_biaya,0) - COALESCE(h.nominal_bayar_2,0) - COALESCE(h.nominal_bayar_3,0) - COALESCE(h.nominal_bayar_4,0) - COALESCE(h.nominal_bayar_5,0))";
		String[] slotJoin = { "h.cara_pembayaran_koperasi", "h.cara_pembayaran_koperasi_2",
				"h.cara_pembayaran_koperasi_3", "h.cara_pembayaran_koperasi_4", "h.cara_pembayaran_koperasi_5" };
		String[] slotNominal = { n1, "COALESCE(h.nominal_bayar_2,0)", "COALESCE(h.nominal_bayar_3,0)",
				"COALESCE(h.nominal_bayar_4,0)", "COALESCE(h.nominal_bayar_5,0)" };
		StringBuilder sql = new StringBuilder("SELECT COALESCE((SELECT SUM(x) FROM (");
		for (int slot = 1; slot <= 5; slot++) {
			if (slot > 1) sql.append(" UNION ALL ");
			sql.append("SELECT ").append(slotNominal[slot - 1]).append(" AS x FROM koperasi.pembelian_anggota_koperasi h ")
					.append("JOIN koperasi.cara_pembayaran_koperasi c").append(slot).append(" ON ")
					.append(slotJoin[slot - 1]).append(" = c").append(slot).append(".id ")
					.append("WHERE c").append(slot).append(".masuk_sebagai_hutang = true AND h.anggota_koperasi = ")
					.append(anggotaId).append(" AND ").append(slotNominal[slot - 1]).append(" > 0");
		}
		sql.append(") t),0) - COALESCE((SELECT SUM(nominal) FROM koperasi.pembayaran_hutang WHERE anggota_koperasi = ")
				.append(anggotaId).append("),0)")
				.append(" + COALESCE((SELECT SUM(COALESCE(d.total_faktur,0) - COALESCE(d.dibayar_awal,0)")
				.append(" - COALESCE((SELECT SUM(a.nominal) FROM koperasi.alokasi_penerimaan_piutang_customer a")
				.append(" WHERE a.piutang_doc = d.id),0)) FROM koperasi.piutang_customer_doc d")
				.append(" WHERE d.customer = ").append(anggotaId).append(" AND d.status = 'AKTIF'),0)");
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql.toString());
		java.sql.ResultSet rs = ps.executeQuery();
		double saldo = rs.next() ? rs.getDouble(1) : 0;
		rs.close(); ps.close();
		return saldo;
	}

	public static void customerDetail(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		Long anggotaId = optLong(request, "anggota_id");
		if (anggotaId == null) {
			tolak(hasil, "ID customer (anggota) wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			if (SalesInventoryMasterTenant.aktif(ctx)) {
				detailCustomerTenant(ctx, hasil, session, anggotaId);
				return;
			}
			AnggotaKoperasi a = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, anggotaId);
			if (a == null) {
				tolak(hasil, "Customer tidak ditemukan.");
				return;
			}
			CustomerInventoryProfile cp = profilCustomer(session, a);
			JSONObject j = new JSONObject();
			j.put("anggotaId", a.getId());
			j.put("kode", str(a.getKode()));
			j.put("nama", str(a.getNama()));
			j.put("alamat", str(a.getAlamat()));
			j.put("telp", str(a.getTelp()));
			j.put("hp", str(a.getHp()));
			j.put("email", str(a.getEmail()));
			j.put("limitKredit", a.getLimitKredit() == null ? 0 : a.getLimitKredit().doubleValue());
			j.put("profilId", cp == null ? JSONObject.NULL : cp.getId());
			j.put("terminHari", cp == null ? 0 : cp.getTerminHari().intValue());
			j.put("diskonDefaultPersen", cp == null ? 0 : cp.getDiskonDefaultPersen().doubleValue());
			j.put("wilayah", cp == null ? "" : str(cp.getWilayah()));
			j.put("noRekening", cp == null ? "" : str(cp.getNoRekening()));
			j.put("atasNama", cp == null ? "" : str(cp.getAtasNama()));
			j.put("bank", cp == null ? "" : str(cp.getBank()));
			Long ownerId = null;
			String ownerNama = "";
			if (cp != null && cp.getSalesOwner() != null) {
				try {
					ownerId = cp.getSalesOwner().getId();
					ownerNama = str(cp.getSalesOwner().getNama());
				} catch (Exception e) {
					// proxy lazy gagal -- biarkan kosong.
				}
			}
			j.put("salesOwnerId", ownerId == null ? JSONObject.NULL : ownerId);
			j.put("salesOwnerNama", ownerNama);
			j.put("aktif", cp == null || Boolean.TRUE.equals(cp.getAktif()));
			j.put("version", cp == null ? JSONObject.NULL : cp.getVersion());
			j.put("auditOleh", cp == null ? "" : str(cp.getOleh()));
			j.put("auditWaktu", cp == null || cp.getTanggal_dirubah() == null ? "" : cp.getTanggal_dirubah().toString());
			j.put("saldoPiutang", saldoPiutang(session, anggotaId.longValue()));
			hasil.put("status", "00");
			hasil.put("data", j);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static CustomerInventoryProfile profilCustomer(Session session, AnggotaKoperasi a) {
		return (CustomerInventoryProfile) session.createCriteria(CustomerInventoryProfile.class)
				.add(Restrictions.eq("anggotaKoperasi", a)).setMaxResults(1).uniqueResult();
	}

	public static void customerSimpan(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		Long anggotaId = optLong(request, "anggota_id");
		boolean baru = anggotaId == null;
		if (!ctx.bolehAksi("master_customer", baru ? "create" : "update")) {
			tolak(hasil, "Akun Anda tidak berhak " + (baru ? "menambah" : "mengubah") + " Master Customer.");
			return;
		}
		String kode = opt(request, "kode");
		String nama = opt(request, "nama");
		if (baru && (kode == null || kode.isEmpty())) {
			tolak(hasil, "Kode customer wajib diisi (teks, nol di depan dipertahankan).");
			return;
		}
		if (baru && (nama == null || nama.isEmpty())) {
			tolak(hasil, "Nama customer wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			if (SalesInventoryMasterTenant.aktif(ctx)) {
				simpanMitraTenant(ctx, tbmuser, request, hasil, session, false, anggotaId, kode, nama);
				return;
			}
			AnggotaKoperasi a;
			if (baru) {
				AnggotaKoperasi dobel = (AnggotaKoperasi) session.createCriteria(AnggotaKoperasi.class)
						.add(Restrictions.eq("kode", kode).ignoreCase()).setMaxResults(1).uniqueResult();
				if (dobel != null) {
					tolak(hasil, "Kode customer \"" + kode + "\" sudah dipakai \"" + str(dobel.getNama())
							+ "\". Duplikasi ditolak -- kode duplikat masuk proses cleansing, bukan digabung otomatis.");
					return;
				}
				a = new AnggotaKoperasi();
				a.setKode(kode);
			} else {
				a = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, anggotaId);
				if (a == null) {
					tolak(hasil, "Customer tidak ditemukan.");
					return;
				}
				if (kode != null && !kode.isEmpty() && !kode.equals(a.getKode())) {
					tolak(hasil, "Kode customer tidak boleh diubah (kunci rekonsiliasi legacy).");
					return;
				}
			}
			if (nama != null && !nama.isEmpty()) a.setNama(nama);
			if (opt(request, "alamat") != null) a.setAlamat(opt(request, "alamat"));
			if (opt(request, "telp") != null) a.setTelp(opt(request, "telp"));
			if (opt(request, "hp") != null) a.setHp(opt(request, "hp"));
			if (opt(request, "email") != null) a.setEmail(opt(request, "email"));
			if (!request.isNull("limit_kredit")) {
				a.setLimitKredit(Double.valueOf(request.optDouble("limit_kredit", 0)));
			}

			tx = session.beginTransaction();
			session.saveOrUpdate(a);
			CustomerInventoryProfile cp = profilCustomer(session, a);
			if (cp == null) {
				cp = new CustomerInventoryProfile();
				cp.setAnggotaKoperasi(a);
			}
			if (!request.isNull("termin_hari")) cp.setTerminHari(Integer.valueOf(request.optInt("termin_hari", 0)));
			BigDecimal diskon = optBigDecimal(request, "diskon_default_persen");
			if (diskon != null) cp.setDiskonDefaultPersen(diskon);
			if (opt(request, "wilayah") != null) cp.setWilayah(opt(request, "wilayah"));
			if (opt(request, "no_rekening") != null) cp.setNoRekening(opt(request, "no_rekening"));
			if (opt(request, "atas_nama") != null) cp.setAtasNama(opt(request, "atas_nama"));
			if (opt(request, "bank") != null) cp.setBank(opt(request, "bank"));
			if (!request.isNull("sales_owner_id")) {
				Long ownerId = optLong(request, "sales_owner_id");
				cp.setSalesOwner(ownerId == null ? null
						: (SalesInventory) session.get(SalesInventory.class, ownerId));
			}
			if (!request.isNull("aktif")) cp.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			isiOleh(cp, tbmuser);
			session.saveOrUpdate(cp);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("anggotaId", a.getId());
			hasil.put("profilId", cp.getId());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void customerDeactivate(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehAksi("master_customer", "deactivate") && !ctx.bolehAksi("master_customer", "delete")) {
			tolak(hasil, "Akun Anda tidak berhak menonaktifkan Master Customer.");
			return;
		}
		Long anggotaId = optLong(request, "anggota_id");
		String alasan = opt(request, "alasan");
		boolean aktifBaru = request != null && request.optBoolean("aktif", false);
		if (anggotaId == null) {
			tolak(hasil, "ID customer wajib diisi.");
			return;
		}
		if (!aktifBaru && (alasan == null || alasan.isEmpty())) {
			tolak(hasil, "Alasan nonaktif wajib diisi (audit).");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			if (SalesInventoryMasterTenant.aktif(ctx)) {
				nonaktifkanTenant(ctx, tbmuser, hasil, session, "customer", anggotaId, aktifBaru);
				return;
			}
			AnggotaKoperasi a = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, anggotaId);
			if (a == null) {
				tolak(hasil, "Customer tidak ditemukan.");
				return;
			}
			tx = session.beginTransaction();
			CustomerInventoryProfile cp = profilCustomer(session, a);
			if (cp == null) {
				cp = new CustomerInventoryProfile();
				cp.setAnggotaKoperasi(a);
			}
			// HANYA profil sales yang dinonaktifkan -- keanggotaan POS (AnggotaKoperasi.aktif)
			// TIDAK disentuh: customer distributor nonaktif boleh tetap jadi member retail.
			cp.setAktif(Boolean.valueOf(aktifBaru));
			isiOleh(cp, tbmuser);
			session.saveOrUpdate(cp);
			tx.commit();
			System.out.println("[SI-CUSTOMER] " + (aktifBaru ? "AKTIFKAN" : "NONAKTIFKAN") + " anggota="
					+ anggotaId + " oleh=" + tbmuser.getUserId() + " alasan=" + str(alasan));
			hasil.put("status", "00");
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// SALES (layar 07) -- SalesInventory (entity dari P1)
	// =============================================================================================

	public static void salesList(EbisnisActorContextResolver.ActorContext ctx, JSONObject request,
			JSONObject hasil) throws Exception {
		int[] h = halaman(request);
		String keyword = opt(request, "keyword");
		String filterAktif = opt(request, "aktif");
		Long idTunggal = optLong(request, "id"); // dipakai si_sales_detail (satu baris, field sama persis)
		Long tokoId = ctx.admin ? optLong(request, "toko_id") : ctx.tokoId;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder where = new StringBuilder(" WHERE 1=1 ");
			java.util.List<Object> params = new java.util.ArrayList<Object>();
			if (idTunggal != null) {
				where.append(" AND s.id = ? ");
				params.add(idTunggal);
			}
			if (keyword != null && !keyword.isEmpty()) {
				where.append(" AND (s.kode ILIKE ? OR s.nama ILIKE ? OR COALESCE(s.area,'') ILIKE ?) ");
				String k = "%" + keyword + "%";
				params.add(k); params.add(k); params.add(k);
			}
			if ("aktif".equals(filterAktif)) {
				where.append(" AND COALESCE(s.aktif, true) = true ");
			} else if ("nonaktif".equals(filterAktif)) {
				where.append(" AND COALESCE(s.aktif, true) = false ");
			}
			boolean jalurTenant = SalesInventoryMasterTenant.aktif(ctx);
			if (tokoId != null) {
				// Toko sales berada di penugasan pada model tenant, bukan kolom pada salesnya.
				where.append(jalurTenant
						? " AND " + SalesInventoryMasterTenant.kolomTokoSales() + " = ? "
						: " AND s.toko = ? ");
				params.add(tokoId);
			}
			String dasar;
			String pilihSales;
			if (jalurTenant) {
				String sk = SalesInventoryMasterTenant.skema(ctx);
				dasar = SalesInventoryMasterTenant.dasarSales(sk, where.toString());
				pilihSales = SalesInventoryMasterTenant.selectSales(sk);
			} else {
				dasar = " FROM koperasi.sales_inventory s LEFT JOIN koperasi.toko t ON s.toko = t.id " + where;
				pilihSales = "SELECT s.id, s.kode, s.nama, s.nomor_perkiraan, s.area, s.telepon, s.target_bulanan, "
						+ "s.limit_penagihan, COALESCE(s.aktif,true), s.toko, t.nama, s.tbmuser_id, "
						+ "(SELECT COUNT(*) FROM koperasi.customer_inventory_profile cip WHERE cip.sales_owner = s.id AND COALESCE(cip.aktif,true) = true) ";
			}
			java.sql.PreparedStatement psTotal = session.connection().prepareStatement("SELECT COUNT(*) " + dasar);
			for (int i = 0; i < params.size(); i++) psTotal.setObject(i + 1, params.get(i));
			java.sql.ResultSet rsTotal = psTotal.executeQuery();
			long total = rsTotal.next() ? rsTotal.getLong(1) : 0;
			rsTotal.close(); psTotal.close();

			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					pilihSales + dasar + " ORDER BY s.kode ASC LIMIT ? OFFSET ?");
			int idx = 1;
			for (int i = 0; i < params.size(); i++) ps.setObject(idx++, params.get(i));
			ps.setInt(idx++, h[1]);
			ps.setInt(idx++, h[2]);
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", str(rs.getString(2)));
				j.put("nama", str(rs.getString(3)));
				j.put("nomorPerkiraan", str(rs.getString(4)));
				j.put("area", str(rs.getString(5)));
				j.put("telepon", str(rs.getString(6)));
				java.math.BigDecimal target = rs.getBigDecimal(7);
				j.put("targetBulanan", target == null ? 0 : target.doubleValue());
				java.math.BigDecimal limit = rs.getBigDecimal(8);
				j.put("limitPenagihan", limit == null ? 0 : limit.doubleValue());
				j.put("aktif", rs.getBoolean(9));
				long tId = rs.getLong(10);
				j.put("tokoId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(tId));
				j.put("tokoNama", str(rs.getString(11)));
				j.put("userId", str(rs.getString(12)));
				j.put("jumlahCustomer", rs.getLong(13));
				arr.put(j);
			}
			rs.close(); ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", total);
			hasil.put("page", h[0]);
			hasil.put("pageSize", h[1]);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void salesSimpan(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		Long id = optLong(request, "id");
		boolean baru = id == null;
		if (!ctx.bolehAksi("master_sales", baru ? "create" : "update")) {
			tolak(hasil, "Akun Anda tidak berhak " + (baru ? "menambah" : "mengubah") + " Master Sales.");
			return;
		}
		String kode = opt(request, "kode");
		String nama = opt(request, "nama");
		if (baru && (kode == null || kode.isEmpty())) {
			tolak(hasil, "Kode sales wajib diisi (teks legacy 2 karakter dipertahankan apa adanya).");
			return;
		}
		if (baru && (nama == null || nama.isEmpty())) {
			tolak(hasil, "Nama sales wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			if (SalesInventoryMasterTenant.aktif(ctx)) {
				// salesperson + sales_assignment ditulis terpisah pada model tenant.
				// Toko dihitung di sini karena deklarasinya di jalur legacy berada di bawah.
				simpanSalesTenant(ctx, tbmuser, request, hasil, session, id, kode, nama,
						ctx.admin ? optLong(request, "toko_id") : ctx.tokoId);
				return;
			}
			Long tokoId = ctx.admin ? optLong(request, "toko_id") : ctx.tokoId;
			SalesInventory s;
			if (baru) {
				if (tokoId == null) {
					tolak(hasil, "Toko wajib ditentukan untuk profil sales (scope per toko).");
					return;
				}
				Toko toko = (Toko) session.get(Toko.class, tokoId);
				if (toko == null) {
					tolak(hasil, "Toko tidak ditemukan.");
					return;
				}
				SalesInventory dobel = (SalesInventory) session.createCriteria(SalesInventory.class)
						.add(Restrictions.eq("kode", kode).ignoreCase())
						.add(Restrictions.eq("toko", toko)).setMaxResults(1).uniqueResult();
				if (dobel != null) {
					tolak(hasil, "Kode sales \"" + kode + "\" sudah dipakai di toko ini.");
					return;
				}
				s = new SalesInventory();
				s.setKode(kode);
				s.setToko(toko);
			} else {
				s = (SalesInventory) session.get(SalesInventory.class, id);
				if (s == null) {
					tolak(hasil, "Sales tidak ditemukan.");
					return;
				}
				if (!ctx.admin && ctx.tokoId != null && s.getToko() != null
						&& !ctx.tokoId.equals(s.getToko().getId())) {
					tolak(hasil, "Sales ini milik toko lain (di luar scope Anda).");
					return;
				}
				if (kode != null && !kode.isEmpty() && !kode.equals(s.getKode())) {
					tolak(hasil, "Kode sales tidak boleh diubah (kunci rekonsiliasi legacy).");
					return;
				}
			}
			if (nama != null && !nama.isEmpty()) s.setNama(nama);
			if (opt(request, "nomor_perkiraan") != null) {
				// Mapping ke COA TIDAK divalidasi keras di P2 (uat-required #1) -- nilai legacy
				// disimpan apa adanya; validasi akun terjadi saat dipakai posting (P6).
				s.setNomorPerkiraan(opt(request, "nomor_perkiraan"));
			}
			if (opt(request, "area") != null) s.setArea(opt(request, "area"));
			if (opt(request, "telepon") != null) s.setTelepon(opt(request, "telepon"));
			if (opt(request, "alamat") != null) s.setAlamat(opt(request, "alamat"));
			BigDecimal target = optBigDecimal(request, "target_bulanan");
			if (target != null) s.setTargetBulanan(target);
			BigDecimal limit = optBigDecimal(request, "limit_penagihan");
			if (limit != null) s.setLimitPenagihan(limit);
			if (!request.isNull("tbmuser_id")) {
				String userIdBaru = opt(request, "tbmuser_id");
				if (userIdBaru == null || userIdBaru.isEmpty()) {
					s.setTbmuser(null);
				} else {
					Tbmuser akun = (Tbmuser) session.get(Tbmuser.class, userIdBaru);
					if (akun == null) {
						tolak(hasil, "Akun login \"" + userIdBaru + "\" tidak ditemukan.");
						return;
					}
					SalesInventory profilLain = (SalesInventory) session.createCriteria(SalesInventory.class)
							.add(Restrictions.eq("tbmuser", akun))
							.add(Restrictions.eq("aktif", Boolean.TRUE))
							.add(s.getId() == null ? Restrictions.isNotNull("id") : Restrictions.ne("id", s.getId()))
							.setMaxResults(1).uniqueResult();
					if (profilLain != null && Boolean.TRUE.equals(s.getAktif())) {
						tolak(hasil, "Akun \"" + userIdBaru + "\" sudah terikat profil sales aktif lain (kode "
								+ str(profilLain.getKode()) + "). Satu akun = satu profil sales aktif.");
						return;
					}
					s.setTbmuser(akun);
				}
			}
			if (!request.isNull("aktif")) s.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			isiOleh(s, tbmuser);
			tx = session.beginTransaction();
			session.saveOrUpdate(s);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", s.getId());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void salesDeactivate(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.bolehAksi("master_sales", "deactivate") && !ctx.bolehAksi("master_sales", "delete")) {
			tolak(hasil, "Akun Anda tidak berhak menonaktifkan Master Sales.");
			return;
		}
		Long id = optLong(request, "id");
		String alasan = opt(request, "alasan");
		boolean aktifBaru = request != null && request.optBoolean("aktif", false);
		if (id == null) {
			tolak(hasil, "ID sales wajib diisi.");
			return;
		}
		if (!aktifBaru && (alasan == null || alasan.isEmpty())) {
			tolak(hasil, "Alasan nonaktif wajib diisi (audit).");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			if (SalesInventoryMasterTenant.aktif(ctx)) {
				nonaktifkanTenant(ctx, tbmuser, hasil, session, "salesperson", id, aktifBaru);
				return;
			}
			SalesInventory s = (SalesInventory) session.get(SalesInventory.class, id);
			if (s == null) {
				tolak(hasil, "Sales tidak ditemukan.");
				return;
			}
			if (!ctx.admin && ctx.tokoId != null && s.getToko() != null
					&& !ctx.tokoId.equals(s.getToko().getId())) {
				tolak(hasil, "Sales ini milik toko lain (di luar scope Anda).");
				return;
			}
			tx = session.beginTransaction();
			s.setAktif(Boolean.valueOf(aktifBaru));
			isiOleh(s, tbmuser);
			session.saveOrUpdate(s);
			tx.commit();
			System.out.println("[SI-SALES] " + (aktifBaru ? "AKTIFKAN" : "NONAKTIFKAN") + " sales=" + id
					+ " oleh=" + tbmuser.getUserId() + " alasan=" + str(alasan));
			hasil.put("status", "00");
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// Jalur schema tenant -- SQL asli, sebab entitas mematok @Table(schema = ...)
	// =============================================================================================

	/** Benar bila kueri COUNT berparameter satu nilai menghasilkan lebih dari nol. */
	private static boolean adaSatu(Session session, String sql, Object... args) throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
		try {
			for (int i = 0; i < args.length; i++) {
				ps.setObject(i + 1, args[i]);
			}
			java.sql.ResultSet rs = ps.executeQuery();
			boolean ada = rs.next() && rs.getLong(1) > 0;
			rs.close();
			return ada;
		} finally {
			ps.close();
		}
	}

	/**
	 * Menonaktifkan/mengaktifkan mitra pada schema tenant.
	 *
	 * <p><b>Alasan nonaktif tidak tersimpan pada jalur tenant.</b> Jalur legacy mewajibkannya
	 * "untuk audit" dan menaruhnya pada profil; model tenant tidak menyediakan kolom untuk itu.
	 * Alasannya tetap <b>diwajibkan dan divalidasi</b> di sini supaya perilaku antarmuka tidak
	 * berubah, tetapi setelah itu tidak ke mana-mana.</p>
	 *
	 * <p>Inilah kebutuhan konkret pertama bagi {@code TenantAuditWriter}, yang sudah berdiri
	 * sejak P4 awal tetapi belum punya satu pun pemanggil. Menyambungkannya di sini akan memberi
	 * alasan itu tempat yang benar; sampai saat itu, keterbatasannya dicatat alih-alih
	 * disembunyikan.</p>
	 */
	/**
	 * Cuplikan satu baris master sebagai teks JSON, atau {@code null} bila barisnya belum/tidak
	 * ada. Nama kolomnya diambil dari metadata hasilnya, sehingga kuerinya yang menentukan isi
	 * muatan — bukan daftar kedua di sini yang bisa berselisih dengannya.
	 */
	private static String cuplikanAudit(Session session, String skema, String tabel, Long id)
			throws Exception {
		if (id == null) {
			return null;
		}
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				SalesInventoryMasterTenant.cuplikanAudit(skema, tabel));
		try {
			ps.setLong(1, id.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			String muatan = null;
			if (rs.next()) {
				java.sql.ResultSetMetaData md = rs.getMetaData();
				JSONObject o = new JSONObject();
				for (int i = 1; i <= md.getColumnCount(); i++) {
					Object v = rs.getObject(i);
					o.put(md.getColumnLabel(i), v == null ? JSONObject.NULL : String.valueOf(v));
				}
				muatan = o.toString();
			}
			rs.close();
			return muatan;
		} finally {
			ps.close();
		}
	}

	/**
	 * Tulis satu revisi audit tenant untuk satu baris master.
	 *
	 * <p>Berjalan pada Session dan transaksi pemanggil — tidak pernah membuka sendiri. Baris
	 * audit <b>wajib</b> berada di transaksi yang sama dengan perubahan datanya: audit yang
	 * commit terpisah dapat bertahan padahal perubahannya dibatalkan, atau hilang padahal
	 * perubahannya jadi.</p>
	 */
	private static void catatAuditMaster(Session session,
			EbisnisActorContextResolver.ActorContext ctx, String aksi, String tabel, Long id,
			int revtype, String sebelum, String sesudah) {
		ais.service.tenant.TenantAuditWriter.Jejak jejak =
				new ais.service.tenant.TenantAuditWriter.Jejak(aksi);
		ais.service.tenant.TenantAuditWriter.catatTunggal(session, ctx.tenant, jejak, tabel, id,
				revtype, sebelum, sesudah);
	}

	private static void nonaktifkanTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject hasil, Session session, String tabel, Long id,
			boolean aktifBaru) throws Exception {
		String sk = SalesInventoryMasterTenant.skema(ctx);
		String oleh = tbmuser == null || tbmuser.getUserId() == null ? "" : tbmuser.getUserId();
		if (!adaSatu(session, SalesInventoryMasterTenant.adaBaris(sk, tabel), id)) {
			tolak(hasil, "Data tidak ditemukan pada tenant ini.");
			return;
		}
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			String sebelum = cuplikanAudit(session, sk, tabel, id);
			java.sql.PreparedStatement ps = session.connection().prepareStatement(
					SalesInventoryMasterTenant.nonaktifkan(sk, tabel));
			try {
				ps.setBoolean(1, aktifBaru);
				ps.setString(2, oleh);
				ps.setLong(3, id.longValue());
				ps.executeUpdate();
			} finally {
				ps.close();
			}
			// Penonaktifan dicatat REVTYPE_DEL, bukan MOD: barisnya memang masih ada, tetapi
			// bagi pemakai data inilah "penghapusan"-nya — dan riwayat yang menyebutnya sekadar
			// perubahan satu kolom menyembunyikan peristiwa yang justru paling ingin ditelusuri.
			// Pengaktifan kembali tetap MOD.
			catatAuditMaster(session, ctx, aktifBaru ? "master_aktifkan" : "master_nonaktifkan",
					tabel, id,
					aktifBaru ? ais.service.tenant.TenantAuditWriter.REVTYPE_MOD
							: ais.service.tenant.TenantAuditWriter.REVTYPE_DEL,
					sebelum, cuplikanAudit(session, sk, tabel, id));
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", id);
			hasil.put("aktif", aktifBaru);
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

	/**
	 * Menyimpan supplier atau customer pada schema tenant: induk dan profilnya sekaligus.
	 *
	 * <p>Aturan legacy dipertahankan: kode wajib untuk data baru, dan kode ganda ditolak.
	 * Pembandingannya tidak peka huruf besar-kecil, sama seperti {@code ignoreCase()} pada
	 * jalur lama -- kalau tidak, KODE-1 dan kode-1 akan lolos sebagai dua mitra berbeda.</p>
	 */
	private static void simpanMitraTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject request, JSONObject hasil, Session session,
			boolean supplierMode, Long id, String kode, String nama) throws Exception {
		String sk = SalesInventoryMasterTenant.skema(ctx);
		String tabel = supplierMode ? "supplier" : "customer";
		String oleh = tbmuser == null || tbmuser.getUserId() == null ? "" : tbmuser.getUserId();
		if (kode != null && !kode.isEmpty()) {
			java.sql.PreparedStatement cek = session.connection().prepareStatement(
					"SELECT COUNT(*) FROM " + sk + tabel + " WHERE LOWER(kode) = LOWER(?)"
							+ " AND id <> COALESCE(?, -1)");
			boolean dobel;
			try {
				cek.setString(1, kode);
				if (id == null) {
					cek.setNull(2, java.sql.Types.BIGINT);
				} else {
					cek.setLong(2, id.longValue());
				}
				java.sql.ResultSet rs = cek.executeQuery();
				dobel = rs.next() && rs.getLong(1) > 0;
				rs.close();
			} finally {
				cek.close();
			}
			if (dobel) {
				tolak(hasil, "Kode " + kode + " sudah dipakai pada tenant ini. Duplikasi ditolak"
						+ " -- gunakan record yang ada atau kode lain.");
				return;
			}
		}
		if (id != null && !adaSatu(session, SalesInventoryMasterTenant.adaBaris(sk, tabel), id)) {
			tolak(hasil, "Data tidak ditemukan pada tenant ini.");
			return;
		}
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			String sebelum = cuplikanAudit(session, sk, tabel, id);
			Long idAkhir = id;
			if (id == null) {
				java.sql.PreparedStatement ins = session.connection().prepareStatement(
						SalesInventoryMasterTenant.sisipMitra(sk, tabel),
						java.sql.Statement.RETURN_GENERATED_KEYS);
				try {
					ins.setString(1, kode);
					ins.setString(2, nama);
					ins.setString(3, oleh);
					ins.executeUpdate();
					java.sql.ResultSet gk = ins.getGeneratedKeys();
					if (gk.next()) {
						idAkhir = Long.valueOf(gk.getLong(1));
					}
					gk.close();
				} finally {
					ins.close();
				}
			} else {
				java.sql.PreparedStatement upd = session.connection().prepareStatement(
						SalesInventoryMasterTenant.ubahMitra(sk, tabel));
				try {
					upd.setString(1, kode);
					upd.setString(2, nama);
					upd.setString(3, oleh);
					upd.setLong(4, id.longValue());
					upd.executeUpdate();
				} finally {
					upd.close();
				}
			}
			if (idAkhir == null) {
				tx.rollback();
				tolak(hasil, "Penyimpanan gagal: id tidak terbentuk.");
				return;
			}
			simpanProfilTenant(session, sk, request, supplierMode, idAkhir, oleh);
			catatAuditMaster(session, ctx, supplierMode ? "master_supplier_simpan"
					: "master_customer_simpan", tabel, idAkhir,
					id == null ? ais.service.tenant.TenantAuditWriter.REVTYPE_ADD
							: ais.service.tenant.TenantAuditWriter.REVTYPE_MOD,
					sebelum, cuplikanAudit(session, sk, tabel, idAkhir));
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", idAkhir);
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

	/** Upsert profil mitra: model tenant memisahkannya dari induk, jadi ditulis terpisah. */
	private static void simpanProfilTenant(Session session, String sk, JSONObject request,
			boolean supplierMode, Long indukId, String oleh) throws Exception {
		String tabel = supplierMode ? "supplier_profile" : "customer_profile";
		String kolomInduk = supplierMode ? "supplier_id" : "customer_id";
		boolean adaProfil;
		java.sql.PreparedStatement cari = session.connection().prepareStatement(
				SalesInventoryMasterTenant.adaProfil(sk, tabel, kolomInduk));
		try {
			cari.setLong(1, indukId.longValue());
			java.sql.ResultSet rs = cari.executeQuery();
			adaProfil = rs.next();
			rs.close();
		} finally {
			cari.close();
		}
		java.sql.PreparedStatement ps;
		if (supplierMode) {
			ps = session.connection().prepareStatement(adaProfil
					? SalesInventoryMasterTenant.ubahProfilSupplier(sk)
					: SalesInventoryMasterTenant.sisipProfilSupplier(sk));
		} else {
			ps = session.connection().prepareStatement(adaProfil
					? SalesInventoryMasterTenant.ubahProfilCustomer(sk)
					: SalesInventoryMasterTenant.sisipProfilCustomer(sk));
		}
		try {
			int i = 1;
			if (!adaProfil) {
				ps.setLong(i++, indukId.longValue());
			}
			if (supplierMode) {
				ps.setString(i++, opt(request, "alamat"));
				ps.setString(i++, opt(request, "telp"));
				ps.setString(i++, opt(request, "kontak"));
				ps.setString(i++, opt(request, "email"));
				ps.setInt(i++, request == null ? 0 : request.optInt("termin_hari", 0));
			} else {
				ps.setString(i++, opt(request, "alamat"));
				ps.setString(i++, opt(request, "telp"));
				ps.setInt(i++, request == null ? 0 : request.optInt("termin_hari", 0));
				ps.setBigDecimal(i++, new java.math.BigDecimal(String.valueOf(
						request == null ? 0 : request.optDouble("diskon_default_persen", 0))));
				ps.setBigDecimal(i++, new java.math.BigDecimal(String.valueOf(
						request == null ? 0 : request.optDouble("limit_kredit", 0))));
			}
			ps.setString(i++, oleh);
			if (adaProfil) {
				ps.setLong(i++, indukId.longValue());
			}
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}

	/**
	 * Menyimpan sales pada schema tenant.
	 *
	 * <p>Model tenant memecah data sales menjadi {@code salesperson} (identitas, akun perkiraan,
	 * telepon) dan {@code sales_assignment} (toko dan wilayah, berjangka waktu). Legacy menaruh
	 * semuanya pada satu tabel.</p>
	 *
	 * <p><b>Dua medan tidak punya padanan:</b> {@code alamat} dan {@code target_bulanan}. Keduanya
	 * diterima permintaan tetapi tidak disimpan. Menaruh target bulanan pada kolom lain akan
	 * membuat laporan pencapaian membandingkan angka terhadap sesuatu yang bukan targetnya.</p>
	 *
	 * <p>Penugasan toko/wilayah ditulis sebagai baris {@code sales_assignment} baru bila belum ada
	 * yang aktif, atau memperbarui yang aktif. Riwayat penugasan lama tidak ditimpa -- itu
	 * gunanya tabel berjangka waktu.</p>
	 */
	private static void simpanSalesTenant(EbisnisActorContextResolver.ActorContext ctx,
			Tbmuser tbmuser, JSONObject request, JSONObject hasil, Session session, Long id,
			String kode, String nama, Long tokoId) throws Exception {
		String sk = SalesInventoryMasterTenant.skema(ctx);
		String oleh = tbmuser == null || tbmuser.getUserId() == null ? "" : tbmuser.getUserId();
		if (kode != null && !kode.isEmpty()) {
			java.sql.PreparedStatement cek = session.connection().prepareStatement(
					"SELECT COUNT(*) FROM " + sk + "salesperson WHERE LOWER(kode) = LOWER(?)"
							+ " AND id <> COALESCE(?, -1)");
			boolean dobel;
			try {
				cek.setString(1, kode);
				if (id == null) {
					cek.setNull(2, java.sql.Types.BIGINT);
				} else {
					cek.setLong(2, id.longValue());
				}
				java.sql.ResultSet rs = cek.executeQuery();
				dobel = rs.next() && rs.getLong(1) > 0;
				rs.close();
			} finally {
				cek.close();
			}
			if (dobel) {
				tolak(hasil, "Kode sales " + kode + " sudah dipakai pada tenant ini.");
				return;
			}
		}
		if (id != null
				&& !adaSatu(session, SalesInventoryMasterTenant.adaBaris(sk, "salesperson"), id)) {
			tolak(hasil, "Sales tidak ditemukan pada tenant ini.");
			return;
		}
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			String sebelum = cuplikanAudit(session, sk, "salesperson", id);
			Long idAkhir = id;
			if (id == null) {
				java.sql.PreparedStatement ins = session.connection().prepareStatement(
						SalesInventoryMasterTenant.sisipSales(sk),
						java.sql.Statement.RETURN_GENERATED_KEYS);
				try {
					ins.setString(1, kode);
					ins.setString(2, nama);
					ins.setString(3, opt(request, "nomor_perkiraan"));
					ins.setString(4, opt(request, "telepon"));
					ins.setString(5, oleh);
					ins.executeUpdate();
					java.sql.ResultSet gk = ins.getGeneratedKeys();
					if (gk.next()) {
						idAkhir = Long.valueOf(gk.getLong(1));
					}
					gk.close();
				} finally {
					ins.close();
				}
			} else {
				java.sql.PreparedStatement upd = session.connection().prepareStatement(
						SalesInventoryMasterTenant.ubahSales(sk));
				try {
					upd.setString(1, kode);
					upd.setString(2, nama);
					upd.setString(3, opt(request, "nomor_perkiraan"));
					upd.setString(4, opt(request, "telepon"));
					upd.setString(5, oleh);
					upd.setLong(6, id.longValue());
					upd.executeUpdate();
				} finally {
					upd.close();
				}
			}
			if (idAkhir == null) {
				tx.rollback();
				tolak(hasil, "Penyimpanan gagal: id tidak terbentuk.");
				return;
			}
			simpanPenugasanTenant(session, sk, idAkhir, tokoId, opt(request, "area"), oleh);
			catatAuditMaster(session, ctx, "master_sales_simpan", "salesperson", idAkhir,
					id == null ? ais.service.tenant.TenantAuditWriter.REVTYPE_ADD
							: ais.service.tenant.TenantAuditWriter.REVTYPE_MOD,
					sebelum, cuplikanAudit(session, sk, "salesperson", idAkhir));
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", idAkhir);
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

	/**
	 * Penugasan toko/wilayah sales. Memperbarui penugasan aktif bila ada, atau membuat yang baru.
	 * Riwayat penugasan lama tidak disentuh.
	 */
	private static void simpanPenugasanTenant(Session session, String sk, Long salesId,
			Long tokoId, String wilayah, String oleh) throws Exception {
		if (tokoId == null && (wilayah == null || wilayah.isEmpty())) {
			return;
		}
		Long penugasanId = null;
		java.sql.PreparedStatement cari = session.connection().prepareStatement(
				"SELECT id FROM " + sk + "sales_assignment WHERE salesperson_id = ?"
						+ " AND COALESCE(aktif,true) = true ORDER BY id DESC LIMIT 1");
		try {
			cari.setLong(1, salesId.longValue());
			java.sql.ResultSet rs = cari.executeQuery();
			if (rs.next()) {
				penugasanId = Long.valueOf(rs.getLong(1));
			}
			rs.close();
		} finally {
			cari.close();
		}
		java.sql.PreparedStatement ps;
		if (penugasanId == null) {
			ps = session.connection().prepareStatement("INSERT INTO " + sk + "sales_assignment"
					+ " (salesperson_id, toko_id, wilayah, aktif, dibuat_pada, oleh)"
					+ " VALUES (?, ?, ?, true, now(), ?)");
		} else {
			ps = session.connection().prepareStatement("UPDATE " + sk + "sales_assignment"
					+ " SET toko_id = ?, wilayah = ?, tanggal_dirubah = now(), oleh = ? WHERE id = ?");
		}
		try {
			int i = 1;
			if (penugasanId == null) {
				ps.setLong(i++, salesId.longValue());
			}
			if (tokoId == null) {
				ps.setNull(i++, java.sql.Types.BIGINT);
			} else {
				ps.setLong(i++, tokoId.longValue());
			}
			ps.setString(i++, wilayah);
			ps.setString(i++, oleh);
			if (penugasanId != null) {
				ps.setLong(i++, penugasanId.longValue());
			}
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}

	/**
	 * Rinci pemasok pada schema tenant.
	 *
	 * <p>Jalur legacy merakit JSON dari getter entitas; entitasnya mematok schema, sehingga di
	 * sini dirakit dari {@code ResultSet}. Nama dan urutan medannya dipertahankan persis supaya
	 * layar tidak perlu tahu jalur mana yang melayaninya.</p>
	 *
	 * <p>{@code version}, {@code auditOleh}, dan {@code auditWaktu} berasal dari profil legacy
	 * yang tidak punya padanan; dikembalikan kosong.</p>
	 */
	private static void detailSupplierTenant(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject hasil, Session session, Long id) throws Exception {
		String sk = SalesInventoryMasterTenant.skema(ctx);
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				SalesInventoryMasterTenant.sqlDetailSupplier(sk));
		try {
			ps.setLong(1, id.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			try {
				if (!rs.next()) {
					tolak(hasil, "Supplier tidak ditemukan pada tenant ini.");
					return;
				}
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", str(rs.getString(2)));
				j.put("nama", str(rs.getString(3)));
				j.put("alamat", str(rs.getString(4)));
				j.put("kodePos", str(rs.getString(5)));
				j.put("telp", str(rs.getString(6)));
				j.put("fax", str(rs.getString(7)));
				j.put("kontak", str(rs.getString(8)));
				j.put("email", str(rs.getString(9)));
				j.put("keterangan", str(rs.getString(10)));
				long profilId = rs.getLong(11);
				j.put("profilId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(profilId));
				j.put("terminHari", rs.getInt(12));
				j.put("wilayah", str(rs.getString(13)));
				j.put("noRekening", str(rs.getString(14)));
				j.put("atasNama", str(rs.getString(15)));
				j.put("bank", str(rs.getString(16)));
				j.put("alamatBank", str(rs.getString(17)));
				j.put("aktif", rs.getBoolean(18));
				j.put("saldoHutang", rs.getDouble(19));
				j.put("version", JSONObject.NULL);
				j.put("auditOleh", "");
				j.put("auditWaktu", "");
				hasil.put("status", "00");
				hasil.put("data", j);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
	}

	/**
	 * Rinci pelanggan pada schema tenant. Lihat catatan pada {@link #detailSupplierTenant};
	 * alasan dan bentuknya sama.
	 *
	 * <p>Rekening bank pelanggan tidak punya padanan pada model tenant sama sekali, sehingga
	 * ketiga medannya kosong.</p>
	 */
	private static void detailCustomerTenant(EbisnisActorContextResolver.ActorContext ctx,
			JSONObject hasil, Session session, Long id) throws Exception {
		String sk = SalesInventoryMasterTenant.skema(ctx);
		java.sql.PreparedStatement ps = session.connection().prepareStatement(
				SalesInventoryMasterTenant.sqlDetailCustomer(sk));
		try {
			ps.setLong(1, id.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			try {
				if (!rs.next()) {
					tolak(hasil, "Customer tidak ditemukan pada tenant ini.");
					return;
				}
				JSONObject j = new JSONObject();
				j.put("anggotaId", rs.getLong(1));
				j.put("kode", str(rs.getString(2)));
				j.put("nama", str(rs.getString(3)));
				j.put("alamat", str(rs.getString(4)));
				j.put("telp", str(rs.getString(5)));
				j.put("hp", str(rs.getString(6)));
				j.put("email", str(rs.getString(7)));
				j.put("limitKredit", rs.getDouble(8));
				long profilId = rs.getLong(9);
				j.put("profilId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(profilId));
				j.put("terminHari", rs.getInt(10));
				j.put("diskonDefaultPersen", rs.getDouble(11));
				j.put("wilayah", str(rs.getString(12)));
				j.put("noRekening", str(rs.getString(13)));
				j.put("atasNama", str(rs.getString(14)));
				j.put("bank", str(rs.getString(15)));
				j.put("salesOwnerId", rs.getLong(16));
				j.put("salesOwnerNama", str(rs.getString(17)));
				j.put("aktif", rs.getBoolean(18));
				j.put("saldoPiutang", rs.getDouble(19));
				j.put("version", JSONObject.NULL);
				j.put("auditOleh", "");
				j.put("auditWaktu", "");
				hasil.put("status", "00");
				hasil.put("data", j);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
	}
}
