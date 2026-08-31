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

import ais.action.master.library.helper.AmbilDataPenerbitBanbox;
import ais.action.master.library.util.DomainPenelitianTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.DomainPenelitianDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DomainPenelitian;
import ais.database.model.library.Penerbit;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk domain penelitian tree. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Tree tree},
 * {@code AmbilDataPenerbitBanbox searchpenerbit}, {@code Textbox nama}, {@code Textbox keterangan}, {@code
 * AmbilDataPenerbitBanbox penerbit}, {@code MyCheckboxConfig defaultItem}, {@code boolean edit};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initTree()}, {@code
 * init()}); pembacaan/pencarian ({@code onReloadTree()}, {@code reloadTreeitem()}, {@code reloadTreeitem()},
 * {@code reloadTotal()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}, {@code
 * openChilds()}, {@code closeChilds()}, {@code hasSomeChilds()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class DomainPenelitianTreeAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;

	private Tree tree;
	private AmbilDataPenerbitBanbox searchpenerbit;

	private Textbox nama;
	private Textbox keterangan;
	private AmbilDataPenerbitBanbox penerbit;
	private MyCheckboxConfig defaultItem;

	private boolean edit = false;
	private boolean delete = false;

	private DomainPenelitian domainPenelitian;
	private boolean add = false;
	private DomainPenelitianTreeModel domainPenelitianTreeModel;

	private TreeMap<DomainPenelitian, Treecell[]> treecellMap = new TreeMap<DomainPenelitian, Treecell[]>();

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

		searchpenerbit.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(null);
			}
		});

		if (addNew != null) { addNew.setVisible(add); }

		initTree();

		onReloadTree(null);

	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Kategori");
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

		DomainPenelitian mydomainPenelitian = new DomainPenelitian();
		mydomainPenelitian.setParent(null);

		init(mydomainPenelitian, new EventListener() {

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

	private void init(DomainPenelitian domainPenelitian,
			final EventListener eventListener) throws Exception {
		this.domainPenelitian = domainPenelitian;
		addWindow.setTitle(domainPenelitian.getId() == null ? "Tambah Standar Biaya" : "Ubah Standar Biaya");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(
				domainPenelitian.getNama() == null ? "" : domainPenelitian
						.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit / Instansi"));
		row.appendChild(penerbit = new AmbilDataPenerbitBanbox());
		penerbit.setAttribute("penerbit", domainPenelitian.getPenerbit());
		penerbit.setValue(domainPenelitian.getPenerbit() == null ? ""
				: domainPenelitian.getPenerbit().getNama());
		penerbit.setWidth("90%");

		if (domainPenelitian.getParent() != null
				&& domainPenelitian.getParent().getPenerbit() != null) {
			penerbit.setAttribute("penerbit", domainPenelitian.getParent()
					.getPenerbit());
			penerbit.setValue(domainPenelitian.getParent().getPenerbit()
					.getNama());
			penerbit.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(defaultItem = new MyCheckboxConfig());
		defaultItem.setChecked(domainPenelitian.getDefaultItem() != null
				&& domainPenelitian.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(domainPenelitian
				.getKeterangan() == null ? "" : domainPenelitian
				.getKeterangan()));
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
							DomainPenelitianTreeAction.this.domainPenelitian));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (penerbit.getAttribute("penerbit") == null) {
			MyMessageboxConfig.show("Penerbit atau instansi penerbit harus diisi",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		DomainPenelitianDao domainPenelitianDao = DaoFactory.getInstance()
				.getDomainPenelitianDao();
		if (domainPenelitian.getId() != null) {
			domainPenelitian = domainPenelitianDao.load(domainPenelitian
					.getId());
		}

		domainPenelitian.setPenerbit((Penerbit) penerbit
				.getAttribute("penerbit"));
		domainPenelitian.setDefaultItem(defaultItem.isChecked());
		domainPenelitian.setNama(nama.getValue());
		domainPenelitian.setKeterangan(keterangan.getValue());

		if (domainPenelitian.getId() != null) {
			domainPenelitianDao.update(domainPenelitian);
		} else {
			domainPenelitianDao.save(domainPenelitian);
		}

		return true;
	}

	public void onReloadTree(Event event) throws Exception {
		addNew.setVisible(add);
		Penerbit penerbit = (Penerbit) searchpenerbit.getAttribute("penerbit");
		domainPenelitianTreeModel = new DomainPenelitianTreeModel(true,
				penerbit);
		tree.setModel(domainPenelitianTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1)
					throws Exception {
				final DomainPenelitian domainPenelitian = (DomainPenelitian) arg1;
				treeitem.setImage("/img/dir.gif");
				try {
					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					hasSomeChilds(treerow, domainPenelitian);

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

							DomainPenelitian mydomainPenelitian = (DomainPenelitian) domainPenelitian
									.clone();
							mydomainPenelitian.setParent(domainPenelitian);
							mydomainPenelitian.setId(null);
							init(mydomainPenelitian, new EventListener() {

								@Override
								public void onEvent(Event arg0)
										throws Exception {
									render(treeitem, domainPenelitian);
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
																				.get(domainPenelitian)[0]
																				.getParent()
																				.getParent();

																		render(myTreeitem,
																				domainPenelitian);

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
																												.get(domainPenelitian)[0]
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

							DomainPenelitian mydomainPenelitian = (DomainPenelitian) domainPenelitian
									.clone();
							mydomainPenelitian.setParent(domainPenelitian
									.getParent());
							mydomainPenelitian.setId(null);
							init(mydomainPenelitian, new EventListener() {

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
							init(domainPenelitian, new EventListener() {

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

													domainPenelitianTreeModel
															.deleteChilds(domainPenelitian);

													Common.refreshDelete(domainPenelitian);

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

	private void hasSomeChilds(Treerow treerow,
			final DomainPenelitian domainPenelitian) {

		Treecell treecell = new Treecell(domainPenelitian.toString());
		treecell.setTooltiptext(domainPenelitian.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		Treecell treecellAktif = new Treecell(
				domainPenelitian.getDefaultItem() == null
						|| !domainPenelitian.getDefaultItem() ? "Tidak"
						: "Aktif");

		if (edit) {
			treecellAktif.setLabel("");
			final MyCheckboxConfig defaultItem;
			treecellAktif.appendChild(defaultItem = new MyCheckboxConfig());
			defaultItem.setChecked(domainPenelitian.getDefaultItem() != null
					&& domainPenelitian.getDefaultItem());
			defaultItem.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					domainPenelitian.setDefaultItem(defaultItem.isChecked());
					session.update(domainPenelitian);
				}
			});
		}
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecellMap.put(domainPenelitian, new Treecell[] { treecellAktif });

	}

}
