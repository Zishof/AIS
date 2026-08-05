package ais.action.master.kursus;

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
import org.zkoss.zul.Comboitem;
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
import ais.action.master.kursus.helper.AmbilDataPesertaKursusBanbox;
import ais.action.master.kursus.helper.AmbilDataProdukKursusBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.kursus.ProdukKursus;
import ais.database.model.kursus.UlasanKursus;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Moderasi review/rating kursus (UlasanKursus) oleh admin. Ulasan sendiri dibuat peserta
 * lewat dashboard self-service (kursus_service.jsp), halaman ini untuk sembunyikan/hapus ulasan.
 */
public class UlasanKursusAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private AmbilDataProdukKursusBanbox produkKursusBanbox;
	private AmbilDataPesertaKursusBanbox pesertaKursusBanbox;
	private Combobox rating;
	private Textbox komentar;
	private Checkbox aktif;

	private boolean edit = false;
	private boolean delete = false;

	private UlasanKursus ulasanKursus;
	private MyToolbarbuttonConfig add;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
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

		String[] contents = new String[] { "id", "kode", "nama", "rating", "komentar", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(UlasanKursus.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);
	}

	class UlasanKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final UlasanKursus ulasanKursus = (UlasanKursus) arg1;
			RevisiHelper.createNewRevisi(UlasanKursus.class, ulasanKursus, ulasanKursus.getNama()).setParent(arg0);

			new Label(ulasanKursus.getProdukKursus() == null ? "" : ulasanKursus.getProdukKursus().getNama())
					.setParent(arg0);
			new Label(ulasanKursus.getPesertaKursus() == null ? "" : ulasanKursus.getPesertaKursus().getNama())
					.setParent(arg0);
			new Label(ulasanKursus.getRating() + " / 5").setParent(arg0);
			new Label(ulasanKursus.getKomentar()).setParent(arg0);
			new Label(Common.dateFormat3.get().format(ulasanKursus.getTanggal())).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Tampilkan");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(ulasanKursus.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ulasanKursus.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(ulasanKursus);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, ulasanKursus, UlasanKursusAction.this).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new UlasanKursus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		ulasanKursus = (UlasanKursus) obj;
		init(ulasanKursus);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(UlasanKursus ulasanKursus) {
		this.ulasanKursus = ulasanKursus;
		addWindow.setTitle(ulasanKursus.getId() == null ? "Tambah Ulasan Kursus" : "Ubah Ulasan Kursus");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kursus *"));
		produkKursusBanbox = new AmbilDataProdukKursusBanbox();
		produkKursusBanbox
				.setValue(ulasanKursus.getProdukKursus() == null ? "" : ulasanKursus.getProdukKursus().getNama());
		produkKursusBanbox.setAttribute("produkKursus", ulasanKursus.getProdukKursus());
		row.appendChild(produkKursusBanbox);
		produkKursusBanbox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peserta *"));
		pesertaKursusBanbox = new AmbilDataPesertaKursusBanbox();
		pesertaKursusBanbox
				.setValue(ulasanKursus.getPesertaKursus() == null ? "" : ulasanKursus.getPesertaKursus().getNama());
		pesertaKursusBanbox.setAttribute("pesertaKursus", ulasanKursus.getPesertaKursus());
		row.appendChild(pesertaKursusBanbox);
		pesertaKursusBanbox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Rating"));
		row.appendChild(rating = new Combobox());
		rating.setReadonly(true);
		rating.setWidth("90%");
		for (int i = 5; i >= 1; i--) {
			Comboitem item = rating.appendItem(i + " / 5");
			item.setValue(i);
			if (i == ulasanKursus.getRating()) {
				rating.setSelectedItem(item);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Komentar"));
		row.appendChild(komentar = new Textbox(ulasanKursus.getKomentar()));
		komentar.setWidth("90%");
		komentar.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampilkan"));
		row.appendChild(aktif = new Checkbox());
		aktif.setChecked(ulasanKursus.getAktif());

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
		if (produkKursusBanbox.getAttribute("produkKursus") == null) {
			MyMessageboxConfig.show("Kursus harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (pesertaKursusBanbox.getAttribute("pesertaKursus") == null) {
			MyMessageboxConfig.show("Peserta harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (ulasanKursus.getId() != null) {
			ulasanKursus = (UlasanKursus) session.load(UlasanKursus.class, ulasanKursus.getId());
		}

		ulasanKursus.setProdukKursus((ProdukKursus) produkKursusBanbox.getAttribute("produkKursus"));
		ulasanKursus.setPesertaKursus((PesertaKursus) pesertaKursusBanbox.getAttribute("pesertaKursus"));
		ulasanKursus.setRating(rating.getSelectedItem() == null ? 5 : (Integer) rating.getSelectedItem().getValue());
		ulasanKursus.setKomentar(komentar.getValue());
		ulasanKursus.setAktif(aktif.isChecked());

		Common.refreshSaveOrUpdate(session, ulasanKursus);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(UlasanKursus.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("tanggal"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("komentar", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<UlasanKursus> ulasanKursus = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(ulasanKursus);
		grid.setRowRenderer(new UlasanKursusRenderer());
		grid.setModelCheckMobile(strset);
	}

}
