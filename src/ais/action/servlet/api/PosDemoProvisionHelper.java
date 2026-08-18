package ais.action.servlet.api;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ProdukContohKatalog;
import ais.common.UnitUsahaKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.JenisProduk;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;
import ais.database.model.library.Penyedia;

/**
 * Provisioning beban POS untuk toko demo. Seluruh aksi wajib melewati tiga gerbang:
 * konfigurasi data_sample_ebisnis, administrator resmi, dan toko.toko_demo=true.
 * Kode deterministik membuat proses idempoten walau tombol ditekan ulang/job terputus.
 */
public final class PosDemoProvisionHelper {
	private static final int TARGET_SUPPLIER = 1000;
	private static final int TARGET_JENIS_PRODUK = 600;
	private static final int TARGET_PRODUK = 50000;
	private static final int TARGET_TRANSAKSI = 200000;
	private static final int TARGET_PELANGGAN = 100000;
	/** Batas jumlah produk contoh PER unit usaha (permintaan bisnis: 250 s.d. 100.000). */
	private static final int UNIT_USAHA_MIN = 250;
	private static final int UNIT_USAHA_MAX = 100000;
	private static final int UKURAN_BATCH = 500;
	private static final Object LOCK = new Object();
	private static volatile boolean berjalan;
	private static volatile String tahap = "Siap";
	private static volatile int selesai;
	private static volatile int target;
	private static volatile String ringkasan = "Belum dijalankan";

	private PosDemoProvisionHelper() { }

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static Long tokoId(JSONObject request) {
		if (request == null) return null;
		Object v = request.has("toko_id") ? request.opt("toko_id") : request.opt("tokoId");
		if (v == null || JSONObject.NULL.equals(v) || String.valueOf(v).trim().length() == 0) return null;
		return Long.valueOf(String.valueOf(v).trim());
	}

