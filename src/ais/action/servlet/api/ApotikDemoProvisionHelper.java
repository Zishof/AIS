package ais.action.servlet.api;

import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.ConstantValues;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sirs.ApotikItemProfile;
import ais.database.model.sirs.AntreanFarmasi;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Kadaluarsa;
import ais.database.model.sirs.JenisItemMedis;
import ais.database.model.sirs.KodeTransaksiMedis;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.Resep;
import ais.database.model.sirs.ResepDetail;
import ais.database.model.sirs.SatuanItem;

/**
 * <h3>Provisioning MINIMAL modul SIRS untuk UAT apotik (aksi {@code apotik_provision_demo}).</h3>
 *
 * <p>Latar: server eCampus akademik (mis. demo.ecampus.id) tidak mem-provisioning modul SIRS,
 * sehingga {@code sirs.kode_transaksi_medis} kosong ({@code ConstantValues.apotikJual == null}) dan
 * {@code sirs.item_medis} kosong -- seluruh transaksi apotik fail-closed dengan benar, tapi jalur-
 * positif (jual, tolak-kedaluwarsa, FEFO, narkotika) tak bisa diuji. Aksi ini menyiapkan data uji
 * MINIMAL supaya UAT bisa jalan.</p>
 *
 * <h3>Pengaman (agar TIDAK pernah jalan tak sengaja di server rumah sakit nyata)</h3>
 * <ul>
	 *   <li>WAJIB konfigurasi {@code data_sample_ebisnis == aktif}; tidak ada konfigurasi berarti NONAKTIF;</li>
 *   <li>WAJIB admin sistem ({@code pedagang == null});</li>
 *   <li>WAJIB token konfirmasi eksplisit {@code konfirmasi == "SEED-DEMO-APOTIK"};</li>
 *   <li>Pembuatan data uji (item/resep) HANYA bila {@code sirs.item_medis} masih KOSONG --
 *       server ber-SIRS nyata (sudah ada item) TIDAK akan disentuh datanya.</li>
 * </ul>
 *
 * <p>Idempoten: kode transaksi &amp; master di-<i>ensure</i> by kode/nama; dipanggil dua kali tidak
 * menggandakan. Tanda ({@code jenis}) kode transaksi mengikuti semantik stok yang tidak ambigu
 * (jual/keluar = -1, beli/masuk = +1) -- rumus stok {@code SUM((qty+qty_bonus)*jenis)}.</p>
 */
public final class ApotikDemoProvisionHelper {

	/** Volume deterministik khusus server demo. Semua nama diberi penanda SAMPLE/UAT
	 * agar tidak disalahartikan sebagai formularium klinis atau daftar produk berizin. */
	private static final int JUMLAH_OBAT_JADI_DEMO = 10000;
	private static final int JUMLAH_BAHAN_RACIKAN_DEMO = 1000;
	private static final int JUMLAH_RESEP_SIAP_DEMO = 500;
	private static final int JUMLAH_ANTREAN_DEMO = 100;
	private static final Object LOCK_PROVISION = new Object();
	private static volatile boolean provisionBerjalan = false;
	private static volatile String provisionTerakhir = "Belum dijalankan";

	private ApotikDemoProvisionHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	/** Ensure satu KodeTransaksiMedis by kode; buat dgn nama+jenis bila belum ada. Set juga
	 *  ConstantValues LIVE supaya tidak perlu restart server. */
	private static KodeTransaksiMedis ensureKode(Session session, String kode, String nama, int jenis) {
		KodeTransaksiMedis k = (KodeTransaksiMedis) session.createCriteria(KodeTransaksiMedis.class)
				.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
		if (k == null) {
			k = new KodeTransaksiMedis();
			k.setKode(kode);
			k.setNama(nama);
			k.setJenis(Integer.valueOf(jenis));
			session.save(k);
		}
		return k;
	}

