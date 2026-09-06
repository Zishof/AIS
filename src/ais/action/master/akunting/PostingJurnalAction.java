package ais.action.master.akunting;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;

import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyInclude;
import ais.ui.util.MyWindow;

/**
 * <h3>PostingJurnalAction — Halaman Utama Posting Jurnal (Multi-Tab)</h3>
 *
 * <p><b>Untuk apa:</b> Controller ZK yang mengelola halaman "Posting Jurnal" —
 * sebuah halaman multi-tab yang mengorganisir semua jenis posting jurnal akuntansi
 * dalam satu antarmuka terpadu. Setiap tab mewakili kategori transaksi keuangan
 * yang berbeda: Jurnal Umum, Gaji, Pajak, Uang Muka dan Kas Kecil, Transaksi Vendor,
 * Siswa dan Mahasiswa, Jurnal Penyusutan Aset, Pengajuan Transfer, Transitori, dan
 * Closing (penutupan periode akuntansi). Ini adalah titik masuk utama bagi staf
 * akuntansi untuk melakukan posting dari berbagai sumber transaksi ke jurnal resmi.</p>
 *
 * <p><b>Cara kerja:</b> Halaman ini menggunakan pola "lazy loading" untuk tab — konten
 * setiap tab tidak dimuat sampai pengguna mengklik tab tersebut. Saat tab diklik, ZK
 * memanggil event handler yang sesuai (misalnya {@code onJurnalUmum}, {@code onGaji},
 * dst.), yang kemudian memanggil metode {@link #loadPanel(Tabpanel, String)} dengan
 * {@code Tabpanel} yang bersangkutan dan URL ZUL yang harus dimuat ke dalamnya.
 * {@link #loadPanel} memeriksa apakah panel sudah terisi (guard idempoten) — jika
 * sudah, tidak dimuat ulang. Ini menghemat waktu dan mencegah duplikasi komponen.
 * Setiap tab dimuat dalam {@code MyWindow} yang dibungkus {@code MyInclude} (ZK
 * include component), sehingga setiap sub-halaman memiliki action controller-nya
 * sendiri yang terpisah.</p>
 *
 * <p><b>Daftar tab dan halaman ZUL yang dimuat:</b>
 * <ul>
 *   <li><b>Jurnal Umum</b>: {@code /pages/master/akunting/grup_transaksi.zul}</li>
 *   <li><b>Gaji</b>: {@code .../payroll/posting_transaksi_transaksi_penggajian.zul}</li>
 *   <li><b>Pajak</b>: {@code .../akunting/posting_pertanggunjawaban_pajak.zul}</li>
 *   <li><b>Uang Muka dan Kas Kecil</b>: {@code .../akunting/uang_muka_dan_kas_kecil.zul}</li>
 *   <li><b>Transaksi Vendor</b>: {@code .../akunting/transaksi_vendor.zul}</li>
 *   <li><b>Siswa dan Mahasiswa</b>: {@code .../akunting/transaksi_siswa_dan_mahasiswa.zul}</li>
 *   <li><b>Jurnal Penyusutan</b>: {@code .../asset/posting_penyusutan_asset.zul}</li>
 *   <li><b>Pengajuan Transfer</b>: {@code .../akunting/posting_proses_transfer.zul}</li>
 *   <li><b>Transitori</b>: {@code .../akunting/posting_proses_transitori.zul}</li>
 *   <li><b>Closing</b>: {@code .../akunting/closing.zul}</li>
 * </ul>
 * </p>
 *
 * <p><b>Threading:</b> Semua metode dijalankan di event thread ZK. Tidak ada
 * threading manual. Komponen ZK (Tabpanel, MyWindow, MyInclude) hanya aman
 * diakses dari event thread.</p>
 *
 * <p><b>Pemeliharaan:</b> Untuk menambah tab baru, deklarasikan field {@code Tabpanel}
 * baru, buat event handler {@code onXxx(Event)}, dan panggil {@code loadPanel}
 * dengan URL ZUL yang sesuai. Pastikan juga mendaftarkan tab di ZUL induk
 * ({@code posting_jurnal.zul}) dengan {@code id} yang cocok untuk autowire ZK.
 * Java 1.7, ZKoss 5.5.</p>
 */
