package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Html;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyWindow;

/**
 * <h2>DasboardPerpustakaan — Papan Informasi Sirkulasi Perpustakaan (Peminjaman &amp; Pengembalian)</h2>
 *
 * <p><b>Untuk apa kelas ini:</b><br>
 * Kelas ini menampilkan sebuah "papan informasi" (dashboard) satu layar yang merangkum
 * seluruh kegiatan pinjam-meminjam koleksi perpustakaan menjadi angka besar dan grafik
 * yang mudah dibaca — tanpa perlu petugas membuka data satu per satu. Isinya menjawab
 * pertanyaan sehari-hari pengelola perpustakaan: berapa buku yang <i>sedang dipinjam</i>,
 * berapa yang <i>sudah kembali</i>, berapa yang <i>terlambat</i>, berapa total <i>denda</i>
 * dan berapa yang <i>belum lunas</i>, <i>buku apa yang paling laris dipinjam</i>,
 * <i>anggota siapa yang paling banyak menunggak denda</i>, serta <i>bagaimana tren
 * peminjaman naik-turun tiap bulan</i>. Papan ini dipanggil dari tombol "Dashboard" pada
 * layar Peminjaman ({@code peminjaman_pengadaan_item.zul}) dan Pengembalian
 * ({@code kembali_pengadaan_item.zul}).</p>
 *
 * <p><b>Bagaimana cara kerjanya (alur teknis):</b></p>
 * <ol>
 *   <li><b>{@link #buka()}</b> — membuka jendela modal, memasang wadah, lalu memanggil
 *       {@link #init(Component)}. Dipakai langsung dari atribut {@code onClick} pada ZUL
 *       sehingga <u>tidak perlu mengubah kelas Action peminjaman/pengembalian</u> (rendah
 *       risiko, maksimal <i>reuse</i>).</li>
 *   <li><b>{@link #init(Component)}</b> — menampilkan bilah "sedang memuat" lebih dulu agar
 *       layar terasa responsif, lalu memakai {@code Timer} ZK untuk memuat data di latar
 *       (langkah demi langkah) dan me-render hasilnya. UI tidak "membeku".</li>
 *   <li><b>{@link #muatData()}</b> — mengambil SEMUA angka dalam <u>satu</u> sesi Hibernate
 *       memakai query agregat ({@code count}/{@code sum}/{@code group by}) dan hanya
 *       memproyeksikan kolom yang diperlukan, sehingga entitas tidak dimuat satu-satu —
 *       <b>hemat memori</b>. Sesi dibuka via {@code openSession()} dan <b>wajib ditutup di
 *       blok {@code finally}</b> (clear &rarr; disconnect &rarr; close).</li>
 *   <li><b>{@link #render(DataPerpus)} / {@link #buildHtml(DataPerpus)}</b> — merangkai
 *       kartu KPI dan grafik (donat, batang, spider, tren) sebagai HTML/CSS modern melalui
 *       {@link ais.ui.util.DashboardUiKit} (bukan JFreeChart), lalu menyisipkannya ke wadah.
 *       Semua kartu grid otomatis turun-baris (<i>responsive</i>) di layar HP.</li>
 * </ol>
 *
 * <p><b>Panel yang ditampilkan &amp; kegunaannya bagi pengguna awam:</b></p>
 * <ul>
 *   <li><b>Kartu ringkasan</b> — total dipinjam, sedang dipinjam, sudah kembali, terlambat,
 *       jumlah anggota peminjam, total denda, dan denda belum lunas.</li>
 *   <li><b>Status Sirkulasi (donat)</b> — perbandingan item yang masih dipinjam, sudah
 *       kembali, dan yang telat.</li>
 *   <li><b>Buku Paling Sering Dipinjam (batang)</b> — judul favorit anggota, berguna untuk
 *       menambah eksemplar buku populer.</li>
 *   <li><b>Anggota dengan Denda Tertinggi (batang)</b> — siapa yang perlu ditagih.</li>
 *   <li><b>Tren Peminjaman per Bulan (batang kronologis)</b> — naik-turun aktivitas 12 bulan
 *       terakhir, berguna melihat musim ramai/sepi.</li>
 *   <li><b>Peminjaman per Perpustakaan (batang)</b> — unit mana yang paling ramai.</li>
 *   <li><b>Peta Aktivitas Perpustakaan (spider)</b> — hanya tampil bila ada &ge; 3 unit,
 *       memperlihatkan sebaran keramaian antar-perpustakaan.</li>
 *   <li><b>Ringkasan Cepat (insight)</b> — poin yang paling perlu diperhatikan hari ini.</li>
 * </ul>
 *
 * <p><b>Catatan pemeliharaan:</b> data sengaja diambil dengan query agregat agar ringan;
 * bila ingin menambah panel baru, cukup tambah field pada {@link DataPerpus}, isi di
 * {@link #muatData()}, dan render di {@link #buildHtml(DataPerpus)} memakai method
 * {@code DashboardUiKit.*}. Kode kompatibel Java 1.7 (tanpa lambda / <i>try-with-resources</i>)
 * dengan blok {@code try/catch} gaya Java 1.6 (tanpa <i>multi-catch</i>). Papan ini
 * di-render sesuai permintaan (on-demand) sehingga selalu menampilkan angka terbaru.</p>
 *
 * @see ais.ui.util.DashboardUiKit Kit grafik HTML/CSS (kartu, donat, batang, spider, tren).
 * @see ais.action.master.library.PeminjamanPengadaanItemAction Layar peminjaman.
 * @see ais.action.master.library.KembaliPengadaanItemAction Layar pengembalian.
 */
