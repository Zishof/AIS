package ais.action.master;

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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Paket;
import ais.database.model.PersyaratanPilihanPaket;

public class PersyaratanPilihanPaketAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Paket paket;

	private Combobox persyaratan;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PersyaratanPilihanPaket persyaratanPilihanPaket;
	private MyToolbarbuttonConfig add;

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

		paket = (Paket) HibernateUtil.currentSession().createCriteria(Paket.class)
				.add(Restrictions.idEq(Long.parseLong(execution.getParameter("paket")))).uniqueResult();

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

	class PersyaratanPilihanPaketRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PersyaratanPilihanPaket persyaratanPilihanPaket = (PersyaratanPilihanPaket) arg1;

			RevisiHelper.createNewRevisi(PersyaratanPilihanPaket.class, persyaratanPilihanPaket,
					persyaratanPilihanPaket.getPaket().getNama()).setParent(arg0);
			new Label(persyaratanPilihanPaket.getPersyaratan().getNama()).setParent(arg0);
			new Label(persyaratanPilihanPaket.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(persyaratanPilihanPaket);
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
									Common.refreshDelete(HibernateUtil.currentSession(), persyaratanPilihanPaket);
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
		init(new PersyaratanPilihanPaket());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PersyaratanPilihanPaket persyaratanPilihanPaket) {
		this.persyaratanPilihanPaket = persyaratanPilihanPaket;
		addWindow.setTitle(persyaratanPilihanPaket.getId() == null ? "Tambah Persyaratan Pilihan Paket" : "Ubah Persyaratan Pilihan Paket");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket Prasyarat"));
		row.appendChild(persyaratan = new Combobox());
		Common.insertCombo(persyaratan, "nama", Paket.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.ne("id", paket.getId())));
		Common.selectComboItem(persyaratan, persyaratanPilihanPaket.getPersyaratan());
		persyaratan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				persyaratanPilihanPaket.getKeterangan() == null ? "" : persyaratanPilihanPaket.getKeterangan()));
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

	public boolean onSave(Event event) throws Exception {
		if (persyaratan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Paket Prasyarat harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaPersyaratanPilihanPaket();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Paket Prasyarat",
					"Paket Prasyarat sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan Paket Prasyarat yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (persyaratanPilihanPaket.getId() != null) {
			persyaratanPilihanPaket = (PersyaratanPilihanPaket) session.load(PersyaratanPilihanPaket.class,
					persyaratanPilihanPaket.getId());

		}

		persyaratanPilihanPaket.setPaket(paket);
		persyaratanPilihanPaket.setPersyaratan(
				(Paket) (persyaratan.getSelectedItem() == null ? null : persyaratan.getSelectedItem().getValue()));
		persyaratanPilihanPaket.setKeterangan(keterangan.getValue());

		if (persyaratanPilihanPaket.getId() != null) {
			session.update(persyaratanPilihanPaket);
		} else {
			session.save(persyaratanPilihanPaket);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PersyaratanPilihanPaket.class).add(Restrictions.eq("paket", paket))
				.createAlias("persyaratan", "persyaratan");

		if (order)
			criteria.addOrder(Order.asc("persyaratan.nama"));
		criteria.add(Restrictions.ilike("persyaratan.nama", searchnama.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PersyaratanPilihanPaket> persyaratanPilihanPaket = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(persyaratanPilihanPaket);
		grid.setRowRenderer(new PersyaratanPilihanPaketRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaPersyaratanPilihanPaket() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PersyaratanPilihanPaket.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("paket", paket))
				.add(Restrictions.eq("persyaratan", persyaratan.getSelectedItem().getValue()))
				.add(this.persyaratanPilihanPaket.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.persyaratanPilihanPaket.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