public class PostingJurnalAction extends GenericAutowireComposer {

	/**
	 * ID serialisasi yang dibutuhkan karena mewarisi {@code Serializable} via ZK.
	 */
	private static final long serialVersionUID = 8614186058966291087L;

	private Tabpanel jurnalUmum;
	private Tabpanel gaji;
	private Tabpanel pajak;
	private Tabpanel uangMukadanKasKecil;
	private Tabpanel transaksiVendor;
	private Tabpanel siswadanMahasiswa;
	private Tabpanel jurnalPenyusutan;
	private Tabpanel jurnalPengajuanTransfer;
	private Tabpanel transitori;
	private Tabpanel closing;
	private Tabpanel postingHpp;

	// Field Tab (bukan Tabpanel) khusus dipakai utk toggle tampil/sembunyi per Konfigurasi
	// ON/OFF (lihat {@link #terapkanKonfigurasiTab}) -- id-nya di-wire dari posting_jurnal.zul.
	private Tab tabDraftJurnal;
	private Tab tabJurnalUmum;
	private Tab tabUangMukadanKasKecil;
	private Tab tabPajak;
	private Tab tabTransaksiVendor;
	private Tab tabGaji;
	private Tab tabSiswadanMahasiswa;
	private Tab tabJurnalPenyusutan;
	private Tab tabJurnalPengajuanTransfer;
	private Tab tabTransitori;
	private Tab tabClosing;
	private Tab tabPostingHpp;