public class DasboardPerpustakaan {

	/** Nama bulan ringkas (Indonesia) agar label tren tidak tergantung locale server. */
	private static final String[] BULAN = { "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt",
			"Nov", "Des" };

	/** Wadah tempat papan digambar (Center/Vbox/Div dari pemanggil). */
	private Component induk;
	/** Bilah "sedang memuat" yang di-render lebih dulu lalu diganti papan final. */
	private Html loadingHtml;

	/** Konstruktor publik untuk pemanggilan {@code new DasboardPerpustakaan().init(...)}. */
	public DasboardPerpustakaan() {
	}

	/** Konstruktor internal yang menyimpan wadah target. */
	private DasboardPerpustakaan(Component induk) {
		this.induk = induk;
	}

	/**
	 * Membuka papan informasi perpustakaan dalam jendela modal. Cocok dipanggil langsung dari
	 * atribut {@code onClick} sebuah tombol ZUL, mis.
	 * {@code onClick="ais.action.master.library.helper.DasboardPerpustakaan.buka();"}.
	 */
	public static void buka() {
		try {
			MyWindow window = new MyWindow("Papan Informasi Perpustakaan", "normal", true);
			window.setWidth("94%");
			window.setHeight("92%");
			window.setContentStyle("overflow:auto;background:#eef2f7;");
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			Vbox wadah = new Vbox();
			wadah.setWidth("100%");
			wadah.setStyle("padding:6px;");
			wadah.setParent(window);

			// Muat SINKRON (query agregat cepat) lalu tampilkan — kokoh di semua konfigurasi
			// event-thread ZK. Untuk penyematan non-modal (mis. di tab), pakai init(parent).
			DasboardPerpustakaan papan = new DasboardPerpustakaan(wadah);
			papan.render(papan.muatData());

			try {
				window.onModal();
			} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/library/helper/DasboardPerpustakaan.java:133");
				// InterruptedException wajar terjadi saat modal ditutup — aman diabaikan.
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Merender papan ke {@code parent}: menampilkan bilah "memuat" lalu memuat data di latar
	 * (via {@code Timer}) dan menggambar grafiknya. Aman dipanggil dari mana pun asal
	 * {@code parent} sudah terpasang di pohon komponen ZK.
	 *
	 * @param parent wadah tujuan (boleh {@code null} &rarr; tidak melakukan apa-apa).
	 */
	public void init(final Component parent) {
		if (parent == null) {
			return;
		}
		final DasboardPerpustakaan papan = new DasboardPerpustakaan(parent);
		try {
			Common.clear(parent);
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/library/helper/DasboardPerpustakaan.java:155");
		}

		papan.loadingHtml = new Html(htmlLoading("Menyiapkan papan informasi perpustakaan...", 8));
		papan.loadingHtml.setParent(parent);

		final int[] langkah = new int[] { 0 };
		final DataPerpus[] holder = new DataPerpus[1];

		final Timer timer = new Timer(180);
		timer.setRepeats(true);
		timer.setParent(parent);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				langkah[0]++;
				try {
					if (langkah[0] == 1) {
						papan.updateLoading("Menghubungkan ke basis data perpustakaan...", 25);
					} else if (langkah[0] == 2) {
						papan.updateLoading("Menghitung peminjaman, pengembalian, denda, dan tren...", 62);
						holder[0] = papan.muatData();
					} else if (langkah[0] == 3) {
						papan.updateLoading("Merapikan grafik & tren bulanan...", 88);
					} else {
						papan.updateLoading("Selesai", 100);
						try {
							timer.stop();
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/library/helper/DasboardPerpustakaan.java:183");
						}
						try {
							timer.detach();
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/library/helper/DasboardPerpustakaan.java:187");
						}
						papan.render(holder[0] == null ? new DataPerpus() : holder[0]);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					try {
						timer.stop();
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/library/helper/DasboardPerpustakaan.java:195");
					}
					try {
						timer.detach();
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/library/helper/DasboardPerpustakaan.java:199");
					}
					papan.render(holder[0] == null ? new DataPerpus() : holder[0]);
				}
			}
		});
		timer.start();
	}

