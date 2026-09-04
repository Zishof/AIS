package ais.action.servlet.api;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.LockMode;
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
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.sirs.ApotikPbfDokumen;
import ais.database.model.sirs.ApotikPbfPembayaran;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Kadaluarsa;
import ais.database.model.sirs.KodeTransaksiMedis;

/**
 * <h3>Persediaan apotik (FASE B) -- terima barang PBF, opname, retur, monitor batch.</h3>
 *
 * <p>SEMUA mutasi stok lewat ledger existing {@code sirs.detail_transaksi_pasien} dengan
 * {@link KodeTransaksiMedis} bertanda yang SUDAH ADA di {@code ConstantValues}: {@code beliMasuk}
 * (penerimaan PBF, +), {@code beliRetur} (retur ke PBF, -), {@code apotikRetur} (retur dari
 * pembeli, +), {@code adjustmentPenambahan}/{@code adjustmentPengurangan} (opname) -- rumus stok
 * SUM((qty+qty_bonus)*jenis) otomatis benar tanpa logika tanda baru. Penerimaan ber-tanggal
 * kedaluwarsa juga menulis batch {@link Kadaluarsa} (pola PenerimaanOrderAction).</p>
 */
public final class ApotikPersediaanHelper {

	private ApotikPersediaanHelper() {
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	/**
	 * Kolom audit pada tabel Apotik baru dibatasi 60 karakter, sementara
	 * {@code tbmuser.userid} secara historis dapat mencapai 255 karakter.
	 * Batasi nilai di boundary persistence supaya akun federasi/tenant yang
	 * memakai userid panjang tidak menggagalkan transaksi bisnis seluruhnya.
	 */
	private static String auditActor(Tbmuser user) {
		String nilai = user == null ? "" : str(user.getUserId()).trim();
		return nilai.length() <= 60 ? nilai : nilai.substring(0, 60);
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

	private static DetailTransaksiPasien barisLedger(KodeTransaksiMedis kode, ItemMedis item, double qty,
			double amount, Object lokasi, String keterangan, Tbmuser tbmuser) {
		DetailTransaksiPasien ledger = new DetailTransaksiPasien();
		ledger.setKodeTransaksi(kode);
		ledger.setItem(item);
		ledger.setQty(Double.valueOf(qty));
		ledger.setAmount(Double.valueOf(amount));
		ledger.setHasilPenghitunganTotal(Double.valueOf(qty * amount));
		ledger.setTanggal(new Date());
		if (lokasi != null) {
			ledger.setLokasi((ais.database.model.asset.Lokasi) lokasi);
		}
		if (keterangan != null && !keterangan.trim().isEmpty()) {
			ledger.setKeterangan(keterangan.trim());
		}
		ledger.setOleh(tbmuser.getUserId());
		ledger.setOlehId(tbmuser.getUserId());
		return ledger;
	}

	// =============================================================================================
	// apotik_terima_barang -- penerimaan PBF langsung (+ batch Kadaluarsa bila ber-ED)
	// =============================================================================================

	public static void terimaBarang(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "apotik_pengadaan", "create")) {
			tolak(hasil, "Akun Anda tidak berhak mencatat penerimaan barang (Pengadaan/PBF).");
			return;
		}
		if (ConstantValues.beliMasuk == null) {
			tolak(hasil, "Kode transaksi 'beli masuk' belum terinisialisasi di server. Hubungi admin.");
			return;
		}
		JSONArray items = request == null ? null : request.optJSONArray("items");
		if (items == null || items.length() == 0) {
			tolak(hasil, "Minimal satu baris penerimaan.");
			return;
		}
		Long lokasiId = optLong(request, "lokasi_id");
		String noFaktur = request.optString("no_faktur", "").trim();
		String penyedia = request.optString("penyedia", "").trim();
		String kodeDokumen = request.optString("kode_dokumen", "").trim();
		if (kodeDokumen.isEmpty()) {
			kodeDokumen = "PBF-" + new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
					+ "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		}
		String catatan = (kodeDokumen + " PBF " + penyedia
				+ (noFaktur.isEmpty() ? "" : " faktur " + noFaktur)).trim();

		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			Object lokasi = lokasiId == null ? null
					: session.get(ais.database.model.asset.Lokasi.class, lokasiId);
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
			tx = session.beginTransaction();
			double totalDokumen = 0;
			for (int i = 0; i < items.length(); i++) {
				JSONObject baris = items.getJSONObject(i);
				totalDokumen += baris.optDouble("qty", 0) * baris.optDouble("harga_beli", 0);
			}
			ApotikPbfDokumen dokumen = new ApotikPbfDokumen();
			dokumen.setKode(kodeDokumen);
			dokumen.setNoFaktur(noFaktur);
			dokumen.setPenyedia(penyedia);
			dokumen.setTanggal(new Date());
			dokumen.setTotal(Double.valueOf(totalDokumen));
			dokumen.setJumlahBaris(Integer.valueOf(items.length()));
			dokumen.setKeterangan(request.optString("keterangan", "").trim());
			String pelaku = auditActor(tbmuser);
			dokumen.setOleh(pelaku);
			dokumen.setOlehId(pelaku);
			session.save(dokumen);
			int barisBatch = 0;
			// IR-09 (sebagian): apakah faktur ini memuat barang rantai dingin.
			// Ditentukan SERVER dari profil item, bukan dari klaim klien --
			// klien yang lupa/salah menandai tidak boleh membuat bukti suhu
			// jadi tidak wajib.
			boolean adaColdChain = false;
			for (int i = 0; i < items.length(); i++) {
				JSONObject baris = items.getJSONObject(i);
				Long itemId = optLong(baris, "item_id");
				double qty = baris.optDouble("qty", 0);
				double hargaBeli = baris.optDouble("harga_beli", 0);
				if (itemId == null || qty <= 0) {
					throw new IllegalArgumentException("Baris " + (i + 1) + ": item_id dan qty (>0) wajib.");
				}
				ItemMedis item = (ItemMedis) session.get(ItemMedis.class, itemId);
				if (item == null) {
					throw new IllegalArgumentException("Baris " + (i + 1) + ": item tidak ditemukan.");
				}
				session.save(barisLedger(ConstantValues.beliMasuk, item, qty, hargaBeli, lokasi,
						catatan + " " + str(baris.optString("keterangan", "")).trim(), tbmuser));
				if (!adaColdChain) {
					java.util.List<?> profil = session
							.createCriteria(ais.database.model.sirs.ApotikItemProfile.class)
							.add(org.hibernate.criterion.Restrictions.eq("item", item))
							.setMaxResults(1).list();
					if (!profil.isEmpty() && Boolean.TRUE.equals(
							((ais.database.model.sirs.ApotikItemProfile) profil.get(0)).getColdChain())) {
						adaColdChain = true;
					}
				}
				String tglEd = baris.optString("tanggal_kadaluarsa", "").trim();
				if (!tglEd.isEmpty()) {
					Kadaluarsa k = new Kadaluarsa();
					k.setItem(item);
					k.setQty(Double.valueOf(qty));
					k.setTanggalKadaluarsa(fmt.parse(tglEd));
					if (lokasi != null) {
						k.setLokasi((ais.database.model.asset.Lokasi) lokasi);
					}
					k.setKeterangan(catatan);
					k.setOleh(tbmuser.getUserId());
					k.setOlehId(tbmuser.getUserId());
					session.save(k);
					barisBatch++;
				}
			}
			// Bukti suhu penerimaan (IR-09 sebagian). Server MENYIMPAN, tidak
			// menolak: keputusan menerima/menolak barang rantai dingin adalah
			// wewenang apoteker penanggung jawab dan bergantung SOP tiap
			// apotek. Yang dijamin di sini hanyalah jejaknya tercatat.
			Double suhu = null;
			if (request != null && !request.isNull("suhu_terima")) {
				suhu = Double.valueOf(request.optDouble("suhu_terima", 0));
			}
			String suhuKeterangan = request == null ? ""
					: request.optString("suhu_keterangan", "").trim();
			if (suhu != null || adaColdChain || suhuKeterangan.length() > 0) {
				ais.database.model.sirs.ApotikPenerimaanSuhu bukti =
						new ais.database.model.sirs.ApotikPenerimaanSuhu();
				bukti.setNoFaktur(noFaktur);
				bukti.setPenyedia(penyedia);
				bukti.setSuhuCelsius(suhu);
				bukti.setAdaColdChain(Boolean.valueOf(adaColdChain));
				bukti.setKeterangan(suhuKeterangan);
				bukti.setWaktu(new java.util.Date());
				bukti.setOleh(pelaku);
				bukti.setOlehId(pelaku);
				session.save(bukti);
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("jumlahBaris", items.length());
			hasil.put("jumlahBatch", barisBatch);
			hasil.put("dokumenId", dokumen.getId());
			hasil.put("kodeDokumen", dokumen.getKode());
			hasil.put("total", totalDokumen);
			hasil.put("adaColdChain", adaColdChain);
			hasil.put("suhuTercatat", suhu != null);
			if (suhu != null) {
				hasil.put("suhuDiLuarRentang",
						ais.database.model.sirs.ApotikPenerimaanSuhu.diLuarRentang(suhu));
			}
		} catch (IllegalArgumentException e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			tolak(hasil, e.getMessage());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Daftar dokumen PBF beserta saldo utang dan status postingnya. */
	public static void pbfList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 100 : request.optInt("page_size", 100);
		if (size < 1) size = 100;
		if (size > 10000) size = 10000;
		String mulai = request == null ? "" : request.optString("mulai", "").trim();
		String sampai = request == null ? "" : request.optString("sampai", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String filter = " WHERE 1=1";
			if (!mulai.isEmpty()) filter += " AND date(d.tanggal)>=date(:mulai)";
			if (!sampai.isEmpty()) filter += " AND date(d.tanggal)<=date(:sampai)";
			org.hibernate.SQLQuery q = session.createSQLQuery("SELECT d.id,d.kode,d.no_faktur,d.penyedia,d.tanggal,d.total,d.jumlah_baris,"
					+ "COALESCE(SUM(b.nominal),0),d.posting_history,COUNT(b.id) FROM sirs.apotik_pbf_dokumen d"
					+ " LEFT JOIN sirs.apotik_pbf_pembayaran b ON b.dokumen=d.id" + filter
					+ " GROUP BY d.id,d.kode,d.no_faktur,d.penyedia,d.tanggal,d.total,d.jumlah_baris,d.posting_history"
					+ " ORDER BY d.tanggal DESC,d.id DESC");
			if (!mulai.isEmpty()) q.setString("mulai", mulai);
			if (!sampai.isEmpty()) q.setString("sampai", sampai);
			q.setFirstResult((page - 1) * size).setMaxResults(size);
			@SuppressWarnings("unchecked") List<Object[]> rows = q.list();
			JSONArray data = new JSONArray();
			for (Object[] r : rows) {
				double total = ((Number) r[5]).doubleValue();
				double dibayar = ((Number) r[7]).doubleValue();
				JSONObject j = new JSONObject();
				j.put("id", r[0]); j.put("kode", str(r[1])); j.put("noFaktur", str(r[2]));
				j.put("penyedia", str(r[3])); j.put("tanggal", r[4] == null ? "" : Common.dateFormat3.get().format((Date) r[4]));
				j.put("total", total); j.put("dibayar", dibayar); j.put("sisa", Math.max(0, total - dibayar));
				j.put("jumlahBaris", r[6]); j.put("jumlahPembayaran", r[9]);
				j.put("statusPembayaran", dibayar <= 0.005 ? "BELUM" : dibayar + 0.005 >= total ? "LUNAS" : "SEBAGIAN");
				j.put("sudahDiposting", r[8] != null);
				data.put(j);
			}
			org.hibernate.SQLQuery qc = session.createSQLQuery("SELECT COUNT(*) FROM sirs.apotik_pbf_dokumen d" + filter);
			if (!mulai.isEmpty()) qc.setString("mulai", mulai);
			if (!sampai.isEmpty()) qc.setString("sampai", sampai);
			Number total = (Number) qc.uniqueResult();
			hasil.put("status", "00"); hasil.put("data", data); hasil.put("page", page);
			hasil.put("pageSize", size); hasil.put("total", total == null ? 0 : total.longValue());
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	/** Mencatat pembayaran utang PBF; jurnalnya dibentuk lewat layar posting khusus Apotik. */
	public static void pbfBayar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "apotik_pengadaan", "update")
				&& !bolehAksi(tbmuser, "apotik_pengadaan", "create")) {
			tolak(hasil, "Akun Anda tidak berhak mencatat pembayaran PBF."); return;
		}
		Long dokumenId = optLong(request, "dokumen_id");
		Long caraId = optLong(request, "cara_bayar_id");
		double nominal = request == null ? 0 : request.optDouble("nominal", 0);
		if (dokumenId == null || caraId == null || nominal <= 0) {
			tolak(hasil, "dokumen_id, cara_bayar_id, dan nominal (>0) wajib diisi."); return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
		try {
			tx = session.beginTransaction();
			ApotikPbfDokumen d = (ApotikPbfDokumen) session.get(ApotikPbfDokumen.class, dokumenId, LockMode.UPGRADE);
			CaraPembayaranKoperasi cara = (CaraPembayaranKoperasi) session.get(CaraPembayaranKoperasi.class, caraId);
			if (d == null) { tx.rollback(); tolak(hasil, "Dokumen PBF tidak ditemukan."); return; }
			if (cara == null || !Boolean.TRUE.equals(cara.getAktif())) { tx.rollback(); tolak(hasil, "Cara pembayaran tidak aktif/tidak ditemukan."); return; }
			Number sudah = (Number) session.createSQLQuery("SELECT COALESCE(SUM(nominal),0) FROM sirs.apotik_pbf_pembayaran WHERE dokumen=" + dokumenId).uniqueResult();
			double dibayar = sudah == null ? 0 : sudah.doubleValue();
			double sisa = d.getTotal().doubleValue() - dibayar;
			if (nominal > sisa + 0.005) { tx.rollback(); tolak(hasil, "Nominal melebihi sisa utang PBF."); return; }
			ApotikPbfPembayaran b = new ApotikPbfPembayaran(); b.setDokumen(d); b.setCaraBayar(cara);
			b.setNominal(Double.valueOf(nominal)); b.setTanggal(new Date());
			b.setKeterangan(request.optString("keterangan", "").trim());
			String pelaku = auditActor(tbmuser); b.setOleh(pelaku); b.setOlehId(pelaku);
			session.save(b); tx.commit();
			hasil.put("status", "00"); hasil.put("pembayaranId", b.getId()); hasil.put("dokumenId", d.getId());
			hasil.put("dibayar", dibayar + nominal); hasil.put("sisa", Math.max(0, sisa - nominal));
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	// =============================================================================================
	// apotik_opname_simpan -- koreksi stok fisik (selisih -> adjustment +/-)
	// =============================================================================================

	public static void opnameSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "apotik_stok_opname", "create")
				&& !bolehAksi(tbmuser, "apotik_stok_opname", "update")) {
			tolak(hasil, "Akun Anda tidak berhak menyimpan Stok Opname Apotik.");
			return;
		}
		if (ConstantValues.adjustmentPenambahan == null || ConstantValues.adjustmentPengurangan == null) {
			tolak(hasil, "Kode transaksi adjustment belum terinisialisasi di server. Hubungi admin.");
			return;
		}
		JSONArray items = request == null ? null : request.optJSONArray("items");
		if (items == null || items.length() == 0) {
			tolak(hasil, "Minimal satu baris opname.");
			return;
		}
		Long lokasiId = optLong(request, "lokasi_id");
		String catatan = "Opname apotik " + request.optString("keterangan", "").trim();

		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			Object lokasi = lokasiId == null ? null
					: session.get(ais.database.model.asset.Lokasi.class, lokasiId);
			List<Long> ids = new java.util.ArrayList<Long>();
			for (int i = 0; i < items.length(); i++) {
				Long itemId = optLong(items.getJSONObject(i), "item_id");
				if (itemId == null) {
					tolak(hasil, "Baris " + (i + 1) + ": item_id wajib.");
					return;
				}
				ids.add(itemId);
			}
			java.util.Map<Long, Double> stok = ApotikApiHelper.stokPerItem(session, ids, lokasiId);
			tx = session.beginTransaction();
			JSONArray rincian = new JSONArray();
			for (int i = 0; i < items.length(); i++) {
				JSONObject baris = items.getJSONObject(i);
				Long itemId = optLong(baris, "item_id");
				double fisik = baris.optDouble("qty_fisik", 0);
				ItemMedis item = (ItemMedis) session.get(ItemMedis.class, itemId);
				if (item == null) {
					throw new IllegalArgumentException("Baris " + (i + 1) + ": item tidak ditemukan.");
				}
				double sistem = stok.containsKey(itemId) ? stok.get(itemId).doubleValue() : 0;
				double selisih = fisik - sistem;
				JSONObject r = new JSONObject();
				r.put("itemId", itemId);
				r.put("nama", str(item.getNama()));
				r.put("stokSistem", sistem);
				r.put("qtyFisik", fisik);
				r.put("selisih", selisih);
				if (Math.abs(selisih) > 0.0001) {
					KodeTransaksiMedis kode = selisih > 0 ? ConstantValues.adjustmentPenambahan
							: ConstantValues.adjustmentPengurangan;
					session.save(barisLedger(kode, item, Math.abs(selisih), 0, lokasi,
							catatan + " (sistem " + sistem + " -> fisik " + fisik + ") "
									+ baris.optString("keterangan", "").trim(),
							tbmuser));
				}
				rincian.put(r);
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("data", rincian);
		} catch (IllegalArgumentException e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			tolak(hasil, e.getMessage());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_retur_simpan -- retur dari pembeli (masuk) ATAU retur ke PBF (keluar)
	// =============================================================================================

	public static void returSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "apotik_retur", "create")) {
			tolak(hasil, "Akun Anda tidak berhak mencatat Retur Obat.");
			return;
		}
		String jenis = request == null ? "" : request.optString("jenis", "").trim();
		boolean kePbf = "pbf".equals(jenis);
		if (!kePbf && !"penjualan".equals(jenis)) {
			tolak(hasil, "jenis wajib 'penjualan' (obat kembali dari pembeli) atau 'pbf' (kembali ke pemasok).");
			return;
		}
		KodeTransaksiMedis kode = kePbf ? ConstantValues.beliRetur : ConstantValues.apotikRetur;
		if (kode == null) {
			tolak(hasil, "Kode transaksi retur belum terinisialisasi di server. Hubungi admin.");
			return;
		}
		JSONArray items = request.optJSONArray("items");
		if (items == null || items.length() == 0) {
			tolak(hasil, "Minimal satu baris retur.");
			return;
		}
		Long lokasiId = optLong(request, "lokasi_id");
		String catatan = ("Retur " + (kePbf ? "ke PBF" : "penjualan apotik") + " "
				+ request.optString("keterangan", "").trim()).trim();

		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			Object lokasi = lokasiId == null ? null
					: session.get(ais.database.model.asset.Lokasi.class, lokasiId);
			if (kePbf) {
				// Retur keluar mengurangi stok -- pastikan cukup (pola apotik_bayar).
				List<Long> ids = new java.util.ArrayList<Long>();
				for (int i = 0; i < items.length(); i++) {
					Long id = optLong(items.getJSONObject(i), "item_id");
					if (id != null) ids.add(id);
				}
				java.util.Map<Long, Double> stok = ApotikApiHelper.stokPerItem(session, ids, lokasiId);
				for (int i = 0; i < items.length(); i++) {
					JSONObject baris = items.getJSONObject(i);
					Long itemId = optLong(baris, "item_id");
					double qty = baris.optDouble("qty", 0);
					double ada = itemId != null && stok.containsKey(itemId) ? stok.get(itemId).doubleValue() : 0;
					if (qty > ada) {
						tolak(hasil, "Stok tidak cukup utk retur item id " + itemId + " (stok " + ada
								+ ", retur " + qty + ").");
						return;
					}
				}
			}
			tx = session.beginTransaction();
			for (int i = 0; i < items.length(); i++) {
				JSONObject baris = items.getJSONObject(i);
				Long itemId = optLong(baris, "item_id");
				double qty = baris.optDouble("qty", 0);
				if (itemId == null || qty <= 0) {
					throw new IllegalArgumentException("Baris " + (i + 1) + ": item_id dan qty (>0) wajib.");
				}
				ItemMedis item = (ItemMedis) session.get(ItemMedis.class, itemId);
				if (item == null) {
					throw new IllegalArgumentException("Baris " + (i + 1) + ": item tidak ditemukan.");
				}
				session.save(barisLedger(kode, item, qty, baris.optDouble("harga", 0), lokasi,
						catatan + " " + baris.optString("keterangan", "").trim(), tbmuser));
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("jumlahBaris", items.length());
		} catch (IllegalArgumentException e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			tolak(hasil, e.getMessage());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_batch_monitor -- batch terdekat kedaluwarsa lintas item (padanan MonitorKadaluarsa)
	// =============================================================================================

	/**
	 * IR-02 sisi TULIS: mengubah status lot (karantina / recall / rusak /
	 * ditahan / kembali layak).
	 *
	 * <p>Aturan yang ditegakkan server:</p>
	 * <ul>
	 *   <li>status wajib salah satu konstanta {@code Kadaluarsa.LOT_*};</li>
	 *   <li>ALASAN WAJIB saat lot ditahan -- menahan stok berdampak pada
	 *       ketersediaan obat, jadi harus dapat ditelusuri siapa dan mengapa;</li>
	 *   <li>alasan disimpan pada {@code keterangan} lot (append, bukan timpa)
	 *       sehingga riwayatnya terbaca; perubahan sendiri terekam Envers.</li>
	 * </ul>
	 */
	public static void batchStatusUbah(ais.database.model.Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (tbmuser == null || tbmuser.getUserId() == null) {
			tolak(hasil, "Sesi tidak dikenali.");
			return;
		}
		Long kadaluarsaId = optLong(request, "kadaluarsa_id");
		String status = request == null ? "" : request.optString("status", "").trim().toUpperCase();
		String alasan = request == null ? "" : request.optString("alasan", "").trim();
		if (kadaluarsaId == null || status.isEmpty()) {
			tolak(hasil, "kadaluarsa_id dan status wajib diisi.");
			return;
		}
		boolean statusSah = ais.database.model.sirs.Kadaluarsa.LOT_ELIGIBLE.equals(status)
				|| ais.database.model.sirs.Kadaluarsa.LOT_HELD.equals(status)
				|| ais.database.model.sirs.Kadaluarsa.LOT_QUARANTINE.equals(status)
				|| ais.database.model.sirs.Kadaluarsa.LOT_RECALL.equals(status)
				|| ais.database.model.sirs.Kadaluarsa.LOT_DAMAGED.equals(status);
		if (!statusSah) {
			tolak(hasil, "Status lot tidak dikenal: " + status);
			return;
		}
		boolean menahan = !ais.database.model.sirs.Kadaluarsa.LOT_ELIGIBLE.equals(status);
		if (menahan && alasan.length() < 5) {
			tolak(hasil, "Alasan wajib diisi (minimal 5 karakter) saat menahan lot -- "
					+ "penahanan stok harus dapat ditelusuri.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.sirs.Kadaluarsa k = (ais.database.model.sirs.Kadaluarsa)
					session.get(ais.database.model.sirs.Kadaluarsa.class, kadaluarsaId);
			if (k == null) {
				tolak(hasil, "Batch tidak ditemukan.");
				return;
			}
			String lama = k.getStatusLot();
			if (lama.equals(status)) {
				hasil.put("status", "00");
				hasil.put("idempotent", true);
				hasil.put("statusLot", status);
				hasil.put("description", "Status lot sudah " + status + ".");
				return;
			}
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
			StringBuilder jejak = new StringBuilder();
			if (k.getKeterangan() != null && !k.getKeterangan().trim().isEmpty()) {
				jejak.append(k.getKeterangan().trim()).append(System.getProperty("line.separator"));
			}
			jejak.append("[").append(fmt.format(new java.util.Date())).append("] ")
					.append(lama).append(" -> ").append(status)
					.append(" oleh ").append(tbmuser.getUserId());
			if (!alasan.isEmpty()) jejak.append(": ").append(alasan);
			k.setStatusLot(status);
			k.setKeterangan(jejak.toString());
			k.setOleh(tbmuser.getUserId());
			session.beginTransaction();
			session.saveOrUpdate(k);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("statusLot", status);
			hasil.put("lotLayak", ais.database.model.sirs.Kadaluarsa.lotLayak(status));
			hasil.put("description", "Status lot diubah " + lama + " -> " + status + ".");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "batchStatusUbah rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void batchMonitor(JSONObject request, JSONObject hasil) throws Exception {
		int hariKeDepan = request == null ? 90 : request.optInt("hari_ke_depan", 90);
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 30 : request.optInt("page_size", 30);
		if (size < 1) size = 30;
		if (size > 100) size = 100;
		Long lokasiId = optLong(request, "lokasi_id");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.util.Calendar batas = java.util.Calendar.getInstance();
			batas.add(java.util.Calendar.DAY_OF_MONTH, hariKeDepan);
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
			Date awalHari = awalHariIni();
			JSONArray arr = new JSONArray();
			// Jangan melakukan paging SEBELUM batch habis disaring. Pola lama mengambil
			// tepat `size` baris dari DB lalu membuang batch bersisa nol, sehingga
			// page_size=100 dapat mengembalikan 99 (atau lebih sedikit) walaupun ribuan
			// batch layak masih tersedia pada halaman berikutnya. Ambil kandidat per
			// chunk, hitung sisa, lalu terapkan offset/limit pada kumpulan LOGIS yang
			// benar-benar layak ditampilkan. Ini menjaga halaman selalu penuh sekaligus
			// mencegah duplikasi/lompatan antarhalaman.
			final int offsetLayak = (page - 1) * size;
			final int ukuranChunk = Math.max(100, size * 2);
			int offsetMentah = 0;
			int jumlahLayakTerlewati = 0;
			boolean kandidatHabis = false;
			while (arr.length() < size && !kandidatHabis) {
				org.hibernate.Criteria c = session.createCriteria(Kadaluarsa.class)
						.add(Restrictions.le("tanggalKadaluarsa", batas.getTime()));
				if (lokasiId != null) {
					c.createAlias("lokasi", "lokasi").add(Restrictions.eq("lokasi.id", lokasiId));
				}
				c.addOrder(Order.asc("tanggalKadaluarsa")).addOrder(Order.asc("id"));
				c.setFirstResult(offsetMentah);
				c.setMaxResults(ukuranChunk);
				@SuppressWarnings("unchecked")
				List<Kadaluarsa> batches = c.list();
				if (batches.size() < ukuranChunk) kandidatHabis = true;
				offsetMentah += batches.size();
				if (batches.isEmpty()) break;

				List<Long> ids = new java.util.ArrayList<Long>();
				for (Kadaluarsa k : batches) ids.add(k.getId());
				java.util.Map<Long, Double> konsumsi = ApotikApiHelper.konsumsiPerBatch(session, ids);
				for (Kadaluarsa k : batches) {
					double awal = k.getQty() == null ? 0 : k.getQty().doubleValue();
					Double pakai = konsumsi.get(k.getId());
					double sisa = awal - (pakai == null ? 0 : pakai.doubleValue());
					if (sisa <= 0.0001) continue;
					if (jumlahLayakTerlewati < offsetLayak) {
						jumlahLayakTerlewati++;
						continue;
					}
					JSONObject j = new JSONObject();
					j.put("kadaluarsaId", k.getId());
					j.put("itemId", k.getItem() == null ? JSONObject.NULL : k.getItem().getId());
					j.put("kode", k.getItem() == null ? "" : str(k.getItem().getKode()));
					j.put("nama", k.getItem() == null ? "" : str(k.getItem().getNama()));
					j.put("tanggalKadaluarsa",
							k.getTanggalKadaluarsa() == null ? "" : fmt.format(k.getTanggalKadaluarsa()));
					j.put("sisa", sisa);
					j.put("kedaluwarsa",
							k.getTanggalKadaluarsa() != null && k.getTanggalKadaluarsa().before(awalHari));
					arr.put(j);
					if (arr.length() >= size) break;
				}
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("page", page);
			hasil.put("pageSize", size);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static Date awalHariIni() {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return c.getTime();
	}
}
