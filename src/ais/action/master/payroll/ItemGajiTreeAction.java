package ais.action.master.payroll;

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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
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
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.payroll.helper.AmbilDataFormatItemGajiBanbox;
import ais.action.master.payroll.helper.AmbilDataItemGajiBanbox;
import ais.action.master.payroll.util.ItemGajiTreeModel;
import ais.action.report.format1.payroll.LaporanItemGaji;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.ItemGaji;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class ItemGajiTreeAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;

	private AmbilDataFormatItemGajiBanbox searchFormatItemGaji;
	private Tree tree;

	private MyCheckboxConfig tampilkanRumus;

	private Textbox nama;
	private Textbox defaultFormula;
	private Textbox kode;
	private MyIntbox nomorUrut;
	private AmbilDataItemGajiBanbox parent;
	private AmbilDataAkunBanbox akun;
	private AmbilDataAkunBanbox akunDebet;
	private Textbox keterangan;
	private Checkbox aktif;
	private Checkbox tampilkanDiSlip;
	private Checkbox space;
	private Checkbox nilaiVariableBisaDiubah;

	private boolean edit = false;
	private boolean delete = false;

	private ItemGaji itemGaji;
	private boolean add = false;
	private ItemGajiTreeModel itemGajiTreeModel;

	private TreeMap<ItemGaji, Treecell[]> treecellMap = new TreeMap<ItemGaji, Treecell[]>();

	private MyToolbarbuttonConfig addNew;
	private MyCheckboxConfig jadikan0JikaMinus;
	private MyCheckboxConfig finalGaji;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchFormatItemGaji.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(arg0);
			}
		});

		if (searchFormatItemGaji != null) { searchFormatItemGaji.setReadonly(true); }
		if (searchFormatItemGaji != null) { searchFormatItemGaji.setCols(8); }

		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		// System.out.println("add = " + add + ", edit = " + edit +
		// ", delete = "
		// + delete);

		if (addNew != null) { addNew.setVisible(add); }

		onReloadTree(null);

	}

	public void onCetak(Event event) throws Exception {
		FormatItemGaji formatItemGaji = (FormatItemGaji) searchFormatItemGaji.getAttribute("formatItemGaji");

		if (formatItemGaji == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Format wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Format yang sesuai pada pilihan yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali proses ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		LaporanItemGaji laporanItemGaji = new LaporanItemGaji(formatItemGaji);
		laporanItemGaji.setTitle("Cetak Item Gaji");
		page.getFirstRoot().appendChild(laporanItemGaji);
		laporanItemGaji.setHeight("95%");
		laporanItemGaji.setWidth("90%");
		laporanItemGaji.setClosable(true);
		laporanItemGaji.onModal();
	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Item Gaji");
		treecol.setParent(treecols);

		if (tampilkanRumus.isChecked()) {
			treecol = new Treecol("Formula");
			treecol.setWidth("15%");
			treecol.setParent(treecols);
		}

		treecol = new Treecol("Hasil Perhitungan");
		treecol.setWidth("10%");
		treecol.setParent(treecols);

		treecol = new Treecol("Debet");
		treecol.setWidth("10%");
		treecol.setParent(treecols);

		treecol = new Treecol("Kredit");
		treecol.setWidth("10%");
		treecol.setParent(treecols);

		treecol = new Treecol("Aktif");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Slip");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Diubah");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Urut");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("");
		treecol.setWidth("15%");
		treecol.setParent(treecols);

		treecols.setParent(tree);
	}

	public void onAdd(Event event) throws Exception {

		FormatItemGaji formatItemGaji = (FormatItemGaji) searchFormatItemGaji.getAttribute("formatItemGaji");

		if (formatItemGaji == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Format wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Format yang sesuai pada pilihan yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali proses ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		ItemGaji myitemGaji = new ItemGaji();
		myitemGaji.setParent(null);

		init(myitemGaji, new EventListener() {

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
	private void init(ItemGaji itemGaji, final EventListener eventListener) throws Exception {
		this.itemGaji = itemGaji;
		addWindow.setTitle(itemGaji.getId() == null ? "Tambah Item Gaji" : "Ubah Item Gaji");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setWidth("40%");

		column = new Column();
		column.setParent(columns);
		column.setWidth("60%");

		final Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Item Gaji")));
		row.appendChild(kode = new Textbox(itemGaji.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Item Gaji")));
		row.appendChild(nama = new Textbox(itemGaji.getNama() == null ? "" : itemGaji.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Formula Penghitungan")));
		row.appendChild(defaultFormula = new Textbox(itemGaji.getDefaultFormula()));
		defaultFormula.setWidth("90%");
		defaultFormula.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nomor Urut")));
		row.appendChild(nomorUrut = new MyIntbox(itemGaji.getNomorUrut()));
		nomorUrut.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new Textbox(itemGaji.getKeterangan() == null ? "" : itemGaji.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Item Gaji Parent")));
		row.appendChild(parent = new AmbilDataItemGajiBanbox(true));
		parent.setValue(itemGaji.getParent() == null ? "" : itemGaji.getParent().toString());
		parent.setAttribute("itemGaji", itemGaji.getParent());
		parent.setWidth("90%");

		Akun a = itemGaji.ambilAkun();

		row = new MyFormRow();
		row.setStyle("border:0px;background: transakun;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Akun Kredit Item Gaji")));
		akun = new AmbilDataAkunBanbox();
		if (a != null && a.getId() != null) {
			itemGaji.setAkun(a);
			new Label(a.getKode() + " " + a.getNama()).setParent(row);
		} else {
			row.appendChild(akun);
		}
		akun.setValue(itemGaji.getAkun() == null ? "" : itemGaji.getAkun().getNama());
		akun.setAttribute("akun", itemGaji.getAkun());
		akun.setWidth("90%");

		a = itemGaji.ambilAkunDebet();

		row = new MyFormRow();
		row.setStyle("border:0px;background: transakun;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Akun Debet Item Gaji")));
		akunDebet = new AmbilDataAkunBanbox();
		if (a != null && a.getId() != null) {
			itemGaji.setAkunDebet(a);
			new Label(a.getKode() + " " + a.getNama()).setParent(row);
		} else {
			row.appendChild(akunDebet);
		}
		akunDebet.setValue(itemGaji.getAkunDebet() == null ? "" : itemGaji.getAkunDebet().getNama());
		akunDebet.setAttribute("akun", itemGaji.getAkunDebet());
		akunDebet.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(nilaiVariableBisaDiubah = new MyCheckboxConfig("Nilai Variable Bisa Diubah"));
		nilaiVariableBisaDiubah.setChecked(itemGaji.getNilaiVariableBisaDiubah());
		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(finalGaji = new MyCheckboxConfig("Merupuakan perhitungan final / hasil total gaji"));
		finalGaji.setChecked(itemGaji.getFinalGaji()); 

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(aktif = new MyCheckboxConfig("Aktif"));
		aktif.setChecked(itemGaji.getAktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(tampilkanDiSlip = new MyCheckboxConfig("Tampilkan item gaji ini di Slip Gaji"));
		tampilkanDiSlip.setChecked(itemGaji.getTampilkanDiSlip());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(jadikan0JikaMinus = new MyCheckboxConfig("Jika hasil pengitungan minus, jadikan 0"));
		jadikan0JikaMinus.setChecked(itemGaji.getJadikan0JikaMinus());

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(space = new MyCheckboxConfig("Item ini hanya space kosong"));
		space.setChecked(itemGaji.getSpace());

		EventListener spaceEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (space.isChecked()) {
					kode.setValue("SPACE");
					nama.setValue("");
					defaultFormula.setValue("");
					akun.setAttribute("akun", null);
					akunDebet.setAttribute("akun", null);
					Common.freeze(rows, true);
					space.setDisabled(false);
					nomorUrut.setDisabled(false);
				} else {
					Common.freeze(rows, false);
				}
			}
		};

		spaceEventListener.onEvent(null);
		space.addEventListener("onCheck", spaceEventListener);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					// onReloadTree(null);
					eventListener.onEvent(new Event("", null, ItemGajiTreeAction.this.itemGaji));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	public boolean onSave(Event event) throws Exception {

		if (!space.isChecked()) {
			if (kode.getValue().trim().equals("")) {
				MyMessageboxConfig.show(
						"Mohon maaf, kolom Kode wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Kode pada kolom yang tersedia; (2) pastikan Kode tidak dikosongkan; (3) simpan kembali data ini.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}

			if (nama.getValue().trim().equals("")) {
				MyMessageboxConfig.show(
						"Mohon maaf, kolom Nama wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama pada kolom yang tersedia; (2) pastikan Nama tidak dikosongkan; (3) simpan kembali data ini.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}
		if (nomorUrut.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Nomor Urut wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nomor Urut pada kolom yang tersedia; (2) pastikan Nomor Urut tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		FormatItemGaji formatItemGaji = (FormatItemGaji) searchFormatItemGaji.getAttribute("formatItemGaji");

		if (formatItemGaji == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Format wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Format yang sesuai pada pilihan yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali proses ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		Session session = HibernateUtil.currentSession();
		if (itemGaji.getId() != null) {
			itemGaji = (ItemGaji) session.load(ItemGaji.class, itemGaji.getId());

		}

		ItemGaji itemGajiParent = (ItemGaji) parent.getAttribute("itemGaji");

		itemGaji.setAkunDebet((Akun) akunDebet.getAttribute("akun"));
		itemGaji.setAkun((Akun) akun.getAttribute("akun"));
		itemGaji.setKode(kode.getValue().trim());
		itemGaji.setFormatItemGaji(formatItemGaji);
		itemGaji.setDefaultFormula(defaultFormula.getValue().trim());
		itemGaji.setNomorUrut(nomorUrut.getValue());
		itemGaji.setParent(itemGajiParent);
		itemGaji.setAktif(aktif.isChecked());
		itemGaji.setNama(nama.getValue());
		itemGaji.setKeterangan(keterangan.getValue());
		itemGaji.setTampilkanDiSlip(tampilkanDiSlip.isChecked());
		itemGaji.setJadikan0JikaMinus(jadikan0JikaMinus.isChecked());
		itemGaji.setNilaiVariableBisaDiubah(nilaiVariableBisaDiubah.isChecked());
		itemGaji.setSpace(space.isChecked());
		itemGaji.setFinalGaji(finalGaji.isChecked());
		if (itemGaji.getId() != null) {
			Common.refreshUpdate(session, itemGaji);
		} else {
			session.save(itemGaji);
		}
		return true;
	}

	public void onReloadTree(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(tree);
				initTree();
				FormatItemGaji formatItemGaji = (FormatItemGaji) searchFormatItemGaji.getAttribute("formatItemGaji");

				if (formatItemGaji == null) {
					return;
				}

				addNew.setVisible(add);
				itemGajiTreeModel = new ItemGajiTreeModel(true, formatItemGaji);
				tree.setModel(itemGajiTreeModel);
				tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

					@Override
					public void render(final Treeitem treeitem, Object arg1) throws Exception {
						final ItemGaji itemGaji = (ItemGaji) arg1;

						try {
							Common.clear(treeitem);
							final Treerow treerow = new Treerow();
							treerow.setParent(treeitem);

							hasSomeChilds(treerow, itemGaji);

							Treecell arg0 = new Treecell();
							arg0.setParent(treerow);
							Hbox toolbar = new Hbox();

							Toolbarbutton button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
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

									ItemGaji myitemGaji = (ItemGaji) itemGaji.clone();
									myitemGaji.setParent(itemGaji);
									myitemGaji.setId(null);
									init(myitemGaji, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											render(treeitem, itemGaji);
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
																		.get(itemGaji)[0].getParent().getParent();

																render(myTreeitem, itemGaji);

																reloadTreeitem(myTreeitem, true, false,
																		new EventListener() {

																			@Override
																			public void onEvent(Event arg0)
																					throws Exception {

																				final Timer timer = new Timer(300);
																				timer.setParent(page.getFirstRoot());
																				timer.addEventListener("onTimer",
																						new EventListener() {

																							@Override
																							public void onEvent(
																									Event arg0)
																									throws Exception {

																								System.out.println(
																										"========================= RELOAD TOTAL ===========================");

																								Treeitem myTreeitem = (Treeitem) treecellMap
																										.get(itemGaji)[0]
																										.getParent()
																										.getParent();
																								reloadTreeitem(
																										myTreeitem,
																										true, false);

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

									ItemGaji myitemGaji = (ItemGaji) itemGaji.clone();
									myitemGaji.setParent(itemGaji.getParent());
									myitemGaji.setId(null);
									init(myitemGaji, new EventListener() {

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
							button.setTooltiptext("Rubah Data");
							button.setVisible(edit);
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									init(itemGaji, new EventListener() {

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
									MyMessageboxConfig.show(
											"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui bahwa data yang telah dihapus tidak dapat dikembalikan lagi.",
											"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
											MyMessageboxConfig.QUESTION, new EventListener() {

												@Override
												public void onEvent(Event event) throws Exception {
													int i = Integer.parseInt(event.getData().toString());
													if (i == MyMessageboxConfig.OK) {
														try {

															itemGajiTreeModel.deleteChilds(itemGaji);

															Common.refreshDelete((itemGaji));

															reloadTreeitem(treeitem, true, true);
														} catch (Exception e) {
															Common.tampilErrorJikaAdmin(e);
															MyMessageboxConfig.show(MyMessageboxConfig.format(
																	"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang masih terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) ulangi kembali proses penghapusan.",
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
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
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

	private void hasSomeChilds(Treerow treerow, final ItemGaji itemGaji) {

		Treecell treecell = new Treecell(itemGaji.toString());
		treecell.setTooltiptext(itemGaji.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		if (tampilkanRumus.isChecked()) {
			treecell = new Treecell(itemGaji.getDefaultFormula());
			treecell.setTooltiptext(itemGaji.getDefaultFormula());
			treecell.setStyle("font-size:x-small;text-align: left;");
			treecell.setParent(treerow);
		}

		Double hasil = itemGajiTreeModel.hitungItemGaji(itemGaji);
		treecell = new Treecell(Common.numberFormat.get().format(hasil));
		treecell.setTooltiptext(Common.numberFormat.get().format(hasil));
		treecell.setStyle("font-size:x-small;text-align: right;");
		treecell.setParent(treerow);

		treecell = new Treecell(itemGaji.getAkunDebet() == null ? "" : itemGaji.getAkunDebet().getNama());
		treecell.setTooltiptext(itemGaji.getAkunDebet() == null ? "" : itemGaji.getAkunDebet().getNama());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		treecell = new Treecell(itemGaji.getAkun() == null ? "" : itemGaji.getAkun().getNama());
		treecell.setTooltiptext(itemGaji.getAkun() == null ? "" : itemGaji.getAkun().getNama());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		Treecell treecellAktif = new Treecell(itemGaji.getAktif() == null || !itemGaji.getAktif() ? "Tidak" : "Ya");
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecellAktif = new Treecell(
				itemGaji.getTampilkanDiSlip() == null || !itemGaji.getTampilkanDiSlip() ? "Tidak" : "Ya");
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecell = new Treecell(itemGaji.getNilaiVariableBisaDiubah() ? "Ya" : "Tidak");
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		treecell = new Treecell(itemGaji.getNomorUrut().toString());
		treecell.setTooltiptext(itemGaji.getNomorUrut().toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		treecellMap.put(itemGaji, new Treecell[] { treecellAktif });

	}

}
