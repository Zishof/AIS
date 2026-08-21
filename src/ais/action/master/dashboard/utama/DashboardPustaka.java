
package ais.action.master.dashboard.utama;
import ais.ui.util.DashboardGridExportHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.PustakaAction;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.library.Anggota;
import ais.database.model.library.AnggotaYangDiblokir;
import ais.database.model.library.DdcItem;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.KembaliPengadaanItem;
import ais.database.model.library.KembaliPengadaanItemDetail;
import ais.database.model.library.KunjunganAnggota;
import ais.database.model.library.PeminjamanPengadaanItem;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.database.model.library.PemesananPengadaanItem;
import ais.database.model.library.PemesananPengadaanItemDetail;
import ais.database.model.library.Penerbit;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.TipeItem;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h2>Dashboard &amp; Katalog Pustaka (Sistem Informasi Perpustakaan)</h2>
 *
 * <p><b>Apa gunanya halaman ini bagi pengguna?</b> Halaman ini adalah "ruang
 * kontrol" perpustakaan. Dalam satu layar, petugas maupun anggota dapat melihat
 * kondisi koleksi dan layanan secara ringkas — berapa banyak buku/eksemplar yang
 * dimiliki, mana yang sedang dipinjam, mana yang terlambat, koleksi apa yang sedang
 * ramai diakses, sampai tren peminjaman per bulan. Tujuannya membantu mengambil
 * keputusan harian dengan cepat tanpa harus membuka banyak menu atau membaca tabel
 * yang rumit. Halaman dibagi menjadi dua tab: <b>Dashboard</b> (ringkasan visual)
 * dan <b>Katalog Pustaka</b> (pencarian koleksi dalam bentuk kartu).</p>
 *
 * <h3>Tab "Dashboard" — panel-panel yang ditampilkan</h3>
 * <p>Setiap panel sengaja diberi judul jelas dan satu kalimat penjelasan dalam
 * bahasa awam (lihat {@link #createPanel(Component, String, String)}), sehingga
 * pengguna yang sama sekali tidak mengerti teknologi tetap paham maksudnya:</p>
 * <ul>
 *   <li><b>Hero &amp; Kartu Ringkasan</b> ({@code renderHero}, {@code renderMetricCards})
 *       — angka-angka penting: total koleksi, e-book, eksemplar, peminjaman,
 *       pengembalian, kunjungan, keterlambatan, dan denda.</li>
 *   <li><b>Alur Layanan Pustaka</b> ({@code renderFunnelLayanan}) — perjalanan koleksi
 *       dari tersedia → dipinjam → terlambat → dikembalikan, untuk melihat titik
 *       layanan yang paling perlu diperhatikan.</li>
 *   <li><b>Komposisi Koleksi per Jenis</b> &amp; <b>Sebaran Eksemplar per Perpustakaan</b>
 *       ({@code renderKomposisiKoleksi}, {@code renderSebaranPerpustakaan}) — keseimbangan
 *       jenis koleksi dan lokasi penyimpanan eksemplar.</li>
 *   <li><b>Koleksi Sering Dilihat/Diunduh</b>, <b>Koleksi Terbaru</b>, <b>Koleksi ber-E-Book</b>
 *       ({@code renderKoleksiPopuler}, {@code renderAktivitasTerkini}, {@code renderKoleksiEbook}).</li>
 *   <li><b>Tren Aktivitas</b> ({@code renderTrenAktivitas}) — grafik garis peminjaman,
 *       pengembalian, dan kunjungan per bulan.</li>
 *   <li><b>Kesiapan Digital</b>, <b>Operasional Harian</b>, <b>Radar Layanan</b>
 *       (diagram laba-laba / spider), <b>Riwayat Anggota</b>, <b>Keterlambatan</b>,
 *       dan <b>Insight</b> — analisis tambahan untuk evaluasi mutu layanan.</li>
 * </ul>
 *
 * <h3>Teknologi visual: HTML + CSS, BUKAN JFreeChart</h3>
 * <p>Seluruh grafik (batang, funnel, garis tren, dan radar/spider) digambar memakai
 * HTML &amp; CSS modern yang ditanam ke komponen {@link org.zkoss.zul.Html}. Pendekatan
 * ini sengaja dipilih agar tampilan ringan, tajam di layar HiDPI, mudah diwarnai sesuai
 * tema, dan responsif — tanpa ketergantungan pustaka gambar server seperti JFreeChart.
 * Bila menambah chart baru, ikuti pola yang sama (bangun String HTML lalu bungkus dengan
 * {@code new Html(...)}); jangan memperkenalkan JFreeChart kembali.</p>
 *
 * <h3>Sumber data &amp; ruang lingkup login</h3>
 * <p>Semua angka dikumpulkan ke dalam satu objek pembawa {@code LibraryDashboardData}
 * (lihat {@code renderDashboardAsync}/{@code renderDashboard}) sehingga pengumpulan data
 * (query Hibernate) terpisah rapi dari penggambaran UI. Untuk performa, sampel dibatasi
 * {@link #DASHBOARD_SAMPLE_LIMIT}. Bila yang membuka adalah <b>anggota</b>
 * ({@code initLoginScope}), data riwayat (peminjaman, pengembalian, kunjungan, denda)
 * otomatis dipersempit hanya milik yang bersangkutan, sementara pilihan perpustakaan
 * tetap terbuka karena peminjaman bisa lintas perpustakaan.</p>
 *
 * <h3>Responsif (mobile &amp; desktop)</h3>
 * <p>Lebar kolom portal menyesuaikan perangkat melalui flag {@code mobile} (100% di
 * ponsel, 50% berdampingan di desktop), dan grid kartu memakai {@code repeat(auto-fit,
 * minmax(...))} agar membungkus dengan rapi pada layar sempit.</p>
 *
 * <h3>Reuse &amp; pemeliharaan (penting untuk maintenance)</h3>
 * <p>Agar mudah dirawat, bagian yang berulang sudah diekstrak menjadi pembantu yang
 * dipakai bersama: {@link #createPanel(Component, String, String)} (kartu panel +
 * deskripsi), {@code buildBarList} (grafik batang horizontal), {@code buildItemList}
 * (daftar koleksi bersampul), {@code funnelBlock}, dan {@code buildTrendCard}.
 * <b>Cara menambah panel baru:</b> buat method {@code renderXxx(parent, data)}, panggil
 * {@code createPanel(parent, "Judul Jelas", "Satu kalimat penjelasan bahasa awam")},
 * lalu isi dengan salah satu pembantu di atas; terakhir daftarkan pemanggilannya di
 * {@code renderDashboard(...)}. <b>Gaya deskripsi panel:</b> tetap pendek, ramah, dan
 * mudah dipahami orang non-IT — hindari istilah teknis dan frasa kaku seperti
 * "Panel ini ...".</p>
 *
 * <p>Kompatibel Java 1.7 dan ZK 5.5. Penggambaran dilakukan secara asinkron (thread
 * latar + loading bar) agar UI tidak membeku saat data besar dihitung.</p>
 *
 * @author Mohammad Fauzi Murtadho
 */
public class DashboardPustaka extends MyWindow {

	private static final long serialVersionUID = -3049796225254178236L;
	private static final int DASHBOARD_SAMPLE_LIMIT = 500;
	private static final int DEFAULT_PAGE_SIZE = 20;

	private Textbox cariIsbn;
	private Textbox cariJudul;
	private Textbox cariPengarang;
	private Textbox cariTema;
	private Textbox cariPenerbit;
	private Textbox cariJenis;
	private Textbox cariKategori;
	private Textbox cariBahasa;
	private Textbox cariKlasifikasi;
	private Textbox cariTahun;
	private Textbox cariHalaman;
	private Textbox cariEdisi;
	private Textbox cariCatatan;
	private Textbox cariPenaklikan;

	private boolean mobile = false;
	private Tbmuser tbmuser;
	private int jumlahDataDalamSatuHalamanElearning = DEFAULT_PAGE_SIZE;
	private transient Html dashboardLoadingHtml;

	private Perpustakaan currentPerpustakaan;
	private Anggota currentAnggota;
	private Combobox filterPerpustakaan;
	private MyDatebox filterMulai;
	private MyDatebox filterSampai;

	public DashboardPustaka() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardPustaka(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {
		// Tombol ekspor dipindah ke dalam tab "Dashboard" (menyatu dengan dashboard) — lihat bawah.
		// (Sebelumnya dipasang di sini tapi langsung terhapus oleh Common.clear(this) di bawah.)
		mobile = Common.isMobile();
		tbmuser = Common.getCurrentUser();
		initLoginScope();
		setWidth("100%");
		setHeight("100%");
		setStyle("border:0; padding:0; margin:0; overflow:hidden; background:#f6f8fb;");
		Common.clear(this);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("border:0; background:#f6f8fb; overflow:hidden;");
		borderlayout.setParent(this);

		Center center = new Center();
		center.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setStyle("border:0; padding:0; overflow:hidden;");
		center.setParent(borderlayout);

		final Tabbox tabbox = new Tabbox();
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");
		tabbox.setStyle("border:0; overflow:hidden;");
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		MyTabConfig tabDashboard = new MyTabConfig("Dashboard", "/img/options.png");
		tabDashboard.setSelected(true);
		tabDashboard.setParent(tabs);

		final Tabpanel dashboardPanel = new ais.ui.util.MyTabpanel();
		dashboardPanel.setWidth("100%");
		dashboardPanel.setHeight("100%");
		dashboardPanel.setStyle("padding:0; overflow:auto; background:#f6f8fb;");
		dashboardPanel.setParent(tabpanels);
		DashboardGridExportHelper.pasangGrup(dashboardPanel, this, "Pustaka");

		MyTabConfig tabKatalog = new MyTabConfig("Katalog Pustaka", "/img/Blue-Books-icon.png");
		tabKatalog.setParent(tabs);

		final Tabpanel katalogPanel = new ais.ui.util.MyTabpanel();
		katalogPanel.setWidth("100%");
		katalogPanel.setHeight("100%");
		katalogPanel.setStyle("padding:0; overflow:auto; background:#ffffff;");
		katalogPanel.setParent(tabpanels);

		tabDashboard.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				renderDashboardAsync(dashboardPanel);
			}
		});

		tabKatalog.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (katalogPanel.getChildren().isEmpty()) {
					renderKatalogPustaka(katalogPanel);
				}
			}
		});

		renderDashboardAsync(dashboardPanel);
	}

	private void initLoginScope() {
		currentPerpustakaan = null;
		currentAnggota = null;
		try {
			currentPerpustakaan = Common.getCurrentPerpustakaan();
		} catch (Exception e) {
			currentPerpustakaan = null;
		}
		try {
			if (tbmuser == null) {
				tbmuser = Common.getCurrentUser();
			}
			if (tbmuser != null) {
				boolean bolehBuatAnggota = tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
						|| tbmuser.ambilDosen() != null || tbmuser.ambilGuru() != null;
				currentAnggota = Anggota.buatAtauAmbilAnggota(tbmuser, bolehBuatAnggota);
			}
		} catch (Exception e) {
			currentAnggota = null;
		}
		if (currentAnggota != null) {
			/*
			 * Anggota boleh meminjam lintas perpustakaan. Karena itu pembatas
			 * currentPerpustakaan diabaikan untuk mode anggota, tetapi riwayat transaksi
			 * tetap dibatasi hanya milik anggota tersebut.
			 */
			currentPerpustakaan = null;
		}
	}

	private void renderDashboardAsync(final Component parent) throws Exception {
		if (parent == null) {
			return;
		}
		ensureDefaultFilterValue();
		tampilkanLoadingDashboard(parent, "Menyiapkan ringkasan koleksi, eksemplar, peminjaman, pengembalian, kunjungan, ebook, dan denda pustaka sesuai filter...", 8);
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					updateLoadingDashboard("Menghitung koleksi dan eksemplar yang tersedia...", 25);
					final LibraryDashboardData data = loadDashboardData();
					updateLoadingDashboard("Menyiapkan grafik, daftar keterlambatan, dan rekomendasi tindakan pustaka...", 70);
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event event2) throws Exception {
							renderDashboard(parent, data);
						}
					});
				} catch (Exception e) {
					renderDashboardError(parent, e);
				}
			}
		});
	}

	private void tampilkanLoadingDashboard(Component parent, String message, int percent) {
		Common.clear(parent);
		dashboardLoadingHtml = new Html(buildLoadingHtml(message, percent));
		Vbox box = new Vbox();
		box.setWidth("100%");
		box.setStyle("padding:14px; box-sizing:border-box; background:#f6f8fb;");
		box.setParent(parent);
		box.appendChild(dashboardLoadingHtml);
	}

	private void updateLoadingDashboard(String message, int percent) {
		try {
			if (dashboardLoadingHtml != null) {
				dashboardLoadingHtml.setContent(buildLoadingHtml(message, percent));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:356");
		}
	}

	private String buildLoadingHtml(String message, int percent) {
		if (percent < 0) {
			percent = 0;
		}
		if (percent > 100) {
			percent = 100;
		}
		return "<div style='padding:22px; border-radius:18px; background:#ffffff; border:1px solid #e5e7eb; "
				+ "box-shadow:0 14px 32px rgba(15,23,42,.08); color:#0f172a;'>"
				+ "<div style='display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap;'>"
				+ "<div><div style='font-size:11px; letter-spacing:.12em; text-transform:uppercase; color:#2563eb; font-weight:900;'>Memproses Dashboard Pustaka</div>"
				+ "<div style='font-size:18px; font-weight:900; margin-top:6px;'><i class='fa fa-spinner fa-spin'></i> Mengambil Data Perpustakaan</div>"
				+ "<div style='font-size:12px; color:#64748b; margin-top:8px; line-height:1.55;'>"
				+ escapeHtml(message) + "</div></div>"
				+ "<div style='min-width:86px; text-align:right; font-size:30px; font-weight:900; color:#0f172a;'>"
				+ percent + "%</div></div>"
				+ "<div style='height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden; margin-top:18px;'>"
				+ "<div style='height:12px; width:" + percent
				+ "%; border-radius:999px; background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));'></div></div>"
				+ "<div style='font-size:11px;color:#64748b;margin-top:12px;'>Ringkasan ini membantu petugas melihat kondisi koleksi, aktivitas pinjam-kembali, dan data yang perlu ditindaklanjuti tanpa membuka banyak menu.</div>"
				+ "</div>";
	}

	private void renderDashboardError(Component parent, Exception e) {
		try {
			Common.clear(parent);
			Vbox box = new Vbox();
			box.setWidth("100%");
			box.setStyle("padding:14px; box-sizing:border-box; background:#f6f8fb;");
			box.setParent(parent);
			box.appendChild(new Html("<div style='padding:18px;border-radius:16px;background:#fff7ed;border:1px solid #fed7aa;color:#9a3412;'>"
					+ "<b>Dashboard Pustaka belum dapat dimuat.</b><br/>Silakan ulangi beberapa saat lagi. Jika pesan ini tetap muncul, hubungi administrator untuk memeriksa koneksi database perpustakaan."
					+ "</div>"));
			Common.tampilErrorJikaAdmin(e);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:394");
		}
	}

	private void renderDashboard(Component parent, LibraryDashboardData data) {
		Common.clear(parent);
		// Center->scroll (bukan Div "overflow:auto" manual, sering tidak memunculkan
		// scrollbar di ZK ini) — lihat Common.tampilanScrollTabbox.
		Center shell = Common.tampilanScrollTabbox(parent);
		// PENTING: Center (child Borderlayout) tak boleh di-setWidth() langsung
		// (readonly di ZK 5.5 — lebar Center mengikuti parent Borderlayout) dan
		// hanya boleh punya SATU child langsung, jadi lebar/style yang dimaksud
		// ditaruh pada Div pembungkus ini, bukan pada shell/Center-nya.
		Div wrapper = new Div();
		wrapper.setWidth("100%");
		wrapper.setStyle("background:#f6f8fb; padding:14px; box-sizing:border-box;");
		wrapper.setParent(shell);

		renderHero(wrapper, data);
		renderDashboardFilter(wrapper, parent, data);
		renderMetricCards(wrapper, data);

		MyPortallayout portalLayout = new MyPortallayout();
		portalLayout.setWidth("100%");
		portalLayout.setStyle("margin-top:12px;");
		portalLayout.setParent(wrapper);

		String pcWidth = mobile ? "100%" : "50%";

		MyPortalchildren pcTop = new MyPortalchildren();
		pcTop.setWidth("100%");
		pcTop.setStyle("padding:6px;");
		pcTop.setParent(portalLayout);

		MyPortalchildren pcLeft = new MyPortalchildren();
		pcLeft.setWidth(pcWidth);
		pcLeft.setStyle("padding:6px;");
		pcLeft.setParent(portalLayout);

		MyPortalchildren pcRight = new MyPortalchildren();
		pcRight.setWidth(pcWidth);
		pcRight.setStyle("padding:6px;");
		pcRight.setParent(portalLayout);

		MyPortalchildren pcBottom = new MyPortalchildren();
		pcBottom.setWidth("100%");
		pcBottom.setStyle("padding:6px;");
		pcBottom.setParent(portalLayout);

		renderFunnelLayanan(pcTop, data);
		renderKomposisiKoleksi(pcLeft, data);
		renderSebaranPerpustakaan(pcRight, data);
		renderKoleksiPopuler(pcLeft, data);
		renderAktivitasTerkini(pcRight, data);
		renderKoleksiEbook(pcLeft, data);
		renderTrenAktivitas(pcRight, data);
		renderDashboardKesiapanDigital(pcLeft, data);
		renderDashboardOperasionalHarian(pcRight, data);
		renderDashboardRadarLayanan(pcBottom, data);
		renderRiwayatAnggota(pcBottom, data);
		renderKeterlambatan(pcBottom, data);
		renderInsight(pcBottom, data);
	}

	private void ensureDefaultFilterValue() {
		if (filterMulai == null || filterMulai.getValue() == null) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, -12);
			if (filterMulai == null) {
				filterMulai = new MyDatebox();
			}
			filterMulai.setValue(cal.getTime());
		}
		if (filterSampai == null || filterSampai.getValue() == null) {
			if (filterSampai == null) {
				filterSampai = new MyDatebox();
			}
			filterSampai.setValue(new Date());
		}
	}

	private Perpustakaan getSelectedFilterPerpustakaan() {
		if (currentAnggota == null && currentPerpustakaan != null) {
			return currentPerpustakaan;
		}
		try {
			if (filterPerpustakaan != null && filterPerpustakaan.getSelectedItem() != null) {
				Object value = filterPerpustakaan.getSelectedItem().getValue();
				if (value instanceof Perpustakaan) {
					return (Perpustakaan) value;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:479");
		}
		return null;
	}

	private Date getFilterMulaiValue() {
		try {
			return filterMulai == null ? null : filterMulai.getValue();
		} catch (Exception e) {
			return null;
		}
	}

	private Date getFilterSampaiValue() {
		try {
			return filterSampai == null ? null : filterSampai.getValue();
		} catch (Exception e) {
			return null;
		}
	}

	private void renderDashboardFilter(Component parent, final Component dashboardParent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Filter Dashboard Pustaka",
				"Gunakan filter ini untuk membatasi data berdasarkan perpustakaan dan rentang tanggal. Filter tanggal mempengaruhi peminjaman, pengembalian, kunjungan, pemesanan, keterlambatan, dan denda; sedangkan data katalog tetap mengikuti perpustakaan yang dipilih.");

		Grid grid = new Grid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(pch);
		Rows rows = new Rows();
		rows.setParent(grid);
		Row row = new Row();
		row.setValign("middle");
		row.setParent(rows);

		row.appendChild(new MyLabelConfig("Perpustakaan"));
		filterPerpustakaan = new Combobox();
		filterPerpustakaan.setReadonly(true);
		filterPerpustakaan.setWidth(mobile ? "95%" : "260px");
		row.appendChild(filterPerpustakaan);
		populateFilterPerpustakaan(data.filterPerpustakaan);

		row.appendChild(new MyLabelConfig("Tanggal Mulai"));
		filterMulai = new MyDatebox(data.filterMulai);
		filterMulai.setReadonly(true);
		filterMulai.setWidth(mobile ? "95%" : "120px");
		row.appendChild(filterMulai);

		row.appendChild(new MyLabelConfig("Tanggal Sampai"));
		filterSampai = new MyDatebox(data.filterSampai);
		filterSampai.setReadonly(true);
		filterSampai.setWidth(mobile ? "95%" : "120px");
		row.appendChild(filterSampai);

		Toolbarbutton refresh = new MyToolbarbuttonConfig("Terapkan Filter", "/img/svg/refresh.svg");
		row.appendChild(refresh);
		EventListener reload = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				renderDashboardAsync(dashboardParent);
			}
		};
		refresh.addEventListener("onClick", reload);
		filterPerpustakaan.addEventListener("onChange", reload);
		filterMulai.addEventListener("onChange", reload);
		filterSampai.addEventListener("onChange", reload);

		String modeInfo;
		if (data.filterAnggota != null) {
			modeInfo = "Anda login sebagai anggota. Data riwayat peminjaman, pengembalian, kunjungan, pemesanan, dan denda hanya menampilkan milik Anda sendiri, tetapi pilihan perpustakaan tetap dapat digunakan karena peminjaman dapat lintas perpustakaan.";
		} else if (currentPerpustakaan != null) {
			modeInfo = "Anda login sebagai petugas perpustakaan. Dashboard dibatasi hanya pada perpustakaan Anda, sehingga pilihan perpustakaan dikunci otomatis.";
		} else {
			modeInfo = "Mode umum/admin. Anda dapat melihat semua perpustakaan atau memilih satu perpustakaan tertentu.";
		}
		pch.appendChild(new Html("<div style='margin-top:10px;padding:10px;border-radius:12px;background:#eff6ff;border:1px solid #bfdbfe;color:#1e40af;font-size:12px;line-height:1.55;'>"
				+ escapeHtml(modeInfo) + "</div>"));
	}

	private void populateFilterPerpustakaan(Perpustakaan selected) {
		Common.clear(filterPerpustakaan);
		if (currentAnggota == null && currentPerpustakaan != null) {
			Comboitem item = new Comboitem(safeString(currentPerpustakaan.getNama()));
			item.setValue(currentPerpustakaan);
			item.setParent(filterPerpustakaan);
			filterPerpustakaan.setSelectedItem(item);
			filterPerpustakaan.setDisabled(true);
			return;
		}
		Comboitem semua = new Comboitem("Semua Perpustakaan");
		semua.setValue(null);
		semua.setParent(filterPerpustakaan);
		filterPerpustakaan.setSelectedItem(semua);
		List<Perpustakaan> list = getDaftarPerpustakaanAktif();
		for (Perpustakaan p : list) {
			Comboitem item = new Comboitem(safeString(p.getNama()));
			item.setValue(p);
			item.setParent(filterPerpustakaan);
			if (selected != null && p != null && selected.getId() != null && selected.getId().equals(p.getId())) {
				filterPerpustakaan.setSelectedItem(item);
			}
		}
		filterPerpustakaan.setDisabled(false);
	}

	private void renderHero(Component parent, LibraryDashboardData data) {
		PerguruanTinggi pt = null;
		try {
			pt = PerguruanTinggiUtil.getPerguruanTinggi();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:588");
		}
		String nama = pt == null || pt.getNama() == null ? "Perpustakaan" : pt.getNama();
		String scope = data.filterPerpustakaan == null ? "Semua perpustakaan" : safeString(data.filterPerpustakaan.getNama());
		String periode = formatDate(data.filterMulai) + " s.d. " + formatDate(data.filterSampai);
		String html = "<div style='border-radius:22px; padding:22px; background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%); color:white; box-shadow:0 18px 34px rgba(15,23,42,.18);'>"
				+ "<div style='display:flex;justify-content:space-between;gap:14px;flex-wrap:wrap;align-items:center;'>"
				+ "<div><div style='font-size:11px;letter-spacing:.14em;text-transform:uppercase;font-weight:900;color:#bfdbfe;'>Dashboard Pustaka Digital</div>"
				+ "<div style='font-size:28px;font-weight:900;margin-top:8px;'>" + escapeHtml(nama) + "</div>"
				+ "<div style='font-size:13px;line-height:1.65;max-width:780px;margin-top:10px;color:#e0f2fe;'>Ringkasan ini menampilkan ringkasan koleksi, ebook, ketersediaan eksemplar, aktivitas peminjaman, pengembalian, kunjungan, keterlambatan, dan denda. Informasi ini membantu petugas dan anggota mengambil tindakan harian dengan cepat.</div>"
				+ "<div style='font-size:12px;color:#bfdbfe;margin-top:8px;'>Filter aktif: " + escapeHtml(scope) + " • Periode: " + escapeHtml(periode) + "</div></div>"
				+ "<div style='text-align:right;min-width:180px;'><div style='font-size:12px;color:#dbeafe;'>Total koleksi aktif</div>"
				+ "<div style='font-size:38px;font-weight:900;'>" + formatNumber(data.totalKoleksi) + "</div>"
				+ "<div style='font-size:12px;color:#dbeafe;'>" + formatNumber(data.totalEksemplar) + " eksemplar/barcode terdata</div></div>"
				+ "</div></div>";
		parent.appendChild(new Html(html));
	}

	private void renderMetricCards(Component parent, LibraryDashboardData data) {
		String html = "<div style='display:flex; gap:10px; flex-wrap:wrap; margin-top:12px;'>"
				+ metricCard("Koleksi Aktif", formatNumber(data.totalKoleksi), "Jumlah judul/koleksi yang dapat dicari dan ditampilkan kepada pengguna.", "#eff6ff", "#1d4ed8")
				+ metricCard("Eksemplar", formatNumber(data.totalEksemplar), "Jumlah barcode atau unit fisik yang tersedia di perpustakaan.", "#ecfdf5", "#047857")
				+ metricCard("Sedang Dipinjam", formatNumber(data.totalDipinjam), "Koleksi yang masih berada di tangan peminjam dan belum dikembalikan.", "#fef3c7", "#92400e")
				+ metricCard("Terlambat", formatNumber(data.totalTerlambat), "Pinjaman yang melewati batas waktu dan perlu ditindaklanjuti.", "#fee2e2", "#b91c1c")
				+ metricCard("Dikembalikan", formatNumber(data.totalDikembalikan), "Jumlah transaksi pengembalian dalam periode yang dipilih.", "#ecfdf5", "#166534")
				+ metricCard("Kunjungan", formatNumber(data.totalKunjungan), "Jumlah kunjungan anggota dalam periode yang dipilih.", "#e0f2fe", "#075985")
				+ metricCard("E-Book", formatNumber(data.totalEbook), "Koleksi yang memiliki file digital, scan, lampiran, atau tautan yang bisa dibaca/diunduh.", "#f0fdf4", "#15803d")
				+ metricCard("Denda", "Rp " + formatMoney(data.totalDenda), "Perkiraan denda yang tercatat pada transaksi pengembalian.", "#f5f3ff", "#6d28d9")
				+ metricCard("Anggota Diblokir", formatNumber(data.totalDiblokir), "Anggota yang memiliki pembatasan akses layanan pustaka.", "#f8fafc", "#334155")
				+ "</div>";
		parent.appendChild(new Html(html));
	}

	private String metricCard(String title, String value, String desc, String bg, String color) {
		return "<div style='background:white;border:1px solid #e2e8f0;border-radius:18px;padding:14px;box-shadow:0 8px 18px rgba(15,23,42,.06);flex:1;min-width:170px;'>"
				+ "<div style='display:inline-block;padding:5px 9px;border-radius:999px;background:" + bg
				+ ";color:" + color + ";font-size:10px;font-weight:900;text-transform:uppercase;'>" + escapeHtml(title)
				+ "</div><div style='font-size:25px;font-weight:900;color:#0f172a;margin-top:10px;'>" + escapeHtml(value)
				+ "</div><div style='font-size:11px;color:#64748b;line-height:1.5;margin-top:6px;'>" + escapeHtml(desc)
				+ "</div></div>";
	}

	/**
	 * Membuat satu "kartu panel" dashboard yang seragam: bingkai membulat dengan bayangan
	 * lembut, judul di atas, dan (opsional) satu kalimat penjelasan berbahasa awam di bawah
	 * judul. Method ini adalah <b>titik reuse utama</b> dashboard — semua panel memakainya
	 * agar tampil konsisten dan mudah dirawat; cukup ubah di sini untuk mengganti gaya
	 * seluruh panel sekaligus.
	 *
	 * <p>Isi panel ditambahkan oleh pemanggil ke {@link Panelchildren} yang dikembalikan
	 * (mis. {@code pch.appendChild(new Html(grafikHtml))}).</p>
	 *
	 * @param parent      komponen induk tempat panel dipasang (umumnya sebuah portalchildren)
	 * @param title       judul panel; tulis jelas dan langsung dimengerti pengguna
	 * @param description deskripsi singkat fungsi panel dalam bahasa awam (boleh {@code null}
	 *                    atau kosong untuk tanpa deskripsi); hindari istilah teknis dan frasa
	 *                    kaku seperti "Panel ini ...". Teks otomatis di-escape HTML.
	 * @return {@link Panelchildren} kosong, siap diisi konten panel oleh pemanggil
	 */
	private Panelchildren createPanel(Component parent, String title, String description) {
		Panel panel = new Panel();
		panel.setTitle(title);
		panel.setBorder("normal");
		panel.setCollapsible(false);
		panel.setClosable(false);
		panel.setMaximizable(false);
		panel.setStyle("margin-bottom:12px;border:1px solid #e5e7eb;border-radius:18px;overflow:hidden;background:#ffffff;box-shadow:0 14px 28px rgba(15,23,42,.08);");
		panel.setParent(parent);
		Panelchildren pch = new Panelchildren();
		pch.setStyle("padding:14px;background:#ffffff;");
		pch.setParent(panel);
		if (description != null && description.trim().length() > 0) {
			pch.appendChild(new Html("<div style='font-size:12px;color:#64748b;line-height:1.6;margin-bottom:12px;'>"
					+ escapeHtml(description) + "</div>"));
		}
		return pch;
	}

	private void renderFunnelLayanan(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Alur Layanan Pustaka",
				"Memperlihatkan perjalanan koleksi mulai dari eksemplar yang tersedia, sedang dipinjam, terlambat, sampai denda. Gunakan bagian ini untuk melihat titik layanan yang paling perlu diperhatikan hari ini.");
		int max = Math.max(data.totalEksemplar, Math.max(data.totalDipinjam, Math.max(data.totalTerlambat, 1)));
		String html = "<div style='display:flex;gap:10px;flex-wrap:wrap;'>"
				+ funnelBlock("Eksemplar Terdata", data.totalEksemplar, max, "#2563eb")
				+ funnelBlock("Sedang Dipinjam", data.totalDipinjam, max, "#f59e0b")
				+ funnelBlock("Terlambat", data.totalTerlambat, max, "#ef4444")
				+ funnelBlock("Dikembalikan", data.totalDikembalikan, max, "#22c55e")
				+ funnelBlock("Kunjungan", data.totalKunjungan, max, "#06b6d4")
				+ funnelBlock("E-Book", data.totalEbook, max, "#16a34a")
				+ funnelBlock("Pemesanan", data.totalPemesanan, max, "#8b5cf6")
				+ "</div>";
		pch.appendChild(new Html(html));
	}

	private String funnelBlock(String title, int value, int max, String color) {
		double pct = max <= 0 ? 0.0 : (value * 100.0 / max);
		return "<div style='flex:1;min-width:160px;border:1px solid #e2e8f0;border-radius:16px;padding:12px;background:#f8fafc;'>"
				+ "<div style='font-size:12px;font-weight:900;color:#0f172a;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:24px;font-weight:900;color:#0f172a;margin:6px 0;'>" + formatNumber(value) + "</div>"
				+ "<div style='height:10px;background:#e2e8f0;border-radius:999px;overflow:hidden;'><div style='height:10px;width:"
				+ formatPercentNumber(pct) + "%;background:" + color + ";border-radius:999px;'></div></div></div>";
	}

	private void renderKomposisiKoleksi(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Komposisi Koleksi per Jenis",
				"Menunjukkan jenis koleksi yang paling banyak dimiliki, misalnya buku, jurnal, atau referensi lain. Informasi ini membantu perpustakaan mengetahui keseimbangan koleksi.");
		pch.appendChild(new Html(buildBarList(data.koleksiPerTipe, "Belum ada data jenis koleksi.")));
	}

	private void renderSebaranPerpustakaan(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Sebaran Eksemplar per Perpustakaan",
				"Menampilkan lokasi/perpustakaan yang menyimpan eksemplar. Petugas dapat melihat unit mana yang paling banyak menyimpan koleksi fisik.");
		pch.appendChild(new Html(buildBarList(data.eksemplarPerPustaka, "Belum ada data eksemplar per perpustakaan.")));
	}

	private String buildBarList(List<NameValue> values, String emptyText) {
		if (values == null || values.isEmpty()) {
			return "<div style='font-size:12px;color:#64748b;padding:10px;border-radius:12px;background:#f8fafc;'>" + escapeHtml(emptyText) + "</div>";
		}
		int max = 1;
		for (NameValue nv : values) {
			if (nv.value > max) {
				max = nv.value;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:Arial,sans-serif;'>");
		for (NameValue nv : values) {
			double pct = nv.value * 100.0 / max;
			sb.append("<div style='margin:9px 0;'>");
			sb.append("<div style='display:flex;justify-content:space-between;gap:10px;font-size:12px;color:#334155;font-weight:800;'>");
			sb.append("<span>").append(escapeHtml(nv.name)).append("</span><span>").append(formatNumber(nv.value)).append("</span></div>");
			sb.append("<div style='height:11px;background:#e2e8f0;border-radius:999px;overflow:hidden;margin-top:4px;'>");
			sb.append("<div style='height:11px;width:").append(formatPercentNumber(pct)).append("%;background:linear-gradient(90deg, var(--ais-theme-primary,#2563eb), var(--ais-theme-accent,#06b6d4));border-radius:999px;'></div></div>");
			sb.append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private void renderKoleksiPopuler(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Koleksi yang Sering Dilihat/Diunduh",
				"Menampilkan koleksi yang paling sering diakses. Koleksi populer dapat dijadikan prioritas digitalisasi, promosi, atau penambahan eksemplar.");
		pch.appendChild(new Html(buildItemList(data.koleksiPopuler, "Belum ada data koleksi populer.")));
	}

	private void renderAktivitasTerkini(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Koleksi Terbaru",
				"Menampilkan koleksi terbaru yang masuk ke sistem. Pengguna dapat mengetahui materi baru yang sudah tersedia untuk dibaca atau dipinjam.");
		pch.appendChild(new Html(buildItemList(data.koleksiTerbaru, "Belum ada koleksi terbaru yang dapat ditampilkan.")));
	}

	private String buildItemList(List<ItemInfo> values, String emptyText) {
		if (values == null || values.isEmpty()) {
			return "<div style='font-size:12px;color:#64748b;padding:10px;border-radius:12px;background:#f8fafc;'>" + escapeHtml(emptyText) + "</div>";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex;flex-direction:column;gap:8px;'>");
		for (ItemInfo info : values) {
			sb.append("<div style='display:flex;gap:10px;border:1px solid #e2e8f0;border-radius:14px;padding:10px;background:#f8fafc;'>");
			sb.append("<img src='").append(escapeHtml(info.coverUrl)).append("' style='width:54px;height:76px;object-fit:cover;border-radius:8px;background:#e2e8f0;'/> ");
			sb.append("<div style='flex:1;min-width:0;'>");
			sb.append("<div style='font-size:12px;font-weight:900;color:#0f172a;line-height:1.35;'>").append(escapeHtml(info.nama)).append("</div>");
			sb.append("<div style='font-size:11px;color:#64748b;margin-top:4px;'>").append(escapeHtml(info.subInfo)).append("</div>");
			sb.append("<div style='font-size:10px;color:#64748b;margin-top:6px;'>Dilihat: ").append(formatNumber(info.dilihat)).append(" &nbsp; | &nbsp; Diunduh: ").append(formatNumber(info.diunduh)).append("</div>");
			if (info.ebook) {
				sb.append(buildEbookButtonsHtml(info));
			}
			sb.append("</div></div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private String buildEbookButtonsHtml(ItemInfo info) {
		if (info == null || !info.ebook) {
			return "";
		}
		String readUrl = info.readUrl == null ? "" : info.readUrl.trim();
		String downloadUrl = info.downloadUrl == null ? "" : info.downloadUrl.trim();
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:flex;flex-wrap:wrap;gap:6px;margin-top:8px;'>");
		if (readUrl.length() > 0) {
			sb.append("<a href='").append(escapeHtml(readUrl)).append("' target='_blank' ")
					.append("style='display:inline-block;padding:6px 10px;border-radius:999px;background:#2563eb;color:#fff;text-decoration:none;font-size:10px;font-weight:900;'>")
					.append("<i class=\"fa fa-book\"></i> Baca</a>");
		}
		if (downloadUrl.length() > 0) {
			sb.append("<a href='").append(escapeHtml(downloadUrl)).append("' target='_blank' ")
					.append("style='display:inline-block;padding:6px 10px;border-radius:999px;background:#16a34a;color:#fff;text-decoration:none;font-size:10px;font-weight:900;'>")
					.append("<i class=\"fa fa-download\"></i> Download</a>");
		}
		if (readUrl.length() == 0 && downloadUrl.length() == 0) {
			sb.append("<span style='display:inline-block;padding:6px 10px;border-radius:999px;background:#ecfdf5;color:#166534;font-size:10px;font-weight:900;'>")
					.append("E-Book tersedia</span>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private void renderKoleksiEbook(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Koleksi yang Memiliki E-Book",
				"Menampilkan buku/koleksi yang memiliki file digital, hasil scan, lampiran, atau tautan bacaan. Anggota dapat memakai bagian ini untuk lebih cepat menemukan bahan bacaan yang bisa diakses secara online.");
		pch.appendChild(new Html(buildItemList(data.koleksiEbook, "Belum ada koleksi e-book pada filter ini.")));
	}

	private void renderTrenAktivitas(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Tren Aktivitas Pustaka",
				"Memperlihatkan perkembangan peminjaman, pengembalian, dan kunjungan berdasarkan bulan. Grafik ini membantu petugas melihat kapan layanan sedang ramai dan kapan perlu promosi atau penataan layanan.");
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:12px;'>");
		sb.append(buildTrendCard("Peminjaman", data.trenPeminjaman, "#f59e0b"));
		sb.append(buildTrendCard("Pengembalian", data.trenPengembalian, "#22c55e"));
		sb.append(buildTrendCard("Kunjungan", data.trenKunjungan, "#06b6d4"));
		sb.append("</div>");
		pch.appendChild(new Html(sb.toString()));
	}

	private String buildTrendCard(String title, List<NameValue> values, String color) {
		if (values == null || values.isEmpty()) {
			return "<div style='padding:12px;border-radius:14px;background:#f8fafc;border:1px solid #e2e8f0;color:#64748b;font-size:12px;'>" + escapeHtml(title) + ": belum ada data.</div>";
		}
		int max = 1;
		for (NameValue nv : values) {
			if (nv != null && nv.value > max) {
				max = nv.value;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='padding:12px;border-radius:14px;background:#f8fafc;border:1px solid #e2e8f0;'>");
		sb.append("<div style='font-size:13px;font-weight:900;color:#0f172a;margin-bottom:10px;'>").append(escapeHtml(title)).append("</div>");
		sb.append("<div style='display:flex;align-items:flex-end;gap:7px;min-height:120px;overflow:auto;'>");
		for (NameValue nv : values) {
			int h = 18 + (int) Math.round(nv.value * 92.0 / max);
			sb.append("<div style='min-width:42px;text-align:center;'>")
					.append("<div title='").append(escapeHtml(nv.name)).append(": ").append(nv.value).append("' style='height:")
					.append(h).append("px;border-radius:10px 10px 4px 4px;background:").append(color)
					.append(";box-shadow:0 8px 14px rgba(15,23,42,.10);'></div>")
					.append("<div style='font-size:10px;color:#0f172a;font-weight:900;margin-top:4px;'>")
					.append(formatNumber(nv.value)).append("</div>")
					.append("<div style='font-size:9px;color:#64748b;white-space:nowrap;'>").append(escapeHtml(nv.name)).append("</div></div>");
		}
		sb.append("</div></div>");
		return sb.toString();
	}


	private void renderDashboardKesiapanDigital(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Kesiapan Digitalisasi Koleksi",
				"Menunjukkan seberapa banyak koleksi yang sudah memiliki akses digital seperti e-book, scan, lampiran, atau tautan bacaan. Data ini membantu petugas menentukan prioritas koleksi yang perlu didigitalisasi berikutnya.");
		int total = data.totalKoleksi <= 0 ? 1 : data.totalKoleksi;
		int ebookPct = percent(data.totalEbook, total);
		int fisikPct = 100 - ebookPct;
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;'>");
		sb.append("<div style='padding:14px;border-radius:16px;background:#f8fafc;border:1px solid #e2e8f0;'>")
				.append("<div style='font-size:13px;font-weight:900;color:#0f172a;'>Rasio E-Book</div>")
				.append("<div style='margin-top:12px;height:18px;background:#e2e8f0;border-radius:999px;overflow:hidden;'>")
				.append("<div style='width:").append(ebookPct).append("%;height:18px;background:linear-gradient(90deg,#16a34a,#22c55e);border-radius:999px;'></div></div>")
				.append("<div style='font-size:24px;font-weight:900;color:#166534;margin-top:10px;'>").append(ebookPct).append("%</div>")
				.append("<div style='font-size:11px;color:#64748b;line-height:1.55;'>")
				.append(formatNumber(data.totalEbook)).append(" dari ").append(formatNumber(data.totalKoleksi))
				.append(" koleksi sudah memiliki akses digital.</div></div>");

		sb.append("<div style='padding:14px;border-radius:16px;background:#fff7ed;border:1px solid #fed7aa;'>")
				.append("<div style='font-size:13px;font-weight:900;color:#9a3412;'>Masih Perlu Digitalisasi</div>")
				.append("<div style='margin-top:12px;height:18px;background:#ffedd5;border-radius:999px;overflow:hidden;'>")
				.append("<div style='width:").append(fisikPct).append("%;height:18px;background:linear-gradient(90deg,#f97316,#f59e0b);border-radius:999px;'></div></div>")
				.append("<div style='font-size:24px;font-weight:900;color:#9a3412;margin-top:10px;'>").append(fisikPct).append("%</div>")
				.append("<div style='font-size:11px;color:#9a3412;line-height:1.55;'>")
				.append("Koleksi tanpa file digital dapat diprioritaskan untuk scan, unggah lampiran, atau penambahan tautan bacaan.</div></div>");
		sb.append("</div>");
		pch.appendChild(new Html(sb.toString()));
	}

	private void renderDashboardOperasionalHarian(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Kompas Operasional Harian",
				"Merangkum kondisi layanan harian perpustakaan dalam bentuk kartu status. Petugas dapat langsung melihat pinjaman aktif, pengembalian, kunjungan, pemesanan, denda, dan anggota yang diblokir.");
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:10px;'>");
		sb.append(opsCard("Pinjaman Aktif", data.totalDipinjam, "Eksemplar yang belum kembali.", "#fef3c7", "#92400e"));
		sb.append(opsCard("Dikembalikan", data.totalDikembalikan, "Pengembalian pada periode filter.", "#dcfce7", "#166534"));
		sb.append(opsCard("Kunjungan", data.totalKunjungan, "Jumlah kunjungan anggota.", "#e0f2fe", "#075985"));
		sb.append(opsCard("Pemesanan", data.totalPemesanan, "Pesanan koleksi yang perlu dipantau.", "#f5f3ff", "#6d28d9"));
		sb.append(opsCard("Keterlambatan", data.totalTerlambat, "Pinjaman yang lewat batas waktu.", "#fee2e2", "#991b1b"));
		sb.append(opsCard("Anggota Diblokir", data.totalDiblokir, "Anggota yang sedang dibatasi aksesnya.", "#f1f5f9", "#334155"));
		sb.append("</div>");
		pch.appendChild(new Html(sb.toString()));
	}

	private String opsCard(String title, int value, String desc, String bg, String color) {
		return "<div style='padding:12px;border-radius:16px;background:" + bg + ";border:1px solid rgba(15,23,42,.08);'>"
				+ "<div style='font-size:11px;font-weight:900;color:" + color + ";text-transform:uppercase;letter-spacing:.05em;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:26px;font-weight:900;color:" + color + ";margin-top:8px;'>" + formatNumber(value) + "</div>"
				+ "<div style='font-size:11px;color:" + color + ";line-height:1.45;margin-top:4px;opacity:.85;'>" + escapeHtml(desc) + "</div></div>";
	}

	private void renderDashboardRadarLayanan(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Radar Kesehatan Layanan Pustaka",
				"Panel radar ini merangkum beberapa indikator penting dalam satu tampilan sederhana. Semakin tinggi nilai layanan dan digitalisasi, serta semakin rendah keterlambatan, semakin sehat kondisi layanan pustaka.");
		int totalAktivitas = data.totalPeminjamanPeriode + data.totalDikembalikan + data.totalKunjungan + data.totalPemesanan;
		int layanan = percent(data.totalDikembalikan + data.totalKunjungan, totalAktivitas <= 0 ? 1 : totalAktivitas);
		int digital = percent(data.totalEbook, data.totalKoleksi <= 0 ? 1 : data.totalKoleksi);
		int disiplin = 100 - percent(data.totalTerlambat, data.totalDipinjam <= 0 ? 1 : data.totalDipinjam);
		int koleksi = percent(data.totalEksemplar, data.totalKoleksi <= 0 ? 1 : data.totalKoleksi);
		int akses = percent(data.totalKunjungan, totalAktivitas <= 0 ? 1 : totalAktivitas);
		String html = "<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px;align-items:center;'>"
				+ "<div style='padding:18px;border-radius:18px;background:#f8fafc;border:1px solid #e2e8f0;text-align:center;'>"
				+ "<div style='margin:auto;width:190px;height:190px;border-radius:999px;position:relative;"
				+ "background:conic-gradient(#2563eb 0 " + layanan + "%,#e2e8f0 " + layanan + "% 100%);"
				+ "box-shadow:inset 0 0 0 18px #fff,0 12px 24px rgba(15,23,42,.08);'>"
				+ "<div style='position:absolute;inset:38px;border-radius:999px;background:conic-gradient(#16a34a 0 " + digital + "%,#e2e8f0 " + digital + "% 100%);'></div>"
				+ "<div style='position:absolute;inset:70px;border-radius:999px;background:#ffffff;display:flex;align-items:center;justify-content:center;font-size:22px;font-weight:900;color:#0f172a;'>"
				+ layanan + "%</div></div>"
				+ "<div style='font-size:11px;color:#64748b;margin-top:10px;'>Lingkar luar: layanan, lingkar dalam: digitalisasi.</div></div>"
				+ "<div style='display:flex;flex-direction:column;gap:8px;'>"
				+ radarRow("Layanan", layanan, "#2563eb")
				+ radarRow("Digitalisasi", digital, "#16a34a")
				+ radarRow("Disiplin Kembali", disiplin, "#f59e0b")
				+ radarRow("Ketersediaan Eksemplar", koleksi, "#7c3aed")
				+ radarRow("Akses/Kunjungan", akses, "#06b6d4")
				+ "</div></div>";
		pch.appendChild(new Html(html));
	}

	private String radarRow(String label, int value, String color) {
		if (value < 0) {
			value = 0;
		}
		if (value > 100) {
			value = 100;
		}
		return "<div><div style='display:flex;justify-content:space-between;font-size:11px;font-weight:900;color:#0f172a;'><span>"
				+ escapeHtml(label) + "</span><span>" + value + "%</span></div>"
				+ "<div style='height:10px;border-radius:999px;background:#e2e8f0;overflow:hidden;margin-top:4px;'>"
				+ "<div style='height:10px;width:" + value + "%;background:" + color + ";border-radius:999px;'></div></div></div>";
	}

	private int percent(int value, int total) {
		if (total <= 0) {
			return 0;
		}
		int p = (int) Math.round((value * 100.0d) / total);
		if (p < 0) {
			return 0;
		}
		if (p > 100) {
			return 100;
		}
		return p;
	}


	private void renderRiwayatAnggota(Component parent, LibraryDashboardData data) {
		if (data.filterAnggota == null) {
			return;
		}
		Panelchildren pch = createPanel(parent, "Riwayat Saya sebagai Anggota",
				"Hanya tampil untuk anggota. Riwayat peminjaman, pengembalian, kunjungan, pemesanan, dan denda dibatasi pada data anggota yang sedang login sehingga anggota lain tidak dapat dilihat.");
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:10px;'>");
		sb.append(insightCard("Pinjaman Saya", formatNumber(data.totalPeminjamanPeriode) + " transaksi peminjaman pada periode ini.", "#fef3c7", "#92400e"));
		sb.append(insightCard("Pengembalian Saya", formatNumber(data.totalDikembalikan) + " transaksi pengembalian pada periode ini.", "#dcfce7", "#166534"));
		sb.append(insightCard("Kunjungan Saya", formatNumber(data.totalKunjungan) + " kunjungan tercatat pada periode ini.", "#e0f2fe", "#075985"));
		sb.append(insightCard("Denda Saya", "Total denda tercatat sekitar Rp " + formatMoney(data.totalDenda) + ".", "#fee2e2", "#991b1b"));
		sb.append("</div>");
		pch.appendChild(new Html(sb.toString()));
	}

	private void renderKeterlambatan(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Daftar Perhatian Keterlambatan",
				"Bagian ini memuat contoh pinjaman yang melewati batas waktu. Gunakan daftar ini untuk mengingatkan peminjam, mengecek status pengembalian, atau menghitung denda.");
		if (data.keterlambatan == null || data.keterlambatan.isEmpty()) {
			pch.appendChild(new Html("<div style='font-size:12px;color:#64748b;padding:10px;border-radius:12px;background:#f8fafc;'>Tidak ada data keterlambatan pada sampel yang diambil.</div>"));
			return;
		}
		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setWidth("100%");
		grid.setParent(pch);
		Rows rows = new Rows();
		rows.setParent(grid);
		for (OverdueInfo info : data.keterlambatan) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(info.namaItem));
			row.appendChild(new Label(info.peminjam));
			row.appendChild(new Label(formatDate(info.batasWaktu)));
			Label status = new Label(info.hariTerlambat + " hari");
			status.setStyle("font-weight:bold;color:#b91c1c;");
			row.appendChild(status);
		}
	}

	private void renderInsight(Component parent, LibraryDashboardData data) {
		Panelchildren pch = createPanel(parent, "Insight dan Saran Tindakan",
				"Merangkum hal yang paling penting untuk segera diperhatikan. Tujuannya agar petugas dapat menentukan prioritas kerja tanpa membaca seluruh tabel satu per satu.");
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:10px;'>");
		sb.append(insightCard("Ketersediaan", data.totalEksemplar <= 0 ? "Belum ada eksemplar fisik terdata." : "Terdapat " + formatNumber(data.totalEksemplar) + " eksemplar yang bisa dipantau statusnya.", "#eff6ff", "#1d4ed8"));
		sb.append(insightCard("Peminjaman", data.totalDipinjam <= 0 ? "Belum ada pinjaman aktif pada data saat ini." : formatNumber(data.totalDipinjam) + " eksemplar sedang dipinjam dan perlu dipantau sampai kembali.", "#fef3c7", "#92400e"));
		sb.append(insightCard("Kunjungan", data.totalKunjungan <= 0 ? "Belum ada kunjungan pada periode ini." : formatNumber(data.totalKunjungan) + " kunjungan tercatat pada periode yang dipilih.", "#e0f2fe", "#075985"));
		sb.append(insightCard("E-Book", data.totalEbook <= 0 ? "Belum ada koleksi digital pada filter ini." : formatNumber(data.totalEbook) + " koleksi memiliki akses digital/scan/lampiran.", "#f0fdf4", "#15803d"));
		sb.append(insightCard("Keterlambatan", data.totalTerlambat <= 0 ? "Tidak ada keterlambatan pada data yang terbaca." : formatNumber(data.totalTerlambat) + " pinjaman terlambat perlu ditindaklanjuti.", "#fee2e2", "#b91c1c"));
		sb.append(insightCard("Denda", data.totalDenda <= 0.0 ? "Belum ada denda tercatat atau semua denda sudah nol." : "Total denda tercatat sekitar Rp " + formatMoney(data.totalDenda) + ".", "#f5f3ff", "#6d28d9"));
		sb.append("</div>");
		pch.appendChild(new Html(sb.toString()));
	}

	private String insightCard(String title, String desc, String bg, String color) {
		return "<div style='border:1px solid #e2e8f0;border-radius:16px;padding:12px;background:#ffffff;'>"
				+ "<div style='display:inline-block;padding:5px 9px;border-radius:999px;background:" + bg + ";color:" + color
				+ ";font-size:10px;font-weight:900;text-transform:uppercase;'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:12px;color:#334155;line-height:1.6;margin-top:8px;'>" + escapeHtml(desc) + "</div></div>";
	}

	private LibraryDashboardData loadDashboardData() {
		LibraryDashboardData data = new LibraryDashboardData();
		ensureDefaultFilterValue();
		data.filterPerpustakaan = getSelectedFilterPerpustakaan();
		data.filterMulai = getFilterMulaiValue();
		data.filterSampai = getFilterSampaiValue();
		data.filterAnggota = currentAnggota;

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			data.totalKoleksi = countItems(session, data.filterPerpustakaan);
			data.totalEksemplar = countEksemplar(session, data.filterPerpustakaan);
			data.totalPenerbit = safeCount(session, Penerbit.class, null);
			data.totalDdc = safeCount(session, DdcItem.class, null);
			data.totalPeminjamanPeriode = countPeminjamanPeriode(session, data.filterPerpustakaan, data.filterMulai,
					data.filterSampai, data.filterAnggota);
			data.totalDipinjam = countPeminjamanAktif(session, data.filterPerpustakaan, data.filterAnggota);
			data.totalTerlambat = countKeterlambatan(session, data.filterPerpustakaan, data.filterAnggota);
			data.totalDikembalikan = countPengembalianPeriode(session, data.filterPerpustakaan, data.filterMulai,
					data.filterSampai, data.filterAnggota);
			data.totalKunjungan = countKunjunganPeriode(session, data.filterPerpustakaan, data.filterMulai,
					data.filterSampai, data.filterAnggota);
			data.totalPemesanan = countPemesananPeriode(session, data.filterPerpustakaan, data.filterMulai,
					data.filterSampai);
			data.totalDiblokir = countAnggotaDiblokir(session);
			data.totalDenda = sumDenda(session, data.filterPerpustakaan, data.filterMulai, data.filterSampai,
					data.filterAnggota);
			data.totalEbook = countEbook(session, data.filterPerpustakaan);
			data.koleksiPerTipe = loadKoleksiPerTipe(session, data.filterPerpustakaan);
			data.eksemplarPerPustaka = loadEksemplarPerPustaka(session, data.filterPerpustakaan);
			data.koleksiPopuler = loadKoleksiPopuler(session, data.filterPerpustakaan);
			data.koleksiTerbaru = loadKoleksiTerbaru(session, data.filterPerpustakaan);
			data.koleksiEbook = loadKoleksiEbook(session, data.filterPerpustakaan);
			data.keterlambatan = loadKeterlambatan(session, data.filterPerpustakaan, data.filterAnggota);
			data.trenPeminjaman = loadTrendPeminjaman(session, data.filterPerpustakaan, data.filterMulai,
					data.filterSampai, data.filterAnggota);
			data.trenPengembalian = loadTrendPengembalian(session, data.filterPerpustakaan, data.filterMulai,
					data.filterSampai, data.filterAnggota);
			data.trenKunjungan = loadTrendKunjungan(session, data.filterPerpustakaan, data.filterMulai,
					data.filterSampai, data.filterAnggota);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSessionSafely(session);
		}
		return data;
	}

	private int safeCount(Session session, Class clazz, Criterion criterion) {
		try {
			Criteria c = session.createCriteria(clazz).setProjection(Projections.rowCount());
			if (criterion != null) {
				c.add(criterion);
			}
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countItems(Session session, Perpustakaan perpustakaan) {
		try {
			if (perpustakaan == null) {
				Criteria c = session.createCriteria(Item.class).setProjection(Projections.rowCount());
				applyItemActiveRestriction(c, "");
				Object result = c.uniqueResult();
				return result == null ? 0 : ((Number) result).intValue();
			}
			Criteria c = session.createCriteria(ItemPunyaBarcode.class);
			c.add(Restrictions.eq("perpustakaan", perpustakaan));
			c.createAlias("item", "item", Criteria.LEFT_JOIN);
			applyItemActiveRestriction(c, "item.");
			c.setProjection(Projections.countDistinct("item.id"));
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countEksemplar(Session session, Perpustakaan perpustakaan) {
		try {
			Criteria c = session.createCriteria(ItemPunyaBarcode.class).setProjection(Projections.rowCount());
			if (perpustakaan != null) {
				c.add(Restrictions.eq("perpustakaan", perpustakaan));
			}
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countPeminjamanPeriode(Session session, Perpustakaan perpustakaan, Date mulai, Date sampai,
			Anggota anggota) {
		try {
			Criteria c = session.createCriteria(PeminjamanPengadaanItem.class).setProjection(Projections.rowCount());
			applyPerpustakaanFilter(c, "perpustakaan", perpustakaan);
			applyDateFilter(c, "tanggalPembuatan", mulai, sampai);
			if (anggota != null) {
				c.add(Restrictions.eq("anggota", anggota));
			}
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countPeminjamanAktif(Session session, Perpustakaan perpustakaan, Anggota anggota) {
		try {
			Criteria c = session.createCriteria(PeminjamanPengadaanItemDetail.class).setProjection(Projections.rowCount());
			c.createAlias("peminjamanPengadaanItem", "peminjaman", Criteria.LEFT_JOIN);
			applyPerpustakaanFilter(c, "peminjaman.perpustakaan", perpustakaan);
			if (anggota != null) {
				c.add(Restrictions.eq("peminjaman.anggota", anggota));
			}
			c.add(Restrictions.isNull("tanggalKembali"));
			c.add(Restrictions.isNull("kembaliPengadaanItemDetail"));
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countKeterlambatan(Session session, Perpustakaan perpustakaan, Anggota anggota) {
		try {
			Criteria c = session.createCriteria(PeminjamanPengadaanItemDetail.class).setProjection(Projections.rowCount());
			c.createAlias("peminjamanPengadaanItem", "peminjaman", Criteria.LEFT_JOIN);
			applyPerpustakaanFilter(c, "peminjaman.perpustakaan", perpustakaan);
			if (anggota != null) {
				c.add(Restrictions.eq("peminjaman.anggota", anggota));
			}
			c.add(Restrictions.isNull("tanggalKembali"));
			c.add(Restrictions.isNull("kembaliPengadaanItemDetail"));
			c.add(Restrictions.isNotNull("batasWaktupengembalian"));
			c.add(Restrictions.lt("batasWaktupengembalian", new Date()));
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countPengembalianPeriode(Session session, Perpustakaan perpustakaan, Date mulai, Date sampai,
			Anggota anggota) {
		try {
			Criteria c = session.createCriteria(KembaliPengadaanItem.class).setProjection(Projections.rowCount());
			applyPerpustakaanFilter(c, "perpustakaan", perpustakaan);
			applyDateFilter(c, "tanggalPembuatan", mulai, sampai);
			if (anggota != null) {
				c.createAlias("peminjamanPengadaanItem", "peminjaman", Criteria.LEFT_JOIN);
				c.add(Restrictions.eq("peminjaman.anggota", anggota));
			}
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countKunjunganPeriode(Session session, Perpustakaan perpustakaan, Date mulai, Date sampai,
			Anggota anggota) {
		try {
			Criteria c = session.createCriteria(KunjunganAnggota.class).setProjection(Projections.rowCount());
			applyPerpustakaanFilter(c, "perpustakaan", perpustakaan);
			applyDateFilter(c, "tanggal", mulai, sampai);
			if (anggota != null) {
				c.add(Restrictions.eq("anggota", anggota));
			}
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countPemesananPeriode(Session session, Perpustakaan perpustakaan, Date mulai, Date sampai) {
		try {
			Criteria c = session.createCriteria(PemesananPengadaanItem.class).setProjection(Projections.rowCount());
			applyPerpustakaanFilter(c, "perpustakaan", perpustakaan);
			applyDateFilter(c, "tanggalPembuatan", mulai, sampai);
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private double sumDenda(Session session, Perpustakaan perpustakaan, Date mulai, Date sampai, Anggota anggota) {
		try {
			Criteria c = session.createCriteria(KembaliPengadaanItemDetail.class).setProjection(Projections.sum("denda"));
			c.createAlias("kembaliPengadaanItem", "kembali", Criteria.LEFT_JOIN);
			applyPerpustakaanFilter(c, "kembali.perpustakaan", perpustakaan);
			applyDateFilter(c, "tanggal", mulai, sampai);
			if (anggota != null) {
				c.createAlias("kembali.peminjamanPengadaanItem", "peminjaman", Criteria.LEFT_JOIN);
				c.add(Restrictions.eq("peminjaman.anggota", anggota));
			}
			Object result = c.uniqueResult();
			return result == null ? 0.0 : ((Number) result).doubleValue();
		} catch (Exception e) {
			return 0.0;
		}
	}

	private int countEbook(Session session, Perpustakaan perpustakaan) {
		try {
			if (perpustakaan == null) {
				Criteria c = session.createCriteria(Item.class).setProjection(Projections.rowCount());
				applyItemActiveRestriction(c, "");
				applyEbookRestriction(c, "");
				Object result = c.uniqueResult();
				return result == null ? 0 : ((Number) result).intValue();
			}
			Criteria c = session.createCriteria(ItemPunyaBarcode.class);
			c.add(Restrictions.eq("perpustakaan", perpustakaan));
			c.createAlias("item", "item", Criteria.LEFT_JOIN);
			applyItemActiveRestriction(c, "item.");
			applyEbookRestriction(c, "item.");
			c.setProjection(Projections.countDistinct("item.id"));
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countItems(Session session) {
		try {
			Criteria c = session.createCriteria(Item.class).setProjection(Projections.rowCount());
			applyItemActiveRestriction(c, "");
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countPeminjamanAktif(Session session) {
		try {
			Criteria c = session.createCriteria(PeminjamanPengadaanItemDetail.class).setProjection(Projections.rowCount());
			c.add(Restrictions.isNull("tanggalKembali"));
			c.add(Restrictions.isNull("kembaliPengadaanItemDetail"));
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countKeterlambatan(Session session) {
		try {
			Criteria c = session.createCriteria(PeminjamanPengadaanItemDetail.class).setProjection(Projections.rowCount());
			c.add(Restrictions.isNull("tanggalKembali"));
			c.add(Restrictions.isNull("kembaliPengadaanItemDetail"));
			c.add(Restrictions.isNotNull("batasWaktupengembalian"));
			c.add(Restrictions.lt("batasWaktupengembalian", new Date()));
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int countAnggotaDiblokir(Session session) {
		try {
			Criteria c = session.createCriteria(AnggotaYangDiblokir.class).setProjection(Projections.rowCount());
			c.add(Restrictions.or(Restrictions.isNull("sampai"), Restrictions.ge("sampai", new Date())));
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private double sumDenda(Session session) {
		try {
			Criteria c = session.createCriteria(KembaliPengadaanItemDetail.class).setProjection(Projections.sum("denda"));
			Object result = c.uniqueResult();
			return result == null ? 0.0 : ((Number) result).doubleValue();
		} catch (Exception e) {
			return 0.0;
		}
	}

	@SuppressWarnings("unchecked")
	private List<NameValue> loadKoleksiPerTipe(Session session, Perpustakaan perpustakaan) {
		if (perpustakaan == null) {
			return loadKoleksiPerTipe(session);
		}
		List<NameValue> result = new ArrayList<NameValue>();
		try {
			Criteria c = session.createCriteria(ItemPunyaBarcode.class);
			c.add(Restrictions.eq("perpustakaan", perpustakaan));
			c.createAlias("item", "item", Criteria.LEFT_JOIN);
			c.createAlias("item.tipeItem", "tipeItem", Criteria.LEFT_JOIN);
			applyItemActiveRestriction(c, "item.");
			ProjectionList pl = Projections.projectionList();
			pl.add(Projections.groupProperty("tipeItem.nama"));
			pl.add(Projections.countDistinct("item.id"));
			c.setProjection(pl);
			List<Object[]> rows = c.list();
			for (Object[] row : rows) {
				String name = row[0] == null ? "Belum dikelompokkan" : String.valueOf(row[0]);
				int value = row[1] == null ? 0 : ((Number) row[1]).intValue();
				result.add(new NameValue(name, value));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1318");
		}
		sortNameValueDesc(result);
		return limitNameValue(result, 10);
	}

	@SuppressWarnings("unchecked")
	private List<NameValue> loadKoleksiPerTipe(Session session) {
		List<NameValue> result = new ArrayList<NameValue>();
		try {
			Criteria c = session.createCriteria(Item.class);
			applyItemActiveRestriction(c, "");
			c.createAlias("tipeItem", "tipeItem", Criteria.LEFT_JOIN);
			ProjectionList pl = Projections.projectionList();
			pl.add(Projections.groupProperty("tipeItem.nama"));
			pl.add(Projections.rowCount());
			c.setProjection(pl);
			List<Object[]> rows = c.list();
			for (Object[] row : rows) {
				String name = row[0] == null ? "Belum dikelompokkan" : String.valueOf(row[0]);
				int value = row[1] == null ? 0 : ((Number) row[1]).intValue();
				result.add(new NameValue(name, value));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1341");
		}
		sortNameValueDesc(result);
		return limitNameValue(result, 10);
	}

	@SuppressWarnings("unchecked")
	private List<NameValue> loadEksemplarPerPustaka(Session session, Perpustakaan perpustakaan) {
		if (perpustakaan == null) {
			return loadEksemplarPerPustaka(session);
		}
		List<NameValue> result = new ArrayList<NameValue>();
		try {
			Criteria c = session.createCriteria(ItemPunyaBarcode.class);
			c.add(Restrictions.eq("perpustakaan", perpustakaan));
			c.setProjection(Projections.rowCount());
			Object value = c.uniqueResult();
			result.add(new NameValue(safeString(perpustakaan.getNama()), value == null ? 0 : ((Number) value).intValue()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1359");
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<NameValue> loadEksemplarPerPustaka(Session session) {
		List<NameValue> result = new ArrayList<NameValue>();
		try {
			Criteria c = session.createCriteria(ItemPunyaBarcode.class);
			c.createAlias("perpustakaan", "perpustakaan", Criteria.LEFT_JOIN);
			ProjectionList pl = Projections.projectionList();
			pl.add(Projections.groupProperty("perpustakaan.nama"));
			pl.add(Projections.rowCount());
			c.setProjection(pl);
			List<Object[]> rows = c.list();
			for (Object[] row : rows) {
				String name = row[0] == null ? "Tanpa lokasi" : String.valueOf(row[0]);
				int value = row[1] == null ? 0 : ((Number) row[1]).intValue();
				result.add(new NameValue(name, value));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1380");
		}
		sortNameValueDesc(result);
		return limitNameValue(result, 10);
	}

	@SuppressWarnings("unchecked")
	private List<ItemInfo> loadKoleksiPopuler(Session session, Perpustakaan perpustakaan) {
		return loadItemInfoByCriteria(session, perpustakaan, false, true);
	}

	@SuppressWarnings("unchecked")
	private List<ItemInfo> loadKoleksiPopuler(Session session) {
		List<ItemInfo> result = new ArrayList<ItemInfo>();
		try {
			Criteria c = session.createCriteria(Item.class);
			applyItemActiveRestriction(c, "");
			c.addOrder(Order.desc("jumlahDilihat"));
			c.addOrder(Order.desc("jumlahDidownload"));
			c.addOrder(Order.desc("id"));
			c.setMaxResults(6);
			List<Item> items = c.list();
			for (Item item : items) {
				result.add(toItemInfo(item));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1405");
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<ItemInfo> loadKoleksiTerbaru(Session session, Perpustakaan perpustakaan) {
		return loadItemInfoByCriteria(session, perpustakaan, false, false);
	}

	@SuppressWarnings("unchecked")
	private List<ItemInfo> loadKoleksiEbook(Session session, Perpustakaan perpustakaan) {
		return loadItemInfoByCriteria(session, perpustakaan, true, false);
	}

	@SuppressWarnings("unchecked")
	private List<ItemInfo> loadItemInfoByCriteria(Session session, Perpustakaan perpustakaan, boolean ebookOnly, boolean populer) {
		List<ItemInfo> result = new ArrayList<ItemInfo>();
		try {
			if (perpustakaan == null) {
				Criteria c = session.createCriteria(Item.class);
				applyItemActiveRestriction(c, "");
				if (ebookOnly) {
					applyEbookRestriction(c, "");
				}
				if (populer) {
					c.addOrder(Order.desc("jumlahDilihat"));
					c.addOrder(Order.desc("jumlahDidownload"));
				}
				c.addOrder(Order.desc("id"));
				c.setMaxResults(6);
				List<Item> items = c.list();
				for (Item item : items) {
					result.add(toItemInfo(item));
				}
				return result;
			}
			Criteria idCriteria = session.createCriteria(ItemPunyaBarcode.class);
			idCriteria.add(Restrictions.eq("perpustakaan", perpustakaan));
			idCriteria.createAlias("item", "item", Criteria.LEFT_JOIN);
			applyItemActiveRestriction(idCriteria, "item.");
			if (ebookOnly) {
				applyEbookRestriction(idCriteria, "item.");
			}
			idCriteria.setProjection(Projections.distinct(Projections.property("item.id")));
			idCriteria.setMaxResults(30);
			List ids = idCriteria.list();
			if (ids == null || ids.isEmpty()) {
				return result;
			}
			Criteria c = session.createCriteria(Item.class);
			c.add(Restrictions.in("id", ids));
			if (populer) {
				c.addOrder(Order.desc("jumlahDilihat"));
				c.addOrder(Order.desc("jumlahDidownload"));
			}
			c.addOrder(Order.desc("id"));
			c.setMaxResults(6);
			List<Item> items = c.list();
			for (Item item : items) {
				result.add(toItemInfo(item));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1467");
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<ItemInfo> loadKoleksiTerbaru(Session session) {
		List<ItemInfo> result = new ArrayList<ItemInfo>();
		try {
			Criteria c = session.createCriteria(Item.class);
			applyItemActiveRestriction(c, "");
			c.addOrder(Order.desc("id"));
			c.setMaxResults(6);
			List<Item> items = c.list();
			for (Item item : items) {
				result.add(toItemInfo(item));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1484");
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<OverdueInfo> loadKeterlambatan(Session session, Perpustakaan perpustakaan, Anggota anggota) {
		List<OverdueInfo> result = new ArrayList<OverdueInfo>();
		try {
			Criteria c = session.createCriteria(PeminjamanPengadaanItemDetail.class);
			c.createAlias("item", "item", Criteria.LEFT_JOIN);
			c.createAlias("peminjamanPengadaanItem", "peminjaman", Criteria.LEFT_JOIN);
			c.createAlias("peminjaman.anggota", "anggota", Criteria.LEFT_JOIN);
			applyPerpustakaanFilter(c, "peminjaman.perpustakaan", perpustakaan);
			if (anggota != null) {
				c.add(Restrictions.eq("peminjaman.anggota", anggota));
			}
			c.add(Restrictions.isNull("tanggalKembali"));
			c.add(Restrictions.isNull("kembaliPengadaanItemDetail"));
			c.add(Restrictions.isNotNull("batasWaktupengembalian"));
			c.add(Restrictions.lt("batasWaktupengembalian", new Date()));
			c.addOrder(Order.asc("batasWaktupengembalian"));
			c.setMaxResults(15);
			List<PeminjamanPengadaanItemDetail> rows = c.list();
			for (PeminjamanPengadaanItemDetail detail : rows) {
				OverdueInfo info = new OverdueInfo();
				info.namaItem = detail.getItem() == null ? "-" : safeString(detail.getItem().getNama());
				try {
					info.peminjam = detail.getPeminjamanPengadaanItem() == null
							|| detail.getPeminjamanPengadaanItem().getAnggota() == null ? "-"
									: safeString(detail.getPeminjamanPengadaanItem().getAnggota().toString());
				} catch (Exception e) {
					info.peminjam = "-";
				}
				info.batasWaktu = detail.getBatasWaktupengembalian();
				info.hariTerlambat = hitungSelisihHari(info.batasWaktu, new Date());
				result.add(info);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1522");
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<NameValue> loadTrendPeminjaman(Session session, Perpustakaan perpustakaan, Date mulai, Date sampai,
			Anggota anggota) {
		Map<String, Integer> map = new java.util.TreeMap<String, Integer>();
		try {
			Criteria c = session.createCriteria(PeminjamanPengadaanItem.class);
			applyPerpustakaanFilter(c, "perpustakaan", perpustakaan);
			applyDateFilter(c, "tanggalPembuatan", mulai, sampai);
			if (anggota != null) {
				c.add(Restrictions.eq("anggota", anggota));
			}
			c.addOrder(Order.desc("tanggalPembuatan"));
			c.setMaxResults(DASHBOARD_SAMPLE_LIMIT);
			List<PeminjamanPengadaanItem> rows = c.list();
			for (PeminjamanPengadaanItem row : rows) {
				addTrend(map, trendKey(row == null ? null : row.getTanggalPembuatan()));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1544");
		}
		return toTrendList(map);
	}

	@SuppressWarnings("unchecked")
	private List<NameValue> loadTrendPengembalian(Session session, Perpustakaan perpustakaan, Date mulai, Date sampai,
			Anggota anggota) {
		Map<String, Integer> map = new java.util.TreeMap<String, Integer>();
		try {
			Criteria c = session.createCriteria(KembaliPengadaanItem.class);
			applyPerpustakaanFilter(c, "perpustakaan", perpustakaan);
			applyDateFilter(c, "tanggalPembuatan", mulai, sampai);
			if (anggota != null) {
				c.createAlias("peminjamanPengadaanItem", "peminjaman", Criteria.LEFT_JOIN);
				c.add(Restrictions.eq("peminjaman.anggota", anggota));
			}
			c.addOrder(Order.desc("tanggalPembuatan"));
			c.setMaxResults(DASHBOARD_SAMPLE_LIMIT);
			List<KembaliPengadaanItem> rows = c.list();
			for (KembaliPengadaanItem row : rows) {
				addTrend(map, trendKey(row == null ? null : row.getTanggalPembuatan()));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1567");
		}
		return toTrendList(map);
	}

	@SuppressWarnings("unchecked")
	private List<NameValue> loadTrendKunjungan(Session session, Perpustakaan perpustakaan, Date mulai, Date sampai,
			Anggota anggota) {
		Map<String, Integer> map = new java.util.TreeMap<String, Integer>();
		try {
			Criteria c = session.createCriteria(KunjunganAnggota.class);
			applyPerpustakaanFilter(c, "perpustakaan", perpustakaan);
			applyDateFilter(c, "tanggal", mulai, sampai);
			if (anggota != null) {
				c.add(Restrictions.eq("anggota", anggota));
			}
			c.addOrder(Order.desc("tanggal"));
			c.setMaxResults(DASHBOARD_SAMPLE_LIMIT);
			List<KunjunganAnggota> rows = c.list();
			for (KunjunganAnggota row : rows) {
				addTrend(map, trendKey(row == null ? null : row.getTanggal()));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1589");
		}
		return toTrendList(map);
	}

	private void addTrend(Map<String, Integer> map, String key) {
		if (key == null) {
			key = "Tanpa Tanggal";
		}
		Integer value = map.get(key);
		map.put(key, Integer.valueOf(value == null ? 1 : value.intValue() + 1));
	}

	@SuppressWarnings("unchecked")
	private List<OverdueInfo> loadKeterlambatan(Session session) {
		List<OverdueInfo> result = new ArrayList<OverdueInfo>();
		try {
			Criteria c = session.createCriteria(PeminjamanPengadaanItemDetail.class);
			c.createAlias("item", "item", Criteria.LEFT_JOIN);
			c.createAlias("peminjamanPengadaanItem", "peminjaman", Criteria.LEFT_JOIN);
			c.createAlias("peminjaman.anggota", "anggota", Criteria.LEFT_JOIN);
			c.add(Restrictions.isNull("tanggalKembali"));
			c.add(Restrictions.isNull("kembaliPengadaanItemDetail"));
			c.add(Restrictions.isNotNull("batasWaktupengembalian"));
			c.add(Restrictions.lt("batasWaktupengembalian", new Date()));
			c.addOrder(Order.asc("batasWaktupengembalian"));
			c.setMaxResults(15);
			List<PeminjamanPengadaanItemDetail> rows = c.list();
			for (PeminjamanPengadaanItemDetail detail : rows) {
				OverdueInfo info = new OverdueInfo();
				info.namaItem = detail.getItem() == null ? "-" : safeString(detail.getItem().getNama());
				try {
					info.peminjam = detail.getPeminjamanPengadaanItem() == null || detail.getPeminjamanPengadaanItem().getAnggota() == null ? "-"
							: safeString(detail.getPeminjamanPengadaanItem().getAnggota().toString());
				} catch (Exception e) {
					info.peminjam = "-";
				}
				info.batasWaktu = detail.getBatasWaktupengembalian();
				info.hariTerlambat = hitungSelisihHari(info.batasWaktu, new Date());
				result.add(info);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1630");
		}
		return result;
	}

	private ItemInfo toItemInfo(Item item) {
		ItemInfo info = new ItemInfo();
		info.id = item == null ? null : item.getId();
		info.nama = item == null || item.getNama() == null ? "Tanpa judul" : item.getNama();
		String pengarang = "";
		try {
			pengarang = item.getPengarangs() == null ? "" : item.getPengarangs();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1642");
		}
		String tahun = "";
		try {
			tahun = item.getTahun() == null ? "" : String.valueOf(item.getTahun());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1647");
		}
		info.subInfo = (pengarang == null || pengarang.trim().length() == 0 ? "Tidak ada pengarang" : pengarang)
				+ (tahun == null || tahun.trim().length() == 0 ? "" : " • " + tahun);
		try {
			Long dilihat = item.getJumlahDilihat();
			info.dilihat = dilihat == null ? 0 : dilihat.intValue();
		} catch (Exception e) {
			info.dilihat = 0;
		}
		try {
			Long diunduh = item.getJumlahDidownload();
			info.diunduh = diunduh == null ? 0 : diunduh.intValue();
		} catch (Exception e) {
			info.diunduh = 0;
		}
		info.coverUrl = getCoverUrl(item);
		info.ebook = isEbookItem(item);
		info.readUrl = buildEbookReadUrl(item);
		info.downloadUrl = buildEbookDownloadUrl(item);
		return info;
	}

	private boolean isEbookItem(Item item) {
		if (item == null) {
			return false;
		}
		try {
			if (item.getBolehDiDownload()) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1678");
		}
		try {
			if (item.getHasScan() != null && item.getHasScan().booleanValue()) {
				return true;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1684");
		}
		return hasText(safeItemText(getLampiranPathSafely(item))) || hasText(safeItemText(getScanLinksSafely(item)))
				|| hasText(safeItemText(getLinkSafely(item)));
	}

	private String buildEbookReadUrl(Item item) {
		String url = normalizeUrl(firstUrl(getScanLinksSafely(item)));
		if (url.length() > 0) {
			return url;
		}
		url = normalizeUrl(getLinkSafely(item));
		if (url.length() > 0) {
			return url;
		}
		url = normalizeUrl(getLampiranPathSafely(item));
		return url;
	}

	private String buildEbookDownloadUrl(Item item) {
		String url = normalizeUrl(getLampiranPathSafely(item));
		if (url.length() > 0) {
			return url;
		}
		url = normalizeUrl(firstUrl(getScanLinksSafely(item)));
		if (url.length() > 0) {
			return url;
		}
		return normalizeUrl(getLinkSafely(item));
	}

	private String getLampiranPathSafely(Item item) {
		try {
			return item == null ? "" : item.getLampiranPath();
		} catch (Exception e) {
			return "";
		}
	}

	private String getScanLinksSafely(Item item) {
		try {
			return item == null ? "" : item.getScanLinks();
		} catch (Exception e) {
			return "";
		}
	}

	private String getLinkSafely(Item item) {
		try {
			return item == null ? "" : item.getLink();
		} catch (Exception e) {
			return "";
		}
	}

	private String firstUrl(String text) {
		if (text == null) {
			return "";
		}
		String[] parts = text.split("[\\n,;|]");
		for (int i = 0; i < parts.length; i++) {
			String p = parts[i] == null ? "" : parts[i].trim();
			if (p.length() > 0) {
				return p;
			}
		}
		return "";
	}

	private String normalizeUrl(String url) {
		if (url == null) {
			return "";
		}
		String u = url.trim();
		if (u.length() == 0) {
			return "";
		}
		String lower = u.toLowerCase(java.util.Locale.ENGLISH);
		if (lower.startsWith("http://") || lower.startsWith("https://")) {
			return u;
		}
		if (u.startsWith("/")) {
			return Common.getRequestHostWithProtocol() + u;
		}
		if (lower.startsWith("www.")) {
			return "http://" + u;
		}
		return Common.getRequestHostWithProtocol() + "/" + u;
	}

	private String safeItemText(String value) {
		return value == null ? "" : value.trim();
	}

	private boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}

	private void renderKatalogPustaka(Component parent) throws Exception {
		Common.clear(parent);
		// Center->scroll (bukan Div "overflow:auto" manual) — WAJIB di sini karena tab ini
		// berisi Tabbox bersarang ber-height:100% (tabboxUtama di bawah); Div biasa membuat
		// height:100% kehilangan acuan dan kolaps (lihat javadoc tampilanScrollTabbox).
		Center shell = Common.tampilanScrollTabbox(parent);
		// PENTING: Center (child Borderlayout) tak boleh di-setWidth() langsung
		// (readonly di ZK 5.5 — lebar Center mengikuti parent Borderlayout) dan
		// hanya boleh punya SATU child langsung, jadi lebar/style yang dimaksud
		// ditaruh pada Div pembungkus ini, bukan pada shell/Center-nya.
		Div wrapper = new Div();
		wrapper.setWidth("100%");
		wrapper.setStyle("padding:14px; box-sizing:border-box; background:#ffffff;");
		wrapper.setParent(shell);

		PerguruanTinggi perguruanTinggi = null;
		try {
			perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1792");
		}
		String namaPt = perguruanTinggi == null || perguruanTinggi.getNama() == null ? "Pustaka Digital" : perguruanTinggi.getNama();
		wrapper.appendChild(new Html("<div style='border-radius:20px;background:#f8fafc;border:1px solid #e2e8f0;padding:18px;margin-bottom:12px;'>"
				+ "<div style='font-size:22px;font-weight:900;color:#0f172a;'>Selamat Datang di Pustaka " + escapeHtml(namaPt) + "</div>"
				+ "<div style='font-size:12px;color:#64748b;line-height:1.6;margin-top:8px;'>Gunakan tab ini untuk mencari koleksi perpustakaan berdasarkan judul, ISBN/ISSN, pengarang, tema, penerbit, kategori, jenis, bahasa, klasifikasi, edisi, tahun, atau jumlah halaman.</div>"
				+ "</div>"));

		final Tabbox tabboxUtama = new Tabbox();
		tabboxUtama.setWidth("100%");
		tabboxUtama.setHeight("100%");
		tabboxUtama.setStyle("border:0; overflow:auto;");
		tabboxUtama.setParent(wrapper);

		Tabs tabsUtama = new Tabs();
		tabsUtama.setParent(tabboxUtama);
		Tabpanels tabpanelsUtama = new Tabpanels();
		tabpanelsUtama.setParent(tabboxUtama);

		List<Perpustakaan> perpustakaans = getDaftarPerpustakaanAktif();
		if (currentAnggota == null && currentPerpustakaan != null) {
			perpustakaans.clear();
			perpustakaans.add(currentPerpustakaan);
		} else {
			perpustakaans.add(null);
		}

		int o = 0;
		for (final Perpustakaan perpustakaan : perpustakaans) {
			String label = perpustakaan == null ? "Semua Koleksi" : safeString(perpustakaan.getNama());
			MyTabConfig tab = new MyTabConfig(label, "/img/Blue-Books-icon.png");
			tab.setParent(tabsUtama);

			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setWidth("100%");
			tabpanel.setStyle("padding:10px; overflow:auto;");
			tabpanel.setParent(tabpanelsUtama);

			final EventListener listener = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (tabpanel.getChildren().isEmpty()) {
						if (perpustakaan != null) {
							PustakaAction.berkunjung(perpustakaan, tbmuser);
						}
						renderKatalogPerPerpustakaan(tabpanel, perpustakaan);
					}
				}
			};
			tab.addEventListener("onClick", listener);
			if (o == 0) {
				listener.onEvent(null);
			}
			o++;
		}
	}

	private List<Perpustakaan> getDaftarPerpustakaanAktif() {
		List<Perpustakaan> result = new ArrayList<Perpustakaan>();
		try {
			Map<Long, Perpustakaan> pustaka = ConstantValues.ambilBerdasarClass(Perpustakaan.class);
			if (pustaka != null) {
				for (Perpustakaan p : pustaka.values()) {
					if (p != null && booleanTrue(p.getAktif())) {
						result.add(p);
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1860");
		}
		Collections.sort(result, new Comparator<Perpustakaan>() {
			@Override
			public int compare(Perpustakaan o1, Perpustakaan o2) {
				String n1 = o1 == null || o1.getNama() == null ? "" : o1.getNama();
				String n2 = o2 == null || o2.getNama() == null ? "" : o2.getNama();
				return n1.compareToIgnoreCase(n2);
			}
		});
		return result;
	}

	private void renderKatalogPerPerpustakaan(final Component parent, final Perpustakaan perpustakaan) throws Exception {
		Common.clear(parent);
		Vbox container = new Vbox();
		container.setWidth("100%");
		container.setStyle("gap:10px;");
		container.setParent(parent);

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("padding:10px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;white-space:normal;");
		toolbar.setParent(container);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Cari koleksi: ")));
		final Textbox cari = new Textbox();
		cari.setWidth(mobile ? "95%" : "260px");
		cari.setParent(toolbar);

		final Div resultPanel = new Div();
		resultPanel.setWidth("100%");
		resultPanel.setStyle("overflow:auto;");

		final Tabbox tabboxTipe = new Tabbox();
		// Permintaan: tab jenis koleksi dipindah dari KIRI (vertical) ke ATAS (horizontal),
		// agar konsisten dengan pola tab umum dan lebih hemat ruang di layar mobile.
		tabboxTipe.setOrient("horizontal");
		tabboxTipe.setWidth("100%");
		tabboxTipe.setStyle("border:0; overflow-x:auto; overflow-y:hidden;");
		tabboxTipe.setSclass("pustaka-tab-jenis");
		tabboxTipe.setParent(container);

		Tabs tabs = new Tabs();
		// Lebar 34px dulu dipakai untuk kolom tab vertikal yang sempit; untuk tab horizontal
		// di atas, biarkan mengisi lebar penuh dan boleh membungkus baris (responsif).
		tabs.setWidth("100%");
		tabs.setStyle("white-space:nowrap; overflow-x:auto; overflow-y:hidden;");
		tabs.setParent(tabboxTipe);
		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabboxTipe);

		MyTabConfig semuaTab = new MyTabConfig("Semua Jenis", "/img/Books-icon1.png");
		semuaTab.setStyle("white-space:nowrap; min-width:110px; text-align:center;");
		semuaTab.setAttribute("tipeItem", null);
		semuaTab.setParent(tabs);
		final Tabpanel semuaPanel = new ais.ui.util.MyTabpanel();
		semuaPanel.setStyle("padding:8px;overflow:auto;");
		semuaPanel.setParent(tabpanels);

		final MyWindow advancedSearch = initPencarian(tabboxTipe, cari, perpustakaan);

		Toolbarbutton btnCari = new MyToolbarbuttonConfig("Cari / Refresh", "/img/svg/search.svg");
		btnCari.setParent(toolbar);
		btnCari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Tab tab = tabboxTipe.getSelectedTab();
				resetAdvancedFilter();
				renderDaftarKoleksi((TipeItem) tab.getAttribute("tipeItem"), tab.getLinkedPanel(), cari, perpustakaan, 0);
			}
		});

		Toolbarbutton btnLanjut = new MyToolbarbuttonConfig("Pencarian lanjut", "/img/Zoom-icon.png");
		btnLanjut.setParent(toolbar);
		btnLanjut.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				advancedSearch.onModal();
			}
		});

		cari.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Tab tab = tabboxTipe.getSelectedTab();
				resetAdvancedFilter();
				renderDaftarKoleksi((TipeItem) tab.getAttribute("tipeItem"), tab.getLinkedPanel(), cari, perpustakaan, 0);
			}
		});

		renderDaftarKoleksi(null, semuaPanel, cari, perpustakaan, 0);

		Map<Long, TipeItem> tipeItems = null;
		try {
			tipeItems = ConstantValues.ambilBerdasarClass(TipeItem.class);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:1953");
		}
		if (tipeItems != null) {
			for (final TipeItem tipeItem : tipeItems.values()) {
				if (tipeItem != null && booleanTrue(tipeItem.getAktif())) {
					final Tab tab = new Tab(safeString(tipeItem.getNama()), "/img/Books-icon1.png");
					tab.setStyle("white-space:nowrap; min-width:86px; text-align:center;");
					tab.setAttribute("tipeItem", tipeItem);
					tab.setParent(tabs);
					final Tabpanel panel = new ais.ui.util.MyTabpanel();
					panel.setStyle("padding:8px;overflow:auto;");
					panel.setParent(tabpanels);
					tab.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							renderDaftarKoleksi(tipeItem, panel, cari, perpustakaan, 0);
						}
					});
				}
			}
		}
	}

	private MyWindow initPencarian(final Tabbox tabbox, final Textbox cari, final Perpustakaan perpustakaan) {
		final MyWindow win = new MyWindow("Pencarian Lanjut", "normal", true);
		win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		win.setHeight("98%");
		win.setWidth(mobile ? "98%" : "360px");
		win.setVisible(false);
		win.setStyle("overflow:auto;");

		Borderlayout bl = new Borderlayout();
		bl.setWidth("100%");
		bl.setHeight("100%");
		bl.setParent(win);

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setBorder("none");
		center.setStyle("overflow:auto;");
		center.setParent(bl);

		Grid grid = new Grid();
		grid.setWidth("100%");
		grid.setSclass("fgrid");
		grid.setParent(center);
		Rows rows = new Rows();
		rows.setParent(grid);

		cariIsbn = addSearchRow(rows, "ISBN/ISSN");
		cariJudul = addSearchRow(rows, "Judul");
		cariTema = addSearchRow(rows, "Tema");
		cariPengarang = addSearchRow(rows, "Pengarang");
		cariPenerbit = addSearchRow(rows, "Penerbit");
		cariKategori = addSearchRow(rows, "Kategori");
		cariJenis = addSearchRow(rows, "Jenis");
		cariBahasa = addSearchRow(rows, "Bahasa");
		cariPenaklikan = addSearchRow(rows, "Deskripsi Fisik / Penaklikan");
		cariKlasifikasi = addSearchRow(rows, "Klasifikasi");
		cariEdisi = addSearchRow(rows, "Edisi");
		cariCatatan = addSearchRow(rows, "Catatan / Abstrak");
		cariTahun = addSearchRow(rows, "Tahun");
		cariHalaman = addSearchRow(rows, "Halaman");

		EventListener searchListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Tab tab = tabbox.getSelectedTab();
				renderDaftarKoleksi((TipeItem) tab.getAttribute("tipeItem"), tab.getLinkedPanel(), cari, perpustakaan, 0);
				win.setVisible(false);
			}
		};
		attachOkSearch(searchListener);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(bl);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		btnTutup.setParent(toolbar);
		btnTutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				win.setVisible(false);
			}
		});
		MyToolbarbuttonConfig btnReset = new MyToolbarbuttonConfig("Reset", "/img/svg/refresh.svg");
		btnReset.setParent(toolbar);
		btnReset.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				resetAdvancedFilter();
			}
		});
		MyToolbarbuttonConfig btnCari = new MyToolbarbuttonConfig("Cari", "/img/search.png");
		btnCari.setParent(toolbar);
		btnCari.addEventListener("onClick", searchListener);
		return win;
	}

	private Textbox addSearchRow(Rows rows, String label) {
		Row labelRow = new Row();
		labelRow.setParent(rows);
		labelRow.appendChild(new MyLabelBoldAja(label));
		Row valueRow = new Row();
		valueRow.setParent(rows);
		Textbox textbox = new Textbox();
		textbox.setWidth("94%");
		textbox.setParent(valueRow);
		return textbox;
	}

	private void attachOkSearch(EventListener listener) {
		Textbox[] boxes = new Textbox[] { cariIsbn, cariJudul, cariTema, cariPengarang, cariPenerbit, cariKategori,
				cariJenis, cariBahasa, cariPenaklikan, cariKlasifikasi, cariEdisi, cariCatatan, cariTahun, cariHalaman };
		for (int i = 0; i < boxes.length; i++) {
			if (boxes[i] != null) {
				try {
					boxes[i].addEventListener("onOK", listener);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:2073");
				}
			}
		}
	}

	private void resetAdvancedFilter() {
		setValue(cariIsbn, "");
		setValue(cariJudul, "");
		setValue(cariPengarang, "");
		setValue(cariTema, "");
		setValue(cariPenerbit, "");
		setValue(cariJenis, "");
		setValue(cariKategori, "");
		setValue(cariBahasa, "");
		setValue(cariKlasifikasi, "");
		setValue(cariTahun, "");
		setValue(cariHalaman, "");
		setValue(cariEdisi, "");
		setValue(cariCatatan, "");
		setValue(cariPenaklikan, "");
	}

	private void setValue(Textbox box, String value) {
		if (box != null) {
			box.setValue(value == null ? "" : value);
		}
	}

	private void renderDaftarKoleksi(final TipeItem tipeItem, final Component parent, final Textbox cari,
			final Perpustakaan perpustakaan, final int activePage) {
		if (parent == null) {
			return;
		}
		Common.clear(parent);
		parent.appendChild(new Html("<div style='font-size:12px;color:#64748b;line-height:1.6;margin-bottom:10px;'>Menampilkan koleksi sesuai kata kunci dan filter yang dipilih. Gunakan tombol pencarian lanjut bila ingin mencari lebih spesifik berdasarkan ISBN, pengarang, penerbit, tahun, atau klasifikasi.</div>"));

		Session session = null;
		int total = 0;
		List<Item> items = new ArrayList<Item>();
		try {
			session = HibernateUtil.openSession();
			total = countKoleksi(session, tipeItem, cari, perpustakaan);
			items = loadKoleksi(session, tipeItem, cari, perpustakaan, activePage);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeSessionSafely(session);
		}

		if (total <= 0) {
			parent.appendChild(new Html("<div style='padding:16px;border-radius:14px;background:#fff7ed;border:1px solid #fed7aa;color:#9a3412;font-size:12px;'>Tidak ada koleksi yang ditemukan. Coba kurangi kata kunci atau reset filter pencarian.</div>"));
			return;
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setParent(parent);
		Rows rows = new Rows();
		rows.setParent(grid);

		if (mobile) {
			for (Item item : items) {
				Row row = new Row();
				row.setValign("top");
				rows.appendChild(row);
				renderKoleksiCell(item, row, perpustakaan);
			}
		} else {
			int col = 3;
			List<Item> sub = new ArrayList<Item>();
			for (Item item : items) {
				sub.add(item);
				if (sub.size() >= col) {
					renderKoleksiRow(rows, sub, col, perpustakaan);
					sub = new ArrayList<Item>();
				}
			}
			if (!sub.isEmpty()) {
				renderKoleksiRow(rows, sub, col, perpustakaan);
			}
		}

		int nextPage = activePage + 1;
		int shown = nextPage * jumlahDataDalamSatuHalamanElearning;
		if (total > shown) {
			Row row = new Row();
			row.setValign("top");
			row.setAlign("center");
			if (!mobile) {
				ais.ui.util.ZkCompat.setSpans(row, "4");
			}
			rows.appendChild(row);
			MyToolbarbutton next = new MyToolbarbutton("fa-spinner", "Tampilkan koleksi selanjutnya.. (sisa "
					+ formatNumber(total - shown) + " data)");
			next.setStyle("font-size:16px;color:#0f172a;margin:10px;");
			next.setParent(row);
			next.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					renderDaftarKoleksi(tipeItem, parent, cari, perpustakaan, activePage + 1);
				}
			});
		}
	}

	private void renderKoleksiRow(Rows rows, List<Item> items, int col, Perpustakaan perpustakaan) {
		Row row = new Row();
		row.setValign("top");
		rows.appendChild(row);
		for (Item item : items) {
			renderKoleksiCell(item, row, perpustakaan);
		}
		for (int i = items.size(); i < col; i++) {
			row.appendChild(new Label());
		}
	}

	private void renderKoleksiCell(Item item, Row row, Perpustakaan perpustakaan) {
		try {
			PustakaAction.displayPustaka(item, row, mobile, tbmuser, perpustakaan);
		} catch (Exception e) {
			try {
				Label label = new Label(ais.common.Common.getBahasaConfig("Koleksi belum dapat ditampilkan"));
				label.setStyle("display:block;padding:12px;border-radius:12px;background:#fff7ed;border:1px solid #fed7aa;color:#9a3412;font-size:11px;");
				label.setParent(row);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:2200");
			}
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:2204");
			}
		}
	}

	private int countKoleksi(Session session, TipeItem tipeItem, Textbox cari, Perpustakaan perpustakaan) {
		try {
			Criteria c;
			if (perpustakaan == null) {
				c = session.createCriteria(Item.class);
				applyItemFilter(c, "", tipeItem, cari);
				c.setProjection(Projections.rowCount());
			} else {
				c = session.createCriteria(ItemPunyaBarcode.class);
				c.add(Restrictions.eq("perpustakaan", perpustakaan));
				c.createAlias("item", "item", Criteria.LEFT_JOIN);
				applyItemFilter(c, "item.", tipeItem, cari);
				c.setProjection(Projections.countDistinct("item.id"));
			}
			Object result = c.uniqueResult();
			return result == null ? 0 : ((Number) result).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	private List<Item> loadKoleksi(Session session, TipeItem tipeItem, Textbox cari, Perpustakaan perpustakaan,
			int activePage) {
		try {
			if (perpustakaan == null) {
				Criteria c = session.createCriteria(Item.class);
				applyItemFilter(c, "", tipeItem, cari);
				c.addOrder(Order.desc("id"));
				c.setMaxResults(jumlahDataDalamSatuHalamanElearning);
				c.setFirstResult(jumlahDataDalamSatuHalamanElearning * Math.max(0, activePage));
				return c.list();
			}

			Criteria idCriteria = session.createCriteria(ItemPunyaBarcode.class);
			idCriteria.add(Restrictions.eq("perpustakaan", perpustakaan));
			idCriteria.createAlias("item", "item", Criteria.LEFT_JOIN);
			applyItemFilter(idCriteria, "item.", tipeItem, cari);
			idCriteria.setProjection(Projections.distinct(Projections.property("item.id")));
			idCriteria.setMaxResults(jumlahDataDalamSatuHalamanElearning);
			idCriteria.setFirstResult(jumlahDataDalamSatuHalamanElearning * Math.max(0, activePage));
			List ids = idCriteria.list();
			if (ids == null || ids.isEmpty()) {
				return new ArrayList<Item>();
			}
			Criteria itemCriteria = session.createCriteria(Item.class);
			itemCriteria.add(Restrictions.in("id", ids));
			itemCriteria.addOrder(Order.desc("id"));
			return itemCriteria.list();
		} catch (Exception e) {
			return new ArrayList<Item>();
		}
	}

	private void applyItemFilter(Criteria c, String prefix, TipeItem tipeItem, Textbox cari) {
		applyItemActiveRestriction(c, prefix);
		if (tipeItem != null) {
			c.add(Restrictions.eq(prefix + "tipeItem", tipeItem));
		}
		if (hasText(cariPenerbit)) {
			c.createAlias(prefix + "penerbit", "penerbit", Criteria.LEFT_JOIN)
					.createAlias(prefix + "penerbit2", "penerbit2", Criteria.LEFT_JOIN)
					.createAlias(prefix + "penerbit3", "penerbit3", Criteria.LEFT_JOIN)
					.createAlias(prefix + "penerbit4", "penerbit4", Criteria.LEFT_JOIN)
					.createAlias(prefix + "penerbit5", "penerbit5", Criteria.LEFT_JOIN);
		}
		if (hasText(cariJenis)) {
			c.createAlias(prefix + "jenisItem", "jenisItem", Criteria.LEFT_JOIN);
		}
		c.add(initCriterionCari(prefix, cari));
	}

	private void applyItemActiveRestriction(Criteria c, String prefix) {
		c.add(Restrictions.or(Restrictions.isNull(prefix + "aktif"), Restrictions.eq(prefix + "aktif", true)));
		c.add(Restrictions.or(Restrictions.isNull(prefix + "folder"), Restrictions.eq(prefix + "folder", false)));
	}

	private Criterion initCriterionCari(String prefix, Textbox cari) {
		Criterion criterionCari = Restrictions.sqlRestriction("true");
		if (hasText(cariIsbn)) {
			setValue(cari, "");
			Criterion car = Restrictions.ilike(prefix + "isbn", value(cariIsbn), MatchMode.ANYWHERE);
			car = Restrictions.or(car, Restrictions.ilike(prefix + "isbn10", value(cariIsbn), MatchMode.ANYWHERE));
			car = Restrictions.or(car, Restrictions.ilike(prefix + "issn", value(cariIsbn), MatchMode.ANYWHERE));
			criterionCari = Restrictions.and(criterionCari, car);
		}
		if (hasText(cariCatatan)) {
			setValue(cari, "");
			Criterion car = Restrictions.ilike(prefix + "catatan", value(cariCatatan), MatchMode.ANYWHERE);
			car = Restrictions.or(car, Restrictions.ilike(prefix + "abstrak", value(cariCatatan), MatchMode.ANYWHERE));
			car = Restrictions.or(car, Restrictions.ilike(prefix + "abstrakEn", value(cariCatatan), MatchMode.ANYWHERE));
			car = Restrictions.or(car, Restrictions.ilike(prefix + "textSnippet", value(cariCatatan), MatchMode.ANYWHERE));
			criterionCari = Restrictions.and(criterionCari, car);
		}
		if (hasText(cariPenaklikan)) {
			setValue(cari, "");
			criterionCari = Restrictions.and(criterionCari, Restrictions.ilike(prefix + "penaklikan", value(cariPenaklikan), MatchMode.ANYWHERE));
		}
		if (hasText(cariJudul)) {
			setValue(cari, "");
			criterionCari = Restrictions.and(criterionCari, Restrictions.ilike(prefix + "nama", value(cariJudul), MatchMode.ANYWHERE));
		}
		if (hasText(cariPengarang)) {
			setValue(cari, "");
			criterionCari = Restrictions.and(criterionCari, Restrictions.ilike(prefix + "pengarangs", value(cariPengarang), MatchMode.ANYWHERE));
		}
		if (hasText(cariTema)) {
			setValue(cari, "");
			criterionCari = Restrictions.and(criterionCari, Restrictions.ilike(prefix + "tema", value(cariTema), MatchMode.ANYWHERE));
		}
		if (hasText(cariPenerbit)) {
			setValue(cari, "");
			Criterion car = Restrictions.ilike("penerbit.nama", value(cariPenerbit), MatchMode.ANYWHERE);
			car = Restrictions.or(car, Restrictions.ilike("penerbit2.nama", value(cariPenerbit), MatchMode.ANYWHERE));
			car = Restrictions.or(car, Restrictions.ilike("penerbit3.nama", value(cariPenerbit), MatchMode.ANYWHERE));
			car = Restrictions.or(car, Restrictions.ilike("penerbit4.nama", value(cariPenerbit), MatchMode.ANYWHERE));
			car = Restrictions.or(car, Restrictions.ilike("penerbit5.nama", value(cariPenerbit), MatchMode.ANYWHERE));
			criterionCari = Restrictions.and(criterionCari, car);
		}
		if (hasText(cariKategori)) {
			setValue(cari, "");
			criterionCari = Restrictions.and(criterionCari, Restrictions.ilike(prefix + "kategories", value(cariKategori), MatchMode.ANYWHERE));
		}
		if (hasText(cariJenis)) {
			setValue(cari, "");
			criterionCari = Restrictions.and(criterionCari, Restrictions.ilike("jenisItem.nama", value(cariJenis), MatchMode.ANYWHERE));
		}
		if (hasText(cariEdisi)) {
			setValue(cari, "");
			criterionCari = Restrictions.and(criterionCari, Restrictions.ilike(prefix + "edisi", value(cariEdisi), MatchMode.ANYWHERE));
		}
		if (hasText(cariBahasa)) {
			setValue(cari, "");
			criterionCari = Restrictions.and(criterionCari, Restrictions.ilike(prefix + "bahasa", value(cariBahasa), MatchMode.ANYWHERE));
		}
		if (hasText(cariKlasifikasi)) {
			setValue(cari, "");
			criterionCari = Restrictions.and(criterionCari, Restrictions.ilike(prefix + "deweyDecimalClass", value(cariKlasifikasi), MatchMode.ANYWHERE));
		}
		if (hasText(cariTahun) && Common.isNumber(value(cariTahun))) {
			setValue(cari, "");
			criterionCari = Restrictions.and(criterionCari, Restrictions.eq(prefix + "tahun", Integer.valueOf(value(cariTahun))));
		}
		if (hasText(cariHalaman) && Common.isNumber(value(cariHalaman))) {
			setValue(cari, "");
			criterionCari = Restrictions.and(criterionCari, Restrictions.eq(prefix + "halaman", Integer.valueOf(value(cariHalaman))));
		}

		String keyword = cari == null ? "" : value(cari);
		if (keyword.length() == 0) {
			return criterionCari;
		}
		Criterion criterion = Restrictions.ilike(prefix + "isbn", keyword, MatchMode.ANYWHERE);
		criterion = Restrictions.or(criterion, Restrictions.ilike(prefix + "isbn10", keyword, MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike(prefix + "issn", keyword, MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike(prefix + "nama", keyword, MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike(prefix + "tema", keyword, MatchMode.ANYWHERE));
		criterion = Restrictions.or(criterion, Restrictions.ilike(prefix + "pengarangs", keyword, MatchMode.ANYWHERE));
		return criterion;
	}

	private boolean hasText(Textbox textbox) {
		return textbox != null && textbox.getValue() != null && textbox.getValue().trim().length() > 0;
	}

	private String value(Textbox textbox) {
		return textbox == null || textbox.getValue() == null ? "" : textbox.getValue().trim();
	}

	private void applyPerpustakaanFilter(Criteria c, String property, Perpustakaan perpustakaan) {
		if (c != null && property != null && perpustakaan != null) {
			c.add(Restrictions.eq(property, perpustakaan));
		}
	}

	private void applyDateFilter(Criteria c, String property, Date mulai, Date sampai) {
		if (c == null || property == null) {
			return;
		}
		if (mulai != null) {
			c.add(Restrictions.ge(property, mulai));
		}
		if (sampai != null) {
			c.add(Restrictions.le(property, sampai));
		}
	}

	private void applyEbookRestriction(Criteria c, String prefix) {
		Criterion ebook = Restrictions.eq(prefix + "bolehDiDownload", true);
		ebook = Restrictions.or(ebook, Restrictions.eq(prefix + "hasScan", true));
		ebook = Restrictions.or(ebook, Restrictions.isNotNull(prefix + "lampiranPath"));
		ebook = Restrictions.or(ebook, Restrictions.isNotNull(prefix + "scanLinks"));
		ebook = Restrictions.or(ebook, Restrictions.isNotNull(prefix + "link"));
		c.add(ebook);
	}

	private String trendKey(Date date) {
		if (date == null) {
			return "Tanpa Tanggal";
		}
		try {
			return new java.text.SimpleDateFormat("MM/yyyy").format(date);
		} catch (Exception e) {
			return "Tanpa Tanggal";
		}
	}

	private List<NameValue> toTrendList(Map<String, Integer> map) {
		List<NameValue> result = new ArrayList<NameValue>();
		if (map == null) {
			return result;
		}
		for (String key : map.keySet()) {
			Integer value = map.get(key);
			result.add(new NameValue(key, value == null ? 0 : value.intValue()));
		}
		Collections.sort(result, new Comparator<NameValue>() {
			@Override
			public int compare(NameValue o1, NameValue o2) {
				return o1.name.compareToIgnoreCase(o2.name);
			}
		});
		return result;
	}

	private void sortNameValueDesc(List<NameValue> list) {
		Collections.sort(list, new Comparator<NameValue>() {
			@Override
			public int compare(NameValue o1, NameValue o2) {
				if (o1.value == o2.value) {
					return o1.name.compareToIgnoreCase(o2.name);
				}
				return o2.value - o1.value;
			}
		});
	}

	private List<NameValue> limitNameValue(List<NameValue> list, int limit) {
		List<NameValue> result = new ArrayList<NameValue>();
		if (list == null) {
			return result;
		}
		for (int i = 0; i < list.size() && i < limit; i++) {
			result.add(list.get(i));
		}
		return result;
	}

	private String getCoverUrl(Item item) {
		try {
			if (item != null && item.getImageUrl() != null && item.getImageUrl().trim().length() > 0
					&& item.getImageUrl().trim().startsWith("http")) {
				return item.getImageUrl().trim();
			}
			if (item != null && item.getId() != null) {
				return Common.getRequestHostWithProtocol() + "/AmbilMedia?id=" + item.getId()
						+ "&name=nama&foto=foto&clazz=ais.database.model.file.FotoGambarItem&property=item&height=152&width=114";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/utama/DashboardPustaka.java:2467");
		}
		return Common.getRequestHostWithProtocol() + "/img/none-icon.png";
	}

	private boolean booleanTrue(Boolean value) {
		return value == null || value.booleanValue();
	}

	private String safeString(String value) {
		return value == null ? "" : value;
	}

	private String formatNumber(int value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatMoney(double value) {
		try {
			return Common.numberFormat.get().format(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatPercentNumber(double value) {
		try {
			if (value < 0.0) {
				value = 0.0;
			}
			if (value > 100.0) {
				value = 100.0;
			}
			return Common.numberFormat.get().format(value).replace(',', '.');
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String formatDate(Date date) {
		if (date == null) {
			return "-";
		}
		try {
			return Common.dateFormat3.get().format(date);
		} catch (Exception e) {
			return "-";
		}
	}

	private int hitungSelisihHari(Date mulai, Date sampai) {
		if (mulai == null || sampai == null) {
			return 0;
		}
		long diff = sampai.getTime() - mulai.getTime();
		if (diff <= 0L) {
			return 0;
		}
		return (int) (diff / (24L * 60L * 60L * 1000L));
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private void closeSessionSafely(Session session) {
		ais.ui.util.DashboardModernHtmlUtil.closeOpenedSession(session);
	}

	private static class LibraryDashboardData {
		Perpustakaan filterPerpustakaan;
		Anggota filterAnggota;
		Date filterMulai;
		Date filterSampai;
		int totalKoleksi;
		int totalEksemplar;
		int totalDipinjam;
		int totalPeminjamanPeriode;
		int totalTerlambat;
		int totalDikembalikan;
		int totalKunjungan;
		int totalPemesanan;
		int totalEbook;
		int totalDiblokir;
		int totalPenerbit;
		int totalDdc;
		double totalDenda;
		List<NameValue> koleksiPerTipe = new ArrayList<NameValue>();
		List<NameValue> eksemplarPerPustaka = new ArrayList<NameValue>();
		List<NameValue> trenPeminjaman = new ArrayList<NameValue>();
		List<NameValue> trenPengembalian = new ArrayList<NameValue>();
		List<NameValue> trenKunjungan = new ArrayList<NameValue>();
		List<ItemInfo> koleksiPopuler = new ArrayList<ItemInfo>();
		List<ItemInfo> koleksiTerbaru = new ArrayList<ItemInfo>();
		List<ItemInfo> koleksiEbook = new ArrayList<ItemInfo>();
		List<OverdueInfo> keterlambatan = new ArrayList<OverdueInfo>();
	}

	private static class NameValue {
		String name;
		int value;

		NameValue(String name, int value) {
			this.name = name == null ? "" : name;
			this.value = value;
		}
	}

	private static class ItemInfo {
		Long id;
		String nama;
		String subInfo;
		String coverUrl;
		int dilihat;
		int diunduh;
		boolean ebook;
		String readUrl;
		String downloadUrl;
	}

	private static class OverdueInfo {
		String namaItem;
		String peminjam;
		Date batasWaktu;
		int hariTerlambat;
	}
}
