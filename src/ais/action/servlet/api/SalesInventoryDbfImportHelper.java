package ais.action.servlet.api;

import java.math.BigDecimal;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.SatuanProduk;
import ais.database.model.inventory.StokOpname;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CustomerInventoryProfile;
import ais.database.model.koperasi.HargaBeliSupplier;
import ais.database.model.koperasi.HargaJualCustomer;
import ais.database.model.koperasi.SalesInventory;
import ais.database.model.koperasi.SupplierInventoryProfile;
import ais.database.model.library.Penyedia;

/**
 * <h3>Impor master legacy DBF (INVENTORY CONTROL FoxPro) -- aksi {@code si_import_legacy}.</h3>
 *
 * <p>Klien (tab "Impor DBF" Konfigurasi, varian Inventory &amp; Sales) mem-parse DBF DI PERANGKAT
 * (STOK/SUPPLIER/CUSTOMER/SALES/masterbl/masterjl) lalu mengirim baris ternormalisasi per batch
 * (maks {@link #MAKS_BARIS}) ke aksi ini. IDEMPOTEN: upsert by kode legacy (kunci rekonsiliasi,
 * teks apa adanya) -- menjalankan ulang impor TIDAK menggandakan data; record existing TIDAK
 * ditimpa (hanya field kosong yang diisi), sesuai aturan master berhistori.</p>
 *
 * <p>Saldo stok legacy TIDAK ditulis langsung ke {@code Produk.stok} (saldo = hasil ledger!) --
 * bila {@code buat_opname_awal=true}, produk BARU diberi baris {@link StokOpname} selisih =
 * stok legacy ("Migrasi DBF"), sehingga ledger menghasilkan saldo awal yang benar.</p>
 *
 * <p>Otorisasi: aktor {@code PEMILIK_SALES_INVENTORY} (atau ADMIN) -- permintaan pemilik:
 * fitur impor hanya utk Pemilik Usaha Sales/Inventory.</p>
 */
public final class SalesInventoryDbfImportHelper {

	private SalesInventoryDbfImportHelper() {
	}

	private static final int MAKS_BARIS = 500;

	private static String s(JSONObject r, String k) {
		return r.isNull(k) ? "" : r.optString(k, "").trim();
	}

	private static Double d(JSONObject r, String k) {
		return r.isNull(k) ? null : Double.valueOf(r.optDouble(k, 0));
	}

	private static java.util.Date tgl(JSONObject r, String k) {
		String v = s(r, k);
		try {
			return v.matches("\\d{4}-\\d{2}-\\d{2}") ? new java.text.SimpleDateFormat("yyyy-MM-dd").parse(v) : null;
		} catch (Exception e) {
			return null;
		}
	}

