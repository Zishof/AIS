package ais.action.master.sop;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DokumenAlurSop;
import ais.database.model.sop.Sop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DokumenAlurSopAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchsop;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private DokumenAlurSop dokumenAlurSop;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	protected LampiranLain lainMahasiswa;
	private Combobox sop;

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

		Criterion criterion = Restrictions.eq("aktif", true);

//		criterion = Restrictions.and(criterion,
//				searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
//						|| searchjurusan.getSelectedItem().getValue() == null
//								? Restrictions.sqlRestriction("1=1")
//								: CommonSearchFilterHelper.eqSelectedWithId("sop.jurusan", searchjurusan, false));
//
//		criterion = Restrictions.and(criterion,
//				searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
//						|| searchfakultas.getSelectedItem().getValue() == null
//								? Restrictions.sqlRestriction("1=1")
//								: CommonSearchFilterHelper.eqSelectedWithId("sop.fakultas", searchfakultas, false));
//
//		criterion = Restrictions.and(criterion,
//				searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
//						|| searchsekolah.getSelectedItem().getValue() == null
//								? Restrictions.sqlRestriction("1=1")
//								: CommonSearchFilterHelper.eqSelectedWithId("sop.sekolah", searchsekolah, false));
//
//		criterion = Restrictions.and(criterion,
//				searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
//						|| searchyayasan.getSelectedItem().getValue() == null
//								? Restrictions.sqlRestriction("1=1")
//								: CommonSearchFilterHelper.eqSelectedWithId("sop.yayasan", searchyayasan, false));

		Common.insertComboDanSemua(searchsop, "nama", Sop.class, criterion);

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

		String[] contents = new String[] { "id", "kode", "nama", "sop", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(DokumenAlurSop.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, DokumenAlurSop.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class DokumenAlurSopRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DokumenAlurSop dokumenAlurSop = (DokumenAlurSop) arg1;
			new Label(dokumenAlurSop.getKode()).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(DokumenAlurSop.class, dokumenAlurSop, dokumenAlurSop.getNama()))
					.setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, dokumenAlurSop.getId(), DokumenAlurSop.class.getName(),
					"Dokumen", false, null, null, false, false, false, false);

			new Label(dokumenAlurSop.getSop() == null ? "Semua"
					: dokumenAlurSop.getSop().getKode() + "-" + dokumenAlurSop.getSop().getNama()).setParent(arg0);

			new Label(dokumenAlurSop.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig wajib = new MyCheckboxConfig("Wajib");
			wajib.setDisabled(!edit);
			wajib.setChecked(dokumenAlurSop.getWajib());
			wajib.setParent(arg0);
			wajib.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dokumenAlurSop.setWajib(wajib.isChecked());
					Common.refreshSaveOrUpdate(dokumenAlurSop);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(dokumenAlurSop.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dokumenAlurSop.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(dokumenAlurSop);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, dokumenAlurSop, DokumenAlurSopAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new DokumenAlurSop());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		dokumenAlurSop = (DokumenAlurSop) obj;
		init(dokumenAlurSop);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(DokumenAlurSop dokumenAlurSop) {
		this.dokumenAlurSop = dokumenAlurSop;
		addWindow.setTitle(dokumenAlurSop.getId() == null ? "Tambah Dokumen SOP" : "Ubah Dokumen SOP");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Dokumen SOP *"));
		row.appendChild(kode = new Textbox(dokumenAlurSop.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Dokumen SOP *"));
		row.appendChild(nama = new Textbox(dokumenAlurSop.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus untuk SOP"));
		row.appendChild(sop = new Combobox());
		Common.insertComboDanSemua(sop, "nama", "kode", Sop.class, Restrictions.eq("aktif", true));
		sop.setWidth("90%");
		Common.selectComboItem(sop, dokumenAlurSop.getSop());

		Common.initKeterangan(rows, "Kosongkan jika berlaku untuk semua SOP");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(dokumenAlurSop.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen SOP"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, dokumenAlurSop.getId(), DokumenAlurSop.class.getName(),
				"Dokumen SOP", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Jika file lampiran Dokumen SOP lebih dari satu file, zip dulu semua file tersebut");

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
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Dokumen SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Kode Dokumen SOP; (2) isikan kode yang unik dan sesuai ketentuan; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Dokumen SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Dokumen SOP; (2) isikan nama dokumen SOP yang sesuai; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (dokumenAlurSop.getId() != null) {
			dokumenAlurSop = (DokumenAlurSop) session.load(DokumenAlurSop.class, dokumenAlurSop.getId());

		}

		dokumenAlurSop.setKode(kode.getValue());
		dokumenAlurSop.setNama(nama.getValue());
		dokumenAlurSop.setSop((Sop) (sop.getSelectedItem() == null ? null : sop.getSelectedItem().getValue()));
		dokumenAlurSop.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, dokumenAlurSop);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(dokumenAlurSop.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DokumenAlurSop.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

		;

		if (order)
			criteria.addOrder(Order.asc("sop")).addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
		criteria.add(searchsop.getSelectedItem() == null || searchsop.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("true")
				: Restrictions.eq("sop", searchsop.getSelectedItem().getValue()))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DokumenAlurSop> dokumenAlurSop = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(dokumenAlurSop);
		grid.setRowRenderer(new DokumenAlurSopRenderer());
		grid.setModelCheckMobile(strset);

	}

}
