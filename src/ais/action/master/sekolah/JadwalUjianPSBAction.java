package ais.action.master.sekolah;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
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
import ais.action.master.sekolah.helper.AktifitasJadwalUjianPSBHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.JadwalUjianPSB;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.UjianPSB;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JadwalUjianPSBAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchujian;
	private Combobox searchgelombangPendaftaranPsb;

	private Combobox ujianPSB;
	private Combobox gelombangPendaftaranPsb;

	private MyDatebox waktuMulai;
	private MyDatebox waktuSampai;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JadwalUjianPSB jadwalUjianPSB;
	private MyToolbarbuttonConfig add;

	protected AktifitasJadwalUjianPSBHelper aktifitasJadwalUjianPSBHelper = new AktifitasJadwalUjianPSBHelper();

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

		Common.insertCombo(searchgelombangPendaftaranPsb, "nama", GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (!searchgelombangPendaftaranPsb.getChildren().isEmpty()) {
			searchgelombangPendaftaranPsb.setSelectedIndex(0);
		}
		if (searchgelombangPendaftaranPsb != null) { searchgelombangPendaftaranPsb.setReadonly(true); }

		if (execution.getParameter("gelombangPendaftaranPsb") != null) {
			GelombangPendaftaranPsb gel = (GelombangPendaftaranPsb) HibernateUtil.currentSession()
					.createCriteria(GelombangPendaftaranPsb.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("gelombangPendaftaranPsb"))))
					.uniqueResult();
			if (gel != null) {
				Common.selectComboItem(true, searchgelombangPendaftaranPsb, gel);
				searchgelombangPendaftaranPsb.setDisabled(true);
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
	}

	class JadwalUjianPSBRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JadwalUjianPSB jadwalUjianPSB = (JadwalUjianPSB) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (detail.getChildren().size() == 0) {
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						aktifitasJadwalUjianPSBHelper.initDetail(jadwalUjianPSB, groupbox);
						detail.appendChild(groupbox);
					}
				}
			});

			RevisiHelper.createNewRevisi(JadwalUjianPSB.class, jadwalUjianPSB, jadwalUjianPSB.getNama())
					.setParent(arg0);
			new Label(Common.dateFormat3.get().format(jadwalUjianPSB.getWaktuMulai())).setParent(arg0);
			new Label(Common.dateFormat3.get().format(jadwalUjianPSB.getWaktuSampai())).setParent(arg0);
			new Label(jadwalUjianPSB.getUjianPSB().toString()).setParent(arg0);
			new Label(jadwalUjianPSB.getGelombangPendaftaranPsb() == null ? "Semua"
					: jadwalUjianPSB.getGelombangPendaftaranPsb().getNama()).setParent(arg0);
			new Label(jadwalUjianPSB.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, jadwalUjianPSB, JadwalUjianPSBAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JadwalUjianPSB());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jadwalUjianPSB = (JadwalUjianPSB) obj;
		init(jadwalUjianPSB);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JadwalUjianPSB jadwalUjianPSB) {
		this.jadwalUjianPSB = jadwalUjianPSB;
		addWindow.setTitle(jadwalUjianPSB.getId() == null ? "Tambah Jadwal Ujian" : "Ubah Jadwal Ujian");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Materi Ujian"));
		row.appendChild(nama = new Textbox(jadwalUjianPSB.getNama() == null ? "" : jadwalUjianPSB.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Mulai"));
		row.appendChild(waktuMulai = new MyDatebox(jadwalUjianPSB.getWaktuMulai()));
		waktuMulai.setFormat(Common.dateFormat.get().toPattern());

		waktuMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Sampai"));
		row.appendChild(waktuSampai = new MyDatebox(jadwalUjianPSB.getWaktuSampai()));
		waktuSampai.setFormat(Common.dateFormat.get().toPattern());

		waktuSampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ujian"));
		row.appendChild(ujianPSB = new Combobox());
		Common.insertCombo(ujianPSB, "nama", UjianPSB.class);
		Common.selectComboItem(ujianPSB, jadwalUjianPSB.getUjianPSB());
		ujianPSB.setWidth("90%");
		ujianPSB.setReadonly(true);

		Sekolah sekolah = SekolahUtil.getSekolah();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran"));
		row.appendChild(gelombangPendaftaranPsb = new Combobox());
		Common.insertCombo(gelombangPendaftaranPsb, "nama", GelombangPendaftaranPsb.class,
				Restrictions.and(
						sekolah == null ? Restrictions.eq("sekolah", sekolah) : Restrictions.sqlRestriction("true"),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		Common.selectComboItem(gelombangPendaftaranPsb, jadwalUjianPSB.getGelombangPendaftaranPsb());
		gelombangPendaftaranPsb.setWidth("90%");

		if (searchgelombangPendaftaranPsb.getSelectedItem() != null
				&& searchgelombangPendaftaranPsb.getSelectedItem().getValue() != null) {
			Common.selectComboItem(true, gelombangPendaftaranPsb,
					searchgelombangPendaftaranPsb.getSelectedItem().getValue());
			gelombangPendaftaranPsb.setDisabled(searchgelombangPendaftaranPsb.isDisabled());

			Common.insertCombo(ujianPSB, "nama", UjianPSB.class, Restrictions.eq("gelombangPendaftaranPsb",
					searchgelombangPendaftaranPsb.getSelectedItem().getValue()));
			Common.selectComboItem(ujianPSB, jadwalUjianPSB.getUjianPSB());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(jadwalUjianPSB.getKeterangan() == null ? "" : jadwalUjianPSB.getKeterangan()));
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
			MyMessageboxConfig.show("Materi ujian harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (waktuMulai.getValue() == null) {
			MyMessageboxConfig.show("Waktu mulai ujian harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (waktuSampai.getValue() == null) {
			MyMessageboxConfig.show("Waktu sampai ujian harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (ujianPSB.getSelectedItem() == null) {
			MyMessageboxConfig.show("Data ujian harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jadwalUjianPSB.getId() != null) {
			jadwalUjianPSB = (JadwalUjianPSB) session.load(JadwalUjianPSB.class, jadwalUjianPSB.getId());

		}

		jadwalUjianPSB.setWaktuMulai(waktuMulai.getValue());
		jadwalUjianPSB.setWaktuSampai(waktuSampai.getValue());
		jadwalUjianPSB.setUjianPSB((UjianPSB) ujianPSB.getSelectedItem().getValue());
		jadwalUjianPSB.setNama(nama.getValue());
		jadwalUjianPSB.setKeterangan(keterangan.getValue());
		jadwalUjianPSB.setGelombangPendaftaranPsb(
				(GelombangPendaftaranPsb) (gelombangPendaftaranPsb.getSelectedItem() == null ? null
						: gelombangPendaftaranPsb.getSelectedItem().getValue()));

		if (jadwalUjianPSB.getId() != null) {
			session.update(jadwalUjianPSB);
		} else {
			session.save(jadwalUjianPSB);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JadwalUjianPSB.class).createAlias("ujianPSB", "ujianPSB");

		if (order)
			criteria.addOrder(Order.asc("waktuMulai"));
		criteria.add(searchujian.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
				: Restrictions.eq("ujianPSB", searchujian.getSelectedItem().getValue()))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchgelombangPendaftaranPsb.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gelombangPendaftaranPsb",
								searchgelombangPendaftaranPsb.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JadwalUjianPSB> jadwalUjianPSB = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jadwalUjianPSB);
		grid.setRowRenderer(new JadwalUjianPSBRenderer());
		grid.setModelCheckMobile(strset);

	}

}
