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
import ais.database.dao.MahasiswaRequestTugasAkhirMintaPembimbingDao;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.MahasiswaRequestTugasAkhirMintaPembimbing;

public class MahasiswaRequestTugasAkhirMintaPembimbingAction extends GenericAutowireComposer {

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

	private MahasiswaRequestTugasAkhirMintaPembimbing mahasiswaRequestTugasAkhirMintaPembimbing;
	private MyToolbarbuttonConfig add;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
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
	}

	class MahasiswaRequestTugasAkhirMintaPembimbingRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final MahasiswaRequestTugasAkhirMintaPembimbing mahasiswaRequestTugasAkhirMintaPembimbing = (MahasiswaRequestTugasAkhirMintaPembimbing) arg1;

			RevisiHelper.createNewRevisi(MahasiswaRequestTugasAkhirMintaPembimbing.class, mahasiswaRequestTugasAkhirMintaPembimbing, mahasiswaRequestTugasAkhirMintaPembimbing.getNama())
					.setParent(arg0);
			new Label(mahasiswaRequestTugasAkhirMintaPembimbing.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(mahasiswaRequestTugasAkhirMintaPembimbing);
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
											Common.refreshDelete(mahasiswaRequestTugasAkhirMintaPembimbing);
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
		init(new MahasiswaRequestTugasAkhirMintaPembimbing());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(MahasiswaRequestTugasAkhirMintaPembimbing mahasiswaRequestTugasAkhirMintaPembimbing) {
		this.mahasiswaRequestTugasAkhirMintaPembimbing = mahasiswaRequestTugasAkhirMintaPembimbing;
		addWindow.setTitle(mahasiswaRequestTugasAkhirMintaPembimbing.getId() == null ? "Tambah MahasiswaRequestTugasAkhirMintaPembimbing" : "Ubah MahasiswaRequestTugasAkhirMintaPembimbing");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama MahasiswaRequestTugasAkhirMintaPembimbing"));
		row.appendChild(nama = new Textbox(mahasiswaRequestTugasAkhirMintaPembimbing.getNama() == null ? "" : mahasiswaRequestTugasAkhirMintaPembimbing
				.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				mahasiswaRequestTugasAkhirMintaPembimbing.getKeterangan() == null ? "" : mahasiswaRequestTugasAkhirMintaPembimbing.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data MahasiswaRequestTugasAkhirMintaPembimbing",
					"Kolom Nama MahasiswaRequestTugasAkhirMintaPembimbing belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama MahasiswaRequestTugasAkhirMintaPembimbing.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaMahasiswaRequestTugasAkhirMintaPembimbing();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data MahasiswaRequestTugasAkhirMintaPembimbing",
					"Nama MahasiswaRequestTugasAkhirMintaPembimbing sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama mahasiswarequesttugasakhirmintapembimbing yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		MahasiswaRequestTugasAkhirMintaPembimbingDao mahasiswaRequestTugasAkhirMintaPembimbingDao = DaoFactory.getInstance().getMahasiswaRequestTugasAkhirMintaPembimbingDao();
		if (mahasiswaRequestTugasAkhirMintaPembimbing.getId() != null) {
			mahasiswaRequestTugasAkhirMintaPembimbing = mahasiswaRequestTugasAkhirMintaPembimbingDao.load(mahasiswaRequestTugasAkhirMintaPembimbing.getId());

		}

		mahasiswaRequestTugasAkhirMintaPembimbing.setNama(nama.getValue());
		mahasiswaRequestTugasAkhirMintaPembimbing.setKeterangan(keterangan.getValue());

		if (mahasiswaRequestTugasAkhirMintaPembimbing.getId() != null) {
			mahasiswaRequestTugasAkhirMintaPembimbingDao.update(mahasiswaRequestTugasAkhirMintaPembimbing);
		} else {
			mahasiswaRequestTugasAkhirMintaPembimbingDao.save(mahasiswaRequestTugasAkhirMintaPembimbing);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MahasiswaRequestTugasAkhirMintaPembimbing.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(Restrictions.ilike("nama", searchnama.getValue(),
				MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<MahasiswaRequestTugasAkhirMintaPembimbing> mahasiswaRequestTugasAkhirMintaPembimbing = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(mahasiswaRequestTugasAkhirMintaPembimbing);
		grid.setRowRenderer(new MahasiswaRequestTugasAkhirMintaPembimbingRenderer());
		grid.setModelCheckMobile(strset);


	}

	public Boolean checkNamaMahasiswaRequestTugasAkhirMintaPembimbing() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session
				.createCriteria(MahasiswaRequestTugasAkhirMintaPembimbing.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.mahasiswaRequestTugasAkhirMintaPembimbing.getId() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ne("id",
						this.mahasiswaRequestTugasAkhirMintaPembimbing.getId())).uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
