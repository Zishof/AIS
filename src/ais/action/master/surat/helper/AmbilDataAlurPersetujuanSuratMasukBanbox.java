package ais.action.master.surat.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.AlurPersetujuanSuratMasuk;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataAlurPersetujuanSuratMasukBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected AlurPersetujuanSuratMasukTreeModel alurPersetujuanSuratMasukTreeModel;

	private Boolean chooseAll = false;
	private Boolean parentOnly;

	public AmbilDataAlurPersetujuanSuratMasukBanbox(String tipe) throws Exception {
		this(true, false, tipe);
	}

	private Combobox fakultas;
	private Combobox jurusan;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox yayasan;
	private Combobox sekolah;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private String tipe = "surat";

	public AmbilDataAlurPersetujuanSuratMasukBanbox(Boolean chooseAll, Boolean parentOnly, String tipe)
			throws Exception {
		super();
		this.tipe = tipe;
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		this.chooseAll = chooseAll;
		this.parentOnly = parentOnly;

		try {
			display();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/helper/AmbilDataAlurPersetujuanSuratMasukBanbox.java:102");
		}
		setReadonly(true);
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public void setChooseAll(Boolean chooseAll) throws Exception {
		this.chooseAll = chooseAll;
		// display();
	}

	class AlurPersetujuanSuratMasukTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) arg1;

			if (alurPersetujuanSuratMasuk == null || alurPersetujuanSuratMasuk.getId() == null) {
				treeitem.setVisible(false);
				return;
			}

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);
				Radio checkbox = new Radio(alurPersetujuanSuratMasuk.toString());

				if (parentOnly) {
					checkbox.setDisabled(alurPersetujuanSuratMasuk.getParent() != null);
				} else {
					checkbox.setDisabled(!(chooseAll
							|| alurPersetujuanSuratMasukTreeModel.getChildCount(alurPersetujuanSuratMasuk) == 0));
				}
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("alurPersetujuanSuratMasuk", alurPersetujuanSuratMasuk);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataAlurPersetujuanSuratMasukBanbox.this.setOpen(false);
						AmbilDataAlurPersetujuanSuratMasukBanbox.this.setAttribute("alurPersetujuanSuratMasuk",
								alurPersetujuanSuratMasuk);
						AmbilDataAlurPersetujuanSuratMasukBanbox.this.setValue(alurPersetujuanSuratMasuk.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(AlurPersetujuanSuratMasuk.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(alurPersetujuanSuratMasuk.getId())).uniqueResult();
						count = count == null ? 0L : count;
						alurPersetujuanSuratMasuk.setJmlDipakai(++count);

						Common.refreshSaveOrUpdate(session, alurPersetujuanSuratMasuk);

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		}

	}

	public void display() throws Exception {
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("700px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(panel);
		toolbar.appendChild(Common.createCleanButton(this, this));

		toolbar.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setCols(4);
		satuanKerja.setEventListener(new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		toolbar.appendChild(fakultas);
		fakultas.setCols(4);
		fakultas.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});
		Tbmuser tbmuser = Common.getCurrentUser();
		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", tbmuser.ambilFakultas()));

		toolbar.appendChild(jurusan);
		jurusan.setCols(4);
		jurusan.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		fakultas.setVisible(pt && fakultas.getChildren().size() > 1);
		jurusan.setVisible(pt && fakultas.getChildren().size() > 1);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		toolbar.appendChild(yayasan);
		yayasan.setCols(4);
		yayasan.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		toolbar.appendChild(sekolah);
		sekolah.setCols(4);
		sekolah.addEventListener("onChange", new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		yayasan.setVisible(ya);
		sekolah.setVisible(ya);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(panelchildren);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSoal = new MyTabConfig("Daftar");
		tabSoal.setParent(tabs);

		MyTabConfig tabJawaban = new MyTabConfig("Sering Dapakai");
		tabJawaban.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelUtama);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		tree = new Tree();
		tree.setZclass("z-dottree");
		tree.setParent(center);

		Treecols columns = new Treecols();

		columns.setParent(tree);

		Treecol column = new Treecol();
		column.setParent(columns);
		column.setLabel("Nama Item");

		final Tabpanel tabpanelUtamaD = new ais.ui.util.MyTabpanel();
		tabpanelUtamaD.setParent(tabpanels);
		tabpanelUtamaD.setHeight("450px");
		tabJawaban.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelUtamaD.getChildren().isEmpty()) {
					tabpanelUtamaD.appendChild(new AlurPersetujuanSuratMasukSeringDipakai());
				}

			}
		});

	}

	public void onSearchDefault(Event event) throws Exception {
		alurPersetujuanSuratMasukTreeModel = new AlurPersetujuanSuratMasukTreeModel(false, fakultas, jurusan, yayasan,
				sekolah, satuanKerja, null, tipe);
		tree.setModel(alurPersetujuanSuratMasukTreeModel);
		tree.setItemRenderer(new AlurPersetujuanSuratMasukTreeRenderer());
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	private class AlurPersetujuanSuratMasukSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;


	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		public AlurPersetujuanSuratMasukSeringDipakai() throws Exception {
			super();
			display();
		}

		private Textbox nama;

		class AlurPersetujuanSuratMasukRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				// TODO Auto-generated method stub
				final AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) arg1;

				Radio checkbox = new Radio(alurPersetujuanSuratMasuk.getNama());
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);

				checkbox.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataAlurPersetujuanSuratMasukBanbox.this.setOpen(false);
						AmbilDataAlurPersetujuanSuratMasukBanbox.this.setAttribute("alurPersetujuanSuratMasuk",
								alurPersetujuanSuratMasuk);
						AmbilDataAlurPersetujuanSuratMasukBanbox.this.setValue(alurPersetujuanSuratMasuk.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(AlurPersetujuanSuratMasuk.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(alurPersetujuanSuratMasuk.getId())).uniqueResult();
						count = count == null ? 0L : count;
						alurPersetujuanSuratMasuk.setJmlDipakai(++count);
						Common.refreshUpdate(session, (alurPersetujuanSuratMasuk));

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

			}

		}

		public void display() throws Exception {

			Center center = new Center();
			center.setParent(this);
			ais.ui.util.ZkCompat.setFlex(center, true);

			org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

			MyGrid searchgrid = new MyGrid();
			searchgrid.setWidth("100%");
			searchgrid.setParent(rowUtama);

			Rows rows = new Rows();
			rows.setParent(searchgrid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(nama = new Textbox());
			nama.setWidth("90%");

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});
			button.setParent(toolbar);

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
			/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
			 * client-side yang dibatasi MAX_RESULT_100. */
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

			Columns columns = new Columns();

			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama Item");

			onSearchDefault(null);

		}

		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) throws Exception {

			SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");
			Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
			if (parent != null) {
				satuanKerjas.clear();
				satuanKerjas.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
			}

			Session session = HibernateUtil.currentSession();
			List<AlurPersetujuanSuratMasuk> alurPersetujuanSuratMasuks = session.createCriteria(AlurPersetujuanSuratMasuk.class)

							.add(Restrictions.or(Restrictions.isNull("tipe"), Restrictions.eq("tipe", tipe)))

							.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											parent == null ? Restrictions.isNull("satuanKerja")
													: Restrictions.sqlRestriction("false"),
											Restrictions.in("satuanKerja", satuanKerjas)))

							.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("jurusan"),
											CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false)))

							.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("fakultas"),
											CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false)))

							.add(sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("sekolah"),
											CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false)))
							.add(yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(Restrictions.isNull("yayasan"),
											CommonSearchFilterHelper.eqSelectedWithId("yayasan", yayasan, false)))

							.add(Restrictions.eq("defaultItem", true)).addOrder(Order.desc("jmlDipakai"))
							.setMaxResults(Common.MAX_RESULT).list();
			ListModel strset = new SimpleListModel(alurPersetujuanSuratMasuks);
			grid.setRowRenderer(new AlurPersetujuanSuratMasukRenderer());
			grid.setModelCheckMobile(strset);

		}

	}

}
