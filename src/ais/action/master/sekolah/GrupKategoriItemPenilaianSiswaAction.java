package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class GrupKategoriItemPenilaianSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Checkbox searchaktif;

	private Textbox nama;
	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Row rowJp;
	private HashMap<Long, DetailGrupKategoriItemPenilaianSiswa> selectedKategoriItemPenilaianSiswa;
	private JSONArray array;
	private Textbox kode;

	private Combobox khususTingkat;
	private Combobox khususSemester;

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

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "sekolah", "formula", "keterangan", "khususTingkat",
				"khususSemester", "nilaiBolehDinputOlehGuru", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, GrupKategoriItemPenilaianSiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class GrupKategoriItemPenilaianSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa = (GrupKategoriItemPenilaianSiswa) arg1;
			new Label(grupKategoriItemPenilaianSiswa.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(GrupKategoriItemPenilaianSiswa.class, grupKategoriItemPenilaianSiswa,
					grupKategoriItemPenilaianSiswa.getNama()).setParent(arg0);
			new Label(grupKategoriItemPenilaianSiswa.getSekolah() == null ? ""
					: grupKategoriItemPenilaianSiswa.getSekolah().getNama()).setParent(arg0);

			Session session = HibernateUtil.currentSession();

			List<DetailGrupKategoriItemPenilaianSiswa> selectedJenisItemPenilaianSiswa = ConstantValues
					.simpleList(
							session.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)
									.createAlias("kategoriItemPenilaianSiswa", "kategoriItemPenilaianSiswa")
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("grupKategoriItemPenilaianSiswa",
											grupKategoriItemPenilaianSiswa))
									.addOrder(Order.asc("kategoriItemPenilaianSiswa.nama")),
							DetailGrupKategoriItemPenilaianSiswa.class);
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;

			for (DetailGrupKategoriItemPenilaianSiswa detailGrupKategoriItemPenilaianSiswa : selectedJenisItemPenilaianSiswa) {
				vbox.appendChild(new MyLabelKecil(
						i + ". " + detailGrupKategoriItemPenilaianSiswa.getKategoriItemPenilaianSiswa().getNama()));
				i++;
			}
			selectedJenisItemPenilaianSiswa = null;

			new Label(grupKategoriItemPenilaianSiswa.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(grupKategoriItemPenilaianSiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					grupKategoriItemPenilaianSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(grupKategoriItemPenilaianSiswa);
				}
			});

			final MyCheckboxConfig nilaiBolehDinputOlehGuru = new MyCheckboxConfig("Boleh Diinput Guru");
			nilaiBolehDinputOlehGuru.setDisabled(!edit);
			nilaiBolehDinputOlehGuru.setChecked(grupKategoriItemPenilaianSiswa.getNilaiBolehDinputOlehGuru());
			nilaiBolehDinputOlehGuru.setParent(arg0);
			nilaiBolehDinputOlehGuru.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					grupKategoriItemPenilaianSiswa.setNilaiBolehDinputOlehGuru(nilaiBolehDinputOlehGuru.isChecked());
					Common.refreshSaveOrUpdate(grupKategoriItemPenilaianSiswa);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, grupKategoriItemPenilaianSiswa,
					GrupKategoriItemPenilaianSiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new GrupKategoriItemPenilaianSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		grupKategoriItemPenilaianSiswa = (GrupKategoriItemPenilaianSiswa) obj;
		init(grupKategoriItemPenilaianSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa) throws Exception {
		this.grupKategoriItemPenilaianSiswa = grupKategoriItemPenilaianSiswa;
		addWindow.setTitle(grupKategoriItemPenilaianSiswa.getId() == null ? "Tambah Grup Kategori Penilaian" : "Ubah Grup Kategori Penilaian");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Grup Kategori Penilaian *"));
		row.appendChild(kode = new Textbox(grupKategoriItemPenilaianSiswa.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Grup Kategori Penilaian *"));
		row.appendChild(nama = new Textbox(grupKategoriItemPenilaianSiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus buat tingkat"));
		row.appendChild(khususTingkat = new Combobox());
		khususTingkat.setWidth("90%");

		Comboitem comboitem = new Comboitem("Semua Tingkat");
		comboitem.setValue(null);
		khususTingkat.appendChild(comboitem);

		for (int i = 1; i <= 12; i++) {
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			khususTingkat.appendChild(comboitem);
		}

		Common.selectComboItem(khususTingkat, grupKategoriItemPenilaianSiswa.getKhususTingkat());
		khususTingkat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus buat Semester"));
		row.appendChild(khususSemester = new Combobox());
		khususSemester.setWidth("90%");

		comboitem = new Comboitem("Semua Semester");
		comboitem.setValue(null);
		khususSemester.appendChild(comboitem);

		for (int i = 1; i <= 2; i++) {
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			khususSemester.appendChild(comboitem);
		}

		Common.selectComboItem(khususSemester, grupKategoriItemPenilaianSiswa.getKhususSemester());
		khususSemester.setReadonly(true);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, grupKategoriItemPenilaianSiswa.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, grupKategoriItemPenilaianSiswa.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(grupKategoriItemPenilaianSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		MyFormRow rowFormula = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowFormula, "2");
		rowFormula.setParent(rows);
		rowFormula.appendChild(new ais.ui.util.MyLabelConfig("Formula"));

		MyFormRow rowIsianFormula = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowIsianFormula, "2");
		rowIsianFormula.setParent(rows);
		array = new JSONArray(grupKategoriItemPenilaianSiswa.getFormula());
		rowFormula = Common.tampilanScroll1(rowIsianFormula);
		HashMap<Long, DetailGrupPenilaian> selectedGrupKategoriItemPenilaianSiswa = null;
		ArrayList<EventListener> eventListeners = null;
		GrupPenilaianAction.reloadFormula(rowFormula, null, array, null, selectedGrupKategoriItemPenilaianSiswa,
				eventListeners);

		selectedKategoriItemPenilaianSiswa = new HashMap<Long, DetailGrupKategoriItemPenilaianSiswa>();
		rowJp = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowJp, "2");
		rowJp.setParent(rows);

		EventListener ubahJenisPenialain = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Yayasan y = (Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue());
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				List<KategoriItemPenilaianSiswa> kategoriItemPenilaianSiswas = ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(KategoriItemPenilaianSiswa.class)

								.add(s == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("sekolah"),
												Restrictions.eq("sekolah", s)))

								.add(y == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("yayasan"),
												Restrictions.eq("yayasan", y)))

								.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						KategoriItemPenilaianSiswa.class);

				if (grupKategoriItemPenilaianSiswa.getId() != null) {
					HibernateUtil.currentSession().refresh(grupKategoriItemPenilaianSiswa);
				}

				if (grupKategoriItemPenilaianSiswa.getId() != null) {
					Session session = HibernateUtil.currentSession();
					List<DetailGrupKategoriItemPenilaianSiswa> detailGrupKategoriItemPenilaianSiswas = ConstantValues
							.simpleList(
									session.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.createAlias("kategoriItemPenilaianSiswa", "kategoriItemPenilaianSiswa")
											.add(Restrictions.or(
													Restrictions.isNull("kategoriItemPenilaianSiswa.aktif"),
													Restrictions.eq("kategoriItemPenilaianSiswa.aktif", true)))
											.add(Restrictions.eq("grupKategoriItemPenilaianSiswa",
													grupKategoriItemPenilaianSiswa)),
									DetailGrupKategoriItemPenilaianSiswa.class);

					selectedKategoriItemPenilaianSiswa.clear();
					for (DetailGrupKategoriItemPenilaianSiswa detailGrupKategoriItemPenilaianSiswa : detailGrupKategoriItemPenilaianSiswas) {
						if (!selectedKategoriItemPenilaianSiswa.containsKey(
								detailGrupKategoriItemPenilaianSiswa.getKategoriItemPenilaianSiswa().getId())) {
							selectedKategoriItemPenilaianSiswa.put(
									detailGrupKategoriItemPenilaianSiswa.getKategoriItemPenilaianSiswa().getId(),
									detailGrupKategoriItemPenilaianSiswa);
						}
					}

				} else {
					selectedKategoriItemPenilaianSiswa.clear();
				}

				Common.clear(rowJp);
				MyGrid vboxSkala = new MyGrid();
				vboxSkala.setParent(rowJp);

				Columns columns = new Columns();
				columns.setParent(vboxSkala);

				MyColumnConfig column = new MyColumnConfig("Pilih Kategori Penilaian");
				column.setParent(columns);

				Rows rowsSkala = new Rows();
				rowsSkala.setParent(vboxSkala);

				for (final KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa : kategoriItemPenilaianSiswas) {

					MyFormRow rowSkala = new MyFormRow();
					rowSkala.setStyle("border:0px;background: transparent;");
					rowSkala.setParent(rowsSkala);

					Hbox vbox = new Hbox();
					rowSkala.appendChild(vbox);

					DetailGrupKategoriItemPenilaianSiswa detailGrupKategoriItemPenilaianSiswaTemp = selectedKategoriItemPenilaianSiswa
							.get(kategoriItemPenilaianSiswa.getId());
					if (detailGrupKategoriItemPenilaianSiswaTemp == null) {
						detailGrupKategoriItemPenilaianSiswaTemp = new DetailGrupKategoriItemPenilaianSiswa();
					}
					detailGrupKategoriItemPenilaianSiswaTemp.setKategoriItemPenilaianSiswa(kategoriItemPenilaianSiswa);
					final DetailGrupKategoriItemPenilaianSiswa detailGrupKategoriItemPenilaianSiswa = detailGrupKategoriItemPenilaianSiswaTemp;

					final Checkbox checkbox = new Checkbox(
							kategoriItemPenilaianSiswa.getKode() + " - " + kategoriItemPenilaianSiswa.getNama());
					checkbox.setAttribute("detailGrupKategoriItemPenilaianSiswa", detailGrupKategoriItemPenilaianSiswa);
					checkbox.setParent(vbox);
					checkbox.setChecked(
							selectedKategoriItemPenilaianSiswa.containsKey(kategoriItemPenilaianSiswa.getId()));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (checkbox.isChecked()) {
								selectedKategoriItemPenilaianSiswa.put(kategoriItemPenilaianSiswa.getId(),
										detailGrupKategoriItemPenilaianSiswa);
							} else {
								selectedKategoriItemPenilaianSiswa.remove(kategoriItemPenilaianSiswa.getId());
							}

						}
					});

				}
			}
		};

		yayasan.addEventListener("onChange", ubahJenisPenialain);
		sekolah.addEventListener("onChange", ubahJenisPenialain);

		Common.createDefaultTimer(ubahJenisPenialain);

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

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Grup Kategori Penilaian harus diisi", "Peringatan", MyMessageboxConfig.OK,
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
		if (grupKategoriItemPenilaianSiswa.getId() != null) {
			grupKategoriItemPenilaianSiswa = (GrupKategoriItemPenilaianSiswa) session
					.load(GrupKategoriItemPenilaianSiswa.class, grupKategoriItemPenilaianSiswa.getId());

		}

		grupKategoriItemPenilaianSiswa.setKode(kode.getValue());
		grupKategoriItemPenilaianSiswa.setNama(nama.getValue());
		grupKategoriItemPenilaianSiswa.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		grupKategoriItemPenilaianSiswa.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		grupKategoriItemPenilaianSiswa.setKeterangan(keterangan.getValue());
		grupKategoriItemPenilaianSiswa.setFormula(array.toString());

		grupKategoriItemPenilaianSiswa.setKhususSemester((Integer) (khususSemester.getSelectedItem() == null ? null
				: khususSemester.getSelectedItem().getValue()));
		grupKategoriItemPenilaianSiswa.setKhususTingkat((Integer) (khususTingkat.getSelectedItem() == null ? null
				: khususTingkat.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, grupKategoriItemPenilaianSiswa);

		List<DetailGrupKategoriItemPenilaianSiswa> d = ConstantValues.simpleList(
				session.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)
						.add(Restrictions.eq("grupKategoriItemPenilaianSiswa", grupKategoriItemPenilaianSiswa)),
				DetailGrupKategoriItemPenilaianSiswa.class);
		for (DetailGrupKategoriItemPenilaianSiswa detailGrupKategoriItemPenilaianSiswa : d) {
			detailGrupKategoriItemPenilaianSiswa.setAktif(false);
			Common.refreshSaveOrUpdate(session, detailGrupKategoriItemPenilaianSiswa);
			session.flush();
		}

		if (selectedKategoriItemPenilaianSiswa != null) {
			for (DetailGrupKategoriItemPenilaianSiswa detailGrupKategoriItemPenilaianSiswa : selectedKategoriItemPenilaianSiswa
					.values()) {

				detailGrupKategoriItemPenilaianSiswa.setAktif(true);
				detailGrupKategoriItemPenilaianSiswa.setGrupKategoriItemPenilaianSiswa(grupKategoriItemPenilaianSiswa);
				Common.refreshSaveOrUpdate(session, detailGrupKategoriItemPenilaianSiswa);
				session.flush();
			}
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GrupKategoriItemPenilaianSiswa.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")

				: Restrictions.or(Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(grupKategoriItemPenilaianSiswa);
		grid.setRowRenderer(new GrupKategoriItemPenilaianSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
