package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Html;
import org.zkoss.zul.Timer;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.PenyediaAsset;
import ais.ui.util.DashboardUiKit;

/**
 * <h2>DasboardVendor &mdash; Papan Informasi Penyedia / Vendor</h2>
 *
 * <p><b>Untuk apa (bahasa awam):</b> menampilkan ringkasan keadaan seluruh penyedia/vendor dalam
 * satu layar: berapa banyak vendor, berapa yang masih aktif, sebarannya per status/kategori/jenis,
 * seberapa lengkap berkasnya (NPWP, akta, rekening, kontak), serta tren penambahan/pembaruan data
 * vendor dari bulan ke bulan. Tujuannya agar pengelola bisa langsung tahu "kondisi vendor kita
 * seperti apa hari ini" tanpa harus membuka data satu per satu.</p>
 *
 * <h3>Mengapa kelas ini dibuat &amp; bagaimana cara kerjanya</h3>
 * <p>Sebelumnya modul vendor hanya berupa daftar (grid) mentah; pengelola sulit melihat gambaran
 * besar. Kelas ini menambah satu papan informasi (dashboard) yang mengubah angka-angka mentah
 * menjadi kartu ringkas dan grafik yang mudah dibaca. Seluruh grafik (donat, batang, jaring
 * laba-laba/spider, tren garis, progress) dibuat memakai HTML + CSS modern lewat pustaka
 * {@link DashboardUiKit} &mdash; <b>tidak memakai JFreeChart</b> &mdash; sehingga ringan, konsisten,
 * dan otomatis ikut tema warna aplikasi.</p>
 *
 * <p>Alur kerja singkat:</p>
 * <ol>
 *   <li><b>Tampilkan progress dulu.</b> Saat papan dibuka pertama kali, kelas ini langsung
 *       menampilkan bilah kemajuan (progress bar) yang menyebutkan sedang memuat data apa dan
 *       sudah berapa persen. Bilah ini menarik dipandang (gradien warna tema) dan otomatis hilang
 *       setelah mencapai 100%.</li>
 *   <li><b>Ambil data secara bertahap di latar.</b> Pengambilan angka dilakukan lewat
 *       {@link Timer} ZK sehingga halaman tidak membeku. Pada langkah inti, satu sesi Hibernate
 *       dibuka via {@link HibernateUtil#getSessionFactory()} {@code .openSession()} dan
 *       <b>selalu</b> ditutup pada blok {@code finally} (clear &rarr; disconnect &rarr; close) agar
 *       tidak ada kebocoran koneksi. Sesi milik request/ZK ({@code currentSession}) sengaja
 *       tidak dipakai di sini agar penutupan eksplisit aman.</li>
 *   <li><b>Hitung yang efisien.</b> Demi hemat memori, perhitungan memakai query agregat
 *       (COUNT, GROUP BY, SUM-CASE) sehingga <b>entitas vendor tidak dimuat satu per satu</b> ke
 *       memori; yang diambil hanya angka hasil agregasi. Untuk tren bulanan hanya kolom tanggal
 *       yang diambil lalu dikelompokkan di Jawa.</li>
 *   <li><b>Render papan.</b> Setelah data siap, HTML dashboard dirakit sekali lalu disisipkan ke
 *       komponen induk; bilah progress dilepas.</li>
 * </ol>
 *
 * <h3>Panel yang ditampilkan</h3>
 * <ul>
 *   <li><b>Kartu ringkas</b> &mdash; total vendor, aktif, nonaktif, jumlah kategori, jumlah jenis.</li>
 *   <li><b>Donat status</b> &mdash; perbandingan vendor menurut status.</li>
 *   <li><b>Batang kategori &amp; jenis</b> &mdash; vendor terbanyak di kategori/jenis tertentu.</li>
 *   <li><b>Progress kelengkapan berkas</b> &mdash; persen vendor yang sudah mengisi NPWP, akta,
 *       rekening, email, telepon, alamat.</li>
 *   <li><b>Spider kelengkapan profil</b> &mdash; gambaran cepat dimensi data mana yang masih banyak
 *       kosong.</li>
 *   <li><b>Tren bulanan</b> &mdash; pergerakan penambahan/pembaruan data vendor 6 bulan terakhir.</li>
 * </ul>
 *
 * <h3>Pakai ulang &amp; pemeliharaan</h3>
 * <p>Kelas berdiri sendiri (stateless terhadap data; hanya menyimpan komponen induk &amp; bilah
 * loading) dan dipanggil cukup dengan {@link #init(Component)}. Mengikuti pola
 * {@code DasboardSop}. Untuk menambah panel baru, tambahkan query agregat kecil di
 * {@link #muatData()} dan satu pemanggilan {@link DashboardUiKit} di {@link #bangunHtml(DataVendor)}.
 * Semua deskripsi panel sengaja ditulis ringkas dan tanpa istilah teknis agar mudah dipahami
 * pengguna non-IT. Kompatibel Java 1.6/1.7 (tanpa lambda/stream/diamond), aman dipanggil dari ZK
 * event thread.</p>
 *
 * @author eCampus
 */
