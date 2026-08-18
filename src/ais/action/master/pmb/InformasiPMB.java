package ais.action.master.pmb;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Html;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import ais.ui.util.MyTabConfig;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import ais.ui.util.MyToolbarbuttonConfig;
import org.zkoss.zul.West;
import ais.ui.util.MyWindow;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;

public class InformasiPMB extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5826165925891023866L;

	private Center center;

	private Rows rows;

	private Tabs tabs;

	private Tabpanels tabpanels;

	public InformasiPMB() {
		super();

	}

	public InformasiPMB(String title, String border, boolean closable) {
		super(title, border, closable);

	}

	public void onCreate() {
		init();
	}

	public void init() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		center = new Center();
		center.setTitle("Informasi");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		tabs = new Tabs();
		tabs.setParent(tabbox);

		tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		if (perguruanTinggi != null) {

			final MyTabConfig tab = new MyTabConfig(perguruanTinggi.getNama());
			tab.setClosable(false);
			tab.setParent(tabs);

			Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setParent(tabpanels);

			Borderlayout subSubBorderlayout = new ais.ui.util.MyBorderlayout();
			subSubBorderlayout.setParent(tabpanelUtama);

			Center subcenter = new Center();
			subcenter.setParent(subSubBorderlayout);
			ais.ui.util.ZkCompat.setFlex(subcenter, true);
			subcenter.setBorder("none");

			MyGrid grids = new MyGrid();
			grids.setMold("paging");
			grids.setParent(subcenter);
			grids.setSclass("fgrid");

			Rows rows = new Rows();
			rows.setParent(grids);

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);

			final Html msg = new ais.ui.util.MyHtml();
			msg.setContent(perguruanTinggi.getDeskripsi());
			msg.setParent(row);

		}

		final West west = new West();
		west.setTitle("Program Studi");
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("250px");
		west.setBorder("none");

		Borderlayout subBorderlayout = new ais.ui.util.MyBorderlayout();
		subBorderlayout.setParent(west);

		North subNorth = new North();
		subNorth.setParent(subBorderlayout);
		subNorth.setHeight("25px");
		subNorth.setBorder("none");

		Borderlayout subSubBorderlayout = new ais.ui.util.MyBorderlayout();
		subSubBorderlayout.setParent(subNorth);

		West subSubwest = new West();
		subSubwest.setParent(subSubBorderlayout);
		subSubwest.setWidth("80%");
		subSubwest.setBorder("none");

		final Textbox cari = new Textbox();
		cari.setWidth("90%");
		cari.setParent(subSubwest);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.setWidth("90%");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		Center subsubcenter = new Center();
		subsubcenter.setParent(subSubBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subsubcenter, true);
		button.setParent(subsubcenter);
		subsubcenter.setBorder("none");

		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(cari.getValue().trim());
			}
		});

		Center subcenter = new Center();
		subcenter.setParent(subBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subcenter, true);
		subcenter.setBorder("none");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(subcenter);
		grid.setWidth("100%");
		grid.setHeight("100%");

		rows = new Rows();
		rows.setParent(grid);
		loadData(cari.getValue());

		west.setCollapsible(true);
		west.setSplittable(true);
		Common.createDefaultTimerNoBusy(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				west.setOpen(false);
			}
		}, "", false, 3000);
	}

	@SuppressWarnings("unchecked")
	public void loadData(String keyword) {

		Common.clear(rows);

		Session session = HibernateUtil.currentSession();
		List<Fakultas> fakultases = session.createCriteria(Fakultas.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).list();

		for (final Fakultas fakultas : fakultases) {

			final Group group = new ais.ui.util.MyGroupConfig();
			group.setParent(rows);

			A a = new A(fakultas.getNama());
			a.setParent(group);

			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					prosess(fakultas);
				}
			});

			List<Jurusan> jurusans = session.createCriteria(Jurusan.class).add(Restrictions.eq("fakultas", fakultas))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(keyword.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE))
					.addOrder(Order.asc("nama")).list();

			for (final Jurusan jurusan : jurusans) {

				final MyFormRow row = new MyFormRow();row.setValign("top");

				row.setParent(rows);

				final A toolbarbutton = new A(jurusan.toString());

				row.appendChild(toolbarbutton);
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.scrollIntoView(row);
						prosess(jurusan);
					}
				});

			}
		}

	}

	@SuppressWarnings("unchecked")
	private void prosess(final Jurusan jurusan) {
		List<Tabpanel> tabpanels = InformasiPMB.this.tabpanels.getChildren();
		synchronized (tabpanels) {
			for (Tabpanel myTabpanel : tabpanels) {

				if (myTabpanel.getAttribute("jurusan") == null) {
					continue;
				}

				Jurusan myjurusan = (Jurusan) myTabpanel.getAttribute("jurusan");

				if (myjurusan.getNama().equals(jurusan.getNama())) {
					myTabpanel.getLinkedTab().setSelected(true);
					return;
				}

			}

			final MyTabConfig tab = new MyTabConfig(jurusan.getNama());
			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			// tabs.setHeight("0px");
			// tab.setHeight("0px");
			tab.setClosable(false);
			// tabs.setVisible(false);
			tab.setParent(tabs);

			tabpanel.setParent(InformasiPMB.this.tabpanels);
			tabpanel.setAttribute("jurusan", jurusan);

			Borderlayout subSubBorderlayout = new ais.ui.util.MyBorderlayout();
			subSubBorderlayout.setParent(tabpanel);

			Center subcenter = new Center();
			subcenter.setParent(subSubBorderlayout);
			ais.ui.util.ZkCompat.setFlex(subcenter, true);
			subcenter.setBorder("none");

			MyGrid grids = new MyGrid();
			grids.setMold("paging");
			grids.setParent(subcenter);
			grids.setSclass("fgrid");

			Rows rows = new Rows();
			rows.setParent(grids);

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);

			final Html msg = new ais.ui.util.MyHtml();
			msg.setContent(jurusan.getDeskripsi());
			msg.setParent(row);

			tab.setSelected(true);
		}

	}

	@SuppressWarnings("unchecked")
	private void prosess(final Fakultas fakultas) {
		List<Tabpanel> tabpanels = InformasiPMB.this.tabpanels.getChildren();
		synchronized (tabpanels) {
			for (Tabpanel myTabpanel : tabpanels) {

				if (myTabpanel.getAttribute("fakultas") == null) {
					continue;
				}

				Fakultas myfakultas = (Fakultas) myTabpanel.getAttribute("fakultas");

				if (myfakultas.getNama().equals(fakultas.getNama())) {
					myTabpanel.getLinkedTab().setSelected(true);
					return;
				}

			}

			final MyTabConfig tab = new MyTabConfig(fakultas.getNama());
			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			// tabs.setHeight("0px");
			// tab.setHeight("0px");
			tab.setClosable(false);
			// tabs.setVisible(false);
			tab.setParent(tabs);

			tabpanel.setParent(InformasiPMB.this.tabpanels);
			tabpanel.setAttribute("fakultas", fakultas);

			Borderlayout subSubBorderlayout = new ais.ui.util.MyBorderlayout();
			subSubBorderlayout.setParent(tabpanel);

			Center subcenter = new Center();
			subcenter.setParent(subSubBorderlayout);
			ais.ui.util.ZkCompat.setFlex(subcenter, true);
			subcenter.setBorder("none");

			MyGrid grids = new MyGrid();
			grids.setMold("paging");
			grids.setParent(subcenter);
			grids.setSclass("fgrid");

			Rows rows = new Rows();
			rows.setParent(grids);

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);

			final Html msg = new ais.ui.util.MyHtml();
			msg.setContent(fakultas.getDeskripsi());
			msg.setParent(row);

			tab.setSelected(true);
		}

	}
}
