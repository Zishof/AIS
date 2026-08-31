package ais.action.maintenance;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

import ais.action.master.helper.MenuTreeModel;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk menu. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Tree tree},
 * {@code Textbox label}, {@code Textbox url}, {@code Textbox bigIcon}, {@code Longbox root}, {@code Longbox
 * child}, {@code MyCheckboxConfig aktif}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code initTree()}, {@code init()}); pembacaan/pencarian ({@code onReloadTree()}, {@code
 * reloadTreeitem()}); validasi/perhitungan ({@code checkId()}, {@code checkKodeMenu()}); mutasi data ({@code
 * onSave()}); operasi domain lain ({@code onMenu()}, {@code onAdd()}, {@code openChilds()}, {@code
 * closeChilds()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class MenuAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;

	private Tree tree;

	private Textbox label;
	private Textbox url;
	private Textbox bigIcon;
	private Longbox root;
	private Longbox child;
	private MyCheckboxConfig aktif;

	private boolean edit = true;
	private boolean delete = true;

	private Menu menu;
	private boolean add = true;
	private MenuTreeModel menuTreeModel;
	private Longbox id;

	private Tabpanel menuTab;
	private MyCheckboxConfig tampilDiSekolah;
	private MyCheckboxConfig tampilDiPt;
	private MyCheckboxConfig bukaHalamanBaru;

	public void onMenu(Event event) {
		if (menuTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(menuTab);
			MyInclude iframe = new MyInclude("/pages/master/menu.zul");
			iframe.setParent(window);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		initTree();
		onReloadTree(null);

		Common.initLaguage();
	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("");
		treecol.setWidth("65%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("10%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("10%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("15%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onAdd(Event event) throws Exception {

		Menu mymenu = new Menu();
		init(mymenu, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(arg0);
			}
		});
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Menu menu, final EventListener eventListener) {
		this.menu = menu;
		addWindow.setTitle(menu.getId() == null ? "Tambah Menu" : "Ubah Menu");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("ID Menu *"));
		row.appendChild(id = new Longbox(menu.getId()));
		id.setWidth("90%");
		id.setDisabled(menu.getId() != null);

		Common.initKeterangan(rows, "ID menu tidak boleh sama");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Root Menu *"));
		row.appendChild(root = new Longbox(menu.getRoot()));
		root.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Child Menu *"));
		row.appendChild(child = new Longbox(menu.getChild()));
		child.setWidth("90%");

		Common.initKeterangan(rows, "Child menu tidak boleh sama");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Label Menu *"));
		row.appendChild(label = new Textbox(menu.getLabel()));
		label.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("URL"));
		row.appendChild(url = new Textbox(menu.getUrl()));
		url.setWidth("90%");
		url.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Icon"));
		row.appendChild(bigIcon = new Textbox(menu.getBigIcon()));
		bigIcon.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(aktif = new MyCheckboxConfig());
		aktif.setChecked(menu.getAktif() == null || menu.getAktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampil Di Perguruan Tinggi"));
		row.appendChild(tampilDiPt = new MyCheckboxConfig());
		tampilDiPt.setChecked(menu.getTampilDiPt());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampil Di Sekolah"));
		row.appendChild(tampilDiSekolah = new MyCheckboxConfig());
		tampilDiSekolah.setChecked(menu.getTampilDiSekolah());
		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Buka Halaman Baru"));
		row.appendChild(bukaHalamanBaru = new MyCheckboxConfig());
		bukaHalamanBaru.setChecked(menu.getBukaHalamanBaru());
		

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
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
					eventListener.onEvent(new Event("", null, MenuAction.this.menu));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (id.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, ID Menu belum diisi. Langkah yang dapat dilakukan: (1) isi kolom ID Menu dengan kode identifikasi yang unik; (2) pastikan ID tidak mengandung karakter khusus atau spasi; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (menu.getId() == null && checkId()) {
			MyMessageboxConfig.show("Mohon maaf, ID Menu yang dimasukkan sudah ada di sistem. Langkah yang dapat dilakukan: (1) ganti ID Menu dengan nilai yang berbeda dan unik; (2) periksa daftar menu yang sudah ada untuk menghindari duplikasi; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (root.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Root Menu belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Root Menu dengan nilai root yang sesuai; (2) pastikan root menu yang dipilih valid dan terdaftar; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (child.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Child Menu belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Child Menu dengan nilai yang sesuai; (2) pastikan nilai child menu valid dan tidak duplikat; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (label.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Label Menu belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Label Menu dengan nama tampilan yang deskriptif; (2) pastikan label tidak kosong dan mudah dimengerti pengguna; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (menu.getId() == null && checkId()) {
			MyMessageboxConfig.show("Mohon maaf, ID Menu yang dimasukkan sudah ada di sistem. Langkah yang dapat dilakukan: (1) ganti ID Menu dengan nilai yang berbeda dan unik; (2) periksa daftar menu yang sudah ada untuk menghindari duplikasi; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (menu.getId() != null) {
			menu = (Menu) session.createCriteria(Menu.class).add(Restrictions.idEq(menu.getId())).uniqueResult();
		}

		boolean ubah = true;
		if (menu == null) {
			menu = new Menu();
			ubah = false;
		}

		menu.setId(id.getValue());
		menu.setLabel(label.getValue());
		menu.setRoot(root.getValue());
		menu.setChild(child.getValue());
		menu.setUrl(url.getValue());
		menu.setBigIcon(bigIcon.getValue());
		menu.setAktif(aktif.isChecked());
		menu.setTampilDiPt(tampilDiPt.isChecked());
		menu.setTampilDiSekolah(tampilDiSekolah.isChecked());
		menu.setBukaHalamanBaru(bukaHalamanBaru.isChecked());

		if (ubah) {
			Common.refreshSaveOrUpdate(session, menu);
		} else {
			session.save(menu);
			session.flush();
		}

		return true;
	}

	public Boolean checkId() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Menu.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("id", id.getValue())).uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkKodeMenu() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Menu.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("label", label.getValue().trim()))
				.add(this.menu.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.menu.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public void onReloadTree(Event event) throws Exception {

		menuTreeModel = new MenuTreeModel();
		tree.setModel(menuTreeModel);

		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final Menu menu = (Menu) arg1;

				try {
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					new Treecell(menu.getLabel()).setParent(treerow);

					Treecell treecell = new Treecell();
					treecell.setStyle("color:black;");
					treecell.setParent(treerow);

					final Intbox nomorUrut = new Intbox(menu.getNomorUrut());
					nomorUrut.setParent(treecell);
					nomorUrut.setWidth("90%");

					nomorUrut.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							menu.setNomorUrut(nomorUrut.getValue());
							Common.refreshUpdate(menu);
						}
					});

					treecell = new Treecell();
					treecell.setStyle("color:black;text-align: right;");

					final MyCheckboxConfig checkbox = new MyCheckboxConfig();
					checkbox.setChecked(menu.getAktif() == null || menu.getAktif());
					checkbox.setParent(treecell);
					checkbox.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							menu.setAktif(checkbox.isChecked());
							Common.refreshUpdate(session, (menu));
						}
					});

					treecell.setParent(treerow);

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();
					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/addthis.svg");
					button.setTooltiptext("Tambah Data");
					button.setVisible(add);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Menu mymenu = (Menu) menu.clone();

							Session session = HibernateUtil.currentSession();

							Number max = ((Number) session.createCriteria(Menu.class)
									.add(Restrictions.eq("root", menu.getChild()))
									.setProjection(Projections.max("child")).uniqueResult());

							mymenu.setChild(
									max == null ? Long.parseLong("" + menu.getChild() + "00") : (max.intValue() + 1));
							mymenu.setRoot(menu.getChild());

							mymenu.setId(null);
							init(mymenu, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									reloadTreeitem(treeitem, true);

								}
							});
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
					button.setTooltiptext("Copy Data");
					button.setVisible(edit & add);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Session session = HibernateUtil.currentSession();
							Number max = ((Number) session.createCriteria(Menu.class)
									.add(Restrictions.eq("root", menu.getRoot()))
									.setProjection(Projections.max("child")).uniqueResult()).longValue();

							max = max == null ? 0 : max;

							Menu mymenu = (Menu) menu.clone();
							mymenu.setRoot(menu.getRoot());

							mymenu.setChild(
									max == null ? Long.parseLong("" + menu.getRoot() + "00") : (max.intValue() + 1));

							mymenu.setId(null);
							init(mymenu, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									reloadTreeitem(treeitem, true);
								}
							});
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(menu, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									reloadTreeitem(treeitem, true);

								}
							});
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
					button.setTooltiptext("Hapus Data");
					button.setVisible(delete && menuTreeModel.getChildCount(menu) == 0);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													Session session = HibernateUtil.currentSession();

													Common.refreshDelete(session, (menu));

													reloadTreeitem(treeitem, true);
												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
													PesanFormalHelper.tampilkanGagalException("penghapusan data Menu", e,
															new String[] {
																	"Periksa apakah menu ini masih berelasi dengan data lain (misalnya hak akses/Role Privilege atau sub-menu) sehingga tidak dapat dihapus.",
																	"Hapus terlebih dahulu sub-menu atau relasi hak akses yang menggunakan menu ini, lalu ulangi penghapusan.",
																	"Hubungi Administrator Sistem apabila menu ini memang harus dihapus." });
												}

											}

										}
									});

						}
					});
					button.setParent(toolbar);
					ais.ui.util.MenuAksiBaris.pasang(toolbar);
					toolbar.setParent(arg0);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});
	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean loadParent) {
		final Treeitem treeitemParent = loadParent ? treeitem.getParentItem() : treeitem;
		if (treeitemParent == null) {
			try {
				onReloadTree(null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else {
			treeitemParent.unload();
			final Timer timer = new Timer(200);
			timer.setParent(page.getFirstRoot());
			timer.addEventListener("onTimer", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event arg0) throws Exception {
					treeitemParent.setOpen(true);
					treeitem.setOpen(true);
					timer.detach();
				}
			});

			timer.start();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void openChilds(final Treeitem treeitemParent, int max, int index) {
		if (max > index) {
			treeitemParent.setOpen(true);
			List treeitems = treeitemParent.getChildren();
			for (Object object : treeitems) {

				if (object instanceof Treechildren) {
					Treechildren treechildren = (Treechildren) object;
					List<MyTreeitemConfig> mytreeitems = treechildren.getChildren();
					for (MyTreeitemConfig treeitem : mytreeitems) {
						openChilds(treeitem, max, (++index));
					}

				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void closeChilds(final Treeitem treeitemParent) {
		treeitemParent.setOpen(false);
		List treeitems = treeitemParent.getChildren();
		for (Object object : treeitems) {

			if (object instanceof Treechildren) {
				Treechildren treechildren = (Treechildren) object;
				List<MyTreeitemConfig> mytreeitems = treechildren.getChildren();
				for (MyTreeitemConfig treeitem : mytreeitems) {
					closeChilds(treeitem);
				}

			}
		}
	}

}
