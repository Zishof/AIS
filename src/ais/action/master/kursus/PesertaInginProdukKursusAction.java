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
import org.zkoss.zul.Columns;
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
import ais.database.model.kursus.PesertaInginProdukKursus;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.kursus.ProdukKursus;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PesertaInginProdukKursusAction extends GenericAutowireComposer
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
	private Textbox searchproduk;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private PesertaInginProdukKursus pesertaInginProdukKursus;
	private MyToolbarbuttonConfig add;
	private Label kode;
	private AmbilDataPesertaKursusBanbox pesertaKursus;
	private AmbilDataProdukKursusBanbox produkKursus;
	private MyDatebox waktuIngin;

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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "produkKursus", "pesertaKursus",
				"waktuIngin" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PesertaInginProdukKursus.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PesertaInginProdukKursus.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class PesertaInginProdukKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			PesertaInginProdukKursus pesertaInginProdukKursus = (PesertaInginProdukKursus) arg1;
			new Label(pesertaInginProdukKursus.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(PesertaInginProdukKursus.class, pesertaInginProdukKursus,
					pesertaInginProdukKursus.getNama()).setParent(arg0);

			new Label(pesertaInginProdukKursus.getWaktuIngin() == null ? ""
					: Common.dateFormat5.get().format(pesertaInginProdukKursus.getWaktuIngin())).setParent(arg0);

			new Label(pesertaInginProdukKursus.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, pesertaInginProdukKursus, PesertaInginProdukKursusAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PesertaInginProdukKursus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pesertaInginProdukKursus = (PesertaInginProdukKursus) obj;
		init(pesertaInginProdukKursus);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PesertaInginProdukKursus pesertaInginProdukKursus) {
		this.pesertaInginProdukKursus = pesertaInginProdukKursus;
		addWindow.setTitle(pesertaInginProdukKursus.getId() == null ? "Tambah Pesanan Peserta" : "Ubah Pesanan Peserta");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Keinginan"));
		row.appendChild(kode = new Label(pesertaInginProdukKursus.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peserta *"));
		row.appendChild(pesertaKursus = new AmbilDataPesertaKursusBanbox());
		pesertaKursus.setAttribute("pesertaKursus", pesertaInginProdukKursus.getPesertaKursus());
		pesertaKursus.setAttribute("myValue", pesertaInginProdukKursus.getPesertaKursus());
		pesertaKursus.setValue(pesertaInginProdukKursus.getPesertaKursus() == null ? ""
				: pesertaInginProdukKursus.getPesertaKursus().getNama());
		pesertaKursus.setWidth("90%");
		pesertaKursus.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Produk *"));
		row.appendChild(produkKursus = new AmbilDataProdukKursusBanbox());
		produkKursus.setAttribute("produkKursus", pesertaInginProdukKursus.getProdukKursus());
		produkKursus.setAttribute("myValue", pesertaInginProdukKursus.getProdukKursus());
		produkKursus.setValue(pesertaInginProdukKursus.getProdukKursus() == null ? ""
				: pesertaInginProdukKursus.getProdukKursus().getNama());
		produkKursus.setWidth("90%");
		produkKursus.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu"));
		waktuIngin = new MyDatebox(pesertaInginProdukKursus.getWaktuIngin());
		waktuIngin.setReadonly(true);
		waktuIngin.setFormat(Common.dateFormat3.get().toPattern());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pesertaInginProdukKursus.getKeterangan()));
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
		if (pesertaKursus.getAttribute("pesertaKursus") == null) {
			MyMessageboxConfig.show("Peserta harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (produkKursus.getAttribute("produkKursus") == null) {
			MyMessageboxConfig.show("Produk harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pesertaInginProdukKursus.getId() != null) {
			pesertaInginProdukKursus = (PesertaInginProdukKursus) session.load(PesertaInginProdukKursus.class,
					pesertaInginProdukKursus.getId());

		}

		PesertaKursus pesertaKursus = (PesertaKursus) this.pesertaKursus.getAttribute("pesertaKursus");
		ProdukKursus produkKursus = (ProdukKursus) this.produkKursus.getAttribute("produkKursus");

		pesertaInginProdukKursus.setKode(kode.getValue());
		pesertaInginProdukKursus.setNama(pesertaKursus.getNama() + " " + produkKursus.getNama());
		pesertaInginProdukKursus.setPesertaKursus(pesertaKursus);
		pesertaInginProdukKursus.setProdukKursus(produkKursus);
		pesertaInginProdukKursus.setWaktuIngin(waktuIngin.getValue());
		pesertaInginProdukKursus.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, pesertaInginProdukKursus);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PesertaInginProdukKursus.class)
				.createAlias("pesertaKursus", "pesertaKursus").createAlias("produkKursus", "produkKursus");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("pesertaKursus.kode", searchnama.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("pesertaKursus.nama", searchnama.getValue().trim(),
										MatchMode.ANYWHERE))

				)

				.add(searchproduk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("produkKursus.kode", searchproduk.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("produkKursus.nama", searchproduk.getValue().trim(),
										MatchMode.ANYWHERE)))

		;
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PesertaInginProdukKursus> pesertaInginProdukKursus = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pesertaInginProdukKursus);
		grid.setRowRenderer(new PesertaInginProdukKursusRenderer());
		grid.setModelCheckMobile(strset);

	}

}
