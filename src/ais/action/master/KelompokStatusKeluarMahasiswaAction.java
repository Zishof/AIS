package ais.action.master;

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
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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

import ais.action.master.helper.KelompokStatusKeluarMahasiswaDetailAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.KelompokStatusKeluarMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusKeluar;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelompokStatusKeluarMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchMahasiswa;

	private Textbox nama;
	private Combobox statusKeluar;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelompokStatusKeluarMahasiswa kelompokStatusKeluarMahasiswa;
	private MyToolbarbuttonConfig add;
	private MyDatebox tanggalLulus;
	private Combobox tahunAkademik;
	private Combobox semester;
//	private Textbox noSk;
//	private MyDatebox tanggalSk;

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

		String[] contents = new String[] { "id", "nama", "statusKeluar", "tahunAkademik", "semester", "tanggalLulus",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KelompokStatusKeluarMahasiswa.class, this,
				contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokStatusKeluarMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KelompokStatusKeluarMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			KelompokStatusKeluarMahasiswa kelompokStatusKeluarMahasiswa = (KelompokStatusKeluarMahasiswa) arg1;

			(new KelompokStatusKeluarMahasiswaDetailAction(kelompokStatusKeluarMahasiswa)).setParent(arg0);

			RevisiHelper.createNewRevisi(KelompokStatusKeluarMahasiswa.class, kelompokStatusKeluarMahasiswa,
					kelompokStatusKeluarMahasiswa.getNama()).setParent(arg0);
			new Label(kelompokStatusKeluarMahasiswa.getStatusKeluar() == null ? ""
					: kelompokStatusKeluarMahasiswa.getStatusKeluar().getNama()).setParent(arg0);

			new Label(kelompokStatusKeluarMahasiswa.getTahunAkademik()).setParent(arg0);
			new Label(kelompokStatusKeluarMahasiswa.getSemester()).setParent(arg0);

			new Label(kelompokStatusKeluarMahasiswa.getTanggalLulus() == null ? "Tidak Ditentukan"
					: Common.dateFormat4.get().format(kelompokStatusKeluarMahasiswa.getTanggalLulus())).setParent(arg0);

//			new Label(kelompokStatusKeluarMahasiswa.getNoSk()).setParent(arg0);
//			new Label(kelompokStatusKeluarMahasiswa.getTanggalSk() == null ? "Tidak Ditentukan"
//					: Common.dateFormat4.get().format(kelompokStatusKeluarMahasiswa.getTanggalSk())).setParent(arg0);

			new Label(kelompokStatusKeluarMahasiswa.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, kelompokStatusKeluarMahasiswa,
					KelompokStatusKeluarMahasiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KelompokStatusKeluarMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kelompokStatusKeluarMahasiswa = (KelompokStatusKeluarMahasiswa) obj;
		init(kelompokStatusKeluarMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokStatusKeluarMahasiswa kelompokStatusKeluarMahasiswa) {
		this.kelompokStatusKeluarMahasiswa = kelompokStatusKeluarMahasiswa;
		addWindow.setTitle(kelompokStatusKeluarMahasiswa.getId() == null ? "Tambah Status Keluar Mahasiswa" : "Ubah Status Keluar Mahasiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok *"));
		row.appendChild(nama = new Textbox(kelompokStatusKeluarMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Keluar *"));
		statusKeluar = new Combobox();
		Common.insertCombo(statusKeluar, "nama", StatusKeluar.class);
		Common.selectComboItem(statusKeluar, kelompokStatusKeluarMahasiswa.getStatusKeluar());
		row.appendChild(statusKeluar);
		statusKeluar.setWidth("90%");
		statusKeluar.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		Common.selectComboItem(tahunAkademik, kelompokStatusKeluarMahasiswa.getTahunAkademik());
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		semester = new Combobox();
		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);
		Common.selectComboItem(semester, kelompokStatusKeluarMahasiswa.getSemester());
		row.appendChild(semester);
		semester.setWidth("90%");
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Status Keluar"));
		row.appendChild(tanggalLulus = new MyDatebox(kelompokStatusKeluarMahasiswa.getTanggalLulus()));
		tanggalLulus.setDisabled(false);
		tanggalLulus.setReadonly(false);

		Common.initKeterangan(rows,
				"Kosongkan tanggal lulus jika mahasiswa dalam kelompok ini mempunyai tanggal lulus yang berbeda");

//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("No. SK"));
//		row.appendChild(noSk = new Textbox(kelompokStatusKeluarMahasiswa.getNoSk()));
//		noSk.setWidth("90%");
//
//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal. SK"));
//		row.appendChild(tanggalSk = new MyDatebox(kelompokStatusKeluarMahasiswa.getTanggalSk()));
//		tanggalSk.setDisabled(false);
//		tanggalSk.setReadonly(false);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelompokStatusKeluarMahasiswa.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelompok Status Keluar Mahasiswa",
					"Kolom Nama Kelompok Status Keluar Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kelompok Status Keluar Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (statusKeluar.getSelectedItem() == null || statusKeluar.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Statatus Keluar",
					"Kolom Statatus Keluar belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Statatus Keluar.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokStatusKeluarMahasiswa.getId() != null) {
			kelompokStatusKeluarMahasiswa = (KelompokStatusKeluarMahasiswa) session
					.load(KelompokStatusKeluarMahasiswa.class, kelompokStatusKeluarMahasiswa.getId());

		}
		kelompokStatusKeluarMahasiswa.setNama(nama.getValue());

		kelompokStatusKeluarMahasiswa.setStatusKeluar((StatusKeluar) (statusKeluar.getSelectedItem() == null ? null
				: statusKeluar.getSelectedItem().getValue()));
		kelompokStatusKeluarMahasiswa.setTanggalLulus(tanggalLulus.getValue());
		kelompokStatusKeluarMahasiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		kelompokStatusKeluarMahasiswa.setSemester((String) semester.getSelectedItem().getValue());
		kelompokStatusKeluarMahasiswa.setKeterangan(keterangan.getValue());
//		kelompokStatusKeluarMahasiswa.setNoSk(noSk.getValue());
//		kelompokStatusKeluarMahasiswa.setTanggalSk(tanggalSk.getValue());

		Common.refreshSaveOrUpdate(session, kelompokStatusKeluarMahasiswa);

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		List<Long> ids = new ArrayList<Long>();

		Session session = HibernateUtil.currentSession();

		if (!searchMahasiswa.getValue().trim().isEmpty()) {
			ids = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(
							Restrictions.ilike("nim", searchMahasiswa.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.ilike("nama", searchMahasiswa.getValue().trim(), MatchMode.ANYWHERE)))
					.add(Restrictions.isNotNull("kelompokStatusKeluarMahasiswa"))
					.setProjection(Projections.groupProperty("kelompokStatusKeluarMahasiswa.id")).list();
		}

		Criteria criteria = session.createCriteria(KelompokStatusKeluarMahasiswa.class);

		if (!ids.isEmpty()) {
			criteria.add(Restrictions.in("id", ids));
		}

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokStatusKeluarMahasiswa> kelompokStatusKeluarMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokStatusKeluarMahasiswa);
		grid.setRowRenderer(new KelompokStatusKeluarMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
