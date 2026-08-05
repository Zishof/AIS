package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import ais.action.master.sekolah.helper.AktifitasPembelajaranHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KelompokMatapelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class MatapelajaranAction extends GenericAutowireComposer
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

	private Textbox kode;
	private Textbox nama;
	private Textbox singkatan;
	private Combobox sekolah;
	private Combobox kelompokMatapelajaran;

	private Combobox jenisPenilaian;
	private MyIntbox urutan;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Matapelajaran matapelajaran;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private MyDoublebox kkm;
	private Textbox namaEn;
	private Textbox namaAr;
	private Textbox namaCh;

	private Tabpanel subMatapelajaranTab;

	public void onSubMatapelajaran(Event event) {
		if (subMatapelajaranTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(subMatapelajaranTab);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/sub_matapelajaran.zul");
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
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "namaEn", "namaAr", "namaCh", "singkatan", "sekolah",
				"jenisPenilaian", "urutan", "kelompokMatapelajaran", "kkm", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Matapelajaran.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class MatapelajaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Matapelajaran matapelajaran = (Matapelajaran) arg1;
			new Label(matapelajaran.getKode()).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Matapelajaran.class, matapelajaran, matapelajaran.getNama()))
					.setParent(arg0);

			if (!matapelajaran.getNama().equalsIgnoreCase(matapelajaran.getNamaEn())) {
				new Label(matapelajaran.getNamaEn()).setParent(a);
			}
			if (!matapelajaran.getNama().equalsIgnoreCase(matapelajaran.getNamaAr())) {
				new Label(matapelajaran.getNamaAr()).setParent(a);
			}
			if (!matapelajaran.getNama().equalsIgnoreCase(matapelajaran.getNamaCh())) {
				new Label(matapelajaran.getNamaCh()).setParent(a);
			}

			new Label(matapelajaran.getSekolah() == null ? "" : matapelajaran.getSekolah().getNama()).setParent(arg0);
			new Label(matapelajaran.getJenisPenilaian() == null ? "" : matapelajaran.getJenisPenilaian().getJenis())
					.setParent(arg0);
			new Label(matapelajaran.getKelompokMatapelajaran() == null ? ""
					: matapelajaran.getKelompokMatapelajaran().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(matapelajaran.getUrutan())).setParent(arg0);
			new Label(Common.numberFormat.get().format(matapelajaran.getKkm())).setParent(arg0);
			new Label(matapelajaran.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(matapelajaran.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					matapelajaran.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(matapelajaran);
				}
			});
			Common.copyEditDeleteButtons(edit, delete, matapelajaran, MatapelajaranAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Matapelajaran());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		matapelajaran = (Matapelajaran) obj;
		init(matapelajaran);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final Matapelajaran matapelajaran) throws Exception {
		this.matapelajaran = matapelajaran;
		addWindow.setTitle(matapelajaran.getId() == null ? "Tambah Matapelajaran" : "Ubah Matapelajaran");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode *"));
		row.appendChild(kode = new Textbox(matapelajaran.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama (Indonesia) *"));
		row.appendChild(nama = new Textbox(matapelajaran.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Singkatan"));
		row.appendChild(singkatan = new Textbox(matapelajaran.getSingkatan()));
		singkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama (English)"));
		row.appendChild(namaEn = new Textbox(matapelajaran.getNamaEn()));
		namaEn.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama (Aksara Arab)"));
		row.appendChild(namaAr = new Textbox(matapelajaran.getNamaAr()));
		namaAr.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama (Aksara Tionghoa)"));
		row.appendChild(namaCh = new Textbox(matapelajaran.getNamaCh()));
		namaCh.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, matapelajaran.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, matapelajaran.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Penilaian *"));
		row.appendChild(jenisPenilaian = new Combobox());
		jenisPenilaian.setWidth("90%");
		jenisPenilaian.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Matapelajaran *"));
		row.appendChild(kelompokMatapelajaran = new Combobox());
		kelompokMatapelajaran.setWidth("90%");
		kelompokMatapelajaran.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);
				Common.insertCombo(jenisPenilaian, new String[] { "jenis", "sekolah" }, JenisPenilaian.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(jenisPenilaian, matapelajaran.getJenisPenilaian());

				Common.insertCombo(kelompokMatapelajaran, new String[] { "nama", "sekolah" },
						KelompokMatapelajaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(kelompokMatapelajaran, matapelajaran.getKelompokMatapelajaran());
			}
		};

		sekolah.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutan"));
		row.appendChild(urutan = new MyIntbox(matapelajaran.getUrutan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KKM"));
		row.appendChild(kkm = new MyDoublebox(matapelajaran.getKkm()));

		if (matapelajaran.getId() != null) {
			AktifitasPembelajaranHelper.tampilkanLampiran(rows, matapelajaran.getId(), null, "_matapelajaran", "", edit,
					"2");
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(matapelajaran.getKeterangan()));
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

		Common.createDefaultTimer(eventListener);
	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Matapelajaran belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Kode Matapelajaran dengan kode yang unik; (2) pastikan kode tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Matapelajaran belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Matapelajaran dengan nama yang jelas; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Yayasan belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Yayasan dan pilih yayasan yang sesuai; (2) pastikan yayasan terpilih sebelum menyimpan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Sekolah belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Sekolah dan pilih sekolah yang sesuai; (2) pastikan yayasan sudah dipilih agar daftar sekolah tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisPenilaian.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Penilaian belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Jenis Penilaian dan pilih jenis yang sesuai dari daftar; (2) pastikan jenis penilaian sudah terpilih; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kelompokMatapelajaran.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kelompok Matapelajaran belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Kelompok Matapelajaran dan pilih kelompok yang sesuai; (2) pastikan kelompok sudah terpilih; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (matapelajaran.getId() != null) {
			matapelajaran = (Matapelajaran) session.load(Matapelajaran.class, matapelajaran.getId());

		}
		matapelajaran.setKode(kode.getValue());
		matapelajaran.setNama(nama.getValue());

		matapelajaran.setNamaAr(namaAr.getValue());
		matapelajaran.setNamaCh(namaCh.getValue());
		matapelajaran.setNamaEn(namaEn.getValue());

		matapelajaran.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		matapelajaran.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		matapelajaran.setJenisPenilaian((JenisPenilaian) jenisPenilaian.getSelectedItem().getValue());
		matapelajaran.setKeterangan(keterangan.getValue());
		matapelajaran.setSingkatan(singkatan.getValue());
		matapelajaran.setUrutan(urutan.getValue());
		matapelajaran
				.setKelompokMatapelajaran((KelompokMatapelajaran) kelompokMatapelajaran.getSelectedItem().getValue());
		matapelajaran.setKkm(kkm.getValue());
		Common.refreshSaveOrUpdate(session, matapelajaran);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Matapelajaran.class);

		if (order)
			criteria.addOrder(Order.asc("urutan")).addOrder(Order.asc("nama"));
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

		List<Matapelajaran> matapelajaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(matapelajaran);
		grid.setRowRenderer(new MatapelajaranRenderer());
		grid.setModelCheckMobile(strset);

	}

}