public final class DasboardVendor {

	/** Komponen induk tempat papan dirender (umumnya sebuah Tabpanel/Div). */
	private final Component induk;

	/** Elemen HTML untuk bilah progress; dilepas otomatis saat papan selesai dirender. */
	private Html loadingHtml;

	/** TTL cache HTML dashboard (10 menit) agar membuka halaman vendor terasa instan. */
	private static final int CACHE_TTL_MS = 10 * 60 * 1000;

	/**
	 * Kunci cache L3 papan vendor, DIPISAH per host agar data tidak bocor antar-tenant
	 * (aplikasi multi-tenant berbasis domain). Dipakai untuk menyimpan/membaca/membatalkan
	 * HTML dashboard di {@link ais.common.DashboardCacheUtil}.
	 *
	 * @return kunci unik per host, mis. {@code "DasboardVendor::kampus-a.ac.id"}.
	 */
	public static String kunciCache() {
		String host = "";
		try {
			host = ais.common.Common.getRequestHost();
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardVendor.java:103");
		}
		return "DasboardVendor::" + (host == null ? "" : host);
	}

	/** Membuat papan untuk komponen induk tertentu. Render dimulai lewat {@link #init(Component)}. */
	public DasboardVendor() {
		this.induk = null;
	}

	private DasboardVendor(Component induk) {
		this.induk = induk;
	}