	// ════════════════════════════════════════════════════════════════════════
	// Pengambilan data — SATU sesi openSession(), agregat (hemat memori),
	// WAJIB ditutup di finally.
	// ════════════════════════════════════════════════════════════════════════

	/**
	 * Mengambil seluruh angka papan dalam satu sesi Hibernate memakai query agregat
	 * ({@code count}/{@code sum}/{@code group by} + {@code setMaxResults}) sehingga entitas
	 * tidak dimuat satu per satu. Sesi dibuka via {@code openSession()} dan ditutup di
	 * {@code finally}.
	 *
	 * @return wadah {@link DataPerpus} berisi semua angka; tidak pernah {@code null}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private DataPerpus muatData() {
		DataPerpus d = new DataPerpus();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			d.totalDipinjam = ambilLong(
					session.createQuery("select count(x.id) from PeminjamanPengadaanItemDetail x").uniqueResult());
			d.sedangDipinjam = ambilLong(session.createQuery(
					"select count(x.id) from PeminjamanPengadaanItemDetail x where x.kembaliPengadaanItemDetail is null")
					.uniqueResult());
			d.sudahKembali = Math.max(0L, d.totalDipinjam - d.sedangDipinjam);
			d.terlambat = ambilLong(session
					.createQuery("select count(x.id) from PeminjamanPengadaanItemDetail x "
							+ "where x.kembaliPengadaanItemDetail is null and x.batasWaktupengembalian is not null "
							+ "and x.batasWaktupengembalian < :now")
					.setParameter("now", ais.ui.util.WaktuUtil.getDate()).uniqueResult());
			d.anggotaAktif = ambilLong(session.createQuery(
					"select count(distinct x.peminjamanPengadaanItem.anggota.id) from PeminjamanPengadaanItemDetail x")
					.uniqueResult());

			d.totalDenda = ambilDouble(
					session.createQuery("select sum(k.denda) from KembaliPengadaanItemDetail k").uniqueResult());
			double totalDibayar = ambilDouble(session
					.createQuery("select sum(k.dibayarSejumlah) from KembaliPengadaanItemDetail k").uniqueResult());
			d.dendaBelumLunas = Math.max(0.0, d.totalDenda - totalDibayar);

			isiMap(d.topBuku, session.createQuery("select x.item.nama, count(x.id) from PeminjamanPengadaanItemDetail x "
					+ "where x.item.nama is not null group by x.item.nama order by count(x.id) desc").setMaxResults(10)
					.list());

			isiMap(d.topAnggotaDenda,
					session.createQuery("select a.nama, sum(k.denda) from KembaliPengadaanItemDetail k "
							+ "join k.peminjamanPengadaanItemDetail pd join pd.peminjamanPengadaanItem p join p.anggota a "
							+ "where k.denda > 0 group by a.nama order by sum(k.denda) desc").setMaxResults(10).list());

			isiMap(d.perPerpustakaan,
					session.createQuery("select pr.nama, count(x.id) from PeminjamanPengadaanItemDetail x "
							+ "join x.peminjamanPengadaanItem p join p.perpustakaan pr "
							+ "group by pr.nama order by count(x.id) desc").setMaxResults(10).list());

			Calendar awal = Calendar.getInstance();
			awal.add(Calendar.MONTH, -11);
			awal.set(Calendar.DAY_OF_MONTH, 1);
			awal.set(Calendar.HOUR_OF_DAY, 0);
			awal.set(Calendar.MINUTE, 0);
			awal.set(Calendar.SECOND, 0);
			awal.set(Calendar.MILLISECOND, 0);
			List tanggals = session
					.createQuery("select p.tanggalPembuatan from PeminjamanPengadaanItem p "
							+ "where p.tanggalPembuatan is not null and p.tanggalPembuatan >= :awal")
					.setParameter("awal", awal.getTime()).list();
			isiTren(d, tanggals);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/library/helper/DasboardPerpustakaan.java:282");
				}
				try {
					session.disconnect();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/library/helper/DasboardPerpustakaan.java:286");
				}
				try {
					session.close();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/library/helper/DasboardPerpustakaan.java:290");
				}
			}
		}
		return d;
	}

	/** Salin hasil {@code [nama, angka]} ke peta (nama kosong/null &rarr; "Tidak diketahui"). */
	private void isiMap(LinkedHashMap<String, Double> target, List<?> rows) {
		if (rows == null) {
			return;
		}
		for (Object o : rows) {
			if (!(o instanceof Object[])) {
				continue;
			}
			Object[] r = (Object[]) o;
			String nama = r.length > 0 && r[0] != null ? r[0].toString().trim() : "";
			if (nama.isEmpty()) {
				nama = "Tidak diketahui";
			}
			double val = r.length > 1 && r[1] instanceof Number ? ((Number) r[1]).doubleValue() : 0.0;
			Double cur = target.get(nama);
			target.put(nama, (cur == null ? 0.0 : cur) + val);
		}
	}

