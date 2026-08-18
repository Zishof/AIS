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
import ais.ui.util.MyDetail;
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

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.StatusKerjasamaMahasiswaHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.StatusKerjasamaMahasiswaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatStatusKerjasamaMahasiswa;
import ais.database.model.StatusKerjasamaMahasiswa;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class StatusKerjasamaMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Textbox searchketerangan;
	private AmbilDataMahasiswaBanbox searchmahasiswa;

	private Textbox nama;
	private MyDatebox date;
	private Textbox instansi;
	private Textbox keterangan;

	private StatusKerjasamaMahasiswa statusKerjasamaMahasiswa;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;

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

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		add.setVisible(CommonPrivilages
				.checkPrevilages(CommonPrivilages.CREATE));
		if (add != null) { add.setTooltiptext("Tambah"); }

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class StatusKerjasamaMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private StatusKerjasamaMahasiswaHelper statusKerjasamaMahasiswaHelper = new StatusKerjasamaMahasiswaHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final StatusKerjasamaMahasiswa statusKerjasamaMahasiswa = (StatusKerjasamaMahasiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen())
						statusKerjasamaMahasiswaHelper
								.displayPrasyaratStatusKerjasamaMahasiswa(
										statusKerjasamaMahasiswa, detail,
										addWindow);
				}

			});

			RevisiHelper.createNewRevisi(StatusKerjasamaMahasiswa.class,
					statusKerjasamaMahasiswa,
					statusKerjasamaMahasiswa.getNama()).setParent(arg0);

			new Label(statusKerjasamaMahasiswa.getInstansi()).setParent(arg0);
			new Label(statusKerjasamaMahasiswa.getKeterangan()).setParent(arg0);
			new Label(statusKerjasamaMahasiswa.getDate() == null ? ""
					: Common.dateFormat.get().format(statusKerjasamaMahasiswa
							.getDate())).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(statusKerjasamaMahasiswa);
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
											Common.refreshDelete(statusKerjasamaMahasiswa);
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
		init(new StatusKerjasamaMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(StatusKerjasamaMahasiswa statusKerjasamaMahasiswa) {
		this.statusKerjasamaMahasiswa = statusKerjasamaMahasiswa;
		Common.clear(addWindow);
		addWindow.setTitle("StatusKerjasamaMahasiswa");
		addWindow.setWidth("550px");
		addWindow.setHeight("500px");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Status Kerjasama Mahasiswa"));
		row.appendChild(nama = new Textbox(
				statusKerjasamaMahasiswa.getNama() == null ? ""
						: statusKerjasamaMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Instansi"));
		row.appendChild(instansi = new Textbox(statusKerjasamaMahasiswa
				.getInstansi() == null ? "" : statusKerjasamaMahasiswa
				.getInstansi()));
		instansi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ketarangan"));
		row.appendChild(keterangan = new Textbox(statusKerjasamaMahasiswa
				.getKeterangan() == null ? "" : statusKerjasamaMahasiswa
				.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(date = new MyDatebox(
				statusKerjasamaMahasiswa.getDate() == null ? ais.ui.util.WaktuUtil.getDate()
						: statusKerjasamaMahasiswa.getDate()));

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
					// loadKurikulum();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		
	}

	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		StatusKerjasamaMahasiswaDao statusKerjasamaMahasiswaDao = DaoFactory
				.getInstance().getStatusKerjasamaMahasiswaDao();
		if (statusKerjasamaMahasiswa.getId() != null) {
			statusKerjasamaMahasiswa = statusKerjasamaMahasiswaDao
					.load(statusKerjasamaMahasiswa.getId());
		}

		statusKerjasamaMahasiswa.setNama(nama.getValue());
		statusKerjasamaMahasiswa.setDate(date.getValue());
		statusKerjasamaMahasiswa.setInstansi(instansi.getValue());
		statusKerjasamaMahasiswa.setKeterangan(keterangan.getValue());

		// statusKerjasamaMahasiswaDao.beginTransaction();
		if (statusKerjasamaMahasiswa.getId() != null) {
			statusKerjasamaMahasiswaDao.update(statusKerjasamaMahasiswa);
		} else {
			statusKerjasamaMahasiswaDao.save(statusKerjasamaMahasiswa);
		}
		// statusKerjasamaMahasiswaDao.commitTransaction();
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa
				.getAttribute("mahasiswa");
		Session session = HibernateUtil.currentSession();
		if (mahasiswa != null) {
			Criteria criteria = session
					.createCriteria(
							MahasiswaDapatStatusKerjasamaMahasiswa.class)
					.setProjection(
							Projections.property("statusKerjasamaMahasiswa"))
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.createCriteria("statusKerjasamaMahasiswa");
			if (order)
				criteria.addOrder(Order.desc("date"));
			criteria.add(
					Restrictions.ilike("nama", searchnama.getValue(),
							MatchMode.ANYWHERE)).add(
					Restrictions.ilike("keterangan",
							searchketerangan.getValue(), MatchMode.ANYWHERE));
			return criteria;
		} else {
			Criteria criteria = session
					.createCriteria(StatusKerjasamaMahasiswa.class);
			if (order)
				criteria.addOrder(Order.desc("date"));
			criteria.add(
					Restrictions.ilike("nama", searchnama.getValue(),
							MatchMode.ANYWHERE)).add(
					Restrictions.ilike("keterangan",
							searchketerangan.getValue(), MatchMode.ANYWHERE));
			return criteria;
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<StatusKerjasamaMahasiswa> statusKerjasamaMahasiswa = initCriteria(
				true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();

		ListModel strset = new SimpleListModel(statusKerjasamaMahasiswa);
		grid.setRowRenderer(new StatusKerjasamaMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

		

	}

}
