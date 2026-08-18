package ais.action.master;

import java.math.BigDecimal;
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
import org.zkoss.zul.Checkbox;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DiskonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisDiskonMahasiswa;
import ais.database.model.Mahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DiskonMahasiswaAction extends GenericAutowireComposer
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

	private AmbilDataMahasiswaBanbox mahasiswa;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private DiskonMahasiswa diskonMahasiswa;
	private MyToolbarbuttonConfig add;
	private Combobox jenisDiskonMahasiswa;
	private Combobox itemBiaya;
	private Decimalbox semesterMulai;
	private Decimalbox semesterSampai;
	private Combobox itemBiaya2;
	private Combobox itemBiaya3;
	private Combobox itemBiaya4;
	private Combobox itemBiaya5;

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

		String[] contents = new String[] { "id", "mahasiswa", "jenisDiskonMahasiswa", "itemBiaya", "semesterMulai",
				"semesterSampai", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(DiskonMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, DiskonMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class DiskonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DiskonMahasiswa diskonMahasiswa = (DiskonMahasiswa) arg1;

			if (diskonMahasiswa.getMahasiswa() != null) {
				new Label(diskonMahasiswa.getMahasiswa() != null ? diskonMahasiswa.getMahasiswa().getNim() : "")
						.setParent(arg0);
				RevisiHelper
						.createNewRevisi(DiskonMahasiswa.class, diskonMahasiswa,
								diskonMahasiswa.getMahasiswa() != null ? diskonMahasiswa.getMahasiswa().getNama() : "")
						.setParent(arg0);
			} else if (diskonMahasiswa.getBiodataCalonMahasiswa() != null) {
				new Label(diskonMahasiswa.getBiodataCalonMahasiswa() != null
						? diskonMahasiswa.getBiodataCalonMahasiswa().getNoRegistrasi()
						: "").setParent(arg0);
				RevisiHelper.createNewRevisi(DiskonMahasiswa.class, diskonMahasiswa,
						diskonMahasiswa.getBiodataCalonMahasiswa() != null
								? diskonMahasiswa.getBiodataCalonMahasiswa().getNama()
								: "")
						.setParent(arg0);
			}
			new Label(diskonMahasiswa.getJenisDiskonMahasiswa().getNama()).setParent(arg0);
			String item = "";
			for (ItemBiaya itemBiaya : diskonMahasiswa.ambilItemBiayas()) {
				item += item.isEmpty() ? itemBiaya.getNama() : ", " + itemBiaya.getNama();
			}

			new Label(item).setParent(arg0);
			new Label(diskonMahasiswa.getSemesterMulai() + " sd " + diskonMahasiswa.getSemesterSampai())
					.setParent(arg0);
			new Label(diskonMahasiswa.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(diskonMahasiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					diskonMahasiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(diskonMahasiswa);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, diskonMahasiswa, DiskonMahasiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new DiskonMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		diskonMahasiswa = (DiskonMahasiswa) obj;
		init(diskonMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final DiskonMahasiswa diskonMahasiswa) throws Exception {
		this.diskonMahasiswa = diskonMahasiswa;
		addWindow.setTitle(diskonMahasiswa.getId() == null ? "Tambah Diskon" : "Ubah Diskon");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa *"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setAttribute("mahasiswa", diskonMahasiswa.getMahasiswa());
		mahasiswa.setValue(diskonMahasiswa.getMahasiswa() == null ? "" : diskonMahasiswa.getMahasiswa().getNama());
		mahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Diskon *"));
		row.appendChild(jenisDiskonMahasiswa = new Combobox());
		Common.insertCombo(jenisDiskonMahasiswa, "nama", "keterangan", JenisDiskonMahasiswa.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisDiskonMahasiswa, diskonMahasiswa.getJenisDiskonMahasiswa());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya *"));
		row.appendChild(itemBiaya = new Combobox());
		Common.insertCombo(itemBiaya, "nama", "deskripsi", ItemBiaya.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(itemBiaya, diskonMahasiswa.getItemBiaya());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya II"));
		row.appendChild(itemBiaya2 = new Combobox());
		Common.insertComboDanSemua(itemBiaya2, new String[] { "nama" }, "deskripsi", ItemBiaya.class,
				"=Item Biaya Ditentukan Nanti=", Restrictions.eq("aktif", true));
		Common.selectComboItem(itemBiaya2, diskonMahasiswa.getItemBiaya2());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya III"));
		row.appendChild(itemBiaya3 = new Combobox());
		Common.insertComboDanSemua(itemBiaya3, new String[] { "nama" }, "deskripsi", ItemBiaya.class,
				"=Item Biaya Ditentukan Nanti=", Restrictions.eq("aktif", true));
		Common.selectComboItem(itemBiaya3, diskonMahasiswa.getItemBiaya3());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya IV"));
		row.appendChild(itemBiaya4 = new Combobox());
		Common.insertComboDanSemua(itemBiaya4, new String[] { "nama" }, "deskripsi", ItemBiaya.class,
				"=Item Biaya Ditentukan Nanti=", Restrictions.eq("aktif", true));
		Common.selectComboItem(itemBiaya4, diskonMahasiswa.getItemBiaya4());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya V"));
		row.appendChild(itemBiaya5 = new Combobox());
		Common.insertComboDanSemua(itemBiaya5, new String[] { "nama" }, "deskripsi", ItemBiaya.class,
				"=Item Biaya Ditentukan Nanti=", Restrictions.eq("aktif", true));
		Common.selectComboItem(itemBiaya5, diskonMahasiswa.getItemBiaya5());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		semesterMulai = new Decimalbox(new BigDecimal(diskonMahasiswa.getSemesterMulai()));
		semesterMulai.setCols(3);
		semesterSampai = new Decimalbox(new BigDecimal(diskonMahasiswa.getSemesterSampai()));
		semesterSampai.setCols(3);
		hbox.appendChild(semesterMulai);
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(semesterSampai);

		EventListener jenisDiskonMahasiswaEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				itemBiaya.setDisabled(false);
				itemBiaya2.setDisabled(false);
				itemBiaya3.setDisabled(false);
				itemBiaya4.setDisabled(false);
				itemBiaya5.setDisabled(false);

				semesterMulai.setDisabled(false);
				semesterSampai.setDisabled(false);
				Common.selectComboItem(itemBiaya, diskonMahasiswa.getItemBiaya());
				JenisDiskonMahasiswa jdm = (JenisDiskonMahasiswa) (jenisDiskonMahasiswa.getSelectedItem() == null ? null
						: jenisDiskonMahasiswa.getSelectedItem().getValue());
				if (jdm != null && jdm.getItemBiaya() != null) {
					Common.selectComboItem(true, itemBiaya, jdm.getItemBiaya());
					itemBiaya.setDisabled(true);
				}

				if (jdm != null && jdm.getItemBiaya2() != null) {
					Common.selectComboItem(true, itemBiaya2, jdm.getItemBiaya2());
					itemBiaya2.setDisabled(true);
				}

				if (jdm != null && jdm.getItemBiaya3() != null) {
					Common.selectComboItem(true, itemBiaya3, jdm.getItemBiaya3());
					itemBiaya3.setDisabled(true);
				}

				if (jdm != null && jdm.getItemBiaya4() != null) {
					Common.selectComboItem(true, itemBiaya4, jdm.getItemBiaya4());
					itemBiaya4.setDisabled(true);
				}

				if (jdm != null && jdm.getItemBiaya5() != null) {
					Common.selectComboItem(true, itemBiaya2, jdm.getItemBiaya5());
					itemBiaya5.setDisabled(true);
				}

				if (jdm != null && jdm.getSemesterMulai() != null) {
					semesterMulai.setValue(new BigDecimal(jdm.getSemesterMulai()));
					semesterMulai.setDisabled(true);
				}

				if (jdm != null && jdm.getSemesterSampai() != null) {
					semesterSampai.setValue(new BigDecimal(jdm.getSemesterSampai()));
					semesterSampai.setDisabled(true);
				}

			}
		};

		jenisDiskonMahasiswa.addEventListener("onChange", jenisDiskonMahasiswaEventListener);
		jenisDiskonMahasiswaEventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(diskonMahasiswa.getKeterangan()));
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
		if (mahasiswa.getAttribute("mahasiswa") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Mahasiswa",
					"Kolom Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisDiskonMahasiswa.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Diskon",
					"Kolom Jenis Diskon belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Diskon.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (itemBiaya.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Item Biaya",
					"Kolom Item Biaya belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Item Biaya.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		Session session = HibernateUtil.currentSession();
		if (diskonMahasiswa.getId() != null) {
			diskonMahasiswa = (DiskonMahasiswa) session.load(DiskonMahasiswa.class, diskonMahasiswa.getId());

		}
		diskonMahasiswa.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		diskonMahasiswa.setItemBiaya((ItemBiaya) itemBiaya.getSelectedItem().getValue());

		diskonMahasiswa.setItemBiaya2(
				(ItemBiaya) (itemBiaya2.getSelectedItem() == null ? null : itemBiaya2.getSelectedItem().getValue()));

		diskonMahasiswa.setItemBiaya3(
				(ItemBiaya) (itemBiaya3.getSelectedItem() == null ? null : itemBiaya3.getSelectedItem().getValue()));

		diskonMahasiswa.setItemBiaya4(
				(ItemBiaya) (itemBiaya4.getSelectedItem() == null ? null : itemBiaya4.getSelectedItem().getValue()));

		diskonMahasiswa.setItemBiaya5(
				(ItemBiaya) (itemBiaya5.getSelectedItem() == null ? null : itemBiaya5.getSelectedItem().getValue()));

		diskonMahasiswa
				.setJenisDiskonMahasiswa((JenisDiskonMahasiswa) jenisDiskonMahasiswa.getSelectedItem().getValue());
		diskonMahasiswa.setKeterangan(keterangan.getValue());
		diskonMahasiswa.setSemesterMulai(semesterMulai.getValue() == null ? null : semesterMulai.getValue().intValue());
		diskonMahasiswa
				.setSemesterSampai(semesterSampai.getValue() == null ? null : semesterSampai.getValue().intValue());

		Common.refreshSaveOrUpdate(session, diskonMahasiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DiskonMahasiswa.class).add(Restrictions.isNotNull("mahasiswa"))
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa", Criteria.LEFT_JOIN)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") :

				Restrictions.or(
						Restrictions.ilike("biodataCalonMahasiswa.nama", searchnama.getValue().trim(),
								MatchMode.ANYWHERE),
						Restrictions.or(
								Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi", searchnama.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("mahasiswa.nama", searchnama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(),
												MatchMode.ANYWHERE))))

		);
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DiskonMahasiswa> diskonMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(diskonMahasiswa);
		grid.setRowRenderer(new DiskonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
