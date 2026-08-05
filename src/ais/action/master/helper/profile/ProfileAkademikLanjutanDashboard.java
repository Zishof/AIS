package ais.action.master.helper.profile;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Html;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.ui.util.MyRowStyled;

/**
 * <h3>ProfileAkademikLanjutanDashboard — Ringkasan Akademik Lanjutan (Perguruan Tinggi)</h3>
 *
 * <p><b>Untuk apa:</b> Kelas utilitas statik yang menambahkan panel ringkasan akademik
 * lanjutan ke halaman profil admin perguruan tinggi atau admin gabungan. Panel ini
 * menampilkan serangkaian kartu angka clickable yang mencakup aspek-aspek akademik
 * yang tidak tercakup oleh {@link ProfileAdminPerguruanTinggiAkademikDashboard},
 * antara lain: status skripsi mahasiswa (belum/telah sidang), pengajuan judul tugas
 * akhir beserta semua statusnya, jadwal perkuliahan aktif, program KKN dan PKL,
 * serta pendaftaran peserta wisuda. Setiap kartu angka dapat diklik oleh pengguna
 * untuk membuka modal popup yang menjelaskan makna angka tersebut beserta panduan
 * menu mana yang mengelola data terkait.</p>
 *
 * <p><b>Desain asinkron (non-blocking):</b> Proses render panel dilakukan dalam dua
 * tahap untuk menghindari blokade UI. Tahap pertama: metode {@link #append(Rows)}
 * langsung memasang komponen {@code Html} berisi indikator "loading" ke dalam
 * {@code Rows} grid profil, sehingga halaman sudah dapat ditampilkan kepada pengguna
 * tanpa menunggu perhitungan selesai. Tahap kedua: sebuah {@link Timer} ZK dengan
 * jeda singkat (120 ms) segera dinyalakan; saat timer terpicu, penghitungan dijalankan
 * di event thread ZK (bukan thread terpisah, sehingga aman untuk ZK), dan hasilnya
 * menggantikan konten slot loading. Pola ini menjaga responsivitas halaman profil
 * meskipun total penghitungan untuk semua kartu memerlukan banyak query COUNT.</p>
 *
 * <p><b>Strategi cache tiga lapis:</b> Setelah HTML kartu selesai dirakit, hasilnya
 * disimpan ke {@link DashboardCacheUtil} pada dua level: L2 (per-sesi pengguna, TTL
 * pendek) dan L3 (per-JVM/shared, TTL lebih panjang). Saat berikutnya pengguna
 * membuka halaman yang sama atau pengguna lain dengan data yang sama membuka profil,
 * kelas ini akan menemukan hasil yang sudah ada di cache dan mengembalikannya langsung
 * tanpa mengulangi semua query COUNT ke database. Strategi ini sangat menghemat beban
 * database pada aplikasi dengan banyak pengguna aktif secara bersamaan.</p>
 *
 * <p><b>Kartu yang disediakan (11 kartu):</b> Skripsi Belum Sidang, Skripsi Telah
 * Sidang, Pengajuan Judul (Diajukan / Disetujui / Seminar / Sidang / Mengulang /
 * Ditolak), Jadwal Perkuliahan, KKN, PKL, dan Peserta Wisuda. Setiap kartu
 * dibuat lewat metode privat {@link #kartu} yang memanggil
 * {@link ProfileUiHelper#statClickable} dan {@link ProfileUiHelper#modal}.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Seluruh metode adalah statik (tidak ada
 * status instance). Kelas dideklarasikan {@code final} sehingga tidak dapat
 * diwarisi. Tidak ada bidang statik berubah, kecuali via cache eksternal.
 * Aman untuk dipanggil dari event thread ZK secara bersamaan selama
 * {@link DashboardCacheUtil} sendiri thread-safe. Java 1.7, ZKoss 5.5.</p>
 */
public final class ProfileAkademikLanjutanDashboard {

