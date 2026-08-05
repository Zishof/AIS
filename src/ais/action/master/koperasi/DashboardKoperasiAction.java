package ais.action.master.koperasi;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.common.Common;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyInclude;

/**
 * <h2>DashboardKoperasiAction — Dasbor Utama "Koperasi" (Pengumpul Semua Dasbor Koperasi)</h2>
 *
 * <p>
 * Composer ZK ini menjadi <b>satu pintu masuk</b> untuk seluruh dasbor koperasi yang tersebar di
 * sistem. Alih-alih membangun ulang grafik atau menyalin logika, halaman ini <b>memakai ulang
 * (reuse) dasbor-dasbor yang sudah ada</b> dengan menampilkannya sebagai tab: Simpan Pinjam,
 * Laporan Simpan Pinjam, Pembagian SHU, dan Kantin/Toko. Dengan begitu, setiap perbaikan pada
 * dasbor aslinya otomatis ikut terlihat di sini, dan pemeliharaan cukup dilakukan di satu tempat.
 * </p>
 *
 * <p>
 * Dasbor utama ini ditampilkan di halaman utama hanya untuk role yang memiliki hak
 * {@code Tbmrole.getDashboardKoperasi()} (default: Administrator). Tautannya dipasang sebagai
 * pintasan header pada {@code MainAction} dan dibuka lewat {@code MainDashboardEventHelper.onKoperasi}.
 * </p>
 *
 * <h3>Reuse via tab yang dimuat saat dibutuhkan (lazy)</h3>
 * <p>
 * Tiap tab memuat ZUL dasbor aslinya melalui {@link MyInclude}. Untuk menghemat sumber daya, hanya
 * tab pertama yang dimuat langsung; tab lain baru dimuat ketika pertama kali dipilih pengguna. Pola
 * ini menghindari memuat empat dasbor berat sekaligus dan membuat halaman terasa ringan di HP maupun
 * desktop.
 * </p>
 *
 * <h3>Menambah/mengurangi dasbor</h3>
 * <p>
 * Untuk menambah dasbor koperasi lain ke halaman ini, cukup tambahkan pasangan judul + URL ZUL pada
 * {@link #JUDUL} dan {@link #URL} — tidak perlu menyentuh kode grafik mana pun. Kompatibel Java 1.7.
 * </p>
 *
 * @see DashboardSimpanPinjamAction
 * @see LaporanSimpanPinjamAction
 * @see PembagianShuAction
 */
public class DashboardKoperasiAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	/** Judul tiap tab dasbor koperasi (sejajar dengan {@link #URL}). */
	private static final String[] JUDUL = { "Simpan Pinjam", "Laporan Simpan Pinjam", "Pembagian SHU",
			"Laporan Keuangan", "Anggaran Kas", "Modal Penyertaan", "ARO Deposito", "Kantin / Toko" };

	/** URL ZUL dasbor yang dipakai ulang untuk tiap tab (sejajar dengan {@link #JUDUL}). */
	private static final String[] URL = { "/pages/master/koperasi/dashboard_simpan_pinjam.zul",
			"/pages/master/koperasi/laporan_simpan_pinjam.zul", "/pages/master/koperasi/pembagian_shu.zul",
			"/pages/master/koperasi/laporan_keuangan_koperasi.zul", "/pages/master/koperasi/anggaran_kas_koperasi.zul",
			"/pages/master/koperasi/modal_penyertaan_koperasi.zul", "/pages/master/koperasi/deposito_aro_koperasi.zul",
			"/pages/master/koperasi/dashboard_kantin.zul" };

	// Auto-wired dari ZUL.
	private Div host;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		DashboardUiKit.attachIntro(comp, "Dasbor Koperasi",
				"Satu tempat untuk semua ringkasan koperasi: simpanan & pinjaman, laporan, pembagian SHU, "
						+ "hingga penjualan kantin. Pilih tab sesuai kebutuhan.");
		buildTabs();
	}

	/** Bangun tabbox berisi seluruh dasbor koperasi (tab pertama eager, sisanya lazy). */
	private void buildTabs() {
		if (host == null) {
			return;
		}
		host.getChildren().clear();

		Tabbox tabbox = new Tabbox();
		tabbox.setWidth("100%");
		tabbox.setHeight("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		Tabpanels panels = new Tabpanels();
		panels.setParent(tabbox);

		int n = Math.min(JUDUL.length, URL.length);
		for (int i = 0; i < n; i++) {
			Tab tab = new Tab(JUDUL[i]);
			tab.setParent(tabs);
			Tabpanel panel = new ais.ui.util.MyTabpanel();
			panel.setParent(panels);
			if (i == 0) {
				muat(panel, URL[i]);
			} else {
				lazy(tab, panel, URL[i]);
			}
		}
		tabbox.setParent(host);
	}

	/** Pasang pemuatan tertunda: dasbor baru dimuat saat tabnya pertama kali dipilih. */
	private void lazy(Tab tab, final Tabpanel panel, final String url) {
		tab.addEventListener("onSelect", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (panel.getChildren().isEmpty()) {
					muat(panel, url);
				}
			}
		});
	}

	/** Muat sebuah ZUL dasbor ke dalam panel via {@link MyInclude} (reuse penuh). */
	private void muat(Tabpanel panel, String url) {
		try {
			MyInclude include = new MyInclude(url);
			include.setWidth("100%");
			include.setHeight("100%");
			include.setParent(panel);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			panel.appendChild(DashboardUiKit.html(
					DashboardUiKit.descChip("Dasbor ini gagal dimuat. Silakan buka menunya secara langsung.")));
		}
	}
}
