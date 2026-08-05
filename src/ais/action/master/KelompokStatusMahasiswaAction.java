package ais.action.master;

import java.math.BigDecimal;
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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.KelompokStatusMahasiswaDetailAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisKelompokStatusMahasiswa;
import ais.database.model.KelompokStatusMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelompokStatusMahasiswaAction extends GenericAutowireComposer
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
	private Combobox statusMahasiswa;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelompokStatusMahasiswa kelompokStatusMahasiswa;
	private MyToolbarbuttonConfig add;
	private Decimalbox smtMulai;
	private Decimalbox smtSampai;

	private Tabpanel manajemenStatusMahasiswa;
	private Combobox jenisKelompokStatusMahasiswa;

	public void onManajemenStatusMahasiswa(Event event) {
		if (manajemenStatusMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenStatusMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/jenis_kelompok_status_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

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

		String[] contents = new String[] { "id", "nama", "statusMahasiswa", "smtMulai", "smtSampai",
				"jenisKelompokStatusMahasiswa", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KelompokStatusMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokStatusMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KelompokStatusMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokStatusMahasiswa kelompokStatusMahasiswa = (KelompokStatusMahasiswa) arg1;

			(new KelompokStatusMahasiswaDetailAction(kelompokStatusMahasiswa)).setParent(arg0);

			RevisiHelper.createNewRevisi(KelompokStatusMahasiswa.class, kelompokStatusMahasiswa,
					kelompokStatusMahasiswa.getNama()).setParent(arg0);
			new Label(kelompokStatusMahasiswa.getStatusMahasiswa() == null ? ""
					: kelompokStatusMahasiswa.getStatusMahasiswa().getNama()).setParent(arg0);

			new Label(kelompokStatusMahasiswa.getJenisKelompokStatusMahasiswa() == null ? ""
					: kelompokStatusMahasiswa.getJenisKelompokStatusMahasiswa().getNama()).setParent(arg0);

			new Label(kelompokStatusMahasiswa.getSmtMulai() + " sd " + kelompokStatusMahasiswa.getSmtSampai())
					.setParent(arg0);

			new Label(kelompokStatusMahasiswa.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, kelompokStatusMahasiswa, KelompokStatusMahasiswaAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KelompokStatusMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kelompokStatusMahasiswa = (KelompokStatusMahasiswa) obj;
		init(kelompokStatusMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokStatusMahasiswa kelompokStatusMahasiswa) {
		this.kelompokStatusMahasiswa = kelompokStatusMahasiswa;
		addWindow.setTitle(kelompokStatusMahasiswa.getId() == null ? "Tambah Kelompok Mahasiswa" : "Ubah Kelompok Mahasiswa");
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
		row.appendChild(nama = new Textbox(kelompokStatusMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa *"));
		statusMahasiswa = new Combobox();
		Common.insertCombo(statusMahasiswa, "nama", StatusMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusMahasiswa, kelompokStatusMahasiswa.getStatusMahasiswa());
		row.appendChild(statusMahasiswa);
		statusMahasiswa.setWidth("90%");
		statusMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis *"));
		jenisKelompokStatusMahasiswa = new Combobox();
		Common.insertCombo(jenisKelompokStatusMahasiswa, "nama", JenisKelompokStatusMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisKelompokStatusMahasiswa, kelompokStatusMahasiswa.getJenisKelompokStatusMahasiswa());
		row.appendChild(jenisKelompokStatusMahasiswa);
		jenisKelompokStatusMahasiswa.setWidth("90%");
		jenisKelompokStatusMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(new Hbox(
				new Component[] { smtMulai = new Decimalbox(new BigDecimal(kelompokStatusMahasiswa.getSmtMulai())),
						new ais.ui.util.MyLabelConfig(" s.d "),
						smtSampai = new Decimalbox(new BigDecimal(kelompokStatusMahasiswa.getSmtSampai())) }));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelompokStatusMahasiswa.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelompok Status Mahasiswa",
					"Kolom Nama Kelompok Status Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kelompok Status Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (statusMahasiswa.getSelectedItem() == null || statusMahasiswa.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Status",
					"Kolom Status belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Status.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisKelompokStatusMahasiswa.getSelectedItem() == null
				|| jenisKelompokStatusMahasiswa.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis",
					"Kolom Jenis belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		Session session = HibernateUtil.currentSession();
		if (kelompokStatusMahasiswa.getId() != null) {
			kelompokStatusMahasiswa = (KelompokStatusMahasiswa) session.load(KelompokStatusMahasiswa.class,
					kelompokStatusMahasiswa.getId());

		}
		kelompokStatusMahasiswa.setNama(nama.getValue());

		kelompokStatusMahasiswa.setStatusMahasiswa((StatusMahasiswa) (statusMahasiswa.getSelectedItem() == null ? null
				: statusMahasiswa.getSelectedItem().getValue()));

		kelompokStatusMahasiswa.setJenisKelompokStatusMahasiswa(
				(JenisKelompokStatusMahasiswa) jenisKelompokStatusMahasiswa.getSelectedItem().getValue());

		kelompokStatusMahasiswa.setSmtMulai(smtMulai.getValue() == null ? null : smtMulai.getValue().intValue());
		kelompokStatusMahasiswa.setSmtSampai(smtSampai.getValue() == null ? null : smtSampai.getValue().intValue());

		kelompokStatusMahasiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, kelompokStatusMahasiswa);

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
					.add(Restrictions.isNotNull("kelompokStatusMahasiswa"))
					.setProjection(Projections.groupProperty("kelompokStatusMahasiswa.id")).list();
		}
		Criteria criteria = session.createCriteria(KelompokStatusMahasiswa.class);

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

		List<KelompokStatusMahasiswa> kelompokStatusMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokStatusMahasiswa);
		grid.setRowRenderer(new KelompokStatusMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
