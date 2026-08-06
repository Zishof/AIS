package ais.action.master;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.keuangan.DashboardPembayaranMahasiswaPerBulan;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.helper.KegiatanProsesHeper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.TagihanUIBuilder;
import ais.action.report.CommonReportHelper;
import ais.common.AsyncTaskManager;
import ais.common.AsyncTaskManager.BackgroundTask;
import ais.common.AsyncTaskManager.UITask;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.IndonesianNumberToWords;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailKegiatan;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyHboxStyled;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class InformasiPembayaranMahasiswaAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 4155860737880329036L;
	private static final String KONFIG_TAMPILKAN_RINGKASAN_KESELURUHAN =
			"tampilkan_ringkasan_keseluruhan_informasi_pembayaran_mahasiswa";

	MyWindow win;
	private TreeMap<String, Kegiatan> kegiatans;
	private Mahasiswa mahasiswa;

	private MyGrid fotoGrid;
	private MyColumnConfig columnBulan;
	
	// 'west' digunakan sebagai wadah form tagihan / rincian pembayaran
	private Div west;

	private JenisKegiatan selectedJenisKegiatan = null;
	private Mahasiswa mhs = null;
	private BiodataCalonMahasiswa calMhs = null;
	private TreeMap<String, Object[]> semua = new TreeMap<String, Object[]>();

	private String lastSelectedJkLabel = null;
	private JenisKegiatan lastSelectedJenisKegiatan = null;
	private Integer lastSelectedSmtMulai = null;
	private Integer lastSelectedSmtSampai = null;

	private Div divDasbor;
	private Vbox vboxRinciOverlay;

	// Memoisasi "Tagihan SEGAR" per-kegiatan untuk SATU kali muat halaman. tagihanSegarKonsisten()
	// bersifat READ-ONLY & deterministik (input k → hasil sama), namun pada satu load dipanggil
	// ~3× per kegiatan (loop segar loadKegiatan + collectDashboardRekapData + KegiatanRenderer),
	// dan tiap panggilan mengulang fan-out getDetailBiayaMahasiswa + ambilDetailKegiatanSaja. Cache
	// per (id kegiatan) menekan itu jadi 1× per kegiatan. DIBERSIHKAN di awal loadKegiatan agar tidak
	// pernah basi antar-muat. Nilai Double.NaN = penanda "sudah dihitung namun null".
	private final java.util.Map<Long, Double> segarCachePerMuat =
			new java.util.concurrent.ConcurrentHashMap<Long, Double>();

	public String getLastSelectedJkLabel() {
		return lastSelectedJkLabel;
	}

	public void setLastSelectedJkLabel(String lastSelectedJkLabel) {
		this.lastSelectedJkLabel = lastSelectedJkLabel;
	}

	public JenisKegiatan getLastSelectedJenisKegiatan() {
		return lastSelectedJenisKegiatan;
	}

	public void setLastSelectedJenisKegiatan(JenisKegiatan lastSelectedJenisKegiatan) {
		this.lastSelectedJenisKegiatan = lastSelectedJenisKegiatan;
	}

	public Integer getLastSelectedSmtMulai() {
		return lastSelectedSmtMulai;
	}

	public void setLastSelectedSmtMulai(Integer lastSelectedSmtMulai) {
		this.lastSelectedSmtMulai = lastSelectedSmtMulai;
	}

	public Integer getLastSelectedSmtSampai() {
		return lastSelectedSmtSampai;
	}

	public void setLastSelectedSmtSampai(Integer lastSelectedSmtSampai) {
		this.lastSelectedSmtSampai = lastSelectedSmtSampai;
	}

	private static void cleanupSession(Session session) {
		if (session != null) {
			try { session.clear(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			try { session.disconnect(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			try { session.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		win = (MyWindow) comp;
		if (execution.getParameter("mahasiswa") != null) {
			mhs = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
					Long.parseLong(execution.getParameter("mahasiswa")));
		} else if (execution.getParameter("calonMahasiswa") != null) {
			calMhs = (BiodataCalonMahasiswa) ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(),
					Long.parseLong(execution.getParameter("calonMahasiswa")));
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			if (execution.getParameter("selectedJenisKegiatan") != null) {
				long paramJkId = Long.parseLong(execution.getParameter("selectedJenisKegiatan"));
				selectedJenisKegiatan = (JenisKegiatan) ConstantValues.ambil(JenisKegiatan.class.getName(), paramJkId);
				if (selectedJenisKegiatan != null) {
					this.lastSelectedJenisKegiatan = selectedJenisKegiatan;
					this.lastSelectedJkLabel = selectedJenisKegiatan.getNamaKegiatan();
				}
			} else {
				selectedJenisKegiatan = null;
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			cleanupSession(session);
		}

		if (mhs == null && calMhs == null) {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser == null || tbmuser.getMahasiswa() == null) {
				// KE-1: akses tanpa konteks mahasiswa yang valid (mis. GET LANGSUNG ke .zul tanpa login).
				// Jangan hanya alert()+return karena itu meninggalkan <window> KOSONG; pada page-load penuh
				// (DHtmlLayoutServlet) halaman kosong ini memicu NullPointerException saat PageImpl.redraw.
				// Render pesan ke dalam window agar struktur halaman tetap sah dan bisa di-redraw.
				try {
					Common.clear(win);
					org.zkoss.zul.Label pesan = new org.zkoss.zul.Label(
							ais.common.Common.getBahasaConfig("Anda harus login sebagai mahasiswa untuk melihat informasi pembayaran."));
					pesan.setStyle("display:block;padding:24px;color:#b45309;font-weight:bold;");
					pesan.setParent(win);
				} catch (Exception ignoreMsg) { ais.common.ErrorAuditUtil.record(ignoreMsg, "auto-audit(empty-catch) src/ais/action/master/InformasiPembayaranMahasiswaAction.java:219");
				}
				return;
			}
			mahasiswa = tbmuser.getMahasiswa();
		} else {
			mahasiswa = mhs;
		}

		ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("info_pembayaran_mahasiswa",
				"Memuat Pembayaran Mahasiswa",
				"Menyiapkan tagihan, riwayat pembayaran, ringkasan semester, dan visual dashboard.", 5);
		AsyncTaskManager.createDefaultTimerTimeout("Sedang mengambil data tagihan dan pembayaran", new BackgroundTask() {
			@Override
			public Object doInBackground() throws Exception {
				return null;
			}
		}, new UITask() {
			@Override
			public void updateUI(Object backgroundResult) throws Exception {
				try {
					ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("info_pembayaran_mahasiswa",
						"Memuat Pembayaran Mahasiswa",
						"Menyusun panel tagihan, proses pembayaran, bukti pembayaran, tabungan, dan histori.", 42);
					init(false);
					ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("info_pembayaran_mahasiswa",
						"Dashboard Pembayaran Siap",
						"Data pembayaran, tren, radar, dan tabel rincian berhasil ditampilkan.", 100);
				} finally {
					ais.ui.util.KeuanganDashboardEnhanceUtil.hideFloatingProgress("info_pembayaran_mahasiswa");
				}
			}
		});
	}

	/**
	 * PERMINTAAN: buka layar Informasi Pembayaran Mahasiswa LANGSUNG sebagai MyWindow, TANPA
	 * lewat .zul/iframe/URL query-string (pola sama seperti SetingBiayaAction.onAddExternal).
	 * Objek mahasiswa/calon-mahasiswa dioper LANGSUNG sebagai parameter Java (bukan di-parse dari
	 * "execution.getParameter(...)" hasil ID di URL) -- menghilangkan seluruh jalur "ID salah
	 * ter-embed di string URL saat iframe dibuka" yang dicurigai sbg penyebab kasus data
	 * mahasiswa tertukar (mis. tagihan Aulia menampilkan data Indra Narta). Karena ini panggilan
	 * in-process (sama thread/desktop dgn tombol yg mengekliknya, bukan request HTTP baru ke
	 * .zul terpisah), tidak ada lagi celah string/serialisasi di mana ID bisa keliru terbaca.
	 *
	 * @param mahasiswaTarget         mahasiswa yang tagihannya ingin dilihat (null bila calon mahasiswa)
	 * @param calonMahasiswaTarget    calon mahasiswa yang tagihannya ingin dilihat (null bila mahasiswa)
	 * @param selectedJenisKegiatanTarget jenis kegiatan yang ingin langsung dipilih (boleh null)
	 */
	public static InformasiPembayaranMahasiswaAction onViewExternal(Mahasiswa mahasiswaTarget,
			BiodataCalonMahasiswa calonMahasiswaTarget, JenisKegiatan selectedJenisKegiatanTarget) throws Exception {
		return onViewExternal(mahasiswaTarget, calonMahasiswaTarget, selectedJenisKegiatanTarget, null);
	}

	/**
	 * Varian dengan callback "onClose" -- dipakai pemanggil yang perlu menyegarkan tampilannya
	 * sendiri setelah jendela ini ditutup (mis. ProfileMahasiswa yang sebelumnya memakai
	 * {@code displayWindowIframe(...).addEventListener("onClose", ...)}).
	 */
	public static InformasiPembayaranMahasiswaAction onViewExternal(Mahasiswa mahasiswaTarget,
			BiodataCalonMahasiswa calonMahasiswaTarget, JenisKegiatan selectedJenisKegiatanTarget,
			EventListener onCloseListener) throws Exception {
		Common.doCheckSecurity();

		final InformasiPembayaranMahasiswaAction action = new InformasiPembayaranMahasiswaAction();
		action.mhs = mahasiswaTarget;
		action.calMhs = calonMahasiswaTarget;
		action.selectedJenisKegiatan = selectedJenisKegiatanTarget;
		if (selectedJenisKegiatanTarget != null) {
			action.lastSelectedJenisKegiatan = selectedJenisKegiatanTarget;
			action.lastSelectedJkLabel = selectedJenisKegiatanTarget.getNamaKegiatan();
		}

		action.win = new MyWindow("Informasi Pembayaran Mahasiswa", "none", true);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(action.win);
		action.win.setHeight("92%");
		action.win.setWidth(Common.isMobile() ? "97%" : "92%");
		if (onCloseListener != null) {
			action.win.addEventListener("onClose", onCloseListener);
		}

		if (action.mhs == null && action.calMhs == null) {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser == null || tbmuser.getMahasiswa() == null) {
				Common.clear(action.win);
				org.zkoss.zul.Label pesan = new org.zkoss.zul.Label(ais.common.Common
						.getBahasaConfig("Anda harus login sebagai mahasiswa untuk melihat informasi pembayaran."));
				pesan.setStyle("display:block;padding:24px;color:#b45309;font-weight:bold;");
				pesan.setParent(action.win);
				action.win.setVisible(true);
				action.win.onModal();
				return action;
			}
			action.mahasiswa = tbmuser.getMahasiswa();
		} else {
			action.mahasiswa = action.mhs;
		}

		ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("info_pembayaran_mahasiswa",
				"Memuat Pembayaran Mahasiswa",
				"Menyiapkan tagihan, riwayat pembayaran, ringkasan semester, dan visual dashboard.", 5);
		AsyncTaskManager.createDefaultTimerTimeout("Sedang mengambil data tagihan dan pembayaran",
				new BackgroundTask() {
					@Override
					public Object doInBackground() throws Exception {
						return null;
					}
				}, new UITask() {
					@Override
					public void updateUI(Object backgroundResult) throws Exception {
						try {
							ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("info_pembayaran_mahasiswa",
									"Memuat Pembayaran Mahasiswa",
									"Menyusun panel tagihan, proses pembayaran, bukti pembayaran, tabungan, dan histori.",
									42);
							action.init(false);
							ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("info_pembayaran_mahasiswa",
									"Dashboard Pembayaran Siap",
									"Data pembayaran, tren, radar, dan tabel rincian berhasil ditampilkan.", 100);
						} finally {
							ais.ui.util.KeuanganDashboardEnhanceUtil.hideFloatingProgress("info_pembayaran_mahasiswa");
						}
					}
				});

		action.win.setVisible(true);
		action.win.onModal();
		return action;
	}

	private void executeAfterRincianLoaded(final boolean finalRefresh, final Runnable action) {
		if (vboxRinciOverlay != null && vboxRinciOverlay.getParent() != null) {
			Common.clear(west); 
			try {
				if (mahasiswa != null) {
					TagihanUIBuilder.loadTagihan(west, selectedJenisKegiatan, mhs, mahasiswa, semua, Common.isMobile(),
							false, null, finalRefresh, InformasiPembayaranMahasiswaAction.this);
				} else if (calMhs != null) {
					Integer SMT = null;
					if (selectedJenisKegiatan != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
							&& selectedJenisKegiatan.getId()
									.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
						SMT = 0;
					}
					TagihanUIBuilder.loadTagihan(west, selectedJenisKegiatan, calMhs, calMhs, semua, Common.isMobile(),
							false, SMT, finalRefresh, InformasiPembayaranMahasiswaAction.this);
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		final org.zkoss.zul.Timer pollingTimer = new org.zkoss.zul.Timer();
		pollingTimer.setDelay(500); 
		pollingTimer.setRepeats(true);
		pollingTimer.setParent(win);

		pollingTimer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (!isTagihanLoading(west)) {
					pollingTimer.stop();
					pollingTimer.detach();
					org.zkoss.zk.ui.util.Clients.clearBusy();
					action.run();
				}
			}
		});
		pollingTimer.start();
	}

	private boolean isTagihanLoading(Component parent) {
		if (parent == null)
			return false;
		if (parent instanceof Progressmeter)
			return true;
		for (Object childObj : parent.getChildren()) {
			if (childObj instanceof Component) {
				if (isTagihanLoading((Component) childObj))
					return true;
			}
		}
		return false;
	}

	private void init(final boolean refresh) throws Exception {
		win.getFellowIfAny("window");
		Common.clear(win);

		final boolean tabsVisible = mhs == null && calMhs == null;
		final boolean vaVisible = tabsVisible && Konfigurasi.AKTIF.equals(
				Common.getKonfigurasi("virtual_account_muncul_di_halaman_mahasiswa", Konfigurasi.AKTIF).getNilai());

		final ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(
				Common.tampilanScrollTabbox(win), "100%", new int[] { 0 });

		org.zkoss.zul.Div panel1 = btnTab.tambahTab(0, "Dasbor & Tagihan Pembayaran", "/img/svg/dashboard-chart.svg");
		panel1.setStyle("overflow: auto;");

		Vbox mainVbox = new Vbox();
		mainVbox.setWidth("100%");
		mainVbox.setParent(panel1);

		// ==============================================================
		// BANNER CTA: PEMBAYARAN WIZARD (paling atas)
		// Tombol pemandu pembayaran berbasis popup. Hanya untuk mahasiswa
		// (mesin pembayaran daftarulang_mahasiswa_lama berbasis Mahasiswa).
		// Gerbang ON/OFF: Konfigurasi > Pembayaran Mahasiswa > "Wizard Pembayaran
		// Mahasiswa" — banner disembunyikan total bila dimatikan admin.
		// ==============================================================
		if (mahasiswa != null && ais.action.master.helper.WizardPembayaranMhsHelper.aktif()) {
			Div bannerWizard = new Div();
			bannerWizard.setWidth("100%");
			bannerWizard.setStyle("background:linear-gradient(135deg,#1e3a8a 0%,#2563eb 60%,#3b82f6 100%);"
					+ "border-radius:14px;padding:14px 18px;margin:6px 5px 4px 5px;display:flex;align-items:center;"
					+ "justify-content:space-between;gap:14px;flex-wrap:wrap;box-sizing:border-box;"
					+ "box-shadow:0 10px 24px rgba(37,99,235,.25);");
			bannerWizard.setParent(mainVbox);

			bannerWizard.appendChild(new org.zkoss.zul.Html(
					"<div style='display:flex;align-items:center;gap:14px;min-width:0;color:#fff;'>"
							+ "<div style='font-size:30px;line-height:1;'>🧾</div>"
							+ "<div style='min-width:0;'>"
							+ "<div style='font-size:17px;font-weight:800;letter-spacing:.2px;'>Pembayaran Wizard</div>"
							+ "<div style='font-size:12px;opacity:.92;line-height:1.45;'>Bayar tagihan langkah-demi-langkah: "
							+ "pilih jenis &amp; semester, centang tagihan, atur angsuran, lalu pilih cara bayar.</div>"
							+ "</div></div>"));

			// CTA SANGAT KONTRAS: tombol amber + animasi pulse agar menarik perhatian
			// mahasiswa. Dibuat sebagai Div ber-sclass (ZK Button mengabaikan style
			// inline karena tema bawaan → terlihat seperti kotak gelap; Div terkendali
			// penuh oleh CSS kita).
			bannerWizard.appendChild(new org.zkoss.zul.Html(
					"<style>"
							+ "@keyframes aisWizardPulse{0%,100%{box-shadow:0 8px 22px rgba(245,158,11,.5),"
							+ "0 0 0 0 rgba(251,191,36,.65);}50%{box-shadow:0 12px 30px rgba(245,158,11,.7),"
							+ "0 0 0 14px rgba(251,191,36,0);}}"
							+ ".ais-wizard-cta{background:linear-gradient(135deg,#fde047 0%,#f59e0b 100%);"
							+ "color:#7c2d12;font-weight:900;font-size:15px;letter-spacing:.3px;padding:14px 26px;"
							+ "border-radius:12px;border:2px solid #ffffff;cursor:pointer;white-space:nowrap;"
							+ "display:inline-flex;align-items:center;gap:9px;text-shadow:0 1px 0 rgba(255,255,255,.35);"
							+ "animation:aisWizardPulse 1.7s ease-in-out infinite;"
							+ "transition:transform .15s ease,filter .15s ease;-webkit-user-select:none;user-select:none;}"
							+ ".ais-wizard-cta:hover{transform:translateY(-2px) scale(1.04);filter:brightness(1.06);}"
							+ ".ais-wizard-cta:active{transform:translateY(0) scale(.98);}"
							+ "@media(max-width:760px){.ais-wizard-cta{width:100%;justify-content:center;}}"
							+ "</style>"));

			Div btnWizard = new Div();
			btnWizard.setSclass("ais-wizard-cta");
			btnWizard.setTooltiptext("Buka wizard pembayaran (popup)");
			btnWizard.appendChild(new org.zkoss.zul.Html(
					"<span style='font-size:20px;line-height:1;'>💳</span>"
							+ "<span>Mulai Wizard Pembayaran</span>"
							+ "<span style='font-size:20px;line-height:1;'>→</span>"));
			btnWizard.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					// Wizard generasi baru (WizardPembayaranMhsHelper): mandiri dari
					// DaftarUlang*Action, Step "Pilih Tagihan" berupa kartu murni sehingga
					// tidak kolaps di tampilan mobile seperti engine tersemat wizard lama.
					ais.action.master.helper.WizardPembayaranMhsHelper.buka(mahasiswa, new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							// Refresh layar setelah wizard ditutup agar status tagihan terbarui.
							init(true);
						}
					});
				}
			});
			bannerWizard.appendChild(btnWizard);
		}

		// ==============================================================
		// PORTAL LAYOUT UTAMA (PROFIL & AKSI)
		// ==============================================================
		MyPortallayout mainPortal = new MyPortallayout();
		mainPortal.setWidth("100%");
		mainPortal.setParent(mainVbox);

		String pcWidth = Common.isMobile() ? "100%" : "50%";

		MyPortalchildren pcTopLeft = new MyPortalchildren();
		pcTopLeft.setWidth(pcWidth);
		pcTopLeft.setStyle("padding: 5px;");
		pcTopLeft.setParent(mainPortal);

		MyPortalchildren pcTopRight = new MyPortalchildren();
		pcTopRight.setWidth(pcWidth);
		pcTopRight.setStyle("padding: 5px;");
		pcTopRight.setParent(mainPortal);

		// 1. PROFIL MAHASISWA (Masuk ke Kiri)
		Panel pnlProfil = new Panel();
		pnlProfil.setTitle("Profil Mahasiswa");
		pnlProfil.setBorder("normal");
		pnlProfil.setStyle("margin-bottom: 10px;");
		pnlProfil.setParent(pcTopLeft);

		Panelchildren pchProfil = new Panelchildren();
		pchProfil.setParent(pnlProfil);
		pchProfil.appendChild(new org.zkoss.zul.Html("<div style='font-size:11px;color:#64748b;line-height:1.45;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:8px;margin:8px;'>Data diri singkat untuk memastikan tagihan yang dibuka memang milik orang yang benar.</div>"));

		Grid gridProfil = new Grid();
		gridProfil.setSclass("dgrid fgrid");
		gridProfil.setWidth("100%");
		gridProfil.setParent(pchProfil);

		Rows rowsProfil = new Rows();
		rowsProfil.setParent(gridProfil);

		Hbox hboxBawah = new Hbox();
		Hbox hbox1 = new MyHboxStyled();

		if (mahasiswa != null) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rowsProfil);

			hbox1.setWidth("90%");
			hbox1.setParent(row);
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox1);
			hbox1.appendChild(new Space());
			hbox1.appendChild(new Space());

			Vbox vbox = new Vbox();
			vbox.setParent(hbox1);
			vbox.appendChild(new ais.ui.util.MyLabelConfig(mahasiswa.getNim()));
			vbox.appendChild(new ais.ui.util.MyLabelConfig(mahasiswa.getNama()));

			Hbox hbox2 = new Hbox();
			hbox2.setParent(vbox);
			mahasiswa.tampilkanHp(hbox2);
			mahasiswa.tampilkanEmail(hbox2);
			hbox1.appendChild(new Space());
			hboxBawah.setParent(vbox);

		} else if (calMhs != null) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rowsProfil);

			hbox1.setWidth("90%");
			hbox1.setParent(row);
			CommonMedia.tampilkanGambarKecil(calMhs).setParent(hbox1);
			hbox1.appendChild(new Space());
			hbox1.appendChild(new Space());

			Vbox vbox = new Vbox();
			vbox.setParent(hbox1);
			vbox.appendChild(new ais.ui.util.MyLabelConfig(calMhs.getNoRegistrasi()));
			vbox.appendChild(new ais.ui.util.MyLabelConfig(calMhs.getNama()));

			Hbox hbox2 = new Hbox();
			hbox2.setParent(vbox);
			calMhs.tampilkanHp(hbox2);
			calMhs.tampilkanEmail(hbox2);
			hbox1.appendChild(new Space());
			hboxBawah.setParent(vbox);
		}

		// 2. PANEL AKSI (Masuk ke Kanan Atas)
		Panel pnlAksi = new Panel();
		pnlAksi.setTitle("Aksi & Proses Pembayaran");
		pnlAksi.setBorder("normal");
		pnlAksi.setCollapsible(true);
		pnlAksi.setWidth("100%");
		pnlAksi.setStyle("margin-bottom: 10px;");
		pnlAksi.setParent(pcTopRight);

		Panelchildren pchAksi = new Panelchildren();
		pchAksi.setStyle("padding: 10px;");
		pchAksi.setParent(pnlAksi);
		pchAksi.appendChild(new org.zkoss.zul.Html("<div style='font-size:11px;color:#64748b;line-height:1.45;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:8px;margin-bottom:8px;'>Tombol cepat untuk memperbarui, mencetak, atau membuka dokumen pembayaran.</div>"));
		hboxBawah.setParent(pchAksi);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/settings_16x16.png");
		hboxBawah.appendChild(button);
		button.setOrient("horizontal");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				init(true);
			}
		});

		// ADDITIVE: pintu masuk eksperimental ke ais.action.master.helper.PembayaranOnlineMahasiswa
		// ("coba bayar via cara baru" — permintaan user). Tidak mengubah alur/tombol lain di layar ini.
		Tbmuser userAksiPembayaran = Common.getCurrentUser();
		boolean loginSebagaiMahasiswa = userAksiPembayaran != null && userAksiPembayaran.getMahasiswa() != null;
		if (!loginSebagaiMahasiswa) {
			MyToolbarbuttonConfig btnCobaCaraBaru = new MyToolbarbuttonConfig("Coba Cara Baru (Eksperimental)",
					"/img/svg/payments.svg");
			hboxBawah.appendChild(btnCobaCaraBaru);
			btnCobaCaraBaru.setOrient("horizontal");
			btnCobaCaraBaru.setStyle("color:#1d4ed8;font-weight:700;");
			btnCobaCaraBaru.setTooltiptext(
					"Coba layar pembayaran alternatif yang sedang dikembangkan (belum menggantikan alur yang ada).");
			btnCobaCaraBaru.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					ais.action.master.helper.PembayaranOnlineMahasiswa.onViewExternal(mahasiswa, calMhs);
				}
			});
		}

		// Tombol "Cetak Tagihan" & "Surat Bebas Tunggakan" DIKEMBALIKAN ke panel "Aksi & Proses
		// Pembayaran" ini (di-parent ke hboxBawah, lihat di bawah) agar berdampingan dengan Refresh
		// & Proses Tagihan — sesuai permintaan. Pembuatan tombolnya tetap di blok "Daftar Pembayaran
		// Keseluruhan" (butuh variabel mahasiswa/calMhs/refresh di sana), tetapi PARENT-nya hboxBawah.

		// ==============================================================
		// DIV TAGIHAN DILETAKKAN SEBELUM DIV DASBOR (AGAR POSISINYA DI BAWAH PROFIL & AKSI)
		// ==============================================================
		Div divTagihan = new Div();
		divTagihan.setWidth("100%");
		divTagihan.setParent(mainVbox);

		divDasbor = new Div();
		divDasbor.setWidth("100%");
		divDasbor.setParent(mainVbox);
		// ==============================================================

		// ==============================================================
		// PORTAL LAYOUT KEDUA (DAFTAR PEMBAYARAN KESELURUHAN & TAGIHAN MAHASISWA)
		// ==============================================================
		MyPortallayout portalTagihan = new MyPortallayout();
		portalTagihan.setWidth("100%");
		portalTagihan.setParent(divTagihan);

		MyPortalchildren pcTagihanKiri = new MyPortalchildren();
		pcTagihanKiri.setWidth(pcWidth);
		pcTagihanKiri.setStyle("padding: 5px;");
		pcTagihanKiri.setParent(portalTagihan);

		MyPortalchildren pcTagihanKanan = new MyPortalchildren();
		pcTagihanKanan.setWidth(pcWidth);
		pcTagihanKanan.setStyle("padding: 5px;");
		pcTagihanKanan.setParent(portalTagihan);

		Panel pnlDaftarTagihan = new Panel();
		pnlDaftarTagihan.setTitle("Daftar Pembayaran Keseluruhan");
		pnlDaftarTagihan.setBorder("normal");
		pnlDaftarTagihan.setCollapsible(true);
		pnlDaftarTagihan.setStyle("margin-bottom: 10px; width: 100%;");
		pnlDaftarTagihan.setParent(pcTagihanKiri);

		Panelchildren pchDaftarTagihan = new Panelchildren();
		pchDaftarTagihan.setParent(pnlDaftarTagihan);

		// Tombol "Hitung Ulang": proses ulang (Proses Tagihan) SEMUA kegiatan mahasiswa/calon
		// yang sedang ditampilkan, agar nilai tagihan TERSIMPAN sinkron dengan hitung-ulang
		// rincian (memperbaiki nilai basi/dobel di dasbor — beda DetailPembayaranMahasiswaRenderer
		// yang menghitung segar vs dasbor yang memakai nilai tersimpan). Memakai engine yang sama
		// dengan alur "Ubah Tagihan" DaftarUlang: KegiatanHelper.checkKegiatan{Mahasiswa|CalonMahasiswa}.
		Div barHitungUlang = new Div();
		barHitungUlang.setStyle("text-align:right;margin:6px 8px 0 8px;");
		barHitungUlang.setParent(pchDaftarTagihan);
		MyToolbarbuttonConfig btnHitungUlang = new MyToolbarbuttonConfig("Hitung Ulang", "/img/refresh.png");
		btnHitungUlang.setStyle("color:#b91c1c;font-weight:700;");
		btnHitungUlang.setTooltiptext("Hitung ulang nilai tagihan semua kegiatan (Proses Tagihan)");
		btnHitungUlang.setParent(barHitungUlang);
		btnHitungUlang.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev0) throws Exception {
				MyMessageboxConfig.show(
						"Apakah Bapak/Ibu yakin ingin menghitung ulang (Proses Tagihan) seluruh tagihan untuk mahasiswa ini? Seluruh nilai tagihan akan dihitung ulang berdasarkan pengaturan biaya yang berlaku saat ini agar sinkron dengan rincian terbaru. Data pembayaran yang telah tercatat tidak akan terpengaruh. Silakan tekan OK untuk melanjutkan, atau Batal untuk membatalkan.",
						"Konfirmasi Hitung Ulang", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
									return;
								}
								hitungUlangSemuaKegiatan();
							}
						});
			}
		});

		// Tombol staf: bersihkan item tagihan yang TAK SESUAI (bukan biaya berlaku & belum dibayar).
		Tbmuser uBersihAsing = Common.getCurrentUser();
		if (uBersihAsing != null && uBersihAsing.ambilDosen() == null && uBersihAsing.getMahasiswa() == null
			&& uBersihAsing.getBiodataCalonMahasiswa() == null) {
			MyToolbarbuttonConfig btnBersihAsing = new MyToolbarbuttonConfig("Bersihkan Item Tak Sesuai", "/img/delete.gif");
			btnBersihAsing.setStyle("color:#92400e;font-weight:700;");
			btnBersihAsing.setTooltiptext("Hapus item tagihan yang BUKAN biaya berlaku & belum dibayar (mis. item prodi lain yang nyangkut)");
			btnBersihAsing.setParent(barHitungUlang);
			btnBersihAsing.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event ev0) throws Exception {
					bersihkanItemTakSesuai();
				}
			});
		}

		// "Cetak Tagihan" & "Surat Bebas Tunggakan": di-parent ke hboxBawah (panel "Aksi & Proses
		// Pembayaran"), BUKAN ke barHitungUlang — dikembalikan ke posisi semula sesuai permintaan.
		// Datanya tetap dari rincian (semua) daftar kegiatan yang TAMPIL di grid bawah: report
		// dijalankan setelah executeAfterRincianLoaded, dan cek tunggakan memakai koleksi 'kegiatans'.
		if (mahasiswa != null) {
			MyToolbarbuttonConfig btnCetakTagihan = new MyToolbarbuttonConfig("Cetak Tagihan",
					"/img/Finance-Invoice-icon.png");
			btnCetakTagihan.setOrient("horizontal");
			btnCetakTagihan.setParent(hboxBawah);
			btnCetakTagihan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					org.zkoss.zk.ui.util.Clients.showBusy("Memverifikasi dan menyiapkan data tagihan...");
					executeAfterRincianLoaded(refresh, new Runnable() {
						@Override
						public void run() {
							try {
								CommonReportHelper.onLaporanTagihan(mahasiswa, semua);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}
					});
				}
			});

			MyToolbarbuttonConfig btnSuratBebas = new MyToolbarbuttonConfig("Surat Bebas Tunggakan",
					"/img/Document-Text-icon.png");
			btnSuratBebas.setOrient("horizontal");
			btnSuratBebas.setParent(hboxBawah);
			btnSuratBebas.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					org.zkoss.zk.ui.util.Clients.showBusy("Memverifikasi sisa tunggakan mahasiswa...");
					executeAfterRincianLoaded(refresh, new Runnable() {
						@Override
						public void run() {
							try {
								double totalTerhutangCek = 0.0;
								if (kegiatans != null && !kegiatans.isEmpty()) {
									for (Kegiatan k : kegiatans.values()) {
										double tagihanSmt = (k.getTagihan() == null ? 0.0 : k.getTagihan());
										double dibayarSmt = (k.getDibayar() == null ? 0.0 : k.getDibayar());
										totalTerhutangCek += (tagihanSmt - dibayarSmt);
									}
								}
								if (totalTerhutangCek > 0.1) {
									MyMessageboxConfig.showFormat(
											"Mohon maaf, Surat Bebas Tunggakan belum dapat dicetak karena sisa tunggakan Bapak/Ibu belum lunas, yaitu sebesar Rp {V1}. Langkah yang dapat dilakukan: (1) mohon lunasi terlebih dahulu sisa tunggakan yang tertera; (2) lakukan pembayaran melalui kanal yang tersedia; (3) setelah tunggakan lunas, silakan cetak kembali Surat Bebas Tunggakan.",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
											Common.numberFormat.get().format(totalTerhutangCek));
									return;
								}
								CommonReportHelper.onLaporanBebasTunggakan(mahasiswa, semua);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}
					});
				}
			});
		} else if (calMhs != null) {
			MyToolbarbuttonConfig btnCetakTagihanCalon = new MyToolbarbuttonConfig("Cetak Tagihan",
					"/img/Finance-Invoice-icon.png");
			btnCetakTagihanCalon.setOrient("horizontal");
			btnCetakTagihanCalon.setParent(hboxBawah);
			btnCetakTagihanCalon.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					org.zkoss.zk.ui.util.Clients.showBusy("Memverifikasi dan menyiapkan data tagihan...");
					executeAfterRincianLoaded(refresh, new Runnable() {
						@Override
						public void run() {
							try {
								CommonReportHelper.onLaporanTagihan(calMhs, semua);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}
					});
				}
			});
		}

		tampilkanHistoryStatusMahasiswa(mainVbox);

		pchDaftarTagihan.appendChild(new org.zkoss.zul.Html("<div style='font-size:11px;color:#64748b;line-height:1.45;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:8px;margin:8px;'>Semua kelompok tagihan dan pembayaran; klik satu baris untuk melihat rinciannya.</div>"));

		// Melemparkan pchDaftarTagihan (untuk List Pembayaran) dan pcTagihanKanan (untuk Panel Rincian)
		Combobox combobox = createListFoto(refresh, pchDaftarTagihan, pcTagihanKanan);

		Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig prosesUlang = KegiatanProsesHeper.prosesUlangTagihanCombo("Proses Tagihan",
				"/img/excel.png", combobox);
		prosesUlang.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null
				&& Common.bolehKonfigurasi("tampilkan_tombol_proses_tagihan"));
		hboxBawah.appendChild(prosesUlang);

		btnTab.tambahTabLazy(1, "Proses Pembayaran", "/img/svg/coin.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				if (mahasiswa == null) {
					return;
				}
				String url = "/common/daftarulang_mahasiswa_lama.zul?mahasiswa=" + mahasiswa.getId();
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%"); window.setWidth("100%"); window.setParent(panel);
				new MyInclude(url).setParent(window);
			}
		});
		btnTab.setVisibleTombol(1, tabsVisible);
		btnTab.tambahTabLazy(2, "Bukti Pembayaran", "/img/svg/check2-circle.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				if (mahasiswa == null) {
					return;
				}
				String url = "/pages/master/bukti_pembayaran.zul?mahasiswa=" + mahasiswa.getId();
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%"); window.setWidth("100%"); window.setParent(panel);
				new MyInclude(url).setParent(window);
			}
		});
		btnTab.setVisibleTombol(2, tabsVisible);
		btnTab.tambahTabLazy(3, "Tabungan Mahasiswa", "/img/svg/money-bills.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				if (mahasiswa == null) {
					return;
				}
				String url = "/pages/master/deposit.zul?mahasiswa=" + mahasiswa.getId();
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%"); window.setWidth("100%"); window.setParent(panel);
				new MyInclude(url).setParent(window);
			}
		});
		btnTab.setVisibleTombol(3, tabsVisible);
		btnTab.tambahTabLazy(4, "Sejarah Pembayaran", "/img/svg/chart-line.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				if (mahasiswa == null) {
					return;
				}
				String url = "/pages/master/log_pembayaran.zul?mahasiswa=" + mahasiswa.getId();
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%"); window.setWidth("100%"); window.setParent(panel);
				new MyInclude(url).setParent(window);
			}
		});
		btnTab.setVisibleTombol(4, tabsVisible);
		btnTab.tambahTabLazy(5, "Info Pembayaran", "/img/svg/person-lines-fill.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				DashboardPembayaranMahasiswaPerBulan laporanKHS = new DashboardPembayaranMahasiswaPerBulan();
				ais.ui.util.BaseDasbordPortal.mountWrapped(laporanKHS, panel,
					"Pembayaran per Bulan", "Tren pembayaran mahasiswa setiap bulan dalam satu tahun akademik.");
			}
		});
		btnTab.setVisibleTombol(5, tabsVisible);
		btnTab.tambahTabLazy(6, "Tagihan Virtual Account", "/img/svg/user-box-line.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
				MyWindow window = new MyWindow("", "none", false);
				window.setHeight("100%"); window.setWidth("100%"); window.setParent(panel);
				new MyInclude("/pages/master/virtual_account_bank.zul").setParent(window);
			}
		});
		btnTab.setVisibleTombol(6, vaVisible);
	}


	@SuppressWarnings("unchecked")
	private void tampilkanHistoryStatusMahasiswa(Component parent) {
		if (parent == null || mahasiswa == null) {
			return;
		}

		Panel panel = new Panel();
		panel.setTitle("Status Awal Mahasiswa per Semester");
		panel.setBorder("normal");
		panel.setCollapsible(true);
		panel.setStyle("margin:0 5px 10px 5px;width:100%;");
		panel.setParent(parent);

		Panelchildren body = new Panelchildren();
		body.setStyle("padding:8px;");
		body.setParent(panel);
		body.appendChild(new org.zkoss.zul.Html("<div style='font-size:11px;color:#64748b;line-height:1.45;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:8px;margin-bottom:8px;'>Riwayat status awal dan program per semester yang dipakai sistem saat memilih tarif billing. Jika nominal tagihan berubah tanpa perubahan tarif utama, periksa perubahan pada baris semester terkait melalui tombol Revisi.</div>"));

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(body);

		Columns columns = new Columns();
		columns.setParent(grid);
		new MyColumnConfig("Tahun Akademik").setParent(columns);
		new MyColumnConfig("Smt").setParent(columns);
		new MyColumnConfig("Jenis").setParent(columns);
		new MyColumnConfig("Program").setParent(columns);
		new MyColumnConfig("Status Awal").setParent(columns);
		new MyColumnConfig("Status Mhs").setParent(columns);
		new MyColumnConfig("SKS").setParent(columns);
		new MyColumnConfig("Tanggal").setParent(columns);
		new MyColumnConfig("Keterangan").setParent(columns);
		new MyColumnConfig("Revisi").setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Criteria criteria = session.createCriteria(HistoryStatusMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.addOrder(Order.asc("semester"))
					.addOrder(Order.asc("tahunAkademik"))
					.addOrder(Order.asc("sp"));
			List<HistoryStatusMahasiswa> histories = criteria.list();
			if (histories == null || histories.isEmpty()) {
				Row row = new Row();
				row.setSpans("10");
				row.setParent(rows);
				row.appendChild(new Label("Belum ada data HistoryStatusMahasiswa untuk mahasiswa ini."));
				return;
			}

			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
			for (HistoryStatusMahasiswa h : histories) {
				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new Label(text(h.getTahunAkademik())));
				row.appendChild(new Label(h.getSemester() == null ? "" : h.getSemester().toString()));
				row.appendChild(new Label(h.getSp() == null ? "Reguler" : "SP"));
				row.appendChild(new Label(text(h.getProgram())));
				row.appendChild(new Label(h.getStatusAwalMahasiswa() == null ? "" : h.getStatusAwalMahasiswa().getNama()));
				row.appendChild(new Label(h.getStatusMahasiswa() == null ? "" : h.getStatusMahasiswa().getNama()));
				row.appendChild(new Label(h.getSks() == null ? "" : h.getSks().toString()));
				row.appendChild(new Label(h.getTanggalStatus() == null ? "" : sdf.format(h.getTanggalStatus())));
				row.appendChild(new Label(text(h.getKeterangan())));
				RevisiHelper.createNewRevisi(HistoryStatusMahasiswa.class, h,
						h.getSemester() == null ? "" : h.getSemester().toString()).setParent(row);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			Row row = new Row();
			row.setSpans("10");
			row.setParent(rows);
			row.appendChild(new Label("Riwayat status belum dapat dimuat: " + e.getMessage()));
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/InformasiPembayaranMahasiswaAction.java:tampilkanHistoryStatusMahasiswa-close");
				}
			}
		}
	}

	private String text(String value) {
		return value == null ? "" : value;
	}

	// -----------------------------------------------------------------------------------
	// HELPER UI: DASBOR REKAP & TREN
	// -----------------------------------------------------------------------------------
	private void buildDashboardRekap(Component parent, TreeMap<String, Kegiatan> kegiatans) {
		Common.clear(parent);
		ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("info_pembayaran_mahasiswa",
				"Menyusun Dashboard Pembayaran",
				"Menghitung total tagihan, pembayaran, sisa kewajiban, tren bulanan, dan komposisi item biaya.", 65);

		DashboardRekapData data = collectDashboardRekapData(kegiatans);

		MyPortallayout portalLayout = new MyPortallayout();
		portalLayout.setWidth("100%");
		portalLayout.setParent(parent);

		String pcWidth = Common.isMobile() ? "100%" : "50%";

		MyPortalchildren pcLeft = new MyPortalchildren();
		pcLeft.setWidth(pcWidth);
		pcLeft.setStyle("padding: 5px;");
		pcLeft.setParent(portalLayout);

		MyPortalchildren pcRight = new MyPortalchildren();
		pcRight.setWidth(pcWidth);
		pcRight.setStyle("padding: 5px;");
		pcRight.setParent(portalLayout);

		MyPortalchildren pcBottom = new MyPortalchildren();
		pcBottom.setWidth("100%");
		pcBottom.setStyle("padding: 5px;");
		pcBottom.setParent(portalLayout);

		if (Common.bolehKonfigurasi(KONFIG_TAMPILKAN_RINGKASAN_KESELURUHAN, Konfigurasi.AKTIF)) {
			renderDashboardSummary(pcLeft, data);
		}
		renderSemesterComparison(pcRight, data);
		renderPrioritasTunggakan(pcRight, data);
		renderPaymentTrend(pcLeft, data);
		renderItemComposition(pcRight, data);
		renderPaymentInsight(pcBottom, data);
		renderPaymentRadar(pcRight, data);
		renderRiwayatPembayaran(pcLeft, data);
		renderRekapSemester(pcBottom, data);
		ais.ui.util.KeuanganDashboardEnhanceUtil.hideFloatingProgress("info_pembayaran_mahasiswa");
	}

	private DashboardRekapData collectDashboardRekapData(TreeMap<String, Kegiatan> sumber) {
		DashboardRekapData data = new DashboardRekapData();
		data.kegiatans = sumber == null ? new TreeMap<String, Kegiatan>() : sumber;
		data.allCicilan = new ArrayList<CicilanPembayaran>();
		data.tagihanPerSemester = new TreeMap<String, Double>();
		data.dibayarPerSemester = new TreeMap<String, Double>();
		data.sisaPerSemester = new TreeMap<String, Double>();
		data.trenBulanan = new TreeMap<String, Double>();
		data.itemBiaya = new TreeMap<String, Double>();

		if (sumber != null && !sumber.isEmpty()) {
			for (Kegiatan k : sumber.values()) {
				if (k == null) {
					continue;
				}
				// Konsisten dengan KegiatanPersistenceHelper: tagihan + dibayar + persentase
				// dihitung bersama agar ringkasan dashboard tidak memakai dibayar yang basi.
				recomputeKegiatanKonsisten(k);
				double tagihanSmt = safeDouble(k.getTagihan());
				double dibayarSmt = safeDouble(k.getDibayar());
				double terhutangSmt = tagihanSmt - dibayarSmt;

				data.totalTagihan += tagihanSmt;
				data.totalDibayar += dibayarSmt;
				data.totalSisa += terhutangSmt;
				data.jumlahKegiatan++;

				String namaSmt = "Smt " + (k.getSemster() != null ? k.getSemster() : "-");
				if (k.getTahunAkademik() != null && k.getTahunAkademik().trim().length() > 0) {
					namaSmt += " " + k.getTahunAkademik();
				}
				addMapValue(data.tagihanPerSemester, namaSmt, tagihanSmt);
				addMapValue(data.dibayarPerSemester, namaSmt, dibayarSmt);
				addMapValue(data.sisaPerSemester, namaSmt, terhutangSmt);

				List<CicilanPembayaran> cicilansK = null;
				try {
					cicilansK = k.ambilCicilan();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				if (cicilansK != null && !cicilansK.isEmpty()) {
					data.allCicilan.addAll(cicilansK);
				}
			}
		}

		SimpleDateFormat sdfTren = new SimpleDateFormat("yyyy-MM");
		for (int i = 0; i < data.allCicilan.size(); i++) {
			CicilanPembayaran cp = (CicilanPembayaran) data.allCicilan.get(i);
			if (cp == null) {
				continue;
			}
			double nilai = safeDouble(cp.getNilai());
			if (cp.getTanggal() != null && nilai > 0.0) {
				addMapValue(data.trenBulanan, sdfTren.format(cp.getTanggal()), nilai);
			}
			if (cp.getItemBiaya() != null && cp.getItemBiaya().getNama() != null && nilai > 0.0) {
				addMapValue(data.itemBiaya, cp.getItemBiaya().getNama(), nilai);
			}
		}

		data.persenLunas = data.totalTagihan > 0.0 ? (data.totalDibayar * 100.0 / data.totalTagihan)
				: (data.totalDibayar > 0.0 ? 100.0 : 0.0);
		if (data.persenLunas > 100.0) {
			data.persenLunas = 100.0;
		}
		data.rataRataBayar = data.allCicilan.isEmpty() ? 0.0 : data.totalDibayar / data.allCicilan.size();
		data.itemTerbesar = getMaxKey(data.itemBiaya);
		data.bulanTerbesar = getMaxKey(data.trenBulanan);

		return data;
	}

	private void renderDashboardSummary(Component parent, DashboardRekapData data) {
		Panel panel = createPanel(parent, "Ringkasan Keseluruhan", "margin-bottom: 15px;");
		Panelchildren body = (Panelchildren) panel.getChildren().get(0);
		body.setStyle("padding: 10px;");

		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;'>");
		html.append("<div style='display:flex;flex-wrap:wrap;gap:10px;margin-bottom:12px;'>");
		appendSummaryCard(html, "Total Tagihan", data.totalTagihan, "#eef4fa", "#b8daff", "#004085");
		appendSummaryCard(html, "Total Terbayar", data.totalDibayar, "#eefaf1", "#c3e6cb", "#155724");
		appendSummaryCard(html, "Sisa Tunggakan", data.totalSisa, "#faeeee", "#f5c6cb", "#721c24");
		appendSummaryCard(html, "Jumlah Transaksi", data.allCicilan.size(), "#fff8e8", "#ffe0a6", "#7c4a03");
		html.append("</div>");

		int persen = percentValue(data.persenLunas);
		html.append("<div style='background:#f8fafc;border:1px solid #e5e7eb;border-radius:9px;padding:10px;'>");
		html.append("<div style='display:flex;justify-content:space-between;gap:10px;font-size:12px;color:#334155;margin-bottom:6px;'>");
		html.append("<b>Progres Pelunasan Keseluruhan</b><b>").append(persen).append("%</b>");
		html.append("</div>");
		html.append("<div style='height:12px;background:#e5e7eb;border-radius:999px;overflow:hidden;'>");
		html.append("<div style='height:12px;width:").append(persen).append("%;background:linear-gradient(90deg,#16a34a,#22c55e);'></div>");
		html.append("</div>");
		html.append("<div style='font-size:11px;color:#64748b;margin-top:8px;'>");
		html.append("Terbilang tagihan: <b>").append(htmlText(toWords(data.totalTagihan))).append("</b><br/>");
		html.append("Terbilang terbayar: <b>").append(htmlText(toWords(data.totalDibayar))).append("</b><br/>");
		html.append("Terbilang sisa: <b>").append(htmlText(toWords(data.totalSisa))).append("</b>");
		html.append("</div></div></div>");
		body.appendChild(new org.zkoss.zul.Html(html.toString()));
	}

	private void renderSemesterComparison(Component parent, DashboardRekapData data) {
		Panel panel = createPanel(parent, "Perbandingan Tagihan vs Terbayar per Semester", "margin-bottom: 15px;");
		Panelchildren body = (Panelchildren) panel.getChildren().get(0);
		body.setStyle("padding: 10px; text-align:left;");
		body.appendChild(new org.zkoss.zul.Html(buildGroupedBarHtml(data.tagihanPerSemester, data.dibayarPerSemester,
				"Tagihan", "Terbayar", "#2563eb", "#16a34a")));
	}

	private void renderPrioritasTunggakan(Component parent, DashboardRekapData data) {
		Panel panel = createPanel(parent, "Prioritas Tunggakan per Semester", "margin-bottom: 15px;");
		Panelchildren body = (Panelchildren) panel.getChildren().get(0);
		body.setStyle("padding: 10px; text-align:left;");
		body.appendChild(new org.zkoss.zul.Html(buildPrioritasTunggakanHtml(data)));
	}

	private void renderPaymentTrend(Component parent, DashboardRekapData data) {
		Panel panel = createPanel(parent, "Tren Aktivitas Pembayaran Bulanan", "margin-bottom: 15px;");
		Panelchildren body = (Panelchildren) panel.getChildren().get(0);
		body.setStyle("padding: 10px; text-align:left;");
		body.appendChild(new org.zkoss.zul.Html(buildBarHtml(data.trenBulanan, "#0ea5e9", "Belum ada pembayaran bulanan.")));
	}

	private void renderItemComposition(Component parent, DashboardRekapData data) {
		Panel panel = createPanel(parent, "Komposisi Berdasarkan Item Biaya", "margin-bottom: 15px;");
		Panelchildren body = (Panelchildren) panel.getChildren().get(0);
		body.setStyle("padding: 10px; text-align:left;");
		body.appendChild(new org.zkoss.zul.Html(buildCompositionHtml(data.itemBiaya)));
	}

	private void renderPaymentInsight(Component parent, DashboardRekapData data) {
		Panel panel = createPanel(parent, "Insight Tagihan dan Pembayaran", "margin-bottom: 15px;");
		Panelchildren body = (Panelchildren) panel.getChildren().get(0);
		body.setStyle("padding: 10px;");
		String status = data.totalSisa <= 0.1 && data.totalTagihan > 0.0 ? "Lunas" : "Belum Lunas";
		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:10px;'>");
		appendInsight(html, "Status keseluruhan", status);
		appendInsight(html, "Rata-rata pembayaran", money(data.rataRataBayar));
		appendInsight(html, "Item biaya dominan", isEmpty(data.itemTerbesar) ? "-" : data.itemTerbesar);
		appendInsight(html, "Bulan pembayaran terbesar", isEmpty(data.bulanTerbesar) ? "-" : data.bulanTerbesar);
		appendInsight(html, "Jumlah kegiatan", String.valueOf(data.jumlahKegiatan));
		appendInsight(html, "Jumlah riwayat cicilan", String.valueOf(data.allCicilan.size()));
		html.append("</div>");
		body.appendChild(new org.zkoss.zul.Html(html.toString()));
	}


	private void renderPaymentRadar(Component parent, DashboardRekapData data) {
		Panel panel = createPanel(parent, "Radar Kondisi Pembayaran", "margin-bottom: 15px;");
		Panelchildren body = (Panelchildren) panel.getChildren().get(0);
		body.setStyle("padding: 10px;");

		int lunas = percentValue(data.persenLunas);
		int amanSisa = percentValue(data.totalTagihan <= 0.0 ? 100.0 : ((data.totalTagihan - data.totalSisa) * 100.0 / data.totalTagihan));
		int aktivitas = percentValue(data.allCicilan == null || data.jumlahKegiatan <= 0 ? 0.0
				: (data.allCicilan.size() * 100.0 / Math.max(1.0, data.jumlahKegiatan * 3.0)));
		int sebaranItem = percentValue(data.itemBiaya == null || data.itemBiaya.isEmpty() ? 0.0
				: (Math.min(6, data.itemBiaya.size()) * 100.0 / 6.0));
		int tren = percentValue(data.trenBulanan == null || data.trenBulanan.isEmpty() ? 0.0
				: (Math.min(6, data.trenBulanan.size()) * 100.0 / 6.0));

		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;'>");
		html.append("<div style='display:flex;gap:14px;flex-wrap:wrap;align-items:center;'>");
		html.append("<div style='width:178px;height:178px;border-radius:999px;border:1px solid #dbeafe;background:radial-gradient(circle,#fff 0,#fff 31%,#e0f2fe 32%,#e0f2fe 33%,#fff 34%,#fff 48%,#dbeafe 49%,#dbeafe 50%,#fff 51%);display:flex;align-items:center;justify-content:center;box-shadow:0 8px 24px rgba(15,23,42,.08);'>");
		html.append("<div style='text-align:center;'><div style='font-size:24px;font-weight:800;color:#0f172a;'>")
				.append(lunas).append("%</div><div style='font-size:11px;color:#64748b;'>lunas</div></div></div>");
		html.append("<div style='flex:1;min-width:240px;'>");
		// Jaring laba-laba (spider) SVG modern dari kit reusable — menggantikan baris radar datar.
		html.append(DashboardUiKit.spider("", "",
				new String[] { "Pelunasan", "Tagihan terkendali", "Aktivitas bayar", "Sebaran item", "Riwayat bulanan" },
				new int[] { lunas, amanSisa, aktivitas, sebaranItem, tren }));
		html.append("<div style='font-size:11px;color:#64748b;margin-top:8px;line-height:1.45;'>Makin lebar jaringnya, makin sehat kondisi pembayaran Anda.</div>");
		html.append("</div></div></div>");
		body.appendChild(new org.zkoss.zul.Html(html.toString()));
	}

	private void appendRadarLine(StringBuilder html, String label, int percent, String color) {
		html.append("<div style='margin-bottom:9px;'>");
		html.append("<div style='display:flex;justify-content:space-between;font-size:11px;color:#334155;font-weight:bold;margin-bottom:3px;'>")
				.append("<span>").append(htmlText(label)).append("</span><span>").append(percent).append("%</span></div>");
		html.append("<div style='height:11px;background:#e5e7eb;border-radius:999px;overflow:hidden;'>")
				.append("<div style='height:11px;width:").append(percent).append("%;background:").append(color)
				.append(";border-radius:999px;'></div></div></div>");
	}

	private void renderRiwayatPembayaran(Component parent, DashboardRekapData data) {
		Panel pnlRiwayat = createPanel(parent, "Riwayat Transaksi Pembayaran Terakhir", "margin-bottom: 15px;");
		Panelchildren pchRiwayat = (Panelchildren) pnlRiwayat.getChildren().get(0);
		pchRiwayat.setStyle("padding: 5px; max-height: 360px; overflow: auto;");

		if (data.allCicilan != null && !data.allCicilan.isEmpty()) {
			Collections.sort(data.allCicilan, new Comparator<CicilanPembayaran>() {
				@Override
				public int compare(CicilanPembayaran c1, CicilanPembayaran c2) {
					if (c1 == null || c1.getTanggal() == null) {
						return 1;
					}
					if (c2 == null || c2.getTanggal() == null) {
						return -1;
					}
					return c2.getTanggal().compareTo(c1.getTanggal());
				}
			});

			Grid gridRiwayat = new Grid();
			gridRiwayat.setMold("paging");
			gridRiwayat.setPageSize(50);
			gridRiwayat.setSclass("dgrid");
			gridRiwayat.setWidth("100%");
			gridRiwayat.setParent(pchRiwayat);

			Columns colsRiw = new Columns();
			colsRiw.setParent(gridRiwayat);
			new MyColumnConfig("Tanggal").setParent(colsRiw);
			new MyColumnConfig("Keterangan").setParent(colsRiw);
			MyColumnConfig colRiwVal = new MyColumnConfig("Nominal");
			colRiwVal.setAlign("right");
			colRiwVal.setParent(colsRiw);

			Rows rowsRiw = new Rows();
			rowsRiw.setParent(gridRiwayat);

			double totalNominal = 0.0;
			for (CicilanPembayaran cp : data.allCicilan) {
				if (cp == null) {
					continue;
				}
				Row r = new Row();
				r.setParent(rowsRiw);
				r.appendChild(new Label(cp.getTanggal() != null ? Common.dateFormat3.get().format(cp.getTanggal()) : "-"));
				r.appendChild(new Label(cp.getKeterangan() != null ? cp.getKeterangan() : "Pembayaran"));

				double nilai = safeDouble(cp.getNilai());
				totalNominal += nilai;

				Label lblNilai = new Label("Rp " + Common.numberFormat.get().format(nilai));
				lblNilai.setStyle("color: #155724; font-weight:bold;");
				r.appendChild(lblNilai);
			}

			Foot footRiwayat = new Foot();
			footRiwayat.setParent(gridRiwayat);

			Footer fTgl = new Footer();
			fTgl.setParent(footRiwayat);

			Footer fKet = new Footer();
			fKet.setParent(footRiwayat);
			fKet.setAlign("right");
			Label lblTextTotal = new Label(ais.common.Common.getBahasaConfig("TOTAL :"));
			lblTextTotal.setStyle("font-weight:bold; color:#333;");
			fKet.appendChild(lblTextTotal);

			Footer fNominal = new Footer();
			fNominal.setParent(footRiwayat);
			fNominal.setAlign("right");
			Label lblSumNominal = new Label("Rp " + Common.numberFormat.get().format(totalNominal));
			lblSumNominal.setStyle("color: #155724; font-weight:bold;");
			fNominal.appendChild(lblSumNominal);
		} else {
			pchRiwayat.appendChild(new org.zkoss.zul.Html("<div style='padding:12px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;border-radius:8px;'>Belum ada riwayat transaksi. Dashboard tetap menampilkan nilai tagihan dan sisa berdasarkan data kegiatan.</div>"));
		}
	}

	private void renderRekapSemester(Component parent, DashboardRekapData data) {
		Panel pnlRekap = createPanel(parent, "Rincian Rekapitulasi Pembayaran per Semester", "margin-bottom: 10px;");
		Panelchildren pchRekap = (Panelchildren) pnlRekap.getChildren().get(0);
		pchRekap.setStyle("padding: 10px;");

		Hbox headerBox = new Hbox();
		headerBox.setWidth("100%");
		headerBox.setPack("end");
		headerBox.setStyle("margin-bottom: 5px;");
		headerBox.setParent(pchRekap);

		MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig("Download Excel", "/img/excel.png");
		btnExcel.setParent(headerBox);
		btnExcel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				downloadExcelRekap();
			}
		});

		MyGrid gridRekap = new MyGrid();
		gridRekap.setMold("paging");
		gridRekap.setPageSize(50);
		gridRekap.setParent(pchRekap);
		gridRekap.setWidth("100%");
		gridRekap.setSclass("dgrid");

		Columns cols = new Columns();
		cols.setParent(gridRekap);
		new MyColumnConfig("Semester / Jenis").setParent(cols);
		new MyColumnConfig("Tahun Akademik").setParent(cols);

		MyColumnConfig cTagihan = new MyColumnConfig("Tagihan");
		cTagihan.setAlign("right");
		cTagihan.setParent(cols);

		MyColumnConfig cDibayar = new MyColumnConfig("Dibayar");
		cDibayar.setAlign("right");
		cDibayar.setParent(cols);

		MyColumnConfig cSisa = new MyColumnConfig("Sisa");
		cSisa.setAlign("right");
		cSisa.setParent(cols);

		MyColumnConfig cProg = new MyColumnConfig("% Lunas");
		cProg.setAlign("center");
		cProg.setWidth("10%");
		cProg.setParent(cols);

		MyColumnConfig cStatus = new MyColumnConfig("Status");
		cStatus.setAlign("center");
		cStatus.setParent(cols);

		Rows rows = new Rows();
		rows.setParent(gridRekap);

		if (data.kegiatans != null && !data.kegiatans.isEmpty()) {
			for (Kegiatan k : data.kegiatans.values()) {
				if (k == null) {
					continue;
				}
				Row row = new Row();
				row.setParent(rows);
				String jenisStr = k.getJenisKegiatan() != null ? k.getJenisKegiatan().getNamaKegiatan() : "Umum";
				new Label("SMT " + k.getSemster() + " - " + jenisStr).setParent(row);
				new Label(k.getTahunAkademik() != null ? k.getTahunAkademik() : "-").setParent(row);

				double tagihan = safeDouble(k.getTagihan());
				double dibayar = safeDouble(k.getDibayar());
				double terhutang = tagihan - dibayar;

				new Label(Common.numberFormat.get().format(tagihan)).setParent(row);
				new Label(Common.numberFormat.get().format(dibayar)).setParent(row);

				Label lblSisa = new Label(Common.numberFormat.get().format(terhutang));
				if (terhutang > 0.1) {
					lblSisa.setStyle("color:#d9534f; font-weight:bold;");
				}
				lblSisa.setParent(row);

				double pLunas = safeDouble(k.getPersentase());
				Label lblPrg = new Label(Common.numberFormat.get().format(pLunas) + "%");
				lblPrg.setParent(row);

				Label lblStatus = new Label(k.getApakahLunas() != null && k.getApakahLunas() ? "LUNAS" : "BELUM");
				if (k.getApakahLunas() != null && k.getApakahLunas()) {
					lblStatus.setStyle("color:#5cb85c; font-weight:bold; padding: 2px 6px; border-radius: 4px; background-color: #dff0d8; font-size:10px;");
				} else {
					lblStatus.setStyle("color:#d9534f; font-weight:bold; padding: 2px 6px; border-radius: 4px; background-color: #f2dede; font-size:10px;");
				}
				lblStatus.setParent(row);
			}

			Foot foot = new Foot();
			foot.setParent(gridRekap);

			Footer f1 = new Footer();
			f1.setParent(foot);
			f1.appendChild(new MyLabelBold("TOTAL KESELURUHAN"));

			Footer f2 = new Footer();
			f2.setParent(foot);

			Footer fTagihan = new Footer();
			fTagihan.setParent(foot);
			fTagihan.setAlign("right");
			fTagihan.appendChild(new MyLabelBold(Common.numberFormat.get().format(data.totalTagihan)));

			Footer fDibayar = new Footer();
			fDibayar.setParent(foot);
			fDibayar.setAlign("right");
			fDibayar.appendChild(new MyLabelBold(Common.numberFormat.get().format(data.totalDibayar)));

			Footer fSisa = new Footer();
			fSisa.setParent(foot);
			fSisa.setAlign("right");
			MyLabelBold lblTotalSisa = new MyLabelBold(Common.numberFormat.get().format(data.totalSisa));
			if (data.totalSisa > 0.1) {
				lblTotalSisa.setStyle("color:#d9534f; font-weight:bold;");
			}
			fSisa.appendChild(lblTotalSisa);

			Footer fProg = new Footer();
			fProg.setParent(foot);
			fProg.setAlign("center");
			fProg.appendChild(new MyLabelBold(Common.numberFormat.get().format(data.persenLunas) + "%"));

			Footer fStatus = new Footer();
			fStatus.setParent(foot);
		} else {
			Row row = new Row();
			row.setParent(rows);
			Label lblEmpty = new Label(ais.common.Common.getBahasaConfig("Belum ada data."));
			row.appendChild(lblEmpty);
		}
	}


	private String getPanelDescriptionHtml(String title) {
		String desc = "Ringkasan pembayaran yang gampang dibaca oleh siapa saja.";
		if (title != null) {
			String lower = title.toLowerCase();
			if (lower.indexOf("ringkasan") >= 0) {
				desc = "Total tagihan, yang sudah dibayar, dan sisanya — semua dalam satu tempat.";
			} else if (lower.indexOf("semester") >= 0) {
				desc = "Bandingkan tiap semester: mana yang sudah lunas, mana yang masih kurang.";
			} else if (lower.indexOf("prioritas") >= 0 || lower.indexOf("tunggakan") >= 0) {
				desc = "Sisa terbesar tampil di atas, supaya tahu mana yang perlu dibayar lebih dulu.";
			} else if (lower.indexOf("tren") >= 0) {
				desc = "Lihat di bulan apa pembayaran paling banyak masuk.";
			} else if (lower.indexOf("komposisi") >= 0 || lower.indexOf("item") >= 0) {
				desc = "Lihat jenis biaya apa yang paling banyak Anda bayar.";
			} else if (lower.indexOf("insight") >= 0) {
				desc = "Kesimpulan singkat: sudah lunas atau belum, dan biaya apa yang paling besar.";
			} else if (lower.indexOf("riwayat") >= 0) {
				desc = "Pembayaran paling baru tampil lebih dulu, jadi mudah dicek.";
			} else if (lower.indexOf("rekap") >= 0 || lower.indexOf("rincian") >= 0) {
				desc = "Angka rinci per semester untuk dicocokkan dan diperiksa.";
			}
		}
		return "<div style='font-size:11px;color:#64748b;margin:0 0 8px 0;line-height:1.45;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:8px;'>"
				+ htmlText(desc) + "</div>";
	}

	private Panel createPanel(Component parent, String title, String style) {
		Panel panel = new Panel();
		panel.setTitle(title);
		panel.setBorder("normal");
		panel.setCollapsible(true);
		panel.setStyle(style == null ? "" : style);
		panel.setParent(parent);

		Panelchildren children = new Panelchildren();
		children.setParent(panel);
		children.appendChild(new org.zkoss.zul.Html(getPanelDescriptionHtml(title)));
		return panel;
	}

	private void appendSummaryCard(StringBuilder html, String title, double amount, String bg, String border, String color) {
		html.append("<div style='flex:1;min-width:145px;padding:10px;background:")
				.append(bg).append(";border:1px solid ").append(border)
				.append(";border-radius:10px;box-shadow:0 1px 3px rgba(15,23,42,0.10);'>");
		html.append("<div style='font-size:10px;color:#475569;font-weight:bold;text-transform:uppercase;'>")
				.append(htmlText(title)).append("</div>");
		html.append("<div style='font-size:15px;font-weight:bold;color:").append(color).append(";margin-top:4px;'>")
				.append(money(amount)).append("</div>");
		html.append("</div>");
	}

	private void appendSummaryCard(StringBuilder html, String title, int jumlah, String bg, String border, String color) {
		html.append("<div style='flex:1;min-width:145px;padding:10px;background:")
				.append(bg).append(";border:1px solid ").append(border)
				.append(";border-radius:10px;box-shadow:0 1px 3px rgba(15,23,42,0.10);'>");
		html.append("<div style='font-size:10px;color:#475569;font-weight:bold;text-transform:uppercase;'>")
				.append(htmlText(title)).append("</div>");
		html.append("<div style='font-size:15px;font-weight:bold;color:").append(color).append(";margin-top:4px;'>")
				.append(jumlah).append(" Record</div>");
		html.append("</div>");
	}

	private String buildGroupedBarHtml(TreeMap<String, Double> series1, TreeMap<String, Double> series2,
			String label1, String label2, String color1, String color2) {
		double max = Math.max(maxValue(series1), maxValue(series2));
		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;'>");
		if (max <= 0.0) {
			html.append("<div style='padding:12px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;border-radius:8px;'>Belum ada nominal tagihan atau pembayaran yang bisa divisualisasikan.</div>");
			html.append("</div>");
			return html.toString();
		}
		html.append("<div style='display:flex;gap:12px;align-items:center;font-size:11px;color:#475569;margin-bottom:8px;'>")
				.append("<span><b style='display:inline-block;width:10px;height:10px;background:").append(color1).append(";'></b> ")
				.append(htmlText(label1)).append("</span>")
				.append("<span><b style='display:inline-block;width:10px;height:10px;background:").append(color2).append(";'></b> ")
				.append(htmlText(label2)).append("</span></div>");
		for (String key : series1.keySet()) {
			double v1 = getMapValue(series1, key);
			double v2 = getMapValue(series2, key);
			int w1 = percentValue(max <= 0.0 ? 0.0 : (v1 * 100.0 / max));
			int w2 = percentValue(max <= 0.0 ? 0.0 : (v2 * 100.0 / max));
			html.append("<div style='margin-bottom:9px;'>");
			html.append("<div style='font-size:11px;color:#334155;margin-bottom:3px;'>").append(htmlText(key)).append("</div>");
			html.append("<div style='height:9px;background:#e5e7eb;border-radius:999px;overflow:hidden;margin-bottom:3px;'><div style='height:9px;width:")
					.append(w1).append("%;background:").append(color1).append(";'></div></div>");
			html.append("<div style='height:9px;background:#e5e7eb;border-radius:999px;overflow:hidden;'><div style='height:9px;width:")
					.append(w2).append("%;background:").append(color2).append(";'></div></div>");
			html.append("<div style='font-size:10px;color:#64748b;margin-top:2px;'>")
					.append(htmlText(label1)).append(": ").append(money(v1)).append(" &nbsp; ")
					.append(htmlText(label2)).append(": ").append(money(v2)).append("</div>");
			html.append("</div>");
		}
		html.append("</div>");
		return html.toString();
	}

	private String buildBarHtml(TreeMap<String, Double> data, String color, String emptyMessage) {
		double max = maxValue(data);
		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;'>");
		if (data == null || data.isEmpty() || max <= 0.0) {
			html.append("<div style='padding:12px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;border-radius:8px;'>")
					.append(htmlText(emptyMessage)).append("</div></div>");
			return html.toString();
		}
		for (Map.Entry<String, Double> entry : data.entrySet()) {
			double value = safeDouble(entry.getValue());
			int width = percentValue(value * 100.0 / max);
			html.append("<div style='margin-bottom:8px;'>");
			html.append("<div style='display:flex;justify-content:space-between;font-size:11px;color:#334155;margin-bottom:3px;'>")
					.append("<span>").append(htmlText(entry.getKey())).append("</span>")
					.append("<b>").append(money(value)).append("</b></div>");
			html.append("<div style='height:12px;background:#e5e7eb;border-radius:999px;overflow:hidden;'>")
					.append("<div style='height:12px;width:").append(width).append("%;background:")
					.append(color).append(";'></div></div>");
			html.append("</div>");
		}
		html.append("</div>");
		return html.toString();
	}

	private String buildPrioritasTunggakanHtml(DashboardRekapData data) {
		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;'>");
		if (data == null || data.sisaPerSemester == null || data.sisaPerSemester.isEmpty() || data.totalSisa <= 0.1) {
			html.append("<div style='padding:12px;color:#166534;background:#f0fdf4;border:1px dashed #bbf7d0;border-radius:8px;'>Tidak ada tunggakan aktif yang perlu diprioritaskan.</div>");
			html.append("</div>");
			return html.toString();
		}
		List<Map.Entry<String, Double>> entries = new ArrayList<Map.Entry<String, Double>>(data.sisaPerSemester.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, Double>>() {
			@Override
			public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b) {
				return Double.valueOf(safeDouble(b == null ? null : b.getValue()))
						.compareTo(Double.valueOf(safeDouble(a == null ? null : a.getValue())));
			}
		});
		double max = 0.0;
		for (Map.Entry<String, Double> entry : entries) {
			double value = safeDouble(entry.getValue());
			if (value > max) {
				max = value;
			}
		}
		int displayed = 0;
		for (Map.Entry<String, Double> entry : entries) {
			double value = safeDouble(entry.getValue());
			if (value <= 0.1) {
				continue;
			}
			int width = percentValue(max <= 0.0 ? 0.0 : (value * 100.0 / max));
			int share = percentValue(data.totalSisa <= 0.0 ? 0.0 : (value * 100.0 / data.totalSisa));
			html.append("<div style='margin-bottom:10px;background:#ffffff;border:1px solid #e2e8f0;border-radius:10px;padding:9px;'>");
			html.append("<div style='display:flex;justify-content:space-between;gap:8px;font-size:11px;color:#334155;font-weight:bold;margin-bottom:4px;'>")
					.append("<span>").append(htmlText(entry.getKey())).append("</span>")
					.append("<span>").append(money(value)).append(" / ").append(share).append("%</span></div>");
			html.append("<div style='height:12px;background:#fee2e2;border-radius:999px;overflow:hidden;'>")
					.append("<div style='height:12px;width:").append(width)
					.append("%;background:linear-gradient(90deg,#f97316,#dc2626);'></div></div>");
			html.append("</div>");
			displayed++;
			if (displayed >= 6) {
				break;
			}
		}
		if (displayed == 0) {
			html.append("<div style='padding:12px;color:#166534;background:#f0fdf4;border:1px dashed #bbf7d0;border-radius:8px;'>Tidak ada tunggakan aktif yang perlu diprioritaskan.</div>");
		}
		html.append("</div>");
		return html.toString();
	}

	private String buildCompositionHtml(TreeMap<String, Double> data) {
		double total = sumValue(data);
		StringBuilder html = new StringBuilder();
		html.append("<div style='font-family:Arial,sans-serif;'>");
		if (data == null || data.isEmpty() || total <= 0.0) {
			html.append("<div style='padding:12px;color:#64748b;background:#f8fafc;border:1px dashed #cbd5e1;border-radius:8px;'>Belum ada komposisi item biaya dari pembayaran.</div></div>");
			return html.toString();
		}
		int index = 0;
		String[] colors = new String[] { "#2563eb", "#16a34a", "#f97316", "#9333ea", "#dc2626", "#0891b2" };
		for (Map.Entry<String, Double> entry : data.entrySet()) {
			double value = safeDouble(entry.getValue());
			int persen = percentValue(value * 100.0 / total);
			String color = colors[index % colors.length];
			html.append("<div style='margin-bottom:9px;'>");
			html.append("<div style='display:flex;justify-content:space-between;font-size:11px;color:#334155;margin-bottom:3px;'>")
					.append("<span>").append(htmlText(entry.getKey())).append("</span>")
					.append("<b>").append(persen).append("% - ").append(money(value)).append("</b></div>");
			html.append("<div style='height:12px;background:#e5e7eb;border-radius:999px;overflow:hidden;'>")
					.append("<div style='height:12px;width:").append(persen).append("%;background:")
					.append(color).append(";'></div></div>");
			html.append("</div>");
			index++;
		}
		html.append("</div>");
		return html.toString();
	}

	private void appendInsight(StringBuilder html, String label, String value) {
		html.append("<div style='background:#ffffff;border:1px solid #e2e8f0;border-radius:10px;padding:10px;box-shadow:0 1px 2px rgba(15,23,42,0.06);'>")
				.append("<div style='font-size:10px;color:#64748b;text-transform:uppercase;font-weight:bold;'>")
				.append(htmlText(label)).append("</div>")
				.append("<div style='font-size:13px;color:#0f172a;font-weight:bold;margin-top:5px;'>")
				.append(htmlText(value)).append("</div></div>");
	}

	private static class DashboardRekapData {
		private TreeMap<String, Kegiatan> kegiatans;
		private List<CicilanPembayaran> allCicilan;
		private TreeMap<String, Double> tagihanPerSemester;
		private TreeMap<String, Double> dibayarPerSemester;
		private TreeMap<String, Double> sisaPerSemester;
		private TreeMap<String, Double> trenBulanan;
		private TreeMap<String, Double> itemBiaya;
		private double totalTagihan;
		private double totalDibayar;
		private double totalSisa;
		private double persenLunas;
		private double rataRataBayar;
		private int jumlahKegiatan;
		private String itemTerbesar;
		private String bulanTerbesar;
	}

	private static void addMapValue(TreeMap<String, Double> map, String key, double value) {
		if (map == null || key == null) {
			return;
		}
		Double old = map.get(key);
		map.put(key, old == null ? value : old.doubleValue() + value);
	}

	private static double getMapValue(TreeMap<String, Double> map, String key) {
		Double value = map == null ? null : map.get(key);
		return value == null ? 0.0 : value.doubleValue();
	}

	private static double safeDouble(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}

	private static double maxValue(TreeMap<String, Double> map) {
		double max = 0.0;
		if (map != null) {
			for (Double value : map.values()) {
				if (value != null && value.doubleValue() > max) {
					max = value.doubleValue();
				}
			}
		}
		return max;
	}

	private static double sumValue(TreeMap<String, Double> map) {
		double sum = 0.0;
		if (map != null) {
			for (Double value : map.values()) {
				sum += value == null ? 0.0 : value.doubleValue();
			}
		}
		return sum;
	}

	private static String getMaxKey(TreeMap<String, Double> map) {
		String key = null;
		double max = 0.0;
		if (map != null) {
			for (Map.Entry<String, Double> entry : map.entrySet()) {
				double value = safeDouble(entry.getValue());
				if (key == null || value > max) {
					key = entry.getKey();
					max = value;
				}
			}
		}
		return key;
	}

	private static int percentValue(double value) {
		int result = (int) Math.round(value);
		if (result < 0) {
			return 0;
		}
		if (result > 100) {
			return 100;
		}
		return result;
	}

	private static String money(double value) {
		return "Rp " + Common.numberFormat.get().format(value);
	}

	private static String toWords(double value) {
		try {
			if (Math.abs(value) <= 0.1) {
				return "Nol Rupiah";
			}
			return IndonesianNumberToWords.convert((long) Math.round(Math.abs(value))) + " Rupiah";
		} catch (Exception e) {
			return "-";
		}
	}

	private static String htmlText(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	@SuppressWarnings("resource")
	private void downloadExcelRekap() {
		try {
			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet sheet = workbook.createSheet("Rekapitulasi Pembayaran");

			HSSFCellStyle styleHeader = workbook.createCellStyle();
			HSSFFont fontHeader = workbook.createFont();
			fontHeader.setBold(true);
			styleHeader.setFont(fontHeader);

			double totalTagihanKumulatif = 0.0;
			double totalDibayarKumulatif = 0.0;

			if (kegiatans != null && !kegiatans.isEmpty()) {
				for (Kegiatan k : kegiatans.values()) {
					totalTagihanKumulatif += (k.getTagihan() == null ? 0.0 : k.getTagihan());
					totalDibayarKumulatif += (k.getDibayar() == null ? 0.0 : k.getDibayar());
				}
			}
			double totalTerhutangKumulatif = totalTagihanKumulatif - totalDibayarKumulatif;

			String mhsName = mahasiswa != null ? mahasiswa.getNama()
					: (calMhs != null ? calMhs.getNama() : "Mahasiswa");
			String mhsNim = mahasiswa != null ? mahasiswa.getNim() : (calMhs != null ? calMhs.getNoRegistrasi() : "-");

			HSSFRow r0 = sheet.createRow(0);
			r0.createCell(0).setCellValue("REKAPITULASI PEMBAYARAN KEUANGAN MAHASISWA");
			r0.getCell(0).setCellStyle(styleHeader);

			sheet.createRow(1).createCell(0).setCellValue("Nama Mahasiswa: " + mhsName);
			sheet.createRow(2).createCell(0).setCellValue("NIM / No Registrasi: " + mhsNim);

			sheet.createRow(4).createCell(0).setCellValue(
					"Total Tagihan Keseluruhan: Rp " + Common.numberFormat.get().format(totalTagihanKumulatif));
			sheet.createRow(5).createCell(0).setCellValue(
					"Total Terbayar Keseluruhan: Rp " + Common.numberFormat.get().format(totalDibayarKumulatif));
			sheet.createRow(6).createCell(0).setCellValue(
					"Sisa Tunggakan Keseluruhan: Rp " + Common.numberFormat.get().format(totalTerhutangKumulatif));

			HSSFRow header = sheet.createRow(8);
			header.createCell(0).setCellValue("Semester / Jenis Kegiatan");
			header.createCell(1).setCellValue("Tahun Akademik");
			header.createCell(2).setCellValue("Total Tagihan");
			header.createCell(3).setCellValue("Total Dibayar");
			header.createCell(4).setCellValue("Sisa Tunggakan");
			header.createCell(5).setCellValue("Persentase Lunas");
			header.createCell(6).setCellValue("Status Lunas");

			for (int i = 0; i < 7; i++) {
				header.getCell(i).setCellStyle(styleHeader);
			}

			int rowIdx = 9;
			if (kegiatans != null) {
				for (Kegiatan k : kegiatans.values()) {
					HSSFRow row = sheet.createRow(rowIdx++);
					String jenisStr = k.getJenisKegiatan() != null ? k.getJenisKegiatan().getNamaKegiatan() : "Umum";

					row.createCell(0).setCellValue("Semester " + k.getSemster() + " - " + jenisStr);
					row.createCell(1).setCellValue(k.getTahunAkademik() == null ? "-" : k.getTahunAkademik());

					double tagihan = k.getTagihan() == null ? 0.0 : k.getTagihan();
					double dibayar = k.getDibayar() == null ? 0.0 : k.getDibayar();
					double terhutang = tagihan - dibayar;

					row.createCell(2).setCellValue(tagihan);
					row.createCell(3).setCellValue(dibayar);
					row.createCell(4).setCellValue(terhutang);

					Double pLunas = k.getPersentase() != null ? k.getPersentase() : 0.0;
					row.createCell(5).setCellValue(Common.numberFormat.get().format(pLunas) + "%");

					row.createCell(6)
							.setCellValue(k.getApakahLunas() != null && k.getApakahLunas() ? "LUNAS" : "BELUM LUNAS");
				}
			}

			for (int i = 0; i < 7; i++) {
				sheet.autoSizeColumn(i);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			out.close();

			String fileName = "Dashboard_Keuangan_" + (mahasiswa != null ? mahasiswa.getNim() : "Calon") + ".xls";
			Filedownload.save(out.toByteArray(), "application/vnd.ms-excel", fileName);

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			try { alert("Gagal mengunduh Excel: " + e.getMessage()); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/InformasiPembayaranMahasiswaAction.java:1688");}
		}
	}

	/**
	 * Hitung ulang tagihan, dibayar, dan persentase kegiatan secara konsisten sebelum ditampilkan.
	 *
	 * Panel ini sebelumnya hanya memanggil hitungTagihan() sehingga nilai "dibayar" dan
	 * "persentase" bisa basi (tidak sinkron dengan tagihan terbaru). Akibatnya muncul baris
	 * tidak valid seperti tagihan 4.300.000 / dibayar 4.200.000 dengan status yang tidak
	 * konsisten. Pola di sini disamakan dengan sumber kebenaran pada
	 * {@link ais.action.master.helper.KegiatanPersistenceHelper}
	 * (hitungTagihan + hitungDibayar + persentase) agar tagihan, pembayaran, persen, dan
	 * status lunas selalu sinkron satu sama lain.
	 */
	private void recomputeKegiatanKonsisten(Kegiatan k) {
		if (k == null) {
			return;
		}
		try {
			// "Tagihan" dihitung ULANG dari item biaya AKTIF/terkini (sama dgn rincian/detail) agar
			// item LAMA/BASI yang ter-DOBEL di JSON 'tagihans' tidak ikut terjumlah. Bila tak bisa
			// dihitung (konfig mati / data tak lengkap), fallback ke nilai JSON tersimpan. READ-ONLY.
			Double tagihan = tagihanSegarKonsisten(k);
			if (tagihan == null || tagihan.doubleValue() <= 0.1) {
				tagihan = k.hitungTagihan();
			}
			Double dibayar = k.hitungDibayar();
			Double persentase = (tagihan != null && tagihan.doubleValue() > 0.0 && dibayar != null)
					? Double.valueOf((dibayar.doubleValue() * 100.0) / tagihan.doubleValue())
					: Double.valueOf(0.0);
			k.setTagihan(tagihan);
			k.setDibayar(dibayar);
			k.setPersentase(persentase);
			k.setApakahLunas(persentase != null && persentase.intValue() >= 100);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Hitung ulang (Proses Tagihan) SELURUH kegiatan yang sedang tampil di dasbor untuk
	 * mahasiswa/calon ini, lalu muat ulang grid + dasbor. Memakai engine recompute yang
	 * SAMA dengan alur "Ubah Tagihan" pada DaftarUlang (hitungUlang=true, reset=true)
	 * sehingga nilai tagihan tersimpan menjadi sinkron dengan hitung-ulang rincian
	 * (memperbaiki angka basi/dobel pada kolom Tagihan dasbor).
	 */
	/**
	 * Tombol "Bersihkan Item Tak Sesuai": cari & hapus DetailKegiatan ASING (item yang BUKAN biaya
	 * berlaku untuk orang ini DAN belum ada pembayaran) pada semua kegiatan yang tampil, lalu hitung
	 * ulang. Pembayaran yang sudah ada TIDAK terpengaruh (item ber-pembayaran tidak ikut terhapus).
	 */
	private void bersihkanItemTakSesuai() throws Exception {
		if (kegiatans == null || kegiatans.isEmpty()) {
			loadKegiatan(true, null, null);
		}
		if (kegiatans == null || kegiatans.isEmpty()) {
			MyMessageboxConfig.show(
				"Mohon maaf, saat ini tidak ada kegiatan pembayaran yang dapat diperiksa untuk mahasiswa ini. Langkah yang dapat dilakukan: (1) pastikan mahasiswa telah memiliki kegiatan/tagihan pembayaran; (2) muat ulang halaman apabila diperlukan; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.",
				"Informasi",
				MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		final java.util.List<Long> idHapus = new java.util.ArrayList<Long>();
		StringBuilder rincian = new StringBuilder();
		java.util.List<Kegiatan> daftar = new java.util.ArrayList<Kegiatan>(kegiatans.values());
		for (Kegiatan k : daftar) {
			if (k == null || k.getId() == null) {
				continue;
			}
			java.util.List<ais.database.model.DetailKegiatan> asing = ais.action.master.helper.KegiatanPersistenceHelper
				.cariDetailKegiatanAsing(k);
			for (ais.database.model.DetailKegiatan dk : asing) {
				if (dk == null || dk.getId() == null) {
					continue;
				}
				idHapus.add(dk.getId());
				String nm = "";
				try {
					nm = dk.getItemBiaya() == null ? "" : dk.getItemBiaya().getNama();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/InformasiPembayaranMahasiswaAction.java:1768");
				}
				rincian.append("\n- ").append(nm).append("  (Smt ").append(k.getSemster()).append(" / ")
					.append(k.getTahunAkademik()).append(")");
			}
		}
		if (idHapus.isEmpty()) {
			MyMessageboxConfig.show(
				"Tidak ditemukan item tagihan yang tidak sesuai. Seluruh item tagihan telah sesuai dengan biaya yang berlaku atau telah memiliki pembayaran, sehingga tidak ada data yang perlu dibersihkan. Terima kasih.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		MyMessageboxConfig.show(
			MyMessageboxConfig.format(
				"Ditemukan {V1} item tagihan yang tidak sesuai (bukan merupakan biaya yang berlaku dan belum memiliki pembayaran), dengan rincian sebagai berikut: {V2}\n\nApakah Bapak/Ibu ingin menghapus item-item tersebut lalu menghitung ulang tagihan? Perlu diketahui, pembayaran yang telah tercatat sebelumnya tidak akan terpengaruh. Silakan tekan OK untuk melanjutkan, atau Batal untuk membatalkan.",
				idHapus.size(), rincian.toString()),
			"Konfirmasi Bersihkan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
			new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
						return;
					}
					int hapus = 0;
					Session session = null;
					try {
						session = HibernateUtil.openSession();
						org.hibernate.Transaction tx = session.beginTransaction();
						for (Long id : idHapus) {
							try {
								ais.database.model.DetailKegiatan dkdb = (ais.database.model.DetailKegiatan) session
									.get(ais.database.model.DetailKegiatan.class, id);
								if (dkdb != null) {
									session.delete(dkdb);
									hapus++;
								}
							} catch (Exception exDel) {
								Common.tampilErrorJikaAdmin(exDel);
							}
						}
						tx.commit();
					} catch (Exception ex) {
						Common.tampilErrorJikaAdmin(ex);
					} finally {
						if (session != null && session.isOpen()) {
							try {
								session.disconnect();
							} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/InformasiPembayaranMahasiswaAction.java:1815");
							}
							try {
								session.close();
							} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/InformasiPembayaranMahasiswaAction.java:1819");
							}
						}
					}
					hitungUlangSemuaKegiatan();
					MyMessageboxConfig.showFormat(
						"Sebanyak {V1} item tagihan yang tidak sesuai telah berhasil dihapus dan tagihan telah dihitung ulang. Terima kasih.",
						"Selesai", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, hapus);
				}
			});
	}

	private void hitungUlangSemuaKegiatan() throws Exception {
		// Pastikan daftar kegiatan terkini termuat lebih dulu.
		if (kegiatans == null || kegiatans.isEmpty()) {
			loadKegiatan(true, null, null);
		}
		if (kegiatans == null || kegiatans.isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon maaf, saat ini tidak ada kegiatan atau tagihan yang dapat dihitung ulang untuk mahasiswa ini. Langkah yang dapat dilakukan: (1) pastikan mahasiswa telah memiliki kegiatan/tagihan pembayaran; (2) muat ulang halaman apabila diperlukan; (3) apabila memerlukan bantuan, mohon hubungi Administrator sistem.",
					"Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		org.zkoss.zk.ui.util.Clients.showBusy("Menghitung ulang tagihan...");
		int sukses = 0;
		try {
			// Snapshot: loadKegiatan() akan membangun ulang map 'kegiatans'.
			java.util.List<Kegiatan> daftar = new java.util.ArrayList<Kegiatan>(kegiatans.values());
			for (Kegiatan kegiatan : daftar) {
				if (kegiatan == null || kegiatan.getId() == null || kegiatan.getJenisKegiatan() == null) {
					continue;
				}
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					org.hibernate.Transaction tx = session.beginTransaction();
					if (mahasiswa != null) {
						ais.action.master.helper.KegiatanHelper.checkKegiatanMahasiswa(kegiatan,
								kegiatan.getJenisKegiatan(), mahasiswa, kegiatan.getSemster(),
								kegiatan.getTahunAkademik(), true, kegiatan.getJadwalPembayaran(), true, false, null,
								session);
					} else if (calMhs != null) {
						ais.action.master.helper.KegiatanHelper.checkKegiatanCalonMahasiswa(kegiatan,
								kegiatan.getJenisKegiatan(), calMhs, kegiatan.getSemster(),
								kegiatan.getTahunAkademik(), true, kegiatan.getJadwalPembayaran(), true, false, null,
								session);
					}
					tx.commit();
					sukses++;
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
				} finally {
					if (session != null && session.isOpen()) {
						try {
							session.clear();
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/InformasiPembayaranMahasiswaAction.java:1876");
						}
						try {
							session.disconnect();
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/InformasiPembayaranMahasiswaAction.java:1880");
						}
						try {
							session.close();
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/InformasiPembayaranMahasiswaAction.java:1884");
						}
					}
				}
			}
			// Muat ulang grid + dasbor dengan nilai terbaru.
			loadKegiatan(true, null, null);
		} finally {
			org.zkoss.zk.ui.util.Clients.clearBusy();
		}

		MyMessageboxConfig.showFormat(
				"Proses hitung ulang telah selesai dilakukan untuk {V1} kegiatan. Seluruh nilai tagihan terkait telah diperbarui sesuai pengaturan biaya yang berlaku. Terima kasih.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, sukses);
	}

	/**
	 * Hitung tagihan SEGAR satu kegiatan memakai SUMBER YANG SAMA seperti
	 * DetailPembayaranMahasiswaRenderer: jumlah {@code ambilJumlahTagihan} atas DetailBiaya
	 * AKTIF/terkini (getDetailBiayaMahasiswa). Karena daftar DetailBiaya itu HANYA item yang
	 * sekarang berlaku, item LAMA/BASI yang masih nyangkut di JSON {@code tagihans} (mis.
	 * ItemBiaya yang sudah diganti ID-nya) TIDAK ikut terjumlah → hasilnya sama dengan total
	 * pada rincian/detail. READ-ONLY: tidak menyimpan apa pun ke DB.
	 */
	@SuppressWarnings("rawtypes")
	private double hitungTagihanSegar(Kegiatan k) {
		Double segar = tagihanSegarKonsisten(k);
		if (segar != null && segar.doubleValue() > 0.1) {
			return segar.doubleValue();
		}
		return (k != null && k.getTagihan() != null) ? k.getTagihan() : 0.0;
	}

	/**
	 * Versi STATIK & LENGKAP penghitung "Tagihan SEGAR": menjumlahkan tagihan dari item biaya
	 * AKTIF/terkini ({@code getDetailBiayaMahasiswa}) — TERMASUK item BULANAN (diambil ulang
	 * seluruh bulan "-1" agar tiap bulan ikut terhitung, konsisten dgn PembayaranUtil). Tujuannya:
	 * nilai "Tagihan" pada dasbor SAMA dengan rincian/detail dan TIDAK ikut menjumlah item LAMA/BASI
	 * yang masih nyangkut di JSON {@code tagihans} (mis. ItemBiaya berganti ID sehingga ter-DOBEL).
	 * READ-ONLY (tidak menulis DB). Mengembalikan {@code null} bila konfigurasi
	 * {@code dashboard_tagihan_segar_konsisten} non-aktif atau data tak lengkap (pemanggil lalu
	 * fallback ke {@code hitungTagihan()} dari JSON tersimpan).
	 */
	/**
	 * Pembungkus ber-CACHE (per satu kali muat halaman) untuk {@link #tagihanSegarKonsistenHitung}.
	 * Karena penghitung itu READ-ONLY & deterministik namun dipanggil ~3× per kegiatan tiap muat,
	 * hasilnya dimemoisasi per (id kegiatan) di {@link #segarCachePerMuat}. Nilai {@code null}
	 * disimpan sebagai {@code Double.NaN} agar tidak dihitung ulang. Untuk kegiatan tanpa id
	 * (transient) langsung dihitung tanpa cache.
	 */
	private Double tagihanSegarKonsisten(Kegiatan k) {
		if (k == null || k.getId() == null) {
			return tagihanSegarKonsistenHitung(k);
		}
		Double cached = segarCachePerMuat.get(k.getId());
		if (cached != null) {
			return Double.isNaN(cached.doubleValue()) ? null : cached;
		}
		Double val = tagihanSegarKonsistenHitung(k);
		segarCachePerMuat.put(k.getId(), val == null ? Double.valueOf(Double.NaN) : val);
		return val;
	}

	/**
	 * Delegasi ke helper bersama {@link ais.action.master.helper.KegiatanPersistenceHelper#hitungTagihanSegarKonsisten(Kegiatan)}
	 * (dipakai juga oleh DetailSettingBiayaAction agar Detail SettingBiaya menampilkan
	 * angka yang identik dengan dasbor mahasiswa ini) -- lihat javadoc method tsb utk
	 * penjelasan lengkap kenapa perhitungan ini read-only & anti bolak-balik.
	 */
	private static Double tagihanSegarKonsistenHitung(Kegiatan k) {
		return ais.action.master.helper.KegiatanPersistenceHelper.hitungTagihanSegarKonsisten(k);
	}

	public void loadKegiatan(boolean refresh, Integer smtMulai, Integer smtSampai) throws Exception {
		if (fotoGrid != null) {
			// Muat baru → buang memoisasi "tagihan segar" agar tidak basi antar-muat.
			segarCachePerMuat.clear();
			kegiatans = new TreeMap<String, Kegiatan>();
			Collection<Kegiatan> listK = new ArrayList<Kegiatan>();

			if (calMhs != null) {
				listK = calMhs.ambilKegiatans(refresh);
			} else {
				if (mahasiswa.getBiodataCalonMahasiswa() != null) {
					BiodataCalonMahasiswa bcm = mahasiswa.getBiodataCalonMahasiswaData();
					if (bcm != null) {
						listK.addAll(bcm.ambilKegiatans(refresh));
					}
				}
				listK.addAll(mahasiswa.ambilKegiatans(refresh));
			}

			// Semester lulus mahasiswa — bila sudah lulus, tagihan di semester setelah lulus
			// tidak perlu ditampilkan (kegiatan "Daftar Ulang" dst. yang ter-generate otomatis
			// melewati semester kelulusan). Hanya berlaku untuk mahasiswa, bukan calMhs.
			Integer smtLulus = null;
			if (calMhs == null && mahasiswa != null) {
				try {
					Integer sl = mahasiswa.getSemesterLulus();
					if (sl != null && sl > 0) {
						smtLulus = sl;
					}
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/InformasiPembayaranMahasiswaAction.java:2089");
				}
			}

			for (Kegiatan kegiatan : listK) {
				if (kegiatan.getAktif()) {
					Integer smt = kegiatan.getSemster();

					if (smt == null) {
						continue;
					}

					// Sembunyikan tagihan yang melewati semester lulus.
					// Misal: mahasiswa lulus semester 8 → tagihan semester 9, 10, dst. disembunyikan.
					if (smtLulus != null && smt > smtLulus) {
						// Kecuali jenis kegiatan khusus alumni (mis. Wisuda) tetap ditampilkan.
						boolean tagAlumni = kegiatan.getJenisKegiatan() != null
								&& Boolean.TRUE.equals(kegiatan.getJenisKegiatan().getTagihanJugaUntukAlumni());
						if (!tagAlumni) {
							continue;
						}
					}

					if (smtMulai != null && smt < smtMulai) {
						continue;
					}
					if (smtSampai != null && smt > smtSampai) {
						continue;
					}

					if (kegiatan.getJenisKegiatan() != null) {
						Integer minSmtMaster = kegiatan.getJenisKegiatan().getMinSmt();
						Integer maxSmtMaster = kegiatan.getJenisKegiatan().getMaxSmt();

						if (minSmtMaster != null && smt < minSmtMaster) {
							continue;
						}
						if (maxSmtMaster != null && smt > maxSmtMaster) {
							continue;
						}
					}

					if (kegiatan.getAmount().intValue() != 0 || kegiatan.getAmountTerhutang().intValue() != 0) {
						// Kunci TreeMap dibuat String -- "10-x"/"11-x" tersortir SEBELUM "2-x"/"3-x"
						// secara leksikografis (bukan numerik), sehingga baris grid "Daftar Pembayaran
						// Keseluruhan" tampil TIDAK berurutan semester (mis. smt 1,10,11,2,3,...).
						// Padding 3-digit (smt maks realistis <1000) menjaga urutan String = urutan angka.
						kegiatans.put(String.format("%03d", smt) + "-" + kegiatan.getId(), kegiatan);
					}
				}
			}

			// KONSISTENSI dgn DetailPembayaranMahasiswaRenderer: nilai "Tagihan" di dasbor
			// memakai getTagihan() (= hitungTagihan dari JSON 'tagihans' tersimpan) yang bisa
			// BASI/DOBEL bila JSON menyimpan item yang sudah tak terpakai (mis. ItemBiaya lama
			// berganti ID). Di sini dihitung ULANG SEGAR memakai SUMBER YANG SAMA dgn detail:
			// Σ ambilJumlahTagihan(kegiatan, detailBiaya) atas getDetailBiayaMahasiswa (hanya item
			// AKTIF/terkini) → di-set IN-MEMORY (read-only, tak menulis DB) agar SEMUA pemakaian
			// getTagihan() (grid, rekap, kartu) sama persis dgn detail. Toggle:
			// Konfigurasi "dashboard_tagihan_segar_konsisten" = Tidak Aktif untuk mematikan.
			if (mahasiswa != null && Konfigurasi.AKTIF.equals(Common
					.getKonfigurasi("dashboard_tagihan_segar_konsisten", Konfigurasi.AKTIF).getNilai())) {
				for (Kegiatan kegiatanSegar : kegiatans.values()) {
					try {
						double segar = hitungTagihanSegar(kegiatanSegar);
						if (segar > 0.1) {
							kegiatanSegar.setTagihan(segar);
						}
					} catch (Exception eSegar) { ais.common.ErrorAuditUtil.record(eSegar, "auto-audit(empty-catch) src/ais/action/master/InformasiPembayaranMahasiswaAction.java:2153");
						// per-kegiatan: pertahankan nilai tersimpan bila gagal hitung
					}
				}
			}

			fotoGrid.setSclass("dgrid");
			ListModel strset = new SimpleListModel(kegiatans.values().toArray(new Kegiatan[] {}));
			fotoGrid.setRowRenderer(new KegiatanRenderer());
			fotoGrid.setModelCheckMobile(strset);
			fotoGrid.renderAll();

			listK.clear();

			buildDashboardRekap(divDasbor, kegiatans);
		}
	}
	
	private Combobox createListFoto(boolean refresh, Component listContainer, Component detailContainer) throws Exception {
	    
	    // 1. Setup Grid Daftar Pembayaran
	    fotoGrid = new MyGrid();
	    fotoGrid.setMold("paging");
	    fotoGrid.setPageSize(1500);
	    fotoGrid.setParent(listContainer); 
	    fotoGrid.setWidth("100%");

	    Columns columns = new Columns();
	    columns.setParent(fotoGrid);

	    MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setAlign("center");
		column.setWidth("35px");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Pembayaran");
		column.setAlign("center");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Akademik");
		column.setAlign("center");
		column.setWidth("18%");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setAlign("center");
		column.setWidth("5%");
		columnBulan = new MyColumnConfig();
		columnBulan.setParent(columns);
		columnBulan.setLabel("Bulan");
		columnBulan.setAlign("center");
		columnBulan.setWidth("7%");
		columnBulan.setVisible(false);
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tagihan");
		column.setAlign("right");
		column.setWidth("15%");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dibayar");
		column.setAlign("right");
		column.setWidth("15%");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Lunas");
		column.setWidth("10%");
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Cetak");
		column.setWidth("8%");

	    loadKegiatan(refresh, null, null);

	    // 2. Setup Panel Rincian Tagihan agar tidak error
	    // Wrapper Panel agar mematuhi aturan MyPortalchildren
	    Panel pnlRinci = new Panel();
	    pnlRinci.setTitle("Rincian Tagihan");
	    pnlRinci.setBorder("normal");
	    pnlRinci.setCollapsible(true);
	    pnlRinci.setParent(detailContainer); // Sekarang di-add ke portalchildren

	    Panelchildren pchRinci = new Panelchildren();
	    pchRinci.setParent(pnlRinci);

	    // Div ini sekarang berada di dalam Panelchildren, jadi aman
	    west = new Div();
	    west.setWidth("100%");
	    west.setStyle("min-height: 250px; height: auto; overflow: auto;");
	    west.setParent(pchRinci); 

	    final Combobox comboboxPlaceholder = new Combobox(); 

	    vboxRinciOverlay = new Vbox(); 
	    vboxRinciOverlay.setAlign("center");
	    vboxRinciOverlay.setPack("start");
	    vboxRinciOverlay.setWidth("90%");
	    vboxRinciOverlay.setStyle("margin-top: 50px; padding: 20px; border: 1px dashed #ccc; background-color: #f9f9f9; border-radius: 8px; margin-left: auto; margin-right: auto;");
	    vboxRinciOverlay.setParent(west);

	    Label lblRinci = new Label(ais.common.Common.getBahasaConfig("Rincian Tagihan"));
	    lblRinci.setStyle("font-weight: bold; color: #333; font-size: 14px; margin-bottom: 5px;");
	    lblRinci.setParent(vboxRinciOverlay);

	    Label lblSubRinci = new Label(ais.common.Common.getBahasaConfig("Data ditunda pemuatannya untuk mempercepat tampilan. Silakan klik tombol di bawah untuk melihat rincian."));
	    lblSubRinci.setStyle("color: #777; font-size: 11px; margin-bottom: 15px; text-align: center;");
	    lblSubRinci.setParent(vboxRinciOverlay);

	    MyToolbarbuttonConfig btnRinci = new MyToolbarbuttonConfig("Tampilkan Rinci", "/img/search.png");
	    btnRinci.setParent(vboxRinciOverlay);

	    final boolean finalRefresh = refresh;
	    btnRinci.addEventListener("onClick", new EventListener() {
	        @Override
	        public void onEvent(Event arg0) throws Exception {
	            Common.clear(west); 
	            if (mahasiswa != null) {
	                TagihanUIBuilder.loadTagihan(west, selectedJenisKegiatan, mhs, mahasiswa, semua, Common.isMobile(),
	                        false, null, finalRefresh, InformasiPembayaranMahasiswaAction.this);
	            } else if (calMhs != null) {
	                Integer SMT = null;
	                if (selectedJenisKegiatan != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
	                        && selectedJenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
	                    SMT = 0;
	                }
	                TagihanUIBuilder.loadTagihan(west, selectedJenisKegiatan, calMhs, calMhs, semua, Common.isMobile(),
	                        false, SMT, finalRefresh, InformasiPembayaranMahasiswaAction.this);
	            }
	        }
	    });

	    return comboboxPlaceholder;
	}

	private class TampilDetailPembayaran implements EventListener {
		private Kegiatan kegiatan;
		private MyDetail detail;
		private MyGrid detailPembayaranGrid;
		private MyGrid detailTagihanGrid;

		class DetailKegiatanRenderer extends ais.ui.util.MyRowRenderer {
			@Override
			public void render(final Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				final CicilanPembayaran cp = (CicilanPembayaran) arg1;
				new Label(cp.getItemBiaya() == null ? "" : cp.getItemBiaya().getKode()).setParent(arg0);
				new Label(cp.getItemBiaya() == null ? "" : cp.getItemBiaya().getNama()).setParent(arg0);
				new Label(cp.getKeterangan()).setParent(arg0);
				new Label(cp.getTanggal() == null ? "" : Common.dateFormat3.get().format(cp.getTanggal()))
						.setParent(arg0);
				new Label(cp.getValidator()).setParent(arg0);
				new Label(cp.getNilai() == null ? "0" : Common.numberFormat.get().format(cp.getNilai()))
						.setParent(arg0);
			}
		}

		class DetailTagihanRenderer extends ais.ui.util.MyRowRenderer {
			@Override
			public void render(final Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				DetailKegiatan dk = (DetailKegiatan) arg1;
				new Label(dk.getItemBiaya() == null ? "" : dk.getItemBiaya().getKode()).setParent(arg0);
				new Label(dk.getItemBiaya() == null ? "" : dk.getItemBiaya().getNama()).setParent(arg0);
				new Label(dk.getKeterangan()).setParent(arg0);
				new Label(dk.getTanggal() == null ? "" : Common.dateFormat3.get().format(dk.getTanggal()))
						.setParent(arg0);
				// Jumlah = nilai SETELAH diskon (NETO), konsisten dengan DetailPembayaranMahasiswaRenderer
				// (mis. BOP bruto 3.500.000 - diskon 1.700.000 = 1.800.000; item diskon 100% = 0).
				// READ-ONLY: kurangi diskon yang SUDAH tersimpan (dk.getDiskon()); JANGAN panggil
				// hitungDiskon (menulis DB) agar tidak menimbulkan hasil bolak-balik saat di-render ulang.
				double brutoTag = dk.getBiaya() == null ? 0.0 : dk.getBiaya().doubleValue();
				double diskonTag = dk.getDiskon() == null ? 0.0 : dk.getDiskon().doubleValue();
				double netoTag = dk.getBukanTagihan() ? 0.0 : (brutoTag - diskonTag);
				if (netoTag < 0.0) {
					netoTag = 0.0;
				}
				new Label(Common.numberFormat.get().format(netoTag)).setParent(arg0);
			}
		}

		private void createList() {
			Common.clear(detail);
			Vbox vbox = new Vbox();
			vbox.setParent(detail);

			MyGroupboxStyled groupbox = new MyGroupboxStyled();
			groupbox.setParent(vbox);
			groupbox.appendChild(new MyCaptionStyled("Daftar Rincian Pembayaran"));

			detailPembayaranGrid = new MyGrid();
			detailPembayaranGrid.setMold("paging");
			detailPembayaranGrid.setPageSize(15);
			detailPembayaranGrid.setParent(groupbox);
			detailPembayaranGrid.setWidth("100%");

			Columns columns = new Columns();
			columns.setParent(detailPembayaranGrid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kode Item Biaya");
			column.setAlign("center");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama Item Biaya");
			column.setAlign("center");
			column.setWidth("25%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Deskripsi Item Biaya");
			column.setAlign("center");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Waktu");
			column.setAlign("center");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Validator");
			column.setAlign("center");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Jumlah");
			column.setAlign("center");
			column.setWidth("10%");

			loadDetailKegiatan();

			MyGroupboxStyled groupbox1 = new MyGroupboxStyled();
			groupbox1.setParent(vbox);
			groupbox1.appendChild(new MyCaptionStyled("Daftar Rincian Tagihan"));

			detailTagihanGrid = new MyGrid();
			detailTagihanGrid.setMold("paging");
			detailTagihanGrid.setPageSize(15);
			detailTagihanGrid.setParent(groupbox1);
			detailTagihanGrid.setWidth("100%");

			Columns columnTags = new Columns();
			columnTags.setParent(detailTagihanGrid);

			MyColumnConfig columnTag = new MyColumnConfig();
			columnTag.setParent(columnTags);
			columnTag.setLabel("Kode Item Biaya");
			columnTag.setAlign("center");
			columnTag.setWidth("15%");

			columnTag = new MyColumnConfig();
			columnTag.setParent(columnTags);
			columnTag.setLabel("Nama Item Biaya");
			columnTag.setAlign("center");
			columnTag.setWidth("25%");

			columnTag = new MyColumnConfig();
			columnTag.setParent(columnTags);
			columnTag.setLabel("Deskripsi Item Biaya");
			columnTag.setAlign("center");

			columnTag = new MyColumnConfig();
			columnTag.setParent(columnTags);
			columnTag.setLabel("Waktu");
			columnTag.setAlign("center");
			columnTag.setWidth("15%");

			columnTag = new MyColumnConfig();
			columnTag.setParent(columnTags);
			columnTag.setLabel("Jumlah");
			columnTag.setAlign("center");
			columnTag.setWidth("10%");

			loadDetailTagihan();
		}

		private void loadDetailKegiatan() {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				List<CicilanPembayaran> kegiatans = KegiatanPersistenceHelper.ambilCicilan(kegiatan, false);

				ListModel strset = new SimpleListModel(kegiatans);
				detailPembayaranGrid.setRowRenderer(new DetailKegiatanRenderer());
				detailPembayaranGrid.setModelCheckMobile(strset);
				detailPembayaranGrid.setOddRowSclass("non-odd");
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				cleanupSession(session);
			}
		}

		private void loadDetailTagihan() {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				List<DetailKegiatan> allDk = KegiatanPersistenceHelper.ambilDetailKegiatanSaja(kegiatan, false);

				// Kumpulkan ItemBiaya ID yang valid dari template billing saat ini
				// (sumber data sama dengan DetailPembayaranMahasiswaRenderer)
				java.util.Set<Long> validItemIds = new java.util.HashSet<Long>();
				if (kegiatan != null && kegiatan.getMahasiswa() != null && kegiatan.getSemster() != null
						&& kegiatan.getJenisKegiatan() != null) {
					java.util.Collection dbs = ais.action.master.helper.PembayaranUtilHelper
							.getDetailBiayaMahasiswa(kegiatan.getMahasiswa(), kegiatan.getSemster(),
									kegiatan.getJenisKegiatan(), false);
					if (dbs != null) {
						for (Object o : dbs) {
							if (o instanceof ais.database.model.DetailBiaya) {
								ais.database.model.DetailBiaya db = (ais.database.model.DetailBiaya) o;
								if (db.getItemBiaya() != null && db.getItemBiaya().getId() != null)
									validItemIds.add(db.getItemBiaya().getId());
							} else if (o instanceof ais.database.model.PengaturanPembayaranBulanan) {
								ais.database.model.PengaturanPembayaranBulanan ppb =
										(ais.database.model.PengaturanPembayaranBulanan) o;
								if (ppb.getDetailBiaya() != null && ppb.getDetailBiaya().getItemBiaya() != null
										&& ppb.getDetailBiaya().getItemBiaya().getId() != null)
									validItemIds.add(ppb.getDetailBiaya().getItemBiaya().getId());
							}
						}
					}
				}

				// Filter DK ke ItemBiaya yang ada di template; dedup per ItemBiaya (simpan id terbesar)
				java.util.Map<Long, DetailKegiatan> latestByItem =
						new java.util.LinkedHashMap<Long, DetailKegiatan>();
				for (DetailKegiatan dk : allDk) {
					if (dk.getItemBiaya() == null || dk.getItemBiaya().getId() == null) continue;
					Long iid = dk.getItemBiaya().getId();
					if (!validItemIds.isEmpty() && !validItemIds.contains(iid)) continue;
					DetailKegiatan existing = latestByItem.get(iid);
					if (existing == null || (dk.getId() != null && existing.getId() != null
							&& dk.getId() > existing.getId()))
						latestByItem.put(iid, dk);
				}
				List<DetailKegiatan> filtered = new ArrayList<DetailKegiatan>(latestByItem.values());

				ListModel strset = new SimpleListModel(filtered);
				detailTagihanGrid.setRowRenderer(new DetailTagihanRenderer());
				detailTagihanGrid.setModelCheckMobile(strset);
				detailTagihanGrid.setOddRowSclass("non-odd");
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				cleanupSession(session);
			}
		}

		public TampilDetailPembayaran(MyDetail detail, Kegiatan kegiatan) {
			this.kegiatan = kegiatan;
			this.detail = detail;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {
			Common.clear(detail);
			if (detail.isOpen()) {
				createList();
			}
		}
	}

	class KegiatanRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Kegiatan kegiatan = (Kegiatan) arg1;

			try {
				// Hitung ulang tagihan + dibayar + persentase bersama-sama agar baris tidak
				// menampilkan kombinasi yang tidak valid (mis. 4.300.000 / 4.200.000 basi).
				recomputeKegiatanKonsisten(kegiatan);
				double tagihanAmt = kegiatan.getTagihan() == null ? 0.0 : kegiatan.getTagihan();
				double dibayarAmt = kegiatan.getDibayar() == null ? 0.0 : kegiatan.getDibayar();
				arg0.setVisible(((int) (tagihanAmt + dibayarAmt)) != 0);
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new TampilDetailPembayaran(detail, kegiatan));

			RevisiHelper
					.createNewRevisi(Kegiatan.class, kegiatan,
							kegiatan.getJenisKegiatan() == null ? "" : kegiatan.getJenisKegiatan().getNamaKegiatan())
					.setParent(arg0);

			new Label(kegiatan.getTahunAkademik()).setParent(arg0);
			new Label(kegiatan.getSemster() + "").setParent(arg0);
			new Label(kegiatan.getBulan() == null ? "N/A" : kegiatan.getBulan().toString()).setParent(arg0);
			if (!columnBulan.isVisible() && kegiatan.getBulan() != null) {
				columnBulan.setVisible(true);
			}

			new Label(Common.numberFormat.get().format(kegiatan.getTagihan())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kegiatan.getDibayar())).setParent(arg0);
			new Label((kegiatan.getApakahLunas() != null && kegiatan.getApakahLunas() ? "Ya" : "Tidak") + " ("
					+ Common.numberFormat.get().format(kegiatan.getPersentase()) + "%)").setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (kegiatan.getMahasiswa() != null) {
						CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, false);
					} else {
						CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, false);
					}
				}
			});
			button.setParent(toolbar);
		}
	}
}
