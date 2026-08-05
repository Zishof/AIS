package ais.action.master.employ;

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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.employ.TipePegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TipePegawaiAction extends GenericAutowireComposer
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

	private MyCheckboxConfig[] haris;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private TipePegawai tipePegawai;
	private MyToolbarbuttonConfig add;
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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "masukPresensi", "masukLembur", "minggu",
				"senin", "selasa", "rabu", "kamis", "jumat", "sabtu", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(TipePegawai.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, TipePegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class TipePegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TipePegawai tipePegawai = (TipePegawai) arg1;
			new Label(tipePegawai.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(TipePegawai.class, tipePegawai, tipePegawai.getNama()).setParent(arg0);
			new Label(tipePegawai.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig masukPresensi = new MyCheckboxConfig("Masuk Presensi");
			masukPresensi.setDisabled(!edit);
			masukPresensi.setChecked(tipePegawai.getMasukPresensi());
			masukPresensi.setParent(arg0);
			masukPresensi.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tipePegawai.setMasukPresensi(masukPresensi.isChecked());
					Common.refreshSaveOrUpdate(tipePegawai);
				}
			});

			final MyCheckboxConfig masukLembur = new MyCheckboxConfig("Masuk Lembur");
			masukLembur.setDisabled(!edit);
			masukLembur.setChecked(tipePegawai.getMasukLembur());
			masukLembur.setParent(arg0);
			masukLembur.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tipePegawai.setMasukLembur(masukLembur.isChecked());
					Common.refreshSaveOrUpdate(tipePegawai);
				}
			});

			final MyCheckboxConfig dapatKonsumsi = new MyCheckboxConfig("Dapat Konsumsi");
			dapatKonsumsi.setDisabled(!edit);
			dapatKonsumsi.setChecked(tipePegawai.getDapatKonsumsi());
			dapatKonsumsi.setParent(arg0);
			dapatKonsumsi.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tipePegawai.setDapatKonsumsi(dapatKonsumsi.isChecked());
					Common.refreshSaveOrUpdate(tipePegawai);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(tipePegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tipePegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(tipePegawai);
				}
			});

			if ((TipePegawai.GURU != null && tipePegawai.getId().equals(TipePegawai.GURU.getId()))
					|| (TipePegawai.DOSEN != null && tipePegawai.getId().equals(TipePegawai.DOSEN.getId()))
					|| (TipePegawai.STAF != null && tipePegawai.getId().equals(TipePegawai.STAF.getId()))) {
				new Label().setParent(arg0);
			} else {
				Common.copyEditDeleteButtons(edit, delete, tipePegawai, TipePegawaiAction.this).setParent(arg0);
			}

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TipePegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		tipePegawai = (TipePegawai) obj;
		init(tipePegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(TipePegawai tipePegawai) {
		this.tipePegawai = tipePegawai;
		addWindow.setTitle(tipePegawai.getId() == null ? "Tambah Tipe Pegawai" : "Ubah Tipe Pegawai");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Tipe Pegawai"));
		row.appendChild(kode = new Textbox(tipePegawai.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Tipe Pegawai"));
		row.appendChild(nama = new Textbox(tipePegawai.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(tipePegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari Aktif"));
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		haris = new MyCheckboxConfig[Common.haris.length];
		Integer hari = 1;
		for (String h : Common.haris) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(haris[hari - 1] = new MyCheckboxConfig(h));
			haris[hari - 1].setChecked(hari.equals(1) ? tipePegawai.getMinggu()
					: hari.equals(2) ? tipePegawai.getSenin()
							: hari.equals(3) ? tipePegawai.getSelasa()
									: hari.equals(4) ? tipePegawai.getRabu()
											: hari.equals(5) ? tipePegawai.getKamis()
													: hari.equals(6) ? tipePegawai.getJumat()
															: hari.equals(7) ? tipePegawai.getSabtu() : false);
			haris[hari - 1].setValue(h);
			haris[hari - 1].setAttribute("hari", hari);
			hari++;
		}

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
			MyMessageboxConfig.show("Mohon maaf, Nama Tipe Pegawai belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Tipe Pegawai pada form; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkKodeTipePegawai();
		if (i) {
			MyMessageboxConfig.show("kode Tipe Pegawai sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		i = checkNamaTipePegawai();
		if (i) {
			MyMessageboxConfig.show("Nama Tipe Pegawai sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (tipePegawai.getId() != null) {
			tipePegawai = (TipePegawai) session.load(TipePegawai.class, tipePegawai.getId());
		}

		tipePegawai.setKode(kode.getValue());
		tipePegawai.setNama(nama.getValue());
		tipePegawai.setKeterangan(keterangan.getValue());

		for (MyCheckboxConfig checkbox : haris) {
			Integer hari = (Integer) checkbox.getAttribute("hari");

			if (hari.equals(1)) {
				tipePegawai.setMinggu(checkbox.isChecked());
			} else if (hari.equals(2)) {
				tipePegawai.setSenin(checkbox.isChecked());
			} else if (hari.equals(3)) {
				tipePegawai.setSelasa(checkbox.isChecked());
			} else if (hari.equals(4)) {
				tipePegawai.setRabu(checkbox.isChecked());
			} else if (hari.equals(5)) {
				tipePegawai.setKamis(checkbox.isChecked());
			} else if (hari.equals(6)) {
				tipePegawai.setJumat(checkbox.isChecked());
			} else if (hari.equals(7)) {
				tipePegawai.setSabtu(checkbox.isChecked());
			}

		}

		Common.refreshSaveOrUpdate(session, tipePegawai);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TipePegawai.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TipePegawai> tipePegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(tipePegawai);
		grid.setRowRenderer(new TipePegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKodeTipePegawai() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(TipePegawai.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.tipePegawai.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.tipePegawai.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkNamaTipePegawai() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(TipePegawai.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.tipePegawai.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.tipePegawai.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
