package ais.action.master.library;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataDdcItemBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.DdcItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.DataDdcItem;
import ais.database.model.library.DdcItem;
import ais.database.model.library.VersiDdcItem;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DdcItemAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Textbox searchnama;
	private Combobox searchversi;

	private MyCheckboxConfig aktif;
	private Textbox kode;
	private Textbox nama;
	private AmbilDataDdcItemBanbox parent;
	private Combobox versiDdcItem;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private DdcItem ddcItem;
	private MyToolbarbuttonConfig add;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertCombo(searchversi, "nama", VersiDdcItem.class);
		Common.insertCombo(versiDdcItem = new Combobox(), "nama", VersiDdcItem.class);

		if (searchversi.getChildren().size() > 0) {
			searchversi.setSelectedIndex(0);
		}

		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(DdcItem.class).add(Restrictions.isNotNull("parent"))
				.add(Restrictions.not(Restrictions.ilike("kode", ".", MatchMode.ANYWHERE)))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		System.out.println("count = " + count);
		if (count > 0) {
			List<Long> ddcItems = session.createCriteria(DdcItem.class).setProjection(Projections.property("id"))
					.add(Restrictions.isNotNull("parent"))
					.add(Restrictions.not(Restrictions.ilike("kode", ".", MatchMode.ANYWHERE)))
					.addOrder(Order.asc("kode")).list();
			for (Long ddcItemId : ddcItems) {
				DdcItem ddcItem = (DdcItem) session.createCriteria(DdcItem.class).add(Restrictions.idEq(ddcItemId))
						.uniqueResult();
				// String newCode = ddcItem.getKode().replaceFirst(
				// ddcItem.getParent().getKode().replaceAll(".", ""),
				// ddcItem.getParent().getKode() + ".");

				String newCode = ddcItem.getParent().getKode() + "."
						+ ddcItem.getKode().substring(ddcItem.getParent().getKode().replaceAll("\\.", "").length());

				System.out.println("kode asli = " + ddcItem.getKode() + ", kode baru = " + newCode);

				// newCode = index.equals(-1) ? newCode :
				// newCode.substring(index);
				// String[] newCodes = newCode.split("\\.");
				ddcItem.setKode(newCode);
				Common.refreshUpdate(session, (ddcItem));
			}
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "parent", "versiDdcItem", "keterangan",
				"defaultItem" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, DdcItem.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class DdcItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final DdcItem ddcItem = (DdcItem) arg1;

			new Label(ddcItem.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(DdcItem.class, ddcItem, ddcItem.getNama()).setParent(arg0);
			new Label(ddcItem.getParent() == null ? "" : ddcItem.getParent().getNama()).setParent(arg0);
			new Label(ddcItem.getVersiDdcItem() == null ? "" : ddcItem.getVersiDdcItem().getNama()).setParent(arg0);
			new Label(ddcItem.getDefaultItem() == null || !ddcItem.getDefaultItem() ? "Tidak Aktif" : "Aktif")
					.setParent(arg0);
			new Label(ddcItem.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(ddcItem);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								try {

									Common.refreshDelete(ddcItem);

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
		init(new DdcItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(DdcItem ddcItem) throws Exception {
		this.ddcItem = ddcItem;
		addWindow.setTitle(ddcItem.getId() == null ? "Tambah Ddc Item" : "Ubah Ddc Item");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Parent DDC"));
		row.appendChild(parent = new AmbilDataDdcItemBanbox());
		parent.setAttribute("ddcItem", ddcItem.getParent());
		parent.setValue(ddcItem.getParent() == null ? "" : ddcItem.getParent().getNama());
		parent.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Versi DDC"));
		row.appendChild(versiDdcItem);
		Common.selectComboItem(versiDdcItem, ddcItem.getVersiDdcItem());
		versiDdcItem.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode DDC"));
		row.appendChild(kode = new Textbox(ddcItem.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama DDC"));
		row.appendChild(nama = new Textbox(ddcItem.getNama() == null ? "" : ddcItem.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(aktif = new MyCheckboxConfig());
		aktif.setChecked(ddcItem.getDefaultItem() != null && ddcItem.getDefaultItem());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(ddcItem.getKeterangan() == null ? "" : ddcItem.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
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

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Ddc Item harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		// if (versiDdcItem.getSelectedItem() == null) {
		// MyMessageboxConfig.show("Versi DDC harus diisi", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		// boolean i = checkNamaDdcItem();
		// if (i) {
		// MyMessageboxConfig.show("Nama Ddc Item sudah ada di database",
		// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		DdcItemDao ddcItemDao = DaoFactory.getInstance().getDdcItemDao();
		if (ddcItem.getId() != null) {
			ddcItem = ddcItemDao.load(ddcItem.getId());

		}

		ddcItem.setVersiDdcItem((VersiDdcItem) (versiDdcItem.getSelectedItem() == null ? null
				: versiDdcItem.getSelectedItem().getValue()));
		ddcItem.setParent((DdcItem) parent.getAttribute("ddcItem"));
		ddcItem.setDefaultItem(aktif.isChecked());
		ddcItem.setKode(kode.getValue());
		ddcItem.setNama(nama.getValue());
		ddcItem.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(ddcItem);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Integer count = ((Number) HibernateUtil.currentSession().createCriteria(DataDdcItem.class)
						.add(Restrictions.eq("ddcItem", ddcItem)).setProjection(Projections.rowCount()).uniqueResult())
								.intValue();
				if (count.equals(0)) {
					DataDdcItem dataDdcItem = new DataDdcItem();
					dataDdcItem.setDdcItem(ddcItem);
					dataDdcItem.setKeterangan(ddcItem.getKode());
					Common.refreshSaveOrUpdate(dataDdcItem);
				}
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DdcItem.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE))
				.add(searchversi.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("versiDdcItem", searchversi.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DdcItem> ddcItem = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(ddcItem);
		grid.setRowRenderer(new DdcItemRenderer());
		grid.setModelCheckMobile(strset);

	}

	// public Boolean checkNamaDdcItem() {
	//
	// Integer kotaCount = null;
	// Session session = HibernateUtil.currentSession();
	// kotaCount = ((Number) session
	// .createCriteria(DdcItem.class)
	// .setProjection(Projections.rowCount())
	// .add(Restrictions.eq("nama", nama.getValue().trim()))
	// .add(this.ddcItem.getId() == null ? Restrictions
	// .sqlRestriction("1=1") : Restrictions.ne("id",
	// this.ddcItem.getId())).uniqueResult()).intValue();
	//
	// return !kotaCount.equals(0);
	// }

}
