package ais.action.maintenance;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.ClientInfoEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Menubar;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.catatan.DasbordCatatan;
import ais.action.master.kalender.DasbordInfoKegiatan;
import ais.action.master.kalender.DasbordKalenderAkademik;
import ais.action.master.PelanggaranMahasiswaAction;
import ais.action.master.TampilanPengumumanAkademisAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.ChangePasswordWindow;
import ais.action.master.helper.DaftarPenggunaOnline;
import ais.action.master.helper.MainHelper;
import ais.action.master.helper.MainMenuHelper;
import ais.action.master.helper.MainTreeMenuHelper;
import ais.action.master.helper.PertemuanHelper;
import ais.action.master.helper.UserOnlineCounter;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.PelanggaranSiswaAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.surat.AlurPersetujuanSuratKeluarStatusAction;
import ais.action.master.surat.AlurPersetujuanSuratMasukStatusAction;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.payroll.LaporanSlipGajiRealPegawaiPerOrang;
import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.PasswordChecker;
import ais.common.RequestContext;
import ais.common.SessionCounter;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CustomerService;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.KategoriPengumuman;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.Mahasiswa;
import ais.database.model.Notifikasi;
import ais.database.model.Pegawai;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.FotoSiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sop.DataSop;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.ui.util.ChatThread;
import ais.ui.util.HeapSizeDemo;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMenuitem;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyToolbarbuttonKecilConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.MyPortallayout;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.WaktuUtil;

