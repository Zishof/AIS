package ais.action.master;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.DashboardLogAktifitasPengguna;
import ais.action.master.dashboard.admin.DashboardRekapKunjunganPengguna;
import ais.action.master.dashboard.admin.DashboardRekapMenuPengguna;
import ais.action.master.dashboard.admin.DashboardStatistikKunjunganPengguna;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.CommonSearchFilterHelper;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlacklistIp;
import ais.database.model.Dosen;
import ais.database.model.LogLogin;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.ui.render.TampilDetailLog;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk log login. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html dashboardHtml},
 * {@code org.zkoss.zul.Html progressHtml}, {@code Component rootComponent}, {@code MyGrid grid}, {@code Paging
 * paging}, {@code Textbox searchnim}, {@code Textbox searchnama}, {@code Textbox searchip};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code tampilDpsaceLog()}, {@code onCatatanUpload()}, {@code onBlacklist()}, {@code
 * onSearchDefault()}, {@code refreshDashboardAman()}, {@code findDescendantById()}); validasi/perhitungan
 * ({@code isChecked()}); mutasi data ({@code onProsesEksporXlsxKunjungan()}); pelaporan/ekspor ({@code
 * onCetakLaporanKunjungan()}); operasi domain lain ({@code onRevisi()}, {@code onLogMenuRinci()}, {@code
 * onMobile()}, {@code onLogNotifikasi()}, {@code onKesalahanSistem()}, {@code onPerformaSistem()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class LogLoginAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private org.zkoss.zul.Html dashboardHtml;
	private org.zkoss.zul.Html progressHtml;
	private Component rootComponent;

	private MyGrid grid;

	private Paging paging;
	private Textbox searchnim;
	private Textbox searchnama;
	private Textbox searchip;
	private Textbox searchLinkProfile;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private MyToolbarbuttonConfig find;
	private MyDatebox start;
	private MyDatebox end;

	private Tabpanel dashboardStatistikKunjunganPengguna;
	private Tabpanel dashboardLogAktifitasPengguna;
	private Tabpanel dashboardLogMenuPengguna;
	private Tabpanel dashboardRekapKunjunganPengguna;
	private Tabpanel dashboardKunjunganPenggunaAndroid;

	private Tabpanel dspace;
	private Tabpanel kesalahanSistem;
	private Tabpanel uploadLog;
	private Tabpanel pemakaianMemory;
	private Tabpanel pemakaianFlagCache;
	private Tabpanel blacklist;
	private Tabpanel dashboardLogNotifikasi;


	private Tabpanel dashboardRevisi;

	public void onRevisi(Event event) {
		if (dashboardRevisi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(dashboardRevisi);
			MyInclude iframe = new MyInclude("/pages/master/revisi.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel dashboardLogMenuRinci;

	public void onLogMenuRinci(Event event) {
		if (dashboardLogMenuRinci.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(dashboardLogMenuRinci);
			MyInclude iframe = new MyInclude("/pages/master/detail_log_login.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel mobile;

	public void onMobile(Event event) {
		if (mobile.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(mobile);
			MyInclude iframe = new MyInclude("/pages/master/log_mobile.zul");
			iframe.setParent(window);
		}
	}

	public static void tampilDpsaceLog() {
		try {

			final MyWindow window = new MyWindow("", "none", true);
			window.setHeight("98%");
			window.setWidth("80%");
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			MyInclude iframe = new MyInclude("/pages/master/dspace_log.zul");
			iframe.setParent(center);

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			window.onModal();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void onLogNotifikasi(Event event) {
		if (dashboardLogNotifikasi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(dashboardLogNotifikasi);
			MyInclude iframe = new MyInclude("/pages/master/notifikasi.zul");
			iframe.setParent(window);
		}
	}

	public void onKesalahanSistem(Event event) {
		if (kesalahanSistem.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kesalahanSistem);
			MyInclude iframe = new MyInclude("/pages/master/error_log.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel performaSistem;

	public void onPerformaSistem(Event event) {
		if (performaSistem.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(performaSistem);
			MyInclude iframe = new MyInclude("/pages/master/performa_log.zul");
			iframe.setParent(window);
		}
	}

	public void onCatatanUpload(Event event) {
		if (uploadLog.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(uploadLog);
			MyInclude iframe = new MyInclude("/pages/master/upload_log.zul");
			iframe.setParent(window);
		}
	}

	public void onPemakaianMemori(Event event) {
		if (pemakaianMemory.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(pemakaianMemory);
			MyInclude iframe = new MyInclude("/pages/master/memory_info.zul");
			iframe.setParent(window);
		}
	}

	public void onPemakaianFlagCache(Event event) {
		if (pemakaianFlagCache.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(pemakaianFlagCache);
			MyInclude iframe = new MyInclude("/pages/master/flag_cache_info.zul");
			iframe.setParent(window);
		}
	}

	public void onBlacklist(Event event) {
		if (blacklist.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(blacklist);
			MyInclude iframe = new MyInclude("/pages/master/blacklistip.zul");
			iframe.setParent(window);
		}
	}

	public void onDspace(Event event) {
		if (dspace.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(dspace);
			MyInclude iframe = new MyInclude("/pages/master/dspace_log.zul");
			iframe.setParent(window);
		}
	}

	public void onKunjunganPenggunaAndroid(Event event) {
		if (dashboardKunjunganPenggunaAndroid.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(dashboardKunjunganPenggunaAndroid);
			MyInclude iframe = new MyInclude("/pages/master/log_login_android.zul?loginFrom=Login+from+Android");
			iframe.setParent(window);
		}
	}

	public void onStatistikKunjunganPengguna(Event event) throws Exception {

		if (dashboardStatistikKunjunganPengguna.getChildren().size() == 0) {
			DashboardStatistikKunjunganPengguna laporan = new DashboardStatistikKunjunganPengguna();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardStatistikKunjunganPengguna,
				"Statistik Kunjungan", "Tren jumlah login dan kunjungan pengguna per hari dan per peran.");
		}
	}

	public void onLogAktifitasPengguna(Event event) throws Exception {

		if (dashboardLogAktifitasPengguna.getChildren().size() == 0) {
			DashboardLogAktifitasPengguna laporan = new DashboardLogAktifitasPengguna();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardLogAktifitasPengguna,
				"Log Aktivitas", "Rekap aktivitas pengguna berdasarkan menu yang dikunjungi dan waktu akses.");
		}
	}

	public void onLogMenuPengguna(Event event) throws Exception {

		if (dashboardLogMenuPengguna.getChildren().size() == 0) {
			DashboardRekapMenuPengguna laporan = new DashboardRekapMenuPengguna();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardLogMenuPengguna,
				"Rekap Menu", "Menu-menu yang paling sering dibuka oleh pengguna sistem.");
		}
	}

	public void onRekapKunjunganPengguna(Event event) throws Exception {

		if (dashboardRekapKunjunganPengguna.getChildren().size() == 0) {
			DashboardRekapKunjunganPengguna laporan = new DashboardRekapKunjunganPengguna();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, dashboardRekapKunjunganPengguna,
				"Rekap Kunjungan", "Rekapitulasi total kunjungan pengguna per periode dan per jenis akun.");
		}
	}

	private String loginFrom = "";
	private MyCheckboxConfig mahasiswa;
	private MyCheckboxConfig dosen;
	private MyCheckboxConfig admin;
	private MyCheckboxConfig siswa;
	private MyCheckboxConfig guru;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		rootComponent = comp;
		org.zkoss.zul.Div mainContainer = (org.zkoss.zul.Div) comp.getFellow("mainContainer");

		int[] tabAktif = {0};
		ais.ui.util.MyButtonTabbox btabs = ais.ui.util.MyButtonTabbox.buat(mainContainer, "100%", tabAktif);

		// Tab 0: Dasbor — inline content
		{
			org.zkoss.zul.Div panel = btabs.tambahTab(0, "Dasbor");
			ais.ui.util.MyButtonTabbox.muatZulEager(panel, "/WEB-INF/z/x/y/pages/master/log_login_tab_0_dasbor.zul");
		}

		// Tab 1: Login — inline content
		{
			org.zkoss.zul.Div panel = btabs.tambahTab(1, "Login");
			ais.ui.util.MyButtonTabbox.muatZulEager(panel, "/WEB-INF/z/x/y/pages/master/log_login_tab_1_login.zul");
		}

		super.doAfterCompose(comp);
		bindIncludedComponents();

		// Lazy tabs added after super so eager-tab fields are already wired
		btabs.tambahTabLazy(2, "Mobile", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(panel);
				MyInclude iframe = new MyInclude("/pages/master/log_mobile.zul");
				iframe.setParent(window);
			}
		});

		btabs.tambahTabLazy(3, "Online", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				ais.ui.util.MyButtonTabbox.muatZul(panel, "/WEB-INF/z/x/y/pages/master/online_users.zul");
			}
		});

		btabs.tambahTabLazy(4, "Statistik", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				DashboardStatistikKunjunganPengguna laporan = new DashboardStatistikKunjunganPengguna();
				ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, panel,
					"Statistik Kunjungan", "Tren jumlah login dan kunjungan pengguna per hari dan per peran.");
			}
		});

		btabs.tambahTabLazy(5, "Notifikasi", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(panel);
				MyInclude iframe = new MyInclude("/pages/master/notifikasi.zul");
				iframe.setParent(window);
			}
		});

		btabs.tambahTabLazy(6, "Revisi", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(panel);
				MyInclude iframe = new MyInclude("/pages/master/revisi.zul");
				iframe.setParent(window);
			}
		});

		btabs.tambahTabLazy(7, "Aktifitas", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				DashboardLogAktifitasPengguna laporan = new DashboardLogAktifitasPengguna();
				ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, panel,
					"Log Aktivitas", "Rekap aktivitas pengguna berdasarkan menu yang dikunjungi dan waktu akses.");
			}
		});

		btabs.tambahTabLazy(8, "Akses Menu", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				DashboardRekapMenuPengguna laporan = new DashboardRekapMenuPengguna();
				ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, panel,
					"Rekap Menu", "Menu-menu yang paling sering dibuka oleh pengguna sistem.");
			}
		});

		btabs.tambahTabLazy(9, "Akses Menu Rinci", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(panel);
				MyInclude iframe = new MyInclude("/pages/master/detail_log_login.zul");
				iframe.setParent(window);
			}
		});

		btabs.tambahTabLazy(10, "Rekap Login", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				DashboardRekapKunjunganPengguna laporan = new DashboardRekapKunjunganPengguna();
				ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, panel,
					"Rekap Kunjungan", "Rekapitulasi total kunjungan pengguna per periode dan per jenis akun.");
			}
		});

		btabs.tambahTabLazy(11, "Blacklist", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(panel);
				MyInclude iframe = new MyInclude("/pages/master/blacklistip.zul");
				iframe.setParent(window);
			}
		});

		btabs.tambahTabLazy(12, "Repository", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(panel);
				MyInclude iframe = new MyInclude("/pages/master/dspace_log.zul");
				iframe.setParent(window);
			}
		});

		btabs.tambahTabLazy(13, "Kesalahan Sistem", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(panel);
				MyInclude iframe = new MyInclude("/pages/master/error_log.zul");
				iframe.setParent(window);
			}
		});

		btabs.tambahTabLazy(14, "Performa Sistem", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(panel);
				MyInclude iframe = new MyInclude("/pages/master/performa_log.zul");
				iframe.setParent(window);
			}
		});

		btabs.tambahTabLazy(15, "Pemakaian Memori", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(panel);
				MyInclude iframe = new MyInclude("/pages/master/memory_info.zul");
				iframe.setParent(window);
			}
		});

		btabs.tambahTabLazy(16, "Pemakaian Flag/Cache", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(panel);
				MyInclude iframe = new MyInclude("/pages/master/flag_cache_info.zul");
				iframe.setParent(window);
			}
		});

		btabs.tambahTabLazy(17, "Catatan Upload", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%");
				window.setWidth("100%");
				window.setParent(panel);
				MyInclude iframe = new MyInclude("/pages/master/upload_log.zul");
				iframe.setParent(window);
			}
		});

		btabs.pulihkanSeleksi(18);

		// --- existing post-wire logic ---
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		if (execution.getParameter("loginFrom") != null) {
			loginFrom = execution.getParameter("loginFrom").trim();
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "nama", "ip", "keterangan", "login", "logout",
				"dosen", "pegawai", "mahasiswa", "tbmuser", "jurusan", "fakultas", "siswa", "guru", "sekolah",
				"yayasan", "hostname", "success_status", "description", "linkProfile", "header");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		Common.appendKeToolbar(mahasiswa = new MyCheckboxConfig("Mahasiswa"), find, comp);
		Common.appendKeToolbar(dosen = new MyCheckboxConfig("Dosen"), find, comp);

		Common.appendKeToolbar(siswa = new MyCheckboxConfig("Siswa"), find, comp);
		Common.appendKeToolbar(guru = new MyCheckboxConfig("Guru"), find, comp);

		Common.appendKeToolbar(admin = new MyCheckboxConfig("Admin"), find, comp);

		if (mahasiswa != null) { mahasiswa.setChecked(true); }
		if (dosen != null) { dosen.setChecked(true); }
		if (siswa != null) { siswa.setChecked(true); }
		if (guru != null) { guru.setChecked(true); }
		if (admin != null) { admin.setChecked(true); }

		mahasiswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});
		dosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		siswa.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});
		guru.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		admin.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});
	}

	class LogLoginRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		private Map<Long, BlacklistIp> blacklistIps = ConstantValues.ambilBerdasarClass(BlacklistIp.class);

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LogLogin logLogin = (LogLogin) arg1;
			if ((logLogin.getSuccess_status() != null && !logLogin.getSuccess_status())) {
				arg0.setStyle(null);
			}

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new TampilDetailLog(detail, logLogin));

			final Label time = new Label("");

			Long diff = null;
			Long diffDetik = 0L;
			Long diffMenit = 0L;
			Long diffJam = 0L;
			if (logLogin.getSuccess_status() != null && !logLogin.getSuccess_status()) {
				time.setValue("");
			} else {
				diff = (logLogin.getLogout() == null ? ais.ui.util.WaktuUtil.getDate() : logLogin.getLogout()).getTime()
						- logLogin.getLogin().getTime();
				diffDetik = (diff / (1000/* * 60 * 60 * 24 */)) % 60;
				diffMenit = (diff / (1000 * 60 /** 60 * 24 */
				)) % 60;
				diffJam = (diff / (1000 * 60 * 60 /** 24 */
				));
				time.setValue(diff == null ? ""
						: Common.numberFormat.get().format(diffJam) + " jam " + Common.numberFormat.get().format(diffMenit)
								+ " menit " + Common.numberFormat.get().format(diffDetik) + " detik");
			}

			RevisiHelper
					.createNewRevisi(LogLogin.class, logLogin,
							logLogin.getLogin() == null ? "" : Common.dateFormat3.get().format(logLogin.getLogin()))
					.setParent(arg0);

			new Label(logLogin.getLogout() == null ? "Belum/tidak logout"
					: Common.dateFormat3.get().format(logLogin.getLogout())).setParent(arg0);
			time.setParent(arg0);

			Vbox myVbox = new Vbox();
			myVbox.setParent(arg0);

			A myIp = new A(logLogin.getIp());
			myIp.setTarget("new");
			myIp.setHref("http://whatismyipaddress.com/ip/" + logLogin.getIp());
			myIp.setParent(myVbox);

			final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Blacklist IP ini");
			checkboxConfig.setParent(myVbox);
			for (BlacklistIp blacklistIp : blacklistIps.values()) {
				if (blacklistIp.getAktif() && blacklistIp.getKode().equalsIgnoreCase(logLogin.getIp())) {
					checkboxConfig.setChecked(true);
					break;
				}
			}
			checkboxConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					BlacklistIp blacklistIpcari = null;
					for (BlacklistIp blacklistIp : blacklistIps.values()) {
						if (blacklistIp.getKode().equalsIgnoreCase(logLogin.getIp())) {
							blacklistIpcari = blacklistIp;
							break;
						}
					}

					Session session = HibernateUtil.currentSession();
					if (blacklistIpcari == null) {
						blacklistIpcari = new BlacklistIp();
						blacklistIpcari.setKode(logLogin.getIp());
						blacklistIpcari.setNama("Blacklist IP " + logLogin.getIp());
					}

					blacklistIpcari.setAktif(checkboxConfig.isChecked());
					Common.refreshSaveOrUpdate(session, blacklistIpcari);
					session.flush();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
		refreshDashboardAman();
		}
					});
				}
			});

			Tbmuser tbmuser = logLogin.getTbmuser();
			Dosen dosen = logLogin.getDosen();
			Mahasiswa mahasiswa = logLogin.getMahasiswa();
			Siswa siswa = logLogin.getSiswa();
			Guru guru = logLogin.getGuru();

			String nama = siswa != null ? siswa.getNama() + " (" + siswa.getNomorIndukNasional() + ")"
					: mahasiswa != null ? mahasiswa.getNama() + " (" + mahasiswa.getNim() + ")"
							: dosen != null ? dosen.getNama() : tbmuser == null ? "" : tbmuser.getUserNama();

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			if (dosen != null) {
				CommonMedia.tampilkanGambarKecil(dosen).setParent(vbox);
			} else if (mahasiswa != null) {
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(vbox);
			} else if (guru != null) {
				CommonMedia.tampilkanGambarKecil(guru).setParent(vbox);
			} else if (siswa != null) {
				CommonMedia.tampilkanGambarKecil(siswa).setParent(vbox);
			} else {
				CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox);
			}
			vbox.appendChild(new Label(nama));

			new Label(tbmuser == null || tbmuser.hakAkses() == null
					? (guru != null ? "Guru"
							: siswa != null ? "Siswa"
									: mahasiswa != null ? Common.getBahasa("label_mahasiswa")
											: dosen != null ? Common.getBahasa("label_dosen") : "")
					: tbmuser.hakAkses().getRoleName()).setParent(arg0);
			if (siswa != null) {
				new Label(siswa.getYayasan() == null ? "" : logLogin.getYayasan().getNama()).setParent(arg0);
				new Label(logLogin.getSekolah() == null ? "" : logLogin.getSekolah().getNama()).setParent(arg0);
			} else {

				new Label(logLogin.getFakultas() == null ? "" : logLogin.getFakultas().getNama()).setParent(arg0);
				new Label(logLogin.getJurusan() == null ? "" : logLogin.getJurusan().getNama()).setParent(arg0);
			}

			new Label(logLogin.getSuccess_status() == null || logLogin.getSuccess_status() ? "Sukses" : "Gagal")
					.setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(logLogin.getDescription() == null ? "" : logLogin.getDescription()).setParent(vbox);
			if (logLogin.getLinkProfile() != null && !logLogin.getLinkProfile().trim().isEmpty()) {
				new Label(logLogin.getLinkProfile()).setParent(vbox);
			}

			new MyLabelKecil(logLogin.getHeader() == null ? "" : logLogin.getHeader()).setParent(vbox);
		}
	}

	private boolean isChecked(MyCheckboxConfig checkbox) {
		try {
			return checkbox != null && checkbox.isChecked();
		} catch (Exception e) {
			return false;
		}
	}

	private String value(Textbox textbox) {
		try {
			return textbox == null || textbox.getValue() == null ? "" : textbox.getValue().trim();
		} catch (Exception e) {
			return "";
		}
	}

	public Criteria initCriteria(boolean order) {

		Criterion criterion = Restrictions.sqlRestriction("false");

		boolean pilihMahasiswa = isChecked(mahasiswa);
		boolean pilihDosen = isChecked(dosen);
		boolean pilihSiswa = isChecked(siswa);
		boolean pilihGuru = isChecked(guru);
		boolean pilihAdmin = isChecked(admin);

		if (pilihMahasiswa && pilihDosen && pilihGuru && pilihAdmin) {
			criterion = Restrictions.sqlRestriction("true");
		} else {

			if (pilihMahasiswa) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("mahasiswa"));
			}
			if (pilihDosen) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("dosen"));
			}

			if (pilihSiswa) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("siswa"));
			}
			if (pilihGuru) {
				criterion = Restrictions.or(criterion, Restrictions.isNotNull("guru"));
			}

			if (pilihAdmin) {
				criterion = Restrictions.or(criterion,
						Restrictions.and(
								Restrictions.and(Restrictions.isNotNull("tbmuser"), Restrictions.isNull("dosen")),
								Restrictions.isNull("guru"))

				);
			}
		}

		Session session = HibernateUtil.currentSession();
		session.createSQLQuery("set statement_timeout to 6000000; commit;").executeUpdate();
		Criteria criteria = session.createCriteria(LogLogin.class)
				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
						"date(this_.login) between date('" + Common.databaseDateFormat.get().format(start.getValue())
								+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')")))
				.add(criterion);
		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add(loginFrom == null || loginFrom.trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("description", loginFrom, MatchMode.START))

				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(value(searchnim).equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("description", value(searchnim), MatchMode.ANYWHERE),
								Restrictions.ilike("header", value(searchnim), MatchMode.ANYWHERE)))

				.add(value(searchip).equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("ip", value(searchip), MatchMode.ANYWHERE))

				.add(value(searchLinkProfile).equals("")
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("linkProfile", value(searchLinkProfile), MatchMode.ANYWHERE))

				.add(value(searchnama).isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", value(searchnama), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		bindIncludedComponents();
		Common.initPaging(initCriteria(false), paging);

		List<LogLogin> logLogin = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(logLogin);
		if (grid == null) {
			return;
		}
		grid.setRowRenderer(new LogLoginRenderer());
		grid.setModelCheckMobile(strset);

	}


	private void refreshDashboardAman() {
		try {
			bindIncludedComponents();
			// paging.getTotalSize() sudah dihitung onSearchDefault (selalu dipanggil tepat sebelum
			// method ini) -> hindari SELECT count(*) yang sama dijalankan dua kali.
			ais.action.master.helper.GenericActionDashboardHelper.refreshFromCriteria(dashboardHtml, progressHtml, this,
					"Dasbor Login Pengguna", "Lihat ringkasan akses pengguna, aktivitas login, perangkat, dan potensi kegagalan login dalam satu tampilan ringkas.",
					paging == null ? -1L : paging.getTotalSize());
		} catch (Exception e) {
			try {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/LogLoginAction.java:681");
			}
		}
	}

	private void bindIncludedComponents() {
		if (rootComponent == null) {
			return;
		}
		if (dashboardHtml == null) {
			dashboardHtml = (org.zkoss.zul.Html) findDescendantById(rootComponent, "dashboardHtml");
		}
		if (progressHtml == null) {
			progressHtml = (org.zkoss.zul.Html) findDescendantById(rootComponent, "progressHtml");
		}
		if (grid == null) {
			grid = (MyGrid) findDescendantById(rootComponent, "grid");
		}
		if (paging == null) {
			paging = (Paging) findDescendantById(rootComponent, "paging");
		}
		if (searchnim == null) {
			searchnim = (Textbox) findDescendantById(rootComponent, "searchnim");
		}
		if (searchnama == null) {
			searchnama = (Textbox) findDescendantById(rootComponent, "searchnama");
		}
		if (searchip == null) {
			searchip = (Textbox) findDescendantById(rootComponent, "searchip");
		}
		if (searchLinkProfile == null) {
			searchLinkProfile = (Textbox) findDescendantById(rootComponent, "searchLinkProfile");
		}
		if (searchfakultas == null) {
			searchfakultas = (Combobox) findDescendantById(rootComponent, "searchfakultas");
		}
		if (searchjurusan == null) {
			searchjurusan = (Combobox) findDescendantById(rootComponent, "searchjurusan");
		}
		if (searchyayasan == null) {
			searchyayasan = (Combobox) findDescendantById(rootComponent, "searchyayasan");
		}
		if (searchsekolah == null) {
			searchsekolah = (Combobox) findDescendantById(rootComponent, "searchsekolah");
		}
		if (find == null) {
			find = (MyToolbarbuttonConfig) findDescendantById(rootComponent, "find");
		}
		if (start == null) {
			start = (MyDatebox) findDescendantById(rootComponent, "start");
		}
		if (end == null) {
			end = (MyDatebox) findDescendantById(rootComponent, "end");
		}
	}

	private Component findDescendantById(Component parent, String id) {
		if (parent == null || id == null) {
			return null;
		}
		if (id.equals(parent.getId())) {
			return parent;
		}
		List children = parent.getChildren();
		if (children == null) {
			return null;
		}
		for (Object childObj : children) {
			if (childObj instanceof Component) {
				Component found = findDescendantById((Component) childObj, id);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/**
	 * Tombol "Cetak Laporan Lengkap" di Dasbor: membangun laporan &amp; analisis kunjungan pengguna
	 * (statistik, tren, per pengguna, per jenis pengguna, per satuan kerja, akses mobile) lalu
	 * menampilkannya pada popup siap-cetak. Rentang tanggal &amp; filter unit mengikuti filter aktif.
	 */
	public void onCetakLaporanKunjungan(Event event) throws Exception {
		java.util.Date mulai = (start != null && start.getValue() != null) ? start.getValue() : null;
		java.util.Date sampai = (end != null && end.getValue() != null) ? end.getValue() : null;
		ais.action.master.helper.LaporanKunjunganPenggunaHelper.tampilkanLaporan(
				self, mulai, sampai, idDari(searchfakultas), idDari(searchjurusan),
				idDari(searchyayasan), idDari(searchsekolah));
	}

	/**
	 * Tombol "Ekspor Excel": mengunduh laporan kunjungan pengguna sebagai .xlsx multi-sheet
	 * (Ringkasan, Per Jenis, Per Satuan Kerja, Per Fakultas, Mobile vs Web, Tren Harian, dan
	 * daftar LENGKAP Per Pengguna). Rentang &amp; filter mengikuti filter aktif.
	 */
	public void onEksporExcelKunjungan(Event event) throws Exception {
		// tampilkan indikator "sedang memproses" lalu susun & unduh berkas pada langkah echo berikutnya
		try { org.zkoss.zk.ui.util.Clients.showBusy("Menyiapkan berkas Excel, mohon tunggu..."); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/LogLoginAction.java:706");}
		org.zkoss.zk.ui.event.Events.echoEvent("onProsesEksporXlsxKunjungan", self, null);
	}

	public void onProsesEksporXlsxKunjungan(Event event) throws Exception {
		try {
			java.util.Date mulai = (start != null && start.getValue() != null) ? start.getValue() : null;
			java.util.Date sampai = (end != null && end.getValue() != null) ? end.getValue() : null;
			ais.action.master.helper.LaporanKunjunganPenggunaHelper.eksporExcel(
					mulai, sampai, idDari(searchfakultas), idDari(searchjurusan),
					idDari(searchyayasan), idDari(searchsekolah));
		} finally {
			try { org.zkoss.zk.ui.util.Clients.clearBusy(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/LogLoginAction.java:718");}
		}
	}

	/** Ambil id (Long) dari item terpilih combobox filter (Fakultas/Jurusan/Yayasan/Sekolah); null = semua. */
	private Long idDari(org.zkoss.zul.Combobox cb) {
		try {
			if (cb == null || cb.getSelectedItem() == null || cb.getSelectedItem().getValue() == null) {
				return null;
			}
			Object v = cb.getSelectedItem().getValue();
			if (v instanceof ais.database.model.Fakultas) return ((ais.database.model.Fakultas) v).getId();
			if (v instanceof ais.database.model.Jurusan) return ((ais.database.model.Jurusan) v).getId();
			if (v instanceof ais.database.model.sekolah.Yayasan) return ((ais.database.model.sekolah.Yayasan) v).getId();
			if (v instanceof ais.database.model.sekolah.Sekolah) return ((ais.database.model.sekolah.Sekolah) v).getId();
			return null;
		} catch (Exception e) {
			return null;
		}
	}

}