	/**
	 * Konstruktor privat — kelas ini adalah utilitas statik murni dan tidak boleh
	 * diinstansiasi. Semua metode disediakan sebagai {@code public static} dan
	 * dapat dipanggil langsung tanpa membuat objek. Mendeklarasikan konstruktor privat
	 * mencegah pemanggil luar secara tidak sengaja membuat instance yang tidak berguna.
	 * Pola ini konsisten dengan kelas utilitas statik lain dalam paket profil,
	 * misalnya {@link ProfileUiHelper} dan {@link ProfileUtil}.
	 */
	private ProfileAkademikLanjutanDashboard() {
	}

	/**
	 * Menambahkan panel "Ringkasan Akademik Lanjutan" ke baris grid profil dengan
	 * pola asinkron dua-tahap: langsung tampil loading, lalu hitung di latar.
	 *
	 * <p><b>Tujuan:</b> Memasang blok kartu angka akademik lanjutan (skripsi, tugas
	 * akhir, jadwal, KKN, PKL, wisuda) ke dalam struktur grid profil admin perguruan
	 * tinggi. Dengan pola asinkron, halaman tidak perlu menunggu semua query COUNT
	 * selesai sebelum ditampilkan, sehingga pengalaman pengguna tetap responsif.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa apakah {@code rows} null — jika iya, langsung return tanpa
	 *       melakukan apa-apa untuk mencegah NullPointerException.</li>
	 *   <li>Menambahkan header kelompok ({@code MyGroupConfig}) bertuliskan
	 *       "Ringkasan Akademik Lanjutan" sebagai pemisah visual.</li>
	 *   <li>Menambahkan baris info ({@link ProfileUiHelper#appendPanelInfoRow})
	 *       berisi deskripsi ringkas panel untuk panduan pengguna.</li>
	 *   <li>Membuat satu {@code Row} baru dengan span 2 (untuk grid 2-kolom)
	 *       berisi {@code Vbox} sebagai wadah konten.</li>
	 *   <li>Memasang komponen {@code Html} slot berisi indikator "loading" dengan
	 *       spinner CSS dan teks "Menghitung ringkasan akademik lanjutan…".</li>
	 *   <li>Membuat {@link Timer} ZK non-repeating dengan delay 120 ms. Saat timer
	 *       terpicu, {@code onTimer} dipanggil di event thread ZK (aman untuk
	 *       mengupdate UI). Di dalam handler, memanggil {@link #htmlBercache()}
	 *       untuk mendapatkan HTML akhir, lalu menyetel {@code slot.setContent()},
	 *       menggantikan indikator loading dengan konten sesungguhnya. Jika terjadi
	 *       kesalahan, slot diisi pesan error dan timer diberhentikan (stop+detach)
	 *       agar tidak terpicu ulang.</li>
	 *   <li>Memanggil {@code timer.start()} untuk mengaktifkan timer.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Error dalam handler timer ditangkap dengan
	 * try-catch dan ditampilkan via {@link Common#tampilErrorJikaAdmin} (hanya
	 * terlihat untuk role admin). Slot kemudian diisi pesan fallback agar UI
	 * tidak meninggalkan spinner tak terbatas. Timer selalu diberhentikan di
	 * blok akhir yang terpisah untuk mencegah kebocoran sumber daya.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika menambah kategori kartu baru, lakukan di
	 * {@link #hitungDanRender()}, bukan di sini. Metode ini hanya mengelola
	 * infrastruktur tampilan asinkron.</p>
	 *
	 * @param rows wadah {@link Rows} grid profil tempat panel akan ditambahkan;
	 *             jika {@code null}, metode tidak melakukan apa-apa
	 */
	public static void append(final Rows rows) {
		if (rows == null) {
			return;
		}
		rows.appendChild(new ais.ui.util.MyGroupConfig("Ringkasan Akademik Lanjutan"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Ringkasan Akademik Lanjutan",
				"Tugas akhir, perkuliahan, KKN/PKL, dan wisuda. Dihitung di latar (dengan cache cepat) lalu ditampilkan. Klik tiap angka untuk rincian.");

		Row row = new MyRowStyled();
		try {
			ais.ui.util.ZkCompat.setSpans(row, "2");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAkademikLanjutanDashboard.java:133");
		}
		row.setParent(rows);

		final Vbox container = new Vbox();
		container.setWidth("100%");
		container.setParent(row);

		final Html slot = new Html("");

		// Tombol "Refresh": muat ulang dari DATABASE & perbarui cache parameter sama.
		org.zkoss.zul.Hbox bar = new org.zkoss.zul.Hbox();
		bar.setWidth("100%");
		bar.setPack("end");
		ais.ui.util.MyToolbarbuttonConfig btnRefresh = new ais.ui.util.MyToolbarbuttonConfig("Refresh",
				"/img/svg/refresh.svg");
		btnRefresh.setTooltiptext("Muat ulang data dari database & perbarui cache ringkasan akademik lanjutan.");
		btnRefresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					String k = cacheKey();
					DashboardCacheUtil.invalidateL2(k);
					DashboardCacheUtil.invalidateL3(k);
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAkademikLanjutanDashboard.java:157");
				}
				muatDiLatar(slot, container);
			}
		});
		btnRefresh.setParent(bar);
		bar.setParent(container);
		slot.setParent(container);

		// Cache parameter sama ADA → tampil LANGSUNG tanpa "Loading"; kosong → "Loading" lalu hitung di latar.
		String cached = ambilCache();
		if (cached != null) {
			slot.setContent(cached);
		} else {
			muatDiLatar(slot, container);
		}
	}

	/** Tampilkan "Loading", lalu hitung HTML DI LATAR (Timer ZK) dan tampilkan ke slot. */
	private static void muatDiLatar(final Html slot, final Vbox container) {
		slot.setContent("<div class=\"ais-profile-modern ais-profile-loading\">"
				+ "<span class=\"ais-profile-spinner\"></span><b>Menghitung ringkasan akademik lanjutan…</b>"
				+ "<br/><span style=\"font-size:11px;\">Mohon tunggu sebentar.</span></div>");
		final Timer timer = new Timer();
		timer.setDelay(120);
		timer.setRepeats(false);
		timer.setParent(container);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					slot.setContent(htmlBercache());
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
					slot.setContent("<div class=\"ais-profile-modern ais-profile-loading\">Ringkasan akademik lanjutan gagal dimuat.</div>");
				}
				try {
					timer.stop();
					timer.detach();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAkademikLanjutanDashboard.java:196");
				}
			}
		});
		timer.start();
	}

	/** Kunci cache di-scope per HOST (tenant) agar tidak bocor antar-tenant (L3 app-wide). */
	private static String cacheKey() {
		String host = "";
		try {
			String h = Common.getRequestHost();
			if (h != null) {
				host = h;
			}
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAkademikLanjutanDashboard.java:211");
		}
		return DashboardCacheUtil.keyWithFilter("ProfileAkademikLanjutan", "PT", null, host);
	}

	/** Peek cache (L2 lalu L3) TANPA menghitung; null bila belum ada. */
	private static String ambilCache() {
		String key = cacheKey();
		try {
			Object l2 = DashboardCacheUtil.getL2(key);
			if (l2 instanceof String) {
				return (String) l2;
			}
			Object l3 = DashboardCacheUtil.getL3(key);
			if (l3 instanceof String) {
				DashboardCacheUtil.putL2(key, l3); // pemanasan L2
				return (String) l3;
			}
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAkademikLanjutanDashboard.java:229");
		}
		return null;
	}

	/**
	 * Mengambil HTML kartu ringkasan dari cache tiga lapis, atau menghitung ulang
	 * bila cache belum tersedia.
	 *
	 * <p><b>Tujuan:</b> Memisahkan logika pengambilan dari cache dengan logika
	 * perhitungan, sehingga {@link #append(Rows)} tetap sederhana dan logika cache
	 * bisa diubah tanpa menyentuh kode UI. Metode ini menjadi satu-satunya titik
	 * masuk untuk mendapatkan HTML akhir panel, baik dari cache maupun lewat
	 * perhitungan baru.</p>
	 *
	 * <p><b>Cara kerja (urutan prioritas cache):</b>
	 * <ol>
	 *   <li>Membuat kunci cache dengan {@link DashboardCacheUtil#key} menggunakan
	 *       nama kelas "ProfileAkademikLanjutan" dan tag "PT". Kunci ini sama untuk
	 *       semua pengguna dan semua sesi karena data yang ditampilkan adalah
	 *       statistik agregat seluruh institusi, bukan per-pengguna.</li>
	 *   <li>Memeriksa cache L2 (per-sesi, TTL pendek). Jika ditemukan objek
	 *       {@code String}, dikembalikan langsung — ini jalur tercepat.</li>
	 *   <li>Jika L2 kosong, memeriksa cache L3 (JVM-shared, TTL lebih panjang).
	 *       Jika ditemukan, hasil L3 juga disimpan ke L2 sebagai "pemanasan" agar
	 *       permintaan sesi berikutnya dalam sesi yang sama juga cepat.</li>
	 *   <li>Jika keduanya kosong (pertama kali atau setelah invalidasi cache),
	 *       memanggil {@link #hitungDanRender()} untuk melakukan semua query COUNT
	 *       dan merakit HTML, lalu menyimpan hasilnya ke L2 dan L3.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Keamanan thread:</b> Keamanan cache bergantung pada implementasi
	 * {@link DashboardCacheUtil}. Dalam kondisi race, dua thread bisa saja
	 * sama-sama memanggil {@link #hitungDanRender()}, tetapi hasilnya identik
	 * sehingga tidak menyebabkan inkonsistensi, hanya duplikasi kerja yang
	 * sangat jarang terjadi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Untuk memaksa kalkulasi ulang tanpa restart server,
	 * invalidasi cache L2 dan L3 di {@link DashboardCacheUtil} dengan kunci yang
	 * sama. Kunci bersifat deterministik dan tidak berubah selama nama kelas
	 * dan tag tetap sama.</p>
	 *
	 * @return String HTML lengkap berisi panel kartu angka clickable; tidak pernah
	 *         {@code null} (minimal mengembalikan HTML dengan panel kosong)
	 */
	private static String htmlBercache() {
		String cached = ambilCache();
		if (cached != null) {
			return cached;
		}
		String html = hitungDanRender();
		try {
			String key = cacheKey();
			DashboardCacheUtil.putL2(key, html);
			DashboardCacheUtil.putL3(key, html);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAkademikLanjutanDashboard.java:285");
		}
		return html;
	}

	/**
	 * Melakukan semua query COUNT ke database dan merakit HTML panel ringkasan
	 * akademik lanjutan beserta modal popup untuk setiap kartu angka.
	 *
	 * <p><b>Tujuan:</b> Menjalankan query Hibernate COUNT untuk setiap entitas
	 * yang dipantau, mengubah setiap angka menjadi kartu HTML clickable, lalu
	 * menggabungkan semua kartu ke dalam panel besar yang siap ditampilkan.
	 * Metode ini adalah inti komputasi dari kelas ini — semua yang mahal secara
	 * I/O ada di sini.</p>
	 *
	 * <p><b>Cara kerja (urutan kartu):</b>
	 * <ol>
	 *   <li><b>Skripsi Belum Sidang</b>: COUNT {@code Skripsi} di mana
	 *       {@code tanggalSidang IS NULL}. Mengidentifikasi mahasiswa yang
	 *       belum melaksanakan sidang skripsi.</li>
	 *   <li><b>Skripsi Telah Sidang</b>: COUNT {@code Skripsi} di mana
	 *       {@code tanggalSidang IS NOT NULL}. Kebalikan dari kartu di atas.</li>
	 *   <li><b>Pengajuan Tugas Akhir — 6 status</b>: COUNT
	 *       {@code MahasiswaRequestTugasAkhir} dikelompokkan berdasarkan nilai
	 *       konstanta status: {@code REQUEST_STATUS} (Diajukan), {@code AKTIF_STATUS}
	 *       (Disetujui), {@code SEMINAR_STATUS} (Seminar/Bimbingan), {@code LULUS_STATUS}
	 *       (Sidang), {@code MENGULANG_STATUS} (Mengulang), dan {@code GAGAL_STATUS}
	 *       (Ditolak). Enam kartu terpisah memberikan gambaran alur pengajuan judul
	 *       dari awal hingga akhir.</li>
	 *   <li><b>Jadwal Perkuliahan</b>: COUNT {@code Perkuliahan} dengan
	 *       {@code aktif = true}. Menunjukkan jumlah kelas/jadwal yang sedang
	 *       berjalan pada semester ini.</li>
	 *   <li><b>KKN</b>: COUNT total entitas {@code Kkn} (semua data KKN tercatat).</li>
	 *   <li><b>PKL</b>: COUNT total entitas {@code Pkl} (semua data PKL tercatat).</li>
	 *   <li><b>Peserta Wisuda</b>: COUNT total {@code PendaftaranWisuda} (semua
	 *       pendaftar wisuda yang pernah tercatat).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Perakitan HTML:</b> Setiap kartu dibuat lewat {@link #kartu} yang
	 * menghasilkan dua hal: string HTML kartu (masuk ke list {@code stats}) dan
	 * string HTML modal (ditambahkan ke {@code StringBuffer modals}). Setelah
	 * semua kartu selesai, {@code stats} dibungkus oleh {@link ProfileUiHelper#statsWrap}
	 * menjadi tata letak grid responsif, lalu seluruh panel (judul + deskripsi +
	 * kartu + modal) dirakit oleh {@link ProfileUiHelper#panel}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Untuk menambah kartu baru, cukup tambahkan satu
	 * pemanggilan {@link #kartu} dengan entitas dan kriteria yang sesuai, di
	 * posisi yang logis dalam urutan tampilan. Nama label kartu akan muncul di UI
	 * dan di judul popup modal secara otomatis.</p>
	 *
	 * @return String HTML lengkap panel beserta semua modal; tidak pernah {@code null}
	 */
	private static String hitungDanRender() {
		List<String> stats = new ArrayList<String>();
		StringBuffer modals = new StringBuffer();

		// ── Skripsi: belum vs telah sidang (berdasarkan tanggal sidang) ──
		kartu(stats, modals, "Skripsi Belum Sidang",
				ProfileUiHelper.hitung(ais.database.model.Skripsi.class, satu(Restrictions.isNull("tanggalSidang"))),
				"Skripsi yang belum melaksanakan sidang.", "Daftar rinci ada di menu Skripsi.");
		kartu(stats, modals, "Skripsi Telah Sidang",
				ProfileUiHelper.hitung(ais.database.model.Skripsi.class, satu(Restrictions.isNotNull("tanggalSidang"))),
				"Skripsi yang sudah melaksanakan sidang.", "Daftar rinci ada di menu Skripsi.");

		// ── Pengajuan Tugas Akhir per status ──
		kartu(stats, modals, "Pengajuan Judul: Diajukan",
				ProfileUiHelper.hitung(MahasiswaRequestTugasAkhir.class,
						satu(Restrictions.eq("status", MahasiswaRequestTugasAkhir.REQUEST_STATUS))),
				"Pengajuan judul tugas akhir yang masih menunggu (status Pengajuan).",
				"Daftar rinci ada di menu Pengajuan Tugas Akhir.");
		kartu(stats, modals, "Pengajuan Judul: Disetujui",
				ProfileUiHelper.hitung(MahasiswaRequestTugasAkhir.class,
						satu(Restrictions.eq("status", MahasiswaRequestTugasAkhir.AKTIF_STATUS))),
				"Pengajuan judul yang sudah disetujui.", "Daftar rinci ada di menu Pengajuan Tugas Akhir.");
		kartu(stats, modals, "Pengajuan Judul: Seminar/Bimbingan",
				ProfileUiHelper.hitung(MahasiswaRequestTugasAkhir.class,
						satu(Restrictions.eq("status", MahasiswaRequestTugasAkhir.SEMINAR_STATUS))),
				"Sedang seminar / proses bimbingan.", "Daftar rinci ada di menu Pengajuan Tugas Akhir.");
		kartu(stats, modals, "Pengajuan Judul: Sidang",
				ProfileUiHelper.hitung(MahasiswaRequestTugasAkhir.class,
						satu(Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS))),
				"Sudah sampai tahap sidang.", "Daftar rinci ada di menu Pengajuan Tugas Akhir.");
		kartu(stats, modals, "Pengajuan Judul: Mengulang",
				ProfileUiHelper.hitung(MahasiswaRequestTugasAkhir.class,
						satu(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS))),
				"Harus mengulang.", "Daftar rinci ada di menu Pengajuan Tugas Akhir.");
		kartu(stats, modals, "Pengajuan Judul: Ditolak",
				ProfileUiHelper.hitung(MahasiswaRequestTugasAkhir.class,
						satu(Restrictions.eq("status", MahasiswaRequestTugasAkhir.GAGAL_STATUS))),
				"Pengajuan ditolak.", "Daftar rinci ada di menu Pengajuan Tugas Akhir.");

		// ── Jadwal Perkuliahan aktif ──
		kartu(stats, modals, "Jadwal Perkuliahan",
				ProfileUiHelper.hitung(ais.database.model.Perkuliahan.class,
						satu(Restrictions.eq("aktif", Boolean.TRUE))),
				"Jadwal/kelas perkuliahan yang aktif.", "Daftar rinci ada di menu Jadwal Perkuliahan.");

		// ── KKN & PKL ──
		kartu(stats, modals, "KKN", ProfileUiHelper.hitung(ais.database.model.Kkn.class, null),
				"Total data KKN tercatat.", "Daftar rinci ada di menu KKN.");
		kartu(stats, modals, "PKL", ProfileUiHelper.hitung(ais.database.model.Pkl.class, null),
				"Total data PKL tercatat.", "Daftar rinci ada di menu PKL.");

		// ── Peserta (Pendaftaran) Wisuda ──
		kartu(stats, modals, "Peserta Wisuda",
				ProfileUiHelper.hitung(ais.database.model.PendaftaranWisuda.class, null),
				"Total pendaftaran/peserta wisuda tercatat.", "Daftar rinci ada di menu Wisuda.");

		String isi = ProfileUiHelper.statsWrap(stats.toArray(new String[stats.size()]));
		return ProfileUiHelper.panel("Tugas Akhir, Perkuliahan, KKN/PKL & Wisuda",
				"Angka untuk pimpinan: skripsi, pengajuan judul per status, jadwal kuliah, KKN/PKL, dan wisuda. Klik tiap angka untuk rincian.",
				isi) + modals.toString();
	}

	/**
	 * Membuat satu pasang kartu-angka clickable beserta modal popup penjelasannya,
	 * lalu menambahkan keduanya ke struktur data output yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Memusatkan logika pembentukan satu unit kartu + modal
	 * sehingga {@link #hitungDanRender()} tidak perlu mengulang kode boilerplate
	 * untuk setiap kartu. Dengan metode ini, menambah kartu baru hanya membutuhkan
	 * satu baris kode tambahan di {@link #hitungDanRender()}.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membangkitkan ID modal unik via {@link ProfileUiHelper#nextModalId}
	 *       dengan prefix "aisAkadLanjut". ID ini digunakan sebagai penghubung
	 *       antara kartu (tombol/onclick) dan modal (target yang ditampilkan saat
	 *       kartu diklik). Keunikan ID dijamin oleh {@code AtomicLong} di
	 *       {@link ProfileUiHelper}.</li>
	 *   <li>Memformat angka {@code jumlah} menjadi string tampilan dengan
	 *       {@link ProfileUiHelper#fmt} (pemisah ribuan sesuai lokal Indonesia,
	 *       misalnya "1.234").</li>
	 *   <li>Membuat HTML kartu clickable via {@link ProfileUiHelper#statClickable}
	 *       yang menyertakan label, nilai terformat, teks hint, dan ID modal.
	 *       Saat diklik, kartu ini akan membuka popup modal yang sesuai.</li>
	 *   <li>Menambahkan HTML kartu ke {@code stats} (list yang nantinya dibungkus
	 *       oleh {@link ProfileUiHelper#statsWrap}).</li>
	 *   <li>Membuat HTML modal via {@link ProfileUiHelper#modal} dengan konten
	 *       dari {@link ProfileUiHelper#modalAngka} yang menampilkan label, nilai,
	 *       hint, dan panduan menu navigasi.</li>
	 *   <li>Menambahkan HTML modal ke {@code modals} (buffer yang nantinya
	 *       digabungkan di akhir HTML panel).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Catatan desain:</b> Modal dan kartu menggunakan ID yang sama (mid)
	 * sebagai referensi silang. Modal ditempatkan di luar grid kartu (di akhir
	 * HTML) agar tidak merusak tata letak grid, sementara JavaScript onclick
	 * kartu akan mencarinya berdasarkan ID tersebut di seluruh dokumen.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada perubahan pada
	 * API {@link ProfileUiHelper}. Pemanggil cukup mengganti label, jumlah,
	 * hint, dan panduan sesuai konteks kartu baru.</p>
	 *
	 * @param stats  list tempat menampung HTML kartu clickable yang sudah dibuat
	 * @param modals buffer tempat menampung HTML modal popup yang sudah dibuat
	 * @param label  judul kartu (contoh: "Skripsi Belum Sidang")
	 * @param jumlah angka hasil COUNT dari database
	 * @param hint   keterangan singkat tentang apa yang dihitung kartu ini
	 * @param panduan teks panduan menu mana yang mengelola data ini
	 */
	private static void kartu(List<String> stats, StringBuffer modals, String label, long jumlah, String hint,
			String panduan) {
		String mid = ProfileUiHelper.nextModalId("aisAkadLanjut");
		String nilai = ProfileUiHelper.fmt(Long.valueOf(jumlah));
		stats.add(ProfileUiHelper.statClickable(label, nilai, hint, mid));
		modals.append(ProfileUiHelper.modal(mid, "Rincian " + label,
				ProfileUiHelper.modalAngka(label, nilai, hint, panduan)));
	}

	/**
	 * Membungkus satu objek {@link Criterion} menjadi array satu elemen agar
	 * kompatibel dengan signature metode {@link ProfileUiHelper#hitung(Class, Criterion[])}.
	 *
	 * <p><b>Tujuan:</b> Metode {@link ProfileUiHelper#hitung} menerima parameter
	 * varargs {@code Criterion[]} untuk mendukung penggunaan dengan nol, satu, atau
	 * banyak kriteria sekaligus. Namun saat memanggil dengan satu {@link Criterion}
	 * yang sudah dikonstruksi sebagai variabel, Java membutuhkan pembungkusan eksplisit
	 * ke dalam array. Metode helper ini menghilangkan boilerplate
	 * {@code new Criterion[]{ c }} yang berulang di setiap pemanggil.</p>
	 *
	 * <p><b>Cara kerja:</b> Sangat sederhana — membuat array {@code Criterion} baru
	 * berisi satu elemen {@code c} yang diteruskan dan mengembalikannya. Tidak ada
	 * validasi null karena {@link ProfileUiHelper#hitung} sendiri sudah menangani
	 * kasus criterion null dengan aman.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika suatu saat perlu mendukung lebih dari satu
	 * criterion di satu kartu (misalnya filter multi-kondisi), gunakan langsung
	 * {@code new Criterion[]{ c1, c2 }} di pemanggil alih-alih memperluas metode
	 * ini, agar pola satu-criterion tetap terbaca jelas. Metode ini tidak perlu
	 * diubah selama signature {@link ProfileUiHelper#hitung} tidak berubah.</p>
	 *
	 * @param c objek {@link Criterion} Hibernate yang akan dibungkus; boleh
	 *          {@code null} jika memang tidak ada filter (tidak ada manfaatnya
	 *          memanggil metode ini dengan {@code null} karena
	 *          {@code ProfileUiHelper.hitung(cls, null)} juga diterima)
	 * @return array satu elemen berisi {@code c}
	 */
	private static Criterion[] satu(Criterion c) {
		return new Criterion[] { c };
	}
}
