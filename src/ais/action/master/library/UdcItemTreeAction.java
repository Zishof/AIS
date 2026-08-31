package ais.action.master.library;

import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.library.util.UdcItemTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.UdcItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.UdcItem;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk udc item tree. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Tree tree},
 * {@code Textbox kode}, {@code Textbox nama}, {@code Textbox keterangan}, {@code MyCheckboxConfig defaultItem},
 * {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code initTree()}, {@code init()}); pembacaan/pencarian ({@code onReloadTree()}, {@code
 * reloadTreeitem()}, {@code reloadTreeitem()}, {@code reloadTotal()}); mutasi data ({@code onSave()}); operasi
 * domain lain ({@code onAdd()}, {@code openChilds()}, {@code closeChilds()}, {@code hasSomeChilds()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class UdcItemTreeAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;

	private Tree tree;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;
	private MyCheckboxConfig defaultItem;

	private boolean edit = false;
	private boolean delete = false;

	private UdcItem udcItem;
	private boolean add = false;
	private UdcItemTreeModel udcItemTreeModel;

	private TreeMap<UdcItem, Treecell[]> treecellMap = new TreeMap<UdcItem, Treecell[]>();

	private MyToolbarbuttonConfig addNew;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		// System.out.println("add = " + add + ", edit = " + edit +
		// ", delete = "
		// + delete);

		if (addNew != null) { addNew.setVisible(add); }

		initTree();

		onReloadTree(null);

	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Udc");
		treecol.setParent(treecols);

		treecol = new Treecol("Aktif");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("15%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onAdd(Event event) throws Exception {

		UdcItem myudcItem = new UdcItem();
		myudcItem.setParent(null);

		init(myudcItem, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Timer timer = new Timer(500);
				timer.setParent(page.getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						timer.detach();
						onReloadTree(arg0);
					}
				});
				timer.start();
			}
		});
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(UdcItem udcItem, final EventListener eventListener) {
		this.udcItem = udcItem;
		addWindow.setTitle(udcItem.getId() == null ? "Tambah Standar Biaya" : "Ubah Standar Biaya");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode DDC"));
		row.appendChild(kode = new Textbox(udcItem.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama DDC"));
		row.appendChild(nama = new Textbox(udcItem.getNama() == null ? ""
				: udcItem.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(defaultItem = new MyCheckboxConfig());
		defaultItem.setChecked(udcItem.getDefaultItem() != null
				&& udcItem.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				udcItem.getKeterangan() == null ? "" : udcItem.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
					// onReloadTree(null);
					eventListener.onEvent(new Event("", null,
							UdcItemTreeAction.this.udcItem));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	public boolean onSave(Event event) throws Exception {

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		UdcItemDao udcItemDao = DaoFactory.getInstance().getUdcItemDao();
		if (udcItem.getId() != null) {
			udcItem = udcItemDao.load(udcItem.getId());
		}

		udcItem.setDefaultItem(defaultItem.isChecked());
		udcItem.setNama(nama.getValue());
		udcItem.setKode(kode.getValue());
		udcItem.setKeterangan(keterangan.getValue());

		if (udcItem.getId() != null) {
			udcItemDao.update(udcItem);
		} else {
			udcItemDao.save(udcItem);
		}

		return true;
	}

	public void onReloadTree(Event event) throws Exception {
		addNew.setVisible(add);
		udcItemTreeModel = new UdcItemTreeModel(true);
		tree.setModel(udcItemTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1)
					throws Exception {
				final UdcItem udcItem = (UdcItem) arg1;
				treeitem.setImage("/img/dir.gif");
				try {
					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					hasSomeChilds(treerow, udcItem);

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
							"/img/reply.png");
					button.setTooltiptext("Refresh");
					// button.setVisible(hasChild);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadTreeitem(treeitem, true, false);
						}
					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/addthis.svg");
					button.setTooltiptext("Tambah Data");
					button.setVisible(add);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							UdcItem myudcItem = (UdcItem) udcItem.clone();
							myudcItem.setParent(udcItem);
							myudcItem.setId(null);
							init(myudcItem, new EventListener() {

								@Override
								public void onEvent(Event arg0)
										throws Exception {
									render(treeitem, udcItem);
									reloadTreeitem(treeitem, true, true,
											new EventListener() {

												@Override
												public void onEvent(Event arg0)
														throws Exception {
													final Timer timer = new Timer(
															300);
													timer.setParent(page
															.getFirstRoot());
													timer.addEventListener(
															"onTimer",
															new EventListener() {

																@SuppressWarnings({})
																@Override
																public void onEvent(
																		Event arg0)
																		throws Exception {
																	System.out
																			.println("======= open tree item =======");

																	try {
																		Treeitem myTreeitem = (Treeitem) treecellMap
																				.get(udcItem)[0]
																				.getParent()
																				.getParent();

																		render(myTreeitem,
																				udcItem);

																		reloadTreeitem(
																				myTreeitem,
																				true,
																				false,
																				new EventListener() {

																					@Override
																					public void onEvent(
																							Event arg0)
																							throws Exception {

																						final Timer timer = new Timer(
																								300);
																						timer.setParent(page
																								.getFirstRoot());
																						timer.addEventListener(
																								"onTimer",
																								new EventListener() {

																									@Override
																									public void onEvent(
																											Event arg0)
																											throws Exception {

																										System.out
																												.println("========================= RELOAD TOTAL ===========================");

																										Treeitem myTreeitem = (Treeitem) treecellMap
																												.get(udcItem)[0]
																												.getParent()
																												.getParent();
																										reloadTreeitem(
																												myTreeitem,
																												true,
																												false);

																										timer.detach();
																									}

																								});
																						timer.start();

																					}
																				});

																	} catch (Exception e) {
																		// TODO
																		// Auto-generated
																		// catch
																		// block
																		Common.tampilErrorJikaAdmin(e); 
																	}

																	timer.detach();
																}
															});

													timer.start();

												}
											});
								}
							});

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

							UdcItem myudcItem = (UdcItem) udcItem.clone();
							myudcItem.setParent(udcItem.getParent());
							myudcItem.setId(null);
							init(myudcItem, new EventListener() {

								@Override
								public void onEvent(Event arg0)
										throws Exception {

									reloadTreeitem(treeitem, true, true);
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
							init(udcItem, new EventListener() {

								@Override
								public void onEvent(Event arg0)
										throws Exception {

									reloadTreeitem(treeitem, false, true);

								}
							});
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
							MyMessageboxConfig.show(
									"Apakah yakin ingin menghapus data ini ?",
									"Question", MyMessageboxConfig.OK
											| MyMessageboxConfig.CANCEL,
									MyMessageboxConfig.QUESTION, new EventListener() {

										@Override
										public void onEvent(Event event)
												throws Exception {
											int i = new Integer(event.getData()
													.toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													udcItemTreeModel
															.deleteChilds(udcItem);

													Common.refreshDelete(udcItem);

													reloadTreeitem(treeitem,
															true, true);
												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e); 
													MyMessageboxConfig.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
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

	private void reloadTreeitem(final Treeitem treeitem,
			final Boolean reloadTotal, final Boolean loadParent) {
		reloadTreeitem(treeitem, reloadTotal, loadParent, null);
	}

	private void reloadTreeitem(final Treeitem treeitem,
			final Boolean reloadTotal, final Boolean loadParent,
			final EventListener eventListener) {
		final Treeitem treeitemParent = loadParent ? treeitem.getParentItem()
				: treeitem;
		if (treeitemParent == null) {
			try {
				onReloadTree(null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}
		} else {
			treeitemParent.unload();
			final Timer timer = new Timer(300);
			timer.setParent(page.getFirstRoot());
			timer.addEventListener("onTimer", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event arg0) throws Exception {
					treeitemParent.setOpen(true);
					treeitem.setOpen(true);
					if (reloadTotal) {
						reloadTotal();
					}
					if (eventListener != null) {
						eventListener.onEvent(null);
					}
					timer.detach();
				}

			});

			timer.start();
		}
	}

	private void reloadTotal() {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings({ "unchecked" })
	public void openChilds(final Treeitem treeitemParent, int max, int index) {
		if (max > index) {
			treeitemParent.setOpen(true);
			List<MyTreeitemConfig> treeitems = treeitemParent.getChildren();
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

	@SuppressWarnings({ "unchecked" })
	public void closeChilds(final Treeitem treeitemParent) {
		treeitemParent.setOpen(false);
		List<MyTreeitemConfig> treeitems = treeitemParent.getChildren();
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

	private void hasSomeChilds(Treerow treerow, final UdcItem udcItem) {

		Treecell treecell = new Treecell(udcItem.toString());
		treecell.setTooltiptext(udcItem.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		Treecell treecellAktif = new Treecell(udcItem.getDefaultItem() == null
				|| !udcItem.getDefaultItem() ? "Tidak" : "Aktif");

		if (edit) {
			treecellAktif.setLabel("");
			final MyCheckboxConfig defaultItem;
			treecellAktif.appendChild(defaultItem = new MyCheckboxConfig());
			defaultItem.setChecked(udcItem.getDefaultItem() != null
					&& udcItem.getDefaultItem());
			defaultItem.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					udcItem.setDefaultItem(defaultItem.isChecked());
					session.update(udcItem);
				}
			});
		}
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecellMap.put(udcItem, new Treecell[] { treecellAktif });

	}

}
