package ais.action.master.payroll;

import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.payroll.util.ItemGajiTreeModel;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.payroll.Cabang;
import ais.database.model.payroll.Departemen;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.LevelJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class FormatItemGajiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private Combobox searchcabang;
	private Combobox searchdepartemen;
	private Combobox searchlevelJabatan;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Checkbox searchaktif;

	private MyTextbox nama;
	private MyTextbox keterangan;

	private Combobox cabang;
	private Combobox departemen;
	private Combobox levelJabatan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean addB = false;

	private FormatItemGaji formatItemGaji;
	private MyToolbarbuttonConfig add;
	private Boolean copy;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		Common.insertComboDanSemua(searchcabang, "nama", "keterangan", Cabang.class);
		Common.insertComboDanSemua(searchdepartemen, "nama", "keterangan", Departemen.class);
		Common.insertComboDanSemua(searchlevelJabatan, "nama", "keterangan", LevelJabatan.class);

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		addB = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		if (add != null) { add.setVisible(addB); }
		if (add != null) { add.setTooltiptext("Tambah"); }

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

	class FormatItemGajiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FormatItemGaji formatItemGaji = (FormatItemGaji) arg1;

			RevisiHelper.createNewRevisi(FormatItemGaji.class, formatItemGaji, formatItemGaji.getNama())
					.setParent(arg0);
			new Label(formatItemGaji.getSatuanKerja() == null ? "" : formatItemGaji.getSatuanKerja().getNama())
					.setParent(arg0);
			new Label(formatItemGaji.getCabang() == null ? "" : formatItemGaji.getCabang().getNama()).setParent(arg0);
			new Label(formatItemGaji.getDepartemen() == null ? "" : formatItemGaji.getDepartemen().getNama())
					.setParent(arg0);
			new Label(formatItemGaji.getLevelJabatan() == null ? "" : formatItemGaji.getLevelJabatan().getNama())
					.setParent(arg0);
			new Label(formatItemGaji.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(formatItemGaji.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					formatItemGaji.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(formatItemGaji);
				}
			});

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(formatItemGaji, false);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
			button.setTooltiptext("Copy Data");
			button.setVisible(addB);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(formatItemGaji, true);
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
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan. Tekan OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(formatItemGaji);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(MyMessageboxConfig.format(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang berelasi; (2) periksa kembali keterkaitan data; (3) hubungi administrator apabila kendala masih berlanjut.",
													e.getMessage()));
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
		init(new FormatItemGaji(), false);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(FormatItemGaji formatItemGaji, final Boolean copy) throws Exception {
		this.formatItemGaji = formatItemGaji;
		this.copy = copy;
		addWindow.setTitle(formatItemGaji.getId() == null ? "Tambah Format Item Gaji" : "Ubah Format Item Gaji");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Format Item Gaji")));
		row.appendChild(nama = new MyTextbox(formatItemGaji.getNama() == null ? "" : formatItemGaji.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(formatItemGaji.getSatuanKerja() == null ? "" : formatItemGaji.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", formatItemGaji.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Cabang")));
		row.appendChild(cabang = new Combobox());
		Common.insertCombo(cabang, "nama", "keterangan", Cabang.class);
		Common.selectComboItem(cabang, formatItemGaji.getCabang());
		cabang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Departemen")));
		row.appendChild(departemen = new Combobox());
		Common.insertCombo(departemen, "nama", "keterangan", Departemen.class);
		Common.selectComboItem(departemen, formatItemGaji.getDepartemen());
		departemen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jabatan")));
		row.appendChild(levelJabatan = new Combobox());
		Common.insertCombo(levelJabatan, "nama", "keterangan", LevelJabatan.class);
		Common.selectComboItem(levelJabatan, formatItemGaji.getLevelJabatan());
		levelJabatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				formatItemGaji.getKeterangan() == null ? "" : formatItemGaji.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
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
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Nama Format Item Gaji belum diisi. Kolom tersebut wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu mengisi kolom Nama Format Item Gaji; (2) pastikan kolom tersebut tidak dikosongkan; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkNamaFormatItemGaji();
		if (i) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nama Format Item Gaji yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) mohon gunakan nama yang berbeda; (2) periksa kembali daftar Format Item Gaji yang telah ada; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		FormatItemGaji tempFormatItemGaji = null;
		if (copy) {
			tempFormatItemGaji = (FormatItemGaji) formatItemGaji.clone();
			formatItemGaji = new FormatItemGaji();
		}

		Session session = HibernateUtil.currentSession();
		if (formatItemGaji.getId() != null) {
			formatItemGaji = (FormatItemGaji) session.load(FormatItemGaji.class, formatItemGaji.getId());
		}

		formatItemGaji.setCabang(
				(Cabang) (cabang.getSelectedItem() == null || cabang.getSelectedItem().getValue() == null ? null
						: cabang.getSelectedItem().getValue()));
		formatItemGaji.setDepartemen(
				(Departemen) (departemen.getSelectedItem() == null || departemen.getSelectedItem().getValue() == null
						? null
						: departemen.getSelectedItem().getValue()));
		formatItemGaji.setLevelJabatan((LevelJabatan) (levelJabatan.getSelectedItem() == null
				|| levelJabatan.getSelectedItem().getValue() == null ? null
						: levelJabatan.getSelectedItem().getValue()));
		formatItemGaji.setNama(nama.getValue());
		formatItemGaji.setKeterangan(keterangan.getValue());

		formatItemGaji.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		Common.refreshSaveOrUpdate(session, formatItemGaji);

		if (copy && tempFormatItemGaji != null) {
			new ItemGajiTreeModel(true, tempFormatItemGaji).copyByFormat(null, null, formatItemGaji);
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<FormatItemGaji> formatItemGaji = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(formatItemGaji);
		grid.setRowRenderer(new FormatItemGajiRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) (searchparent == null ? null : searchparent.getAttribute("satuanKerja"));
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(FormatItemGaji.class)

				.add(Restrictions.or(
						!Common.getApakahAdmin() ? Restrictions.sqlRestriction("false")
								: Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)))
				.add(searchcabang.getSelectedItem() == null || searchcabang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("cabang", searchcabang.getSelectedItem().getValue()))
				.add(searchdepartemen.getSelectedItem() == null || searchdepartemen.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("departemen", searchdepartemen.getSelectedItem().getValue()))
				.add(searchlevelJabatan.getSelectedItem() == null
						|| searchlevelJabatan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("levelJabatan", searchlevelJabatan.getSelectedItem().getValue()));
		return criteria;
	}

	public Boolean checkNamaFormatItemGaji() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(FormatItemGaji.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.formatItemGaji.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.formatItemGaji.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