	/**
	 * Daftar tab Posting Jurnal: {slug konfigurasi, label, path ZUL}.
	 *
	 * <p>Sengaja publik dan tunggal: kontrak native menyajikan daftar yang SAMA
	 * agar tab pada layar native tidak pernah menyimpang dari layar ZK. Tiap
	 * baris tetap tunduk pada saklar Konfigurasi
	 * {@code POSTING_JURNAL_TAB_PREFIX + slug}; urutannya adalah urutan tampil.</p>
	 */
	public static final String[][] TABS = {
			{ "draft_jurnal", "Draft Jurnal", "/WEB-INF/z/x/y/pages/master/akunting/draft_jurnal.zul" },
			{ "jurnal_umum", "Jurnal Umum", "/WEB-INF/z/x/y/pages/master/akunting/grup_transaksi.zul" },
			{ "uang_muka_kas", "Uang Muka dan Kas", "/WEB-INF/z/x/y/pages/master/akunting/uang_muka_dan_kas_kecil.zul" },
			{ "pajak", "Pajak", "/WEB-INF/z/x/y/pages/master/akunting/posting_pertanggunjawaban_pajak.zul" },
			{ "transaksi_vendor", "Transaksi Vendor", "/WEB-INF/z/x/y/pages/master/akunting/transaksi_vendor.zul" },
			{ "gaji", "Gaji", "/WEB-INF/z/x/y/pages/master/payroll/posting_transaksi_transaksi_penggajian.zul" },
			{ "siswa_mahasiswa", "Siswa dan Mahasiswa", "/WEB-INF/z/x/y/pages/master/akunting/transaksi_siswa_dan_mahasiswa.zul" },
			{ "penyusutan", "Penyusutan", "/WEB-INF/z/x/y/pages/master/asset/posting_penyusutan_tabs.zul" },
			{ "pengajuan_transfer", "Pengajuan Transfer", "/WEB-INF/z/x/y/pages/master/akunting/posting_proses_transfer.zul" },
			{ "transitori", "Transitori", "/WEB-INF/z/x/y/pages/master/akunting/posting_proses_transitori.zul" },
			{ "closing", "Closing", "/WEB-INF/z/x/y/pages/master/akunting/closing.zul" },
			{ "posting_hpp", "Posting HPP", "/WEB-INF/z/x/y/pages/master/koperasi/posting_hpp_kantin.zul" },
			{ "posting_penjualan", "Posting Penjualan", "/WEB-INF/z/x/y/pages/master/koperasi/posting_penjualan_kantin.zul" },
			// Empat posting penutup rantai pengadaan->pembayaran toko (2026-08-20). Tanpa ini akun
			// Persediaan hanya pernah dikredit jurnal HPP dan Utang Usaha tak pernah terbentuk.
			{ "posting_kulakan", "Posting Kulakan", "/WEB-INF/z/x/y/pages/master/koperasi/posting_kulakan_toko.zul" },
			{ "posting_bayar_hutang", "Posting Bayar Hutang", "/WEB-INF/z/x/y/pages/master/koperasi/posting_bayar_hutang_toko.zul" },
			{ "posting_terima_piutang", "Posting Terima Piutang", "/WEB-INF/z/x/y/pages/master/koperasi/posting_terima_piutang_toko.zul" },
			{ "posting_penyesuaian", "Posting Penyesuaian", "/WEB-INF/z/x/y/pages/master/koperasi/posting_penyesuaian_toko.zul" },
			// Empat tab khusus Apotik. Jangan diarahkan ke halaman Kantin: sumber transaksi,
			// penanda idempotensi, dan pemetaan akunnya terpisah.
			{ "apotik_posting_penjualan", "Apotik - Posting Penjualan", "/WEB-INF/z/x/y/pages/master/sirs/posting_penjualan_apotik.zul" },
			{ "apotik_posting_hpp", "Apotik - Posting HPP", "/WEB-INF/z/x/y/pages/master/sirs/posting_hpp_apotik.zul" },
			{ "apotik_posting_pbf", "Apotik - Posting Penerimaan PBF", "/WEB-INF/z/x/y/pages/master/sirs/posting_pbf_apotik.zul" },
			{ "apotik_posting_bayar_hutang_pbf", "Apotik - Posting Bayar Utang PBF", "/WEB-INF/z/x/y/pages/master/sirs/posting_bayar_hutang_pbf_apotik.zul" },
			// Siklus akuntansi: saldo awal, penyesuaian berkala, dan tutup buku (2026-08-20).
			{ "saldo_awal", "Saldo Awal", "/WEB-INF/z/x/y/pages/master/koperasi/siklus_saldo_awal.zul" },
			{ "jurnal_penyesuaian", "Jurnal Penyesuaian", "/WEB-INF/z/x/y/pages/master/koperasi/siklus_penyesuaian.zul" },
			{ "tutup_buku_laba_ditahan", "Tutup Buku", "/WEB-INF/z/x/y/pages/master/koperasi/siklus_tutup_buku.zul" },
	};

	/**
	 * Memeriksa hak akses pengguna sebelum halaman dirender oleh ZK framework.
	 *
	 * <p><b>Tujuan:</b> Mencegah akses tidak sah ke halaman Posting Jurnal dengan
	 * memverifikasi hak akses pengguna yang sedang login sebelum komponen ZK
	 * diinisialisasi. Jika pengguna tidak memiliki hak akses yang cukup,
	 * {@link Common#doCheckSecurity()} akan melempar exception atau mengarahkan
	 * pengguna ke halaman error, sehingga halaman tidak pernah ditampilkan.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} yang memeriksa
	 * role dan privilege pengguna dari sesi aktif, lalu meneruskan ke implementasi
	 * induk {@code super.doBeforeCompose} yang menyelesaikan fase pra-komposisi ZK
	 * (wire-up metadata, dsb.).</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika logika keamanan halaman ini perlu dikustomisasi
	 * (misalnya pembatasan berdasarkan cabang atau unit kerja), tambahkan pemeriksaan
	 * setelah {@code doCheckSecurity()} namun sebelum {@code super.doBeforeCompose}.</p>
	 *
	 * @param page     halaman ZK yang sedang dikomposisi
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen dari framework ZK
	 * @return informasi komponen yang dikembalikan oleh implementasi induk
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Menyelesaikan inisialisasi controller setelah semua komponen ZK tersedia.
	 *
	 * <p>Membangun {@link MyButtonTabbox} di dalam {@code mainContainer}, lalu menambahkan
	 * setiap tab secara lazy. Tab yang dimatikan admin lewat Konfigurasi tidak ditambahkan
	 * sama sekali (setara dengan perilaku {@link #terapkanKonfigurasiTab()} sebelumnya).
	 * Urutan dan slug konfigurasi identik dengan implementasi sebelumnya.</p>
	 *
	 * @param comp komponen root halaman yang sudah selesai dikompilasi
	 * @throws Exception jika inisialisasi induk gagal
	 */
	public void doAfterCompose(Component comp) throws Exception {
		Div mainContainer = (Div) comp.getFellow("mainContainer");
		int[] tabAktif = {0};
		MyButtonTabbox btabs = MyButtonTabbox.buat(mainContainer, "100%", tabAktif);
		int idx = 0;

		final String P = Konfigurasi.POSTING_JURNAL_TAB_PREFIX;
		for (int i = 0; i < TABS.length; i++) {
			if (Common.bolehKonfigurasi(P + TABS[i][0]) && bolehMelihatTab(TABS[i][0])) {
				btabs.tambahTabZul(idx++, TABS[i][1], TABS[i][2]);
			}
		}

		super.doAfterCompose(comp);
		btabs.pulihkanSeleksi(idx);
	}

