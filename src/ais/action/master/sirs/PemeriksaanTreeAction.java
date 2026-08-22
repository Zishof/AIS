package ais.action.master.sirs;

import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Window;

import ais.action.master.sirs.util.PemeriksaanTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.Pemeriksaan;
import ais.ui.util.MyMessageboxConfig;

public class PemeriksaanTreeAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;

	private Tree tree;

	private Textbox nama;
	private Combobox jenis;
	private Textbox nilaiYangMungkin;
	private Textbox nilaiDefault;
	private Textbox normal;
	private Textbox satuan;
	private Textbox keterangan;
	private Checkbox aktif;
	private Checkbox harusDiisi;
	private Intbox jumlahBaris;

	private boolean edit = false;
	private boolean delete = false;

	private Pemeriksaan pemeriksaan;
	private boolean add = false;
	private PemeriksaanTreeModel pemeriksaanTreeModel;

	private TreeMap<Pemeriksaan, Treecell[]> treecellMap = new TreeMap<Pemeriksaan, Treecell[]>();

	private Toolbarbutton addNew;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
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
		Treecol treecol = new Treecol("Keluhan, Riwayat, atau Pemeriksaan");
		treecol.setParent(treecols);

		treecol = new Treecol("Jenis");
		treecol.setWidth("10%");
		treecol.setParent(treecols);

		treecol = new Treecol("Nilai-nilai");
		treecol.setWidth("15%");
		treecol.setParent(treecols);

		treecol = new Treecol("Default");
		treecol.setWidth("6%");
		treecol.setParent(treecols);

		treecol = new Treecol("Normal");
		treecol.setWidth("6%");
		treecol.setParent(treecols);

		treecol = new Treecol("Satuan");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Aktif");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Harus diisi");
		treecol.setWidth("7%");
		treecol.setParent(treecols);

		treecol = new Treecol("Baris");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("15%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onAdd(Event event) throws Exception {

		Pemeriksaan mypemeriksaan = new Pemeriksaan();
		mypemeriksaan.setParent(null);

		init(mypemeriksaan, new EventListener() {

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

	private void init(Pemeriksaan pemeriksaan, final EventListener eventListener) {
		this.pemeriksaan = pemeriksaan;
		addWindow.setTitle(pemeriksaan.getId() == null ? "Tambah Keluhan, Riwayat, atau Pemeriksaan" : "Ubah Keluhan, Riwayat, atau Pemeriksaan");
		Common.clear(addWindow);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Isi Keluhan, Riwayat, atau Pemeriksaan")));
		row.appendChild(nama = new Textbox(pemeriksaan.getNama() == null ? "" : pemeriksaan.getNama()));
		nama.setWidth("90%");
		nama.setRows(3);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis")));
		row.appendChild(jenis = new Combobox());
		Comboitem c = new Comboitem(Pemeriksaan.JENIS_KELUHAN);
		c.setValue(Pemeriksaan.JENIS_KELUHAN);
		jenis.appendChild(c);
		c = new Comboitem(Pemeriksaan.JENIS_RIWAYAT);
		c.setValue(Pemeriksaan.JENIS_RIWAYAT);
		jenis.appendChild(c);
		c = new Comboitem(Pemeriksaan.JENIS_PERIKSA);
		c.setValue(Pemeriksaan.JENIS_PERIKSA);
		jenis.appendChild(c);
		Common.selectComboItem(jenis, pemeriksaan.getJenis());
		jenis.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai yang mungkin")));
		row.appendChild(nilaiYangMungkin = new Textbox(pemeriksaan.getNilaiYangMungkin()));
		nilaiYangMungkin.setWidth("90%");
		nilaiYangMungkin.setRows(3);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai default")));
		row.appendChild(nilaiDefault = new Textbox(pemeriksaan.getNilaiDefault()));
		nilaiDefault.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai Normal (jika ada)")));
		row.appendChild(normal = new Textbox(pemeriksaan.getNormal()));
		normal.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Satuan")));
		row.appendChild(satuan = new Textbox(pemeriksaan.getSatuan()));
		satuan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Aktif")));
		row.appendChild(aktif = new Checkbox());
		aktif.setChecked(pemeriksaan.getAktif());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Harus diisi")));
		row.appendChild(harusDiisi = new Checkbox());
		harusDiisi.setChecked(pemeriksaan.getHarusDiisi());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah baris form isian")));
		row.appendChild(jumlahBaris = new Intbox(pemeriksaan.getJumlahBaris()));
		jumlahBaris.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(
				keterangan = new Textbox(pemeriksaan.getKeterangan() == null ? "" : pemeriksaan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					// onReloadTree(null);
					eventListener.onEvent(new Event("", null, PemeriksaanTreeAction.this.pemeriksaan));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Nama Keluhan, Riwayat, atau Pemeriksaan harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Keluhan, Riwayat, atau Pemeriksaan; (2) pastikan isian tidak kosong; (3) ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenis.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Jenis harus diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih jenis pada kolom Jenis; (2) pastikan jenis yang dipilih sudah benar; (3) ulangi proses penyimpanan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pemeriksaan.getId() != null) {
			pemeriksaan = (Pemeriksaan) session.load(Pemeriksaan.class, pemeriksaan.getId());
		}

		pemeriksaan.setJumlahBaris(jumlahBaris.getValue());
		pemeriksaan.setNilaiDefault(nilaiDefault.getValue().trim());
		pemeriksaan.setJenis((String) jenis.getSelectedItem().getValue());
		pemeriksaan.setNilaiYangMungkin(nilaiYangMungkin.getValue().trim());
		pemeriksaan.setNormal(normal.getValue().trim());
		pemeriksaan.setSatuan(satuan.getValue().trim());
		pemeriksaan.setAktif(aktif.isChecked());
		pemeriksaan.setHarusDiisi(harusDiisi.isChecked());
		pemeriksaan.setNama(nama.getValue());
		pemeriksaan.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, pemeriksaan);

		return true;
	}

	public void onReloadTree(Event event) throws Exception {
		addNew.setVisible(add);
		pemeriksaanTreeModel = new PemeriksaanTreeModel(true);
		tree.setModel(pemeriksaanTreeModel);
		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final Pemeriksaan pemeriksaan = (Pemeriksaan) arg1;

				try {
					Common.clear(treeitem);
					final Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					hasSomeChilds(treerow, pemeriksaan);

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();

					Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/reply.png");
					button.setTooltiptext("Refresh");
					// button.setVisible(hasChild);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadTreeitem(treeitem, true, false);
						}
					});
					button.setParent(toolbar);

					button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/new.gif");
					button.setTooltiptext("Tambah Data");
					button.setVisible(add);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Pemeriksaan mypemeriksaan = (Pemeriksaan) pemeriksaan.clone();
							mypemeriksaan.setParent(pemeriksaan);
							mypemeriksaan.setId(null);
							init(mypemeriksaan, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									render(treeitem, pemeriksaan);
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
														Treeitem myTreeitem = (Treeitem) treecellMap.get(pemeriksaan)[0]
																.getParent().getParent();

														render(myTreeitem, pemeriksaan);

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
																				.get(pemeriksaan)[0].getParent()
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
														ais.common.Common.tampilErrorJikaAdmin(e);
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

					button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/copy.png");
					button.setTooltiptext("Copy Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Pemeriksaan mypemeriksaan = (Pemeriksaan) pemeriksaan.clone();
							mypemeriksaan.setParent(pemeriksaan.getParent());
							mypemeriksaan.setId(null);
							init(mypemeriksaan, new EventListener() {

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

					button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
					button.setTooltiptext("Rubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(pemeriksaan, new EventListener() {

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

					button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
					button.setTooltiptext("Hapus Data");
					button.setVisible(delete);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show(
									"Apakah Bapak/Ibu yakin ingin menghapus data ini? Data ini beserta seluruh data turunannya akan dihapus secara permanen dan tidak dapat dikembalikan.",
									"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = new Integer(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													Session session = HibernateUtil.currentSession();

													pemeriksaanTreeModel.deleteChilds(pemeriksaan);

													session.delete(session.merge(pemeriksaan));

													reloadTreeitem(treeitem, true, true);
												} catch (Exception e) {
													ais.common.Common.tampilErrorJikaAdmin(e);
													MyMessageboxConfig.show(Common.pesan(
															"Data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait; (2) pastikan data ini tidak sedang digunakan pada transaksi lain; (3) apabila kendala berlanjut, hubungi administrator sistem.",
															e.getMessage()));
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
					ais.common.Common.tampilErrorJikaAdmin(e);
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
				ais.common.Common.tampilErrorJikaAdmin(e);
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
			List<Treeitem> treeitems = treeitemParent.getChildren();
			for (Object object : treeitems) {

				if (object instanceof Treechildren) {
					Treechildren treechildren = (Treechildren) object;
					List<Treeitem> mytreeitems = treechildren.getChildren();
					for (Treeitem treeitem : mytreeitems) {
						openChilds(treeitem, max, (++index));
					}

				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void closeChilds(final Treeitem treeitemParent) {
		treeitemParent.setOpen(false);
		List<Treeitem> treeitems = treeitemParent.getChildren();
		for (Object object : treeitems) {

			if (object instanceof Treechildren) {
				Treechildren treechildren = (Treechildren) object;
				List<Treeitem> mytreeitems = treechildren.getChildren();
				for (Treeitem treeitem : mytreeitems) {
					closeChilds(treeitem);
				}

			}
		}
	}

	private void hasSomeChilds(Treerow treerow, final Pemeriksaan pemeriksaan) {

		Treecell treecell = new Treecell(pemeriksaan.toString());
		treecell.setTooltiptext(pemeriksaan.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		treecell = new Treecell(pemeriksaan.getJenis());
		treecell.setTooltiptext(pemeriksaan.getJenis());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		treecell = new Treecell(pemeriksaan.getNilaiYangMungkin());
		treecell.setTooltiptext(pemeriksaan.getNilaiYangMungkin());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		treecell = new Treecell(pemeriksaan.getNilaiDefault());
		treecell.setTooltiptext(pemeriksaan.getNilaiDefault());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		treecell = new Treecell(pemeriksaan.getNormal());
		treecell.setTooltiptext(pemeriksaan.getNormal());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		treecell = new Treecell(pemeriksaan.getSatuan());
		treecell.setTooltiptext(pemeriksaan.getSatuan());
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		Treecell treecellAktif = new Treecell(
				pemeriksaan.getAktif() == null || !pemeriksaan.getAktif() ? "Tidak" : "Aktif");

		if (edit) {
			treecellAktif.setLabel("");
			final Checkbox aktif;
			treecellAktif.appendChild(aktif = new Checkbox());
			aktif.setChecked(pemeriksaan.getAktif() != null && pemeriksaan.getAktif());
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pemeriksaan.setAktif(aktif.isChecked());
					session.update(pemeriksaan);
				}
			});
		}
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		Treecell treecellHarusDiisi = new Treecell();

		boolean noChild = pemeriksaanTreeModel.getChildCount(pemeriksaan) == 0;
		if (edit && noChild) {
			treecellHarusDiisi.setLabel("");
			final Checkbox harusDiisi;
			treecellHarusDiisi.appendChild(harusDiisi = new Checkbox());
			harusDiisi.setChecked(pemeriksaan.getHarusDiisi() != null && pemeriksaan.getHarusDiisi());
			harusDiisi.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pemeriksaan.setHarusDiisi(harusDiisi.isChecked());
					session.update(pemeriksaan);
				}
			});
		}
		treecellHarusDiisi.setStyle("font-size:x-small;text-align: left;");
		treecellHarusDiisi.setParent(treerow);

		treecell = new Treecell(noChild ? pemeriksaan.getJumlahBaris().toString() : "");
		treecell.setTooltiptext(noChild ? pemeriksaan.getJumlahBaris().toString() : "");
		treecell.setStyle("font-size:xx-small;text-align: left;");
		treecell.setParent(treerow);

		treecellMap.put(pemeriksaan, new Treecell[] { treecellAktif, treecellHarusDiisi });

	}

}