	public static void provisionDemo(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!Common.bolehKonfigurasi(Konfigurasi.DATA_SAMPLE_EBISNIS, Konfigurasi.TIDAK_AKTIF)) {
			tolak(hasil, "Provisioning data sample dinonaktifkan. Aktifkan konfigurasi data_sample_ebisnis terlebih dahulu.");
			return;
		}
		if (tbmuser == null || tbmuser.getPedagang() != null) {
			tolak(hasil, "Hanya admin sistem yang boleh menjalankan provisioning demo apotik.");
			return;
		}
		String konfirmasi = request == null ? "" : request.optString("konfirmasi", "").trim();
		if (!"SEED-DEMO-APOTIK".equals(konfirmasi)) {
			tolak(hasil, "Konfirmasi wajib: kirim {\"konfirmasi\":\"SEED-DEMO-APOTIK\"}. "
					+ "Aksi ini HANYA untuk server demo/UAT tanpa modul SIRS -- jangan dipakai di server rumah sakit nyata.");
			return;
		}
		if (request != null && request.optBoolean("status_only", false)) {
			hasil.put("status", "00");
			hasil.put("berjalan", provisionBerjalan);
			hasil.put("ringkasan", provisionTerakhir);
			hasil.put("verifikasi", verifikasiVolumeDemo());
			return;
		}
		// Default dijalankan sebagai daemon agar request maupun bootstrap Tomcat tidak
		// menunggu puluhan ribu INSERT. background=false tetap tersedia untuk UAT
		// terkontrol/command line yang memang ingin menunggu hasil akhir.
		if (request == null || request.optBoolean("background", true)) {
			synchronized (LOCK_PROVISION) {
				if (provisionBerjalan) {
					hasil.put("status", "01");
					hasil.put("description", "Provisioning demo sedang berjalan di latar.");
					hasil.put("ringkasan", provisionTerakhir);
					return;
				}
				provisionBerjalan = true;
				provisionTerakhir = "Dimulai " + new Date();
			}
			final Tbmuser userRef = tbmuser;
			final JSONObject requestLatar = new JSONObject(request == null ? "{}" : request.toString());
			requestLatar.put("background", false);
			Thread pekerja = new Thread(new Runnable() {
				@Override
				public void run() {
					JSONObject hasilLatar = new JSONObject();
					try {
						provisionDemo(userRef, requestLatar, hasilLatar);
						provisionTerakhir = hasilLatar.toString();
					} catch (Throwable e) {
						provisionTerakhir = "GAGAL: " + e.getMessage();
						e.printStackTrace();
						try {
							ais.common.ErrorAuditUtil.record(e, "apotik-provision-demo-background");
						} catch (Throwable ignored) {
							// Logging tidak boleh membuat status job tertahan selamanya.
						}
					} finally {
						provisionBerjalan = false;
					}
				}
			}, "AIS-Demo-Apotik-Seed");
			pekerja.setDaemon(true);
			pekerja.start();
			// Status 00 = permintaan DITERIMA. Flag `berjalan` membedakan job latar;
			// klien Api_eBisnis menganggap status selain 00 sebagai kegagalan.
			hasil.put("status", "00");
			hasil.put("description", "Provisioning 10.000 obat jadi, 1.000 bahan racikan, 500 resep siap jual, 100 antrean, dan tenaga medis dimulai di latar.");
			hasil.put("berjalan", true);
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			JSONObject ringkas = new JSONObject();

			// 1) Kode transaksi + ConstantValues live (jenis: masuk +1 / keluar -1).
			KodeTransaksiMedis aj = ensureKode(session, "AJ", "Apotik Jual", -1);
			KodeTransaksiMedis bm = ensureKode(session, "BM", "Beli Masuk", 1);
			KodeTransaksiMedis adt = ensureKode(session, "ADT", "Adjustment Penambahan", 1);
			KodeTransaksiMedis adk = ensureKode(session, "ADK", "Adjustment Pengurangan", -1);
			KodeTransaksiMedis ar = ensureKode(session, "AR", "Apotik Retur", 1);
			KodeTransaksiMedis br = ensureKode(session, "BR", "Beli Retur", -1);
			ensureKode(session, "RAC", "Jasa Racik", 1);
			ConstantValues.apotikJual = aj;
			ConstantValues.beliMasuk = bm;
			ConstantValues.adjustmentPenambahan = adt;
			ConstantValues.adjustmentPengurangan = adk;
			ConstantValues.apotikRetur = ar;
			ConstantValues.beliRetur = br;
			ringkas.put("kodeTransaksi", "AJ/BM/ADT/ADK/AR/BR/RAC dipastikan + ConstantValues di-set live");

			// 2) Data uji item/resep -- HANYA bila tabel kosong ATAU penanda katalog demo
			// sudah ada. Kondisi kedua memungkinkan katalog demo lama yang baru berisi
			// UJI-PCT/UJI-CDN dilengkapi sampai volume UAT, tanpa menyentuh server nyata.
			long jumlahItem = ((Number) session.createQuery("select count(i) from ItemMedis i")
					.uniqueResult()).longValue();
			long penandaDemo = ((Number) session.createQuery("select count(i) from ItemMedis i "
					+ "where i.kode = :kodeUji or i.kode like :kodeDemo")
					.setString("kodeUji", "UJI-PCT").setString("kodeDemo", "DEMO-OBT-%")
					.uniqueResult()).longValue();
			Map<String, Tbmrole> roleDemo = ApotikEmedikSeedHelper.pastikanRoleDemo(session);
			seedAkunOperasionalDemo(session, roleDemo, ringkas);
			seedTenagaMedis(session, ringkas);
			if (jumlahItem > 0 && penandaDemo == 0) {
				ringkas.put("dataUji", "DILEWATI -- sirs.item_medis sudah berisi " + jumlahItem
						+ " item (server ber-SIRS nyata, tidak disentuh)");
				tx.commit();
				hasil.put("status", "00");
				hasil.put("ringkasan", ringkas);
				return;
			}
			// Master pendukung (satuan + jenis item), idempoten by nama/kode.
			SatuanItem satuan = (SatuanItem) session.createCriteria(SatuanItem.class)
					.add(Restrictions.eq("nama", "Tablet").ignoreCase()).setMaxResults(1).uniqueResult();
			if (satuan == null) {
				satuan = new SatuanItem();
				satuan.setNama("Tablet");
				satuan.setNamaAwal("Tab");
				satuan.setJumlah(Integer.valueOf(1));
				session.save(satuan);
			}
			JenisItemMedis jenis = (JenisItemMedis) session.createCriteria(JenisItemMedis.class)
					.add(Restrictions.eq("kode", "OBT").ignoreCase()).setMaxResults(1).uniqueResult();
			if (jenis == null) {
				jenis = new JenisItemMedis();
				jenis.setKode("OBT");
				jenis.setNama("Obat");
				session.save(jenis);
			}
			SatuanItem satuanGram = (SatuanItem) session.createCriteria(SatuanItem.class)
					.add(Restrictions.eq("nama", "Gram").ignoreCase()).setMaxResults(1).uniqueResult();
			if (satuanGram == null) {
				satuanGram = new SatuanItem();
				satuanGram.setNama("Gram");
				satuanGram.setNamaAwal("g");
				satuanGram.setJumlah(Integer.valueOf(1));
				session.save(satuanGram);
			}
			JenisItemMedis jenisBahan = (JenisItemMedis) session.createCriteria(JenisItemMedis.class)
					.add(Restrictions.eq("kode", "BRC").ignoreCase()).setMaxResults(1).uniqueResult();
			if (jenisBahan == null) {
				jenisBahan = new JenisItemMedis();
				jenisBahan.setKode("BRC");
				jenisBahan.setNama("Bahan Baku Racikan");
				session.save(jenisBahan);
			}

			Map<String, ItemMedis> itemDemo = muatItemDemo(session);
			Map<Long, ApotikItemProfile> profilDemo = muatProfilDemo(session);
			Set<Long> stokDemo = muatIdItem(session,
					"select d.item.id from DetailTransaksiPasien d where d.keterangan like :penanda",
					"STOK-DEMO-%");
			Set<Long> batchDemo = muatIdItem(session,
					"select k.item.id from Kadaluarsa k where k.keterangan like :penanda",
					"BATCH-DEMO-%");

			// Dua ItemMedis uji: satu LASA (bebas), satu terkendali (narkotika).
			ItemMedis obatA = buatItemDemo(session, itemDemo, "UJI-PCT",
					"Paracetamol 500mg (SAMPLE/UAT)", satuan, jenis, 3000, 1500,
					"Paracetamol 500 mg; data sample, bukan acuan terapi");
			ItemMedis obatB = buatItemDemo(session, itemDemo, "UJI-CDN",
					"Codein 10mg (SAMPLE/UAT, Narkotika)", satuan, jenis, 8000, 4000,
					"Codeine 10 mg; data sample terkendali, bukan acuan terapi");
			ensureBatchDemo(session, obatA, 250, 720, stokDemo, batchDemo);
			ensureBatchDemo(session, obatB, 120, 540, stokDemo, batchDemo);

			// Profil apotik: A = LASA/bebas, B = narkotika (agar apotik_bayar wajib register).
			ensureProfilDemo(session, profilDemo, obatA, ApotikItemProfile.GOLONGAN_BEBAS, true);
			ensureProfilDemo(session, profilDemo, obatB, ApotikItemProfile.GOLONGAN_NARKOTIKA, false);

			// Katalog besar dibuat deterministik dan idempoten. Nama, dosis, harga,
			// barcode, golongan, produsen, dan negara bervariasi sehingga dashboard demo
			// representatif tanpa menyimpan 10.000 literal di source code. Seluruh nama
			// tetap eksplisit SAMPLE/UAT, bukan klaim produk terdaftar di negara tersebut.
			String[] zat = { "Amoxicillin", "Paracetamol", "Ibuprofen", "Cetirizine",
					"Metformin", "Amlodipine", "Omeprazole", "Azithromycin", "Salbutamol",
					"Vitamin B Kompleks", "Asam Mefenamat", "Captopril", "Domperidone",
					"Dexamethasone", "Ambroxol", "Cefixime", "Loratadine", "Simvastatin",
					"Furosemide", "Clopidogrel" };
			String[] bentuk = { "Tablet", "Kaplet", "Kapsul", "Sirup", "Drops", "Salep" };
			String[] produsen = { "Nusantara Pharma", "GlobalCare Labs", "Medika Sehat",
					"Sakura Health", "Alpine Remedies", "Andes Biomed", "Pacific Therapeutics",
					"Sahara Life Sciences", "Nordic Medica", "Meridian Farma" };
			String[] negara = { "Indonesia", "Malaysia", "Jepang", "India", "Jerman",
					"Brasil", "Kanada", "Australia", "Afrika Selatan", "Turki" };
			for (int i = 3; i <= JUMLAH_OBAT_JADI_DEMO; i++) {
				String kode = "DEMO-OBT-" + pad(i, 5);
				String nama = zat[(i - 3) % zat.length] + " " + (50 + ((i * 25) % 950)) + "mg "
						+ bentuk[(i - 3) % bentuk.length] + " — "
						+ produsen[i % produsen.length] + " / " + negara[i % negara.length]
						+ " (SAMPLE " + pad(i, 5) + ")";
				double beli = 500 + ((i * 137) % 95000);
				double jual = Math.ceil((beli * (1.15 + ((i % 8) / 100.0))) / 100.0) * 100.0;
				ItemMedis item = buatItemDemo(session, itemDemo, kode, nama, satuan, jenis,
						jual, beli, zat[(i - 3) % zat.length] + "; DATA SAMPLE/UAT");
				item.setBarcode("89977" + pad(i, 8));
				item.setBatasMinimalStok(Integer.valueOf(10 + (i % 90)));
				session.saveOrUpdate(item);
				String golongan = (i % 250 == 0) ? ApotikItemProfile.GOLONGAN_NARKOTIKA
						: ApotikItemProfile.GOLONGAN_BEBAS;
				ensureProfilDemo(session, profilDemo, item, golongan, i % 37 == 0);
				ensureBatchDemo(session, item, 30 + (i % 170), 120 + (i % 900), stokDemo, batchDemo);
				if (i % 250 == 0) {
					session.flush();
				}
			}

			// Bahan baku racikan terpisah dari obat jadi, lengkap dgn stok dan batch.
			String[] bahan = { "Laktosa", "Amilum", "Magnesium Stearat", "Talkum Farmasi",
					"Avicel", "Mannitol", "Sukrosa", "Natrium Benzoat", "Aqua Purificata",
					"CMC-Na", "Gelatin", "Propilen Glikol", "Gliserin", "Etanol Farmasi",
					"Asam Sitrat", "Natrium Sitrat", "Mentol", "Zinc Oxide", "Kaolin", "Pektin" };
			List<ItemMedis> daftarBahan = new java.util.ArrayList<ItemMedis>();
			for (int i = 1; i <= JUMLAH_BAHAN_RACIKAN_DEMO; i++) {
				String kode = "DEMO-BHN-" + pad(i, 4);
				String nama = bahan[(i - 1) % bahan.length] + " Grade Farmasi "
						+ pad(i, 4) + " (BAHAN RACIKAN SAMPLE/UAT)";
				double beli = 100 + ((i * 83) % 25000);
				ItemMedis item = buatItemDemo(session, itemDemo, kode, nama, satuanGram,
						jenisBahan, beli * 1.2, beli,
						bahan[(i - 1) % bahan.length] + "; bahan racikan DATA SAMPLE/UAT");
				item.setBarcode("89988" + pad(i, 8));
				item.setBatasMinimalStok(Integer.valueOf(100 + (i % 400)));
				session.saveOrUpdate(item);
				ensureProfilDemo(session, profilDemo, item, ApotikItemProfile.GOLONGAN_BEBAS, false);
				ensureBatchDemo(session, item, 500 + (i % 500), 180 + (i % 1080), stokDemo, batchDemo);
				daftarBahan.add(item);
				if (i % 250 == 0) session.flush();
			}

			// Sedikitnya 500 resep yang belum ditebus. Tiap resep memiliki tiga bahan
			// sehingga layar racikan menampilkan komposisi nyata, bukan daftar kosong.
			Map<String, Resep> resepDemo = muatResepDemo(session);
			Resep resep = null;
			for (int i = 1; i <= JUMLAH_RESEP_SIAP_DEMO; i++) {
				String kodeRacikan = "RSP-DEMO-" + pad(i, 4);
				Resep racikan = resepDemo.get(kodeRacikan);
				if (racikan == null) {
					racikan = new Resep();
					racikan.setKode(kodeRacikan);
					racikan.setKeterangan("Resep siap jual SAMPLE/UAT " + pad(i, 4)
							+ " — puyer/kapsul, 3 komponen");
					racikan.setOleh("seed_demo");
					racikan.setOlehId("seed_demo");
					session.save(racikan);
					resepDemo.put(kodeRacikan, racikan);
					for (int d = 0; d < 3; d++) {
						ItemMedis komponen = daftarBahan.get((i * 7 + d * 113) % daftarBahan.size());
						ResepDetail detail = new ResepDetail();
						detail.setResep(racikan);
						detail.setItem(komponen);
						detail.setJumlah(Double.valueOf(0.1 + (((i + d) % 9) * 0.1)));
						detail.setKeterangan("Komponen " + (d + 1) + " DATA SAMPLE/UAT");
						detail.setTanggal(new Date());
						session.save(detail);
					}
				}
				if (resep == null) resep = racikan;
				if (i % 100 == 0) session.flush();
			}
			seedAntreanDemo(session, request, daftarBahan, ringkas);

			tx.commit();
			JSONArray items = new JSONArray();
			items.put(new JSONObject().put("itemId", obatA.getId()).put("kode", "UJI-PCT")
					.put("golongan", "BEBAS").put("lasa", true));
			items.put(new JSONObject().put("itemId", obatB.getId()).put("kode", "UJI-CDN")
					.put("golongan", "NARKOTIKA"));
			ringkas.put("dataUji", "dibuat");
			ringkas.put("jumlahObatJadiTarget", JUMLAH_OBAT_JADI_DEMO);
			ringkas.put("jumlahBahanRacikanTarget", JUMLAH_BAHAN_RACIKAN_DEMO);
			ringkas.put("jumlahResepSiapTarget", JUMLAH_RESEP_SIAP_DEMO);
			ringkas.put("verifikasi", verifikasiVolumeDemo());
			ringkas.put("items", items);
			ringkas.put("resepId", resep.getId());
			ringkas.put("catatan", "Seluruh master adalah DATA SAMPLE/UAT. Setiap obat jadi dan bahan racikan dilengkapi batch serta stok awal; bukan acuan terapi atau klaim registrasi merek.");
			hasil.put("status", "00");
			hasil.put("ringkasan", ringkas);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static String pad(int nilai, int panjang) {
		String hasil = String.valueOf(nilai);
		while (hasil.length() < panjang) {
			hasil = "0" + hasil;
		}
		return hasil;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, ItemMedis> muatItemDemo(Session session) {
		Map<String, ItemMedis> hasil = new HashMap<String, ItemMedis>();
		List<ItemMedis> daftar = session.createCriteria(ItemMedis.class)
				.add(Restrictions.disjunction()
						.add(Restrictions.eq("kode", "UJI-PCT"))
						.add(Restrictions.eq("kode", "UJI-CDN"))
						.add(Restrictions.like("kode", "DEMO-OBT-%"))
						.add(Restrictions.like("kode", "DEMO-BHN-%")))
				.list();
		for (ItemMedis item : daftar) hasil.put(item.getKode(), item);
		return hasil;
	}

	@SuppressWarnings("unchecked")
	private static Map<Long, ApotikItemProfile> muatProfilDemo(Session session) {
		Map<Long, ApotikItemProfile> hasil = new HashMap<Long, ApotikItemProfile>();
		List<ApotikItemProfile> daftar = session.createCriteria(ApotikItemProfile.class)
				.createAlias("item", "item")
				.add(Restrictions.disjunction()
						.add(Restrictions.eq("item.kode", "UJI-PCT"))
						.add(Restrictions.eq("item.kode", "UJI-CDN"))
						.add(Restrictions.like("item.kode", "DEMO-OBT-%"))
						.add(Restrictions.like("item.kode", "DEMO-BHN-%")))
				.list();
		for (ApotikItemProfile profil : daftar) hasil.put(profil.getItem().getId(), profil);
		return hasil;
	}

	@SuppressWarnings("unchecked")
	private static Set<Long> muatIdItem(Session session, String hql, String penanda) {
		Set<Long> hasil = new HashSet<Long>();
		List<Long> daftar = session.createQuery(hql).setString("penanda", penanda).list();
		hasil.addAll(daftar);
		return hasil;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Resep> muatResepDemo(Session session) {
		Map<String, Resep> hasil = new HashMap<String, Resep>();
		List<Resep> daftar = session.createCriteria(Resep.class)
				.add(Restrictions.like("kode", "RSP-DEMO-%")).list();
		for (Resep resep : daftar) hasil.put(resep.getKode(), resep);
		return hasil;
	}

	private static ItemMedis buatItemDemo(Session session, Map<String, ItemMedis> cache,
			String kode, String nama, SatuanItem satuan, JenisItemMedis jenis,
			double hargaJual, double hargaBeli, String kandungan) {
		ItemMedis item = cache.get(kode);
		if (item == null) {
			item = new ItemMedis();
			item.setKode(kode);
			cache.put(kode, item);
		}
		item.setNama(nama);
		item.setSatuanItem(satuan);
		item.setJenisItem(jenis);
		item.setDefaultHargaJual(Double.valueOf(hargaJual));
		item.setDefaultHargaBeli(Double.valueOf(hargaBeli));
		item.setKandungan(kandungan);
		item.setKeterangan("DATA SAMPLE/UAT APOTIK — bukan data formularium klinis");
		session.saveOrUpdate(item);
		return item;
	}

	private static void ensureProfilDemo(Session session,
			Map<Long, ApotikItemProfile> cache, ItemMedis item, String golongan, boolean lasa) {
		ApotikItemProfile profil = cache.get(item.getId());
		if (profil == null) {
			profil = new ApotikItemProfile();
			profil.setItem(item);
			cache.put(item.getId(), profil);
		}
		profil.setGolonganObat(golongan);
		profil.setLasa(Boolean.valueOf(lasa));
		profil.setKeterangan("DATA SAMPLE/UAT APOTIK");
		session.saveOrUpdate(profil);
	}

	private static void ensureBatchDemo(Session session, ItemMedis item, int qty,
			int hariKedaluwarsa, Set<Long> stokDemo, Set<Long> batchDemo) {
		if (!stokDemo.contains(item.getId())) {
			DetailTransaksiPasien ledger = new DetailTransaksiPasien();
			ledger.setKodeTransaksi(ConstantValues.beliMasuk);
			ledger.setItem(item);
			ledger.setQty(Double.valueOf(qty));
			ledger.setAmount(item.getDefaultHargaBeli() == null
					? Double.valueOf(0) : item.getDefaultHargaBeli());
			ledger.setHasilPenghitunganTotal(Double.valueOf(qty
					* (item.getDefaultHargaBeli() == null ? 0 : item.getDefaultHargaBeli().doubleValue())));
			ledger.setTanggal(new Date());
			ledger.setKeterangan("STOK-DEMO-" + item.getKode());
			ledger.setOlehId("seed_demo");
			ledger.setOleh("Provisioning data sample eBisnis");
			session.save(ledger);
			stokDemo.add(item.getId());
		}
		if (!batchDemo.contains(item.getId())) {
			Calendar kalender = Calendar.getInstance();
			kalender.add(Calendar.DAY_OF_YEAR, hariKedaluwarsa);
			Kadaluarsa batch = new Kadaluarsa();
			batch.setItem(item);
			batch.setQty(Double.valueOf(qty));
			batch.setTanggalKadaluarsa(kalender.getTime());
			batch.setKeterangan("BATCH-DEMO-" + item.getKode());
			batch.setOlehId("seed_demo");
			batch.setOleh("Provisioning data sample eBisnis");
			session.save(batch);
			batchDemo.add(item.getId());
		}
	}

	@SuppressWarnings("unchecked")
	private static void seedAntreanDemo(Session session, JSONObject request,
			List<ItemMedis> daftarBahan, JSONObject ringkas) throws Exception {
		Long tokoId = Long.valueOf(request == null ? 1 : request.optLong("toko_id", 1));
		Calendar awal = Calendar.getInstance();
		awal.set(Calendar.HOUR_OF_DAY, 0);
		awal.set(Calendar.MINUTE, 0);
		awal.set(Calendar.SECOND, 0);
		awal.set(Calendar.MILLISECOND, 0);
		List<AntreanFarmasi> hariIni = session.createCriteria(AntreanFarmasi.class)
				.add(Restrictions.eq("tokoId", tokoId))
				.add(Restrictions.ge("tanggalDibuat", awal.getTime())).list();
		Set<String> kodeAda = new HashSet<String>();
		for (AntreanFarmasi antrean : hariIni) kodeAda.add(antrean.getKodeAntrean());
		int dibuat = 0;
		for (int i = 1; i <= JUMLAH_ANTREAN_DEMO; i++) {
			String kode = "UAT" + pad(i, 3);
			if (kodeAda.contains(kode)) continue;
			AntreanFarmasi antrean = new AntreanFarmasi();
			antrean.setTokoId(tokoId);
			antrean.setKodeAntrean(kode);
			antrean.setNomorRekamMedis("RM-SAMPLE-" + pad(i, 6));
			antrean.setNamaPasien("Pasien Sample UAT " + pad(i, 3));
			antrean.setJenis(i % 3 == 0 ? AntreanFarmasi.JENIS_CAMPURAN
					: (i % 2 == 0 ? AntreanFarmasi.JENIS_RACIKAN : AntreanFarmasi.JENIS_JADI));
			antrean.setStatus(i <= 12 ? AntreanFarmasi.STATUS_SIAP
					: (i <= 40 ? AntreanFarmasi.STATUS_DISIAPKAN : AntreanFarmasi.STATUS_MENUNGGU));
			antrean.setLoket("Loket " + (1 + (i % 8)));
			antrean.setUrutan(Integer.valueOf(i));
			antrean.setTanggalDibuat(new Date());
			JSONArray obat = new JSONArray();
			obat.put(new JSONObject().put("nama", daftarBahan.get(i % daftarBahan.size()).getNama())
					.put("jumlah", (1 + (i % 3)) + " bungkus"));
			antrean.setDaftarObat(obat.toString());
			antrean.setCatatanPublik(i <= 12 ? "Obat siap diambil di loket."
					: "Obat sedang diproses oleh Instalasi Farmasi.");
			antrean.setOleh("Provisioning DATA SAMPLE/UAT");
			antrean.setOlehId("seed_demo");
			session.save(antrean);
			dibuat++;
		}
		ringkas.put("antreanFarmasi", JUMLAH_ANTREAN_DEMO
				+ " antrean hari ini dipastikan untuk toko " + tokoId + " (baru " + dibuat + ")");
	}

	private static JSONObject verifikasiVolumeDemo() throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONObject hasil = new JSONObject();
			long obatJadi = ((Number) session.createQuery("select count(i) from ItemMedis i "
					+ "where i.kode = 'UJI-PCT' or i.kode = 'UJI-CDN' or i.kode like 'DEMO-OBT-%'")
					.uniqueResult()).longValue();
			long bahan = ((Number) session.createQuery("select count(i) from ItemMedis i "
					+ "where i.kode like 'DEMO-BHN-%'").uniqueResult()).longValue();
			long resepSiap = ((Number) session.createQuery("select count(r) from Resep r "
					+ "where r.kode like 'RSP-DEMO-%' and not exists "
					+ "(select tm.id from TransaksiMedis tm where tm.resep = r)")
					.uniqueResult()).longValue();
			hasil.put("obatJadi", obatJadi);
			hasil.put("bahanRacikan", bahan);
			hasil.put("resepSiapJual", resepSiap);
			hasil.put("targetObatJadi", JUMLAH_OBAT_JADI_DEMO);
			hasil.put("targetBahanRacikan", JUMLAH_BAHAN_RACIKAN_DEMO);
			hasil.put("targetResepSiapJual", JUMLAH_RESEP_SIAP_DEMO);
			hasil.put("lulus", obatJadi >= JUMLAH_OBAT_JADI_DEMO
					&& bahan >= JUMLAH_BAHAN_RACIKAN_DEMO
					&& resepSiap >= JUMLAH_RESEP_SIAP_DEMO);
			return hasil;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static void ensureBatchDemo(Session session, ItemMedis item, int qty,
			int hariKedaluwarsa) {
		ensureStokDemo(session, item, qty);
		Kadaluarsa batch = (Kadaluarsa) session.createCriteria(Kadaluarsa.class)
				.add(Restrictions.eq("item", item))
				.add(Restrictions.eq("keterangan", "BATCH-DEMO-" + item.getKode()))
				.setMaxResults(1).uniqueResult();
		if (batch != null) return;
		Calendar kalender = Calendar.getInstance();
		kalender.add(Calendar.DAY_OF_YEAR, hariKedaluwarsa);
		batch = new Kadaluarsa();
		batch.setItem(item);
		batch.setQty(Double.valueOf(qty));
		batch.setTanggalKadaluarsa(kalender.getTime());
		batch.setKeterangan("BATCH-DEMO-" + item.getKode());
		batch.setOlehId("seed_demo");
		batch.setOleh("Provisioning data sample eBisnis");
		session.save(batch);
	}

	private static void ensureStokDemo(Session session, ItemMedis item, int qty) {
		String penanda = "STOK-DEMO-" + item.getKode();
		long ada = ((Number) session.createQuery(
				"select count(d) from DetailTransaksiPasien d where d.item = :item and d.keterangan = :penanda")
				.setParameter("item", item).setString("penanda", penanda)
				.uniqueResult()).longValue();
		if (ada > 0) return;
		DetailTransaksiPasien ledger = new DetailTransaksiPasien();
		ledger.setKodeTransaksi(ConstantValues.beliMasuk);
		ledger.setItem(item);
		ledger.setQty(Double.valueOf(qty));
		ledger.setAmount(item.getDefaultHargaBeli() == null
				? Double.valueOf(0) : item.getDefaultHargaBeli());
		ledger.setHasilPenghitunganTotal(Double.valueOf(qty
				* (item.getDefaultHargaBeli() == null ? 0 : item.getDefaultHargaBeli().doubleValue())));
		ledger.setTanggal(new Date());
		ledger.setKeterangan(penanda);
		ledger.setOleh("seed_demo");
		ledger.setOlehId("seed_demo");
		session.save(ledger);
	}

	@SuppressWarnings("unchecked")
	private static int ensureBatchKatalogDemo(Session session) {
		java.util.List<ItemMedis> items = session.createQuery(
				"from ItemMedis i where i.kode = :ujiA or i.kode = :ujiB or i.kode like :demo")
				.setString("ujiA", "UJI-PCT").setString("ujiB", "UJI-CDN")
				.setString("demo", "DEMO-OBT-%").list();
		int sebelum = ((Number) session.createQuery(
				"select count(k) from Kadaluarsa k where k.keterangan like :demo")
				.setString("demo", "BATCH-DEMO-%").uniqueResult()).intValue();
		int nomor = 0;
		for (ItemMedis item : items) {
			nomor++;
			ensureBatchDemo(session, item, 30 + (nomor % 170), 120 + (nomor % 900));
			if (nomor % 250 == 0) session.flush();
		}
		int sesudah = ((Number) session.createQuery(
				"select count(k) from Kadaluarsa k where k.keterangan like :demo")
				.setString("demo", "BATCH-DEMO-%").uniqueResult()).intValue();
		return Math.max(0, sesudah - sebelum);
	}

	private static void seedTenagaMedis(Session session, JSONObject ringkas) throws Exception {
		Tbmrole roleMedis = (Tbmrole) session.get(Tbmrole.class, Tbmrole.DOKTER);
		if (roleMedis == null) {
			roleMedis = new Tbmrole();
			roleMedis.setRoleId(Tbmrole.DOKTER);
			roleMedis.setRoleName("Tenaga Medis");
			roleMedis.setAktif(Boolean.TRUE);
			roleMedis.setEmedic(Boolean.TRUE);
			roleMedis.setHalamanUtama(Tbmrole.HALAMAN_UTAMA_EMEDIK);
			roleMedis.setEbisnisMenu(ApotikEmedikSeedHelper.menuRoleTenagaMedisDemo());
			session.save(roleMedis);
		} else {
			roleMedis.setAktif(Boolean.TRUE);
			roleMedis.setEmedic(Boolean.TRUE);
			if (roleMedis.getHalamanUtama() == null || roleMedis.getHalamanUtama().trim().isEmpty()) {
				roleMedis.setHalamanUtama(Tbmrole.HALAMAN_UTAMA_EMEDIK);
			}
			if (roleMedis.getEbisnisMenu() == null || roleMedis.getEbisnisMenu().trim().isEmpty()) {
				roleMedis.setEbisnisMenu(ApotikEmedikSeedHelper.menuRoleTenagaMedisDemo());
			}
			session.saveOrUpdate(roleMedis);
		}
		int dibuat = 0;
		dibuat += buatTenagaMedis(session, roleMedis, "DOK", "Dokter", Dokter.UMUM, 100);
		dibuat += buatTenagaMedis(session, roleMedis, "BDN", "Bidan", Dokter.BIDAN, 100);
		dibuat += buatTenagaMedis(session, roleMedis, "PRW", "Perawat", Dokter.PERAWAT, 500);
		dibuat += buatTenagaMedis(session, roleMedis, "MED", "Tenaga Medis", Dokter.LAIN, 100);
		ringkas.put("tenagaMedis", "800 data dipastikan (baru " + dibuat
				+ "): 100 dokter, 100 bidan, 500 perawat, 100 tenaga medis lain");
		seedAliasTenagaMedis(session, roleMedis, ringkas);
	}

	private static void seedAkunOperasionalDemo(Session session, Map<String, Tbmrole> roleDemo,
			JSONObject ringkas) throws Exception {
		JSONArray akun = new JSONArray();
		buatAkunDemo(session, "demo_apoteker", "Apoteker Demo",
				roleDemo.get(ApotikEmedikSeedHelper.ROLE_APOTIK), null, null);
		akun.put(akunJson("demo_apoteker", "Apoteker", "Akses penuh apotik dan kasir eMedik"));
		buatAkunDemo(session, "demo_kasir_apotik", "Kasir Apotik Demo",
				roleDemo.get(ApotikEmedikSeedHelper.ROLE_APOTIK_KASIR_DEMO), null, null);
		akun.put(akunJson("demo_kasir_apotik", "Kasir Apotik", "Penjualan, resep, dan tagihan"));
		buatAkunDemo(session, "demo_gudang_apotik", "Gudang Apotik Demo",
				roleDemo.get(ApotikEmedikSeedHelper.ROLE_APOTIK_GUDANG_DEMO), null, null);
		akun.put(akunJson("demo_gudang_apotik", "Gudang Apotik",
				"Formularium, batch, pengadaan, opname, dan retur"));
		buatAkunDemo(session, "demo_pendaftaran", "Pendaftaran eMedik Demo",
				roleDemo.get(ApotikEmedikSeedHelper.ROLE_EMEDIK_PENDAFTARAN_DEMO), null, null);
		akun.put(akunJson("demo_pendaftaran", "Pendaftaran eMedik",
				"Pendaftaran, tagihan, deposit, dan penjamin"));
		ringkas.put("akunOperasional", akun);
	}

	private static void seedAliasTenagaMedis(Session session, Tbmrole roleMedis, JSONObject ringkas)
			throws Exception {
		JSONArray akun = ringkas.optJSONArray("akunOperasional");
		if (akun == null) akun = new JSONArray();
		buatAliasTenagaMedis(session, roleMedis, "demo_dokter", "DEMO-DOK-0001", "Dokter");
		akun.put(akunJson("demo_dokter", "Dokter", "Layanan eMedik; terhubung ke dokter demo"));
		buatAliasTenagaMedis(session, roleMedis, "demo_bidan", "DEMO-BDN-0001", "Bidan");
		akun.put(akunJson("demo_bidan", "Bidan", "Layanan eMedik; terhubung ke bidan demo"));
		buatAliasTenagaMedis(session, roleMedis, "demo_perawat", "DEMO-PRW-0001", "Perawat");
		akun.put(akunJson("demo_perawat", "Perawat", "Layanan eMedik; terhubung ke perawat demo"));
		ringkas.put("akunDemo", akun);
		ringkas.put("passwordAkunDemo", "Password setiap akun sama dengan username; khusus server demo/UAT");
	}

	private static void buatAliasTenagaMedis(Session session, Tbmrole role, String userId,
			String kode, String label) throws Exception {
		Pegawai pegawai = (Pegawai) session.createCriteria(Pegawai.class)
				.add(Restrictions.eq("code", kode)).setMaxResults(1).uniqueResult();
		Dokter dokter = (Dokter) session.createCriteria(Dokter.class)
				.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
		buatAkunDemo(session, userId, label + " Demo", role, pegawai, dokter);
	}

	private static void buatAkunDemo(Session session, String userId, String nama, Tbmrole role,
			Pegawai pegawai, Dokter dokter) throws Exception {
		Tbmuser user = (Tbmuser) session.get(Tbmuser.class, userId);
		if (user == null) {
			user = new Tbmuser();
			user.setUserId(userId);
		}
		user.setUserNama(nama);
		user.setUserPassword(Common.desEncrypter.get().encrypt(userId));
		user.setIs_encripted(Boolean.TRUE);
		user.setAktif(Boolean.TRUE);
		user.setRoot(Boolean.FALSE);
		user.setUserShow(Integer.valueOf(1));
		user.setUserRole(role);
		if (pegawai != null) user.setPegawai(pegawai);
		if (dokter != null) user.setDokter(dokter);
		session.saveOrUpdate(user);
	}

	private static JSONObject akunJson(String username, String peran, String akses) throws Exception {
		return new JSONObject().put("username", username).put("password", username)
				.put("peran", peran).put("akses", akses);
	}

	private static int buatTenagaMedis(Session session, Tbmrole roleMedis, String prefix,
			String label, String kategori, int jumlah) throws Exception {
		int dibuat = 0;
		for (int i = 1; i <= jumlah; i++) {
			String kode = "DEMO-" + prefix + "-" + pad(i, 4);
			String nama = label + " Demo " + pad(i, 4);
			Pegawai pegawai = (Pegawai) session.createCriteria(Pegawai.class)
					.add(Restrictions.eq("code", kode)).setMaxResults(1).uniqueResult();
			if (pegawai == null) {
				pegawai = new Pegawai();
				pegawai.setCode(kode);
				pegawai.setNama(nama);
				pegawai.setAktif(Boolean.TRUE);
				pegawai.setJenis("Tenaga Medis");
				session.save(pegawai);
				dibuat++;
			}
			Dokter dokter = (Dokter) session.createCriteria(Dokter.class)
					.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
			if (dokter == null) {
				dokter = new Dokter();
				dokter.setKode(kode);
				dokter.setNama(nama);
				dokter.setKategori(kategori);
				dokter.setAktif(Boolean.TRUE);
				dokter.setKeterangan("Data demo " + label + "; terhubung ke public.pegawai dan tbmuser");
				session.save(dokter);
			}
			String userId = ("demo_" + prefix + "_" + pad(i, 4)).toLowerCase();
			Tbmuser user = (Tbmuser) session.get(Tbmuser.class, userId);
			if (user == null) {
				user = new Tbmuser();
				user.setUserId(userId);
				user.setUserNama(nama);
				user.setUserPassword(Common.desEncrypter.get().encrypt(userId));
				user.setIs_encripted(Boolean.TRUE);
				user.setRoot(Boolean.FALSE);
				user.setUserShow(Integer.valueOf(1));
				user.setUserRole(roleMedis);
				user.setAktif(Boolean.TRUE);
				user.setPegawai(pegawai);
				user.setDokter(dokter);
				session.save(user);
			} else {
				user.setPegawai(pegawai);
				user.setDokter(dokter);
				session.saveOrUpdate(user);
			}
			if (i % 100 == 0) {
				session.flush();
			}
		}
		return dibuat;
	}

	private static ItemMedis buatItem(Session session, String kode, String nama, SatuanItem satuan,
			JenisItemMedis jenis, double hargaJual, double hargaBeli) {
		ItemMedis it = (ItemMedis) session.createCriteria(ItemMedis.class)
				.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
		if (it == null) {
			it = new ItemMedis();
			it.setKode(kode);
			it.setNama(nama);
			it.setSatuanItem(satuan);
			it.setJenisItem(jenis);
			it.setDefaultHargaJual(Double.valueOf(hargaJual));
			it.setDefaultHargaBeli(Double.valueOf(hargaBeli));
			session.save(it);
		}
		return it;
	}

	private static void ensureProfil(Session session, ItemMedis item, String golongan, boolean lasa) {
		ApotikItemProfile p = (ApotikItemProfile) session.createCriteria(ApotikItemProfile.class)
				.add(Restrictions.eq("item", item)).setMaxResults(1).uniqueResult();
		if (p == null) {
			p = new ApotikItemProfile();
			p.setItem(item);
		}
		p.setGolonganObat(golongan);
		p.setLasa(Boolean.valueOf(lasa));
		session.saveOrUpdate(p);
	}
}
