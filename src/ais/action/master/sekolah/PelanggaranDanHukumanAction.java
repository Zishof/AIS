package ais.action.master.sekolah;


import ais.action.master.pelanggaran.DasbordPelanggaran;
import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.zkoss.zul.Column;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Hukuman;
import ais.database.model.sekolah.Pelanggaran;
import ais.database.model.sekolah.PelanggaranDanHukuman;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PelanggaranDanHukumanAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Tabpanel tabDasbor;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Textbox nama;
	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PelanggaranDanHukuman pelanggaranDanHukuman;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Set<Pelanggaran> selectedPelanggaran;
	private Set<Hukuman> selectedHukuman;

	private Hbox labelYayasanSekolah;
	private Hbox nilaiYayasanSekolah;
	private MyColumnConfig colSekolah;
	private boolean ya;
	private PerguruanTinggi perguruanTinggi = null;

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

		Sekolah sekolah1 = SekolahUtil.getSekolah();
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		ya = (Common.bolehKonfigurasi("apakah_aktifkan_modul_pesantren", Konfigurasi.TIDAK_AKTIF)
				|| Common.bolehKonfigurasi("apakah_aktifkan_modul_sekolah", Konfigurasi.TIDAK_AKTIF)
				|| sekolah1.getId() != null);
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.getMahasiswa() != null || tbmuser.getDosen() != null) {
			ya = false;
		} else if (tbmuser.getSiswa() != null || tbmuser.getGuru() != null) {
			ya = true;
		}

		if (labelYayasanSekolah != null) { labelYayasanSekolah.setVisible(ya); }
		if (nilaiYayasanSekolah != null) { nilaiYayasanSekolah.setVisible(ya); }
		if (colSekolah != null) { colSekolah.setVisible(ya); }

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

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

		String[] contents = new String[] { "id", "nama", "sekolah", "perguruanTinggi", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PelanggaranDanHukuman.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
		onDasbor(null);
	}

	public void onDasbor(org.zkoss.zk.ui.event.Event event) {
		if (tabDasbor.getChildren().size() == 0) {
			DasbordPelanggaran dasbord = new DasbordPelanggaran(DasbordPelanggaran.Lingkup.SEMUA);
			ais.ui.util.BaseDasbordPortal.mountWrapped(dasbord, tabDasbor,
				"Pelanggaran & Hukuman",
				"Ringkasan pelanggaran dan sanksi yang diberikan.");
		}
	}

	class PelanggaranDanHukumanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PelanggaranDanHukuman pelanggaranDanHukuman = (PelanggaranDanHukuman) arg1;

			RevisiHelper.createNewRevisi(PelanggaranDanHukuman.class, pelanggaranDanHukuman,
					pelanggaranDanHukuman.getNama()).setParent(arg0);
			new Label(pelanggaranDanHukuman.getSekolah() == null ? "" : pelanggaranDanHukuman.getSekolah().getNama())
					.setParent(arg0);
			new Label(pelanggaranDanHukuman.getKeterangan()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (Pelanggaran pelanggaran : new TreeSet<Pelanggaran>(pelanggaranDanHukuman.getPelanggarans())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + pelanggaran.getNama()));
				i++;
			}

			vbox = new Vbox();
			vbox.setParent(arg0);
			i = 1;
			for (Hukuman hukuman : new TreeSet<Hukuman>(pelanggaranDanHukuman.getHukumans())) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + hukuman.getNama()));
				i++;
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(pelanggaranDanHukuman.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pelanggaranDanHukuman.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pelanggaranDanHukuman);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, pelanggaranDanHukuman, PelanggaranDanHukumanAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PelanggaranDanHukuman());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pelanggaranDanHukuman = (PelanggaranDanHukuman) obj;
		init(pelanggaranDanHukuman);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation" })
	private void init(PelanggaranDanHukuman pelanggaranDanHukuman) {
		this.pelanggaranDanHukuman = pelanggaranDanHukuman;
		addWindow.setTitle(pelanggaranDanHukuman.getId() == null ? "Tambah Jenis Pelanggaran Dan Hukuman" : "Ubah Jenis Pelanggaran Dan Hukuman");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pelanggaran Dan Hukuman *"));
		row.appendChild(nama = new Textbox(pelanggaranDanHukuman.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		yayasan = new Combobox();
		sekolah = new Combobox();
		if (ya) {
			Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
			row.appendChild(yayasan);
			Common.selectComboItem(yayasan, pelanggaranDanHukuman.getYayasan());
			yayasan.setWidth("90%");
			yayasan.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
			row.appendChild(sekolah);
			Common.pilihSekolah(sekolah, pelanggaranDanHukuman.getSekolah());
			sekolah.setWidth("90%");
			sekolah.setReadonly(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pelanggaranDanHukuman.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Pelanggaran"));

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		List<Pelanggaran> pelanggarans = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(Pelanggaran.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				Pelanggaran.class);

		if (pelanggaranDanHukuman.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pelanggaranDanHukuman);
		}
		selectedPelanggaran = this.pelanggaranDanHukuman.getPelanggarans();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final Pelanggaran pelanggaran : pelanggarans) {
			final Checkbox checkbox = new Checkbox(pelanggaran.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedPelanggaran.contains(pelanggaran));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedPelanggaran.add(pelanggaran);
					} else {
						selectedPelanggaran.remove(pelanggaran);
					}
				}
			});
		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		subGrid = new MyGrid();
		row.appendChild(subGrid);

		subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Hukuman"));

		subRows = new Rows();
		subRows.setParent(subGrid);

		subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		List<Hukuman> hukumans = ConstantValues.simpleList(HibernateUtil.currentSession().createCriteria(Hukuman.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), Hukuman.class);

		if (pelanggaranDanHukuman.getId() != null) {
			HibernateUtil.currentSession().refresh(this.pelanggaranDanHukuman);
		}
		selectedHukuman = this.pelanggaranDanHukuman.getHukumans();

		vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final Hukuman hukuman : hukumans) {
			final Checkbox checkbox = new Checkbox(hukuman.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedHukuman.contains(hukuman));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedHukuman.add(hukuman);
					} else {
						selectedHukuman.remove(hukuman);
					}
				}
			});
		}

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
			MyMessageboxConfig.show("Jenis Pelanggaran Dan Hukuman harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pelanggaranDanHukuman.getId() != null) {
			pelanggaranDanHukuman = (PelanggaranDanHukuman) session.load(PelanggaranDanHukuman.class,
					pelanggaranDanHukuman.getId());

		}

		pelanggaranDanHukuman.setNama(nama.getValue());
		pelanggaranDanHukuman.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));
		pelanggaranDanHukuman.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		
		pelanggaranDanHukuman.setKeterangan(keterangan.getValue());
		pelanggaranDanHukuman.setPelanggarans(selectedPelanggaran);
		pelanggaranDanHukuman.setHukumans(selectedHukuman);
		pelanggaranDanHukuman.setPerguruanTinggi(perguruanTinggi);

		Common.refreshSaveOrUpdate(session, pelanggaranDanHukuman);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PelanggaranDanHukuman.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

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

		List<PelanggaranDanHukuman> pelanggaranDanHukuman = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pelanggaranDanHukuman);
		grid.setRowRenderer(new PelanggaranDanHukumanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
