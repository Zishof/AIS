package ais.action.servlet.api;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.ConstantValues;
import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sirs.ApotikBatchKonsumsi;
import ais.database.model.sirs.ApotikItemProfile;
import ais.database.model.sirs.ApotikNarkotikaLog;
import ais.database.model.sirs.ApotikPembayaranTransaksi;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Kadaluarsa;
import ais.database.model.sirs.Resep;
import ais.database.model.sirs.ResepDetail;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;

/**
 * <h3>API kasir "POS Apotik" (FASE A) -- membungkus modul SIRS existing, TIDAK menyalin logikanya.</h3>
 *
 * <p>Fakta survei yang jadi dasar (path di komentar per method): stok item = ledger
 * {@code sirs.detail_transaksi_pasien} x tanda {@code kode_transaksi_medis.jenis} (rumus PERSIS
 * dari {@code AmbilDataItemMedisBanyakBerdasarkanStok}); batch-kedaluwarsa = {@code sirs.kadaluarsa}
 * (qty per batch saat diterima); penjualan existing TIDAK memvalidasi kedaluwarsa -- validasi itu
 * (plus konsumsi batch {@link ApotikBatchKonsumsi} dan register terkendali
 * {@link ApotikNarkotikaLog}) adalah kontribusi FASE A.</p>
 *
 * <p>Aturan keras FASE A: obat kedaluwarsa TIDAK BISA terjual (ditolak server, bukan
 * peringatan); obat terkendali tanpa data register = SELURUH transaksi ditahan (rollback).</p>
 *
 * <p>Keterbatasan yang DISENGAJA di FASE A (bukan kelupaan): racikan belum bisa dijual lewat
 * jalur ini (ditolak dgn pesan jelas); entity {@code Pembayaran} SIRS belum dibuat oleh
 * {@code apotik_bayar} (transaksi dicatat lunas tunai + keterangan -- integrasi kasir Pembayaran
 * menyusul fase kasir medis). Keduanya tercatat di respons/UAT.</p>
 */
public final class ApotikApiHelper {

