package ais.action.servlet.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.sirs.ApotikBatchKonsumsi;
import ais.database.model.sirs.ApotikItemProfile;
import ais.database.model.sirs.ApotikNarkotikaLog;
import ais.database.model.sirs.ApotikPembayaranTransaksi;
import ais.database.model.sirs.BahanBakuItem;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Kadaluarsa;
import ais.database.model.sirs.KodeTransaksiMedis;
import ais.database.model.sirs.Produksi;
import ais.database.model.sirs.ProduksiDetail;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.RacikanDetail;
import ais.database.model.sirs.Resep;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;

/** Alur end-to-end Racikan dan Produksi Farmasi untuk POS Apotik. */
public final class ApotikRacikanProduksiHelper {

	private ApotikRacikanProduksiHelper() { }

	private static final class Validasi extends Exception {
		private static final long serialVersionUID = 1L;
		Validasi(String pesan) { super(pesan); }
	}

	private static final class AlokasiBatch {
		final Kadaluarsa batch;
		final double qty;
		AlokasiBatch(Kadaluarsa batch, double qty) {
			this.batch = batch;
			this.qty = qty;
		}
	}

	private static final class KomponenRacikan {
		final ItemMedis item;
		final ApotikItemProfile profil;
		final double qty;
		final double harga;
		final List<AlokasiBatch> batch;
		KomponenRacikan(ItemMedis item, ApotikItemProfile profil, double qty,
				double harga, List<AlokasiBatch> batch) {
			this.item = item;
			this.profil = profil;
			this.qty = qty;
			this.harga = harga;
			this.batch = batch;
		}
	}

	private static final class BarisRacikan {
		final Racikan racikan;
		final double qty;
		final double harga;
		final double diskon;
		final List<KomponenRacikan> komponen;
		BarisRacikan(Racikan racikan, double qty, double harga, double diskon,
				List<KomponenRacikan> komponen) {
			this.racikan = racikan;
			this.qty = qty;
			this.harga = harga;
			this.diskon = diskon;
			this.komponen = komponen;
		}
	}

	private static final class BarisItem {
		final ItemMedis item;
		final ApotikItemProfile profil;
		final double qty;
		final double harga;
		final double diskon;
		final List<AlokasiBatch> batch;
		BarisItem(ItemMedis item, ApotikItemProfile profil, double qty,
				double harga, double diskon, List<AlokasiBatch> batch) {
			this.item = item;
			this.profil = profil;
			this.qty = qty;
			this.harga = harga;
			this.diskon = diskon;
			this.batch = batch;
		}
	}

	private static final class BarisBayar {
		CaraPembayaranKoperasi cara;
		double nominal;
		Double tunai;
		Double kembalian;
		String referensi;
	}

	private static String str(Object nilai) {
		return nilai == null ? "" : nilai.toString();
	}

