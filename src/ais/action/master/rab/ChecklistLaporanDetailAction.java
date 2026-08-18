package ais.action.master.rab;

import java.util.List;
import java.util.TreeMap;

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

import ais.action.master.rab.helper.AmbilDataChecklistLaporanBanbox;
import ais.action.master.rab.util.ChecklistLaporanDetailTreeModel;
import ais.action.report.format1.rab.LaporanChecklist;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.ChecklistLaporanDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoBuktiChecklistLaporan;
import ais.database.model.rab.ChecklistLaporan;
import ais.database.model.rab.ChecklistLaporanDetail;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

public class ChecklistLaporanDetailAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;

	private Tree tree;
	private AmbilDataChecklistLaporanBanbox checklistLaporan;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;
	private MyCheckboxConfig diperlukan;

	private boolean edit = false;
	private boolean delete = false;

	private ChecklistLaporanDetail checklistLaporanDetail;
	private boolean add = false;
	private ChecklistLaporanDetailTreeModel checklistLaporanDetailTreeModel;

	private TreeMap<ChecklistLaporanDetail, Treecell[]> treecellMap = new TreeMap<ChecklistLaporanDetail, Treecell[]>();

	private MyToolbarbuttonConfig addNew;
	private MyCheckboxConfig ada;
	private ChecklistLaporan sessionChecklistLaporan;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
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

		if (session.getAttribute("checklistLaporan") != null) {
			sessionChecklistLaporan = (ChecklistLaporan) session.getAttribute("checklistLaporan");
			checklistLaporan.setValue(sessionChecklistLaporan.toString());
			checklistLaporan.setAttribute("checklistLaporan", sessionChecklistLaporan);
			checklistLaporan.setDisabled(true);
		}

		checklistLaporan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(null);
			}
		});

		onReloadTree(null);

	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Kode/Nama Item");
		treecol.setParent(treecols);

		treecol = new Treecol("Diperlukan");
		treecol.setWidth("10%");
		treecol.setParent(treecols);

		treecol = new Treecol("Ada");
		treecol.setWidth("10%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("17%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onAdd(Event event) throws Exception {

		ChecklistLaporanDetail mychecklistLaporanDetail = new ChecklistLaporanDetail();
		mychecklistLaporanDetail.setParent(null);

		init(mychecklistLaporanDetail, new EventListener() {

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

	private void init(ChecklistLaporanDetail checklistLaporanDetail, final EventListener eventListener)
			throws Exception {
		this.checklistLaporanDetail = checklistLaporanDetail;
		addWindow.setTitle(checklistLaporanDetail.getId() == null ? "Tambah Checklist Laporan" : "Ubah Checklist Laporan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(checklistLaporanDetail.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(
				nama = new Textbox(checklistLaporanDetail.getNama() == null ? "" : checklistLaporanDetail.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diperlukan"));
		row.appendChild(diperlukan = new MyCheckboxConfig());
		diperlukan.setChecked(checklistLaporanDetail.getDiperlukan() != null && checklistLaporanDetail.getDiperlukan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ada"));
		row.appendChild(ada = new MyCheckboxConfig());
		ada.setChecked(checklistLaporanDetail.getAda() != null && checklistLaporanDetail.getAda());
		EventListener listener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (diperlukan.isChecked()) {
					ada.setChecked(true);
					ada.setDisabled(true);
				} else {
					ada.setDisabled(false);
				}
			}
		};
		listener.onEvent(null);
		diperlukan.addEventListener("onCheck", listener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				checklistLaporanDetail.getKeterangan() == null ? "" : checklistLaporanDetail.getKeterangan()));
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
					eventListener
							.onEvent(new Event("", null, ChecklistLaporanDetailAction.this.checklistLaporanDetail));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	public void onCetak(Event event) throws Exception {
		LaporanChecklist laporanChecklist = new LaporanChecklist(
				(ChecklistLaporan) checklistLaporan.getAttribute("checklistLaporan"));
		laporanChecklist.setTitle("Checklist Laporan");
		page.getFirstRoot().appendChild(laporanChecklist);
		laporanChecklist.setHeight("95%");
		laporanChecklist.setWidth("90%");
		laporanChecklist.setClosable(true);
		laporanChecklist.onModal();
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

		// boolean i = checkKodeChecklistLaporanDetail();
		// if (i) {
		// MyMessageboxConfig.show("Kode sudah ada di database", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		ChecklistLaporanDetailDao checklistLaporanDetailDao = DaoFactory.getInstance().getChecklistLaporanDetailDao();
		if (checklistLaporanDetail.getId() != null) {
			checklistLaporanDetail = checklistLaporanDetailDao.load(checklistLaporanDetail.getId());
		}

		checklistLaporanDetail.setAda(ada.isChecked());
		checklistLaporanDetail.setDiperlukan(diperlukan.isChecked());
		checklistLaporanDetail.setKode(kode.getValue());
		checklistLaporanDetail.setNama(nama.getValue());
		checklistLaporanDetail.setKeterangan(keterangan.getValue());
		checklistLaporanDetail
				.setChecklistLaporan((ChecklistLaporan) checklistLaporan.getAttribute("checklistLaporan"));

		if (checklistLaporanDetail.getId() != null) {
			checklistLaporanDetailDao.update(checklistLaporanDetail);
		} else {
			checklistLaporanDetailDao.save(checklistLaporanDetail);
		}

		return true;
	}

	public Boolean checkKodeChecklistLaporanDetail() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(ChecklistLaporanDetail.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.checklistLaporanDetail.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.checklistLaporanDetail.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public void onReloadTree(Event event) throws Exception {
		addNew.setVisible(add);
		checklistLaporanDetailTreeModel = new ChecklistLaporanDetailTreeModel(
				(ChecklistLaporan) checklistLaporan.getAttribute("checklistLaporan"));
		tree.setModel(checklistLaporanDetailTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final ChecklistLaporanDetail checklistLaporanDetail = (ChecklistLaporanDetail) arg1;

				try {
					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					hasSomeChilds(treerow, checklistLaporanDetail);

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();

					Integer count = 0;
					Session streamSession = StreamingHibernateUtil.getInstance().currentSession();
					count = ((Number) streamSession.createCriteria(FotoBuktiChecklistLaporan.class)
							.add(Restrictions.eq("checklistLaporanDetail", checklistLaporanDetail.getId()))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					StreamingHibernateUtil.getInstance().closeSession();

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("[" + count + "]", "/img/attachment-icon.png");
					button.setTooltiptext("Lampiran");
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							final MyWindow window = new MyWindow("Daftar Lampiran" + checklistLaporanDetail, "none",
									true);
							page.getFirstRoot().appendChild(window);

							window.setHeight("90%");
							window.setWidth("100%");
							window.setClosable(false);

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							borderlayout.setParent(window);
							Center center = new Center();
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);

							session.setAttribute("checklistLaporanDetail", checklistLaporanDetail);

							MyIframe include = new MyIframe("/pages/master/rab/foto_bukti_checklist_laporan.zul");
							center.appendChild(include);

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setHeight("30px");
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
							cancel.setParent(toolbar);
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									render(treeitem, checklistLaporanDetail);
									window.detach();
								}
							});

							window.onModal();
						}
					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
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

							ChecklistLaporanDetail mychecklistLaporanDetail = (ChecklistLaporanDetail) checklistLaporanDetail
									.clone();
							mychecklistLaporanDetail.setParent(checklistLaporanDetail);
							mychecklistLaporanDetail.setId(null);
							init(mychecklistLaporanDetail, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									render(treeitem, checklistLaporanDetail);
									reloadTreeitem(treeitem, true, true, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											final Timer timer = new Timer(300);
											timer.setParent(page.getFirstRoot());
											timer.addEventListener("onTimer", new EventListener() {

												@SuppressWarnings({})
												@Override
												public void onEvent(Event arg0) throws Exception {
													System.out.println("======= open tree item =======");

													try {
														Treeitem myTreeitem = (Treeitem) treecellMap
																.get(checklistLaporanDetail)[0].getParent().getParent();

														render(myTreeitem, checklistLaporanDetail);

														reloadTreeitem(myTreeitem, true, false, new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {

																final Timer timer = new Timer(300);
																timer.setParent(page.getFirstRoot());
																timer.addEventListener("onTimer", new EventListener() {

																	@Override
																	public void onEvent(Event arg0) throws Exception {

																		System.out.println(
																				"========================= RELOAD TOTAL ===========================");

																		Treeitem myTreeitem = (Treeitem) treecellMap
																				.get(checklistLaporanDetail)[0]
																						.getParent().getParent();
																		reloadTreeitem(myTreeitem, true, false);

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

							ChecklistLaporanDetail mychecklistLaporanDetail = (ChecklistLaporanDetail) checklistLaporanDetail
									.clone();
							mychecklistLaporanDetail.setParent(checklistLaporanDetail.getParent());
							mychecklistLaporanDetail.setId(null);
							init(mychecklistLaporanDetail, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

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
							init(checklistLaporanDetail, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

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
							MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {


											checklistLaporanDetailTreeModel.deleteChilds(checklistLaporanDetail);

											Common.refreshDelete((checklistLaporanDetail));

											reloadTreeitem(treeitem, true, true);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
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

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotal, final Boolean loadParent) {
		reloadTreeitem(treeitem, reloadTotal, loadParent, null);
	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotal, final Boolean loadParent,
			final EventListener eventListener) {
		final Treeitem treeitemParent = loadParent ? treeitem.getParentItem() : treeitem;
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

	private void hasSomeChilds(Treerow treerow, final ChecklistLaporanDetail checklistLaporanDetail) throws Exception {

		Treecell treecell = new Treecell(checklistLaporanDetail.toString());
		treecell.setTooltiptext(checklistLaporanDetail.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		Treecell treecellDiperlukan = new Treecell(
				checklistLaporanDetail.getDiperlukan() == null || !checklistLaporanDetail.getDiperlukan() ? "Tidak"
						: "Ya");

		Treecell treecellAda = new Treecell(
				checklistLaporanDetail.getAda() == null || !checklistLaporanDetail.getAda() ? "Tidak" : "Ya");

		if (edit) {
			treecellDiperlukan.setLabel("");
			final MyCheckboxConfig diperlukan;
			treecellDiperlukan.appendChild(diperlukan = new MyCheckboxConfig());
			diperlukan.setChecked(
					checklistLaporanDetail.getDiperlukan() != null && checklistLaporanDetail.getDiperlukan());

			treecellAda.setLabel("");
			final MyCheckboxConfig ada;
			treecellAda.appendChild(ada = new MyCheckboxConfig());
			ada.setChecked(checklistLaporanDetail.getAda() != null && checklistLaporanDetail.getAda());

			EventListener listener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (diperlukan.isChecked()) {
						ada.setChecked(true);
						ada.setDisabled(true);
					} else {
						ada.setDisabled(false);
					}

					Session session = HibernateUtil.currentSession();
					checklistLaporanDetail.setAda(ada.isChecked());
					checklistLaporanDetail.setDiperlukan(diperlukan.isChecked());
					session.update(checklistLaporanDetail);
				}
			};
			diperlukan.addEventListener("onCheck", listener);
			ada.addEventListener("onCheck", listener);
			if (diperlukan.isChecked()) {
				ada.setChecked(true);
				ada.setDisabled(true);
			} else {
				ada.setDisabled(false);
			}
		}
		treecellDiperlukan.setStyle("font-size:x-small;text-align: left;");
		treecellDiperlukan.setParent(treerow);

		treecellAda.setStyle("font-size:x-small;text-align: left;");
		treecellAda.setParent(treerow);

		treecellMap.put(checklistLaporanDetail, new Treecell[] { treecellDiperlukan, treecellAda });

	}

}
