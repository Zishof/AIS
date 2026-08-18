package ais.action.master.bkd;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import ais.ui.util.MyGrid;
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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KewajibanBebanDosen;
import ais.database.model.StatusKewajibanBebanDosen;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KewajibanBebanDosenAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchstatus;

	private Combobox statusKewajibanBebanDosen;
	private MyDoublebox minimalSks;
	private MyDoublebox minimalSksPendidikan;
	private MyDoublebox minimalSksPenelitian;
	private MyDoublebox minimalSksPengabdian;
	private MyDoublebox minimalSksPenunjang;
	private MyDoublebox maksimalSks;
	private MyCheckboxConfig pendidikan;
	private MyCheckboxConfig penelitian;
	private MyCheckboxConfig pengabdian;
	private MyCheckboxConfig penunjang;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KewajibanBebanDosen kewajibanBebanDosen;
	private MyToolbarbuttonConfig add;

	public static String[] contents = new String[] { "id", "statusKewajibanBebanDosen", "minimalSks",
			"minimalSksPendidikan", "minimalSksPenelitian", "minimalSksPengabdian", "minimalSksPenunjang",
			"maksimalSks", "pendidikan", "penelitian", "pengabdian", "penunjang", "aktif", "keterangan" };

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

		Common.insertCombo(searchstatus, "nama", StatusKewajibanBebanDosen.class);

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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KewajibanBebanDosen.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

	}

	class AsesorPenunjangKinerjaDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final KewajibanBebanDosen kewajibanBebanDosen = (KewajibanBebanDosen) arg1;

			RevisiHelper.createNewRevisi(KewajibanBebanDosen.class, kewajibanBebanDosen,
					kewajibanBebanDosen.getStatusKewajibanBebanDosen().getNama()).setParent(arg0);

			new Label(Common.numberFormat.get().format(kewajibanBebanDosen.getMinimalSks()) + " s.d "
					+ Common.numberFormat.get().format(kewajibanBebanDosen.getMaksimalSks())).setParent(arg0);

			new Label(kewajibanBebanDosen.getPendidikan() ? "Ya" : "Tidak").setParent(arg0);
			new Label(kewajibanBebanDosen.getPenelitian() ? "Ya" : "Tidak").setParent(arg0);
			new Label(kewajibanBebanDosen.getPengabdian() ? "Ya" : "Tidak").setParent(arg0);
			new Label(kewajibanBebanDosen.getPenunjang() ? "Ya" : "Tidak").setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kewajibanBebanDosen.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kewajibanBebanDosen.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kewajibanBebanDosen);
				}
			});

			new Label(kewajibanBebanDosen.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kewajibanBebanDosen);
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

											Common.refreshDelete(kewajibanBebanDosen);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		KewajibanBebanDosen kewajibanBebanDosen = new KewajibanBebanDosen();
		init(kewajibanBebanDosen);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KewajibanBebanDosen kewajibanBebanDosen) throws Exception {

		this.kewajibanBebanDosen = kewajibanBebanDosen;
		addWindow.setTitle(kewajibanBebanDosen.getId() == null ? "Tambah Kewajiban Dosen" : "Ubah Kewajiban Dosen");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Dosen"));
		row.appendChild(statusKewajibanBebanDosen = new Combobox());
		Common.insertCombo(statusKewajibanBebanDosen, "nama", StatusKewajibanBebanDosen.class);
		Common.selectComboItem(statusKewajibanBebanDosen, kewajibanBebanDosen.getStatusKewajibanBebanDosen());
		statusKewajibanBebanDosen.setWidth("90%");
		statusKewajibanBebanDosen.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(pendidikan = new MyCheckboxConfig("Pendidikan"));
		pendidikan.setChecked(kewajibanBebanDosen.getPendidikan());
		hbox.appendChild(new MyLabelConfig(", minimal "));
		hbox.appendChild(minimalSksPendidikan = new MyDoublebox(kewajibanBebanDosen.getMinimalSksPendidikan()));
		hbox.appendChild(new MyLabelConfig(" sks "));
		minimalSksPendidikan.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(penelitian = new MyCheckboxConfig("Penelitian"));
		penelitian.setChecked(kewajibanBebanDosen.getPenelitian());
		hbox.appendChild(new MyLabelConfig(", minimal "));
		hbox.appendChild(minimalSksPenelitian = new MyDoublebox(kewajibanBebanDosen.getMinimalSksPenelitian()));
		hbox.appendChild(new MyLabelConfig(" sks "));
		minimalSksPenelitian.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(pengabdian = new MyCheckboxConfig("Pengabdian"));
		pengabdian.setChecked(kewajibanBebanDosen.getPengabdian());
		hbox.appendChild(new MyLabelConfig(", minimal "));
		hbox.appendChild(minimalSksPengabdian = new MyDoublebox(kewajibanBebanDosen.getMinimalSksPengabdian()));
		hbox.appendChild(new MyLabelConfig(" sks "));
		minimalSksPengabdian.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(penunjang = new MyCheckboxConfig("Penunjang"));
		penunjang.setChecked(kewajibanBebanDosen.getPenunjang());
		hbox.appendChild(new MyLabelConfig(", minimal "));
		hbox.appendChild(minimalSksPenunjang = new MyDoublebox(kewajibanBebanDosen.getMinimalSksPenunjang()));
		hbox.appendChild(new MyLabelConfig(" sks "));
		minimalSksPenunjang.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal SKS"));
		row.appendChild(minimalSks = new MyDoublebox(kewajibanBebanDosen.getMinimalSks()));
		// minimalSks.setDisabled(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				minimalSksPendidikan.setDisabled(!pendidikan.isChecked());
				if (minimalSksPendidikan.isDisabled()) {
					minimalSksPendidikan.setValue(0.0);
				}
				minimalSksPenelitian.setDisabled(!penelitian.isChecked());
				if (minimalSksPenelitian.isDisabled()) {
					minimalSksPenelitian.setValue(0.0);
				}
				minimalSksPengabdian.setDisabled(!pengabdian.isChecked());
				if (minimalSksPengabdian.isDisabled()) {
					minimalSksPengabdian.setValue(0.0);
				}
				minimalSksPenunjang.setDisabled(!penunjang.isChecked());
				if (minimalSksPenunjang.isDisabled()) {
					minimalSksPenunjang.setValue(0.0);
				}

				// Double total = (minimalSksPendidikan.getValue() == null ? 0.0
				// : minimalSksPendidikan.getValue())
				// + (minimalSksPenelitian.getValue() == null ? 0.0 :
				// minimalSksPenelitian.getValue())
				// + (minimalSksPengabdian.getValue() == null ? 0.0 :
				// minimalSksPengabdian.getValue())
				// + (minimalSksPenunjang.getValue() == null ? 0.0 :
				// minimalSksPenunjang.getValue());
				//
				// minimalSks.setValue(total);
			}
		};

		pendidikan.addEventListener("onClick", eventListener);
		penelitian.addEventListener("onClick", eventListener);
		pengabdian.addEventListener("onClick", eventListener);
		penunjang.addEventListener("onClick", eventListener);

		minimalSksPendidikan.addEventListener("onChange", eventListener);
		minimalSksPenelitian.addEventListener("onChange", eventListener);
		minimalSksPengabdian.addEventListener("onChange", eventListener);
		minimalSksPenunjang.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Maksimal SKS"));
		row.appendChild(maksimalSks = new MyDoublebox(kewajibanBebanDosen.getMaksimalSks()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				kewajibanBebanDosen.getKeterangan() == null ? "" : kewajibanBebanDosen.getKeterangan()));
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

		if (statusKewajibanBebanDosen.getSelectedItem() == null) {
			MyMessageboxConfig.show("Status dosen harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kewajibanBebanDosen.getId() != null) {
			kewajibanBebanDosen = (KewajibanBebanDosen) session.load(KewajibanBebanDosen.class,
					kewajibanBebanDosen.getId());

		}

		kewajibanBebanDosen.setStatusKewajibanBebanDosen(
				(StatusKewajibanBebanDosen) statusKewajibanBebanDosen.getSelectedItem().getValue());
		kewajibanBebanDosen.setMaksimalSks(maksimalSks.getValue());
		kewajibanBebanDosen.setMinimalSks(minimalSks.getValue());
		kewajibanBebanDosen.setPendidikan(pendidikan.isChecked());
		kewajibanBebanDosen.setPenelitian(penelitian.isChecked());
		kewajibanBebanDosen.setPengabdian(pengabdian.isChecked());
		kewajibanBebanDosen.setPenunjang(penunjang.isChecked());
		kewajibanBebanDosen.setKeterangan(keterangan.getValue());

		kewajibanBebanDosen.setMinimalSksPendidikan(minimalSksPendidikan.getValue());
		kewajibanBebanDosen.setMinimalSksPenelitian(minimalSksPenelitian.getValue());
		kewajibanBebanDosen.setMinimalSksPengabdian(minimalSksPengabdian.getValue());
		kewajibanBebanDosen.setMinimalSksPenunjang(minimalSksPenunjang.getValue());

		Common.refreshSaveOrUpdate(session, kewajibanBebanDosen);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KewajibanBebanDosen.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("true")
				: Restrictions.eq("statusKewajibanBebanDosen", searchstatus.getSelectedItem().getValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KewajibanBebanDosen> kewajibanBebanDosen = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kewajibanBebanDosen);
		grid.setRowRenderer(new AsesorPenunjangKinerjaDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