	private static Long optLong(JSONObject json, String kunci) {
		if (json == null || json.isNull(kunci)) return null;
		try { return Long.valueOf((json.get(kunci) + "").trim()); }
		catch (Exception e) { return null; }
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static ApotikItemProfile profil(Session session, ItemMedis item) {
		return (ApotikItemProfile) session.createCriteria(ApotikItemProfile.class)
				.add(Restrictions.eq("item", item)).setMaxResults(1).uniqueResult();
	}

	private static boolean kedaluwarsa(Kadaluarsa batch) {
		if (batch.getTanggalKadaluarsa() == null) return false;
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return batch.getTanggalKadaluarsa().before(c.getTime());
	}

	@SuppressWarnings("unchecked")
	private static List<RacikanDetail> detailRacikan(Session session, Racikan racikan) {
		return session.createCriteria(RacikanDetail.class)
				.add(Restrictions.eq("racikan", racikan)).addOrder(Order.asc("id")).list();
	}

	private static double hargaKomponen(RacikanDetail detail) {
		double tersimpan = detail.getHargaTransaksi() == null ? 0
				: detail.getHargaTransaksi().doubleValue();
		if (tersimpan > 0) return tersimpan;
		return detail.getItem() == null || detail.getItem().getDefaultHargaJual() == null
				? 0 : detail.getItem().getDefaultHargaJual().doubleValue();
	}

	/** Ringkasan satu racikan, dipakai katalog dan detail tebus resep. */
	static JSONObject ringkasRacikan(Session session, Racikan racikan, Long lokasiId)
			throws Exception {
		List<RacikanDetail> details = detailRacikan(session, racikan);
		List<Long> ids = new ArrayList<Long>();
		for (RacikanDetail d : details) {
			if (d.getItem() != null) ids.add(d.getItem().getId());
		}
		Map<Long, Double> stok = ApotikApiHelper.stokPerItem(session, ids, lokasiId);
		double harga = 0;
		double maksimal = Double.MAX_VALUE;
		boolean terkendali = false;
		boolean lasa = false;
		boolean highAlert = false;
		boolean coldChain = false;
		JSONArray komponen = new JSONArray();
		for (RacikanDetail d : details) {
			if (d.getItem() == null || d.getJumlah() == null || d.getJumlah() <= 0) continue;
			ItemMedis item = d.getItem();
			double jumlah = d.getJumlah().doubleValue();
			double s = stok.containsKey(item.getId()) ? stok.get(item.getId()).doubleValue() : 0;
			maksimal = Math.min(maksimal, Math.floor(s / jumlah));
			harga += hargaKomponen(d) * jumlah;
			ApotikItemProfile p = profil(session, item);
			terkendali = terkendali || (p != null && ApotikItemProfile.terkendali(p.getGolonganObat()));
			lasa = lasa || (p != null && Boolean.TRUE.equals(p.getLasa()));
			highAlert = highAlert || (p != null && Boolean.TRUE.equals(p.getHighAlert()));
			coldChain = coldChain || (p != null && Boolean.TRUE.equals(p.getColdChain()));
			JSONObject k = new JSONObject();
			k.put("itemId", item.getId());
			k.put("kode", str(item.getKode()));
			k.put("nama", str(item.getNama()));
			k.put("jumlah", jumlah);
			k.put("stok", s);
			komponen.put(k);
		}
		if (maksimal == Double.MAX_VALUE) maksimal = 0;
		JSONObject j = new JSONObject();
		j.put("id", racikan.getId());
		j.put("racikanId", racikan.getId());
		j.put("racikan", true);
		j.put("kode", str(racikan.getKode()));
		j.put("nama", str(racikan.getNama()).isEmpty() ? str(racikan.getKode()) : str(racikan.getNama()));
		j.put("satuan", "Paket");
		j.put("hargaJual", harga);
		j.put("stok", maksimal);
		j.put("terkendali", terkendali);
		j.put("lasa", lasa);
		j.put("highAlert", highAlert);
		j.put("coldChain", coldChain);
		j.put("jumlahKomponen", komponen.length());
		j.put("komponen", komponen);
		return j;
	}

	/** Daftar formula racikan yang benar-benar memiliki komponen. */
	public static void racikanList(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request == null ? "" : request.optString("keyword", "").trim();
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 40 : request.optInt("page_size", 40);
		if (size < 1) size = 40;
		if (size > 100) size = 100;
		Long lokasiId = optLong(request, "lokasi_id");
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(Racikan.class);
			org.hibernate.Criteria count = session.createCriteria(Racikan.class);
			if (!keyword.isEmpty()) {
				org.hibernate.criterion.Criterion cari = Restrictions.disjunction()
						.add(Restrictions.ilike("kode", "%" + keyword + "%"))
						.add(Restrictions.ilike("nama", "%" + keyword + "%"));
				c.add(cari);
				count.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", "%" + keyword + "%"))
						.add(Restrictions.ilike("nama", "%" + keyword + "%")));
			}
			long total = ((Number) count.setProjection(Projections.rowCount()).uniqueResult()).longValue();
			c.addOrder(Order.asc("nama")).addOrder(Order.asc("id"));
			c.setFirstResult((page - 1) * size).setMaxResults(size);
			@SuppressWarnings("unchecked")
			List<Racikan> daftar = c.list();
			JSONArray data = new JSONArray();
			for (Racikan r : daftar) {
				JSONObject ringkas = ringkasRacikan(session, r, lokasiId);
				if (ringkas.optInt("jumlahKomponen", 0) > 0) data.put(ringkas);
			}
			hasil.put("status", "00");
			hasil.put("data", data);
			hasil.put("page", page);
			hasil.put("pageSize", size);
			hasil.put("total", total);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<AlokasiBatch> alokasiFefo(Session session, ItemMedis item,
			double qty, Long lokasiId, Map<Long, Double> dipesan) throws Validasi {
		org.hibernate.Criteria c = session.createCriteria(Kadaluarsa.class)
				.add(Restrictions.eq("item", item))
				.addOrder(Order.asc("tanggalKadaluarsa")).addOrder(Order.asc("id"));
		if (lokasiId != null) c.createAlias("lokasi", "lokasi").add(Restrictions.eq("lokasi.id", lokasiId));
		List<Kadaluarsa> daftar = c.list();
		if (daftar.isEmpty()) return Collections.emptyList();
		List<Long> ids = new ArrayList<Long>();
		for (Kadaluarsa k : daftar) ids.add(k.getId());
		Map<Long, Double> konsumsi = ApotikApiHelper.konsumsiPerBatch(session, ids);
		List<AlokasiBatch> hasil = new ArrayList<AlokasiBatch>();
		double kurang = qty;
		for (Kadaluarsa k : daftar) {
			if (!Kadaluarsa.lotLayak(k.getStatusLot()) || kedaluwarsa(k)) continue;
			double sisa = (k.getQty() == null ? 0 : k.getQty().doubleValue())
					- (konsumsi.containsKey(k.getId()) ? konsumsi.get(k.getId()).doubleValue() : 0)
					- (dipesan.containsKey(k.getId()) ? dipesan.get(k.getId()).doubleValue() : 0);
			if (sisa <= 0) continue;
			double ambil = Math.min(kurang, sisa);
			hasil.add(new AlokasiBatch(k, ambil));
			Double sebelumnya = dipesan.get(k.getId());
			dipesan.put(k.getId(), Double.valueOf(
					(sebelumnya == null ? 0 : sebelumnya.doubleValue()) + ambil));
			kurang -= ambil;
			if (kurang <= 0.0001) break;
		}
		if (kurang > 0.0001) {
			throw new Validasi("Batch layak FEFO untuk \"" + str(item.getNama())
					+ "\" tidak cukup (kurang " + kurang + "). Transaksi ditahan.");
		}
		return hasil;
	}

	private static List<BarisBayar> muatPembayaran(Session session, JSONObject request,
			double total) throws Validasi {
		List<BarisBayar> daftar = new ArrayList<BarisBayar>();
		JSONArray arr = request == null ? null : request.optJSONArray("pembayaran");
		if (arr != null && arr.length() > 0) {
			double jumlah = 0;
			for (int i = 0; i < arr.length(); i++) {
				JSONObject b = arr.optJSONObject(i);
				Long id = optLong(b, "cara_bayar_id");
				CaraPembayaranKoperasi cara = id == null ? null
						: (CaraPembayaranKoperasi) session.get(CaraPembayaranKoperasi.class, id);
				if (cara == null || !Boolean.TRUE.equals(cara.getAktif()))
					throw new Validasi("Metode pembayaran baris ke-" + (i + 1) + " tidak valid.");
				double nominal = b.optDouble("nominal", 0);
				if (nominal <= 0) throw new Validasi("Nominal pembayaran harus lebih dari 0.");
				BarisBayar row = new BarisBayar();
				row.cara = cara;
				row.nominal = nominal;
				row.tunai = b.isNull("tunai") ? null : Double.valueOf(b.optDouble("tunai", 0));
				row.kembalian = b.isNull("kembalian") ? null : Double.valueOf(b.optDouble("kembalian", 0));
				row.referensi = b.optString("referensi", "").trim();
				daftar.add(row);
				jumlah += nominal;
			}
			if (Math.abs(jumlah - total) > 0.5)
				throw new Validasi("Jumlah pembayaran tidak sama dengan total transaksi.");
			return daftar;
		}
		Long id = optLong(request, "cara_bayar_id");
		if (id == null) return daftar;
		CaraPembayaranKoperasi cara = (CaraPembayaranKoperasi) session
				.get(CaraPembayaranKoperasi.class, id);
		if (cara == null || !Boolean.TRUE.equals(cara.getAktif()))
			throw new Validasi("Metode pembayaran tidak dikenal atau sudah nonaktif.");
		BarisBayar row = new BarisBayar();
		row.cara = cara;
		row.nominal = total;
		row.tunai = request.isNull("tunai") ? null : Double.valueOf(request.optDouble("tunai", 0));
		row.kembalian = request.isNull("kembalian") ? null : Double.valueOf(request.optDouble("kembalian", 0));
		row.referensi = request.optString("referensi_bayar", "").trim();
		daftar.add(row);
		return daftar;
	}

	private static JSONArray simpanPembayaran(Session session, TransaksiMedis trx,
			List<BarisBayar> daftar, Tbmuser user) throws Exception {
		JSONArray hasil = new JSONArray();
		for (BarisBayar b : daftar) {
			ApotikPembayaranTransaksi row = new ApotikPembayaranTransaksi();
			row.setTransaksi(trx);
			row.setCaraBayar(b.cara);
			row.setNamaCaraBayar(str(b.cara.getNama()));
			row.setNominal(Double.valueOf(b.nominal));
			row.setTunai(b.tunai);
			row.setKembalian(b.kembalian);
			row.setReferensi(b.referensi);
			row.setWaktu(new Date());
			row.setOleh(user.getUserId());
			row.setOlehId(user.getUserId());
			session.save(row);
			JSONObject j = new JSONObject();
			j.put("nama", str(b.cara.getNama()));
			j.put("nominal", b.nominal);
			hasil.put(j);
		}
		return hasil;
	}

	/**
	 * Penjualan racikan atau tebus resep campuran: obat jadi, komponen racikan,
	 * batch FEFO, register, dan pembayaran dibukukan dalam satu commit.
	 */
	public static void bayarRacikan(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		if (!ApotikApiHelper.bolehAksiMenu(user, "apotik_racikan", "create")
				&& !ApotikApiHelper.bolehAksiMenu(user, "apotik_kasir", "create")) {
			tolak(hasil, "Akun tidak berhak menjual racikan."); return;
		}
		JSONArray items = request == null ? null : request.optJSONArray("items");
		if (items == null || items.length() == 0) { tolak(hasil, "Minimal satu item atau racikan."); return; }
		Long ajId = ApotikKodeTransaksiHelper.pastikanId("AJ", "Apotik Jual", -1);
		Long lokasiId = optLong(request, "lokasi_id");
		Long resepId = optLong(request, "resep_id");
		String kode = request.optString("kode", "").trim();
		JSONObject pembeli = request.optJSONObject("pembeli");
		String namaPembeli = pembeli == null ? "" : pembeli.optString("nama", "").trim();
		String alamatPembeli = pembeli == null ? "" : pembeli.optString("alamat", "").trim();
		String namaDokter = request.optString("nama_dokter", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			if (!kode.isEmpty()) {
				TransaksiMedis lama = (TransaksiMedis) session.createCriteria(TransaksiMedis.class)
						.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
				if (lama != null) {
					hasil.put("status", "00"); hasil.put("id", lama.getId());
					hasil.put("kode", lama.getKode()); hasil.put("idempoten", true); return;
				}
			}
			KodeTransaksiMedis aj = (KodeTransaksiMedis) session.get(KodeTransaksiMedis.class, ajId);
			Lokasi lokasi = lokasiId == null ? null : (Lokasi) session.get(Lokasi.class, lokasiId);
			Resep resep = resepId == null ? null : (Resep) session.get(Resep.class, resepId);
			if (resepId != null && resep == null) throw new Validasi("Resep tidak ditemukan.");
			List<BarisRacikan> baris = new ArrayList<BarisRacikan>();
			List<BarisItem> barisItem = new ArrayList<BarisItem>();
			Map<Long, Double> kebutuhan = new HashMap<Long, Double>();
			Map<Long, Double> batchDipesan = new HashMap<Long, Double>();
			List<Long> komponenIds = new ArrayList<Long>();
			double total = 0;
			for (int i = 0; i < items.length(); i++) {
				JSONObject input = items.optJSONObject(i);
				Long id = optLong(input, "racikan_id");
				double qtyPaket = input == null ? 0 : input.optDouble("qty", 0);
				if (qtyPaket <= 0) throw new Validasi("Jumlah baris ke-" + (i + 1) + " tidak valid.");
				if (id == null) {
					Long itemId = optLong(input, "item_id");
					ItemMedis item = itemId == null ? null
							: (ItemMedis) session.get(ItemMedis.class, itemId);
					if (item == null) throw new Validasi("Obat jadi baris ke-" + (i + 1) + " tidak ditemukan.");
					ApotikItemProfile p = profil(session, item);
					String golongan = p == null ? ApotikItemProfile.GOLONGAN_BEBAS : p.getGolonganObat();
					if (ApotikItemProfile.terkendali(golongan)
							&& (namaPembeli.isEmpty() || (resep == null && namaDokter.isEmpty()))) {
						throw new Validasi("Obat terkendali \"" + str(item.getNama())
								+ "\": nama pasien dan resep/nama dokter wajib.");
					}
					double hargaDefault = item.getDefaultHargaJual() == null ? 0
							: item.getDefaultHargaJual().doubleValue();
					double harga = input.optDouble("harga_satuan", hargaDefault);
					double diskon = input.optDouble("diskon", 0);
					Double lama = kebutuhan.get(item.getId());
					kebutuhan.put(item.getId(), Double.valueOf(
							(lama == null ? 0 : lama.doubleValue()) + qtyPaket));
					komponenIds.add(item.getId());
					barisItem.add(new BarisItem(item, p, qtyPaket, harga, diskon,
							alokasiFefo(session, item, qtyPaket, lokasiId, batchDipesan)));
					total += qtyPaket * harga - diskon;
					continue;
				}
				Racikan racikan = (Racikan) session.get(Racikan.class, id);
				if (racikan == null) throw new Validasi("Racikan baris ke-" + (i + 1) + " tidak ditemukan.");
				List<RacikanDetail> formula = detailRacikan(session, racikan);
				if (formula.isEmpty()) throw new Validasi("Racikan \"" + str(racikan.getNama()) + "\" belum memiliki komposisi.");
				List<KomponenRacikan> komponen = new ArrayList<KomponenRacikan>();
				double hargaHitung = 0;
				for (RacikanDetail d : formula) {
					if (d.getItem() == null || d.getJumlah() == null || d.getJumlah() <= 0) continue;
					double jumlah = d.getJumlah().doubleValue() * qtyPaket;
					ApotikItemProfile p = profil(session, d.getItem());
					String golongan = p == null ? ApotikItemProfile.GOLONGAN_BEBAS : p.getGolonganObat();
					if (ApotikItemProfile.terkendali(golongan)
							&& (namaPembeli.isEmpty() || (resep == null && namaDokter.isEmpty()))) {
						throw new Validasi("Racikan memuat obat terkendali \"" + str(d.getItem().getNama())
								+ "\": nama pasien dan resep/nama dokter wajib.");
					}
					double h = hargaKomponen(d);
					hargaHitung += h * d.getJumlah().doubleValue();
					Double lama = kebutuhan.get(d.getItem().getId());
					kebutuhan.put(d.getItem().getId(), Double.valueOf((lama == null ? 0 : lama.doubleValue()) + jumlah));
					komponenIds.add(d.getItem().getId());
					komponen.add(new KomponenRacikan(d.getItem(), p, jumlah, h,
							alokasiFefo(session, d.getItem(), jumlah, lokasiId, batchDipesan)));
				}
				if (komponen.isEmpty()) throw new Validasi("Komposisi racikan tidak valid.");
				double harga = input.optDouble("harga_satuan", hargaHitung);
				double diskon = input.optDouble("diskon", 0);
				total += qtyPaket * harga - diskon;
				baris.add(new BarisRacikan(racikan, qtyPaket, harga, diskon, komponen));
			}
			Map<Long, Double> stok = ApotikApiHelper.stokPerItem(session, komponenIds, lokasiId);
			for (Map.Entry<Long, Double> e : kebutuhan.entrySet()) {
				double tersedia = stok.containsKey(e.getKey()) ? stok.get(e.getKey()).doubleValue() : 0;
				if (e.getValue().doubleValue() > tersedia)
					throw new Validasi("Stok komponen id " + e.getKey() + " tidak cukup (stok "
							+ tersedia + ", dibutuhkan " + e.getValue() + ").");
			}
			List<BarisBayar> pembayaran = muatPembayaran(session, request, total);
			tx = session.beginTransaction();
			TransaksiMedis trx = new TransaksiMedis();
			trx.setKode(kode.isEmpty() ? "APT-RAC-" + System.currentTimeMillis() : kode);
			trx.setJenisTransaksi(TransaksiMedis.TRX_ITEM);
			trx.setSumber(TransaksiMedis.SUMBER_APOTIK);
			trx.setBebas(Boolean.TRUE); trx.setLunas(Boolean.TRUE);
			trx.setTanggalTransaksi(new Date()); trx.setResep(resep); trx.setLokasi(lokasi);
			if (!namaPembeli.isEmpty()) trx.setNama(namaPembeli);
			if (!alamatPembeli.isEmpty()) trx.setAlamat(alamatPembeli);
			trx.setKeterangan(barisItem.isEmpty()
					? "Kasir Apotik - penjualan racikan"
					: "Kasir Apotik - tebus resep campuran");
			trx.setOleh(user.getUserId()); trx.setOlehId(user.getUserId());
			session.save(trx);
			for (BarisRacikan b : baris) {
				TransaksiMedisDetail detail = new TransaksiMedisDetail();
				detail.setTransaksi(trx); detail.setRacikan(b.racikan);
				detail.setQty(Double.valueOf(b.qty)); detail.setAmount(Double.valueOf(b.harga));
				detail.setDiskon(Double.valueOf(b.diskon)); detail.setTanggal(new Date());
				detail.setHasilPenghitunganTotal(Double.valueOf(b.qty * b.harga - b.diskon));
				detail.setOleh(user.getUserId()); detail.setOlehId(user.getUserId());
				session.save(detail);
				for (KomponenRacikan k : b.komponen) {
					DetailTransaksiPasien ledger = new DetailTransaksiPasien();
					ledger.setKodeTransaksi(aj); ledger.setItem(k.item); ledger.setQty(Double.valueOf(k.qty));
					ledger.setQtyBonus(Double.valueOf(0)); ledger.setAmount(Double.valueOf(k.harga));
					ledger.setTransaksiDetail(detail); ledger.setTanggal(new Date());
					ledger.setKeterangan("Komponen racikan " + str(b.racikan.getNama()));
					ledger.setLunas(Boolean.TRUE); ledger.setLokasi(lokasi);
					ledger.setOleh(user.getUserId()); ledger.setOlehId(user.getUserId());
					session.save(ledger);
					for (AlokasiBatch a : k.batch) {
						ApotikBatchKonsumsi konsumsi = new ApotikBatchKonsumsi();
						konsumsi.setKadaluarsa(a.batch); konsumsi.setTransaksiDetail(detail);
						konsumsi.setQty(Double.valueOf(a.qty)); konsumsi.setWaktu(new Date());
						konsumsi.setOleh(user.getUserId()); konsumsi.setOlehId(user.getUserId());
						session.save(konsumsi);
					}
					String golongan = k.profil == null ? ApotikItemProfile.GOLONGAN_BEBAS : k.profil.getGolonganObat();
					if (ApotikItemProfile.terkendali(golongan)) {
						ApotikNarkotikaLog log = new ApotikNarkotikaLog();
						log.setItem(k.item); log.setTransaksiDetail(detail); log.setResep(resep);
						log.setQty(Double.valueOf(k.qty)); log.setGolonganObat(golongan);
						log.setNamaPembeli(namaPembeli); log.setAlamatPembeli(alamatPembeli);
						log.setNamaDokter(namaDokter); log.setWaktu(new Date());
						log.setKeterangan("Komponen racikan " + str(b.racikan.getKode()));
						log.setOleh(user.getUserId()); log.setOlehId(user.getUserId());
						session.save(log);
					}
				}
			}
			for (BarisItem b : barisItem) {
				TransaksiMedisDetail detail = new TransaksiMedisDetail();
				detail.setTransaksi(trx); detail.setItem(b.item);
				detail.setQty(Double.valueOf(b.qty)); detail.setAmount(Double.valueOf(b.harga));
				detail.setDiskon(Double.valueOf(b.diskon)); detail.setTanggal(new Date());
				detail.setHasilPenghitunganTotal(Double.valueOf(b.qty * b.harga - b.diskon));
				detail.setOleh(user.getUserId()); detail.setOlehId(user.getUserId());
				session.save(detail);
				DetailTransaksiPasien ledger = new DetailTransaksiPasien();
				ledger.setKodeTransaksi(aj); ledger.setItem(b.item); ledger.setQty(Double.valueOf(b.qty));
				ledger.setQtyBonus(Double.valueOf(0)); ledger.setAmount(Double.valueOf(b.harga));
				ledger.setDiskon(Double.valueOf(b.diskon));
				ledger.setHasilPenghitunganTotal(Double.valueOf(b.qty * b.harga - b.diskon));
				ledger.setTransaksiDetail(detail); ledger.setTanggal(new Date());
				ledger.setKeterangan("Obat jadi pada tebus resep campuran");
				ledger.setLunas(Boolean.TRUE); ledger.setLokasi(lokasi);
				ledger.setOleh(user.getUserId()); ledger.setOlehId(user.getUserId());
				session.save(ledger);
				for (AlokasiBatch a : b.batch) {
					ApotikBatchKonsumsi konsumsi = new ApotikBatchKonsumsi();
					konsumsi.setKadaluarsa(a.batch); konsumsi.setTransaksiDetail(detail);
					konsumsi.setQty(Double.valueOf(a.qty)); konsumsi.setWaktu(new Date());
					konsumsi.setOleh(user.getUserId()); konsumsi.setOlehId(user.getUserId());
					session.save(konsumsi);
				}
				String golongan = b.profil == null ? ApotikItemProfile.GOLONGAN_BEBAS
						: b.profil.getGolonganObat();
				if (ApotikItemProfile.terkendali(golongan)) {
					ApotikNarkotikaLog log = new ApotikNarkotikaLog();
					log.setItem(b.item); log.setTransaksiDetail(detail); log.setResep(resep);
					log.setQty(Double.valueOf(b.qty)); log.setGolonganObat(golongan);
					log.setNamaPembeli(namaPembeli); log.setAlamatPembeli(alamatPembeli);
					log.setNamaDokter(namaDokter); log.setWaktu(new Date());
					log.setKeterangan("Obat jadi pada tebus resep campuran");
					log.setOleh(user.getUserId()); log.setOlehId(user.getUserId());
					session.save(log);
				}
			}
			JSONArray pembayaranJson = simpanPembayaran(session, trx, pembayaran, user);
			tx.commit();
			hasil.put("status", "00"); hasil.put("id", trx.getId()); hasil.put("kode", trx.getKode());
			hasil.put("total", total); hasil.put("pembayaran", pembayaranJson);
			StringBuilder cara = new StringBuilder();
			for (BarisBayar b : pembayaran) { if (cara.length() > 0) cara.append(" + "); cara.append(str(b.cara.getNama())); }
			hasil.put("caraBayar", cara.toString());
		} catch (Validasi e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
			tolak(hasil, e.getMessage());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
			ais.common.ErrorAuditUtil.record(e, "ApotikRacikanProduksiHelper.bayarRacikan");
			tolak(hasil, "Gagal menyimpan penjualan racikan: " + str(e.getMessage()));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Katalog barang jadi yang memiliki formula bahan baku produksi. */
	public static void produksiKatalog(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request == null ? "" : request.optString("keyword", "").trim().toLowerCase();
		Long lokasiId = optLong(request, "lokasi_id");
		int size = request == null ? 40 : request.optInt("page_size", 40);
		if (size < 1) size = 40; if (size > 100) size = 100;
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			List<ItemMedis> induk = session.createQuery(
					"select distinct b.itemInduk from BahanBakuItem b where b.itemInduk is not null order by b.itemInduk.nama")
					.setMaxResults(size * 3).list();
			JSONArray data = new JSONArray();
			for (ItemMedis item : induk) {
				if (!keyword.isEmpty() && !(str(item.getNama()).toLowerCase().contains(keyword)
						|| str(item.getKode()).toLowerCase().contains(keyword))) continue;
				@SuppressWarnings("unchecked")
				List<BahanBakuItem> formula = session.createCriteria(BahanBakuItem.class)
						.add(Restrictions.eq("itemInduk", item)).addOrder(Order.asc("id")).list();
				List<Long> ids = new ArrayList<Long>();
				for (BahanBakuItem b : formula) if (b.getItem() != null) ids.add(b.getItem().getId());
				Map<Long, Double> stok = ApotikApiHelper.stokPerItem(session, ids, lokasiId);
				double dapat = Double.MAX_VALUE;
				JSONArray komponen = new JSONArray();
				for (BahanBakuItem b : formula) {
					if (b.getItem() == null || b.getQty() == null || b.getQty() <= 0) continue;
					double s = stok.containsKey(b.getItem().getId()) ? stok.get(b.getItem().getId()) : 0;
					dapat = Math.min(dapat, Math.floor(s / b.getQty()));
					JSONObject k = new JSONObject(); k.put("nama", str(b.getItem().getNama()));
					k.put("jumlah", b.getQty()); k.put("stok", s); komponen.put(k);
				}
				if (dapat == Double.MAX_VALUE) dapat = 0;
				JSONObject j = new JSONObject(); j.put("id", item.getId()); j.put("itemId", item.getId());
				j.put("kode", str(item.getKode())); j.put("nama", str(item.getNama())); j.put("satuan", "Batch produksi");
				j.put("hargaJual", item.getDefaultHargaJual() == null ? 0 : item.getDefaultHargaJual());
				j.put("stok", dapat); j.put("jumlahKomponen", komponen.length()); j.put("komponen", komponen);
				j.put("produksi", true); data.put(j);
				if (data.length() >= size) break;
			}
			hasil.put("status", "00"); hasil.put("data", data); hasil.put("total", data.length());
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	/** Produksi disetujui langsung oleh petugas berhak, dengan mutasi bahan dan hasil satu commit. */
	public static void prosesProduksi(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		if (!ApotikApiHelper.bolehAksiMenu(user, "apotik_racikan", "create")
				&& !ApotikApiHelper.bolehAksiMenu(user, "apotik_kasir", "create")) {
			tolak(hasil, "Akun tidak berhak menjalankan produksi farmasi."); return;
		}
		JSONArray items = request == null ? null : request.optJSONArray("items");
		if (items == null || items.length() == 0) { tolak(hasil, "Minimal satu barang jadi."); return; }
		String kode = request.optString("kode", "").trim();
		if (kode.isEmpty()) { tolak(hasil, "Kode idempoten produksi wajib diisi."); return; }
		String tanggalText = request.optString("tanggal_kadaluarsa", "").trim();
		Date tanggalKadaluarsa;
		try { tanggalKadaluarsa = new SimpleDateFormat("yyyy-MM-dd").parse(tanggalText); }
		catch (Exception e) { tolak(hasil, "Tanggal kedaluwarsa hasil produksi wajib format yyyy-MM-dd."); return; }
		if (tanggalKadaluarsa.before(new Date())) { tolak(hasil, "Tanggal kedaluwarsa hasil produksi harus di masa depan."); return; }
		Long prodId = ApotikKodeTransaksiHelper.pastikanId("PROD", "Produksi Farmasi", 1);
		Long bbId = ApotikKodeTransaksiHelper.pastikanId("BB", "Bahan Baku Produksi", -1);
		Long lokasiId = optLong(request, "lokasi_id");
		String lotDasar = request.optString("nomor_batch", "").trim();
		if (lotDasar.isEmpty()) lotDasar = kode;
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			String kodePertama = items.length() == 1 ? kode : kode + "-1";
			Produksi lama = (Produksi) session.createCriteria(Produksi.class)
					.add(Restrictions.eq("kode", kodePertama)).setMaxResults(1).uniqueResult();
			if (lama != null) { hasil.put("status", "00"); hasil.put("id", lama.getId());
				hasil.put("kode", kode); hasil.put("idempoten", true); return; }
			Lokasi lokasi = lokasiId == null ? null : (Lokasi) session.get(Lokasi.class, lokasiId);
			KodeTransaksiMedis kodeProd = (KodeTransaksiMedis) session.get(KodeTransaksiMedis.class, prodId);
			KodeTransaksiMedis kodeBb = (KodeTransaksiMedis) session.get(KodeTransaksiMedis.class, bbId);
			List<ItemMedis> outputs = new ArrayList<ItemMedis>();
			List<Double> quantities = new ArrayList<Double>();
			List<List<BahanBakuItem>> formulas = new ArrayList<List<BahanBakuItem>>();
			Map<Long, Double> kebutuhan = new HashMap<Long, Double>();
			Map<Long, Double> batchDipesan = new HashMap<Long, Double>();
			List<Long> bahanIds = new ArrayList<Long>();
			for (int i = 0; i < items.length(); i++) {
				JSONObject row = items.optJSONObject(i); Long itemId = optLong(row, "item_id");
				double qty = row == null ? 0 : row.optDouble("qty", 0);
				ItemMedis output = itemId == null ? null : (ItemMedis) session.get(ItemMedis.class, itemId);
				if (output == null || qty <= 0) throw new Validasi("Baris produksi ke-" + (i + 1) + " tidak valid.");
				@SuppressWarnings("unchecked")
				List<BahanBakuItem> formula = session.createCriteria(BahanBakuItem.class)
						.add(Restrictions.eq("itemInduk", output)).addOrder(Order.asc("id")).list();
				if (formula.isEmpty()) throw new Validasi("Formula produksi \"" + str(output.getNama()) + "\" belum tersedia.");
				for (BahanBakuItem b : formula) {
					if (b.getItem() == null || b.getQty() == null || b.getQty() <= 0) continue;
					double perlu = b.getQty().doubleValue() * qty; Double ada = kebutuhan.get(b.getItem().getId());
					kebutuhan.put(b.getItem().getId(), Double.valueOf((ada == null ? 0 : ada.doubleValue()) + perlu));
					bahanIds.add(b.getItem().getId());
					// Validasi hanya lot yang layak dan belum kedaluwarsa bila bahan dikelola per batch.
					alokasiFefo(session, b.getItem(), perlu, lokasiId, batchDipesan);
				}
				outputs.add(output); quantities.add(Double.valueOf(qty)); formulas.add(formula);
			}
			Map<Long, Double> stok = ApotikApiHelper.stokPerItem(session, bahanIds, lokasiId);
			for (Map.Entry<Long, Double> e : kebutuhan.entrySet()) {
				double tersedia = stok.containsKey(e.getKey()) ? stok.get(e.getKey()) : 0;
				if (e.getValue() > tersedia) throw new Validasi("Stok bahan baku id " + e.getKey()
						+ " tidak cukup (stok " + tersedia + ", dibutuhkan " + e.getValue() + ").");
			}
			tx = session.beginTransaction();
			JSONArray dibuat = new JSONArray();
			for (int i = 0; i < outputs.size(); i++) {
				ItemMedis output = outputs.get(i); double qty = quantities.get(i);
				Produksi p = new Produksi(); p.setKode(items.length() == 1 ? kode : kode + "-" + (i + 1));
				p.setItem(output); p.setQty(Double.valueOf(qty)); p.setBiayaTambahan(Double.valueOf(0));
				p.setTanggalPembuatan(new Date()); p.setTanggalPersetujuan(new Date());
				p.setDibuatOleh(user); p.setDisetujuiOleh(user); p.setLokasi(lokasi);
				p.setKeterangan("Produksi farmasi POS; batch " + lotDasar);
				p.setOleh(user.getUserId()); p.setOlehId(user.getUserId()); session.save(p);
				double totalBiaya = 0;
				for (BahanBakuItem f : formulas.get(i)) {
					if (f.getItem() == null || f.getQty() == null || f.getQty() <= 0) continue;
					double harga = f.getItem().getDefaultHargaJual() == null ? 0 : f.getItem().getDefaultHargaJual();
					ProduksiDetail d = new ProduksiDetail(); d.setProduksi(p); d.setItem(f.getItem());
					d.setJumlah(f.getQty()); d.setHargaBeli(Double.valueOf(harga));
					d.setKeterangan(str(f.getKeterangan())); d.setOleh(user.getUserId()); d.setOlehId(user.getUserId());
					session.save(d); totalBiaya += harga * f.getQty() * qty;
					DetailTransaksiPasien keluar = new DetailTransaksiPasien(); keluar.setProduksiDetail(d);
					keluar.setKodeTransaksi(kodeBb); keluar.setItem(f.getItem()); keluar.setQty(Double.valueOf(f.getQty() * qty));
					keluar.setQtyBonus(Double.valueOf(0)); keluar.setAmount(Double.valueOf(harga)); keluar.setLokasi(lokasi);
					keluar.setTanggal(new Date()); keluar.setKeterangan("Bahan baku " + str(output.getNama()));
					keluar.setOleh(user.getUserId()); keluar.setOlehId(user.getUserId()); session.save(keluar);
				}
				p.setBiaya(Double.valueOf(totalBiaya)); p.setBiayaSatuan(Double.valueOf(totalBiaya / qty)); session.update(p);
				DetailTransaksiPasien masuk = new DetailTransaksiPasien(); masuk.setProduksi(p);
				masuk.setKodeTransaksi(kodeProd); masuk.setItem(output); masuk.setQty(Double.valueOf(qty));
				masuk.setQtyBonus(Double.valueOf(0)); masuk.setAmount(p.getBiayaSatuan()); masuk.setLokasi(lokasi);
				masuk.setTanggal(new Date()); masuk.setKeterangan("Hasil produksi " + str(output.getNama()));
				masuk.setOleh(user.getUserId()); masuk.setOlehId(user.getUserId()); session.save(masuk);
				Kadaluarsa batch = new Kadaluarsa(); batch.setItem(output); batch.setQty(Double.valueOf(qty));
				batch.setLokasi(lokasi); batch.setTanggalKadaluarsa(tanggalKadaluarsa); batch.setStatusLot(Kadaluarsa.LOT_ELIGIBLE);
				batch.setKeterangan(lotDasar + (outputs.size() == 1 ? "" : "-" + (i + 1)));
				batch.setOleh(user.getUserId()); batch.setOlehId(user.getUserId()); session.save(batch);
				JSONObject j = new JSONObject(); j.put("id", p.getId()); j.put("itemId", output.getId());
				j.put("nama", str(output.getNama())); j.put("qty", qty); j.put("batch", batch.getKeterangan()); dibuat.put(j);
			}
			tx.commit(); hasil.put("status", "00"); hasil.put("kode", kode); hasil.put("data", dibuat);
			hasil.put("jumlahProduksi", dibuat.length());
		} catch (Validasi e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
			tolak(hasil, e.getMessage());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
			ais.common.ErrorAuditUtil.record(e, "ApotikRacikanProduksiHelper.prosesProduksi");
			tolak(hasil, "Gagal memproses produksi farmasi: " + str(e.getMessage()));
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}
}