	/** Susun peta tren 12 bulan terakhir (urut kronologis) lalu isi jumlah per bulan. */
	private void isiTren(DataPerpus d, List<?> tanggals) {
		Calendar it = Calendar.getInstance();
		it.add(Calendar.MONTH, -11);
		it.set(Calendar.DAY_OF_MONTH, 1);
		HashMap<String, String> keyKeLabel = new HashMap<String, String>();
		for (int i = 0; i < 12; i++) {
			int m = it.get(Calendar.MONTH);
			int y = it.get(Calendar.YEAR);
			String key = y + "-" + m;
			String label = BULAN[m] + " " + String.format("%02d", y % 100);
			d.trenBulanan.put(label, 0.0);
			keyKeLabel.put(key, label);
			it.add(Calendar.MONTH, 1);
		}
		if (tanggals != null) {
			Calendar c = Calendar.getInstance();
			for (Object o : tanggals) {
				if (!(o instanceof Date)) {
					continue;
				}
				c.setTime((Date) o);
				String key = c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH);
				String label = keyKeLabel.get(key);
				if (label != null) {
					Double cur = d.trenBulanan.get(label);
					d.trenBulanan.put(label, (cur == null ? 0.0 : cur) + 1.0);
				}
			}
		}
	}

	// ════════════════════════════════════════════════════════════════════════
	// Render HTML/CSS (via DashboardUiKit — TANPA JFreeChart)
	// ════════════════════════════════════════════════════════════════════════

	/** Ganti bilah "memuat" dengan papan final berisi kartu &amp; grafik. */
	private void render(DataPerpus d) {
		if (induk == null) {
			return;
		}
		try {
			Common.clear(induk);
		} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/library/helper/DasboardPerpustakaan.java:360");
		}
		try {
			new Html(buildHtml(d)).setParent(induk);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Merangkai seluruh HTML papan (intro + kartu + grid grafik + ringkasan). */
	private String buildHtml(DataPerpus d) {
		StringBuilder sb = new StringBuilder(8192);

		sb.append(DashboardUiKit.introBanner("Papan Informasi Perpustakaan",
				"Ringkasan sirkulasi buku: yang sedang dipinjam, sudah kembali, terlambat, denda, buku favorit, "
						+ "sampai tren peminjaman tiap bulan."));

		List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
		kartu.add(new DashboardUiKit.Stat("Total Dipinjam", angka(d.totalDipinjam),
				"Seluruh item yang pernah dipinjam", "#2563eb"));
		kartu.add(new DashboardUiKit.Stat("Sedang Dipinjam", angka(d.sedangDipinjam),
				"Masih di tangan anggota", "#0ea5e9"));
		kartu.add(new DashboardUiKit.Stat("Sudah Kembali", angka(d.sudahKembali), "Sudah dikembalikan", "#22c55e"));
		kartu.add(new DashboardUiKit.Stat("Terlambat", angka(d.terlambat), "Lewat batas & belum kembali", "#ef4444"));
		kartu.add(new DashboardUiKit.Stat("Anggota Meminjam", angka(d.anggotaAktif),
				"Jumlah anggota yang berbeda", "#7c3aed"));
		kartu.add(new DashboardUiKit.Stat("Total Denda", DashboardUiKit.money(d.totalDenda),
				"Akumulasi denda keterlambatan", "#f59e0b"));
		kartu.add(new DashboardUiKit.Stat("Denda Belum Lunas", DashboardUiKit.money(d.dendaBelumLunas),
				"Masih harus ditagih", "#e11d48"));
		sb.append(DashboardUiKit.cards(kartu));

		sb.append(DashboardUiKit.openGrid(340));

		LinkedHashMap<String, Double> status = new LinkedHashMap<String, Double>();
		status.put("Sedang Dipinjam", (double) d.sedangDipinjam);
		status.put("Sudah Kembali", (double) d.sudahKembali);
		status.put("Terlambat", (double) d.terlambat);
		sb.append(DashboardUiKit.donut("Status Sirkulasi",
				"Perbandingan item yang masih dipinjam, sudah kembali, dan yang telat.", status, false,
				"Belum ada data peminjaman."));

		sb.append(DashboardUiKit.barList("Buku Paling Sering Dipinjam", "Judul yang paling diminati anggota.",
				d.topBuku, "#2563eb", "kali", false, "Belum ada peminjaman."));

		sb.append(DashboardUiKit.barList("Anggota dengan Denda Tertinggi",
				"Anggota yang paling banyak menunggak denda keterlambatan.", d.topAnggotaDenda, "#ef4444", "", true,
				"Belum ada denda."));

		sb.append(DashboardUiKit.barList("Tren Peminjaman per Bulan",
				"Naik-turun jumlah peminjaman selama 12 bulan terakhir.", d.trenBulanan, "#7c3aed", "kali", false,
				"Belum ada data tren."));

		sb.append(DashboardUiKit.barList("Peminjaman per Perpustakaan",
				"Unit perpustakaan mana yang paling ramai meminjamkan buku.", d.perPerpustakaan, "#0d9488", "kali",
				false, "Belum ada data."));

		if (d.perPerpustakaan.size() >= 3) {
			int n = Math.min(6, d.perPerpustakaan.size());
			String[] labels = new String[n];
			int[] nilai = new int[n];
			double maxv = 0;
			for (Double v : d.perPerpustakaan.values()) {
				if (v != null && v.doubleValue() > maxv) {
					maxv = v.doubleValue();
				}
			}
			int i = 0;
			for (Map.Entry<String, Double> e : d.perPerpustakaan.entrySet()) {
				if (i >= n) {
					break;
				}
				labels[i] = e.getKey();
				nilai[i] = DashboardUiKit.pct(e.getValue() == null ? 0 : e.getValue().doubleValue(),
						maxv <= 0 ? 1 : maxv);
				i++;
			}
			sb.append(DashboardUiKit.spider("Peta Aktivitas Perpustakaan",
					"Makin luas jaring, makin ramai unit itu meminjamkan buku.", labels, nilai));
		}

		sb.append(DashboardUiKit.closeGrid());

		LinkedHashMap<String, String> ringkas = new LinkedHashMap<String, String>();
		ringkas.put("Sedang dipinjam", angka(d.sedangDipinjam) + " item");
		ringkas.put("Perlu ditagih (terlambat)", angka(d.terlambat) + " item");
		ringkas.put("Denda belum lunas", DashboardUiKit.money(d.dendaBelumLunas));
		sb.append(DashboardUiKit.insight("Ringkasan Cepat",
				"Hal-hal yang paling perlu diperhatikan petugas saat ini.", ringkas));

		return sb.toString();
	}

	/** HTML bilah "sedang memuat" dengan progress bar sederhana. */
	private String htmlLoading(String pesan, int pct) {
		int p = Math.max(0, Math.min(100, pct));
		return "<div style='padding:26px 20px;'>"
				+ "<div style='font-weight:700;color:#334155;margin-bottom:10px;'>" + DashboardUiKit.esc(pesan)
				+ "</div>" + "<div style='height:10px;background:#e2e8f0;border-radius:999px;overflow:hidden;'>"
				+ "<div style='height:100%;width:" + p
				+ "%;background:linear-gradient(90deg,#2563eb,#0ea5e9);transition:width .3s ease;'></div>"
				+ "</div></div>";
	}

	/** Perbarui isi bilah "memuat" (dipanggil dari onTimer). */
	private void updateLoading(String pesan, int pct) {
		try {
			if (loadingHtml != null) {
				loadingHtml.setContent(htmlLoading(pesan, pct));
			}
		} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/library/helper/DasboardPerpustakaan.java:470");
		}
	}

	/** Format bilangan bulat dengan pemisah ribuan sesuai konfigurasi aplikasi. */
	private static String angka(long n) {
		return Common.numberFormat.get().format((double) n);
	}

	/** Ambil {@code long} dari hasil query agregat (null/bukan-angka &rarr; 0). */
	private static long ambilLong(Object o) {
		return o instanceof Number ? ((Number) o).longValue() : 0L;
	}

	/** Ambil {@code double} dari hasil query agregat (null/bukan-angka &rarr; 0). */
	private static double ambilDouble(Object o) {
		return o instanceof Number ? ((Number) o).doubleValue() : 0.0;
	}

	/**
	 * Wadah ringan (POJO) berisi semua angka yang ditampilkan papan. Dipisah agar
	 * {@link #muatData()} (baca DB) dan {@link #buildHtml(DataPerpus)} (render) tidak saling
	 * bergantung — memudahkan pengujian &amp; penambahan panel baru.
	 */
	public static class DataPerpus {
		long totalDipinjam;
		long sedangDipinjam;
		long sudahKembali;
		long terlambat;
		long anggotaAktif;
		double totalDenda;
		double dendaBelumLunas;
		final LinkedHashMap<String, Double> topBuku = new LinkedHashMap<String, Double>();
		final LinkedHashMap<String, Double> topAnggotaDenda = new LinkedHashMap<String, Double>();
		final LinkedHashMap<String, Double> perPerpustakaan = new LinkedHashMap<String, Double>();
		final LinkedHashMap<String, Double> trenBulanan = new LinkedHashMap<String, Double>();
	}
}
