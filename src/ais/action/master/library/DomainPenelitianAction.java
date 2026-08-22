package ais.action.master.library;

import java.util.List;

import org.hibernate.Criteria;
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
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import ais.ui.util.MyTabConfig;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataDomainPenelitianBanbox;
import ais.action.master.library.helper.AmbilDataPenerbitBanbox;
import ais.action.master.library.helper.DomainPenelitianPunyaPemeriksaHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.DomainPenelitianDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DomainPenelitian;
import ais.database.model.library.Penerbit;
import ais.database.model.library.PenerbitPunyaPemeriksa;

public class DomainPenelitianAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataDomainPenelitianBanbox searchparent;
	private AmbilDataPenerbitBanbox searchpenerbit;

	private MyCheckboxConfig aktif;
	private Textbox nama;
	private AmbilDataDomainPenelitianBanbox parent;
	private AmbilDataPenerbitBanbox penerbit;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private DomainPenelitian domainPenelitian;
	private MyToolbarbuttonConfig add;
	private MyGrid gridPemeriksa;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {Common.doCheckSecurity();return super.doBeforeCompose(page, parent, compInfo);}public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		add.setVisible(CommonPrivilages
				.checkPrevilages(CommonPrivilages.CREATE));
		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		searchpenerbit.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	protected void initDetail(final DomainPenelitian domainPenelitian,
			Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabPemeriksa = new MyTabConfig("Default Pemeriksa");
		tabPemeriksa.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelPemeriksa = new ais.ui.util.MyTabpanel();
		tabpanelPemeriksa.setParent(tabpanels);

		tabpanelPemeriksa.appendChild(new DomainPenelitianPunyaPemeriksaHelper(
				gridPemeriksa = new MyGrid()).initDetail(domainPenelitian));
	}

	class DomainPenelitianRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final DomainPenelitian domainPenelitian = (DomainPenelitian) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("450px");
						window.setWidth("100%");
						window.setParent(detail);
						initDetail(domainPenelitian, window);
					}
				}
			});

			RevisiHelper.createNewRevisi(DomainPenelitian.class,
					domainPenelitian, domainPenelitian.getNama()).setParent(
					arg0);
			new Label(domainPenelitian.getPenerbit() == null ? ""
					: domainPenelitian.getPenerbit().getNama()).setParent(arg0);
			new Label(domainPenelitian.getParent() == null ? ""
					: domainPenelitian.getParent().getNama()).setParent(arg0);
			new Label(domainPenelitian.getDefaultItem() == null
					|| !domainPenelitian.getDefaultItem() ? "Tidak Aktif"
					: "Aktif").setParent(arg0);
			new Label(domainPenelitian.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(domainPenelitian);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											
											Common.refreshDelete(domainPenelitian);
											
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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new DomainPenelitian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(DomainPenelitian domainPenelitian) throws Exception {
		this.domainPenelitian = domainPenelitian;
		addWindow.setTitle(domainPenelitian.getId() == null ? "Tambah Domain Penelitian" : "Ubah Domain Penelitian");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("60%");

		initDetail(domainPenelitian, east);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Domain Penelitian"));
		row.appendChild(nama = new Textbox(
				domainPenelitian.getNama() == null ? "" : domainPenelitian
						.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit / Instansi"));
		row.appendChild(penerbit = new AmbilDataPenerbitBanbox());
		penerbit.setAttribute("penerbit", domainPenelitian.getPenerbit());
		penerbit.setValue(domainPenelitian.getPenerbit() == null ? ""
				: domainPenelitian.getPenerbit().getNama());
		penerbit.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Parent"));
		row.appendChild(parent = new AmbilDataDomainPenelitianBanbox());
		parent.setAttribute("domainPenelitian", domainPenelitian.getParent());
		parent.setValue(domainPenelitian.getParent() == null ? ""
				: domainPenelitian.getParent().getNama());
		parent.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(aktif = new MyCheckboxConfig());
		aktif.setChecked(domainPenelitian.getDefaultItem() != null
				&& domainPenelitian.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(domainPenelitian
				.getKeterangan() == null ? "" : domainPenelitian
				.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
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
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Domain Penelitian harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (penerbit.getAttribute("penerbit") == null) {
			MyMessageboxConfig.show("Penerbit atau instansi penerbit harus diisi",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		List<Row> rowsPemeriksa = gridPemeriksa.getRows().getChildren();
		for (Row row : rowsPemeriksa) {
			PenerbitPunyaPemeriksa penerbitPunyaPemeriksa = (PenerbitPunyaPemeriksa) row
					.getAttribute("penerbitPunyaPemeriksa");
			if (penerbitPunyaPemeriksa.getPemeriksa() == null) {
				MyMessageboxConfig.show("Pemeriksa harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		DomainPenelitianDao domainPenelitianDao = DaoFactory.getInstance()
				.getDomainPenelitianDao();
		if (domainPenelitian.getId() != null) {
			domainPenelitian = domainPenelitianDao.load(domainPenelitian
					.getId());

		}

		domainPenelitian.setPenerbit((Penerbit) penerbit
				.getAttribute("penerbit"));
		domainPenelitian.setParent((DomainPenelitian) parent
				.getAttribute("domainPenelitian"));
		domainPenelitian.setDefaultItem(aktif.isChecked());
		domainPenelitian.setNama(nama.getValue());
		domainPenelitian.setKeterangan(keterangan.getValue());

		if (domainPenelitian.getId() != null) {
			domainPenelitianDao.update(domainPenelitian);
		} else {
			domainPenelitianDao.save(domainPenelitian);
		}

		Session session = HibernateUtil.currentSession();
		for (Row row : rowsPemeriksa) {
			PenerbitPunyaPemeriksa penerbitPunyaPemeriksa = (PenerbitPunyaPemeriksa) row
					.getAttribute("penerbitPunyaPemeriksa");
			penerbitPunyaPemeriksa.setDomainPenelitian(domainPenelitian);
			penerbitPunyaPemeriksa.setPenerbit(domainPenelitian.getPenerbit());
			session.saveOrUpdate(penerbitPunyaPemeriksa);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		DomainPenelitian domainPenelitian = (DomainPenelitian) searchparent
				.getAttribute("domainPenelitian");
		Penerbit penerbit = (Penerbit) searchpenerbit.getAttribute("penerbit");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DomainPenelitian.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(
				Restrictions.ilike("nama", searchnama.getValue(),
						MatchMode.ANYWHERE))
				.add(domainPenelitian == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("parent",
						domainPenelitian))
				.add(penerbit == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("penerbit", penerbit));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DomainPenelitian> domainPenelitian = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(domainPenelitian);
		grid.setRowRenderer(new DomainPenelitianRenderer());
		grid.setModelCheckMobile(strset);

		

	}

	// public Boolean checkNamaDomainPenelitian() {
	//
	// Integer kotaCount = null;
	// Session session = HibernateUtil.currentSession();
	// kotaCount = ((Number) session
	// .createCriteria(DomainPenelitian.class)
	// .setProjection(Projections.rowCount())
	// .add(Restrictions.eq("nama", nama.getValue().trim()))
	// .add(this.domainPenelitian.getId() == null ? Restrictions
	// .sqlRestriction("1=1") : Restrictions.ne("id",
	// this.domainPenelitian.getId())).uniqueResult())
	// .intValue();
	//
	// return !kotaCount.equals(0);
	// }

}
