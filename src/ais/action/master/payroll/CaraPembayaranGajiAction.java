package ais.action.master.payroll;

import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
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

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.payroll.CaraPembayaranGaji;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class CaraPembayaranGajiAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox serachnama;
	private Textbox serachkode;
	private Checkbox searchaktif;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private Textbox nama;
	private Textbox kode;
	private Textbox deskripsi;
	private AmbilDataAkunBanbox akun;

	public CaraPembayaranGaji caraPembayaranGaji;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		}

		// add.setDisabled((Common.getCurrentUser().getRoot() == null ||
		// !Common.getCurrentUser().getRoot()));

		if (add != null) { add.setTooltiptext("Tambah"); }
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class CaraPembayaranGajiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CaraPembayaranGaji caraPembayaranGaji = (CaraPembayaranGaji) arg1;

			RevisiHelper
					.createNewRevisi(CaraPembayaranGaji.class, caraPembayaranGaji,
							caraPembayaranGaji.getKode() == null ? "" : caraPembayaranGaji.getKode().trim().toString())
					.setParent(arg0);
			new Label(caraPembayaranGaji.getNama()).setParent(arg0);
			new Label(caraPembayaranGaji.getDeskripsi()).setParent(arg0);

			new Label(caraPembayaranGaji.getAkun() == null ? ""
					: caraPembayaranGaji.getAkun().getKode() + "-" + caraPembayaranGaji.getAkun().getNama())
					.setParent(arg0);

			new Label(caraPembayaranGaji.getSatuanKerja() == null ? ""
					: caraPembayaranGaji.getSatuanKerja().getKode() + "-"
							+ caraPembayaranGaji.getSatuanKerja().getNama())
					.setParent(arg0);

			final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
			aktif.setChecked(caraPembayaranGaji.getAktif());
			aktif.setParent(arg0);
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					caraPembayaranGaji.setAktif(aktif.isChecked());
					Common.refreshSaveOrUpdate(caraPembayaranGaji);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Default");
			checkbox.setChecked(caraPembayaranGaji.getDefaultPembayaran());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					caraPembayaranGaji.setDefaultPembayaran(checkbox.isChecked());
					Common.refreshSaveOrUpdate(caraPembayaranGaji);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, caraPembayaranGaji, CaraPembayaranGajiAction.this)
					.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new CaraPembayaranGaji());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(CaraPembayaranGaji caraPembayaranGaji) throws Exception {
		addWindow.setTitle(caraPembayaranGaji.getId() == null ? "Tambah Cara Pembayaran" : "Ubah Cara Pembayaran");
		this.caraPembayaranGaji = caraPembayaranGaji;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode *"));
		row.appendChild(
				kode = new Textbox((caraPembayaranGaji.getKode() == null ? "" : caraPembayaranGaji.getKode().trim())));
		kode.setWidth("90%");
		// kode.setDisabled(Common.getCurrentUser().getRoot() == null ||
		// !Common.getCurrentUser().getRoot());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama *"));
		row.appendChild(
				nama = new Textbox(caraPembayaranGaji.getNama() == null ? "" : caraPembayaranGaji.getNama().trim()));
		nama.setWidth("90%");
		// nama.setDisabled(Common.getCurrentUser().getRoot() == null ||
		// !Common.getCurrentUser().getRoot());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi"));
		row.appendChild(deskripsi = new Textbox(
				caraPembayaranGaji.getDeskripsi() == null ? "" : caraPembayaranGaji.getDeskripsi()));
		deskripsi.setWidth("90%");
		deskripsi.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun"));
		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(caraPembayaranGaji.getAkun() == null ? "" : caraPembayaranGaji.getAkun().getNama());
		akun.setAttribute("akun", caraPembayaranGaji.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setValue(caraPembayaranGaji.getSatuanKerja() == null ? ""
				: caraPembayaranGaji.getSatuanKerja().getKode() + "-" + caraPembayaranGaji.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", caraPembayaranGaji.getSatuanKerja());
		satuanKerja.setWidth("90%");

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

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, kolom Nama belum diisi. Langkah yang dapat dilakukan: (1) isikan Nama pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkKode();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Kode Item yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) gunakan Kode Item yang berbeda; (2) periksa kembali daftar cara pembayaran gaji yang telah ada; (3) simpan kembali data ini.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		i = checkNama();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Item yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) gunakan Nama Item yang berbeda; (2) periksa kembali daftar cara pembayaran gaji yang telah ada; (3) simpan kembali data ini.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (caraPembayaranGaji.getId() != null) {
			caraPembayaranGaji = (CaraPembayaranGaji) session.load(CaraPembayaranGaji.class,
					caraPembayaranGaji.getId());
		}
		caraPembayaranGaji.setAkun((Akun) akun.getAttribute("akun"));
		caraPembayaranGaji.setKode(kode.getValue());
		caraPembayaranGaji.setNama(nama.getValue());
		caraPembayaranGaji.setDeskripsi(deskripsi.getValue());

		caraPembayaranGaji.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		Common.refreshSaveOrUpdate(session, caraPembayaranGaji);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CaraPembayaranGaji.class)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.asc("kode"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CaraPembayaranGaji> caraPembayaranGaji = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(caraPembayaranGaji);
		grid.setRowRenderer(new CaraPembayaranGajiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKode() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(CaraPembayaranGaji.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.caraPembayaranGaji.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.caraPembayaranGaji.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkNama() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(CaraPembayaranGaji.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.caraPembayaranGaji.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.caraPembayaranGaji.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		CaraPembayaranGaji caraPembayaranGaji = (CaraPembayaranGaji) obj;
		init(caraPembayaranGaji);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
