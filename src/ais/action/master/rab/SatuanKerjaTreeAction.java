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
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.rab.helper.AmbilDataKppnBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanLokasiBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.SatuanKerjaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Kppn;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SatuanLokasi;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

public class SatuanKerjaTreeAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;

	private Tree tree;

	private Textbox kode;
	private Textbox nama;
	private AmbilDataKppnBanbox kppn;
	private AmbilDataSatuanLokasiBanbox satuanLokasi;
	private Textbox keterangan;
	private Textbox alamat;
	private MyCheckboxConfig defaultItem;

	private boolean edit = false;
	private boolean delete = false;

	private SatuanKerja satuanKerja;
	private boolean add = false;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private MyCheckboxConfig searchaktif;

	private TreeMap<SatuanKerja, Treecell[]> treecellMap = new TreeMap<SatuanKerja, Treecell[]>();

	private MyToolbarbuttonConfig addNew;
	private Yayasan yayasan = null;
	protected LampiranLain kop;
	protected LampiranLain kopBawah;

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

		yayasan = SekolahUtil.getYayasan();

		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (addNew != null) { addNew.setVisible(add); }

		initTree();

		onReloadTree(null);

	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Satuan Kerja");
		treecol.setParent(treecols);

		treecol = new Treecol("Lokasi");
		treecol.setWidth("15%");
		treecol.setParent(treecols);

		treecol = new Treecol("KPPN");
		treecol.setWidth("0%");
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

		SatuanKerja mysatuanKerja = new SatuanKerja();
		mysatuanKerja.setParent(null);

		init(mysatuanKerja, new EventListener() {

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

	private void init(SatuanKerja satuanKerja, final EventListener eventListener) {
		this.satuanKerja = satuanKerja;
		addWindow.setTitle(satuanKerja.getId() == null ? "Tambah Satuan Kerja" : "Ubah Satuan Kerja");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(satuanKerja.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(satuanKerja.getNama() == null ? "" : satuanKerja.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));
		row.appendChild(satuanLokasi = new AmbilDataSatuanLokasiBanbox());
		satuanLokasi.setValue(satuanKerja.getSatuanLokasi() == null ? "" : satuanKerja.getSatuanLokasi().toString());
		satuanLokasi.setAttribute("satuanLokasi", satuanKerja.getSatuanLokasi());
		satuanLokasi.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KPPN"));
		row.appendChild(kppn = new AmbilDataKppnBanbox());
		kppn.setValue(satuanKerja.getKppn() == null ? "" : satuanKerja.getKppn().toString());
		kppn.setAttribute("kppn", satuanKerja.getKppn());
		kppn.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(defaultItem = new MyCheckboxConfig());
		defaultItem.setChecked(satuanKerja.getDefaultItem() != null && satuanKerja.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Textbox(satuanKerja.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(satuanKerja.getKeterangan() == null ? "" : satuanKerja.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		
		kop = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Atas (JPG) "));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, satuanKerja.getId(), LampiranLain.KOP_SATKER, "KOP", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kop = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		kopBawah = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Bawah (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, satuanKerja.getId(), LampiranLain.KOP_BAWAH_SATKER, "KOP",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kopBawah = (LampiranLain) arg0.getData();
					}
				}); 
		hbox.setParent(row);

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
					eventListener.onEvent(new Event("", null, SatuanKerjaTreeAction.this.satuanKerja));
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

		// boolean i = checkKodeSatuanKerja();
		// if (i) {
		// MyMessageboxConfig.show("Kode sudah ada di database", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		SatuanKerjaDao satuanKerjaDao = DaoFactory.getInstance().getSatuanKerjaDao();
		if (satuanKerja.getId() != null) {
			satuanKerja = satuanKerjaDao.load(satuanKerja.getId());
		}

		satuanKerja.setAlamat(alamat.getValue());
		satuanKerja.setDefaultItem(defaultItem.isChecked());
		satuanKerja.setKode(kode.getValue());
		satuanKerja.setNama(nama.getValue());
		satuanKerja.setKeterangan(keterangan.getValue());
		satuanKerja.setKppn((Kppn) kppn.getAttribute("kppn"));
		satuanKerja.setSatuanLokasi((SatuanLokasi) satuanLokasi.getAttribute("satuanLokasi"));
		satuanKerja.setYayasan(yayasan);
		if (satuanKerja.getId() != null) {
			satuanKerjaDao.update(satuanKerja);
		} else {
			satuanKerjaDao.save(satuanKerja);
		}
		
		if (kop != null && kop.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kop);
				kop.setRef(satuanKerja.getId());

				session.getTransaction().begin();
				session.update(kop);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (kopBawah != null && kopBawah.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kopBawah);
				kopBawah.setRef(satuanKerja.getId());

				session.getTransaction().begin();
				session.update(kopBawah);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	public Boolean checkKodeSatuanKerja() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(SatuanKerja.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.satuanKerja.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.satuanKerja.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public void onReloadTree(Event event) throws Exception {
		addNew.setVisible(add);

		Tbmuser tbmuser = Common.getCurrentUser();

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(
				tbmuser != null && tbmuser.getUserRole().getMelihatDataSatkerLain() ? null : tbmuser.ambilSatuanKerja(),
				!searchaktif.isChecked());
		tree.setModel(satuanKerjaTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final SatuanKerja satuanKerja = (SatuanKerja) arg1;

				try {
					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					boolean hasChild = satuanKerjaTreeModel.getChildCount(satuanKerja) != 0;

					hasSomeChilds(treerow, satuanKerja);

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
					button.setTooltiptext("Refresh");
					button.setVisible(hasChild);
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

							SatuanKerja mysatuanKerja = (SatuanKerja) satuanKerja.clone();
							mysatuanKerja.setParent(satuanKerja);
							mysatuanKerja.setId(null);
							init(mysatuanKerja, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									render(treeitem, satuanKerja);
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
														Treeitem myTreeitem = (Treeitem) treecellMap.get(satuanKerja)[0]
																.getParent().getParent();

														render(myTreeitem, satuanKerja);

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
																				.get(satuanKerja)[0].getParent()
																				.getParent();
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

							SatuanKerja mysatuanKerja = (SatuanKerja) satuanKerja.clone();
							mysatuanKerja.setParent(satuanKerja.getParent());
							mysatuanKerja.setId(null);
							init(mysatuanKerja, new EventListener() {

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
							init(satuanKerja, new EventListener() {

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
													satuanKerjaTreeModel.deleteChilds(satuanKerja);

													Common.refreshDelete((satuanKerja));

													reloadTreeitem(treeitem, true, true);
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

	private void hasSomeChilds(Treerow treerow, final SatuanKerja satuanKerja) {

		Treecell treecell = new Treecell(satuanKerja.toString());
		treecell.setTooltiptext(satuanKerja.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		Treecell treecellDurasi = new Treecell(
				satuanKerja.getSatuanLokasi() == null ? "" : satuanKerja.getSatuanLokasi().toString());
		treecellDurasi.setStyle("font-size:x-small;text-align: left;");
		treecellDurasi.setParent(treerow);

		Treecell treecellMulai = new Treecell(satuanKerja.getKppn() == null ? "" : satuanKerja.getKppn().toString());
		treecellMulai.setStyle("font-size:x-small;text-align: left;");
		treecellMulai.setParent(treerow);

		Treecell treecellAktif = new Treecell(
				satuanKerja.getDefaultItem() == null || !satuanKerja.getDefaultItem() ? "Tidak" : "Aktif");

		if (edit) {
			treecellAktif.setLabel("");
			final MyCheckboxConfig defaultItem;
			treecellAktif.appendChild(defaultItem = new MyCheckboxConfig());
			defaultItem.setChecked(satuanKerja.getDefaultItem() != null && satuanKerja.getDefaultItem());
			defaultItem.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					satuanKerja.setDefaultItem(defaultItem.isChecked());
					session.update(satuanKerja);
				}
			});
		}
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecellMap.put(satuanKerja, new Treecell[] { treecellDurasi, treecellMulai, treecellAktif });

	}

}
