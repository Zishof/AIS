package ais.action.master.employ;

import java.util.ArrayList;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Intbox;
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
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.JenisJabatanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisJabatanAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Checkbox searchaktif;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisJabatan jenisJabatan;
	private MyToolbarbuttonConfig add;
	private Combobox grup;
	private Textbox jenisPengguna;
	private Textbox usernamePengguna;
	private MyCheckboxConfig hanyaBoleh;
	private Textbox kode;

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

		String[] contents = new String[] { "id", "key", "kode", "nama", "jenisPengguna", "usernamePengguna",
				"nomorUrut", "grup", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisJabatan.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisJabatan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class JenisJabatanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisJabatan jenisJabatan = (JenisJabatan) arg1;
			new Label(jenisJabatan.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(JenisJabatan.class, jenisJabatan, jenisJabatan.getNama()).setParent(arg0);
			new Label(jenisJabatan.getGrup()).setParent(arg0);

			new Label(jenisJabatan.getJenisPengguna() == null ? "Semua" : jenisJabatan.getJenisPengguna())
					.setParent(arg0);
			new Label(jenisJabatan.getUsernamePengguna() == null ? "Semua" : jenisJabatan.getUsernamePengguna())
					.setParent(arg0);

			final Intbox intbox = new Intbox(jenisJabatan.getNomorUrut());
			intbox.setWidth("90%");
			intbox.setParent(arg0);
			intbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisJabatan.setNomorUrut(intbox.getValue());
					Common.refreshSaveOrUpdate(jenisJabatan);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisJabatan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisJabatan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisJabatan);
				}
			});

			new Label(jenisJabatan.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, jenisJabatan, JenisJabatanAction.this).setParent(arg0);

		}

	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisJabatan = (JenisJabatan) obj;
		init(jenisJabatan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new JenisJabatan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	private void init(JenisJabatan jenisJabatan) {
		this.jenisJabatan = jenisJabatan;
		addWindow.setTitle(jenisJabatan.getId() == null ? "Tambah Jenis Jabatan" : "Ubah Jenis Jabatan");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Jabatan"));
		row.appendChild(kode = new Textbox(jenisJabatan.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Jabatan *"));
		row.appendChild(nama = new Textbox(jenisJabatan.getNama() == null ? "" : jenisJabatan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Jabatan"));
		row.appendChild(grup = new Combobox());
		grup.setWidth("90%");
		grup.setReadonly(false);

		List<String> grups = HibernateUtil.currentSession().createCriteria(JenisJabatan.class)
				.setProjection(Projections.groupProperty("grup")).add(Restrictions.isNotNull("grup"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();
		if (grups.isEmpty()) {
			grups.add("Pejabat");
		}
		for (String s : grups) {
			Comboitem comboitem = new Comboitem(s);
			comboitem.setValue(s);
			grup.appendChild(comboitem);
		}

		Common.selectComboItem(grup, jenisJabatan.getGrup());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaBoleh = new MyCheckboxConfig(
				"Disposisi oleh jenis pengguna (id role), jika tidak dipilih oleh id penguna (username)"));
		hanyaBoleh.setChecked(jenisJabatan.getJenisPengguna() != null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hanya boleh di disposisi oleh jenis pengguna"));
		row.appendChild(jenisPengguna = new Textbox(jenisJabatan.getJenisPengguna()));
		jenisPengguna.setWidth("90%");
		jenisPengguna.setRows(2);

		final Row s = Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua jenis pengguna");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan oleh username pengguna"));
		row.appendChild(usernamePengguna = new Textbox(jenisJabatan.getUsernamePengguna()));
		usernamePengguna.setWidth("90%");
		usernamePengguna.setRows(2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Pengguna",
				"/img/user_male_add.png");

		final MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								usernamePengguna.setValue(usernamePengguna.getValue()
										+ (usernamePengguna.getValue().isEmpty() ? tbmuser.getUserId()
												: "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		final Row a = Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua username pengguna");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jenisPengguna.getParent().setVisible(hanyaBoleh.isChecked());
				usernamePengguna.getParent().setVisible(!hanyaBoleh.isChecked());
				rowAmbilPengguna.setVisible(!hanyaBoleh.isChecked());
				a.setVisible(!hanyaBoleh.isChecked());
				s.setVisible(hanyaBoleh.isChecked());
			}

		};
		hanyaBoleh.addEventListener("onClick", eventListener);
		try {
			eventListener.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(jenisJabatan.getKeterangan() == null ? "" : jenisJabatan.getKeterangan()));
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
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Jabatan belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Jenis Jabatan pada form; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		JenisJabatanDao jenisJabatanDao = DaoFactory.getInstance().getJenisJabatanDao();
		if (jenisJabatan.getId() != null) {
			jenisJabatan = jenisJabatanDao.load(jenisJabatan.getId());
		}

		jenisJabatan.setKode(kode.getValue().trim());
		jenisJabatan.setNama(nama.getValue());
		jenisJabatan.setKeterangan(keterangan.getValue());
		jenisJabatan.setGrup(grup.getValue());
		jenisJabatan.setJenisPengguna(hanyaBoleh.isChecked() ? jenisPengguna.getValue() : null);
		jenisJabatan.setUsernamePengguna(hanyaBoleh.isChecked() ? null : usernamePengguna.getValue());

		if (jenisJabatan.getId() != null) {
			jenisJabatanDao.update(jenisJabatan);
		} else {
			jenisJabatanDao.save(jenisJabatan);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisJabatan.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisJabatan> jenisJabatan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisJabatan);
		grid.setRowRenderer(new JenisJabatanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
