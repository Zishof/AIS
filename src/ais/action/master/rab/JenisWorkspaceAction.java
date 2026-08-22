package ais.action.master.rab;

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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataJenisWorkspaceBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.JenisWorkspaceDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.JenisWorkspace;

public class JenisWorkspaceAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;
	private Textbox warna;
	private Textbox warnaText;

	private boolean edit = false;
	private boolean delete = false;

	private JenisWorkspace jenisWorkspace;
	private MyToolbarbuttonConfig add;
	private MyCheckboxConfig defaultItem;
	private AmbilDataJenisWorkspaceBanbox parent;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
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
	}

	class JenisWorkspaceRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisWorkspace jenisWorkspace = (JenisWorkspace) arg1;

			new Label(jenisWorkspace.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisWorkspace.class, jenisWorkspace, jenisWorkspace.getNama())
					.setParent(arg0);
			Label label = new Label(jenisWorkspace.getWarna());
			label.setStyle((jenisWorkspace.getWarna() != null ? "background-color:" + jenisWorkspace.getWarna() + ";"
					: "")
					+ (jenisWorkspace.getWarnaText() != null ? "color:" + jenisWorkspace.getWarnaText() + ";" : ""));
			label.setParent(arg0);

			new Label(jenisWorkspace.getParent() == null ? "" : jenisWorkspace.getParent().toString()).setParent(arg0);
			new Label(jenisWorkspace.getDefaultItem() != null && jenisWorkspace.getDefaultItem() ? "Aktif" : "Tidak")
					.setParent(arg0);
			new Label(jenisWorkspace.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jenisWorkspace);
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
											JenisWorkspaceDao jenisWorkspaceDao = DaoFactory.getInstance()
													.getJenisWorkspaceDao();
											// jenisWorkspaceDao.beginTransaction();
											jenisWorkspaceDao.delete((jenisWorkspace));
											// jenisWorkspaceDao.commitTransaction();
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
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
		init(new JenisWorkspace());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisWorkspace jenisWorkspace) throws Exception {
		this.jenisWorkspace = jenisWorkspace;
		addWindow.setTitle(jenisWorkspace.getId() == null ? "Tambah Jenis Item Perencanaaan" : "Ubah Jenis Item Perencanaaan");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("60%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(jenisWorkspace.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Item"));
		row.appendChild(nama = new Textbox(jenisWorkspace.getNama() == null ? "" : jenisWorkspace.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Warna Dasar"));
		row.appendChild(warna = new Textbox());
		if (jenisWorkspace.getWarna() != null) {
			warna.setValue(jenisWorkspace.getWarna());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Warna Text"));
		row.appendChild(warnaText = new Textbox());
		if (jenisWorkspace.getWarnaText() != null) {
			warnaText.setValue(jenisWorkspace.getWarnaText());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(jenisWorkspace.getKeterangan() == null ? "" : jenisWorkspace.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Item Parent"));
		row.appendChild(parent = new AmbilDataJenisWorkspaceBanbox(true));
		parent.setValue(jenisWorkspace.getParent() == null ? "" : jenisWorkspace.getParent().toString());
		parent.setAttribute("jenisWorkspace", jenisWorkspace.getParent());
		parent.setWidth("90%");
		parent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// JenisWorkspace jenisWorkspaceParent = (JenisWorkspace) parent
				// .getAttribute("jenisWorkspace");
				// System.out.println("jenisWorkspaceParent = "
				// + jenisWorkspaceParent);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(defaultItem = new MyCheckboxConfig());
		defaultItem.setChecked(jenisWorkspace.getDefaultItem() != null && jenisWorkspace.getDefaultItem());

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
			MyMessageboxConfig.show("Nama Jenis Item Perencanaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		// boolean i = checkNamaJenisWorkspace();
		// if (i) {
		// MyMessageboxConfig.show(
		// "Kode Jenis Item Perencanaan sudah ada di database",
		// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		JenisWorkspaceDao jenisWorkspaceDao = DaoFactory.getInstance().getJenisWorkspaceDao();
		if (jenisWorkspace.getId() != null) {
			jenisWorkspace = jenisWorkspaceDao.load(jenisWorkspace.getId());

		}

		JenisWorkspace jenisWorkspaceParent = (JenisWorkspace) parent.getAttribute("jenisWorkspace");
		// System.out.println("jenisWorkspaceParent = " + jenisWorkspaceParent);

		jenisWorkspace.setParent(jenisWorkspaceParent);
		jenisWorkspace.setDefaultItem(defaultItem.isChecked());
		jenisWorkspace.setKode(kode.getValue());
		jenisWorkspace.setNama(nama.getValue());
		jenisWorkspace.setKeterangan(keterangan.getValue());
		jenisWorkspace.setWarna(warna.getValue());
		jenisWorkspace.setWarnaText(warnaText.getValue());

		if (jenisWorkspace.getId() != null) {
			jenisWorkspaceDao.update(jenisWorkspace);
		} else {
			jenisWorkspaceDao.save(jenisWorkspace);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisWorkspace.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisWorkspace> jenisWorkspace = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisWorkspace);
		grid.setRowRenderer(new JenisWorkspaceRenderer());
		grid.setModelCheckMobile(strset);

	}

	// public Boolean checkNamaJenisWorkspace() {
	//
	// Integer kotaCount = null;
	// Session session = HibernateUtil.currentSession();
	// kotaCount = ((Number) session
	// .createCriteria(JenisWorkspace.class)
	// .setProjection(Projections.rowCount())
	// .add(Restrictions.eq("kode", kode.getValue().trim()))
	// .add(this.jenisWorkspace.getId() == null ? Restrictions
	// .sqlRestriction("1=1") : Restrictions.ne("id",
	// this.jenisWorkspace.getId())).uniqueResult())
	// .intValue();
	//
	// return !kotaCount.equals(0);
	// }

}
