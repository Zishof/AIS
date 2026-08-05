package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.HashMap;
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
import ais.ui.util.MyInclude;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JenisPenilaian;
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

public class JenisPenilaianAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Checkbox searchaktif;

	private Textbox nama;
	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisPenilaian jenisPenilaian;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;

	private Tabpanel itemPenilaianTab;
	private HashMap<Long, DetailJenisPenilaian> selectedJenisItemPenilaianSiswa;

	public void onItemPenilaian(Event event) {
		if (itemPenilaianTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(itemPenilaianTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/jenis_item_penilaian_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel nilaiHurufTab;

	public void onNilaiHuruf(Event event) {
		if (nilaiHurufTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(nilaiHurufTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/nilai_huruf_sekolah.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel jenisNilaiHurufTab;

	public void onJenisNilaiHuruf(Event event) {
		if (jenisNilaiHurufTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisNilaiHurufTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/jenis_nilai_huruf.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel grupKategoriTab;

	public void onGrupKategori(Event event) {
		if (grupKategoriTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(grupKategoriTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/grup_kategori_item_penilaian_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel grupPenilaianTab;

	public void onGrupPenilaian(Event event) {
		if (grupPenilaianTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(grupPenilaianTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/grup_penilaian.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel kategoriPenilaianTab;

	public void onKategoriPenilaian(Event event) {
		if (kategoriPenilaianTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kategoriPenilaianTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/kategori_item_penilaian_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel konstanta;
	private Row rowJp;

	public void onKonstanta(Event event) {
		if (konstanta.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(konstanta);
			MyInclude iframe = new MyInclude("/pages/master/konstanta.zul");
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

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "jenis", "sekolah", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPenilaian.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class JenisPenilaianRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisPenilaian jenisPenilaian = (JenisPenilaian) arg1;

			RevisiHelper.createNewRevisi(JenisPenilaian.class, jenisPenilaian, jenisPenilaian.getJenis())
					.setParent(arg0);
			new Label(jenisPenilaian.getSekolah() == null ? "" : jenisPenilaian.getSekolah().getNama()).setParent(arg0);
			new Label(jenisPenilaian.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPenilaian.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPenilaian.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPenilaian);
				}
			});

			Session session = HibernateUtil.currentSession();

			List<DetailJenisPenilaian> selectedJenisItemPenilaianSiswa = ConstantValues.simpleList(session
					.createCriteria(DetailJenisPenilaian.class).createAlias("grupPenilaian", "grupPenilaian")
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("jenisPenilaian", jenisPenilaian)).addOrder(Order.asc("grupPenilaian.nama")),
					DetailJenisPenilaian.class);
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;

			for (DetailJenisPenilaian jenisItemPenilaianSiswa : selectedJenisItemPenilaianSiswa) {
				vbox.appendChild(new MyLabelKecil(i + ". " + jenisItemPenilaianSiswa.getGrupPenilaian().getNama()));
				i++;
			}
			selectedJenisItemPenilaianSiswa = null;

			Common.copyEditDeleteButtons(edit, delete, jenisPenilaian, JenisPenilaianAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisPenilaian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPenilaian = (JenisPenilaian) obj;
		init(jenisPenilaian);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(final JenisPenilaian jenisPenilaian) throws Exception {
		this.jenisPenilaian = jenisPenilaian;
		addWindow.setTitle(jenisPenilaian.getId() == null ? "Tambah Jenis Penilaian" : "Ubah Jenis Penilaian");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Penilaian *"));
		row.appendChild(nama = new Textbox(jenisPenilaian.getJenis()));
		nama.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, jenisPenilaian.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, jenisPenilaian.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPenilaian.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		selectedJenisItemPenilaianSiswa = new HashMap<Long, DetailJenisPenilaian>();

		rowJp = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowJp, "2");
		rowJp.setParent(rows);

		EventListener ubahJenisPenialain = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Yayasan y = (Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue());
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(GrupPenilaian.class)

								.add(s == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("sekolah"),
												Restrictions.eq("sekolah", s)))

								.add(y == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(Restrictions.isNull("yayasan"),
												Restrictions.eq("yayasan", y)))

								.addOrder(Order.asc("nama"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						GrupPenilaian.class);

				if (jenisPenilaian.getId() != null) {
					HibernateUtil.currentSession().refresh(jenisPenilaian);
				}

				if (jenisPenilaian.getId() != null) {
					Session session = HibernateUtil.currentSession();
					List<DetailJenisPenilaian> detailJenisPenilaians = ConstantValues
							.simpleList(
									session.createCriteria(DetailJenisPenilaian.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("jenisPenilaian", jenisPenilaian)),
									DetailJenisPenilaian.class);

					selectedJenisItemPenilaianSiswa.clear();
					for (DetailJenisPenilaian detailJenisPenilaian : detailJenisPenilaians) {
						if (!selectedJenisItemPenilaianSiswa
								.containsKey(detailJenisPenilaian.getGrupPenilaian().getId())) {
							selectedJenisItemPenilaianSiswa.put(detailJenisPenilaian.getGrupPenilaian().getId(),
									detailJenisPenilaian);
						}
					}

				} else {
					selectedJenisItemPenilaianSiswa.clear();
				}

				Common.clear(rowJp);
				MyGrid vboxSkala = new MyGrid();
				vboxSkala.setParent(rowJp);

				Columns columns = new Columns();
				columns.setParent(vboxSkala);

				MyColumnConfig column = new MyColumnConfig("Pilih Grup Penilaian");
				column.setParent(columns);

				Rows rowsSkala = new Rows();
				rowsSkala.setParent(vboxSkala);

				KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa = new KategoriItemPenilaianSiswa();
				kategoriItemPenilaianSiswa.setId(-1L);

				for (final GrupPenilaian grupPenilaian : grupPenilaians) {

					MyFormRow rowSkala = new MyFormRow();
					rowSkala.setStyle("border:0px;background: transparent;");
					rowSkala.setParent(rowsSkala);

					DetailJenisPenilaian detailJenisPenilaianTemp = selectedJenisItemPenilaianSiswa
							.get(grupPenilaian.getId());
					if (detailJenisPenilaianTemp == null) {
						detailJenisPenilaianTemp = new DetailJenisPenilaian();
					}
					detailJenisPenilaianTemp.setGrupPenilaian(grupPenilaian);
					final DetailJenisPenilaian detailJenisPenilaian = detailJenisPenilaianTemp;

					final Checkbox checkbox = new Checkbox(grupPenilaian.getNama());
					checkbox.setAttribute("grupPenilaian", grupPenilaian);
					checkbox.setParent(rowSkala);
					checkbox.setChecked(selectedJenisItemPenilaianSiswa.containsKey(grupPenilaian.getId()));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (checkbox.isChecked()) {
								selectedJenisItemPenilaianSiswa.put(grupPenilaian.getId(), detailJenisPenilaian);
							} else {
								selectedJenisItemPenilaianSiswa.remove(grupPenilaian.getId());
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
			MyMessageboxConfig.show("Nama Jenis Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
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
		if (jenisPenilaian.getId() != null) {
			jenisPenilaian = (JenisPenilaian) session.load(JenisPenilaian.class, jenisPenilaian.getId());
		}

		jenisPenilaian.setJenis(nama.getValue());
		jenisPenilaian.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		jenisPenilaian.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		jenisPenilaian.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisPenilaian);
		session.flush();

		List<DetailJenisPenilaian> d = ConstantValues.simpleList(session.createCriteria(DetailJenisPenilaian.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("jenisPenilaian", jenisPenilaian)), DetailJenisPenilaian.class);
		for (DetailJenisPenilaian detailJenisPenilaian : d) {
			detailJenisPenilaian.setAktif(false);
			Common.refreshSaveOrUpdate(session, detailJenisPenilaian);
			session.flush();
		}

		if (selectedJenisItemPenilaianSiswa != null) {
			for (DetailJenisPenilaian detailJenisPenilaian : selectedJenisItemPenilaianSiswa.values()) {
				detailJenisPenilaian.setAktif(true);
				detailJenisPenilaian.setJenisPenilaian(jenisPenilaian);
				Common.refreshSaveOrUpdate(session, detailJenisPenilaian);
				session.flush();
			}
		}
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPenilaian.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("jenis"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("jenis", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPenilaian> jenisPenilaian = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPenilaian);
		grid.setRowRenderer(new JenisPenilaianRenderer());
		grid.setModelCheckMobile(strset);

	}

}
