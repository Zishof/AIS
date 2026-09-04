package ais.action.servlet.api;

import java.util.Date;
import java.util.Calendar;
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
import ais.database.model.sirs.BahanBakuItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Kadaluarsa;
import ais.database.model.sirs.JenisItemMedis;
import ais.database.model.sirs.KodeTransaksiMedis;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.Resep;
import ais.database.model.sirs.ResepDetail;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.RacikanDetail;
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

	private static final int JUMLAH_OBAT_DEMO = 10000;
	private static final int JUMLAH_BAHAN_RACIKAN_DEMO = 1000;
	private static final int JUMLAH_RACIKAN_DEMO = 5000;
	private static final int JUMLAH_ANTREAN_DEMO = 100;
	private static final int JUMLAH_FORMULA_UAT = 250;
	private static final Object LOCK_PROVISION = new Object();
	private static volatile boolean provisionBerjalan = false;
	private static volatile boolean provisionPernahDijalankan = false;
	private static volatile boolean provisionSelesai = false;
	private static volatile boolean provisionBerhasil = false;
	private static volatile String provisionTerakhir = "Belum dijalankan";
	private static volatile String provisionTahap = "Belum dijalankan";

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
		if (tbmuser == null || !Common.getApakahAdminLain(tbmuser)) {
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
			hasil.put("pernahDijalankan", provisionPernahDijalankan);
			hasil.put("selesai", provisionSelesai);
			hasil.put("berhasil", provisionBerhasil);
			hasil.put("ringkasan", provisionTerakhir);
			hasil.put("tahap", provisionTahap);
			hasil.put("verifikasi", verifikasiVolumeDemo());
			return;
		}
		// Default dijalankan sebagai daemon agar request maupun bootstrap Tomcat tidak
		// menunggu puluhan ribu INSERT. background=false tetap tersedia untuk UAT
		// terkontrol/command line yang memang ingin menunggu hasil akhir.
		if (request == null || request.optBoolean("background", true)) {
			synchronized (LOCK_PROVISION) {
				if (provisionBerjalan) {
					// Idempoten: klik ulang hanya mengembalikan job yang sedang aktif,
					// tidak membuat worker kedua dan tidak dianggap gagal oleh klien.
					hasil.put("status", "00");
					hasil.put("description", "Provisioning demo sedang berjalan di latar.");
					hasil.put("berjalan", true);
					hasil.put("ringkasan", provisionTerakhir);
					return;
				}
				provisionBerjalan = true;
				provisionPernahDijalankan = true;
				provisionSelesai = false;
				provisionBerhasil = false;
				provisionTerakhir = "Dimulai " + new Date();
				provisionTahap = "Menyiapkan katalog obat dan racikan";
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
						provisionBerhasil = "00".equals(hasilLatar.optString("status", ""));
					} catch (Throwable e) {
						provisionTerakhir = "GAGAL: " + e.getMessage();
						provisionBerhasil = false;
						e.printStackTrace();
						try {
							ais.common.ErrorAuditUtil.record(e, "apotik-provision-demo-background");
						} catch (Throwable ignored) {
							// Logging tidak boleh membuat status job tertahan selamanya.
						}
					} finally {
						if (!provisionBerhasil && provisionTahap.indexOf("Gagal") < 0) {
							provisionTahap = "Gagal: " + provisionTerakhir;
						}
						provisionSelesai = true;
						provisionBerjalan = false;
					}
				}
			}, "AIS-Demo-Apotik-Seed");
			pekerja.setDaemon(true);
			pekerja.start();
			// Status 00 = permintaan DITERIMA. Flag `berjalan` membedakan job latar;
			// klien Api_eBisnis menganggap status selain 00 sebagai kegagalan.
			hasil.put("status", "00");
			hasil.put("description", "Provisioning 10.000 obat jadi, 1.000 bahan racikan, 5.000 resep, "
					+ "100 antrean, dan tenaga medis dimulai di latar.");
			hasil.put("berjalan", true);
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			JSONObject ringkas = new JSONObject();
			provisionTahap = "Menyiapkan kode transaksi apotik";

			// 1) Kode transaksi + ConstantValues live (jenis: masuk +1 / keluar -1).
			KodeTransaksiMedis aj = ensureKode(session, "AJ", "Apotik Jual", -1);
			KodeTransaksiMedis bm = ensureKode(session, "BM", "Beli Masuk", 1);
			KodeTransaksiMedis adt = ensureKode(session, "ADT", "Adjustment Penambahan", 1);
			KodeTransaksiMedis adk = ensureKode(session, "ADK", "Adjustment Pengurangan", -1);
			KodeTransaksiMedis ar = ensureKode(session, "AR", "Apotik Retur", 1);
			KodeTransaksiMedis br = ensureKode(session, "BR", "Beli Retur", -1);
			KodeTransaksiMedis rac = ensureKode(session, "RAC", "Jasa Racik", 1);
			KodeTransaksiMedis prod = ensureKode(session, "PROD", "Produksi Farmasi", 1);
			KodeTransaksiMedis bb = ensureKode(session, "BB", "Bahan Baku Produksi", -1);
			ConstantValues.apotikJual = aj;
			ConstantValues.beliMasuk = bm;
			ConstantValues.adjustmentPenambahan = adt;
			ConstantValues.adjustmentPengurangan = adk;
			ConstantValues.apotikRetur = ar;
			ConstantValues.beliRetur = br;
			ConstantValues.jasaRacik = rac;
			ConstantValues.produksi = prod;
			ConstantValues.bahanBaku = bb;
			ringkas.put("kodeTransaksi", "AJ/BM/ADT/ADK/AR/BR/RAC/PROD/BB dipastikan + ConstantValues di-set live");

			// 2) Data uji item/resep -- hanya tabel kosong atau katalog bertanda demo.
			// Katalog demo parsial (mis. baru UJI-PCT/UJI-CDN) harus dapat dilengkapi
			// secara idempoten sampai volume UAT. Server SIRS nyata tanpa penanda demo
			// tetap tidak disentuh.
			long jumlahItem = ((Number) session.createQuery("select count(i) from ItemMedis i")
					.uniqueResult()).longValue();
			long penandaDemo = ((Number) session.createQuery("select count(i) from ItemMedis i "
					+ "where i.kode = :kodeUji or i.kode like :kodeDemo")
					.setString("kodeUji", "UJI-PCT").setString("kodeDemo", "DEMO-OBT-%")
					.uniqueResult()).longValue();
			if (jumlahItem > 0 && penandaDemo == 0) {
				ringkas.put("dataUji", "DILEWATI -- sirs.item_medis sudah berisi " + jumlahItem
						+ " item (server ber-SIRS nyata, tidak disentuh)");
				session.getTransaction().commit();
				seedPendukungDemoTerpisah(ringkas);
				provisionTahap = "Selesai";
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

			// Dua ItemMedis uji: satu LASA (bebas), satu terkendali (narkotika).
			ItemMedis obatA = buatItem(session, "UJI-PCT", "Paracetamol 500 mg Tablet", satuan, jenis, 3000, 1500);
			ItemMedis obatB = buatItem(session, "UJI-CDN", "Codeine 10 mg Tablet", satuan, jenis, 8000, 4000);
			ensureBatchDemo(session, obatA, 250, 720);
			ensureBatchDemo(session, obatB, 120, 540);

			// Profil apotik: A = LASA/bebas, B = narkotika (agar apotik_bayar wajib register).
			ensureProfil(session, obatA, ApotikItemProfile.GOLONGAN_BEBAS, true);
			ensureProfil(session, obatB, ApotikItemProfile.GOLONGAN_NARKOTIKA, false);

			// Katalog besar dibuat deterministik dan idempoten. Nama, dosis, harga,
			// barcode, golongan, serta penanda LASA bervariasi sehingga dashboard demo
			// langsung representatif tanpa menyimpan 10.000 literal di source code.
			boolean katalogDemoLengkap = jumlahItem >= JUMLAH_OBAT_DEMO
					&& penandaDemo >= JUMLAH_OBAT_DEMO - 2;
			provisionTahap = katalogDemoLengkap
					? "Katalog obat lengkap; melengkapi formula racikan dan produksi"
					: "Membuat 10.000 obat dan batch stok";
			for (int i = 3; !katalogDemoLengkap && i <= JUMLAH_OBAT_DEMO; i++) {
				String kode = "DEMO-OBT-" + pad(i, 5);
				String nama = namaObatApotik(i);
				double beli = 500 + ((i * 137) % 95000);
				double jual = Math.ceil((beli * (1.15 + ((i % 8) / 100.0))) / 100.0) * 100.0;
				ItemMedis item = buatItem(session, kode, nama, satuan, jenis, jual, beli);
				item.setBarcode("89977" + pad(i, 8));
				item.setBatasMinimalStok(Integer.valueOf(10 + (i % 90)));
				session.saveOrUpdate(item);
				String golongan = (i % 250 == 0) ? ApotikItemProfile.GOLONGAN_NARKOTIKA
						: ApotikItemProfile.GOLONGAN_BEBAS;
				ensureProfil(session, item, golongan, i % 37 == 0);
				ensureBatchDemo(session, item, 30 + (i % 170), 120 + (i % 900));
				if (i % 250 == 0) {
					// KE-FIX (connection closed / c3p0 unreturnedConnectionTimeout): 10.000 item
					// dulu ditahan dalam SATU transaksi/koneksi dari awal sampai akhir -- pada
					// DB/latensi tertentu ini bisa melebihi 30 menit, sehingga c3p0 merebut paksa
					// koneksi yang dianggap bocor (lihat hibernate.cfg.xml unreturnedConnectionTimeout)
					// dan sisa loop gagal "This connection has been closed." Commit periodik
					// melepas & mengambil ulang koneksi tiap 250 item (batch aman krn idempoten
					// by kode), jauh di bawah ambang 30 menit. Entity yg dipakai lintas-iterasi
					// (satuan, jenis, obatA/obatB) TETAP managed karena session tidak di-clear.
					session.flush();
					session.getTransaction().commit();
					session.beginTransaction();
				}
			}

			int bahanDibuat = ensureBahanRacikanDemo(session);
			int racikanDibuat = ensureRacikanDemo(session, obatA, obatB);
			int formulaRacikanDibuat = ensureFormulaRacikanUat(session);
			int formulaProduksiDibuat = ensureFormulaProduksiUat(session);
			int antreanDibuat = ensureAntreanDemo(session, request);
			Resep resep = (Resep) session.createCriteria(Resep.class)
					.add(Restrictions.eq("kode", "RSP-UJI-1")).setMaxResults(1).uniqueResult();

			session.getTransaction().commit();
			seedPendukungDemoTerpisah(ringkas);
			JSONArray items = new JSONArray();
			items.put(new JSONObject().put("itemId", obatA.getId()).put("kode", "UJI-PCT")
					.put("golongan", "BEBAS").put("lasa", true));
			items.put(new JSONObject().put("itemId", obatB.getId()).put("kode", "UJI-CDN")
					.put("golongan", "NARKOTIKA"));
			ringkas.put("dataUji", "dibuat");
			ringkas.put("jumlahObat", JUMLAH_OBAT_DEMO);
			ringkas.put("jumlahBahanRacikan", JUMLAH_BAHAN_RACIKAN_DEMO);
			ringkas.put("jumlahRacikan", JUMLAH_RACIKAN_DEMO);
			ringkas.put("jumlahAntrean", JUMLAH_ANTREAN_DEMO);
			ringkas.put("bahanRacikanBaru", bahanDibuat);
			ringkas.put("racikanBaru", racikanDibuat);
			ringkas.put("formulaRacikanOperasionalBaru", formulaRacikanDibuat);
			ringkas.put("formulaProduksiOperasionalBaru", formulaProduksiDibuat);
			ringkas.put("antreanBaru", antreanDibuat);
			ringkas.put("items", items);
			ringkas.put("resepId", resep.getId());
			ringkas.put("catatan", "Setiap obat demo dilengkapi batch dan stok awal; gunakan Penerimaan PBF/Opname untuk menguji mutasi berikutnya.");
			hasil.put("status", "00");
			hasil.put("ringkasan", ringkas);
			provisionTahap = "Selesai";
		} catch (Exception e) {
			try {
				Transaction aktif = session.getTransaction();
				if (aktif != null && aktif.isActive()) aktif.rollback();
			} catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Data akun dan tenaga medis sengaja memakai transaksi terpisah. Katalog obat
	 * adalah hasil utama tombol Data Contoh dan tidak boleh ikut di-rollback bila
	 * data pegawai lama pada suatu instalasi belum lengkap.
	 */
	private static void seedPendukungDemoTerpisah(JSONObject ringkas) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			provisionTahap = "Menyiapkan akun dan tenaga medis demo";
			tx = session.beginTransaction();
			Map<String, Tbmrole> roleDemo = ApotikEmedikSeedHelper.pastikanRoleDemo(session);
			seedAkunOperasionalDemo(session, roleDemo, ringkas);
			seedTenagaMedis(session, ringkas);
			tx.commit();
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			try {
				ringkas.put("peringatanDataPendukung", "Katalog obat berhasil, tetapi akun/tenaga medis belum lengkap: "
						+ (e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
			} catch (Exception ignore) { }
			try {
				ais.common.ErrorAuditUtil.record(e, "apotik-provision-demo-pendukung");
			} catch (Throwable ignore) { }
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Pastikan 1.000 bahan racikan sintetik, stok, batch, dan profilnya tersedia. */
	private static int ensureBahanRacikanDemo(Session session) {
		SatuanItem satuan = (SatuanItem) session.createCriteria(SatuanItem.class)
				.add(Restrictions.eq("nama", "Gram").ignoreCase()).setMaxResults(1).uniqueResult();
		if (satuan == null) {
			satuan = new SatuanItem();
			satuan.setNama("Gram");
			satuan.setNamaAwal("g");
			satuan.setJumlah(Integer.valueOf(1));
			session.save(satuan);
		}
		JenisItemMedis jenis = (JenisItemMedis) session.createCriteria(JenisItemMedis.class)
				.add(Restrictions.eq("kode", "BRC").ignoreCase()).setMaxResults(1).uniqueResult();
		if (jenis == null) {
			jenis = new JenisItemMedis();
			jenis.setKode("BRC");
			jenis.setNama("Bahan Racikan");
			session.save(jenis);
		}
		String[] zat = { "Amylum", "Lactosum", "Saccharum Lactis", "Aerosil", "Talcum",
				"Zinc Oxide", "Acidum Salicylicum", "Mentholum", "Camphora", "Glycerinum",
				"Paraffinum Liquidum", "Vaselin Album", "Cetyl Alcohol", "Propylene Glycol",
				"Sodium Bicarbonate", "Calcium Carbonate", "Magnesium Oxide", "Kaolin",
				"Pectin", "Gelatin" };
		int dibuat = 0;
		for (int i = 1; i <= JUMLAH_BAHAN_RACIKAN_DEMO; i++) {
			String kode = "DEMO-BHN-" + pad(i, 4);
			ItemMedis sebelum = (ItemMedis) session.createCriteria(ItemMedis.class)
					.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
			String nama = "DATA SAMPLE Bahan Racikan " + zat[(i - 1) % zat.length]
					+ " Grade Farmasi " + pad(i, 4);
			double beli = 100 + ((i * 37) % 7500);
			double jual = Math.ceil((beli * 1.25) / 100.0) * 100.0;
			ItemMedis item = buatItem(session, kode, nama, satuan, jenis, jual, beli);
			item.setBarcode("89888" + pad(i, 8));
			item.setKandungan(zat[(i - 1) % zat.length]
					+ " — DATA SINTETIK/UAT, bukan acuan formulasi klinis");
			item.setBatasMinimalStok(Integer.valueOf(50 + (i % 150)));
			session.saveOrUpdate(item);
			ensureProfil(session, item, ApotikItemProfile.GOLONGAN_BEBAS, false);
			ensureBatchDemo(session, item, 500 + (i % 1500), 180 + (i % 720));
			if (sebelum == null) dibuat++;
			if (i % 250 == 0) {
				session.flush();
				session.getTransaction().commit();
				session.beginTransaction();
			}
		}
		return dibuat;
	}

	@SuppressWarnings("unchecked")
	private static int ensureAntreanDemo(Session session, JSONObject request) throws Exception {
		long nilaiToko = request == null ? 1L : request.optLong("toko_id", 1L);
		if (nilaiToko <= 0) nilaiToko = 1L;
		Long tokoId = Long.valueOf(nilaiToko);
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
		List<ItemMedis> bahan = session.createQuery(
				"from ItemMedis i where i.kode like :kode order by i.kode")
				.setString("kode", "DEMO-BHN-%").setMaxResults(JUMLAH_BAHAN_RACIKAN_DEMO).list();
		List<Resep> resep = session.createQuery(
				"from Resep r where r.kode like :kode order by r.id desc")
				.setString("kode", "RSP-DEMO-%").setMaxResults(JUMLAH_ANTREAN_DEMO).list();
		int dibuat = 0;
		for (int i = 1; i <= JUMLAH_ANTREAN_DEMO; i++) {
			String kode = "UAT" + pad(i, 3);
			if (kodeAda.contains(kode)) continue;
			AntreanFarmasi antrean = new AntreanFarmasi();
			antrean.setTokoId(tokoId);
			if (!resep.isEmpty()) antrean.setResepId(resep.get((i - 1) % resep.size()).getId());
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
			if (!bahan.isEmpty()) {
				obat.put(new JSONObject().put("nama", bahan.get(i % bahan.size()).getNama())
						.put("jumlah", (1 + (i % 3)) + " bungkus"));
				obat.put(new JSONObject().put("nama", bahan.get((i + 37) % bahan.size()).getNama())
						.put("jumlah", (1 + (i % 2)) + " kapsul"));
			}
			antrean.setDaftarObat(obat.toString());
			antrean.setCatatanPublik(i <= 12 ? "Obat siap diambil di loket."
					: "Obat sedang diproses oleh Instalasi Farmasi.");
			antrean.setOleh("Provisioning DATA SAMPLE/UAT");
			antrean.setOlehId("seed_demo");
			session.save(antrean);
			dibuat++;
		}
		return dibuat;
	}

	private static JSONObject verifikasiVolumeDemo() throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONObject verifikasi = new JSONObject();
			long obat = ((Number) session.createQuery("select count(i) from ItemMedis i where "
					+ "i.kode = 'UJI-PCT' or i.kode = 'UJI-CDN' or i.kode like 'DEMO-OBT-%'")
					.uniqueResult()).longValue();
			long bahan = ((Number) session.createQuery("select count(i) from ItemMedis i "
					+ "where i.kode like 'DEMO-BHN-%'").uniqueResult()).longValue();
			long formulaRacikan = ((Number) session.createQuery("select count(r) from Racikan r "
					+ "where r.kode like 'RAC-UAT-%'").uniqueResult()).longValue();
			long formulaProduksi = ((Number) session.createQuery("select count(distinct b.itemInduk.id) "
					+ "from BahanBakuItem b where b.itemInduk.kode like 'DEMO-OBT-%'")
					.uniqueResult()).longValue();
			long resepSiap = ((Number) session.createQuery("select count(r) from Resep r "
					+ "where (r.kode = 'RSP-UJI-1' or r.kode like 'RSP-DEMO-%') and not exists "
					+ "(select tm.id from TransaksiMedis tm where tm.resep = r)")
					.uniqueResult()).longValue();
			Calendar awal = Calendar.getInstance();
			awal.set(Calendar.HOUR_OF_DAY, 0);
			awal.set(Calendar.MINUTE, 0);
			awal.set(Calendar.SECOND, 0);
			awal.set(Calendar.MILLISECOND, 0);
			long antrean = ((Number) session.createQuery("select count(a) from AntreanFarmasi a "
					+ "where a.tanggalDibuat >= :awal").setTimestamp("awal", awal.getTime())
					.uniqueResult()).longValue();
			verifikasi.put("obatJadi", obat);
			verifikasi.put("bahanRacikan", bahan);
			verifikasi.put("resepSiapJual", resepSiap);
			verifikasi.put("formulaRacikanOperasional", formulaRacikan);
			verifikasi.put("formulaProduksiOperasional", formulaProduksi);
			verifikasi.put("antreanHariIni", antrean);
			verifikasi.put("targetObatJadi", JUMLAH_OBAT_DEMO);
			verifikasi.put("targetBahanRacikan", JUMLAH_BAHAN_RACIKAN_DEMO);
			verifikasi.put("targetResepSiapJual", 500);
			verifikasi.put("targetFormulaRacikanOperasional", JUMLAH_FORMULA_UAT);
			verifikasi.put("targetFormulaProduksiOperasional", JUMLAH_FORMULA_UAT);
			verifikasi.put("targetAntrean", JUMLAH_ANTREAN_DEMO);
			verifikasi.put("lulus", obat >= JUMLAH_OBAT_DEMO
					&& bahan >= JUMLAH_BAHAN_RACIKAN_DEMO && resepSiap >= 500
					&& formulaRacikan >= JUMLAH_FORMULA_UAT
					&& formulaProduksi >= JUMLAH_FORMULA_UAT
					&& antrean >= JUMLAH_ANTREAN_DEMO);
			return verifikasi;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	@SuppressWarnings("unchecked")
	private static int ensureRacikanDemo(Session session, ItemMedis obatA, ItemMedis obatB) {
		if (obatA == null || obatB == null) return 0;
		List<ItemMedis> bahan = session.createQuery(
				"from ItemMedis i where i.kode like :kode order by i.kode")
				.setString("kode", "DEMO-BHN-%").list();
		java.util.List<Resep> daftar = session.createQuery(
				"from Resep r where r.kode = :uji or r.kode like :demo")
				.setString("uji", "RSP-UJI-1").setString("demo", "RSP-DEMO-%").list();
		java.util.Set<String> ada = new java.util.HashSet<String>();
		for (Resep resepAda : daftar) {
			ada.add(resepAda.getKode());
			int nomor = "RSP-UJI-1".equals(resepAda.getKode()) ? 1
					: nomorDariKode(resepAda.getKode(), "RSP-DEMO-");
			if (nomor > 0) {
				String namaBaru = namaRacikanApotik(nomor);
				if (!namaBaru.equals(resepAda.getKeterangan())) {
					resepAda.setKeterangan(namaBaru);
					session.update(resepAda);
				}
			}
		}
		int dibuat = 0;
		if (!ada.contains("RSP-UJI-1")) {
			Resep resep = new Resep();
			resep.setKode("RSP-UJI-1");
			resep.setKeterangan(namaRacikanApotik(1));
			session.save(resep);
			buatDetailRacikan(session, resep, bahan.isEmpty() ? obatA : bahan.get(0), 10);
			dibuat++;
		}
		for (int i = 2; i <= JUMLAH_RACIKAN_DEMO; i++) {
			// Pertahankan format historis 3 digit (002..999; 1000 dst tetap utuh)
			// agar provisioning versi baru tidak menggandakan racikan lama 2..100.
			String kode = "RSP-DEMO-" + pad(i, 3);
			if (!ada.contains(kode)) {
				Resep racikan = new Resep();
				racikan.setKode(kode);
				racikan.setKeterangan(namaRacikanApotik(i));
				session.save(racikan);
				if (bahan.isEmpty()) {
					buatDetailRacikan(session, racikan, i % 2 == 0 ? obatA : obatB, 1 + (i % 10));
				} else {
					buatDetailRacikan(session, racikan, bahan.get(i % bahan.size()), 1 + (i % 5));
					buatDetailRacikan(session, racikan, bahan.get((i + 97) % bahan.size()), 1 + ((i + 2) % 4));
					buatDetailRacikan(session, racikan, bahan.get((i + 211) % bahan.size()), 1 + ((i + 1) % 3));
				}
				dibuat++;
			}
			if (i % 250 == 0) {
				// KE-FIX: lihat catatan commit periodik di loop katalog utama -- 5.000
				// racikan dalam satu transaksi juga bisa melewati unreturnedConnectionTimeout.
				session.flush();
				session.getTransaction().commit();
				session.beginTransaction();
			}
		}
		return dibuat;
	}

	private static void buatDetailRacikan(Session session, Resep resep, ItemMedis item, int jumlah) {
		ResepDetail detail = new ResepDetail();
		detail.setResep(resep);
		detail.setItem(item);
		detail.setJumlah(Double.valueOf(jumlah));
		detail.setTanggal(new Date());
		detail.setKeterangan("Komposisi DATA SAMPLE/UAT; wajib diverifikasi apoteker sebelum penggunaan nyata.");
		session.save(detail);
	}

	/**
	 * Membuat formula Racikan sungguhan (bukan hanya baris item resep) dan
	 * menautkan 250 resep demo. Ini yang dipakai endpoint kasir racikan dan UAT
	 * tebus resep campuran; seluruh isi adalah DATA SAMPLE, bukan formula klinis.
	 */
	@SuppressWarnings("unchecked")
	private static int ensureFormulaRacikanUat(Session session) {
		List<ItemMedis> bahan = session.createQuery(
				"from ItemMedis i where i.kode like :kode order by i.kode")
				.setString("kode", "DEMO-BHN-%").setMaxResults(JUMLAH_BAHAN_RACIKAN_DEMO).list();
		List<Resep> resep = session.createQuery(
				"from Resep r where r.kode like :kode order by r.kode")
				.setString("kode", "RSP-DEMO-%").setMaxResults(JUMLAH_FORMULA_UAT).list();
		if (bahan.size() < 3) return 0;
		int dibuat = 0;
		for (int i = 1; i <= JUMLAH_FORMULA_UAT; i++) {
			String kode = "RAC-UAT-" + pad(i, 4);
			Racikan racikan = (Racikan) session.createCriteria(Racikan.class)
					.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
			if (racikan == null) {
				racikan = new Racikan();
				racikan.setKode(kode);
				racikan.setNama(namaRacikanApotik(i));
				racikan.setKeterangan("DATA SAMPLE/UAT -- formula wajib diverifikasi apoteker sebelum penggunaan nyata.");
				racikan.setOleh("Provisioning DATA SAMPLE/UAT");
				racikan.setOlehId("seed_demo");
				session.save(racikan);
				dibuat++;
			}
			long jumlahDetail = ((Number) session.createQuery(
					"select count(d) from RacikanDetail d where d.racikan = :racikan")
					.setParameter("racikan", racikan).uniqueResult()).longValue();
			if (jumlahDetail == 0) {
				for (int x = 0; x < 3; x++) {
					ItemMedis komponen = bahan.get((i * 17 + x * 97) % bahan.size());
					RacikanDetail d = new RacikanDetail();
					d.setRacikan(racikan);
					d.setItem(komponen);
					d.setJumlah(Double.valueOf(1 + ((i + x) % 4)));
					d.setHargaTransaksi(komponen.getDefaultHargaJual());
					d.setKeterangan("Komponen DATA SAMPLE/UAT");
					d.setOleh("Provisioning DATA SAMPLE/UAT");
					d.setOlehId("seed_demo");
					session.save(d);
				}
			}
			if (i <= resep.size()) {
				Resep resepUat = resep.get(i - 1);
				long sudah = ((Number) session.createQuery(
						"select count(d) from ResepDetail d where d.resep = :resep and d.racikan = :racikan")
						.setParameter("resep", resepUat).setParameter("racikan", racikan)
						.uniqueResult()).longValue();
				if (sudah == 0) {
					ResepDetail rd = new ResepDetail();
					rd.setResep(resepUat); rd.setRacikan(racikan); rd.setJumlah(Double.valueOf(1));
					rd.setTanggal(new Date()); rd.setKeterangan("Racikan DATA SAMPLE/UAT");
					rd.setOleh("Provisioning DATA SAMPLE/UAT"); rd.setOlehId("seed_demo");
					session.save(rd);
					racikan.setResepDetail(rd);
					session.update(racikan);
				}
			}
		}
		return dibuat;
	}

	/** 250 formula produksi barang jadi, masing-masing memakai dua bahan demo. */
	@SuppressWarnings("unchecked")
	private static int ensureFormulaProduksiUat(Session session) {
		List<ItemMedis> hasil = session.createQuery(
				"from ItemMedis i where i.kode like :kode order by i.kode")
				.setString("kode", "DEMO-OBT-%").setMaxResults(JUMLAH_FORMULA_UAT).list();
		List<ItemMedis> bahan = session.createQuery(
				"from ItemMedis i where i.kode like :kode order by i.kode")
				.setString("kode", "DEMO-BHN-%").setMaxResults(JUMLAH_BAHAN_RACIKAN_DEMO).list();
		if (bahan.size() < 2) return 0;
		int dibuat = 0;
		for (int i = 0; i < hasil.size(); i++) {
			ItemMedis output = hasil.get(i);
			long jumlah = ((Number) session.createQuery(
					"select count(b) from BahanBakuItem b where b.itemInduk = :output")
					.setParameter("output", output).uniqueResult()).longValue();
			if (jumlah > 0) continue;
			for (int x = 0; x < 2; x++) {
				BahanBakuItem b = new BahanBakuItem();
				b.setItemInduk(output);
				b.setItem(bahan.get((i * 11 + x * 101) % bahan.size()));
				b.setQty(Double.valueOf(1 + ((i + x) % 3)));
				b.setKeterangan("Formula produksi DATA SAMPLE/UAT");
				b.setOleh("Provisioning DATA SAMPLE/UAT");
				b.setOlehId("seed_demo");
				session.save(b);
			}
			dibuat++;
		}
		return dibuat;
	}

	@SuppressWarnings("unchecked")
	private static int perbaruiNamaKatalogDemo(Session session) {
		java.util.List<ItemMedis> items = session.createQuery(
				"from ItemMedis i where i.kode = :ujiA or i.kode = :ujiB or i.kode like :demo")
				.setString("ujiA", "UJI-PCT").setString("ujiB", "UJI-CDN")
				.setString("demo", "DEMO-OBT-%").list();
		int diperbarui = 0;
		for (ItemMedis item : items) {
			String namaBaru;
			if ("UJI-PCT".equals(item.getKode())) namaBaru = "Paracetamol 500 mg Tablet";
			else if ("UJI-CDN".equals(item.getKode())) namaBaru = "Codeine 10 mg Tablet";
			else {
				int nomor = nomorDariKode(item.getKode(), "DEMO-OBT-");
				if (nomor < 1) continue;
				namaBaru = namaObatApotik(nomor);
			}
			if (!namaBaru.equals(item.getNama())) {
				item.setNama(namaBaru);
				session.update(item);
				diperbarui++;
			}
			if (diperbarui > 0 && diperbarui % 250 == 0) {
				// KE-FIX: sama seperti loop katalog utama -- bisa berjalan atas ribuan item
				// pada re-run, jadi ditahan periodik agar tak melewati unreturnedConnectionTimeout.
				session.flush();
				session.getTransaction().commit();
				session.beginTransaction();
			}
		}
		return diperbarui;
	}

	private static int nomorDariKode(String kode, String prefix) {
		if (kode == null || !kode.startsWith(prefix)) return -1;
		try { return Integer.parseInt(kode.substring(prefix.length())); }
		catch (NumberFormatException e) { return -1; }
	}

	private static String namaObatApotik(int nomor) {
		String[] zat = { "Amoxicillin", "Paracetamol", "Ibuprofen", "Cetirizine",
				"Metformin", "Amlodipine", "Omeprazole", "Azithromycin", "Salbutamol",
				"Vitamin B Kompleks", "Asam Mefenamat", "Captopril", "Domperidone",
				"Dexamethasone", "Ambroxol", "Cefixime", "Loratadine", "Simvastatin",
				"Furosemide", "Clopidogrel", "Acetylcysteine", "Allopurinol",
				"Bisoprolol", "Candesartan", "Diclofenac Sodium", "Gabapentin",
				"Lansoprazole", "Levofloxacin", "Meloxicam", "Ondansetron" };
		String[] bentuk = { "Tablet", "Kaplet", "Kapsul", "Sirup", "Drops", "Salep" };
		String[] pabrik = { "Kimia Farma", "Indofarma", "Phapros", "Dexa Medica",
				"Sanbe Farma", "Bernofarm", "Novapharin", "Meprofarm", "Erela", "Ifars" };
		int dasar = Math.max(0, nomor - 1);
		int dosis = 50 + ((nomor * 25) % 950);
		return zat[dasar % zat.length] + " " + dosis + " mg "
				+ bentuk[(dasar / zat.length) % bentuk.length] + " "
				+ pabrik[(dasar / (zat.length * bentuk.length)) % pabrik.length];
	}

	private static String namaRacikanApotik(int nomor) {
		String[] kebutuhan = { "Demam Dewasa", "Demam Anak", "Batuk Kering", "Batuk Berdahak",
				"Pilek dan Alergi", "Nyeri Ringan", "Gangguan Lambung", "Mual dan Muntah",
				"Vitamin Pemulihan", "Perawatan Kulit" };
		String[] bentuk = { "Puyer 10 Bungkus", "Kapsul 10 Butir", "Sirup 60 ml",
				"Krim 10 g", "Salep 10 g" };
		int dasar = Math.max(0, nomor - 1);
		return "Racikan " + kebutuhan[dasar % kebutuhan.length] + " - "
				+ bentuk[(dasar / kebutuhan.length) % bentuk.length];
	}

	private static String pad(int nilai, int panjang) {
		String hasil = String.valueOf(nilai);
		while (hasil.length() < panjang) {
			hasil = "0" + hasil;
		}
		return hasil;
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
			if (nomor % 250 == 0) {
				session.flush();
				session.getTransaction().commit();
				session.beginTransaction();
			}
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
		// Tiga jenis pengguna Apotik baku: Pemilik, Kasir, Manajemen (masing-masing
		// satu akun demo, roleId lihat ApotikEmedikSeedHelper.pastikanRoleDemo()).
		buatAkunDemo(session, "demo_pemilik_apotik", "Pemilik Apotik Demo",
				roleDemo.get(ApotikEmedikSeedHelper.ROLE_APOTIK_PEMILIK_DEMO), null, null);
		akun.put(akunJson("demo_pemilik_apotik", "Pemilik Apotik",
				"Akses penuh seluruh modul apotik dan kasir eMedik"));
		buatAkunDemo(session, "demo_kasir_apotik", "Kasir Apotik Demo",
				roleDemo.get(ApotikEmedikSeedHelper.ROLE_APOTIK_KASIR_DEMO), null, null);
		akun.put(akunJson("demo_kasir_apotik", "Kasir Apotik", "Penjualan, resep, dan tagihan"));
		buatAkunDemo(session, "demo_manajemen_apotik", "Manajemen Apotik Demo",
				roleDemo.get(ApotikEmedikSeedHelper.ROLE_APOTIK_MANAJEMEN_DEMO), null, null);
		akun.put(akunJson("demo_manajemen_apotik", "Manajemen Apotik",
				"Formularium, pengadaan, opname, retur, dan laporan"));
		buatAkunDemo(session, "demo_apoteker", "Apoteker Demo",
				roleDemo.get(ApotikEmedikSeedHelper.ROLE_APOTIK), null, null);
		akun.put(akunJson("demo_apoteker", "Apoteker", "Akses penuh apotik dan kasir eMedik"));
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
		// KE-FIX (tbmuser.userrole NOT NULL): sebelumnya role null diteruskan diam-diam
		// ke Hibernate dan baru gagal di level constraint DB dengan pesan generik
		// c3p0/PSQLException yang tidak menunjuk akun/role mana yang bermasalah. Gagal
		// cepat &amp; jelas di sini jauh lebih mudah didiagnosis.
		if (role == null) {
			throw new IllegalStateException("Role demo untuk akun '" + userId
					+ "' belum tersedia -- pastikanRoleDemo() gagal membuat/memuat role terkait.");
		}
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
		}
		it.setNama(nama);
		it.setSatuanItem(satuan);
		it.setJenisItem(jenis);
		it.setDefaultHargaJual(Double.valueOf(hargaJual));
		it.setDefaultHargaBeli(Double.valueOf(hargaBeli));
		if (it.getId() == null) {
			session.save(it);
		} else {
			session.saveOrUpdate(it);
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