	public static void importLegacy(EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser,
			JSONObject request, JSONObject hasil) throws Exception {
		if (!ctx.admin && !EbisnisActorContextResolver.ACTOR_PEMILIK.equals(ctx.actorType)) {
			hasil.put("status", "91");
			hasil.put("description", "Impor DBF hanya untuk Pemilik Usaha Sales/Inventory.");
			return;
		}
		String jenis = request.optString("jenis", "").trim();
		JSONArray rows = request.optJSONArray("rows");
		if (rows == null || rows.length() == 0) {
			hasil.put("status", "91");
			hasil.put("description", "rows kosong.");
			return;
		}
		if (rows.length() > MAKS_BARIS) {
			hasil.put("status", "91");
			hasil.put("description", "Maksimal " + MAKS_BARIS + " baris per batch.");
			return;
		}
		Long tokoId = ctx.admin && !request.isNull("toko_id")
				? Long.valueOf((request.get("toko_id") + "").trim())
				: ctx.tokoId;
		boolean opnameAwal = request.optBoolean("buat_opname_awal", false);

		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		int dibuat = 0, diperbarui = 0, dilewati = 0, gagal = 0;
		JSONArray exceptions = new JSONArray();
		try {
			Toko toko = tokoId == null ? null : (Toko) session.get(Toko.class, tokoId);
			if (("produk".equals(jenis) || "sales".equals(jenis)) && toko == null) {
				hasil.put("status", "91");
				hasil.put("description", "Toko aktif wajib diketahui untuk impor " + jenis
						+ " (akun Pemilik harus terikat/memilih toko).");
				return;
			}
			tx = session.beginTransaction();
			for (int i = 0; i < rows.length(); i++) {
				JSONObject r = rows.getJSONObject(i);
				try {
					int status;
					if ("supplier".equals(jenis)) {
						status = importSupplier(session, tbmuser, r);
					} else if ("customer".equals(jenis)) {
						status = importCustomer(session, tbmuser, r);
					} else if ("sales".equals(jenis)) {
						status = importSales(session, tbmuser, r, toko);
					} else if ("produk".equals(jenis)) {
						status = importProduk(session, tbmuser, r, toko, opnameAwal);
					} else if ("harga_beli".equals(jenis)) {
						status = importHargaBeli(session, tbmuser, r, toko);
					} else if ("harga_jual".equals(jenis)) {
						status = importHargaJual(session, tbmuser, r, toko);
					} else {
						tx.rollback();
						hasil.put("status", "91");
						hasil.put("description", "jenis tidak dikenal: " + jenis);
						return;
					}
					if (status == 1) dibuat++;
					else if (status == 2) diperbarui++;
					else dilewati++;
				} catch (Exception e) {
					gagal++;
					if (exceptions.length() < 50) {
						exceptions.put("Baris " + (i + 1) + " (" + s(r, "kode") + s(r, "kode_produk")
								+ "): " + e.getMessage());
					}
				}
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("dibuat", dibuat);
			hasil.put("diperbarui", diperbarui);
			hasil.put("dilewati", dilewati);
			hasil.put("gagal", gagal);
			hasil.put("exceptions", exceptions);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// return 1=dibuat 2=diperbarui 0=dilewati
	private static int importSupplier(Session session, Tbmuser tbmuser, JSONObject r) throws Exception {
		String kode = s(r, "kode");
		if (kode.isEmpty()) throw new Exception("kode kosong");
		Penyedia p = (Penyedia) session.createCriteria(Penyedia.class)
				.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
		boolean baru = p == null;
		if (baru) {
			p = new Penyedia();
			p.setKode(kode);
		}
		boolean ubah = false;
		if (isiBilaKosong(p.getNama(), s(r, "nama"))) { p.setNama(s(r, "nama")); ubah = true; }
		if (isiBilaKosong(p.getAlamat(), s(r, "alamat"))) { p.setAlamat(s(r, "alamat")); ubah = true; }
		if (isiBilaKosong(p.getTelp(), s(r, "telp"))) { p.setTelp(s(r, "telp")); ubah = true; }
		if (baru || ubah) {
			p.setOleh(tbmuser.getUserId());
			session.saveOrUpdate(p);
		}
		SupplierInventoryProfile sp = (SupplierInventoryProfile) session
				.createCriteria(SupplierInventoryProfile.class)
				.add(Restrictions.eq("penyedia", p)).setMaxResults(1).uniqueResult();
		boolean profilBaru = sp == null;
		if (profilBaru) {
			sp = new SupplierInventoryProfile();
			sp.setPenyedia(p);
		}
		boolean ubahProfil = false;
		Double termin = d(r, "termin");
		if (termin != null && (profilBaru || sp.getTerminHari().intValue() == 0)) {
			sp.setTerminHari(Integer.valueOf(termin.intValue())); ubahProfil = true;
		}
		if (isiBilaKosong(sp.getWilayah(), s(r, "wilayah"))) { sp.setWilayah(s(r, "wilayah")); ubahProfil = true; }
		if (isiBilaKosong(sp.getNoRekening(), s(r, "rekening"))) { sp.setNoRekening(s(r, "rekening")); ubahProfil = true; }
		if (isiBilaKosong(sp.getAtasNama(), s(r, "atas_nama"))) { sp.setAtasNama(s(r, "atas_nama")); ubahProfil = true; }
		if (isiBilaKosong(sp.getBank(), s(r, "bank"))) { sp.setBank(s(r, "bank")); ubahProfil = true; }
		if (isiBilaKosong(sp.getAlamatBank(), s(r, "alamat_bank"))) { sp.setAlamatBank(s(r, "alamat_bank")); ubahProfil = true; }
		if (profilBaru || ubahProfil) {
			sp.setOleh(tbmuser.getUserId());
			session.saveOrUpdate(sp);
		}
		return baru ? 1 : ((ubah || ubahProfil || profilBaru) ? 2 : 0);
	}

	private static int importCustomer(Session session, Tbmuser tbmuser, JSONObject r) throws Exception {
		String kode = s(r, "kode");
		if (kode.isEmpty()) throw new Exception("kode kosong");
		AnggotaKoperasi a = (AnggotaKoperasi) session.createCriteria(AnggotaKoperasi.class)
				.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
		boolean baru = a == null;
		if (baru) {
			a = new AnggotaKoperasi();
			a.setKode(kode);
		}
		boolean ubah = false;
		if (isiBilaKosong(a.getNama(), s(r, "nama"))) { a.setNama(s(r, "nama")); ubah = true; }
		if (isiBilaKosong(a.getAlamat(), s(r, "alamat"))) { a.setAlamat(s(r, "alamat")); ubah = true; }
		if (isiBilaKosong(a.getTelp(), s(r, "telp"))) { a.setTelp(s(r, "telp")); ubah = true; }
		if (baru || ubah) {
			session.saveOrUpdate(a);
		}
		CustomerInventoryProfile cp = (CustomerInventoryProfile) session
				.createCriteria(CustomerInventoryProfile.class)
				.add(Restrictions.eq("anggotaKoperasi", a)).setMaxResults(1).uniqueResult();
		boolean profilBaru = cp == null;
		if (profilBaru) {
			cp = new CustomerInventoryProfile();
			cp.setAnggotaKoperasi(a);
		}
		boolean ubahProfil = false;
		Double termin = d(r, "termin");
		if (termin != null && (profilBaru || cp.getTerminHari().intValue() == 0)) {
			cp.setTerminHari(Integer.valueOf(termin.intValue())); ubahProfil = true;
		}
		Double diskon = d(r, "diskon");
		if (diskon != null && (profilBaru || cp.getDiskonDefaultPersen().signum() == 0)) {
			cp.setDiskonDefaultPersen(new BigDecimal(String.valueOf(diskon))); ubahProfil = true;
		}
		if (isiBilaKosong(cp.getWilayah(), s(r, "wilayah"))) { cp.setWilayah(s(r, "wilayah")); ubahProfil = true; }
		if (isiBilaKosong(cp.getNoRekening(), s(r, "rekening"))) { cp.setNoRekening(s(r, "rekening")); ubahProfil = true; }
		if (isiBilaKosong(cp.getAtasNama(), s(r, "atas_nama"))) { cp.setAtasNama(s(r, "atas_nama")); ubahProfil = true; }
		if (isiBilaKosong(cp.getBank(), s(r, "bank"))) { cp.setBank(s(r, "bank")); ubahProfil = true; }
		if (profilBaru || ubahProfil) {
			cp.setOleh(tbmuser.getUserId());
			session.saveOrUpdate(cp);
		}
		return baru ? 1 : ((ubah || ubahProfil || profilBaru) ? 2 : 0);
	}

	private static int importSales(Session session, Tbmuser tbmuser, JSONObject r, Toko toko) throws Exception {
		String kode = s(r, "kode");
		if (kode.isEmpty()) throw new Exception("kode kosong");
		SalesInventory sI = (SalesInventory) session.createCriteria(SalesInventory.class)
				.add(Restrictions.eq("kode", kode))
				.add(Restrictions.eq("toko", toko)).setMaxResults(1).uniqueResult();
		boolean baru = sI == null;
		if (baru) {
			sI = new SalesInventory();
			sI.setKode(kode);
			sI.setToko(toko);
		}
		boolean ubah = false;
		if (isiBilaKosong(sI.getNama(), s(r, "nama"))) { sI.setNama(s(r, "nama")); ubah = true; }
		if (isiBilaKosong(sI.getNomorPerkiraan(), s(r, "no_perkiraan"))) {
			sI.setNomorPerkiraan(s(r, "no_perkiraan")); ubah = true;
		}
		if (baru || ubah) {
			sI.setOleh(tbmuser.getUserId());
			sI.setOlehId(tbmuser.getUserId());
			session.saveOrUpdate(sI);
		}
		return baru ? 1 : (ubah ? 2 : 0);
	}

	private static int importProduk(Session session, Tbmuser tbmuser, JSONObject r, Toko toko,
			boolean opnameAwal) throws Exception {
		String kode = s(r, "kode");
		if (kode.isEmpty()) throw new Exception("kode kosong");
		Produk p = (Produk) session.createCriteria(Produk.class)
				.add(Restrictions.eq("kode", kode))
				.add(Restrictions.eq("toko", toko)).setMaxResults(1).uniqueResult();
		boolean baru = p == null;
		if (baru) {
			p = new Produk();
			p.setKode(kode);
			p.setToko(toko);
			p.setAktif(Boolean.TRUE);
			p.setJenisItem("JUAL");
		}
		boolean ubah = false;
		if (isiBilaKosong(p.getNama(), s(r, "nama"))) { p.setNama(s(r, "nama")); ubah = true; }
		Double hargaBeli = d(r, "harga_beli");
		if (hargaBeli != null && (baru || p.getHargaBeli() == null || p.getHargaBeli().doubleValue() == 0)) {
			p.setHargaBeli(hargaBeli); ubah = true;
		}
		Double hargaJual = d(r, "harga_jual");
		if (hargaJual != null && (baru || p.getHargaJual() == null || p.getHargaJual().doubleValue() == 0)) {
			p.setHargaJual(hargaJual); ubah = true;
		}
		Double stokMin = d(r, "stok_minimum");
		if (stokMin != null && (baru || p.getStokMinimum() == null || p.getStokMinimum().doubleValue() == 0)) {
			p.setStokMinimum(stokMin); ubah = true;
		}
		String namaSatuan = s(r, "satuan");
		if (!namaSatuan.isEmpty() && p.getSatuan() == null) {
			SatuanProduk sat = (SatuanProduk) session.createCriteria(SatuanProduk.class)
					.add(Restrictions.eq("nama", namaSatuan).ignoreCase()).setMaxResults(1).uniqueResult();
			if (sat == null) {
				sat = new SatuanProduk();
				sat.setNama(namaSatuan);
				session.save(sat);
			}
			p.setSatuan(sat);
			ubah = true;
		}
		if (baru || ubah) {
			session.saveOrUpdate(p);
		}
		Double stokLegacy = d(r, "stok_legacy");
		if (baru && opnameAwal && stokLegacy != null && stokLegacy.doubleValue() != 0) {
			// Saldo awal legacy MASUK LEWAT LEDGER (opname), bukan tulis langsung Produk.stok.
			StokOpname so = new StokOpname();
			so.setProduk(p);
			so.setToko(toko);
			so.setStokSistem(Double.valueOf(0));
			so.setStokFisik(stokLegacy);
			so.setSelisih(stokLegacy);
			so.setWaktuOpname(ais.ui.util.WaktuUtil.getDate());
			so.setKeterangan("Migrasi DBF (saldo awal legacy STOK.DBF)");
			so.setOleh(tbmuser.getUserId());
			session.save(so);
			p.setStok(stokLegacy);
			session.saveOrUpdate(p);
		}
		return baru ? 1 : (ubah ? 2 : 0);
	}

	private static int importHargaBeli(Session session, Tbmuser tbmuser, JSONObject r, Toko toko) throws Exception {
		String kodeSupplier = s(r, "kode_supplier");
		String kodeProduk = s(r, "kode_produk");
		java.util.Date tanggal = tgl(r, "tanggal");
		Double harga = d(r, "harga");
		if (kodeSupplier.isEmpty() || kodeProduk.isEmpty() || tanggal == null || harga == null) {
			throw new Exception("kode_supplier/kode_produk/tanggal/harga tidak lengkap");
		}
		Penyedia sup = (Penyedia) session.createCriteria(Penyedia.class)
				.add(Restrictions.eq("kode", kodeSupplier)).setMaxResults(1).uniqueResult();
		if (sup == null) throw new Exception("supplier " + kodeSupplier + " belum ada (impor SUPPLIER.DBF dulu)");
		Produk prod = cariProduk(session, kodeProduk, toko);
		if (prod == null) throw new Exception("produk " + kodeProduk + " belum ada (impor STOK.DBF dulu)");
		HargaBeliSupplier ada = (HargaBeliSupplier) session.createCriteria(HargaBeliSupplier.class)
				.add(Restrictions.eq("supplier", sup))
				.add(Restrictions.eq("produk", prod))
				.add(Restrictions.eq("tanggalEfektif", tanggal)).setMaxResults(1).uniqueResult();
		if (ada != null) return 0;
		HargaBeliSupplier h = new HargaBeliSupplier();
		h.setSupplier(sup);
		h.setProduk(prod);
		h.setTanggalEfektif(tanggal);
		h.setHarga(new BigDecimal(String.valueOf(harga)));
		h.setKeterangan("Migrasi DBF masterbl");
		h.setOleh(tbmuser.getUserId());
		session.save(h);
		return 1;
	}

	private static int importHargaJual(Session session, Tbmuser tbmuser, JSONObject r, Toko toko) throws Exception {
		String kodeCustomer = s(r, "kode_customer");
		String kodeProduk = s(r, "kode_produk");
		java.util.Date tanggal = tgl(r, "tanggal");
		Double harga = d(r, "harga");
		if (kodeProduk.isEmpty() || tanggal == null || harga == null) {
			throw new Exception("kode_produk/tanggal/harga tidak lengkap");
		}
		AnggotaKoperasi cust = null;
		if (!kodeCustomer.isEmpty()) {
			cust = (AnggotaKoperasi) session.createCriteria(AnggotaKoperasi.class)
					.add(Restrictions.eq("kode", kodeCustomer)).setMaxResults(1).uniqueResult();
			if (cust == null) throw new Exception("customer " + kodeCustomer + " belum ada (impor CUSTOMER.DBF dulu)");
		}
		Produk prod = cariProduk(session, kodeProduk, toko);
		if (prod == null) throw new Exception("produk " + kodeProduk + " belum ada (impor STOK.DBF dulu)");
		org.hibernate.Criteria c = session.createCriteria(HargaJualCustomer.class)
				.add(Restrictions.eq("produk", prod))
				.add(Restrictions.eq("tanggalEfektif", tanggal));
		if (cust == null) c.add(Restrictions.isNull("anggotaKoperasi"));
		else c.add(Restrictions.eq("anggotaKoperasi", cust));
		if (c.setMaxResults(1).uniqueResult() != null) return 0;
		HargaJualCustomer h = new HargaJualCustomer();
		h.setAnggotaKoperasi(cust);
		h.setProduk(prod);
		h.setTanggalEfektif(tanggal);
		h.setHarga(new BigDecimal(String.valueOf(harga)));
		h.setKeterangan("Migrasi DBF masterjl");
		h.setOleh(tbmuser.getUserId());
		session.save(h);
		return 1;
	}

	private static Produk cariProduk(Session session, String kode, Toko toko) {
		if (toko != null) {
			Produk p = (Produk) session.createCriteria(Produk.class)
					.add(Restrictions.eq("kode", kode))
					.add(Restrictions.eq("toko", toko)).setMaxResults(1).uniqueResult();
			if (p != null) return p;
		}
		return (Produk) session.createCriteria(Produk.class)
				.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
	}

	private static boolean isiBilaKosong(String nilaiSekarang, String nilaiBaru) {
		return (nilaiSekarang == null || nilaiSekarang.trim().isEmpty()) && !nilaiBaru.isEmpty();
	}
}
