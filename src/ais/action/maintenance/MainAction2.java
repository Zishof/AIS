package ais.action.maintenance;

import java.net.URLEncoder;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
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
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
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

import ais.action.master.PelanggaranMahasiswaAction;
import ais.action.master.TampilanPengumumanAkademisAction;
import ais.action.master.akunting.helper.DasboardAkuntansi;
import ais.action.master.akunting.helper.DasboardAkunting;
import ais.action.master.akunting.helper.DasboardKeuangan;
import ais.action.master.akunting.helper.DasboardPembayaranPerguruanTinggi;
import ais.action.master.akunting.helper.DasboardPembayaranSekolah;
import ais.action.master.asset.helper.DasboardAnalisisVendor;
import ais.action.master.asset.helper.DasboardPengadaan;
import ais.action.master.dashboard.admin.DasboardKepegawaian;
import ais.action.master.dashboard.admin.DashboardKegiatanKedosenan;
import ais.action.master.dashboard.admin.DashboardKegiatanKedosenanAdmin;
import ais.action.master.dashboard.admin.DashboardKegiatanKemahasiswaan;
import ais.action.master.dashboard.admin.DashboardKegiatanKemahasiswaanAdmin;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.dashboard.akunting.DasboardBukuBesar;
import ais.action.master.dashboard.akunting.DasboardNeracaLajur;
import ais.action.master.dashboard.employ.DasboardKinerja;
import ais.action.master.dashboard.helper.DashboardRekapAset;
import ais.action.master.dashboard.keuangan.DasboardPiutang;
import ais.action.master.dashboard.keuangan.DasboardPiutangRInci;
import ais.action.master.dashboard.keuangan.DasboardPiutangRInciExcel;
import ais.action.master.dashboard.keuangan.DasboardPiutangRinciSekolah;
import ais.action.master.dashboard.sekolah.DashboardKegiatanKesiswaanAdmin;
import ais.action.master.dashboard.utama.DashboardData;
import ais.action.master.dashboard.utama.DashboardDataSekolah;
import ais.action.master.dashboard.utama.DashboardPustaka;
import ais.action.master.helper.ChangePasswordWindow;
import ais.action.master.helper.DaftarPenggunaOnline;
import ais.action.master.helper.MainHelper;
import ais.action.master.helper.MainMenuHelper;
import ais.action.master.helper.MainTreeMenuHelper;
import ais.action.master.helper.PertemuanHelper;
import ais.action.master.helper.UserOnlineCounter;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.PelanggaranSiswaAction;
import ais.action.master.sekolah.helper.ElearningSekolah;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.DasboardSop;
import ais.action.master.surat.AlurPersetujuanSuratKeluarStatusAction;
import ais.action.master.surat.AlurPersetujuanSuratMasukStatusAction;
import ais.action.master.surat.helper.DasboardAlurSurat;
import ais.action.master.surat.helper.DasboardSurat;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.payroll.LaporanSlipGajiRealPegawaiPerOrang;
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
import ais.ui.util.WaktuUtil;

public class MainAction2 extends GenericAutowireComposer {

	private static final long serialVersionUID = 2446397351568124278L;
	private Tabbox iframe;
	private Borderlayout tinggiFrame;
	private Center centerTinggiFrame;
	private Div footer;

	private West navigation;
	private West navigasi;
	private Center navigasicenter;
	private North mycenter;

	private Toolbarbutton usersOnline;
	private MyToolbarbuttonConfig customerService;
	private MyToolbarbuttonConfig menuService;

	private LogLogin login;
	private Label loginInformation;
	private Konfigurasi konfigurasi;

	private MyToolbarbuttonConfig chat;
	private MyToolbarbuttonConfig message;

	private Image foto;
	private Tab tabChat;
	private Tabs tabs;
	private Tabpanels tabpanels;

	private Image idLogo;
	private Tab tabDashboard, tabPustaka, tabWorkflow, tabAdministrasi, tabPengadaan;
	private Tab tabPembayaran, tabAkunting, tabKinerja, tabKeuangan, tabKepegawaian;
	private Tab tabKalenderAkademik, tabInfoKegiatan, tabLearning, tabPresensi, tabPengumuman, tabPrestasi;
	private Tabpanel panel_home;
	private Popup popupmenu;
	private Popup treeitemUtama = null;

	private Tbmuser tbmuser = null;
	private int desktopHeight, desktopWidth;
	public static Map<String, Integer> desktopWidths = Collections.synchronizedMap(new HashMap<String, Integer>());
	public static Map<String, Integer> desktopHeights = Collections.synchronizedMap(new HashMap<String, Integer>());
	public static Map<String, ChatThread> mapChat = Collections.synchronizedMap(new HashMap<String, ChatThread>());

	private MyToolbarbuttonKecilConfig eLearningButton, ePustakaButton, ePrestasiButton, ePresensiButton;
	private MyToolbarbuttonKecilConfig eAkademikButton, ePembayaranButton, eAkuntingButton, eKinerjaButton;
	private MyToolbarbuttonKecilConfig eWorkflowButton, eAdministrasiButton, ePengadaanButton;
	private MyToolbarbuttonKecilConfig eKeuanganButton, eKepegawaianButton, eKalenderAkademikButton,
			eInfoKegiatanButton;

	private Hbox headerHboxButton;
	private Hbox main2ModuleMenu;
	private MyToolbarbuttonKecilConfig IdTampilanLama;
	private Label title, motto;
	private Row rowHeader, rowHeader2;
	private Grid gridHeader;

	private MyToolbarbuttonConfig menubutton;
	private Row headerHbox, rowPencarian, rowDicari, rowPengumuman;
	private boolean merupakanAdmin = false;
	private boolean mobile = false;
	private boolean udah = false;
	private String menuBgColor;
	private PengumumanAkademis pengumumanAkademis;
	private boolean pt = true, ya = true;
	private List<Tbmrole> tbmroles;

	// --- HELPER METHODS UNTUK MENGURANGI DUPLIKASI KODE ---

	private void selectExistingTab(Tab tab) {
		if (tab == null || tab.isInvalidated()) {
			return;
		}
		tab.setSelected(true);
		Common.pilihMenu(navigasi, menuService);
	}

	private Tabbox createStandardTabbox(Tabs[] outTabs, Tabpanels[] outPanels) {
		Tabbox tabbox = new Tabbox();
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		outTabs[0] = new Tabs();
		outTabs[0].setParent(tabbox);
		outPanels[0] = new Tabpanels();
		outPanels[0].setParent(tabbox);
		return tabbox;
	}

	private Tabpanel createStandardTab(Tabs tabs, Tabpanels panels, String label, String heightPx) {
		// MyTab: drop-in Tab dgn guard "Exactly one selected tab is required: []"
		// (race klik lama dari klien saat tabbox dashboard ini dibangun ulang
		// tiap kali menu dibuka) - lihat ais.ui.util.MyTab. MainAction (shell
		// klasik) sudah pakai MyTab di sini; MainAction2 (shell modern) tadinya
		// masih pakai Tab biasa, jadi disamakan.
		Tab tab = new ais.ui.util.MyTab(label);
		tabs.appendChild(tab);
		Tabpanel panel = new ais.ui.util.MyTabpanel();
		panel.setHeight(heightPx != null ? heightPx : "100%");
		panel.setWidth("100%");
		panel.setParent(panels);
		return panel;
	}

	// --- EVENT LISTENERS TAB UTAMA ---

	public void onPustaka(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabPustaka != null && !tabPustaka.isInvalidated()) {
					selectExistingTab(tabPustaka);
					return;
				}
				DashboardPustaka a = new DashboardPustaka();
				a.setHeight("100%");
				a.setWidth("100%");
				tabPustaka = Common.insertUrl(navigasi, menuService, iframe, "Pustaka", "Pustaka",
						"/img/svg/books-thin.svg", a);
			}
		});
	}

	public void onWorkflow(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabWorkflow != null && !tabWorkflow.isInvalidated()) {
					selectExistingTab(tabWorkflow);
					return;
				}
				tabWorkflow = Common.insertUrl(navigasi, menuService, iframe, "Workflow", "Workflow",
						"/img/svg/journal-arrow-up.svg", new DasboardSop(tabs, tabpanels, new MyWindow()));
			}
		});
	}

