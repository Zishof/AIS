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
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.DetailAbsenGuruPiketHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;
import ais.database.model.sekolah.AbsenGuruPiket;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class AbsenGuruPiketAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	protected Textbox searchketerangan;
	protected AmbilDataGuruBanbox searchguru;

	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchta;
	private Combobox searchsmt;

	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private AbsenGuruPiket absenGuruPiket;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private AmbilDataGuruBanbox guru;
	private Combobox tahunAjaran;
	private Combobox semester;

	private MyDatebox tanggal;

	protected Tabpanel absenPanel;
	private Combobox jamKe;

	public void onAbsen(Event event) {

		if (absenPanel.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(absenPanel);
			MyInclude iframe = new MyInclude("/welsis.zul");
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
		Common.generateTahunAjaran(searchta);

		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(1); }
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(2); }
		searchsmt.appendChild(comboitem);
		if (searchsmt != null) { searchsmt.setCols(2); }

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		if (searchsmt != null) { searchsmt.setReadonly(true); }

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

		String[] contents1 = new String[] { "id", "tahunAjaran", "semester", "tanggal", "jamKe", "guru", "sekolah",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents1);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AbsenGuruPiket.class, contents1);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

	        FilterLanjutHelper.setup(comp);
}

	class AbsenGuruPiketRenderer extends ais.ui.util.MyRowRenderer {

		private DetailAbsenGuruPiketHelper detailAbsenGuruPiketHelper = new DetailAbsenGuruPiketHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final AbsenGuruPiket absenGuruPiket = (AbsenGuruPiket) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detail.getChildren().isEmpty() && detail.isOpen()) {

						detailAbsenGuruPiketHelper.displayDetailPA(absenGuruPiket, detail, addWindow);

					}

				}

			});

			new Label(absenGuruPiket.getTahunAjaran() + "/" + absenGuruPiket.getSemester()).setParent(arg0);
			RevisiHelper
					.createNewRevisi(AbsenGuruPiket.class, absenGuruPiket,
							Common.dateFormat5.get().format(absenGuruPiket.getTanggal())
									+ (absenGuruPiket.getJamke() > 0 ? " jam ke " + absenGuruPiket.getJamke() : ""))
					.setParent(arg0);
			new Label(absenGuruPiket.getSekolah() == null ? "" : absenGuruPiket.getSekolah().getNama()).setParent(arg0);
			new Label(absenGuruPiket.getGuru() == null ? "" : absenGuruPiket.getGuru().getNama()).setParent(arg0);
			new Label("Jam ke-" + absenGuruPiket.getJamke()).setParent(arg0);
			new Label(absenGuruPiket.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, absenGuruPiket, AbsenGuruPiketAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new AbsenGuruPiket());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		absenGuruPiket = (AbsenGuruPiket) obj;
		init(absenGuruPiket);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final AbsenGuruPiket absenGuruPiket) throws Exception {
		this.absenGuruPiket = absenGuruPiket;
		addWindow.setTitle(absenGuruPiket.getId() == null ? "Tambah Absen Piket Guru" : "Ubah Absen Piket Guru");
		addWindow.setWidth("550px");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		Common.selectComboItem(true, tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
				absenGuruPiket.getTahunAjaran());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(semester = new Combobox());
		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(1);
		semester.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(2);
		semester.appendChild(comboitem);
		Common.selectComboItem(true, semester, absenGuruPiket.getSemester());
		semester.setWidth("90%");
		semester.setReadonly(true);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, absenGuruPiket.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, absenGuruPiket.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru Pembuat"));
		row.appendChild(guru = new AmbilDataGuruBanbox());

		if (searchguru.getAttribute("guru") != null) {
			absenGuruPiket.setGuru((Guru) searchguru.getAttribute("guru"));
			guru.setDisabled(searchguru.isDisabled());
		}

		guru.setAttribute("guru", absenGuruPiket.getGuru());
		guru.setAttribute("myValue", absenGuruPiket.getGuru());
		guru.setValue(absenGuruPiket.getGuru() == null ? "" : absenGuruPiket.getGuru().getNamaGuru());
		guru.setWidth("90%");
		guru.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal/Waktu Absen *"));
		row.appendChild(tanggal = new MyDatebox(absenGuruPiket.getTanggal()));
		tanggal.setFormat(Common.dateFormat3.get().toPattern());
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jam Ke"));
		row.appendChild(jamKe = new Combobox());
		jamKe.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				Common.clear(jamKe);
				List<Integer> jamKes = AbsenGuruPiket.jamKes(s == null || s.getId() == null ? 0L : s.getId());
				if (jamKes.isEmpty()) {
					Comboitem comboitem = new Comboitem("Tidak ada");
					comboitem.setValue(0);
					jamKe.appendChild(comboitem);
				} else {
					for (Integer jam : jamKes) {
						Comboitem comboitem = new Comboitem("Jam ke " + jam);
						comboitem.setValue(0);
						jamKe.appendChild(comboitem);
					}
				}
				Common.selectComboItem(true, jamKe, absenGuruPiket.getJamke());
			}
		};

		Common.createDefaultTimer(eventListener);
		sekolah.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(absenGuruPiket.getKeterangan()));
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

		Session session = HibernateUtil.currentSession();
		try {
			if (absenGuruPiket.getId() != null) {
				absenGuruPiket = (AbsenGuruPiket) session.load(AbsenGuruPiket.class, absenGuruPiket.getId());

			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/AbsenGuruPiketAction.java:401");
			// TODO: handle exception
		}
		absenGuruPiket.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		absenGuruPiket.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());

		absenGuruPiket.setGuru((Guru) guru.getAttribute("guru"));
		absenGuruPiket.setKeterangan(keterangan.getValue());
		absenGuruPiket.setTahunAjaran((String) tahunAjaran.getSelectedItem().getValue());
		absenGuruPiket.setSemester((Integer) semester.getSelectedItem().getValue());
		absenGuruPiket.setTanggal(tanggal.getValue());
		absenGuruPiket
				.setJamke((Integer) (jamKe.getSelectedItem() == null ? null : jamKe.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, absenGuruPiket);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AbsenGuruPiket.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchguru == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchguru.getAttribute("guru") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("guru", searchguru.getAttribute("guru")),
								Restrictions.eq("guru", searchguru.getAttribute("guru")))))

				.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
						|| searchsmt.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

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

		List<AbsenGuruPiket> absenGuruPiket = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(absenGuruPiket);
		grid.setRowRenderer(new AbsenGuruPiketRenderer());
		grid.setModelCheckMobile(strset);

	}

}
