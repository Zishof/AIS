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
import ais.database.model.sirs.ApotikBatchKonsumsi;
import ais.database.model.sirs.ApotikNarkotikaLog;
import ais.database.model.sirs.ApotikPembayaranTransaksi;
import ais.database.model.sirs.AlergiPasien;
import ais.database.model.sirs.AntreanFarmasi;
import ais.database.model.sirs.BahanBakuItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Kadaluarsa;
import ais.database.model.sirs.JenisItemMedis;
import ais.database.model.sirs.KodeTransaksiMedis;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Icd;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.Resep;
import ais.database.model.sirs.ResepDetail;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.RacikanDetail;
import ais.database.model.sirs.SatuanItem;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;

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
	private static final int JUMLAH_ANTREAN_DEMO = 500;
	private static final int JUMLAH_PROFIL_RESEP_UAT = 500;
	private static final int JUMLAH_PENJUALAN_TERKENDALI_UAT = 500;
	private static final int JUMLAH_FORMULA_RACIKAN_UAT = 500;
	private static final int JUMLAH_FORMULA_PRODUKSI_UAT = 500;
	private static final int JUMLAH_COLD_CHAIN_UAT = 100;
	private static final int JUMLAH_RECALL_UAT = 100;
	private static final int STOK_MINIMAL_BAHAN_FORMULA_UAT = 10000;
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
					+ "500 profil resep lengkap, 500 penjualan terkendali, 500 antrean, dan tenaga medis dimulai di latar.");
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
			JSONObject stokBahanFormula = ensureStokBahanFormulaUat(session);
			int profilColdChainDiperbarui = ensureColdChainUat(session);
			int batchRecallDibuat = ensureRecallUat(session);
			int pasienKlinisDibuat = ensurePasienKlinisDemo(session);
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
			ringkas.put("stokBahanFormula", stokBahanFormula);
			ringkas.put("jumlahProfilResepLengkap", JUMLAH_PROFIL_RESEP_UAT);
			ringkas.put("profilColdChainDiperbarui", profilColdChainDiperbarui);
			ringkas.put("batchRecallBaru", batchRecallDibuat);
			ringkas.put("pasienKlinisSampleBaru", pasienKlinisDibuat);
			ringkas.put("antreanBaru", antreanDibuat);
			ringkas.put("items", items);
			ringkas.put("resepId", resep.getId());
			ringkas.put("catatan", "Setiap obat demo dilengkapi batch dan stok awal; 500 resep pertama memakai profil klinis SAMPLE/UAT lengkap. Gunakan Penerimaan PBF/Opname untuk menguji mutasi berikutnya.");
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

	private static Date tanggalKlinisSample(int nomor) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, -((nomor - 1) % 30));
		c.set(Calendar.HOUR_OF_DAY, 8 + (nomor % 10));
		c.set(Calendar.MINUTE, (nomor * 7) % 60);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	private static Icd ensureIcdSample(Session session, String kode, String nama) {
		Icd icd = (Icd) session.createCriteria(Icd.class)
				.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
		if (icd == null) {
			icd = new Icd();
			icd.setKode(kode);
			icd.setNama_indonesia(nama);
			icd.setNama_english("DATA SAMPLE/UAT");
			icd.setOleh("Provisioning DATA SAMPLE/UAT");
			icd.setOlehId("seed_demo");
			session.save(icd);
		}
		return icd;
	}

	private static Poly ensurePolySample(Session session, String kode, String nama) {
		Poly poly = (Poly) session.createCriteria(Poly.class)
				.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
		if (poly == null) {
			poly = new Poly();
			poly.setKode(kode);
			poly.setNama(nama);
			poly.setJenis("Rawat Jalan");
			poly.setKeterangan("DATA SAMPLE/UAT");
			poly.setOleh("Provisioning DATA SAMPLE/UAT");
			poly.setOlehId("seed_demo");
			session.save(poly);
		}
		return poly;
	}

	private static ais.database.model.sirs.Instalasi ensureInstalasiSample(Session session) {
		ais.database.model.sirs.Instalasi instalasi = (ais.database.model.sirs.Instalasi) session
				.createCriteria(ais.database.model.sirs.Instalasi.class)
				.add(Restrictions.eq("nama", "Klinik Rawat Jalan Demo").ignoreCase())
				.setMaxResults(1).uniqueResult();
		if (instalasi == null) {
			instalasi = new ais.database.model.sirs.Instalasi();
			instalasi.setNama("Klinik Rawat Jalan Demo");
			instalasi.setKeterangan("DATA SAMPLE/UAT — fasilitas sintetis, bukan fasilitas kesehatan nyata.");
			instalasi.setOleh("Provisioning DATA SAMPLE/UAT");
			instalasi.setOlehId("seed_demo");
			session.save(instalasi);
		}
		return instalasi;
	}

	/**
	 * Hubungkan sedikitnya 500 resep demo ke pasien, dokter, kunjungan,
	 * diagnosis ICD, alergi, dan aturan pakai. Seluruh nilai ditandai SAMPLE
	 * dan idempoten; resep/pasien nyata tidak disentuh.
	 */
	@SuppressWarnings("unchecked")
	private static int ensurePasienKlinisDemo(Session session) {
		List<Resep> reseps = session.createQuery(
				"from Resep r where r.kode like :kode order by r.id desc")
				.setString("kode", "RSP-DEMO-%").setMaxResults(JUMLAH_PROFIL_RESEP_UAT).list();
		List<Dokter> dokters = session.createQuery(
				"from Dokter d where d.kode like :kode order by d.kode")
				.setString("kode", "DEMO-DOK-%").setMaxResults(100).list();
		Shift shift = (Shift) session.createCriteria(Shift.class)
				.setMaxResults(1).uniqueResult();
		if (shift == null) {
			shift = new Shift();
			shift.setNama("Shift Demo Apotik");
			shift.setKeterangan("DATA SAMPLE/UAT");
			shift.setOleh("Provisioning DATA SAMPLE/UAT");
			shift.setOlehId("seed_demo");
			session.save(shift);
		}
		String[] kodeIcd = { "J06.9", "K30", "I10", "E11.9", "L20.9", "R51",
				"M79.1", "J30.9", "R05", "A09", "K21.9", "N39.0" };
		String[] penyakit = { "Infeksi saluran napas atas akut", "Dispepsia",
				"Hipertensi esensial", "Diabetes melitus tipe 2 tanpa komplikasi",
				"Dermatitis atopik", "Sakit kepala", "Nyeri otot", "Rhinitis alergi",
				"Batuk", "Gastroenteritis", "Refluks gastroesofageal", "Infeksi saluran kemih" };
		String[] keluhan = { "Demam ringan, pilek, dan tenggorokan tidak nyaman sejak dua hari",
				"Perih ulu hati dan cepat kenyang", "Pusing; tekanan darah perlu dipantau",
				"Kontrol berkala gula darah", "Gatal dan kemerahan pada kulit",
				"Sakit kepala tanpa tanda bahaya", "Nyeri otot setelah aktivitas",
				"Bersin berulang dan hidung gatal", "Batuk tanpa sesak",
				"Mual dan buang air besar cair", "Rasa panas dari lambung ke dada",
				"Nyeri saat berkemih" };
		String[] aturan = { "3 x 1 sesudah makan", "2 x 1 sebelum makan", "1 x 1 pagi",
				"1 x 1 malam", "oles tipis 2 x sehari", "bila perlu, maksimal 3 x sehari" };
		String[] poliNama = { "Poli Umum Demo", "Poli Penyakit Dalam Demo", "Poli Anak Demo",
				"Poli Kulit Demo", "Poli THT Demo", "Poli Geriatri Demo" };
		Poly[] poli = new Poly[poliNama.length];
		for (int x = 0; x < poli.length; x++) {
			poli[x] = ensurePolySample(session, "APT-UAT-POLI-" + pad(x + 1, 2), poliNama[x]);
		}
		ais.database.model.sirs.Instalasi instalasi = ensureInstalasiSample(session);
		Icd[] daftarIcd = new Icd[kodeIcd.length];
		for (int x = 0; x < daftarIcd.length; x++) {
			daftarIcd[x] = ensureIcdSample(session, kodeIcd[x], penyakit[x]);
		}
		int dibuat = 0;
		for (int i = 1; i <= reseps.size(); i++) {
			Resep resep = reseps.get(i - 1);
			String nomor = resep.getKode() != null && resep.getKode().startsWith("RSP-DEMO-")
					? resep.getKode().substring("RSP-DEMO-".length()) : pad(i, 3);
			String kodePasien = "APT-UAT-" + nomor;
			Pasien pasien = (Pasien) session.createCriteria(Pasien.class)
					.add(Restrictions.eq("kode", kodePasien)).setMaxResults(1).uniqueResult();
			if (pasien == null) {
				pasien = new Pasien();
				pasien.setKode(kodePasien);
				pasien.setNama("Pasien Sample Apotik " + nomor);
				dibuat++;
			}
			int umur = 18 + (i % 63);
			Calendar lahir = Calendar.getInstance();
			lahir.add(Calendar.YEAR, -umur);
			lahir.set(Calendar.MONTH, (i - 1) % 12);
			lahir.set(Calendar.DAY_OF_MONTH, 1 + (i % 27));
			pasien.setJenisKelamin(i % 2 == 0 ? "P" : "L");
			pasien.setTanggalLahir(lahir.getTime());
			pasien.setUmur(Integer.valueOf(umur));
			pasien.setTempatLahir(i % 3 == 0 ? "Bandung" : (i % 3 == 1 ? "Jakarta" : "Surabaya"));
			pasien.setAlamat("Jl. Contoh UAT No. " + i);
			pasien.setAlamatLengkap("Jl. Contoh UAT No. " + i + ", Indonesia — DATA SAMPLE/UAT");
			pasien.setNoHp("0800" + pad(i, 8));
			pasien.setKewarganegaraan("Indonesia");
			pasien.setStatusPerkawinan(i % 3 == 0 ? "Menikah" : "Belum Menikah");
			pasien.setAktif(Boolean.TRUE);
			pasien.setKeterangan("Profil pasien sintetis untuk demonstrasi keselamatan farmasi; bukan pasien nyata.");
			pasien.setOleh("Provisioning DATA SAMPLE/UAT");
			pasien.setOlehId("seed_demo");
			session.saveOrUpdate(pasien);

			DiagnosaPenyakit diagnosa = resep.getDiagnosaPenyakit();
			if (diagnosa == null) {
				diagnosa = new DiagnosaPenyakit();
				diagnosa.setKode("DX-APT-UAT-" + nomor);
			}
			int jenis = (i - 1) % penyakit.length;
			Date tanggal = tanggalKlinisSample(i);
			Dokter dokter = dokters.isEmpty() ? null : dokters.get((i - 1) % dokters.size());
			Poly poliResep = poli[(i - 1) % poli.length];
			diagnosa.setKode("DX-APT-UAT-" + nomor);
			diagnosa.setKeterangan(penyakit[jenis] + " [DATA SAMPLE/UAT]");
			diagnosa.setKeluhanPasien(keluhan[jenis]);
			diagnosa.setKeluhanDiagnosa("Anamnesis dan pemeriksaan SAMPLE/UAT; wajib diverifikasi tenaga medis.");
			diagnosa.setKesimpulanPemeriksaan(penyakit[jenis]
					+ " — indikasi contoh, bukan keputusan klinis untuk pasien nyata.");
			diagnosa.setApakahMenular(DiagnosaPenyakit.TIDAK_MENULAR);
			diagnosa.setDiagnosaAwal1(daftarIcd[jenis]);
			diagnosa.setDiagnosaAkhir1(daftarIcd[jenis]);
			diagnosa.setPasien(pasien);
			diagnosa.setDokter(dokter);
			diagnosa.setPoly(poliResep);
			diagnosa.setInstalasi(instalasi);
			diagnosa.setTanggal(tanggal);
			diagnosa.setShift(shift);
			diagnosa.setOleh("Provisioning DATA SAMPLE/UAT");
			diagnosa.setOlehId("seed_demo");

			if (dokter != null) {
				Tbmuser userDokter = (Tbmuser) session.createCriteria(Tbmuser.class)
						.add(Restrictions.eq("dokter", dokter)).setMaxResults(1).uniqueResult();
				if (userDokter != null) {
					String kodeKunjungan = "APT-UAT-KUNJ-" + nomor;
					Pendaftaran pendaftaran = (Pendaftaran) session.createCriteria(Pendaftaran.class)
							.add(Restrictions.eq("kode", kodeKunjungan)).setMaxResults(1).uniqueResult();
					if (pendaftaran == null) {
						pendaftaran = new Pendaftaran();
						pendaftaran.setKode(kodeKunjungan);
					}
					pendaftaran.setPasien(pasien);
					pendaftaran.setDokter(dokter);
					pendaftaran.setTbmuser(userDokter);
					pendaftaran.setPoly(poliResep);
					pendaftaran.setTanggalPendaftaran(tanggal);
					pendaftaran.setDilayaniTanggal(tanggal);
					pendaftaran.setJenis(Pendaftaran.RAWAT_JALAN);
					pendaftaran.setStatusPendaftaran(Pendaftaran.KELUAR);
					pendaftaran.setSumberPasien(i % 5 == 0
							? Pendaftaran.SUMBER_PASIEN_DARI_TAMU : Pendaftaran.SUMBER_PASIEN_POLI);
					pendaftaran.setNamaDokterPengirim(i % 5 == 0 ? "dr. Pengirim Sample " + nomor : "");
					pendaftaran.setNamaPenjamin(i % 4 == 0 ? "Asuransi Sample/UAT" : "Umum");
					pendaftaran.setNomorAntrian(Integer.valueOf(1 + (i % 50)));
					pendaftaran.setUmur(Integer.valueOf(umur));
					pendaftaran.setBaru(Boolean.FALSE);
					pendaftaran.setKeterangan("Kunjungan sintetis DATA SAMPLE/UAT; bukan rekam medis nyata.");
					pendaftaran.setOleh("Provisioning DATA SAMPLE/UAT");
					pendaftaran.setOlehId("seed_demo");
					session.saveOrUpdate(pendaftaran);
					diagnosa.setPendaftaran(pendaftaran);
				}
			}
			session.saveOrUpdate(diagnosa);
			resep.setDiagnosaPenyakit(diagnosa);
			session.saveOrUpdate(resep);

			List<ResepDetail> semuaDetail = session.createQuery(
					"from ResepDetail d where d.resep = :resep order by d.id")
					.setParameter("resep", resep).list();
			for (int d = 0; d < semuaDetail.size(); d++) {
				ResepDetail detail = semuaDetail.get(d);
				detail.setTanggal(tanggal);
				detail.setKeterangan("Aturan pakai SAMPLE/UAT: " + aturan[(i + d) % aturan.length]
						+ "; jumlah dan dosis wajib diverifikasi dokter serta apoteker.");
				session.saveOrUpdate(detail);
			}

			long jumlahAlergi = ((Number) session.createQuery(
					"select count(a) from AlergiPasien a where a.pasien = :pasien")
					.setParameter("pasien", pasien).uniqueResult()).longValue();
			if (jumlahAlergi == 0) {
				AlergiPasien alergi = new AlergiPasien();
				alergi.setPasien(pasien);
				alergi.setKategori(i % 4 == 0 ? AlergiPasien.KATEGORI_OBAT : AlergiPasien.KATEGORI_MAKANAN);
				alergi.setReaksi(i % 4 == 0 ? "Ruam dan sesak" : "Gatal ringan");
				alergi.setKeparahan(i % 4 == 0 ? AlergiPasien.KEPARAHAN_BERAT : AlergiPasien.KEPARAHAN_RINGAN);
				alergi.setStatusKlinis(AlergiPasien.STATUS_AKTIF);
				alergi.setPencatat("Apoteker Demo");
				alergi.setKeterangan("DATA SAMPLE/UAT — wajib diverifikasi sebelum penggunaan nyata.");
				alergi.setOleh("Provisioning DATA SAMPLE/UAT");
				alergi.setOlehId("seed_demo");
				if (i % 4 == 0) {
					List<ResepDetail> detail = session.createQuery(
							"from ResepDetail d where d.resep = :resep and d.item is not null order by d.id")
							.setParameter("resep", resep).setMaxResults(1).list();
					if (!detail.isEmpty()) {
						ItemMedis item = detail.get(0).getItem();
						alergi.setItemMedis(item);
						alergi.setSubstansi(item.getNama());
					}
				} else {
					alergi.setSubstansi(i % 2 == 0 ? "Kacang" : "Udang");
				}
				session.save(alergi);
			}
			if (i % 100 == 0) session.flush();
		}
		return dibuat;
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
			tx = null;
			try {
				provisionTahap = "Melengkapi 500 profil resep dan transaksi laporan";
				tx = session.beginTransaction();
				int pasienLengkap = ensurePasienKlinisDemo(session);
				int terkendali = ensurePenjualanTerkendaliDemo(session);
				ringkas.put("pasienKlinisSampleBaru", pasienLengkap);
				ringkas.put("penjualanTerkendaliSampleBaru", terkendali);
				tx.commit();
				tx = null;
			} catch (Exception e) {
				try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
				try {
					ringkas.put("peringatanDataKlinis", "Akun/tenaga medis berhasil, tetapi data klinis/laporan belum lengkap: "
							+ (e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
				} catch (Exception ignore) { }
				try { ais.common.ErrorAuditUtil.record(e, "apotik-provision-demo-klinis"); }
				catch (Throwable ignore) { }
			}
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

	/**
	 * Buat 500 transaksi historis obat terkendali yang utuh: transaksi, detail,
	 * ledger stok, konsumsi batch, pembayaran, dan register narkotika. Hanya
	 * kode/penanda SAMPLE/UAT yang dipakai, sehingga transaksi nyata tidak
	 * pernah dimodifikasi.
	 */
	@SuppressWarnings("unchecked")
	private static int ensurePenjualanTerkendaliDemo(Session session) {
		List<ApotikItemProfile> profil = session.createQuery(
				"from ApotikItemProfile p where (p.item.kode = :uji or p.item.kode like :demo) "
				+ "and (p.golonganObat = :narkotika or p.golonganObat = :psikotropika) order by p.item.kode")
				.setString("uji", "UJI-CDN").setString("demo", "DEMO-OBT-%")
				.setString("narkotika", ApotikItemProfile.GOLONGAN_NARKOTIKA)
				.setString("psikotropika", ApotikItemProfile.GOLONGAN_PSIKOTROPIKA).list();
		List<Pasien> pasien = session.createQuery(
				"from Pasien p where p.kode like :kode order by p.kode")
				.setString("kode", "APT-UAT-%").setMaxResults(JUMLAH_PROFIL_RESEP_UAT).list();
		List<Dokter> dokter = session.createQuery(
				"from Dokter d where d.kode like :kode order by d.kode")
				.setString("kode", "DEMO-DOK-%").setMaxResults(100).list();
		List<ais.database.model.koperasi.CaraPembayaranKoperasi> caraBayar = session.createCriteria(
				ais.database.model.koperasi.CaraPembayaranKoperasi.class)
				.add(Restrictions.eq("aktif", Boolean.TRUE)).list();
		KodeTransaksiMedis jual = (KodeTransaksiMedis) session.createCriteria(KodeTransaksiMedis.class)
				.add(Restrictions.eq("kode", "AJ")).setMaxResults(1).uniqueResult();
		if (profil.isEmpty() || pasien.isEmpty() || dokter.isEmpty() || jual == null) return 0;

		/*
		 * Muat seluruh data SAMPLE/UAT sekali di depan. Implementasi sebelumnya
		 * menjalankan beberapa Criteria tanpa indeks untuk SETIAP baris (hingga
		 * 3.000 query untuk 500 transaksi). Pada database demo yang sudah besar,
		 * provisioning dapat terlihat berhenti belasan menit. Map berikut membuat
		 * jumlah query konstan dan tetap menjaga sifat idempoten.
		 */
		Map<String, TransaksiMedis> transaksiPerKode = new java.util.HashMap<String, TransaksiMedis>();
		List<TransaksiMedis> transaksiAda = session.createQuery(
				"from TransaksiMedis t where t.kode like :kode")
				.setString("kode", "TRX-APT-UAT-CTL-%").list();
		for (TransaksiMedis t : transaksiAda) transaksiPerKode.put(t.getKode(), t);

		Map<Long, TransaksiMedisDetail> detailPerTransaksi =
				new java.util.HashMap<Long, TransaksiMedisDetail>();
		List<TransaksiMedisDetail> detailAda = session.createQuery(
				"from TransaksiMedisDetail d where d.transaksi.kode like :kode")
				.setString("kode", "TRX-APT-UAT-CTL-%").list();
		for (TransaksiMedisDetail d : detailAda) {
			if (d.getTransaksi() != null && d.getTransaksi().getId() != null) {
				detailPerTransaksi.put(d.getTransaksi().getId(), d);
			}
		}

		Map<Long, DetailTransaksiPasien> ledgerPerDetail =
				new java.util.HashMap<Long, DetailTransaksiPasien>();
		List<DetailTransaksiPasien> ledgerAda = session.createQuery(
				"from DetailTransaksiPasien d where d.keterangan like :kode")
				.setString("kode", "PENJUALAN-TERKENDALI-SAMPLE/UAT-%").list();
		for (DetailTransaksiPasien d : ledgerAda) {
			if (d.getTransaksiDetail() != null && d.getTransaksiDetail().getId() != null) {
				ledgerPerDetail.put(d.getTransaksiDetail().getId(), d);
			}
		}

		Map<Long, ApotikBatchKonsumsi> konsumsiPerDetail =
				new java.util.HashMap<Long, ApotikBatchKonsumsi>();
		List<ApotikBatchKonsumsi> konsumsiAdaList = session.createQuery(
				"from ApotikBatchKonsumsi b where b.transaksiDetail.transaksi.kode like :kode")
				.setString("kode", "TRX-APT-UAT-CTL-%").list();
		for (ApotikBatchKonsumsi b : konsumsiAdaList) {
			if (b.getTransaksiDetail() != null && b.getTransaksiDetail().getId() != null) {
				konsumsiPerDetail.put(b.getTransaksiDetail().getId(), b);
			}
		}

		Map<Long, ApotikNarkotikaLog> logPerDetail =
				new java.util.HashMap<Long, ApotikNarkotikaLog>();
		List<ApotikNarkotikaLog> logAda = session.createQuery(
				"from ApotikNarkotikaLog n where n.transaksiDetail.transaksi.kode like :kode")
				.setString("kode", "TRX-APT-UAT-CTL-%").list();
		for (ApotikNarkotikaLog n : logAda) {
			if (n.getTransaksiDetail() != null && n.getTransaksiDetail().getId() != null) {
				logPerDetail.put(n.getTransaksiDetail().getId(), n);
			}
		}

		Map<Long, ApotikPembayaranTransaksi> pembayaranPerTransaksi =
				new java.util.HashMap<Long, ApotikPembayaranTransaksi>();
		List<ApotikPembayaranTransaksi> pembayaranAda = session.createQuery(
				"from ApotikPembayaranTransaksi p where p.transaksi.kode like :kode")
				.setString("kode", "TRX-APT-UAT-CTL-%").list();
		for (ApotikPembayaranTransaksi p : pembayaranAda) {
			if (p.getTransaksi() != null && p.getTransaksi().getId() != null) {
				pembayaranPerTransaksi.put(p.getTransaksi().getId(), p);
			}
		}

		Set<ItemMedis> itemDipakai = new HashSet<ItemMedis>();
		for (int i = 0; i < JUMLAH_PENJUALAN_TERKENDALI_UAT; i++) {
			itemDipakai.add(profil.get(i % profil.size()).getItem());
		}
		Map<Long, Kadaluarsa> batchPertamaPerItem = new java.util.HashMap<Long, Kadaluarsa>();
		if (!itemDipakai.isEmpty()) {
			List<Kadaluarsa> batchAda = session.createCriteria(Kadaluarsa.class)
					.add(Restrictions.in("item", itemDipakai))
					.addOrder(org.hibernate.criterion.Order.asc("tanggalKadaluarsa")).list();
			for (Kadaluarsa k : batchAda) {
				if (k.getItem() != null && k.getItem().getId() != null
						&& !batchPertamaPerItem.containsKey(k.getItem().getId())) {
					batchPertamaPerItem.put(k.getItem().getId(), k);
				}
			}
		}

		int dibuat = 0;
		for (int i = 1; i <= JUMLAH_PENJUALAN_TERKENDALI_UAT; i++) {
			ApotikItemProfile p = profil.get((i - 1) % profil.size());
			ItemMedis item = p.getItem();
			Pasien pembeli = pasien.get((i - 1) % pasien.size());
			Dokter peresep = dokter.get((i - 1) % dokter.size());
			String golongan = i % 2 == 0 ? ApotikItemProfile.GOLONGAN_PSIKOTROPIKA
					: ApotikItemProfile.GOLONGAN_NARKOTIKA;
			p.setGolonganObat(golongan);
			session.saveOrUpdate(p);
			Date waktu = tanggalKlinisSample(i);
			String kode = "TRX-APT-UAT-CTL-" + pad(i, 4);
			TransaksiMedis transaksi = transaksiPerKode.get(kode);
			boolean transaksiBaru = transaksi == null;
			if (transaksi == null) {
				transaksi = new TransaksiMedis();
				transaksi.setKode(kode);
			}
			transaksi.setPasien(pembeli);
			transaksi.setNama(pembeli.getNama());
			transaksi.setJenisKelamin(pembeli.getJenisKelamin());
			transaksi.setTanggalLahir(pembeli.getTanggalLahir());
			transaksi.setAlamat(pembeli.getAlamatLengkap());
			transaksi.setBebas(Boolean.TRUE);
			transaksi.setLunas(Boolean.TRUE);
			transaksi.setJenisTransaksi(TransaksiMedis.TRX_ITEM);
			transaksi.setSumber(TransaksiMedis.SUMBER_APOTIK);
			transaksi.setTanggalTransaksi(waktu);
			transaksi.setKeterangan("DATA SAMPLE/UAT — penjualan obat terkendali sintetis.");
			transaksi.setOleh("Kasir Apotik Demo");
			transaksi.setOlehId("seed_demo");
			session.saveOrUpdate(transaksi);
			transaksiPerKode.put(kode, transaksi);

			TransaksiMedisDetail detail = transaksiBaru ? null
					: detailPerTransaksi.get(transaksi.getId());
			boolean detailBaru = detail == null;
			double harga = item.getDefaultHargaJual() == null ? 0 : item.getDefaultHargaJual().doubleValue();
			if (detail == null) {
				detail = new TransaksiMedisDetail();
				detail.setTransaksi(transaksi);
				detail.setItem(item);
			}
			detail.setDokter(peresep);
			detail.setTanggal(waktu);
			detail.setTanggalTindakan(waktu);
			detail.setQty(Double.valueOf(1));
			detail.setAmount(Double.valueOf(harga));
			detail.setHasilPenghitunganTotal(Double.valueOf(harga));
			detail.setKeterangan("DATA SAMPLE/UAT — sesuai resep sintetis; verifikasi apoteker.");
			detail.setOleh("Kasir Apotik Demo");
			detail.setOlehId("seed_demo");
			session.saveOrUpdate(detail);
			if (transaksi.getId() != null) detailPerTransaksi.put(transaksi.getId(), detail);

			String penandaLedger = "PENJUALAN-TERKENDALI-SAMPLE/UAT-" + pad(i, 4);
			DetailTransaksiPasien ledger = ledgerPerDetail.get(detail.getId());
			if (ledger == null) {
				ledger = new DetailTransaksiPasien();
				ledger.setTransaksiDetail(detail);
				ledger.setKodeTransaksi(jual);
				ledger.setItem(item);
				ledger.setPasien(pembeli);
				ledger.setQty(Double.valueOf(1));
				ledger.setQtyBonus(Double.valueOf(0));
				ledger.setAmount(Double.valueOf(harga));
				ledger.setHasilPenghitunganTotal(Double.valueOf(harga));
				ledger.setTanggal(waktu);
				ledger.setPosting(Boolean.TRUE);
				ledger.setTanggalPosting(waktu);
				ledger.setLunas(Boolean.TRUE);
				ledger.setKeterangan(penandaLedger);
				ledger.setOleh("Kasir Apotik Demo");
				ledger.setOlehId("seed_demo");
				session.save(ledger);
				if (detail.getId() != null) ledgerPerDetail.put(detail.getId(), ledger);
			}

			Kadaluarsa batch = batchPertamaPerItem.get(item.getId());
			if (batch != null) {
				if (detailBaru || !konsumsiPerDetail.containsKey(detail.getId())) {
					ApotikBatchKonsumsi konsumsi = new ApotikBatchKonsumsi();
					konsumsi.setKadaluarsa(batch);
					konsumsi.setTransaksiDetail(detail);
					konsumsi.setQty(Double.valueOf(1));
					konsumsi.setWaktu(waktu);
					konsumsi.setOleh("Kasir Apotik Demo");
					konsumsi.setOlehId("seed_demo");
					session.save(konsumsi);
					if (detail.getId() != null) konsumsiPerDetail.put(detail.getId(), konsumsi);
				}
			}

			ApotikNarkotikaLog log = logPerDetail.get(detail.getId());
			if (log == null) {
				log = new ApotikNarkotikaLog();
				log.setTransaksiDetail(detail);
				log.setItem(item);
				dibuat++;
				if (detail.getId() != null) logPerDetail.put(detail.getId(), log);
			}
			log.setQty(Double.valueOf(1));
			log.setGolonganObat(golongan);
			log.setNamaPembeli(pembeli.getNama());
			log.setAlamatPembeli(pembeli.getAlamatLengkap());
			log.setNamaDokter(peresep.getNama());
			log.setKeterangan("DATA SAMPLE/UAT — register sintetis, bukan transaksi pasien nyata.");
			log.setWaktu(waktu);
			log.setOleh("Kasir Apotik Demo");
			log.setOlehId("seed_demo");
			session.saveOrUpdate(log);

			ApotikPembayaranTransaksi pembayaran = transaksiBaru ? null
					: pembayaranPerTransaksi.get(transaksi.getId());
			if (pembayaran == null) {
				pembayaran = new ApotikPembayaranTransaksi();
				pembayaran.setTransaksi(transaksi);
				if (transaksi.getId() != null) pembayaranPerTransaksi.put(transaksi.getId(), pembayaran);
			}
			ais.database.model.koperasi.CaraPembayaranKoperasi metode = caraBayar.isEmpty()
					? null : caraBayar.get((i - 1) % caraBayar.size());
			pembayaran.setCaraBayar(metode);
			pembayaran.setNamaCaraBayar(metode == null ? "Tunai Sample/UAT" : metode.getNama());
			pembayaran.setNominal(Double.valueOf(harga));
			pembayaran.setTunai(metode != null && Boolean.TRUE.equals(metode.getAdaKembalian())
					? Double.valueOf(harga) : null);
			pembayaran.setKembalian(metode != null && Boolean.TRUE.equals(metode.getAdaKembalian())
					? Double.valueOf(0) : null);
			pembayaran.setReferensi("PAY-APT-UAT-" + pad(i, 4));
			pembayaran.setWaktu(waktu);
			pembayaran.setOleh("Kasir Apotik Demo");
			pembayaran.setOlehId("seed_demo");
			session.saveOrUpdate(pembayaran);
			if (i % 100 == 0) session.flush();
		}
		return dibuat;
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

	/**
	 * Isi ulang stok komponen SAMPLE/UAT bila sisa ledger atau batch layak turun
	 * di bawah ambang aman. Provisioning lama hanya membuat stok satu kali;
	 * setelah beberapa regression run, formula tetap ada tetapi tidak lagi dapat
	 * dijual. Selain bahan {@code DEMO-BHN-*}, versi lama pernah menautkan obat
	 * bootstrap ke resep demo sebelum katalog bahan tersedia. Karena itu cakupan
	 * diambil dari relasi yang benar-benar dipakai formula/resep SAMPLE/UAT, bukan
	 * hanya dari prefix kode bahan. Ledger item dan kuantitas batch selalu ditambah
	 * dengan nilai yang sama.
	 */
	@SuppressWarnings("unchecked")
	private static JSONObject ensureStokBahanFormulaUat(Session session) throws Exception {
		List<ItemMedis> bahanDemo = session.createQuery(
				"from ItemMedis i where i.kode like :kode order by i.kode")
				.setString("kode", "DEMO-BHN-%")
				.setMaxResults(JUMLAH_BAHAN_RACIKAN_DEMO).list();
		List<ItemMedis> komponenRacikan = session.createQuery(
				"select distinct d.item from RacikanDetail d where d.item is not null "
				+ "and d.racikan.kode like :kode")
				.setString("kode", "RAC-UAT-%").list();
		List<ItemMedis> komponenProduksi = session.createQuery(
				"select distinct b.item from BahanBakuItem b where b.item is not null "
				+ "and b.itemInduk.kode like :kode")
				.setString("kode", "DEMO-OBT-%").list();
		List<ItemMedis> itemResepMenunggu = session.createQuery(
				"select distinct d.item from ResepDetail d where d.item is not null "
				+ "and d.resep.kode like :kode and not exists "
				+ "(select tm.id from TransaksiMedis tm where tm.resep = d.resep)")
				.setString("kode", "RSP-DEMO-%").list();
		java.util.Map<Long, ItemMedis> unik = new java.util.LinkedHashMap<Long, ItemMedis>();
		tambahItemFormulaUat(unik, bahanDemo);
		tambahItemFormulaUat(unik, komponenRacikan);
		tambahItemFormulaUat(unik, komponenProduksi);
		tambahItemFormulaUat(unik, itemResepMenunggu);
		List<ItemMedis> items = new java.util.ArrayList<ItemMedis>(unik.values());
		JSONObject hasil = new JSONObject();
		hasil.put("targetPerBahan", STOK_MINIMAL_BAHAN_FORMULA_UAT);
		hasil.put("jumlahBahanDiperiksa", items.size());
		if (items.isEmpty()) {
			hasil.put("jumlahBahanDitopUp", 0);
			hasil.put("totalQtyDitopUp", 0);
			return hasil;
		}

		List<Long> itemIds = new java.util.ArrayList<Long>();
		for (ItemMedis item : items) itemIds.add(item.getId());
		Map<Long, Double> stokItem = ApotikApiHelper.stokPerItem(session, itemIds, null);

		List<Kadaluarsa> batches = session.createQuery(
				"from Kadaluarsa k where k.item.id in (:ids)")
				.setParameterList("ids", itemIds).list();
		Map<Long, Kadaluarsa> batchTopUpPerItem = new java.util.HashMap<Long, Kadaluarsa>();
		List<Long> batchIds = new java.util.ArrayList<Long>();
		for (Kadaluarsa batch : batches) {
			if (batch.getItem() == null || batch.getItem().getId() == null) continue;
			String penanda = "BATCH-TOPUP-UAT-" + batch.getItem().getId();
			if (penanda.equals(batch.getKeterangan()))
				batchTopUpPerItem.put(batch.getItem().getId(), batch);
			batchIds.add(batch.getId());
		}
		Map<Long, Double> konsumsiBatch = ApotikApiHelper.konsumsiPerBatch(session, batchIds);
		Map<Long, Double> sisaBatchPerItem = new java.util.HashMap<Long, Double>();
		Calendar hariIni = Calendar.getInstance();
		hariIni.set(Calendar.HOUR_OF_DAY, 0);
		hariIni.set(Calendar.MINUTE, 0);
		hariIni.set(Calendar.SECOND, 0);
		hariIni.set(Calendar.MILLISECOND, 0);
		for (Kadaluarsa batch : batches) {
			if (batch.getItem() == null || batch.getItem().getId() == null) continue;
			if (!Kadaluarsa.lotLayak(batch.getStatusLot())) continue;
			if (batch.getTanggalKadaluarsa() != null
					&& batch.getTanggalKadaluarsa().before(hariIni.getTime())) continue;
			double terpakai = konsumsiBatch.containsKey(batch.getId())
					? konsumsiBatch.get(batch.getId()).doubleValue() : 0;
			double sisa = Math.max(0, (batch.getQty() == null ? 0
					: batch.getQty().doubleValue()) - terpakai);
			Double lama = sisaBatchPerItem.get(batch.getItem().getId());
			sisaBatchPerItem.put(batch.getItem().getId(), Double.valueOf(
					(lama == null ? 0 : lama.doubleValue()) + sisa));
		}

		int jumlahDitopUp = 0;
		double totalDitopUp = 0;
		long penandaWaktu = System.currentTimeMillis();
		for (ItemMedis item : items) {
			double stok = stokItem.containsKey(item.getId())
					? stokItem.get(item.getId()).doubleValue() : 0;
			double sisaBatch = sisaBatchPerItem.containsKey(item.getId())
					? sisaBatchPerItem.get(item.getId()).doubleValue() : 0;
			double tambahan = Math.ceil(Math.max(
					STOK_MINIMAL_BAHAN_FORMULA_UAT - stok,
					STOK_MINIMAL_BAHAN_FORMULA_UAT - sisaBatch));
			if (tambahan <= 0) continue;

			Kadaluarsa batch = batchTopUpPerItem.get(item.getId());
			if (batch == null) {
				batch = new Kadaluarsa();
				batch.setItem(item);
				batch.setQty(Double.valueOf(0));
				batch.setKeterangan("BATCH-TOPUP-UAT-" + item.getId());
				batch.setOlehId("seed_demo");
				batch.setOleh("Provisioning data sample eBisnis");
				batchTopUpPerItem.put(item.getId(), batch);
			}
			Calendar kedaluwarsa = Calendar.getInstance();
			kedaluwarsa.add(Calendar.DAY_OF_YEAR, 3650);
			batch.setTanggalKadaluarsa(kedaluwarsa.getTime());
			batch.setStatusLot(Kadaluarsa.LOT_ELIGIBLE);

			DetailTransaksiPasien ledger = new DetailTransaksiPasien();
			ledger.setKodeTransaksi(ConstantValues.beliMasuk);
			ledger.setItem(item);
			ledger.setQty(Double.valueOf(tambahan));
			ledger.setQtyBonus(Double.valueOf(0));
			ledger.setAmount(item.getDefaultHargaBeli() == null
					? Double.valueOf(0) : item.getDefaultHargaBeli());
			ledger.setHasilPenghitunganTotal(Double.valueOf(tambahan
					* (item.getDefaultHargaBeli() == null ? 0 : item.getDefaultHargaBeli().doubleValue())));
			ledger.setTanggal(new Date());
			ledger.setKeterangan("STOK-TOPUP-UAT-" + item.getKode() + "-" + penandaWaktu);
			ledger.setOleh("seed_demo");
			ledger.setOlehId("seed_demo");
			session.save(ledger);

			batch.setQty(Double.valueOf((batch.getQty() == null ? 0 : batch.getQty().doubleValue())
					+ tambahan));
			session.saveOrUpdate(batch);
			jumlahDitopUp++;
			totalDitopUp += tambahan;
		}
		hasil.put("jumlahBahanDitopUp", jumlahDitopUp);
		hasil.put("totalQtyDitopUp", totalDitopUp);
		return hasil;
	}

	private static void tambahItemFormulaUat(Map<Long, ItemMedis> tujuan,
			List<ItemMedis> sumber) {
		for (ItemMedis item : sumber) {
			if (item == null || item.getId() == null) continue;
			tujuan.put(item.getId(), item);
		}
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
			long resepCampuranSiap = ((Number) session.createQuery("select count(distinct r) from Resep r "
					+ "where r.kode like 'RSP-DEMO-%' and not exists "
					+ "(select tm.id from TransaksiMedis tm where tm.resep = r) and exists "
					+ "(select dr.id from ResepDetail dr where dr.resep = r and dr.racikan is not null) and exists "
					+ "(select di.id from ResepDetail di where di.resep = r and di.item is not null)")
					.uniqueResult()).longValue();
			long resepKlinisLengkap = ((Number) session.createQuery("select count(r) from Resep r "
					+ "where r.kode like 'RSP-DEMO-%' and r.diagnosaPenyakit is not null "
					+ "and r.diagnosaPenyakit.pasien is not null and r.diagnosaPenyakit.dokter is not null "
					+ "and r.diagnosaPenyakit.pendaftaran is not null")
					.uniqueResult()).longValue();
			long penjualanTerkendali = ((Number) session.createQuery(
					"select count(l) from ApotikNarkotikaLog l where l.keterangan like :penanda")
					.setString("penanda", "DATA SAMPLE/UAT%").uniqueResult()).longValue();
			long pembayaranSample = ((Number) session.createQuery(
					"select count(p) from ApotikPembayaranTransaksi p where p.referensi like :penanda")
					.setString("penanda", "PAY-APT-UAT-%").uniqueResult()).longValue();
			long coldChain = ((Number) session.createQuery("select count(p) from ApotikItemProfile p "
					+ "where p.item.kode like 'DEMO-OBT-%' and p.coldChain = true")
					.uniqueResult()).longValue();
			long recall = ((Number) session.createQuery("select count(k) from Kadaluarsa k "
					+ "where k.keterangan like 'BATCH-DEMO-RECALL-%' and k.statusLot = :status")
					.setString("status", Kadaluarsa.LOT_RECALL).uniqueResult()).longValue();
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
			verifikasi.put("resepCampuranSiapTebus", resepCampuranSiap);
			verifikasi.put("resepKlinisLengkap", resepKlinisLengkap);
			verifikasi.put("penjualanTerkendali", penjualanTerkendali);
			verifikasi.put("pembayaranSample", pembayaranSample);
			verifikasi.put("itemColdChain", coldChain);
			verifikasi.put("batchRecall", recall);
			verifikasi.put("formulaRacikanOperasional", formulaRacikan);
			verifikasi.put("formulaProduksiOperasional", formulaProduksi);
			verifikasi.put("antreanHariIni", antrean);
			verifikasi.put("targetObatJadi", JUMLAH_OBAT_DEMO);
			verifikasi.put("targetBahanRacikan", JUMLAH_BAHAN_RACIKAN_DEMO);
			verifikasi.put("targetResepSiapJual", 500);
			verifikasi.put("targetResepCampuranSiapTebus", 100);
			verifikasi.put("targetResepKlinisLengkap", JUMLAH_PROFIL_RESEP_UAT);
			verifikasi.put("targetPenjualanTerkendali", JUMLAH_PENJUALAN_TERKENDALI_UAT);
			verifikasi.put("targetPembayaranSample", JUMLAH_PENJUALAN_TERKENDALI_UAT);
			verifikasi.put("targetItemColdChain", JUMLAH_COLD_CHAIN_UAT);
			verifikasi.put("targetBatchRecall", JUMLAH_RECALL_UAT);
			verifikasi.put("targetFormulaRacikanOperasional", JUMLAH_FORMULA_RACIKAN_UAT);
			verifikasi.put("targetFormulaProduksiOperasional", JUMLAH_FORMULA_PRODUKSI_UAT);
			verifikasi.put("targetAntrean", JUMLAH_ANTREAN_DEMO);
			verifikasi.put("lulus", obat >= JUMLAH_OBAT_DEMO
					&& bahan >= JUMLAH_BAHAN_RACIKAN_DEMO && resepSiap >= 500
					&& resepCampuranSiap >= 100
					&& resepKlinisLengkap >= JUMLAH_PROFIL_RESEP_UAT
					&& penjualanTerkendali >= JUMLAH_PENJUALAN_TERKENDALI_UAT
					&& pembayaranSample >= JUMLAH_PENJUALAN_TERKENDALI_UAT
					&& coldChain >= JUMLAH_COLD_CHAIN_UAT
					&& recall >= JUMLAH_RECALL_UAT
					&& formulaRacikan >= JUMLAH_FORMULA_RACIKAN_UAT
					&& formulaProduksi >= JUMLAH_FORMULA_PRODUKSI_UAT
					&& antrean >= JUMLAH_ANTREAN_DEMO);
			return verifikasi;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Menandai sedikitnya 100 item demo sebagai cold-chain. Penanda ini hanya
	 * memperkaya profil farmasi dan tidak mengubah saldo maupun kelayakan lot.
	 */
	@SuppressWarnings("unchecked")
	private static int ensureColdChainUat(Session session) {
		List<ApotikItemProfile> profil = session.createQuery(
				"from ApotikItemProfile p where p.item.kode like :kode order by p.item.kode")
				.setString("kode", "DEMO-OBT-%")
				.setMaxResults(JUMLAH_COLD_CHAIN_UAT).list();
		int diperbarui = 0;
		for (ApotikItemProfile p : profil) {
			if (!Boolean.TRUE.equals(p.getColdChain())) {
				p.setColdChain(Boolean.TRUE);
				p.setOleh("seed_demo");
				p.setOlehId("seed_demo");
				session.saveOrUpdate(p);
				diperbarui++;
			}
		}
		return diperbarui;
	}

	/**
	 * Membuat lot recall khusus UAT beserta ledger stok sumbernya. Lot ini sengaja
	 * berstatus tidak layak sehingga tidak pernah dipilih FEFO untuk penjualan;
	 * batch layak reguler pada item yang sama tetap tersedia untuk kasir.
	 */
	@SuppressWarnings("unchecked")
	private static int ensureRecallUat(Session session) {
		List<ItemMedis> items = session.createQuery(
				"from ItemMedis i where i.kode like :kode order by i.kode")
				.setString("kode", "DEMO-OBT-%")
				.setMaxResults(JUMLAH_RECALL_UAT).list();
		int dibuat = 0;
		int nomor = 0;
		for (ItemMedis item : items) {
			nomor++;
			String penanda = "BATCH-DEMO-RECALL-" + item.getKode();
			ensureStokRecallUat(session, item);
			Kadaluarsa batch = (Kadaluarsa) session.createCriteria(Kadaluarsa.class)
					.add(Restrictions.eq("item", item))
					.add(Restrictions.eq("keterangan", penanda))
					.setMaxResults(1).uniqueResult();
			if (batch == null) {
				batch = new Kadaluarsa();
				batch.setItem(item);
				batch.setQty(Double.valueOf(1));
				Calendar kalender = Calendar.getInstance();
				kalender.add(Calendar.DAY_OF_YEAR, 30 + nomor);
				batch.setTanggalKadaluarsa(kalender.getTime());
				batch.setKeterangan(penanda);
				batch.setOlehId("seed_demo");
				batch.setOleh("Provisioning data sample eBisnis");
				dibuat++;
			}
			batch.setStatusLot(Kadaluarsa.LOT_RECALL);
			session.saveOrUpdate(batch);
		}
		return dibuat;
	}

	private static void ensureStokRecallUat(Session session, ItemMedis item) {
		String penanda = "STOK-DEMO-RECALL-" + item.getKode();
		long ada = ((Number) session.createQuery(
				"select count(d) from DetailTransaksiPasien d where d.item = :item and d.keterangan = :penanda")
				.setParameter("item", item).setString("penanda", penanda)
				.uniqueResult()).longValue();
		if (ada > 0) return;
		DetailTransaksiPasien ledger = new DetailTransaksiPasien();
		ledger.setKodeTransaksi(ConstantValues.beliMasuk);
		ledger.setItem(item);
		ledger.setQty(Double.valueOf(1));
		ledger.setAmount(item.getDefaultHargaBeli() == null
				? Double.valueOf(0) : item.getDefaultHargaBeli());
		ledger.setHasilPenghitunganTotal(item.getDefaultHargaBeli() == null
				? Double.valueOf(0) : item.getDefaultHargaBeli());
		ledger.setTanggal(new Date());
		ledger.setKeterangan(penanda);
		ledger.setOleh("seed_demo");
		ledger.setOlehId("seed_demo");
		session.save(ledger);
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
	 * menautkan 500 resep demo. Ini yang dipakai endpoint kasir racikan dan UAT
	 * tebus resep campuran; seluruh isi adalah DATA SAMPLE, bukan formula klinis.
	 */
	@SuppressWarnings("unchecked")
	private static int ensureFormulaRacikanUat(Session session) {
		List<ItemMedis> bahan = session.createQuery(
				"from ItemMedis i where i.kode like :kode order by i.kode")
				.setString("kode", "DEMO-BHN-%").setMaxResults(JUMLAH_BAHAN_RACIKAN_DEMO).list();
		List<Resep> resep = session.createQuery(
				"from Resep r where r.kode like :kode and not exists "
				+ "(select tm.id from TransaksiMedis tm where tm.resep = r) order by r.kode")
				.setString("kode", "RSP-DEMO-%").setMaxResults(JUMLAH_FORMULA_RACIKAN_UAT).list();
		if (bahan.size() < 3) return 0;
		int dibuat = 0;
		for (int i = 1; i <= JUMLAH_FORMULA_RACIKAN_UAT; i++) {
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

	/** 500 formula produksi barang jadi, masing-masing memakai dua bahan demo. */
	@SuppressWarnings("unchecked")
	private static int ensureFormulaProduksiUat(Session session) {
		List<ItemMedis> hasil = session.createQuery(
				"from ItemMedis i where i.kode like :kode order by i.kode")
				.setString("kode", "DEMO-OBT-%").setMaxResults(JUMLAH_FORMULA_PRODUKSI_UAT).list();
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
