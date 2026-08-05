package ais.action.master.sop;

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
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
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
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sop.KelompokParameterTambahanAlurSop;

public class KelompokParameterTambahanAlurSopAction extends GenericAutowireComposer {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelompokParameterTambahanAlurSop kelompokParameterTambahanAlurSop;
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

	class KelompokParameterTambahanAlurSopRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokParameterTambahanAlurSop kelompokParameterTambahanAlurSop = (KelompokParameterTambahanAlurSop) arg1;

			RevisiHelper
					.createNewRevisi(KelompokParameterTambahanAlurSop.class,
							kelompokParameterTambahanAlurSop, kelompokParameterTambahanAlurSop.getNama())
					.setParent(arg0);
			new Label(kelompokParameterTambahanAlurSop.getKeterangan()).setParent(arg0);



			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");checkbox.setDisabled(!edit);
			checkbox.setChecked(kelompokParameterTambahanAlurSop.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokParameterTambahanAlurSop.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kelompokParameterTambahanAlurSop);
				}
			});

			final Intbox intbox = new Intbox(kelompokParameterTambahanAlurSop.getNomorUrut());
			intbox.setWidth("90%");
			intbox.setParent(arg0);
			intbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kelompokParameterTambahanAlurSop.setNomorUrut(intbox.getValue());
					Common.refreshSaveOrUpdate(kelompokParameterTambahanAlurSop);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelompokParameterTambahanAlurSop);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && !kelompokParameterTambahanAlurSop.getDefaultData());
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(kelompokParameterTambahanAlurSop);
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

	public void onAdd(Event event) throws Exception {
		init(new KelompokParameterTambahanAlurSop());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokParameterTambahanAlurSop kelompokParameterTambahanAlurSop) {
		this.kelompokParameterTambahanAlurSop = kelompokParameterTambahanAlurSop;
		addWindow.setTitle(kelompokParameterTambahanAlurSop.getId() == null ? "Tambah Kelompok" : "Ubah Kelompok");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok"));
		row.appendChild(nama = new Textbox(kelompokParameterTambahanAlurSop.getNama() == null ? ""
				: kelompokParameterTambahanAlurSop.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelompokParameterTambahanAlurSop.getKeterangan() == null ? ""
				: kelompokParameterTambahanAlurSop.getKeterangan()));
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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Kelompok Parameter belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Kelompok; (2) isikan nama kelompok parameter yang sesuai; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaKelompokParameterTambahanAlurSop();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Kelompok tersebut sudah terdaftar di database. Langkah yang dapat dilakukan: (1) periksa kembali nama kelompok yang diinputkan; (2) gunakan nama yang berbeda dan belum terdaftar; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokParameterTambahanAlurSop.getId() != null) {
			kelompokParameterTambahanAlurSop = (KelompokParameterTambahanAlurSop) session.load(
					KelompokParameterTambahanAlurSop.class, kelompokParameterTambahanAlurSop.getId());

		}

		kelompokParameterTambahanAlurSop.setNama(nama.getValue());
		kelompokParameterTambahanAlurSop.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, kelompokParameterTambahanAlurSop);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelompokParameterTambahanAlurSop.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokParameterTambahanAlurSop> kelompokParameterTambahanAlurSop = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokParameterTambahanAlurSop);
		grid.setRowRenderer(new KelompokParameterTambahanAlurSopRenderer());
		grid.setModelCheckMobile(strset);



	}

	public Boolean checkNamaKelompokParameterTambahanAlurSop() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokParameterTambahanAlurSop.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kelompokParameterTambahanAlurSop.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokParameterTambahanAlurSop.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