//	public void onAdministrasi(Event event) throws Exception {
//		Common.createDefaultTimer(new EventListener() {
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				if (tabAdministrasi != null && !tabAdministrasi.isInvalidated()) {
//					selectExistingTab(tabAdministrasi);
//					return;
//				}
//				tabAdministrasi = Common.insertUrl(navigasi, menuService, iframe, "Administrasi", "Administrasi",
//						"/img/svg/journal-bookmark.svg", new DasboardSurat());
//			}
//		});
//	}

	public void onAdministrasi(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					if (tabAdministrasi != null && !tabAdministrasi.isInvalidated()) {
						selectExistingTab(tabAdministrasi);
						return;
					}

					Tabs[] myTabs = new Tabs[1];
					Tabpanels[] myPanels = new Tabpanels[1];
					Tabbox tabbox = createStandardTabbox(myTabs, myPanels);

					String rHeight = (desktopHeight * 25) + "px";
					String dashboardHeight = (desktopHeight * 25) + "px";

					// =========================================================
					// TAB 1: Dashboard Persuratan existing.
					// =========================================================
					DasboardSurat dashSurat = new DasboardSurat();
					dashSurat.setHeight("100%");
					dashSurat.setWidth("100%");

					Tabpanel panelDashboardSurat = createStandardTab(myTabs[0], myPanels[0], "Dashboard Persuratan",
							dashboardHeight);
					applyAutoScrollableTabpanel(panelDashboardSurat, dashboardHeight);
					panelDashboardSurat.appendChild(dashSurat);

					// =========================================================
					// TAB 2: Alur Persetujuan.
					// Menggantikan tab lama:
					// - Alur Persetujuan Surat
					// - Kontrol SLA Surat
					// - Analitik Alur & SLA
					// - Alur Surat Gabungan
					// - Alur Surat Masuk
					// =========================================================
					Tab tabAlurPersetujuan = new ais.ui.util.MyTab("Alur Persetujuan");
					myTabs[0].appendChild(tabAlurPersetujuan);

					final Tabpanel panelAlurPersetujuan = new ais.ui.util.MyTabpanel();
					applyAutoScrollableTabpanel(panelAlurPersetujuan, rHeight);
					panelAlurPersetujuan.setParent(myPanels[0]);

					final EventListener loadAlurPersetujuanListener = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (panelAlurPersetujuan.getChildren().isEmpty()) {
								DasboardAlurSurat l = new DasboardAlurSurat();
								l.setHeight("100%");
								l.setWidth("100%");
								l.setParent(panelAlurPersetujuan);
							}
						}
					};
					tabAlurPersetujuan.addEventListener("onClick", loadAlurPersetujuanListener);
					tabAlurPersetujuan.addEventListener("onSelect", loadAlurPersetujuanListener);

					if (myTabs[0] != null && myTabs[0].getChildren() != null && !myTabs[0].getChildren().isEmpty()) {
						((Tab) myTabs[0].getChildren().get(0)).setSelected(true);
					}
					tabbox.setSelectedIndex(0);

					tabAdministrasi = Common.insertUrl(navigasi, menuService, iframe, "Persuratan", "Administrasi",
							"/img/svg/journal-bookmark.svg", tabbox);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		});
	}

	public void onPengadaan(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabPengadaan != null && !tabPengadaan.isInvalidated()) {
					selectExistingTab(tabPengadaan);
					return;
				}

				Tabs[] myTabs = new Tabs[1];
				Tabpanels[] myPanels = new Tabpanels[1];
				Tabbox tabbox = createStandardTabbox(myTabs, myPanels);

				String panelHeight = (desktopHeight * 10) + "px";

				// ==========================================================
				// TAB 1: PROSES PENGADAAN
				// ==========================================================
				Tabpanel panelUtama = createStandardTab(myTabs[0], myPanels[0], "Proses Pengadaan", panelHeight);
				panelUtama.appendChild(new DasboardPengadaan());

				// ==========================================================
				// TAB 2: REKAP INVENTARIS
				// ==========================================================
				Tab tabInv = new ais.ui.util.MyTab("Rekap Inventaris");
				myTabs[0].appendChild(tabInv);

				final Tabpanel panelInv = new ais.ui.util.MyTabpanel();
				panelInv.setHeight(panelHeight);
				panelInv.setWidth("100%");
				panelInv.setParent(myPanels[0]);

				tabInv.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (panelInv.getChildren().isEmpty()) {
							DashboardRekapAset laporan = new DashboardRekapAset();
							laporan.setHeight("100%");
							laporan.setWidth("100%");
							laporan.setParent(panelInv);
						}
					}
				});

				// ==========================================================
				// TAB 3: ANALISIS VENDOR
				// ==========================================================
				Tab tabAnalisisVendor = new ais.ui.util.MyTab("Analisis Vendor");
				myTabs[0].appendChild(tabAnalisisVendor);

				final Tabpanel panelAnalisisVendor = new ais.ui.util.MyTabpanel();
				panelAnalisisVendor.setHeight(panelHeight);
				panelAnalisisVendor.setWidth("100%");
				panelAnalisisVendor.setParent(myPanels[0]);

				tabAnalisisVendor.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						if (panelAnalisisVendor.getChildren().isEmpty()) {
							DasboardAnalisisVendor dashboard = new DasboardAnalisisVendor();
							dashboard.setHeight("100%");
							dashboard.setWidth("100%");
							dashboard.setParent(panelAnalisisVendor);
						}
					}
				});

				tabPengadaan = Common.insertUrl(navigasi, menuService, iframe, "Pengadaan", "Pengadaan",
						"/img/svg/boxes.svg", tabbox);
			}
		});
	}

	public void onKepegawaian(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabKepegawaian != null && !tabKepegawaian.isInvalidated()) {
					selectExistingTab(tabKepegawaian);
					return;
				}
				tabKepegawaian = Common.insertUrl(navigasi, menuService, iframe, "Kepegawaian", "Kepegawaian",
						"/img/svg/user-tie.svg", new DasboardKepegawaian());
			}
		});
	}

	public void onKeuangan(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabKeuangan != null && !tabKeuangan.isInvalidated()) {
					selectExistingTab(tabKeuangan);
					return;
				}
				tabKeuangan = Common.insertUrl(navigasi, menuService, iframe, "Keuangan", "Keuangan",
						"/img/svg/money-bills.svg", new DasboardKeuangan());
			}
		});
	}

	private void applyAutoScrollableTabpanel(Tabpanel panel, String height) {
		if (panel == null) {
			return;
		}
		panel.setHeight(height);
		panel.setWidth("100%");
		panel.setStyle("height:" + height + "; max-height:" + height
				+ "; overflow:auto; padding:0; margin:0; box-sizing:border-box; background:#f8fafc;");
	}

	public void onPembayaran(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					if (tabPembayaran != null && !tabPembayaran.isInvalidated()) {
						selectExistingTab(tabPembayaran);
						return;
					}

					Tabs[] myTabs = new Tabs[1];
					Tabpanels[] myPanels = new Tabpanels[1];
					Tabbox tabbox = createStandardTabbox(myTabs, myPanels);

					final String rHeight = (desktopHeight * 15) + "px";
					final String dashboardHeight = (desktopHeight * 15) + "px";

					final boolean modeGabungan = pt && ya;

					// =========================================================
					// BLOK 1: PERGURUAN TINGGI (PT)
					// Jika pt && ya, blok ini dibuat pertama agar default aktif.
					// =========================================================
					if (pt) {
						String labelDashPT = modeGabungan ? "Dashboard Pembayaran PT" : "Dashboard Pembayaran";
						String labelPiutangPT = modeGabungan ? "Kartu Piutang PT" : "Kartu Piutang";

						DasboardPembayaranPerguruanTinggi dashPT = new DasboardPembayaranPerguruanTinggi();
						dashPT.setHeight("100%");
						dashPT.setWidth("100%");

						Tabpanel panelDashPT = createStandardTab(myTabs[0], myPanels[0], labelDashPT, dashboardHeight);
						applyAutoScrollableTabpanel(panelDashPT, dashboardHeight);
						panelDashPT.appendChild(dashPT);

						Tab tabBukuBesar = new ais.ui.util.MyTab(labelPiutangPT);
						myTabs[0].appendChild(tabBukuBesar);

						Tab tabBukuRinci = new ais.ui.util.MyTab("Kartu Piutang Rinci");
						myTabs[0].appendChild(tabBukuRinci);

						Tab tabBukuExcel = new ais.ui.util.MyTab("Kartu Piutang Rinci Excel");
						myTabs[0].appendChild(tabBukuExcel);

						final Tabpanel panelBuku = new ais.ui.util.MyTabpanel();
						applyAutoScrollableTabpanel(panelBuku, rHeight);
						panelBuku.setParent(myPanels[0]);

						final Tabpanel panelRinci = new ais.ui.util.MyTabpanel();
						applyAutoScrollableTabpanel(panelRinci, rHeight);
						panelRinci.setParent(myPanels[0]);

						final Tabpanel panelExcel = new ais.ui.util.MyTabpanel();
						applyAutoScrollableTabpanel(panelExcel, rHeight);
						panelExcel.setParent(myPanels[0]);

						final EventListener loadPiutangPTListener = new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (panelBuku.getChildren().isEmpty()) {
									DasboardPiutang l = new DasboardPiutang();
									l.setHeight("100%");
									l.setWidth("100%");
									l.setParent(panelBuku);
								}
							}
						};
						tabBukuBesar.addEventListener("onClick", loadPiutangPTListener);
						tabBukuBesar.addEventListener("onSelect", loadPiutangPTListener);

						final EventListener loadPiutangRinciPTListener = new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (panelRinci.getChildren().isEmpty()) {
									DasboardPiutangRInci l = new DasboardPiutangRInci();
									l.setHeight("100%");
									l.setWidth("100%");
									l.setParent(panelRinci);
								}
							}
						};
						tabBukuRinci.addEventListener("onClick", loadPiutangRinciPTListener);
						tabBukuRinci.addEventListener("onSelect", loadPiutangRinciPTListener);

						final EventListener loadPiutangExcelPTListener = new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (panelExcel.getChildren().isEmpty()) {
									DasboardPiutangRInciExcel l = new DasboardPiutangRInciExcel();
									l.setHeight("100%");
									l.setWidth("100%");
									l.setParent(panelExcel);
								}
							}
						};
						tabBukuExcel.addEventListener("onClick", loadPiutangExcelPTListener);
						tabBukuExcel.addEventListener("onSelect", loadPiutangExcelPTListener);
					}

					// =========================================================
					// BLOK 2: SEKOLAH / YAYASAN (YA)
					// LaporanRincianPembayaranSiswaGrid ditambahkan paling akhir.
					// =========================================================
					if (ya) {
						String labelDashSekolah = modeGabungan ? "Dashboard Pembayaran Sekolah"
								: "Dashboard Pembayaran";
						String labelPiutangSekolah = modeGabungan ? "Kartu Piutang Sekolah" : "Kartu Piutang";

						if (modeGabungan) {
							Tab tabDashSekolah = new ais.ui.util.MyTab(labelDashSekolah);
							myTabs[0].appendChild(tabDashSekolah);

							final Tabpanel panelDashSekolah = new ais.ui.util.MyTabpanel();
							applyAutoScrollableTabpanel(panelDashSekolah, dashboardHeight);
							panelDashSekolah.setParent(myPanels[0]);

							final EventListener loadDashboardSekolahListener = new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									if (panelDashSekolah.getChildren().isEmpty()) {
										DasboardPembayaranSekolah l = new DasboardPembayaranSekolah();
										l.setHeight("100%");
										l.setWidth("100%");
										l.setParent(panelDashSekolah);
									}
								}
							};

							tabDashSekolah.addEventListener("onClick", loadDashboardSekolahListener);
							tabDashSekolah.addEventListener("onSelect", loadDashboardSekolahListener);
						} else {
							DasboardPembayaranSekolah dashSekolah = new DasboardPembayaranSekolah();
							dashSekolah.setHeight("100%");
							dashSekolah.setWidth("100%");

							Tabpanel panelDashSekolahLangsung = createStandardTab(myTabs[0], myPanels[0],
									labelDashSekolah, dashboardHeight);
							applyAutoScrollableTabpanel(panelDashSekolahLangsung, dashboardHeight);
							panelDashSekolahLangsung.appendChild(dashSekolah);
						}

						Tab tabBukuBesarSekolah = new ais.ui.util.MyTab(labelPiutangSekolah);
						myTabs[0].appendChild(tabBukuBesarSekolah);

						final Tabpanel panelBukuSekolah = new ais.ui.util.MyTabpanel();
						applyAutoScrollableTabpanel(panelBukuSekolah, rHeight);
						panelBukuSekolah.setParent(myPanels[0]);

						final EventListener loadPiutangSekolahListener = new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (panelBukuSekolah.getChildren().isEmpty()) {
									DasboardPiutangRinciSekolah l = new DasboardPiutangRinciSekolah();
									l.setHeight("100%");
									l.setWidth("100%");
									l.setParent(panelBukuSekolah);
								}
							}
						};

						tabBukuBesarSekolah.addEventListener("onClick", loadPiutangSekolahListener);
						tabBukuBesarSekolah.addEventListener("onSelect", loadPiutangSekolahListener);

						// TAB PALING AKHIR: Dashboard Grid Laporan Rincian Pembayaran Siswa.
						Tab tabLaporanRincianGrid = new ais.ui.util.MyTab("Dashboard Rincian Pembayaran");
						myTabs[0].appendChild(tabLaporanRincianGrid);

						final Tabpanel panelLaporanRincianGrid = new ais.ui.util.MyTabpanel();
						applyAutoScrollableTabpanel(panelLaporanRincianGrid, rHeight);
						panelLaporanRincianGrid.setParent(myPanels[0]);

						final EventListener loadLaporanRincianGridListener = new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (panelLaporanRincianGrid.getChildren().isEmpty()) {
									ais.action.report.format1.sekolah.LaporanRincianPembayaranSiswaGrid l = new ais.action.report.format1.sekolah.LaporanRincianPembayaranSiswaGrid();
									l.setHeight("100%");
									l.setWidth("100%");
									l.setParent(panelLaporanRincianGrid);
								}
							}
						};

						tabLaporanRincianGrid.addEventListener("onClick", loadLaporanRincianGridListener);
						tabLaporanRincianGrid.addEventListener("onSelect", loadLaporanRincianGridListener);
					}

					// =========================================================
					// PAKSA DEFAULT AKTIF KE TAB PERTAMA
					// Saat pt && ya, tab pertama adalah Dashboard Pembayaran PT.
					// =========================================================
					if (myTabs[0] != null && myTabs[0].getChildren() != null && !myTabs[0].getChildren().isEmpty()) {
						Component firstTab = (Component) myTabs[0].getChildren().get(0);
						if (firstTab instanceof Tab) {
							((Tab) firstTab).setSelected(true);
						}
					}

					tabPembayaran = Common.insertUrl(navigasi, menuService, iframe, "Pembayaran", "Pembayaran",
							"/img/svg/payments.svg", tabbox);

				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});
	}

	public void onKinerja(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabKinerja != null && !tabKinerja.isInvalidated()) {
					selectExistingTab(tabKinerja);
					return;
				}
				tabKinerja = Common.insertUrl(navigasi, menuService, iframe, "Kinerja", "Kinerja",
						"/img/svg/card-checklist.svg", new DasboardKinerja());
			}
		});
	}

	/*
	 * PATCH onAkunting(Event event)
	 *
	 * Tambahkan import jika class pemanggil belum berada di package yang sama:
	 * import ais.action.master.akunting.helper.DasboardAkuntansi;
	 */
	public void onAkunting(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabAkunting != null && !tabAkunting.isInvalidated()) {
					selectExistingTab(tabAkunting);
					return;
				}

				Tabs[] myTabs = new Tabs[1];
				Tabpanels[] myPanels = new Tabpanels[1];
				Tabbox tabbox = createStandardTabbox(myTabs, myPanels);

				/*
				 * Tab utama baru: Dashboard Akuntansi menjadi landing page utama modul
				 * Akuntansi.
				 */
				createStandardTab(myTabs[0], myPanels[0], "Dashboard Akuntansi", (desktopHeight * 10) + "px")
						.appendChild(new DasboardAkuntansi());

				/*
				 * Tab existing tetap dipertahankan supaya laporan lama tidak hilang.
				 */
				Tab tabLaporan = new ais.ui.util.MyTab("Laporan Keuangan");
				myTabs[0].appendChild(tabLaporan);
				final Tabpanel panelLaporan = new ais.ui.util.MyTabpanel();
				panelLaporan.setHeight((desktopHeight - 30) + "px");
				panelLaporan.setWidth("100%");
				panelLaporan.setParent(myPanels[0]);
				tabLaporan.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						if (panelLaporan.getChildren().isEmpty()) {
							DasboardAkunting l = new DasboardAkunting();
							l.setHeight("100%");
							l.setWidth("100%");
							l.setParent(panelLaporan);
						}
					}
				});

				Tab tabBuku = new ais.ui.util.MyTab("Buku Besar");
				myTabs[0].appendChild(tabBuku);
				final Tabpanel panelBuku = new ais.ui.util.MyTabpanel();
				panelBuku.setHeight((desktopHeight - 30) + "px");
				panelBuku.setWidth("100%");
				panelBuku.setParent(myPanels[0]);
				tabBuku.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) {
						if (panelBuku.getChildren().isEmpty()) {
							DasboardBukuBesar l = new DasboardBukuBesar();
							l.setHeight("100%");
							l.setWidth("100%");
							l.setParent(panelBuku);
						}
					}
				});

				Tab tabNeraca = new ais.ui.util.MyTab("Neraca Lajur");
				myTabs[0].appendChild(tabNeraca);
				final Tabpanel panelNeraca = new ais.ui.util.MyTabpanel();
				panelNeraca.setHeight((desktopHeight - 30) + "px");
				panelNeraca.setWidth("100%");
				panelNeraca.setParent(myPanels[0]);
				tabNeraca.addEventListener("onClick", new EventListener() {
					public void onEvent(Event arg0) {
						if (panelNeraca.getChildren().isEmpty()) {
							DasboardNeracaLajur l = new DasboardNeracaLajur();
							l.setHeight("100%");
							l.setWidth("100%");
							l.setParent(panelNeraca);
						}
					}
				});

				tabAkunting = Common.insertUrl(navigasi, menuService, iframe, "Akuntansi", "Akuntansi",
						"/img/svg/file-earmark-text.svg", tabbox);
			}
		});
	}

	public void onDashboard(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabDashboard != null && !tabDashboard.isInvalidated()) {
					selectExistingTab(tabDashboard);
					return;
				}

				if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.ambilDosen() != null
						|| tbmuser.ambilGuru() != null || tbmuser.getSiswa() != null)) {
					MyWindow window = new MyWindow();
					ProfileAction.initProfile(tbmuser, window, null);
					tabDashboard = Common.insertUrl(navigasi, menuService, iframe, "Akademik", "Akademik",
							"/img/svg/dashboard-speed.svg", window);
				} else {
					String tinggi = mobile ? ((desktopHeight * 10) + "px") : ((desktopHeight * 10) + "px");
					Tabs[] myTabs = new Tabs[1];
					Tabpanels[] myPanels = new Tabpanels[1];
					Tabbox tabbox = createStandardTabbox(myTabs, myPanels);
					tabDashboard = Common.insertUrl(navigasi, menuService, iframe, "Akademik", "Akademik",
							"/img/svg/dashboard-speed.svg", tabbox);

					if (pt) {
						DashboardData a = new DashboardData();
						a.setHeight(tinggi);
						a.setWidth("100%");
						createStandardTab(myTabs[0], myPanels[0], "Akademik Perguruan Tinggi", tinggi).appendChild(a);
					}
					if (ya) {
						DashboardDataSekolah a = new DashboardDataSekolah();
						a.setHeight(tinggi);
						a.setWidth("100%");
						createStandardTab(myTabs[0], myPanels[0], "Akademik Sekolah", tinggi).appendChild(a);
					}
				}
			}
		});
	}

	public void onKegiatanDanPrestasi(Event event) {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabPrestasi != null && !tabPrestasi.isInvalidated()) {
					selectExistingTab(tabPrestasi);
					return;
				}

				tbmuser = Common.getCurrentUser();
				if (tbmuser.ambilDosen() != null) {
					DashboardKegiatanKedosenan window = new DashboardKegiatanKedosenan(tbmuser.ambilDosen());
					window.setHeight("100%");
					window.setWidth("100%");
					tabPrestasi = Common.insertUrl(navigasi, menuService, iframe, "Prestasi", "Prestasi",
							"/img/svg/trophy.svg", window);
				} else if (tbmuser.ambilGuru() != null) {
					tabPrestasi = Common.insertUrl(navigasi, menuService, iframe, "Prestasi",
							"/pages/master/prestasi_pegawai.zul", "/img/svg/trophy.svg", null);
				} else if (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null) {
					DashboardKegiatanKemahasiswaan window = new DashboardKegiatanKemahasiswaan(tbmuser.getMahasiswa());
					window.setHeight("100%");
					window.setWidth("100%");
					tabPrestasi = Common.insertUrl(navigasi, menuService, iframe, "Prestasi", "Prestasi",
							"/img/svg/trophy.svg", window);
				} else {
					boolean[] ptYa = Common.chekPtAtauSekolah();
					pt = ptYa[0];
					ya = ptYa[1];

					String tinggi = mobile ? ((desktopHeight * 4) + "px") : ((desktopHeight * 1.75) + "px");
					Tabs[] myTabs = new Tabs[1];
					Tabpanels[] myPanels = new Tabpanels[1];
					Tabbox tabbox = createStandardTabbox(myTabs, myPanels);
					tabbox.setHeight(tinggi);
					tabPrestasi = Common.insertUrl(navigasi, menuService, iframe, "Prestasi", "Prestasi",
							"/img/svg/trophy.svg", tabbox);

					if (pt) {
						Tabpanel p1 = createStandardTab(myTabs[0], myPanels[0], "Mahasiswa", tinggi);
						DashboardKegiatanKemahasiswaanAdmin w1 = new DashboardKegiatanKemahasiswaanAdmin("", "none",
								false);
						w1.setHeight("100%");
						w1.setWidth("100%");
						w1.setParent(p1);

						Tabpanel p2 = createStandardTab(myTabs[0], myPanels[0], "Dosen", tinggi);
						DashboardKegiatanKedosenanAdmin w2 = new DashboardKegiatanKedosenanAdmin("", "none", false);
						w2.setHeight("100%");
						w2.setWidth("100%");
						w2.setParent(p2);
					} else if (ya) {
						Tabpanel p1 = createStandardTab(myTabs[0], myPanels[0], "Siswa", tinggi);
						DashboardKegiatanKesiswaanAdmin w1 = new DashboardKegiatanKesiswaanAdmin("", "none", false);
						w1.setHeight("100%");
						w1.setWidth("100%");
						w1.setParent(p1);
					}

					Tab tabPegawai = new ais.ui.util.MyTab("Pegawai");
					myTabs[0].appendChild(tabPegawai);
					final Tabpanel panelPegawai = new ais.ui.util.MyTabpanel();
					panelPegawai.setParent(myPanels[0]);
					tabPegawai.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) {
							if (panelPegawai.getChildren().isEmpty()) {
								new MyInclude("/pages/master/prestasi_pegawai.zul").setParent(panelPegawai);
							}
						}
					});
				}
			}
		});
	}

	public void onPengumumanPerkuliahan(Event event) throws Exception {
		if (tabLearning != null && !tabLearning.isInvalidated()) {
			selectExistingTab(tabLearning);
			return;
		}
		Sekolah sekolah1 = SekolahUtil.getSekolah();
		if (sekolah1 != null && sekolah1.getId() != null) {
			tabLearning = Common.insertUrl(navigasi, menuService, iframe, "e-Learning", "e-Learning",
					"/img/svg/book.svg", new ElearningSekolah());
		} else {
			tabLearning = Common.insertUrl(navigasi, menuService, iframe, "e-Learning",
					"/common/mobile/e_learning.zul?user=" + URLEncoder.encode(tbmuser.getUserId(), "UTF-8"),
					"/img/svg/book.svg", null);
		}
	}

	public void onPengumuman(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabPengumuman != null && !tabPengumuman.isInvalidated()) {
					tabPengumuman.setSelected(true);
					return;
				}
				tabPengumuman = Common.insertUrl(navigasi, menuService, iframe, "Pengumuman",
						"/pages/main/tampilan_pengumuman_akademis_mobile.zul", "/img/svg/comment-2-text-line.svg",
						null);
			}
		});
	}

	public void onPresensi(Event event) throws Exception {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabPresensi != null && !tabPresensi.isInvalidated()) {
					selectExistingTab(tabPresensi);
					return;
				}
				tabPresensi = Common.insertUrl(navigasi, menuService, iframe, "Presensi", "/presensi.zul",
						"/img/svg/check-circled-outline.svg", null);
			}
		});
	}

	public void onTutupPopup(Event event) {
		if (popupmenu != null)
			popupmenu.close();
		if (treeitemUtama != null)
			treeitemUtama.close();
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
		aa.setStyle("font-size:11px;font-weight: bolder;");

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
						aa.setStyle("font-size:11px;font-weight: bolder;");
					}

					aa = new Label(hp);
					vb.appendChild(aa);
					aa.setStyle("font-size:11px;font-weight: bolder;");

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
		tabKalenderAkademik = Common.insertUrl(navigasi, menuService, iframe, "Kalender Akademik",
				"/pages/master/kalender_akademik_mahasiswa.zul", "/img/svg/information-circle-outline.svg", null);
	}

	public void onInformasiKegiatan(Event event) throws Exception {
		if (tabInfoKegiatan != null && !tabInfoKegiatan.isInvalidated()) {
			selectExistingTab(tabInfoKegiatan);
			return;
		}
		tabInfoKegiatan = Common.insertUrl(navigasi, menuService, iframe, "Info Kegiatan",
				"/pages/master/jadwal_kegiatan_kampus.zul", "/img/svg/list-task.svg", null);
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
				.getKonfigurasi("footer_style_main",
						"font-family: 'Open Sans', sans-serif;font-size: 12px;color:white;font-weight: bold;")
				.getNilai();

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setHeight("100%");
		grid.setWidth("100%");
		grid.setStyle("border:0px;background: transparent;");
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
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);

		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
		final String nama = (sekolah != null && sekolah.getId() != null) ? sekolah.getNama()
				: (yayasan != null && yayasan.getId() != null) ? yayasan.getNama()
						: (pt != null && pt.getId() != null) ? pt.getNama() : "";

		row = new MyFormRow();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);

		Label alamat = new Label();
		alamat.setStyle(styleCopyright);
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
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			Hbox hbox = new Hbox();
			hbox.setWidth("100%");
			hbox.setPack("center");
			hbox.setAlign("center");
			hbox.setParent(row);

			if (telp != null && !telp.trim().isEmpty()) {
				Toolbarbutton phoneBtn = new MyToolbarbuttonConfig(telp, "/img/svg/phone-white.svg");
				phoneBtn.setStyle(styleCopyright);
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
				waBtn.setStyle(styleCopyright);
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
				emailBtn.setStyle(styleCopyright);
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
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		Label copyright = new Label();
		copyright.setStyle(styleCopyright);
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
			centerTinggiFrame.setStyle("background:url('" + Common.ROOT
					+ "/img/bg_default.jpg') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");
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
				centerTinggiFrame.setStyle("background:url('" + kop.createLinkUri(true, true)
						+ "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;background-size: cover;-o-background-size: cover;");
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

		//desktopWidth = desktopWidth * 2;
		//desktopHeight = desktopHeight * 2;

		if (tbmuser != null && tbmuser.getUserId() != null) {
			desktopWidths.put(tbmuser.getUserId(), Integer.valueOf(desktopWidth));
			desktopHeights.put(tbmuser.getUserId(), Integer.valueOf(desktopHeight));
		}

		if (tinggiFrame == null || iframe == null) {
			return;
		}

		/*
		 * index2.zul memakai shell modern berbasis ZKoss 5.5.
		 * Versi sebelumnya memperbesar tinggi frame sampai 1.9x - 4x tinggi layar
		 * sehingga muncul double scrollbar dan ruang kerja terasa tidak terpusat.
		 * Pada versi ini tinggi area kerja dihitung dari viewport dikurangi header,
		 * sehingga scroll utama cukup berada di tabpanel/konten utama.
		 */
		applyMain2HeaderAndMenuFix();
		applyMain2ViewportSizing();
		applyMain2FullWidthFix();

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
			menuService.setVisible(false);
			navigasi.setOpen(true);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
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
				if (!StringUtils.contains(chat.getStyle(), ";background: transparent;"))
					chat.setStyle(chat.getStyle() + ";background: transparent;");
				chat.setStyle(org.apache.commons.lang3.StringUtils.replace(chat.getStyle(), ";background: yellow;",
						";background: transparent;"));
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
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction2.java:1489");
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
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction2.java:1593");
				}
			}
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (streamingSession != null) {
				try {
					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
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
										navigasi.setOpen(true);
										menuService.setVisible(false);
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
		if (tbmuser == null || (tbmuser != null && !tbmuser.getAktif())) {
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
		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

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
		applyModernShellEnhancement(comp);

		Common.createDefaultTimer(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				Clients.confirmClose(Common.getBahasaConfig("Apakah Anda yakin ingin keluar dari aplikasi ini ?"));
			}
		});

		SessionCounter.initSessionTimeout(request.getSession(), tbmuser, false);
		MainAction2.initBg(centerTinggiFrame);
		MainAction2.footer(footer);

		menuBgColor = Common.getKonfigurasi("menu_bg_color", "#F5F5F5").getNilai();
		Common.initBahasaParameter(execution.getParameter("lang"));
		Common.REAL_PATH = session.getWebApp().getRealPath("/");
		Common.REAL_PATH_REPORT_TEMP = session.getWebApp().getRealPath("/report");
		Common.initTemp();
		CommonMedia.getMediaDirectory();

		if (!MainHelper.initMain(session, execution, page, tbmuser))
			return;
		login = (LogLogin) session.getAttribute("login");

		session.setAttribute("tabs", tabs);
		session.setAttribute("tabpanels", tabpanels);

		Timer timer = new Timer(60000);
		timer.setRepeats(true);
		timer.setParent(page.getFirstRoot());
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
				if (!mobile) {
					navigasi.setOpen(true);
					menuService.setVisible(false);
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			initData();
			applyModernShellEnhancement(comp);
		}
	}


	/**
	 * Enhancement ringan untuk index2.zul. Logika utama tetap sama seperti MainAction,
	 * tetapi class/sumber CSS dibuat lebih modern dan tetap aman untuk ZKoss 5.5.
	 */
	private void applyModernShellEnhancement(Component comp) {
		try {
			injectMain2Css();
			appendSclass(comp, "main2-root main2-zk55");
			appendSclass(tinggiFrame, "main2-layout-frame");
			appendSclass(centerTinggiFrame, "main2-center");
			appendSclass(navigasi, "main2-sidebar");
			appendSclass(navigasicenter, "main2-sidebar-center");
			appendSclass(iframe, "main2-tabbox");
			appendSclass(tabs, "main2-tabs");
			appendSclass(tabpanels, "main2-tabpanels");
			appendSclass(panel_home, "main2-home-panel");
			appendSclass(headerHbox, "main2-header");
			appendSclass(headerHboxButton, "main2-header-actions");
			appendSclass(usersOnline, "main2-online-button");
			appendStyle(usersOnline, "max-width:420px;white-space:normal;line-height:1.15;font-size:10px;padding:4px 10px;");
			appendSclass(footer, "main2-footer");
			appendSclass(customerService, "main2-floating-wa");
			appendSclass(menuService, "main2-floating-menu");
			appendSclass(loginInformation, "main2-login-info");
			appendSclass(main2ModuleMenu, "main2-module-menu");
			renderMain2HeaderFromContext();
			renderMain2ShortcutMenu();
			applyMain2SidebarMode();
			applyMain2HeaderAndMenuFix();
			applyMain2ViewportSizing();
			applyMain2FullWidthFix();
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction2.java:2033");
			}
		}
	}


	/**
	 * Merapikan header index2.zul agar tidak kosong/terlalu tinggi dan memastikan
	 * tombol modul dashboard tetap terlihat pada desktop. Method ini dipanggil
	 * setelah compose, setelah initData(), dan setelah onClientInfo agar aman untuk
	 * ZKoss 5.5 yang sering menulis ulang ukuran komponen saat render awal.
	 */
	private void applyMain2HeaderAndMenuFix() {
		boolean isMobile = false;
		try {
			isMobile = Common.isMobile();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		boolean tampilShortcutHeader = false;
		try {
			tampilShortcutHeader = Common.bolehKonfigurasi("main2_tampilkan_shortcut_header", Konfigurasi.TIDAK_AKTIF);
		} catch (Exception e) {
			tampilShortcutHeader = false;
		}

		String defaultHeight = tampilShortcutHeader ? (isMobile ? "126px" : "146px") : (isMobile ? "86px" : "96px");

		try {
			if (headerHbox != null) {
				appendSclass(headerHbox, tampilShortcutHeader ? "main2-header main2-header-ready main2-has-shortcut"
						: "main2-header main2-header-ready");
				String key = isMobile ? "main2_tinggi_header_mobile" : "main2_tinggi_header_desktop";
				String height = Common.getKonfigurasi(key, defaultHeight).getNilai();
				if (height == null || height.trim().length() == 0) {
					height = defaultHeight;
				}
				if (!tampilShortcutHeader && parsePx(height, parsePx(defaultHeight, 86)) > 110) {
					height = defaultHeight;
				}
				headerHbox.setHeight(height.trim());
				appendStyle(headerHbox,
						"background:var(--theme-gradient) !important;color:var(--theme-text-on-primary) !important;overflow:hidden;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (gridHeader != null) {
				gridHeader.setWidth("100%");
				gridHeader.setHeight((headerHbox != null && headerHbox.getHeight() != null && headerHbox.getHeight().trim().length() > 0) ? headerHbox.getHeight().trim() : defaultHeight);
				appendSclass(gridHeader, tampilShortcutHeader ? "main2-header-grid main2-has-shortcut" : "main2-header-grid");
				appendStyle(gridHeader, "width:100%;background:transparent;border:0px;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (rowHeader != null) {
				rowHeader.setVisible(true);
				rowHeader.setHeight(tampilShortcutHeader ? (isMobile ? "74px" : "90px") : ((headerHbox != null && headerHbox.getHeight() != null && headerHbox.getHeight().trim().length() > 0) ? headerHbox.getHeight().trim() : defaultHeight));
				appendStyle(rowHeader, "background:transparent;border:0px;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (rowHeader2 != null) {
				rowHeader2.setVisible(tampilShortcutHeader);
				rowHeader2.setHeight(tampilShortcutHeader ? (isMobile ? "52px" : "56px") : "0px");
				appendSclass(rowHeader2, tampilShortcutHeader ? "main2-module-row" : "main2-module-row main2-shortcut-hidden");
				appendStyle(rowHeader2, tampilShortcutHeader ? "background:transparent;border:0px;"
						: "background:transparent;border:0px;display:none;height:0px;min-height:0px;max-height:0px;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (main2ModuleMenu != null) {
				main2ModuleMenu.setVisible(tampilShortcutHeader);
			}
			if (tampilShortcutHeader) {
				renderMain2ShortcutMenu();
				refreshMain2ModuleButtonVisibility();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		applyMain2SidebarMode();
	}

	private int parsePx(String value, int defaultValue) {
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
	 * Menjamin tombol shortcut dashboard pada header mengikuti hak akses dan tetap
	 * tampak pada index2.zul. Ini hanya mengulang status visible yang sudah ada di
	 * initData(), tanpa mengubah logika hak akses.
	 */
	private void refreshMain2ModuleButtonVisibility() {
		Tbmrole hakAkses = tbmuser == null ? null : tbmuser.hakAkses();
		if (hakAkses == null) {
			return;
		}
		try { if (eLearningButton != null) eLearningButton.setVisible(Boolean.TRUE.equals(hakAkses.getElearning())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (ePrestasiButton != null) ePrestasiButton.setVisible(Boolean.TRUE.equals(hakAkses.getKegiatanDanPrestasi())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (ePustakaButton != null) ePustakaButton.setVisible(Boolean.TRUE.equals(hakAkses.getPustaka())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (eWorkflowButton != null) eWorkflowButton.setVisible(Boolean.TRUE.equals(hakAkses.getWorkflow())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (eAkademikButton != null) eAkademikButton.setVisible(Boolean.TRUE.equals(hakAkses.getDashboard())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (eAdministrasiButton != null) eAdministrasiButton.setVisible(Boolean.TRUE.equals(hakAkses.getAdministrasi())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (ePengadaanButton != null) ePengadaanButton.setVisible(Boolean.TRUE.equals(hakAkses.getPengadaan())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (ePembayaranButton != null) ePembayaranButton.setVisible(Boolean.TRUE.equals(hakAkses.getPembayaran())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (eKeuanganButton != null) eKeuanganButton.setVisible(Boolean.TRUE.equals(hakAkses.getKeuangan())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (eAkuntingButton != null) eAkuntingButton.setVisible(Boolean.TRUE.equals(hakAkses.getAkunting()) && (tbmuser == null || (tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null))); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (eKepegawaianButton != null) eKepegawaianButton.setVisible(Boolean.TRUE.equals(hakAkses.getKepegawaian())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (eKinerjaButton != null) eKinerjaButton.setVisible(Boolean.TRUE.equals(hakAkses.getKinerja())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (ePresensiButton != null) ePresensiButton.setVisible(Boolean.TRUE.equals(hakAkses.getPresensiKehadiran())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (eKalenderAkademikButton != null) eKalenderAkademikButton.setVisible(Boolean.TRUE.equals(hakAkses.getKalenderAkademik())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { if (eInfoKegiatanButton != null) eInfoKegiatanButton.setVisible(Boolean.TRUE.equals(hakAkses.getInfoKegiatan())); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Memastikan area kerja utama memakai seluruh sisa lebar layar.
	 *
	 * Pada ZKoss 5.5, kombinasi Grid + Borderlayout terkadang membuat cell mengikuti
	 * lebar konten awal. Akibatnya panel Home terlihat menumpuk di kiri dan area kanan
	 * kosong. Method ini sengaja dipanggil setelah compose dan setelah onClientInfo agar
	 * Tabbox, Tabpanels, dan panel Home selalu melebar 100% mengikuti center layout.
	 */
	private void applyMain2FullWidthFix() {
		try {
			if (tinggiFrame != null) {
				tinggiFrame.setWidth("100%");
				appendStyle(tinggiFrame, "width:100%;min-width:0;box-sizing:border-box;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (centerTinggiFrame != null) {
				ais.ui.util.ZkCompat.setFlex(centerTinggiFrame, true);
				appendStyle(centerTinggiFrame, "min-width:0;box-sizing:border-box;padding:0px;margin-top:0px;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (iframe != null) {
				iframe.setWidth("100%");
				appendStyle(iframe, "width:100%;min-width:0;box-sizing:border-box;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (tabs != null) {
				tabs.setWidth("100%");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (tabpanels != null) {
				tabpanels.setWidth("100%");
				appendStyle(tabpanels, "width:100%;min-width:0;box-sizing:border-box;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (panel_home != null) {
				panel_home.setWidth("100%");
				appendStyle(panel_home, "width:100%;min-width:0;box-sizing:border-box;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (navigasi != null && !Common.isMobile()) {
				navigasi.setWidth(Common.getKonfigurasi("main2_lebar_sidebar_desktop", "248px").getNilai());
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}


	/**
	 * Membuat tinggi area kerja selalu mengikuti viewport agar tidak muncul double
	 * scrollbar. Scroll utama hanya berada pada tabpanel/konten, bukan pada body
	 * browser dan bukan pada beberapa nested frame sekaligus.
	 */
	private void applyMain2ViewportSizing() {
		try {
			if (desktopHeight <= 0) {
				return;
			}
			int headerHeight = getMain2CurrentHeaderHeight();
			int footerReserve = 0;
			try {
				footerReserve = parsePx(Common.getKonfigurasi("main2_footer_reserve_px", "0px").getNilai(), 0);
			} catch (Exception e) {
				footerReserve = 0;
			}
			int frameHeight = desktopHeight - headerHeight - footerReserve - 2;
			if (frameHeight < 360) {
				frameHeight = 360;
			}
			String frameHeightPx = frameHeight + "px";
			int tabPanelHeight = frameHeight;
			if (tabPanelHeight < 300) {
				tabPanelHeight = 300;
			}
			String tabPanelHeightPx = tabPanelHeight + "px";

			if (tinggiFrame != null) {
				tinggiFrame.setHeight(frameHeightPx);
				appendStyle(tinggiFrame, "height:" + frameHeightPx + " !important;max-height:" + frameHeightPx
						+ " !important;overflow:hidden;box-sizing:border-box;");
			}
			if (iframe != null) {
				iframe.setHeight(frameHeightPx);
				appendStyle(iframe, "height:" + frameHeightPx + " !important;max-height:" + frameHeightPx
						+ " !important;overflow:hidden;box-sizing:border-box;");
			}
			if (tabs != null) {
				tabs.setHeight("0px");
				appendStyle(tabs, "height:0px !important;min-height:0px !important;max-height:0px !important;"
						+ "display:none !important;overflow:hidden;box-sizing:border-box;");
			}
			if (tabpanels != null) {
				tabpanels.setHeight(tabPanelHeightPx);
				appendStyle(tabpanels, "height:" + tabPanelHeightPx + " !important;max-height:" + tabPanelHeightPx
						+ " !important;overflow:hidden;box-sizing:border-box;");
			}
			if (panel_home != null) {
				panel_home.setHeight(tabPanelHeightPx);
				appendStyle(panel_home, "height:" + tabPanelHeightPx + " !important;max-height:" + tabPanelHeightPx
						+ " !important;overflow:auto;box-sizing:border-box;");
			}
			if (navigasi != null) {
				appendStyle(navigasi, "height:" + frameHeightPx + " !important;max-height:" + frameHeightPx
						+ " !important;overflow-y:auto;overflow-x:visible;box-sizing:border-box;");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private int getMain2CurrentHeaderHeight() {
		try {
			if (headerHbox != null && headerHbox.getHeight() != null && headerHbox.getHeight().trim().length() > 0) {
				return parsePx(headerHbox.getHeight(), Common.isMobile() ? 86 : 96);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			boolean tampilShortcutHeader = Common.bolehKonfigurasi("main2_tampilkan_shortcut_header", Konfigurasi.TIDAK_AKTIF);
			if (tampilShortcutHeader) {
				return Common.isMobile() ? 122 : 138;
			}
			return Common.isMobile() ? 86 : 96;
		} catch (Exception e) {
			return 96;
		}
	}

	/**
	 * Mode default sidebar mengikuti theme yang dipilih institusi. Jika admin ingin
	 * kembali ke sidebar terang seperti eksperimen sebelumnya, cukup aktifkan
	 * konfigurasi main2_sidebar_terang.
	 */
	private void applyMain2SidebarMode() {
		try {
			if (navigasi == null) {
				return;
			}
			boolean sidebarTerang = Common.bolehKonfigurasi("main2_sidebar_terang", Konfigurasi.TIDAK_AKTIF);
			appendSclass(navigasi, sidebarTerang ? "main2-sidebar-light" : "main2-sidebar-theme");
		} catch (Exception e) {
			try {
				appendSclass(navigasi, "main2-sidebar-theme");
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction2.java:2359");
			}
		}
	}

	private void appendStyle(Component component, String style) {
		if (component == null || style == null || style.trim().length() == 0) {
			return;
		}
		try {
			java.lang.reflect.Method getter = component.getClass().getMethod("getStyle", new Class[] {});
			java.lang.reflect.Method setter = component.getClass().getMethod("setStyle", new Class[] { String.class });
			Object oldObj = getter.invoke(component, new Object[] {});
			String old = oldObj == null ? "" : String.valueOf(oldObj);
			String newStyle = old == null || old.trim().length() == 0 ? style : old + ";" + style;
			setter.invoke(component, new Object[] { newStyle });
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private void appendSclass(Component component, String css) {
		if (component == null || css == null || css.trim().length() == 0) {
			return;
		}
		try {
			java.lang.reflect.Method getter = component.getClass().getMethod("getSclass", new Class[] {});
			java.lang.reflect.Method setter = component.getClass().getMethod("setSclass", new Class[] { String.class });
			Object oldObj = getter.invoke(component, new Object[] {});
			String old = oldObj == null ? "" : String.valueOf(oldObj);
			String[] parts = css.trim().split("\\s+");
			String result = old;
			for (int i = 0; i < parts.length; i++) {
				if (parts[i] != null && parts[i].trim().length() > 0
						&& (" " + result + " ").indexOf(" " + parts[i].trim() + " ") < 0) {
					result = (result == null || result.trim().length() == 0 ? parts[i].trim()
							: result + " " + parts[i].trim());
				}
			}
			setter.invoke(component, new Object[] { result });
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction2.java:2399");
			// Tidak semua komponen ZK memiliki sclass. Abaikan agar kompatibel dengan ZKoss 5.5.
		}
	}

	private void injectMain2Css() {
		try {
			String root = Common.ROOT == null ? "" : Common.ROOT;
			String version = String.valueOf(System.currentTimeMillis());
			String themeHref = "";
			try {
				HttpServletRequest request = null;
				if (ExecutionsCtrl.getCurrent() != null) {
					request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
				}
				PerguruanTinggi ptAktif = PerguruanTinggiUtil.getPerguruanTinggi(request);
				if (ptAktif != null && ptAktif.getCss() != null && ptAktif.getCss().trim().length() > 0) {
					String rawCss = ptAktif.getCss().trim();
					String fileName = rawCss.substring(rawCss.lastIndexOf("/") + 1);
					themeHref = root + "/css/baru/" + fileName + "?v=" + version;
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			StringBuilder js = new StringBuilder();
			js.append("(function(){");
			js.append("function addCss(id,href){try{if(!href){return;}var old=document.getElementById(id);if(old){old.href=href;return;}var l=document.createElement('link');l.id=id;l.rel='stylesheet';l.type='text/css';l.href=href;document.getElementsByTagName('head')[0].appendChild(l);}catch(e){}};");
			js.append("addCss('ecampus-base-theme-zk','").append(js(root + "/css/baru/base-theme.css?v=" + version)).append("');");
			js.append("addCss('ecampus-main2-zk','").append(js(root + "/css/baru/main-modern-zk.css?v=" + version)).append("');");
			js.append("function fixMain2Layout(){try{var de=document.documentElement,bd=document.body;if(de){de.style.overflow='hidden';de.style.height='100%';}if(bd){bd.style.overflow='hidden';bd.style.height='100%';}var header=document.querySelector('.main2-header-ready')||document.querySelector('.main2-header');var hh=header?Math.ceil(header.getBoundingClientRect().height):96;if(!hh||hh<76){hh=96;}var vh=window.innerHeight||document.documentElement.clientHeight||800;var fh=Math.max(380,vh-hh-2);var ph=Math.max(320,fh);var cs=document.querySelectorAll('.main2-layout-frame .main2-center,.main2-center.content');for(var i=0;i<cs.length;i++){cs[i].style.marginTop='0px';cs[i].style.padding='0px';cs[i].style.width='auto';cs[i].style.right='0px';cs[i].style.overflow='hidden';}var fs=document.querySelectorAll('.main2-layout-frame,.main2-tabbox');for(var f=0;f<fs.length;f++){fs[f].style.height=fh+'px';fs[f].style.maxHeight=fh+'px';fs[f].style.overflow='hidden';}var ts=document.querySelectorAll('.main2-tabless .main2-tabs,.main2-tabs-hidden');for(var t=0;t<ts.length;t++){ts[t].style.display='none';ts[t].style.height='0px';ts[t].style.maxHeight='0px';ts[t].style.minHeight='0px';}var ps=document.querySelectorAll('.main2-tabpanels,.main2-tabpanels .z-tabpanel,.main2-home-panel,.main2-home-panel .z-tabpanel-cnt');for(var p=0;p<ps.length;p++){ps[p].style.height=ph+'px';ps[p].style.maxHeight=ph+'px';ps[p].style.overflowX='hidden';ps[p].style.overflowY='auto';ps[p].style.boxSizing='border-box';}var eb=document.querySelectorAll('.main2-home-panel .z-east-body,.main2-home-panel .z-east');for(var e=0;e<eb.length;e++){eb[e].style.overflow='visible';eb[e].style.maxHeight='none';}var sb=document.querySelectorAll('.main2-sidebar,.main2-sidebar .z-west-body');for(var s=0;s<sb.length;s++){sb[s].style.height=fh+'px';sb[s].style.maxHeight=fh+'px';sb[s].style.overflowY='auto';sb[s].style.overflowX='visible';}var hs=document.querySelectorAll('.main2-header-ready:not(.main2-has-shortcut)');for(var j=0;j<hs.length;j++){hs[j].style.maxHeight=hh+'px';}}catch(e){}}fixMain2Layout();setTimeout(fixMain2Layout,150);setTimeout(fixMain2Layout,700);try{window.onresize=function(){fixMain2Layout();};}catch(e){};");
			if (themeHref.length() > 0) {
				js.append("addCss('ecampus-selected-theme-zk','").append(js(themeHref)).append("');");
			}
			js.append("})();");
			Clients.evalJavaScript(js.toString());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private String js(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", "");
	}


	/**
	 * Mengisi header index2.zul memakai data institusi aktif, mengikuti pola
	 * navbar.jsp/header.jsp tetapi tetap berbasis komponen ZKoss 5.5.
	 */
	private void renderMain2HeaderFromContext() {
		HttpServletRequest request = null;
		try {
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		String judulText = "Enterprise Education";
		String mottoText = "Aplikasi kampus dan sekolah lengkap terintegrasi";
		String logoUrl = "/img/logo.png";

		try {
			PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);
			Sekolah sekolah = SekolahUtil.getSekolah(request);
			Yayasan yayasan = SekolahUtil.getYayasan(request);
			boolean[] ptYa = Common.chekPtAtauSekolah();
			boolean yaData = ptYa != null && ptYa.length > 1 && ptYa[1];

			if (perguruanTinggi != null) {
				if (perguruanTinggi.getNama() != null && perguruanTinggi.getNama().trim().length() > 0) {
					judulText = perguruanTinggi.getNama().trim();
				}
				if (perguruanTinggi.getMotto() != null && perguruanTinggi.getMotto().trim().length() > 0) {
					mottoText = perguruanTinggi.getMotto().trim();
				}
				String logo = PerguruanTinggiUtil.getPerguruanTinggiMedia(request, "logo_perguruanTinggi_", perguruanTinggi);
				if (logo != null && logo.trim().length() > 0) {
					logoUrl = logo.trim();
				}
			}

			if (yaData && sekolah != null && sekolah.getId() != null) {
				if (sekolah.getNama() != null && sekolah.getNama().trim().length() > 0) {
					judulText = sekolah.getNama().trim();
				}
				if (sekolah.getMotto() != null && sekolah.getMotto().trim().length() > 0) {
					mottoText = sekolah.getMotto().trim();
				}
				String logo = SekolahUtil.getSekolahMedia(request, "logo_sekolah_", sekolah);
				if (logo != null && logo.trim().length() > 0 && !logo.endsWith("logo.png")) {
					logoUrl = logo.trim();
				}
			} else if (yaData && yayasan != null && yayasan.getId() != null) {
				if (yayasan.getNama() != null && yayasan.getNama().trim().length() > 0) {
					judulText = yayasan.getNama().trim();
				}
				if (yayasan.getMotto() != null && yayasan.getMotto().trim().length() > 0) {
					mottoText = yayasan.getMotto().trim();
				}
				String logo = SekolahUtil.getYayasanMedia(request, "logo_yayasan_", yayasan);
				if (logo != null && logo.trim().length() > 0 && !logo.endsWith("logo.png")) {
					logoUrl = logo.trim();
				}
			}
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction2.java:2512");
			}
		}

		try {
			if (title != null) {
				title.setValue(judulText);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (motto != null) {
				motto.setValue(mottoText);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (idLogo != null) {
				idLogo.setSrc(logoUrl);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}


	/**
	 * Menata ulang shortcut bawaan yang dideklarasikan langsung di index2.zul.
	 * Pendekatan ini sengaja mengembalikan pola index.zul lama: tombol tetap memiliki
	 * id yang sama sehingga mekanisme visible/hak akses lama tetap berjalan, tetapi
	 * sclass dan style-nya dibuat modern. Ini juga mencegah header kosong ketika tombol
	 * dinamis belum terbentuk pada render awal ZKoss 5.5.
	 */
	private boolean styleExistingMain2ShortcutButtons() {
		boolean hasExisting = false;
		try {
			hasExisting = main2ModuleMenu != null && main2ModuleMenu.getChildren() != null
					&& !main2ModuleMenu.getChildren().isEmpty();
		} catch (Exception e) {
			hasExisting = false;
		}
		if (!hasExisting) {
			return false;
		}

		try { styleMain2Shortcut(eLearningButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(ePrestasiButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(ePustakaButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(eWorkflowButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(eAkademikButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(eAdministrasiButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(ePengadaanButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(ePembayaranButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(eKeuanganButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(eAkuntingButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(eKepegawaianButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(eKinerjaButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(ePresensiButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(eKalenderAkademikButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try { styleMain2Shortcut(eInfoKegiatanButton, "main2-module-button"); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		String root = Common.ROOT == null || Common.ROOT.trim().length() == 0 ? "" : Common.ROOT;
		try {
			if (IdTampilanLama != null) {
				IdTampilanLama.setLabel(Common.getKonfigurasi("main2_label_versi_lama", "Versi Lama").getNilai());
				IdTampilanLama.setHref(root + "/main?versilama=true");
				IdTampilanLama.setVisible(Common.bolehKonfigurasi("main2_tampilkan_tombol_versi_lama"));
				styleMain2Shortcut(IdTampilanLama, "main2-switch-button");
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			refreshMain2ModuleButtonVisibility();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return true;
	}


	/**
	 * Membentuk ulang shortcut modul di header agar selalu tampil kontras, tidak
	 * bergantung pada warna bawaan komponen, dan tetap mengikuti hak akses role.
	 */
	private void renderMain2ShortcutMenu() {
		if (main2ModuleMenu == null) {
			return;
		}

		/*
		 * Jika index2.zul sudah menyediakan tombol shortcut dengan id lama
		 * (eLearningButton, ePustakaButton, dan seterusnya), jangan dihapus.
		 * Tombol tersebut dipakai oleh logika lama untuk hak akses dan visible.
		 */
		if (styleExistingMain2ShortcutButtons()) {
			return;
		}

		if (tbmuser == null || tbmuser.hakAkses() == null) {
			return;
		}
		try {
			Common.clear(main2ModuleMenu);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		final Tbmrole hakAkses = tbmuser.hakAkses();
		try {
			addMain2Shortcut("Pengumuman", "/img/svg/comment-2-text-line-white.svg", true, new EventListener() {
				public void onEvent(Event event) throws Exception { onPengumuman(event); }
			});
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			eLearningButton = addMain2Shortcut("e-Learning", "/img/svg/book-white.svg", bool(hakAkses.getElearning()), new EventListener() {
				public void onEvent(Event event) throws Exception { onPengumumanPerkuliahan(event); }
			});
			ePrestasiButton = addMain2Shortcut("Prestasi", "/img/svg/trophy-white.svg", bool(hakAkses.getKegiatanDanPrestasi()), new EventListener() {
				public void onEvent(Event event) throws Exception { onKegiatanDanPrestasi(event); }
			});
			ePustakaButton = addMain2Shortcut("Pustaka", "/img/svg/books-thin-white.svg", bool(hakAkses.getPustaka()), new EventListener() {
				public void onEvent(Event event) throws Exception { onPustaka(event); }
			});
			eWorkflowButton = addMain2Shortcut("Pengajuan Anda", "/img/svg/journal-arrow-up-white.svg", bool(hakAkses.getWorkflow()), new EventListener() {
				public void onEvent(Event event) throws Exception { onWorkflow(event); }
			});
			eAkademikButton = addMain2Shortcut("Akademik", "/img/svg/dashboard-speed-white.svg", bool(hakAkses.getDashboard()), new EventListener() {
				public void onEvent(Event event) throws Exception { onDashboard(event); }
			});
			eAdministrasiButton = addMain2Shortcut("Surat Menyurat", "/img/svg/journal-bookmark-white.svg", bool(hakAkses.getAdministrasi()), new EventListener() {
				public void onEvent(Event event) throws Exception { onAdministrasi(event); }
			});
			ePengadaanButton = addMain2Shortcut("Pengadaan", "/img/svg/boxes-white.svg", bool(hakAkses.getPengadaan()), new EventListener() {
				public void onEvent(Event event) throws Exception { onPengadaan(event); }
			});
			ePembayaranButton = addMain2Shortcut("Pembayaran", "/img/svg/payments-white.svg", bool(hakAkses.getPembayaran()), new EventListener() {
				public void onEvent(Event event) throws Exception { onPembayaran(event); }
			});
			eKeuanganButton = addMain2Shortcut("Keuangan", "/img/svg/money-bills-white.svg", bool(hakAkses.getKeuangan()), new EventListener() {
				public void onEvent(Event event) throws Exception { onKeuangan(event); }
			});
			eAkuntingButton = addMain2Shortcut("Akuntansi", "/img/svg/file-earmark-text-white.svg", bool(hakAkses.getAkunting()) && (tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null), new EventListener() {
				public void onEvent(Event event) throws Exception { onAkunting(event); }
			});
			eKepegawaianButton = addMain2Shortcut("Kepegawaian", "/img/svg/user-tie-white.svg", bool(hakAkses.getKepegawaian()), new EventListener() {
				public void onEvent(Event event) throws Exception { onKepegawaian(event); }
			});
			eKinerjaButton = addMain2Shortcut("Kinerja", "/img/svg/card-checklist-white.svg", bool(hakAkses.getKinerja()), new EventListener() {
				public void onEvent(Event event) throws Exception { onKinerja(event); }
			});
			ePresensiButton = addMain2Shortcut("Presensi", "/img/svg/check-circled-outline-white.svg", bool(hakAkses.getPresensiKehadiran()), new EventListener() {
				public void onEvent(Event event) throws Exception { onPresensi(event); }
			});
			eKalenderAkademikButton = addMain2Shortcut("Kalender Akademik", "/img/svg/list-task-white.svg", bool(hakAkses.getKalenderAkademik()), new EventListener() {
				public void onEvent(Event event) throws Exception { onInformasiKalenderAkademik(event); }
			});
			eInfoKegiatanButton = addMain2Shortcut("Info Kegiatan", "/img/svg/information-circle-outline-white.svg", bool(hakAkses.getInfoKegiatan()), new EventListener() {
				public void onEvent(Event event) throws Exception { onInformasiKegiatan(event); }
			});
		} catch (Exception e) {
			try { Common.tampilErrorJikaAdmin(e); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction2.java:2707");}
		}

		try {
			String root = Common.ROOT == null || Common.ROOT.trim().length() == 0 ? "" : Common.ROOT;
			if (Common.bolehKonfigurasi("main2_tampilkan_tombol_versi_lama")) {
				IdTampilanLama = new MyToolbarbuttonKecilConfig(Common.getKonfigurasi("main2_label_versi_lama", "Versi Lama").getNilai(), "/img/svg/desktop-light.svg");
				IdTampilanLama.setHref(root + "/main?versilama=true");
				styleMain2Shortcut(IdTampilanLama, "main2-switch-button");
				main2ModuleMenu.appendChild(IdTampilanLama);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private boolean bool(Boolean value) {
		return value != null && value.booleanValue();
	}

	private MyToolbarbuttonKecilConfig addMain2Shortcut(String label, String image, boolean visible, EventListener listener) {
		if (!visible || main2ModuleMenu == null) {
			return null;
		}
		String text = label;
		try {
			text = Common.getBahasaConfig(label);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		MyToolbarbuttonKecilConfig button = new MyToolbarbuttonKecilConfig(text, image);
		styleMain2Shortcut(button, "main2-module-button");
		try {
			button.addEventListener("onClick", listener);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		main2ModuleMenu.appendChild(button);
		return button;
	}

	private void styleMain2Shortcut(MyToolbarbuttonKecilConfig button, String sclass) {
		if (button == null) {
			return;
		}
		try {
			button.setSclass(sclass);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			button.setStyle("font-size:10px;font-weight:800;color:var(--theme-text-on-primary);border-radius:999px;padding:6px 12px;min-width:92px;height:46px;margin:0 4px;");
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private void renderMain2LandingDashboard() {
		if (panel_home == null || tbmuser == null) {
			return;
		}

		try {
			List<Component> children = new ArrayList<Component>(panel_home.getChildren());
			for (Component child : children) {
				if (child != null && Boolean.TRUE.equals(child.getAttribute("main2LandingDashboard"))) {
					child.detach();
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Tbmrole hakAkses = null;
		try {
			hakAkses = tbmuser.hakAkses();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		final Div landing = new Div();
		landing.setAttribute("main2LandingDashboard", Boolean.TRUE);
		landing.setSclass("container-fluid px-0 main2-landing-dashboard");
		landing.setWidth("100%");

		Div hero = new Div();
		hero.setSclass("card shadow-sm border-0 main2-landing-hero");
		hero.setParent(landing);

		Div heroText = new Div();
		heroText.setSclass("main2-landing-hero-text");
		heroText.setParent(hero);

		Label eyebrow = new Label(ais.common.Common.getBahasaConfig("Dashboard Utama"));
		eyebrow.setSclass("main2-landing-eyebrow");
		eyebrow.setParent(heroText);

		Label titleLabel = new Label("Selamat datang, " + main2DisplayName());
		titleLabel.setSclass("main2-landing-title");
		titleLabel.setParent(heroText);

		Label desc = new Label(
				ais.common.Common.getBahasaConfig("Pilih kartu dashboard untuk membuka pekerjaan utama tanpa deretan tab. Menu mengikuti hak akses Anda dan tetap memakai alur ZKoss yang sudah ada."));
		desc.setSclass("main2-landing-desc");
		desc.setParent(heroText);

		Div heroActions = new Div();
		heroActions.setSclass("row g-2 main2-landing-actions");
		heroActions.setParent(hero);

		addMain2LandingMetric(heroActions, "Akses", hakAkses == null ? "-" : safeText(hakAkses.getRoleName(), "Aktif"),
				"Hak akses yang sedang digunakan");
		addMain2LandingMetric(heroActions, "Tampilan", "ZK Landing", "Default baru tanpa tab terlihat");
		addMain2LandingMetric(heroActions, "Menu", "Responsif", "Nyaman di desktop dan mobile");

		Div grid = new Div();
		grid.setSclass("row g-3 main2-dashboard-card-grid");
		grid.setParent(landing);

		addMain2LandingCard(grid, "Pengumuman", "Baca informasi terbaru dari kampus atau sekolah.",
				"/img/svg/comment-2-text-line.svg", true, new EventListener() {
					public void onEvent(Event event) throws Exception {
						onPengumuman(event);
					}
				});
		addMain2LandingCard(grid, "e-Learning", "Buka kelas, materi, tugas, ujian, dan aktivitas belajar.",
				"/img/svg/book.svg", hakAkses != null && bool(hakAkses.getElearning()), new EventListener() {
					public void onEvent(Event event) throws Exception {
						onPengumumanPerkuliahan(event);
					}
				});
		addMain2LandingCard(grid, "Prestasi", "Pantau kegiatan, pencapaian, karya, dan riwayat pembinaan.",
				"/img/svg/trophy.svg", hakAkses != null && bool(hakAkses.getKegiatanDanPrestasi()),
				new EventListener() {
					public void onEvent(Event event) throws Exception {
						onKegiatanDanPrestasi(event);
					}
				});
		addMain2LandingCard(grid, "Pustaka", "Cari koleksi, lihat aktivitas pustaka, dan akses katalog.",
				"/img/svg/books-thin.svg", hakAkses != null && bool(hakAkses.getPustaka()), new EventListener() {
					public void onEvent(Event event) throws Exception {
						onPustaka(event);
					}
				});
		addMain2LandingCard(grid, "Pengajuan Anda", "Ikuti proses workflow dan pekerjaan yang perlu ditindaklanjuti.",
				"/img/svg/journal-arrow-up.svg", hakAkses != null && bool(hakAkses.getWorkflow()),
				new EventListener() {
					public void onEvent(Event event) throws Exception {
						onWorkflow(event);
					}
				});
		addMain2LandingCard(grid, "Akademik", "Lihat ringkasan akademik, aktivitas belajar, dan data utama.",
				"/img/svg/dashboard-speed.svg", hakAkses != null && bool(hakAkses.getDashboard()),
				new EventListener() {
					public void onEvent(Event event) throws Exception {
						onDashboard(event);
					}
				});
		addMain2LandingCard(grid, "Surat Menyurat", "Pantau surat masuk, surat keluar, dan alur persetujuan.",
				"/img/svg/journal-bookmark.svg", hakAkses != null && bool(hakAkses.getAdministrasi()),
				new EventListener() {
					public void onEvent(Event event) throws Exception {
						onAdministrasi(event);
					}
				});
		addMain2LandingCard(grid, "Pengadaan", "Pantau pengadaan, vendor, aset, dan kebutuhan inventaris.",
				"/img/svg/boxes.svg", hakAkses != null && bool(hakAkses.getPengadaan()), new EventListener() {
					public void onEvent(Event event) throws Exception {
						onPengadaan(event);
					}
				});
		addMain2LandingCard(grid, "Pembayaran", "Lihat pembayaran, tagihan, piutang, dan rincian transaksi.",
				"/img/svg/payments.svg", hakAkses != null && bool(hakAkses.getPembayaran()), new EventListener() {
					public void onEvent(Event event) throws Exception {
						onPembayaran(event);
					}
				});
		addMain2LandingCard(grid, "Keuangan", "Pantau posisi keuangan dan ringkasan penerimaan.",
				"/img/svg/money-bills.svg", hakAkses != null && bool(hakAkses.getKeuangan()), new EventListener() {
					public void onEvent(Event event) throws Exception {
						onKeuangan(event);
					}
				});
		addMain2LandingCard(grid, "Akuntansi", "Buka laporan akuntansi, buku besar, dan neraca lajur.",
				"/img/svg/file-earmark-text.svg",
				hakAkses != null && bool(hakAkses.getAkunting()) && tbmuser.getSiswa() == null
						&& tbmuser.getMahasiswa() == null,
				new EventListener() {
					public void onEvent(Event event) throws Exception {
						onAkunting(event);
					}
				});
		addMain2LandingCard(grid, "Kepegawaian", "Pantau data pegawai, status kerja, dan informasi SDM.",
				"/img/svg/user-tie.svg", hakAkses != null && bool(hakAkses.getKepegawaian()), new EventListener() {
					public void onEvent(Event event) throws Exception {
						onKepegawaian(event);
					}
				});
		addMain2LandingCard(grid, "Kinerja", "Lihat capaian, evaluasi, dan aktivitas pekerjaan utama.",
				"/img/svg/card-checklist.svg", hakAkses != null && bool(hakAkses.getKinerja()), new EventListener() {
					public void onEvent(Event event) throws Exception {
						onKinerja(event);
					}
				});
		addMain2LandingCard(grid, "Presensi", "Buka ringkasan kehadiran dan aktivitas presensi.",
				"/img/svg/check-circled-outline.svg", hakAkses != null && bool(hakAkses.getPresensiKehadiran()),
				new EventListener() {
					public void onEvent(Event event) throws Exception {
						onPresensi(event);
					}
				});
		addMain2LandingCard(grid, "Kalender Akademik", "Lihat jadwal akademik dan agenda penting.",
				"/img/svg/list-task.svg", hakAkses != null && bool(hakAkses.getKalenderAkademik()),
				new EventListener() {
					public void onEvent(Event event) throws Exception {
						onInformasiKalenderAkademik(event);
					}
				});
		addMain2LandingCard(grid, "Info Kegiatan", "Lihat jadwal kegiatan dan informasi agenda terbaru.",
				"/img/svg/information-circle-outline.svg", hakAkses != null && bool(hakAkses.getInfoKegiatan()),
				new EventListener() {
					public void onEvent(Event event) throws Exception {
						onInformasiKegiatan(event);
					}
				});

		try {
			if (panel_home.getChildren() != null && !panel_home.getChildren().isEmpty()) {
				panel_home.insertBefore(landing, (Component) panel_home.getChildren().get(0));
			} else {
				landing.setParent(panel_home);
			}
		} catch (Exception e) {
			landing.setParent(panel_home);
		}
	}

	private void addMain2LandingMetric(Component parent, String label, String value, String description) {
		Div box = new Div();
		box.setSclass("card shadow-sm border-0 main2-landing-metric");
		box.setParent(parent);

		Label valueLabel = new Label(safeText(value, "-"));
		valueLabel.setSclass("main2-landing-metric-value");
		valueLabel.setParent(box);

		Label labelText = new Label(safeText(label, ""));
		labelText.setSclass("main2-landing-metric-label");
		labelText.setParent(box);

		Label desc = new Label(safeText(description, ""));
		desc.setSclass("main2-landing-metric-desc");
		desc.setParent(box);
	}

	private void addMain2LandingCard(Component parent, String label, String description, String image, boolean visible,
			EventListener listener) {
		if (!visible || parent == null) {
			return;
		}

		Div card = new Div();
		card.setSclass("card h-100 shadow-sm border-0 main2-dashboard-card");
		card.setParent(parent);
		try {
			card.addEventListener("onClick", listener);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Div iconWrap = new Div();
		iconWrap.setSclass("rounded-circle d-flex align-items-center justify-content-center main2-dashboard-card-icon");
		iconWrap.setParent(card);
		Html faIcon = new Html("<span class=\"" + fontAwesomeClass(label)
				+ " main2-dashboard-card-fa\" aria-hidden=\"true\"></span>");
		faIcon.setParent(iconWrap);

		Div text = new Div();
		text.setSclass("main2-dashboard-card-body");
		text.setParent(card);

		Label title = new Label(safeText(label, "Menu"));
		title.setSclass("main2-dashboard-card-title");
		title.setParent(text);

		Label desc = new Label(safeText(description, ""));
		desc.setSclass("main2-dashboard-card-desc");
		desc.setParent(text);

		Label arrow = new Label(ais.common.Common.getBahasaConfig("Buka >"));
		arrow.setSclass("main2-dashboard-card-action");
		arrow.setParent(card);
	}

	private String fontAwesomeClass(String label) {
		String key = label == null ? "" : label.toLowerCase();
		if (key.indexOf("pengumuman") >= 0) return "fas fa-bullhorn";
		if (key.indexOf("learning") >= 0) return "fas fa-book-open";
		if (key.indexOf("prestasi") >= 0) return "fas fa-trophy";
		if (key.indexOf("pustaka") >= 0) return "fas fa-book";
		if (key.indexOf("pengajuan") >= 0 || key.indexOf("workflow") >= 0) return "fas fa-project-diagram";
		if (key.indexOf("akademik") >= 0) return "fas fa-university";
		if (key.indexOf("surat") >= 0 || key.indexOf("administrasi") >= 0) return "fas fa-envelope-open-text";
		if (key.indexOf("pengadaan") >= 0) return "fas fa-boxes";
		if (key.indexOf("pembayaran") >= 0) return "fas fa-money-check-alt";
		if (key.indexOf("keuangan") >= 0) return "fas fa-wallet";
		if (key.indexOf("akuntansi") >= 0) return "fas fa-file-invoice-dollar";
		if (key.indexOf("kepegawaian") >= 0) return "fas fa-user-tie";
		if (key.indexOf("kinerja") >= 0) return "fas fa-chart-line";
		if (key.indexOf("presensi") >= 0) return "fas fa-user-check";
		if (key.indexOf("kalender") >= 0) return "fas fa-calendar-alt";
		if (key.indexOf("info") >= 0) return "fas fa-info-circle";
		return "fas fa-th-large";
	}

	private String main2DisplayName() {
		try {
			if (tbmuser != null && tbmuser.getUserNama() != null && tbmuser.getUserNama().trim().length() > 0) {
				return tbmuser.getUserNama().trim();
			}
			if (tbmuser != null && tbmuser.getUserId() != null && tbmuser.getUserId().trim().length() > 0) {
				return tbmuser.getUserId().trim();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return "Pengguna";
	}

	private String safeText(String value, String fallback) {
		if (value == null || value.trim().length() == 0) {
			return fallback == null ? "" : fallback;
		}
		return value.trim();
	}

	private void initData() throws Exception {
		Common.clear(navigasi);
		boolean mobileAndroid = login != null && login.getLinkProfile() != null
				&& login.getLinkProfile().equalsIgnoreCase("Login via mobile");
		mobile = Common.isMobile();
		boolean langsungPengumuman = false;

		PerguruanTinggi ptAktif = PerguruanTinggiUtil.getPerguruanTinggi();
		Sekolah sekolahAktif = SekolahUtil.getSekolah();
		Yayasan yayasanAktif = SekolahUtil.getYayasan();

		Session hibernateSession = null;
		List<PengumumanAkademis> listPengumumanAkademis = new ArrayList<PengumumanAkademis>();

		try {
			hibernateSession = HibernateUtil.currentNativeSession();
			listPengumumanAkademis = ConstantValues.simpleList(
					TampilanPengumumanAkademisAction.initCriteriaStatic(true, tbmuser, ptAktif, null, hibernateSession)
							.setMaxResults(Common.ROWS_COUNT_ON_PAGE_1),
					PengumumanAkademis.class);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (hibernateSession != null && hibernateSession.isOpen()) {
				try {
					hibernateSession.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction2.java:3069");
				}
			}
			HibernateUtil.closeSession();
		}

		pengumumanAkademis = !listPengumumanAkademis.isEmpty() ? listPengumumanAkademis.get(0) : null;

		if (tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
				|| tbmuser.ambilDosen() != null || tbmuser.ambilGuru() != null || tbmuser.ambilYayasan() != null)) {
			if (mobile) {
				if (pengumumanAkademis != null && (KategoriPengumuman.PENGUMUMAN_UTAMA != null
						&& pengumumanAkademis.getKategoriPengumuman() != null
						&& pengumumanAkademis.getTampilkanPengumumanLain() && KategoriPengumuman.PENGUMUMAN_UTAMA
								.getId().equals(pengumumanAkademis.getKategoriPengumuman().getId()))) {
					Rows rows = TampilanPengumumanAkademisAction.tampil(panel_home, pengumumanAkademis, sekolahAktif,
							tbmuser, ptAktif, null, false, true, new EventListener() {
								public void onEvent(Event arg0) throws Exception {
									TampilanPengumumanAkademisAction.prosess(
											((PengumumanAkademis) arg0.getTarget().getAttribute("akademis")).getId(),
											tabs, tabpanels, headerHbox);
								}
							});
					MyFormRow rowLagi = new MyFormRow();
					rowLagi.setParent(rows);
					ProfileAction.initProfile(tbmuser, rowLagi, pengumumanAkademis);
				} else {
					if (pengumumanAkademis == null || !pengumumanAkademis.getTampilkanProfile())
						ProfileAction.initProfile(tbmuser, panel_home, pengumumanAkademis);
				}
			} else {
				if (pengumumanAkademis != null && (KategoriPengumuman.PENGUMUMAN_UTAMA != null
						&& pengumumanAkademis.getKategoriPengumuman() != null
						&& pengumumanAkademis.getTampilkanPengumumanLain() && KategoriPengumuman.PENGUMUMAN_UTAMA
								.getId().equals(pengumumanAkademis.getKategoriPengumuman().getId()))) {
					TampilanPengumumanAkademisAction.tampil(panel_home, pengumumanAkademis, sekolahAktif, tbmuser,
							ptAktif, null, false, true, new EventListener() {
								public void onEvent(Event arg0) throws Exception {
									TampilanPengumumanAkademisAction.prosess(
											((PengumumanAkademis) arg0.getTarget().getAttribute("akademis")).getId(),
											tabs, tabpanels, headerHbox);
								}
							});
				} else {
					panel_home.appendChild(new MyInclude("/pages/main/tampilan_pengumuman_akademis.zul"));
					langsungPengumuman = true;
				}
			}
		} else {
			if (pengumumanAkademis != null && (KategoriPengumuman.PENGUMUMAN_UTAMA != null
					&& pengumumanAkademis.getKategoriPengumuman() != null
					&& pengumumanAkademis.getTampilkanPengumumanLain() && KategoriPengumuman.PENGUMUMAN_UTAMA.getId()
							.equals(pengumumanAkademis.getKategoriPengumuman().getId()))) {
				TampilanPengumumanAkademisAction.tampil(panel_home, pengumumanAkademis, sekolahAktif, tbmuser, ptAktif,
						null, false, true, new EventListener() {
							public void onEvent(Event arg0) throws Exception {
								TampilanPengumumanAkademisAction.prosess(
										((PengumumanAkademis) arg0.getTarget().getAttribute("akademis")).getId(), tabs,
										tabpanels, headerHbox);
							}
						});
			} else {
				panel_home.appendChild(new MyInclude("/pages/main/tampilan_pengumuman_akademis.zul"));
				langsungPengumuman = true;
			}
		}

		renderMain2LandingDashboard();

		if (headerHbox != null && (mobile || mobileAndroid)) {
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
				grid.setSclass("dgrid");
				grid.setOddRowSclass("non-odd");
				headerHbox.setHeight("230px");
				grid.setHeight("230px");
				grid.setStyle("border:0px;background: transparent;");
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
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);
				row.appendChild(image);

				headerHbox.setSclass("headerHbox_mobile");

				Label title = new Label(judul);
				Label motto = new Label(Motto);
				Label alamat = new Label(Alamat1 + ", Telp. " + Telepon + ", Email : " + Email);
				title.setStyle("font-size: 14px;");
				title.setSclass("title1");
				motto.setStyle("font-size: 12px;");
				motto.setSclass("motto");
				alamat.setStyle("font-size: 9px;");
				alamat.setSclass("alamat");

				row = new MyFormRow();
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);
				row.appendChild(title);
				row = new MyFormRow();
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);
				row.appendChild(motto);
				row = new MyFormRow();
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);
				row.appendChild(alamat);
				image.setHeight("58px");

				row = new MyFormRow();
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);

				Grid gridsub = new Grid();
				gridsub.setSclass("dgrid");
				gridsub.setOddRowSclass("non-odd");
				gridsub.setWidth("100%");
				gridsub.setStyle("border:0px;background: transparent;");
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
				rowsub_el.setStyle("border:0px;background: transparent;");
				rowsub_el.setParent(rowssub);

				MyToolbarbutton toolbarbuttonMenu = new MyToolbarbutton("fa-bars fa-big", "");
				toolbarbuttonMenu.setSclass("user_button_profile");
				treeitemUtama = new Popup();
				treeitemUtama.setWidth("290px");
				treeitemUtama.setHeight("80%");
				treeitemUtama.setParent(page.getFirstRoot());
				toolbarbuttonMenu.setPopup(treeitemUtama);

				Hbox hbox = new Hbox();
				rowsub_el.appendChild(hbox);
				hbox.appendChild(toolbarbuttonMenu);
				notif(hbox);

				List<MyMenuitem> toolbarbuttons = new ArrayList<MyMenuitem>();

				MyMenuitem menuitemKeluar = new MyMenuitem("Keluar", "/img/svg/power.svg");
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
					MyMenuitem menuitemVersi = new MyMenuitem("Versi Mobile dan Desktop", "/img/svg/android-logo-thin.svg");
					menuitemVersi.addEventListener("onClick", new EventListener() {
						public void onEvent(Event arg0) throws Exception {
							MyWindow w = new MyWindow();
							w.setHeight("99%");
							w.setWidth("400px");
							w.setClosable(true);
							w.setTitle("Aplikasi Versi Mobile dan Desktop");
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

				MyToolbarbutton rightButton = new MyToolbarbutton("fa-user fa-big", tbmuser.getUserNama() + " ("
						+ (tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleName()) + ")", true);
				rightButton.setSclass("user_button_profile");
				rowsub_el.appendChild(rightButton);
				Menupopup treeitem = new Menupopup();
				treeitem.setParent(page.getFirstRoot());
				rightButton.setPopup(treeitem);

				for (int i = toolbarbuttons.size() - 1; i >= 0; i--)
					treeitem.appendChild(toolbarbuttons.get(i));

				row = new MyFormRow();
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);
				usersOnline = new Toolbarbutton("Online user(s): 0");
				usersOnline.setStyle("font-size:8px;");
				usersOnline.setSclass("users_online_button");
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
				grid2.setSclass("dgrid");
				grid2.setOddRowSclass("non-odd");
				grid2.setStyle("border:0px;background: transparent;");
				grid2.setParent(headerHbox);
				Rows rowssub2 = new Rows();
				rowssub2.setParent(grid2);
				MyFormRow rowsub2 = new MyFormRow();
				rowsub2.setStyle("border:0px;background: transparent;");

				int size = 0, jumlahRowBaru = 0;
				for (MyToolbarbuttonKecilConfig component : myToolbarbuttonKecilConfigs) {
					if (component.isVisible()) {
						if (size % tampilPerRow == 0) {
							rowsub2 = new MyFormRow();
							rowsub2.setStyle("border:0px;background: transparent;");
							rowsub2.setParent(rowssub2);
							jumlahRowBaru++;
						}
						size++;
						component.setStyle("font-size:10px;font-size:9px;color:white;");
						component.setParent(rowsub2);
					}
				}
				myToolbarbuttonKecilConfigs.clear();

				if (jumlahRowBaru > 0) {
					row = new MyFormRow();
					row.setStyle("border:0px;background: transparent;");
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
			Common.clear(headerHboxButton);
			/* Pemilih Bahasa (Indonesia/English/Arab) — paling kiri pada bilah kanan header. */
			try {
				headerHboxButton.appendChild(ais.ui.util.BahasaSwitchHelper.buatComboBahasa());
			} catch (Exception eLang) { ais.common.ErrorAuditUtil.record(eLang, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction2.java:3598");
			}
			notif(headerHboxButton);
			MyToolbarbutton rightButton = new MyToolbarbutton("fa-user fa-big", tbmuser.getUserNama() + " ("
					+ (tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getRoleName()) + ")", true);
			headerHboxButton.appendChild(rightButton);
			rightButton.setSclass("user_button_profile");
			Menupopup treeitem = new Menupopup();
			treeitem.setParent(page.getFirstRoot());
			rightButton.setPopup(treeitem);

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

			MyMenuitem menuVersi = new MyMenuitem("Versi Mobile dan Desktop", "/img/svg/android-logo-thin.svg");
			menuVersi.addEventListener("onClick", new EventListener() {
				public void onEvent(Event arg0) throws Exception {
					MyWindow w = new MyWindow();
					w.setHeight("99%");
					w.setWidth("400px");
					w.setTitle("Aplikasi Versi Mobile dan Desktop");
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

		Tbmrole hakAkses = tbmuser == null ? null : tbmuser.hakAkses();
		if (hakAkses != null) {
			if (eLearningButton != null)
				eLearningButton.setVisible(hakAkses.getElearning());
			if (ePrestasiButton != null)
				ePrestasiButton.setVisible(hakAkses.getKegiatanDanPrestasi());
			if (ePustakaButton != null)
				ePustakaButton.setVisible(hakAkses.getPustaka());
			if (eAkademikButton != null)
				eAkademikButton.setVisible(hakAkses.getDashboard());
			if (eWorkflowButton != null)
				eWorkflowButton.setVisible(hakAkses.getWorkflow());
			if (eAdministrasiButton != null)
				eAdministrasiButton.setVisible(hakAkses.getAdministrasi());
			if (ePengadaanButton != null)
				ePengadaanButton.setVisible(hakAkses.getPengadaan());
			if (eKeuanganButton != null)
				eKeuanganButton.setVisible(hakAkses.getKeuangan());
			if (ePembayaranButton != null)
				ePembayaranButton.setVisible(hakAkses.getPembayaran());
			if (eKepegawaianButton != null)
				eKepegawaianButton.setVisible(hakAkses.getKepegawaian());
			if (eAkuntingButton != null)
				eAkuntingButton.setVisible(hakAkses.getAkunting());
			if (eKinerjaButton != null)
				eKinerjaButton.setVisible(hakAkses.getKinerja());
			if (ePresensiButton != null)
				ePresensiButton.setVisible(hakAkses.getPresensiKehadiran());
			if (eKalenderAkademikButton != null)
				eKalenderAkademikButton.setVisible(hakAkses.getKalenderAkademik());
			if (eInfoKegiatanButton != null)
				eInfoKegiatanButton.setVisible(hakAkses.getInfoKegiatan());
		}

		if (popupmenu != null && iframe != null) {
			iframe.addEventListener("onClick", new EventListener() {
				public void onEvent(Event arg0) {
					popupmenu.close();
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
					PelanggaranMahasiswaAction.checkDanTampil(tbmuser.getMahasiswa());
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
					PelanggaranSiswaAction.checkDanTampil(tbmuser.getSiswa());
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
			if (!mobile)
				rowPencarian = Common.tampilanScroll1(navigasi == null ? navigasicenter : navigasi);
			else {
				if (popupmenu != null)
					popupmenu.setWidth("310px");
				rowPencarian = navigasicenter != null ? Common.tampilanScroll1(navigasicenter)
						: Common.tampilanScroll1(popupmenu);
			}
		}

		rowPencarian.setStyle("border:0px;background: transparent;");
		rowPencarian.getGrid().setStyle("border:0px;background: transparent;");

		rowDicari = new MyFormRow();
		rowDicari.setStyle("border:0px;background: transparent;");
		rowDicari.setParent(rowPencarian.getParent());
		rowPengumuman = new MyFormRow();
		rowPengumuman.setStyle("border:0px;background: transparent;");
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
	}

	private void notif(Component component) {
		if (component == null)
			return;
		if (tbmuser != null && tbmuser.getUserId() != null
				&& Common.bolehKonfigurasi("tampilkan_notif_di_dafboard")) {
			final MyToolbarbutton toolbarbuttonBell = new MyToolbarbutton("fa-bell fa-big", "Info");
			toolbarbuttonBell.setSclass("user_button_profile");
			component.appendChild(toolbarbuttonBell);

			Common.createDefaultTimer(new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Menupopup treeitem = new Menupopup();
					treeitem.setParent(page.getFirstRoot());
					toolbarbuttonBell.setPopup(treeitem);

					// Dibaca dari cache multi-level (L1/L2/L3) yang dihangatkan saat startup.
					// Memakai Item agar diketahui id + status dibaca/belum.
					List<ais.common.NotifikasiCache.Item> notifikasis = new ArrayList<ais.common.NotifikasiCache.Item>();
					try {
						notifikasis.addAll(ais.common.NotifikasiCache.itemLonceng(tbmuser.getUserId(), 20));
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}

					if (notifikasis.isEmpty()) {
						new MyMenuitem("Tidak ada informasi", "/img/svg/check2.svg").setParent(treeitem);
					} else {
						Set<String> keys = new HashSet<String>();
						int count = 0;
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
									/* Status baca PER-PENERIMA untuk pengguna yang sedang masuk. */
									final String userIdNotif = tbmuser.getUserId();
									final boolean sudahDibaca = ais.common.NotifikasiCache.sudahDibacaOleh(notifId,
											userIdNotif);
									String objKet = classData.optString("object_keterangan", "");
									GeneralValueObject gvoRaw = ConstantValues.ambil(classData.optString("name", ""),
											ais.common.JsonCompatUtil.optLong(classData, "id", 0L));
									GeneralValueObject gvoAttached = gvoRaw;
									try {
										if (gvoRaw != null && gvoRaw.getId() != null) {
											String entityName = gvoRaw.getClass().getName().split("_")[0];
											Object fresh = ais.database.hibernate.HibernateUtil.currentSession().get(entityName, gvoRaw.getId());
											if (fresh instanceof GeneralValueObject) { gvoAttached = (GeneralValueObject) fresh; }
										}
									} catch (Exception eAttach) { ais.common.ErrorAuditUtil.record(eAttach, "auto-audit(empty-catch) src/ais/action/maintenance/MainAction2.java:3934");}
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
											/* Sekali diklik: tandai SUDAH DIBACA + ubah tampilan jadi redup. */
											if (!sudahDibaca) {
												try {
													ais.common.NotifikasiCache.tandaiSudahDibaca(notifId, userIdNotif);
													menuitem.setSclass("menu_item ais-notif-item ais-notif-dibaca");
												} catch (Exception eTandai) {
													ais.common.ErrorAuditUtil.record(eTandai,
															"tandai notifikasi dibaca (lonceng MainAction2)");
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
													TampilanAlurSopAction.prosess(((ais.database.model.sop.DisposisiSop) gvo).getId(), null, null, true, arg0.getTarget());
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
						keys.clear();
					}
					notifikasis.clear();
				}
			});
		}
	}

	private void initMenuPengumuman(final Textbox cari) {
		if (pengumumanAkademis == null || !pengumumanAkademis.getTampilkanPengumumanLain()) {
			if (Common.bolehKonfigurasi("aktifkan_menu_baru_untuk_pengguna", Konfigurasi.TIDAK_AKTIF) || Common.isAsliMobile()) {
				Common.clear(rowPengumuman);
				Rows rows = new Rows();
				Grid grid = new Grid();
				grid.setParent(rowPengumuman);
				grid.appendChild(rows);

				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
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
					}, criteria, tbmuser, null, mobile, "border:0px;background:" + menuBgColor + " repeat-x 0 0;");
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/maintenance/MainAction2.java:4059");
				} finally {
					if (session != null) {
						try {
							session.close();
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					}
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

		MyLabelConfig c = new MyLabelConfig("Cari:");
		c.setStyle("font-size:11px;");
		c.setSclass("cari_menu");

		final Textbox cari = new Textbox();
		cari.setCols(18);
		hbox.appendChild(cari);
		MyToolbarbuttonConfig config = new MyToolbarbuttonConfig("", "/img/svg/search_menu.svg");
		hbox.appendChild(config);

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
			tree.setZclass("z-dottree");
			tree.setStyle("border-color:#EEEEEE;border:0px;background:" + menuBgColor + " repeat-x 0 0;");
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
			if (usersOnline != null) {
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
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/maintenance/MainAction2.java:4206");
		}
	}

	private void closeNavigasi() {
		try {
			if (navigation != null)
				navigation.setOpen(false);
			if (popupmenu != null)
				popupmenu.close();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}
}