	/** Tab Apotik mengikuti hak view role eBisnis; tab lain mempertahankan perilaku lama. */
	private boolean bolehMelihatTab(String slug) {
		if (slug == null || !slug.startsWith("apotik_posting_")) return true;
		ais.database.model.Tbmuser pengguna = Common.getCurrentUser();
		if (Common.getApakahAdminLain(pengguna)) return true;
		ais.database.model.Tbmrole role = pengguna == null ? null : pengguna.hakAkses();
		if (role == null) return true;
		String kunci = "posting_penjualan";
		if ("apotik_posting_hpp".equals(slug)) kunci = "posting_hpp";
		else if ("apotik_posting_pbf".equals(slug)) kunci = "posting_kulakan";
		else if ("apotik_posting_bayar_hutang_pbf".equals(slug)) kunci = "posting_bayar_hutang";
		return ais.common.EbisnisMenuKatalog.bolehAksiAkuntansi(
				role.getEbisnisMenu(), role.getRoleId(), kunci, "view");
	}

	/**
	 * Sembunyikan tiap tab yang dimatikan admin lewat Konfigurasi &gt; Posting Jurnal
	 * (kunci {@code Konfigurasi.POSTING_JURNAL_TAB_PREFIX + "<slug>"}, lihat
	 * {@code KonfigurasiNewAction.initTabPostingJurnal()}). Semua default AKTIF (tampil) — hanya
	 * tab yang eksplisit dimatikan admin yang disembunyikan. Bila tab "Draft Jurnal" (tab yang
	 * terpilih di awal) ikut disembunyikan, pilih tab pertama yang masih tampil sebagai gantinya
	 * supaya halaman tidak terbuka tanpa tab aktif.
	 */
	private void terapkanKonfigurasiTab() {
		String P = Konfigurasi.POSTING_JURNAL_TAB_PREFIX;
		Tab[] semuaTab = { tabDraftJurnal, tabJurnalUmum, tabUangMukadanKasKecil, tabPajak, tabTransaksiVendor,
				tabGaji, tabSiswadanMahasiswa, tabJurnalPenyusutan, tabJurnalPengajuanTransfer, tabTransitori,
				tabClosing, tabPostingHpp };
		String[] slug = { "draft_jurnal", "jurnal_umum", "uang_muka_kas", "pajak", "transaksi_vendor", "gaji",
				"siswa_mahasiswa", "penyusutan", "pengajuan_transfer", "transitori", "closing", "posting_hpp" };
		Tab tabTerpilihTerlihat = null;
		for (int i = 0; i < semuaTab.length; i++) {
			Tab t = semuaTab[i];
			if (t == null) {
				continue;
			}
			boolean tampil = Common.bolehKonfigurasi(P + slug[i]);
			t.setVisible(tampil);
			if (tampil && tabTerpilihTerlihat == null) {
				tabTerpilihTerlihat = t;
			}
		}
		if (tabDraftJurnal != null && !tabDraftJurnal.isVisible() && tabTerpilihTerlihat != null) {
			tabTerpilihTerlihat.setSelected(true);
		}
	}