	/**
	 * Titik masuk: tampilkan bilah progress lalu muat &amp; render papan vendor secara bertahap di
	 * latar (lewat {@link Timer}) sehingga halaman tidak membeku.
	 *
	 * @param parent komponen induk; bila {@code null} metode langsung berhenti.
	 */
	public void init(final Component parent) {
		if (parent == null) {
			return;
		}
		final DasboardVendor papan = new DasboardVendor(parent);
		try {
			Common.clear(parent);
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardVendor.java:130");
		}

		// Cache L3 per-host: jika papan sudah dihitung < TTL, tampilkan INSTAN tanpa loading.
		try {
			Object tersimpan = ais.common.DashboardCacheUtil.getL3(kunciCache());
			if (tersimpan instanceof String) {
				new Html((String) tersimpan).setParent(parent);
				return;
			}
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardVendor.java:140");
		}

		papan.loadingHtml = new Html(papan.htmlLoading("Menyiapkan papan informasi vendor...", 8));
		papan.loadingHtml.setParent(parent);

		final int[] langkah = new int[] { 0 };
		final DataVendor[] holder = new DataVendor[1];

		final Timer timer = new Timer(180);
		timer.setRepeats(true);
		timer.setParent(parent);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				langkah[0]++;
				try {
					if (langkah[0] == 1) {
						papan.updateLoading("Menghubungkan ke basis data vendor...", 22);
					} else if (langkah[0] == 2) {
						papan.updateLoading("Menghitung jumlah, status, kategori, dan kelengkapan...", 58);
						holder[0] = papan.muatData();
					} else if (langkah[0] == 3) {
						papan.updateLoading("Merapikan grafik, spider, dan tren bulanan...", 88);
					} else {
						papan.updateLoading("Selesai", 100);
						papan.render(holder[0] == null ? new DataVendor() : holder[0]);
						try {
							timer.stop();
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardVendor.java:169");
						}
						timer.detach();
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					try {
						timer.stop();
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardVendor.java:177");
					}
					timer.detach();
					papan.render(holder[0] == null ? new DataVendor() : holder[0]);
				}
			}
		});
		timer.start();
	}

	// ════════════════════════════════════════════════════════════════════════
	// Pengambilan data (agregat, hemat memori) — satu sesi, ditutup di finally
	// ════════════════════════════════════════════════════════════════════════

	/**
	 * Mengambil seluruh angka dashboard dalam satu sesi Hibernate memakai query agregat sehingga
	 * entitas tidak dimuat satu per satu (hemat memori). Sesi dibuka via {@code openSession()} dan
	 * <b>wajib</b> ditutup di {@code finally}.
	 *
	 * @return wadah {@link DataVendor} berisi semua angka; tidak pernah {@code null}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private DataVendor muatData() {
		DataVendor d = new DataVendor();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			d.total = ambilLong(session.createQuery("select count(p.id) from PenyediaAsset p").uniqueResult());
			d.aktif = ambilLong(session
					.createQuery("select count(p.id) from PenyediaAsset p where p.aktif = true").uniqueResult());
			d.nonaktif = Math.max(0, d.total - d.aktif);
			d.jumlahKategori = ambilLong(session
					.createQuery("select count(distinct p.kategoriPenyediaAsset.id) from PenyediaAsset p")
					.uniqueResult());
			d.jumlahJenis = ambilLong(session
					.createQuery("select count(distinct p.jenisPenyediaAsset.id) from PenyediaAsset p").uniqueResult());

			isiDistribusi(d.perStatus, session.createQuery(
					"select s.nama, count(p.id) from PenyediaAsset p left join p.statusPenyediaAsset s group by s.nama")
					.list());
			isiDistribusi(d.perKategori, session.createQuery(
					"select k.nama, count(p.id) from PenyediaAsset p left join p.kategoriPenyediaAsset k group by k.nama")
					.list());
			isiDistribusi(d.perJenis, session.createQuery(
					"select j.nama, count(p.id) from PenyediaAsset p left join p.jenisPenyediaAsset j group by j.nama")
					.list());

			// Status verifikasi berkas vendor (Belum Diverifikasi / Terverifikasi / Harus Direvisi).
			isiDistribusi(d.perStatusDokumen, session.createQuery(
					"select pd.status, count(pd.id) from PenyediaAssetPunyaDokumen pd group by pd.status").list(),
					ais.database.model.asset.PenyediaAssetPunyaDokumen.BELUM);

			// Pemenuhan dokumen WAJIB: vendor yang sudah mengunggah SEMUA dokumen wajib (yang aktif).
			// Catatan: kolom "aktif" pada DokumenPenyediaAsset bisa NULL (baris bawaan reloadDefault()
			// tidak pernah memanggil setAktif(...)); idiom di basis kode ini memperlakukan NULL sebagai
			// aktif (lihat DokumenPenyediaAsset.getAktif() dan DokumenPenyediaAssetAction.initCriteria()),
			// sehingga setiap query di sini WAJIB toleran NULL, bukan "dok.aktif = true" langsung.
			d.totalDokumenWajib = ambilLong(session.createQuery(
					"select count(dok.id) from DokumenPenyediaAsset dok where dok.wajib = true and (dok.aktif is null or dok.aktif = true)")
					.uniqueResult());
			if (d.totalDokumenWajib <= 0) {
				// Tidak ada dokumen wajib aktif yang terdaftar → kepatuhan TIDAK DAPAT DINILAI.
				// (Sebelumnya kode ini menganggap seluruh vendor "lengkap", positif palsu yang
				// menyesatkan karena tidak ada satu pun dokumen yang benar-benar diperiksa.)
				d.vendorLengkapWajib = 0;
				d.dokumenWajibTidakTerdaftar = true;
			} else {
				List idsLengkap = session.createQuery(
						"select pd.penyediaAsset.id from PenyediaAssetPunyaDokumen pd join pd.dokumenPenyediaAsset dok "
								+ "where dok.wajib = true and (dok.aktif is null or dok.aktif = true) group by pd.penyediaAsset.id "
								+ "having count(distinct pd.dokumenPenyediaAsset.id) >= " + d.totalDokumenWajib).list();
				d.vendorLengkapWajib = idsLengkap == null ? 0 : idsLengkap.size();

				List rowsUpload = session.createQuery("select dok.nama, count(distinct pd.penyediaAsset.id) "
						+ "from PenyediaAssetPunyaDokumen pd join pd.dokumenPenyediaAsset dok "
						+ "where dok.wajib = true and (dok.aktif is null or dok.aktif = true) group by dok.nama").list();
				List rowsWajib = session.createQuery(
						"select dok.nama from DokumenPenyediaAsset dok where dok.wajib = true and (dok.aktif is null or dok.aktif = true)")
						.list();
				isiDokumenWajibKurang(d, rowsWajib, rowsUpload);
			}

			// Masa berlaku dokumen: hitung yang sudah kedaluwarsa & yang akan kedaluwarsa (≤30 hari).
			java.util.Date sekarang = new java.util.Date();
			Calendar c30 = Calendar.getInstance();
			c30.add(Calendar.DAY_OF_MONTH, 30);
			d.dokumenKedaluwarsa = ambilLong(session.createQuery("select count(pd.id) from PenyediaAssetPunyaDokumen pd "
					+ "where pd.tanggalBerlakuSampai is not null and pd.tanggalBerlakuSampai < :now")
					.setParameter("now", sekarang).uniqueResult());
			d.dokumenSegeraKedaluwarsa = ambilLong(session.createQuery(
					"select count(pd.id) from PenyediaAssetPunyaDokumen pd "
							+ "where pd.tanggalBerlakuSampai >= :now and pd.tanggalBerlakuSampai <= :batas")
					.setParameter("now", sekarang).setParameter("batas", c30.getTime()).uniqueResult());

			// Kelengkapan berkas dalam SATU query (SUM-CASE) agar efisien.
			Object[] lengkap = (Object[]) session.createQuery("select count(p.id),"
					+ " sum(case when p.npwp is not null and p.npwp <> '' then 1 else 0 end),"
					+ " sum(case when p.noAktaPendirian is not null and p.noAktaPendirian <> '' then 1 else 0 end),"
					+ " sum(case when p.bank is not null and p.bank <> '' then 1 else 0 end),"
					+ " sum(case when p.email is not null and p.email <> '' then 1 else 0 end),"
					+ " sum(case when p.telp is not null and p.telp <> '' then 1 else 0 end),"
					+ " sum(case when p.alamat is not null and p.alamat <> '' then 1 else 0 end)"
					+ " from PenyediaAsset p").uniqueResult();
			if (lengkap != null) {
				long basis = ambilLong(lengkap[0]);
				d.npwpTerisi = ambilLong(lengkap[1]);
				d.aktaTerisi = ambilLong(lengkap[2]);
				d.rekeningTerisi = ambilLong(lengkap[3]);
				d.emailTerisi = ambilLong(lengkap[4]);
				d.telpTerisi = ambilLong(lengkap[5]);
				d.alamatTerisi = ambilLong(lengkap[6]);
				if (basis > 0) {
					d.total = Math.max(d.total, basis);
				}
			}

			// Tren bulanan: ambil HANYA kolom tanggal (proyeksi ringan) lalu kelompokkan di Java.
			List tanggals = session
					.createQuery("select p.tanggal_dirubah from PenyediaAsset p where p.tanggal_dirubah is not null")
					.list();
			isiTren(d, tanggals);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardVendor.java:299");
				}
				try {
					session.disconnect();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardVendor.java:303");
				}
				try {
					session.close();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardVendor.java:307");
				}
			}
		}
		return d;
	}

	/** Salin hasil {@code [nama, count]} ke peta distribusi (nama kosong/null &rarr; "Tidak diisi"). */
	@SuppressWarnings("rawtypes")
	private void isiDistribusi(LinkedHashMap<String, Double> target, List rows) {
		isiDistribusi(target, rows, "Tidak diisi");
	}

	/** Varian dengan label default kustom untuk baris yang nama/kategorinya null/kosong. */
	@SuppressWarnings("rawtypes")
	private void isiDistribusi(LinkedHashMap<String, Double> target, List rows, String defaultLabel) {
		if (rows == null) {
			return;
		}
		for (int i = 0; i < rows.size(); i++) {
			Object[] r = (Object[]) rows.get(i);
			String nama = r == null || r[0] == null ? defaultLabel : String.valueOf(r[0]).trim();
			if (nama.isEmpty()) {
				nama = defaultLabel;
			}
			double jml = r == null ? 0 : ambilLong(r[1]);
			Double lama = target.get(nama);
			target.put(nama, (lama == null ? 0 : lama) + jml);
		}
	}

	/**
	 * Menghitung, untuk SETIAP dokumen wajib aktif, berapa vendor yang BELUM mengunggahnya
	 * (kekurangan = total vendor &minus; jumlah vendor yang sudah mengunggah). Dokumen yang sama
	 * sekali belum diunggah siapa pun tetap terdaftar karena diambil dari daftar dokumen wajib
	 * lengkap, bukan dari data unggahan.
	 *
	 * @param d          wadah data tujuan ({@code dokumenWajibKurang} diisi nama&rarr;jumlah kurang)
	 * @param rowsWajib  daftar nama seluruh dokumen wajib aktif
	 * @param rowsUpload baris {@code [namaDokumen, jumlahVendorMengunggah]}
	 */
	@SuppressWarnings("rawtypes")
	private void isiDokumenWajibKurang(DataVendor d, List rowsWajib, List rowsUpload) {
		java.util.HashMap<String, Long> sudahUnggah = new java.util.HashMap<String, Long>();
		if (rowsUpload != null) {
			for (int i = 0; i < rowsUpload.size(); i++) {
				Object[] r = (Object[]) rowsUpload.get(i);
				if (r != null && r[0] != null) {
					sudahUnggah.put(String.valueOf(r[0]).trim(), Long.valueOf(ambilLong(r[1])));
				}
			}
		}
		if (rowsWajib != null) {
			for (int i = 0; i < rowsWajib.size(); i++) {
				Object o = rowsWajib.get(i);
				String nama = o == null ? "" : String.valueOf(o).trim();
				if (nama.isEmpty()) {
					nama = "Tanpa nama";
				}
				Long u = sudahUnggah.get(nama);
				long kurang = d.total - (u == null ? 0 : u.longValue());
				if (kurang < 0) {
					kurang = 0;
				}
				d.dokumenWajibKurang.put(nama, Double.valueOf(kurang));
			}
		}
	}

	/** Kelompokkan daftar tanggal menjadi 6 bulan terakhir (label "MMM yy" + jumlah per bulan). */
	@SuppressWarnings("rawtypes")
	private void isiTren(DataVendor d, List tanggals) {
		final int BULAN = 6;
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		// Mundur (BULAN-1) bulan dari bulan ini sebagai bulan awal.
		c.add(Calendar.MONTH, -(BULAN - 1));

		long[] hitung = new long[BULAN];
		int[] tahunBulanAwal = new int[] { c.get(Calendar.YEAR), c.get(Calendar.MONTH) };
		for (int i = 0; i < BULAN; i++) {
			Calendar cc = Calendar.getInstance();
			cc.set(Calendar.YEAR, tahunBulanAwal[0]);
			cc.set(Calendar.MONTH, tahunBulanAwal[1]);
			cc.add(Calendar.MONTH, i);
			d.trenLabel.add(labelBulan(cc));
		}

		if (tanggals != null) {
			Calendar t = Calendar.getInstance();
			for (int i = 0; i < tanggals.size(); i++) {
				Object o = tanggals.get(i);
				if (!(o instanceof Date)) {
					continue;
				}
				t.setTime((Date) o);
				int idx = (t.get(Calendar.YEAR) - tahunBulanAwal[0]) * 12
						+ (t.get(Calendar.MONTH) - tahunBulanAwal[1]);
				if (idx >= 0 && idx < BULAN) {
					hitung[idx]++;
				}
			}
		}
		for (int i = 0; i < BULAN; i++) {
			d.trenNilai.add(Long.valueOf(hitung[i]));
		}
	}

	private static String labelBulan(Calendar c) {
		String[] nama = { "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des" };
		int bulan = c.get(Calendar.MONTH);
		String thn = String.valueOf(c.get(Calendar.YEAR));
		return nama[bulan] + " " + thn.substring(thn.length() - 2);
	}

	// ════════════════════════════════════════════════════════════════════════
	// Render papan (HTML via DashboardUiKit)
	// ════════════════════════════════════════════════════════════════════════

	/** Rakit HTML papan lalu sisipkan ke induk; lepas bilah loading. */
	private void render(DataVendor d) {
		try {
			if (induk == null) {
				return;
			}
			if (loadingHtml != null) {
				try {
					loadingHtml.detach();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardVendor.java:439");
				}
				loadingHtml = null;
			}
			String html = bangunHtml(d);
			try {
				ais.common.DashboardCacheUtil.putL3(kunciCache(), html, CACHE_TTL_MS);
			} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardVendor.java:446");
			}
			Html papan = new Html(html);
			papan.setParent(induk);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Merakit seluruh HTML dashboard dari angka {@link DataVendor} memakai {@link DashboardUiKit}.
	 * Semua deskripsi panel ditulis ringkas dan tanpa istilah teknis.
	 */
	private String bangunHtml(DataVendor d) {
		StringBuilder sb = new StringBuilder(8192);

		sb.append(DashboardUiKit.introBanner("Papan Informasi Vendor / Penyedia",
				"Ringkasan keadaan semua vendor: jumlah, status, kategori, kelengkapan berkas, dan tren."));

		// Peringatan masa berlaku dokumen — hanya muncul bila ada yang sudah/akan kedaluwarsa.
		if (d.dokumenKedaluwarsa > 0 || d.dokumenSegeraKedaluwarsa > 0) {
			sb.append(bannerKedaluwarsa(d));
		}

		List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
		kartu.add(new DashboardUiKit.Stat("Total Vendor", String.valueOf(d.total), "Seluruh vendor terdaftar",
				"#2563eb"));
		kartu.add(new DashboardUiKit.Stat("Vendor Aktif", String.valueOf(d.aktif), "Masih bisa dipakai bertransaksi",
				"#16a34a"));
		kartu.add(new DashboardUiKit.Stat("Vendor Nonaktif", String.valueOf(d.nonaktif), "Dinonaktifkan / tidak dipakai",
				"#dc2626"));
		kartu.add(new DashboardUiKit.Stat("Kategori", String.valueOf(d.jumlahKategori), "Ragam kategori vendor",
				"#f59e0b"));
		kartu.add(new DashboardUiKit.Stat("Jenis", String.valueOf(d.jumlahJenis), "Ragam jenis vendor", "#7c3aed"));
		sb.append(DashboardUiKit.cards(kartu));

		sb.append(DashboardUiKit.openGrid(320));

		sb.append(DashboardUiKit.donut("Sebaran per Status",
				"Perbandingan jumlah vendor menurut status. Klik tetap di sini untuk lihat porsi terbesar.",
				toDoubleMap(d.perStatus), false, "Belum ada data status vendor."));

		sb.append(DashboardUiKit.barList("Vendor Terbanyak per Kategori",
				"Kategori yang paling banyak vendornya berada di urutan atas.", topN(d.perKategori, 8), "#2563eb",
				"vendor", false, "Belum ada data kategori vendor."));

		sb.append(DashboardUiKit.barList("Vendor Terbanyak per Jenis",
				"Jenis vendor yang paling sering muncul ada di urutan atas.", topN(d.perJenis, 8), "#0ea5e9", "vendor",
				false, "Belum ada data jenis vendor."));

		sb.append(DashboardUiKit.donut("Status Pemeriksaan Berkas Vendor",
				"Berapa banyak berkas vendor yang sudah diperiksa, masih menunggu diperiksa, atau harus diperbaiki.",
				toDoubleMap(d.perStatusDokumen), false, "Belum ada berkas vendor yang diunggah."));

		LinkedHashMap<String, Double> pemenuhanWajib = new LinkedHashMap<String, Double>();
		if (!d.dokumenWajibTidakTerdaftar) {
			pemenuhanWajib.put("Lengkap", (double) d.vendorLengkapWajib);
			pemenuhanWajib.put("Belum Lengkap", (double) Math.max(0, d.total - d.vendorLengkapWajib));
		}
		sb.append(DashboardUiKit.donut("Pemenuhan Dokumen Wajib",
				"Berapa banyak vendor yang sudah mengunggah SEMUA dokumen yang diwajibkan.",
				pemenuhanWajib, false,
				"Belum ada dokumen wajib aktif yang terdaftar — kepatuhan belum dapat dinilai."));

		sb.append(DashboardUiKit.barList("Dokumen Wajib Paling Sering Belum Diunggah",
				"Dokumen wajib yang paling banyak belum dipenuhi vendor ada di urutan atas — paling perlu ditagih.",
				topN(d.dokumenWajibKurang, 8), "#dc2626", "vendor", false,
				"Semua dokumen wajib sudah dipenuhi, atau belum ada dokumen wajib."));

		sb.append(DashboardUiKit.progressLines("Kelengkapan Berkas Vendor",
				"Persen vendor yang sudah mengisi berkas penting. Makin penuh batangnya, makin lengkap datanya.",
				kelengkapanPersen(d)));

		sb.append(DashboardUiKit.spider("Peta Kelengkapan Profil",
				"Sudut yang pendek menandakan data di sisi itu masih banyak yang kosong dan perlu dilengkapi.",
				new String[] { "NPWP", "Akta", "Rekening", "Email", "Telepon", "Alamat" },
				new int[] { persen(d.npwpTerisi, d.total), persen(d.aktaTerisi, d.total),
						persen(d.rekeningTerisi, d.total), persen(d.emailTerisi, d.total),
						persen(d.telpTerisi, d.total), persen(d.alamatTerisi, d.total) }));

		sb.append(DashboardUiKit.sparkline("Tren Data Vendor (6 Bulan)",
				"Pergerakan penambahan/pembaruan data vendor tiap bulan. Garis naik berarti aktivitas meningkat.",
				d.trenNilai, "#2563eb", "Belum ada data tren vendor."));

		sb.append(DashboardUiKit.closeGrid());

		LinkedHashMap<String, String> insight = new LinkedHashMap<String, String>();
		insight.put("Kategori terbanyak", namaTerbanyak(d.perKategori));
		insight.put("Jenis terbanyak", namaTerbanyak(d.perJenis));
		insight.put("Berkas paling perlu dilengkapi", berkasPalingKosong(d));
		insight.put("Berkas menunggu diperiksa", jumlahMenungguVerifikasi(d) + " berkas");
		insight.put("Vendor belum lengkap dokumen wajib", d.dokumenWajibTidakTerdaftar
				? "Tidak dapat dinilai (belum ada dokumen wajib aktif terdaftar)"
				: Math.max(0, d.total - d.vendorLengkapWajib) + " vendor");
		insight.put("Dokumen sudah kedaluwarsa", d.dokumenKedaluwarsa + " berkas");
		insight.put("Dokumen akan kedaluwarsa (≤30 hari)", d.dokumenSegeraKedaluwarsa + " berkas");
		sb.append(DashboardUiKit.insight("Ringkasan Cepat", "Hal-hal yang paling perlu diperhatikan saat ini.",
				insight));

		return sb.toString();
	}

	// ── Util kecil ──────────────────────────────────────────────────────────

	private static long ambilLong(Object o) {
		if (o instanceof Number) {
			return ((Number) o).longValue();
		}
		try {
			return o == null ? 0 : Long.parseLong(String.valueOf(o).trim());
		} catch (Exception e) {
			return 0;
		}
	}

	private static int persen(long bagian, long total) {
		if (total <= 0) {
			return 0;
		}
		long p = Math.round((bagian * 100.0) / total);
		if (p < 0) {
			p = 0;
		}
		if (p > 100) {
			p = 100;
		}
		return (int) p;
	}

	private static LinkedHashMap<String, Double> toDoubleMap(LinkedHashMap<String, Double> src) {
		return src == null ? new LinkedHashMap<String, Double>() : src;
	}

	/** Ambil N teratas (nilai terbesar) dari peta distribusi. */
	private static LinkedHashMap<String, Double> topN(LinkedHashMap<String, Double> src, int n) {
		LinkedHashMap<String, Double> out = new LinkedHashMap<String, Double>();
		if (src == null || src.isEmpty()) {
			return out;
		}
		List<String> keys = new ArrayList<String>(src.keySet());
		// urut menurun by nilai (insertion sort sederhana, jumlah item sedikit)
		for (int i = 0; i < keys.size(); i++) {
			for (int j = i + 1; j < keys.size(); j++) {
				if (src.get(keys.get(j)) > src.get(keys.get(i))) {
					String tmp = keys.get(i);
					keys.set(i, keys.get(j));
					keys.set(j, tmp);
				}
			}
		}
		int batas = Math.min(n, keys.size());
		for (int i = 0; i < batas; i++) {
			out.put(keys.get(i), src.get(keys.get(i)));
		}
		return out;
	}

	private LinkedHashMap<String, Integer> kelengkapanPersen(DataVendor d) {
		LinkedHashMap<String, Integer> m = new LinkedHashMap<String, Integer>();
		m.put("NPWP", Integer.valueOf(persen(d.npwpTerisi, d.total)));
		m.put("Akta Pendirian", Integer.valueOf(persen(d.aktaTerisi, d.total)));
		m.put("Rekening Bank", Integer.valueOf(persen(d.rekeningTerisi, d.total)));
		m.put("Email", Integer.valueOf(persen(d.emailTerisi, d.total)));
		m.put("Telepon", Integer.valueOf(persen(d.telpTerisi, d.total)));
		m.put("Alamat", Integer.valueOf(persen(d.alamatTerisi, d.total)));
		return m;
	}

	private static String namaTerbanyak(LinkedHashMap<String, Double> src) {
		if (src == null || src.isEmpty()) {
			return "-";
		}
		String nama = "-";
		double max = -1;
		for (String k : src.keySet()) {
			Double v = src.get(k);
			if (v != null && v > max && !"Tidak diisi".equals(k)) {
				max = v;
				nama = k;
			}
		}
		return nama + (max > 0 ? " (" + (long) max + ")" : "");
	}

	private String berkasPalingKosong(DataVendor d) {
		String[] nama = { "NPWP", "Akta Pendirian", "Rekening Bank", "Email", "Telepon", "Alamat" };
		int[] persen = { persen(d.npwpTerisi, d.total), persen(d.aktaTerisi, d.total),
				persen(d.rekeningTerisi, d.total), persen(d.emailTerisi, d.total), persen(d.telpTerisi, d.total),
				persen(d.alamatTerisi, d.total) };
		int min = Integer.MAX_VALUE;
		String pilih = "-";
		for (int i = 0; i < nama.length; i++) {
			if (persen[i] < min) {
				min = persen[i];
				pilih = nama[i];
			}
		}
		return d.total <= 0 ? "-" : pilih + " (baru " + min + "% terisi)";
	}

	/**
	 * Banner peringatan masa berlaku dokumen (warna merah lembut). Hanya dipanggil bila ada berkas
	 * yang sudah/akan kedaluwarsa, agar pengelola segera menindaklanjuti pembaruan dokumen vendor.
	 */
	private static String bannerKedaluwarsa(DataVendor d) {
		return "<div style='margin:0 0 12px;padding:12px 16px;border-radius:14px;border:1px solid #fecaca;"
				+ "background:#fef2f2;color:#7f1d1d;display:flex;align-items:center;gap:10px;flex-wrap:wrap;'>"
				+ "<span style='font-size:18px;'>&#9888;</span>"
				+ "<span style='font-weight:800;'>Perhatian masa berlaku dokumen:</span>"
				+ "<span><b>" + d.dokumenKedaluwarsa + "</b> berkas sudah kedaluwarsa &amp; <b>"
				+ d.dokumenSegeraKedaluwarsa + "</b> akan kedaluwarsa dalam 30 hari. "
				+ "Mohon minta vendor memperbarui dokumen terkait.</span></div>";
	}

	/** Jumlah berkas vendor yang belum tuntas diperiksa (Belum Diverifikasi + Harus Direvisi). */
	private static long jumlahMenungguVerifikasi(DataVendor d) {
		long n = 0;
		if (d.perStatusDokumen != null) {
			Double belum = d.perStatusDokumen.get(ais.database.model.asset.PenyediaAssetPunyaDokumen.BELUM);
			Double revisi = d.perStatusDokumen.get(ais.database.model.asset.PenyediaAssetPunyaDokumen.REVISI);
			n += belum == null ? 0 : belum.longValue();
			n += revisi == null ? 0 : revisi.longValue();
		}
		return n;
	}

	// ── Progress / loading ──────────────────────────────────────────────────

	private void updateLoading(String pesan, int persen) {
		try {
			if (loadingHtml != null) {
				loadingHtml.setContent(htmlLoading(pesan, persen));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/DasboardVendor.java:674");
		}
	}

	/** HTML bilah progress yang menarik (gradien tema) + spinner + persentase besar. */
	private String htmlLoading(String pesan, int persen) {
		if (persen < 0) {
			persen = 0;
		}
		if (persen > 100) {
			persen = 100;
		}
		String safe = DashboardUiKit.esc(pesan == null ? "Memuat data vendor..." : pesan);
		return "<div style=\"padding:22px;border-radius:18px;background:#ffffff;border:1px solid #e5e7eb;"
				+ "box-shadow:0 14px 32px rgba(15,23,42,.08);color:#0f172a;max-width:680px;margin:8px auto;\">"
				+ "<div style=\"display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;\">"
				+ "<div><div style=\"font-size:11px;letter-spacing:.12em;text-transform:uppercase;color:#2563eb;"
				+ "font-weight:900;\">Papan Informasi Vendor</div>"
				+ "<div style=\"font-size:18px;font-weight:900;margin-top:6px;\">"
				+ "<i class=\"fa fa-spinner fa-spin\"></i> Memuat Data Vendor</div>"
				+ "<div style=\"font-size:12px;color:#64748b;margin-top:8px;line-height:1.55;\">" + safe + "</div></div>"
				+ "<div style=\"min-width:86px;text-align:right;font-size:30px;font-weight:900;\">" + persen
				+ "%</div></div>"
				+ "<div style=\"height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:18px;\">"
				+ "<div style=\"height:12px;width:" + persen + "%;border-radius:999px;transition:width .3s ease;"
				+ "background:linear-gradient(90deg,var(--ais-theme-primary,#2563eb),var(--ais-theme-accent,#06b6d4));"
				+ "\"></div></div>"
				+ "<div style=\"display:flex;gap:8px;flex-wrap:wrap;margin-top:14px;font-size:11px;color:#475569;\">"
				+ chip("Ringkasan") + chip("Status &amp; Kategori") + chip("Kelengkapan") + chip("Tren")
				+ "</div></div>";
	}

	private static String chip(String teks) {
		return "<span style=\"padding:6px 10px;border-radius:999px;background:#eff6ff;color:#1d4ed8;font-weight:800;\">"
				+ teks + "</span>";
	}

	// ── Wadah data ──────────────────────────────────────────────────────────

	/** Kumpulan angka mentah hasil agregasi yang menjadi sumber seluruh panel. */
	private static final class DataVendor {
		long total;
		long aktif;
		long nonaktif;
		long jumlahKategori;
		long jumlahJenis;
		long npwpTerisi;
		long aktaTerisi;
		long rekeningTerisi;
		long emailTerisi;
		long telpTerisi;
		long alamatTerisi;
		final LinkedHashMap<String, Double> perStatus = new LinkedHashMap<String, Double>();
		final LinkedHashMap<String, Double> perKategori = new LinkedHashMap<String, Double>();
		final LinkedHashMap<String, Double> perJenis = new LinkedHashMap<String, Double>();
		final LinkedHashMap<String, Double> perStatusDokumen = new LinkedHashMap<String, Double>();
		final LinkedHashMap<String, Double> dokumenWajibKurang = new LinkedHashMap<String, Double>();
		long totalDokumenWajib;
		long vendorLengkapWajib;
		/** true bila tidak ada dokumen wajib AKTIF terdaftar → kepatuhan tidak dapat dinilai (lihat muatData()). */
		boolean dokumenWajibTidakTerdaftar;
		long dokumenKedaluwarsa;
		long dokumenSegeraKedaluwarsa;
		final List<String> trenLabel = new ArrayList<String>();
		final List<Number> trenNilai = new ArrayList<Number>();
	}
}
