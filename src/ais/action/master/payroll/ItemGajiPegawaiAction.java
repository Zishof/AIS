package ais.action.master.payroll;

import java.util.ArrayList;
import java.util.Calendar;
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
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.payroll.helper.AmbilDataFormatItemGajiBanbox;
import ais.action.master.payroll.util.ItemGajiPegawaiTreeModel;
import ais.action.report.format1.payroll.LaporanItemGajiPegawai;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.ItemGajiPegawai;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class ItemGajiPegawaiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;

	private AmbilDataFormatItemGajiBanbox searchFormatItemGaji;
	private AmbilDataPegawaiBanbox searchPegawai;
	private MyCheckboxConfig tampilkanRumus;
	private Tree tree;

	private Textbox nama;
	private Textbox defaultFormula;
	private Textbox kode;
	private MyIntbox nomorUrut;
	private Textbox keterangan;
	private Checkbox aktif;
	private Checkbox space;
	private Checkbox nilaiVariableBisaDiubah;

	private boolean edit = false;
	private boolean delete = false;

	private ItemGajiPegawai itemGajiPegawai;
	private boolean add = false;
	private ItemGajiPegawaiTreeModel itemGajiPegawaiTreeModel;

	private TreeMap<ItemGajiPegawai, Treecell[]> treecellMap = new TreeMap<ItemGajiPegawai, Treecell[]>();

	private MyToolbarbuttonConfig addNew;
	private ItemGajiPegawai itemGajiPegawaiParent;

	private Checkbox ikutiItemGaji;
	private Double hasil = 0.0;

	private Pegawai pegawai = null;
	private MyCheckboxConfig finalGaji;

	private Integer ke = 1;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (execution.getParameter("ke") != null) {
			try {
				ke = Integer.parseInt(execution.getParameter("ke").trim());
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		pegawai = null;
		if (execution.getParameter("pegawai") != null) {
			pegawai = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(),
					Long.parseLong(execution.getParameter("pegawai")));

			if (pegawai != null) {

				searchFormatItemGaji.setLevelJabatan(pegawai.getLevelJabatan());
				searchFormatItemGaji.setCabang(pegawai.getCabang());
				searchFormatItemGaji.setDepertemen(pegawai.getDepartemen());

				if (ke.equals(2)) {
					searchFormatItemGaji.setAttribute("formatItemGaji", pegawai.getFormatItemGaji2());
					searchFormatItemGaji.setValue(
							pegawai.getFormatItemGaji2() == null ? "" : pegawai.getFormatItemGaji2().getNama());
				} else if (ke.equals(3)) {
					searchFormatItemGaji.setAttribute("formatItemGaji", pegawai.getFormatItemGaji3());
					searchFormatItemGaji.setValue(
							pegawai.getFormatItemGaji3() == null ? "" : pegawai.getFormatItemGaji3().getNama());
				} else if (ke.equals(4)) {
					searchFormatItemGaji.setAttribute("formatItemGaji", pegawai.getFormatItemGaji4());
					searchFormatItemGaji.setValue(
							pegawai.getFormatItemGaji4() == null ? "" : pegawai.getFormatItemGaji4().getNama());
				} else if (ke.equals(5)) {
					searchFormatItemGaji.setAttribute("formatItemGaji", pegawai.getFormatItemGaji5());
					searchFormatItemGaji.setValue(
							pegawai.getFormatItemGaji5() == null ? "" : pegawai.getFormatItemGaji5().getNama());
				} else {
					searchFormatItemGaji.setAttribute("formatItemGaji", pegawai.getFormatItemGaji());
					searchFormatItemGaji
							.setValue(pegawai.getFormatItemGaji() == null ? "" : pegawai.getFormatItemGaji().getNama());
				}

			}
		}

		if (pegawai != null) {
			searchPegawai.setAttribute("pegawai", pegawai);
			searchPegawai.setValue(pegawai.toString());
			searchPegawai.setDisabled(true);
		}

		searchFormatItemGaji.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pegawai pegawai = (Pegawai) searchPegawai.getAttribute("pegawai");
				if (pegawai != null) {
					Session session = HibernateUtil.currentNativeSession();
					session.refresh(pegawai);
					if (ke.equals(2)) {
						pegawai.setFormatItemGaji2(
								(FormatItemGaji) searchFormatItemGaji.getAttribute("formatItemGaji"));
					} else if (ke.equals(3)) {
						pegawai.setFormatItemGaji3(
								(FormatItemGaji) searchFormatItemGaji.getAttribute("formatItemGaji"));
					} else if (ke.equals(4)) {
						pegawai.setFormatItemGaji4(
								(FormatItemGaji) searchFormatItemGaji.getAttribute("formatItemGaji"));
					} else if (ke.equals(5)) {
						pegawai.setFormatItemGaji5(
								(FormatItemGaji) searchFormatItemGaji.getAttribute("formatItemGaji"));
					} else {
						pegawai.setFormatItemGaji((FormatItemGaji) searchFormatItemGaji.getAttribute("formatItemGaji"));
					}

					session.getTransaction().begin();
					Common.refreshUpdate(session, pegawai);
					session.getTransaction().commit();
					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}
					HibernateUtil.closeSession();
				}
				onReloadTree(arg0);
			}
		});
		if (searchFormatItemGaji != null) { searchFormatItemGaji.setReadonly(true); }
		if (searchFormatItemGaji != null) { searchFormatItemGaji.setCols(8); }

		searchPegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				onReloadTree(arg0);
			}
		});

		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (addNew != null) { addNew.setVisible(add); }

		onReloadTree(null);

	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("Item Gaji");
		treecol.setParent(treecols);

		if (tampilkanRumus.isChecked()) {
			treecol = new Treecol("Formula");
			treecol.setWidth("15%");
			treecol.setParent(treecols);

			treecol = new Treecol("Penghitungan");
			treecol.setWidth("10%");
			treecol.setParent(treecols);
		}

		treecol = new Treecol("Hasil");
		treecol.setWidth("10%");
		treecol.setParent(treecols);

		treecol = new Treecol("Aktif");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Ya");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Ikut");
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

		ItemGajiPegawai myitemGajiPegawai = new ItemGajiPegawai();
		myitemGajiPegawai.setParent(null);

		init(myitemGajiPegawai, new EventListener() {

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
	private void init(ItemGajiPegawai itemGajiPegawai, final EventListener eventListener) throws Exception {
		this.itemGajiPegawai = itemGajiPegawai;
		this.itemGajiPegawaiParent = itemGajiPegawai.getParent();
		addWindow.setTitle(itemGajiPegawai.getId() == null ? "Tambah Item Gaji Pegawai" : "Ubah Item Gaji Pegawai");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(""));
		row.appendChild(ikutiItemGaji = new MyCheckboxConfig("Ikuti Data Item Gaji"));
		ikutiItemGaji.setChecked(itemGajiPegawai.getIkutiItemGaji());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Item Gaji")));
		row.appendChild(kode = new Textbox(itemGajiPegawai.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Item Gaji")));
		row.appendChild(nama = new Textbox(itemGajiPegawai.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Formula Penghitungan")));
		row.appendChild(defaultFormula = new Textbox(itemGajiPegawai.getDefaultFormula()));
		defaultFormula.setWidth("90%");
		defaultFormula.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nomor Urut")));
		row.appendChild(nomorUrut = new MyIntbox(itemGajiPegawai.getNomorUrut()));
		nomorUrut.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new Textbox(itemGajiPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(nilaiVariableBisaDiubah = new MyCheckboxConfig("Nilai Variable Bisa Diubah"));
		nilaiVariableBisaDiubah.setChecked(itemGajiPegawai.getNilaiVariableBisaDiubah());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(finalGaji = new MyCheckboxConfig("Merupuakan perhitungan final / hasil total gaji"));
		finalGaji.setChecked(itemGajiPegawai.getFinalGaji());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(aktif = new MyCheckboxConfig("Aktif"));
		aktif.setChecked(itemGajiPegawai.getAktif());

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Item ini hanya space kosong")));
		row.appendChild(space = new Checkbox());
		space.setChecked(itemGajiPegawai.getSpace());

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		final MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		final MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					// onReloadTree(null);
					eventListener.onEvent(new Event("", null, ItemGajiPegawaiAction.this.itemGajiPegawai));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		EventListener spaceEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (space.isChecked()) {
					kode.setValue("SPACE");
					nama.setValue("");
					defaultFormula.setValue("");
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

		EventListener eventListener2 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kode.setDisabled(ikutiItemGaji.isChecked());
				nama.setDisabled(ikutiItemGaji.isChecked());
				defaultFormula.setDisabled(ikutiItemGaji.isChecked());

				nomorUrut.setDisabled(ikutiItemGaji.isChecked());
				keterangan.setDisabled(ikutiItemGaji.isChecked());
				nilaiVariableBisaDiubah.setDisabled(ikutiItemGaji.isChecked());
				aktif.setDisabled(ikutiItemGaji.isChecked());
				space.setDisabled(ikutiItemGaji.isChecked());
				finalGaji.setDisabled(ikutiItemGaji.isChecked());
			}
		};

		eventListener2.onEvent(null);

		ikutiItemGaji.addEventListener("onClick", eventListener2);
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
		if (itemGajiPegawai.getId() != null) {
			itemGajiPegawai = (ItemGajiPegawai) session.load(ItemGajiPegawai.class, itemGajiPegawai.getId());

		}

		itemGajiPegawai.setIkutiItemGaji(ikutiItemGaji.isChecked());
		itemGajiPegawai.setNilaiVariableBisaDiubah(nilaiVariableBisaDiubah.isChecked());
		itemGajiPegawai.setSpace(space.isChecked());
		itemGajiPegawai.setKode(kode.getValue().trim());
		itemGajiPegawai.setFormatItemGaji(formatItemGaji);
		itemGajiPegawai.setDefaultFormula(defaultFormula.getValue().trim());
		itemGajiPegawai.setNomorUrut(nomorUrut.getValue());
		itemGajiPegawai.setParent(itemGajiPegawaiParent);
		itemGajiPegawai.setAktif(aktif.isChecked());
		itemGajiPegawai.setNama(nama.getValue());
		itemGajiPegawai.setKeterangan(keterangan.getValue());
		itemGajiPegawai.setFinalGaji(finalGaji.isChecked());

		Common.refreshSaveOrUpdate(session, itemGajiPegawai);

		return true;
	}

	public void onResetTree(Event event) throws Exception {
		if (itemGajiPegawaiTreeModel != null) {
			MyMessageboxConfig.show(
					"Apakah Bapak/Ibu yakin ingin me-reset gaji pegawai ini? Perlu diketahui bahwa seluruh perhitungan gaji pegawai ini akan dihitung ulang dari awal.",
					"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
					new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								itemGajiPegawaiTreeModel.reset();
								onReloadTree(event);
							}

						}
					});

		}
	}

	public void onCetak(Event event) throws Exception {
		FormatItemGaji formatItemGaji = (FormatItemGaji) searchFormatItemGaji.getAttribute("formatItemGaji");

		if (formatItemGaji == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Format wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Format yang sesuai pada pilihan yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali proses ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		Pegawai pegawai = (Pegawai) searchPegawai.getAttribute("pegawai");
		if (pegawai == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, data Pegawai wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Pegawai yang sesuai pada pilihan yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali proses ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		LaporanItemGajiPegawai laporanItemGaji = new LaporanItemGajiPegawai(formatItemGaji, pegawai);
		laporanItemGaji.setTitle("Cetak Item Gaji Pegawai");
		page.getFirstRoot().appendChild(laporanItemGaji);
		laporanItemGaji.setHeight("95%");
		laporanItemGaji.setWidth("90%");
		laporanItemGaji.setClosable(true);
		laporanItemGaji.onModal();
	}

	public void onReloadTree(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(tree);
				initTree();

				FormatItemGaji formatItemGaji = (FormatItemGaji) searchFormatItemGaji.getAttribute("formatItemGaji");

				Pegawai pegawai = (Pegawai) searchPegawai.getAttribute("pegawai");
				if (formatItemGaji == null || pegawai == null) {
					return;
				}

				addNew.setVisible(add);
				itemGajiPegawaiTreeModel = new ItemGajiPegawaiTreeModel(true, formatItemGaji, pegawai, null);

				itemGajiPegawaiTreeModel.checkExistingItemGaji();

				tree.setModel(itemGajiPegawaiTreeModel);
				tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

					@Override
					public void render(final Treeitem treeitem, Object arg1) throws Exception {
						final ItemGajiPegawai itemGajiPegawai = (ItemGajiPegawai) arg1;

						try {
							Common.clear(treeitem);

							if (itemGajiPegawai == null) {
								return;
							}

							final Treerow treerow = new Treerow();
							treerow.setParent(treeitem);

							hasSomeChilds(treerow, itemGajiPegawai);

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

									ItemGajiPegawai myitemGajiPegawai = (ItemGajiPegawai) itemGajiPegawai.clone();
									myitemGajiPegawai.setParent(itemGajiPegawai);
									myitemGajiPegawai.setId(null);
									init(myitemGajiPegawai, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											render(treeitem, itemGajiPegawai);
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
																		.get(itemGajiPegawai)[0].getParent()
																		.getParent();

																render(myTreeitem, itemGajiPegawai);

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
																										.get(itemGajiPegawai)[0]
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

									ItemGajiPegawai myitemGajiPegawai = (ItemGajiPegawai) itemGajiPegawai.clone();
									myitemGajiPegawai.setParent(itemGajiPegawai.getParent());
									myitemGajiPegawai.setId(null);
									init(myitemGajiPegawai, new EventListener() {

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
									init(itemGajiPegawai, new EventListener() {

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

															itemGajiPegawaiTreeModel.deleteChilds(itemGajiPegawai);

															Common.refreshDelete((itemGajiPegawai));

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
							toolbar.setParent(arg0);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});

				if (ItemGajiPegawaiAction.this.pegawai != null) {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (ItemGajiPegawaiAction.this.pegawai.getNilaiGaji().intValue() != hasil.intValue()) {
								Session session = HibernateUtil.currentSession();
								session.refresh(ItemGajiPegawaiAction.this.pegawai);
								ItemGajiPegawaiAction.this.pegawai.setNilaiGaji(hasil);
								Common.refreshUpdate(session, ItemGajiPegawaiAction.this.pegawai);
								session.flush();
							}
						}
					});
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

	private void hasSomeChilds(Treerow treerow, final ItemGajiPegawai itemGajiPegawai) throws Exception {

		Treecell treecell = new Treecell(itemGajiPegawai.toString());
		treecell.setTooltiptext(itemGajiPegawai.toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		List<String> penghitungan = new ArrayList<String>();
		hasil = itemGajiPegawaiTreeModel.hitungItemGajiPegawai(itemGajiPegawai.getKode(),
				itemGajiPegawai.getDefaultFormula(), ais.ui.util.WaktuUtil.getDate(),
				ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1,
				ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR), null, penghitungan);

		if (itemGajiPegawai != null && itemGajiPegawai.getItemGaji() != null
				&& itemGajiPegawai.getItemGaji().getJadikan0JikaMinus() && hasil < 0.0) {
			hasil = 0.0;
		}

		if (tampilkanRumus.isChecked()) {
			treecell = new Treecell(itemGajiPegawai.getDefaultFormula());
			treecell.setTooltiptext(itemGajiPegawai.getDefaultFormula());
			treecell.setStyle("font-size:x-small;text-align: left;");
			treecell.setParent(treerow);

			treecell = new Treecell();
			treecell.setStyle("font-size:x-small;text-align: left;");
			treecell.setParent(treerow);

			Vbox vbox = new Vbox();
			for (String ss : penghitungan) {
				vbox.appendChild(new MyLabelAgakKecil(ss));
			}
			treecell.appendChild(vbox);
		}

		treecell = new Treecell(Common.numberFormat.get().format(hasil));
		treecell.setTooltiptext(Common.numberFormat.get().format(hasil));
		treecell.setStyle("font-size:x-small;text-align: right;");
		treecell.setParent(treerow);

		Treecell treecellAktif = new Treecell(
				itemGajiPegawai.getAktif() == null || !itemGajiPegawai.getAktif() ? "Tidak" : "Ya");
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecellAktif = new Treecell(itemGajiPegawai.getTampilkanDiSlip() ? "Ya" : "Tidak");
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecellAktif = new Treecell(itemGajiPegawai.getIkutiItemGaji() ? "Ya" : "Tidak");
		treecellAktif.setStyle("font-size:x-small;text-align: left;");
		treecellAktif.setParent(treerow);

		treecell = new Treecell(itemGajiPegawai.getNilaiVariableBisaDiubah() ? "Ya" : "Tidak");
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		treecell = new Treecell(itemGajiPegawai.getNomorUrut().toString());
		treecell.setTooltiptext(itemGajiPegawai.getNomorUrut().toString());
		treecell.setStyle("font-size:x-small;text-align: left;");
		treecell.setParent(treerow);

		treecellMap.put(itemGajiPegawai, new Treecell[] { treecellAktif });

	}

}