	/**
	 * Memuat konten sub-halaman ZUL ke dalam {@code Tabpanel} secara lazy dan idempoten.
	 *
	 * <p><b>Tujuan:</b> Sentralisasi logika pemuatan sub-halaman tab agar setiap
	 * event handler tab tidak perlu mengulang kode yang sama. Lazy loading berarti
	 * sub-halaman hanya dimuat pertama kali tab dibuka, menghemat waktu inisialisasi
	 * dan sumber daya server. Idempoten berarti pemanggilan berulang (misal tab dibuka
	 * lagi) tidak memuat ulang konten yang sudah ada.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Guard: jika {@code panel} null atau {@code url} kosong, return langsung.</li>
	 *   <li>Guard idempoten: jika {@code panel} sudah memiliki anak komponen
	 *       ({@code getChildren().size() > 0}), konten sudah dimuat — return langsung.</li>
	 *   <li>Tampilkan progress loading via {@code PostingJurnalLoadingUtil.show}
	 *       (umpan balik visual kepada pengguna saat halaman disiapkan).</li>
	 *   <li>Buat {@code MyWindow} (container) dengan tinggi dan lebar 100% tanpa border,
	 *       dan pasang ke {@code panel}.</li>
	 *   <li>Buat {@code MyInclude} dengan URL ZUL yang diberikan, dan pasang ke window.
	 *       ZK framework akan memuat dan merender ZUL tersebut beserta controller-nya.</li>
	 *   <li>Di blok finally, panggil {@code PostingJurnalLoadingUtil.complete} untuk
	 *       menyelesaikan indikator loading.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari pemuatan ZUL (misalnya file tidak
	 * ditemukan) tidak ditangkap di sini — akan menyebar ke event handler pemanggil
	 * dan akhirnya ke ZK framework. Loading indicator selalu diselesaikan di finally.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika delay loading atau pesan loading perlu disesuaikan,
	 * ubah argumen ketiga {@code PostingJurnalLoadingUtil.show} (step/progress value).</p>
	 *
	 * @param panel wadah {@code Tabpanel} ZK tempat sub-halaman akan dimuat
	 * @param url   path absolut ZUL yang akan dimuat (dimulai dari {@code /pages/...})
	 */
	private void loadPanel(Tabpanel panel, String url) {
		if (panel == null || url == null || url.trim().length() == 0) {
			return;
		}
		if (panel.getChildren().size() > 0) {
			return;
		}
		ais.ui.util.PostingJurnalLoadingUtil.show("Membuka Halaman Posting",
				"Menyiapkan halaman dan filter jurnal yang dipilih.", 12);
		try {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setStyle("border:0; overflow:hidden;");
			window.setParent(panel);

			MyInclude iframe = new MyInclude(url);
			iframe.setHeight("100%");
			iframe.setWidth("100%");
			iframe.setParent(window);
			ais.ui.util.PostingJurnalLoadingUtil.update("Halaman Posting Dibuka",
					"Data halaman akan dimuat oleh menu yang dipilih.", 70);
		} finally {
			ais.ui.util.PostingJurnalLoadingUtil.complete("Halaman Posting Siap", "Halaman berhasil dibuka.", 100);
		}
	}

	/**
	 * Memuat tab Jurnal Umum dengan halaman draft jurnal saat pertama kali diklik.
	 *
	 * <p><b>Tujuan:</b> Menangani event klik pada tab "Jurnal Umum" dan memuat
	 * {@code grup_transaksi.zul} — halaman daftar draft jurnal umum yang belum
	 * terposting, dikelola oleh {@link GrupTransaksiAction}. Pemuatan lazy:
	 * hanya terjadi sekali, selanjutnya konten yang sudah ada digunakan kembali.</p>
	 *
	 * @param event event ZK yang dikirim saat tab diklik (tidak digunakan langsung)
	 */
	public void onJurnalUmum(Event event) {
		loadPanel(jurnalUmum, "/pages/master/akunting/grup_transaksi.zul");
	}

