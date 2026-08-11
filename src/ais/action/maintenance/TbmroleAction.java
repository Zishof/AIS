package ais.action.maintenance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Vbox;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.LabelBahasa;
import ais.database.model.Mahasiswa;
import ais.database.model.Menu;
import ais.database.model.Program;
import ais.database.model.RolePrivilage;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.inventory.Toko;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

public class TbmroleAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyWindow addMenu;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private MyTextbox searchid;
	private Checkbox searchaktif;

	private MyTextbox nama;
	private MyTextbox kode;

	private Tbmrole tbmrole;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;
	private MyCheckboxConfig aktif;
	private MyCheckboxConfig elearning;
	private MyCheckboxConfig kegiatanDanPrestasi;
	private MyCheckboxConfig pustaka;
	private MyCheckboxConfig dashboard;
	private MyCheckboxConfig workflow;
	private MyCheckboxConfig administrasi;
	private MyCheckboxConfig pengadaan;
	private MyCheckboxConfig keuangan;
	private MyCheckboxConfig kinerja;
	private MyCheckboxConfig kantin;
	private MyCheckboxConfig tampilPos;
	private MyCheckboxConfig dashboardKoperasi;

	private MyCheckboxConfig pembayaran;
	private MyCheckboxConfig kalenderAkademik;
	private MyCheckboxConfig infoKegiatan;
	private MyCheckboxConfig melihatDataPegawaiLain;
	private MyCheckboxConfig melihatDataSatkerLain;
	private MyCheckboxConfig presensiKehadiran;
	private MyCheckboxConfig absenLangsung;
	private MyCheckboxConfig akunting;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Sekolah sekolah1;
	private boolean pt;
	private boolean ya;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;
	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox jenisJabatan;
	private MyCheckboxConfig kepegawaian;
	private MyCheckboxConfig melihatSemuaSurat;
	private MyCheckboxConfig melihatSemuaSop;
	private MyCheckboxConfig updateFormatLaporan;
	private MyCheckboxConfig bolehEntryTopup;
	private MyTextbox kodeAkses;
	private MyTextbox satuanKerjas;
	private MyCheckboxConfig mengajukanPengajuanPegawaiLain;
	private MyCheckboxConfig repository;
	private MyCheckboxConfig antarJemput;
	private MyCheckboxConfig spmi;
	private MyCheckboxConfig gaji;
	private MyCheckboxConfig emedic;
	private MyCheckboxConfig aksesFeeder;
	private MyCheckboxConfig aksesSister;
	private Combobox dashboardDefaultMain;
	private MyCheckboxConfig aksesSupervisorKantin;
	private MyCheckboxConfig aksesKasir;
	private MyCheckboxConfig aksesBerandaKantin;
	private MyCheckboxConfig aksesRingkasan;
	private MyCheckboxConfig aksesPesanan;
	private MyCheckboxConfig aksesAnggota;
	private MyCheckboxConfig aksesProduk;
	private MyCheckboxConfig aksesStokOpname;
	private MyCheckboxConfig aksesKulakan;
	private MyCheckboxConfig aksesDiskon;
	private MyCheckboxConfig aksesLaporanTransaksi;
	private MyCheckboxConfig aksesLaporan;
	private MyCheckboxConfig aksesLaporanKeuangan;
	private MyCheckboxConfig aksesRiwayatSinkronisasi;
	private MyCheckboxConfig aksesLogError;
	private MyCheckboxConfig aksesKonfigurasi;
	private MyCheckboxConfig aksesPembayaran;
	private MyCheckboxConfig aksesPedagang;
	private MyCheckboxConfig aksesMeja;
	private MyCheckboxConfig aksesPenyedia;
	private MyCheckboxConfig aksesKasKasir;
	private MyCheckboxConfig aksesSetoranTenant;
	private MyCheckboxConfig aksesJadwalOpname;
	private MyCheckboxConfig aksesStokExpired;
	private MyCheckboxConfig aksesLimitKredit;
	private MyCheckboxConfig aksesMutasiRekening;
	private MyCheckboxConfig aksesProduksi;
	private MyCheckboxConfig aksesPengaturanLaporan;
	private MyCheckboxConfig aksesRiwayatPenjualan;
	private MyCheckboxConfig aksesReturPenjualan;
	private MyCheckboxConfig kantinMemberLandingPage;
	private Map<Long, MyCheckboxConfig> tokoAksesCheckboxMap;
	/**
	 * Checkbox grid Create/Update/Delete/Approve/Reject per menu {@link ais.common.EbisnisMenuKatalog#KUNCI_CRUD}
	 * -- kunci peta {@code "<kunci>_<aksi>"} (mis. {@code "produk_create"}), lihat {@link #appendBarisAksesCrud}.
	 * "Read" TIDAK di sini -- tetap pakai field akses* yg sudah ada (jadi tidak perlu 16 field checkbox baru).
	 */
	private Map<String, MyCheckboxConfig> crudCheckboxMap;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		Tbmuser tbmuser1 = Common.getCurrentUser();

		if (tbmuser1 == null) {
			Common.goLogoff();
			return;
		}

		sekolah1 = SekolahUtil.getSekolah();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class TbmroleRenderer extends ais.ui.util.MyRowRenderer {

		class MenuSelecter {

			private Map<Long, Object[]> checkboxs = new HashMap<Long, Object[]>();
			private MyWindow window;
			private Tbmrole job;
			private MyDiv groupbox;

			public MenuSelecter createInstance(Tbmrole job, MyDiv groupbox) throws Exception {

				// System.out.println("job menu = " + job.getMenus().size());
				// this.window=addMenu;
				MenuSelecter selector = new MenuSelecter();
				selector.window = addMenu;
				selector.job = job;
				selector.groupbox = groupbox;
				selector.init();
				selector.window.setVisible(true);
				selector.window.onModal();
				return selector;
			}

			private MenuSelecter() {

			}

			private Boolean hasChild(Long root, List<Menu> menus) {
				if (root == null || menus == null) {
					return false;
				}
				for (Menu menu : menus) {
					if (menu != null && root.equals(menu.getRoot())) {
						return true;
					}
				}
				return false;
			}

			private void createTreerow(MyTreeitemConfig treeitem, final Menu menu) {
				Treerow treerow = new Treerow();
				new Treecell(menu.getLabel()).setParent(treerow);

				Treecell treecell = new Treecell();
				final MyCheckboxConfig checkbox = new MyCheckboxConfig();
				if (job != null && job.getMenus() != null) {
					for (Menu myMenu : job.getMenus()) {
						if (menu.getId().equals(myMenu.getId())) {
							checkbox.setChecked(true);
							break;
						}
					}
				}
				checkboxs.put(menu == null || menu.getId() == null ? Long.valueOf(System.nanoTime()) : menu.getId(),
						new Object[] { checkbox, menu });
				checkbox.setParent(treecell);
				treecell.setParent(treerow);
				treerow.setParent(treeitem);
			}

			private void createRootSubMenu(Long root, List<Menu> menus, Component componen, int coba) {
				if (coba > 50) {
					return;
				}
				Treechildren tc1 = new Treechildren();
				for (Menu menu : menus) {
					if (menu.getRoot().equals(root)) {
						Boolean ada = hasChild(menu.getChild(), menus);
						if (ada) {
							MyTreeitemConfig treeitem = new MyTreeitemConfig();
							treeitem.setOpen(false);
							treeitem.setValue(menu.getUrl());
							createTreerow(treeitem, menu);
							treeitem.setParent(tc1);
							createRootSubMenu(menu.getChild(), menus, treeitem, ++coba);
						} else {
							MyTreeitemConfig treeitem = new MyTreeitemConfig();
							treeitem.setOpen(false);
							treeitem.setValue(menu.getUrl());
							createTreerow(treeitem, menu);
							treeitem.setParent(tc1);
						}
					}
				}

				tc1.setParent(componen);
			}

			public void createTreeMenu(Tree tree, final List<Menu> menus) {

				Treecols treecols = new Treecols();
				Treecol treecol = new Treecol("Menu");
				treecol.setWidth("90%");
				treecol.setParent(treecols);
				treecol = new Treecol("");
				treecol.setWidth("5%");
				treecol.setParent(treecols);
				treecols.setParent(tree);

				Treechildren tc1 = new Treechildren();
				for (Menu menu : menus) {
					if (menu.getRoot().equals(0L)) {
						MyTreeitemConfig treeitem = new MyTreeitemConfig();
						treeitem.setOpen(false);
						treeitem.setValue(menu.getUrl());
						createTreerow(treeitem, menu);
						treeitem.setParent(tc1);
						createRootSubMenu(menu.getChild(), menus, treeitem, 0);
					}
				}
				tc1.setParent(tree);
			}

			private void save() throws Exception {
				job.setMenus(new HashSet<Menu>());

				for (Object[] objs : this.checkboxs.values()) {
					MyCheckboxConfig combobox = (MyCheckboxConfig) objs[0];
					Menu myMenu = (Menu) objs[1];
					Boolean check = combobox.isChecked();
					System.out.println("menu = " + myMenu.getLabel() + ", check = " + check);
					if (check) {

						job.getMenus().add(myMenu);
					}

				}
				Session session = HibernateUtil.currentSession();
				try {

					Common.refreshUpdate(session, (job));
					ais.common.newui.NewUiCacheInvalidator.invalidateRole(job.getRoleId());

					List<?> list = groupbox.getChildren();
					((Component) list.get(0)).detach();
					((Component) list.get(0)).detach();

					Tree mytree = createTree(job, groupbox);
					mytree.setVisible(true);
					tbmroleActionHelper.createTreeMenu(groupbox, mytree, job);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);

					PesanFormalHelper.tampilkanGagalException("penambahan/penyimpanan data Grup Pengguna (Tbmrole)", e,
							new String[] {
									"Periksa kembali struktur menu yang dipilih, kemungkinan ada menu yang sudah tidak valid/berubah.",
									"Muat ulang halaman lalu coba tambahkan kembali data grup pengguna ini.",
									"Jika kendala berulang, hubungi Administrator Sistem untuk memeriksa relasi menu-grup pengguna terkait." });
				}

				Tbmuser.refreshHakAksesUntukRole(job);

//				Tbmuser.getUserRoleYgDipakai.put(tbmuser.getUserId(), tbmrole);

				window.setVisible(false);
			}

			private void init() {

				Common.clear(window);
				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				Center center = new Center();
				ais.ui.util.ZkCompat.setFlex(center, true);

				Session session = HibernateUtil.currentSession();

				List<Menu> menus = ConstantValues
						.simpleList(
								session.createCriteria(Menu.class).addOrder(Order.asc("nomorUrut"))
										.addOrder(Order.asc("root")).addOrder(Order.asc("child")).add(Restrictions
												.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
								Menu.class);
				Tree tree = new Tree();
				createTreeMenu(tree, menus);
				tree.setParent(center);

				center.setParent(borderlayout);

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				toolbar.setHeight("30px");
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				button.setTooltiptext("Tutup");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						window.setVisible(false);
					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				button.setTooltiptext("Simpan");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						save();
					}

				});
				button.setParent(toolbar);

				toolbar.setParent(south);

				borderlayout.setParent(window);
				// caption.setParent(window);
			}

		}

		class TbmroleActionHelper {

			private Boolean hasChild(Long root, Collection<Menu> menus) {
				for (Menu menu : menus) {
					if (menu.getRoot().equals(root)) {
						return true;
					}
				}
				return false;
			}

			private void createTreerow(final Tree tree, final MyDiv groupbox, MyTreeitemConfig treeitem,
					final Menu menu, final Map<Long, RolePrivilage> mapRolePrivilage, final Tbmrole job, int coba) {
				if (coba > 50) {
					return;
				}
				Treerow treerow = new Treerow();

				if (Common.getApakahAdmin()) {
					final Textbox textboxLabel = new Textbox(Common.getBahasaConfig(job.getRoleId(), menu.getLabel()));
					textboxLabel.setWidth("90%");
					Treecell t = new Treecell();
					t.setParent(treerow);
					t.appendChild(textboxLabel);

					textboxLabel.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							String key = org.apache.commons.lang3.StringUtils.replace(menu.getLabel(), " ", "_")
									.toLowerCase().trim();
							String prefix = job.getRoleId();

							key = org.apache.commons.lang3.StringUtils.replace(key, "/", "").toLowerCase().trim();

							key = org.apache.commons.lang3.StringUtils.replace(key, "&", "_").toLowerCase().trim();

							key = (prefix == null || prefix.trim().isEmpty() ? "" : prefix.trim() + "_") + key;

							Session session = HibernateUtil.currentSession();
							LabelBahasa labelBahasa = (LabelBahasa) session.createCriteria(LabelBahasa.class)
									.add(Restrictions.eq("nama", key)).setMaxResults(1).uniqueResult();

							String currentLang = null;
							try {
								currentLang = (String) Sessions.getCurrent(true).getAttribute("current_lang");
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}

							try {
								String lang = ExecutionsCtrl.getCurrent().getParameter("lang");
								if (lang != null && !lang.trim().isEmpty()) {
									if (lang.trim().equalsIgnoreCase("id")) {
										currentLang = Tbmuser.INDONESIA;
									} else if (lang.trim().equalsIgnoreCase("en")) {
										currentLang = Tbmuser.ENGLISH;
									} else if (lang.trim().equalsIgnoreCase("ar")) {
										currentLang = Tbmuser.ARAB;
									}

									Tbmuser tbmuser = Common.getCurrentUser();
									if (tbmuser != null && currentLang != null && !currentLang.isEmpty()) {
										tbmuser.setBahasa(currentLang);
										if (tbmuser.getMahasiswa() != null) {
											tbmuser.getMahasiswa().setBahasa(currentLang);
										} else if (tbmuser.getSiswa() != null) {
											tbmuser.getSiswa().setBahasa(currentLang);
										} else if (tbmuser.ambilDosen() != null) {
											tbmuser.ambilDosen().setBahasa(currentLang);
										} else if (tbmuser.ambilPegawai() != null) {
											tbmuser.ambilPegawai().setBahasa(currentLang);
										}
									}
								}
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}

							if (currentLang == null) {
								currentLang = Tbmuser.INDONESIA;
							}

							if (labelBahasa != null) {

								if (currentLang.equalsIgnoreCase(Tbmuser.INDONESIA)) {
									labelBahasa.setIndonesia(textboxLabel.getValue().trim());
								} else if (currentLang.equalsIgnoreCase(Tbmuser.ARAB)) {
									labelBahasa.setArab(textboxLabel.getValue().trim());
								} else {
									labelBahasa.setEnglish(textboxLabel.getValue().trim());
								}

								MemoryDbUtil.getBahasaIndonesias().put(key, labelBahasa.getIndonesia());
								MemoryDbUtil.getBahasaEnglishs().put(key, labelBahasa.getEnglish());
								MemoryDbUtil.getBahasaArabs().put(key, labelBahasa.getArab());
							} else {
								MemoryDbUtil.getBahasaIndonesias().put(key, textboxLabel.getValue().trim());
								MemoryDbUtil.getBahasaEnglishs().put(key, textboxLabel.getValue().trim());
								MemoryDbUtil.getBahasaArabs().put(key, textboxLabel.getValue().trim());

								labelBahasa = new LabelBahasa();
								labelBahasa.setNama(key);
								labelBahasa.setIndonesia(textboxLabel.getValue().trim());
								labelBahasa.setEnglish(textboxLabel.getValue().trim());
								labelBahasa.setArab(textboxLabel.getValue().trim());
								Common.refreshSaveOrUpdate(session, labelBahasa);

							}
						}
					});
				} else {
					Treecell t = new Treecell(Common.getBahasaConfig(job.getRoleId(), menu.getLabel()));
					t.setParent(treerow);
				}

				final MyCheckboxConfig all = new MyCheckboxConfig();
				final MyCheckboxConfig approve = new MyCheckboxConfig();
				final MyCheckboxConfig reject = new MyCheckboxConfig();
				final MyCheckboxConfig update = new MyCheckboxConfig();
				final MyCheckboxConfig read = new MyCheckboxConfig();
				final MyCheckboxConfig delete = new MyCheckboxConfig();
				final MyCheckboxConfig create = new MyCheckboxConfig();

				RolePrivilage roles = mapRolePrivilage.get(menu.getId());

				Treecell treecell0 = new Treecell();
				approve.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						RolePrivilage rolePrivilage = mapRolePrivilage.get(menu.getId());

						if (rolePrivilage == null) {
							rolePrivilage = new RolePrivilage();
							rolePrivilage.setCreate(0);
							rolePrivilage.setDelete(0);
							rolePrivilage.setUpdate(0);
							rolePrivilage.setApprove(0);
							rolePrivilage.setReject(0);
						} else {
							rolePrivilage.setRole(job);
							rolePrivilage.setMenu(menu);
							if (approve.isChecked()) {
								rolePrivilage.setApprove(1);
								if (rolePrivilage.getRead().equals(1) && rolePrivilage.getCreate().equals(1)
										&& rolePrivilage.getApprove().equals(1) && rolePrivilage.getReject().equals(1)
										&& rolePrivilage.getUpdate().equals(1) && rolePrivilage.getDelete().equals(1)) {
									all.setChecked(true);
								}
							} else {
								rolePrivilage.setApprove(0);
								all.setChecked(false);
							}

						}
						Common.refreshSaveOrUpdate(rolePrivilage);
						mapRolePrivilage.put(menu.getId(), rolePrivilage);
					}

				});

				approve.setParent(treecell0);
				treecell0.setParent(treerow);

				Treecell treecell1 = new Treecell();
				reject.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						// TODO Auto-generated method stub
						RolePrivilage rolePrivilage = mapRolePrivilage.get(menu.getId());

						if (rolePrivilage == null) {
							rolePrivilage = new RolePrivilage();
							rolePrivilage.setCreate(0);
							rolePrivilage.setDelete(0);
							rolePrivilage.setUpdate(0);
							rolePrivilage.setApprove(0);
							rolePrivilage.setReject(0);
						} else {
							rolePrivilage.setRole(job);
							rolePrivilage.setMenu(menu);
							if (reject.isChecked()) {
								rolePrivilage.setReject(1);
								if (rolePrivilage.getRead().equals(1) && rolePrivilage.getCreate().equals(1)
										&& rolePrivilage.getApprove().equals(1) && rolePrivilage.getReject().equals(1)
										&& rolePrivilage.getUpdate().equals(1) && rolePrivilage.getDelete().equals(1)) {
									all.setChecked(true);
								}
							} else {
								rolePrivilage.setReject(0);
								all.setChecked(false);
							}

						}
						Common.refreshSaveOrUpdate(rolePrivilage);
						mapRolePrivilage.put(menu.getId(), rolePrivilage);
					}

				});

				reject.setParent(treecell1);
				treecell1.setParent(treerow);

				Treecell treecell = new Treecell();
				read.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						RolePrivilage rolePrivilage = mapRolePrivilage.get(menu.getId());

						if (rolePrivilage == null) {
							rolePrivilage = new RolePrivilage();
							rolePrivilage.setCreate(0);
							rolePrivilage.setDelete(0);
							rolePrivilage.setUpdate(0);
							rolePrivilage.setApprove(0);
							rolePrivilage.setReject(0);
						} else {
							rolePrivilage.setRole(job);
							rolePrivilage.setMenu(menu);
							if (read.isChecked()) {
								rolePrivilage.setRead(1);
								if (rolePrivilage.getRead().equals(1) && rolePrivilage.getCreate().equals(1)
										&& rolePrivilage.getApprove().equals(1) && rolePrivilage.getReject().equals(1)
										&& rolePrivilage.getUpdate().equals(1) && rolePrivilage.getDelete().equals(1)) {
									all.setChecked(true);
								}
							} else {
								rolePrivilage.setRead(0);
								all.setChecked(false);
							}

						}
						Common.refreshSaveOrUpdate(rolePrivilage);
						mapRolePrivilage.put(menu.getId(), rolePrivilage);
					}

				});

				read.setParent(treecell);
				treecell.setParent(treerow);

				// new Treecell("Update").setParent(treerow);
				Treecell treecell2 = new Treecell();
				update.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						RolePrivilage rolePrivilage = mapRolePrivilage.get(menu.getId());

						if (rolePrivilage == null) {
							rolePrivilage = new RolePrivilage();
							rolePrivilage.setRead(0);
							rolePrivilage.setDelete(0);
							rolePrivilage.setCreate(0);
							rolePrivilage.setApprove(0);
							rolePrivilage.setReject(0);
						} else {
							rolePrivilage.setRole(job);
							rolePrivilage.setMenu(menu);
							if (update.isChecked()) {
								rolePrivilage.setUpdate(1);
								if (rolePrivilage.getRead().equals(1) && rolePrivilage.getCreate().equals(1)
										&& rolePrivilage.getUpdate().equals(1) && rolePrivilage.getApprove().equals(1)
										&& rolePrivilage.getReject().equals(1) && rolePrivilage.getDelete().equals(1)) {
									all.setChecked(true);
								}

							} else {
								rolePrivilage.setUpdate(0);
								all.setChecked(false);
							}

						}
						Common.refreshSaveOrUpdate(rolePrivilage);
						mapRolePrivilage.put(menu.getId(), rolePrivilage);
					}
				});

				update.setParent(treecell2);
				treecell2.setParent(treerow);

				// new Treecell("Create").setParent(treerow);
				Treecell treecell3 = new Treecell();

				create.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						RolePrivilage rolePrivilage = mapRolePrivilage.get(menu.getId());

						if (rolePrivilage == null) {
							rolePrivilage = new RolePrivilage();
							rolePrivilage.setRead(0);
							rolePrivilage.setDelete(0);
							rolePrivilage.setUpdate(0);
							rolePrivilage.setApprove(0);
							rolePrivilage.setReject(0);
						} else {
							rolePrivilage.setRole(job);
							rolePrivilage.setMenu(menu);
							// rolePrivilage.setCreate(create.isChecked()?1:0);

							if (create.isChecked()) {
								rolePrivilage.setCreate(1);
								if (rolePrivilage.getRead().equals(1) && rolePrivilage.getCreate().equals(1)
										&& rolePrivilage.getUpdate().equals(1) && rolePrivilage.getApprove().equals(1)
										&& rolePrivilage.getReject().equals(1) && rolePrivilage.getDelete().equals(1)) {
									all.setChecked(true);
								}

							} else {
								rolePrivilage.setCreate(0);
								all.setChecked(false);
							}

						}
						Common.refreshSaveOrUpdate(rolePrivilage);
						mapRolePrivilage.put(menu.getId(), rolePrivilage);
					}
				});
				create.setParent(treecell3);
				treecell3.setParent(treerow);

				// new Treecell("Delete").setParent(treerow);
				Treecell treecell4 = new Treecell();

				delete.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						RolePrivilage rolePrivilage = mapRolePrivilage.get(menu.getId());

						if (rolePrivilage == null) {
							rolePrivilage = new RolePrivilage();
							rolePrivilage.setRead(0);
							rolePrivilage.setCreate(0);
							rolePrivilage.setUpdate(0);
							rolePrivilage.setApprove(0);
							rolePrivilage.setReject(0);
						} else {
							rolePrivilage.setRole(job);
							rolePrivilage.setMenu(menu);
							// rolePrivilage.setDelete(delete.isChecked()?1:0);
							if (delete.isChecked()) {
								rolePrivilage.setDelete(1);
								if (rolePrivilage.getRead().equals(1) && rolePrivilage.getCreate().equals(1)
										&& rolePrivilage.getUpdate().equals(1) && rolePrivilage.getApprove().equals(1)
										&& rolePrivilage.getReject().equals(1) && rolePrivilage.getDelete().equals(1)) {
									all.setChecked(true);
								}

							} else {
								rolePrivilage.setDelete(0);
								all.setChecked(false);
							}

						}

						Common.refreshSaveOrUpdate(rolePrivilage);
						mapRolePrivilage.put(menu.getId(), rolePrivilage);
					}
				});

				delete.setParent(treecell4);
				treecell4.setParent(treerow);
				// TODO: end
				Treecell treecell6 = new Treecell();

				all.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						RolePrivilage rolePrivilage = mapRolePrivilage.get(menu.getId());

						if (rolePrivilage == null) {
							rolePrivilage = new RolePrivilage();
							rolePrivilage.setCreate(1);
							rolePrivilage.setRead(1);
							rolePrivilage.setUpdate(1);
							rolePrivilage.setDelete(1);
							rolePrivilage.setApprove(1);
							rolePrivilage.setReject(1);
						} else {
							rolePrivilage.setRole(job);
							rolePrivilage.setMenu(menu);

							rolePrivilage.setRead(all.isChecked() ? 1 : 0);
							read.setChecked(true);
							rolePrivilage.setCreate(all.isChecked() ? 1 : 0);
							create.setChecked(true);
							rolePrivilage.setUpdate(all.isChecked() ? 1 : 0);
							update.setChecked(true);
							rolePrivilage.setDelete(all.isChecked() ? 1 : 0);
							approve.setChecked(true);
							rolePrivilage.setApprove(all.isChecked() ? 1 : 0);
							reject.setChecked(true);
							rolePrivilage.setReject(all.isChecked() ? 1 : 0);

							if (all.isChecked()) {
								rolePrivilage.setRead(1);
								read.setChecked(true);
								rolePrivilage.setCreate(1);
								create.setChecked(true);
								rolePrivilage.setUpdate(1);
								update.setChecked(true);
								rolePrivilage.setDelete(1);
								delete.setChecked(true);
								rolePrivilage.setApprove(1);
								approve.setChecked(true);
								rolePrivilage.setReject(1);
								reject.setChecked(true);
							} else {
								read.setChecked(false);
								rolePrivilage.setRead(0);
								create.setChecked(false);
								rolePrivilage.setCreate(0);
								update.setChecked(false);
								rolePrivilage.setUpdate(0);
								delete.setChecked(false);
								rolePrivilage.setDelete(0);
								rolePrivilage.setApprove(0);
								approve.setChecked(false);
								rolePrivilage.setReject(0);
								reject.setChecked(false);
							}

						}
						Common.refreshSaveOrUpdate(rolePrivilage);
						mapRolePrivilage.put(menu.getId(), rolePrivilage);
					}
				});
				all.setParent(treecell6);
				treecell6.setParent(treerow);

				if (roles != null && roles.getRead().equals(1) && roles.getCreate().equals(1)
						&& roles.getUpdate().equals(1) && roles.getDelete().equals(1)) {
					all.setChecked(true);
				} else {
					all.setChecked(false);
				}

				if (roles != null && roles.getRead().equals(1)) {
					read.setChecked(true);
				} else {
					read.setChecked(false);
				}
				if (roles != null && roles.getUpdate().equals(1)) {
					update.setChecked(true);
				} else {
					update.setChecked(false);
				}
				if (roles != null && roles.getCreate().equals(1)) {
					create.setChecked(true);
				} else {
					create.setChecked(false);
				}
				if (roles != null && roles.getDelete().equals(1)) {
					delete.setChecked(true);
				} else {
					delete.setChecked(false);
				}

				if (roles != null && roles.getApprove().equals(1)) {
					approve.setChecked(true);
				} else {
					approve.setChecked(false);
				}

				if (roles != null && roles.getReject().equals(1)) {
					reject.setChecked(true);
				} else {
					reject.setChecked(false);
				}

				Treecell treecell5 = new Treecell();
				Toolbar toolbar = new Toolbar();
				toolbar.setHeight("30px");
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						// TODO Auto-generated method stub
						MyMessageboxConfig.show("Apakah anda ingin menghapus menu ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											job.getMenus().remove(menu);

											try {

												Common.refreshUpdate(job);

												List<?> list = groupbox.getChildren();
												((Component) list.get(0)).detach();
												((Component) list.get(0)).detach();

												final Tree mytree = createTree(job, groupbox);
												mytree.setVisible(true);
												createTreeMenu(groupbox, mytree, job);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);

												PesanFormalHelper.tampilkanGagalException("perubahan hak akses menu pada Grup Pengguna (Tbmrole)", e,
														new String[] {
																"Muat ulang halaman ini, kemungkinan struktur menu grup pengguna berubah pada saat bersamaan.",
																"Coba ulangi proses hapus/ubah menu pada grup pengguna ini beberapa saat lagi.",
																"Jika kendala berulang, hubungi Administrator Sistem untuk memeriksa data menu terkait." });
											}

										}

									}
								});

					}

				});
				button.setParent(toolbar);

				toolbar.setParent(treecell5);
				treecell5.setParent(treerow);
				treerow.setParent(treeitem);

				// Edited by Fauzi

				if (roles == null) {
					roles = new RolePrivilage();
					roles.setCreate(0);
					roles.setDelete(0);
					roles.setRead(0);
					roles.setUpdate(0);
					roles.setReject(0);
					roles.setApprove(0);
					roles.setMenu(menu);
					roles.setRole(job);

					mapRolePrivilage.put(menu.getId(), roles);
				}

			}

			private void createTreerowParent(final Tree tree, final MyDiv groupbox, MyTreeitemConfig treeitem,
					final Menu menu, final Tbmrole job) {
				Treerow treerow = new Treerow();
				// MyToolbarbuttonConfig button = new MyToolbarbuttonConfig();

				if (Common.getApakahAdmin()) {
					final Textbox textboxLabel = new Textbox(Common.getBahasaConfig(job.getRoleId(), menu.getLabel()));
					textboxLabel.setWidth("90%");
					Treecell t = new Treecell();
					t.setParent(treerow);
					t.appendChild(textboxLabel);

					textboxLabel.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							String key = org.apache.commons.lang3.StringUtils.replace(menu.getLabel(), " ", "_")
									.toLowerCase().trim();
							String prefix = job.getRoleId();

							key = org.apache.commons.lang3.StringUtils.replace(key, "/", "").toLowerCase().trim();

							key = org.apache.commons.lang3.StringUtils.replace(key, "&", "_").toLowerCase().trim();

							key = (prefix == null || prefix.trim().isEmpty() ? "" : prefix.trim() + "_") + key;

							Session session = HibernateUtil.currentSession();
							LabelBahasa labelBahasa = (LabelBahasa) session.createCriteria(LabelBahasa.class)
									.add(Restrictions.eq("nama", key)).setMaxResults(1).uniqueResult();

							String currentLang = null;
							try {
								currentLang = (String) Sessions.getCurrent(true).getAttribute("current_lang");
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}

							try {
								String lang = ExecutionsCtrl.getCurrent().getParameter("lang");
								if (lang != null && !lang.trim().isEmpty()) {
									if (lang.trim().equalsIgnoreCase("id")) {
										currentLang = Tbmuser.INDONESIA;
									} else if (lang.trim().equalsIgnoreCase("en")) {
										currentLang = Tbmuser.ENGLISH;
									} else if (lang.trim().equalsIgnoreCase("ar")) {
										currentLang = Tbmuser.ARAB;
									}

									Tbmuser tbmuser = Common.getCurrentUser();
									if (tbmuser != null && currentLang != null && !currentLang.isEmpty()) {
										tbmuser.setBahasa(currentLang);
										if (tbmuser.getMahasiswa() != null) {
											tbmuser.getMahasiswa().setBahasa(currentLang);
										} else if (tbmuser.getSiswa() != null) {
											tbmuser.getSiswa().setBahasa(currentLang);
										} else if (tbmuser.ambilDosen() != null) {
											tbmuser.ambilDosen().setBahasa(currentLang);
										} else if (tbmuser.ambilPegawai() != null) {
											tbmuser.ambilPegawai().setBahasa(currentLang);
										}
									}
								}
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}

							if (currentLang == null) {
								currentLang = Tbmuser.INDONESIA;
							}

							if (labelBahasa != null) {

								if (currentLang.equalsIgnoreCase(Tbmuser.INDONESIA)) {
									labelBahasa.setIndonesia(textboxLabel.getValue().trim());
								} else if (currentLang.equalsIgnoreCase(Tbmuser.ARAB)) {
									labelBahasa.setArab(textboxLabel.getValue().trim());
								} else {
									labelBahasa.setEnglish(textboxLabel.getValue().trim());
								}

								MemoryDbUtil.getBahasaIndonesias().put(key, labelBahasa.getIndonesia());
								MemoryDbUtil.getBahasaEnglishs().put(key, labelBahasa.getEnglish());
								MemoryDbUtil.getBahasaArabs().put(key, labelBahasa.getArab());
							} else {
								MemoryDbUtil.getBahasaIndonesias().put(key, textboxLabel.getValue().trim());
								MemoryDbUtil.getBahasaEnglishs().put(key, textboxLabel.getValue().trim());
								MemoryDbUtil.getBahasaArabs().put(key, textboxLabel.getValue().trim());

								labelBahasa = new LabelBahasa();
								labelBahasa.setNama(key);
								labelBahasa.setIndonesia(textboxLabel.getValue().trim());
								labelBahasa.setEnglish(textboxLabel.getValue().trim());
								labelBahasa.setArab(textboxLabel.getValue().trim());
								Common.refreshSaveOrUpdate(session, labelBahasa);

							}
						}
					});
				} else {
					Treecell t = new Treecell(Common.getBahasaConfig(job.getRoleId(), menu.getLabel()));
					t.setParent(treerow);
				}
				final Label all = new Label();
				final Label approve = new Label();
				final Label reject = new Label();
				final Label update = new Label();
				final Label read = new Label();
				final Label delete = new Label();
				final Label create = new Label();

				Treecell treecell0 = new Treecell();

				approve.setParent(treecell0);
				treecell0.setParent(treerow);

				Treecell treecell1 = new Treecell();

				reject.setParent(treecell1);
				treecell1.setParent(treerow);

				Treecell treecell = new Treecell();

				read.setParent(treecell);
				treecell.setParent(treerow);

				// new Treecell("Update").setParent(treerow);
				Treecell treecell2 = new Treecell();

				update.setParent(treecell2);
				treecell2.setParent(treerow);

				// new Treecell("Create").setParent(treerow);
				Treecell treecell3 = new Treecell();

				create.setParent(treecell3);
				treecell3.setParent(treerow);

				// new Treecell("Delete").setParent(treerow);
				Treecell treecell4 = new Treecell();

				delete.setParent(treecell4);
				treecell4.setParent(treerow);
				// TODO: end
				Treecell treecell6 = new Treecell();

				all.setParent(treecell6);
				treecell6.setParent(treerow);

				Treecell treecell5 = new Treecell();
				Toolbar toolbar = new Toolbar();
				toolbar.setHeight("30px");
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						// TODO Auto-generated method stub
						MyMessageboxConfig.show("Apakah anda ingin menghapus menu ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											job.getMenus().remove(menu);

											try {

												Common.refreshUpdate(job);

												List<?> list = groupbox.getChildren();
												((Component) list.get(0)).detach();
												((Component) list.get(0)).detach();

												final Tree mytree = createTree(job, groupbox);
												mytree.setVisible(true);
												createTreeMenu(groupbox, mytree, job);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);

												PesanFormalHelper.tampilkanGagalException("perubahan hak akses menu pada Grup Pengguna (Tbmrole)", e,
														new String[] {
																"Muat ulang halaman ini, kemungkinan struktur menu grup pengguna berubah pada saat bersamaan.",
																"Coba ulangi proses hapus/ubah menu pada grup pengguna ini beberapa saat lagi.",
																"Jika kendala berulang, hubungi Administrator Sistem untuk memeriksa data menu terkait." });
											}

										}

									}
								});

					}

				});
				button.setParent(toolbar);
				toolbar.setParent(treecell5);
				treecell5.setParent(treerow);
				treerow.setParent(treeitem);

			}

			private void createRootSubMenu(Tree tree, final MyDiv groupbox, Long root, Collection<Menu> menus,
					Component componen, Map<Long, RolePrivilage> mapRolePrivilage, Tbmrole job, Set<Long> check,
					int coba) {
				if (coba > 50) {
					return;
				}
				Treechildren tc1 = new Treechildren();
				for (Menu menu : menus) {
					if (menu.getRoot().equals(root) && !check.contains(menu.getId())) {
						check.add(menu.getId());
						Boolean ada = hasChild(menu.getChild(), menus);
						if (ada) {
							MyTreeitemConfig treeitem = new MyTreeitemConfig();
							treeitem.setOpen(false);
							treeitem.setValue(menu.getUrl());
							createTreerowParent(tree, groupbox, treeitem, menu, job);
							treeitem.setParent(tc1);
							createRootSubMenu(tree, groupbox, menu.getChild(), menus, treeitem, mapRolePrivilage, job,
									check, ++coba);
						} else {
							MyTreeitemConfig treeitem = new MyTreeitemConfig();
							treeitem.setOpen(false);
							treeitem.setValue(menu.getUrl());
							createTreerow(tree, groupbox, treeitem, menu, mapRolePrivilage, job, ++coba);
							treeitem.setParent(tc1);
						}
					}
				}

				tc1.setParent(componen);
			}

			public void createTreeMenu(final MyDiv groupbox, Tree tree, Tbmrole job) {
				Set<Long> check = new HashSet<Long>();
				Treecols treecols = new Treecols();
				Treecol treecol = new Treecol("");
				treecol.setWidth("65%");
				treecol.setParent(treecols);
				treecol = new Treecol("Approve");
				treecol.setWidth("5%");
				treecol.setParent(treecols);
				treecol = new Treecol("Reject");
				treecol.setWidth("5%");
				treecol.setParent(treecols);
				treecol = new Treecol("Read");
				treecol.setWidth("5%");
				treecol.setParent(treecols);
				treecol = new Treecol("Update");
				treecol.setWidth("5%");
				treecol.setParent(treecols);
				treecol = new Treecol("Create");
				treecol.setWidth("5%");
				treecol.setParent(treecols);
				treecol = new Treecol("Delete");
				treecol.setWidth("5%");
				treecol.setParent(treecols);
				treecol = new Treecol("Check All");
				treecol.setWidth("5%");
				treecol.setParent(treecols);
				treecol = new Treecol("");
				treecol.setWidth("0px");
				treecol.setParent(treecols);
				treecols.setParent(tree);

				Set<Menu> menusa = job.getMenus();
				List<Long> ids = new ArrayList<Long>();
				for (Menu m : menusa) {
					ids.add(m.getId());
				}
				Session session = HibernateUtil.currentSession();

				List<Menu> menus = ConstantValues
						.simpleList(
								session.createCriteria(Menu.class)
										.add(ids.isEmpty() ? Restrictions.sqlRestriction("false")
												: Restrictions.in("id", ids))
										.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("root"))
										.addOrder(Order.asc("child")).add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true))),
								Menu.class);

				List<RolePrivilage> rolePrivilages = ConstantValues
						.simpleList(session.createCriteria(RolePrivilage.class).add(Restrictions.isNotNull("menu"))
								.add(Restrictions.eq("role", job)), RolePrivilage.class);
				Map<Long, RolePrivilage> mapRolePrivilage = new HashMap<Long, RolePrivilage>();
				for (RolePrivilage rolePrivilage : rolePrivilages) {
					mapRolePrivilage.put(rolePrivilage.getMenu().getId(), rolePrivilage);
				}

				System.out.println("menus -> " + menus.size() + ", rolePrivilages -> " + rolePrivilages.size());

				Treechildren tc1 = new Treechildren();
				for (Menu menu : menus) {
					if (menu.getRoot().equals(0L) && !check.contains(menu.getId())) {
						check.add(menu.getId());
						System.out.println("menu = " + menu.getLabel());
						MyTreeitemConfig treeitem = new MyTreeitemConfig();
						treeitem.setOpen(false);
						treeitem.setValue(menu.getUrl());
						Boolean ada = hasChild(menu.getChild(), menus);
						if (ada) {
							createTreerowParent(tree, groupbox, treeitem, menu, job);
						} else {
							createTreerow(tree, groupbox, treeitem, menu, mapRolePrivilage, job, 0);
						}
						treeitem.setParent(tc1);
						createRootSubMenu(tree, groupbox, menu.getChild(), menus, treeitem, mapRolePrivilage, job,
								check, 0);
					}
				}
				tc1.setParent(tree);
			}

		}

		private MenuSelecter menuSelecter = new MenuSelecter();
		private TbmroleActionHelper tbmroleActionHelper = new TbmroleActionHelper();

		private boolean mulaiUploadMenu = false;

		public Tree createTree(final Tbmrole job, final MyDiv groupbox) {
			final Tree tree = new Tree();

			tree.setId("tree_" + (Math.random() * 10000000.0));
			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("30px");

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah Struktur Menu", "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					menuSelecter.createInstance(job, groupbox);

				}

			});

			button.setParent(toolbar);

			List<String> columnHeadersAdding = new ArrayList<String>();
			columnHeadersAdding.add("read");
			columnHeadersAdding.add("update");
			columnHeadersAdding.add("create");
			columnHeadersAdding.add("delete");
			columnHeadersAdding.add("approve");
			columnHeadersAdding.add("reject");
			final String[] contents = new String[] { "id", "label" };
			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					Menu menu = (Menu) objects[0];

					XSSFRow row = (XSSFRow) objects[2];

					Session session = HibernateUtil.currentNativeSession();

					RolePrivilage rolePrivilage = (RolePrivilage) ConstantValues
							.simpleObject(session.createCriteria(RolePrivilage.class).add(Restrictions.eq("role", job))
									.add(Restrictions.eq("menu", menu)).setMaxResults(1), RolePrivilage.class);

					row.createCell(contents.length + 0)
							.setCellValue(rolePrivilage == null ? 0 : rolePrivilage.getRead());
					row.createCell(contents.length + 1)
							.setCellValue(rolePrivilage == null ? 0 : rolePrivilage.getUpdate());
					row.createCell(contents.length + 2)
							.setCellValue(rolePrivilage == null ? 0 : rolePrivilage.getCreate());
					row.createCell(contents.length + 3)
							.setCellValue(rolePrivilage == null ? 0 : rolePrivilage.getDelete());
					row.createCell(contents.length + 4)
							.setCellValue(rolePrivilage == null ? 0 : rolePrivilage.getApprove());
					row.createCell(contents.length + 5)
							.setCellValue(rolePrivilage == null ? 0 : rolePrivilage.getReject());

					// session.disconnect();
					ais.common.Common.closeOpenedSession(session);
				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Menu.class, new DataCriteria() {

				@Override
				public Object initCriteria(boolean order) {
					Session session = HibernateUtil.currentSession();
					session.refresh(job);
					List<Menu> menus = new ArrayList<Menu>(job.getMenus());
					return menus;
				}
			}, "Download Menu", "/img/print.png", columnHeadersAdding, dataAdding, contents);
			toolbar.appendChild(cetakToolbarbutton);

			MyToolbarbuttonConfig upload = Common.uploadData(TbmroleAction.this, Menu.class, new EventListener() {

				@SuppressWarnings("rawtypes")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] data = (Object[]) arg0.getData();
					Menu menu = (Menu) data[0];
					Session session = (Session) data[1];
					Map datum = (Map) data[2];
					if (menu != null && menu.getId() != null) {
						if (!mulaiUploadMenu) {
							session.refresh(job);
							mulaiUploadMenu = true;
						}
						Set<Menu> menus = job.getMenus();
						menus.add(menu);
						job.setMenus(menus);

						System.out.println("datum -> " + datum);

						Object read = 0;
						Object update = 0;
						Object create = 0;
						Object delete = 0;
						Object approve = 0;
						Object reject = 0;

						for (Object key : datum.keySet()) {
							if (key.toString().trim().equalsIgnoreCase("READ")) {
								read = datum.get(key);
							} else if (key.toString().trim().equalsIgnoreCase("UPDATE")) {
								update = datum.get(key);
							} else if (key.toString().trim().equalsIgnoreCase("CREATE")) {
								create = datum.get(key);
							} else if (key.toString().trim().equalsIgnoreCase("Delete")) {
								delete = datum.get(key);
							} else if (key.toString().trim().equalsIgnoreCase("APPROVE")) {
								approve = datum.get(key);
							} else if (key.toString().trim().equalsIgnoreCase("REJECT")) {
								reject = datum.get(key);
							}
						}

						RolePrivilage rolePrivilage = (RolePrivilage) ConstantValues
								.simpleObject(
										session.createCriteria(RolePrivilage.class).add(Restrictions.eq("role", job))
												.add(Restrictions.eq("menu", menu)).setMaxResults(1),
										RolePrivilage.class);

						System.out.println("1. rolePrivilage -> " + rolePrivilage);

						if (rolePrivilage == null) {
							rolePrivilage = new RolePrivilage();
						}
						rolePrivilage.setRole(job);
						rolePrivilage.setMenu(menu);
						rolePrivilage
								.setRead(read == null || !Common.isNumber(read + "") ? 0 : Integer.parseInt(read + ""));
						rolePrivilage.setUpdate(
								update == null || !Common.isNumber(update + "") ? 0 : Integer.parseInt(update + ""));
						rolePrivilage.setCreate(
								create == null || !Common.isNumber(create + "") ? 0 : Integer.parseInt(create + ""));
						rolePrivilage.setDelete(
								delete == null || !Common.isNumber(delete + "") ? 0 : Integer.parseInt(delete + ""));
						rolePrivilage.setApprove(
								approve == null || !Common.isNumber(approve + "") ? 0 : Integer.parseInt(approve + ""));
						rolePrivilage.setReject(
								reject == null || !Common.isNumber(reject + "") ? 0 : Integer.parseInt(reject + ""));

						session.getTransaction().begin();
						if (rolePrivilage.getId() == null) {
							session.save(rolePrivilage);
						} else {
							Common.refreshUpdate(session, rolePrivilage);
						}
						session.getTransaction().commit();
						ais.common.newui.NewUiCacheInvalidator.invalidateRole(job.getRoleId());

						System.out.println("2. rolePrivilage -> " + rolePrivilage);
					}
				}

			}, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					mulaiUploadMenu = false;
				}

			}, contents);
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
			toolbar.appendChild(upload);

			button = new MyToolbarbuttonConfig("Reset Menu", "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Session session = HibernateUtil.currentSession();
					List<RolePrivilage> rolePrivilages = ConstantValues.simpleList(
							session.createCriteria(RolePrivilage.class).add(Restrictions.eq("role", job)),
							RolePrivilage.class);
					session.refresh(job);
					for (RolePrivilage rolePrivilage : rolePrivilages) {
						Set<Menu> menus = job.getMenus();
						menus.add(rolePrivilage.getMenu());
						job.setMenus(menus);
					}
					Common.refreshUpdate(session, job);
					ais.common.newui.NewUiCacheInvalidator.invalidateRole(job.getRoleId());

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
				}

			});

			button.setParent(toolbar);

			toolbar.setParent(groupbox);
			tree.setParent(groupbox);
			tree.setVisible(true);
			return tree;
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Tbmrole job = (Tbmrole) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			detail.addEventListener("onOpen", new EventListener() {
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Common.clear(detail);
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						groupbox.setWidth("95%");

						Tbmrole myjob = (Tbmrole) HibernateUtil.currentSession().load(Tbmrole.class, job.getRoleId());

						Tree tree = createTree(myjob, groupbox);
						tbmroleActionHelper.createTreeMenu(groupbox, tree, myjob);
						groupbox.setParent(detail);
					}
				}
			});

			new Label(job.getRoleId()).setParent(arg0);
			new Label(job.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Tbmrole.class, job, job.getRoleName()).setParent(arg0);

			new Label(job.getSatuanKerja() == null ? "" : job.getSatuanKerja().getNama()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			if (job.getFakultas() != null)
				new Label(job.getFakultas().getNama()).setParent(vbox);
			if (job.getJurusan() != null)
				new Label(job.getJurusan().getNama()).setParent(vbox);

			if (job.getYayasan() != null)
				new Label(job.getYayasan().getNama()).setParent(vbox);
			if (job.getSekolah() != null)
				new Label(job.getSekolah().getNama()).setParent(vbox);

			int qty;
			if (job.getRoleId() != null && job.getRoleId().equals(Tbmrole.MAHASISWA)) {
				qty = ((Number) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			} else if (job.getRoleId() != null && job.getRoleId().equals(Tbmrole.SISWA)) {
				qty = ((Number) HibernateUtil.currentSession().createCriteria(Siswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			} else {
				qty = ((Number) HibernateUtil.currentSession().createCriteria(Tbmuser.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions
								.or(Restrictions.eq("userRole5", job),
										Restrictions.or(Restrictions.eq("userRole4", job),
												Restrictions.or(Restrictions.eq("userRole3", job),
														Restrictions.or(Restrictions.eq("userRole", job),
																Restrictions.eq("userRole2", job)))))

						).setProjection(Projections.rowCount()).uniqueResult()).intValue();
			}
			new Label(Common.numberFormat.get().format(qty)).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(job.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					job.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(job);
				}
			});

			Hbox toolbar = new Hbox();
			toolbar.setHeight("30px");
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(job);
					addWindow.setVisible(true);
					addWindow.onModal();

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
			button.setTooltiptext("Copy Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Tbmrole copyObjt = new Tbmrole();

					try {
						BeanUtilsBean.getInstance().copyProperties(copyObjt, job);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException("penyalinan data Grup Pengguna (Tbmrole)", e,
								new String[] {
										"Periksa kembali data grup pengguna sumber (job) yang hendak disalin, kemungkinan ada properti yang tidak kompatibel.",
										"Isi ulang formulir grup pengguna baru secara manual apabila hasil salinan tidak lengkap." });
					}

					copyObjt.setRoleId(null);
					copyObjt.setCopyDari(job);
					init(copyObjt);
					addWindow.setVisible(true);
					addWindow.onModal();

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					// TODO Auto-generated method stub

					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											// FIX: ganti Common.refreshDeleteFlush yang pakai currentNativeSession
											// (kena statement_timeout saat DELETE job_has_menu).
											// Gunakan session dedicated + SET LOCAL statement_timeout=0 agar
											// DELETE koleksi Tbmrole.menus tidak terpotong lock-timeout.
											Session sessHapus = null;
											try {
												sessHapus = HibernateUtil.getSessionFactory().openSession();
												sessHapus.beginTransaction();
												sessHapus.createSQLQuery("SET LOCAL statement_timeout = 0")
														.executeUpdate();
												Tbmrole jobFresh = (Tbmrole) sessHapus.get(Tbmrole.class,
														job.getRoleId());
												if (jobFresh != null) {
													sessHapus.delete(jobFresh);
												}
												sessHapus.getTransaction().commit();
											} catch (Exception eHapus) {
												if (sessHapus != null && sessHapus.getTransaction() != null) {
													try {
														sessHapus.getTransaction().rollback();
													} catch (Exception er) {
														ais.common.ErrorAuditUtil.record(er,
																"auto-audit(empty-catch) TbmroleAction:hapus:rollback");
													}
												}
												throw eHapus;
											} finally {
												if (sessHapus != null && sessHapus.isOpen()) {
													try {
														sessHapus.clear();
														sessHapus.close();
													} catch (Exception er) {
														ais.common.ErrorAuditUtil.record(er,
																"auto-audit(empty-catch) TbmroleAction:hapus:close");
													}
												}
											}
											ais.common.newui.NewUiCacheInvalidator.invalidateRole(job.getRoleId());

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("penghapusan data Grup Pengguna (Tbmrole)", e,
													new String[] {
															"Periksa apakah grup pengguna ini masih dipakai oleh akun pengguna (Tbmuser) lain, karena data yang masih berelasi tidak dapat dihapus.",
															"Pindahkan terlebih dahulu pengguna yang memakai grup ini ke grup lain, lalu ulangi penghapusan.",
															"Hubungi Administrator Sistem apabila grup pengguna ini memang harus dihapus." });
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

			if ((job.getRoleId() != null && job.getRoleId().equals(Tbmrole.DOSEN))
					|| (job.getRoleId() != null && job.getRoleId().equals(Tbmrole.GURU))
					|| (job.getRoleId() != null && job.getRoleId().equals(Tbmrole.SISWA))
					|| (job.getRoleId() != null && job.getRoleId().equals(Tbmrole.MAHASISWA))
					|| (job.getRoleId() != null && job.getRoleId().equals(Tbmrole.PEGAWAI))) {
				button.setVisible(false);
			}

			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Tbmrole());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({})
	private void init(final Tbmrole tbmrole) throws Exception {
		this.tbmrole = tbmrole;
		addWindow.setTitle(tbmrole.getId() == null ? "Tambah Grup Pengguna" : "Ubah Grup Pengguna");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		ais.ui.util.MyButtonTabbox btnTabRole = ais.ui.util.MyButtonTabbox.buat(center, "100%", new int[] { 0 });
		org.zkoss.zul.Div panelPenamaan = btnTabRole.tambahTab(0, "Penamaan Hak Akses", "/img/svg/key.svg");
		org.zkoss.zul.Div panelDashboard = btnTabRole.tambahTab(1, "Dashboard & Menu", "/img/svg/dashboard-chart.svg");
		org.zkoss.zul.Div panelPedagang = btnTabRole.tambahTab(2, "Hak Akses Pedagang", "/img/svg/user-business.svg");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(panelPenamaan);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);
		final Rows rowsPenamaan = rows;

		MyGrid gridDashboard = new MyGrid();
		gridDashboard.setWidth("100%");
		gridDashboard.setHeight("100%");
		gridDashboard.setParent(panelDashboard);
		Columns columnsDashboard = new Columns();
		columnsDashboard.setParent(gridDashboard);
		MyColumnConfig columnDashboard = new MyColumnConfig();
		columnDashboard.setParent(columnsDashboard);
		columnDashboard.setWidth("30%");
		columnDashboard = new MyColumnConfig();
		columnDashboard.setParent(columnsDashboard);
		final Rows rowsDashboard = new Rows();
		rowsDashboard.setParent(gridDashboard);

		MyGrid gridPedagang = new MyGrid();
		gridPedagang.setWidth("100%");
		gridPedagang.setHeight("100%");
		gridPedagang.setParent(panelPedagang);
		Columns columnsPedagang = new Columns();
		columnsPedagang.setParent(gridPedagang);
		MyColumnConfig columnPedagang = new MyColumnConfig();
		columnPedagang.setParent(columnsPedagang);
		columnPedagang.setWidth("30%");
		columnPedagang = new MyColumnConfig();
		columnPedagang.setParent(columnsPedagang);
		final Rows rowsPedagang = new Rows();
		rowsPedagang.setParent(gridPedagang);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Grup Pengguna"));
		row.appendChild(kode = new MyTextbox(tbmrole.getRoleId() == null ? "" : tbmrole.getRoleId()));
		kode.setWidth("90%");

		if (tbmrole.getRoleId() != null && !tbmrole.getRoleId().trim().isEmpty()) {
			kode.setDisabled(true);
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Akses"));
		row.appendChild(kodeAkses = new MyTextbox(tbmrole.getKode()));
		kodeAkses.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Grup Pengguna"));
		row.appendChild(nama = new MyTextbox(tbmrole.getRoleName() == null ? "" : tbmrole.getRoleName()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan kerja pada hak akses"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true, false));
		satuanKerja.setAttribute("satuanKerja", tbmrole.getSatuanKerja());
		satuanKerja.setValue(tbmrole.getSatuanKerja() == null ? "" : tbmrole.getSatuanKerja().getNama());
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Daftar kode satuan kerja pada hak akses"));
		row.appendChild(satuanKerjas = new MyTextbox(tbmrole.getSatuanKerjas()));
		satuanKerjas.setWidth("90%");
		satuanKerjas.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Jabatan pada hak akses"));
		row.appendChild(jenisJabatan = new Combobox());
		Common.insertComboDanSemua(jenisJabatan, "nama", JenisJabatan.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(true, jenisJabatan, tbmrole.getJenisJabatan());
		jenisJabatan.setWidth("90%");
		jenisJabatan.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas pada hak akses"));
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null, false);
		Common.selectComboItem(true, fakultas, tbmrole.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi pada hak akses"));
		Common.selectComboItem(true, jurusan, tbmrole.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program pada hak akses"));

		program = new Combobox();
		Common.insertComboDanSemua(program, "namaBaru", Program.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, program, tbmrole.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null, false, false);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan pada hak akses"));

		Common.selectComboItem(true, yayasan, tbmrole.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah pada hak akses"));

		Common.selectComboItem(true, sekolah, tbmrole.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		if (sekolah1 != null && sekolah1.getId() != null) {
			fakultas.getParent().setVisible(false);
			jurusan.getParent().setVisible(false);
			program.getParent().setVisible(false);

			sekolah.getParent().setVisible(true);
			yayasan.getParent().setVisible(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		aktif = new MyCheckboxConfig("Aktif");
		aktif.setDisabled(!edit);
		aktif.setChecked(tbmrole.getAktif());
		aktif.setParent(row);

		rows = rowsDashboard;

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dashboard default ketika login"));
		dashboardDefaultMain = new Combobox();
		dashboardDefaultMain.setWidth("90%");
		dashboardDefaultMain.setReadonly(true);
		Comboitem kosongDashboardDefault = new Comboitem("== Tidak Ditentukan ==");
		kosongDashboardDefault.setValue(null);
		kosongDashboardDefault.setParent(dashboardDefaultMain);
		String nilaiDashboardDefault = tbmrole.getDashboardDefaultMain();
		dashboardDefaultMain.setSelectedItem(kosongDashboardDefault);
		for (MainDashboardDefaultHelper.Option option : MainDashboardDefaultHelper.options()) {
			Comboitem item = option.toComboitem();
			item.setParent(dashboardDefaultMain);
			if (nilaiDashboardDefault != null && nilaiDashboardDefault.equals(option.value)) {
				dashboardDefaultMain.setSelectedItem(item);
			}
		}
		row.appendChild(dashboardDefaultMain);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		elearning = new MyCheckboxConfig("Tampilkan Dashboard E-learning");
		elearning.setChecked(tbmrole.getElearning());
		elearning.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		kegiatanDanPrestasi = new MyCheckboxConfig("Tampilkan Dashboard Kegiatan dan Prestasi");
		kegiatanDanPrestasi.setChecked(tbmrole.getKegiatanDanPrestasi());
		kegiatanDanPrestasi.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		pustaka = new MyCheckboxConfig("Tampilkan Dashboard Pustaka");
		pustaka.setChecked(tbmrole.getPustaka());
		pustaka.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		dashboard = new MyCheckboxConfig("Tampilkan Dashboard Akademik");
		dashboard.setChecked(tbmrole.getDashboard());
		dashboard.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		workflow = new MyCheckboxConfig("Tampilkan Dashboard Workflow / SOP");
		workflow.setChecked(tbmrole.getWorkflow());
		workflow.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		administrasi = new MyCheckboxConfig("Tampilkan Dashboard Administrasi / Surat Menyurat");
		administrasi.setChecked(tbmrole.getAdministrasi());
		administrasi.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		pengadaan = new MyCheckboxConfig("Tampilkan Dashboard Pengadaan Barang/Jasa");
		pengadaan.setChecked(tbmrole.getPengadaan());
		pengadaan.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		keuangan = new MyCheckboxConfig("Tampilkan Dashboard Keuangan");
		keuangan.setChecked(tbmrole.getKeuangan());
		keuangan.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		kepegawaian = new MyCheckboxConfig("Tampilkan Dashboard Kepegawaian");
		kepegawaian.setChecked(tbmrole.getKepegawaian());
		kepegawaian.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		pembayaran = new MyCheckboxConfig("Tampilkan Dashboard Piutang dan Pembayaran");
		pembayaran.setChecked(tbmrole.getPembayaran());
		pembayaran.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		akunting = new MyCheckboxConfig("Tampilkan Dashboard Akuntansi");
		akunting.setChecked(tbmrole.getAkunting());
		akunting.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		kinerja = new MyCheckboxConfig("Tampilkan Dashboard Kinerja");
		kinerja.setChecked(tbmrole.getKinerja());
		kinerja.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		kantin = new MyCheckboxConfig("Tampilkan Modul Kantin");
		kantin.setChecked(tbmrole.getKantin());
		kantin.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		tampilPos = new MyCheckboxConfig("Tampilkan Dasbor POS (Kasir) di halaman utama");
		tampilPos.setChecked(tbmrole.getTampilPos());
		tampilPos.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		dashboardKoperasi = new MyCheckboxConfig("Tampilkan Dasbor Utama \"Koperasi\" di halaman utama");
		dashboardKoperasi.setChecked(tbmrole.getDashboardKoperasi());
		dashboardKoperasi.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		repository = new MyCheckboxConfig("Tampilkan Dasbor Repository");
		repository.setChecked(tbmrole.getDasborRepository());
		repository.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		aksesFeeder = new MyCheckboxConfig("Boleh Akses Dasbor Neo Feeder");
		aksesFeeder.setChecked(tbmrole.getBolehAksesFeeder());
		aksesFeeder.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		aksesSister = new MyCheckboxConfig("Boleh Akses Dasbor SISTER");
		aksesSister.setChecked(tbmrole.getBolehAksesSister());
		aksesSister.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		antarJemput = new MyCheckboxConfig("Tampilkan Dasbor Antar Jemput");
		antarJemput.setChecked(tbmrole.getDasboardAntarJemput());
		antarJemput.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		spmi = new MyCheckboxConfig("Tampilkan Dasbor SPMI");
		spmi.setChecked(tbmrole.getTampilkanSpmi());
		spmi.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		gaji = new MyCheckboxConfig("Tampilkan Dasbor Penggajian (Gaji)");
		gaji.setChecked(tbmrole.getTampilkanGaji());
		gaji.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		emedic = new MyCheckboxConfig("Tampilkan Dasbor Rumah Sakit / Klinik (eMedic)");
		emedic.setChecked(tbmrole.getEmedic());
		emedic.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		bolehEntryTopup = new MyCheckboxConfig("Boleh entry topup / tabungan / deposit");
		bolehEntryTopup.setChecked(tbmrole.getBolehEntryTopup());
		bolehEntryTopup.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		mengajukanPengajuanPegawaiLain = new MyCheckboxConfig("Mengajukan Pengajuan Pegawai Lain");
		mengajukanPengajuanPegawaiLain.setChecked(tbmrole.getMengajukanPengajuanPegawaiLain());
		mengajukanPengajuanPegawaiLain.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		kalenderAkademik = new MyCheckboxConfig("Tampilkan Kalender Akademik");
		kalenderAkademik.setChecked(tbmrole.getKalenderAkademik());
		kalenderAkademik.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		infoKegiatan = new MyCheckboxConfig("Tampilkan Info Kegiatan");
		infoKegiatan.setChecked(tbmrole.getInfoKegiatan());
		infoKegiatan.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				melihatDataPegawaiLain = new MyCheckboxConfig("Pengguna memiliki hak akses melihat data pegawai lain"));
		melihatDataPegawaiLain.setChecked(tbmrole.getMelihatDataPegawaiLain());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				melihatDataSatkerLain = new MyCheckboxConfig("Pengguna memiliki hak akses melihat data satker lain"));
		melihatDataSatkerLain.setChecked(tbmrole.getMelihatDataSatkerLain());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(melihatSemuaSurat = new MyCheckboxConfig("Pengguna memiliki hak akses melihat semua surat"));
		melihatSemuaSurat.setChecked(tbmrole.getMelihatSemuaSurat());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(melihatSemuaSop = new MyCheckboxConfig("Pengguna memiliki hak akses melihat semua SOP"));
		melihatSemuaSop.setChecked(tbmrole.getMelihatSemuaSop());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				presensiKehadiran = new MyCheckboxConfig("Pengguna memiliki hak akses membuka presensi kehadiran"));
		presensiKehadiran.setChecked(tbmrole.getPresensiKehadiran());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				updateFormatLaporan = new MyCheckboxConfig("Pengguna memiliki hak akses untuk update format laporan"));
		updateFormatLaporan.setChecked(tbmrole.getUpdateFormatLaporan());

		if ((tbmrole.getRoleId() != null && tbmrole.getRoleId().equals(Tbmrole.DOSEN))
				|| (tbmrole.getRoleId() != null && tbmrole.getRoleId().equals(Tbmrole.GURU))
				|| (tbmrole.getRoleId() != null && tbmrole.getRoleId().equals(Tbmrole.SISWA))
				|| (tbmrole.getRoleId() != null && tbmrole.getRoleId().equals(Tbmrole.MAHASISWA))
				|| (tbmrole.getRoleId() != null && tbmrole.getRoleId().equals(Tbmrole.PEGAWAI))) {

			melihatDataPegawaiLain.setDisabled(true);
			melihatDataSatkerLain.setDisabled(true);
			akunting.setDisabled(true);
			pembayaran.setDisabled(true);
			kepegawaian.setDisabled(true);
			melihatSemuaSurat.setDisabled(true);
			if (melihatSemuaSop != null) {
				melihatSemuaSop.setDisabled(true);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(absenLangsung = new MyCheckboxConfig(
				"Pengguna memiliki hak akses absen langsung di menu presensi kehadiran"));
		absenLangsung.setChecked(tbmrole.getAbsenLangsung());

		if ((tbmrole.getRoleId() != null && tbmrole.getRoleId().equals(Tbmrole.SISWA))
				|| (tbmrole.getRoleId() != null && tbmrole.getRoleId().equals(Tbmrole.MAHASISWA))) {
			absenLangsung.setDisabled(true);
			presensiKehadiran.setDisabled(true);
			kinerja.setDisabled(true);
			kantin.setDisabled(true);
			tampilPos.setDisabled(true);
			dashboardKoperasi.setDisabled(true);
		}

		rows = rowsPedagang;
		tokoAksesCheckboxMap = new java.util.LinkedHashMap<Long, MyCheckboxConfig>();
		crudCheckboxMap = new java.util.LinkedHashMap<String, MyCheckboxConfig>();

		// Semua checkbox "Hak Akses Pedagang" (eBisnis) di bawah ini SEKARANG dibaca dari SATU kolom
		// JSON Tbmrole.ebisnisMenu (bukan lagi 26 kolom Boolean terpisah -- lihat JavaDoc
		// EbisnisMenuKatalog). Widget checkbox-nya TIDAK berubah (tetap satu-satu spt sebelumnya, tak
		// perlu refactor tata letak); hanya sumber nilai awal & tujuan simpannya yang berubah.
		JSONObject ebisnisMenuTersimpan = ais.common.EbisnisMenuKatalog.urai(tbmrole.getEbisnisMenu());
		JSONObject ebisnisMenuMap = ebisnisMenuTersimpan.getJSONObject("menu");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Main menu kantin member"));
		row.appendChild(kantinMemberLandingPage = new MyCheckboxConfig(
				"Arahkan langsung ke halaman kantin (/WEB-INF/baru/modul/kantin/index.jsp)"));
		kantinMemberLandingPage.setChecked(ebisnisMenuTersimpan.optBoolean("landingKantin", false));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesSupervisorKantin = new MyCheckboxConfig("Supervisor"));
		aksesSupervisorKantin.setChecked(ebisnisMenuTersimpan.optBoolean("supervisor", false));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesBerandaKantin = new MyCheckboxConfig("Beranda Kantin"));
		aksesBerandaKantin.setChecked(ebisnisMenuTersimpan.optBoolean("berandaKantin", false));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesKasir = new MyCheckboxConfig("Kasir"));
		aksesKasir.setChecked(ebisnisMenuMap.optBoolean("kasir", true));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesRingkasan = new MyCheckboxConfig("Ringkasan"));
		aksesRingkasan.setChecked(ebisnisMenuMap.optBoolean("ringkasan", true));

		tambahHeaderKolomCrud(rows);
		aksesPesanan = new MyCheckboxConfig("");
		aksesPesanan.setChecked(ebisnisMenuMap.optBoolean("pesanan", true));
		appendBarisAksesCrud(rows, "Pesanan", "pesanan", aksesPesanan, ebisnisMenuTersimpan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesKonfigurasi = new MyCheckboxConfig("Pengaturan"));
		aksesKonfigurasi.setChecked(ebisnisMenuMap.optBoolean("konfigurasi", true));

		aksesAnggota = new MyCheckboxConfig("");
		aksesAnggota.setChecked(ebisnisMenuMap.optBoolean("anggota", true));
		appendBarisAksesCrud(rows, "Anggota", "anggota", aksesAnggota, ebisnisMenuTersimpan);

		aksesProduk = new MyCheckboxConfig("");
		aksesProduk.setChecked(ebisnisMenuMap.optBoolean("produk", true));
		appendBarisAksesCrud(rows, "Barang", "produk", aksesProduk, ebisnisMenuTersimpan);

		aksesKulakan = new MyCheckboxConfig("");
		aksesKulakan.setChecked(ebisnisMenuMap.optBoolean("kulakan", true));
		appendBarisAksesCrud(rows, "Kulakan", "kulakan", aksesKulakan, ebisnisMenuTersimpan);

		aksesDiskon = new MyCheckboxConfig("");
		aksesDiskon.setChecked(ebisnisMenuMap.optBoolean("diskon", true));
		appendBarisAksesCrud(rows, "Diskon", "diskon", aksesDiskon, ebisnisMenuTersimpan);

		aksesPembayaran = new MyCheckboxConfig("");
		aksesPembayaran.setChecked(ebisnisMenuMap.optBoolean("pembayaran", true));
		appendBarisAksesCrud(rows, "Pembayaran", "pembayaran", aksesPembayaran, ebisnisMenuTersimpan);

		aksesPedagang = new MyCheckboxConfig("");
		aksesPedagang.setChecked(ebisnisMenuMap.optBoolean("pedagang", true));
		appendBarisAksesCrud(rows, "Pedagang", "pedagang", aksesPedagang, ebisnisMenuTersimpan);

		aksesStokOpname = new MyCheckboxConfig("");
		aksesStokOpname.setChecked(ebisnisMenuMap.optBoolean("stokopname", true));
		appendBarisAksesCrud(rows, "Stok", "stokopname", aksesStokOpname, ebisnisMenuTersimpan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesMeja = new MyCheckboxConfig("Meja"));
		aksesMeja.setChecked(ebisnisMenuMap.optBoolean("meja", true));

		aksesPenyedia = new MyCheckboxConfig("");
		aksesPenyedia.setChecked(ebisnisMenuMap.optBoolean("penyedia", true));
		appendBarisAksesCrud(rows, "Vendor", "penyedia", aksesPenyedia, ebisnisMenuTersimpan);

		aksesKasKasir = new MyCheckboxConfig("");
		aksesKasKasir.setChecked(ebisnisMenuMap.optBoolean("kaskasir", true));
		appendBarisAksesCrud(rows, "Kas Kasir", "kaskasir", aksesKasKasir, ebisnisMenuTersimpan);

		aksesSetoranTenant = new MyCheckboxConfig("");
		aksesSetoranTenant.setChecked(ebisnisMenuMap.optBoolean("setorantenant", true));
		appendBarisAksesCrud(rows, "Setoran Tenant", "setorantenant", aksesSetoranTenant, ebisnisMenuTersimpan);

		aksesJadwalOpname = new MyCheckboxConfig("");
		aksesJadwalOpname.setChecked(ebisnisMenuMap.optBoolean("jadwalopname", true));
		appendBarisAksesCrud(rows, "Jadwal Opname", "jadwalopname", aksesJadwalOpname, ebisnisMenuTersimpan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesStokExpired = new MyCheckboxConfig("Stok Expired"));
		aksesStokExpired.setChecked(ebisnisMenuMap.optBoolean("stokexpired", true));

		aksesLimitKredit = new MyCheckboxConfig("");
		aksesLimitKredit.setChecked(ebisnisMenuMap.optBoolean("limitkredit", true));
		appendBarisAksesCrud(rows, "Limit Kredit", "limitkredit", aksesLimitKredit, ebisnisMenuTersimpan);

		aksesMutasiRekening = new MyCheckboxConfig("");
		aksesMutasiRekening.setChecked(ebisnisMenuMap.optBoolean("mutasirekening", true));
		appendBarisAksesCrud(rows, "Rekening Koran", "mutasirekening", aksesMutasiRekening, ebisnisMenuTersimpan);

		aksesProduksi = new MyCheckboxConfig("");
		aksesProduksi.setChecked(ebisnisMenuMap.optBoolean("produksi", true));
		appendBarisAksesCrud(rows, "Produksi", "produksi", aksesProduksi, ebisnisMenuTersimpan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesLaporan = new MyCheckboxConfig("Laporan"));
		aksesLaporan.setChecked(ebisnisMenuMap.optBoolean("laporan", true));
		row.appendChild(aksesLaporanKeuangan = new MyCheckboxConfig("Laporan Keuangan"));
		aksesLaporanKeuangan.setChecked(ebisnisMenuMap.optBoolean("laporankeuangan", true));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesLaporanTransaksi = new MyCheckboxConfig("Lap. Transaksi"));
		aksesLaporanTransaksi.setChecked(ebisnisMenuMap.optBoolean("laporantransaksi", true));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesPengaturanLaporan = new MyCheckboxConfig("Konf. Laporan"));
		aksesPengaturanLaporan.setChecked(ebisnisMenuMap.optBoolean("pengaturanlaporan", true));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesRiwayatSinkronisasi = new MyCheckboxConfig("Sinkronisasi"));
		aksesRiwayatSinkronisasi.setChecked(ebisnisMenuMap.optBoolean("riwayatsinkronisasi", true));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesLogError = new MyCheckboxConfig("Log Error"));
		aksesLogError.setChecked(ebisnisMenuMap.optBoolean("logerror", true));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aksesRiwayatPenjualan = new MyCheckboxConfig("Riwayat Penjualan"));
		aksesRiwayatPenjualan.setChecked(ebisnisMenuMap.optBoolean("riwayatpenjualan", true));

		aksesReturPenjualan = new MyCheckboxConfig("");
		aksesReturPenjualan.setChecked(ebisnisMenuMap.optBoolean("returpenjualan", true));
		appendBarisAksesCrud(rows, "Retur Penjualan", "returpenjualan", aksesReturPenjualan, ebisnisMenuTersimpan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Toko yang boleh dilihat"));
		Vbox boxTokoAkses = new Vbox();
		boxTokoAkses.setSpacing("4px");
		boxTokoAkses.setParent(row);
		List<Long> tokoAksesDipilih = parseTokoAksesJson(tbmrole.getTokoAksesJson());
		Session sessionTokoAkses = HibernateUtil.currentSession();
		@SuppressWarnings("unchecked")
		List<Toko> daftarTokoAkses = sessionTokoAkses.createCriteria(Toko.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.addOrder(Order.asc("nama")).list();
		for (Toko tokoPilihan : daftarTokoAkses) {
			if (tokoPilihan == null || tokoPilihan.getId() == null) {
				continue;
			}
			Hbox hboxToko = new Hbox();
			hboxToko.setSpacing("6px");
			hboxToko.setAlign("center");
			hboxToko.setParent(boxTokoAkses);
			MyCheckboxConfig chkToko = new MyCheckboxConfig("");
			chkToko.setChecked(tokoAksesDipilih.contains(tokoPilihan.getId()));
			chkToko.setParent(hboxToko);
			new Label(tokoPilihan.getNama() == null ? ("Toko " + tokoPilihan.getId()) : tokoPilihan.getNama()).setParent(hboxToko);
			tokoAksesCheckboxMap.put(tokoPilihan.getId(), chkToko);
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	private List<Long> parseTokoAksesJson(String json) {
		List<Long> hasil = new ArrayList<Long>();
		if (json == null || json.trim().isEmpty()) {
			return hasil;
		}
		try {
			JSONArray arr = new JSONArray(json);
			for (int i = 0; i < arr.length(); i++) {
				try {
					hasil.add(Long.valueOf(arr.get(i).toString()));
				} catch (Exception ignore) {
					ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/maintenance/TbmroleAction.java:parseTokoAksesJson");
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return hasil;
	}

	/** Lebar tetap tiap kolom checkbox CRUD -- dipakai SAMA di header ({@link #tambahHeaderKolomCrud})
	 * &amp; tiap baris ({@link #appendBarisAksesCrud}) supaya kolomnya lurus, dan cukup sempit (tanpa
	 * label teks per-checkbox) supaya 6 kolom muat satu baris di panel modal yang sempit -- checkbox
	 * berlabel penuh ("Read"/"Create"/dst di TIAP baris) yg dipakai sebelumnya kepanjangan & melebar
	 * ke baris baru, itulah yg diminta "dikembalikan sederhana". */
	private static final String LEBAR_KOLOM_CRUD = "40px";

	/**
	 * Header kolom grid CRUD (Read/Create/Update/Delete/Approve/Reject/Check All) -- dipanggil SEKALI
	 * saja tepat sebelum baris ber-CRUD {@link ais.common.EbisnisMenuKatalog#KUNCI_CRUD} PERTAMA muncul,
	 * bukan diulang tiap baris (itu yg bikin tiap baris jadi lebar & melebar ke baris baru sebelumnya).
	 */
	private void tambahHeaderKolomCrud(Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Hbox box = new Hbox();
		box.setSpacing("4px");
		row.appendChild(box);
		String[] judul = { "Read", "Create", "Update", "Delete", "Approve", "Reject", "Semua" };
		for (String j : judul) {
			Label l = new Label(j);
			l.setStyle("display:inline-block;width:" + LEBAR_KOLOM_CRUD + ";font-size:10px;font-weight:700;color:#64748b;text-align:center;");
			l.setParent(box);
		}
	}

	/**
	 * Baris "Hak Akses Pedagang" utk menu ber-CRUD ({@link ais.common.EbisnisMenuKatalog#KUNCI_CRUD}) --
	 * grid Read/Create/Update/Delete/Approve/Reject + "Check All" per baris, nilainya tersimpan di
	 * {@code Tbmrole.ebisnisMenu.crud} -- BUKAN {@code RolePrivilage}/{@code Menu} entity, krn menu POS/
	 * e-Kantin ini sengaja tidak punya baris {@code Menu} tersendiri, lihat JavaDoc {@code EbisnisMenuKatalog}.
	 * Checkbox TANPA label teks (kolomnya sudah dijelaskan sekali lewat {@link #tambahHeaderKolomCrud})
	 * supaya baris tetap ringkas satu baris -- "Supervisor" TETAP hanya satu toggle blanket di atas
	 * (lihat {@code aksesSupervisorKantin}), BUKAN kolom per-baris di sini (itu pernah keliru ditambahkan
	 * & bikin baris kepanjangan/pecah -- sudah dibuang, jangan ditambah ulang).
	 * {@code read} HARUS sudah dibuat &amp; di-{@code setChecked} pemanggil (field akses* yg sudah ada,
	 * dipakai lagi apa adanya, sesuai catatan di {@link #buildEbisnisMenuJson()}).
	 */
	private void appendBarisAksesCrud(Rows rows, String label, final String kunci, final MyCheckboxConfig read,
			JSONObject roleEbisnisMenu) {
		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(label));

		Hbox box = new Hbox();
		box.setSpacing("4px");
		box.setAlign("center");
		row.appendChild(box);

		JSONObject aksiTersimpan = roleEbisnisMenu == null ? null : roleEbisnisMenu.optJSONObject("crud");
		aksiTersimpan = aksiTersimpan == null ? null : aksiTersimpan.optJSONObject(kunci);

		read.setLabel("");
		read.setStyle("display:inline-block;width:" + LEBAR_KOLOM_CRUD + ";text-align:center;");
		box.appendChild(read);

		final MyCheckboxConfig create = new MyCheckboxConfig();
		create.setChecked(aksiTersimpan == null || aksiTersimpan.optBoolean("create", true));
		create.setStyle("display:inline-block;width:" + LEBAR_KOLOM_CRUD + ";text-align:center;");
		box.appendChild(create);
		crudCheckboxMap.put(kunci + "_create", create);

		final MyCheckboxConfig update = new MyCheckboxConfig();
		update.setChecked(aksiTersimpan == null || aksiTersimpan.optBoolean("update", true));
		update.setStyle("display:inline-block;width:" + LEBAR_KOLOM_CRUD + ";text-align:center;");
		box.appendChild(update);
		crudCheckboxMap.put(kunci + "_update", update);

		final MyCheckboxConfig delete = new MyCheckboxConfig();
		delete.setChecked(aksiTersimpan == null || aksiTersimpan.optBoolean("delete", true));
		delete.setStyle("display:inline-block;width:" + LEBAR_KOLOM_CRUD + ";text-align:center;");
		box.appendChild(delete);
		crudCheckboxMap.put(kunci + "_delete", delete);

		final MyCheckboxConfig approve = new MyCheckboxConfig();
		approve.setChecked(aksiTersimpan == null || aksiTersimpan.optBoolean("approve", true));
		approve.setStyle("display:inline-block;width:" + LEBAR_KOLOM_CRUD + ";text-align:center;");
		box.appendChild(approve);
		crudCheckboxMap.put(kunci + "_approve", approve);

		final MyCheckboxConfig reject = new MyCheckboxConfig();
		reject.setChecked(aksiTersimpan == null || aksiTersimpan.optBoolean("reject", true));
		reject.setStyle("display:inline-block;width:" + LEBAR_KOLOM_CRUD + ";text-align:center;");
		box.appendChild(reject);
		crudCheckboxMap.put(kunci + "_reject", reject);

		final MyCheckboxConfig checkAll = new MyCheckboxConfig();
		checkAll.setChecked(read.isChecked() && create.isChecked() && update.isChecked() && delete.isChecked()
				&& approve.isChecked() && reject.isChecked());
		checkAll.setStyle("display:inline-block;width:" + LEBAR_KOLOM_CRUD + ";text-align:center;");
		box.appendChild(checkAll);
		checkAll.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				boolean v = checkAll.isChecked();
				read.setChecked(v);
				create.setChecked(v);
				update.setChecked(v);
				delete.setChecked(v);
				approve.setChecked(v);
				reject.setChecked(v);
			}
		});
	}

	/**
	 * Susun JSON {@code Tbmrole.ebisnisMenu} dari 26 checkbox "Hak Akses Pedagang" (widget-nya TETAP
	 * satu-satu spt sebelumnya -- lihat komentar di baris pembuatan checkbox) -- pengganti 26 baris
	 * {@code tbmrole.setAksesX(...)} yang dulu ada di sini. Lihat {@link ais.common.EbisnisMenuKatalog}.
	 */
	private String buildEbisnisMenuJson() throws org.json.JSONException {
		JSONObject obj = new JSONObject();
		obj.put("supervisor", aksesSupervisorKantin != null && aksesSupervisorKantin.isChecked());
		obj.put("berandaKantin", aksesBerandaKantin != null && aksesBerandaKantin.isChecked());
		obj.put("landingKantin", kantinMemberLandingPage != null && kantinMemberLandingPage.isChecked());
		JSONObject menu = new JSONObject();
		menu.put("kasir", aksesKasir == null || aksesKasir.isChecked());
		menu.put("ringkasan", aksesRingkasan == null || aksesRingkasan.isChecked());
		menu.put("pesanan", aksesPesanan == null || aksesPesanan.isChecked());
		menu.put("konfigurasi", aksesKonfigurasi == null || aksesKonfigurasi.isChecked());
		menu.put("anggota", aksesAnggota == null || aksesAnggota.isChecked());
		menu.put("produk", aksesProduk == null || aksesProduk.isChecked());
		menu.put("kulakan", aksesKulakan == null || aksesKulakan.isChecked());
		menu.put("diskon", aksesDiskon == null || aksesDiskon.isChecked());
		menu.put("pembayaran", aksesPembayaran == null || aksesPembayaran.isChecked());
		menu.put("pedagang", aksesPedagang == null || aksesPedagang.isChecked());
		menu.put("stokopname", aksesStokOpname == null || aksesStokOpname.isChecked());
		menu.put("meja", aksesMeja == null || aksesMeja.isChecked());
		menu.put("penyedia", aksesPenyedia == null || aksesPenyedia.isChecked());
		menu.put("kaskasir", aksesKasKasir == null || aksesKasKasir.isChecked());
		menu.put("setorantenant", aksesSetoranTenant == null || aksesSetoranTenant.isChecked());
		menu.put("jadwalopname", aksesJadwalOpname == null || aksesJadwalOpname.isChecked());
		menu.put("stokexpired", aksesStokExpired == null || aksesStokExpired.isChecked());
		menu.put("limitkredit", aksesLimitKredit == null || aksesLimitKredit.isChecked());
		menu.put("mutasirekening", aksesMutasiRekening == null || aksesMutasiRekening.isChecked());
		menu.put("produksi", aksesProduksi == null || aksesProduksi.isChecked());
		menu.put("laporan", aksesLaporan == null || aksesLaporan.isChecked());
		menu.put("laporankeuangan", aksesLaporanKeuangan == null || aksesLaporanKeuangan.isChecked());
		menu.put("laporantransaksi", aksesLaporanTransaksi == null || aksesLaporanTransaksi.isChecked());
		menu.put("pengaturanlaporan", aksesPengaturanLaporan == null || aksesPengaturanLaporan.isChecked());
		menu.put("riwayatsinkronisasi", aksesRiwayatSinkronisasi == null || aksesRiwayatSinkronisasi.isChecked());
		menu.put("logerror", aksesLogError == null || aksesLogError.isChecked());
		menu.put("riwayatpenjualan", aksesRiwayatPenjualan == null || aksesRiwayatPenjualan.isChecked());
		menu.put("returpenjualan", aksesReturPenjualan == null || aksesReturPenjualan.isChecked());
		obj.put("menu", menu);

		JSONObject crud = new JSONObject();
		if (crudCheckboxMap != null) {
			for (String kunci : ais.common.EbisnisMenuKatalog.KUNCI_CRUD) {
				JSONObject aksiMenu = new JSONObject();
				for (String aksi : ais.common.EbisnisMenuKatalog.AKSI_CRUD) {
					MyCheckboxConfig checkbox = crudCheckboxMap.get(kunci + "_" + aksi);
					aksiMenu.put(aksi, checkbox == null || checkbox.isChecked());
				}
				crud.put(kunci, aksiMenu);
			}
		}
		obj.put("crud", crud);

		return obj.toString();
	}

	private String buildTokoAksesJson() {
		JSONArray arr = new JSONArray();
		if (tokoAksesCheckboxMap == null) {
			return arr.toString();
		}
		for (Map.Entry<Long, MyCheckboxConfig> entry : tokoAksesCheckboxMap.entrySet()) {
			if (entry.getValue() != null && entry.getValue().isChecked()) {
				arr.put(entry.getKey());
			}
		}
		return arr.toString();
	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Grup Pengguna belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Kode Grup Pengguna dengan kode unik; (2) pastikan kode tidak mengandung karakter tidak valid; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Grup Pengguna belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Grup Pengguna dengan nama yang deskriptif; (2) pastikan nama tidak duplikat dengan grup yang sudah ada; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkNamaTbmrole();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Grup Pengguna yang dimasukkan sudah ada di database. Langkah yang dapat dilakukan: (1) ganti nama grup dengan nama yang berbeda dan unik; (2) periksa daftar grup yang sudah ada sebelum membuat baru; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", 1,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		i = checkKodeTbmrole();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Kode Grup Pengguna yang dimasukkan sudah ada di database. Langkah yang dapat dilakukan: (1) ganti kode grup dengan kode yang berbeda dan unik; (2) periksa daftar kode grup yang sudah ada sebelum membuat baru; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", 1,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (tbmrole.getRoleId() != null) {
			tbmrole = (Tbmrole) session.load(Tbmrole.class, tbmrole.getRoleId());

		}
		tbmrole.setDasboardAntarJemput(antarJemput.isChecked());
		tbmrole.setDasborRepository(repository.isChecked());
		tbmrole.setBolehAksesFeeder(aksesFeeder.isChecked());
		tbmrole.setBolehAksesSister(aksesSister.isChecked());
		tbmrole.setTampilkanSpmi(spmi.isChecked());
		tbmrole.setTampilkanGaji(gaji.isChecked());
		tbmrole.setEmedic(emedic.isChecked());
		tbmrole.setDashboardDefaultMain(
				dashboardDefaultMain == null || dashboardDefaultMain.getSelectedItem() == null ? null
						: (String) dashboardDefaultMain.getSelectedItem().getValue());
		tbmrole.setKode(kodeAkses.getValue().trim());
		tbmrole.setRoleName(nama.getValue());
		tbmrole.setRoleId(kode.getValue());
		tbmrole.setAktif(aktif.isChecked());
		tbmrole.setElearning(elearning.isChecked());
		tbmrole.setKegiatanDanPrestasi(kegiatanDanPrestasi.isChecked());
		tbmrole.setPustaka(pustaka.isChecked());
		tbmrole.setDashboard(dashboard.isChecked());
		tbmrole.setWorkflow(workflow.isChecked());
		tbmrole.setAdministrasi(administrasi.isChecked());
		tbmrole.setPengadaan(pengadaan.isChecked());
		tbmrole.setKeuangan(keuangan.isChecked());
		tbmrole.setKepegawaian(kepegawaian.isChecked());
		tbmrole.setPembayaran(pembayaran.isChecked());
		tbmrole.setKalenderAkademik(kalenderAkademik.isChecked());
		tbmrole.setInfoKegiatan(infoKegiatan.isChecked());
		tbmrole.setMelihatDataPegawaiLain(melihatDataPegawaiLain.isChecked());
		tbmrole.setMelihatDataSatkerLain(melihatDataSatkerLain.isChecked());
		tbmrole.setPresensiKehadiran(presensiKehadiran.isChecked());
		tbmrole.setAbsenLangsung(absenLangsung.isChecked());
		tbmrole.setAkunting(akunting.isChecked());
		tbmrole.setUpdateFormatLaporan(updateFormatLaporan.isChecked());
		tbmrole.setKinerja(kinerja.isChecked());
		tbmrole.setKantin(kantin.isChecked());
		tbmrole.setTampilPos(tampilPos.isChecked());
		tbmrole.setDashboardKoperasi(dashboardKoperasi.isChecked());
		tbmrole.setEbisnisMenu(buildEbisnisMenuJson());
		tbmrole.setTokoAksesJson(buildTokoAksesJson());
		tbmrole.setMengajukanPengajuanPegawaiLain(mengajukanPengajuanPegawaiLain.isChecked());
		tbmrole.setJenisJabatan((JenisJabatan) (jenisJabatan.getSelectedItem() == null ? null
				: jenisJabatan.getSelectedItem().getValue()));
		tbmrole.setSatuanKerjas(satuanKerjas.getValue().trim());
		tbmrole.setMelihatSemuaSurat(melihatSemuaSurat.isChecked());
		tbmrole.setMelihatSemuaSop(melihatSemuaSop != null && melihatSemuaSop.isChecked());

		tbmrole.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		tbmrole.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		tbmrole.setProgram(
				(Program) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));

		tbmrole.setYayasan((Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		tbmrole.setSekolah((Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		tbmrole.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		tbmrole.setBolehEntryTopup(bolehEntryTopup.isChecked());

		if (tbmrole.getCopyDari() != null) {

			Tbmrole tbmroleData = (Tbmrole) tbmrole.getCopyDari();
			session.refresh(tbmroleData);
			Set<Menu> menus = tbmroleData.getMenus();

			Set<Menu> menus2 = new HashSet<Menu>();
			for (Menu menu : menus) {
				menus2.add(menu);
			}

			tbmrole.setMenus(menus2);
		}

		if (tbmrole.getRoleId() != null) {
			Common.refreshUpdate(session, tbmrole);
		} else {
			session.save(tbmrole);
		}

		if (tbmrole.getCopyDari() != null) {

			Tbmrole tbmroleData = (Tbmrole) tbmrole.getCopyDari();
			session.refresh(tbmroleData);
			Set<Menu> menus = tbmroleData.getMenus();

			for (Menu menu : menus) {
				RolePrivilage rolePrivilage = (RolePrivilage) ConstantValues.simpleObject(
						session.createCriteria(RolePrivilage.class).add(Restrictions.eq("role", tbmrole.getCopyDari()))
								.add(Restrictions.eq("menu", menu)).setMaxResults(1),
						RolePrivilage.class);

				if (rolePrivilage != null) {
					RolePrivilage rolePrivilageBaru = new RolePrivilage();

					try {
						BeanUtilsBean.getInstance().copyProperties(rolePrivilageBaru, rolePrivilage);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}

					rolePrivilageBaru.setId(null);
					rolePrivilageBaru.setMenu(menu);
					rolePrivilageBaru.setRole(tbmrole);
					session.save(rolePrivilageBaru);
					session.flush();
				}
			}

		}
		ais.common.newui.NewUiCacheInvalidator.invalidateRole(tbmrole.getRoleId());
		// hakAkses() memakai cache role aktif per pengguna. Segarkan cache tersebut
		// agar perubahan hak (termasuk Boleh entry topup/deposit) langsung dipakai
		// oleh dashboard tanpa menunggu pengguna login ulang atau mengganti role.
		Tbmuser.refreshHakAksesUntukRole(tbmrole);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Tbmrole.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("roleName", searchnama.getValue().trim(), MatchMode.ANYWHERE)))
				.add((searchid == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchid.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("roleId", searchid.getValue().trim(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.asc("roleName"));

		return criteria;
	}

	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<Tbmrole> job = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Tbmrole.class);

		ListModel strset = new SimpleListModel(job);

		grid.setRowRenderer(new TbmroleRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKodeTbmrole() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Tbmrole.class).setProjection(Projections.count("roleId"))

				.add(Restrictions.eq("roleId", kode.getValue().trim()))
				.add(this.tbmrole.getRoleId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("roleId", this.tbmrole.getRoleId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkNamaTbmrole() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Tbmrole.class).setProjection(Projections.count("roleId"))
				.add(Restrictions.eq("roleName", nama.getValue().trim()))
				.add(this.tbmrole.getRoleId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("roleId", this.tbmrole.getRoleId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
