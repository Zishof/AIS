package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

import ais.action.master.helper.AmbilDataBerkasBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.BerkasTreeModel;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Berkas;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class BerkasAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;

	private Tree tree;

	private Textbox searchnama;
	private Textbox searchkode;

	private Textbox nama;
	private Textbox keterangan;
	private AmbilDataBerkasBanbox parent;

	private boolean edit = false;
	private boolean delete = false;

	private Berkas berkas;
	private MyToolbarbuttonConfig add;
	private MyToolbarbuttonConfig addBerkas;
	private BerkasTreeModel berkasTreeModel;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Combobox fakultas;
	private Combobox jurusan;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		if (addBerkas != null) { addBerkas.setVisible((add != null && add.isVisible())); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onReloadTree(null);
		onSearchDefault(null);

	}

	class BerkasRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Berkas berkas = (Berkas) arg1;

			new Label(berkas.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Berkas.class, berkas, berkas.getNama()).setParent(arg0);

			new Label(berkas.getFakultas() == null ? "Semua" : berkas.getFakultas().getNama()).setParent(arg0);
			new Label(berkas.getJurusan() == null ? "Semua" : berkas.getJurusan().getNama()).setParent(arg0);
			new Label(berkas.getProgram() == null || berkas.getProgram().isEmpty() ? "Semua" : berkas.getProgram())
					.setParent(arg0);
			new Label(berkas.getTahunAkademik() == null || berkas.getTahunAkademik().isEmpty() ? "Semua"
					: berkas.getTahunAkademik()).setParent(arg0);
			new Label(berkas.getJenisSemester() == null || berkas.getJenisSemester().isEmpty() ? "Semua"
					: berkas.getJenisSemester()).setParent(arg0);

			new Label(berkas.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(berkas, true, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(arg0);
						}
					});
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && berkasTreeModel.getChildCount(berkas) == 0);
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

											Common.refreshDelete(berkas);

											onSearchDefault(event);
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
		}

	}

	public void onAddBerkas(Event event) throws Exception {
		init(new Berkas(), false, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(arg0);
			}
		});
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new Berkas(), false, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Berkas berkas, Boolean isedit, final EventListener eventListener) {
		this.berkas = berkas;
		addWindow.setTitle(berkas.getId() == null ? "Tambah Berkas" : "Ubah Berkas");
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
		// //grid.setOddRowSclass("non-odd");
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Berkas"));
		row.appendChild(nama = new Textbox(berkas.getNama() == null ? "" : berkas.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		Common.selectComboItem(tahunAkademik, berkas.getTahunAkademik());
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		jenisSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		jenisSemester.appendChild(comboitem);

		Common.selectComboItem(jenisSemester, berkas.getJenisSemester());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setReadonly(true);

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		Tbmuser tbmuser = Common.getCurrentUser();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, berkas.getFakultas() == null ? tbmuser.ambilFakultas() : berkas.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		// fakultas.setDisabled(false);

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, berkas.getJurusan() == null ? tbmuser.ambilJurusan() : berkas.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Induk"));
		row.appendChild(parent = new AmbilDataBerkasBanbox(true));
		parent.setValue(berkas.getParent() == null ? "" : berkas.getParent().toString());
		parent.setAttribute("berkas", berkas.getParent());
		parent.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(berkas.getKeterangan() == null ? "" : berkas.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(4);

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
					// onSearchDefault(null);
					eventListener.onEvent(new Event("", null, BerkasAction.this.berkas));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Berkas",
					"Kolom Nama Berkas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Berkas.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (berkas.getId() != null) {
			berkas = (Berkas) session.load(Berkas.class, berkas.getId());
		}

		berkas.setNama(nama.getValue());
		berkas.setKeterangan(keterangan.getValue());
		berkas.setParent((Berkas) parent.getAttribute("berkas"));
		berkas.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		berkas.setJurusan((Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
				? null : jurusan.getSelectedItem().getValue()));
		berkas.setTahunAkademik(
				(String) (tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						? null : tahunAkademik.getSelectedItem().getValue()));
		berkas.setJenisSemester(
				(String) (jenisSemester.getSelectedItem() == null ? null : jenisSemester.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, berkas);
		return true;
	}

	public void onReloadTree(Event event) {
		berkasTreeModel = new BerkasTreeModel();
		tree.setModel(berkasTreeModel);

		tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

			@Override
			public void render(final Treeitem treeitem, Object arg1) throws Exception {
				final Berkas berkas = (Berkas) arg1;

				try {
					Treerow treerow = new Treerow();
					treerow.setParent(treeitem);

					Treecell arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(berkas.getNama()).setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(berkas.getFakultas() == null ? "Semua" : berkas.getFakultas().getNama()).setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(berkas.getJurusan() == null ? "Semua" : berkas.getJurusan().getNama()).setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(berkas.getProgram() == null || berkas.getProgram().trim().isEmpty() ? "Semua"
							: berkas.getProgram()).setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(berkas.getTahunAkademik() == null || berkas.getTahunAkademik().trim().isEmpty() ? "Semua"
							: berkas.getTahunAkademik()).setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(berkas.getJenisSemester() == null || berkas.getJenisSemester().trim().isEmpty() ? "Semua"
							: berkas.getJenisSemester()).setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					new Label(berkas.getKeterangan()).setParent(arg0);

					arg0 = new Treecell();
					arg0.setParent(treerow);
					Hbox toolbar = new Hbox();
					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/addthis.svg");
					button.setTooltiptext("Tambah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Berkas myberkas = (Berkas) berkas.clone();
							myberkas.setId(null);
							myberkas.setParent(berkas);
							init(myberkas, false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									reloadTreeitem(treeitem);
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

							Berkas myberkas = (Berkas) berkas.clone();
							myberkas.setId(null);
							myberkas.setKode(berkas.getKode());
							init(myberkas, false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									reloadTreeitem(treeitem);
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
							init(berkas, true, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									reloadTreeitem(treeitem);

								}
							});
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					button.setParent(toolbar);

					button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
					button.setTooltiptext("Hapus Data");
					button.setVisible(delete && berkasTreeModel.getChildCount(berkas) == 0);
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


													Common.refreshDelete((berkas));

													treeitem.detach();

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

	private void reloadTreeitem(final Treeitem treeitem) {
		final Treeitem treeitemParent = treeitem.getParentItem();
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

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<Berkas> berkas = session.createCriteria(Berkas.class).addOrder(Order.asc("kode"))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE))

				.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(berkas);
		grid.setRowRenderer(new BerkasRenderer());
		grid.setModelCheckMobile(strset);

	}

}
