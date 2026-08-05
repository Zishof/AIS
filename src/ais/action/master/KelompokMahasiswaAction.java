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

import ais.action.master.helper.KelompokMahasiswaDetailAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisDiskonMahasiswa;
import ais.database.model.KelompokMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusAwalMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelompokMahasiswaAction extends GenericAutowireComposer
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
	private Combobox statusAwalMahasiswa;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelompokMahasiswa kelompokMahasiswa;
	private MyToolbarbuttonConfig add;
	private Decimalbox smtMulai;
	private Decimalbox smtSampai;
	private Combobox statusAwalMahasiswa2;
	private Decimalbox smtMulai2;
	private Decimalbox smtSampai2;
	private Combobox statusAwalMahasiswa3;
	private Decimalbox smtMulai3;
	private Decimalbox smtSampai3;

	private Tabpanel manajemenStatusAwalMahasiswa;
	private Combobox jenisDiskonMahasiswa;

	public void onManajemenStatusAwalMahasiswa(Event event) {
		if (manajemenStatusAwalMahasiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenStatusAwalMahasiswa);
			MyInclude iframe = new MyInclude("/pages/master/status_awal_mahasiswa.zul");
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

		String[] contents = new String[] { "id", "nama", "statusAwalMahasiswa", "smtMulai", "smtSampai"

				, "statusAwalMahasiswa2", "smtMulai2", "smtSampai2", "statusAwalMahasiswa3", "smtMulai3", "smtSampai3"

				, "jenisDiskonMahasiswa", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KelompokMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KelompokMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelompokMahasiswa kelompokMahasiswa = (KelompokMahasiswa) arg1;

			(new KelompokMahasiswaDetailAction(kelompokMahasiswa)).setParent(arg0);

			RevisiHelper.createNewRevisi(KelompokMahasiswa.class, kelompokMahasiswa, kelompokMahasiswa.getNama())
					.setParent(arg0);
			new Label(kelompokMahasiswa.getStatusAwalMahasiswa() == null ? ""
					: kelompokMahasiswa.getStatusAwalMahasiswa().getNama()).setParent(arg0);
			new Label(kelompokMahasiswa.getJenisDiskonMahasiswa() == null ? ""
					: kelompokMahasiswa.getJenisDiskonMahasiswa().getNama()).setParent(arg0);
			new Label(kelompokMahasiswa.getSmtMulai() + " sd " + kelompokMahasiswa.getSmtSampai()).setParent(arg0);

			new Label(kelompokMahasiswa.getStatusAwalMahasiswa2() == null ? ""
					: kelompokMahasiswa.getStatusAwalMahasiswa2().getNama()).setParent(arg0);
			new Label(kelompokMahasiswa.getSmtMulai2() + " sd " + kelompokMahasiswa.getSmtSampai2()).setParent(arg0);

			new Label(kelompokMahasiswa.getStatusAwalMahasiswa3() == null ? ""
					: kelompokMahasiswa.getStatusAwalMahasiswa3().getNama()).setParent(arg0);
			new Label(kelompokMahasiswa.getSmtMulai3() + " sd " + kelompokMahasiswa.getSmtSampai3()).setParent(arg0);

			new Label(kelompokMahasiswa.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, kelompokMahasiswa, KelompokMahasiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KelompokMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kelompokMahasiswa = (KelompokMahasiswa) obj;
		init(kelompokMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokMahasiswa kelompokMahasiswa) {
		this.kelompokMahasiswa = kelompokMahasiswa;
		addWindow.setTitle(kelompokMahasiswa.getId() == null ? "Tambah Kelompok Mahasiswa" : "Ubah Kelompok Mahasiswa");
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
		row.appendChild(nama = new Textbox(kelompokMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal (I) *"));
		statusAwalMahasiswa = new Combobox();
		Common.insertCombo(statusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusAwalMahasiswa, kelompokMahasiswa.getStatusAwalMahasiswa());
		row.appendChild(statusAwalMahasiswa);
		statusAwalMahasiswa.setWidth("90%");
		statusAwalMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (I)"));
		row.appendChild(
				new Hbox(new Component[] { smtMulai = new Decimalbox(new BigDecimal(kelompokMahasiswa.getSmtMulai())),
						new ais.ui.util.MyLabelConfig(" s.d "),
						smtSampai = new Decimalbox(new BigDecimal(kelompokMahasiswa.getSmtSampai())) }));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diskon (I)"));
		row.appendChild(jenisDiskonMahasiswa = new Combobox());
		Common.insertComboDanSemua(jenisDiskonMahasiswa, new String[] { "nama" }, "keterangan",
				JenisDiskonMahasiswa.class, "=Tanpa Diskon=", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisDiskonMahasiswa, kelompokMahasiswa.getJenisDiskonMahasiswa());
		jenisDiskonMahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal (II) *"));
		statusAwalMahasiswa2 = new Combobox();
		Common.insertCombo(statusAwalMahasiswa2, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusAwalMahasiswa2, kelompokMahasiswa.getStatusAwalMahasiswa2());
		row.appendChild(statusAwalMahasiswa2);
		statusAwalMahasiswa2.setWidth("90%");
		statusAwalMahasiswa2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (II)"));
		row.appendChild(
				new Hbox(new Component[] { smtMulai2 = new Decimalbox(new BigDecimal(kelompokMahasiswa.getSmtMulai2())),
						new ais.ui.util.MyLabelConfig(" s.d "),
						smtSampai2 = new Decimalbox(new BigDecimal(kelompokMahasiswa.getSmtSampai2())) }));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal (III) *"));
		statusAwalMahasiswa3 = new Combobox();
		Common.insertCombo(statusAwalMahasiswa3, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusAwalMahasiswa3, kelompokMahasiswa.getStatusAwalMahasiswa3());
		row.appendChild(statusAwalMahasiswa3);
		statusAwalMahasiswa3.setWidth("90%");
		statusAwalMahasiswa3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester (III)"));
		row.appendChild(
				new Hbox(new Component[] { smtMulai3 = new Decimalbox(new BigDecimal(kelompokMahasiswa.getSmtMulai3())),
						new ais.ui.util.MyLabelConfig(" s.d "),
						smtSampai3 = new Decimalbox(new BigDecimal(kelompokMahasiswa.getSmtSampai3())) }));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelompokMahasiswa.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelompok Mahasiswa",
					"Kolom Nama Kelompok Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Kelompok Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (statusAwalMahasiswa.getSelectedItem() == null || statusAwalMahasiswa.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Statatus Awal (I)",
					"Kolom Statatus Awal (I) belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Statatus Awal (I).",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokMahasiswa.getId() != null) {
			kelompokMahasiswa = (KelompokMahasiswa) session.load(KelompokMahasiswa.class, kelompokMahasiswa.getId());

		}
		kelompokMahasiswa.setNama(nama.getValue());

		kelompokMahasiswa
				.setStatusAwalMahasiswa((StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null ? null
						: statusAwalMahasiswa.getSelectedItem().getValue()));

		kelompokMahasiswa.setSmtMulai(smtMulai.getValue() == null ? null : smtMulai.getValue().intValue());
		kelompokMahasiswa.setSmtSampai(smtSampai.getValue() == null ? null : smtSampai.getValue().intValue());

		kelompokMahasiswa
				.setStatusAwalMahasiswa2((StatusAwalMahasiswa) (statusAwalMahasiswa2.getSelectedItem() == null ? null
						: statusAwalMahasiswa2.getSelectedItem().getValue()));

		kelompokMahasiswa.setSmtMulai2(smtMulai2.getValue() == null ? null : smtMulai2.getValue().intValue());
		kelompokMahasiswa.setSmtSampai2(smtSampai2.getValue() == null ? null : smtSampai2.getValue().intValue());

		kelompokMahasiswa
				.setStatusAwalMahasiswa3((StatusAwalMahasiswa) (statusAwalMahasiswa3.getSelectedItem() == null ? null
						: statusAwalMahasiswa3.getSelectedItem().getValue()));

		kelompokMahasiswa.setSmtMulai3(smtMulai3.getValue() == null ? null : smtMulai3.getValue().intValue());
		kelompokMahasiswa.setSmtSampai3(smtSampai3.getValue() == null ? null : smtSampai3.getValue().intValue());

		kelompokMahasiswa
				.setJenisDiskonMahasiswa((JenisDiskonMahasiswa) (jenisDiskonMahasiswa.getSelectedItem() == null ? null
						: jenisDiskonMahasiswa.getSelectedItem().getValue()));
		kelompokMahasiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, kelompokMahasiswa);

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
					.add(Restrictions.isNotNull("kelompokMahasiswa"))
					.setProjection(Projections.groupProperty("kelompokMahasiswa.id")).list();
		}

		Criteria criteria = session.createCriteria(KelompokMahasiswa.class);

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

		List<KelompokMahasiswa> kelompokMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokMahasiswa);
		grid.setRowRenderer(new KelompokMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
