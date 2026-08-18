package ais.action.master.employ;

import ais.action.master.pelanggaran.DasbordPelanggaran;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.employ.HukumanPegawai;
import ais.database.model.employ.PelanggaranDanHukumanPegawai;
import ais.database.model.employ.PelanggaranPegawai;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PelanggaranDanHukumanPegawaiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Tabpanel tabDasbor;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PelanggaranDanHukumanPegawai pelanggaranDanHukumanPegawai;
	private MyToolbarbuttonConfig add;
	private Set<PelanggaranPegawai> selectedPelanggaranPegawai;
	private Set<HukumanPegawai> selectedHukumanPegawai;

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

		String[] contents = new String[] { "id", "nama", "sekolah", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PelanggaranDanHukumanPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
		onDasbor(null);
	}

	public void onDasbor(org.zkoss.zk.ui.event.Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordPelanggaran dasbord = new DasbordPelanggaran(DasbordPelanggaran.Lingkup.SEMUA);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Pelanggaran & Sanksi Pegawai",
				"Rekap pelanggaran dan sanksi yang diberikan kepada pegawai.");
		}
	}

	class PelanggaranDanHukumanPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PelanggaranDanHukumanPegawai pelanggaranDanHukumanPegawai = (PelanggaranDanHukumanPegawai) arg1;

			RevisiHelper.createNewRevisi(PelanggaranDanHukumanPegawai.class, pelanggaranDanHukumanPegawai,
					pelanggaranDanHukumanPegawai.getNama()).setParent(arg0);

			new Label(pelanggaranDanHukumanPegawai.getKeterangan()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (PelanggaranPegawai pelanggaranPegawai : new TreeSet<PelanggaranPegawai>(
					pelanggaranDanHukumanPegawai.getPelanggaranPegawais())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + pelanggaranPegawai.getNama()));
				i++;
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			i = 1;
			for (HukumanPegawai hukumanPegawai : new TreeSet<HukumanPegawai>(
					pelanggaranDanHukumanPegawai.getHukumanPegawais())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + hukumanPegawai.getNama()));
				i++;
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(pelanggaranDanHukumanPegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pelanggaranDanHukumanPegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pelanggaranDanHukumanPegawai);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, pelanggaranDanHukumanPegawai,
					PelanggaranDanHukumanPegawaiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PelanggaranDanHukumanPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pelanggaranDanHukumanPegawai = (PelanggaranDanHukumanPegawai) obj;
		init(pelanggaranDanHukumanPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(PelanggaranDanHukumanPegawai pelanggaranDanHukumanPegawai) {
		this.pelanggaranDanHukumanPegawai = pelanggaranDanHukumanPegawai;
		addWindow.setTitle(pelanggaranDanHukumanPegawai.getId() == null ? "Tambah Jenis Pelanggaran Dan Hukuman" : "Ubah Jenis Pelanggaran Dan Hukuman");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pelanggaran Dan Hukuman *"));
		row.appendChild(nama = new Textbox(pelanggaranDanHukumanPegawai.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pelanggaranDanHukumanPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Pelanggaran Pegawai"));

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		List<PelanggaranPegawai> pelanggaranPegawais = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(PelanggaranPegawai.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				PelanggaranPegawai.class);

		if (pelanggaranDanHukumanPegawai.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pelanggaranDanHukumanPegawai);
		}
		selectedPelanggaranPegawai = this.pelanggaranDanHukumanPegawai.getPelanggaranPegawais();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final PelanggaranPegawai pelanggaranPegawai : pelanggaranPegawais) {
			final Checkbox checkbox = new Checkbox(pelanggaranPegawai.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedPelanggaranPegawai.contains(pelanggaranPegawai));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedPelanggaranPegawai.add(pelanggaranPegawai);
					} else {
						selectedPelanggaranPegawai.remove(pelanggaranPegawai);
					}
				}
			});
		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		subGrid = new MyGrid();
		row.appendChild(subGrid);

		subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Hukuman Pegawai"));

		subRows = new Rows();
		subRows.setParent(subGrid);

		subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		List<HukumanPegawai> hukumanPegawais = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(HukumanPegawai.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				HukumanPegawai.class);

		if (pelanggaranDanHukumanPegawai.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pelanggaranDanHukumanPegawai);
		}
		selectedHukumanPegawai = this.pelanggaranDanHukumanPegawai.getHukumanPegawais();

		vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final HukumanPegawai hukumanPegawai : hukumanPegawais) {
			final Checkbox checkbox = new Checkbox(hukumanPegawai.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedHukumanPegawai.contains(hukumanPegawai));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedHukumanPegawai.add(hukumanPegawai);
					} else {
						selectedHukumanPegawai.remove(hukumanPegawai);
					}
				}
			});
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
			MyMessageboxConfig.show("Mohon maaf, Jenis Pelanggaran dan Hukuman Pegawai belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Jenis Pelanggaran Dan Hukuman pada form; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pelanggaranDanHukumanPegawai.getId() != null) {
			pelanggaranDanHukumanPegawai = (PelanggaranDanHukumanPegawai) session
					.load(PelanggaranDanHukumanPegawai.class, pelanggaranDanHukumanPegawai.getId());

		}

		pelanggaranDanHukumanPegawai.setNama(nama.getValue());
		pelanggaranDanHukumanPegawai.setKeterangan(keterangan.getValue());
		pelanggaranDanHukumanPegawai.setPelanggaranPegawais(selectedPelanggaranPegawai);
		pelanggaranDanHukumanPegawai.setHukumanPegawais(selectedHukumanPegawai);

		Common.refreshSaveOrUpdate(session, pelanggaranDanHukumanPegawai);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PelanggaranDanHukumanPegawai.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PelanggaranDanHukumanPegawai> pelanggaranDanHukumanPegawai = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pelanggaranDanHukumanPegawai);
		grid.setRowRenderer(new PelanggaranDanHukumanPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
