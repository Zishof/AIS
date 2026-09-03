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
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.PengadaanProduk;
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
		boolean jalurTenant = SalesInventoryTenantSchema.aktif(ctx);
		if (jalurTenant && !SalesInventoryDbfImportTenant.jenisDidukung(
				request.optString("jenis", "").trim())) {
			// GAGAL-TERTUTUP untuk jenis yang jalur tenantnya belum ditulis. Menjalankan jalur
			// legacy di sini akan membuat master tenant mendarat di schema BERSAMA sementara
			// schema tenantnya sendiri tetap kosong — impor melapor sukses, lalu layarnya
			// tidak menampilkan apa pun. Ditolak dengan MENYEBUT jenis yang sudah bisa.
			hasil.put("status", "91");
			hasil.put("description", "Impor DBF jenis \"" + request.optString("jenis", "").trim()
					+ "\" belum tersedia pada schema tenant. Yang sudah bisa: "
					+ SalesInventoryDbfImportTenant.daftarJenisDidukung() + ".");
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
		// Medan DBF yang tidak punya rumah pada model tenant. Dikumpulkan lalu dilaporkan,
		// bukan dibuang diam-diam: impor yang menelan kolom tanpa berkata apa-apa adalah impor
		// yang datanya hilang tanpa jejak.
		java.util.Set<String> peringatan = new java.util.LinkedHashSet<String>();
		try {
			// Pada jalur tenant, Toko adalah entitas ber-schema tersemat: membacanya akan
			// menoleh ke schema BERSAMA. Yang diperlukan di bawah hanya id-nya.
			Toko toko = (jalurTenant || tokoId == null) ? null
					: (Toko) session.get(Toko.class, tokoId);
			if (("produk".equals(jenis) || "sales".equals(jenis)
					|| "pembelian_legacy".equals(jenis) || "penjualan_legacy".equals(jenis))
					&& (jalurTenant ? tokoId == null : toko == null)) {
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
					if (jalurTenant) {
						status = imporBarisTenant(session, ctx, tbmuser, r, jenis, tokoId,
								opnameAwal, peringatan);
					} else if ("supplier".equals(jenis)) {
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
					} else if ("pembelian_legacy".equals(jenis)) {
						status = importPembelianLegacy(session, tbmuser, r, toko);
					} else if ("penjualan_legacy".equals(jenis)) {
						status = importPenjualanLegacy(session, tbmuser, r, toko);
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
			if (!peringatan.isEmpty()) {
				JSONArray dilewatiMedan = new JSONArray();
				for (java.util.Iterator<String> it = peringatan.iterator(); it.hasNext();) {
					dilewatiMedan.put(it.next());
				}
				hasil.put("medanDilewati", dilewatiMedan);
				hasil.put("peringatan", "Sebagian medan DBF tidak punya kolom padanan pada model"
						+ " tenant dan TIDAK tersimpan. Lihat medanDilewati.");
			}
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

	private static int importPembelianLegacy(Session session, Tbmuser tbmuser, JSONObject r,
			Toko toko) throws Exception {
		String faktur = s(r, "nomor_faktur");
		String kodeProduk = s(r, "kode_produk");
		java.util.Date tanggal = tgl(r, "tanggal");
		Double qty = d(r, "qty");
		Double harga = d(r, "harga_beli");
		if (faktur.isEmpty() || kodeProduk.isEmpty() || tanggal == null || qty == null || harga == null) {
			throw new Exception("nomor_faktur/kode_produk/tanggal/qty/harga_beli tidak lengkap");
		}
		Produk produk = cariProduk(session, kodeProduk, toko);
		if (produk == null) throw new Exception("produk " + kodeProduk + " belum ada (impor STOK.DBF dulu)");
		PengadaanProduk ada = (PengadaanProduk) session.createCriteria(PengadaanProduk.class)
				.add(Restrictions.eq("nomorFaktur", faktur))
				.add(Restrictions.eq("produk", produk))
				.add(Restrictions.eq("toko", toko))
				.add(Restrictions.eq("waktuPengadaan", tanggal))
				.setMaxResults(1).uniqueResult();
		if (ada != null) return 0;
		PengadaanProduk pengadaan = new PengadaanProduk();
		pengadaan.setNomorFaktur(faktur);
		pengadaan.setProduk(produk);
		pengadaan.setToko(toko);
		pengadaan.setQty(qty);
		pengadaan.setHargaBeliSatuan(harga);
		pengadaan.setTotalHarga(Double.valueOf(qty.doubleValue() * harga.doubleValue()));
		pengadaan.setWaktuPengadaan(tanggal);
		pengadaan.setNamaSupplier(s(r, "kode_supplier"));
		pengadaan.setKeterangan("Migrasi BELI.DBF; batch=" + s(r, "nomor_batch")
				+ "; ED=" + s(r, "tanggal_expired"));
		pengadaan.setOleh(tbmuser.getUserId());
		session.save(pengadaan);
		return 1;
	}

	private static int importPenjualanLegacy(Session session, Tbmuser tbmuser, JSONObject r,
			Toko toko) throws Exception {
		String faktur = s(r, "nomor_faktur");
		String kodeProduk = s(r, "kode_produk");
		java.util.Date tanggal = tgl(r, "tanggal");
		Double qty = d(r, "qty");
		Double harga = d(r, "harga_jual");
		if (faktur.isEmpty() || kodeProduk.isEmpty() || tanggal == null || qty == null || harga == null) {
			throw new Exception("nomor_faktur/kode_produk/tanggal/qty/harga_jual tidak lengkap");
		}
		Produk produk = cariProduk(session, kodeProduk, toko);
		if (produk == null) throw new Exception("produk " + kodeProduk + " belum ada (impor STOK.DBF dulu)");
		String kodeLegacy = potong("LEGACY-JUAL-" + faktur + "-" + kodeProduk + "-"
				+ s(r, "nomor_batch") + "-" + new java.text.SimpleDateFormat("yyyyMMdd").format(tanggal), 250);
		Pembelian ada = (Pembelian) session.createCriteria(Pembelian.class)
				.add(Restrictions.eq("kode", kodeLegacy))
				.add(Restrictions.eq("toko", toko)).setMaxResults(1).uniqueResult();
		if (ada != null) return 0;
		Pembelian penjualan = new Pembelian();
		penjualan.setKode(kodeLegacy);
		penjualan.setNama(s(r, "nama_produk"));
		penjualan.setProduk(produk);
		penjualan.setToko(toko);
		penjualan.setQty(qty);
		penjualan.setHargaSatuan(harga);
		penjualan.setHargaJual(Double.valueOf(qty.doubleValue() * harga.doubleValue()));
		penjualan.setTotal(Double.valueOf(qty.doubleValue() * harga.doubleValue()));
		penjualan.setWaktu(tanggal);
		penjualan.setAktif(Boolean.TRUE);
		penjualan.setTbmuser(tbmuser);
		penjualan.setKeterangan("Migrasi JUAL.DBF faktur " + faktur + "; customer="
				+ s(r, "kode_customer") + "; sales=" + s(r, "kode_sales") + "; batch="
				+ s(r, "nomor_batch"));
		penjualan.setOleh(tbmuser.getUserId());
		penjualan.setOlehId(tbmuser.getUserId());
		session.save(penjualan);
		return 1;
	}

	private static String potong(String nilai, int maksimum) {
		return nilai == null || nilai.length() <= maksimum ? nilai : nilai.substring(0, maksimum);
	}

	// =============================================================================================
	// Jalur schema tenant (§23). Enam jenis master; dua jenis transaksional ditolak di atas.
	// =============================================================================================

	/** Satu id hasil kueri berparameter tunggal, atau {@code null}. */
	private static Long satuId(Session session, String sql, Object p1) throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
		try {
			ps.setObject(1, p1);
			java.sql.ResultSet rs = ps.executeQuery();
			Long v = rs.next() ? Long.valueOf(rs.getLong(1)) : null;
			rs.close();
			return v;
		} finally {
			ps.close();
		}
	}

	/** Menyisipkan satu baris lalu mengembalikan id-nya lewat {@code RETURNING}. */
	private static Long sisipKembalikanId(Session session, String sql, Object[] params)
			throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
		try {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]);
			}
			java.sql.ResultSet rs = ps.executeQuery();
			Long v = rs.next() ? Long.valueOf(rs.getLong(1)) : null;
			rs.close();
			return v;
		} finally {
			ps.close();
		}
	}

	/** Menjalankan satu pernyataan dan mengembalikan jumlah baris tersentuh. */
	private static int jalankan(Session session, String sql, Object[] params) throws Exception {
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
		try {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]);
			}
			return ps.executeUpdate();
		} finally {
			ps.close();
		}
	}

	/** Catat medan DBF yang tidak punya kolom padanan, bila baris ini memang mengisinya. */
	private static void lewati(java.util.Set<String> peringatan, JSONObject r, String medan) {
		if (!s(r, medan).isEmpty()) {
			peringatan.add(medan);
		}
	}

	/**
	 * Satu baris impor pada schema tenant.
	 *
	 * <p>Nilai kembaliannya sama dengan jalur legacy: 1 = dibuat, 2 = diperbarui, 0 = dilewati.
	 * Perbedaan hasilnya bukan pada penghitungan itu melainkan pada tempat datanya mendarat.</p>
	 */
	private static int imporBarisTenant(Session session,
			EbisnisActorContextResolver.ActorContext ctx, Tbmuser tbmuser, JSONObject r,
			String jenis, Long tokoId, boolean opnameAwal, java.util.Set<String> peringatan)
			throws Exception {
		String sk = SalesInventoryDbfImportTenant.skema(ctx);
		String oleh = tbmuser == null || tbmuser.getUserId() == null ? "" : tbmuser.getUserId();
		if ("supplier".equals(jenis) || "customer".equals(jenis)) {
			return imporMitraTenant(session, sk, r, "supplier".equals(jenis), oleh, peringatan);
		}
		if ("sales".equals(jenis)) {
			return imporSalesTenant(session, sk, r, tokoId, oleh);
		}
		if ("produk".equals(jenis)) {
			return imporProdukTenant(session, sk, r, tokoId, opnameAwal, oleh, peringatan);
		}
		if ("harga_beli".equals(jenis)) {
			return imporHargaBeliTenant(session, sk, r, oleh);
		}
		if ("harga_jual".equals(jenis)) {
			return imporHargaJualTenant(session, sk, r, oleh);
		}
		if ("pembelian_legacy".equals(jenis) || "penjualan_legacy".equals(jenis)) {
			return imporRiwayatTenant(session, sk, r, "pembelian_legacy".equals(jenis), tokoId,
					oleh);
		}
		throw new Exception("jenis tidak dikenal pada jalur tenant: " + jenis);
	}

	/**
	 * Supplier/customer: induk + profilnya, dengan aturan ISI BILA KOSONG yang ditegakkan di SQL.
	 *
	 * <p>Empat medan legacy ({@code wilayah}, {@code rekening}, {@code bank}, dan — untuk
	 * supplier — {@code atas_nama}) tidak punya kolom padanan pada profil tenant. Keduanya
	 * dilewati dan dilaporkan lewat {@code medanDilewati}.</p>
	 */
	private static int imporMitraTenant(Session session, String sk, JSONObject r,
			boolean supplierMode, String oleh, java.util.Set<String> peringatan) throws Exception {
		String kode = s(r, "kode");
		if (kode.isEmpty()) {
			throw new Exception("kode kosong");
		}
		String tabel = supplierMode ? "supplier" : "customer";
		Long id = satuId(session, SalesInventoryDbfImportTenant.cariKode(sk, tabel), kode);
		boolean baru = id == null;
		int tersentuh = 0;
		if (baru) {
			id = sisipKembalikanId(session, SalesInventoryDbfImportTenant.sisipMitra(sk, tabel),
					new Object[] { kode, s(r, "nama"), oleh });
			if (id == null) {
				throw new Exception("gagal menyisipkan " + tabel + " " + kode);
			}
		} else {
			tersentuh += jalankan(session,
					SalesInventoryDbfImportTenant.isiNamaMitra(sk, tabel),
					new Object[] { s(r, "nama"), id });
		}
		Double termin = d(r, "termin");
		Integer terminHari = termin == null ? null : Integer.valueOf(termin.intValue());
		lewati(peringatan, r, "wilayah");
		lewati(peringatan, r, "rekening");
		lewati(peringatan, r, "bank");
		if (supplierMode) {
			lewati(peringatan, r, "atas_nama");
			lewati(peringatan, r, "alamat_bank");
			tersentuh += jalankan(session, SalesInventoryDbfImportTenant.sisipProfilSupplier(sk),
					new Object[] { id, s(r, "alamat"), null, s(r, "telp"), terminHari, oleh, id });
			tersentuh += jalankan(session, SalesInventoryDbfImportTenant.isiProfilSupplier(sk),
					new Object[] { s(r, "alamat"), null, s(r, "telp"), terminHari, id });
		} else {
			Double diskon = d(r, "diskon");
			java.math.BigDecimal diskonBd = diskon == null ? null
					: new java.math.BigDecimal(String.valueOf(diskon));
			tersentuh += jalankan(session, SalesInventoryDbfImportTenant.sisipProfilCustomer(sk),
					new Object[] { id, s(r, "alamat"), null, s(r, "telp"), s(r, "atas_nama"),
							terminHari, diskonBd, oleh, id });
			tersentuh += jalankan(session, SalesInventoryDbfImportTenant.isiProfilCustomer(sk),
					new Object[] { s(r, "alamat"), null, s(r, "telp"), s(r, "atas_nama"),
							terminHari, diskonBd, id });
		}
		return baru ? 1 : (tersentuh > 0 ? 2 : 0);
	}

	/**
	 * Sales: {@code salesperson} berlaku SE-TENANT, dan yang mengikatnya ke toko adalah
	 * {@code sales_assignment}.
	 *
	 * <p>Konsekuensi yang dicatat: jalur legacy mencari sales per (kode, toko), sehingga dua
	 * toko boleh punya sales berkode sama. Pada tenant kodenya unik se-tenant — baris kedua
	 * berkode sama akan dikenali sebagai sales yang SAMA, lalu ditambahi penugasan toko kedua.
	 * Itu bukan kehilangan data melainkan penggabungan, dan penggabungan itulah yang benar bila
	 * memang orangnya satu.</p>
	 */
	private static int imporSalesTenant(Session session, String sk, JSONObject r, Long tokoId,
			String oleh) throws Exception {
		String kode = s(r, "kode");
		if (kode.isEmpty()) {
			throw new Exception("kode kosong");
		}
		Long id = satuId(session, SalesInventoryDbfImportTenant.cariKode(sk, "salesperson"), kode);
		boolean baru = id == null;
		int tersentuh = 0;
		if (baru) {
			id = sisipKembalikanId(session, SalesInventoryDbfImportTenant.sisipSales(sk),
					new Object[] { kode, s(r, "nama"), s(r, "no_perkiraan"), oleh });
			if (id == null) {
				throw new Exception("gagal menyisipkan salesperson " + kode);
			}
		} else {
			tersentuh += jalankan(session, SalesInventoryDbfImportTenant.isiSales(sk),
					new Object[] { s(r, "nama"), s(r, "no_perkiraan"), id });
		}
		if (tokoId != null) {
			tersentuh += jalankan(session, SalesInventoryDbfImportTenant.sisipPenugasan(sk),
					new Object[] { id, tokoId, oleh, id, tokoId });
		}
		return baru ? 1 : (tersentuh > 0 ? 2 : 0);
	}

	/**
	 * Produk, berikut saldo awal legacy bila diminta.
	 *
	 * <p>Saldo awalnya menjadi SATU baris {@code mutasi_stok} berjenis opname — model tenant
	 * tidak punya kolom stok, sehingga tidak ada tempat kedua yang bisa berselisih. Penjaganya
	 * {@code WHERE NOT EXISTS} pada {@code nomor_dokumen = 'MIGRASI-DBF'}: menjalankan ulang
	 * berkas yang sama tidak boleh melipatgandakan saldo pembukanya.</p>
	 *
	 * <p>Saldo awal butuh gudang, dan gudang itu ditentukan dari tokonya. Bila toko itu belum
	 * punya gudang, saldo awalnya DITOLAK alih-alih ditaruh di gudang mana saja: menaruh stok
	 * pada gudang yang salah lebih buruk daripada tidak menaruhnya, sebab angkanya lalu tampak
	 * benar di tempat yang keliru.</p>
	 */
	private static int imporProdukTenant(Session session, String sk, JSONObject r, Long tokoId,
			boolean opnameAwal, String oleh, java.util.Set<String> peringatan) throws Exception {
		String kode = s(r, "kode");
		if (kode.isEmpty()) {
			throw new Exception("kode kosong");
		}
		Long satuanId = null;
		String namaSatuan = s(r, "satuan");
		if (!namaSatuan.isEmpty()) {
			satuanId = satuId(session, SalesInventoryDbfImportTenant.cariSatuan(sk), namaSatuan);
			if (satuanId == null) {
				satuanId = sisipKembalikanId(session,
						SalesInventoryDbfImportTenant.sisipSatuan(sk),
						new Object[] { namaSatuan.toUpperCase(), namaSatuan, oleh });
			}
		}
		Double hargaBeli = d(r, "harga_beli");
		Double hargaJual = d(r, "harga_jual");
		Double stokMin = d(r, "stok_minimum");
		Long id = satuId(session, SalesInventoryDbfImportTenant.cariKode(sk, "produk"), kode);
		boolean baru = id == null;
		int tersentuh = 0;
		if (baru) {
			id = sisipKembalikanId(session, SalesInventoryDbfImportTenant.sisipProduk(sk),
					new Object[] { kode, s(r, "nama"), satuanId, hargaJual, hargaBeli, stokMin,
							oleh });
			if (id == null) {
				throw new Exception("gagal menyisipkan produk " + kode);
			}
		} else {
			tersentuh += jalankan(session, SalesInventoryDbfImportTenant.isiProduk(sk),
					new Object[] { s(r, "nama"), satuanId, hargaJual, hargaBeli, stokMin, id });
		}
		Double stokLegacy = d(r, "stok_legacy");
		if (baru && opnameAwal && stokLegacy != null && stokLegacy.doubleValue() > 0) {
			Long gudangId = tokoId == null ? null
					: satuId(session, SalesInventoryDbfImportTenant.gudangToko(sk), tokoId);
			if (gudangId == null) {
				throw new Exception("saldo awal produk " + kode + " tidak dapat ditempatkan:"
						+ " toko ini belum punya gudang aktif");
			}
			tersentuh += jalankan(session, SalesInventoryDbfImportTenant.sisipSaldoAwal(sk),
					new Object[] { id, gudangId,
							new java.math.BigDecimal(String.valueOf(stokLegacy)), oleh, id });
		} else if (stokLegacy != null && stokLegacy.doubleValue() < 0) {
			lewati(peringatan, r, "stok_legacy");
		}
		return baru ? 1 : (tersentuh > 0 ? 2 : 0);
	}

	/**
	 * Riwayat BELI.DBF/JUAL.DBF sebagai satu baris {@code mutasi_stok}.
	 *
	 * <p>Bukan dokumen — lihat alasannya pada
	 * {@link SalesInventoryDbfImportTenant#sisipMutasiRiwayat}. Ringkasnya: baris DBF-nya tidak
	 * membawa supplier/customer sebagai relasi, hanya teks kode, dan tidak membawa termin, jatuh
	 * tempo, maupun status. Dokumen yang dibentuk dari situ akan punya kepala berisi tebakan lalu
	 * ikut masuk ke umur hutang dan piutang seolah tagihan sungguhan.</p>
	 *
	 * <p>Kunci idempotensinya menirukan kunci sintetis jalur legacy, sehingga berkas yang sama
	 * boleh diimpor ulang tanpa menggandakan pergerakan stok. Bedanya, di sini penjaganya indeks
	 * unik — bukan pembacaan lebih dulu — sehingga dua permintaan serentak pun tetap
	 * menghasilkan satu baris.</p>
	 */
	private static int imporRiwayatTenant(Session session, String sk, JSONObject r,
			boolean pembelian, Long tokoId, String oleh) throws Exception {
		String faktur = s(r, "nomor_faktur");
		String kodeProduk = s(r, "kode_produk");
		java.util.Date tanggal = tgl(r, "tanggal");
		Double qty = d(r, "qty");
		Double harga = d(r, pembelian ? "harga_beli" : "harga_jual");
		if (faktur.isEmpty() || kodeProduk.isEmpty() || tanggal == null || qty == null
				|| harga == null) {
			throw new Exception("nomor_faktur/kode_produk/tanggal/qty/"
					+ (pembelian ? "harga_beli" : "harga_jual") + " tidak lengkap");
		}
		if (qty.doubleValue() <= 0) {
			throw new Exception("qty harus lebih dari 0");
		}
		Long produkId = satuId(session, SalesInventoryDbfImportTenant.cariKode(sk, "produk"),
				kodeProduk);
		if (produkId == null) {
			throw new Exception("produk " + kodeProduk + " belum ada (impor STOK.DBF dulu)");
		}
		Long gudangId = tokoId == null ? null
				: satuId(session, SalesInventoryDbfImportTenant.gudangToko(sk), tokoId);
		if (gudangId == null) {
			// Sama seperti saldo awal: menaruh pergerakan pada gudang yang salah lebih buruk
			// daripada tidak menaruhnya, sebab angkanya lalu tampak benar di tempat yang keliru.
			throw new Exception("riwayat " + faktur + " tidak dapat ditempatkan: toko ini belum"
					+ " punya gudang aktif");
		}
		String kunci = potong((pembelian ? "LEGACY-BELI-" : "LEGACY-JUAL-") + faktur + "-"
				+ kodeProduk + "-" + s(r, "nomor_batch") + "-"
				+ new java.text.SimpleDateFormat("yyyyMMdd").format(tanggal), 128);
		java.sql.PreparedStatement cek = session.connection().prepareStatement(
				SalesInventoryDbfImportTenant.adaMutasiRiwayat(sk));
		boolean sudah;
		try {
			cek.setString(1, kunci);
			java.sql.ResultSet rs = cek.executeQuery();
			sudah = rs.next();
			rs.close();
		} finally {
			cek.close();
		}
		if (sudah) {
			return 0;
		}
		java.math.BigDecimal kuantitas = new java.math.BigDecimal(String.valueOf(qty));
		java.math.BigDecimal hargaSatuan = new java.math.BigDecimal(String.valueOf(harga));
		String keterangan = pembelian
				? ("Migrasi BELI.DBF; supplier=" + s(r, "kode_supplier") + "; batch="
						+ s(r, "nomor_batch") + "; ED=" + s(r, "tanggal_expired"))
				: ("Migrasi JUAL.DBF; customer=" + s(r, "kode_customer") + "; sales="
						+ s(r, "kode_sales") + "; batch=" + s(r, "nomor_batch"));
		jalankan(session, SalesInventoryDbfImportTenant.sisipMutasiRiwayat(sk, pembelian),
				new Object[] { produkId, gudangId, new java.sql.Date(tanggal.getTime()),
						kuantitas, hargaSatuan, kuantitas.multiply(hargaSatuan), faktur,
						keterangan, kunci, oleh });
		return 1;
	}

	/** Harga beli per supplier; idempoten pada (supplier, produk, berlaku_dari). */
	private static int imporHargaBeliTenant(Session session, String sk, JSONObject r, String oleh)
			throws Exception {
		String kodeSupplier = s(r, "kode_supplier");
		String kodeProduk = s(r, "kode_produk");
		java.util.Date tanggal = tgl(r, "tanggal");
		Double harga = d(r, "harga");
		if (kodeSupplier.isEmpty() || kodeProduk.isEmpty() || tanggal == null || harga == null) {
			throw new Exception("kode_supplier/kode_produk/tanggal/harga tidak lengkap");
		}
		Long supplierId = satuId(session,
				SalesInventoryDbfImportTenant.cariKode(sk, "supplier"), kodeSupplier);
		if (supplierId == null) {
			throw new Exception("supplier " + kodeSupplier + " belum ada (impor SUPPLIER.DBF dulu)");
		}
		Long produkId = satuId(session, SalesInventoryDbfImportTenant.cariKode(sk, "produk"),
				kodeProduk);
		if (produkId == null) {
			throw new Exception("produk " + kodeProduk + " belum ada (impor STOK.DBF dulu)");
		}
		java.sql.Date tgl = new java.sql.Date(tanggal.getTime());
		int n = jalankan(session, SalesInventoryDbfImportTenant.sisipHargaBeli(sk),
				new Object[] { supplierId, produkId, tgl,
						new java.math.BigDecimal(String.valueOf(harga)), oleh,
						supplierId, produkId, tgl });
		return n > 0 ? 1 : 0;
	}

	/**
	 * Harga jual; {@code customer_id} boleh kosong dan itulah harga umum.
	 *
	 * <p>Penjaga duplikatnya memakai {@code IS NOT DISTINCT FROM}, bukan {@code =}: perbandingan
	 * biasa terhadap {@code NULL} selalu tidak-diketahui, sehingga penjaganya akan lolos setiap
	 * kali dan harga umum yang sama berlipat pada tiap impor ulang.</p>
	 */
	private static int imporHargaJualTenant(Session session, String sk, JSONObject r, String oleh)
			throws Exception {
		String kodeCustomer = s(r, "kode_customer");
		String kodeProduk = s(r, "kode_produk");
		java.util.Date tanggal = tgl(r, "tanggal");
		Double harga = d(r, "harga");
		if (kodeProduk.isEmpty() || tanggal == null || harga == null) {
			throw new Exception("kode_produk/tanggal/harga tidak lengkap");
		}
		Long customerId = null;
		if (!kodeCustomer.isEmpty()) {
			customerId = satuId(session, SalesInventoryDbfImportTenant.cariKode(sk, "customer"),
					kodeCustomer);
			if (customerId == null) {
				throw new Exception("customer " + kodeCustomer
						+ " belum ada (impor CUSTOMER.DBF dulu)");
			}
		}
		Long produkId = satuId(session, SalesInventoryDbfImportTenant.cariKode(sk, "produk"),
				kodeProduk);
		if (produkId == null) {
			throw new Exception("produk " + kodeProduk + " belum ada (impor STOK.DBF dulu)");
		}
		java.sql.Date tgl = new java.sql.Date(tanggal.getTime());
		int n = jalankan(session, SalesInventoryDbfImportTenant.sisipHargaJual(sk),
				new Object[] { customerId, produkId, tgl,
						new java.math.BigDecimal(String.valueOf(harga)), oleh,
						customerId, produkId, tgl });
		return n > 0 ? 1 : 0;
	}

	private static boolean isiBilaKosong(String nilaiSekarang, String nilaiBaru) {
		return (nilaiSekarang == null || nilaiSekarang.trim().isEmpty()) && !nilaiBaru.isEmpty();
	}
}
