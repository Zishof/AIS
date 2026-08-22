package ais.action.master.employ;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
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

import ais.action.master.employ.util.JabatanFungsionalTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.JabatanFungsionalDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.payroll.KodeTunjangan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyTreeitemConfig;
import ais.ui.util.MyWindow;

public class JabatanFungsionalTreeAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;

	private Tree tree;

	private Textbox nama;
	private Textbox keterangan;
	private MyCheckboxConfig defaultItem;

	private boolean edit = false;
	private boolean delete = false;

	private JabatanFungsional jabatanFungsional;
	private boolean add = false;
	private JabatanFungsionalTreeModel jabatanFungsionalTreeModel;

	private TreeMap<JabatanFungsional, Treecell[]> treecellMap = new TreeMap<JabatanFungsional, Treecell[]>();

	private MyToolbarbuttonConfig addNew;
	private JSONArray array;
	private Textbox kode;
	private Row rowTunjangan;

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

		onReloadTree(null);

	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Jabatan Fungsional");
		treecol.setParent(treecols);

		treecol = new Treecol("Tunjangan");
		treecol.setWidth("50%");
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

		JabatanFungsional myjabatanFungsional = new JabatanFungsional();
		myjabatanFungsional.setParent(null);

		init(myjabatanFungsional, new EventListener() {

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

	@SuppressWarnings("deprecation")
	private void init(JabatanFungsional jabatanFungsional, final EventListener eventListener) throws Exception {
		this.jabatanFungsional = jabatanFungsional;
		addWindow.setTitle(jabatanFungsional.getId() == null ? "Tambah Jabatan Fungsional" : "Ubah Jabatan Fungsional");
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
		row.appendChild(kode = new Textbox(jabatanFungsional.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(jabatanFungsional.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(defaultItem = new MyCheckboxConfig());
		defaultItem.setChecked(jabatanFungsional.getDefaultItem() != null && jabatanFungsional.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				jabatanFungsional.getKeterangan() == null ? "" : jabatanFungsional.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tunjangan"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		array = new JSONArray(jabatanFungsional.getTunjangans());
		rowTunjangan = Common.tampilanScroll1(row);
		reloadTunjangan(rowTunjangan, array, "TUNJ_FUNG", KodeTunjangan.JABATAN_FUNGSIONAL);

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
					eventListener.onEvent(new Event("", null, JabatanFungsionalTreeAction.this.jabatanFungsional));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	@SuppressWarnings("unchecked")
	public static void reloadDataTunjangan(final Row rowU, final JSONArray array, final String prefix,
			final String jenis) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Tanggal Efektif");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Kode Tunjangan");
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig("Formula Tunjangan");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Kerja Selama (thn)");
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);

		List<KodeTunjangan> kodeTunjangans = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(KodeTunjangan.class).add(Restrictions.eq("jenis", jenis))
						.add(Restrictions.eq("aktif", true)).addOrder(Order.asc("nama")),
				KodeTunjangan.class);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

//			System.out.println("jsonObject -> " + jsonObject);

			if (!jsonObject.isNull("tgl")) {

				Date tgl = new Date();
				String nilai = "0.0";
				Integer tahun = 0;

				String kode = "";

				if (!jsonObject.isNull("tgl")) {
					tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());
				}
				if (!jsonObject.isNull("nilai")) {
					nilai = jsonObject.get("nilai") + "";
				}
				if (!jsonObject.isNull("kode")) {
					kode = jsonObject.get("kode") + "";
				}
				if (!jsonObject.isNull("tahun")) {
					tahun = jsonObject.getInt("tahun");
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				final MyDatebox datebox = new MyDatebox(tgl);
				datebox.setWidth("90%");
				row.appendChild(datebox);

				final Combobox kodebox = new Combobox();

				for (KodeTunjangan kodeTunjangan : kodeTunjangans) {
					Comboitem comboitem = new Comboitem(kodeTunjangan.getNama() + " (" + kodeTunjangan.getJenis()
							+ (kodeTunjangan.getKode().isEmpty() ? "" : "_" + kodeTunjangan.getKode()) + ")");
					comboitem.setValue(kodeTunjangan.getKode());
					kodebox.appendChild(comboitem);
				}

				kodebox.setReadonly(true);
				kodebox.setWidth("90%");
				row.appendChild(kodebox);
				Common.selectComboItem(kodebox, kode);

				final Textbox doublebox = new Textbox(nilai);
				doublebox.setWidth("90%");
				doublebox.setRows(3);
				row.appendChild(doublebox);

				final MyIntbox intbox = new MyIntbox(tahun);
				intbox.setWidth("90%");
				row.appendChild(intbox);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						jsonObject.put("tgl",
								datebox.getValue() == null ? "" : Common.dateFormat1.get().format(datebox.getValue()));
						jsonObject.put("nilai", doublebox.getValue() == null ? "" : doublebox.getValue());
						jsonObject.put("tahun", intbox.getValue() == null ? "" : intbox.getValue());
						jsonObject.put("kode",
								kodebox.getSelectedItem() == null ? "" : kodebox.getSelectedItem().getValue());
					}
				};

				datebox.addEventListener("onChange", eventListener);
				doublebox.addEventListener("onChange", eventListener);
				intbox.addEventListener("onChange", eventListener);
				kodebox.addEventListener("onChange", eventListener);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												array.put(index, new JSONObject());

												reloadDataTunjangan(rowU, array, prefix, jenis);

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
				button.setParent(row);
			}
		}
	}

	public static void reloadTunjangan(final Row rowTunjangan, final JSONArray array, final String prefix,
			final String jenis) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Tunjangan", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("tgl", Common.dateFormat1.get().format(new Date()));
				jsonObject.put("nilai", 0.0);
				jsonObject.put("tahun", 0);
				jsonObject.put("kode", "");
				array.put(jsonObject);

				reloadDataTunjangan(rowU, array, prefix, jenis);
			}
		});
		button.setParent(rowTunjangan);

		rowU.setParent(rowTunjangan.getParent());

		reloadDataTunjangan(rowU, array, prefix, jenis);

	}

	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama pada form; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		JabatanFungsionalDao jabatanFungsionalDao = DaoFactory.getInstance().getJabatanFungsionalDao();
		if (jabatanFungsional.getId() != null) {
			jabatanFungsional = jabatanFungsionalDao.load(jabatanFungsional.getId());
		}

		jabatanFungsional.setDefaultItem(defaultItem.isChecked());
		jabatanFungsional.setNama(nama.getValue());
		jabatanFungsional.setKode(kode.getValue());
		jabatanFungsional.setTunjangans(array.toString());
		jabatanFungsional.setKeterangan(keterangan.getValue());

		if (jabatanFungsional.getId() != null) {
			jabatanFungsionalDao.update(jabatanFungsional);
		} else {
			jabatanFungsionalDao.save(jabatanFungsional);
		}

		return true;
	}

	public void onReloadTree(Event event) throws Exception {
		addNew.setVisible(add);
		jabatanFungsionalTreeModel = new JabatanFungsionalTreeModel(true);
		tree.setModel(jabatanFungsionalTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final JabatanFungsional jabatanFungsional = (JabatanFungsional) arg1;
				treeitem.setImage("/img/dir.gif");
				try {
					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					hasSomeChilds(treerow, jabatanFungsional);

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
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

							JabatanFungsional myjabatanFungsional = (JabatanFungsional) jabatanFungsional.clone();
							myjabatanFungsional.setParent(jabatanFungsional);
							myjabatanFungsional.setId(null);
							init(myjabatanFungsional, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									render(treeitem, jabatanFungsional);
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
																.get(jabatanFungsional)[0].getParent().getParent();

														render(myTreeitem, jabatanFungsional);

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
																				.get(jabatanFungsional)[0].getParent()
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

							JabatanFungsional myjabatanFungsional = (JabatanFungsional) jabatanFungsional.clone();
							myjabatanFungsional.setParent(jabatanFungsional.getParent());
							myjabatanFungsional.setId(null);
							init(myjabatanFungsional, new EventListener() {

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
							init(jabatanFungsional, new EventListener() {

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

													jabatanFungsionalTreeModel.deleteChilds(jabatanFungsional);

													Common.refreshDelete((jabatanFungsional));

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
					ais.ui.util.MenuAksiBaris.pasang(toolbar);
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

	private void hasSomeChilds(Treerow treerow, final JabatanFungsional jabatanFungsional) throws Exception {

		Treecell treecell = new Treecell(jabatanFungsional.toString());
		treecell.setTooltiptext(jabatanFungsional.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		JSONArray jsonArray = new JSONArray(jabatanFungsional.getTunjangans());
		String t = "<ol style='font-size:x-small;text-align: left;'>";
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			String d = "";
			if (!jsonObject.isNull("tgl")) {
				d = "Tgl:" + jsonObject.get("tgl");
			}
			if (!jsonObject.isNull("nilai")) {
				d += ", Formula: " + jsonObject.get("nilai");
			}
			if (!jsonObject.isNull("tahun")) {
				d += ", Thn: " + Common.numberFormat.get().format(jsonObject.getInt("tahun"));
			}
			if (!d.isEmpty()) {
				t += "<li>" + d + "</li>";
			}
		}

		t += "</ol>";

		treecell = new Treecell();
		new Html(t).setParent(treecell);
		treecell.setParent(treerow);

		Treecell treecellAktif = new Treecell(
				jabatanFungsional.getDefaultItem() == null || !jabatanFungsional.getDefaultItem() ? "Tidak" : "Aktif");

		if (edit) {
			treecellAktif.setLabel("");
			final MyCheckboxConfig defaultItem;
			treecellAktif.appendChild(defaultItem = new MyCheckboxConfig());
			defaultItem.setChecked(jabatanFungsional.getDefaultItem() != null && jabatanFungsional.getDefaultItem());
			defaultItem.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					jabatanFungsional.setDefaultItem(defaultItem.isChecked());
					session.update(jabatanFungsional);
				}
			});
		}
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecellMap.put(jabatanFungsional, new Treecell[] { treecellAktif });

	}

}