	/**
	 * Memuat tab Gaji dengan halaman posting transaksi penggajian.
	 *
	 * <p><b>Tujuan:</b> Menangani klik tab "Gaji" dan memuat halaman posting
	 * transaksi penggajian dari modul payroll ({@code posting_transaksi_transaksi_penggajian.zul}).
	 * Tab ini menghubungkan modul akuntansi dengan modul penggajian, memungkinkan
	 * staf akuntansi memposting jurnal gaji langsung dari halaman Posting Jurnal.</p>
	 *
	 * @param event event ZK yang dikirim saat tab diklik
	 */
	public void onGaji(Event event) {
		loadPanel(gaji, "/pages/master/payroll/posting_transaksi_transaksi_penggajian.zul");
	}

	/**
	 * Memuat tab Pajak dengan halaman posting pertanggungjawaban pajak.
	 *
	 * <p><b>Tujuan:</b> Menangani klik tab "Pajak" dan memuat
	 * {@code posting_pertanggunjawaban_pajak.zul} — halaman untuk memposting
	 * transaksi pajak ke jurnal akuntansi setelah pertanggungjawaban pajak
	 * diverifikasi. Memungkinkan staf akuntansi mengelola posting pajak
	 * dalam satu halaman terpadu.</p>
	 *
	 * @param event event ZK yang dikirim saat tab diklik
	 */
	public void onPajak(Event event) {
		loadPanel(pajak, "/pages/master/akunting/posting_pertanggunjawaban_pajak.zul");
	}

	/**
	 * Memuat tab Uang Muka dan Kas Kecil dengan halaman terpadu posting keduanya.
	 *
	 * <p><b>Tujuan:</b> Menangani klik tab "Uang Muka dan Kas Kecil" dan memuat
	 * {@code uang_muka_dan_kas_kecil.zul} — halaman posting gabungan untuk
	 * transaksi uang muka dan kas kecil. Penggabungan kedua jenis transaksi
	 * ini dalam satu tab memberikan efisiensi bagi staf akuntansi yang biasanya
	 * memproses keduanya secara bersamaan.</p>
	 *
	 * @param event event ZK yang dikirim saat tab diklik
	 */
	public void onUangMukadanKasKecil(Event event) {
		loadPanel(uangMukadanKasKecil, "/pages/master/akunting/uang_muka_dan_kas_kecil.zul");
	}

	/**
	 * Memuat tab Transaksi Vendor dengan halaman posting transaksi dari vendor.
	 *
	 * <p><b>Tujuan:</b> Menangani klik tab "Transaksi Vendor" dan memuat
	 * {@code transaksi_vendor.zul} — halaman untuk memposting transaksi pengadaan
	 * dan pembayaran vendor/pemasok ke jurnal akuntansi. Terhubung dengan
	 * modul asset untuk transaksi pengadaan barang/jasa.</p>
	 *
	 * @param event event ZK yang dikirim saat tab diklik
	 */
	public void onTransaksiVendor(Event event) {
		loadPanel(transaksiVendor, "/pages/master/akunting/transaksi_vendor.zul");
	}

	/**
	 * Memuat tab Siswa dan Mahasiswa dengan halaman posting transaksi pembayaran.
	 *
	 * <p><b>Tujuan:</b> Menangani klik tab "Siswa dan Mahasiswa" dan memuat
	 * {@code transaksi_siswa_dan_mahasiswa.zul} — halaman untuk memposting
	 * transaksi keuangan yang berkaitan dengan siswa (pembayaran SPP sekolah)
	 * dan mahasiswa (pembayaran UKT/SPP perguruan tinggi) ke jurnal akuntansi.</p>
	 *
	 * @param event event ZK yang dikirim saat tab diklik
	 */
	public void onSiswadanMahasiswa(Event event) {
		loadPanel(siswadanMahasiswa, "/pages/master/akunting/transaksi_siswa_dan_mahasiswa.zul");
	}

