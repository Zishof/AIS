package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.MasaJadwalPelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class MasaJadwalPelajaranAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchketerangan;
	private Combobox searchtahunajaran;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchprogram;

	private Textbox nama;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private Combobox tahunAjaran;
	private Combobox sekolah;
	private Combobox yayasan;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private MasaJadwalPelajaran masaJadwalPelajaran;
	private MyToolbarbuttonConfig add;
	private EventListener eventListener;
	private Combobox semester;
	private Tbmuser tbmuser;

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
		tbmuser = Common.getCurrentUser();
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Common.generateTahunAjaran(searchtahunajaran);
		Common.initPrograms(searchprogram);

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
	        FilterLanjutHelper.setup(comp);
}

	public static void onAddExternal(Event event, EventListener eventListener, MasaJadwalPelajaran masaJadwalPelajaran)
			throws Exception {
		MasaJadwalPelajaranAction masaJadwalPelajaranAction = new MasaJadwalPelajaranAction();
		masaJadwalPelajaranAction.eventListener = eventListener;
		masaJadwalPelajaranAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(masaJadwalPelajaranAction.addWindow);
		masaJadwalPelajaranAction.addWindow.setHeight("350px");
		masaJadwalPelajaranAction.addWindow.setWidth("550px");

		masaJadwalPelajaranAction.init(masaJadwalPelajaran);

		masaJadwalPelajaranAction.addWindow.setVisible(true);
		masaJadwalPelajaranAction.addWindow.onModal();
	}

	class MasaJadwalPelajaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MasaJadwalPelajaran masaJadwalPelajaran = (MasaJadwalPelajaran) arg1;

			RevisiHelper.createNewRevisi(MasaJadwalPelajaran.class, masaJadwalPelajaran, masaJadwalPelajaran.getNama())
					.setParent(arg0);

			new Label(masaJadwalPelajaran.getMulai() == null ? ""
					: Common.dateFormat6.get().format(masaJadwalPelajaran.getMulai())).setParent(arg0);
			new Label(masaJadwalPelajaran.getSampai() == null ? ""
					: Common.dateFormat6.get().format(masaJadwalPelajaran.getSampai())).setParent(arg0);
			new Label(masaJadwalPelajaran.getTahunAjaran()).setParent(arg0);
			new Label(masaJadwalPelajaran.getNamaSmt()).setParent(arg0);

			new Label(masaJadwalPelajaran.getSekolah() == null ? "Semua" : masaJadwalPelajaran.getSekolah().getNama())
					.setParent(arg0);
			new Label(masaJadwalPelajaran.getYayasan() == null ? "Semua" : masaJadwalPelajaran.getYayasan().getNama())
					.setParent(arg0);

			int jumlahJadwal = ((Number) HibernateUtil.currentSession().createCriteria(JadwalPelajaran.class)
					.add(Restrictions.eq("masaJadwalPelajaran", masaJadwalPelajaran))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			new Label(Common.numberFormat.get().format(jumlahJadwal)).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(masaJadwalPelajaran.getAktif());
			checkbox.setParent(vbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					masaJadwalPelajaran.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(masaJadwalPelajaran);
				}
			});
			final MyCheckboxConfig defaultData = new MyCheckboxConfig("Default");
			defaultData.setChecked(masaJadwalPelajaran.getDefaultData());
			defaultData.setParent(vbox);
			defaultData.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					masaJadwalPelajaran.setDefaultData(defaultData.isChecked());
					Common.refreshSaveOrUpdate(masaJadwalPelajaran);

					HibernateUtil.currentSession()
							.createSQLQuery("update sekolah.masa_jadwal_pelajaran set default_data=false where id != "
									+ masaJadwalPelajaran.getId() + " and tahunajaran='"
									+ masaJadwalPelajaran.getTahunAjaran() + "' and semester="
									+ masaJadwalPelajaran.getSemester())
							.executeUpdate();
					onSearchDefault(arg0);
				}
			});

			new Label(masaJadwalPelajaran.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(masaJadwalPelajaran);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											onDelete(masaJadwalPelajaran);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);

			GeneralValueObject.tampilKunci(toolbar, masaJadwalPelajaran, tbmuser, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}

			}, false);
		}

	}

	public static void onDelete(MasaJadwalPelajaran masaJadwalPelajaran) {

		Common.refreshDelete(masaJadwalPelajaran);
	}

	public void onAdd(Event event) throws Exception {
		init(new MasaJadwalPelajaran());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(MasaJadwalPelajaran masaJadwalPelajaran) {
		this.masaJadwalPelajaran = masaJadwalPelajaran;
		addWindow.setTitle(masaJadwalPelajaran.getId() == null ? "Tambah Masa JadwalPelajaran" : "Ubah Masa JadwalPelajaran");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Pembelajaran *"));
		row.appendChild(nama = new Textbox(masaJadwalPelajaran.getNama() == null ? "" : masaJadwalPelajaran.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai *"));
		row.appendChild(mulai = new MyDatebox(masaJadwalPelajaran.getMulai()));
		mulai.setWidth("90%");
		mulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai *"));
		row.appendChild(sampai = new MyDatebox(masaJadwalPelajaran.getSampai()));
		sampai.setWidth("90%");
		sampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		row.appendChild(tahunAjaran = new Combobox());
		Common.generateTahunAjaran(tahunAjaran);
		Common.selectComboItem(tahunAjaran, masaJadwalPelajaran.getTahunAjaran());
		tahunAjaran.setWidth("90%");
		tahunAjaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(semester = new Combobox());
		Comboitem comboitem = new Comboitem(JadwalPelajaran.GANJIL);
		comboitem.setValue(1);
		semester.appendChild(comboitem);
		comboitem = new Comboitem(JadwalPelajaran.GENAP);
		comboitem.setValue(2);
		semester.appendChild(comboitem);
		Common.selectComboItem(true, semester, masaJadwalPelajaran.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, masaJadwalPelajaran.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, masaJadwalPelajaran.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				masaJadwalPelajaran.getKeterangan() == null ? "" : masaJadwalPelajaran.getKeterangan()));
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

					if (eventListener != null) {
						eventListener
								.onEvent(new Event("", addWindow, MasaJadwalPelajaranAction.this.masaJadwalPelajaran));
					}

					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		if (masaJadwalPelajaran.getDikunci() != null) {
			save.setVisible(false);
			Common.freezeGanti(center, true);
		}

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Masa JadwalPelajaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tahunAjaran.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Ajaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (mulai.getValue() == null) {
			MyMessageboxConfig.show("Mulai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sampai.getValue() == null) {
			MyMessageboxConfig.show("Sampai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (masaJadwalPelajaran.getId() != null) {
			masaJadwalPelajaran = (MasaJadwalPelajaran) session.load(MasaJadwalPelajaran.class,
					masaJadwalPelajaran.getId());

		}
		masaJadwalPelajaran.setTahunAjaran((String) tahunAjaran.getSelectedItem().getValue());
		masaJadwalPelajaran.setNama(nama.getValue());
		masaJadwalPelajaran.setKeterangan(keterangan.getValue());
		masaJadwalPelajaran.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null ? null
						: sekolah.getSelectedItem().getValue()));
		masaJadwalPelajaran.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null ? null
						: yayasan.getSelectedItem().getValue()));
		masaJadwalPelajaran.setMulai(mulai.getValue());
		masaJadwalPelajaran.setSampai(sampai.getValue());
		masaJadwalPelajaran.setSemester((Integer) semester.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, masaJadwalPelajaran);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MasaJadwalPelajaran.class);
		if (order)
			criteria.addOrder(Order.asc("mulai"));
		if (order)
			criteria.addOrder(Order.asc("sampai"));

		criteria.add(Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchtahunajaran.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAjaran", searchtahunajaran.getSelectedItem().getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchketerangan == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);
		List<MasaJadwalPelajaran> masaJadwalPelajaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(masaJadwalPelajaran);
		grid.setRowRenderer(new MasaJadwalPelajaranRenderer());
		grid.setModelCheckMobile(strset);

	}
}