	private static Toko validasi(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		if (!Common.bolehKonfigurasi(Konfigurasi.DATA_SAMPLE_EBISNIS, Konfigurasi.TIDAK_AKTIF)) {
			tolak(hasil, "Data sample dinonaktifkan. Aktifkan konfigurasi data_sample_ebisnis.");
			return null;
		}
		if (user == null || !Common.getApakahAdminLain(user)) {
			tolak(hasil, "Hanya administrator yang boleh membuat data beban.");
			return null;
		}
		Long id = tokoId(request);
		if (id == null) {
			tolak(hasil, "Pilih toko demo terlebih dahulu.");
			return null;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, id);
			if (toko == null || !Boolean.TRUE.equals(toko.getTokoDemo())) {
				tolak(hasil, "Toko tidak ditemukan atau belum ditandai sebagai toko demo.");
				return null;
			}
			return new Toko(toko.getId());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void status(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		Toko toko = validasi(user, request, hasil);
		if (toko == null) return;
		hasil.put("status", "00");
		hasil.put("berjalan", berjalan);
		hasil.put("tahap", tahap);
		hasil.put("selesai", selesai);
		hasil.put("target", target);
		hasil.put("ringkasan", ringkasan);
	}

	public static void mulaiProduk(final Tbmuser user, final JSONObject request, JSONObject hasil) throws Exception {
		mulai(user, request, hasil, "SEED-DEMO-PRODUK-50000", true);
	}

	public static void mulaiTransaksi(final Tbmuser user, final JSONObject request, JSONObject hasil) throws Exception {
		mulai(user, request, hasil, "SEED-DEMO-TRANSAKSI-200000", false);
	}

	/**
	 * Generate produk contoh BERDASARKAN unit usaha toko ({@link UnitUsahaKatalog}).
	 * Urutan resolusi unit usaha: (1) param {@code unit_usaha} (JSON array kode, dari popup
	 * checkbox klien), (2) kolom {@code Toko.unitUsahaJson}. Bila keduanya kosong, respons
	 * berisi {@code perlu_pilih_unit_usaha=true} + katalog lengkap supaya SEMUA kanal klien
	 * (Android/Desktop/JSP/ZK) menampilkan popup checkbox pemilihan unit usaha, lalu
	 * memanggil ulang aksi ini dengan pilihan pengguna.
	 *
	 * <p>Param {@code jumlah_per_unit} di-clamp {@link #UNIT_USAHA_MIN}..{@link #UNIT_USAHA_MAX}
	 * per unit usaha. Kode produk deterministik {@code DEMO-U-<KODE_UNIT>-<nomor>} menjadikan
	 * proses idempoten per unit -- aman diulang atau dilanjutkan setelah terputus.</p>
	 */
	public static void mulaiProdukUnitUsaha(final Tbmuser user, final JSONObject request,
			JSONObject hasil) throws Exception {
		final Toko toko = validasi(user, request, hasil);
		if (toko == null) return;
		final Set<String> unit = new LinkedHashSet<String>();
		JSONArray minta = request == null ? null : request.optJSONArray("unit_usaha");
		if (minta != null) {
			for (int i = 0; i < minta.length(); i++) {
				String kode = minta.optString(i, "").trim();
				if (UnitUsahaKatalog.dikenal(kode)) unit.add(kode);
			}
		}
		if (unit.isEmpty()) unit.addAll(bacaUnitUsahaToko(toko.getId()));
		if (unit.isEmpty()) {
			// Bukan kegagalan: toko belum memilih unit usaha, klien harus menampilkan
			// popup checkbox lalu memanggil ulang dengan param unit_usaha terisi.
			hasil.put("status", "00");
			hasil.put("perlu_pilih_unit_usaha", true);
			hasil.put("description",
					"Toko belum memiliki unit usaha. Pilih unit usaha yang produk contohnya akan diimpor.");
			JSONArray katalog = new JSONArray();
			for (UnitUsahaKatalog.Entri e : UnitUsahaKatalog.DAFTAR) {
				katalog.put(new JSONObject().put("kode", e.kode)
						.put("label", e.label).put("grup", e.grup));
			}
			hasil.put("katalog", katalog);
			return;
		}
		if (!"SEED-DEMO-PRODUK-UNIT-USAHA".equals(request.optString("konfirmasi", ""))) {
			tolak(hasil, "Konfirmasi provisioning tidak valid.");
			return;
		}
		int diminta = request.optInt("jumlah_per_unit", UNIT_USAHA_MIN);
		if (diminta < UNIT_USAHA_MIN) diminta = UNIT_USAHA_MIN;
		if (diminta > UNIT_USAHA_MAX) diminta = UNIT_USAHA_MAX;
		final int jumlahPerUnit = diminta;
		final int jumlahKelompok = 4;
		synchronized (LOCK) {
			if (berjalan) {
				hasil.put("status", "00");
				hasil.put("description", "Job data sample sedang berjalan: " + tahap);
				hasil.put("berjalan", true);
				return;
			}
			berjalan = true;
			tahap = "Produk contoh per unit usaha";
			selesai = 0;
			target = unit.size() * (jumlahKelompok + jumlahPerUnit);
			ringkasan = "Dimulai " + new Date();
		}
		Thread pekerja = new Thread(new Runnable() {
			@Override public void run() {
				try {
					int offset = 0;
					for (String kode : unit) {
						tahap = "Produk contoh " + UnitUsahaKatalog.labelDari(kode);
						seedJenisProdukUnit(kode);
						offset += jumlahKelompok;
						seedProdukUnit(toko.getId(), kode, jumlahPerUnit, offset);
						offset += jumlahPerUnit;
						selesai = offset;
					}
					tahap = "Produk contoh per unit usaha";
					ringkasan = tahap + " selesai: " + unit.size() + " unit usaha x "
							+ jumlahPerUnit + " produk.";
				} catch (Throwable e) {
					ringkasan = "GAGAL pada " + tahap + ": " + e.getMessage();
					try { ais.common.ErrorAuditUtil.record(e, "pos-demo-produk-unit-usaha-background"); }
					catch (Throwable abaikan) { abaikan.printStackTrace(); }
				} finally { berjalan = false; }
			}
		}, "AIS-Demo-Produk-UnitUsaha");
		pekerja.setDaemon(true);
		pekerja.start();
		hasil.put("status", "00");
		hasil.put("berjalan", true);
		hasil.put("unit_usaha_diproses", new JSONArray(unit));
		hasil.put("jumlah_per_unit", jumlahPerUnit);
		hasil.put("description", tahap + " dimulai di latar; halaman tetap dapat digunakan.");
	}

	/** Baca ulang pilihan unit usaha toko dari DB (validasi() hanya mengembalikan id). */
	private static Set<String> bacaUnitUsahaToko(Long tokoId) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Toko toko = (Toko) session.get(Toko.class, tokoId);
			return toko == null ? new LinkedHashSet<String>()
					: UnitUsahaKatalog.urai(toko.getUnitUsahaJson());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void mulaiPelanggan(final Tbmuser user, final JSONObject request, JSONObject hasil) throws Exception {
		final Toko toko = validasi(user, request, hasil);
		if (toko == null) return;
		if (!"SEED-DEMO-PELANGGAN-100000".equals(request.optString("konfirmasi", ""))) {
			tolak(hasil, "Konfirmasi provisioning pelanggan tidak valid.");
			return;
		}
		synchronized (LOCK) {
			if (berjalan) {
				hasil.put("status", "00");
				hasil.put("description", "Job data sample sedang berjalan: " + tahap);
				hasil.put("berjalan", true);
				return;
			}
			berjalan = true;
			tahap = "Pelanggan demo";
			selesai = 0;
			target = TARGET_PELANGGAN;
			ringkasan = "Dimulai " + new Date();
		}
		Thread pekerja = new Thread(new Runnable() {
			@Override public void run() {
				try {
					seedPelanggan();
					ringkasan = tahap + " selesai. Total target " + target + ".";
				} catch (Throwable e) {
					ringkasan = "GAGAL pada " + tahap + ": " + e.getMessage();
					try { ais.common.ErrorAuditUtil.record(e, "pos-demo-customer-background"); }
					catch (Throwable abaikan) { abaikan.printStackTrace(); }
				} finally { berjalan = false; }
			}
		}, "AIS-Demo-Pelanggan-100000");
		pekerja.setDaemon(true);
		pekerja.start();
		hasil.put("status", "00");
		hasil.put("berjalan", true);
		hasil.put("description", tahap + " dimulai di latar; halaman tetap dapat digunakan.");
	}

	private static void mulai(Tbmuser user, JSONObject request, JSONObject hasil,
			String token, final boolean produk) throws Exception {
		final Toko toko = validasi(user, request, hasil);
		if (toko == null) return;
		if (!token.equals(request.optString("konfirmasi", ""))) {
			tolak(hasil, "Konfirmasi provisioning tidak valid.");
			return;
		}
		synchronized (LOCK) {
			if (berjalan) {
				// Idempoten: penekanan ulang bukan kegagalan. Klien cukup diberi status
				// job yang sudah aktif dan tidak boleh memulai worker kedua.
				hasil.put("status", "00");
				hasil.put("description", "Job data sample sedang berjalan: " + tahap);
				hasil.put("berjalan", true);
				return;
			}
			berjalan = true;
			tahap = produk ? "Supplier dan produk demo" : "Transaksi demo";
			selesai = 0;
			target = produk ? TARGET_SUPPLIER + TARGET_JENIS_PRODUK + TARGET_PRODUK : TARGET_TRANSAKSI;
			ringkasan = "Dimulai " + new Date();
		}
		Thread pekerja = new Thread(new Runnable() {
			@Override public void run() {
				try {
					if (produk) {
						seedSupplier();
						seedJenisProduk();
						seedProduk(toko.getId());
					} else seedTransaksi(toko.getId());
					ringkasan = tahap + " selesai. Total target " + target + ".";
				} catch (Throwable e) {
					ringkasan = "GAGAL pada " + tahap + ": " + e.getMessage();
					try { ais.common.ErrorAuditUtil.record(e, "pos-demo-provision-background"); }
					catch (Throwable abaikan) { abaikan.printStackTrace(); }
				} finally { berjalan = false; }
			}
		}, produk ? "AIS-Demo-Produk-50000" : "AIS-Demo-Transaksi-200000");
		pekerja.setDaemon(true);
		pekerja.start();
		hasil.put("status", "00");
		hasil.put("berjalan", true);
		hasil.put("description", tahap + " dimulai di latar; halaman tetap dapat digunakan.");
	}

	/**
	 * Supplier bersifat master global pada skema library, sehingga kode DEMO yang
	 * deterministik dipakai sebagai kunci idempotensi. Data nyata tidak disentuh.
	 */
	@SuppressWarnings("unchecked")
	private static void seedSupplier() throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			List<String> daftar = session.createQuery("select p.kode from Penyedia p where p.kode like :prefix")
					.setString("prefix", "DEMO-SUP-%").list();
			Set<String> ada = new HashSet<String>(daftar);
			tx = session.beginTransaction();
			int sejakCommit = 0;
			for (int i = 1; i <= TARGET_SUPPLIER; i++) {
				String kode = "DEMO-SUP-" + pad(i, 4);
				if (!ada.contains(kode)) {
					Penyedia p = new Penyedia();
					p.setKode(kode);
					p.setNama("Supplier Demo " + pad(i, 4));
					p.setKontak("PIC Supplier " + pad(i, 4));
					p.setTelp("08" + pad(100000000 + i, 9));
					p.setEmail("supplier" + pad(i, 4) + "@demo.ebisnis.id");
					p.setAlamat("Alamat supplier demo nomor " + i);
					p.setKodePos(pad(10000 + (i % 89999), 5));
					p.setKeterangan("Data sample eBisnis POS untuk uji performa");
					session.save(p);
					sejakCommit++;
				}
				selesai = i;
				if (sejakCommit >= UKURAN_BATCH) {
					tx.commit(); session.clear(); tx = session.beginTransaction(); sejakCommit = 0;
				}
			}
			if (tx.isActive()) tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			throw e;
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	@SuppressWarnings("unchecked")
	private static void seedJenisProduk() throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			List<JenisProduk> daftar = session.createQuery(
					"from JenisProduk j where j.keterangan like :prefix")
					.setString("prefix", "DEMO-JP-%").list();
			Set<String> ada = new HashSet<String>();
			tx = session.beginTransaction();
			int sejakCommit = 0;
			for (JenisProduk lama : daftar) {
				String penanda = lama.getKeterangan();
				ada.add(penanda);
				int nomor = nomorDariKode(penanda, "DEMO-JP-");
				if (nomor > 0 && !namaJenisProdukMinimarket(nomor).equals(lama.getNama())) {
					lama.setNama(namaJenisProdukMinimarket(nomor));
					lama.setAktif(Boolean.TRUE);
					session.update(lama);
					sejakCommit++;
				}
			}
			for (int i = 1; i <= TARGET_JENIS_PRODUK; i++) {
				String penanda = "DEMO-JP-" + pad(i, 4);
				if (!ada.contains(penanda)) {
					JenisProduk jenis = new JenisProduk();
					jenis.setNama(namaJenisProdukMinimarket(i));
					jenis.setKeterangan(penanda);
					jenis.setAktif(Boolean.TRUE);
					jenis.setDefaultProduk(Boolean.FALSE);
					jenis.setMaksimalHarian(Double.valueOf(100000000.0));
					session.save(jenis);
					sejakCommit++;
				}
				selesai = TARGET_SUPPLIER + i;
				if (sejakCommit >= UKURAN_BATCH) {
					tx.commit(); session.clear(); tx = session.beginTransaction(); sejakCommit = 0;
				}
			}
			if (tx.isActive()) tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			throw e;
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	@SuppressWarnings("unchecked")
	private static void seedProduk(Long tokoId) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			List<Long> jenisIds = session.createQuery(
					"select j.id from JenisProduk j where j.keterangan like :prefix order by j.id")
					.setString("prefix", "DEMO-JP-%").setMaxResults(TARGET_JENIS_PRODUK).list();
			if (jenisIds.size() < TARGET_JENIS_PRODUK) {
				throw new IllegalStateException("Pembuatan 600 jenis produk demo belum lengkap.");
			}
			List<Produk> produkLama = session.createQuery("from Produk p where p.toko.id=:toko and p.kode like :prefix")
					.setLong("toko", tokoId.longValue()).setString("prefix", "DEMO-POS-%").list();
			Set<String> ada = new HashSet<String>();
			tx = session.beginTransaction();
			int sejakCommit = 0;
			// Migrasi data versi lama TANPA delete agar seluruh FK rincian transaksi tetap utuh.
			// Nama "Produk Uji Beban" diganti katalog minimarket yang representatif; kode
			// deterministik tetap menjadi kunci teknis idempotensi dan tidak tampil sebagai nama.
			for (Produk lama : produkLama) {
				String kodeLama = lama.getKode();
				ada.add(kodeLama);
				int nomor = nomorDariKode(kodeLama, "DEMO-POS-");
				if (nomor > 0) {
					String namaBaru = namaProdukMinimarket(nomor);
					Long jenisId = jenisIds.get((nomor - 1) % jenisIds.size());
					if (!namaBaru.equals(lama.getNama())
							|| !"Katalog contoh minimarket".equals(lama.getKeterangan())
							|| lama.getJenisProduk() == null
							|| !jenisId.equals(lama.getJenisProduk().getId())) {
						lama.setNama(namaBaru);
						lama.setKeterangan("Katalog contoh minimarket");
						lama.setJenisProduk((JenisProduk) session.load(JenisProduk.class, jenisId));
						session.update(lama);
						sejakCommit++;
					}
				}
				if (sejakCommit >= UKURAN_BATCH) {
					tx.commit(); session.clear(); tx = session.beginTransaction(); sejakCommit = 0;
				}
			}
			Toko toko = (Toko) session.load(Toko.class, tokoId);
			for (int i = 1; i <= TARGET_PRODUK; i++) {
				String kode = "DEMO-POS-" + pad(i, 6);
				if (!ada.contains(kode)) {
					double beli = 500.0 + ((i * 137) % 250000);
					double jual = Math.ceil((beli * (1.12 + ((i % 18) / 100.0))) / 100.0) * 100.0;
					Produk p = new Produk();
					p.setKode(kode);
					p.setBarcode("89988" + pad(i, 8));
					p.setNama(namaProdukMinimarket(i));
					p.setKeterangan("Katalog contoh minimarket");
					p.setJenisProduk((JenisProduk) session.load(JenisProduk.class,
							jenisIds.get((i - 1) % jenisIds.size())));
					p.setHargaBeli(Double.valueOf(beli));
					p.setHargaJual(Double.valueOf(jual));
					p.setStok(Double.valueOf(10 + (i % 990)));
					p.setStokMinimum(Double.valueOf(5 + (i % 25)));
					p.setAktif(Boolean.TRUE);
					p.setIzinkanJualMinusStok(Boolean.FALSE);
					p.setToko(toko);
					session.save(p);
					sejakCommit++;
				}
				selesai = TARGET_SUPPLIER + TARGET_JENIS_PRODUK + i;
				if (sejakCommit >= UKURAN_BATCH) {
					tx.commit(); session.clear(); tx = session.beginTransaction();
					toko = (Toko) session.load(Toko.class, tokoId); sejakCommit = 0;
				}
			}
			if (tx.isActive()) tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			throw e;
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	/**
	 * Jenis produk per unit usaha: 4 kelompok turunan grup katalog (mis. "Bengkel Mobil -
	 * Jasa"). Penanda idempotensi di keterangan: {@code DEMO-JPU-<KODE_UNIT>-<n>}.
	 */
	@SuppressWarnings("unchecked")
	private static void seedJenisProdukUnit(String kodeUnit) throws Exception {
		String label = UnitUsahaKatalog.labelDari(kodeUnit);
		String grup = grupDariKode(kodeUnit);
		String[] kelompok = ProdukContohKatalog.kelompokJenis(grup);
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			List<String> daftar = session.createQuery(
					"select j.keterangan from JenisProduk j where j.keterangan like :prefix")
					.setString("prefix", "DEMO-JPU-" + kodeUnit + "-%").list();
			Set<String> ada = new HashSet<String>(daftar);
			tx = session.beginTransaction();
			for (int i = 1; i <= kelompok.length; i++) {
				String penanda = "DEMO-JPU-" + kodeUnit + "-" + i;
				if (!ada.contains(penanda)) {
					JenisProduk jenis = new JenisProduk();
					jenis.setNama(label + " - " + kelompok[i - 1]);
					jenis.setKeterangan(penanda);
					jenis.setAktif(Boolean.TRUE);
					jenis.setDefaultProduk(Boolean.FALSE);
					jenis.setMaksimalHarian(Double.valueOf(100000000.0));
					session.save(jenis);
				}
			}
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			throw e;
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	/**
	 * Produk contoh satu unit usaha. Kode {@code DEMO-U-<KODE_UNIT>-<nomor>} deterministik =
	 * kunci idempotensi per unit; barcode dibedakan lewat indeks unit di katalog sehingga
	 * tidak bentrok antar-unit maupun dengan seed minimarket lama (prefix 89988).
	 */
	@SuppressWarnings("unchecked")
	private static void seedProdukUnit(Long tokoId, String kodeUnit, int jumlah,
			int offsetProgres) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			List<Long> jenisIds = session.createQuery(
					"select j.id from JenisProduk j where j.keterangan like :prefix order by j.id")
					.setString("prefix", "DEMO-JPU-" + kodeUnit + "-%").list();
			if (jenisIds.isEmpty()) {
				throw new IllegalStateException("Jenis produk unit usaha " + kodeUnit + " belum dibuat.");
			}
			List<String> daftar = session.createQuery(
					"select p.kode from Produk p where p.toko.id=:toko and p.kode like :prefix")
					.setLong("toko", tokoId.longValue())
					.setString("prefix", "DEMO-U-" + kodeUnit + "-%").list();
			Set<String> ada = new HashSet<String>(daftar);
			String keterangan = "Katalog contoh " + UnitUsahaKatalog.labelDari(kodeUnit);
			int indeksUnit = indeksUnit(kodeUnit);
			tx = session.beginTransaction();
			Toko toko = (Toko) session.load(Toko.class, tokoId);
			int sejakCommit = 0;
			for (int i = 1; i <= jumlah; i++) {
				String kode = "DEMO-U-" + kodeUnit + "-" + pad(i, 6);
				if (!ada.contains(kode)) {
					double beli = ProdukContohKatalog.hargaBeli(kodeUnit, i);
					double jual = Math.ceil((beli * (1.12 + ((i % 18) / 100.0))) / 100.0) * 100.0;
					Produk p = new Produk();
					p.setKode(kode);
					p.setBarcode("8990" + pad(indeksUnit, 2) + pad(i, 7));
					p.setNama(ProdukContohKatalog.namaProduk(kodeUnit, i));
					p.setKeterangan(keterangan);
					p.setJenisProduk((JenisProduk) session.load(JenisProduk.class,
							jenisIds.get((i - 1) % jenisIds.size())));
					p.setHargaBeli(Double.valueOf(beli));
					p.setHargaJual(Double.valueOf(jual));
					p.setStok(Double.valueOf(10 + (i % 990)));
					p.setStokMinimum(Double.valueOf(5 + (i % 25)));
					p.setAktif(Boolean.TRUE);
					p.setIzinkanJualMinusStok(Boolean.FALSE);
					p.setToko(toko);
					session.save(p);
					sejakCommit++;
				}
				selesai = offsetProgres + i;
				if (sejakCommit >= UKURAN_BATCH) {
					tx.commit(); session.clear(); tx = session.beginTransaction();
					toko = (Toko) session.load(Toko.class, tokoId); sejakCommit = 0;
				}
			}
			if (tx.isActive()) tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			throw e;
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	private static String grupDariKode(String kodeUnit) {
		for (UnitUsahaKatalog.Entri e : UnitUsahaKatalog.DAFTAR) {
			if (e.kode.equals(kodeUnit)) return e.grup;
		}
		return UnitUsahaKatalog.GRUP_LAINNYA;
	}

	/** Posisi kode di katalog (0-based) -- dipakai membedakan blok barcode antar-unit. */
	private static int indeksUnit(String kodeUnit) {
		for (int i = 0; i < UnitUsahaKatalog.DAFTAR.size(); i++) {
			if (UnitUsahaKatalog.DAFTAR.get(i).kode.equals(kodeUnit)) return i;
		}
		return 99;
	}

	private static int nomorDariKode(String kode, String prefix) {
		if (kode == null || !kode.startsWith(prefix)) return -1;
		try { return Integer.parseInt(kode.substring(prefix.length())); }
		catch (NumberFormatException e) { return -1; }
	}

	/**
	 * Nama produk sengaja dibangun dari barang yang lazim dijual minimarket. Kombinasi
	 * produk, kemasan, dan isi menghasilkan puluhan ribu SKU yang tetap tampak alami,
	 * tanpa label "uji", "beban", atau nomor dummy pada nama yang dilihat pengguna.
	 */
	private static String namaProdukMinimarket(int nomor) {
		String[] produk = {
				"Aqua Air Mineral", "Le Minerale Air Mineral", "Indomie Mi Goreng",
				"Mie Sedaap Goreng", "Ultra Milk Susu UHT Cokelat", "Frisian Flag Susu UHT",
				"Teh Botol Sosro", "Pucuk Harum Teh Melati", "Kapal Api Kopi Bubuk",
				"Good Day Cappuccino", "Roma Biskuit Kelapa", "Khong Guan Crackers",
				"SilverQueen Cokelat Susu", "Beng-Beng Wafer Cokelat", "Chitato Sapi Panggang",
				"Qtela Keripik Singkong", "Taro Net Rumput Laut", "Sari Roti Tawar",
				"Morisca Roti Cokelat", "ABC Kecap Manis", "Sasa Tepung Bumbu",
				"Bimoli Minyak Goreng", "Sania Gula Kristal Putih", "Cap Rose Brand Tepung Beras",
				"Indofood Sambal Pedas", "BonCabe Sambal Tabur", "Sunlight Sabun Cuci Piring",
				"Rinso Deterjen Bubuk", "Molto Pelembut Pakaian", "So Klin Pewangi Pakaian",
				"Lifebuoy Sabun Mandi", "Lux Sabun Mandi", "Pepsodent Pasta Gigi",
				"Ciptadent Pasta Gigi", "Pantene Sampo", "Sunsilk Sampo",
				"Kleenex Tisu Wajah", "Paseo Tisu Toilet", "Charm Pembalut",
				"MamyPoko Popok Bayi", "Baygon Aerosol", "Hit Obat Nyamuk",
				"Panadol Tablet", "Tolak Angin Cair", "Antangin JRG",
				"Yakult Minuman Probiotik", "Cimory Yogurt Drink", "Bear Brand Susu Steril",
				"Floridina Orange", "Pocari Sweat"
		};
		String[] kemasan = {
				"Sachet", "Pouch", "Botol", "Kaleng", "Kotak", "Bungkus",
				"Cup", "Pack", "Refill", "Family Pack"
		};
		String[] ukuran = {
				"50 g", "75 g", "100 g", "125 g", "200 g", "250 g", "500 g",
				"1 kg", "200 ml", "250 ml", "330 ml", "500 ml", "600 ml", "1 liter"
		};
		int dasar = Math.max(0, nomor - 1);
		String nama = produk[dasar % produk.length] + " "
				+ ukuran[(dasar / produk.length) % ukuran.length] + " "
				+ kemasan[(dasar / (produk.length * ukuran.length)) % kemasan.length];
		int isi = 1 + ((dasar / (produk.length * ukuran.length * kemasan.length)) % 24);
		return isi == 1 ? nama : nama + " Isi " + isi;
	}

	private static String namaJenisProdukMinimarket(int nomor) {
		String[] kelompok = {
				"Minuman", "Makanan Pokok", "Makanan Instan", "Camilan", "Roti dan Kue",
				"Susu dan Olahan", "Bumbu Dapur", "Bahan Kue", "Produk Beku", "Buah dan Sayur",
				"Perawatan Tubuh", "Perawatan Rambut", "Perawatan Wajah", "Kesehatan",
				"Kebersihan Rumah", "Perawatan Pakaian", "Perlengkapan Bayi", "Perlengkapan Wanita",
				"Alat Tulis", "Peralatan Dapur", "Perlengkapan Rumah", "Elektronik Ringan",
				"Otomotif", "Hewan Peliharaan", "Produk Musiman"
		};
		String[] subKelompok = {
				"Air Mineral", "Teh Siap Minum", "Kopi dan Cokelat", "Jus Buah", "Minuman Isotonik",
				"Minuman Tradisional", "Beras dan Biji-bijian", "Mi dan Pasta", "Sereal Sarapan",
				"Biskuit dan Wafer", "Keripik dan Kacang", "Permen dan Cokelat", "Saus dan Kecap",
				"Minyak dan Margarin", "Sabun dan Pembersih", "Tisu dan Kapas", "Sampo dan Kondisioner",
				"Pasta Gigi dan Mulut", "Popok dan Perawatan", "Wadah dan Penyimpanan",
				"Baterai dan Lampu", "Aksesori dan Perkakas", "Higienis dan Sanitasi", "Paket Hemat"
		};
		int dasar = Math.max(0, nomor - 1);
		return kelompok[dasar % kelompok.length] + " - "
				+ subKelompok[(dasar / kelompok.length) % subKelompok.length];
	}

	/**
	 * Membuat pelanggan dengan nama Indonesia yang lazim dan kode teknis yang
	 * deterministik. Nama tidak diberi nomor/dummy marker supaya data UAT tetap
	 * menyerupai katalog pelanggan nyata; identitas DEMO hanya berada pada kode.
	 */
	@SuppressWarnings("unchecked")
	private static void seedPelanggan() throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			List<String> daftar = session.createQuery(
					"select a.kode from AnggotaKoperasi a where a.kode like :prefix")
					.setString("prefix", "DEMO-MBR-%").list();
			Set<String> ada = new HashSet<String>(daftar);
			tx = session.beginTransaction();
			int sejakCommit = 0;
			for (int i = 1; i <= TARGET_PELANGGAN; i++) {
				String kode = "DEMO-MBR-" + pad(i, 6);
				if (!ada.contains(kode)) {
					AnggotaKoperasi anggota = new AnggotaKoperasi();
					anggota.setKode(kode);
					anggota.setKodeIdentitas("MBR" + pad(i, 7));
					anggota.setNama(namaPelangganIndonesia(i));
					anggota.setAlamat(alamatPelangganIndonesia(i));
					String hp = "08" + pad(100000000 + (i % 900000000), 9);
					anggota.setHp(hp);
					anggota.setTelp(hp);
					anggota.setNomorHpNormalisasi("62" + hp.substring(1));
					anggota.setEmail("pelanggan" + pad(i, 6) + "@contoh.ebisnis.id");
					anggota.setAktif(Boolean.TRUE);
					anggota.setPihakTerkait(Boolean.FALSE);
					anggota.setLimitKredit(Double.valueOf(0.0));
					anggota.setKeterangan("Pelanggan contoh eBisnis POS");
					session.save(anggota);
					sejakCommit++;
				}
				selesai = i;
				if (sejakCommit >= UKURAN_BATCH) {
					tx.commit(); session.clear(); tx = session.beginTransaction(); sejakCommit = 0;
				}
			}
			if (tx.isActive()) tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			throw e;
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	private static String namaPelangganIndonesia(int nomor) {
		String[] depan = {
				"Ahmad", "Muhammad", "Abdul", "Agus", "Andi", "Arif", "Bambang", "Bayu",
				"Budi", "Dedi", "Dimas", "Eko", "Fajar", "Fauzi", "Hadi", "Hendra",
				"Irfan", "Joko", "Lukman", "Rizal", "Rudi", "Satria", "Taufik", "Wahyu",
				"Yusuf", "Aisyah", "Anisa", "Ayu", "Citra", "Dewi", "Dian", "Dinda",
				"Fitri", "Hana", "Indah", "Intan", "Laila", "Maya", "Nadia", "Nisa",
				"Nur", "Putri", "Rahma", "Rina", "Sari", "Siti", "Sri", "Tiara",
				"Wulan", "Zahra"
		};
		String[] tengah = {
				"Adi", "Akbar", "Amin", "Ananda", "Ari", "Bagus", "Cahyo", "Darmawan",
				"Fadli", "Firmansyah", "Gunawan", "Hakim", "Hidayat", "Kurnia", "Maulana",
				"Nugraha", "Pratama", "Ramadhan", "Saputra", "Setiawan", "Syahputra", "Wijaya",
				"Amelia", "Anggraini", "Aulia", "Cahyani", "Damayanti", "Febriani", "Kusuma",
				"Lestari", "Maharani", "Ningrum", "Oktaviani", "Permata", "Puspita", "Ramadhani",
				"Safitri", "Utami", "Wahyuni", "Yuliana"
		};
		String[] belakang = {
				"Abdullah", "Aditya", "Alamsyah", "Anwar", "Ardiansyah", "Budiman", "Darma",
				"Fahmi", "Fauzan", "Halim", "Hamzah", "Hasan", "Hermawan", "Ibrahim",
				"Iskandar", "Kurniawan", "Mahendra", "Nugroho", "Prasetyo", "Putra", "Rahman",
				"Ramli", "Santoso", "Setiawan", "Siregar", "Suharto", "Surya", "Syafii",
				"Wibowo", "Wijaya", "Aminah", "Andini", "Astuti", "Handayani", "Hasanah",
				"Hidayati", "Kurniasari", "Lestari", "Marlina", "Melati", "Ningsih", "Pertiwi",
				"Pratiwi", "Rahayu", "Rahmawati", "Salsabila", "Susanti", "Wulandari",
				"Yulianti", "Zulaikha"
		};
		int dasar = Math.max(0, nomor - 1);
		String namaDepan = depan[dasar % depan.length];
		String namaTengah = tengah[(dasar / depan.length) % tengah.length];
		String namaBelakang = belakang[(dasar / (depan.length * tengah.length)) % belakang.length];
		return namaDepan + " " + namaTengah + " " + namaBelakang;
	}

	private static String alamatPelangganIndonesia(int nomor) {
		String[] jalan = { "Mawar", "Melati", "Kenanga", "Anggrek", "Flamboyan", "Diponegoro",
				"Sudirman", "Ahmad Yani", "Merdeka", "Pemuda", "Pahlawan", "Cendana" };
		String[] kota = { "Jakarta", "Bandung", "Surabaya", "Semarang", "Yogyakarta", "Malang",
				"Bogor", "Bekasi", "Tangerang", "Depok", "Solo", "Cirebon", "Tasikmalaya",
				"Purwokerto", "Kediri", "Madiun", "Jember", "Sukabumi", "Serang", "Tegal" };
		int dasar = Math.max(0, nomor - 1);
		return "Jl. " + jalan[dasar % jalan.length] + " No. " + (1 + (dasar % 250))
				+ ", " + kota[(dasar / jalan.length) % kota.length];
	}

	@SuppressWarnings("unchecked")
	private static void seedTransaksi(Long tokoId) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			List<Long> produkIds = session.createQuery("select p.id from Produk p where p.toko.id=:toko and p.kode like :prefix order by p.id")
					.setLong("toko", tokoId.longValue()).setString("prefix", "DEMO-POS-%").setMaxResults(TARGET_PRODUK).list();
			if (produkIds.size() < TARGET_PRODUK) throw new IllegalStateException("Buat 50.000 produk demo terlebih dahulu.");
			List<String> daftar = session.createQuery("select p.kode from PembelianAnggotaKoperasi p where p.toko.id=:toko and p.kode like :prefix")
					.setLong("toko", tokoId.longValue()).setString("prefix", "DEMO-TRX-%").list();
			Set<String> ada = new HashSet<String>(daftar);
			tx = session.beginTransaction();
			Toko toko = (Toko) session.load(Toko.class, tokoId);
			int sejakCommit = 0;
			long sekarang = System.currentTimeMillis();
			for (int i = 1; i <= TARGET_TRANSAKSI; i++) {
				String kode = "DEMO-TRX-" + pad(i, 9);
				if (!ada.contains(kode)) {
					Date waktu = new Date(sekarang - ((long) (i % 365) * 86400000L) - ((long) (i % 1440) * 60000L));
					Long produkId = produkIds.get(i % produkIds.size());
					Produk produk = (Produk) session.load(Produk.class, produkId);
					double qty = 1 + (i % 5);
					double harga = produk.getHargaJual() == null ? 1000.0 : produk.getHargaJual().doubleValue();
					double total = qty * harga;
					PembelianAnggotaKoperasi master = new PembelianAnggotaKoperasi();
					master.setKode(kode); master.setKeterangan("Transaksi sample beban eBisnis POS");
					master.setTanggalPembayaran(waktu); master.setToko(toko);
					master.setKasirLoginNama("ADMIN DEMO"); master.setNamaMesin("DEMO LOAD TEST");
					master.setSumberTransaksi(PembelianAnggotaKoperasi.SUMBER_ONLINE);
					master.setBiaya(total); master.setTotalBiaya(total); master.setBayarTunai(total);
					master.setBayarNonTunai(0.0); master.setDiskon(0.0); master.setTotalDiskon(0.0);
					master.setPpn(0.0); master.setPajak(0.0); master.setKembalian(0.0); master.setRetur(0.0);
					master.setTotalCashback(0.0); session.save(master);
					Pembelian detail = new Pembelian();
					detail.setKode(kode + "-1"); detail.setProduk(produk); detail.setToko(toko);
					detail.setPembelianAnggotaKoperasi(master); detail.setWaktu(waktu);
					detail.setQty(Double.valueOf(qty)); detail.setHargaSatuan(Double.valueOf(harga));
					detail.setHargaJual(Double.valueOf(total)); detail.setDiskon(0.0);
					detail.setAktif(Boolean.TRUE); detail.setTerlayani(Boolean.TRUE); session.save(detail);
					sejakCommit++;
				}
				selesai = i;
				if (sejakCommit >= UKURAN_BATCH) {
					tx.commit(); session.clear(); tx = session.beginTransaction();
					toko = (Toko) session.load(Toko.class, tokoId); sejakCommit = 0;
				}
			}
			if (tx.isActive()) tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			throw e;
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	private static String pad(int value, int width) {
		String s = String.valueOf(value);
		StringBuilder b = new StringBuilder();
		for (int i = s.length(); i < width; i++) b.append('0');
		return b.append(s).toString();
	}
}
