package ais.action.master.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Filedownload;
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
import org.zkoss.zul.Treerow;

import com.lowagie.text.pdf.PdfCopyFields;
import com.lowagie.text.pdf.PdfReader;

import ais.action.master.library.helper.AmbilDataItemPublishBanyak;
import ais.action.master.library.util.ItemTreeModel;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.resources.helper.PerpustakaanResourcesHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.ItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoItem;
import ais.database.model.library.Item;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

public class ItemTreeAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Tree tree;
	private MyWindow addWindow;

	private Decimalbox urutan;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private boolean add = false;
	private ItemTreeModel itemTreeModel;

	private TreeMap<Item, Treecell[]> treecellMap = new TreeMap<Item, Treecell[]>();

	private MyToolbarbuttonConfig addNew;

	private Item item;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		LibraryUtil.checkDirectory();
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (addNew != null) { addNew.setVisible(add); }

		initTree();

		onReloadTree(null);

		if (satuanKerja != null) { satuanKerja.setChooseAll(false); }
		satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(null);
			}
		});
	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Judul");
		treecol.setParent(treecols);

		treecol = new Treecol("Aktif");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("25%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onAdd(Event event) throws Exception {

		Item myitem = new Item();
		myitem.setParent(null);

		init(myitem, new EventListener() {

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

	}

	public void onReloadTree(Event event) throws Exception {
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return;
		}

		addNew.setVisible(add);
		itemTreeModel = new ItemTreeModel(true, LibraryUtil.KARYA_ILMIAH,
				(SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		tree.setModel(itemTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1)
					throws Exception {
				final Item item = (Item) arg1;
				if (item == null) {
					treeitem.detach();
					return;
				}

				try {
					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					if (item.getFolder() != null && item.getFolder()) {
						treerow.setImage("/img/dir.gif");
					} else {
						treerow.setImage("/img/doc.png");
					}

					hasSomeChilds(treerow, item);

					if (!item.getFolder()
							&& !item.getStatusTerbitItem().getId()
									.equals(LibraryUtil.PUBLISH.getId())) {
						treerow.setStyle("background-color: rgba(205,92,92,0.4);color:yellow;");
					}

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("",
							"/img/reply.png");
					button.setVisible(item.getFolder());
					button.setTooltiptext("Refresh");
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadTreeitem(treeitem, true, false);
						}
					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("",
							"/img/stock_data_edit_table.png");
					button.setTooltiptext("Tambah Karya Ilmiah");
					button.setVisible(add && edit && item.getFolder());
					button.addEventListener("onClick", new EventListener() {
						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event event) throws Exception {

							Session session = HibernateUtil.currentSession();
							List<Item> items = session
									.createCriteria(Item.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("folder", false))
									.add(Restrictions.eq("parent", item))
									.list();

							AmbilDataItemPublishBanyak ambilDataItemBanyak = new AmbilDataItemPublishBanyak(
									items);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage()
									.getFirstRoot()
									.appendChild(ambilDataItemBanyak);
							ambilDataItemBanyak
									.setEventListener(new EventListener() {

										@Override
										public void onEvent(Event arg0)
												throws Exception {
											List<Item> items = (List<Item>) arg0
													.getData();
											Session session = HibernateUtil
													.currentSession();
											for (Item myitem : items) {
												myitem.setParent(item);
												session.update(myitem);
											}

											render(treeitem, item);
											reloadTreeitem(treeitem, true,
													true, new EventListener() {

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

																		@SuppressWarnings({})
																		@Override
																		public void onEvent(
																				Event arg0)
																				throws Exception {
																			System.out
																					.println("======= open tree item =======");

																			try {
																				Treeitem myTreeitem = (Treeitem) treecellMap
																						.get(item)[0]
																						.getParent()
																						.getParent();

																				render(myTreeitem,
																						item);

																				reloadTreeitem(
																						myTreeitem,
																						true,
																						false,
																						new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {

																								// final
																								// Timer
																								// timer
																								// =
																								// new
																								// Timer(
																								// 300);
																								// timer.setParent(page
																								// .getFirstRoot());
																								// timer.addEventListener(
																								// "onTimer",
																								// new
																								// EventListener()
																								// {
																								//
																								// @Override
																								// public
																								// void
																								// onEvent(
																								// Event
																								// arg0)
																								// throws
																								// Exception
																								// {
																								//
																								// System.out
																								// .println("========================= RELOAD TOTAL ===========================");
																								//
																								// MyTreeitemConfig
																								// myTreeitem
																								// =
																								// (MyTreeitemConfig)
																								// treecellMap
																								// .get(item)[0]
																								// .getParent()
																								// .getParent();
																								// reloadTreeitem(
																								// myTreeitem,
																								// true,
																								// false);
																								//
																								// timer.detach();
																								// }
																								//
																								// });
																								// timer.start();

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
							ambilDataItemBanyak.setWidth("97%");
							ambilDataItemBanyak.setHeight("97%");
							ambilDataItemBanyak.setVisible(true);
							ambilDataItemBanyak.onModal();

						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/addthis.svg");
					button.setTooltiptext("Tambah Data");
					button.setVisible(add && item.getFolder());
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Item myitem = (Item) item.clone();
							myitem.setParent(item);
							myitem.setId(null);
							init(myitem, new EventListener() {

								@Override
								public void onEvent(Event arg0)
										throws Exception {
									render(treeitem, item);
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
																				.get(item)[0]
																				.getParent()
																				.getParent();

																		render(myTreeitem,
																				item);

																		reloadTreeitem(
																				myTreeitem,
																				true,
																				false,
																				new EventListener() {

																					@Override
																					public void onEvent(
																							Event arg0)
																							throws Exception {

																						// final
																						// Timer
																						// timer
																						// =
																						// new
																						// Timer(
																						// 300);
																						// timer.setParent(page
																						// .getFirstRoot());
																						// timer.addEventListener(
																						// "onTimer",
																						// new
																						// EventListener()
																						// {
																						//
																						// @Override
																						// public
																						// void
																						// onEvent(
																						// Event
																						// arg0)
																						// throws
																						// Exception
																						// {
																						//
																						// System.out
																						// .println("========================= RELOAD TOTAL ===========================");
																						//
																						// MyTreeitemConfig
																						// myTreeitem
																						// =
																						// (MyTreeitemConfig)
																						// treecellMap
																						// .get(item)[0]
																						// .getParent()
																						// .getParent();
																						// reloadTreeitem(
																						// myTreeitem,
																						// true,
																						// false);
																						//
																						// timer.detach();
																						// }
																						//
																						// });
																						// timer.start();

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

						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
					button.setTooltiptext("Copy Data");
					button.setVisible(edit && item.getFolder());
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Item myitem = (Item) item.clone();
							myitem.setParent(item.getParent());
							myitem.setId(null);
							init(myitem, new EventListener() {

								@Override
								public void onEvent(Event arg0)
										throws Exception {

									reloadTreeitem(treeitem, true, true);
								}
							});

						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
					button.setTooltiptext("Ubah Urutan");
					button.setVisible(edit && !item.getFolder());
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							KaryaTulisItemAction.onAddExternal(event,
									new EventListener() {

										@Override
										public void onEvent(Event arg0)
												throws Exception {
											reloadTreeitem(treeitem, true, true);
										}
									}, item);

							// final MyWindow window = new MyWindow();
							// window.setClosable(true);
							// window.setParent(page.getFirstRoot());
							// window.setHeight("170px");
							// window.setWidth("500px");
							// window.setTitle("Ubah Urutan");
							// Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							// borderlayout.setParent(window);
							// Center center = new Center();
							// center.setParent(borderlayout);
							// ais.ui.util.ZkCompat.setFlex(center, true);
							// MyGrid grid = new MyGrid();grid.setWidth("100%");
							// grid.setParent(center);
							// grid.setWidth("100%");
							// grid.setHeight("100%");
							//
							// Columns columns = new Columns();
							// columns.setParent(grid);
							//
							// MyColumnConfig column = new MyColumnConfig();
							// column.setParent(columns);
							// column.setWidth("30%");
							//
							// column = new MyColumnConfig();
							// column.setParent(columns);
							//
							// Rows rows = new Rows();
							// rows.setParent(grid);
							//
							// MyFormRow row = new MyFormRow();row.setValign("top");
							//							// row.setParent(rows);
							// row.appendChild(new ais.ui.util.MyLabelConfig("Urutan ke"));
							// final Intbox urutan;
							// row.appendChild(urutan = new Intbox(item
							// .getUrutan()));
							// urutan.setWidth("90%");
							//
							// South south = new South();
							// ais.ui.util.ZkCompat.setFlex(south, true);
							// south.setParent(borderlayout);
							//
							// Toolbar toolbar = new Toolbar();
							// toolbar.setHeight("30px");
							// toolbar.setParent(south);
							// MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal",
							// "/img/cancel.gif");
							// cancel.setTooltiptext("Tutup");
							// cancel.addEventListener("onClick",
							// new EventListener() {
							// @Override
							// public void onEvent(Event event)
							// throws Exception {
							// window.detach();
							// }
							// });
							// cancel.setParent(toolbar);
							// MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan",
							// "/img/save.gif");
							// save.setTooltiptext("Simpan");
							// save.addEventListener("onClick",
							// new EventListener() {
							// @Override
							// public void onEvent(Event event)
							// throws Exception {
							//
							// if (urutan.getValue() == null) {
							// MyMessageboxConfig.show(
							// "Masukkan data urutan",
							// "Peringatan",
							// MyMessageboxConfig.OK,
							// MyMessageboxConfig.EXCLAMATION);
							// return;
							// }
							//
							// Session session = HibernateUtil
							// .currentSession();
							//
							// item.setUrutan(urutan.getValue());
							// Common.refreshUpdate(session,(item));
							//
							// reloadTreeitem(treeitem, true, true);
							// window.detach();
							// }
							// });
							// save.setParent(toolbar);
							//
							// window.onModal();

						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit && item.getFolder());
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(item, new EventListener() {

								@Override
								public void onEvent(Event arg0)
										throws Exception {

									reloadTreeitem(treeitem, false, true);

								}
							});
						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/print.png");
					button.setTooltiptext("Cetak Data");
					button.addEventListener("onClick", new EventListener() {
						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event event) throws Exception {
							try {
								Session session = HibernateUtil
										.currentSession();

								Set<Long> parents = new HashSet<Long>();
								parents.add(item.getId());
								PerpustakaanResourcesHelper perpustakaanResourcesHelper = new PerpustakaanResourcesHelper();
								perpustakaanResourcesHelper
										.generateChildsByIds(session,
												item.getId(), parents);

								List<Long> items = session
										.createCriteria(Item.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.setProjection(
												Projections.property("id"))
										.add(parents == null
												|| parents.size() == 0 ? Restrictions
												.sqlRestriction("1!=1")
												: Restrictions.in("parent.id",
														parents))
										.addOrder(Order.asc("urutan"))
										.addOrder(Order.asc("nama"))
										.addOrder(Order.desc("id")).list();

								Session streamingSession = StreamingHibernateUtil
										.getInstance().currentSession();

								List<File> files = new ArrayList<File>();
								for (Long item : items) {

									ProjectionList projectionList = Projections
											.projectionList();
									projectionList.add(Projections
											.property("id"));
									projectionList.add(Projections
											.property("nama"));

									List<Object[]> myserializables = streamingSession
											.createCriteria(FotoItem.class)
											.setProjection(projectionList)
											.add(Restrictions.eq("item", item))
											.add(Restrictions.eq("keterangan",
													"application/pdf")).list();

									for (Object[] serializables : myserializables) {
										Long strid = (Long) serializables[0];
										String myName = (String) serializables[1];
										File myfile = new File(CommonMedia
												.getMediaDirectory()
												.getAbsolutePath()
												+ "/"
												+ strid
												+ "_"
												+ FotoItem.class.getName()
												+ "_"
												+ myName.toString().replaceAll(
														" ", "_"));
										if (!myfile.exists()) {
											Blob blob = (Blob) streamingSession
													.createCriteria(
															FotoItem.class)
													.add(Restrictions
															.idEq(strid))
													.setProjection(
															Projections
																	.property("foto"))
													.setMaxResults(1)
													.uniqueResult();
											if (blob != null) {
												Common.writeBlobToFile(blob,
														myfile);
											}
										}
										files.add(myfile);
									}
								}

								StreamingHibernateUtil.getInstance()
										.closeSession();

								File hasil = new File(CommonMedia
										.getMediaDirectory().getAbsolutePath()
										+ "/"
										+ item.getId()
										+ "_"
										+ item.getNama().toString()
												.replaceAll(" ", "_") + ".pdf");
								try {
									PdfCopyFields copy = new PdfCopyFields(
											new FileOutputStream(hasil));
									copy.open();
									for (File file : files) {
										try {
											PdfReader reader = new PdfReader(
													new FileInputStream(file));
											copy.addDocument(reader);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
										}
									}
									copy.close();
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e); 
								}

								Filedownload.save(hasil, "application/pdf");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e); 
							}
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

													if (item.getFolder()) {
														itemTreeModel
																.deleteChilds(item);

														Common.refreshDelete(item);

													} else {
														item.setParent(null);

														Common.refreshUpdate(item);
													}

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
					toolbar.setParent(arg0);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}

			}
		});
	}

	private void init(Item item, final EventListener eventListener)
			throws Exception {
		this.item = item;
		addWindow.setTitle(item.getId() == null ? "Tambah Direktori" : "Ubah Direktori");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutan ke"));
		row.appendChild(urutan = new Decimalbox(
				new BigDecimal(item.getUrutan())));
		urutan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(item.getNama() == null ? "" : item
				.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				item.getKeterangan() == null ? "" : item.getKeterangan()));
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
					eventListener.onEvent(new Event("", null,
							ItemTreeAction.this.item));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Satuan kerja harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		ItemDao itemDao = DaoFactory.getInstance().getItemDao();
		if (item.getId() != null) {
			item = itemDao.load(item.getId());
		}

		item.setUrutan(urutan.getValue() == null ? null : urutan.getValue()
				.intValue());
		item.setFolder(true);
		item.setNama(nama.getValue());
		item.setKeterangan(keterangan.getValue());
		item.setDefaultSatuanKerja((SatuanKerja) satuanKerja
				.getAttribute("satuanKerja"));
		item.setTipeItem(LibraryUtil.KARYA_ILMIAH);

		if (item.getId() != null) {
			itemDao.update(item);
		} else {
			itemDao.save(item);
		}

		return true;
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

	private void hasSomeChilds(Treerow treerow, final Item item) {

		treerow.setLabel(item.toString());
		treerow.setTooltiptext(item.toString());
		treerow.setStyle("font-size:x-small;text-align: left;");

		Treecell treecellAktif = new Treecell(item.getDefaultItem() == null
				|| !item.getDefaultItem() ? "Tidak" : "Aktif");

		if (edit) {
			treecellAktif.setLabel("");
			final MyCheckboxConfig defaultItem;
			treecellAktif.appendChild(defaultItem = new MyCheckboxConfig());
			defaultItem.setChecked(item.getDefaultItem() != null
					&& item.getDefaultItem());
			defaultItem.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					item.setDefaultItem(defaultItem.isChecked());
					session.update(item);
				}
			});
		}
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecellMap.put(item, new Treecell[] { treecellAktif });

	}

}