public class MainAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 2446397351568124278L;
	private static final int MODERN_PENGUMUMAN_PAGE_SIZE = 5;
	private static final String DESKTOP_RELEASE_API =
			"https://api.github.com/repos/asroboy/ais_mobile/releases?per_page=20";
	private static final String DESKTOP_RELEASE_FALLBACK =
			"https://github.com/asroboy/ais_mobile/releases/latest";
	private static final long DESKTOP_RELEASE_CACHE_TTL_MS = 10L * 60L * 1000L;
	private static final Map<String, String> desktopReleaseUrlCache =
			Collections.synchronizedMap(new HashMap<String, String>());
	private static final Map<String, Long> desktopReleaseTimeCache =
			Collections.synchronizedMap(new HashMap<String, Long>());
	Tabbox iframe;
	private Borderlayout tinggiFrame;
	private Center centerTinggiFrame;
	private Div footer;

	private West navigation;
	West navigasi;
	private Center navigasicenter;
	private North mycenter;

	private Toolbarbutton usersOnline;
	private MyToolbarbuttonConfig customerService;
	MyToolbarbuttonConfig menuService;

	private LogLogin login;
	private Label loginInformation;
	private Konfigurasi konfigurasi;

	private MyToolbarbuttonConfig chat;
	private MyToolbarbuttonConfig message;

	private Image foto;
	private Tab tabChat;
	Tabs tabs;
	Tabpanels tabpanels;

	private Image idLogo;
	Tab tabDashboard, tabPustaka, tabWorkflow, tabRepository, tabAntarJemput, tabAdministrasi, tabPengadaan, tabSpmi;
	Tab tabPembayaran, tabAkunting, tabKinerja, tabKeuangan, tabKepegawaian, tabGaji;
	Tab tabKalenderAkademik, tabInfoKegiatan, tabLearning, tabPresensi, tabPengumuman, tabPrestasi;
	Tab tabDasbordCatatan;
	private Tabpanel panel_home;
	private Popup popupmenu;
	private Popup treeitemUtama = null;

	Tbmuser tbmuser = null;
	private int desktopHeight, desktopWidth;
	public static Map<String, Integer> desktopWidths = Collections.synchronizedMap(new HashMap<String, Integer>());
	public static Map<String, Integer> desktopHeights = Collections.synchronizedMap(new HashMap<String, Integer>());
	public static Map<String, ChatThread> mapChat = Collections.synchronizedMap(new HashMap<String, ChatThread>());

	private MyToolbarbuttonKecilConfig eLearningButton, eMenuButton, ePustakaButton, ePrestasiButton, ePresensiButton;
	private MyToolbarbuttonKecilConfig eAkademikButton, ePembayaranButton, eAkuntingButton, eKinerjaButton;
	private MyToolbarbuttonKecilConfig eWorkflowButton, eRepositoryButton, eAntarJemputButton, eAdministrasiButton, ePengadaanButton;
	private MyToolbarbuttonKecilConfig eSpmiButton;
	private MyToolbarbuttonKecilConfig eKantinButton;
	private MyToolbarbuttonKecilConfig ePosButton;
	private MyToolbarbuttonKecilConfig eKoperasiButton;
	private MyToolbarbuttonKecilConfig eMedicButton;
	private MyToolbarbuttonKecilConfig eGajiButton;
	private MyToolbarbuttonKecilConfig eKeuanganButton, eKepegawaianButton, eKalenderAkademikButton,
			eInfoKegiatanButton;
	private MyToolbarbuttonKecilConfig eFeederButton;
	private MyToolbarbuttonKecilConfig eSisterButton;
	private MyToolbarbuttonKecilConfig desktopDownloadButton;

	Tab tabSinkronisasiFeeder;
	Tab tabSinkronisasiSister;
	Tab tabKantin;
	Tab tabPos;
	Tab tabKoperasi;
	Tab tabEmedic;

	private Hbox headerHboxButton;
	private Row rowHeader, rowHeader2;
	private Grid gridHeader;
	private MyToolbarbuttonConfig menubutton;
	private Row headerHbox, rowPencarian, rowDicari, rowPengumuman;
	private boolean merupakanAdmin = false;
	boolean mobile = false;
	private boolean udah = false;
	private String menuBgColor;
	private PengumumanAkademis pengumumanAkademis;
	boolean pt = true, ya = true;
	private List<Tbmrole> tbmroles;

	void selectExistingTab(Tab tab) {
		if (tab == null || tab.isInvalidated()) {
			return;
		}
		try {
			tab.setSelected(true);
			Common.pilihMenu(navigasi, menuService);
		} catch (Exception e) {
			showError(e);
		}
	}

	Tabbox createStandardTabbox(Tabs[] outTabs, Tabpanels[] outPanels) {
		Tabbox tabbox = new Tabbox();
		String resolvedHeight = resolveAutoScrollablePanelHeight("100%");
		MainStyleHelper.applyStandardTabbox(tabbox, resolvedHeight);
		outTabs[0] = new Tabs();
		MainStyleHelper.appendSclassOnce(outTabs[0], "main-dashboard-tabs");
		outTabs[0].setParent(tabbox);
		outPanels[0] = new Tabpanels();
		MainStyleHelper.applyStandardTabpanels(outPanels[0], resolvedHeight);
		outPanels[0].setParent(tabbox);
		return tabbox;
	}

	Tabpanel createStandardTab(Tabs tabs, Tabpanels panels, String label, String heightPx) {
		// MyTab: drop-in Tab dgn guard "Exactly one selected tab is required: []" (race klik pada
		// tabbox yang tab-nya sudah kosong/terlepas). Perilaku label sama seperti Tab biasa.
		Tab tab = new ais.ui.util.MyTab(label);
		tabs.appendChild(tab);
		Tabpanel panel = new ais.ui.util.MyTabpanel();
		applyAutoScrollableTabpanel(panel, heightPx != null ? heightPx : "100%");
		panel.setParent(panels);
		return panel;
	}

	static String clean(String value) {
		return value == null ? "" : value.trim();
	}

	private String desktopInstallerProduct() {
		try {
			boolean[] jenisInstansi = Common.chekPtAtauSekolah();
			if (jenisInstansi != null && jenisInstansi.length > 1 && jenisInstansi[1]) {
				return "eSchool";
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "resolve desktop installer product MainAction.java");
		}
		return "eCampus";
	}

	private String resolveLatestDesktopDownloadUrl(String product) {
		String safeProduct = "eSchool".equalsIgnoreCase(product) ? "eSchool" : "eCampus";
		Long cacheTime = desktopReleaseTimeCache.get(safeProduct);
		String cacheUrl = desktopReleaseUrlCache.get(safeProduct);
		long now = System.currentTimeMillis();
		if (cacheTime != null && cacheUrl != null
				&& now - cacheTime.longValue() < DESKTOP_RELEASE_CACHE_TTL_MS) {
			return cacheUrl;
		}

		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(DESKTOP_RELEASE_API).openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(3500);
			connection.setReadTimeout(3500);
			connection.setRequestProperty("Accept", "application/vnd.github+json");
			connection.setRequestProperty("User-Agent", "eCampus-ZKoss-Desktop-Downloader");
			int responseCode = connection.getResponseCode();
			if (responseCode >= 200 && responseCode < 300) {
				StringBuilder json = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
					String line;
					while ((line = reader.readLine()) != null) {
						json.append(line);
					}
				}
				JSONArray releases = new JSONArray(json.toString());
				for (int releaseIndex = 0; releaseIndex < releases.length(); releaseIndex++) {
					JSONObject release = releases.optJSONObject(releaseIndex);
					if (release == null || release.optBoolean("draft", false)
							|| release.optBoolean("prerelease", false)) {
						continue;
					}
					JSONArray assets = release.optJSONArray("assets");
					if (assets == null) {
						continue;
					}
					for (int i = 0; i < assets.length(); i++) {
						JSONObject asset = assets.optJSONObject(i);
						if (asset == null) {
							continue;
						}
						String name = asset.optString("name", "");
						String downloadUrl = asset.optString("browser_download_url", "");
						if (name.startsWith(safeProduct + "-Setup-")
								&& name.toLowerCase().endsWith(".exe")
								&& downloadUrl.startsWith("https://github.com/asroboy/ais_mobile/")) {
							desktopReleaseUrlCache.put(safeProduct, downloadUrl);
							desktopReleaseTimeCache.put(safeProduct, Long.valueOf(now));
							return downloadUrl;
						}
					}
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "resolve latest desktop release MainAction.java");
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return DESKTOP_RELEASE_FALLBACK;
	}

	public void onDownloadDesktop(Event event) {
		String url = resolveLatestDesktopDownloadUrl(desktopInstallerProduct());
		String safeUrl = url.replace("'", "%27");
		Clients.evalJavaScript("window.open('" + safeUrl
				+ "', '_blank', 'noopener,noreferrer');");
	}

	int safeDesktopHeight() {
		return desktopHeight > 0 ? desktopHeight : 768;
	}

	String dashboardHeight(int multiplier) {
		boolean isMobile = false;
		try {
			isMobile = mobile || Common.isMobile();
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:282");
		}
		int value = getConfiguredFrameMinimumHeight(isMobile);
		if (value < 720) {
			value = 720;
		}
		return value + "px";
	}

	String panelHeightOffset(int offset) {
		int value = safeDesktopHeight() - offset;
		if (value < 480) {
			value = 480;
		}
		return value + "px";
	}

	void showError(Exception e) {
		try {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:302");
		}
	}

	private Session openLocalSession() {
		return HibernateUtil.getSessionFactory().openSession();
	}

	private void closeNativeSessionQuietly(Session dbSession) {
		if (dbSession == null) {
			return;
		}
		try {
			if (dbSession.isOpen()) {
				dbSession.clear();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:318");
		}
		try {
			dbSession.disconnect();
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:322");
		}
		try {
			if (dbSession.isOpen()) {
				dbSession.close();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:328");
		}
	}

	private void closePopupQuietly(Popup popup) {
		if (popup == null) {
			return;
		}
		try {
			popup.close();
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:338");
		}
	}

	void applyFullSize(Component component) {
		MainStyleHelper.applyFullSize(component);
	}


	private String dashboardContentHeight() {
		int value = parsePx(panelHeightOffset(150), 620);
		try {
			int configured = getConfiguredFrameMinimumHeight(Common.isMobile()) - 190;
			if (configured > value) {
				value = configured;
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:354");
		}
		if (value < 720) {
			value = 720;
		}
		return value + "px";
	}

	private Div createDashboardMessage(String title, String description, String color) {
		Div message = new Div();
		MainStyleHelper.applyDashboardMessage(message, color);
		Label titleLabel = new Label(title == null ? "Informasi" : title);
		MainStyleHelper.applyDashboardMessageTitle(titleLabel);
		titleLabel.setParent(message);
		Label descriptionLabel = new Label(description == null ? "" : description);
		MainStyleHelper.applyDashboardMessageDescription(descriptionLabel);
		descriptionLabel.setParent(message);
		return message;
	}

	Vbox createDashboardFrame(String title, String description, Component content) {
		String contentHeight = dashboardContentHeight();
		Vbox box = new Vbox();
		box.setWidth("100%");
		box.setHeight("100%");
		MainStyleHelper.applyDashboardFrame(box);

		final Div progress = MainProgressHelper.createProgressPanel("Memuat " + clean(title),
				"Menyiapkan tampilan dan data awal", 12);
		progress.setParent(box);

		Div contentWrapper = new Div();
		MainStyleHelper.applyDashboardContent(contentWrapper, contentHeight);
		contentWrapper.setParent(box);

		/*
		 * Header/pesan dashboard ditempatkan di dalam area scroll konten, namun
		 * wrapper tidak disembunyikan dengan setVisible(false). Pada ZK 5.x,
		 * menyembunyikan wrapper yang berisi komponen kompleks lalu menampilkannya
		 * kembali melalui Timer dapat memicu client error setAttr/mount ketika
		 * widget anak belum selesai terpasang di browser.
		 */
		appendDashboardHeader(contentWrapper, title, description);

		if (content != null) {
			try {
				MainStyleHelper.applyFullSize(content, contentHeight);
				content.setParent(contentWrapper);
			} catch (Exception e) {
				showError(e);
				createDashboardMessage("Tampilan belum dapat dibuka",
						"Silakan muat ulang halaman atau buka menu ini kembali. Data tidak diubah.", "#fecaca")
						.setParent(contentWrapper);
			}
		} else {
			createDashboardMessage("Belum ada data yang ditampilkan",
					"Pilih menu atau filter yang tersedia untuk mulai menampilkan data.", "#bfdbfe")
					.setParent(contentWrapper);
		}
		/*
		 * Cukup sembunyikan progress panel setelah selesai. Content wrapper tetap
		 * mounted sejak awal agar ZK client tidak menerima setAttr untuk widget yang
		 * belum/ tidak lagi ada.
		 */
		MainProgressHelper.startProgressAnimation(progress, "Selesai", true);
		return box;
	}

	private void appendDashboardHeader(Component parent, String title, String description) {
		if (parent == null) {
			return;
		}
		Div header = new Div();
		MainStyleHelper.applyDashboardHeader(header);
		header.setParent(parent);

		Label labelTitle = new Label(clean(title));
		MainStyleHelper.applyDashboardHeaderTitle(labelTitle);
		labelTitle.setParent(header);

		Label labelDescription = new Label(clean(description));
		MainStyleHelper.applyDashboardHeaderDescription(labelDescription);
		labelDescription.setParent(header);
	}

	public void onPustaka(Event event) throws Exception {
		MainDashboardEventHelper.onPustaka(this, event);
	}

	public void onWorkflow(Event event) throws Exception {
		MainDashboardEventHelper.onWorkflow(this, event);
	}

	public void onRepository(Event event) throws Exception {
		MainDashboardEventHelper.onRepository(this, event);
	}

	public void onAntarJemput(Event event) throws Exception {
		MainDashboardEventHelper.onAntarJemput(this, event);
	}

	public void onSpmi(Event event) throws Exception {
		MainDashboardEventHelper.onSpmi(this, event);
	}

	public void onKantin(Event event) throws Exception {
		MainDashboardEventHelper.onKantin(this, event);
	}

	public void onPos(Event event) throws Exception {
		MainDashboardEventHelper.onPos(this, event);
	}

	public void onKoperasi(Event event) throws Exception {
		MainDashboardEventHelper.onKoperasi(this, event);
	}

	public void onEmedic(Event event) throws Exception {
		MainDashboardEventHelper.onEmedic(this, event);
	}

	public void onGaji(Event event) throws Exception {
		MainDashboardEventHelper.onGaji(this, event);
	}

	public void onSinkronisasiFeeder(Event event) throws Exception {
		MainDashboardEventHelper.onSinkronisasiFeeder(this, event);
	}

	public void onSinkronisasiSister(Event event) throws Exception {
		MainDashboardEventHelper.onSinkronisasiSister(this, event);
	}

	public void onAdministrasi(Event event) throws Exception {
		MainDashboardEventHelper.onAdministrasi(this, event);
	}

	public void onPengadaan(Event event) throws Exception {
		MainDashboardEventHelper.onPengadaan(this, event);
	}

	public void onKepegawaian(Event event) throws Exception {
		MainDashboardEventHelper.onKepegawaian(this, event);
	}

	public void onKeuangan(Event event) throws Exception {
		MainDashboardEventHelper.onKeuangan(this, event);
	}

	void applyAutoScrollableTabpanel(Tabpanel panel, String height) {
		if (panel == null) {
			return;
		}

		String resolvedHeight = resolveAutoScrollablePanelHeight(height);
		MainStyleHelper.applyAutoScrollableTabpanel(panel, resolvedHeight);
	}

	private String resolveAutoScrollablePanelHeight(String requestedHeight) {
		boolean isMobile = false;
		try {
			isMobile = mobile || Common.isMobile();
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:516");
		}
		int configuredHeight = getConfiguredFrameMinimumHeight(isMobile);
		int requestedPx = parsePx(requestedHeight, 0);
		boolean relativeHeight = requestedHeight == null || requestedHeight.trim().length() == 0
				|| requestedHeight.indexOf('%') >= 0 || requestedHeight.toLowerCase().indexOf("auto") >= 0;
		if (relativeHeight || requestedPx < configuredHeight) {
			return configuredHeight + "px";
		}
		return requestedPx + "px";
	}

	public void onPembayaran(Event event) throws Exception {
		MainDashboardEventHelper.onPembayaran(this, event);
	}

	public void onKinerja(Event event) throws Exception {
		MainDashboardEventHelper.onKinerja(this, event);
	}

	public void onAkunting(Event event) throws Exception {
		MainDashboardEventHelper.onAkunting(this, event);
	}

	public void onDashboard(Event event) throws Exception {
		MainDashboardEventHelper.onDashboard(this, event);
	}

	public void onKegiatanDanPrestasi(Event event) {
		MainDashboardEventHelper.onKegiatanDanPrestasi(this, event);
	}

	public void onPengumumanPerkuliahan(Event event) throws Exception {
		MainDashboardEventHelper.onPengumumanPerkuliahan(this, event);
	}

	public void onPengumuman(Event event) throws Exception {
		MainDashboardEventHelper.onPengumuman(this, event);
	}

	public void onPresensi(Event event) throws Exception {
		MainDashboardEventHelper.onPresensi(this, event);
	}

	public void onTutupPopup(Event event) {
		closePopupQuietly(popupmenu);
		closePopupQuietly(treeitemUtama);
	}

	public void onTop(Event event) {
		Clients.scrollIntoView(idLogo);
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public void onCustomerService(Event event) throws Exception {
		Map<Long, CustomerService> customerServices = ConstantValues.ambilBerdasarClass(CustomerService.class);

		final MyWindow window = new MyWindow();
		window.setTitle("LAYANAN PENGGUNA");
		window.setClosable(true);
		window.setBorder("none");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setHeight("99%");
		window.setWidth("400px");

		Borderlayout borderlayoutencarian = new Borderlayout();
		borderlayoutencarian.setParent(window);

		Center center = new Center();
		center.setParent(borderlayoutencarian);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid gridcari = new Grid();
		gridcari.setWidth("100%");
		gridcari.setParent(center);
		gridcari.setHeight("100%");

		Rows rowscari = new Rows();
		rowscari.setParent(gridcari);

		MyFormRow rowcari = new MyFormRow();
		rowcari.setParent(rowscari);
		ais.ui.util.ZkCompat.setSpans(rowcari, "2");

		Vbox vbox = new Vbox();
		vbox.setWidth("100%");
		vbox.setHeight("100%");
		vbox.setAlign("center");
		vbox.setPack("center");
		rowcari.appendChild(vbox);

		Image img = new Image(Common.getRequestHostWithProtocol() + "/img/customer-service-man-icon.png");
		vbox.appendChild(img);
		img.setWidth("90%");

		Label aa = new Label(ais.common.Common.getBahasaConfig("BERIKUT DAFTAR LAYANAN PENGGUNA SISTEM :"));
		vbox.appendChild(aa);
		MainStyleHelper.setStyle(aa, MainStyleHelper.CUSTOMER_SERVICE_LABEL);

		for (CustomerService customerService : customerServices.values()) {
			if (customerService != null && Boolean.TRUE.equals(customerService.getAktif())) {
				Group group = new ais.ui.util.MyGroupConfig(customerService.getNama());
				group.setParent(rowscari);
				ais.ui.util.ZkCompat.setSpans(group, "2");

				for (String hp : (customerService.getKeterangan() == null ? "" : customerService.getKeterangan())
						.split(",")) {
					String[] sp = hp.split(":");
					String nama = sp.length > 1 ? sp[1].trim() : "";
					hp = sp.length > 1 ? sp[0].trim() : hp.trim();

					rowcari = new MyFormRow();
					rowcari.setParent(rowscari);

					Vbox vb = new Vbox();
					rowcari.appendChild(vb);

					if (!nama.isEmpty()) {
						aa = new Label(nama);
						vb.appendChild(aa);
						MainStyleHelper.setStyle(aa, MainStyleHelper.CUSTOMER_SERVICE_LABEL);
					}

					aa = new Label(hp);
					vb.appendChild(aa);
					MainStyleHelper.setStyle(aa, MainStyleHelper.CUSTOMER_SERVICE_LABEL);

					Hbox hbox = new Hbox();
					hbox.setParent(rowcari);

					hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
					hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
					hp = !hp.startsWith("+") ? "+62" + hp : hp;

					Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/Whatsapp-icon_kecil.png");
					hbox.appendChild(toolbarbutton);
					toolbarbutton.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text=Halo..");
					toolbarbutton.setTarget("_blank");

					toolbarbutton = new MyToolbarbuttonConfig("", "/img/Apps-Whatsapp-B-icon.png");
					hbox.appendChild(toolbarbutton);
					toolbarbutton.setHref("tel:" + hp);
					toolbarbutton.setTarget("_blank");
				}
			}
		}

		South southPencarian = new South();
		ais.ui.util.ZkCompat.setFlex(southPencarian, true);
		southPencarian.setParent(borderlayoutencarian);

		Toolbar toolbarPencarian = new Toolbar();
		toolbarPencarian.setHeight("25px");
		toolbarPencarian.setParent(southPencarian);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) {
				window.detach();
			}
		});
		cancel.setParent(toolbarPencarian);

		window.setVisible(true);
		window.onModal();
	}

	public void onPopup(Event event) {
		if (isHeaderDropdownMenuEnabled()) {
			try {
				if (popupmenu != null) {
					popupmenu.setWidth("360px");
					popupmenu.setHeight("85%");
					MainStyleHelper.appendSclassOnce(popupmenu, "main-responsive-menu-popup");
					Component anchor = eMenuButton != null ? eMenuButton : (event == null ? null : event.getTarget());
					if (anchor != null) {
						popupmenu.open(anchor, "after_start");
					} else if (iframe == null) {
						popupmenu.open(page.getFirstRoot(), "overlap_after");
					} else {
						popupmenu.open(iframe, "overlap_before");
					}
				}
			} catch (Exception e) {
				showError(e);
			}
			return;
		}
		if (navigation != null)
			navigation.setOpen(true);
		if (popupmenu != null) {
			if (iframe == null)
				popupmenu.open(page.getFirstRoot(), "overlap_before");
			else
				popupmenu.open(iframe, "overlap_before");

			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (session.getAttribute("treeitem") != null
							&& session.getAttribute("treeitem") instanceof MyTreeitemConfig) {
						final MyTreeitemConfig treeitem = (MyTreeitemConfig) session.getAttribute("treeitem");
						if (treeitem != null) {
							try {
								treeitem.getParentItem().setOpen(false);
							} catch (Exception e) {
								treeitem.setOpen(false);
							}
							Common.createDefaultTimer(new EventListener() {
								public void onEvent(Event arg0) {
									if (treeitem != null) {
										try {
											try {
												treeitem.getParentItem().setOpen(true);
											} catch (Exception e) {
												treeitem.setOpen(true);
											}
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								}
							});
						}
					}
				}
			});
		}
	}

	public void onInformasiKalenderAkademik(Event event) throws Exception {
		if (tabKalenderAkademik != null && !tabKalenderAkademik.isInvalidated()) {
			selectExistingTab(tabKalenderAkademik);
			return;
		}
		DasbordKalenderAkademik dashboard = new DasbordKalenderAkademik();
		dashboard.setWidth("100%");
		dashboard.setHeight("100%");
		tabKalenderAkademik = Common.insertUrl(navigasi, menuService, iframe,
				"Kalender Akademik", "Kalender Akademik",
				"/img/svg/information-circle-outline.svg",
				createDashboardFrame("Kalender Akademik",
						"Jadwal seluruh kegiatan akademik kampus dalam satu tampilan — "
						+ "lihat status, durasi, dan sebaran kegiatan sepanjang semester.",
						dashboard));
	}

	public void onDasbordCatatan(Event event) throws Exception {
		if (tabDasbordCatatan != null && !tabDasbordCatatan.isInvalidated()) {
			selectExistingTab(tabDasbordCatatan);
			return;
		}
		DasbordCatatan dashboard = new DasbordCatatan();
		dashboard.setWidth("100%");
		dashboard.setHeight("100%");
		tabDasbordCatatan = Common.insertUrl(navigasi, menuService, iframe,
				"Dasbor Catatan", "Dasbor Catatan",
				"/img/svg/list-task.svg",
				createDashboardFrame("Dasbor Catatan",
						"Ringkasan dan analisis semua catatan — tren, distribusi jenis, "
						+ "pola hari aktif, dan daftar lengkap catatan Anda.",
						dashboard));
	}

	public void onInformasiKegiatan(Event event) throws Exception {
		if (tabInfoKegiatan != null && !tabInfoKegiatan.isInvalidated()) {
			selectExistingTab(tabInfoKegiatan);
			return;
		}
		DasbordInfoKegiatan dashboard = new DasbordInfoKegiatan();
		dashboard.setWidth("100%");
		dashboard.setHeight("100%");
		tabInfoKegiatan = Common.insertUrl(navigasi, menuService, iframe,
				"Info Kegiatan", "Info Kegiatan",
				"/img/svg/information-circle-outline.svg",
				createDashboardFrame("Info Kegiatan",
						"Seluruh kegiatan kampus dalam satu tampilan — "
						+ "lihat status, jadwal waktu, dan detail pelaksanaan setiap kegiatan.",
						dashboard));
	}

	public static void footer(Div parent) {
		if (parent == null) {
			return;
		}
		Sekolah sekolah = SekolahUtil.getSekolah();
		Yayasan yayasan = SekolahUtil.getYayasan();
		String telp = "";
		String notelp = Common.getKonfigurasi("no_whatsapp_operator", "").getNilai();
		String email = "";
		String styleCopyright = ais.common.Common
				.getKonfigurasi("footer_style_main", MainStyleHelper.DEFAULT_FOOTER_COPYRIGHT_STYLE)
				.getNilai();

		Grid grid = new Grid();
		MainStyleHelper.setSclass(grid, "dgrid");
		grid.setHeight("100%");
		grid.setWidth("100%");
		MainStyleHelper.applyTransparent(grid);
		grid.setParent(parent);

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("100%");
		column.setAlign("center");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		MainStyleHelper.applyTransparent(row);
		row.setParent(rows);

		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
		final String nama = (sekolah != null && sekolah.getId() != null) ? sekolah.getNama()
				: (yayasan != null && yayasan.getId() != null) ? yayasan.getNama()
						: (pt != null && pt.getId() != null) ? pt.getNama() : "";

		row = new MyFormRow();
		MainStyleHelper.applyTransparent(row);
		row.setParent(rows);

		Label alamat = new Label();
		MainStyleHelper.applyFooterText(alamat, styleCopyright);
		alamat.setParent(row);
		alamat.setValue(sekolah != null && sekolah.getId() != null && !sekolah.getAlamat().isEmpty()
				? ("Alamat: " + sekolah.getAlamat())
				: yayasan != null && yayasan.getId() != null && !yayasan.getAlamat().isEmpty()
						? ("Alamat: " + yayasan.getAlamat())
						: pt == null ? "" : ("Alamat: " + pt.getAlamat1()));

		if (sekolah != null && sekolah.getId() != null && !sekolah.getWa().isEmpty())
			notelp = sekolah.getWa();
		else if (yayasan != null && yayasan.getId() != null && !yayasan.getWa().isEmpty())
			notelp = yayasan.getWa();
		else if (pt != null && pt.getId() != null && !pt.getWa().isEmpty())
			notelp = pt.getWa();

		if (sekolah != null && sekolah.getId() != null && !sekolah.getTelp().isEmpty())
			telp = sekolah.getTelp();
		else if (yayasan != null && yayasan.getId() != null && !yayasan.getTelp().isEmpty())
			telp = yayasan.getTelp();
		else if (pt != null && pt.getId() != null && !pt.getTelepon().isEmpty())
			telp = pt.getTelepon();

		if (sekolah != null && sekolah.getId() != null && !sekolah.getEmail().isEmpty())
			email = sekolah.getEmail();
		else if (yayasan != null && yayasan.getId() != null && !yayasan.getEmail().isEmpty())
			email = yayasan.getEmail();
		else if (pt != null && pt.getId() != null && !pt.getEmail().isEmpty())
			email = pt.getEmail();

		if ((telp != null && !telp.trim().isEmpty()) || (notelp != null && !notelp.trim().isEmpty())
				|| (email != null && !email.trim().isEmpty())) {
			row = new MyFormRow();
			MainStyleHelper.applyTransparent(row);
			row.setParent(rows);
			Hbox hbox = new Hbox();
			hbox.setWidth("100%");
			hbox.setPack("center");
			hbox.setAlign("center");
			hbox.setParent(row);

			if (telp != null && !telp.trim().isEmpty()) {
				Toolbarbutton phoneBtn = new MyToolbarbuttonConfig(telp, "/img/svg/phone-white.svg");
				MainStyleHelper.applyFooterText(phoneBtn, styleCopyright);
				hbox.appendChild(phoneBtn);
				final String t = telp;
				phoneBtn.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						Executions.getCurrent().sendRedirect("tel:" + t, "_blank");
					}
				});
			}

			if (notelp != null && !notelp.trim().isEmpty()) {
				Toolbarbutton waBtn = new MyToolbarbuttonConfig(notelp, "/img/svg/whats-white.svg");
				MainStyleHelper.applyFooterText(waBtn, styleCopyright);
				hbox.appendChild(waBtn);
				final String t = notelp;
				waBtn.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						String text = "Kami ingin mendapatkan informasi tentang \"" + nama + "\"";
						String hp = t.startsWith("0") ? "+62" + t.substring(1) : t;
						Executions.getCurrent().sendRedirect("https://api.whatsapp.com/send?phone=" + hp + "&text="
								+ URLEncoder.encode(text.replaceAll("<br>", "\n"), "UTF-8"), "_blank");
					}
				});
			}

			if (email != null && !email.trim().isEmpty()) {
				Toolbarbutton emailBtn = new MyToolbarbuttonConfig(email, "/img/svg/mail-send-line-white.svg");
				MainStyleHelper.applyFooterText(emailBtn, styleCopyright);
				hbox.appendChild(emailBtn);
				final String t = email;
				emailBtn.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						String text = "Kami ingin mendapatkan informasi tentang \"" + nama + "\"";
						Executions.getCurrent().sendRedirect("mailto:" + t + "?&subject="
								+ URLEncoder.encode(text, "UTF-8") + "&body=" + URLEncoder.encode(text, "UTF-8"),
								"_blank");
					}
				});
			}
		}

		row = new MyFormRow();
		MainStyleHelper.applyTransparent(row);
		row.setParent(rows);
		Label copyright = new Label();
		MainStyleHelper.applyFooterText(copyright, styleCopyright);
		copyright.setParent(row);

		copyright.setValue(sekolah != null && sekolah.getId() != null
				? ("Copyright " + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + " "
						+ sekolah.getNama())
				: yayasan != null && yayasan.getId() != null
						? ("Copyright " + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + " "
								+ yayasan.getNama())
						: pt == null ? ""
								: ("Copyright " + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + " "
										+ pt.getNama()));
	}

	public void initPesan() throws Exception {
		Konfigurasi conf = Common.getKonfigurasi("message_enabled", Konfigurasi.AKTIF);
		if (message != null)
			message.setVisible(conf.getNilai().equals(Konfigurasi.AKTIF));
	}

	public static void initBg(Center centerTinggiFrame) {
		LampiranLain kop = null;
		try {
			MainStyleHelper.applyBackground(centerTinggiFrame, Common.ROOT + "/img/bg_default.jpg");
			Sekolah sekolah = SekolahUtil.getSekolah();
			if ((sekolah != null && sekolah.getId() != null)) {
				kop = LampiranLain.ambil(sekolah.getId(), LampiranLain.BG_SEKOLAH);
				if (kop == null || kop.getId() == null)
					kop = LampiranLain.ambil(sekolah.getYayasan().getId(), LampiranLain.BG_YAYASAN);
			}
			if (kop == null) {
				Yayasan yayasan = SekolahUtil.getYayasan();
				if ((yayasan != null && yayasan.getId() != null))
					kop = LampiranLain.ambil(yayasan.getId(), LampiranLain.BG_YAYASAN);
			}
			if (kop == null) {
				PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
				if ((pt != null && pt.getId() != null))
					kop = LampiranLain.ambil(pt.getId(), LampiranLain.BG_PT);
			}

			if (kop != null)
				MainStyleHelper.applyBackground(centerTinggiFrame, kop.createLinkUri(true, true));
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public void onInfo(ClientInfoEvent evt) throws Exception {
		if (evt == null) {
			return;
		}

		desktopHeight = evt.getDesktopHeight();
		desktopWidth = evt.getDesktopWidth();
		mobile = Common.isMobile();


		if (tbmuser != null && tbmuser.getUserId() != null) {
			desktopWidths.put(tbmuser.getUserId(), Integer.valueOf(desktopWidth));
			desktopHeights.put(tbmuser.getUserId(), Integer.valueOf(desktopHeight));
		}

		if (tinggiFrame == null || iframe == null) {
			return;
		}

		applyResponsiveShellSizing();

	}

	public void initChatRoom() throws Exception {
		if (mapChat.containsKey(tbmuser.getUserId()))
			mapChat.get(tbmuser.getUserId()).onExit();
		mapChat.put(tbmuser.getUserId(), new ChatThread());

		if (chat != null) {
			session.setAttribute("chat", chat);
			Konfigurasi conf = Common.getKonfigurasi("chat_enabled", Konfigurasi.AKTIF);
			if (conf.getNilai().equals(Konfigurasi.AKTIF)) {
				chat.setVisible(true);
				if (tabChat != null && tabChat.getLinkedPanel() != null) {
					try {
						tabChat.setVisible(false);
						tabChat.getLinkedPanel().setVisible(false);
						tabChat.setSelected(false);
						session.setAttribute("tabChat", tabChat);
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
				if (navigation != null)
					navigation.setOpen(true);
				((Tab) iframe.getTabs().getChildren().get(0)).setSelected(true);
			} else {
				chat.setVisible(false);
			}
		}
	}

	public void onOpenMenu(Event event) throws Exception {
		try {
			if (isHeaderDropdownMenuEnabled()) {
				onPopup(event);
				return;
			}
			menuService.setVisible(false);
			navigasi.setOpen(true);
			forceMainSidebarReflow(true);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public void onNavigasiOpen(Event event) throws Exception {
		try {
			if (isHeaderDropdownMenuEnabled()) {
				applyHeaderDropdownMenuLayout();
				return;
			}
			boolean open = navigasi == null || navigasi.isOpen();
			forceMainSidebarReflow(open);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private boolean isHeaderDropdownMenuEnabled() {
		// Mode dropdown HANYA berlaku pada layout yang menyediakan tombol "Menu" dropdown,
		// yaitu eMenuButton (ADA di index.zul, TIDAK ADA di dekstop.zul). Pada dekstop.zul
		// menu utama berupa panel navigasi kiri (West id="navigasi" title="Menu", lebar 250px).
		// Bila eMenuButton tidak ada tetapi mode dropdown tetap dipaksa, menu malah dialihkan ke
		// popupmenu yang TIDAK punya tombol pembuka → panel "Menu" kosong / menu tidak muncul.
		// Maka: jika eMenuButton null, paksa NON-dropdown agar menu mengisi panel "Menu" kiri.
		if (eMenuButton == null) {
			return false;
		}
		try {
			return Common.bolehKonfigurasi("tampilkan_menu_utama_sebagai_dropdown_header");
		} catch (Exception e) {
			return true;
		}
	}

	private boolean tampilkanShortcutRepositoryAntarJemputDiHeader() {
		try {
			return Common.bolehKonfigurasi("tampilkan_shortcut_repository_antar_jemput_di_header", Konfigurasi.TIDAK_AKTIF);
		} catch (Exception e) {
			return false;
		}
	}


	private boolean roleFlag(Boolean value) {
		return Boolean.TRUE.equals(value);
	}

	private void bukaDashboardDefaultKetikaLogin() {
		try {
			final Tbmrole hakAkses = tbmuser == null ? null : tbmuser.hakAkses();
			final String dashboardDefault = hakAkses == null ? null : hakAkses.getDashboardDefaultMain();
			if (dashboardDefault == null || dashboardDefault.trim().isEmpty()) {
				return;
			}
			final String guard = "dashboard_default_main_dibuka_" + (tbmuser == null ? "" : tbmuser.getUserId()) + "_"
					+ (hakAkses == null ? "" : hakAkses.getRoleId());
			if (session != null && session.getAttribute(guard) != null) {
				return;
			}
			if (session != null) {
				session.setAttribute(guard, Boolean.TRUE);
			}
			Common.createDefaultTimer(new EventListener() {
				public void onEvent(Event event) throws Exception {
					bukaDashboardDefault(dashboardDefault);
				}
			});
		} catch (Exception e) {
			showError(e);
		}
	}

	private void bukaDashboardDefault(String dashboardDefault) throws Exception {
		if (MainDashboardDefaultHelper.MAIN_ELEARNING.equals(dashboardDefault)) {
			onPengumumanPerkuliahan(null);
		} else if (MainDashboardDefaultHelper.MAIN_PRESTASI.equals(dashboardDefault)) {
			onKegiatanDanPrestasi(null);
		} else if (MainDashboardDefaultHelper.MAIN_PUSTAKA.equals(dashboardDefault)) {
			onPustaka(null);
		} else if (MainDashboardDefaultHelper.MAIN_WORKFLOW.equals(dashboardDefault)) {
			onWorkflow(null);
		} else if (MainDashboardDefaultHelper.MAIN_REPOSITORY.equals(dashboardDefault)) {
			onRepository(null);
		} else if (MainDashboardDefaultHelper.MAIN_ANTAR_JEMPUT.equals(dashboardDefault)) {
			onAntarJemput(null);
		} else if (MainDashboardDefaultHelper.MAIN_SPMI.equals(dashboardDefault)) {
			onSpmi(null);
		} else if (MainDashboardDefaultHelper.MAIN_TOKO.equals(dashboardDefault)) {
			onKantin(null);
		} else if (MainDashboardDefaultHelper.MAIN_POS.equals(dashboardDefault)) {
			onPos(null);
		} else if (MainDashboardDefaultHelper.MAIN_KOPERASI.equals(dashboardDefault)) {
			onKoperasi(null);
		} else if (MainDashboardDefaultHelper.MAIN_EMEDIC.equals(dashboardDefault)) {
			onEmedic(null);
		} else if (MainDashboardDefaultHelper.MAIN_GAJI.equals(dashboardDefault)) {
			onGaji(null);
		} else if (MainDashboardDefaultHelper.MAIN_AKADEMIK.equals(dashboardDefault)) {
			onDashboard(null);
		} else if (MainDashboardDefaultHelper.MAIN_ADMINISTRASI.equals(dashboardDefault)) {
			onAdministrasi(null);
		} else if (MainDashboardDefaultHelper.MAIN_PENGADAAN.equals(dashboardDefault)) {
			onPengadaan(null);
		} else if (MainDashboardDefaultHelper.MAIN_PEMBAYARAN.equals(dashboardDefault)) {
			onPembayaran(null);
		} else if (MainDashboardDefaultHelper.MAIN_AKUNTANSI.equals(dashboardDefault)) {
			onAkunting(null);
		} else if (MainDashboardDefaultHelper.MAIN_KINERJA.equals(dashboardDefault)) {
			onKinerja(null);
		} else if (MainDashboardDefaultHelper.MAIN_KEPEGAWAIAN.equals(dashboardDefault)) {
			onKepegawaian(null);
		} else if (MainDashboardDefaultHelper.MAIN_KEUANGAN.equals(dashboardDefault)) {
			onKeuangan(null);
		} else if (MainDashboardDefaultHelper.MAIN_PRESENSI.equals(dashboardDefault)) {
			onPresensi(null);
		} else if (MainDashboardDefaultHelper.MAIN_KALENDER.equals(dashboardDefault)) {
			onInformasiKalenderAkademik(null);
		} else if (MainDashboardDefaultHelper.MAIN_INFO_KEGIATAN.equals(dashboardDefault)) {
			onInformasiKegiatan(null);
		} else if (MainDashboardDefaultHelper.MAIN_FEEDER.equals(dashboardDefault)) {
			onSinkronisasiFeeder(null);
		} else if (MainDashboardDefaultHelper.MAIN_SISTER.equals(dashboardDefault)) {
			onSinkronisasiSister(null);
		} else if (MainDashboardDefaultHelper.isClassOption(dashboardDefault)) {
			bukaDashboardDefaultClass(dashboardDefault);
		}
	}

	private void bukaDashboardDefaultClass(String dashboardDefault) throws Exception {
		MyWindow window = MainDashboardDefaultHelper.createWindow(dashboardDefault);
		if (window == null) {
			return;
		}
		applyFullSize(window);
		String label = MainDashboardDefaultHelper.label(dashboardDefault);
		Common.insertUrl(navigasi, menuService, iframe, label, label, "/img/svg/dashboard-speed.svg",
				createDashboardFrame(label, "Dashboard default berdasarkan pengaturan Grup Pengguna.", window));
	}

	private void setHeaderShortcutVisible(Component component, boolean visible) {
		if (component == null) {
			return;
		}
		try {
			component.setVisible(visible);
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1092");
		}
	}

	private void hideAllHeaderRoleShortcuts() {
		setHeaderShortcutVisible(eLearningButton, false);
		setHeaderShortcutVisible(ePrestasiButton, false);
		setHeaderShortcutVisible(ePustakaButton, false);
		setHeaderShortcutVisible(eAkademikButton, false);
		setHeaderShortcutVisible(eWorkflowButton, false);
		setHeaderShortcutVisible(eRepositoryButton, false);
		setHeaderShortcutVisible(eAntarJemputButton, false);
		setHeaderShortcutVisible(eSpmiButton, false);
		setHeaderShortcutVisible(eKantinButton, false);
		setHeaderShortcutVisible(ePosButton, false);
		setHeaderShortcutVisible(eKoperasiButton, false);
		setHeaderShortcutVisible(eMedicButton, false);
		setHeaderShortcutVisible(eGajiButton, false);
		setHeaderShortcutVisible(eAdministrasiButton, false);
		setHeaderShortcutVisible(ePengadaanButton, false);
		setHeaderShortcutVisible(eKeuanganButton, false);
		setHeaderShortcutVisible(ePembayaranButton, false);
		setHeaderShortcutVisible(eKepegawaianButton, false);
		setHeaderShortcutVisible(eAkuntingButton, false);
		setHeaderShortcutVisible(eKinerjaButton, false);
		setHeaderShortcutVisible(ePresensiButton, false);
		setHeaderShortcutVisible(eKalenderAkademikButton, false);
		setHeaderShortcutVisible(eInfoKegiatanButton, false);
		setHeaderShortcutVisible(eFeederButton, false);
		setHeaderShortcutVisible(eSisterButton, false);
	}

	/* Item paling atas dropdown profil: identitas pengguna (nama + hak akses).
	 * Bukan aksi (disabled); penampilannya diatur CSS .ais-profil-identitas
	 * di css_utama.css blok "PROFIL DROPDOWN MODERN". */
	private static MyMenuitem buatItemIdentitasProfil(Tbmuser tbmuser) {
		String nama = tbmuser == null || tbmuser.getUserNama() == null ? "" : tbmuser.getUserNama();
		String role = tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleName();
		MyMenuitem identitas = new MyMenuitem();
		identitas.setLabel(role == null || role.trim().isEmpty() ? nama : nama + "  ·  " + role);
		identitas.setImage("/img/svg/user-circle-thin.svg");
		identitas.setSclass("menu_item ais-profil-identitas");
		identitas.setDisabled(true);
		return identitas;
	}

	private void refreshHeaderShortcutVisibility() {
		try {
			hideAllHeaderRoleShortcuts();
			boolean dropdown = isHeaderDropdownMenuEnabled();
			setHeaderShortcutVisible(eMenuButton, dropdown);

			Tbmrole hakAkses = tbmuser == null ? null : tbmuser.hakAkses();
			if (hakAkses == null) {
				injectMenuArrowScript();
				return;
			}

			setHeaderShortcutVisible(eLearningButton, roleFlag(hakAkses.getElearning()));
			setHeaderShortcutVisible(ePrestasiButton, roleFlag(hakAkses.getKegiatanDanPrestasi()));
			setHeaderShortcutVisible(ePustakaButton, roleFlag(hakAkses.getPustaka()));
			setHeaderShortcutVisible(eAkademikButton, roleFlag(hakAkses.getDashboard()));
			setHeaderShortcutVisible(eWorkflowButton, roleFlag(hakAkses.getWorkflow()));
			setHeaderShortcutVisible(eAdministrasiButton, roleFlag(hakAkses.getAdministrasi()));
			setHeaderShortcutVisible(ePengadaanButton, roleFlag(hakAkses.getPengadaan()));
			setHeaderShortcutVisible(eKeuanganButton, roleFlag(hakAkses.getKeuangan()));
			setHeaderShortcutVisible(ePembayaranButton, roleFlag(hakAkses.getPembayaran()));
			setHeaderShortcutVisible(eKepegawaianButton, roleFlag(hakAkses.getKepegawaian()));
			setHeaderShortcutVisible(eAkuntingButton,
					roleFlag(hakAkses.getAkunting()) && tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null);
			setHeaderShortcutVisible(eKinerjaButton, roleFlag(hakAkses.getKinerja()));
			setHeaderShortcutVisible(ePresensiButton, roleFlag(hakAkses.getPresensiKehadiran()));
			setHeaderShortcutVisible(eKalenderAkademikButton, roleFlag(hakAkses.getKalenderAkademik()));
			setHeaderShortcutVisible(eInfoKegiatanButton, roleFlag(hakAkses.getInfoKegiatan()));
			setHeaderShortcutVisible(eFeederButton, ais.common.Common.getApakahAdminBolehAksesFeeder());
			setHeaderShortcutVisible(eSisterButton, ais.common.Common.getApakahAdminBolehAksesSister());

			boolean shortcutRepoAntarJemput = tampilkanShortcutRepositoryAntarJemputDiHeader();
			setHeaderShortcutVisible(eRepositoryButton,
					shortcutRepoAntarJemput && roleFlag(hakAkses.getDasborRepository()));
			setHeaderShortcutVisible(eAntarJemputButton,
					shortcutRepoAntarJemput && roleFlag(hakAkses.getDasboardAntarJemput()));
			setHeaderShortcutVisible(eSpmiButton, roleFlag(hakAkses.getTampilkanSpmi()));
			setHeaderShortcutVisible(eKantinButton, roleFlag(hakAkses.getKantin()));
			setHeaderShortcutVisible(ePosButton, roleFlag(hakAkses.getTampilPos()));
			setHeaderShortcutVisible(eKoperasiButton, roleFlag(hakAkses.getDashboardKoperasi()));
			setHeaderShortcutVisible(eMedicButton, roleFlag(hakAkses.getEmedic()));
			setHeaderShortcutVisible(eGajiButton, roleFlag(hakAkses.getTampilkanGaji()));

			injectMenuArrowScript();
		} catch (Exception e) {
			showError(e);
		}
	}

	/**
	 * Suntik skrip klien panah geser strip modul:
	 * - Klik panah kiri/kanan -> gulir strip seketika (sisi klien).
	 * - Panah HANYA tampil saat strip benar-benar melebihi lebar (kelas
	 *   .ais-has-overflow pada baris modul); bila semua modul muat, panah
	 *   disembunyikan. Diperbarui saat resize/scroll. Idempoten.
	 */
	private void injectMenuArrowScript() {
		try {
			injectAutoReloadOnAuErrorScript();
			Clients.evalJavaScript(
				"(function(){var W=window;"
				+ "function row(){return document.querySelector('.main-responsive-module-row');}"
				+ "function scn(){var r=row();if(!r)return null;var c=r.querySelector('.z-row-cnt'),s=r.querySelector('.main-responsive-module-strip');if(c&&c.scrollWidth>c.clientWidth+2)return c;if(s&&s.scrollWidth>s.clientWidth+2)return s;return c||s;}"
				+ "function upd(){var r=row();if(!r)return;var c=r.querySelector('.z-row-cnt'),s=r.querySelector('.main-responsive-module-strip');var h=(c&&c.scrollWidth>c.clientWidth+2)||(s&&s.scrollWidth>s.clientWidth+2);if(r.classList){if(h)r.classList.add('ais-has-overflow');else r.classList.remove('ais-has-overflow');}}"
				+ "W._aisMenuArrowUpd=upd;"
				+ "function go(d){var el=scn();if(!el)return;var px=d*Math.max(180,Math.round(el.clientWidth*0.7));if(el.scrollBy){el.scrollBy({left:px,behavior:'smooth'});}else{el.scrollLeft+=px;}setTimeout(upd,400);}"
				+ "if(!W._aisMenuArrow){W._aisMenuArrow=1;"
				+ "document.addEventListener('click',function(e){var t=e.target;while(t&&t.nodeType===1){var k=''+(t.className||'');if(k.indexOf('main-module-arrow-left')>=0){e.preventDefault();go(-1);return;}if(k.indexOf('main-module-arrow-right')>=0){e.preventDefault();go(1);return;}t=t.parentNode;}},true);"
				+ "W.addEventListener('resize',function(){setTimeout(upd,120);});}"
				+ "var sc=scn();if(sc&&!sc._aisS){sc._aisS=1;sc.addEventListener('scroll',upd);}"
				+ "setTimeout(upd,60);setTimeout(upd,300);setTimeout(upd,900);})();");
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1209");
		}
	}

	/**
	 * Suntik skrip klien agar dialog ZK <i>"The server is temporarily out of service. Would you like
	 * to try again?"</i> (muncul tiap restart Tomcat, ketika request AU menerima halaman HTML—mis.
	 * error proxy / halaman login—alih-alih skrip ZK, sehingga klien gagal mem-parse: "Unexpected
	 * token '&lt;'") TIDAK lagi memunculkan popup, melainkan browser LANGSUNG dimuat ulang ke halaman
	 * utama/login.
	 *
	 * <p><b>Cara kerja:</b> dialog itu adalah {@code window.confirm()} bawaan browser. Skrip ini
	 * meng-override {@code window.confirm} HANYA untuk pesan khas ZK tersebut (deteksi kata
	 * "out of service" / "Unexpected token")—confirm lain di aplikasi tetap berjalan normal. Saat
	 * pesan itu terdeteksi: tidak menampilkan dialog, mengembalikan {@code false} (tidak retry), dan
	 * memuat ulang halaman. Idempoten via flag {@code W._aisAuReload}. Aman: tidak menghapus/mengubah
	 * data apa pun, murni penanganan UI sisi klien.</p>
	 *
	 * <p><b>Catatan deploy:</b> karena ini skrip sisi klien yang dikirim saat halaman dimuat, browser
	 * yang halamannya sudah terbuka SEBELUM versi ini ter-deploy masih memakai perilaku lama pada
	 * restart pertama; setelah user me-refresh sekali, restart berikutnya akan mulus otomatis.</p>
	 */
	private void injectAutoReloadOnAuErrorScript() {
		try {
			// DINONAKTIFKAN secara default (default MATI): jangan auto-reload halaman saat request AU
			// gagal/timeout pada aksi normal (mis. klik "Lihat Tagihan" yang membuka jendela berat) —
			// reload itulah yang memunculkan dialog "Muat ulang situs?". Restart Tomcat tetap ditangani
			// oleh zk.xml error-reload (berbasis HTTP status 404/410/502/503/504), jadi tidak hilang.
			// AKTIFKAN kembali TANPA redeploy: set Konfigurasi "aktifkan_auto_reload_au_error" = Aktif.
			if (!ais.database.model.Konfigurasi.AKTIF.equals(Common.getKonfigurasi(
					"aktifkan_auto_reload_au_error", ais.database.model.Konfigurasi.TIDAK_AKTIF).getNilai())) {
				return;
			}
			Clients.evalJavaScript(
				"(function(){var W=window;if(W._aisAuReload)return;W._aisAuReload=1;"
				+ "var _c=W.confirm;"
				+ "W.confirm=function(m){try{var s=(m==null?'':''+m);"
				// KONSERVATIF: HANYA reload bila respons benar-benar RUSAK ("Unexpected token '<'" =
				// server membalas HTML, bukan skrip ZK = mati/restart). Untuk gangguan SEMENTARA
				// ("server temporarily out of service / try again?" akibat lambat/timeout), JANGAN
				// reload — biarkan dialog "coba lagi" bawaan ZK menanganinya (pulih tanpa reload penuh,
				// kerja pengguna tidak hilang). Ini membuat reload jauh lebih JARANG.
				+ "if(/Unexpected token/i.test(s)){"
				// Reload PEMULIHAN: bersihkan dulu peringatan beforeunload (Clients.confirmClose / ZK)
				// supaya halaman dimuat ulang MULUS tanpa dialog 'Perubahan mungkin tidak disimpan'.
				// Request AU sudah gagal, jadi tak ada yang bisa disimpan dari aksi terakhir itu.
				+ "setTimeout(function(){"
				+ "try{W.onbeforeunload=null;}catch(e){}"
				+ "try{if(W.zk){W.zk.confirmClose=null;}}catch(e){}"
				+ "try{W.location.reload();}catch(e){try{W.location.href=W.location.pathname;}catch(e2){}}"
				+ "},50);"
				+ "return false;}}catch(e){}"
				+ "return _c.apply(W,arguments);};"
				+ "})();");
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1263");
		}
	}

	private void applyHeaderDropdownMenuLayout() {
		try {
			boolean dropdown = isHeaderDropdownMenuEnabled();
			if (eMenuButton != null) {
				eMenuButton.setVisible(dropdown);
				MainStyleHelper.appendSclassOnce(eMenuButton,
						"main-responsive-header-menu-button main-responsive-header-menu-primary");
			}
			boolean mobileClient = false;
			try {
				mobileClient = mobile || Common.isMobile();
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1278");
			}
			if (popupmenu != null) {
				popupmenu.setWidth(mobileClient ? "92%" : "360px");
				popupmenu.setHeight(mobileClient ? "82%" : "85%");
				MainStyleHelper.appendSclassOnce(popupmenu, "main-responsive-menu-popup");
			}
			if (tinggiFrame != null && dropdown) {
				MainStyleHelper.appendSclassOnce(tinggiFrame, "main-responsive-frame-dropdown");
			}
			if (navigasi != null) {
				if (dropdown) {
					navigasi.setVisible(false);
					navigasi.setOpen(false);
					ais.ui.util.ZkCompat.setFlex(navigasi, false);
					navigasi.setWidth("0px");
					MainStyleHelper.appendSclassOnce(navigasi, "main-responsive-sidebar-dropdown-hidden");
				} else {
					navigasi.setVisible(true);
					ais.ui.util.ZkCompat.setFlex(navigasi, true);
					navigasi.setWidth("250px");
				}
			}
			refreshHeaderShortcutVisibility();
			if (dropdown) {
				Clients.evalJavaScript(buildHeaderDropdownLayoutScript());
			}
		} catch (Exception e) {
			showError(e);
		}
	}

	private String buildHeaderDropdownLayoutScript() {
		return "(function(){"
				+ "function all(s,r){return (r||document).querySelectorAll(s);}"
				+ "function fire(){try{if(window.zUtl){zUtl.fireSized();}}catch(e){}}"
				+ "function sp(e,k,v){try{e.style.setProperty(k,v,'important');}catch(x){e.style[k]=v;}}"
				+ "function addCls(e,c){if(!e){return;}var s=' '+(e.className||'')+' ';if(s.indexOf(' '+c+' ')<0){e.className=(e.className?e.className+' ':'')+c;}}"
				+ "function sizeFull(e){if(!e){return;}sp(e,'left','0px');sp(e,'right','0px');sp(e,'width','100%');sp(e,'max-width','100%');sp(e,'min-width','0px');sp(e,'box-sizing','border-box');}"
				+ "function hideWest(w){if(!w){return;}sp(w,'display','none');sp(w,'visibility','hidden');sp(w,'left','-9999px');sp(w,'width','0px');sp(w,'min-width','0px');sp(w,'max-width','0px');sp(w,'overflow','hidden');var b=all('.z-west-body,[class*=z-west-body]',w);for(var i=0;i<b.length;i++){sp(b[i],'display','none');sp(b[i],'width','0px');sp(b[i],'min-width','0px');sp(b[i],'max-width','0px');}}"
				+ "function fix(){try{addCls(document.body,'ecampus-dropdown-menu-active');var frames=all('.main-responsive-frame');for(var f=0;f<frames.length;f++){addCls(frames[f],'main-responsive-frame-dropdown');sizeFull(frames[f]);var west=all('.navigasi,.main-responsive-sidebar,.main-responsive-sidebar-dropdown-hidden,.z-west,.z-west-noborder',frames[f]);for(var w=0;w<west.length;w++){hideWest(w);}var centers=all('.main-responsive-center,.z-center,.z-center-noborder,[class*=z-center]',frames[f]);for(var c=0;c<centers.length;c++){sizeFull(centers[c]);sp(centers[c],'overflow-x','hidden');var bodies=all('.z-center-body,.z-center-body-noborder,[class*=z-center-body]',centers[c]);for(var b=0;b<bodies.length;b++){sizeFull(bodies[b]);}}}}catch(e){}}"
				+ "fix();setTimeout(function(){fire();fix();},30);setTimeout(fix,90);setTimeout(fix,180);setTimeout(fix,420);setTimeout(fix,900);setTimeout(fix,1600);"
				+ "try{if(!window.ecampusHeaderDropdownNoWest){window.ecampusHeaderDropdownNoWest=true;window.addEventListener('resize',function(){setTimeout(fix,20);setTimeout(fix,160);setTimeout(fix,420);});document.addEventListener('click',function(){setTimeout(fix,60);setTimeout(fix,220);setTimeout(fix,700);},true);var n=0,t=setInterval(function(){fix();n++;if(n>20){clearInterval(t);}},350);}}catch(e){}"
				+ "})();";
	}


	private File getInfoPerubahanTampilanFile() {
		try {
			String userId = tbmuser == null || tbmuser.getUserId() == null ? "unknown" : tbmuser.getUserId();
			userId = userId.replaceAll("[^A-Za-z0-9_-]", "_");
			String root = Common.REAL_PATH;
			if (root == null || root.trim().length() == 0) {
				try {
					root = session == null || session.getWebApp() == null ? null : session.getWebApp().getRealPath("/");
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1333");
				}
			}
			File folder = root == null || root.trim().length() == 0 ? new File(System.getProperty("java.io.tmpdir"), "ais")
					: new File(root, "temporary");
			if (!folder.exists()) {
				folder.mkdirs();
			}
			return new File(folder, "info_pengguna_perubahan_tampilan_" + userId + ".txt");
		} catch (Exception e) {
			return new File(System.getProperty("java.io.tmpdir"), "info_pengguna_perubahan_tampilan.txt");
		}
	}

	private void tampilkanInformasiPerubahanTampilanJikaPerlu() {
		try {
			if (!isHeaderDropdownMenuEnabled() || tbmuser == null || tbmuser.getUserId() == null
					|| Common.isMobile()) {
				/* Pada tampilan MOBILE, popup "menu utama berpindah ke tombol Menu (kiri atas)" TIDAK
				 * relevan — navigasi mobile berbeda dari desktop — dan justru membingungkan/mengganggu
				 * (keluhan: menekan tombolnya seolah keluar aplikasi). Karena itu info pembaruan tampilan
				 * DILEWATI di mobile; pengumuman akademik tetap berjalan seperti biasa. Untuk layout lain
				 * (bukan header-dropdown) juga dilewati. */
				tampilkanPengumumanLangsungTampilJikaPerlu();
				return;
			}
			/* Flag per pengguna disimpan langsung di tabel ais_flags_data_baru
			 * (BacaTulisUtil.bacaFlag/tulisFlag): berlaku lintas perangkat dan
			 * tidak hilang saat berganti server. */
			final String kunciFlag = "info_tampilan_baru_user_" + tbmuser.getUserId();
			String apakahSudahPernah = BacaTulisUtil.bacaFlag(kunciFlag);
			if (apakahSudahPernah != null && apakahSudahPernah.trim().length() > 0) {
				tampilkanPengumumanLangsungTampilJikaPerlu();
				return;
			}

			Common.createDefaultTimer(new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					try {
						String cekUlang = BacaTulisUtil.bacaFlag(kunciFlag);
						if (cekUlang != null && cekUlang.trim().length() > 0) {
							tampilkanPengumumanLangsungTampilJikaPerlu();
							return;
						}
						tampilkanPopupInformasiPerubahanTampilan(kunciFlag);
					} catch (Exception e) {
						showError(e);
					}
				}
			});
		} catch (Exception e) {
			showError(e);
		}
	}

	private void tampilkanPopupInformasiPerubahanTampilan(final String kunciFlag) {
		try {
			/* Cegah popup ganda menumpuk (timer/fallback bisa memicu dua kali):
			 * popup kembar di belakang membuat klik "Ya, Paham" tampak tidak
			 * menutup apa pun. */
			if (desktop != null && desktop.getAttribute("aisPopupInfoTampilanAktif") != null) {
				return;
			}
			if (desktop != null) {
				desktop.setAttribute("aisPopupInfoTampilanAktif", Boolean.TRUE);
			}

			Clients.evalJavaScript(buildMenuUpdateCalloutScript(true));

			final MyWindow window = new MyWindow("Informasi Pembaruan Tampilan Sistem", "none", false);
			window.setClosable(false);
			window.setWidth(Common.isMobile() ? "94%" : "560px");
			window.setSclass("ais-ui-update-info-window");
			window.setParent(page == null ? null : page.getFirstRoot());

			Vbox body = new Vbox();
			body.setWidth("100%");
			body.setSclass("ais-ui-update-info-body");
			body.setParent(window);

			Hbox header = new Hbox();
			header.setWidth("100%");
			header.setAlign("center");
			header.setSclass("ais-ui-update-info-head");
			header.setParent(body);

			Label icon = new Label("i");
			icon.setSclass("ais-ui-update-info-icon");
			icon.setParent(header);

			Vbox titleBox = new Vbox();
			titleBox.setParent(header);
			Label title = new Label(ais.common.Common.getBahasaConfig("Sistem telah mendapatkan pembaruan tampilan"));
			title.setSclass("ais-ui-update-info-title");
			title.setParent(titleBox);
			Label subtitle = new Label(ais.common.Common.getBahasaConfig("Navigasi dibuat lebih ringkas agar area kerja utama menjadi lebih luas."));
			subtitle.setSclass("ais-ui-update-info-subtitle");
			subtitle.setParent(titleBox);

			Label paragraf = new Label(
					"Yth. Pengguna Sistem, saat ini aplikasi menggunakan tampilan baru. Menu utama yang sebelumnya berada pada panel navigasi samping telah dipindahkan ke tombol Menu di bagian kiri atas halaman. Silakan klik tombol Menu untuk membuka daftar modul, atau gunakan kolom pencarian di dalam menu tersebut untuk menemukan fitur yang dibutuhkan.");
			paragraf.setMultiline(true);
			paragraf.setSclass("ais-ui-update-info-text");
			paragraf.setParent(body);

			Div info = new Div();
			info.setSclass("ais-ui-update-info-note");
			info.setParent(body);
			Label note = new Label(
					ais.common.Common.getBahasaConfig("Catatan: perubahan ini hanya memindahkan letak menu. Hak akses, data, dan alur kerja Anda tetap sama seperti sebelumnya."));
			note.setMultiline(true);
			note.setParent(info);

			Hbox actions = new Hbox();
			actions.setWidth("100%");
			actions.setPack("end");
			actions.setAlign("center");
			actions.setSclass("ais-ui-update-info-actions");
			actions.setParent(body);

			Toolbarbutton ok = new MyToolbarbuttonConfig("Ya, Paham");
			MainStyleHelper.appendSclassOnce(ok, "ais-ui-update-info-ok");
			ok.setParent(actions);
			ok.addEventListener("onClick", new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					try {
						BacaTulisUtil.tulisFlag(kunciFlag,
								"YA_PAHAM|" + Common.dateFormat51.get().format(WaktuUtil.getDate()));
					} catch (Exception e) {
						showError(e);
					}
					// Callout kecil di dekat tombol Menu jangan ikut ditutup saat popup utama ditutup.
					// Callout memiliki tombol paham sendiri agar pengguna tetap melihat lokasi menu baru.
					/* Penutupan berlapis: setVisible + detach di server, lalu JS
					 * fallback membuang sisa DOM window kembar/mask modal yang
					 * kadang tertinggal di client (gejala "popup tidak mau hilang"). */
					try {
						window.setVisible(false);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1471");
					}
					try {
						window.detach();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1475");
					}
					try {
						if (desktop != null) {
							desktop.removeAttribute("aisPopupInfoTampilanAktif");
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1481");
					}
					Clients.evalJavaScript("(function(){try{"
							+ "var ws=document.querySelectorAll('.ais-ui-update-info-window');"
							+ "for(var i=0;i<ws.length;i++){var n=ws[i];if(n&&n.parentNode){n.parentNode.removeChild(n);}}"
							+ "var ms=document.querySelectorAll('.z-modal-mask');"
							+ "for(var j=0;j<ms.length;j++){if(ms[j]&&ms[j].parentNode){ms[j].parentNode.removeChild(ms[j]);}}"
							+ "}catch(e){}})();");
					tampilkanPengumumanLangsungTampilJikaPerlu();
				}
			});

			window.setVisible(true);
			window.onModal();
		} catch (Exception e) {
			showError(e);
		}
	}

	/* ==================================================================
	 * POPUP PENGUMUMAN AKADEMIK langsungMunculDiTab=true
	 * Tampil berurutan (satu per satu) sebanyak pengumuman yang belum
	 * pernah ditandai "Sudah Mengerti" oleh pengguna ini. Penanda
	 * disimpan langsung di tabel ais_flags_data_baru per user+pengumuman.
	 * ================================================================== */

	private String kunciFlagPengumumanTab(Long idPengumuman) {
		return "pengumuman_tab_user_" + (tbmuser == null ? "0" : tbmuser.getUserId()) + "_" + idPengumuman;
	}

	private void tampilkanPengumumanLangsungTampilJikaPerlu() {
		try {
			if (tbmuser == null || tbmuser.getUserId() == null) {
				return;
			}
			/* Cegah rantai popup ganda menumpuk (timer/fallback bisa memicu dua kali,
			 * atau pemanggil berbeda bisa memanggil method ini lebih dari sekali):
			 * popup kembar di belakang membuat klik "Sudah Mengerti" tampak tidak
			 * menutup apa pun karena modal overlay dari popup lain menghalangi. */
			if (desktop != null && desktop.getAttribute("aisPengumumanTabChainAktif") != null) {
				return;
			}
			if (desktop != null) {
				desktop.setAttribute("aisPengumumanTabChainAktif", Boolean.TRUE);
			}
			Common.createDefaultTimer(new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					muatDanTampilkanPengumumanTab();
				}
			});
		} catch (Exception e) {
			if (desktop != null) {
				try { desktop.removeAttribute("aisPengumumanTabChainAktif"); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1533");}
			}
			showError(e);
		}
	}

	@SuppressWarnings("unchecked")
	private void muatDanTampilkanPengumumanTab() {
		List<PengumumanAkademis> belumDibaca = new ArrayList<PengumumanAkademis>();
		Session hibernateSession = null;
		try {
			hibernateSession = openLocalSession();
			PerguruanTinggi ptAktif = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			Criteria criteria = TampilanPengumumanAkademisAction.initCriteriaStatic(true, tbmuser, ptAktif, null,
					hibernateSession);
			criteria.add(Restrictions.eq("langsungMunculDiTab", true));
			List<PengumumanAkademis> daftar = ConstantValues.simpleList(criteria.setMaxResults(20),
					PengumumanAkademis.class);
			if (daftar != null) {
				org.zkoss.zk.ui.Session zkSess = org.zkoss.zk.ui.Sessions.getCurrent();
				for (PengumumanAkademis pengumuman : daftar) {
					if (pengumuman == null || pengumuman.getId() == null) {
						continue;
					}
					String kunciSess = "__pgtab_" + pengumuman.getId();
					if (zkSess != null && zkSess.getAttribute(kunciSess) != null) {
						continue;
					}
					String sudah = BacaTulisUtil.bacaFlag(kunciFlagPengumumanTab(pengumuman.getId()));
					if (sudah == null || sudah.trim().length() == 0) {
						belumDibaca.add(pengumuman);
					}
				}
			}
		} catch (Exception e) {
			showError(e);
		} finally {
			closeNativeSessionQuietly(hibernateSession);
		}

		if (!belumDibaca.isEmpty()) {
			tampilkanPopupPengumumanTab(belumDibaca, 0);
		} else {
			/* Tidak ada pengumuman yang perlu ditampilkan — lepas guard. */
			if (desktop != null) {
				try { desktop.removeAttribute("aisPengumumanTabChainAktif"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1578");}
			}
		}
	}

	private void tampilkanPopupPengumumanTab(final List<PengumumanAkademis> daftar, final int index) {
		try {
			if (daftar == null || index >= daftar.size()) {
				/* Rantai selesai — lepas guard agar bisa berjalan lagi jika diperlukan. */
				if (desktop != null) {
					try { desktop.removeAttribute("aisPengumumanTabChainAktif"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1588");}
				}
				return;
			}
			final PengumumanAkademis pengumuman = daftar.get(index);

			final MyWindow window = new MyWindow("Pengumuman Akademik", "none", false);
			window.setClosable(false);
			window.setWidth(Common.isMobile() ? "94%" : "560px");
			window.setSclass("ais-ui-update-info-window");
			window.setParent(page == null ? null : page.getFirstRoot());

			Vbox body = new Vbox();
			body.setWidth("100%");
			body.setSclass("ais-ui-update-info-body");
			body.setParent(window);

			Hbox header = new Hbox();
			header.setWidth("100%");
			header.setAlign("center");
			header.setSclass("ais-ui-update-info-head");
			header.setParent(body);

			Label icon = new Label("!");
			icon.setSclass("ais-ui-update-info-icon");
			icon.setParent(header);

			Vbox titleBox = new Vbox();
			titleBox.setParent(header);
			Label title = new Label(pengumuman.getJudul() == null ? Common.getBahasaConfig("Pengumuman")
					: Common.terjemahDinamis(pengumuman.getJudul()));
			title.setSclass("ais-ui-update-info-title");
			title.setParent(titleBox);
			Label subtitle = new Label("Pengumuman " + (index + 1) + " dari " + daftar.size()
					+ " yang perlu Anda baca.");
			subtitle.setSclass("ais-ui-update-info-subtitle");
			subtitle.setParent(titleBox);

			Div isi = new Div();
			isi.setSclass("ais-ui-update-info-text ais-ui-update-info-isi");
			isi.setParent(body);
			String keterangan = null;
			try {
				keterangan = pengumuman.getKeterangan();
			} catch (Exception e) {
				keterangan = null;
			}
			new org.zkoss.zul.Html(keterangan == null || keterangan.trim().isEmpty()
					? "<i>Tidak ada rincian tambahan.</i>" : keterangan).setParent(isi);

			Hbox actions = new Hbox();
			actions.setWidth("100%");
			actions.setPack("end");
			actions.setAlign("center");
			actions.setSclass("ais-ui-update-info-actions");
			actions.setParent(body);

			Toolbarbutton ok = new MyToolbarbuttonConfig("Sudah Mengerti");
			MainStyleHelper.appendSclassOnce(ok, "ais-ui-update-info-ok");
			ok.setParent(actions);
			ok.addEventListener("onClick", new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					try {
						org.zkoss.zk.ui.Session zkSess = org.zkoss.zk.ui.Sessions.getCurrent();
						if (zkSess != null) {
							zkSess.setAttribute("__pgtab_" + pengumuman.getId(), "1");
						}
						try {
							BacaTulisUtil.tulisFlag(kunciFlagPengumumanTab(pengumuman.getId()),
									"SUDAH_MENGERTI|" + Common.dateFormat51.get().format(WaktuUtil.getDate()));
						} catch (Throwable e) {
							/* Tangkap Throwable (termasuk NoClassDefFoundError/SSL)
							 * agar flag-write error tidak mencegah window tertutup. */
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/maintenance/MainAction.java:1661");
						}
					} finally {
						/* PENTING: MyWindow.detach() hanya meng-HIDE (pakaiClose=true),
						 * window modal tidak benar-benar lepas → popup "tidak mau hilang"
						 * walau flag sudah tersimpan. Detach SUNGGUHAN lewat setParent(null)
						 * (pola yang sama dipakai TampilanELearningAction) agar ZK membuang
						 * window beserta modal mask-nya. Di finally → SELALU dijalankan. */
						try { window.setVisible(false); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1669");}
						try { window.setParent(null); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1670");}
						tampilkanPopupPengumumanTab(daftar, index + 1);
						/* JS fallback: bersihkan sisa modal-mask dari popup duplikat
						 * yang mungkin tertinggal di DOM akibat race condition sebelumnya. */
						try {
							Clients.evalJavaScript("(function(){try{"
									+ "var ms=document.querySelectorAll('.z-modal-mask');"
									+ "if(ms.length>1){for(var j=1;j<ms.length;j++){if(ms[j]&&ms[j].parentNode){ms[j].parentNode.removeChild(ms[j]);}}}"
									+ "}catch(e){}})();");
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1679");}
					}
				}
			});

			window.setVisible(true);
			window.onModal();
		} catch (Exception e) {
			/* Exception saat membuat popup — lepas guard agar tidak mengunci selamanya. */
			if (desktop != null) {
				try { desktop.removeAttribute("aisPengumumanTabChainAktif"); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1689");}
			}
			showError(e);
		}
	}

	private String buildMenuUpdateCalloutScript(boolean tampil) {
		if (!tampil) {
			return "(function(){var c=document.getElementById('ecampusMenuUpdateCallout');if(c&&c.parentNode){c.parentNode.removeChild(c);}})();";
		}
		return "(function(){"
				+ "var old=document.getElementById('ecampusMenuUpdateCallout');if(old&&old.parentNode){old.parentNode.removeChild(old);}"
				+ "var btn=document.querySelector('.main-responsive-header-menu-primary')||document.querySelector('.main-responsive-header-menu-button');if(!btn){return;}"
				+ "var r=btn.getBoundingClientRect();var d=document.createElement('div');d.id='ecampusMenuUpdateCallout';d.className='ecampus-menu-update-callout';"
				+ "d.innerHTML='<b>Menu utama berpindah</b><span>Menu utama yang sebelumnya berada di panel samping kini dibuka melalui tombol <strong>Menu</strong> pada header. Klik tombol Menu untuk membuka daftar modul dan gunakan kolom pencarian untuk menemukan fitur.</span><button type=\"button\" onclick=\"var c=document.getElementById(\\\'ecampusMenuUpdateCallout\\\');if(c&&c.parentNode){c.parentNode.removeChild(c);}\">Ya, Mengerti</button>';"
				+ "document.body.appendChild(d);var w=d.offsetWidth||280;var left=Math.max(12,Math.min(r.left,window.innerWidth-w-12));var top=r.bottom+10;"
				+ "d.style.left=left+'px';d.style.top=top+'px';setTimeout(function(){d.className+=\" ecampus-menu-update-callout-show\";},30);"
				+ "})();";
	}

	private void forceMainSidebarReflow(boolean open) {
		try {
			if (isHeaderDropdownMenuEnabled()) {
				applyHeaderDropdownMenuLayout();
				return;
			}
			if (navigasi != null) {
				if (open) {
					navigasi.setWidth("250px");
				} else {
					/* ZK 5.x kadang menyisakan width West lama saat collapse.
					 * Width kecil ini hanya untuk state tertutup agar Center bisa mengisi
					 * ruang yang kosong; ketika dibuka kembali width dikembalikan ke 250px. */
					navigasi.setWidth("24px");
				}
			}
			applyResponsiveShellWidth();
			applyResponsiveShellSizing();
			Clients.evalJavaScript(buildMainSidebarReflowScript(open));
		} catch (Exception e) {
			showError(e);
		}
	}

	private String buildMainSidebarReflowScript(boolean open) {
		int collapsedWidth = 24;
		int openedWidth = 250;
		String forcedWidth = open ? String.valueOf(openedWidth) : String.valueOf(collapsedWidth);
		String openText = open ? "true" : "false";
		return "(function(){"
				+ "function q(s){return document.querySelector(s);}"
				+ "function px(v,d){var m=String(v||'').match(/-?\\d+/);return m?parseInt(m[0],10):d;}"
				+ "function bodyOf(r){return r?r.querySelector('.z-west-body'):null;}"
				+ "function isClosed(w){if(!w){return false;}var b=bodyOf(w);var c=(' '+(w.className||' ')+' ');var btn=w.querySelector('.z-west-exp,.z-borderlayout-icon');return !" + openText + "||c.indexOf(' z-west-collapsed ')>=0||c.indexOf(' z-west-colpsd ')>=0||(btn&&(' '+(btn.className||'')+' ').indexOf(' z-west-exp ')>=0)||(b&&(b.style.display=='none'||b.offsetWidth<5));}"
				+ "function fire(){try{if(window.zUtl){zUtl.fireSized();}}catch(e){}}"
				+ "function fit(){try{var bl=q('.main-responsive-frame.z-borderlayout')||q('.main-responsive-frame');if(!bl){return;}var west=q('.main-responsive-sidebar.z-west')||q('.main-responsive-sidebar');var center=q('.main-responsive-center.z-center')||q('.main-responsive-center');var pw=bl.clientWidth||px(bl.style.width,window.innerWidth||document.documentElement.clientWidth||0);var closed=isClosed(west);var ww=closed?" + collapsedWidth + ":(west?(west.offsetWidth||px(west.style.width," + openedWidth + ")):" + forcedWidth + ");if(west){if(closed){west.className=(' '+west.className+' ').indexOf(' ais-sidebar-collapsed ')>=0?west.className:west.className+' ais-sidebar-collapsed';west.style.width='" + collapsedWidth + "px';west.style.minWidth='" + collapsedWidth + "px';west.style.maxWidth='" + collapsedWidth + "px';west.style.overflow='visible';var wb=bodyOf(west);if(wb){wb.style.display='none';}}else{west.className=(' '+west.className+' ').replace(' ais-sidebar-collapsed ',' ');west.style.width='" + openedWidth + "px';west.style.minWidth='" + openedWidth + "px';west.style.maxWidth='" + openedWidth + "px';west.style.overflowX='hidden';west.style.overflowY='auto';var wb2=bodyOf(west);if(wb2){wb2.style.display='block';wb2.style.width='100%';}}}if(center){var cw=Math.max(0,pw-ww);center.style.left=ww+'px';center.style.width=cw+'px';center.style.maxWidth=cw+'px';center.style.minWidth='0';center.style.boxSizing='border-box';center.style.overflowX='hidden';var cave=center.querySelector('.z-center-body');if(cave){cave.style.width='100%';cave.style.maxWidth='100%';cave.style.minWidth='0';cave.style.boxSizing='border-box';}}fire();}catch(e){}}fit();setTimeout(fit,80);setTimeout(fit,220);setTimeout(fit,600);})();";
	}

	@SuppressWarnings("unchecked")
	public void onPesan(Event event) throws Exception {
		List<MyTabConfig> tabsList = iframe.getTabs().getChildren();
		boolean ada = false;
		for (Tab tab : tabsList) {
			if (tab.getLabel().equalsIgnoreCase("Pesan")) {
				ada = true;
				tab.setSelected(true);
				break;
			}
		}
		if (!ada) {
			final MyTabConfig tab = new MyTabConfig("Pesan");
			tab.setClosable(true);
			tab.setSelected(true);
			tab.setImage("/img/mail_read.png");

			MyIframe include = new MyIframe("/pages/master/message/message.zul");
			include.setHeight("100%");
			include.setWidth("100%");

			iframe.getTabs().appendChild(tab);
			Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.appendChild(include);
			iframe.getTabpanels().appendChild(tabpanel);
		}
	}

	public void onChatRoom(Event event) throws Exception {
		try {
			if (chat != null) {
				chat.setLabel("Chat");
				MainStyleHelper.applyChatTransparent(chat);
			}
			page.setTitle(page.getTitle().replaceAll("PESAN MASUK - ", ""));

			if (tabChat == null) {
				tabChat = new MyTabConfig("Chat");
				tabChat.setClosable(true);
				tabChat.setImage("/img/users16x16.png");
				tabChat.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) {
						page.setTitle(page.getTitle().replaceAll("PESAN MASUK - ", ""));
					}
				});
				MyInclude chartMyIframe = new MyInclude("/pages/master/chat.zul");
				chartMyIframe.setHeight("100%");
				chartMyIframe.setWidth("100%");
				iframe.getTabs().appendChild(tabChat);
				Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
				tabpanel.appendChild(chartMyIframe);
				iframe.getTabpanels().appendChild(tabpanel);
			}
			tabChat.setVisible(true);
			tabChat.setSelected(true);
			tabChat.getLinkedPanel().setVisible(true);
		} catch (Exception e) {
			try {
				tabChat = new MyTabConfig("Chat");
				tabChat.setClosable(false);
				tabChat.setImage("/img/users_24.png");
				tabChat.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) {
						page.setTitle(page.getTitle().replaceAll("PESAN MASUK - ", ""));
					}
				});
				MyInclude chartMyIframe = new MyInclude("/pages/master/chat.zul");
				chartMyIframe.setHeight("100%");
				chartMyIframe.setWidth("100%");
				iframe.getTabs().appendChild(tabChat);
				Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
				tabpanel.appendChild(chartMyIframe);
				iframe.getTabpanels().appendChild(tabpanel);
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1820");
			}
		}
		Sessions.getCurrent().setAttribute("tabChat", tabChat);
	}

	public void onUploadFoto(Event event) throws Exception {
		ForwardEvent forwardEvent = (ForwardEvent) event;
		UploadEvent uploadEvent = (UploadEvent) forwardEvent.getOrigin();
		Session streamingSession = null;

		try {
			streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();

			Blob fotoBlob = Common.getBlobFromMedia(uploadEvent.getMedia());
			String namaFile = uploadEvent.getMedia().getName();
			String contentType = uploadEvent.getMedia().getContentType();

			if (tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getId() != null) {
				Mahasiswa mhs = tbmuser.getMahasiswa();
				FotoMahasiswa fm = (FotoMahasiswa) streamingSession.createCriteria(FotoMahasiswa.class)
						.addOrder(Order.desc("id")).add(Restrictions.eq("mahasiswa", mhs.getId())).setMaxResults(1)
						.uniqueResult();
				if (fm != null)
					streamingSession.delete(fm);
				fm = new FotoMahasiswa();
				fm.setNama(namaFile);
				fm.setKeterangan(contentType);
				fm.setMahasiswa(mhs.getId());
				fm.setFoto(fotoBlob);
				streamingSession.save(fm);
				foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(mhs), 90, 80));
			} else if (tbmuser.getSiswa() != null && tbmuser.getSiswa().getId() != null) {
				Siswa siswa = tbmuser.getSiswa();
				FotoSiswa fs = (FotoSiswa) streamingSession.createCriteria(FotoSiswa.class).addOrder(Order.desc("id"))
						.add(Restrictions.eq("siswa", siswa.getId())).setMaxResults(1).uniqueResult();
				if (fs != null)
					streamingSession.delete(fs);
				fs = new FotoSiswa();
				fs.setNama(namaFile);
				fs.setKeterangan(contentType);
				fs.setSiswa(siswa.getId());
				fs.setFoto(fotoBlob);
				streamingSession.save(fs);
				foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa), 90, 80));
			} else if (tbmuser.getDosen() != null && tbmuser.getDosen().getId() != null) {
				Dosen dosen = tbmuser.getDosen();
				FotoDosen fd = (FotoDosen) streamingSession.createCriteria(FotoDosen.class)
						.add(Restrictions.eq("dosen", dosen.getId())).setMaxResults(1).uniqueResult();
				if (fd != null)
					streamingSession.delete(fd);
				fd = new FotoDosen();
				fd.setNama(namaFile);
				fd.setKeterangan(contentType);
				fd.setDosen(dosen.getId());
				fd.setFoto(fotoBlob);
				streamingSession.save(fd);
				foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen), 90, 80));
			} else if (tbmuser.getGuru() != null && tbmuser.getGuru().getId() != null) {
				Guru guru = tbmuser.getGuru();
				FotoGuru fg = (FotoGuru) streamingSession.createCriteria(FotoGuru.class)
						.add(Restrictions.eq("guru", guru.getId())).setMaxResults(1).uniqueResult();
				if (fg != null)
					streamingSession.delete(fg);
				fg = new FotoGuru();
				fg.setNama(namaFile);
				fg.setKeterangan(contentType);
				fg.setGuru(guru.getId());
				fg.setFoto(fotoBlob);
				streamingSession.save(fg);
				foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(guru), 90, 80));
			} else if (tbmuser.getPegawai() != null && tbmuser.getPegawai().getId() != null) {
				Pegawai pegawai = tbmuser.getPegawai();
				FotoPegawai fp = (FotoPegawai) streamingSession.createCriteria(FotoPegawai.class)
						.add(Restrictions.eq("pegawai", pegawai.getId())).setMaxResults(1).uniqueResult();
				if (fp != null)
					streamingSession.delete(fp);
				fp = new FotoPegawai();
				fp.setNama(namaFile);
				fp.setKeterangan(contentType);
				fp.setPegawai(pegawai.getId());
				fp.setFoto(fotoBlob);
				streamingSession.save(fp);
				foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(pegawai), 90, 80));
			} else if (tbmuser != null && tbmuser.getUserId() != null) {
				FotoAdmin fa = (FotoAdmin) streamingSession.createCriteria(FotoAdmin.class)
						.add(Restrictions.eq("tbmuser", tbmuser.getUserId())).setMaxResults(1).uniqueResult();
				if (fa != null)
					streamingSession.delete(fa);
				fa = new FotoAdmin();
				fa.setNama(namaFile);
				fa.setKeterangan(contentType);
				fa.setTbmuser(tbmuser.getUserId());
				fa.setFoto(fotoBlob);
				streamingSession.save(fa);
				foto.setSrc(CommonMedia.getUrlFotoPengguna(tbmuser, 90, 80));
			}

			streamingSession.getTransaction().commit();
		} catch (Exception e) {
			if (streamingSession != null && streamingSession.getTransaction().isActive()) {
				try {
					streamingSession.getTransaction().rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1924");
				}
			}
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (streamingSession != null) {
				try {
					streamingSession.clear();
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:1932");
				}
			}
		}
	}

	public void tampilPilihRole(final Tbmuser tbmuser, List<Tbmrole> tbmroles) throws Exception {
		final MyWindow window = new MyWindow("Pilih Hak Akses", "none", false);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("90%");
		window.setWidth(Common.isMobile() ? "100%" : "550px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig columnConfig = new MyColumnConfig("");
		columnConfig.setWidth("100px");
		columnConfig.setParent(columns);
		columnConfig = new MyColumnConfig("Nama Pengguna");
		columnConfig.setWidth("40%");
		columnConfig.setParent(columns);
		columnConfig = new MyColumnConfig("Jenis Pengguna");
		columnConfig.setParent(columns);
		columnConfig = new MyColumnConfig("Login");
		columnConfig.setWidth("15%");
		columnConfig.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		final Tbmrole curentRole = tbmuser.hakAkses();

		for (final Tbmrole tbmrole : tbmroles) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			CommonMedia.tampilkanGambarKecil(tbmuser).setParent(row);

			new Label(tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")").setParent(row);
			new Label(tbmrole.getRoleName()).setParent(row);

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Login", "/img/svg/check2.svg");
			toolbarbutton.setOrient("vertical");
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					window.detach();
					HttpServletRequest request = null;
					if (ExecutionsCtrl.getCurrent() != null) {
						request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
					}

					if (request == null) {
						request = RequestContext.get();
					}
					request.getSession(true).setAttribute("udah_tanya", true);
					Tbmuser.getUserRoleYgDipakai.put(tbmuser.getUserId(), tbmrole);
					Common.createDefaultTimer(new EventListener() {
						public void onEvent(Event arg0) {
							Sessions.getCurrent().removeAttribute("current_menus");
							Long sek1 = tbmrole.getSekolah() == null ? -1L : tbmrole.getSekolah().getId();
							Long sek2 = curentRole == null || curentRole.getSekolah() == null ? -1L
									: curentRole.getSekolah().getId();
							Long sek3 = tbmrole.getFakultas() == null
									|| tbmrole.getFakultas().getPerguruanTinggi() == null ? -1L
											: tbmrole.getFakultas().getPerguruanTinggi().getId();
							Long sek4 = curentRole.getFakultas() == null
									|| curentRole.getFakultas().getPerguruanTinggi() == null ? -1L
											: curentRole.getFakultas().getPerguruanTinggi().getId();

							if (sek1.equals(sek2) && sek3.equals(sek4)) {
								try {
									ais.ui.util.ZkCompat.setFlex(navigasi, true);
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								try {
									if (!mobile) {
										if (navigasi != null) {
											navigasi.setOpen(true);
										}
										if (menuService != null) {
											menuService.setVisible(false);
										}
									}
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
								try {
									initData();
								} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
							} else {
								Clients.confirmClose(null);
								ExecutionsCtrl.getCurrent().sendRedirect("/main?d=" + Common.randLong());
							}
						}
					});
				}
			});
			toolbarbutton.setParent(row);
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		window.onModal();
	}

	@Override
	public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
		try {
			String judul = Common.getKonfigurasi("judul_header", "eCampus").getNilai();
			Sekolah sekolah = SekolahUtil.getSekolah();
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			if (sekolah != null && sekolah.getId() != null) {
				judul = Common.getKonfigurasi("judul_header_sekolah", "eSchool").getNilai();
				page.setTitle((judul.isEmpty() ? "" : judul + " | ") + sekolah.getNama());
			} else if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				page.setTitle((judul.isEmpty() ? "" : judul + " | ") + perguruanTinggi.getNama());
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings({ "unchecked" })
	public void doAfterCompose(Component comp) throws Exception {
		tbmuser = Common.getCurrentUser();
		System.out.println("[SOCIAL-LOGIN] MainAction.doAfterCompose(): Common.getCurrentUser() => "
				+ (tbmuser == null ? "NULL (sesi TIDAK terbawa ke halaman main -- ini penyebab ke-logoff-an)"
						: "userId=" + tbmuser.getUserId() + ", aktif=" + tbmuser.getAktif()));
		// getAktif() bisa mengembalikan Boolean NULL (mis. akun anggota koperasi/pedagang yang aktif-nya
		// null) -> "!tbmuser.getAktif()" auto-unbox NULL = NullPointerException di SETIAP muat halaman
		// utama untuk akun tsb (kandidat "fast-throw" NPE tanpa stack trace). Pakai Boolean.FALSE.equals
		// agar hanya logoff bila aktif BENAR-BENAR false; null diperlakukan sbg aktif (tidak mengunci user).
		if (tbmuser == null || Boolean.FALSE.equals(tbmuser.getAktif())) {
			System.out.println("[SOCIAL-LOGIN] MainAction.doAfterCompose(): panggil Common.goLogoff() -- "
					+ (tbmuser == null ? "tbmuser null" : "tbmuser.getAktif()==false"));
			Common.goLogoff();
			return;
		}

		Tbmrole tbmrole = tbmuser.hakAkses();
		if (tbmrole != null && tbmrole.getRoleId() != null && tbmrole.getRoleId().equalsIgnoreCase(Tbmrole.KANTIN)) {
			ExecutionsCtrl.getCurrent().sendRedirect("/baru?p=kantin&s=ringkasan");
			return;
		}

		HttpServletRequest request = null;
		if (ExecutionsCtrl.getCurrent() != null) {
			request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		}

		if (request == null) {
			request = RequestContext.get();
		}

		if (ConstantValues.passwordKuat) {
			try {
				String pass = tbmuser.getUserPassword();
				if (!PasswordChecker.isValidPassword(Common.desEncrypter.get().decrypt(pass))) {
					MyMessageboxConfig.show(
							"Demi menjaga keamanan akun Bapak/Ibu, mohon maaf Bapak/Ibu diharapkan segera mengganti kata sandi. Kata sandi baru wajib terdiri atas minimal 8 karakter serta memuat kombinasi huruf, angka, dan sekurang-kurangnya satu karakter khusus seperti !@#$%^&*() untuk keamanan yang lebih baik.\n\nLangkah yang dapat dilakukan: (1) tekan tombol OK untuk membuka formulir penggantian kata sandi; (2) masukkan kata sandi baru sesuai ketentuan di atas; (3) simpan perubahan kata sandi tersebut.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
								public void onEvent(Event arg0) throws Exception {
									ChangePasswordWindow window = new ChangePasswordWindow(true, false);
									window.setVisible(true);
									window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
									window.setHeight("450px");
									window.setWidth("600px");
									window.onModal();
								}
							});
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		merupakanAdmin = Common.getApakahAdmin();

		Sekolah sekolah = SekolahUtil.getSekolah(request);
		Siswa siswa = tbmuser.getSiswa();

		if ((siswa != null && (sekolah == null || sekolah.getId() == null))
				|| (siswa != null && siswa.getSekolah() != null && (sekolah != null && sekolah.getId() != null
						&& !sekolah.getId().equals(siswa.getSekolah().getId())))) {
			if (siswa != null && siswa.getSekolah() != null)
				sekolah = siswa.getSekolah();

			if (sekolah == null || !sekolah.getSiswaDiizinkanDiPortalYayasan()) {
				Clients.confirmClose(null);
				Common.createDefaultTimer(new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						ExecutionsCtrl.getCurrent().sendRedirect("/logoff?login_error=" + (URLEncoder.encode(
								"Akun siswa tidak diizinkan login di portal bukan sekolah. Harap menghubungi bagian akademik untuk informasi lebih lanjut.",
								"UTF-8")) + "");
						session.invalidate();
					}
				});
				return;
			}
		}

		Mahasiswa mahasiswa = tbmuser.getMahasiswa();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
		Yayasan yayasan = SekolahUtil.getYayasan(request);
		// Guard defensif (samakan dgn zscript index.zul yg sudah cek "ptYa!=null && ptYa.length>1"):
		// cegah NPE/AIOOBE bila chekPtAtauSekolah() mengembalikan null / array pendek.
		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa != null && ptYa.length > 0 ? ptYa[0] : true;
		ya = ptYa != null && ptYa.length > 1 ? ptYa[1] : false;

		try {
			String judul = Common.getKonfigurasi("judul_header", "eCampus").getNilai();
			String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
					.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_", perguruanTinggi);
			if (logo_PerguruanTinggi == null || logo_PerguruanTinggi.trim().isEmpty())
				logo_PerguruanTinggi = "/img/logo.png";

			if (ya && (sekolah != null && sekolah.getId() != null)) {
				judul = Common.getKonfigurasi("judul_header_sekolah", "eSchool").getNilai();
				ExecutionsCtrl.getCurrentCtrl().getCurrentPageDefinition()
						.setTitle((judul.isEmpty() ? "" : judul + " | ") + sekolah.getNama());
				String logo_PT_local = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia(request,
						"logo_sekolah_", sekolah);
				if (logo_PT_local != null && !logo_PT_local.endsWith("logo.png"))
					logo_PerguruanTinggi = logo_PT_local;
			} else if (ya && (yayasan != null && yayasan.getId() != null)) {
				judul = Common.getKonfigurasi("judul_header_sekolah", "eSchool").getNilai();
				ExecutionsCtrl.getCurrentCtrl().getCurrentPageDefinition()
						.setTitle((judul.isEmpty() ? "" : judul + " | ") + yayasan.getNama());
				String logo_PT_local = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia(request,
						"logo_yayasan_", yayasan);
				if (logo_PT_local != null && !logo_PT_local.endsWith("logo.png"))
					logo_PerguruanTinggi = logo_PT_local;
			} else if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
				ExecutionsCtrl.getCurrentCtrl().getCurrentPageDefinition()
						.setTitle((judul.isEmpty() ? "" : judul + " | ") + perguruanTinggi.getNama());
			}
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().setAttribute("myFavicon", logo_PerguruanTinggi);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if ((mahasiswa != null && (perguruanTinggi == null || perguruanTinggi.getId() == null))
				&& (mahasiswa != null && mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null
						&& mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null
						&& (perguruanTinggi != null && perguruanTinggi.getId() != null && !perguruanTinggi.getId()
								.equals(mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getId())))) {
			Clients.confirmClose(null);
			Common.createDefaultTimer(new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					ExecutionsCtrl.getCurrent().sendRedirect("/logoff?login_error=" + (URLEncoder.encode(
							"Akun mahasiswa tidak diizinkan login di portal bukan perguruan tinggi. Harap menghubungi bagian akademik untuk informasi lebih lanjut.",
							"UTF-8")) + "");
					session.invalidate();
				}
			});
			return;
		}

		tbmroles = tbmuser.ambilRoles();
		if (tbmroles.size() > 1 && request.getSession(true).getAttribute("udah_tanya") == null)
			tampilPilihRole(tbmuser, tbmroles);

		if (request != null
				&& (request.getServerName().startsWith("pmb") || request.getServerName().startsWith("spmb"))) {
			ExecutionsCtrl.getCurrent().sendRedirect("/pmb");
			return;
		}
		if (request != null
				&& (request.getServerName().startsWith("alumni") || request.getServerName().startsWith("tracer"))) {
			ExecutionsCtrl.getCurrent().sendRedirect("/alumni");
			return;
		}

		if (tbmuser != null && tbmuser.getPenyediaAsset() != null) {
			HttpSession session = request.getSession(true);
			session.setAttribute("PenyediaAsset", tbmuser.getPenyediaAsset());
			session.setAttribute("mytbmuser", tbmuser);
			session.setAttribute("usersTemp", tbmuser);
			session.setAttribute("user", tbmuser);
			ExecutionsCtrl.getCurrent().sendRedirect("/vendor");
			return;
		}

		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			Common.setLogin(tbmuser.getBiodataCalonMahasiswa());
			ExecutionsCtrl.getCurrent().sendRedirect("/pmb");
			return;
		}
		if (tbmuser != null && tbmuser.getCalonSiswa() != null) {
			Common.setLogin(tbmuser.getCalonSiswa());
			ExecutionsCtrl.getCurrent().sendRedirect("/psb");
			return;
		}

		if (tbmuser != null && tbmuser.getCalonPegawai() != null) {
			HttpSession session = request.getSession(true);
			session.setAttribute("CalonPegawai", tbmuser.getCalonPegawai());
			session.setAttribute("mytbmuser", tbmuser);
			session.setAttribute("usersTemp", tbmuser);
			session.setAttribute("user", tbmuser);
			ExecutionsCtrl.getCurrent().sendRedirect("/karir");
			return;
		}

		if (tbmuser != null && request.getServerName().startsWith("digital.")) {
			HttpSession session = request.getSession(true);
			session.setAttribute("PesertaKursus", tbmuser.getPesertaKursus());
			session.setAttribute("mytbmuser", tbmuser);
			session.setAttribute("usersTemp", tbmuser);
			session.setAttribute("user", tbmuser);
			ExecutionsCtrl.getCurrent().sendRedirect("/kursus");
			return;
		}

		super.doAfterCompose(comp);

		refreshHeaderShortcutVisibility();

		Common.createDefaultTimer(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				Clients.confirmClose(Common.getBahasaConfig("Apakah Anda yakin ingin keluar dari aplikasi ini ?"));
			}
		});

		SessionCounter.initSessionTimeout(request.getSession(), tbmuser, false);
		MainAction.initBg(centerTinggiFrame);
		MainAction.footer(footer);

		menuBgColor = Common.getKonfigurasi("menu_bg_color", "#F5F5F5").getNilai();
		Common.initBahasaParameter(execution.getParameter("lang"));
		Common.REAL_PATH = session.getWebApp().getRealPath("/");
		Common.REAL_PATH_REPORT_TEMP = session.getWebApp().getRealPath("/report");
		Common.initTemp();
		CommonMedia.getMediaDirectory();

		if (!MainHelper.initMain(session, execution, page, tbmuser))
			return;
		login = (LogLogin) session.getAttribute("login");

		if (session != null) { session.setAttribute("tabs", tabs); }
		if (session != null) { session.setAttribute("tabpanels", tabpanels); }
		bukaDashboardDefaultKetikaLogin();

		Timer timer = new Timer(60000);
		if (timer != null) { timer.setRepeats(true); }
		if (timer != null) { timer.setParent(page.getFirstRoot()); }
		timer.addEventListener("onTimer", new EventListener() {
			public void onEvent(Event arg0) {
				updateUserOnline();
			}
		});
		timer.start();

		if (customerService != null) {
			customerService.setVisible(false);
			Map<Long, CustomerService> customerServices = ConstantValues.ambilBerdasarClass(CustomerService.class);
			for (CustomerService cs : customerServices.values()) {
				if (cs.getAktif()) {
					customerService.setVisible(true);
					break;
				}
			}
		}

		if (!udah) {
			udah = true;
			try {
				ais.ui.util.ZkCompat.setFlex(navigasi, true);
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			try {
				mobile = Common.isMobile();
				if (isHeaderDropdownMenuEnabled()) {
					applyHeaderDropdownMenuLayout();
				} else if (!mobile) {
					// Null-safe: dekstop.zul memiliki West "navigasi" tetapi TIDAK punya komponen
					// "menuService" (hanya index.zul yang punya) → tanpa guard, menuService null → NPE
					// di doAfterCompose saat membuka halaman desktop (mode klasik/non-dropdown).
					if (navigasi != null) {
						navigasi.setOpen(true);
					}
					if (menuService != null) {
						menuService.setVisible(false);
					}
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			initData();
			applyResponsiveShellEnhancement(comp);
			tampilkanInformasiPerubahanTampilanJikaPerlu();
		}
	}

	/**
	 * Suntik tombol NATIVE (anchor HTML murni, BUKAN komponen ZK) untuk berpindah
	 * Mode Desktop / Mode HP. Karena anchor href biasa, tombol SELALU bisa diklik
	 * walau menu dropdown ZK bermasalah di sebagian HP (keluhan "menu tak bisa diklik").
	 *
	 * LABEL ditentukan oleh HALAMAN yang sedang dibuka (bukan flag is_mobile, yang
	 * bisa lengket dan menyesatkan): di halaman desktop (dekstop.zul lewat servlet
	 * Desktop.java — ditandai atribut ZK desktop "aisHalamanDesktop") tampil
	 * "Mode HP"; di halaman biasa (index.zul) tampil "Mode Desktop".
	 *
	 * TAMPIL OTOMATIS & RESPONSIF: hanya muncul di layar kecil (≤980px) lewat CSS
	 * media query, jadi di PC layar lebar tidak mengganggu. Mengapung fixed
	 * kiri-bawah, z-index maksimum agar tak tertutup overlay.
	 */
	private void tampilkanTombolModeTampilan() {
		try {
			HttpServletRequest req = (HttpServletRequest) execution.getNativeRequest();
			if (req == null) {
				return;
			}
			String ctx = req.getContextPath();
			if (ctx == null) {
				ctx = "";
			}

			boolean halamanDesktop = false;
			try {
				halamanDesktop = Boolean.TRUE.equals(execution.getDesktop().getAttribute("aisHalamanDesktop"));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:2377");
			}

			// Perangkat HP ASLI tetapi sedang dirender dalam mode desktop (mis. is_mobile=false
			// "lengket" di sesi) → tampilan jadi sempit/berantakan. Tawarkan "Mode Mobile" dan
			// PAKSA tombol tetap terlihat (viewport bisa selebar desktop sehingga media query
			// ≤980px tidak berlaku), supaya pengguna bisa mencoba mode mobile.
			if (Common.isAsliMobile() && !Common.isMobile()) {
				ais.ui.util.ModeTampilanUtil.inject(ctx + "/main?is_mobile=true", "&#128241;", "Mode Mobile", true);
				return;
			}

			String label;
			String icon;
			String href;
			if (halamanDesktop) {
				// Sedang di tampilan desktop (servlet Desktop.java / dekstop.zul) → kembali ke HP.
				// Arahkan ke servlet Main (/main) — entri utama dgn routing lengkap — sambil
				// menyetel is_mobile=true agar tampilan kembali ke mode HP.
				label = "Mode HP";
				icon = "&#128241;"; // ponsel
				href = ctx + "/main?is_mobile=true";
			} else {
				// Halaman biasa (index.zul). Default & saat diakses dari HP → tawarkan Mode Desktop.
				label = "Mode Desktop";
				icon = "&#128421;"; // monitor
				href = ctx + "/desktop";
			}

			// Injeksi tombol dipusatkan di ModeTampilanUtil (dipakai juga pmb.zul/psb.zul).
			ais.ui.util.ModeTampilanUtil.inject(href, icon, label);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:2408");
			// jangan ganggu render bila injeksi gagal
		}
	}

	private void initData() throws Exception {
		Common.clear(navigasi);
		try {
			if (popupmenu != null) {
				Common.clear(popupmenu);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		applyHeaderDropdownMenuLayout();
		boolean mobileAndroid = login != null && login.getLinkProfile() != null
				&& login.getLinkProfile().equalsIgnoreCase("Login via mobile");
		mobile = Common.isMobile();
		boolean dropdownLayout = isHeaderDropdownMenuEnabled();
		boolean langsungPengumuman = false;

		PerguruanTinggi ptAktif = PerguruanTinggiUtil.getPerguruanTinggi();
		Sekolah sekolahAktif = SekolahUtil.getSekolah();
		Yayasan yayasanAktif = SekolahUtil.getYayasan();

		Session hibernateSession = null;
		List<PengumumanAkademis> listPengumumanAkademis = new ArrayList<PengumumanAkademis>();

		try {
			hibernateSession = openLocalSession();
			listPengumumanAkademis = ConstantValues.simpleList(
					TampilanPengumumanAkademisAction.initCriteriaStatic(true, tbmuser, ptAktif, null, hibernateSession)
							.setMaxResults(Common.ROWS_COUNT_ON_PAGE_1),
					PengumumanAkademis.class);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			closeNativeSessionQuietly(hibernateSession);
		}

		pengumumanAkademis = !listPengumumanAkademis.isEmpty() ? listPengumumanAkademis.get(0) : null;

		langsungPengumuman = renderModernHomeCenter(ptAktif);

		tampilkanTombolModeTampilan();

		if (headerHbox != null && (mobile || mobileAndroid) && !dropdownLayout) {
			Common.clear(headerHbox);
			try {
				HttpServletRequest request = (HttpServletRequest) execution.getNativeRequest();
				ais.database.model.PerguruanTinggi ptLokal = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggi(request);
				String logo_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil
						.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_");
				if (logo_PerguruanTinggi == null || logo_PerguruanTinggi.trim().isEmpty())
					logo_PerguruanTinggi = "/img/logo.png";

				String judul = ptLokal == null ? "" : ptLokal.getNama();
				String Motto = ptLokal == null || ptLokal.getMotto() == null || ptLokal.getMotto().trim().isEmpty()
						? "eCampus Information System"
						: ptLokal.getMotto();
				String Alamat1 = ptLokal == null ? "" : ptLokal.getAlamat1();
				String Telepon = ptLokal == null ? "" : ptLokal.getTelepon();
				String Email = ptLokal == null ? "" : ptLokal.getEmail();

				if (sekolahAktif != null && sekolahAktif.getId() != null) {
					judul = sekolahAktif.getNama();
					Motto = sekolahAktif.getMotto() != null ? sekolahAktif.getMotto() : Motto;
					Alamat1 = sekolahAktif.getAlamat() != null ? sekolahAktif.getAlamat() : Alamat1;
					Telepon = sekolahAktif.getTelp() != null ? sekolahAktif.getTelp() : Telepon;
					Email = sekolahAktif.getEmail() != null ? sekolahAktif.getEmail() : Email;
					logo_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getSekolahMedia("logo_sekolah_");
				} else if (yayasanAktif != null && yayasanAktif.getId() != null) {
					judul = yayasanAktif.getNama();
					Motto = yayasanAktif.getMotto() != null ? yayasanAktif.getMotto() : Motto;
					Alamat1 = yayasanAktif.getAlamat() != null ? yayasanAktif.getAlamat() : Alamat1;
					Telepon = yayasanAktif.getTelp() != null ? yayasanAktif.getTelp() : Telepon;
					Email = yayasanAktif.getEmail() != null ? yayasanAktif.getEmail() : Email;
					logo_PerguruanTinggi = ais.action.master.sekolah.util.SekolahUtil.getYayasanMedia("logo_yayasan_");
				}

				Image image = new Image(logo_PerguruanTinggi);

				Grid grid = new Grid();
				MainStyleHelper.setSclass(grid, "dgrid");
				grid.setOddRowSclass("non-odd");
				headerHbox.setHeight("230px");
				grid.setHeight("230px");
				MainStyleHelper.applyTransparent(grid);
				grid.setParent(headerHbox);

				Columns columns = new Columns();
				columns.setParent(grid);
				Column column = new Column();
				column.setWidth("100%");
				column.setAlign("center");
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				MainStyleHelper.applyTransparent(row);
				row.setParent(rows);
				row.appendChild(image);

				MainStyleHelper.setSclass(headerHbox, "headerHbox_mobile");

				Label title = new Label(judul);
				Label motto = new Label(Motto);
				Label alamat = new Label(Alamat1 + ", Telp. " + Telepon + ", Email : " + Email);
				MainStyleHelper.setStyle(title, MainStyleHelper.MOBILE_TITLE);
				MainStyleHelper.setSclass(title, "title1");
				MainStyleHelper.setStyle(motto, MainStyleHelper.MOBILE_MOTTO);
				MainStyleHelper.setSclass(motto, "motto");
				MainStyleHelper.setStyle(alamat, MainStyleHelper.MOBILE_ADDRESS);
				MainStyleHelper.setSclass(alamat, "alamat");

				row = new MyFormRow();
				MainStyleHelper.applyTransparent(row);
				row.setParent(rows);
				row.appendChild(title);
				row = new MyFormRow();
				MainStyleHelper.applyTransparent(row);
				row.setParent(rows);
				row.appendChild(motto);
				row = new MyFormRow();
				MainStyleHelper.applyTransparent(row);
				row.setParent(rows);
				row.appendChild(alamat);
				image.setHeight("58px");

				row = new MyFormRow();
				MainStyleHelper.applyTransparent(row);
				row.setParent(rows);

				Grid gridsub = new Grid();
				MainStyleHelper.setSclass(gridsub, "dgrid");
				gridsub.setOddRowSclass("non-odd");
				gridsub.setWidth("100%");
				MainStyleHelper.applyTransparent(gridsub);
				gridsub.setParent(row);
				Columns columnssub = new Columns();
				columnssub.setParent(gridsub);
				Column col = new Column();
				col.setWidth("50%");
				col.setAlign("left");
				col.setParent(columnssub);
				col = new Column();
				col.setWidth("50%");
				col.setAlign("right");
				col.setParent(columnssub);

				Rows rowssub = new Rows();
				rowssub.setParent(gridsub);
				MyFormRow rowsub_el = new MyFormRow();
				MainStyleHelper.applyTransparent(rowsub_el);
				rowsub_el.setParent(rowssub);

				MyToolbarbutton toolbarbuttonMenu = new MyToolbarbutton("fa-bars fa-big", "");
				MainStyleHelper.setSclass(toolbarbuttonMenu, "user_button_profile");
				treeitemUtama = new Popup();
				treeitemUtama.setWidth("290px");
				treeitemUtama.setHeight("80%");
				treeitemUtama.setParent(page.getFirstRoot());
				toolbarbuttonMenu.setPopup(treeitemUtama);

				Hbox hbox = new Hbox();
				/* Penyelaras tombol kanan header: css_utama.css blok "HEADER KANAN RAPI". */
				MainStyleHelper.appendSclassOnce(hbox, "ais-header-kanan");
				rowsub_el.appendChild(hbox);
				hbox.appendChild(toolbarbuttonMenu);
				notif(hbox);

				List<MyMenuitem> toolbarbuttons = new ArrayList<MyMenuitem>();

				MyMenuitem menuitemKeluar = new MyMenuitem("Keluar", "/img/svg/power.svg");
				menuitemKeluar.setSclass("menu_item ais-profil-keluar");
				menuitemKeluar.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						MainHelper.onKeluar(login);
					}
				});
				toolbarbuttons.add(menuitemKeluar);

				if (!mobileAndroid) {
					MyMenuitem menuitemDesktop = new MyMenuitem("Tampilan Desktop", "/img/svg/desktop-light.svg");
					menuitemDesktop.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) {
							execution.sendRedirect("desktop?is_mobile=false");
						}
					});
					toolbarbuttons.add(menuitemDesktop);
				}

				if (tbmroles.size() > 1 && !mobileAndroid) {
					MyMenuitem menuitemHak = new MyMenuitem("Ganti Hak Akses", "/img/svg/user-group.svg");
					menuitemHak.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							tampilPilihRole(tbmuser, tbmroles);
						}
					});
					toolbarbuttons.add(menuitemHak);
				}

				MyMenuitem menuitemBantuan = new MyMenuitem("Bantuan", "/img/svg/question-circle.svg");
				menuitemBantuan.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						MainHelper.onKatalogBantuan(login);
					}
				});
				toolbarbuttons.add(menuitemBantuan);

				if (!mobileAndroid) {
					MyMenuitem menuitemVersi = new MyMenuitem("Versi Mobile", "/img/svg/android-logo-thin.svg");
					menuitemVersi.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							MyWindow w = new MyWindow();
							w.setHeight("99%");
							w.setWidth("400px");
							w.setClosable(true);
							w.setTitle("Aplikasi Versi Mobile");
							w.setBorder("none");
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(w);
							MainHelper.onDapatkanKode(w, true);
							w.setVisible(true);
							w.onModal();
						}
					});
					toolbarbuttons.add(menuitemVersi);
				}

				MyMenuitem menuitemProfil = new MyMenuitem("Profil", "/img/svg/user-circle-thin.svg");
				menuitemProfil.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						MainHelper.onUbahBiodata(tbmuser, foto);
					}
				});
				toolbarbuttons.add(menuitemProfil);

				MyMenuitem menuitemPass = new MyMenuitem("Password", "/img/svg/key.svg");
				menuitemPass.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						ChangePasswordWindow w = new ChangePasswordWindow(false, true);
						w.setVisible(true);
						w.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						w.setHeight("450px");
						w.setWidth("600px");
						w.onModal();
					}
				});
				toolbarbuttons.add(menuitemPass);

				if (tbmuser != null && tbmuser.getPegawai() != null
						&& Common.bolehKonfigurasi("tampilkan_slip_gaji_di_menu", Konfigurasi.TIDAK_AKTIF)) {
					MyMenuitem menuitemSlip = new MyMenuitem("Slip Gaji", "/img/svg/cash.svg");
					menuitemSlip.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							LaporanSlipGajiRealPegawaiPerOrang w = new LaporanSlipGajiRealPegawaiPerOrang(
									tbmuser.getPegawai());
							w.setVisible(true);
							w.setClosable(true);
							w.setTitle("Slip Gaji");
							w.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
							w.setHeight("95%");
							w.setWidth("95%");
							w.onModal();
						}
					});
					toolbarbuttons.add(menuitemSlip);
				}

				MyMenuitem mi1 = (MyMenuitem) DashboardTimelinePertemuan.createScanAbsenPegawai(tbmuser, mobileAndroid);
				if (mi1.isVisible())
					toolbarbuttons.add(mi1);
				MyMenuitem mi2 = DashboardTimelinePertemuan.createScanQrCode(tbmuser, true, false);
				if (mi2.isVisible())
					toolbarbuttons.add(mi2);

				org.zkoss.zul.A rightButton = buatTombolProfilHeader();
				rowsub_el.appendChild(rightButton);
				Menupopup treeitem = new Menupopup();
				/* Dropdown profil modern: lihat css_utama.css blok "PROFIL DROPDOWN MODERN" */
				treeitem.setSclass("ais-profil-menu");
				treeitem.setParent(page.getFirstRoot());
				rightButton.setPopup(treeitem);

				treeitem.appendChild(buatItemIdentitasProfil(tbmuser));
				for (int i = toolbarbuttons.size() - 1; i >= 0; i--)
					treeitem.appendChild(toolbarbuttons.get(i));

				row = new MyFormRow();
				MainStyleHelper.applyTransparent(row);
				row.setParent(rows);
				usersOnline = new Toolbarbutton("Online user(s): 0");
				MainStyleHelper.setStyle(usersOnline, MainStyleHelper.ONLINE_MOBILE);
				MainStyleHelper.setSclass(usersOnline, "users_online_button");
				row.appendChild(usersOnline);
				usersOnline.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						onLihatOnline(arg0);
					}
				});

				List<MyToolbarbuttonKecilConfig> myToolbarbuttonKecilConfigs = new ArrayList<MyToolbarbuttonKecilConfig>();

				if (!langsungPengumuman) {
					MyToolbarbuttonKecilConfig ePengumumanButton = new MyToolbarbuttonKecilConfig("Pengumuman",
							"/img/svg/comment-2-text-line-white.svg");
					myToolbarbuttonKecilConfigs.add(ePengumumanButton);
					ePengumumanButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onPengumuman(arg0);
						}
					});
				}

				Tbmrole hakAkses = tbmuser.hakAkses();

				if (hakAkses.getEmedic()) {
					eMedicButton = new MyToolbarbuttonKecilConfig("eMedic", "/img/svg/hospital-white.svg");
					myToolbarbuttonKecilConfigs.add(eMedicButton);
					eMedicButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onEmedic(arg0);
						}
					});
				}
				if (hakAkses.getElearning()) {
					eLearningButton = new MyToolbarbuttonKecilConfig("e-Learning", "/img/svg/book-white.svg");
					myToolbarbuttonKecilConfigs.add(eLearningButton);
					eLearningButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onPengumumanPerkuliahan(arg0);
						}
					});
				}
				if (hakAkses.getKegiatanDanPrestasi()) {
					ePrestasiButton = new MyToolbarbuttonKecilConfig("Prestasi", "/img/svg/trophy-white.svg");
					myToolbarbuttonKecilConfigs.add(ePrestasiButton);
					ePrestasiButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onKegiatanDanPrestasi(arg0);
						}
					});
				}
				if (hakAkses.getPustaka()) {
					ePustakaButton = new MyToolbarbuttonKecilConfig("Pustaka", "/img/svg/books-thin-white.svg");
					myToolbarbuttonKecilConfigs.add(ePustakaButton);
					ePustakaButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onPustaka(arg0);
						}
					});
				}
				if (hakAkses.getWorkflow()) {
					eWorkflowButton = new MyToolbarbuttonKecilConfig("Workflow", "/img/svg/journal-arrow-up-white.svg");
					myToolbarbuttonKecilConfigs.add(eWorkflowButton);
					eWorkflowButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onWorkflow(arg0);
						}
					});
				}
				if (tampilkanShortcutRepositoryAntarJemputDiHeader() && hakAkses.getDasborRepository()) {
					eRepositoryButton = new MyToolbarbuttonKecilConfig("Repository", "/img/svg/folder2.svg");
					myToolbarbuttonKecilConfigs.add(eRepositoryButton);
					eRepositoryButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onRepository(arg0);
						}
					});
				}
				if (tampilkanShortcutRepositoryAntarJemputDiHeader() && hakAkses.getDasboardAntarJemput()) {
					eAntarJemputButton = new MyToolbarbuttonKecilConfig("Antar Jemput", "/img/svg/calendar2.svg");
					myToolbarbuttonKecilConfigs.add(eAntarJemputButton);
					eAntarJemputButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onAntarJemput(arg0);
						}
					});
				}
				if (hakAkses.getTampilkanSpmi()) {
					eSpmiButton = new MyToolbarbuttonKecilConfig("SPMI", "/img/svg/card-checklist-white.svg");
					myToolbarbuttonKecilConfigs.add(eSpmiButton);
					eSpmiButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onSpmi(arg0);
						}
					});
				}
				if (hakAkses.getKantin()) {
					eKantinButton = new MyToolbarbuttonKecilConfig("Toko", "/img/svg/basket3-white.svg");
					myToolbarbuttonKecilConfigs.add(eKantinButton);
					eKantinButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onKantin(arg0);
						}
					});
				}
				if (hakAkses.getTampilPos()) {
					ePosButton = new MyToolbarbuttonKecilConfig("POS", "/img/svg/cash-register-white.svg");
					myToolbarbuttonKecilConfigs.add(ePosButton);
					ePosButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onPos(arg0);
						}
					});
				}
				if (hakAkses.getDashboardKoperasi()) {
					eKoperasiButton = new MyToolbarbuttonKecilConfig("Koperasi", "/img/svg/money-bills-white.svg");
					myToolbarbuttonKecilConfigs.add(eKoperasiButton);
					eKoperasiButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onKoperasi(arg0);
						}
					});
				}
				if (hakAkses.getTampilkanGaji()) {
					eGajiButton = new MyToolbarbuttonKecilConfig("Gaji", "/img/svg/money-bills-white.svg");
					myToolbarbuttonKecilConfigs.add(eGajiButton);
					eGajiButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onGaji(arg0);
						}
					});
				}
				if (ais.common.Common.getApakahAdminBolehAksesFeeder()) {
					eFeederButton = new MyToolbarbuttonKecilConfig("Neo Feeder", "/img/svg/journal-arrow-up-white.svg");
					myToolbarbuttonKecilConfigs.add(eFeederButton);
					eFeederButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onSinkronisasiFeeder(arg0);
						}
					});
				}
				if (ais.common.Common.getApakahAdminBolehAksesSister()) {
					eSisterButton = new MyToolbarbuttonKecilConfig("Sister", "/img/svg/journal-arrow-up-white.svg");
					myToolbarbuttonKecilConfigs.add(eSisterButton);
					eSisterButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onSinkronisasiSister(arg0);
						}
					});
				}
				if (hakAkses.getDashboard()) {
					eAkademikButton = new MyToolbarbuttonKecilConfig("Dashboard", "/img/svg/dashboard-speed-white.svg");
					myToolbarbuttonKecilConfigs.add(eAkademikButton);
					eAkademikButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onDashboard(arg0);
						}
					});
				}
				if (hakAkses.getAdministrasi()) {
					eAdministrasiButton = new MyToolbarbuttonKecilConfig("Administrasi",
							"/img/svg/journal-bookmark-white.svg");
					myToolbarbuttonKecilConfigs.add(eAdministrasiButton);
					eAdministrasiButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onAdministrasi(arg0);
						}
					});
				}
				if (hakAkses.getPengadaan()) {
					ePengadaanButton = new MyToolbarbuttonKecilConfig("Pengadaan", "/img/svg/boxes-white.svg");
					myToolbarbuttonKecilConfigs.add(ePengadaanButton);
					ePengadaanButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onPengadaan(arg0);
						}
					});
				}
				if (hakAkses.getPembayaran()) {
					ePembayaranButton = new MyToolbarbuttonKecilConfig("Pembayaran", "/img/svg/payments-white.svg");
					myToolbarbuttonKecilConfigs.add(ePembayaranButton);
					ePembayaranButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onPembayaran(arg0);
						}
					});
				}
				if (hakAkses.getAkunting() && tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null) {
					eAkuntingButton = new MyToolbarbuttonKecilConfig("Akuntansi",
							"/img/svg/file-earmark-text-white.svg");
					myToolbarbuttonKecilConfigs.add(eAkuntingButton);
					eAkuntingButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onAkunting(arg0);
						}
					});
				}
				if (hakAkses.getKinerja()) {
					eKinerjaButton = new MyToolbarbuttonKecilConfig("Kinerja", "/img/svg/file-earmark-text-white.svg");
					myToolbarbuttonKecilConfigs.add(eKinerjaButton);
					eKinerjaButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onKinerja(arg0);
						}
					});
				}
				if (hakAkses.getKepegawaian()) {
					eKepegawaianButton = new MyToolbarbuttonKecilConfig("Kepegawaian", "/img/svg/user-tie-white.svg");
					myToolbarbuttonKecilConfigs.add(eKepegawaianButton);
					eKepegawaianButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onKepegawaian(arg0);
						}
					});
				}
				if (hakAkses.getKeuangan()) {
					eKeuanganButton = new MyToolbarbuttonKecilConfig("Keuangan", "/img/svg/money-bills-white.svg");
					myToolbarbuttonKecilConfigs.add(eKeuanganButton);
					eKeuanganButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onKeuangan(arg0);
						}
					});
				}
				if (hakAkses.getPresensiKehadiran()) {
					ePresensiButton = new MyToolbarbuttonKecilConfig("Presensi",
							"/img/svg/check-circled-outline-white.svg");
					myToolbarbuttonKecilConfigs.add(ePresensiButton);
					ePresensiButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onPresensi(arg0);
						}
					});
				}
				if (hakAkses.getKalenderAkademik()) {
					eKalenderAkademikButton = new MyToolbarbuttonKecilConfig("Kalender Akademik",
							"/img/svg/list-task-white.svg");
					myToolbarbuttonKecilConfigs.add(eKalenderAkademikButton);
					eKalenderAkademikButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onInformasiKalenderAkademik(arg0);
						}
					});
				}
				if (hakAkses.getInfoKegiatan()) {
					eInfoKegiatanButton = new MyToolbarbuttonKecilConfig("Info Kegiatan",
							"/img/svg/information-circle-outline-white.svg");
					myToolbarbuttonKecilConfigs.add(eInfoKegiatanButton);
					eInfoKegiatanButton.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							onInformasiKegiatan(arg0);
						}
					});
				}

				int tampilPerRow = 3;
				Grid grid2 = new Grid();
				MainStyleHelper.setSclass(grid2, "dgrid");
				grid2.setOddRowSclass("non-odd");
				MainStyleHelper.applyTransparent(grid2);
				grid2.setParent(headerHbox);
				Rows rowssub2 = new Rows();
				rowssub2.setParent(grid2);
				MyFormRow rowsub2 = new MyFormRow();
				MainStyleHelper.applyTransparent(rowsub2);

				int size = 0, jumlahRowBaru = 0;
				for (MyToolbarbuttonKecilConfig component : myToolbarbuttonKecilConfigs) {
					if (component.isVisible()) {
						if (size % tampilPerRow == 0) {
							rowsub2 = new MyFormRow();
							MainStyleHelper.applyTransparent(rowsub2);
							rowsub2.setParent(rowssub2);
							jumlahRowBaru++;
						}
						size++;
						MainStyleHelper.setStyle(component, MainStyleHelper.MOBILE_MODULE_BUTTON);
						component.setParent(rowsub2);
					}
				}
				myToolbarbuttonKecilConfigs.clear();

				if (jumlahRowBaru > 0) {
					row = new MyFormRow();
					MainStyleHelper.applyTransparent(row);
					row.setParent(rows);
					Groupbox groupbox = new Groupbox();
					groupbox.appendChild(grid2);
					groupbox.setParent(row);
					int tinggi = 230 + (jumlahRowBaru * 40);
					headerHbox.setHeight(tinggi + "px");
					grid.setHeight(tinggi + "px");
				} else {
					headerHbox.setHeight("230px");
					grid.setHeight("230px");
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

		} else if (headerHbox != null) {
			if ((mobile || mobileAndroid) && dropdownLayout) {
				MainStyleHelper.appendSclassOnce(headerHbox, "main-responsive-header-mobile-dropdown");
				MainStyleHelper.appendSclassOnce(rowHeader2, "main-responsive-module-row-mobile-dropdown");
				/* zscript index.zul menyembunyikan rowHeader & rowHeader2 saat mobile
				 * (warisan layout mobile legacy yang membangun header sendiri).
				 * Pada layout dropdown, header standar tetap dipakai sehingga
				 * keduanya harus ditampilkan kembali; tanpa ini header (logo,
				 * judul, tombol profil) hilang setelah refresh di tampilan mobile. */
				if (rowHeader != null) {
					rowHeader.setVisible(true);
				}
				if (rowHeader2 != null) {
					rowHeader2.setVisible(true);
				}
				if (eMenuButton != null) {
					eMenuButton.setVisible(true);
				}
				applyHeaderDropdownMenuLayout();
			}
			Common.clear(headerHboxButton);
			/* Penyelaras tombol kanan header: css_utama.css blok "HEADER KANAN RAPI". */
			MainStyleHelper.appendSclassOnce(headerHboxButton, "ais-header-kanan");
			/* Resolver memilih aset eCampus/eSchool dari GitHub Release terbaru. */
			if (desktopDownloadButton != null) {
				desktopDownloadButton.setVisible(!mobile && !mobileAndroid);
				if (desktopDownloadButton.isVisible()) {
					headerHboxButton.appendChild(desktopDownloadButton);
				}
			}
			/* Pemilih Bahasa (Indonesia/English/Arab) — paling kiri pada bilah kanan header. */
			try {
				headerHboxButton.appendChild(ais.ui.util.BahasaSwitchHelper.buatComboBahasa());
			} catch (Exception eLang) { ais.common.ErrorAuditUtil.record(eLang, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:3029");
			}
			notif(headerHboxButton);
			org.zkoss.zul.A rightButton = buatTombolProfilHeader();
			headerHboxButton.appendChild(rightButton);
			Menupopup treeitem = new Menupopup();
			/* Dropdown profil modern: styling kartu putih + responsif mobile
			 * ada di css_utama.css blok "PROFIL DROPDOWN MODERN" */
			treeitem.setSclass("ais-profil-menu");
			treeitem.setParent(page.getFirstRoot());
			rightButton.setPopup(treeitem);

			treeitem.appendChild(buatItemIdentitasProfil(tbmuser));

			MyMenuitem menuProfil = new MyMenuitem("Profil", "/img/svg/user-circle-thin.svg");
			menuProfil.addEventListener("onClick", new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					MainHelper.onUbahBiodata(tbmuser, foto);
				}
			});
			treeitem.appendChild(menuProfil);

			MyMenuitem mi1 = (MyMenuitem) DashboardTimelinePertemuan.createScanAbsenPegawai(tbmuser, mobileAndroid);
			if (mi1.isVisible())
				treeitem.appendChild(mi1);
			MyMenuitem mi2 = DashboardTimelinePertemuan.createScanQrCode(tbmuser, true, false);
			if (mi2.isVisible())
				treeitem.appendChild(mi2);

			MyMenuitem menuBantuan = new MyMenuitem("Bantuan", "/img/svg/question-circle.svg");
			menuBantuan.addEventListener("onClick", new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					MainHelper.onKatalogBantuan(login);
				}
			});
			treeitem.appendChild(menuBantuan);

			MyMenuitem menuVersi = new MyMenuitem("Versi Mobile", "/img/svg/android-logo-thin.svg");
			menuVersi.addEventListener("onClick", new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					MyWindow w = new MyWindow();
					w.setHeight("99%");
					w.setWidth("400px");
					w.setTitle("Aplikasi Versi Mobile");
					w.setBorder("none");
					w.setClosable(true);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(w);
					MainHelper.onDapatkanKode(w, true);
					w.setVisible(true);
					w.onModal();
				}
			});
			treeitem.appendChild(menuVersi);

			if (tbmroles.size() > 1) {
				MyMenuitem menuHak = new MyMenuitem("Ganti Hak Akses", "/img/svg/user-group.svg");
				menuHak.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						tampilPilihRole(tbmuser, tbmroles);
					}
				});
				treeitem.appendChild(menuHak);
			}

			MyMenuitem menuPass = new MyMenuitem("Password", "/img/svg/key.svg");
			menuPass.addEventListener("onClick", new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					ChangePasswordWindow w = new ChangePasswordWindow(false, true);
					w.setVisible(true);
					w.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					w.setHeight("450px");
					w.setWidth("600px");
					w.onModal();
				}
			});
			treeitem.appendChild(menuPass);

			if (tbmuser != null && tbmuser.getPegawai() != null
					&& Common.bolehKonfigurasi("tampilkan_slip_gaji_di_menu", Konfigurasi.TIDAK_AKTIF)) {
				MyMenuitem menuSlip = new MyMenuitem("Slip Gaji", "/img/svg/cash.svg");
				menuSlip.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						LaporanSlipGajiRealPegawaiPerOrang w = new LaporanSlipGajiRealPegawaiPerOrang(
								tbmuser.getPegawai());
						w.setVisible(true);
						w.setClosable(true);
						w.setTitle("Slip Gaji");
						w.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						w.setHeight("95%");
						w.setWidth("95%");
						w.onModal();
					}
				});
				treeitem.appendChild(menuSlip);
			}

			MyMenuitem menuKeluar = new MyMenuitem("Keluar", "/img/svg/power.svg");
			menuKeluar.setSclass("menu_item ais-profil-keluar");
			menuKeluar.addEventListener("onClick", new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					MainHelper.onKeluar(login);
				}
			});
			treeitem.appendChild(menuKeluar);
		}

		if (menubutton != null && Common.isMobile())
			menubutton.setVisible(false);

		Sessions.getCurrent().setAttribute("navigation", navigation);
		Sessions.getCurrent().setAttribute("mycenter", mycenter);
		Sessions.getCurrent().setAttribute("iframe", iframe);
		Sessions.getCurrent().setAttribute("langSession", "in");

		if (ConstantValues.AKTIF == null) {
			ConstantValues.hasbeeninit = false;
			ConstantValues.init();
		}

		refreshHeaderShortcutVisibility();

		if (popupmenu != null && iframe != null) {
			iframe.addEventListener("onClick", new EventListener() {
				public void onEvent(Event arg0) {
					closePopupQuietly(popupmenu);
				}
			});
		}

		if (foto != null) {
			try {
				foto.setSrc(CommonMedia.getUrlFotoPengguna(tbmuser, 90, 80));
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		konfigurasi = Common.checkKonfigurasiBigIcon();
		if (konfigurasi.getInfo1().equalsIgnoreCase("true")) {
			if (mycenter != null) {
				mycenter.detach();
				mycenter = null;
			}
		}

		session.setAttribute("usersTemp", tbmuser);

		String nama = tbmuser == null ? "" : tbmuser.getUserNama();
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& tbmuser.getDosen().getId() != null) {
			nama = tbmuser.ambilDosen().getNama() + " "
					+ (tbmuser.ambilDosen().getCode() != null && tbmuser.ambilDosen().getCode().equals("") ? ""
							: tbmuser.ambilDosen().getCode());
		}
		if (tbmuser != null && tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getId() != null) {
			nama = tbmuser.getMahasiswa().getNama() + " "
					+ (tbmuser.getMahasiswa().getNim() != null && tbmuser.getMahasiswa().getNim().equals("") ? ""
							: tbmuser.getMahasiswa().getNim());
		}

		String role = tbmuser == null || tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleName();
		String jurusan = tbmuser == null || tbmuser.ambilJurusan() == null ? "" : tbmuser.ambilJurusan().getNama();

		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")
				&& tbmuser.ambilDosen().getJurusan() != null) {
			role = "Dosen";
			jurusan = tbmuser.ambilDosen().getJurusan().getNama();
		}
		if (tbmuser != null && tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getJurusan() != null) {
			role = "Mahasiswa";
			jurusan = tbmuser.getMahasiswa().getJurusan().getNama();
			try {
				String hum = tbmuser.getMahasiswa().retreive("hasilUjianMahasiswa");
				HasilUjianMahasiswa.tampilkanUjianKembali(hum);
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			Common.createDefaultTimer(new EventListener() {
				public void onEvent(Event arg0) {
					// KE-2: auto-check tanpa proteksi -- koneksi DB transient (mis. diputus
					// administrator) sebelumnya lolos ke ZK EventProcessor & membanjiri log
					// admin. Lewati diam-diam bila transient, laporkan bila error nyata.
					try {
						PelanggaranMahasiswaAction.checkDanTampil(tbmuser.getMahasiswa());
					} catch (Exception e) {
						if (!Common.isTransientKoneksiError(e)) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			});
		}
		if (tbmuser != null && tbmuser.getSiswa() != null && tbmuser.getSiswa().getSekolah() != null) {
			role = "Siswa";
			jurusan = tbmuser.getSiswa().getSekolah().getNama();
			try {
				String hum = tbmuser.getSiswa().retreive("hasilUjianMahasiswa");
				HasilUjianMahasiswa.tampilkanUjianKembali(hum);
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			Common.createDefaultTimer(new EventListener() {
				public void onEvent(Event arg0) {
					// KE-2 (pola sama dgn checkDanTampil Mahasiswa di atas): lindungi dari
					// koneksi DB transient agar tak membanjiri log admin.
					try {
						PelanggaranSiswaAction.checkDanTampil(tbmuser.getSiswa());
					} catch (Exception e) {
						if (!Common.isTransientKoneksiError(e)) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			});
		}
		if (tbmuser != null && tbmuser.ambilGuru() != null && tbmuser.ambilGuru().getSekolah() != null) {
			role = "Guru";
			jurusan = tbmuser.ambilGuru().getSekolah().getNama();
		}

		if (loginInformation != null)
			loginInformation.setValue(nama + " (" + role + "), " + jurusan + " ");

		if (treeitemUtama != null) {
			if (navigasi != null)
				navigasi.detach();
			rowPencarian = Common.tampilanScroll1(treeitemUtama);
		} else {
			if (isHeaderDropdownMenuEnabled() && popupmenu != null) {
				popupmenu.setWidth((mobile || mobileAndroid) ? "92%" : "360px");
				popupmenu.setHeight((mobile || mobileAndroid) ? "82%" : "85%");
				MainStyleHelper.appendSclassOnce(popupmenu, "main-responsive-menu-popup");
				rowPencarian = Common.tampilanScroll1(popupmenu);
			} else if (!mobile) {
				rowPencarian = Common.tampilanScroll1(navigasi == null ? navigasicenter : navigasi);
			} else {
				if (popupmenu != null)
					popupmenu.setWidth("310px");
				rowPencarian = navigasicenter != null ? Common.tampilanScroll1(navigasicenter)
						: Common.tampilanScroll1(popupmenu);
			}
		}

		MainStyleHelper.applyTransparent(rowPencarian);
		MainStyleHelper.applyTransparent(rowPencarian.getGrid());

		rowDicari = new MyFormRow();
		MainStyleHelper.applyTransparent(rowDicari);
		rowDicari.setParent(rowPencarian.getParent());
		rowPengumuman = new MyFormRow();
		MainStyleHelper.applyTransparent(rowPengumuman);
		rowPengumuman.setParent(rowPencarian.getParent());

		initPencarianMenu();
		if (mycenter != null)
			mycenter.setVisible(true);

		Common.createDefaultTimer(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				initChatRoom();
				initPesan();
				updateUserOnline();
			}
		});

		if (execution.getParameter("tambah_akun") != null && execution.getParameter("tambah_akun").equals("true")) {
			Common.createDefaultTimer(new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					session.setAttribute("tambah_akun", "true");
					MainHelper.onUbahBiodata(tbmuser, foto);
				}
			});
		} else if (!ConstantValues.passwordKuat && Common.checkApakahMediaSosialSudahAda(tbmuser)) {
			Common.checkApakahPasswordSudahDiganti(tbmuser);
		}
		applyResponsiveShellEnhancement(page == null ? null : page.getFirstRoot());
	}

	private boolean renderModernHomeCenter(PerguruanTinggi ptAktif) {
		try {
			if (panel_home == null) {
				return false;
			}
			Common.clear(panel_home);

			/* Wrapper dengan padding agar konten tidak menempel ke tepi tabpanel. */
			Div wrapper = new Div();
			MainStyleHelper.applyHomeStack(wrapper);
			MainStyleHelper.setSclass(wrapper, "main-home-stack");
			wrapper.setParent(panel_home);

			/* Portal 2 kolom: kiri (kehadiran + pengumuman + kalender) dan kanan (profil).
			 * Kalender di kiri agar tidak tertekan jauh ke bawah oleh Profil yang panjang.
			 * Di layar sempit (<=760px) kolom menumpuk otomatis via CSS ais-ce-portallayout. */
			MyPortallayout portal = new MyPortallayout();
			portal.setWidth("100%");
			portal.setParent(wrapper);

			MyPortalchildren leftCol = new MyPortalchildren();
			leftCol.setWidth("62%");
			leftCol.setParent(portal);

			MyPortalchildren rightCol = new MyPortalchildren();
			rightCol.setWidth("38%");
			rightCol.setParent(portal);

			appendKehadiranPengajarPanel(leftCol);

			List<PengumumanAkademis> pengumumans = loadModernHomePengumuman(ptAktif);
			if (!pengumumans.isEmpty()) {
				appendModernPengumumanPanel(leftCol, pengumumans);
			}

			appendEmedicDashboardHomePanel(leftCol);

			appendKalenderAkademikPanel(leftCol);

			// Kehadiran sudah dirender di kolom kiri (appendKehadiranPengajarPanel) → tandai
			// agar panel profil TIDAK merender kehadiran lagi saat mobile (cegah tampil 2x).
			ais.action.master.PengumumanAkademisAction.tandaiKehadiranHomeDitampilkan(true);
			boolean adaProfil;
			try {
				adaProfil = appendModernProfilePanel(rightCol);
			} finally {
				ais.action.master.PengumumanAkademisAction.tandaiKehadiranHomeDitampilkan(false);
			}
			if (!adaProfil) {
				// profil dinonaktifkan → rightCol kosong, expand leftCol ke full
				rightCol.setParent(null);
				leftCol.setWidth("100%");
			}

			boolean adaPengumuman = !pengumumans.isEmpty();
			pengumumans.clear();
			return adaPengumuman;
		} catch (Exception e) {
			showError(e);
			try {
				if (panel_home != null) {
					ProfileAction.initProfile(tbmuser, panel_home, pengumumanAkademis);
				}
			} catch (Exception ex) {
				showError(ex);
			}
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private List<PengumumanAkademis> loadModernHomePengumuman(PerguruanTinggi ptAktif) {
		Session hibernateSession = null;
		try {
			hibernateSession = openLocalSession();
			Criteria criteria = TampilanPengumumanAkademisAction.initCriteriaStatic(true, tbmuser, ptAktif, null,
					hibernateSession);
			criteria.add(Restrictions.or(Restrictions.isNull("langsungMunculDiTab"),
					Restrictions.eq("langsungMunculDiTab", false)));
			int max = parsePx(Common.getKonfigurasi("jumlah_pengumuman_home_modern", "30").getNilai(), 30);
			if (max < 5) {
				max = 5;
			}
			if (max > 80) {
				max = 80;
			}
			return ConstantValues.simpleList(criteria.setMaxResults(max), PengumumanAkademis.class);
		} catch (Exception e) {
			showError(e);
		} finally {
			closeNativeSessionQuietly(hibernateSession);
		}
		return new ArrayList<PengumumanAkademis>();
	}

	/**
	 * Panel informasi kehadiran dosen/guru hari ini di atas papan pengumuman.
	 * Sumber data sama dengan halaman /hadir (HadirAction.onInfo):
	 * PengumumanAkademisAction.tampilkanKehadiranDosen / tampilkanKehadiranGuru.
	 */
	private void appendKehadiranPengajarPanel(Component parent) {
		if (parent == null) {
			return;
		}
		try {
			ais.database.model.sekolah.Sekolah sekolahAktif = SekolahUtil.getSekolah();
			boolean mobile = Common.isMobile();
			String htmlKehadiran = sekolahAktif == null || sekolahAktif.getId() == null
					? ais.action.master.PengumumanAkademisAction.tampilkanKehadiranDosen(tbmuser, mobile)
					: ais.action.master.PengumumanAkademisAction.tampilkanKehadiranGuru(tbmuser, mobile);
			if (htmlKehadiran == null || htmlKehadiran.trim().length() == 0) {
				return;
			}

			Div panel = new Div();
			MainStyleHelper.setSclass(panel, "main-announcement-panel main-teacher-presence-panel");
			MainStyleHelper.applyAnnouncementPanel(panel);
			panel.setParent(parent);

			Div hero = new Div();
			MainStyleHelper.setSclass(hero, "main-announcement-hero main-teacher-presence-hero");
			hero.setParent(panel);

			Div heroText = new Div();
			MainStyleHelper.setSclass(heroText, "main-announcement-hero-text");
			heroText.setParent(hero);

			Label eyebrow = new Label(Common.getBahasaConfig("Informasi Hari Ini"));
			MainStyleHelper.setSclass(eyebrow, "main-announcement-eyebrow");
			eyebrow.setParent(heroText);

			Label judulPanel = new Label(Common.getBahasaConfig(sekolahAktif == null || sekolahAktif.getId() == null
					? "Kehadiran Dosen" : "Kehadiran Guru"));
			MainStyleHelper.setSclass(judulPanel, "main-announcement-title");
			judulPanel.setParent(heroText);

			Label hadirDesc = new Label(Common.getBahasaConfig(sekolahAktif == null || sekolahAktif.getId() == null
					? "Siapa saja dosen yang hadir hari ini dan sedang mengajar di kelas."
					: "Siapa saja guru yang hadir hari ini dan sedang mengajar di kelas."));
			MainStyleHelper.setSclass(hadirDesc, "main-announcement-subtitle");
			hadirDesc.setParent(heroText);

			Div body = new Div();
			MainStyleHelper.setSclass(body, "main-teacher-presence-body");
			body.setParent(panel);

			org.zkoss.zul.Html htmlComponent = new org.zkoss.zul.Html(htmlKehadiran);
			htmlComponent.setParent(body);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Panel "Dasbor eMedic" di bawah Papan Pengumuman pada tab Home, dikendalikan oleh
	 * konfigurasi {@code home_tampilkan_dashboard_emedic} (default TIDAK AKTIF via
	 * {@link Konfigurasi#TIDAK_AKTIF} — lihat {@code KonfigurasiNewAction.initTabPortalHalamanDepan}).
	 *
	 * <p>Konfigurasi ini TERPISAH dari {@code Tbmrole.emedic} yang menggerbangi tombol "eMedic"
	 * pada header (lihat {@code onEmedic}/{@code eMedicButton}): menonaktifkan konfigurasi ini
	 * hanya menghilangkan dashboard yang tertanam otomatis di tab Home, tombol header tetap
	 * tampil/tersembunyi sesuai hak akses role seperti biasa.</p>
	 *
	 * <p>{@link ais.action.master.dashboard.sirs.DashboardSirsKomprehensif} dipasang langsung
	 * sebagai komponen anak (bukan lewat MyInclude/iframe) — kelas tersebut memiliki konstruktor
	 * tanpa parameter yang membangun seluruh isinya sendiri (grid, chart, dsb) via {@code init()},
	 * sehingga cukup di-{@code setParent(...)} ke container di sini.</p>
	 */
	private void appendEmedicDashboardHomePanel(Component parent) {
		if (parent == null) {
			return;
		}
		try {
			if (!Common.bolehKonfigurasi("home_tampilkan_dashboard_emedic", Konfigurasi.TIDAK_AKTIF)) {
				return;
			}

			Div panel = new Div();
			MainStyleHelper.setSclass(panel, "main-announcement-panel main-emedic-home-panel");
			MainStyleHelper.applyAnnouncementPanel(panel);
			panel.setParent(parent);

			Div hero = new Div();
			MainStyleHelper.setSclass(hero, "main-announcement-hero");
			hero.setParent(panel);

			Div heroText = new Div();
			MainStyleHelper.setSclass(heroText, "main-announcement-hero-text");
			heroText.setParent(hero);

			Label eyebrow = new Label(Common.getBahasaConfig("Rumah Sakit / Klinik"));
			MainStyleHelper.setSclass(eyebrow, "main-announcement-eyebrow");
			eyebrow.setParent(heroText);

			Label title = new Label(Common.getBahasaConfig("Dasbor eMedic"));
			MainStyleHelper.setSclass(title, "main-announcement-title");
			title.setParent(heroText);

			Label subtitle = new Label(Common.getBahasaConfig(
					"Ringkasan layanan rumah sakit/klinik: pendaftaran, kunjungan, pendapatan, diagnosa terbanyak, okupansi tempat tidur, dan kadaluarsa farmasi."));
			MainStyleHelper.setSclass(subtitle, "main-announcement-subtitle");
			subtitle.setParent(heroText);

			Div body = new Div();
			MainStyleHelper.setSclass(body, "main-emedic-home-body");
			body.setParent(panel);

			ais.action.master.dashboard.sirs.DashboardSirsKomprehensif dashboard =
					new ais.action.master.dashboard.sirs.DashboardSirsKomprehensif();
			dashboard.setParent(body);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Panel "Kalender Akademik" di bawah papan pengumuman Home.
	 * Sumber data & filternya mengikuti
	 * WEB-INF/baru/modul/home/admin/_kalender_akademik.jsp: KalenderAkademik
	 * aktif, difilter hierarki jurusan/fakultas/program/sekolah/yayasan milik
	 * user; layout kartu (blok tanggal + badge TA/semester + deskripsi +
	 * rentang tanggal) juga meniru JSP tersebut (CSS: blok
	 * "KALENDER AKADEMIK HOME" di css_utama.css).
	 */
	private void appendKalenderAkademikPanel(Component parent) {
		if (parent == null) {
			return;
		}
		try {
			/* Cek ada tidaknya data kalender sebelum membuat panel.
			 * Jika tidak ada satu pun agenda (semua tahun, semua semester),
			 * panel ini tidak ditampilkan sama sekali. */
			List<ais.database.model.KalenderAkademik> cekData = loadKalenderAkademikHome("", "");
			boolean adaData = !cekData.isEmpty();
			cekData.clear();
			if (!adaData) {
				return;
			}

			/* Data dimuat oleh muatUlangKalender di bawah sesuai filter terpilih. */
			List<ais.database.model.KalenderAkademik> listKegiatan = new ArrayList<ais.database.model.KalenderAkademik>();

			Div panel = new Div();
			MainStyleHelper.setSclass(panel, "main-announcement-panel main-kalender-akademik-panel");
			MainStyleHelper.applyAnnouncementPanel(panel);
			panel.setParent(parent);

			Div hero = new Div();
			MainStyleHelper.setSclass(hero, "main-announcement-hero main-kalender-akademik-hero");
			hero.setParent(panel);

			Div heroText = new Div();
			MainStyleHelper.setSclass(heroText, "main-announcement-hero-text");
			heroText.setParent(hero);

			Label eyebrow = new Label(ais.common.Common.getBahasaConfig("Agenda Kampus"));
			MainStyleHelper.setSclass(eyebrow, "main-announcement-eyebrow");
			eyebrow.setParent(heroText);

			Label judulPanel = new Label(ais.common.Common.getBahasaConfig("Kalender Akademik"));
			MainStyleHelper.setSclass(judulPanel, "main-announcement-title");
			judulPanel.setParent(heroText);

			Label kalDesc = new Label("Semua jadwal kegiatan penting kampus — pilih tahun/semester untuk melihat agenda yang diinginkan.");
			MainStyleHelper.setSclass(kalDesc, "main-announcement-subtitle");
			kalDesc.setParent(heroText);

			final Label jumlah = new Label(listKegiatan.size() + " agenda");
			MainStyleHelper.setSclass(jumlah, "main-announcement-count");
			jumlah.setParent(hero);

			/*
			 * Baris filter — meniru kalender_akademik.jsp: dropdown Tahun Akademik
			 * (default tahun akademik berjalan, opsi "Semua Tahun") ditambah dropdown
			 * Semester (default "Semua Semester"). Mengubah salah satunya langsung
			 * memuat ulang daftar agenda tanpa reload halaman.
			 */
			Div filterBar = new Div();
			MainStyleHelper.setSclass(filterBar, "main-announcement-search-panel main-kalender-filter-baris");
			MainStyleHelper.applyAnnouncementSearchPanel(filterBar);
			filterBar.setParent(panel);

			Label lblTahun = new Label(ais.common.Common.getBahasaConfig("Tahun:"));
			MainStyleHelper.setSclass(lblTahun, "main-kalender-filter-label");
			lblTahun.setParent(filterBar);

			final Combobox cmbTahun = new Combobox();
			cmbTahun.setReadonly(true);
			cmbTahun.setWidth("140px");
			cmbTahun.setTooltiptext("Saring agenda berdasarkan tahun akademik");
			MainStyleHelper.setSclass(cmbTahun, "main-kalender-filter-combo");
			cmbTahun.setParent(filterBar);
			Comboitem itemSemuaTahun = new Comboitem("Semua Tahun");
			itemSemuaTahun.setValue("");
			itemSemuaTahun.setParent(cmbTahun);
			String tahunBerjalan = "";
			try {
				tahunBerjalan = Common.getCurrentTahunAkademik();
			} catch (Exception e) {
				tahunBerjalan = "";
			}
			Comboitem itemTahunTerpilih = itemSemuaTahun;
			if (Common.tahunAngkatans != null) {
				for (String ta : Common.tahunAngkatans) {
					if (ta == null || ta.trim().length() == 0) {
						continue;
					}
					Comboitem item = new Comboitem(ta);
					item.setValue(ta);
					item.setParent(cmbTahun);
					if (ta.equals(tahunBerjalan)) {
						itemTahunTerpilih = item;
					}
				}
			}
			cmbTahun.setSelectedItem(itemTahunTerpilih);

			Label lblSemester = new Label(ais.common.Common.getBahasaConfig("Semester:"));
			MainStyleHelper.setSclass(lblSemester, "main-kalender-filter-label");
			lblSemester.setParent(filterBar);

			final Combobox cmbSemester = new Combobox();
			cmbSemester.setReadonly(true);
			cmbSemester.setWidth("150px");
			cmbSemester.setTooltiptext("Saring agenda berdasarkan semester ganjil/genap");
			MainStyleHelper.setSclass(cmbSemester, "main-kalender-filter-combo");
			cmbSemester.setParent(filterBar);
			Comboitem itemSemuaSemester = new Comboitem("Semua Semester");
			itemSemuaSemester.setValue("");
			itemSemuaSemester.setParent(cmbSemester);
			String[] pilihanSemester = new String[] { "Ganjil", "Genap" };
			for (int i = 0; i < pilihanSemester.length; i++) {
				Comboitem item = new Comboitem(pilihanSemester[i]);
				item.setValue(pilihanSemester[i]);
				item.setParent(cmbSemester);
			}
			cmbSemester.setSelectedItem(itemSemuaSemester);

			final Div body = new Div();
			MainStyleHelper.setSclass(body, "main-kalender-akademik-body");
			body.setParent(panel);

			final EventListener muatUlangKalender = new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					String ta = cmbTahun.getSelectedItem() == null ? ""
							: String.valueOf(cmbTahun.getSelectedItem().getValue());
					String smt = cmbSemester.getSelectedItem() == null ? ""
							: String.valueOf(cmbSemester.getSelectedItem().getValue());
					List<ais.database.model.KalenderAkademik> data = loadKalenderAkademikHome(ta, smt);
					jumlah.setValue(data.size() + " agenda");
					Common.clear(body);
					new org.zkoss.zul.Html(buatHtmlKalenderAkademik(data)).setParent(body);
					data.clear();
				}
			};
			cmbTahun.addEventListener("onChange", muatUlangKalender);
			cmbTahun.addEventListener("onSelect", muatUlangKalender);
			cmbSemester.addEventListener("onChange", muatUlangKalender);
			cmbSemester.addEventListener("onSelect", muatUlangKalender);

			listKegiatan.clear();
			muatUlangKalender.onEvent(null);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * @param tahunAjaran filter tahun akademik; kosong = semua tahun. Mengikuti
	 *        _kalender_akademik.jsp: agenda tanpa tahun ajaran tetap ikut tampil.
	 * @param semester filter Ganjil/Genap; kosong = semua semester.
	 */
	@SuppressWarnings("unchecked")
	private List<ais.database.model.KalenderAkademik> loadKalenderAkademikHome(String tahunAjaran, String semester) {
		Session hibernateSession = null;
		try {
			hibernateSession = openLocalSession();
			Criteria c = hibernateSession.createCriteria(ais.database.model.KalenderAkademik.class);
			c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

			if (tahunAjaran != null && tahunAjaran.trim().length() > 0) {
				c.add(Restrictions.or(Restrictions.isNull("tahunAjaran"),
						Restrictions.eq("tahunAjaran", tahunAjaran.trim())));
			}
			if (semester != null && semester.trim().length() > 0) {
				c.add(Restrictions.ilike("ganjilGenap", semester.trim(), MatchMode.EXACT));
			}

			ais.database.model.Jurusan jurusan = tbmuser == null ? null : tbmuser.ambilJurusan();
			ais.database.model.Fakultas fakultas = tbmuser == null ? null : tbmuser.ambilFakultas();
			ais.database.model.Program program = tbmuser == null ? null : tbmuser.ambilProgram();
			ais.database.model.sekolah.Sekolah sekolahUser = tbmuser == null ? null : tbmuser.ambilSekolah();
			ais.database.model.sekolah.Yayasan yayasanUser = tbmuser == null ? null : tbmuser.ambilYayasan();

			if (jurusan != null) {
				c.add(Restrictions.or(Restrictions.isNull("jurusan"), Restrictions.eq("jurusan", jurusan)));
			}
			if (fakultas != null) {
				c.add(Restrictions.or(Restrictions.isNull("fakultas"), Restrictions.eq("fakultas", fakultas)));
			}
			if (program != null) {
				c.add(Restrictions.or(Restrictions.isNull("program"), Restrictions.eq("program", program.getNama())));
			}
			if (sekolahUser != null) {
				c.add(Restrictions.eq("sekolah", sekolahUser));
			}
			if (yayasanUser != null) {
				c.add(Restrictions.eq("yayasan", yayasanUser));
			}

			c.addOrder(org.hibernate.criterion.Order.desc("tanggalMulai"))
					.addOrder(org.hibernate.criterion.Order.desc("tanggalSelesai"));

			int max = parsePx(Common.getKonfigurasi("jumlah_kalender_akademik_home", "12").getNilai(), 12);
			if (max < 3) {
				max = 3;
			}
			if (max > 60) {
				max = 60;
			}
			return filterInstanceOfKalenderAkademik(
					ConstantValues.simpleList(c.setMaxResults(max), ais.database.model.KalenderAkademik.class));
		} catch (Exception e) {
			showError(e);
		} finally {
			closeNativeSessionQuietly(hibernateSession);
		}
		return new ArrayList<ais.database.model.KalenderAkademik>();
	}

	/**
	 * PERBAIKAN (ClassCastException Long->KalenderAkademik): ConstantValues.simpleList(...)
	 * bisa mengembalikan List mentah yang isinya id (Long) alih-alih entity utuh apabila
	 * cache MemoryCacheUtil untuk kelas ini tidak konsisten. Menyaring elemen di sini
	 * (raw iteration, tanpa checkcast generic langsung) mencegah ClassCastException di
	 * caller (mis. perulangan for-each pada buatHtmlKalenderAkademik).
	 */
	@SuppressWarnings("rawtypes")
	private static List<ais.database.model.KalenderAkademik> filterInstanceOfKalenderAkademik(List rawList) {
		List<ais.database.model.KalenderAkademik> hasil = new ArrayList<ais.database.model.KalenderAkademik>();
		if (rawList == null) {
			return hasil;
		}
		for (Object o : rawList) {
			if (o instanceof ais.database.model.KalenderAkademik) {
				hasil.add((ais.database.model.KalenderAkademik) o);
			}
		}
		return hasil;
	}

	private static String escapeHtmlKalender(String s) {
		return s == null ? ""
				: s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private String buatHtmlKalenderAkademik(List<ais.database.model.KalenderAkademik> listKegiatan) {
		java.util.Locale idn = new java.util.Locale("id", "ID");
		java.text.SimpleDateFormat fmtBulan = new java.text.SimpleDateFormat("MMM", idn);
		java.text.SimpleDateFormat fmtHari = new java.text.SimpleDateFormat("dd", idn);
		java.text.SimpleDateFormat fmtFull = new java.text.SimpleDateFormat("dd MMM yyyy", idn);

		StringBuilder html = new StringBuilder();
		html.append("<div class=\"main-kalender-grid\">");
		if (listKegiatan == null || listKegiatan.isEmpty()) {
			html.append("<div class=\"main-kalender-kosong\">")
					.append(escapeHtmlKalender(
							Common.getBahasaConfig("Tidak ada agenda akademik untuk tahun ajaran ini.")))
					.append("</div>");
		} else {
			for (ais.database.model.KalenderAkademik k : listKegiatan) {
				if (k == null) {
					continue;
				}
				String bln = k.getTanggalMulai() == null ? "-" : fmtBulan.format(k.getTanggalMulai()).toUpperCase();
				String tgl = k.getTanggalMulai() == null ? "-" : fmtHari.format(k.getTanggalMulai());
				String rentang = (k.getTanggalMulai() == null ? "" : fmtFull.format(k.getTanggalMulai())) + " s/d "
						+ (k.getTanggalSelesai() == null ? "" : fmtFull.format(k.getTanggalSelesai()));
				String smt = k.getGanjilGenap() == null ? "" : k.getGanjilGenap().trim();
				String smtClass = smt.equalsIgnoreCase("Ganjil") ? "main-kalender-badge-ganjil"
						: "main-kalender-badge-genap";

				html.append("<div class=\"main-kalender-card\">");
				html.append("<div class=\"main-kalender-tanggal\"><span class=\"main-kalender-bulan\">")
						.append(escapeHtmlKalender(bln)).append("</span><span class=\"main-kalender-hari\">")
						.append(escapeHtmlKalender(tgl)).append("</span></div>");
				html.append("<div class=\"main-kalender-isi\">");
				html.append("<div class=\"main-kalender-judul\">")
						.append(escapeHtmlKalender(k.getNamaKegiatanAkademik())).append("</div>");
				html.append("<div class=\"main-kalender-badge-baris\">");
				html.append("<span class=\"main-kalender-badge main-kalender-badge-ta\">")
						.append(escapeHtmlKalender(k.getTahunAjaran() == null ? "-" : k.getTahunAjaran()))
						.append("</span>");
				if (!smt.isEmpty()) {
					html.append("<span class=\"main-kalender-badge ").append(smtClass).append("\">")
							.append(escapeHtmlKalender(smt)).append("</span>");
				}
				html.append("</div>");
				html.append("<div class=\"main-kalender-deskripsi\">")
						.append(escapeHtmlKalender(k.getDeskripsiKegiatanAkademik() == null ? "-"
								: k.getDeskripsiKegiatanAkademik()))
						.append("</div>");
				html.append("<div class=\"main-kalender-rentang\">").append(escapeHtmlKalender(rentang))
						.append("</div>");
				html.append("</div></div>");
			}
		}
		html.append("</div>");
		return html.toString();
	}

	private void appendModernPengumumanPanel(Component parent, List<PengumumanAkademis> pengumumans) {
		if (parent == null || pengumumans == null || pengumumans.isEmpty()) {
			return;
		}

		final List<PengumumanAkademis> dataPengumuman = new ArrayList<PengumumanAkademis>();
		for (PengumumanAkademis item : pengumumans) {
			if (item != null && item.getId() != null) {
				dataPengumuman.add(item);
			}
		}
		if (dataPengumuman.isEmpty()) {
			return;
		}

		final int[] halaman = new int[] { 0 };

		Div panel = new Div();
		MainStyleHelper.setSclass(panel, "main-announcement-panel main-announcement-board-panel main-announcement-modern-v7");
		MainStyleHelper.applyAnnouncementPanel(panel);
		panel.setParent(parent);

		Div hero = new Div();
		MainStyleHelper.setSclass(hero, "main-announcement-hero");
		hero.setParent(panel);

		Div headerText = new Div();
		MainStyleHelper.setSclass(headerText, "main-announcement-hero-text");
		headerText.setParent(hero);

		Label eyebrow = new Label(ais.common.Common.getBahasaConfig("Informasi Kampus"));
		MainStyleHelper.setSclass(eyebrow, "main-announcement-eyebrow");
		eyebrow.setParent(headerText);

		Label title = new Label(ais.common.Common.getBahasaConfig("Papan Pengumuman"));
		MainStyleHelper.setSclass(title, "main-announcement-title main-announcement-board-title");
		title.setParent(headerText);

		/*
		 * Header meniru WEB-INF/baru/modul/home/pengumuman.jsp: judul di kiri,
		 * sisi kanan berisi badge jumlah + kolom cari compact (input dan tombol
		 * menyatu ala input-group Bootstrap). Subtitle panjang, blok pencarian
		 * terpisah, dan tombol Reset dihilangkan agar komponen tidak bertumpuk;
		 * petunjuk pencarian dipindahkan ke tooltip, dan mengosongkan kotak cari
		 * otomatis menampilkan semua data kembali (onChange).
		 * Penataan visual: css_utama.css blok "PAPAN PENGUMUMAN GAYA JSP".
		 */
		Div heroRight = new Div();
		MainStyleHelper.setSclass(heroRight, "main-announcement-hero-right");
		heroRight.setParent(hero);

		final Label count = new Label(dataPengumuman.size() + " item");
		MainStyleHelper.setSclass(count, "main-announcement-count main-announcement-board-count");
		count.setParent(heroRight);

		Div searchGroup = new Div();
		MainStyleHelper.setSclass(searchGroup, "main-announcement-search-group");
		searchGroup.setParent(heroRight);

		final Textbox keyword = new Textbox();
		keyword.setTooltiptext("Cari judul, kategori, tanggal, sasaran, atau isi pengumuman. Kosongkan untuk menampilkan semua.");
		MainStyleHelper.setSclass(keyword, "main-announcement-search-input main-announcement-search-group-input");
		keyword.setParent(searchGroup);

		Toolbarbutton cari = new MyToolbarbuttonConfig("Cari");
		MainStyleHelper.setSclass(cari, "main-announcement-search-button main-announcement-search-primary main-announcement-search-group-button");
		cari.setParent(searchGroup);

		Div list = new Div();
		MainStyleHelper.setSclass(list, "main-announcement-list main-announcement-board-list");
		list.setParent(panel);

		final Component announcementContainer = list;

		final EventListener renderListener = new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				halaman[0] = 0;
				renderModernPengumumanPage(announcementContainer, count, dataPengumuman, keyword, halaman);
			}
		};
		cari.addEventListener("onClick", renderListener);
		keyword.addEventListener("onOK", renderListener);
		keyword.addEventListener("onChange", renderListener);

		renderModernPengumumanPage(announcementContainer, count, dataPengumuman, keyword, halaman);
	}

	private void renderModernPengumumanPage(final Component container, final Label count, final List<PengumumanAkademis> dataPengumuman,
			final Textbox keyword, final int[] halaman) {
		if (container == null) {
			return;
		}
		Common.clear(container);
		List<PengumumanAkademis> filtered = filterModernPengumuman(dataPengumuman, keyword == null ? "" : keyword.getValue());
		List<PengumumanAkademis> ordered = orderModernPengumuman(filtered);
		int total = ordered.size();
		int totalHalaman = total <= 0 ? 1 : ((total + MODERN_PENGUMUMAN_PAGE_SIZE - 1) / MODERN_PENGUMUMAN_PAGE_SIZE);
		if (halaman[0] < 0) halaman[0] = 0;
		if (halaman[0] >= totalHalaman) halaman[0] = totalHalaman - 1;
		if (count != null) {
			count.setValue(total + " dari " + dataPengumuman.size() + " item | Halaman " + (halaman[0] + 1) + "/" + totalHalaman);
		}
		if (total == 0) {
			Div emptyBox = new Div();
			MainStyleHelper.setSclass(emptyBox, "main-announcement-empty-box");
			emptyBox.setParent(container);
			Label empty = new Label(ais.common.Common.getBahasaConfig("Pengumuman tidak ditemukan. Coba gunakan kata kunci lain."));
			MainStyleHelper.setSclass(empty, "main-announcement-empty");
			MainStyleHelper.applyAnnouncementEmpty(empty);
			empty.setParent(emptyBox);
			return;
		}
		int mulai = halaman[0] * MODERN_PENGUMUMAN_PAGE_SIZE;
		int sampai = mulai + MODERN_PENGUMUMAN_PAGE_SIZE;
		if (sampai > total) sampai = total;
		List<PengumumanAkademis> pageItems = new ArrayList<PengumumanAkademis>();
		for (int i = mulai; i < sampai; i++) pageItems.add(ordered.get(i));
		List<PengumumanAkademis> utama = new ArrayList<PengumumanAkademis>();
		Map<String, List<PengumumanAkademis>> lain = new LinkedHashMap<String, List<PengumumanAkademis>>();
		for (PengumumanAkademis item : pageItems) {
			if (isPengumumanUtama(item)) { utama.add(item); } else {
				String kategori = getKategoriPengumumanLabel(item);
				List<PengumumanAkademis> list = lain.get(kategori);
				if (list == null) { list = new ArrayList<PengumumanAkademis>(); lain.put(kategori, list); }
				list.add(item);
			}
		}
		if (!utama.isEmpty()) appendModernPengumumanGroup(container, "Prioritas Utama", utama, true);
		for (Map.Entry<String, List<PengumumanAkademis>> entry : lain.entrySet()) appendModernPengumumanGroup(container, entry.getKey(), entry.getValue(), false);
		appendModernPengumumanPager(container, count, dataPengumuman, keyword, halaman, totalHalaman);
		filtered.clear(); ordered.clear(); pageItems.clear(); utama.clear(); lain.clear();
	}

	private List<PengumumanAkademis> filterModernPengumuman(List<PengumumanAkademis> items, String keyword) {
		List<PengumumanAkademis> result = new ArrayList<PengumumanAkademis>();
		String query = normalizeSearchText(keyword);
		String[] tokens = query.length() == 0 ? new String[0] : query.split("\\s+");
		if (items == null) return result;
		for (PengumumanAkademis item : items) {
			if (item == null || item.getId() == null) continue;
			String text = normalizeSearchText(safeTitle(item) + " " + getKategoriPengumumanLabel(item) + " " + safeText(item.getDiperuntukkan(), "") + " " + formatTanggalPengumuman(item) + " " + stripHtml(item.getCatatan()));
			boolean cocok = true;
			for (int i = 0; i < tokens.length; i++) if (tokens[i].length() > 0 && text.indexOf(tokens[i]) < 0) { cocok = false; break; }
			if (cocok) result.add(item);
		}
		return result;
	}

	private String normalizeSearchText(String value) {
		return safeText(value, "").toLowerCase().replaceAll("[^a-z0-9\\u00c0-\\u024f\\u1e00-\\u1eff]+", " ").replaceAll("\\s+", " ").trim();
	}

	private List<PengumumanAkademis> orderModernPengumuman(List<PengumumanAkademis> items) {
		List<PengumumanAkademis> result = new ArrayList<PengumumanAkademis>();
		Map<String, List<PengumumanAkademis>> perKategori = new LinkedHashMap<String, List<PengumumanAkademis>>();
		if (items == null) return result;
		for (PengumumanAkademis item : items) {
			if (isPengumumanUtama(item)) result.add(item); else {
				String kategori = getKategoriPengumumanLabel(item);
				List<PengumumanAkademis> list = perKategori.get(kategori);
				if (list == null) { list = new ArrayList<PengumumanAkademis>(); perKategori.put(kategori, list); }
				list.add(item);
			}
		}
		for (Map.Entry<String, List<PengumumanAkademis>> entry : perKategori.entrySet()) result.addAll(entry.getValue());
		perKategori.clear();
		return result;
	}

	private void appendModernPengumumanPager(final Component container, final Label count, final List<PengumumanAkademis> dataPengumuman,
			final Textbox keyword, final int[] halaman, final int totalHalaman) {
		if (container == null || totalHalaman <= 1) return;
		Hbox pager = new Hbox();
		pager.setWidth("100%");
		pager.setAlign("center");
		pager.setPack("center");
		MainStyleHelper.setSclass(pager, "main-announcement-pager");
		pager.setParent(container);
		Toolbarbutton prev = new MyToolbarbuttonConfig("Sebelumnya");
		MainStyleHelper.setSclass(prev, "main-announcement-page-button");
		prev.setDisabled(halaman[0] <= 0);
		prev.setParent(pager);
		prev.addEventListener("onClick", new EventListener() { public void onEvent(Event e) throws Exception { halaman[0]--; renderModernPengumumanPage(container, count, dataPengumuman, keyword, halaman); } });
		int awal = halaman[0] - 2; if (awal < 0) awal = 0;
		int akhir = awal + 5; if (akhir > totalHalaman) { akhir = totalHalaman; awal = akhir - 5; if (awal < 0) awal = 0; }
		for (int i = awal; i < akhir; i++) {
			final int idx = i;
			Toolbarbutton page = new MyToolbarbuttonConfig(String.valueOf(i + 1));
			MainStyleHelper.setSclass(page, i == halaman[0] ? "main-announcement-page-button main-announcement-page-active" : "main-announcement-page-button");
			page.setDisabled(i == halaman[0]);
			page.setParent(pager);
			page.addEventListener("onClick", new EventListener() { public void onEvent(Event e) throws Exception { halaman[0] = idx; renderModernPengumumanPage(container, count, dataPengumuman, keyword, halaman); } });
		}
		Toolbarbutton next = new MyToolbarbuttonConfig("Berikutnya");
		MainStyleHelper.setSclass(next, "main-announcement-page-button");
		next.setDisabled(halaman[0] >= totalHalaman - 1);
		next.setParent(pager);
		next.addEventListener("onClick", new EventListener() { public void onEvent(Event e) throws Exception { halaman[0]++; renderModernPengumumanPage(container, count, dataPengumuman, keyword, halaman); } });
	}


	private void appendModernPengumumanGroup(final Component container, String judulGroup,
			final List<PengumumanAkademis> items, final boolean utama) {
		if (container == null || items == null || items.isEmpty()) {
			return;
		}

		String toneClass = getPengumumanToneClass(items.get(0), utama);
		Div groupBox = new Div();
		MainStyleHelper.setSclass(groupBox, (utama ? "main-announcement-group main-announcement-group-primary"
				: "main-announcement-group") + " " + toneClass);
		MainStyleHelper.applyAnnouncementGroupBox(groupBox);
		groupBox.setParent(container);

		Label title = new Label(Common.terjemahDinamis(safeText(judulGroup, "Pengumuman")) + " (" + items.size() + ")");
		MainStyleHelper.applyAnnouncementGroupTitle(title);
		title.setParent(groupBox);

		for (int i = 0; i < items.size(); i++) {
			final PengumumanAkademis item = items.get(i);
			if (item == null || item.getId() == null) {
				continue;
			}
			appendModernPengumumanCard(container, item, utama);
		}
	}

	private void appendModernPengumumanCard(final Component container, final PengumumanAkademis item, final boolean utama) {
		if (container == null || item == null || item.getId() == null) {
			return;
		}

		String toneClass = getPengumumanToneClass(item, utama);
		Div card = new Div();
		MainStyleHelper.setSclass(card, (utama ? "main-announcement-card main-announcement-card-primary main-announcement-board-card"
				: "main-announcement-card main-announcement-board-card") + " " + toneClass);
		MainStyleHelper.applyAnnouncementCard(card);
		card.setParent(container);

		Div meta = new Div();
		MainStyleHelper.setSclass(meta, "main-announcement-meta");
		MainStyleHelper.applyAnnouncementMeta(meta);
		meta.setParent(card);

		appendModernPengumumanChip(meta, utama ? Common.terjemahDinamis("Prioritas") : getKategoriPengumumanLabel(item),
				utama);
		String tanggal = formatTanggalPengumuman(item);
		if (tanggal != null && tanggal.trim().length() > 0) {
			appendModernPengumumanChip(meta, tanggal, false);
		}
		String sasaran = Common.terjemahDinamis(safeText(item.getDiperuntukkan(), ""));
		if (sasaran.length() > 0) {
			appendModernPengumumanChip(meta, shorten(sasaran, 48), false);
		}

		Toolbarbutton link = new MyToolbarbuttonConfig(safeTitle(item));
		MainStyleHelper.setSclass(link, "main-announcement-link main-announcement-board-link");
		MainStyleHelper.applyAnnouncementLink(link);
		link.setTooltiptext("Buka detail pengumuman");
		link.setParent(card);
		link.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				try {
					if (tabs != null && tabs.getChildren() != null && !tabs.getChildren().isEmpty()) {
						((Tab) tabs.getChildren().get(0)).setSelected(true);
					}
					TampilanPengumumanAkademisAction.prosess(item.getId(), tabs, tabpanels, true, headerHbox);
				} catch (Exception e) {
					showError(e);
				}
			}
		});

		// Atribut per-pengumuman "Langsung tampil di Beranda": bila dicentang, isi LANGSUNG tampil
		// penuh (render HTML apa adanya) tanpa pengguna perlu mengklik judul dulu.
		// PENTING: pengumuman bisa berupa POSTER GAMBAR / media saja (tanpa teks). stripHtml() akan
		// mengosongkan teks untuk kasus itu — jadi "ada isi" = ada teks ATAU ada media (img/iframe/…),
		// supaya pengumuman bergambar tetap tampil inline.
		String catatanBeranda = item.getCatatan();
		boolean adaIsiBeranda = catatanBeranda != null && catatanBeranda.trim().length() > 0
				&& (stripHtml(catatanBeranda).trim().length() > 0
						|| catatanBeranda.toLowerCase().indexOf("<img") >= 0
						|| catatanBeranda.toLowerCase().indexOf("<iframe") >= 0
						|| catatanBeranda.toLowerCase().indexOf("<video") >= 0
						|| catatanBeranda.toLowerCase().indexOf("<audio") >= 0
						|| catatanBeranda.toLowerCase().indexOf("<embed") >= 0
						|| catatanBeranda.toLowerCase().indexOf("<svg") >= 0
						|| catatanBeranda.toLowerCase().indexOf("background") >= 0);
		boolean tampilPenuh = Boolean.TRUE.equals(item.getLangsungTampilBeranda()) && adaIsiBeranda;
		if (tampilPenuh) {
			org.zkoss.zul.Html isiPenuh = new org.zkoss.zul.Html(Common.terjemahDinamis(item.getCatatan()));
			MainStyleHelper.setSclass(isiPenuh, "main-announcement-desc main-announcement-board-desc");
			isiPenuh.setParent(card);
		} else {
			String desc = shorten(Common.terjemahDinamis(stripHtml(item.getCatatan())), 230);
			if (desc == null || desc.trim().length() == 0) {
				desc = Common.getBahasaConfig("Klik judul pengumuman untuk membaca informasi lengkap.");
			}
			Label description = new Label(desc);
			MainStyleHelper.setSclass(description, "main-announcement-desc main-announcement-board-desc");
			MainStyleHelper.applyAnnouncementDescription(description);
			description.setParent(card);
		}
	}

	private void appendModernPengumumanChip(Component parent, String text, boolean primary) {
		if (parent == null || text == null || text.trim().length() == 0) {
			return;
		}
		Label chip = new Label(text.trim());
		MainStyleHelper.setSclass(chip, primary ? "main-announcement-chip main-announcement-chip-primary"
				: "main-announcement-chip");
		MainStyleHelper.applyAnnouncementChip(chip);
		chip.setParent(parent);
	}


	private String getPengumumanToneClass(PengumumanAkademis item, boolean utama) {
		if (utama) {
			return "main-announcement-tone-primary";
		}
		long seed = 1L;
		try {
			if (item != null && item.getKategoriPengumuman() != null && item.getKategoriPengumuman().getId() != null) {
				seed = item.getKategoriPengumuman().getId().longValue();
			} else if (item != null && item.getId() != null) {
				seed = item.getId().longValue();
			}
		} catch (Exception e) {
			seed = 1L;
		}
		int tone = (int) (Math.abs(seed) % 5L) + 1;
		return "main-announcement-tone-" + tone;
	}

	private boolean appendModernProfilePanel(Component parent) {
		try {
			if (parent == null) {
				return false;
			}

			Vbox panel = new Vbox();
			panel.setWidth("100%");
			MainStyleHelper.setSclass(panel, "main-profile-bottom-panel");
			MainStyleHelper.applyProfileBottomPanel(panel);
			panel.setParent(parent);

			// Header "Profil & Ringkasan Anda" + subjudul DIHAPUS atas permintaan —
			// konten profil langsung tampil tanpa teks pengantar.
			Vbox body = new Vbox();
			body.setWidth("100%");
			MainStyleHelper.setSclass(body, "main-profile-bottom-body");
			MainStyleHelper.applyProfileBottomBody(body);
			body.setParent(panel);

			ProfileAction.initProfile(tbmuser, body, pengumumanAkademis);
			if (body.getChildren() == null || body.getChildren().isEmpty()) {
				ProfileAction.initProfile(tbmuser, body, null);
			}

			// Jika profil dinonaktifkan → body tetap kosong → hapus wrapper panel
			boolean adaKonten = body.getChildren() != null && !body.getChildren().isEmpty();
			if (!adaKonten) {
				panel.setParent(null);
			}
			return adaKonten;
		} catch (Exception e) {
			showError(e);
			return false;
		}
	}

	private boolean isPengumumanUtama(PengumumanAkademis item) {
		try {
			return item != null && KategoriPengumuman.PENGUMUMAN_UTAMA != null
					&& item.getKategoriPengumuman() != null
					&& KategoriPengumuman.PENGUMUMAN_UTAMA.getId() != null
					&& KategoriPengumuman.PENGUMUMAN_UTAMA.getId().equals(item.getKategoriPengumuman().getId());
		} catch (Exception e) {
			return false;
		}
	}

	private String getKategoriPengumumanLabel(PengumumanAkademis item) {
		try {
			if (item != null && item.getKategoriPengumuman() != null) {
				// Nama kategori: teks Indonesia diterjemahkan otomatis mengikuti bahasa aktif
				// (English/Arab/Mandarin) via translater internal — TANPA disimpan ke DB.
				String nama = Common.terjemahDinamis(item.getKategoriPengumuman().getNama());
				if (nama != null && nama.trim().length() > 0) {
					return nama.trim();
				}
			}
		} catch (Exception e) {
			showError(e);
		}
		return Common.getBahasaConfig("Pengumuman dan Informasi");
	}

	private String safeTitle(PengumumanAkademis item) {
		try {
			// Judul pengumuman: diterjemahkan otomatis mengikuti bahasa aktif (English/Arab/Mandarin)
			// via translater internal — TANPA disimpan ke DB.
			String title = item == null ? "" : Common.terjemahDinamis(item.getJudul());
			return shorten(safeText(title, "Pengumuman"), 170);
		} catch (Exception e) {
			return "Pengumuman";
		}
	}

	private String formatTanggalPengumuman(PengumumanAkademis item) {
		try {
			return item == null || item.getTanggal() == null ? "" : Common.dateFormat2.get().format(item.getTanggal());
		} catch (Exception e) {
			return "";
		}
	}

	private String stripHtml(String value) {
		String text = safeText(value, "");
		text = text.replaceAll("(?is)<style[^>]*>.*?</style>", " ");
		text = text.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
		text = text.replaceAll("<[^>]*>", " ");
		text = text.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
		text = text.replaceAll("(?is)/\\*.*?\\*/", " ");
		text = text.replaceAll("(?is)@[a-zA-Z\\-]+\\s*[^;{]*\\{[^}]*\\}", " ");
		text = text.replaceAll("(?is)\\.[a-zA-Z0-9_-]+\\s*\\{[^}]*\\}", " ");
		text = text.replaceAll("(?is)#[a-zA-Z0-9_-]+\\s*\\{[^}]*\\}", " ");
		text = text.replaceAll("(?is)(^|\\s)[a-zA-Z0-9_-]+\\s*:\\s*[^;]{1,120};", " ");

		int cssComment = indexOfIgnoreCase(text, "/*");
		if (cssComment >= 0) {
			text = text.substring(0, cssComment);
		}
		int cssClass = indexOfCssBlockStart(text);
		if (cssClass >= 0) {
			text = text.substring(0, cssClass);
		}
		int cssWord = indexOfIgnoreCase(text, "css untuk");
		if (cssWord >= 0 && cssWord < 140) {
			text = text.substring(0, cssWord);
		}

		return text.replaceAll("\\s+", " ").trim();
	}

	private int indexOfIgnoreCase(String text, String token) {
		if (text == null || token == null) {
			return -1;
		}
		return text.toLowerCase().indexOf(token.toLowerCase());
	}

	private int indexOfCssBlockStart(String text) {
		if (text == null) {
			return -1;
		}
		int classPos = text.indexOf(".");
		while (classPos >= 0 && classPos + 1 < text.length()) {
			int brace = text.indexOf("{", classPos);
			if (brace > classPos && brace - classPos < 80) {
				String candidate = text.substring(classPos + 1, brace).trim();
				if (candidate.matches("[a-zA-Z0-9_-]+")) {
					return classPos;
				}
			}
			classPos = text.indexOf(".", classPos + 1);
		}
		return -1;
	}

	private String shorten(String value, int max) {
		String text = safeText(value, "");
		if (max < 4 || text.length() <= max) {
			return text;
		}
		return text.substring(0, max - 3) + "...";
	}

	private String safeText(String value, String defaultValue) {
		if (value == null || value.trim().length() == 0) {
			return defaultValue == null ? "" : defaultValue;
		}
		return value.trim();
	}

	/**
	 * Merapikan shell MainAction lama agar lebar mengikuti viewport dan scroll
	 * horizontal tidak bocor ke body browser. Tinggi minimum tetap mengikuti
	 * konfigurasi lama tinggi_iframe_* supaya konten dashboard historis tidak
	 * terpotong.
	 */
	private void applyResponsiveShellEnhancement(Component comp) {
		try {
			MainStyleHelper.injectResponsiveShellCss(Common.ROOT == null ? "" : Common.ROOT, getConfiguredFrameMinimumHeight(true), getConfiguredFrameMinimumHeight(false));
			MainStyleHelper.appendSclassOnce(comp, "main-responsive-root zk55-responsive-shell");
			MainStyleHelper.appendSclassOnce(headerHbox, "main-responsive-header");
			MainStyleHelper.appendSclassOnce(gridHeader, "main-responsive-header-grid");
			MainStyleHelper.appendSclassOnce(rowHeader2, "main-responsive-module-row");
			MainStyleHelper.appendSclassOnce(tinggiFrame, "main-responsive-frame");
			MainStyleHelper.appendSclassOnce(centerTinggiFrame, "main-responsive-center");
			MainStyleHelper.appendSclassOnce(navigasi, "main-responsive-sidebar");
			MainStyleHelper.appendSclassOnce(iframe, "main-responsive-tabbox");
			MainStyleHelper.appendSclassOnce(tabs, "main-responsive-tabs");
			MainStyleHelper.appendSclassOnce(tabpanels, "main-responsive-tabpanels");
			MainStyleHelper.appendSclassOnce(panel_home, "main-responsive-home-panel");
			MainStyleHelper.appendSclassOnce(headerHboxButton, "main-responsive-header-actions");
			MainStyleHelper.appendSclassOnce(usersOnline, "main-responsive-online-button");
			MainStyleHelper.appendSclassOnce(customerService, "main-responsive-floating-button");
			MainStyleHelper.appendSclassOnce(menuService, "main-responsive-floating-menu");
			MainStyleHelper.appendSclassOnce(eMenuButton, "main-responsive-header-menu-button main-responsive-header-menu-primary");
			MainStyleHelper.appendSclassOnce(footer, "main-responsive-footer");

			applyResponsiveShellWidth();
			applyResponsiveShellSizing();
		} catch (Exception e) {
			showError(e);
		}
	}

	private void applyResponsiveShellWidth() {
		try {
			if (tinggiFrame != null) {
				tinggiFrame.setWidth("100%");
				MainStyleHelper.applyResponsiveFrameWidth(tinggiFrame);
			}
			if (centerTinggiFrame != null) {
				ais.ui.util.ZkCompat.setFlex(centerTinggiFrame, true);
				MainStyleHelper.applyResponsiveCenterWidth(centerTinggiFrame);
			}
			if (iframe != null) {
				iframe.setWidth("100%");
				MainStyleHelper.applyResponsiveFrameWidth(iframe);
			}
			if (tabs != null) {
				tabs.setWidth("100%");
				MainStyleHelper.applyResponsiveTabsWidth(tabs);
			}
			if (tabpanels != null) {
				tabpanels.setWidth("100%");
				MainStyleHelper.applyResponsivePanelWidth(tabpanels);
			}
			if (panel_home != null) {
				panel_home.setWidth("100%");
				MainStyleHelper.applyResponsiveHomePanelWidth(panel_home);
			}
			if (gridHeader != null) {
				gridHeader.setWidth("100%");
				MainStyleHelper.applyResponsiveGridHeaderWidth(gridHeader);
			}
		} catch (Exception e) {
			showError(e);
		}
	}

	private void applyResponsiveShellSizing() {
		try {
			if (tinggiFrame == null || iframe == null) {
				return;
			}
			boolean isMobile = Common.isMobile();
			int minimumHeight = getConfiguredFrameMinimumHeight(isMobile);
			int viewportHeight = desktopHeight > 0 ? desktopHeight : minimumHeight;
			int headerHeight = getResponsiveHeaderHeight(isMobile);
			int frameHeight = Math.max(minimumHeight, viewportHeight - headerHeight - 8);
			if (frameHeight < 480) {
				frameHeight = 480;
			}

			String frameHeightPx = frameHeight + "px";
			String minHeightPx = minimumHeight + "px";
			tinggiFrame.setHeight(frameHeightPx);
			iframe.setHeight(frameHeightPx);
			MainStyleHelper.applyResponsiveFrameHeight(tinggiFrame, frameHeightPx, minHeightPx);
			MainStyleHelper.applyResponsiveFrameHeight(iframe, frameHeightPx, minHeightPx);

			if (tabpanels != null) {
				tabpanels.setHeight(frameHeightPx);
				MainStyleHelper.applyResponsivePanelHeight(tabpanels, frameHeightPx, minHeightPx);
			}
			if (panel_home != null) {
				panel_home.setHeight(frameHeightPx);
				MainStyleHelper.applyResponsiveHomePanelHeight(panel_home, frameHeightPx, minHeightPx);
			}
			if (navigasi != null && !isHeaderDropdownMenuEnabled()) {
				MainStyleHelper.applyResponsiveSidebarHeight(navigasi, frameHeightPx);
			} else if (isHeaderDropdownMenuEnabled()) {
				applyHeaderDropdownMenuLayout();
			}
		} catch (Exception e) {
			showError(e);
		}
	}

	public static int getConfiguredFrameMinimumHeight(boolean isMobile) {
		if(true) {
			return 20000;
		}
		@SuppressWarnings("unused")
		String key = isMobile ? "tinggi_iframe_mobile_baru" : "tinggi_iframe_baru_banget";
		String defaultValue = isMobile ? "3000px" : "1750px";
		try {
			return parsePx(Common.getKonfigurasi(key, defaultValue).getNilai(), parsePx(defaultValue, 1750));
		} catch (Exception e) {
			return parsePx(defaultValue, 1750);
		}
	}

	private int getResponsiveHeaderHeight(boolean isMobile) {
		try {
			if (headerHbox != null && headerHbox.getHeight() != null && headerHbox.getHeight().trim().length() > 0) {
				return parsePx(headerHbox.getHeight(), isMobile ? 230 : 112);
			}
		} catch (Exception e) {
			showError(e);
		}
		if (isMobile) {
			if (isHeaderDropdownMenuEnabled()) {
				return rowHeader2 != null && rowHeader2.isVisible() ? 108 : 72;
			}
			return 230;
		}
		return rowHeader2 != null && rowHeader2.isVisible() ? 112 : 72;
	}

	private static int parsePx(String value, int defaultValue) {
		try {
			if (value == null) {
				return defaultValue;
			}
			String angka = value.replaceAll("[^0-9]", "");
			if (angka.length() == 0) {
				return defaultValue;
			}
			return Integer.parseInt(angka);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * Tombol profil header bergaya WEB-INF/baru/include/navbar.jsp:
	 * pill putih berisi foto bulat pengguna + nama + role bertumpuk
	 * (chevron kecil ditambahkan via CSS ::after). Dipakai header desktop
	 * maupun mobile sehingga tampilannya konsisten.
	 * Styling: css_utama.css blok "HEADER GAYA NAVBAR JSP".
	 */
	private org.zkoss.zul.A buatTombolProfilHeader() {
		org.zkoss.zul.A pill = new org.zkoss.zul.A();
		MainStyleHelper.setSclass(pill, "user_button_profile ais-header-profile-pill");
		pill.setTooltiptext("Pengaturan akun");

		org.zkoss.zul.Image avatar = new org.zkoss.zul.Image();
		String urlFoto = null;
		try {
			urlFoto = ais.common.ProfileImageUtil.getUrlFotoDariObject(tbmuser, true);
		} catch (Exception e) {
			urlFoto = null;
		}
		avatar.setSrc(urlFoto == null || urlFoto.trim().length() == 0 ? "/img/user_default.png" : urlFoto);
		MainStyleHelper.setSclass(avatar, "ais-header-profile-avatar");
		avatar.setParent(pill);

		Div teks = new Div();
		MainStyleHelper.setSclass(teks, "ais-header-profile-teks");
		teks.setParent(pill);

		Label nama = new Label(tbmuser == null ? "" : tbmuser.getUserNama());
		MainStyleHelper.setSclass(nama, "ais-header-profile-nama");
		nama.setParent(teks);

		Label role = new Label(tbmuser == null || tbmuser.hakAkses() == null ? ""
				: tbmuser.hakAkses().getRoleName());
		MainStyleHelper.setSclass(role, "ais-header-profile-role");
		role.setParent(teks);
		return pill;
	}

	/**
	 * Setel badge merah "belum dibaca" pada lonceng header.
	 *
	 * <p>
	 * Selain {@code setVisible}, kelas penanda {@code ais-badge-kosong} WAJIB ikut
	 * dipasang saat jumlahnya 0. Alasannya: aturan CSS badge memakai
	 * {@code display:inline-flex !important} (supaya angka ter-tengah rapi), dan
	 * deklarasi ber-{@code !important} pada stylesheet MENGALAHKAN inline style
	 * {@code display:none} yang dipasang ZK ketika {@code setVisible(false)}. Tanpa
	 * kelas ini, lingkaran merah KOSONG tetap terlihat walaupun semua notifikasi sudah
	 * dibaca.
	 * </p>
	 *
	 * @param badge label badge (boleh null)
	 * @param bell  tombol lonceng untuk tooltip (boleh null)
	 * @param belum jumlah notifikasi yang belum dibaca
	 */
	private static void setBadgeNotif(Label badge, MyToolbarbutton bell, int belum) {
		if (badge != null) {
			badge.setValue(belum > 99 ? "99+" : String.valueOf(belum));
			badge.setSclass(belum > 0 ? "ais-header-bell-badge" : "ais-header-bell-badge ais-badge-kosong");
			badge.setVisible(belum > 0);
		}
		if (bell != null) {
			bell.setTooltiptext(belum > 0 ? belum + " informasi belum dibaca" : "Tidak ada informasi baru");
		}
	}

	private void notif(Component component) {
		if (component == null)
			return;
		if (tbmuser != null && tbmuser.getUserId() != null
				&& Common.bolehKonfigurasi("tampilkan_notif_di_dafboard")) {
			/* Lonceng gaya navbar.jsp: ikon saja dengan badge angka merah di pojok
			 * (bukan teks "Info (n)"). Badge disembunyikan saat tidak ada notifikasi. */
			final MyToolbarbutton toolbarbuttonBell = new MyToolbarbutton("fa-bell fa-big", "Info");
			MainStyleHelper.setSclass(toolbarbuttonBell, "user_button_profile ais-header-bell");
			toolbarbuttonBell.setTooltiptext("Pusat pemberitahuan");
			final Label badgeNotif = new Label("");
			MainStyleHelper.setSclass(badgeNotif, "ais-header-bell-badge");
			badgeNotif.setVisible(false);
			/* MyToolbarbutton (Toolbarbutton ZK) tidak boleh punya anak —
			 * "Child not allowed". Badge dijadikan SIBLING di dalam Div
			 * pembungkus ber-position:relative (CSS: ais-header-bell-wrap),
			 * sehingga tetap menempel di pojok lonceng. */
			Div bellWrap = new Div();
			MainStyleHelper.setSclass(bellWrap, "ais-header-bell-wrap");
			bellWrap.appendChild(toolbarbuttonBell);
			bellWrap.appendChild(badgeNotif);
			component.appendChild(bellWrap);

			/* Popup lama dilepas tiap refresh timer supaya tidak menumpuk di page root. */
			final Menupopup[] popupNotifLama = new Menupopup[1];

			final EventListener refreshNotif = new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Menupopup treeitem = new Menupopup();
					/* Styling kartu putih + responsif: css_utama.css blok
					 * "PROFIL DROPDOWN MODERN" dan "NOTIFIKASI INFO DROPDOWN". */
					treeitem.setSclass("ais-profil-menu ais-notif-menu");
					treeitem.setParent(page.getFirstRoot());
					toolbarbuttonBell.setPopup(treeitem);
					if (popupNotifLama[0] != null) {
						try {
							popupNotifLama[0].detach();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:4427");
						}
					}
					popupNotifLama[0] = treeitem;

					MyMenuitem judulNotif = new MyMenuitem("Notifikasi", "/img/svg/information-circle-outline.svg");
					judulNotif.setSclass("menu_item ais-profil-identitas");
					judulNotif.setDisabled(true);
					judulNotif.setParent(treeitem);

					// Dibaca dari cache multi-level (L1/L2/L3) yang sudah dihangatkan saat
					// startup, sehingga lonceng tidak menembak basis data tiap detik.
					// Memakai Item (bukan sekadar keterangan) agar diketahui id notifikasi
					// (untuk menandai sudah dibaca saat diklik) dan status dibaca/belum.
					List<ais.common.NotifikasiCache.Item> notifikasis = new ArrayList<ais.common.NotifikasiCache.Item>();
					try {
						notifikasis.addAll(ais.common.NotifikasiCache.itemLonceng(tbmuser.getUserId(), 20));
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}

					if (notifikasis.isEmpty()) {
						setBadgeNotif(badgeNotif, toolbarbuttonBell, 0);
						MyMenuitem kosong = new MyMenuitem("Tidak ada informasi baru", "/img/svg/check2.svg");
						kosong.setSclass("menu_item ais-notif-kosong");
						kosong.setDisabled(true);
						kosong.setParent(treeitem);
					} else {
						Set<String> keys = new HashSet<String>();
						int count = 0;
						/* Badge merah HANYA menghitung notifikasi yang BELUM dibaca. Disimpan di
						 * array agar nilainya bisa dikurangi seketika ketika item diklik. */
						final int[] belumDibaca = new int[1];
						for (final ais.common.NotifikasiCache.Item item : notifikasis) {
							if (count >= 20)
								break;
							try {
								String ket = item.getKeterangan();
								JSONObject jsonObject = new JSONObject(ket);
								JSONObject classData = jsonObject.getJSONObject("classData");
								String subject = jsonObject.optString("subject", "");
								final String judulNotifItem = subject;
								final String bukaZk = jsonObject.optString("bukaZk", "");

								if (!keys.contains(subject)) {
									keys.add(subject);
									count++;
									final Long notifId = item.getId();
									/* Status baca PER-PENERIMA (tabel notifikasi_dibaca): dihitung
									 * untuk pengguna yang sedang masuk, bukan status per-record. */
									final String userIdNotif = tbmuser.getUserId();
									final boolean sudahDibaca = ais.common.NotifikasiCache.sudahDibacaOleh(notifId,
											userIdNotif);
									if (!sudahDibaca) {
										belumDibaca[0]++;
									}
									String objKet = classData.optString("object_keterangan", "");
									GeneralValueObject gvoRaw = ConstantValues.ambil(classData.optString("name", ""),
											ais.common.JsonCompatUtil.optLong(classData, "id", 0L));
									// PENTING: ConstantValues.ambil mengambil objek dari cache / sesi
									// read-only yang sudah ditutup, sehingga objek DETACHED. Saat notifikasi
									// diklik (mis. Disposisi Surat Keluar/Masuk), pembukaan halaman mengakses
									// relasi LAZY (suratKeluar/pejabat/disposisi) → LazyInitializationException
									// → halaman tidak tampil. Ambil ulang pada SESSION AKTIF agar objek
									// ter-attach dan relasi lazy bisa dibuka. Berlaku untuk SEMUA jenis notif.
									GeneralValueObject gvoAttached = gvoRaw;
									try {
										if (gvoRaw != null && gvoRaw.getId() != null) {
											String entityName = gvoRaw.getClass().getName().split("_")[0];
											Object fresh = ais.database.hibernate.HibernateUtil.currentSession()
													.get(entityName, gvoRaw.getId());
											if (fresh instanceof GeneralValueObject) {
												gvoAttached = (GeneralValueObject) fresh;
											}
										}
									} catch (Exception eAttach) { ais.common.ErrorAuditUtil.record(eAttach, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction.java:4488");
									}
									final GeneralValueObject gvo = gvoAttached;

									final MyMenuitem menuitem = new MyMenuitem(
											subject + (objKet.trim().isEmpty() ? "" : " (" + objKet + ")"),
											sudahDibaca ? "/img/svg/check2.svg"
													: "/img/svg/information-circle-outline.svg");
									/* Bedakan warna: belum dibaca = menonjol, sudah dibaca = redup. */
									menuitem.setSclass("menu_item ais-notif-item "
											+ (sudahDibaca ? "ais-notif-dibaca" : "ais-notif-baru"));
									menuitem.setParent(treeitem);
									menuitem.addEventListener("onClick", new EventListener() {
										public void onEvent(Event arg0) throws Exception {
											/* Sekali diklik: tandai SUDAH DIBACA -> tidak dihitung lagi pada
											 * badge merah, dan tampilannya berubah menjadi redup. */
											if (!sudahDibaca) {
												try {
													ais.common.NotifikasiCache.tandaiSudahDibaca(notifId, userIdNotif);
													menuitem.setSclass("menu_item ais-notif-item ais-notif-dibaca");
													belumDibaca[0] = belumDibaca[0] > 0 ? belumDibaca[0] - 1 : 0;
													setBadgeNotif(badgeNotif, toolbarbuttonBell, belumDibaca[0]);
												} catch (Exception eTandai) {
													ais.common.ErrorAuditUtil.record(eTandai,
															"tandai notifikasi dibaca (lonceng MainAction)");
												}
											}
											if (bukaZk != null && !bukaZk.trim().isEmpty()) {
												try {
													String url = bukaZk.trim();
													if (!url.toLowerCase().startsWith("http")) {
														url = Common.getRequestHostWithProtocol()
																+ (url.startsWith("/") ? "" : "/") + url;
													}
													Common.displayWindowIframe(url, true, "92%", "92%", judulNotifItem);
													return;
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
												}
											}
											if (gvo != null && gvo.getId() != null) {
												if (gvo instanceof ais.database.model.ticket.Ticket) {
													ais.action.master.ticket.TicketingAction.bukaDetailNotifikasi(gvo.getId());
												} else if (gvo instanceof DataSop) {
													ais.database.model.sop.DisposisiSop dsNotif = ((DataSop) gvo).getDisposisiSop();
													if (dsNotif != null && dsNotif.getId() != null) {
														TampilanAlurSopAction.prosess(dsNotif.getId(), null, null, true, arg0.getTarget());
													}
												} else if (gvo instanceof ais.database.model.sop.DisposisiAlurSop) {
													ais.database.model.sop.DisposisiAlurSop das = (ais.database.model.sop.DisposisiAlurSop) gvo;
													if (das.getDisposisiSop() != null) {
														TampilanAlurSopAction.prosess(das.getDisposisiSop().getId(), null, null, true, arg0.getTarget());
													}
												} else if (gvo instanceof ais.database.model.sop.DisposisiSop) {
													// Sebagian notif menyimpan DisposisiSop langsung → buka alur SOP-nya.
													TampilanAlurSopAction.prosess(
															((ais.database.model.sop.DisposisiSop) gvo).getId(), null,
															null, true, arg0.getTarget());
												} else if (gvo instanceof Kegiatan) {
													Kegiatan kg = (Kegiatan) gvo;
													if (kg.getMahasiswa() != null)
														CommonReportHelper.cetakBuktipembayaranMahasiswa(kg, false);
													else
														CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kg,
																false);
												} else if (gvo instanceof Pertemuan) {
													new PertemuanHelper(Common.getCurrentUser().getMahasiswa(),
															Common.getCurrentUser().getBiodataCalonMahasiswa())
															.display((Pertemuan) gvo, new DataLoader() {
																public void loadData(Object value) {
																}
															}, 1);
												} else if (gvo instanceof AlurPersetujuanSuratKeluarStatus) {
													AlurPersetujuanSuratKeluarStatusAction
															.onAddExternal(new EventListener() {
																public void onEvent(Event arg0) {
																}
															}, (AlurPersetujuanSuratKeluarStatus) gvo);
												} else if (gvo instanceof AlurPersetujuanSuratMasukStatus) {
													AlurPersetujuanSuratMasukStatusAction
															.onAddExternal(new EventListener() {
																public void onEvent(Event arg0) {
																}
															}, (AlurPersetujuanSuratMasukStatus) gvo);
												} else if (gvo instanceof PengumumanAkademis) {
													TampilanPengumumanAkademisAction.prosess(
															((PengumumanAkademis) gvo).getId(), null, null, true, null);
												}
											}
										}
									});
								}
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}
						/* Badge merah di pojok lonceng HANYA menghitung yang BELUM dibaca;
						 * notifikasi yang sudah diklik/dibaca tetap tampil (redup) tapi tidak
						 * ikut dihitung. Bila 0 -> lingkaran merah disembunyikan sepenuhnya. */
						setBadgeNotif(badgeNotif, toolbarbuttonBell, belumDibaca[0]);
						keys.clear();
					}
					notifikasis.clear();
				}
			};
			// Tampilan awal lonceng saat halaman dibuka (sekali, segera).
			Common.createDefaultTimer(refreshNotif);
			/*
			 * CEK BERKALA tiap 10 menit TANPA reload halaman & TANPA spinner "busy", agar
			 * disposisi (pengajuan SOP / persuratan) yang baru masuk untuk user yang harus
			 * menindaklanjuti LANGSUNG tampil di lonceng "Info" dan badge merah ter-update.
			 * Sebelumnya lonceng hanya dibangun SEKALI saat load (timer one-shot) sehingga
			 * notifikasi baru tidak muncul sampai halaman di-reload manual.
			 */
			final org.zkoss.zul.Timer pollNotif = new org.zkoss.zul.Timer();
			pollNotif.setDelay(10 * 60 * 1000);
			pollNotif.setRepeats(true);
			pollNotif.addEventListener("onTimer", refreshNotif);
			bellWrap.appendChild(pollNotif);
		}
	}

	private void initMenuPengumuman(final Textbox cari) {
		if (rowPengumuman != null) {
			Common.clear(rowPengumuman);
		}
		// Daftar pengumuman dipindahkan ke panel Home center agar tidak lagi
		// menumpuk di bawah menu utama pada West/sidebar.
		if (Common.bolehKonfigurasi("pengumuman_home_modern_di_center")) {
			return;
		}
		if (pengumumanAkademis == null || !pengumumanAkademis.getTampilkanPengumumanLain()) {
			if (Common.bolehKonfigurasi("aktifkan_menu_baru_untuk_pengguna", Konfigurasi.TIDAK_AKTIF) || Common.isAsliMobile()) {
				Common.clear(rowPengumuman);
				Rows rows = new Rows();
				Grid grid = new Grid();
				grid.setParent(rowPengumuman);
				grid.appendChild(rows);

				Session session = null;
				try {
					session = openLocalSession();
					Criteria criteria = TampilanPengumumanAkademisAction.initCriteriaStatic(true, tbmuser,
							PerguruanTinggiUtil.getPerguruanTinggi(), null, session);
					TampilanPengumumanAkademisAction.loadData(rows, cari.getValue().trim(), new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							Long p;
							try {
								Object[] obj = (Object[]) arg0.getData();
								p = (Long) obj[0];
								Clients.scrollIntoView((Row) obj[1]);
							} catch (Exception e) {
								p = ((PengumumanAkademis) arg0.getData()).getId();
							}

							((Tab) tabs.getChildren().get(0)).setSelected(true);
							TampilanPengumumanAkademisAction.prosess(p, tabs, tabpanels, headerHbox);

							if (!cari.getValue().trim().isEmpty()) {
								cari.setValue("");
								initPencarianMenuJikaKosong();
								initMenuPengumuman(cari);
							}
						}
					}, new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							PengumumanAkademis pa = (PengumumanAkademis) arg0.getData();
							TampilanPengumumanAkademisAction.prosess(pa.getId(), tabs, tabpanels, true, headerHbox);
							MyMessageboxConfig.showFormat(
									"Terdapat Polling / Jejak Pendapat \"{V1}\". Mohon Bapak/Ibu berkenan melengkapi Polling / Jejak Pendapat berikut terlebih dahulu.",
									"Polling / Jejak Pendapat", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
									pa.getJudul());
						}
					}, criteria, tbmuser, null, mobile, MainStyleHelper.menuBackgroundStyle(menuBgColor));
				} catch (Exception ex) {
					showError(ex);
				} finally {
					closeNativeSessionQuietly(session);
				}
			}
		}
	}

	private void initPencarianMenu() throws Exception {
		Hbox hbox = new Hbox();
		hbox.setParent(rowPencarian);
		hbox.setWidth("100%");
		hbox.setPack("center");
		hbox.setAlign("center");
		MainStyleHelper.appendSclassOnce(hbox, "main-responsive-menu-searchbar");

		MyLabelConfig c = new MyLabelConfig("Cari:");
		MainStyleHelper.setStyle(c, MainStyleHelper.SEARCH_LABEL);
		MainStyleHelper.setSclass(c, "cari_menu main-responsive-menu-search-label");

		final Textbox cari = new Textbox();
		cari.setCols(18);
		MainStyleHelper.appendSclassOnce(cari, "main-responsive-menu-search-input");
		hbox.appendChild(cari);

		MyToolbarbuttonConfig config = new MyToolbarbuttonConfig("", "/img/svg/search_menu.svg");
		MainStyleHelper.appendSclassOnce(config, "main-responsive-menu-search-button");
		config.setTooltiptext("Cari menu");
		hbox.appendChild(config);

		MyToolbarbuttonConfig close = new MyToolbarbuttonConfig("Tutup");
		MainStyleHelper.appendSclassOnce(close, "main-responsive-menu-popup-close-button");
		close.setTooltiptext("Tutup menu");
		hbox.appendChild(close);
		close.addEventListener("onClick", new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				closePopupQuietly(popupmenu);
			}
		});

		EventListener eventListener = new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				String cv = cari.getValue().trim();
				if (cv.isEmpty()) {
					initPencarianMenuJikaKosong();
				} else {
					Common.clear(rowDicari);
					MainMenuHelper.loadMenuCari(tbmuser, login, rowDicari, new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							closeNavigasi();
							onTutupPopup(null);
							if (mycenter != null) {
								mycenter.detach();
								mycenter = null;
							}
							if (!cari.getValue().trim().isEmpty()) {
								cari.setValue("");
								initPencarianMenuJikaKosong();
							}
						}
					}, iframe, navigasi, menuService, cv);
				}
				initMenuPengumuman(cari);
			}
		};
		cari.addEventListener("onOK", eventListener);
		config.addEventListener("onClick", eventListener);
		eventListener.onEvent(null);
	}

	private void initPencarianMenuJikaKosong() {
		Common.clear(rowDicari);
		if (!mobile) {
			Menubar menubar = new Menubar();
			menubar.setWidth("100%");
			menubar.setHeight("100%");
			menubar.setOrient("vertical");
			menubar.setAutodrop(true);
			rowDicari.appendChild(menubar);
			MainMenuHelper.loadTree(tbmuser, login, menubar, new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					closeNavigasi();
					onTutupPopup(null);
					if (mycenter != null) {
						mycenter.detach();
						mycenter = null;
					}
				}
			}, iframe, navigasi, menuService, pt, ya);
		} else {
			Tree tree = new Tree();
			tree.setRows(200);
			tree.setWidth("100%");
			MainStyleHelper.setZclass(tree, "z-dottree");
			MainStyleHelper.applyMobileMenuTree(tree, menuBgColor);
			if (navigasi != null) {
				try {
					Common.clear(navigasi);
					navigasi.detach();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				Common.tampilanScroll1(rowDicari).appendChild(tree);
			} else {
				Common.clear(rowDicari);
				rowDicari.appendChild(tree);
			}
			MainTreeMenuHelper.loadTree(tbmuser, login, tree, new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					closeNavigasi();
					onTutupPopup(null);
					if (mycenter != null) {
						mycenter.detach();
						mycenter = null;
					}
				}
			}, iframe, navigasi, menuService, pt, ya);
		}
	}

	public void setDashboardTitle(MyTabConfig tab, String src) {
		if (src == null || tab == null)
			return;
		if (src.contains("admin"))
			tab.setLabel("Dashboard Admin");
		if (src.contains("keuangan"))
			tab.setLabel("Dashboard Keuangan");
	}

	public void onMobile(Event event) throws Exception {
		execution.sendRedirect(execution.getContextPath() + "/main?is_mobile=true");
	}

	public void onLihatOnline(Event event) throws Exception {
		DaftarPenggunaOnline w = new DaftarPenggunaOnline();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(w);
		w.setVisible(true);
		w.onModal();
	}

	public void updateUserOnline() {
		try {
			/* Komponen bisa sudah terlepas dari desktop (header dibangun ulang /
			 * tab ditutup) ketika timer ini menyala; setLabel pada komponen
			 * detached melempar NPE di getAttachedUiEngine. */
			if (usersOnline != null && usersOnline.getDesktop() != null && usersOnline.getPage() != null) {
				if (UserOnlineCounter.count == null || UserOnlineCounter.countOnline == null)
					UserOnlineCounter.check();
				Date d = WaktuUtil.getDate();
				boolean tampilTa = Common.bolehKonfigurasi("tampil_ta_di_status_online");
				String result = "Akses: " + Common.numberFormat.get().format(UserOnlineCounter.count) + ", Login: "
						+ Common.numberFormat.get().format(UserOnlineCounter.countOnline)
						+ (!tampilTa ? ""
								: (" TA: " + Common.getCurrentTahunAkademik(d) + ", Smt: "
										+ (Common.isNowSemensterGanjil(d) ? Perkuliahan.GANJIL : Perkuliahan.GENAP)));
				usersOnline.setLabel(result + ", Wkt: " + Common.dateFormat51.get().format(d)
						+ (merupakanAdmin ? ", " + HeapSizeDemo.check() : ""));
			}
		} catch (Exception e1) {
			showError(e1);
		}
	}

	private void closeNavigasi() {
		try {
			if (!isHeaderDropdownMenuEnabled() && navigation != null)
				navigation.setOpen(false);
			closePopupQuietly(popupmenu);
			closePopupQuietly(treeitemUtama);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}
}
