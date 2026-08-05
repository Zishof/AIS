package ais.action.master.helper.profile;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.zkoss.zul.Html;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.action.master.RencanaTahunAkademikAction;
import ais.action.master.dashboard.admin.DashboardAkademikHtmlCssHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.RencanaTahunAkademik;
import ais.database.model.Tbmuser;
import ais.ui.util.MyRowStyled;

/**
 * <h3>ProfileAdminPerguruanTinggiAkademikDashboard — Panel Ringkasan Akademik Per Prodi</h3>
 *
 * <p><b>Untuk apa:</b> Menyajikan panel "Ringkasan Akademik" pada halaman profil admin
 * perguruan tinggi. Panel ini menampilkan enam kartu angka utama kampus (mahasiswa aktif
 * semester berjalan, lulusan, dosen aktif, calon mahasiswa, mahasiswa cuti, rasio mhs/dosen)
 * yang dapat diklik untuk melihat rincian per program studi, serta grafik batang berkelompok
 * yang membandingkan lima metrik tersebut antar prodi.</p>
 *
 * <p><b>Cara kerja:</b>
 * <ol>
 *   <li>Method {@link #append(Rows)} dipanggil oleh
 *       {@link ProfileAdminPerguruanTinggi#lanjut(Rows, Tbmuser, boolean)}.</li>
 *   <li>Satu query SQL agregat ({@link #ambilDataPerProdi()}) mengambil data untuk semua
 *       prodi sekaligus: mahasiswa aktif (via {@code history_status_mahasiswa} semester
 *       berjalan), lulusan ({@code status_keluar=1}), dosen, calon mahasiswa, dan cuti.</li>
 *   <li>Kartu ringkasan dibangun dari total agregat dan menggunakan ID unik statis
 *       (berbasis nama, bukan sequential) untuk popup — tidak ada konflik ID karena
 *       panel ini hanya dirender sekali per halaman.</li>
 *   <li>Popup rincian per prodi ({@link #binaModalRincian}) dibangun dari data yang sudah
 *       dimuat tanpa query tambahan (reuse).</li>
 *   <li>Grafik batang berkelompok ({@code DashboardAkademikHtmlCssHelper.groupedBarChart})
 *       menerima labels (nama prodi) dan values (array 5 int per prodi).</li>
 * </ol>
 * </p>
 *
 * <p><b>Definisi "Mahasiswa Aktif":</b> Mahasiswa yang berstatus AKTIF (via tabel
 * {@code history_status_mahasiswa}) pada tahun akademik dan semester berjalan saat ini,
 * bukan sekadar flag {@code mahasiswa.aktif}. Jika {@code ConstantValues.AKTIF} null
 * (status belum dikonfigurasi), fallback ke flag agar angka tidak kosong.</p>
 *
 * <p><b>Hak akses:</b> Query di-filter berdasarkan fakultas/jurusan/PT yang melekat pada
 * {@code Tbmuser} yang sedang login — admin per fakultas hanya melihat data fakultasnya.</p>
 *
 * <p><b>Threading:</b> Kelas ini utility statik final; tidak ada state yang dibagi
 * antar thread. Query dilakukan via {@code Common.ambilSql} yang menggunakan sesi
 * Hibernate yang dikelola framework. Harus dipanggil dari ZK event thread.</p>
 *
 * <p><b>Pemeliharaan:</b> Java 1.7 dan ZKoss 5.5. Perubahan definisi "aktif" harus
 * diperbarui secara konsisten dengan {@code CariMahasiswaAction} dan
 * {@code LaporanRasioJumlahMahasiswaDanDosen}.</p>
 */
public final class ProfileAdminPerguruanTinggiAkademikDashboard {

	/**
	 * Konstruktor privat untuk mencegah instansiasi class utility statik.
	 *
	 * <p>Semua method di class ini adalah statik; class hanya menyediakan fungsi
	 * tanpa state instance. Konstruktor privat memaksa penggunaan via method statik.</p>
	 */
	private ProfileAdminPerguruanTinggiAkademikDashboard() {
	}

	/**
	 * TTL cache HTML "Ringkasan Kampus" = 30 menit. Semua query COUNT/agregat (per-prodi + funnel
	 * PMB + bimbingan) BERAT; hasilnya berubah lambat (hanya saat ada pendaftar baru atau setelah
	 * tombol "Singkronkan"). Maka cukup dihitung sekali per 30 menit per (tenant+scope+TA+semester),
	 * dan tombol "Singkronkan" meng-invalidasi cache (lihat {@link #jalankanSingkron}).
	 */
	private static final int TTL_RINGKASAN_MS = 30 * 60 * 1000;

	/**
	 * Kunci cache di-scope per HOST (tenant) + Perguruan Tinggi + Fakultas + Jurusan admin + parameter
	 * (Tahun Akademik + Semester). Tujuan: (a) sesama admin (mis. Akademik & Admin) dengan scope &
	 * parameter SAMA memakai hasil dari cache — TIDAK query DB lagi; (b) tidak bocor antar-tenant/scope
	 * (cache L3 bersifat app-wide). Berlaku untuk profil admin (PerguruanTinggi/Gabungan/Sekolah) yang
	 * memanggil panel ini.
	 */
	private static String cacheKeyRingkasan(String selTA, String selSem) {
		String host = "";
		try {
			String h = Common.getRequestHost();
			if (h != null) {
				host = h;
			}
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:100");
		}
		Long ptId = null, fakId = null, jurId = null;
		try {
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
			if (pt != null) {
				ptId = pt.getId();
			}
			Tbmuser u = Common.getCurrentUser();
			if (u != null) {
				Fakultas f = u.ambilFakultas();
				if (f != null) {
					fakId = f.getId();
				}
				Jurusan j = u.ambilJurusan();
				if (j != null) {
					jurId = j.getId();
				}
			}
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:119");
		}
		return "RingkasanAkad::" + host + "::" + ptId + "::" + fakId + "::" + jurId + "::" + selTA + "::" + selSem;
	}

	/**
	 * Ambil HTML panel ringkasan dari cache 2-lapis (L2 per-sesi, L3 app-wide). Bila belum ada,
	 * hitung sekali ({@link #bangunHtmlRingkasan}) lalu simpan ke kedua cache. Inilah yang mewujudkan
	 * "parameter sama → tidak query DB lagi".
	 */
	/**
	 * Peek cache (L2 per-sesi lalu L3 app-wide) untuk parameter ini; kembalikan null bila BELUM ada.
	 * TIDAK menghitung apa pun — dipakai renderKartu untuk memutuskan "tampil langsung" (cache ada)
	 * vs "tampil Loading lalu hitung di latar" (cache kosong).
	 */
	private static String ambilCacheRingkasan(String selTA, String selSem) {
		String key = cacheKeyRingkasan(selTA, selSem);
		try {
			Object l2 = ais.common.DashboardCacheUtil.getL2(key);
			if (l2 instanceof String) {
				return (String) l2;
			}
			Object l3 = ais.common.DashboardCacheUtil.getL3(key);
			if (l3 instanceof String) {
				ais.common.DashboardCacheUtil.putL2(key, l3); // pemanasan L2
				return (String) l3;
			}
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:146");
		}
		return null;
	}

	/** Ambil dari cache bila ada; jika kosong → hitung sekali ({@link #bangunHtmlRingkasan}) lalu simpan. */
	private static String htmlRingkasanBercache(String selTA, String selSem) {
		String cached = ambilCacheRingkasan(selTA, selSem);
		if (cached != null) {
			return cached;
		}
		String html = bangunHtmlRingkasan(selTA, selSem);
		try {
			String key = cacheKeyRingkasan(selTA, selSem);
			ais.common.DashboardCacheUtil.putL2(key, html);
			ais.common.DashboardCacheUtil.putL3(key, html, TTL_RINGKASAN_MS);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:162");
		}
		return html;
	}

