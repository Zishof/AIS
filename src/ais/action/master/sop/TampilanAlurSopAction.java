package ais.action.master.sop;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.helper.DasboardSop;
import ais.action.master.sop.helper.ParameterTambahanDisposisiAlurSopListener;
import ais.action.master.sop.helper.RevisiDisposisiAlurSopHelper;
import ais.action.master.sop.helper.SopUtil;
import ais.action.report.Report;
import ais.action.servlet.Wa;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.PenggantianKasKecil;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.ProsesTransferStandingInstruction;
import ais.database.model.akunting.StandingInstruction;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.PembayaranDpMasterAssetDetail;
import ais.database.model.asset.PembayaranPengadaanMasterAssetDetail;
import ais.database.model.asset.PembayaranTerminMasterAssetDetail;
import ais.database.model.file.LampiranLain;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sop.AktorSop;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.sop.DokumenAlurSop;
import ais.database.model.sop.JenisSop;
import ais.database.model.sop.KelompokParameterTambahanAlurSop;
import ais.database.model.sop.ParameterTambahanAlurSop;
import ais.database.model.sop.Sop;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.DataCriteria;
import ais.ui.util.FormSop;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecilBoldBiru;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.SmartDateTimeUtil;

/* TAMPILAN_ALUR_SOP_COMPACT_CARD_V12_2026_06_04 */
/**
 * Controller/action ZK untuk tampilan alur sop. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String ATTR_TAMPILKAN_ALUR_MENUNGGU},
 * {@code Rows rows}, {@code Textbox cari}, {@code Tabpanels tabpanels}, {@code Component menu}, {@code
 * Borderlayout layoutUtama}, {@code Tabs tabs}, {@code MyWindow window}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code buatToolbarAdminLaporan()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code loadMenu()}, {@code loadData()}, {@code tampil()}, {@code
 * tampilInfoAktorDisposisi()}, {@code ambilSemuaAlurSopUntukOverview()}); mutasi data ({@code prosess()}, {@code
 * prosess()}, {@code prosess()}, {@code prosess()}, {@code prosess()}); penghapusan/pembatalan ({@code
 * appendTombolBatalMenungguJikaAktor()}); pelaporan/ekspor ({@code cetakDisposisi()}, {@code
 * renderWorkflowOverviewPanel()}, {@code renderWorkflowOverview()}, {@code renderWorkflowOverview()}, {@code
 * renderWorkflowProgressCard()}, {@code renderBaganSopDefinisi()}); operasi domain lain ({@code
 * cleanupSession()}, {@code isUserLoginTermasukAktorBerikutnya()}, {@code safeString()}, {@code hasText()},
 * {@code createFormSop()}, {@code createEmptyFormData()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
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
public class TampilanAlurSopAction extends GenericAutowireComposer {

	private static final long serialVersionUID = -2301873239699174688L;

	private static final String ATTR_TAMPILKAN_ALUR_MENUNGGU = "TampilanAlurSopAction.TampilkanAlurMenunggu";

	private Rows rows;
	private Textbox cari;
	private Tabpanels tabpanels;
	private Component menu;
	private Borderlayout layoutUtama;
	private Tabs tabs;
	private MyWindow window;
	private String menuBgColor;

	// ===================================================================================
	// HELPER: SESSION CURRENTSESSION
	// ===================================================================================
	private static void cleanupSession(Session session) {
		/*
		 * Class ini sengaja kembali memakai HibernateUtil.currentSession().
		 * Karena currentSession dikelola oleh konteks request/thread aplikasi, helper ini
		 * tidak boleh melakukan disconnect() atau close() terhadap session tersebut.
		 * Method tetap dipertahankan supaya struktur try/finally lama tidak hilang.
		 */
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		menuBgColor = Common.getKonfigurasi("menu_bg_color", "#F5F5F5").getNilai();
		String desktopWidth = execution.getParameter("desktopWidth");

		if (Common.isMobile() || (desktopWidth != null
				&& Integer.parseInt(desktopWidth.replaceAll("px", "")) < ConstantValues.UKURAN_BATAS_MOBILE)) {
			menu = new North();
			((North) menu).setHeight("100%");
			layoutUtama.appendChild(menu);
			if (desktopWidth != null) {
				window.setWidth(desktopWidth);
			}
		} else {
			menu = new West();
			((West) menu).setWidth("200px");
			layoutUtama.appendChild(menu);
		}

		loadMenu();

		MyTabConfig tab = new MyTabConfig("Dashboard SOP (Workflow)");
		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

		if (tab != null) { tab.setParent(tabs); }
		if (tabpanel != null) { tabpanel.setParent(tabpanels); }
		DasboardSop portallayout = new DasboardSop(tabs, tabpanels, menu);
		tabpanel.appendChild(portallayout);
	}

	private void loadMenu() {
		if (menu == null || tabpanels == null || tabs == null) {
			return;
		}

		Row rowPencarian = Common.tampilanScroll1(menu);
		rowPencarian.setStyle("border:0px;background: " + menuBgColor + ";");

		Hbox hbox = new Hbox();
		hbox.setParent(rowPencarian);

		hbox.setWidth("100%");
		hbox.setPack("center");
		hbox.setAlign("center");

		MyLabelConfig c;
		hbox.appendChild(c = new MyLabelConfig("Cari:"));
		c.setStyle("font-size:11px;");

		cari = new Textbox();
		cari.setCols(10);
		hbox.appendChild(cari);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		hbox.appendChild(button);

		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		cari.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		Row rowDicari = new Row();
		rowDicari.setStyle("border:0px;background: transparent;");
		rowDicari.setParent(rowPencarian.getParent());

		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setParent(rowDicari);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100%");

		rows = new Rows();
		rows.setParent(grid);

		loadData(cari.getValue());
	}

	public void loadData(final String keyword) {
		Common.clear(rows);

		Session session = null;
		List<DisposisiSop> disposisiSopes = new ArrayList<DisposisiSop>();

		try {
			session = HibernateUtil.currentSession();
			Criteria criteria = initCriteria(true, session);

			if (keyword != null && !keyword.trim().isEmpty()) {
				criteria.add(Restrictions.or(Restrictions.ilike("properti", keyword.trim(), MatchMode.ANYWHERE),
						Restrictions.or(Restrictions.ilike("keterangan", keyword.trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("sop.nama", keyword.trim(), MatchMode.ANYWHERE))));
			}
			criteria.setMaxResults(500);
			disposisiSopes = ConstantValues.simpleList(criteria, DisposisiSop.class);

			int jumlahkategori = 1;
			JenisSop jenisSop = new JenisSop();
			jenisSop.setId(-1L);

			for (final DisposisiSop disposisiSop : disposisiSopes) {
				if (disposisiSop.getSop().getJenisSop() != null && (jenisSop == null
						|| !jenisSop.getId().equals(disposisiSop.getSop().getJenisSop().getId()))) {
					jenisSop = disposisiSop.getSop().getJenisSop();
					Group group = new ais.ui.util.MyGroupConfig(jenisSop.getNama());
					group.setParent(rows);
					jumlahkategori++;
				}

				final Row row = new Row();
				row.setValign("top");
				row.setStyle("border:0px;background: " + menuBgColor + ";font-size: 10px;");
				row.setParent(rows);

				String text = disposisiSop.getKeterangan() + " (" + disposisiSop.getSop().getNama() + ")";
				text = text.length() > 275 ? text.substring(0, 274) + ".." : text;

				Toolbarbutton toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig(text);
				toolbarbutton.setStyle("font-size: xx-small;");

				row.appendChild(toolbarbutton);
				toolbarbutton.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.scrollIntoView(row);
						TampilanAlurSopAction.prosess(disposisiSop.getId(), tabs, tabpanels, false, menu,
								new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										loadData(keyword);
									}
								});
					}
				});
			}
			if (menu instanceof North) {
				((North) menu).setHeight(((disposisiSopes.size() + jumlahkategori) * 30) + "px");
			}

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (disposisiSopes != null)
				disposisiSopes.clear();
			cleanupSession(session);
		}
	}

	public static void prosess(Long disposisiSopId, Tabs tabspeng, Tabpanels tabpanelspeng, final Component scrollto)
			throws Exception {
		prosess(disposisiSopId, tabspeng, tabpanelspeng, false, scrollto, null);
	}

	public static void prosess(Long disposisiSopId, Tabs tabspeng, Tabpanels tabpanelspeng, final Component scrollto,
			final EventListener dataSearchDefault) throws Exception {
		prosess(disposisiSopId, tabspeng, tabpanelspeng, false, scrollto, dataSearchDefault);
	}

	public static void prosess(Long disposisiSopId, Tabs tabspeng, Tabpanels tabpanelspeng, boolean sederhana,
			final Component scrollto) throws Exception {
		prosess(disposisiSopId, tabspeng, tabpanelspeng, sederhana, false, scrollto, null);
	}

	public static void prosess(Long disposisiSopId, Tabs tabspeng, Tabpanels tabpanelspeng, boolean sederhana,
			final Component scrollto, final EventListener dataSearchDefault) throws Exception {
		prosess(disposisiSopId, tabspeng, tabpanelspeng, sederhana, false, scrollto, dataSearchDefault);
	}

	@SuppressWarnings({ "unchecked" })
	public static void prosess(final Long disposisiSopId, final Tabs tabspeng, final Tabpanels tabpanelspeng,
			final boolean sederhana, final boolean tampiltanpaWindow, final Component scrollto,
			final EventListener dataSearchDefault) throws Exception {

		final DisposisiSop disposisiSop = (DisposisiSop) ConstantValues.ambil(DisposisiSop.class.getName(),
				disposisiSopId);

		if (disposisiSop != null) {
			List<Component> tabpanelsData = tabpanelspeng == null ? new ArrayList<Component>()
					: tabpanelspeng.getChildren();
			synchronized (tabpanelsData) {

				Window window = null;

				if (!tampiltanpaWindow) {
					String desktopWidth = ExecutionsCtrl.getCurrent().getParameter("desktopWidth");
					if (sederhana || Common.isMobile() || (desktopWidth != null && Integer
							.parseInt(desktopWidth.replaceAll("px", "")) < ConstantValues.UKURAN_BATAS_MOBILE)) {
						window = new Window(disposisiSop.getSop().getNama(), "none", false);
						window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						window.setHeight("95%");
						window.setWidth("90%");

						Clients.scrollIntoView(window);
					}
				}

				if (window == null) {
					for (Component cc : tabpanelsData) {
						final Tabpanel myTabpanel = (Tabpanel) cc;
						if (myTabpanel.getAttribute("disposisiSop") == null) {
							continue;
						}

						DisposisiSop myDisposisiSop = (DisposisiSop) myTabpanel.getAttribute("disposisiSop");

						if (myTabpanel != null
								&& myDisposisiSop.getId().toString().equals(disposisiSop.getId().toString())) {
							myTabpanel.getLinkedTab().setSelected(true);
							if (scrollto != null) {
								Clients.scrollIntoView(scrollto);
							}
							return;
						}
					}
				}

				final MyTabConfig tab = new MyTabConfig(disposisiSop.getSop().getNama());
				tab.setClosable(true);
				final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
				if (scrollto != null) {
					Clients.scrollIntoView(scrollto);
				}

				if (window == null) {
					tab.setParent(tabspeng);
					tabpanel.setParent(tabpanelspeng);
				}

				tabpanel.setAttribute("disposisiSop", disposisiSop);

				Borderlayout subSubBorderlayout = new ais.ui.util.MyBorderlayout();
				subSubBorderlayout.setParent(window == null ? tabpanel : window);

				North north = new North();
				north.setParent(subSubBorderlayout);
				ais.ui.util.ZkCompat.setFlex(north, true);
				north.setBorder("none");

				Center mainCenter = new Center();
				mainCenter.setParent(subSubBorderlayout);
				ais.ui.util.ZkCompat.setFlex(mainCenter, true);
				mainCenter.setBorder("none");
				mainCenter.setAutoscroll(true);

				ais.ui.util.MyPortallayout portal = new ais.ui.util.MyPortallayout();
				portal.setStyle("height:100%;overflow:auto;");
				portal.setParent(mainCenter);

				final ais.ui.util.MyPortalchildren leftCol = new ais.ui.util.MyPortalchildren();
				leftCol.setWidth("40%");
				leftCol.setParent(portal);

				final ais.ui.util.MyPortalchildren rightCol = new ais.ui.util.MyPortalchildren();
				rightCol.setWidth("60%");
				rightCol.setStyle("overflow:auto;background:#ffffff;");
				rightCol.setParent(portal);

				final Component subcenter = leftCol;
				final Component east = rightCol;

				final MyToolbarbuttonConfig batalkan = new MyToolbarbuttonConfig("Batalkan Pengajuan Ini",
						"/img/svg/trash.svg");

				Toolbar toolbar = new Toolbar();
				toolbar.setParent(north);

				// =========================================================================
				// TOMBOL REFRESH DENGAN PENANGANAN JSON & SESI YANG BENAR
				// =========================================================================
				MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/refresh.png");
				refresh.setTooltiptext("Refresh");
				refresh.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (disposisiSop.getDisposisiStart() != null
								&& disposisiSop.getDisposisiStart().getAlurSop() != null
								&& hasText(disposisiSop.getDisposisiStart().getAlurSop().getFormInputan())) {
							FormSop formSop = (FormSop) Class
									.forName(disposisiSop.getDisposisiStart().getAlurSop().getFormInputan())
									.newInstance();
							if (formSop != null) {

								String key = formSop.ambilClass().getName();

								GeneralValueObject generalValueObject = null;
								JSONObject o = new JSONObject(disposisiSop.getProperti());
								JSONObject jsonObject = o.isNull(key) ? null : o.getJSONObject(key);

								try {
									Session session = HibernateUtil.currentSession();
									generalValueObject = disposisiSop.ambil(session, formSop);
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}

								if (jsonObject == null)
									jsonObject = new JSONObject();

								if (generalValueObject != null) {
									jsonObject.put("id", generalValueObject.getId());
									jsonObject.put("kode", generalValueObject.getKode());
									jsonObject.put("nama", generalValueObject.getNama());
								}

								o.put(key, jsonObject);

								disposisiSop.setProperti(o.toString());
								// Jaga kolom kode DisposisiSop selalu = kode class di properti (selalu tampil).
								ais.action.master.sop.helper.SopKodeUtil.sinkronkanKode(disposisiSop);
								Common.refreshUpdate(disposisiSop);
							}
						}

						tampil(subcenter, east, disposisiSop, batalkan, dataSearchDefault);
					}
				});
				refresh.setParent(toolbar);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						RevisiDisposisiAlurSopHelper revisiHelper = new RevisiDisposisiAlurSopHelper(disposisiSop,
								new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										tampil(subcenter, east, disposisiSop, batalkan, dataSearchDefault);
									}
								});
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
						revisiHelper.setVisible(true);
						revisiHelper.onModal();
					}
				});
				button.setParent(toolbar);

				String[] contents = new String[] { "diajukanOleh.userNama", "mahasiswa.nama", "siswa.nama", "waktu",
						"waktuMaksimal", "keterangan", "alurSop.kode", "alurSop.nama", "alurSop.sop.kode",
						"alurSop.sop.nama", "alurSop.sop.jurusan", "alurSop.sop.fakultas", "alurSop.sop.yayasan",
						"alurSop.sop.sekolah", "alurSop.sop.satuanKerja", "keyword", "sebelumnya", "setelahnya",
						"properti" };

				MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(DisposisiAlurSop.class, new DataCriteria() {
					@Override
					public Object initCriteria(boolean order) {
						Session sessionCr = HibernateUtil.currentSession();
						return sessionCr.createCriteria(DisposisiAlurSop.class).add(Restrictions.isNotNull("alurSop"))
								.add(Restrictions.eq("disposisiSop", disposisiSop)).addOrder(Order.asc("id"));
					}
				}, contents);
				refresh.getParent().appendChild(cetakToolbarbutton);

				button = new MyToolbarbuttonConfig("Cetak", "/img/svg/printer.svg");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						TampilanAlurSopAction.cetakDisposisi(disposisiSop, false);
					}
				});
				button.setParent(toolbar);

				// ── TOGGLE "PERLUAS TABEL" ── switch on/off modern: saat ON, panel kiri
				// (Riwayat Langkah) disembunyikan & Bagan Alur Proses melebar 100%; saat OFF
				// kembali seperti semula (2 kolom). Dibuat dari ZK Checkbox bersclass
				// "ais-toggle-switch" (di-style CSS jadi sakelar on/off).
				final org.zkoss.zul.Checkbox togglePanel = new org.zkoss.zul.Checkbox("Perluas Tabel");
				togglePanel.setSclass("ais-toggle-switch");
				togglePanel.setTooltiptext(
						"Aktifkan untuk menyembunyikan panel kiri agar Bagan Alur Proses melebar 100%");
				togglePanel.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						boolean on = togglePanel.isChecked();
						// PENTING: CSS .ais-ce-portalchildren memakai "display:flex !important" dan
						// (saat ber-width inline) "min-width:280px !important". Akibatnya setVisible(false)
						// & setWidth("0%") DIABAIKAN (panel kiri tetap tampil ≥280px, kolom kanan 100%
						// malah wrap ke bawah → bagan seolah hilang). Solusi: pakai INLINE !important
						// (prioritas tertinggi, mengalahkan !important pada class).
						leftCol.setVisible(true);
						rightCol.setVisible(true);
						if (on) {
							// Sembunyikan total panel kiri (Riwayat Langkah).
							leftCol.setSclass("sop-riwayat-horizontal"); leftCol.setStyle("display:flex !important;flex:0 0 100% !important;width:100% !important;max-width:100% !important;min-width:0 !important;align-self:flex-start !important;height:auto !important;");
							try { org.zkoss.zk.ui.HtmlBasedComponent prtH = (org.zkoss.zk.ui.HtmlBasedComponent) rightCol.getParent(); if (prtH != null) prtH.setStyle("height:auto;overflow:auto;align-content:flex-start;align-items:flex-start;"); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/sop/TampilanAlurSopAction.java:549");}
							rightCol.setWidth("100%");
							rightCol.setStyle("display:flex !important;flex:1 1 100% !important;width:100% !important;"
									+ "max-width:100% !important;min-width:0 !important;overflow:auto;background:#ffffff;");
						} else {
							// Kembali dua kolom: kiri 40% (lebar bawaan), kanan 60%.
							leftCol.setSclass(""); leftCol.setStyle("");
							try { org.zkoss.zk.ui.HtmlBasedComponent prtH = (org.zkoss.zk.ui.HtmlBasedComponent) rightCol.getParent(); if (prtH != null) prtH.setStyle("height:100%;overflow:auto;"); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/sop/TampilanAlurSopAction.java:556");}
							rightCol.setWidth("60%");
							rightCol.setStyle("overflow:auto;background:#ffffff;");
						}
						togglePanel.setTooltiptext(on
								? "Bagan diperluas — matikan untuk menampilkan kembali panel kiri (Riwayat Langkah)"
								: "Aktifkan untuk menyembunyikan panel kiri agar Bagan Alur Proses melebar 100%");
						// Paksa ZK render ulang SELURUH portal agar lebar kolom (flex) dihitung
						// ulang — kolom kanan benar-benar melebar 100% saat kolom kiri disembunyikan.
						try {
							Component induk = rightCol.getParent();
							if (induk != null) {
								induk.invalidate();
							} else {
								rightCol.invalidate();
							}
						} catch (Exception ig) {
							rightCol.invalidate();
						}
					}
				});
				togglePanel.setParent(toolbar);

				// ── TOMBOL "EDIT SOP INI" — HANYA TAMPIL UNTUK ADMIN ──
				// Admin mendapat pintasan langsung untuk meninjau sekaligus mengelola
				// definisi Bagan Alur + daftar langkah SOP tanpa harus berpindah halaman.
				if (Common.getApakahAdmin()) {
					final Sop sopUntukEdit = disposisiSop.getSop();
					MyToolbarbuttonConfig btnEditSop = new MyToolbarbuttonConfig("Edit SOP ini", "/img/svg/edit.svg");
					btnEditSop.setTooltiptext("Lihat dan kelola definisi langkah Alur SOP ini (Bagan + Tabel) — hanya admin");
					btnEditSop.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							bukaPopupEditSop(sopUntukEdit);
						}
					});
					btnEditSop.setParent(toolbar);
				}

				Tbmuser tbmuser = Common.getCurrentUser();

				if ((tbmuser != null && tbmuser.getUserId() != null && disposisiSop.getDiajukanOleh() != null
						&& disposisiSop.getDiajukanOleh().getUserId() != null
						&& tbmuser.getUserId().equals(disposisiSop.getDiajukanOleh().getUserId()))) {

					final Window w = window;

					batalkan.setTooltiptext("Hapus Data");
					batalkan.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show("Apakah yakin ingin membatalkan pengajuan ini ?", "Pertanyaan",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {
													disposisiSop.setAktif(false);
													Common.refreshUpdate(disposisiSop);
													try {
														if (w != null) {
															w.setVisible(false);
														} else {
															try {
																tabpanel.setVisible(false);
																tabpanel.getLinkedTab().setVisible(false);
															} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
															try {
																tabpanel.getTabbox().setSelectedIndex(0);
															} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
														}
														if (dataSearchDefault != null) {
															dataSearchDefault.onEvent(null);
														}
													} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
													MyMessageboxConfig.show(
															"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																	+ e.getMessage());
												}
											}
										}
									});
						}
					});
					batalkan.setParent(toolbar);
				}

				tampil(subcenter, east, disposisiSop, batalkan, dataSearchDefault);

				if (window != null) {
					try {
						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(subSubBorderlayout);

						final Window myWindow = window;
						Toolbar toolbar1 = new Toolbar();
						toolbar1.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
						cancel.setTooltiptext("Tutup");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								myWindow.detach();
								if (dataSearchDefault != null) {
									dataSearchDefault.onEvent(null);
								}
							}
						});
						cancel.setParent(toolbar1);

						window.onModal();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				} else {
					tab.setSelected(true);
					Clients.scrollIntoView(tab);
				}
			}
		}
	}



	private static void appendTombolBatalMenungguJikaAktor(final Vbox parent,
			final DisposisiAlurSop disposisiAlurSop, final DisposisiSop attachedSopForListener,
			final Component subcenter, final Component eastPanel, final Button batalkan,
			final EventListener dataSearchDefault) {
		if (parent == null || disposisiAlurSop == null || attachedSopForListener == null) {
			return;
		}

		MyToolbarbuttonConfig buttonHapus = new MyToolbarbuttonConfig("Batal", "/img/svg/trash.svg");
		buttonHapus.setTooltiptext("Batalkan proses menunggu ini");
		buttonHapus.setVisible(true);
		buttonHapus.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									Session s = null;
									Transaction tx = null;
									try {
										s = HibernateUtil.currentSession();
										tx = s.beginTransaction();

										if (disposisiAlurSop.getAlurSop().getStart()) {
											String sql = "delete from disposisi_alur_sop where disposisi_sop="
													+ attachedSopForListener.getId();
											s.createSQLQuery(sql).executeUpdate();
											tx.commit();

											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													Session s2 = null;
													Transaction tx2 = null;
													try {
														s2 = HibernateUtil.currentSession();
														tx2 = s2.beginTransaction();
														String sql2 = "delete from disposisi_sop where id="
																+ attachedSopForListener.getId();
														s2.createSQLQuery(sql2).executeUpdate();
														tx2.commit();
													} catch (Exception ex) {
														if (tx2 != null)
															tx2.rollback();
													} finally {
														cleanupSession(s2);
													}

													Component _c = subcenter;
													while (_c != null && _c.getParent() != null
															&& !(_c.getParent() instanceof Tabpanel)
															&& !(_c.getParent() instanceof Window)) {
														_c = _c.getParent();
													}
													Component _host = (_c != null) ? _c.getParent() : null;
													if (_host instanceof Tabpanel) {
														Tabpanel tabpanel = (Tabpanel) _host;
														Tab tab = tabpanel.getLinkedTab();
														tabpanel.detach();
														tab.detach();
													} else if (_host != null) {
														_host.detach();
													}
												}
											});

										} else {
											if (attachedSopForListener.getDisposisiEnd() != null
													&& attachedSopForListener.getDisposisiEnd().getId()
															.equals(disposisiAlurSop.getId())) {
												attachedSopForListener.setDisposisiEnd(null);
											}
											if (attachedSopForListener.getDisposisiSetuju() != null
													&& attachedSopForListener.getDisposisiSetuju().getId()
															.equals(disposisiAlurSop.getId())) {
												attachedSopForListener.setDisposisiSetuju(null);
											}
											if (attachedSopForListener.getDisposisiStart() != null
													&& attachedSopForListener.getDisposisiStart().getId()
															.equals(disposisiAlurSop.getId())) {
												attachedSopForListener.setDisposisiStart(null);
											}

											s.update(attachedSopForListener);
											// Hapus instance TERKELOLA: proxy #id (via getDisposisi* / lazy-load) bisa terdaftar di
											// session sedangkan disposisiAlurSop mungkin DETACHED -> s.delete(detached) memicu
											// NonUniqueObjectException "a different object with the same identifier". Ambil via
											// s.get lalu hapus instance itu (null-safe bila baris sudah tidak ada di DB).
											DisposisiAlurSop alurHapus = (DisposisiAlurSop) s.get(DisposisiAlurSop.class, disposisiAlurSop.getId());
											if (alurHapus != null) {
												s.delete(alurHapus);
											}
											tx.commit();

											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event arg0) throws Exception {
													tampil(subcenter, eastPanel, attachedSopForListener, batalkan,
															dataSearchDefault);
													if (dataSearchDefault != null) {
														dataSearchDefault.onEvent(null);
													}
												}
											});
										}
									} catch (Exception e) {
										if (tx != null)
											tx.rollback();
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(
												"Data ini tidak dapat dihapus karena digunakan untuk transaksi");
									} finally {
										cleanupSession(s);
									}
								}
							}
						});
			}
		});
		buttonHapus.setParent(parent);
	}

	private static boolean isUserLoginTermasukAktorBerikutnya(Tbmuser tbmuserCurrent, Component actorContainer,
			boolean hasilSopUtil) {
		if (tbmuserCurrent == null || tbmuserCurrent.getUserId() == null) {
			return false;
		}

		try {
			Object usernamePengguna = actorContainer == null ? null : actorContainer.getAttribute("usernamePengguna");
			if (usernamePengguna != null && hasText(usernamePengguna.toString())) {
				String userLogin = tbmuserCurrent.getUserId().trim();
				String[] daftar = usernamePengguna.toString().split("[;,]");
				for (int i = 0; i < daftar.length; i++) {
					String userAktor = daftar[i] == null ? "" : daftar[i].trim();
					if (userAktor.equalsIgnoreCase(userLogin)) {
						return true;
					}
				}
				return false;
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		return hasilSopUtil;
	}

	private static String safeString(String value) {
		return value == null ? "" : value;
	}

	private static boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}

	private static FormSop createFormSop(String className) throws Exception {
		if (!hasText(className)) {
			return null;
		}
		Object obj = Class.forName(className.trim()).newInstance();
		if (!(obj instanceof FormSop)) {
			return null;
		}
		FormSop formSop = (FormSop) obj;
		formSop.setPersetujuan(true);
		return formSop;
	}

	private static GeneralValueObject createEmptyFormData(FormSop formSop) {
		try {
			return formSop == null || formSop.ambilClass() == null ? null
					: (GeneralValueObject) formSop.ambilClass().newInstance();
		} catch (Exception e) {
			return null;
		}
	}

	private static GeneralValueObject resolveFormData(Session session, DisposisiSop disposisiSop, FormSop formSop) {
		GeneralValueObject data = null;
		if (formSop == null) {
			return null;
		}

		try {
			if (session != null && disposisiSop != null && disposisiSop.getId() != null) {
				data = disposisiSop.ambil(session, formSop);
				if (data != null && data.getId() != null) {
					return data;
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		try {
			if (disposisiSop != null && hasText(disposisiSop.getProperti())) {
				data = resolveFormDataFromJson(disposisiSop.getProperti(), formSop);
				if (data != null && data.getId() != null) {
					return data;
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		/*
		 * Workflow lama tidak selalu menyalin referensi form ke properti induk
		 * DisposisiSop. Referensi id tetap tersimpan pada langkah awal. Tanpa
		 * fallback ini form dibangun dari object kosong sehingga seluruh detail
		 * (pegawai, status izin, periode dan jatah cuti) tampak blank.
		 */
		try {
			if (disposisiSop != null && disposisiSop.getDisposisiStart() != null
					&& hasText(disposisiSop.getDisposisiStart().getProperti())) {
				data = resolveFormDataFromJson(disposisiSop.getDisposisiStart().getProperti(), formSop);
				if (data != null && data.getId() != null) {
					return data;
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		try {
			if (session != null && formSop.ambilClass() != null && disposisiSop != null && disposisiSop.getId() != null) {
				// FlushMode MANUAL: lookup data form (read-only) tidak boleh memicu auto-flush yang
				// menulis entitas kotor di tengah transaksi (penyebab deadlock pada pembayaran_termin).
				data = (GeneralValueObject) session.createCriteria(formSop.ambilClass())
						.add(Restrictions.eq("disposisiSop", disposisiSop)).setMaxResults(1)
						.setFlushMode(org.hibernate.FlushMode.MANUAL).uniqueResult();
				if (data != null && data.getId() != null) {
					return data;
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		return data == null ? createEmptyFormData(formSop) : data;
	}

	private static GeneralValueObject resolveFormDataFromJson(String properti, FormSop formSop) throws Exception {
		if (!hasText(properti) || formSop == null || formSop.ambilClass() == null) {
			return null;
		}
		JSONObject root = new JSONObject(properti);
		JSONObject jsonObject = root;
		String key = formSop.ambilClass().getName();
		if (!root.isNull(key)) {
			Object nilai = root.get(key);
			if (nilai instanceof JSONObject) {
				jsonObject = (JSONObject) nilai;
			}
		}
		if (jsonObject == null || jsonObject.isNull("id")) {
			return null;
		}
		String id = String.valueOf(jsonObject.get("id"));
		if (!hasText(id)) {
			return null;
		}
		return (GeneralValueObject) GeneralValueObject.ambilData(formSop.ambilClass(), id, true);
	}

	private static Long createMapKey(GeneralValueObject data) {
		if (data != null && data.getId() != null) {
			return data.getId();
		}
		return Common.randLong();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static File cetakDisposisi(final DisposisiSop disposisiSop, final boolean kirim) throws Exception {

		Session session = null;
		File fileData = null;
		File fileHasilTemp = null;
		File fileHasil = null;

		List<DisposisiAlurSop> disposisiAlurSops = new ArrayList<DisposisiAlurSop>();
		Map parameters = new HashMap();
		HashMap<String, String> telps = new HashMap<String, String>();
		Set<String> emails = new HashSet<String>();
		JSONArray userIds = new JSONArray();
		List<Map> maps = new ArrayList<Map>();
		String kode = "";
		GeneralValueObject generalValueObject = null;
		FormSop formSop = null;
		boolean formSudahDiproses = false;

		try {
			session = HibernateUtil.currentSession();

			disposisiAlurSops = session.createCriteria(DisposisiAlurSop.class).add(Restrictions.isNotNull("alurSop"))
					.add(Restrictions.eq("disposisiSop", disposisiSop)).addOrder(Order.asc("id")).list();

			Common.insertProperty(DisposisiSop.class, disposisiSop, parameters, "");

			for (DisposisiAlurSop disposisiAlurSop : disposisiAlurSops) {
				Map map = new HashMap();
				Common.insertProperty(DisposisiAlurSop.class, disposisiAlurSop, map, "", 3, "");
				maps.add(map);

				// Expand SEMUA nilai parameter tambahan alur ini ke parameter laporan — termasuk
				// SELURUH property objek terpilih (mis. PenyediaAsset utk parameter vendor).
				// Key yang tersedia di JRXML: param.id.<ptId>, param.nama.<nama>, param.kode.<kode>,
				// <kelId>_<ptId>, dan <kelId>_<ptId>.<field> / param.kode.<kode>.<field>.
				try {
					ParameterTambahan.masukkanSemuaParameterKeMap(disposisiAlurSop.getParameterTambahanInds(),
							parameters);
					ParameterTambahan.masukkanSemuaParameterKeMap(disposisiAlurSop.getParameterTambahanInds(), map);
				} catch (Exception ePar) {
					ais.common.ErrorAuditUtil.record(ePar, "cetakDisposisi: expand parameter tambahan");
				}

				try {
					if (disposisiAlurSop.getMahasiswa() != null) {
						userIds.put(disposisiAlurSop.getMahasiswa().getNim());
					}
					if (disposisiAlurSop.getSiswa() != null
							&& disposisiAlurSop.getSiswa().getNomorIndukNasional() != null
							&& !disposisiAlurSop.getSiswa().getNomorIndukNasional().trim().isEmpty()) {
						userIds.put(disposisiAlurSop.getSiswa().getNomorIndukNasional());
					}
					if (disposisiAlurSop.getDiajukanOleh() != null) {
						userIds.put(disposisiAlurSop.getDiajukanOleh().getUserId());
					}

					if (disposisiAlurSop.getMahasiswa() != null && disposisiAlurSop.getMahasiswa().getTelp() != null
							&& !disposisiAlurSop.getMahasiswa().getTelp().trim().isEmpty()) {
						telps.put(disposisiAlurSop.getMahasiswa().getTelp(), disposisiAlurSop.getMahasiswa().getNama());
					}
					if (disposisiAlurSop.getMahasiswa() != null
							&& disposisiAlurSop.getMahasiswa().getBiodataCalonMahasiswaData() != null
							&& disposisiAlurSop.getMahasiswa().getBiodataCalonMahasiswaData().getHp() != null
							&& !disposisiAlurSop.getMahasiswa().getBiodataCalonMahasiswaData().getHp().trim()
									.isEmpty()) {
						telps.put(disposisiAlurSop.getMahasiswa().getBiodataCalonMahasiswaData().getHp(),
								disposisiAlurSop.getMahasiswa().getNama());
					}
					if (disposisiAlurSop.getMahasiswa() != null
							&& disposisiAlurSop.getMahasiswa().getBiodataCalonMahasiswaData() != null
							&& disposisiAlurSop.getMahasiswa().getBiodataCalonMahasiswaData().getTeleponRumah() != null
							&& !disposisiAlurSop.getMahasiswa().getBiodataCalonMahasiswaData().getTeleponRumah().trim()
									.isEmpty()) {
						telps.put(disposisiAlurSop.getMahasiswa().getBiodataCalonMahasiswaData().getTeleponRumah(),
								disposisiAlurSop.getMahasiswa().getNama());
					}
					if (disposisiAlurSop.getSiswa() != null) {
						for (String n : disposisiAlurSop.getSiswa().ambilTelp()) {
							telps.put(n, disposisiAlurSop.getSiswa().getNama());
						}
					}
					if (disposisiAlurSop.getDiajukanOleh() != null && disposisiAlurSop.getDiajukanOleh().getHp() != null
							&& !disposisiAlurSop.getDiajukanOleh().getHp().trim().isEmpty()) {
						telps.put(disposisiAlurSop.getDiajukanOleh().getHp(),
								disposisiAlurSop.getDiajukanOleh().getUserNama());
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

				if (disposisiAlurSop.getMahasiswa() != null && disposisiAlurSop.getMahasiswa().getEmail() != null
						&& !disposisiAlurSop.getMahasiswa().getEmail().isEmpty()) {
					emails.add(disposisiAlurSop.getMahasiswa().getEmail());
				}
				if (disposisiAlurSop.getSiswa() != null && disposisiAlurSop.getSiswa().getAlamatEmail() != null
						&& !disposisiAlurSop.getSiswa().getAlamatEmail().isEmpty()) {
					emails.add(disposisiAlurSop.getSiswa().getAlamatEmail());
				}
				if (disposisiAlurSop.getDiajukanOleh() != null && disposisiAlurSop.getDiajukanOleh().getEmail() != null
						&& !disposisiAlurSop.getDiajukanOleh().getEmail().isEmpty()) {
					emails.add(disposisiAlurSop.getDiajukanOleh().getEmail());
				}

				AlurSop alurSop = disposisiAlurSop.getAlurSop();
				if (!formSudahDiproses && hasText(alurSop.getFormInputan())) {
					try {
						formSop = createFormSop(alurSop.getFormInputan());
						if (formSop != null) {
							generalValueObject = resolveFormData(session, disposisiSop, formSop);
							if (generalValueObject == null) {
								generalValueObject = createEmptyFormData(formSop);
							}

							// PENTING: formSop.form(...) MEMBANGUN komponen ZK (Paging/Grid → Events.postEvent,
							// setMold, setPageSize) yang MEMBUTUHKAN ZK Execution aktif. cetakDisposisi sering
							// dijalankan di THREAD LATAR (DisposisiSopAction$11$1.run, performa) yang TIDAK punya
							// Execution → form() melempar NullPointerException (Events.postEvent). Data untuk PDF
							// (kode + property "data") diambil dari generalValueObject (hasil resolveFormData),
							// BUKAN dari komponen grid; sedangkan isi form pada PDF dihasilkan terpisah lewat
							// formSop.cetakData(...). Maka build grid HANYA dilakukan bila ada Execution; bila tidak,
							// dilewati tanpa mengganggu data PDF.
							if (org.zkoss.zk.ui.Executions.getCurrent() != null) {
								try {
									MyGrid rowLampiran = formSop.form(generalValueObject, disposisiSop,
											new MyToolbarbuttonConfig(), null);
									if (generalValueObject != null) {
										map.put(createMapKey(generalValueObject), rowLampiran);
									}
								} catch (Exception eForm) {
									ais.common.Common.tampilErrorJikaAdmin(eForm);
								}
							}
							if (generalValueObject != null) {
								kode = generalValueObject.getKode();
								Common.insertProperty(formSop.ambilClass(), generalValueObject, parameters, "data");
							}
							formSudahDiproses = true;
						}
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
			}

			try {
				if (generalValueObject != null && generalValueObject.getId() != null && formSop != null) {
					fileData = formSop.cetakData(generalValueObject);
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			parameters.put("maps", maps);

			LampiranLain lainMahasiswa = LampiranLain.ambil(disposisiSop.getSop().getId(),
					LampiranLain.FILE_JRXML_LAYOUT_DISPOSISI_ALUR_SOP);

			if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
				fileHasilTemp = Report.generateCompileFileReport(Report.PDF, parameters,
						lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
			} else {
				fileHasilTemp = Report.generateFileReport(Report.PDF, parameters, "disposisi_sop",
						disposisiSop.getTanggal_dirubah(), new Toolbar());
			}

			if (fileData != null && fileData.exists()) {
				fileHasil = new File(
						fileHasilTemp.getParentFile().getAbsolutePath() + "/" + Common.getGeneratedBarCode() + ".pdf");
				PDFMergerUtility ut = new PDFMergerUtility();
				ut.addSource(fileHasilTemp);
				ut.addSource(fileData);
				ut.setDestinationStream(new FileOutputStream(fileHasil));
				ut.mergeDocuments();
				// Salin HTML companion agar toggle pratinjau HTML/PDF muncul setelah merge
				File htmlTemp = new File(fileHasilTemp.getAbsolutePath() + ".html");
				if (htmlTemp.exists() && htmlTemp.length() > 0) {
					File htmlHasil = new File(fileHasil.getAbsolutePath() + ".html");
					java.io.FileInputStream isCopy = null;
					java.io.FileOutputStream osCopy = null;
					try {
						isCopy = new java.io.FileInputStream(htmlTemp);
						osCopy = new java.io.FileOutputStream(htmlHasil);
						byte[] buf = new byte[8192];
						int n;
						while ((n = isCopy.read(buf)) > 0) osCopy.write(buf, 0, n);
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/sop/TampilanAlurSopAction.java:1110");
					} finally {
						if (isCopy != null) try { isCopy.close(); } catch (Exception ig2) { ais.common.ErrorAuditUtil.record(ig2, "auto-audit(empty-catch) src/ais/action/master/sop/TampilanAlurSopAction.java:1112");}
						if (osCopy != null) try { osCopy.close(); } catch (Exception ig2) { ais.common.ErrorAuditUtil.record(ig2, "auto-audit(empty-catch) src/ais/action/master/sop/TampilanAlurSopAction.java:1113");}
					}
				}
			} else {
				fileHasil = fileHasilTemp;
			}

			if (kirim) {
				String emailUser = "";
				for (String email : emails) {
					if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email)) {
						emailUser += emailUser.trim().isEmpty() ? email.trim() : "," + email.trim();
					}
				}

				if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
					String subject = disposisiSop.getSop().getNama() + " (" + disposisiSop.getSop().getKode() + ") "
							+ kode;
					String body = "Yth. Bapak/Ibu/Sdr/i<br><br>Dengan hormat,<br><br>";
					body += "Bersama ini, kami memberitahukan bahwa Anda telah menerima alur disposisi SOP<br><br>Dimohon untuk segera menindaklanjuti disposisi tersebut sesuai dengan arahan yang tertera dalam file terlampir.<br>"
							+ "\r\nDemikian notifikasi/pemberitahuan ini kami sampaikan, atas perhatian dan kerjasamanya kami ucapkan terima kasih.";

					String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
					MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser, disposisiSop, false,
							fileHasil);
				}

				if (Common.bolehKonfigurasi("aktifkan_kirim_notif_disposisi_sop_ke_wa")) {
					if (!telps.isEmpty()) {
						String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal",
								"*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n")
								.getNilai();
						for (String telp : telps.keySet()) {
							String nama = telps.get(telp);
							String body = "Dengan hormat *" + nama + "*,\n\n";
							body += "Bersama ini, kami memberitahukan bahwa Anda telah menerima alur disposisi SOP. \nDimohon untuk segera menindaklanjuti disposisi tersebut sesuai dengan arahan yang tertera dalam file terlampir.\n"
									+ "\r\nDemikian notifikasi/pemberitahuan ini kami sampaikan, atas perhatian dan kerjasamanya kami ucapkan terima kasih.";

							String urlD = null;
							if (fileHasil != null) {
								urlD = Common.getRequestHostWithProtocolSimple()
										+ fileHasil.getAbsolutePath().split("webapps")[1];
							}
							Wa.kirimWaViaUltramsg(telp, dawal + body, "Disposisi_SOP.pdf", urlD);
						}
					}
				}
			} else {
				// Admin melihat toolbar khusus: Parameter, Download/Upload JRXML, History —
				// untuk merawat format layout laporan disposisi SOP ini tanpa keluar dari popup.
				if (Common.getApakahAdmin()) {
					Report.tampil(fileHasil, parameters, buatToolbarAdminLaporan(disposisiSop, parameters));
				} else {
					Report.tampil(fileHasil, parameters);
				}
			}

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (disposisiAlurSops != null)
				disposisiAlurSops.clear();
			if (parameters != null)
				parameters.clear();
			if (maps != null)
				maps.clear();
			cleanupSession(session);
		}

		return fileHasil;
	}

	/**
	 * Toolbar khusus ADMIN pada popup Laporan disposisi SOP: tombol <b>Parameter</b> (daftar seluruh
	 * parameter yang dipakai laporan), <b>Download/Upload JRXML</b> (format layout per-SOP, penyimpanan
	 * sama dengan menu SOP), dan <b>History</b> (info format layout yang sedang aktif). Hanya dipasang
	 * bila {@code Common.getApakahAdmin()}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Toolbar buatToolbarAdminLaporan(final DisposisiSop disposisiSop, Map parameters) {
		// Salin map: pemanggil MENG-CLEAR parameters di blok finally setelah popup tampil,
		// sementara tombol Parameter baru dibaca saat diklik.
		final Map paramSnapshot = new java.util.LinkedHashMap(parameters == null ? new HashMap() : parameters);

		Toolbar toolbar = new Toolbar();
		toolbar.setAlign("end");
		toolbar.setStyle("border:0;background:transparent;padding:2px 6px;white-space:nowrap;");

		// SATU BARIS HORIZONTAL: semua tombol dirangkai dalam Hbox (tabel satu baris,
		// tidak pernah wrap) agar toolbar tidak menumpuk vertikal/meninggi.
		Hbox barisTombol = new Hbox();
		barisTombol.setAlign("center");
		barisTombol.setSpacing("10px");
		barisTombol.setParent(toolbar);

		MyToolbarbuttonConfig btnParam = new MyToolbarbuttonConfig("Parameter", "/img/svg/search.svg");
		btnParam.setTooltiptext("Lihat seluruh parameter yang dipakai laporan ini");
		btnParam.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				MyWindow win = new MyWindow("Parameter Laporan", "none", true);
				win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				win.setWidth("700px");
				win.setHeight("70%");

				// Searchbox filter: cocokkan ke NAMA parameter DAN NILAI-nya (live saat
				// mengetik), case-insensitive — memudahkan mencari di daftar yang panjang.
				Hbox barisCari = new Hbox();
				barisCari.setAlign("center");
				barisCari.setWidth("100%");
				barisCari.setStyle("padding:4px 6px;");
				barisCari.setParent(win);
				barisCari.appendChild(new MyLabelConfig("Cari"));
				final org.zkoss.zul.Textbox cari = new org.zkoss.zul.Textbox();
				cari.setWidth("92%");
				barisCari.appendChild(cari);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setHeight("92%");
				grid.setParent(win);

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig col = new MyColumnConfig();
				col.setLabel("Parameter");
				col.setWidth("35%");
				col.setParent(columns);
				col = new MyColumnConfig();
				col.setLabel("Nilai");
				col.setParent(columns);

				final Rows rows = new Rows();
				rows.setParent(grid);

				final EventListener render = new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						String saring = cari.getValue() == null ? "" : cari.getValue().trim().toLowerCase();
						if (ev instanceof org.zkoss.zk.ui.event.InputEvent) {
							String v = ((org.zkoss.zk.ui.event.InputEvent) ev).getValue();
							saring = v == null ? "" : v.trim().toLowerCase();
						}
						Common.clear(rows);
						for (Object key : paramSnapshot.keySet()) {
							Object val = paramSnapshot.get(key);
							String namaParam = String.valueOf(key);
							String teksPenuh = val == null ? "" : String.valueOf(val);
							if (!saring.isEmpty() && namaParam.toLowerCase().indexOf(saring) < 0
									&& teksPenuh.toLowerCase().indexOf(saring) < 0) {
								continue;
							}
							String teks = teksPenuh.length() > 300 ? teksPenuh.substring(0, 300) + "..." : teksPenuh;
							Row row = new Row();
							row.setParent(rows);
							row.appendChild(new MyLabelConfig(namaParam));
							row.appendChild(new Label(teks));
						}
					}
				};
				render.onEvent(null);
				cari.addEventListener("onChanging", render);
				cari.addEventListener("onChange", render);
				cari.addEventListener("onOK", render);

				win.setVisible(true);
				win.onModal();
			}
		});
		barisTombol.appendChild(btnParam);

		MyToolbarbuttonConfig btnHistory = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		btnHistory.setTooltiptext("Info format layout JRXML yang sedang aktif untuk SOP ini");
		btnHistory.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				LampiranLain aktif = LampiranLain.ambil(disposisiSop.getSop().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_DISPOSISI_ALUR_SOP, true);
				String pesan;
				if (aktif != null && aktif.getId() != null) {
					pesan = "Laporan ini memakai format layout JRXML KUSTOM yang diunggah untuk SOP \""
							+ disposisiSop.getSop().getNama() + "\".\n"
							+ "Nama file : " + (aktif.getNama() == null ? "-" : aktif.getNama()) + "\n"
							+ "Terakhir diubah : " + (aktif.getTanggal_dirubah() == null ? "-"
									: Common.dateFormat.get().format(aktif.getTanggal_dirubah())) + "\n"
							+ "Oleh : " + (aktif.getOleh() == null || aktif.getOleh().trim().isEmpty()
									? (aktif.getOlehId() == null ? "-" : aktif.getOlehId()) : aktif.getOleh());
				} else {
					pesan = "Laporan ini memakai format layout BAWAAN sistem (report/disposisi_sop.jrxml). "
							+ "Belum ada file JRXML kustom yang diunggah untuk SOP \""
							+ disposisiSop.getSop().getNama() + "\".";
				}
				MyMessageboxConfig.show(pesan, "History Format Layout", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
			}
		});
		barisTombol.appendChild(btnHistory);

		// Download *.jrxml — SELALU tampil: unduh file KUSTOM per-SOP bila sudah pernah
		// diunggah; bila belum, unduh SUMBER BAWAAN FormSop (report/disposisi_sop.jrxml)
		// sebagai template awal untuk diedit admin.
		MyToolbarbuttonConfig btnJrxml = new MyToolbarbuttonConfig("Download *.jrxml", "/img/download.png");
		btnJrxml.setTooltiptext("Unduh format layout JRXML yang sedang aktif (kustom bila ada, bawaan bila belum)");
		btnJrxml.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				LampiranLain aktif = LampiranLain.ambil(disposisiSop.getSop().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_DISPOSISI_ALUR_SOP, true);
				if (aktif != null && aktif.getId() != null && aktif.ambilFile() != null
						&& aktif.ambilFile().exists()) {
					org.zkoss.zul.Filedownload.save(aktif.ambilFile(), "text/xml");
					return;
				}
				File sumber = new File(Common.ambilREAL_PATH_REPORT() + "/disposisi_sop.jrxml");
				if (sumber.exists()) {
					org.zkoss.zul.Filedownload.save(sumber, "text/xml");
				} else {
					MyMessageboxConfig.show(
							"File sumber report/disposisi_sop.jrxml tidak ditemukan di server. Mohon hubungi tim teknis.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
			}
		});
		barisTombol.appendChild(btnJrxml);

		// Upload JRXML per-SOP — widget & penyimpanan SAMA dengan menu SOP
		// (SopAction: LampiranLain ref=sop.id jenis=FILE_JRXML_LAYOUT_DISPOSISI_ALUR_SOP),
		// sehingga file yang diunggah dari sini langsung dipakai cetakDisposisi berikutnya.
		// Label dipendekkan + ikut dalam Hbox agar tetap satu baris horizontal.
		Hbox selUpload = new Hbox();
		selUpload.setAlign("center");
		barisTombol.appendChild(selUpload);
		LampiranLain.createDownloadUploadFileLain(selUpload, disposisiSop.getSop().getId(),
				LampiranLain.FILE_JRXML_LAYOUT_DISPOSISI_ALUR_SOP, "JRXML", false,
				new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						MyMessageboxConfig.show(
								"File format JRXML tersimpan. Tutup lalu buka kembali laporan ini untuk melihat "
										+ "hasil dengan format baru.",
								"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}
				}, null, false, false, false, true);

		return toolbar;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private static void tampil(final Component subcenter, final Component eastPanel, final DisposisiSop disposisiSop,
			final Button batalkan, final EventListener dataSearchDefault) throws Exception {
		final boolean tampilkanAlurMenunggu = eastPanel != null
				&& Boolean.TRUE.equals(eastPanel.getAttribute(ATTR_TAMPILKAN_ALUR_MENUNGGU));

		Common.clear(subcenter);
		Common.clear(eastPanel);

		Vbox dataPanelContainer = new Vbox();
		dataPanelContainer.setWidth("100%");
		dataPanelContainer.setHeight("100%");
		dataPanelContainer.setStyle("padding:8px 10px 14px 10px;box-sizing:border-box;overflow:auto;background:transparent;max-height:100%;");
		dataPanelContainer.setParent(eastPanel);

		Row rowUtamaData = Common.tampilanScroll1(subcenter);
		rowUtamaData.getGrid().setSclass("fgrid");

		final Vbox workflowContainer = new Vbox();
		workflowContainer.setWidth("100%");
		workflowContainer.setStyle("padding:10px 16px 18px 16px;box-sizing:border-box;overflow-x:auto;max-width:100%;");
		workflowContainer.setParent(rowUtamaData);

		Html descWorkflow = new Html("<div style='padding:8px 12px;margin-bottom:10px;border-radius:10px;"
				+ "background:#f0f9ff;border:1px solid #bae6fd;font-size:11px;color:#0369a1;line-height:1.5;'>"
				+ "📌 <strong>Riwayat Langkah</strong> — Setiap kartu di bawah mewakili satu tahap proses yang sudah dilalui. "
				+ "Gulir ke bawah untuk melihat tahap yang masih menunggu atau tombol tindak lanjut."
				+ "</div>");
		descWorkflow.setParent(workflowContainer);

		MyGrid grids = new MyGrid();
		grids.setWidth("100%");
		grids.setMold("paging");
		grids.setParent(workflowContainer);
		grids.setSclass("fgrid sop-riwayat-grid");
		grids.setStyle("border:0;background:transparent;");

		Columns columns = new Columns();
		columns.setParent(grids);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grids);

		Tbmuser tbmuserCurrent = Common.getCurrentUser();

		Session session = null;
		List<DisposisiAlurSop> disposisiAlurSops = new ArrayList<DisposisiAlurSop>();
		Map<Long, Grid> map = new HashMap<Long, Grid>();
		GeneralValueObject generalValueObject = null;
		boolean formSudahDitampilkan = false;

		try {
			session = HibernateUtil.currentSession();

			DisposisiSop attachedDisposisiSop = (DisposisiSop) session.get(DisposisiSop.class, disposisiSop.getId());

			disposisiAlurSops = session.createCriteria(DisposisiAlurSop.class).add(Restrictions.isNotNull("alurSop"))
					.add(Restrictions.eq("disposisiSop", attachedDisposisiSop)).addOrder(Order.asc("id")).list();

			if (batalkan != null) {
				int jmlSetujui = 0;
				for (DisposisiAlurSop disposisiAlurSop : disposisiAlurSops) {
					if (disposisiAlurSop.getDiajukanOleh() != null || disposisiAlurSop.getMahasiswa() != null
							|| disposisiAlurSop.getSiswa() != null) {
						jmlSetujui++;
					}
				}
				batalkan.setDisabled(jmlSetujui > 1);
			}

			boolean adaForm = false;
			int indexDisposisi = 0;
			int jumlahDisposisi = 0;
			DisposisiAlurSop disposisiAlurSopTerakhir = null;
			DisposisiAlurSop disposisiAlurSopSetujui = null;

			for (DisposisiAlurSop disposisiAlurSop : disposisiAlurSops) {
				disposisiAlurSopTerakhir = disposisiAlurSop;

				if (disposisiAlurSop != null && disposisiAlurSop.getAlurSop() != null && disposisiAlurSop.setujui()) {
					disposisiAlurSopSetujui = disposisiAlurSop;
				}

				if (disposisiAlurSopSetujui == null) {
					if (disposisiAlurSop != null && disposisiAlurSop.getAlurSop() != null
							&& disposisiAlurSop.getAlurSop().getJikaProsesDisetujuiMakaSelesai()) {
						disposisiAlurSopSetujui = disposisiAlurSop;
					}
				}

				try {
					if (hasText(disposisiAlurSop.getAlurSop().getFormInputan())) {
						adaForm = true;
					}

					if (disposisiAlurSop.getDiajukanOleh() != null || disposisiAlurSop.getMahasiswa() != null
							|| disposisiAlurSop.getSiswa() != null) {
						jumlahDisposisi++;
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

			DisposisiAlurSopAction.checkAndSave(session, disposisiAlurSopTerakhir, disposisiAlurSopSetujui,
					attachedDisposisiSop);
			eastPanel.setVisible(adaForm || !disposisiAlurSops.isEmpty());
			renderWorkflowOverviewPanel(dataPanelContainer, attachedDisposisiSop, disposisiAlurSops, jumlahDisposisi,
					adaForm, tampilkanAlurMenunggu, subcenter, eastPanel, batalkan, dataSearchDefault);

			// KARTU RINGKASAN PROGRES ALUR
			try {
				renderWorkflowProgressCard(dataPanelContainer, attachedDisposisiSop, disposisiAlurSops,
						jumlahDisposisi);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			int nomor = 0;
			for (final DisposisiAlurSop disposisiAlurSop : disposisiAlurSops) {

				if (disposisiAlurSop.getDiajukanOleh() != null || disposisiAlurSop.getMahasiswa() != null
						|| disposisiAlurSop.getSiswa() != null) {

					nomor++;
					indexDisposisi++;

					Row rowDataSub = new Row();
					rowDataSub.setValign("top");
					rowDataSub.setParent(rows);

					Groupbox groupbox = new Groupbox();
					groupbox.setWidth("98%");
					groupbox.setParent(rowDataSub);
					groupbox.setStyle("border:1px solid #d7e3f5;border-radius:14px;margin:10px 4px 14px 4px;"
							+ "background:#ffffff;box-shadow:0 7px 18px rgba(15,23,42,0.08);overflow:hidden;");

					String ds = disposisiAlurSop.getWaktu() == null ? ""
							: (SmartDateTimeUtil.getDayString(disposisiAlurSop.getWaktu(), null)
									+ Common.dateFormat5.get().format(disposisiAlurSop.getWaktu()));

					Caption captionAlur = createWorkflowCaption(nomor, disposisiAlurSop, jumlahDisposisi);
					groupbox.appendChild(captionAlur);

					Row rowUtamaLagi = Common.tampilanScroll1(groupbox);
					try {
						rowUtamaLagi.getGrid().setStyle("border:0;background:transparent;");
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
					Row rowUtamaLagi1 = new Row();
					rowUtamaLagi1.setParent(rowUtamaLagi.getParent());

					Vbox vbox111 = new Vbox();
					vbox111.setParent(rowUtamaLagi1);
					vbox111.setWidth("100%");
					vbox111.setStyle("padding:8px 10px;background:#f8fafc;border-radius:10px;margin:4px 0;");

					try {
						JSONObject jsonObject = new JSONObject(disposisiAlurSop.getDisposisiSop().getProperti());
						Iterator<String> keys = jsonObject.keys();
						if (keys.hasNext()) {
							String key = keys.next();
							jsonObject = jsonObject.getJSONObject(key);
							if (jsonObject != null && !jsonObject.isNull("kode")) {
								String kodeStr = jsonObject.get("kode") + "";
								if (!kodeStr.isEmpty()) {
									vbox111.appendChild(new MyLabelBold(kodeStr));
								}
							}
						}
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

					RevisiHelper.createNewRevisi(DisposisiSop.class, attachedDisposisiSop, "Waktu: " + ds)
							.setParent(vbox111);

					try {
						Component catatanComponent = RevisiHelper.createNewRevisi(DisposisiAlurSop.class, disposisiAlurSop,
								"Catatan: " + nullSafe(disposisiAlurSop.getKeterangan()));
						applyWrapStyle(catatanComponent);
						vbox111.appendChild(catatanComponent);
					} catch (Exception e1) {
						Label labelCatatan = new Label("Catatan: " + nullSafe(disposisiAlurSop.getKeterangan()));
						applyWrapStyle(labelCatatan);
						vbox111.appendChild(labelCatatan);
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sop/TampilanAlurSopAction.java:1369");
					}

					Hbox hbox11 = new Hbox();
					hbox11.setParent(vbox111);
					LampiranLain.createDownloadUploadFileLain(hbox11, disposisiAlurSop.getId(),
							"Lampiran Catatan Disposisi", "Lampiran Catatan Disposisi", false, new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
								}
							}, null, false, false, false, false);

					rowUtamaLagi1 = new Row();
					rowUtamaLagi1.setParent(rowUtamaLagi.getParent());

					if (disposisiAlurSop.getSetelahnya() != null
							&& disposisiAlurSop.getSetelahnya().getAlurSop() != null) {
						new MyLabelAgakKecilBoldBiru(
								"Opsi : " + disposisiAlurSop.getSetelahnya().getAlurSop().getOpsi())
								.setParent(rowUtamaLagi1);
					} else {
						new MyLabelAgakKecilBoldBiru("Opsi : Tidak ada opsi").setParent(rowUtamaLagi1);
					}

					try {
						rowUtamaLagi1 = new Row();
						rowUtamaLagi1.setParent(rowUtamaLagi.getParent());

						Grid dokumenGrid1 = new Grid();
						dokumenGrid1.setMold("paging");
						dokumenGrid1.setSclass("dgrid");
						dokumenGrid1.setParent(rowUtamaLagi1);

						Rows rowsdokumen = new Rows();
						rowsdokumen.setParent(dokumenGrid1);

						ArrayList<Row> parameterRows = new ArrayList<Row>();
						HashMap<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();
						ParameterTambahanDisposisiAlurSopListener parameterTambahanListener = new ParameterTambahanDisposisiAlurSopListener(
								disposisiAlurSop, parameterRows, lampiranLains, rowsdokumen, true);

						parameterTambahanListener.onEvent(null);
						rowUtamaLagi1.setVisible(parameterTambahanListener.getTampil());
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}

					rowUtamaLagi1 = new Row();
					rowUtamaLagi1.setParent(rowUtamaLagi.getParent());

					Grid dokumenGrid1 = new Grid();
					dokumenGrid1.setMold("paging");
					dokumenGrid1.setSclass("dgrid");
					dokumenGrid1.setParent(rowUtamaLagi1);

					Rows rowsdokumenD = new Rows();
					rowsdokumenD.setParent(dokumenGrid1);

					Vbox vbox = new Vbox();
					vbox.setParent(rowUtamaLagi);
					vbox.setWidth("100%");
					vbox.setStyle("padding:10px 12px;background:#ffffff;border-left:4px solid #60a5fa;box-sizing:border-box;");

					Tbmuser tbmuser = disposisiAlurSop.getDiajukanOleh();
					if (tbmuser == null && disposisiAlurSop.getMahasiswa() != null) {
						tbmuser = new Tbmuser(disposisiAlurSop.getMahasiswa());
					} else if (tbmuser == null && disposisiAlurSop.getSiswa() != null) {
						tbmuser = new Tbmuser(disposisiAlurSop.getSiswa());
					}
					try {
						CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox);
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

					vbox.appendChild(new Html(buildActorHtml(tbmuser, disposisiAlurSop)));

					/* Beberapa callee (listener parameter tambahan, media, dll)
					 * dapat menutup session thread-local di tengah loop;
					 * pulihkan agar tidak "Session is closed". */
					if (session == null || !session.isOpen()) {
						session = HibernateUtil.currentNativeSession();
					}

					// FlushMode MANUAL: lookup ini tidak boleh memicu auto-flush yang menulis entitas
					// "kotor" (akibat getter ber-efek samping) di tengah render → penyebab deadlock/
					// timeout/aborted-transaction pada alur SOP. Penulisan yang disengaja tetap commit.
					AlurSop alurSop = (AlurSop) session.createCriteria(AlurSop.class)
							.add(Restrictions.idEq(disposisiAlurSop.getAlurSop().getId()))
							.setFlushMode(org.hibernate.FlushMode.MANUAL).uniqueResult();

					final Set<DokumenAlurSop> dokumenAlurSops = alurSop.getDokumenAlurSops();
					final Set<KelompokParameterTambahanAlurSop> kelompokParameterTambahanAlurSops = alurSop
							.getKelompokParameterTambahanAlurSops();

					if (!dokumenAlurSops.isEmpty()) {
						for (DokumenAlurSop dokumenAlurSop : dokumenAlurSops) {
							if (dokumenAlurSop.getAktif()) {
								Row rowdokumen = new Row();
								rowdokumen.setValign("top");
								rowdokumen.setParent(rowsdokumenD);

								final LampiranLain lampiranLain;
								if (disposisiAlurSop.getAlurSop().getStart()) {
									lampiranLain = LampiranLain.ambil(attachedDisposisiSop.getId(),
											DokumenAlurSop.class.getName() + "_" + dokumenAlurSop.getId());
								} else {
									// ref harus disposisiAlurSop.getId() (ID step), bukan disposisiSop.getId() (ID workflow),
									// agar konsisten dengan cara upload di DisposisiAlurSopAction (ref = disposisiAlurSop.getId())
									lampiranLain = LampiranLain.ambil(disposisiAlurSop.getId(),
											DokumenAlurSop.class.getName() + "_alur_" + dokumenAlurSop.getId());
								}

								if (lampiranLain != null) {
									A aa = new A(dokumenAlurSop.getNama() + (" -> " + lampiranLain.getNama()));
									aa.setParent(rowdokumen);
									aa.setWidth("95%");
									aa.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.display(lampiranLain);
										}
									});
								} else {
									rowdokumen.appendChild(new Label(dokumenAlurSop.getNama()));
								}
							}
						}
					}

					if (!kelompokParameterTambahanAlurSops.isEmpty()) {
						Grid parameterGrid = new Grid();
						parameterGrid.setMold("paging");
						parameterGrid.setSclass("dgrid");
						map.put(Common.randLong(), parameterGrid);

						Columns columnsparameter = new Columns();
						columnsparameter.setParent(parameterGrid);

						MyColumnConfig columnparameter = new MyColumnConfig("");
						columnparameter.setParent(columnsparameter);
						columnparameter.setWidth("30%");

						columnparameter = new MyColumnConfig("");
						columnparameter.setParent(columnsparameter);

						Rows rowsParameter = new Rows();
						rowsParameter.setParent(parameterGrid);

						for (KelompokParameterTambahanAlurSop kelompokParameterTambahanAlurSop : kelompokParameterTambahanAlurSops) {
							Row rowParameterTambahan = new Row();
							rowParameterTambahan.setVisible(false);
							rowParameterTambahan.setParent(rowsParameter);
							ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
							rowParameterTambahan
									.appendChild(new MyLabelStyled(kelompokParameterTambahanAlurSop.getNama() + ""));

							List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
									session.createCriteria(ParameterTambahanAlurSop.class)
											.add(Restrictions.eq("kelompokParameterTambahanAlurSop",
													kelompokParameterTambahanAlurSop))
											.createAlias("parameterTambahan", "parameterTambahan")
											.createAlias("kelompokParameterTambahanAlurSop",
													"kelompokParameterTambahanAlurSop")
											.add(Restrictions.eq("parameterTambahan.aktif", true))
											.add(Restrictions.eq("kelompokParameterTambahanAlurSop.aktif", true))
											.setProjection(Projections.groupProperty("parameterTambahan.id")),
									ParameterTambahan.class, false);
							Collections.sort(parameterTambahans);

							rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
							if (!parameterTambahans.isEmpty()) {
								for (final ParameterTambahan parameterTambahan : parameterTambahans) {
									String jenis = kelompokParameterTambahanAlurSop.getId() + "->"
											+ parameterTambahan.getId();
									Row rowParameter = new Row();
									rowParameter.setParent(rowsParameter);
									rowParameter.appendChild(new Label(parameterTambahan.getLabelInputan()
											+ (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));

									String val = "";
									String[] spl = disposisiAlurSop.getParameterTambahanInds().split("\n");
									for (String d : spl) {
										String[] value = d.split("<=>");
										if (value[0].trim().equalsIgnoreCase(jenis)) {
											val = value.length > 1 ? value[1].trim() : "";
										}
									}

									String[] ss = val.split("->");
									if (ss.length > 1) {
										val = ss[1];
									}

									if (parameterTambahan.getHarusMenyertakanLampiran()) {
										Vbox vbox11 = new Vbox();
										rowParameter.appendChild(vbox11);
										vbox11.appendChild(new Label(val));

										Hbox hbox = new Hbox();
										hbox.setWidth("100%");

										LampiranLain.createDownloadUploadFileLain(hbox,
												disposisiAlurSop.getId() == null ? Common.refSementara()
														: disposisiAlurSop.getId(),
												jenis,
												parameterTambahan.getLabelInputan()
														+ (parameterTambahan.getLampiranWajibDiisi() ? " (*)" : " "),
												false, new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
													}
												}, null, false, false, false, false, null);
										hbox.setParent(vbox11);

									} else {
										rowParameter.appendChild(new Label(val));
									}
								}
							}
						}
					}

					if (!formSudahDitampilkan && hasText(alurSop.getFormInputan())) {
						generalValueObject = null;
						try {
							FormSop formSop = createFormSop(alurSop.getFormInputan());
							if (formSop != null) {
								generalValueObject = resolveFormData(session, attachedDisposisiSop, formSop);
								if (generalValueObject == null) {
									generalValueObject = createEmptyFormData(formSop);
								}

								MyGrid rowLampiran = formSop.form(generalValueObject, attachedDisposisiSop,
										new MyToolbarbuttonConfig(), null);
								if (rowLampiran != null) {
									map.put(createMapKey(generalValueObject), rowLampiran);
									formSudahDitampilkan = true;
								}
							}
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}

					// =================================================================================
					// PERBAIKAN BUG OFF-BY-ONE:
					// Karena indexDisposisi dimulai dari 1 (increment sebelum pengecekan),
					// maka data terakhir adalah saat indexDisposisi SAMA DENGAN jumlahDisposisi
					// =================================================================================
					final boolean bolehEdit = indexDisposisi == jumlahDisposisi;

					boolean sama = false;
					// final: diakses dari inner class (listener onClick) di bawah → wajib final (Java 1.6/1.7).
					final Boolean isAdmin = Common.getApakahAdmin();
					if (isAdmin != null && isAdmin.booleanValue()) {
						sama = true;
					} else if (tbmuserCurrent != null && tbmuser != null
							&& ((tbmuserCurrent.getUserId() != null && tbmuser.getUserId() != null
									&& tbmuserCurrent.getUserId().equals(tbmuser.getUserId()))
									|| (tbmuserCurrent.getMahasiswa() != null && tbmuser.getMahasiswa() != null
											&& tbmuserCurrent.getMahasiswa().getId()
													.equals(tbmuser.getMahasiswa().getId()))
									|| (tbmuserCurrent.getSiswa() != null && tbmuser.getSiswa() != null
											&& tbmuserCurrent.getSiswa().getId().equals(tbmuser.getSiswa().getId())))) {
						sama = true;
					}

					// Admin (Common.getApakahAdmin) boleh edit SEMUA langkah: pengaju/disposisi
					// paling awal, revisi di tengah, maupun langkah terakhir. Non-admin tetap
					// hanya boleh pada langkah terakhir miliknya (bolehEdit && sama).
					boolean finalVisible = (bolehEdit && sama) || Boolean.TRUE.equals(isAdmin);

					// kebab popup (⋯) via UIHelper.buatBarisAksi — tombol aksi langkah alur
					// disusun ringkas. Induknya tetap 'vbox' (sel yang sama) karena sel ini juga
					// memuat foto aktor + Html info, sehingga tidak menambah kolom baru pada baris.
					final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
							new java.util.ArrayList<org.zkoss.zk.ui.Component>();
					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");

					// Visibilitas tombol 'Ubah' bergantung pada variabel finalVisible dari hasil
					// debug
					button.setVisible(finalVisible);
					button.setTooltiptext("Ubah Data");

					final DisposisiSop attachedSopForListener = attachedDisposisiSop;

					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							if (disposisiAlurSop.getAlurSop().getStart()) {
								DisposisiSop disposisi = disposisiAlurSop.getDisposisiSop();
								DisposisiSopAction.onAddExternal(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												tampil(subcenter, eastPanel, attachedSopForListener, batalkan,
														dataSearchDefault);
												if (dataSearchDefault != null) {
													dataSearchDefault.onEvent(null);
												}
											}
										});
									}
								}, disposisi, true);
							} else {
								DisposisiAlurSopAction.onAddExternal(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												tampil(subcenter, eastPanel, attachedSopForListener, batalkan,
														dataSearchDefault);
												if (dataSearchDefault != null) {
													dataSearchDefault.onEvent(null);
												}
											}
										});
									}
								}, disposisiAlurSop, disposisiAlurSop.getAlurSop(), attachedSopForListener,
										dokumenAlurSops, disposisiAlurSop.getSetelahnya() == null, true);
							}
						}
					});
					aksiButtons.add(button);

					MyToolbarbuttonConfig buttonHapus = new MyToolbarbuttonConfig("Batal", "/img/svg/trash.svg");
					buttonHapus.setTooltiptext("Hapus Data");

					// Tombol 'Batal' (HAPUS data) sengaja TETAP dibatasi langkah terakhir milik
					// sendiri/aktor (bolehEdit && sama) — TIDAK dibuka ke semua langkah utk Admin,
					// agar Admin tak sengaja menghapus langkah tengah dan merusak rantai alur.
					// Admin tetap bisa EDIT (tombol Ubah) di semua langkah.
					buttonHapus.setVisible(bolehEdit && sama);

					buttonHapus.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												Session s = null;
												Transaction tx = null;
												try {
													s = HibernateUtil.currentSession();
													tx = s.beginTransaction();

													if (disposisiAlurSop.getAlurSop().getStart()) {
														String sql = "delete from disposisi_alur_sop where disposisi_sop="
																+ attachedSopForListener.getId();
														s.createSQLQuery(sql).executeUpdate();
														tx.commit();

														Common.createDefaultTimer(new EventListener() {
															@Override
															public void onEvent(Event arg0) throws Exception {
																Session s2 = null;
																Transaction tx2 = null;
																try {
																	s2 = HibernateUtil.currentSession();
																	tx2 = s2.beginTransaction();
																	String sql2 = "delete from disposisi_sop where id="
																			+ attachedSopForListener.getId();
																	s2.createSQLQuery(sql2).executeUpdate();
																	tx2.commit();
																} catch (Exception ex) {
																	if (tx2 != null)
																		tx2.rollback();
																} finally {
																	cleanupSession(s2);
																}

																if (subcenter.getParent()
																		.getParent() instanceof Tabpanel) {
																	Tabpanel tabpanel = (Tabpanel) subcenter.getParent()
																			.getParent();
																	Tab tab = tabpanel.getLinkedTab();
																	tabpanel.detach();
																	tab.detach();
																} else {
																	subcenter.getParent().getParent().detach();
																}
															}
														});

													} else {
														if (attachedSopForListener.getDisposisiEnd() != null
																&& attachedSopForListener.getDisposisiEnd().getId()
																		.equals(disposisiAlurSop.getId())) {
															attachedSopForListener.setDisposisiEnd(null);
														}
														if (attachedSopForListener.getDisposisiSetuju() != null
																&& attachedSopForListener.getDisposisiSetuju().getId()
																		.equals(disposisiAlurSop.getId())) {
															attachedSopForListener.setDisposisiSetuju(null);
														}
														if (attachedSopForListener.getDisposisiStart() != null
																&& attachedSopForListener.getDisposisiStart().getId()
																		.equals(disposisiAlurSop.getId())) {
															attachedSopForListener.setDisposisiStart(null);
														}

														// Selain langkah yang sedang dihapus, referensi disposisi bisa menunjuk
														// baris disposisi_alur_sop yang SUDAH dihapus sebelumnya (dangling FK)
														// sehingga update melanggar FK ("Key (disposisi_end)=(..) is not
														// present"). Null-kan bila baris yang direferensikan tak ada lagi di DB.
														if (attachedSopForListener.getDisposisiEnd() != null
																&& s.get(DisposisiAlurSop.class, attachedSopForListener
																		.getDisposisiEnd().getId()) == null) {
															attachedSopForListener.setDisposisiEnd(null);
														}
														if (attachedSopForListener.getDisposisiSetuju() != null
																&& s.get(DisposisiAlurSop.class, attachedSopForListener
																		.getDisposisiSetuju().getId()) == null) {
															attachedSopForListener.setDisposisiSetuju(null);
														}
														if (attachedSopForListener.getDisposisiStart() != null
																&& s.get(DisposisiAlurSop.class, attachedSopForListener
																		.getDisposisiStart().getId()) == null) {
															attachedSopForListener.setDisposisiStart(null);
														}

														s.update(attachedSopForListener);
														// Hapus instance TERKELOLA: proxy #id (via getDisposisi* / lazy-load) bisa terdaftar di
														// session sedangkan disposisiAlurSop mungkin DETACHED -> s.delete(detached) memicu
														// NonUniqueObjectException "a different object with the same identifier". Ambil via
														// s.get lalu hapus instance itu (null-safe bila baris sudah tidak ada di DB).
														DisposisiAlurSop alurHapus = (DisposisiAlurSop) s.get(DisposisiAlurSop.class, disposisiAlurSop.getId());
														if (alurHapus != null) {
															s.delete(alurHapus);
														}
														tx.commit();

														Common.createDefaultTimer(new EventListener() {
															@Override
															public void onEvent(Event arg0) throws Exception {
																tampil(subcenter, eastPanel, attachedSopForListener,
																		batalkan, dataSearchDefault);
																if (dataSearchDefault != null) {
																	dataSearchDefault.onEvent(null);
																}
															}
														});
													}
												} catch (Exception e) {
													if (tx != null)
														tx.rollback();
													Common.tampilErrorJikaAdmin(e);
													MyMessageboxConfig.show(
															"Data ini tidak dapat dihapus karena digunakan untuk transaksi");
												} finally {
													cleanupSession(s);
												}
											}
										}
									});
						}
					});
					aksiButtons.add(buttonHapus);

					// Susun semua tombol: max 3 per baris, rata tengah
					ais.ui.util.UIHelper.buatBarisAksi(vbox, 3, aksiButtons);
				}
			}

			Row rowUtamalagiData = new Row();
			rowUtamalagiData.setParent(rowUtamaData.getParent());

			DisposisiAlurSop terakhir = null;
			for (DisposisiAlurSop disposisiAlurSop : disposisiAlurSops) {
				terakhir = disposisiAlurSop;
			}

			if (terakhir != null && (terakhir.getSelesai() || (terakhir.getSetelahnya() == null
					&& terakhir.getAlurSop().getSetelahnya() == null && terakhir.getAlurSop().getSetelahnya2() == null
					&& terakhir.getAlurSop().getSetelahnya3() == null && terakhir.getAlurSop().getSetelahnya4() == null
					&& terakhir.getAlurSop().getSetelahnya5() == null && terakhir.getAlurSop().getSetelahnya6() == null
					&& terakhir.getAlurSop().getSetelahnya7() == null && terakhir.getAlurSop().getSetelahnya8() == null
					&& terakhir.getAlurSop().getSetelahnya9() == null
					&& terakhir.getAlurSop().getSetelahnya10() == null)) && terakhir.getWaktu() != null) {

				String oleh = "";
				if (terakhir.getDiajukanOleh() != null) {
					oleh = terakhir.getDiajukanOleh().getUserNama();
				} else if (terakhir.getMahasiswa() != null) {
					oleh = terakhir.getMahasiswa().getNama();
				} else if (terakhir.getSiswa() != null) {
					oleh = terakhir.getSiswa().getNama();
				}

				Component selesaiInfo = new ais.ui.util.MyLabelAgakKecilBoldMerah("Proses \""
						+ attachedDisposisiSop.getSop().getNama() + "\" oleh \"" + oleh + "\" telah selesai pada waktu "
						+ Common.dateFormat61.get().format(terakhir.getWaktu()) + " di alur \""
						+ terakhir.getAlurSop().getNama() + "\" dengan catatan \"" + nullSafe(terakhir.getKeterangan()) + "\"");
				applyWrapStyle(selesaiInfo);
				rowUtamalagiData.appendChild(selesaiInfo);
			}

			else if (!disposisiAlurSops.isEmpty()) {

				boolean ada = false;
				String infototal = "";
				for (final DisposisiAlurSop disposisiAlurSop : disposisiAlurSops) {

					if ((disposisiAlurSop.getDiajukanOleh() == null && disposisiAlurSop.getMahasiswa() == null
							&& disposisiAlurSop.getSiswa() == null)) {
						ada = true;
					}

					if (disposisiAlurSop.getAlurSop() != null && disposisiAlurSop.getAlurSop().getPenolakanAdaDiSini()
							&& disposisiAlurSop.getSebelumnya() != null
							&& disposisiAlurSop.getSebelumnya().getAlurSop() != null) {

						infototal = "Ditolak : " + disposisiAlurSop.getSebelumnya().getAlurSop().getAktor() + " - "
								+ disposisiAlurSop.getSebelumnya().getAlurSop().getNama();

						if (disposisiAlurSop.getSebelumnya().getDiajukanOleh() != null) {
							infototal += " " + disposisiAlurSop.getSebelumnya().getDiajukanOleh().getUserNama();
						} else if (disposisiAlurSop.getSebelumnya().getMahasiswa() != null) {
							infototal += " " + disposisiAlurSop.getSebelumnya().getMahasiswa().getNama();
						} else if (disposisiAlurSop.getSebelumnya().getSiswa() != null) {
							infototal += " " + disposisiAlurSop.getSebelumnya().getSiswa().getNama();
						}
					}
				}

				// Jika semua langkah yang ada sudah diproses (ada=false) namun desain alur
				// masih punya tahap berikutnya yang belum dibuat sebagai record DisposisiAlurSop,
				// tampilkan "Sedang menunggu" berdasarkan desain AlurSop agar semua pengguna
				// (termasuk pengaju yang bukan aktor berikutnya) bisa melihat alur yang pending.
				AlurSop alurBerikutnyaDariDesain = null;
				if (!ada && infototal.trim().isEmpty() && terakhir != null && terakhir.getAlurSop() != null) {
					try {
						// BUG "aktor selanjutnya tampil sebelum aktor sekarang bertindak":
						// 'terakhir' (record id tertinggi) bisa jadi langkah yang MASIH menunggu diproses
						// (mis. revisi HRD → "Tim Divisi Usaha"). Tapi getDiajukanOleh()/getWaktu() adalah
						// getter TERKOMPUTASI yang bisa ter-mask non-null (kembaliKePengaju/start) sehingga
						// loop deteksi menganggap langkah itu SUDAH diproses (ada=false), lalu panel melompat
						// satu langkah ke aktor BERIKUTNYA (mis. "Koord Usaha"). Untuk mencegahnya, tentukan
						// status 'terakhir' dari KOLOM MENTAH (bukan getter). DEFAULT AMAN: anggap masih
						// menunggu (jangan lompat) — hanya maju ke 'setelahnya' bila kolom mentah terisi
						// (benar-benar sudah diproses). FlushMode.MANUAL agar tak mem-flush field ter-mutasi.
						boolean terakhirMasihPending = true;
						try {
							Object[] mentah = (Object[]) session.createCriteria(DisposisiAlurSop.class)
									.add(Restrictions.idEq(terakhir.getId()))
									.setProjection(Projections.projectionList()
											.add(Projections.property("diajukanOleh"))
											.add(Projections.property("mahasiswa"))
											.add(Projections.property("siswa")))
									.setFlushMode(org.hibernate.FlushMode.MANUAL).uniqueResult();
							if (mentah != null && (mentah[0] != null || mentah[1] != null || mentah[2] != null)) {
								terakhirMasihPending = false; // sudah diproses aktornya → boleh tampilkan tahap berikutnya
							}
						} catch (Exception exMentah) {
							ais.common.ErrorAuditUtil.record(exMentah,
									"auto-audit src/ais/action/master/sop/TampilanAlurSopAction.java:alurBerikutnya-rawcheck");
						}

						if (terakhirMasihPending) {
							// Aktor yang menunggu = LANGKAH 'terakhir' itu SENDIRI (frontier, mis. Tim Divisi
							// Usaha saat revisi), BUKAN langkah setelahnya (Koord Usaha).
							alurBerikutnyaDariDesain = terakhir.getAlurSop();
						}
						// else: 'terakhir' SUDAH diproses aktornya (kolom mentah diajukanOleh/mahasiswa/siswa
						// terisi) TAPI record tahap berikutnya BELUM dibuat. JANGAN mensintesis aktor tahap
						// berikutnya dari desain (mis. "Persetujuan oleh Koord. Usaha"): itu memunculkan
						// pengguna yang BELUM benar-benar menerima disposisi (keluhan pengguna — aktor
						// selanjutnya tampil sebelum ada disposisi nyata ke sana). Biarkan alurBerikutnyaDariDesain
						// tetap null sehingga panel hanya menampilkan record disposisi yang NYATA ada.
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
					if (alurBerikutnyaDariDesain != null) {
						ada = true;
					}
				}

				if (!infototal.trim().isEmpty()) {
					rowUtamalagiData.appendChild(new MyLabelAgakKecilBoldMerah(infototal));
				} else if (ada) {
					Groupbox groupboxUtamaLagi = new Groupbox();
					groupboxUtamaLagi.setParent(rowUtamalagiData);
					groupboxUtamaLagi.setStyle("border:1px solid #facc15;border-radius:14px;background:#fffdf4;"
							+ "box-shadow:0 6px 16px rgba(202,138,4,0.12);margin-top:12px;");
					Caption captionMenunggu = new Caption("⏳ Sedang menunggu proses disposisi SOP (Workflow)");
					captionMenunggu.setStyle("font-weight:bold;color:#854d0e;background:#fef3c7;padding:8px 12px;"
							+ "border-radius:10px;margin:2px;");
					groupboxUtamaLagi.appendChild(captionMenunggu);

					Html descMenunggu = new Html("<div style='padding:8px 12px 4px 12px;font-size:11px;"
							+ "color:#78350f;line-height:1.5;'>"
							+ "Pengajuan ini sedang menunggu tindak lanjut dari petugas yang bertanggung jawab. "
							+ "Kolom <strong>Aktor Disposisi</strong> menunjukkan siapa yang perlu mengambil tindakan."
							+ "</div>");
					groupboxUtamaLagi.appendChild(descMenunggu);

					grids = new MyGrid();
					grids.setWidth("100%");
					grids.setMold("paging");
					grids.setParent(groupboxUtamaLagi);
					grids.setSclass("dgrid");

					columns = new Columns();
					columns.setParent(grids);

					column = new MyColumnConfig("No.");
					column.setParent(columns);
					column.setWidth("4%");

					column = new MyColumnConfig("Kode");
					column.setParent(columns);
					column.setWidth("8%");

					column = new MyColumnConfig("Alur/Workflow");
					column.setParent(columns);
					column.setWidth("30%");

					column = new MyColumnConfig("Aktor Disposisi");
					column.setParent(columns);

					rows = new Rows();
					rows.setParent(grids);

					for (final DisposisiAlurSop disposisiAlurSop : disposisiAlurSops) {

						if (disposisiAlurSop.getAktif() && (disposisiAlurSop.getDiajukanOleh() == null
								&& disposisiAlurSop.getMahasiswa() == null && disposisiAlurSop.getSiswa() == null)) {

							nomor++;
							AlurSop alurSopA = disposisiAlurSop.getAlurSop();
							final AlurSop alurSop = (AlurSop) session.createCriteria(AlurSop.class)
									.add(Restrictions.idEq(alurSopA.getId())).uniqueResult();

							Row row = new Row();
							row.setValign("top");
							row.setParent(rows);
							row.appendChild(new Label(nomor + ""));
							row.appendChild(new Label(alurSop.getKode()));

							try {
								Vbox a;
								(a = RevisiHelper.createNewRevisi(DisposisiAlurSop.class, disposisiAlurSop,
										disposisiAlurSop.getAlurSop().getNama())).setParent(row);

								if (disposisiAlurSop.getWaktuMaksimal() != null) {
									String d = disposisiAlurSop.getWaktuMaksimal() == null ? ""
											: "Batas Waktu: " + (SmartDateTimeUtil
													.getDayString(disposisiAlurSop.getWaktuMaksimal(), null)
													+ Common.dateFormat5.get().format(disposisiAlurSop.getWaktuMaksimal()));
									a.appendChild(new MyLabelAgakKecilBoldMerah(d));
								}
							} catch (Exception e1) {
								row.appendChild(new Label(alurSop.getNama()));
								e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sop/TampilanAlurSopAction.java:2002");
							}

							Hbox hbox = new Hbox();
							Vbox vbox = new Vbox();
							row.appendChild(vbox);
							vbox.appendChild(hbox);

							try {
								JSONObject jsonObject = new JSONObject(
										disposisiAlurSop.getDisposisiSop().getProperti());
								Iterator<String> keys = jsonObject.keys();
								if (keys.hasNext()) {
									String key = keys.next();
									jsonObject = jsonObject.getJSONObject(key);
									if (jsonObject != null && !jsonObject.isNull("kode")) {
										String kodeStr = jsonObject.get("kode") + "";
										if (!kodeStr.isEmpty()) {
											vbox.appendChild(new MyLabelBold(kodeStr));
										}
									}
								}
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

							ada = false;
							final Set<DokumenAlurSop> dokumenAlurSops = alurSop.getDokumenAlurSops();

							try {
								ada = SopUtil.tampilAktor(tbmuserCurrent, safeString(alurSop.getKhususUsername()),
										alurSop.getAktorSop() != null ? safeString(alurSop.getAktorSop().getJenisPengguna()) : "",
										disposisiAlurSop.getDisposisiSop(), alurSop, hbox);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
								ada = false;
							}

							boolean userLoginAdalahAktorBerikutnya = isUserLoginTermasukAktorBerikutnya(tbmuserCurrent, hbox, ada);

							if (userLoginAdalahAktorBerikutnya) {
								MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
										"Tindak Lanjuti SOP sebagai \"" + alurSop.getAktor() + "\"",
										"/img/svg/clipboard-check.svg");
								button.setTooltiptext("Tindak lanjuti proses SOP pada tahap ini");

								final DisposisiSop attachedSopForListener = attachedDisposisiSop;

								button.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										DisposisiAlurSopAction.onAddExternal(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												Common.createDefaultTimer(new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														tampil(subcenter, eastPanel, attachedSopForListener, batalkan,
																dataSearchDefault);
														if (dataSearchDefault != null) {
															dataSearchDefault.onEvent(null);
														}
													}
												});
											}
										}, disposisiAlurSop, disposisiAlurSop.getAlurSop(), attachedSopForListener,
											dokumenAlurSops, true, true);
									}
								});
								button.setParent(vbox);
								appendTombolBatalMenungguJikaAktor(vbox, disposisiAlurSop, attachedSopForListener,
										subcenter, eastPanel, batalkan, dataSearchDefault);
							} else {
								if (hbox == null || hbox.getChildren() == null || hbox.getChildren().isEmpty()) {
									vbox.appendChild(new Label(disposisiAlurSop.getAlurSop().getAktor()));
								}
							}
								// Selalu tampilkan nama Aktor (peran) + Hak Akses + UserId yang SEHARUSNYA dapat
								// disposisi, agar kolom "Aktor Disposisi" tidak kosong walau pelihat bukan aktornya.
								tampilInfoAktorDisposisi(vbox, alurSop, hbox);
}
					}

					// Jika tidak ada pending DisposisiAlurSop yang ditampilkan, cek desain AlurSop
					// untuk menampilkan tahap berikutnya yang sedang menunggu. Ini memastikan
					// pengguna non-aktor (mis. pengaju) tetap melihat info siapa yang harus bertindak.
					if (alurBerikutnyaDariDesain != null && rows != null
							&& (rows.getChildren() == null || rows.getChildren().isEmpty())) {
						try {
							Row rowDesain = new Row();
							rowDesain.setValign("top");
							rowDesain.setParent(rows);
							rowDesain.appendChild(new Label("1"));
							rowDesain.appendChild(new Label(nullSafe(alurBerikutnyaDariDesain.getKode())));

							Vbox vboxNamaDesain = new Vbox();
							vboxNamaDesain.appendChild(new Label(nullSafe(alurBerikutnyaDariDesain.getNama())));
							if (alurBerikutnyaDariDesain.getAktor() != null
									&& !alurBerikutnyaDariDesain.getAktor().trim().isEmpty()) {
								Label lAktor = new Label("Peran: " + alurBerikutnyaDariDesain.getAktor());
								lAktor.setStyle("font-size:10px;color:#64748b;");
								vboxNamaDesain.appendChild(lAktor);
							}
							rowDesain.appendChild(vboxNamaDesain);

							Hbox hboxDesain = new Hbox();
							Vbox vboxDesain = new Vbox();
							rowDesain.appendChild(vboxDesain);
							vboxDesain.appendChild(hboxDesain);

							try {
								SopUtil.tampilAktor(tbmuserCurrent,
										safeString(alurBerikutnyaDariDesain.getKhususUsername()),
										alurBerikutnyaDariDesain.getAktorSop() != null
												? safeString(alurBerikutnyaDariDesain.getAktorSop().getJenisPengguna())
												: "",
										attachedDisposisiSop, alurBerikutnyaDariDesain, hboxDesain);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}

							if (hboxDesain.getChildren() == null || hboxDesain.getChildren().isEmpty()) {
								vboxDesain.appendChild(
										new Label(nullSafe(alurBerikutnyaDariDesain.getAktor())));
							}
								tampilInfoAktorDisposisi(vboxDesain, alurBerikutnyaDariDesain, hboxDesain);
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			}

			MyGrid grids1 = new MyGrid();
			grids1.setWidth("100%");
			grids1.setMold("paging");
			grids1.setParent(dataPanelContainer);
			grids1.setSclass("fgrid");
			grids1.setPageSize(1000);
			grids1.setStyle("min-height:500px;max-width:100%;overflow:auto;border:0;background:transparent;");
			Columns columns1 = new Columns();
			columns1.setParent(grids1);

			MyColumnConfig column1 = new MyColumnConfig("");
			column1.setParent(columns1);

			Rows rows1 = new Rows();
			rows1.setParent(grids1);

			for (Grid objects : map.values()) {
				Row row = new Row();
				row.setValign("top");
				row.setParent(rows1);
				row.appendChild(objects);
				Common.freezeGanti(objects, true);
			}

			boolean adaDpc = false;
			DaftarPengajuanTransfer daftarPengajuanTransfer = null;
			StandingInstruction standingInstruction = null;
			if (generalValueObject != null && generalValueObject.getId() != null
					&& generalValueObject instanceof UangMuka) {
				adaDpc = true;
				daftarPengajuanTransfer = ((UangMuka) generalValueObject).getDaftarPengajuanTransfer();
			} else if (generalValueObject != null && generalValueObject.getId() != null
					&& generalValueObject instanceof PembayaranGaji) {
				adaDpc = true;
				standingInstruction = ((PembayaranGaji) generalValueObject).getStandingInstruction();
			} else if (generalValueObject != null && generalValueObject.getId() != null
					&& generalValueObject instanceof Pertangungjawaban) {
				adaDpc = true;
				daftarPengajuanTransfer = ((Pertangungjawaban) generalValueObject).getDaftarPengajuanTransfer();
			} else if (generalValueObject != null && generalValueObject.getId() != null
					&& generalValueObject instanceof DanaTalangan) {
				adaDpc = true;
				daftarPengajuanTransfer = ((DanaTalangan) generalValueObject).getDaftarPengajuanTransfer();
			} else if (generalValueObject != null && generalValueObject.getId() != null
					&& generalValueObject instanceof PenggantianKasKecil) {
				adaDpc = true;
				daftarPengajuanTransfer = ((PenggantianKasKecil) generalValueObject).getDaftarPengajuanTransfer();
			} else if (generalValueObject != null && generalValueObject.getId() != null
					&& generalValueObject instanceof PembayaranPengadaanMasterAssetDetail) {
				adaDpc = true;
				daftarPengajuanTransfer = ((PembayaranPengadaanMasterAssetDetail) generalValueObject)
						.getDaftarPengajuanTransfer();
			} else if (generalValueObject != null && generalValueObject.getId() != null
					&& generalValueObject instanceof PembayaranTerminMasterAssetDetail) {
				adaDpc = true;
				daftarPengajuanTransfer = ((PembayaranTerminMasterAssetDetail) generalValueObject)
						.getDaftarPengajuanTransfer();
			} else if (generalValueObject != null && generalValueObject.getId() != null
					&& generalValueObject instanceof PembayaranDpMasterAssetDetail) {
				adaDpc = true;
				daftarPengajuanTransfer = ((PembayaranDpMasterAssetDetail) generalValueObject)
						.getDaftarPengajuanTransfer();
			} else if (generalValueObject != null && generalValueObject.getId() != null
					&& generalValueObject instanceof Tagihan) {
				adaDpc = true;
				daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
						.createCriteria(DaftarPengajuanTransfer.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.isNotNull("prosesTransfer")).addOrder(Order.desc("id"))
						.add(Restrictions.eq("diskonTagihan", generalValueObject)).setMaxResults(1).uniqueResult();
			}

			if (adaDpc) {
				Row row = new Row();
				row.setValign("top");
				row.setParent(rows1);
				Vbox vbox1 = new Vbox();
				row.appendChild(vbox1);

				if (standingInstruction != null) {
					JSONObject jsonObjectTransfer = new JSONObject(standingInstruction.getTransferVia());
					Iterator<String> iterator = jsonObjectTransfer.keys();
					String transfer = "";
					while (iterator.hasNext()) {
						String d = iterator.next();
						Long idBank = Long.parseLong(d);
						Bank bank = (Bank) ConstantValues.ambil(Bank.class.getName(), idBank);

						JSONObject jsonObjectData = null;
						try {
							jsonObjectData = jsonObjectTransfer.isNull(idBank.toString()) ? null
									: jsonObjectTransfer.getJSONObject(idBank.toString());
						} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

						Long idS = jsonObjectData == null || jsonObjectData.isNull("si")
								|| jsonObjectData.get("si").toString().trim().isEmpty() ? null
										: Long.parseLong(jsonObjectData.get("si").toString().trim());

						if (idS != null) {
							ProsesTransferStandingInstruction prosesTransferStandingInstruction = (ProsesTransferStandingInstruction) session
									.createCriteria(ProsesTransferStandingInstruction.class).add(Restrictions.idEq(idS))
									.uniqueResult();
							if (prosesTransferStandingInstruction != null) {
								Double nilai = jsonObjectData.isNull("nilai") ? 0.0 : jsonObjectData.getDouble("nilai");
								String t = (bank == null ? "Tidak ada bank" : "Transfer ke " + bank.getNama()) + " "
										+ Common.numberFormat.get().format(nilai);
								transfer += transfer.isEmpty() ? t : ", " + t;
							}
						}
					}
					new Label(transfer).setParent(vbox1);
				} else {
					DaftarPengajuanTransfer.tampilStatus(daftarPengajuanTransfer, vbox1);
				}
			}

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (disposisiAlurSops != null)
				disposisiAlurSops.clear();
			if (map != null)
				map.clear();
			cleanupSession(session);
		}
	}


	private static String nullSafe(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static String appendStyle(String oldStyle, String extraStyle) {
		if (oldStyle == null) {
			oldStyle = "";
		}
		if (extraStyle == null || extraStyle.trim().length() == 0) {
			return oldStyle;
		}
		String s = oldStyle.trim();
		if (s.length() > 0 && !s.endsWith(";")) {
			s += ";";
		}
		return s + extraStyle;
	}

	private static void applyWrapStyle(Component component) {
		if (component == null) {
			return;
		}
		try {
			if (component instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
				org.zkoss.zk.ui.HtmlBasedComponent htmlBasedComponent = (org.zkoss.zk.ui.HtmlBasedComponent) component;
				htmlBasedComponent.setStyle(appendStyle(htmlBasedComponent.getStyle(),
						"max-width:100%;white-space:normal;word-break:break-word;overflow-wrap:break-word;line-height:1.45;"));
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (component instanceof Label) {
				((Label) component).setMultiline(true);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			List children = component.getChildren();
			if (children != null) {
				for (Iterator it = children.iterator(); it.hasNext();) {
					Object child = it.next();
					if (child instanceof Component) {
						applyWrapStyle((Component) child);
					}
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menampilkan info aktor disposisi di kolom "Aktor Disposisi": nama Aktor (peran),
	 * Hak Akses (role/jenis pengguna yang berhak), dan UserId pengguna yang SEHARUSNYA dapat
	 * melakukan disposisi. UserId diambil dari hasil resolusi SopUtil.tampilAktor (atribut
	 * "usernamePengguna" pada hbox). Bila belum ada user aktif dengan hak akses itu, diberi
	 * keterangan agar jelas penyebab kolom kosong.
	 */
	private static void tampilInfoAktorDisposisi(org.zkoss.zk.ui.Component parent, AlurSop alurSop,
		org.zkoss.zk.ui.Component hbox) {
		if (parent == null || alurSop == null) {
			return;
		}
		try {
			String aktor = alurSop.getAktor();
			if (hasText(aktor)) {
				Label lAktor = new Label("Aktor: " + aktor.trim());
				lAktor.setStyle("font-size:11px;font-weight:bold;color:#334155;");
				lAktor.setParent(parent);
			}

			String hakAkses = alurSop.getAktorSop() != null ? nullSafe(alurSop.getAktorSop().getJenisPengguna()) : "";
			String khusus = nullSafe(alurSop.getKhususUsername());
			StringBuilder ha = new StringBuilder();
			if (hasText(hakAkses)) {
				ha.append(hakAkses.trim());
			}
			if (hasText(khusus)) {
				if (ha.length() > 0) {
					ha.append("; ");
				}
				ha.append("khusus user: ").append(khusus.trim());
			}
			if (ha.length() > 0) {
				Label lHak = new Label("Hak Akses: " + ha.toString());
				lHak.setStyle("font-size:10px;color:#2563eb;");
				lHak.setParent(parent);
			}

			String userIds = null;
			try {
				Object attr = hbox == null ? null : hbox.getAttribute("usernamePengguna");
				userIds = attr == null ? null : attr.toString();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/TampilanAlurSopAction.java:2360");
			}
			Label lUser = new Label(hasText(userIds) ? ("UserId: " + userIds)
				: "UserId: (belum ada user aktif dengan hak akses ini)");
			lUser.setStyle("font-size:10px;color:" + (hasText(userIds) ? "#16a34a" : "#dc2626") + ";");
			lUser.setParent(parent);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	private static Caption createWorkflowCaption(int nomor, DisposisiAlurSop disposisiAlurSop, int jumlahDisposisi) {
		String namaAlur = "Alur";
		String aktor = "";
		String status = "Selesai";
		try {
			if (disposisiAlurSop != null && disposisiAlurSop.getAlurSop() != null) {
				namaAlur = disposisiAlurSop.getAlurSop().getNama();
				aktor = disposisiAlurSop.getAlurSop().getAktor();
			}
			if (disposisiAlurSop != null && disposisiAlurSop.getWaktu() == null) {
				status = "Proses";
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Caption caption = new Caption();
		caption.setStyle("border:1px solid #d6dce5;border-left:5px solid #a38b18;background:#f8fafc;"
				+ "color:#1f2937;font-weight:bold;padding:8px 11px;border-radius:9px;margin:2px;"
				+ "box-shadow:0 1px 3px rgba(15,23,42,0.06);");
		StringBuilder sb = new StringBuilder();
		// Kolom tunggal — hindari justify-content:space-between agar tidak overflow ke luar Caption
		sb.append("<div style='display:flex;flex-direction:column;gap:4px;'>");
		// Baris 1: nomor + nama alur + aktor
		sb.append("<div style='display:flex;align-items:center;gap:8px;flex-wrap:wrap;'>");
		sb.append("<span style='display:inline-block;width:26px;height:26px;line-height:26px;text-align:center;"
				+ "background:#ffffff;color:#6b5b0b;border:1px solid #d4c36a;border-radius:50%;font-weight:bold;flex-shrink:0;'>")
				.append(nomor).append("</span>");
		sb.append("<span style='font-size:13px;'>").append(html(namaAlur)).append("</span>");
		if (aktor != null && aktor.trim().length() > 0) {
			sb.append("<span style='font-size:10px;font-weight:normal;background:#fff7d6;color:#6b5b0b;"
					+ "border:1px solid #eadb85;border-radius:999px;padding:3px 8px;'>").append(html(aktor)).append("</span>");
		}
		sb.append("</div>");
		// Baris 2: status pill + nomor langkah
		sb.append("<div style='display:flex;align-items:center;gap:6px;font-size:10px;font-weight:normal;'>");
		sb.append("<span style='background:#eef2f7;color:#334155;border:1px solid #cbd5e1;border-radius:999px;padding:3px 8px;'>")
				.append(html(status)).append("</span>");
		sb.append("<span style='opacity:.8;'>Langkah ").append(nomor).append(" dari ")
				.append(jumlahDisposisi <= 0 ? nomor : jumlahDisposisi).append("</span>");
		sb.append("</div>");
		sb.append("</div>");
		caption.appendChild(new Html(sb.toString()));
		return caption;
	}

	
	private static void renderWorkflowOverviewPanel(final Vbox parent, final DisposisiSop disposisiSop,
			final List<DisposisiAlurSop> disposisiAlurSops, final int jumlahDisposisi, final boolean adaForm,
			final boolean tampilkanAlurMenunggu, final Component subcenter, final Component eastPanel,
			final Button batalkan, final EventListener dataSearchDefault) {
		if (parent == null) {
			return;
		}

		Vbox wrapper = new Vbox();
		wrapper.setWidth("100%");
		wrapper.setStyle("margin:0 0 8px 0;");
		wrapper.setParent(parent);

		// Deskripsi singkat untuk pengguna awam
		String namaProses = "";
		try {
			namaProses = disposisiSop != null && disposisiSop.getSop() != null
					? disposisiSop.getSop().getNama() : "";
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		Html descHdr = new Html("<div style='padding:10px 14px;margin-bottom:8px;border-radius:12px;"
				+ "background:linear-gradient(135deg,#eff6ff,#f0fdf4);border:1px solid #bfdbfe;"
				+ "font-size:12px;color:#1e40af;line-height:1.5;'>"
				+ "<strong>📋 Bagan Alur Proses</strong> — "
				+ "Berikut urutan tahapan pengajuan <em>" + html(namaProses) + "</em>. "
				+ "Setiap kotak menunjukkan satu tahap, siapa yang bertanggung jawab, dan sudah sampai mana prosesnya."
				+ "</div>");
		descHdr.setParent(wrapper);

		Hbox opsiBar = new Hbox();
		opsiBar.setWidth("100%");
		opsiBar.setAlign("center");
		opsiBar.setStyle("padding:6px 10px;margin:0 0 6px 0;border-radius:10px;background:#ffffff;"
				+ "border:1px solid #e5e7eb;box-shadow:0 1px 3px rgba(15,23,42,0.05);box-sizing:border-box;");
		opsiBar.setParent(wrapper);

		final Checkbox chkTampilkanAlurMenunggu = new Checkbox("Tampilan Alur Menunggu");
		chkTampilkanAlurMenunggu.setChecked(tampilkanAlurMenunggu);
		chkTampilkanAlurMenunggu.setTooltiptext(
				"Jika dipilih, sistem menampilkan rencana alur setelah tahap yang sedang menunggu. Jika tidak dipilih, bagan hanya ditampilkan sampai tahap yang sedang berjalan.");
		chkTampilkanAlurMenunggu.setStyle("font-size:11px;font-weight:bold;color:#334155;");
		chkTampilkanAlurMenunggu.setParent(opsiBar);

		Label info = new Label(ais.common.Common.getBahasaConfig("Aktifkan pilihan ini jika ingin melihat alur lanjutan setelah tahap yang sedang menunggu."));
		info.setStyle("font-size:10px;color:#64748b;margin-left:8px;");
		info.setParent(opsiBar);

		final Vbox overviewBody = new Vbox();
		overviewBody.setWidth("100%");
		overviewBody.setParent(wrapper);

		renderWorkflowOverview(overviewBody, disposisiSop, disposisiAlurSops, jumlahDisposisi, adaForm,
				tampilkanAlurMenunggu);

		chkTampilkanAlurMenunggu.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eastPanel != null) {
					eastPanel.setAttribute(ATTR_TAMPILKAN_ALUR_MENUNGGU,
							Boolean.valueOf(chkTampilkanAlurMenunggu.isChecked()));
				}
				tampil(subcenter, eastPanel, disposisiSop, batalkan, dataSearchDefault);
			}
		});
	}

	private static void renderWorkflowOverview(Vbox parent, DisposisiSop disposisiSop,
			List<DisposisiAlurSop> disposisiAlurSops, int jumlahDisposisi, boolean adaForm) {
		renderWorkflowOverview(parent, disposisiSop, disposisiAlurSops, jumlahDisposisi, adaForm, false);
	}

private static void renderWorkflowOverview(Vbox parent, DisposisiSop disposisiSop,
			List<DisposisiAlurSop> disposisiAlurSops, int jumlahDisposisi, boolean adaForm,
			boolean tampilkanAlurMenunggu) {
		if (parent == null) {
			return;
		}
		try {
			int selesai = 0;
			int menunggu = 0;
			int belumDilewati = 0;
			int lewatBatasWaktu = 0;
			int jumlahPunyaDeadline = 0;
			Date deadlineTerdekat = null;
			Date sekarang = new Date();
			StringBuilder steps = new StringBuilder();
			int jumlahRencanaBerikutnya = 0;

			Map<Long, DisposisiAlurSop> disposisiByAlur = new HashMap<Long, DisposisiAlurSop>();
			if (disposisiAlurSops != null) {
				for (DisposisiAlurSop d : disposisiAlurSops) {
					try {
						if (d != null && d.getAlurSop() != null && d.getAlurSop().getId() != null) {
							disposisiByAlur.put(d.getAlurSop().getId(), d);
						}
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}
			}

			List<AlurSop> semuaAlur = ambilSemuaAlurSopUntukOverview(disposisiSop);
			if (semuaAlur.isEmpty() && disposisiAlurSops != null) {
				for (DisposisiAlurSop d : disposisiAlurSops) {
					if (d != null && d.getAlurSop() != null) {
						semuaAlur.add(d.getAlurSop());
					}
				}
				Collections.sort(semuaAlur);
			}

			int no = 0;
			Set<Long> alurRencanaDitampilkan = new HashSet<Long>();
			for (AlurSop alur : semuaAlur) {
				if (alur == null || alur.getId() == null) {
					continue;
				}
				DisposisiAlurSop d = disposisiByAlur.get(alur.getId());
				if (d == null && alurRencanaDitampilkan.contains(alur.getId())) {
					continue;
				}
				if (!tampilkanAlurMenunggu && d == null) {
					continue;
				}
				boolean sudahDilewati = d != null;
				boolean sudahDiisi = d != null && (d.getDiajukanOleh() != null || d.getMahasiswa() != null || d.getSiswa() != null);
				Date deadline = null;
				try {
					deadline = d == null ? null : d.getWaktuMaksimal();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				if (deadline != null) {
					jumlahPunyaDeadline++;
					if (!sudahDiisi && deadline.before(sekarang)) {
						lewatBatasWaktu++;
					}
					if (!sudahDiisi && (deadlineTerdekat == null || deadline.before(deadlineTerdekat))) {
						deadlineTerdekat = deadline;
					}
				}
				if (!sudahDilewati) {
					belumDilewati++;
				} else if (!sudahDiisi) {
					menunggu++;
				} else {
					selesai++;
				}
				no++;
				appendWorkflowStep(steps, alur, d, no, sudahDilewati, sudahDiisi);

				/*
				 * Jika sebuah tahap masih menunggu, langkah berikutnya tidak lagi ditaruh
				 * sebagai teks di dalam card tersebut. Langkah berikutnya dirender sebagai
				 * card lanjutan sampai alur berhenti, sehingga pengguna dapat melihat
				 * rencana proses berikutnya secara berurutan.
				 */
				if (tampilkanAlurMenunggu && sudahDilewati && !sudahDiisi) {
					int beforePlan = no;
					Set<Long> path = new HashSet<Long>();
					path.add(alur.getId());
					no = appendFutureWorkflowCards(steps, alur, no, path, 0, disposisiByAlur,
							alurRencanaDitampilkan);
					jumlahRencanaBerikutnya += no - beforePlan;
				}
			}

			String namaSop = "Proses SOP";
			String kode = "";
			try {
				if (disposisiSop != null && disposisiSop.getSop() != null && disposisiSop.getSop().getNama() != null) {
					namaSop = disposisiSop.getSop().getNama();
				}
				kode = disposisiSop == null || disposisiSop.getSop() == null || disposisiSop.getSop().getKode() == null ? "" : disposisiSop.getSop().getKode();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			String statusSop = "Sedang Berjalan";
			String statusBg = "#fef3c7";
			String statusColor = "#854d0e";
			if (no <= 0) {
				statusSop = "Belum Ada Alur";
				statusBg = "#e5e7eb";
				statusColor = "#374151";
			} else if (menunggu <= 0 && belumDilewati <= 0) {
				statusSop = "Selesai";
				statusBg = "#dcfce7";
				statusColor = "#166534";
			} else if (lewatBatasWaktu > 0) {
				statusSop = "Perlu Segera Ditindaklanjuti";
				statusBg = "#fee2e2";
				statusColor = "#991b1b";
			}

			String deadlineText = "Belum ada batas waktu aktif";
			if (deadlineTerdekat != null) {
				deadlineText = "Batas waktu terdekat: " + formatDeadline(deadlineTerdekat);
			} else if (jumlahPunyaDeadline > 0 && menunggu <= 0) {
				deadlineText = "Semua langkah yang memiliki batas waktu sudah diproses";
			}

			StringBuilder html = new StringBuilder();
			html.append("<div style='margin:0 0 8px 0;padding:10px 11px;border-radius:12px;"
					+ "background:#f8fafc;color:#1f2937;border:1px solid #d6dce5;"
					+ "box-shadow:0 1px 4px rgba(15,23,42,0.06);font-family:Arial,sans-serif;'>");
			html.append("<div style='display:flex;justify-content:space-between;align-items:flex-start;gap:12px;flex-wrap:wrap;'>");
			html.append("<div style='max-width:760px;'><div style='font-size:15px;font-weight:bold;color:#1f2937;'>▣ ").append(html(namaSop)).append("</div>");
			if (kode != null && kode.trim().length() > 0) {
				html.append("<div style='font-size:11px;color:#64748b;margin-top:3px;'>Kode: ").append(html(kode)).append("</div>");
			}
			html.append("<div style='font-size:11px;color:#64748b;margin-top:5px;line-height:1.55;'>"
					+ "Bagan ini menunjukkan posisi pengajuan sampai tahap yang sedang berjalan. Centang Tampilan Alur Menunggu untuk melihat rencana alur berikutnya setelah tahap yang masih menunggu."
					+ "</div>");
			html.append("<div style='margin-top:8px;display:flex;gap:8px;flex-wrap:wrap;'>");
			html.append("<span style='padding:5px 9px;border-radius:999px;background:").append(statusBg)
					.append(";color:").append(statusColor).append(";font-size:11px;font-weight:bold;border:1px solid #d6dce5;'>Status SOP: ")
					.append(html(statusSop)).append("</span>");
			html.append("<span style='padding:5px 9px;border-radius:999px;background:#eff6ff;color:#1d4ed8;font-size:11px;font-weight:bold;border:1px solid #bfdbfe;'>")
					.append(html(deadlineText)).append("</span>");
			html.append("</div></div>");
			html.append("<div style='display:flex;gap:8px;flex-wrap:wrap;'>");
			html.append(statPill("Total Kotak", no + "", "#e0f2fe", "#075985"));
			html.append(statPill("Sudah Diproses", selesai + "", "#dcfce7", "#166534"));
			html.append(statPill("Masih Menunggu", menunggu + "", "#fef3c7", "#854d0e"));
			if (tampilkanAlurMenunggu) {
				html.append(statPill("Belum Dilewati", belumDilewati + "", "#e5e7eb", "#374151"));
				if (jumlahRencanaBerikutnya > 0) {
					html.append(statPill("Rencana Berikutnya", jumlahRencanaBerikutnya + "", "#ede9fe", "#5b21b6"));
				}
			}
			html.append(statPill("Lewat Batas Waktu", lewatBatasWaktu + "", lewatBatasWaktu > 0 ? "#fee2e2" : "#e5e7eb", lewatBatasWaktu > 0 ? "#991b1b" : "#374151"));
			html.append("</div></div>");
			html.append("<div style='margin-top:10px;display:flex;flex-wrap:wrap;gap:7px;align-items:stretch;"
					+ "overflow-x:hidden;white-space:normal;padding-bottom:4px;'>");
			html.append(steps.toString());
			html.append("</div></div>");
			Html h = new Html(html.toString());
			h.setParent(parent);
			semuaAlur.clear();
			disposisiByAlur.clear();
			alurRencanaDitampilkan.clear();
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/TampilanAlurSopAction.java:2664");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static List<AlurSop> ambilSemuaAlurSopUntukOverview(DisposisiSop disposisiSop) {
		List<AlurSop> list = new ArrayList<AlurSop>();
		try {
			if (disposisiSop == null || disposisiSop.getSop() == null || disposisiSop.getSop().getId() == null) {
				return list;
			}
			/*
			 * Versi lama menyisir seluruh cache AlurSop. Pada jumlah SOP besar, proses ini
			 * membuat diagram lambat. Ambil langsung alur aktif milik SOP ini saja dari
			 * database menggunakan currentSession() yang memang sedang aktif pada request.
			 */
			Session session = HibernateUtil.currentNativeSession();
			list = session.createCriteria(AlurSop.class)
					.add(Restrictions.eq("sop", disposisiSop.getSop()))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("nomor")).addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).list();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return list == null ? new ArrayList<AlurSop>() : list;
	}



	private static void appendWorkflowStep(StringBuilder sb, AlurSop alur, DisposisiAlurSop d, int no,
			boolean sudahDilewati, boolean sudahDiisi) {
		String nama = "Alur";
		String aktor = "";
		String icon = "…";
		String bg = "#f1f5f9";
		String border = "#cbd5e1";
		String color = "#64748b";
		String statusText = "Belum dilewati";
		String statusBg = "#e5e7eb";
		String statusColor = "#374151";
		Date deadline = null;
		try {
			nama = alur == null ? nama : alur.getNama();
			aktor = alur == null ? aktor : alur.getAktor();
			deadline = d == null ? null : d.getWaktuMaksimal();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		if (sudahDiisi) {
			icon = "✓";
			bg = "#ffffff";
			border = "#cbd5e1";
			color = "#334155";
			statusText = "Sudah diproses";
			statusBg = "#dcfce7";
			statusColor = "#166534";
		} else if (sudahDilewati) {
			icon = "…";
			bg = "#fffdf2";
			border = "#d8c46d";
			color = "#7c640b";
			statusText = "Menunggu proses";
			statusBg = "#fef3c7";
			statusColor = "#854d0e";
		}
		if (sudahDilewati && !sudahDiisi && deadline != null && deadline.before(new Date())) {
			icon = "!";
			bg = "#fff1f2";
			border = "#fecdd3";
			color = "#991b1b";
			statusText = "Lewat batas waktu";
			statusBg = "#fee2e2";
			statusColor = "#991b1b";
		}

		appendWorkflowArrow(sb, no);
		sb.append("<div style='flex:1 1 190px;max-width:300px;min-width:160px;position:relative;box-sizing:border-box;"
				+ "white-space:normal;margin:0 0 6px 0;padding:7px 8px;border-radius:10px;background:").append(bg)
				.append(";border:1px solid ").append(border).append(";color:#0f172a;word-break:break-word;overflow-wrap:break-word;'>");
		sb.append("<div style='display:flex;align-items:flex-start;gap:6px;'>");
		sb.append("<span style='display:inline-block;min-width:22px;width:22px;height:22px;line-height:22px;font-size:11px;text-align:center;"
				+ "border-radius:50%;background:").append(color).append(";color:white;font-weight:bold;'>")
				.append(html(icon)).append("</span>");
		sb.append("<div style='min-width:0;width:100%;'>");
		sb.append("<b style='font-size:11px;line-height:1.25;white-space:normal;word-break:break-word;overflow-wrap:break-word;'>")
				.append(no).append(". ").append(html(nama)).append("</b>");
		sb.append("<div style='margin-top:4px;'>")
				.append("<span style='display:inline-block;border-radius:999px;padding:2px 6px;font-size:9px;font-weight:bold;background:")
				.append(statusBg).append(";color:").append(statusColor).append(";'>")
				.append(html(statusText)).append("</span></div>");
		if (aktor != null && aktor.trim().length() > 0) {
			sb.append("<div style='font-size:9px;color:#64748b;margin-top:4px;white-space:normal;word-break:break-word;overflow-wrap:break-word;'>👤 ")
					.append(html(aktor)).append("</div>");
		}
		if (deadline != null) {
			sb.append("<div style='font-size:9px;color:").append(deadline.before(new Date()) && !sudahDiisi ? "#991b1b" : "#475569")
					.append(";margin-top:4px;line-height:1.25;white-space:normal;word-break:break-word;overflow-wrap:break-word;'>⏱ Batas waktu: ")
					.append(html(formatDeadline(deadline))).append("</div>");
		}
		/* Keterangan panjang di bawah card sengaja dihilangkan agar setiap kotak alur lebih ringkas dan tidak terlalu tinggi. */
		/* Informasi alur berikutnya pada card yang sudah diproses dihilangkan agar kotak tetap ringkas. */
		sb.append("</div></div></div>");
	}


	private static int appendFutureWorkflowCards(StringBuilder sb, AlurSop current, int nomorAwal,
			Set<Long> path, int depth, Map<Long, DisposisiAlurSop> disposisiByAlur,
			Set<Long> alurRencanaDitampilkan) {
		if (sb == null || current == null || depth >= 20) {
			return nomorAwal;
		}
		int no = nomorAwal;
		try {
			List<AlurSop> nexts = current.ambilAlurSetelahnya();
			List<String> opsi = current.ambilOpsiAlurSetelahnya();
			if (nexts == null || nexts.isEmpty()) {
				return no;
			}
			for (int i = 0; i < nexts.size(); i++) {
				AlurSop next = nexts.get(i);
				if (next == null || next.getId() == null) {
					continue;
				}
				String opsiText = opsi != null && i < opsi.size() && opsi.get(i) != null ? opsi.get(i).trim() : "";
				boolean kembaliKeAlurSebelumnya = path != null && path.contains(next.getId());
				if (alurRencanaDitampilkan != null
						&& (disposisiByAlur == null || !disposisiByAlur.containsKey(next.getId()))) {
					alurRencanaDitampilkan.add(next.getId());
				}
				no++;
				appendPlannedWorkflowStep(sb, next, no, opsiText, kembaliKeAlurSebelumnya);

				if (!kembaliKeAlurSebelumnya) {
					Set<Long> childPath = new HashSet<Long>();
					if (path != null) {
						childPath.addAll(path);
					}
					childPath.add(next.getId());
					no = appendFutureWorkflowCards(sb, next, no, childPath, depth + 1, disposisiByAlur,
							alurRencanaDitampilkan);
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return no;
	}

	private static void appendPlannedWorkflowStep(StringBuilder sb, AlurSop alur, int no, String opsiText,
			boolean kembaliKeAlurSebelumnya) {
		String nama = "Alur lanjutan";
		String kode = "";
		String aktor = "";
		try {
			if (alur != null) {
				nama = hasText(alur.getNama()) ? alur.getNama() : nama;
				kode = hasText(alur.getKode()) ? alur.getKode() : "";
				aktor = hasText(alur.getAktor()) ? alur.getAktor() : "";
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		String statusText = kembaliKeAlurSebelumnya ? "Kembali ke alur sebelumnya" : "Rencana berikutnya";
		String bg = kembaliKeAlurSebelumnya ? "#fff7ed" : "#f5f3ff";
		String border = kembaliKeAlurSebelumnya ? "#fdba74" : "#c4b5fd";
		String color = kembaliKeAlurSebelumnya ? "#9a3412" : "#5b21b6";
		String statusBg = kembaliKeAlurSebelumnya ? "#fed7aa" : "#ede9fe";
		String statusColor = kembaliKeAlurSebelumnya ? "#9a3412" : "#5b21b6";

		appendWorkflowArrow(sb, no);
		sb.append("<div style='flex:1 1 190px;max-width:300px;min-width:160px;position:relative;box-sizing:border-box;"
				+ "white-space:normal;margin:0 0 6px 0;padding:7px 8px;border-radius:10px;background:").append(bg)
				.append(";border:1px dashed ").append(border).append(";color:#0f172a;word-break:break-word;overflow-wrap:break-word;'>");
		sb.append("<div style='display:flex;align-items:flex-start;gap:6px;'>");
		sb.append("<span style='display:inline-block;min-width:22px;width:22px;height:22px;line-height:22px;font-size:11px;text-align:center;"
				+ "border-radius:50%;background:").append(color).append(";color:white;font-weight:bold;'>→</span>");
		sb.append("<div style='min-width:0;width:100%;'>");
		sb.append("<b style='font-size:11px;line-height:1.25;white-space:normal;word-break:break-word;overflow-wrap:break-word;'>")
				.append(no).append(". ");
		if (kode.length() > 0) {
			sb.append(html(kode)).append(" - ");
		}
		sb.append(html(nama)).append("</b>");
		sb.append("<div style='margin-top:4px;'>")
				.append("<span style='display:inline-block;border-radius:999px;padding:2px 6px;font-size:9px;font-weight:bold;background:")
				.append(statusBg).append(";color:").append(statusColor).append(";'>")
				.append(html(statusText)).append("</span></div>");
		if (aktor.length() > 0) {
			sb.append("<div style='font-size:9px;color:#64748b;margin-top:4px;white-space:normal;word-break:break-word;overflow-wrap:break-word;'>👤 ")
					.append(html(aktor)).append("</div>");
		}
		if (hasText(opsiText)) {
			sb.append("<div style='font-size:9px;color:#92400e;margin-top:4px;line-height:1.25;white-space:normal;word-break:break-word;overflow-wrap:break-word;'>Opsi cabang: ")
					.append(html(opsiText)).append("</div>");
		}
		/* Keterangan panjang pada rencana berikutnya dihilangkan agar kotak alur lebih tipis. */
		sb.append("</div></div></div>");
	}



	private static void appendWorkflowArrow(StringBuilder sb, int nomorKotak) {
		if (sb == null || nomorKotak <= 1) {
			return;
		}
		sb.append("<div style='align-self:center;min-width:16px;width:16px;text-align:center;"
				+ "font-size:16px;font-weight:900;color:#94a3b8;margin:0 -1px 6px -1px;"
				+ "font-family:Arial,sans-serif;'>→</div>");
	}


	private static String buildFutureBranchText(AlurSop alur) {
		StringBuilder sb = new StringBuilder();
		try {
			if (alur == null) {
				return "";
			}
			List<AlurSop> nexts = alur.ambilAlurSetelahnya();
			List<String> opsi = alur.ambilOpsiAlurSetelahnya();
			for (int i = 0; nexts != null && i < nexts.size(); i++) {
				AlurSop next = nexts.get(i);
				if (next == null) {
					continue;
				}
				String opsiText = opsi != null && i < opsi.size() && opsi.get(i) != null ? opsi.get(i).trim() : "";
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("<b>").append(html(next.getKode())).append("</b>");
				if (hasText(next.getNama())) {
					sb.append(" - ").append(html(next.getNama()));
				}
				if (opsiText.length() > 0) {
					sb.append(" <span style='color:#92400e;'>").append(html(opsiText)).append("</span>");
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return sb.toString();
	}


	private static void appendWorkflowStep(StringBuilder sb, DisposisiAlurSop d, int no, boolean sudahDiisi) {
		String nama = "Alur";
		String aktor = "";
		String icon = sudahDiisi ? "✓" : "…";
		String bg = sudahDiisi ? "#ffffff" : "#fffdf2";
		String border = sudahDiisi ? "#cbd5e1" : "#d8c46d";
		String color = sudahDiisi ? "#334155" : "#7c640b";
		String statusText = sudahDiisi ? "Sudah diproses" : "Menunggu proses";
		String statusBg = sudahDiisi ? "#dcfce7" : "#fef3c7";
		String statusColor = sudahDiisi ? "#166534" : "#854d0e";
		Date deadline = null;
		try {
			nama = d.getAlurSop().getNama();
			aktor = d.getAlurSop().getAktor();
			deadline = d.getWaktuMaksimal();
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		if (!sudahDiisi && deadline != null && deadline.before(new Date())) {
			icon = "!";
			bg = "#fff1f2";
			border = "#fecdd3";
			color = "#991b1b";
			statusText = "Lewat batas waktu";
			statusBg = "#fee2e2";
			statusColor = "#991b1b";
		}

		/*
		 * Maksimal 3 kotak per baris. Jika alur lebih dari 3, CSS flex-wrap akan
		 * otomatis menurunkan kotak berikutnya ke baris baru. Ini menghindari bagan
		 * melebar ke samping dan terpotong pada layar kecil maupun saat tampil di popup.
		 */
		appendWorkflowArrow(sb, no);
		sb.append("<div style='flex:1 1 190px;max-width:300px;min-width:160px;position:relative;box-sizing:border-box;"
				+ "white-space:normal;margin:0 0 6px 0;padding:7px 8px;border-radius:10px;background:").append(bg)
				.append(";border:1px solid ").append(border).append(";color:#0f172a;word-break:break-word;overflow-wrap:break-word;'>");
		sb.append("<div style='display:flex;align-items:flex-start;gap:6px;'>");
		sb.append("<span style='display:inline-block;min-width:22px;width:22px;height:22px;line-height:22px;font-size:11px;text-align:center;"
				+ "border-radius:50%;background:").append(color).append(";color:white;font-weight:bold;'>")
				.append(html(icon)).append("</span>");
		sb.append("<div style='min-width:0;width:100%;'>");
		sb.append("<b style='font-size:11px;line-height:1.25;white-space:normal;word-break:break-word;overflow-wrap:break-word;'>")
				.append(no).append(". ").append(html(nama)).append("</b>");
		sb.append("<div style='margin-top:4px;'>")
				.append("<span style='display:inline-block;border-radius:999px;padding:2px 6px;font-size:9px;font-weight:bold;background:")
				.append(statusBg).append(";color:").append(statusColor).append(";'>")
				.append(html(statusText)).append("</span></div>");
		if (aktor != null && aktor.trim().length() > 0) {
			sb.append("<div style='font-size:9px;color:#64748b;margin-top:4px;white-space:normal;word-break:break-word;overflow-wrap:break-word;'>👤 ")
					.append(html(aktor)).append("</div>");
		}
		if (deadline != null) {
			sb.append("<div style='font-size:9px;color:").append(deadline.before(new Date()) && !sudahDiisi ? "#991b1b" : "#475569")
					.append(";margin-top:4px;line-height:1.25;white-space:normal;word-break:break-word;overflow-wrap:break-word;'>⏱ Batas waktu: ")
					.append(html(formatDeadline(deadline))).append("</div>");
		}
		sb.append("</div></div></div>");
	}

	private static String formatDeadline(Date deadline) {
		if (deadline == null) {
			return "-";
		}
		try {
			return Common.dateFormat61.get().format(deadline);
		} catch (Exception e) {
			try {
				return Common.dateFormat3.get().format(deadline);
			} catch (Exception ex) {
				return deadline.toString();
			}
		}
	}

	private static String statPill(String label, String value, String bg, String color) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='min-width:82px;background:").append(bg).append(";color:").append(color)
				.append(";border:1px solid #d6dce5;border-radius:9px;padding:5px 8px;text-align:center;'>");
		sb.append("<div style='font-size:15px;font-weight:bold;'>").append(html(value)).append("</div>");
		sb.append("<div style='font-size:10px;'>").append(html(label)).append("</div></div>");
		return sb.toString();
	}

	private static String buildActorHtml(Tbmuser tbmuser, DisposisiAlurSop disposisiAlurSop) {
		String nama = "Pengguna";
		String role = "";
		String aktor = "";
		try {
			if (tbmuser != null && tbmuser.getUserNama() != null) {
				nama = tbmuser.getUserNama();
			}
			if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleName() != null) {
				role = tbmuser.hakAkses().getRoleName();
			}
			if (disposisiAlurSop != null && disposisiAlurSop.getAlurSop() != null) {
				aktor = disposisiAlurSop.getAlurSop().getAktor();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='margin-top:6px;padding:8px 10px;border-radius:10px;background:#f1f5f9;"
				+ "border:1px solid #e2e8f0;'>");
		sb.append("<div style='font-weight:bold;color:#0f172a;font-size:12px;'>👤 ").append(html(nama));
		if (role != null && role.trim().length() > 0) {
			sb.append(" <span style='font-weight:normal;color:#64748b;'> (").append(html(role)).append(")</span>");
		}
		sb.append("</div>");
		if (aktor != null && aktor.trim().length() > 0) {
			sb.append("<div style='font-size:11px;color:#1d4ed8;margin-top:3px;font-weight:bold;'>")
					.append(html(aktor)).append("</div>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private static void renderWorkflowProgressCard(Vbox parent, DisposisiSop disposisiSop,
			List disposisiAlurSops, int jumlahDisposisi) {
		try {
			if (parent == null) {
				return;
			}
			String namaProses = "";
			String pengaju = "";
			String tglPengajuan = "";
			int selesai = jumlahDisposisi;
			int total = disposisiAlurSops != null ? disposisiAlurSops.size() : 0;
			int menunggu = total - selesai;
			if (menunggu < 0) {
				menunggu = 0;
			}
			try {
				if (disposisiSop != null && disposisiSop.getSop() != null) {
					namaProses = nullSafe(disposisiSop.getSop().getNama());
				}
			} catch (Exception ex) {
				ais.common.Common.tampilErrorJikaAdmin(ex);
			}
			try {
				if (disposisiSop != null && disposisiSop.getDiajukanOleh() != null) {
					pengaju = nullSafe(disposisiSop.getDiajukanOleh().getUserNama());
				}
			} catch (Exception ex) {
				ais.common.Common.tampilErrorJikaAdmin(ex);
			}
			try {
				if (disposisiSop != null) {
					tglPengajuan = Common.dateFormat3.get().format(disposisiSop.getWaktu());
				}
			} catch (Exception ex) {
				ais.common.Common.tampilErrorJikaAdmin(ex);
			}

			int persen = total > 0 ? (selesai * 100 / total) : 0;
			boolean sudahSelesaiSemua = total > 0 && selesai >= total;

			StringBuilder sb = new StringBuilder();
			sb.append("<div style='margin:0 0 10px 0;padding:12px 14px;border-radius:12px;"
					+ "background:linear-gradient(135deg,#eff6ff,#f0fdf4);"
					+ "border:1px solid #bfdbfe;font-family:sans-serif;box-sizing:border-box;'>");

			// Judul
			sb.append("<div style='font-size:13px;font-weight:bold;color:#1e40af;margin-bottom:4px;'>")
					.append("&#x1F4CA; Progres Pengajuan</div>");
			sb.append("<div style='font-size:11px;color:#64748b;margin-bottom:8px;line-height:1.4;'>")
					.append("Ringkasan perjalanan pengajuan ")
					.append(namaProses.isEmpty() ? "" : "<em>" + html(namaProses) + "</em> ")
					.append("dari tahap pertama hingga selesai.</div>");

			// Pills statistik
			sb.append("<div style='display:flex;flex-wrap:wrap;gap:5px;margin-bottom:8px;'>");
			sb.append("<span style='padding:3px 9px;border-radius:999px;font-size:11px;font-weight:bold;"
					+ "background:#dcfce7;color:#166534;border:1px solid #bbf7d0;'>&#x2713; Selesai: ")
					.append(selesai).append("</span>");
			if (menunggu > 0) {
				sb.append("<span style='padding:3px 9px;border-radius:999px;font-size:11px;font-weight:bold;"
						+ "background:#fef3c7;color:#854d0e;border:1px solid #fde68a;'>&#x23F3; Menunggu: ")
						.append(menunggu).append("</span>");
			}
			if (total > 0) {
				sb.append("<span style='padding:3px 9px;border-radius:999px;font-size:11px;"
						+ "background:#f1f5f9;color:#475569;border:1px solid #e2e8f0;'>Total: ")
						.append(total).append(" tahap</span>");
			}
			if (sudahSelesaiSemua) {
				sb.append("<span style='padding:3px 9px;border-radius:999px;font-size:11px;font-weight:bold;"
						+ "background:#d1fae5;color:#064e3b;border:1px solid #6ee7b7;'>&#x2714;&#x2714; Selesai Semua</span>");
			}
			sb.append("</div>");

			// Progress bar
			if (total > 0) {
				String barColor = sudahSelesaiSemua
						? "linear-gradient(90deg,#22c55e,#16a34a)"
						: "linear-gradient(90deg,#3b82f6,#2563eb)";
				sb.append("<div style='font-size:10px;color:#64748b;margin-bottom:3px;'>Ketercapaian: <b>")
						.append(persen).append("%</b></div>");
				sb.append("<div style='height:10px;border-radius:999px;background:#e2e8f0;overflow:hidden;margin-bottom:8px;'>");
				sb.append("<div style='height:100%;width:").append(persen)
						.append("%;background:").append(barColor)
						.append(";border-radius:999px;transition:width 0.3s;'></div>");
				sb.append("</div>");
			}

			// Info pengaju + tanggal
			if (pengaju != null && !pengaju.trim().isEmpty()) {
				sb.append("<div style='font-size:11px;color:#334155;margin-bottom:3px;'>")
						.append("&#x1F464; Diajukan oleh: <b>").append(html(pengaju)).append("</b></div>");
			}
			if (tglPengajuan != null && !tglPengajuan.trim().isEmpty()) {
				sb.append("<div style='font-size:11px;color:#334155;'>")
						.append("&#x1F4C5; Tanggal pengajuan: <b>").append(html(tglPengajuan)).append("</b></div>");
			}

			sb.append("</div>");

			Html hCard = new Html(sb.toString());
			hCard.setParent(parent);
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/TampilanAlurSopAction.java:3132");
			}
		}
	}

	private static String limitText(String text, int max) {
		if (text == null) {
			return "";
		}
		String t = text.trim();
		if (max <= 0 || t.length() <= max) {
			return t;
		}
		if (max <= 3) {
			return t.substring(0, max);
		}
		return t.substring(0, max - 3) + "...";
	}

	private static String html(Object value) {
		String s = value == null ? "" : String.valueOf(value);
		StringBuilder sb = new StringBuilder(s.length() + 16);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '&') {
				sb.append("&amp;");
			} else if (c == '<') {
				sb.append("&lt;");
			} else if (c == '>') {
				sb.append("&gt;");
			} else if (c == '"') {
				sb.append("&quot;");
			} else if (c == '\'') {
				sb.append("&#39;");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order, Session session) {
		Tbmuser tbmuser = Common.getCurrentUser();
		boolean melihatSemuaSop = AktorSop.bolehMelihatSemuaSop(tbmuser);

		List<Long> disposisi = new ArrayList<Long>();
		if (!melihatSemuaSop) {
			Criteria criteriaDisposisi = session.createCriteria(DisposisiAlurSop.class).add(Restrictions.isNotNull("alurSop"))
						.createAlias("alurSop", "alurSop")
						.createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
						.createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN)
						// Alias EKSPLISIT untuk disposisiSop supaya projection groupProperty("disposisiSop.id")
						// dan join "atasan langsung/pejabat" (disposisiSop.diajukanOleh.pegawai) yang ditambahkan
						// AktorSop.buatCriterion memakai SATU join yang sama. Tanpa ini, projection membuat join
						// disposisiSop tersendiri sehingga alias pegawai (pegawaidat5_) hilang dari FROM ->
						// "missing FROM-clause entry for table pegawaidat5_".
						.createAlias("disposisiSop", "disposisiSop");
				/*
				 * PENTING: teruskan criteriaDisposisi ke buatCriterion. Tanpa argumen criteria,
				 * buatCriterion memakai criteria=null sehingga pencocokan AKTOR untuk pegawai hanya
				 * mengecek aktorSop.semuaPegawai; pencocokan "atasan langsung"/"atasan pejabat"
				 * (butuh join ke disposisiSop.diajukanOleh.pegawai) DILEWATI, sehingga aktor yang
				 * menyetujui pengajuan bawahannya (mis. Koordinator sbg atasan langsung) tidak
				 * melihat disposisi yang menunggu persetujuannya.
				 */
				criteriaDisposisi.add(tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null)
						? Restrictions.or(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()),
								Restrictions.eq("siswa", tbmuser.getSiswa()))
						: Restrictions.or(AktorSop.buatCriterion(tbmuser, true, criteriaDisposisi),
								(Common.getApakahAdmin() ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("diajukanOleh", tbmuser))));
				// JANGAN pakai setProjection(groupProperty("disposisiSop.id")) di sini: pada
				// Hibernate 3.6, projection + join dalam (alias pegawai dari AktorSop.buatCriterion,
				// mis. pegawaidat5_) menghasilkan SQL yang membuang alias join dari FROM →
				// "missing FROM-clause entry for table pegawaidat5_". Ambil entity-nya (join WHERE
				// tetap utuh) lalu kumpulkan id disposisiSop secara unik di Java.
				List<DisposisiAlurSop> rowsDisposisi = criteriaDisposisi.list();
				java.util.Set<Long> setDisposisiId = new java.util.LinkedHashSet<Long>();
				for (DisposisiAlurSop dd : rowsDisposisi) {
					if (dd != null && dd.getDisposisiSop() != null && dd.getDisposisiSop().getId() != null) {
						setDisposisiId.add(dd.getDisposisiSop().getId());
					}
				}
				disposisi = new ArrayList<Long>(setDisposisiId);
		}

		Criteria criteria = session.createCriteria(DisposisiSop.class).createAlias("sop", "sop")
				.add(Restrictions.eq("sop.aktif", true))
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		if (!melihatSemuaSop) {
			criteria.add(Restrictions.or(
					disposisi.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", disposisi),
					tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null)
							? Restrictions.or(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()),
									Restrictions.eq("siswa", tbmuser.getSiswa()))
							: (Common.getApakahAdmin() ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("diajukanOleh", tbmuser))));

			if (tbmuser != null) {
				criteria.add(tbmuser.ambilJurusan() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("sop.jurusan"),
								Restrictions.eq("sop.jurusan", tbmuser.ambilJurusan())))
						.add(tbmuser.ambilFakultas() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sop.fakultas"),
										Restrictions.eq("sop.fakultas", tbmuser.ambilFakultas())))
						.add(tbmuser.ambilSekolah() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sop.sekolah"),
										Restrictions.eq("sop.sekolah", tbmuser.ambilSekolah())))
						.add(tbmuser.ambilYayasan() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sop.yayasan"),
										Restrictions.eq("sop.yayasan", tbmuser.ambilYayasan())));
			}
		}

		if (order) {
			criteria.addOrder(Order.asc("id"));
		}

		return criteria;
	}

	// ==================================================================================
	// POPUP "EDIT SOP INI" — BAGAN + TABEL (ADMIN ONLY)
	// ==================================================================================

	/**
	 * Membuka popup {@link MyWindow} berisi dua tab untuk meninjau dan mengelola definisi SOP:
	 * <ul>
	 *   <li><b>Tab 1 — Bagan Alur Proses:</b> Flowchart visual vertikal yang menampilkan
	 *       semua langkah ({@link AlurSop}) milik SOP ini secara berurutan, lengkap dengan
	 *       titik mulai (START), titik selesai (END), panah arah alur, pelaku (aktor),
	 *       dokumen yang diperlukan, dan informasi percabangan bila ada lebih dari satu
	 *       langkah berikutnya.</li>
	 *   <li><b>Tab 2 — Tabel Data Alur:</b> Grid interaktif yang memuat seluruh langkah
	 *       ({@link AlurSop}) dalam bentuk baris-kolom. Setiap baris dapat diedit secara
	 *       langsung melalui form kecil inline tanpa harus berpindah halaman.</li>
	 * </ul>
	 *
	 * <p><b>Syarat akses:</b> Tombol pemicu method ini hanya ditampilkan ketika
	 * {@code Common.merupakanadmin == true}. Pengecekan dilakukan di sisi pemanggil
	 * sebelum method ini dipanggil; method ini sendiri tidak mengulang pengecekan agar
	 * tetap efisien.</p>
	 *
	 * <p><b>Cara kerja popup:</b></p>
	 * <ol>
	 *   <li>Admin menekan tombol "Edit SOP ini" di toolbar halaman {@code TampilanAlurSopAction}.</li>
	 *   <li>Method ini menerima objek {@link Sop} yang sedang aktif sebagai konteks.</li>
	 *   <li>Popup terbuka dalam mode overlapped (tidak memblokir interaksi di halaman utama),
	 *       berukuran 92% lebar × 88% tinggi viewport.</li>
	 *   <li>Tab "Bagan Alur Proses" ditampilkan sebagai default saat popup terbuka.</li>
	 *   <li>Admin dapat beralih ke tab "Tabel Data Alur" untuk menyunting langkah.</li>
	 * </ol>
	 *
	 * <p><b>Struktur komponen dalam popup:</b></p>
	 * <pre>
	 * MyWindow ("Edit SOP: [nama SOP]")
	 *   └─ MyBorderlayout
	 *       ├─ North: Toolbar → [Tombol Tutup]
	 *       └─ Center: Tabbox
	 *           ├─ Tab "Bagan Alur Proses"
	 *           │   └─ MyTabpanel → renderBaganSopDefinisi(...)
	 *           └─ Tab "Tabel Data Alur"
	 *               └─ MyTabpanel → renderTabelAlurSopUntukPopup(...)
	 * </pre>
	 *
	 * <p><b>Aturan session ZK:</b> Method ini berjalan di thread event ZK sehingga TIDAK
	 * boleh memanggil {@code close()} atau {@code disconnect()} pada session yang diperoleh
	 * melalui {@code currentSession()} maupun {@code currentNativeSession()}. Session
	 * tersebut dikelola oleh konteks ZK dan akan ditutup otomatis ketika request selesai.
	 * Semua operasi database di dalam method ini bersifat read-only; penulisan hanya
	 * terjadi di dalam handler "Simpan" pada formulir edit per-langkah di tab Tabel.</p>
	 *
	 * <p><b>Kompatibilitas Java 1.7:</b> Semua listener event menggunakan anonymous inner
	 * class, bukan lambda expression. Semua variabel yang dirujuk di dalam anonymous class
	 * dideklarasikan sebagai {@code final} sesuai aturan bahasa Java 1.7.</p>
	 *
	 * <p><b>Penanganan kesalahan:</b> Seluruh blok dilingkupi {@code try/catch} yang
	 * meneruskan exception ke {@code Common.tampilErrorJikaAdmin(e)} agar error
	 * hanya tampil kepada admin dan tidak merusak tampilan pengguna biasa.</p>
	 *
	 * @param sop definisi SOP yang akan ditampilkan; bila {@code null} method langsung
	 *            kembali tanpa melakukan apa pun
	 */
	private static void bukaPopupEditSop(final Sop sop) {
		if (sop == null) {
			return;
		}
		try {
			String judulPopup = "Edit SOP: " + (sop.getNama() != null ? sop.getNama() : "");
			final MyWindow popup = new MyWindow(judulPopup, "normal", true);
			popup.setWidth("92%");
			popup.setHeight("88%");

			org.zkoss.zk.ui.Component pageRoot =
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
			popup.setParent(pageRoot);

			Borderlayout bl = new ais.ui.util.MyBorderlayout();
			bl.setParent(popup);

			// ── North: toolbar dengan tombol tutup ──
			North northBl = new North();
			northBl.setParent(bl);
			northBl.setBorder("none");
			ais.ui.util.ZkCompat.setFlex(northBl, true);

			Toolbar tbPopup = new Toolbar();
			tbPopup.setParent(northBl);

			MyToolbarbuttonConfig btnTutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			btnTutup.setTooltiptext("Tutup popup ini");
			btnTutup.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					popup.detach();
				}
			});
			btnTutup.setParent(tbPopup);

			// Keterangan admin di sebelah kanan tombol tutup
			Label lblAdmin = new Label(ais.common.Common.getBahasaConfig("Mode Admin — perubahan langsung tersimpan ke database."));
			lblAdmin.setStyle("font-size:10px;color:#64748b;margin-left:10px;");
			lblAdmin.setParent(tbPopup);

			// ── Center: tabbox dua tab ──
			Center centerBl = new Center();
			centerBl.setParent(bl);
			centerBl.setAutoscroll(true);
			ais.ui.util.ZkCompat.setFlex(centerBl, true);

			org.zkoss.zul.Tabbox tabbox = new org.zkoss.zul.Tabbox();
			tabbox.setWidth("100%");
			tabbox.setHeight("100%");
			tabbox.setParent(centerBl);

			Tabs tabsPopup = new Tabs();
			tabsPopup.setParent(tabbox);

			Tabpanels tabpanelsPopup = new Tabpanels();
			tabpanelsPopup.setParent(tabbox);

			// Tab 1: Bagan Alur Proses
			MyTabConfig tabBagan = new MyTabConfig("Bagan Alur Proses");
			tabBagan.setParent(tabsPopup);
			final Tabpanel tpBagan = new ais.ui.util.MyTabpanel();
			tpBagan.setStyle("overflow:auto;height:100%;padding:4px;box-sizing:border-box;");
			tpBagan.setParent(tabpanelsPopup);

			// Tab 2: Tabel Data Alur
			MyTabConfig tabTabel = new MyTabConfig("Tabel Data Alur");
			tabTabel.setParent(tabsPopup);
			final Tabpanel tpTabel = new ais.ui.util.MyTabpanel();
			tpTabel.setStyle("overflow:auto;height:100%;padding:4px;box-sizing:border-box;");
			tpTabel.setParent(tabpanelsPopup);

			// Render tiap tab dengan proteksi terpisah SEBELUM menampilkan modal. Window modal (onModal)
			// MENAHAN eksekusi sampai ditutup, sehingga seluruh isi WAJIB dirender lebih dulu. Proteksi
			// per-tab memastikan kegagalan render satu tab tidak membuat onModal() gagal dipanggil
			// (penyebab lama "Edit SOP diklik tidak muncul apa-apa": render melempar exception sehingga
			// perintah menampilkan window tak pernah tercapai).
			try {
				renderBaganSopDefinisi(tpBagan, sop);
			} catch (Exception exBagan) {
				new Html("<div style='padding:16px;color:#b91c1c;font-size:12px;'>Gagal memuat Bagan Alur: "
						+ html(exBagan.getMessage()) + "</div>").setParent(tpBagan);
				Common.tampilErrorJikaAdmin(exBagan);
			}
			try {
				renderTabelAlurSopUntukPopup(tpTabel, sop);
			} catch (Exception exTabel) {
				new Html("<div style='padding:16px;color:#b91c1c;font-size:12px;'>Gagal memuat Tabel Alur: "
						+ html(exTabel.getMessage()) + "</div>").setParent(tpTabel);
				Common.tampilErrorJikaAdmin(exTabel);
			}

			// Tampilkan sebagai MODAL memakai pola yang terbukti bekerja di halaman ini (lih. revisiHelper
			// & window utama: setVisible(true) → onModal()). doOverlapped() sebelumnya tidak menampilkan
			// window pada konteks tab ini.
			popup.setVisible(true);
			popup.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Merender flowchart visual definisi SOP (bukan instance pengajuan) ke dalam komponen
	 * induk yang diberikan. Berbeda dari {@code renderWorkflowOverview} yang menampilkan
	 * progres satu pengajuan ({@link DisposisiSop}), method ini menampilkan keseluruhan
	 * rancangan alur ({@link AlurSop}) yang menjadi cetak biru SOP tersebut.
	 *
	 * <p><b>Struktur visual yang dihasilkan:</b></p>
	 * <ol>
	 *   <li>Header info SOP: nama, kode, jumlah langkah aktif/nonaktif.</li>
	 *   <li>Node <b>START</b> (oval hijau) — menandai awal alur.</li>
	 *   <li>Untuk setiap {@link AlurSop} (diurutkan berdasarkan ID):
	 *       <ul>
	 *         <li>Panah arah (↓)</li>
	 *         <li>Kartu langkah: header berwarna (biru = Awal Pengajuan, hijau = Aktif,
	 *             abu-abu = Nonaktif) berisi nomor urut, kode, nama.</li>
	 *         <li>Body kartu: aktor, daftar dokumen wajib/opsional, info langkah berikutnya
	 *             atau cabang bila ada percabangan ({@code ambilAlurSetelahnya().size() > 1}).</li>
	 *       </ul>
	 *   </li>
	 *   <li>Node <b>SELESAI</b> (oval merah) — menandai akhir alur.</li>
	 * </ol>
	 *
	 * <p><b>CSS scoping:</b> Semua class CSS menggunakan awalan {@code sopdef-} untuk
	 * mencegah konflik dengan class global ZK atau Bootstrap yang sudah ada di halaman.</p>
	 *
	 * <p><b>Penanganan session:</b> Menggunakan {@code currentNativeSession()} (tidak
	 * menutup). Query dikonfigurasi dengan {@code FlushMode.MANUAL} agar auto-flush
	 * tidak menulis entitas kotor di tengah render, mencegah deadlock.</p>
	 *
	 * <p><b>Percabangan (branching):</b> Bila sebuah langkah memiliki lebih dari satu
	 * langkah berikutnya, setiap cabang ditampilkan sebagai pill berwarna berbeda
	 * menggunakan array {@code branchBg} dan {@code branchFg} yang dirotasi secara
	 * modular sehingga mendukung jumlah cabang yang tidak terbatas.</p>
	 *
	 * <p><b>Responsif:</b> Menggunakan media query {@code @media(max-width:560px)}
	 * untuk mengecilkan padding dan font pada layar sempit (mobile).</p>
	 *
	 * <p><b>N+1 aman:</b> Dokumen ({@link DokumenAlurSop}) diakses melalui koleksi
	 * {@code Set} yang sudah di-load bersama {@link AlurSop} dalam satu query Hibernate
	 * (lazy-load ditoleransi karena session masih aktif pada thread ZK ini).</p>
	 *
	 * <p><b>Fallback kosong:</b> Bila SOP belum memiliki langkah yang terdefinisi,
	 * method menampilkan pesan informatif dan langsung kembali.</p>
	 *
	 * <p><b>Kompatibilitas Java 1.7:</b> Tidak menggunakan lambda, enhanced for-loop
	 * digunakan sepanjang kode, variabel lokal dalam anonymous class bersifat final.</p>
	 *
	 * <p><b>Penanganan error:</b> Setiap akses properti yang berpotensi melempar exception
	 * (terutama koleksi lazy-load) dibungkus {@code try/catch} individual agar satu
	 * langkah yang error tidak menghentikan render keseluruhan flowchart.</p>
	 *
	 * @param parent  komponen ZK tempat hasil HTML dilekatkan; bila {@code null}
	 *                method langsung kembali
	 * @param sop     definisi SOP yang langkah-langkahnya akan divisualisasikan;
	 *                bila {@code null} method langsung kembali
	 */
	@SuppressWarnings("unchecked")
	private static void renderBaganSopDefinisi(Component parent, Sop sop) {
		if (parent == null || sop == null) {
			return;
		}
		try {
			Session sessionBagan = HibernateUtil.currentNativeSession();
			List<AlurSop> alurList = sessionBagan.createCriteria(AlurSop.class)
					.add(Restrictions.eq("sop", sop))
					.addOrder(Order.asc("id"))
					.setFlushMode(org.hibernate.FlushMode.MANUAL)
					.list();

			if (alurList == null || alurList.isEmpty()) {
				new Html("<div style='padding:30px 20px;text-align:center;color:#94a3b8;font-size:13px;'>"
						+ "<div style='font-size:32px;margin-bottom:10px;'>📋</div>"
						+ "Belum ada langkah yang terdefinisi untuk SOP ini.</div>")
						.setParent(parent);
				return;
			}

			// ── Scoped CSS (prefix sopdef-) ──
			StringBuilder cssBuf = new StringBuilder();
			cssBuf.append("<style>");
			cssBuf.append(".sopdef-wrap{padding:16px 20px;max-width:700px;margin:0 auto;font-family:Arial,sans-serif;overflow-x:auto;}");
			cssBuf.append(".sopdef-header{padding:10px 14px;border-radius:12px;background:linear-gradient(135deg,#eff6ff,#f0fdf4);");
			cssBuf.append("border:1px solid #bfdbfe;margin-bottom:16px;font-size:12px;color:#1e40af;line-height:1.5;}");
			cssBuf.append(".sopdef-node-wrap{text-align:center;margin:0 0 0 0;}");
			cssBuf.append(".sopdef-pill{padding:7px 26px;border-radius:999px;font-weight:bold;font-size:13px;display:inline-block;}");
			cssBuf.append(".sopdef-arrow{text-align:center;font-size:22px;color:#94a3b8;line-height:1;margin:3px 0;letter-spacing:0;}");
			cssBuf.append(".sopdef-card{border-radius:12px;border:2px solid #cbd5e1;background:#fff;");
			cssBuf.append("margin:0 0 0 0;overflow:hidden;box-shadow:0 2px 8px rgba(15,23,42,0.07);}");
			cssBuf.append(".sopdef-head{padding:8px 12px;display:flex;align-items:center;gap:8px;flex-wrap:wrap;}");
			cssBuf.append(".sopdef-body{padding:8px 12px;background:#f8fafc;font-size:11px;color:#334155;line-height:1.6;}");
			cssBuf.append(".sopdef-badge{border-radius:999px;padding:3px 8px;font-size:10px;font-weight:bold;display:inline-block;margin:1px 2px;}");
			cssBuf.append(".sopdef-num{min-width:24px;width:24px;height:24px;line-height:24px;text-align:center;border-radius:50%;");
			cssBuf.append("background:rgba(255,255,255,0.25);color:#fff;font-size:11px;font-weight:bold;flex-shrink:0;border:1px solid rgba(255,255,255,0.5);}");
			cssBuf.append("@media(max-width:560px){.sopdef-wrap{padding:8px 6px;}.sopdef-header{font-size:11px;}}");
			cssBuf.append("</style>");
			new Html(cssBuf.toString()).setParent(parent);

			// ── Header info SOP ──
			String namaSop = html(sop.getNama());
			String kodeSop = html(sop.getKode());
			int cntAktif = 0;
			int cntNonaktif = 0;
			for (AlurSop a : alurList) {
				if (Boolean.TRUE.equals(a.getAktif())) {
					cntAktif++;
				} else {
					cntNonaktif++;
				}
			}

			StringBuilder htmlBuf = new StringBuilder();
			htmlBuf.append("<div class='sopdef-wrap'>");
			htmlBuf.append("<div class='sopdef-header'><b>").append(namaSop).append("</b>");
			if (kodeSop != null && kodeSop.trim().length() > 0) {
				htmlBuf.append(" <span style='font-weight:normal;color:#64748b;'>— Kode: ").append(kodeSop).append("</span>");
			}
			htmlBuf.append("<div style='margin-top:5px;display:flex;gap:6px;flex-wrap:wrap;'>");
			htmlBuf.append("<span class='sopdef-badge' style='background:#dcfce7;color:#166534;'>")
					.append(cntAktif).append(" Langkah Aktif</span>");
			if (cntNonaktif > 0) {
				htmlBuf.append("<span class='sopdef-badge' style='background:#e5e7eb;color:#374151;'>")
						.append(cntNonaktif).append(" Nonaktif</span>");
			}
			htmlBuf.append("</div></div>");

			// ── Node START ──
			htmlBuf.append("<div class='sopdef-node-wrap'>"
					+ "<span class='sopdef-pill' style='background:#15803d;color:#fff;'>▶ Mulai</span></div>");

			// ── Kartu per langkah ──
			final String[] branchBg = {"#eff6ff", "#f0fdf4", "#fef3c7", "#fdf2f8", "#fff7ed"};
			final String[] branchFg = {"#1d4ed8", "#166534", "#854d0e", "#9333ea", "#9a3412"};

			int nomor = 0;
			for (AlurSop alur : alurList) {
				nomor++;
				boolean aktif = Boolean.TRUE.equals(alur.getAktif());
				boolean isStart = Boolean.TRUE.equals(alur.getStart());

				String headBg = isStart ? "#1e40af" : (aktif ? "#15803d" : "#475569");
				String borderColor = isStart ? "#1e40af" : (aktif ? "#15803d" : "#94a3b8");
				String cardBg = aktif ? "#ffffff" : "#f1f5f9";

				// Panah
				htmlBuf.append("<div class='sopdef-arrow'>↓</div>");

				// Kartu
				htmlBuf.append("<div class='sopdef-card' style='border-color:").append(borderColor)
						.append(";background:").append(cardBg).append(";'>");

				// Header kartu
				htmlBuf.append("<div class='sopdef-head' style='background:").append(headBg).append(";'>");
				htmlBuf.append("<span class='sopdef-num'>").append(nomor).append("</span>");
				htmlBuf.append("<span style='color:#ffffff;font-size:12px;font-weight:bold;flex:1;min-width:0;"
						+ "word-break:break-word;overflow-wrap:break-word;'>");
				if (hasText(alur.getKode())) {
					htmlBuf.append(html(alur.getKode())).append(" — ");
				}
				htmlBuf.append(hasText(alur.getNama()) ? html(alur.getNama()) : "(tanpa nama)").append("</span>");
				if (isStart) {
					htmlBuf.append("<span class='sopdef-badge' style='background:#bfdbfe;color:#1e3a8a;"
							+ "font-size:9px;white-space:nowrap;'>Awal Pengajuan</span>");
				}
				if (!aktif) {
					htmlBuf.append("<span class='sopdef-badge' style='background:#d1d5db;color:#374151;"
							+ "font-size:9px;white-space:nowrap;'>Nonaktif</span>");
				}
				htmlBuf.append("</div>");

				// Body kartu
				htmlBuf.append("<div class='sopdef-body'>");
				if (hasText(alur.getAktor())) {
					htmlBuf.append("<div>👤 <b>").append(html(alur.getAktor())).append("</b></div>");
				}
				if (hasText(alur.getKeterangan())) {
					htmlBuf.append("<div style='color:#64748b;margin-top:2px;'>")
							.append(html(alur.getKeterangan())).append("</div>");
				}

				// Dokumen
				try {
					Set<DokumenAlurSop> dokumens = alur.getDokumenAlurSops();
					if (dokumens != null && !dokumens.isEmpty()) {
						boolean adaDok = false;
						for (DokumenAlurSop dok : dokumens) {
							if (dok != null && Boolean.TRUE.equals(dok.getAktif())) {
								adaDok = true;
								break;
							}
						}
						if (adaDok) {
							htmlBuf.append("<div style='margin-top:5px;'>📎 Dokumen:<ul style='margin:2px 0 0 16px;padding:0;list-style:disc;'>");
							for (DokumenAlurSop dok : dokumens) {
								if (dok == null || !Boolean.TRUE.equals(dok.getAktif())) {
									continue;
								}
								String wBg = Boolean.TRUE.equals(dok.getWajib()) ? "#fee2e2" : "#eff6ff";
								String wFg = Boolean.TRUE.equals(dok.getWajib()) ? "#991b1b" : "#1d4ed8";
								String wTxt = Boolean.TRUE.equals(dok.getWajib()) ? "Wajib" : "Opsional";
								htmlBuf.append("<li style='margin-bottom:2px;'>")
										.append(hasText(dok.getNama()) ? html(dok.getNama()) : "(dokumen)")
										.append(" <span class='sopdef-badge' style='background:").append(wBg)
										.append(";color:").append(wFg).append(";font-size:9px;'>")
										.append(wTxt).append("</span></li>");
							}
							htmlBuf.append("</ul></div>");
						}
					}
				} catch (Exception eDok) { ais.common.ErrorAuditUtil.record(eDok, "auto-audit(empty-catch) src/ais/action/master/sop/TampilanAlurSopAction.java:3627");
					// tolerate lazy-load exception per-step
				}

				// Langkah berikutnya / percabangan
				try {
					List<AlurSop> nexts = alur.ambilAlurSetelahnya();
					List<String> opsiList = alur.ambilOpsiAlurSetelahnya();
					if (nexts != null && nexts.size() == 1 && nexts.get(0) != null) {
						String namaNext = hasText(nexts.get(0).getNama()) ? nexts.get(0).getNama()
								: (hasText(nexts.get(0).getKode()) ? nexts.get(0).getKode() : "langkah berikutnya");
						htmlBuf.append("<div style='margin-top:4px;color:#0369a1;font-size:10px;'>"
								+ "→ Berikutnya: <b>").append(html(namaNext)).append("</b></div>");
					} else if (nexts != null && nexts.size() > 1) {
						htmlBuf.append("<div style='margin-top:5px;font-size:10px;'>🔀 Percabangan:</div>"
								+ "<div style='display:flex;flex-wrap:wrap;gap:4px;margin-top:3px;'>");
						for (int bi = 0; bi < nexts.size(); bi++) {
							AlurSop nx = nexts.get(bi);
							if (nx == null) {
								continue;
							}
							String bg2 = branchBg[bi % branchBg.length];
							String fg2 = branchFg[bi % branchFg.length];
							String opsiTxt = opsiList != null && bi < opsiList.size() ? opsiList.get(bi) : "";
							htmlBuf.append("<span class='sopdef-badge' style='background:").append(bg2)
									.append(";color:").append(fg2).append(";font-size:9px;'>");
							if (hasText(opsiTxt)) {
								htmlBuf.append(html(opsiTxt)).append(": ");
							}
							htmlBuf.append(hasText(nx.getNama()) ? html(nx.getNama()) : html(nx.getKode()))
									.append("</span>");
						}
						htmlBuf.append("</div>");
					}
				} catch (Exception eNext) { ais.common.ErrorAuditUtil.record(eNext, "auto-audit(empty-catch) src/ais/action/master/sop/TampilanAlurSopAction.java:3661");
					// tolerate
				}

				htmlBuf.append("</div></div>"); // close body + card
			}

			// ── Node SELESAI ──
			htmlBuf.append("<div class='sopdef-arrow'>↓</div>");
			htmlBuf.append("<div class='sopdef-node-wrap'>"
					+ "<span class='sopdef-pill' style='background:#dc2626;color:#fff;'>■ Selesai</span></div>");
			htmlBuf.append("</div>"); // close wrap

			new Html(htmlBuf.toString()).setParent(parent);

			alurList.clear();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Merender tabel grid interaktif berisi daftar langkah ({@link AlurSop}) milik SOP
	 * yang diberikan ke dalam komponen induk. Grid ini memungkinkan administrator untuk
	 * meninjau seluruh definisi langkah alur kerja dan mengedit setiap langkah secara
	 * inline melalui form kecil yang muncul di bawah baris yang dipilih.
	 *
	 * <p><b>Kolom grid yang ditampilkan:</b></p>
	 * <ol>
	 *   <li><b>No</b> — Nomor urut langkah.</li>
	 *   <li><b>Kode</b> — Kode unik langkah ({@code AlurSop.kode}).</li>
	 *   <li><b>Nama</b> — Nama deskriptif langkah.</li>
	 *   <li><b>Aktor</b> — Pihak yang bertanggung jawab menyelesaikan langkah ini.</li>
	 *   <li><b>Status</b> — Pill berwarna: "Awal Pengajuan" (biru), "Aktif" (hijau),
	 *       atau "Nonaktif" (abu-abu).</li>
	 *   <li><b>Aksi</b> — Tombol "Edit" yang membuka form inline di bawah baris tersebut
	 *       untuk mengubah kode, nama, aktor, keterangan, dan status aktif.</li>
	 * </ol>
	 *
	 * <p><b>Form Edit Inline:</b></p>
	 * <ul>
	 *   <li>Field yang dapat diubah: Kode, Nama, Aktor, Keterangan, Aktif (checkbox).</li>
	 *   <li>Tombol "Simpan" memanggil {@code Common.refreshUpdate(alurSop)} untuk
	 *       menyimpan perubahan ke database melalui sesi Hibernate yang aktif.</li>
	 *   <li>Tombol "Batal" menutup form tanpa menyimpan.</li>
	 *   <li>Setelah simpan berhasil, baris grid diperbarui dan form ditutup
	 *       sehingga admin dapat langsung melihat hasil perubahannya.</li>
	 * </ul>
	 *
	 * <p><b>Batasan dan catatan penting:</b></p>
	 * <ul>
	 *   <li>Pengurutan langkah menggunakan {@code Order.asc("id")} (urutan input ke DB).
	 *       Bila SOP menggunakan urutan manual, perlu menyesuaikan kriteria pengurutan.</li>
	 *   <li>Pengaturan langkah berikutnya (opsi percabangan) TIDAK dapat diubah dari
	 *       popup ini — untuk mengubah koneksi antar-langkah, admin harus menggunakan
	 *       halaman AlurSopAction yang lebih lengkap.</li>
	 *   <li>Query menggunakan {@code FlushMode.MANUAL} untuk mencegah auto-flush yang
	 *       tidak disengaja selama render.</li>
	 * </ul>
	 *
	 * <p><b>Penanganan session:</b> Menggunakan {@code currentNativeSession()} untuk
	 * baca data (tidak menutup). Operasi simpan menggunakan {@code currentSession()}
	 * dan memanggil {@code Common.refreshUpdate()} yang sudah mengelola transaksi
	 * secara internal.</p>
	 *
	 * <p><b>Kompatibilitas Java 1.7:</b> Anonymous inner class untuk semua listener,
	 * variabel closure bersifat {@code final}. Array digunakan sebagai workaround
	 * satu elemen ({@code final Row[]}) untuk memungkinkan referensi ke variabel
	 * yang dimodifikasi di dalam anonymous class.</p>
	 *
	 * @param parent  komponen ZK tempat grid dilekatkan; bila {@code null} method
	 *                langsung kembali tanpa melakukan apa pun
	 * @param sop     definisi SOP yang langkah-langkahnya akan ditampilkan dalam grid;
	 *                bila {@code null} method langsung kembali
	 */
	@SuppressWarnings("unchecked")
	private static void renderTabelAlurSopUntukPopup(final Component parent, final Sop sop) {
		if (parent == null || sop == null) {
			return;
		}
		final Vbox vboxWrapper = new Vbox();
		vboxWrapper.setWidth("100%");
		vboxWrapper.setStyle("padding:10px 14px;box-sizing:border-box;");
		vboxWrapper.setParent(parent);

		// ── Header info ──
		new Html("<div style='padding:8px 12px;border-radius:10px;background:#f0f9ff;"
				+ "border:1px solid #bae6fd;font-size:12px;color:#0369a1;margin-bottom:10px;line-height:1.5;'>"
				+ "📋 <b>Tabel Langkah Alur SOP</b> — Daftar tahapan yang membentuk proses SOP ini. "
				+ "Klik <b>Edit</b> pada baris untuk mengubah detail langkah secara langsung.</div>")
				.setParent(vboxWrapper);

		// ── Grid ──
		final MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(20);
		grid.setSclass("dgrid fgrid");
		grid.setStyle("border:0;background:transparent;");
		grid.setParent(vboxWrapper);

		Columns cols = new Columns();
		cols.setParent(grid);
		String[] colLabels = {"No", "Kode", "Nama Langkah", "Aktor", "Status", ""};
		String[] colWidths = {"40px", "80px", null, "120px", "100px", "70px"};
		for (int ci = 0; ci < colLabels.length; ci++) {
			MyColumnConfig col = new MyColumnConfig(colLabels[ci]);
			if (colWidths[ci] != null) {
				col.setWidth(colWidths[ci]);
			}
			col.setParent(cols);
		}

		final Rows rowsGrid = new Rows();
		rowsGrid.setParent(grid);

		// ── Muat baris ──
		muatBarisTabelAlurSop(rowsGrid, sop);
	}

	/**
	 * Memuat ulang baris-baris grid langkah {@link AlurSop} untuk SOP yang diberikan.
	 * Method ini dipisah agar dapat dipanggil kembali setelah operasi simpan/batal
	 * sehingga grid selalu menampilkan data terkini tanpa harus menutup dan membuka
	 * ulang popup.
	 *
	 * <p>Setiap baris terdiri dari sel-sel data (nomor, kode, nama, aktor, status)
	 * dan sebuah tombol aksi "Edit" yang saat diklik akan menambahkan baris form
	 * inline langsung di bawah baris data. Form inline memungkinkan pengeditan
	 * cepat tanpa navigasi ke halaman lain.</p>
	 *
	 * <p><b>Alur form inline:</b></p>
	 * <ol>
	 *   <li>Tombol "Edit" diklik → baris form ({@code formRow}) ditambahkan setelah
	 *       baris data, tombol "Edit" dinonaktifkan selama form terbuka.</li>
	 *   <li>Pengguna mengubah nilai di Textbox/Checkbox lalu menekan "Simpan".</li>
	 *   <li>Handler Simpan: memperbarui entitas {@link AlurSop}, memanggil
	 *       {@code Common.refreshUpdate(alurSop)}, menutup form, memanggil
	 *       {@code muatBarisTabelAlurSop} ulang untuk menyegarkan grid.</li>
	 *   <li>Tombol "Batal": mendetach {@code formRow} tanpa menyimpan, mengaktifkan
	 *       kembali tombol "Edit" pada baris terkait.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan session:</b> Operasi baca menggunakan {@code currentNativeSession()}.
	 * Operasi simpan menggunakan {@code currentSession()} melalui
	 * {@code Common.refreshUpdate()} yang mengelola transaksi secara internal.</p>
	 *
	 * <p><b>Kompatibilitas Java 1.7:</b> Anonymous inner class, variabel final,
	 * array satu elemen sebagai wadah referensi mutable di dalam closure.</p>
	 *
	 * @param rowsGrid  komponen {@link Rows} ZK tempat baris data dilekatkan
	 * @param sop       definisi SOP yang langkahnya akan ditampilkan
	 */
	@SuppressWarnings("unchecked")
	private static void muatBarisTabelAlurSop(final Rows rowsGrid, final Sop sop) {
		if (rowsGrid == null || sop == null) {
			return;
		}
		Common.clear(rowsGrid);
		List<AlurSop> list = new ArrayList<AlurSop>();
		try {
			Session sessionT = HibernateUtil.currentNativeSession();
			list = sessionT.createCriteria(AlurSop.class)
					.add(Restrictions.eq("sop", sop))
					.addOrder(Order.asc("id"))
					.setFlushMode(org.hibernate.FlushMode.MANUAL)
					.list();
		} catch (Exception eLoad) {
			Common.tampilErrorJikaAdmin(eLoad);
			return;
		}

		if (list.isEmpty()) {
			Row emptyRow = new Row();
			emptyRow.setParent(rowsGrid);
			org.zkoss.zul.Cell emptyCell = new org.zkoss.zul.Cell();
			emptyCell.setColspan(6);
			emptyCell.setParent(emptyRow);
			new Label(ais.common.Common.getBahasaConfig("Belum ada langkah yang terdefinisi untuk SOP ini.")).setParent(emptyCell);
			return;
		}

		int nomor = 0;
		for (final AlurSop alurSop : list) {
			nomor++;
			final int nomorFinal = nomor;
			boolean aktif = Boolean.TRUE.equals(alurSop.getAktif());
			boolean isStart = Boolean.TRUE.equals(alurSop.getStart());

			final Row dataRow = new Row();
			dataRow.setValign("middle");
			String rowBg = aktif ? "#ffffff" : "#f8fafc";
			dataRow.setStyle("background:" + rowBg + ";border-bottom:1px solid #e5e7eb;");
			dataRow.setParent(rowsGrid);

			// Sel No
			Label lblNo = new Label(String.valueOf(nomorFinal));
			lblNo.setStyle("font-size:12px;color:#64748b;text-align:center;");
			dataRow.appendChild(lblNo);

			// Sel Kode
			Label lblKode = new Label(alurSop.getKode() != null ? alurSop.getKode() : "");
			lblKode.setStyle("font-size:11px;font-weight:bold;color:#1e40af;");
			dataRow.appendChild(lblKode);

			// Sel Nama
			Label lblNama = new Label(alurSop.getNama() != null ? alurSop.getNama() : "");
			lblNama.setStyle("font-size:12px;");
			dataRow.appendChild(lblNama);

			// Sel Aktor
			Label lblAktor = new Label(alurSop.getAktor() != null ? alurSop.getAktor() : "");
			lblAktor.setStyle("font-size:11px;color:#475569;");
			dataRow.appendChild(lblAktor);

			// Sel Status
			String statusBg = isStart ? "#bfdbfe" : (aktif ? "#dcfce7" : "#e5e7eb");
			String statusFg = isStart ? "#1e3a8a" : (aktif ? "#166534" : "#374151");
			String statusTxt = isStart ? "Awal" : (aktif ? "Aktif" : "Nonaktif");
			new Html("<span style='border-radius:999px;padding:3px 8px;font-size:10px;font-weight:bold;"
					+ "background:" + statusBg + ";color:" + statusFg + ";display:inline-block;'>"
					+ statusTxt + "</span>").setParent(dataRow);

			// Sel Aksi — tombol Edit
			final MyToolbarbuttonConfig btnEdit = new MyToolbarbuttonConfig("Edit", "/img/svg/edit.svg");
			btnEdit.setTooltiptext("Edit langkah: " + (alurSop.getNama() != null ? alurSop.getNama() : ""));
			dataRow.appendChild(btnEdit);

			// Tombol Edit → buka Window BARU yang memanggil form Ubah Alur SOP milik AlurSopAction
			// (memuat halaman alur_sop.zul lengkap lalu auto-membuka form edit untuk baris ini),
			// menggantikan form grid inline sebelumnya.
			btnEdit.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bukaWindowEditAlurSop(alurSop);
				}
			});
		}
		if (list != null) {
			list.clear();
		}
	}

	/**
	 * Buka Window BARU berisi halaman {@code AlurSopAction} (alur_sop.zul) yang lengkap, lalu
	 * secara otomatis membukakan form <b>Ubah Alur SOP</b> untuk langkah {@code alurSop} yang
	 * dipilih. Auto-edit dipicu lewat arg {@code autoEditAlurSopId} yang dibaca AlurSopAction
	 * di {@code doAfterCompose}. Dengan cara ini form edit yang tampil adalah form asli milik
	 * AlurSopAction (lengkap: aktor, dokumen, parameter tambahan, percabangan, dst.) dan proses
	 * simpan/refresh-nya tetap berjalan pada komponen yang ter-wire penuh — bukan form grid inline.
	 */
	private static void bukaWindowEditAlurSop(final AlurSop alurSop) {
		if (alurSop == null || alurSop.getId() == null) {
			return;
		}
		try {
			final MyWindow win = new MyWindow(
					"Ubah Alur SOP" + (alurSop.getNama() != null ? ": " + alurSop.getNama() : ""),
					"normal", true);
			win.setWidth("95%");
			win.setHeight("95%");
			win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

			java.util.Map<String, Object> argEdit = new java.util.HashMap<String, Object>();
			argEdit.put("autoEditAlurSopId", String.valueOf(alurSop.getId()));
			org.zkoss.zk.ui.Executions.createComponents(
					"/WEB-INF/z/x/y/pages/master/sop/alur_sop.zul", win, argEdit);

			win.setVisible(true);
			try {
				win.doModal();
			} catch (Exception exModal) {
				win.doOverlapped();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

}
