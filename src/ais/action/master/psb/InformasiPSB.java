package ais.action.master.psb;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
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
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class InformasiPSB extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5826165925891023866L;

	private Center center;

	private Rows rows;

	private Tabs tabs;

	private Tabpanels tabpanels;

	public InformasiPSB() {
		super();

	}

	public InformasiPSB(String title, String border, boolean closable) {
		super(title, border, closable);

	}

	public void onCreate() {
		init();
	}

	public void init() {

		if (Common.isMobile()) {

		} else {
			this.setStyle("border-radius:25px;");
		}

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

		final MyTabConfig tab = new MyTabConfig("Tentang PSB");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		String defaultValue = ""
				+ "<div style='padding: 90px;border: 1px solid #4CAF50;'><div style=\"color: rgb(0, 0, 0); font-family: arial; font-weight: 700; text-align: center;\">"
				+ "<img alt=\"\" src=\"" + request.getContextPath()
				+ "/img/logo.png\" style=\"border: 0px; width: 128px; height: 130px;\" /></div>"
				+ "<h2>Selamat Datang di Seleksi Penerimaan Siswa Baru "
				+ Common.getKonfigurasi("label_instansi_sekolah", "Nama Instansi Sekolah").getNilai() + "</h2>" + "<p>"
				+ Common.getKonfigurasi("info_banner_psb",
						"Kegiatan seleksi penerimaan siswa baru merupakan kegiatan yang bertujuan mendapatkan calon siswa yang berkualitas dan memiliki kompetensi dasar yang baik sesuai dengan standar yang ditetapkan. Kegiatan ini merupaka kegiatan rutin bagi , karena itu penyelenggaraannya harus profesional, terjamin, terukur dan efesien.")
						.getNilai()
				+ "</p><div>";

		final Html msg = new ais.ui.util.MyHtml();
		msg.setContent(defaultValue);
		msg.setParent(row);

		final West west = new West();
		west.setTitle("Sekolah");
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

		subSubBorderlayout = new ais.ui.util.MyBorderlayout();
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

		subcenter = new Center();
		subcenter.setParent(subBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subcenter, true);
		subcenter.setBorder("none");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(subcenter);
		grid.setWidth("100%");
		grid.setHeight("100%");

		this.rows = new Rows();
		this.rows.setParent(grid);
		loadData(cari.getValue());

		west.setCollapsible(true);
		west.setSplittable(true);

	}

	@SuppressWarnings("unchecked")
	public void loadData(String keyword) {

		Common.clear(rows);

		Session session = HibernateUtil.currentSession();
		List<Yayasan> yayasanes = ConstantValues
				.simpleList(session.createCriteria(Yayasan.class).addOrder(Order.asc("nama")), Yayasan.class);

		for (final Yayasan yayasan : yayasanes) {

			final Group group = new ais.ui.util.MyGroupConfig();
			group.setParent(rows);

			A a = new A(yayasan.getNama());
			a.setParent(group);

			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					prosess(yayasan);
				}
			});

			List<Sekolah> sekolahs = ConstantValues.simpleList(session.createCriteria(Sekolah.class)
					.add(yayasan == null || yayasan.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("yayasan", yayasan))
					.add(keyword.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE))
					.addOrder(Order.asc("nama")), Sekolah.class);

			for (final Sekolah sekolah : sekolahs) {

				final MyFormRow row = new MyFormRow();
				row.setValign("top");

				row.setParent(rows);

				final A toolbarbutton = new A(sekolah.toString());

				row.appendChild(toolbarbutton);
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.scrollIntoView(row);
						prosess(sekolah);
					}
				});

			}
		}

	}

	@SuppressWarnings("unchecked")
	private void prosess(final Sekolah sekolah) {
		List<Tabpanel> tabpanels = InformasiPSB.this.tabpanels.getChildren();
		synchronized (tabpanels) {
			for (Tabpanel myTabpanel : tabpanels) {

				if (myTabpanel.getAttribute("sekolah") == null) {
					continue;
				}

				Sekolah mysekolah = (Sekolah) myTabpanel.getAttribute("sekolah");

				if (mysekolah.getNama().equals(sekolah.getNama())) {
					myTabpanel.getLinkedTab().setSelected(true);
					return;
				}

			}

			final MyTabConfig tab = new MyTabConfig(sekolah.getNama());
			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			// tabs.setHeight("0px");
			// tab.setHeight("0px");
			tab.setClosable(false);
			// tabs.setVisible(false);
			tab.setParent(tabs);

			tabpanel.setParent(InformasiPSB.this.tabpanels);
			tabpanel.setAttribute("sekolah", sekolah);

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

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			final Html msg = new ais.ui.util.MyHtml();
			msg.setContent(sekolah.getDeskripsi());
			msg.setParent(row);

			tab.setSelected(true);
		}

	}

	@SuppressWarnings("unchecked")
	private void prosess(final Yayasan yayasan) {
		List<Tabpanel> tabpanels = InformasiPSB.this.tabpanels.getChildren();
		synchronized (tabpanels) {
			for (Tabpanel myTabpanel : tabpanels) {

				if (myTabpanel.getAttribute("yayasan") == null) {
					continue;
				}

				Yayasan myyayasan = (Yayasan) myTabpanel.getAttribute("yayasan");

				if (myyayasan.getNama().equals(yayasan.getNama())) {
					myTabpanel.getLinkedTab().setSelected(true);
					return;
				}

			}

			final MyTabConfig tab = new MyTabConfig(yayasan.getNama());
			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			// tabs.setHeight("0px");
			// tab.setHeight("0px");
			tab.setClosable(false);
			// tabs.setVisible(false);
			tab.setParent(tabs);

			tabpanel.setParent(InformasiPSB.this.tabpanels);
			tabpanel.setAttribute("yayasan", yayasan);

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

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			final Html msg = new ais.ui.util.MyHtml();
			msg.setContent(yayasan.getDeskripsi());
			msg.setParent(row);

			tab.setSelected(true);
		}

	}
}
