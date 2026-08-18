package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import ais.common.Common;
import ais.common.scholar.GoogleScholarCrawler;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ScholarArticle;
import ais.database.model.ScholarAuthor;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataDariGoogleScholarBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	private EventListener eventListener;

	public JsonFactory jsonFactory = new JacksonFactory();

	private Set<ScholarArticle> ids = new HashSet<ScholarArticle>();
	private List<ScholarArticle> itemselected = new ArrayList<ScholarArticle>();
	private String katakunci = "";

	public AmbilDataDariGoogleScholarBanyak(String katakunci) {
		super();
		this.katakunci = katakunci;
		try {
			display();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/library/helper/AmbilDataDariGoogleScholarBanyak.java:82");
		}
	}

	private MyTextbox title;

	private Paging paging;
	private MyGrid gridSebelumnya;
	private MyTabConfig tab1;
	private MyTabConfig tab1AsistenMahasiswa;

	@SuppressWarnings("deprecation")
	public void display() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Grid searchgrid = new Grid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(north);

		Columns columns = new Columns();
		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth(Common.isMobile() ? "30%" : "15%");

		column = new MyColumnConfig();

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelBoldAja(
				"Untuk mengambil artikel dari google scholar, masukkan kata kunci pencarian berikut :"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kata kunci"));
		row.appendChild(title = new MyTextbox());
		title.setWidth("90%");
		title.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(row);

		Borderlayout borderlayoutsub = new ais.ui.util.MyBorderlayout();
		borderlayoutsub.setParent(center);
		Center centersub = new Center();
		centersub.setParent(borderlayoutsub);
		ais.ui.util.ZkCompat.setFlex(centersub, true);

		South southsub = new South();
		southsub.setParent(borderlayoutsub);
		paging = new Paging();
		paging.setMold("os");
		southsub.appendChild(paging);
		paging.setPageSize(5);
		paging.setVisible(false);
		paging.addEventListener("onPaging", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(centersub);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		tab1AsistenMahasiswa = new MyTabConfig();
		tab1AsistenMahasiswa.setSelected(true);
		tab1AsistenMahasiswa.setParent(tabs);
		tab1AsistenMahasiswa.setLabel("Daftar artikel yang sebelumnya sudah diambil");

		tab1 = new MyTabConfig();
		tab1.setParent(tabs);
		tab1.setLabel("Cari artikel lain di Google Scholar");
		tab1.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				paging.setVisible(false);
				if ((katakunci != null && !katakunci.trim().isEmpty()) || !title.getValue().trim().isEmpty()) {
					onSearchDefault(arg0);
				}
			}
		});

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel managemenPenilaian = new ais.ui.util.MyTabpanel();
		managemenPenilaian.setParent(tabpanels);

		MyBorderlayout borderlayoutsubsub = new ais.ui.util.MyBorderlayout();
		borderlayoutsubsub.setParent(managemenPenilaian);
		Center centersubsub = new Center();
		centersubsub.setParent(borderlayoutsubsub);
		ais.ui.util.ZkCompat.setFlex(centersubsub, true);

		gridSebelumnya = new MyGrid();
		gridSebelumnya.setWidth("100%");
		gridSebelumnya.setMold("paging");
		gridSebelumnya.setPageSize(1000);
		gridSebelumnya.setParent(centersubsub);

		columns = new Columns();
		columns.setParent(gridSebelumnya);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Judul");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penulis");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Deskripsi");
		column.setWidth("55%");

		managemenPenilaian = new ais.ui.util.MyTabpanel();
		managemenPenilaian.setParent(tabpanels);

		borderlayoutsubsub = new ais.ui.util.MyBorderlayout();
		borderlayoutsubsub.setParent(managemenPenilaian);
		centersubsub = new Center();
		centersubsub.setParent(borderlayoutsubsub);
		ais.ui.util.ZkCompat.setFlex(centersubsub, true);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(centersubsub);

		columns = new Columns();
		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Judul");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penulis");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Deskripsi");
		column.setWidth("55%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataDariGoogleScholarBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(final Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (eventListener != null) {
							List<ScholarArticle> volumes = new ArrayList<ScholarArticle>();
							if (grid != null && grid.getRows() != null) {
								List<Row> rows = grid.getRows().getChildren();
								for (Row row : rows) {
									if (row.getAttribute("checkbox") != null) {
										MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");

										if (checkbox.isChecked() && !checkbox.isDisabled()) {
											ScholarArticle scholarArticle = (ScholarArticle) row
													.getAttribute("scholarArticle");
											volumes.add(scholarArticle);
										}
									}
								}
							}

							if (gridSebelumnya != null && gridSebelumnya.getRows() != null) {
								List<Row> rows = gridSebelumnya.getRows().getChildren();
								for (Row row : rows) {
									if (row.getAttribute("checkbox") != null) {
										MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");

										if (checkbox.isChecked() && !checkbox.isDisabled()) {
											ScholarArticle scholarArticle = (ScholarArticle) row
													.getAttribute("scholarArticle");
											volumes.add(scholarArticle);
										}
									}
								}
							}

							System.out.println("volumes => " + volumes);

							Event myEvent = new Event("myEvent", event.getTarget(), volumes);
							eventListener.onEvent(myEvent);
						}
						AmbilDataDariGoogleScholarBanyak.this.detach();
					}
				});
			}
		});
		button.setParent(toolbar);

		onSearchDefault(null);
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) throws Exception {

		if (tab1.isSelected()) {
			final List<ScholarArticle> articleList = new ArrayList<ScholarArticle>();
			articleList.addAll(ids);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					int total = 500;
					int sekaliAmbil = 10;
					paging.setVisible(total > sekaliAmbil);
					paging.setPageSize(sekaliAmbil);
					paging.setTotalSize(total);
					if (paging.isVisible()) {
						((South) paging.getParent()).setHeight("25px");
					}

					ListModel strset = new SimpleListModel(articleList);
					grid.setRowRenderer(new ItemRendererBaru());
					grid.setModelCheckMobile(strset);
					grid.renderAll();
				}
			};

			if (!katakunci.trim().isEmpty() || !title.getValue().trim().isEmpty()) {
				final Label label = Common.displayLoadBar(eventListener);

				new Thread(new Runnable() {

					@Override
					public void run() {
						String query = title.getValue().trim().isEmpty() ? katakunci.trim() : title.getValue().trim();
						try {
							GoogleScholarCrawler googleScholarCrawler = new GoogleScholarCrawler(label);
							articleList.addAll(googleScholarCrawler
									.startCrawl(10 * (paging == null ? 0 : paging.getActivePage()), query));
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/library/helper/AmbilDataDariGoogleScholarBanyak.java:388");
						}
					}
				}).start();
			} else {
				eventListener.onEvent(null);
			}

		} else if (tab1AsistenMahasiswa.isSelected()) {
			Common.initPaging(initCriteria(false), paging);
			List<ScholarArticle> item = new ArrayList<ScholarArticle>();
			item.addAll(itemselected);

			List<ScholarArticle> myItem = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

			item.addAll(myItem);

			ListModel strset = new SimpleListModel(item);
			gridSebelumnya.setRowRenderer(new ItemRendererBaru());
			gridSebelumnya.setModelCheckMobile(strset);
		}
	}

	class ItemRendererBaru extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final ScholarArticle scholarArticle = (ScholarArticle) arg1;
			arg0.setAttribute("scholarArticle", scholarArticle);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (ScholarArticle myItem : itemselected) {
				if (myItem.getId().equals(scholarArticle.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						itemselected.add(scholarArticle);
					} else {
						itemselected.remove(scholarArticle);
					}
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(scholarArticle.getNama()).setParent(vbox);
			MyButtonConfig myButtonConfig = new MyButtonConfig("Lihat Isi Artikel",
					"/img/education-university-icon.png");
			myButtonConfig.setParent(vbox);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(scholarArticle.getLink(), "_blank");
					} else {
						Clients.evalJavaScript("popupCenter({url: '" + scholarArticle.getLink()
								+ "', title: 'Artikel', w: 1200, h: 600});");
					}
				}
			});

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			HibernateUtil.currentSession().refresh(scholarArticle);
			for (final ScholarAuthor scholarAuthor : scholarArticle.getScholarAuthors()) {

				if (scholarAuthor.getKeterangan() == null || scholarAuthor.getKeterangan().equalsIgnoreCase("empty")) {
					new Label(scholarAuthor.getNama()).setParent(hbox);
				} else {
					Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig(scholarAuthor.getNama(),
							"/img/education-university-icon.png");
					toolbarbutton.setParent(hbox);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (Common.isMobile()) {
								ExecutionsCtrl.getCurrent().sendRedirect(scholarAuthor.getKeterangan(), "_blank");
							} else {
								Clients.evalJavaScript("popupCenter({url: '" + scholarAuthor.getKeterangan()
										+ "', title: 'Author', w: 1200, h: 600});");
							}
						}
					});
				}
			}

			new ais.ui.util.MyHtml("<div style='font-size:8px'>" + scholarArticle.getKeterangan() + "</div>").setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> ids = new ArrayList<Long>();
		for (ScholarArticle scholarArticle : itemselected) {
			ids.add(scholarArticle.getId());
		}

		Criteria criteria = session.createCriteria(ScholarArticle.class)
				.add(ids.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.not(Restrictions.in("id", ids)))

				.add(title.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", title.getValue().trim(), MatchMode.ANYWHERE))

				.add(katakunci.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kewords", katakunci.trim(), MatchMode.ANYWHERE));
		if (order) {
			criteria.addOrder(Order.asc("nama"));
		}

		return criteria;
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
