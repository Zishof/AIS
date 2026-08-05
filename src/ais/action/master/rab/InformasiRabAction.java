package ais.action.master.rab;

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
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.InformasiRabPunyaFotoHelper;
import ais.action.master.rab.helper.InformasiRabPunyaKomentarHelper;
import ais.action.master.rab.util.RabUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.rab.InformasiRabDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoInformasiRab;
import ais.database.model.rab.InformasiRab;
import ais.database.model.rab.JenisInformasiRab;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class InformasiRabAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox jenisInformasiRab;

	private MyDatebox mulai;
	private MyDatebox sampai;
	private MyCkEditor content = new MyCkEditor();

	private boolean edit = false;
	private boolean delete = false;

	private InformasiRab informasiRab;
	private MyToolbarbuttonConfig add;
	private MyGrid gridDocument;
	@SuppressWarnings("unused")
	private MyGrid gridKomentar;

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

		@SuppressWarnings("unused")
		JenisInformasiRab informasiRab = RabUtil.INFORMASI;

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
	}

	class InformasiRabRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final InformasiRab informasiRab = (InformasiRab) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("450px");
						window.setWidth("100%");
						window.setParent(detail);
						initDetail(informasiRab, window);
					}
				}
			});

			RevisiHelper
					.createNewRevisi(InformasiRab.class, informasiRab,
							informasiRab.getSatuanKerja() == null ? "" : informasiRab.getSatuanKerja().toString())
					.setParent(arg0);

			new Label(informasiRab.getJenisInformasiRab() == null ? "" : informasiRab.getJenisInformasiRab().getNama())
					.setParent(arg0);

			new Label(informasiRab.getMulai() == null ? "" : Common.dateFormat6.get().format(informasiRab.getMulai()))
					.setParent(arg0);

			new Label(informasiRab.getSampai() == null ? "" : Common.dateFormat6.get().format(informasiRab.getSampai()))
					.setParent(arg0);

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">" + informasiRab.getContent() + "</font>")
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(informasiRab);
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
											InformasiRabDao informasiRabDao = DaoFactory.getInstance()
													.getInformasiRabDao();
											informasiRabDao.delete((informasiRab));
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
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
		init(new InformasiRab());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final InformasiRab informasiRab, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabInformasi = new MyTabConfig("Isi Informasi");
		tabInformasi.setParent(tabs);

		final MyTabConfig tabDocument = new MyTabConfig("File Lampiran");
		tabDocument.setParent(tabs);

		final MyTabConfig tabKomentar = new MyTabConfig("Komentar-Komentar");
		tabKomentar.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		content.setValue(informasiRab.getContent());
		content.setHeight("100%");
		content.setWidth("100%");

		final Tabpanel tabpanelInformasi = new ais.ui.util.MyTabpanel();
		tabpanelInformasi.setParent(tabpanels);
		tabpanelInformasi.appendChild(content);

		final Tabpanel tabpanelDocument = new ais.ui.util.MyTabpanel();
		tabpanelDocument.setParent(tabpanels);

		final Tabpanel tabpanelKomentar = new ais.ui.util.MyTabpanel();
		tabpanelKomentar.setParent(tabpanels);

		tabpanelDocument
				.appendChild(new InformasiRabPunyaFotoHelper(gridDocument = new MyGrid()).initDetail(informasiRab));

		tabpanelKomentar
				.appendChild(new InformasiRabPunyaKomentarHelper(gridKomentar = new MyGrid()).initDetail(informasiRab));

	}

	private void init(InformasiRab informasiRab) throws Exception {
		this.informasiRab = informasiRab;
		addWindow.setTitle(informasiRab.getId() == null ? "Tambah Informasi Rab" : "Ubah Informasi Rab");
		Common.clear(addWindow);

		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setAttribute("satuanKerja", informasiRab.getSatuanKerja());
		satuanKerja.setValue(informasiRab.getSatuanKerja() == null ? "" : informasiRab.getSatuanKerja().toString());

		mulai = new MyDatebox(informasiRab.getMulai());
		sampai = new MyDatebox(informasiRab.getSampai());
		mulai.setFormat(Common.dateFormat6.get().toPattern());
		sampai.setFormat(Common.dateFormat6.get().toPattern());

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		East east = new East();
		east.setWidth("70%");
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setParent(borderlayout);
		initDetail(informasiRab, east);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai);
		mulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai);
		sampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Informasi"));
		row.appendChild(jenisInformasiRab = new Combobox());
		Common.insertCombo(jenisInformasiRab, "nama", JenisInformasiRab.class);
		Common.selectComboItem(jenisInformasiRab, informasiRab.getJenisInformasiRab());
		jenisInformasiRab.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

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
		if (mulai.getValue() == null) {
			MyMessageboxConfig.show("Mulai terbit harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (jenisInformasiRab.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis informasi harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (content.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Content harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Satuan kerja harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsDocument = gridDocument.getRows().getChildren();
		for (Row row : rowsDocument) {
			FotoInformasiRab fotoInformasiRab = (FotoInformasiRab) row.getAttribute("fotoInformasiRab");
			if (fotoInformasiRab.getInformasiRab() == null) {
				MyMessageboxConfig.show("File harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		InformasiRabDao informasiRabDao = DaoFactory.getInstance().getInformasiRabDao();
		if (informasiRab.getId() != null) {
			informasiRab = informasiRabDao.load(informasiRab.getId());

		}

		informasiRab.setJenisInformasiRab((JenisInformasiRab) jenisInformasiRab.getSelectedItem().getValue());
		informasiRab.setMulai(mulai.getValue());
		informasiRab.setSampai(sampai.getValue());
		informasiRab.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		informasiRab.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
		informasiRab.setContent(content.getValue());

		if (informasiRab.getId() != null) {
			informasiRabDao.update(informasiRab);
		} else {
			informasiRabDao.save(informasiRab);
		}

		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsDocument) {
				FotoInformasiRab fotoInformasiRab = (FotoInformasiRab) row.getAttribute("fotoInformasiRab");
				fotoInformasiRab.setInformasiRab(informasiRab.getId());
				mysession.saveOrUpdate(fotoInformasiRab);
			}
			mysession.getTransaction().commit();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		StreamingHibernateUtil.getInstance().closeSession();

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(InformasiRab.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(Restrictions.ilike("content", searchnama.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<InformasiRab> informasiRab = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(informasiRab);
		grid.setRowRenderer(new InformasiRabRenderer());
		grid.setModelCheckMobile(strset);

	}

}