	private ApotikApiHelper() {
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

	/** Awal hari ini (00:00) -- batch bertanggal kadaluarsa SEBELUM hari ini = kedaluwarsa. */
	private static Date awalHariIni() {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	private static boolean kedaluwarsa(Kadaluarsa k) {
		return k.getTanggalKadaluarsa() != null && k.getTanggalKadaluarsa().before(awalHariIni());
	}

	/** Aksi granular menu apotik -- fail-closed: crud kunci apotik default FALSE (lihat
	 *  EbisnisMenuKatalog.defaultObj + KUNCI_DEFAULT_NONAKTIF). Admin global (tanpa role) boleh. */
	private static boolean bolehAksi(Tbmuser tbmuser, String kunciMenu, String aksi) {
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		return EbisnisMenuKatalog.bolehAksi(EbisnisMenuKatalog.urai(role.getEbisnisMenu()), kunciMenu, aksi);
	}

	/** Stok per item dari ledger -- SQL PERSIS pola AmbilDataItemMedisBanyakBerdasarkanStok
	 *  (sirs/helper, baris 340-346): SUM((qty+qty_bonus)*jenis), opsional per lokasi.
	 *  Package-visible: dipakai juga ApotikPersediaanHelper (opname hitung selisih). */
	/* package */ static java.util.Map<Long, Double> stokPerItem(Session session, List<Long> itemIds, Long lokasiId)
			throws Exception {
		java.util.Map<Long, Double> stok = new java.util.HashMap<Long, Double>();
		if (itemIds.isEmpty()) {
			return stok;
		}
		StringBuilder in = new StringBuilder();
		for (int i = 0; i < itemIds.size(); i++) {
			if (i > 0) in.append(",");
			in.append(itemIds.get(i));
		}
		String sql = "select a.item, sum((a.qty+a.qty_bonus)*b.jenis) from sirs.detail_transaksi_pasien a "
				+ "inner join sirs.kode_transaksi_medis b on (a.kode_transaksi = b.id) "
				+ "where a.item in (" + in + ") "
				+ (lokasiId == null ? "" : " and a.lokasi = " + lokasiId + " ")
				+ "group by a.item";
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
		java.sql.ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			stok.put(Long.valueOf(rs.getLong(1)), Double.valueOf(rs.getDouble(2)));
		}
		rs.close();
		ps.close();
		return stok;
	}

	/** Konsumsi batch ter-agregasi per kadaluarsa id (sisa = Kadaluarsa.qty - nilai peta ini). */
	/* package */ static java.util.Map<Long, Double> konsumsiPerBatch(Session session, List<Long> kadaluarsaIds) {
		java.util.Map<Long, Double> peta = new java.util.HashMap<Long, Double>();
		if (kadaluarsaIds.isEmpty()) {
			return peta;
		}
		@SuppressWarnings("unchecked")
		List<Object[]> rows = session.createQuery(
				"select bk.kadaluarsa.id, sum(bk.qty) from ApotikBatchKonsumsi bk "
						+ "where bk.kadaluarsa.id in (:ids) group by bk.kadaluarsa.id")
				.setParameterList("ids", kadaluarsaIds).list();
		for (Object[] row : rows) {
			peta.put((Long) row[0], row[1] == null ? Double.valueOf(0) : Double.valueOf(((Number) row[1]).doubleValue()));
		}
		return peta;
	}

	private static ApotikItemProfile profilItem(Session session, ItemMedis item) {
		return (ApotikItemProfile) session.createCriteria(ApotikItemProfile.class)
				.add(Restrictions.eq("item", item)).setMaxResults(1).uniqueResult();
	}

	// =============================================================================================
	// apotik_item_cari -- pencarian obat + stok + profil golongan/LASA (kasir & formularium)
	// =============================================================================================

	public static void itemCari(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request == null ? "" : request.optString("keyword", "").trim();
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 20 : request.optInt("page_size", 20);
		if (size < 1) size = 20;
		if (size > 100) size = 100;
		Long lokasiId = optLong(request, "lokasi_id");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(ItemMedis.class);
			if (!keyword.isEmpty()) {
				c.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", "%" + keyword + "%"))
						.add(Restrictions.ilike("barcode", "%" + keyword + "%"))
						.add(Restrictions.ilike("nama", "%" + keyword + "%")));
			}
			c.addOrder(Order.asc("nama"));
			c.setFirstResult((page - 1) * size);
			c.setMaxResults(size);
			@SuppressWarnings("unchecked")
			List<ItemMedis> items = c.list();

			List<Long> ids = new java.util.ArrayList<Long>();
			for (ItemMedis it : items) {
				ids.add(it.getId());
			}
			java.util.Map<Long, Double> stok = stokPerItem(session, ids, lokasiId);
			java.util.Map<Long, ApotikItemProfile> profil = new java.util.HashMap<Long, ApotikItemProfile>();
			if (!ids.isEmpty()) {
				@SuppressWarnings("unchecked")
				List<ApotikItemProfile> profiles = session.createCriteria(ApotikItemProfile.class)
						.createAlias("item", "item").add(Restrictions.in("item.id", ids)).list();
				for (ApotikItemProfile p : profiles) {
					profil.put(p.getItem().getId(), p);
				}
			}

			JSONArray arr = new JSONArray();
			for (ItemMedis it : items) {
				ApotikItemProfile p = profil.get(it.getId());
				JSONObject j = new JSONObject();
				j.put("id", it.getId());
				j.put("kode", str(it.getKode()));
				j.put("barcode", str(it.getBarcode()));
				j.put("nama", str(it.getNama()));
				j.put("satuan", it.getSatuanItem() == null ? "" : str(it.getSatuanItem().getNama()));
				j.put("kandungan", str(it.getKandungan()));
				j.put("hargaJual", it.getDefaultHargaJual() == null ? 0 : it.getDefaultHargaJual().doubleValue());
				j.put("stok", stok.containsKey(it.getId()) ? stok.get(it.getId()).doubleValue() : 0);
				j.put("golonganObat", p == null ? ApotikItemProfile.GOLONGAN_BEBAS : p.getGolonganObat());
				j.put("terkendali", p != null && ApotikItemProfile.terkendali(p.getGolonganObat()));
				j.put("lasa", p != null && Boolean.TRUE.equals(p.getLasa()));
				// IR-01: atribut pembeda & penanda risiko utk kartu obat kasir.
				j.put("bentukSediaan", p == null ? "" : str(p.getBentukSediaan()));
				j.put("kekuatan", p == null ? "" : str(p.getKekuatan()));
				j.put("highAlert", p != null && Boolean.TRUE.equals(p.getHighAlert()));
				j.put("coldChain", p != null && Boolean.TRUE.equals(p.getColdChain()));
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("page", page);
			hasil.put("pageSize", size);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_item_batch -- daftar batch per item, urut FEFO; kedaluwarsa ditandai TAK BISA dipilih
	// =============================================================================================

	/**
	 * IR-07 -- daftar metode pembayaran yang boleh dipakai kasir apotik.
	 *
	 * <p>Memakai ULANG master {@code CaraPembayaranKoperasi} milik POS (tidak
	 * membuat master baru yang harus dipelihara terpisah): hanya yang berstatus
	 * aktif yang dikirim. UI WAJIB menampilkan metode dari daftar ini saja --
	 * tidak boleh menampilkan metode yang tidak dikonfigurasi server.</p>
	 */
	public static void caraBayarList(JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			List<ais.database.model.koperasi.CaraPembayaranKoperasi> daftar = session
					.createCriteria(ais.database.model.koperasi.CaraPembayaranKoperasi.class)
					.add(Restrictions.eq("aktif", Boolean.TRUE))
					.addOrder(Order.asc("nama")).list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.koperasi.CaraPembayaranKoperasi cb : daftar) {
				JSONObject j = new JSONObject();
				j.put("id", cb.getId());
				j.put("kode", str(cb.getKode()));
				j.put("nama", str(cb.getNama()));
				j.put("manual", Boolean.TRUE.equals(cb.getManual()));
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void itemBatch(JSONObject request, JSONObject hasil) throws Exception {
		Long itemId = optLong(request, "item_id");
		Long lokasiId = optLong(request, "lokasi_id");
		if (itemId == null) {
			tolak(hasil, "item_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ItemMedis item = (ItemMedis) session.get(ItemMedis.class, itemId);
			if (item == null) {
				tolak(hasil, "Item tidak ditemukan.");
				return;
			}
			org.hibernate.Criteria c = session.createCriteria(Kadaluarsa.class)
					.add(Restrictions.eq("item", item));
			if (lokasiId != null) {
				c.createAlias("lokasi", "lokasi").add(Restrictions.eq("lokasi.id", lokasiId));
			}
			c.addOrder(Order.asc("tanggalKadaluarsa")); // FEFO: terdekat kedaluwarsa didahulukan
			@SuppressWarnings("unchecked")
			List<Kadaluarsa> batches = c.list();
			List<Long> ids = new java.util.ArrayList<Long>();
			for (Kadaluarsa k : batches) {
				ids.add(k.getId());
			}
			java.util.Map<Long, Double> konsumsi = konsumsiPerBatch(session, ids);
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
			JSONArray arr = new JSONArray();
			for (Kadaluarsa k : batches) {
				double awal = k.getQty() == null ? 0 : k.getQty().doubleValue();
				Double pakai = konsumsi.get(k.getId());
				double sisa = awal - (pakai == null ? 0 : pakai.doubleValue());
				JSONObject j = new JSONObject();
				j.put("kadaluarsaId", k.getId());
				j.put("tanggalKadaluarsa",
						k.getTanggalKadaluarsa() == null ? "" : fmt.format(k.getTanggalKadaluarsa()));
				j.put("qtyAwal", awal);
				j.put("sisa", sisa);
				j.put("lokasiId", k.getLokasi() == null ? JSONObject.NULL : k.getLokasi().getId());
				j.put("kedaluwarsa", kedaluwarsa(k));
				// IR-02: status lot + alasan manusiawi bila tidak dapat dipilih.
				j.put("statusLot", k.getStatusLot());
				j.put("lotLayak", Kadaluarsa.lotLayak(k.getStatusLot()));
				String alasanLot = Kadaluarsa.alasanLotDitahan(k.getStatusLot());
				j.put("alasanLot", alasanLot == null ? "" : alasanLot);
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_resep_list / apotik_resep_detail -- tebus resep: pilih resep, bukan ketik obat
	// =============================================================================================

	public static void resepList(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request == null ? "" : request.optString("keyword", "").trim();
		boolean hanyaMenunggu = request == null || request.optBoolean("hanya_menunggu", true);
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 20 : request.optInt("page_size", 20);
		if (size < 1) size = 20;
		if (size > 100) size = 100;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(Resep.class);
			if (!keyword.isEmpty()) {
				c.add(Restrictions.ilike("kode", "%" + keyword + "%"));
			}
			c.addOrder(Order.desc("id"));
			c.setFirstResult((page - 1) * size);
			c.setMaxResults(size);
			@SuppressWarnings("unchecked")
			List<Resep> reseps = c.list();

			// Status "sudah ditebus" = sudah ada TransaksiMedis yang menunjuk resep itu
			// (FACT_SOURCE: TransaksiMedis.resep FK; Resep sendiri tanpa kolom status).
			java.util.Set<Long> sudahDitebus = new java.util.HashSet<Long>();
			if (!reseps.isEmpty()) {
				List<Long> ids = new java.util.ArrayList<Long>();
				for (Resep r : reseps) {
					ids.add(r.getId());
				}
				@SuppressWarnings("unchecked")
				List<Long> tebus = session.createQuery(
						"select distinct tm.resep.id from TransaksiMedis tm where tm.resep.id in (:ids)")
						.setParameterList("ids", ids).list();
				sudahDitebus.addAll(tebus);
			}

			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
			JSONArray arr = new JSONArray();
			for (Resep r : reseps) {
				boolean ditebus = sudahDitebus.contains(r.getId());
				if (hanyaMenunggu && ditebus) {
					continue;
				}
				JSONObject j = new JSONObject();
				j.put("id", r.getId());
				j.put("kode", str(r.getKode()));
				j.put("keterangan", str(r.getKeterangan()));
				j.put("ditebus", ditebus);
				long jumlahBaris = ((Number) session.createQuery(
						"select count(rd) from ResepDetail rd where rd.resep.id = :id")
						.setParameter("id", r.getId()).uniqueResult()).longValue();
				j.put("jumlahBaris", jumlahBaris);
				try {
					j.put("diagnosa", r.getDiagnosaPenyakit() == null ? "" : str(r.getDiagnosaPenyakit()));
				} catch (Exception e) {
					j.put("diagnosa", "");
				}
				j.put("oleh", str(r.getOleh()));
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("page", page);
			hasil.put("pageSize", size);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void resepDetail(JSONObject request, JSONObject hasil) throws Exception {
		Long resepId = optLong(request, "resep_id");
		if (resepId == null) {
			tolak(hasil, "resep_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Resep resep = (Resep) session.get(Resep.class, resepId);
			if (resep == null) {
				tolak(hasil, "Resep tidak ditemukan.");
				return;
			}
			@SuppressWarnings("unchecked")
			List<ResepDetail> details = session.createCriteria(ResepDetail.class)
					.add(Restrictions.eq("resep", resep)).addOrder(Order.asc("id")).list();
			boolean adaRacikan = false;
			List<Long> itemIds = new java.util.ArrayList<Long>();
			for (ResepDetail d : details) {
				if (d.getItem() != null) {
					itemIds.add(d.getItem().getId());
				}
			}
			java.util.Map<Long, Double> stok = stokPerItem(session, itemIds, null);
			JSONArray arr = new JSONArray();
			for (ResepDetail d : details) {
				JSONObject j = new JSONObject();
				j.put("resepDetailId", d.getId());
				j.put("jumlah", d.getJumlah() == null ? 0 : d.getJumlah().doubleValue());
				j.put("keterangan", str(d.getKeterangan()));
				if (d.getRacikan() != null) {
					adaRacikan = true;
					j.put("racikan", true);
					j.put("nama", str(d.getRacikan().getNama()));
					j.put("kode", str(d.getRacikan().getKode()));
				} else if (d.getItem() != null) {
					ItemMedis it = d.getItem();
					ApotikItemProfile p = profilItem(session, it);
					j.put("racikan", false);
					j.put("itemId", it.getId());
					j.put("kode", str(it.getKode()));
					j.put("nama", str(it.getNama()));
					j.put("satuan", it.getSatuanItem() == null ? "" : str(it.getSatuanItem().getNama()));
					j.put("hargaJual", it.getDefaultHargaJual() == null ? 0 : it.getDefaultHargaJual().doubleValue());
					j.put("stok", stok.containsKey(it.getId()) ? stok.get(it.getId()).doubleValue() : 0);
					j.put("golonganObat", p == null ? ApotikItemProfile.GOLONGAN_BEBAS : p.getGolonganObat());
					j.put("terkendali", p != null && ApotikItemProfile.terkendali(p.getGolonganObat()));
					j.put("bentukSediaan", p == null ? "" : str(p.getBentukSediaan()));
					j.put("kekuatan", p == null ? "" : str(p.getKekuatan()));
					j.put("highAlert", p != null && Boolean.TRUE.equals(p.getHighAlert()));
					j.put("coldChain", p != null && Boolean.TRUE.equals(p.getColdChain()));
					j.put("lasa", p != null && Boolean.TRUE.equals(p.getLasa()));
				}
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("resepId", resep.getId());
			hasil.put("kode", str(resep.getKode()));
			hasil.put("data", arr);
			// Keterbatasan FASE A yang DISENGAJA: penyerahan racikan butuh konsumsi komposisi
			// (BOM) -- menyusul; klien wajib menampilkan ini, bukan diam-diam melewatkan baris.
			hasil.put("adaRacikan", adaRacikan);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_item_profil_simpan -- golongan obat + LASA (formularium)
	// =============================================================================================

	public static void itemProfilSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if (!bolehAksi(tbmuser, "apotik_formularium", "update")
				&& !bolehAksi(tbmuser, "apotik_formularium", "create")) {
			tolak(hasil, "Akun Anda tidak berhak mengubah profil obat (Formularium).");
			return;
		}
		Long itemId = optLong(request, "item_id");
		String golongan = request == null ? null : request.optString("golongan_obat", "").trim();
		if (itemId == null) {
			tolak(hasil, "item_id wajib diisi.");
			return;
		}
		if (golongan == null || golongan.isEmpty() || !ApotikItemProfile.golonganValid(golongan)) {
			tolak(hasil, "golongan_obat wajib salah satu: BEBAS, BEBAS_TERBATAS, KERAS, NARKOTIKA, PSIKOTROPIKA.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			ItemMedis item = (ItemMedis) session.get(ItemMedis.class, itemId);
			if (item == null) {
				tolak(hasil, "Item tidak ditemukan.");
				return;
			}
			tx = session.beginTransaction();
			ApotikItemProfile p = profilItem(session, item);
			if (p == null) {
				p = new ApotikItemProfile();
				p.setItem(item);
			}
			p.setGolonganObat(golongan);
			if (!request.isNull("lasa")) {
				p.setLasa(Boolean.valueOf(request.optBoolean("lasa", false)));
			}
			if (!request.isNull("keterangan")) {
				p.setKeterangan(request.optString("keterangan", "").trim());
			}
			p.setOleh(tbmuser.getUserId());
			p.setOlehId(tbmuser.getUserId());
			session.saveOrUpdate(p);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("profilId", p.getId());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_bayar -- penjualan atomic: validasi kedaluwarsa MENAHAN, terkendali MENAHAN
	// =============================================================================================

	public static void bayar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (ConstantValues.apotikJual == null) {
			// Fail-closed: tanpa kode transaksi bertanda, baris ledger stok tidak bisa ditulis
			// benar -- lebih baik menolak daripada mencatat penjualan yang tidak mengurangi stok.
			tolak(hasil, "Kode transaksi 'apotik jual' belum terinisialisasi di server. Hubungi admin.");
			return;
		}
		JSONArray items = request == null ? null : request.optJSONArray("items");
		if (items == null || items.length() == 0) {
			tolak(hasil, "Minimal satu baris obat.");
			return;
		}
		Long lokasiId = optLong(request, "lokasi_id");
		Long resepId = optLong(request, "resep_id");
		JSONObject pembeli = request.optJSONObject("pembeli");
		String namaPembeli = pembeli == null ? "" : pembeli.optString("nama", "").trim();
		String alamatPembeli = pembeli == null ? "" : pembeli.optString("alamat", "").trim();
		String namaDokter = request.optString("nama_dokter", "").trim();
		String kodeIdem = request.optString("kode", "").trim();

		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			// Idempoten: retry perangkat dgn kode yang sama TIDAK membuat transaksi kedua.
			if (!kodeIdem.isEmpty()) {
				TransaksiMedis sudahAda = (TransaksiMedis) session.createCriteria(TransaksiMedis.class)
						.add(Restrictions.eq("kode", kodeIdem)).setMaxResults(1).uniqueResult();
				if (sudahAda != null) {
					hasil.put("status", "00");
					hasil.put("id", sudahAda.getId());
					hasil.put("kode", str(sudahAda.getKode()));
					hasil.put("idempoten", true);
					return;
				}
			}

			Resep resep = null;
			if (resepId != null) {
				resep = (Resep) session.get(Resep.class, resepId);
				if (resep == null) {
					tolak(hasil, "Resep tidak ditemukan.");
					return;
				}
			}
			Object lokasi = lokasiId == null ? null
					: session.get(ais.database.model.asset.Lokasi.class, lokasiId);

			// ---- Muat & validasi SELURUH baris dulu (fail-fast sebelum menulis apa pun) ----
			List<ItemMedis> itemList = new java.util.ArrayList<ItemMedis>();
			List<ApotikItemProfile> profilList = new java.util.ArrayList<ApotikItemProfile>();
			List<Double> qtyList = new java.util.ArrayList<Double>();
			List<Double> hargaList = new java.util.ArrayList<Double>();
			List<Double> diskonList = new java.util.ArrayList<Double>();
			List<List<Kadaluarsa>> batchList = new java.util.ArrayList<List<Kadaluarsa>>();
			List<List<Double>> batchQtyList = new java.util.ArrayList<List<Double>>();
			List<Long> semuaItemId = new java.util.ArrayList<Long>();

			for (int i = 0; i < items.length(); i++) {
				JSONObject baris = items.getJSONObject(i);
				Long itemId = optLong(baris, "item_id");
				double qty = baris.optDouble("qty", 0);
				if (itemId == null || qty <= 0) {
					tolak(hasil, "Baris " + (i + 1) + ": item_id dan qty (>0) wajib.");
					return;
				}
				ItemMedis item = (ItemMedis) session.get(ItemMedis.class, itemId);
				if (item == null) {
					tolak(hasil, "Baris " + (i + 1) + ": item tidak ditemukan.");
					return;
				}
				ApotikItemProfile profil = profilItem(session, item);
				String golongan = profil == null ? ApotikItemProfile.GOLONGAN_BEBAS : profil.getGolonganObat();

				// Obat terkendali: register WAJIB bisa dibuat -- tanpa identitas pembeli dan
				// (resep ATAU nama dokter), transaksi DITAHAN. Bukan peringatan.
				if (ApotikItemProfile.terkendali(golongan)) {
					if (namaPembeli.isEmpty()) {
						tolak(hasil, "\"" + str(item.getNama()) + "\" adalah obat terkendali (" + golongan
								+ "): nama pembeli/pasien WAJIB untuk register. Transaksi ditahan.");
						return;
					}
					if (resep == null && namaDokter.isEmpty()) {
						tolak(hasil, "\"" + str(item.getNama()) + "\" adalah obat terkendali (" + golongan
								+ "): wajib resep atau nama dokter penulis. Transaksi ditahan.");
						return;
					}
				}

				// Batch: bila item PUNYA catatan batch-kedaluwarsa, penjualan WAJIB memilih batch
				// (FEFO disarankan klien; server menegakkan sisa & tanggal). Item lama tanpa
				// catatan batch tetap bisa dijual (data historis tidak menghalangi operasional).
				long jumlahBatch = ((Number) session.createQuery(
						"select count(k) from Kadaluarsa k where k.item.id = :id")
						.setParameter("id", item.getId()).uniqueResult()).longValue();
				JSONArray batchJson = baris.optJSONArray("batch");
				List<Kadaluarsa> batchTerpilih = new java.util.ArrayList<Kadaluarsa>();
				List<Double> batchQty = new java.util.ArrayList<Double>();
				if (jumlahBatch > 0) {
					if (batchJson == null || batchJson.length() == 0) {
						tolak(hasil, "\"" + str(item.getNama())
								+ "\" ber-batch: pilih batch (FEFO) sebelum menjual.");
						return;
					}
					double totalBatch = 0;
					for (int b = 0; b < batchJson.length(); b++) {
						JSONObject bj = batchJson.getJSONObject(b);
						Long kadId = optLong(bj, "kadaluarsa_id");
						double bq = bj.optDouble("qty", 0);
						if (kadId == null || bq <= 0) {
							tolak(hasil, "Batch tidak valid pada \"" + str(item.getNama()) + "\".");
							return;
						}
						Kadaluarsa k = (Kadaluarsa) session.get(Kadaluarsa.class, kadId);
						if (k == null || k.getItem() == null || !k.getItem().getId().equals(item.getId())) {
							tolak(hasil, "Batch bukan milik item \"" + str(item.getNama()) + "\".");
							return;
						}
						// ATURAN KERAS (IR-02): lot karantina/recall/rusak/ditahan TIDAK BISA
						// terjual -- sejajar dengan aturan kedaluwarsa di bawah. Penahan,
						// bukan peringatan; UI tidak boleh melewatinya.
						if (!Kadaluarsa.lotLayak(k.getStatusLot())) {
							tolak(hasil, "DITOLAK: "
									+ Kadaluarsa.alasanLotDitahan(k.getStatusLot())
									+ " pada batch \"" + str(item.getNama())
									+ "\" -- tidak boleh dijual.");
							return;
						}
						// ATURAN KERAS: kedaluwarsa TIDAK BISA terjual. Penahan, bukan peringatan.
						if (kedaluwarsa(k)) {
							java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
							tolak(hasil, "DITOLAK: batch \"" + str(item.getNama()) + "\" kedaluwarsa "
									+ (k.getTanggalKadaluarsa() == null ? "-" : fmt.format(k.getTanggalKadaluarsa()))
									+ " tidak boleh dijual sama sekali.");
							return;
						}
						java.util.Map<Long, Double> pakai = konsumsiPerBatch(session,
								java.util.Collections.singletonList(k.getId()));
						double sisa = (k.getQty() == null ? 0 : k.getQty().doubleValue())
								- (pakai.containsKey(k.getId()) ? pakai.get(k.getId()).doubleValue() : 0);
						if (bq > sisa) {
							tolak(hasil, "Sisa batch \"" + str(item.getNama()) + "\" hanya "
									+ sisa + ", diminta " + bq + ".");
							return;
						}
						batchTerpilih.add(k);
						batchQty.add(Double.valueOf(bq));
						totalBatch += bq;
					}
					if (Math.abs(totalBatch - qty) > 0.0001) {
						tolak(hasil, "Jumlah batch (" + totalBatch + ") harus sama dgn qty ("
								+ qty + ") pada \"" + str(item.getNama()) + "\".");
						return;
					}
				}

				double harga = baris.optDouble("harga_satuan",
						item.getDefaultHargaJual() == null ? 0 : item.getDefaultHargaJual().doubleValue());
				double diskon = baris.optDouble("diskon", 0);

				itemList.add(item);
				profilList.add(profil);
				qtyList.add(Double.valueOf(qty));
				hargaList.add(Double.valueOf(harga));
				diskonList.add(Double.valueOf(diskon));
				batchList.add(batchTerpilih);
				batchQtyList.add(batchQty);
				semuaItemId.add(item.getId());
			}

			// Stok ledger cukup? (per item, akumulasi qty baris duplikat)
			java.util.Map<Long, Double> stok = stokPerItem(session, semuaItemId, lokasiId);
			java.util.Map<Long, Double> butuh = new java.util.HashMap<Long, Double>();
			for (int i = 0; i < itemList.size(); i++) {
				Long id = itemList.get(i).getId();
				Double b = butuh.get(id);
				butuh.put(id, Double.valueOf((b == null ? 0 : b.doubleValue()) + qtyList.get(i).doubleValue()));
			}
			for (java.util.Map.Entry<Long, Double> e : butuh.entrySet()) {
				double ada = stok.containsKey(e.getKey()) ? stok.get(e.getKey()).doubleValue() : 0;
				if (e.getValue().doubleValue() > ada) {
					tolak(hasil, "Stok tidak cukup utk item id " + e.getKey() + " (stok " + ada
							+ ", diminta " + e.getValue() + ").");
					return;
				}
			}

			// ---- Tulis SEMUA dalam SATU transaksi -- gagal satu = batal semua ----
			tx = session.beginTransaction();
			TransaksiMedis trx = new TransaksiMedis();
			trx.setKode(kodeIdem.isEmpty() ? "APT" + System.currentTimeMillis() : kodeIdem);
			// jenis_transaksi NOT NULL (kolom wajib) -- penjualan item apotek = TRX_ITEM, sama
			// dgn jalur rumah sakit (TransaksiAction). sumber=APOTIK menandai asal transaksi.
			trx.setJenisTransaksi(TransaksiMedis.TRX_ITEM);
			trx.setSumber(TransaksiMedis.SUMBER_APOTIK);
			trx.setBebas(Boolean.TRUE);
			trx.setLunas(Boolean.TRUE);
			trx.setTanggalTransaksi(new Date());
			if (resep != null) {
				trx.setResep(resep);
			}
			if (!namaPembeli.isEmpty()) {
				trx.setNama(namaPembeli);
			}
			if (!alamatPembeli.isEmpty()) {
				trx.setAlamat(alamatPembeli);
			}
			if (lokasi != null) {
				trx.setLokasi((ais.database.model.asset.Lokasi) lokasi);
			}
			// Keterbatasan FASE A (disengaja): entity Pembayaran SIRS belum dibuat di sini --
			// dicatat tunai lunas; integrasi kasir Pembayaran menyusul fase kasir medis.
			trx.setKeterangan(("Kasir Apotik Flutter; tunai lunas. "
					+ request.optString("keterangan", "")).trim());
			trx.setOleh(tbmuser.getUserId());
			trx.setOlehId(tbmuser.getUserId());

			// IR-07: metode pembayaran (opsional demi kompatibilitas klien lama).
			// Bila dikirim, WAJIB metode yang benar-benar ada & aktif -- klien
			// tidak boleh menyodorkan metode di luar konfigurasi server.
			Long caraBayarId = optLong(request, "cara_bayar_id");
			ais.database.model.koperasi.CaraPembayaranKoperasi caraBayar = null;
			if (caraBayarId != null) {
				caraBayar = (ais.database.model.koperasi.CaraPembayaranKoperasi) session
						.get(ais.database.model.koperasi.CaraPembayaranKoperasi.class, caraBayarId);
				if (caraBayar == null || !Boolean.TRUE.equals(caraBayar.getAktif())) {
					tolak(hasil, "Metode pembayaran tidak dikenal atau sudah nonaktif.");
					return;
				}
			}

			session.save(trx);

			double total = 0;
			for (int i = 0; i < itemList.size(); i++) {
				ItemMedis item = itemList.get(i);
				double qty = qtyList.get(i).doubleValue();
				double harga = hargaList.get(i).doubleValue();
				double diskon = diskonList.get(i).doubleValue();
				double subtotal = qty * harga - diskon;
				total += subtotal;

				TransaksiMedisDetail detail = new TransaksiMedisDetail();
				detail.setTransaksi(trx);
				detail.setItem(item);
				detail.setQty(Double.valueOf(qty));
				detail.setAmount(Double.valueOf(harga));
				detail.setDiskon(Double.valueOf(diskon));
				detail.setHasilPenghitunganTotal(Double.valueOf(subtotal));
				detail.setTanggal(new Date());
				detail.setOleh(tbmuser.getUserId());
				detail.setOlehId(tbmuser.getUserId());
				session.save(detail);

				// Baris ledger stok -- pola PERSIS CommonPendaftaranUtil (kodeTransaksi apotikJual).
				ais.database.model.sirs.DetailTransaksiPasien ledger =
						new ais.database.model.sirs.DetailTransaksiPasien();
				ledger.setKodeTransaksi(ConstantValues.apotikJual);
				ledger.setItem(item);
				ledger.setQty(Double.valueOf(qty));
				ledger.setAmount(Double.valueOf(harga));
				ledger.setDiskon(Double.valueOf(diskon));
				ledger.setHasilPenghitunganTotal(Double.valueOf(subtotal));
				ledger.setTransaksiDetail(detail);
				ledger.setTanggal(new Date());
				ledger.setLunas(Boolean.TRUE);
				if (lokasi != null) {
					ledger.setLokasi((ais.database.model.asset.Lokasi) lokasi);
				}
				ledger.setOleh(tbmuser.getUserId());
				ledger.setOlehId(tbmuser.getUserId());
				session.save(ledger);

				List<Kadaluarsa> batches = batchList.get(i);
				List<Double> bqty = batchQtyList.get(i);
				for (int b = 0; b < batches.size(); b++) {
					ApotikBatchKonsumsi konsumsi = new ApotikBatchKonsumsi();
					konsumsi.setKadaluarsa(batches.get(b));
					konsumsi.setTransaksiDetail(detail);
					konsumsi.setQty(bqty.get(b));
					konsumsi.setWaktu(new Date());
					konsumsi.setOleh(tbmuser.getUserId());
					konsumsi.setOlehId(tbmuser.getUserId());
					session.save(konsumsi);
				}

				ApotikItemProfile profil = profilList.get(i);
				String golongan = profil == null ? ApotikItemProfile.GOLONGAN_BEBAS : profil.getGolonganObat();
				if (ApotikItemProfile.terkendali(golongan)) {
					ApotikNarkotikaLog log = new ApotikNarkotikaLog();
					log.setItem(item);
					log.setTransaksiDetail(detail);
					log.setResep(resep);
					log.setQty(Double.valueOf(qty));
					log.setGolonganObat(golongan);
					log.setNamaPembeli(namaPembeli);
					log.setAlamatPembeli(alamatPembeli);
					log.setNamaDokter(namaDokter);
					log.setKeterangan(items.getJSONObject(i).optString("keterangan_terkendali", "").trim());
					log.setWaktu(new Date());
					log.setOleh(tbmuser.getUserId());
					log.setOlehId(tbmuser.getUserId());
					session.save(log);
				}
			}
			// IR-07: catat metode pembayaran DI DALAM transaksi yang sama supaya
			// tidak pernah ada transaksi tanpa jejak metode saat metode dikirim.
			if (caraBayar != null) {
				ApotikPembayaranTransaksi bayarRow = new ApotikPembayaranTransaksi();
				bayarRow.setTransaksi(trx);
				bayarRow.setCaraBayar(caraBayar);
				bayarRow.setNamaCaraBayar(str(caraBayar.getNama()));
				bayarRow.setNominal(Double.valueOf(total));
				bayarRow.setReferensi(request == null ? null
						: request.optString("referensi_bayar", "").trim());
				bayarRow.setWaktu(new Date());
				bayarRow.setOleh(tbmuser.getUserId());
				bayarRow.setOlehId(tbmuser.getUserId());
				session.save(bayarRow);
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", trx.getId());
			hasil.put("kode", str(trx.getKode()));
			hasil.put("total", total);
			hasil.put("caraBayar", caraBayar == null ? "" : str(caraBayar.getNama()));
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			ais.common.ErrorAuditUtil.record(e, "ApotikApiHelper.bayar");
			// Surface penyebab NYATA ke klien (bukan "Terjadi kesalahan sistem" generik) --
			// pesan spesifik jauh lebih berguna utk kasir & diagnosa. Sertakan sebab-akar bila ada.
			Throwable akar = e;
			while (akar.getCause() != null && akar.getCause() != akar) {
				akar = akar.getCause();
			}
			tolak(hasil, "Gagal menyimpan penjualan: " + e.getClass().getSimpleName()
					+ (akar != e ? " -> " + akar.getClass().getSimpleName() : "")
					+ ": " + (akar.getMessage() == null ? "(tanpa pesan)" : akar.getMessage()));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