	/**
	 * Memuat tab Jurnal Penyusutan dengan halaman posting penyusutan aset.
	 *
	 * <p><b>Tujuan:</b> Menangani klik tab "Jurnal Penyusutan" dan memuat
	 * {@code posting_penyusutan_asset.zul} — halaman untuk memposting jurnal
	 * penyusutan aset tetap ke buku besar. Menghubungkan modul asset dengan
	 * modul akuntansi untuk pencatatan depresiasi aset secara periodik.</p>
	 *
	 * @param event event ZK yang dikirim saat tab diklik
	 */
	public void onJurnalPenyusutan(Event event) {
		loadPanel(jurnalPenyusutan, "/pages/master/asset/posting_penyusutan_tabs.zul");
	}

	/**
	 * Memuat tab Pengajuan Transfer dengan halaman posting proses transfer dana.
	 *
	 * <p><b>Tujuan:</b> Menangani klik tab "Pengajuan Transfer" dan memuat
	 * {@code posting_proses_transfer.zul} — halaman untuk memposting transaksi
	 * transfer dana antar rekening yang sudah mendapat persetujuan ke jurnal
	 * akuntansi. Tahap akhir dari alur persetujuan transfer sebelum dana
	 * benar-benar berpindah secara akuntansi.</p>
	 *
	 * @param event event ZK yang dikirim saat tab diklik
	 */
	public void onJurnalPengajuanTransfer(Event event) {
		loadPanel(jurnalPengajuanTransfer, "/pages/master/akunting/posting_proses_transfer.zul");
	}

	/**
	 * Memuat tab Transitori dengan halaman posting proses transitori akuntansi.
	 *
	 * <p><b>Tujuan:</b> Menangani klik tab "Transitori" dan memuat
	 * {@code posting_proses_transitori.zul} — halaman untuk memposting ayat
	 * jurnal transitori (jurnal akrual/deferral sementara) ke buku besar.
	 * Transitori digunakan untuk mencatat pendapatan atau beban yang belum
	 * jatuh tempo dalam periode akuntansi saat ini namun perlu diakui terlebih dahulu.</p>
	 *
	 * @param event event ZK yang dikirim saat tab diklik
	 */
	public void onTransitori(Event event) {
		loadPanel(transitori, "/pages/master/akunting/posting_proses_transitori.zul");
	}

	/**
	 * Memuat tab Closing dengan halaman closing (penutupan) periode akuntansi.
	 *
	 * <p><b>Tujuan:</b> Menangani klik tab "Closing" dan memuat
	 * {@code closing.zul} — halaman untuk melakukan penutupan periode akuntansi
	 * (jurnal penutup). Closing adalah proses akhir siklus akuntansi yang
	 * memindahkan saldo akun-akun nominal (pendapatan dan beban) ke akun
	 * laba/rugi dan selanjutnya ke ekuitas, serta mereset akun nominal
	 * untuk periode berikutnya.</p>
	 *
	 * @param event event ZK yang dikirim saat tab diklik
	 */
	public void onClosing(Event event) {
		loadPanel(closing, "/pages/master/akunting/closing.zul");
	}

	/**
	 * Memuat tab Posting HPP dengan halaman posting HPP (beban pokok penjualan) Kantin.
	 *
	 * <p><b>Tujuan:</b> Menangani klik tab "Posting HPP" dan memuat
	 * {@code posting_hpp_kantin.zul} — halaman pratinjau &amp; posting HPP penjualan produk
	 * Kantin yang tertaut barang persediaan (modul Aset) ke jurnal akuntansi, dikelola oleh
	 * {@link ais.action.master.koperasi.PostingHppKantinAction}. Halaman ini sebelumnya hanya
	 * bisa diakses lewat menu terpisah; tab ini menyatukannya ke alur Posting Jurnal utama.</p>
	 *
	 * @param event event ZK yang dikirim saat tab diklik
	 */
	public void onPostingHpp(Event event) {
		loadPanel(postingHpp, "/pages/master/koperasi/posting_hpp_kantin.zul");
	}
}