	/**
	 * Menambahkan panel "Ringkasan Akademik" perguruan tinggi ke dalam baris grid.
	 *
	 * <p><b>Tujuan:</b> Titik masuk utama class ini. Merender kelompok panel "Ringkasan
	 * Akademik" (header group + info row + kartu stat clickable + popup + grafik batang)
	 * ke dalam {@code Rows} ZK yang diberikan.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menambahkan group header "Ringkasan Akademik" dan baris info ke {@code rows}.</li>
	 *   <li>Membuat baris kolspan-2 berisi {@code Vbox} sebagai container.</li>
	 *   <li>Memanggil {@link #ambilDataPerProdi()} untuk mendapatkan data semua prodi
	 *       dari satu query SQL agregat.</li>
	 *   <li>Menjumlahkan semua nilai per kolom (mahasiswa, lulus, dosen, calon, cuti)
	 *       dari hasil query.</li>
	 *   <li>Membangun HTML kartu statistik (6 kartu clickable) via
	 *       {@code DashboardAkademikHtmlCssHelper.statClickable}.</li>
	 *   <li>Membangun semua popup rincian per prodi via {@link #binaModalRincian}
	 *       dan popup penjelasan rasio via {@link #binaModalRasio}.</li>
	 *   <li>Menambahkan grafik batang berkelompok via
	 *       {@code DashboardAkademikHtmlCssHelper.groupedBarChart}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Tidak ada guard eksplisit untuk kegagalan query;
	 * pemanggil ({@link ProfileAdminPerguruanTinggi#lanjut}) membungkus panggilan ini
	 * dalam try-catch dan menampilkan error ke admin.</p>
	 *
	 * @param rows baris ZK tujuan; jika {@code null} metode langsung return
	 */
	public static void append(Rows rows) {
		if (rows == null) {
			return;
		}
		rows.appendChild(new ais.ui.util.MyGroupConfig("Ringkasan Akademik"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Ringkasan Akademik",
				"Angka utama kampus ditampilkan cepat agar pimpinan mudah melihat kondisi mahasiswa, lulusan, dosen, pendaftar, dan cuti.");

		Row row = new MyRowStyled();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		Vbox container = new Vbox();
		container.setWidth("100%");
		container.setParent(row);

		// Default TA & paritas semester = TA/semester BERJALAN.
		final String taSekarang = Common.getCurrentTahunAkademik();
		final boolean ganjilSekarang = Boolean.TRUE.equals(Common.isNowSemensterGanjil());
		final String semSekarang = ganjilSekarang ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

		// ---- Bar filter: Tahun Akademik + Semester + tombol Singkronkan ----
		org.zkoss.zul.Hbox filterBar = new org.zkoss.zul.Hbox();
		filterBar.setSclass("ais-akad-filterbar");
		filterBar.setAlign("center");
		filterBar.setWidth("100%");
		filterBar.setParent(container);

		filterBar.appendChild(new ais.ui.util.MyLabelKecilBold("Tahun Akademik"));
		final org.zkoss.zul.Combobox comboTA = new org.zkoss.zul.Combobox();
		comboTA.setReadonly(true);
		comboTA.setWidth("120px");
		for (String ta : ambilDaftarTahunAkademik(taSekarang)) {
			org.zkoss.zul.Comboitem ci = comboTA.appendItem(ta);
			ci.setValue(ta);
			if (ta.equals(taSekarang)) {
				comboTA.setSelectedItem(ci);
			}
		}
		if (comboTA.getSelectedItem() == null && comboTA.getItemCount() > 0) {
			comboTA.setSelectedIndex(0);
		}
		comboTA.setParent(filterBar);

		filterBar.appendChild(new ais.ui.util.MyLabelKecilBold("Semester"));
		final org.zkoss.zul.Combobox comboSem = new org.zkoss.zul.Combobox();
		comboSem.setReadonly(true);
		comboSem.setWidth("90px");
		org.zkoss.zul.Comboitem ciGanjil = comboSem.appendItem(Perkuliahan.GANJIL);
		ciGanjil.setValue(Perkuliahan.GANJIL);
		org.zkoss.zul.Comboitem ciGenap = comboSem.appendItem(Perkuliahan.GENAP);
		ciGenap.setValue(Perkuliahan.GENAP);
		comboSem.setSelectedItem(ganjilSekarang ? ciGanjil : ciGenap);
		comboSem.setParent(filterBar);

		// Host kartu — di-render ulang saat filter berubah / setelah singkronisasi.
		final Vbox cardHost = new Vbox();
		cardHost.setWidth("100%");
		cardHost.setParent(container);

		// Tombol "Singkronkan" (di sebelah filter, dekat kartu Mahasiswa Aktif): regenerasi
		// history status mahasiswa utk TA & semester terpilih (sama spt "Singkronkan status"
		// di menu Mahasiswa -> HistoryStatusMahasiswaUtil.singkronisasiStatusMahasiswa).
		ais.ui.util.MyToolbarbuttonConfig btnSinkron = new ais.ui.util.MyToolbarbuttonConfig("Singkronkan",
				"/img/sync.png");
		btnSinkron.setSclass("ais-akad-sinkron-btn");
		btnSinkron.setTooltiptext(
				"Singkronkan status mahasiswa untuk Tahun Akademik & Semester terpilih (sama seperti Singkronkan status di menu Mahasiswa).");
		btnSinkron.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event e) throws Exception {
				String selTA = comboTA.getSelectedItem() == null ? taSekarang
						: String.valueOf(comboTA.getSelectedItem().getValue());
				String selSem = comboSem.getSelectedItem() == null ? semSekarang
						: String.valueOf(comboSem.getSelectedItem().getValue());
				konfirmasiDanSingkronkan(selTA, selSem, cardHost);
			}
		});
		btnSinkron.setParent(filterBar);

		// Tombol "Refresh": muat ulang angka dari DATABASE (hitung ulang) untuk TA & semester terpilih,
		// SEKALIGUS memperbarui cache parameter sama agar pengguna lain ikut dapat angka terbaru. Beda
		// dengan "Singkronkan" yang menghitung ulang history status mahasiswa (lebih berat) — Refresh
		// hanya menyegarkan tampilan dari kondisi DB saat ini.
		ais.ui.util.MyToolbarbuttonConfig btnRefresh = new ais.ui.util.MyToolbarbuttonConfig("Refresh",
				"/img/svg/refresh.svg");
		btnRefresh.setTooltiptext(
				"Muat ulang data dari database & perbarui cache untuk Tahun Akademik & Semester terpilih.");
		btnRefresh.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event e) throws Exception {
				String selTA = comboTA.getSelectedItem() == null ? taSekarang
						: String.valueOf(comboTA.getSelectedItem().getValue());
				String selSem = comboSem.getSelectedItem() == null ? semSekarang
						: String.valueOf(comboSem.getSelectedItem().getValue());
				// Buang cache parameter ini → renderKartu akan MISS → tampil "Loading" → hitung ulang
				// dari DB → simpan lagi ke cache (parameter sama kini berisi angka terbaru).
				try {
					String key = cacheKeyRingkasan(selTA, selSem);
					ais.common.DashboardCacheUtil.invalidateL2(key);
					ais.common.DashboardCacheUtil.invalidateL3(key);
					// Angka mahasiswa juga dihitung ulang -> renderKartu build di LATAR + progress bar.
					ais.common.RingkasanKampusCache.invalidasi(selTA, selSem);
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:300");
				}
				renderKartu(cardHost, selTA, selSem);
			}
		});
		btnRefresh.setParent(filterBar);

		org.zkoss.zk.ui.event.EventListener onUbahFilter = new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event e) throws Exception {
				String selTA = comboTA.getSelectedItem() == null ? taSekarang
						: String.valueOf(comboTA.getSelectedItem().getValue());
				String selSem = comboSem.getSelectedItem() == null ? semSekarang
						: String.valueOf(comboSem.getSelectedItem().getValue());
				renderKartu(cardHost, selTA, selSem);
			}
		};
		comboTA.addEventListener("onChange", onUbahFilter);
		comboSem.addEventListener("onChange", onUbahFilter);

		// Render awal memakai TA & semester berjalan.
		renderKartu(cardHost, taSekarang, semSekarang);
	}

	/**
	 * Membangun ulang kartu "Ringkasan Kampus" (Mahasiswa Aktif/Lulusan/Dosen/Calon/Cuti/Rasio)
	 * untuk Tahun Akademik (selTA) & semester (selSem = Ganjil/Genap) terpilih. Dipanggil saat
	 * render awal, saat filter diubah, dan setelah singkronisasi. Kartu lama dibersihkan dulu.
	 */
	private static void renderKartu(final Vbox cardHost, final String selTA, final String selSem) {
		if (cardHost == null) {
			return;
		}
		try {
			cardHost.getChildren().clear();
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:335");
		}
		// Bila cache untuk parameter (TA+semester+scope) yang SAMA sudah ada → tampilkan LANGSUNG
		// tanpa "Loading" (tinggal load). HANYA bila cache kosong, tampilkan "Loading" lalu hitung
		// di latar (saat akses pertama / setelah Refresh / setelah TTL habis).
		String cachedHtml = ambilCacheRingkasan(selTA, selSem);
		if (cachedHtml != null) {
			cardHost.appendChild(new org.zkoss.zul.Html(cachedHtml));
			return;
		}
		// PROGRESS BAR: hitung cache di THREAD LATAR (tidak memblokir UI), tampilkan fase & persen,
		// lalu rakit HTML ringkasan saat selesai (perakitan butuh konteks request -> di event-thread).
		final org.zkoss.zul.Vbox prog = new org.zkoss.zul.Vbox();
		prog.setStyle("padding:24px 16px;align-items:center;");
		prog.setParent(cardHost);
		final org.zkoss.zul.Label lblFase = new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Menyiapkan data ringkasan kampus…"));
		lblFase.setStyle("font-size:13px;font-weight:bold;color:#1e40af;margin-bottom:10px;");
		lblFase.setParent(prog);
		final org.zkoss.zul.Progressmeter meter = new org.zkoss.zul.Progressmeter(0);
		meter.setWidth("440px");
		meter.setStyle("max-width:80%;height:16px;");
		meter.setParent(prog);
		final org.zkoss.zul.Label lblPct = new org.zkoss.zul.Label("0%");
		lblPct.setStyle("font-size:12px;color:#475569;margin-top:8px;");
		lblPct.setParent(prog);

		// SATU builder latar dikoalesai di RingkasanKampusCache untuk SEMUA pembaca — JANGAN lagi
		// spawn 1 thread "profil-ringkasan-build" + 1 Timer PER render. Dulu, saat banyak admin
		// membuka dashboard sementara warmup masih membangun (berjam-jam), tiap render menumpuk
		// satu thread yang BLOCKED di build lock (sampai 75 thread macet). Timer di bawah cukup
		// memantau RingkasanKampusCache.isReady(...) tanpa memblokir.
		final boolean[] selesai = { false };
		ais.common.RingkasanKampusCache.ensureBuiltAsync(selTA, selSem);

		// Aktifkan server push agar ZK Timer memperbarui progress bar SECARA OTOMATIS
		// tanpa menunggu interaksi pengguna (klik/scroll). Tanpa ini, timer hanya
		// mengirim update pada request berikutnya — progress bar tampak beku di 0%.
		final org.zkoss.zk.ui.Desktop progressDesktop;
		org.zkoss.zk.ui.Desktop tmpDt = null;
		try {
			org.zkoss.zk.ui.Execution exec = org.zkoss.zk.ui.Executions.getCurrent();
			if (exec != null) {
				tmpDt = exec.getDesktop();
				if (tmpDt != null && !tmpDt.isServerPushEnabled()) {
					tmpDt.enableServerPush(true);
				}
			}
		} catch (Exception ig) {
			ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) ProfileAdminPT:serverPush-enable");
		}
		progressDesktop = tmpDt;

		final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer();
		timer.setDelay(500);
		timer.setRepeats(true);
		timer.setParent(cardHost);
		timer.addEventListener("onTimer", new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event event) throws Exception {
				selesai[0] = ais.common.RingkasanKampusCache.isReady(selTA, selSem);
				int pct = ais.common.RingkasanKampusCache.progressPersen();
				String fase = ais.common.RingkasanKampusCache.progressFase;
				int total = ais.common.RingkasanKampusCache.progressTotal;
				int proses = ais.common.RingkasanKampusCache.progressProses;
				try {
					lblFase.setValue(fase == null || fase.length() == 0 ? "Memproses…" : fase);
					meter.setValue(selesai[0] ? 100 : pct);
					lblPct.setValue((selesai[0] ? 100 : pct) + "%"
							+ (total > 0 ? "  (" + proses + " / " + total + " mahasiswa)" : ""));
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:386");
				}
				if (selesai[0]) {
					// Matikan server push segera setelah selesai agar tidak konsumsi resource
					try {
						if (progressDesktop != null && progressDesktop.isAlive()
								&& progressDesktop.isServerPushEnabled()) {
							progressDesktop.enableServerPush(false);
						}
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) ProfileAdminPT:serverPush-disable");
					}
					try {
						timer.stop();
						timer.detach();
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:392");
					}
					try {
						String html = htmlRingkasanBercache(selTA, selSem);
						prog.detach();
						cardHost.appendChild(new org.zkoss.zul.Html(html));
					} catch (Exception ex) {
						try {
							Common.tampilErrorJikaAdmin(ex);
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:401");
						}
						try {
							prog.detach();
							cardHost.appendChild(new org.zkoss.zul.Html(
									"<div class=\"ais-profile-modern ais-profile-loading\">Ringkasan kampus gagal dimuat.</div>"));
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:407");
						}
					}
				}
			}
		});
		timer.start();
	}

	/**
	 * Hitung SEMUA query (per-prodi + funnel PMB + bimbingan) lalu rakit menjadi SATU string HTML
	 * panel "Ringkasan Kampus". Inilah inti komputasi yang mahal; dipanggil hanya saat cache kosong.
	 */
	private static String bangunHtmlRingkasan(String selTA, String selSem) {
		StringBuffer html = new StringBuffer();
		List<Object[]> data = ambilDataPerProdi(selTA, selSem);
		int totalAktif = 0;
		int totalLulus = 0;
		int totalDosen = 0;
		int totalCalon = 0;
		int totalCuti = 0;
		int totalDropout = 0;
		int totalNonAktif = 0;
		int totalWisudaPendaftar = 0;
		int totalWisudaAcc = 0;

		List<String> labels = new ArrayList<String>();
		List<int[]> values = new ArrayList<int[]>();
		if (data != null) {
			for (Object[] obj : data) {
				String nama = obj != null && obj.length > 1 && obj[1] != null ? obj[1].toString() : "";
				// Default: nilai SQL lama (fallback). Lalu DITIMPA dari RingkasanKampusCache (logika
				// PERSIS LaporanRekapJumlahMahasiswa) supaya angka cocok dgn rekap, tanpa query ulang.
				int aktif = toInt(obj, 2);
				int lulus = toInt(obj, 3);
				int dosen = toInt(obj, 4);
				int calon = toInt(obj, 5);
				int cuti = toInt(obj, 6);
				int dropout = toInt(obj, 7);
				int nonaktif = toInt(obj, 8);
				int wisudaPendaftar = 0;
				int wisudaAcc = 0;
				Long jurusanId = (obj != null && obj.length > 0 && obj[0] instanceof Number)
						? ((Number) obj[0]).longValue() : null;
				try {
					if (jurusanId != null) {
						aktif = ais.common.RingkasanKampusCache.countMahasiswaProdi(selTA, selSem,
								ais.common.ConstantValues.AKTIF.getId(), jurusanId);
						cuti = ais.common.RingkasanKampusCache.countMahasiswaProdi(selTA, selSem,
								ais.common.ConstantValues.CUTI.getId(), jurusanId);
						lulus = ais.common.RingkasanKampusCache.countMahasiswaProdi(selTA, selSem,
								ais.common.ConstantValues.LULUS.getId(), jurusanId);
						// Drop Out = KELUAR + DROP_OUT (persis rekap).
						dropout = ais.common.RingkasanKampusCache.countMahasiswaProdi(selTA, selSem,
								ais.common.ConstantValues.KELUAR.getId(), jurusanId)
								+ ais.common.RingkasanKampusCache.countMahasiswaProdi(selTA, selSem,
										ais.common.ConstantValues.DROP_OUT.getId(), jurusanId);
						if (ais.common.ConstantValues.TIDAK_AKTIF != null) {
							nonaktif = ais.common.RingkasanKampusCache.countMahasiswaProdi(selTA, selSem,
									ais.common.ConstantValues.TIDAK_AKTIF.getId(), jurusanId);
						}
						dosen = ais.common.RingkasanKampusCache.countDosen(selTA, selSem, jurusanId);
						calon = ais.common.RingkasanKampusCache.countCalon(selTA, selSem, jurusanId);
						wisudaPendaftar = ais.common.RingkasanKampusCache.countWisudaPendaftar(selTA, selSem, jurusanId);
						wisudaAcc = ais.common.RingkasanKampusCache.countWisudaAcc(selTA, selSem, jurusanId);
					}
				} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:473");
					// constant belum siap / cache gagal -> tetap pakai nilai SQL di atas.
				}

				totalAktif += aktif;
				totalLulus += lulus;
				totalDosen += dosen;
				totalCalon += calon;
				totalCuti += cuti;
				totalDropout += dropout;
				totalNonAktif += nonaktif;
				totalWisudaPendaftar += wisudaPendaftar;
				totalWisudaAcc += wisudaAcc;

				labels.add(nama);
				values.add(new int[] { aktif, lulus, dosen, calon, cuti, dropout, nonaktif });
			}
		}

		final String idMhs = "aisAkadModalMhs";
		final String idNon = "aisAkadModalNon";
		final String idCuti = "aisAkadModalCuti";
		final String idLulus = "aisAkadModalLulus";
		final String idDO = "aisAkadModalDO";
		final String idDosen = "aisAkadModalDosen";
		final String idCalon = "aisAkadModalCalon";
		final String rasioModalId = "aisRasioMhsDosenModal";
		String konteks = " (" + selTA + " — " + selSem + ")";
		int rasioPembilang = totalAktif + totalCuti + totalNonAktif;

		// Dosen untuk kartu "Dosen Aktif" dan rasio Mhs/Dosen = jumlah dosen DISTINCT
		// se-UNIVERSITAS: dosen yang mengajar di lebih dari satu prodi dihitung SEKALI,
		// dosen tanpa SKS pada semester berjalan tidak dihitung. Penjumlahan per-prodi
		// (totalDosen) tetap dipakai untuk popup "Rincian Dosen per Prodi" agar baris
		// per prodinya tetap selaras dengan totalnya.
		int totalDosenUniv = totalDosen;
		try {
			int dosenDistinct = ais.common.RingkasanKampusCache.countDosenUniversitas(selTA, selSem);
			if (dosenDistinct > 0) {
				totalDosenUniv = dosenDistinct;
			}
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:514");
			// fallback: tetap pakai penjumlahan per-prodi bila cache distinct tak tersedia.
		}

		StringBuffer stats = new StringBuffer();
		stats.append("<div class=\"ais-akad-stats\">");
		stats.append(DashboardAkademikHtmlCssHelper.statClickable("Mahasiswa Aktif",
				DashboardAkademikHtmlCssHelper.fmt(totalAktif),
				"Mahasiswa berstatus AKTIF di semester berjalan (= Aktif Total laporan Rekap Jumlah Mahasiswa)"
						+ konteks + ".", idMhs));
		stats.append(DashboardAkademikHtmlCssHelper.statClickable("Mahasiswa Non Aktif",
				DashboardAkademikHtmlCssHelper.fmt(totalNonAktif),
				"Mahasiswa berstatus TIDAK AKTIF di semester berjalan (= Tidak Aktif Total pada Rekap)" + konteks + ".",
				idNon));
		stats.append(DashboardAkademikHtmlCssHelper.statClickable("Mahasiswa Cuti",
				DashboardAkademikHtmlCssHelper.fmt(totalCuti),
				"Mahasiswa berstatus CUTI (disetujui) di semester berjalan (= Cuti Total pada Rekap)" + konteks + ".",
				idCuti));
		stats.append(DashboardAkademikHtmlCssHelper.statClickable("Lulusan",
				DashboardAkademikHtmlCssHelper.fmt(totalLulus),
				"Total lulusan sampai dengan semester berjalan (= Lulus Total pada Rekap)" + konteks + ".", idLulus));
		stats.append(DashboardAkademikHtmlCssHelper.statClickable("Drop Out",
				DashboardAkademikHtmlCssHelper.fmt(totalDropout),
				"Mahasiswa berstatus Drop Out (= Drop Out Total pada Rekap)" + konteks + ".", idDO));
		stats.append(DashboardAkademikHtmlCssHelper.statClickable("Dosen Aktif",
				DashboardAkademikHtmlCssHelper.fmt(totalDosenUniv),
				"Jumlah dosen (orang) yang memiliki JADWAL (mengajar) pada Tahun Akademik terpilih. "
						+ "Dosen yang mengajar di lebih dari satu prodi dihitung SEKALI" + konteks + ".",
				idDosen));
		stats.append(DashboardAkademikHtmlCssHelper.statClickable("Calon Mahasiswa",
				DashboardAkademikHtmlCssHelper.fmt(totalCalon),
				"Jumlah pendaftar/calon mahasiswa pada Tahun Akademik terpilih (" + selTA + ").", idCalon));
		stats.append(DashboardAkademikHtmlCssHelper.stat("Pendaftar Wisuda",
				DashboardAkademikHtmlCssHelper.fmt(totalWisudaPendaftar),
				"Jumlah mahasiswa yang mendaftar wisuda (total seluruh data Pendaftaran Wisuda)."));
		stats.append(DashboardAkademikHtmlCssHelper.stat("Wisuda di-ACC",
				DashboardAkademikHtmlCssHelper.fmt(totalWisudaAcc),
				"Pendaftar wisuda yang sudah disetujui/di-ACC (persetujuan wisuda = Ya)."));
		stats.append(DashboardAkademikHtmlCssHelper.statClickable("Rasio Mhs/Dosen",
				rasio(rasioPembilang, totalDosenUniv),
				"Perbandingan (Aktif + Cuti + Non Aktif) dengan jumlah dosen DISTINCT se-universitas "
						+ "yang mengajar (dosen lintas prodi dihitung sekali)" + konteks + ".",
				rasioModalId));
		stats.append("</div>");

		html.append(DashboardAkademikHtmlCssHelper.panel("Ringkasan Kampus",
				"Angka besar ini membantu membaca kondisi kampus dalam sekali lihat. Pilih Tahun Akademik &amp; Semester di atas; klik tiap angka untuk rincian.",
				stats.toString()));

		StringBuffer popups = new StringBuffer();
		popups.append(DashboardAkademikHtmlCssHelper.modal(idMhs, "Rincian Mahasiswa Aktif per Prodi",
				binaModalRincian("Mahasiswa berstatus AKTIF di semester berjalan" + konteks + ".", labels, values, 0,
						totalAktif)));
		popups.append(DashboardAkademikHtmlCssHelper.modal(idNon, "Rincian Mahasiswa Non Aktif per Prodi",
				binaModalRincian("Mahasiswa berstatus TIDAK AKTIF di semester berjalan" + konteks + ".", labels, values,
						6, totalNonAktif)));
		popups.append(DashboardAkademikHtmlCssHelper.modal(idCuti, "Rincian Mahasiswa Cuti per Prodi",
				binaModalRincian("Mahasiswa berstatus CUTI di semester berjalan" + konteks + ".", labels, values, 4,
						totalCuti)));
		popups.append(DashboardAkademikHtmlCssHelper.modal(idLulus, "Rincian Lulusan per Prodi",
				binaModalRincian("Total lulusan sampai dengan semester berjalan" + konteks + ".", labels, values, 1,
						totalLulus)));
		popups.append(DashboardAkademikHtmlCssHelper.modal(idDO, "Rincian Drop Out per Prodi",
				binaModalRincian("Mahasiswa berstatus Drop Out" + konteks + ".", labels, values, 5, totalDropout)));
		popups.append(DashboardAkademikHtmlCssHelper.modal(idDosen, "Rincian Dosen Mengajar per Prodi",
				binaModalRincian("Dosen yang memiliki jadwal (mengajar) pada TA & semester terpilih" + konteks
						+ ". Catatan: jumlah per prodi di bawah dapat lebih besar dari angka kartu \"Dosen Aktif\" ("
						+ DashboardAkademikHtmlCssHelper.fmt(totalDosenUniv)
						+ " orang) karena seorang dosen yang mengajar di beberapa prodi terhitung pada tiap prodi.",
						labels, values, 2, totalDosen)));
		popups.append(DashboardAkademikHtmlCssHelper.modal(idCalon, "Rincian Calon Mahasiswa per Prodi",
				binaModalRincian("Pendaftar/calon mahasiswa pada Tahun Akademik terpilih (" + selTA + ").", labels,
						values, 3, totalCalon)));
		popups.append(DashboardAkademikHtmlCssHelper.modal(rasioModalId, "Cara Menghitung Rasio Mhs/Dosen",
				binaModalRasio(rasioPembilang, totalDosenUniv)));
		html.append(popups.toString());

		// === RINGKASAN dulu (POSISI ATAS) ===
		// Panel funnel calon mahasiswa (PMB): peminat -> bayar -> lulus -> dapat NIM -> jadi mahasiswa.
		html.append(htmlFunnelCalon(selTA));

		// Panel bimbingan/sidang/tugas akhir/akademik.
		html.append(htmlBimbingan(selTA, selSem));

		// === GRAFIK di POSISI BAWAH (setelah semua ringkasan) ===
		html.append(DashboardAkademikHtmlCssHelper.groupedBarChart("Perbandingan Akademik per Prodi",
				"Grafik memperlihatkan aktif, lulus, dosen, calon, cuti, drop out, dan non aktif pada setiap prodi.",
				labels, new String[] { "Aktif", "Lulus", "Dosen", "Calon", "Cuti", "Drop Out", "Non Aktif" }, values));

		return html.toString();
	}

	/** Jalankan SQL agregat 2-kolom (count, dummy) lalu kembalikan kolom-0 sbg int. Gagal-diam -> 0. */
	private static int hitungSatu(String sql) {
		try {
			List<Object[]> r = Common.ambilSql(sql);
			if (r != null && !r.isEmpty()) {
				return toInt(r.get(0), 0);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:613");
		}
		return 0;
	}

	/**
	 * Panel "Bimbingan, Tugas Akhir &amp; Sidang" pada TA/semester terpilih (scope jurusan/fakultas/PT
	 * admin via mahasiswa-&gt;jurusan-&gt;fakultas): (1) jumlah DOSEN yang membimbing/menguji sidang di
	 * skripsi (pembimbing/ketua_sidang/penguji1-5/pembimbing3, distinct), (2) jumlah MAHASISWA tugas
	 * akhir berstatus Disetujui (mahasiswa_request_tugas_akhir), (3) jumlah MAHASISWA bimbingan
	 * akademik (punya KrsMahasiswa) pada TA + paritas semester. Gagal-diam agar tak ganggu kartu utama.
	 */
	private static String htmlBimbingan(String selTA, String selSem) {
		try {
			Tbmuser tbmuser = Common.getCurrentUser();
			Fakultas fakultas = tbmuser == null ? null : tbmuser.ambilFakultas();
			Jurusan jurusan = tbmuser == null ? null : tbmuser.ambilJurusan();
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			String taAman = selTA == null ? "" : selTA.replace("'", "''");
			int parity = Perkuliahan.GANJIL.equalsIgnoreCase(selSem) ? 1 : 0; // KRS: Ganjil=odd, Genap=even

			StringBuilder scope = new StringBuilder();
			if (jurusan != null && jurusan.getId() != null) {
				scope.append("AND m.jurusan = ").append(jurusan.getId()).append(" ");
			}
			if (fakultas != null && fakultas.getId() != null) {
				scope.append("AND j.fakultas = ").append(fakultas.getId()).append(" ");
			}
			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				scope.append("AND f.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
			}
			String join = "INNER JOIN mahasiswa m ON (X.mahasiswa = m.id) INNER JOIN jurusan j ON (m.jurusan = j.id) "
					+ "INNER JOIN fakultas f ON (j.fakultas = f.id) ";

			// 1) Dosen pembimbing/penguji sidang skripsi (DISTINCT dosen) pada TA.
			int dosenSidang = hitungSatu(
					"SELECT COUNT(DISTINCT dz) AS c_dosen, 0 AS c_dummy FROM (SELECT unnest(ARRAY[s.pembimbing, s.ketua_sidang, s.penguji1, s.penguji2, s.penguji3, s.penguji4, s.penguji5, s.pembimbing3]) AS dz "
							+ "FROM skripsi s " + join.replace("X.", "s.") + "WHERE s.tahun_akademik = '" + taAman + "' "
							+ scope + ") z");

			// 2) Mahasiswa tugas akhir berstatus Disetujui pada TA.
			int taDisetujui = hitungSatu(
					"SELECT COUNT(DISTINCT mr.mahasiswa) AS c_ta, 0 AS c_dummy FROM mahasiswa_request_tugas_akhir mr "
							+ join.replace("X.", "mr.") + "WHERE mr.status = 'Disetujui' AND mr.tahun_akademik = '"
							+ taAman + "' " + scope);

			// 3) Mahasiswa bimbingan akademik (punya KRS) pada TA + paritas semester.
			int krsBimbingan = hitungSatu(
					"SELECT COUNT(DISTINCT k.mahasiswa) AS c_krs, 0 AS c_dummy FROM krs_mahasiswa k "
							+ join.replace("X.", "k.") + "WHERE k.tahunakademik = '" + taAman + "' AND (k.semester % 2) = "
							+ parity + " " + scope);

			StringBuffer stats = new StringBuffer();
			stats.append("<div class=\"ais-akad-stats\">");
			stats.append(DashboardAkademikHtmlCssHelper.stat("Dosen Pembimbing/Penguji Sidang",
					DashboardAkademikHtmlCssHelper.fmt(dosenSidang),
					"Jumlah dosen yang membimbing/menguji sidang skripsi pada Tahun Akademik " + selTA + "."));
			stats.append(DashboardAkademikHtmlCssHelper.stat("Mahasiswa Tugas Akhir (Disetujui)",
					DashboardAkademikHtmlCssHelper.fmt(taDisetujui),
					"Mahasiswa dengan pengajuan Tugas Akhir berstatus Disetujui pada Tahun Akademik " + selTA + "."));
			stats.append(DashboardAkademikHtmlCssHelper.stat("Bimbingan Akademik (KRS)",
					DashboardAkademikHtmlCssHelper.fmt(krsBimbingan),
					"Mahasiswa yang memiliki KRS pada Tahun Akademik & semester terpilih (" + selTA + " — " + selSem
							+ ")."));
			stats.append("</div>");

			return DashboardAkademikHtmlCssHelper.panel("Bimbingan, Tugas Akhir & Sidang",
					"Aktivitas bimbingan, tugas akhir, dan sidang pada Tahun Akademik & semester terpilih.",
					stats.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:682");
			// Jangan ganggu render kartu utama.
		}
		return "";
	}

	/**
	 * Panel "Progres Calon Mahasiswa (PMB)" pada Tahun Akademik terpilih: Peminat (pendaftar),
	 * Membayar Formulir, Lulus/Diterima, Dapat NIM, dan Jadi Mahasiswa — meniru footer Total layar
	 * Calon Mhs &gt; Progres (RekapPendaftarSpmbSemua): bayar = kegiatan(pembayaran_registrasi).dibayar&gt;0,
	 * lulus = prodi_lulus not null, dapat NIM = mahasiswa not null. "Jadi Mahasiswa" = jumlah data
	 * mahasiswa angkatan = tahun TA. Di-scope mengikuti jurusan/fakultas/PT admin. Gagal-diam agar
	 * tak mengganggu kartu utama.
	 */
	private static String htmlFunnelCalon(String selTA) {
		try {
			Tbmuser tbmuser = Common.getCurrentUser();
			Fakultas fakultas = tbmuser == null ? null : tbmuser.ambilFakultas();
			Jurusan jurusan = tbmuser == null ? null : tbmuser.ambilJurusan();
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			String taAman = selTA == null ? "" : selTA.replace("'", "''");
			int tahun;
			try {
				tahun = Integer.parseInt(taAman.split("/")[0].trim());
			} catch (Exception e) {
				tahun = 0;
			}

			StringBuilder sql = new StringBuilder();
			sql.append("SELECT COUNT(*) AS c_peminat, ");
			sql.append("SUM(CASE WHEN kr.dibayar > 0.1 THEN 1 ELSE 0 END) AS c_membayar, ");
			sql.append("SUM(CASE WHEN bcm.prodi_lulus IS NOT NULL THEN 1 ELSE 0 END) AS c_lulus, ");
			sql.append("SUM(CASE WHEN bcm.mahasiswa IS NOT NULL THEN 1 ELSE 0 END) AS c_dapatnim ");
			sql.append("FROM biodata_calon_mahasiswa bcm ");
			sql.append("LEFT JOIN kegiatan kr ON (bcm.pembayaran_registrasi = kr.id) ");
			sql.append("LEFT JOIN jurusan jx ON (jx.id = COALESCE(bcm.prodi_lulus, bcm.prodi_1)) ");
			sql.append("LEFT JOIN fakultas fx ON (fx.id = jx.fakultas) ");
			sql.append("WHERE (bcm.aktif = true OR bcm.aktif IS NULL) AND bcm.tahunakademik = '").append(taAman)
					.append("' ");
			if (jurusan != null && jurusan.getId() != null) {
				sql.append("AND COALESCE(bcm.prodi_lulus, bcm.prodi_1) = ").append(jurusan.getId()).append(" ");
			}
			if (fakultas != null && fakultas.getId() != null) {
				sql.append("AND jx.fakultas = ").append(fakultas.getId()).append(" ");
			}
			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				sql.append("AND fx.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
			}
			List<Object[]> r = Common.ambilSql(sql.toString());
			int peminat = 0, membayar = 0, lulus = 0, dapatnim = 0;
			if (r != null && !r.isEmpty()) {
				Object[] o = r.get(0);
				peminat = toInt(o, 0);
				membayar = toInt(o, 1);
				lulus = toInt(o, 2);
				dapatnim = toInt(o, 3);
			}

			// "Jadi Mahasiswa" = jumlah data mahasiswa angkatan = tahun TA (scope sama).
			int jadimhs = 0;
			StringBuilder sql2 = new StringBuilder();
			// 2 kolom agar Common.ambilSql mengembalikan Object[] (query 1-kolom -> skalar, bukan array).
			sql2.append("SELECT COUNT(*) AS c_jadimhs, 0 AS c_dummy FROM mahasiswa m ");
			sql2.append("INNER JOIN jurusan j ON (m.jurusan = j.id) INNER JOIN fakultas f ON (j.fakultas = f.id) ");
			sql2.append("WHERE m.tahunangkatan = ").append(tahun).append(" ");
			if (jurusan != null && jurusan.getId() != null) {
				sql2.append("AND m.jurusan = ").append(jurusan.getId()).append(" ");
			}
			if (fakultas != null && fakultas.getId() != null) {
				sql2.append("AND j.fakultas = ").append(fakultas.getId()).append(" ");
			}
			if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				sql2.append("AND f.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
			}
			List<Object[]> r2 = Common.ambilSql(sql2.toString());
			if (r2 != null && !r2.isEmpty()) {
				jadimhs = toInt(r2.get(0), 0);
			}

			StringBuffer stats = new StringBuffer();
			stats.append("<div class=\"ais-akad-stats\">");
			stats.append(DashboardAkademikHtmlCssHelper.stat("Peminat (Pendaftar)",
					DashboardAkademikHtmlCssHelper.fmt(peminat),
					"Jumlah calon mahasiswa yang mendaftar pada Tahun Akademik " + selTA + "."));
			stats.append(DashboardAkademikHtmlCssHelper.stat("Membayar Formulir",
					DashboardAkademikHtmlCssHelper.fmt(membayar),
					"Calon yang sudah membayar formulir pendaftaran (registrasi)."));
			stats.append(DashboardAkademikHtmlCssHelper.stat("Lulus / Diterima",
					DashboardAkademikHtmlCssHelper.fmt(lulus), "Calon yang lulus/diterima (sudah punya prodi lulus)."));
			stats.append(DashboardAkademikHtmlCssHelper.stat("Dapat NIM", DashboardAkademikHtmlCssHelper.fmt(dapatnim),
					"Calon yang sudah mendapat NIM (tertaut ke data mahasiswa)."));
			stats.append(DashboardAkademikHtmlCssHelper.stat("Jadi Mahasiswa",
					DashboardAkademikHtmlCssHelper.fmt(jadimhs), "Jumlah data mahasiswa angkatan " + tahun + "."));
			stats.append("</div>");

			return DashboardAkademikHtmlCssHelper.panel("Progres Calon Mahasiswa (PMB)",
					"Perjalanan calon mahasiswa pada Tahun Akademik " + selTA
							+ ": mulai dari peminat, yang membayar, yang lulus/diterima, dapat NIM, sampai resmi jadi mahasiswa.",
					stats.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:781");
			// Jangan ganggu render kartu utama bila funnel gagal.
		}
		return "";
	}

	/**
	 * Daftar Tahun Akademik untuk combobox filter, diambil dari nilai distinct di
	 * history_status_mahasiswa (urut terbaru dulu). TA berjalan dipastikan ada & di paling atas.
	 */
	private static List<String> ambilDaftarTahunAkademik(String current) {
		List<String> hasil = new ArrayList<String>();
		try {
			List<Object[]> rows = Common.ambilSql(
					"SELECT tahunakademik, COUNT(*) FROM history_status_mahasiswa WHERE tahunakademik IS NOT NULL AND trim(tahunakademik) <> '' GROUP BY tahunakademik ORDER BY tahunakademik DESC");
			if (rows != null) {
				for (Object[] r : rows) {
					if (r != null && r.length > 0 && r[0] != null) {
						String ta = r[0].toString().trim();
						if (ta.length() > 0 && !hasil.contains(ta)) {
							hasil.add(ta);
						}
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:806");
		}
		if (current != null && current.trim().length() > 0) {
			hasil.remove(current.trim());
			hasil.add(0, current.trim());
		}
		return hasil;
	}

	/**
	 * Konfirmasi lalu jalankan singkronisasi status mahasiswa untuk TA & semester terpilih.
	 */
	private static void konfirmasiDanSingkronkan(final String selTA, final String selSem, final Vbox cardHost)
			throws Exception {
		ais.ui.util.MyMessageboxConfig.show(
				"Singkronkan status mahasiswa untuk Tahun Akademik " + selTA + " semester " + selSem
						+ " ?\n\nProses menghitung ulang status (Aktif/Cuti/Lulus) seperti \"Singkronkan status\" di menu Mahasiswa. Untuk data banyak bisa memakan waktu.",
				"Konfirmasi", ais.ui.util.MyMessageboxConfig.OK | ais.ui.util.MyMessageboxConfig.CANCEL,
				ais.ui.util.MyMessageboxConfig.QUESTION, new org.zkoss.zk.ui.event.EventListener() {
					@Override
					public void onEvent(org.zkoss.zk.ui.event.Event e) throws Exception {
						if (e.getData() != null
								&& Integer.parseInt(e.getData().toString()) == ais.ui.util.MyMessageboxConfig.OK) {
							jalankanSingkron(selTA, selSem, cardHost);
						}
					}
				});
	}

	/**
	 * Menjalankan singkronisasi status di THREAD LATAR (agar UI tidak beku). Progres dipantau
	 * lewat Timer (thread event) yang membaca label bersama; setelah selesai kartu di-render ulang.
	 * Memakai session khusus thread (openSession) untuk MENGAMBIL daftar id mahasiswa; tiap
	 * mahasiswa disinkronkan via HistoryStatusMahasiswaUtil (yang membuka session sendiri).
	 */
	private static void jalankanSingkron(final String selTA, final String selSem, final Vbox cardHost) {
		final String[] progressText = { ais.common.Common.getBahasaConfig("Memulai singkronisasi...") };
		final boolean[] selesai = { false };
		final org.zkoss.zul.Vbox progressBox = new org.zkoss.zul.Vbox();
		progressBox.setStyle("padding:18px 16px;margin:8px 0;border:1px solid #bfdbfe;border-radius:8px;background:#eff6ff;");
		final org.zkoss.zul.Label progressLabel = new org.zkoss.zul.Label(progressText[0]);
		progressLabel.setStyle("font-size:12px;font-weight:bold;color:#1d4ed8;margin-bottom:8px;");
		progressLabel.setParent(progressBox);
		final org.zkoss.zul.Progressmeter progressMeter = new org.zkoss.zul.Progressmeter(0);
		progressMeter.setWidth("440px");
		progressMeter.setStyle("max-width:80%;height:14px;");
		progressMeter.setParent(progressBox);
		try {
			if (cardHost != null) {
				cardHost.getChildren().clear();
				progressBox.setParent(cardHost);
			}
		} catch (Exception ig) {
			ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) ProfileAdminPT:sync-local-progress");
		}
		final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer(400);
		timer.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event e) throws Exception {
				progressLabel.setValue(progressText[0]);
				int pct = parseProgressPersen(progressText[0]);
				if (pct >= 0) {
					progressMeter.setValue(pct);
				}
				if (selesai[0]) {
					timer.detach();
					// Data berubah setelah singkronisasi -> buang cache agar dihitung ulang (fresh).
					try {
						String keyInv = cacheKeyRingkasan(selTA, selSem);
						ais.common.DashboardCacheUtil.invalidateL2(keyInv);
						ais.common.DashboardCacheUtil.invalidateL3(keyInv);
							// Invalidasi (BUKAN rebuild sinkron) -> renderKartu build ulang di LATAR + progress bar.
							ais.common.RingkasanKampusCache.invalidasi(selTA, selSem);
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:862");
					}
					try {
						progressBox.detach();
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) ProfileAdminPT:sync-progress-detach");
					}
					renderKartu(cardHost, selTA, selSem);
					ais.ui.util.MyMessageboxConfig.show("Singkronisasi status selesai.", "Pemberitahuan",
							ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
				}
			}
		});
		timer.start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				org.hibernate.Session session = null;
				try {
					session = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
					List<?> ids = ambilIdMahasiswaScope(session);
					int total = ids == null ? 0 : ids.size();
					int i = 0;
					if (ids != null) {
						for (Object idObj : ids) {
							try {
								if (idObj != null) {
									Long id = ((Number) idObj).longValue();
									ais.database.model.Mahasiswa mhs = (ais.database.model.Mahasiswa) session
											.get(ais.database.model.Mahasiswa.class, id);
									if (mhs != null) {
										ais.action.master.helper.HistoryStatusMahasiswaUtil
												.singkronisasiStatusMahasiswa(null, mhs, selTA, selSem, false);
									}
								}
							} catch (Exception ex) {
								Common.tampilErrorJikaAdmin(ex);
							}
							i++;
							progressText[0] = "Singkronisasi status... (" + i + "/" + total + ")";
						}
					}
				} catch (Exception e1) {
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/profile/ProfileAdminPerguruanTinggiAkademikDashboard.java:901");
				} finally {
					ais.database.hibernate.HibernateUtil.closeSessionQuietly(session);
					selesai[0] = true;
				}
			}
		}).start();
	}

	private static int parseProgressPersen(String text) {
		try {
			if (text == null) {
				return -1;
			}
			int p1 = text.indexOf('(');
			int p2 = text.indexOf('/', p1);
			int p3 = text.indexOf(')', p2);
			if (p1 < 0 || p2 < 0 || p3 < 0) {
				return -1;
			}
			int proses = Integer.parseInt(text.substring(p1 + 1, p2).trim());
			int total = Integer.parseInt(text.substring(p2 + 1, p3).trim());
			if (total <= 0) {
				return 0;
			}
			int pct = (int) (proses * 100L / total);
			return pct < 0 ? 0 : (pct > 100 ? 100 : pct);
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Daftar id mahasiswa aktif sesuai cakupan admin (jurusan/fakultas/PT) — memakai session
	 * yang diberikan (thread latar). Mengembalikan list id (Number).
	 */
	private static List<?> ambilIdMahasiswaScope(org.hibernate.Session session) {
		Tbmuser tbmuser = Common.getCurrentUser();
		Fakultas fakultas = tbmuser == null ? null : tbmuser.ambilFakultas();
		Jurusan jurusan = tbmuser == null ? null : tbmuser.ambilJurusan();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT a.id FROM mahasiswa a ");
		sql.append("INNER JOIN jurusan j ON (a.jurusan = j.id) ");
		sql.append("INNER JOIN fakultas f ON (j.fakultas = f.id) ");
		sql.append("WHERE (a.aktif IS NULL OR a.aktif = true) ");
		if (jurusan != null && jurusan.getId() != null) {
			sql.append("AND a.jurusan = ").append(jurusan.getId()).append(" ");
		}
		if (fakultas != null && fakultas.getId() != null) {
			sql.append("AND j.fakultas = ").append(fakultas.getId()).append(" ");
		}
		if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			sql.append("AND f.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
		}
		return session.createSQLQuery(sql.toString()).list();
	}

	/**
	 * Mengambil data akademik agregat per program studi dalam satu query SQL.
	 *
	 * <p><b>Tujuan:</b> Menggantikan banyak query terpisah (mahasiswa, lulus, dosen, calon,
	 * cuti) dengan satu query JOIN yang efisien, sehingga halaman profil admin PT tetap
	 * responsif meski jumlah prodi banyak.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengambil {@code Tbmuser} saat ini untuk mendapatkan filter fakultas/jurusan.</li>
	 *   <li>Mengambil {@code PerguruanTinggi} via {@code PerguruanTinggiUtil.getPerguruanTinggi()}
	 *       untuk filter multi-tenant.</li>
	 *   <li>Menyusun filter SQL {@code filterAktifSemester}: jika {@code ConstantValues.AKTIF}
	 *       sudah dikonfigurasi, menambahkan subquery ke {@code history_status_mahasiswa}
	 *       untuk membatasi hitungan mahasiswa aktif pada semester dan tahun akademik berjalan;
	 *       jika tidak, fallback tanpa filter status (flag {@code aktif}).</li>
	 *   <li>Membangun query SELECT dengan 7 kolom: id prodi, nama prodi, jumlah mahasiswa
	 *       aktif semester berjalan, jumlah mahasiswa lulus ({@code status_keluar=1}),
	 *       jumlah dosen aktif, jumlah calon mahasiswa aktif per prodi lulus, dan
	 *       jumlah mahasiswa cuti yang disetujui.</li>
	 *   <li>Menambahkan WHERE filter berdasarkan jurusan/fakultas/PT jika melekat pada user.</li>
	 *   <li>Memanggil {@code Common.ambilSql(sql)} yang mengembalikan {@code List<Object[]>}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Keamanan SQL:</b> Filter ID disisipkan langsung ke string SQL via
	 * {@code jurusan.getId()} (Long). Ini aman dari SQL injection karena nilainya pasti
	 * bertipe Long dari entity Hibernate, bukan input pengguna.</p>
	 *
	 * @return daftar array objek per baris: {@code [id, nama, mhsAktif, lulus, dosen, calon, cuti]};
	 *         tidak pernah {@code null} (dapat kosong jika tidak ada prodi aktif)
	 */
	private static List<Object[]> ambilDataPerProdi(String selTA, String selSem) {
		Tbmuser tbmuser = Common.getCurrentUser();
		Fakultas fakultas = tbmuser == null ? null : tbmuser.ambilFakultas();
		Jurusan jurusan = tbmuser == null ? null : tbmuser.ambilJurusan();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		/*
		 * DISAMAKAN dengan laporan "Rekap Jumlah Mahasiswa" (LaporanRekapJumlahMahasiswa): untuk tiap
		 * mahasiswa (berflag aktif & jenis kelamin valid) dihitung SEMESTER BERJALAN-nya memakai rumus
		 * Common.getSemester [smt = mulaiSemester + ((tahun*2 + musimSekarang) - (angkatan*2 + musimMasuk))],
		 * lalu diambil status TERBARU pada (tahunakademik = TA terpilih, semester = smt) dari
		 * history_status_mahasiswa, dikategorikan via ID status (AKTIF/CUTI/LULUS/DROP_OUT/TIDAK_AKTIF).
		 * Catatan: mulaiSemester (pindahKeKampusIniMasukSemester) diasumsikan 1 (non-pindahan = mayoritas).
		 * Agar PERSIS sama dengan rekap, history_status_mahasiswa harus sudah ter-singkron (tombol Singkronkan).
		 * Dosen Aktif = dosen yang punya JADWAL (perkuliahan) pada TA+semester terpilih (per jurusan home dosen).
		 * Calon = biodata_calon_mahasiswa pada TA terpilih.
		 */
		long aktifId = ais.common.ConstantValues.AKTIF == null ? -1L : ais.common.ConstantValues.AKTIF.getId();
		long lulusId = ais.common.ConstantValues.LULUS == null ? -1L : ais.common.ConstantValues.LULUS.getId();
		long cutiId = ais.common.ConstantValues.CUTI == null ? -1L : ais.common.ConstantValues.CUTI.getId();
		long dropoutId = ais.common.ConstantValues.DROP_OUT == null ? -1L : ais.common.ConstantValues.DROP_OUT.getId();
		long tidakAktifId = ais.common.ConstantValues.TIDAK_AKTIF == null ? -1L
				: ais.common.ConstantValues.TIDAK_AKTIF.getId();

		String taAman = selTA == null ? "" : selTA.replace("'", "''");
		boolean genap = Perkuliahan.GENAP.equalsIgnoreCase(selSem);
		int perkuliahanParity = genap ? 0 : 1; // perkuliahan.semester % 2 (Ganjil=1/odd, Genap=0/even)

		StringBuilder sql = new StringBuilder();
		// WAJIB alias UNIK tiap kolom: Common.ambilSql memetakan hasil native query per NAMA kolom;
		// tanpa alias, ke-7 COALESCE bernama sama ("coalesce") -> Hibernate mengembalikan nilai kolom
		// pertama untuk SEMUA posisi (menyebabkan semua angka jadi sama).
		sql.append("SELECT j.id AS j_id, j.nama AS j_nama, ");
		sql.append("COALESCE(st.aktif, 0) AS c_aktif, COALESCE(st.lulus, 0) AS c_lulus, COALESCE(dos.jumlah, 0) AS c_dosen, ");
		sql.append("COALESCE(c.jumlah, 0) AS c_calon, COALESCE(st.cuti, 0) AS c_cuti, COALESCE(st.dropout, 0) AS c_dropout, COALESCE(st.tidakaktif, 0) AS c_tidakaktif ");
		sql.append("FROM jurusan j ");
		sql.append("INNER JOIN fakultas f ON (j.fakultas = f.id) ");
		// Status TERBARU tiap mahasiswa pada semester berjalannya (LATERAL: 1 status per mahasiswa).
		sql.append("LEFT JOIN (SELECT a.jurusan AS jurusan, ");
		// AKTIF wajib status_keluar IS NULL (mahasiswa yang sudah keluar/lulus/DO tak dihitung aktif
		// walau record history terakhirnya kebetulan AKTIF).
		sql.append("SUM(CASE WHEN hh.status_mahasiswa = ").append(aktifId)
				.append(" AND a.status_keluar IS NULL THEN 1 ELSE 0 END) AS aktif, ");
		sql.append("SUM(CASE WHEN hh.status_mahasiswa = ").append(lulusId).append(" THEN 1 ELSE 0 END) AS lulus, ");
		sql.append("SUM(CASE WHEN hh.status_mahasiswa = ").append(cutiId).append(" THEN 1 ELSE 0 END) AS cuti, ");
		sql.append("SUM(CASE WHEN hh.status_mahasiswa = ").append(dropoutId).append(" THEN 1 ELSE 0 END) AS dropout, ");
		sql.append("SUM(CASE WHEN hh.status_mahasiswa = ").append(tidakAktifId)
				.append(" THEN 1 ELSE 0 END) AS tidakaktif ");
		sql.append("FROM mahasiswa a ");
		// Status TERBARU (terminal/saat ini) tiap mahasiswa SAMPAI DENGAN TA terpilih: ambil record
		// history_status_mahasiswa paling baru (tahunakademik <= selTA, urut TA & semester & id menurun).
		// Menyamai cara rekap (currentStatus): lulusan->LULUS, DO->DROP_OUT, cuti->CUTI, aktif->AKTIF,
		// dan KEBAL terhadap selisih rumus semester (sebab tidak meng-key ke nomor semester tertentu).
		// WAJIB sudah di-Singkronkan agar status mutakhir tercatat di history.
		sql.append("JOIN LATERAL (SELECT h.status_mahasiswa FROM history_status_mahasiswa h ");
		sql.append("WHERE h.mahasiswa = a.id AND h.tahunakademik <= '").append(taAman)
				.append("' ORDER BY h.tahunakademik DESC, h.semester DESC, h.id DESC LIMIT 1) hh ON true ");
		sql.append("WHERE (a.aktif IS NULL OR a.aktif = true) AND lower(trim(COALESCE(a.kelamin,''))) IN ('laki-laki','perempuan') ");
		sql.append("GROUP BY a.jurusan) st ON (st.jurusan = j.id) ");
		// Dosen yang punya JADWAL (perkuliahan) di TA+semester terpilih -> per jurusan home dosen.
		sql.append("LEFT JOIN (SELECT d.jurusan, COUNT(DISTINCT d.id) jumlah FROM dosen d WHERE d.id IN (");
		sql.append("SELECT dosen1 FROM perkuliahan WHERE tahun_ajaran = '").append(taAman)
				.append("' AND (semester % 2) = ").append(perkuliahanParity).append(" AND dosen1 IS NOT NULL ");
		sql.append("UNION SELECT dosen2 FROM perkuliahan WHERE tahun_ajaran = '").append(taAman)
				.append("' AND (semester % 2) = ").append(perkuliahanParity).append(" AND dosen2 IS NOT NULL ");
		sql.append("UNION SELECT dosen3 FROM perkuliahan WHERE tahun_ajaran = '").append(taAman)
				.append("' AND (semester % 2) = ").append(perkuliahanParity).append(" AND dosen3 IS NOT NULL");
		sql.append(") GROUP BY d.jurusan) dos ON (dos.jurusan = j.id) ");
		// Calon mahasiswa pada TA terpilih.
		sql.append("LEFT JOIN (SELECT prodi_lulus, COUNT(*) jumlah FROM biodata_calon_mahasiswa WHERE (aktif = true OR aktif IS NULL) AND tahunakademik = '")
				.append(taAman).append("' GROUP BY prodi_lulus) c ON (c.prodi_lulus = j.id) ");
		sql.append("WHERE j.aktif = true ");

		if (jurusan != null && jurusan.getId() != null) {
			sql.append("AND j.id = ").append(jurusan.getId()).append(" ");
		}
		if (fakultas != null && fakultas.getId() != null) {
			sql.append("AND j.fakultas = ").append(fakultas.getId()).append(" ");
		}
		if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			sql.append("AND f.perguruan_tinggi = ").append(perguruanTinggi.getId()).append(" ");
		}
		sql.append("ORDER BY j.nama ASC");

		return Common.ambilSql(sql.toString());
	}

	/**
	 * Mengambil nilai integer aman dari array Object[] hasil query SQL.
	 *
	 * <p><b>Tujuan:</b> Menghindari NullPointerException dan ClassCastException saat
	 * membaca nilai numerik dari baris hasil query SQL yang dikembalikan sebagai
	 * {@code Object[]}. Query SQL kadang mengembalikan {@code null} untuk kolom
	 * COALESCE atau kolom yang tidak memiliki baris terkait.</p>
	 *
	 * <p><b>Cara kerja:</b> Memeriksa apakah {@code obj} tidak null, panjangnya cukup,
	 * dan nilai di index tidak null. Jika lolos, melakukan cast ke {@code Number} dan
	 * memanggil {@code intValue()}. Exception saat cast (mis. nilai bertipe aneh)
	 * diswallow dan mengembalikan 0.</p>
	 *
	 * @param obj array Object hasil satu baris query; boleh {@code null}
	 * @param idx indeks kolom yang akan dibaca (0-based)
	 * @return nilai integer dari kolom tersebut, atau {@code 0} jika null/tidak valid
	 */
	private static int toInt(Object[] obj, int idx) {
		if (obj == null || obj.length <= idx || obj[idx] == null) {
			return 0;
		}
		try {
			return ((Number) obj[idx]).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * Deteksi paritas semester berjalan (0=Genap, 1=Ganjil) untuk tahun akademik tertentu.
	 *
	 * Urutan prioritas:
	 * 1. RencanaTahunAkademik yang sedang aktif (tanggalMulai <= sekarang <= tanggalSampai)
	 * 2. RencanaTahunAkademik yang paling baru berakhir untuk TA ini (menghindari kesalahan
	 *    fallback tanggal saat periode telah selesai tapi semester berikutnya belum mulai)
	 * 3. Fallback tanggal: bulan >= 7 (Agustus) = Ganjil; bulan < 7 = Genap
	 *    Catatan: fallback bawaan isNowSemensterGanjil() memakai >= 5 (Juni) yang terlalu
	 *    awal — Juni-Juli masih Genap di hampir semua kalender akademik Indonesia.
	 */
	private static int ambilParitasSemesterBerjalan(String taSekarang) {
		Date sekarang = ais.ui.util.WaktuUtil.getDate();

		// 1. Periode akademik yang sedang aktif
		RencanaTahunAkademik rtaAktif = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(
				Common.getCurrentUser(), sekarang);
		if (rtaAktif != null) {
			return Perkuliahan.GANJIL.equals(rtaAktif.getSemester()) ? 1 : 0;
		}

		// 2. RTA paling baru yang sudah berakhir untuk TA ini
		if (taSekarang != null && Common.rencanaTahunAkademiks != null) {
			RencanaTahunAkademik mostRecent = null;
			for (RencanaTahunAkademik rta : Common.rencanaTahunAkademiks) {
				if (rta == null || rta.getTanggalSampai() == null) continue;
				if (!taSekarang.equals(rta.getNama())) continue;
				if (!rta.getTanggalSampai().before(sekarang)) continue; // belum berakhir
				if (mostRecent == null || rta.getTanggalSampai().after(mostRecent.getTanggalSampai())) {
					mostRecent = rta;
				}
			}
			if (mostRecent != null) {
				return Perkuliahan.GANJIL.equals(mostRecent.getSemester()) ? 1 : 0;
			}
		}

		// 3. Fallback tanggal (lebih konservatif: Ganjil mulai Agustus, bukan Juni)
		Calendar cal = Calendar.getInstance();
		cal.setTime(sekarang);
		return cal.get(Calendar.MONTH) >= 7 ? 1 : 0; // 7 = Agustus
	}

	/**
	 * Memformat rasio mahasiswa aktif berbanding dosen aktif sebagai string "N : 1".
	 *
	 * <p><b>Tujuan:</b> Menyajikan rasio dalam format yang mudah dibaca pada kartu
	 * "Rasio Mhs/Dosen". Rasio ini umum dipakai pimpinan PT untuk memonitor beban
	 * mengajar dosen — makin kecil angka N, makin ideal.</p>
	 *
	 * <p><b>Cara kerja:</b> Jika {@code dosen &lt;= 0}, mengembalikan {@code "-"} untuk
	 * menghindari pembagian dengan nol. Sebaliknya menghitung
	 * {@code round(mahasiswa / dosen)} dan memformat sebagai {@code "N : 1"}.</p>
	 *
	 * @param mahasiswa jumlah mahasiswa aktif semester berjalan (total semua prodi)
	 * @param dosen     jumlah dosen aktif (total semua prodi)
	 * @return string format rasio mis. {@code "15 : 1"}, atau {@code "-"} jika dosen 0
	 */
	private static String rasio(int mahasiswa, int dosen) {
		if (dosen <= 0) {
			return "-";
		}
		int r = (int) Math.round((double) mahasiswa / (double) dosen);
		return r + " : 1";
	}

	/**
	 * Membangun konten HTML untuk popup penjelasan cara menghitung Rasio Mhs/Dosen.
	 *
	 * <p><b>Tujuan:</b> Memberikan transparansi pada pimpinan PT tentang definisi
	 * dan cara perhitungan angka rasio yang ditampilkan di kartu "Rasio Mhs/Dosen".
	 * Popup ini membedakan "mahasiswa aktif semester berjalan" (bukan total semua
	 * angkatan) dan "dosen aktif" yang digunakan sebagai pembilang dan penyebut.</p>
	 *
	 * <p><b>Cara kerja:</b> Membangun string HTML dengan:
	 * <ul>
	 *   <li>Paragraf penjelasan teks rasio.</li>
	 *   <li>Blok formula: rumus, substitusi nilai aktual, hasil desimal, dan nilai bulat.</li>
	 *   <li>Paragraf definisi "Mahasiswa Aktif" (semester dan tahun akademik berjalan).</li>
	 *   <li>Paragraf definisi "Dosen Aktif".</li>
	 *   <li>Catatan hak akses (per fakultas/prodi).</li>
	 * </ul>
	 * Nilai diformat via {@code DashboardAkademikHtmlCssHelper.fmt} (format angka ribuan)
	 * dan tahun akademik/semester diambil dari {@code Common}.</p>
	 *
	 * @param mahasiswa jumlah mahasiswa aktif yang dipakai di pembilang
	 * @param dosen     jumlah dosen aktif yang dipakai di penyebut
	 * @return string HTML konten popup; tidak pernah {@code null}
	 */
	private static String binaModalRasio(int mahasiswa, int dosen) {
		String ta = Common.getCurrentTahunAkademik();
		String smt = ambilParitasSemesterBerjalan(ta) == 1 ? "Ganjil" : "Genap";
		StringBuffer sb = new StringBuffer();
		sb.append("<p><b>Rasio Mhs/Dosen</b> membandingkan jumlah <b>mahasiswa aktif</b> dengan jumlah ")
				.append("<b>dosen aktif</b> &mdash; makin kecil angka di kiri, makin ideal beban dosen.</p>");
		sb.append("<div class=\"ais-akad-formula\">");
		sb.append("Rasio = Mahasiswa Aktif &divide; Dosen Aktif<br/>");
		sb.append("= ").append(DashboardAkademikHtmlCssHelper.fmt(mahasiswa)).append(" &divide; ")
				.append(DashboardAkademikHtmlCssHelper.fmt(dosen));
		if (dosen > 0) {
			double exact = (double) mahasiswa / (double) dosen;
			int bulat = (int) Math.round(exact);
			sb.append(" = ").append(new java.text.DecimalFormat("#,##0.0").format(exact));
			sb.append(" &asymp; <b>").append(bulat).append(" : 1</b>");
		} else {
			sb.append(" = <b>-</b> (dosen aktif 0)");
		}
		sb.append("</div>");
		sb.append("<p><b>Mahasiswa Aktif</b> dihitung dari mahasiswa berstatus <b>Aktif</b> pada <b>Tahun Akademik ")
				.append(DashboardAkademikHtmlCssHelper.esc(ta)).append("</b> semester <b>").append(smt)
				.append("</b> (diambil dari riwayat status mahasiswa), <b>bukan</b> total seluruh mahasiswa lintas angkatan.</p>");
		sb.append("<p><b>Dosen Aktif</b> = jumlah dosen (orang) yang memiliki jadwal mengajar pada Tahun Akademik ")
				.append("berjalan. Seorang dosen yang mengajar di lebih dari satu prodi dihitung <b>SEKALI</b>, ")
				.append("dan dosen tanpa SKS/jadwal pada semester berjalan <b>tidak</b> dihitung &mdash; sehingga ")
				.append("rasio tingkat universitas tidak menggelembung.</p>");
		sb.append("<p style=\"color:#64748b;font-size:11px;\">Jumlah mahasiswa dihitung per prodi lalu dijumlahkan; ")
				.append("jumlah dosen dihitung DISTINCT se-universitas (tidak dijumlahkan antar prodi).</p>");
		return sb.toString();
	}

	/**
	 * Membangun konten HTML untuk popup rincian per program studi dari satu metrik angka.
	 *
	 * <p><b>Tujuan:</b> Menampilkan tabel rincian nilai kartu "Angka Penting" (mis.
	 * "Mahasiswa Aktif") dipecah per program studi. Data diambil dari koleksi yang sudah
	 * dimuat di {@link #append(Rows)} tanpa query tambahan ke database (reuse).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menampilkan keterangan pendek jika tidak kosong.</li>
	 *   <li>Iterasi {@code labels} dan {@code values} secara sejajar; mengambil nilai
	 *       di kolom {@code idx} dari {@code values.get(i)} untuk setiap prodi.</li>
	 *   <li>Prodi dengan nilai 0 disembunyikan agar tabel tidak panjang dengan
	 *       baris kosong.</li>
	 *   <li>Baris diurut descending berdasarkan jumlah menggunakan
	 *       {@code Collections.sort} dengan {@code Comparator} anonim (Java 1.7).</li>
	 *   <li>Membangun tabel HTML dengan header "Program Studi | Jumlah", baris data,
	 *       dan footer "Total" dari parameter {@code total}.</li>
	 *   <li>Menambahkan catatan bahwa daftar rinci individu tersedia di menu terkait.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Indeks kolom {@code idx}:</b>
	 * <ul>
	 *   <li>0 = Mahasiswa aktif semester berjalan</li>
	 *   <li>1 = Mahasiswa lulus</li>
	 *   <li>2 = Dosen aktif</li>
	 *   <li>3 = Calon mahasiswa</li>
	 *   <li>4 = Mahasiswa cuti</li>
	 * </ul>
	 * </p>
	 *
	 * @param keterangan teks penjelasan singkat di atas tabel; boleh {@code null} atau kosong
	 * @param labels     daftar nama program studi (sejajar dengan {@code values})
	 * @param values     daftar array int per prodi (5 elemen: mahasiswa/lulus/dosen/calon/cuti)
	 * @param idx        indeks kolom metrik yang akan ditampilkan (0–4)
	 * @param total      total semua prodi untuk kolom tersebut (ditampilkan di footer)
	 * @return string HTML konten popup; tidak pernah {@code null}
	 */
	private static String binaModalRincian(String keterangan, List<String> labels, List<int[]> values, int idx,
			int total) {
		StringBuffer sb = new StringBuffer();
		if (keterangan != null && keterangan.trim().length() > 0) {
			sb.append("<p>").append(DashboardAkademikHtmlCssHelper.esc(keterangan)).append("</p>");
		}

		List<int[]> baris = new ArrayList<int[]>();
		if (labels != null && values != null) {
			for (int i = 0; i < labels.size(); i++) {
				int[] row = i < values.size() ? values.get(i) : null;
				int v = row != null && idx >= 0 && idx < row.length ? row[idx] : 0;
				if (v > 0) {
					baris.add(new int[] { i, v });
				}
			}
		}
		java.util.Collections.sort(baris, new java.util.Comparator<int[]>() {
			public int compare(int[] a, int[] b) {
				return b[1] - a[1];
			}
		});

		if (baris.isEmpty()) {
			sb.append("<div class=\"ais-akad-empty\">Belum ada data untuk ditampilkan.</div>");
			return sb.toString();
		}

		sb.append("<p>Rincian per program studi (menjumlah menjadi <b>")
				.append(DashboardAkademikHtmlCssHelper.fmt(total)).append("</b>):</p>");
		sb.append("<table class=\"ais-akad-mtbl\"><thead><tr><th>Program Studi</th><th>Jumlah</th></tr></thead><tbody>");
		for (int k = 0; k < baris.size(); k++) {
			int[] b = baris.get(k);
			sb.append("<tr><td>").append(DashboardAkademikHtmlCssHelper.esc(labels.get(b[0]))).append("</td><td>")
					.append(DashboardAkademikHtmlCssHelper.fmt(b[1])).append("</td></tr>");
		}
		sb.append("</tbody><tfoot><tr><td>Total</td><td>").append(DashboardAkademikHtmlCssHelper.fmt(total))
				.append("</td></tr></tfoot></table>");
		sb.append("<p style=\"color:#64748b;font-size:11px;\">Untuk daftar rinci per individu, gunakan menu terkait ")
				.append("(Mahasiswa, Dosen, Calon Mahasiswa, dll).</p>");
		return sb.toString();
	}
}
