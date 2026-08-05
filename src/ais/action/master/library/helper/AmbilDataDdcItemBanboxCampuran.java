package ais.action.master.library.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
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
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import ais.ui.util.MyRadioConfig;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import ais.action.master.library.util.DdcItemTreeModel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DdcItem;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataDdcItemBanboxCampuran extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 6452461056684904810L;
	protected Tree tree;

	protected EventListener eventListener;
	protected DdcItemTreeModel ddcItemTreeModel;

	private Boolean chooseAll = false;
	private boolean hasDisplayed = false;

	public AmbilDataDdcItemBanboxCampuran() throws Exception {
		this(true);
	}

	public AmbilDataDdcItemBanboxCampuran(Boolean chooseAll) throws Exception {
		super();
		ddcItemTreeModel = new DdcItemTreeModel(false);
		this.chooseAll = chooseAll;

		final Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("700px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (hasDisplayed) {
					return;
				}
				display(radiogroup);
			}
		});

	}

	public void setChooseAll(Boolean chooseAll) throws Exception {
		this.chooseAll = chooseAll;
		// display();
	}

	class DdcItemTreeRenderer extends ais.ui.util.MyTreeitemRenderer {

		@Override
		public void render(final Treeitem treeitem, Object arg1) {
			// TODO Auto-generated method stub
			final DdcItem ddcItem = (DdcItem) arg1;

			try {
				Treerow treerow = new Treerow();
				treerow.setParent(treeitem);

				Treecell arg0 = new Treecell();
				arg0.setParent(treerow);

				new Label(ddcItem.toString()).setParent(arg0);

				arg0 = new Treecell();
				arg0.setParent(treerow);
				MyRadioConfig checkbox = new MyRadioConfig();
				checkbox.setVisible(chooseAll || ddcItemTreeModel.getChildCount(ddcItem) == 0);
				checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
				checkbox.setAttribute("ddcItem", ddcItem);

				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataDdcItemBanboxCampuran.this.setOpen(false);
						AmbilDataDdcItemBanboxCampuran.this.setAttribute("ddcItem", ddcItem);
						AmbilDataDdcItemBanboxCampuran.this.setValue(ddcItem.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(DdcItem.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(ddcItem.getId())).uniqueResult();
						count = count == null ? 0L : count;
						ddcItem.setJmlDipakai(++count);
						Common.refreshUpdate(session, (ddcItem));

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

	public void display(Radiogroup radiogroup) throws Exception {
		if (hasDisplayed) {
			return;
		}
		hasDisplayed = true;
		Common.clear(radiogroup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		toolbar.appendChild(Common.createCleanButton(this, this));

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

		column = new Treecol();
		column.setParent(columns);
		column.setLabel("Pilih");
		column.setWidth("10%");

		onSearchDefault(null);

		tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setParent(tabpanels);
		tabpanelUtama.appendChild(new DdcItemSeringDipakai());

	}

	public void onSearchDefault(Event event) throws Exception {

		tree.setModel(ddcItemTreeModel);
		tree.setItemRenderer(new DdcItemTreeRenderer());
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	private class DdcItemSeringDipakai extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6452461056684904810L;
		private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
		public DdcItemSeringDipakai() throws Exception {
			super();
			display();
		}

		private MyTextbox nama;
		private MyTextbox kode;

		class DdcItemRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
				// TODO Auto-generated method stub
				final DdcItem ddcItem = (DdcItem) arg1;

				arg0.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						AmbilDataDdcItemBanboxCampuran.this.setOpen(false);
						AmbilDataDdcItemBanboxCampuran.this.setAttribute("ddcItem", ddcItem);
						AmbilDataDdcItemBanboxCampuran.this.setValue(ddcItem.toString());

						Session session = HibernateUtil.currentSession();
						Long count = (Long) session.createCriteria(DdcItem.class)
								.setProjection(Projections.property("jmlDipakai"))
								.add(Restrictions.idEq(ddcItem.getId())).uniqueResult();
						count = count == null ? 0L : count;
						ddcItem.setJmlDipakai(++count);
						Common.refreshUpdate(session, (ddcItem));

						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					}
				});

				new Label(ddcItem.getNama()).setParent(arg0);
				new Label(ddcItem.getParent() == null ? "" : ddcItem.getParent().getNama()).setParent(arg0);

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

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
			row.appendChild(kode = new MyTextbox());
			kode.setWidth("90%");
			kode.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(nama = new MyTextbox());
			nama.setWidth("90%");
			nama.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

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

			grid = new MyGrid();//grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			/* setPageSize legacy dihapus: grid bukan mold "paging" sehingga setPageSize melempar IllegalStateException ("Available only the paging mold") dan daftar tidak pernah tampil. Paging ditangani AmbilDataPagingHelper. */
			/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
			 * client-side yang dibatasi MAX_RESULT. */
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
			column.setLabel("Kode DDC");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama DDC");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Parent");
			column.setWidth("25%");

			onSearchDefault(null);

		}

		@SuppressWarnings("unchecked")
		public void onSearchDefault(Event event) {

			Session session = HibernateUtil.currentSession();

			List<DdcItem> myDdcItem = session.createCriteria(DdcItem.class)
					// .add(Restrictions.eq("defaultItem", true))
					.addOrder(Order.asc("kode"))
					.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(kode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.ANYWHERE))
					.setMaxResults(Common.MAX_RESULT).list();
			ListModel strset = new SimpleListModel(myDdcItem);
			grid.setRowRenderer(new DdcItemRenderer());
			grid.setModelCheckMobile(strset);

			
		}

	}

}
